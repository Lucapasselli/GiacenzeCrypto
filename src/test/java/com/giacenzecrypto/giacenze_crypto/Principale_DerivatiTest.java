package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * "Classifica come derivato" ({@link Principale_Derivati}): anteprima, classificazione e ritorno a "da classificare"
 * dei movimenti marcati come derivati, e l'effetto sul motore delle plusvalenze.
 *
 * <p>Dati inventati, nella forma che esce dall'import OKX di un perpetuo: un prelievo di margine con la sua
 * commissione collegata e un deposito alla chiusura, tutti marcati nel campo 44 e con il campo 18 vuoto.</p>
 */
class Principale_DerivatiTest {

    private static final String ID_ACQ = "20260101100000_OKX_001_001_AC";
    private static final String ID_PC = "20260110100000_OKX_001_001_PC";
    private static final String ID_CM = "20260110100000_OKX_C01_001_CM";
    private static final String ID_DC = "20260110103000_OKX_001_001_DC";
    private static final String ID_SPOT = "20260111100000_OKX_001_001_DC";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static String[] movimento(String id, String data, String campo5, String monetaU, String qtaU,
            String monetaE, String qtaE, String valore, String derivato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "OKX";
        v[4] = "Principale";
        v[5] = campo5;
        v[8] = monetaU;
        v[9] = monetaU.isEmpty() ? "" : (monetaU.equals("EUR") ? "FIAT" : "Crypto");
        v[10] = qtaU;
        v[11] = monetaE;
        v[12] = monetaE.isEmpty() ? "" : "Crypto";
        v[13] = qtaE;
        v[15] = valore;
        v[22] = "A";
        v[Derivati.CAMPO] = derivato;
        MappaCryptoWallet.put(id, v);
        return v;
    }

    private static final java.util.function.Supplier<java.util.Set<String>> ANNI_DEFINITIVE = Principale_Derivati.AnniConDefinitiva;

    @AfterEach
    void rimettiAnniConDefinitiva() {
        Principale_Derivati.AnniConDefinitiva = ANNI_DEFINITIVE;
        Principale_Derivati.ScartaInAttesa();
    }

    @BeforeEach
    void archivio() {
        //Nessuna definitiva archiviata, qualunque cosa ci sia nella cartella di lavoro
        Principale_Derivati.AnniConDefinitiva = java.util.Set::of;
        Principale_Derivati.ScartaInAttesa();
        MappaCryptoWallet.clear();
        MovimentiStorico.AzzeraBuffer();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        //20 USDC comprati a 18,40 euro (0,92 l'uno)
        movimento(ID_ACQ, "2026-01-01 10:00", "ACQUISTO CRYPTO", "EUR", "-18.40", "USDC", "20", "18.40", "");
        //Margine in uscita: 10 USDC, valore 9,40. Commissione collegata: 0,02 USDC, valore 0,02
        String[] pc = movimento(ID_PC, "2026-01-10 10:00", "PRELIEVO CRYPTO", "USDC", "-10", "", "", "9.40", Derivati.PNL);
        String[] cm = movimento(ID_CM, "2026-01-10 10:00", "COMMISSIONI", "USDC", "-0.02", "", "", "0.02", Derivati.COMMISSIONE);
        CommissioniCollegate.Collega(pc, cm);
        //Chiusura: rientrano 10,5 USDC, valore 9,87
        movimento(ID_DC, "2026-01-10 10:30", "DEPOSITO CRYPTO", "", "", "USDC", "10.5", "9.87", Derivati.PNL);
        //Un deposito qualunque, non marcato
        movimento(ID_SPOT, "2026-01-11 10:00", "DEPOSITO CRYPTO", "", "", "USDC", "3", "2.80", "");
    }

    @Test
    void lAnteprimaContaAccreditiAddebitiECommissioniCollegate() {
        //La commissione non e' selezionata: entra perche' e' collegata al prelievo
        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_PC, ID_DC, ID_SPOT));

        assertEquals(3, p.voci.size());
        assertEquals(1, p.conta(Principale_Derivati.Tipo.ACCREDITO));
        assertEquals(1, p.conta(Principale_Derivati.Tipo.ADDEBITO));
        assertEquals(1, p.conta(Principale_Derivati.Tipo.COMMISSIONE));
        //9,87 - 9,40 - 0,02 = 0,45
        assertEquals(0, p.risultato().compareTo(new BigDecimal("0.45")));
        assertEquals("non è marcato come derivato", p.scartati.get(ID_SPOT));
        //L'anteprima non tocca nulla
        assertEquals("", MappaCryptoWallet.get(ID_PC)[18]);
        String testo = Principale_Derivati.TestoAnteprima(p);
        assertTrue(testo.contains("Risultato dei derivati: 0.45 euro"), testo);
        assertFalse(testo.contains(";"), "nei testi per l'utente niente punto e virgola");
    }

    @Test
    void ilProfittoDiUnFillConIlSuoTokenRestaFuoriDalRisultatoDellAnteprima() {
        //La chiusura ha la sua riga di token (stesso exchange e fill): il suo reddito e' quello in LIFO della chiusura del token
        MappaCryptoWallet.get(ID_DC)[24] = "77";
        String[] token = movimento("20260110103000_OKX_903_001_PC", "2026-01-10 10:30", "PRELIEVO CRYPTO",
                "DERIV.BTC-USDT-SWAP.LONG", "-1", "", "", "0", Derivati.CONTRATTO);
        token[24] = "77";
        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_PC, ID_DC));

        assertEquals(1, p.conToken());
        //- 9,40 - 0,02, senza i 9,87 della chiusura
        assertEquals(0, p.risultato().compareTo(new BigDecimal("-9.42")));
        String testo = Principale_Derivati.TestoAnteprima(p);
        assertTrue(testo.contains("Movimenti di un contratto con il suo token (derivati OKX): 1."), testo);

        //Senza token il paragrafo non c'e'
        Principale_Derivati.Piano senza = Principale_Derivati.Pianifica(List.of(ID_PC));
        assertEquals(0, senza.conToken());
        assertFalse(Principale_Derivati.TestoAnteprima(senza).contains("token"));
    }

    @Test
    void laClassificazioneScriveIMarcatoriELoStoricoEIlMotoreSeparaIlRisultato() {
        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_PC, ID_DC));

        assertEquals(3, Principale_Derivati.Applica(p));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        String[] pc = MappaCryptoWallet.get(ID_PC), cm = MappaCryptoWallet.get(ID_CM), dc = MappaCryptoWallet.get(ID_DC);
        assertEquals(Derivati.CAMPO5_ADDEBITO, pc[5]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, pc[18]);
        assertEquals("COMMISSIONI", cm[5], "il campo 5 di una commissione non cambia");
        assertEquals(Derivati.CAMPO18_ADDEBITO, cm[18]);
        assertEquals(Derivati.CAMPO5_ACCREDITO, dc[5]);
        assertEquals(Derivati.CAMPO18_ACCREDITO, dc[18]);
        //Ogni modifica ha un lignaggio, cioe' una voce nello storico
        assertFalse(pc[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank());
        assertFalse(cm[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank());
        assertFalse(dc[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank());
        //Motore: risultato dei derivati -9,40, -0,02, +9,87. Plusvalenza cripto: 10 USDC a 9,20 venduti a 9,40
        assertEquals("-9.40", pc[Derivati.CAMPO_RISULTATO]);
        assertEquals("-0.02", cm[Derivati.CAMPO_RISULTATO]);
        assertEquals("9.87", dc[Derivati.CAMPO_RISULTATO]);
        assertEquals(0, new BigDecimal(pc[19]).compareTo(new BigDecimal("0.20")));
        assertEquals("9.87", dc[17], "il deposito entra al valore, non a costo zero");
        assertEquals("0.00", dc[19]);
    }

    @Test
    void senzaClassificazioneIlDepositoEntraACostoZeroEdEQuelloCheLaClassificazioneCorregge() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("0.00", MappaCryptoWallet.get(ID_DC)[17]);
        assertEquals("", MappaCryptoWallet.get(ID_DC)[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void riportareDaClassificareRimetteIlMovimentoComEraELaCommissioneLoSegue() {
        String campo5Prima = MappaCryptoWallet.get(ID_PC)[5];
        Principale_Derivati.Applica(Principale_Derivati.Pianifica(List.of(ID_PC, ID_DC)));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Si annulla selezionando il solo prelievo: la commissione collegata torna con lui, il deposito resta
        assertEquals(2, Principale_Derivati.Annulla(List.of(ID_PC)));
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        String[] pc = MappaCryptoWallet.get(ID_PC), cm = MappaCryptoWallet.get(ID_CM), dc = MappaCryptoWallet.get(ID_DC);
        assertEquals("", pc[18]);
        assertEquals(campo5Prima, pc[5]);
        assertEquals("", pc[Derivati.CAMPO_RISULTATO]);
        assertEquals("", cm[18]);
        assertEquals("", cm[Derivati.CAMPO_RISULTATO]);
        assertEquals(Derivati.PNL, Derivati.Tipo(pc), "il marcatore resta: dice che cosa e' il movimento");
        assertEquals(Derivati.CAMPO18_ACCREDITO, dc[18]);
    }

    @Test
    void unMovimentoGiaClassificatoNonSiRiclassificaEFiniSceFraIGiaClassificati() {
        Principale_Derivati.Applica(Principale_Derivati.Pianifica(List.of(ID_DC)));

        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_DC, ID_PC));

        assertEquals(List.of(ID_DC), p.giaClassificati);
        assertEquals(2, p.voci.size(), "prelievo e commissione");
    }

    @Test
    void nonSiClassificaCioCheHaUnAltraClassificazioneOEInUnGruppo() {
        MappaCryptoWallet.get(ID_DC)[18] = "DCZ - Deposito a costo zero";
        MappaCryptoWallet.get(ID_PC)[20] = ID_DC;

        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_DC, ID_PC, "inesistente"));

        assertTrue(p.voci.isEmpty());
        assertEquals("ha già un'altra classificazione", p.scartati.get(ID_DC));
        assertEquals("è collegato ad altri movimenti", p.scartati.get(ID_PC));
        assertEquals("movimento non trovato", p.scartati.get("inesistente"));
        assertEquals(0, Principale_Derivati.Applica(p));
    }

    /**
     * Decisione D5 (21/11/2026, lettura A): un Dual Investment e' una cripto-attivita' per scelta. Non si classifica come derivato
     * (un acquisto non abbinato diventerebbe un onere c-quater dell'intero importo) e non conta fra i derivati da segnalare nei quadri.
     */
    @Test
    void unDualInvestmentNonSiClassificaENonContaFraIDerivatiDaSegnalare() {
        MappaCryptoWallet.clear();
        String idDual = "20250301100000_Binance_001_001_PC";
        movimento(idDual, "2025-03-01 10:00", "PRELIEVO CRYPTO", "USDT", "-100", "", "", "92.00", Derivati.DUAL);

        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(idDual));

        assertTrue(p.voci.isEmpty());
        assertEquals("è un Dual Investment, che il programma tratta come cripto-attività", p.scartati.get(idDual));
        assertFalse(Derivati.DaSegnalare("2025"), "nell'anno del contratto");
        assertFalse(Derivati.DaSegnalare("2026"), "negli anni dopo");
        assertEquals("", Derivati.TestoAvvisoArchivio("2025"));

        //Un derivato vero non classificato invece si segnala, anche l'anno dopo
        movimento("20250302100000_Binance_001_001_DC", "2025-03-02 10:00", "DEPOSITO CRYPTO", "", "", "USDT", "3", "2.76", Derivati.PNL);
        assertTrue(Derivati.DaSegnalare("2025"));
        assertTrue(Derivati.DaSegnalare("2026"));
        assertEquals(1, Derivati.ContaNellAnno("2025"));
    }

    @Test
    void laVoceApplicataDopoUnCambioDellArchivioSiSalta() {
        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(ID_DC));
        MappaCryptoWallet.get(ID_DC)[18] = "DCZ - Deposito a costo zero";   //classificato da un'altra parte nel frattempo

        assertEquals(0, Principale_Derivati.Applica(p));
        assertEquals("DCZ - Deposito a costo zero", MappaCryptoWallet.get(ID_DC)[18]);
    }

    @Test
    void laVoceDelMenuEAttivaSoloPerIMovimentiMarcatiEStaFraQuelleCheModificano() {
        assertTrue(Principale_Derivati.VoceAttiva(MappaCryptoWallet.get(ID_PC)));
        assertFalse(Principale_Derivati.VoceAttiva(MappaCryptoWallet.get(ID_SPOT)));
        assertFalse(Principale_Derivati.VoceAttiva(null));
        assertTrue(Funzioni.POPUP_VOCI_MODIFICA.contains(Principale_Derivati.VOCE_MENU));
    }

    // ===== Classificazione all'importazione (17/11/2026) =====

    /** Una riga come la produce un importatore, non ancora in archivio. */
    private static String[] daImportare(String id, String monetaU, String qtaU, String monetaE, String qtaE, String derivato) {
        String[] v = movimento(id, "2026-02-01 10:00", "DA CLASSIFICARE", monetaU, qtaU, monetaE, qtaE, "4.60", derivato);
        MappaCryptoWallet.remove(id);
        return v;
    }

    @Test
    void inUnAnnoConUnaDefinitivaIlMovimentoNuovoRestaDaClassificareFinoAllaSceltaDellUtente() {
        //Righe riscaricate di un anno gia' dichiarato: restano da classificare, la commissione segue il suo movimento
        Principale_Derivati.AnniConDefinitiva = () -> java.util.Set.of("2026");
        String[] dc = daImportare("20260201100000_OKX_501_001_DC", "", "", "USDC", "5", Derivati.PNL);
        dc[43] = "K1";
        String[] cm = daImportare("20260201100000_OKX_503_001_CM", "USDC", "-0.5", "", "", Derivati.COMMISSIONE);
        cm[43] = "K1";
        String[] fuori = daImportare("20250201100000_OKX_504_001_DC", "", "", "USDC", "2", Derivati.PNL);
        fuori[1] = "2025-02-01 10:00";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(dc, cm, fuori)), false, 0);

        assertEquals("", MappaCryptoWallet.get(dc[0])[18]);
        assertEquals("", MappaCryptoWallet.get(cm[0])[18], "la commissione segue il suo movimento");
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(fuori[0])[18], "un anno senza definitiva non cambia");
        assertEquals(1, Principale_Derivati.InAttesa().size(), "la commissione non e' fra quelli in attesa: segue il movimento");
        assertEquals(java.util.Set.of("2026"), Principale_Derivati.AnniInAttesa());

        //L'utente sceglie di classificarli: nascono come gli altri, senza storico, e la commissione li segue
        assertEquals(2, Principale_Derivati.ClassificaInAttesa());
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(dc[0])[18]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, MappaCryptoWallet.get(cm[0])[18]);
        assertTrue(Funzioni.noData(MappaCryptoWallet.get(dc[0])[MovimentiStorico.CAMPO_LIGNAGGIO]));
        assertTrue(Principale_Derivati.InAttesa().isEmpty());
    }

    @Test
    void lasciatiDaClassificareRestanoComeIMovimentiVecchi() {
        Principale_Derivati.AnniConDefinitiva = () -> java.util.Set.of("2026");
        String[] dc = daImportare("20260201100000_OKX_501_001_DC", "", "", "USDC", "5", Derivati.PNL);
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(dc)), false, 0);
        assertEquals(1, Principale_Derivati.InAttesa().size());
        Principale_Derivati.ScartaInAttesa();
        assertTrue(Principale_Derivati.InAttesa().isEmpty());
        assertEquals("", MappaCryptoWallet.get(dc[0])[18]);
        //resta classificabile con la voce di menu
        assertEquals(1, Principale_Derivati.Pianifica(List.of(dc[0])).voci.size());
    }

    @Test
    void unMovimentoNuovoMarcatoNasceGiaClassificato() {
        String[] dc = daImportare("20260201100000_OKX_501_001_DC", "", "", "USDC", "5", Derivati.PNL);
        String[] pc = daImportare("20260201100000_OKX_502_001_PC", "USDC", "-5", "", "", Derivati.FUNDING);
        String[] cm = daImportare("20260201100000_OKX_503_001_CM", "USDC", "-0.5", "", "", Derivati.COMMISSIONE);
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(dc, pc, cm)), false, 0);
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(dc[0])[18]);
        assertEquals(Derivati.CAMPO5_ACCREDITO, MappaCryptoWallet.get(dc[0])[5]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, MappaCryptoWallet.get(pc[0])[18]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, MappaCryptoWallet.get(cm[0])[18]);
        assertEquals("DA CLASSIFICARE", MappaCryptoWallet.get(cm[0])[5], "il campo 5 di una commissione non cambia");
        //nessuna voce di storico: e' la forma con cui il movimento nasce
        assertTrue(Funzioni.noData(MappaCryptoWallet.get(dc[0])[MovimentiStorico.CAMPO_LIGNAGGIO]));
    }

    /** D13 (17/11/2026): il bonus nasce accredito e il suo recupero addebito. Dual Investment e movimenti non marcati no. */
    @Test
    void bonusERecuperoNasconoClassificatiIlDualInvestmentNo() {
        String[] bonus = daImportare("20260201100000_OKX_511_001_DC", "", "", "USDC", "5", Derivati.BONUS);
        String[] recupero = daImportare("20260201100000_OKX_514_001_PC", "USDC", "-3", "", "", Derivati.BONUS);
        String[] dual = daImportare("20260201100000_OKX_512_001_DC", "", "", "USDC", "5", Derivati.DUAL);
        String[] spot = daImportare("20260201100000_OKX_513_001_DC", "", "", "USDC", "5", "");
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(bonus, recupero, dual, spot)), false, 0);
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(bonus[0])[18]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, MappaCryptoWallet.get(recupero[0])[18]);
        assertEquals("", MappaCryptoWallet.get(dual[0])[18]);
        assertEquals("", MappaCryptoWallet.get(spot[0])[18]);
    }

    /** Un movimento gia' in archivio lasciato da classificare resta cosi' anche se lo scaricamento lo sovrascrive (anni dichiarati). */
    @Test
    void unaRigaCheSostituisceUnMovimentoNonClassificatoNonSiClassifica() {
        String[] vecchio = movimento("20260201100000_OKX_521_001_DC", "2026-02-01 10:00", "DEPOSITO CRYPTO", "", "", "USDC", "5", "4.60", Derivati.PNL);
        assertEquals("", vecchio[18]);
        String[] nuovo = daImportare(vecchio[0], "", "", "USDC", "5", Derivati.PNL);
        MappaCryptoWallet.put(vecchio[0], vecchio);
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(nuovo)), true, 0);
        assertSame(nuovo, MappaCryptoWallet.get(vecchio[0]));
        assertEquals("", MappaCryptoWallet.get(vecchio[0])[18]);
    }

    /** Se invece il movimento sostituito era classificato come derivato, la riga nuova lo resta. */
    @Test
    void unaRigaCheSostituisceUnMovimentoClassificatoRestaClassificata() {
        String[] vecchio = movimento("20260201100000_OKX_531_001_DC", "2026-02-01 10:00", Derivati.CAMPO5_ACCREDITO, "", "", "USDC", "5", "4.60", Derivati.PNL);
        vecchio[18] = Derivati.CAMPO18_ACCREDITO;
        String[] nuovo = daImportare(vecchio[0], "", "", "USDC", "5", Derivati.PNL);
        MappaCryptoWallet.put(vecchio[0], vecchio);
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(nuovo)), true, 0);
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(vecchio[0])[18]);
    }

    /** Una commissione collegata a un movimento che resta da classificare (un Dual Investment) resta da classificare con lui. */
    @Test
    void laCommissioneDiUnMovimentoNonClassificatoRestaDaClassificare() {
        String[] dual = daImportare("20260201100000_OKX_541_001_PC", "USDC", "-5", "", "", Derivati.DUAL);
        String[] fee = daImportare("20260201100000_OKX_542_001_CM", "USDC", "-0.1", "", "", Derivati.COMMISSIONE);
        dual[CommissioniCollegate.CAMPO] = "chiave-inventata-1";
        fee[CommissioniCollegate.CAMPO] = "chiave-inventata-1";
        //la commissione prima del suo movimento nella lista
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(fee, dual)), false, 0);
        assertEquals("", MappaCryptoWallet.get(dual[0])[18]);
        assertEquals("", MappaCryptoWallet.get(fee[0])[18]);
    }

    /**
     * La commissione importata da sola, il cui movimento è già in archivio da classificare (un Dual importato prima), resta da
     * classificare: prima si classificava perché il movimento non era nella stessa scrittura. Con il movimento classificato si classifica.
     */
    @Test
    void laCommissioneImportataDopoIlSuoMovimentoLoSegue() {
        String[] dual = daImportare("20260201100000_OKX_545_001_PC", "USDC", "-5", "", "", Derivati.DUAL);
        dual[CommissioniCollegate.CAMPO] = "chiave-inventata-5";
        dual[24] = "545";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(dual)), false, 0);
        String[] fee = daImportare("20260201100000_OKX_546_001_CM", "USDC", "-0.1", "", "", Derivati.COMMISSIONE);
        fee[CommissioniCollegate.CAMPO] = "chiave-inventata-5";
        fee[24] = "546";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(fee)), false, 0);
        assertEquals("", commissione("chiave-inventata-5")[18]);

        String[] pnl = daImportare("20260201100000_OKX_547_001_DC", "", "", "USDC", "5", Derivati.PNL);
        pnl[CommissioniCollegate.CAMPO] = "chiave-inventata-6";
        pnl[24] = "547";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(pnl)), false, 0);
        String[] fee2 = daImportare("20260201100000_OKX_548_001_CM", "USDC", "-0.2", "", "", Derivati.COMMISSIONE);
        fee2[CommissioniCollegate.CAMPO] = "chiave-inventata-6";
        fee2[24] = "548";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(fee2)), false, 0);
        assertEquals(Derivati.CAMPO18_ADDEBITO, commissione("chiave-inventata-6")[18]);
    }

    private static String[] commissione(String chiave) {
        return MappaCryptoWallet.values().stream()
                .filter(v -> chiave.equals(v[CommissioniCollegate.CAMPO]) && CommissioniCollegate.isCommissione(v)).findFirst().orElseThrow();
    }

    /** La commissione collegata a un profitto classificato si classifica anche se viene prima nella lista. */
    @Test
    void laCommissioneDiUnProfittoClassificatoSiClassifica() {
        String[] pnl = daImportare("20260201100000_OKX_551_001_DC", "", "", "USDC", "5", Derivati.PNL);
        String[] fee = daImportare("20260201100000_OKX_552_001_CM", "USDC", "-0.1", "", "", Derivati.COMMISSIONE);
        pnl[CommissioniCollegate.CAMPO] = "chiave-inventata-2";
        fee[CommissioniCollegate.CAMPO] = "chiave-inventata-2";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(fee, pnl)), false, 0);
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(pnl[0])[18]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, MappaCryptoWallet.get(fee[0])[18]);
    }

    /** Un movimento modificato a mano (lignaggio) non si classifica da solo, anche se passa dalla scrittura degli import. */
    @Test
    void unMovimentoConStoricoNonSiClassificaDaSolo() {
        String[] v = daImportare("20260201100000_OKX_561_001_DC", "", "", "USDC", "5", Derivati.PNL);
        v[MovimentiStorico.CAMPO_LIGNAGGIO] = "lignaggio-inventato";
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(v)), false, 0);
        assertEquals("", MappaCryptoWallet.get(v[0])[18]);
    }

    // ===== Derivati regolati in valuta FIAT (20/11/2026) =====

    /** Un deposito o prelievo FIAT ({@code DF}/{@code PF}) come lo scrive un importatore, marcato come derivato. */
    private static String[] fiat(String id, String moneta, String qta, String valore, String derivato) {
        boolean entra = !qta.startsWith("-");
        String[] v = movimento(id, "2026-01-15 10:00", entra ? "DEPOSITO FIAT" : "PRELIEVO FIAT", entra ? "" : moneta,
                entra ? "" : qta, entra ? moneta : "", entra ? qta : "", valore, derivato);
        if (entra) v[12] = "FIAT";
        else v[9] = "FIAT";
        return v;
    }

    private static String[] impronta(String[] v) {
        return new String[]{v[16], v[17], v[19], v[33], v[Derivati.CAMPO_RISULTATO]};
    }

    @Test
    void unDerivatoRegolatoInEuroOInDollariVaFraIDerivatiSenzaToccareLeCripto() {
        //Le cripto prima: i loro risultati non devono cambiare
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        java.util.Map<String, String[]> prima = new java.util.HashMap<>();
        for (String[] v : MappaCryptoWallet.values()) prima.put(v[0], impronta(v));

        //Profitto di 100 euro, perdita di 30 euro, commissione di 1,50 euro, profitto di 50 dollari che valgono 46 euro
        String[] profitto = fiat("20260115100000_ExFiat_601_001_DF", "EUR", "100", "100", Derivati.PNL);
        String[] perdita = fiat("20260115110000_ExFiat_602_001_PF", "EUR", "-30", "30", Derivati.PNL);
        String[] commissione = fiat("20260115110000_ExFiat_C02_001_CM", "EUR", "-1.5", "1.5", Derivati.COMMISSIONE);
        commissione[5] = "COMMISSIONI";
        String[] dollari = fiat("20260115120000_ExFiat_603_001_DF", "USD", "50", "46", Derivati.PNL);

        Principale_Derivati.Piano p = Principale_Derivati.Pianifica(List.of(profitto[0], perdita[0], commissione[0], dollari[0]));
        assertTrue(p.scartati.isEmpty(), p.scartati.toString());
        assertEquals(2, p.conta(Principale_Derivati.Tipo.ACCREDITO), "un deposito FIAT e' un accredito, mai una commissione");
        assertEquals(1, p.conta(Principale_Derivati.Tipo.ADDEBITO));
        assertEquals(1, p.conta(Principale_Derivati.Tipo.COMMISSIONE));
        //100 - 30 - 1,50 + 46 = 114,50
        assertEquals(0, p.risultato().compareTo(new BigDecimal("114.50")));
        assertEquals(4, Principale_Derivati.Applica(p));
        assertEquals(Derivati.CAMPO18_ACCREDITO, profitto[18]);
        assertEquals("DEPOSITO FIAT", profitto[5], "il campo 5 FIAT resta: lo legge la riconciliazione Binance");
        assertEquals(Derivati.CAMPO18_ADDEBITO, perdita[18]);
        assertEquals("PRELIEVO FIAT", perdita[5]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, commissione[18]);

        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals(0, new BigDecimal("100").compareTo(Derivati.Risultato(profitto)));
        assertEquals(0, new BigDecimal("-30").compareTo(Derivati.Risultato(perdita)));
        assertEquals(0, new BigDecimal("-1.5").compareTo(Derivati.Risultato(commissione)));
        assertEquals(0, new BigDecimal("46").compareTo(Derivati.Risultato(dollari)), "il valore in euro, non la quantita' in dollari");
        for (String[] v : List.of(profitto, perdita, commissione, dollari)) {
            assertEquals(0, BigDecimal.ZERO.compareTo(new BigDecimal(v[19].isBlank() ? "0" : v[19])), "nessuna plusvalenza cripto su " + v[0]);
        }
        for (java.util.Map.Entry<String, String[]> e : prima.entrySet()) {
            assertArrayEquals(e.getValue(), impronta(MappaCryptoWallet.get(e.getKey())), "il movimento " + e.getKey() + " ha cambiato risultato");
        }

        //Annullando tornano depositi e prelievi FIAT senza risultato
        assertEquals(4, Principale_Derivati.Annulla(List.of(profitto[0], perdita[0], commissione[0], dollari[0])));
        assertEquals("DEPOSITO FIAT", profitto[5]);
        assertEquals("PRELIEVO FIAT", perdita[5]);
        assertEquals("", profitto[18]);
        assertEquals("", profitto[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void unDepositoFiatNuovoMarcatoNasceClassificato() {
        String[] v = fiat("20260201100000_ExFiat_701_001_DF", "EUR", "12", "12", Derivati.PNL);
        MappaCryptoWallet.remove(v[0]);
        Importazioni.ScriviListaSuMappaCrypto(new java.util.ArrayList<>(List.<String[]>of(v)), false, 0);
        assertEquals(Derivati.CAMPO18_ACCREDITO, MappaCryptoWallet.get(v[0])[18]);
        assertEquals("DEPOSITO FIAT", MappaCryptoWallet.get(v[0])[5]);
    }
}
