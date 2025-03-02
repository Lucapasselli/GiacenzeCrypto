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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Trans_Solana {
    
    private static String HELIUS_API_KEY = DatabaseH2.Opzioni_Leggi("ApiKey_Helius"); // Inserisci la tua API key
    private static final String HELIUS_RPC_URL = "https://api.helius.xyz/v0/addresses/";
    private static final String HELIUS_RPC_URL2 = "https://mainnet.helius-rpc.com/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Map<String, String> tokenNameCache = new HashMap<>();
    static boolean verbose = false;


    public static Map<String, TransazioneDefi> fetchAndParseTransactions(String walletAddress, int afterBlock) throws InterruptedException {
        try {
            
            //JSONArray transactions = getParsedTransactions(walletAddress, afterSignature);
            JSONArray transactions =  getAllTransactions(walletAddress, afterBlock);
            
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

    
  private static JSONArray getAllTransactions(String walletAddress, int afterBlock) throws IOException {
    HELIUS_API_KEY = DatabaseH2.Opzioni_Leggi("ApiKey_Helius");
    JSONArray allTransactions = new JSONArray();
    String beforeSignature = null; // Per iterare le pagine delle transazioni
    boolean hasMore = true;
    int limit = 40; // Numero di transazioni per richiesta
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
                if (verbose) {
                    System.out.println(jsonString);
                }
                if (Funzioni.isValidJSONArray(jsonString)&&!CDC_Grafica.InterrompiCiclo) {
                    JSONArray transactions = new JSONArray(jsonString);

                    if (transactions.length() > 0) {
                        for (int i = 0; i < transactions.length(); i++) {
                            JSONObject tx = transactions.getJSONObject(i);
                            // String currentSignature = tx.getString("signature");
                            //String currentBlock = tx.getString("slot");
                            int currentBlock = tx.optInt("slot", 0);
                            // se il blocco trovato è minore o uguale al blocco di riferimento fermo lo scaricamento delle transazioni
                            if (currentBlock <= afterBlock) {
                                hasMore = false;
                                break;
                            }
                            //System.out.println("Transazione scaricata"+tx);
                            allTransactions.put(tx);
                        }

                        // Imposta la signature dell'ultima transazione per continuare il download
                        beforeSignature = transactions.getJSONObject(transactions.length() - 1).getString("signature");
                    } else {
                        hasMore = false; // Se non ci sono più transazioni, fermiamo il ciclo
                    }
                }else{
                    //Se trovo un errore devo pulire tutte le transazioni e interrompere il ciclo
                    //Siccome le transazioni vengono prese a ritroso poi si rischierebbe di non importare quelle vecchie
                    CDC_Grafica.InterrompiCiclo=false;
                    allTransactions.clear();
                    hasMore = false;
                    break;
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


  public static boolean isApiKeyValida(String ApiKey) {
      //HELIUS_RPC_URL2+"?api-key="+ApiKey
        //System.out.println(ApiKey);
        String jsonPayload = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":1,"
                + "\"method\":\"getBalance\","
                + "\"params\":[\"11111111111111111111111111111111\"]"
                + "}";

        
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(HELIUS_RPC_URL2+"?api-key="+ApiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String responseString = response.body().string();
                //System.out.println(responseString);
                return response.isSuccessful() && responseString.contains("\"result\"");
            }
        } catch (Exception e) {
            System.err.println("Errore nella verifica API Key: " + e.getMessage());
        }
        return false;
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

        for (int i = 0; i < transactions.length(); i++) {
            int numMovimenti = 0;
            TransazioneDefi trans = new TransazioneDefi();
            trans.Rete = "SOL";
            trans.Wallet = walletAddress;
            JSONObject tx = transactions.getJSONObject(i);

            String description = tx.optString("description", "N/A");
            String signature = tx.optString("signature", "N/A");
            String block = tx.optString("slot", "N/A");
            long timestamp = tx.optLong("timestamp", 0);
            String formattedTimestamp = OperazioniSuDate.ConvertiDatadaLongAlSecondo(timestamp * 1000);
            BigDecimal fee = BigDecimal.ZERO;

            String feePayer = tx.optString("feePayer", "N/A");
            if (feePayer.equalsIgnoreCase(walletAddress)) {
                fee = tx.optBigDecimal("fee", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));
                numMovimenti++;
                trans.QtaCommissioni = "-" + fee.toPlainString();
            }

            trans.Blocco = block;
            trans.HashTransazione = signature;
            trans.MonetaCommissioni = "SOL";
            if (verbose) {
                System.out.println("\nTimestamp: " + formattedTimestamp + " - Transazione #" + (i + 1) + " - Signature: " + signature);
            }
            trans.DataOra = formattedTimestamp;
            if (verbose) {
                System.out.println("Descrizione: " + description);
            }
            String AddressNoWallet = "";

            JSONArray AccountChanges = tx.optJSONArray("accountData");
            BigDecimal NativeSOL = new BigDecimal(0);
            if (AccountChanges != null && AccountChanges.length() > 0) {
                if (verbose) {
                    System.out.println("AccountChanges:");
                }
                for (int j = 0; j < AccountChanges.length(); j++) {
                    JSONObject AccountChange = AccountChanges.getJSONObject(j);
                    String account = AccountChange.optString("account", "N/A");
                    BigDecimal nativeSolAmount = AccountChange.optBigDecimal("nativeBalanceChange", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));

                    if (account.equalsIgnoreCase(walletAddress) && nativeSolAmount.compareTo(BigDecimal.ZERO) != 0) {
                        //Se arrivo qua vuol dire che ho dei nativeSOL da gestire
                        //Il totale dei NativeSol Movimentati sarà quindi quello che vedo nel change meno le fee
                        NativeSOL = nativeSolAmount.add(fee);
                        if (NativeSOL.compareTo(BigDecimal.ZERO) != 0) {
                            numMovimenti++;
                            trans.InserisciMonete("SOL", "SOL", "SOL", AddressNoWallet, NativeSOL.toPlainString(), "Crypto");
                        }
                    }
                    //Adesso controllo il "tokenBalanceChanges" che è un array JSON
                    //per controllare qui token di cui è cambiata la giacenza
                    JSONArray tokenBalanceChanges = AccountChange.optJSONArray("tokenBalanceChanges");
                    if (tokenBalanceChanges != null && tokenBalanceChanges.length() > 0) {
                        for (int k = 0; k < tokenBalanceChanges.length(); k++) {
                            // System.out.println("numero tokenBalanceChanges "+tokenBalanceChanges.length());
                            JSONObject tokenBalanceChange = tokenBalanceChanges.getJSONObject(k);
                            String userAccount = tokenBalanceChange.optString("userAccount", "N/A");
                            if (userAccount.equalsIgnoreCase(walletAddress)) {
                                String mint = tokenBalanceChange.optString("mint", "N/A");
                                JSONObject dettagli = tokenBalanceChange.getJSONObject("rawTokenAmount");
                                BigDecimal decimals = BigDecimal.TEN.pow(dettagli.optInt("decimals", 0));
                                if (decimals.compareTo(BigDecimal.ZERO) != 0) {
                                    BigDecimal tokenAmount = dettagli.optBigDecimal("tokenAmount", BigDecimal.ZERO).divide(decimals);
                                    if (tokenAmount.compareTo(BigDecimal.ZERO) != 0) {
                                        String tokenName = "N/A";
                                        String tokenSymbol = mint;
                                        String Tipologia = "Crypto";
                                        try {
                                            String DettagliMoneta[]= getTokenName(mint);
                                            tokenSymbol = DettagliMoneta[0];
                                            tokenName = DettagliMoneta[1];
                                            Tipologia = DettagliMoneta[2];
                                        } catch (IOException ex) {
                                            Logger.getLogger(Trans_Solana.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        if (!tokenSymbol.equalsIgnoreCase("SOL")) {
                                            numMovimenti++;
                                            trans.InserisciMonete(tokenSymbol, tokenName, mint, AddressNoWallet, tokenAmount.toPlainString(), Tipologia);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            if (numMovimenti > 0) {
                MappaTransazioniDefi.put(walletAddress + "." + trans.HashTransazione, trans);
            }

        }
        return MappaTransazioniDefi;
    }


    private static String[] getTokenName(String mintAddress) throws IOException {
        //campo[0]=Simbolo
        //campo[1]=Nome Token
        //campo[2]=Tipo
        String ritorno[]=new String[3];
        
        if (DatabaseH2.TokenSolana_Leggi(mintAddress)!=null||tokenNameCache.get(mintAddress)!=null) {
            if (tokenNameCache.get(mintAddress)!=null&&tokenNameCache.get(mintAddress).equalsIgnoreCase("N/A"))
            {
                ritorno[0]="N/A";
                ritorno[1]="N/A";
                ritorno[2]="Crypto";
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
                    String Tipologia = jsonResponse.getJSONObject("result").optString("interface");
                    if (metadata != null) {
                        ritorno[1] = metadata.optString("name", "N/A");
                        ritorno[0] = metadata.optString("symbol", "");
                        if (Tipologia!=null && !Tipologia.equalsIgnoreCase("FungibleToken"))ritorno[2]="NFT";
                        else ritorno[2]="Crypto";
                        // Salva nella cache e restituisce il valore
                        //String fullTokenName = ritorno[0].isEmpty() ? ritorno[1] : ritorno[1] + " (" + ritorno[0] + ")";
                        DatabaseH2.TokenSolana_AggiungiToken(mintAddress, ritorno[0], ritorno[1],ritorno[2]);
                        return ritorno;
                    }
                }
            }
        }
        //DatabaseH2.TokenSolana_AggiungiToken(mintAddress, "N/A", "N/A");
        tokenNameCache.put(mintAddress, "N/A");
        ritorno[0]="N/A";
        ritorno[1]="N/A";
        ritorno[2]="Crypto";
        return ritorno;
    }
}






