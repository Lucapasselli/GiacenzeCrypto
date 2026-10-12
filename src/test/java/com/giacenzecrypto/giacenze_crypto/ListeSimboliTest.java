package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le liste di simboli di {@code config/varie/Simboli.json} ({@link ListeSimboli}). */
class ListeSimboliTest {

    private static String file() throws Exception {
        return Files.readString(Path.of("config/varie/" + ListeSimboli.NOME + ".json"));
    }

    @Test
    void ilFileHaEsattamenteLaListaCheEraNelCodice() throws Exception {
        //Stesso contenuto e stesso ordine di Prezzi.SimboliPrioritari prima dello spostamento: nessun prezzo cambia
        assertArrayEquals(ListeSimboli.PRIORITARI_PREDEFINITI, ListeSimboli.InterpretaPrioritari(file(), ListeSimboli.NOME));
        assertEquals(27, ListeSimboli.PRIORITARI_PREDEFINITI.length);
    }

    @Test
    void ilFileEInstallatoMaNonCentralizzato() throws Exception {
        assertTrue(Arrays.asList(MappeCausali.FILE_VARIE).contains(ListeSimboli.NOME), "installato e riallineato come gli altri");
        assertFalse(file().contains("\"centralizzato\""),
                "un file centralizzato non ancora pubblicato verrebbe cancellato all'avvio");
    }

    @Test
    void contenutoNonValidoDaNull() {
        assertNull(ListeSimboli.InterpretaPrioritari(null, "x"));
        assertNull(ListeSimboli.InterpretaPrioritari("  ", "x"));
        assertNull(ListeSimboli.InterpretaPrioritari("non json", "x"));
        assertNull(ListeSimboli.InterpretaPrioritari("{\"prioritari\":{\"simboli\":[]}}", "x"));
        assertNull(ListeSimboli.InterpretaPrioritari("{\"altro\":1}", "x"));
    }

    @Test
    void glispaziEVociVuoteSiScartano() {
        assertArrayEquals(new String[]{"AAA", "BBB"},
                ListeSimboli.InterpretaPrioritari("{\"prioritari\":{\"simboli\":[\" AAA \",\"\",\"BBB\"]}}", "x"));
    }

    @Test
    void SimboliPrioritariSenzaFileUtilizzabileRipiegaSuiPredefiniti() {
        String[] s = Prezzi.SimboliPrioritari();
        assertNotNull(s);
        assertTrue(Arrays.asList(s).contains("USDT"));
        assertTrue(Arrays.asList(s).contains("BTC"));
    }

    @Test
    void ilConfrontoConIlSimboloIgnoraLeMaiuscole() {
        //Fino al 2026-10-12 il simbolo si portava in maiuscolo e si confrontava con la lista cosi' com'e': EUROe ed EURe
        //(scritti con minuscole) non coincidevano mai e restavano fuori dal prezzo diretto dagli exchange
        for (String voce : new String[]{"EUROe", "EURe"}) {
            for (String simbolo : new String[]{voce, voce.toUpperCase(), voce.toLowerCase()}) {
                assertTrue(Arrays.stream(Prezzi.SimboliPrioritari()).anyMatch(simbolo::equalsIgnoreCase), simbolo);
            }
        }
    }

    @Test
    void leValuteFiatDelFileSonoQuelleCheEranoNelCodice() throws Exception {
        assertArrayEquals(ListeSimboli.FIAT_PREDEFINITE, ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "fiat"));
        assertEquals(java.util.Set.of("EUR", "USD", "TRY", "GBP"), ListeSimboli.Fiat());
    }

    @Test
    void ilTipoAutomaticoDellaMonetaSegueLaListaFiat() {
        for (String f : new String[]{"EUR", "USD", "TRY", "GBP"}) {
            Moneta m = new Moneta();
            m.Moneta = f;
            m.AssegnaTipoAuto();
            assertEquals("FIAT", m.Tipo, f);
        }
        for (String c : new String[]{"BTC", "USDT", "eur", "EURC"}) {
            Moneta m = new Moneta();
            m.Moneta = c;
            m.AssegnaTipoAuto();
            assertEquals("Crypto", m.Tipo, c);
        }
    }

    @Test
    void leStablecoinDelFileSonoQuelleCheEranoInPrincipale() throws Exception {
        assertArrayEquals(ListeSimboli.STABLECOIN_DUBBIE_PREDEFINITE,
                ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "stablecoinDubbie"));
        assertArrayEquals(ListeSimboli.STABLECOIN_ALTRE_PREDEFINITE,
                ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "stablecoinAltre"));
        assertArrayEquals(ListeSimboli.STABLECOIN_DUBBIE_PREDEFINITE, ListeSimboli.StablecoinDubbie());
        assertArrayEquals(ListeSimboli.STABLECOIN_ALTRE_PREDEFINITE, ListeSimboli.StablecoinAltre());
    }
}
