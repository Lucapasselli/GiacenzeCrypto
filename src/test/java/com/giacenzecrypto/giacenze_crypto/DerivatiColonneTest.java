package com.giacenzecrypto.giacenze_crypto;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le colonne "Derivato" e "Risultato derivati" della tabella dei movimenti e il totale che accompagna l'etichetta delle righe. */
class DerivatiColonneTest {

    private static String[] riga(String tipo, String risultato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[Derivati.CAMPO] = tipo;
        v[Derivati.CAMPO_RISULTATO] = risultato;
        return v;
    }

    @Test
    void lColonnaDerivatoELMarcatoreEQuellaDelRisultatoELEuroConDueDecimali() {
        assertEquals("FUNDING", Derivati.ColonnaDerivato(riga("FUNDING", "")));
        assertEquals("", Derivati.ColonnaDerivato(riga("", "")));
        assertEquals("-0.06", Derivati.ColonnaRisultato(riga("CONTRATTO", "-0.0625")));
        assertEquals("4500.00", Derivati.ColonnaRisultato(riga("CONTRATTO", "4500")));
        assertEquals("", Derivati.ColonnaRisultato(riga("PNL", "")));
        assertEquals("", Derivati.ColonnaRisultato(riga("", "non un numero")));
    }

    @Test
    void ilTotaleSommaSoloIRisultatiPresentiEDiceQuantiSono() {
        List<String[]> righe = List.of(riga("CONTRATTO", "4500.00"), riga("PNL", "0.00"), riga("COMMISSIONE", "-1.90"),
                riga("MARGINE", ""), riga("", ""));
        Object[] t = Derivati.TotaleRisultato(righe);
        assertEquals(0, new java.math.BigDecimal("4498.10").compareTo((java.math.BigDecimal) t[0]));
        assertEquals(3, t[1]);
    }

    @Test
    void senzaNessunRisultatoLaSommaENulla() {
        assertNull(Derivati.TotaleRisultato(List.of(riga("MARGINE", ""), riga("", "")))[0]);
        assertNull(Derivati.TotaleRisultato(null)[0]);
    }

    @Test
    void ilLayoutDellaTabellaConosceLeDueColonneNuove() {
        assertEquals(44, LayoutColonneMovimenti.COLONNA_MASSIMA);
        assertEquals(LayoutColonneMovimenti.COLONNA_MASSIMA + 1 + Principale_GiacenzeaData.DETTAGLIO_INIZIO_MOVIMENTI, Principale_GiacenzeaData.DETTAGLIO_COLONNE);
    }
}
