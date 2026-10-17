package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link Funzioni#Export_ScriviExcelDaTabella}: una cella {@code null} del model non deve far fallire
 * l'export. Il caso reale sono i dettagli RW, il cui model ha 18 colonne ("Stato estero" in coda) mentre
 * le righe CRYPTO hanno 17 campi: {@code DefaultTableModel.addRow} completa la riga con {@code null}, e
 * fino al 2026-10-16 il {@code toString()} su quella cella interrompeva l'export lasciando un file da 0 byte.
 */
class ExportExcelTabellaTest {

    @TempDir
    Path cartella;

    private static String contenuto(ZipFile zip, String nome) throws Exception {
        ZipEntry voce = zip.getEntry(nome);
        assertNotNull(voce, "manca " + nome + " nel file xlsx");
        try (InputStream in = zip.getInputStream(voce)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void rigaPiuCortaDelModelVieneEsportataConLaCellaVuota() throws Exception {
        String[] colonne = new String[18];
        for (int i = 0; i < colonne.length; i++) colonne[i] = "Col" + i;
        DefaultTableModel model = new DefaultTableModel(colonne, 0);
        String[] rigaCrypto = new String[17];
        for (int i = 0; i < rigaCrypto.length; i++) rigaCrypto[i] = "v" + i;
        rigaCrypto[2] = "<html>Mon. <br>Inizio</html>";
        model.addRow(rigaCrypto);
        assertNull(model.getValueAt(0, 17), "premessa: addRow completa la riga corta con null");

        JTable tabella = new JTable(model);
        tabella.setName("RW_Tabella_Dettagli");
        File f = cartella.resolve("Tabella.xlsx").toFile();

        Funzioni.Export_ScriviExcelDaTabella(tabella, f);

        assertTrue(f.length() > 0, "il file non deve essere vuoto");
        try (ZipFile zip = new ZipFile(f)) {
            String foglio = contenuto(zip, "xl/worksheets/sheet1.xml");
            String testi = contenuto(zip, "xl/sharedStrings.xml");
            assertTrue(foglio.contains("r=\"Q2\""), "la riga dati deve arrivare almeno alla colonna 17");
            assertTrue(testi.contains("v16"), "l'ultimo campo valorizzato deve essere esportato");
            assertTrue(testi.contains("Mon. Inizio"), "l'HTML delle celle va ripulito");
            assertTrue(testi.contains("Col17"), "l'intestazione della colonna in piu' resta");
        }
    }
}
