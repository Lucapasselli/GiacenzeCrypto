package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Importazione Tron ({@link Trans_Tron}): regole del saldo su transazioni costruite a mano (indirizzi generati, nessun
 * wallet vero nel repository; gli unici indirizzi reali sono i contratti pubblici dei token USDT e REA) e, se ci sono, sulle storie
 * complete di wallet pubblici in {@code nocommit/DatiTestChain/TRX/<indirizzo>/}: lì la somma deve dare TRX posseduti,
 * TRC10 e TRC20, salvo i token truffa e quelli con commissione sul trasferimento.
 */
class Trans_TronTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/TRX");
    private static final String USDT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    private static final String USDT_HEX = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c";

    private static final String W_HEX = "41" + "11".repeat(20);
    private static final String ALTRO_HEX = "41" + "22".repeat(20);
    private static final String CONTRATTO_HEX = "41" + "33".repeat(20);
    private static final String W = Trans_Tron.Base58(W_HEX);
    private static final String ALTRO = Trans_Tron.Base58(ALTRO_HEX);
    private static final String FALSO = Trans_Tron.Base58("41" + "44".repeat(20));

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    private static JSONObject tx(String id, long blocco, String tipo, JSONObject valore, String esito, long fee) {
        return new JSONObject().put("txID", id).put("blockNumber", blocco).put("block_timestamp", 1_700_000_000_000L + blocco * 3000)
                .put("ret", new JSONArray().put(new JSONObject().put("contractRet", esito).put("fee", fee)))
                .put("raw_data", new JSONObject().put("contract", new JSONArray().put(new JSONObject().put("type", tipo)
                        .put("parameter", new JSONObject().put("value", valore)))))
                .put("internal_transactions", new JSONArray());
    }

    private static JSONObject trasferimento(String id, String da, String a, String valore, String tipo) {
        return new JSONObject().put("transaction_id", id).put("from", da).put("to", a).put("value", valore).put("type", tipo)
                .put("block_timestamp", 1_700_000_900_000L)
                .put("token_info", new JSONObject().put("address", USDT).put("symbol", "USDT").put("decimals", 6));
    }

    private static List<Trans_Tron.Operazione> analizza(List<JSONObject> nat, List<JSONObject> t20, List<JSONObject> interne,
            Set<String> truffe, List<String> avvisi) {
        return Trans_Tron.Analizza(W, nat, t20, interne, Map.of("T1", 500L, "T2", 501L), truffe, avvisi);
    }

    // ------------------------------------------------------------------
    // Regole del saldo
    // ------------------------------------------------------------------

    @Test
    void unInvioDiTrxPagaLaCommissioneDiRet() {
        JSONObject t = tx("A", 10, "TransferContract", new JSONObject().put("owner_address", W_HEX).put("to_address", ALTRO_HEX)
                .put("amount", 5_000_000), "SUCCESS", 1_100_000);
        Trans_Tron.Operazione o = analizza(List.of(t), List.of(), List.of(), Set.of(), new ArrayList<>()).get(0);
        assertEquals(BigInteger.valueOf(-5_000_000), o.variazioni().get("TRX"));
        assertEquals(BigInteger.valueOf(1_100_000), o.commissione(), "attivazione del conto compresa");
        assertEquals(ALTRO, o.controparte());
        assertEquals("Transfer", o.tipo());
    }

    @Test
    void unaTransazioneFallitaCostaSoloLaCommissione() {
        JSONObject t = tx("B", 11, "TriggerSmartContract", new JSONObject().put("owner_address", W_HEX)
                .put("contract_address", CONTRATTO_HEX).put("call_value", 9_000_000), "REVERT", 2_000_000);
        Trans_Tron.Operazione o = analizza(List.of(t), List.of(), List.of(), Set.of(), new ArrayList<>()).get(0);
        assertFalse(o.riuscita());
        assertTrue(o.variazioni().isEmpty());
        assertEquals(BigInteger.valueOf(2_000_000), o.commissione());
    }

    @Test
    void unoSwapRiceveITrxDalleInterneEITokenDagliEventi() {
        JSONObject t = tx("C", 12, "TriggerSmartContract", new JSONObject().put("owner_address", W_HEX)
                .put("contract_address", CONTRATTO_HEX).put("call_value", 0), "SUCCESS", 3_000_000);
        t.put("internal_transactions", new JSONArray()
                .put(new JSONObject().put("from_address", CONTRATTO_HEX).put("to_address", W_HEX)
                        .put("data", new JSONObject().put("rejected", false).put("call_value", new JSONObject().put("_", 40_000_000))))
                .put(new JSONObject().put("from_address", CONTRATTO_HEX).put("to_address", W_HEX)
                        .put("data", new JSONObject().put("rejected", true).put("call_value", new JSONObject().put("_", 99_000_000)))));
        List<Trans_Tron.Operazione> op = analizza(List.of(t), List.of(trasferimento("C", W, ALTRO, "10000000", "Transfer"),
                trasferimento("C", W, ALTRO, "115792089237316195423570985008687907853269984665640564039457584007913129639935", "Approval")),
                List.of(), Set.of(), new ArrayList<>());
        assertEquals(1, op.size(), "token e TRX della stessa transazione sono un'operazione");
        assertEquals(BigInteger.valueOf(40_000_000), op.get(0).variazioni().get("TRX"), "l'interna respinta non conta");
        assertEquals(BigInteger.valueOf(-10_000_000), op.get(0).variazioni().get(USDT), "gli Approval non sono trasferimenti");
    }

    @Test
    void unAccreditoTrc20HaIlBloccoDaFuori() {
        Trans_Tron.Operazione o = analizza(List.of(), List.of(trasferimento("T1", ALTRO, W, "2500000", "Transfer")),
                List.of(), Set.of(), new ArrayList<>()).get(0);
        assertEquals(500, o.blocco());
        assertEquals(BigInteger.valueOf(2_500_000), o.variazioni().get(USDT));
        assertEquals(BigInteger.ZERO, o.commissione());
        assertEquals(ALTRO, o.controparte());
    }

    @Test
    void leUsciteInventateDiUnTokenTruffaSiScartano() {
        //Address poisoning: il contratto falso emette un Transfer "dal wallet" in una transazione di altri
        JSONObject falso = trasferimento("T2", W, FALSO, "1000000000", "Transfer");
        falso.getJSONObject("token_info").put("address", FALSO);
        List<String> avvisi = new ArrayList<>();
        assertTrue(analizza(List.of(), List.of(falso), List.of(), Set.of(FALSO), avvisi).isEmpty());
        assertEquals(List.of(), avvisi, "solo nel log: sui wallet attivi sarebbe una finestra a ogni scaricamento");
        //Lo stesso token che entra resta (con il marcatore SCAM): e' un movimento vero, anche se senza valore
        JSONObject entra = trasferimento("T1", FALSO, W, "5", "Transfer");
        entra.getJSONObject("token_info").put("address", FALSO);
        assertEquals(1, analizza(List.of(), List.of(entra), List.of(), Set.of(FALSO), new ArrayList<>()).size());
    }

    @Test
    void iTrxDaiContrattiInTransazioniDiAltriArrivanoDaTronScan() {
        JSONObject interna = new JSONObject().put("hash", "D").put("from", ALTRO).put("to", W).put("block", 600)
                .put("timestamp", 1_700_001_800_000L).put("rejected", false)
                .put("token_list", new JSONObject().put("token_id", "_").put("call_value", 566_426_758L));
        Trans_Tron.Operazione o = analizza(List.of(), List.of(), List.of(interna), Set.of(), new ArrayList<>()).get(0);
        assertEquals(BigInteger.valueOf(566_426_758L), o.variazioni().get("TRX"));
        assertEquals(600, o.blocco());
        //Se la transazione e' gia' fra quelle di TronGrid, le sue interne sono gia' contate
        JSONObject nota = tx("D", 600, "TransferContract", new JSONObject().put("owner_address", ALTRO_HEX)
                .put("to_address", W_HEX).put("amount", 1), "SUCCESS", 0);
        assertEquals(BigInteger.ONE, analizza(List.of(nota), List.of(), List.of(interna), Set.of(), new ArrayList<>())
                .get(0).variazioni().get("TRX"));
    }

    @Test
    void lostakingNonMuoveNullaLeRicompenseSi() {
        JSONObject congela = tx("E", 13, "FreezeBalanceV2Contract", new JSONObject().put("owner_address", W_HEX)
                .put("frozen_balance", 100_000_000), "SUCCESS", 0);
        JSONObject ritira = tx("F", 14, "WithdrawBalanceContract", new JSONObject().put("owner_address", W_HEX), "SUCCESS", 0)
                .put("withdraw_amount", 12_721_641);
        List<Trans_Tron.Operazione> op = analizza(List.of(congela, ritira), List.of(), List.of(), Set.of(), new ArrayList<>());
        assertEquals(1, op.size(), "il congelamento senza commissione non e' un'operazione");
        assertEquals(BigInteger.valueOf(12_721_641), op.get(0).ricompense());
        assertTrue(op.get(0).variazioni().isEmpty());
    }

    @Test
    void laRipresaScartaIlBloccoGiaImportato() {
        //TronGrid restituisce ancora il blocco con l'orario usato come limite: si scarta per numero di blocco
        Trans_Tron.Operazione vecchia = new Trans_Tron.Operazione("V", 87369501, 1, "WithdrawBalance", "", true, Map.of(),
                BigInteger.ZERO, BigInteger.TEN);
        Trans_Tron.Operazione nuova = new Trans_Tron.Operazione("N", 87369512, 2, "Transfer", "", true,
                Map.of("TRX", BigInteger.ONE), BigInteger.ZERO, BigInteger.ZERO);
        assertEquals(List.of(nuova), Trans_Tron.DalBlocco(List.of(vecchia, nuova), 87369502));
        assertEquals(2, Trans_Tron.DalBlocco(List.of(vecchia, nuova), 1).size(), "la prima importazione tiene tutto");
    }

    @Test
    void iTrxPossedutiComprendonoCongelatiEDelegati() {
        JSONObject c = new JSONObject().put("balance", 100)
                .put("frozenV2", new JSONArray().put(new JSONObject().put("amount", 1000)).put(new JSONObject().put("type", "ENERGY").put("amount", 20)))
                .put("unfrozenV2", new JSONArray().put(new JSONObject().put("unfreeze_amount", 3)))
                .put("delegated_frozenV2_balance_for_bandwidth", 4)
                .put("account_resource", new JSONObject().put("delegated_frozenV2_balance_for_energy", 5));
        assertEquals(BigInteger.valueOf(1132), Trans_Tron.Posseduti(c));
    }

    // ------------------------------------------------------------------
    // Indirizzi e simboli
    // ------------------------------------------------------------------

    @Test
    void indirizziEsadecimaliEBase58() {
        assertEquals(USDT, Trans_Tron.Base58(USDT_HEX));
        assertTrue(Trans_Tron.isIndirizzo(USDT));
        assertTrue(Trans_Tron.isIndirizzo(W));
        assertFalse(Trans_Tron.isIndirizzo(USDT.substring(0, 33) + "u"), "il controllo deve scartarlo");
        assertTrue(Trans_Tron.isIdTRC10("1004977"));
        assertFalse(Trans_Tron.isIdTRC10("TRX"));
        assertTrue(Funzioni_WalletDeFi.isValidAddress(USDT, Trans_Tron.RETE));
        assertTrue(Funzioni_WalletDeFi.isValidAddress("1002000", Trans_Tron.RETE));
        assertFalse(Funzioni_WalletDeFi.isValidAddress(Trans_Tron.MONETA, Trans_Tron.RETE));
        assertTrue(Funzioni_WalletDeFi.isValidDefiWallet(W + " (TRX)"));
        assertFalse(Funzioni_WalletDeFi.isValidDefiWallet("1002000 (TRX)"));
        assertTrue(Reti.isDefi(Trans_Tron.RETE));
        assertEquals(Trans_Tron.PROVIDER, Importazioni.DeFi_ProviderDefault(Trans_Tron.RETE));
    }

    @Test
    void iTokenTruffaSonoScamEIlSimboloVaAiVerificati() {
        Trans_Tron.Operazione a = new Trans_Tron.Operazione("A", 1, 1, "TRC20", "", true,
                Map.of(FALSO, BigInteger.ONE), BigInteger.ZERO, BigInteger.ZERO);
        Trans_Tron.Operazione b = new Trans_Tron.Operazione("B", 2, 2, "TRC20", "", true,
                Map.of(USDT, BigInteger.ONE, ALTRO, BigInteger.ONE), BigInteger.ZERO, BigInteger.ZERO);
        Map<String, Trans_Tron.Token> token = Map.of(
                FALSO, new Trans_Tron.Token(FALSO, "USDT", "Tether USD", 6, true),
                USDT, new Trans_Tron.Token(USDT, "USDT", "Tether USD", 6, false),
                ALTRO, new Trans_Tron.Token(ALTRO, "USDT", "Un altro USDT", 6, false));
        Map<String, Trans_Tron.Token> s = Trans_Tron.Simboli(List.of(a, b), token, Map.of(USDT, "2", FALSO, "4"), Map.of());
        assertEquals("USDT", s.get(USDT).simbolo(), "il token verificato tiene il simbolo");
        assertEquals("USDT (" + ALTRO + ")", s.get(ALTRO).simbolo());
        assertTrue(Funzioni.isSCAM(s.get(FALSO).simbolo()), "la truffa ha il marcatore SCAM");
    }

    // ------------------------------------------------------------------
    // Storie complete di wallet pubblici (nocommit, non nel repository)
    // ------------------------------------------------------------------

    @Test
    void sulleStorieRealiLaSommaDaISaldiDelConto() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(DATI_REALI), "dati di prova Tron assenti (" + DATI_REALI + ")");
        List<Path> cartelle;
        try (Stream<Path> s = Files.list(DATI_REALI)) {
            cartelle = s.filter(Files::isDirectory).sorted().toList();
        }
        Assumptions.assumeFalse(cartelle.isEmpty());
        for (Path c : cartelle) {
            String w = c.getFileName().toString();
            JSONObject conto = new JSONObject(Files.readString(c.resolve("conto.json"), StandardCharsets.UTF_8))
                    .getJSONObject("conto").getJSONArray("data").getJSONObject(0);
            JSONObject t = new JSONObject(Files.readString(c.resolve("transazioni.json"), StandardCharsets.UTF_8));
            List<JSONObject> nat = Lista(t.getJSONArray("native")), t20 = Lista(t.getJSONArray("trc20"));
            List<JSONObject> interne = Lista(t.getJSONArray("tronscan_interne")), ts = Lista(t.getJSONArray("tronscan_trc20"));
            Map<String, Long> blocchi = new HashMap<>();
            Map<String, String> livelli = new HashMap<>();
            Set<String> truffe = new HashSet<>();
            for (JSONObject x : ts) {
                blocchi.put(x.getString("transaction_id"), x.getLong("block"));
                String l = x.getJSONObject("tokenInfo").optString("tokenLevel");
                livelli.put(x.getString("contract_address"), l);
                if (Trans_Tron.LIVELLO_TRUFFA.equals(l)) truffe.add(x.getString("contract_address"));
            }
            List<String> avvisi = new ArrayList<>();
            List<Trans_Tron.Operazione> op = Trans_Tron.Analizza(w, nat, t20, interne, blocchi, truffe, avvisi);
            assertFalse(op.isEmpty(), w);
            for (Trans_Tron.Operazione o : op) assertTrue(o.blocco() > 0, "operazione senza blocco in " + w);
            Map<String, Trans_Tron.Token> token = new HashMap<>();
            for (Trans_Tron.Operazione o : op) {
                for (String k : o.variazioni().keySet()) {
                    if (!k.equals(Trans_Tron.MONETA)) token.putIfAbsent(k, new Trans_Tron.Token(k, k, k, 0, truffe.contains(k)));
                }
            }
            List<String> differenze = Trans_Tron.VerificaSaldi(op, conto, Trans_Tron.Simboli(op, token, livelli, Map.of()));
            //REA toglie una commissione a ogni trasferimento senza evento: la differenza e' attesa e va segnalata
            differenze.removeIf(d -> d.contains("TH5ydFhBnLV4ZHF2bgBVaTBfX8LY17kj9W"));
            assertEquals(List.of(), differenze, "saldi del wallet " + w);
        }
    }

    private static List<JSONObject> Lista(JSONArray a) {
        List<JSONObject> l = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) l.add(a.getJSONObject(i));
        return l;
    }
}
