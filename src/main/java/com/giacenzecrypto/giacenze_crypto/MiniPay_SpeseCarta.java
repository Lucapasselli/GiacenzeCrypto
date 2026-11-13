package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Il CSV delle spese della carta MiniPay ("card history" dell'app) come documento di supporto dei movimenti
 * {@code SPESA CON CARTA} già scaricati dalla catena ({@link Trans_MiniPay}).
 *
 * <p>Il CSV dice quanto è stato davvero speso (importo originale, in genere in euro) e a quale commerciante: il movimento
 * della catena conosce solo gli USDT usciti dalla Safe, valorizzati al prezzo di mercato. Qui si abbina ogni movimento ai
 * suoi pagamenti, si sostituisce il valore in euro con quello reale e si scrive nelle note il dettaglio del pagamento e lo
 * spread (valore salvato meno valore reale).</p>
 *
 * <p><b>Come si abbina.</b> I {@code Payment} non hanno l'hash della transazione (solo i {@code Deposit} l'hanno): si
 * abbina per importo. Un movimento della catena è la somma, per transazione, dei trasferimenti Safe verso il ponte, quindi
 * può raccogliere più pagamenti: si cercano i pagamenti <b>consecutivi nel tempo</b>, non successivi al movimento, la cui
 * somma in USD è esattamente la quantità uscita. Nessun abbinamento approssimato: ciò che non quadra resta com'è e finisce
 * nell'elenco dei non abbinati. Il comportamento reale del ponte (un trasferimento per pagamento o raggruppati) si è
 * verificato solo sul CSV di prova, quindi questo è il punto da rivedere sui dati veri.</p>
 *
 * <p><b>Stato dei pagamenti.</b> Contano i {@code settled} (o {@code approved}) <b>e i {@code pending} con un importo</b>
 * (dal 2026-11-13): il ponte della carta trasferisce dalla Safe due volte al giorno (06:00 e 18:00 UTC) e lo stato
 * {@code settled} del file arriva <i>dopo</i> l'uscita sulla catena, quindi un pagamento ancora {@code pending} può essere già
 * uscito e restare senza abbinamento per giorni. Un {@code pending} si abbina solo con lo stesso criterio esatto degli
 * altri (la somma in USD dei pagamenti consecutivi uguale al trasferimento), quindi un pagamento non ancora uscito semplicemente
 * non trova il suo movimento e resta com'è. Le preautorizzazioni a importo zero (PayPal, Amazon Prime) si ignorano. Un
 * movimento abbinato a un {@code pending} porta nelle note «pagamento in attesa di conferma»: alla prossima importazione, se
 * il file ha quel pagamento {@code settled} con gli stessi importi, la nota si toglie ({@link Conferma}); se l'importo è
 * cambiato il movimento non si tocca e si segnala. Una riga già abbinata si riconosce dal suo UUID ({@code Reference})
 * scritto nelle note, quindi rileggere lo stesso file, o uno più recente che lo contiene, non duplica niente.</p>
 *
 * <p><b>Data e divisione (2026-11-13).</b> L'orario che vale è quello del pagamento con la carta, non quello del giro del
 * ponte sulla catena. Quando i pagamenti di un movimento sono tutti {@code settled}, la somma torna e il movimento è
 * autonomo, il movimento unico della catena si <b>divide</b> in uno per pagamento ({@link #OP_DIVISIONE}) o, se il pagamento è
 * uno solo, si <b>sposta</b> all'ora del pagamento ({@link #OP_ORARIO}). Ogni pezzo ha quantità uguale all'addebito, valore reale
 * e una quota proporzionale del valore salvato, e tiene blocco, hash e lignaggio dell'originale: il blocco è ciò che impedisce a
 * un nuovo scaricamento di ricreare il movimento, e {@link Trans_MiniPay} salta comunque gli hash già abbinati
 * ({@link #HashAbbinati}). Il lignaggio porta nello storico la riga della catena, quindi {@code FormeImportate} la riconosce
 * ancora. Non si divide, e resta solo valore e note come prima, se un pagamento è ancora {@code pending} (la quantità dei pezzi
 * è strutturale e non si corregge se l'importo cambia), se il movimento è legato ad altri ({@code [20]}, {@code [43]},
 * {@code [45]}, riga generata, altra riga con lo stesso hash) o se anticipare l'orario peggiorerebbe il saldo del token nel
 * wallet (una ricarica arrivata fra il pagamento e il giro). La decisione si rivaluta a ogni importazione dalle sole note,
 * quindi un movimento già abbinato con la vecchia regola si divide alla prima importazione utile e uno già diviso non cambia.</p>
 *
 * <p>Nessuna Swing qui: la finestra sta in {@code Importazioni_Gestione}.</p>
 */
public class MiniPay_SpeseCarta {

    /** Inizio di ogni riga di note scritta qui: da lì si riconoscono i movimenti già abbinati. */
    static final String MARCATORE = "Carta MiniPay";
    static final String FONTE_PREZZO = "|||Personalizzato";
    /** Scritto nella riga di un pagamento abbinato quando il file lo dava ancora {@code pending}: si toglie alla conferma. */
    static final String ATTESA = "pagamento in attesa di conferma";
    /** Etichette dello storico modifiche: movimento diviso per pagamento, orario spostato a quello del pagamento. */
    static final String OP_DIVISIONE = "DividiPagamentiCarta";
    static final String OP_ORARIO = "OrarioPagamentoCarta";
    private static final int[] CAMPI_MOTORE_DA_SVUOTARE = {16, 19, 31, 33, 38};
    private static final Pattern SALVATO_IN_NOTA = Pattern.compile("valore salvato (-?[0-9]+,[0-9]{2}) EUR");
    /** Un movimento può chiudersi dopo il pagamento, mai prima (tolleranza per gli orologi). */
    static final long TOLLERANZA_DOPO_MS = 5 * 60_000L;
    /** Quanto prima del movimento può essere stato fatto il pagamento. */
    static final long FINESTRA_PRIMA_MS = 14L * 24 * 3600_000L;
    private static final String INTESTAZIONE = "date,type,status,description,merchant";
    private static final Pattern RIFERIMENTO = Pattern.compile("rif\\. ([0-9a-fA-F-]{36})");
    private static final Pattern RIFERIMENTO_IN_ATTESA = Pattern.compile(", " + Pattern.quote(ATTESA) + ", rif\\. ([0-9a-fA-F-]{36})");
    private static final Pattern PREFISSO_INDIRIZZO = Pattern.compile("0x[0-9a-fA-F]{6,}");

    /**
     * Lo stato di una riga conclusa: {@code settled} per i pagamenti, {@code approved} per i depositi. Il primo è quello
     * che l'app MiniPay scrive davvero (verificato sul file del 11/11/2026): il codice iniziale conosceva solo
     * {@code approved} e scartava tutti i pagamenti.
     */
    static boolean isStatoCompletato(String stato) {
        return "settled".equalsIgnoreCase(stato) || "approved".equalsIgnoreCase(stato);
    }

    /** Una riga del CSV. */
    record Riga(long istanteMs, String tipo, String stato, String descrizione, String commerciante, String citta,
            String paese, String mcc, BigDecimal importo, String valuta, String direzione, BigDecimal importoOriginale,
            String valutaOriginale, String hash, String riferimento) {

        boolean isPagamentoApprovato() {
            return "payment".equalsIgnoreCase(tipo) && isStatoCompletato(stato)
                    && "debit".equalsIgnoreCase(direzione) && importo != null && importo.signum() > 0;
        }

        /** Un pagamento ancora {@code pending} ma con un importo: puo' essere gia' uscito dalla Safe. */
        boolean isPagamentoInAttesa() {
            return "payment".equalsIgnoreCase(tipo) && "pending".equalsIgnoreCase(stato)
                    && "debit".equalsIgnoreCase(direzione) && importo != null && importo.signum() > 0;
        }

        /** Un {@code pending} che non e' un pagamento da abbinare (importo zero: preautorizzazione): si ignora. */
        boolean isPreautorizzazione() {
            return "payment".equalsIgnoreCase(tipo) && "pending".equalsIgnoreCase(stato) && !isPagamentoInAttesa();
        }
    }

    /** Un movimento gia' abbinato a un pagamento che nel file adesso e' concluso, con gli stessi importi: la nota si pulisce. */
    record Conferma(String[] movimento, String riferimento) {
    }

    /** Cosa si fa al movimento oltre a scrivere valore e note. */
    enum Esecuzione {
        /** Resta com'e' (orario della catena): ancora in attesa, collegato ad altri, saldo che non regge. */
        SOLO_VALORE,
        /** Un pagamento solo: il movimento prende l'ora del pagamento. */
        ORARIO,
        /** Piu' pagamenti: un movimento per pagamento, ognuno con la sua ora. */
        DIVISIONE
    }

    /** Un movimento della catena con i pagamenti che lo spiegano e i valori a confronto. */
    static final class Abbinamento {
        final String[] movimento;
        final List<Riga> pagamenti;
        /** Valore salvato sul movimento (prezzo di mercato), {@code null} se di un abbinamento passato non si legge dalle note. */
        final BigDecimal salvato;
        final BigDecimal reale;
        /** Scelto fra più combinazioni identiche per ordine di arrivo. */
        final boolean perOrdine;
        Esecuzione esecuzione = Esecuzione.SOLO_VALORE;
        /** Perche' un movimento che si potrebbe dividere o spostare resta com'e', vuoto se non c'e' niente da dire. */
        String motivo = "";
        /** Abbinato in passato: valore e note ci sono gia', qui serve solo la parte strutturale. */
        boolean giaScritto = false;
        /** Il valore reale di ogni pagamento (nello stesso ordine di {@link #pagamenti}), la cui somma e' {@link #reale}. */
        List<BigDecimal> realiPagamenti = List.of();
        /** Conferme delle note «in attesa» di un movimento abbinato in passato, da fare se poi non lo si ricostruisce. */
        List<Conferma> confermeSeNonRicostruito = List.of();

        Abbinamento(String[] movimento, List<Riga> pagamenti, BigDecimal salvato, BigDecimal reale, boolean perOrdine) {
            this.movimento = movimento;
            this.pagamenti = pagamenti;
            this.salvato = salvato;
            this.reale = reale;
            this.perOrdine = perOrdine;
        }

        /** Almeno un pagamento era ancora {@code pending} nel file: il valore e' provvisorio fino alla conferma. */
        boolean inAttesa() {
            for (Riga p : pagamenti) {
                if (p.isPagamentoInAttesa()) return true;
            }
            return false;
        }

        /**
         * Il valore salvato per i totali e i riepiloghi: se di un abbinamento passato non si legge dalle note
         * ({@code salvato == null}, per esempio perche' l'utente ha modificato la nota) vale quello reale, cioe' nessuno spread.
         * Chi somma o mostra il salvato deve passare di qui: {@code a.salvato} diretto da' NullPointerException.
         */
        BigDecimal salvatoOReale() {
            return salvato == null ? reale : salvato;
        }

        /** Valore salvato meno valore reale: positivo se il movimento era valorizzato più della spesa. */
        BigDecimal spread() {
            return salvatoOReale().subtract(reale);
        }

        BigDecimal spreadPercentuale() {
            return Percentuale(spread(), reale);
        }

        /** Tutti i pagamenti hanno l'importo originale in euro: nessuna conversione, quindi lo spread e' quello della carta. */
        boolean interamenteInEuro() {
            for (Riga p : pagamenti) {
                if (!"EUR".equalsIgnoreCase(p.valutaOriginale())) return false;
            }
            return true;
        }
    }

    /** Il risultato dell'analisi, prima di scrivere qualsiasi cosa. */
    static final class Piano {
        /** L'archivio su cui si e' pianificato: ci si sostituiscono i movimenti divisi o spostati. */
        Map<String, String[]> archivio;
        int righeLette = 0;
        int pagamentiApprovati = 0;
        /** {@code pending} con un importo letti dal file e non abbinati in passato: partecipano all'abbinamento come gli altri. */
        int pagamentiInAttesa = 0;
        /** {@code pending} a importo zero (preautorizzazioni): ignorati. */
        int preautorizzazioniIgnorate = 0;
        int altreRighe = 0;
        int pagamentiGiaAbbinati = 0;
        final List<Abbinamento> abbinamenti = new ArrayList<>();
        /** Movimenti abbinati in passato a un pagamento {@code pending} che il file ora da' concluso. */
        final List<Conferma> conferme = new ArrayList<>();
        /** Pagamenti abbinati in passato che nel file sono ancora {@code pending}. */
        int giaAbbinatiInAttesa = 0;
        /** Movimenti che restano all'orario della catena pur avendo tutti i pagamenti conclusi (diagnosi per l'utente). */
        final List<String> nonRiorari = new ArrayList<>();

        int movimentiDaDividere() {
            int n = 0;
            for (Abbinamento a : abbinamenti) if (a.esecuzione == Esecuzione.DIVISIONE) n++;
            return n;
        }

        int movimentiDaRiorare() {
            int n = 0;
            for (Abbinamento a : abbinamenti) if (a.esecuzione == Esecuzione.ORARIO) n++;
            return n;
        }

        /** Quanti movimenti nasceranno dalle divisioni. */
        int pezziDaCreare() {
            int n = 0;
            for (Abbinamento a : abbinamenti) if (a.esecuzione == Esecuzione.DIVISIONE) n += a.pagamenti.size();
            return n;
        }
        /** Movimenti già abbinati in precedenza (hanno il marcatore nelle note). */
        int movimentiGiaAbbinati = 0;
        /** Una riga di diagnosi per ogni pagamento o movimento che non si è abbinato. */
        final List<String> nonAbbinati = new ArrayList<>();

        BigDecimal totaleSalvato() {
            BigDecimal t = BigDecimal.ZERO;
            for (Abbinamento a : abbinamenti) t = t.add(a.salvatoOReale());
            return t;
        }

        BigDecimal totaleReale() {
            BigDecimal t = BigDecimal.ZERO;
            for (Abbinamento a : abbinamenti) t = t.add(a.reale);
            return t;
        }

        /** Media ponderata: somma delle differenze su somma dei valori reali, non media delle percentuali. */
        BigDecimal spreadPercentuale() {
            return Percentuale(totaleSalvato().subtract(totaleReale()), totaleReale());
        }

        /** Salvato, reale e numero dei movimenti, solo per le spese interamente in euro o solo per le altre. */
        BigDecimal[] totali(boolean inEuro) {
            BigDecimal salvato = BigDecimal.ZERO, reale = BigDecimal.ZERO;
            int n = 0;
            for (Abbinamento a : abbinamenti) {
                if (a.interamenteInEuro() != inEuro) continue;
                salvato = salvato.add(a.salvatoOReale());
                reale = reale.add(a.reale);
                n++;
            }
            return new BigDecimal[]{salvato, reale, BigDecimal.valueOf(n)};
        }
    }

    //=====================================================================================================
    //=== LETTURA DEL FILE
    //=====================================================================================================

    /**
     * Legge il CSV.
     * @throws IllegalArgumentException se l'intestazione non è quella della cronologia della carta MiniPay
     */
    static List<Riga> Leggi(String contenuto) {
        List<Riga> righe = new ArrayList<>();
        boolean intestazioneVista = false;
        for (String r : contenuto.replace("\r", "").split("\n")) {
            String riga = r.replace("﻿", "").trim();
            if (riga.isEmpty()) continue;
            String[] c = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");
            if (!intestazioneVista) {
                if (!riga.toLowerCase(Locale.ROOT).startsWith(INTESTAZIONE)) {
                    throw new IllegalArgumentException("L'intestazione non e' quella della cronologia della carta MiniPay.");
                }
                intestazioneVista = true;
                continue;
            }
            if (c.length < 15) continue;
            Instant istante;
            try {
                istante = Instant.parse(c[0].trim());
            } catch (RuntimeException e) {
                continue;
            }
            righe.add(new Riga(istante.toEpochMilli(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(), Spazi(c[5]),
                    c[6].trim(), c[7].trim(), Numero(c[8]), c[9].trim(), c[10].trim(), Numero(c[11]), c[12].trim(),
                    c[13].trim(), c[14].trim()));
        }
        if (!intestazioneVista) throw new IllegalArgumentException("Il file e' vuoto.");
        return righe;
    }

    /**
     * Il prefisso dell'indirizzo del wallet, dalle descrizioni dei {@code Deposit} ({@code Deposit from 0xabcdef01...}),
     * minuscolo, {@code null} se il file non ha depositi.
     */
    static String PrefissoWallet(List<Riga> righe) {
        for (Riga r : righe) {
            if (!"deposit".equalsIgnoreCase(r.tipo())) continue;
            Matcher m = PREFISSO_INDIRIZZO.matcher(r.descrizione());
            if (m.find()) return m.group().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * I wallet su Celo registrati in Gestione Wallet compatibili col file.
     * @param righeWallet le righe della tabella dei wallet ({@code indirizzo;rete;...})
     * @param prefisso il prefisso letto dai depositi ({@link #PrefissoWallet}), {@code null} se il file non ne ha: tutti i
     *        wallet Celo sono candidati
     * @return gli indirizzi come sono salvati (il maiuscolo/minuscolo fa parte del {@code [3]} dei movimenti), nell'ordine
     *         della tabella
     */
    static List<String> WalletCompatibili(Collection<String> righeWallet, String prefisso) {
        List<String> wallet = new ArrayList<>();
        for (String riga : righeWallet) {
            String[] p = riga.split(";");
            if (p.length < 2 || !Trans_MiniPay.RETE.equalsIgnoreCase(p[1].trim())) continue;
            String a = p[0].trim();
            if (prefisso == null || a.toLowerCase(Locale.ROOT).startsWith(prefisso)) wallet.add(a);
        }
        return wallet;
    }

    /** Il momento dell'ultima riga approvata del file (di qualunque tipo), 0 se non ce ne sono. */
    static long UltimoApprovatoMs(List<Riga> righe) {
        long massimo = 0;
        for (Riga r : righe) {
            if (isStatoCompletato(r.stato())) massimo = Math.max(massimo, r.istanteMs());
        }
        return massimo;
    }

    /** Il momento del movimento piu' recente fra quelli dei nomi di wallet indicati, 0 se non ce ne sono. */
    static long UltimoMovimentoMs(Map<String, String[]> archivio, Set<String> nomiWallet) {
        long massimo = 0;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 3 || v[3] == null || !nomiWallet.contains(v[3].trim().toLowerCase(Locale.ROOT))) continue;
            massimo = Math.max(massimo, Istante(v));
        }
        return massimo;
    }

    /** I nomi di wallet (il {@code [3]}, minuscolo) del wallet MiniPay e della sua Safe, se gia' nota. */
    static Set<String> NomiWallet(String wallet, String safe) {
        Set<String> nomi = new HashSet<>();
        nomi.add((wallet.trim() + " (" + Trans_MiniPay.RETE + ")").toLowerCase(Locale.ROOT));
        if (safe != null && !safe.isBlank()) nomi.add((safe.trim() + " (" + Trans_MiniPay.RETE + ")").toLowerCase(Locale.ROOT));
        return nomi;
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================

    /**
     * Cerca per ogni spesa della catena i pagamenti che la spiegano. Non modifica niente.
     *
     * @param righe il file letto con {@link #Leggi}
     * @param archivio i movimenti ({@code Principale.MappaCryptoWallet})
     * @param nomiWallet i nomi di wallet (il {@code [3]}: {@code indirizzo (CELO)}) del wallet MiniPay e della sua Safe,
     *        minuscoli
     * @param cambioUsdEur converte un importo in USD in euro per un giorno ({@code yyyy-MM-dd}), {@code null} se non
     *        c'e' il cambio
     */
    static Piano Pianifica(List<Riga> righe, Map<String, String[]> archivio, Set<String> nomiWallet,
            BiFunction<String, String, String> cambioUsdEur) {
        Piano piano = new Piano();
        piano.archivio = archivio;
        piano.righeLette = righe.size();
        Set<String> giaScritti = RiferimentiGiaScritti(archivio.values());

        List<Riga> pagamenti = new ArrayList<>();
        for (Riga r : righe) {
            if (r.isPagamentoApprovato() || r.isPagamentoInAttesa()) {
                if (giaScritti.contains(r.riferimento().toLowerCase(Locale.ROOT))) {
                    piano.pagamentiGiaAbbinati++;
                    if (r.isPagamentoInAttesa()) piano.giaAbbinatiInAttesa++;
                    continue;
                }
                if (r.isPagamentoInAttesa()) piano.pagamentiInAttesa++;
                else piano.pagamentiApprovati++;
                pagamenti.add(r);
            } else if (r.isPreautorizzazione()) {
                piano.preautorizzazioniIgnorate++;
            } else {
                piano.altreRighe++;
            }
        }
        pagamenti.sort((a, b) -> Long.compare(a.istanteMs(), b.istanteMs()));

        List<String[]> spese = new ArrayList<>();
        List<String[]> giaAbbinati = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (!isSpesaCarta(v) || !nomiWallet.contains(v[3].trim().toLowerCase(Locale.ROOT))) continue;
            if (v[21] != null && v[21].contains(MARCATORE)) {
                piano.movimentiGiaAbbinati++;
                giaAbbinati.add(v);
                continue;
            }
            spese.add(v);
        }
        spese.sort((a, b) -> Long.compare(Istante(a), Istante(b)));

        Set<Riga> usati = new HashSet<>();
        for (String[] v : spese) {
            BigDecimal quantita = Quantita(v);
            long ms = Istante(v);
            if (quantita == null || ms == 0) {
                piano.nonAbbinati.add(Descrivi(v) + ": quantita' o data non leggibili");
                continue;
            }
            List<Riga> candidati = new ArrayList<>();
            for (Riga p : pagamenti) {
                if (usati.contains(p)) continue;
                if (p.istanteMs() <= ms + TOLLERANZA_DOPO_MS && p.istanteMs() >= ms - FINESTRA_PRIMA_MS) candidati.add(p);
            }
            List<List<Riga>> soluzioni = Combinazioni(candidati, quantita);
            if (soluzioni.isEmpty()) {
                piano.nonAbbinati.add(Descrivi(v) + ": nessun pagamento del file ha questo importo");
                continue;
            }
            boolean perOrdine = false;
            if (soluzioni.size() > 1) {
                if (!TutteEquivalenti(soluzioni)) {
                    piano.nonAbbinati.add(Descrivi(v) + ": " + soluzioni.size() + " combinazioni di pagamenti diverse danno"
                            + " questo importo, non scelgo");
                    continue;
                }
                perOrdine = true;
            }
            List<Riga> scelta = soluzioni.get(0);
            BigDecimal reale = BigDecimal.ZERO;
            boolean cambioMancante = false;
            List<BigDecimal> reali = new ArrayList<>();
            for (Riga p : scelta) {
                BigDecimal e = ValoreEuro(p, cambioUsdEur);
                if (e == null) {
                    cambioMancante = true;
                    break;
                }
                reale = reale.add(e);
                reali.add(e);
            }
            if (cambioMancante) {
                piano.nonAbbinati.add(Descrivi(v) + ": cambio USD/EUR non disponibile per un pagamento");
                continue;
            }
            usati.addAll(scelta);
            BigDecimal salvato = Numero(v[15]);
            Abbinamento nuovo = new Abbinamento(v, scelta, (salvato == null ? BigDecimal.ZERO : salvato).setScale(2, RoundingMode.HALF_UP),
                    reale.setScale(2, RoundingMode.HALF_UP), perOrdine);
            nuovo.realiPagamenti = reali;
            piano.abbinamenti.add(nuovo);
        }
        for (Riga p : pagamenti) {
            //Un pending che non ha trovato il suo movimento e' il caso normale (non e' ancora uscito dalla Safe): si conta,
            //non e' una anomalia da elencare
            if (!usati.contains(p) && !p.isPagamentoInAttesa()) {
                piano.nonAbbinati.add("Pagamento " + Data(p) + " " + p.commerciante() + " " + p.importo().toPlainString()
                        + " " + p.valuta() + ": nessun movimento della catena corrispondente (non ancora scaricato o non"
                        + " ancora regolato)");
            }
        }
        //Movimenti abbinati in passato: conferma dei pagamenti ora conclusi e, se manca, divisione o spostamento d'orario
        List<Abbinamento> daStrutturare = new ArrayList<>(piano.abbinamenti);
        for (String[] v : giaAbbinati) {
            Abbinamento a = AbbinamentoGiaScritto(v, righe, cambioUsdEur, piano);
            if (a != null) {
                daStrutturare.add(a);
                piano.abbinamenti.add(a);
            }
        }
        DecidiStruttura(daStrutturare, archivio, piano);
        return piano;
    }

    /**
     * Rivaluta un movimento abbinato in passato. Conferma le righe scritte «in attesa» il cui UUID e' adesso concluso con gli
     * stessi importi ({@link Conferma}); con importi diversi segnala e non tocca niente. Se tutti i pagamenti del movimento
     * sono conclusi e il movimento non e' ancora all'ora del pagamento (o e' ancora unico con piu' pagamenti), ritorna
     * un abbinamento {@code giaScritto} da dividere o spostare ({@link #DecidiStruttura}); altrimenti {@code null}.
     */
    private static Abbinamento AbbinamentoGiaScritto(String[] movimento, List<Riga> righe,
            BiFunction<String, String, String> cambioUsdEur, Piano piano) {
        List<String> uuid = new ArrayList<>();
        Matcher m = RIFERIMENTO.matcher(movimento[21]);
        while (m.find()) uuid.add(m.group(1));
        if (uuid.isEmpty()) return null;

        List<Riga> pagamenti = new ArrayList<>();
        for (String u : uuid) {
            Riga trovata = null;
            for (Riga r : righe) {
                if (r.riferimento().equalsIgnoreCase(u) && (r.isPagamentoApprovato() || r.isPagamentoInAttesa())) {
                    trovata = r;
                    break;
                }
            }
            //Il file non ha tutti i pagamenti del movimento: non si puo' dividere, resta com'e'
            if (trovata == null) {
                CercaConferme(movimento, righe, piano);
                return null;
            }
            pagamenti.add(trovata);
        }

        List<Conferma> conferme = new ArrayList<>();
        boolean tuttiConclusi = true;
        boolean importiUguali = true;
        for (Riga r : pagamenti) {
            if (r.isPagamentoInAttesa()) {
                tuttiConclusi = false;
                continue;
            }
            String riga = RigaDelRiferimento(movimento[21], r.riferimento());
            boolean uguali = riga != null && riga.contains(FrammentoImporti(r));
            if (riga != null && riga.contains(", " + ATTESA + ", rif. ")) {
                if (uguali) {
                    conferme.add(new Conferma(movimento, r.riferimento()));
                } else {
                    piano.nonAbbinati.add(Descrivi(movimento) + ": il pagamento " + Data(r) + " " + r.commerciante()
                            + " e' concluso con un importo diverso da quello in attesa, il movimento non si tocca");
                    return null;
                }
            }
            if (!uguali) importiUguali = false;
        }
        if (!tuttiConclusi) {
            piano.conferme.addAll(conferme);
            return null;
        }
        BigDecimal quantita = Quantita(movimento);
        BigDecimal somma = BigDecimal.ZERO;
        for (Riga r : pagamenti) somma = somma.add(r.importo());
        if (!importiUguali || quantita == null || somma.compareTo(quantita) != 0) {
            piano.conferme.addAll(conferme);
            return null;
        }
        pagamenti.sort(Comparator.comparingLong(Riga::istanteMs));
        //Gia' all'ora del suo unico pagamento: niente da fare oltre alla conferma delle note
        if (pagamenti.size() == 1 && AllOraDi(movimento, pagamenti.get(0).istanteMs())) {
            piano.conferme.addAll(conferme);
            return null;
        }
        List<BigDecimal> reali = new ArrayList<>();
        BigDecimal reale = BigDecimal.ZERO;
        for (Riga r : pagamenti) {
            BigDecimal e = ValoreEuro(r, cambioUsdEur);
            if (e == null) {
                piano.conferme.addAll(conferme);
                return null;
            }
            reali.add(e);
            reale = reale.add(e);
        }
        Abbinamento a = new Abbinamento(movimento, pagamenti, SalvatoDaNota(movimento[21]),
                reale.setScale(2, RoundingMode.HALF_UP), false);
        a.giaScritto = true;
        a.realiPagamenti = reali;
        a.confermeSeNonRicostruito = conferme;
        return a;
    }

    /**
     * Le righe scritte «in attesa» di un movimento senza ricostruzione possibile: conferma quelle il cui UUID e' adesso
     * concluso con gli stessi importi, segnala quelle con importo diverso.
     */
    private static void CercaConferme(String[] movimento, List<Riga> righe, Piano piano) {
        Matcher m = RIFERIMENTO_IN_ATTESA.matcher(movimento[21]);
        while (m.find()) {
            String uuid = m.group(1);
            for (Riga r : righe) {
                if (!r.riferimento().equalsIgnoreCase(uuid) || !r.isPagamentoApprovato()) continue;
                String riga = RigaDelRiferimento(movimento[21], uuid);
                if (riga != null && riga.contains(FrammentoImporti(r))) {
                    piano.conferme.add(new Conferma(movimento, uuid));
                } else {
                    piano.nonAbbinati.add(Descrivi(movimento) + ": il pagamento " + Data(r) + " " + r.commerciante()
                            + " e' concluso con un importo diverso da quello in attesa, il movimento non si tocca");
                }
            }
        }
    }

    /** Il valore salvato scritto nelle note di un abbinamento passato, {@code null} se non si legge. */
    static BigDecimal SalvatoDaNota(String note) {
        Matcher m = SALVATO_IN_NOTA.matcher(note == null ? "" : note);
        return m.find() ? Numero(m.group(1).replace(',', '.')) : null;
    }

    /** La riga di note (separate da {@code <br>}) che contiene il riferimento, {@code null} se non c'e'. */
    private static String RigaDelRiferimento(String note, String uuid) {
        for (String riga : note.split("<br>")) {
            if (riga.toLowerCase(Locale.ROOT).contains("rif. " + uuid.toLowerCase(Locale.ROOT))) return riga;
        }
        return null;
    }

    /** Il pezzo di nota che dice quanto e' stato addebitato e quanto era l'importo originale. */
    private static String FrammentoImporti(Riga p) {
        StringBuilder s = new StringBuilder("addebito ").append(Importo(p.importo())).append(" ").append(p.valuta());
        if (p.importoOriginale() != null && !p.valutaOriginale().isBlank()) {
            s.append(", importo originale ").append(Importo(p.importoOriginale())).append(" ").append(p.valutaOriginale());
        }
        return s.toString();
    }

    //=====================================================================================================
    //=== DATA DEL PAGAMENTO: DIVISIONE E SPOSTAMENTO
    //=====================================================================================================

    /** Una variazione del saldo di un token in un wallet, per verificare che anticipare l'orario non lo renda negativo. */
    private record Evento(long ms, BigDecimal delta, String[] riga) {
    }

    /**
     * Per ogni abbinamento decide se il movimento si divide, si sposta all'ora del pagamento o resta com'e', e perche'.
     * Non modifica l'archivio. Gli abbinamenti passati ({@code giaScritto}) che restano com'erano escono dal piano: di loro
     * rimane la conferma delle note, o una riga di diagnosi.
     */
    private static void DecidiStruttura(List<Abbinamento> candidati, Map<String, String[]> archivio, Piano piano) {
        Map<String, Integer> righePerHash = new java.util.HashMap<>();
        for (String[] w : archivio.values()) {
            if (w == null || w.length <= 24 || w[3] == null || Funzioni.noData(w[24])) continue;
            righePerHash.merge(ChiaveHash(w), 1, Integer::sum);
        }
        Map<String, List<Evento>> saldi = new java.util.HashMap<>();
        List<Abbinamento> ordinati = new ArrayList<>(candidati);
        ordinati.sort(Comparator.comparingLong(a -> Istante(a.movimento)));
        for (Abbinamento a : ordinati) {
            String[] v = a.movimento;
            a.esecuzione = Esecuzione.SOLO_VALORE;
            a.motivo = "";
            boolean unico = a.pagamenti.size() == 1;
            if (a.inAttesa()) {
                a.motivo = "un pagamento e' ancora in attesa: orario e divisione quando il file lo da' concluso";
            } else if (unico && AllOraDi(v, a.pagamenti.get(0).istanteMs())) {
                //Gia' all'ora del pagamento
            } else {
                String non = NonStrutturabile(v, righePerHash);
                if (non != null) {
                    a.motivo = non;
                } else {
                    List<Evento> eventi = saldi.computeIfAbsent(ChiaveSaldo(v), k -> EventiSaldo(v, archivio));
                    List<Evento> dopo = SaldoDopo(eventi, a);
                    if (SaldoNonPeggiora(eventi, dopo, a)) {
                        a.esecuzione = unico ? Esecuzione.ORARIO : Esecuzione.DIVISIONE;
                        saldi.put(ChiaveSaldo(v), dopo);
                    } else {
                        a.motivo = "anticipare l'orario renderebbe negativo il saldo del token nel wallet (una ricarica e' arrivata fra"
                                + " il pagamento e il giro della catena)";
                    }
                }
            }
            if (a.esecuzione == Esecuzione.SOLO_VALORE && a.giaScritto) {
                piano.abbinamenti.remove(a);
                piano.conferme.addAll(a.confermeSeNonRicostruito);
                if (!a.motivo.isBlank() && !a.inAttesa()) {
                    piano.nonRiorari.add(Descrivi(v) + ": resta all'orario della catena, " + a.motivo);
                }
            }
        }
    }

    /** Perche' un movimento non si puo' dividere o spostare da solo, {@code null} se si puo'. */
    private static String NonStrutturabile(String[] v, Map<String, Integer> righePerHash) {
        if ("AU".equalsIgnoreCase(v[22])) return "e' una riga generata da una classificazione";
        if (!Funzioni.noData(v[20]) || (v.length > 43 && !Funzioni.noData(v[43])) || (v.length > 45 && !Funzioni.noData(v[45]))) {
            return "e' collegato ad altri movimenti (classificazione, commissione o operazione)";
        }
        if (v[0].split("_").length < 5) return "l'ID non e' nella forma attesa";
        if (!Funzioni.noData(v[13]) || Funzioni.noData(v[10])) return "non e' un'uscita semplice di un solo token";
        if (!Funzioni.noData(v[24]) && righePerHash.getOrDefault(ChiaveHash(v), 0) > 1) {
            return "altre righe della stessa transazione sulla catena (commissione o altra gamba)";
        }
        return null;
    }

    private static String ChiaveHash(String[] v) {
        return v[3].trim().toLowerCase(Locale.ROOT) + "|" + v[24].trim().toLowerCase(Locale.ROOT);
    }

    private static String ChiaveSaldo(String[] v) {
        return (v[3].trim() + "|" + v[8].trim() + "|" + (v.length > 26 ? v[26].trim() : "")).toLowerCase(Locale.ROOT);
    }

    /** Le variazioni del token del movimento nel suo wallet, da tutto l'archivio. */
    private static List<Evento> EventiSaldo(String[] v, Map<String, String[]> archivio) {
        List<Evento> eventi = new ArrayList<>();
        String wallet = v[3].trim(), simbolo = v[8].trim(), address = v.length > 26 ? v[26].trim() : "";
        for (String[] w : archivio.values()) {
            if (w == null || w.length <= 28 || w[3] == null || !wallet.equalsIgnoreCase(w[3].trim())) continue;
            BigDecimal delta = BigDecimal.ZERO;
            BigDecimal usc = Numero(w[10]), ent = Numero(w[13]);
            if (usc != null && simbolo.equalsIgnoreCase(w[8].trim()) && address.equalsIgnoreCase(w[26].trim())) delta = delta.subtract(usc.abs());
            if (ent != null && simbolo.equalsIgnoreCase(w[11].trim()) && address.equalsIgnoreCase(w[28].trim())) delta = delta.add(ent.abs());
            if (delta.signum() != 0) eventi.add(new Evento(Istante(w), delta, w));
        }
        return eventi;
    }

    /** La stessa linea del tempo col movimento sostituito dai suoi pagamenti, ognuno all'ora in cui e' stato fatto. */
    private static List<Evento> SaldoDopo(List<Evento> eventi, Abbinamento a) {
        List<Evento> dopo = new ArrayList<>();
        for (Evento e : eventi) {
            if (e.riga() != a.movimento) dopo.add(e);
        }
        for (Riga p : a.pagamenti) dopo.add(new Evento(p.istanteMs(), p.importo().negate(), null));
        return dopo;
    }

    /**
     * Anticipare i pagamenti non deve far scendere il saldo piu' di quanto fosse gia' sceso: dall'istante piu' lontano toccato
     * in poi, il saldo minimo dopo non e' sotto il minore fra quello di prima e zero.
     */
    private static boolean SaldoNonPeggiora(List<Evento> prima, List<Evento> dopo, Abbinamento a) {
        long da = Istante(a.movimento);
        for (Riga p : a.pagamenti) da = Math.min(da, p.istanteMs());
        BigDecimal minPrima = MinimoDa(prima, da);
        BigDecimal minDopo = MinimoDa(dopo, da);
        return minDopo.compareTo(minPrima.min(BigDecimal.ZERO)) >= 0;
    }

    /** Il saldo piu' basso dall'istante {@code da} in poi (compreso quello con cui si entra nel tratto). */
    private static BigDecimal MinimoDa(List<Evento> eventi, long da) {
        List<Evento> ordinati = new ArrayList<>(eventi);
        //A pari istante prima le entrate: e' la lettura piu' favorevole e coincide con quella dei movimenti nati insieme
        ordinati.sort(Comparator.comparingLong(Evento::ms).thenComparing(e -> e.delta().negate()));
        BigDecimal corrente = BigDecimal.ZERO, minimo = null;
        for (Evento e : ordinati) {
            if (e.ms() >= da && minimo == null) minimo = corrente;
            corrente = corrente.add(e.delta());
            if (e.ms() >= da) minimo = minimo.min(corrente);
        }
        return minimo == null ? corrente : minimo;
    }

    /** Pagamenti consecutivi, nell'ordine di tempo dei candidati, la cui somma e' esattamente {@code quantita}. */
    private static List<List<Riga>> Combinazioni(List<Riga> candidati, BigDecimal quantita) {
        List<List<Riga>> soluzioni = new ArrayList<>();
        for (int i = 0; i < candidati.size(); i++) {
            BigDecimal somma = BigDecimal.ZERO;
            for (int j = i; j < candidati.size(); j++) {
                somma = somma.add(candidati.get(j).importo());
                int c = somma.compareTo(quantita);
                if (c == 0) {
                    soluzioni.add(new ArrayList<>(candidati.subList(i, j + 1)));
                    break;
                }
                if (c > 0) break;
            }
        }
        return soluzioni;
    }

    /** Stessa sequenza di importi e di valori originali: scegliere l'una o l'altra non cambia nessun valore. */
    private static boolean TutteEquivalenti(List<List<Riga>> soluzioni) {
        List<Riga> prima = soluzioni.get(0);
        for (List<Riga> s : soluzioni) {
            if (s.size() != prima.size()) return false;
            for (int i = 0; i < s.size(); i++) {
                Riga a = s.get(i), b = prima.get(i);
                if (a.importo().compareTo(b.importo()) != 0 || !Uguali(a.importoOriginale(), b.importoOriginale())
                        || !a.valutaOriginale().equalsIgnoreCase(b.valutaOriginale())) return false;
            }
        }
        return true;
    }

    private static boolean Uguali(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    /**
     * Il valore in euro del pagamento: l'importo originale se e' in euro, altrimenti l'addebito in USD al cambio del
     * giorno ({@code null} se manca).
     */
    static BigDecimal ValoreEuro(Riga p, BiFunction<String, String, String> cambioUsdEur) {
        if ("EUR".equalsIgnoreCase(p.valutaOriginale()) && p.importoOriginale() != null) return p.importoOriginale();
        String euro = cambioUsdEur.apply(p.importo().toPlainString(), FunzioniDate.ConvertiDatadaLong(p.istanteMs()));
        if (euro == null) return null;
        try {
            return new BigDecimal(euro);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    //=====================================================================================================
    //=== SCRITTURA
    //=====================================================================================================

    /**
     * Applica il piano: per ogni movimento valore e note (con lo storico delle modifiche) e, se il piano lo prevede, divisione
     * in un movimento per pagamento o spostamento all'ora del pagamento; poi il collegamento del documento a tutto il gruppo.
     * Una divisione che non si riesce a fare (ID non univoco) ripiega su valore e note, senza lasciare niente a meta'.
     * Va chiamata sul thread della finestra: sostituisce righe dell'archivio che le tabelle leggono.
     * @param idDocumento il documento di supporto registrato, {@code 0} se la registrazione non e' riuscita
     * @return quanti movimenti sono stati modificati
     */
    static int Applica(Piano piano, int idDocumento) {
        int n = 0;
        for (Abbinamento a : piano.abbinamenti) {
            String[] v = a.movimento;
            if (a.esecuzione == Esecuzione.DIVISIONE && Dividi(piano, a, idDocumento)) {
                n++;
                continue;
            }
            if (!a.giaScritto) ScriviValoreENote(a, a.esecuzione == Esecuzione.ORARIO);
            String[] finale = v;
            if (a.esecuzione == Esecuzione.ORARIO) {
                String[] spostato = SpostaIstante(piano.archivio, v, a.pagamenti.get(0).istanteMs());
                if (spostato != null) finale = spostato;
            }
            if (idDocumento > 0) DocumentiFonte.CollegaAlGruppo(finale, idDocumento);
            n++;
        }
        for (Conferma c : piano.conferme) {
            String[] v = c.movimento();
            String nota = v[21] == null ? "" : v[21];
            String nuova = nota.replace(", " + ATTESA + ", rif. " + c.riferimento(), ", rif. " + c.riferimento());
            if (nuova.equals(nota)) continue;
            //Stesso valore, stesse informazioni sul prezzo: cambia solo la nota (e la modifica entra nello storico)
            GUI_ModificaMovimento.ScriviSoloPrezzoENote(v, v[15], nuova, true, v[40]);
            if (idDocumento > 0) DocumentiFonte.CollegaAlGruppo(v, idDocumento);
            n++;
        }
        return n;
    }

    /** Scrive sul movimento il valore reale e le note del pagamento, senza cambiare l'orario. */
    private static void ScriviValoreENote(Abbinamento a, boolean conOrarioSpostato) {
        String[] v = a.movimento;
        String note = v[21] == null ? "" : v[21];
        String nuove = Note(a);
        if (conOrarioSpostato) nuove += "<br>" + RigaOrarioCatena(v);
        GUI_ModificaMovimento.ScriviSoloPrezzoENote(v, a.reale.toPlainString(),
                note.isBlank() ? nuove : note + "<br>" + nuove, true, FONTE_PREZZO);
    }

    private static String RigaOrarioCatena(String[] v) {
        return MARCATORE + ": orario del movimento sulla catena " + FunzioniDate.ConvertiDatadaLongAlSecondo(Istante(v))
                + ", spostato a quello del pagamento";
    }

    /**
     * Il movimento e' gia' all'istante {@code ms}. Si confronta la forma memorizzata (il prefisso a 14 cifre dell'ID, ora locale),
     * non i millisecondi: nell'ora che in ottobre si ripete due istanti diversi hanno lo stesso prefisso, e col confronto in
     * millisecondi il movimento verrebbe spostato a ogni importazione.
     */
    static boolean AllOraDi(String[] v, long ms) {
        return v[0] != null && v[0].split("_")[0].equals(FunzioniDate.FormattaDataID(ms));
    }

    /** Il {@code [1]} (al minuto) di un istante. */
    private static String Minuto(long ms) {
        String data = FunzioniDate.ConvertiDatadaLongAlSecondo(ms).trim();
        return data.substring(0, data.length() - 3);
    }

    private static void SvuotaCampiMotore(String[] r) {
        for (int c : CAMPI_MOTORE_DA_SVUOTARE) if (c < r.length) r[c] = "";
        if (r.length > 17) r[17] = "";
    }

    /** Un nuovo ID con l'istante cambiato e il resto uguale, univoco nell'archivio ({@code null} se non si riesce). */
    private static String IdConIstante(Map<String, String[]> archivio, String[] v, long ms) {
        String[] seg = v[0].split("_");
        if (seg.length < 5) return null;
        return MovimentiCrypto.getIDUnivoco(archivio, FunzioniDate.FormattaDataID(ms) + "_" + seg[1] + "_" + seg[2] + "_"
                + seg[3] + "_" + seg[4]);
    }

    /**
     * Sposta un movimento all'istante indicato, come Trasla Orario ma per un movimento autonomo: ID e {@code [1]},
     * {@code [29]}, campi del motore svuotati, riga d'origine nello storico. Il lignaggio resta quello del movimento.
     * @return la nuova riga, {@code null} se non si e' potuto (il movimento resta dov'e')
     */
    static String[] SpostaIstante(Map<String, String[]> archivio, String[] v, long ms) {
        if (AllOraDi(v, ms)) return v;
        String id = IdConIstante(archivio, v, ms);
        if (id == null) return null;
        String lignaggio = MovimentiStorico.AssicuraLignaggio(v);
        String[] r = v.clone();
        r[0] = id;
        r[1] = Minuto(ms);
        r[29] = String.valueOf(ms);
        SvuotaCampiMotore(r);
        MovimentiStorico.AccodaModifica(lignaggio, id, v[0], Importazioni.SerializzaRiga(v), OP_ORARIO);
        archivio.remove(v[0]);
        archivio.put(id, r);
        return r;
    }

    /**
     * Divide il movimento in uno per pagamento. Ogni pezzo e' una copia dell'originale con l'ID e l'istante del pagamento,
     * la quantita' dell'addebito, il valore reale e una quota proporzionale del valore salvato, e le sole note del suo
     * pagamento. Blocco, hash, lignaggio e documenti restano quelli dell'originale. Lo storico conserva la riga della catena
     * in una voce sola, perche' tutti i pezzi hanno lo stesso lignaggio.
     * @return {@code false} se non e' stato possibile (nessun movimento toccato)
     */
    private static boolean Dividi(Piano piano, Abbinamento a, int idDocumento) {
        String[] v = a.movimento;
        Map<String, String[]> archivio = piano.archivio;
        int n = a.pagamenti.size();
        BigDecimal totaleUsd = BigDecimal.ZERO;
        for (Riga p : a.pagamenti) totaleUsd = totaleUsd.add(p.importo());
        String lignaggio = MovimentiStorico.AssicuraLignaggio(v);
        String notaBase = NoteSenzaMarcatore(v[21]);
        String istanteCatena = FunzioniDate.ConvertiDatadaLongAlSecondo(Istante(v));

        List<String[]> pezzi = new ArrayList<>();
        BigDecimal salvatoResiduo = a.salvato, realeResiduo = a.reale;
        for (int i = 0; i < n; i++) {
            Riga p = a.pagamenti.get(i);
            boolean ultimo = i == n - 1;
            //L'ultimo pezzo prende il resto: la somma dei pezzi e' esattamente il valore del movimento
            BigDecimal reale = ultimo ? realeResiduo : a.realiPagamenti.get(i).setScale(2, RoundingMode.HALF_UP);
            BigDecimal salvato = a.salvato == null ? null
                    : ultimo ? salvatoResiduo
                    : a.salvato.multiply(p.importo()).divide(totaleUsd, 2, RoundingMode.HALF_UP);
            realeResiduo = realeResiduo.subtract(reale);
            if (salvatoResiduo != null && salvato != null) salvatoResiduo = salvatoResiduo.subtract(salvato);

            String id = IdConIstante(archivio, v, p.istanteMs());
            if (id == null) {
                for (String[] pz : pezzi) archivio.remove(pz[0]);
                return false;
            }
            String[] r = v.clone();
            r[0] = id;
            r[1] = Minuto(p.istanteMs());
            r[29] = String.valueOf(p.istanteMs());
            r[10] = "-" + p.importo().stripTrailingZeros().toPlainString();
            r[15] = reale.toPlainString();
            r[32] = "SI";
            r[40] = FONTE_PREZZO;
            SvuotaCampiMotore(r);
            String nuove = NotePezzo(p, i + 1, n, totaleUsd, salvato, reale, istanteCatena, a.perOrdine);
            r[21] = notaBase.isBlank() ? nuove : notaBase + "<br>" + nuove;
            //Il pezzo entra subito: il successivo cerca il suo ID univoco anche contro di lui
            archivio.put(id, r);
            pezzi.add(r);
        }
        MovimentiStorico.AccodaModifica(lignaggio, pezzi.get(0)[0], v[0], Importazioni.SerializzaRiga(v), OP_DIVISIONE);
        archivio.remove(v[0]);
        if (idDocumento > 0) {
            for (String[] pz : pezzi) DocumentiFonte.CollegaAlGruppo(pz, idDocumento);
        }
        return true;
    }

    /** Le note dell'utente di un movimento, senza le righe scritte da questa importazione. */
    static String NoteSenzaMarcatore(String note) {
        if (note == null || note.isBlank()) return "";
        List<String> tenute = new ArrayList<>();
        for (String riga : note.split("<br>")) {
            if (!riga.startsWith(MARCATORE) && !riga.isBlank()) tenute.add(riga);
        }
        return String.join("<br>", tenute);
    }

    /** Le note di un pezzo: il suo pagamento, la sua quota di valore e da quale movimento della catena viene. */
    static String NotePezzo(Riga p, int indice, int totale, BigDecimal totaleUsd, BigDecimal salvato, BigDecimal reale,
            String istanteCatena, boolean perOrdine) {
        StringBuilder r = new StringBuilder(RigaPagamento(p, null));
        r.append("<br>").append(MARCATORE).append(": parte ").append(indice).append("/").append(totale)
                .append(" del movimento della catena del ").append(istanteCatena).append(" (")
                .append(Importo(totaleUsd)).append(" USDT) diviso per pagamento, con l'orario del pagamento");
        r.append("<br>").append(MARCATORE).append(": ");
        if (salvato == null) {
            r.append("valore reale ").append(Importo(reale)).append(" EUR");
        } else {
            BigDecimal sp = salvato.subtract(reale);
            r.append("valore salvato ").append(Importo(salvato)).append(" EUR (quota del movimento), valore reale ")
                    .append(Importo(reale)).append(" EUR, spread ").append(Segno(sp)).append(" EUR (")
                    .append(Segno(Percentuale(sp, reale))).append("%)");
        }
        if (perOrdine) r.append(", pagamenti scelti per ordine di arrivo");
        return r.toString().replace(";", ",");
    }

    /**
     * Le transazioni della catena i cui movimenti portano gia' i dati di un pagamento ({@code wallet|hash}, minuscolo): dopo una
     * divisione o uno spostamento d'orario il movimento della catena non esiste piu' nella sua forma, e uno scaricamento che
     * lo rileggesse lo ricreerebbe. {@link Trans_MiniPay#Scarica} li salta.
     */
    static Set<String> HashAbbinati(Collection<String[]> archivio) {
        Set<String> hash = new HashSet<>();
        for (String[] v : archivio) {
            if (!isSpesaCarta(v) || v.length <= 24 || Funzioni.noData(v[24]) || v[21] == null || !v[21].contains(MARCATORE)) continue;
            hash.add(ChiaveHash(v));
        }
        return hash;
    }

    /** Il testo da aggiungere alle note: una riga per pagamento e, per un movimento, lo spread. Niente ';' (il file li cancella). */
    static String Note(Abbinamento a) {
        List<String> righe = new ArrayList<>();
        int i = 0;
        for (Riga p : a.pagamenti) {
            i++;
            righe.add(RigaPagamento(p, a.pagamenti.size() > 1 ? " (" + i + "/" + a.pagamenti.size() + ")" : null));
        }
        BigDecimal sp = a.spread();
        righe.add(MARCATORE + ": valore salvato " + Importo(a.salvatoOReale()) + " EUR, valore reale " + Importo(a.reale)
                + " EUR, spread " + Segno(sp) + " EUR (" + Segno(a.spreadPercentuale()) + "%)"
                + (a.perOrdine ? ", pagamenti scelti per ordine di arrivo" : ""));
        return String.join("<br>", righe).replace(";", ",");
    }

    /** La riga di note di un pagamento, che finisce col riferimento (da cui si riconosce che e' gia' abbinato). */
    private static String RigaPagamento(Riga p, String contatore) {
        StringBuilder s = new StringBuilder(MARCATORE);
        if (contatore != null) s.append(contatore);
        s.append(": ").append(p.commerciante().isBlank() ? Spazi(p.descrizione()) : p.commerciante());
        if (!p.citta().isBlank()) s.append(", ").append(p.citta());
        if (!p.paese().isBlank()) s.append(" (").append(p.paese()).append(")");
        if (!p.mcc().isBlank()) s.append(", MCC ").append(p.mcc());
        s.append(", ").append(Data(p)).append(", addebito ").append(Importo(p.importo())).append(" ").append(p.valuta());
        if (p.importoOriginale() != null && !p.valutaOriginale().isBlank()) {
            s.append(", importo originale ").append(Importo(p.importoOriginale())).append(" ").append(p.valutaOriginale());
        }
        if (!"EUR".equalsIgnoreCase(p.valutaOriginale())) s.append(", convertito al cambio BCE del giorno");
        if (p.isPagamentoInAttesa()) s.append(", ").append(ATTESA);
        s.append(", rif. ").append(p.riferimento());
        return s.toString().replace(";", ",");
    }

    //=====================================================================================================
    //=== RIEPILOGO
    //=====================================================================================================

    /**
     * Il riepilogo per l'utente (HTML): tabella dei movimenti con valore salvato, reale e spread, totali e conteggi.
     * @param massimoNonAbbinati quante righe di diagnosi mostrare al massimo
     */
    static String RiepilogoHtml(Piano p, int massimoNonAbbinati) {
        StringBuilder h = new StringBuilder("<div style='width:760px'>");
        if (!p.abbinamenti.isEmpty()) {
            h.append("<table border='1' cellspacing='0' cellpadding='3'><tr><th>Data sulla catena</th><th>Commerciante</th><th>Addebito USDT</th>")
                    .append("<th>Salvato EUR</th><th>Reale EUR</th><th>Spread EUR</th><th>Spread %</th><th>Data e divisione</th></tr>");
            for (Abbinamento a : p.abbinamenti) {
                Riga prima = a.pagamenti.get(0);
                String chi = Html(prima.commerciante().isBlank() ? prima.descrizione() : prima.commerciante())
                        + (a.pagamenti.size() > 1 ? " (+" + (a.pagamenti.size() - 1) + ")" : "")
                        + (a.inAttesa() ? " <i>(in attesa)</i>" : "");
                BigDecimal usd = BigDecimal.ZERO;
                for (Riga r : a.pagamenti) usd = usd.add(r.importo());
                h.append("<tr><td>").append(Html(a.movimento[1])).append("</td><td>").append(chi).append("</td><td align='right'>")
                        .append(Importo(usd)).append("</td><td align='right'>").append(Importo(a.salvatoOReale()))
                        .append("</td><td align='right'>").append(Importo(a.reale)).append("</td><td align='right'>")
                        .append(Segno(a.spread())).append("</td><td align='right'>").append(Segno(a.spreadPercentuale()))
                        .append("</td><td>").append(Html(EsitoTesto(a))).append("</td></tr>");
            }
            h.append("<tr><td colspan='3'><b>Totale</b></td><td align='right'><b>").append(Importo(p.totaleSalvato()))
                    .append("</b></td><td align='right'><b>").append(Importo(p.totaleReale()))
                    .append("</b></td><td align='right'><b>").append(Segno(p.totaleSalvato().subtract(p.totaleReale())))
                    .append("</b></td><td align='right'><b>").append(Segno(p.spreadPercentuale())).append("</b></td><td></td></tr></table><br>");
            h.append("Spread complessivo: <b>").append(Segno(p.totaleSalvato().subtract(p.totaleReale()))).append(" EUR (")
                    .append(Segno(p.spreadPercentuale())).append("%)</b> su ").append(p.abbinamenti.size())
                    .append(" movimenti.<br>");
            BigDecimal[] euro = p.totali(true), altre = p.totali(false);
            if (euro[2].signum() > 0 && altre[2].signum() > 0) {
                h.append("Spese in euro (").append(euro[2]).append("): ").append(Segno(euro[0].subtract(euro[1]))).append(" EUR (")
                        .append(Segno(Percentuale(euro[0].subtract(euro[1]), euro[1]))).append("%). Spese in altra valuta (")
                        .append(altre[2]).append("): ").append(Segno(altre[0].subtract(altre[1]))).append(" EUR (")
                        .append(Segno(Percentuale(altre[0].subtract(altre[1]), altre[1]))).append("%).<br>");
            }
            if (p.movimentiDaDividere() > 0 || p.movimentiDaRiorare() > 0) {
                h.append("Orario dei pagamenti, movimenti da dividere: <b>").append(p.movimentiDaDividere()).append("</b> (in <b>")
                        .append(p.pezziDaCreare()).append("</b> movimenti, uno per pagamento), da spostare all'ora del pagamento: <b>")
                        .append(p.movimentiDaRiorare()).append("</b>. Gli altri restano all'orario della catena, col motivo nella"
                                + " colonna.<br>");
            }
            h.append("<i>Spread = valore salvato sul movimento meno valore reale della spesa: positivo vuol dire che il movimento"
                    + " era valorizzato piu' della spesa. Comprende il margine di cambio della carta, lo scostamento"
                    + " USDT/USD e lo sfasamento orario del prezzo.</i><br>");
        }
        h.append("<br>Preautorizzazioni a importo zero, ignorate: <b>").append(p.preautorizzazioniIgnorate)
                .append("</b>. Gia' abbinati in passato: <b>").append(p.pagamentiGiaAbbinati).append("</b>.<br>");
        int inAttesaUsciti = countPagamentiInAttesa(p);
        if (inAttesaUsciti > 0 || p.pagamentiInAttesa > 0 || !p.conferme.isEmpty() || p.giaAbbinatiInAttesa > 0) {
            h.append("Pagamenti ancora in attesa nel file: <b>").append(inAttesaUsciti).append("</b> gia' usciti dalla Safe (i movimenti"
                    + " vengono abbinati e segnati «in attesa di conferma»), <b>")
                    .append(p.pagamentiInAttesa - inAttesaUsciti).append("</b> non ancora usciti, <b>")
                    .append(p.giaAbbinatiInAttesa).append("</b> abbinati in passato e ancora in attesa. Confermati dal file: <b>")
                    .append(p.conferme.size()).append("</b>.<br>");
        }
        if (!p.nonRiorari.isEmpty()) {
            h.append("<br><b>Abbinati in passato, restano all'orario della catena (").append(p.nonRiorari.size()).append("):</b><br>");
            for (String r : p.nonRiorari) h.append("&bull; ").append(Html(r)).append("<br>");
        }
        if (!p.nonAbbinati.isEmpty()) {
            h.append("<br><b>Non abbinati (").append(p.nonAbbinati.size()).append("), non vengono toccati:</b><br>");
            int n = 0;
            for (String s : p.nonAbbinati) {
                if (n++ >= massimoNonAbbinati) {
                    h.append("... e altri ").append(p.nonAbbinati.size() - massimoNonAbbinati).append(".<br>");
                    break;
                }
                h.append("&bull; ").append(Html(s)).append("<br>");
            }
        }
        return h.append("</div>").toString();
    }

    /** Cosa succede al movimento, per la colonna del riepilogo. */
    private static String EsitoTesto(Abbinamento a) {
        return switch (a.esecuzione) {
            case DIVISIONE -> "diviso in " + a.pagamenti.size() + " movimenti, orario dei pagamenti"
                    + (a.giaScritto ? " (gia' abbinato)" : "");
            case ORARIO -> "orario del pagamento " + Data(a.pagamenti.get(0)) + (a.giaScritto ? " (gia' abbinato)" : "");
            case SOLO_VALORE -> a.motivo.isBlank() ? "orario invariato" : "orario della catena: " + a.motivo;
        };
    }

    /** Quanti dei pagamenti in attesa del piano sono entrati in un abbinamento. */
    private static int countPagamentiInAttesa(Piano p) {
        int n = 0;
        for (Abbinamento a : p.abbinamenti) {
            for (Riga r : a.pagamenti) if (r.isPagamentoInAttesa()) n++;
        }
        return n;
    }

    static String Html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    /** Gli UUID dei pagamenti gia' scritti nelle note di qualche movimento (minuscoli). */
    static Set<String> RiferimentiGiaScritti(Collection<String[]> movimenti) {
        Set<String> uuid = new HashSet<>();
        for (String[] v : movimenti) {
            if (v == null || v.length <= 21 || v[21] == null || !v[21].contains(MARCATORE)) continue;
            Matcher m = RIFERIMENTO.matcher(v[21]);
            while (m.find()) uuid.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return uuid;
    }

    /** Un movimento di spesa con la carta: pagamento (uscita) con la causale scritta da {@link Trans_MiniPay}. */
    static boolean isSpesaCarta(String[] v) {
        if (v == null || v.length <= 21 || v[0] == null || v[3] == null || v[7] == null) return false;
        if (!Trans_MiniPay.CAUSALE_CARTA.equals(v[7])) return false;
        String[] parti = v[0].split("_");
        return "PC".equalsIgnoreCase(parti[parti.length - 1]);
    }

    private static BigDecimal Quantita(String[] v) {
        BigDecimal q = Numero(v[10]);
        return q == null ? null : q.abs();
    }

    /**
     * L'istante del movimento al secondo. Il {@code [1]} e' al minuto: i secondi stanno solo nel prefisso a 14 cifre dell'ID
     * ({@code yyyyMMddHHmmss_...}), da cui lo leggono anche Auto-Invest e Dual Investment. Senza prefisso valido si ripiega sul
     * minuto, 0 se neanche quello si legge.
     */
    static long Istante(String[] v) {
        if (v[0] != null && v[0].length() >= 14 && v[0].substring(0, 14).chars().allMatch(Character::isDigit)) {
            long ms = FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
            if (ms != 0) return ms;
        }
        return v[1] == null || v[1].length() < 16 ? 0 : FunzioniDate.ConvertiDatainLongMinuto(v[1].substring(0, 16));
    }

    private static String Descrivi(String[] v) {
        BigDecimal q = Quantita(v);
        return "Movimento " + v[1] + " " + (q == null ? v[10] : q.toPlainString()) + " " + v[8];
    }

    private static String Data(Riga p) {
        return FunzioniDate.ConvertiDatadaLongAlSecondo(p.istanteMs());
    }

    static BigDecimal Numero(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String Spazi(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    static BigDecimal Percentuale(BigDecimal differenza, BigDecimal base) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        return differenza.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }

    /** Importo con due decimali e la virgola, come nei testi per l'utente. */
    static String Importo(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Come {@link #Importo} ma con il segno sempre visibile. */
    static String Segno(BigDecimal v) {
        String s = Importo(v);
        return v.signum() > 0 ? "+" + s : s;
    }
}
