package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Gli esempi completi del manuale delle configurazioni ({@code docs/documentazione/creazione-json-importazioni-personalizzate.md})
 * passano davvero per l'import. Sono il testo che un'IA, o un utente, copia alla lettera: fino al 2026-10-25 l'esempio
 * con la sola data faceva scartare tutte le righe e quello di Binance leggeva la coppia come moneta.
 * <p>Per ogni esempio il CSV del blocco {@code text} e la configurazione del blocco {@code json} che lo segue: lo
 * schema pubblicato deve accettarla, nessuna riga deve essere scartata e le categorie dei movimenti devono essere
 * quelle attese, che il manuale descrive nella riga "Risultato".
 */
class EsempiManualeConfigImportTest {

    private static final Path MANUALE = Path.of("docs/documentazione/creazione-json-importazioni-personalizzate.md");

    /** Categorie attese (ultimo segmento dell'ID), in ordine alfabetico, per numero di esempio */
    private static final Map<String, List<String>> ATTESE = Map.of(
            "1", List.of("CM", "DC", "SC"),
            "2", List.of("DC", "PC", "RW"),
            "3", List.of("CM", "DC", "PC", "SC"),
            "4", List.of("RW", "SC"),
            "5", List.of("PC", "RW", "RW"));

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        //Un movimento senza controvalore fa consultare la cache dei prezzi: serve il DB
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        Funzioni.ReteDisabilitataPerTest = true;
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        Importazioni.AzzeraContatori();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Numero dell'esempio -> {csv, json} */
    private static Map<String, String[]> esempi() throws Exception {
        String testo = Files.readString(MANUALE, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int inizio = testo.indexOf("## Esempi completi");
        int fine = testo.indexOf("\n## ", inizio + 1);
        assertTrue(inizio > 0 && fine > inizio, "sezione degli esempi non trovata");
        Map<String, String[]> esempi = new LinkedHashMap<>();
        Matcher titolo = Pattern.compile("(?m)^### Esempio (\\d+)").matcher(testo);
        List<int[]> posizioni = new ArrayList<>();
        List<String> numeri = new ArrayList<>();
        while (titolo.find()) {
            if (titolo.start() < inizio || titolo.start() > fine) continue;
            posizioni.add(new int[]{titolo.start()});
            numeri.add(titolo.group(1));
        }
        for (int i = 0; i < numeri.size(); i++) {
            int a = posizioni.get(i)[0];
            int b = i + 1 < numeri.size() ? posizioni.get(i + 1)[0] : fine;
            String parte = testo.substring(a, b);
            Matcher csv = Pattern.compile("```text\\n(.*?)```", Pattern.DOTALL).matcher(parte);
            Matcher json = Pattern.compile("```json\\n(.*?)```", Pattern.DOTALL).matcher(parte);
            assertTrue(csv.find() && json.find(), "esempio " + numeri.get(i) + " senza blocco text o json");
            esempi.put(numeri.get(i), new String[]{csv.group(1), json.group(1)});
        }
        return esempi;
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    @Test
    void gliEsempiDelManualeSiImportanoSenzaScarti() throws Exception {
        Schema schema = SchemaLoader.load(new JSONObject(Files.readString(
                Path.of("docs/schema/configurazione-import.schema.json"), StandardCharsets.UTF_8)));
        Map<String, String[]> esempi = esempi();
        assertEquals(ATTESE.keySet(), esempi.keySet(), "esempi del manuale e attese del test non coincidono");

        for (Map.Entry<String, String[]> e : esempi.entrySet()) {
            String n = e.getKey();
            schema.validate(new JSONObject(e.getValue()[1]));

            Path csv = tempDir.resolve("esempio" + n + ".csv");
            Path json = tempDir.resolve("esempio" + n + ".json");
            Files.writeString(csv, e.getValue()[0], StandardCharsets.UTF_8);
            Files.writeString(json, e.getValue()[1], StandardCharsets.UTF_8);
            ConfigurazioneImport cfg = ConfigurazioneImport.carica(json.toString());
            assertEquals(List.of(), cfg.chiaviSconosciute, "esempio " + n);

            Importazioni.AzzeraContatori();
            List<String[]> righe = ImportazioneGenerica.leggiCSV(csv.toString(), cfg);
            righe = ImportazioneGenerica.scartaAnnullamenti(righe, cfg);
            righe = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, cfg);
            List<String[]> movimenti = new ArrayList<>();
            for (List<String[]> gruppo : ImportazioneGenerica.raggruppaRighe(righe, cfg)) {
                movimenti.addAll(ImportazioneGenerica.consolidaGruppo(gruppo, cfg, new ArrayList<>()));
            }
            assertEquals("", Importazioni.movimentiSconosciuti, "esempio " + n + ": righe scartate");
            assertEquals(ATTESE.get(n), movimenti.stream().map(EsempiManualeConfigImportTest::categoria).sorted().toList(),
                    "esempio " + n);
        }
    }
}
