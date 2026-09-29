package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collegamento informativo fra commissioni e movimenti ({@link CommissioniCollegate}, campo 43): scrittura
 * della chiave di gruppo, commissioni che resterebbero orfane a una cancellazione, e conservazione della
 * chiave nelle operazioni che ricostruiscono i movimenti (separazione, fusione in scambio, unione di
 * movimenti omogenei con la proposta di unire anche le commissioni).
 */
class CommissioniCollegateTest {

    private static final String DATA_ID = "20240315103000";
    private static final String TIMESTAMP = "1710495000000";

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
        MovimentiStorico.AzzeraBuffer();
    }

    private static String[] movimento(String ID, String Campo5,
            String MonetaU, String TipoU, String QtaU,
            String MonetaE, String TipoE, String QtaE, String Prezzo, String Chiave) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-15 10:30";
        v[2] = "1 di 1";
        v[3] = "Wallet Test";
        v[5] = Campo5;
        v[6] = (MonetaU + " -> " + MonetaE).trim();
        v[8] = MonetaU;
        v[9] = TipoU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = TipoE;
        v[13] = QtaE;
        v[15] = Prezzo;
        v[22] = "A";
        v[29] = TIMESTAMP;
        v[32] = "SI";
        v[40] = "|||Personalizzato";
        v[CommissioniCollegate.CAMPO] = Chiave;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static String[] reward(String Progressivo, String Qta, String Chiave) {
        return movimento(DATA_ID + "_WalletTest_" + Progressivo + "_001_RW", "STAKING REWARDS",
                "", "", "", "ETH", "Crypto", Qta, "10.00", Chiave);
    }

    private static String[] commissione(String Progressivo, String Qta, String Chiave) {
        return movimento(DATA_ID + "_WalletTestC_" + Progressivo + "_001_CM", "COMMISSIONI",
                "ETH", "Crypto", Qta, "", "", "", "1.00", Chiave);
    }

    // =============================================================================================
    // SCRITTURA DELLA CHIAVE
    // =============================================================================================

    @Test
    void collega_scriveLaStessaChiaveSuMovimentoECommissione() {
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00", "");
        String Fee[] = commissione("001", "-0.01", "");

        CommissioniCollegate.Collega(Scambio, Fee);

        assertFalse(CommissioniCollegate.Chiave(Scambio).isEmpty());
        assertEquals(CommissioniCollegate.Chiave(Scambio), CommissioniCollegate.Chiave(Fee));
    }

    @Test
    void collegaGruppo_diSoleCommissioni_nonScriveNulla() {
        //Transazione fallita: c'e' solo il gas, non c'e' un movimento a cui collegarlo
        String Fee[] = commissione("001", "-0.01", "");
        List<String[]> Righe = new ArrayList<>();
        Righe.add(Fee);
        CommissioniCollegate.CollegaGruppo(Righe);
        assertEquals("", CommissioniCollegate.Chiave(Fee));
    }

    @Test
    void collegaGruppo_unaCommissioneSuPiuMovimenti_liCollegaTutti() {
        //Il gas di una transazione DeFi che ha mosso due token
        String A[] = reward("001", "1", "");
        String B[] = reward("002", "2", "");
        String Fee[] = commissione("001", "-0.01", "");
        CommissioniCollegate.CollegaGruppo(new ArrayList<>(List.of(A, B, Fee)));
        String K = CommissioniCollegate.Chiave(Fee);
        assertFalse(K.isEmpty());
        assertEquals(K, CommissioniCollegate.Chiave(A));
        assertEquals(K, CommissioniCollegate.Chiave(B));
    }

    @Test
    void collegaGruppo_conPreferiti_collegaSoloQuelli() {
        String Reward[] = reward("001", "1", "");
        String Scambio[] = movimento(DATA_ID + "_WalletTest_002_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00", "");
        String Fee[] = commissione("001", "-0.01", "");
        List<String[]> Lista = new ArrayList<>(List.of(Reward, Scambio, Fee));

        List<String[]> Preferiti = new ArrayList<>();
        Preferiti.add(Scambio);
        CommissioniCollegate.CollegaGruppo(Lista, Preferiti);

        assertEquals(CommissioniCollegate.Chiave(Scambio), CommissioniCollegate.Chiave(Fee));
        assertEquals("", CommissioniCollegate.Chiave(Reward), "il reward dello stesso secondo non c'entra");
    }

    @Test
    void collega_suMovimentiConChiaviDiverse_fondeIGruppiSenzaLasciareOrfani() {
        //Due movimenti gia' collegati ciascuno alla propria commissione, piu' una commissione nuova da
        //attribuire a entrambi: la commissione vecchia del secondo deve seguire il suo movimento
        String A[] = reward("001", "1", "K1");
        String B[] = reward("002", "2", "K2");
        String FeeA[] = commissione("001", "-0.01", "K1");
        String FeeB[] = commissione("002", "-0.02", "K2");
        String Nuova[] = commissione("003", "-0.03", "");
        MappaCryptoWallet.clear(); //righe di un'importazione: non ancora in mappa
        List<String[]> Ambito = new ArrayList<>(List.of(A, B, FeeA, FeeB, Nuova));
        List<String[]> Nuove = new ArrayList<>();
        Nuove.add(Nuova);

        CommissioniCollegate.Collega(List.of(A, B), Nuove, Ambito);

        String K = CommissioniCollegate.Chiave(A);
        for (String[] v : Ambito) assertEquals(K, CommissioniCollegate.Chiave(v), v[0]);
    }

    // =============================================================================================
    // COMMISSIONI ORFANE ALLA CANCELLAZIONE
    // =============================================================================================

    @Test
    void cancellandoLUnicoMovimento_laCommissioneEOrfana() {
        String A[] = reward("001", "1", "K1");
        String Fee[] = commissione("001", "-0.01", "K1");
        assertEquals(Set.of(Fee[0]), CommissioniCollegate.CommissioniOrfane(List.of(A[0])));
    }

    @Test
    void commissioneCondivisaConUnMovimentoCheResta_nonEOrfana() {
        String A[] = reward("001", "1", "K1");
        reward("002", "2", "K1");
        commissione("001", "-0.01", "K1");
        assertTrue(CommissioniCollegate.CommissioniOrfane(List.of(A[0])).isEmpty());
    }

    @Test
    void cancellandoTuttiIMovimentiDelGruppo_laCommissioneEOrfana() {
        String A[] = reward("001", "1", "K1");
        String B[] = reward("002", "2", "K1");
        String Fee[] = commissione("001", "-0.01", "K1");
        assertEquals(Set.of(Fee[0]), CommissioniCollegate.CommissioniOrfane(List.of(A[0], B[0])));
    }

    @Test
    void unMovimentoGeneratoAutomaticamente_nonTieneInVitaLaCommissione() {
        //Lo scambio sintetico di uno scambio differito sparisce con la classificazione del prelievo
        String A[] = reward("001", "1", "K1");
        String Sintetico[] = reward("002", "2", "K1");
        Sintetico[22] = "AU";
        String Fee[] = commissione("001", "-0.01", "K1");
        assertEquals(Set.of(Fee[0]), CommissioniCollegate.CommissioniOrfane(List.of(A[0])));
    }

    @Test
    void cancellandoLaSolaCommissione_nonCiSonoOrfane() {
        reward("001", "1", "K1");
        String Fee[] = commissione("001", "-0.01", "K1");
        assertTrue(CommissioniCollegate.CommissioniOrfane(List.of(Fee[0])).isEmpty());
    }

    // =============================================================================================
    // CONSERVAZIONE NELLE OPERAZIONI SUI MOVIMENTI
    // =============================================================================================

    @Test
    void separazione_leDueGambeEreditanoLaChiave() {
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00", "K1");
        commissione("001", "-0.01", "K1");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        assertEquals("K1", CommissioniCollegate.Chiave(MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC")));
        assertEquals("K1", CommissioniCollegate.Chiave(MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC")));
    }

    @Test
    void fusioneInScambio_conChiaviDiverse_leCommissioniFinisconoNelloStessoGruppo() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00", "K1");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00", "K2");
        String Fee1[] = commissione("001", "-0.01", "K1");
        String Fee2[] = commissione("002", "-0.02", "K2");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Scambio[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC");
        assertEquals("K1", CommissioniCollegate.Chiave(Scambio), "lo scambio tiene la chiave del prelievo");
        assertEquals("K1", CommissioniCollegate.Chiave(Fee1));
        assertEquals("K1", CommissioniCollegate.Chiave(Fee2), "la commissione del deposito non resta orfana");
    }

    @Test
    void unioneOmogenei_proponeDiUnireLeCommissioniDiUnSoloMovimentoEFondeLeChiavi() {
        String A[] = reward("001", "1", "K1");
        String B[] = reward("002", "2", "K2");
        commissione("001", "-0.001", "K1");
        commissione("002", "-0.002", "K2");

        List<String[]> Gruppo = Principale_Movimenti_SeparaUnisci.TrovaGruppoOmogeneo(List.of(A[0], B[0]));
        assertNotNull(Gruppo);
        List<List<String[]>> DaUnire = Principale_Movimenti_SeparaUnisci.CommissioniUnificabili(Gruppo);
        assertEquals(1, DaUnire.size(), "una sola moneta: un solo gruppo di commissioni");
        assertEquals(2, DaUnire.get(0).size());

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiUnioneOmogenei(Gruppo, null));
        for (List<String[]> Commissioni : DaUnire) {
            assertTrue(Principale_Movimenti_SeparaUnisci.EseguiUnioneOmogenei(Commissioni, null));
        }

        assertEquals(2, MappaCryptoWallet.size(), "un movimento unito e una commissione unita");
        String K = null;
        for (String[] v : MappaCryptoWallet.values()) {
            if (K == null) K = CommissioniCollegate.Chiave(v);
            assertEquals(K, CommissioniCollegate.Chiave(v), "stesso gruppo per movimento e commissione");
            if (CommissioniCollegate.isCommissione(v)) {
                assertEquals(0, new BigDecimal("-0.003").compareTo(new BigDecimal(v[10])));
            }
        }
        assertFalse(K.isEmpty());
    }

    @Test
    void unioneOmogenei_senzaUnireLeCommissioni_leFondeComunqueNelloStessoGruppo() {
        String A[] = reward("001", "1", "K1");
        String B[] = reward("002", "2", "K2");
        String Fee1[] = commissione("001", "-0.001", "K1");
        String Fee2[] = commissione("002", "-0.002", "K2");

        List<String[]> Gruppo = Principale_Movimenti_SeparaUnisci.TrovaGruppoOmogeneo(List.of(A[0], B[0]));
        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiUnioneOmogenei(Gruppo, null));

        assertEquals(3, MappaCryptoWallet.size());
        assertEquals(CommissioniCollegate.Chiave(Fee1), CommissioniCollegate.Chiave(Fee2));
        assertFalse(CommissioniCollegate.Chiave(Fee1).isEmpty());
    }

    @Test
    void unioneOmogenei_unaCommissioneCondivisaConUnAltroMovimento_nonVieneProposta() {
        String A[] = reward("001", "1", "K1");
        String B[] = reward("002", "2", "K2");
        //Il gruppo K1 ha un secondo movimento non selezionato: la sua commissione non e' solo di A
        movimento(DATA_ID + "_WalletTest_003_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00", "K1");
        commissione("001", "-0.001", "K1");
        commissione("002", "-0.002", "K2");

        List<String[]> Gruppo = Principale_Movimenti_SeparaUnisci.TrovaGruppoOmogeneo(List.of(A[0], B[0]));
        assertNotNull(Gruppo);
        assertTrue(Principale_Movimenti_SeparaUnisci.CommissioniUnificabili(Gruppo).isEmpty(),
                "resta una sola commissione candidata: non c'e' niente da unire");
    }
}
