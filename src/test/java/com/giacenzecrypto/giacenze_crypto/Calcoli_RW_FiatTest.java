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
        }
        // Regime della liquidità : riparto sempre dal default (IVAFE ordinaria liquidata), così i
        // test misurano il comportamento reale del programma e non una configurazione di comodo.
        DatabaseH2.Pers_Opzioni_Scrivi(Calcoli_RW_Fiat.OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO,
                Calcoli_RW_Fiat.LIQUIDITA_SOLO_MONITORAGGIO_DEFAULT);
        progressivo = 0;
        // Il Fiat Wallet Crypto.com e' un file nella working directory : va azzerato fra un test e
        // l'altro, altrimenti le gambe di un test le vede anche il successivo.
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(VarStatiche.getFile_CDCFiatWallet()));
        VarCondivise.CDC_FiatWallet_MappaTipiMovimenti.clear();
    }

    /**
     * Scrive il CSV del Fiat Wallet Crypto.com nella working directory dei test.
     * Formato reale del file : {@code data,descrizione,valuta,importo,valutaTo,importoTo,valutaNat,
     * importoNat,usd,tipo} e una virgola finale.
     */
    private static void fiatWallet(String... righe) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String r : righe) {
            sb.append(r).append("\n");
        }
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(VarStatiche.getFile_CDCFiatWallet()), sb.toString());
    }

    /** Una riga del Fiat Wallet in euro. */
    private static String rigaFW(String istante, String descrizione, String importo, String tipo) {
        return istante + "," + descrizione + ",EUR," + importo + ",EUR," + importo
                + ",EUR," + importo + ",0," + tipo + ",";
    }

    /** riga GUI di GRUPPO_PERIODO_RW, tipo FIAT, senza dati fiscali. */
    private static String[] periodoFiat(String prog, String di, String df,
            String valIni, String valFin, String modIni, String modFin) {
        return periodoFiat(prog, di, df, valIni, valFin, modIni, modFin, "");
    }

    /** riga GUI completa di GRUPPO_PERIODO_RW, tipo FIAT, con lo Stato estero del periodo. */
    private static String[] periodoFiat(String prog, String di, String df,
            String valIni, String valFin, String modIni, String modFin, String statoEstero) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = "FIAT";
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = prog;
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = di;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = df;
        r[Principale_GruppiWalletRW.COL_VAL_INIZIALE] = valIni;
        r[Principale_GruppiWalletRW.COL_VAL_FINALE] = valFin;
        r[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = modIni;
        r[Principale_GruppiWalletRW.COL_MOD_FINALE] = modFin;
        r[Principale_GruppiWalletRW.COL_STATO_ESTERO] = statoEstero;
        return r;
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
    void intervalliFiat_unicoPeriodoSenzaDate_statoFissoTuttoLAnno() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "", "", "", "", "092")));
        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(1, iv.size());
        assertEquals("092", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
    }

    @Test
    void intervalliFiat_cambioStatoEstero_spezzaInDueConLoStatoGiusto() {
        // la coppia che nasce da un cambio di Stato estero : una riga chiusa il giorno prima e una
        // aperta da lì in avanti. La prima non ha data di inizio -> parte dal primo movimento.
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "2024-05-31", "", "", "", "", "040"),
                periodoFiat("2", "2024-06-01", "", "", "", "", "", "092")));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(2, iv.size());
        assertEquals("2024-01-01", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-05-31", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("040", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
        assertEquals("2024-06-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("2024-12-31", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("092", iv(iv, 1, Calcoli_RW_Fiat.IV_STATO));
        // i due periodi non hanno modalità impostate
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_FINALE));
        assertEquals("", iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
    }

    @Test
    void intervalliFiat_secondoPeriodoSenzaDate_partOndaFineDelPrecedentePiuUno() {
        // riga 2 senza NESSUNA data : l'inizio si deduce dalla fine della riga 1 (+1 giorno)
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "2024-05-31", "", "", "", "", "040"),
                periodoFiat("2", "", "", "", "", "", "", "092")));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 01", "2024");
        assertEquals(2, iv.size());
        assertEquals("2024-05-31", iv(iv, 0, Calcoli_RW_Fiat.IV_DATA_FINE));
        assertEquals("040", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
        assertEquals("2024-06-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("092", iv(iv, 1, Calcoli_RW_Fiat.IV_STATO));
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
    void intervalliFiat_trattiScopertiFraDuePeriodi_senzaStatoESenzaModalita() {
        // buco volontario fra i due periodi (conto chiuso e riaperto) : il tratto in mezzo esiste
        // lo stesso — l'anno va comunque coperto — ma non ha né Stato né modalità.
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "2024-03-01", "2024-05-31", "", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI,
                        Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE, "040"),
                periodoFiat("2", "2024-09-01", "", "", "", "", "", "092")));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2024");
        // tagli : 2024-03-01, 2024-06-01 (fine del primo + 1), 2024-09-01
        assertEquals(4, iv.size());
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO), "prima del primo periodo : nessuno Stato");
        assertEquals("2024-03-01", iv(iv, 1, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("040", iv(iv, 1, Calcoli_RW_Fiat.IV_STATO));
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE, iv(iv, 1, Calcoli_RW_Fiat.IV_MOD_FINALE));
        assertEquals("2024-06-01", iv(iv, 2, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("", iv(iv, 2, Calcoli_RW_Fiat.IV_STATO), "tratto scoperto");
        assertEquals("", iv(iv, 2, Calcoli_RW_Fiat.IV_MOD_INIZIALE));
        assertEquals("2024-09-01", iv(iv, 3, Calcoli_RW_Fiat.IV_DATA_INIZIO));
        assertEquals("092", iv(iv, 3, Calcoli_RW_Fiat.IV_STATO));
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
    void intervalliFiat_annoSuccessivoAlCambioStato_prendeLoStatoELeModalitaDelPeriodoInCorso() {
        // La coppia da cambio di Stato estero, guardata l'anno DOPO : nessun taglio cade nell'anno, un
        // solo tratto. Il periodo chiuso (inizio indefinito, primo nell'ordinamento) non deve portare
        // né il proprio Stato né la propria modalità iniziale a un anno in cui era già finito.
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", Arrays.<String[]>asList(
                periodoFiat("1", "", "2024-05-31", "1500.00", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, "", "040"),
                periodoFiat("2", "2024-06-01", "", "", "", "", "", "092")));

        List<String[]> iv = Calcoli_RW_Fiat.intervalliFiat("Wallet 06", "2025");
        assertEquals(1, iv.size());
        assertEquals("092", iv(iv, 0, Calcoli_RW_Fiat.IV_STATO));
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_MOD_INIZIALE),
                "la modalità del periodo già chiuso non deve arrivare all'anno successivo");
        assertEquals("", iv(iv, 0, Calcoli_RW_Fiat.IV_VAL_INIZIALE_MAN),
                "il valore manuale del periodo già chiuso non deve arrivare all'anno successivo");
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
        // Col regime di default l'unico avviso e' quello dell'IVAFE liquidata : nessun errore.
        assertTrue(r.get(0)[15].contains("IVAFE"), r.get(0)[15]);
        assertFalse(r.get(0)[15].toLowerCase().contains("errore"), r.get(0)[15]);
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
    void generaRighiFiat_cambioStatoEstero_dueRighiConStatoDiverso() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "2024-05-31", "", "", "", "", "040"),
                periodoFiat("2", "2024-06-01", "", "", "", "", "", "092")));
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
    void generaRighiFiat_periodoInItalia_nessunRigo() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "", "", "", "", StatiEsteri.CODICE_ITALIA)));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "3000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        assertTrue(righiFiat("Wallet 01").isEmpty(), "un conto in Italia non produce righi RW valuta");
    }

    @Test
    void generaRighiFiat_daEsteroAItalia_soloIlTrattoEstero() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "2024-05-31", "", "", "", "", "040"),
                periodoFiat("2", "2024-06-01", "", "", "", "", "", StatiEsteri.CODICE_ITALIA)));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "2000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals(1, r.size(), "il tratto in Italia non produce rigo");
        assertEquals("040", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
        assertEquals("2024-05-31 23:59", r.get(0)[9]);
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

    // ------------------------------------------------------------------
    // conto corrente estero (EContoCorrente = SI) : codice bene 1, IVAFE fissa
    // ------------------------------------------------------------------

    /** riga GUI FIAT con lo Stato estero e il flag "e' conto corrente" = SI. */
    private static String[] periodoFiatCC(String prog, String di, String df, String statoEstero) {
        String[] r = periodoFiat(prog, di, df, "", "", "", "", statoEstero);
        r[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE] = Principale_GruppiWalletRW.CONTO_CORRENTE_SI;
        return r;
    }

    @Test
    void generaRighiFiat_contoCorrente_annoInteroSopraSoglia_ivafeFissa3420() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals(1, r.size());
        assertEquals(Calcoli_RW_Fiat.FIAT_COLONNE, r.get(0).length);
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, r.get(0)[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("NO", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
        assertEquals("10000.00", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_VALORE_MEDIO]);
        assertEquals("34.20", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_IVAFE], "anno intero: 34,20 x 365/365");
        // il saldo finale reale resta in [10], indipendente dal valore medio
        assertEquals("10000", new BigDecimal(r.get(0)[10]).toPlainString());
    }

    @Test
    void generaRighiFiat_contoCorrente_annoBisestile_ivafeAncora3420() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR); // 366 giorni

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals("34.20", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_IVAFE], "366/366 = 1, non 366/365");
    }

    @Test
    void generaRighiFiat_contoCorrente_sottoSoglia5000_ivafeNonDovutaMaRigoPresente() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "3000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        assertEquals(1, r.size(), "il rigo esce comunque per il monitoraggio");
        assertEquals("0.00", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        assertEquals("SI", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, r.get(0)[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertTrue(r.get(0)[15].toLowerCase().contains("soglia"), r.get(0)[15]);
    }

    @Test
    void generaRighiFiat_contoCorrente_infraAnno_sogliaSullaVitaNonPonderataPerGiorni() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        // conto corrente da 01/07 in poi ; prima del 01/07 non è conto corrente (nessun periodo lo copre)
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "2025-06-30", "", "", "", "", "092"),
                periodoFiatCC("2", "2025-07-01", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "9000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 01");
        // due tratti : solo il secondo è conto corrente
        assertEquals(2, r.size());
        String[] cc = r.get(1);
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, cc[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        // valore medio sulla vita del conto (2° semestre) = 9000, sopra soglia anche se ~metà anno
        assertEquals("9000.00", cc[Calcoli_RW_Fiat.FIAT_COL_VALORE_MEDIO]);
        assertEquals("NO", cc[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
        // IVAFE prorata sui giorni del tratto (01/07-31/12 = 184 gg su 365)
        int gg = Integer.parseInt(cc[11]);
        BigDecimal atteso = new BigDecimal("34.20").multiply(BigDecimal.valueOf(gg))
                .divide(BigDecimal.valueOf(365), 2, java.math.RoundingMode.HALF_UP);
        assertEquals(atteso.toPlainString(), cc[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        // il primo tratto (non conto corrente) resta codice bene 14, ma col regime di default
        // liquida l'IVAFE ordinaria : la scelta sulla liquidità non tocca i righi conto corrente.
        assertEquals("14", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("NO", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
    }

    @Test
    void generaRighiFiat_contoCorrente_valoreMedioDistintoDalSaldoFinale() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        // 10.000 tutto l'anno, poi +10.000 il 31/12 : saldo finale 20.000, media ~10.000
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");
        mov("2025-12-31 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] cc = righiFiat("Wallet 01").get(0);
        assertEquals("20000", new BigDecimal(cc[10]).toPlainString(), "[10] = saldo finale reale");
        BigDecimal media = new BigDecimal(cc[Calcoli_RW_Fiat.FIAT_COL_VALORE_MEDIO]);
        assertTrue(media.compareTo(new BigDecimal("10000")) >= 0 && media.compareTo(new BigDecimal("10100")) < 0,
                "valore medio ~10.000, non il saldo finale : " + media);
        assertEquals("20000.00", cc[Calcoli_RW_Fiat.FIAT_COL_VALORE_MASSIMO]);
    }

    @Test
    void generaRighiFiat_contoCorrente_apertoESvuotatoNellAnno_rigoComunquePresente() {
        // Stesso schema di generaRighiFiat_saldoZeroAiDueEstremi_nessunRigo, ma il periodo è
        // "e' conto corrente" = SI : per un vero conto corrente la misura è la giacenza MEDIA
        // sull'anno di vita, non i due estremi, quindi lo skip di punto 14 NON deve applicarsi.
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 09");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 09", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        // saldo netto zero portato nel 2025 (primo movimento nel 2023 : nessuno spostamento apertura)
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "100");
        mov("2023-06-01 10:00", "PF", "Kraken", "EUR", "FIAT", "100", "", "", "");
        // conto alimentato e svuotato dentro il 2025 : 0 ai due estremi, 40.000 nel mezzo
        mov("2025-03-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "40000");
        mov("2025-11-01 10:00", "PF", "Kraken", "EUR", "FIAT", "40000", "", "", "");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 09");
        assertEquals(1, r.size(), "il conto corrente deve produrre il rigo anche con estremi a zero");
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, r.get(0)[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("0", new BigDecimal(r.get(0)[10]).toPlainString(), "[10] = saldo finale reale = 0");
        BigDecimal media = new BigDecimal(r.get(0)[Calcoli_RW_Fiat.FIAT_COL_VALORE_MEDIO]);
        assertTrue(media.compareTo(new BigDecimal("5000")) > 0 && media.compareTo(new BigDecimal("40000")) < 0,
                "valore medio sulla vita del conto, > 5.000 e < 40.000 : " + media);
        assertEquals("NO", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO], "media > 5.000 : IVAFE dovuta");
        assertEquals("34.20", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_IVAFE], "anno intero di detenzione");
        assertEquals("40000.00", r.get(0)[Calcoli_RW_Fiat.FIAT_COL_VALORE_MASSIMO]);
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

    // ------------------------------------------------------------------
    // IVAFE sulla liquidità in valuta (codice bene 14) e opzione utente
    // ------------------------------------------------------------------

    private static void regimeLiquidita(boolean soloMonitoraggio) {
        DatabaseH2.Pers_Opzioni_Scrivi(Calcoli_RW_Fiat.OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO,
                soloMonitoraggio ? "SI" : "NO");
    }

    @Test
    void liquidita_regimeDiDefault_liquidaIvafeOrdinariaSulValoreFinale() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("14", r[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        // 10.000 x 0,20 % x 365/365 = 20,00
        assertEquals("20.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        assertEquals("NO", r[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
    }

    @Test
    void liquidita_opzioneSoloMonitoraggio_nessunaIvafeECasella16Barrata() {
        regimeLiquidita(true);
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("14", r[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("0.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        assertEquals("SI", r[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
        assertEquals("", r[15], "in solo monitoraggio non deve comparire l'avviso sull'IVAFE");
    }

    /** L'imposta è rapportata ai giorni del tratto, e il denominatore è la lunghezza reale dell'anno. */
    @Test
    void liquidita_ivafeProrataSuiGiorniDelTratto_annoBisestile() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", "Wallet 02");
        mov("2024-07-01 10:00", "DF", "Binance", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        String[] r = righiFiat("Wallet 02").get(0);
        int gg = Integer.parseInt(r[11]);
        assertEquals(184, gg);                                    // 01/07 - 31/12
        BigDecimal atteso = new BigDecimal("10000").multiply(new BigDecimal("0.002"))
                .multiply(BigDecimal.valueOf(gg))
                .divide(BigDecimal.valueOf(366), 2, java.math.RoundingMode.HALF_UP);  // 2024 bisestile
        assertEquals(atteso.toPlainString(), r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
    }

    /**
     * L'opzione non tocca i conti correnti : lì l'imposta è quella in misura fissa, e non è in
     * discussione. Anche col solo monitoraggio scelto per la liquidità, il conto corrente paga.
     */
    @Test
    void liquidita_soloMonitoraggio_nonTogliieIvafeAlContoCorrente() {
        regimeLiquidita(true);
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "9000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, r[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("34.20", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        assertEquals("NO", r[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
    }

    /**
     * Un tratto conto corrente non deve mai portare l'avviso della liquidità : l'imposta la scrive
     * applicaContoCorrente(), e un avviso smentito subito dopo resterebbe in coda alla colonna 15.
     */
    @Test
    void contoCorrente_nessunAvvisoDiLiquiditaAncheColRegimeDiDefault() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "092")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "9000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertFalse(r[15].contains("liquidità"), r[15]);
        assertTrue(r[15].contains("conto corrente"), r[15]);
    }

    /** Saldo finale nullo : nessuna base imponibile, quindi nessuna imposta e casella 16 barrata. */
    @Test
    void liquidita_saldoFinaleNullo_nessunaIvafe() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "5000");
        mov("2025-06-30 10:00", "PF", "Kraken", "EUR", "FIAT", "5000", "", "", "");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("0", new BigDecimal(r[10]).toPlainString());
        assertEquals("0.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
        assertEquals("SI", r[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO]);
    }


    // ------------------------------------------------------------------
    // Aliquota maggiorata (Stati a fiscalità privilegiata, art. 19 c. 20-bis)
    // ------------------------------------------------------------------

    /** Hong Kong (103) è nell'elenco del D.M. 4 maggio 1999 : dal 2024 l'aliquota è doppia. */
    @Test
    void liquidita_statoPrivilegiato_aliquotaRaddoppiataDal2024() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "", "", "", "", "103")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("103", r[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
        assertEquals("40.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);   // 10.000 x 0,40 %
        assertTrue(r[15].contains("0,40 %"), r[15]);
        assertTrue(r[15].contains("colonna 21"), r[15]);
    }

    /** Prima del 2024 la maggiorazione non esiste : stesso Stato, aliquota ordinaria. */
    @Test
    void liquidita_statoPrivilegiato_primaDel2024AliquotaOrdinaria() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "", "", "", "", "103")));
        mov("2022-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2023", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("20.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);   // 10.000 x 0,20 %
        assertFalse(r[15].contains("colonna 21"), r[15]);
    }

    /** Uno Stato fuori elenco resta all'aliquota ordinaria anche dopo il 2024. */
    @Test
    void liquidita_statoNonPrivilegiato_aliquotaOrdinaria() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiat("1", "", "", "", "", "", "", "092")));   // LUSSEMBURGO
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        assertEquals("20.00", righiFiat("Wallet 01").get(0)[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
    }

    /**
     * Uno Stato non indicato non fa scattare la maggiorazione : il rigo porta già l'avviso sullo
     * Stato mancante, e presumere la fiscalità privilegiata sarebbe peggio che non presumere nulla.
     */
    @Test
    void liquidita_statoEsteroMancante_aliquotaOrdinaria() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "10000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals("", r[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO]);
        assertEquals("20.00", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
    }

    /** Il comma 20-bis parla dei soli "prodotti finanziari" : il conto corrente resta a 34,20 fissi. */
    @Test
    void contoCorrente_inStatoPrivilegiato_restaAllaMisuraFissa() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", Arrays.<String[]>asList(
                periodoFiatCC("1", "", "", "103")));
        mov("2023-01-01 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "9000");

        Calcoli_RW_Fiat.generaRighiFiat("2025", SOLO_EUR);

        String[] r = righiFiat("Wallet 01").get(0);
        assertEquals(Calcoli_RW_Fiat.CODICE_BENE_CONTO_CORRENTE, r[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE]);
        assertEquals("34.20", r[Calcoli_RW_Fiat.FIAT_COL_IVAFE]);
    }

    @Test
    void aliquotaIvafe_sceltaPerAnnoEStato() {
        assertEquals(Calcoli_RW_Fiat.ALIQUOTA_IVAFE_PRIVILEGIATA, Calcoli_RW_Fiat.aliquotaIvafe(2024, "103"));
        assertEquals(Calcoli_RW_Fiat.ALIQUOTA_IVAFE_ORDINARIA,    Calcoli_RW_Fiat.aliquotaIvafe(2023, "103"));
        assertEquals(Calcoli_RW_Fiat.ALIQUOTA_IVAFE_ORDINARIA,    Calcoli_RW_Fiat.aliquotaIvafe(2025, "092"));
        assertEquals(Calcoli_RW_Fiat.ALIQUOTA_IVAFE_ORDINARIA,    Calcoli_RW_Fiat.aliquotaIvafe(2025, ""));
    }


    // ------------------------------------------------------------------
    // Crypto.com App : la parte FIAT viene dal Fiat Wallet, non dai movimenti crypto
    // ------------------------------------------------------------------

    @Test
    void fiatWalletCDC_ilSaldoVieneDalFiatWalletENonDaiMovimentiCrypto() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        // Movimento crypto con gamba FIAT : per Crypto.com App NON deve essere contato.
        mov("2024-02-01 10:00", "DF", "Crypto.com App", "", "", "", "EUR", "FIAT", "9999");
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                rigaFW("2024-04-01 09:00:00", "Buy CRO", "400.0", "viban_purchase"));

        Map<String, BigDecimal> s = Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true);
        assertEquals("600", saldo(s, "EUR"), "1000 in ingresso - 400 in uscita, i 9999 del movimento crypto non contano");
    }

    @Test
    void fiatWalletCDC_gambaFiatDelMovimentoCryptoNonRaddoppiaIlSaldo() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        // Stesso importo su entrambe le sorgenti : se non fosse escluso il movimento crypto,
        // il saldo verrebbe 2000 invece di 1000.
        mov("2024-03-01 09:00", "DF", "Crypto.com App", "", "", "", "EUR", "FIAT", "1000");
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"));

        assertEquals("1000", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));
    }

    @Test
    void fiatWalletCDC_esclusionePerNomeExchangeNonPerGruppo() throws Exception {
        // Crypto.com App raggruppato insieme a un altro exchange : le gambe FIAT dell'altro
        // exchange devono restare, altrimenti l'esclusione per gruppo le cancellerebbe.
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 10");
        mov("2024-02-01 10:00", "DF", "Crypto.com App", "", "", "", "EUR", "FIAT", "9999"); // scartato
        mov("2024-02-02 10:00", "DF", "Kraken", "", "", "", "EUR", "FIAT", "500");          // resta
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"));

        assertEquals("1500", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));
    }

    @Test
    void fiatWalletCDC_ilSegnoVieneDalTipoMovimentoNonDallImporto() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        // Il file porta l'importo col segno meno : l'importatore lo rende positivo e il segno
        // arriva solo dal tipo movimento (viban_withdrawal = uscita).
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                rigaFW("2024-04-01 09:00:00", "Withdrawal", "-250.0", "viban_withdrawal"));

        assertEquals("750", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));
    }

    @Test
    void fiatWalletCDC_tipoSconosciutoScartatoESegnalato() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                rigaFW("2024-04-01 09:00:00", "Boh", "700.0", "tipo_mai_visto"));

        assertEquals("1000", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);
        List<String[]> r = righiFiat("Wallet 10");
        assertEquals(1, r.size());
        assertTrue(r.get(0)[15].contains("tipo sconosciuto"), r.get(0)[15]);
    }

    @Test
    void fiatWalletCDC_movimentoNonInEuroScartatoESegnalato() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                "2024-04-01 09:00:00,GBP Deposit,GBP,300.0,GBP,300.0,GBP,300.0,0,viban_deposit,");

        assertEquals("1000", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);
        List<String[]> r = righiFiat("Wallet 10");
        assertEquals(1, r.size());
        assertTrue(r.get(0)[15].contains("non in euro"), r.get(0)[15]);
    }

    @Test
    void fiatWalletCDC_righeLockEUnlockIgnorate() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        fiatWallet(rigaFW("2024-03-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                rigaFW("2024-03-02 09:00:00", "Lock", "500.0", "trading.limit_order.fiat_wallet.purchase_lock"),
                rigaFW("2024-03-02 09:00:01", "Unlock", "500.0", "trading.limit_order.fiat_wallet.purchase_unlock"));

        assertEquals("1000", saldo(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true), "EUR"));
        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);
        assertFalse(righiFiat("Wallet 10").get(0)[15].contains("sconosciuto"), "lock/unlock non sono errori");
    }

    @Test
    void fiatWalletCDC_gambeInOrdineCronologicoDentroLaGiornata() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        // Le descrizioni sono in ordine alfabetico inverso rispetto all'ora : l'ordine delle gambe
        // deve venire dall'istante (l'id le porta), non dalla chiave del Fiat Wallet.
        fiatWallet(rigaFW("2024-03-01 08:00:00", "Zeta deposito", "100.0", "viban_deposit"),
                rigaFW("2024-03-01 09:00:00", "Alfa deposito", "200.0", "viban_deposit"));

        List<String[]> gambe = Calcoli_RW_Fiat.gambeFiatGiorno("Wallet 10", "2024-03-01");
        assertEquals(2, gambe.size());
        assertEquals("100", new BigDecimal(gambe.get(0)[2]).stripTrailingZeros().toPlainString());
        assertEquals("200", new BigDecimal(gambe.get(1)[2]).stripTrailingZeros().toPlainString());
    }

    @Test
    void fiatWalletCDC_assenteNessunaGambaENessunAvviso() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        mov("2024-02-01 10:00", "AC", "Crypto.com App", "EUR", "FIAT", "100", "CRO", "CRYPTO", "1");

        assertTrue(Calcoli_RW_Fiat.saldiFiatPerValuta("Wallet 10", "2024-12-31", true).isEmpty());
        assertFalse(Calcoli_RW_Fiat.haMovimentiFiat("Wallet 10"));
    }

    @Test
    void fiatWalletCDC_rigoRWCoiValoriDelFiatWallet() throws Exception {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Crypto.com App", "Wallet 10");
        fiatWallet(rigaFW("2023-06-01 09:00:00", "EUR Deposit (via SEPA)", "1000.0", "viban_deposit"),
                rigaFW("2024-09-01 09:00:00", "Buy CRO", "400.0", "viban_purchase"));

        Calcoli_RW_Fiat.generaRighiFiat("2024", SOLO_EUR);

        List<String[]> r = righiFiat("Wallet 10");
        assertEquals(1, r.size());
        assertEquals("1000", new BigDecimal(r.get(0)[5]).stripTrailingZeros().toPlainString());  // residuo da fine 2023
        assertEquals("600", new BigDecimal(r.get(0)[10]).stripTrailingZeros().toPlainString());  // 1000 - 400
    }

}
