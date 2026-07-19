package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test end-to-end di CARATTERIZZAZIONE di {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenze()}
 * su un database H2 temporaneo (Fase 2 della voce B3 di Documentazione/Analisi_Bug_Criticita.md).
 *
 * Ogni test popola Principale.MappaCryptoWallet con movimenti costruiti a mano,
 * esegue il ricalcolo completo e verifica i campi scritti dal motore:
 *   v[16] = vecchio costo di carico (dal LIFO)
 *   v[17] = nuovo costo di carico
 *   v[19] = plusvalenza
 *   v[33] = flag calcolo plusvalenza (S/N)
 *   v[38] = flag anomalia LIFO
 *
 * Come per la Fase 1, i test fotografano il comportamento ATTUALE, inclusi i bug
 * noti (qui la voce C1): se una correzione cambia deliberatamente un esito,
 * il test corrispondente va aggiornato nello stesso commit.
 *
 * Formato ID transazione (5 segmenti separati da "_", vedi IDTS[4] nel motore):
 *   "<data>_<sorgente>_<progressivo>_<seq>_<TIPO>"  con TIPO = VC/AC/RW/TI/CM/DC/PC...
 * Il primo segmento inizia con l'anno a 4 cifre (richiesto da Funzioni.CashbackComeFIAT)
 * e l'iterazione di MappaCryptoWallet (TreeMap sull'ID) segue quindi l'ordine cronologico.
 */
class CalcoliPlusvalenzeNewAggiornaPlusvalenzeTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
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
        // Opzioni di default per tutti gli scenari: ogni test può sovrascriverle.
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Staking", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_CashBack", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Airdrop", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Earn", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Reward", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
    }

    /**
     * Crea un movimento e lo registra in MappaCryptoWallet.
     *
     * @param data           "yyyy-MM-dd HH:mm" (determina anche l'ordine di elaborazione)
     * @param tipoID         quinto segmento dell'ID (VC, AC, RW, TI, CM, DC, PC...)
     * @param classificazione contenuto di v[18] (es. "DTW - Trasferimento", "" se non classificato)
     * @param descrizione    v[5] (usata da RewardRilevante/CashbackComeFIAT: STAKING, CASHBACK...)
     * @param monetaU/tipoU/qtaU  moneta uscita, suo tipo (Crypto/FIAT/NFT/"") e quantità
     * @param monetaE/tipoE/qtaE  moneta entrata, suo tipo e quantità
     * @param valore         v[15], controvalore in EUR della transazione
     */
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

    private static String[] acquisto(String data, String moneta, String qta, String valore) {
        return movimento(data, "AC", "", "ACQUISTO", "EUR", "FIAT", valore, moneta, "Crypto", qta, valore);
    }

    private static String[] vendita(String data, String moneta, String qta, String valore) {
        return movimento(data, "VC", "", "VENDITA", moneta, "Crypto", qta, "EUR", "FIAT", valore, valore);
    }

    // ------------------------------------------------------------------
    // Scenari base: acquisto e vendita
    // ------------------------------------------------------------------

    @Test
    void acquistoConFiat_nessunaPlusvalenza_caricoUgualeAlValore() {
        String[] acq = acquisto("2024-01-01 10:00", "BTC", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", acq[16]);      // nessun vecchio costo di carico
        assertEquals("1000", acq[17]);  // nuovo costo di carico = valore transazione
        assertEquals("0.00", acq[19]);  // nessuna plusvalenza
        assertEquals("N", acq[33]);
    }

    @Test
    void venditaTotale_plusvalenzaUgualeAValoreMenoCostoDiCarico() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "1", "1500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", ven[16]); // costo di carico recuperato dal LIFO
        assertEquals("", ven[17]);
        assertEquals("500.00", ven[19]);  // 1500 - 1000
        assertEquals("S", ven[33]);
        assertEquals("", ven[38]);        // nessuna anomalia LIFO
    }

    @Test
    void venditaParziale_consumaIlLottoPiuRecente_LIFO() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "BTC", "1", "2000");
        String[] ven = vendita("2024-03-01 10:00", "BTC", "1", "1800");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // LIFO: esce il lotto acquistato a 2000, non quello a 1000 -> minusvalenza
        assertEquals("2000.00", ven[16]);
        assertEquals("-200.00", ven[19]);
        assertEquals("S", ven[33]);
    }

    @Test
    void venditaOltreGiacenza_plusvalenzaConCostoParzialeEMovimentoMarcato() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "2", "3000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Coperto solo 1 BTC su 2: costo di carico parziale e anomalia segnalata
        assertEquals("1000.00", ven[16]);
        assertEquals("2000.00", ven[19]); // 3000 - 1000
        assertEquals("S", ven[33]);
        assertEquals("A", ven[38]);
    }

    // ------------------------------------------------------------------
    // Scambi crypto-crypto
    // ------------------------------------------------------------------

    @Test
    void scambioOmogeneoPost2023_neutrale_trasferisceIlCostoDiCarico() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] scambio = movimento("2024-02-01 10:00", "SC", "", "SCAMBIO",
                "BTC", "Crypto", "1", "ETH", "Crypto", "10", "1200");
        String[] ven = vendita("2024-06-01 10:00", "ETH", "10", "1600");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Lo scambio crypto->crypto post-2023 è fiscalmente neutro
        assertEquals("1000.00", scambio[16]);
        assertEquals("1000.00", scambio[17]); // il costo di carico si trasferisce su ETH
        assertEquals("0.00", scambio[19]);
        assertEquals("N", scambio[33]);
        // La vendita successiva di ETH usa il costo di carico ereditato da BTC
        assertEquals("1000.00", ven[16]);
        assertEquals("600.00", ven[19]); // 1600 - 1000
        assertEquals("S", ven[33]);
    }

    @Test
    void scambioPre2023_conOpzioneScambiRilevanti_generaPlusvalenza() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "SI");
        acquisto("2022-01-01 10:00", "BTC", "1", "1000");
        String[] scambio = movimento("2022-06-01 10:00", "SC", "", "SCAMBIO",
                "BTC", "Crypto", "1", "ETH", "Crypto", "10", "1200");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Pre-2023 con opzione attiva: lo scambio è realizzo, il carico di ETH è il valore
        assertEquals("1000.00", scambio[16]);
        assertEquals("1200", scambio[17]);
        assertEquals("200.00", scambio[19]); // 1200 - 1000
        assertEquals("S", scambio[33]);
    }

    // ------------------------------------------------------------------
    // Reward / staking
    // ------------------------------------------------------------------

    @Test
    void rewardStakingRilevante_plusvalenzaImmediataPariAlValore() {
        String[] rw = movimento("2024-01-05 10:00", "RW", "", "STAKING REWARDS",
                "", "", "", "SOL", "Crypto", "5", "100");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", rw[16]);
        assertEquals("100", rw[17]);  // carico = valore di mercato alla ricezione
        assertEquals("100", rw[19]);  // plusvalenza immediata (nota: valore grezzo, non riscalato)
        assertEquals("S", rw[33]);
    }

    @Test
    void rewardStakingNonRilevante_caricoZeroENessunaPlusvalenza() {
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Staking", "NO");
        String[] rw = movimento("2024-01-05 10:00", "RW", "", "STAKING REWARDS",
                "", "", "", "SOL", "Crypto", "5", "100");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", rw[16]);
        assertEquals("0.00", rw[17]); // entra nel LIFO a costo zero
        assertEquals("0.00", rw[19]);
        assertEquals("N", rw[33]);
    }

    // ------------------------------------------------------------------
    // Trasferimenti tra wallet
    // ------------------------------------------------------------------

    @Test
    void trasferimentoTraWallet_neutro_eNonToccaIlLifo() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] prelievo = movimento("2024-02-01 10:00", "PC", "PTW - Trasferimento tra Wallet", "PRELIEVO",
                "BTC", "Crypto", "1", "", "", "", "1100");
        String[] deposito = movimento("2024-02-01 10:05", "DC", "DTW - Trasferimento tra Wallet", "DEPOSITO",
                "", "", "", "BTC", "Crypto", "1", "1100");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "1", "1500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Entrambi i lati del trasferimento sono fiscalmente neutri
        assertEquals("0.00", prelievo[19]);
        assertEquals("N", prelievo[33]);
        assertEquals("0.00", deposito[19]);
        assertEquals("N", deposito[33]);
        // Il LIFO non è stato toccato: la vendita successiva usa il costo originale
        assertEquals("1000.00", ven[16]);
        assertEquals("500.00", ven[19]);
    }

    // ------------------------------------------------------------------
    // Movimenti non classificati
    // ------------------------------------------------------------------

    @Test
    void depositoNonClassificato_conOpzioneAttiva_entraNelLifoACostoZero() {
        String[] dep = movimento("2024-01-01 10:00", "DC", "", "DEPOSITO",
                "", "", "", "BTC", "Crypto", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.00", dep[17]);
        assertEquals("0.00", dep[19]);
        assertEquals("N", dep[33]);
    }

    @Test
    void depositoNonClassificato_conOpzioneDisattivata_vieneIgnorato() {
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "NO");
        String[] dep = movimento("2024-01-01 10:00", "DC", "", "DEPOSITO",
                "", "", "", "BTC", "Crypto", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", dep[17]); // non entra nel LIFO
        assertEquals("0.00", dep[19]);
        assertEquals("N", dep[33]);
    }

    @Test
    void prelievoNonClassificato_conOpzioneAttiva_trattatoComeCashOut() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] pre = movimento("2024-06-01 10:00", "PC", "", "PRELIEVO",
                "BTC", "Crypto", "1", "", "", "", "1500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", pre[16]);
        assertEquals("500.00", pre[19]); // 1500 - 1000: il prelievo NC realizza plusvalenza
        assertEquals("S", pre[33]);
    }

    // ------------------------------------------------------------------
    // Scenari derivati dai pattern del dataset reale (Fase 3, anonimizzati):
    // commissioni, cashback come FIAT, rimborsi cashback, NFT, rettifiche
    // PWN, donazioni ricevute, depositi DCZ, cashout PCO, token EMoney.
    // Wallet, monete, quantità e importi sono fittizi.
    // ------------------------------------------------------------------

    @Test
    void commissione_realizzaPlusvalenzaComeCashout() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] com = movimento("2024-03-01 10:00", "CM", "", "COMMISSIONI",
                "BTC", "Crypto", "0.1", "", "", "", "5");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // La commissione consuma il LIFO e realizza (qui una minusvalenza):
        // costo di carico di 0.1 BTC = 100, valore 5 -> -95
        assertEquals("100.00", com[16]);
        assertEquals("", com[17]);
        assertEquals("-95.00", com[19]);
        assertEquals("S", com[33]);
    }

    @Test
    void commissione_conOpzioneNoPlusvalenzeCommissioni_neutraMaConsumaIlLifo() {
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "SI");
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] com = movimento("2024-03-01 10:00", "CM", "", "COMMISSIONI",
                "BTC", "Crypto", "0.1", "", "", "", "5");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "0.9", "1800");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Con l'opzione attiva la commissione non genera plusvalenza...
        assertEquals("100.00", com[16]);
        assertEquals("0.00", com[19]);
        assertEquals("N", com[33]);
        // ...ma toglie comunque 0.1 BTC dal LIFO: la vendita trova solo 0.9 BTC a 900
        assertEquals("900.00", ven[16]);
        assertEquals("900.00", ven[19]); // 1800 - 900
        assertEquals("S", ven[33]);
    }

    @Test
    void cashbackComeFIAT_depositoNeutroConCaricoPariAlValore() {
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIATAnno", "2010");
        String[] cb = movimento("2024-01-10 10:00", "RW", "", "CASHBACK",
                "", "", "", "CRO", "Crypto", "100", "25");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Assimilato ai cashback fiat: nessuna plusvalenza, carico = valore
        assertEquals("", cb[16]);
        assertEquals("25", cb[17]);
        assertEquals("0.00", cb[19]);
        assertEquals("N", cb[33]);
    }

    @Test
    void cashbackComeFIAT_primaDellAnnoImpostato_restaRewardTassata() {
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIATAnno", "2025");
        String[] cb = movimento("2024-01-10 10:00", "RW", "", "CASHBACK",
                "", "", "", "CRO", "Crypto", "100", "25");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Il movimento è del 2024, prima dell'anno impostato: resta una reward
        // fiscalmente rilevante (PDD_CashBack=SI di default nei test)
        assertEquals("25", cb[17]);
        assertEquals("25", cb[19]);
        assertEquals("S", cb[33]);
    }

    @Test
    void rimborsoCashback_conCashbackComeFIAT_neutro() {
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIATAnno", "2010");
        movimento("2024-01-10 10:00", "RW", "", "CASHBACK",
                "", "", "", "CRO", "Crypto", "100", "25");
        String[] rimborso = movimento("2024-02-10 10:00", "RW", "", "RIMBORSO CASHBACK",
                "CRO", "Crypto", "40", "", "", "", "10");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Il rimborso restituisce il cashback senza realizzo: toglie dal LIFO
        // il carico proporzionale (25 * 40/100 = 10) ma la plusvalenza resta zero
        assertEquals("10.00", rimborso[16]);
        assertEquals("", rimborso[17]);
        assertEquals("0.00", rimborso[19]);
        assertEquals("N", rimborso[33]);
    }

    @Test
    void scambioEterogeneo_cryptoVersoNft_realizzaPlusvalenza() {
        acquisto("2024-01-01 10:00", "ETH", "10", "1000");
        String[] scambio = movimento("2024-02-01 10:00", "SC", "", "ACQUISTO NFT",
                "ETH", "Crypto", "10", "COOLNFT #1", "NFT", "1", "1500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // Crypto -> NFT non è omogeneo: realizzo anche post-2023
        assertEquals("1000.00", scambio[16]);
        assertEquals("1500", scambio[17]); // l'NFT entra nel LIFO al valore di scambio
        assertEquals("500.00", scambio[19]);
        assertEquals("S", scambio[33]);
    }

    @Test
    void rettificaGiacenzaPWN_riduceIlLifoSenzaPlusvalenza() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] rettifica = movimento("2024-03-01 10:00", "PC", "PWN - RETTIFICA GIACENZA", "RETTIFICA GIACENZA",
                "BTC", "Crypto", "0.4", "", "", "", "0");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "0.6", "1200");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // La rettifica toglie dal LIFO senza generare plusvalenza
        assertEquals("400.00", rettifica[16]);
        assertEquals("", rettifica[17]);
        assertEquals("0.00", rettifica[19]);
        assertEquals("N", rettifica[33]);
        // La vendita trova solo i 0.6 BTC residui (costo 600)
        assertEquals("600.00", ven[16]);
        assertEquals("600.00", ven[19]); // 1200 - 600
        assertEquals("", ven[38]);       // giacenza sufficiente, nessuna anomalia
    }

    @Test
    void donazioneRicevutaDDO_entraNelLifoAlCostoDiCaricoIndicato() {
        String[] don = movimento("2024-01-01 10:00", "DC", "DDO - Donazione", "DONAZIONE",
                "", "", "", "BTC", "Crypto", "1", "1000");
        don[17] = "300"; // costo di carico storico del donante, impostato a mano nell'app
        String[] ven = vendita("2024-06-01 10:00", "BTC", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        // La donazione è neutra e mantiene il costo di carico indicato in v[17]
        assertEquals("300", don[17]);
        assertEquals("0.00", don[19]);
        assertEquals("N", don[33]);
        // La vendita usa il costo di carico del donante
        assertEquals("300.00", ven[16]);
        assertEquals("700.00", ven[19]); // 1000 - 300
        assertEquals("S", ven[33]);
    }

    @Test
    void depositoACostoZeroDCZ_neutroECaricoZero() {
        String[] dep = movimento("2024-01-01 10:00", "DC", "DCZ - Deposito a costo zero (no plusvalenza)",
                "DEPOSITO CRYPTO (a costo zero)", "", "", "", "BTC", "Crypto", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.00", dep[17]); // entra nel LIFO a costo zero
        assertEquals("0.00", dep[19]);
        assertEquals("N", dep[33]);
    }

    @Test
    void cashoutClassificatoPCO_realizzaPlusvalenza() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] cashout = movimento("2024-06-01 10:00", "PC",
                "PCO - Cashout, acquisti con crypto etc.. (plusvalenza)", "CASHOUT o SIMILARI",
                "BTC", "Crypto", "1", "", "", "", "1500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", cashout[16]);
        assertEquals("", cashout[17]);
        assertEquals("500.00", cashout[19]); // 1500 - 1000
        assertEquals("S", cashout[33]);
    }

    @Test
    void tokenEMoney_scambioDaCryptoDiventaEterogeneoERealizza() {
        // Un token marcato EMoney (tabella EMONEY / Mappa_EMoney) da una certa data
        // rende lo scambio crypto->EMoney NON omogeneo anche post-2023
        Principale.Mappa_EMoney.put("USDX", "2023-06-01");
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] scambio = movimento("2024-02-01 10:00", "SC", "", "SCAMBIO",
                "BTC", "Crypto", "1", "USDX", "Crypto", "1200", "1200");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", scambio[16]);
        assertEquals("1200", scambio[17]);
        assertEquals("200.00", scambio[19]); // 1200 - 1000: realizzo
        assertEquals("S", scambio[33]);
    }

    // ------------------------------------------------------------------
    // Correzioni C1 e A3 (robustezza su DB nuovo e ID malformati)
    // ------------------------------------------------------------------

    @Test
    void databaseSenzaOpzioni_usaIDefaultENonCrasha_correzioneC1() throws Exception {
        // Correzione C1: su un database dove le opzioni non sono mai state
        // scritte il ricalcolo non deve più crashare con NPE ma usare i default
        // (PL_CosiderareMovimentiNC=SI, PDD_*=SI, CashBackComeFIAT=NO).
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "DELETE FROM OPZIONI")) {
            ps.executeUpdate();
        }
        String[] acq = acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        String[] ven = vendita("2024-06-01 10:00", "BTC", "1", "1500");
        String[] rw = movimento("2024-03-01 10:00", "RW", "", "STAKING REWARD",
                "", "", "", "ETH", "Crypto", "1", "100");

        assertDoesNotThrow(Calcoli_PlusvalenzeNew::AggiornaPlusvalenze);

        // Comportamento coerente con i default: vendita realizzata, reward rilevante
        assertEquals("1000", acq[17]);
        assertEquals("500.00", ven[19]);
        assertEquals("S", ven[33]);
        assertEquals("100", rw[19]); // PDD_Staking default SI -> reward tassata
        assertEquals("S", rw[33]);
    }

    @Test
    void idMalformato_nonCrashaEVieneSegnalatoConFlagM_correzioneA3() {
        // Correzione A3: un ID con meno di 5 segmenti non deve far crashare il
        // ricalcolo; il movimento viene segnalato con la lettera "M" nel campo 38
        // e la classificazione ricade sul campo 18 (qui vuoto -> nessun effetto).
        String[] malformato = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(malformato, "");
        malformato[0] = "IDCORTO";
        malformato[1] = "2024-02-01 10:00";
        malformato[3] = "TEST";
        malformato[11] = "BTC";
        malformato[12] = "Crypto";
        malformato[13] = "1";
        malformato[15] = "100";
        Principale.MappaCryptoWallet.put(malformato[0], malformato);
        String[] acq = acquisto("2024-01-01 10:00", "BTC", "1", "1000");

        assertDoesNotThrow(Calcoli_PlusvalenzeNew::AggiornaPlusvalenze);

        assertTrue(malformato[38].contains("M"),
                "atteso flag M nel campo 38, trovato: \"" + malformato[38] + "\"");
        // Gli altri movimenti vengono comunque elaborati normalmente
        assertEquals("1000", acq[17]);
    }
}
