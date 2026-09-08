package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip delle tre tabelle di configurazione del quadro RW dei gruppi wallet
 * introdotte in Fase 1 (2026-08-30): {@code EXCHANGE_ANAGRAFICA},
 * {@code GRUPPO_RIFERIMENTO_ESTERO}, {@code GRUPPO_PERIODO_RW}.
 *
 * <p>Su H2 temporaneo, come {@link DatabaseH2UpsertTest}. Il punto delicato coperto:
 * {@code GRUPPO_PERIODO_RW} ha una chiave PRIMARIA SINTETICA ({@code Gruppo_Tipo_Prog})
 * perché {@code U_ScriviRecord} accetta una sola {@code primaryKeyColumn}; progressivi
 * diversi dello stesso gruppo/tipo NON si devono sovrascrivere a vicenda, e cancellare
 * un singolo periodo o un intero gruppo deve colpire esattamente le righe giuste.</p>
 *
 * <p>Nessuna delle tre tabelle è ancora letta da {@code Calcoli_RW.AggiornaRWFR}: che il
 * calcolo RW resti invariato lo verifica {@link Calcoli_RW_GoldenMasterTest}, non questo.</p>
 */
class DatabaseH2_RwConfigTest {

    @TempDir
    static Path tempDir;

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
    void puliscePartenza() {
        for (String id : new TreeSet<>(DatabaseH2.Pers_ExchangeAnagrafica_LeggiTabella().keySet())) {
            DatabaseH2.Pers_ExchangeAnagrafica_Cancella(id); // cancella a cascata anche i periodi
        }
        for (String id : new TreeSet<>(DatabaseH2.Pers_ExchangePeriodo_LeggiTabella().keySet())) {
            DatabaseH2.Pers_ExchangePeriodo_CancellaExchange(id); // periodi rimasti orfani
        }
        for (String g : new TreeSet<>(DatabaseH2.Pers_GruppoRiferimento_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoRiferimento_Cancella(g);
        }
        for (String g : new TreeSet<>(DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo(g);
        }
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_ANAGRAFICA_SEED", "");
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_SEED_FISCALE", "");
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");
        DatabaseH2.Pers_Opzioni_Scrivi("GRUPPO_WALLET_PRECONF_SEED", "");
    }

    // ------------------------------------------------------------------
    //  DDL — le tabelle esistono con le colonne attese
    // ------------------------------------------------------------------

    @Test
    void ddl_treTabelleCreateConLeColonneAttese() throws Exception {
        assertEquals(
                Set.of("EXCHANGEID", "NOME", "STATOESTERO", "IDENTIFICATIVOFISCALE", "NOTE", "FONTE", "DATAAGGIORNAMENTO", "ORIGINE"),
                colonne("EXCHANGE_ANAGRAFICA"));
        assertEquals(
                Set.of("GRUPPO", "MODALITA", "STATOESTERO", "IDENTIFICATIVOFISCALE", "EXCHANGEID"),
                colonne("GRUPPO_RIFERIMENTO_ESTERO"));
        assertEquals(
                Set.of("GRUPPO_TIPO_PROG", "GRUPPO", "TIPORIGO", "PROGRESSIVO", "DATAINIZIO", "DATAFINE",
                        "VALOREINIZIALEMANUALE", "NOTAVALOREINIZIALE", "VALOREFINALEMANUALE", "NOTAVALOREFINALE",
                        "MODALITACALCOLOINIZIALE", "MODALITACALCOLOFINALE", "PAGABOLLOPERIODO", "ORIGINE", "CHIAVEDEFAULT"),
                colonne("GRUPPO_PERIODO_RW"));
        assertEquals(
                Set.of("EXCHANGEID_PROGRESSIVO", "EXCHANGEID", "PROGRESSIVO", "DATAINIZIO", "DATAFINE",
                        "NOME", "STATOESTERO", "IDENTIFICATIVOFISCALE", "NOTE", "FONTE", "ORIGINE", "CHIAVEDEFAULT"),
                colonne("EXCHANGE_PERIODO"));
    }

    private static Set<String> colonne(String tabella) throws Exception {
        Set<String> out = new TreeSet<>();
        try (var ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?")) {
            ps.setString(1, tabella);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1).toUpperCase());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    //  EXCHANGE_ANAGRAFICA
    // ------------------------------------------------------------------

    @Test
    void exchangeAnagrafica_roundTrip_tutti7Campi() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("kraken", "Kraken", "092", "LU12345678",
                "sede spostata in LU", "https://esempio/fonte", "2026-08-31");

        String[] r = DatabaseH2.Pers_ExchangeAnagrafica_Leggi("kraken");
        assertArrayEquals(new String[]{"kraken", "Kraken", "092", "LU12345678",
                "sede spostata in LU", "https://esempio/fonte", "2026-08-31", null}, r);
    }

    @Test
    void exchangeAnagrafica_leggiAssente_ritornaArrayDiNull() {
        assertArrayEquals(new String[8], DatabaseH2.Pers_ExchangeAnagrafica_Leggi("inesistente"));
    }

    @Test
    void exchangeAnagrafica_scriviNome_nonToccaLeColonneFiscaliLegacy() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("kraken", "Kraken", "040", "IE111",
                "nota legacy", "fonte legacy", "2026-01-01");

        DatabaseH2.Pers_ExchangeAnagrafica_ScriviNome("kraken", "Kraken (rinominato)");

        assertArrayEquals(new String[]{"kraken", "Kraken (rinominato)", "040", "IE111",
                "nota legacy", "fonte legacy", "2026-01-01", null},
                DatabaseH2.Pers_ExchangeAnagrafica_Leggi("kraken"));
    }

    @Test
    void exchangeAnagrafica_scriviNome_creaLaRigaSeAssente() {
        DatabaseH2.Pers_ExchangeAnagrafica_ScriviNome("nuovo", "Nuovo Exchange");

        String[] r = DatabaseH2.Pers_ExchangeAnagrafica_Leggi("nuovo");
        assertEquals("nuovo", r[0]);
        assertEquals("Nuovo Exchange", r[1]);
        assertNull(r[2]);
    }

    @Test
    void exchangeAnagrafica_riscritturaStessaChiave_aggiornaSenzaDuplicare() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("okx", "OKX", "111", "AAA", null, null, null);
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("okx", "OKX Europe", "092", "BBB", "nota", "fonte", "2026-08-31");

        assertEquals(1, DatabaseH2.Pers_ExchangeAnagrafica_LeggiTabella().size());
        String[] r = DatabaseH2.Pers_ExchangeAnagrafica_Leggi("okx");
        assertEquals("OKX Europe", r[1]);
        assertEquals("092", r[2]);
        assertEquals("BBB", r[3]);
        assertEquals("nota", r[4]);
    }

    @Test
    void exchangeAnagrafica_seed_elencoNomiSenzaDatiFiscali() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();

        Map<String, String[]> tab = DatabaseH2.Pers_ExchangeAnagrafica_LeggiTabella();
        assertTrue(tab.containsKey("binance"), "il seed deve includere gli exchange noti");
        assertTrue(tab.size() >= 10);
        for (String[] r : tab.values()) {
            assertNotNull(r[1], "il nome visualizzato è previsto nel seed");
            assertNull(r[2], "il seed non deve inventare lo stato estero (" + r[0] + ")");
            assertNull(r[3], "il seed non deve inventare l'identificativo fiscale (" + r[0] + ")");
        }
        assertEquals("SI", DatabaseH2.Pers_Opzioni_Leggi("EXCHANGE_ANAGRAFICA_SEED"));
    }

    @Test
    void exchangeAnagrafica_seed_nonRicreaUnElementoCancellatoDallUtente() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangeAnagrafica_Cancella("binance");

        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi(); // seconda passata: guardata dall'opzione

        assertNull(DatabaseH2.Pers_ExchangeAnagrafica_Leggi("binance")[0],
                "un exchange cancellato dall'utente non deve ricomparire al riavvio");
    }

    // ------------------------------------------------------------------
    //  EXCHANGE_PERIODO — periodi fiscali dell'exchange
    // ------------------------------------------------------------------

    @Test
    void exchangePeriodo_roundTrip_tuttiICampi() {
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("coinbase", 2, "2025-06-20", null,
                "Coinbase Luxembourg S.A.", "092", "LU36476644", "nota", "VIES");

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("coinbase");
        assertEquals(1, righe.size());
        assertArrayEquals(new String[]{
                "coinbase_2", "coinbase", "2", "2025-06-20", null,
                "Coinbase Luxembourg S.A.", "092", "LU36476644", "nota", "VIES", null, null}, righe.get(0));
    }

    @Test
    void exchangePeriodo_progressiviDiversi_nonSiSovrascrivono_ordinatiPerProgressivo() {
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("kraken", 2, "2021-01-01", null,
                "Payward Europe Solutions Ltd", "040", "711781", null, null);
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("kraken", 1, "", "2021-01-01",
                "Payward Ltd", "031", null, null, null);

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("kraken");
        assertEquals(2, righe.size());
        assertEquals("kraken_1", righe.get(0)[0]);
        assertEquals("031", righe.get(0)[6]);
        assertEquals("kraken_2", righe.get(1)[0]);
        assertEquals("040", righe.get(1)[6]);
    }

    @Test
    void exchangePeriodo_cancellaExchange_rimuoveSoloQuello() {
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("okx", 1, null, null, "OKX Europe Ltd", "105", "C88193", null, null);
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("bybit", 1, null, null, "Bybit EU GmbH", "008", "636180i", null, null);

        DatabaseH2.Pers_ExchangePeriodo_CancellaExchange("okx");

        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("okx").isEmpty());
        assertEquals(1, DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("bybit").size());
        assertFalse(DatabaseH2.Pers_ExchangePeriodo_HaPeriodi("okx"));
        assertTrue(DatabaseH2.Pers_ExchangePeriodo_HaPeriodi("bybit"));
    }

    @Test
    void exchangeAnagrafica_cancella_cancellaACascataIPeriodi() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("okx", "OKX", null, null, null, null, null);
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("okx", 1, null, null, "OKX Europe Ltd", "105", "C88193", null, null);

        DatabaseH2.Pers_ExchangeAnagrafica_Cancella("okx");

        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("okx").isEmpty());
    }

    @Test
    void seedPeriodiFiscali_creaUnPeriodoCorrentePerLeEntitaMiCA() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali();

        List<String[]> cb = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("coinbase");
        assertEquals(1, cb.size());
        assertEquals("1", cb.get(0)[2]);
        assertEquals("2025-06-20", cb.get(0)[3]);   // data di autorizzazione CASP (default indicativo)
        assertNull(cb.get(0)[4]);                   // periodo ancora in corso
        assertEquals("092", cb.get(0)[6]);
        assertEquals("LU36476644", cb.get(0)[7]);   // P.IVA verificata su VIES
        assertNotNull(cb.get(0)[9], "la fonte va valorizzata");

        assertEquals("ATU78580117", DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("bitpanda").get(0)[7]);
        assertEquals("C88392", DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("cryptocom").get(0)[7]); // ripiego reg. imprese

        // identificativo non reperito -> resta vuoto, ma stato e nota ci sono
        String[] gm = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("gemini").get(0);
        assertEquals("105", gm[6]);
        assertNull(gm[7]);
        assertNotNull(gm[8]);

        // exchange senza entità UE MiCA : nessun periodo
        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("binance").isEmpty());
        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("bitfinex").isEmpty());

        assertEquals("2026-09-06", DatabaseH2.Pers_Opzioni_Leggi("EXCHANGE_PERIODO_SEED_FISCALE"));
    }

    @Test
    void seedPeriodiFiscali_nonToccaExchangeConPeriodiGiaPresenti() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("kraken", 1, "2019-01-01", null, "Kraken (mio)", "999", "MIO", null, null);

        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali();

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("kraken");
        assertEquals(1, righe.size());
        assertEquals("999", righe.get(0)[6]);
        assertEquals("MIO", righe.get(0)[7]);
    }

    @Test
    void seedPeriodiFiscali_saltaExchangeCancellatoDallAnagrafica() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangeAnagrafica_Cancella("coinbase");

        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali();

        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("coinbase").isEmpty());
    }

    @Test
    void seedPeriodiFiscali_secondaPassata_noOp() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali();
        int prima = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("okx").size();

        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali(); // guardata dall'opzione versionata

        assertEquals(prima, DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("okx").size());
    }

    @Test
    void travaso_riversaDatiAnagraficaLegacyInUnPrimoPeriodoConDateVuote() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("kraken", "Kraken", "040", "IE111",
                "sede in IE", "fonte utente", "2026-01-15");
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");

        DatabaseH2.Pers_ExchangePeriodo_TravasoDaAnagrafica();

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("kraken");
        assertEquals(1, righe.size());
        assertEquals("1", righe.get(0)[2]);
        assertNull(righe.get(0)[3], "il travaso non inventa una data di inizio");
        assertNull(righe.get(0)[4]);
        assertEquals("040", righe.get(0)[6]);
        assertEquals("IE111", righe.get(0)[7]);
        assertTrue(righe.get(0)[8].contains("2026-01-15"), "la DataAggiornamento legacy finisce nelle note");
        assertEquals("SI", DatabaseH2.Pers_Opzioni_Leggi("EXCHANGE_PERIODO_TRAVASO"));
    }

    @Test
    void travaso_nonRiguardaExchangeSenzaDatiFiscaliLegacy() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("binance", "Binance", null, null, null, null, null);
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");

        DatabaseH2.Pers_ExchangePeriodo_TravasoDaAnagrafica();

        assertTrue(DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("binance").isEmpty());
    }

    @Test
    void travaso_secondaPassata_nonDuplica_eNonToccaPeriodiCorrettiAMano() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("okx", "OKX", "105", "C88193", null, null, null);
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");

        DatabaseH2.Pers_ExchangePeriodo_TravasoDaAnagrafica();
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("okx", 1, "2025-01-27", null,
                "OKX Europe Limited", "105", "C88193", "corretto a mano", "MFSA");
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");

        DatabaseH2.Pers_ExchangePeriodo_TravasoDaAnagrafica();

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("okx");
        assertEquals(1, righe.size(), "un secondo travaso non deve duplicare");
        assertEquals("corretto a mano", righe.get(0)[8], "il periodo corretto a mano non va sovrascritto");
        assertEquals("2025-01-27", righe.get(0)[3]);
    }

    @Test
    void travaso_poiSeed_ilSeedSaltaGliExchangeGiaTravasati() {
        DatabaseH2.Pers_ExchangeAnagrafica_SeedElencoNomi();
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("coinbase", "Coinbase", "040", "IE-OLD", "vecchia sede IE", null, null);
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_PERIODO_TRAVASO", "");

        DatabaseH2.Pers_ExchangePeriodo_TravasoDaAnagrafica();
        DatabaseH2.Pers_ExchangePeriodo_SeedDatiFiscali();

        List<String[]> righe = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange("coinbase");
        assertEquals(1, righe.size(), "il seed non aggiunge un periodo se il travaso ne ha già creato uno");
        assertEquals("040", righe.get(0)[6], "resta il dato travasato, non quello del seed (092)");
    }

    // ------------------------------------------------------------------
    //  Gruppi wallet preconfigurati (Wallet 101..)
    // ------------------------------------------------------------------

    @Test
    void seedPreconfigurati_creaGruppo101ERiferimentoAllExchange() {
        DatabaseH2.Pers_GruppoRiferimento_Cancella("Wallet 101");
        DatabaseH2.Pers_GruppoWallet_SeedPreconfigurati();

        assertEquals("Binance", DatabaseH2.Pers_GruppoAlias_Leggi("Wallet 101")[1]);
        assertArrayEquals(new String[]{"Wallet 101", "EXCHANGE", null, null, "binance"},
                DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 101"));
        // ultimo della lista EXCHANGE_NOTI (14 elementi) -> Wallet 114
        assertEquals("Bitfinex", DatabaseH2.Pers_GruppoAlias_Leggi("Wallet 114")[1]);
        assertEquals("bitfinex", DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 114")[4]);
        assertEquals("SI", DatabaseH2.Pers_Opzioni_Leggi("GRUPPO_WALLET_PRECONF_SEED"));
    }

    @Test
    void seedPreconfigurati_nonSovrascriveUnRiferimentoModificatoDallUtente() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 102", "STATO", "064", "MIO", null);
        DatabaseH2.Pers_Opzioni_Scrivi("GRUPPO_WALLET_PRECONF_SEED", "");

        DatabaseH2.Pers_GruppoWallet_SeedPreconfigurati();

        assertArrayEquals(new String[]{"Wallet 102", "STATO", "064", "MIO", null},
                DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 102"));
    }

    // ------------------------------------------------------------------
    //  GRUPPO_RIFERIMENTO_ESTERO
    // ------------------------------------------------------------------

    @Test
    void gruppoRiferimento_roundTrip_modalitaStatoEModalitaExchange() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 01", "STATO", "092", "LU99999999", null);
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 02", "EXCHANGE", null, null, "binance");

        assertArrayEquals(new String[]{"Wallet 01", "STATO", "092", "LU99999999", null},
                DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 01"));
        assertArrayEquals(new String[]{"Wallet 02", "EXCHANGE", null, null, "binance"},
                DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 02"));
        assertEquals(2, DatabaseH2.Pers_GruppoRiferimento_LeggiTabella().size());
    }

    @Test
    void gruppoRiferimento_leggiAssente_ritornaArrayDiNull() {
        assertArrayEquals(new String[5], DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 40"));
    }

    @Test
    void gruppoRiferimento_riscrittura_aggiornaInPlace() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 03", "STATO", "040", null, null);
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 03", "EXCHANGE", null, null, "coinbase");

        assertEquals(1, DatabaseH2.Pers_GruppoRiferimento_LeggiTabella().size());
        assertArrayEquals(new String[]{"Wallet 03", "EXCHANGE", null, null, "coinbase"},
                DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 03"));
    }

    // ------------------------------------------------------------------
    //  GRUPPO_PERIODO_RW — chiave sintetica Gruppo_Tipo_Prog
    // ------------------------------------------------------------------

    @Test
    void gruppoPeriodoRW_roundTrip_trediciCampi() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 05", "FIAT", 1,
                "2024-01-01", "2024-06-30", "1500.00", "saldo da estratto conto",
                "0.00", "conto svuotato", "PRIMO_APPORTO", "ULTIMA_USCITA", "SI");

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 05");
        assertEquals(1, righe.size());
        assertArrayEquals(new String[]{
                "Wallet 05_FIAT_1", "Wallet 05", "FIAT", "1", "2024-01-01", "2024-06-30",
                "1500.00", "saldo da estratto conto", "0.00", "conto svuotato",
                "PRIMO_APPORTO", "ULTIMA_USCITA", "SI", null, null}, righe.get(0));
    }

    @Test
    void gruppoPeriodoRW_progressiviDiversi_nonSiSovrascrivono() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "CRYPTO", 1, "2023-01-01", "2023-05-31",
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "CRYPTO", 2, "2023-09-01", null,
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "FIAT", 1, "2023-01-01", null,
                null, null, null, null, null, null, null);

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 06");
        assertEquals(3, righe.size());
        // ordinamento: TipoRigo poi Progressivo
        assertEquals("Wallet 06_CRYPTO_1", righe.get(0)[0]);
        assertEquals("Wallet 06_CRYPTO_2", righe.get(1)[0]);
        assertEquals("Wallet 06_FIAT_1", righe.get(2)[0]);
        assertEquals("2023-05-31", righe.get(0)[5]);
        assertEquals("2023-09-01", righe.get(1)[4]);
    }

    @Test
    void gruppoPeriodoRW_stessoProgressivo_aggiornaInPlace() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 07", "FIAT", 1, "2024-01-01", null,
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 07", "FIAT", 1, "2024-02-15", "2024-12-31",
                "10.00", "nota", null, null, "SOMMA_APPORTI_GIORNO", "SOMMA_USCITE_GIORNO", "NO");

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 07");
        assertEquals(1, righe.size());
        assertEquals("2024-02-15", righe.get(0)[4]);
        assertEquals("2024-12-31", righe.get(0)[5]);
        assertEquals("10.00", righe.get(0)[6]);
        assertEquals("SOMMA_APPORTI_GIORNO", righe.get(0)[10]);
    }

    @Test
    void gruppoPeriodoRW_cancellaSingolo_lasciaGliAltri() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 08", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 08", "CRYPTO", 2, "2022-07-01", null, null, null, null, null, null, null, null);

        DatabaseH2.Pers_GruppoPeriodoRW_Cancella("Wallet 08", "CRYPTO", 1);

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 08");
        assertEquals(1, righe.size());
        assertEquals("Wallet 08_CRYPTO_2", righe.get(0)[0]);
    }

    @Test
    void gruppoPeriodoRW_cancellaGruppo_rimuoveSoloQuelGruppo() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 09", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 09", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 10", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);

        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo("Wallet 09");

        assertTrue(DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 09").isEmpty());
        assertEquals(1, DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 10").size());
    }

    @Test
    void gruppoPeriodoRW_leggiTabella_raggruppaPerGruppo() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 11", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 11", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 12", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);

        Map<String, List<String[]>> tab = DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella();
        assertEquals(2, tab.size());
        assertEquals(2, tab.get("Wallet 11").size());
        assertEquals(1, tab.get("Wallet 12").size());
    }
}
