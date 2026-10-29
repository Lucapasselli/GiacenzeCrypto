package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il fallimento di una piattaforma ({@link FallimentiPiattaforme}) nel motore delle plusvalenze, sugli esempi
 * concordati in {@code nocommit/Documentazione/Analisi_Fallimenti_Piattaforme.md} (§7.2, metodo A').
 *
 * <p>Le righe sono quelle che scriverà il dialogo: blocco {@code BTC → CELSIUS.CREDITO.BTC} con {@code FBL}, tranche
 * dal token credito alla moneta ricevuta con {@code FRB} (quantità uscita = frazione consumata già calcolata),
 * chiusura con {@code PWN}. Dati comuni: credito di 1 BTC alla data della domanda = 20.000 euro, tranche del
 * 57,65% (11.530 euro in BTC) e del 2,75% (550 euro).
 */
class FallimentiPiattaformeTest {

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
        //GuardiaRete lo accende una volta per classe, e i test del quadro RW lo spengono nel finally
        Funzioni.ReteDisabilitataPerTest = true;
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_CREDITO);
        FallimentiPiattaforme.CancellaProcedura("CELSIUS");
        FallimentiPiattaforme.CambioUSDEUR = (usd, giorno) -> new BigDecimal(usd).multiply(new BigDecimal("0.9")).toPlainString();
        FallimentiPiattaforme.Invalida();
    }

    @AfterAll
    static void ripristinaCambio() {
        FallimentiPiattaforme.CambioUSDEUR = Prezzi::CambioUSDEUR;
        FallimentiPiattaforme.Invalida();
    }

    private static String[] movimento(String data, String tipoID, String campo18,
            String monetaU, String tipoU, String qtaU, String monetaE, String tipoE, String qtaE, String valore) {
        String id = data.replace(":", ".") + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "Celsius";
        m[8] = monetaU;
        m[9] = tipoU;
        //Come nei movimenti veri l'uscita è negativa: il motore delle plusvalenze usa il valore assoluto,
        //le giacenze del quadro RW e di Giacenze a data sommano col segno
        m[10] = qtaU.isEmpty() || qtaU.startsWith("-") ? qtaU : "-" + qtaU;
        m[11] = monetaE;
        m[12] = tipoE;
        m[13] = qtaE;
        m[15] = valore;
        m[18] = campo18;
        m[22] = "M";
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    private static String[] acquisto(String data, String moneta, String qta, String valore) {
        return movimento(data, "AC", "", "EUR", "FIAT", valore, moneta, "Crypto", qta, valore);
    }

    private static String[] blocco(String data, String moneta, String qta, String valoreCredito) {
        return movimento(data, "SC", FallimentiPiattaforme.CAMPO18_BLOCCO,
                moneta, "Crypto", qta, "CELSIUS.CREDITO." + moneta, "Crypto", qta, valoreCredito);
    }

    private static String[] rimborsoInCripto(String data, String credito, String qtaCredito,
            String moneta, String qta, String valore) {
        return movimento(data, "SC", FallimentiPiattaforme.CAMPO18_RIMBORSO,
                credito, "Crypto", qtaCredito, moneta, "Crypto", qta, valore);
    }

    private static String[] rimborsoInEuro(String data, String credito, String qtaCredito, String valore) {
        return movimento(data, "VC", FallimentiPiattaforme.CAMPO18_RIMBORSO,
                credito, "Crypto", qtaCredito, "EUR", "FIAT", valore, valore);
    }

    private static String[] chiusura(String data, String credito, String qtaResidua) {
        return movimento(data, "PC", "PWN - CHIUSURA PROCEDURA FALLIMENTO",
                credito, "Crypto", qtaResidua, "", "", "", "0.00");
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    // ------------------------------------------------------------------
    // Esempi del §7.2
    // ------------------------------------------------------------------

    /** Caso 1 con recupero atteso 60% (A'): costo 5.000, plusvalenze 6.725,83 e 354,17, residuo zero. */
    @Test
    void costoBasso_recuperoAtteso60_plusvalenzaUgualeAlGuadagnoReale() {
        acquisto("2021-01-01 10:00", "BTC", "1", "5000");
        String[] bl = blocco("2022-07-13 00:00", "BTC", "1", "20000");
        //57,65 / 60 = 0,9608333... del credito bloccato
        String[] t1 = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", "0.96083333333333",
                "BTC", "0.27", "11530");
        //Resta il 3,9166...%: la tranche del 2,75% / 60 lo supera e consuma solo il residuo
        String[] t2 = rimborsoInEuro("2025-03-01 10:00", "CELSIUS.CREDITO.BTC", "0.03916666666667", "550");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("5000.00", bl[16]);
        assertEquals("5000.00", bl[17]);
        assertEquals("0.00", bl[19]);
        assertEquals("N", bl[33]);

        assertEquals("4804.17", t1[16]);
        assertEquals("11530", t1[17]);
        assertEquals("6725.83", t1[19]);
        assertEquals("S", t1[33]);

        assertEquals("195.83", t2[16]);
        assertEquals("354.17", t2[19]);
        assertEquals("S", t2[33]);
        assertEquals("", t2[38]);

        assertEquals(0, bd(t1[19]).add(bd(t2[19])).compareTo(bd("7080.00")), "12.080 ricevuti - 5.000 di costo");
    }

    /** Caso 2 con recupero atteso 100% (A): costo 30.000, minusvalenze sulle tranche, residuo perso senza minusvalenza. */
    @Test
    void costoAlto_recuperoAtteso100_minusvalenzeSulleTrancheEChiusuraNeutra() {
        acquisto("2021-01-01 10:00", "BTC", "1", "30000");
        blocco("2022-07-13 00:00", "BTC", "1", "20000");
        String[] t1 = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", "0.5765", "BTC", "0.27", "11530");
        String[] t2 = rimborsoInEuro("2025-03-01 10:00", "CELSIUS.CREDITO.BTC", "0.0275", "550");
        String[] ch = chiusura("2026-06-30 10:00", "CELSIUS.CREDITO.BTC", "0.396");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("17295.00", t1[16]);
        assertEquals("-5765.00", t1[19]);
        assertEquals("825.00", t2[16]);
        assertEquals("-275.00", t2[19]);
        //Chiusura: il residuo di costo esce dal LIFO senza minusvalenza
        assertEquals("11880.00", ch[16]);
        assertEquals("0.00", ch[19]);
        assertEquals("N", ch[33]);
        assertEquals("", ch[38]);
    }

    /** Due monete perse e due ricevute: pesi sul valore del credito (BTC 2/3, ETH 1/3), metodo A. */
    @Test
    void piuMonete_ripartizioneSulValoreDelCredito() {
        acquisto("2021-01-01 10:00", "BTC", "1", "5000");
        acquisto("2021-01-02 10:00", "ETH", "10", "8000");
        blocco("2022-07-13 00:00", "BTC", "1", "20000");
        blocco("2022-07-13 00:01", "ETH", "10", "10000");
        //Tranche del 57,65%: 0,27 BTC (11.530 euro) e 5.765 euro. Quote delle monete ricevute 2/3 e 1/3
        String[] r1 = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", "0.38433333333333", "BTC", "0.18", "7686.67");
        String[] r2 = rimborsoInEuro("2024-01-15 10:01", "CELSIUS.CREDITO.BTC", "0.19216666666667", "3843.33");
        String[] r3 = rimborsoInCripto("2024-01-15 10:02", "CELSIUS.CREDITO.ETH", "3.84333333333333", "BTC", "0.09", "3843.33");
        String[] r4 = rimborsoInEuro("2024-01-15 10:03", "CELSIUS.CREDITO.ETH", "1.92166666666667", "1921.67");
        String[] ven = movimento("2024-06-01 10:00", "VC", "", "BTC", "Crypto", "0.27", "EUR", "FIAT", "12000", "12000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        BigDecimal plusBTC = bd(r1[19]).add(bd(r2[19]));
        BigDecimal plusETH = bd(r3[19]).add(bd(r4[19]));
        assertTrue(plusBTC.subtract(bd("8647.50")).abs().compareTo(bd("0.01")) <= 0, "BTC: " + plusBTC);
        assertTrue(plusETH.subtract(bd("1153.00")).abs().compareTo(bd("0.01")) <= 0, "ETH: " + plusETH);
        //I BTC ricevuti hanno come costo il loro valore all'arrivo (7.686,67 + 3.843,33)
        assertEquals("11530.00", ven[16]);
        assertEquals("470.00", ven[19]);
    }

    /** Rimborso oltre il valore del credito (FTX): l'eccedenza è semplicemente plusvalenza. */
    @Test
    void rimborsoOltreIlCredito_eccedenzaInPlusvalenza() {
        acquisto("2021-01-01 10:00", "BTC", "1", "1000");
        blocco("2022-11-11 00:00", "BTC", "1", "2000");
        String[] t = rimborsoInEuro("2025-02-18 10:00", "CELSIUS.CREDITO.BTC", "1", "2400");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", t[16]);
        assertEquals("1400.00", t[19]);
    }

    // ------------------------------------------------------------------
    // I marcatori decidono da soli, a prescindere da opzioni e tipi
    // ------------------------------------------------------------------

    @Test
    void blocco2022_conOpzioniScambiRilevanti_restaNeutro() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "SI");
        acquisto("2021-01-01 10:00", "BTC", "1", "1000");
        String[] bl = blocco("2022-07-13 00:00", "BTC", "1", "20000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", bl[17]);
        assertEquals("0.00", bl[19]);
        assertEquals("N", bl[33]);
    }

    @Test
    void bloccoDiUnEmoney_tipiDiversi_restaNeutro() {
        Principale.Mappa_EMoney.put("USDC", "2000-01-01");
        acquisto("2024-01-01 10:00", "USDC", "1000", "900");
        String[] bl = blocco("2024-07-13 00:00", "USDC", "1000", "920");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("900.00", bl[17]);
        assertEquals("0.00", bl[19]);
        assertEquals("N", bl[33]);
    }

    @Test
    void restituzioneInNatura_conScambiSempreRilevanti_trasferisceIlCosto() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "SI");
        acquisto("2021-01-01 10:00", "BTC", "1", "1000");
        blocco("2022-07-13 00:00", "BTC", "1", "20000");
        String[] gc = movimento("2023-03-01 10:00", "SC", FallimentiPiattaforme.CAMPO18_RESTITUZIONE_IN_NATURA,
                "CELSIUS.CREDITO.BTC", "Crypto", "0.725", "BTC", "Crypto", "0.725", "15000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("725.00", gc[16]);
        assertEquals("725.00", gc[17]);
        assertEquals("0.00", gc[19]);
        assertEquals("N", gc[33]);
    }

    @Test
    void movimentoRilevante_seguiIlMarcatore() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "SI");
        String[] bl = blocco("2022-07-13 00:00", "BTC", "1", "20000");
        String[] gc = movimento("2023-03-01 10:00", "SC", FallimentiPiattaforme.CAMPO18_RESTITUZIONE_IN_NATURA,
                "CELSIUS.CREDITO.BTC", "Crypto", "0.5", "BTC", "Crypto", "0.5", "15000");
        assertFalse(Funzioni.MovimentoRilevante(bl));
        assertFalse(Funzioni.MovimentoRilevante(gc));

        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "NO");
        String[] rb = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", "0.5", "ETH", "4", "11530");
        assertTrue(Funzioni.MovimentoRilevante(rb));
    }

    @Test
    void quadroRW_bloccoNonChiude_rimborsoChiudeTranneConIlMetodoB() {
        Moneta[] m = { new Moneta(), new Moneta() };
        m[0].Moneta = "CELSIUS.CREDITO.BTC";
        m[0].Tipo = "Crypto";
        m[1].Moneta = "ETH";
        m[1].Tipo = "Crypto";
        for (String metodo : new String[]{"B", "C", "D"}) {
            assertFalse(Calcoli_RW.ScambioRilevante(m, "2022-07-13 00:00", metodo, FallimentiPiattaforme.CAMPO18_BLOCCO), metodo);
            assertFalse(Calcoli_RW.ScambioRilevante(m, "2023-03-01 00:00", metodo,
                    FallimentiPiattaforme.CAMPO18_RESTITUZIONE_IN_NATURA), metodo);
        }
        assertFalse(Calcoli_RW.ScambioRilevante(m, "2024-01-15 10:00", "B", FallimentiPiattaforme.CAMPO18_RIMBORSO));
        assertTrue(Calcoli_RW.ScambioRilevante(m, "2024-01-15 10:00", "C", FallimentiPiattaforme.CAMPO18_RIMBORSO));
        assertTrue(Calcoli_RW.ScambioRilevante(m, "2024-01-15 10:00", "D", FallimentiPiattaforme.CAMPO18_RIMBORSO));
        //Senza marcatore resta la regola di sempre: stesso tipo, non rilevante col metodo C
        assertFalse(Calcoli_RW.ScambioRilevante(m, "2024-01-15 10:00", "C", ""));
    }

    // ------------------------------------------------------------------
    // Registro e valore del token credito nelle giacenze (opzione RW_ValoreCreditiFallimento)
    // ------------------------------------------------------------------

    private static final long FINE_2023 = FunzioniDate.ConvertiDatainLongMinuto("2023-12-31 23:59");
    private static final long FINE_2024 = FunzioniDate.ConvertiDatainLongMinuto("2024-12-31 23:59");

    private static Moneta credito(String simbolo, String qta) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Qta = qta;
        m.Tipo = "Crypto";
        return m;
    }

    /** Blocco di 1 BTC (costo 5.000, credito 20.000 euro) e tranche del 57,65%, che consuma {@code qtaUscitaTranche} del credito. */
    private static void procedura(String qtaUscitaTranche) {
        acquisto("2021-01-01 10:00", "BTC", "1", "5000");
        String[] bl = blocco("2022-07-13 00:00", "BTC", "1", "20000");
        bl[GruppoOperazione.CAMPO] = FallimentiPiattaforme.PREFISSO_CHIAVE + "CELSIUS";
        String[] t1 = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", qtaUscitaTranche, "BTC", "0.27", "11530");
        t1[GruppoOperazione.CAMPO] = FallimentiPiattaforme.PREFISSO_CHIAVE + "CELSIUS";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
    }

    private static FallimentiPiattaforme.Procedura pianoCelsius() {
        FallimentiPiattaforme.Procedura p = new FallimentiPiattaforme.Procedura();
        p.Id = "CELSIUS";
        p.NomeBreve = "CELSIUS";
        p.Exchange = "Celsius";
        p.DataDomanda = "2022-07-13";
        p.CreditoUSD = new BigDecimal("21000");
        FallimentiPiattaforme.Tranche t = new FallimentiPiattaforme.Tranche();
        t.Data = "2024-01-15 10:00";
        t.Percentuale = new BigDecimal("57.65");
        t.USDPagati = new BigDecimal("12106.50");
        p.Tranche.add(t);
        return p;
    }

    private static BigDecimal valore(String qta, long istante) {
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(credito("CELSIUS.CREDITO.BTC", qta), null, istante, null, "");
        assertNotNull(IP);
        return IP.prezzoQta;
    }

    private static void assertCirca(String atteso, BigDecimal valore) {
        assertTrue(valore.subtract(new BigDecimal(atteso)).abs().compareTo(new BigDecimal("0.01")) <= 0,
                "atteso " + atteso + ", ottenuto " + valore.toPlainString());
    }

    @Test
    void valoreCredito_senzaProcedura_valoreUnitarioAllaDataDellaDomanda() {
        procedura("0.5765");
        FallimentiPiattaforme.Ricostruisci();

        assertCirca("20000", valore("1", FINE_2023));
        assertCirca("8470", valore("0.4235", FINE_2024));
    }

    @Test
    void valoreCredito_conProcedura_creditoNominaleResiduoAlCambio() {
        procedura("0.5765");
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        FallimentiPiattaforme.Ricostruisci();

        //21.000 dollari × 0,9
        assertCirca("18900", valore("1", FINE_2023));
        //(21.000 - 12.106,50) × 0,9 = 8.004,15
        assertCirca("8004.15", valore("0.4235", FINE_2024));
    }

    @Test
    void valoreCredito_recuperoAttesoMinoreDi100_restaIlCreditoNominaleResiduo() {
        //Con atteso 60% la tranche consuma il 96,08% della quantità, ma il credito residuo resta il 42,35%
        procedura("0.96083333333333");
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        FallimentiPiattaforme.Ricostruisci();

        assertCirca("8004.15", valore("0.03916666666667", FINE_2024));
    }

    @Test
    void valoreCosto_costoUnitarioDelBlocco() {
        DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_COSTO);
        procedura("0.5765");
        FallimentiPiattaforme.Ricostruisci();

        assertCirca("2117.50", valore("0.4235", FINE_2024));
    }

    @Test
    void valorePrezzoOriginale_prezzoDellaMonetaBloccata() {
        DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_PREZZO_ORIGINALE);
        //Moneta originale un e-money in euro: prezzo 1 senza rete
        Principale.Mappa_EMoney.put("EURe", "2020-01-01");
        acquisto("2021-01-01 10:00", "EURe", "1000", "1000");
        blocco("2022-07-13 00:00", "EURe", "1000", "1000");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        FallimentiPiattaforme.Ricostruisci();

        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(credito("CELSIUS.CREDITO.EURe", "1000"), null, FINE_2023, null, "");
        assertNotNull(IP);
        assertCirca("1000", IP.prezzoQta);
    }

    @Test
    void cambioDiOpzione_laValutazioneSuccessivaUsaLaNuovaScelta() {
        //Nessuna cache fra l'opzione e il prezzo: il quadro W/RW ricalcolato dopo il cambio ha i valori nuovi
        procedura("0.5765");
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        FallimentiPiattaforme.Ricostruisci();

        assertCirca("8004.15", valore("0.4235", FINE_2024));
        DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_COSTO);
        assertCirca("2117.50", valore("0.4235", FINE_2024));
        assertEquals("costo di carico trasferito al credito", FallimentiPiattaforme.DescrizioneValore());
        DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_CREDITO);
        assertCirca("8004.15", valore("0.4235", FINE_2024));
    }

    @Test
    void opzioneNellaDichiarazione_soloNegliAnniConUnCredito() {
        //Credito bloccato il 13/07/2022 e rimborsato per intero il 15/01/2024
        procedura("1");
        FallimentiPiattaforme.Ricostruisci();

        assertFalse(FallimentiPiattaforme.InfluisconoSullAnno("2021"));
        assertTrue(FallimentiPiattaforme.InfluisconoSullAnno("2022"));
        assertTrue(FallimentiPiattaforme.InfluisconoSullAnno("2023"));
        assertTrue(FallimentiPiattaforme.InfluisconoSullAnno("2024"));
        assertFalse(FallimentiPiattaforme.InfluisconoSullAnno("2025"));

        Principale.MappaCryptoWallet.clear();
        FallimentiPiattaforme.Invalida();
        assertFalse(FallimentiPiattaforme.InfluisconoSullAnno("2023"));
    }

    @Test
    void monetaNonCredito_nonToccata() {
        procedura("0.5765");
        FallimentiPiattaforme.Ricostruisci();
        assertNull(FallimentiPiattaforme.PrezzoCredito(credito("BTC", "1"), FINE_2024));
    }

    // ------------------------------------------------------------------
    // Fino alle tabelle che l'utente legge: Giacenze a data e quadro RW
    // (il quadro RT somma i [19] con [33] = "S", fissati dai test del motore qui sopra)
    // ------------------------------------------------------------------

    @Test
    void giacenzeAData_costoResiduoDelCreditoEDeiBTCRicevuti() {
        procedura("0.96083333333333");
        long FineAnno = FunzioniDate.ConvertiDatainLong("2024-12-31") + 86400000L;
        Principale_GiacenzeaData.CostiCaricoRimanenze Costi = Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(FineAnno);

        assertEquals("195.83", Costi.CostoDelleRimanenze("Tutti",
                Principale_GiacenzeaData.ChiaveRiga("CELSIUS.CREDITO.BTC", "Crypto", "", ""), "0.03916666666667"));
        assertEquals("11530.00", Costi.CostoDelleRimanenze("Tutti",
                Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "0.27"));
    }

    /**
     * Quadro RW 2024 sulla procedura col piano: credito di 21.000 dollari (cambio 0,9 → 18.900 euro), tranche del
     * 15/01/2024 del 57,65% ricevuta in EURe (prezzo 1 senza rete). Il credito resta valutato al nominale residuo.
     */
    private static List<String[]> righiRW2024(String Metodo) throws Exception {
        Funzioni.ReteDisabilitataPerTest = true;
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM WALLETGRUPPO");
            st.execute("DELETE FROM GRUPPO_PERIODO_RW");
        }
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        DatabaseH2.Pers_GruppoWallet_Scrivi("Celsius", "Wallet 01");
        for (String[] o : new String[][] {{"RW_Rilevanza", Metodo}, {"RW_ChiudiRWsuTrasferimento", "NO"},
                {"RW_StakingZero", "NO"}, {"RW_LiFoComplessivo", "NO"}, {"RW_LiFoSubMovimenti", "NO"},
                {"RW_FiatInRW", "NO"}}) {
            DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
        }
        Principale.Mappa_EMoney.put("EURe", "2020-01-01");
        acquisto("2021-01-01 10:00", "BTC", "1", "5000");
        String[] bl = blocco("2022-07-13 00:00", "BTC", "1", "20000");
        bl[GruppoOperazione.CAMPO] = FallimentiPiattaforme.PREFISSO_CHIAVE + "CELSIUS";
        String[] t1 = rimborsoInCripto("2024-01-15 10:00", "CELSIUS.CREDITO.BTC", "0.5765", "EURe", "11530", "11530");
        t1[GruppoOperazione.CAMPO] = FallimentiPiattaforme.PREFISSO_CHIAVE + "CELSIUS";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        try {
            Calcoli_RW.AggiornaRWFR("2024");
        } finally {
            Funzioni.ReteDisabilitataPerTest = false;
        }
        List<String[]> lista = Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 01");
        assertNotNull(lista);
        return lista;
    }

    private static String descrivi(List<String[]> lista) {
        return lista.stream().map(x -> x[2] + " " + x[3] + " " + x[4] + " " + x[5] + " → " + x[7] + " " + x[8] + " "
                + x[9] + " " + x[10]).toList().toString();
    }

    private static String[] rigo(List<String[]> lista, java.util.function.Predicate<String[]> p) {
        return lista.stream().filter(p).findFirst().orElseThrow(() -> new AssertionError(descrivi(lista)));
    }

    @Test
    void quadroRW_metodoC_ilRimborsoChiudeIlCreditoAlValoreDellaTranche() throws Exception {
        List<String[]> lista = righiRW2024("C");

        String[] Chiuso = rigo(lista, x -> "CELSIUS.CREDITO.BTC".equals(x[7]) && x[9].startsWith("2024-01-15"));
        assertCirca("10895.85", new BigDecimal(Chiuso[5]));
        assertCirca("11530", new BigDecimal(Chiuso[10]));
        String[] Residuo = rigo(lista, x -> "CELSIUS.CREDITO.BTC".equals(x[7]) && x[9].startsWith("2024-12-31"));
        assertCirca("8004.15", new BigDecimal(Residuo[5]));
        assertCirca("8004.15", new BigDecimal(Residuo[10]));
        String[] Ricevuti = rigo(lista, x -> "EURe".equals(x[7]));
        assertTrue(Ricevuti[4].startsWith("2024-01-15"), descrivi(lista));
        assertCirca("11530", new BigDecimal(Ricevuti[10]));
    }

    @Test
    void quadroRW_metodoB_ilRimborsoInCriptoNonChiudeIlRigo() throws Exception {
        List<String[]> lista = righiRW2024("B");

        assertTrue(lista.stream().noneMatch(x -> "CELSIUS.CREDITO.BTC".equals(x[7]) && x[9].startsWith("2024-01-15")),
                descrivi(lista));
        //Il lotto del credito passa sugli EURe ricevuti: inizia al 01/01 col valore del credito
        String[] Ricevuti = rigo(lista, x -> "EURe".equals(x[7]));
        assertEquals("CELSIUS.CREDITO.BTC", Ricevuti[2], descrivi(lista));
        assertCirca("10895.85", new BigDecimal(Ricevuti[5]));
        assertCirca("11530", new BigDecimal(Ricevuti[10]));
    }

    // ------------------------------------------------------------------
    // Nuova procedura: giacenze alla data, accorpamento e righe di blocco (Principale_Fallimenti)
    // ------------------------------------------------------------------

    private static String[] acquistoSu(String data, String sottoWallet, String moneta, String qta, String valore) {
        String[] m = acquisto(data, moneta, qta, valore);
        m[4] = sottoWallet;
        return m;
    }

    @Test
    void nuovaProcedura_accorpaEBloccaUnaRigaPerMoneta() {
        acquistoSu("2021-01-01 10:00", "Earn", "BTC", "0.8", "4000");
        acquistoSu("2021-02-01 10:00", "Custody", "BTC", "0.2", "6000");
        acquistoSu("2021-03-01 10:00", "Earn", "ETH", "10", "8000");
        //Un movimento dopo la domanda: va segnalato
        acquistoSu("2022-08-01 10:00", "Custody", "ETH", "1", "1500");

        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        assertEquals(3, Pos.size(), () -> Pos.stream().map(p -> p.SottoWallet + " " + p.Moneta + " " + p.Qta).toList().toString());
        for (Principale_Fallimenti.Posizione p : Pos) {
            p.ValoreEUR = p.Moneta.equals("BTC") ? p.Qta.multiply(new BigDecimal("20000")) : new BigDecimal("10000");
        }
        assertEquals(1, Principale_Fallimenti.MovimentiSuccessivi("Celsius", "2022-07-13").size());

        FallimentiPiattaforme.Procedura P = new FallimentiPiattaforme.Procedura();
        P.Id = "CELSIUS";
        P.NomeBreve = "CELSIUS";
        P.Exchange = "Celsius";
        P.DataDomanda = "2022-07-13";
        assertNull(Principale_Fallimenti.CreaProcedura(P, Pos));
        assertNotNull(Principale_Fallimenti.CreaProcedura(P, Pos), "una seconda procedura con lo stesso nome va rifiutata");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        List<String[]> Blocchi = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.TrasferisceCosto(v[18])).toList();
        assertEquals(2, Blocchi.size(), "una riga di blocco per moneta");
        String[] BloccoBTC = Blocchi.stream().filter(v -> v[8].equals("BTC")).findFirst().orElseThrow();
        assertEquals("CELSIUS.CREDITO.BTC", BloccoBTC[11]);
        assertEquals(Principale_Fallimenti.SOTTOWALLET, BloccoBTC[4]);
        assertEquals("20000.00", BloccoBTC[15]);
        assertEquals("10000.00", BloccoBTC[17], "i due lotti dei BTC confluiscono nel costo del credito");
        assertEquals("0.00", BloccoBTC[19]);
        assertTrue(BloccoBTC[0].endsWith("_SC"));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[0].contains("Fallimento CELSIUS")) {
                assertEquals("FALL-CELSIUS", v[GruppoOperazione.CAMPO], v[0]);
                assertEquals("M", v[22], v[0]);
                assertFalse(v[14].isBlank(), "il valore in [14] tiene la riga fuori dai ricalcoli dei prezzi: " + v[0]);
            }
        }
        //Dopo il blocco la piattaforma non ha più BTC e ha il credito nel sotto-wallet
        long Dopo = FunzioniDate.ConvertiDatainLong("2022-07-14");
        java.util.Map<String, Moneta> Earn = Principale_GiacenzeaData.SommaQuantitaAData(
                Principale.MappaCryptoWallet.values(), Dopo, "Celsius", "Earn");
        //(gli euro negativi sono gli acquisti di prova, fatti senza deposito)
        assertTrue(Earn.values().stream().filter(m -> !m.Tipo.equals("FIAT")).allMatch(m -> new BigDecimal(m.Qta).signum() == 0),
                () -> Earn.values().stream().map(m -> m.Moneta + " " + m.Qta).toList().toString());
        java.util.Map<String, Moneta> Credito = Principale_GiacenzeaData.SommaQuantitaAData(
                Principale.MappaCryptoWallet.values(), Dopo, "Celsius", Principale_Fallimenti.SOTTOWALLET);
        assertTrue(Credito.values().stream().anyMatch(m -> m.Moneta.equals("CELSIUS.CREDITO.BTC") && new BigDecimal(m.Qta).compareTo(BigDecimal.ONE) == 0));
        assertEquals("Fallimento CELSIUS", GruppoOperazione.Descrizione("FALL-CELSIUS"));
        //Una seconda procedura sulla stessa piattaforma non ripropone i token credito della prima
        FallimentiPiattaforme.Invalida();
        assertTrue(Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-20").stream()
                .noneMatch(p -> FallimentiPiattaforme.isTokenCredito(p.Moneta)));

        //Il giro di salvataggio e ricaricamento del file dei movimenti non perde chiave, valore e marcatore
        String[] Riletta = Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(BloccoBTC));
        assertFalse(GruppoOperazione.MigraAllaFormaAttuale(Riletta), "la riga della procedura non va migrata");
        assertEquals("FALL-CELSIUS", Riletta[GruppoOperazione.CAMPO]);
        assertEquals(FallimentiPiattaforme.CAMPO18_BLOCCO, Riletta[18]);
        assertEquals(BloccoBTC[14], Riletta[14]);
        assertEquals("M", Riletta[22]);
    }

    @Test
    void proceduraRegistrataSenzaMovimenti_siPuoRicreare() {
        //Creata e mai salvata: alla riapertura la tabella c'è, i movimenti no
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        acquistoSu("2021-01-01 10:00", "Earn", "BTC", "1", "5000");
        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        Pos.forEach(p -> p.ValoreEUR = new BigDecimal("20000"));
        FallimentiPiattaforme.Procedura P = pianoCelsius();
        P.Tranche.clear();
        P.Exchange = "Celsius";
        assertNull(Principale_Fallimenti.CreaProcedura(P, Pos));
        assertEquals(0, FallimentiPiattaforme.LeggiProcedure().get("CELSIUS").Tranche.size());
    }

    @Test
    void valoreCredito_iFiatDellaPiattaformaPesanoNelCredito() {
        procedura("0.5765");
        FallimentiPiattaforme.Procedura P = pianoCelsius();
        //20.000 euro di FIAT accanto ai 20.000 del BTC: al BTC tocca metà del credito
        P.FiatEUR = new BigDecimal("20000");
        assertTrue(FallimentiPiattaforme.ScriviProcedura(P));
        FallimentiPiattaforme.Ricostruisci();

        assertCirca("9450", valore("1", FINE_2023));
        assertEquals(0, new BigDecimal("20000").compareTo(FallimentiPiattaforme.LeggiProcedure().get("CELSIUS").FiatEUR));
    }

    @Test
    void nomiToken_omonimiConSuffisso() {
        Principale_Fallimenti.Posizione a = new Principale_Fallimenti.Posizione();
        a.Moneta = "USDC";
        a.Rete = "ETH";
        Principale_Fallimenti.Posizione b = new Principale_Fallimenti.Posizione();
        b.Moneta = "USDC";
        b.Rete = "ARB";
        java.util.Map<String, String> Nomi = Principale_Fallimenti.NomiToken("CELSIUS", List.of(a, b));
        assertEquals("CELSIUS.CREDITO.USDC", Nomi.get(a.ChiaveMoneta()));
        assertEquals("CELSIUS.CREDITO.USDC-2", Nomi.get(b.ChiaveMoneta()));
        assertEquals("NEXOPRO", Principale_Fallimenti.NomeBreveProposto("Nexo Pro"));
    }

    // ------------------------------------------------------------------
    // Tranche di rimborso (Principale_Fallimenti.RegistraTranche)
    // ------------------------------------------------------------------

    /** Deposito importato su un'altra piattaforma, con l'ID nella forma vera (data al secondo in testa). */
    private static String[] depositoSu(String exchange, String idData, String data, String moneta, String tipo, String qta, String valore) {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = idData + "_" + exchange + "_001_001_" + (tipo.equals("FIAT") ? "DF" : "DC");
        m[1] = data;
        m[3] = exchange;
        m[4] = "Main";
        m[5] = "DEPOSITO";
        m[11] = moneta;
        m[12] = tipo;
        m[13] = qta;
        m[15] = valore;
        m[22] = "A";
        Principale.MappaCryptoWallet.put(m[0], m);
        return m;
    }

    /** Procedura Celsius creata col dialogo: 1 BTC (costo 5.000, credito 20.000 euro, 21.000 dollari). */
    private static FallimentiPiattaforme.Procedura proceduraCreata(String atteso) {
        acquistoSu("2021-01-01 10:00", "Earn", "BTC", "1", "5000");
        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        Pos.forEach(p -> p.ValoreEUR = new BigDecimal("20000"));
        FallimentiPiattaforme.Procedura P = new FallimentiPiattaforme.Procedura();
        P.Id = "CELSIUS";
        P.NomeBreve = "CELSIUS";
        P.Exchange = "Celsius";
        P.DataDomanda = "2022-07-13";
        P.CreditoUSD = new BigDecimal("21000");
        P.RecuperoAtteso = new BigDecimal(atteso);
        assertNull(Principale_Fallimenti.CreaProcedura(P, Pos));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        return P;
    }

    private static Principale_Fallimenti.Ricevuta ricevuta(String moneta, String tipo, String qta, String valore, String idDeposito) {
        Principale_Fallimenti.Ricevuta r = new Principale_Fallimenti.Ricevuta();
        r.Moneta = moneta;
        r.Tipo = tipo;
        r.Qta = new BigDecimal(qta);
        r.ValoreEUR = new BigDecimal(valore);
        r.IDDeposito = idDeposito;
        return r;
    }

    private static BigDecimal sommaPlusvalenze(java.util.function.Predicate<String[]> filtro) {
        BigDecimal s = BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (filtro.test(v) && "S".equals(v[33])) s = s.add(new BigDecimal(v[19]));
        }
        return s;
    }

    @Test
    void tranche_esempioDelCaso1_conRecuperoAtteso60() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("60");
        String[] Dep = depositoSu("Coinbase", "20240115100500", "2024-01-15 10:05", "BTC", "Crypto", "0.27", "11530");

        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("57.65"), null,
                new BigDecimal("60"), List.of(ricevuta("BTC", "Crypto", "0.27", "11530", Dep[0]))));
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-03-01 10:00", new BigDecimal("2.75"), null,
                new BigDecimal("60"), List.of(ricevuta("EUR", "FIAT", "550", "550", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertCirca("6725.83", sommaPlusvalenze(v -> v[1].startsWith("2024")));
        assertCirca("354.17", sommaPlusvalenze(v -> v[1].startsWith("2025")));
        //Il deposito è diventato l'arrivo di un trasferimento e porta il costo dei BTC ricevuti
        assertTrue(Dep[18].startsWith("DTW"), Dep[18]);
        assertEquals("11530.00", Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(FunzioniDate.ConvertiDatainLong("2024-12-31"))
                .CostoDelleRimanenze("Tutti", Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "0.27"));
        //Il credito è finito: la seconda tranche ha consumato solo il residuo
        FallimentiPiattaforme.Invalida();
        assertEquals(0, FallimentiPiattaforme.Registro().Token.get("CELSIUS.CREDITO.BTC")
                .QtaAl(FunzioniDate.ConvertiDatainLongMinuto("2025-12-31 23:59")).signum());
        //Tranche e recupero atteso nel database, dollari pagati = percentuale × credito
        FallimentiPiattaforme.Procedura Letta = FallimentiPiattaforme.LeggiProcedure().get("CELSIUS");
        assertEquals(2, Letta.Tranche.size());
        assertEquals(0, new BigDecimal("12106.50").compareTo(Letta.Tranche.get(0).USDPagati));
        //Credito esaurito: un altro rimborso si registra lo stesso, come rimborso oltre il credito (tutto plusvalenza)
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-06-01 10:00", BigDecimal.ONE, null,
                new BigDecimal("60"), List.of(ricevuta("EUR", "FIAT", "10", "10", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertCirca("10", sommaPlusvalenze(v -> v[1].startsWith("2025-06")));

        //Eliminando la procedura il deposito torna da classificare
        Principale_Fallimenti.EliminaProcedura("CELSIUS");
        assertEquals("", Dep[18]);
        assertEquals("", Dep[20]);
    }

    @Test
    void tranche_iFiatDellaPiattaformaComprano_laLoroQuota() {
        depositoSu("Celsius", "20201201100000", "2020-12-01 10:00", "EUR", "FIAT", "15000", "15000");
        FallimentiPiattaforme.Procedura P = proceduraCreataConFiat();
        String[] Dep = depositoSu("Coinbase", "20240115100500", "2024-01-15 10:05", "BTC", "Crypto", "0.3", "15000");

        Principale_Fallimenti.Tranche T = Principale_Fallimenti.PreparaTranche(P, "2024-01-15 10:00", new BigDecimal("50"),
                new BigDecimal("100"), List.of(ricevuta("BTC", "Crypto", "0.3", "15000", Dep[0])));
        assertNull(T.Errore);
        assertEquals(2, T.Anteprima.size(), "BTC dal credito e BTC comprati con gli euro");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("50"), null,
                new BigDecimal("100"), List.of(ricevuta("BTC", "Crypto", "0.3", "15000", Dep[0]))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Credito BTC (peso 2/3): 0,2 BTC per 10.000, costo 2.500 → 7.500. Quota FIAT (1/3): acquisto, nessun risultato
        assertCirca("7500", sommaPlusvalenze(v -> v[1].startsWith("2024")));
        assertEquals("15000.00", Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(FunzioniDate.ConvertiDatainLong("2024-12-31"))
                .CostoDelleRimanenze("Tutti", Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "0.3"));
    }

    private static FallimentiPiattaforme.Procedura proceduraCreataConFiat() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        P.FiatEUR = new BigDecimal("10000");
        assertTrue(FallimentiPiattaforme.ScriviProcedura(P));
        return P;
    }

    @Test
    void tranche_controlliSugliIngressi() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        String[] Dep = depositoSu("Coinbase", "20240115100500", "2024-01-15 10:05", "BTC", "Crypto", "0.27", "11530");
        //Cripto senza deposito, quantità diversa dal deposito, rimborso non prima del deposito
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2024-01-15 10:00", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("BTC", "Crypto", "0.27", "11530", null))).Errore);
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2024-01-15 10:00", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("BTC", "Crypto", "0.2", "11530", Dep[0]))).Errore);
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2024-01-15 10:05", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("BTC", "Crypto", "0.27", "11530", Dep[0]))).Errore);
        //Una moneta a valore zero accanto a una con valore non avrebbe quota di credito: diventerebbe un deposito senza uscita
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2024-01-15 10:00", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("BTC", "Crypto", "0.27", "11530", Dep[0]), ricevuta("EUR", "FIAT", "5", "0", null))).Errore);
        assertEquals(1, Principale_Fallimenti.DepositiCandidati(P).size());
    }

    @Test
    void movimentiSenzaPiano_nuovaProceduraConLoStessoNomeRifiutata() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        //Ripristino parziale: le righe del blocco ci sono, i dati del piano no
        assertTrue(FallimentiPiattaforme.CancellaProcedura(P.Id));
        assertFalse(FallimentiPiattaforme.LeggiProcedure().containsKey(P.Id));
        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        Pos.forEach(p -> p.ValoreEUR = new BigDecimal("20000"));
        assertNotNull(Principale_Fallimenti.CreaProcedura(P, Pos), "un secondo blocco si sommerebbe al primo");
    }

    @Test
    void tranche_restituzioneInNatura_giroconto() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        String[] Dep = depositoSu("Coinbase", "20230301100500", "2023-03-01 10:05", "BTC", "Crypto", "0.725", "15000");
        Principale_Fallimenti.Ricevuta r = ricevuta("BTC", "Crypto", "0.725", "15000", Dep[0]);
        r.InNatura = true;
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2023-03-01 10:00", new BigDecimal("72.5"), null,
                new BigDecimal("100"), List.of(r)));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertCirca("0", sommaPlusvalenze(v -> true));
        //Costo trasferito: il 72,5% dei 5.000
        assertEquals("3625.00", Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(FunzioniDate.ConvertiDatainLong("2023-12-31"))
                .CostoDelleRimanenze("Tutti", Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "0.725"));
    }

    @Test
    void tranche_registrataEMaiSalvata_nonContaNelCredito() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("57.65"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "11530", "11530", null))));
        FallimentiPiattaforme.Procedura Letta = FallimentiPiattaforme.LeggiProcedure().get("CELSIUS");
        assertEquals(1, FallimentiPiattaforme.TrancheValide(Letta).size());
        //Riapertura senza salvare: la tabella ha la tranche, la mappa no
        Principale.MappaCryptoWallet.values().removeIf(v -> v[1].startsWith("2024-01-15"));
        FallimentiPiattaforme.Invalida();
        assertEquals(0, FallimentiPiattaforme.TrancheValide(Letta).size());
        //Opzione B: 21.000 dollari interi (× 0,9), non il residuo di un pagamento mai avvenuto
        assertCirca("18900", valore("1", FINE_2024));
    }

    @Test
    void tranche_inNaturaConPiuMonete_rifiutata() {
        acquistoSu("2021-01-02 10:00", "Earn", "ETH", "10", "8000");
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        //proceduraCreata ha bloccato anche gli ETH: il credito ha due monete
        String[] Dep = depositoSu("Coinbase", "20230301100500", "2023-03-01 10:05", "BTC", "Crypto", "0.5", "10000");
        Principale_Fallimenti.Ricevuta r = ricevuta("BTC", "Crypto", "0.5", "10000", Dep[0]);
        r.InNatura = true;
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2023-03-01 10:00", new BigDecimal("50"),
                new BigDecimal("100"), List.of(r)).Errore);
    }

    @Test
    void tranche_inDollariSuUnAltroExchange_comeFTX() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        String[] Dep = depositoSu("Kraken", "20250218100500", "2025-02-18 10:05", "USD", "FIAT", "24000", "22000");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-02-18 10:00", new BigDecimal("120"), null,
                new BigDecimal("100"), List.of(ricevuta("USD", "FIAT", "24000", "22000", Dep[0]))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Oltre il 100%: il costo (5.000) si esaurisce, l'eccedenza è plusvalenza
        assertCirca("17000", sommaPlusvalenze(v -> v[1].startsWith("2025")));
        assertTrue(Dep[18].startsWith("DTW"), Dep[18]);
        assertTrue(Principale.MappaCryptoWallet.values().stream().anyMatch(v -> v[0].endsWith("_VC")
                && FallimentiPiattaforme.Realizza(v[18]) && v[11].equals("USD")));
    }

    @Test
    void quadroRW_rimborsoArrivatoSuUnAltroGruppo() throws Exception {
        Funzioni.ReteDisabilitataPerTest = true;
        try {
            try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
                st.execute("DELETE FROM WALLETGRUPPO");
                st.execute("DELETE FROM GRUPPO_PERIODO_RW");
            }
            DatabaseH2.Mappa_Wallet_Gruppo.clear();
            DatabaseH2.Pers_GruppoWallet_Scrivi("Celsius", "Wallet 01");
            DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase", "Wallet 02");
            DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
            for (String[] o : new String[][] {{"RW_Rilevanza", "C"}, {"RW_ChiudiRWsuTrasferimento", "NO"},
                    {"RW_StakingZero", "NO"}, {"RW_LiFoComplessivo", "NO"}, {"RW_LiFoSubMovimenti", "NO"},
                    {"RW_FiatInRW", "NO"}}) {
                DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
            }
            Principale.Mappa_EMoney.put("EURe", "2020-01-01");
            FallimentiPiattaforme.Procedura P = proceduraCreata("100");
            String[] Dep = depositoSu("Coinbase", "20240115100500", "2024-01-15 10:05", "EURe", "Crypto", "11530", "11530");
            assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("57.65"), null,
                    new BigDecimal("100"), List.of(ricevuta("EURe", "Crypto", "11530", "11530", Dep[0]))));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            Calcoli_RW.AggiornaRWFR("2024");

            List<String[]> Coinbase = Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 02");
            assertNotNull(Coinbase);
            String[] Ricevuti = rigo(Coinbase, x -> "EURe".equals(x[7]));
            assertTrue(Ricevuti[4].startsWith("2024-01-15"), descrivi(Coinbase));
            assertCirca("11530", new BigDecimal(Ricevuti[5]));
            assertCirca("11530", new BigDecimal(Ricevuti[10]));
            for (List<String[]> L : Principale.Mappa_RW_ListeXGruppoWallet.values()) {
                for (String[] x : L) {
                    String Avvisi = x.length > 15 && x[15] != null ? x[15] : "";
                    assertFalse(Avvisi.contains("Errore"), descrivi(L) + " " + Avvisi);
                }
            }
        } finally {
            Funzioni.ReteDisabilitataPerTest = false;
            DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        }
    }

    // ------------------------------------------------------------------
    // Chiusura, annullamento, Trasla Orario (fase 4)
    // ------------------------------------------------------------------

    /**
     * Celsius con 1 BTC (costo 5.000, credito 20.000 euro) e 15.000 euro rimasti sul sotto-wallet Main: credito
     * totale 35.000 euro, pesi 4/7 e 3/7.
     */
    private static FallimentiPiattaforme.Procedura proceduraConEuro() {
        depositoSu("Celsius", "20201201100000", "2020-12-01 10:00", "EUR", "FIAT", "20000", "20000");
        acquistoSu("2021-01-01 10:00", "Main", "BTC", "1", "5000");
        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        assertEquals(1, Pos.size());
        Pos.forEach(p -> p.ValoreEUR = new BigDecimal("20000"));
        FallimentiPiattaforme.Procedura P = new FallimentiPiattaforme.Procedura();
        P.Id = "CELSIUS";
        P.NomeBreve = "CELSIUS";
        P.Exchange = "Celsius";
        P.DataDomanda = "2022-07-13";
        P.CreditoUSD = new BigDecimal("38888.89");
        P.FiatEUR = new BigDecimal("15000");
        assertNull(Principale_Fallimenti.CreaProcedura(P, Pos));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        return P;
    }

    private static List<String> saldiNegativi() {
        return Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values());
    }

    @Test
    void chiusura_ilResiduoDelCreditoEIFiatEsconoSenzaValore() {
        FallimentiPiattaforme.Procedura P = proceduraConEuro();
        //Il blocco sposta anche gli euro nel sotto-wallet del credito: le quote dei rimborsi escono da lì
        assertEquals(List.of(), saldiNegativi());
        assertEquals("Aperta, residuo 100%", Principale_Fallimenti.Stato("CELSIUS"));

        //Tranche del 50% ricevuta in euro fuori dal programma: 17.500 euro, 10.000 al BTC e 7.500 agli euro
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("50"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "17500", "17500", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals(List.of(), saldiNegativi());
        assertCirca("7500", sommaPlusvalenze(v -> v[1].startsWith("2024")));
        FallimentiPiattaforme.Invalida();
        assertEquals("Aperta, residuo 50%", Principale_Fallimenti.Stato("CELSIUS"));

        //La chiusura va dopo l'ultimo movimento della procedura
        assertNotNull(Principale_Fallimenti.PreparaChiusura(P, "2024-01-15 10:00").Errore);
        Principale_Fallimenti.Chiusura C = Principale_Fallimenti.PreparaChiusura(P, "2025-06-30 12:00");
        assertNull(C.Errore);
        assertEquals(List.of("0.5 CELSIUS.CREDITO.BTC (costo residuo 2500.00 EUR non dedotto)", "7500 EUR (FIAT)"), C.Descrizione);
        assertNull(Principale_Fallimenti.ChiudiProcedura(P, "2025-06-30 12:00"));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Nessuna minusvalenza, valore zero protetto dai ricalcoli dei prezzi, piattaforma vuota
        assertCirca("0", sommaPlusvalenze(v -> v[1].startsWith("2025")));
        String[] Pwn = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.CAMPO18_CHIUSURA.equals(v[18])).findFirst().orElseThrow();
        assertTrue(Pwn[0].endsWith("_PC"), Pwn[0]);
        assertEquals("0.00", Pwn[15]);
        assertEquals("0.00 EUR", Pwn[14]);
        assertEquals("FALL-CELSIUS", Pwn[GruppoOperazione.CAMPO]);
        assertEquals(List.of(), saldiNegativi());
        java.util.Map<String, Moneta> Dopo = Principale_GiacenzeaData.SommaQuantitaAData(Principale.MappaCryptoWallet.values(),
                FunzioniDate.ConvertiDatainLong("2025-07-01"), "Celsius", "tutti");
        assertTrue(Dopo.values().stream().allMatch(m -> new BigDecimal(m.Qta).signum() == 0),
                () -> Dopo.values().stream().map(m -> m.Moneta + " " + m.Qta).toList().toString());
        FallimentiPiattaforme.Invalida();
        assertEquals("2025-06-30 12:00", FallimentiPiattaforme.DataChiusura("CELSIUS"));
        assertTrue(Principale_Fallimenti.Stato("CELSIUS").startsWith("Chiusa"));
        //Opzione B: a fine 2024 metà del credito, dopo la chiusura niente
        assertCirca("10000", valore("0.5", FINE_2024));
        assertCirca("0", valore("0.5", FunzioniDate.ConvertiDatainLongMinuto("2025-12-31 23:59")));

        //Dopo la chiusura un rimborso si prepara lo stesso (la chiusura si sposta), una seconda chiusura no
        Principale_Fallimenti.Tranche Tardiva = Principale_Fallimenti.PreparaTranche(P, "2025-09-01 10:00", BigDecimal.ONE,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "10", "10", null)));
        assertNull(Tardiva.Errore);
        assertTrue(Tardiva.Avviso.contains("chiusa"), Tardiva.Avviso);
        assertNotNull(Principale_Fallimenti.PreparaChiusura(P, "2025-12-01 12:00").Errore);

        //Annulla: prima la chiusura, poi la tranche (anche dalla tabella), poi non resta nulla da annullare
        assertEquals("la chiusura del 2025-06-30 12:00", Principale_Fallimenti.UltimaOperazione("CELSIUS").Descrizione);
        assertNull(Principale_Fallimenti.AnnullaUltimaOperazione("CELSIUS"));
        assertNull(FallimentiPiattaforme.DataChiusura("CELSIUS"));
        assertEquals("il rimborso del 2024-01-15 10:00", Principale_Fallimenti.UltimaOperazione("CELSIUS").Descrizione);
        assertNull(Principale_Fallimenti.AnnullaUltimaOperazione("CELSIUS"));
        assertEquals(0, FallimentiPiattaforme.LeggiProcedure().get("CELSIUS").Tranche.size());
        assertNull(Principale_Fallimenti.UltimaOperazione("CELSIUS"));
        assertNotNull(Principale_Fallimenti.AnnullaUltimaOperazione("CELSIUS"));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertCirca("0", sommaPlusvalenze(v -> true));
        assertEquals("Aperta, residuo 100%", Principale_Fallimenti.Stato("CELSIUS"));
        //Restano il blocco: giroconti di BTC ed euro e la riga FBL
        assertEquals(5, Principale_Fallimenti.MovimentiProcedura("CELSIUS").size());
    }

    @Test
    void chiusura_conUnSottoWalletFiatNegativo_laPiattaformaTornaAZero() {
        //Main +20.000 euro, Card −5.000 (acquisto importato senza deposito): il credito conta il netto, 15.000
        depositoSu("Celsius", "20201201100000", "2020-12-01 10:00", "EUR", "FIAT", "20000", "20000");
        acquistoSu("2021-01-01 10:00", "Card", "BTC", "1", "5000");
        List<Principale_Fallimenti.Posizione> Pos = Principale_Fallimenti.GiacenzeAllaData("Celsius", "2022-07-13");
        Pos.forEach(p -> p.ValoreEUR = new BigDecimal("20000"));
        FallimentiPiattaforme.Procedura P = new FallimentiPiattaforme.Procedura();
        P.Id = "CELSIUS";
        P.NomeBreve = "CELSIUS";
        P.Exchange = "Celsius";
        P.DataDomanda = "2022-07-13";
        P.FiatEUR = new BigDecimal("15000");
        assertNull(Principale_Fallimenti.CreaProcedura(P, Pos));
        String[] Giroconto = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[4].equals(Principale_Fallimenti.SOTTOWALLET) && v[11].equals("EUR")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("15000").compareTo(new BigDecimal(Giroconto[13])), "si sposta il netto, non i 20.000 di Main");

        Principale_Fallimenti.Chiusura C = Principale_Fallimenti.PreparaChiusura(P, "2025-06-30 12:00");
        assertEquals(List.of("1 CELSIUS.CREDITO.BTC", "15000 EUR (FIAT)"), C.Descrizione);
        assertNull(Principale_Fallimenti.ChiudiProcedura(P, "2025-06-30 12:00"));
        //Il sotto-wallet del credito non va mai in negativo e la piattaforma torna a zero (Main +5.000, Card −5.000)
        assertTrue(saldiNegativi().stream().noneMatch(s -> s.contains(Principale_Fallimenti.SOTTOWALLET)), saldiNegativi().toString());
        java.util.Map<String, Moneta> Dopo = Principale_GiacenzeaData.SommaQuantitaAData(Principale.MappaCryptoWallet.values(),
                FunzioniDate.ConvertiDatainLong("2025-07-01"), "Celsius", "tutti");
        assertTrue(Dopo.values().stream().allMatch(m -> new BigDecimal(m.Qta).signum() == 0),
                () -> Dopo.values().stream().map(m -> m.Moneta + " " + m.Qta).toList().toString());
    }

    @Test
    void annulla_trancheAbbinataAUnDeposito_ilDepositoTornaDaClassificare() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        String[] Dep = depositoSu("Coinbase", "20240115100500", "2024-01-15 10:05", "BTC", "Crypto", "0.27", "11530");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("57.65"), null,
                new BigDecimal("100"), List.of(ricevuta("BTC", "Crypto", "0.27", "11530", Dep[0]))));
        assertTrue(Dep[18].startsWith("DTW"), Dep[18]);
        assertNull(Principale_Fallimenti.AnnullaUltimaOperazione("CELSIUS"));
        assertEquals("", Dep[18]);
        assertEquals("", Dep[20]);
        assertTrue(Principale.MappaCryptoWallet.containsKey(Dep[0]), "il deposito importato resta");
        //Il deposito si può riusare per la stessa tranche
        assertEquals(1, Principale_Fallimenti.DepositiCandidati(P).size());
    }

    @Test
    void tranche_inOrdineDiDataEDopoIlBlocco() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2022-07-13 00:00", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("EUR", "FIAT", "10", "10", null))).Errore, "nello stesso istante del blocco");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", BigDecimal.TEN, null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "2000", "2000", null))));
        assertNotNull(Principale_Fallimenti.PreparaTranche(P, "2023-06-01 10:00", BigDecimal.TEN, new BigDecimal("100"),
                List.of(ricevuta("EUR", "FIAT", "2000", "2000", null))).Errore, "prima dell'ultima tranche");
    }

    @Test
    void traslaOrario_nonSpostaLeRigheDellaProcedura() {
        proceduraCreata("100");
        List<String> Righe = Principale_Fallimenti.MovimentiProcedura("CELSIUS");
        assertFalse(Righe.isEmpty());
        assertEquals(0, Principale_TraslaOrario.EseguiTraslazione(Righe, 3_600_000L));
        assertEquals(Righe, Principale_Fallimenti.MovimentiProcedura("CELSIUS"));
    }

    // ------------------------------------------------------------------
    // Rimborso oltre il credito (FRT) e procedura chiusa
    // ------------------------------------------------------------------

    /** L'acquisto sintetico di {@link #proceduraCreata} paga 5.000 euro che sul sotto-wallet Earn non sono mai entrati. */
    private static final List<String> SOLO_ACQUISTO_SENZA_DEPOSITO = List.of("Celsius;Earn;EUR");

    /** Procedura Celsius (1 BTC, costo 5.000) con il credito esaurito da una tranche del 60% su atteso 60%. */
    private static FallimentiPiattaforme.Procedura creditoEsaurito() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("60");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("60"), null,
                new BigDecimal("60"), List.of(ricevuta("EUR", "FIAT", "12000", "12000", null))));
        FallimentiPiattaforme.Invalida();
        assertEquals("Credito esaurito", Principale_Fallimenti.Stato("CELSIUS"));
        return P;
    }

    @Test
    void oltreIlCredito_criptoSuUnAltroExchange_tuttoPlusvalenza() {
        FallimentiPiattaforme.Procedura P = creditoEsaurito();
        String[] Dep = depositoSu("Coinbase", "20250915100500", "2025-09-15 10:05", "BTC", "Crypto", "0.01", "900");
        List<Principale_Fallimenti.Ricevuta> R = List.of(ricevuta("BTC", "Crypto", "0.01", "900", Dep[0]));

        Principale_Fallimenti.Tranche T = Principale_Fallimenti.PreparaTranche(P, "2025-09-15 10:00", new BigDecimal("3"),
                new BigDecimal("60"), R);
        assertNull(T.Errore);
        assertEquals(0, T.Frazione.signum());
        assertEquals(1, T.Anteprima.size());
        assertEquals(0, T.Anteprima.get(0).Costo.signum());
        assertTrue(T.Avviso.contains("esaurito"), T.Avviso);
        //Nessun token credito: un deposito FRT e l'uscita verso Coinbase
        assertTrue(T.Righe.stream().noneMatch(v -> FallimentiPiattaforme.isTokenCredito(v[8]) || FallimentiPiattaforme.isTokenCredito(v[11])));

        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-09-15 10:00", new BigDecimal("3"), null, new BigDecimal("60"), R));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        String[] Frt = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.RimborsoOltreIlCredito(v[18])).findFirst().orElseThrow();
        assertTrue(Frt[0].endsWith("_DC"), Frt[0]);
        assertEquals(FallimentiPiattaforme.CAMPO5_RIMBORSO_OLTRE_IL_CREDITO, Frt[5]);
        assertEquals("FALL-CELSIUS", Frt[GruppoOperazione.CAMPO]);
        assertCirca("900", new BigDecimal(Frt[17]));
        assertEquals("S", Frt[33]);
        assertCirca("900", sommaPlusvalenze(v -> v[1].startsWith("2025-09")));
        assertTrue(Funzioni.MovimentoRilevante(Frt));
        assertTrue(Principale_LifoMancante.CausaleDepositoRiconosciuta("DC", Frt[18]));
        //I BTC arrivano su Coinbase con costo 900, il credito resta a zero, nessuna giacenza negativa
        assertTrue(Dep[18].startsWith("DTW"), Dep[18]);
        assertEquals("900.00", Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(FunzioniDate.ConvertiDatainLong("2025-12-31"))
                .CostoDelleRimanenze("Tutti", Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "0.01"));
        assertEquals(SOLO_ACQUISTO_SENZA_DEPOSITO, saldiNegativi());
        FallimentiPiattaforme.Invalida();
        assertEquals("Credito esaurito, 1 rimborso oltre il credito", Principale_Fallimenti.Stato("CELSIUS"));
        assertEquals(2, FallimentiPiattaforme.TrancheValide(FallimentiPiattaforme.LeggiProcedure().get("CELSIUS")).size());

        //Annulla: il deposito importato torna da classificare
        assertEquals("il rimborso del 2025-09-15 10:00", Principale_Fallimenti.UltimaOperazione("CELSIUS").Descrizione);
        assertNull(Principale_Fallimenti.AnnullaUltimaOperazione("CELSIUS"));
        assertEquals("", Dep[18]);
        assertEquals("Credito esaurito", Principale_Fallimenti.Stato("CELSIUS"));
    }

    @Test
    void oltreIlCredito_euroFuoriDalProgramma_plusvalenzaAncheNelQuadroRT() throws Exception {
        FallimentiPiattaforme.Procedura P = creditoEsaurito();
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-09-15 10:00", new BigDecimal("3"), null,
                new BigDecimal("60"), List.of(ricevuta("EUR", "FIAT", "900", "900", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        String[] Frt = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.RimborsoOltreIlCredito(v[18])).findFirst().orElseThrow();
        assertTrue(Frt[0].endsWith("_DF"), Frt[0]);
        assertEquals("S", Frt[33]);
        assertCirca("900", new BigDecimal(Frt[19]));
        assertTrue(Funzioni.MovimentoRilevante(Frt));
        assertEquals(SOLO_ACQUISTO_SENZA_DEPOSITO, saldiNegativi());

        //Il quadro RT somma la plusvalenza dell'anno e la mette su una voce EUR a giacenza zero
        Download[] Finestra = new Download[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> Finestra[0] = new Download());
        try {
            Calcoli_RT.AnalisiPlus A = Calcoli_RT.CalcoliPlusvalenzeXAnno(Finestra[0]);
            assertNotNull(A);
            assertCirca("900", A.Get_TabellaPlusXAnno().get("2025")[3]);
            Calcoli_RT.PlusXMoneta Eur = A.MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get("2025").values().stream()
                    .map(m -> m.get("EUR")).filter(java.util.Objects::nonNull).findFirst().orElseThrow();
            assertCirca("900", new BigDecimal(Eur.PlusRealizzata));
            assertEquals("0", Eur.Mon.Qta);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> Finestra[0].dispose());
        }
    }

    @Test
    void oltreIlCredito_quadroRW_ilLottoRicevutoParteDalValore() throws Exception {
        Funzioni.ReteDisabilitataPerTest = true;
        try {
            for (String[] o : new String[][] {{"RW_Rilevanza", "C"}, {"RW_ChiudiRWsuTrasferimento", "NO"},
                    {"RW_StakingZero", "NO"}, {"RW_LiFoComplessivo", "NO"}, {"RW_LiFoSubMovimenti", "NO"},
                    {"RW_FiatInRW", "NO"}}) {
                DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
            }
            Principale.Mappa_EMoney.put("EURe", "2020-01-01");
            FallimentiPiattaforme.Procedura P = creditoEsaurito();
            String[] Dep = depositoSu("Coinbase", "20250915100500", "2025-09-15 10:05", "EURe", "Crypto", "900", "900");
            assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-09-15 10:00", new BigDecimal("3"), null,
                    new BigDecimal("60"), List.of(ricevuta("EURe", "Crypto", "900", "900", Dep[0]))));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            Calcoli_RW.AggiornaRWFR("2025");

            List<String> Righi = new java.util.ArrayList<>();
            String[] Ricevuti = null;
            for (List<String[]> L : Principale.Mappa_RW_ListeXGruppoWallet.values()) {
                for (String[] x : L) {
                    String Avvisi = x.length > 15 && x[15] != null ? x[15] : "";
                    assertFalse(Avvisi.contains("Errore"), descrivi(L) + " " + Avvisi);
                    if ("EURe".equals(x[7]) && x[4].startsWith("2025-09-15")) Ricevuti = x;
                }
                Righi.add(descrivi(L));
            }
            assertNotNull(Ricevuti, Righi.toString());
            assertCirca("900", new BigDecimal(Ricevuti[5]));
        } finally {
            Funzioni.ReteDisabilitataPerTest = false;
        }
    }

    @Test
    void proceduraChiusa_unRimborsoUsaIlResiduoESpostaLaChiusura() {
        FallimentiPiattaforme.Procedura P = proceduraCreata("100");
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("50"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "10000", "10000", null))));
        assertNull(Principale_Fallimenti.ChiudiProcedura(P, "2024-06-01 12:00"));
        FallimentiPiattaforme.Invalida();
        assertEquals("Chiusa il 2024-06-01 12:00", Principale_Fallimenti.Stato("CELSIUS"));

        //Il 10% arriva dopo la chiusura: usa il residuo con il suo costo (500), la chiusura si rifà dopo
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-01-10 10:00", new BigDecimal("10"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "2000", "2000", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertCirca("1500", sommaPlusvalenze(v -> v[1].startsWith("2025")));
        FallimentiPiattaforme.Invalida();
        assertEquals("Chiusa il 2025-01-10 10:01", Principale_Fallimenti.Stato("CELSIUS"));
        String[] Pwn = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.CAMPO18_CHIUSURA.equals(v[18])).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("0.4").compareTo(new BigDecimal(Pwn[10]).abs()), Pwn[10]);
        assertEquals(1, Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.CAMPO18_CHIUSURA.equals(v[18])).count(), "una sola chiusura");
        assertEquals(SOLO_ACQUISTO_SENZA_DEPOSITO, saldiNegativi());

        //Il resto arriva ancora più tardi e consuma tutto: non resta nulla da chiudere
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-06-10 10:00", new BigDecimal("40"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "8000", "8000", null))));
        FallimentiPiattaforme.Invalida();
        assertEquals("Credito esaurito", Principale_Fallimenti.Stato("CELSIUS"));
        assertTrue(Principale.MappaCryptoWallet.values().stream().noneMatch(v -> FallimentiPiattaforme.CAMPO18_CHIUSURA.equals(v[18])));
        assertEquals(SOLO_ACQUISTO_SENZA_DEPOSITO, saldiNegativi());
    }

    @Test
    void proceduraChiusaConEuro_ilRimborsoTardivoRiportaGliEuroERichiude() {
        //Credito 35.000: BTC 20.000 (4/7) ed euro 15.000 (3/7). Tranche del 50% in euro, chiusura con 0,5 token e 7.500 euro
        FallimentiPiattaforme.Procedura P = proceduraConEuro();
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("50"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "17500", "17500", null))));
        assertNull(Principale_Fallimenti.ChiudiProcedura(P, "2025-06-30 12:00"));
        FallimentiPiattaforme.Invalida();

        //Il 10% arriva dopo: 3.500 euro, 2.000 al BTC (costo 500) e 1.500 agli euro della piattaforma
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2025-09-01 10:00", BigDecimal.TEN, null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "3500", "3500", null))));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertCirca("1500", sommaPlusvalenze(v -> v[1].startsWith("2025-09")));
        FallimentiPiattaforme.Invalida();
        assertEquals("Chiusa il 2025-09-01 10:01", Principale_Fallimenti.Stato("CELSIUS"));
        assertEquals(List.of(), saldiNegativi());
        //Una sola chiusura: 0,4 token e i 6.000 euro rimasti
        List<String[]> Chiusura = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> FallimentiPiattaforme.CAMPO5_CHIUSURA.equals(v[5])).toList();
        assertEquals(2, Chiusura.size());
        assertTrue(Chiusura.stream().anyMatch(v -> new BigDecimal("0.4").compareTo(new BigDecimal(v[10]).abs()) == 0
                && FallimentiPiattaforme.CAMPO18_CHIUSURA.equals(v[18])));
        assertTrue(Chiusura.stream().anyMatch(v -> "EUR".equals(v[8]) && new BigDecimal("6000").compareTo(new BigDecimal(v[10]).abs()) == 0));
        java.util.Map<String, Moneta> Dopo = Principale_GiacenzeaData.SommaQuantitaAData(Principale.MappaCryptoWallet.values(),
                FunzioniDate.ConvertiDatainLong("2025-09-02"), "Celsius", "tutti");
        assertTrue(Dopo.values().stream().allMatch(m -> new BigDecimal(m.Qta).signum() == 0),
                () -> Dopo.values().stream().map(m -> m.Moneta + " " + m.Qta).toList().toString());
    }

    @Test
    void depositiCandidati_nonPropongonoLeRigheDiUnAltraProcedura() {
        //La tranche in euro di Celsius scrive un deposito FIAT sul sotto-wallet del credito, senza campo 18
        FallimentiPiattaforme.Procedura P = proceduraConEuro();
        assertNull(Principale_Fallimenti.RegistraTranche(P, "2024-01-15 10:00", new BigDecimal("50"), null,
                new BigDecimal("100"), List.of(ricevuta("EUR", "FIAT", "17500", "17500", null))));
        assertTrue(Principale.MappaCryptoWallet.values().stream().anyMatch(v -> v[0].endsWith("_DF")
                && "FALL-CELSIUS".equals(v[GruppoOperazione.CAMPO]) && v[18].isBlank()));
        String[] Kraken = depositoSu("Kraken", "20250218100500", "2025-02-18 10:05", "USD", "FIAT", "2163", "2075");

        FallimentiPiattaforme.Procedura Altra = new FallimentiPiattaforme.Procedura();
        Altra.Id = "FTX";
        Altra.Exchange = "FTX";
        Altra.DataDomanda = "2022-11-11";
        List<String[]> Candidati = Principale_Fallimenti.DepositiCandidati(Altra);
        assertEquals(List.of(Kraken[0]), Candidati.stream().map(v -> v[0]).toList());
    }

    @Test
    void procedura_scriviELeggi() {
        assertTrue(FallimentiPiattaforme.ScriviProcedura(pianoCelsius()));
        FallimentiPiattaforme.Procedura p = FallimentiPiattaforme.LeggiProcedure().get("CELSIUS");
        assertNotNull(p);
        assertEquals("2022-07-13", p.DataDomanda);
        assertEquals(0, new BigDecimal("21000").compareTo(p.CreditoUSD));
        assertEquals(0, new BigDecimal("100").compareTo(p.RecuperoAtteso));
        assertEquals(1, p.Tranche.size());
        assertEquals(0, new BigDecimal("12106.50").compareTo(p.Tranche.get(0).USDPagati));
    }
}
