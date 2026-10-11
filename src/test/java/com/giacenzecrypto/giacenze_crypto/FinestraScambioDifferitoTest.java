package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Finestra dei candidati nella classificazione "scambio crypto differito": il filtro dei giorni deve
 * valere nel verso giusto (il deposito segue il prelievo) con 24 ore di tolleranza dall'altra parte.
 */
class FinestraScambioDifferitoTest {

    private static final long GIORNO = 86400000L;
    private static final long ORA = 1_700_000_000_000L;

    @Test
    void conUnPrelievoAttualeSiCercanoDepositiDa24OrePrimaAiGiorniScelti() {
        long[] f = GUI_ClassificazioneMovimento.FinestraScambioDifferito(true, ORA, 15 * GIORNO);
        assertArrayEquals(new long[]{ORA - GIORNO, ORA + 15 * GIORNO}, f);
    }

    @Test
    void conUnDepositoAttualeSiCercanoPrelieviDaiGiorniSceltiFinoA24OreDopo() {
        long[] f = GUI_ClassificazioneMovimento.FinestraScambioDifferito(false, ORA, 15 * GIORNO);
        assertArrayEquals(new long[]{ORA - 15 * GIORNO, ORA + GIORNO}, f);
    }

    @Test
    void ilLimiteDeiGiorniNonSiEstendeDi24Ore() {
        long[] f = GUI_ClassificazioneMovimento.FinestraScambioDifferito(true, ORA, GIORNO);
        assertEquals(ORA + GIORNO, f[1]);
        assertTrue(ORA + GIORNO + 60_000L > f[1], "un deposito a 1 giorno e 1 minuto è fuori da un limite di 1 giorno");
    }
}
