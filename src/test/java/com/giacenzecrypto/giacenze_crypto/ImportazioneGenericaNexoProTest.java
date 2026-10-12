package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
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
 * Caratterizza le due config {@code config/import/Nexo Pro *.json}: lo storico degli ordini del Nexo Pro
 * ({@code nexo_pro_trade}) e i suoi depositi/prelievi ({@code DnWHistory}), e l'opzione
 * {@code coppia.importoQuote} che serve ai trade perche' il file non dichiara l'importo della quota.
 *
 * <p>Le righe sono quelle reali del 2023 (solo date e importi). Verificato sui due export completi: la
 * somma degli acquisti, 68,0599 USDT, coincide con gli USDT netti passati dal Savings al Pro, e i
 * 0,0023952 BTC netti (lordo - fee) con i prelievi di BTC verso il Savings.</p>
 */
class ImportazioneGenericaNexoProTest {

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

    private static final String INTESTAZIONE_TRADE = "id,timestamp,pair,side,type,price,executedPrice,triggerPrice,"
            + "requestedAmount,filledAmount,tradingFee,feeCurrency,status,orderId";
    private static final String INTESTAZIONE_DNW = "timestamp,amount,asset,side";

    /** Carica la config e punta il controvalore su una colonna sintetica, per non cercare prezzi in rete. */
    private static ConfigurazioneImport cfg(String nome, int colonnaValore) throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/" + nome + ".json");
        c.colonnaValoreEuro = colonnaValore;
        return c;
    }

    private static String scrivi(String nome, String... righe) throws Exception {
        Path f = tempDir.resolve(nome);
        Files.write(f, (String.join("\n", righe) + "\n").getBytes("UTF-8"));
        return f.toString();
    }

    private static List<String[]> importa(String file, ConfigurazioneImport c) throws Exception {
        List<String[]> movs = new ArrayList<>();
        for (String[] r : ImportazioneGenerica.leggiCSV(file, c)) {
            String[] cv = Arrays.copyOf(r, c.colonnaValoreEuro + 1);
            for (int i = r.length; i < c.colonnaValoreEuro; i++) cv[i] = "";
            cv[c.colonnaValoreEuro] = "1.00";
            List<String[]> m = ImportazioneGenerica.costruisciMovimenti(cv, null, c);
            if (m != null) movs.addAll(m);
        }
        return movs;
    }

    private static String categoria(String[] m) {
        return m[0].split("_")[4];
    }

    private static BigDecimal q(String s) {
        return new BigDecimal(s);
    }

    @Test
    void configurazioniReali_nomeExchange() throws Exception {
        assertEquals("Nexo Pro", cfg("Nexo Pro Trades", 17).nomeExchange);
        assertEquals("Nexo Pro", cfg("Nexo Pro Depositi e Prelievi", 4).nomeExchange);
    }

    @Test
    void acquisto_baseEntraQuotaEsce_quotaCalcolataDaPrezzo_feeInBasePerConto() throws Exception {
        // 2023-04-17: buy 0.0007 BTC a 28550 -> 19.985 USDT, fee 0.0000014 BTC.
        String file = scrivi("Trade1.csv", INTESTAZIONE_TRADE,
                "43523577,2023-04-17 13:16:46.611,BTC/USDT,buy,limit,28550.00000000,28550.00000000,,0.00070,0.00070000,"
                + "0.0000014000000000,BTC,completed,77edf865-b579-4ce3-b80f-2c4a7e99d281");
        List<String[]> movs = importa(file, cfg("Nexo Pro Trades", 17));

        assertEquals(2, movs.size(), "scambio + commissione: " + movs.stream().map(m -> m[0]).toList());
        String[] sc = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        String[] cm = movs.stream().filter(m -> categoria(m).equals("CM")).findFirst().orElseThrow();

        assertEquals("Nexo Pro", sc[3]);
        assertEquals("USDT", sc[8]);
        assertEquals(0, q("-19.985").compareTo(q(sc[10])), "la quota e' quantita' x prezzo di esecuzione");
        assertEquals("BTC", sc[11]);
        assertEquals(0, q("0.0007").compareTo(q(sc[13])), "la base e' la quantita' lorda eseguita");
        assertEquals("BTC", cm[8]);
        assertEquals(0, q("-0.0000014").compareTo(q(cm[10])));
        assertEquals(0, q("0.0006986").compareTo(q(sc[13]).add(q(cm[10]))),
                "lordo + commissione = netto, cioe' quanto il Pro ha poi trasferito al Savings");
    }

    @Test
    void vendita_baseEsceQuotaEntra() throws Exception {
        String file = scrivi("Trade2.csv", INTESTAZIONE_TRADE,
                "1,2023-05-01 10:00:00.5,BTC/USDT,sell,limit,30000,30000,,0.001,0.00100000,0.06,USDT,completed,aaaa");
        List<String[]> movs = importa(file, cfg("Nexo Pro Trades", 17));
        String[] sc = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        assertEquals("BTC", sc[8]);
        assertEquals(0, q("-0.001").compareTo(q(sc[10])));
        assertEquals("USDT", sc[11]);
        assertEquals(0, q("30").compareTo(q(sc[13])));
    }

    @Test
    void ordineAnnullato_nonProduceMovimenti() throws Exception {
        String file = scrivi("Trade3.csv", INTESTAZIONE_TRADE,
                "43785429,2023-04-21 21:02:52.311,BTC/USDT,buy,limit,26600.00000000,,,0.00060,,,,cancelled,e1490640");
        assertTrue(importa(file, cfg("Nexo Pro Trades", 17)).isEmpty());
    }

    @Test
    void depositiEPrelievi_frazioniDiSecondoDaUnaATreCifre() throws Exception {
        String file = scrivi("DnW.csv", INTESTAZIONE_DNW,
                "2023-04-06 13:11:20.95,10.0000000000000000,USDT,DEPOSIT",
                "2023-04-20 06:06:59.238,0.0006986000000000,BTC,WITHDRAW",
                "2023-04-30 06:57:08.7,15.9600000000000000,USDT,WITHDRAW");
        List<String[]> movs = importa(file, cfg("Nexo Pro Depositi e Prelievi", 4));
        assertEquals(3, movs.size());

        String[] dep = movs.stream().filter(m -> categoria(m).equals("DC")).findFirst().orElseThrow();
        assertEquals("Nexo Pro", dep[3]);
        assertEquals("USDT", dep[11]);
        assertEquals(0, q("10").compareTo(q(dep[13])));
        // L'ID porta l'ora nel fuso del sistema (UTC+2 ad aprile), non quella UTC del file: conta solo il giorno.
        assertTrue(dep[0].startsWith("20230406"), dep[0]);

        List<String[]> prelievi = movs.stream().filter(m -> categoria(m).equals("PC")).toList();
        assertEquals(2, prelievi.size());
        assertTrue(prelievi.stream().anyMatch(m -> m[8].equals("BTC") && q(m[10]).compareTo(q("-0.0006986")) == 0));
        assertTrue(prelievi.stream().anyMatch(m -> m[8].equals("USDT") && q(m[10]).compareTo(q("-15.96")) == 0));
    }
}
