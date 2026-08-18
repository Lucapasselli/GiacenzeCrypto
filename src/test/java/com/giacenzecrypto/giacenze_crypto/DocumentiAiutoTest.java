package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tiene insieme le due metà del collegamento ai manuali: le costanti di {@link DocumentiAiuto} e i file
 * pubblicati in {@code docs/documentazione/}.
 * <p>Senza questa verifica un file rinominato non dà nessun errore: l'applicazione compila, il pulsante
 * si preme, e l'utente riceve un 404 dal sito. È lo stesso motivo per cui {@code LoghiImportTest}
 * controlla che i loghi delle voci native esistano davvero.
 */
public class DocumentiAiutoTest {

    /** Cartella pubblicata da GitHub Pages, dentro il repository */
    private static final File PUBBLICATI = new File("docs/documentazione");

    @Test
    public void ogniDocumentoDichiaratoEPubblicato() {
        //Le costanti nominano la pagina .html che GitHub Pages genera; nel repository c'è il .md
        for (String documento : DocumentiAiuto.TUTTI) {
            String sorgente = documento.replace(".html", ".md");
            assertTrue(new File(PUBBLICATI, sorgente).isFile(),
                    "manca docs/documentazione/" + sorgente + ": il collegamento darebbe 404");
        }
    }

    @Test
    public void iManualiRestanoDisponibiliAncheInPdf() {
        //Le versioni del programma rilasciate fino alla 1.0.61 aprono i manuali all'indirizzo .pdf: quei file devono
        //continuare a esistere, rigenerati dal Markdown con docs/strumenti/genera-pdf.sh
        for (String documento : DocumentiAiuto.TUTTI) {
            if (documento.equals(DocumentiAiuto.NOVITA_VERSIONI)) {
                continue;//pagina nuova, non è mai stata un PDF
            }
            String pdf = documento.replace(".html", ".pdf");
            assertTrue(new File(PUBBLICATI, pdf).isFile(),
                    "manca docs/documentazione/" + pdf + ": i collegamenti delle versioni precedenti darebbero 404");
        }
    }

    @Test
    public void iNomiDeiFileSonoIndirizziValidi() {
        //minuscoli e senza spazi: uno spazio andrebbe codificato %20 in ogni chiamata, ed è esattamente
        //il genere di cosa che si dimentica in uno dei nove punti che aprono un manuale
        for (String documento : DocumentiAiuto.TUTTI) {
            assertEquals(documento.toLowerCase(), documento, "il nome del file deve essere minuscolo");
            assertTrue(documento.matches("[a-z0-9._-]+"), "nome non adatto a un URL: " + documento);
            assertTrue(documento.endsWith(".html"), "il manuale non è una pagina del sito: " + documento);
        }
    }

    @Test
    public void iDocumentiStannoSulSitoDelProgettoENonSuSourceforge() {
        String url = DocumentiAiuto.Url(DocumentiAiuto.DISCLAIMER);
        assertTrue(url.startsWith("https://"), "il collegamento deve essere in https: " + url);
        assertTrue(url.contains("lucapasselli.github.io/GiacenzeCrypto"), url);
        assertTrue(url.endsWith("/" + DocumentiAiuto.DISCLAIMER), url);
    }

    @Test
    public void nessunoApreAncoraIManualiDaSourceforge() {
        //la sostituzione riguardava nove punti in due classi: se ne ricomparisse uno, il sito nuovo
        //resterebbe aggiornato e quel pulsante no
        for (String sorgente : new String[]{"Principale.java", "Importazioni_Gestione.java"}) {
            File f = new File("src/main/java/com/giacenzecrypto/giacenze_crypto", sorgente);
            assertTrue(f.isFile(), "non trovo " + f);
            String testo = LeggiTutto(f);
            assertTrue(!testo.contains("sourceforge.net/projects/giacenze-crypto-com/files/Documentazione"),
                    sorgente + " apre ancora un manuale da SourceForge: usare DocumentiAiuto.Apri()");
        }
    }

    private static String LeggiTutto(File f) {
        try {
            return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
