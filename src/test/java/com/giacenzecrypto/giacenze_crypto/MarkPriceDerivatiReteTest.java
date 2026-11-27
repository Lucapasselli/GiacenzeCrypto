package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova dal vivo di {@link MarkPriceDerivati}: gli endpoint pubblici di OKX e di Bybit rispondono nella forma attesa. Va in rete di proposito
 * ({@link GuardiaRete}), senza chiavi e senza dati dell'utente: chiede il BTC dell'ultimo minuto del 2025. Se la rete manca o un exchange non
 * risponde il test si salta, non fallisce.
 */
@Tag(GuardiaRete.TAG)
class MarkPriceDerivatiReteTest {

    /** 2025-12-31 23:59 UTC. */
    private static final long MINUTO = 1_767_225_540_000L;

    private static BigDecimal leggi(String piattaforma, String simbolo, boolean inverso) {
        MarkPriceDerivati.Esito e = MarkPriceDerivati.FONTE.leggi(piattaforma, simbolo, inverso, MINUTO);
        Assumptions.assumeTrue(e != null && e.certo(), piattaforma + " non ha risposto");
        return e.prezzo();
    }

    @Test
    void okxEBybitDannoLoStessoOrdineDiGrandezzaPerIlBtc() {
        BigDecimal okx = leggi("OKX", "BTC-USDT-SWAP", false);
        BigDecimal bybit = leggi("BYBIT", "BTCUSDT", false);
        assertNotNull(okx);
        assertNotNull(bybit);
        assertTrue(okx.compareTo(new BigDecimal("10000")) > 0 && okx.compareTo(new BigDecimal("500000")) < 0, "OKX " + okx);
        //I due mark price di uno stesso minuto stanno entro l'1%
        BigDecimal scarto = okx.subtract(bybit).abs().divide(okx, 6, java.math.RoundingMode.HALF_UP);
        assertTrue(scarto.compareTo(new BigDecimal("0.01")) < 0, "OKX " + okx + ", Bybit " + bybit);
    }

    @Test
    void bybitServeAncheLaCategoriaInversa() {
        BigDecimal inverso = leggi("BYBIT", "BTCUSD", true);
        assertNotNull(inverso);
        assertTrue(inverso.compareTo(new BigDecimal("10000")) > 0, "Bybit inverso " + inverso);
    }
}
