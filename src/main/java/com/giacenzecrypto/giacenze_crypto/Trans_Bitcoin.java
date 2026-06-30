/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

/**
 *
 * @author luca.passelli
 */
public class Trans_Bitcoin {

    private static final String MEMPOOL_API = "https://mempool.space/api";
    private static final String UNISAT_API  = "https://open-api.unisat.io";
    private static final int    UNISAT_PAGE = 100;

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // Numero massimo di indirizzi consecutivi non usati prima di fermare la scansione (BIP44 gap limit)
    static final int GAP_LIMIT = 20;
    static boolean verbose = false;

    // Version bytes delle chiavi estese Bitcoin mainnet
    private static final byte[] XPUB_BYTES = {0x04, (byte) 0x88, (byte) 0xB2, (byte) 0x1E};
    private static final byte[] YPUB_BYTES = {0x04, (byte) 0x9D, (byte) 0x7C, (byte) 0xB2};
    private static final byte[] ZPUB_BYTES = {0x04, (byte) 0xB2, (byte) 0x47, (byte) 0x46};

    enum ExtKeyType {
        XPUB,  // P2PKH - indirizzi legacy (1xxx)
        YPUB,  // P2SH-P2WPKH - indirizzi SegWit compatibili (3xxx)
        ZPUB   // P2WPKH - indirizzi native SegWit (bc1q...)
    }

    /**
     * Ritorna true se la stringa è una chiave pubblica estesa (xpub/ypub/zpub/tpub).
     */
    public static boolean isExtendedKey(String key) {
        if (key == null) return false;
        String k = key.trim().toLowerCase();
        return k.startsWith("xpub") || k.startsWith("ypub") || k.startsWith("zpub") || k.startsWith("tpub");
    }

    /**
     * Converte ypub/zpub/tpub a format xpub (stessi byte di dati, soli version bytes diversi)
     * in modo che bitcoinj possa deserializzarla.
     */
    private static String reencodeAsXpub(String extKey) throws Exception {
        // decodeChecked verifica il checksum e restituisce il payload senza checksum (78 bytes)
        byte[] payload = Base58.decodeChecked(extKey);
        // Sostituisci i primi 4 bytes con la versione xpub mainnet
        System.arraycopy(XPUB_BYTES, 0, payload, 0, 4);
        // Ricalcola e aggiungi il checksum (SHA256d dei 78 bytes)
        byte[] hash = Sha256Hash.hashTwice(payload);
        byte[] withChecksum = new byte[payload.length + 4];
        System.arraycopy(payload, 0, withChecksum, 0, payload.length);
        System.arraycopy(hash, 0, withChecksum, payload.length, 4);
        return Base58.encode(withChecksum);
    }

    private static ExtKeyType detectKeyType(String extKey) {
        String lower = extKey.trim().toLowerCase();
        if (lower.startsWith("ypub")) return ExtKeyType.YPUB;
        if (lower.startsWith("zpub")) return ExtKeyType.ZPUB;
        return ExtKeyType.XPUB; // xpub o tpub
    }

    /**
     * Genera l'indirizzo Bitcoin dal chiave derivata, in base al tipo di chiave estesa.
     */
    private static String addressFromKey(DeterministicKey key, ExtKeyType type) {
        NetworkParameters params = MainNetParams.get();
        switch (type) {
            case YPUB: {
                // P2SH-P2WPKH (BIP49): P2WPKH output script wrapped in P2SH
                Script p2wpkhScript = ScriptBuilder.createP2WPKHOutputScript(key);
                byte[] scriptHash = Utils.sha256hash160(p2wpkhScript.getProgram());
                return LegacyAddress.fromScriptHash(params, scriptHash).toString();
            }
            case ZPUB:
                // P2WPKH native SegWit (BIP84)
                return SegwitAddress.fromKey(params, key).toString();
            default:
                // P2PKH legacy (BIP44)
                return LegacyAddress.fromKey(params, key).toString();
        }
    }

    /**
     * Deriva tutti gli indirizzi Bitcoin usati da una chiave estesa xpub/ypub/zpub.
     * Usa il gap limit BIP44 (GAP_LIMIT indirizzi consecutivi non usati = stop).
     * Scansiona sia la catena esterna (m/0/n) che quella di cambio (m/1/n).
     */
    public static Set<String> deriveUsedAddresses(String extKey, Download progressb) throws Exception {
        ExtKeyType keyType = detectKeyType(extKey);
        String xpubStr = (keyType == ExtKeyType.XPUB) ? extKey : reencodeAsXpub(extKey);

        NetworkParameters params = MainNetParams.get();
        DeterministicKey masterKey = DeterministicKey.deserializeB58(xpubStr, params);

        Set<String> usedAddresses = new LinkedHashSet<>();
        String keyPrefix = extKey.substring(0, Math.min(12, extKey.length()));

        System.out.println("[BTC] Inizio derivazione indirizzi da chiave " + keyPrefix + "... (tipo: " + keyType + ", gap limit: " + GAP_LIMIT + ")");

        for (int chain = 0; chain <= 1; chain++) {
            DeterministicKey chainKey = HDKeyDerivation.deriveChildKey(masterKey, chain);
            int consecutiveUnused = 0;
            String chainLabel = (chain == 0) ? "esterna" : "cambio";
            System.out.println("[BTC] Scansione catena " + chainLabel + " (m/" + chain + "/n)...");

            for (int idx = 0; consecutiveUnused < GAP_LIMIT; idx++) {
                if (progressb != null && progressb.FineThread()) break;

                DeterministicKey childKey = HDKeyDerivation.deriveChildKey(chainKey, idx);
                String address = addressFromKey(childKey, keyType);

                String chainLabelShort = (chain == 0) ? "ext" : "chg";
                if (progressb != null) {
                    progressb.SetLabel("[BTC] Scansione " + chainLabelShort + " #" + idx + ": " + abbreviate(address));
                }
                if (verbose) {
                    System.out.println("[BTC]   m/" + chain + "/" + idx + " -> " + address);
                }

                int txCount = getAddressTxCount(address);
                if (txCount > 0) {
                    usedAddresses.add(address);
                    consecutiveUnused = 0;
                    System.out.println("[BTC]   Trovato m/" + chain + "/" + idx + ": " + address + " (" + txCount + " tx)");
                } else {
                    consecutiveUnused++;
                    if (verbose) {
                        System.out.println("[BTC]   Vuoto  m/" + chain + "/" + idx + " (gap " + consecutiveUnused + "/" + GAP_LIMIT + ")");
                    }
                }
                Thread.sleep(120); // rispetta il rate limit di mempool.space
            }
            System.out.println("[BTC] Catena " + chainLabel + " completata — " + usedAddresses.size() + " indirizzi usati finora.");
        }
        System.out.println("[BTC] Derivazione completata: " + usedAddresses.size() + " indirizzi usati per " + keyPrefix + "...");
        return usedAddresses;
    }

    private static String abbreviate(String s) {
        if (s == null || s.length() <= 20) return s;
        return s.substring(0, 10) + "…" + s.substring(s.length() - 6);
    }

    /**
     * Interroga l'API mempool.space per ottenere il numero di transazioni di un indirizzo.
     */
    private static int getAddressTxCount(String address) {
        String url = MEMPOOL_API + "/address/" + address;
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String json = response.body().string();
                if (json != null && json.startsWith("{")) {
                    JSONObject obj = new JSONObject(json);
                    JSONObject stats = obj.optJSONObject("chain_stats");
                    if (stats != null) return stats.optInt("tx_count", 0);
                }
            }
        } catch (Exception e) {
            System.err.println("[BTC] Errore stats " + address + ": " + e.getMessage());
        }
        return 0;
    }

    /**
     * Scarica le transazioni confermate di un singolo indirizzo da mempool.space,
     * partendo dal blocco fromBlock in avanti (le transazioni vengono restituite
     * dalla più recente alla più vecchia, ci si ferma quando si trovano transazioni
     * precedenti a fromBlock).
     */
    private static List<JSONObject> fetchAddressTxs(String address, int fromBlock) throws IOException, InterruptedException {
        List<JSONObject> collected = new ArrayList<>();
        String lastTxId = null;
        boolean hasMore = true;

        while (hasMore) {
            String url = MEMPOOL_API + "/address/" + address + "/txs/chain";
            if (lastTxId != null) url += "/" + lastTxId;

            Request request = new Request.Builder().url(url).get().build();
            JSONArray page = null;
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String json = response.body().string();
                    if (Funzioni.isValidJSONArray(json)) {
                        page = new JSONArray(json);
                    }
                }
            }

            if (page == null || page.length() == 0) break;

            boolean reachedOld = false;
            for (int i = 0; i < page.length(); i++) {
                JSONObject tx = page.getJSONObject(i);
                JSONObject status = tx.optJSONObject("status");
                if (status == null || !status.optBoolean("confirmed", false)) continue;
                int blockHeight = status.optInt("block_height", 0);
                if (blockHeight < fromBlock) {
                    reachedOld = true;
                    break;
                }
                collected.add(tx);
            }

            if (reachedOld || page.length() < 25) {
                hasMore = false;
            } else {
                lastTxId = page.getJSONObject(page.length() - 1).optString("txid", null);
                if (lastTxId == null || lastTxId.isEmpty()) hasMore = false;
            }
            Thread.sleep(150);
        }
        return collected;
    }

    /**
     * Converte satoshi in BTC come BigDecimal.
     */
    private static BigDecimal satsToDecimal(long sats) {
        return BigDecimal.valueOf(sats).scaleByPowerOfTen(-8).stripTrailingZeros();
    }

    /**
     * Converte una singola transazione Bitcoin (JSON da mempool.space) in TransazioneDefi.
     * ourAddresses: tutti gli indirizzi appartenenti al wallet (derivati o singolo indirizzo).
     * walletEntry: chiave xpub o indirizzo singolo (senza il suffisso "(BTC)").
     */
    private static TransazioneDefi parseTransaction(JSONObject tx, String walletEntry, Set<String> ourAddresses) {
        String txid = tx.optString("txid", "N/A");
        JSONObject status = tx.optJSONObject("status");
        long blockTime = (status != null) ? status.optLong("block_time", 0) : 0;
        int blockHeight = (status != null) ? status.optInt("block_height", 0) : 0;

        boolean isCoinbase = false;
        long sentSats = 0;     // satoshi in uscita da nostri indirizzi (input)
        long receivedSats = 0; // satoshi in entrata su nostri indirizzi (output)
        String counterparty = "";

        JSONArray vin = tx.optJSONArray("vin");
        if (vin != null) {
            for (int i = 0; i < vin.length(); i++) {
                JSONObject inp = vin.getJSONObject(i);
                if (inp.optBoolean("is_coinbase", false)) {
                    isCoinbase = true;
                    continue;
                }
                JSONObject prevout = inp.optJSONObject("prevout");
                if (prevout != null) {
                    String addr = prevout.optString("scriptpubkey_address", "");
                    if (ourAddresses.contains(addr)) {
                        sentSats += prevout.optLong("value", 0);
                    }
                }
            }
        }

        JSONArray vout = tx.optJSONArray("vout");
        if (vout != null) {
            for (int i = 0; i < vout.length(); i++) {
                JSONObject out = vout.getJSONObject(i);
                String addr = out.optString("scriptpubkey_address", "");
                if (ourAddresses.contains(addr)) {
                    receivedSats += out.optLong("value", 0);
                } else if (counterparty.isEmpty() && !addr.isEmpty()) {
                    // Prima controparte esterna trovata negli output
                    counterparty = addr;
                }
            }
        }

        // Se non trovato negli output, cerca la controparte negli input
        if (counterparty.isEmpty() && vin != null) {
            for (int i = 0; i < vin.length(); i++) {
                JSONObject inp = vin.getJSONObject(i);
                if (inp.optBoolean("is_coinbase", false)) continue;
                JSONObject prevout = inp.optJSONObject("prevout");
                if (prevout != null) {
                    String addr = prevout.optString("scriptpubkey_address", "");
                    if (!addr.isEmpty() && !ourAddresses.contains(addr)) {
                        counterparty = addr;
                        break;
                    }
                }
            }
        }

        // Scarta le transazioni che non coinvolgono i nostri indirizzi
        if (receivedSats == 0 && sentSats == 0 && !isCoinbase) return null;

        long feeSats = tx.optLong("fee", 0);

        TransazioneDefi trans = new TransazioneDefi();
        trans.Rete = "BTC";
        trans.Wallet = walletEntry;
        trans.HashTransazione = txid;
        trans.Blocco = String.valueOf(blockHeight);
        trans.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(blockTime * 1000);
        trans.TimeStamp = String.valueOf(blockTime);
        trans.MonetaCommissioni = "BTC";
        trans.TransazioneOK = true;
        trans.TipoTransazione = isCoinbase ? "Coinbase" : "Transfer";

        if (isCoinbase) {
            // Ricompensa di mining: deposito BTC
            BigDecimal btcAmount = satsToDecimal(receivedSats);
            trans.InserisciMonete("BTC", "BTC", "BTC", counterparty, btcAmount.toPlainString(), "Crypto");

        } else if (sentSats > 0) {
            // Abbiamo speso degli input
            // Valore netto inviato all'esterno = speso - cambio ricevuto - commissione
            long actualSent = sentSats - receivedSats - feeSats;
            BigDecimal btcFee = satsToDecimal(feeSats);

            if (actualSent > 0) {
                // Pagamento a indirizzo esterno + commissione separata
                BigDecimal btcSent = satsToDecimal(actualSent);
                trans.InserisciMonete("BTC", "BTC", "BTC", counterparty, "-" + btcSent.toPlainString(), "Crypto");
                trans.QtaCommissioni = "-" + btcFee.toPlainString();
            } else {
                // Auto-consolidazione (solo fee, nessuna uscita netta all'esterno)
                trans.QtaCommissioni = "-" + btcFee.toPlainString();
            }
        } else {
            // Ricezione pura (sentSats == 0): deposito
            BigDecimal btcAmount = satsToDecimal(receivedSats);
            trans.InserisciMonete("BTC", "BTC", "BTC", counterparty, btcAmount.toPlainString(), "Crypto");
        }

        if (verbose) {
            System.out.println("[BTC] " + txid.substring(0, 12) + "... block=" + blockHeight
                    + " sent=" + sentSats + " received=" + receivedSats + " fee=" + feeSats);
        }

        return trans;
    }

    /**
     * Punto di ingresso principale: scarica e analizza tutte le transazioni Bitcoin
     * per un wallet (indirizzo singolo o chiave estesa xpub/ypub/zpub).
     *
     * @param walletEntry indirizzo singolo oppure chiave estesa (xpub/ypub/zpub)
     * @param fromBlock   blocco da cui partire (incluso); passare 1 per la prima importazione
     * @param ccc         componente Swing per dialoghi
     * @param progressb   finestra di avanzamento
     * @return mappa txKey -> TransazioneDefi
     */
    public static Map<String, TransazioneDefi> fetchAndParseTransactions(
            String walletEntry, int fromBlock, Component ccc, Download progressb) throws InterruptedException {

        Map<String, TransazioneDefi> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try {
            System.out.println("[BTC] === Importazione wallet: " + walletEntry + " (dal blocco " + fromBlock + ") ===");

            Set<String> addresses;
            if (isExtendedKey(walletEntry)) {
                progressb.SetLabel("[BTC] Derivazione indirizzi da chiave estesa...");
                addresses = deriveUsedAddresses(walletEntry, progressb);
            } else {
                addresses = new LinkedHashSet<>();
                addresses.add(walletEntry);
                System.out.println("[BTC] Indirizzo singolo: " + walletEntry);
            }

            if (addresses.isEmpty()) {
                System.out.println("[BTC] Nessun indirizzo trovato per " + walletEntry);
                return result;
            }

            System.out.println("[BTC] Totale indirizzi da scansionare: " + addresses.size());

            // Scarica tutte le transazioni (deduplicate per txid)
            Map<String, JSONObject> txById = new LinkedHashMap<>();
            int addrDone = 0;
            for (String addr : addresses) {
                if (progressb.FineThread()) return result;
                addrDone++;
                progressb.SetLabel("[BTC] Scaricamento " + addrDone + "/" + addresses.size()
                        + ": " + abbreviate(addr));
                System.out.println("[BTC] Scaricamento tx indirizzo " + addrDone + "/" + addresses.size() + ": " + addr);

                List<JSONObject> addrTxs = fetchAddressTxs(addr, fromBlock);
                System.out.println("[BTC]   -> " + addrTxs.size() + " transazioni trovate");
                for (JSONObject tx : addrTxs) {
                    String txid = tx.optString("txid");
                    txById.putIfAbsent(txid, tx);
                }
                Thread.sleep(120);
            }

            // Ordina le transazioni per altezza di blocco crescente
            List<JSONObject> sorted = new ArrayList<>(txById.values());
            sorted.sort(Comparator.comparingInt(tx -> {
                JSONObject s = ((JSONObject) tx).optJSONObject("status");
                return (s != null) ? s.optInt("block_height", 0) : 0;
            }));

            System.out.println("[BTC] " + sorted.size() + " transazioni uniche da elaborare (deduplicate)");
            progressb.SetLabel("[BTC] Elaborazione " + sorted.size() + " transazioni...");
            progressb.SetMassimo(sorted.size());
            int done = 0;
            int movimentiValidi = 0;

            for (JSONObject tx : sorted) {
                if (progressb.FineThread()) return result;
                TransazioneDefi trans = parseTransaction(tx, walletEntry, addresses);
                if (trans != null && !trans.isEmpty()) {
                    result.put(walletEntry + "." + trans.HashTransazione, trans);
                    movimentiValidi++;
                    if (verbose) {
                        System.out.println("[BTC]   + " + trans.DataOra + " " + trans.TipoTransazione);
                    }
                }
                progressb.SetAvanzamento(++done);
            }

            System.out.println("[BTC] === Completato: " + movimentiValidi + " movimenti importati su " + sorted.size() + " transazioni ===");

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            System.err.println("[BTC] Errore importazione: " + e.getMessage());
            LoggerGC.ScriviErrore(e);
        }

        return result;
    }

    // =========================================================================
    //  UNISAT API — BRC-20 e Runes
    // =========================================================================

    /**
     * Valida l'API key di UniSat con una chiamata di prova.
     */
    public static boolean isApiKeyValida(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return false;
        try {
            Request req = new Request.Builder()
                    .url(UNISAT_API + "/v1/indexer/blockchain/info")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();
            try (Response r = httpClient.newCall(req).execute()) {
                if (!r.isSuccessful()) return false;
                String body = r.body() != null ? r.body().string() : "";
                return new JSONObject(body).optInt("code", -1) == 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ottiene i ticker BRC-20 per cui un indirizzo ha avuto attività (saldo attuale).
     */
    private static List<String> getBRC20Tickers(String address, String apiKey)
            throws IOException, InterruptedException {
        List<String> tickers = new ArrayList<>();
        int cursor = 0;
        while (true) {
            String url = UNISAT_API + "/v1/indexer/address/" + address
                    + "/brc20/summary?cursor=" + cursor + "&size=100";
            Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + apiKey).build();
            try (Response r = httpClient.newCall(req).execute()) {
                if (!r.isSuccessful()) break;
                String body = r.body() != null ? r.body().string() : "";
                if (!body.startsWith("{")) break;
                JSONObject json = new JSONObject(body);
                if (json.optInt("code", -1) != 0) break;
                JSONObject data = json.optJSONObject("data");
                if (data == null) break;
                JSONArray detail = data.optJSONArray("detail");
                if (detail == null || detail.length() == 0) break;
                for (int i = 0; i < detail.length(); i++) {
                    String t = detail.getJSONObject(i).optString("ticker", "");
                    if (!t.isBlank()) tickers.add(t);
                }
                cursor += detail.length();
                if (cursor >= data.optInt("total", 0)) break;
            }
            Thread.sleep(200);
        }
        return tickers;
    }

    /**
     * Scarica la cronologia BRC-20 per un ticker su un indirizzo.
     * Tipi UniSat: 1=inscribe-mint, 2=inscribe-transfer (preparazione, skip),
     *              3=transfer (invio), 5=transfer-from (ricezione).
     */
    private static Map<String, TransazioneDefi> fetchBRC20HistoryForTicker(
            String address, String ticker, String apiKey, int fromBlock, String walletEntry)
            throws IOException, InterruptedException {

        Map<String, TransazioneDefi> result = new LinkedHashMap<>();
        int cursor = 0;
        boolean done = false;
        String encodedTicker;
        try { encodedTicker = java.net.URLEncoder.encode(ticker, "UTF-8"); }
        catch (Exception e) { encodedTicker = ticker; }

        while (!done) {
            String url = UNISAT_API + "/v1/indexer/address/" + address
                    + "/brc20/" + encodedTicker + "/history?cursor=" + cursor + "&size=" + UNISAT_PAGE;
            Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + apiKey).build();
            try (Response r = httpClient.newCall(req).execute()) {
                if (!r.isSuccessful()) break;
                String body = r.body() != null ? r.body().string() : "";
                if (!body.startsWith("{")) break;
                JSONObject json = new JSONObject(body);
                if (json.optInt("code", -1) != 0) break;
                JSONObject data = json.optJSONObject("data");
                if (data == null) break;
                JSONArray detail = data.optJSONArray("detail");
                if (detail == null || detail.length() == 0) break;

                for (int i = 0; i < detail.length(); i++) {
                    JSONObject item = detail.getJSONObject(i);
                    int blockHeight = item.optInt("height", item.optInt("blockHeight", 0));
                    if (blockHeight > 0 && blockHeight <= fromBlock) { done = true; break; }
                    if (!item.optBoolean("valid", true)) continue;

                    int type = item.optInt("type", -1);
                    if (type == 2 || type == 0 || type == -1) continue; // skip deploy e preparazione

                    String txid = item.optString("txid", "");
                    if (txid.isBlank()) continue;
                    String amountStr = item.optString("amount", "0");
                    if (amountStr.isBlank() || amountStr.equals("0")) continue;

                    long timestamp = item.optLong("timestamp", item.optLong("time", 0));
                    String from = item.optString("from", "");
                    String to   = item.optString("to", "");

                    boolean isMint    = (type == 1);
                    boolean isReceive = isMint || (type == 5) || address.equalsIgnoreCase(to);
                    boolean isSend    = (type == 3) || (!isReceive && address.equalsIgnoreCase(from));
                    if (!isReceive && !isSend) continue;

                    String counterparty = isReceive ? from : to;
                    String qty = isSend ? "-" + amountStr : amountStr;
                    String tickerUp = ticker.toUpperCase();

                    TransazioneDefi trans = new TransazioneDefi();
                    trans.Wallet          = walletEntry;
                    trans.HashTransazione = txid;
                    trans.Rete            = "BTC";
                    trans.TransazioneOK   = true;
                    trans.TipoTransazione = isMint ? "Mint" : "Transfer";
                    trans.Blocco          = String.valueOf(blockHeight);
                    trans.DataOra         = timestamp > 0
                            ? FunzioniDate.ConvertiDatadaLongAlSecondo(timestamp * 1000L) : "";
                    trans.TimeStamp       = String.valueOf(timestamp);
                    trans.InserisciMonete(tickerUp, tickerUp, ticker.toLowerCase(), counterparty, qty, "Crypto");

                    result.put(walletEntry + "." + txid + "_brc20_" + ticker.toLowerCase(), trans);
                }

                cursor += detail.length();
                if (cursor >= data.optInt("total", 0) || detail.length() == 0) done = true;
            }
            Thread.sleep(200);
        }
        return result;
    }

    /**
     * Ottiene la lista dei Rune ID con saldo corrente per un indirizzo.
     */
    private static List<JSONObject> getRunesBalances(String address, String apiKey)
            throws IOException, InterruptedException {
        List<JSONObject> runes = new ArrayList<>();
        int cursor = 0;
        while (true) {
            String url = UNISAT_API + "/v1/indexer/address/" + address
                    + "/runes/balance-list?cursor=" + cursor + "&size=100";
            Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + apiKey).build();
            try (Response r = httpClient.newCall(req).execute()) {
                if (!r.isSuccessful()) break;
                String body = r.body() != null ? r.body().string() : "";
                if (!body.startsWith("{")) break;
                JSONObject json = new JSONObject(body);
                if (json.optInt("code", -1) != 0) break;
                JSONObject data = json.optJSONObject("data");
                if (data == null) break;
                JSONArray detail = data.optJSONArray("detail");
                if (detail == null || detail.length() == 0) break;
                for (int i = 0; i < detail.length(); i++) runes.add(detail.getJSONObject(i));
                cursor += detail.length();
                if (cursor >= data.optInt("total", 0)) break;
            }
            Thread.sleep(200);
        }
        return runes;
    }

    /**
     * Scarica la cronologia di uno specifico Rune su un indirizzo.
     * Tipi UniSat: "mint"=entrata da mint, "send"=uscita, "receive"=entrata, "etch"=skip.
     */
    private static Map<String, TransazioneDefi> fetchRunesHistoryForRune(
            String address, String runeid, String runeName, int divisibility,
            String apiKey, int fromBlock, String walletEntry)
            throws IOException, InterruptedException {

        Map<String, TransazioneDefi> result = new LinkedHashMap<>();
        int cursor = 0;
        boolean done = false;
        String encodedRuneid;
        try { encodedRuneid = java.net.URLEncoder.encode(runeid, "UTF-8"); }
        catch (Exception e) { encodedRuneid = runeid.replace(":", "%3A"); }

        // Simbolo senza spazi/bullet per il campo Moneta
        String runeSymbol = runeName.replace("•", "").replace(" ", "");

        while (!done) {
            String url = UNISAT_API + "/v1/indexer/address/" + address
                    + "/runes/" + encodedRuneid + "/history?cursor=" + cursor + "&size=" + UNISAT_PAGE;
            Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + apiKey).build();
            try (Response r = httpClient.newCall(req).execute()) {
                if (!r.isSuccessful()) break;
                String body = r.body() != null ? r.body().string() : "";
                if (!body.startsWith("{")) break;
                JSONObject json = new JSONObject(body);
                if (json.optInt("code", -1) != 0) break;
                JSONObject data = json.optJSONObject("data");
                if (data == null) break;
                JSONArray detail = data.optJSONArray("detail");
                if (detail == null || detail.length() == 0) break;

                for (int i = 0; i < detail.length(); i++) {
                    JSONObject item = detail.getJSONObject(i);
                    int blockHeight = item.optInt("height", item.optInt("blockHeight", 0));
                    if (blockHeight > 0 && blockHeight <= fromBlock) { done = true; break; }

                    String txid = item.optString("txid", "");
                    if (txid.isBlank()) continue;
                    String type = item.optString("type", "").toLowerCase();
                    if (type.equals("etch") || type.isBlank()) continue;

                    String amountStr = item.optString("amount", "0");
                    if (amountStr.isBlank() || amountStr.equals("0")) continue;

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountStr);
                        if (divisibility > 0)
                            amount = amount.scaleByPowerOfTen(-divisibility).stripTrailingZeros();
                    } catch (Exception e) { continue; }

                    long timestamp = item.optLong("timestamp", item.optLong("time", 0));
                    String from = item.optString("from", "");
                    String to   = item.optString("to", "");

                    boolean isMint    = type.equals("mint");
                    boolean isReceive = isMint || type.equals("receive") || address.equalsIgnoreCase(to);
                    boolean isSend    = type.equals("send") || (!isReceive && address.equalsIgnoreCase(from));
                    if (!isReceive && !isSend) continue;

                    String counterparty = isReceive ? from : to;
                    String qty = isSend ? "-" + amount.toPlainString() : amount.toPlainString();

                    TransazioneDefi trans = new TransazioneDefi();
                    trans.Wallet          = walletEntry;
                    trans.HashTransazione = txid;
                    trans.Rete            = "BTC";
                    trans.TransazioneOK   = true;
                    trans.TipoTransazione = isMint ? "Mint" : "Transfer";
                    trans.Blocco          = String.valueOf(blockHeight);
                    trans.DataOra         = timestamp > 0
                            ? FunzioniDate.ConvertiDatadaLongAlSecondo(timestamp * 1000L) : "";
                    trans.TimeStamp       = String.valueOf(timestamp);
                    trans.InserisciMonete(runeSymbol, runeName, runeid, counterparty, qty, "Crypto");

                    String safeRuneid = runeid.replace(":", "_");
                    result.put(walletEntry + "." + txid + "_rune_" + safeRuneid, trans);
                }

                cursor += detail.length();
                if (cursor >= data.optInt("total", 0) || detail.length() == 0) done = true;
            }
            Thread.sleep(200);
        }
        return result;
    }

    /**
     * Punto di ingresso con UniSat: BTC da mempool.space (gratuito) + BRC-20 e Runes da UniSat.
     */
    public static Map<String, TransazioneDefi> fetchAndParseTransactionsWithUniSat(
            String walletEntry, int fromBlock, String uniSatApiKey,
            Component ccc, Download progressb) throws InterruptedException {

        Map<String, TransazioneDefi> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try {
            System.out.println("[BTC+UniSat] === Importazione wallet: " + walletEntry + " (dal blocco " + fromBlock + ") ===");

            Set<String> addresses;
            if (isExtendedKey(walletEntry)) {
                progressb.SetLabel("[BTC+UniSat] Derivazione indirizzi da chiave estesa...");
                addresses = deriveUsedAddresses(walletEntry, progressb);
            } else {
                addresses = new LinkedHashSet<>();
                addresses.add(walletEntry);
                System.out.println("[BTC+UniSat] Indirizzo singolo: " + walletEntry);
            }
            if (addresses.isEmpty()) {
                System.out.println("[BTC+UniSat] Nessun indirizzo trovato per " + walletEntry);
                return result;
            }
            System.out.println("[BTC+UniSat] Totale indirizzi da scansionare: " + addresses.size());

            // 1 – Transazioni BTC standard via mempool.space (nessun costo API UniSat)
            System.out.println("[BTC+UniSat] --- FASE 1: transazioni BTC via mempool.space ---");
            progressb.SetLabel("[BTC+UniSat] Scaricamento transazioni BTC da mempool.space...");
            Map<String, JSONObject> txById = new LinkedHashMap<>();
            int addrDone = 0;
            for (String addr : addresses) {
                if (progressb.FineThread()) return result;
                addrDone++;
                progressb.SetLabel("[BTC+UniSat] BTC " + addrDone + "/" + addresses.size()
                        + ": " + abbreviate(addr));
                System.out.println("[BTC+UniSat] Scaricamento BTC " + addrDone + "/" + addresses.size() + ": " + addr);
                List<JSONObject> addrTxs = fetchAddressTxs(addr, fromBlock);
                System.out.println("[BTC+UniSat]   -> " + addrTxs.size() + " transazioni trovate");
                for (JSONObject tx : addrTxs) {
                    txById.putIfAbsent(tx.optString("txid"), tx);
                }
                Thread.sleep(120);
            }
            List<JSONObject> sorted = new ArrayList<>(txById.values());
            sorted.sort(Comparator.comparingInt(tx -> {
                JSONObject s = ((JSONObject) tx).optJSONObject("status");
                return s != null ? s.optInt("block_height", 0) : 0;
            }));
            System.out.println("[BTC+UniSat] " + sorted.size() + " transazioni BTC uniche da elaborare (deduplicate)");
            int btcMovimentiValidi = 0;
            for (JSONObject tx : sorted) {
                if (progressb.FineThread()) return result;
                TransazioneDefi trans = parseTransaction(tx, walletEntry, addresses);
                if (trans != null && !trans.isEmpty()) {
                    result.put(walletEntry + "." + trans.HashTransazione, trans);
                    btcMovimentiValidi++;
                }
            }
            System.out.println("[BTC+UniSat] FASE 1 completata: " + btcMovimentiValidi + " movimenti BTC importati");

            // 2 – BRC-20 e Runes via UniSat per ogni indirizzo derivato
            System.out.println("[BTC+UniSat] --- FASE 2: BRC-20 e Runes via UniSat ---");
            int addrIdx = 0;
            for (String addr : addresses) {
                if (progressb.FineThread()) return result;
                addrIdx++;

                // BRC-20
                progressb.SetLabel("[UniSat] BRC-20 " + addrIdx + "/" + addresses.size()
                        + ": " + abbreviate(addr));
                System.out.println("[UniSat] BRC-20 — ricerca ticker per indirizzo " + addrIdx + "/" + addresses.size() + ": " + addr);
                List<String> tickers = getBRC20Tickers(addr, uniSatApiKey);
                if (tickers.isEmpty()) {
                    System.out.println("[UniSat]   -> Nessun ticker BRC-20 trovato");
                } else {
                    System.out.println("[UniSat]   -> " + tickers.size() + " ticker trovati: " + String.join(", ", tickers));
                    for (String ticker : tickers) {
                        if (progressb.FineThread()) return result;
                        progressb.SetLabel("[UniSat] BRC-20 " + ticker + " su " + abbreviate(addr));
                        System.out.println("[UniSat]   Scaricamento cronologia BRC-20 " + ticker + "...");
                        Map<String, TransazioneDefi> brc20Txs = fetchBRC20HistoryForTicker(addr, ticker, uniSatApiKey, fromBlock, walletEntry);
                        System.out.println("[UniSat]   -> " + brc20Txs.size() + " movimenti BRC-20 " + ticker);
                        result.putAll(brc20Txs);
                        Thread.sleep(200);
                    }
                }

                // Runes
                progressb.SetLabel("[UniSat] Runes " + addrIdx + "/" + addresses.size()
                        + ": " + abbreviate(addr));
                System.out.println("[UniSat] Runes — ricerca per indirizzo " + addrIdx + "/" + addresses.size() + ": " + addr);
                List<JSONObject> runesList = getRunesBalances(addr, uniSatApiKey);
                if (runesList.isEmpty()) {
                    System.out.println("[UniSat]   -> Nessuna Rune trovata");
                } else {
                    System.out.println("[UniSat]   -> " + runesList.size() + " Rune trovate");
                    for (JSONObject runeInfo : runesList) {
                        if (progressb.FineThread()) return result;
                        String runeid   = runeInfo.optString("runeid", "");
                        String runeName = runeInfo.optString("rune", runeInfo.optString("spacedRune", runeid));
                        int    divisi   = runeInfo.optInt("divisibility", 0);
                        if (runeid.isBlank()) continue;
                        progressb.SetLabel("[UniSat] Rune " + runeName + " su " + abbreviate(addr));
                        System.out.println("[UniSat]   Scaricamento cronologia Rune " + runeName + " (" + runeid + ")...");
                        Map<String, TransazioneDefi> runeTxs = fetchRunesHistoryForRune(
                                addr, runeid, runeName, divisi, uniSatApiKey, fromBlock, walletEntry);
                        System.out.println("[UniSat]   -> " + runeTxs.size() + " movimenti Rune " + runeName);
                        result.putAll(runeTxs);
                        Thread.sleep(200);
                    }
                }
            }

            System.out.println("[BTC+UniSat] === Completato: " + result.size() + " movimenti totali per " + walletEntry + " ===");

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            System.err.println("[BTC+UniSat] Errore: " + e.getMessage());
            LoggerGC.ScriviErrore(e);
        }
        return result;
    }

    // =========================================================================

    /**
     * Valida un indirizzo Bitcoin singolo o una chiave pubblica estesa (xpub/ypub/zpub/tpub).
     */
    public static boolean isValidBitcoinAddress(String address) {
        if (address == null || address.isBlank()) return false;
        // P2PKH (1...) o P2SH (3...)
        if (address.matches("^[13][1-9A-HJ-NP-Za-km-z]{25,34}$")) return true;
        // Bech32 native SegWit (bc1q...) o Taproot (bc1p...)
        if (address.toLowerCase().matches("^bc1[ac-hj-np-z02-9]{6,87}$")) return true;
        // Chiavi pubbliche estese
        if (address.matches("^[xyzt]pub[1-9A-HJ-NP-Za-km-z]{100,115}$")) return true;
        return false;
    }
}
