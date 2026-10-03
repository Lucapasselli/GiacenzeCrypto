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
 * Caratterizza tre causali aggiunte a {@code config/import/Binance CSV.json} (v1.015).
 *
 * <ul>
 *   <li>{@code Transfer} con Remark {@code Binance Pay - ...}: pagamenti e invii da/verso altri utenti
 *       Binance. Diventano PC/DC da classificare a mano, come {@code C2C Transfer}. Il confronto delle
 *       causali è esatto, quindi {@code Transfer} non cattura le varie {@code Transfer Between ...}.</li>
 *   <li>{@code Pre Auth - Capture}: coppia Funding -1 / Spot +1 nello stesso secondo, giroconto interno
 *       a saldo zero, ignorato come {@code Transfer Between Spot and Funding}.</li>
 *   <li>{@code NFT - Burn and Win}: premio della promozione Mobox, entrata senza contropartita.</li>
 * </ul>
 *
 * <p>Layout righe: {@code User ID,Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore
 * sintetico (evita la ricerca prezzi in rete).</p>
 */
class ImportazioneGenericaBinancePayTest {

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

    private static ConfigurazioneImport cfg() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    private static String[] riga(String time, String account, String operation, String coin, String change, String remark) {
        return new String[]{"1", time, account, operation, coin, change, remark, "1.00"};
    }

    private static List<String[]> importa(List<String[]> righe) throws Exception {
        ConfigurazioneImport c = cfg();
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(new ArrayList<>(righe), c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        return movs;
    }

    @Test
    void causaliMappate() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals("TRASFERIMENTO-CRYPTO", c.convertiCausale("Transfer"));
        assertEquals("IGNORA", c.convertiCausale("Pre Auth - Capture"));
        assertEquals("REWARD", c.convertiCausale("NFT - Burn and Win"));
        assertEquals("IGNORA", c.convertiCausale("Transfer Between Spot and Funding"),
                "il confronto esatto non deve confondere Transfer con le causali che lo contengono");
    }

    @Test
    void pagamentoBinancePay_prelievoDaClassificare() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2022-10-28 09:50:43", "Spot", "Transfer", "USDT", "-50", "Binance Pay - P_X")));
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_PC"), "prelievo crypto: " + movs.get(0)[0]);
        assertEquals("USDT", movs.get(0)[8]);
        assertEquals("", movs.get(0)[18], "campo 18 vuoto: la classificazione la decide l'utente");
    }

    @Test
    void incassoBinancePay_depositoDaClassificare() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2024-01-23 19:34:04", "Funding", "Transfer", "USDT", "1.19", "Binance Pay - P_Y")));
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_DC"), "deposito crypto: " + movs.get(0)[0]);
        assertEquals("USDT", movs.get(0)[11]);
    }

    @Test
    void pagamentoConDueMonete_restanoDuePrelievi() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2022-10-06 09:59:19", "Spot", "Transfer", "BUSD", "-0.22469707", "Binance Pay - P_Z"),
                riga("2022-10-06 09:59:19", "Spot", "Transfer", "BNB", "-0.00264863", "Binance Pay - P_Z")));
        assertEquals(2, movs.size(), "TRASFERIMENTO-CRYPTO è chiusa: due prelievi, non uno scambio");
        for (String[] m : movs) {
            assertTrue(m[0].endsWith("_PC"), m[0]);
        }
    }

    @Test
    void preAuthCapture_nonProduceMovimenti() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2022-08-12 03:59:43", "Spot", "Pre Auth - Capture", "BUSD", "1", ""),
                riga("2022-08-12 03:59:43", "Funding", "Pre Auth - Capture", "BUSD", "-1", "")));
        assertEquals(0, movs.size());
    }

    @Test
    void nftBurnAndWin_reward() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2022-06-08 09:31:17", "Spot", "NFT - Burn and Win", "MBOX", "2", "")));
        assertEquals(1, movs.size());
        assertEquals("MBOX", movs.get(0)[11]);
        assertEquals("2", movs.get(0)[13]);
    }
}
