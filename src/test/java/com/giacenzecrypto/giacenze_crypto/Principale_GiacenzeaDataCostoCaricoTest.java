package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di CARATTERIZZAZIONE del costo di carico delle rimanenze mostrato dalla tabella
 * "Giacenze a data" ({@link Principale_GiacenzeaData#CalcolaCostiCaricoRimanenze(long)}).
 * <p>
 * I lotti non vengono valorizzati qui: il costo arriva da {@code v[16]}/{@code v[17]}, scritti dal
 * motore delle plusvalenze, quindi ogni scenario fa girare prima
 * {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenze()} e solo dopo interroga le pile. Un test che
 * costruisse i costi a mano fotograferebbe se stesso.
 */
class Principale_GiacenzeaDataCostoCaricoTest {

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
    void setUp() {
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
    }

    // ------------------------------------------------------------------
    // Costruzione dei movimenti
    // ------------------------------------------------------------------

    private static String[] movimento(String data, String wallet, String tipoID, String classificazione,
            String descrizione,
            String monetaU, String tipoU, String qtaU,
            String monetaE, String tipoE, String qtaE,
            String valore) {
        String id = data.replace(":", ".") + "_TEST_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = wallet;
        m[4] = wallet;
        m[5] = descrizione;
        m[8] = monetaU;
        m[9] = tipoU;
        m[10] = qtaU;
        m[11] = monetaE;
        m[12] = tipoE;
        m[13] = qtaE;
        m[15] = valore;
        m[18] = classificazione;
        m[22] = "M";
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    private static String[] acquisto(String data, String moneta, String qta, String valore) {
        return acquisto(data, "TestWallet", moneta, qta, valore);
    }

    private static String[] acquisto(String data, String wallet, String moneta, String qta, String valore) {
        return movimento(data, wallet, "AC", "", "ACQUISTO", "EUR", "FIAT", valore, moneta, "Crypto", qta, valore);
    }

    private static String[] vendita(String data, String moneta, String qta, String valore) {
        return movimento(data, "TestWallet", "VC", "", "VENDITA", moneta, "Crypto", qta, "EUR", "FIAT", valore, valore);
    }

    /** La data di riferimento come la calcola la tabella: fine della giornata scelta. */
    private static long fineGiornata(String giorno) {
        return FunzioniDate.ConvertiDatainLong(giorno) + 86400000L;
    }

    private static String costo(long dataRiferimento, String wallet, String moneta, String qta) {
        return Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(dataRiferimento)
                .CostoDelleRimanenze(wallet, Principale_GiacenzeaData.ChiaveRiga(moneta, "Crypto", "", ""), qta);
    }

    // ------------------------------------------------------------------
    // Scenari
    // ------------------------------------------------------------------

    @Test
    void dopoUnaVenditaParzialeRestaIlCostoDelLottoPiuVecchio() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "BTC", "1", "2000");
        vendita("2024-03-01 10:00", "BTC", "1", "2500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Il LIFO ha venduto il lotto da 2000 : resta 1 BTC caricato a 1000
        assertEquals("1000.00", costo(fineGiornata("2024-12-31"), "Tutti", "BTC", "1"));
    }

    @Test
    void laDataDiRiferimentoTagliaIMovimentiSuccessivi() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "BTC", "1", "2000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", costo(fineGiornata("2024-01-15"), "Tutti", "BTC", "1"));
        assertEquals("3000.00", costo(fineGiornata("2024-02-15"), "Tutti", "BTC", "2"));
    }

    @Test
    void unaGiacenzaParzialeNeConsumaSoloLaParteAcquistataPerUltima() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "BTC", "1", "3000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Mezzo BTC : LIFO, quindi metà del lotto più recente
        assertEquals("1500.00", costo(fineGiornata("2024-12-31"), "Tutti", "BTC", "0.5"));
    }

    @Test
    void unaGiacenzaNegativaONullaNonHaCostoDiCarico() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        vendita("2024-02-01 10:00", "BTC", "2", "3000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.00", costo(fineGiornata("2024-12-31"), "Tutti", "BTC", "-1"));
        assertEquals("0.00", costo(fineGiornata("2024-12-31"), "Tutti", "BTC", "0"));
    }

    @Test
    void unaMonetaSenzaLottiNonInventaUnCosto() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("0.00", costo(fineGiornata("2024-12-31"), "Tutti", "ETH", "5"));
    }

    /**
     * Lo stesso simbolo su due reti diverse è due righe distinte nella tabella: se condividessero
     * la pila, ognuna delle due leggerebbe il lotto dell'altra e il costo verrebbe contato due volte.
     */
    @Test
    void loStessoSimboloSuDueRetiHaDuePileDistinte() {
        String[] eth = acquisto("2024-01-01 10:00", "USDT", "1000", "900");
        eth[34] = "ethereum";
        eth[28] = "0xAAA";
        String[] bsc = acquisto("2024-02-01 10:00", "USDT", "1000", "950");
        bsc[34] = "bsc";
        bsc[28] = "0xBBB";

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"));
        assertEquals("900.00", costi.CostoDelleRimanenze("Tutti",
                Principale_GiacenzeaData.ChiaveRiga("USDT", "Crypto", "0xAAA", "ethereum"), "1000"));
        assertEquals("950.00", costi.CostoDelleRimanenze("Tutti",
                Principale_GiacenzeaData.ChiaveRiga("USDT", "Crypto", "0xBBB", "bsc"), "1000"));
    }

    /**
     * Il caso per cui la passata non può essere filtrata per wallet: un giroconto interno non porta
     * con sé nessun costo di carico (il motore lascia {@code v[16]} e {@code v[17]} vuoti), quindi una
     * pila costruita sui soli movimenti del wallet di destinazione mostrerebbe la giacenza a costo zero.
     */
    @Test
    void unaMonetaArrivataConUnGiroContoInternoConservaIlSuoCostoDiCarico() {
        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "1", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Il giroconto non sposta nulla (v[16] e v[17] restano vuoti), ma il costo resta leggibile
        assertEquals("", prelievo[16]);
        assertEquals("", deposito[17]);
        assertEquals("1000.00", costo(fineGiornata("2024-12-31"), "WalletB", "BTC", "1"));
    }

    /**
     * Con le plusvalenze divise per gruppo wallet, invece, un trasferimento fra due gruppi sposta
     * davvero il costo di carico: il gruppo di partenza resta senza, quello di arrivo se lo trova.
     */
    @Test
    void conPlusvalenzeXGruppoIlTrasferimentoSpostaIlCostoDiCarico() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "1", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", deposito[17], "il motore deve aver spostato il costo di carico sul deposito");

        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"));
        String chiave = Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", "");
        assertEquals("1000.00", costi.CostoDelleRimanenze("WalletB", chiave, "1"));
        assertEquals("0.00", costi.CostoDelleRimanenze("WalletA", chiave, "1"));
        //Su "Tutti" il costo non si duplica : è sempre lo stesso BTC
        assertEquals("1000.00", costi.CostoDelleRimanenze("Tutti", chiave, "1"));
    }

    /**
     * Su "Tutti" i lotti dei vari gruppi si uniscono e si riordinano per data: se la giacenza mostrata
     * è <b>inferiore</b> alla somma delle pile, il LIFO consuma i lotti più recenti a prescindere dal
     * gruppo in cui si trovano. È una scelta, non un caso: la domanda a cui la colonna risponde è
     * "quanto è costato quello che resta", e su una selezione larga la risposta LIFO è la stessa che
     * darebbe una pila sola.
     */
    @Test
    void suTuttiUnaGiacenzaInferioreAllaSommaDellePileConsumaILottiPiuRecenti() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "WalletB", "BTC", "1", "4000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"));
        String chiave = Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", "");
        //1 BTC su 2 disponibili : viene preso il lotto di febbraio, che sta in un altro gruppo
        assertEquals("4000.00", costi.CostoDelleRimanenze("Tutti", chiave, "1"));
        assertEquals("4500.00", costi.CostoDelleRimanenze("Tutti", chiave, "1.5"));
    }

    @Test
    void unaPassataInterrottaSiFermaSubito() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Interrotto sempre vero : la passata esce al primo controllo e non carica nessun lotto
        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"), () -> true);
        assertEquals("0.00", costi.CostoDelleRimanenze("Tutti",
                Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", ""), "1"));
    }

    @Test
    void laSelezioneDiUnGruppoLeggeLaPilaDiQuelGruppo() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "WalletB", "BTC", "1", "4000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"));
        String chiave = Principale_GiacenzeaData.ChiaveRiga("BTC", "Crypto", "", "");
        assertEquals("1000.00", costi.CostoDelleRimanenze("Gruppo : Wallet 01 ( alias )", chiave, "1"));
        assertEquals("4000.00", costi.CostoDelleRimanenze("Gruppo : Wallet 02 ( alias )", chiave, "1"));
        assertEquals("5000.00", costi.CostoDelleRimanenze("Tutti", chiave, "2"));
    }

    // ------------------------------------------------------------------
    // Giacenze prima e dopo un movimento (dettaglio movimento)
    // ------------------------------------------------------------------

    private static Principale_GiacenzeaData.GiacenzeMoneta giacenza(String id, String moneta) {
        for (Principale_GiacenzeaData.GiacenzeMoneta g : Principale_GiacenzeaData.GiacenzeAttornoAlMovimento(id)) {
            if (g.Moneta.equals(moneta)) return g;
        }
        fail("Nessuna giacenza per " + moneta);
        return null;
    }

    private static final int GLOBALE = Principale_GiacenzeaData.LIVELLO_GLOBALE;
    private static final int GRUPPO = Principale_GiacenzeaData.LIVELLO_GRUPPO;
    private static final int WALLET = Principale_GiacenzeaData.LIVELLO_WALLET;

    @Test
    void attornoAUnaVenditaLaGiacenzaPerdeIlLottoPiuRecente() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "BTC", "1", "2000");
        //Nei dati reali la quantità in uscita è negativa, e le giacenze la sommano così com'è
        String[] vendita = vendita("2024-03-01 10:00", "BTC", "-1", "2500");
        //Un movimento successivo non deve entrare né nel "prima" né nel "dopo"
        acquisto("2024-04-01 10:00", "BTC", "5", "9000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.GiacenzeMoneta btc = giacenza(vendita[0], "BTC");
        assertEquals(0, new java.math.BigDecimal("2").compareTo(btc.QtaPrima[GLOBALE]));
        assertEquals("3000.00", btc.CostoPrima[GLOBALE]);
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(btc.QtaDopo[GLOBALE]));
        assertEquals("1000.00", btc.CostoDopo[GLOBALE]);
        assertEquals(0, new java.math.BigDecimal("2500").compareTo(btc.PrezzoUnitario));
        //Stesso wallet e stesso gruppo : i tre livelli coincidono
        assertEquals(btc.QtaDopo[GLOBALE], btc.QtaDopo[WALLET]);
        assertEquals(btc.CostoDopo[GLOBALE], btc.CostoDopo[GRUPPO]);

        //La gamba in euro : solo quantità, nessun costo di carico
        Principale_GiacenzeaData.GiacenzeMoneta eur = giacenza(vendita[0], "EUR");
        assertTrue(eur.isFiat());
        assertNull(eur.CostoDopo[GLOBALE]);
    }

    @Test
    void unDepositoDaUnAltroGruppoSpostaIlCostoAlGruppoDiArrivo() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "-1", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.GiacenzeMoneta dep = giacenza(deposito[0], "BTC");
        //Globale : fra prelievo e deposito il BTC è "in viaggio", le quantità sono sommate come in
        //Giacenze a data e il prelievo l'ha già tolto. Arrivato, torna con il suo costo
        assertEquals(0, dep.QtaPrima[GLOBALE].signum());
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(dep.QtaDopo[GLOBALE]));
        assertEquals("1000.00", dep.CostoDopo[GLOBALE]);
        //Gruppo di arrivo : da zero a 1 BTC con il suo costo
        assertEquals(0, dep.QtaPrima[GRUPPO].signum());
        assertEquals("0.00", dep.CostoPrima[GRUPPO]);
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(dep.QtaDopo[GRUPPO]));
        assertEquals("1000.00", dep.CostoDopo[GRUPPO]);
        assertEquals("1000.00", dep.CostoDopo[WALLET]);

        //Sul prelievo il wallet si svuota subito, il costo lascia il gruppo solo al deposito
        Principale_GiacenzeaData.GiacenzeMoneta pre = giacenza(prelievo[0], "BTC");
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(pre.QtaPrima[WALLET]));
        assertEquals(0, pre.QtaDopo[WALLET].signum());
        assertEquals("1000.00", pre.CostoPrima[GRUPPO]);
    }

    @Test
    void unGiroContoDentroLoStessoGruppoMuoveIWalletMaNonIlGruppo() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 01");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "2", "2000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "-1", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.GiacenzeMoneta dep = giacenza(deposito[0], "BTC");
        //Prima del deposito il BTC è già uscito da WalletA : il gruppo ne ha 1, dopo di nuovo 2
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(dep.QtaPrima[GRUPPO]));
        assertEquals(0, new java.math.BigDecimal("2").compareTo(dep.QtaDopo[GRUPPO]));
        assertEquals(0, dep.QtaPrima[WALLET].signum());
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(dep.QtaDopo[WALLET]));
        //Nessun costo si è mosso : la pila del gruppo è sempre quella dell'acquisto
        assertEquals("2000.00", dep.CostoDopo[GRUPPO]);
        assertEquals("1000.00", dep.CostoDopo[WALLET]);
    }

    /**
     * Il lato che il test sul deposito non vede: il gruppo di partenza deve davvero perdere il lotto
     * trasferito. Se la passata filtrata perdesse lo scarico della controparte, il gruppo di partenza
     * leggerebbe ancora il lotto da 3000 invece di quello da 1000.
     */
    @Test
    void ilGruppoDiPartenzaPerdeIlLottoTrasferito() {
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletA", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("WalletB", "Wallet 02");

        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-01-15 10:00", "WalletA", "BTC", "1", "3000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "-1", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];
        String[] successivo = acquisto("2024-03-01 10:00", "WalletA", "BTC", "1", "5000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.GiacenzeMoneta g = giacenza(successivo[0], "BTC");
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(g.QtaPrima[GRUPPO]));
        assertEquals("1000.00", g.CostoPrima[GRUPPO]);
        assertEquals("6000.00", g.CostoDopo[GRUPPO]);
    }

    /**
     * Un giroconto nello stesso gruppo non ha costo di carico scritto dal motore: il dettaglio ne mostra
     * uno informativo, il costo dei lotti più recenti che coprono la quantità mossa. Sul deposito i lotti
     * sono ancora quelli, perché il prelievo verso un wallet proprio non scarica la pila.
     */
    @Test
    void unGiroContoSenzaCostoNeHaUnoInformativo() {
        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-01-15 10:00", "WalletA", "BTC", "1", "3000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "-1.5", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1.5", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];
        String[] vendita = vendita("2024-03-01 10:00", "BTC", "-0.5", "2000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", prelievo[16]);
        assertEquals("", deposito[17]);
        //1.5 BTC : tutto il lotto da 3000 più metà di quello da 1000
        Principale_GiacenzeaData.DettaglioGiacenze p = Principale_GiacenzeaData.CalcolaDettaglio(prelievo[0]);
        assertEquals("3500.00", p.CostoInformativo[0]);
        assertNull(p.CostoInformativo[1]);
        assertEquals("3500.00", Principale_GiacenzeaData.CalcolaDettaglio(deposito[0]).CostoInformativo[1]);
        //Dove il motore il costo l'ha scritto, niente costo informativo
        assertNull(Principale_GiacenzeaData.CalcolaDettaglio(vendita[0]).CostoInformativo[0]);
        assertEquals("2333.3333333333", GUI_DettaglioTransazione.CostoUnitario("3500.00", "-1.5"));
    }

    @Test
    void leRigheDelDettaglioSonoUnaPerMoneta() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        //Nei dati reali la quantità in uscita è negativa, e le giacenze la sommano così com'è
        String[] vendita = vendita("2024-03-01 10:00", "BTC", "-1", "2500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        java.util.List<String[]> righe = Principale_GiacenzeaData.RigheDettaglio(vendita[0]);
        assertEquals(2, righe.size());
        assertEquals("Giacenze BTC", righe.get(0)[0]);
        assertTrue(righe.get(0)[1].contains("€ 1000.00"), righe.get(0)[1]);
    }

    // ------------------------------------------------------------------
    // Colonne di costo della tabella dettaglio movimenti e colonne derivate della tabella principale
    // ------------------------------------------------------------------

    /**
     * Fa scorrere i movimenti come il ciclo che riempie la tabella dettaglio: tutti i movimenti alla
     * passata LIFO, e la quantità residua sommata solo per i movimenti del wallet scelto (o tutti).
     * @return il costo di carico residuo dopo l'ultimo movimento, come lo leggerebbe l'ultima riga
     */
    private static String costoResiduoDopoUltimoMovimento(String wallet, String moneta) {
        Principale_GiacenzeaData.CostiDettaglioToken passata =
                new Principale_GiacenzeaData.CostiDettaglioToken(wallet, moneta, "Crypto", "", "");
        java.math.BigDecimal qta = java.math.BigDecimal.ZERO;
        String costo = "0.00";
        for (String[] m : Principale.MappaCryptoWallet.values()) {
            passata.Avanza(m);
            boolean delWallet = wallet.equalsIgnoreCase("tutti") || wallet.equalsIgnoreCase(m[3]);
            if (!delWallet) {
                continue;
            }
            if (m[8].equals(moneta)) {
                qta = qta.add(new java.math.BigDecimal(m[10]));
            }
            if (m[11].equals(moneta)) {
                qta = qta.add(new java.math.BigDecimal(m[13]));
            }
            costo = passata.CostoResiduo(qta.toPlainString());
        }
        return costo;
    }

    /**
     * L'ultima riga del dettaglio deve coincidere con la colonna "Costo Carico" della tabella principale,
     * con "Tutti" e con un singolo wallet. Il caso del singolo wallet è quello che rompe una passata
     * filtrata: WalletB riceve il BTC con un giroconto interno, che non porta costo con sé.
     */
    @Test
    void lUltimaRigaDelDettaglioCoincideConLaTabellaPrincipale() {
        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-01-15 10:00", "WalletA", "BTC", "1", "3000");
        String[] prelievo = movimento("2024-02-01 10:00", "WalletA", "PC", "PTW - Prelievo verso wallet proprio",
                "TRASFERIMENTO", "BTC", "Crypto", "-1.5", "", "", "", "0.00");
        String[] deposito = movimento("2024-02-01 10:01", "WalletB", "DC", "DTW - Deposito da wallet proprio",
                "TRASFERIMENTO", "", "", "", "BTC", "Crypto", "1.5", "0.00");
        prelievo[20] = deposito[0];
        deposito[20] = prelievo[0];

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        long data = fineGiornata("2024-12-31");
        assertEquals("4000.00", costo(data, "Tutti", "BTC", "2"));
        assertEquals("4000.00", costoResiduoDopoUltimoMovimento("Tutti", "BTC"));
        //WalletB ha 1.5 BTC : tutto il lotto da 3000 più metà di quello da 1000
        assertEquals("3500.00", costo(data, "WalletB", "BTC", "1.5"));
        assertEquals("3500.00", costoResiduoDopoUltimoMovimento("WalletB", "BTC"));
    }

    @Test
    void lePilePassanoPerTuttiIMovimentiAncheQuelliDiAltriWallet() {
        acquisto("2024-01-01 10:00", "WalletA", "BTC", "1", "1000");
        acquisto("2024-02-01 10:00", "WalletB", "BTC", "1", "2000");
        vendita("2024-03-01 10:00", "BTC", "-1", "2500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //La vendita è di TestWallet ma scarica la pila unica (PlusXWallet spenta): la riga di WalletA
        //vede il proprio BTC al costo del lotto che resta, quello da 1000
        assertEquals("1000.00", costoResiduoDopoUltimoMovimento("Tutti", "BTC"));
        Principale_GiacenzeaData.CostiDettaglioToken passata =
                new Principale_GiacenzeaData.CostiDettaglioToken("WalletA", "BTC", "Crypto", "", "");
        String[] primo = Principale.MappaCryptoWallet.firstEntry().getValue();
        passata.Avanza(primo);
        assertEquals("1000.00", passata.CostoResiduo("1"));
    }

    @Test
    void leColonneCostiDelDettaglioPerUnaRigaDiAcquistoEUnaDiVendita() {
        String[] acq = acquisto("2024-01-01 10:00", "BTC", "2", "1000");
        String[] ven = vendita("2024-03-01 10:00", "BTC", "-1", "2500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiDettaglioToken passata =
                new Principale_GiacenzeaData.CostiDettaglioToken("Tutti", "BTC", "Crypto", "", "");
        passata.Avanza(acq);
        //Costo del movimento, prezzo unitario, valore della qta residua, costo della qta residua
        assertArrayEquals(new String[]{"1000.00", "500", "1000.00", "1000.00"},
                Principale_GiacenzeaData.ColonneCostiDettaglio(passata, acq, true, "2", "2"));
        passata.Avanza(ven);
        //Venduto 1 BTC di 2 : il costo del movimento è quello del BTC uscito, resta la metà
        String[] colVendita = Principale_GiacenzeaData.ColonneCostiDettaglio(passata, ven, false, "-1", "1");
        assertEquals("500.00", colVendita[0]);
        assertEquals("500.00", colVendita[3]);
    }

    @Test
    void laGambaFiatNonHaColonneDiCosto() {
        String[] acq = acquisto("2024-01-01 10:00", "BTC", "1", "1000");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiDettaglioToken passata =
                new Principale_GiacenzeaData.CostiDettaglioToken("Tutti", "EUR", "FIAT", "", "");
        passata.Avanza(acq);
        //La gamba in uscita dell'acquisto è l'euro speso
        assertArrayEquals(new String[]{"", "", "", ""},
                Principale_GiacenzeaData.ColonneCostiDettaglio(passata, acq, false, "1000", "-1000"));
    }

    @Test
    void lePilaDelDettaglioNonSiConfondeConUnaMonetaDiversa() {
        acquisto("2024-01-01 10:00", "BTC", "1", "1000");
        acquisto("2024-01-02 10:00", "ETH", "10", "500");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("1000.00", costoResiduoDopoUltimoMovimento("Tutti", "BTC"));
        assertEquals("500.00", costoResiduoDopoUltimoMovimento("Tutti", "ETH"));
    }

    @Test
    void ilCostoEsattoNonArrotondaEIlCostoMostratoSi() {
        acquisto("2024-01-01 10:00", "SHIB", "1000000", "0.004");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Principale_GiacenzeaData.CostiCaricoRimanenze costi =
                Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(fineGiornata("2024-12-31"));
        String chiave = Principale_GiacenzeaData.ChiaveRiga("SHIB", "Crypto", "", "");
        assertEquals("0.00", costi.CostoDelleRimanenze("Tutti", chiave, "1000000"));
        assertEquals(0, new java.math.BigDecimal("0.004").compareTo(
                costi.CostoEsattoDelleRimanenze("Tutti", chiave, "1000000")));
    }

    @Test
    void leColonneDerivateDellaTabellaPrincipale() {
        java.math.BigDecimal costoEsatto = new java.math.BigDecimal("60");
        Double[] d = Principale_GiacenzeaData.ValoriDerivatiRiga("Crypto", "2", new java.math.BigDecimal("50"),
                100.0, 60.0, costoEsatto);
        assertEquals(50.0, d[0]);
        assertEquals(30.0, d[1]);
        assertEquals(40.0, d[2]);

        //Token senza prezzo : niente differenza, che sarebbe l'intero costo mostrato come perdita
        d = Principale_GiacenzeaData.ValoriDerivatiRiga("Crypto", "2", null, 0.0, 60.0, costoEsatto);
        assertNull(d[0]);
        assertEquals(30.0, d[1]);
        assertNull(d[2]);

        //FIAT, giacenza nulla o negativa : nessuna colonna
        for (Double[] vuoto : new Double[][]{
            Principale_GiacenzeaData.ValoriDerivatiRiga("FIAT", "100", java.math.BigDecimal.ONE, 100.0, 0.0, java.math.BigDecimal.ZERO),
            Principale_GiacenzeaData.ValoriDerivatiRiga("Crypto", "0", java.math.BigDecimal.ONE, 0.0, 0.0, java.math.BigDecimal.ZERO),
            Principale_GiacenzeaData.ValoriDerivatiRiga("Crypto", "-3", java.math.BigDecimal.ONE, -3.0, 0.0, java.math.BigDecimal.ZERO)}) {
            assertNull(vuoto[0]);
            assertNull(vuoto[1]);
            assertNull(vuoto[2]);
        }
    }

    @Test
    void ilValoreUnitarioNonSiScriveInNotazioneScientifica() {
        assertEquals("0.0000000015", Principale_GiacenzeaData.FormattaUnitario(1.5E-9));
        assertEquals("1234.5678", Principale_GiacenzeaData.FormattaUnitario(1234.5678));
        assertEquals("0", Principale_GiacenzeaData.FormattaUnitario(0.0));
        assertEquals("", Principale_GiacenzeaData.FormattaUnitario(null));
    }
}
