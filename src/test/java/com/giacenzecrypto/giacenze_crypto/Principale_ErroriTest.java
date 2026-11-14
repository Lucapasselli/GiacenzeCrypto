package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Principale_Errori}: le voci del pulsante Errori. Solo quelle con almeno un errore, in un ordine fisso, e un tipo nuovo e'
 * una voce in piu' e non un pulsante in piu'.
 */
class Principale_ErroriTest {

    private static Principale_Errori.Contatori contatori(int nonClass, int prezzo, int lifo, int saldi, int collegati, int senzaColl, boolean plus) {
        return new Principale_Errori.Contatori(nonClass, prezzo, lifo, saldi, collegati, senzaColl, plus);
    }

    @Test
    void senzaErrori_nonCiSonoVoci_eTuttiITipiSonoSenzaProblemi() {
        Principale_Errori.Contatori c = contatori(0, 0, 0, 0, 0, 0, false);

        assertEquals(0, c.totale());
        assertTrue(Principale_Errori.Voci(c).isEmpty());
        assertEquals(6, Principale_Errori.TitoliSenzaProblemi(c).size());
    }

    @Test
    void siMostranoSoloLeVociConAlmenoUnErrore_nell_ordineFisso() {
        Principale_Errori.Contatori c = contatori(40, 6, 12, 2, 0, 3, true);

        List<Principale_Errori.Voce> v = Principale_Errori.Voci(c);

        assertEquals(List.of(Principale_Errori.ID_NON_CLASSIFICATI, Principale_Errori.ID_TRASFERIMENTI_SENZA_COLLEGAMENTO,
                Principale_Errori.ID_SENZA_PREZZO, Principale_Errori.ID_LIFO_MANCANTE, Principale_Errori.ID_GIACENZE_NEGATIVE),
                v.stream().map(Principale_Errori.Voce::id).toList());
        assertEquals(63, c.totale());
        assertEquals(List.of("movimenti collegati"), Principale_Errori.TitoliSenzaProblemi(c));
    }

    @Test
    void ognivoceHaIlSuoConteggioETestiNonVuoti() {
        for (Principale_Errori.Voce v : Principale_Errori.Voci(contatori(1, 1, 1, 1, 1, 1, true))) {
            assertEquals(1, v.quanti(), v.id());
            assertFalse(v.titolo().isBlank(), v.id());
            assertFalse(v.effetto().isBlank(), v.id());
            assertFalse(v.descrizione().isBlank(), v.id());
            //Regola dei testi per l'utente: niente punto e virgola
            assertFalse((v.titolo() + v.effetto() + v.descrizione()).contains(";"), v.id());
        }
    }

    @Test
    void ilTrasferimentoSenzaCollegamento_dicePerOgniOpzioneCheEffettoHa() {
        String acceso = Principale_Errori.Voci(contatori(0, 0, 0, 0, 0, 2, true)).get(0).effetto();
        String spento = Principale_Errori.Voci(contatori(0, 0, 0, 0, 0, 2, false)).get(0).effetto();

        assertNotEquals(acceso, spento);
        assertTrue(acceso.contains("costo di carico"));
        assertTrue(spento.contains("spente"));
    }

    @Test
    void ilIdentificativiSonoQuelliDelleCorrezioni() {
        assertEquals("nonClassificati", Principale_Errori.ID_NON_CLASSIFICATI);
        assertEquals("PrezzoMancante", Principale_Errori.ID_SENZA_PREZZO);
        assertEquals("LifoMancante", Principale_Errori.ID_LIFO_MANCANTE);
        assertEquals("GiacenzeNegative", Principale_Errori.ID_GIACENZE_NEGATIVE);
        assertEquals("MovimentiCollegati", Principale_Errori.ID_MOVIMENTI_COLLEGATI);
    }
}
