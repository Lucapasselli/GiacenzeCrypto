package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caratterizza le righe della carta Binance in {@code config/import/Binance CSV.json} (v1.020):
 * {@code Spending} e {@code Cashback} del conto Funding, Remark {@code Binance Card}, come compaiono negli
 * export dal 2022. Fino alla 1.019 erano scartate come causali sconosciute, e il saldo EUR restava più alto
 * del reale. Righe sintetiche, nessun dato reale.
 *
 * <ul>
 *   <li>spesa in EUR: prelievo fiat, il rimborso (quantità positiva) un deposito fiat;</li>
 *   <li>spesa in crypto: prelievo da classificare, anche quando la carta preleva due monete nello stesso
 *       secondo (PRELIEVO FIAT è chiusa, non nasce uno scambio fra le due monete);</li>
 *   <li>cashback: ricompensa CASHBACK, come {@code Card Cashback} dei file più vecchi.</li>
 * </ul>
 *
 * <p>Layout righe: {@code User ID,Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore
 * sintetico (evita la ricerca prezzi in rete).</p>
 */
class ImportazioneGenericaBinanceCartaTest {

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

    private static ConfigurazioneImport cfg() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    private static String[] carta(String time, String operation, String coin, String change) {
        return new String[]{"1", time, "Funding", operation, coin, change, "Binance Card", "1.00"};
    }

    private static List<String[]> importa(List<String[]> righe) throws Exception {
        ConfigurazioneImport c = cfg();
        List<String[]> movs = new ArrayList<>();
        Importazioni.AzzeraContatori();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(new ArrayList<>(righe), c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        assertEquals(0, Importazioni.TrasazioniSconosciute, "nessuna riga scartata come causale sconosciuta");
        return movs;
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    @Test
    void causaliMappate() throws Exception {
        ConfigurazioneImport c = cfg();
        assertEquals("PRELIEVO FIAT", c.convertiCausale("Spending"));
        assertEquals("CASHBACK", c.convertiCausale("Cashback"));
        assertEquals("CASHBACK", c.convertiCausale("Cashback Voucher"),
                "il confronto esatto non deve confondere Cashback con le causali che lo contengono");
    }

    @Test
    void spesaInEuro_prelievoFiat() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                carta("2022-05-13 11:36:59", "Spending", "EUR", "-23.56")));
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("PF", categoria(m));
        assertEquals("EUR", m[8]);
        assertEquals(0, new BigDecimal(m[10]).compareTo(new BigDecimal("-23.56")));
    }

    @Test
    void rimborsoInEuro_depositoFiat() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                carta("2022-09-25 08:22:09", "Spending", "EUR", "15.78")));
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("DF", categoria(m));
        assertEquals("EUR", m[11]);
        assertEquals(0, new BigDecimal(m[13]).compareTo(new BigDecimal("15.78")));
    }

    @Test
    void spesaInDueCryptoNelloStessoSecondo_duePrelieviDaClassificare() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                carta("2023-10-23 16:34:49", "Spending", "BNB", "-0.11116915"),
                carta("2023-10-23 16:34:49", "Spending", "USDT", "-14.28101835")));
        assertEquals(2, movs.size(), "PRELIEVO FIAT è chiusa: due prelievi, non uno scambio");
        for (String[] m : movs) {
            assertEquals("PC", categoria(m), m[0]);
            assertEquals("", m[11], "nessuna moneta ricevuta");
        }
    }

    @Test
    void spesaInCryptoEdEuroNelloStessoSecondo_unPrelievoPerMoneta() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                carta("2023-11-16 13:01:58", "Spending", "BNB", "-0.00419161"),
                carta("2023-11-16 13:01:58", "Spending", "EUR", "-0.86370412")));
        assertEquals(2, movs.size());
        String[] bnb = movs.stream().filter(m -> "BNB".equals(m[8])).findFirst().orElseThrow();
        String[] eur = movs.stream().filter(m -> "EUR".equals(m[8])).findFirst().orElseThrow();
        assertEquals("PC", categoria(bnb));
        assertEquals("PF", categoria(eur));
    }

    @Test
    void cashback_ricompensa() throws Exception {
        List<String[]> movs = importa(List.<String[]>of(
                carta("2022-05-16 07:37:11", "Cashback", "BNB", "0.00301069")));
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("RW", categoria(m));
        assertEquals("CASHBACK", m[5]);
        assertEquals("BNB", m[11]);
        assertEquals(0, new BigDecimal(m[13]).compareTo(new BigDecimal("0.00301069")));
    }
}
