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
 * Il motore delle plusvalenze separa il risultato dei derivati (c-quater) dalle plusvalenze cripto: un deposito
 * classificato {@code DDV} e un prelievo o una commissione classificati {@code PDV} scrivono il risultato in
 * {@code v[46]} ({@link Derivati#CAMPO_RISULTATO}) e <b>mai</b> in {@code v[19]}/{@code v[33]}, che restano solo
 * plusvalenze da cripto-attivita'. Decisioni in {@code nocommit/Documentazione/Decisioni_Derivati.md} (D10, D15).
 *
 * <p>Dati inventati. Valori in euro, USDC acquistati a 0,92 euro e valutati 0,94 euro quando entrano o escono dai derivati.</p>
 */
class CalcoliPlusvalenzeNewDerivatiTest {

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
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
    }

    private static String[] movimento(String data, String tipoID, String classificazione, String descrizione,
            String monetaU, String tipoU, String qtaU,
            String monetaE, String tipoE, String qtaE,
            String valore) {
        String id = data.replace(":", ".") + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "TestWallet";
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

    private static String[] acquistoUsdc(String data, String qta, String valore) {
        return movimento(data, "AC", "", "ACQUISTO", "EUR", "FIAT", valore, "USDC", "Crypto", qta, valore);
    }

    /** Deposito di USDC da derivati (PnL positivo, funding incassato...). */
    private static String[] accredito(String data, String qta, String valore, String classificazione) {
        String[] m = movimento(data, "DC", classificazione, "ACCREDITO DERIVATI", "", "", "", "USDC", "Crypto", qta, valore);
        Derivati.Marca(m, Derivati.PNL);
        return m;
    }

    /** Prelievo di USDC per derivati (PnL negativo, funding pagato...). */
    private static String[] addebito(String data, String qta, String valore, String classificazione) {
        String[] m = movimento(data, "PC", classificazione, "ADDEBITO DERIVATI", "USDC", "Crypto", qta, "", "", "", valore);
        Derivati.Marca(m, Derivati.PNL);
        return m;
    }

    @Test
    void unAccreditoDaDerivatiCaricaAlValoreEPortaIlProventoInV46() {
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] acc = accredito("2026-01-02 10:00", "10", "9.40", Derivati.CAMPO18_ACCREDITO);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("9.40", acc[17], "la moneta entra nella pila al valore del momento");
        assertEquals("0.00", acc[19], "nessuna plusvalenza cripto");
        assertEquals("N", acc[33]);
        assertEquals("9.40", acc[Derivati.CAMPO_RISULTATO], "provento dei derivati");
    }

    @Test
    void unAddebitoPerDerivatiESceDalLifoConPlusvalenzaCriptoEOnereInV46() {
        //Doppio effetto (D10): 10 USDC a costo 9,20 e valore 9,40 -> plusvalenza cripto 0,20, onere dei derivati 9,40
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] add = addebito("2026-01-02 10:00", "10", "9.40", Derivati.CAMPO18_ADDEBITO);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("9.20", add[16], "costo di carico preso dalla pila");
        assertEquals("0.20", add[19], "plusvalenza cripto = valore - costo");
        assertEquals("S", add[33]);
        assertEquals("-9.40", add[Derivati.CAMPO_RISULTATO], "onere dei derivati, segno meno");
    }

    @Test
    void laCommissioneDiUnDerivatoEUnOnereDeiDerivati() {
        //2 USDC a costo 1,84 e valore 1,90: plusvalenza cripto 0,06 come ogni commissione, onere dei derivati 1,90
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] cm = movimento("2026-01-02 10:00", "CM", Derivati.CAMPO18_ADDEBITO, "COMMISSIONI",
                "USDC", "Crypto", "2", "", "", "", "1.90");
        Derivati.Marca(cm, Derivati.COMMISSIONE);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.06", cm[19]);
        assertEquals("S", cm[33]);
        assertEquals("-1.90", cm[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void conLOpzioneNoPlusvalenzeCommissioniLaCommissioneDerivataRestaUnOnereDeiDerivati() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "SI");
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] cm = movimento("2026-01-02 10:00", "CM", Derivati.CAMPO18_ADDEBITO, "COMMISSIONI",
                "USDC", "Crypto", "2", "", "", "", "1.90");
        Derivati.Marca(cm, Derivati.COMMISSIONE);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.00", cm[19], "come le altre commissioni con l'opzione attiva");
        assertEquals("N", cm[33]);
        assertEquals("-1.90", cm[Derivati.CAMPO_RISULTATO], "l'onere dei derivati non dipende dall'opzione sulle cripto");
    }

    @Test
    void unMovimentoMarcatoMaNonClassificatoRestaCom_eraEIlRisultatoDeiDerivatiEVuoto() {
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] add = addebito("2026-01-02 10:00", "10", "9.40", "");   //[44] PNL ma campo 18 vuoto: da classificare

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.20", add[19], "cashout non classificato, come prima dei derivati");
        assertEquals("", add[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void unMovimentoNonDerivatoNonHaIlRisultatoDeiDerivati() {
        String[] acq = acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] ven = movimento("2026-01-05 10:00", "VC", "", "VENDITA", "USDC", "Crypto", "10", "EUR", "FIAT", "9.50", "9.50");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", acq[Derivati.CAMPO_RISULTATO]);
        assertEquals("", ven[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.30", ven[19]);
    }

    @Test
    void ilRisultatoDeiDerivatiNonEntraNellePlusvalenzeDellEccedenza() {
        //Sequenza completa: acquisto 10 USDC a 9,20, accredito di 5 USDC a 4,70, addebito di 5 USDC a 4,75.
        //Pila: 10 @ 9,20 e 5 @ 4,70 (la piu' recente esce per prima). L'addebito prende i 5 a 4,70: plusvalenza 0,05.
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] acc = accredito("2026-01-02 10:00", "5", "4.70", Derivati.CAMPO18_ACCREDITO);
        String[] add = addebito("2026-01-03 10:00", "5", "4.75", Derivati.CAMPO18_ADDEBITO);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("4.70", add[16]);
        assertEquals("0.05", add[19]);
        assertEquals("4.70", acc[Derivati.CAMPO_RISULTATO]);
        assertEquals("-4.75", add[Derivati.CAMPO_RISULTATO]);
        //Plusvalenze cripto totali: solo i 0,05 dell'addebito. Il risultato dei derivati sta altrove: +4,70 - 4,75 = -0,05
        assertEquals(0, new java.math.BigDecimal(acc[19]).add(new java.math.BigDecimal(add[19]))
                .compareTo(new java.math.BigDecimal("0.05")));
    }

    @Test
    void cambiareLaClassificazioneRifaIlCalcoloDaQuelPunto() {
        //Il ricalcolo e' incrementale e il campo 18 e' nell'impronta: da vuoto a PDV il risultato compare senza ricalcolo completo
        acquistoUsdc("2026-01-01 10:00", "10", "9.20");
        String[] add = addebito("2026-01-02 10:00", "10", "9.40", "");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", add[Derivati.CAMPO_RISULTATO]);

        add[18] = Derivati.CAMPO18_ADDEBITO;
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("-9.40", add[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.20", add[19]);
    }

    @Test
    void ilDialogoDelLifoMancanteRiconoscePerDepositiIDDV() {
        assertTrue(Principale_LifoMancante.CausaleDepositoRiconosciuta("DC", Derivati.CAMPO18_ACCREDITO));
    }

    @Test
    void ilCampoDelRisultatoNonSiCopiaVerbatim() {
        assertTrue(MovimentiCrypto.CampiNonCopiabiliVerbatim.contains(Derivati.CAMPO_RISULTATO));
        assertEquals(Derivati.CAMPO_RISULTATO + 1, Derivati.CAMPO_DATI, "il campo dei dati del contratto segue il risultato");
        assertEquals(Derivati.CAMPO_DATI + 1, Importazioni.ColonneTabella);
        assertFalse(MovimentiCrypto.CampiNonCopiabiliVerbatim.contains(Derivati.CAMPO_DATI), "dato di provenienza: si copia verbatim");
    }
}
