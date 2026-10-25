package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caratterizza {@code config/import/Exodus.json}: entrata e uscita sulla stessa riga, la fee di rete come movimento a
 * parte anche sulle righe di sola entrata, e le ricompense di partecipazione di Algorand riconosciute con
 * {@code regoleRiga} (deposito di ALGO senza portafoglio di arrivo).
 *
 * <p>Dati sintetici nella forma dell'export originale: indirizzi e hash sono inventati, i CSV veri dell'utente non
 * vanno committati, e nemmeno date e importi delle righe a campione, che su una blockchain pubblica bastano a
 * ritrovare le transazioni. La forma (fee sempre in piu' dell'importo) e' quella verificata on-chain il 2026-10-25.</p>
 */
class ImportazioneGenericaExodusTest {

    private static final String INTESTAZIONE = "DATE,TYPE,FROMPORTFOLIO,TOPORTFOLIO,OUTAMOUNT,OUTCURRENCY,FEEAMOUNT,"
            + "FEECURRENCY,FROMADDRESS,TOADDRESS,OUTTXID,OUTTXURL,INAMOUNT,INCURRENCY,INTXID,INTXURL,ORDERID,PERSONALNOTE";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        System.clearProperty("prezzi.servizio.abilitato");
        Importazioni.AzzeraContatori();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Legge il CSV con la configurazione distribuita e costruisce i movimenti come l'importazione. */
    private static List<String[]> importa(String... righe) throws Exception {
        Path csv = tempDir.resolve("exodus.csv");
        Files.writeString(csv, INTESTAZIONE + "\n" + String.join("\n", righe) + "\n", StandardCharsets.UTF_8);
        ConfigurazioneImport cfg = ConfigurazioneImport.carica("config/import/Exodus.json");
        assertEquals(List.of(), cfg.chiaviSconosciute);

        Importazioni.AzzeraContatori();
        List<String[]> lette = ImportazioneGenerica.leggiCSV(csv.toString(), cfg);
        List<String[]> movimenti = new ArrayList<>();
        for (List<String[]> gruppo : ImportazioneGenerica.raggruppaRighe(lette, cfg)) {
            movimenti.addAll(ImportazioneGenerica.consolidaGruppo(gruppo, cfg, new ArrayList<>()));
        }
        assertEquals("", Importazioni.movimentiSconosciuti, "righe scartate");
        return movimenti;
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    private static List<String[]> conCategoria(List<String[]> movs, String categoria) {
        return movs.stream().filter(m -> categoria(m).equals(categoria)).toList();
    }

    @Test
    void configurazioneReale() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Exodus.json");
        assertEquals("Exodus", c.nomeExchange, "e' anche il nome proposto dagli import manuali dei wallet: non cambiarlo");
        assertFalse(c.consolidaRigheStessaData, "ogni riga e' un movimento a se'");
    }

    @Test
    void deposito_unaGambaInEntrataSenzaFee() throws Exception {
        List<String[]> movs = importa(
                "2020-05-02T10:11:12.000Z,deposit,,exodus_0,,,,,DAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA,,,,12,DGB,"
                + "aaaa000000000000000000000000000000000000000000000000000000000001,https://esempio/tx/1,,");
        assertEquals(1, movs.size());
        String[] d = movs.get(0);
        assertEquals("DC", categoria(d));
        assertEquals("Exodus", d[3]);
        assertEquals("DGB", d[11]);
        assertEquals("12", d[13]);
        assertEquals("aaaa000000000000000000000000000000000000000000000000000000000001", d[24], "TxID da INTXID");
    }

    @Test
    void prelievo_importoNettoEFeeInPiu() throws Exception {
        List<String[]> movs = importa(
                "2021-06-10T08:00:00.000Z,withdrawal,exodus_0,,-40,DOGE,-2.5,DOGE,,DBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB,"
                + "bbbb000000000000000000000000000000000000000000000000000000000002,https://esempio/tx/2,,,,,,");
        assertEquals(List.of("CM", "PC"), movs.stream().map(ImportazioneGenericaExodusTest::categoria).sorted().toList());
        String[] p = conCategoria(movs, "PC").get(0);
        assertEquals("DOGE", p[8]);
        assertEquals("-40", p[10], "OUTAMOUNT e' quanto arrivato al destinatario: niente lordo da ricostruire");
        String[] f = conCategoria(movs, "CM").get(0);
        assertEquals("DOGE", f[8]);
        assertEquals("-2.5", f[10]);
    }

    @Test
    void rewardAlgo_ricompensaLordaPiuCommissione() throws Exception {
        String indirizzo = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        List<String[]> movs = importa(
                "2021-02-03T12:00:00.000Z,deposit,exodus_0,,,,-0.001,ALGO," + indirizzo + "," + indirizzo + ",,,0.3125,ALGO,"
                + "TXALGO0000000000000000000000000000000000000000000001,https://esempio/tx/3,,",
                "2021-02-17T12:00:00.000Z,deposit,exodus_0,,,,-0.001,ALGO," + indirizzo + "," + indirizzo + ",,,0.125,ALGO,"
                + "TXALGO0000000000000000000000000000000000000000000002,https://esempio/tx/4,,");
        assertEquals(List.of("CM", "CM", "RW", "RW"), movs.stream().map(ImportazioneGenericaExodusTest::categoria).sorted().toList());
        assertEquals(List.of("0.125", "0.3125"), conCategoria(movs, "RW").stream().map(m -> m[13]).sorted().toList(),
                "INAMOUNT e' il sender-rewards dell'indexer: la fee non va tolta");
        for (String[] f : conCategoria(movs, "CM")) {
            assertEquals("ALGO", f[8]);
            assertEquals("-0.001", f[10]);
        }
    }

    @Test
    void algoConPortafoglioDiArrivoOInUscita_restaUnTrasferimento() throws Exception {
        List<String[]> movs = importa(
                "2021-02-01T10:00:00.000Z,deposit,,exodus_0,,,,,ALGOMITTENTE,,,,100,ALGO,TXALGODEP,https://esempio/tx/5,,",
                "2021-02-02T10:00:00.000Z,withdrawal,exodus_0,,-40,ALGO,-0.001,ALGO,,ALGODESTINATARIO,TXALGOPRE,https://esempio/tx/6,,,,,,");
        assertEquals(List.of("CM", "DC", "PC"), movs.stream().map(ImportazioneGenericaExodusTest::categoria).sorted().toList());
    }

    @Test
    void depositoSenzaPortafoglioDiArrivoDiUnAltraMoneta_restaUnDeposito() throws Exception {
        //La forma della reward ALGO su un'altra rete non e' una ricompensa nota: resta un deposito da classificare
        List<String[]> movs = importa(
                "2021-03-01T10:00:00.000Z,deposit,exodus_0,,,,-0.0001,ETH,0xaaaa,0xaaaa,,,0.5,ETH,TXETH,https://esempio/tx/7,,");
        assertEquals(List.of("CM", "DC"), movs.stream().map(ImportazioneGenericaExodusTest::categoria).sorted().toList());
    }

    @Test
    void scambio_dueGambeSullaStessaRiga() throws Exception {
        //Forma non ancora vista in un export reale: fissa quello che la configurazione ne fa oggi
        List<String[]> movs = importa(
                "2021-06-01T12:00:00.000Z,exchange,exodus_0,exodus_0,-0.01,BTC,-0.0002,BTC,bc1qmittente,3destinatario,"
                + "TXBTC,https://esempio/tx/8,0.15,ETH,TXETH2,https://esempio/tx/9,ORDINE1,");
        assertEquals(List.of("CM", "SC"), movs.stream().map(ImportazioneGenericaExodusTest::categoria).sorted().toList());
        String[] s = conCategoria(movs, "SC").get(0);
        assertEquals("BTC", s[8]);
        assertEquals("-0.01", s[10]);
        assertEquals("ETH", s[11]);
        assertEquals("0.15", s[13]);
    }
}
