package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fuso orario degli export OKX ({@code config/import/OKX_Funding.json} e {@code OKX_Trading.json}): gli export
 * recenti dichiarano nella prima riga il fuso scelto su OKX ({@code Time Zone:UTC+8}), che puo' non essere
 * quello italiano. Visto su un export reale: Funding in UTC+8 accanto a un Trading in UTC+2, e con
 * {@code Europe/Rome} fisso tutto il Funding finiva 6 ore piu' tardi.
 */
class ImportazioneGenericaOkxFusoTest {

    @TempDir
    static Path tempDir;

    private static final String INTESTAZIONE_FUNDING = "id,Time,Type,Amount,Before Balance,After Balance,Symbol,Amount-EUR";
    private static final String RIGA_DEPOSITO = "1,2026-07-07 17:44:49,Deposit,5,0,5,BNB,2534.07";

    private static ConfigurazioneImport leggiFunding(String primaRiga) throws Exception {
        Path f = tempDir.resolve("funding" + System.nanoTime() + ".csv");
        Files.write(f, (primaRiga + "\n" + INTESTAZIONE_FUNDING + "\n" + RIGA_DEPOSITO + "\n").getBytes("UTF-8"));
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/OKX_Funding.json");
        List<String[]> righe = ImportazioneGenerica.leggiCSV(f.toString(), c);
        assertEquals(1, righe.size());
        return c;
    }

    @Test
    void ilFusoDichiaratoNellIntestazioneSiNormalizza() {
        assertEquals("UTC+08:00", ImportazioneGenerica.estraiFusoDaRigaIntestazione(
                "UID:123,Account Type:Main,Time Zone:UTC+8", "Time Zone:"));
        assertEquals("UTC-03:30", ImportazioneGenerica.estraiFusoDaRigaIntestazione("Time Zone: UTC-3:30", "Time Zone:"));
        assertEquals("UTC", ImportazioneGenerica.estraiFusoDaRigaIntestazione("\"Time Zone:UTC\",x", "time zone:"));
        assertEquals("UTC", ImportazioneGenerica.estraiFusoDaRigaIntestazione("Time Zone:UTC+0", "Time Zone:"));
        assertNull(ImportazioneGenerica.estraiFusoDaRigaIntestazione("UID:123,Account Type:Main", "Time Zone:"));
        assertNull(ImportazioneGenerica.estraiFusoDaRigaIntestazione("Time Zone:Asia/Shanghai", "Time Zone:"));
        assertNull(ImportazioneGenerica.estraiFusoDaRigaIntestazione("Time Zone:UTC+8", null));
    }

    @Test
    void unExportInUtc8SiLeggeInUtc8() throws Exception {
        ConfigurazioneImport c = leggiFunding("UID:123,Account Type:Main,Time Zone:UTC+8");
        assertEquals("UTC+08:00", c.fuso);
        assertEquals(Instant.parse("2026-07-07T09:44:49Z").toEpochMilli(), c.convertiDataInMillis("2026-07-07 17:44:49"));
    }

    @Test
    void unExportSenzaFusoDichiaratoTieneQuelloDellaConfigurazione() throws Exception {
        ConfigurazioneImport c = leggiFunding("UID:123,Account Type:Main");
        assertEquals("Europe/Rome", c.fuso);
        assertEquals(Instant.parse("2026-07-07T15:44:49Z").toEpochMilli(), c.convertiDataInMillis("2026-07-07 17:44:49"));
    }

    /** UTC+2 e' l'ora legale di Europe/Rome: si tiene Europe/Rome, che in inverno passa da solo a UTC+1. */
    @Test
    void unoScostamentoDiEuropaRomaTieneEuropaRoma() throws Exception {
        ConfigurazioneImport c = leggiFunding("UID:123,Account Type:Main,Time Zone:UTC+2");
        assertEquals("Europe/Rome", c.fuso);
        assertEquals(Instant.parse("2026-01-07T16:44:49Z").toEpochMilli(), c.convertiDataInMillis("2026-01-07 17:44:49"));
        assertTrue(c.FusoCompatibile("UTC+01:00"));
        assertFalse(c.FusoCompatibile("UTC"));
    }

    @Test
    void laConfigurazioneTradingLeggeAncheLeiIlFuso() throws Exception {
        assertEquals("Time Zone:", ConfigurazioneImport.carica("config/import/OKX_Trading.json").fusoDaIntestazione);
    }

    @Test
    void iTipiBtcYieldERewardsSonoMappati() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/OKX_Funding.json");
        assertEquals("REWARD", c.convertiCausale("BTC Yield+ earnings"));
        assertEquals("REWARD", c.convertiCausale("Rewards"));
        assertEquals("IGNORA", c.convertiCausale("BTC Yield+ subscription"));
    }
}
