package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link GiacenzeExchange}: la giacenza di OKX ricostruita dai {@code bal} dei bill, con i casi trovati sui dati reali
 * (catene dei bot, ordine per billId, Earn, storia che comincia a meta'), e la somma dei saldi di Binance.
 */
class GiacenzeExchangeTest {

    private static long id = 1000;

    private static GiacenzeExchange.Bill B(String conto, String ccy, long ts, String chg, String bal) {
        return B(conto, ccy, ts, chg, bal, "2", "");
    }

    private static GiacenzeExchange.Bill B(String conto, String ccy, long ts, String chg, String bal, String tipo, String note) {
        return new GiacenzeExchange.Bill(conto, ccy, ts, id++, new BigDecimal(chg), new BigDecimal(bal), tipo, note);
    }

    private static BigDecimal Saldo(GiacenzeExchange.StoricoOKX s, String ccy, long t) {
        return s.Saldo(ccy, t);
    }

    @Test
    void dueBotSullaStessaMonetaSonoDueCatene() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("T", "USDC", 100, "100", "100", "1", ""));      //conto
        b.add(B("T", "USDC", 200, "50", "50"));                 //bot A, parte da zero
        b.add(B("T", "USDC", 300, "30", "30"));                 //bot B, parte da zero
        b.add(B("T", "USDC", 400, "-10", "40"));                //bot A
        b.add(B("T", "USDC", 500, "5", "35"));                  //bot B
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of());
        assertNull(Saldo(s, "USDC", 100), "prima del primo bill non si sa");
        assertEquals(0, new BigDecimal("100").compareTo(Saldo(s, "USDC", 150)));
        assertEquals(0, new BigDecimal("180").compareTo(Saldo(s, "USDC", 350)));
        assertEquals(0, new BigDecimal("175").compareTo(Saldo(s, "USDC", 600)), "100 + 40 + 35");
    }

    @Test
    void unBotFermatoSiChiudeAZero() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("T", "SOL", 100, "2", "2", "1", ""));
        b.add(B("T", "SOL", 200, "1", "1", "12", ""));          //trasferimento verso il bot
        b.add(B("T", "SOL", 300, "-1", "0", "12", ""));          //il bot si ferma
        assertEquals(0, new BigDecimal("3").compareTo(GiacenzeExchange.Costruisci(b, List.of()).Saldo("SOL", 250)));
        assertEquals(0, new BigDecimal("2").compareTo(GiacenzeExchange.Costruisci(b, List.of()).Saldo("SOL", 400)));
    }

    @Test
    void laCatenaSegueIlBillIdEnonLOrario() {
        //Caso reale: il trasferimento dal Trading ha un billId precedente ma l'orario di un secondo dopo
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(new GiacenzeExchange.Bill("F", "ETH", 1000, 1, new BigDecimal("0.5"), new BigDecimal("0.5"), "1", "Deposit"));
        b.add(new GiacenzeExchange.Bill("F", "ETH", 2000, 2, new BigDecimal("-0.5"), BigDecimal.ZERO, "75", "Simple Earn subscription"));
        b.add(new GiacenzeExchange.Bill("F", "ETH", 3001, 3, new BigDecimal("0.017"), new BigDecimal("0.017"), "130", "Received from trading account"));
        b.add(new GiacenzeExchange.Bill("F", "ETH", 3000, 4, new BigDecimal("-0.017"), BigDecimal.ZERO, "75", "Simple Earn subscription"));
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of());
        assertEquals(0, new BigDecimal("0.517").compareTo(s.Saldo("ETH", 5000)),
                "tutto in Earn, nessuna giacenza fantasma nel Funding");
        assertEquals(0, new BigDecimal("0.517").compareTo(s.InEarn("ETH", 5000)));
    }

    @Test
    void ilRiscattoConGliInteressiNonLasciaUnCapitaleNegativo() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("F", "BTC", 100, "1", "1", "1", "Deposit"));
        b.add(B("F", "BTC", 200, "-1", "0", "75", "Simple Earn subscription"));
        b.add(B("F", "BTC", 300, "1.01", "1.01", "76", "Simple Earn redemption"));
        b.add(B("F", "BTC", 400, "-0.5", "0.51", "75", "Simple Earn subscription"));
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of());
        assertEquals(0, s.InEarn("BTC", 350).signum(), "l'interesse del riscatto non diventa capitale negativo");
        assertEquals(0, new BigDecimal("0.5").compareTo(s.InEarn("BTC", 500)));
        assertEquals(0, new BigDecimal("1.01").compareTo(s.Saldo("BTC", 500)));
        assertEquals("1.01 (in Earn)", s.Testo("BTC", 500));
        assertNull(GiacenzeBlockchain.Coincide("1.01", s.Testo("BTC", 500)), "in Earn il confronto non si colora");
        assertEquals("1.01", s.Testo("BTC", 350));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, s.Testo("BTC", 50));
    }

    @Test
    void laPosizioneOnChainSiChiudeAlRiscattoDelloStorico() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("F", "USDC", 100, "900", "900", "1", "Deposit"));
        b.add(B("F", "USDC", 200, "-874.1", "25.9", "80", "On-chain Earn subscription"));
        GiacenzeExchange.OrdineOnChain o = new GiacenzeExchange.OrdineOnChain("1", "USDC", new BigDecimal("874.1"), 210, 5000);
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of(o));
        assertEquals(0, new BigDecimal("900").compareTo(s.Saldo("USDC", 1000)));
        assertEquals(0, new BigDecimal("25.9").compareTo(s.Saldo("USDC", 6000)), "convertita in LYUSDC, non e' piu' USDC");
    }

    @Test
    void unaSottoscrizioneOnChainSenzaOrdineRendeIgnotaLaGiacenza() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("F", "USDC", 100, "900", "900", "1", "Deposit"));
        b.add(B("F", "USDC", 200, "-874.1", "25.9", "80", "On-chain Earn subscription"));
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of());
        assertNotNull(s.Saldo("USDC", 150));
        assertNull(s.Saldo("USDC", 300));
    }

    @Test
    void unaCatenaCheNonParteDaZeroEsistevaGiaPrima() {
        List<GiacenzeExchange.Bill> b = new ArrayList<>();
        b.add(B("F", "XCH", 100, "0.01", "0.01", "1", "Deposit"));
        b.add(B("T", "BTC", 500, "0.1", "2.1"));                 //2 BTC gia' presenti prima dei documenti
        GiacenzeExchange.StoricoOKX s = GiacenzeExchange.Costruisci(b, List.of());
        assertEquals(0, new BigDecimal("2").compareTo(s.Saldo("BTC", 300)));
        assertEquals(0, new BigDecimal("2.1").compareTo(s.Saldo("BTC", 600)));
        assertEquals(0, s.Saldo("ETH", 600).signum(), "storia completa: una moneta mai vista vale zero");
        GiacenzeExchange.StoricoOKX parziale = GiacenzeExchange.Costruisci(b, List.of(), false);
        assertNull(parziale.Saldo("ETH", 600), "scaricamenti incrementali: potrebbe essere su OKX senza essersi mossa");
        assertEquals(0, new BigDecimal("2.1").compareTo(parziale.Saldo("BTC", 600)));
    }

    @Test
    void laStoriaECompletaSeUnoScaricamentoHaChiestoTuttoDallInizio() {
        Map<String, GiacenzeExchange.Bill> bills = new LinkedHashMap<>();
        Map<String, GiacenzeExchange.OrdineOnChain> ordini = new LinkedHashMap<>();
        long[] inizio = {Long.MAX_VALUE};
        GiacenzeExchange.LeggiRisposta("{\"script\":\"OKX_Bills\",\"argomenti\":\"OKX startDate=1713123632000 hostname=my.okx.com\","
                + "\"risposta\":{\"okx_fundingBills\":[]}}", bills, ordini, inizio);
        assertEquals(1713123632000L, inizio[0]);
        assertFalse(inizio[0] < GiacenzeExchange.INIZIO_STORIA_COMPLETA);
        GiacenzeExchange.LeggiRisposta("{\"script\":\"OKX_Bills\",\"argomenti\":\"OKX startDate=1483228800000\","
                + "\"risposta\":{\"okx_fundingBills\":[]}}", bills, ordini, inizio);
        assertTrue(inizio[0] < GiacenzeExchange.INIZIO_STORIA_COMPLETA);
    }

    @Test
    void lePrimaRighePrendonoIBillDiFundingTradingEArchivioSenzaDoppioni() {
        Map<String, GiacenzeExchange.Bill> bills = new LinkedHashMap<>();
        Map<String, GiacenzeExchange.OrdineOnChain> ordini = new LinkedHashMap<>();
        String bill = "{\"bal\":\"1\",\"balChg\":\"1\",\"billId\":\"7\",\"ccy\":\"BTC\",\"ts\":\"1000\",\"type\":\"2\"}";
        GiacenzeExchange.LeggiRisposta("{\"risposta\":{\"okx_tradingBills\":[" + bill + "],\"okx_archivioBills\":[" + bill
                + "],\"okx_fundingBills\":[" + bill + "],\"staking_storico\":[{\"ordId\":\"9\",\"purchasedTime\":\"5\","
                + "\"redeemedTime\":\"8\",\"investData\":[{\"ccy\":\"USDC\",\"amt\":\"10\"}]}]}}", bills, ordini);
        assertEquals(2, bills.size(), "lo stesso bill del Trading in API e archivio conta una volta, il Funding a parte");
        assertEquals(1, ordini.size());
        GiacenzeExchange.LeggiRisposta("{\"risposta\":", bills, ordini);
        assertEquals(2, bills.size(), "una riga troncata non rompe la lettura");
    }

    @Test
    void iSaldiDiBinanceSiSommanoESeUnaParteManca() {
        List<String> avvisi = new ArrayList<>();
        Map<String, BigDecimal> s = GiacenzeExchange.InterpretaSaldiBinance("{\"spot\":{\"BTC\":[\"0.1\",\"0.01\"]},"
                + "\"funding\":{\"BTC\":[\"0.2\"]},\"earnFlessibile\":{\"USDC\":[\"100\"]},\"earnBloccato\":{},\"errori\":{}}", avvisi);
        assertEquals(0, new BigDecimal("0.31").compareTo(s.get("btc")));
        assertEquals(0, new BigDecimal("100").compareTo(s.get("USDC")));
        assertNull(GiacenzeExchange.InterpretaSaldiBinance("{\"spot\":{},\"errori\":{\"funding\":\"timeout\"}}", avvisi),
                "senza il Funding il totale sembrerebbe giusto e non lo sarebbe");
        assertTrue(avvisi.get(0).contains("funding"));
    }

    @Test
    void siConfrontaSoloIlWalletDellExchangeConTuttiISottoWallet() {
        assertEquals("OKX", GiacenzeExchange.ExchangeDaLeggere("okx", "Tutti"));
        assertEquals("Binance", GiacenzeExchange.ExchangeDaLeggere("Binance", "Tutti"));
        assertNull(GiacenzeExchange.ExchangeDaLeggere("Binance", "Principale"));
        assertNull(GiacenzeExchange.ExchangeDaLeggere("Tutti", "Tutti"));
    }
}
