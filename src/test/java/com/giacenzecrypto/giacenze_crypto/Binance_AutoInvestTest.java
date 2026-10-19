package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa {@link Binance_AutoInvest}: i movimenti dei piani Auto-Invest di Binance sistemati con i file di dettaglio
 * Subscription, Rebalancing e Redemption.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>i movimenti importati cambiano solo nei campi 5, 18 e 20: quantità e valore restano quelli del CSV;</li>
 *   <li>un'uscita che paga più monete si abbina a tutte le sue entrate; due piani allo stesso orario con la stessa
 *       moneta si separano per somma esatta;</li>
 *   <li>senza uscita nel CSV (prelievo da Flexible Savings prima del 2023) l'uscita si genera;</li>
 *   <li>le monete di un piano indice restano sul sotto-wallet, i ribilanciamenti prendono il verso che fa tornare
 *       il riscatto, e alla fine il sotto-wallet è a zero;</li>
 *   <li>uno scambio differito automatico giusto resta, uno sbagliato si rifà;</li>
 *   <li>un secondo passaggio non cambia nulla, annullare un movimento riporta l'esecuzione com'era importata.</li>
 * </ul>
 */
class Binance_AutoInvestTest {

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

    /** Prezzi fissi in euro per unità, per i ribilanciamenti. */
    static final Map<String, BigDecimal> PREZZI = new TreeMap<>(Map.of(
            "BTC", new BigDecimal("50000"), "ETH", new BigDecimal("2000"), "ADA", new BigDecimal("0.5"),
            "SOL", new BigDecimal("100"), "LTC", new BigDecimal("80")));
    static final Binance_AutoInvest.Prezzatore PREZZATORE =
            (moneta, quantita, ts) -> PREZZI.getOrDefault(moneta, BigDecimal.ZERO).multiply(quantita);

    /** Movimento importato dal CSV principale. */
    private static String[] movimento(String data, String causale, String moneta, String tipo, String qta, String valore) {
        boolean uscita = qta.startsWith("-");
        String cat = uscita ? ("FIAT".equals(tipo) ? "PF" : "PC") : ("FIAT".equals(tipo) ? "DF" : "DC");
        //Come l'import: movimenti nello stesso secondo si distinguono dal quarto segmento
        String id = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, data.replaceAll(" |-|:", "") + "_Binance_1_1_" + cat);
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = data.substring(0, 16);
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = uscita ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        v[7] = causale;
        if (uscita) {
            v[8] = moneta;
            v[9] = tipo;
            v[10] = qta;
        } else {
            v[11] = moneta;
            v[12] = tipo;
            v[13] = qta;
        }
        v[15] = valore;
        v[22] = "A";
        v[32] = "SI";
        v[41] = "7";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(id, v);
        return v;
    }

    private static File file(String nome, String intestazione, String... righe) throws IOException {
        Path f = tempDir.resolve(System.nanoTime() + "_" + nome);
        Files.writeString(f, intestazione + "\n" + String.join("\n", righe) + "\n");
        return f.toFile();
    }

    private static File sottoscrizioni(String... righe) throws IOException {
        return file("Auto_Invest_History_Subscription_202610181852UTC+2_part1.csv",
                "﻿Time,Holding Coin,Amount Per Period,Trading Fee,Units,From,Status", righe);
    }

    private static File ribilanciamenti(String... righe) throws IOException {
        return file("Auto_Invest_History_Rebalancing_202610181853UTC+2_part1.csv",
                "Time,Index Name,Rebalancing Coin,Rebalancing Amount,Total Rebalancing Fee", righe);
    }

    private static File riscatti(String... righe) throws IOException {
        return file("Auto_Invest_History_Redemption_202610181853UTC+2_part1.csv",
                "Time,Index Name,Coin,Redemption Amount,Redemption Fee", righe);
    }

    private static Map<String, BigDecimal> saldiSottoWallet() {
        Map<String, BigDecimal> saldi = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!Binance_AutoInvest.WALLET_AUTO_INVEST.equals(v[4])) continue;
            if (!Funzioni.noData(v[8])) saldi.merge(v[8], new BigDecimal(v[10]), BigDecimal::add);
            if (!Funzioni.noData(v[11])) saldi.merge(v[11], new BigDecimal(v[13]), BigDecimal::add);
        }
        return saldi;
    }

    private static void assertGruppoSimmetrico(String[] v) {
        List<String> gruppo = new ArrayList<>(Arrays.asList(v[20].split(",")));
        gruppo.add(v[0]);
        for (String id : gruppo) {
            String[] m = MappaCryptoWallet.get(id);
            assertNotNull(m, "Movimento del gruppo mancante: " + id);
            for (String altro : gruppo) {
                if (!altro.equals(id)) assertTrue(Arrays.asList(m[20].split(",")).contains(altro), id + " non cita " + altro);
            }
        }
    }

    // =============================================================================================

    @Test
    void pianoSuPiuMonete_unaUscitaTreEntrate() throws Exception {
        String[] uscita = movimento("2023-08-31 01:07:39", "Auto-Invest Transaction", "BUSD", "Crypto", "-2", "1.84");
        String[] bnb = movimento("2023-08-31 01:10:35", "Auto-Invest Transaction", "BNB", "Crypto", "0.00177812", "0.37");
        String[] btc = movimento("2023-08-31 01:10:39", "Auto-Invest Transaction", "BTC", "Crypto", "0.00002917", "0.73");
        String[] eth = movimento("2023-08-31 01:10:45", "Auto-Invest Transaction", "ETH", "Crypto", "0.00046675", "0.74");

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2023-08-31 01:00:00,BNB,0.40000000 BUSD,0.00080000 BUSD,0.00177812 BNB,Flexible Savings,Success",
                "2023-08-31 01:00:00,BTC,0.80000000 BUSD,0.00160000 BUSD,0.00002917 BTC,Spot Wallet,Success",
                "2023-08-31 01:00:00,ETH,0.80000000 BUSD,0.00160000 BUSD,0.00046675 ETH,Spot Wallet,Success"));

        assertEquals(1, e.esecuzioni);
        assertEquals(1, e.esecuzioniCreate, e.dettagli.toString());
        assertEquals(Binance_AutoInvest.CAMPO18_USCITA, uscita[18]);
        for (String[] r : new String[][]{bnb, btc, eth}) assertEquals(Binance_AutoInvest.CAMPO18_ENTRATA, r[18]);
        //Importati intatti nelle quantità e nei valori
        assertEquals("-2", uscita[10]);
        assertEquals("1.84", uscita[15]);
        assertEquals("0.00177812", bnb[13]);
        //Uscita + speculare + 3 scambi + 3 speculari + 3 entrate
        assertEquals(11, uscita[20].split(",").length + 1);
        assertGruppoSimmetrico(uscita);
        //Sul sotto-wallet entra e esce tutto
        saldiSottoWallet().forEach((m, s) -> assertEquals(0, s.signum(), "saldo " + m));
        //Lo scambio prende il valore dell'entrata, calcolato all'import
        String[] scambioBtc = MappaCryptoWallet.values().stream()
                .filter(v -> Binance_AutoInvest.WALLET_AUTO_INVEST.equals(v[4]) && "BTC".equals(v[11]) && "BUSD".equals(v[8]))
                .findFirst().orElseThrow();
        assertEquals("0.73", scambioBtc[15]);
        assertEquals("-0.8", scambioBtc[10]);
        assertEquals(Binance_AutoInvest.ORIGINE_SOTTOSCRIZIONE, scambioBtc[7]);
        assertEquals("7", scambioBtc[41]);

        //Secondo passaggio: niente da fare
        int n = MappaCryptoWallet.size();
        Binance_AutoInvest.Esito e2 = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2023-08-31 01:00:00,BNB,0.40000000 BUSD,0.00080000 BUSD,0.00177812 BNB,Flexible Savings,Success",
                "2023-08-31 01:00:00,BTC,0.80000000 BUSD,0.00160000 BUSD,0.00002917 BTC,Spot Wallet,Success",
                "2023-08-31 01:00:00,ETH,0.80000000 BUSD,0.00160000 BUSD,0.00046675 ETH,Spot Wallet,Success"));
        assertEquals(1, e2.esecuzioniGiaAPosto);
        assertEquals(0, e2.Creati());
        assertEquals(n, MappaCryptoWallet.size());

        //Annullando l'entrata si torna all'importato
        Funzioni.RimuoviMovimenti(List.of(btc[0]));
        assertEquals(3, MappaCryptoWallet.size(), "restano uscita e le altre due entrate");
        assertTrue(Funzioni.noData(uscita[18]) && Funzioni.noData(uscita[20]));
        assertEquals("PRELIEVO CRYPTO", uscita[5]);
        assertTrue(Funzioni.noData(eth[18]));
    }

    @Test
    void dueUsciteStessoOrario_siSeparanoPerSommaEsatta() throws Exception {
        String[] u1 = movimento("2023-09-29 01:01:04", "Auto-Invest Transaction", "FDUSD", "Crypto", "-1", "0.94");
        String[] u2 = movimento("2023-09-29 01:02:11", "Auto-Invest Transaction", "FDUSD", "Crypto", "-0.5", "0.47");
        String[] bel = movimento("2023-09-29 01:10:43", "Auto-Invest Transaction", "BEL", "Crypto", "0.75770036", "0.46");
        String[] iotx = movimento("2023-09-29 01:10:48", "Auto-Invest Transaction", "IOTX", "Crypto", "64.93376881", "0.93");

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2023-09-29 01:00:00,BEL,0.50000000 FDUSD,0.00100000 FDUSD,0.75770036 BEL,Spot Wallet,Success",
                "2023-09-29 01:00:00,IOTX,1.00000000 FDUSD,0.00200000 FDUSD,64.93376881 IOTX,Spot Wallet,Success"));

        assertEquals(2, e.esecuzioniCreate, e.dettagli.toString());
        assertTrue(Arrays.asList(u1[20].split(",")).contains(iotx[0]), "1 FDUSD ha pagato gli IOTX");
        assertTrue(Arrays.asList(u2[20].split(",")).contains(bel[0]), "0,5 FDUSD hanno pagato i BEL");
        assertFalse(Arrays.asList(u1[20].split(",")).contains(bel[0]));
    }

    @Test
    void senzaUscitaNelCsv_laSiGenera() throws Exception {
        String[] eth = movimento("2022-11-11 14:22:46", "Auto-Invest Transaction", "ETH", "Crypto", "0.00000784", "0.01");
        movimento("2022-10-01 10:00:00", "Deposit", "BUSD", "Crypto", "10", "10.00"); //solo per il tipo della moneta

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2022-11-11 14:00:00,ETH,0.01000000 BUSD,--,0.00000784 ETH,Spot Wallet + Flexible Savings,Success"));

        assertEquals(1, e.esecuzioniCreate, e.dettagli.toString());
        assertEquals(1, e.usciteGenerate);
        String[] generata = MappaCryptoWallet.values().stream()
                .filter(v -> "BUSD".equals(v[8]) && "Principale".equals(v[4]) && "AU".equals(v[22])).findFirst().orElseThrow();
        assertEquals("-0.01", generata[10]);
        assertEquals(Binance_AutoInvest.CAMPO18_USCITA, generata[18]);
        assertTrue(Arrays.asList(eth[20].split(",")).contains(generata[0]));
        saldiSottoWallet().forEach((m, s) -> assertEquals(0, s.signum(), "saldo " + m));

        //Annullando, l'uscita generata sparisce
        Funzioni.RimuoviMovimenti(List.of(generata[0]));
        assertEquals(2, MappaCryptoWallet.size());
        assertTrue(Funzioni.noData(eth[18]));
    }

    @Test
    void pianoIndice_ribilanciamentoERiscatto_sottoWalletAZero() throws Exception {
        //Due acquisti EUR di BTC e ADA, un ribilanciamento che vende ADA e compra BTC e SOL, un riscatto finale
        String[] u1 = movimento("2023-06-13 03:08:49", "Auto-Invest Transaction", "EUR", "FIAT", "-0.4", "0.40");
        String[] u2 = movimento("2023-06-14 03:07:47", "Auto-Invest Transaction", "EUR", "FIAT", "-0.4", "0.40");
        String[] rBtc = movimento("2023-08-10 05:36:45", "Index-Auto-Investment - Redeem Positions", "BTC", "Crypto", "0.000009", "0.45");
        String[] rAda = movimento("2023-08-10 05:36:45", "Index-Auto-Investment - Redeem Positions", "ADA", "Crypto", "0.5", "0.25");
        String[] rSol = movimento("2023-08-10 05:36:45", "Index-Auto-Investment - Redeem Positions", "SOL", "Crypto", "0.001", "0.10");

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE,
                sottoscrizioni(
                        "2023-06-13 03:00:00,BTC,0.20000000 EUR,--,0.000004 BTC,Spot Wallet,Success",
                        "2023-06-13 03:00:00,ADA,0.20000000 EUR,--,0.4 ADA,Spot Wallet,Success",
                        "2023-06-14 03:00:00,BTC,0.20000000 EUR,--,0.000004 BTC,Spot Wallet,Success",
                        "2023-06-14 03:00:00,ADA,0.20000000 EUR,--,0.4 ADA,Spot Wallet,Success"),
                ribilanciamenti(
                        "2023-08-10 05:36:45,Indice,LTC,0,0.00000000 USDT",
                        "2023-06-30 10:00:15,Indice,ADA,0.3,0.00100000 USDT",
                        "2023-06-30 10:00:15,Indice,BTC,0.000001,0.00100000 USDT",
                        "2023-06-30 10:00:15,Indice,SOL,0.001,0.00100000 USDT"),
                riscatti(
                        "2023-08-10 05:36:45,Indice,BTC,0.000009,0",
                        "2023-08-10 05:36:45,Indice,ADA,0.5,0",
                        "2023-08-10 05:36:45,Indice,SOL,0.001,0"));

        assertEquals(2, e.esecuzioniCreate, e.dettagli.toString());
        assertEquals(4, e.acquistiIndice);
        assertEquals(1, e.ribilanciamenti);
        assertEquals(1, e.ribilanciamentiCreati, e.dettagli.toString());
        assertEquals(3, e.monetePerQuadratura);
        assertEquals(3, e.riscattiCreati);
        assertEquals(Binance_AutoInvest.CAMPO18_USCITA, u1[18]);
        assertEquals(Binance_AutoInvest.CAMPO18_ENTRATA, rAda[18]);
        saldiSottoWallet().forEach((m, s) -> assertEquals(0, s.signum(), "saldo " + m));
        assertGruppoSimmetrico(u2);
        assertGruppoSimmetrico(rBtc);

        //Il ribilanciamento: ADA venduto, BTC e SOL comprati, scambi diretti senza USDT
        List<String[]> scambi = MappaCryptoWallet.values().stream()
                .filter(v -> Binance_AutoInvest.ORIGINE_RIBILANCIAMENTO.equals(v[7])).toList();
        assertEquals(2, scambi.size());
        BigDecimal adaVenduto = BigDecimal.ZERO;
        for (String[] s : scambi) {
            assertEquals("ADA", s[8]);
            adaVenduto = adaVenduto.add(new BigDecimal(s[10]));
            assertGruppoSimmetrico(s);
        }
        assertEquals(0, new BigDecimal("-0.3").compareTo(adaVenduto));
        //0,3 ADA (0,15 euro) ripartiti per valore: BTC 0,05 euro, SOL 0,10 euro
        String[] versoBtc = scambi.stream().filter(s -> "BTC".equals(s[11])).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("-0.1").compareTo(new BigDecimal(versoBtc[10])));
        assertEquals(0, new BigDecimal("0.000001").compareTo(new BigDecimal(versoBtc[13])));

        //Gli acquisti EUR sono acquisti, valorizzati all'importo
        String[] acquisto = MappaCryptoWallet.values().stream()
                .filter(v -> Binance_AutoInvest.ORIGINE_SOTTOSCRIZIONE.equals(v[7]) && "EUR".equals(v[8])).findFirst().orElseThrow();
        assertEquals("AC", acquisto[0].split("_")[4]);
        assertEquals("0.20", acquisto[15]);

        //Secondo passaggio: niente da fare
        int n = MappaCryptoWallet.size();
        Binance_AutoInvest.Esito e2 = Binance_AutoInvest.Abbina(PREZZATORE,
                sottoscrizioni(
                        "2023-06-13 03:00:00,BTC,0.20000000 EUR,--,0.000004 BTC,Spot Wallet,Success",
                        "2023-06-13 03:00:00,ADA,0.20000000 EUR,--,0.4 ADA,Spot Wallet,Success",
                        "2023-06-14 03:00:00,BTC,0.20000000 EUR,--,0.000004 BTC,Spot Wallet,Success",
                        "2023-06-14 03:00:00,ADA,0.20000000 EUR,--,0.4 ADA,Spot Wallet,Success"),
                ribilanciamenti(
                        "2023-06-30 10:00:15,Indice,ADA,0.3,0.00100000 USDT",
                        "2023-06-30 10:00:15,Indice,BTC,0.000001,0.00100000 USDT",
                        "2023-06-30 10:00:15,Indice,SOL,0.001,0.00100000 USDT"),
                riscatti(
                        "2023-08-10 05:36:45,Indice,BTC,0.000009,0",
                        "2023-08-10 05:36:45,Indice,ADA,0.5,0",
                        "2023-08-10 05:36:45,Indice,SOL,0.001,0"));
        assertEquals(0, e2.Creati());
        assertEquals(2, e2.esecuzioniGiaAPosto);
        assertEquals(1, e2.ribilanciamentiGiaAPosto);
        assertEquals(3, e2.riscattiGiaAPosto);
        assertEquals(n, MappaCryptoWallet.size());
        assertNotNull(rSol);
    }

    @Test
    void pianoIndiceAperto_versoDaiPrezzi() throws Exception {
        movimento("2023-06-13 03:08:49", "Auto-Invest Transaction", "EUR", "FIAT", "-150", "150.00");
        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE,
                sottoscrizioni(
                        "2023-06-13 03:00:00,BTC,50.00000000 EUR,--,0.001 BTC,Spot Wallet,Success",
                        "2023-06-13 03:00:00,ETH,100.00000000 EUR,--,0.05 ETH,Spot Wallet,Success"),
                ribilanciamenti(
                        "2023-06-30 10:00:15,Indice,ETH,0.0125,0.01 USDT",
                        "2023-06-30 10:00:15,Indice,BTC,0.0005,0.01 USDT"));
        assertEquals(1, e.ribilanciamentiCreati, e.dettagli.toString());
        assertEquals(2, e.monetePerPrezzo);
        //ETH vale 100 euro e BTC 50: il ribilanciamento sposta 25 euro, e lo sbilancio è nullo in tutti e due i versi.
        //Decide il peso uguale: vendere ETH e comprare BTC porta a 75 e 75, il contrario a 125 e 25
        List<String[]> scambi = MappaCryptoWallet.values().stream()
                .filter(v -> Binance_AutoInvest.ORIGINE_RIBILANCIAMENTO.equals(v[7])).toList();
        assertEquals(1, scambi.size());
        assertEquals("ETH", scambi.get(0)[8]);
        assertEquals("BTC", scambi.get(0)[11]);
        saldiSottoWallet().forEach((m, s) -> assertTrue(s.signum() >= 0, "saldo " + m));
    }

    @Test
    void scambioDifferitoAutomatico_giustoResta_sbagliatoSiRifa() throws Exception {
        //Giusto: 0,1 USDT -> FUN della stessa esecuzione
        String[] uFun = movimento("2023-05-02 09:02:09", "Auto-Invest Transaction", "USDT", "Crypto", "-0.1", "0.09");
        String[] eFun = movimento("2023-05-02 09:13:42", "Auto-Invest Transaction", "FUN", "Crypto", "17.37873739", "0.09");
        //Sbagliato: 2 EUR del piano indice abbinati ai BNB di un altro piano
        String[] uEur = movimento("2023-08-30 02:38:16", "Auto-Invest Transaction", "EUR", "FIAT", "-2", "2.00");
        String[] uBusd = movimento("2023-08-30 02:42:00", "Auto-Invest Transaction", "BUSD", "Crypto", "-2", "1.84");
        String[] eBnb = movimento("2023-08-30 02:46:35", "Auto-Invest Transaction", "BNB", "Crypto", "0.00175691", "0.37");
        String[] eBtc = movimento("2023-08-30 02:46:39", "Auto-Invest Transaction", "BTC", "Crypto", "0.00002866", "0.73");
        String[] eEth = movimento("2023-08-30 02:46:46", "Auto-Invest Transaction", "ETH", "Crypto", "0.00045972", "0.74");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(uFun[0], eFun[0]));
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(uEur[0], eBnb[0]));
        String idFun = uFun[0];

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2023-05-02 09:00:00,FUN,0.10000000 USDT,--,17.37873739 FUN,Flexible Savings,Success",
                "2023-08-30 02:30:00,BTC,1.00000000 EUR,--,0.00002 BTC,Spot Wallet,Success",
                "2023-08-30 02:30:00,ETH,1.00000000 EUR,--,0.0005 ETH,Spot Wallet,Success",
                "2023-08-30 02:40:00,BNB,0.40000000 BUSD,0.00080000 BUSD,0.00175691 BNB,Flexible Savings,Success",
                "2023-08-30 02:40:00,BTC,0.80000000 BUSD,0.00160000 BUSD,0.00002866 BTC,Spot Wallet,Success",
                "2023-08-30 02:40:00,ETH,0.80000000 BUSD,0.00160000 BUSD,0.00045972 ETH,Spot Wallet,Success"));

        assertEquals(1, e.esecuzioniScambioDifferito, e.dettagli.toString());
        assertEquals(1, e.scambiDifferitiSostituiti);
        assertEquals(1, e.riparazioni.size());
        assertEquals(2, e.esecuzioniCreate, e.dettagli.toString());
        assertEquals(idFun, uFun[0], "lo scambio giusto non si tocca");
        assertEquals(MovimentiCollegati.CAMPO18_PRELIEVO_DIFFERITO, uFun[18]);
        assertEquals(Binance_AutoInvest.CAMPO18_USCITA, uEur[18]);
        assertEquals(Binance_AutoInvest.CAMPO18_USCITA, uBusd[18]);
        assertEquals(Binance_AutoInvest.CAMPO18_ENTRATA, eBnb[18]);
        assertTrue(Arrays.asList(uBusd[20].split(",")).contains(eBnb[0]));
        assertTrue(Arrays.asList(uBusd[20].split(",")).contains(eBtc[0]));
        assertTrue(Arrays.asList(uBusd[20].split(",")).contains(eEth[0]));
        assertEquals(0, MovimentiCollegati.Controlla(MappaCryptoWallet.values()).Totale());
    }

    @Test
    void entrataClassificataAMano_nonSiTocca() throws Exception {
        String[] u = movimento("2023-05-02 09:02:09", "Auto-Invest Transaction", "USDT", "Crypto", "-0.1", "0.09");
        String[] en = movimento("2023-05-02 09:13:42", "Auto-Invest Transaction", "FUN", "Crypto", "17.37873739", "0.09");
        en[18] = "DCZ - DEPOSITO A COSTO 0";
        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, sottoscrizioni(
                "2023-05-02 09:00:00,FUN,0.10000000 USDT,--,17.37873739 FUN,Flexible Savings,Success"));
        assertEquals(1, e.esecuzioniNonTrovate);
        assertEquals(0, e.esecuzioniCreate);
        assertTrue(Funzioni.noData(u[18]));
        assertEquals(2, MappaCryptoWallet.size());
    }

    @Test
    void offsetDalNomeDelFile() {
        assertEquals(2, Binance_AutoInvest.EstraiOffset("01_Binance_Auto_Invest_History_Subscription_202610181852UTC2_part1.csv"));
        assertEquals(2, Binance_AutoInvest.EstraiOffset("x_202610181852UTC+2.csv"));
        assertEquals(-3, Binance_AutoInvest.EstraiOffset("x_202610181852UTC-3_part2.csv"));
        assertEquals(0, Binance_AutoInvest.EstraiOffset("senza_fuso.csv"));
        assertNull(Binance_AutoInvest.OffsetDalNome("senza_fuso.csv"));
        assertEquals(0, Binance_AutoInvest.OffsetDalNome("x_202610181852UTC0_part1.csv"));
    }

    /**
     * L'utente può rinominare i file di dettaglio: il tipo viene dall'intestazione e il fuso, che Binance scrive nel
     * nome, si ricava dal contenuto. Due piani a un'ora di distanza con lo stesso importo (forma vista su un dataset reale, valori inventati):
     * con gli orari del file spostati di due ore ogni esecuzione cadrebbe più vicina all'uscita dell'altra, e le
     * uscite si abbinerebbero alle monete sbagliate.
     */
    @Test
    void fileRinominati_fusoDalContenuto() throws Exception {
        String intestazione = "Time,Holding Coin,Amount Per Period,Trading Fee,Units,From,Status";
        String[] righe = {
            "2023-09-18 01:00:00,IOTX,1.00000000 FDUSD,0.00200000 FDUSD,50.12345678 IOTX,Spot Wallet,Success",
            "2023-09-18 02:00:00,ADA,1.00000000 FDUSD,0.00200000 FDUSD,200.87654321 ADA,Spot Wallet,Success"};
        for (boolean conAltroFile : new boolean[]{false, true}) {
            MappaCryptoWallet.clear();
            String[] u1 = movimento("2023-09-18 01:01:10", "Auto-Invest Transaction", "FDUSD", "Crypto", "-1", "0.94");
            String[] iotx = movimento("2023-09-18 01:11:20", "Auto-Invest Transaction", "IOTX", "Crypto", "50.12345678", "0.93");
            String[] u2 = movimento("2023-09-18 02:01:10", "Auto-Invest Transaction", "FDUSD", "Crypto", "-1", "0.94");
            String[] ada = movimento("2023-09-18 02:10:30", "Auto-Invest Transaction", "ADA", "Crypto", "200.87654321", "0.93");

            File rinominato = file("acquisti.csv", intestazione, righe);
            //Con un altro file col nome di Binance il fuso viene da quello, senza stime né avvisi
            Binance_AutoInvest.Esito e = conAltroFile
                    ? Binance_AutoInvest.Abbina(PREZZATORE, rinominato, riscatti())
                    : Binance_AutoInvest.Abbina(PREZZATORE, rinominato);

            assertEquals(2, e.esecuzioniCreate, e.dettagli.toString());
            assertTrue(Arrays.asList(u1[20].split(",")).contains(iotx[0]), "l'uscita delle 01:01 ha pagato gli IOTX");
            assertTrue(Arrays.asList(u2[20].split(",")).contains(ada[0]), "l'uscita delle 02:01 ha pagato gli ADA");
            if (conAltroFile) {
                assertTrue(e.avvisi.stream().noneMatch(a -> a.contains("fuso")), e.avvisi.toString());
            } else {
                assertTrue(e.avvisi.stream().anyMatch(a -> a.contains("spostati di -2 ore")), e.avvisi.toString());
            }
        }
    }

    /** Senza fuso nel nome e senza nulla nel CSV a cui appaiare i file, lo dice il resoconto. */
    @Test
    void fileRinominati_fusoNonRicavabile() throws Exception {
        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(PREZZATORE, file("acquisti.csv",
                "Time,Holding Coin,Amount Per Period,Trading Fee,Units,From,Status",
                "2022-11-14 15:00:00,ETH,0.01000000 BUSD,--,0.00000750 ETH,Spot Wallet,Success"));
        assertTrue(e.avvisi.stream().anyMatch(a -> a.contains("letti come UTC")), e.avvisi.toString());
    }

    @Test
    void soluzioniSegni_ricercaPerMeta() {
        long[] q = {5, 3, 7, 11};
        //+5 -3 +7 +11 = 20 ; 5+3-7+11 = 12 ...
        List<Long> s = Binance_AutoInvest.SoluzioniSegni(q, 20, 10);
        assertNotNull(s);
        assertEquals(1, s.size());
        assertEquals(0b0010L, (long) s.get(0));
        assertTrue(Binance_AutoInvest.SoluzioniSegni(q, 1, 10).isEmpty());
    }
}
