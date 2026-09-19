package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link Binance_DualInvestment}: l'abbinamento dei movimenti "Dual Savings Purchase"/"Dual
 * Savings Settlement" (importati come TRASFERIMENTO-CRYPTO, categoria PC/DC, campo18 vuoto) tramite il
 * file "Advanced Earn - Dual Investment History" scaricabile da Binance, che porta l'informazione che
 * il CSV normale non ha: quale importo uscito corrisponde a quale importo rientrato, anche a mesi di
 * distanza e in una moneta diversa.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>il filtro decisivo è il campo {@code [7]} (causale grezza): un prelievo/deposito ordinario con
 *       la stessa moneta/quantità non deve mai essere abbinato per errore;</li>
 *   <li>quando più candidati condividono moneta e quantità, vince quello cronologicamente più vicino
 *       al contratto (frequente nel dataset reale: più Buy Low da 100 USDT in date diverse);</li>
 *   <li>un pareggio esatto fra due candidati equidistanti resta "ambiguo", non abbinato a caso;</li>
 *   <li>un contratto ancora aperto (Status ≠ Settled) è contato ma non richiede alcun controparte.</li>
 * </ul>
 */
class Binance_DualInvestmentTest {

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

    /** Movimento PC ("Dual Savings Purchase" per default) o DC, con [7] impostabile per i casi negativi. */
    private static String[] movimento(String ID, String causale, String moneta, String qta, String data) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[5] = ID.endsWith("_PC") ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        v[7] = causale;
        if (ID.endsWith("_PC")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "1.00";
        v[22] = "A";
        v[29] = "1710495000000";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    /**
     * Precarica un prezzo in cache ({@code PrezziNew} di {@code prezzi.mv.db}) perché {@code
     * CreaMovimentiScambioCryptoDifferito} non richieda una ricerca online: il minuto deve combaciare
     * con quello della data del movimento di deposito/settlement, l'unico che la funzione interroga
     * (tolleranza propria della cache, ±5 minuti - vedi {@code Prezzi.DammiPrezzoDaDatabase}).
     */
    private static void seedPrezzo(String simbolo, String dataMovimentoSettlement) throws Exception {
        long ts = FunzioniDate.ConvertiDatainLongMinuto(dataMovimentoSettlement);
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, "binance");
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new java.math.BigDecimal("1.00"));
            ps.executeUpdate();
        }
    }

    private static String scriviDettaglio(String... righe) throws IOException {
        StringBuilder sb = new StringBuilder(
                "Product,Order Type,Product Id,Subscription Date,Type,Subscription Amount,Target Price,"
                + "Settlement Date,Fixing Price,APY,Settlement Amount,Status\n");
        for (String r : righe) sb.append(r).append("\n");
        Path f = tempDir.resolve("dettaglio_" + System.nanoTime() + "_202609182038UTC+2.csv");
        Files.writeString(f, sb.toString());
        return f.toString();
    }

    // =============================================================================================
    // CASO BASE: un solo candidato per lato, si abbina
    // =============================================================================================

    @Test
    void unicoCandidatoPerLato_monetaDiversa_siAbbinaComeScambioDifferito() throws Exception {
        // Moneta sottoscritta (USDT) diversa da quella liquidata (BTC): vera permuta, passa da
        // CreaMovimentiScambioCryptoDifferito, che rinumera entrambi gli endpoint.
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String settlement[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "BTC", "0.00350000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(settlement[0], settlement);
        // Catturati PRIMA della chiamata: CreaMovimentiScambioCryptoDifferito riscrive [0] in-place
        // sugli stessi array (letti dalla mappa per riferimento).
        String idPurchaseOriginale = purchase[0];
        String idSettlementOriginale = settlement[0];
        // USDT è sempre la moneta prioritaria per il prezzo quando presente in una delle due gambe
        // (Prezzi.DammiPrezzoInfoTransazione, punto 3), indipendentemente da quale lato la porti.
        seedPrezzo("USDT", "2022-05-24 08:33:41");

        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,0.00350000 BTC,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.abbinati, esito.dettagli.toString());
        assertEquals(0, esito.ambigui);
        assertEquals(0, esito.nonTrovati);
        assertNull(MappaCryptoWallet.get(idPurchaseOriginale), "il purchase è stato rinumerato dall'abbinamento");
        assertNull(MappaCryptoWallet.get(idSettlementOriginale), "il settlement è stato rinumerato dall'abbinamento");
    }

    // =============================================================================================
    // LIQUIDAZIONE NELLA STESSA MONETA SOTTOSCRITTA: TI verso/da "Dual Savings" + reward separata
    // =============================================================================================

    @Test
    void unicoCandidatoPerLato_stessaMoneta_capitaleTrasferitoEDifferenzaComeReward() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String settlement[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "USDT", "102.00000000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(settlement[0], settlement);
        String idPurchase = purchase[0];
        String idSettlement = settlement[0];

        String file = scriviDettaglio(
                "USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.abbinati, esito.dettagli.toString());

        // Purchase e Settlement mantengono il proprio ID: qui non c'è rinumerazione, solo mutazione
        // in-place, come CreaMovimentoTrasferimentoA/Da del meccanismo Vault generico.
        String[] p = MappaCryptoWallet.get(idPurchase);
        String[] s = MappaCryptoWallet.get(idSettlement);
        assertNotNull(p);
        assertNotNull(s);
        assertTrue(p[18].contains("PTW"), "il purchase deve restare un TI in uscita (PTW)");
        assertTrue(s[18].contains("DTW"), "il settlement deve restare un TI in entrata (DTW)");

        // Il settlement, dopo lo sdoppiamento, porta solo il capitale (100), non l'intero liquidato (102).
        assertEquals(0, new BigDecimal("100").compareTo(new BigDecimal(s[13])),
                "il settlement deve riportare solo il capitale, il resto va alla reward");

        // Gamba speculare sul sotto-wallet "Dual Savings": uscita di 100 in corrispondenza del rientro.
        String idMirrorSettlement = idSettlement.split("_")[0] + "_" + idSettlement.split("_")[1]
                + "_0" + idSettlement.split("_")[2] + "_" + idSettlement.split("_")[3] + "_PC";
        String[] mirrorSettlement = MappaCryptoWallet.get(idMirrorSettlement);
        assertNotNull(mirrorSettlement, "manca la gamba speculare del settlement sul sotto-wallet");
        assertEquals("Dual Savings", mirrorSettlement[4]);
        assertEquals(0, new BigDecimal("-100").compareTo(new BigDecimal(mirrorSettlement[10])));

        // Reward separata per la differenza (102 - 100 = 2), sul wallet principale (non sul sotto-wallet).
        String idReward = idSettlement.split("_")[0] + "_" + idSettlement.split("_")[1]
                + "_00" + idSettlement.split("_")[2] + "_" + idSettlement.split("_")[3] + "_DC";
        String[] reward = MappaCryptoWallet.get(idReward);
        assertNotNull(reward, "manca il movimento reward per la differenza");
        assertEquals("REWARD", reward[5]);
        assertTrue(reward[18].contains("DAI"), "la reward deve essere riconosciuta come deposito a costo (DAI)");
        assertEquals(s[3], reward[3]);
        assertEquals(s[4], reward[4], "la reward arriva sul wallet reale, non sul sotto-wallet Dual Savings");
        assertEquals(0, new BigDecimal("2").compareTo(new BigDecimal(reward[13])));
    }

    @Test
    void unicoCandidatoPerLato_stessaMoneta_liquidatoUgualeAlSottoscritto_nessunaReward() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String settlement[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "USDT", "100.00000000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(settlement[0], settlement);
        String idSettlement = settlement[0];

        String file = scriviDettaglio(
                "USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,0.00%,100.00000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.abbinati, esito.dettagli.toString());
        String idReward = idSettlement.split("_")[0] + "_" + idSettlement.split("_")[1]
                + "_00" + idSettlement.split("_")[2] + "_" + idSettlement.split("_")[3] + "_DC";
        assertNull(MappaCryptoWallet.get(idReward), "nessuna reward se liquidato == sottoscritto");
    }

    // =============================================================================================
    // FILTRO SUL CAMPO [7]: un prelievo ordinario con stessa moneta/quantità non va confuso
    // =============================================================================================

    @Test
    void unPrelievoOrdinario_conStessaMonetaEQuantita_nonVieneAbbinato() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        // Stessa moneta/quantità, ma è un prelievo vero (withdraw), non un Dual Savings
        String prelievoOrdinario[] = movimento("20220512063000_Binance_005_001_PC", "withdraw",
                "USDT", "100.00000000", "2022-05-12 06:30:00");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(prelievoOrdinario[0], prelievoOrdinario);

        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        // Manca il settlement: il contratto non si abbina comunque, ma quel che conta è che il
        // prelievo ordinario non sia mai stato scelto al posto del vero purchase.
        assertEquals(0, esito.abbinati);
        assertNotNull(MappaCryptoWallet.get(prelievoOrdinario[0]), "il prelievo ordinario non va toccato");
        assertEquals("withdraw", MappaCryptoWallet.get(prelievoOrdinario[0])[7]);
    }

    // =============================================================================================
    // PIÙ CANDIDATI CON STESSA MONETA/QUANTITÀ: vince il più vicino in data
    // =============================================================================================

    @Test
    void dueCandidatiStessaMonetaEQuantita_vinceIlPiuVicinoInData() throws Exception {
        String purchaseLontano[] = movimento("20220513110000_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-13 11:00:00");
        String purchaseVicino[] = movimento("20220519091718_Binance_002_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-19 09:17:18");
        String settlement[] = movimento("20220729111102_Binance_003_001_DC", "Dual Savings Settlement",
                "USDT", "103.17000000", "2022-07-29 11:11:02");
        MappaCryptoWallet.put(purchaseLontano[0], purchaseLontano);
        MappaCryptoWallet.put(purchaseVicino[0], purchaseVicino);
        MappaCryptoWallet.put(settlement[0], settlement);
        String idPurchaseVicinoOriginale = purchaseVicino[0];

        // Subscription Date coincide (a meno del fuso +2h già gestito) con il purchase "vicino"
        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232613,2022-05-19 11:17:18,Settled,100.00000000 USDT,20000,"
                + "2022-07-29 13:11:02,23919.55,16.34%,103.17000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.abbinati, esito.dettagli.toString());
        // Liquidazione nella stessa moneta sottoscritta: qui non c'è rinumerazione (vedi il gruppo di
        // test dedicato), il candidato abbinato resta al suo ID ma viene riclassificato come TI.
        String[] vicino = MappaCryptoWallet.get(idPurchaseVicinoOriginale);
        assertNotNull(vicino, "il candidato più vicino in data resta al suo ID, solo riclassificato");
        assertTrue(vicino[18].contains("PTW"), "il candidato più vicino in data è stato abbinato");
        assertNotNull(MappaCryptoWallet.get(purchaseLontano[0]), "il candidato lontano non va toccato");
        assertTrue(MappaCryptoWallet.get(purchaseLontano[0])[18].isBlank(), "il candidato lontano non va toccato");
    }

    // =============================================================================================
    // PAREGGIO ESATTO: resta ambiguo, nessuno dei due va abbinato a caso
    // =============================================================================================

    @Test
    void dueCandidatiEquidistanti_restanoAmbigui() throws Exception {
        // Le date dei movimenti sono lette come ora di Roma (FunzioniDate.ConvertiDatainLongSecondo), che
        // a maggio è CEST = UTC+2, lo stesso offset dichiarato nel nome del file di dettaglio: per essere
        // equidistanti in UTC vero le ore di parete devono differire di 15s l'una dall'altra e di 15s dal
        // target "11:17:15" del dettaglio, non essere "prima" dell'offset.
        String primo[] = movimento("20220519111700_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-19 11:17:00");
        String secondo[] = movimento("20220519111730_Binance_002_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-19 11:17:30");
        // Il contratto cerca un istante esattamente a metà: 15 secondi da entrambi
        String settlement[] = movimento("20220729111102_Binance_003_001_DC", "Dual Savings Settlement",
                "USDT", "103.17000000", "2022-07-29 11:11:02");
        MappaCryptoWallet.put(primo[0], primo);
        MappaCryptoWallet.put(secondo[0], secondo);
        MappaCryptoWallet.put(settlement[0], settlement);

        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232613,2022-05-19 11:17:15,Settled,100.00000000 USDT,20000,"
                + "2022-07-29 13:11:02,23919.55,16.34%,103.17000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(0, esito.abbinati);
        assertEquals(1, esito.ambigui);
        assertNotNull(MappaCryptoWallet.get(primo[0]));
        assertNotNull(MappaCryptoWallet.get(secondo[0]));
    }

    // =============================================================================================
    // CONTRATTO NON ANCORA LIQUIDATO: contato, nessun tentativo di abbinamento
    // =============================================================================================

    @Test
    void contrattoNonAncoraLiquidato_vieneSoloContato() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        MappaCryptoWallet.put(purchase[0], purchase);

        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Purchased,100.00000000 USDT,28500,"
                + ",,,,Purchased");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.contrattiTotali);
        assertEquals(1, esito.nonAncoraLiquidati);
        assertEquals(0, esito.abbinati);
        assertNotNull(MappaCryptoWallet.get(purchase[0]), "un contratto aperto non deve toccare il purchase");
    }

    // =============================================================================================
    // NESSUN CANDIDATO: non trovato, non un errore
    // =============================================================================================

    @Test
    void nessunCandidato_none_trovato() throws Exception {
        String file = scriviDettaglio(
                "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled");

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.nonTrovati);
        assertEquals(0, esito.abbinati);
    }

    // =============================================================================================
    // UTILITY DI PARSING
    // =============================================================================================

    @Test
    void estraiOffset_riconosceIlFormatoSenzaParentesi() {
        assertEquals(2, Binance_DualInvestment.estraiOffset(
                "Binance_Advanced_Earn—Dual_Investment_History_202609182038UTC+2.csv"));
        assertEquals(-5, Binance_DualInvestment.estraiOffset("export_UTC-5.csv"));
        assertEquals(0, Binance_DualInvestment.estraiOffset("nessun_offset.csv"));
    }

    @Test
    void parseImportoMoneta_separaQuantitaEMoneta() {
        assertArrayEquals(new String[]{"0.00001474", "BTC"},
                Binance_DualInvestment.parseImportoMoneta("0.00001474 BTC"));
        assertArrayEquals(new String[]{"100.00000000", "USDT"},
                Binance_DualInvestment.parseImportoMoneta("100.00000000 USDT"));
        assertNull(Binance_DualInvestment.parseImportoMoneta("formato non valido"));
    }
}
