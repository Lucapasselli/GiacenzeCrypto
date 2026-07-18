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
    // Bug C1 (comportamento attuale documentato)
    // ------------------------------------------------------------------

    @Test
    void opzioneMancanteNelDatabase_attualmenteLanciaNPE_bugC1() throws Exception {
        // Comportamento attuale documentato dalla voce C1 dell'analisi:
        // su un database dove PL_CosiderareMovimentiNC non è mai stata scritta,
        // AggiornaPlusvalenze() crasha con NullPointerException ancora prima
        // di elaborare i movimenti. Quando C1 verrà corretta questo test DEVE
        // essere sostituito con la verifica del comportamento di default.
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "DELETE FROM OPZIONI WHERE Opzione = ?")) {
            ps.setString(1, "PL_CosiderareMovimentiNC");
            ps.executeUpdate();
        }

        assertThrows(NullPointerException.class, Calcoli_PlusvalenzeNew::AggiornaPlusvalenze);
    }
}
