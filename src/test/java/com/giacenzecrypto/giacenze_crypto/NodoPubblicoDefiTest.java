package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NodoPubblicoDefi} su una catena finta: la bisezione deve trovare tutti e soli i blocchi in cui lo stato del
 * wallet cambia, e le righe che escono devono essere quelle che {@code DeFi_RitornaTransazioni} si aspetta da
 * Etherscan.
 */
class NodoPubblicoDefiTest {

    static final String W = "0x00000000000000000000000000000000000000aa";
    static final String X = "0x00000000000000000000000000000000000000bb";
    static final String ROUTER = "0x00000000000000000000000000000000000000cc";
    static final String USDC = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913";
    static final String WETH = "0x4200000000000000000000000000000000000006";
    static final String NUOVO = "0x00000000000000000000000000000000000000dd";
    static final String RIBASA = "0x00000000000000000000000000000000000000ee";
    static final BigInteger ETH = BigInteger.TEN.pow(18);

    static final NodoPubblicoDefi.Configurazione CONF = new NodoPubblicoDefi.Configurazione("BASE",
            List.of("x"), List.of("x"), List.of("x"), List.of(USDC, WETH), List.of(WETH));

    //=====================================================================================================
    //=== CATENA FINTA
    //=====================================================================================================

    /** Una transazione della catena finta, con i suoi effetti sul wallet. */
    static final class Tx {
        String hash, from, to;
        BigInteger value = BigInteger.ZERO, gas = BigInteger.valueOf(21000), prezzo = BigInteger.valueOf(1_000_000_000),
                l1 = BigInteger.ZERO, interno = BigInteger.ZERO;
        boolean ok = true;
        List<JsonObject> logs = new ArrayList<>();
    }

    static final class Catena implements NodoPubblicoDefi.Nodo {
        final long testa;
        final TreeMap<Long, List<Tx>> blocchi = new TreeMap<>();
        /** Saldi dei token del wallet per blocco: contratto -> (blocco da cui vale -> saldo). */
        final Map<String, TreeMap<Long, BigInteger>> saldi = new HashMap<>();
        /** Token il cui saldo cresce di 1 a ogni blocco a partire da un blocco, senza log. */
        final Map<String, Long> ribasanti = new HashMap<>();
        String codice = "0x";
        boolean guasto = false;
        int chiamate = 0;

        Catena(long testa) {
            this.testa = testa;
        }

        Tx tx(long blocco, String hash, String from, String to) {
            Tx t = new Tx();
            t.hash = hash;
            t.from = from;
            t.to = to;
            blocchi.computeIfAbsent(blocco, k -> new ArrayList<>()).add(t);
            return t;
        }

        void saldo(String token, long blocco, BigInteger valore) {
            saldi.computeIfAbsent(token, k -> new TreeMap<>()).put(blocco, valore);
        }

        BigInteger nonce(long b) {
            long n = 0;
            for (Map.Entry<Long, List<Tx>> e : blocchi.headMap(b, true).entrySet())
                for (Tx t : e.getValue()) if (t.from.equals(W)) n++;
            return BigInteger.valueOf(n);
        }

        BigInteger eth(long b) {
            BigInteger s = ETH.multiply(BigInteger.TEN); //10 ETH iniziali, dal blocco 0
            for (Map.Entry<Long, List<Tx>> e : blocchi.headMap(b, true).entrySet()) {
                for (Tx t : e.getValue()) {
                    if (t.from.equals(W)) {
                        s = s.subtract(t.gas.multiply(t.prezzo)).subtract(t.l1);
                        if (t.ok && !t.to.equals(W)) s = s.subtract(t.value);
                    } else if (t.to.equals(W) && t.ok) {
                        s = s.add(t.value);
                    }
                    if (t.ok) s = s.add(t.interno);
                }
            }
            return s;
        }

        BigInteger token(String t, long b) {
            if (ribasanti.containsKey(t)) return b < ribasanti.get(t) ? BigInteger.ZERO : BigInteger.valueOf(b - ribasanti.get(t) + 1);
            TreeMap<Long, BigInteger> m = saldi.get(t);
            if (m == null) return BigInteger.ZERO;
            Map.Entry<Long, BigInteger> e = m.floorEntry(b);
            return e == null ? BigInteger.ZERO : e.getValue();
        }

        @Override
        public JsonElement chiama(NodoPubblicoDefi.Uso uso, String metodo, JsonArray p) throws Exception {
            chiamate++;
            if (guasto) throw new Exception("nodo giu'");
            switch (metodo) {
                case "eth_getCode": return new JsonPrimitive(codice);
                case "eth_blockNumber": return new JsonPrimitive("0x" + Long.toHexString(testa + 3));
                case "eth_getTransactionCount": return Hex(nonce(Blocco(p.get(1))));
                case "eth_getBalance": return Hex(eth(Blocco(p.get(1))));
                case "eth_call": return Call(p.get(0).getAsJsonObject(), p.get(1));
                case "eth_getBlockByNumber": return Blocco(Blocco(p.get(0)));
                case "eth_getLogs": return Logs(p.get(0).getAsJsonObject());
                case "eth_getTransactionReceipt": return Ricevuta(p.get(0).getAsString());
                default: throw new Exception("metodo non previsto " + metodo);
            }
        }

        static long Blocco(JsonElement e) {
            return Long.decode(e.getAsString());
        }

        static JsonPrimitive Hex(BigInteger v) {
            return new JsonPrimitive("0x" + v.toString(16));
        }

        JsonElement Call(JsonObject c, JsonElement blocco) {
            String to = c.get("to").getAsString().toLowerCase();
            String data = c.get("data").getAsString().substring(2);
            if (to.equals(Trans_XLayer.MULTICALL3)) {
                long b = Blocco(blocco);
                List<BigInteger> ris = new ArrayList<>();
                int n = new BigInteger(data.substring(8 + 64, 8 + 128), 16).intValueExact();
                int contenuto = 8 + 128;
                for (int i = 0; i < n; i++) {
                    int el = contenuto + new BigInteger(data.substring(contenuto + i * 64, contenuto + i * 64 + 64), 16).intValueExact() * 2;
                    String target = "0x" + data.substring(el + 24, el + 64);
                    String sel = data.substring(el + 256, el + 264);
                    if (sel.equals(Trans_XLayer.SEL_GET_ETH_BALANCE)) ris.add(eth(b));
                    else ris.add(token(target, b));
                }
                return new JsonPrimitive(Aggregate3Risultato(ris));
            }
            String sel = data.substring(0, 8);
            if (sel.equals(Trans_XLayer.SEL_DECIMALS)) return new JsonPrimitive("0x" + Trans_XLayer.Parola(to.equals(USDC) ? "6" : "c"));
            String nome = to.equals(USDC) ? "USDC" : to.equals(WETH) ? "WETH" : "Claim on: esempio.com";
            return new JsonPrimitive(StringaAbi(nome));
        }

        JsonObject Blocco(long b) {
            JsonObject o = new JsonObject();
            o.addProperty("timestamp", "0x" + Long.toHexString(1_700_000_000L + b * 2));
            JsonArray txs = new JsonArray();
            for (Tx t : blocchi.getOrDefault(b, List.of())) {
                JsonObject j = new JsonObject();
                j.addProperty("hash", t.hash);
                j.addProperty("from", t.from);
                j.addProperty("to", t.to);
                j.addProperty("value", "0x" + t.value.toString(16));
                txs.add(j);
            }
            //una transazione d'altri nello stesso blocco, che non deve entrare
            JsonObject altra = new JsonObject();
            altra.addProperty("hash", "0xaltra" + b);
            altra.addProperty("from", X);
            altra.addProperty("to", ROUTER);
            altra.addProperty("value", "0x5");
            txs.add(altra);
            o.add("transactions", txs);
            return o;
        }

        JsonArray Logs(JsonObject f) {
            long b = Blocco(f.get("fromBlock"));
            JsonArray topics = f.getAsJsonArray("topics");
            Set<String> eventi = new HashSet<>();
            topics.get(0).getAsJsonArray().forEach(e -> eventi.add(e.getAsString()));
            int pos = topics.size() - 1;
            String parola = topics.get(pos).getAsString();
            Set<String> indirizzi = null;
            if (f.has("address")) {
                indirizzi = new HashSet<>();
                for (JsonElement e : f.getAsJsonArray("address")) indirizzi.add(e.getAsString());
            }
            JsonArray r = new JsonArray();
            for (Tx t : blocchi.getOrDefault(b, List.of())) {
                if (!t.ok) continue;
                for (JsonObject l : t.logs) {
                    JsonArray tp = l.getAsJsonArray("topics");
                    if (!eventi.contains(tp.get(0).getAsString()) || tp.size() <= pos) continue;
                    if (!tp.get(pos).getAsString().equals(parola)) continue;
                    if (indirizzi != null && !indirizzi.contains(l.get("address").getAsString())) continue;
                    r.add(l);
                }
            }
            return r;
        }

        JsonObject Ricevuta(String hash) {
            for (List<Tx> l : blocchi.values()) for (Tx t : l) {
                if (!t.hash.equals(hash)) continue;
                JsonObject o = new JsonObject();
                o.addProperty("status", t.ok ? "0x1" : "0x0");
                o.addProperty("gasUsed", "0x" + t.gas.toString(16));
                o.addProperty("effectiveGasPrice", "0x" + t.prezzo.toString(16));
                o.addProperty("l1Fee", "0x" + t.l1.toString(16));
                o.add("contractAddress", com.google.gson.JsonNull.INSTANCE);
                return o;
            }
            return null;
        }
    }

    static int indiceLog = 0;

    static JsonObject Log(String contratto, String evento, String a1, String a2, BigInteger valore, String txHash) {
        JsonObject l = new JsonObject();
        l.addProperty("address", contratto);
        JsonArray tp = new JsonArray();
        tp.add(evento);
        tp.add("0x" + Trans_XLayer.Parola(a1));
        if (a2 != null) tp.add("0x" + Trans_XLayer.Parola(a2));
        l.add("topics", tp);
        l.addProperty("data", "0x" + Trans_XLayer.Parola(valore.toString(16)));
        l.addProperty("transactionHash", txHash);
        l.addProperty("logIndex", "0x" + Integer.toHexString(indiceLog++));
        return l;
    }

    static String Aggregate3Risultato(List<BigInteger> valori) {
        StringBuilder sb = new StringBuilder("0x").append(Trans_XLayer.Parola("20")).append(Trans_XLayer.Parola(Integer.toHexString(valori.size())));
        int n = valori.size();
        for (int i = 0; i < n; i++) sb.append(Trans_XLayer.Parola(Integer.toHexString(n * 32 + i * 128)));
        for (BigInteger v : valori) {
            sb.append(Trans_XLayer.Parola("1")).append(Trans_XLayer.Parola("40")).append(Trans_XLayer.Parola("20")).append(Trans_XLayer.Parola(v.toString(16)));
        }
        return sb.toString();
    }

    static String StringaAbi(String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder h = new StringBuilder();
        for (byte x : b) h.append(String.format("%02x", x));
        String dati = h + "0".repeat((64 - h.length() % 64) % 64);
        return "0x" + Trans_XLayer.Parola("20") + Trans_XLayer.Parola(Integer.toHexString(b.length)) + dati;
    }

    static NodoPubblicoDefi.Esito Scansiona(Catena c, Set<String> token) {
        return new NodoPubblicoDefi.Scansione(CONF, c, W, null, null, 0).Esegui(1, token);
    }

    static List<JSONObject> Righe(JSONArray a, String hash) {
        List<JSONObject> r = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) if (a.getJSONObject(i).getString("hash").equals(hash)) r.add(a.getJSONObject(i));
        return r;
    }

    //=====================================================================================================
    //=== TEST
    //=====================================================================================================

    @Test
    void trovaTuttiIBlocchiCambiatiERicercaITokenScoperti() {
        Catena c = new Catena(100_000);
        //1) invio di 1 ETH, con la quota L1 della commissione
        Tx invio = c.tx(1_000, "0xinvio", W, X);
        invio.value = ETH;
        invio.l1 = BigInteger.valueOf(777);
        //2) USDC ricevuti senza fare nulla (token seguito)
        Tx usdc = c.tx(20_000, "0xusdc", X, ROUTER);
        usdc.logs.add(Log(USDC, NodoPubblicoDefi.TOPIC_TRANSFER, X, W, BigInteger.valueOf(5_000_000), "0xusdc"));
        c.saldo(USDC, 20_000, BigInteger.valueOf(5_000_000));
        //3) scambio USDC -> NUOVO (token non seguito: lo si scopre dai log)
        Tx scambio = c.tx(50_000, "0xscambio", W, ROUTER);
        scambio.logs.add(Log(USDC, NodoPubblicoDefi.TOPIC_TRANSFER, W, ROUTER, BigInteger.valueOf(5_000_000), "0xscambio"));
        scambio.logs.add(Log(NUOVO, NodoPubblicoDefi.TOPIC_TRANSFER, ROUTER, W, BigInteger.valueOf(42), "0xscambio"));
        c.saldo(USDC, 50_000, BigInteger.ZERO);
        c.saldo(NUOVO, 50_000, BigInteger.valueOf(42));
        //4) NUOVO ricevuto prima dello scambio, senza fare nulla: lo trova solo la seconda ricerca
        Tx airdrop = c.tx(30_000, "0xairdrop", X, NUOVO);
        airdrop.logs.add(Log(NUOVO, NodoPubblicoDefi.TOPIC_TRANSFER, X, W, BigInteger.valueOf(8), "0xairdrop"));
        c.saldo(NUOVO, 30_000, BigInteger.valueOf(8));
        c.saldo(NUOVO, 50_000, BigInteger.valueOf(50));
        scambio.logs.set(1, Log(NUOVO, NodoPubblicoDefi.TOPIC_TRANSFER, ROUTER, W, BigInteger.valueOf(42), "0xscambio"));

        NodoPubblicoDefi.Esito e = Scansiona(c, new LinkedHashSet<>(CONF.tokenNoti()));

        assertTrue(e.completo(), String.join("\n", e.avvisi()));
        assertEquals(4, e.blocchi());
        List<JSONObject> tx = Righe(e.txlist(), "0xinvio");
        assertEquals(1, tx.size());
        assertEquals(ETH.toString(), tx.get(0).getString("value"));
        assertEquals("0", tx.get(0).getString("isError"));
        assertEquals(BigInteger.valueOf(21000L * 1_000_000_000L + 777).toString(), tx.get(0).getString("commissioneWei"),
                "la commissione comprende la quota L1");
        assertEquals("1700002000", tx.get(0).getString("timeStamp"));
        assertTrue(Righe(e.txlist(), "0xaltra1000").isEmpty(), "le transazioni d'altri nel blocco non entrano");
        assertEquals(1, Righe(e.tokentx(), "0xusdc").size());
        assertEquals(2, Righe(e.tokentx(), "0xscambio").size());
        List<JSONObject> air = Righe(e.tokentx(), "0xairdrop");
        assertEquals(1, air.size(), "il token scoperto nello scambio va cercato anche prima");
        assertEquals("Claim on: esempio.com", air.get(0).getString("tokenSymbol"), "il nome resta com'e', SCAM compresi");
        assertEquals("12", air.get(0).getString("tokenDecimal"));
        assertEquals(0, e.txlistinternal().length());
    }

    @Test
    void lEthInternoDiUnaSolaTransazioneSiRicavaDalSaldo() {
        Catena c = new Catena(10_000);
        Tx t = c.tx(4_000, "0xsvolgi", W, WETH);
        t.interno = ETH.divide(BigInteger.TWO);
        t.logs.add(Log(WETH, NodoPubblicoDefi.TOPIC_PRELIEVO_AVVOLTO, W, null, ETH.divide(BigInteger.TWO), "0xsvolgi"));
        c.saldo(WETH, 0, ETH);
        c.saldo(WETH, 4_000, ETH.divide(BigInteger.TWO));

        NodoPubblicoDefi.Esito e = Scansiona(c, new LinkedHashSet<>(CONF.tokenNoti()));

        assertTrue(e.completo(), String.join("\n", e.avvisi()));
        List<JSONObject> interne = Righe(e.txlistinternal(), "0xsvolgi");
        assertEquals(1, interne.size());
        assertEquals(ETH.divide(BigInteger.TWO).toString(), interne.get(0).getString("value"));
        assertEquals(W, interne.get(0).getString("to"));
        assertEquals(WETH, interne.get(0).getString("from"));
        List<JSONObject> weth = Righe(e.tokentx(), "0xsvolgi");
        assertEquals(1, weth.size(), "Withdrawal del token avvolto come trasferimento in uscita");
        assertEquals(W, weth.get(0).getString("from"));
        assertTrue(e.avvisi().isEmpty(), "WETH non e' un token a saldo variabile: " + e.avvisi());
    }

    @Test
    void unTokenASaldoVariabileEsceDalloStatoSenzaScendereAOgniBlocco() {
        Catena c = new Catena(200_000);
        c.ribasanti.put(RIBASA, 150_000L);
        Tx t = c.tx(100_000, "0xinvio", W, X);
        t.value = BigInteger.ONE;
        Set<String> token = new LinkedHashSet<>(CONF.tokenNoti());
        token.add(RIBASA);

        NodoPubblicoDefi.Esito e = Scansiona(c, token);

        assertTrue(e.completo());
        assertEquals(2, e.blocchi(), "il blocco dell'invio e il primo cambio del token, non i 50.000 seguenti");
        assertTrue(e.avvisi().stream().anyMatch(a -> a.contains(RIBASA)), e.avvisi().toString());
        assertEquals(1, e.txlist().length());
    }

    @Test
    void unaTransazioneFallitaPagaSoloLaCommissione() {
        Catena c = new Catena(10_000);
        Tx t = c.tx(5_000, "0xfallita", W, ROUTER);
        t.value = ETH;
        t.ok = false;

        NodoPubblicoDefi.Esito e = Scansiona(c, new LinkedHashSet<>(CONF.tokenNoti()));

        assertTrue(e.completo(), String.join("\n", e.avvisi()));
        JSONObject r = Righe(e.txlist(), "0xfallita").get(0);
        assertEquals("1", r.getString("isError"));
        assertEquals(0, e.txlistinternal().length(), "la commissione spiega tutto il cambio di saldo");
        assertTrue(e.avvisi().isEmpty(), e.avvisi().toString());
    }

    @Test
    void unWalletConCodiceVieneRifiutato() {
        Catena c = new Catena(10_000);
        c.codice = "0xef0100" + "11".repeat(20);
        NodoPubblicoDefi.Esito e = Scansiona(c, new LinkedHashSet<>(CONF.tokenNoti()));
        assertFalse(e.completo());
        assertTrue(e.avvisi().get(0).contains("codice"));
    }

    @Test
    void unGuastoDeiNodiNonRestituisceUnaParte() {
        Catena c = new Catena(10_000);
        c.guasto = true;
        NodoPubblicoDefi.Esito e = Scansiona(c, new LinkedHashSet<>(CONF.tokenNoti()));
        assertFalse(e.completo());
    }

    @Test
    void laCommissioneComprendeQuotaL1EQuotaOperatore() {
        JsonObject r = new JsonObject();
        r.addProperty("gasUsed", "0x5208");               //21000
        r.addProperty("effectiveGasPrice", "0x3b9aca00");  //1 gwei
        r.addProperty("l1Fee", "0x64");                   //100
        assertEquals(BigInteger.valueOf(21_000_000_000_100L), NodoPubblicoDefi.Scansione.Commissione(r));
        r.addProperty("operatorFeeScalar", "0xf4240");     //1e6: un wei per gas
        r.addProperty("operatorFeeConstant", "0x7");
        assertEquals(BigInteger.valueOf(21_000_000_000_100L + 21_000 + 7), NodoPubblicoDefi.Scansione.Commissione(r));
    }

    @Test
    void leStringheAbiRestanoComeSono() {
        assertEquals("Claim on: esempio.com", NodoPubblicoDefi.Stringa(StringaAbi("Claim on: esempio.com")));
        assertEquals("MKR", NodoPubblicoDefi.Stringa("0x4d4b520000000000000000000000000000000000000000000000000000000000"),
                "bytes32 fino al primo zero");
        assertEquals("", NodoPubblicoDefi.Stringa("0x"));
    }

    @Test
    void ilLottoErc1155DaUnaCoppiaPerId() {
        //ids [1, 2], values [10, 20]
        String dati = "0x" + Trans_XLayer.Parola("40") + Trans_XLayer.Parola("a0")
                + Trans_XLayer.Parola("2") + Trans_XLayer.Parola("1") + Trans_XLayer.Parola("2")
                + Trans_XLayer.Parola("2") + Trans_XLayer.Parola("a") + Trans_XLayer.Parola("14");
        List<BigInteger[]> l = NodoPubblicoDefi.Lotto1155(dati);
        assertEquals(2, l.size());
        assertEquals(BigInteger.TWO, l.get(1)[0]);
        assertEquals(BigInteger.valueOf(20), l.get(1)[1]);
    }
}
