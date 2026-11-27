package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Il token di possesso {@code ETH.DA_CONSEGNARE} dello scambio differito con il deposito PRIMA del prelievo: la moneta
 * già ceduta con lo scambio (plusvalenza realizzata al deposito) che fisicamente resta nel wallet fino al prelievo.
 *
 * <p>Punto 1 (valore e plusvalenza latente di fine anno): il DA_CONSEGNARE non ha lotti LIFO, quindi si prezza come la
 * moneta di cui è la copia e il suo costo è il suo valore, cioè plusvalenza latente zero. Senza questa regola il
 * quadro RT segnerebbe come latente tutto il valore (costo zero) e "Giacenze a data" lo mostrerebbe come utile.
 *
 * <p>La forma con il DA_CONSEGNARE si ottiene dalla classificazione attuale riscrivendo MT1 (entrata del token), il
 * prelievo (token DA_CONSEGNARE) e il wallet di MS/MT1/MT2: lo stesso passaggio che farà la classificazione.
 */
class ScambioDifferitoDaConsegnareTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        Funzioni.ReteDisabilitataPerTest = true;
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        MappaCryptoWallet.clear();
        Principale.Mappa_RW_ListeXGruppoWallet.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_RW_ListeXGruppoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM WALLETGRUPPO");
            st.execute("DELETE FROM GRUPPO_PERIODO_RW");
        }
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");
        for (String[] o : new String[][]{{"PL_CosiderareMovimentiNC", "SI"}, {"PlusXWallet", "NO"},
            {"Plusvalenze_NoPlusvalenzeCommissioni", "NO"}, {"Plusvalenze_Pre2023EarnCostoZero", "NO"},
            {"Plusvalenze_Pre2023ScambiRilevanti", "NO"}, {"Plusvalenze_ScambiSempreRilevanti", "NO"},
            {"PDD_Staking", "SI"}, {"PDD_CashBack", "SI"}, {"PDD_Airdrop", "SI"}, {"PDD_Earn", "SI"},
            {"PDD_Reward", "SI"}, {"CashBackComeFIAT", "NO"}}) {
            DatabaseH2.Pers_Opzioni_Scrivi(o[0], o[1]);
        }
        progressivo = 0;
    }

    // ------------------------------------------------------------------
    // Scenario
    // ------------------------------------------------------------------

    private static String[] riga(String data, String tipo, String exchange, String descr,
            String monU, String tipoU, String qtaU, String monE, String tipoE, String qtaE, String valore) {
        String id = data.replace("-", "").replace(":", "").replace(" ", "") + "00_" + exchange + "_001_"
                + String.format("%03d", ++progressivo) + "_" + tipo;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[2] = "1 di 1";
        m[3] = exchange;
        m[4] = "Principale";
        m[5] = descr;
        m[8] = monU; m[9] = tipoU; m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        m[15] = valore;
        m[22] = tipo.equals("DC") || tipo.equals("PC") ? "A" : "M";
        m[32] = "SI";
        m[29] = String.valueOf(FunzioniDate.ConvertiDatainLongMinuto(data));
        MappaCryptoWallet.put(id, m);
        return m;
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

    /** Riferimenti ai movimenti dello scenario. */
    private static class Scenario {
        String[] deposito, prelievo;
        String mt1, ms, mt2;
    }

    private static void seedPrezzi(String... date) throws Exception {
        for (String d : date) {
            seedPrezzo("BTC", d, "60000");
            seedPrezzo("ETH", d, "3000");
        }
    }

    private static final String[] DATE_PREZZI = {"2024-06-01 10:00", "2024-12-28 12:00", "2024-12-30 12:00",
        "2024-12-31 23:00", "2024-12-31 23:59", "2025-01-01 00:00", "2025-01-02 12:00"};

    /** Scenario non classificato: 10 ETH comprati l'1/6/2024 per 20.000, 0,5 BTC incassati il 28/12 (deposito), 5 ETH
     *  comprati il 30/12 per 15.000, 10 ETH spediti il 2/1/2025 (prelievo). Prezzi: ETH 3.000, BTC 60.000. */
    private static Scenario scenarioNonClassificato(String exchangeDeposito) throws Exception {
        Scenario s = new Scenario();
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        s.deposito = riga("2024-12-28 12:00", "DC", exchangeDeposito, "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        riga("2024-12-30 12:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-15000", "ETH", "Crypto", "5", "15000");
        s.prelievo = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        seedPrezzi(DATE_PREZZI);
        return s;
    }

    private static Scenario classifica(Scenario s) {
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(s.prelievo[0], s.deposito[0]));
        String[] ids = MappaCryptoWallet.get(s.prelievo[0]).clone()[20].split(",");
        s.mt1 = ids[0]; s.ms = ids[1]; s.mt2 = ids[2];
        return s;
    }

    private static Scenario scenarioDaConsegnare() throws Exception {
        return classifica(scenarioNonClassificato("WalletA"));
    }

    private static Map<String, String[]> copia() {
        Map<String, String[]> c = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) c.put(v[0], v.clone());
        return c;
    }

    private static Map<String, String> fotografia() {
        Map<String, String> m = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) m.put(v[0], String.join(";", v));
        return m;
    }

    /**
     * La forma di prima del token di possesso, ricostruita dalla nuova: moneta ceduta senza suffisso, MT1 all'istante del
     * prelievo con il valore del prelievo, MT1/MS/MT2 sulla piattaforma fittizia.
     * In {@code idMT1} l'ID di MT1 nella forma nuova e in quella ricostruita.
     */
    private static Map<String, String[]> formaPiattaforma(Map<String, String[]> nuova, Scenario s, String[] idMT1) {
        String[] pre = nuova.get(s.prelievo[0]);
        String vecchioId = s.mt1;
        String[] seg = vecchioId.split("_");
        seg[0] = pre[0].split("_")[0];
        String nuovoId = String.join("_", seg);
        idMT1[0] = vecchioId;
        idMT1[1] = nuovoId;
        Map<String, String[]> ris = new TreeMap<>();
        for (String[] v : nuova.values()) {
            String[] c = v.clone();
            if (c[0].equals(s.prelievo[0])) c[8] = TokenDaConsegnare.Base(c[8]);
            if (c[0].equals(vecchioId)) {
                c[0] = nuovoId; c[1] = pre[1]; c[29] = pre[29];
                c[11] = TokenDaConsegnare.Base(c[11]);
                c[15] = pre[15];
            }
            if (c[0].equals(nuovoId) || c[0].equals(s.ms) || c[0].equals(s.mt2)) c[4] = "Piattaforma di scambio";
            c[20] = c[20].replace(vecchioId, nuovoId);
            ris.put(c[0], c);
        }
        return ris;
    }

    /** Esegue il motore su una mappa e restituisce, per ID, i campi 16/17/19/33/38. */
    private static Map<String, String> calcola(Map<String, String[]> mappa) {
        MappaCryptoWallet.clear();
        for (String[] v : mappa.values()) MappaCryptoWallet.put(v[0], v.clone());
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Map<String, String> ris = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values())
            ris.put(v[0], v[16] + "|" + v[17] + "|" + v[19] + "|" + v[33] + "|" + v[38]);
        return ris;
    }

    private static BigDecimal quantitaAl(String gruppo, String token, String fineIstante) {
        BigDecimal tot = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[0].split("_")[0].compareTo(fineIstante) > 0) continue;
            if (!gruppo.equals(DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true))) continue;
            if (token.equals(v[8]) && !v[10].isBlank()) tot = tot.add(new BigDecimal(v[10]));
            if (token.equals(v[11]) && !v[13].isBlank()) tot = tot.add(new BigDecimal(v[13]));
        }
        return tot;
    }

    // ------------------------------------------------------------------
    // La forma dei cinque movimenti
    // ------------------------------------------------------------------

    @Test
    void forma_cinqueMovimentiCollegatiConIlTokenDiPossesso() throws Exception {
        Scenario s = scenarioDaConsegnare();
        String[] pre = MappaCryptoWallet.get(s.prelievo[0]);
        String[] mt1 = MappaCryptoWallet.get(s.mt1), ms = MappaCryptoWallet.get(s.ms), mt2 = MappaCryptoWallet.get(s.mt2);
        String[] dep = MappaCryptoWallet.get(s.deposito[0]);
        assertEquals(2 + 5, MappaCryptoWallet.size(), "due acquisti + i cinque movimenti dello scambio (prelievo e deposito originali compresi)");
        //Il prelievo originale esce come ETH.DA_CONSEGNARE, alla sua data
        assertEquals("ETH.DA_CONSEGNARE", pre[8]);
        assertEquals("PTW - Scambio Differito", pre[18]);
        assertTrue(pre[0].startsWith("20250102120000_00"));
        //MT1: entra ETH.DA_CONSEGNARE alla data del deposito, sul wallet del prelievo
        assertTrue(mt1[0].startsWith("20241228120000_01") && mt1[0].endsWith("_DC"), mt1[0]);
        assertEquals("ETH.DA_CONSEGNARE", mt1[11]);
        assertEquals("10", mt1[13]);
        assertEquals("AU", mt1[22]);
        assertEquals("DTW - Trasferimento Interno", mt1[18]);
        assertEquals(pre[4], mt1[4]);
        //Scambio e MT2 sul wallet del prelievo, non sulla piattaforma fittizia
        assertEquals(pre[4], ms[4]);
        assertEquals(pre[4], mt2[4]);
        assertEquals("ETH", ms[8]);
        assertEquals("10", ms[10].replace("-", ""));
        //Il valore di MT1 è quello della moneta ceduta alla data del deposito (10 ETH a 3.000), non quello del prelievo
        assertEquals(0, new BigDecimal("30000").compareTo(new BigDecimal(mt1[15])), mt1[15]);
        assertNotEquals(pre[15], mt1[15]);
        //Note che spiegano il token
        assertTrue(mt1[21].contains("ETH.DA_CONSEGNARE") && ms[21].contains("ETH.DA_CONSEGNARE"), mt1[21] + " / " + ms[21]);
        //Descrizioni: il ruolo per primo, la stessa coda per tutte
        assertEquals("CONSEGNA PER SCAMBIO DIFFERITO", pre[5]);
        assertEquals("INCASSO PER SCAMBIO DIFFERITO", dep[5]);
        assertEquals("POSSESSO DA CONSEGNARE PER SCAMBIO DIFFERITO", mt1[5]);
        assertEquals("USCITA TECNICA PER SCAMBIO DIFFERITO", mt2[5]);
        assertEquals("SCAMBIO CRYPTO", ms[5]);
        //MT2 spiega che è la compensazione dell'ingresso dello scambio (stesso wallet di prelievo e deposito)
        assertTrue(mt2[21].contains("Uscita tecnica dei BTC") && mt2[21].contains("stesso wallet"), mt2[21]);
        //Collegamenti simmetrici: ogni movimento cita gli altri quattro e nessun altro
        String[] tutti = {pre[0], mt1[0], ms[0], mt2[0], dep[0]};
        for (String id : tutti) {
            String[] v = MappaCryptoWallet.get(id);
            java.util.Set<String> citati = new java.util.TreeSet<>(Arrays.asList(v[20].split(",")));
            java.util.Set<String> attesi = new java.util.TreeSet<>(Arrays.asList(tutti));
            attesi.remove(id);
            assertEquals(attesi, citati, "[20] di " + id);
        }
    }

    @Test
    void forma_walletDiversi_laNotaDiMT2DiceCheIBtcPassanoAlWalletDelDeposito() throws Exception {
        Scenario s = classifica(scenarioNonClassificato("WalletB"));
        String nota = MappaCryptoWallet.get(s.mt2)[21];
        assertTrue(nota.contains("Uscita dei BTC ricevuti dal wallet del prelievo") && nota.contains("wallet del deposito"), nota);
        assertFalse(nota.contains("stesso wallet"), nota);
    }

    @Test
    void forma_ilValoreDiMT1EQuelloDellaMonetaCedutaNonDiQuellaRicevuta() throws Exception {
        //OTC con sconto: 10 ETH valgono 30.000 ma si ricevono 0,48 BTC (28.800)
        Scenario s = scenarioNonClassificato("WalletA");
        MappaCryptoWallet.get(s.deposito[0])[13] = "0.48";
        seedPrezzo("BTC", "2024-12-28 12:00", "60000");
        classifica(s);
        String[] mt1 = MappaCryptoWallet.get(s.mt1), ms = MappaCryptoWallet.get(s.ms);
        assertEquals(0, new BigDecimal("28800").compareTo(new BigDecimal(ms[15])), "MS prezza la moneta ricevuta: " + ms[15]);
        assertEquals(0, new BigDecimal("30000").compareTo(new BigDecimal(mt1[15])), "MT1 la moneta ceduta: " + mt1[15]);
    }

    /** Saldo corrente per moneta del sotto-wallet indicato, nell'ordine della mappa (cioè degli ID): la stessa lettura dei saldi negativi. */
    private static Map<String, BigDecimal> minimiSottoWallet(String sottoWallet, List<String> cronologia) {
        Map<String, BigDecimal> corrente = new TreeMap<>(), minimo = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!v[4].equals(sottoWallet)) continue;
            for (int k : new int[]{8, 11}) {
                String q = v[k + 2];
                if (v[k].isBlank() || q.isBlank()) continue;
                BigDecimal nuovo = corrente.merge(v[k], new BigDecimal(q), BigDecimal::add);
                minimo.merge(v[k], nuovo, BigDecimal::min);
                cronologia.add(v[1] + " " + v[5].substring(0, Math.min(28, v[5].length())) + " " + v[k] + " " + q + " -> saldo " + nuovo.stripTrailingZeros().toPlainString());
            }
        }
        return minimo;
    }

    @Test
    void scartoNelloStessoGiorno_conLaMonetaGiaNelWallet_formaConPiattaformaCheNonVaMaiInNegativo() throws Exception {
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        String[] pre = riga("2024-12-28 12:01", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        seedPrezzi(DATE_PREZZI);
        classifica(s);
        assertForma_piattaforma(s);
        List<String> cronologia = new java.util.ArrayList<>();
        Map<String, BigDecimal> minimo = minimiSottoWallet("Piattaforma di scambio", cronologia);
        System.out.println("PIATTAFORMA " + cronologia);
        for (Map.Entry<String, BigDecimal> e : minimo.entrySet())
            assertTrue(e.getValue().signum() >= 0, "la piattaforma non va in negativo su " + e.getKey() + ": " + cronologia);
        assertFalse(Funzioni.ControllaSaldiNegativi().stream().anyMatch(x -> x.toString().contains("Piattaforma")));
    }

    @Test
    void scartoNelloStessoGiorno_conLaMonetaCheArrivaDopoIlDeposito_serveIlTokenEIlNegativoSiVede() throws Exception {
        //Deposito 12:00, acquisto degli ETH 12:20, prelievo 12:40: al deposito la pila non ha ETH da vendere
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        riga("2024-12-28 12:20", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-30000", "ETH", "Crypto", "10", "30000");
        String[] pre = riga("2024-12-28 12:40", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        seedPrezzi(DATE_PREZZI);
        classifica(s);
        assertEquals("ETH.DA_CONSEGNARE", MappaCryptoWallet.get(s.prelievo[0])[8], "con la moneta mancante al deposito serve il token");
        List<String> neg = Funzioni.ControllaSaldiNegativi().stream().map(Object::toString).toList();
        assertTrue(neg.stream().anyMatch(x -> x.endsWith(";ETH")), "il negativo compare sull'ETH: " + neg);
        assertFalse(neg.stream().anyMatch(x -> x.contains("Piattaforma") || x.contains("DA_CONSEGNARE")), neg.toString());
    }

    @Test
    void scartoDiGiorniDiversi_ancheConLaMonetaPresente_serveIlToken() throws Exception {
        Scenario s = scenarioBloccatiPrimaDelPrelievoSenzaAcquistoIntermedio();
        assertEquals("ETH.DA_CONSEGNARE", MappaCryptoWallet.get(s.prelievo[0])[8]);
    }

    private static Scenario scenarioBloccatiPrimaDelPrelievoSenzaAcquistoIntermedio() throws Exception {
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        Scenario s = new Scenario();
        s.deposito = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        s.prelievo = riga("2024-12-29 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        seedPrezzi(DATE_PREZZI);
        return classifica(s);
    }

    @Test
    void forma_ordineNormale_resteLaPiattaformaESenzaToken() throws Exception {
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        String[] pre = riga("2024-12-28 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        String[] dep = riga("2025-01-02 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        seedPrezzi(DATE_PREZZI);
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pre[0], dep[0]));
        String[] mt1 = MappaCryptoWallet.get(pre[20].split(",")[0]);
        assertTrue(mt1[0].startsWith("20241228120000_01"), mt1[0]);
        assertEquals("ETH", mt1[11]);
        assertEquals("Piattaforma di scambio", mt1[4]);
        assertEquals("ETH", MappaCryptoWallet.get(pre[0]).clone()[8]);
        //(l'EUR del wallet è negativo per costruzione dello scenario: gli acquisti non hanno un deposito in euro)
        assertEquals(List.of(), Funzioni.ControllaSaldiNegativi().stream().filter(x -> !x.toString().endsWith(";EUR")).toList());
    }

    // ------------------------------------------------------------------
    // Quando non si usa il token: forma con la piattaforma, MT1 comunque alla data del deposito
    // ------------------------------------------------------------------

    private void assertForma_piattaforma(Scenario s) {
        String[] pre = MappaCryptoWallet.get(s.prelievo[0]);
        String[] mt1 = MappaCryptoWallet.get(s.mt1);
        assertFalse(TokenDaConsegnare.Is(pre[8]));
        assertFalse(TokenDaConsegnare.Is(mt1[11]));
        assertEquals("Piattaforma di scambio", mt1[4]);
        assertEquals("Piattaforma di scambio", MappaCryptoWallet.get(s.ms)[4]);
        assertEquals("INGRESSO TECNICO PER SCAMBIO DIFFERITO", mt1[5]);
        assertEquals("CONSEGNA PER SCAMBIO DIFFERITO", pre[5]);
        assertTrue(mt1[0].startsWith("20241228120000_01"), "MT1 alla data del deposito: " + mt1[0]);
    }

    @Test
    void guardie_monetaScam() throws Exception {
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "XYZ **", "Crypto", "10", "20000");
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "XYZ **", "Crypto", "-10", "", "", "", "31000");
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        seedPrezzi(DATE_PREZZI);
        classifica(s);
        assertForma_piattaforma(s);
    }

    @Test
    void guardie_gambaFiat() throws Exception {
        //Il prelievo cripto contro un deposito in euro (vendita differita): niente token
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        String[] dep = riga("2024-12-28 12:00", "DF", "WalletA", "DEPOSITO FIAT", "", "", "", "EUR", "FIAT", "30000", "30000");
        String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        seedPrezzi(DATE_PREZZI);
        classifica(s);
        assertForma_piattaforma(s);
    }

    @Test
    void guardie_contoDerivati() throws Exception {
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        pre[4] = "Derivati";
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        seedPrezzi(DATE_PREZZI);
        classifica(s);
        assertForma_piattaforma(s);
    }

    @Test
    void guardie_piattaformaDiversaDallaPredefinita() throws Exception {
        Scenario s = scenarioNonClassificato("WalletA");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(s.prelievo[0], s.deposito[0], "Dual Savings"));
        String[] ids = MappaCryptoWallet.get(s.prelievo[0]).clone()[20].split(",");
        String[] mt1 = MappaCryptoWallet.get(ids[0]);
        assertEquals("Dual Savings", mt1[4]);
        assertEquals("ETH", mt1[11]);
        assertEquals("ETH", MappaCryptoWallet.get(s.prelievo[0])[8]);
    }

    @Test
    void guardie_etherFiDepositoDiPochiMinutiPrima_formaConPiattaformaSenzaSaldiNegativi() throws Exception {
        //ether.fi: l'abbinamento accetta un deposito fino a 5 minuti PRIMA della richiesta e passa la propria piattaforma
        //("ether.fi swap"): non è mai il caso del token, ma l'ingresso sulla piattaforma sta alla data del deposito
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-20000", "ETH", "Crypto", "10", "20000");
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        String[] pre = riga("2024-12-28 12:03", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        seedPrezzi(DATE_PREZZI);
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pre[0], dep[0], "ether.fi swap"));
        String[] mt1 = MappaCryptoWallet.get(MappaCryptoWallet.get(pre[0])[20].split(",")[0]);
        assertEquals("ether.fi swap", mt1[4]);
        assertEquals("ETH", mt1[11]);
        assertEquals("ETH", MappaCryptoWallet.get(pre[0])[8]);
        assertTrue(mt1[0].startsWith("20241228120000_01"), "ingresso alla data del deposito: " + mt1[0]);
        assertFalse(Funzioni.ControllaSaldiNegativi().stream().anyMatch(x -> x.toString().contains("ether.fi swap")),
                Funzioni.ControllaSaldiNegativi().toString());
    }

    // ------------------------------------------------------------------
    // Motore LIFO: stessi risultati della forma con la piattaforma
    // ------------------------------------------------------------------

    private void confrontaMotore(String exchangeDeposito, boolean plusXWallet, boolean scambiSempreRilevanti) throws Exception {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", plusXWallet ? "SI" : "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", scambiSempreRilevanti ? "SI" : "NO");
        MappaCryptoWallet.clear();
        progressivo = 0;
        Scenario s = classifica(scenarioNonClassificato(exchangeDeposito));
        riga("2025-03-01 10:00", "VC", "WalletA", "VENDITA", "ETH", "Crypto", "-5", "EUR", "FIAT", "17000", "17000");
        riga("2025-06-01 10:00", "VC", "WalletA", "VENDITA", "BTC", "Crypto", "-0.5", "EUR", "FIAT", "35000", "35000");
        Map<String, String[]> nuova = copia();
        String[] idMT1 = new String[2];
        Map<String, String[]> vecchia = formaPiattaforma(nuova, s, idMT1);
        Map<String, String> risNuova = calcola(nuova);
        Map<String, String> risVecchia = calcola(vecchia);
        assertEquals(risNuova.size(), risVecchia.size());
        for (Map.Entry<String, String> e : risNuova.entrySet()) {
            String id = e.getKey();
            assertEquals(e.getValue(), risVecchia.get(id.equals(idMT1[0]) ? idMT1[1] : id), "campi del motore diversi su " + id);
        }
        long conPlus = risNuova.values().stream().filter(x -> !x.split("\\|", -1)[2].equals("0.00") && !x.split("\\|", -1)[2].isBlank()).count();
        assertTrue(conPlus >= 2, "lo scenario deve produrre plusvalenze vere: " + risNuova);
    }

    @Test
    void motore_identicoInTutteLeConfigurazioni() throws Exception {
        for (String exchange : new String[]{"WalletA", "WalletB"}) {
            for (boolean xw : new boolean[]{false, true}) {
                for (boolean sr : new boolean[]{false, true}) {
                    confrontaMotore(exchange, xw, sr);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Giacenze e saldi
    // ------------------------------------------------------------------

    @Test
    void giacenze_ilDaConsegnareCopreIlPeriodoFraDepositoEPrelievoSenzaSaldiNegativi() throws Exception {
        Scenario s = scenarioDaConsegnare();
        assertFalse(Funzioni.ControllaSaldiNegativi().stream().anyMatch(x -> x.contains("Piattaforma") || x.contains("DA_CONSEGNARE")),
                "nessun saldo negativo su piattaforma o DA_CONSEGNARE: " + Funzioni.ControllaSaldiNegativi());
        //Prima del deposito
        assertEquals(0, new BigDecimal("10").compareTo(quantitaAl("Wallet 01", "ETH", "20241227235959")));
        assertEquals(0, BigDecimal.ZERO.compareTo(quantitaAl("Wallet 01", "ETH.DA_CONSEGNARE", "20241227235959")));
        //31/12: 5 ETH comprati il 30/12 e 10 ETH ancora nel wallet come DA_CONSEGNARE, 0,5 BTC incassati
        assertEquals(0, new BigDecimal("5").compareTo(quantitaAl("Wallet 01", "ETH", "20241231235959")));
        assertEquals(0, new BigDecimal("10").compareTo(quantitaAl("Wallet 01", "ETH.DA_CONSEGNARE", "20241231235959")));
        assertEquals(0, new BigDecimal("0.5").compareTo(quantitaAl("Wallet 01", "BTC", "20241231235959")));
        //Dopo il prelievo il DA_CONSEGNARE è uscito
        assertEquals(0, BigDecimal.ZERO.compareTo(quantitaAl("Wallet 01", "ETH.DA_CONSEGNARE", "20250103000000")));
        assertEquals(0, new BigDecimal("5").compareTo(quantitaAl("Wallet 01", "ETH", "20250103000000")));
    }

    @Test
    void giacenze_etherMancanteAlDeposito_ilNegativoEDichiaratoSuEth() throws Exception {
        //Gli ETH si comprano DOPO il deposito: al deposito la pila non li ha. Il saldo negativo compare sull'ETH (non sulla piattaforma)
        riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "0.5", "30000");
        String[] dep = MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_DC")).findFirst().orElseThrow();
        riga("2024-12-29 12:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-30000", "ETH", "Crypto", "10", "30000");
        String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "ETH", "Crypto", "-10", "", "", "", "31000");
        seedPrezzi(DATE_PREZZI);
        Scenario s = new Scenario(); s.deposito = dep; s.prelievo = pre;
        classifica(s);
        List<String> neg = Funzioni.ControllaSaldiNegativi().stream().map(Object::toString).toList();
        assertFalse(neg.stream().anyMatch(x -> x.contains("Piattaforma") || x.contains("DA_CONSEGNARE")), neg.toString());
        assertTrue(neg.stream().anyMatch(x -> x.endsWith(";ETH")), "il negativo è dichiarato sull'ETH: " + neg);
    }

    // ------------------------------------------------------------------
    // Annullamento e ripristino
    // ------------------------------------------------------------------

    @Test
    void annullamento_ripristinaIlTokenEDLeRigheOriginali() throws Exception {
        Scenario s = scenarioNonClassificato("WalletA");
        Map<String, String> prima = fotografia();
        classifica(s);
        assertEquals(prima.size() + 3, MappaCryptoWallet.size());
        String[] parti = (s.prelievo[0] + "," + s.prelievo[20]).split(",");
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(parti, s.prelievo[0]);
        assertEquals(prima, fotografia(), "dopo l'annullamento la mappa è identica a prima, token compreso");
    }

    @Test
    void riabbinamentoDopoAnnullamento_stessoRisultato() throws Exception {
        Scenario s = scenarioDaConsegnare();
        Map<String, String> primo = fotografia();
        String[] parti = (s.prelievo[0] + "," + s.prelievo[20]).split(",");
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(parti, s.prelievo[0]);
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(s.prelievo[0], s.deposito[0]));
        assertEquals(primo, fotografia());
    }

    @Test
    void reimport_ilPrelievoPrimaDellaClassificazioneTornaConLaSuaMoneta() throws Exception {
        Scenario s = scenarioDaConsegnare();
        String[] pre = MappaCryptoWallet.get(s.prelievo[0]);
        String[] copia = GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(pre);
        assertNotNull(copia);
        assertEquals("ETH", copia[8]);
        assertEquals("ETH.DA_CONSEGNARE", pre[8], "la riga dell'archivio non si altera");
        assertEquals(pre[10], copia[10]);
    }

    @Test
    void riparazioneScambiSovrascritti_nonScambiaIlWalletDelPrelievoPerLaPiattaforma() throws Exception {
        Scenario s = scenarioDaConsegnare();
        //Il movimento sintetico d'entrata sta sul wallet del prelievo: non è il nome di una piattaforma
        String[] mt1 = MappaCryptoWallet.get(s.mt1);
        assertTrue(TokenDaConsegnare.Is(mt1[11]));
        assertEquals(MovimentiCollegati.EsitoRiparazione.NON_SERVE,
                MovimentiCollegati.RiparaScambioDifferito(MappaCryptoWallet.get(s.prelievo[0]), MappaCryptoWallet.get(s.deposito[0]), "Piattaforma di scambio"));
    }

    // ------------------------------------------------------------------
    // Trasla Orario
    // ------------------------------------------------------------------

    @Test
    void traslaOrario_sulDeposito_muoveDepositoScambioEdEntrata() throws Exception {
        Scenario s = scenarioDaConsegnare();
        assertEquals(1, Principale_TraslaOrario.EseguiTraslazione(List.of(s.deposito[0]), 3_600_000L));
        long alle13 = MappaCryptoWallet.keySet().stream().filter(k -> k.startsWith("20241228130000")).count();
        assertEquals(4, alle13, "deposito, scambio, uscita ed entrata del token si spostano insieme");
        assertTrue(MappaCryptoWallet.keySet().stream().anyMatch(k -> k.startsWith("20250102120000_00")), "il prelievo non si muove");
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[20].isBlank()) continue;
            for (String id : v[20].split(",")) assertNotNull(MappaCryptoWallet.get(id), "ID citato e inesistente: " + id);
        }
    }

    private static Calcoli_RT.AnalisiPlus calcolaRT() throws Exception {
        //Il RT ricostruisce i lotti da v[16]/v[17], che scrive il motore: va eseguito prima
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

    // ------------------------------------------------------------------
    // Punto 1: valore e plusvalenza latente di fine anno
    // ------------------------------------------------------------------

    @Test
    void prezzo_ilDaConsegnareSiPrezzaComeLaMoneta() throws Exception {
        scenarioDaConsegnare();
        Moneta bloccati = new Moneta();
        bloccati.Moneta = "ETH.DA_CONSEGNARE";
        bloccati.Tipo = "Crypto";
        bloccati.Qta = "10";
        long data = FunzioniDate.ConvertiDatainLongMinuto("2024-12-31 23:00");
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(bloccati, null, data, "", "");
        assertNotNull(ip, "ETH.DA_CONSEGNARE deve avere il prezzo di ETH");
        assertEquals(0, new BigDecimal("3000").compareTo(ip.prezzoUnitario));
        assertEquals("ETH.DA_CONSEGNARE", bloccati.Moneta, "la moneta originale non si altera");
    }

    @Test
    void quadroRT_laLatenteDelDaConsegnareEZeroEIlValoreEntraNelleRimanenze() throws Exception {
        scenarioDaConsegnare();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        BigDecimal[] anno = a.Get_TabellaPlusXAnno().get("2024");
        assertNotNull(anno);
        //Latente 2024: ETH (5 a 3000 contro 15000) 0, BTC (0,5 a 60000 contro il costo riportato 20000) 10000, DA_CONSEGNARE 0
        assertEquals(0, new BigDecimal("10000").compareTo(anno[4].setScale(0, java.math.RoundingMode.HALF_UP)), "latente 2024: " + anno[4]);
        //Valore di fine anno: ETH 15000 + BTC 30000 + DA_CONSEGNARE 30000
        assertEquals(0, new BigDecimal("75000").compareTo(anno[5].setScale(0, java.math.RoundingMode.HALF_UP)), "rimanenze 2024: " + anno[5]);

        Calcoli_RT.PlusXMoneta riga = a.MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get("2024").values().stream()
                .map(m -> m.get("ETH.DA_CONSEGNARE")).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        assertNotNull(riga, "la riga ETH.DA_CONSEGNARE deve restare visibile");
        assertEquals("10", riga.Mon.Qta);
        assertEquals(0, new BigDecimal(riga.PlusLatente).signum(), "latente del DA_CONSEGNARE");
    }

    @Test
    void quadroRT_dopoIlPrelievoIlBloccatiNonCompare() throws Exception {
        scenarioDaConsegnare();
        Calcoli_RT.AnalisiPlus a = calcolaRT();
        Map<String, Calcoli_RT.PlusXMoneta> gruppo = a.MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get("2025").values().iterator().next();
        Calcoli_RT.PlusXMoneta riga = gruppo.get("ETH.DA_CONSEGNARE");
        assertTrue(riga == null || new BigDecimal(riga.Mon.Qta).signum() == 0, "il DA_CONSEGNARE esce al prelievo");
    }

    // ------------------------------------------------------------------
    // Punto 1: Giacenze a data
    // ------------------------------------------------------------------

    @Test
    void giacenzeAData_costoDelDaConsegnareUgualeAlValore() {
        BigDecimal prezzo = new BigDecimal("3000");
        //Con il prezzo: valore esatto, differenza valore - costo zero
        BigDecimal costo = TokenDaConsegnare.CostoRigaGiacenze("ETH.DA_CONSEGNARE", "10", prezzo, BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("30000").compareTo(costo));
        Double[] d = Principale_GiacenzeaData.ValoriDerivatiRiga("Crypto", "10", prezzo, 30000d,
                costo.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue(), costo);
        assertEquals(3000d, d[0]);
        assertEquals(3000d, d[1]);
        assertEquals(0d, d[2]);
        //Senza prezzo: costo zero (la riga resta "Token senza prezzo"), nessuna differenza
        assertEquals(0, BigDecimal.ZERO.compareTo(TokenDaConsegnare.CostoRigaGiacenze("ETH.DA_CONSEGNARE", "10", null, new BigDecimal("5"))));
        //Una moneta qualunque non cambia
        BigDecimal lotti = new BigDecimal("15000");
        assertSame(lotti, TokenDaConsegnare.CostoRigaGiacenze("ETH", "5", prezzo, lotti));
    }

    @Test
    void giacenzeAData_dettaglioConCostoResiduoUgualeAlValore() throws Exception {
        Scenario s = scenarioDaConsegnare();
        String[] mt1 = MappaCryptoWallet.get(s.mt1);
        Principale_GiacenzeaData.CostiDettaglioToken costi =
                new Principale_GiacenzeaData.CostiDettaglioToken("Tutti", "ETH.DA_CONSEGNARE", "Crypto", "", "");
        for (String[] v : new TreeMap<>(MappaCryptoWallet).values()) {
            costi.Avanza(v);
            if (v[0].equals(mt1[0])) break;
        }
        String[] col = Principale_GiacenzeaData.ColonneCostiDettaglio(costi, mt1, true, mt1[13], "10");
        assertFalse(col[2].isEmpty(), "il valore residuo deve esserci: " + Arrays.toString(col));
        assertEquals(col[2], col[3], "costo residuo = valore residuo: " + Arrays.toString(col));
        assertEquals("0.00", col[4], "differenza zero: " + Arrays.toString(col));
        assertFalse(Principale_GiacenzeaData.PrezzoDaCompletare(col));
    }

    // ------------------------------------------------------------------
    // Quadro RW con i lotti LIFO (metodi B/C/D): E-Money in euro, prezzi 1:1 senza rete
    // ------------------------------------------------------------------

    private static List<String> righeRW(String anno) {
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Calcoli_RW.AggiornaRWFR(anno);
        List<String> righe = new java.util.ArrayList<>();
        for (List<String[]> lista : Principale.Mappa_RW_ListeXGruppoWallet.values())
            for (String[] r : lista) righe.add(String.join("|", r));
        return righe;
    }

    @Test
    void quadroRW_metodiConLotti_ilTokenRestaNelQuadroFraDepositoEPrelievo() throws Exception {
        Principale.Mappa_EMoney.put("EURe", "2020-01-01");
        Principale.Mappa_EMoney.put("EURu", "2020-01-01");
        //Deposito sullo stesso gruppo del prelievo e su un gruppo diverso
        for (String walletDeposito : new String[]{"WalletA", "WalletB"}) {
          for (String chiudi : new String[]{"NO", "SI"}) {
           for (String complessivo : new String[]{"NO", "SI"}) {
            for (String rilevanza : new String[]{"B", "C", "D"}) {
                MappaCryptoWallet.clear();
                progressivo = 0;
                DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", rilevanza);
                DatabaseH2.Pers_Opzioni_Scrivi("RW_ChiudiRWsuTrasferimento", chiudi);
                DatabaseH2.Pers_Opzioni_Scrivi("RW_LiFoComplessivo", complessivo);
                riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-10000", "EURe", "Crypto", "10000", "10000");
                String[] dep = riga("2024-12-28 12:00", "DC", walletDeposito, "DEPOSITO CRYPTO", "", "", "", "EURu", "Crypto", "10000", "10000");
                riga("2024-12-30 12:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-5000", "EURe", "Crypto", "5000", "5000");
                String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "EURe", "Crypto", "-10000", "", "", "", "10000");
                assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pre[0], dep[0]));
                String quando = walletDeposito + " rilevanza " + rilevanza + " chiudiSuTrasferimento " + chiudi + " lifoComplessivo " + complessivo;

                //Anno del deposito: il token resta nel quadro dal deposito a fine anno, senza errori
                List<String> r24 = righeRW("2024");
                assertFalse(r24.stream().anyMatch(r -> r.contains("Errore")), "nessun errore 2024 (" + quando + "): " + r24);
                assertTrue(r24.stream().anyMatch(r -> r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-28 12:00|10000.00|")
                        && r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-31 23:59|")), "lotto del 2024 (" + quando + "): " + r24);

                //Anno del prelievo: il lotto va dal 1/1 al prelievo e si chiude lì
                List<String> r25 = righeRW("2025");
                assertFalse(r25.stream().anyMatch(r -> r.contains("Errore")), "nessun errore 2025 (" + quando + "): " + r25);
                assertTrue(r25.stream().anyMatch(r -> r.contains("|EURe.DA_CONSEGNARE|10000|2025-01-01 00:00|10000.00|")
                        && r.contains("|EURe.DA_CONSEGNARE|10000|2025-01-02 12:00|") && r.contains("Consegna dei token ceduti")),
                        "lotto del 2025 (" + quando + "): " + r25);
                //A fine 2025 nessun lotto del token resta aperto
                assertFalse(r25.stream().anyMatch(r -> r.contains("EURe.DA_CONSEGNARE") && r.contains("2025-12-31 23:59")), "(" + quando + "): " + r25);
                //Il token sta sul gruppo del prelievo (Wallet 01), anche se il deposito è su un altro gruppo
                if (complessivo.equals("NO")) {
                    righeRW("2024");
                    assertTrue(Principale.Mappa_RW_ListeXGruppoWallet.getOrDefault("Wallet 01", List.of()).stream()
                            .anyMatch(r -> r[2].equals("EURe.DA_CONSEGNARE")), "token su Wallet 01 (" + quando + ")");
                    assertTrue(Principale.Mappa_RW_ListeXGruppoWallet.getOrDefault("Wallet 02", List.of()).stream()
                            .noneMatch(r -> r[2].equals("EURe.DA_CONSEGNARE")), "token non su Wallet 02 (" + quando + ")");
                }
            }
           }
          }
        }
    }

    @Test
    void quadroRW_confineDiPeriodoFraDepositoEPrelievo_ilLottoSiSpezzaESiRiapre() throws Exception {
        Principale.Mappa_EMoney.put("EURe", "2020-01-01");
        Principale.Mappa_EMoney.put("EURu", "2020-01-01");
        DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", "C");
        riga("2024-06-01 10:00", "AC", "WalletA", "ACQUISTO", "EUR", "FIAT", "-10000", "EURe", "Crypto", "10000", "10000");
        String[] dep = riga("2024-12-28 12:00", "DC", "WalletA", "DEPOSITO CRYPTO", "", "", "", "EURu", "Crypto", "10000", "10000");
        String[] pre = riga("2025-01-02 12:00", "PC", "WalletA", "PRELIEVO CRYPTO", "EURe", "Crypto", "-10000", "", "", "", "10000");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pre[0], dep[0]));
        //Periodi CRYPTO: fino al 29/12 e dal 30/12, quindi il confine cade fra il deposito (28/12) e il prelievo (2/1)
        List<String[]> periodi = new java.util.ArrayList<>();
        for (String[] p : new String[][]{{"1", "", "2024-12-29"}, {"2", "2024-12-30", ""}}) {
            String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
            Arrays.fill(r, "");
            r[Principale_GruppiWalletRW.COL_TIPO] = "CRYPTO";
            r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = p[0];
            r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = p[1];
            r[Principale_GruppiWalletRW.COL_DATA_FINE] = p[2];
            r[Principale_GruppiWalletRW.COL_BOLLO] = "NO";
            r[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = Principale_GruppiWalletRW.MOD_SOLO_RESIDUO;
            r[Principale_GruppiWalletRW.COL_MOD_FINALE] = Principale_GruppiWalletRW.MOD_SOLO_RESIDUO;
            periodi.add(r);
        }
        List<String> errori = Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", periodi);
        assertTrue(errori.isEmpty(), () -> "periodi non salvati: " + errori);

        List<String> r24 = righeRW("2024");
        System.out.println("RW periodi 2024: " + r24);
        assertFalse(r24.stream().anyMatch(r -> r.contains("Errore")), "nessun errore: " + r24);
        long righeToken = r24.stream().filter(r -> r.contains("|EURe.DA_CONSEGNARE|")).count();
        assertEquals(2, righeToken, "un tratto per periodo, mai un lotto aperto due volte: " + r24);
        //Primo periodo: dal deposito al 29/12 alle 23:59. Secondo: dal 30/12 alle 00:00 a fine anno
        assertTrue(r24.stream().anyMatch(r -> r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-28 12:00|")
                && r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-29 23:59|")), r24.toString());
        assertTrue(r24.stream().anyMatch(r -> r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-30 00:00|")
                && r.contains("|EURe.DA_CONSEGNARE|10000|2024-12-31 23:59|")), r24.toString());
        List<String> r25 = righeRW("2025");
        System.out.println("RW periodi 2025: " + r25);
        assertFalse(r25.stream().anyMatch(r -> r.contains("Errore")), "nessun errore 2025: " + r25);
    }

    @Test
    void quadroRW_rilevanzaA_ilTokenCompareAFineAnnoDelDeposito() throws Exception {
        scenarioDaConsegnare();
        Calcoli_RW_Giacenze.PrezzoGiacenza prezzo = (m, istante) ->
                new BigDecimal(m.Qta).multiply(new BigDecimal(TokenDaConsegnare.Base(m.Moneta).equals("BTC") ? "60000" : "3000")).stripTrailingZeros().toPlainString();
        List<String[]> r24 = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno("2024", prezzo).get("Wallet 01");
        String[] rigo = r24.stream().filter(x -> x[2].equals("ETH.DA_CONSEGNARE")).findFirst().orElse(null);
        assertNotNull(rigo, "il token compare a fine 2024");
        assertEquals("10", rigo[8]);
        assertEquals(0, new BigDecimal("30000").compareTo(new BigDecimal(rigo[10])), "valore a fine 2024: " + Arrays.toString(rigo));
        List<String[]> r25 = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno("2025", prezzo).get("Wallet 01");
        assertTrue(r25.stream().noneMatch(x -> x[2].equals("ETH.DA_CONSEGNARE") && new BigDecimal(x[8]).signum() != 0),
                "dopo il prelievo il token non c'è più a fine 2025");
    }

    // ------------------------------------------------------------------
    // TokenDaConsegnare: il punto unico delle eccezioni
    // ------------------------------------------------------------------

    @Test
    void tokenDaConsegnare_riconoscimentoESuffisso() {
        assertTrue(TokenDaConsegnare.Is("ETH.DA_CONSEGNARE"));
        assertFalse(TokenDaConsegnare.Is("ETH"));
        assertFalse(TokenDaConsegnare.Is(".DA_CONSEGNARE"), "il solo suffisso non è un token");
        assertFalse(TokenDaConsegnare.Is(null));
        assertEquals("ETH", TokenDaConsegnare.Base("ETH.DA_CONSEGNARE"));
        assertEquals("ETH", TokenDaConsegnare.Base("ETH"));
        assertEquals("ETH.DA_CONSEGNARE", TokenDaConsegnare.Di("ETH"));
        assertEquals("ETH", TokenDaConsegnare.Base(TokenDaConsegnare.Di("ETH")));
    }

    @Test
    void tokenDaConsegnare_costoDiCaricoESoloQuelloDelValoreSeEIlToken() {
        BigDecimal valore = new BigDecimal("30000"), lotti = new BigDecimal("15000");
        assertSame(valore, TokenDaConsegnare.CostoDiCarico("ETH.DA_CONSEGNARE", valore, lotti));
        assertSame(lotti, TokenDaConsegnare.CostoDiCarico("ETH", valore, lotti));
    }

    @Test
    void tokenDaConsegnare_ramiRW() {
        assertTrue(TokenDaConsegnare.IsIngressoRW("DTW - Trasferimento Interno", "ETH.DA_CONSEGNARE"));
        assertFalse(TokenDaConsegnare.IsIngressoRW("DTW - Trasferimento Interno", "ETH"));
        assertFalse(TokenDaConsegnare.IsIngressoRW("DTW - Scambio Differito", "ETH.DA_CONSEGNARE"));
        assertTrue(TokenDaConsegnare.IsConsegnaRW("PTW - Scambio Differito", "ETH.DA_CONSEGNARE"));
        assertFalse(TokenDaConsegnare.IsConsegnaRW("PTW - Scambio Differito", "ETH"));
        assertFalse(TokenDaConsegnare.IsConsegnaRW("PTW - Trasferimento tra Wallet", "ETH.DA_CONSEGNARE"));
        assertFalse(TokenDaConsegnare.IsConsegnaRW(null, "ETH.DA_CONSEGNARE"));
    }
}
