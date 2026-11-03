package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * «Annullo spesa carta» : il rimborso di un pagamento con carta entra come un acquisto ({@code DAC}) a costo di carico
 * pari al valore del pagamento, e il pagamento resta il cashout che era. Fissa tre cose :
 * <ul>
 *   <li>il motore : la plusvalenza del pagamento non cambia, il rimborso non ne genera e carica il lotto al valore;</li>
 *   <li>il legame {@code [20]} fra i due è coerente per il controllo dei movimenti collegati e non cambia il calcolo;</li>
 *   <li>annullare la classificazione del rimborso (o cancellarlo) non toglie al pagamento la sua classificazione.</li>
 * </ul>
 */
class AnnulloSpesaCartaTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    private static final String PCO = "PCO - Cashout, acquisti con crypto etc.. (plusvalenza)";

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
        MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
    }

    private static String[] movimento(String data, String tipoID, String classificazione, String descrizione,
            String monetaU, String qtaU, String monetaE, String qtaE, String valore) {
        String id = data.replace(":", ".") + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "TestWallet";
        m[5] = descrizione;
        m[8] = monetaU;
        m[9] = monetaU.isEmpty() ? "" : "Crypto";
        m[10] = qtaU;
        m[11] = monetaE;
        m[12] = monetaE.isEmpty() ? "" : "Crypto";
        m[13] = qtaE;
        m[15] = valore;
        m[18] = classificazione;
        m[22] = "M";
        MappaCryptoWallet.put(id, m);
        return m;
    }

    private static String[] acquisto(String data, String moneta, String qta, String valore) {
        String[] m = movimento(data, "AC", "", "ACQUISTO", "EUR", valore, moneta, qta, valore);
        m[9] = "FIAT";
        return m;
    }

    private static String[] pagamento(String data, String moneta, String qta, String valore) {
        return movimento(data, "PC", PCO, "CASHOUT o SIMILARI", moneta, qta, "", "", valore);
    }

    /** Il deposito com'è dopo la classificazione : lo stesso che scrive {@code ClassificaMovimenti} con il nuovo valore. */
    private static String[] rimborsoClassificato(String data, String moneta, String qta, String valoreMercato, String costo) {
        String[] r = movimento(data, "DC", "", "DEPOSITO CRYPTO", "", "", moneta, qta, valoreMercato);
        r[5] = GUI_ClassificazioneMovimento.CAMPO5_ANNULLO_SPESA_CARTA;
        r[18] = GUI_ClassificazioneMovimento.CAMPO18_ANNULLO_SPESA_CARTA;
        r[35] = valoreMercato;
        r[15] = costo;
        return r;
    }

    private static void assertValore(String atteso, String effettivo) {
        assertEquals(0, new BigDecimal(atteso).compareTo(new BigDecimal(effettivo)), "atteso " + atteso + " trovato " + effettivo);
    }

    // ------------------------------------------------------------------ motore

    @Test
    void ilPagamentoNonCambiaEIlRimborsoCaricaAlValoreSenzaPlusvalenza() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        //Rimborso in un altro token, valore di mercato 580 ma costo di carico = valore del pagamento (600)
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertValore("500", pag[16]);
        assertValore("100", pag[19]);         // 600 - 500, come senza rimborso
        assertEquals("S", pag[33]);
        assertValore("600", rimb[17]);        // il lotto nasce al valore del pagamento
        assertValore("0", rimb[19]);
        assertEquals("N", rimb[33]);

        //Il lotto rimborsato, venduto per 700, realizza la differenza da 600
        String[] ven = movimento("2024-08-01 10:00", "VC", "", "VENDITA", "USDC", "700", "EUR", "700", "700");
        ven[12] = "FIAT";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertValore("600", ven[16]);
        assertValore("100", ven[19]);
    }

    @Test
    void ilLegameFraRimborsoEPagamentoNonCambiaIlCalcolo() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        String senzaLegame = pag[16] + "|" + pag[19] + "|" + pag[33] + "|" + rimb[17] + "|" + rimb[19] + "|" + rimb[33];

        GUI_ClassificazioneMovimento.AbbinaAnnulloSpesaCarta(rimb, pag);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals(senzaLegame, pag[16] + "|" + pag[19] + "|" + pag[33] + "|" + rimb[17] + "|" + rimb[19] + "|" + rimb[33]);
        assertEquals("", pag[38], "nessuna anomalia LiFo sul pagamento");
        assertEquals("", rimb[38], "nessuna anomalia LiFo sul rimborso");
    }

    @Test
    void ilLegameESimmetricoPerIlControlloDeiMovimentiCollegati() {
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");

        GUI_ClassificazioneMovimento.AbbinaAnnulloSpesaCarta(rimb, pag);

        assertEquals(pag[0], rimb[20]);
        assertEquals(rimb[0], pag[20]);
        assertEquals(0, MovimentiCollegati.Controlla(MappaCryptoWallet.values()).Totale());
    }

    // ------------------------------------------------------------------ ripristino

    @Test
    void annullareLaClassificazioneDelRimborsoLasciaIlPagamentoComeEra() {
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");
        GUI_ClassificazioneMovimento.AbbinaAnnulloSpesaCarta(rimb, pag);

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((rimb[0] + "," + rimb[20]).split(","), rimb[0]);

        assertEquals("CASHOUT o SIMILARI", pag[5]);
        assertEquals(PCO, pag[18]);
        assertEquals("", pag[20]);
        assertEquals("DEPOSITO CRYPTO", rimb[5]);
        assertEquals("", rimb[18]);
        assertEquals("", rimb[20]);
        assertEquals("580", rimb[15], "torna il valore di mercato");
        assertEquals("", rimb[35]);
    }

    @Test
    void cancellareIlRimborsoLasciaIlPagamentoClassificato() {
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");
        GUI_ClassificazioneMovimento.AbbinaAnnulloSpesaCarta(rimb, pag);

        Funzioni.RimuoviMovimentazioneXID(rimb[0]);

        assertNull(MappaCryptoWallet.get(rimb[0]));
        assertEquals(PCO, pag[18]);
        assertEquals("", pag[20]);
    }

    @Test
    void ripristinareIlPagamentoStessoLoRiportaComeOgniAltroMovimentoEScioglieIlRimborso() {
        String[] pag = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        String[] rimb = rimborsoClassificato("2024-06-10 10:00", "USDC", "700", "580", "600.00");
        GUI_ClassificazioneMovimento.AbbinaAnnulloSpesaCarta(rimb, pag);

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((pag[0] + "," + pag[20]).split(","), pag[0]);

        assertEquals("PRELIEVO CRYPTO", pag[5]);
        assertEquals("", pag[18]);
        assertEquals("", pag[20]);
        assertEquals("", rimb[18]);
        assertEquals("", rimb[20]);
        assertEquals("580", rimb[15]);
    }

    @Test
    void unTrasferimentoNormaleSiRipristinaComePrima() {
        //Il ramo nuovo non deve toccare i gruppi che non hanno un annullo di spesa con carta
        String[] p = movimento("2024-06-01 10:00", "PC", "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)",
                "TRASFERIMENTO TRA WALLET", "BTC", "1", "", "", "100");
        String[] d = movimento("2024-06-01 10:01", "DC", "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)",
                "TRASFERIMENTO TRA WALLET", "", "", "BTC", "1", "100");
        p[20] = d[0];
        d[20] = p[0];

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((p[0] + "," + p[20]).split(","), p[0]);

        assertEquals("PRELIEVO CRYPTO", p[5]);
        assertEquals("", p[18]);
        assertEquals("DEPOSITO CRYPTO", d[5]);
        assertEquals("", d[18]);
    }

    // ------------------------------------------------------------------ tabella dei pagamenti e costo proposto

    private static final long GIORNO = 86400000L;

    @Test
    void sonoCandidatiSoloIPagamentiCashoutNonAbbinatiEPrimaDelRimborso() {
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "700", "580");
        String[] ok = pagamento("2024-06-01 10:00", "BTC", "0.5", "600");
        assertTrue(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, ok, 30 * GIORNO));

        String[] nonClassificato = movimento("2024-06-02 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "600");
        assertFalse(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, nonClassificato, 30 * GIORNO));

        String[] generato = pagamento("2024-06-03 10:00", "BTC", "0.5", "600");
        generato[22] = "AU";
        assertFalse(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, generato, 30 * GIORNO));

        String[] altroRimborsoAbbinato = pagamento("2024-06-04 10:00", "BTC", "0.5", "600");
        altroRimborsoAbbinato[20] = "2024.06.08 10.00_TEST_999_001_DC";
        assertFalse(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, altroRimborsoAbbinato, 30 * GIORNO));

        String[] giaAbbinatoAQuestoRimborso = pagamento("2024-06-05 10:00", "BTC", "0.5", "600");
        giaAbbinatoAQuestoRimborso[20] = rimb[0];
        assertTrue(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, giaAbbinatoAQuestoRimborso, 30 * GIORNO),
                "riclassificando lo stesso rimborso il suo pagamento resta scegliibile");

        String[] dopo = pagamento("2024-06-11 10:00", "BTC", "0.5", "600");
        assertFalse(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, dopo, 30 * GIORNO));

        String[] troppoVecchio = pagamento("2024-01-01 10:00", "BTC", "0.5", "600");
        assertFalse(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, troppoVecchio, 30 * GIORNO));
        assertTrue(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, troppoVecchio, 365 * GIORNO));
    }

    @Test
    void costoProposto_valoreDelPagamentoSeIlTokenECambiato() {
        String[] pag = pagamento("2024-06-01 10:00", "weEUR", "0.8685", "0.87");
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "1", "0.86");
        assertEquals("0.87", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, pag));
    }

    @Test
    void costoProposto_stessoTokenConQuantitaMinoreESproporzionale() {
        String[] pag = pagamento("2024-06-01 10:00", "USDC", "10", "9.00");
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "4", "3.70");
        assertEquals("3.60", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, pag));

        String[] rimbPieno = movimento("2024-06-11 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "10", "9.40");
        assertEquals("9.00", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimbPieno, pag));
    }

    @Test
    void costoProposto_senzaPagamentoELValoreDiMercatoOriginale() {
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "1", "0.86");
        assertEquals("0.86", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, null));

        //Già classificato : in [15] c'è il costo scelto, il valore di mercato è in [35]
        rimb[35] = "0.86";
        rimb[15] = "0.90";
        assertEquals("0.86", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, null));
    }

    // ------------------------------------------------------------------ spesa con carta (prelievo)

    @Test
    void laSpesaConCartaRealizzaAlValoreScelto() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] pag = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "600");
        //Come lo lascia ClassificaMovimenti con il valore scelto dall'utente (580 al posto dei 600 calcolati)
        pag[5] = MovimentiCrypto.CAMPO5_SPESA_CON_CARTA;
        pag[18] = MovimentiCrypto.CAMPO18_SPESA_CON_CARTA;
        pag[35] = "600";
        pag[15] = "580.00";

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertValore("500", pag[16]);
        assertValore("80", pag[19]);   // 580 - 500: la plusvalenza segue il valore scelto
        assertEquals("S", pag[33]);
    }

    @Test
    void ilValoreProposto_ePrimaIlCalcolatoPoiQuelloOriginale() {
        String[] v = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "600");
        assertEquals("600.00", GUI_ClassificazioneMovimento.ValoreOriginale(v));
        //Gia' classificata con un valore scelto: si riparte da quello calcolato, messo da parte in [35]
        v[35] = "600";
        v[15] = "580.00";
        assertEquals("600.00", GUI_ClassificazioneMovimento.ValoreOriginale(v));
    }

    @Test
    void annullareLaSpesaConCartaRiportaIlValoreCalcolato() {
        String[] pag = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "600");
        pag[5] = MovimentiCrypto.CAMPO5_SPESA_CON_CARTA;
        pag[18] = MovimentiCrypto.CAMPO18_SPESA_CON_CARTA;
        pag[35] = "600";
        pag[15] = "580.00";

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(new String[]{pag[0]}, pag[0]);

        assertEquals("PRELIEVO CRYPTO", pag[5]);
        assertEquals("", pag[18]);
        assertEquals("600", pag[15]);
        assertEquals("", pag[35]);
    }

    @Test
    void laVoceDellaCombo_eInFondoEPreselezionaIlMarcatore() {
        assertEquals(GUI_ClassificazioneMovimento.NTIPO_SPESA_CON_CARTA,
                java.util.Arrays.asList(GUI_ClassificazioneMovimento.CB_PC_SINGOLO).indexOf(GUI_ClassificazioneMovimento.CB_PC_SPESACARTA));
        assertEquals(GUI_ClassificazioneMovimento.NTIPO_ANNULLO_SPESA_CARTA,
                java.util.Arrays.asList(GUI_ClassificazioneMovimento.CB_DC_SINGOLO).indexOf(GUI_ClassificazioneMovimento.CB_DC_ANNULLOSPESA));
    }

    @Test
    void unaSpesaConCartaImportataEUnPagamentoCandidatoPerIlRimborso() {
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "700", "580");
        String[] pag = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "600");
        MovimentiCrypto.MarcaSpesaConCarta(pag);
        assertEquals("SPESA CON CARTA", pag[5]);
        assertEquals("PCO - SPESA CON CARTA", pag[18]);
        assertTrue(GUI_ClassificazioneMovimento.PagamentoAbbinabileAnnullo(rimb, pag, 30 * GIORNO));
    }

    @Test
    void ilValoreScrittoDaMano_virgolaOPuntoMaMaiNegativo() {
        assertEquals("0.85", GUI_ClassificazioneMovimento.NormalizzaValoreEuro("0,85"));
        assertEquals("0.85", GUI_ClassificazioneMovimento.NormalizzaValoreEuro(" 0.854 "));
        assertEquals("0.00", GUI_ClassificazioneMovimento.NormalizzaValoreEuro("0"));
        assertNull(GUI_ClassificazioneMovimento.NormalizzaValoreEuro("-0,85"));
        assertNull(GUI_ClassificazioneMovimento.NormalizzaValoreEuro("abc"));
        assertNull(GUI_ClassificazioneMovimento.NormalizzaValoreEuro(""));
        assertNull(GUI_ClassificazioneMovimento.NormalizzaValoreEuro(null));
    }

    @Test
    void unMovimentoSenzaValore_proponeZeroInveceDiFallire() {
        String[] v = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO CRYPTO", "BTC", "0.5", "", "", "");
        assertEquals("0.00", GUI_ClassificazioneMovimento.ValoreOriginale(v));
        v[15] = "non un numero";
        assertEquals("0.00", GUI_ClassificazioneMovimento.ValoreOriginale(v));
    }

    @Test
    void costoProposto_conUnPagamentoSenzaValoreNonFallisce() {
        String[] rimb = movimento("2024-06-10 10:00", "DC", "", "DEPOSITO CRYPTO", "", "", "USDC", "1", "0.86");
        String[] pagSenzaValore = pagamento("2024-06-01 10:00", "weEUR", "0.8685", "");
        assertEquals("0.00", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, pagSenzaValore));

        //Stesso token, quantita' pagata vuota: nessuna proporzione, resta il valore del pagamento
        String[] pagSenzaQta = pagamento("2024-06-01 10:00", "USDC", "", "9.00");
        assertEquals("9.00", GUI_ClassificazioneMovimento.ValoreProposto_AnnulloSpesaCarta(rimb, pagSenzaQta));
    }
}
