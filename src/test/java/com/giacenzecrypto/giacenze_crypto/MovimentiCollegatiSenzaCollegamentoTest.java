package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MovimentiCollegati.Esito#SenzaCollegamento}: un trasferimento classificato ({@code PTW}/{@code DTW}) che non cita nessuno e che
 * nessuno cita. Conta a parte, non entra in {@code Totale()} (il contatore dei movimenti collegati incoerenti). Dati inventati.
 */
class MovimentiCollegatiSenzaCollegamentoTest {

    private static String[] riga(String id, String classificazione, String collegati, String origine) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[3] = "Wallet";
        v[18] = classificazione;
        v[20] = collegati;
        v[22] = origine;
        return v;
    }

    private static List<String[]> lista(String[]... righe) {
        return new ArrayList<>(Arrays.asList(righe));
    }

    @Test
    void unPTWSenzaCollegamentoENonCitato_siSegnalaAParte() {
        List<String[]> m = lista(riga("20250101100000_A_001_001_PC", "PTW - Trasferimento tra Wallet", "", "A"));

        MovimentiCollegati.Esito e = MovimentiCollegati.Controlla(m);

        assertEquals(List.of("20250101100000_A_001_001_PC"), new ArrayList<>(e.SenzaCollegamento));
        assertEquals(0, e.Totale(), "non entra nel contatore dei collegati incoerenti");
    }

    @Test
    void unTrasferimentoCollegatoNonSiSegnala() {
        String[] p = riga("20250101100000_A_001_001_PC", "PTW - Trasferimento tra Wallet", "20250101100000_B_001_001_DC", "A");
        String[] d = riga("20250101100000_B_001_001_DC", "DTW - Trasferimento tra Wallet", "20250101100000_A_001_001_PC", "A");

        MovimentiCollegati.Esito e = MovimentiCollegati.Controlla(lista(p, d));

        assertTrue(e.SenzaCollegamento.isEmpty());
        assertEquals(0, e.Totale());
    }

    @Test
    void unMovimentoSenzaRiferimentiMaCitatoDaUnAltro_nonSiSegnalaASeStesso() {
        //Le gambe del Dual a moneta uguale nella forma precedente: citate a senso unico, le risolve il loro controllo
        String[] settlement = riga("20250101100000_A_001_001_DC", "DTW - Settlement", "20250101100000_A_001_002_PC", "A");
        String[] gamba = riga("20250101100000_A_001_002_PC", "PTW - Trasferimento Interno", "", "AU");

        MovimentiCollegati.Esito e = MovimentiCollegati.Controlla(lista(settlement, gamba));

        assertTrue(e.SenzaCollegamento.isEmpty());
    }

    @Test
    void unMovimentoNonClassificatoNonSiSegnala() {
        List<String[]> m = lista(riga("20250101100000_A_001_001_DC", "", "", "A"));

        assertTrue(MovimentiCollegati.Controlla(m).SenzaCollegamento.isEmpty());
    }

    @Test
    void sonoInOrdineDiID_eVaBeneAncheUnDepositoEUnaRigaGenerata() {
        String[] c = riga("20250103100000_C_001_001_DC", "DTW - Trasferimento tra Wallet", "", "A");
        String[] a = riga("20250101100000_A_001_001_PC", "PTW - Trasferimento tra Wallet", "", "M");
        String[] b = riga("20250102100000_B_001_001_PC", "PTW - Trasferimento Interno", "", "AU");

        MovimentiCollegati.Esito e = MovimentiCollegati.Controlla(lista(c, a, b));

        assertEquals(List.of(a[0], b[0], c[0]), new ArrayList<>(e.SenzaCollegamento));
    }

    @Test
    void ilCasoDiUnaClassificazioneDiAltroTipoNonSiSegnala() {
        //Solo PTW e DTW: una reward o un provento non sono trasferimenti
        List<String[]> m = lista(riga("20250101100000_A_001_001_DC", "DAI - Provento da Detenzione", "", "A"));

        assertTrue(MovimentiCollegati.Controlla(m).SenzaCollegamento.isEmpty());
    }
}
