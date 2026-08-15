package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Prezzi#DeduplicaMappaCoinMarketCap}: la lista di
 * {@code /v1/cryptocurrency/map} contiene token diversi con lo stesso simbolo (bug B9, segnalato
 * dall'utente su {@code USDF}), mentre in {@code GESTITICOINMARKETCAP} il simbolo è chiave
 * primaria. Qui si fissa quale dei doppioni sopravvive: quello con il rank migliore, e a parità
 * il primo.
 */
class PrezziMappaCoinMarketCapTest {

    private static JsonArray json(String testo) {
        return JsonParser.parseString(testo).getAsJsonArray();
    }

    private static Map<String, String> perSimbolo(List<String[]> righe) {
        return righe.stream().collect(Collectors.toMap(r -> r[0], r -> r[1]));
    }

    @Test
    void aSimboliDiversiNonScartaNulla() {
        List<String[]> gestiti = Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":1,"symbol":"BTC","rank":1,"is_active":1},
                 {"id":1027,"symbol":"ETH","rank":2,"is_active":1}]"""));

        assertEquals(2, gestiti.size());
        assertEquals(Map.of("BTC", "1", "ETH", "1027"), perSimbolo(gestiti));
    }

    @Test
    void suSimboloRipetutoVinceIlRankMigliorePursEArrivatoDopo() {
        List<String[]> gestiti = Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":900,"symbol":"USDF","rank":950,"is_active":1},
                 {"id":100,"symbol":"USDF","rank":120,"is_active":1}]"""));

        assertEquals(1, gestiti.size());
        assertEquals("100", perSimbolo(gestiti).get("USDF"));
    }

    @Test
    void ilTokenSenzaRankPerdeControUnoClassificato() {
        //campo assente
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":100,"symbol":"USDF","rank":120,"is_active":1},
                 {"id":900,"symbol":"USDF","is_active":1}]"""))).get("USDF"));

        //campo presente ma null
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":100,"symbol":"USDF","rank":120,"is_active":1},
                 {"id":900,"symbol":"USDF","rank":null,"is_active":1}]"""))).get("USDF"));

        //ordine invertito: il classificato arriva secondo e deve comunque vincere
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":900,"symbol":"USDF","is_active":1},
                 {"id":100,"symbol":"USDF","rank":120,"is_active":1}]"""))).get("USDF"));
    }

    @Test
    void aParitaDiRankVinceIlPrimoCioeLOrdineDellApi() {
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":100,"symbol":"USDF","rank":120,"is_active":1},
                 {"id":900,"symbol":"USDF","rank":120,"is_active":1}]"""))).get("USDF"));

        //nessuno dei due ha rank: si ricade sull'ordine di risposta, che è sort=cmc_rank
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":100,"symbol":"USDF","is_active":1},
                 {"id":900,"symbol":"USDF","is_active":1}]"""))).get("USDF"));
    }

    @Test
    void ilRankSiLeggeAncheDalCampoCmcRankDegliAltriEndpoint() {
        assertEquals("100", perSimbolo(Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":900,"symbol":"USDF","cmc_rank":950,"is_active":1},
                 {"id":100,"symbol":"USDF","cmc_rank":120,"is_active":1}]"""))).get("USDF"));
    }

    @Test
    void iTokenNonAttiviSonoEsclusiMaIlCampoAssenteValeComeAttivo() {
        List<String[]> gestiti = Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":1,"symbol":"BTC","rank":1,"is_active":1},
                 {"id":2,"symbol":"MORTO","rank":5000,"is_active":0},
                 {"id":3,"symbol":"SENZAFLAG","rank":300}]"""));

        assertEquals(Map.of("BTC", "1", "SENZAFLAG", "3"), perSimbolo(gestiti));
    }

    @Test
    void ilSimboloEMaiuscoloERipulitoDagliSpazi() {
        List<String[]> gestiti = Prezzi.DeduplicaMappaCoinMarketCap(json("""
                [{"id":100,"symbol":" usdf ","rank":120,"is_active":1},
                 {"id":900,"symbol":"USDF","rank":950,"is_active":1}]"""));

        assertEquals(1, gestiti.size(), "le due grafie sono lo stesso simbolo per la chiave primaria");
        assertEquals("100", perSimbolo(gestiti).get("USDF"));
    }

    @Test
    void listaVuota() {
        assertTrue(Prezzi.DeduplicaMappaCoinMarketCap(json("[]")).isEmpty());
    }
}
