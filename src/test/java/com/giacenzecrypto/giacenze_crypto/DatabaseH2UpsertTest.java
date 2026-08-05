package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di CARATTERIZZAZIONE delle scritture "inserisci o aggiorna" di {@link DatabaseH2}
 * su un database H2 temporaneo (voce B5 di Documentazione/Analisi_Bug_Criticita.md).
 *
 * Sono stati scritti **prima** della migrazione da SELECT COUNT + UPDATE/INSERT a
 * {@code MERGE INTO} tramite {@link DatabaseH2#U_ScriviRecord}, e fatti passare sul codice
 * pre-migrazione: servono a dimostrare che la migrazione non cambia il comportamento.
 * Nessun altro test della suite tocca queste due tabelle, quindi senza di essi una
 * regressione sulle credenziali API o sui token EMoney non verrebbe rilevata.
 *
 * Il rischio specifico coperto è quello introdotto dalla correzione M5: {@code EXCHANGEAPI}
 * ha cinque colonne, di cui {@code Opzionale} (la passphrase di OKX) e {@code Nome}, che
 * riceve lo stesso valore di {@code Exchange}. In un {@code MERGE INTO t (colonne) KEY(pk)}
 * le colonne non elencate vengono azzerate sul percorso di inserimento: dimenticarne una
 * cancellerebbe silenziosamente la passphrase, annullando M5.
 */
class DatabaseH2UpsertTest {

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
        for (String exchange : DatabaseH2.Pers_ExchangeApi_LeggiTabella().keySet()) {
            DatabaseH2.Pers_ExchangeApi_Cancella(exchange);
        }
        Principale.Mappa_EMoney.clear();
    }

    // ------------------------------------------------------------------
    // EXCHANGEAPI — 5 colonne, chiave Nome, che riceve il valore di Exchange
    // ------------------------------------------------------------------

    @Test
    void exchangeApi_scritturaNuova_rileggeTuttiECinqueICampi() {
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "chiave1", "segreto1", "passphrase1");

        String[] letto = DatabaseH2.Pers_ExchangeApi_Leggi("OKX");
        assertEquals("OKX", letto[0]);          // Nome (chiave primaria)
        assertEquals("OKX", letto[1]);          // Exchange, stesso valore di Nome
        assertEquals("chiave1", letto[2]);
        assertEquals("segreto1", letto[3]);
        assertEquals("passphrase1", letto[4]);  // Opzionale: la passphrase di M5
    }

    @Test
    void exchangeApi_riscritturaStessaChiave_aggiornaSenzaDuplicare() {
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "chiave1", "segreto1", "passphrase1");
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "chiave2", "segreto2", "passphrase2");

        assertEquals(1, DatabaseH2.Pers_ExchangeApi_LeggiTabella().size(),
                "la riscrittura sulla stessa chiave deve aggiornare, non aggiungere una riga");
        String[] letto = DatabaseH2.Pers_ExchangeApi_Leggi("OKX");
        assertEquals("chiave2", letto[2]);
        assertEquals("segreto2", letto[3]);
        assertEquals("passphrase2", letto[4]);
    }

    @Test
    void exchangeApi_esistenteRiscrittoConPassphraseDiversa_nonLaPerde() {
        // Percorso di UPDATE: è quello in cui una colonna dimenticata azzererebbe la passphrase
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "chiave1", "segreto1", "passphrase1");
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "chiave1", "segreto1", "passphraseNuova");

        assertEquals("passphraseNuova", DatabaseH2.Pers_ExchangeApi_Leggi("OKX")[4]);
    }

    @Test
    void exchangeApi_overloadSenzaOpzionale_scriveStringaVuotaNonNull() {
        DatabaseH2.Pers_ExchangeApi_Scrivi("Binance", "chiave", "segreto");

        assertEquals("", DatabaseH2.Pers_ExchangeApi_Leggi("Binance")[4],
                "gli exchange senza passphrase devono avere Opzionale vuoto, mai null");
    }

    @Test
    void exchangeApi_opzionaleNull_normalizzatoAStringaVuota() {
        DatabaseH2.Pers_ExchangeApi_Scrivi("Kraken", "chiave", "segreto", null);

        assertEquals("", DatabaseH2.Pers_ExchangeApi_Leggi("Kraken")[4]);
    }

    @Test
    void exchangeApi_piuExchangeDistinti_convivonoSenzaSovrascriversi() {
        DatabaseH2.Pers_ExchangeApi_Scrivi("OKX", "k1", "s1", "p1");
        DatabaseH2.Pers_ExchangeApi_Scrivi("Binance", "k2", "s2", "");

        assertEquals(2, DatabaseH2.Pers_ExchangeApi_LeggiTabella().size());
        assertEquals("p1", DatabaseH2.Pers_ExchangeApi_Leggi("OKX")[4]);
        assertEquals("", DatabaseH2.Pers_ExchangeApi_Leggi("Binance")[4]);
    }

    // ------------------------------------------------------------------
    // EMONEY — 2 colonne, chiave Moneta, più l'allineamento della mappa in memoria
    // ------------------------------------------------------------------

    @Test
    void emoney_scritturaNuova_finisceSuDbEMappaInMemoria() {
        DatabaseH2.Pers_Emoney_Scrivi("EURC", "2024-01-01");

        assertEquals("2024-01-01", DatabaseH2.Pers_Emoney_Leggi("EURC"));
        assertEquals("2024-01-01", Principale.Mappa_EMoney.get("EURC"),
                "la mappa in memoria è la base delle ricerche e va tenuta allineata al DB");
    }

    @Test
    void emoney_riscritturaStessaMoneta_aggiornaLaDataSenzaDuplicare() {
        DatabaseH2.Pers_Emoney_Scrivi("EURC", "2024-01-01");
        DatabaseH2.Pers_Emoney_Scrivi("EURC", "2025-06-30");

        assertEquals("2025-06-30", DatabaseH2.Pers_Emoney_Leggi("EURC"));
        assertEquals("2025-06-30", Principale.Mappa_EMoney.get("EURC"));

        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();
        assertEquals(1, Principale.Mappa_EMoney.size(),
                "la riscrittura deve aggiornare la riga esistente, non aggiungerne una seconda");
    }

    @Test
    void emoney_monetaNullaOVuota_restaUnErroreDiProgrammazione() {
        // Comportamento preesistente da non perdere nella migrazione: la validazione degli
        // argomenti avviene prima di toccare il database e solleva IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.Pers_Emoney_Scrivi("", "2024-01-01"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.Pers_Emoney_Scrivi(null, "2024-01-01"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.Pers_Emoney_Scrivi("EURC", null));
    }

    // ------------------------------------------------------------------
    // RINOMINATOKEN — l'UPDATE parziale è intenzionale: VecchioNome non si tocca
    // ------------------------------------------------------------------

    @Test
    void rinominaToken_scritturaNuova_salvaVecchioENuovoNome() {
        DatabaseH2.RinominaToken_Scrivi("0xAAA_ETH", "PIPPO", "PIPPO **");

        String[] nomi = DatabaseH2.RinominaToken_Leggi("0xAAA_ETH");
        assertEquals("PIPPO", nomi[0]);
        assertEquals("PIPPO **", nomi[1]);
    }

    /**
     * Il test che protegge il rischio principale della migrazione di questo metodo: l'UPDATE
     * scrive <b>solo</b> {@code NuovoNome}, quindi il nome originale del token sopravvive anche
     * se il chiamante ne passa uno diverso. È il nome a cui viene aggiunto il suffisso " **"
     * della marcatura SCAM: perderlo sarebbe definitivo e irrecuperabile.
     */
    @Test
    void rinominaToken_riscritturaConVecchioNomeDiverso_conservaQuelloOriginale() {
        DatabaseH2.RinominaToken_Scrivi("0xBBB_ETH", "ORIGINALE", "ORIGINALE **");
        DatabaseH2.RinominaToken_Scrivi("0xBBB_ETH", "ORIGINALE **", "ORIGINALE ****");

        String[] nomi = DatabaseH2.RinominaToken_Leggi("0xBBB_ETH");
        assertEquals("ORIGINALE", nomi[0],
                "VecchioNome è il nome originale del token e non deve mai essere sovrascritto");
        assertEquals("ORIGINALE ****", nomi[1]);
    }

    @Test
    void rinominaToken_riscrittura_nonDuplicaLaRiga() {
        DatabaseH2.RinominaToken_Scrivi("0xCCC_ETH", "TOK", "TOK **");
        DatabaseH2.RinominaToken_Scrivi("0xCCC_ETH", "TOK", "TOK ***");

        long righe = DatabaseH2.RinominaToken_LeggiTabella().keySet().stream()
                .filter(k -> k.equalsIgnoreCase("0xCCC_ETH")).count();
        assertEquals(1, righe);
    }

    @Test
    void rinominaToken_argomentiNulliOVuoti_restanoErroriDiProgrammazione() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.RinominaToken_Scrivi("", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.RinominaToken_Scrivi("k", "", "b"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseH2.RinominaToken_Scrivi("k", "a", ""));
    }

    // ------------------------------------------------------------------
    // GOPLUSSECURITY — 20 colonne: è la tabella dove una colonna dimenticata costa di più
    // ------------------------------------------------------------------

    /** I campi di dettaglio sono nell'ordine con cui la scrittura li elenca. */
    private static final String[] CAMPI_GOPLUS = {
        "is_honeypot", "is_blacklisted", "cannot_sell_all", "is_true_token", "is_airdrop_scam",
        "trust_list", "sell_tax", "mintable", "freezable", "closable",
        "balance_mutable_authority", "trusted_token", "fake_token", "holder_count",
        "dex_count", "is_open_source"};

    /**
     * Un valore diverso per ogni colonna, così uno scambio di colonne verrebbe rilevato, ma
     * lungo al massimo 4 caratteri: quasi tutte le colonne della tabella sono {@code VARCHAR(5)}
     * e un valore più lungo farebbe fallire la scrittura con un errore solo loggato.
     */
    private static java.util.Map<String, String> valoriGoPlus(String marcatore) {
        java.util.Map<String, String> v = new java.util.HashMap<>();
        for (int i = 0; i < CAMPI_GOPLUS.length; i++) {
            v.put(CAMPI_GOPLUS[i], String.format("%02d", i) + marcatore);
        }
        return v;
    }

    @Test
    void goPlus_scritturaNuova_rileggeTutteESediciLeColonneDiDettaglio() {
        DatabaseH2.GoPlusSecurity_Scrivi("0xD1_ETH", "ETH", "0xD1", valoriGoPlus("v1"));

        java.util.Map<String, String> letto = DatabaseH2.GoPlusSecurity_Leggi("0xD1_ETH");
        assertNotNull(letto);
        assertEquals(16, letto.size());
        for (java.util.Map.Entry<String, String> atteso : valoriGoPlus("v1").entrySet()) {
            assertEquals(atteso.getValue(), letto.get(atteso.getKey()),
                    "colonna persa o azzerata: " + atteso.getKey());
        }
    }

    @Test
    void goPlus_riscrittura_aggiornaTutteLeColonneSenzaDuplicare() {
        DatabaseH2.GoPlusSecurity_Scrivi("0xD2_ETH", "ETH", "0xD2", valoriGoPlus("v1"));
        DatabaseH2.GoPlusSecurity_Scrivi("0xD2_ETH", "ETH", "0xD2", valoriGoPlus("v2"));

        java.util.Map<String, String> letto = DatabaseH2.GoPlusSecurity_Leggi("0xD2_ETH");
        for (java.util.Map.Entry<String, String> atteso : valoriGoPlus("v2").entrySet()) {
            assertEquals(atteso.getValue(), letto.get(atteso.getKey()),
                    "colonna non aggiornata: " + atteso.getKey());
        }
    }

    @Test
    void goPlus_campoAssenteDaiValori_restaNullSenzaFarSaltareLaScrittura() {
        // L'API GoPlusLabs non restituisce tutti i campi su tutte le reti: valori.get() torna null
        java.util.Map<String, String> parziali = valoriGoPlus("v1");
        parziali.remove("holder_count");

        DatabaseH2.GoPlusSecurity_Scrivi("0xD3_SOL", "SOL", "0xD3", parziali);

        java.util.Map<String, String> letto = DatabaseH2.GoPlusSecurity_Leggi("0xD3_SOL");
        assertNotNull(letto);
        assertNull(letto.get("holder_count"));
        assertEquals("00v1", letto.get("is_honeypot"));
    }

    // ------------------------------------------------------------------
    // GIACENZEBLOCKCHAIN e TOKENSOLANA
    // ------------------------------------------------------------------

    @Test
    void giacenzeBlockchain_scritturaERiscrittura() {
        DatabaseH2.GiacenzeWalletMonetaBlockchain_Scrivi("0xW1_CRO_CRO_100", "12.5");
        assertEquals("12.5", DatabaseH2.GiacenzeWalletMonetaBlockchain_Leggi("0xW1_CRO_CRO_100"));

        DatabaseH2.GiacenzeWalletMonetaBlockchain_Scrivi("0xW1_CRO_CRO_100", "99.75");
        assertEquals("99.75", DatabaseH2.GiacenzeWalletMonetaBlockchain_Leggi("0xW1_CRO_CRO_100"));
    }

    @Test
    void tokenSolana_scritturaNuova_rileggeSimboloNomeTipo() {
        DatabaseH2.TokenSolana_AggiungiToken("MintAddr1", "SOL1", "Token Uno", "fungible");

        String[] letto = DatabaseH2.TokenSolana_Leggi("MintAddr1");
        assertEquals("SOL1", letto[0]);
        assertEquals("Token Uno", letto[1]);
        assertEquals("fungible", letto[2]);
    }

    @Test
    void tokenSolana_riscrittura_aggiornaTuttiETreICampi() {
        DatabaseH2.TokenSolana_AggiungiToken("MintAddr2", "VEC", "Nome Vecchio", "tipoA");
        DatabaseH2.TokenSolana_AggiungiToken("MintAddr2", "NUO", "Nome Nuovo", "tipoB");

        String[] letto = DatabaseH2.TokenSolana_Leggi("MintAddr2");
        assertEquals("NUO", letto[0]);
        assertEquals("Nome Nuovo", letto[1]);
        assertEquals("tipoB", letto[2]);
    }

    @Test
    void tokenSolana_addressCaseSensitive_sonoTokenDistinti() {
        // Gli address Solana sono case-sensitive: non vanno mai trattati come la stessa chiave
        DatabaseH2.TokenSolana_AggiungiToken("AbCdEf", "MAIU", "Maiuscolo", "t1");
        DatabaseH2.TokenSolana_AggiungiToken("abcdef", "MINU", "Minuscolo", "t2");

        assertEquals("MAIU", DatabaseH2.TokenSolana_Leggi("AbCdEf")[0]);
        assertEquals("MINU", DatabaseH2.TokenSolana_Leggi("abcdef")[0]);
    }
}
