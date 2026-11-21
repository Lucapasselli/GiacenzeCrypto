package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * I futures nel CSV Binance ({@code config/import/Binance CSV.json} dalla 1.021): profitto realizzato, funding, commissioni,
 * rimborso commissioni da referral e giroconti dei conti {@code USD-M Futures}/{@code COIN-M Futures}. Fino alla 1.020 erano
 * causali sconosciute, tranne il {@code Fee}, che entrava come commissione spot. Il {@code Fee} del conto futures ha la stessa
 * causale di quello spot: lo distinguono le regole di riga dal conto (colonna 2). I movimenti nuovi nascono marcati e
 * classificati come derivati. Righe inventate, nessun dato reale.
 *
 * <p>Layout: {@code User_ID,UTC_Time,Account,Operation,Coin,Change,Remark}, colonne 7 e 8 vuote (le usano le regole) e
 * [9] = controvalore sintetico (evita la ricerca dei prezzi in rete).</p>
 */
class ImportazioneGenericaBinanceFuturesTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Principale.MappaCryptoWallet.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuota() {
        Principale.MappaCryptoWallet.clear();
    }

    private static String riga(String time, String account, String operation, String coin, String change) {
        return "1," + time + "," + account + "," + operation + "," + coin + "," + change + ",,,,1.00";
    }

    /** Lettura del file (regole di riga comprese), somma per giorno, consolidamento e scrittura in archivio, come l'import vero. */
    private static List<String[]> importa(String... righe) throws Exception {
        Path f = Files.createTempFile(tempDir, "binance", ".csv");
        List<String> testo = new ArrayList<>();
        testo.add("User_ID,UTC_Time,Account,Operation,Coin,Change,Remark,Appoggio1,Appoggio2,Valore");
        testo.addAll(List.of(righe));
        Files.write(f, testo, StandardCharsets.UTF_8);
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 9;
        Importazioni.AzzeraContatori();
        Importazioni.AzzeraSconosciuti();
        List<String[]> lette = ImportazioneGenerica.leggiCSV(f.toString(), c);
        lette = ImportazioneGenerica.scartaAnnullamenti(lette, c);
        lette = ImportazioneGenerica.consolidaCausaliPerGiorno(lette, c);
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(lette, c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        assertEquals(0, Importazioni.TrasazioniSconosciute, () -> "righe scartate: " + Importazioni.Sconosciuti());
        Importazioni.ScriviListaSuMappaCrypto(movs, false, 0);
        return new ArrayList<>(Principale.MappaCryptoWallet.values());
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    private static String[] unico(List<String[]> movs, String causale) {
        List<String[]> t = movs.stream().filter(m -> causale.equals(m[7])).toList();
        assertEquals(1, t.size(), () -> "movimenti con causale " + causale + ": " + movs.stream().map(m -> m[0] + " " + m[7]).toList());
        return t.get(0);
    }

    @Test
    void ilFeeSpotRestaUnaCommissioneNormale() throws Exception {
        List<String[]> movs = importa(riga("2025-03-01 10:00:00", "Spot", "Fee", "BNB", "-0.001"));
        String[] m = unico(movs, "Fee");
        assertEquals("CM", categoria(m));
        assertFalse(Derivati.isDerivato(m));
        assertEquals("", m[18]);
    }

    @Test
    void ilFeeDeiFuturesEUnaCommissioneDeiDerivatiGiaClassificata() throws Exception {
        List<String[]> movs = importa(
                riga("2025-03-01 10:00:00", "USD-M Futures", "Fee", "USDT", "-0.05"),
                riga("2025-03-02 10:00:00", "COIN-M Futures", "Fee", "BTC", "-0.000001"));
        List<String[]> fee = movs.stream().filter(m -> "Futures Fee".equals(m[7])).toList();
        assertEquals(2, fee.size());
        for (String[] m : fee) {
            assertEquals("CM", categoria(m));
            assertEquals(Derivati.COMMISSIONE, Derivati.Tipo(m));
            assertEquals(Derivati.CAMPO18_ADDEBITO, m[18]);
        }
    }

    @Test
    void ilProfittoRealizzatoNeiDueVersiEClassificato() throws Exception {
        List<String[]> movs = importa(
                riga("2025-03-01 11:00:00", "USD-M Futures", "Realized Profit and Loss", "USDT", "3.5"),
                riga("2025-03-02 11:00:00", "USD-M Futures", "Realized Profit and Loss", "USDT", "-1.2"));
        List<String[]> pnl = movs.stream().filter(m -> "Realized Profit and Loss".equals(m[7])).toList();
        assertEquals(2, pnl.size());
        String[] dc = pnl.stream().filter(m -> categoria(m).equals("DC")).findFirst().orElseThrow();
        String[] pc = pnl.stream().filter(m -> categoria(m).equals("PC")).findFirst().orElseThrow();
        assertEquals(Derivati.PNL, Derivati.Tipo(dc));
        assertEquals(Derivati.CAMPO18_ACCREDITO, dc[18]);
        assertEquals(Derivati.CAMPO18_ADDEBITO, pc[18]);
    }

    @Test
    void ilFundingESommatoPerGiorno() throws Exception {
        List<String[]> movs = importa(
                riga("2025-03-01 00:00:00", "USD-M Futures", "Funding Fee", "USDT", "-0.01"),
                riga("2025-03-01 08:00:00", "USD-M Futures", "Funding Fee", "USDT", "-0.02"),
                riga("2025-03-01 16:00:00", "USD-M Futures", "Funding Fee", "USDT", "0.005"));
        String[] m = unico(movs, "Funding Fee");
        assertEquals("PC", categoria(m));
        assertEquals(0, new java.math.BigDecimal("-0.025").compareTo(new java.math.BigDecimal(m[10])));
        assertEquals(Derivati.FUNDING, Derivati.Tipo(m));
        assertEquals(Derivati.CAMPO18_ADDEBITO, m[18]);
    }

    @Test
    void rimborsoCommissioniEGirocontiDeiFutures() throws Exception {
        List<String[]> movs = importa(
                riga("2025-03-01 12:00:00", "USD-M Futures", "Referee Commission", "USDT", "0.01"),
                riga("2025-03-01 13:00:00", "Spot", "Transfer Between Spot and UM Futures", "USDT", "-50"),
                riga("2025-03-01 13:00:00", "USD-M Futures", "Transfer Between Spot and UM Futures", "USDT", "50"));
        String[] r = unico(movs, "Referee Commission");
        assertEquals(Derivati.RIMBORSO_COMMISSIONI, Derivati.Tipo(r));
        assertEquals(Derivati.CAMPO18_ACCREDITO, r[18]);
        assertEquals(OKXDerivati.SOTTOWALLET, r[4], "dalla 1.0.67 il conto futures e' il sotto-wallet Derivati");
        //Il giroconto: due righe con la stessa causale, distinte dal conto. Dalla 1.0.67 sono due gambe di trasferimento interno
        String[] spot = unico(movs, "Transfer Between Spot and UM Futures");
        String[] futures = unico(movs, "Transfer Between Spot and UM Futures (conto futures)");
        assertEquals("TI", categoria(spot));
        assertEquals("TI", categoria(futures));
        assertEquals("Principale", spot[4]);
        assertEquals(OKXDerivati.SOTTOWALLET, futures[4]);
        assertEquals(0, new java.math.BigDecimal("-50").compareTo(new java.math.BigDecimal(spot[10])));
        assertEquals(0, new java.math.BigDecimal("50").compareTo(new java.math.BigDecimal(futures[13])));
        assertEquals(Derivati.MARGINE, Derivati.Tipo(spot));
        assertEquals(Derivati.MARGINE, Derivati.Tipo(futures));
        assertEquals("", spot[18], "un giroconto non si classifica");
        assertEquals(3, movs.size());
    }

    @Test
    void tuttoIlContoFuturesStaSulSottoWalletDerivati_loSpotNo() throws Exception {
        List<String[]> movs = importa(
                riga("2025-03-01 10:00:00", "USD-M Futures", "Fee", "USDT", "-0.05"),
                riga("2025-03-01 11:00:00", "USD-M Futures", "Realized Profit and Loss", "USDT", "3.5"),
                riga("2025-03-01 12:00:00", "USD-M Futures", "Funding Fee", "USDT", "-0.01"),
                riga("2025-03-01 14:00:00", "Spot", "Fee", "BNB", "-0.001"),
                riga("2025-03-02 13:00:00", "USD-M Futures", "Transfer Between Spot and UM Futures", "USDT", "-20"),
                riga("2025-03-02 13:00:00", "Spot", "Transfer Between Spot and UM Futures", "USDT", "20"));
        for (String[] m : movs) {
            boolean conto = !"Fee".equals(m[7]) && !"Transfer Between Spot and UM Futures".equals(m[7]);
            assertEquals(conto ? OKXDerivati.SOTTOWALLET : "Principale", m[4], m[7]);
        }
        String[] esce = unico(movs, "Transfer Between Spot and UM Futures (conto futures)");
        assertEquals(0, new java.math.BigDecimal("-20").compareTo(new java.math.BigDecimal(esce[10])), "il rientro esce dal conto");
    }
}
