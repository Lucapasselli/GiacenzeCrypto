package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link CcxtInterop#convertDepositi} e {@link CcxtInterop#convertPrelievi} sullo stato restituito
 * da Binance ({@code Scripts/depositi.js}, {@code Scripts/prelievi.js}): un deposito respinto o un prelievo
 * annullato, respinto o fallito non ha mosso fondi e non deve diventare un movimento. Quelli ancora in corso
 * si importano, perché lo scaricamento successivo parte dopo l'ultimo movimento e non li ritroverebbe.
 * La conversione gira senza ricerca prezzi ({@link Prezzi#SenzaValorizzare}).
 */
class CcxtInteropBinanceStatiTest {

    private static JsonArray righe(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    @Test
    void iDepositiRespintiOSbagliatiNonSiImportano() {
        List<String[]> lista = Prezzi.SenzaValorizzare(() -> CcxtInterop.convertDepositi(righe("""
            [{"id":"d1","coin":"ETH","amount":"0.5","status":"1","insertTime":"1767225600000","address":"0xabc","txId":"t1"},
             {"id":"d2","coin":"ETH","amount":"0.6","status":"2","insertTime":"1767229200000","address":"0xabc","txId":"t2"},
             {"id":"d3","coin":"ETH","amount":"0.7","status":"7","insertTime":"1767232800000","address":"0xabc","txId":"t3"},
             {"id":"d4","coin":"ETH","amount":"0.8","status":"0","insertTime":"1767236400000","address":"0xabc","txId":"t4"}]
            """), "Binance"));

        assertEquals(2, lista.size());
        assertEquals("0.5", lista.get(0)[13]);
        //In attesa (status 0): si importa
        assertEquals("0.8", lista.get(1)[13]);
    }

    @Test
    void iPrelieviAnnullatiRespintiOFallitiNonSiImportano() {
        List<String[]> lista = Prezzi.SenzaValorizzare(() -> CcxtInterop.convertPrelievi(righe("""
            [{"id":"w1","coin":"BTC","amount":"0.1","transactionFee":"0.0001","status":"6","insertTime":"1767225600000","address":"bc1x","txId":"t1"},
             {"id":"w2","coin":"BTC","amount":"0.2","transactionFee":"0.0001","status":"1","insertTime":"1767229200000","address":"bc1x","txId":"t2"},
             {"id":"w3","coin":"BTC","amount":"0.3","transactionFee":"0.0001","status":"3","insertTime":"1767232800000","address":"bc1x","txId":"t3"},
             {"id":"w4","coin":"BTC","amount":"0.4","transactionFee":"0.0001","status":"5","insertTime":"1767236400000","address":"bc1x","txId":"t4"},
             {"id":"w5","coin":"BTC","amount":"0.5","transactionFee":"0.0001","status":"4","insertTime":"1767240000000","address":"bc1x","txId":"t5"}]
            """), "Binance"));

        //Restano il completato (6) e quello in lavorazione (4), con l'eventuale riga della commissione
        assertTrue(lista.stream().anyMatch(r -> r[10].equals("-0.1")), "manca il prelievo completato");
        assertTrue(lista.stream().anyMatch(r -> r[10].equals("-0.5")), "manca il prelievo in lavorazione");
        for (String q : new String[]{"-0.2", "-0.3", "-0.4"}) {
            assertTrue(lista.stream().noneMatch(r -> r[10].equals(q)), "importato un prelievo non andato a buon fine: " + q);
        }
    }
}
