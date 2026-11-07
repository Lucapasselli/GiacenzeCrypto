package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le righe scartate del resoconto ({@link Importazioni#Sconosciuti()}). Fino al 2026-11-06 erano una {@code String}
 * accodata con {@code +=}, quadratica: 40.000 righe scartate (un file grande letto con la configurazione sbagliata)
 * costavano 13 s, contro 0,3 s adesso, e il tempo cresceva col quadrato delle righe.
 */
class ImportazioniSconosciutiTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void azzera() {
        Importazioni.AzzeraContatori();
    }

    @Test
    void accodaPremettiImpostaAzzera() {
        Importazioni.AzzeraSconosciuti();
        Importazioni.AccodaSconosciuti("riga 1\n");
        Importazioni.AccodaSconosciuti("riga 2\n");
        Importazioni.PremettiSconosciuti("AVVISO\n");
        assertEquals("AVVISO\nriga 1\nriga 2\n", Importazioni.Sconosciuti());
        Importazioni.ImpostaSconosciuti("di prima\n");
        assertEquals("di prima\n", Importazioni.Sconosciuti());
        Importazioni.AzzeraContatori();
        assertEquals("", Importazioni.Sconosciuti(), "AzzeraContatori svuota anche le righe scartate");
    }

    @Test
    void moltissimeRigheScartate_tempoLineare() throws Exception {
        //Un CSV Binance letto con la configurazione Coinbase: tre righe di testa saltate, poi nessuna data leggibile
        StringBuilder csv = new StringBuilder("User_ID,UTC_Time,Account,Operation,Coin,Change,Remark\n");
        String coda = ",Spot,Deposit,BTC,0.00012345,Una nota lunga quanto quelle vere di un export, per pesare\n";
        for (int i = 0; i < 40_000; i++) csv.append(i).append(",2024-01-01 10:00:00").append(coda);
        Path f = tempDir.resolve("binance.csv");
        Files.writeString(f, csv, StandardCharsets.UTF_8);
        ConfigurazioneImport cfg = ConfigurazioneImport.carica("config/import/Coinbase CSV.json");

        Importazioni.AzzeraContatori();
        List<String[]> righe = assertTimeoutPreemptively(Duration.ofSeconds(20),
                () -> ImportazioneGenerica.leggiCSV(f.toString(), cfg));

        assertEquals(0, righe.size());
        assertTrue(Importazioni.TrasazioniSconosciute > 39_000, "scartate: " + Importazioni.TrasazioniSconosciute);
        assertEquals(Importazioni.TrasazioniSconosciute, Importazioni.Sconosciuti().split("\n").length);
    }
}
