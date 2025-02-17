/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

/**
 *
 * @author luca.passelli
 */
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class SolanaHeliusAnalyzer {

    private static final String HELIUS_API_KEY = "apiHelius"; // Inserisci la tua API key
    private static final String HELIUS_RPC_URL = "https://api.helius.xyz/v0/addresses/";
    private static final String HELIUS_RPC_URL2 = "https://mainnet.helius-rpc.com/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Map<String, String> tokenNameCache = new HashMap<>();


    public static void fetchAndParseTransactions(String walletAddress, String afterSignature) throws InterruptedException {
        try {
            JSONArray transactions = getParsedTransactions(walletAddress, afterSignature);
            if (transactions != null) {
                parseTransactions(transactions, walletAddress);
            } else {
                System.out.println("Nessuna transazione trovata.");
            }
        } catch (IOException e) {
            System.err.println("Errore nel recupero delle transazioni: " + e.getMessage());
        }
    }

    private static JSONArray getParsedTransactions(String walletAddress, String afterSignature) throws IOException {
        String url = HELIUS_RPC_URL + walletAddress + "/transactions?api-key=" + HELIUS_API_KEY + "&limit=20";
        if (afterSignature != null) {
            url += "&before=" + afterSignature;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonString = response.body().string();
                System.out.println("Risultato JSON: " + jsonString);
                return new JSONArray(jsonString);
            }
        }
        return null;
    }

    private static void parseTransactions(JSONArray transactions, String walletAddress) {
        System.out.println("\n Transazioni per il wallet: " + walletAddress);
        for (int i = 0; i < transactions.length(); i++) {
            JSONObject tx = transactions.getJSONObject(i);

            String description = tx.optString("description", "N/A");
            String signature = tx.optString("signature", "N/A");
            long timestamp = tx.optLong("timestamp", 0);
            String formattedTimestamp = OperazioniSuDate.ConvertiDatadaLongAlSecondo(timestamp*1000);
            BigDecimal fee = tx.optBigDecimal("fee", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));
            String feePayer = tx.optString("feePayer", "N/A");

            System.out.println("\nTransazione #" + (i + 1));
            System.out.println("Signature: " + signature);
            System.out.println("Timestamp: " + formattedTimestamp);
            if (feePayer.equalsIgnoreCase(walletAddress)) {
                System.out.println("Fee: " + fee + " SOL");
                System.out.println("Fee Payer: " + feePayer);
            }
            System.out.println("Descrizione: " + description);

            // Token Transfers
            JSONArray tokenTransfers = tx.optJSONArray("tokenTransfers");
            if (tokenTransfers != null && tokenTransfers.length() > 0) {
                System.out.println("Token Transfers:");
                for (int j = 0; j < tokenTransfers.length(); j++) {
                    JSONObject transfer = tokenTransfers.getJSONObject(j);
                    String fromUser = transfer.optString("fromUserAccount", "N/A");
                    String toUser = transfer.optString("toUserAccount", "N/A");
                    double amount = transfer.optDouble("tokenAmount", 0);
                    String mint = transfer.optString("mint", "N/A");

                    // Recupera il nome del token
                    String tokenName = "N/A";
                    try {
                        tokenName = getTokenName(mint);
                    } catch (IOException e) {
                        System.err.println("   - Errore nel recupero dei metadati per il token " + mint + ": " + e.getMessage());
                    }

                    System.out.println("   - " + fromUser + " -> " + toUser + " | " + amount + " " + tokenName + " (Mint: " + mint + ")");
                }
            }

            // Native SOL Transfers
            JSONArray nativeTransfers = tx.optJSONArray("nativeTransfers");
            if (nativeTransfers != null && nativeTransfers.length() > 0) {
                System.out.println("Native SOL Transfers:");
                BigDecimal TotaleNativeSOL=new BigDecimal(0);
                for (int j = 0; j < nativeTransfers.length(); j++) {
                    JSONObject transfer = nativeTransfers.getJSONObject(j);
                    String fromUser = transfer.optString("fromUserAccount", "N/A");
                    String toUser = transfer.optString("toUserAccount", "N/A");
                    BigDecimal solAmount = transfer.optBigDecimal("amount", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));
                    if (fromUser.equalsIgnoreCase(walletAddress) || toUser.equalsIgnoreCase(walletAddress)) {
                        if (fromUser.equalsIgnoreCase(walletAddress)&&toUser.equalsIgnoreCase(walletAddress)){}
                        else if (fromUser.equalsIgnoreCase(walletAddress)&&toUser.equalsIgnoreCase(walletAddress)){
                            //in questo caso non faccio nulla perchè vuol dire che mi sono spedito i solana al mio wallet
                        }
                        else if (fromUser.equalsIgnoreCase(walletAddress)){
                        //Questo significa che i solana sono in uscita dal mio wallet quindi li sottraggo dal TotaleNativoSOL
                            TotaleNativeSOL=TotaleNativeSOL.subtract(solAmount);
                        }
                        else if (toUser.equalsIgnoreCase(walletAddress)){
                        //Questo invece significa che i SOL stanno entrando nel wallet quindi li sommo al totale
                            TotaleNativeSOL=TotaleNativeSOL.add(solAmount);
                        }
                        System.out.println("   - " + fromUser + " -> " + toUser + " | " + solAmount + " SOL");
                    }
                }
                System.out.println("Totale Token Nativi Entrati = "+TotaleNativeSOL);
            }
        }
    }


    private static String getTokenName(String mintAddress) throws IOException {
        
        if (DatabaseH2.TokenSolana_Leggi(mintAddress)!=null||tokenNameCache.get(mintAddress)!=null) {
            if (tokenNameCache.get(mintAddress)!=null&&tokenNameCache.get(mintAddress).equalsIgnoreCase("N/A"))
            {
                return "N/A";
            }else{
                String DettagliToken[]=DatabaseH2.TokenSolana_Leggi(mintAddress);
                String fullTokenName = DettagliToken[0].isEmpty() ? DettagliToken[1] : DettagliToken[1] + " (" + DettagliToken[0] + ")";
                return fullTokenName;
            }
        }

        String jsonPayload = "{" +
                "\"jsonrpc\":\"2.0\"," +
                "\"id\":\"text\"," +
                "\"method\":\"getAsset\"," +
                "\"params\": {\"id\": \"" + mintAddress + "\"}}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(HELIUS_RPC_URL2 + "?api-key=" + HELIUS_API_KEY)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonString = response.body().string();
                System.out.println("Risposta JSON completa getTokenName: " + jsonString);
                JSONObject jsonResponse = new JSONObject(jsonString);
                if (jsonResponse.has("result")) {
                    JSONObject metadata = jsonResponse.getJSONObject("result").optJSONObject("content").optJSONObject("metadata");
                    if (metadata != null) {
                        String tokenName = metadata.optString("name", "N/A");
                        String tokenSymbol = metadata.optString("symbol", "");

                        // Salva nella cache e restituisce il valore
                        String fullTokenName = tokenSymbol.isEmpty() ? tokenName : tokenName + " (" + tokenSymbol + ")";
                        DatabaseH2.TokenSolana_AggiungiToken(mintAddress, tokenSymbol, tokenName);
                        //tokenNameCache.put(mintAddress, fullTokenName);
                        return fullTokenName;
                    }
                }
            }
        }
        //DatabaseH2.TokenSolana_AggiungiToken(mintAddress, "N/A", "N/A");
        tokenNameCache.put(mintAddress, "N/A");
        return "N/A";
    }
}






