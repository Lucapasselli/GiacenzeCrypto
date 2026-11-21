package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La tabella dei valori dei contratti OKX di {@code config/varie/ValoriContratti.json} e l'ordine in cui
 * {@link OKXDerivati#ValoreContratto(String, String)} la usa: tabella, rete, perpetuo della stessa famiglia.
 */
class ValoriContrattiTest {

    private static List<ValoriContratti.Regola> regoleVere() throws Exception {
        String testo = Files.readString(Path.of("config/varie/" + ValoriContratti.NOME + ".json"), StandardCharsets.UTF_8);
        List<ValoriContratti.Regola> r = ValoriContratti.Interpreta(testo, ValoriContratti.NOME);
        assertNotNull(r, "il file del repository deve essere valido");
        return r;
    }

    @BeforeEach
    void setUp() throws Exception {
        OKXDerivati.AzzeraCacheValore();
        ValoriContratti.ImpostaPerTest(regoleVere());
    }

    @AfterEach
    void ripristina() {
        OKXDerivati.FonteValoreContratto = id -> "";
        OKXDerivati.AzzeraCacheValore();
        ValoriContratti.ImpostaPerTest(null);
    }

    @Test
    void famigliaETipoDallInstId() {
        assertArrayEquals(new String[]{"BTC-USDT", "FUTURES"}, ValoriContratti.FamigliaETipo("BTC-USDT-250328"));
        assertArrayEquals(new String[]{"BTC-USDT", "SWAP"}, ValoriContratti.FamigliaETipo("BTC-USDT-SWAP"));
        assertArrayEquals(new String[]{"BTC-USD_UM", "FUTURES"}, ValoriContratti.FamigliaETipo("BTC-USD_UM-261127"));
        assertNull(ValoriContratti.FamigliaETipo("BTC-USD-261127-80000-C"), "le opzioni non sono gestite");
        assertNull(ValoriContratti.FamigliaETipo("BTC"));
    }

    @Test
    void unFuturesInUsdtPrendeIlValoreDelPeriodo() {
        OKXDerivati.FonteValoreContratto = id -> { throw new AssertionError("non serve la rete: " + id); };
        assertEquals("0.01", OKXDerivati.ValoreContratto("BTC-USDT-250328", "2025-01-10"));
        assertEquals("0.0001", OKXDerivati.ValoreContratto("BTC-USDT-200626", "2020-03-02"));
        assertEquals("0.01", OKXDerivati.ValoreContratto("BTC-USDT-200626", "2020-03-27"), "dal giorno del cambio vale il nuovo");
        assertEquals("0.0001", OKXDerivati.ValoreContratto("BTC-USDT-SWAP", "2020-03-19"));
    }

    @Test
    void unPerpetuoDiOggiSiChiedeAllaRete() {
        OKXDerivati.FonteValoreContratto = id -> "0.01";
        assertEquals("0.01", OKXDerivati.ValoreContratto("BTC-USDT-SWAP", "2024-05-01"));
    }

    @Test
    void unFuturesInversoNonElencatoPrendeIlValoreDelPerpetuoDellaStessaFamiglia() {
        OKXDerivati.FonteValoreContratto = id -> "ETH-USD-SWAP".equals(id) ? "10" : "";
        assertEquals("10", OKXDerivati.ValoreContratto("ETH-USD-250328", "2025-01-10"));
    }

    @Test
    void unFuturesAncoraElencatoUsaIlSuoValoreNonQuelloDelPerpetuo() {
        OKXDerivati.FonteValoreContratto = id -> "ETH-USD-SWAP".equals(id) ? "10" : "100";
        assertEquals("100", OKXDerivati.ValoreContratto("ETH-USD-270326", "2026-11-17"));
    }

    /**
     * Se OKX non risponde per il futures il valore del perpetuo non lo sostituisce: verrebbe scritto per sempre sul movimento anche
     * se il futures è ancora elencato con un valore diverso. Si riprova al prossimo import, e se poi OKX dice che non lo elenca vale
     * il perpetuo.
     */
    @Test
    void conUnErroreDiReteSulFuturesNonSiPrendeIlPerpetuo() {
        String[] risposta = {null};
        OKXDerivati.FonteValoreContratto = id -> "ETH-USD-SWAP".equals(id) ? "10" : risposta[0];
        assertEquals("10", OKXDerivati.ValoreContratto("ETH-USD-SWAP", "2025-01-10"), "il perpetuo e' gia' in memoria");
        assertEquals("", OKXDerivati.ValoreContratto("ETH-USD-250328", "2025-01-10"));
        OKXDerivati.FonteValoreContratto = id -> { throw new IllegalStateException("rete"); };
        assertEquals("", OKXDerivati.ValoreContratto("ETH-USD-250328", "2025-01-10"));
        risposta[0] = "";
        OKXDerivati.FonteValoreContratto = id -> "ETH-USD-SWAP".equals(id) ? "10" : risposta[0];
        assertEquals("10", OKXDerivati.ValoreContratto("ETH-USD-250328", "2025-01-10"), "OKX ha risposto: non elencato");
    }

    @Test
    void unaFamigliaNuovaNonPrendeIlValoreDiUnAltraFamiglia() {
        //ETH-USD_UM (0,001 ETH) non e' ETH-USD (10 dollari) ne' ETH-USDT (0,1 ETH): nessuna regola, solo la rete
        OKXDerivati.FonteValoreContratto = id -> "";
        assertEquals("", OKXDerivati.ValoreContratto("ETH-USD_UM-250328", "2025-01-10"));
        assertEquals("", OKXDerivati.ValoreContratto("BTC-USD_UM_XPERP-991231", "2025-01-10"));
    }

    @Test
    void senzaGiornoNonValgonoLeRegoleConUnPeriodo() {
        assertNull(ValoriContratti.Cerca("BTC-USDT-SWAP", null));
        assertTrue(ValoriContratti.Cerca("SOL-USDT-250328", null).comeSwap());
    }

    @Test
    void unaRegolaNonValidaFaUsareLaCopiaNelJar() {
        assertNull(ValoriContratti.Interpreta("{\"okx\":[{\"famiglia\":\"BTC-USDT\",\"tipo\":\"FUTURES\",\"valore\":\"zero\"}]}", "x"));
        assertNull(ValoriContratti.Interpreta("{\"okx\":[{\"famiglia\":\"BTC-USDT\",\"tipo\":\"SWAP\",\"comeSwap\":true}]}", "x"));
        assertNull(ValoriContratti.Interpreta("{\"okx\":[{\"famiglia\":\"BTC-USDT\",\"tipo\":\"FUTURES\",\"valore\":\"1\",\"dal\":\"27/03/2020\"}]}", "x"));
        assertNull(ValoriContratti.Interpreta("{\"okx\":[{\"famiglia\":\"BTC-USDT\",\"tipo\":\"FUTURES\",\"valore\":\"-1\"}]}", "x"));
    }
}
