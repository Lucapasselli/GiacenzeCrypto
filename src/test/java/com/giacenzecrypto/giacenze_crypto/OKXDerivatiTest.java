package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token del contratto e margine parcheggiato di OKX ({@link OKXDerivati}): dal bill alla riga portatrice, dalla riga ai
 * movimenti, la guardia sulle chiusure senza apertura e, soprattutto, l'<b>effetto fiscale nullo</b> sul motore delle
 * plusvalenze (LIFO, costi, plusvalenze e risultato dei derivati di tutti gli altri movimenti restano identici).
 *
 * <p>Dati inventati. La forma dei bill e' quella vera, verificata sui bill di un perpetuo xperp in isolated margin.</p>
 */
class OKXDerivatiTest {

    @TempDir
    static Path tempDir;

    private static final String CONTRATTO = "BTC-USD_UM_XPERP-991231";
    private static final String TOKEN_LONG = Derivati.NomeTokenContratto(CONTRATTO, true);
    private static final String TOKEN_SHORT = Derivati.NomeTokenContratto(CONTRATTO, false);

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
    void setUp() {
        Principale.InterrompiCiclo = false; //lo lascia acceso qualche altra classe: convertOKXBills risponderebbe null
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_ScambiSempreRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
        OKXDerivati.AzzeraCacheValore();
        ValoriContratti.ImpostaPerTest(java.util.List.of());   //le regole vere hanno una prova a parte
        OKXDerivati.FonteValoreContratto = id -> "0.0001";
    }

    @AfterEach
    void ripristina() {
        OKXDerivati.FonteValoreContratto = id -> "";
        ValoriContratti.ImpostaPerTest(null);
    }

    // ---------------------------------------------------------------------------------------------
    // Bill di prova
    // ---------------------------------------------------------------------------------------------

    /** Un fill: {@code balChg} e' ricavato dall'identita' {@code -posBalChg + pnl + fee}, come nei bill veri. */
    private static JSONObject fill(String billId, long ts, String subType, String sz, String posBalChg, String pnl, String fee) {
        java.math.BigDecimal bal = new java.math.BigDecimal(posBalChg).negate()
                .add(new java.math.BigDecimal(pnl)).add(new java.math.BigDecimal(fee));
        return new JSONObject()
                .put("billId", billId).put("instId", CONTRATTO).put("instType", "FUTURES").put("ccy", "USDC")
                .put("type", "2").put("subType", subType).put("sz", sz).put("px", "60100.5")
                .put("fillIdxPx", "60130.2").put("fillMarkPx", "60105.8")
                .put("balChg", bal.toPlainString()).put("posBalChg", posBalChg).put("posBal", "13.00517")
                .put("pnl", pnl).put("fee", fee).put("mgnMode", "isolated")
                .put("ordId", "7" + billId).put("tradeId", "9" + billId).put("ts", String.valueOf(ts));
    }

    private static final long T0 = 1_767_348_000_000L; //2026-01-02 10:00 UTC

    private static JSONObject apertura() {
        return fill("1001", T0, "1", "2", "13.0", "0", "-0.0065");
    }

    private static JSONObject chiusuraParziale() {
        return fill("1002", T0 + 3_600_000L, "2", "1", "-6.5", "0.003", "-0.0013");
    }

    private static JSONObject chiusuraFinale() {
        return fill("1003", T0 + 7_200_000L, "2", "1", "-6.5", "-0.002", "-0.0013");
    }

    private static String[] portatrice(JSONObject bill) {
        String[] r = OKXDerivati.RigaContratto(bill);
        assertNotNull(r);
        r[1] = "OKX";
        r[2] = "Principale";
        return r;
    }

    // ---------------------------------------------------------------------------------------------
    // Dal bill alla riga portatrice
    // ---------------------------------------------------------------------------------------------

    @Test
    void laRigaPortatriceConservaTuttiIDatiDelFill() {
        String[] r = portatrice(apertura());
        assertEquals("Contratto|FUTURES", r[4]);
        assertEquals("1001-DRV", r[14], "il billId porta un suffisso, o la deduplica per billId la scambia per la riga del pnl");
        assertEquals("71001", r[13]);
        Map<String, String> d = Derivati.LeggiDati(r[8]);
        assertEquals("OKX", d.get("piattaforma"));
        assertEquals(CONTRATTO, d.get("contratto"));
        assertEquals("long", d.get("verso"));
        assertEquals("apre", d.get("azione"));
        assertEquals("2", d.get("contratti"));
        assertEquals("60100.5", d.get("prezzo"));
        assertEquals("60130.2", d.get("prezzoIndice"));
        assertEquals("60105.8", d.get("prezzoMark"));
        assertEquals("13", d.get("margine"));
        assertEquals("USDC", d.get("moneta"));
        assertEquals("isolated", d.get("modoMargine"));
        assertEquals("71001", d.get("ordine"));
        assertEquals("91001", d.get("scambio"));
        assertEquals("1001", d.get("bill"));
    }

    @Test
    void ilVersoSiLeggeDalSubTypeEDalSegnoDelMargine() {
        //net mode: l'acquisto apre un long ma chiude uno short, la vendita apre uno short ma chiude un long
        assertEquals(Boolean.TRUE, OKXDerivati.Verso("1", true));
        assertEquals(Boolean.FALSE, OKXDerivati.Verso("1", false));
        assertEquals(Boolean.FALSE, OKXDerivati.Verso("2", true));
        assertEquals(Boolean.TRUE, OKXDerivati.Verso("2", false));
        //long/short mode: i codici dicono tutto, e devono concordare col segno
        assertEquals(Boolean.TRUE, OKXDerivati.Verso("3", true));
        assertEquals(Boolean.FALSE, OKXDerivati.Verso("4", true));
        assertEquals(Boolean.TRUE, OKXDerivati.Verso("5", false));
        assertEquals(Boolean.FALSE, OKXDerivati.Verso("6", false));
        assertNull(OKXDerivati.Verso("3", false));
        assertNull(OKXDerivati.Verso("6", true));
        assertNull(OKXDerivati.Verso("99", true));
    }

    @Test
    void unBillCheNonRispettaLIdentitaOChiNonHaIlMargineRipiegaSullaFormaDiPrima() {
        assertNotNull(OKXDerivati.RigaContratto(apertura()));
        assertNull(OKXDerivati.RigaContratto(apertura().put("balChg", "-13.5")), "identita' non rispettata");
        JSONObject senzaMargine = apertura();
        senzaMargine.remove("posBalChg");
        assertNull(OKXDerivati.RigaContratto(senzaMargine), "senza margine");
        assertNull(OKXDerivati.RigaContratto(apertura().put("instType", "OPTION")), "le opzioni non hanno il margine di posizione");
        assertNull(OKXDerivati.RigaContratto(apertura().put("type", "8")), "solo i fill");
        assertNull(OKXDerivati.RigaContratto(apertura().put("subType", "7")), "verso sconosciuto");
        assertNull(OKXDerivati.RigaContratto(apertura().put("sz", "0")), "nessuna quantita'");
    }

    @Test
    void convertOKXBillsAggiungeLaRigaPortatriceSenzaTogliereQuelleDiPrima() {
        JsonArray bills = JsonParser.parseString("[" + apertura() + "," + chiusuraParziale() + "]").getAsJsonArray();
        List<String[]> righe = CcxtInterop.convertOKXBills(bills, "Trading");
        long portatrici = righe.stream().filter(OKXDerivati::IsRiga).count();
        assertEquals(2, portatrici);
        //la commissione dell'apertura e il pnl della chiusura restano, come prima
        assertTrue(righe.stream().anyMatch(r -> r[4].equals("Fee|FUTURES") && r[14].equals("1001")));
        assertTrue(righe.stream().anyMatch(r -> r[4].equals("Buy|FUTURES") && r[14].equals("1002")));
        //la deduplica per billId non deve mangiarsi le portatrici
        assertEquals(righe.size(), CcxtInterop.deduplicaBillOKX(righe).size());
    }

    @Test
    void unBillSenzaPosBalChgDelCsvDellArchivioNonProduceLaRigaPortatrice() {
        JSONObject csv = apertura();
        csv.remove("posBalChg");
        JsonArray bills = JsonParser.parseString("[" + csv + "]").getAsJsonArray();
        List<String[]> righe = CcxtInterop.convertOKXBills(bills, "Trading");
        assertTrue(righe.stream().noneMatch(OKXDerivati::IsRiga));
    }

    // ---------------------------------------------------------------------------------------------
    // Dalla riga ai movimenti
    // ---------------------------------------------------------------------------------------------

    @Test
    void l_aperturaParcheggiaIlMargineEApreIlToken() {
        List<String[]> m = OKXDerivati.CreaMovimenti(portatrice(apertura()));
        assertEquals(3, m.size());

        String[] esce = m.get(0);
        String[] entra = m.get(1);
        String[] token = m.get(2);

        assertTrue(esce[0].endsWith("_TI"));
        assertEquals("Principale", esce[4]);
        assertEquals("USDC", esce[8]);
        assertEquals("-13", esce[10]);
        assertEquals(Derivati.MARGINE, Derivati.Tipo(esce));

        assertTrue(entra[0].endsWith("_TI"));
        assertEquals(OKXDerivati.SOTTOWALLET, entra[4]);
        assertEquals("USDC", entra[11]);
        assertEquals("13", entra[13]);

        assertTrue(token[0].endsWith("_DC"));
        assertEquals(OKXDerivati.SOTTOWALLET, token[4]);
        assertEquals(TOKEN_LONG, token[11]);
        assertEquals("2", token[13], "quantita' in contratti");
        assertEquals("0.00", token[15], "valore zero");
        assertEquals("SI", token[32], "gia' prezzato: nessuna ricerca");
        assertEquals(OKXDerivati.CAMPO18_APERTURA, token[18]);
        assertEquals(Derivati.CONTRATTO, Derivati.Tipo(token));

        for (String[] r : m) {
            assertEquals("1001", r[24], "[24] e' il billId puro, lo stesso delle righe del pnl e della commissione");
            assertEquals("OKX", r[3]);
            Map<String, String> d = Derivati.Dati(r);
            assertEquals(CONTRATTO, d.get("contratto"));
            assertEquals("0.0001", d.get("valoreContratto"));
        }
    }

    @Test
    void laChiusuraScaricaIlTokenERiportaIndietroIlMargine() {
        List<String[]> m = OKXDerivati.CreaMovimenti(portatrice(chiusuraParziale()));
        assertEquals(3, m.size());
        assertEquals(OKXDerivati.SOTTOWALLET, m.get(0)[4]);
        assertEquals("-6.5", m.get(0)[10]);
        assertEquals("Principale", m.get(1)[4]);
        assertEquals("6.5", m.get(1)[13]);
        String[] token = m.get(2);
        assertTrue(token[0].endsWith("_PC"));
        assertEquals(TOKEN_LONG, token[8]);
        assertEquals("-1", token[10]);
        assertEquals(OKXDerivati.CAMPO18_CHIUSURA, token[18]);
    }

    @Test
    void ilValoreDelContrattoNonLettoLasciaIlCampoVuotoSenzaErrori() {
        OKXDerivati.AzzeraCacheValore();
        OKXDerivati.FonteValoreContratto = id -> {
            throw new IllegalStateException("rete assente");
        };
        List<String[]> m = OKXDerivati.CreaMovimenti(portatrice(apertura()));
        assertEquals(3, m.size());
        assertNull(Derivati.Dati(m.get(2)).get("valoreContratto"));
    }

    @Test
    void ilTokenDiUnContrattoHaPrezzoZeroSenzaCercarloInRete() {
        Moneta m = new Moneta();
        m.Moneta = TOKEN_LONG;
        m.Qta = "2";
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(m, null, T0, "", "");
        assertNotNull(ip);
        assertEquals(0, ip.prezzoQta.signum());
        assertEquals("Contratto derivato", ip.Fonte);
        assertTrue(Derivati.IsTokenContratto(TOKEN_LONG));
        assertTrue(Derivati.IsTokenContratto(TOKEN_SHORT));
        assertFalse(Derivati.IsTokenContratto("BTC"));
        assertNotEquals(TOKEN_LONG, TOKEN_SHORT);
    }

    // ---------------------------------------------------------------------------------------------
    // Chiusure senza apertura
    // ---------------------------------------------------------------------------------------------

    private static List<String[]> movimenti(JSONObject... bills) {
        List<String[]> tutti = new ArrayList<>();
        for (JSONObject b : bills) tutti.addAll(OKXDerivati.CreaMovimenti(portatrice(b)));
        return tutti;
    }

    @Test
    void unaChiusuraSenzaAperturaPerdeTokenEMargineMaNonLeAperture() {
        List<String[]> lista = movimenti(chiusuraParziale());
        assertEquals(3, lista.size());
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(lista, List.of());
        assertTrue(filtrata.isEmpty(), "la posizione e' stata aperta con la forma di prima: nessun token da scaricare");
    }

    @Test
    void apertureEChiusureNellaStessaCorsaPassanoInOrdineDiEsecuzione() {
        //passate alla rovescia: il filtro le ordina per esecuzione
        List<String[]> lista = movimenti(chiusuraFinale(), chiusuraParziale(), apertura());
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(lista, List.of());
        assertEquals(9, filtrata.size());
    }

    @Test
    void laChiusuraPiuGrandeDellAperturaChiudeCoiTokenIContrattiAperti() {
        //Dal 19/11/2026: prima la chiusura si scartava tutta e il token dei 2 contratti restava aperto per sempre
        List<String[]> lista = movimenti(apertura(), fill("1005", T0 + 60_000L, "2", "3", "-19.5", "0.001", "-0.002"));
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(lista, List.of());
        assertEquals(6, filtrata.size(), "apertura e chiusura, nessun rientro: non c'e' un prelievo di margine con la forma di prima");
        String[] chiusura = filtrata.stream().filter(r -> Derivati.Tipo(r).equals(Derivati.CONTRATTO) && !r[8].isBlank())
                .findFirst().orElseThrow();
        assertEquals("-2", chiusura[10]);
        assertEquals("3", Derivati.Dati(chiusura).get("contrattiBorsa"));
        assertEquals(0, saldoMargine(filtrata).signum(), "il margine parcheggiato rientra tutto");
    }

    @Test
    void lAperturaGiaInArchivioPermetteLaChiusuraDiUnaCorsaSuccessiva() {
        List<String[]> archivio = movimenti(apertura());
        List<String[]> nuovi = movimenti(chiusuraParziale());
        assertEquals(3, OKXDerivati.FiltraPerSaldoAperto(nuovi, archivio).size());
        //la seconda chiusura da 1 contratto trova ancora 1 aperto nell'archivio piu' la prima chiusura gia' scritta
        archivio.addAll(nuovi);
        assertEquals(3, OKXDerivati.FiltraPerSaldoAperto(movimenti(chiusuraFinale()), archivio).size());
        archivio.addAll(movimenti(chiusuraFinale()));
        assertTrue(OKXDerivati.FiltraPerSaldoAperto(movimenti(fill("1006", T0 + 9_000_000L, "2", "1", "-6.5", "0", "0")), archivio).isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // Effetto fiscale nullo
    // ---------------------------------------------------------------------------------------------

    private static int progressivo = 0;

    private static String[] riga(String data, String tipoID, String classificazione, String descrizione,
            String monetaU, String tipoU, String qtaU, String monetaE, String tipoE, String qtaE, String valore) {
        String id = data.replace(":", ".").replace(" ", "") + "00_TESTOKX_" + String.format("%03d", ++progressivo) + "_001_" + tipoID;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "OKX";
        m[4] = "Principale";
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

    /** I movimenti di prima di un contratto: l'acquisto degli USDC e il pnl e le commissioni di chiusura, come DDV/PDV. */
    private static List<String[]> movimentiDiPrima() {
        List<String[]> l = new ArrayList<>();
        l.add(riga("2026-01-01 10:00", "AC", "", "ACQUISTO", "EUR", "FIAT", "100", "USDC", "Crypto", "100", "100"));
        String[] acc = riga("2026-01-02 11:00", "DC", Derivati.CAMPO18_ACCREDITO, "ACCREDITO DERIVATI",
                "", "", "", "USDC", "Crypto", "0.003", "0.0028");
        Derivati.Marca(acc, Derivati.PNL);
        l.add(acc);
        String[] com = riga("2026-01-02 10:00", "CM", Derivati.CAMPO18_ADDEBITO, "COMMISSIONI",
                "USDC", "Crypto", "-0.0065", "", "", "", "0.0061");
        Derivati.Marca(com, Derivati.COMMISSIONE);
        l.add(com);
        String[] add = riga("2026-01-02 12:00", "PC", Derivati.CAMPO18_ADDEBITO, "ADDEBITO DERIVATI",
                "USDC", "Crypto", "-0.002", "", "", "", "0.0019");
        Derivati.Marca(add, Derivati.PNL);
        l.add(add);
        return l;
    }

    private static String[] impronta(String[] v) {
        return new String[]{v[16], v[17], v[19], v[33], v[38], v[46]};
    }

    @Test
    void ilTokenEIlMargineNonCambianoNullaDelCalcoloDeglialtriMovimenti() {
        List<String[]> prima = movimentiDiPrima();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String[]> senza = new ArrayList<>();
        for (String[] v : prima) senza.add(impronta(v));
        assertFalse(Arrays.asList(senza.get(1)).isEmpty());

        //Aggiungo apertura e chiusure del contratto: token e margine nel sotto-wallet
        for (String[] v : movimenti(apertura(), chiusuraParziale(), chiusuraFinale())) Principale.MappaCryptoWallet.put(v[0], v);
        //ricalcolo completo, non incrementale: il confronto non deve dipendere dalla via scelta dal motore
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        for (int i = 0; i < prima.size(); i++) {
            assertArrayEquals(senza.get(i), impronta(prima.get(i)), "il movimento " + prima.get(i)[0] + " ha cambiato risultato");
        }
        //e le nuove righe non producono plusvalenze ne' risultato dei derivati
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!Derivati.IsRigaContrattoOMargine(v)) continue;
            assertTrue(v[19].isBlank() || new java.math.BigDecimal(v[19]).signum() == 0, v[0] + " plusvalenza " + v[19]);
            assertTrue(v[Derivati.CAMPO_RISULTATO].isBlank(), v[0] + " risultato derivati " + v[Derivati.CAMPO_RISULTATO]);
        }
    }

    @Test
    void aPosizioneChiusaIlSottoWalletEPulitoESenzaGiacenzeNegative() {
        movimentiDiPrima();
        for (String[] v : movimenti(apertura(), chiusuraParziale(), chiusuraFinale())) Principale.MappaCryptoWallet.put(v[0], v);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).isEmpty());

        java.math.BigDecimal token = java.math.BigDecimal.ZERO;
        java.math.BigDecimal margine = java.math.BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!OKXDerivati.SOTTOWALLET.equals(v[4])) continue;
            java.math.BigDecimal q = !v[13].isBlank() ? new java.math.BigDecimal(v[13]) : new java.math.BigDecimal(v[10]);
            String moneta = !v[11].isBlank() ? v[11] : v[8];
            if (Derivati.IsTokenContratto(moneta)) token = token.add(q);
            else margine = margine.add(q);
        }
        assertEquals(0, token.signum(), "il token si azzera: due chiusure da un contratto contro un'apertura da due");
        assertEquals(0, margine.signum(), "il margine parcheggiato rientra tutto");
    }

    // ---------------------------------------------------------------------------------------------
    // Dal bill al movimento, passando dall'import via API
    // ---------------------------------------------------------------------------------------------

    private static JsonArray array(JSONObject... bills) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bills.length; i++) sb.append(i > 0 ? "," : "").append(bills[i]);
        return JsonParser.parseString(sb.append("]").toString()).getAsJsonArray();
    }

    private static long contaTipo(String tipo) {
        return Principale.MappaCryptoWallet.values().stream().filter(v -> Derivati.Tipo(v).equals(tipo)).count();
    }

    @Test
    void importandoViaApiNascono14MovimentiEIlReimportNonNeAggiungeNessuno() {
        JsonArray bills = array(apertura(), chiusuraParziale(), chiusuraFinale());
        int[] esito = Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(bills, "Trading")));
        //commissione + (pnl) + tre righe di contratto per ognuno dei tre fill
        assertEquals(14, esito[0], "movimenti importati");
        assertEquals(14, Principale.MappaCryptoWallet.size());
        assertEquals(3, contaTipo(Derivati.CONTRATTO));
        assertEquals(6, contaTipo(Derivati.MARGINE));

        int[] secondo = Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(bills, "Trading")));
        assertEquals(0, secondo[0], "lo stesso scaricamento ripetuto non aggiunge nulla");
        assertEquals(14, Principale.MappaCryptoWallet.size());
    }

    @Test
    void unaPosizioneAperta_conLaFormaDiPrima_nonRiceveIlTokenNeAllaChiusura() {
        //apertura importata con la forma di prima: nessun posBalChg, quindi nessuna riga di contratto
        JSONObject vecchia = apertura();
        vecchia.remove("posBalChg");
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(array(vecchia), "Trading")));
        assertEquals(0, contaTipo(Derivati.CONTRATTO));
        int prima = Principale.MappaCryptoWallet.size();

        //ora lo scaricamento porta apertura (gia' in archivio) e due chiusure: ne' token ne' margine, solo pnl e commissioni
        JsonArray bills = array(apertura(), chiusuraParziale(), chiusuraFinale());
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(bills, "Trading")));
        assertEquals(0, contaTipo(Derivati.CONTRATTO));
        assertEquals(0, contaTipo(Derivati.MARGINE));
        //pnl e commissione delle due chiusure, come sempre, e nessun rientro: il margine di quell'apertura non e' mai uscito
        assertEquals(prima + 4, Principale.MappaCryptoWallet.size());
        assertTrue(rientri().isEmpty());
        //il sotto-wallet dei derivati non e' mai in negativo (gli USDC senza deposito dei dati inventati non c'entrano)
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).stream()
                .noneMatch(x -> x.contains(OKXDerivati.SOTTOWALLET)));
    }

    // ---------------------------------------------------------------------------------------------
    // Funding e margine che non coincidono con quello parcheggiato
    // ---------------------------------------------------------------------------------------------

    /** Margine che resta nel sotto-wallet dei derivati dopo queste righe (le entrate meno le uscite). */
    private static java.math.BigDecimal saldoMargine(List<String[]> righe) {
        java.math.BigDecimal tot = java.math.BigDecimal.ZERO;
        for (String[] v : righe) {
            //Tutte le monete del sotto-wallet: nella forma "conto" ci stanno anche profitto, perdita, commissione e funding
            if (!OKXDerivati.SOTTOWALLET.equals(v[4]) || Derivati.Tipo(v).equals(Derivati.CONTRATTO)) continue;
            tot = tot.add(OKXDerivati.QuantitaConSegno(v));
        }
        return tot;
    }

    @Test
    void ilFundingPagatoNonLasciaUnResiduoNelSottoWallet() {
        //l'exchange restituisce meno di quanto parcheggiato: il funding pagato e' uscito dal margine della posizione
        List<String[]> lista = movimenti(apertura(),
                fill("1002", T0 + 3_600_000L, "2", "1", "-6.4995", "0.003", "-0.0013"),
                fill("1003", T0 + 7_200_000L, "2", "1", "-6.4995", "-0.002", "-0.0013"));
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(lista, List.of());
        assertEquals(9, filtrata.size());
        assertEquals(0, saldoMargine(filtrata).signum(), "a posizione chiusa il sotto-wallet e' a zero");
        //il valore dell'exchange resta nei dati, accanto a quello riportato
        String[] chiusura = filtrata.stream().filter(r -> r[24].equals("1002") && Derivati.Tipo(r).equals(Derivati.CONTRATTO)).findFirst().orElseThrow();
        assertEquals("6.4995", Derivati.Dati(chiusura).get("margineBorsa"));
        assertEquals("6.5", Derivati.Dati(chiusura).get("margineRiportato"));
    }

    @Test
    void ilFundingIncassatoNonPortaIlSottoWalletInNegativo() {
        List<String[]> lista = movimenti(apertura(),
                fill("1002", T0 + 3_600_000L, "2", "1", "-6.5010", "0.003", "-0.0013"),
                fill("1003", T0 + 7_200_000L, "2", "1", "-6.5010", "-0.002", "-0.0013"));
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(lista, List.of());
        assertEquals(0, saldoMargine(filtrata).signum());
        for (String[] v : filtrata) {
            if (Derivati.Tipo(v).equals(Derivati.MARGINE) && !v[10].isBlank()) {
                //nessuna uscita dal sotto-wallet supera quanto vi era parcheggiato
                assertTrue(new java.math.BigDecimal(v[10]).abs().compareTo(new java.math.BigDecimal("13")) <= 0);
            }
        }
    }

    @Test
    void laChiusuraParzialeRiportaIlMargineInProporzioneAiContratti() {
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(
                movimenti(apertura(), fill("1002", T0 + 3_600_000L, "2", "1", "-6.5100", "0", "-0.001")), List.of());
        java.math.BigDecimal dopoLaParziale = saldoMargine(filtrata);
        assertEquals(0, new java.math.BigDecimal("6.5").compareTo(dopoLaParziale), "meta' dei contratti, meta' del margine");
    }

    @Test
    void ilMargineParcheggiatoInArchivioServeAllaChiusuraDiUnaCorsaSuccessiva() {
        List<String[]> archivio = movimenti(apertura());
        List<String[]> nuovi = movimenti(fill("1002", T0 + 3_600_000L, "2", "1", "-6.4000", "0", "-0.001"));
        List<String[]> filtrata = OKXDerivati.FiltraPerSaldoAperto(nuovi, archivio);
        assertEquals(3, filtrata.size());
        archivio.addAll(filtrata);
        List<String[]> ultima = OKXDerivati.FiltraPerSaldoAperto(
                movimenti(fill("1003", T0 + 7_200_000L, "2", "1", "-6.4000", "0", "-0.001")), archivio);
        archivio.addAll(ultima);
        assertEquals(0, saldoMargine(archivio).signum(), "l'ultima chiusura riporta tutto il residuo");
    }

    @Test
    void conIlFundingNelMezzoIlCalcoloDeglialtriMovimentiNonCambia() {
        List<String[]> prima = movimentiDiPrima();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String[]> senza = new ArrayList<>();
        for (String[] v : prima) senza.add(impronta(v));

        List<String[]> lista = movimenti(apertura(),
                fill("1002", T0 + 3_600_000L, "2", "1", "-6.5010", "0.003", "-0.0013"),
                fill("1003", T0 + 7_200_000L, "2", "1", "-6.5010", "-0.002", "-0.0013"));
        for (String[] v : OKXDerivati.FiltraPerSaldoAperto(lista, List.of())) Principale.MappaCryptoWallet.put(v[0], v);
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        for (int i = 0; i < prima.size(); i++) {
            assertArrayEquals(senza.get(i), impronta(prima.get(i)));
        }
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).stream()
                .noneMatch(x -> x.contains(OKXDerivati.SOTTOWALLET)));
    }

    // ---------------------------------------------------------------------------------------------
    // Funding (bill type 8)
    // ---------------------------------------------------------------------------------------------

    /** Funding come lo scrive OKX: balChg 0, l'importo in pnl e in posBalChg (esce dal margine della posizione). */
    private static JSONObject funding(String billId, long ts, String importo) {
        return new JSONObject()
                .put("billId", billId).put("instId", CONTRATTO).put("instType", "FUTURES").put("ccy", "USDC")
                .put("type", "8").put("subType", "173").put("sz", "1").put("px", "60080.4")
                .put("balChg", "0.0000000000000000").put("posBalChg", importo).put("posBal", "6.502814455019873")
                .put("pnl", importo).put("fee", "0").put("mgnMode", "isolated").put("tradeId", "0").put("ts", String.valueOf(ts));
    }

    /** Bill di liquidazione (type 5) o di clawback (type 10) nella forma vista il 21/11/2026 su un conto vero, con numeri inventati. */
    private static JSONObject liquidazione(String billId, long ts, String tipo, String subType, String sz, String posBalChg, String posBal,
            String pnl, String fee) {
        return new JSONObject()
                .put("billId", billId).put("instId", CONTRATTO).put("instType", "FUTURES").put("ccy", "USDC")
                .put("type", tipo).put("subType", subType).put("sz", sz).put("px", "60000")
                .put("balChg", "0").put("posBalChg", posBalChg).put("posBal", posBal)
                .put("pnl", pnl).put("fee", fee).put("mgnMode", "isolated").put("ordId", "").put("tradeId", "").put("ts", String.valueOf(ts));
    }

    /**
     * Liquidazione (decisione dell'utente del 21/11/2026): si importa come una chiusura. Long di 4 contratti con 0,5 USDC di margine, 0,002
     * di funding pagato, liquidazione con perdita 0,47, commissione 0,0021 e penale 0,03 (ricavata: il bill non la riporta), deficit di
     * 0,0041 coperto da OKX con il clawback. Il token torna a zero, il sotto-wallet anche, penale e clawback sono movimenti a parte e la
     * somma dei movimenti e' quella dei balChg dell'exchange.
     */
    @Test
    void laLiquidazioneEUnaChiusuraConPenaleEClawback() {
        depositoIniziale();
        JSONObject ap = fill("3001", T0, "1", "4", "0.5", "0", "-0.001");
        JSONObject fu = funding("3002", T0 + 3_600_000L, "-0.002");
        JSONObject liq = liquidazione("3003", T0 + 7_200_000L, "5", "107", "4", "-0.5021", "-0.0041", "-0.47", "-0.0021");
        JSONObject claw = liquidazione("3004", T0 + 7_200_000L, "10", "108", "0.0041", "0.0041", "0", "0", "0");
        assertEquals(0, new java.math.BigDecimal("-0.03").compareTo(OKXDerivati.PenaleLiquidazione(liq)));
        java.math.BigDecimal prima = sommaUSDC();
        importa(ap, fu, liq, claw);

        //Token del contratto: aperto e chiuso, a zero; sotto-wallet a zero
        assertEquals(2, contaTipo(Derivati.CONTRATTO));
        String[] chiusura = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && "3003".equals(v[24])).findFirst().orElseThrow();
        java.util.Map<String, String> d = Derivati.Dati(chiusura);
        assertEquals("chiude", d.get("azione"));
        assertEquals("long", d.get("verso"));
        assertEquals("liquidazione", d.get("evento"));
        assertEquals("-0.03", d.get("penale"));
        assertEquals("0", d.get("margineTotale"), "il deficit lo copre il clawback");
        assertEquals(0, saldo(OKXDerivati.SOTTOWALLET, "USDC").signum(), "il margine parcheggiato rientra tutto");
        //Perdita, commissione, penale e clawback
        String[] penale = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[7] != null && v[7].startsWith(CcxtInterop.CAUSALE_PENALE_DERIVATI_OKX + "|")).findFirst().orElseThrow();
        assertEquals(0, new java.math.BigDecimal("-0.03").compareTo(new java.math.BigDecimal(penale[10])));
        assertEquals(Derivati.COMMISSIONE, Derivati.Tipo(penale));
        assertEquals(Derivati.CAMPO18_ADDEBITO, penale[18], "nasce classificata come ogni commissione dei derivati");
        String[] clawback = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[7] != null && v[7].startsWith(CcxtInterop.CAUSALE_CLAWBACK_DERIVATI_OKX + "|")).findFirst().orElseThrow();
        assertEquals(0, new java.math.BigDecimal("0.0041").compareTo(new java.math.BigDecimal(clawback[13])));
        assertEquals(Derivati.PNL, Derivati.Tipo(clawback));
        assertEquals(Derivati.CAMPO18_ACCREDITO, clawback[18]);
        assertTrue(Principale.MappaCryptoWallet.values().stream()
                .anyMatch(v -> "3003".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.PNL) && "-0.47".equals(v[10])), "la perdita di OKX");
        //Il saldo torna con l'exchange: solo l'apertura ha mosso il saldo (margine piu' commissione)
        assertEquals(0, sommaBalChg(ap, fu, liq, claw).compareTo(sommaUSDC().subtract(prima)), "somma dei movimenti = somma dei balChg");
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).isEmpty());

        //Reimportando gli stessi bill non entra nulla
        int righe = Principale.MappaCryptoWallet.size();
        importa(ap, fu, liq, claw);
        assertEquals(righe, Principale.MappaCryptoWallet.size());
    }

    /** ADL e consegna a scadenza chiudono la posizione come un fill (stessa identita' del margine, nessuna penale). */
    @Test
    void adlEConsegnaChiudonoLaPosizione() {
        for (String[] t : new String[][]{{"9", "128", "adl"}, {"3", "112", "consegna"}}) {
            Principale.MappaCryptoWallet.clear();
            depositoIniziale();
            JSONObject ap = fill("4001", T0, "1", "2", "1.0", "0", "-0.001");
            JSONObject ch = liquidazione("4002", T0 + 3_600_000L, t[0], t[1], "2", "-1.0", "0", "0.05", "-0.002");
            ch.put("balChg", "1.048");
            importa(ap, ch);
            String[] chiusura = Principale.MappaCryptoWallet.values().stream()
                    .filter(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && "4002".equals(v[24])).findFirst().orElseThrow();
            assertEquals(t[2], Derivati.Dati(chiusura).get("evento"));
            assertNull(Derivati.Dati(chiusura).get("penale"));
            assertEquals(0, saldo(OKXDerivati.SOTTOWALLET, "USDC").signum(), t[2]);
            assertTrue(Principale.MappaCryptoWallet.values().stream().noneMatch(v -> v[7] != null && v[7].startsWith(CcxtInterop.CAUSALE_PENALE_DERIVATI_OKX)));
        }
    }

    @Test
    void ilFundingPagatoDiventaUnaRigaConLImportoNegativoEIlContratto() {
        List<String[]> righe = CcxtInterop.convertOKXBills(array(funding("2001", T0, "-0.000713405")), "Trading");
        assertEquals(1, righe.size());
        assertEquals("Funding|FUTURES", righe.get(0)[4]);
        assertEquals("-0.000713405", righe.get(0)[6]);
        assertEquals("USDC", righe.get(0)[5]);
        assertEquals(CONTRATTO, righe.get(0)[7]);
        assertEquals("", righe.get(0)[12], "nessuna commissione");
        assertEquals(Derivati.FUNDING, CcxtInterop.TipoDerivatoDaCausaleOKX(righe.get(0)[4]));
    }

    @Test
    void ilFundingIncassatoHaLImportoPositivoEUnoNulloNonDaRiga() {
        assertEquals("0.0003", CcxtInterop.convertOKXBills(array(funding("2002", T0, "0.0003")), "Trading").get(0)[6]);
        assertTrue(CcxtInterop.convertOKXBills(array(funding("2003", T0, "0")), "Trading").isEmpty());
    }

    @Test
    void ilFundingNonSiLeggeSulFundingAccountNeSenzaImporto() {
        assertTrue(CcxtInterop.convertOKXBills(array(funding("2004", T0, "-0.01")), "Funding").stream()
                .noneMatch(r -> r[4].startsWith("Funding|")));
        JSONObject senza = funding("2005", T0, "-0.01");
        senza.remove("pnl");
        senza.put("balChg", "-0.01");
        assertTrue(CcxtInterop.convertOKXBills(array(senza), "Trading").get(0)[4].startsWith("OKX type 8"));
    }

    @Test
    void laMappaCausaliOKXConosceIlFundingDeiDerivati() throws Exception {
        org.json.JSONObject mappa = new org.json.JSONObject(java.nio.file.Files.readString(
                java.nio.file.Path.of("config/importmappe/OKX.json"))).getJSONObject("mappaCausali");
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.getString("Funding|FUTURES"));
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.getString("Funding|SWAP"));
    }

    @Test
    void importandoViaApiIlFundingNasceClassificatoConIlContratto() {
        JsonArray bills = array(apertura(), funding("2006", T0 + 1_800_000L, "-0.0009"), funding("2007", T0 + 2_000_000L, "0.0004"));
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(bills, "Trading")));
        String[] pagato = Principale.MappaCryptoWallet.values().stream().filter(v -> "2006".equals(v[24])).findFirst().orElseThrow();
        String[] incassato = Principale.MappaCryptoWallet.values().stream().filter(v -> "2007".equals(v[24])).findFirst().orElseThrow();
        assertTrue(pagato[0].endsWith("_PC"));
        assertEquals("USDC", pagato[8]);
        assertEquals("-0.0009", pagato[10]);
        assertEquals(Derivati.FUNDING, Derivati.Tipo(pagato));
        //un movimento nuovo nasce classificato come derivato (decisione del 17/11/2026): solo i movimenti gia' in archivio restano come sono
        assertEquals(Derivati.CAMPO18_ADDEBITO, pagato[18]);
        assertTrue(pagato[21].contains("Contratto " + CONTRATTO), pagato[21]);
        assertTrue(incassato[0].endsWith("_DC"));
        assertEquals("0.0004", incassato[13]);
        assertEquals(Derivati.FUNDING, Derivati.Tipo(incassato));
        assertEquals(Derivati.CAMPO18_ACCREDITO, incassato[18]);
    }
    // ---------------------------------------------------------------------------------------------
    // Posizione aperta con la versione pubblicata (1.0.66) e chiusa dopo l'aggiornamento (D18 punto 8)
    // ---------------------------------------------------------------------------------------------

    /**
     * L'apertura come la registrava la 1.0.66 (forma verificata sul codice pubblicato): un prelievo da classificare del margine,
     * causale "Sell", senza marcatore ne' nota del contratto, e la commissione a parte.
     */
    private static void aperturaPubblicata(String billId, long ts, String margine, String commissione) {
        String data = FunzioniDate.ConvertiDatadaLongAlSecondo(ts);
        String prefisso = data.replaceAll(" |-|:", "");
        String[] pc = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(pc, "");
        pc[0] = prefisso + "_OKX" + billId + "_001_001_PC";
        pc[1] = data.substring(0, 16);
        pc[3] = "OKX";
        pc[4] = "Principale";
        pc[5] = "PRELIEVO CRYPTO";
        pc[7] = "Sell";
        pc[8] = "USDC";
        pc[9] = "Crypto";
        pc[10] = "-" + margine;
        pc[15] = margine;
        pc[22] = "A";
        pc[24] = billId;
        Principale.MappaCryptoWallet.put(pc[0], pc);
        String[] cm = pc.clone();
        cm[0] = prefisso + "_OKX" + billId + "C_001_001_CM";
        cm[5] = "COMMISSIONI";
        cm[7] = "";
        cm[10] = commissione;
        Principale.MappaCryptoWallet.put(cm[0], cm);
    }

    private static List<String[]> rientri() {
        return Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[7] != null && v[7].startsWith(OKXDerivati.CAUSALE_RIENTRO + "|")).toList();
    }

    /** Somma di tutti i movimenti in USDC dell'archivio, sotto-wallet compresi. */
    private static java.math.BigDecimal sommaUSDC() {
        java.math.BigDecimal tot = java.math.BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if ("USDC".equals(v[8]) && Funzioni.isNumeric(v[10], false)) tot = tot.add(new java.math.BigDecimal(v[10]));
            if ("USDC".equals(v[11]) && Funzioni.isNumeric(v[13], false)) tot = tot.add(new java.math.BigDecimal(v[13]));
        }
        return tot;
    }

    private static java.math.BigDecimal sommaBalChg(JSONObject... bills) {
        java.math.BigDecimal tot = java.math.BigDecimal.ZERO;
        for (JSONObject b : bills) tot = tot.add(new java.math.BigDecimal(b.getString("balChg")));
        return tot;
    }

    private static void importa(JSONObject... bills) {
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(array(bills), "Trading")));
    }

    @Test
    void apertaConLaVersionePubblicataEChiusaDopo_ilMarginRientraColFundingRiaggiunto() {
        Importazioni.CausaliDerivatiSegnalate.clear();
        JSONObject ap = apertura();                                   //2 contratti, margine 13, commissione 0,0065
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        //dopo l'aggiornamento: un funding pagato (esce dal margine della posizione) e la chiusura, che restituisce 12,99
        JSONObject f = funding("2101", T0 + 1_800_000L, "-0.01");
        JSONObject ch = fill("1003", T0 + 7_200_000L, "2", "2", "-12.99", "0.003", "-0.0013");
        importa(f, ch);

        assertEquals(0, contaTipo(Derivati.CONTRATTO), "la chiusura non trova l'apertura fra i token");
        assertEquals(0, contaTipo(Derivati.MARGINE));
        List<String[]> r = rientri();
        assertEquals(1, r.size());
        String[] rientro = r.get(0);
        //12,99 restituiti dall'exchange piu' il funding gia' importato a parte: il margine uscito con l'apertura
        assertEquals(0, new java.math.BigDecimal("13").compareTo(new java.math.BigDecimal(rientro[13])));
        assertEquals("DEPOSITO CRYPTO", rientro[5]);
        assertEquals("", rientro[18], "da classificare come il prelievo di allora");
        assertEquals("", Derivati.Tipo(rientro), "non marcato: non nasce classificato e non va fra quelli in attesa");
        assertEquals("Principale", rientro[4]);
        assertEquals("Contratto " + CONTRATTO, rientro[21]);
        assertEquals("0.01", Derivati.Dati(rientro).get("fundingRiaggiunto"));
        //La somma dei movimenti della posizione e' quella delle variazioni di saldo dell'exchange
        assertEquals(0, sommaBalChg(ap, f, ch).compareTo(sommaUSDC()), "saldo USDC dell'archivio uguale a quello dell'exchange");
        //Il resoconto lo spiega e non elenca la causale fra i tipi di derivati
        String avviso = Importazioni.TestoAvvisoDerivati();
        assertTrue(avviso.contains("con una versione precedente"));
        assertFalse(avviso.contains("<b>" + OKXDerivati.CAUSALE_RIENTRO));

        //Lo stesso scaricamento ripetuto non aggiunge un secondo rientro
        int prima = Principale.MappaCryptoWallet.size();
        importa(f, ch);
        assertEquals(prima, Principale.MappaCryptoWallet.size());
    }

    @Test
    void loScaricamentoCheRiportaAncheLAperturaVecchiaNonLaDuplica() {
        //Al confine dello scaricamento (riparte dall'ultimo movimento) torna anche il bill dell'apertura: la riga della 1.0.66 ha
        //[24] = billId (verificato sul tag), quindi la deduplica la scarta e la chiusura resta senza token
        JSONObject ap = apertura();
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        JSONObject f1 = funding("2101", T0 + 600_000L, "-0.004");    //non importato dalla 1.0.66, porta adesso
        JSONObject f2 = funding("2102", T0 + 1_800_000L, "0.001");   //incassato
        JSONObject ch = fill("1003", T0 + 7_200_000L, "2", "2", "-12.997", "0.003", "-0.0013");
        importa(ap, f1, f2, ch);

        assertEquals(2, Principale.MappaCryptoWallet.values().stream().filter(v -> "1001".equals(v[24])).count(),
                "l'apertura non rientra nella forma nuova");
        assertEquals(0, contaTipo(Derivati.CONTRATTO));
        List<String[]> r = rientri();
        assertEquals(1, r.size());
        //12,997 restituiti + 0,004 pagato - 0,001 incassato = 13, il margine uscito con l'apertura
        assertEquals(0, new java.math.BigDecimal("13").compareTo(new java.math.BigDecimal(r.get(0)[13])));
        assertEquals(0, sommaBalChg(ap, f1, f2, ch).compareTo(sommaUSDC()));
    }

    @Test
    void apertaConLaVersionePubblicataEChiusaInDueVolte_dueRientriCheSommanoIlMargine() {
        JSONObject ap = apertura();
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        JSONObject p = chiusuraParziale();
        JSONObject c = chiusuraFinale();
        importa(p, c);
        List<String[]> r = rientri();
        assertEquals(2, r.size());
        for (String[] v : r) assertEquals(0, new java.math.BigDecimal("6.5").compareTo(new java.math.BigDecimal(v[13])));
        assertEquals(0, sommaBalChg(ap, p, c).compareTo(sommaUSDC()));
    }

    @Test
    void apertaConLaVersionePubblicataEAumentataDopo_iTokenChiudonoLaParteNuovaIlRestoRientra() {
        JSONObject vecchia = fill("0999", T0 - 3_600_000L, "1", "1", "6.5", "0", "-0.0033");
        aperturaPubblicata("0999", T0 - 3_600_000L, "6.5", "-0.0033");
        JSONObject nuova = fill("1001", T0, "1", "1", "6.5", "0", "-0.0033");
        JSONObject ch = fill("1003", T0 + 7_200_000L, "2", "2", "-13.0", "0.004", "-0.0026");
        importa(nuova, ch);

        //Il token chiude il solo contratto aperto coi token, con tutto il margine parcheggiato
        String[] chiusura = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && !v[8].isBlank()).findFirst().orElseThrow();
        assertEquals(0, java.math.BigDecimal.ONE.negate().compareTo(new java.math.BigDecimal(chiusura[10])));
        assertEquals("2", Derivati.Dati(chiusura).get("contrattiBorsa"));
        assertEquals(0, saldoMargine(new ArrayList<>(Principale.MappaCryptoWallet.values())).signum(), "sotto-wallet a zero");
        List<String[]> r = rientri();
        assertEquals(1, r.size());
        assertEquals(0, new java.math.BigDecimal("6.5").compareTo(new java.math.BigDecimal(r.get(0)[13])));
        assertEquals(0, sommaBalChg(vecchia, nuova, ch).compareTo(sommaUSDC()));
    }

    /**
     * Come sopra, con un funding pagato fra l'apertura nuova e la chiusura: il funding va sul conto (la parte nuova e' aperta fra i token)
     * e torna allo spot con il margine riportato, quindi il rientro della parte vecchia non lo riaggiunge una seconda volta.
     */
    @Test
    void apertaConLaVersionePubblicataEAumentataDopo_conFundingSulConto() {
        JSONObject vecchia = fill("0999", T0 - 3_600_000L, "1", "1", "6.5", "0", "-0.0033");
        aperturaPubblicata("0999", T0 - 3_600_000L, "6.5", "-0.0033");
        JSONObject nuova = fill("1001", T0, "1", "1", "6.5", "0", "-0.0033");
        JSONObject f = funding("2201", T0 + 1_800_000L, "-0.004");
        JSONObject ch = fill("1003", T0 + 7_200_000L, "2", "2", "-12.996", "0.004", "-0.0026");
        importa(nuova, f, ch);

        String[] funding = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> Derivati.Tipo(v).equals(Derivati.FUNDING)).findFirst().orElseThrow();
        assertEquals(OKXDerivati.SOTTOWALLET, funding[4], "pagato dal margine della parte nuova");
        assertEquals(0, saldoMargine(new ArrayList<>(Principale.MappaCryptoWallet.values())).signum(), "sotto-wallet a zero");
        List<String[]> r = rientri();
        assertEquals(1, r.size());
        assertEquals(0, new java.math.BigDecimal("6.5").compareTo(new java.math.BigDecimal(r.get(0)[13])), "12,996 - 6,496 riportati");
        assertEquals(0, sommaBalChg(vecchia, nuova, f, ch).compareTo(sommaUSDC()));
    }

    @Test
    void unaChiusuraSenzaNessunaAperturaInArchivio_nonRiportaNulla() {
        //L'apertura non c'e' proprio (archivio iniziato dopo): il margine non e' mai uscito dall'archivio
        importa(chiusuraFinale());
        assertTrue(rientri().isEmpty());
        assertEquals(0, contaTipo(Derivati.CONTRATTO));
    }

    // ---------------------------------------------------------------------------------------------
    // Abbinamento del rientro del margine ai prelievi della 1.0.66 (Principale_RientroMargine, 20/11/2026)
    // ---------------------------------------------------------------------------------------------

    /** La chiusura che azzera la posizione sull'exchange: {@code posBal} zero. */
    private static JSONObject chiusuraTotale(String billId, long ts, String sz, String posBalChg, String pnl, String fee) {
        return fill(billId, ts, "2", sz, posBalChg, pnl, fee).put("posBal", "0");
    }

    /** Una chiusura come la registrava la 1.0.66: deposito "Buy" di margine e profitto, da classificare. */
    private static void chiusuraPubblicata(String billId, long ts, String importo) {
        String data = FunzioniDate.ConvertiDatadaLongAlSecondo(ts);
        String[] dc = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(dc, "");
        dc[0] = data.replaceAll(" |-|:", "") + "_OKX" + billId + "_001_001_DC";
        dc[1] = data.substring(0, 16);
        dc[3] = "OKX";
        dc[4] = "Principale";
        dc[5] = "DEPOSITO CRYPTO";
        dc[7] = "Buy";
        dc[11] = "USDC";
        dc[12] = "Crypto";
        dc[13] = importo;
        dc[15] = importo;
        dc[22] = "A";
        dc[24] = billId;
        Principale.MappaCryptoWallet.put(dc[0], dc);
    }

    /** 100 USDC entrati sul conto prima di tutto, perche' i saldi del conto non partano da zero. */
    private static void depositoIniziale() {
        chiusuraPubblicata("0001", T0 - 2_592_000_000L, "100");
        String[] dc = Principale.MappaCryptoWallet.values().stream().filter(v -> "0001".equals(v[24])).findFirst().orElseThrow();
        dc[7] = "Deposit";
    }

    private static String idPrelievo(String billId) {
        return Principale.MappaCryptoWallet.values().stream()
                .filter(v -> billId.equals(v[24]) && v[0].endsWith("_PC") && "Sell".equals(v[7])).findFirst().orElseThrow()[0];
    }

    /** Somma di una moneta su un sotto-wallet. */
    private static java.math.BigDecimal saldo(String wallet, String moneta) {
        java.math.BigDecimal tot = java.math.BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!wallet.equals(v[4])) continue;
            if (moneta.equals(v[8]) && Funzioni.isNumeric(v[10], false)) tot = tot.add(new java.math.BigDecimal(v[10]));
            if (moneta.equals(v[11]) && Funzioni.isNumeric(v[13], false)) tot = tot.add(new java.math.BigDecimal(v[13]));
        }
        return tot;
    }

    /** Ogni movimento del gruppo cita tutti gli altri, e nessun altro. */
    private static void gruppoSimmetrico(java.util.Set<String> gruppo) {
        for (String id : gruppo) {
            java.util.Set<String> citati = new java.util.HashSet<>(Arrays.asList(Principale.MappaCryptoWallet.get(id)[20].split(",")));
            java.util.Set<String> attesi = new java.util.HashSet<>(gruppo);
            attesi.remove(id);
            assertEquals(attesi, citati, "campo 20 di " + id);
        }
    }

    private static java.util.Set<String> gruppoDi(String id) {
        java.util.Set<String> g = new java.util.TreeSet<>(Arrays.asList(Principale.MappaCryptoWallet.get(id)[20].split(",")));
        g.add(id);
        return g;
    }

    @Test
    void ilRientroSiAbbinaAlPrelievoDiAllora_eIlMargineNonRientratoDiventaUnAddebitoDaDerivati() {
        //Una posizione vecchia gia' chiusa con la 1.0.66 (prelievo e deposito Buy) e la posizione aperta con la 1.0.66: 13 di margine
        depositoIniziale();
        aperturaPubblicata("0500", T0 - 864_000_000L, "10.0", "-0.005");
        chiusuraPubblicata("0501", T0 - 777_600_000L, "10.4");
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        //0,02 di funding pagato prima dell'aggiornamento (la 1.0.66 non lo importava): OKX restituisce 12,98
        JSONObject ch = chiusuraTotale("1003", T0 + 7_200_000L, "2", "-12.98", "0.003", "-0.0013");
        importa(ch);
        String[] rientro = rientri().get(0);
        rientro[15] = "12.98";                                    //valore in euro, 1 USDC = 1 euro
        //Il profitto della stessa chiusura lasciato da classificare (anno con una definitiva): l'abbinamento non deve toccarlo
        String[] pnl = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> "1003".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.PNL)).findFirst().orElseThrow();
        pnl[5] = "DEPOSITO CRYPTO";
        pnl[18] = "";
        java.math.BigDecimal totalePrima = sommaUSDC();
        int righePrima = Principale.MappaCryptoWallet.size();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();             //stato incrementale di prima, come nel programma

        assertTrue(Principale_RientroMargine.VoceAttiva(rientro));
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(rientro[0]);
        assertNull(p.motivo, p.motivo);
        assertEquals(CONTRATTO, p.contratto);
        assertEquals(List.of(rientro[0]), p.rientri);
        assertEquals(List.of(idPrelievo("1001")), p.candidati, "solo i prelievi dopo l'ultima chiusura della 1.0.66");
        assertEquals(java.util.Set.of(idPrelievo("1001")), p.proposti, "l'unico candidato e' gia' spuntato");

        Principale_RientroMargine.Anteprima a = Principale_RientroMargine.Calcola(p, p.proposti, Principale.MappaCryptoWallet,
                java.util.Set.of("2026"));
        assertEquals(0, new java.math.BigDecimal("13.0").compareTo(a.bloccato));
        assertEquals(0, new java.math.BigDecimal("12.98").compareTo(a.rientrato));
        assertEquals(0, new java.math.BigDecimal("0.02").compareTo(a.residuo));
        assertEquals("0.02", a.valoreResiduo.toPlainString());
        assertEquals(java.util.Set.of("2026"), a.anniDichiarati);
        assertFalse(a.residuoSospetto);
        assertTrue(a.chiusureVecchieInMezzo.isEmpty(), "la chiusura della 1.0.66 e' prima del prelievo scelto");
        assertTrue(Principale_RientroMargine.TestoAnteprima(p, a).contains("addebito per derivati"));

        assertTrue(Prezzi.SenzaValorizzare(() -> Principale_RientroMargine.Applica(p, p.proposti)));
        String[] prelievo = Principale.MappaCryptoWallet.get(idPrelievo("1001"));
        assertEquals(Principale_RientroMargine.DETTAGLIO_BLOCCO, prelievo[18]);
        assertEquals("-13.0", prelievo[10], "le quantita' originali non cambiano");
        assertEquals(Principale_RientroMargine.DETTAGLIO_SBLOCCO, rientro[18]);
        String[] residuo = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[7].startsWith(Principale_RientroMargine.CAUSALE_RESIDUO + "|")).findFirst().orElseThrow();
        assertTrue(residuo[0].endsWith("_PC"));
        assertEquals("-0.02", residuo[10]);
        assertEquals("0.02", residuo[15]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, residuo[18]);
        assertEquals(Derivati.FUNDING, Derivati.Tipo(residuo));
        assertEquals(Principale_RientroMargine.WALLET_COLLATERALE, residuo[4]);
        assertEquals("AU", residuo[22]);
        assertEquals(CONTRATTO, OKXDerivati.ContrattoDaNota(residuo[21]));
        assertEquals("", pnl[18], "il profitto della chiusura resta com'era");
        assertEquals(Derivati.PNL, Derivati.Tipo(pnl));
        assertTrue(Principale.MappaCryptoWallet.get(idPrelievo("0500"))[18].isBlank(), "il prelievo non scelto non cambia");

        //Il collaterale torna a zero, il totale della moneta non cambia, un solo gruppo simmetrico e nessun errore di collegamento
        assertEquals(0, saldo(Principale_RientroMargine.WALLET_COLLATERALE, "USDC").signum());
        assertEquals(0, totalePrima.compareTo(sommaUSDC()));
        java.util.Set<String> gruppo = gruppoDi(rientro[0]);
        assertEquals(5, gruppo.size(), "prelievo, blocco, rientro, sblocco, residuo");
        gruppoSimmetrico(gruppo);
        MovimentiCollegati.Esito collegati = MovimentiCollegati.Controlla(Principale.MappaCryptoWallet.values());
        assertEquals(0, collegati.Totale(), collegati.Altri.toString());
        assertTrue(collegati.SenzaCollegamento.isEmpty());
        assertFalse(Principale_RientroMargine.VoceAttiva(rientro));

        //Lo stesso scaricamento ripetuto non tocca l'abbinamento
        importa(ch);
        assertEquals(righePrima + 3, Principale.MappaCryptoWallet.size());
        assertEquals(Principale_RientroMargine.DETTAGLIO_SBLOCCO, Principale.MappaCryptoWallet.get(rientro[0])[18]);
        assertEquals(gruppo, gruppoDi(rientro[0]));

        //Il motore: la passata incrementale da' gli stessi risultati di quella completa
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Map<String, String> incrementale = new java.util.TreeMap<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) incrementale.put(v[0], v[19] + "|" + v[Derivati.CAMPO_RISULTATO]);
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            assertEquals(incrementale.get(v[0]), v[19] + "|" + v[Derivati.CAMPO_RISULTATO], "passata incrementale di " + v[0]);
        }
        //Nessuna plusvalenza sul margine, il residuo e' un onere dei derivati
        assertTrue(prelievo[19].isBlank() || new java.math.BigDecimal(prelievo[19]).signum() == 0, "plusvalenza " + prelievo[19]);
        assertEquals("-0.02", residuo[Derivati.CAMPO_RISULTATO]);
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).isEmpty(),
                String.valueOf(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values())));

        //P2 (ContoDerivati): il collaterale dell'abbinamento e' il conto derivati di allora. Il prelievo della 1.0.66 e' una cessione,
        //il blocco carica la pila del conto al valore, lo sblocco la scarica (reddito valore - costo), il residuo e' un onere pari al costo
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "SI");
        try {
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("S", prelievo[33], "il margine uscito dallo spot e' una cessione");
            assertEquals(0, new java.math.BigDecimal(prelievo[15]).subtract(new java.math.BigDecimal(prelievo[16]))
                    .compareTo(new java.math.BigDecimal(prelievo[19])));
            String[] blocco = Principale.MappaCryptoWallet.values().stream()
                    .filter(v -> gruppo.contains(v[0]) && Principale_RientroMargine.WALLET_COLLATERALE.equals(v[4]) && v[0].endsWith("_DC"))
                    .findFirst().orElseThrow();
            assertEquals(blocco[15], blocco[17], "entra nella pila del conto al valore");
            String[] sblocco = Principale.MappaCryptoWallet.values().stream()
                    .filter(v -> gruppo.contains(v[0]) && Principale_RientroMargine.WALLET_COLLATERALE.equals(v[4])
                    && Principale_RientroMargine.DESCRIZIONE_SBLOCCO.equals(v[5])).findFirst().orElseThrow();
            assertEquals("0.00", sblocco[Derivati.CAMPO_RISULTATO], "12,98 USDC valgono 12,98 e nella pila costano 12,98");
            assertNotEquals("S", sblocco[33]);
            assertEquals("-0.02", residuo[Derivati.CAMPO_RISULTATO], "0,02 USDC caricati a 1 euro");
            assertEquals("12.98", rientro[17], "il rientro carica lo spot al valore");
            Map<String, String> incrementaleP2 = new java.util.TreeMap<>();
            for (String[] v : Principale.MappaCryptoWallet.values()) incrementaleP2.put(v[0], v[19] + "|" + v[Derivati.CAMPO_RISULTATO]);
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            for (String[] v : Principale.MappaCryptoWallet.values()) {
                assertEquals(incrementaleP2.get(v[0]), v[19] + "|" + v[Derivati.CAMPO_RISULTATO], "P2, passata completa di " + v[0]);
            }
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
        }
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(prelievo[19].isBlank() || new java.math.BigDecimal(prelievo[19]).signum() == 0, "di nuovo P1: " + prelievo[19]);

        //Annullando da un membro qualunque tutto torna com'era
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(gruppo.toArray(new String[0]), residuo[0]);
        assertEquals(righePrima, Principale.MappaCryptoWallet.size());
        assertEquals("", prelievo[18]);
        assertEquals("", prelievo[20]);
        assertEquals("", rientro[18]);
        assertEquals("Contratto " + CONTRATTO, rientro[21], "la nota del contratto resta");
        assertTrue(Principale_RientroMargine.VoceAttiva(rientro));
    }

    @Test
    void dueAperturedueChiusureEFundingIncassato_unSoloGruppoEUnAccreditoDaDerivati() {
        depositoIniziale();
        aperturaPubblicata("0998", T0 - 3_600_000L, "6.5", "-0.0033");
        aperturaPubblicata("1001", T0, "6.5", "-0.0033");
        //chiusura parziale con 6,5 di margine, poi quella finale con 6,52: 0,02 di funding incassato prima dell'aggiornamento
        importa(chiusuraParziale(), chiusuraTotale("1003", T0 + 7_200_000L, "1", "-6.52", "-0.002", "-0.0013"));
        List<String[]> r = new ArrayList<>(rientri());
        assertEquals(2, r.size());
        for (String[] v : r) v[15] = v[13];

        //Dal rientro della chiusura parziale si ricava tutta la posizione
        String parziale = r.stream().filter(v -> !Principale_RientroMargine.Chiude(v)).findFirst().orElseThrow()[0];
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(parziale);
        assertNull(p.motivo, p.motivo);
        assertEquals(2, p.rientri.size());
        assertEquals(parziale, p.rientri.get(0));
        assertEquals(2, p.candidati.size());
        assertTrue(p.proposti.isEmpty(), "due prelievi possibili: sceglie l'utente");

        assertTrue(Prezzi.SenzaValorizzare(() -> Principale_RientroMargine.Applica(p, p.candidati)));
        String[] residuo = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> v[7].startsWith(Principale_RientroMargine.CAUSALE_RESIDUO + "|")).findFirst().orElseThrow();
        assertTrue(residuo[0].endsWith("_DC"));
        assertEquals("0.02", residuo[13]);
        assertEquals(Derivati.CAMPO18_ACCREDITO, residuo[18]);
        assertEquals(0, saldo(Principale_RientroMargine.WALLET_COLLATERALE, "USDC").signum());
        java.util.Set<String> gruppo = gruppoDi(parziale);
        assertEquals(9, gruppo.size(), "due prelievi, due rientri, le loro quattro righe speculari e il residuo");
        gruppoSimmetrico(gruppo);
        assertEquals(0, MovimentiCollegati.Controlla(Principale.MappaCryptoWallet.values()).Totale());
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("0.02", residuo[Derivati.CAMPO_RISULTATO]);
        assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).isEmpty(),
                String.valueOf(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values())));
    }

    @Test
    void unaChiusuraRegistrataDallaVersioneVecchiaFraPrelievoERientro_nonSiAbbina() {
        depositoIniziale();
        aperturaPubblicata("1001", T0, "100.0", "-0.05");
        //la 1.0.66 ha chiuso un contratto su due: deposito Buy di 50 di margine e 5 di profitto. Abbinando 100 con il rientro di 50
        //risulterebbe un addebito per derivati di 50 che non c'e' stato (decisione dell'utente del 20/11/2026: non si abbina)
        chiusuraPubblicata("1002", T0 + 3_600_000L, "55");
        importa(chiusuraTotale("1003", T0 + 7_200_000L, "1", "-50", "0.002", "-0.0013"));
        String[] rientro = rientri().get(0);
        assertEquals(0, new java.math.BigDecimal("50").compareTo(new java.math.BigDecimal(rientro[13])));
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(rientro[0]);
        assertNotNull(p.motivo);
        assertTrue(p.motivo.contains("chiusura registrata da quella versione"), p.motivo);
        assertTrue(p.candidati.isEmpty());
        assertFalse(Principale_RientroMargine.Applica(p, List.of(idPrelievo("1001"))));
        assertEquals("", rientro[18]);
        assertEquals("", Principale.MappaCryptoWallet.get(idPrelievo("1001"))[18]);
        //e la difesa nell'anteprima, se un prelievo prima della chiusura arrivasse comunque
        p.motivo = null;
        Principale_RientroMargine.Anteprima a = Principale_RientroMargine.Calcola(p, List.of(idPrelievo("1001")),
                Principale.MappaCryptoWallet, java.util.Set.of());
        assertEquals(1, a.chiusureVecchieInMezzo.size());
    }

    @Test
    void gliAnniDichiaratiSonoTuttiQuelliFraPrelievoERientro() {
        aperturaPubblicata("1001", T0 - 2L * 366 * 86_400_000L, "13.0", "-0.0065");   //due anni prima
        importa(chiusuraTotale("1003", T0 + 7_200_000L, "2", "-13.0", "0.003", "-0.0013"));
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(rientri().get(0)[0]);
        Principale_RientroMargine.Anteprima a = Principale_RientroMargine.Calcola(p, p.candidati, Principale.MappaCryptoWallet,
                java.util.Set.of("2025"));
        assertEquals(java.util.Set.of("2025"), a.anniDichiarati, "un anno senza movimenti della posizione cambia le giacenze di fine anno");
    }

    @Test
    void unaPosizioneNonAncoraChiusaNonSiAbbina() {
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        importa(chiusuraParziale());
        String[] rientro = rientri().get(0);
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(rientro[0]);
        assertNotNull(p.motivo);
        assertTrue(p.motivo.contains("non risulta ancora chiusa"), p.motivo);
        assertFalse(Principale_RientroMargine.Applica(p, List.of(idPrelievo("1001"))));
        assertEquals("", rientro[18]);
    }

    @Test
    void iRientriAppenaImportatiSiPropongonoUnaVoltaPerPosizione() {
        Principale_RientroMargine.DaAbbinareDopoImportazione();          //svuota quanto lasciato da altri test
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        importa(chiusuraParziale(), chiusuraTotale("1003", T0 + 7_200_000L, "1", "-6.5", "-0.002", "-0.0013"));
        List<String> da = Principale_RientroMargine.DaAbbinareDopoImportazione();
        assertEquals(1, da.size(), "due rientri della stessa posizione: una domanda sola");
        assertTrue(Principale_RientroMargine.DaAbbinareDopoImportazione().isEmpty(), "l'elenco si svuota");
    }

    // ---------------------------------------------------------------------------------------------
    // Contratti aperti a una data (Principale_ContrattiAperti, 20/11/2026)
    // ---------------------------------------------------------------------------------------------

    @Test
    void iContrattiApertiAUnaDataConPrezzoMedioEMargineParcheggiato() {
        //Apertura di 2 contratti con 13 USDC di margine, chiusura di 1 dopo un'ora, chiusura dell'altro dopo due ore
        for (String[] v : movimenti(apertura(), chiusuraParziale())) Principale.MappaCryptoWallet.put(v[0], v);

        assertTrue(Principale_ContrattiAperti.Calcola(T0 - 1_000L).isEmpty(), "prima dell'apertura non c'e' nulla");

        List<Principale_ContrattiAperti.Riga> dopoApertura = Principale_ContrattiAperti.Calcola(T0 + 1_800_000L);
        assertEquals(1, dopoApertura.size());
        Principale_ContrattiAperti.Riga r = dopoApertura.get(0);
        assertEquals(CONTRATTO, r.contratto);
        assertTrue(r.lungo);
        assertEquals("OKX", r.exchange);
        assertEquals(0, new java.math.BigDecimal("2").compareTo(r.contratti));
        assertEquals(0, new java.math.BigDecimal("13").compareTo(r.margine));
        assertEquals(0, new java.math.BigDecimal("60100.5").compareTo(r.prezzoMedio));
        assertEquals("USDC", r.moneta);
        assertFalse(r.idMovimento.isBlank(), "una riga di token per il riepilogo del contratto");
        assertTrue(Principale_Derivati.RiepilogoDisponibile(Principale.MappaCryptoWallet.get(r.idMovimento)));

        //Dopo la chiusura parziale: 1 contratto e meta' del margine
        r = Principale_ContrattiAperti.Calcola(T0 + 5_400_000L).get(0);
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(r.contratti));
        assertEquals(0, new java.math.BigDecimal("6.5").compareTo(r.margine));

        //A posizione chiusa non compare piu'
        for (String[] v : movimenti(chiusuraFinale())) Principale.MappaCryptoWallet.put(v[0], v);
        assertTrue(Principale_ContrattiAperti.Calcola(T0 + 86_400_000L).isEmpty());
        assertEquals(1, Principale_ContrattiAperti.Calcola(T0 + 5_400_000L).size(), "alla data di prima era ancora aperta");
        assertFalse(Principale_ContrattiAperti.TestoIntestazione("02/01/2026", 1).contains(";"));
    }

    // ---------------------------------------------------------------------------------------------
    // Il giro di un movimento (Principale_GiroMovimento, 20/11/2026)
    // ---------------------------------------------------------------------------------------------

    private static java.util.Set<String> idGiro(String id) {
        java.util.Set<String> s = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Principale_GiroMovimento.Voce x : Principale_GiroMovimento.Calcola(id).voci) s.add(x.id);
        return s;
    }

    private static String idDi(java.util.function.Predicate<String[]> filtro) {
        return Principale.MappaCryptoWallet.values().stream().filter(filtro).findFirst().orElseThrow()[0];
    }

    @Test
    void ilGiroDiUnaPosizioneEUgualeDaQualunqueMovimentoEdEsclusoLEpisodioDopo() {
        //Posizione 1: apertura, funding, chiusura parziale, chiusura finale. Posizione 2, dopo: un'altra apertura
        importa(apertura(), funding("2006", T0 + 1_800_000L, "-0.0009"), chiusuraParziale(), chiusuraFinale());
        java.util.Set<String> primaPosizione = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        primaPosizione.addAll(Principale.MappaCryptoWallet.keySet());
        importa(fill("2001", T0 + 36_000_000L, "1", "1", "6.5", "0", "-0.003"));

        String apertura = idDi(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && "1001".equals(Derivati.Dati(v).get("bill")));
        String profitto = idDi(v -> Derivati.Tipo(v).equals(Derivati.PNL) && "1002".equals(v[24]));
        String fundingPagato = idDi(v -> Derivati.Tipo(v).equals(Derivati.FUNDING));
        String margine = idDi(v -> Derivati.Tipo(v).equals(Derivati.MARGINE) && OKXDerivati.SOTTOWALLET.equals(v[4]));
        java.util.Set<String> giro = idGiro(apertura);
        assertEquals(primaPosizione, giro, "tutta la prima posizione, niente della seconda");
        assertEquals(giro, idGiro(profitto));
        assertEquals(giro, idGiro(fundingPagato));
        assertEquals(giro, idGiro(margine));
        String secondaApertura = idDi(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && "2001".equals(Derivati.Dati(v).get("bill")));
        assertFalse(giro.contains(secondaApertura));
        assertFalse(idGiro(secondaApertura).contains(apertura), "la posizione dopo e' un giro a se'");

        //Ogni riga ha una spiegazione, senza punto e virgola, e il contratto e' quello della posizione
        Principale_GiroMovimento.Giro g = Principale_GiroMovimento.Calcola(profitto);
        assertEquals(CONTRATTO, g.contratto);
        for (Principale_GiroMovimento.Voce x : g.voci) {
            assertFalse(x.spiegazione.isBlank(), x.id);
            assertFalse(x.spiegazione.contains(";"), x.spiegazione);
            assertFalse(x.spiegazione.contains(OperazioniCalcolate.PREFISSO), "mai la chiave di un'operazione");
        }
        assertEquals(1, g.voci.stream().filter(x -> x.legame == Principale_GiroMovimento.Legame.SCELTO).count());
        assertTrue(Principale_GiroMovimento.Spiega(Principale.MappaCryptoWallet.get(apertura), Principale.MappaCryptoWallet).startsWith("Apertura di 2 contratti long"));
        assertTrue(Principale_GiroMovimento.Spiega(Principale.MappaCryptoWallet.get(fundingPagato), Principale.MappaCryptoWallet).startsWith("Funding pagato"));
        assertTrue(Principale_GiroMovimento.Spiega(Principale.MappaCryptoWallet.get(margine), Principale.MappaCryptoWallet).startsWith("Giroconto dallo spot al sotto-wallet Derivati"));
    }

    @Test
    void ilGiroDiUnRientroAbbinatoComprendeIlGruppoELaChiusura() {
        depositoIniziale();
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        importa(chiusuraTotale("1003", T0 + 7_200_000L, "2", "-12.98", "0.003", "-0.0013"));
        String[] rientro = rientri().get(0);
        rientro[15] = "12.98";
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(rientro[0]);
        assertTrue(Prezzi.SenzaValorizzare(() -> Principale_RientroMargine.Applica(p, p.proposti)));

        //Dal vecchio prelievo: il gruppo dell'abbinamento (un passo di [20]) e, dal rientro, profitto e commissione della chiusura
        java.util.Set<String> giro = idGiro(idPrelievo("1001"));
        assertTrue(giro.containsAll(gruppoDi(rientro[0])), "tutto il gruppo dell'abbinamento");
        String pnl = idDi(v -> Derivati.Tipo(v).equals(Derivati.PNL) && "1003".equals(v[24]));
        assertTrue(idGiro(rientro[0]).contains(pnl), "il profitto della stessa chiusura");
        for (Principale_GiroMovimento.Voce x : Principale_GiroMovimento.Calcola(rientro[0]).voci) assertFalse(x.spiegazione.isBlank());
        assertTrue(Principale_GiroMovimento.Spiega(rientro, Principale.MappaCryptoWallet).contains("abbinato come sblocco"));
    }

    @Test
    void unMovimentoIsolatoHaUnGiroDiUnaRigaEUnaCommissioneCollegataPortaIlSuoMovimento() {
        depositoIniziale();
        String isolato = idDi(v -> "0001".equals(v[24]));
        Principale_GiroMovimento.Giro g = Principale_GiroMovimento.Calcola(isolato);
        assertEquals(1, g.voci.size());
        assertEquals("", g.contratto);
        assertTrue(g.voci.get(0).spiegazione.contains("da classificare"), g.voci.get(0).spiegazione);
        assertTrue(Principale_GiroMovimento.Calcola("inesistente").voci.isEmpty());

        //Una commissione collegata ([43]) porta il suo movimento, e viceversa
        aperturaPubblicata("1001", T0, "13.0", "-0.0065");
        String[] pc = Principale.MappaCryptoWallet.get(idPrelievo("1001"));
        String[] cm = Principale.MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_CM")).findFirst().orElseThrow();
        CommissioniCollegate.Collega(pc, cm);
        assertTrue(idGiro(cm[0]).contains(pc[0]));
        assertTrue(idGiro(pc[0]).contains(cm[0]));
        assertTrue(Principale_GiroMovimento.Spiega(cm, Principale.MappaCryptoWallet).startsWith("Commissione collegata"));
    }

    // ---------------------------------------------------------------------------------------------
    // Forma "conto" (decisione dell'utente del 22/11/2026)
    // ---------------------------------------------------------------------------------------------

    /** Un fill con il margine della posizione dopo il fill ({@code posBal}), come lo scrive OKX. */
    private static JSONObject fillConSaldo(String billId, long ts, String subType, String sz, String posBalChg, String pnl, String fee,
            String posBal) {
        return fill(billId, ts, subType, sz, posBalChg, pnl, fee).put("posBal", posBal);
    }

    /**
     * Profitto, perdita, commissione e funding stanno sul sotto-wallet dei derivati, e dopo ogni fill il sotto-wallet vale il margine della
     * posizione secondo OKX ({@code posBal}). Lo spot ha solo margine e commissione d'apertura: la perdita della chiusura non lo porta mai
     * sotto zero, perche' la paga il conto e rientra il netto. Apertura di 2 contratti con 13 USDC, funding pagato 0,009, chiusura di
     * meta' con profitto 0,003, chiusura del resto con perdita 0,2.
     */
    @Test
    void laFormaConto_ilSottoWalletSegueIlMargineDiOKXELoSpotNonVaMaiSottoZero() {
        chiusuraPubblicata("0001", T0 - 2_592_000_000L, "13.0065");
        Principale.MappaCryptoWallet.values().stream().filter(v -> "0001".equals(v[24])).findFirst().orElseThrow()[7] = "Deposit";
        JSONObject[] bill = {
            fillConSaldo("5001", T0, "1", "2", "13", "0", "-0.0065", "13"),
            funding("5002", T0 + 1_800_000L, "-0.009").put("posBal", "12.991"),
            fillConSaldo("5003", T0 + 3_600_000L, "2", "1", "-6.4955", "0.003", "-0.0013", "6.4955"),
            fillConSaldo("5004", T0 + 7_200_000L, "2", "1", "-6.4955", "-0.2", "-0.0013", "0")};
        java.math.BigDecimal spot = new java.math.BigDecimal("13.0065");
        for (JSONObject b : bill) {
            importa(b);
            spot = spot.add(new java.math.BigDecimal(b.getString("balChg")));
            java.math.BigDecimal conto = saldo(OKXDerivati.SOTTOWALLET, "USDC");
            assertEquals(0, new java.math.BigDecimal(b.getString("posBal")).compareTo(conto), "conto dopo il bill " + b.getString("billId") + ": " + conto);
            assertEquals(0, spot.compareTo(sommaUSDC().subtract(conto)), "spot dopo il bill " + b.getString("billId"));
            assertTrue(Funzioni.ControllaSaldiNegativi(Principale.MappaCryptoWallet.values()).isEmpty(), "nessun saldo negativo");
        }
        //Le righe del risultato stanno sul conto, il giroconto e' il netto: all'apertura margine piu' commissione, alla chiusura il balChg
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!"OKX".equals(v[3]) || "0001".equals(v[24])) continue;
            String tipo = Derivati.Tipo(v);
            if (tipo.equals(Derivati.PNL) || tipo.equals(Derivati.COMMISSIONE) || tipo.equals(Derivati.FUNDING)) {
                assertEquals(OKXDerivati.SOTTOWALLET, v[4], v[0]);
            }
        }
        java.util.function.Function<String, java.math.BigDecimal> giroconto = billId -> Principale.MappaCryptoWallet.values().stream()
                .filter(v -> billId.equals(v[24]) && Derivati.Tipo(v).equals(Derivati.MARGINE) && OKXDerivati.SOTTOWALLET.equals(v[4]))
                .map(OKXDerivati::QuantitaConSegno).findFirst().orElseThrow();
        assertEquals(0, new java.math.BigDecimal("13.0065").compareTo(giroconto.apply("5001")));
        assertEquals(0, new java.math.BigDecimal("-6.4972").compareTo(giroconto.apply("5003")));
        assertEquals(0, new java.math.BigDecimal("-6.2942").compareTo(giroconto.apply("5004")));
        //La commissione dell'apertura viene dopo il giroconto in entrata, nello stesso secondo: il conto ha gia' il denaro
        String commissione = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> "5001".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.COMMISSIONE)).findFirst().orElseThrow()[0];
        String entrata = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> "5001".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.MARGINE) && OKXDerivati.SOTTOWALLET.equals(v[4]))
                .findFirst().orElseThrow()[0];
        assertTrue(entrata.compareToIgnoreCase(commissione) < 0, entrata + " prima di " + commissione);
        //Contratti aperti dopo l'apertura e il funding: margine parcheggiato 12,991 (letto dai dati, non dal giroconto)
        List<Principale_ContrattiAperti.Riga> aperti = Principale_ContrattiAperti.Calcola(T0 + 3_000_000L, Principale.MappaCryptoWallet.values());
        assertEquals(1, aperti.size());
        assertEquals(0, new java.math.BigDecimal("12.991").compareTo(aperti.get(0).margine));
    }

    /**
     * Due aperture nello stesso secondo con gli stessi contratti, margine diverso e la stessa commissione, arrivate in due scaricamenti
     * diversi: la seconda non si scarta come doppione della prima (stessa chiave logica: istante, moneta, quantita'), perche' ha un altro
     * billId. Prima si perdevano il suo token e la sua commissione, e la chiusura non trovava tutti i contratti aperti.
     */
    @Test
    void dueFillUgualiNelloStessoSecondoInDueScaricamenti_nonSiScartano() {
        chiusuraPubblicata("0001", T0 - 2_592_000_000L, "20");
        Principale.MappaCryptoWallet.values().stream().filter(v -> "0001".equals(v[24])).findFirst().orElseThrow()[7] = "Deposit";
        JSONObject a1 = fillConSaldo("6001", T0, "1", "1", "6.5", "0", "-0.0013", "6.5");
        JSONObject a2 = fillConSaldo("6002", T0, "1", "1", "6.6", "0", "-0.0013", "13.1");
        JSONObject ch = fillConSaldo("6003", T0 + 3_600_000L, "2", "2", "-13.1", "0.01", "-0.0026", "0");
        importa(a1);
        importa(a2);
        assertEquals(2, Principale.MappaCryptoWallet.values().stream()
                .filter(v -> Derivati.Tipo(v).equals(Derivati.CONTRATTO) && !v[11].isBlank()).count(), "due token di apertura");
        assertEquals(2, Principale.MappaCryptoWallet.values().stream()
                .filter(v -> Derivati.Tipo(v).equals(Derivati.COMMISSIONE) && ("6001".equals(v[24]) || "6002".equals(v[24]))).count(), "due commissioni");
        importa(ch);
        assertEquals(0, saldo(OKXDerivati.SOTTOWALLET, "USDC").signum(), "conto a zero a posizione chiusa");
        assertEquals(0, new java.math.BigDecimal("20").add(sommaBalChg(a1, a2, ch)).compareTo(sommaUSDC()), "somma dei movimenti = deposito + balChg");
        assertTrue(rientri().isEmpty(), "la chiusura trova tutti i contratti fra i token");
        //Reimportando tutto non entra nulla
        int righe = Principale.MappaCryptoWallet.size();
        importa(a1, a2, ch);
        assertEquals(righe, Principale.MappaCryptoWallet.size());
    }

    /**
     * Inversione in net mode (D18 punto 10): chiusura del long e apertura dello short nello stesso millisecondo, con la stessa commissione,
     * arrivate in due scaricamenti diversi. Entrambe le commissioni restano, e il conto segue il margine di OKX.
     */
    @Test
    void inversioneInDueScaricamenti_entrambeLeCommissioniRestano() {
        chiusuraPubblicata("0001", T0 - 2_592_000_000L, "20");
        Principale.MappaCryptoWallet.values().stream().filter(v -> "0001".equals(v[24])).findFirst().orElseThrow()[7] = "Deposit";
        JSONObject ap = fillConSaldo("7001", T0, "1", "1", "6.5", "0", "-0.0013", "6.5");
        JSONObject chiude = fillConSaldo("7002", T0 + 3_600_000L, "2", "1", "-6.5", "-0.01", "-0.003", "0").put("ordId", "77");
        JSONObject apre = fillConSaldo("7003", T0 + 3_600_000L, "2", "1", "6.4", "0", "-0.003", "6.4").put("ordId", "77");
        importa(ap);
        importa(chiude);
        importa(apre);
        assertEquals(3, Principale.MappaCryptoWallet.values().stream().filter(v -> Derivati.Tipo(v).equals(Derivati.COMMISSIONE)).count(),
                Principale.MappaCryptoWallet.values().stream().map(v -> v[0] + " " + v[24]).toList().toString());
        assertEquals(0, new java.math.BigDecimal("6.4").compareTo(saldo(OKXDerivati.SOTTOWALLET, "USDC")));
        assertEquals(0, new java.math.BigDecimal("20").add(sommaBalChg(ap, chiude, apre)).add(new java.math.BigDecimal("6.4")).compareTo(sommaUSDC()));
    }
}
