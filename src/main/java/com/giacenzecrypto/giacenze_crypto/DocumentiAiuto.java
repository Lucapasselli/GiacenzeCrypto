package com.giacenzecrypto.giacenze_crypto;

/**
 * I manuali che le varie schermate aprono nel browser dell'utente.
 * <p>I file sono pubblicati sul sito del progetto, le cui sorgenti stanno in {@code docs/} nel
 * repository (GitHub Pages): è lo stesso dominio dell'informativa privacy dichiarata a Microsoft.
 * <p>Fino al 18/08/2026 stavano invece su SourceForge, e ogni schermata ne scriveva l'indirizzo per
 * conto proprio — nove copie a mano dello stesso URL, una delle quali ripetuta. Il cambio di sede non è
 * un obbligo di policy (aprire un PDF nel browser non è né esecuzione di codice scaricato né
 * installazione di software, cioè non tocca le 10.2.2 e 10.2.3): un indirizzo {@code /download} di
 * SourceForge apre una pagina con pubblicità e con l'elenco dei file scaricabili, installer compresi,
 * qualche rete aziendale la blocca, e nove letterali sono nove occasioni di refuso.
 * <p>I nomi dei file sono <b>tutti minuscoli e senza spazi</b>: un indirizzo con gli spazi va
 * codificato a mano ({@code %20}) e ogni chiamante deve ricordarsene.
 * <p>Dal 18/08/2026 i manuali sono <b>pagine</b> e non più PDF: la sorgente è il Markdown in
 * {@code docs/documentazione/}, che GitHub Pages pubblica come {@code .html} con lo stesso nome. I PDF
 * restano pubblicati accanto alle pagine, rigenerati dallo stesso Markdown da
 * {@code docs/strumenti/genera-pdf.sh}, perché le versioni fino alla 1.0.61 li aprono a
 * quell'indirizzo e non devono trovare un 404 né un testo vecchio. Un nome qui va quindi cambiato
 * <b>solo</b> insieme al nome del file sorgente.
 *
 * @author luca.passelli
 */
public class DocumentiAiuto {

    /** Cartella pubblica dei manuali. Le copie da pubblicare stanno in {@code docs/documentazione/}. */
    static final String BASE = "https://lucapasselli.github.io/GiacenzeCrypto/documentazione/";

    public static final String OPZIONI_CALCOLO_RW = "opzioni-calcolo-rw.html";
    public static final String DISCLAIMER = "disclaimer.html";
    public static final String AVVERTENZE_PROBLEMI_NOTI = "avvertenze-problemi-noti.html";
    public static final String CALCOLO_PLUSVALENZE_OPZIONI = "calcolo-plusvalenze-opzioni.html";
    public static final String CLASSIFICAZIONI_MOVIMENTI = "classificazioni-movimenti.html";
    public static final String EXPORT_IMPORT_CSV = "export-import-csv.html";
    public static final String CREAZIONE_JSON_IMPORTAZIONI = "creazione-json-importazioni-personalizzate.html";

    /** Elenco delle novità di ogni versione: pagina sola, non ha una copia in PDF */
    public static final String NOVITA_VERSIONI = "changelog.html";

    /** Tutti i documenti, per le verifiche: un nome qui e nessun file pubblicato è un collegamento rotto */
    static final String[] TUTTI = {
        OPZIONI_CALCOLO_RW, DISCLAIMER, AVVERTENZE_PROBLEMI_NOTI, CALCOLO_PLUSVALENZE_OPZIONI,
        CLASSIFICAZIONI_MOVIMENTI, EXPORT_IMPORT_CSV, CREAZIONE_JSON_IMPORTAZIONI, NOVITA_VERSIONI
    };

    private DocumentiAiuto() {
    }

    /** @return l'indirizzo pubblico del documento indicato */
    static String Url(String Documento) {
        return BASE + Documento;
    }

    /**
     * Apre il documento nel browser predefinito.
     * @param Documento una delle costanti di questa classe
     */
    public static void Apri(String Documento) {
        Funzioni.ApriWeb(Url(Documento));
    }
}
