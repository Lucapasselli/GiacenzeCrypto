package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Taglio dei lotti LIFO del motore RW ({@link Calcoli_RW#AggiornaRWFR}) ai confini dei periodi CRYPTO, metodi
 * B/C/D: chiusura alle 23:59 del giorno prima e riapertura alle 00:00 del confine, allo stesso prezzo
 * (decisione 3 di {@code Analisi_QuadroRW_Crypto_Periodi.md} §6). La moneta è l'E-Money "EURe", valorizzata
 * 1:1 in euro: prezzi deterministici senza rete.
 */
class Calcoli_RW_PeriodiMotoreTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        Funzioni.ConnInternetAttiva = false;
        Funzioni.TimesTampUltimoControlloInternet = System.currentTimeMillis();
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_RW_ListeXGruppoWallet.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        Principale.MappaCryptoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM WALLETGRUPPO");
            st.execute("DELETE FROM GRUPPO_PERIODO_RW");
        }
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_EMoney.put("EURe", "2020-01-01");
        for (String[] o : new String[][] {{"RW_Rilevanza", "D"}, {"RW_ChiudiRWsuTrasferimento", "NO"},
                {"RW_StakingZero", "NO"}, {"RW_LiFoComplessivo", "NO"}, {"RW_LiFoSubMovimenti", "NO"},
                {"RW_FiatInRW", "NO"}}) {
            DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
        }
        progressivo = 0;
    }

    /** Acquisto di EURe con euro (categoria AC): apre un lotto al valore del movimento. */
    private static void acquisto(String data, String qta) {
        String prefisso = data.replace("-", "").replace(":", "").replace(" ", "");
        String id = prefisso + "_TEST_" + String.format("%03d", ++progressivo) + "_001_AC";
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "Kraken";
        m[8] = "EUR";  m[9] = "FIAT";  m[10] = "-" + qta;
        m[11] = "EURe"; m[12] = "Crypto"; m[13] = qta;
        m[15] = qta;
        Principale.MappaCryptoWallet.put(id, m);
    }

    /** Taglio al 01/07/2024: periodo 1 fino al 30/06, periodo 2 dal 01/07. */
    private static void periodiConTaglioAl1Luglio() {
        List<String[]> righe = new ArrayList<>();
        righe.add(periodo("1", "", "2024-06-30"));
        righe.add(periodo("2", "2024-07-01", ""));
        List<String> errori = Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", righe);
        assertTrue(errori.isEmpty(), () -> "periodi non salvati: " + errori);
    }

    private static String[] periodo(String prog, String di, String df) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = "CRYPTO";
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = prog;
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = di;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = df;
        r[Principale_GruppiWalletRW.COL_BOLLO] = "NO";
        r[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = Principale_GruppiWalletRW.MOD_SOLO_RESIDUO;
        r[Principale_GruppiWalletRW.COL_MOD_FINALE] = Principale_GruppiWalletRW.MOD_SOLO_RESIDUO;
        return r;
    }

    private static List<String[]> righi2024() {
        Calcoli_RW.AggiornaRWFR("2024");
        List<String[]> lista = Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 01");
        assertNotNull(lista);
        return lista;
    }

    private static String[] rigo(List<String[]> lista, String dataInizio, String dataFine) {
        return lista.stream().filter(x -> x[4].equals(dataInizio) && x[9].equals(dataFine)).findFirst()
                .orElseThrow(() -> new AssertionError("nessun rigo " + dataInizio + " → " + dataFine + " in "
                        + lista.stream().map(x -> x[4] + "→" + x[9]).toList()));
    }

    private static double valore(String s) {
        return Double.parseDouble(s);
    }

    @Test
    void senzaPeriodi_unRigoSullAnno() {
        acquisto("2023-03-01 10:00", "1000");

        List<String[]> lista = righi2024();
        assertEquals(1, lista.size());
        String[] r = rigo(lista, "2024-01-01 00:00", "2024-12-31 23:59");
        assertEquals("366", r[11]);
        assertEquals(1000.0, valore(r[5]), 0.001);
    }

    @Test
    void taglioSenzaMovimenti_chiusuraERiaperturaAlloStessoValore() {
        acquisto("2023-03-01 10:00", "1000");
        periodiConTaglioAl1Luglio();

        List<String[]> lista = righi2024();
        assertEquals(2, lista.size());
        String[] p1 = rigo(lista, "2024-01-01 00:00", "2024-06-30 23:59");
        String[] p2 = rigo(lista, "2024-07-01 00:00", "2024-12-31 23:59");
        assertEquals("Fine Periodo", p1[12]);
        assertEquals("Giacenza Fine Periodo", p1[14]);
        assertEquals("Giacenza Inizio Periodo", p2[13]);
        assertEquals("Fine Anno", p2[12]);
        assertEquals(1000.0, valore(p1[10]), 0.001);
        assertEquals(valore(p1[10]), valore(p2[5]), 0.001, "valore finale del periodo 1 = iniziale del periodo 2");
        assertEquals(366, Integer.parseInt(p1[11]) + Integer.parseInt(p2[11]));
    }

    @Test
    void movimentiPrimaEDopoIlTaglio_ogniLottoInUnSoloPeriodo() {
        acquisto("2023-03-01 10:00", "1000");
        acquisto("2024-03-01 10:00", "200");
        acquisto("2024-09-01 10:00", "500");
        periodiConTaglioAl1Luglio();

        List<String[]> lista = righi2024();
        for (String[] r : lista) {
            boolean primo = r[9].compareTo("2024-07-01") < 0;
            assertEquals(primo, r[4].compareTo("2024-07-01") < 0, "rigo a cavallo del taglio: " + r[4] + " → " + r[9]);
        }
        rigo(lista, "2024-01-01 00:00", "2024-06-30 23:59");
        assertEquals(200.0, valore(rigo(lista, "2024-03-01 10:00", "2024-06-30 23:59")[5]), 0.001);
        String[] riaperto = rigo(lista, "2024-07-01 00:00", "2024-12-31 23:59");
        assertEquals("1200", riaperto[8], "i lotti del periodo 1 si riaprono come un lotto unico");
        assertEquals(500.0, valore(rigo(lista, "2024-09-01 10:00", "2024-12-31 23:59")[5]), 0.001);
    }

    /** Motore + tabella di sintesi: due righi di periodo, nessun valore contato due volte. */
    @Test
    void motoreETabella_dueRighiDiPeriodoSenzaDoppioConteggio() {
        acquisto("2023-03-01 10:00", "1000");
        periodiConTaglioAl1Luglio();

        righi2024();
        java.util.Map<String, String[]> q = Principale_QuadroRW.RighiCrypto(Principale.Mappa_RW_ListeXGruppoWallet,
                Funzioni.RW_GiacenzeInizioFineAnno("2024"), g -> Calcoli_RW_PeriodiCrypto.trattiCrypto(g, "2024"),
                DatabaseH2::Pers_GruppoAlias_Leggi, false, false, true);
        String[] p1 = q.get("Wallet 01|2024-01-01");
        String[] p2 = q.get("Wallet 01|2024-07-01");
        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals(1000.0, valore(p1[1]), 0.001);
        assertEquals(1000.0, valore(p2[1]), 0.001);
        assertEquals(1000.0, valore(p2[2]), 0.001);
        assertEquals(182.0, valore(p1[3]), 0.001);
        assertEquals(184.0, valore(p2[3]), 0.001);
        assertNull(q.get("Wallet 01"), "nessun rigo unico accanto ai righi di periodo");
    }

    @Test
    void rilevanzaA_righiPerPeriodoDallaFotografia() {
        DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", "A");
        acquisto("2023-03-01 10:00", "1000");
        periodiConTaglioAl1Luglio();

        List<String[]> lista = righi2024();
        assertEquals(2, lista.size());
        assertEquals("Giacenza Fine Periodo", rigo(lista, "2024-01-01 00:00", "2024-06-30 23:59")[14]);
        assertEquals("Giacenza Inizio Periodo", rigo(lista, "2024-07-01 00:00", "2024-12-31 23:59")[13]);
    }

    @Test
    void lifoComplessivo_ilTaglioSiApplicaLoStesso() {
        DatabaseH2.Pers_Opzioni_Scrivi("RW_LiFoComplessivo", "SI");
        acquisto("2023-03-01 10:00", "1000");
        periodiConTaglioAl1Luglio();

        List<String[]> lista = righi2024();
        rigo(lista, "2024-01-01 00:00", "2024-06-30 23:59");
        rigo(lista, "2024-07-01 00:00", "2024-12-31 23:59");
    }

}
