package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 3 delle commissioni collegate ({@link Principale_CommissioniCollegate}): abbinamento delle
 * commissioni già in archivio, collegamento e scollegamento manuali, righe del dettaglio del movimento.
 */
class Principale_CommissioniCollegateTest {

    private static final String SECONDO = "20240315103000";
    private static final String SECONDO_DOPO = "20240315103005";

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID, String Wallet, String MonetaU, String QtaU,
            String MonetaE, String QtaE, String Hash, String Chiave) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-15 10:30";
        v[3] = Wallet;
        v[5] = "PROVA";
        v[8] = MonetaU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[13] = QtaE;
        v[15] = "1.00";
        v[22] = "A";
        v[24] = Hash;
        v[CommissioniCollegate.CAMPO] = Chiave;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static String[] scambio(String Secondo, String Prog, String Wallet, String Hash) {
        return movimento(Secondo + "_" + Wallet + "_" + Prog + "_1_SC", Wallet, "BTC", "-1", "ETH", "10", Hash, "");
    }

    private static String[] fee(String Secondo, String Prog, String Wallet, String Hash) {
        return movimento(Secondo + "_" + Wallet + "C_" + Prog + "_1_CM", Wallet, "BNB", "-0.01", "", "", Hash, "");
    }

    private static String K(String[] v) {
        return CommissioniCollegate.Chiave(v);
    }

    // =============================================================================================
    // ABBINAMENTO DELL'ARCHIVIO
    // =============================================================================================

    @Test
    void stessoHash_collegaLaCommissioneATuttiIMovimentiDellaTransazione() {
        String A[] = scambio(SECONDO, "001", "Wallet", "0xabc");
        String B[] = movimento(SECONDO + "_Wallet_002_1_DC", "Wallet", "", "", "USDC", "5", "0xabc", "");
        String F[] = fee(SECONDO, "001", "Wallet", "0xabc");

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(1, E.Collegate);
        assertFalse(K(F).isEmpty());
        assertEquals(K(F), K(A));
        assertEquals(K(F), K(B));
    }

    @Test
    void unSoloMovimentoNelloStessoSecondo_vieneCollegato() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        scambio(SECONDO_DOPO, "001", "Binance", ""); //un altro secondo: non conta

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(1, E.Collegate);
        assertEquals(K(A), K(F));
        assertFalse(K(F).isEmpty());
    }

    @Test
    void piuMovimentiNelloStessoSecondoSenzaHash_restaNonCollegata() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String B[] = scambio(SECONDO, "002", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(0, E.Collegate);
        assertEquals(1, E.Ambigue);
        assertEquals("", K(F));
        assertEquals("", K(A));
        assertEquals("", K(B));
    }

    @Test
    void movimentoSuUnAltroWallet_nonECandidato() {
        scambio(SECONDO, "001", "Kraken", "");
        String F[] = fee(SECONDO, "001", "Binance", "");

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(1, E.SenzaMovimento);
        assertEquals("", K(F));
    }

    @Test
    void hashRipetutoSuTroppiMovimenti_nonIdentificaUnaTransazione() {
        for (int i = 1; i <= Principale_CommissioniCollegate.MAX_CANDIDATI_HASH + 1; i++) {
            scambio(SECONDO, String.format("%03d", i), "Wallet", "stesso");
        }
        String F[] = fee(SECONDO, "001", "Wallet", "stesso");

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(1, E.Ambigue);
        assertEquals("", K(F));
    }

    @Test
    void commissioneGeneratadallaClassificazione_nonVieneToccata() {
        scambio(SECONDO, "001", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        F[22] = "AU";

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(0, E.Collegate);
        assertEquals("", K(F));
    }

    @Test
    void secondaPassata_nonCambiaNulla() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        Principale_CommissioniCollegate.AbbinaArchivio();
        String Chiave = K(F);

        Principale_CommissioniCollegate.Esito E = Principale_CommissioniCollegate.AbbinaArchivio();

        assertEquals(0, E.Collegate);
        assertEquals(1, E.GiaCollegate);
        assertEquals(Chiave, K(F));
        assertEquals(Chiave, K(A));
    }

    // =============================================================================================
    // COLLEGAMENTO E SCOLLEGAMENTO MANUALI
    // =============================================================================================

    @Test
    void collegabile_soloConAlmenoUnaCommissioneEUnMovimento() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String B[] = scambio(SECONDO, "002", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        assertTrue(Principale_CommissioniCollegate.isCollegabile(List.of(A[0], F[0])));
        assertFalse(Principale_CommissioniCollegate.isCollegabile(List.of(A[0], B[0])));
        assertFalse(Principale_CommissioniCollegate.isCollegabile(List.of(F[0])));
        F[22] = "AU";
        assertFalse(Principale_CommissioniCollegate.isCollegabile(List.of(A[0], F[0])));
    }

    @Test
    void scollegandoLaCommissioneDiUnGruppoUnoAUno_ancheIlMovimentoPerdeLaChiave() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        A[CommissioniCollegate.CAMPO] = "K1";
        F[CommissioniCollegate.CAMPO] = "K1";

        Principale_CommissioniCollegate.Scollega(List.of(F[0]));

        assertEquals("", K(F));
        assertEquals("", K(A), "un gruppo di soli movimenti non collega piu' niente");
    }

    @Test
    void scollegandoUnoDiDueMovimenti_ilGruppoRestaSugliAltri() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String B[] = scambio(SECONDO, "002", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        for (String[] v : List.of(A, B, F)) v[CommissioniCollegate.CAMPO] = "K1";

        Principale_CommissioniCollegate.Scollega(List.of(A[0]));

        assertEquals("", K(A));
        assertEquals("K1", K(B));
        assertEquals("K1", K(F));
        assertTrue(Principale_CommissioniCollegate.isScollegabile(List.of(B[0])));
        assertFalse(Principale_CommissioniCollegate.isScollegabile(List.of(A[0])));
    }

    // =============================================================================================
    // DETTAGLIO DEL MOVIMENTO
    // =============================================================================================

    @Test
    void dettaglio_mostraLeCommissioniDelMovimentoEIlMovimentoDellaCommissione() {
        String A[] = scambio(SECONDO, "001", "Binance", "");
        String F[] = fee(SECONDO, "001", "Binance", "");
        A[CommissioniCollegate.CAMPO] = "K1";
        F[CommissioniCollegate.CAMPO] = "K1";

        List<String[]> DelMovimento = Principale_CommissioniCollegate.RigheDettaglio(A[0]);
        assertEquals("Commissioni collegate", DelMovimento.get(0)[0]);
        assertTrue(DelMovimento.get(0)[1].contains(F[0]));

        List<String[]> DellaCommissione = Principale_CommissioniCollegate.RigheDettaglio(F[0]);
        assertEquals("Commissione del movimento", DellaCommissione.get(0)[0]);
        assertTrue(DellaCommissione.get(0)[1].contains(A[0]));

        List<String[]> Nessuna = new ArrayList<>(Principale_CommissioniCollegate.RigheDettaglio(
                scambio(SECONDO_DOPO, "001", "Binance", "")[0]));
        assertTrue(Nessuna.isEmpty(), "un movimento non collegato non aggiunge righe al dettaglio");
    }
}
