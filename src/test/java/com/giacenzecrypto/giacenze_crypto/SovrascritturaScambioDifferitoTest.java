package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa il reimport con «sovrascrivi esistenti» di un file che contiene gli estremi di uno scambio differito.
 * Classificando lo scambio prelievo e deposito prendono il prefisso {@code 00}/{@code 04} nell'ID: la sovrascrittura
 * non li trovava con l'ID del file e li aggiungeva una seconda volta. Ora li ritrova nella forma rinominata, annulla
 * lo scambio come ogni altra classificazione e mette la riga del file al loro posto, e il resoconto conta i movimenti
 * tornati da classificare. Senza «sovrascrivi» la forma rinominata non si cerca: una riga nuova resta nuova.
 */
class SovrascritturaScambioDifferitoTest {

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

    private static final String ID_P = "20220512062450_Binance_001_001_PC";
    private static final String ID_S = "20220524083341_Binance_002_001_DC";

    @BeforeEach
    void preparaScambio() throws Exception {
        MappaCryptoWallet.clear();
        Importazioni.AzzeraContatori();
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
        MappaCryptoWallet.put(ID_P, riga(ID_P, "USDT", "100", "2022-05-12 06:24:50"));
        MappaCryptoWallet.put(ID_S, riga(ID_S, "BTC", "0.0035", "2022-05-24 08:33:41"));
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(ID_P, ID_S));
        assertEquals(5, MappaCryptoWallet.size());
        assertNull(MappaCryptoWallet.get(ID_P), "il prelievo è stato rinominato");
    }

    /** Una riga come la produce l'import del file, non classificata. */
    private static String[] riga(String ID, String moneta, String qta, String data) {
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

    private static int conMoneta(String Moneta) {
        int n = 0;
        for (String[] v : MappaCryptoWallet.values()) {
            if ("A".equals(v[22]) && (Moneta.equals(v[8]) || Moneta.equals(v[11]))) n++;
        }
        return n;
    }

    @Test
    void sovrascrivendoIlPrelievo_loScambioSiAnnullaENienteRaddoppia() {
        String[] nuova = riga(ID_P, "USDT", "100", "2022-05-12 06:24:50");

        Importazioni.InserisciMovimentosuMappaCryptoWallet(ID_P, nuova, true);

        assertEquals(2, MappaCryptoWallet.size(), "prelievo e deposito, senza i tre generati: " + MappaCryptoWallet.keySet());
        assertSame(nuova, MappaCryptoWallet.get(ID_P), "la riga del file prende il posto del prelievo");
        assertNotNull(MappaCryptoWallet.get(ID_S), "il deposito torna al suo ID, non classificato");
        assertEquals("", MappaCryptoWallet.get(ID_S)[18]);
        assertEquals(2, Importazioni.ClassificazioniAnnullate, "prelievo e deposito tornano entrambi da classificare");
    }

    @Test
    void sovrascrivendoIlDeposito_loScambioSiAnnullaENienteRaddoppia() {
        String[] nuova = riga(ID_S, "BTC", "0.0035", "2022-05-24 08:33:41");

        Importazioni.InserisciMovimentosuMappaCryptoWallet(ID_S, nuova, true);

        assertEquals(2, MappaCryptoWallet.size(), MappaCryptoWallet.keySet().toString());
        assertSame(nuova, MappaCryptoWallet.get(ID_S));
        assertNotNull(MappaCryptoWallet.get(ID_P));
        assertEquals(2, Importazioni.ClassificazioniAnnullate);
    }

    @Test
    void reimportDiEntrambiConSovrascrivi_unPrelievoEUnDeposito() {
        List<String[]> file = new ArrayList<>();
        file.add(riga(ID_P, "USDT", "100", "2022-05-12 06:24:50"));
        file.add(riga(ID_S, "BTC", "0.0035", "2022-05-24 08:33:41"));

        Importazioni.ScriviListaSuMappaCrypto(file, true);

        assertEquals(1, conMoneta("USDT"), "un solo prelievo");
        assertEquals(1, conMoneta("BTC"), "un solo deposito");
        assertEquals(2, MappaCryptoWallet.size());
        assertEquals(2, Importazioni.ClassificazioniAnnullate, "contati una volta sola, non di nuovo al secondo");
        assertTrue(Importazioni.TestoClassificazioniAnnullate().contains("2"));
    }

    @Test
    void senzaSovrascrivi_unaRigaNuovaConLoStessoIDNonToccaLoScambio() {
        //Un movimento diverso che ha per caso lo stesso ID di partenza del deposito, ormai rinominato
        String[] nuova = riga(ID_S, "ETH", "1", "2022-05-24 08:33:41");

        Importazioni.InserisciMovimentosuMappaCryptoWallet(ID_S, nuova);

        assertEquals(6, MappaCryptoWallet.size(), "lo scambio resta com'era e la riga si aggiunge");
        assertSame(nuova, MappaCryptoWallet.get(ID_S));
        assertEquals(0, Importazioni.ClassificazioniAnnullate);
    }

    @Test
    void sovrascrivereUnMovimentoNonClassificato_nonSiConta() {
        MappaCryptoWallet.clear();
        String ID = "20230101100000_Binance_001_001_PC";
        MappaCryptoWallet.put(ID, riga(ID, "BTC", "0.01", "2023-01-01 10:00:00"));

        Importazioni.InserisciMovimentosuMappaCryptoWallet(ID, riga(ID, "BTC", "0.01", "2023-01-01 10:00:00"), true);

        assertEquals(1, MappaCryptoWallet.size());
        assertEquals(0, Importazioni.ClassificazioniAnnullate);
        assertEquals("", Importazioni.TestoClassificazioniAnnullate());
    }

    @Test
    void ilConteggioSiSommaNelResocontoDiPiuImportazioni() {
        Importazioni.ClassificazioniAnnullate = 2;
        Importazioni.Esito a = Importazioni.Esito.daiContatori("a.csv");
        Importazioni.ClassificazioniAnnullate = 3;
        Importazioni.Esito b = Importazioni.Esito.daiContatori("b.csv");
        Importazioni.Esito somma = new Importazioni.Esito();

        somma.Somma(a);
        somma.Somma(b);

        assertEquals(5, somma.ClassificazioniAnnullate);
    }
}
