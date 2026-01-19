/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;
import com.google.gson.Gson;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
public class Exchanges {


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

        // ===== 1️⃣ Costruzione mappa params opzionali (solo per firma) =====
        Map<String, Object> paramsForSign = new TreeMap<>();
        if (timeframe != null) paramsForSign.put("timeframe", timeframe);
        if (endTime != null) paramsForSign.put("end_time", endTime);
        if (limit != null) paramsForSign.put("limit", limit);

        // ===== 2️⃣ Costruzione mappa completa firma (v1 style) =====
        Map<String, Object> sigMap = new TreeMap<>();
        sigMap.put("api_key", apiKey);
        sigMap.put("id", id);
        sigMap.put("method", "private/user-balance-history");
        sigMap.put("nonce", nonce);
        sigMap.putAll(paramsForSign); // aggiunge eventuali param opzionali

        // ===== 3️⃣ Concatenazione key+value in ordine alfabetico =====
        StringBuilder sigPayload = new StringBuilder();
        for (Map.Entry<String, Object> e : sigMap.entrySet()) {
            sigPayload.append(e.getKey()).append(e.getValue());
        }
        sigPayload.append(apiSecret); // appendi secret alla fine

        String signature = sha256Hex(sigPayload.toString());

        // ===== 4️⃣ Costruzione body JSON =====
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("method", "private/user-balance-history");
        body.put("api_key", apiKey);
        body.put("nonce", nonce);

        // Params nel body JSON (anche vuoto)
        Map<String, Object> bodyParams = new LinkedHashMap<>();
        if (timeframe != null) bodyParams.put("timeframe", timeframe);
        if (endTime != null) bodyParams.put("end_time", endTime);
        if (limit != null) bodyParams.put("limit", limit);
        body.put("params", bodyParams);

        body.put("sign", signature);

        String jsonBody = new Gson().toJson(body);

        // ===== 5️⃣ Invio richiesta HTTP POST =====
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

    // ===== 6️⃣ SHA256 helper =====
    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) hex.append('0');
            hex.append(s);
        }
        return hex.toString();
    }
}
