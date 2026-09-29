package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Collegamento delle commissioni ({@link CommissioniCollegate}) nell'import CSV generico, sul caso più
 * comune: il CSV di Binance, dove uno scambio arriva su più righe (Transaction Spend + Transaction Buy)
 * e la commissione su una riga a sé (Transaction Fee), con causale {@code COMMISSIONI} fra le causali
 * chiuse. La riga di commissione passa da sola per {@code costruisciMovimenti} e va collegata allo
 * scambio ricostruito dal gruppo.
 */
class ImportazioneGenericaCommissioniCollegateTest {

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

    private static ConfigurazioneImport cfgBinanceVeloce() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 7; // controvalore sintetico: niente ricerca prezzi online
        return c;
    }

    /** {@code User_ID,UTC_Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore sintetico */
    private static String[] rigaBinance(String time, String operation, String coin, String change) {
        return new String[]{"1", time, "Spot", operation, coin, change, "", "1.00"};
    }

    @Test
    void rigaFeeASe_vieneCollegataAlloScambioDelGruppo() throws Exception {
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2023-05-12 06:24:50", "Transaction Spend", "USDT", "-100.00000000"));
        gruppo.add(rigaBinance("2023-05-12 06:24:50", "Transaction Buy", "BTC", "0.00400000"));
        gruppo.add(rigaBinance("2023-05-12 06:24:50", "Transaction Fee", "BNB", "-0.00100000"));

        List<String[]> movs = ImportazioneGenerica.consolidaGruppo(gruppo, cfgBinanceVeloce(), new ArrayList<>());

        String[] fee = movs.stream().filter(CommissioniCollegate::isCommissione).findFirst().orElse(null);
        String[] scambio = movs.stream().filter(m -> !CommissioniCollegate.isCommissione(m)).findFirst().orElse(null);
        assertNotNull(fee, "la riga Transaction Fee produce un movimento di commissione");
        assertNotNull(scambio, "le due gambe producono lo scambio");
        assertFalse(CommissioniCollegate.Chiave(fee).isEmpty(), "la commissione ha la chiave");
        assertEquals(CommissioniCollegate.Chiave(scambio), CommissioniCollegate.Chiave(fee));
    }

    @Test
    void commissioneDaSola_restaSenzaChiave() throws Exception {
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2023-05-12 06:24:50", "Transaction Fee", "BNB", "-0.00100000"));

        List<String[]> movs = ImportazioneGenerica.consolidaGruppo(gruppo, cfgBinanceVeloce(), new ArrayList<>());

        assertEquals(1, movs.size());
        assertEquals("", CommissioniCollegate.Chiave(movs.get(0)), "non c'e' un movimento a cui collegarla");
    }
}
