package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del blocco "A bis" di {@link Prezzi#DammiPrezzoInfoTransazione}: gli EMoney token
 * denominati in euro (simbolo contenente "EUR", presenti in {@link Principale#Mappa_EMoney}
 * con data di decorrenza anteriore o uguale a quella del movimento) devono essere valorizzati
 * al cambio fisso 1:1 con l'euro, con fonte prezzo {@code "EURO"}.
 *
 * Sono testati solo i casi POSITIVI (quelli in cui il blocco restituisce subito il prezzo):
 * quando la condizione non è soddisfatta la funzione prosegue con la ricerca prezzi su
 * database/exchange, che non è eseguibile in un test unitario senza connessione.
 */
class PrezziEmoneyEuroTest {

    private static final long DATA_2024_06_01 = FunzioniDate.ConvertiDatainLong("2024-06-01");

    @BeforeEach
    void setUp() {
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney.put("EURe", "2024-01-01");
    }

    @AfterEach
    void tearDown() {
        Principale.Mappa_EMoney.clear();
    }

    private static Moneta moneta(String simbolo, String qta) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Qta = qta;
        m.Tipo = "Crypto";
        return m;
    }

    @Test
    void emoneyEuro_dataSuccessiva_valorizzatoUnoAUno() {
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(moneta("EURe", "10"), null, DATA_2024_06_01, null, "");

        assertNotNull(IP);
        assertEquals("EURO", IP.Fonte);
        assertEquals(0, BigDecimal.ONE.compareTo(IP.prezzoUnitario));
        assertEquals(0, new BigDecimal("10").compareTo(IP.prezzoQta));
        assertEquals("EURe", IP.Moneta);
        assertEquals(DATA_2024_06_01, IP.timestamp);
    }

    @Test
    void emoneyEuro_dataUgualeAllaDecorrenza_valorizzatoUnoAUno() {
        long dataDecorrenza = FunzioniDate.ConvertiDatainLong("2024-01-01");
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(moneta("EURe", "3.5"), null, dataDecorrenza, null, "");

        assertNotNull(IP);
        assertEquals("EURO", IP.Fonte);
        assertEquals(0, new BigDecimal("3.5").compareTo(IP.prezzoQta));
    }

    @Test
    void emoneyEuro_qtaNegativa_mantieneIlSegnoInQtaEValoreAssolutoInPrezzoQta() {
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(moneta("EURe", "-25"), null, DATA_2024_06_01, null, "");

        assertNotNull(IP);
        assertEquals(0, new BigDecimal("-25").compareTo(IP.Qta));
        assertEquals(0, new BigDecimal("25").compareTo(IP.prezzoQta));
    }

    @Test
    void emoneyEuro_haPrioritaSulSimboloPrioritarioAbbinato() {
        //Scambio EURe <-> BTC: il prezzo deve arrivare dall'EMoney in euro, non da BTC
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(moneta("BTC", "0.001"), moneta("EURe", "60"), DATA_2024_06_01, null, "");

        assertNotNull(IP);
        assertEquals("EURO", IP.Fonte);
        assertEquals("EURe", IP.Moneta);
        assertEquals(0, new BigDecimal("60").compareTo(IP.prezzoQta));
    }

    @Test
    void fiatEuro_restaPrioritarioSullEmoney() {
        //Il blocco FIAT EUR precede quello EMoney: con EUR fiat nello scambio vince il fiat
        Moneta fiat = moneta("EUR", "100");
        fiat.Tipo = "FIAT";
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(fiat, moneta("EURe", "100"), DATA_2024_06_01, null, "");

        assertNotNull(IP);
        assertEquals("", IP.Fonte);
        assertEquals("EUR", IP.Moneta);
    }
}
