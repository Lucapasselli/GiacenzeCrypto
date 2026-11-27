package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Quadro RT e conto derivati (P2): le monete del sotto-wallet {@code Derivati} hanno una pila e una giacenza proprie (chiave interna col
 * suffisso {@link ContoDerivati#SUFFISSO_CHIAVE}), come le pile separate del motore. Stanno nella tabella per token ma non nei totali di
 * plusvalenza latente e rimanenze, e non nella tabella per gruppo. Costi, corrispettivi e plusvalenza realizzata non dipendono dalle pile.
 */
class RTContoDerivatiTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        Funzioni.ReteDisabilitataPerTest = true;
    }

    @AfterAll
    static void chiude() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        MappaCryptoWallet.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void archivio() throws Exception {
        MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("RT_EscludiAnnoCorrente", "NO");
        seedPrezzo("BTC", "2025-01-01 00:00", "60000");
    }

    private static void seedPrezzo(String simbolo, String data, String prezzo) throws Exception {
        long ts = FunzioniDate.ConvertiDatainLongMinuto(data);
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ts); ps.setString(2, "binance"); ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal(prezzo)); ps.executeUpdate();
        }
    }

    private static String[] movimento(String id, String data, String sottoWallet, String campo5, String monetaU, String qtaU,
            String monetaE, String qtaE, String valore, String derivato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "OKX";
        v[4] = sottoWallet;
        v[5] = campo5;
        v[8] = monetaU;
        v[9] = monetaU.isEmpty() ? "" : (monetaU.equals("EUR") ? "FIAT" : "Crypto");
        v[10] = qtaU;
        v[11] = monetaE;
        v[12] = monetaE.isEmpty() ? "" : (monetaE.equals("EUR") ? "FIAT" : "Crypto");
        v[13] = qtaE;
        v[15] = valore;
        v[22] = "A";
        v[Derivati.CAMPO] = derivato;
        v[29] = String.valueOf(FunzioniDate.ConvertiDatainLongMinuto(data));
        MappaCryptoWallet.put(id, v);
        return v;
    }

    /** 1 BTC comprato a 20.000, poi 0,4 BTC spostati sul conto derivati (valore 24.000): 0,6 sullo spot e 0,4 sul conto. */
    private static void scenario() {
        movimento("20240201100000_OKX_001_001_AC", "2024-02-01 10:00", "Principale", "ACQUISTO CRYPTO", "EUR", "-20000", "BTC", "1", "20000.00", "");
        movimento("20240301100000_OKX_901_001_TI", "2024-03-01 10:00", "Principale", "TRASFERIMENTO INTERNO", "BTC", "-0.4", "", "", "24000.00", Derivati.MARGINE);
        movimento("20240301100000_OKX_902_001_TI", "2024-03-01 10:00", OKXDerivati.SOTTOWALLET, "TRASFERIMENTO INTERNO", "", "", "BTC", "0.4", "24000.00", Derivati.MARGINE);
    }

    private static Calcoli_RT.AnalisiPlus calcolaRT() throws Exception {
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Download[] finestra = new Download[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> finestra[0] = new Download());
        try {
            Calcoli_RT.AnalisiPlus a = Calcoli_RT.CalcoliPlusvalenzeXAnno(finestra[0]);
            assertNotNull(a);
            return a;
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> finestra[0].dispose());
        }
    }

    private static boolean uguale(String atteso, BigDecimal v) {
        return new BigDecimal(atteso).compareTo(v.setScale(2, java.math.RoundingMode.HALF_UP)) == 0;
    }

    @Test
    void ilContoHaLaSuaPilaELaSuaGiacenza() throws Exception {
        scenario();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        Map<String, Calcoli_RT.PlusXMoneta> gruppo = a.MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get("2024").values().iterator().next();
        Calcoli_RT.PlusXMoneta spot = gruppo.get("BTC");
        Calcoli_RT.PlusXMoneta conto = gruppo.get("BTC" + ContoDerivati.SUFFISSO_CHIAVE);
        assertNotNull(spot);
        assertNotNull(conto, "il conto derivati ha una riga a parte");
        assertEquals(0, new BigDecimal("0.6").compareTo(new BigDecimal(spot.Mon.Qta)), "giacenza spot");
        assertEquals(0, new BigDecimal("0.4").compareTo(new BigDecimal(conto.Mon.Qta)), "giacenza del conto");
        assertEquals("BTC", conto.Mon.Moneta, "Mon.Moneta resta il simbolo vero, per prezzi e alias");
        //Lo spot ha il costo di 0,6 BTC (12.000) e latente 36.000 - 12.000, il conto e' entrato al valore (24.000) e vale 24.000
        assertTrue(uguale("24000.00", new BigDecimal(spot.PlusLatente)), "latente spot " + spot.PlusLatente);
        assertTrue(uguale("0.00", new BigDecimal(conto.PlusLatente)), "latente del conto " + conto.PlusLatente);
    }

    @Test
    void conTotaliSeparatiPerCriptoEDerivati() throws Exception {
        scenario();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        BigDecimal[] anno = a.Get_TabellaPlusXAnno().get("2024");
        assertNotNull(anno);
        assertTrue(uguale("36000.00", anno[5]), "rimanenze cripto = solo 0,6 BTC spot a 60.000: " + anno[5]);
        assertTrue(uguale("24000.00", anno[4]), "latente cripto = solo lo spot: " + anno[4]);
        //Il conto ha il suo totale: 0,4 BTC a 60.000 contro il valore di ingresso 24.000
        assertTrue(uguale("24000.00", anno[8]), "rimanenze derivati: " + anno[8]);
        assertTrue(uguale("0.00", anno[7]), "latente derivati: " + anno[7]);
    }

    @Test
    void laTabellaPerTokenMostraIlContoELaTabellaPerGruppoNo() throws Exception {
        scenario();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        List<Object[]> token = a.RitornaTabellaAnno("2024");
        assertTrue(token.stream().anyMatch(r -> "BTC".equals(r[1])));
        assertTrue(token.stream().anyMatch(r -> ("BTC" + ContoDerivati.SUFFISSO_CHIAVE).equals(r[1])));
        double rimanenzeGruppi = 0;
        for (Object[] r : a.RitornaTabellaGruppiAnno("2024")) {
            rimanenzeGruppi += (double) r[5];
        }
        assertEquals(36000.0, rimanenzeGruppi, 0.01, "il gruppo non conta il conto derivati");
    }

    @Test
    void ilRealizzatoNonDipendeDalleDuePile() throws Exception {
        scenario();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        BigDecimal[] anno = a.Get_TabellaPlusXAnno().get("2024");
        //Il giroconto fuori dallo spot e' una cessione c-sexies: 0,4 BTC a 24.000 contro il costo di 8.000
        assertTrue(uguale("16000.00", anno[3]), "plusvalenza realizzata " + anno[3]);
        assertTrue(uguale("24000.00", anno[2]), "corrispettivi " + anno[2]);
        assertTrue(uguale("8000.00", anno[1]), "costi " + anno[1]);
    }
}
