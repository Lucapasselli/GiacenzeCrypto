package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link OperazioniCalcolate}: l'operazione di un movimento è l'unione dei legami {@code [20]} (classificazione),
 * {@code [43]} (commissioni), {@code [45]} (identità, contratto Dual) e stesso {@code [24]} sullo stesso exchange entro
 * un'ora. Non si salva: annullare una classificazione la scioglie da sola.
 */
class OperazioniCalcolateTest {

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
    void svuota() throws Exception {
        MappaCryptoWallet.clear();
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (String c : new String[]{"BTC", "USDT"}) {
                ps.setLong(1, FunzioniDate.ConvertiDatainLongMinuto("2022-05-24 08:33:41"));
                ps.setString(2, "binance");
                ps.setString(3, c);
                ps.setBigDecimal(4, BigDecimal.ONE);
                ps.executeUpdate();
            }
        }
    }

    private static String[] movimento(String ID, String exchange, String moneta, String qta, String data) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = exchange;
        v[4] = "Principale";
        v[5] = ID.endsWith("_PC") || ID.endsWith("_CM") ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        if (ID.endsWith("_PC") || ID.endsWith("_CM")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static Map<String, String> calcola() {
        return OperazioniCalcolate.Calcola(MappaCryptoWallet);
    }

    @Test
    void trasferimentoConCommissioneDiRete_unaSolaOperazione() {
        String[] p = movimento("20230101095000_Kraken_001_001_PC", "Kraken", "BTC", "0.0105", "2023-01-01 09:50:00");
        String[] fee = movimento("20230101095000_Kraken_001_002_CM", "Kraken", "BTC", "0.0001", "2023-01-01 09:50:00");
        String[] d = movimento("20230101100000_Binance_001_001_DC", "Binance", "BTC", "0.01", "2023-01-01 10:00:00");
        CommissioniCollegate.Collega(p, fee);
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);

        Map<String, String> op = calcola();

        assertEquals(4, MappaCryptoWallet.size(), "prelievo, commissione di rete, deposito, commissione del trasferimento");
        String K = op.get(p[0]);
        assertEquals(OperazioniCalcolate.PREFISSO + p[0], K, "chiave dal primo movimento");
        for (String[] v : MappaCryptoWallet.values()) assertEquals(K, op.get(v[0]), v[0]);
    }

    @Test
    void contrattoDual_laChiaveELIdentitaDelContratto() {
        String[] p = movimento("20220512062450_Binance_001_001_PC", "Binance", "USDT", "100", "2022-05-12 06:24:50");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "Binance", "BTC", "0.0035", "2022-05-24 08:33:41");
        GruppoOperazione.Scrivi(p, "DUAL-7");
        GruppoOperazione.Scrivi(s, "DUAL-7");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));

        Map<String, String> op = calcola();

        assertEquals(5, op.size());
        for (String[] v : MappaCryptoWallet.values()) assertEquals("DUAL-7", op.get(v[0]), v[0]);
    }

    @Test
    void annullareLaClassificazione_sciolgeLOperazione() {
        String[] p = movimento("20220512062450_Binance_001_001_PC", "Binance", "USDT", "100", "2022-05-12 06:24:50");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "Binance", "BTC", "0.0035", "2022-05-24 08:33:41");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));
        assertEquals(5, calcola().size());

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((p[0] + "," + p[20]).split(","), p[0]);

        assertTrue(calcola().isEmpty(), "prelievo e deposito tornano movimenti a sé");
    }

    @Test
    void stessoHash_stessoExchangeEntroUnOra_eUnaOperazione() {
        String[] a = movimento("20230101100000_Wallet_001_001_DC", "0xWallet", "TOKA", "1", "2023-01-01 10:00:00");
        String[] b = movimento("20230101100000_Wallet_001_002_DC", "0xWallet", "TOKB", "2", "2023-01-01 10:00:00");
        a[24] = "0xabc123def456";
        b[24] = "0xABC123DEF456";

        Map<String, String> op = calcola();

        assertNotNull(op.get(a[0]), "maiuscole e minuscole non contano");
        assertEquals(op.get(a[0]), op.get(b[0]));
    }

    @Test
    void stessoHash_nonBasta_seCambiaExchangeOSiSuperaLOraOESegnaposto() {
        String[] a = movimento("20230101100000_Binance_001_001_DC", "Binance", "BTC", "1", "2023-01-01 10:00:00");
        String[] b = movimento("20230101100000_Kraken_001_001_DC", "Kraken", "BTC", "1", "2023-01-01 10:00:00");
        String[] c = movimento("20230101120000_Binance_002_001_DC", "Binance", "BTC", "1", "2023-01-01 12:00:00");
        String[] d = movimento("20230101100000_Binance_003_001_DC", "Binance", "ETH", "1", "2023-01-01 10:00:00");
        String[] e = movimento("20230101100000_Binance_004_001_DC", "Binance", "SOL", "1", "2023-01-01 10:00:00");
        for (String[] v : new String[][]{a, b, c}) v[24] = "12345678";
        d[24] = "-";
        e[24] = "-";

        Map<String, String> op = calcola();

        assertNull(op.get(b[0]), "altro exchange: id d'ordine non confrontabile");
        assertNull(op.get(c[0]), "due ore dopo: un'altra transazione");
        assertNull(op.get(d[0]), "un segnaposto non lega nulla");
        assertNull(op.get(e[0]));
        assertNull(op.get(a[0]), "resta da solo, quindi non è un'operazione");
    }

    @Test
    void oltreVentiMovimentiConLoStessoHash_ilValoreNonIdentificaUnaTransazione() {
        for (int i = 1; i <= 21; i++) {
            String[] v = movimento(String.format("20230101100000_Binance_%03d_001_DC", i), "Binance", "BTC", "1", "2023-01-01 10:00:00");
            v[24] = "ripetuto";
        }

        assertTrue(calcola().isEmpty());
    }

    @Test
    void unMovimentoSolo_nonEUnOperazione_salvoAbbiaUnIdentita() {
        String[] a = movimento("20230101100000_Binance_001_001_DC", "Binance", "BTC", "1", "2023-01-01 10:00:00");
        assertTrue(calcola().isEmpty());

        GruppoOperazione.Scrivi(a, "DUAL-9");

        assertEquals("DUAL-9", calcola().get(a[0]), "un Purchase di un contratto annullato resta riconoscibile");
    }

    @Test
    void dettaglio_elencaGliAltriMovimentiConQuelloCheSono() {
        String[] p = movimento("20230101095000_Kraken_001_001_PC", "Kraken", "BTC", "0.01", "2023-01-01 09:50:00");
        String[] d = movimento("20230101100000_Binance_001_001_DC", "Binance", "BTC", "0.01", "2023-01-01 10:00:00");
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);

        List<String[]> righe = OperazioniCalcolate.RigheDettaglio(p[0]);

        assertEquals("Operazione (2 movimenti)", righe.get(0)[0]);
        assertTrue(righe.get(0)[1].contains(d[0]));
        assertTrue(righe.get(0)[1].contains(d[18]), "dice cosa è l'altro movimento");
        assertFalse(righe.get(0)[1].contains(p[0] + ")"), "il movimento non elenca se stesso");
    }
}
