package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le liste di simboli di {@code config/varie/Simboli.json} ({@link ListeSimboli}). */
class ListeSimboliTest {

    private static String file() throws Exception {
        return Files.readString(Path.of("config/varie/" + ListeSimboli.NOME + ".json"));
    }

    @Test
    void ilFileHaLeVociIndispensabili() throws Exception {
        //Non si confronta con una copia nel codice (non c'e'): il file si puo' modificare, ma queste voci reggono i motori
        List<String> prioritari = Arrays.asList(ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "prioritari"));
        for (String s : List.of("USDT", "USDC", "BTC", "ETH", "EUROe", "EURe")) assertTrue(prioritari.contains(s), s);
        List<String> fiat = Arrays.asList(ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "fiat"));
        for (String s : List.of("EUR", "USD", "TRY", "GBP")) assertTrue(fiat.contains(s), s);
        assertTrue(Arrays.asList(ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "stablecoinDubbie")).contains("USDT"));
        assertTrue(Arrays.asList(ListeSimboli.Interpreta(file(), ListeSimboli.NOME, "stablecoinAltre")).contains("DAI"));
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
    void SimboliPrioritariSiLeggonoDalFile() {
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
    void leValuteFiatInMemoriaVengonoDalFile() {
        assertTrue(ListeSimboli.Fiat().containsAll(Set.of("EUR", "USD", "TRY", "GBP")));
        assertFalse(ListeSimboli.Fiat().contains("BTC"));
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
    void leStablecoinSiLeggonoDalFile() {
        assertTrue(Arrays.asList(ListeSimboli.StablecoinDubbie()).contains("BUSD"));
        assertTrue(Arrays.asList(ListeSimboli.StablecoinAltre()).contains("FRAX"));
    }

}
