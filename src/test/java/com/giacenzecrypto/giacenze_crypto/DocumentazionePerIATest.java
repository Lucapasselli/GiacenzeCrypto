package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * {@code docs/llms.txt} e {@code docs/llms-full.txt}, i due file per le IA pubblicati con i manuali: il primo elenca
 * tutti i manuali, il secondo e' la loro copia rigenerata e non deve restare indietro (come i PDF, si rigenera con
 * {@code docs/strumenti/genera-pdf.sh}).
 */
public class DocumentazionePerIATest {

    private static final File DOCS = new File("docs");

    @Test
    public void llmsTxtElencaTuttiIManuali() throws Exception {
        Set<String> manuali = new TreeSet<>();
        for (File f : new File(DOCS, "documentazione").listFiles((d, n) -> n.endsWith(".md"))) {
            String nome = f.getName().replace(".md", "");
            if (!nome.equals("index") && !nome.equals("changelog")) manuali.add(nome);
        }
        List<String> elencati = DocumentazionePerIA.Manuali(DOCS);
        assertEquals(manuali, new TreeSet<>(elencati), "manuali di docs/documentazione e collegamenti di docs/llms.txt");
        assertEquals(elencati.size(), new TreeSet<>(elencati).size(), "manuale elencato due volte");
    }

    @Test
    public void iCollegamentiDiLlmsTxtPortanoAFilePubblicati() throws Exception {
        String indice = Files.readString(new File(DOCS, "llms.txt").toPath(), StandardCharsets.UTF_8);
        String sito = DocumentiAiuto.BASE.replace("documentazione/", "");
        int trovati = 0;
        for (String parte : indice.split("\\(" + java.util.regex.Pattern.quote(sito))) {
            if (trovati++ == 0) continue;
            String percorso = parte.substring(0, parte.indexOf(')'));
            File f = new File(DOCS, percorso.endsWith(".html") ? percorso.replace(".html", ".md") : percorso);
            assertTrue(f.isFile(), "docs/llms.txt collega " + percorso + ", che non e' pubblicato");
        }
        assertTrue(trovati > 8, "collegamenti trovati: " + trovati);
    }

    @Test
    public void llmsFullTxtEAggiornato() throws Exception {
        String pubblicato = Files.readString(new File(DOCS, "llms-full.txt").toPath(), StandardCharsets.UTF_8);
        assertEquals(DocumentazionePerIA.TestoCompleto(DOCS), pubblicato,
                "docs/llms-full.txt non corrisponde ai manuali: rigenerarlo con sh docs/strumenti/genera-pdf.sh");
    }
}
