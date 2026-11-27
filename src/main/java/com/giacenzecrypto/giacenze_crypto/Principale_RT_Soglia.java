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
 * tabella principale RT va stampata a zero, e scrive la nota che dice l'effetto. Logica senza Swing,
 * come le altre {@code Principale_<area>}: la tabella la riempie {@code Principale}.
 *
 * <p>Il calcolo vero della soglia è {@link SogliaGiacenzaAnte2023}. Qui c'è solo l'uso che ne fa il quadro RT
 * (decisione dell'utente del 2026-11-12, opzione B di {@code Analisi_Soglia_51645.md}):</p>
 * <ul>
 *   <li><b>Soglia non superata e verificata</b>: la tabella mostra i valori calcolati, con la cella della plusvalenza in giallo e la nota,
 *       mentre nel quadro RT stampato corrispettivi, costi e plusvalenza dell'anno vanno a zero (l'archivio tiene i valori calcolati)
 *       (plusvalenze e minusvalenze non concorrono al reddito). Latente e valore di fine anno restano, perché non
 *       dipendono dalla soglia.</li>
 *   <li><b>Soglia non superata ma non verificabile</b> (qualche token in giacenza senza prezzo al 1° gennaio): la
 *       riga non si tocca e la nota lo dice, perché con quel valore la soglia potrebbe risultare superata.</li>
 *   <li><b>Soglia superata</b>: la riga non si tocca, la nota dice quando.</li>
 *   <li>Dal 2023, o con l'opzione spenta: nessun effetto e nessuna nota.</li>
 * </ul>
 *
 * <p>Il motore delle plusvalenze, il LIFO e i costi di carico <b>non cambiano</b>, e la tabella mostra sempre i
 * valori calcolati (dal 2026-11-14: azzerarli nella tabella confondeva): l'azzeramento riguarda solo quello che si
 * stampa nel PDF del quadro, tramite {@link Risultato#valoreStampato}: l'archivio tiene i valori della tabella. Quindi l'opzione non sta in {@code OpzioniRicalcolo.Epoca()}.</p>
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

    /** Riferimento normativo scritto nell'Excel di un anno azzerato. Niente punto e virgola, come ogni testo per l'utente. */
    static final String RIFERIMENTO_NORMATIVO = "Riferimenti normativi: circolare 30/E del 27 ottobre 2023 dell'Agenzia delle entrate, paragrafo 2.3.1 "
            + "(redditi fino al 31 dicembre 2022), che applica alle cripto-valute l'art. 67, comma 1, lettera c-ter), e comma 1-ter, del TUIR: "
            + "i prelievi da wallet sono cessioni rilevanti solo se la giacenza media di tutti i wallet, al cambio del 1° gennaio, supera 51.645,69 euro "
            + "per almeno sette giorni lavorativi continui nel periodo d'imposta.\n"
            + "La circolare precisa che, se la condizione non è integrata, "
            + "\"non si rendono deducibili neppure le minusvalenze eventualmente realizzate\".\n"
            + "Per la somma algebrica di plusvalenze e minusvalenze e il riporto delle eccedenze vale l'art. 68, comma 5, del TUIR.";

    /**
     * Avviso di un anno azzerato che ha movimenti su derivati: dice l'effetto e il riferimento, mai cosa fare. Il programma
     * li tratta come cripto-attività (limite noto), quindi li azzera con il resto.
     */
    static final String AVVISO_DERIVATI = "Nell'anno ci sono movimenti su derivati. Quelli non classificati come derivati sono trattati come "
            + "cripto-attività e sono riportati a zero insieme al resto, di quelli classificati solo l'effetto sulle cripto-attività "
            + "(il risultato dei derivati sta nella sezione dei redditi diversi di natura finanziaria e non è azzerato).\n"
            + "Per la circolare 30/E le cessioni a termine rilevano sempre (paragrafo 2.3.1) "
            + "e i redditi da derivati sono redditi diversi di natura finanziaria (paragrafo 2.3.2).";
    static final String AVVISO_DERIVATI_BREVE = "Derivati dell'anno azzerati con il resto";

    private static final String APRI = "<!--SOGLIA-->";
    private static final String CHIUDI = "<!--/SOGLIA-->";
    /** Segno, dentro la nota, di un anno la cui plusvalenza non va nel quadro RT: la tabella ne evidenzia in giallo la cella. */
    private static final String ZERO = "<!--SOGLIA-ZERO-->";
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

        /** {@code true} se nel quadro RT la riga va a zero. */
        public boolean azzerato() {
            return stato == Stato.NON_SUPERATA;
        }

        /**
         * Le righe da scrivere sotto il riepilogo dell'Excel di un anno azzerato nel quadro RT (vuoto negli altri casi):
         * l'Excel mostra i valori calcolati e queste righe dicono perché nel quadro stampato non compaiono, con i
         * riferimenti normativi.
         */
        public String[] righeExcel() {
            if (!azzerato()) {
                return new String[0];
            }
            return new String[] {
                "I valori di questo prospetto sono quelli calcolati dal programma. Nel quadro RT stampato sono riportati a zero.",
                nota,
                RIFERIMENTO_NORMATIVO };
        }

        /**
         * Il valore di una colonna della riga RT come va nel quadro stampato (non nell'archivio, che tiene i valori della tabella): zero per costi,
         * corrispettivi e plusvalenza di un anno azzerato, altrimenti il valore della tabella com'è.
         */
        public Object valoreStampato(int colonna, Object valoreTabella) {
            if (azzerato() && (colonna == COSTI || colonna == CORRISPETTIVI || colonna == PLUSVALENZA)) {
                return "0";
            }
            return valoreTabella;
        }
    }

    /** Opzione: la giacenza sul conto derivati conta nella soglia (default si', per prudenza). */
    public static final String OPZIONE_CONTO_DERIVATI = "Plusvalenze_SogliaContoDerivati";

    /** Nome con cui l'opzione, se spenta, compare fra quelle archiviate con la dichiarazione. */
    public static final String NOME_OPZIONE_CONTO_DERIVATI = "Nella soglia dei 51.645,69 euro non conta la giacenza sul conto derivati";

    /**
     * {@code true} se nel calcolo della soglia conta anche la giacenza sul conto derivati (margine e saldo sul sotto-wallet {@code Derivati}):
     * manca = conta. E' la lettura prudenziale: la circolare 30/E (par. 2.3.1) parla di giacenza di tutti i wallet, e con P2 il conto e'
     * parte del derivato ma resta un saldo in cripto presso l'exchange. La scelta e' dell'utente.
     */
    public static boolean contoDerivatiConta() {
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CONTO_DERIVATI);
        return v == null || !v.equalsIgnoreCase("NO");
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
     * Valuta l'esito per la riga dell'anno (7 colonne della tabella principale RT, come stringhe). <b>Non modifica la
     * riga</b>: la tabella mostra i valori calcolati, l'azzeramento di un anno sotto soglia lo applica solo la stampa del
     * quadro con {@link Risultato#valoreStampato}. La nota la aggiunge il chiamante con {@link #ConNota}.
     *
     * @param riga   le 7 colonne dell'anno, lette soltanto
     * @param esito  esito dell'anno, {@code null} se non calcolato (anno dal 2023, opzione spenta)
     */
    public static Risultato Applica(String[] riga, SogliaGiacenzaAnte2023.Esito esito) {
        return Applica(riga, esito, false);
    }

    /**
     * Come {@link #Applica(String[], SogliaGiacenzaAnte2023.Esito)}; con {@code derivatiNellAnno} la nota di un anno
     * azzerato dice anche che i movimenti su derivati dell'anno sono azzerati con il resto: tutto il movimento se non è
     * classificato come derivato (trattato come cripto-attività), solo il lato cripto se lo è (il risultato {@code [46]}
     * non entra nel quadro RT). La circolare 30/E tratta i derivati a parte: da rivedere con la sezione II-A.
     *
     * @param derivatiNellAnno {@code true} se l'anno ha movimenti su derivati ({@link Derivati#ContaNellAnno})
     */
    public static Risultato Applica(String[] riga, SogliaGiacenzaAnte2023.Esito esito, boolean derivatiNellAnno) {
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
                    + elenco(esito) + ", valorizzati a zero (" + massimo + " senza di loro).\nLa riga dell'anno non è stata azzerata." + acquisto(esito),
                    "Soglia 51.645,69 euro non verificabile\nmancano i prezzi di " + esito.tokenSenzaPrezzo.size()
                    + " token\n(pulsante Prezzi del 1° gennaio)" + acquistoBreve(esito), plus);
        }
        String serie = esito.serieMassima == 0 ? ""
                : ", serie più lunga " + esito.serieMassima + (esito.serieMassima == 1 ? " giorno" : " giorni") + " lavorativi";
        //Il risultato dell'anno può essere una minusvalenza: la dicitura segue il segno (sotto soglia vale per entrambe)
        String risultato = new BigDecimal(plus.trim()).signum() < 0 ? "Minusvalenza" : "Plusvalenza";
        String nota = "Soglia di 51.645,69 euro non superata (" + massimo + serie + "): plusvalenze e minusvalenze dell'anno non concorrono al reddito.\n"
                + "Nel quadro RT stampato corrispettivi, costi e plusvalenza sono riportati a zero, mentre la tabella e l'archivio riportano i valori calcolati." + acquisto(esito)
                + (derivatiNellAnno ? "\n" + AVVISO_DERIVATI : "");
        return new Risultato(Stato.NON_SUPERATA, nota,
                "Soglia 51.645,69 euro non superata\n(" + massimo + ")\n" + risultato + " non riportata in RT" + acquistoBreve(esito)
                + (derivatiNellAnno ? "\n" + AVVISO_DERIVATI_BREVE : ""), plus);
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
        return ConNota(colonnaErrori, nota, false);
    }

    /**
     * Come {@link #ConNota(String, String)}; con {@code azzerata} la nota porta il segno che fa evidenziare in giallo
     * la riga della tabella ({@link #RigaAzzerata}).
     */
    public static String ConNota(String colonnaErrori, String nota, boolean azzerata) {
        if (nota == null || nota.isEmpty()) {
            return colonnaErrori;
        }
        String gia = colonnaErrori == null ? "" : colonnaErrori.replace("<html>", "").replace("</html>", "");
        if (!gia.isEmpty() && !gia.endsWith("<br>")) {
            gia = gia + "<br>";
        }
        return "<html>" + gia + APRI + (azzerata ? ZERO : "") + Html(nota).replace("\n", "<br>") + CHIUDI + "</html>";
    }

    /** {@code true} se la colonna "Errori" porta la nota di un anno che nel quadro RT va a zero (riga da evidenziare). */
    public static boolean RigaAzzerata(Object colonnaErrori) {
        return colonnaErrori != null && colonnaErrori.toString().contains(ZERO);
    }

    /**
     * Le voci del dettaglio di una dichiarazione archiviata che l'archivio evidenzia in giallo, perché legate fra loro: la
     * plusvalenza che nel quadro RT è a zero, la nota della soglia. Vuoto se la dichiarazione non ha la
     * nota (anno non azzerato), così le stesse voci di un anno normale restano come sono.
     *
     * @param etichette le etichette (colonna "Voce") delle righe del dettaglio
     */
    public static java.util.Set<String> VociDaEvidenziare(Iterable<String> etichette) {
        boolean azzerato = false;
        for (String e : etichette) {
            if (TOTALE_NOTA.equals(e)) {
                azzerato = true;
                break;
            }
        }
        return azzerato
                ? java.util.Set.of(TOTALE_NOTA, Principale_Dichiarazioni.TOT_RT_PLUS)
                : java.util.Set.of();
    }

    /**
     * La nota breve per la colonna «Nota» dell'elenco dell'archivio, vuota se la dichiarazione non ha la nota della
     * soglia: dice perché la plusvalenza è in giallo. Segue il segno del risultato, come la nota della tabella.
     *
     * @param totali i totali della dichiarazione archiviata
     */
    public static String NotaElenco(Map<String, String> totali) {
        if (totali == null || !totali.containsKey(TOTALE_NOTA)) {
            return "";
        }
        boolean minusvalenza;
        try {
            minusvalenza = new BigDecimal(totali.getOrDefault(Principale_Dichiarazioni.TOT_RT_PLUS, "0").trim()).signum() < 0;
        } catch (RuntimeException e) {
            minusvalenza = false;
        }
        return "Soglia 51.645,69 euro non superata, " + (minusvalenza ? "minusvalenza" : "plusvalenza") + " non riportata in RT";
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
        return "\n" + (n == 1 ? "Un token è stato valorizzato" : n + " token sono stati valorizzati")
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

    private static int annoNumerico(String anno) {
        try {
            return Integer.parseInt(anno.trim());
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Un testo semplice reso sicuro per l'HTML (i simboli dei token possono contenere caratteri speciali). */
    public static String Html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
