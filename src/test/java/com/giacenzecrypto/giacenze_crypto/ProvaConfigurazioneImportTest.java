package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La prova di una configurazione ({@link ProvaConfigurazioneImport}): produce i movimenti dell'importazione vera senza
 * scriverli e senza cercare prezzi, e dice cosa non va. Piu' l'avviso sui tipi interni sconosciuti
 * ({@code ConfigurazioneImport.TipiSconosciuti}), che vale anche per l'importazione vera.
 */
class ProvaConfigurazioneImportTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        Funzioni.ReteDisabilitataPerTest = true;
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static final String CSV_BINANCE = """
            "User_ID","UTC_Time","Account","Operation","Coin","Change","Remark"
            "1","2025-03-10 14:22:01","Spot","Transaction Buy","BTC","0.01",""
            "1","2025-03-10 14:22:01","Spot","Transaction Spend","USDT","-620.50",""
            "1","2025-03-10 14:22:01","Spot","Transaction Fee","BTC","-0.00001",""
            "1","2025-03-12 09:05:44","Spot","Deposit","ETH","0.5",""
            "1","2025-03-13 10:00:00","Spot","Mistero","ETH","0.1",""
            """;

    private static final String CFG_BINANCE = """
            {
            "nomeExchange": "Binance", "nomeWallet": "Principale",
            "separatore": ",", "formatoData": "yyyy-MM-dd HH:mm:ss", "fuso": "UTC",
            "colonne": { "data": 1, "causale": 3, "moneta": 4, "quantita": 5 },
            "consolidaRigheStessaData": true, "tolleranzaSecondiConsolidamento": 1,
            "mappaCausali": {
              "Transaction Buy": "SCAMBIO CRYPTO-CRYPTO", "Transaction Spend": "SCAMBIO CRYPTO-CRYPTO",
              "Transaction Fee": "COMMISSIONI", "Deposit": "TRASFERIMENTO-CRYPTO"
            },
            "causaliChiuse": ["TRASFERIMENTO-CRYPTO", "COMMISSIONI"]
            }
            """;

    private static File scrivi(String nome, String testo) throws Exception {
        Path f = tempDir.resolve(nome);
        Files.writeString(f, testo, StandardCharsets.UTF_8);
        return f.toFile();
    }

    @Test
    void produceIMovimentiSenzaScriverli() throws Exception {
        int primaInArchivio = Principale.MappaCryptoWallet.size();
        ProvaConfigurazioneImport.Esito e = ProvaConfigurazioneImport.Esegui(scrivi("cfg.json", CFG_BINANCE),
                scrivi("binance.csv", CSV_BINANCE));

        assertNull(e.errore);
        assertEquals(5, e.righeLette);
        assertEquals(List.of("CM", "DC", "SC"), e.movimenti.stream().map(ProvaConfigurazioneImport.Movimento::categoria).sorted().toList());
        assertEquals(1, e.scarti.size());
        assertTrue(e.scarti.get(0).startsWith("CAUSALE SCONOSCIUTA: Mistero"), e.scarti.get(0));
        assertTrue(e.ciSonoProblemi());

        //Il gruppo dello scambio, con le sue tre causali, produce lo scambio e la commissione
        ProvaConfigurazioneImport.Riepilogo scambio = e.riepilogo.stream()
                .filter(r -> r.causali().equals("Transaction Buy + Transaction Fee + Transaction Spend")).findFirst().orElseThrow();
        assertEquals(1, scambio.gruppi());
        assertEquals("CM x1, SC x1", scambio.testoCategorie());

        ProvaConfigurazioneImport.Saldo btc = e.saldi.stream().filter(s -> s.moneta().equals("BTC")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("0.00999").compareTo(btc.saldo()), btc.toString());

        //Niente in archivio, contatori dell'importazione lasciati puliti
        assertEquals(primaInArchivio, Principale.MappaCryptoWallet.size());
        assertEquals("", Importazioni.movimentiSconosciuti);
        assertEquals(0, Importazioni.TrasazioniSconosciute);

        String testo = e.TestoPerIA();
        assertTrue(testo.contains("RIGHE SCARTATE") && testo.contains("CAUSALE SCONOSCIUTA: Mistero"), testo);
        assertTrue(testo.contains("Transaction Buy + Transaction Fee + Transaction Spend"), testo);
    }

    @Test
    void segnalaGliErroriTipiciDiUnaConfigurazione() throws Exception {
        String cfg = CFG_BINANCE
                .replace("\"Deposit\": \"TRASFERIMENTO-CRYPTO\"", "\"Deposit\": \"deposito crypto\", \"Mistero\": \"reward\"")
                .replace("\"causaliChiuse\": [\"TRASFERIMENTO-CRYPTO\", \"COMMISSIONI\"]",
                        "\"causaliChiuse\": [\"Deposit\", \"COMMISSIONI\", \"ignora\"], \"colonnne\": {}");
        ProvaConfigurazioneImport.Esito e = ProvaConfigurazioneImport.Esegui(scrivi("cfg2.json", cfg),
                scrivi("binance2.csv", CSV_BINANCE));
        assertNull(e.errore);
        assertEquals(List.of("colonnne"), e.chiaviSconosciute);
        //"ignora" in minuscolo funziona: il codice confronta IGNORA senza maiuscole
        assertEquals(List.of("mappaCausali: Deposit -> deposito crypto", "mappaCausali: Mistero -> reward", "causaliChiuse: Deposit"),
                e.tipiSconosciuti);
        assertTrue(e.TestoPerIA().contains("mappaCausali: Mistero -> reward"));
    }

    @Test
    void unaDataSenzaOraScartaTutteLeRighe() throws Exception {
        String csv = "Data,Causale,Moneta,Quantita\n2025-01-10,Interessi,USDC,0.5\n";
        String cfg = "{\"nomeExchange\":\"X\",\"formatoData\":\"yyyy-MM-dd\",\"colonne\":{\"data\":0,\"causale\":1,\"moneta\":2,\"quantita\":3},"
                + "\"mappaCausali\":{\"Interessi\":\"EARN\"}}";
        ProvaConfigurazioneImport.Esito e = ProvaConfigurazioneImport.Esegui(scrivi("cfg3.json", cfg), scrivi("solodata.csv", csv));
        assertEquals(0, e.righeLette);
        assertTrue(e.scarti.get(0).startsWith("DATA NON PARSABILE"), e.scarti.toString());
    }

    @Test
    void unJsonMalScrittoDiceDove() throws Exception {
        ProvaConfigurazioneImport.Esito e = ProvaConfigurazioneImport.Esegui(
                scrivi("rotto.json", "{\n\"nomeExchange\": \"X\",\n\"colonne\": {\"data\": 0,}\n,,}"), scrivi("x.csv", CSV_BINANCE));
        assertNotNull(e.errore);
        assertTrue(e.errore.contains("line"), e.errore);
        assertTrue(e.TestoPerIA().contains("ERRORE"));

        ProvaConfigurazioneImport.Esito r = ProvaConfigurazioneImport.Esegui(scrivi("regola.json",
                "{\"regoleRiga\":[{\"colonna\":1,\"uguale\":\"a\",\"contiene\":\"b\",\"scarta\":true}]}"), scrivi("y.csv", CSV_BINANCE));
        assertTrue(r.errore != null && r.errore.contains("regoleRiga"), String.valueOf(r.errore));
    }

    @Test
    void ilFusoSegueLaRegolaDellImportazione() {
        assertEquals("UTC", ProvaConfigurazioneImport.FusoPerFile("UTC", "export (UTC+2).csv"));
        assertEquals("UTC+2", ProvaConfigurazioneImport.FusoPerFile("UTC?", "export (UTC+2).csv"));
        assertEquals("UTC", ProvaConfigurazioneImport.FusoPerFile("UTC?", "export.csv"));
        assertEquals("UTC+2", ProvaConfigurazioneImport.FusoPerFile("", "export (UTC+2).csv"));
        assertNull(ProvaConfigurazioneImport.FusoPerFile("", "export.csv"));
    }

    @Test
    void leConfigurazioniDistribuiteNonHannoTipiSconosciuti() throws Exception {
        //L'avviso compare anche nell'importazione vera: nessun falso allarme sulle configurazioni del programma
        List<String> errori = new ArrayList<>();
        for (String cartella : List.of("config/import", "ImportConfig")) {
            for (File f : new File(cartella).listFiles((d, n) -> n.toLowerCase().endsWith(".json"))) {
                List<String> t = ImportazioneGenerica.ConfigurazioneImport.TipiSconosciuti(
                        new JSONObject(Files.readString(f.toPath(), StandardCharsets.UTF_8)));
                if (!t.isEmpty()) errori.add(f.getName() + ": " + t);
            }
        }
        assertEquals(List.of(), errori);
    }

    @Test
    void ilResocontoDellImportazioneNominaITipiSconosciuti() {
        Importazioni.AzzeraContatori();
        assertEquals("", Importazioni.TestoTipiConfigSconosciuti());
        Importazioni.TipiConfigSconosciuti.add("mappaCausali: Interest -> reward");
        assertTrue(Importazioni.TestoTipiConfigSconosciuti().contains("Interest -&gt; reward"));
        Importazioni.AzzeraContatori();
        assertEquals("", Importazioni.TestoTipiConfigSconosciuti());
    }
}
