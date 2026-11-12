package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
 * <p><b>Stato dei pagamenti.</b> Solo i {@code settled} (o {@code approved}) contano: un {@code pending} può cambiare importo (la
 * preautorizzazione PayPal è a zero) e non è ancora sulla catena. Una riga già abbinata si riconosce dal suo UUID
 * ({@code Reference}) scritto nelle note, quindi rileggere lo stesso file, o uno più recente che lo contiene, non
 * duplica niente.</p>
 *
 * <p>Nessuna Swing qui: la finestra sta in {@code Importazioni_Gestione}.</p>
 */
public class MiniPay_SpeseCarta {

    /** Inizio di ogni riga di note scritta qui: da lì si riconoscono i movimenti già abbinati. */
    static final String MARCATORE = "Carta MiniPay";
    static final String FONTE_PREZZO = "|||Personalizzato";
    /** Un movimento può chiudersi dopo il pagamento, mai prima (tolleranza per gli orologi). */
    static final long TOLLERANZA_DOPO_MS = 5 * 60_000L;
    /** Quanto prima del movimento può essere stato fatto il pagamento. */
    static final long FINESTRA_PRIMA_MS = 14L * 24 * 3600_000L;
    private static final String INTESTAZIONE = "date,type,status,description,merchant";
    private static final Pattern RIFERIMENTO = Pattern.compile("rif\\. ([0-9a-fA-F-]{36})");
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

        boolean isPagamentoInAttesa() {
            return "payment".equalsIgnoreCase(tipo) && "pending".equalsIgnoreCase(stato);
        }
    }

    /** Un movimento della catena con i pagamenti che lo spiegano e i valori a confronto. */
    static final class Abbinamento {
        final String[] movimento;
        final List<Riga> pagamenti;
        final BigDecimal salvato;
        final BigDecimal reale;
        /** Scelto fra più combinazioni identiche per ordine di arrivo. */
        final boolean perOrdine;

        Abbinamento(String[] movimento, List<Riga> pagamenti, BigDecimal salvato, BigDecimal reale, boolean perOrdine) {
            this.movimento = movimento;
            this.pagamenti = pagamenti;
            this.salvato = salvato;
            this.reale = reale;
            this.perOrdine = perOrdine;
        }

        /** Valore salvato meno valore reale: positivo se il movimento era valorizzato più della spesa. */
        BigDecimal spread() {
            return salvato.subtract(reale);
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
        int righeLette = 0;
        int pagamentiApprovati = 0;
        int pagamentiInAttesa = 0;
        int altreRighe = 0;
        int pagamentiGiaAbbinati = 0;
        final List<Abbinamento> abbinamenti = new ArrayList<>();
        /** Movimenti già abbinati in precedenza (hanno il marcatore nelle note). */
        int movimentiGiaAbbinati = 0;
        /** Una riga di diagnosi per ogni pagamento o movimento che non si è abbinato. */
        final List<String> nonAbbinati = new ArrayList<>();

        BigDecimal totaleSalvato() {
            BigDecimal t = BigDecimal.ZERO;
            for (Abbinamento a : abbinamenti) t = t.add(a.salvato);
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
                salvato = salvato.add(a.salvato);
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
        piano.righeLette = righe.size();
        Set<String> giaScritti = RiferimentiGiaScritti(archivio.values());

        List<Riga> pagamenti = new ArrayList<>();
        for (Riga r : righe) {
            if (r.isPagamentoApprovato()) {
                if (giaScritti.contains(r.riferimento().toLowerCase(Locale.ROOT))) {
                    piano.pagamentiGiaAbbinati++;
                    continue;
                }
                piano.pagamentiApprovati++;
                pagamenti.add(r);
            } else if (r.isPagamentoInAttesa()) {
                piano.pagamentiInAttesa++;
            } else {
                piano.altreRighe++;
            }
        }
        pagamenti.sort((a, b) -> Long.compare(a.istanteMs(), b.istanteMs()));

        List<String[]> spese = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (!isSpesaCarta(v) || !nomiWallet.contains(v[3].trim().toLowerCase(Locale.ROOT))) continue;
            if (v[21] != null && v[21].contains(MARCATORE)) {
                piano.movimentiGiaAbbinati++;
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
                piano.nonAbbinati.add(Descrivi(v) + ": nessun pagamento approvato del file ha questo importo"
                        + " (puo' essere ancora in attesa)");
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
            for (Riga p : scelta) {
                BigDecimal e = ValoreEuro(p, cambioUsdEur);
                if (e == null) {
                    cambioMancante = true;
                    break;
                }
                reale = reale.add(e);
            }
            if (cambioMancante) {
                piano.nonAbbinati.add(Descrivi(v) + ": cambio USD/EUR non disponibile per un pagamento");
                continue;
            }
            usati.addAll(scelta);
            BigDecimal salvato = Numero(v[15]);
            piano.abbinamenti.add(new Abbinamento(v, scelta, (salvato == null ? BigDecimal.ZERO : salvato).setScale(2, RoundingMode.HALF_UP),
                    reale.setScale(2, RoundingMode.HALF_UP), perOrdine));
        }
        for (Riga p : pagamenti) {
            if (!usati.contains(p)) {
                piano.nonAbbinati.add("Pagamento " + Data(p) + " " + p.commerciante() + " " + p.importo().toPlainString()
                        + " " + p.valuta() + ": nessun movimento della catena corrispondente (non ancora scaricato o non"
                        + " ancora regolato)");
            }
        }
        return piano;
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
     * Applica il piano: valore e note di ogni movimento (con lo storico delle modifiche) e collegamento del documento a
     * tutto il gruppo del movimento.
     * @param idDocumento il documento di supporto registrato, {@code 0} se la registrazione non e' riuscita
     * @return quanti movimenti sono stati modificati
     */
    static int Applica(Piano piano, int idDocumento) {
        int n = 0;
        for (Abbinamento a : piano.abbinamenti) {
            String[] v = a.movimento;
            String note = v[21] == null ? "" : v[21];
            String nuove = Note(a);
            GUI_ModificaMovimento.ScriviSoloPrezzoENote(v, a.reale.toPlainString(),
                    note.isBlank() ? nuove : note + "<br>" + nuove, true, FONTE_PREZZO);
            if (idDocumento > 0) DocumentiFonte.CollegaAlGruppo(v, idDocumento);
            n++;
        }
        return n;
    }

    /** Il testo da aggiungere alle note: una riga per pagamento e, per un movimento, lo spread. Niente ';' (il file li cancella). */
    static String Note(Abbinamento a) {
        List<String> righe = new ArrayList<>();
        int i = 0;
        for (Riga p : a.pagamenti) {
            i++;
            StringBuilder s = new StringBuilder(MARCATORE);
            if (a.pagamenti.size() > 1) s.append(" (").append(i).append("/").append(a.pagamenti.size()).append(")");
            s.append(": ").append(p.commerciante().isBlank() ? Spazi(p.descrizione()) : p.commerciante());
            if (!p.citta().isBlank()) s.append(", ").append(p.citta());
            if (!p.paese().isBlank()) s.append(" (").append(p.paese()).append(")");
            if (!p.mcc().isBlank()) s.append(", MCC ").append(p.mcc());
            s.append(", ").append(Data(p)).append(", addebito ").append(Importo(p.importo())).append(" ").append(p.valuta());
            if (p.importoOriginale() != null && !p.valutaOriginale().isBlank()) {
                s.append(", importo originale ").append(Importo(p.importoOriginale())).append(" ").append(p.valutaOriginale());
            }
            if (!"EUR".equalsIgnoreCase(p.valutaOriginale())) s.append(", convertito al cambio BCE del giorno");
            s.append(", rif. ").append(p.riferimento());
            righe.add(s.toString());
        }
        BigDecimal sp = a.spread();
        righe.add(MARCATORE + ": valore salvato " + Importo(a.salvato) + " EUR, valore reale " + Importo(a.reale)
                + " EUR, spread " + Segno(sp) + " EUR (" + Segno(a.spreadPercentuale()) + "%)"
                + (a.perOrdine ? ", pagamenti scelti per ordine di arrivo" : ""));
        return String.join("<br>", righe).replace(";", ",");
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
            h.append("<table border='1' cellspacing='0' cellpadding='3'><tr><th>Data</th><th>Commerciante</th><th>Addebito USDT</th>")
                    .append("<th>Salvato EUR</th><th>Reale EUR</th><th>Spread EUR</th><th>Spread %</th></tr>");
            for (Abbinamento a : p.abbinamenti) {
                Riga prima = a.pagamenti.get(0);
                String chi = Html(prima.commerciante().isBlank() ? prima.descrizione() : prima.commerciante())
                        + (a.pagamenti.size() > 1 ? " (+" + (a.pagamenti.size() - 1) + ")" : "");
                BigDecimal usd = BigDecimal.ZERO;
                for (Riga r : a.pagamenti) usd = usd.add(r.importo());
                h.append("<tr><td>").append(Html(a.movimento[1])).append("</td><td>").append(chi).append("</td><td align='right'>")
                        .append(Importo(usd)).append("</td><td align='right'>").append(Importo(a.salvato))
                        .append("</td><td align='right'>").append(Importo(a.reale)).append("</td><td align='right'>")
                        .append(Segno(a.spread())).append("</td><td align='right'>").append(Segno(a.spreadPercentuale()))
                        .append("</td></tr>");
            }
            h.append("<tr><td colspan='3'><b>Totale</b></td><td align='right'><b>").append(Importo(p.totaleSalvato()))
                    .append("</b></td><td align='right'><b>").append(Importo(p.totaleReale()))
                    .append("</b></td><td align='right'><b>").append(Segno(p.totaleSalvato().subtract(p.totaleReale())))
                    .append("</b></td><td align='right'><b>").append(Segno(p.spreadPercentuale())).append("</b></td></tr></table><br>");
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
            h.append("<i>Spread = valore salvato sul movimento meno valore reale della spesa: positivo vuol dire che il movimento"
                    + " era valorizzato piu' della spesa. Comprende il margine di cambio della carta, lo scostamento"
                    + " USDT/USD e lo sfasamento orario del prezzo.</i><br>");
        }
        h.append("<br>Pagamenti in attesa, ignorati: <b>").append(p.pagamentiInAttesa).append("</b>. Gia' abbinati in passato: <b>")
                .append(p.pagamentiGiaAbbinati).append("</b>.<br>");
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
