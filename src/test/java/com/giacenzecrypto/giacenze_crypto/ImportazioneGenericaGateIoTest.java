package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caratterizza le tre config {@code config/import/Gate.io *.json} e, con loro, le tre
 * funzionalità generiche aggiunte a {@link ImportazioneGenerica} per supportarle (nessuna delle
 * config esistenti le usa, quindi il comportamento di default resta invariato):
 * <ul>
 *   <li>{@code separatoreValoreMoneta}: una cella "NUMERO SIMBOLO" (Gate.io Spot Trade History
 *       impacchetta importo e moneta nella stessa colonna, es. "407.57 AME") si legge quando
 *       {@code moneta}/{@code quantita} (o {@code monetaUscita}/{@code quantitaUscita}, o
 *       {@code monetaFee}/{@code quantitaFee}) puntano alla STESSA colonna;</li>
 *   <li>{@code causaliScambiaGambe}: su una riga con entrambe le gambe già presenti, per le
 *       causali elencate scambia le celle grezze di quantita'/quantitaUscita (e moneta/
 *       monetaUscita) prima di ogni lettura — serve perché "Deal amount"/"Total" di Gate.io sono
 *       sempre positivi così come esportati, e la direzione reale (chi entra, chi esce) dipende
 *       dalla causale Buy/Sell della riga, non dal segno della cella;</li>
 *   <li>{@code versoForzato}: forza il segno su OGNI riga del file indipendentemente dalla
 *       causale — serve per gli export a file separato per direzione (depositi/prelievi Gate.io),
 *       dove non esiste alcuna colonna causale da cui dedurre il verso riga per riga.</li>
 * </ul>
 *
 * <p>Le config caricano dati sintetici (non i CSV reali dell'utente, che non vanno committati):
 * righe scritte a mano nella stessa forma grezza degli export originali di Gate.io, incluso il TAB
 * che Gate.io lascia in coda alle celle 'No' e 'Deal price' del trade history. I test
 * {@code fileReale_*} scrivono quelle righe su disco con encoding, BOM e fine riga degli export
 * veri e le leggono con {@link ImportazioneGenerica#leggiCSV}, perche' gli errori di formato stanno
 * proprio li'. La 1.000 delle config era stata scritta su un file passato da un editor che aveva
 * sostituito ogni spazio con ';', e non leggeva gli export originali.</p>
 *
 * <p>Verificato anche contro gli export originali dell'utente (non committati, 2026-09-30): 35
 * trade -> 70 movimenti (35 scambi + 35 commissioni), 6 prelievi -> 12 movimenti, 2 depositi -> 2
 * movimenti, un file depositi con la sola intestazione -> 0 righe, nessuna riga scartata.</p>
 */
class ImportazioneGenericaGateIoTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static ConfigurazioneImport cfgTrades() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Gate.io Spot Trades.json");
        c.colonnaValoreEuro = 9; // evita la ricerca prezzi in rete, non pertinente a questo test
        return c;
    }

    private static ConfigurazioneImport cfgWithdrawals() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Gate.io Withdrawals.json");
        c.colonnaValoreEuro = 11;
        return c;
    }

    private static ConfigurazioneImport cfgDeposits() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Gate.io Deposits.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    /** {@code No,Time,Trade type,Role,Market,Deal price,Deal amount SIM,Total SIM,Fee SIM} + [9]=controvalore sintetico */
    private static String[] rigaTrade(String no, String time, String tipo, String dealAmount, String total, String fee) {
        return new String[]{no + "\t", time, tipo, "maker", "XXX/USDT", "0\t", dealAmount, total, fee, "1.00"};
    }

    // ---- colonne composte + scambio gambe -----------------------------------------------------

    @Test
    void configurazioneReale_trades() throws Exception {
        ConfigurazioneImport c = cfgTrades();
        assertEquals("Gate.io", c.nomeExchange);
        assertEquals(" ", c.separatoreValoreMoneta);
        assertTrue(c.causaliScambiaGambe.contains("Sell"));
        assertFalse(c.causaliScambiaGambe.contains("Buy"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.mappaCausali.get("Buy"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.mappaCausali.get("Sell"));
    }

    @Test
    void buy_baseEntraQuotaEsce() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaTrade("1", "2024-01-15 10:00:00", "Buy", "0.001 BTC", "20.5 USDT", "0.02 POINT"),
                null, cfgTrades());
        assertNotNull(movs);
        assertEquals(2, movs.size(), "scambio + commissioni");

        String[] scambio = movs.stream().filter(m -> !m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertEquals("USDT", scambio[8], "la quota esce");
        assertEquals("-20.5", scambio[10]);
        assertEquals("BTC", scambio[11], "la base entra");
        assertEquals("0.001", scambio[13]);

        String[] comm = movs.stream().filter(m -> m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertEquals("POINT", comm[8], "fee importata cosi' come e', anche in POINT");
        assertEquals("-0.02", comm[10]);
    }

    @Test
    void sell_baseEsceQuotaEntra_gambeScambiateRispettoAlBuy() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaTrade("2", "2024-01-16 11:00:00", "Sell", "0.001 BTC", "21 USDT", "0.021 USDT"),
                null, cfgTrades());
        assertNotNull(movs);
        assertEquals(2, movs.size());

        String[] scambio = movs.stream().filter(m -> !m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertEquals("BTC", scambio[8], "sulla Sell la base esce (ruolo scambiato rispetto alla Buy)");
        assertEquals("-0.001", scambio[10]);
        assertEquals("USDT", scambio[11], "sulla Sell la quota entra");
        assertEquals("21", scambio[13]);

        String[] comm = movs.stream().filter(m -> m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertEquals("USDT", comm[8], "fee nella valuta reale della quota, non POINT");
        assertEquals("-0.021", comm[10]);
    }

    // ---- versoForzato ---------------------------------------------------------------------------

    /** {@code OrderID,Time,Network,Address,AddressName,TxID,Coin,Amount,Fee,Received,Status} */
    private static String[] rigaPrelievo(String id, String time, String coin, String amount, String fee) {
        return new String[]{id, time, "BTC", "bc1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "", "deadbeef",
            coin, amount, fee, "0", "Success", "1.00"};
    }

    @Test
    void prelievo_quantitaSempreForzataInUscitaAncheSenzaColonnaCausale() throws Exception {
        ConfigurazioneImport c = cfgWithdrawals();
        assertEquals(-1, c.colonnaCausale, "nessuna colonna causale affidabile in questo export");

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaPrelievo("9001", "2024-02-01 09:00:00", "BTC", "1.5", "0.1"),
                null, c);
        assertNotNull(movs, "'Success' riconosciuto via causalePerNota (contains, case-insensitive)");
        assertEquals(2, movs.size(), "prelievo + commissioni");

        String[] prelievo = movs.stream().filter(m -> !m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertTrue(prelievo[0].endsWith("_PC"), "PRELIEVO CRYPTO: " + prelievo[0]);
        assertEquals("BTC", prelievo[8]);
        assertEquals("-1.5", prelievo[10], "quantita' lorda (Amount), non Amount Received");

        String[] comm = movs.stream().filter(m -> m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertEquals("-0.1", comm[10]);
    }

    /** {@code OrderID,Time,Address,TxID,Coin,Amount,Status} */
    private static String[] rigaDeposito(String id, String time, String coin, String amount) {
        return new String[]{id, time, "bc1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "cafebabe", coin, amount, "Credited", "1.00"};
    }

    @Test
    void deposito_quantitaSempreForzataInEntrata() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaDeposito("9002", "2024-03-01 08:00:00", "BTC", "2.0"),
                null, cfgDeposits());
        assertNotNull(movs, "'credited' riconosciuto via causalePerNota");
        assertEquals(1, movs.size(), "nessuna colonna fee in questo export");

        String[] m = movs.get(0);
        assertTrue(m[0].endsWith("_DC"), "DEPOSITO CRYPTO: " + m[0]);
        assertEquals("BTC", m[11]);
        assertEquals("2", m[13], "stripTrailingZeros: '2.0' -> '2'");
    }

    // ---- lettura dal file, nel formato degli export originali -----------------------------------

    /** Scrive {@code righe} come farebbe Gate.io: BOM + encoding dato, fine riga dato. */
    private static String scriviFile(String nome, String encoding, String fineRiga, String... righe) throws Exception {
        Path f = tempDir.resolve(nome);
        String testo = "\uFEFF" + String.join(fineRiga, righe) + fineRiga;
        Files.write(f, testo.getBytes(encoding));
        return f.toString();
    }

    /** Aggiunge a ogni riga letta il controvalore sintetico, come {@code colonnaValoreEuro} delle cfg*() si aspetta. */
    private static List<String[]> conValore(List<String[]> righe) {
        List<String[]> out = new ArrayList<>();
        for (String[] r : righe) {
            String[] c = Arrays.copyOf(r, r.length + 1);
            c[r.length] = "1.00";
            out.add(c);
        }
        return out;
    }

    @Test
    void fileReale_trades_utf8ConBomETabInCoda() throws Exception {
        String file = scriviFile("Spot_TradeHistory_2021.csv", "UTF-8", "\n",
                "No,Time,Trade type,Role,Market,Deal price,Deal amount,Total,Fee",
                "40982960158\t,2021-04-13 18:29:01,Buy,taker,GT/USDT,3.7459\t,0.259 GT,0.9701881 USDT,0.0019403762 POINT",
                "98156856334\t,2021-11-28 10:00:27,Sell,taker,NFTX/USDT,98.69\t,0.013 NFTX,1.28297 USDT,0.00384891 USDT");
        ConfigurazioneImport c = cfgTrades();
        List<String[]> righe = ImportazioneGenerica.leggiCSV(file, c);
        assertEquals(2, righe.size(), "nessuna riga scartata per data o formato");

        List<String[]> movs = new ArrayList<>();
        for (String[] r : conValore(righe)) movs.addAll(ImportazioneGenerica.costruisciMovimenti(r, null, c));
        assertEquals(4, movs.size(), "2 scambi + 2 commissioni");

        String[] buy = movs.stream().filter(m -> "GT".equals(m[11])).findFirst().orElseThrow();
        assertEquals("0.259", buy[13]);
        assertEquals("USDT", buy[8]);
        assertEquals("-0.9701881", buy[10]);
        String[] sell = movs.stream().filter(m -> "NFTX".equals(m[8])).findFirst().orElseThrow();
        assertEquals("-0.013", sell[10]);
        assertEquals("USDT", sell[11]);
        assertEquals("1.28297", sell[13]);
    }

    @Test
    void fileReale_prelievi_utf16leConBomECrlf() throws Exception {
        String file = scriviFile("mywithdrawals_2024.csv", "UTF-16LE", "\r\n",
                "Order ID\tTime\tNetwork\tAddress\tAddress Name\tTxID\tCoin\tAmount\tTrading Fee\tAmount Received\tStatus",
                "62675616\t2024-09-04 22:11:38\tXLM\tGXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX 1234567\t\tdeadbeef\tXLM\t22.55\t5.4\t17.15\tSuccess");
        ConfigurazioneImport c = cfgWithdrawals();
        List<String[]> righe = ImportazioneGenerica.leggiCSV(file, c);
        assertEquals(1, righe.size());

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(conValore(righe).get(0), null, c);
        assertNotNull(movs);
        assertEquals(2, movs.size(), "prelievo + commissioni");
        String[] prelievo = movs.stream().filter(m -> !m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
        assertTrue(prelievo[0].endsWith("_PC"), prelievo[0]);
        assertEquals("XLM", prelievo[8]);
        assertEquals("-22.55", prelievo[10]);
    }

    @Test
    void fileReale_depositi_utf16leConBomECrlf_ancheSoloIntestazione() throws Exception {
        String intestazione = "Order ID\tTime\tAddress\tTxID\tCoin\tAmount\tStatus";
        ConfigurazioneImport c = cfgDeposits();
        assertTrue(ImportazioneGenerica.leggiCSV(
                scriviFile("mydeposits_vuoto.csv", "UTF-16LE", "\r\n", intestazione), c).isEmpty(),
                "un anno senza depositi: file con la sola intestazione, zero righe");

        List<String[]> righe = ImportazioneGenerica.leggiCSV(scriviFile("mydeposits_2023.csv", "UTF-16LE", "\r\n",
                intestazione, "168772221\t2023-12-28 09:36:34\taXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\tcafebabe\tFIRO\t0.837\tCredited"), c);
        assertEquals(1, righe.size());
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(conValore(righe).get(0), null, c);
        assertNotNull(movs);
        assertEquals(1, movs.size());
        assertTrue(movs.get(0)[0].endsWith("_DC"), movs.get(0)[0]);
        assertEquals("FIRO", movs.get(0)[11]);
        assertEquals("0.837", movs.get(0)[13]);
    }
}
