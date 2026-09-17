package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova end-to-end di {@link Prezzi#ConvertiStablecoinInEuro}: la conversione usata per i punti
 * "grezzi" che {@code Historical_Multi_Eur.js} restituisce quando nessun exchange (Binance compreso)
 * ha un tasso di cambio EUR valido per quel minuto esatto (vedi CLAUDE.md, caso CRO/crypto.com del
 * 2026-09-17).
 *
 * <p>È un test di integrazione (chiama davvero CoinMarketCap e, tramite {@code CambioUSDEUR}, Banca
 * d'Italia): si auto-salta se la rete non risponde, stesso schema di {@code PrezziLottoCCXTTest}.
 */
class ConvertiStablecoinInEuroTest {

    @TempDir
    static Path tempDir;

    /** 2022-01-01 00:00 UTC: stessa finestra usata per verificare dal vivo il caso CRO/crypto.com. */
    private static final long ISTANTE = 1640995200000L;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        System.setProperty("prezzi.servizio.abilitato", "false");
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
        System.clearProperty("prezzi.servizio.abilitato");
    }

    @AfterEach
    void ripulisceInterruzione() {
        Interruzione.Azzera();
    }

    @Test
    void usdSiConvertePassandoSoloDaBancaDitalia() {
        BigDecimal risultato = Prezzi.ConvertiStablecoinInEuro(new BigDecimal("100"), "USD", ISTANTE);
        Assumptions.assumeTrue(risultato != null, "Banca d'Italia non ha risposto: prova saltata");
        //Cambio EUR/USD 2022-01-01 attorno a 1.13: 100 USD devono valere un po' meno di 100 EUR.
        assertTrue(risultato.compareTo(new BigDecimal("70")) > 0
                && risultato.compareTo(new BigDecimal("100")) < 0,
                "100 USD al 2022-01-01 dovrebbero valere un valore ragionevole di EUR, trovato " + risultato);
    }

    @Test
    void usdtSiConverteConCoinMarketCapEBancaDitalia() {
        BigDecimal risultato = Prezzi.ConvertiStablecoinInEuro(new BigDecimal("100"), "USDT", ISTANTE);
        Assumptions.assumeTrue(risultato != null, "CoinMarketCap o Banca d'Italia non hanno risposto: prova saltata");
        //USDT vale circa 1 USD: il risultato deve restare vicino a quello del test precedente.
        assertTrue(risultato.compareTo(new BigDecimal("70")) > 0
                && risultato.compareTo(new BigDecimal("100")) < 0,
                "100 USDT al 2022-01-01 dovrebbero valere un valore ragionevole di EUR, trovato " + risultato);
    }

    @Test
    void usdcSiConverteConCoinMarketCapEBancaDitalia() {
        BigDecimal risultato = Prezzi.ConvertiStablecoinInEuro(new BigDecimal("100"), "USDC", ISTANTE);
        Assumptions.assumeTrue(risultato != null, "CoinMarketCap o Banca d'Italia non hanno risposto: prova saltata");
        assertTrue(risultato.compareTo(new BigDecimal("70")) > 0
                && risultato.compareTo(new BigDecimal("100")) < 0,
                "100 USDC al 2022-01-01 dovrebbero valere un valore ragionevole di EUR, trovato " + risultato);
    }

    /** Legge il prezzo scritto in {@code PrezziNew} per (simbolo, exchange), o {@code null} se assente. */
    private static BigDecimal prezzoInCache(String simbolo, String exchange) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "SELECT prezzo FROM PrezziNew WHERE symbol = ? AND exchange = ? AND timestamp = ?")) {
            ps.setString(1, simbolo);
            ps.setString(2, exchange);
            ps.setLong(3, ISTANTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    @Test
    void unPuntoGrezzoVieneScrittoConLExchangeDiProvenienzaNonCoinMarketCap() throws Exception {
        //Riproduce l'esito che Historical_Multi_Eur.js produce per CRO su crypto.com quando nessun
        //exchange ha un tasso EUR/USDT valido per quel minuto: il prezzo c'è (0.5 USDT), la
        //conversione no, va convertito qui e scritto con l'exchange DI PROVENIENZA ("cryptocom"),
        //non "CoinMarketCap" (Decisione 1, 2026-09-17).
        JsonObject grezzo = new JsonObject();
        grezzo.addProperty("valore", 0.5);
        grezzo.addProperty("denom", "USDT");
        JsonObject grezzi = new JsonObject();
        grezzi.add("cryptocom", grezzo);
        JsonObject punto = new JsonObject();
        punto.addProperty("timestamp", ISTANTE);
        punto.add("prices", new JsonObject());
        punto.add("grezzi", grezzi);
        JsonArray punti = new JsonArray();
        punti.add(punto);

        Prezzi.ScriviPuntiPrezzoInCache("TESTGREZZO", punti);

        BigDecimal scritto = prezzoInCache("TESTGREZZO", "cryptocom");
        Assumptions.assumeTrue(scritto != null, "CoinMarketCap o Banca d'Italia non hanno risposto: prova saltata");
        assertTrue(scritto.compareTo(BigDecimal.ZERO) > 0 && scritto.compareTo(new BigDecimal("0.5")) < 0,
                "0.5 USDT al 2022-01-01 dovrebbero valere un po' meno di 0.5 EUR, trovato " + scritto);
        assertNull(prezzoInCache("TESTGREZZO", "CoinMarketCap"),
                "la riga non deve finire sotto la fonte CoinMarketCap ma sotto l'exchange di provenienza");
    }

    @Test
    void idCmcCablatiFunzionanoSenzaApiKeyConfigurata() {
        //Nessuna ApiKey_CoinMarketCap impostata in questo database temporaneo: la mappa dinamica
        //GestitiCoinMarketCap resta vuota, quindi solo la riserva ID_CMC_STABLECOIN puo' far
        //funzionare la conversione di USDT/USDC.
        assertNull(DatabaseH2.Opzioni_Leggi("ApiKey_CoinMarketCap"),
                "il test presuppone nessuna API key CoinMarketCap configurata");
        BigDecimal risultato = Prezzi.ConvertiStablecoinInEuro(new BigDecimal("1"), "USDT", ISTANTE);
        Assumptions.assumeTrue(risultato != null, "CoinMarketCap o Banca d'Italia non hanno risposto: prova saltata");
        assertTrue(risultato.compareTo(BigDecimal.ZERO) > 0);
    }
}
