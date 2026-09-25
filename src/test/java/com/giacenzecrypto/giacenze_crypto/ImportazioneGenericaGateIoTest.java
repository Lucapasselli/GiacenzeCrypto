package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
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
 *   <li>{@code separatoreValoreMoneta}: una cella "NUMERO;SIMBOLO" (Gate.io Spot Trade History
 *       impacchetta importo e moneta nella stessa colonna, es. "407.57;AME") si legge quando
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
 * righe scritte a mano nella stessa forma grezza dell'export reale, incluso il carattere ';' al
 * posto dello spazio che il file dell'utente porta ovunque (vedi {@code _commentoFormato} nelle
 * config) e il TAB spurio che Gate.io inserisce dopo la 1a e la 6a colonna del trade history.</p>
 *
 * <p>Verificato anche contro i 3 CSV reali dell'utente (non committati): 35 trade -> 70
 * movimenti (35 scambi + 35 commissioni), 6 prelievi -> 12 movimenti, 3 depositi -> 3 movimenti,
 * nessuna riga scartata, importi e direzioni corretti su ogni riga.</p>
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

    /** {@code No,Time,Trade type,Role,Market,Deal price,Deal amount;SIM,Total;SIM,Fee;SIM} + [9]=controvalore sintetico */
    private static String[] rigaTrade(String no, String time, String tipo, String dealAmount, String total, String fee) {
        return new String[]{no + "\t", time, tipo, "maker", "XXX/USDT", "0", dealAmount + "\t", total, fee, "1.00"};
    }

    // ---- colonne composte + scambio gambe -----------------------------------------------------

    @Test
    void configurazioneReale_trades() throws Exception {
        ConfigurazioneImport c = cfgTrades();
        assertEquals("Gate.io", c.nomeExchange);
        assertEquals(";", c.separatoreValoreMoneta);
        assertTrue(c.causaliScambiaGambe.contains("Sell"));
        assertFalse(c.causaliScambiaGambe.contains("Buy"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.mappaCausali.get("Buy"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.mappaCausali.get("Sell"));
    }

    @Test
    void buy_baseEntraQuotaEsce() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaTrade("1", "2024-01-15;10:00:00", "Buy", "0.001;BTC", "20.5;USDT", "0.02;POINT"),
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
                rigaTrade("2", "2024-01-16;11:00:00", "Sell", "0.001;BTC", "21;USDT", "0.021;USDT"),
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
            coin, amount, fee, "0", "Success;;;", "1.00"};
    }

    @Test
    void prelievo_quantitaSempreForzataInUscitaAncheSenzaColonnaCausale() throws Exception {
        ConfigurazioneImport c = cfgWithdrawals();
        assertEquals(-1, c.colonnaCausale, "nessuna colonna causale affidabile in questo export");

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaPrelievo("9001", "2024-02-01;09:00:00", "BTC", "1.5", "0.1"),
                null, c);
        assertNotNull(movs, "'success;;;' riconosciuto via causalePerNota (contains, non match esatto)");
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
                rigaDeposito("9002", "2024-03-01;08:00:00", "BTC", "2.0"),
                null, cfgDeposits());
        assertNotNull(movs, "'credited' riconosciuto via causalePerNota");
        assertEquals(1, movs.size(), "nessuna colonna fee in questo export");

        String[] m = movs.get(0);
        assertTrue(m[0].endsWith("_DC"), "DEPOSITO CRYPTO: " + m[0]);
        assertEquals("BTC", m[11]);
        assertEquals("2", m[13], "stripTrailingZeros: '2.0' -> '2'");
    }
}
