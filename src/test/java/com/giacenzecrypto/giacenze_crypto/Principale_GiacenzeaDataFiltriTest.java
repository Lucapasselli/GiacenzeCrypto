package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * La chiave dei filtri con cui "Giacenze a data" e' stata calcolata decide se la tabella mostrata corrisponde ancora ai
 * selettori in alto: se no si accende l'etichetta "premere CALCOLA" e la rettifica della quantità residua non parte.
 */
class Principale_GiacenzeaDataFiltriTest {

    @Test
    void chiaveUgualeSoloSeWalletSottoWalletEDataCoincidono() {
        String calcolata = Principale_GiacenzeaData.ChiaveFiltri("Tutti", "Tutti", "2025-12-31");
        assertEquals(calcolata, Principale_GiacenzeaData.ChiaveFiltri("Tutti", "Tutti", "2025-12-31"));
        //il caso della segnalazione: scelto un wallet ma calcolato con Tutti
        assertNotEquals(calcolata, Principale_GiacenzeaData.ChiaveFiltri("Binance", "Tutti", "2025-12-31"));
        assertNotEquals(calcolata, Principale_GiacenzeaData.ChiaveFiltri("Tutti", "Earn", "2025-12-31"));
        assertNotEquals(calcolata, Principale_GiacenzeaData.ChiaveFiltri("Tutti", "Tutti", "2025-12-30"));
    }

    @Test
    void maiuscoleSpaziEValoriMancantiNonCreanoDifferenze() {
        assertEquals(Principale_GiacenzeaData.ChiaveFiltri("Tutti", "Tutti", "2025-12-31"),
                Principale_GiacenzeaData.ChiaveFiltri(" tutti ", "TUTTI", "2025-12-31 "));
        assertEquals(Principale_GiacenzeaData.ChiaveFiltri("", "", ""),
                Principale_GiacenzeaData.ChiaveFiltri(null, null, null));
    }

    @Test
    void ilSeparatoreNonConfondeLeTreParti() {
        assertNotEquals(Principale_GiacenzeaData.ChiaveFiltri("a", "b|c", ""),
                Principale_GiacenzeaData.ChiaveFiltri("a|b", "c", ""));
    }
}
