package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa le due protezioni delle installazioni rimaste indietro rispetto alle config scaricate da {@code master}:
 * <ul>
 * <li>{@code ConfigurazioneImport.CHIAVI_NOTE}: le chiavi che questa versione non legge finiscono nel resoconto
 *     dell'import invece di essere ignorate in silenzio;</li>
 * <li>{@code Funzioni.TrattieniCopiaLocale}: la sincronizzazione non sovrascrive una copia leggibile con un file
 *     che dichiara una {@code versioneMinimaApp} piu' recente.</li>
 * </ul>
 */
class ChiaviConfigImportTest {

    private String versioneOriginale;

    @BeforeEach
    void salvaVersione() {
        versioneOriginale = VarStatiche.Versione;
        VarStatiche.Versione = "1.0.65.07";
    }

    @AfterEach
    void ripristinaVersione() {
        VarStatiche.Versione = versioneOriginale;
        Importazioni.AzzeraContatori();
    }

    @Test
    void ogniConfigurazioneDistribuitaUsaSoloChiaviNote() throws Exception {
        //Fallisce quando una config usa una chiave che carica() legge ma CHIAVI_NOTE non elenca: va aggiunta li'
        List<String> errori = new ArrayList<>();
        for (String cartella : List.of("config/import", "ImportConfig")) {
            File[] elenco = new File(cartella).listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
            assertNotNull(elenco, cartella);
            for (File f : elenco) {
                JSONObject root = new JSONObject(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                List<String> sconosciute = ImportazioneGenerica.ConfigurazioneImport.ChiaviSconosciute(root);
                if (!sconosciute.isEmpty()) errori.add(cartella + "/" + f.getName() + ": " + sconosciute);
            }
        }
        assertTrue(errori.isEmpty(), String.join("\n", errori));
    }

    @Test
    void leChiaviDellaRadiceSonoQuelleCheCaricaLegge() throws Exception {
        //Il confronto con il sorgente tiene allineato l'elenco anche per le chiavi che nessuna config usa ancora
        String sorgente = Files.readString(Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto/ImportazioneGenerica.java"),
                StandardCharsets.UTF_8);
        Set<String> lette = new TreeSet<>();
        Matcher m = Pattern.compile("\\broot\\.(?:has|opt\\w*|get\\w*)\\(\"(\\w+)\"").matcher(sorgente);
        while (m.find()) lette.add(m.group(1));
        assertTrue(lette.size() > 40, "il sorgente non e' stato letto come previsto: " + lette);

        Set<String> note = new TreeSet<>(ImportazioneGenerica.ConfigurazioneImport.CHIAVI_NOTE.get(""));
        Set<String> nonElencate = new TreeSet<>(lette);
        nonElencate.removeAll(note);
        assertEquals(Set.of(), nonElencate, "lette da carica() ma assenti da CHIAVI_NOTE");

        note.removeAll(lette);
        //Metadati delle config, che carica() non ha bisogno di leggere
        assertEquals(Set.of("$schema", "versione", "noteRevisione"), note, "in CHIAVI_NOTE ma non lette da carica()");
    }

    @Test
    void chiaviInventateRiconosciuteAncheAnnidate() {
        JSONObject root = new JSONObject("""
                {
                  "_commento": "ignorato",
                  "nomeExchange": "Prova",
                  "chiaveDelFuturo": true,
                  "colonne": {"data": 0, "colonnna": 3},
                  "coppia": {"colonna": 1, "base": 0, "quote": 1,
                             "importoQuote": {"colonna": 2, "quantita": 0, "prezzo": 1, "commissione": 4}},
                  "regoleRiga": [{"colonna": 1, "uguale": "x", "imposta": {"colonna": 2, "valore": "y", "_nota": "ok"}},
                                 {"colonna": 1, "vuota": true, "scarta": true, "seAnche": 3}],
                  "mappaCausali": {"Qualunque causale": "DEPOSITO CRYPTO"},
                  "tipoSeCrypto": {"Purchase": "VC"}
                }
                """);
        assertEquals(List.of("chiaveDelFuturo", "colonne.colonnna", "coppia.importoQuote.commissione", "regoleRiga[].seAnche"),
                ImportazioneGenerica.ConfigurazioneImport.ChiaviSconosciute(root));
    }

    @Test
    void caricaLeRegistraEIlResocontoLeNomina(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("prova.json");
        Files.writeString(f, "{\"nomeExchange\": \"Prova\", \"colonne\": {\"data\": 0, \"moneta\": 1, \"quantita\": 2},"
                + " \"tipoSeOggi\": {\"Buy\": \"VC\"}}", StandardCharsets.UTF_8);
        ImportazioneGenerica.ConfigurazioneImport cfg = ImportazioneGenerica.ConfigurazioneImport.carica(f.toString());
        assertEquals(List.of("tipoSeOggi"), cfg.chiaviSconosciute);

        Importazioni.AzzeraContatori();
        assertEquals("", Importazioni.TestoChiaviConfigSconosciute());
        Importazioni.ChiaviConfigSconosciute.addAll(cfg.chiaviSconosciute);
        String testo = Importazioni.TestoChiaviConfigSconosciute();
        assertTrue(testo.contains("tipoSeOggi") && testo.contains("1.0.65.07"), testo);
        //Un avviso, non un errore: il riquadro dei movimenti non importati resta vuoto
        assertEquals("", Importazioni.movimentiSconosciuti);
        Importazioni.AzzeraContatori();
        assertEquals("", Importazioni.TestoChiaviConfigSconosciute());
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void sincronizzazioneTieneLaCopiaLocaleSeIlFileNuovoRichiedeUnaVersioneSuccessiva() {
        String locale = "{\"nomeExchange\": \"Bybit\", \"versione\": \"1.003\"}";
        String nuovo = "{\"nomeExchange\": \"Bybit\", \"versione\": \"1.004\", \"versioneMinimaApp\": \"1.0.66\"}";
        assertTrue(Funzioni.TrattieniCopiaLocale(b(nuovo), b(locale)));

        //File nuovo leggibile da questa versione: si scarica come sempre
        assertFalse(Funzioni.TrattieniCopiaLocale(b("{\"versioneMinimaApp\": \"1.0.65.07\"}"), b(locale)));
        assertFalse(Funzioni.TrattieniCopiaLocale(b("{\"versione\": \"1.004\"}"), b(locale)));
        //Copia locale illeggibile, o anche lei troppo nuova: non vale la pena tenerla
        assertFalse(Funzioni.TrattieniCopiaLocale(b(nuovo), b("{ rotto")));
        assertFalse(Funzioni.TrattieniCopiaLocale(b(nuovo), b("{\"versioneMinimaApp\": \"1.0.66\"}")));
        //Versione non interpretabile (build senza filtering): non blocca nulla, come VersioneAppAlmeno
        VarStatiche.Versione = "sconosciuta";
        assertFalse(Funzioni.TrattieniCopiaLocale(b(nuovo), b(locale)));
    }
}
