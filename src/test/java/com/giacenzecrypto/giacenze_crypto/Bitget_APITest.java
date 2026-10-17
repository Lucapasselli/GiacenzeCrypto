package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Import Bitget via API fiscale ({@link Bitget_API}): firma, conversione dei record in righe del CSV e
 * passaggio dalla configurazione {@code Bitget CSV.json}. Record sintetici, nessun dato reale, nessuna rete.
 */
class Bitget_APITest {

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

    private static ConfigurazioneImport bitget() throws Exception {
        return ConfigurazioneImport.carica("config/import/Bitget CSV.json");
    }

    private static JSONObject record(String id, long ts, String coin, String tipo, String amount, String biz) {
        return new JSONObject().put("id", id).put("ts", String.valueOf(ts)).put("coin", coin)
                .put("spotTaxType", tipo).put("amount", amount).put("fee", "0").put("balance", "0")
                .put("bizOrderId", biz);
    }

    /** 2025-03-10 08:00:00 UTC */
    private static final long T = 1741593600000L;

    @Test
    void firma_base64DiHmacSha256SuTimestampMetodoPercorso() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("startTime", "1");
        p.put("endTime", "2");
        String pq = "/api/v2/tax/spot-record" + Bitget_API.Query(p);
        assertEquals("/api/v2/tax/spot-record?startTime=1&endTime=2", pq);
        assertEquals("oN7/ZK6CPcRavTQGnJrHlJffgLcxpasjh413jyaVZfg=",
                Bitget_API.Firma("segreto", "1700000000000", "GET", pq, ""));
    }

    @Test
    void inRighe_colonneDelCsvDataUtcECodiciTradotti() throws Exception {
        List<JSONObject> rec = List.of(record("11", T, "XAUT", "batch_interest_user_in", "0.0000031", "1"));
        String[] r = Bitget_API.InRighe(rec, bitget().causaliAPI).get(0);
        assertArrayEquals(new String[]{"11", "2025-03-10 08:00:00", "XAUT", "Interest", "0.0000031", "0", "0"}, r);
    }

    @Test
    void inRighe_tipoVuoto_coppiaLaunchXSoloConEntrambeLeGambe() throws Exception {
        List<JSONObject> rec = new ArrayList<>();
        rec.add(record("1", T, "BGB", "", "-0.5", "77"));
        rec.add(record("2", T, "J", "", "9", "77"));
        rec.add(record("3", T + 5000, "ABC", "", "4", "88"));
        List<String[]> righe = Bitget_API.InRighe(rec, bitget().causaliAPI);
        assertEquals("LaunchX expense", righe.get(0)[3]);
        assertEquals("LaunchX income", righe.get(1)[3]);
        assertEquals(Bitget_API.TIPO_VUOTO, righe.get(2)[3], "un tipo vuoto senza controparte resta sconosciuto");
        assertEquals(Map.of(Bitget_API.TIPO_VUOTO, 1), Bitget_API.TipiSconosciuti(righe, bitget()));
    }

    @Test
    void causaliAPI_portanoTutteAUnaCausaleNota() throws Exception {
        ConfigurazioneImport c = bitget();
        assertFalse(c.causaliAPI.isEmpty());
        for (Map.Entry<String, String> e : c.causaliAPI.entrySet()) {
            assertTrue(c.mappaCausali.containsKey(e.getValue()), e.getKey() + " -> " + e.getValue() + " non e' in mappaCausali");
        }
    }

    @Test
    void consolidaPerGiorno_conDataUltimaRigaPrendeLUltimoAccredito() throws Exception {
        ConfigurazioneImport c = bitget();
        List<String[]> righe = new ArrayList<>();
        righe.add(new String[]{"1", "2025-03-10 08:00:00", "BGB", "Interest", "0.1", "0", "0"});
        righe.add(new String[]{"2", "2025-03-10 09:00:00", "BGB", "Interest", "0.2", "0", "0"});
        assertEquals("2025-03-10 08:00:00", ImportazioneGenerica.consolidaCausaliPerGiorno(copia(righe), c, false).get(0)[1]);
        List<String[]> u = ImportazioneGenerica.consolidaCausaliPerGiorno(copia(righe), c, true);
        assertEquals(1, u.size());
        assertEquals("2025-03-10 09:00:00", u.get(0)[1]);
        assertEquals(0, new BigDecimal(u.get(0)[4]).compareTo(new BigDecimal("0.3")));
    }

    @Test
    void interessiConIDueNomiDellApi_unSoloMovimentoAlGiorno() throws Exception {
        ConfigurazioneImport c = bitget();
        c.colonnaValoreEuro = 7;
        List<JSONObject> rec = new ArrayList<>();
        rec.add(record("1", T, "BGB", "Interest", "0.1", "1"));
        rec.add(record("2", T + 3600_000, "BGB", "batch_interest_user_in", "0.2", "2"));
        rec.add(record("3", T + 7200_000, "BGB", "financial_lock_out", "-5", "3"));
        List<String[]> righe = new ArrayList<>();
        for (String[] r : Bitget_API.InRighe(rec, c.causaliAPI)) {
            righe.add(new String[]{r[0], r[1], r[2], r[3], r[4], r[5], r[6], "5.00"});
        }
        Importazioni.AzzeraContatori();
        ImportazioneGenerica.ordinaRighe(righe, c);
        righe = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, c, true);
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(righe, c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        assertEquals(0, Importazioni.TrasazioniSconosciute);
        assertEquals(1, movs.size(), "i due interessi sono un solo EARN, il passaggio al conto Earn e' ignorato");
        String[] m = movs.get(0);
        assertEquals("RW", m[0].substring(m[0].lastIndexOf('_') + 1));
        assertEquals(0, new BigDecimal(m[13]).compareTo(new BigDecimal("0.3")));
    }

    private static List<String[]> copia(List<String[]> righe) {
        List<String[]> out = new ArrayList<>();
        for (String[] r : righe) out.add(r.clone());
        return out;
    }
}
