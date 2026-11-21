package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il token di un contratto su derivati ({@code DERIV.<contratto>.LONG|SHORT}, {@link OKXDerivati}) non e' una cripto-attivita':
 * e' la fotografia di una posizione, a valore zero. Non entra nel quadro W/RW (ne' nei righi ne' nelle giacenze di inizio e fine),
 * altrimenti ogni posizione aperta a fine anno darebbe un rigo con "Valore non valorizzato". Il margine parcheggiato nel
 * sotto-wallet dei derivati resta invece una cripto-attivita' (decisione D15, P1). Dati inventati.
 */
class Calcoli_RW_DerivatiTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;
    private static final String TOKEN = "DERIV.BTC-USDT-SWAP.LONG";

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        Funzioni.ReteDisabilitataPerTest = true;
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
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
        progressivo = 0;
    }

    private static void opzioni(String rilevanza) {
        for (String[] o : new String[][] {{"RW_Rilevanza", rilevanza}, {"RW_ChiudiRWsuTrasferimento", "NO"},
                {"RW_StakingZero", "NO"}, {"RW_LiFoComplessivo", "NO"}, {"RW_LiFoSubMovimenti", "NO"},
                {"RW_FiatInRW", "NO"}}) {
            DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
        }
    }

    private static String[] riga(String data, String categoria, String sottoWallet) {
        String prefisso = data.replace("-", "").replace(":", "").replace(" ", "") + "00";
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = prefisso + "_Kraken_" + String.format("%03d", ++progressivo) + "_001_" + categoria;
        m[1] = data;
        m[3] = "Kraken";
        m[4] = sottoWallet;
        Principale.MappaCryptoWallet.put(m[0], m);
        return m;
    }

    /** Acquisto di EURe con euro: apre un lotto al valore del movimento. */
    private static void acquisto(String data, String qta) {
        String[] m = riga(data, "AC", "Principale");
        m[8] = "EUR"; m[9] = "FIAT"; m[10] = "-" + qta;
        m[11] = "EURe"; m[12] = "Crypto"; m[13] = qta;
        m[15] = qta;
    }

    /** Il token del contratto come lo scrive l'import: deposito a costo zero nel sotto-wallet dei derivati, prezzo zero. */
    private static void aperturaContratto(String data) {
        String[] m = riga(data, "DC", OKXDerivati.SOTTOWALLET);
        m[5] = "APERTURA CONTRATTO DERIVATO";
        m[11] = TOKEN; m[12] = "Crypto"; m[13] = "1";
        m[14] = "Contratto derivato";
        m[15] = "0.00";
        m[18] = "DCZ - APERTURA CONTRATTO DERIVATO";
        m[32] = "SI";
        Derivati.Marca(m, Derivati.CONTRATTO);
    }

    private static void chiusuraContratto(String data) {
        String[] m = riga(data, "PC", OKXDerivati.SOTTOWALLET);
        m[5] = "CHIUSURA CONTRATTO DERIVATO";
        m[8] = TOKEN; m[9] = "Crypto"; m[10] = "-1";
        m[14] = "Contratto derivato";
        m[15] = "0.00";
        m[18] = "PWN - CHIUSURA CONTRATTO DERIVATO";
        m[32] = "SI";
        Derivati.Marca(m, Derivati.CONTRATTO);
    }

    private static List<String[]> righi2024() {
        Calcoli_RW.AggiornaRWFR("2024");
        List<String[]> lista = Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 01");
        assertNotNull(lista);
        return lista;
    }

    private static boolean citaIlToken(String[] r) {
        return (r[2] != null && r[2].contains("DERIV.")) || (r[7] != null && r[7].contains("DERIV."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "B", "C", "D"})
    void unaPosizioneApertaAFineAnnoNonEntraNelQuadro(String rilevanza) {
        opzioni(rilevanza);
        acquisto("2023-03-01 10:00", "1000");
        aperturaContratto("2023-11-01 10:00");
        aperturaContratto("2024-03-01 10:00");
        List<String[]> lista = righi2024();
        assertTrue(lista.stream().noneMatch(Calcoli_RW_DerivatiTest::citaIlToken),
                () -> "righi con il token del contratto: " + lista.stream().filter(Calcoli_RW_DerivatiTest::citaIlToken)
                        .map(r -> r[2] + " -> " + r[7] + " " + r[15]).toList());
        assertTrue(lista.stream().anyMatch(r -> "EURe".equals(r[2]) || "EURe".equals(r[7])), "la cripto-attivita' vera resta");
        assertTrue(lista.stream().noneMatch(r -> r[15] != null && r[15].contains("non Valorizzato")),
                () -> "errori: " + lista.stream().map(r -> r[2] + " " + r[15]).toList());
    }

    /** Il token aperto e chiuso nell'anno, anche come ultimo movimento dell'archivio. */
    @ParameterizedTest
    @ValueSource(strings = {"A", "D"})
    void unaPosizioneChiusaNellAnnoNonEntraNelQuadro(String rilevanza) {
        opzioni(rilevanza);
        acquisto("2024-01-10 10:00", "1000");
        aperturaContratto("2024-03-01 10:00");
        chiusuraContratto("2024-12-31 23:00");
        List<String[]> lista = righi2024();
        assertTrue(lista.stream().noneMatch(Calcoli_RW_DerivatiTest::citaIlToken));
        assertTrue(lista.stream().anyMatch(r -> "EURe".equals(r[2]) || "EURe".equals(r[7])));
    }

    /** Il token come primo movimento dell'anno dopo: non deve togliere nulla al quadro dell'anno. */
    @ParameterizedTest
    @ValueSource(strings = {"A", "D"})
    void ilTokenComePrimoMovimentoDellAnnoDopo(String rilevanza) {
        opzioni(rilevanza);
        acquisto("2024-01-10 10:00", "1000");
        aperturaContratto("2025-01-01 00:00");
        List<String[]> lista = righi2024();
        assertTrue(lista.stream().noneMatch(Calcoli_RW_DerivatiTest::citaIlToken));
        assertTrue(lista.stream().anyMatch(r -> "EURe".equals(r[7]) && r[9].startsWith("2024-12-31")),
                () -> "manca la giacenza di fine anno: " + lista.stream().map(r -> r[2] + " " + r[4] + " -> " + r[9]).toList());
    }
}
