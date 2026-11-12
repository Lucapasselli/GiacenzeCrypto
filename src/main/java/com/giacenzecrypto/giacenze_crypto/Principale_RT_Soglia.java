package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

/**
 * La soglia dei 51.645,69 euro dentro il quadro T/RT (anni fino al 2022): decide, per ogni anno, se la riga della
 * tabella principale RT va mostrata e stampata a zero, e scrive la nota che dice l'effetto. Logica senza Swing,
 * come le altre {@code Principale_<area>}: la tabella la riempie {@code Principale}.
 *
 * <p>Il calcolo vero della soglia è {@link SogliaGiacenzaAnte2023}. Qui c'è solo l'uso che ne fa il quadro RT
 * (decisione dell'utente del 2026-11-12, opzione B di {@code Analisi_Soglia_51645.md}):</p>
 * <ul>
 *   <li><b>Soglia non superata e verificata</b>: corrispettivi, costi e plusvalenza dell'anno si mostrano a zero
 *       (plusvalenze e minusvalenze non concorrono al reddito). Latente e valore di fine anno restano, perché non
 *       dipendono dalla soglia. I valori calcolati finiscono nella nota.</li>
 *   <li><b>Soglia non superata ma non verificabile</b> (qualche token in giacenza senza prezzo al 1° gennaio): la
 *       riga non si tocca e la nota lo dice, perché con quel valore la soglia potrebbe risultare superata.</li>
 *   <li><b>Soglia superata</b>: la riga non si tocca, la nota dice quando.</li>
 *   <li>Dal 2023, o con l'opzione spenta: nessun effetto e nessuna nota.</li>
 * </ul>
 *
 * <p>Il motore delle plusvalenze, il LIFO e i costi di carico <b>non cambiano</b>: questo agisce solo sulla riga
 * mostrata e stampata, quindi l'opzione non sta in {@code OpzioniRicalcolo.Epoca()}.</p>
 *
 * <p>La nota viaggia nella colonna "Errori" della tabella, racchiusa fra due commenti HTML, perché lì l'utente guarda
 * prima di stampare. Non è un errore da correggere: chi legge quella colonna per decidere se ci sono errori deve
 * toglierla con {@link #SenzaNota}, come fa già per i derivati.</p>
 */
public final class Principale_RT_Soglia {

    private Principale_RT_Soglia() {
    }

    /** Chiave dell'opzione in {@code personale.mv.db}: {@code SI} (default) o {@code NO}. */
    public static final String OPZIONE = "Plusvalenze_Pre2023Soglia";

    /** Testo con cui l'opzione compare fra le opzioni delle dichiarazioni archiviate. */
    public static final String NOME_OPZIONE = "Fino al 2022 verifica la soglia dei 51.645,69 euro (sette giorni lavorativi continui)";

    /** Chiave del totale che riporta la nota nella dichiarazione archiviata. */
    public static final String TOTALE_NOTA = "Soglia 51.645,69 euro";

    /** Chiave del totale con la plusvalenza calcolata ma non imponibile, solo per un anno azzerato. */
    public static final String TOTALE_PLUS_CALCOLATA = "Plusvalenza calcolata (non imponibile)";

    private static final String APRI = "<!--SOGLIA-->";
    private static final String CHIUDI = "<!--/SOGLIA-->";
    private static final Pattern NOTA = Pattern.compile(Pattern.quote(APRI) + ".*?" + Pattern.quote(CHIUDI), Pattern.DOTALL);

    /** Indici delle colonne della tabella principale RT che l'azzeramento riguarda. */
    private static final int COSTI = 1;
    private static final int CORRISPETTIVI = 2;
    private static final int PLUSVALENZA = 3;
    private static final int ERRORI = 6;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Come è andata la soglia per un anno. */
    public enum Stato {
        /** Anno dal 2023, opzione spenta o esito non calcolato: la riga resta com'è e non c'è nota. */
        NON_APPLICABILE,
        /** Superata: la riga resta com'è. */
        SUPERATA,
        /** Non superata e verificata: corrispettivi, costi e plusvalenza vanno a zero. */
        NON_SUPERATA,
        /** Non superata ma con token senza prezzo: la riga resta com'è. */
        NON_VERIFICABILE
    }

    /** Esito dell'applicazione a un anno: lo stato, la nota (testo semplice) e i valori calcolati prima dell'azzeramento. */
    public static final class Risultato {
        public final Stato stato;
        /** Nota in testo semplice, vuota se {@link Stato#NON_APPLICABILE}. Per stampa, archivio e avvisi. */
        public final String nota;
        /**
         * Versione corta della nota per la colonna "Errori" della tabella, che è stretta: righe corte separate da
         * {@code \n}, perché la tabella calcola l'altezza della riga dal numero di {@code <br>} e non manda a capo da sola.
         */
        public final String notaBreve;
        /** Plusvalenza calcolata dal motore, prima dell'azzeramento (come stringa della tabella). */
        public final String plusvalenzaCalcolata;

        Risultato(Stato stato, String nota, String notaBreve, String plusvalenzaCalcolata) {
            this.stato = stato;
            this.nota = nota;
            this.notaBreve = notaBreve;
            this.plusvalenzaCalcolata = plusvalenzaCalcolata;
        }

        /** {@code true} se la riga è stata azzerata. */
        public boolean azzerato() {
            return stato == Stato.NON_SUPERATA;
        }
    }

    /** {@code true} se l'opzione è attiva: manca = attiva. */
    public static boolean attiva() {
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE);
        return v == null || !v.equalsIgnoreCase("NO");
    }

    /**
     * L'esito della soglia di ogni anno ante 2023 fra quelli della tabella RT, a prezzo di mercato dalla cache/fonti.
     * Da chiamare <b>una volta</b> dopo il calcolo delle plusvalenze e fuori dal thread grafico: i prezzi possono
     * richiedere la rete. Il risultato non si ricalcola più: tabella, bozza, stampa e finestra di avviso leggono tutti
     * lo stesso, quindi non possono divergere. Mappa vuota se l'opzione è spenta.
     *
     * @param anni      anni della tabella ({@code yyyy})
     * @param interrotto vero se l'utente ha annullato (si ferma e restituisce quanto fatto)
     */
    public static Map<String, SogliaGiacenzaAnte2023.Esito> CalcolaEsiti(Iterable<String> anni, BooleanSupplier interrotto) {
        return CalcolaEsiti(anni, interrotto, attiva(), SogliaGiacenzaAnte2023::Calcola);
    }

    /** Come {@link #CalcolaEsiti(Iterable, BooleanSupplier)}, con opzione e calcolo iniettati. */
    static Map<String, SogliaGiacenzaAnte2023.Esito> CalcolaEsiti(Iterable<String> anni, BooleanSupplier interrotto,
            boolean opzioneAttiva, IntFunction<SogliaGiacenzaAnte2023.Esito> calcolo) {
        Map<String, SogliaGiacenzaAnte2023.Esito> esiti = new LinkedHashMap<>();
        if (!opzioneAttiva) {
            return esiti;
        }
        for (String anno : anni) {
            if (interrotto.getAsBoolean()) {
                break;
            }
            int a = annoNumerico(anno);
            if (a > 0 && a <= SogliaGiacenzaAnte2023.ULTIMO_ANNO) {
                esiti.put(anno, calcolo.apply(a));
            }
        }
        return esiti;
    }

    /**
     * Applica l'esito alla riga dell'anno (7 colonne della tabella principale RT, come stringhe): se la soglia non è
     * superata e verificata azzera costi, corrispettivi e plusvalenza <b>nella riga</b>. Non tocca la colonna
     * "Errori": la nota la aggiunge il chiamante con {@link #ConNota}.
     *
     * @param riga   le 7 colonne dell'anno, modificate sul posto solo se la soglia non è superata e verificata
     * @param esito  esito dell'anno, {@code null} se non calcolato (anno dal 2023, opzione spenta)
     */
    public static Risultato Applica(String[] riga, SogliaGiacenzaAnte2023.Esito esito) {
        if (esito == null || riga == null || riga.length <= PLUSVALENZA) {
            return new Risultato(Stato.NON_APPLICABILE, "", "", riga == null || riga.length <= PLUSVALENZA ? "" : riga[PLUSVALENZA]);
        }
        String plus = riga[PLUSVALENZA];
        if (esito.superata) {
            return new Risultato(Stato.SUPERATA, "Soglia di 51.645,69 euro superata per sette giorni lavorativi continui (dal "
                    + data(esito.inizioPeriodo) + " al " + data(esito.finePeriodo)
                    + "): plusvalenze e minusvalenze dell'anno concorrono al reddito." + acquisto(esito),
                    "Soglia 51.645,69 euro superata\ndal " + data(esito.inizioPeriodo) + " al " + data(esito.finePeriodo)
                    + acquistoBreve(esito), plus);
        }
        String massimo = "massimo " + euro(esito.valoreMassimo) + " euro"
                + (esito.giornoMassimo == null ? "" : " il " + data(esito.giornoMassimo));
        if (esito.incompleto()) {
            return new Risultato(Stato.NON_VERIFICABILE, "Soglia di 51.645,69 euro non verificabile: manca il prezzo del 1° gennaio di "
                    + elenco(esito) + ", valorizzati a zero (" + massimo + " senza di loro). La riga dell'anno non è stata azzerata." + acquisto(esito),
                    "Soglia 51.645,69 euro non verificabile\nmancano i prezzi di " + esito.tokenSenzaPrezzo.size()
                    + " token\n(pulsante Prezzi del 1° gennaio)" + acquistoBreve(esito), plus);
        }
        String serie = esito.serieMassima == 0 ? ""
                : ", serie più lunga " + esito.serieMassima + (esito.serieMassima == 1 ? " giorno" : " giorni") + " lavorativi";
        String nota = "Soglia di 51.645,69 euro non superata (" + massimo + serie + "): plusvalenze e minusvalenze dell'anno non concorrono al reddito. "
                + "Valori calcolati prima dell'azzeramento: plusvalenza " + euro(plus) + " euro, corrispettivi "
                + euro(riga[CORRISPETTIVI]) + " euro, costi " + euro(riga[COSTI]) + " euro." + acquisto(esito);
        riga[COSTI] = "0";
        riga[CORRISPETTIVI] = "0";
        riga[PLUSVALENZA] = "0";
        return new Risultato(Stato.NON_SUPERATA, nota,
                "Soglia 51.645,69 euro non superata\n(" + massimo + ")\nanno azzerato" + acquistoBreve(esito), plus);
    }

    /** L'istante del prezzo della soglia: le 00:00 del 1° gennaio, in millisecondi (ora di Roma). */
    public static long IstantePrezzo(int anno) {
        return FunzioniDate.ConvertiDatainLongSecondo(anno + "-01-01 00:00:00");
    }

    /**
     * Salva il prezzo unitario che l'utente assegna a un token (o NFT) senza prezzo al 1° gennaio, come prezzo
     * personalizzato per tutti i gruppi wallet, alle 00:00 del 1° gennaio: da lì lo legge il calcolo della soglia,
     * come ogni altro prezzo personalizzato (viene prima delle fonti). Zero è ammesso: è la risposta per un token
     * tecnico o senza valore. Il prezzo vale anche per il quadro RW e per i movimenti di quell'istante, come tutti i
     * personalizzati.
     *
     * @param m                     il token, con address e rete come compaiono nei movimenti
     * @param anno                  anno della soglia (il prezzo è quello delle 00:00 del suo 1° gennaio)
     * @param prezzoUnitario        euro per una unità, numero non negativo
     * @param fonte                 etichetta della fonte (vuota o {@code null} = "Personalizzato")
     * @param timestampDaCancellare istante di un eventuale prezzo personalizzato precedente da sostituire, 0 se non c'è
     * @return {@code true} se il prezzo è stato salvato
     */
    public static boolean SalvaPrezzo(Moneta m, int anno, BigDecimal prezzoUnitario, String fonte, long timestampDaCancellare) {
        if (m == null || prezzoUnitario == null || prezzoUnitario.signum() < 0) {
            return false;
        }
        String f = fonte == null || fonte.isBlank() ? "Personalizzato" : fonte;
        return DatabaseH2.InserisciPrezzoPresonalizzato(IstantePrezzo(anno), f, m.Moneta,
                prezzoUnitario.toPlainString(), m.Rete, m.MonetaAddress, "TUTTI", timestampDaCancellare);
    }

    /** La colonna "Errori" con la nota in coda (dopo gli eventuali errori già presenti), in HTML. */
    public static String ConNota(String colonnaErrori, String nota) {
        if (nota == null || nota.isEmpty()) {
            return colonnaErrori;
        }
        String gia = colonnaErrori == null ? "" : colonnaErrori.replace("<html>", "").replace("</html>", "");
        if (!gia.isEmpty() && !gia.endsWith("<br>")) {
            gia = gia + "<br>";
        }
        return "<html>" + gia + APRI + Html(nota).replace("\n", "<br>") + CHIUDI + "</html>";
    }

    /** La colonna "Errori" senza la nota della soglia, perché non è un errore da correggere. */
    public static String SenzaNota(String colonnaErrori) {
        return colonnaErrori == null ? "" : NOTA.matcher(colonnaErrori).replaceAll("");
    }

    /**
     * Frase della nota completa sui token valorizzati al prezzo di acquisto perché senza prezzo del 1° gennaio, vuota se
     * non ce ne sono: la norma non dice cosa fare per quei token, quindi il valore usato va dichiarato.
     */
    private static String acquisto(SogliaGiacenzaAnte2023.Esito esito) {
        int n = esito.prezzoDiAcquisto.size();
        if (n == 0) {
            return "";
        }
        return " " + (n == 1 ? "Un token è stato valorizzato" : n + " token sono stati valorizzati")
                + " al prezzo di acquisto perché senza prezzo del 1° gennaio (elenco e correzione con il pulsante Prezzi del 1° gennaio).";
    }

    /** Come {@link #acquisto} per la nota breve della tabella: una riga in più. */
    private static String acquistoBreve(SogliaGiacenzaAnte2023.Esito esito) {
        int n = esito.prezzoDiAcquisto.size();
        return n == 0 ? "" : "\n" + n + " token" + " al prezzo di acquisto";
    }

    private static String elenco(SogliaGiacenzaAnte2023.Esito esito) {
        int n = esito.tokenSenzaPrezzo.size();
        int mostrati = Math.min(n, 5);
        String s = String.join(", ", esito.tokenSenzaPrezzo.subList(0, mostrati));
        return n > mostrati ? s + " e altri " + (n - mostrati) + " token" : s;
    }

    private static String data(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }

    private static String euro(BigDecimal v) {
        return String.format(Locale.ITALY, "%,.2f", v);
    }

    /** Come {@link #euro(BigDecimal)} per un valore di tabella (stringa decimale), che può mancare. */
    private static String euro(String v) {
        try {
            return euro(new BigDecimal(v.trim()));
        } catch (RuntimeException e) {
            return v == null ? "0,00" : v;
        }
    }

    private static int annoNumerico(String anno) {
        try {
            return Integer.parseInt(anno.trim());
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Un testo semplice reso sicuro per l'HTML (i simboli dei token possono contenere caratteri speciali). */
    public static String Html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
