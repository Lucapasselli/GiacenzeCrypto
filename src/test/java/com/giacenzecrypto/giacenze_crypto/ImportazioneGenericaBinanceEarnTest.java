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
 * Fissa i bonus Simple Earn di {@code config/import/Binance CSV.json} (v1.017). Dagli export 2026 Binance scrive ogni
 * Bonus Tiered APR due volte: sul conto Earn ({@code Simple Earn Flexible - Rewards Income}) e sul conto Spot, dove lo
 * paga ({@code Simple Earn Flexible Interest}, stessa moneta e quantità, di solito 0-2 ore dopo). Verificato sullo storico
 * Rewards dell'app: è la stessa ricompensa. Si tiene la riga Spot, come negli anni in cui la riga Earn non c'era.
 *
 * <p>Layout righe: {@code User ID,Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore sintetico.</p>
 */
class ImportazioneGenericaBinanceEarnTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
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

    private static String[] riga(String time, String account, String operation, String coin, String change) {
        return new String[]{"1", time, account, operation, coin, change, "Binance Earn", "1.00"};
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
        assertEquals("IGNORA", c.convertiCausale("Simple Earn Flexible - Rewards Income"));
        assertEquals("IGNORA", c.convertiCausale("Simple Earn Locked - Rewards Income"));
        assertEquals("EARN", c.convertiCausale("Simple Earn Flexible Interest"));
        assertEquals("EARN", c.convertiCausale("Simple Earn Locked Rewards"));
    }

    @Test
    void ilBonusScrittoSuEarnESuSpot_entraUnaVoltaSola() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                riga("2024-03-15 03:16:11", "Earn", "Simple Earn Flexible - Rewards Income", "USDC", "0.027396"),
                riga("2024-03-15 04:19:06", "Spot", "Simple Earn Flexible Interest", "USDC", "0.027396"),
                riga("2024-03-15 06:10:00", "Earn", "Simple Earn Locked - Rewards Income", "ETH", "0.0001"),
                riga("2024-03-15 06:10:00", "Spot", "Simple Earn Locked Rewards", "ETH", "0.0001")));

        assertEquals(2, movs.size(), "un bonus per moneta, dalla riga Spot");
        for (String[] m : movs) assertTrue(m[0].endsWith("_RW"), m[0]);
        assertEquals("USDC", movs.get(0)[11]);
        assertTrue(movs.get(0)[0].startsWith("20240315"), movs.get(0)[0]);
    }
}
