package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Il prezzo scelto dalla tabella di {@link GUI_ModificaPrezzo} marca il movimento come valorizzato,
 * anche quando vale {@code 0.00}: è il caso che restava fra gli errori "senza prezzo" ma, con il periodo
 * selezionato che non lo comprende, non compariva nel filtro relativo.
 */
class GUI_ModificaPrezzoApplicaPrezzoTest {

    /** Lo storico ha un buffer statico: ogni prova parte vuota e non lascia voci alle altre classi. */
    @BeforeEach
    @AfterEach
    void azzeraLoStorico() {
        MovimentiStorico.AzzeraBuffer();
    }

    private static String[] movimentoSenzaPrezzo() {
        String[] m = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < m.length; i++) m[i] = "";
        m[0] = "20240512062450_Test_001_001_PC";
        m[8] = "USDT";
        m[15] = "0.00";
        m[32] = "NO";
        return m;
    }

    @Test
    void zeroScelto_ilMovimentoNonRestaSenzaPrezzo() {
        String[] m = movimentoSenzaPrezzo();
        assertFalse(Prezzi.isMovimentoPrezzato(m.clone()), "premessa: prima della scelta e' senza prezzo");

        GUI_ModificaPrezzo.ApplicaPrezzoScelto(m, "0.00", "USDT|1652336690|0.00|Personalizzato");

        assertEquals("SI", m[32]);
        assertEquals("0.00", m[15]);
        assertEquals("USDT|1652336690|0.00|Personalizzato", m[40]);
        assertTrue(Prezzi.isMovimentoPrezzato(m), "uno zero confermato e' un prezzo, non un prezzo mancante");
    }

    @Test
    void prezzoDiversoDaZero_restaPrezzato() {
        String[] m = movimentoSenzaPrezzo();

        GUI_ModificaPrezzo.ApplicaPrezzoScelto(m, "12.34", "USDT|1652336690|1.00|Binance");

        assertEquals("SI", m[32]);
        assertEquals("12.34", m[15]);
        assertTrue(Prezzi.isMovimentoPrezzato(m));
    }

    @Test
    void ilPrezzoSceltoVaNelloStoricoConIlLignaggio() {
        String[] m = movimentoSenzaPrezzo();
        m[15] = "10.00";
        m[32] = "SI";
        m[40] = "USDT|1|10.00|Binance";

        GUI_ModificaPrezzo.ApplicaPrezzoScelto(m, "12.34", "USDT|2|12.34|Coingecko");

        assertEquals(1, MovimentiStorico.VociInAttesa());
        String lignaggio = m[MovimentiStorico.CAMPO_LIGNAGGIO];
        assertFalse(lignaggio.isBlank(), "il movimento porta il lignaggio della sua catena");
        //Il contenuto della riga di prima si legge dal database: lo prova MovimentiStoricoTest, che lo apre
        assertEquals("12.34", m[15]);
    }

    @Test
    void scegliereLoStessoPrezzoNonEUnaModifica() {
        String[] m = movimentoSenzaPrezzo();
        m[15] = "10.00";
        m[32] = "SI";
        m[40] = "USDT|1|10.00|Binance";

        GUI_ModificaPrezzo.ApplicaPrezzoScelto(m, "10.00", "USDT|1|10.00|Binance");

        assertEquals(0, MovimentiStorico.VociInAttesa());
        assertEquals("", m[MovimentiStorico.CAMPO_LIGNAGGIO]);
    }
}
