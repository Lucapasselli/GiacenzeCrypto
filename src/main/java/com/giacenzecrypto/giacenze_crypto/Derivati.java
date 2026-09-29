package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;

/**
 * Marcatore dei movimenti che riguardano <b>strumenti derivati</b> (perpetui, futures, Dual Investment...),
 * nel campo {@value #CAMPO} della riga del movimento.
 *
 * <p><b>Oggi è solo identificazione.</b> Nessun motore di calcolo legge questo campo: i movimenti
 * marcati sono trattati esattamente come gli altri movimenti di cripto-attività (depositi/prelievi da
 * classificare, scambi, commissioni). Non è corretto, perché i redditi da derivati sono redditi diversi
 * di natura finanziaria (art. 67 c.1 lett. c-quater TUIR) e non si compensano con le cripto-attività:
 * per questo l'import avvisa ({@link Importazioni#TestoAvvisoDerivati()}) e le stampe dei quadri W/RW e
 * T/RT avvisano quando nell'anno, o in quelli precedenti, ci sono movimenti marcati
 * ({@link #TestoAvvisoQuadro}). Il marcatore
 * esiste già adesso perché la gestione fiscale dei derivati, quando verrà fatta, trovi i movimenti
 * riconoscibili senza doverli reimportare. Disegno e decisioni aperte in
 * {@code nocommit/Documentazione/Analisi_Derivati.md}.
 *
 * <p>Il valore è un tipo breve (vedi le costanti), vuoto per ogni movimento che non è un derivato. Lo
 * scrive l'import generico a partire dalla chiave {@code causaliDerivati} della configurazione, che
 * associa ogni causale del file al suo tipo. Regole di copia come per il campo 43: fuori da
 * {@code MovimentiCrypto.CampiNonCopiabiliVerbatim}, dentro le liste di provenienza di
 * {@code Principale_Movimenti_SeparaUnisci}. Resta fuori da {@code Calcoli_PlusvalenzeNew.Impronta}
 * finché il motore non lo legge.
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
    /** Sottoscrizione o regolamento di un Dual Investment (prodotto a termine/opzionario). */
    public static final String DUAL = "DUAL";
    /** Commissione separata nata da una riga di derivati. */
    public static final String COMMISSIONE = "COMMISSIONE";

    /** @return il tipo di derivato del movimento, stringa vuota se non è un derivato */
    public static String Tipo(String[] v) {
        if (v == null || v.length <= CAMPO || v[CAMPO] == null) return "";
        return v[CAMPO].trim();
    }

    /** @return {@code true} se il movimento è marcato come derivato */
    public static boolean isDerivato(String[] v) {
        return !Tipo(v).isEmpty();
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
     * Movimenti su derivati dell'anno ({@code [0]}) e degli anni precedenti ({@code [1]}). Contano anche
     * i secondi: le cripto arrivate o uscite con un derivato restano nel LIFO e nelle giacenze, quindi un
     * PnL del 2022 trattato come deposito a costo zero diventa plusvalenza cripto nell'anno in cui quelle
     * cripto vengono vendute, e le giacenze di fine anno lo portano con se'.
     */
    static int[] Conta(String Anno) {
        int[] n = {0, 0};
        if (Anno == null || Anno.isBlank() || Principale.MappaCryptoWallet == null) return n;
        String anno = Anno.trim();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] == null || v[1].length() < 4 || !isDerivato(v)) continue;
            int cmp = v[1].substring(0, 4).compareTo(anno);
            if (cmp == 0) n[0]++;
            else if (cmp < 0) n[1]++;
        }
        return n;
    }

    /** @return {@code true} se l'anno o gli anni precedenti hanno movimenti su derivati */
    public static boolean InfluisconoSullAnno(String Anno) {
        int[] n = Conta(Anno);
        return n[0] + n[1] > 0;
    }

    /**
     * Testo HTML dell'avviso da mettere nelle stampe dei quadri W/RW e T/RT quando l'anno o quelli prima
     * hanno movimenti su derivati, o stringa vuota se non ce ne sono. Il testo sta in
     * {@code config/varie/NoteCompilazione.json} (chiave {@link NoteCompilazione#DERIVATI}), come le altre
     * note fiscali dei quadri.
     */
    public static String TestoAvvisoQuadro(String Anno) {
        int[] n = Conta(Anno);
        if (n[0] + n[1] == 0) return "";
        return NoteCompilazione.Testo(NoteCompilazione.DERIVATI, Anno,
                Map.of("numero", String.valueOf(n[0]), "precedenti", String.valueOf(n[1])));
    }

    /**
     * Messaggio breve per le finestre di avviso prima della stampa (il testo completo va nel report).
     *
     * @return il messaggio, o stringa vuota se l'anno e quelli prima non hanno movimenti su derivati
     */
    public static String MessaggioAvviso(String Anno) {
        int[] n = Conta(Anno);
        if (n[0] + n[1] == 0) return "";
        return "Movimenti su strumenti derivati: " + n[0] + " nell'anno " + Anno + ", " + n[1]
                + " negli anni precedenti.\n\n"
                + "Il programma non gestisce ancora i derivati: questi movimenti sono trattati come "
                + "cripto-attività e concorrono a plusvalenze e giacenze cripto, anche negli anni "
                + "successivi (le cripto ricevute o pagate con i derivati restano nel calcolo). Non è "
                + "corretto, perché i redditi da derivati sono redditi diversi di natura finanziaria, da "
                + "dichiarare in una sezione diversa e non compensabili con le cripto-attività.\n\n"
                + "Il report riporta l'avviso completo. Si consiglia di verificare con un professionista.";
    }
}
