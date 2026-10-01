package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
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
 * Caratterizza le tre config {@code config/import/KuCoin *.json} e l'opzione generica {@code coppia}
 * di {@link ImportazioneGenerica} che serve allo Spot: l'export KuCoin non ha una colonna moneta ma
 * la sola coppia {@code BASE-QUOTE} ("KCS-ETH"), che la lettura del file spezza in due colonne
 * virtuali (16 e 17, oltre l'ultima colonna vera) lette poi come {@code moneta}/{@code monetaUscita}.
 *
 * <p>Dati sintetici scritti nella stessa forma degli export originali (intestazioni comprese):
 * i CSV veri dell'utente non vanno committati. Verificato anche sugli export reali (non committati,
 * 2026-10-01): 127 righe spot, 16 depositi, 11 prelievi.</p>
 */
class ImportazioneGenericaKuCoinTest {

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

    private static final String INTESTAZIONE_SPOT = "UID,Account Type,Order ID,Symbol,Side,Order Type,Avg. Filled Price,"
            + "Filled Amount,Filled Volume,Filled Volume (USDT),Filled Time(UTC+00:00),Fee,Tax,Maker/Taker,Fee Currency,Account Mode";
    private static final String INTESTAZIONE_DEP = "UID,Account Type,Time(UTC+00:00),Coin,Amount,Fee,Hash,Deposit Address,"
            + "Transfer Network,Status,Remarks";
    private static final String INTESTAZIONE_PRE = "UID,Account Type,Time(UTC+00:00),Coin,Amount,Fee,Hash,"
            + "Withdrawal Address/Account,Transfer Network,Status,Remarks";

    private static ConfigurazioneImport cfg(String nome, int colonnaValore) throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/" + nome + ".json");
        c.colonnaValoreEuro = colonnaValore; // evita la ricerca prezzi in rete, non pertinente
        return c;
    }

    private static String scrivi(String nome, String... righe) throws Exception {
        Path f = tempDir.resolve(nome);
        Files.write(f, (String.join("\n", righe) + "\n").getBytes("UTF-8"));
        return f.toString();
    }

    /** Legge il file e costruisce i movimenti, con il controvalore sintetico accodato a ogni riga. */
    private static List<String[]> importa(String file, ConfigurazioneImport c) throws Exception {
        List<String[]> movs = new ArrayList<>();
        for (String[] r : ImportazioneGenerica.leggiCSV(file, c)) {
            String[] cv = Arrays.copyOf(r, r.length + 1);
            cv[r.length] = "1.00";
            assertEquals(c.colonnaValoreEuro, r.length, "il controvalore sintetico deve stare subito dopo l'ultima colonna");
            List<String[]> m = ImportazioneGenerica.costruisciMovimenti(cv, null, c);
            assertNotNull(m, Arrays.toString(r));
            movs.addAll(m);
        }
        return movs;
    }

    private static String[] scambio(List<String[]> movs) {
        return movs.stream().filter(m -> !m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
    }

    private static String[] commissione(List<String[]> movs) {
        return movs.stream().filter(m -> m[5].equalsIgnoreCase("COMMISSIONI")).findFirst().orElseThrow();
    }

    @Test
    void configurazioneReale_spot() throws Exception {
        ConfigurazioneImport c = cfg("KuCoin Spot Trades", 18);
        assertEquals("KuCoin", c.nomeExchange, "e' la chiave del gruppo wallet (Wallet 108): non cambiarla");
        assertEquals(3, c.colonnaCoppia);
        assertEquals(16, c.colonnaCoppiaBase);
        assertEquals(17, c.colonnaCoppiaQuote);
        assertTrue(c.causaliScambiaGambe.contains("SELL"));
        assertFalse(c.causaliScambiaGambe.contains("BUY"));
    }

    @Test
    void spotBuy_baseEntraQuotaEsce_feeNellaMonetaDellaColonna() throws Exception {
        String file = scrivi("Spot1.csv", INTESTAZIONE_SPOT,
                "1,mainAccount,aaaa0001,KCS-ETH,BUY,MARKET,0.001369,5.98977355,0.00819999998995,4.88,"
                + "2020-12-06 00:17:29,0.0000163999999799,,TAKER,ETH,CLASSIC");
        List<String[]> movs = importa(file, cfg("KuCoin Spot Trades", 18));
        assertEquals(2, movs.size(), "scambio + commissioni");

        String[] s = scambio(movs);
        assertEquals("ETH", s[8], "la quota esce");
        assertEquals("-0.00819999998995", s[10]);
        assertEquals("KCS", s[11], "la base entra");
        assertEquals("5.98977355", s[13]);

        String[] f = commissione(movs);
        assertEquals("ETH", f[8]);
        assertEquals("-0.0000163999999799", f[10]);
    }

    @Test
    void spotSell_baseEsceQuotaEntra() throws Exception {
        String file = scrivi("Spot2.csv", INTESTAZIONE_SPOT,
                "1,mainAccount,aaaa0002,ETH-USDT,SELL,MARKET,602.72,0.0008818,0.531478496,0.53,"
                + "2020-12-06 00:23:12,0.000531478496,,TAKER,USDT,CLASSIC");
        List<String[]> movs = importa(file, cfg("KuCoin Spot Trades", 18));
        assertEquals(2, movs.size());

        String[] s = scambio(movs);
        assertEquals("ETH", s[8], "sulla SELL la base esce");
        assertEquals("-0.0008818", s[10]);
        assertEquals("USDT", s[11], "sulla SELL la quota entra");
        assertEquals("0.531478496", s[13]);
        assertEquals("USDT", commissione(movs)[8]);
    }

    @Test
    void spot_righeIdenticheDellOrdineParzialeEntranoTutte() throws Exception {
        String riga = "1,mainAccount,aaaa0003,TRX-KCS,BUY,LIMIT,0.024802,27.4615,0.681100123,7.36,"
                + "2025-01-04 13:05:42,0.000681100123,,MAKER,KCS,CLASSIC";
        List<String[]> movs = importa(scrivi("Spot3.csv", INTESTAZIONE_SPOT, riga, riga, riga),
                cfg("KuCoin Spot Trades", 18));
        assertEquals(6, movs.size(), "3 scambi + 3 commissioni: nessuna riga persa");
    }

    @Test
    void spot_coppiaSenzaSeparatoreScartata() throws Exception {
        ConfigurazioneImport c = cfg("KuCoin Spot Trades", 18);
        List<String[]> righe = ImportazioneGenerica.leggiCSV(scrivi("Spot4.csv", INTESTAZIONE_SPOT,
                "1,mainAccount,aaaa0004,KCSETH,BUY,MARKET,1,1,1,1,2020-12-06 00:17:29,0.1,,TAKER,ETH,CLASSIC"), c);
        assertEquals(0, righe.size());
    }

    @Test
    void prelievo_amountENettoEFeeInPiu() throws Exception {
        ConfigurazioneImport c = cfg("KuCoin Withdrawals", 11);
        List<String[]> movs = importa(scrivi("Pre.csv", INTESTAZIONE_PRE,
                "1,mainAccount,2022-05-15 15:45:45,USDT,14,1,beefbeef,TXXXaddr,TRX,SUCCESS,"), c);
        assertEquals(2, movs.size(), "prelievo + commissioni");

        String[] p = scambio(movs);
        assertTrue(p[0].endsWith("_PC"), "PRELIEVO CRYPTO: " + p[0]);
        assertEquals("USDT", p[8]);
        assertEquals("-14", p[10], "Amount e' il netto: la fee non si somma");
        assertEquals("-1", commissione(movs)[10], "la fee e' un movimento a parte, in piu'");
    }

    @Test
    void deposito_quantitaSempreInEntrata() throws Exception {
        ConfigurazioneImport c = cfg("KuCoin Deposits", 11);
        List<String[]> movs = importa(scrivi("Dep.csv", INTESTAZIONE_DEP,
                "1,mainAccount,2020-12-06 00:06:55,ETH,0.01,0,0xabc,0xaddr,ETH,SUCCESS,Deposit"), c);
        assertEquals(1, movs.size(), "nessuna fee nei depositi");
        assertTrue(movs.get(0)[0].endsWith("_DC"), "DEPOSITO CRYPTO: " + movs.get(0)[0]);
        assertEquals("ETH", movs.get(0)[11]);
        assertEquals("0.01", movs.get(0)[13]);
    }
}
