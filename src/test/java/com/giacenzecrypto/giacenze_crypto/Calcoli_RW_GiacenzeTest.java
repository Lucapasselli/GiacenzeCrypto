package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regole della fotografia di inizio/fine anno ({@link Calcoli_RW_Giacenze}), l'unica che produce i righi
 * della Rilevanza A e della sostituzione per i gruppi col bollo. Decise con l'utente il 2026-09-26:
 * NFT dentro e FIAT fuori, apertura nell'anno valorizzata al valore del movimento (mercato se zero), la
 * moneta ceduta col primo movimento non è una giacenza iniziale, giacenze negative a zero ma segnalate.
 * Prezzi iniettati: nessun accesso a rete o cache.
 */
class Calcoli_RW_GiacenzeTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    /** Chiamate alla fonte dei prezzi: "moneta@istante". */
    private final List<String> chiamate = new ArrayList<>();

    /** Prezzo finto: 100 € per unità, e registra la chiamata. */
    private final Calcoli_RW_Giacenze.PrezzoGiacenza prezzo = (m, istante) -> {
        chiamate.add(m.Moneta + "@" + istante);
        return new BigDecimal(m.Qta).multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();
    };

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_RW_ListeXGruppoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM WALLETGRUPPO");
        }
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        progressivo = 0;
        chiamate.clear();
    }

    private static String[] mov(String data, String cat, String valore,
            String monU, String tipoU, String qtaU,
            String monE, String tipoE, String qtaE) {
        String prefisso = data.replace("-", "").replace(":", "").replace(" ", "");
        String id = prefisso + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "Kraken";
        m[8] = monU;  m[9] = tipoU;  m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        m[15] = valore;
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    private static long istante(String data) {
        return FunzioniDate.ConvertiDatainLongMinuto(data);
    }

    private List<String[]> righi2024() {
        Map<String, List<String[]>> r = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno("2024", prezzo);
        List<String[]> lista = r.get("Wallet 01");
        assertNotNull(lista, "il gruppo deve essere nel risultato");
        return lista;
    }

    private static String[] rigo(List<String[]> lista, String moneta) {
        return lista.stream().filter(x -> x[2].equals(moneta)).findFirst()
                .orElseThrow(() -> new AssertionError("nessun rigo per " + moneta));
    }

    @Test
    void nftInclusi_fiatEsclusi_prezziAgliEstremiDellAnno() {
        mov("2023-03-01 10:00", "AC", "1000", "EUR", "FIAT", "-1000", "BTC", "Crypto", "0.5");
        mov("2023-04-01 10:00", "DC", "0", "", "", "", "PUNK", "NFT", "1");

        List<String[]> lista = righi2024();
        assertEquals(2, lista.size(), "BTC e NFT, niente EUR");
        String[] btc = rigo(lista, "BTC");
        assertEquals("0.5", btc[3]);
        assertEquals("50", btc[5]);
        assertEquals("2024-01-01 00:00", btc[4]);
        assertEquals("2024-12-31 23:59", btc[9]);
        assertEquals("366", btc[11]);
        assertEquals("Giacenza Inizio Anno", btc[13]);
        assertTrue(chiamate.contains("BTC@" + istante("2024-01-01 00:00")));
        assertTrue(chiamate.contains("BTC@" + istante("2025-01-01 00:00")));
        rigo(lista, "PUNK");
    }

    @Test
    void gruppoApertoConUnDepositoInEuro_lEuroNonEntraNelValoreIniziale() {
        mov("2024-03-10 10:00", "DF", "1000", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-03-11 10:00", "AC", "1000", "EUR", "FIAT", "-1000", "BTC", "Crypto", "0.02");

        List<String[]> lista = righi2024();
        assertEquals(1, lista.size());
        String[] btc = rigo(lista, "BTC");
        assertEquals("0", btc[3], "il BTC non c'era all'apertura del gruppo");
        assertEquals("2024-03-10 10:00", btc[4], "l'inizio è il primo movimento del gruppo");
        assertEquals("297", btc[11]);
        assertEquals("Fine Anno", btc[12]);
    }

    @Test
    void monetaDiApertura_valorizzataAlValoreDelMovimento() {
        String[] primo = mov("2024-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "0.5");

        String[] btc = rigo(righi2024(), "BTC");
        assertEquals("0.5", btc[3]);
        assertEquals("15000", btc[5]);
        assertEquals(primo[0], btc[13]);
        assertEquals("Apertura Wallet/Fine Anno", btc[12]);
        assertFalse(chiamate.contains("BTC@" + istante("2024-02-01 10:00")), "il valore c'è: niente prezzo di mercato");
    }

    @Test
    void monetaDiApertura_valoreDelMovimentoZero_siUsaIlMercato() {
        mov("2024-02-01 10:00", "DC", "0", "", "", "", "BTC", "Crypto", "0.5");

        String[] btc = rigo(righi2024(), "BTC");
        assertEquals("50", btc[5]);
        assertTrue(chiamate.contains("BTC@" + istante("2024-02-01 10:00")));
    }

    @Test
    void aperturaNonNellAnno_nessunaDicituraDiApertura() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "0.5");

        String[] btc = rigo(righi2024(), "BTC");
        assertEquals("Fine Anno", btc[12]);
        assertEquals("Giacenza Inizio Anno", btc[13]);
        assertEquals("50", btc[5], "prezzo di mercato all'1/1, non il valore di un movimento del 2023");
    }

    /**
     * Apertura d'anno ≠ apertura wallet: per un gruppo che esiste già, il primo movimento dell'anno non
     * conta nulla nel valore iniziale — contano solo la giacenza e il prezzo alle 00:00 del 1° gennaio.
     */
    @Test
    void aperturaDAnno_ilPrimoMovimentoDellAnnoNonConta() {
        mov("2023-06-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "0.5");
        mov("2024-01-01 10:00", "DC", "99999", "", "", "", "BTC", "Crypto", "2");

        String[] btc = rigo(righi2024(), "BTC");
        assertEquals("0.5", btc[3], "giacenza a inizio giornata del 1/1, senza il deposito delle 10:00");
        assertEquals("50", btc[5], "prezzo alle 00:00, non il valore del movimento");
        assertEquals("2024-01-01 00:00", btc[4]);
        assertEquals("366", btc[11]);
        assertEquals("Fine Anno", btc[12]);
        assertEquals("Giacenza Inizio Anno", btc[13]);
        assertEquals("2.5", btc[8]);
        assertTrue(chiamate.contains("BTC@" + istante("2024-01-01 00:00")));
        assertFalse(chiamate.contains("BTC@" + istante("2024-01-01 10:00")));
    }

    @Test
    void primoMovimentoScambio_laMonetaCedutaNonEUnaGiacenzaIniziale() {
        mov("2024-05-01 10:00", "SC", "3000", "ETH", "Crypto", "-1", "BTC", "Crypto", "0.05");

        List<String[]> lista = righi2024();
        String[] btc = rigo(lista, "BTC");
        assertEquals("0.05", btc[3]);
        String[] eth = rigo(lista, "ETH");
        assertEquals("0", eth[3]);
        assertEquals("-1", eth[8]);
        assertEquals(Calcoli_RW_Giacenze.VALORE_ZERO, eth[10]);
        assertEquals(Calcoli_RW_Giacenze.ERRORE_GIACENZA_NEGATIVA, eth[15]);
        assertTrue(chiamate.stream().noneMatch(c -> c.startsWith("ETH@")), "una giacenza negativa non si prezza");
    }

    @Test
    void giacenzaNegativa_valoreZeroESegnalata_maiValoreAssoluto() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-06-01 10:00", "PC", "0", "BTC", "Crypto", "-2", "", "", "");

        String[] btc = rigo(righi2024(), "BTC");
        assertEquals("100", btc[5], "l'inizio è positivo e si prezza");
        assertEquals("-1", btc[8]);
        assertEquals(Calcoli_RW_Giacenze.VALORE_ZERO, btc[10]);
        assertEquals(Calcoli_RW_Giacenze.ERRORE_GIACENZA_NEGATIVA, btc[15]);
    }

    @Test
    void gruppoConMovimentiSoloDopoLAnno_presenteSenzaRighi() {
        mov("2025-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");

        assertTrue(righi2024().isEmpty());
    }

    // ------------------------------------------------------------------
    // tratti (periodi CRYPTO) e modalità sui confini
    // ------------------------------------------------------------------

    private static String[] periodo(String prog, String di, String df, String modIni, String modFin) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = "CRYPTO";
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = prog;
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = di;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = df;
        r[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = modIni;
        r[Principale_GruppiWalletRW.COL_MOD_FINALE] = modFin;
        return r;
    }

    /** Due periodi con taglio al 01/07/2024 e le modalità indicate sul confine. */
    private List<String[]> righiDueTratti(String modFinP1, String modIniP2) {
        List<String[]> tratti = Calcoli_RW_PeriodiCrypto.trattiCrypto("2024", List.of(
                periodo("1", "", "2024-06-30", "", modFinP1),
                periodo("2", "2024-07-01", "", modIniP2, "")), false);
        List<String[]> lista = Calcoli_RW_Giacenze.GiacenzePerTratti("2024", g -> tratti, prezzo).get("Wallet 01");
        assertNotNull(lista);
        return lista;
    }

    private static String[] rigo(List<String[]> lista, String moneta, String dataFine) {
        return lista.stream().filter(x -> x[2].equals(moneta) && x[9].equals(dataFine)).findFirst()
                .orElseThrow(() -> new AssertionError("nessun rigo per " + moneta + " al " + dataFine));
    }

    @Test
    void dueTratti_ilFinaleDelPrimoEIlInizialeDelSecondo_giorniCheSommanoAllAnno() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");

        List<String[]> lista = righiDueTratti("", "");
        assertEquals(2, lista.size());
        String[] p1 = rigo(lista, "BTC", "2024-06-30 23:59");
        String[] p2 = rigo(lista, "BTC", "2024-12-31 23:59");
        assertEquals("2024-01-01 00:00", p1[4]);
        assertEquals("100", p1[5]);
        assertEquals("100", p1[10]);
        assertEquals("Giacenza Inizio Anno", p1[13]);
        assertEquals("Giacenza Fine Periodo", p1[14]);
        assertEquals("Fine Periodo", p1[12]);
        assertEquals("2024-07-01 00:00", p2[4]);
        assertEquals(p1[10], p2[5], "valore finale del periodo 1 = valore iniziale del periodo 2");
        assertEquals("Giacenza Inizio Periodo", p2[13]);
        assertEquals("Giacenza Fine Anno", p2[14]);
        assertEquals("Fine Anno", p2[12]);
        assertEquals(366, Integer.parseInt(p1[11]) + Integer.parseInt(p2[11]));
        assertTrue(chiamate.contains("BTC@" + istante("2024-07-01 00:00")));
    }

    @Test
    void confineSenzaModalita_ilMovimentoDelGiornoNonConta() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-07-01 10:00", "DC", "40000", "", "", "", "BTC", "Crypto", "0.5");

        List<String[]> lista = righiDueTratti("", "");
        assertEquals("1", rigo(lista, "BTC", "2024-12-31 23:59")[3]);
        assertEquals("100", rigo(lista, "BTC", "2024-12-31 23:59")[5]);
        assertEquals("1", rigo(lista, "BTC", "2024-06-30 23:59")[8]);
    }

    @Test
    void primoApporto_soloIlPrimoMovimentoInEntrataDelGiorno_alSuoValore() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-07-01 10:00", "DC", "20000", "", "", "", "BTC", "Crypto", "0.5");
        mov("2024-07-01 11:00", "DC", "500", "", "", "", "ETH", "Crypto", "0.2");

        List<String[]> lista = righiDueTratti("", Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO);
        String[] btc = rigo(lista, "BTC", "2024-12-31 23:59");
        assertEquals("1.5", btc[3]);
        assertEquals("20100", btc[5], "residuo al prezzo delle 00:00 + apporto al valore del movimento");
        assertEquals("0", rigo(lista, "ETH", "2024-12-31 23:59")[3], "il secondo apporto non conta");
    }

    @Test
    void sommaApporti_tuttiIMovimentiInEntrataDelGiorno_esclusiITrasferimentiInterni() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-07-01 10:00", "DC", "20000", "", "", "", "BTC", "Crypto", "0.5");
        mov("2024-07-01 11:00", "DC", "500", "", "", "", "ETH", "Crypto", "0.2");
        mov("2024-07-01 12:00", "TI", "900", "", "", "", "SOL", "Crypto", "3");

        List<String[]> lista = righiDueTratti("", Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI);
        assertEquals("20100", rigo(lista, "BTC", "2024-12-31 23:59")[5]);
        String[] eth = rigo(lista, "ETH", "2024-12-31 23:59");
        assertEquals("0.2", eth[3]);
        assertEquals("500", eth[5]);
        assertEquals("0", rigo(lista, "SOL", "2024-12-31 23:59")[3], "un trasferimento interno non è un apporto");
    }

    @Test
    void ultimaUscita_siRiaggiungeSoloLUltima_alSuoValore() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-06-30 09:00", "PC", "9000", "BTC", "Crypto", "-0.3", "", "", "");
        mov("2024-06-30 18:00", "PC", "3000", "BTC", "Crypto", "-0.1", "", "", "");

        String[] p1 = rigo(righiDueTratti(Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA, ""), "BTC", "2024-06-30 23:59");
        assertEquals("0.7", p1[8]);
        assertEquals("3060", p1[10], "giacenza di fine giornata al prezzo del confine + ultima uscita al suo valore");
    }

    @Test
    void sommaUscite_siRiaggiungonoTutte() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        mov("2024-06-30 09:00", "PC", "9000", "BTC", "Crypto", "-0.3", "", "", "");
        mov("2024-06-30 18:00", "PC", "3000", "BTC", "Crypto", "-0.1", "", "", "");

        String[] p1 = rigo(righiDueTratti(Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE, ""), "BTC", "2024-06-30 23:59");
        assertEquals("1", p1[8]);
        assertEquals("12060", p1[10]);
    }

    @Test
    void gruppoApertoNelSecondoTratto_primoTrattoVuoto_secondoDallApertura() {
        String[] primo = mov("2024-08-10 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "0.5");

        List<String[]> lista = righiDueTratti("", Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI);
        assertEquals(1, lista.size(), "prima dell'apertura il gruppo non esisteva");
        String[] p2 = lista.get(0);
        assertEquals("2024-08-10 10:00", p2[4]);
        assertEquals("144", p2[11]);
        assertEquals("15000", p2[5]);
        assertEquals(primo[0], p2[13]);
        assertEquals("Apertura Wallet/Fine Anno", p2[12]);
    }

    @Test
    void rilevanzaA_riprendeIRighiDellaFotografia() {
        mov("2023-02-01 10:00", "DC", "15000", "", "", "", "BTC", "Crypto", "1");
        Map<String, List<String[]>> giacenze = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno("2024", prezzo);

        Calcoli_RW.ChiudiRWGiacenzeFinali("Wallet 01", giacenze);
        Calcoli_RW.ChiudiRWGiacenzeFinali("Wallet 77", giacenze);

        assertEquals(1, Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 01").size());
        assertTrue(Principale.Mappa_RW_ListeXGruppoWallet.get("Wallet 77").isEmpty());
    }
}
