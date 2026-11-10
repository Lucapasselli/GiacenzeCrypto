package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** {@link GUI_ClassificazioneMovimento#ValoriVicini}: evidenziazione dei candidati dello scambio differito per valore in euro. */
class ValoriViciniScambioDifferitoTest {

    private static final BigDecimal CINQUE = new BigDecimal("5");

    @Test
    void dentroELimiteDelCinquePerCento() {
        assertTrue(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "100.00", CINQUE));
        assertTrue(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "95.00", CINQUE));
        assertTrue(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "105.00", CINQUE));
    }

    @Test
    void oltreIlLimite() {
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "94.99", CINQUE));
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "105.01", CINQUE));
    }

    @Test
    void ilSegnoNonConta() {
        assertTrue(GUI_ClassificazioneMovimento.ValoriVicini("-100.00", "98.50", CINQUE));
        assertTrue(GUI_ClassificazioneMovimento.ValoriVicini("100.00", "-98.50", CINQUE));
    }

    @Test
    void valoriMancantiONonNumericiNonEvidenziano() {
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("0.00", "0.00", CINQUE));
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("", "10", CINQUE));
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("100", null, CINQUE));
        assertFalse(GUI_ClassificazioneMovimento.ValoriVicini("100", "abc", CINQUE));
    }
}
