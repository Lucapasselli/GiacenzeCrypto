package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link CcxtInterop#convertBinanceStaking}: la risposta di {@code Scripts/Binance_Staking.js} (staking SOL ed
 * ETH, RWUSD, Tax Payment di Binance Pay) prende le stesse forme che l'import del CSV dà alle stesse operazioni.
 * Valori sintetici. La conversione gira senza ricerca prezzi ({@link Prezzi#SenzaValorizzare}).
 */
class CcxtInteropBinanceStakingTest {

    /** 2026-01-01 00:00:00 UTC: la data di partenza dello scaricamento. */
    private static final long INIZIO = 1767225600000L;
    private static final long ORA = 3600000L;
    private static final long GIORNO = 24 * ORA;

    private static List<String[]> converti(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        return Prezzi.SenzaValorizzare(() -> CcxtInterop.convertBinanceStaking(o, "Binance"));
    }

    private static String categoria(String[] r) {
        return r[0].substring(r[0].lastIndexOf('_') + 1);
    }

    @Test
    void loStakingEUnoScambioADueGambe() {
        List<String[]> l = converti("""
            {"inizio": %d, "solStaking": [{"time": %d, "asset": "SOL", "amount": "2", "distributeAsset": "BNSOL",
              "distributeAmount": "1.8", "status": "SUCCESS"}],
             "rwusdSottoscrizioni": [{"time": %d, "asset": "USDC", "amount": "100", "receiveAsset": "RWUSD",
              "receiveAmount": "100", "status": "SUCCESS"}]}
            """.formatted(INIZIO, INIZIO + ORA, INIZIO + 2 * ORA));
        assertEquals(2, l.size());
        assertEquals("SOL", l.get(0)[8]);
        assertEquals("-2", l.get(0)[10]);
        assertEquals("BNSOL", l.get(0)[11]);
        assertEquals("1.8", l.get(0)[13]);
        assertEquals("SC", categoria(l.get(0)));
        assertEquals("SOL Staking - Purchase (API)", l.get(0)[7]);
        assertEquals("RWUSD", l.get(1)[11]);
        assertEquals("A", l.get(1)[39]);
    }

    /**
     * Il riscatto di BNSOL o RWUSD sono due righe a una gamba, come "Deduction"/"Distribute" del CSV. Si importa solo
     * se l'accredito è nuovo: richiesto prima della partenza e accreditato dopo (era in corso all'ultimo scaricamento)
     * entra per intero, accreditato prima no (è già in archivio, e reimportarlo annullerebbe lo scambio differito).
     */
    @Test
    void ilRiscattoSiImportaSoloSeLAccreditoENuovo() {
        List<String[]> l = converti("""
            {"inizio": %d, "solRiscatti": [
              {"time": %d, "arrivalTime": %d, "asset": "BNSOL", "amount": "1", "distributeAsset": "SOL",
               "distributeAmount": "1.1", "status": "SUCCESS"},
              {"time": %d, "arrivalTime": %d, "asset": "BNSOL", "amount": "5", "distributeAsset": "SOL",
               "distributeAmount": "5.5", "status": "SUCCESS"},
              {"time": %d, "arrivalTime": %d, "asset": "BNSOL", "amount": "7", "distributeAsset": "SOL",
               "distributeAmount": "7.7", "status": "PENDING"}]}
            """.formatted(INIZIO, INIZIO - 2 * GIORNO, INIZIO + 2 * GIORNO,
                INIZIO - 9 * GIORNO, INIZIO - 5 * GIORNO,
                INIZIO + GIORNO, INIZIO + 4 * GIORNO));
        assertEquals(2, l.size(), "solo il riscatto accreditato dopo la partenza e completato");
        assertEquals("BNSOL", l.get(0)[8]);
        assertEquals("-1", l.get(0)[10]);
        assertEquals("", l.get(0)[11]);
        assertEquals("PC", categoria(l.get(0)));
        assertEquals("SOL", l.get(1)[11]);
        assertEquals("1.1", l.get(1)[13]);
        assertEquals("DC", categoria(l.get(1)));
    }

    /** Il riscatto di WBETH è uno scambio unico all'accredito, come "WBETH2.0 - Redemption Unfreeze and Transfer". */
    @Test
    void ilRiscattoDiWbethEUnoScambioAllAccredito() {
        long arrivo = INIZIO + 5 * GIORNO;
        List<String[]> l = converti("""
            {"inizio": %d, "ethRiscatti": [{"time": %d, "arrivalTime": %d, "asset": "WBETH", "amount": "0.9",
              "distributeAsset": "ETH", "distributeAmount": "1", "status": "SUCCESS"}]}
            """.formatted(INIZIO, INIZIO + GIORNO, arrivo));
        assertEquals(1, l.size());
        assertEquals("WBETH", l.get(0)[8]);
        assertEquals("ETH", l.get(0)[11]);
        assertEquals(FunzioniDate.ConvertiDatadaLongAlSecondo(arrivo).substring(0, 16), l.get(0)[1]);
    }

    /** Il Tax Payment è un cash out della moneta usata, al valore in euro del pagamento; gli altri Pay si segnalano. */
    @Test
    void taxPaymentDiventaCashOutAlValoreInEuro() {
        String json = """
            {"inizio": %d, "nonDisponibili": {"rwusd/history/subscriptionHistory": "non disponibile"},
             "pay": [{"orderType": "TAX", "transactionTime": %d, "amount": "-11.9", "currency": "EUR",
                      "fundsDetail": [{"currency": "USDC", "amount": "13.5"}]},
                     {"orderType": "C2C", "transactionTime": %d, "amount": "5", "currency": "USDT"}]}
            """.formatted(INIZIO, INIZIO + ORA, INIZIO + 2 * ORA);
        List<String[]> l = converti(json);
        assertEquals(1, l.size());
        assertEquals("USDC", l.get(0)[8]);
        assertEquals("-13.5", l.get(0)[10]);
        assertEquals("PC", categoria(l.get(0)));
        assertTrue(l.get(0)[18].startsWith("PCO"), l.get(0)[18]);
        assertEquals("11.90", l.get(0)[15]);

        List<String> avvisi = CcxtInterop.AvvisiBinanceStaking(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(2, avvisi.size());
        assertTrue(avvisi.get(0).contains("rwusd/history/subscriptionHistory"));
        assertTrue(avvisi.get(1).contains("C2C") && avvisi.get(1).endsWith(": 1"));
    }
}
