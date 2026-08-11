package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica che il documento di origine ({@code [41]}) sopravviva ai movimenti che
 * {@link GUI_ClassificazioneMovimento} <b>genera</b> a partire da altri movimenti.
 *
 * <p>È il caso più facile da rompere in silenzio: questi movimenti nascono da
 * {@code new String[ColonneTabella]} o da {@code creaMovimento} e <b>non passano</b> dal punto di
 * strozzatura {@link Importazioni#ScriviListaSuMappaCrypto}, che è dove tutti gli altri vengono timbrati.
 * Se l'ereditarietà salta, il campo resta semplicemente vuoto e nulla lo segnala.
 *
 * <p>I casi sono costruiti perché il calcolo del prezzo resti locale (controvalori già presenti sui
 * movimenti di partenza), come già fa {@link Principale_Movimenti_SeparaUnisciTest}: nessun test deve
 * dipendere da una ricerca prezzi online.
 */
class DocumentiFonteEreditarietaTest {

    private static final String DATA_ID = "20240315103000";
    private static final String DATA = "2024-03-15 10:30";
    private static final String TIMESTAMP = "1710495000000";

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /**
     * Costruisce un movimento grezzo e lo inserisce in mappa.
     * @param ID identificativo completo
     * @param Campo5 descrizione del tipo di transazione
     * @param MonetaU/QtaU moneta e quantità in uscita (vuote se assenti)
     * @param MonetaE/QtaE moneta e quantità in entrata (vuote se assenti)
     * @param Prezzo controvalore in euro
     * @param Documento contenuto del campo 41
     * @return il movimento inserito
     */
    private static String[] movimento(String ID, String Campo5, String MonetaU, String QtaU,
            String MonetaE, String QtaE, String Prezzo, String Documento) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = DATA;
        v[2] = "1 di 1";
        v[3] = "Wallet Test";
        v[5] = Campo5;
        v[8] = MonetaU;
        v[9] = MonetaU.isEmpty() ? "" : "Crypto";
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = MonetaE.isEmpty() ? "" : "Crypto";
        v[13] = QtaE;
        v[15] = Prezzo;
        v[22] = "M";
        v[29] = TIMESTAMP;
        v[32] = "SI";
        v[41] = Documento;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    // =============================================================================================
    // COMMISSIONE E REWARD — figli di un trasferimento fra wallet propri
    // =============================================================================================

    @Test
    void commissione_ereditaIlDocumentoDiOrigineDelPrelievo() {
        //Prelevo 1 BTC e ne arrivano 0.9: la differenza diventa un movimento di commissione
        movimento(DATA_ID + "_WalletTest_001_1_PC", "PRELIEVO CRYPTO", "BTC", "-1", "", "", "50000.00", "12");
        movimento(DATA_ID + "_WalletTest_002_1_DC", "DEPOSITO CRYPTO", "", "", "BTC", "0.9", "45000.00", "12");

        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(
                DATA_ID + "_WalletTest_001_1_PC", DATA_ID + "_WalletTest_002_1_DC");

        String Commissione[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_2_1_CM");
        assertNotNull(Commissione, "la differenza fra prelievo e deposito deve generare la commissione");
        assertEquals("12", Commissione[41],
                "la commissione descrive la stessa riga del prelievo, quindi ne eredita il documento");
    }

    @Test
    void reward_ereditaIlDocumentoDiOrigineDelDeposito() {
        //Prelevo 1 BTC e ne arrivano 1.1: la differenza in più diventa un reward
        movimento(DATA_ID + "_WalletTest_001_1_PC", "PRELIEVO CRYPTO", "BTC", "-1", "", "", "50000.00", "12");
        movimento(DATA_ID + "_WalletTest_002_1_DC", "DEPOSITO CRYPTO", "", "", "BTC", "1.1", "55000.00", "12");

        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(
                DATA_ID + "_WalletTest_001_1_PC", DATA_ID + "_WalletTest_002_1_DC");

        String Reward[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_0001_1_RW");
        assertNotNull(Reward, "l'eccedenza rispetto al prelievo deve generare il reward");
        assertEquals("12", Reward[41], "il reward nasce dal deposito e ne eredita il documento");
    }

    @Test
    void commissione_senzaDocumentoSulPadre_restaSenzaDocumento() {
        movimento(DATA_ID + "_WalletTest_001_1_PC", "PRELIEVO CRYPTO", "BTC", "-1", "", "", "50000.00", "");
        movimento(DATA_ID + "_WalletTest_002_1_DC", "DEPOSITO CRYPTO", "", "", "BTC", "0.9", "45000.00", "");

        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(
                DATA_ID + "_WalletTest_001_1_PC", DATA_ID + "_WalletTest_002_1_DC");

        String Commissione[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_2_1_CM");
        assertNotNull(Commissione);
        assertTrue(Funzioni.noData(Commissione[41]),
                "senza documento sul padre il figlio non deve inventarsene uno");
    }

    // =============================================================================================
    // SCAMBIO DIFFERITO — i tre movimenti generati da ConsolidaMovimentiDifferiti
    // =============================================================================================

    @Test
    void scambioDifferito_iTreMovimentiGeneratiEreditanoIlDocumento() {
        //Vendita differita: esce BTC dal prelievo, entrano EUR sul deposito. Il controvalore è già sui
        //movimenti, quindi il prezzo non va cercato online
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "12");
        Prelievo[25] = "BTC";
        Prelievo[26] = "BTC";
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "12");
        Deposito[12] = "FIAT";
        Deposito[27] = "EUR";
        Deposito[28] = "EUR";

        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(
                DATA_ID + "_WalletTest_001_1_PC", DATA_ID + "_WalletTest_002_1_DF");

        //Trasferimento, scambio, trasferimento: gli ID portano il progressivo 01/02/03 davanti al wallet.
        //Il deposito è FIAT, quindi il secondo trasferimento diventa PF e lo scambio VC (vendita)
        String MT1[] = MappaCryptoWallet.get(DATA_ID + "_01WalletTest_001_1_DC");
        String MT2[] = MappaCryptoWallet.get(DATA_ID + "_03WalletTest_001_1_PF");
        assertNotNull(MT1, "il primo trasferimento deve essere stato creato");
        assertNotNull(MT2, "il secondo trasferimento deve essere stato creato");
        assertEquals("12", MT1[41]);
        assertEquals("12", MT2[41]);

        String MS[] = MappaCryptoWallet.get(DATA_ID + "_02WalletTest_001_1_VC");
        assertNotNull(MS, "lo scambio deve essere stato creato");
        assertEquals("12", MS[41],
                "i tre movimenti generati risalgono agli stessi file dei due movimenti di partenza");
    }

    @Test
    void scambioDifferito_conDocumentoSoloSulPrelievo_loScambioLoEredita() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "12");
        Prelievo[25] = "BTC";
        Prelievo[26] = "BTC";
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "");
        Deposito[12] = "FIAT";
        Deposito[27] = "EUR";
        Deposito[28] = "EUR";

        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(
                DATA_ID + "_WalletTest_001_1_PC", DATA_ID + "_WalletTest_002_1_DF");

        assertEquals("12", MappaCryptoWallet.get(DATA_ID + "_02WalletTest_001_1_VC")[41],
                "se una sola gamba ha il documento, lo scambio prende quello");
    }
}
