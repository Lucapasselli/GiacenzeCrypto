/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

/**
 *
 * @author lucap
 */
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * BinanceTaxReportClient - versione completa con discovery simboli spot
 * - Endpoint corretti per Trades ed Earn
 * - Paginazione robusta (offset, fromId, finestre temporali)
 * - Intervallo di default: tutto lo storico (dal 2017-01-01)
 * - Normalizzazione Earn + export CSV
 * - Discovery dei simboli Spot da depositi/prelievi/earn
 */
public class BinanceTaxReportClient {

    private static final String BASE_URL = "https://api.binance.com";

    private final String apiKey;
    private final String secretKey;

    private long timeOffset = 0L;           // serverTime - localTime
    private long defaultRecvWindow = 10000; // 10s

    // “Tutto lo storico”: partenza 2017-01-01 UTC
    private static final long EPOCH_ALL_HISTORY = ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant().toEpochMilli();

    // Quote comuni (usate come controparti): le usiamo per assemblare simboli, ma NON come "core asset"
    private static final Set<String> COMMON_QUOTES = new HashSet<>(Arrays.asList(
            "USDT","FDUSD","BUSD","USDC","TUSD","DAI",
            "BTC","BNB","ETH",
            "EUR","TRY","GBP","AUD","BRL","RUB","UAH"
    ));

    // Quote preferite per costruire i simboli candidati
    private static final List<String> PREFERRED_QUOTES = Arrays.asList(
            "USDT","FDUSD","USDC","BUSD","BTC","BNB","ETH","EUR","TRY"
    );

    public BinanceTaxReportClient(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    // ------------------ Utilità Tempo & Firma ------------------

    public void syncTime() throws Exception {
        String resp = httpGet(BASE_URL + "/api/v3/time", null, false);
        JSONObject json = new JSONObject(resp);
        long serverTime = json.getLong("serverTime");
        timeOffset = serverTime - System.currentTimeMillis();
        System.out.println("[LOG] Time offset (server-local): " + timeOffset + " ms");
    }

    public boolean checkAuthentication() {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("timestamp", String.valueOf(nowWithOffset()));
            params.put("recvWindow", String.valueOf(defaultRecvWindow));
            String query = buildQuery(params);
            String signature = hmacSha256(secretKey, query);
            String urlStr = BASE_URL + "/api/v3/account?" + query + "&signature=" + signature;

            HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-MBX-APIKEY", apiKey);
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);

            int code = con.getResponseCode();
            String response = readResponse(con, code);
            if (code == 200) {
                System.out.println("[LOG] Autenticazione OK");
                return true;
            } else {
                System.err.println("[LOG] Errore autenticazione: " + code + " - " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[LOG] checkAuthentication error: " + e.getMessage());
            return false;
        }
    }

    private long nowWithOffset() {
        return System.currentTimeMillis() + timeOffset;
    }

    private static String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(2 * rawHmac.length);
        for (byte b : rawHmac) hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
    }

    // ------------------ HTTP helpers ------------------

    private static String buildQuery(Map<String, String> params) throws Exception {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(e.getKey()).append("=")
              .append(URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8.name()));
        }
        return sb.toString();
    }

    private String httpGet(String urlStr, Map<String, String> headers, boolean withApiKey) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(20000);
        con.setReadTimeout(60000);
        if (withApiKey) con.setRequestProperty("X-MBX-APIKEY", apiKey);
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                con.setRequestProperty(h.getKey(), h.getValue());
            }
        }
        int code = con.getResponseCode();
        String response = readResponse(con, code);
        if (code != 200) {
            throw new RuntimeException("HTTP " + code + " - " + response);
        }
        return response;
    }

    private static String readResponse(HttpURLConnection con, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? con.getInputStream() : con.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) out.append(line);
            return out.toString();
        }
    }

    // Chiamata GET firmata (SAPI e API) con timestamp+recvWindow
    private String signedGet(String endpoint, Map<String, String> params) throws Exception {
        Map<String, String> p = new LinkedHashMap<>();
        if (params != null) p.putAll(params);
        p.put("recvWindow", String.valueOf(defaultRecvWindow));
        p.put("timestamp", String.valueOf(nowWithOffset()));

        String query = buildQuery(p);
        String signature = hmacSha256(secretKey, query);
        String urlStr = BASE_URL + endpoint + "?" + query + "&signature=" + signature;

        return httpGet(urlStr, null, true);
    }

    // ------------------ Parser generico ------------------

    private static class ParsedResponse {
        final JSONArray items;
        final JSONObject root;
        ParsedResponse(JSONArray items, JSONObject root) { this.items = items; this.root = root; }
    }

    private ParsedResponse parseToArray(String response, String preferredArrayField) {
        if (response == null || response.isBlank()) return new ParsedResponse(new JSONArray(), null);
        String t = response.trim();
        if (t.startsWith("[")) return new ParsedResponse(new JSONArray(t), null);

        JSONObject obj = new JSONObject(t);
        if (preferredArrayField != null && obj.has(preferredArrayField) && obj.get(preferredArrayField) instanceof JSONArray) {
            return new ParsedResponse(obj.getJSONArray(preferredArrayField), obj);
        }
        if (obj.has("rows") && obj.get("rows") instanceof JSONArray) return new ParsedResponse(obj.getJSONArray("rows"), obj);
        if (obj.has("data") && obj.get("data") instanceof JSONArray) return new ParsedResponse(obj.getJSONArray("data"), obj);
        if (obj.has("list") && obj.get("list") instanceof JSONArray) return new ParsedResponse(obj.getJSONArray("list"), obj);

        return new ParsedResponse(new JSONArray(), obj);
    }

    // Timestamp extractor resiliente
    private long getItemTimestamp(JSONObject item) {
        String[] keys = new String[] {
                "time", "timestamp", "insertTime", "operateTime", "updateTime",
                "divTime", "interestAccruedTime", "distributeTime", "payTime", "calcTime"
        };
        for (String k : keys) {
            if (item.has(k)) {
                try { return item.getLong(k); } catch (Exception ignored) {}
            }
        }
        return 0L;
    }

    private static String firstNonNullString(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k)) {
                Object v = obj.get(k);
                if (v == null) continue;
                if (v instanceof String) {
                    String s = (String) v;
                    if (!s.isEmpty()) return s;
                } else {
                    return String.valueOf(v);
                }
            }
        }
        return null;
    }

    // ------------------ Depositi / Prelievi (tutto lo storico) ------------------

    // /sapi/v1/capital/deposit/hisrec -> ARRAY root, supporta limit e offset
    public JSONArray fetchDepositsAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Depositi (ALL)...");
        final int LIMIT = 1000;
        int offset = 0;
        JSONArray all = new JSONArray();
        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("limit", String.valueOf(LIMIT));
            p.put("offset", String.valueOf(offset));
            String resp = signedGet("/sapi/v1/capital/deposit/hisrec", p);
            ParsedResponse pr = parseToArray(resp, null); // array root
            JSONArray page = pr.items;
            for (int i = 0; i < page.length(); i++) all.put(page.get(i));
            if (page.length() < LIMIT) break;
            offset += LIMIT;
        }
        return all;
    }

    // /sapi/v1/capital/withdraw/history -> ARRAY root, supporta limit e offset
    public JSONArray fetchWithdrawalsAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Prelievi (ALL)...");
        final int LIMIT = 1000;
        int offset = 0;
        JSONArray all = new JSONArray();
        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("limit", String.valueOf(LIMIT));
            p.put("offset", String.valueOf(offset));
            String resp = signedGet("/sapi/v1/capital/withdraw/history", p);
            ParsedResponse pr = parseToArray(resp, null); // array root
            JSONArray page = pr.items;
            for (int i = 0; i < page.length(); i++) all.put(page.get(i));
            if (page.length() < LIMIT) break;
            offset += LIMIT;
        }
        return all;
    }

    // ------------------ Trades Spot (per simbolo, poi multi-simbolo) ------------------

    // /api/v3/myTrades -> symbol OBBLIGATORIO, paginazione via fromId
    public JSONArray fetchSpotTradesForSymbolAllHistory(String symbol) throws Exception {
        System.out.println("[LOG] Scaricando Trades Spot per " + symbol + " (ALL)...");
        final int LIMIT = 1000;
        Long fromId = null;
        JSONArray all = new JSONArray();
        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("symbol", symbol);
            p.put("limit", String.valueOf(LIMIT));
            if (fromId != null) p.put("fromId", String.valueOf(fromId));

            String resp = signedGet("/api/v3/myTrades", p);
            ParsedResponse pr = parseToArray(resp, null); // array root
            JSONArray arr = pr.items;
            if (arr.length() == 0) break;

            for (int i = 0; i < arr.length(); i++) all.put(arr.get(i));

            long lastId = arr.getJSONObject(arr.length() - 1).getLong("id");
            long nextFromId = lastId + 1;
            if (fromId != null && nextFromId <= fromId) break; // evita loop
            fromId = nextFromId;
            if (arr.length() < LIMIT) break;
        }
        return all;
    }

    public JSONArray fetchSpotTradesForSymbolsAllHistory(Collection<String> symbols) throws Exception {
        JSONArray all = new JSONArray();
        for (String s : symbols) {
            JSONArray part = fetchSpotTradesForSymbolAllHistory(s);
            for (int i = 0; i < part.length(); i++) {
                JSONObject o = part.getJSONObject(i);
                o.put("symbol", s);
                all.put(o);
            }
        }
        return all;
    }

    // ------------------ Earn: Asset Dividends ------------------

    // /sapi/v1/asset/assetDividend -> object { rows, total }
    public JSONArray fetchAssetDividendsAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Asset Dividends (ALL)...");
        final int LIMIT = 1000;
        Long cursor = EPOCH_ALL_HISTORY;
        JSONArray all = new JSONArray();

        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("limit", String.valueOf(LIMIT));
            if (cursor != null) p.put("startTime", String.valueOf(cursor));
            System.out.println("Risposta 1 : "+cursor);

            String resp = signedGet("/sapi/v1/asset/assetDividend", p);
            System.out.println("Risposta : "+resp);
            ParsedResponse pr = parseToArray(resp, "rows");
            JSONArray rows = pr.items;
            if (rows.length() == 0) break;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                o.put("sourceType", "ASSET_DIVIDEND");
                all.put(o);
            }

            long lastTs = getItemTimestamp(rows.getJSONObject(rows.length() - 1));
            if (lastTs <= (cursor == null ? 0 : cursor)) break;
            cursor = lastTs + 1;
            if (rows.length() < LIMIT) break;
        }
        return all;
    }

    // ------------------ Earn: Simple Earn (Flexible / Locked) ------------------

    // /sapi/v1/simple-earn/flexible/history/rewards
    public JSONArray fetchSimpleEarnFlexibleRewardsAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Simple Earn Flexible (ALL)...");
        final int LIMIT = 1000;
        Long cursor = EPOCH_ALL_HISTORY;
        JSONArray all = new JSONArray();

        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("limit", String.valueOf(LIMIT));
            if (cursor != null) p.put("startTime", String.valueOf(cursor));

            String resp = signedGet("/sapi/v1/simple-earn/flexible/history/rewards", p);
            ParsedResponse pr = parseToArray(resp, "rows");
            JSONArray rows = pr.items;
            if (rows.length() == 0) break;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                o.put("sourceType", "SIMPLE_EARN_FLEXIBLE");
                all.put(o);
            }

            long lastTs = getItemTimestamp(rows.getJSONObject(rows.length() - 1));
            if (lastTs <= (cursor == null ? 0 : cursor)) break;
            cursor = lastTs + 1;
            if (rows.length() < LIMIT) break;
        }
        return all;
    }

    // /sapi/v1/simple-earn/locked/history/rewards
    public JSONArray fetchSimpleEarnLockedRewardsAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Simple Earn Locked (ALL)...");
        final int LIMIT = 1000;
        Long cursor = EPOCH_ALL_HISTORY;
        JSONArray all = new JSONArray();

        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("limit", String.valueOf(LIMIT));
            if (cursor != null) p.put("startTime", String.valueOf(cursor));

            String resp = signedGet("/sapi/v1/simple-earn/locked/history/rewards", p);
            ParsedResponse pr = parseToArray(resp, "rows");
            JSONArray rows = pr.items;
            if (rows.length() == 0) break;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                o.put("sourceType", "SIMPLE_EARN_LOCKED");
                all.put(o);
            }

            long lastTs = getItemTimestamp(rows.getJSONObject(rows.length() - 1));
            if (lastTs <= (cursor == null ? 0 : cursor)) break;
            cursor = lastTs + 1;
            if (rows.length() < LIMIT) break;
        }
        return all;
    }

    // ------------------ Earn: Staking / DeFi Staking ------------------

    // /sapi/v1/staking/record con txnType=INTEREST e product = STAKING | L_DEFI | F_DEFI
    public JSONArray fetchStakingInterestsAllHistory(String product) throws Exception {
        System.out.println("[LOG] Scaricando Staking Interests product=" + product + " (ALL)...");
        final int LIMIT = 1000;
        Long cursor = EPOCH_ALL_HISTORY;
        JSONArray all = new JSONArray();

        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("txnType", "INTEREST");
            p.put("product", product);
            p.put("limit", String.valueOf(LIMIT));
            if (cursor != null) p.put("startTime", String.valueOf(cursor));

            String resp = signedGet("/sapi/v1/staking/record", p);
            ParsedResponse pr = parseToArray(resp, "rows");
            JSONArray rows = pr.items;
            if (rows.length() == 0) break;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                o.put("sourceType", product.equals("STAKING") ? "STAKING_INTEREST"
                        : (product.equals("L_DEFI") ? "DEFI_LOCKED_INTEREST" : "DEFI_FLEX_INTEREST"));
                all.put(o);
            }

            long lastTs = getItemTimestamp(rows.getJSONObject(rows.length() - 1));
            if (lastTs <= (cursor == null ? 0 : cursor)) break;
            cursor = lastTs + 1;
            if (rows.length() < LIMIT) break;
        }
        return all;
    }

    // ------------------ Earn: Legacy Savings (opzionale) ------------------

    // /sapi/v1/lending/union/interestHistory -> spesso supporta size e current (pagina)
    public JSONArray fetchLegacySavingsInterestAllHistory() throws Exception {
        System.out.println("[LOG] Scaricando Legacy Savings Interest (ALL)...");
        final int SIZE = 1000;
        int current = 1;
        JSONArray all = new JSONArray();

        while (true) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("size", String.valueOf(SIZE));
            p.put("current", String.valueOf(current));

            String resp = signedGet("/sapi/v1/lending/union/interestHistory", p);
            ParsedResponse pr = parseToArray(resp, "rows");
            JSONArray rows = pr.items;
            if (rows.length() == 0) break;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                o.put("sourceType", "LEGACY_SAVINGS_INTEREST");
                all.put(o);
            }

            if (rows.length() < SIZE) break;
            current++;
        }
        return all;
    }

    // ------------------ Aggregazione: TUTTI GLI EARN ------------------

    public JSONArray fetchAllEarnAllHistory() throws Exception {
        JSONArray all = new JSONArray();

        // 1) Asset dividends
        append(all, fetchAssetDividendsAllHistory());

        // 2) Simple Earn
        append(all, fetchSimpleEarnFlexibleRewardsAllHistory());
        append(all, fetchSimpleEarnLockedRewardsAllHistory());

        // 3) Staking / DeFi Staking
        append(all, fetchStakingInterestsAllHistory("STAKING"));
        append(all, fetchStakingInterestsAllHistory("L_DEFI"));
        append(all, fetchStakingInterestsAllHistory("F_DEFI"));

        // 4) Legacy Savings (se presente storico)
        append(all, fetchLegacySavingsInterestAllHistory());

        return all;
    }

    private void append(JSONArray target, JSONArray src) {
        for (int i = 0; i < src.length(); i++) target.put(src.get(i));
    }

    // ------------------ Normalizzazione ed Export ------------------

    // Normalizza record Earn eterogenei ad uno schema uniforme
    public JSONArray normalizeEarn(JSONArray earnItems) {
        JSONArray out = new JSONArray();
        for (int i = 0; i < earnItems.length(); i++) {
            JSONObject raw = earnItems.getJSONObject(i);

            long ts = getItemTimestamp(raw);

            String asset = firstNonNullString(raw,
                    "asset", "coin", "token", "rewardAsset", "currency");
            if (asset == null) asset = "";

            String amountStr = firstNonNullString(raw,
                    "amount", "interest", "dividendAmount", "rewardAmt", "income");
            double amount = 0d;
            if (amountStr != null) {
                try { amount = Double.parseDouble(amountStr); } catch (Exception ignored) {}
            } else {
                for (String k : new String[]{"amount", "interest", "income"}) {
                    if (raw.has(k)) {
                        try { amount = raw.getDouble(k); } catch (Exception ignored) {}
                    }
                }
            }

            String sourceType = raw.optString("sourceType", "");
            String product = firstNonNullString(raw, "product", "project", "type", "productType");
            String txId = firstNonNullString(raw, "tranId", "txId", "orderId", "subscriptionId", "distributionId");

            JSONObject norm = new JSONObject();
            norm.put("ts", ts);
            norm.put("asset", asset);
            norm.put("amount", amount);
            norm.put("sourceType", sourceType);
            norm.put("product", product != null ? product : "");
            norm.put("txId", txId != null ? txId : "");
            norm.put("raw", raw);

            out.put(norm);
        }
        return out;
    }

    public void exportEarnToCSV(JSONArray normalizedEarn, Appendable out) throws Exception {
        writeLine(out, "ts,datetime_utc,asset,amount,sourceType,product,txId");
        for (int i = 0; i < normalizedEarn.length(); i++) {
            JSONObject o = normalizedEarn.getJSONObject(i);
            long ts = o.optLong("ts", 0L);
            String dt = ts > 0 ? Instant.ofEpochMilli(ts).atZone(ZoneOffset.UTC).toString() : "";
            String line = String.join(",",
                    csv(o.optLong("ts", 0L)),
                    csv(dt),
                    csv(o.optString("asset", "")),
                    csv(o.opt("amount")),
                    csv(o.optString("sourceType", "")),
                    csv(o.optString("product", "")),
                    csv(o.optString("txId", ""))
            );
            writeLine(out, line);
        }
    }

    private static String csv(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    private static void writeLine(Appendable out, String line) throws Exception {
        if (out instanceof BufferedWriter) {
            BufferedWriter bw = (BufferedWriter) out;
            bw.write(line);
            bw.newLine();
            bw.flush();
        } else {
            out.append(line).append("\n");
        }
    }

    // ------------------ NEW: Discovery simboli Spot ------------------

    private static class SymbolInfo {
        String symbol;
        String baseAsset;
        String quoteAsset;
    }

    // Scarica i simboli Spot TRADING dall'exchangeInfo
    public List<SymbolInfo> fetchExchangeTradingSymbols() throws Exception {
        String resp = httpGet(BASE_URL + "/api/v3/exchangeInfo", null, false);
        JSONObject obj = new JSONObject(resp);
        JSONArray arr = obj.getJSONArray("symbols");
        List<SymbolInfo> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject s = arr.getJSONObject(i);
            String status = s.optString("status", "TRADING");
            boolean spotAllowed = s.optBoolean("isSpotTradingAllowed", true);
            if (!"TRADING".equals(status)) continue;
            if (!spotAllowed) continue;

            SymbolInfo si = new SymbolInfo();
            si.symbol = s.getString("symbol");
            si.baseAsset = s.getString("baseAsset");
            si.quoteAsset = s.getString("quoteAsset");
            out.add(si);
        }
        return out;
    }

    // Raccoglie asset da depositi, prelievi e TUTTI gli earn
    public Set<String> collectHistoryAssets() throws Exception {
        Set<String> assets = new HashSet<>();

        // Depositi
        try {
            JSONArray dep = fetchDepositsAllHistory();
            for (int i = 0; i < dep.length(); i++) {
                JSONObject o = dep.getJSONObject(i);
                String coin = o.optString("coin", null);
                if (coin != null && !coin.isEmpty()) assets.add(coin.toUpperCase(Locale.ROOT));
            }
        } catch (Exception e) {
            System.err.println("[WARN] Deposits scan failed: " + e.getMessage());
        }

        // Prelievi
        try {
            JSONArray w = fetchWithdrawalsAllHistory();
            for (int i = 0; i < w.length(); i++) {
                JSONObject o = w.getJSONObject(i);
                String coin = o.optString("coin", null);
                if (coin != null && !coin.isEmpty()) assets.add(coin.toUpperCase(Locale.ROOT));
            }
        } catch (Exception e) {
            System.err.println("[WARN] Withdrawals scan failed: " + e.getMessage());
        }

        // Earn (grezzo, non normalizzato, perché mantiene campi originali)
        try {
            JSONArray earn = fetchAllEarnAllHistory();
            for (int i = 0; i < earn.length(); i++) {
                JSONObject o = earn.getJSONObject(i);
                String a = firstNonNullString(o, "asset","coin","token","rewardAsset","currency");
                if (a != null && !a.isEmpty()) assets.add(a.toUpperCase(Locale.ROOT));
            }
        } catch (Exception e) {
            System.err.println("[WARN] Earn scan failed: " + e.getMessage());
        }

        return assets;
    }

    // Determina simboli spot probabili dai tuoi asset storici
    public Set<String> discoverLikelySymbolsFromHistory() throws Exception {
        System.out.println("[LOG] Discovery simboli spot dai tuoi asset storici...");
        Set<String> seenAssets = collectHistoryAssets();

        if (seenAssets.isEmpty()) {
            System.out.println("[LOG] Nessun asset storico rilevato.");
            return Collections.emptySet();
        }

        // "Core" = asset che NON sono quote comuni (per usarli come baseAsset)
        Set<String> coreAssets = new HashSet<>();
        // Note: teniamo comunque traccia delle quote viste (se servisse una logica aggiuntiva in futuro)
        Set<String> quotesSeen = new HashSet<>();

        for (String a : seenAssets) {
            if (COMMON_QUOTES.contains(a)) quotesSeen.add(a);
            else coreAssets.add(a);
        }

        // Fallback: se non rimane nulla (es. hai solo USDT), considera comunque USDT come core
        if (coreAssets.isEmpty() && !quotesSeen.isEmpty()) {
            // in questo caso, limitiamo la ricerca a simboli "classici" contro USDT/FDUSD
            coreAssets.addAll(quotesSeen); // poca scelta: useremo come base anche le quote
        }

        List<SymbolInfo> all = fetchExchangeTradingSymbols();

        // Indicizza per base e per quote
        Map<String, List<SymbolInfo>> byBase = new HashMap<>();
        Map<String, List<SymbolInfo>> byQuote = new HashMap<>();
        for (SymbolInfo si : all) {
            byBase.computeIfAbsent(si.baseAsset, k -> new ArrayList<>()).add(si);
            byQuote.computeIfAbsent(si.quoteAsset, k -> new ArrayList<>()).add(si);
        }

        Set<String> result = new HashSet<>();

        // 1) Per ogni asset "core", aggiungi coppie con quote preferite
        for (String base : coreAssets) {
            List<SymbolInfo> candidates = byBase.getOrDefault(base, Collections.emptyList());
            for (SymbolInfo si : candidates) {
                if (PREFERRED_QUOTES.contains(si.quoteAsset)) {
                    result.add(si.symbol);
                }
            }
        }

        // 2) Coppie tra asset "core" (es. ADA/BTC se entrambi visti)
        for (String base : coreAssets) {
            List<SymbolInfo> candidates = byBase.getOrDefault(base, Collections.emptyList());
            for (SymbolInfo si : candidates) {
                if (coreAssets.contains(si.quoteAsset)) {
                    result.add(si.symbol);
                }
            }
        }

        // Nota: eviterei di generare tutte le coppie dove il tuo asset è usato come quote,
        // perché esplode facilmente (es. USDT). La regola 1+2 sopra è un buon compromesso.

        System.out.println("[LOG] Simboli spot candidati: " + result.size());
        return result;
    }

    // Scarica i trades spot su tutti i simboli “probabili”
    public JSONArray fetchAllSpotTradesAllHistoryAuto() throws Exception {
        Set<String> symbols = discoverLikelySymbolsFromHistory();
        if (symbols.isEmpty()) {
            System.out.println("[LOG] Nessun simbolo candidato trovato.");
            return new JSONArray();
        }
        return fetchSpotTradesForSymbolsAllHistory(symbols);
    }

    // ------------------ Esempio d’uso ------------------

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("BINANCE_API_KEY");
        String secretKey = System.getenv("BINANCE_SECRET_KEY");

        BinanceTaxReportClient c = new BinanceTaxReportClient(apiKey, secretKey);

        c.syncTime();
        if (!c.checkAuthentication()) {
            System.err.println("API key/permessi non validi.");
            return;
        }

        // 1) TUTTI GLI EARN (ALL TIME) + normalizzazione + CSV su stdout
        JSONArray earn = c.fetchAllEarnAllHistory();
        System.out.println("[LOG] Earn totali: " + earn.length());
        JSONArray norm = c.normalizeEarn(earn);
        c.exportEarnToCSV(norm, new OutputStreamWriter(System.out));

        // 2) Discovery simboli e scarico trades spot completo
        Set<String> symbols = c.discoverLikelySymbolsFromHistory();
        System.out.println("[LOG] Simboli scoperti: " + symbols);
        JSONArray trades = c.fetchSpotTradesForSymbolsAllHistory(symbols);
        System.out.println("[LOG] Trades totali (auto): " + trades.length());
    }
}

