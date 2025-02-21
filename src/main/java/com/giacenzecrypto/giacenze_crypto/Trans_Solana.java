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

public class Trans_Solana {
    
    private static final String HELIUS_API_KEY = DatabaseH2.Opzioni_Leggi("ApiKey_Helius"); // Inserisci la tua API key
    private static final String HELIUS_RPC_URL = "https://api.helius.xyz/v0/addresses/";
    private static final String HELIUS_RPC_URL2 = "https://mainnet.helius-rpc.com/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Map<String, String> tokenNameCache = new HashMap<>();


    public static Map<String, TransazioneDefi> fetchAndParseTransactions(String walletAddress, String afterSignature) throws InterruptedException {
        try {
            //JSONArray transactions = getParsedTransactions(walletAddress, afterSignature);
            JSONArray transactions =  getAllTransactions(walletAddress, afterSignature);
            
            if (transactions != null) {
                return parseTransactions(transactions, walletAddress);
            } else {
                System.out.println("Nessuna transazione trovata.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Errore nel recupero delle transazioni: " + e.getMessage());
            return null;
        }
    }

    
  private static JSONArray getAllTransactions(String walletAddress, String afterSignature) throws IOException {
    JSONArray allTransactions = new JSONArray();
    String beforeSignature = null; // Per iterare le pagine delle transazioni
    boolean hasMore = true;
    int limit = 25; // Numero di transazioni per richiesta
    int Ntrans = 1;

    while (hasMore) {
        System.out.println("Scaricamento Transazioni da "+Ntrans+" a "+(Ntrans+limit-1));
        Ntrans=Ntrans+limit;
        String url = HELIUS_RPC_URL + walletAddress + "/transactions?api-key=" + HELIUS_API_KEY + "&limit=" + limit;

        // Se abbiamo una signature precedente, la usiamo per continuare indietro nel tempo
        if (beforeSignature != null) {
            url += "&before=" + beforeSignature;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonString = response.body().string();
               // System.out.println(jsonString);
                JSONArray transactions = new JSONArray(jsonString);

                if (transactions.length() > 0) {
                    for (int i = 0; i < transactions.length(); i++) {
                        JSONObject tx = transactions.getJSONObject(i);
                        String currentSignature = tx.getString("signature");

                        // Se incontriamo la afterSignature, ci fermiamo
                        if (afterSignature != null && currentSignature.equals(afterSignature)) {
                            hasMore = false;
                            break;
                        }

                        allTransactions.put(tx);
                    }

                    // Imposta la signature dell'ultima transazione per continuare il download
                    beforeSignature = transactions.getJSONObject(transactions.length() - 1).getString("signature");
                } else {
                    hasMore = false; // Se non ci sono più transazioni, fermiamo il ciclo
                }
            }
        }

        // Aggiunge un ritardo tra le richieste per evitare di sovraccaricare l'API
        try {
            Thread.sleep(1000); // 500 ms di pausa tra una richiesta e l'altra
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    return sortTransactionsByTimestamp(allTransactions);
}


// Funzione per ordinare le transazioni dal più vecchio al più recente
private static JSONArray sortTransactionsByTimestamp(JSONArray transactions) {
    List<JSONObject> transactionList = new ArrayList<>();

    for (int i = 0; i < transactions.length(); i++) {
        transactionList.add(transactions.getJSONObject(i));
    }

    transactionList.sort(Comparator.comparingLong(t -> t.optLong("timestamp", 0)));

    return new JSONArray(transactionList);
}


    private static Map<String, TransazioneDefi> parseTransactions(JSONArray transactions, String walletAddress) {
        Map<String, TransazioneDefi> MappaTransazioniDefi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //System.out.println("\n Transazioni per il wallet: " + walletAddress);
        boolean verbose = false;

        for (int i = 0; i < transactions.length(); i++) {
            int numMovimenti = 0;
            TransazioneDefi trans = new TransazioneDefi();
            trans.Rete = "SOL";
            trans.Wallet = walletAddress;
            JSONObject tx = transactions.getJSONObject(i);

            String description = tx.optString("description", "N/A");
            String signature = tx.optString("signature", "N/A");
            long timestamp = tx.optLong("timestamp", 0);
            String formattedTimestamp = OperazioniSuDate.ConvertiDatadaLongAlSecondo(timestamp * 1000);
            BigDecimal fee = tx.optBigDecimal("fee", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));
            String feePayer = tx.optString("feePayer", "N/A");

            // System.out.println("\nTransazione #" + (i + 1));
            // System.out.println("Signature: " + signature);
            trans.Blocco = signature;
            trans.HashTransazione = signature;
            trans.MonetaCommissioni = "SOL";
            if (verbose) {
                System.out.println("\nTimestamp: " + formattedTimestamp + " - Transazione #" + (i + 1) + " - Signature: " + signature);
            }
            trans.DataOra = formattedTimestamp;
            if (feePayer.equalsIgnoreCase(walletAddress)) {
                numMovimenti++;
                // System.out.println("Fee: " + fee + " SOL");
                // System.out.println("Fee Payer: " + feePayer);
                trans.QtaCommissioni = "-" + fee.toPlainString();
            }
            if (verbose) {
                System.out.println("Descrizione: " + description);
            }

            // Token Transfers
            JSONArray tokenTransfers = tx.optJSONArray("tokenTransfers");
            //   BigDecimal TotTransferSOL=new BigDecimal(0);
            String AddressNoWallet = "";
            if (tokenTransfers != null && tokenTransfers.length() > 0) {

                if (verbose) {
                    System.out.println("Token Transfers:");
                }
                for (int j = 0; j < tokenTransfers.length(); j++) {
                    JSONObject transfer = tokenTransfers.getJSONObject(j);
                    String fromUser = transfer.optString("fromUserAccount", "N/A");
                    String toUser = transfer.optString("toUserAccount", "N/A");
                    BigDecimal amount = transfer.optBigDecimal("tokenAmount", BigDecimal.ZERO);
                    String mint = transfer.optString("mint", "N/A");

                    // Recupera il nome del token
                    String tokenName = "N/A";
                    String tokenSymbol;

                    try {
                        tokenSymbol = getTokenName(mint)[0];
                        tokenName = getTokenName(mint)[1];

                        if (fromUser.equalsIgnoreCase(walletAddress)) {
                            AddressNoWallet = toUser;
                            amount = BigDecimal.ZERO.subtract(amount);
                        }
                        if (toUser.equalsIgnoreCase(walletAddress)) {
                            AddressNoWallet = fromUser;
                        }
                        //se è un movimento di SOL , se amount è zero o destinazione e origine coincidono non salvo il movimento
                        if (!tokenSymbol.equalsIgnoreCase("SOL") && amount.compareTo(BigDecimal.ZERO) != 0 && !fromUser.equals(toUser)) {
                            numMovimenti++;
                            //inserisco subito la moneta solo se non è sol
                            //se è sol la inserisco dalla somma dei native token
                            trans.InserisciMonete(tokenSymbol, tokenName, mint, AddressNoWallet, amount.toPlainString(), "Crypto");
                        }
                    } catch (IOException e) {
                        System.err.println("   - Errore nel recupero dei metadati per il token " + mint + ": " + e.getMessage());
                    }

                    if (verbose) {
                        System.out.println("   - " + fromUser + " -> " + toUser + " | " + amount + " " + tokenName + " (Mint: " + mint + ")");
                    }
                }
            }

            // Native SOL Transfers
            JSONArray nativeTransfers = tx.optJSONArray("nativeTransfers");
            BigDecimal TotaleNativeSOL = new BigDecimal(0);
            if (nativeTransfers != null && nativeTransfers.length() > 0) {
                if (verbose) {
                    System.out.println("Native SOL Transfers:");
                }
                for (int j = 0; j < nativeTransfers.length(); j++) {
                    JSONObject transfer = nativeTransfers.getJSONObject(j);
                    String fromUser = transfer.optString("fromUserAccount", "N/A");
                    String toUser = transfer.optString("toUserAccount", "N/A");
                    BigDecimal solAmount = transfer.optBigDecimal("amount", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));

                    if (fromUser.equalsIgnoreCase(walletAddress) && !solAmount.equals(0)) {
                        //Questo significa che i solana sono in uscita dal mio wallet quindi li sottraggo dal TotaleNativoSOL
                        TotaleNativeSOL = TotaleNativeSOL.subtract(solAmount);
                    }
                    if (toUser.equalsIgnoreCase(walletAddress) && !solAmount.equals(0)) {
                        //Questo invece significa che i SOL stanno entrando nel wallet quindi li sommo al totale
                        TotaleNativeSOL = TotaleNativeSOL.add(solAmount);
                    }
                    if (verbose) {
                        System.out.println("   - " + fromUser + " -> " + toUser + " | " + solAmount + " SOL");
                    }

                }
                if (TotaleNativeSOL.compareTo(BigDecimal.ZERO) != 0) {
                    numMovimenti++;
                    trans.InserisciMonete("SOL", "SOL", "SOL", AddressNoWallet, TotaleNativeSOL.toPlainString(), "Crypto");
                }
                //System.out.println("Totale Token Nativi Entrati = "+TotaleNativeSOL);
            }
            if (numMovimenti > 0) {
                MappaTransazioniDefi.put(walletAddress + "." + trans.HashTransazione, trans);
            }
            //Fine analisi Transazione
            //Adesso analizzo i NativeTokenTrasnfer con i SOL movimentati
            //Faccio "Native SOL Transfer - SOL Transfer"
            //Se totale negativo e SOL Transfer = 0 allora creo rigo di prelievo
            //Se totale negativo e SOL Transfer valorizzato aggiungo la differenza alle commissioni
            //Se totale positivo e SOL Transfer = 0 allora creo rigo di deposito
            //Se totale positivo e SOL Transfer valorizzato creo un nuovo rigo separato di deposito
            /*  BigDecimal differenza=TotaleNativeSOL.subtract(TotTransferSOL);
            System.out.println("Differenza "+differenza);
            System.out.println("TotaleNativeSOL "+TotaleNativeSOL);
            System.out.println("TotTransferSOL "+TotTransferSOL);
                    
                    //TotaleNativeSOL=TotaleNativeSOL.add(differenza);
                    if (TotaleNativeSOL.compareTo(BigDecimal.ZERO)!=0){
                        numMovimenti++;
                        trans.InserisciMonete("SOL", "SOL", "SOL", AddressNoWallet, TotaleNativeSOL.toPlainString(), "Crypto");
                    }
             */

            //System.out.println("Numero movimenti validi = "+numMovimenti);
        }
        return MappaTransazioniDefi;
    }


    private static String[] getTokenName(String mintAddress) throws IOException {
        //campo[0]=Simbolo
        //campo[1]=Nome Token
        String ritorno[]=new String[2];
        
        if (DatabaseH2.TokenSolana_Leggi(mintAddress)!=null||tokenNameCache.get(mintAddress)!=null) {
            if (tokenNameCache.get(mintAddress)!=null&&tokenNameCache.get(mintAddress).equalsIgnoreCase("N/A"))
            {
                ritorno[0]="N/A";
                ritorno[1]="N/A";
                return ritorno;
            }else{
                ritorno=DatabaseH2.TokenSolana_Leggi(mintAddress);
                //String fullTokenName = ritorno[0].isEmpty() ? ritorno[1] : ritorno[1] + " (" + ritorno[0] + ")";
                return ritorno;
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
                //System.out.println("Risposta JSON completa getTokenName: " + jsonString);
                JSONObject jsonResponse = new JSONObject(jsonString);
                if (jsonResponse.has("result")) {
                    JSONObject metadata = jsonResponse.getJSONObject("result").optJSONObject("content").optJSONObject("metadata");
                    if (metadata != null) {
                        ritorno[1] = metadata.optString("name", "N/A");
                        ritorno[0] = metadata.optString("symbol", "");

                        // Salva nella cache e restituisce il valore
                        //String fullTokenName = ritorno[0].isEmpty() ? ritorno[1] : ritorno[1] + " (" + ritorno[0] + ")";
                        DatabaseH2.TokenSolana_AggiungiToken(mintAddress, ritorno[0], ritorno[1]);
                        return ritorno;
                    }
                }
            }
        }
        //DatabaseH2.TokenSolana_AggiungiToken(mintAddress, "N/A", "N/A");
        tokenNameCache.put(mintAddress, "N/A");
        ritorno[0]="N/A";
        ritorno[1]="N/A";
        return ritorno;
    }
}






