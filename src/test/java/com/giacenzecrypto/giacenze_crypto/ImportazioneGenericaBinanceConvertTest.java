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
 * Caratterizza {@code Binance Convert} in {@code config/import/Binance CSV.json} (v1.013).
 *
 * <p>Nell'export Binance le due gambe di un Convert possono avere timestamp sfalsati di 1 secondo
 * (gli acquisti ricorrenti EUR&rarr;BTC: BTC alle 17:48:20, EUR alle 17:48:21). Senza tolleranza
 * {@code raggruppaRighe} le metteva in due gruppi e ne uscivano un deposito e un prelievo invece di
 * uno scambio. Non c'entra la gamba fiat: EUR&rarr;BTC nello stesso secondo si univa già.</p>
 *
 * <p>Layout righe: {@code User ID,Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore
 * sintetico (evita la ricerca prezzi in rete).</p>
 */
class ImportazioneGenericaBinanceConvertTest {

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

    private static String[] riga(String time, String coin, String change) {
        return new String[]{"1", time, "Spot", "Binance Convert", coin, change, "", "1.00"};
    }

    @Test
    void convertConGambeSfalsateDiUnSecondo_unSoloGruppo() throws Exception {
        List<String[]> righe = new ArrayList<>(List.of(
                riga("2026-01-10 17:48:20", "BTC", "0.0000128"),
                riga("2026-01-10 17:48:21", "EUR", "-1")));
        assertEquals(1, ImportazioneGenerica.raggruppaRighe(righe, cfg()).size());
    }

    @Test
    void convertNelloStessoSecondo_unSoloGruppo() throws Exception {
        List<String[]> righe = new ArrayList<>(List.of(
                riga("2026-01-06 16:06:48", "BREV", "-11"),
                riga("2026-01-06 16:06:48", "BTC", "0.00004525")));
        assertEquals(1, ImportazioneGenerica.raggruppaRighe(righe, cfg()).size());
    }

    @Test
    void convertDistintiInGiorniDiversi_restanoSeparati() throws Exception {
        List<String[]> righe = new ArrayList<>(List.of(
                riga("2026-01-10 17:48:20", "BTC", "0.0000128"),
                riga("2026-01-10 17:48:21", "EUR", "-1"),
                riga("2026-01-11 17:48:20", "BTC", "0.00001275"),
                riga("2026-01-11 17:48:21", "EUR", "-1")));
        assertEquals(2, ImportazioneGenerica.raggruppaRighe(righe, cfg()).size());
    }

    @Test
    void convertEurBtcSfalsato_dannoUnoScambioUnico() throws Exception {
        ConfigurazioneImport c = cfg();
        List<String[]> righe = new ArrayList<>(List.of(
                riga("2026-01-10 17:48:20", "BTC", "0.0000128"),
                riga("2026-01-10 17:48:21", "EUR", "-1")));
        List<List<String[]>> gruppi = ImportazioneGenerica.raggruppaRighe(righe, c);
        List<String[]> movs = ImportazioneGenerica.consolidaGruppo(gruppi.get(0), c, new ArrayList<>());

        assertEquals(1, movs.size(), "un solo movimento congiunto, non deposito + prelievo");
        assertEquals("EUR", movs.get(0)[8], "gamba in uscita");
        assertEquals("BTC", movs.get(0)[11], "gamba in entrata");
    }
}
