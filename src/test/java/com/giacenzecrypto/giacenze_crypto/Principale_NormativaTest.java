package com.giacenzecrypto.giacenze_crypto;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Principale_Normativa}: lettura di {@code fonti.csv}, mappatura delle categorie,
 * estrazione del testo per la ricerca (PDF/XML/Markdown) e sua cache in {@code NORMATIVA_TESTO}.
 *
 * <p>Gira su una working directory temporanea (come {@code DocumentiFonteTest}): {@code Normativa/}
 * viene quindi creata lì, mai nell'archivio vero dell'utente.
 */
class Principale_NormativaTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    // ------------------------------------------------------------------ fonti.csv -> catalogo

    @Test
    void leggeIlCatalogoDaFontiCsv() throws Exception {
        Path normativa = tempDir.resolve("Normativa");
        Files.createDirectories(normativa);
        //Riga "ufficiale" con tutti i campi valorizzati; riga "derivato" con url fra virgolette (il
        //solo campo che oggi contiene il delimitatore ';', per i consolidati che elencano più file);
        //riga con data_documento/argomenti mancanti (tollerato, non SCONOSCIUTO); riga SCONOSCIUTO.
        String csv = String.join("\n",
                "file;tipo;titolo;autorita;identificativo;data_documento;argomenti;url;scaricato_il;sha256;byte",
                "Prassi_AgenziaEntrate/circolare.pdf;ufficiale;Circolare di prova;Agenzia delle entrate;"
                        + "Circolare n. 1/E;2023-10-27;plusvalenze, quadro RW;https://esempio.it/circolare.pdf;"
                        + "2026-08-21;abc123;100",
                "Leggi/Consolidato/prova_evoluzione.md;derivato;Evoluzione di prova;accostamento;"
                        + "NON UFFICIALE;2024-12-30;aliquota;"
                        + "\"Leggi/Originale/a.xml; Leggi/Originale/b.xml\";;def456;50",
                "Istruzioni_Dichiarazioni/istruzioni.pdf;ufficiale;Istruzioni di prova;Agenzia delle entrate;"
                        + "Istruzioni 2023;;;https://esempio.it/istruzioni.pdf;2026-08-21;ghi789;200",
                "Misteriosa/file.pdf;SCONOSCIUTO;;;;;;;;jkl000;10") + "\n";
        Files.writeString(normativa.resolve("fonti.csv"), csv);

        List<DocumentoNormativa> catalogo = Principale_Normativa.LeggiCatalogo();

        assertEquals(4, catalogo.size());

        DocumentoNormativa circolare = catalogo.get(0);
        assertEquals("Prassi_AgenziaEntrate/circolare.pdf", circolare.percorso());
        assertTrue(circolare.ufficiale());
        assertEquals("Circolare di prova", circolare.titolo());
        assertEquals(LocalDate.of(2023, 10, 27), circolare.dataDocumento());
        assertEquals(List.of("plusvalenze", "quadro RW"), circolare.argomenti());
        assertEquals("Prassi Agenzia delle Entrate", circolare.categoria());
        assertEquals("abc123", circolare.sha256());

        DocumentoNormativa consolidato = catalogo.get(1);
        //il campo url era fra virgolette proprio perché contiene il delimitatore ';': il parser deve
        //restituirlo intero, non troncato al primo ';' che incontra
        assertEquals("Leggi/Originale/a.xml; Leggi/Originale/b.xml", consolidato.url());
        assertEquals("Leggi — consolidato", consolidato.categoria());
        assertFalse(consolidato.ufficiale());

        DocumentoNormativa istruzioni = catalogo.get(2);
        assertNull(istruzioni.dataDocumento(), "data_documento mancante deve restare null, non essere indovinata");
        assertTrue(istruzioni.argomenti().isEmpty());
        assertEquals("Istruzioni dichiarazioni", istruzioni.categoria());

        DocumentoNormativa sconosciuta = catalogo.get(3);
        assertEquals("SCONOSCIUTO", sconosciuta.tipo());
    }

    @Test
    void tornaListaVuotaSeLaCartellaNonEsisteAncora() {
        VarStatiche.setWorkingDirectory(tempDir.resolve("non-esiste").toString() + "/");
        try {
            assertTrue(Principale_Normativa.LeggiCatalogo().isEmpty());
        } finally {
            VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        }
    }

    // ------------------------------------------------------------------ categoria

    @Test
    void mappaLaCategoriaDalleDueCartelleSottoLeggi() {
        assertEquals("Leggi — testo originale",
                Principale_Normativa.Categoria("Leggi/Originale/LeggiBilancio/x.xml"));
        assertEquals("Leggi — estratti", Principale_Normativa.Categoria("Leggi/Estratti/NormeRichiamate/x.md"));
        assertEquals("Leggi — consolidato", Principale_Normativa.Categoria("Leggi/Consolidato/x.md"));
    }

    @Test
    void mappaLaCategoriaDallaSolaCartellaDiPrimoLivelloPerLeAltre() {
        assertEquals("Prassi Agenzia delle Entrate", Principale_Normativa.Categoria("Prassi_AgenziaEntrate/x.pdf"));
        assertEquals("Istruzioni dichiarazioni", Principale_Normativa.Categoria("Istruzioni_Dichiarazioni/x.pdf"));
        assertEquals("ISEE / DSU", Principale_Normativa.Categoria("ISEE_DSU/x.pdf"));
    }

    // ------------------------------------------------------------------ url apribile

    @Test
    void urlApribileSoloSeHttpOHttps() {
        DocumentoNormativa http = documento("a.pdf", "http://esempio.it/a.pdf", "sha1");
        DocumentoNormativa https = documento("b.pdf", "https://esempio.it/b.pdf", "sha2");
        DocumentoNormativa listaFile = documento("c.md", "Leggi/a.xml; Leggi/b.xml", "sha3");

        assertTrue(Principale_Normativa.UrlApribile(http));
        assertTrue(Principale_Normativa.UrlApribile(https));
        assertFalse(Principale_Normativa.UrlApribile(listaFile));
        assertFalse(Principale_Normativa.UrlApribile(null));
    }

    // ------------------------------------------------------------------ estrazione testo + cache

    @Test
    void estraeECacheaIlTestoMarkdownERiestraeSoloSeLoShaCambia() throws Exception {
        Path file = scriviFile("Prassi_AgenziaEntrate/cache_md.md", "Testo di prova sulle plusvalenze.");
        DocumentoNormativa v1 = documento("Prassi_AgenziaEntrate/cache_md.md", "", "sha-v1");

        assertEquals("Testo di prova sulle plusvalenze.", Principale_Normativa.TestoDocumento(v1));
        assertEquals("sha-v1", DatabaseH2.NormativaTesto_LeggiSha("Prassi_AgenziaEntrate/cache_md.md"));

        //il file cambia (come dopo un aggiornamento da GitHub) e con lui lo sha
        Files.writeString(file, "Nuovo contenuto, diverso dal precedente.");
        DocumentoNormativa v2 = documento("Prassi_AgenziaEntrate/cache_md.md", "", "sha-v2");

        assertEquals("Nuovo contenuto, diverso dal precedente.", Principale_Normativa.TestoDocumento(v2));
        assertEquals("sha-v2", DatabaseH2.NormativaTesto_LeggiSha("Prassi_AgenziaEntrate/cache_md.md"));

        //sha invariato e file sparito: la cache deve bastare da sola, senza toccare il disco
        Files.delete(file);
        assertEquals("Nuovo contenuto, diverso dal precedente.", Principale_Normativa.TestoDocumento(v2));
    }

    @Test
    void estraeIlTestoDaUnPdf() throws Exception {
        Path pdf = tempDir.resolve("Normativa/Istruzioni_Dichiarazioni/cache_pdf.pdf");
        Files.createDirectories(pdf.getParent());
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(pdf.toFile()));
        doc.open();
        doc.add(new Paragraph("Parola chiave PDFTEST dentro il documento."));
        doc.close();

        DocumentoNormativa d = documento("Istruzioni_Dichiarazioni/cache_pdf.pdf", "", "sha-pdf");

        assertTrue(Principale_Normativa.TestoDocumento(d).contains("PDFTEST"));
    }

    @Test
    void estraeIlTestoDaUnXmlIgnorandoITag() throws Exception {
        scriviFile("Leggi/Originale/LeggiBilancio/cache_xml.xml",
                "<?xml version=\"1.0\"?><root><section><para>Contiene la parola chiave XMLTEST qui dentro.</para></section></root>");

        DocumentoNormativa d = documento("Leggi/Originale/LeggiBilancio/cache_xml.xml", "", "sha-xml");

        String testo = Principale_Normativa.TestoDocumento(d);
        assertTrue(testo.contains("XMLTEST"));
        assertFalse(testo.contains("<para>"), "i tag non devono comparire nel testo estratto");
    }

    private static Path scriviFile(String percorsoRelativo, String contenuto) throws Exception {
        Path file = tempDir.resolve("Normativa").resolve(percorsoRelativo);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contenuto);
        return file;
    }

    private static DocumentoNormativa documento(String percorso, String url, String sha256) {
        return new DocumentoNormativa(percorso, "ufficiale", "Titolo di prova", "Autorità di prova",
                "Identificativo di prova", null, List.of(), url, sha256,
                Principale_Normativa.Categoria(percorso));
    }
}
