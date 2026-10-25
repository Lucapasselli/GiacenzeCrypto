package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Tiene lo schema pubblicato delle configurazioni di import ({@code docs/schema/configurazione-import.schema.json})
 * allineato a quello che {@code ConfigurazioneImport.carica} legge davvero. Lo schema e' quello che si passa a
 * un'IA, o a un editor, per scrivere una configurazione: una chiave che manca li' non verra' mai proposta, una
 * chiave che c'e' solo li' verrebbe proposta e poi ignorata.
 * <ul>
 * <li>ogni oggetto a chiavi fisse dello schema ha esattamente le chiavi di {@code CHIAVI_NOTE} per lo stesso percorso;</li>
 * <li>le configurazioni distribuite ({@code config/import}, {@code ImportConfig}) rispettano lo schema;</li>
 * <li>i tipi interni ammessi comprendono tutti quelli che il programma tratta in modo proprio.</li>
 * </ul>
 */
class SchemaConfigImportTest {

    private static final Path SCHEMA = Path.of("docs/schema/configurazione-import.schema.json");

    private static JSONObject schemaGrezzo() throws Exception {
        return new JSONObject(Files.readString(SCHEMA, StandardCharsets.UTF_8));
    }

    private static Schema schema() throws Exception {
        return SchemaLoader.load(schemaGrezzo());
    }

    /** Gli oggetti a chiavi fisse dello schema (additionalProperties false), per percorso nella forma di CHIAVI_NOTE */
    private static void oggettiFissi(JSONObject nodo, String percorso, Map<String, Set<String>> trovati) {
        if (nodo.has("properties") && Boolean.FALSE.equals(nodo.opt("additionalProperties"))) {
            trovati.put(percorso, new TreeSet<>(nodo.getJSONObject("properties").keySet()));
        }
        JSONObject proprieta = nodo.optJSONObject("properties");
        if (proprieta == null) return;
        for (String chiave : proprieta.keySet()) {
            JSONObject figlio = proprieta.getJSONObject(chiave);
            String sotto = percorso.isEmpty() ? chiave : percorso + "." + chiave;
            oggettiFissi(figlio, sotto, trovati);
            JSONObject elementi = figlio.optJSONObject("items");
            if (elementi != null) oggettiFissi(elementi, sotto + "[]", trovati);
        }
    }

    @Test
    void loSchemaHaLeStesseChiaviDiCaricaAOgniLivello() throws Exception {
        Map<String, Set<String>> nelloSchema = new TreeMap<>();
        oggettiFissi(schemaGrezzo(), "", nelloSchema);
        Map<String, Set<String>> nelCodice = new TreeMap<>();
        ImportazioneGenerica.ConfigurazioneImport.CHIAVI_NOTE.forEach((k, v) -> nelCodice.put(k, new TreeSet<>(v)));
        assertEquals(nelCodice, nelloSchema,
                "docs/schema/configurazione-import.schema.json e CHIAVI_NOTE non elencano le stesse chiavi");
    }

    @Test
    void ogniChiaveDellaRadiceHaUnaDescrizione() throws Exception {
        //La descrizione e' la sola documentazione che un'IA legge insieme allo schema
        JSONObject proprieta = schemaGrezzo().getJSONObject("properties");
        List<String> senza = new ArrayList<>();
        for (String chiave : proprieta.keySet()) {
            if (proprieta.getJSONObject(chiave).optString("description").isBlank()) senza.add(chiave);
        }
        assertEquals(List.of(), senza, "chiavi dello schema senza description");
    }

    @Test
    void iRimandiAlManualeEsistono() throws Exception {
        //Le descrizioni finiscono con "#ancora" della sezione del manuale: una sezione rinominata li renderebbe muti
        String manuale = Files.readString(Path.of("docs/documentazione/creazione-json-importazioni-personalizzate.md"),
                StandardCharsets.UTF_8);
        Set<String> ancore = new TreeSet<>();
        Matcher a = Pattern.compile("\\{#([^}]+)\\}").matcher(manuale);
        while (a.find()) ancore.add(a.group(1));
        Set<String> mancanti = new TreeSet<>();
        Matcher r = Pattern.compile("(?<=\\s|^)#([a-z0-9-]+)").matcher(schemaGrezzo().toString());
        int trovati = 0;
        while (r.find()) {
            trovati++;
            if (!ancore.contains(r.group(1))) mancanti.add(r.group(1));
        }
        assertTrue(trovati > 30, "rimandi trovati: " + trovati);
        assertEquals(Set.of(), mancanti, "ancore citate dallo schema ma assenti dal manuale");
    }

    @Test
    void leConfigurazioniDistribuiteRispettanoLoSchema() throws Exception {
        Schema schema = schema();
        List<String> errori = new ArrayList<>();
        int controllate = 0;
        for (String cartella : List.of("config/import", "ImportConfig")) {
            File[] elenco = new File(cartella).listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
            assertNotNull(elenco, cartella);
            for (File f : elenco) {
                controllate++;
                try {
                    schema.validate(new JSONObject(Files.readString(f.toPath(), StandardCharsets.UTF_8)));
                } catch (ValidationException ex) {
                    errori.add(cartella + "/" + f.getName() + ": " + String.join(" | ", ex.getAllMessages()));
                }
            }
        }
        assertTrue(controllate > 20, "trovate solo " + controllate + " configurazioni");
        assertTrue(errori.isEmpty(), String.join("\n", errori));
    }

    @Test
    void iTipiInterniAmmessiComprendonoQuelliTrattatiDalProgramma() throws Exception {
        JSONArray elenco = schemaGrezzo().getJSONObject("definitions").getJSONObject("tipoInterno").getJSONArray("enum");
        Set<String> ammessi = new TreeSet<>();
        for (int i = 0; i < elenco.length(); i++) ammessi.add(elenco.getString(i));
        //Lo schema e l'avviso del resoconto (TipiSconosciuti) devono dire la stessa cosa
        assertEquals(new TreeSet<>(ImportazioneGenerica.ConfigurazioneImport.TIPI_INTERNI), ammessi,
                "tipoInterno dello schema e ConfigurazioneImport.TIPI_INTERNI");
        Set<String> attesi = new TreeSet<>(MovimentiCrypto.TipologieMovimento.keySet());
        //Trattati a parte da ImportazioneGenerica
        attesi.addAll(Set.of("IGNORA", "NON CONSIDERARE", "SCAMBIO DIFFERITO"));
        attesi.removeAll(ammessi);
        assertEquals(Set.of(), attesi, "tipi interni che il programma riconosce ma lo schema rifiuta");
    }

    @Test
    void iCampiDellAutoDetectSonoQuelliDelCodice() throws Exception {
        String sorgente = Files.readString(Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto/ImportazioneGenerica.java"),
                StandardCharsets.UTF_8);
        Set<String> nelCodice = new TreeSet<>();
        Matcher m = Pattern.compile("case \"(\\w+)\"\\s*->\\s*colonna\\w+\\s*=\\s*idx").matcher(sorgente);
        while (m.find()) nelCodice.add(m.group(1));
        assertTrue(nelCodice.size() >= 10, "il sorgente non e' stato letto come previsto: " + nelCodice);

        JSONArray elenco = schemaGrezzo().getJSONObject("properties").getJSONObject("mappaAutoDetect")
                .getJSONObject("additionalProperties").getJSONArray("enum");
        Set<String> nelloSchema = new TreeSet<>();
        for (int i = 0; i < elenco.length(); i++) nelloSchema.add(elenco.getString(i).toLowerCase());
        assertEquals(nelCodice, nelloSchema);
    }

    @Test
    void gliErroriTipiciVengonoRifiutati() throws Exception {
        Schema schema = schema();
        String base = "{\"nomeExchange\": \"Prova\", \"colonne\": {\"data\": 0, \"causale\": 1, \"moneta\": 2, \"quantita\": 3},";
        //Una configurazione corretta passa, commenti e $schema compresi
        schema.validate(new JSONObject(base + "\"_nota\": \"x\", \"$schema\": \"https://example.org/s.json\","
                + "\"mappaCausali\": {\"Deposit\": \"TRASFERIMENTO-CRYPTO\"}}"));

        List<String> sbagliate = List.of(
                base + "\"mappaCausali\": {\"Interest\": \"reward\"}}",            //tipo in minuscolo: diventerebbe un movimento generico
                base + "\"mappaCausali\": {\"Deposit\": \"DEPOSITO CRYPTO\"}}",    //tipo inventato
                base + "\"causaliChiuse\": [\"Deposit\"]}",                       //causale CSV dove va un tipo interno
                base + "\"mappaColonne\": {}}",                                    //chiave inventata
                "{\"colonne\": {\"data\": \"0\"}}",                                //indice scritto come testo
                "{\"colonne\": {\"importo\": 3}}",                                 //campo inesistente
                "{\"regoleRiga\": [{\"colonna\": 1, \"uguale\": \"a\", \"contiene\": \"b\", \"scarta\": true}]}",
                "{\"regoleRiga\": [{\"colonna\": 1, \"uguale\": \"a\", \"scarta\": true, \"imposta\": {\"colonna\": 2, \"valore\": \"x\"}}]}",
                "{\"versoForzato\": \"uscita\"}",
                "{\"oraSeSoloData\": \"0:00\"}");
        for (String json : sbagliate) {
            assertThrows(ValidationException.class, () -> schema.validate(new JSONObject(json)), json);
        }
    }
}
