package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;

/**
 * Marcatore dei movimenti che riguardano <b>strumenti derivati</b> (perpetui, futures, Dual Investment...),
 * nel campo {@value #CAMPO} della riga del movimento.
 *
 * <p>Il marcatore da solo non cambia nessun calcolo: un movimento marcato resta un deposito/prelievo da
 * classificare, trattato come cripto-attività. Lo diventa la classificazione {@code DDV}/{@code PDV} del campo 18
 * ({@code Principale_Derivati}, automatica per i movimenti nuovi), con cui il motore scrive il risultato in
 * {@link #CAMPO_RISULTATO}, e per i contratti OKX il token e il margine neutri ({@code OKXDerivati}) con il reddito
 * in LIFO ({@code DerivatiLifo}). Il risultato entra nella sezione II-A del quadro RT e nella sezione II del quadro T
 * (movimento per movimento, {@code Principale_RT_Derivati}), non nel W/RW. L'import avvisa
 * ({@link Importazioni#TestoAvvisoDerivati()}) e le stampe dei quadri W/RW e T/RT avvisano quando
 * {@link #DaSegnalare} ({@link #TestoAvvisoQuadro}). Disegno e decisioni in
 * {@code nocommit/Documentazione/Decisioni_Derivati.md}.
 *
 * <p>Il valore è un tipo breve (vedi le costanti), vuoto per ogni movimento che non è un derivato. Lo
 * scrive l'import generico a partire dalla chiave {@code causaliDerivati} della configurazione, che
 * associa ogni causale del file al suo tipo. Regole di copia come per il campo 43: fuori da
 * {@code MovimentiCrypto.CampiNonCopiabiliVerbatim}, dentro le liste di provenienza di
 * {@code Principale_Movimenti_SeparaUnisci}. Sta nell'impronta del ricalcolo incrementale ({@code Calcoli_PlusvalenzeNew.Impronta}):
 * con l'opzione P2 ({@link ContoDerivati}) il motore lo legge.
 */
public final class Derivati {

    private Derivati() {
    }

    /** Colonna della riga del movimento che porta il tipo di derivato. */
    public static final int CAMPO = 44;

    /** Risultato realizzato di una posizione (differenziale positivo o negativo). */
    public static final String PNL = "PNL";
    /** Funding dei perpetui, incassato o pagato. */
    public static final String FUNDING = "FUNDING";
    /** Bonus di trading accreditato dall'exchange, o il suo recupero. */
    public static final String BONUS = "BONUS";
    /** Rimborso di commissioni sui derivati. */
    public static final String RIMBORSO_COMMISSIONI = "RIMBORSO_COMMISSIONI";
    /** Regolamento alla scadenza di un contratto a termine. */
    public static final String CONSEGNA = "CONSEGNA";
    /**
     * Sottoscrizione o regolamento di un Dual Investment. Solo un'etichetta (filtro, giro del movimento): dal 21/11/2026 (decisione D5,
     * lettura A) il programma tratta i Dual come cripto-attività per scelta, quindi non contano fra i derivati da segnalare
     * ({@link #Conta}) e non si classificano come derivati ({@code Principale_Derivati.MotivoNonClassificabile}).
     */
    public static final String DUAL = "DUAL";
    /** Commissione separata nata da una riga di derivati. */
    public static final String COMMISSIONE = "COMMISSIONE";

    /**
     * Colonna del <b>risultato dei derivati in euro</b> (c-quater), scritta dal motore delle plusvalenze: positiva
     * per un accredito, negativa per un addebito o una commissione. Vuota per ogni movimento che non è stato
     * classificato come derivato. <b>Resta separata da {@code v[19]}/{@code v[33]}</b>, che sono solo plusvalenze
     * da cripto-attività (c-sexies): tutti i totali le sommano senza condizioni, e un risultato dei derivati
     * messo lì finirebbe fra le cripto in silenzio. Calcolata: sta in {@code MovimentiCrypto.CampiNonCopiabiliVerbatim}.
     */
    public static final int CAMPO_RISULTATO = 46;

    /** Token che rappresenta una posizione aperta su un contratto (apertura e chiusura, nel sotto-wallet dei derivati). */
    public static final String CONTRATTO = "CONTRATTO";
    /** Margine parcheggiato nel sotto-wallet dei derivati all'apertura, o riportato indietro alla chiusura. */
    public static final String MARGINE = "MARGINE";

    /**
     * Colonna dei <b>dati del contratto</b> (2026-11-16): tutto quello che l'exchange dice di un'operazione su derivati,
     * come coppie {@code chiave=valore} separate da {@code |} (vedi {@link #Dati} e {@link #ImpostaDati}). Solo
     * informazione: nessun motore la legge, sta fuori da {@code Impronta} e dal golden master. Scritta dall'import
     * sulle righe di contratto e di margine, si copia tale e quale (come {@code [42]}/{@code [43]}/{@code [45]}).
     */
    public static final int CAMPO_DATI = 47;

    /** Prefisso del simbolo del token di un contratto, e suffissi del verso: {@code DERIV.BTC-USD.UM.XPERP-991231.LONG}. */
    public static final String PREFISSO_TOKEN = "DERIV.";
    public static final String SUFFISSO_LONG = ".LONG";
    public static final String SUFFISSO_SHORT = ".SHORT";

    /**
     * Simbolo del token che rappresenta la posizione su un contratto. Il verso sta nel simbolo: un long e uno short sono
     * due token distinti e nessuno dei due ha mai quantità negativa (altrimenti scatterebbero le giacenze negative).
     * I trattini bassi dell'{@code instId} diventano punti, perché {@code creaMovimento} li toglie dai nomi.
     *
     * @param instId identificativo del contratto, come lo scrive l'exchange (es. {@code BTC-USD_UM_XPERP-991231})
     * @param lungo {@code true} per una posizione long
     */
    public static String NomeTokenContratto(String instId, boolean lungo) {
        String base = instId == null ? "" : instId.trim().replace('_', '.').replaceAll("[;,\\s|=]", "");
        return PREFISSO_TOKEN + base + (lungo ? SUFFISSO_LONG : SUFFISSO_SHORT);
    }

    /** @return {@code true} se il simbolo è quello del token di un contratto ({@link #NomeTokenContratto}) */
    public static boolean IsTokenContratto(String simbolo) {
        if (simbolo == null) return false;
        String s = simbolo.trim().toUpperCase();
        return s.startsWith(PREFISSO_TOKEN) && (s.endsWith(SUFFISSO_LONG) || s.endsWith(SUFFISSO_SHORT));
    }

    /**
     * Prezzo del token di un contratto: <b>zero, senza cercare nulla</b>. È una fotografia della posizione, non una
     * cripto-attività con un mercato: senza questa regola ogni valutazione a moneta singola (giacenze del quadro RW,
     * Giacenze a data, soglia dei 51.645 euro) lo cercherebbe sulle fonti, in rete, e lo segnalerebbe come prezzo mancante.
     *
     * @return l'informazione di prezzo a zero, oppure {@code null} se la moneta non è un token di contratto
     */
    public static Prezzi.InfoPrezzo PrezzoContratto(Moneta m, long data) {
        if (m == null || !IsTokenContratto(m.Moneta)) return null;
        Prezzi.InfoPrezzo ip = new Prezzi.InfoPrezzo();
        ip.Moneta = m.Moneta;
        ip.Qta = m.Qta == null || m.Qta.isBlank() || !Funzioni.isNumeric(m.Qta, false)
                ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(m.Qta);
        ip.prezzoUnitario = java.math.BigDecimal.ZERO;
        ip.prezzoQta = java.math.BigDecimal.ZERO;
        ip.Fonte = "Contratto derivato";
        ip.timestamp = data;
        return ip;
    }

    /** @return le coppie del campo {@link #CAMPO_DATI} nell'ordine in cui sono scritte, vuota se il campo è vuoto */
    public static Map<String, String> Dati(String[] v) {
        return LeggiDati(v != null && v.length > CAMPO_DATI ? v[CAMPO_DATI] : "");
    }

    /** Come {@link #Dati(String[])}, da un testo già estratto. */
    public static Map<String, String> LeggiDati(String testo) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        if (testo == null || testo.isBlank()) return m;
        for (String coppia : testo.split("\\|")) {
            int uguale = coppia.indexOf('=');
            if (uguale <= 0) continue;
            m.put(coppia.substring(0, uguale).trim(), coppia.substring(uguale + 1).trim());
        }
        return m;
    }

    /** Il testo del campo dati: i valori vuoti si omettono, {@code ;} {@code |} {@code =} e gli a capo si tolgono dai valori. */
    public static String ScriviDati(Map<String, String> dati) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : dati.entrySet()) {
            if (e.getValue() == null || e.getValue().isBlank()) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append('=').append(e.getValue().replaceAll("[;|=\\r\\n]", "").trim());
        }
        return sb.toString();
    }

    /** Scrive il campo dati sul movimento. Una riga più corta del campo si lascia stare. */
    public static void ImpostaDati(String[] v, Map<String, String> dati) {
        if (v == null || v.length <= CAMPO_DATI) return;
        v[CAMPO_DATI] = ScriviDati(dati);
    }

    /** @return {@code true} se il movimento è una riga di contratto o di margine nata dall'import ({@link #CONTRATTO}, {@link #MARGINE}) */
    public static boolean IsRigaContrattoOMargine(String[] v) {
        String t = Tipo(v);
        return t.equals(CONTRATTO) || t.equals(MARGINE);
    }

    /** Marcatore del campo 18 di un deposito che è un accredito da derivati (PnL positivo, funding incassato, bonus). */
    public static final String MARCATORE_ACCREDITO = "DDV";
    /** Testo completo del campo 18 per un accredito da derivati. */
    public static final String CAMPO18_ACCREDITO = "DDV - ACCREDITO DA DERIVATI";
    /** Marcatore del campo 18 di un prelievo (o commissione) che è un addebito per derivati. */
    public static final String MARCATORE_ADDEBITO = "PDV";
    /** Testo completo del campo 18 per un addebito per derivati. */
    public static final String CAMPO18_ADDEBITO = "PDV - ADDEBITO PER DERIVATI";
    /** Campo 5 dei movimenti classificati come derivati. */
    public static final String CAMPO5_ACCREDITO = "ACCREDITO DERIVATI";
    public static final String CAMPO5_ADDEBITO = "ADDEBITO DERIVATI";

    /** @return {@code true} se il campo 18 porta uno dei due marcatori dei derivati */
    public static boolean HaMarcatore(String[] v) {
        return v != null && v.length > 18 && v[18] != null
                && (v[18].contains(MARCATORE_ACCREDITO) || v[18].contains(MARCATORE_ADDEBITO));
    }

    /** @return il risultato dei derivati del movimento (in euro, con segno), {@code null} se vuoto o non numerico */
    public static java.math.BigDecimal Risultato(String[] v) {
        if (v == null || v.length <= CAMPO_RISULTATO || v[CAMPO_RISULTATO] == null || v[CAMPO_RISULTATO].isBlank()) return null;
        try {
            return new java.math.BigDecimal(v[CAMPO_RISULTATO].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Il {@code [46]} che il motore scrive per un movimento classificato come derivato: {@code +valore} con {@code DDV},
     * {@code -valore} con {@code PDV} (prelievo o commissione). Unica copia della regola: la usano
     * {@code Calcoli_PlusvalenzeNew.ElaboraMovimento} e {@link DerivatiLifo#AggiornaRisultati}, che deve rimettere il valore
     * del motore sulle righe di pnl che la passata incrementale non ha rielaborato.
     *
     * @param accredito {@code true} per {@code DDV}, {@code false} per {@code PDV}
     * @param valore il valore in euro del movimento ({@code [15]})
     */
    static String RisultatoDaClassificazione(boolean accredito, java.math.BigDecimal valore) {
        return (accredito ? valore : valore.negate()).toPlainString();
    }

    /** @return il tipo di derivato del movimento, stringa vuota se non è un derivato */
    public static String Tipo(String[] v) {
        if (v == null || v.length <= CAMPO || v[CAMPO] == null) return "";
        return v[CAMPO].trim();
    }

    /** @return {@code true} se il movimento è marcato come derivato */
    public static boolean isDerivato(String[] v) {
        return !Tipo(v).isEmpty();
    }

    /** Colonna 43 "Derivato" della tabella dei movimenti: il tipo del marcatore ({@code [44]}), vuota per ogni altro movimento. */
    public static String ColonnaDerivato(String[] v) {
        return Tipo(v);
    }

    /**
     * Colonna 44 "Risultato derivati": il risultato in euro di {@code [46]} come testo, vuota se il movimento non ne ha uno.
     * Il risultato dei derivati non e' in {@code v[19]}: va letto da qui.
     */
    public static String ColonnaRisultato(String[] v) {
        java.math.BigDecimal r = Risultato(v);
        return r == null ? "" : r.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Somma del risultato dei derivati ({@code [46]}) di un insieme di movimenti, e quanti ne hanno uno.
     *
     * @return {@code [somma, quanti]}, la somma {@code null} se nessun movimento ha un risultato
     */
    public static Object[] TotaleRisultato(Iterable<String[]> movimenti) {
        java.math.BigDecimal somma = null;
        int n = 0;
        if (movimenti != null) {
            for (String[] v : movimenti) {
                java.math.BigDecimal r = Risultato(v);
                if (r == null) continue;
                somma = somma == null ? r : somma.add(r);
                n++;
            }
        }
        return new Object[]{somma, n};
    }

    /** @return {@code true} se il movimento è marcato come Dual Investment ({@link #DUAL}) */
    public static boolean IsDual(String[] v) {
        return v != null && v.length > CAMPO && DUAL.equalsIgnoreCase(v[CAMPO] == null ? "" : v[CAMPO].trim());
    }

    /** Scrive il tipo sul movimento. Un tipo vuoto o {@code null} non cancella un marcatore già presente. */
    public static void Marca(String[] v, String Tipo) {
        if (v == null || v.length <= CAMPO || Tipo == null || Tipo.isBlank()) return;
        v[CAMPO] = Tipo.trim();
    }

    /**
     * Numero dei movimenti marcati come derivati con data nell'anno indicato. Nessun filtro per wallet:
     * l'avviso vale per l'anno d'imposta, indipendentemente da quali gruppi entrano nel quadro.
     *
     * @param Anno anno a quattro cifre
     */
    public static int ContaNellAnno(String Anno) {
        return Conta(Anno)[0];
    }

    /**
     * Movimenti su derivati dell'anno ({@code [0]}) e degli anni precedenti ({@code [1]}), e fra questi quelli non
     * classificati come derivati ({@code [2]} nell'anno, {@code [3]} prima: campo 18 senza {@code DDV}/{@code PDV}).
     * Un movimento non classificato conta anche negli anni dopo: le cripto arrivate o uscite con quel derivato
     * restano nel LIFO e nelle giacenze, quindi un PnL del 2022 trattato come deposito a costo zero diventa
     * plusvalenza cripto nell'anno in cui quelle cripto vengono vendute. Uno classificato no: il deposito carica
     * le cripto al loro valore (decisione D10), e il suo effetto si ferma al suo anno. Con l'opzione P2 un movimento da classificare
     * sul conto derivati conta come classificato ({@link ContoDerivati#CalcolatoComeDerivato}): il motore lo tratta come tale.
     */
    static int[] Conta(String Anno) {
        int[] n = {0, 0, 0, 0};
        if (Anno == null || Anno.isBlank() || Principale.MappaCryptoWallet == null) return n;
        String anno = Anno.trim();
        Boolean contoP2 = null;   //letta solo se serve: l'opzione sta nel database personale
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] == null || v[1].length() < 4 || !isDerivato(v)) continue;
            //Margine parcheggiato e token del contratto nascono gia' neutri (OKXDerivati): non sono movimenti trattati come cripto-attivita'
            if (IsRigaContrattoOMargine(v)) continue;
            //Dual Investment: cripto-attivita' per scelta (D5), non un derivato trattato come cripto
            if (IsDual(v)) continue;
            boolean classificato = HaMarcatore(v);
            if (!classificato && ContoDerivati.DaClassificareSulConto(v, Principale.MappaCryptoWallet)) {
                if (contoP2 == null) contoP2 = ContoDerivati.Attivo();
                classificato = contoP2 && ContoDerivati.CalcolatoComeDerivato(v, Principale.MappaCryptoWallet);
            }
            int cmp = v[1].substring(0, 4).compareTo(anno);
            if (cmp == 0) {
                n[0]++;
                if (!classificato) n[2]++;
            } else if (cmp < 0) {
                n[1]++;
                if (!classificato) n[3]++;
            }
        }
        return n;
    }

    /**
     * Testo della voce {@code Avviso} dei totali di una dichiarazione archiviata, vuoto se non c'è niente da segnalare
     * ({@link #DaSegnalare}). La voce resta fuori dal confronto con la definitiva ({@code Dichiarazioni.TOTALI_NON_CONFRONTATI}):
     * le definitive archiviate prima portano il testo di allora.
     */
    public static String TestoAvvisoArchivio(String Anno) {
        if (!DaSegnalare(Anno)) return "";
        int[] n = Conta(Anno);
        java.util.List<String> parti = new java.util.ArrayList<>();
        if (n[0] - n[2] > 0) parti.add((n[0] - n[2]) + " classificati come derivati nell'anno");
        if (n[2] + n[3] > 0) parti.add((n[2] + n[3]) + " non classificati trattati come cripto-attività");
        return "Movimenti su derivati: " + String.join(", ", parti);
    }

    /**
     * @return {@code true} se l'anno ha movimenti su derivati (il loro risultato va nella sezione II-A del T/RT, quelli non
     * classificati sono trattati come cripto-attività) o se gli anni precedenti ne hanno di non classificati. Decide
     * l'avviso prima della stampa, la voce della colonna Errori del quadro RT e la nota stampata
     */
    public static boolean DaSegnalare(String Anno) {
        int[] n = Conta(Anno);
        return n[0] + n[3] > 0;
    }

    /** @return la somma del risultato dei derivati ({@code [46]}) dei movimenti con data nell'anno, {@code null} se nessuno ne ha uno */
    static java.math.BigDecimal RisultatoDellAnno(String Anno) {
        if (Anno == null || Anno.isBlank() || Principale.MappaCryptoWallet == null) return null;
        String anno = Anno.trim();
        java.util.List<String[]> dellAnno = new java.util.ArrayList<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] != null && v[1].startsWith(anno)) dellAnno.add(v);
        }
        return (java.math.BigDecimal) TotaleRisultato(dellAnno)[0];
    }

    /**
     * Testo HTML dell'avviso da mettere nelle stampe dei quadri W/RW e T/RT quando {@link #DaSegnalare}, o stringa
     * vuota. Il testo sta in {@code config/varie/NoteCompilazione.json} (chiave {@link NoteCompilazione#DERIVATI}),
     * come le altre note fiscali dei quadri.
     */
    public static String TestoAvvisoQuadro(String Anno) {
        if (!DaSegnalare(Anno)) return "";
        int[] n = Conta(Anno);
        return NoteCompilazione.Testo(NoteCompilazione.DERIVATI_II_A, Anno,
                Map.of("numero", String.valueOf(n[0]), "precedenti", String.valueOf(n[1])));
    }

    /**
     * Messaggio breve per le finestre di avviso prima della stampa (il testo completo va nel report).
     *
     * @return il messaggio, o stringa vuota se non c'è niente da segnalare ({@link #DaSegnalare})
     */
    public static String MessaggioAvviso(String Anno) {
        if (!DaSegnalare(Anno)) return "";
        int[] n = Conta(Anno);
        StringBuilder sb = new StringBuilder();
        sb.append("Movimenti su strumenti derivati: ").append(n[0]).append(" nell'anno ").append(Anno).append(", ")
                .append(n[1]).append(" negli anni precedenti.");
        int classificatiAnno = n[0] - n[2];
        if (classificatiAnno > 0) {
            java.math.BigDecimal r = RisultatoDellAnno(Anno);
            sb.append("\n\n").append(classificatiAnno).append(classificatiAnno == 1 ? " movimento dell'anno è classificato" : " movimenti dell'anno sono classificati")
                    .append(" come derivati: il loro risultato (colonna Risultato derivati");
            if (r != null) sb.append(", in totale ").append(r.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(" euro nel ").append(Anno);
            sb.append(") è riportato nel quadro T/RT, nella sezione dei redditi diversi di natura finanziaria, movimento per movimento.");
        }
        int nonClassificati = n[2] + n[3];
        if (nonClassificati > 0) {
            sb.append("\n\n").append(nonClassificati).append(nonClassificati == 1 ? " movimento non classificato" : " movimenti non classificati")
                    .append(" (").append(n[2]).append(" nell'anno, ").append(n[3]).append(" negli anni precedenti) ")
                    .append(nonClassificati == 1 ? "è trattato come cripto-attività e concorre" : "sono trattati come cripto-attività e concorrono")
                    .append(" a plusvalenze e giacenze cripto, anche negli anni successivi ")
                    .append("(le cripto ricevute o pagate con i derivati restano nel calcolo). ")
                    .append(nonClassificati == 1 ? "Il suo risultato non entra" : "Il loro risultato non entra")
                    .append(" fra i redditi da derivati del quadro T/RT.");
        }
        sb.append("\n\nI redditi da derivati sono redditi diversi di natura finanziaria, da dichiarare in una sezione diversa ")
                .append("e non compensabili con le cripto-attività.\n\n")
                .append("Il report riporta l'avviso completo. Si consiglia di verificare con un professionista.");
        return sb.toString();
    }
}
