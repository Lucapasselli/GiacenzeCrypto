package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Costo medio netto di "Giacenze a data" ({@link Principale_GiacenzeaData_CostoNetto}): (speso - incassato) / quantità.
 * Funzione pura sui movimenti, nessun database e nessuna rete. Gli esempi numerici sono quelli spiegati all'utente:
 * una scheda in stile My Portfolio di CoinGecko (dati inventati) e il token XYZ in cinque passi.
 */
class Principale_GiacenzeaData_CostoNettoTest {

    private static final long DATA_LONTANA = FunzioniDate.ConvertiDatainLong("2030-01-01 00:00");
    private static final double TOLLERANZA = 0.0000001;

    private int progressivo;
    private final List<String[]> movimenti = new ArrayList<>();

    @BeforeEach
    void azzera() {
        progressivo = 0;
        movimenti.clear();
    }

    // ------------------------------------------------------------------
    // Costruzione dei movimenti
    // ------------------------------------------------------------------

    private String[] riga(String data, String categoria, String c18, String descrizione,
            String monetaU, String tipoU, String qtaU,
            String monetaE, String tipoE, String qtaE, String valore) {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = data.replace(":", ".") + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + categoria;
        m[1] = data;
        m[3] = "Wallet";
        m[4] = "Wallet";
        m[5] = descrizione;
        m[8] = monetaU;
        m[9] = tipoU;
        m[10] = qtaU;
        m[11] = monetaE;
        m[12] = tipoE;
        m[13] = qtaE;
        m[15] = valore;
        m[18] = c18;
        m[32] = "SI";
        movimenti.add(m);
        return m;
    }

    /** Acquisto con euro: esce FIAT, entra il token. */
    private String[] acquisto(String data, String token, String qta, String euro) {
        return riga(data, "AC", "", "ACQUISTO CRYPTO", "EUR", "FIAT", "-" + euro, token, "Crypto", qta, euro);
    }

    /** Vendita per euro: esce il token, entra FIAT. */
    private String[] vendita(String data, String token, String qta, String euro) {
        return riga(data, "VC", "", "VENDITA CRYPTO", token, "Crypto", "-" + qta, "EUR", "FIAT", euro, euro);
    }

    private String[] scambio(String data, String daToken, String daQta, String aToken, String aQta, String euro) {
        return riga(data, "SC", "", "SCAMBIO CRYPTO", daToken, "Crypto", "-" + daQta, aToken, "Crypto", aQta, euro);
    }

    private String[] depositoNC(String data, String token, String qta) {
        return riga(data, "DC", "", "DEPOSITO CRYPTO", "", "", "", token, "Crypto", qta, "0.00");
    }

    private String[] prelievoNC(String data, String token, String qta, String euro) {
        return riga(data, "PC", "", "PRELIEVO CRYPTO", token, "Crypto", "-" + qta, "", "", "", euro);
    }

    private Principale_GiacenzeaData_CostoNetto.Risultato calcola() {
        return Principale_GiacenzeaData_CostoNetto.Calcola(movimenti, DATA_LONTANA, null);
    }

    private Principale_GiacenzeaData_CostoNetto.Riga riga(String token, String prezzo) {
        return Principale_GiacenzeaData_CostoNetto.PerRiga(calcola(), token, "Crypto",
                prezzo == null ? null : new BigDecimal(prezzo));
    }

    // ------------------------------------------------------------------
    // Gli esempi spiegati all'utente
    // ------------------------------------------------------------------

    @Test
    void schedaPortafoglioConAcquistiEVendite() {
        //Scheda in stile "My Portfolio" con dati inventati: costo totale 300,00, vendite 200,00, proprieta' 600: costo medio netto 0,166667
        acquisto("2026-03-01 10:00", "DEMO", "100", "20.00");
        acquisto("2026-03-10 10:00", "DEMO", "500", "80.00");
        acquisto("2026-03-15 10:00", "DEMO", "200", "150.00");
        acquisto("2026-07-29 10:00", "DEMO", "100", "50.00");
        vendita("2026-03-25 10:00", "DEMO", "50", "40.00");
        vendita("2026-04-05 10:00", "DEMO", "100", "60.00");
        vendita("2026-05-13 10:00", "DEMO", "150", "100.00");

        Principale_GiacenzeaData_CostoNetto.Riga r = riga("DEMO", "0.50");
        assertEquals(300.00, r.Speso, TOLLERANZA);
        assertEquals(200.00, r.Incassato, TOLLERANZA);
        assertEquals(100.00 / 600.0, r.CostoMedioNetto, 0.00001);
        assertEquals(0.166667, r.CostoMedioNetto, 0.00001);
        //valore 600 x 0,50 + 200,00 - 300,00
        assertEquals(600 * 0.50 + 200.00 - 300.00, r.ProfittoPerdita, 0.01);
        assertEquals("", r.Note);
    }

    @Test
    void tokenXyzInCinquePassi() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        assertEquals(1.0, riga("XYZ", "6").CostoMedioNetto, TOLLERANZA);

        acquisto("2026-01-02 10:00", "XYZ", "100", "300.00");
        assertEquals(2.0, riga("XYZ", "6").CostoMedioNetto, TOLLERANZA);

        vendita("2026-01-03 10:00", "XYZ", "100", "400.00");
        assertEquals(0.0, riga("XYZ", "6").CostoMedioNetto, TOLLERANZA);

        vendita("2026-01-04 10:00", "XYZ", "50", "250.00");
        assertEquals(-5.0, riga("XYZ", "6").CostoMedioNetto, TOLLERANZA);

        acquisto("2026-01-05 10:00", "XYZ", "100", "200.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "6");
        assertEquals(-1.0 / 3.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals(600.0, r.Speso, TOLLERANZA);
        assertEquals(650.0, r.Incassato, TOLLERANZA);
        //150 x 6 + 650 - 600
        assertEquals(950.0, r.ProfittoPerdita, TOLLERANZA);
    }

    @Test
    void quattroPassiConAcquistoDopoLaVendita() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        acquisto("2026-01-02 10:00", "XYZ", "100", "300.00");
        vendita("2026-01-03 10:00", "XYZ", "100", "400.00");
        acquisto("2026-01-04 10:00", "XYZ", "100", "500.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "6");
        assertEquals(2.5, r.CostoMedioNetto, TOLLERANZA);
        //200 x 6 + 400 - 900, uguale per ogni metodo di costo
        assertEquals(700.0, r.ProfittoPerdita, TOLLERANZA);
    }

    @Test
    void pareggioEIlPrezzoACuiIlProfittoTotaleEZero() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        acquisto("2026-01-02 10:00", "XYZ", "100", "300.00");
        double pareggio = riga("XYZ", "6").CostoMedioNetto;
        assertEquals(0.0, riga("XYZ", Double.toString(pareggio)).ProfittoPerdita, 0.001);
    }

    // ------------------------------------------------------------------
    // Una regola per riga della tabella delle decisioni
    // ------------------------------------------------------------------

    @Test
    void rewardEntraAZero() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "RW", "", "STAKING REWARD", "", "", "", "XYZ", "Crypto", "10", "15.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(100.0 / 110.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals(100.0, r.Speso, TOLLERANZA);
        //110 x 2 - 100
        assertEquals(120.0, r.ProfittoPerdita, TOLLERANZA);
        assertEquals("", r.Note, "una reward a zero e' una scelta, non una anomalia");
    }

    @Test
    void depositoNonClassificatoAZeroESegnalato() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        depositoNC("2026-01-02 10:00", "XYZ", "50");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(100.0 / 150.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals("1 deposito non classificato a zero", r.Note);
    }

    @Test
    void prelievoNonClassificatoSenzaIncassoESegnalato() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        prelievoNC("2026-01-02 10:00", "XYZ", "50", "100.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(2.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals(0.0, r.Incassato, TOLLERANZA);
        assertEquals("1 prelievo non classificato senza incasso", r.Note);
    }

    @Test
    void depositoACostoZeroEDonazioneRicevutaNonSonoDepositiNonClassificati() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "DC", "DCZ - DEPOSITO A COSTO 0", "DEPOSITO A COSTO 0", "", "", "", "XYZ", "Crypto", "10", "0.00");
        riga("2026-01-03 10:00", "DC", "DDO - Donazione", "DONAZIONE", "", "", "", "XYZ", "Crypto", "10", "20.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "1");
        assertEquals(100.0 / 120.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals(100.0, r.Speso, TOLLERANZA);
        assertEquals("", r.Note);
    }

    @Test
    void depositoClassificatoComeAcquistoSpendeIlValore() {
        riga("2026-01-02 10:00", "DC", "DAC - Acquisto Crypto", "ACQUISTO CRYPTO", "", "", "", "XYZ", "Crypto", "100", "130.00");
        //Come l'acquisto Coinbase pagato con carta : categoria AC nell'ID, nessuna gamba FIAT
        riga("2026-01-03 10:00", "AC", "", "ACQUISTO CRYPTO", "", "", "", "XYZ", "Crypto", "100", "170.00");
        assertEquals(300.0, riga("XYZ", "1").Speso, TOLLERANZA);
        assertEquals(1.5, riga("XYZ", "1").CostoMedioNetto, TOLLERANZA);
    }

    @Test
    void movimentoSenzaPrezzoRendeVuotoIlCostoESegnalaLaRiga() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        scambio("2026-01-02 10:00", "XYZ", "50", "ABC", "20", "");
        Principale_GiacenzeaData_CostoNetto.Riga xyz = riga("XYZ", "2");
        Principale_GiacenzeaData_CostoNetto.Riga abc = riga("ABC", "2");
        assertNull(xyz.CostoMedioNetto);
        assertNull(xyz.ProfittoPerdita);
        assertNull(abc.CostoMedioNetto);
        assertEquals("1 movimento senza prezzo", xyz.Note);
        assertEquals("1 movimento senza prezzo", abc.Note);
    }

    @Test
    void valoreZeroScrittoEUnValoreValido() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        scambio("2026-01-02 10:00", "XYZ", "50", "ABC", "20", "0.00");
        assertEquals(2.0, riga("XYZ", "2").CostoMedioNetto, TOLLERANZA);
        assertEquals("", riga("XYZ", "2").Note);
        assertEquals(0.0, riga("ABC", "2").CostoMedioNetto, TOLLERANZA);
    }

    @Test
    void ilSenzaPrezzoNonScattaPerRewardETrasferimenti() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "RW", "", "REWARD", "", "", "", "XYZ", "Crypto", "0.0000001", "");
        assertEquals("", riga("XYZ", "1").Note);
        assertNotNull(riga("XYZ", "1").CostoMedioNetto);
    }

    @Test
    void commissioneInCriptoEscePerQuantitaSenzaIncasso() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "CM", "", "COMMISSIONI", "XYZ", "Crypto", "-1", "", "", "", "2.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(100.0 / 99.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals(0.0, r.Incassato, TOLLERANZA);
        //99 x 2 - 100
        assertEquals(98.0, r.ProfittoPerdita, TOLLERANZA);
    }

    @Test
    void commissioneRettificataComePcoNonESuccessoIncassoMaUnCashoutSi() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        //Rettifica DeFi/CRO : categoria PC, campo 18 PCO, descrizione COMMISSIONI : come nel motore e' una commissione
        riga("2026-01-02 10:00", "PC", "PCO - Commissione", "COMMISSIONI", "XYZ", "Crypto", "-1", "", "", "", "2.00");
        assertEquals(0.0, riga("XYZ", "2").Incassato, TOLLERANZA);
        //Un cashout o pagamento di un servizio e' un incasso
        riga("2026-01-03 10:00", "PC", "PCO - CASHOUT O SIMILARE", "CASHOUT O SIMILARE", "XYZ", "Crypto", "-10", "", "", "", "30.00");
        assertEquals(30.0, riga("XYZ", "2").Incassato, TOLLERANZA);
    }

    @Test
    void rettificaPwnERimborsoDiRewardNonIncassano() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "PC", "PWN - RETTIFICA GIACENZA", "RETTIFICA GIACENZA", "XYZ", "Crypto", "-10", "", "", "", "20.00");
        riga("2026-01-03 10:00", "RW", "", "RIMBORSO STAKING REWARD", "XYZ", "Crypto", "-5", "", "", "", "0.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(0.0, r.Incassato, TOLLERANZA);
        assertEquals(100.0 / 85.0, r.CostoMedioNetto, TOLLERANZA);
        assertEquals("", r.Note, "PWN e rimborso di reward sono classificati, non sono prelievi non classificati");
    }

    @Test
    void trasferimentoFraWalletNonMuoveDenaroMaLaCommissioneDiReteSiVedeNellaQuantita() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "PC", "PTW - Trasferimento tra Wallet di proprietà", "TRASFERIMENTO", "XYZ", "Crypto", "-40", "", "", "", "80.00");
        riga("2026-01-02 10:01", "DC", "DTW - Trasferimento tra Wallet di proprietà", "TRASFERIMENTO", "", "", "", "XYZ", "Crypto", "39.5", "79.00");
        riga("2026-01-03 10:00", "TI", "", "TRASFERIMENTO INTERNO", "XYZ", "Crypto", "-10", "XYZ", "Crypto", "10", "20.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(100.0, r.Speso, TOLLERANZA);
        assertEquals(0.0, r.Incassato, TOLLERANZA);
        assertEquals(100.0 / 99.5, r.CostoMedioNetto, TOLLERANZA);
        assertEquals("", r.Note);
    }

    @Test
    void scambioCriptoCriptoIncassaAESpendeB() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        scambio("2026-01-02 10:00", "XYZ", "50", "ABC", "20", "100.00");
        Principale_GiacenzeaData_CostoNetto.Riga xyz = riga("XYZ", "2");
        Principale_GiacenzeaData_CostoNetto.Riga abc = riga("ABC", "2");
        assertEquals(100.0, xyz.Incassato, TOLLERANZA);
        assertEquals(0.0, xyz.CostoMedioNetto, TOLLERANZA);
        assertEquals(100.0, abc.Speso, TOLLERANZA);
        assertEquals(5.0, abc.CostoMedioNetto, TOLLERANZA);
    }

    @Test
    void scambioConUnNftEUnaVenditaOUnAcquistoDellaGambaCripto() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        riga("2026-01-02 10:00", "SC", "", "ACQUISTO NFT", "XYZ", "Crypto", "-10", "BAYC", "NFT", "1", "30.00");
        assertEquals(30.0, riga("XYZ", "1").Incassato, TOLLERANZA);
        assertNull(Principale_GiacenzeaData_CostoNetto.PerRiga(calcola(), "BAYC", "NFT", null).Speso);
    }

    @Test
    void fallimentoComeScambioNormale() {
        acquisto("2026-01-01 10:00", "BTC", "1", "50000.00");
        riga("2026-02-01 10:00", "SC", "FBL - BLOCCO", "BLOCCO PER FALLIMENTO", "BTC", "Crypto", "-1", "CELSIUS.CREDITO.BTC", "Crypto", "1", "30000.00");
        Principale_GiacenzeaData_CostoNetto.Riga credito = riga("CELSIUS.CREDITO.BTC", null);
        assertEquals(30000.0, credito.Speso, TOLLERANZA);
        assertEquals(30000.0, credito.CostoMedioNetto, TOLLERANZA);
        assertNull(riga("BTC", null).CostoMedioNetto, "quantita' zero: costo non calcolabile");
        assertEquals(30000.0, riga("BTC", null).Incassato, TOLLERANZA);
    }

    // ------------------------------------------------------------------
    // Commissioni in euro collegate
    // ------------------------------------------------------------------

    private String[] commissioneFiat(String data, String euro, String chiave) {
        String[] fee = riga(data, "CM", "", "COMMISSIONI", "EUR", "FIAT", "-" + euro, "", "", "", euro);
        fee[CommissioniCollegate.CAMPO] = chiave;
        return fee;
    }

    @Test
    void commissioneInEuroSiSommaAlloSpesoDellAcquistoDelSuoGruppo() {
        String[] ac = acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        ac[CommissioniCollegate.CAMPO] = "K1";
        commissioneFiat("2026-01-01 10:00", "5.00", "K1");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(105.0, r.Speso, TOLLERANZA);
        assertEquals(1.05, r.CostoMedioNetto, TOLLERANZA);
        //100 x 2 - 105
        assertEquals(95.0, r.ProfittoPerdita, TOLLERANZA);
    }

    @Test
    void commissioneInEuroSenzaDestinazioneUnivocaNonSiAttribuisce() {
        String[] ac1 = acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        String[] ac2 = acquisto("2026-01-01 10:00", "ABC", "10", "50.00");
        ac1[CommissioniCollegate.CAMPO] = "K1";
        ac2[CommissioniCollegate.CAMPO] = "K1";
        commissioneFiat("2026-01-01 10:00", "5.00", "K1");
        commissioneFiat("2026-01-01 10:00", "7.00", "SENZA-DESTINAZIONE");
        assertEquals(100.0, riga("XYZ", "2").Speso, TOLLERANZA);
        assertEquals(50.0, riga("ABC", "2").Speso, TOLLERANZA);
    }

    // ------------------------------------------------------------------
    // Casi di contorno della riga
    // ------------------------------------------------------------------

    @Test
    void giacenzaNullaONegativaLasciaVuoteLeColonneDerivate() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        vendita("2026-01-02 10:00", "XYZ", "100", "150.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", "2");
        assertEquals(100.0, r.Speso, TOLLERANZA);
        assertEquals(150.0, r.Incassato, TOLLERANZA);
        assertNull(r.CostoMedioNetto);
        assertNull(r.ProfittoPerdita);

        vendita("2026-01-03 10:00", "XYZ", "10", "20.00");
        assertNull(riga("XYZ", "2").CostoMedioNetto, "venduto piu' di quanto posseduto");
    }

    @Test
    void senzaPrezzoIlProfittoResteVuotoMaIlCostoNo() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        Principale_GiacenzeaData_CostoNetto.Riga r = riga("XYZ", null);
        assertEquals(1.0, r.CostoMedioNetto, TOLLERANZA);
        assertNull(r.ProfittoPerdita);
    }

    @Test
    void fiatNftENonCriptoNonHannoColonne() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        Principale_GiacenzeaData_CostoNetto.Risultato ris = calcola();
        for (String tipo : new String[]{"FIAT", "NFT", ""}) {
            Principale_GiacenzeaData_CostoNetto.Riga r = Principale_GiacenzeaData_CostoNetto.PerRiga(ris, "EUR", tipo, null);
            assertNull(r.Speso);
            assertNull(r.CostoMedioNetto);
        }
    }

    @Test
    void ilPoolEPerSimboloEIlBridgeFraReteNonLoFalsa() {
        String[] ac = acquisto("2026-01-01 10:00", "ETH", "100", "100.00");
        ac[28] = "";
        //Sposto 40 ETH su un'altra rete : il pool non guarda ne' address ne' rete
        String[] pr = riga("2026-01-02 10:00", "PC", "PTW - Bridge", "BRIDGE", "ETH", "Crypto", "-40", "", "", "", "0.00");
        pr[26] = "";
        String[] dp = riga("2026-01-02 10:01", "DC", "DTW - Bridge", "BRIDGE", "", "", "", "ETH", "Crypto", "40", "0.00");
        dp[28] = "0xabc";
        assertEquals(1.0, riga("ETH", "2").CostoMedioNetto, TOLLERANZA);
        assertEquals(100.0, riga("ETH", "2").Speso, TOLLERANZA);
    }

    @Test
    void iMovimentiDopoLaDataNonContano() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        acquisto("2026-06-01 10:00", "XYZ", "100", "900.00");
        Principale_GiacenzeaData_CostoNetto.Risultato ris = Principale_GiacenzeaData_CostoNetto.Calcola(
                movimenti, FunzioniDate.ConvertiDatainLong("2026-03-01 00:00"), null);
        assertEquals(1.0, Principale_GiacenzeaData_CostoNetto.PerRiga(ris, "XYZ", "Crypto", null).CostoMedioNetto, TOLLERANZA);
    }

    @Test
    void ilCalcoloNonDipendeDallOrdineDeiMovimenti() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        vendita("2026-01-02 10:00", "XYZ", "40", "100.00");
        acquisto("2026-01-03 10:00", "XYZ", "10", "30.00");
        double dritto = riga("XYZ", "2").CostoMedioNetto;
        java.util.Collections.reverse(movimenti);
        assertEquals(dritto, riga("XYZ", "2").CostoMedioNetto, TOLLERANZA);
    }

    @Test
    void interrompibile() {
        acquisto("2026-01-01 10:00", "XYZ", "100", "100.00");
        Principale_GiacenzeaData_CostoNetto.Risultato ris = Principale_GiacenzeaData_CostoNetto.Calcola(
                movimenti, DATA_LONTANA, () -> true);
        assertNull(ris.Pool("XYZ", "Crypto"), "annullato prima del primo movimento: nessun dato");
    }
}
