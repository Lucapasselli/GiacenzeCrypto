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
        // I tre movimenti sintetici (trasferimento, scambio, trasferimento) stanno sul sotto-wallet
        // "Dual Savings", non sulla "Piattaforma di scambio" generica degli altri scambi differiti.
        long sintetici = MappaCryptoWallet.values().stream()
                .filter(v -> "AU".equals(v[22])).peek(v -> assertEquals("Dual Savings", v[4])).count();
        assertEquals(3, sintetici);
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

        // Dal 2026-10-05 (bug C18) il Settlement non viene ridotto: porta l'intero liquidato, come nel CSV.
        // Prima portava solo il capitale e la reward stava sul wallet principale.
        assertEquals("102.00000000", s[13], "il settlement resta com'era nel CSV");
        assertEquals("1.00", s[15]);
        assertEquals("100.00000000", p[10].replace("-", ""), "il purchase resta com'era nel CSV");

        // Gamba speculare sul sotto-wallet "Dual Savings": esce l'intero liquidato (capitale + reward).
        String idMirrorSettlement = idSettlement.split("_")[0] + "_" + idSettlement.split("_")[1]
                + "_0" + idSettlement.split("_")[2] + "_" + idSettlement.split("_")[3] + "_PC";
        String[] mirrorSettlement = MappaCryptoWallet.get(idMirrorSettlement);
        assertNotNull(mirrorSettlement, "manca la gamba speculare del settlement sul sotto-wallet");
        assertEquals("Dual Savings", mirrorSettlement[4]);
        assertEquals(0, new BigDecimal("-102").compareTo(new BigDecimal(mirrorSettlement[10])));
        assertEquals(s[15], mirrorSettlement[15]);

        // Reward per la differenza (102 - 100 = 2), sul sotto-wallet: è lì che il contratto l'ha prodotta.
        String idReward = idSettlement.split("_")[0] + "_" + idSettlement.split("_")[1]
                + "_00" + idSettlement.split("_")[2] + "_" + idSettlement.split("_")[3] + "_DC";
        String[] reward = MappaCryptoWallet.get(idReward);
        assertNotNull(reward, "manca il movimento reward per la differenza");
        assertEquals("REWARD", reward[5]);
        assertTrue(reward[18].contains("DAI"), "la reward deve essere riconosciuta come deposito a costo (DAI)");
        assertEquals(s[3], reward[3]);
        assertEquals("Dual Savings", reward[4], "la reward entra sul sotto-wallet, da cui esce col capitale");
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

    // =============================================================================================
    // CHIAVE DI GRUPPO [43]: dal dettaglio di un movimento si vedono gli altri del contratto
    // =============================================================================================

    private static final String RIGA_STESSA_MONETA = "USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,"
            + "100.00000000 USDT,28500,2022-05-24 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled";

    private static String[] purchaseStessaMoneta() {
        String p[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String s[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "USDT", "102.00000000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(p[0], p);
        MappaCryptoWallet.put(s[0], s);
        return p;
    }

    @Test
    void stessaMoneta_tuttiIMovimentiDelContrattoPortanoLaStessaChiave() throws Exception {
        purchaseStessaMoneta();
        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));

        // Purchase, Settlement, le due gambe sul sotto-wallet e la reward: cinque movimenti
        java.util.List<String[]> delContratto = MappaCryptoWallet.values().stream()
                .filter(v -> "DUAL-1232611".equals(CommissioniCollegate.Chiave(v))).toList();
        assertEquals(5, delContratto.size());
        assertEquals(MappaCryptoWallet.size(), delContratto.size(), "nessun movimento resta fuori dal gruppo");
    }

    @Test
    void monetaDiversa_cinqueMovimentiPortanoLaStessaChiave() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String settlement[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "BTC", "0.00350000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(settlement[0], settlement);
        seedPrezzo("USDT", "2022-05-24 08:33:41");

        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(
                "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,0.00350000 BTC,Settled")));

        assertEquals(5, MappaCryptoWallet.size());
        for (String[] v : MappaCryptoWallet.values()) {
            assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(v), v[0]);
        }
    }

    @Test
    void contrattiDiversi_hannoChiaviDiverse() throws Exception {
        purchaseStessaMoneta();
        String p2[] = movimento("20220601062450_Binance_003_001_PC", "Dual Savings Purchase",
                "USDT", "50.00000000", "2022-06-01 06:24:50");
        String s2[] = movimento("20220610083341_Binance_004_001_DC", "Dual Savings Settlement",
                "USDT", "51.00000000", "2022-06-10 08:33:41");
        MappaCryptoWallet.put(p2[0], p2);
        MappaCryptoWallet.put(s2[0], s2);

        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA,
                "USDT/USDT,Buy Low,1999999,2022-06-01 08:24:50,Settled,50.00000000 USDT,28500,"
                + "2022-06-10 10:33:41,29351.32,146.28%,51.00000000 USDT,Settled")));

        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(MappaCryptoWallet.get("20220512062450_Binance_001_001_PC")));
        assertEquals("DUAL-1999999", CommissioniCollegate.Chiave(MappaCryptoWallet.get(p2[0])));
        assertEquals("DUAL-1999999", CommissioniCollegate.Chiave(MappaCryptoWallet.get(s2[0])));
    }

    @Test
    void unaCommissioneGiaCollegataAlPurchase_restaNelGruppoDelContratto() throws Exception {
        String p[] = purchaseStessaMoneta();
        String fee[] = movimento("20220512062450_Binance_001_002_CM", "Dual Savings Purchase",
                "USDT", "0.10000000", "2022-05-12 06:24:50");
        String chiaveFee = "chiave-della-commissione";
        p[CommissioniCollegate.CAMPO] = chiaveFee;
        fee[CommissioniCollegate.CAMPO] = chiaveFee;
        MappaCryptoWallet.put(fee[0], fee);

        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));

        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(fee), "la commissione segue il suo movimento");
        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(p));
        // Una commissione nel gruppo non lo rende un "gruppo di commissioni" da dissolvere
        assertFalse(Principale_CommissioniCollegate.isScollegabile(java.util.List.of(p[0])));
    }

    @Test
    void scollegaNonDissolveIlGruppoDiUnContratto() throws Exception {
        String p[] = purchaseStessaMoneta();
        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));

        assertFalse(Principale_CommissioniCollegate.isScollegabile(java.util.List.of(p[0])));
        Principale_CommissioniCollegate.Scollega(java.util.List.of(p[0]));
        for (String[] v : MappaCryptoWallet.values()) {
            assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(v), v[0]);
        }
    }

    @Test
    void ilDettaglioMostraGliAltriMovimentiDelContratto_senzaDireCheManchinoCommissioni() throws Exception {
        String p[] = purchaseStessaMoneta();
        String idSettlement = "20220524083341_Binance_002_001_DC";
        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));

        java.util.List<String[]> righe = Principale_CommissioniCollegate.RigheDettaglio(p[0]);
        assertEquals(1, righe.size(), "nessuna riga sulle commissioni mancanti");
        assertEquals("Contratto Dual Investment 1232611", righe.get(0)[0]);
        assertTrue(righe.get(0)[1].contains(idSettlement), righe.get(0)[1]);
        assertFalse(righe.get(0)[1].contains(p[0]), "il movimento non elenca se stesso");
    }

    @Test
    void unaCommissioneNelGruppoSiVedeComeCommissioneCollegata() throws Exception {
        String p[] = purchaseStessaMoneta();
        String fee[] = movimento("20220512062450_Binance_001_002_CM", "Dual Savings Purchase",
                "USDT", "0.10000000", "2022-05-12 06:24:50");
        p[CommissioniCollegate.CAMPO] = "k";
        fee[CommissioniCollegate.CAMPO] = "k";
        MappaCryptoWallet.put(fee[0], fee);
        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));

        java.util.List<String[]> righe = Principale_CommissioniCollegate.RigheDettaglio(p[0]);
        assertEquals("Contratto Dual Investment 1232611", righe.get(0)[0]);
        assertEquals("Commissioni collegate", righe.get(1)[0]);
        assertTrue(righe.get(1)[1].contains(fee[0]));
    }

    // =============================================================================================
    // RIPASSARE IL FILE SU CONTRATTI GIA' ABBINATI (da una versione che non scriveva il gruppo)
    // =============================================================================================

    private static final String RIGA_B_STESSA_MONETA = "USDT/USDT,Buy Low,1999999,2022-06-01 08:24:50,Settled,"
            + "100.00000000 USDT,28500,2022-06-10 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled";

    /** Riporta l'archivio allo stato di una versione precedente: abbinato, ma senza il gruppo del contratto. */
    private static void togliLeChiavi() {
        for (String[] v : MappaCryptoWallet.values()) v[CommissioniCollegate.CAMPO] = "";
    }

    private static String fotografia() {
        StringBuilder sb = new StringBuilder();
        for (String[] v : MappaCryptoWallet.values()) {
            sb.append(String.join("|", v)).append('\n');
        }
        return sb.toString();
    }

    @Test
    void ripassareIlFile_suContrattoGiaAbbinato_scriveSoloIlGruppo() throws Exception {
        purchaseStessaMoneta();
        String file = scriviDettaglio(RIGA_STESSA_MONETA);
        Binance_DualInvestment.Abbina(new java.io.File(file));
        togliLeChiavi();
        int movimentiPrima = MappaCryptoWallet.size();
        String s[] = MappaCryptoWallet.get("20220524083341_Binance_002_001_DC");
        String qtaSettlementPrima = s[13];
        String senzaChiavi = fotografia();

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(0, esito.abbinati, "non va rifatto l'abbinamento");
        assertEquals(1, esito.aggiornati, esito.dettagli.toString());
        assertEquals(0, esito.nonTrovati);
        assertEquals(movimentiPrima, MappaCryptoWallet.size(), "nessun movimento creato in più");
        assertEquals(qtaSettlementPrima, MappaCryptoWallet.get(s[0])[13], "la quantità non va ridotta due volte");
        for (String[] v : MappaCryptoWallet.values()) {
            assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(v), v[0]);
        }
        //Oltre al campo 43 non cambia nulla: la fotografia coincide una volta tolta la chiave
        togliLeChiavi();
        assertEquals(senzaChiavi, fotografia());
    }

    @Test
    void ripassareIlFile_suContrattoGiaAbbinatoAMonetaDiversa_scriveIlGruppo() throws Exception {
        String purchase[] = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-05-12 06:24:50");
        String settlement[] = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement",
                "BTC", "0.00350000", "2022-05-24 08:33:41");
        MappaCryptoWallet.put(purchase[0], purchase);
        MappaCryptoWallet.put(settlement[0], settlement);
        seedPrezzo("USDT", "2022-05-24 08:33:41");
        String file = scriviDettaglio("USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,"
                + "2022-05-24 10:33:41,29351.32,146.28%,0.00350000 BTC,Settled");
        Binance_DualInvestment.Abbina(new java.io.File(file));
        togliLeChiavi();

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.aggiornati, esito.dettagli.toString());
        assertEquals(5, MappaCryptoWallet.size());
        for (String[] v : MappaCryptoWallet.values()) {
            assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(v), v[0]);
        }
    }

    @Test
    void ripassareIlFile_unaTerzaVolta_nonCambiaNulla() throws Exception {
        purchaseStessaMoneta();
        String file = scriviDettaglio(RIGA_STESSA_MONETA);
        Binance_DualInvestment.Abbina(new java.io.File(file));
        String prima = fotografia();

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(0, esito.abbinati);
        assertEquals(0, esito.aggiornati);
        assertEquals(1, esito.giaAPosto);
        assertEquals(prima, fotografia());
    }

    @Test
    void ripassareIlFile_conUnContrattoVecchioEUnoNuovoDiUgualiImporti_nonLiScambia() throws Exception {
        //A abbinato da una versione precedente, B appena importato: stessa moneta e stessa quantità
        purchaseStessaMoneta();
        Binance_DualInvestment.Abbina(new java.io.File(scriviDettaglio(RIGA_STESSA_MONETA)));
        togliLeChiavi();
        String p2[] = movimento("20220601062450_Binance_003_001_PC", "Dual Savings Purchase",
                "USDT", "100.00000000", "2022-06-01 06:24:50");
        String s2[] = movimento("20220610083341_Binance_004_001_DC", "Dual Savings Settlement",
                "USDT", "102.00000000", "2022-06-10 08:33:41");
        MappaCryptoWallet.put(p2[0], p2);
        MappaCryptoWallet.put(s2[0], s2);

        //B prima di A nel file, per provare che l'ordine delle righe non conta
        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(
                scriviDettaglio(RIGA_B_STESSA_MONETA, RIGA_STESSA_MONETA)));

        assertEquals(1, esito.abbinati, esito.dettagli.toString());
        assertEquals(1, esito.aggiornati, esito.dettagli.toString());
        assertEquals(0, esito.nonTrovati);
        assertEquals(10, MappaCryptoWallet.size());
        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(MappaCryptoWallet.get("20220512062450_Binance_001_001_PC")));
        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(MappaCryptoWallet.get("20220524083341_Binance_002_001_DC")));
        assertEquals("DUAL-1999999", CommissioniCollegate.Chiave(MappaCryptoWallet.get(p2[0])));
        assertEquals("DUAL-1999999", CommissioniCollegate.Chiave(MappaCryptoWallet.get(s2[0])));
        assertEquals(5, MappaCryptoWallet.values().stream()
                .filter(v -> "DUAL-1999999".equals(CommissioniCollegate.Chiave(v))).count());
    }

    @Test
    void ripassareIlFile_unaCommissioneGiaCollegataAlPurchase_restaNelGruppoDelContratto() throws Exception {
        String p[] = purchaseStessaMoneta();
        String file = scriviDettaglio(RIGA_STESSA_MONETA);
        Binance_DualInvestment.Abbina(new java.io.File(file));
        togliLeChiavi();
        String fee[] = movimento("20220512062450_Binance_005_001_CM", "Dual Savings Purchase",
                "USDT", "0.10000000", "2022-05-12 06:24:50");
        MappaCryptoWallet.put(fee[0], fee);
        CommissioniCollegate.Collega(MappaCryptoWallet.get(p[0]), fee);
        assertFalse(CommissioniCollegate.Chiave(fee).isEmpty());

        Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals("DUAL-1232611", CommissioniCollegate.Chiave(fee), "la commissione segue il Purchase nel gruppo");
    }

    @Test
    void ripassareIlFile_conPurchaseAbbinatoESettlementNo_nonToccaNulla() throws Exception {
        purchaseStessaMoneta();
        String file = scriviDettaglio(RIGA_STESSA_MONETA);
        Binance_DualInvestment.Abbina(new java.io.File(file));
        togliLeChiavi();
        //Il Settlement torna da classificare: il contratto e' a meta'
        String id = "20220524083341_Binance_002_001_DC";
        String[] s = MappaCryptoWallet.get(id);
        s[5] = "DEPOSITO CRYPTO";
        s[13] = "102.00000000";
        s[18] = "";
        String prima = fotografia();

        Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new java.io.File(file));

        assertEquals(1, esito.nonTrovati);
        assertEquals(0, esito.aggiornati);
        assertEquals(prima, fotografia());
    }

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
