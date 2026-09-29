package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della mappa di CoinMarketCap ({@link Prezzi#ElencoMappaCoinMarketCap}) e della scelta fra omonimi
 * ({@link Prezzi#ScegliOmonimo}). Fino al 2026-09-29 la mappa teneva un solo id per simbolo, quello
 * col rank migliore di oggi: BIT puntava a Biconomy Exchange Token invece che a BitDAO, e i prezzi storici
 * erano quelli di un'altra moneta. Ora la mappa tiene tutti gli omonimi e la scelta si fa alla data, per volume.
 */
class PrezziMappaCoinMarketCapTest {

    private static JsonArray json(String testo) {
        return JsonParser.parseString(testo).getAsJsonArray();
    }

    @Test
    void simboliDiversi_unaRigaCiascuno() {
        List<String[]> righe = Prezzi.ElencoMappaCoinMarketCap(json("""
                [{"id":1,"name":"Bitcoin","symbol":"BTC","rank":1,"is_active":1},
                 {"id":1027,"name":"Ethereum","symbol":"ETH","rank":2,"is_active":1}]"""));
        assertEquals(2, righe.size());
        assertArrayEquals(new String[]{"BTC", "1", "Bitcoin", "1"}, righe.get(0));
    }

    @Test
    void omonimi_tenutiTuttiConNomeERank() {
        List<String[]> righe = Prezzi.ElencoMappaCoinMarketCap(json("""
                [{"id":11500,"name":"Biconomy Exchange Token","symbol":"BIT","rank":4096,"is_active":1},
                 {"id":11221,"name":"BitDAO","symbol":"BIT","rank":5046,"is_active":1}]"""));
        assertEquals(2, righe.size(), "con un solo id per simbolo BitDAO spariva");
        assertEquals("BitDAO", righe.get(1)[2]);
        assertEquals("5046", righe.get(1)[3]);
    }

    @Test
    void laStessaMonetaRipetuta_unaRigaSola() {
        assertEquals(1, Prezzi.ElencoMappaCoinMarketCap(json("""
                [{"id":100,"symbol":"USDF","rank":120,"is_active":1},
                 {"id":100,"symbol":" usdf ","rank":120,"is_active":1}]""")).size());
    }

    @Test
    void rankAssenteONullo_inFondo() {
        List<String[]> righe = Prezzi.ElencoMappaCoinMarketCap(json("""
                [{"id":900,"symbol":"USDF","is_active":1},
                 {"id":901,"symbol":"USDF","rank":null,"is_active":1},
                 {"id":100,"symbol":"USDF","cmc_rank":120,"is_active":1}]"""));
        assertEquals(String.valueOf(Integer.MAX_VALUE), righe.get(0)[3]);
        assertEquals(String.valueOf(Integer.MAX_VALUE), righe.get(1)[3]);
        assertEquals("120", righe.get(2)[3], "il rank si legge anche da cmc_rank");
    }

    @Test
    void iTokenNonAttiviSonoEsclusiMaIlCampoAssenteValeComeAttivo() {
        List<String[]> righe = Prezzi.ElencoMappaCoinMarketCap(json("""
                [{"id":1,"symbol":"BTC","rank":1,"is_active":1},
                 {"id":2,"symbol":"MORTO","rank":5000,"is_active":0},
                 {"id":3,"symbol":"SENZAFLAG","rank":300}]"""));
        assertEquals(List.of("BTC", "SENZAFLAG"), righe.stream().map(r -> r[0]).toList());
    }

    @Test
    void listaVuota() {
        assertTrue(Prezzi.ElencoMappaCoinMarketCap(json("[]")).isEmpty());
    }

    // ---------------------------------------------------------------- scelta fra omonimi

    private static Prezzi.CandelaCmc candela(double volume) {
        return new Prezzi.CandelaCmc(1642611600000L, 1.0, volume);
    }

    @Test
    void sceltaPerVolume_vinceIlVolumeTotalePiuAlto() {
        //Il caso del 19/01/2022: Biconomy ha il rank migliore oggi ma un volume 500 volte piu' basso
        Map<Integer, List<Prezzi.CandelaCmc>> storici = new LinkedHashMap<>();
        storici.put(11500, List.of(candela(196_777), candela(197_254)));
        storici.put(11221, List.of(candela(103_372_265), candela(106_366_486)));
        assertEquals(11221, Prezzi.ScegliOmonimo(storici));
    }

    @Test
    void sceltaPerCapitalizzazione_primaDelVolume() {
        //Il caso di aprile-maggio 2022: Biconomy ha un volume dichiarato piu' alto di BitDAO ma capitalizzazione 0
        Map<Integer, List<Prezzi.CandelaCmc>> storici = new LinkedHashMap<>();
        storici.put(11500, List.of(new Prezzi.CandelaCmc(1649761200000L, 0.0000145, 54_805_248, 0)));
        storici.put(11221, List.of(new Prezzi.CandelaCmc(1649761200000L, 1.156, 32_139_667, 687_571_453)));
        assertEquals(11221, Prezzi.ScegliOmonimo(storici));
    }

    @Test
    void sceltaPerVolume_aParitaVinceIlPrimo() {
        Map<Integer, List<Prezzi.CandelaCmc>> storici = new LinkedHashMap<>();
        storici.put(1, List.of(candela(10)));
        storici.put(2, List.of(candela(10)));
        assertEquals(1, Prezzi.ScegliOmonimo(storici));
    }

    @Test
    void rispostaStorico_leggePrezzoEVolumeEScartaIPrezziAZero() {
        List<Prezzi.CandelaCmc> c = Prezzi.CandeleDaRispostaCoinMarketCap("""
                {"data":{"id":11221,"name":"BitDAO","symbol":"BIT","quotes":[
                  {"timeOpen":"2022-01-19T18:00:00.000Z","quote":{"open":1.97,"close":1.973,"volume":107540344.97,"marketCap":1157237702.98}},
                  {"timeOpen":"2022-01-19T19:00:00.000Z","quote":{"open":0,"volume":5}},
                  {"timeOpen":"2022-01-19T20:00:00.000Z","quote":{"open":1.95}}]}}""");
        assertEquals(2, c.size());
        assertEquals(1642615200000L, c.get(0).ms);
        assertEquals(107540344.97, c.get(0).volume, 0.001);
        assertEquals(0, c.get(1).volume, 0.0, "volume assente = 0");
        assertEquals(1157237702.98, c.get(0).marketCap, 0.001);
        assertEquals(0, c.get(1).marketCap, 0.0, "capitalizzazione assente = 0");
    }

    @Test
    void etichette_nomeDellaMonetaFraParentesi_eNonSuperanoLaColonnaDellaCache() {
        assertEquals("CoinMarketCap (BitDAO)", Prezzi.EtichettaCmc("BitDAO"));
        assertEquals("CoinMarketCap", Prezzi.EtichettaCmc(" "), "senza nome resta la fonte nuda");
        assertEquals("CoinMarketCap omonimo (Biconomy Exchange Token)", Prezzi.EtichettaOmonimoCmc("Biconomy Exchange Token"));
        assertEquals("CoinMarketCap (AB)", Prezzi.EtichettaCmc("A;B|"), "niente separatori del file movimenti");
        String lunga = Prezzi.EtichettaOmonimoCmc("x".repeat(300));
        assertTrue(lunga.length() <= 100, "exchange e' VARCHAR(100): " + lunga.length());
        assertTrue(lunga.endsWith(")"));
    }
}
