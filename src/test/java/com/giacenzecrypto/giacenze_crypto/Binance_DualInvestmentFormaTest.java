package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Fissa la forma dei contratti Dual Investment introdotta il 2026-10-05 e la sistemazione dei contratti già
 * abbinati quando si ripassa il file di dettaglio.
 *
 * <ul>
 *   <li><b>Moneta uguale (bug C18)</b>: Purchase e Settlement restano come nel CSV, la reward entra sul
 *       sotto-wallet Dual Savings, da cui esce l'intero liquidato, e i movimenti del contratto formano un solo
 *       gruppo {@code [20]}. Annullare da un movimento qualunque riporta Purchase e Settlement allo stato
 *       importato. I contratti nella forma precedente (Settlement ridotto al capitale, reward sul wallet
 *       principale, {@code [20]} in un verso solo) vengono convertiti senza cambiare i risultati del motore.</li>
 *   <li><b>Moneta diversa (bug C17)</b>: un contratto il cui scambio differito era stato sovrascritto da un
 *       altro liquidato nello stesso secondo viene ricostruito, senza toccare l'altro. Se non si può, nulla
 *       cambia.</li>
 * </ul>
 * Le forme "vecchie" sono costruite a mano, perché il codice attuale non le produce più.
 */
class Binance_DualInvestmentFormaTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
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
        Principale.Mappa_EMoney.clear();
    }

    private static final String ID_P = "20220512062450_Binance_001_001_PC";
    private static final String ID_S = "20220524083341_Binance_002_001_DC";
    private static final String RIGA_STESSA_MONETA = "USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,"
            + "100.00000000 USDT,28500,2022-05-24 10:33:41,29351.32,146.28%,102.00000000 USDT,Settled";

    private static String[] movimento(String ID, String causale, String moneta, String qta, String data, String valore) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = ID.endsWith("_PC") ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        v[7] = causale;
        if (ID.endsWith("_PC")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = valore;
        v[22] = "A";
        v[29] = "1710495000000";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static String[] purchase() {
        return movimento(ID_P, "Dual Savings Purchase", "USDT", "100.00000000", "2022-05-12 06:24:50", "100.00");
    }

    private static String[] settlement() {
        return movimento(ID_S, "Dual Savings Settlement", "USDT", "102.00000000", "2022-05-24 08:33:41", "102.00");
    }

    private static String file(String... righe) throws IOException {
        StringBuilder sb = new StringBuilder("Product,Order Type,Product Id,Subscription Date,Type,Subscription Amount,"
                + "Target Price,Settlement Date,Fixing Price,APY,Settlement Amount,Status\n");
        for (String r : righe) sb.append(r).append("\n");
        Path f = tempDir.resolve("dettaglio_" + System.nanoTime() + "_202609182038UTC+2.csv");
        Files.writeString(f, sb.toString());
        return f.toString();
    }

    private static Binance_DualInvestment.Esito abbina(String... righe) throws IOException {
        return Binance_DualInvestment.Abbina(new java.io.File(file(righe)));
    }

    /** La riga senza il gruppo del contratto ([43]), che resta per scelta anche dopo un annullamento. */
    private static String senzaGruppo(String[] v) {
        String[] c = v.clone();
        c[GruppoOperazione.CAMPO] = "";
        return String.join("|", c);
    }

    private static Map<String, String> fotografia() {
        Map<String, String> m = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) m.put(v[0], String.join("|", v));
        return m;
    }

    /** Il contratto come lo lasciava l'abbinamento prima del 2026-10-05. */
    private static List<String[]> formaPrecedente(String[] p, String[] s) {
        String[] sp = p[0].split("_"), ss = s[0].split("_");
        String[] mirrorP = movimento(sp[0] + "_" + sp[1] + "_" + sp[2] + "A_" + sp[3] + "_DC", "", "USDT", "100", p[1], "100.00");
        mirrorP[4] = "Dual Savings"; mirrorP[5] = "TRASFERIMENTO INTERNO"; mirrorP[18] = "DTW - Trasferimento Interno"; mirrorP[22] = "AU";
        String[] mirrorS = movimento(ss[0] + "_" + ss[1] + "_0" + ss[2] + "_" + ss[3] + "_PC", "", "USDT", "100", s[1], "100.00");
        mirrorS[4] = "Dual Savings"; mirrorS[5] = "TRASFERIMENTO INTERNO"; mirrorS[18] = "PTW - Trasferimento Interno"; mirrorS[22] = "AU";
        String[] reward = movimento(ss[0] + "_" + ss[1] + "_00" + ss[2] + "_" + ss[3] + "_DC", "", "USDT", "2", s[1], "2.00");
        reward[5] = "REWARD"; reward[18] = Binance_DualInvestment.CAMPO18_REWARD; reward[22] = "AU";
        reward[20] = mirrorS[0];
        p[5] = "TRASFERIMENTO A DUAL INVESTMENT"; p[18] = Binance_DualInvestment.CAMPO18_PURCHASE_STESSA_MONETA; p[20] = mirrorP[0];
        s[5] = "TRASFERIMENTO DA DUAL INVESTMENT"; s[18] = Binance_DualInvestment.CAMPO18_SETTLEMENT_STESSA_MONETA;
        s[13] = "100"; s[15] = "100.00";
        s[20] = mirrorS[0] + "," + reward[0];
        return List.of(mirrorP, mirrorS, reward);
    }

    /** Ogni riga del contratto cita tutte le altre e solo quelle. */
    private static void assertGruppoCompleto(List<String[]> contratto) {
        for (String[] v : contratto) {
            List<String> citati = Arrays.asList(v[20].split(","));
            assertEquals(contratto.size() - 1, citati.size(), v[0] + " -> " + v[20]);
            for (String[] a : contratto) if (a != v) assertTrue(citati.contains(a[0]), v[0] + " non cita " + a[0]);
        }
    }

    private static List<String[]> contrattoDi(String[] p) {
        List<String[]> c = new ArrayList<>();
        c.add(p);
        for (String ID : p[20].split(",")) c.add(MappaCryptoWallet.get(ID));
        return c;
    }

    // =============================================================================================
    // MONETA UGUALE, FORMA ATTUALE
    // =============================================================================================

    @Test
    void stessaMoneta_unSoloGruppoSimmetricoDiCinqueMovimenti() throws Exception {
        String[] p = purchase();
        settlement();
        assertEquals(1, abbina(RIGA_STESSA_MONETA).abbinati);
        assertEquals(5, MappaCryptoWallet.size());
        assertGruppoCompleto(contrattoDi(p));
    }

    @Test
    void stessaMoneta_laRewardPrecedeLUscitaDalSottoWallet() throws Exception {
        purchase();
        settlement();
        abbina(RIGA_STESSA_MONETA);
        String reward = null, uscita = null;
        for (String[] v : MappaCryptoWallet.values()) {
            if (Binance_DualInvestment.CAMPO18_REWARD.equals(v[18])) reward = v[0];
            if ("Dual Savings".equals(v[4]) && v[18].contains("PTW")) uscita = v[0];
        }
        assertNotNull(reward);
        assertNotNull(uscita);
        //Il controllo delle giacenze negative scorre per ID: la reward deve entrare prima che il liquidato esca
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(reward, uscita) < 0, reward + " / " + uscita);
    }

    @Test
    void stessaMoneta_sottoWalletAZeroEGiacenzaDelGruppoInvariata() throws Exception {
        purchase();
        settlement();
        abbina(RIGA_STESSA_MONETA);
        BigDecimal dual = BigDecimal.ZERO, totale = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            BigDecimal q = (v[10].isBlank() ? BigDecimal.ZERO : new BigDecimal(v[10]))
                    .add(v[13].isBlank() ? BigDecimal.ZERO : new BigDecimal(v[13]));
            totale = totale.add(q);
            if ("Dual Savings".equals(v[4])) dual = dual.add(q);
        }
        assertEquals(0, dual.signum(), "il sotto-wallet si svuota alla liquidazione");
        assertEquals(0, new BigDecimal("2").compareTo(totale), "nel gruppo resta solo la reward");
    }

    @Test
    void stessaMoneta_annullareDaQualunqueMovimento_riportaPurchaseESettlementAlloStatoImportato() throws Exception {
        for (int i = 0; i < 5; i++) {
            MappaCryptoWallet.clear();
            String[] p = purchase();
            String[] s = settlement();
            String pPrima = senzaGruppo(p), sPrima = senzaGruppo(s);
            abbina(RIGA_STESSA_MONETA);
            String[] membro = contrattoDi(p).get(i);
            String ID = membro[0];
            if ("AU".equals(membro[22])) {
                //Un movimento generato non si modifica: si elimina, e la sua eliminazione annulla il contratto
                Funzioni.RimuoviMovimentazioneXID(ID);
            } else {
                //Come Modifica Movimento: annulla la classificazione dell'intero gruppo
                GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((ID + "," + membro[20]).split(","), ID);
            }
            assertEquals(2, MappaCryptoWallet.size(), "restano solo Purchase e Settlement, partendo da " + ID);
            assertEquals(pPrima, senzaGruppo(MappaCryptoWallet.get(ID_P)), "Purchase, partendo da " + ID);
            assertEquals(sPrima, senzaGruppo(MappaCryptoWallet.get(ID_S)), "Settlement, partendo da " + ID);
        }
    }

    @Test
    void stessaMoneta_dopoLAnnullamentoIlFileLoRiabbina() throws Exception {
        purchase();
        settlement();
        abbina(RIGA_STESSA_MONETA);
        String[] s = MappaCryptoWallet.get(ID_S);
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((ID_S + "," + s[20]).split(","), ID_S);

        Binance_DualInvestment.Esito esito = abbina(RIGA_STESSA_MONETA);

        assertEquals(1, esito.abbinati, esito.dettagli.toString());
        assertEquals(5, MappaCryptoWallet.size());
    }

    @Test
    void stessaMoneta_liquidatoMenoDelSottoscritto_nonAbbinaENonCreaNulla() throws Exception {
        purchase();
        movimento(ID_S, "Dual Savings Settlement", "USDT", "98.00000000", "2022-05-24 08:33:41", "98.00");
        Map<String, String> prima = fotografia();

        Binance_DualInvestment.Esito esito = abbina("USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,"
                + "100.00000000 USDT,28500,2022-05-24 10:33:41,29351.32,146.28%,98.00000000 USDT,Settled");

        assertEquals(0, esito.abbinati);
        assertEquals(1, esito.nonTrovati);
        assertEquals(prima, fotografia());
    }

    // =============================================================================================
    // MONETA UGUALE, CONTRATTI NELLA FORMA PRECEDENTE
    // =============================================================================================

    @Test
    void formaPrecedente_vieneConvertita_eUnaSecondaVoltaNonCambiaNulla() {
        String[] p = purchase();
        String[] s = settlement();
        List<String[]> generati = formaPrecedente(p, s);
        String[] mirrorS = generati.get(1), reward = generati.get(2);

        assertEquals(MovimentiCollegati.EsitoRiparazione.RIPARATO, Binance_DualInvestment.AggiornaFormaStessaMoneta(p, s));

        assertEquals(0, new BigDecimal("102").compareTo(new BigDecimal(s[13])), "il Settlement riprende la reward");
        assertEquals("102.00", s[15]);
        assertEquals("Dual Savings", reward[4]);
        assertEquals(0, new BigDecimal("-102").compareTo(new BigDecimal(mirrorS[10])));
        assertEquals("102.00", mirrorS[15]);
        assertGruppoCompleto(contrattoDi(p));

        Map<String, String> dopo = fotografia();
        assertEquals(MovimentiCollegati.EsitoRiparazione.NON_SERVE, Binance_DualInvestment.AggiornaFormaStessaMoneta(p, s));
        assertEquals(dopo, fotografia());
    }

    @Test
    void formaPrecedente_ripassandoIlFile_vieneConvertita() throws Exception {
        String[] p = purchase();
        String[] s = settlement();
        formaPrecedente(p, s);

        Binance_DualInvestment.Esito esito = abbina(RIGA_STESSA_MONETA);

        assertEquals(0, esito.abbinati, "non si rifà l'abbinamento");
        assertEquals(1, esito.migrati, esito.dettagli.toString());
        assertEquals(1, esito.aggiornati);
        assertEquals(5, MappaCryptoWallet.size());
        assertEquals(0, new BigDecimal("102").compareTo(new BigDecimal(s[13])));
        for (String[] v : MappaCryptoWallet.values()) assertEquals("DUAL-1232611", GruppoOperazione.ChiaveEffettiva(v), v[0]);

        Binance_DualInvestment.Esito ancora = abbina(RIGA_STESSA_MONETA);
        assertEquals(0, ancora.migrati);
        assertEquals(1, ancora.giaAPosto);
    }

    @Test
    void formaPrecedente_conLaSolaRewardEliminata_vieneSegnalataENonContataComeAPosto() throws Exception {
        String[] p = purchase();
        String[] s = settlement();
        String reward = formaPrecedente(p, s).get(2)[0];
        //Con la forma vecchia eliminare la reward toglie anche l'uscita dal sotto-wallet e lascia il Settlement
        //ridotto, con un [20] che punta a movimenti cancellati (bug C18)
        Funzioni.RimuoviMovimentazioneXID(reward);
        Map<String, String> prima = fotografia();

        Binance_DualInvestment.Esito esito = abbina(RIGA_STESSA_MONETA);

        assertEquals(0, esito.giaAPosto, "un contratto danneggiato non è a posto");
        assertEquals(0, esito.migrati);
        assertEquals(1, esito.nonTrovati);
        assertTrue(esito.dettagli.toString().contains("1232611"), esito.dettagli.toString());
        assertEquals(prima, fotografia(), "nulla viene toccato, nemmeno il gruppo del contratto");
    }

    @Test
    void formaPrecedente_convertita_ilMotoreDaGliStessiRisultati() {
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("PDD_Reward", "SI");
        //Un acquisto prima del contratto e una vendita dopo, per avere un LIFO da confrontare
        String[] acquisto = movimento("20220501100000_Binance_009_001_AC", "", "", "", "2022-05-01 10:00:00", "100.00");
        acquisto[5] = "ACQUISTO CRYPTO";
        acquisto[8] = "EUR"; acquisto[9] = "FIAT"; acquisto[10] = "-100";
        acquisto[11] = "USDT"; acquisto[12] = "Crypto"; acquisto[13] = "100";
        String[] vendita = movimento("20220601100000_Binance_010_001_VC", "", "", "", "2022-06-01 10:00:00", "105.00");
        vendita[5] = "VENDITA CRYPTO";
        vendita[8] = "USDT"; vendita[9] = "Crypto"; vendita[10] = "-102";
        vendita[11] = "EUR"; vendita[12] = "FIAT"; vendita[13] = "105";
        String[] p = purchase();
        String[] s = settlement();
        formaPrecedente(p, s);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Map<String, String> prima = risultatiMotore();
        Binance_DualInvestment.AggiornaFormaStessaMoneta(p, s);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals(prima, risultatiMotore());
        assertFalse(vendita[19].isBlank(), "la vendita ha una plusvalenza calcolata");
    }

    /** I campi scritti dal motore, per ID. */
    private static Map<String, String> risultatiMotore() {
        Map<String, String> m = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            m.put(v[0], v[16] + "|" + v[17] + "|" + v[19] + "|" + v[33] + "|" + v[38]);
        }
        return m;
    }

    // =============================================================================================
    // MONETA DIVERSA: SCAMBIO DIFFERITO SOVRASCRITTO DA UN ALTRO CONTRATTO (bug C17)
    // =============================================================================================

    private static final String DATA_DEPOSITO = "2022-08-26 10:40:36";
    private static final String RIGA_A = "BUSD/BTC,Buy Low,1005680,2022-04-08 16:19:52,Settled,100.00000000 BUSD,28500,"
            + "2022-08-26 12:40:36,20000,10%,0.00500000 BTC,Settled";
    private static final String RIGA_B = "BUSD/BTC,Buy Low,1055026,2022-04-17 14:26:47,Settled,38.40000000 BUSD,28500,"
            + "2022-08-26 12:40:36,20000,10%,0.00200000 BTC,Settled";

    private static void seedPrezzo(String simbolo, String data) throws Exception {
        long ts = FunzioniDate.ConvertiDatainLongMinuto(data);
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, "binance");
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal("1.00"));
            ps.executeUpdate();
        }
    }

    /**
     * Due contratti liquidati nello stesso secondo, abbinati, poi riportati allo stato che lasciava il codice
     * prima del 2026-10-05: lo scambio e l'uscita del secondo (B) sono spariti e il suo {@code [20]} cita quelli
     * di A. {@code QuartoSegmentoB} permette di dare a B un ID che {@code getIDUnivoco} non sa incrementare.
     * @return {Purchase A, Purchase B, Settlement B}
     */
    private static String[][] collisione(String QuartoSegmentoB) throws Exception {
        String[] pA = movimento("20220408141952_Binance_001_001_PC", "Dual Savings Purchase", "BUSD", "100.00000000", "2022-04-08 14:19:52", "100.00");
        String[] pB = movimento("20220417122647_Binance_001_" + QuartoSegmentoB + "_PC", "Dual Savings Purchase", "BUSD", "38.40000000", "2022-04-17 12:26:47", "38.40");
        movimento("20220826104036_Binance_001_001_DC", "Dual Savings Settlement", "BTC", "0.00500000", DATA_DEPOSITO, "100.00");
        String[] sB = movimento("20220826104036_Binance_001_002_DC", "Dual Savings Settlement", "BTC", "0.00200000", DATA_DEPOSITO, "40.00");
        seedPrezzo("BTC", DATA_DEPOSITO);
        seedPrezzo("BUSD", DATA_DEPOSITO);
        assertEquals(2, abbina(RIGA_A, RIGA_B).abbinati);

        //Lo stato lasciato dalla collisione: via scambio e uscita di B, il suo gruppo cita quelli di A
        String[] idA = pA[20].split(","), idB = pB[20].split(",");
        MappaCryptoWallet.remove(idB[1]);
        MappaCryptoWallet.remove(idB[2]);
        for (String[] v : new String[][]{pB, MappaCryptoWallet.get(idB[0]), sB}) {
            v[20] = v[20].replace(idB[1], idA[1]).replace(idB[2], idA[2]);
        }
        return new String[][]{pA, pB, sB};
    }

    @Test
    void collisione_ripassandoIlFile_ricostruisceSoloIlContrattoPerdente() throws Exception {
        String[][] m = collisione("001");
        List<String> contrattoA = new ArrayList<>();
        for (String[] v : contrattoDi(m[0])) contrattoA.add(String.join("|", v));
        assertEquals(8, MappaCryptoWallet.size());

        Binance_DualInvestment.Esito esito = abbina(RIGA_A, RIGA_B);

        assertEquals(1, esito.riparati, esito.dettagli.toString());
        assertEquals(1, esito.riparazioni.size());
        assertEquals(10, MappaCryptoWallet.size(), "due contratti da cinque movimenti");
        List<String> contrattoADopo = new ArrayList<>();
        for (String[] v : contrattoDi(m[0])) contrattoADopo.add(String.join("|", v));
        assertEquals(contrattoA, contrattoADopo, "il contratto vincente non si tocca");
        List<String[]> b = contrattoDi(m[1]);
        assertEquals(5, b.size());
        for (String[] v : b) assertNotNull(v);
        assertGruppoCompleto(b);
        for (String[] v : b) assertEquals("DUAL-1055026", GruppoOperazione.ChiaveEffettiva(v), v[0]);

        Binance_DualInvestment.Esito ancora = abbina(RIGA_A, RIGA_B);
        assertEquals(0, ancora.riparati);
        assertEquals(10, MappaCryptoWallet.size());
    }

    @Test
    void collisione_seLoScambioNonSiPuoRicreare_nonCambiaNulla() throws Exception {
        //Quarto segmento non numerico: se l'ID dello scambio di B è occupato, getIDUnivoco non trova un'alternativa
        String[][] m = collisione("X");
        String[] sp = m[1][0].split("_"), ss = m[2][0].split("_");
        //ID che lo scambio di B riceverebbe: istante del deposito + 02 + coda del prelievo (originale, senza "00")
        String idScambioB = ss[0] + "_02" + sp[1].substring(2) + "_" + sp[2] + "_" + sp[3] + "_SC";
        movimento(idScambioB, "", "BTC", "1", DATA_DEPOSITO, "1.00");
        Map<String, String> prima = fotografia();

        Binance_DualInvestment.Esito esito = abbina(RIGA_A, RIGA_B);

        assertEquals(0, esito.riparati);
        assertTrue(esito.dettagli.toString().contains("1055026"), esito.dettagli.toString());
        assertEquals(prima, fotografia());
    }
}
