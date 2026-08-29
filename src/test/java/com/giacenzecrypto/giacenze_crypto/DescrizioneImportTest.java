package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.FileWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa il campo opzionale {@code descrizione} di {@code config/import/*.json}: testo libero che non
 * incide sull'importazione e viene mostrato come tooltip sulla voce nella finestra
 * {@code Importazioni_Gestione} (tramite {@code VoceImport.tooltip()} e
 * {@code LoghiImport.RenderComboConLogo}).
 */
public class DescrizioneImportTest {

    @Test
    public void laConfigurazioneBitgetPortaUnaDescrizione() throws Exception {
        var cfg = ImportazioneGenerica.ConfigurazioneImport.carica("config/import/Bitget CSV.json");
        assertTrue(cfg.descrizione.contains("Bitget"), "la descrizione deve essere valorizzata");
    }

    @Test
    public void senzaIlCampoLaDescrizioneEVuota(@TempDir File dir) throws Exception {
        File file = new File(dir, "senza-descrizione.json");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"nomeExchange\": \"Kraken\"}");
        }
        var cfg = ImportazioneGenerica.ConfigurazioneImport.carica(file.getAbsolutePath());
        assertEquals("", cfg.descrizione);
    }

    @Test
    public void ilTooltipDellaVoceEIlTestoDellaDescrizioneOppureNull() {
        var conTesto = new Importazioni_Gestione.VoceImport("Bitget", "CSV", null, new File("x.json"), "  caratteristiche  ");
        assertEquals("caratteristiche", conTesto.tooltip(), "il tooltip è la descrizione, spazi esterni tolti");

        var senzaTesto = new Importazioni_Gestione.VoceImport("Bitget", "CSV", null, new File("x.json"), "   ");
        assertNull(senzaTesto.tooltip(), "descrizione vuota o di soli spazi = nessun tooltip");

        var nativa = new Importazioni_Gestione.VoceImport("Binance", "Formato storico", "NAT_BINANCE_OLD", null);
        assertNull(nativa.tooltip(), "una voce senza descrizione non ha tooltip");
    }
}
