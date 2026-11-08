package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
 * Il CSV delle transazioni della carta ether.fi Cash ("transaction history" dell'app) come documento di supporto dei
 * movimenti {@code SPESA CON CARTA} già scaricati dalla catena ({@link Trans_EtherFi}). Stessa idea di
 * {@link MiniPay_SpeseCarta}: il valore in euro del movimento, calcolato al prezzo di mercato del token speso, si sostituisce
 * con quello che la carta ha davvero addebitato, e nelle note finiscono il dettaglio del pagamento e lo spread (valore salvato
 * meno valore reale).
 *
 * <p><b>Come si abbina, e perché non come MiniPay.</b> Le righe del file non hanno nessun identificativo della transazione, e
 * l'importo non coincide con la quantità del movimento: il token speso è weEUR, USDC o simili (weEUR vale più di un euro) e
 * l'evento {@code Spend} porta solo il suo valore in dollari, che {@link Trans_EtherFi} scrive nella causale
 * ({@code (USD 39.176904)}). Si abbina quindi per <b>vicinanza di tempo</b> (l'evento e' emesso al momento del pagamento) e
 * <b>coerenza dell'importo</b> (il valore in dollari convertito in euro deve stare entro il 5% dall'addebito). Mai uno senza
 * l'altro, e nell'anteprima si vede lo scarto di tempo di ogni coppia. Se due pagamenti restano ugualmente plausibili non si
 * sceglie.</p>
 *
 * <p>Solo le spese {@code CLEARED} contano: una {@code PENDING} puo' cambiare importo (mancia, conguaglio) e una
 * {@code CANCELLED} non e' mai avvenuta. Le altre righe (prelievi, swap, bridge, depositi) non sono spese e si ignorano.
 * Una riga gia' abbinata si riconosce dal suo riferimento ({@code rif. EF-<secondi>-<importo>}) scritto nelle note.</p>
 *
 * <p>Nessuna Swing qui: la finestra sta in {@code Importazioni_Gestione}.</p>
 */
public class EtherFi_SpeseCarta {

    /** Inizio di ogni riga di note scritta qui. */
    static final String MARCATORE = "Carta ether.fi";
    static final String FONTE_PREZZO = "|||Personalizzato";
    /** Quanto distano al massimo l'evento sulla catena e l'orario del pagamento nel file. */
    static final long FINESTRA_MS = 30 * 60_000L;
    /** Due candidati con scarti di tempo piu' vicini di cosi' sono ugualmente plausibili. */
    static final long DISTINZIONE_MS = 60_000L;
    /** Scarto massimo, in rapporto all'addebito, fra il valore in dollari dell'evento (in euro) e l'addebito. */
    static final BigDecimal TOLLERANZA_IMPORTO = new BigDecimal("0.05");
    private static final String INTESTAZIONE = "timestamp,type,description,status,amount,currency,card";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");
    private static final Pattern RIFERIMENTO = Pattern.compile("rif\\. (EF-\\d+-[0-9.]+)");
    private static final Pattern USD_NELLA_CAUSALE = Pattern.compile("\\(USD ([0-9.]+)\\)");

    /** Una riga del CSV. Il nome del titolare e la carta non si leggono: non servono e sono dati personali. */
    record Riga(long istanteMs, String tipo, String descrizione, String stato, BigDecimal importo, String valuta,
            BigDecimal importoOriginale, String valutaOriginale, BigDecimal cashback, String valutaCashback,
            String categoria, String modalita) {

        boolean isSpesaConclusa() {
            return "card_spend".equalsIgnoreCase(tipo) && "cleared".equalsIgnoreCase(stato) && importo != null && importo.signum() > 0;
        }

        boolean isSpesaInAttesa() {
            return "card_spend".equalsIgnoreCase(tipo) && "pending".equalsIgnoreCase(stato);
        }

        boolean isSpesaAnnullata() {
            return "card_spend".equalsIgnoreCase(tipo) && ("cancelled".equalsIgnoreCase(stato) || "declined".equalsIgnoreCase(stato)
                    || "reversed".equalsIgnoreCase(stato));
        }

        /** Il riferimento stabile della riga, scritto nelle note: il file non ha identificativi. */
        String riferimento() {
            return "EF-" + (istanteMs / 1000) + "-" + (importo == null ? "0" : importo.stripTrailingZeros().toPlainString());
        }
    }

    /** Un movimento della catena con il pagamento che lo spiega. */
    static final class Abbinamento {
        final String[] movimento;
        final Riga pagamento;
        final BigDecimal salvato;
        final BigDecimal reale;
        /** Scarto di tempo fra evento e pagamento, in millisecondi, con segno (positivo: evento dopo il pagamento). */
        final long scartoMs;

        Abbinamento(String[] movimento, Riga pagamento, BigDecimal salvato, BigDecimal reale, long scartoMs) {
            this.movimento = movimento;
            this.pagamento = pagamento;
            this.salvato = salvato;
            this.reale = reale;
            this.scartoMs = scartoMs;
        }

        BigDecimal spread() {
            return salvato.subtract(reale);
        }

        BigDecimal spreadPercentuale() {
            return MiniPay_SpeseCarta.Percentuale(spread(), reale);
        }
    }

    /** Il risultato dell'analisi, prima di scrivere qualsiasi cosa. */
    static final class Piano {
        int righeLette = 0;
        int inAttesa = 0;
        int annullate = 0;
        int altreRighe = 0;
        int pagamentiGiaAbbinati = 0;
        int movimentiGiaAbbinati = 0;
        final List<Abbinamento> abbinamenti = new ArrayList<>();
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

        /** Media ponderata: somma delle differenze su somma dei valori reali. */
        BigDecimal spreadPercentuale() {
            return MiniPay_SpeseCarta.Percentuale(totaleSalvato().subtract(totaleReale()), totaleReale());
        }

        BigDecimal totaleCashback() {
            BigDecimal t = BigDecimal.ZERO;
            for (Abbinamento a : abbinamenti) {
                if (a.pagamento.cashback() != null && "EUR".equalsIgnoreCase(a.pagamento.valutaCashback())) t = t.add(a.pagamento.cashback());
            }
            return t;
        }
    }

    //=====================================================================================================
    //=== LETTURA DEL FILE
    //=====================================================================================================

    /**
     * Legge il CSV.
     * @throws IllegalArgumentException se l'intestazione non è quella della cronologia della carta ether.fi
     */
    static List<Riga> Leggi(String contenuto) {
        List<Riga> righe = new ArrayList<>();
        boolean intestazioneVista = false;
        for (String r : contenuto.replace("\r", "").split("\n")) {
            String riga = r.replace("﻿", "").trim();
            if (riga.isEmpty()) continue;
            if (!intestazioneVista) {
                if (!riga.toLowerCase(Locale.ROOT).startsWith(INTESTAZIONE)) {
                    throw new IllegalArgumentException("L'intestazione non e' quella della cronologia della carta ether.fi.");
                }
                intestazioneVista = true;
                continue;
            }
            String[] c = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");
            if (c.length < 14) continue;
            long istante;
            try {
                istante = LocalDateTime.parse(c[0].trim(), FORMATO_DATA).toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (RuntimeException e) {
                continue;
            }
            righe.add(new Riga(istante, c[1].trim(), MiniPay_SpeseCarta.Spazi(c[2]), c[3].trim(), MiniPay_SpeseCarta.Numero(c[4]),
                    c[5].trim(), MiniPay_SpeseCarta.Numero(c[8]), c[9].trim(), MiniPay_SpeseCarta.Numero(c[10]), c[11].trim(),
                    MiniPay_SpeseCarta.Spazi(c[12]), c[13].trim()));
        }
        if (!intestazioneVista) throw new IllegalArgumentException("Il file e' vuoto.");
        return righe;
    }

    /**
     * Le Safe ether.fi registrate in Gestione Wallet.
     * @param righeWallet le righe della tabella dei wallet ({@code indirizzo;rete;...})
     * @param safeRiconosciute gli indirizzi riconosciuti come conto ether.fi Cash ({@link Trans_EtherFi#SafeRegistrati})
     * @return gli indirizzi come sono salvati, nell'ordine della tabella
     */
    static List<String> WalletCompatibili(Collection<String> righeWallet, Set<String> safeRiconosciute) {
        List<String> wallet = new ArrayList<>();
        for (String riga : righeWallet) {
            String[] p = riga.split(";");
            if (p.length < 2 || !Trans_EtherFi.RETE.equalsIgnoreCase(p[1].trim())) continue;
            String a = p[0].trim();
            if (safeRiconosciute.contains(a.toLowerCase(Locale.ROOT))) wallet.add(a);
        }
        return wallet;
    }

    /** Il momento dell'ultima spesa conclusa del file, 0 se non ce ne sono. */
    static long UltimaSpesaConclusaMs(List<Riga> righe) {
        long massimo = 0;
        for (Riga r : righe) {
            if (r.isSpesaConclusa()) massimo = Math.max(massimo, r.istanteMs());
        }
        return massimo;
    }

    /** Il momento del movimento piu' recente del wallet, 0 se non ce ne sono. */
    static long UltimoMovimentoMs(Map<String, String[]> archivio, String wallet) {
        String nome = NomeWallet(wallet);
        long massimo = 0;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 3 || v[3] == null || !nome.equalsIgnoreCase(v[3].trim())) continue;
            massimo = Math.max(massimo, MiniPay_SpeseCarta.Istante(v));
        }
        return massimo;
    }

    static String NomeWallet(String wallet) {
        return wallet.trim() + " (" + Trans_EtherFi.RETE + ")";
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================

    /**
     * Cerca per ogni spesa della catena il pagamento del file che la spiega. Non modifica niente.
     *
     * @param righe il file letto con {@link #Leggi}
     * @param archivio i movimenti ({@code Principale.MappaCryptoWallet})
     * @param wallet l'indirizzo della Safe
     * @param cambioUsdEur converte un importo in USD in euro per un giorno ({@code yyyy-MM-dd}), {@code null} se manca
     */
    static Piano Pianifica(List<Riga> righe, Map<String, String[]> archivio, String wallet,
            BiFunction<String, String, String> cambioUsdEur) {
        Piano piano = new Piano();
        piano.righeLette = righe.size();
        Set<String> giaScritti = RiferimentiGiaScritti(archivio.values());

        List<Riga> pagamenti = new ArrayList<>();
        for (Riga r : righe) {
            if (r.isSpesaConclusa()) {
                if (giaScritti.contains(r.riferimento())) piano.pagamentiGiaAbbinati++;
                else pagamenti.add(r);
            } else if (r.isSpesaInAttesa()) {
                piano.inAttesa++;
            } else if (r.isSpesaAnnullata()) {
                piano.annullate++;
            } else {
                piano.altreRighe++;
            }
        }

        String nome = NomeWallet(wallet);
        List<String[]> spese = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (!isSpesaCarta(v) || !nome.equalsIgnoreCase(v[3].trim())) continue;
            if (v[21] != null && v[21].contains(MARCATORE)) {
                piano.movimentiGiaAbbinati++;
                continue;
            }
            spese.add(v);
        }
        spese.sort((a, b) -> Long.compare(MiniPay_SpeseCarta.Istante(a), MiniPay_SpeseCarta.Istante(b)));

        Set<Riga> usati = new HashSet<>();
        for (String[] v : spese) {
            long ms = MiniPay_SpeseCarta.Istante(v);
            if (ms == 0) {
                piano.nonAbbinati.add(Descrivi(v) + ": data non leggibile");
                continue;
            }
            BigDecimal usd = DollariDellaCausale(v[7]);
            List<Riga> plausibili = new ArrayList<>();
            for (Riga p : pagamenti) {
                if (usati.contains(p) || Math.abs(p.istanteMs() - ms) > FINESTRA_MS) continue;
                if (ImportoCoerente(p, usd, ms, cambioUsdEur)) plausibili.add(p);
            }
            if (plausibili.isEmpty()) {
                piano.nonAbbinati.add(Descrivi(v) + ": nessuna spesa conclusa del file entro mezz'ora con un importo coerente"
                        + " (puo' essere ancora in attesa)");
                continue;
            }
            plausibili.sort((a, b) -> Long.compare(Math.abs(a.istanteMs() - ms), Math.abs(b.istanteMs() - ms)));
            Riga scelta = plausibili.get(0);
            if (plausibili.size() > 1) {
                Riga seconda = plausibili.get(1);
                boolean vicini = Math.abs(seconda.istanteMs() - ms) - Math.abs(scelta.istanteMs() - ms) < DISTINZIONE_MS;
                boolean stessoImporto = scelta.importo().compareTo(seconda.importo()) == 0;
                if (vicini && !stessoImporto) {
                    piano.nonAbbinati.add(Descrivi(v) + ": due pagamenti ugualmente plausibili, non scelgo");
                    continue;
                }
            }
            BigDecimal reale = ValoreEuro(scelta, cambioUsdEur);
            if (reale == null) {
                piano.nonAbbinati.add(Descrivi(v) + ": valuta del pagamento non gestita o cambio non disponibile");
                continue;
            }
            usati.add(scelta);
            BigDecimal salvato = MiniPay_SpeseCarta.Numero(v[15]);
            piano.abbinamenti.add(new Abbinamento(v, scelta,
                    (salvato == null ? BigDecimal.ZERO : salvato).setScale(2, RoundingMode.HALF_UP),
                    reale.setScale(2, RoundingMode.HALF_UP), ms - scelta.istanteMs()));
        }
        for (Riga p : pagamenti) {
            if (!usati.contains(p)) {
                piano.nonAbbinati.add("Pagamento " + Data(p) + " " + p.descrizione() + " " + MiniPay_SpeseCarta.Importo(p.importo())
                        + " " + p.valuta() + ": nessun movimento della catena corrispondente (non ancora scaricato)");
            }
        }
        return piano;
    }

    /**
     * Il valore in dollari dell'evento, convertito in euro, deve stare entro la tolleranza dall'addebito. Senza il valore in
     * dollari (causale diversa) si accetta solo uno scarto di tempo di due minuti.
     */
    private static boolean ImportoCoerente(Riga p, BigDecimal usd, long ms, BiFunction<String, String, String> cambioUsdEur) {
        if (usd == null) return Math.abs(p.istanteMs() - ms) <= 2 * 60_000L;
        BigDecimal addebito = ValoreEuro(p, cambioUsdEur);
        if (addebito == null || addebito.signum() == 0) return false;
        String euro = cambioUsdEur.apply(usd.toPlainString(), FunzioniDate.ConvertiDatadaLong(ms));
        BigDecimal evento = MiniPay_SpeseCarta.Numero(euro);
        if (evento == null) return false;
        return evento.subtract(addebito).abs().divide(addebito, 6, RoundingMode.HALF_UP).compareTo(TOLLERANZA_IMPORTO) <= 0;
    }

    /** L'addebito in euro: com'e' se la carta addebita in euro, al cambio BCE del giorno se in dollari, {@code null} altrimenti. */
    static BigDecimal ValoreEuro(Riga p, BiFunction<String, String, String> cambioUsdEur) {
        if ("EUR".equalsIgnoreCase(p.valuta())) return p.importo();
        if ("USD".equalsIgnoreCase(p.valuta())) {
            return MiniPay_SpeseCarta.Numero(cambioUsdEur.apply(p.importo().toPlainString(), FunzioniDate.ConvertiDatadaLong(p.istanteMs())));
        }
        return null;
    }

    /** I dollari scritti da {@link Trans_EtherFi} nella causale di una spesa, {@code null} se non ci sono. */
    static BigDecimal DollariDellaCausale(String causale) {
        if (causale == null) return null;
        Matcher m = USD_NELLA_CAUSALE.matcher(causale);
        return m.find() ? MiniPay_SpeseCarta.Numero(m.group(1)) : null;
    }

    //=====================================================================================================
    //=== SCRITTURA
    //=====================================================================================================

    /**
     * Applica il piano: valore e note di ogni movimento (con lo storico delle modifiche) e collegamento del documento a tutto
     * il gruppo del movimento.
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

    /** Il testo da aggiungere alle note. Niente ';' (il file dei movimenti li cancella). */
    static String Note(Abbinamento a) {
        Riga p = a.pagamento;
        StringBuilder s = new StringBuilder(MARCATORE).append(": ").append(p.descrizione());
        if (!p.categoria().isBlank()) s.append(", ").append(p.categoria());
        s.append(", ").append(Data(p)).append(", addebito ").append(MiniPay_SpeseCarta.Importo(p.importo())).append(" ").append(p.valuta());
        if (!"EUR".equalsIgnoreCase(p.valuta())) s.append(" (convertito al cambio BCE del giorno)");
        if (p.cashback() != null && p.cashback().signum() > 0) {
            s.append(", cashback maturato ").append(MiniPay_SpeseCarta.Importo(p.cashback())).append(" ").append(p.valutaCashback());
        }
        if (!p.modalita().isBlank()) s.append(", ").append(p.modalita());
        s.append(", rif. ").append(p.riferimento());
        s.append("<br>").append(MARCATORE).append(": valore salvato ").append(MiniPay_SpeseCarta.Importo(a.salvato))
                .append(" EUR, valore reale ").append(MiniPay_SpeseCarta.Importo(a.reale)).append(" EUR, spread ")
                .append(MiniPay_SpeseCarta.Segno(a.spread())).append(" EUR (").append(MiniPay_SpeseCarta.Segno(a.spreadPercentuale()))
                .append("%)");
        return s.toString().replace(";", ",");
    }

    //=====================================================================================================
    //=== RIEPILOGO
    //=====================================================================================================

    /**
     * Il riepilogo per l'utente (HTML): tabella con valore salvato, reale, spread e scarto di tempo, totali e conteggi.
     * @param massimoNonAbbinati quante righe di diagnosi mostrare al massimo
     */
    static String RiepilogoHtml(Piano p, int massimoNonAbbinati) {
        StringBuilder h = new StringBuilder("<div style='width:760px'>");
        if (!p.abbinamenti.isEmpty()) {
            h.append("<table border='1' cellspacing='0' cellpadding='3'><tr><th>Data</th><th>Commerciante</th><th>Addebito</th>")
                    .append("<th>Salvato EUR</th><th>Reale EUR</th><th>Spread EUR</th><th>Spread %</th><th>Scarto s</th></tr>");
            for (Abbinamento a : p.abbinamenti) {
                h.append("<tr><td>").append(MiniPay_SpeseCarta.Html(a.movimento[1])).append("</td><td>")
                        .append(MiniPay_SpeseCarta.Html(a.pagamento.descrizione())).append("</td><td align='right'>")
                        .append(MiniPay_SpeseCarta.Importo(a.pagamento.importo())).append("</td><td align='right'>")
                        .append(MiniPay_SpeseCarta.Importo(a.salvato)).append("</td><td align='right'>")
                        .append(MiniPay_SpeseCarta.Importo(a.reale)).append("</td><td align='right'>")
                        .append(MiniPay_SpeseCarta.Segno(a.spread())).append("</td><td align='right'>")
                        .append(MiniPay_SpeseCarta.Segno(a.spreadPercentuale())).append("</td><td align='right'>")
                        .append(a.scartoMs / 1000).append("</td></tr>");
            }
            BigDecimal diff = p.totaleSalvato().subtract(p.totaleReale());
            h.append("<tr><td colspan='3'><b>Totale</b></td><td align='right'><b>").append(MiniPay_SpeseCarta.Importo(p.totaleSalvato()))
                    .append("</b></td><td align='right'><b>").append(MiniPay_SpeseCarta.Importo(p.totaleReale()))
                    .append("</b></td><td align='right'><b>").append(MiniPay_SpeseCarta.Segno(diff))
                    .append("</b></td><td align='right'><b>").append(MiniPay_SpeseCarta.Segno(p.spreadPercentuale()))
                    .append("</b></td><td></td></tr></table><br>");
            h.append("Spread complessivo: <b>").append(MiniPay_SpeseCarta.Segno(diff)).append(" EUR (")
                    .append(MiniPay_SpeseCarta.Segno(p.spreadPercentuale())).append("%)</b> su ").append(p.abbinamenti.size())
                    .append(" movimenti. Cashback maturato nelle stesse spese: ")
                    .append(MiniPay_SpeseCarta.Importo(p.totaleCashback())).append(" EUR (non entra nel valore).<br>");
            h.append("<i>Spread = valore salvato sul movimento meno valore reale della spesa: positivo vuol dire che il movimento"
                    + " era valorizzato piu' della spesa. Comprende il cambio dollaro/euro dell'evento, il prezzo del token e"
                    + " lo sfasamento orario. Lo scarto di tempo e' evento meno pagamento.</i><br>");
        }
        h.append("<br>Spese in attesa, ignorate: <b>").append(p.inAttesa).append("</b>. Annullate: <b>").append(p.annullate)
                .append("</b>. Gia' abbinate in passato: <b>").append(p.pagamentiGiaAbbinati)
                .append("</b>. Altre righe (prelievi, swap, bridge...): <b>").append(p.altreRighe).append("</b>.<br>");
        if (!p.nonAbbinati.isEmpty()) {
            h.append("<br><b>Non abbinati (").append(p.nonAbbinati.size()).append("), non vengono toccati:</b><br>");
            int n = 0;
            for (String s : p.nonAbbinati) {
                if (n++ >= massimoNonAbbinati) {
                    h.append("... e altri ").append(p.nonAbbinati.size() - massimoNonAbbinati).append(".<br>");
                    break;
                }
                h.append("&bull; ").append(MiniPay_SpeseCarta.Html(s)).append("<br>");
            }
        }
        return h.append("</div>").toString();
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    /** I riferimenti dei pagamenti gia' scritti nelle note di qualche movimento. */
    static Set<String> RiferimentiGiaScritti(Collection<String[]> movimenti) {
        Set<String> rif = new HashSet<>();
        for (String[] v : movimenti) {
            if (v == null || v.length <= 21 || v[21] == null || !v[21].contains(MARCATORE)) continue;
            Matcher m = RIFERIMENTO.matcher(v[21]);
            while (m.find()) rif.add(m.group(1));
        }
        return rif;
    }

    /** Un movimento di spesa con la carta: pagamento (uscita) con la causale scritta da {@link Trans_EtherFi}. */
    static boolean isSpesaCarta(String[] v) {
        if (v == null || v.length <= 21 || v[0] == null || v[3] == null || v[7] == null) return false;
        if (!v[7].startsWith(Trans_EtherFi.CAUSALE_CARTA)) return false;
        String[] parti = v[0].split("_");
        return "PC".equalsIgnoreCase(parti[parti.length - 1]);
    }

    private static String Descrivi(String[] v) {
        return "Movimento " + v[1] + " " + v[10] + " " + v[8];
    }

    private static String Data(Riga p) {
        return FunzioniDate.ConvertiDatadaLongAlSecondo(p.istanteMs());
    }
}
