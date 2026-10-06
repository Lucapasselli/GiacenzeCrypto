package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Files;
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
 * Fissa {@link MovimentiCollegati}: il quinto contatore del pulsante Errori e la ricostruzione degli scambi
 * differiti sovrascritti (bug C17) che ne parte. Un archivio classificato normalmente non deve dare nessuna
 * segnalazione, ogni caso anomalo deve finire nella sua categoria.
 */
class MovimentiCollegatiTest {

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

    private static final String DATA_DEPOSITO = "2022-08-26 10:40:36";

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
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

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

    private static MovimentiCollegati.Esito controlla() {
        return MovimentiCollegati.Controlla(new ArrayList<>(MappaCryptoWallet.values()));
    }

    private static List<String[]> gruppoDi(String[] v) {
        List<String[]> g = new ArrayList<>();
        g.add(v);
        for (String ID : v[20].split(",")) g.add(MappaCryptoWallet.get(ID));
        return g;
    }

    /** Due scambi differiti col deposito nello stesso secondo, il secondo nello stato lasciato dal bug C17. */
    private static String[][] collisione() throws Exception {
        String[] pA = movimento("20220408141952_Binance_001_001_PC", "", "BUSD", "100", "2022-04-08 14:19:52", "100.00");
        String[] pB = movimento("20220417122647_Binance_001_001_PC", "", "BUSD", "38.4", "2022-04-17 12:26:47", "38.40");
        String[] dA = movimento("20220826104036_Binance_001_001_DC", "", "BTC", "0.005", DATA_DEPOSITO, "100.00");
        String[] dB = movimento("20220826104036_Binance_001_002_DC", "", "BTC", "0.002", DATA_DEPOSITO, "40.00");
        seedPrezzo("BTC", DATA_DEPOSITO);
        seedPrezzo("BUSD", DATA_DEPOSITO);
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pA[0], dA[0]));
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(pB[0], dB[0]));
        String[] idA = pA[20].split(","), idB = pB[20].split(",");
        MappaCryptoWallet.remove(idB[1]);
        MappaCryptoWallet.remove(idB[2]);
        for (String[] v : new String[][]{pB, MappaCryptoWallet.get(idB[0]), dB}) {
            v[20] = v[20].replace(idB[1], idA[1]).replace(idB[2], idA[2]);
        }
        return new String[][]{pA, pB, dB};
    }

    @Test
    void archivioClassificatoNormalmente_nessunaSegnalazione() throws Exception {
        //Trasferimento con commissione di rete (prelievo maggiore del deposito) e scambio differito
        String[] p = movimento("20230101100000_Kraken_001_001_PC", "", "BTC", "0.0105", "2023-01-01 10:00:00", "105.00");
        String[] d = movimento("20230101101000_Binance_001_001_DC", "", "BTC", "0.01", "2023-01-01 10:10:00", "100.00");
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);
        assertEquals(3, MappaCryptoWallet.size(), "prelievo, deposito e commissione");
        String[] p2 = movimento("20230201100000_Binance_002_001_PC", "", "USDT", "100", "2023-02-01 10:00:00", "100.00");
        String[] d2 = movimento("20230202100000_Binance_003_001_DC", "", "ETH", "0.05", "2023-02-02 10:00:00", "100.00");
        seedPrezzo("ETH", "2023-02-02 10:00:00");
        seedPrezzo("USDT", "2023-02-02 10:00:00");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p2[0], d2[0]));

        assertEquals(0, controlla().Totale());
    }

    @Test
    void scambioSovrascritto_vieneRiconosciuto_eRicostruitoSenzaToccareLAltro() throws Exception {
        String[][] m = collisione();
        List<String> scambioA = new ArrayList<>();
        for (String[] v : gruppoDi(m[0])) scambioA.add(String.join("|", v));

        MovimentiCollegati.Esito E = controlla();
        assertEquals(List.of(m[1][0]), new ArrayList<>(E.ScambiSovrascritti));
        assertTrue(E.Altri.isEmpty(), "le righe dello scambio rotto non vanno contate una seconda volta: " + E.Altri);
        assertTrue(E.DualFormaPrecedente.isEmpty());

        List<MovimentiCollegati.Riparazione> R = MovimentiCollegati.RiparaScambiSovrascritti(E.ScambiSovrascritti);

        assertEquals(1, R.size());
        assertEquals(MovimentiCollegati.EsitoRiparazione.RIPARATO, R.get(0).Esito);
        assertEquals(10, MappaCryptoWallet.size());
        assertEquals(0, controlla().Totale());
        List<String> scambioADopo = new ArrayList<>();
        for (String[] v : gruppoDi(m[0])) scambioADopo.add(String.join("|", v));
        assertEquals(scambioA, scambioADopo, "l'altro scambio non si tocca");
        for (String[] v : gruppoDi(m[1])) {
            if ("AU".equals(v[22])) assertEquals("Piattaforma di scambio", v[4], "stesso sotto-wallet di prima");
        }
    }

    @Test
    void scambioSovrascrittoDiUnContrattoDual_conservaSottoWalletEGruppo() throws Exception {
        String[] pA = movimento("20220408141952_Binance_001_001_PC", "Dual Savings Purchase", "BUSD", "100.00000000", "2022-04-08 14:19:52", "100.00");
        String[] pB = movimento("20220417122647_Binance_001_001_PC", "Dual Savings Purchase", "BUSD", "38.40000000", "2022-04-17 12:26:47", "38.40");
        movimento("20220826104036_Binance_001_001_DC", "Dual Savings Settlement", "BTC", "0.00500000", DATA_DEPOSITO, "100.00");
        String[] sB = movimento("20220826104036_Binance_001_002_DC", "Dual Savings Settlement", "BTC", "0.00200000", DATA_DEPOSITO, "40.00");
        seedPrezzo("BTC", DATA_DEPOSITO);
        seedPrezzo("BUSD", DATA_DEPOSITO);
        StringBuilder f = new StringBuilder("Product,Order Type,Product Id,Subscription Date,Type,Subscription Amount,"
                + "Target Price,Settlement Date,Fixing Price,APY,Settlement Amount,Status\n");
        f.append("BUSD/BTC,Buy Low,1005680,2022-04-08 16:19:52,Settled,100.00000000 BUSD,28500,2022-08-26 12:40:36,20000,10%,0.00500000 BTC,Settled\n");
        f.append("BUSD/BTC,Buy Low,1055026,2022-04-17 14:26:47,Settled,38.40000000 BUSD,28500,2022-08-26 12:40:36,20000,10%,0.00200000 BTC,Settled\n");
        Path file = tempDir.resolve("dettaglio_" + System.nanoTime() + "_202609182038UTC+2.csv");
        Files.writeString(file, f.toString());
        assertEquals(2, Binance_DualInvestment.Abbina(file.toFile()).abbinati);
        String[] idA = pA[20].split(","), idB = pB[20].split(",");
        MappaCryptoWallet.remove(idB[1]);
        MappaCryptoWallet.remove(idB[2]);
        for (String[] v : new String[][]{pB, MappaCryptoWallet.get(idB[0]), sB}) {
            v[20] = v[20].replace(idB[1], idA[1]).replace(idB[2], idA[2]);
        }

        MovimentiCollegati.Esito E = controlla();
        assertEquals(1, E.ScambiSovrascritti.size());
        MovimentiCollegati.RiparaScambiSovrascritti(E.ScambiSovrascritti);

        assertEquals(0, controlla().Totale());
        for (String[] v : gruppoDi(pB)) {
            assertEquals("DUAL-1055026", GruppoOperazione.ChiaveEffettiva(v), v[0]);
            if ("AU".equals(v[22])) assertEquals("Dual Savings", v[4]);
        }
        for (String[] v : gruppoDi(pA)) assertEquals("DUAL-1005680", GruppoOperazione.ChiaveEffettiva(v), v[0]);
    }

    @Test
    void contrattoDualNellaFormaPrecedente_vaFraQuelliDaAggiornare_nonFraGliErrori() {
        String[] p = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase", "USDT", "100", "2022-05-12 06:24:50", "100.00");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement", "USDT", "100", "2022-05-24 08:33:41", "100.00");
        String[] mirrorP = movimento("20220512062450_Binance_001A_001_DC", "", "USDT", "100", p[1], "100.00");
        mirrorP[4] = "Dual Savings"; mirrorP[18] = "DTW - Trasferimento Interno"; mirrorP[22] = "AU";
        String[] mirrorS = movimento("20220524083341_Binance_0002_001_PC", "", "USDT", "100", s[1], "100.00");
        mirrorS[4] = "Dual Savings"; mirrorS[18] = "PTW - Trasferimento Interno"; mirrorS[22] = "AU";
        String[] reward = movimento("20220524083341_Binance_00002_001_DC", "", "USDT", "2", s[1], "2.00");
        reward[18] = Binance_DualInvestment.CAMPO18_REWARD; reward[22] = "AU"; reward[20] = mirrorS[0];
        p[18] = Binance_DualInvestment.CAMPO18_PURCHASE_STESSA_MONETA; p[20] = mirrorP[0];
        s[18] = Binance_DualInvestment.CAMPO18_SETTLEMENT_STESSA_MONETA; s[20] = mirrorS[0] + "," + reward[0];

        MovimentiCollegati.Esito E = controlla();

        assertEquals(List.of(s[0]), new ArrayList<>(E.DualFormaPrecedente));
        assertTrue(E.Altri.isEmpty(), E.Altri.toString());
        assertTrue(E.ScambiSovrascritti.isEmpty());

        Binance_DualInvestment.AggiornaFormaStessaMoneta(p, s);
        assertEquals(0, controlla().Totale(), "dopo la conversione non resta nulla");
    }

    @Test
    void riferimentoAUnMovimentoCancellato_vaFraGliAltri() {
        String[] p = movimento("20230101100000_Kraken_001_001_PC", "", "BTC", "0.01", "2023-01-01 10:00:00", "100.00");
        String[] d = movimento("20230101101000_Binance_001_001_DC", "", "BTC", "0.01", "2023-01-01 10:10:00", "100.00");
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);
        //Tolto senza passare dall'annullamento della classificazione
        MappaCryptoWallet.remove(d[0]);

        MovimentiCollegati.Esito E = controlla();

        assertEquals(List.of(p[0]), new ArrayList<>(E.Altri));
        assertEquals(1, E.Totale());
    }

    // =============================================================================================
    // CHIAVE DEL CONTRATTO DUAL SUI MOVIMENTI GENERATI
    // =============================================================================================

    private static void tuttiConLaChiave(String[] v, String Chiave) {
        for (String[] m : gruppoDi(v)) assertEquals(Chiave, GruppoOperazione.ChiaveEffettiva(m), m[0]);
    }

    @Test
    void contrattoAnnullatoERiassociatoAMano_laChiaveVaSuTuttiECinque() throws Exception {
        //Lo scenario segnalato: abbinamento dal file, cancellazione di un generato (annulla il contratto),
        //poi prelievo e deposito riassociati a mano come scambio differito
        String[] p = movimento("20220512062450_Binance_001_001_PC", "Dual Savings Purchase", "USDT", "100.00000000", "2022-05-12 06:24:50", "100.00");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "Dual Savings Settlement", "BTC", "0.00350000", "2022-05-24 08:33:41", "100.00");
        seedPrezzo("BTC", "2022-05-24 08:33:41");
        seedPrezzo("USDT", "2022-05-24 08:33:41");
        Path file = tempDir.resolve("dettaglio_" + System.nanoTime() + "_202609182038UTC+2.csv");
        Files.writeString(file, "Product,Order Type,Product Id,Subscription Date,Type,Subscription Amount,Target Price,"
                + "Settlement Date,Fixing Price,APY,Settlement Amount,Status\n"
                + "USDT/BTC,Buy Low,1232611,2022-05-12 08:24:50,Settled,100.00000000 USDT,28500,2022-05-24 10:33:41,29351.32,"
                + "146.28%,0.00350000 BTC,Settled\n");
        assertEquals(1, Binance_DualInvestment.Abbina(file.toFile()).abbinati);
        String generato = null;
        for (String[] v : MappaCryptoWallet.values()) if ("AU".equals(v[22])) generato = v[0];
        Funzioni.RimuoviMovimentazioneXID(generato);
        assertEquals(2, MappaCryptoWallet.size());

        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));

        assertEquals(5, MappaCryptoWallet.size());
        tuttiConLaChiave(p, "DUAL-1232611");
        assertEquals(0, controlla().Totale());
    }

    @Test
    void iGenerati_nonPortanoLaChiave_laRicavanoDalGruppo() throws Exception {
        String[] p = movimento("20220512062450_Binance_001_001_PC", "", "USDT", "100", "2022-05-12 06:24:50", "100.00");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "", "BTC", "0.0035", "2022-05-24 08:33:41", "100.00");
        seedPrezzo("BTC", "2022-05-24 08:33:41");
        seedPrezzo("USDT", "2022-05-24 08:33:41");
        GruppoOperazione.Scrivi(p, "DUAL-1232611");
        GruppoOperazione.Scrivi(s, "DUAL-1232611");

        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));

        for (String[] m : gruppoDi(p)) {
            if ("AU".equals(m[22])) assertEquals("", GruppoOperazione.Chiave(m), "la chiave scritta sta solo sugli originali: " + m[0]);
            else assertEquals("DUAL-1232611", GruppoOperazione.Chiave(m));
        }
        tuttiConLaChiave(p, "DUAL-1232611");
        assertEquals(5, GruppoOperazione.Membri("DUAL-1232611").size(), "i membri comprendono i generati");
        assertEquals(0, controlla().Totale());
    }

    @Test
    void trasferimentoFraMovimentiDelloStessoContratto_laCommissioneGenerataPrendeLaChiave() {
        String[] p = movimento("20230101100000_Binance_001_001_PC", "", "USDT", "100.5", "2023-01-01 10:00:00", "100.50");
        String[] d = movimento("20230101101000_Binance_002_001_DC", "", "USDT", "100", "2023-01-01 10:10:00", "100.00");
        p[GruppoOperazione.CAMPO] = "DUAL-7";
        d[GruppoOperazione.CAMPO] = "DUAL-7";

        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);

        assertEquals(3, MappaCryptoWallet.size(), "prelievo, deposito e commissione");
        tuttiConLaChiave(p, "DUAL-7");
    }

    @Test
    void chiaviDiverseOCommissioni_laRegolaNonScatta() throws Exception {
        seedPrezzo("BTC", "2022-05-24 08:33:41");
        seedPrezzo("USDT", "2022-05-24 08:33:41");
        //Prelievo e deposito di due contratti diversi: nessuna chiave sui trasferimenti, nessuna segnalazione
        String[] p = movimento("20220512062450_Binance_001_001_PC", "", "USDT", "100", "2022-05-12 06:24:50", "100.00");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "", "BTC", "0.0035", "2022-05-24 08:33:41", "100.00");
        p[GruppoOperazione.CAMPO] = "DUAL-1";
        s[GruppoOperazione.CAMPO] = "DUAL-2";
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));
        for (String[] m : gruppoDi(p)) {
            if ("AU".equals(m[22]) && !m[0].endsWith("_SC")) assertEquals("", GruppoOperazione.ChiaveEffettiva(m), m[0]);
        }
        assertEquals(0, controlla().Totale());

        //Chiave di commissioni sul prelievo: lo scambio la eredita come prima, i trasferimenti no
        MappaCryptoWallet.clear();
        String[] p2 = movimento("20220512062450_Binance_001_001_PC", "", "USDT", "100", "2022-05-12 06:24:50", "100.00");
        String[] s2 = movimento("20220524083341_Binance_002_001_DC", "", "BTC", "0.0035", "2022-05-24 08:33:41", "100.00");
        p2[CommissioniCollegate.CAMPO] = "chiave-commissione";
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p2[0], s2[0]));
        for (String[] m : gruppoDi(p2)) {
            if (!"AU".equals(m[22])) continue;
            assertEquals(m[0].endsWith("_SC") ? "chiave-commissione" : "", CommissioniCollegate.Chiave(m), m[0]);
        }
        assertEquals(0, controlla().Totale());
    }
}
