package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Porta nell'importatore generico l'abbinamento automatico "scambio differito" già presente negli
 * importatori CSV storici Binance/OKX ({@code Ex_Binance_Consolida} + {@code ConsolidaMovimentiDifferiti}
 * + {@code GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito}).
 *
 * <p>Tre cose fissate qui, oltre alla meccanica di base:</p>
 * <ul>
 *   <li>la tolleranza in minuti tra prelievo e deposito è ora <b>parametrica per configurazione</b>
 *       ({@code minutiScambioDifferito}, 15 di default come su Binance storico) invece che fissa nel
 *       codice;</li>
 *   <li>un movimento entra in <b>al più un</b> abbinamento ({@code break} nel doppio ciclo di
 *       {@code ConsolidaMovimentiDifferiti}): prima non c'era, e un prelievo con più depositi
 *       compatibili veniva abbinato più volte, con un secondo tentativo su un ID già rinominato dal
 *       primo;</li>
 *   <li>il filtro "movimento già esistente" di {@code ConsolidaMovimentiDifferiti} va valutato
 *       <b>prima</b> della scrittura in {@code MappaCryptoWallet}, non dopo: con
 *       {@code SovrascrivoEsistenti=false} (il default della casella "Sovrascrivere movimenti già
 *       presenti") un movimento appena scritto da questo stesso import risulta indistinguibile, guardando
 *       la sola mappa, da uno che c'era già da prima - e finiva sempre escluso, disattivando
 *       l'abbinamento automatico su ogni import normale. L'overload a 4 argomenti riceve lo snapshot
 *       calcolato dal chiamante prima della scrittura ({@link ImportazioneGenerica}) proprio per questo.</li>
 *   <li>prelievo/deposito si riconoscono dalla <b>categoria in coda all'ID</b> (PC/PF, DC/DF) più campo
 *       {@code [18]} vuoto (non ancora classificato), non dal testo di campo {@code [5]} — identico su
 *       un {@code TRASFERIMENTO-CRYPTO} qualunque, che passa dallo stesso ramo di fallback.</li>
 * </ul>
 *
 * <p>I controvalori sono sempre già presenti sui movimenti di partenza (come in
 * {@link DocumentiFonteEreditarietaTest}), così il prezzo non richiede una ricerca online.</p>
 */
class ImportazioneGenericaScambioDifferitoTest {

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
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID, String Campo5, String MonetaU, String QtaU,
            String MonetaE, String QtaE, String Prezzo, String Data) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = Data;
        v[2] = "1 di 1";
        v[3] = "Wallet Test";
        v[5] = Campo5;
        v[8] = MonetaU;
        v[9] = tipo(MonetaU);
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = tipo(MonetaE);
        v[13] = QtaE;
        v[15] = Prezzo;
        v[22] = "A";
        v[29] = "1710495000000";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    /**
     * Tipo di una moneta per {@code [9]}/{@code [12]}, come {@link Moneta#InserisciMonetaNoTipo}: FIAT
     * solo per EUR/USD. Senza questo una gamba EUR marcata "Crypto" fa cercare a
     * {@code Prezzi.DammiPrezzoTransazione} un prezzo "cripto" dell'euro, il che scatena una ricerca
     * online vera (CCXT/CoinMarketCap) invece del calcolo locale immediato di una gamba FIAT.
     */
    private static String tipo(String moneta) {
        if (moneta.isEmpty()) return "";
        return (moneta.equalsIgnoreCase("EUR") || moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
    }

    /** Calcola lo snapshot "già esistenti prima della scrittura", come fa {@link ImportazioneGenerica}. */
    private static Set<String> snapshotGiaEsistenti(List<String[]> differiti) {
        Set<String> s = new HashSet<>();
        for (String[] r : differiti) {
            if (MappaCryptoWallet.get(r[0]) != null) s.add(r[0]);
        }
        return s;
    }

    // =============================================================================================
    // MECCANICA DI BASE: abbinamento entro tolleranza, con lo snapshot pre-scrittura
    // =============================================================================================

    @Test
    void prelievoEDepositoEntroLaTolleranza_vengonoAbbinati() {
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        prelievo[25] = "BTC"; prelievo[26] = "BTC";
        String deposito[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:40"); // 10 minuti dopo
        deposito[27] = "EUR"; deposito[28] = "EUR";

        // Catturati PRIMA della chiamata: CreaMovimentiScambioCryptoDifferito riscrive [0] in-place
        // sugli stessi array (letti dalla mappa per riferimento), quindi prelievo[0]/deposito[0] dopo
        // la chiamata sono già gli ID nuovi, non quelli originali.
        String idPrelievoOriginale = prelievo[0];
        String idDepositoOriginale = deposito[0];

        List<String[]> lista = List.of(prelievo, deposito);
        List<String[]> differiti = new ArrayList<>(lista);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti); // vuoto: nessuno dei due è ancora in mappa

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti);

        assertNull(MappaCryptoWallet.get(idPrelievoOriginale), "il prelievo originale è stato rinumerato");
        assertNull(MappaCryptoWallet.get(idDepositoOriginale), "il deposito originale è stato rinumerato");
        assertNotNull(MappaCryptoWallet.get("20240315103000_00WalletTest_001_1_PC"), "prelievo rinumerato");
        assertNotNull(MappaCryptoWallet.get("20240315103000_01WalletTest_001_1_DC"), "trasferimento verso la piattaforma fittizia");
        assertNotNull(MappaCryptoWallet.get("20240315104000_02WalletTest_001_1_VC"), "lo scambio vero e proprio (vendita, deposito FIAT)");
        assertNotNull(MappaCryptoWallet.get("20240315104000_03WalletTest_001_1_PF"), "trasferimento di ritorno");
        assertNotNull(MappaCryptoWallet.get("20240315104000_04WalletTest_002_1_DF"), "deposito rinumerato");
    }

    @Test
    void prelievoEDepositoOltreLaTolleranza_nonVengonoAbbinati() {
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        String deposito[] = movimento("20240315104600_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:46"); // 16 minuti dopo: fuori tolleranza

        List<String[]> lista = List.of(prelievo, deposito);
        List<String[]> differiti = new ArrayList<>(lista);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti);

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti);

        assertNotNull(MappaCryptoWallet.get(prelievo[0]), "fuori tolleranza: il prelievo resta quello originale");
        assertNotNull(MappaCryptoWallet.get(deposito[0]), "fuori tolleranza: il deposito resta quello originale");
        assertEquals("PRELIEVO CRYPTO", MappaCryptoWallet.get(prelievo[0])[5]);
    }

    // =============================================================================================
    // TOLLERANZA PARAMETRICA: la stessa coppia si abbina o no a seconda del valore configurato
    // =============================================================================================

    @Test
    void laStessaCoppia_conTolleranzaStrettaNonSiAbbina_conTolleranzaLargaSi() {
        String data10min[] = {"2024-03-15 10:30", "2024-03-15 10:40"};

        // Tolleranza 5 minuti: 10 minuti di distanza sono troppi
        MappaCryptoWallet.clear();
        String prelievo5[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", data10min[0]);
        String deposito5[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", data10min[1]);
        List<String[]> lista5 = List.of(prelievo5, deposito5);
        List<String[]> differiti5 = new ArrayList<>(lista5);
        Set<String> giaEsistenti5 = snapshotGiaEsistenti(differiti5);
        Importazioni.ScriviListaSuMappaCrypto(lista5, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti5, false, 5, giaEsistenti5);
        assertNotNull(MappaCryptoWallet.get(prelievo5[0]), "tolleranza 5 min: 10 min di distanza non bastano ad abbinare");

        // Stessa coppia, tolleranza 15 minuti: si abbina
        MappaCryptoWallet.clear();
        String prelievo15[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", data10min[0]);
        prelievo15[25] = "BTC"; prelievo15[26] = "BTC";
        String deposito15[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", data10min[1]);
        deposito15[27] = "EUR"; deposito15[28] = "EUR";
        String idPrelievo15Originale = prelievo15[0]; // catturato prima: vedi nota sopra
        List<String[]> lista15 = List.of(prelievo15, deposito15);
        List<String[]> differiti15 = new ArrayList<>(lista15);
        Set<String> giaEsistenti15 = snapshotGiaEsistenti(differiti15);
        Importazioni.ScriviListaSuMappaCrypto(lista15, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti15, false, 15, giaEsistenti15);
        assertNull(MappaCryptoWallet.get(idPrelievo15Originale), "tolleranza 15 min: 10 min di distanza bastano ad abbinare");
    }

    // =============================================================================================
    // NIENTE DOPPIO ABBINAMENTO (break)
    // =============================================================================================

    @Test
    void unPrelievoConDueDepositiCompatibili_siAbbinaSoloAlPrimo() {
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        prelievo[25] = "BTC"; prelievo[26] = "BTC";
        // Due depositi entrambi compatibili (entro tolleranza e prezzo)
        String depositoA[] = movimento("20240315103500_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:35");
        depositoA[27] = "EUR"; depositoA[28] = "EUR";
        String depositoB[] = movimento("20240315104000_WalletTest_003_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:40");
        depositoB[27] = "EUR"; depositoB[28] = "EUR";

        // Catturati prima: chi viene abbinato ha [0] riscritto in-place dalla chiamata sotto.
        String idPrelievoOriginale = prelievo[0];
        String idDepositoAOriginale = depositoA[0];
        String idDepositoBOriginale = depositoB[0];

        List<String[]> lista = List.of(prelievo, depositoA, depositoB);
        List<String[]> differiti = new ArrayList<>(lista);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti);

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        // Prima della correzione (nessun break) questa chiamata avrebbe tentato di abbinare il
        // prelievo anche al secondo deposito, sul cui ID CreaMovimentiScambioCryptoDifferito avrebbe
        // agito una seconda volta con MovimentoPrelievo ormai rinumerato dal primo abbinamento.
        assertDoesNotThrow(() ->
                Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti));

        assertNull(MappaCryptoWallet.get(idPrelievoOriginale), "il prelievo è stato abbinato (al primo deposito compatibile)");
        assertNull(MappaCryptoWallet.get(idDepositoAOriginale), "il primo deposito è stato consumato dall'abbinamento");
        assertNotNull(MappaCryptoWallet.get(idDepositoBOriginale),
                "il secondo deposito, pur compatibile, non deve essere toccato: il prelievo si ferma al primo");
        assertEquals("DEPOSITO FIAT", MappaCryptoWallet.get(idDepositoBOriginale)[5]);
    }

    // =============================================================================================
    // IL FILTRO "GIÀ ESISTENTE" VA VALUTATO PRIMA DELLA SCRITTURA, NON DOPO
    // =============================================================================================

    @Test
    void senzaLoSnapshotPreScrittura_unImportNormaleNonAbbinaMai() {
        // Stesso identico scenario di prelievoEDepositoEntroLaTolleranza_vengonoAbbinati, ma usando
        // l'overload storico (senza lo snapshot, equivalente a "giaEsistentiPrimaDellImport = null"):
        // dimostra il difetto che lo snapshot corregge, per non doverlo scoprire di nuovo.
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        String deposito[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:40");

        List<String[]> lista = List.of(prelievo, deposito);
        List<String[]> differiti = new ArrayList<>(lista);

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        // SovrascrivoEsistenti=false, nessuno snapshot: il controllo storico rilevato DOPO la
        // scrittura trova entrambi "già in mappa" (li ha appena scritti lui) e li esclude.
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15);

        assertNotNull(MappaCryptoWallet.get(prelievo[0]),
                "senza lo snapshot pre-scrittura l'abbinamento non scatta su un import normale (difetto storico)");
    }

    // =============================================================================================
    // consolidaGruppo POPOLA LA LISTA "differiti" PER LA CAUSALE SCAMBIO DIFFERITO
    // =============================================================================================

    private static ConfigurazioneImport cfgBinanceVeloce() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 7; // controvalore sintetico: niente ricerca prezzi online
        return c;
    }

    /** {@code User_ID,UTC_Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore sintetico */
    private static String[] rigaBinance(String time, String operation, String coin, String change) {
        return new String[]{"1", time, "Spot", operation, coin, change, "", "1.00"};
    }

    @Test
    void consolidaGruppo_gruppoDiUnaRiga_popolaDifferitiPerAutoInvest() throws Exception {
        ConfigurazioneImport cfg = cfgBinanceVeloce();
        assertEquals("SCAMBIO DIFFERITO", cfg.mappaCausali.get("Auto-Invest Transaction"),
                "precondizione: la causale reale di Binance deve mappare su SCAMBIO DIFFERITO");
        assertTrue(cfg.causaliChiuse.contains("SCAMBIO DIFFERITO"),
                "precondizione: deve essere fra le causaliChiuse (movimento singolo anche in gruppo)");

        List<String[]> differiti = new ArrayList<>();
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2022-05-12 06:24:50", "Auto-Invest Transaction", "BTC", "-0.01"));

        List<String[]> risultato = ImportazioneGenerica.consolidaGruppo(gruppo, cfg, differiti);

        assertEquals(1, risultato.size());
        assertEquals(1, differiti.size(), "il movimento va anche nella lista da riesaminare");
        assertSame(risultato.get(0), differiti.get(0), "stesso oggetto riga: ScriviListaSuMappaCrypto lo rinumera in-place per entrambi");
    }

    @Test
    void consolidaGruppo_causaleNonDifferita_nonTocca_differiti() throws Exception {
        ConfigurazioneImport cfg = cfgBinanceVeloce();
        List<String[]> differiti = new ArrayList<>();
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2022-05-12 06:24:50", "Staking Rewards", "BTC", "0.001"));

        ImportazioneGenerica.consolidaGruppo(gruppo, cfg, differiti);

        assertTrue(differiti.isEmpty(), "una causale diversa da SCAMBIO DIFFERITO non deve finire nella lista");
    }

    // =============================================================================================
    // IL CRITERIO E' LA CATEGORIA IN CODA ALL'ID (PC/PF/DC/DF) + CAMPO18 VUOTO, NON IL TESTO DI CAMPO5
    // =============================================================================================
    //
    // Campo5 ("PRELIEVO CRYPTO"/"DEPOSITO FIAT"...) e' identico su un TRASFERIMENTO-CRYPTO qualunque:
    // entrambe le causali passano dallo stesso ramo di fallback di creaMovimento quando il TipoTr non e'
    // nella mappa interna. Non e' quindi un discriminante valido preso da solo - lo scoping vero e' a
    // monte (solo le righe SCAMBIO DIFFERITO finiscono in movimentiDifferiti), ma la funzione deve
    // comunque riconoscere correttamente prelievo/deposito fra le righe che le arrivano.

    @Test
    void unaGambaFiat_vieneRiconosciutaComePrelievoODeposito() {
        // PF (prelievo FIAT) <-> DC (deposito crypto): "acquisto differito", il simmetrico del caso
        // PC<->DF già coperto sopra. Prova che il riconoscimento non e' ristretto al solo crypto-crypto.
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PF", "PRELIEVO FIAT",
                "EUR", "-20000", "", "", "20000.00", "2024-03-15 10:30");
        prelievo[25] = "EUR"; prelievo[26] = "EUR";
        String deposito[] = movimento("20240315104000_WalletTest_002_1_DC", "DEPOSITO CRYPTO",
                "", "", "BTC", "0.5", "20000.00", "2024-03-15 10:40");
        deposito[27] = "BTC"; deposito[28] = "BTC";

        String idPrelievoOriginale = prelievo[0];
        String idDepositoOriginale = deposito[0];

        List<String[]> lista = List.of(prelievo, deposito);
        List<String[]> differiti = new ArrayList<>(lista);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti);

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti);

        assertNull(MappaCryptoWallet.get(idPrelievoOriginale), "il prelievo FIAT deve essere stato riconosciuto e abbinato");
        assertNull(MappaCryptoWallet.get(idDepositoOriginale), "il deposito crypto deve essere stato riconosciuto e abbinato");
    }

    @Test
    void unMovimentoGiaClassificato_nonVieneRiconsiderato() {
        // Stessa coppia di prelievoEDepositoEntroLaTolleranza_vengonoAbbinati, ma il prelievo ha già
        // campo18 valorizzato (come dopo un primo abbinamento, o una classificazione manuale): la
        // categoria nell'ID è ancora PC (la rinumerazione non la cambia), ma non va più toccato.
        String prelievo[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        prelievo[18] = "PTW - Scambio Differito"; // già classificato
        String deposito[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:40");

        List<String[]> lista = List.of(prelievo, deposito);
        List<String[]> differiti = new ArrayList<>(lista);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti);

        Importazioni.ScriviListaSuMappaCrypto(lista, false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti);

        assertNotNull(MappaCryptoWallet.get(prelievo[0]),
                "un prelievo già classificato (campo18 valorizzato) non deve essere riabbinato");
        assertEquals("PTW - Scambio Differito", MappaCryptoWallet.get(prelievo[0])[18]);
    }

    @Test
    void unTrasferimentoOrdinario_conCampo5IdenticoAUnoScambioDifferito_nonSiConfonde() {
        // TRASFERIMENTO-CRYPTO produce lo stesso campo5 ("PRELIEVO CRYPTO"/"DEPOSITO CRYPTO") e la stessa
        // categoria (PC/DC) di SCAMBIO DIFFERITO, perché entrambe le causali passano dallo stesso ramo
        // di fallback di creaMovimento. La prova che oggi non si confondono non sta nel controllo dentro
        // ConsolidaMovimentiDifferiti (che da solo non li distinguerebbe), ma nel fatto che un
        // TRASFERIMENTO-CRYPTO non entra mai in movimentiDifferiti/listaScambiDifferiti a monte: qui lo
        // dimostriamo passando esplicitamente solo la riga SCAMBIO DIFFERITO, come fa il codice reale.
        String prelievoDifferito[] = movimento("20240315103000_WalletTest_001_1_PC", "PRELIEVO CRYPTO",
                "BTC", "-0.5", "", "", "20000.00", "2024-03-15 10:30");
        prelievoDifferito[25] = "BTC"; prelievoDifferito[26] = "BTC";
        // Un prelievo "normale" (TRASFERIMENTO-CRYPTO), stesso campo5/categoria, MAI passato a
        // ConsolidaMovimentiDifferiti: solo scritto in mappa, a simulare che coesista nell'archivio.
        String prelievoOrdinario[] = movimento("20240315103100_WalletTest_005_1_PC", "PRELIEVO CRYPTO",
                "ETH", "-1", "", "", "3000.00", "2024-03-15 10:31");
        String deposito[] = movimento("20240315104000_WalletTest_002_1_DF", "DEPOSITO FIAT",
                "", "", "EUR", "20000", "20000.00", "2024-03-15 10:40");
        deposito[27] = "EUR"; deposito[28] = "EUR";

        List<String[]> tuttiScrittiInMappa = List.of(prelievoDifferito, prelievoOrdinario, deposito);
        // Solo il prelievo SCAMBIO DIFFERITO e il deposito vanno alla funzione: il prelievo ordinario
        // resta fuori, come farebbe il vero import (mai aggiunto a movimentiDifferiti).
        List<String[]> differiti = List.of(prelievoDifferito, deposito);
        Set<String> giaEsistenti = snapshotGiaEsistenti(differiti);

        Importazioni.ScriviListaSuMappaCrypto(new ArrayList<>(tuttiScrittiInMappa), false);
        Importazioni.ConsolidaMovimentiDifferiti(differiti, false, 15, giaEsistenti);

        assertNotNull(MappaCryptoWallet.get(prelievoOrdinario[0]),
                "il prelievo ordinario, mai passato alla funzione, non deve mai essere toccato");
        assertEquals("PRELIEVO CRYPTO", MappaCryptoWallet.get(prelievoOrdinario[0])[5]);
    }
}
