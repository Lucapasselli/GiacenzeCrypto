package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caratterizza {@code config/import/Coinbase Pro GDAX.json} dopo il passaggio a causale unica
 * (v2.000): niente più composita {@code type.unit}, quindi aggiungere una moneta non richiede una
 * riga nuova in {@code mappaCausali}. Il formato è quello dello statement
 * {@code account.csv}: {@code portfolio,type,time,amount,balance,unit,transfer id,trade
 * id,order id}.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>la config reale carica senza colonna causale2 e con le 5 causali {@code type} nude;</li>
 *   <li>{@code deposit}/{@code withdrawal} sono classificati dall'auto-classificazione del motore
 *       in base al tipo moneta: EUR &rarr; {@code DF}/{@code PF}, cripto &rarr; {@code DC}/{@code
 *       PC} (con campo18 vuoto, da abbinare a mano come trasferimento tra wallet);</li>
 *   <li>una coppia {@code match} + la sua {@code fee} sullo stesso {@code trade id} danno uno
 *       scambio consolidato + un movimento {@code COMMISSIONI} a sé;</li>
 *   <li>{@code rebate} non viene scartato.</li>
 * </ul>
 */
class ImportazioneGenericaCoinbaseProGdaxTest {

    @TempDir
    static Path tempDir;

    /** Un movimento in cripto senza prezzo fa consultare la cache prezzi/EMoney: serve il DB. */
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

    private static ConfigurazioneImport cfg() throws Exception {
        return ConfigurazioneImport.carica("config/import/Coinbase Pro GDAX.json");
    }

    /**
     * Come {@link #cfg()} ma con una colonna controvalore sintetica (indice 9, aggiunta da
     * {@link #riga}): l'import reale non porta un prezzo e i movimenti in cripto senza prezzo
     * farebbero partire una ricerca prezzi in rete, che qui non serve e renderebbe il test lento e
     * non deterministico. Non incide sulla categoria (DF/DC/PC/PF), che è ciò che il test fissa.
     */
    private static ConfigurazioneImport cfgVeloce() throws Exception {
        ConfigurazioneImport c = cfg();
        c.colonnaValoreEuro = 9;
        return c;
    }

    /** {@code portfolio,type,time,amount,balance,unit,transfer id,trade id,order id} + [9]=controvalore sintetico */
    private static String[] riga(String type, String time, String amount, String unit,
            String transferId, String tradeId) {
        return new String[]{"default", type, time, amount, "0", unit, transferId, tradeId, "", "1.00"};
    }

    // ---- forma della configurazione reale --------------------------------------------------------

    @Test
    void configurazioneReale_causaleUnica_senzaComposita() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals("Coinbase Pro", c.nomeExchange, "wallet distinto dal retail (campo [3])");
        assertEquals("Principale", c.nomeWallet);
        assertEquals(-1, c.colonnaCausale2, "niente più causale composita type.unit");
        assertEquals("DEPOSITO FIAT", c.mappaCausali.get("deposit"));
        assertEquals("PRELIEVO FIAT", c.mappaCausali.get("withdrawal"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.mappaCausali.get("match"));
        assertEquals("COMMISSIONI", c.mappaCausali.get("fee"));
        assertEquals("REWARD", c.mappaCausali.get("rebate"));
        assertEquals("CELO", c.rinominaMonete.get("CGLD"), "allineato al retail");
    }

    // ---- deposit / withdrawal: EUR vs cripto decisi dall'auto-classificazione -------------------

    @Test
    void depositInEuro_diventaDepositoFiat() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("deposit", "2025-01-10T10:00:00.000Z", "50", "EUR",
                        "00000000-0000-0000-0000-000000000001", ""),
                null, cfgVeloce());
        assertNotNull(movs, "causale 'deposit' riconosciuta");
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_DF"), "EUR in ingresso -> DEPOSITO FIAT: " + movs.get(0)[0]);
    }

    @Test
    void depositInCripto_diventaDepositoCrypto() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("deposit", "2025-01-11T10:00:00.000Z", "0.0600000000000000", "ZEC",
                        "00000000-0000-0000-0000-000000000002", ""),
                null, cfgVeloce());
        assertNotNull(movs, "'deposit.ZEC' non è più una causale sconosciuta");
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertTrue(m[0].endsWith("_DC"), "cripto in ingresso -> DEPOSITO CRYPTO: " + m[0]);
        assertEquals("ZEC", m[11], "gamba cripto in entrata");
        assertEquals("", m[18], "campo18 vuoto: da classificare a mano come trasferimento tra wallet");
    }

    @Test
    void withdrawalInCripto_diventaPrelievoCrypto() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("withdrawal", "2025-01-12T10:00:00.000Z", "-500.0000000000000000", "XLM",
                        "00000000-0000-0000-0000-000000000003", ""),
                null, cfgVeloce());
        assertNotNull(movs, "'withdrawal.XLM' non è più una causale sconosciuta");
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertTrue(m[0].endsWith("_PC"), "cripto in uscita -> PRELIEVO CRYPTO: " + m[0]);
        assertEquals("XLM", m[8], "gamba cripto in uscita");
    }

    @Test
    void withdrawalInEuro_diventaPrelievoFiat() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("withdrawal", "2021-06-15T09:00:00.000Z", "-200.00", "EUR",
                        "aaaaaaaa-0000-0000-0000-000000000000", ""),
                null, cfgVeloce());
        assertNotNull(movs);
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_PF"), "EUR in uscita -> PRELIEVO FIAT: " + movs.get(0)[0]);
    }

    // ---- match + fee sullo stesso trade id -----------------------------------------------------

    @Test
    void matchDueGambePiuFee_scambioConsolidatoPiuCommissioniASe() throws Exception {
        ConfigurazioneImport c = cfgVeloce();
        List<String[]> righe = new ArrayList<>();
        righe.add(riga("match", "2025-01-13T10:00:00.000Z", "-0.0600000000000000", "ZEC", "", "100001"));
        righe.add(riga("match", "2025-01-13T10:00:00.000Z", "0.0001200000000000", "BTC", "", "100001"));
        righe.add(riga("fee",   "2025-01-13T10:00:00.000Z", "-0.0000006000000000", "BTC", "", "100001"));

        List<List<String[]>> gruppi = ImportazioneGenerica.raggruppaRighe(righe, c);
        assertEquals(1, gruppi.size(), "stesso trade id, stesso istante -> un solo gruppo");

        List<String[]> movs = ImportazioneGenerica.consolidaGruppo(gruppi.get(0), c, new ArrayList<>());
        assertNotNull(movs);
        assertEquals(2, movs.size(), "uno scambio + un movimento COMMISSIONI");

        String[] scambio = movs.stream()
                .filter(m -> !m[5].toUpperCase().contains("COMMISSION"))
                .findFirst().orElseThrow();
        assertEquals("ZEC", scambio[8], "moneta ceduta");
        assertEquals("BTC", scambio[11], "moneta acquisita");

        String[] comm = movs.stream()
                .filter(m -> m[5].toUpperCase().contains("COMMISSION"))
                .findFirst().orElseThrow();
        assertEquals("BTC", comm[8], "commissione pagata in BTC, scaricata dalla giacenza");
    }

    // ---- rebate ------------------------------------------------------------------------------------

    @Test
    void rebate_nonScartato() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("rebate", "2022-03-01T10:00:00.000Z", "0.01500000", "EUR", "", "999999"),
                null, cfgVeloce());
        assertNotNull(movs, "'rebate' mappato a REWARD, non scartato");
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_RW"), "categoria REWARD: " + movs.get(0)[0]);
    }
}
