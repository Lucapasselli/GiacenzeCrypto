/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author luca.passelli
 */
public class Exchanges {
    
    // Classe helper per mantenere stato
private static long lastNonce = 0;




public static String CryptoComExchangeV2GetAccountSummarySafe(
        String apiKey,
        String apiSecret
) throws Exception {

    String API_URL = "https://api.crypto.com/v2/private/get-account-summary";

    // Nonce sempre > precedente (incrementa di 1ms minimo)
    long nonce = Math.max(lastNonce + 1, Instant.now().toEpochMilli() - 5000);
    lastNonce = nonce;

    int id = (int) (nonce % 1_000_000);
    String method = "private/get-account-summary";

    Map<String, Object> params = new LinkedHashMap<>(); // vuoto

    Map<String, Object> paramsForSign = new TreeMap<>(params);
    StringBuilder paramString = new StringBuilder();
    for (Map.Entry<String, Object> e : paramsForSign.entrySet()) {
        paramString.append(e.getKey()).append(e.getValue());
    }

    String sigPayload = method + id + apiKey + paramString + nonce;
    System.out.println("nonce usato: " + nonce + " | sigPayload: " + sigPayload);

    String signature = hmacSha256Hex(apiSecret, sigPayload);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", id);
    body.put("method", method);
    body.put("api_key", apiKey);
    body.put("params", params);
    body.put("nonce", nonce);
    body.put("sig", signature);
String jsonBody = new Gson().toJson(body);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("RESPONSE: " + response.body());
    return response.body();
    // ... resto invariato (HttpClient, etc.)
    // [copia il resto dalla funzione precedente]
}

    
 

//UTILIZZO
   /*     for (String splittata[] : DatabaseH2.Pers_ExchangeApi_LeggiTabella().values()) {
            try {
                // Prezzi.RecuperaGiacenzeDaCCXT("binance", splittata[2], splittata[3], "2026-01-11");
                if (splittata[0].equalsIgnoreCase("Crypto.com Exchange")) {
                    System.out.println(splittata[0]);
                    System.out.println(splittata[2]);
                    System.out.println(splittata[3]);
                   // String json = Exchanges.CryptoComExchangeV2GetAccountSummarySafe(splittata[2], splittata[3]);
                    String json = Exchanges.CryptoComExchangeBalanceHistory(
                            splittata[2],
                            splittata[3],
                            "D1", // giornaliero
                            null, // fino ad ora
                            30 // massimo consentito
                    );
                    System.out.println(json);

                    break;
                }
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }*/






    
    
    
public static String CryptoComExchangeBalanceHistory(
        String apiKey,
        String apiSecret,
        String timeframe,     // "D1" o "H1" o null
        Long endTime,         // millis oppure null
        Integer limit         // oppure null
) throws Exception {

    String API_URL = "https://api.crypto.com/exchange/v1/private/user-balance-history";

    long nonce = Instant.now().toEpochMilli();
    int id = (int) (nonce % 1_000_000);

    // ===== 1️⃣ Params usati per la firma (solo quelli che vanno in "params") =====
    // Attenzione: solo parametri reali dell'endpoint, in ordine alfabetico per chiave
    Map<String, Object> paramsForSign = new TreeMap<>();
    if (endTime != null)   paramsForSign.put("end_time", endTime);
    if (limit != null)     paramsForSign.put("limit", limit);
    if (timeframe != null) paramsForSign.put("timeframe", timeframe);

    // Costruisci la paramString = key1 + value1 + key2 + value2 + ...
    StringBuilder paramString = new StringBuilder();
    for (Map.Entry<String, Object> e : paramsForSign.entrySet()) {
        paramString.append(e.getKey()).append(e.getValue());
    }

    String method = "private/user-balance-history";

    // ===== 2️⃣ Sig payload secondo specifica Exchange =====
    // method + id + api_key + paramString + nonce
    String sigPayload = method
            + id
            + apiKey
            + paramString
            + nonce;

    String signature = hmacSha256Hex(apiSecret, sigPayload);

    // ===== 3️⃣ Costruzione body JSON =====
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", id);
    body.put("method", method);
    body.put("api_key", apiKey);
    body.put("nonce", nonce);

    // Params nel body JSON (anche vuoto, ma qui solo se non null)
    Map<String, Object> bodyParams = new LinkedHashMap<>();
    if (timeframe != null) bodyParams.put("timeframe", timeframe);
    if (endTime != null)   bodyParams.put("end_time", endTime);
    if (limit != null)     bodyParams.put("limit", limit);
    body.put("params", bodyParams);

    body.put("sig", signature); // NB: campo "sig" (non "sign") per Exchange

    String jsonBody = new Gson().toJson(body);

    // ===== 4️⃣ Invio richiesta HTTP POST =====
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

    return response.body();
}

// ===== HMAC-SHA256 helper per la firma =====
private static String hmacSha256Hex(String secret, String message) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

    StringBuilder hex = new StringBuilder();
    for (byte b : rawHmac) {
        String h = Integer.toHexString(0xff & b);
        if (h.length() == 1) hex.append('0');
        hex.append(h);
    }
    return hex.toString();
}





  /*  private static String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }*/

  
}
