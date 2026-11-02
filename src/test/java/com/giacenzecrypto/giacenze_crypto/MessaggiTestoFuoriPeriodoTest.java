package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Nota del dialogo degli errori sui movimenti fuori dal periodo selezionato ({@link Messaggi#TestoFuoriPeriodo}). */
class MessaggiTestoFuoriPeriodoTest {

    @Test
    void nessunoFuoriPeriodo_testoVuoto() {
        assertEquals("", Messaggi.TestoFuoriPeriodo(0, 0));
    }

    @Test
    void soloSenzaPrezzo_neNominaUnoSolo() {
        String t = Messaggi.TestoFuoriPeriodo(6, 0);
        assertTrue(t.contains("6 transazioni senza prezzo"), t);
        assertFalse(t.contains("LIFO"), t);
    }

    @Test
    void singolare() {
        assertTrue(Messaggi.TestoFuoriPeriodo(1, 0).contains("1 transazione senza prezzo"));
        assertTrue(Messaggi.TestoFuoriPeriodo(0, 1).contains("1 movimento con parte del LIFO mancante"));
    }

    @Test
    void elenco_vuotoSeZero() {
        assertEquals("", Messaggi.ElencoFuoriPeriodo(0, 0));
        assertEquals("2 movimenti con parte del LIFO mancante", Messaggi.ElencoFuoriPeriodo(0, 2));
    }

    @Test
    void entrambi_congiunti() {
        String t = Messaggi.TestoFuoriPeriodo(6, 2);
        assertTrue(t.contains("6 transazioni senza prezzo e 2 movimenti con parte del LIFO mancante"), t);
        assertFalse(t.contains(";"), "niente punto e virgola nei testi per l'utente");
    }
}
