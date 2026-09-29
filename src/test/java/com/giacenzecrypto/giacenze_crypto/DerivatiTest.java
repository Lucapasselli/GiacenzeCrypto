package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Caratterizza {@link Derivati}: il marcatore del campo 44 e gli avvisi per anno che finiscono nelle
 * stampe dei quadri W/RW e T/RT. Movimenti sintetici in {@code Principale.MappaCryptoWallet}.
 */
class DerivatiTest {

    @BeforeEach
    void svuota() {
        Principale.MappaCryptoWallet.clear();
    }

    @AfterEach
    void pulisci() {
        Principale.MappaCryptoWallet.clear();
    }

    private static String[] movimento(String id, String data, String tipoDerivato) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = data;
        Importazioni.RiempiVuotiArray(v);
        Derivati.Marca(v, tipoDerivato);
        Principale.MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void marcatore_vuotoNonCancellaUnTipoGiaScritto() {
        String[] v = movimento("20240101000000_Bybit_1_1_DC", "2024-01-01 00:00", Derivati.PNL);
        assertTrue(Derivati.isDerivato(v));
        Derivati.Marca(v, "");
        Derivati.Marca(v, null);
        assertEquals(Derivati.PNL, Derivati.Tipo(v));
        assertTrue(Derivati.CAMPO < Importazioni.ColonneTabella, "il campo deve stare dentro la riga");
    }

    @Test
    void contaNellAnno_soloIMarcatiDiQuellAnno() {
        movimento("20231231235900_Bybit_1_1_DC", "2023-12-31 23:59", Derivati.FUNDING);
        movimento("20240101000000_Bybit_1_1_DC", "2024-01-01 00:00", Derivati.PNL);
        movimento("20240601000000_Bybit_1_1_PC", "2024-06-01 00:00", Derivati.COMMISSIONE);
        movimento("20240701000000_Bybit_1_1_DC", "2024-07-01 00:00", "");

        assertEquals(1, Derivati.ContaNellAnno("2023"));
        assertEquals(2, Derivati.ContaNellAnno("2024"));
        assertEquals(0, Derivati.ContaNellAnno("2025"));
    }

    @Test
    void derivatiDegliAnniPrecedenti_avvisanoAncheDopo() {
        //Un PnL del 2022 entrato come deposito a costo zero diventa plusvalenza cripto quando quegli USDT
        //vengono venduti, e resta nelle giacenze: l'anno dopo deve avvisare anche senza derivati propri
        movimento("20220601000000_Bybit_1_1_DC", "2022-06-01 00:00", Derivati.PNL);

        assertFalse(Derivati.InfluisconoSullAnno("2021"), "gli anni prima del primo derivato restano puliti");
        assertTrue(Derivati.InfluisconoSullAnno("2022"));
        assertTrue(Derivati.InfluisconoSullAnno("2023"));
        String nota = Derivati.TestoAvvisoQuadro("2023");
        assertTrue(nota.contains("<b>0</b> nell'anno 2023") && nota.contains("<b>1</b> negli anni precedenti"), nota);
        assertTrue(Derivati.MessaggioAvviso("2023").contains("0 nell'anno 2023, 1 negli anni precedenti"));
        assertEquals("", Derivati.MessaggioAvviso("2021"));
    }

    @Test
    void senzaDerivati_nessunAvviso() {
        movimento("20240701000000_Bybit_1_1_DC", "2024-07-01 00:00", "");
        assertEquals("", Derivati.MessaggioAvviso("2024"));
        assertEquals("", Derivati.TestoAvvisoQuadro("2024"));
    }

    @Test
    void conDerivati_avvisoConAnnoENumeroRisolti() {
        movimento("20240101000000_Bybit_1_1_DC", "2024-01-01 00:00", Derivati.PNL);
        movimento("20240601000000_Bybit_1_1_PC", "2024-06-01 00:00", Derivati.FUNDING);

        String messaggio = Derivati.MessaggioAvviso("2024");
        assertTrue(messaggio.contains("2 nell'anno 2024, 0 negli anni precedenti"), messaggio);

        String nota = Derivati.TestoAvvisoQuadro("2024");
        assertTrue(nota.contains("<b>2</b> nell'anno 2024"), "il numero dei movimenti deve comparire nella nota: " + nota);
        assertFalse(nota.matches("(?s).*\\{\\w+\\}.*"), "segnaposto non risolti: " + nota);
    }
}
