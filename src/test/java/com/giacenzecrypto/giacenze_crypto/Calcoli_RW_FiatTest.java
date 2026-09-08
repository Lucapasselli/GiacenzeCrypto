package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
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
 * Unit test delle primitive di saldo di {@link Calcoli_RW_Fiat} (fase F1 della parte FIAT del
 * quadro W/RW). Movimenti costruiti a mano in {@code Principale.MappaCryptoWallet}, associazione
 * wallet→gruppo in {@code WALLETGRUPPO} : nessun accesso a rete o cache prezzi.
 *
 * <p>Ciò che i test fissano :
 * <ul>
 *   <li>la direzione della gamba FIAT dipende dalla <b>posizione</b> (uscita {@code v[8..10]} /
 *       entrata {@code v[11..13]}) e non dal segno memorizzato nella quantità ;</li>
 *   <li>il saldo è per <b>valuta</b> ; i trasferimenti interni ({@code TI}) non incidono ;</li>
 *   <li>un giroconto fra gruppi diversi è assorbito dalla somma per gruppo, senza logica dedicata ;</li>
 *   <li>{@code inclusivo} distingue il saldo a fine giornata dal "residuo" di inizio giornata ;</li>
 *   <li>{@link Calcoli_RW_Fiat#gambeFiatGiorno} elenca le gambe di una giornata in ordine di ID.</li>
 * </ul>
 */
class Calcoli_RW_FiatTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

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
    void setUp() throws Exception {
        Principale.MappaCryptoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM WALLETGRUPPO");
            st.execute("DELETE FROM GRUPPO_PERIODO_RW");
            st.execute("DELETE FROM GRUPPO_RIFERIMENTO_ESTERO");
            st.execute("DELETE FROM EXCHANGE_PERIODO");
        }
        progressivo = 0;
    }

    /** riga GUI di GRUPPO_PERIODO_RW a 11 colonne, tipo FIAT. */
    private static String[] periodoFiat(String prog, String di, String df,
            String valIni, String valFin, String modIni, String modFin) {
        return new String[] {"FIAT", prog, di, df, valIni, "", valFin, "", modIni, modFin, ""};
    }

    private static String iv(List<String[]> intervalli, int riga, int col) {
        return intervalli.get(riga)[col];
    }

    // ------------------------------------------------------------------
    // costruzione movimenti
    // ------------------------------------------------------------------

    /**
     * Crea un movimento e lo registra in {@code MappaCryptoWallet}.
     *
     * @param data   "yyyy-MM-dd HH:mm" (determina anche l'ordine di elaborazione)
     * @param cat    ultimo segmento dell'ID = categoria fiscale (DF, PF, SF, AC, VC, TI...)
     * @param wallet valore di {@code v[3]} (mappato a un gruppo via WALLETGRUPPO)
     */
    private static String[] mov(String data, String cat, String wallet,
            String monU, String tipoU, String qtaU,
            String monE, String tipoE, String qtaE) {
        String prefisso = data.replace("-", "").replace(":", "").replace(" ", "");
        String id = prefisso + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = wallet;
        m[8] = monU;  m[9] = tipoU;  m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        m[15] = "0";
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    private static String saldo(Map<String, BigDecimal> saldi, String valuta) {
        BigDecimal b = saldi.get(valuta);
        return b == null ? null : b.stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------
    // saldo per valuta
    // ------------------------------------------------------------------

    @Test
    void depositoEPrelievoEuro_saldoNetto() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-05-01 10:00", "PF", "Kraken", "EUR", "FIAT", "300", "", "", "");

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true);
        assertEquals("700", saldo(s, "EUR"));
    }

    @Test
    void direzioneDaPosizioneNonDalSegnoDellaQuantita() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        // stesso prelievo di 300 EUR, una volta con quantità "300" e una con "-300"
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-03-01 10:00", "PF", "Kraken", "EUR", "FIAT", "300", "", "", "");
        Map<String, BigDecimal> conPositivo = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true);

        Principale.MappaCryptoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-03-01 10:00", "PF", "Kraken", "EUR", "FIAT", "-300", "", "", "");
        Map<String, BigDecimal> conNegativo = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true);

        assertEquals("700", saldo(conPositivo, "EUR"));
        assertEquals("700", saldo(conNegativo, "EUR"));
    }

    @Test
    void gambeFiatDiAcquistoEVendita() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", "Wallet 02");
        // spendo 1000 EUR per comprare 0,02 BTC, poi vendo 0,02 BTC per 1500 EUR
        mov("2024-01-10 09:00", "AC", "Binance", "EUR", "FIAT", "1000", "BTC", "Crypto", "0.02");
        mov("2024-09-10 09:00", "VC", "Binance", "BTC", "Crypto", "0.02", "EUR", "FIAT", "1500");

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 02", "2024-12-31", true);
        assertEquals("500", saldo(s, "EUR"));
    }

    @Test
    void multivaluta_saldiSeparatiPerValuta() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Bitstamp", "Wallet 03");
        mov("2024-02-01 10:00", "DF", "Bitstamp", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-02-02 10:00", "DF", "Bitstamp", "", "", "", "USD", "FIAT", "500");
        mov("2024-06-01 10:00", "SF", "Bitstamp", "EUR", "FIAT", "200", "USD", "FIAT", "210");

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 03", "2024-12-31", true);
        assertEquals("800", saldo(s, "EUR"));
        assertEquals("710", saldo(s, "USD"));
    }

    @Test
    void inclusivoDistingueFineGiornoDaResiduoInizioGiorno() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-03-10 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-03-15 10:00", "PF", "Kraken", "EUR", "FIAT", "400", "", "", "");

        Map<String, BigDecimal> residuo = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-03-15", false);
        Map<String, BigDecimal> fineGiorno = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-03-15", true);

        assertEquals("1000", saldo(residuo, "EUR"));   // il prelievo del 15 non è ancora contato
        assertEquals("600", saldo(fineGiorno, "EUR")); // contato
    }

    @Test
    void trasferimentoInternoNonIncideSulSaldo() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        // un TI con una gamba FIAT non deve spostare il saldo del gruppo
        mov("2024-03-01 10:00", "TI", "Kraken", "EUR", "FIAT", "1000", "", "", "");

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true);
        assertEquals("1000", saldo(s, "EUR"));
    }

    @Test
    void girocontoTraGruppiDiversiAssorbitoDallaSommaPerGruppo() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Bitpanda", "Wallet 05");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        // giroconto: esce da Kraken, entra su Bitpanda (due movimenti, gruppi diversi)
        mov("2024-06-01 10:00", "PF", "Kraken", "EUR", "FIAT", "500", "", "", "");
        mov("2024-06-01 10:05", "DF", "Bitpanda", "", "", "", "EUR", "FIAT", "500");

        assertEquals("500", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true), "EUR"));
        assertEquals("500", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 05", "2024-12-31", true), "EUR"));
    }

    @Test
    void valuteConSaldoZeroNonCompaiono() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "100");
        mov("2024-03-01 10:00", "PF", "Kraken", "EUR", "FIAT", "100", "", "", "");

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 01", "2024-12-31", true);
        assertTrue(s.isEmpty(), "una valuta con saldo netto zero non deve comparire: " + s);
    }

    @Test
    void gruppoSenzaMovimentiFiat_saldoVuoto() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", "Wallet 02");
        mov("2024-01-10 09:00", "SC", "Binance", "ETH", "Crypto", "1", "BTC", "Crypto", "0.05");

        assertTrue(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 02", "2024-12-31", true).isEmpty());
        assertFalse(Calcoli_RW_Fiat.haMovimentiFiat("Wallet 02"));
    }

    // ------------------------------------------------------------------
    // gambe FIAT della giornata
    // ------------------------------------------------------------------

    @Test
    void gambeFiatGiorno_ordinatePerIdConUnaVocePerGamba() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Bitstamp", "Wallet 03");
        mov("2024-07-01 08:00", "DF", "Bitstamp", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-07-01 12:00", "SF", "Bitstamp", "EUR", "FIAT", "200", "USD", "FIAT", "210");
        mov("2024-07-01 18:00", "PF", "Bitstamp", "EUR", "FIAT", "100", "", "", "");
        mov("2024-07-02 09:00", "DF", "Bitstamp", "", "", "", "EUR", "FIAT", "50"); // altro giorno

        List<String[]> gambe = Calcoli_RW_Fiat.gambeFiatGiorno("Wallet 03", "2024-07-01");

        assertEquals(4, gambe.size(), "DF(1) + SF(2) + PF(1)");
        assertEquals("EUR", gambe.get(0)[1]);
        assertEquals("1000", new BigDecimal(gambe.get(0)[2]).toPlainString());   // apporto DF
        assertEquals("-200", new BigDecimal(gambe.get(1)[2]).toPlainString());   // uscita EUR dello swap
        assertEquals("USD", gambe.get(2)[1]);
        assertEquals("210", new BigDecimal(gambe.get(2)[2]).toPlainString());    // apporto USD dello swap
        assertEquals("-100", new BigDecimal(gambe.get(3)[2]).toPlainString());   // uscita PF
    }

    @Test
    void gambeFiatGiorno_trasferimentoInternoEscluso() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-07-01 08:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        mov("2024-07-01 09:00", "TI", "Kraken", "EUR", "FIAT", "1000", "", "", "");

        List<String[]> gambe = Calcoli_RW_Fiat.gambeFiatGiorno("Wallet 01", "2024-07-01");
        assertEquals(1, gambe.size());
        assertEquals("1000", new BigDecimal(gambe.get(0)[2]).toPlainString());
    }

    @Test
    void haMovimentiFiat_veroSeEsisteUnaGambaFiat() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2024-02-01 10:00", "AC", "Kraken", "EUR", "FIAT", "100", "BTC", "Crypto", "0.002");
        assertTrue(Calcoli_RW_Fiat.haMovimentiFiat("Wallet 01"));
    }

    // ------------------------------------------------------------------
    // intervalliFiat
    // ------------------------------------------------------------------

    @Test
    void intervalliFiat_nessunTaglio_unSoloTrattoSullAnno() {
        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(1, iv.size());
        assertEquals("2024-01-01", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-12-31", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
    }

    @Test
    void intervalliFiat_modalitaStato_statoFissoTuttoLAnno() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 01",
                Principale_GruppiWalletRW.RIFERIMENTO_STATO, "092", "LU123", null);
        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(1, iv.size());
        assertEquals("092", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
    }

    @Test
    void intervalliFiat_cambioStatoExchange_spezzaInDueConLoStatoGiusto() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 01",
                Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE, null, null, "kraken");
        Principale_PeriodiExchange.salvaPeriodi("kraken", Arrays.asList(
                new String[] {"1", "", "2024-05-31", "Payward Ltd", "040", "", "", ""},
                new String[] {"2", "2024-06-01", "", "Payward Europe", "092", "", "", ""}));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(2, iv.size());
        assertEquals("2024-01-01", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-05-31", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("040", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
        assertEquals("2024-06-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-12-31", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("092", iv(iv, 1, Calcoli_RW_Fiat.IV_STATO));
        // nessun confine utente : niente modalità
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_FINALE));
        assertEquals("", iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
    }

    @Test
    void intervalliFiat_periodoFiatManuale_portaModalitaEValoriSoloAiSuoiBordi() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2024-01-01", "2024-06-30", "1500.00", "0.00",
                        Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO,
                        Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA)));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2024");
        assertEquals(2, iv.size());
        // tratto 1 : inizio == DataInizio del periodo, fine == DataFine del periodo -> confini utente
        assertEquals("2024-06-30", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO, iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals("1500.00", iv(iv, 0, Calcoli_RW_Fiat.IV_VAL_INIZIALE_MAN));
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA, iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_FINALE));
        assertEquals("0.00", iv(iv, 0, Calcoli_RW_Fiat.IV_VAL_FINALE_MAN));
        // tratto 2 : resto dell'anno, nessun confine utente -> giacenza pura
        assertEquals("2024-07-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("", iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals("", iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_FINALE));
    }

    @Test
    void intervalliFiat_periodoManualeSpezzatoDaCambioStato_modIniSulPrimoModFinSullUltimo() {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 06",
                Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE, null, null, "kraken");
        Principale_PeriodiExchange.salvaPeriodi("kraken", Arrays.asList(
                new String[] {"1", "", "2024-05-31", "A", "040", "", "", ""},
                new String[] {"2", "2024-06-01", "", "B", "092", "", "", ""}));
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2024-03-01", "2024-08-31", "", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI,
                        Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE)));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2024");
        // tagli: 2024-03-01 (periodo FIAT), 2024-06-01 (cambio stato), 2024-09-01 (fine periodo FIAT)
        assertEquals(4, iv.size());
        assertEquals("2024-03-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals("", iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_FINALE), "il taglio interno non è un confine utente");
        assertEquals("2024-06-01", iv(iv, 2, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("", iv(iv, 2, Calcoli_RW_Fiat.IV_MOD_INIZIALE), "il taglio interno non è un confine utente");
        assertEquals("2024-08-31", iv(iv, 2, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE, iv(iv, 2, Calcoli_RW_Fiat.IV_MOD_FINALE));
        assertEquals("2024-09-01", iv(iv, 3, Calcoli_RW_Fiat.IV_DATA_INIZIO));
    }

    @Test
    void intervalliFiat_periodoDateVuote_copreAnnoEPortaModalitaAgliEstremi() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "100.00", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI,
                        Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA)));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2024");
        assertEquals(1, iv.size());
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals("100.00", iv(iv, 0, Calcoli_RW_Fiat.IV_VAL_INIZIALE_MAN));
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA, iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_FINALE));
    }

    @Test
    void intervalliFiat_periodoFuoriDallAnno_ignorato() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2023-01-01", "2023-12-31", "", "", "", "")));
        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2024");
        assertEquals(1, iv.size());
        assertEquals("2024-01-01", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-12-31", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
    }

    @Test
    void intervalliFiat_annoNonValido_listaVuota() {
        assertTrue(Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "abcd").isEmpty());
        assertTrue(Calcoli_RW_Fiat.intervalliFiat("", "2024").isEmpty());
    }

    // ------------------------------------------------------------------
    // generaRighiFiat (F3)
    // ------------------------------------------------------------------

    /** Cambio di test: solo EUR (1:1), ogni altra valuta non convertibile. */
    private static final Calcoli_RW_Fiat.ControvaloreEUR SOLO_EUR =
            (valuta, importo, dataIso) -> "EUR".equalsIgnoreCase(valuta) ? importo : null;

    private static List<String[]> righiFiat(String gruppo) {
        List<String[]> l = Principale.Mappa_RW_ListeXGruppoWallet_Fiat.get(gruppo);
        return l == null ? java.util.Collections.emptyList() : l;
    }

    @Test
    void generaRighiFiat_saldoInteroAnno_unRigoConGiacenzaPuraAiDueEstremi() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2023-06-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000"); // residuo da anno prima
        mov("2024-09-01 10:00", "PF", "Kraken", "EUR", "FIAT", "400", "", "", "");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals(1, r.size());
        assertEquals("1000", new BigDecimal(r.get(0)[5]).toPlainString());   // valore iniziale = residuo
        assertEquals("600", new BigDecimal(r.get(0)[10]).toPlainString());   // valore finale = 1000 - 400
        assertEquals("2024-01-01 00:00", r.get(0)[4]);
        assertEquals("2024-12-31 23:59", r.get(0)[9]);
        assertEquals("366", r.get(0)[11]);                                   // 2024 bisestile
        assertEquals(Calcoli_RW_Fiat.CAUSALE_PERIODO, r.get(0)[12]);
        assertEquals("", r.get(0)[15]);                                      // nessun avviso
        assertEquals("", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
    }

    @Test
    void generaRighiFiat_aperturaInfraAnno_primoTrattoParteDalPrimoMovimento() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", "Wallet 02");
        mov("2024-03-15 09:00", "DF", "Binance", "", "", "", "EUR", "FIAT", "1000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 02");
        assertEquals(1, r.size());
        assertEquals("2024-03-15 00:00", r.get(0)[4]);
        assertEquals("1000", new BigDecimal(r.get(0)[5]).toPlainString());   // saldo a fine del primo giorno
        assertEquals("1000", new BigDecimal(r.get(0)[10]).toPlainString());
    }

    @Test
    void generaRighiFiat_cambioStatoExchange_dueRighiConStatoDiverso() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 01",
                Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE, null, null, "kraken");
        Principale_PeriodiExchange.salvaPeriodi("kraken", Arrays.asList(
                new String[] {"1", "", "2024-05-31", "Payward Ltd", "040", "", "", ""},
                new String[] {"2", "2024-06-01", "", "Payward Europe", "092", "", "", ""}));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "2000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals(2, r.size());
        assertEquals("2024-05-31 23:59", r.get(0)[9]);
        assertEquals("040", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
        assertEquals("2024-06-01 00:00", r.get(1)[4]);
        assertEquals("092", r.get(1)[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
        assertEquals("2000", new BigDecimal(r.get(1)[10]).toPlainString());
    }

    @Test
    void generaRighiFiat_periodoManualeConValori_usaIValoriManuali() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 06");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2024-01-01", "2024-06-30", "1500.00", "0.00", "", "")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "100");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "800");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 06");
        assertEquals(2, r.size());
        assertEquals("1500.00", r.get(0)[5]);                                // valore manuale iniziale
        assertEquals("0.00", r.get(0)[10]);                                  // valore manuale finale
        assertEquals("900", new BigDecimal(r.get(1)[5]).toPlainString());    // 2° tratto: giacenza pura 100+800
        assertEquals("900", new BigDecimal(r.get(1)[10]).toPlainString());
    }

    @Test
    void generaRighiFiat_modalitaPrimoApporto_residuoPiuIlPrimoApportoDelGiorno() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 06");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2024-04-01", "2024-12-31", "", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO, "")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "100"); // residuo
        mov("2024-04-01 08:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "50");
        mov("2024-04-01 09:00", "PF", "Kraken", "EUR", "FIAT", "20", "", "", "");
        mov("2024-04-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "30");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 06");
        assertEquals(2, r.size());
        // tratto 2: inizio 2024-04-01 = confine utente, modalità PRIMO_APPORTO -> residuo(100) + primo apporto(50)
        assertEquals("2024-04-01 00:00", r.get(1)[4]);
        assertEquals("150", new BigDecimal(r.get(1)[5]).toPlainString());
        // valore finale = giacenza pura al 31/12 = 100 + 50 - 20 + 30
        assertEquals("160", new BigDecimal(r.get(1)[10]).toPlainString());
    }

    @Test
    void generaRighiFiat_saldoNegativo_rigoConValoriAZeroEAvviso() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 07");
        mov("2024-02-01 10:00", "PF", "Kraken", "EUR", "FIAT", "500", "", "", ""); // nessun deposito prima

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 07");
        assertEquals(1, r.size());
        assertEquals("0", new BigDecimal(r.get(0)[5]).toPlainString());
        assertEquals("0", new BigDecimal(r.get(0)[10]).toPlainString());
        assertTrue(r.get(0)[15].toLowerCase().contains("negativo"), r.get(0)[15]);
        assertFalse(r.get(0)[15].toLowerCase().contains("error"), "l'avviso non deve propagare ERRORI");
    }

    @Test
    void generaRighiFiat_saldoZeroAiDueEstremi_nessunRigo() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 08");
        // primo movimento nel 2023 -> nessuno spostamento dell'apertura, residuo al 1/1/2024 = 0
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "100");
        mov("2023-06-01 10:00", "PF", "Kraken", "EUR", "FIAT", "100", "", "", "");
        // deposito e prelievo totale dentro il 2024 : > 0 nel mezzo ma 0 ai due estremi
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "300");
        mov("2024-11-01 10:00", "PF", "Kraken", "EUR", "FIAT", "300", "", "", "");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);
        assertTrue(righiFiat("Wallet 08").isEmpty());
    }

    @Test
    void generaRighiFiat_multivaluta_valoreEUREconvertitoENonConvertibileSegnalato() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Bitstamp", "Wallet 03");
        mov("2023-01-01 10:00", "DF", "Bitstamp", "", "", "", "EUR", "FIAT", "1000");
        mov("2023-01-02 10:00", "DF", "Bitstamp", "", "", "", "USD", "FIAT", "500");

        // cambio che converte USD a 0,90 EUR
        Calcoli_RW_Fiat.ControvaloreEUR eurEUsd = (valuta, importo, dataIso) ->
                "EUR".equalsIgnoreCase(valuta) ? importo
                : "USD".equalsIgnoreCase(valuta) ? importo.multiply(new BigDecimal("0.90")) : null;
        Calcoli_RW_Fiat.generaRighiFiat("2024", eurEUsd);
        assertEquals("1450.00", new BigDecimal(righiFiat("Wallet 03").get(0)[10]).toPlainString()); // 1000 + 500*0.9

        // con SOLO_EUR l'USD non è convertibile -> avviso, contributo zero
        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);
        List<String[]> r = righiFiat("Wallet 03");
        assertEquals("1000", new BigDecimal(r.get(0)[10]).toPlainString());
        assertTrue(r.get(0)[15].toUpperCase().contains("USD"), r.get(0)[15]);
    }

    @Test
    void generaRighiFiat_opzioneDisattiva_mappaVuotaDaAggiornaRWFR() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", "D");
        mov("2024-02-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "1000");
        DatabaseH2.Pers_Opzioni_Scrivi("RW_FiatInRW", "NO");
        try {
            Calcoli_RW.AggiornaRWFR("2024");
            assertTrue(Principale.Mappa_RW_ListeXGruppoWallet_Fiat.isEmpty());

            DatabaseH2.Pers_Opzioni_Scrivi("RW_FiatInRW", "SI");
            Calcoli_RW.AggiornaRWFR("2024");
            assertFalse(Principale.Mappa_RW_ListeXGruppoWallet_Fiat.isEmpty(), "con l'opzione attiva il rigo FIAT c'è");
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi("RW_FiatInRW", "SI");
        }
    }
}
