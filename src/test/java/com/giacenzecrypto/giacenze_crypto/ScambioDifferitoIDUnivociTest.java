package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayList;
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
 * Fissa gli ID dei movimenti generati da {@link GUI_ClassificazioneMovimento#CreaMovimentiScambioCryptoDifferito}.
 *
 * <p>Lo scambio (MS) e il trasferimento dalla piattaforma (MT2) prendono l'istante del deposito e la coda
 * dell'ID del prelievo. Due scambi differiti con lo stesso istante di deposito e prelievi con la stessa coda
 * (due contratti Dual Investment di Binance liquidati nello stesso secondo) producevano lo stesso ID, e il
 * secondo sovrascriveva lo scambio del primo: una permuta spariva in silenzio e restava un saldo fantasma
 * sulla piattaforma. Ora gli ID generati sono resi univoci, e se non si possono generare non si tocca nulla.
 */
class ScambioDifferitoIDUnivociTest {

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
    }

    private static String[] movimento(String ID, String moneta, String qta, String data) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = ID.endsWith("_PC") ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        if (ID.endsWith("_PC")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    /** Prezzo in cache all'istante del deposito, così lo scambio non cerca il prezzo in rete. */
    private static void seedPrezzo(String simbolo, String data) throws Exception {
        long ts = FunzioniDate.ConvertiDatainLongMinuto(data);
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

    private static final String DATA_DEPOSITO = "2022-08-26 10:40:36";

    /** Due contratti: prelievi in giorni diversi con la stessa coda dell'ID, depositi nello stesso secondo. */
    private static String[][] dueContrattiNelloStessoSecondo() throws Exception {
        String[] p1 = movimento("20220408141952_Binance_001_001_PC", "BUSD", "100", "2022-04-08 14:19:52");
        String[] p2 = movimento("20220417122647_Binance_001_001_PC", "BUSD", "38.4", "2022-04-17 12:26:47");
        String[] d1 = movimento("20220826104036_Binance_001_001_DC", "BTC", "0.005", DATA_DEPOSITO);
        String[] d2 = movimento("20220826104036_Binance_001_002_DC", "BTC", "0.002", DATA_DEPOSITO);
        for (String[] v : new String[][]{p1, p2, d1, d2}) MappaCryptoWallet.put(v[0], v);
        seedPrezzo("BTC", DATA_DEPOSITO);
        seedPrezzo("BUSD", DATA_DEPOSITO);
        return new String[][]{p1, p2, d1, d2};
    }

    /** I cinque movimenti dello scambio di cui fa parte il prelievo indicato, letti dal suo [20]. */
    private static List<String[]> scambioDi(String[] prelievo) {
        List<String[]> ris = new ArrayList<>();
        ris.add(prelievo);
        for (String ID : prelievo[20].split(",")) ris.add(MappaCryptoWallet.get(ID));
        return ris;
    }

    @Test
    void dueScambiConDepositoNelloStessoSecondo_nessunMovimentoSovrascritto() throws Exception {
        String[][] m = dueContrattiNelloStessoSecondo();

        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[0][0], m[2][0], "Dual Savings"));
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[1][0], m[3][0], "Dual Savings"));

        assertEquals(10, MappaCryptoWallet.size(), "due scambi da cinque movimenti, nessuno sovrascritto");
        assertEquals(2, MappaCryptoWallet.keySet().stream().filter(k -> k.endsWith("_SC")).count(), "due scambi distinti");

        //Ogni membro di ogni scambio esiste e cita tutti gli altri membri del proprio scambio
        for (String[] prelievo : new String[][]{m[0], m[1]}) {
            List<String[]> membri = scambioDi(prelievo);
            assertEquals(5, membri.size());
            for (String[] v : membri) {
                assertNotNull(v, "membro mancante nello scambio di " + prelievo[0]);
                assertSame(v, MappaCryptoWallet.get(v[0]), "membro sovrascritto: " + v[0]);
                for (String[] altro : membri) {
                    if (altro != v) assertTrue(List.of(v[20].split(",")).contains(altro[0]),
                            v[0] + " non cita " + altro[0]);
                }
            }
        }
    }

    @Test
    void ordineDeiMovimentiGeneratiResta_trasferimento_scambio_trasferimento() throws Exception {
        String[][] m = dueContrattiNelloStessoSecondo();
        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[0][0], m[2][0], "Dual Savings");
        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[1][0], m[3][0], "Dual Savings");

        //Il secondo scambio ha gli ID resi univoci: lo scambio deve restare fra il proprio trasferimento
        //verso la piattaforma e quello di uscita, e prima del proprio deposito
        String[] ID = m[1][20].split(",");
        String IDMT1 = ID[0], IDMS = ID[1], IDMT2 = ID[2];
        assertTrue(IDMS.endsWith("_SC") && IDMT2.endsWith("_PC") && IDMT1.endsWith("_DC"), String.join(",", ID));
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(IDMS, IDMT2) < 0, "lo scambio precede l'uscita dalla piattaforma");
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(IDMT2, m[3][0]) < 0, "l'uscita precede il deposito");
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(m[1][0], IDMT1) < 0, "il prelievo precede l'entrata sulla piattaforma");
    }

    @Test
    void annullareUnoScambio_lasciaIntattoLAltro() throws Exception {
        String[][] m = dueContrattiNelloStessoSecondo();
        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[0][0], m[2][0], "Dual Savings");
        GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(m[1][0], m[3][0], "Dual Savings");
        List<String> secondo = new ArrayList<>();
        for (String[] v : scambioDi(m[1])) secondo.add(v[0]);

        String[] parti = (m[0][0] + "," + m[0][20]).split(",");
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(parti, m[0][0]);

        for (String ID : secondo) assertNotNull(MappaCryptoWallet.get(ID), "l'annullamento del primo ha cancellato " + ID);
        assertNotNull(MappaCryptoWallet.get("20220408141952_Binance_001_001_PC"), "prelievo del primo ripristinato");
        assertNotNull(MappaCryptoWallet.get("20220826104036_Binance_001_001_DC"), "deposito del primo ripristinato");
        assertEquals(7, MappaCryptoWallet.size());
    }

    @Test
    void senzaIDUnivocoDisponibile_nonToccaNulla() throws Exception {
        //Quarto segmento non numerico: getIDUnivoco non può incrementarlo
        String[] p = movimento("20220408141952_Binance_001_X_PC", "BUSD", "100", "2022-04-08 14:19:52");
        String[] d = movimento("20220826104036_Binance_001_001_DC", "BTC", "0.005", DATA_DEPOSITO);
        //Un movimento occupa già l'ID che toccherebbe allo scambio
        String[] occupante = movimento("20220826104036_02Binance_001_X_SC", "BTC", "1", DATA_DEPOSITO);
        for (String[] v : new String[][]{p, d, occupante}) MappaCryptoWallet.put(v[0], v);
        Map<String, String> prima = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) prima.put(v[0], String.join(";", v));

        assertNull(GUI_ClassificazioneMovimento.IDScambioDifferito(p[0], d[0]));
        assertFalse(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], d[0]));

        Map<String, String> dopo = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) dopo.put(v[0], String.join(";", v));
        assertEquals(prima, dopo, "nessun movimento modificato, tolto o aggiunto");
    }

    @Test
    void movimentoInesistente_nonToccaNulla() {
        String[] d = movimento("20220826104036_Binance_001_001_DC", "BTC", "0.005", DATA_DEPOSITO);
        MappaCryptoWallet.put(d[0], d);
        assertFalse(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito("20220408141952_Binance_001_001_PC", d[0]));
        assertEquals(1, MappaCryptoWallet.size());
        assertSame(d, MappaCryptoWallet.get(d[0]));
    }
}
