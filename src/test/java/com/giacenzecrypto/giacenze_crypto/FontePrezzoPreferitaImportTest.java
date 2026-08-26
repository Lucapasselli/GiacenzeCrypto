package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.FileWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa il nuovo campo opzionale {@code fontePrezzoPreferita} di {@code ImportConfig/*.json}: guida
 * quale exchange preferire, fra quelli già scaricati, quando si cerca il prezzo di un movimento
 * importato da quella configurazione (vedi {@code ImportazioneGenerica.ConfigurazioneImport} e
 * {@code Prezzi.DammiPrezzoDaDatabase}). Non limita da chi si scarica - tutti gli exchange configurati
 * vengono comunque interrogati - sceglie solo quale prezzo già in cache preferire.
 */
public class FontePrezzoPreferitaImportTest {

    @Test
    public void laConfigurazioneBinanceDichiaraBinanceComeFontePreferita() throws Exception {
        var cfg = ImportazioneGenerica.ConfigurazioneImport.carica("ImportConfig/Binance CSV.json");
        assertEquals("binance", cfg.fontePrezzoPreferita);
    }

    @Test
    public void unaConfigurazioneSenzaIlCampoNonHaAlcunaPreferenza(@TempDir File dir) throws Exception {
        File file = new File(dir, "senza-fonte.json");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"nomeExchange\": \"Kraken\"}");
        }
        var cfg = ImportazioneGenerica.ConfigurazioneImport.carica(file.getAbsolutePath());
        assertEquals("", cfg.fontePrezzoPreferita);
    }
}
