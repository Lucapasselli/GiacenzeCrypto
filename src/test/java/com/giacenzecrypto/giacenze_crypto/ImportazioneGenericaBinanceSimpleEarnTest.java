package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa {@code config/import/Binance Simple Earn Flexible.json}: lo storico Simple Earn Flessibile scaricato dall'app
 * Binance. Entrano solo le "Real-time APR Rewards", che l'export normale delle transazioni non contiene; i "Bonus Tiered
 * APR Rewards" sono già lì come "Simple Earn Flexible Interest" e si ignorano. Le Real-time hanno solo il giorno: con
 * {@code oraSeSoloData} diventano le 00:00:00 UTC di quel giorno, che restano nello stesso giorno anche in ora italiana.
 *
 * <p>Layout righe: {@code Time,Coin,Amount,Type} + [4]=controvalore sintetico (evita la ricerca prezzi in rete).</p>
 */
class ImportazioneGenericaBinanceSimpleEarnTest {

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
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance Simple Earn Flexible.json");
        c.colonnaValoreEuro = 4;
        c.fuso = "UTC";
        return c;
    }

    private static String[] riga(String time, String coin, String amount, String type) {
        return new String[]{time, coin, amount, type, "1.00"};
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
    void laDataSenzaOra_eLeZeroUTCDiQuelGiorno() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals(Instant.parse("2026-03-15T00:00:00Z").toEpochMilli(), c.convertiDataInMillis("2026-03-15"));
        assertEquals(Instant.parse("2026-03-15T03:16:11Z").toEpochMilli(), c.convertiDataInMillis("2026-03-15 03:16:11"),
                "le righe con l'ora restano come sono");
        assertEquals(0L, c.convertiDataInMillis("15/03/2026"), "un formato diverso resta illeggibile");
    }

    @Test
    void senzaOraSeSoloData_laRigaConIlSoloGiornoNonSiLegge() throws Exception {
        ConfigurazioneImport c = cfg();
        c.oraSeSoloData = null;
        assertEquals(0L, c.convertiDataInMillis("2026-03-15"));
    }

    @Test
    void entranoSoloLeRealTime_iBonusSonoGiaNellExportNormale() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals("EARN", c.convertiCausale("Real-time APR Rewards"));
        assertEquals("IGNORA", c.convertiCausale("Bonus Tiered APR Rewards"));

        List<String[]> movs = importa(List.<String[]>of(
                riga("2026-03-16 05:48:00", "USDC", "0.027396", "Bonus Tiered APR Rewards"),
                riga("2026-03-15", "USDC", "0.02887891", "Real-time APR Rewards"),
                riga("2026-03-15", "ETH", "0.00000477", "Real-time APR Rewards"),
                riga("2026-03-15 04:16:00", "USDC", "0.027396", "Bonus Tiered APR Rewards")));

        assertEquals(2, movs.size(), "due Real-time di monete diverse nello stesso istante restano due ricompense");
        for (String[] m : movs) {
            assertTrue(m[0].endsWith("_RW"), m[0]);
            assertEquals("Binance", m[3]);
            assertEquals("Principale", m[4]);
            assertTrue(m[0].startsWith("20260315"), "stesso giorno anche in ora italiana: " + m[0]);
        }
        assertEquals("0.02887891", movs.stream().filter(m -> m[11].equals("USDC")).findFirst().orElseThrow()[13]);
    }
}
