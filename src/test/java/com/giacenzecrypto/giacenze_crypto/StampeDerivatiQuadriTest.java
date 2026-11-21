package com.giacenzecrypto.giacenze_crypto;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I redditi da derivati sui moduli T/RT ({@link Stampe}, 19/11/2026): ogni modello nuovo esiste, non è cifrato, e i valori
 * finiscono nel PDF. Le posizioni sono state controllate a vista sui PDF generati (la corrispondenza fra coordinate del modulo e
 * punto di stampa è nei commenti di {@link Stampe#AggiungiRT2025}). Dati inventati.
 */
class StampeDerivatiQuadriTest {

    @TempDir
    Path dir;

    private static String[] mov(String data, String ris) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = data.replace("-", "") + "100000_OKX_001_001_DC";
        v[1] = data + " 10:00";
        v[3] = "OKX";
        v[Derivati.CAMPO] = Derivati.PNL;
        v[Derivati.CAMPO_RISULTATO] = ris;
        return v;
    }

    private static String testo(File f) throws Exception {
        PdfReader r = new PdfReader(Files.readAllBytes(f.toPath()));
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor ex = new PdfTextExtractor(r);
        for (int p = 1; p <= r.getNumberOfPages(); p++) sb.append(ex.getTextFromPage(p)).append('\n');
        r.close();
        return sb.toString();
    }

    @Test
    void iModelliNuoviCiSonoENonSonoCifrati() throws Exception {
        for (String m : List.of("QuadroRT_2023.pdf", "QuadroRT_2024_2.pdf", "QuadroT_2024_1.pdf", "QuadroT_2025_1.pdf")) {
            File f = new File("Immagini/" + m);
            assertTrue(f.isFile(), m);
            PdfReader r = new PdfReader(Files.readAllBytes(f.toPath()));
            assertFalse(r.isEncrypted(), m + " cifrato: senza Bouncy Castle la stampa fallirebbe");
            assertEquals(1, r.getNumberOfPages(), m);
            r.close();
        }
    }

    @Test
    void iValoriDeiDerivatiFinisconoSuiModuliDiOgniAnno() throws Exception {
        for (String anno : List.of("2023", "2024", "2025")) {
            Principale_RT_Derivati.Esito d = Principale_RT_Derivati.Calcola(anno, List.of(mov(anno + "-03-01", "1385.00"), mov(anno + "-03-02", "-1412.00")));
            File rt = dir.resolve("RT_" + anno + ".pdf").toFile();
            Stampe s = new Stampe(rt.getPath());
            s.ApriDocumento();
            switch (anno) {
                case "2023" -> s.AggiungiRT2023("Immagini/QuadroRT_2023.pdf", "12345", "2345", "", anno, d);
                case "2024" -> {
                    s.AggiungiRT("Immagini/QuadroRT_2024.jpg", "12345", "2345", "", anno, d);
                    s.NuovaPagina();
                    s.AggiungiRT2025p2("Immagini/QuadroRT_2024_2.pdf", "12345", "2345", "", anno, d);
                }
                default -> {
                    s.AggiungiRT2025("Immagini/QuadroRT_2025.pdf", "12345", "2345", "", anno, d);
                    s.NuovaPagina();
                    s.AggiungiRT2025p2("Immagini/QuadroRT_2025_2.pdf", "12345", "2345", "", anno, d);
                }
            }
            s.ScriviPDF(false);
            String t = testo(rt);
            //colonna 1, colonna 2 e la perdita (risultato e rigo delle perdite)
            assertTrue(t.contains("1.385") && t.contains("1.412"), anno + ": " + t);
            assertTrue(t.contains("27"), anno);

            if (!anno.equals("2023")) {
                File tf = dir.resolve("T_" + anno + ".pdf").toFile();
                Stampe st = new Stampe(tf.getPath());
                st.ApriDocumento();
                st.AggiungiTSezioneII("Immagini/" + (anno.equals("2024") ? "QuadroT_2024_1.pdf" : "QuadroT_2025_1.pdf"), d, "");
                st.ScriviPDF(false);
                String tt = testo(tf);
                assertTrue(tt.contains("1.385") && tt.contains("1.412"), anno + " T: " + tt);
            }
        }
    }
}
