package com.giacenzecrypto.giacenze_crypto;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Riga "Trasferimento da ... a ..." del dettaglio movimento ({@link GUI_DettaglioTransazione#WalletDelTrasferimento}):
 * la controparte va trovata anche quando {@code v[20]} elenca altri movimenti oltre a lei.
 */
class DettaglioWalletTrasferimentoTest {

    private Map<String, String[]> salvata;

    @BeforeEach
    void setUp() {
        salvata = new TreeMap<>(Principale.MappaCryptoWallet);
        Principale.MappaCryptoWallet.clear();
    }

    @AfterEach
    void tearDown() {
        Principale.MappaCryptoWallet.clear();
        Principale.MappaCryptoWallet.putAll(salvata);
    }

    private static String[] mov(String id, String wallet, String campo18, String monetaU, String monetaE, String collegati) {
        String v[] = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[3] = wallet;
        v[8] = monetaU;
        v[11] = monetaE;
        v[18] = campo18;
        v[20] = collegati;
        Principale.MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void conLaCommissioneDiTrasferimentoTraICollegatiLaControparteSiTrovaLoStesso() {
        String ptw[] = mov("20210119153349_W_001_001_PC", "Wallet BSC", "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)",
                "BNB", "", "20210119153559_Binance_001_001_DC,20210119153349_W_2_1_CM");
        String dtw[] = mov("20210119153559_Binance_001_001_DC", "Binance", "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)",
                "", "BNB", "20210119153349_W_001_001_PC,20210119153349_W_2_1_CM");
        mov("20210119153349_W_2_1_CM", "Wallet BSC", "", "BNB", "", "");
        assertArrayEquals(new String[]{"Wallet BSC", "Binance"}, GUI_DettaglioTransazione.WalletDelTrasferimento(ptw));
        assertArrayEquals(new String[]{"Wallet BSC", "Binance"}, GUI_DettaglioTransazione.WalletDelTrasferimento(dtw));
    }

    @Test
    void inUnoScambioDifferitoLaControparteELaGambaConLaStessaMoneta() {
        String ptwA[] = mov("1_W_001_001_PC", "Wallet", "PTW - Scambio Differito", "USDT", "", "1_P_001_001_DC,2_P_001_001_SC,3_P_001_001_PC,3_W_001_001_DC");
        mov("1_P_001_001_DC", "Piattaforma", "DTW - Scambio Differito", "", "USDT", "1_W_001_001_PC");
        mov("3_P_001_001_PC", "Piattaforma", "PTW - Scambio Differito", "BTC", "", "3_W_001_001_DC");
        String dtwB[] = mov("3_W_001_001_DC", "Wallet", "DTW - Scambio Differito", "", "BTC", "1_W_001_001_PC,1_P_001_001_DC,2_P_001_001_SC,3_P_001_001_PC");
        assertArrayEquals(new String[]{"Wallet", "Piattaforma"}, GUI_DettaglioTransazione.WalletDelTrasferimento(ptwA));
        assertArrayEquals(new String[]{"Piattaforma", "Wallet"}, GUI_DettaglioTransazione.WalletDelTrasferimento(dtwB));
    }

    @Test
    void unIdCollegatoVuotoOMancanteNonRompeNulla() {
        String ptw[] = mov("1_W_001_001_PC", "Wallet", "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", "ETH", "", "2_X_001_001_DC,,inesistente");
        mov("2_X_001_001_DC", "Exchange", "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", "", "ETH", "1_W_001_001_PC");
        assertArrayEquals(new String[]{"Wallet", "Exchange"}, GUI_DettaglioTransazione.WalletDelTrasferimento(ptw));
    }
}
