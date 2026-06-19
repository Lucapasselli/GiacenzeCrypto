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

        for (int chain = 0; chain <= 1; chain++) {
            DeterministicKey chainKey = HDKeyDerivation.deriveChildKey(masterKey, chain);
            int consecutiveUnused = 0;

            for (int idx = 0; consecutiveUnused < GAP_LIMIT; idx++) {
                if (progressb != null && progressb.FineThread()) break;

                DeterministicKey childKey = HDKeyDerivation.deriveChildKey(chainKey, idx);
                String address = addressFromKey(childKey, keyType);

                String chainLabel = (chain == 0) ? "ext" : "chg";
                if (progressb != null) {
                    progressb.SetLabel("[BTC] Scansione " + chainLabel + " #" + idx + ": " + abbreviate(address));
                }
                if (verbose) {
                    System.out.println("[BTC] " + chainLabel + "/" + idx + " -> " + address);
                }

                int txCount = getAddressTxCount(address);
                if (txCount > 0) {
                    usedAddresses.add(address);
                    consecutiveUnused = 0;
                } else {
                    consecutiveUnused++;
                }
                Thread.sleep(120); // rispetta il rate limit di mempool.space
            }
        }
        System.out.println("[BTC] Chiave " + extKey.substring(0, Math.min(12, extKey.length()))
                + "... -> " + usedAddresses.size() + " indirizzi usati");
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
            trans.InserisciMonete("BTC", "Bitcoin", "BTC", counterparty, btcAmount.toPlainString(), "Crypto");

        } else if (sentSats > 0) {
            // Abbiamo speso degli input
            // Valore netto inviato all'esterno = speso - cambio ricevuto - commissione
            long actualSent = sentSats - receivedSats - feeSats;
            BigDecimal btcFee = satsToDecimal(feeSats);

            if (actualSent > 0) {
                // Pagamento a indirizzo esterno + commissione separata
                BigDecimal btcSent = satsToDecimal(actualSent);
                trans.InserisciMonete("BTC", "Bitcoin", "BTC", counterparty, "-" + btcSent.toPlainString(), "Crypto");
                trans.QtaCommissioni = "-" + btcFee.toPlainString();
            } else {
                // Auto-consolidazione (solo fee, nessuna uscita netta all'esterno)
                trans.QtaCommissioni = "-" + btcFee.toPlainString();
            }
        } else {
            // Ricezione pura (sentSats == 0): deposito
            BigDecimal btcAmount = satsToDecimal(receivedSats);
            trans.InserisciMonete("BTC", "Bitcoin", "BTC", counterparty, btcAmount.toPlainString(), "Crypto");
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
            Set<String> addresses;
            if (isExtendedKey(walletEntry)) {
                progressb.SetLabel("[BTC] Derivazione indirizzi da chiave estesa...");
                addresses = deriveUsedAddresses(walletEntry, progressb);
            } else {
                addresses = new LinkedHashSet<>();
                addresses.add(walletEntry);
            }

            if (addresses.isEmpty()) {
                System.out.println("[BTC] Nessun indirizzo trovato per " + walletEntry);
                return result;
            }

            // Scarica tutte le transazioni (deduplicate per txid)
            Map<String, JSONObject> txById = new LinkedHashMap<>();
            int addrDone = 0;
            for (String addr : addresses) {
                if (progressb.FineThread()) return result;
                addrDone++;
                progressb.SetLabel("[BTC] Scaricamento " + addrDone + "/" + addresses.size()
                        + ": " + abbreviate(addr));

                List<JSONObject> addrTxs = fetchAddressTxs(addr, fromBlock);
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

            System.out.println("[BTC] " + sorted.size() + " transazioni nuove da elaborare per " + walletEntry);
            progressb.SetMassimo(sorted.size());
            int done = 0;

            for (JSONObject tx : sorted) {
                if (progressb.FineThread()) return result;
                TransazioneDefi trans = parseTransaction(tx, walletEntry, addresses);
                if (trans != null && !trans.isEmpty()) {
                    result.put(walletEntry + "." + trans.HashTransazione, trans);
                }
                progressb.SetAvanzamento(++done);
            }

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            System.err.println("[BTC] Errore importazione: " + e.getMessage());
            LoggerGC.ScriviErrore(e);
        }

        return result;
    }

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
