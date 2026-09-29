package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pesi delle gambe in uno scambio a più monete ({@link TransazioneDefi#AssegnaPesiaPartiTransazione}).
 * Caso reale: conversione dei piccoli saldi di Bybit del 2022, con il BIT ricevuto prezzato da un omonimo di
 * CoinMarketCap (valore quasi zero). Prima tutto il BIT finiva sulla prima moneta ceduta con un prezzo.
 */
class TransazioneDefiPesiTest {

    private static Moneta moneta(String simbolo, String qta, String prezzo) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Qta = qta;
        m.Tipo = "Crypto";
        m.Prezzo = prezzo;
        return m;
    }

    @Test
    void valoreTransazioneSottostimato_ipesiDelleMoneteCeduteRestanoProporzionali() {
        TransazioneDefi t = new TransazioneDefi();
        t.InserisciMoneteCEX(moneta("BIT", "5.04715341", "0.00002"), "Principale", "convert", "1");
        t.InserisciMoneteCEX(moneta("ACH", "-109.09", "1.31"), "Principale", "dust", "2");
        t.InserisciMoneteCEX(moneta("APEX", "-1.6796", "0.32"), "Principale", "dust", "3");
        t.InserisciMoneteCEX(moneta("SON", "-143.1", "0"), "Principale", "dust", "4");
        assertEquals("Scambio", t.IdentificaTipoTransazioneCEX());
        t.AssegnaPesiaPartiTransazione();

        BigDecimal ach = new BigDecimal(t.RitornaMappaTokenUscita().get("ACH").Peso);
        BigDecimal apex = new BigDecimal(t.RitornaMappaTokenUscita().get("APEX").Peso);
        assertTrue(ach.compareTo(BigDecimal.ONE) < 0, "ACH non puo' prendersi tutto il BIT: peso " + ach);
        assertEquals(0, ach.setScale(4, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal("0.8037")),
                "1,31 / (1,31 + 0,32)");
        assertEquals(0, apex.setScale(4, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal("0.1963")));
    }

    @Test
    void valoreTransazioneCorretto_nessunCambiamento() {
        //BIT prezzato bene (2 euro): la base resta il valore della transazione, e la moneta senza prezzo si
        //prende il residuo
        TransazioneDefi t = new TransazioneDefi();
        t.InserisciMoneteCEX(moneta("BIT", "5", "2.00"), "Principale", "convert", "1");
        t.InserisciMoneteCEX(moneta("ACH", "-109.09", "1.00"), "Principale", "dust", "2");
        t.InserisciMoneteCEX(moneta("SON", "-143.1", "0"), "Principale", "dust", "3");
        assertEquals("Scambio", t.IdentificaTipoTransazioneCEX());
        t.AssegnaPesiaPartiTransazione();

        assertEquals(0, new BigDecimal(t.RitornaMappaTokenUscita().get("ACH").Peso).compareTo(new BigDecimal("0.5")));
        assertEquals(0, new BigDecimal(t.RitornaMappaTokenUscita().get("SON").Peso).compareTo(new BigDecimal("0.5")));
    }
}
