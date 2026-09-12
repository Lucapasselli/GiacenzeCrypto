package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa {@code walletSpecularePerCausale} e la sua configurazione reale sui Dual Investment di
 * Binance ({@code config/import/Binance CSV.json} &ge; v1.005).
 *
 * <p>Il problema che la chiave risolve: {@code Dual Savings Purchase} e {@code Dual Savings
 * Settlement} sono due righe distanti settimane, che l'import non può accoppiare, e il rientro può
 * avvenire in un'altra moneta. Mapparle a {@code IGNORA} perderebbe il rendimento e farebbe comparire
 * BTC dal nulla; emetterle come due gambe speculari su un sotto-wallet dedicato tiene il saldo esatto
 * e lascia lo scarto come giacenza negativa da compensare a mano.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>{@code [3]} resta l'exchange su <b>entrambe</b> le gambe (è la chiave del gruppo wallet
 *       fiscale e della deduplica del re-import), a cambiare è il solo sotto-wallet {@code [4]};</li>
 *   <li><b>l'uscita precede sempre l'entrata</b> nell'ordinamento degli ID, e quale delle due gambe
 *       sia l'uscita dipende dal segno della riga: è l'asserzione che si rompe se qualcuno riporta i
 *       due {@code numMovimento} a {@code 1};</li>
 *   <li>entrambe le gambe sono {@code TI}, quindi non muovono il LIFO;</li>
 *   <li>{@code TRASFERIMENTO-CRYPTO-INTERNO} è fra le {@code causaliChiuse}: senza, un gruppo
 *       multi-riga passerebbe da {@code TransazioneDefi}/{@code RitornaScambi}, che chiama
 *       {@code creaMovimento} con {@code TipoTr = null} e ne butta via la causale;</li>
 *   <li>{@code Funzioni.ControllaSaldiNegativi} distingue i due sotto-wallet, che è ciò che rende
 *       visibile lo scarto.</li>
 * </ul>
 */
class ImportazioneGenericaWalletSpecularePerCausaleTest {

    @TempDir
    static Path tempDir;

    /** Un movimento in cripto senza prezzo consulta la cache prezzi/EMoney: serve il DB. */
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

    private static ConfigurazioneImport cfg() throws Exception {
        return ConfigurazioneImport.carica("config/import/Binance CSV.json");
    }

    /**
     * Come {@link #cfg()} ma con una colonna controvalore sintetica (indice 7, aggiunta da
     * {@link #riga}): senza prezzo il movimento farebbe partire una ricerca in rete, che qui non
     * serve e renderebbe il test lento e non deterministico.
     */
    private static ConfigurazioneImport cfgVeloce() throws Exception {
        ConfigurazioneImport c = cfg();
        c.colonnaValoreEuro = 7;
        return c;
    }

    /** {@code User_ID,UTC_Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore sintetico */
    private static String[] riga(String time, String operation, String coin, String change) {
        return new String[]{"1", time, "Spot", operation, coin, change, "", "1.00"};
    }

    // ---- forma della configurazione reale --------------------------------------------------------

    @Test
    void configurazioneReale_dualSavingsMappatiSuTrasferimentoInternoConSottoWalletDedicato() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals("Binance", c.nomeExchange);
        assertEquals("Principale", c.nomeWallet);
        assertEquals("TRASFERIMENTO-CRYPTO-INTERNO", c.mappaCausali.get("Dual Savings Purchase"));
        assertEquals("TRASFERIMENTO-CRYPTO-INTERNO", c.mappaCausali.get("Dual Savings Settlement"));
        assertEquals("Investimenti", c.walletSpecularePerCausale.get("Dual Savings Purchase"));
        assertEquals("Investimenti", c.walletSpecularePerCausale.get("Dual Savings Settlement"));
    }

    /**
     * Senza questa voce un gruppo multi-riga (due settlement della stessa moneta nello stesso secondo:
     * succede sul dataset reale il 26/08 alle 08:40:36 e il 30/09 alle 09:43:55) verrebbe accumulato in
     * {@code TransazioneDefi} e riemesso da {@code RitornaScambi}, che chiama {@code creaMovimento} con
     * {@code TipoTr = null}: la causale andrebbe persa (DC/PC invece di TI) e le due gambe della stessa
     * moneta verrebbero <b>sommate in una</b>, perché la mappa dei token è indicizzata per simbolo.
     */
    @Test
    void configurazioneReale_trasferimentoInternoEUnaCausaleChiusa() throws Exception {
        assertTrue(cfg().causaliChiuse.contains("TRASFERIMENTO-CRYPTO-INTERNO"),
                "TRASFERIMENTO-CRYPTO-INTERNO deve essere chiusa, o i gruppi multi-riga perdono la causale");
    }

    // ---- le due gambe ---------------------------------------------------------------------------

    @Test
    void purchase_dueGambeStessoExchangeSottoWalletDiversi() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce());
        assertNotNull(movs, "causale riconosciuta");
        assertEquals(2, movs.size(), "una gamba principale + una speculare");

        String[] uscita = movs.get(0);
        String[] entrata = movs.get(1);

        assertEquals("Binance", uscita[3], "[3] non cambia: è la chiave del gruppo wallet fiscale");
        assertEquals("Binance", entrata[3], "[3] non cambia nemmeno sulla gamba speculare");
        assertEquals("Principale", uscita[4]);
        assertEquals("Investimenti", entrata[4], "la speculare va sul sotto-wallet dichiarato");

        assertEquals("USDT", uscita[8], "principale in uscita da Principale");
        assertEquals("-100", uscita[10]);
        assertEquals("USDT", entrata[11], "speculare in entrata su Investimenti");
        assertEquals("100", entrata[13]);
    }

    @Test
    void settlement_laGambaSpecularaEQuellaInUscita() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-24 08:33:41", "Dual Savings Settlement", "USDT", "102.00000000"),
                null, cfgVeloce());
        assertNotNull(movs);
        assertEquals(2, movs.size());

        String[] principale = movs.get(0);
        String[] speculare = movs.get(1);

        assertEquals("Principale", principale[4]);
        assertEquals("USDT", principale[11], "il rientro arriva su Principale");
        assertEquals("102", principale[13]);

        assertEquals("Investimenti", speculare[4]);
        assertEquals("USDT", speculare[8], "e lascia il comparto Investimenti");
        assertEquals("-102", speculare[10]);
    }

    /**
     * Entrambe le gambe sono TI: il ramo TI di {@code Calcoli_PlusvalenzeNew} non tocca il LIFO quando
     * la controparte è nello stesso gruppo wallet, ed è il caso qui perché {@code [3]} non cambia.
     */
    @Test
    void entrambeLeGambeSonoTrasferimentiInterni() throws Exception {
        for (String[] m : ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce())) {
            assertTrue(m[0].endsWith("_TI"), "categoria TI attesa nell'ID: " + m[0]);
            assertEquals("TRASFERIMENTO INTERNO", m[5]);
            assertEquals("", m[18], "campo18 vuoto: nessuna classificazione fiscale imposta");
        }
    }

    // ---- l'ordinamento: l'uscita prima dell'entrata ---------------------------------------------

    /**
     * {@code MappaCryptoWallet} è una {@code TreeMap} sull'ID, e le due gambe condividono data,
     * identificazione e categoria (TI entrambe): a decidere l'ordine resta il terzo campo dell'ID.
     * Si usa il terzo e non il quarto perché il quarto è quello che {@code getIDUnivoco} incrementa in
     * scrittura per risolvere le collisioni, e un ordinamento affidato a quello potrebbe essere
     * ribaltato da una collisione esterna.
     */
    @Test
    void purchase_lUscitaPrecedeLEntrataNellOrdinamentoDegliID() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce());
        String idUscita = movs.get(0)[0];      // Principale, -100
        String idEntrata = movs.get(1)[0];     // Investimenti, +100
        assertTrue(idUscita.compareToIgnoreCase(idEntrata) < 0,
                "l'uscita deve ordinarsi prima dell'entrata: " + idUscita + " vs " + idEntrata);
    }

    @Test
    void settlement_lUscitaPrecedeLEntrataAncheQuandoESpeculare() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-24 08:33:41", "Dual Savings Settlement", "USDT", "102.00000000"),
                null, cfgVeloce());
        String idEntrata = movs.get(0)[0];     // Principale, +102
        String idUscita = movs.get(1)[0];      // Investimenti, -102
        assertTrue(idUscita.compareToIgnoreCase(idEntrata) < 0,
                "sul settlement l'uscita è la gamba speculare, e deve ordinarsi prima: "
                + idUscita + " vs " + idEntrata);
    }

    /**
     * L'ordine deve reggere anche quando l'archivio contiene già un movimento con lo stesso ID: in
     * scrittura {@code ScriviListaSuMappaCrypto} passa ogni movimento per {@code getIDUnivoco}, che
     * incrementa il <b>quarto</b> campo. Finché a distinguere le due gambe è il terzo, una collisione
     * sposta {@code 001_001} in {@code 001_002}, che continua a precedere {@code 002_001}. È la ragione
     * per cui l'ordinamento non poteva stare sul quarto campo.
     */
    @Test
    void lOrdineReggeAncheSeGetIDUnivocoDeveRisolvereUnaCollisione() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce());

        //Archivio che occupa già gli ID di partenza di entrambe le gambe
        java.util.TreeMap<String, String[]> archivio =
                new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        archivio.put(movs.get(0)[0], new String[]{});
        archivio.put(movs.get(1)[0], new String[]{});

        String idUscita = MovimentiCrypto.getIDUnivoco(archivio, movs.get(0)[0]);
        archivio.put(idUscita, movs.get(0));
        String idEntrata = MovimentiCrypto.getIDUnivoco(archivio, movs.get(1)[0]);

        assertNotEquals(movs.get(0)[0], idUscita, "la collisione deve essere stata risolta davvero");
        assertTrue(idUscita.compareToIgnoreCase(idEntrata) < 0,
                "l'uscita resta prima anche dopo getIDUnivoco: " + idUscita + " vs " + idEntrata);
    }

    @Test
    void leDueGambeHannoIdDiversi() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce());
        assertNotEquals(movs.get(0)[0], movs.get(1)[0],
                "ID distinti per costruzione, non per intervento di getIDUnivoco in scrittura");
    }

    // ---- lo scarto diventa una giacenza negativa sul sotto-wallet -------------------------------

    /**
     * È il senso di tutto il meccanismo: 100 entrano nel comparto, 102 ne escono, e i 2 di rendimento
     * restano come giacenza negativa su {@code Binance;Investimenti;USDT} — che l'utente compensa
     * registrando la reward. {@code ControllaSaldiNegativi} costruisce la chiave con {@code [3]} e
     * {@code [4]}, quindi il sotto-wallet basta: non serve toccare {@code [3]}.
     */
    @Test
    void loScartoFraPurchaseESettlementDiventaGiacenzaNegativaSulSottoWallet() throws Exception {
        List<String[]> movimenti = new ArrayList<>();
        movimenti.addAll(ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"),
                null, cfgVeloce()));
        movimenti.addAll(ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-24 08:33:41", "Dual Savings Settlement", "USDT", "102.00000000"),
                null, cfgVeloce()));

        List<String> negativi = Funzioni.ControllaSaldiNegativi(movimenti);
        assertTrue(negativi.contains("Binance;Investimenti;USDT"),
                "atteso il comparto in negativo di 2 USDT, trovato: " + negativi);
    }

    /**
     * Il contatore mostrato sul pulsante degli errori è la dimensione di questa lista, e Principale
     * non ha bisogno di ricalcolarla: se il purchase manca del tutto (CSV caricati fuori ordine, o
     * storico incompleto) la segnalazione compare comunque, e sparisce da sé quando la riga arriva.
     */
    @Test
    void ilSoloSettlementSenzaPurchaseSegnalaComunqueIlComparto() throws Exception {
        List<String[]> movimenti = new ArrayList<>(ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-24 08:33:41", "Dual Savings Settlement", "USDT", "102.00000000"),
                null, cfgVeloce()));

        assertEquals(List.of("Binance;Investimenti;USDT"), Funzioni.ControllaSaldiNegativi(movimenti));
    }

    // ---- guardie ---------------------------------------------------------------------------------

    /**
     * Su una riga che muove già due monete lo speculare non è definito: si emette il solo movimento
     * principale invece di rispecchiarne una a caso.
     */
    @Test
    void suUnaRigaADueGambeLaSpeculareNonVieneEmessa() throws Exception {
        ConfigurazioneImport c = cfgVeloce();
        c.colonnaMonetaUscita = 8;
        c.colonnaQuantitaUscita = 9;
        String[] r = new String[]{"1", "2022-05-12 06:24:50", "Spot", "Dual Savings Purchase",
            "BTC", "0.005", "", "1.00", "USDT", "-100"};

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(r, null, c);
        assertNotNull(movs);
        assertEquals(1, movs.size(), "niente gamba speculare su un movimento a due monete");
    }

    /** Una causale senza la chiave continua a produrre un movimento solo. */
    @Test
    void unaCausaleSenzaChiaveNonCambiaComportamento() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("2022-05-12 06:24:50", "Staking Rewards", "BTC", "0.00000224"),
                null, cfgVeloce());
        assertNotNull(movs);
        assertEquals(1, movs.size());
        assertEquals("Principale", movs.get(0)[4]);
    }
}
