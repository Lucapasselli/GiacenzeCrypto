package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Fissa il documento di origine ({@code [41]}) dei movimenti generati da una classificazione: lo copiano dal movimento
 * da cui nascono, come già facevano lo scambio differito e il trasferimento fra wallet. Prima i Dual Investment a
 * moneta uguale e i trasferimenti verso piattaforma ne restavano senza, e il filtro per documento li lasciava fuori.
 * Il pregresso si completa al caricamento ({@link DocumentiFonte#CompletaDocumentoGenerati}).
 */
class DocumentoOrigineGeneratiTest {

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
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID, String causale, String moneta, String qta, String data, String documento) {
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
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        v[41] = documento;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    @Test
    void dualAMonetaUguale_ogniGeneratoPrendeIlDocumentoDelMovimentoDaCuiNasce() throws Exception {
        //Purchase e Settlement da due file diversi
        String[] p = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase", "USDT", "100.00000000", "2022-05-12 06:24:50", "3");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement", "USDT", "102.00000000", "2022-05-24 08:33:41", "5");
        Path f = tempDir.resolve("dettaglio_" + System.nanoTime() + "_202609182038UTC+2.csv");
        Files.writeString(f, "Product,Order Type,Product Id,Subscription Date,Type,Subscription Amount,Target Price,"
                + "Settlement Date,Fixing Price,APY,Settlement Amount,Status\n"
                + "USDT/USDT,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,2022-05-24 10:33:41,"
                + "29351.32,146.28%,102.00000000 USDT,Settled\n");

        assertEquals(1, Binance_DualInvestment.Abbina(f.toFile()).abbinati);

        for (String[] v : MappaCryptoWallet.values()) {
            String atteso = v[0].startsWith("20220512") ? "3" : "5";
            assertEquals(atteso, v[41], v[0] + " " + v[5]);
        }
        assertEquals(5, MappaCryptoWallet.size());
    }

    @Test
    void trasferimentoAPiattaforma_laGambaSpecularePrendeIlDocumento() {
        String[] p = movimento("20230101100000_Binance_001_001_PC", "", "USDT", "50", "2023-01-01 10:00:00", "7");

        GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA(p[0], "TRASFERIMENTO A PIATTAFORMA",
                "PTW - Trasferimento a Vault/Piattaforma a Rendita", "Piattaforma/DeFi", "TRASFERIMENTO A PIATTAFORMA");

        assertEquals(2, MappaCryptoWallet.size());
        for (String[] v : MappaCryptoWallet.values()) assertEquals("7", v[41], v[0]);
    }

    @Test
    void pregresso_ilGeneratoPrendeIlDocumentoDellOriginaleConLoStessoIstante() {
        Map<String, String[]> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String[] p = movimento("20220512062450_Binance_001_001_PC", "", "USDT", "100", "2022-05-12 06:24:50", "3");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "", "USDT", "102", "2022-05-24 08:33:41", "5");
        String[] gp = movimento("20220512062450_Binance_001A_001_DC", "", "USDT", "100", "2022-05-12 06:24:50", "");
        String[] gs = movimento("20220524083341_Binance_0002_001_PC", "", "USDT", "102", "2022-05-24 08:33:41", "");
        for (String[] g : new String[][]{gp, gs}) g[22] = "AU";
        String gruppo = p[0] + "," + s[0] + "," + gp[0] + "," + gs[0];
        for (String[] v : new String[][]{p, s, gp, gs}) {
            v[20] = gruppo.replace(v[0] + ",", "").replace("," + v[0], "");
            m.put(v[0], v);
        }

        assertEquals(2, DocumentiFonte.CompletaDocumentoGenerati(m));

        assertEquals("3", gp[41], "nasce dal Purchase");
        assertEquals("5", gs[41], "nasce dal Settlement");
        assertEquals(0, DocumentiFonte.CompletaDocumentoGenerati(m), "una seconda volta non c'è nulla da fare");
    }

    @Test
    void pregresso_senzaOriginaleNelloStessoIstante_soloSeIlDocumentoECondiviso() {
        Map<String, String[]> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String[] p = movimento("20220512062450_Binance_001_001_PC", "", "USDT", "100", "2022-05-12 06:24:50", "3");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "", "BTC", "0.0035", "2022-05-24 08:33:41", "3");
        String[] g = movimento("20220520000000_Binance_09_001_SC", "", "USDT", "100", "2022-05-20 00:00:00", "");
        g[22] = "AU";
        p[20] = s[0] + "," + g[0];
        s[20] = p[0] + "," + g[0];
        g[20] = p[0] + "," + s[0];
        for (String[] v : new String[][]{p, s, g}) m.put(v[0], v);

        assertEquals(1, DocumentiFonte.CompletaDocumentoGenerati(m));
        assertEquals("3", g[41]);

        //Originali da documenti diversi e nessuno nello stesso istante: non si indovina
        g[41] = "";
        s[41] = "5";
        assertEquals(0, DocumentiFonte.CompletaDocumentoGenerati(m));
        assertEquals("", g[41]);
    }
}
