package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elenco dei token di moneta elettronica noti ({@link EmoneyToken}) e controllo dei movimenti con token dell'elenco
 * che la sezione E-Money non contiene, per l'avviso del calcolo dei quadri ({@link Principale_EmoneyMancanti}).
 */
class EmoneyTokenTest {

    private Map<String, String> emoneyPrima;
    private Map<String, String> casePrima;

    @BeforeEach
    void salvaSezione() {
        emoneyPrima = new HashMap<>(Principale.Mappa_EMoney);
        casePrima = new HashMap<>(Principale.Mappa_EMoney_CaseSensitive);
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
    }

    @AfterEach
    void ripristinaSezione() {
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney.putAll(emoneyPrima);
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_EMoney_CaseSensitive.putAll(casePrima);
    }

    private static EmoneyToken.Elenco elencoDelFile() throws Exception {
        String contenuto = Files.readString(Path.of("config/varie/" + EmoneyToken.NOME + ".json"));
        return EmoneyToken.Interpreta(contenuto, EmoneyToken.NOME);
    }

    @Test
    void ilFileContieneITokenCheEranoNelCodicePiuUSDG() throws Exception {
        EmoneyToken.Elenco e = elencoDelFile();
        assertNotNull(e);
        //L'array EmoneyMica che stava in Principale fino al 2026-10-09: spostandolo nel file non se ne perde nessuno
        for (String s : List.of("EURC", "EUROe", "EURQ", "USDQ", "EURCV", "EURI", "EURR", "EURe", "USDC", "USDR", "USDG")) {
            assertTrue(e.simboli().contains(s), "manca " + s);
        }
        assertEquals("2023-01-01", e.decorrenza(), "la stessa data che usava il pulsante dei token standard");
        assertTrue(Arrays.asList(MappeCausali.FILE_VARIE).contains(EmoneyToken.NOME), "installato e riallineato come gli altri");
        assertFalse(Files.readString(Path.of("config/varie/" + EmoneyToken.NOME + ".json")).contains("\"centralizzato\""),
                "un file centralizzato non ancora pubblicato verrebbe cancellato all'avvio");
    }

    @Test
    void unContenutoSenzaTokenOConDataSbagliataNonEValido() {
        assertNull(EmoneyToken.Interpreta("{\"decorrenza\":\"2023-01-01\",\"emoneyToken\":[]}", "prova"));
        assertNull(EmoneyToken.Interpreta("{\"decorrenza\":\"01/01/2023\",\"emoneyToken\":[{\"simbolo\":\"USDC\"}]}", "prova"));
        assertNull(EmoneyToken.Interpreta("non e' json", "prova"));
    }

    private static final EmoneyToken.Elenco ELENCO = new EmoneyToken.Elenco(List.of("USDC", "USDG", "EURe"), "2023-01-01");

    private static void movimento(Map<String, String[]> archivio, String id, String uscita, String tipoUscita,
            String entrata, String tipoEntrata) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[8] = uscita;
        v[9] = tipoUscita;
        v[11] = entrata;
        v[12] = tipoEntrata;
        archivio.put(id, v);
    }

    @Test
    void contaIMovimentiConUnTokenDellElencoAssenteDallaSezione() {
        Map<String, String[]> archivio = new TreeMap<>();
        movimento(archivio, "20261004122013_W_001_001_SC", "USDG", "Crypto", "aXlrUSDG", "Crypto");
        movimento(archivio, "20261009100116_W_001_001_DC", "", "", "USDG", "Crypto");
        movimento(archivio, "20250301100000_OKX_001_001_SC", "usdc", "Crypto", "BTC", "Crypto");   //minuscolo
        movimento(archivio, "20250301110000_OKX_001_001_AC", "EUR", "FIAT", "BTC", "Crypto");
        Principale.Mappa_EMoney.put("USDT", "2000-01-01");

        Map<String, Integer> m = EmoneyToken.MancantiNeiMovimenti(archivio, ELENCO);
        assertEquals(Map.of("USDG", 2, "USDC", 1), m);
        assertEquals("USDC", m.keySet().stream().filter("usdc"::equalsIgnoreCase).findFirst().orElseThrow(),
                "il nome e' quello dell'elenco, non quello del movimento");
    }

    @Test
    void unTokenGiaNellaSezioneNonEContatoNemmenoConMaiuscoleDiverse() {
        Map<String, String[]> archivio = new TreeMap<>();
        movimento(archivio, "20261009100116_W_001_001_DC", "", "", "EURE", "Crypto");
        Principale.Mappa_EMoney.put("EURe", "2023-01-01");
        assertTrue(EmoneyToken.MancantiNeiMovimenti(archivio, ELENCO).isEmpty());

        //Marcato case sensitive nella sezione: "EURE" non e' quel token, ed e' quindi ancora da proporre
        Principale.Mappa_EMoney_CaseSensitive.put("EURe", "SI");
        assertEquals(Map.of("EURe", 1), EmoneyToken.MancantiNeiMovimenti(archivio, ELENCO));
    }

    @Test
    void restanoFuoriGambeFiatTokenScamEMovimentiPrimaDellaDecorrenza() {
        Map<String, String[]> archivio = new TreeMap<>();
        movimento(archivio, "20261001100000_X_001_001_VC", "USDC", "FIAT", "", "");            //gamba FIAT
        movimento(archivio, "20261001110000_X_001_001_DC", "", "", "USDC **", "Crypto");        //marcato SCAM
        movimento(archivio, "20221231235959_X_001_001_SC", "USDC", "Crypto", "ETH", "Crypto");  //prima del 2023
        assertTrue(EmoneyToken.MancantiNeiMovimenti(archivio, ELENCO).isEmpty());
    }

    @Test
    void ilTestoDiceLEffettoEPortaLaData() {
        String t = Principale_EmoneyMancanti.Testo(new TreeMap<>(Map.of("USDG", 5, "USDC", 1)), "2023-01-01");
        assertTrue(t.contains("<b>USDG</b>: 5 movimenti"));
        assertTrue(t.contains("<b>USDC</b>: 1 movimento<"));
        assertTrue(t.contains("dal 01/01/2023"));
        assertTrue(t.contains("compresi quelli già dichiarati"));
        assertFalse(t.contains(";"), "niente punto e virgola nei testi per l'utente");
    }
}
