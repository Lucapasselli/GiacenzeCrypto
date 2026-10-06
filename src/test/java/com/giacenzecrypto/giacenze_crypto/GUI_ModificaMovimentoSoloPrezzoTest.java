package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa la modalità «solo prezzo e note» di {@link GUI_ModificaMovimento}: un movimento generato dalla
 * classificazione ({@code AU}) o che fa parte di un gruppo ({@code [20]} non vuoto) si modifica solo in valore,
 * note e informazioni sul prezzo, senza sciogliere il gruppo e senza toccare quantità, monete, wallet o data.
 */
class GUI_ModificaMovimentoSoloPrezzoTest {

    @BeforeEach
    void azzeraStorico() {
        MovimentiStorico.AzzeraBuffer();
    }

    private static String[] movimento(String ID, String Auto, String Collegati) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2022-05-24 08:33";
        v[3] = "Binance";
        v[4] = "Dual Savings";
        v[8] = "USDT";
        v[9] = "Crypto";
        v[10] = "-102";
        v[15] = "102.00";
        v[18] = "PTW - Trasferimento Interno";
        v[20] = Collegati;
        v[21] = "nota vecchia";
        v[22] = Auto;
        v[40] = "USDT|1653380000000|1|binance";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    @Test
    void generatoOCollegato_soloPrezzoENote_altrimentiTutto() {
        assertTrue(GUI_ModificaMovimento.SoloPrezzoENote(movimento("20220524083341_Binance_0002_001_PC", "AU", "")),
                "un generato, anche senza collegamenti (forma vecchia dei Dual)");
        assertTrue(GUI_ModificaMovimento.SoloPrezzoENote(movimento("20220524083341_Binance_002_001_DC", "A",
                "20220524083341_Binance_0002_001_PC")), "un importato classificato");
        assertTrue(GUI_ModificaMovimento.SoloPrezzoENote(movimento("20220524083341_Binance_002_001_DC", "M",
                "20220524083341_Binance_0002_001_PC")), "un manuale classificato: è il buco della tabella RW");
        assertFalse(GUI_ModificaMovimento.SoloPrezzoENote(movimento("20220524083341_Binance_002_001_DC", "A", "")));
        assertFalse(GUI_ModificaMovimento.SoloPrezzoENote(movimento("20220524083341_Binance_002_001_DC", "M", "")));
        assertFalse(GUI_ModificaMovimento.SoloPrezzoENote(null));
    }

    @Test
    void laModifica_cambiaSoloValoreNotePrezzoELignaggio() {
        String[] v = movimento("20220524083341_Binance_0002_001_PC", "AU", "20220524083341_Binance_002_001_DC");
        String[] prima = v.clone();

        GUI_ModificaMovimento.ScriviSoloPrezzoENote(v, "110.00", "nota nuova", true, "USDT|1653380000000|1.08|binance");

        assertEquals("110.00", v[15]);
        assertEquals("nota nuova", v[21]);
        assertEquals("SI", v[32]);
        assertEquals("USDT|1653380000000|1.08|binance", v[40]);
        assertFalse(v[42].isBlank(), "il lignaggio lega la modifica allo storico");
        for (int i = 0; i < v.length; i++) {
            if (i == 15 || i == 21 || i == 32 || i == 40 || i == 42) continue;
            assertEquals(prima[i], v[i], "campo " + i + " cambiato");
        }
    }
}
