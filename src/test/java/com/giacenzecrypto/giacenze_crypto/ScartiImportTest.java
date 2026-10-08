package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link ScartiImport}: la rilettura dei documenti OKX alla ricerca dei movimenti scartati perché
 * sconosciuti.
 *
 * <p>Il caso reale da cui nasce: bill {@code type 328} (Staking earnings) scaricati prima che la tabella dei
 * codici li conoscesse, mai più richiesti perché la data di partenza degli scaricamenti li aveva superati. I
 * test difendono le tre regole della nota di classe: certo solo se registrato, presenza giudicata per unità (un
 * ordine in più fill non è un candidato), unità mista segnalata e non recuperabile.
 */
class ScartiImportTest {

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
    void puliscePartenza() throws Exception {
        DocumentiFonte.ScartaBuffer();
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            DocumentiFonte.Annulla(d.Id);
        }
        try (var st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM SCARTI_IMPORT");
            st.execute("DELETE FROM SCARTI_ANALISI");
        }
        Principale.InterrompiCiclo = false;
    }

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    private static final long T0 = 1784268000000L;   //2026-07-17 08:00 circa

    /** Un bill nel formato di OKX_Bills.js */
    private static String bill(String billId, String type, String ccy, String balChg, long ts, String ordId) {
        return "{\"billId\":\"" + billId + "\",\"type\":\"" + type + "\",\"ccy\":\"" + ccy + "\",\"balChg\":\""
                + balChg + "\",\"ts\":\"" + ts + "\",\"notes\":\"\"" + (ordId == null ? "" : ",\"ordId\":\"" + ordId + "\"") + "}";
    }

    /** Crea un documento NDJSON di uno scaricamento OKX con i bill dati e ne restituisce l'id */
    private static int documento(List<String> funding, List<String> trading) {
        int id = DocumentiFonte.ApriSessione("OKX");
        assertTrue(id > 0);
        String risposta = "{\"okx_fundingBills\":[" + String.join(",", funding) + "],\"okx_tradingBills\":["
                + String.join(",", trading) + "],\"okx_completo\":true}";
        DocumentiFonte.AggiungiAllaSessione(id, "OKX_Bills", "OKX startDate=0", risposta);
        DocumentiFonte.ChiudiSessione(id);
        return id;
    }

    /** Un movimento OKX in archivio con quel [24] */
    private static void inArchivio(Map<String, String[]> archivio, String id24) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = "20260717080000_OKX" + id24 + "_001_001_DC";
        v[3] = "OKX";
        v[24] = id24;
        archivio.put(v[0], v);
    }

    private static void registra(int idDoc, String chiave) {
        List<String[]> s = new ArrayList<>();
        s.add(new String[]{chiave, "OKX type 328", "2026-07-17 08:00:00"});
        ScartiImport.Registra(ScartiImport.ORIGINE_OKX, idDoc, s);
    }

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------

    @Test
    void unRecordGiaRegistratoNonVieneSovrascrittoENonPerdeLIgnorato() {
        int doc = documento(List.of(bill("1", "328", "OKSOL", "0.01", T0, null)), List.of());
        registra(doc, "Funding:1");
        assertTrue(ScartiImport.HaScarti(doc));

        ScartiImport.Candidato c = new ScartiImport.Candidato();
        c.IdDocumento = doc;
        c.Righe.add(new String[]{"2026-07-17 08:00:00", "OKX", "Funding", "", "Staking earnings", "OKSOL", "0.01",
            "", "", "", "", "", "", "", "1", "NO", "", "", ""});
        ScartiImport.Ignora(List.of(c));
        registra(doc, "Funding:1");   //un secondo scaricamento sovrapposto rivede lo stesso bill
        assertEquals("IGNORATO", ScartiImport.Stati(ScartiImport.ORIGINE_OKX).get("Funding:1"));
    }

    @Test
    void senzaDocumentoNonSiRegistraNullaEDocumentoEliminatoPortaViaIlRegistro() {
        registra(0, "Funding:9");
        assertTrue(ScartiImport.Stati(ScartiImport.ORIGINE_OKX).isEmpty());

        int doc = documento(List.of(bill("1", "328", "OKSOL", "0.01", T0, null)), List.of());
        registra(doc, "Funding:1");
        ScartiImport.SegnaAnalizzato(doc, "x");
        DocumentiFonte.Annulla(doc);
        assertFalse(ScartiImport.HaScarti(doc));
        assertNull(ScartiImport.ImprontaAnalisi(doc));
    }

    @Test
    void leRigheSconosciuteSonoQuelleSenzaCategoriaConContoEBillIdNellaChiave() {
        String[] nota = {"2026-07-17 08:00:00", "OKX", "Funding", "REWARD", "Staking earnings", "OKSOL", "1",
            "", "", "", "", "", "", "", "11", "NO", "", "", ""};
        String[] ignota = {"2026-07-17 08:00:00", "OKX", "Trading", "", "OKX type 999", "BTC", "1",
            "", "", "", "", "", "", "", "12", "NO", "", "", ""};
        List<String[]> s = ScartiImport.RigheSconosciute(List.of(nota, ignota));
        assertEquals(1, s.size());
        assertEquals("Trading:12", s.get(0)[0]);
    }

    // ------------------------------------------------------------------
    // Analisi
    // ------------------------------------------------------------------

    @Test
    void ilRewardScartatoERegistratoECertoQuelloSoloAssenteEIncerto() {
        int doc = documento(List.of(
                bill("100", "1", "USDT", "50", T0, null),               //deposito gia' in archivio
                bill("101", "328", "OKSOL", "0.0118", T0 + 3_600_000, null),   //scartato e registrato
                bill("102", "328", "OKSOL", "0.0112", T0 + 7_200_000, null),   //assente, mai registrato
                bill("103", "99999", "ABC", "1", T0 + 9_000_000, null)),       //ancora sconosciuto
                List.of());
        Map<String, String[]> archivio = new TreeMap<>();
        inArchivio(archivio, "100");
        registra(doc, "Funding:101");

        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(archivio, false);
        assertEquals(2, a.Recuperabili.size(), "solo i due reward: il deposito c'e', il type 99999 resta ignoto");
        assertTrue(a.Misti.isEmpty());
        ScartiImport.Candidato certo = a.Recuperabili.stream().filter(c -> c.Righe.get(0)[14].equals("101")).findFirst().orElseThrow();
        ScartiImport.Candidato incerto = a.Recuperabili.stream().filter(c -> c.Righe.get(0)[14].equals("102")).findFirst().orElseThrow();
        assertTrue(certo.Certo);
        assertFalse(incerto.Certo);
        assertEquals("Staking earnings", certo.Causali());
        assertEquals(doc, certo.IdDocumento);
        assertTrue(a.SenzaSospesi.isEmpty(), "un documento con candidati non va segnato come analizzato");
    }

    @Test
    void unOrdineInPiuFillGiaImportatoNonEUnCandidato() {
        //4 bill dello stesso ordine: il movimento consolidato in archivio cita solo i primi due
        int doc = documento(List.of(), List.of(
                bill("201", "2", "USDC", "-10", T0, "O1"),
                bill("202", "2", "BTC", "0.0001", T0, "O1"),
                bill("203", "2", "USDC", "-5", T0, "O1"),
                bill("204", "2", "BTC", "0.00005", T0, "O1")));
        Map<String, String[]> archivio = new TreeMap<>();
        inArchivio(archivio, "201-202");

        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(archivio, false);
        assertTrue(a.Vuota());
        assertEquals(List.of(doc), a.SenzaSospesi);
    }

    @Test
    void unoScambioDiCuiUnaGambaEraScartataEMisto() {
        int doc = documento(List.of(), List.of(
                bill("301", "2", "USDC", "-10", T0, "O2"),
                bill("302", "2", "BTC", "0.0001", T0, "O2")));
        Map<String, String[]> archivio = new TreeMap<>();
        inArchivio(archivio, "301");
        registra(doc, "Trading:302");

        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(archivio, false);
        assertTrue(a.Recuperabili.isEmpty());
        assertEquals(1, a.Misti.size());
        assertEquals(2, a.Misti.get(0).Righe.size(), "le due gambe dello scambio sono un'unita' sola");
    }

    @Test
    void unDocumentoGiaRilettoConLeStesseMappeNonSiRilegge() {
        int doc = documento(List.of(bill("401", "328", "OKSOL", "0.01", T0, null)), List.of());
        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(new TreeMap<>(), false);
        assertEquals(1, a.Recuperabili.size());

        ScartiImport.SegnaAnalizzato(doc, a.Impronta);
        assertTrue(ScartiImport.AnalizzaOKX(new TreeMap<>(), false).Vuota());
        assertEquals(1, ScartiImport.AnalizzaOKX(new TreeMap<>(), true).Recuperabili.size());

        ScartiImport.SegnaAnalizzato(doc, "impronta di mappe vecchie");
        assertEquals(1, ScartiImport.AnalizzaOKX(new TreeMap<>(), false).Recuperabili.size());
    }

    @Test
    void ciCheLUtenteHaIgnoratoNonTornaPiu() {
        documento(List.of(bill("501", "328", "OKSOL", "0.01", T0, null)), List.of());
        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(new TreeMap<>(), false);
        ScartiImport.Ignora(a.Recuperabili);
        assertTrue(ScartiImport.AnalizzaOKX(new TreeMap<>(), true).Vuota());
    }

    @Test
    void loStessoBillInDueDocumentiSiProponeUnaVoltaSolaDalPiuVecchio() {
        int vecchio = documento(List.of(bill("601", "328", "OKSOL", "0.01", T0, null)), List.of());
        documento(List.of(bill("601", "328", "OKSOL", "0.01", T0, null)), List.of());
        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(new TreeMap<>(), false);
        assertEquals(1, a.Recuperabili.size());
        assertEquals(vecchio, a.Recuperabili.get(0).IdDocumento);
    }

    @Test
    void unCandidatoConfermatoCheLImportNonHaScrittoRisultaNonRecuperato() {
        documento(List.of(
                bill("901", "328", "OKSOL", "0.01", T0, null),
                bill("902", "328", "OKSOL", "0.02", T0 + 3_600_000, null)), List.of());
        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(new TreeMap<>(), false);
        assertEquals(2, a.Recuperabili.size());

        //Dopo il recupero l'archivio contiene solo il primo: il secondo non e' stato scritto
        Map<String, String[]> archivio = new TreeMap<>();
        inArchivio(archivio, "901");
        List<ScartiImport.Candidato> mancanti = ScartiImport.NonRecuperati(a.Recuperabili, archivio);
        assertEquals(1, mancanti.size());
        assertEquals("902", mancanti.get(0).Righe.get(0)[14]);

        //Segnato ignorato non torna piu', anche a mappe cambiate
        ScartiImport.Ignora(mancanti);
        ScartiImport.Analisi dopo = ScartiImport.AnalizzaOKX(archivio, true);
        assertTrue(dopo.Vuota());
    }

    /** Documento con una risposta OKX_Earn che porta lo storico On-chain Earn dato */
    private static int documentoConStorico(List<String> funding, String storico) {
        int id = DocumentiFonte.ApriSessione("OKX");
        DocumentiFonte.AggiungiAllaSessione(id, "OKX_Bills", "OKX startDate=0",
                "{\"okx_fundingBills\":[" + String.join(",", funding) + "],\"okx_tradingBills\":[],\"okx_completo\":true}");
        DocumentiFonte.AggiungiAllaSessione(id, "OKX_Earn", "OKX startDate=0", "{\"staking_storico\":[" + storico + "]}");
        DocumentiFonte.ChiudiSessione(id);
        return id;
    }

    @Test
    void unaConversioneInLiquidStakingSiRecuperaSoloConLaGambaInUscitaRicostruita() {
        //Caso della segnalazione: 43,248 SOL in On-chain Earn convertiti in 43,248 OKSOL (type 330), il bill
        //porta solo l'entrata. Lo storico conservato nello stesso documento permette di ricostruire l'uscita.
        long ts = 1784192108000L;
        documentoConStorico(List.of(bill("1001", "330", "OKSOL", "43.248", ts, null)),
                "{\"ordId\":\"10297757\",\"ccy\":\"SOL\",\"state\":\"3\",\"redeemedTime\":\"" + ts
                + "\",\"investData\":[{\"ccy\":\"SOL\",\"amt\":\"43.248\"}]}");
        ScartiImport.Analisi a = ScartiImport.AnalizzaOKX(new TreeMap<>(), false);
        assertEquals(1, a.Recuperabili.size());
        ScartiImport.Candidato c = a.Recuperabili.get(0);
        assertEquals(2, c.Righe.size(), "entrata OKSOL e uscita SOL ricostruita, un solo scambio");
        assertTrue(c.Movimento().contains("-43.248 SOL"), c.Movimento());
    }

    @Test
    void unaConversioneInLiquidStakingSenzaPosizioneDiOrigineNonSiPropone() {
        //Senza la posizione nello storico l'import la lascia fra le sconosciute: recuperarla darebbe OKSOL dal nulla
        documentoConStorico(List.of(bill("1002", "330", "OKSOL", "0.3", T0, null)), "");
        assertTrue(ScartiImport.AnalizzaOKX(new TreeMap<>(), false).Vuota());
    }

    @Test
    void unDocumentoTroncatoSiLeggeFinoAllUltimaRigaCompleta() throws Exception {
        int id = DocumentiFonte.ApriSessione("OKX");
        DocumentiFonte.ChiudiSessione(id);
        File gz = DocumentiFonte.FileConservato(id);
        String riga = "{\"ts\":1,\"script\":\"OKX_Bills\",\"argomenti\":\"OKX startDate=0\",\"risposta\":{\"okx_fundingBills\":["
                + bill("701", "328", "OKSOL", "0.01", T0, null) + "],\"okx_tradingBills\":[]}}\n";
        //Scritto e non chiuso: lo stream gzip finisce senza il suo trailer, come quando il programma viene chiuso a meta' scaricamento
        OutputStream out = new GZIPOutputStream(new FileOutputStream(gz));
        out.write(riga.getBytes(StandardCharsets.UTF_8));
        out.write("{\"ts\":2,\"script\":\"OKX_Bills\",\"risp".getBytes(StandardCharsets.UTF_8));
        out.flush();
        ((GZIPOutputStream) out).finish();
        out.close();
        //Tronca a meta' del trailer
        try (var raf = new java.io.RandomAccessFile(gz, "rw")) {
            raf.setLength(raf.length() - 4);
        }
        List<String[]> righe = ScartiImport.LeggiRigheOKX(id, TipiOKX.Carica());
        assertNotNull(righe);
        assertEquals(1, righe.size());
        assertEquals("701", righe.get(0)[14]);
    }

    @Test
    void ilRaggruppamentoSeparatoDalConsolidamentoDaGliStessiGruppi() {
        TipiOKX tipi = TipiOKX.Carica();
        com.google.gson.JsonArray bills = com.google.gson.JsonParser.parseString("[" + String.join(",",
                bill("801", "2", "USDC", "-10", T0, "A"),
                bill("802", "2", "BTC", "0.0001", T0, "A"),
                bill("803", "2", "USDC", "-5", T0, "B"),
                bill("804", "2", "ETH", "0.002", T0, "B"),
                bill("805", "2", "USDC", "-1", T0 + 5000, "C")) + "]").getAsJsonArray();
        List<String[]> righe = Importazioni.Ex_OKX_Ordina(CcxtInterop.convertOKXBills(bills, "Trading", tipi));
        List<List<String[]>> gruppi = Importazioni.Ex_OKX_Raggruppa(righe);
        assertEquals(3, gruppi.size(), "due ordini nello stesso istante e un terzo dopo: tre gruppi");
        assertEquals(2, gruppi.get(0).size());
        assertEquals(2, gruppi.get(1).size());
        assertEquals(1, gruppi.get(2).size());
    }
}
