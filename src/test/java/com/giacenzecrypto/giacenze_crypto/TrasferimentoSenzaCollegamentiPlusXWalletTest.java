package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Un trasferimento classificato ({@code PTW}/{@code DTW}) a cui manca il collegamento {@code [20]}, con le plusvalenze divise per
 * gruppo wallet: il motore non va in errore, lo tratta come un trasferimento interno e non sposta il costo di carico fra le pile
 * dei due gruppi ({@link Calcoli_PlusvalenzeNew#RitornaIDeGruppoControparteSeGruppoDiverso} restituisce {@code null} se
 * {@code [20]} è vuoto). Se i due wallet stanno in gruppi diversi il costo di carico resta nella pila di partenza, e la
 * vendita fatta dal gruppo di arrivo non trova il lotto. Dati inventati.
 */
class TrasferimentoSenzaCollegamentiPlusXWalletTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "NO");
    }

    private static String[] movimento(String data, String wallet, String tipoID, String classificazione, String descrizione,
            String monetaU, String tipoU, String qtaU, String monetaE, String tipoE, String qtaE, String valore) {
        String id = data.replace(":", ".") + "_" + wallet + "_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = wallet;
        m[5] = descrizione;
        m[8] = monetaU;
        m[9] = tipoU;
        m[10] = qtaU;
        m[11] = monetaE;
        m[12] = tipoE;
        m[13] = qtaE;
        m[15] = valore;
        m[18] = classificazione;
        m[22] = "M";
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    /** Acquisto di 1 BTC a 1000 su WalletA, trasferimento a WalletB, vendita su WalletB a 3000. */
    private String[][] scenario(String gruppoB, boolean collegati) {
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", gruppoB);
        String[] acq = movimento("2024-01-01 10:00", "WalletA", "AC", "", "ACQUISTO", "EUR", "FIAT", "1000", "BTC", "Crypto", "1", "1000");
        String[] pre = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Trasferimento tra Wallet", "TRASFERIMENTO TRA WALLET",
                "BTC", "Crypto", "-1", "", "", "", "1500");
        String[] dep = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Trasferimento tra Wallet", "TRASFERIMENTO TRA WALLET",
                "", "", "", "BTC", "Crypto", "1", "1500");
        if (collegati) {
            pre[20] = dep[0];
            dep[20] = pre[0];
        }
        String[] ven = movimento("2024-03-01 10:00", "WalletB", "VC", "", "VENDITA", "BTC", "Crypto", "-1", "EUR", "FIAT", "3000", "3000");
        return new String[][]{acq, pre, dep, ven};
    }

    @Test
    void gruppiDiversi_collegati_ilCostoDiCaricoSiSposta_ePlusvalenzaCorretta() {
        String[][] m = scenario("Wallet 02", true);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", m[2][17], "il deposito riceve il costo di carico dalla pila del gruppo di partenza");
        assertEquals("2000.00", m[3][19], "3000 - 1000");
        assertFalse(m[3][38].contains("A"), "nessun LiFo mancante");
    }

    @Test
    void gruppiDiversi_senzaCollegamento20_nonVaInErrore_maNonSpostaIlCosto() {
        String[][] m = scenario("Wallet 02", false);

        assertDoesNotThrow(Calcoli_PlusvalenzeNew::AggiornaPlusvalenze, "il motore non va in errore");

        assertEquals("", m[2][17], "il deposito non riceve nessun costo di carico");
        assertTrue(m[3][38].contains("A"), "la vendita del gruppo di arrivo non trova il lotto: parte del LiFo mancante");
        assertEquals("3000.00", m[3][19], "senza lotto il costo è zero e tutta la vendita diventa plusvalenza");
    }

    @Test
    void gruppiDiversi_senzaCollegamento20_ilLottoRestaNellaPilaDiPartenza() {
        //Una seconda vendita sul wallet di PARTENZA, dopo il trasferimento (una vendita che l'utente non farebbe, ma dice dove
        //sta il lotto): col collegamento il lotto è già passato all'altro gruppo e manca, senza collegamento è ancora lì
        String[][] collegati = scenario("Wallet 02", true);
        String[] venditaDiPartenza = movimento("2024-04-01 10:00", "WalletA", "VC", "", "VENDITA", "BTC", "Crypto", "-1", "EUR", "FIAT", "2500", "2500");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(venditaDiPartenza[38].contains("A"), "col collegamento il gruppo di partenza non ha più il lotto");
        assertEquals("2500.00", venditaDiPartenza[19]);

        Principale.MappaCryptoWallet.clear();
        scenario("Wallet 02", false);
        String[] venditaDiPartenza2 = movimento("2024-04-01 10:00", "WalletA", "VC", "", "VENDITA", "BTC", "Crypto", "-1", "EUR", "FIAT", "2500", "2500");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(venditaDiPartenza2[38].contains("A"), "senza collegamento il lotto è ancora nella pila di partenza");
        assertEquals("1500.00", venditaDiPartenza2[19], "2500 - 1000: il lotto è stato usato due volte");
    }

    @Test
    void stessoGruppo_senzaCollegamento20_ilRisultatoNonCambia() {
        String[][] collegati = scenario("Wallet 01", true);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        String plusCollegati = collegati[3][19];
        String lifoCollegati = collegati[3][38];

        Principale.MappaCryptoWallet.clear();
        String[][] scollegati = scenario("Wallet 01", false);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("2000.00", plusCollegati);
        assertEquals(plusCollegati, scollegati[3][19], "nello stesso gruppo il collegamento non serve");
        assertEquals(lifoCollegati, scollegati[3][38]);
    }

    @Test
    void ilControlloDeiMovimentiCollegati_nonVedeUnTrasferimentoSenza20_seNessunoLoCita() {
        String[][] m = scenario("Wallet 02", false);

        MovimentiCollegati.Esito e = MovimentiCollegati.Controlla(Principale.MappaCryptoWallet.values());

        assertEquals(0, e.Totale(), "il pulsante Errori non segnala un PTW/DTW senza [20] che nessun altro cita");
        assertEquals("", m[1][20]);
    }
}
