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
    
    private static String HELIUS_API_KEY; //letta a ogni importazione da getAllTransactions, non al caricamento della classe (servirebbe il DB)
    private static final String HELIUS_RPC_URL = "https://api.helius.xyz/v0/addresses/";
    private static final String HELIUS_RPC_URL2 = "https://mainnet.helius-rpc.com/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Map<String, String> tokenNameCache = new HashMap<>();
    //Il log verboso è ora governato da VarCondivise.LogJsonDefi (checkbox in Opzioni -> Varie).


    /**
     * Scarica da Helius tutte le transazioni Solana di un wallet a partire da un blocco, e le converte in
     * oggetti {@link TransazioneDefi} tramite {@link #parseTransactions}.
     * @param walletAddress indirizzo del wallet Solana
     * @param afterBlock numero di blocco a partire dal quale recuperare le transazioni
     * @return mappa signature → {@link TransazioneDefi} delle transazioni trovate, oppure {@code null} se non ci sono transazioni o si verifica un errore di rete
     * @throws InterruptedException se il thread viene interrotto durante l'attesa tra le richieste
     */
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

    
  private static JSONArray fetchTransactions(String walletAddress, int afterBlock, boolean useTokenFilter) throws IOException {
    JSONArray allTransactions = new JSONArray();
    String beforeSignature = null; // Per iterare le pagine delle transazioni
    boolean hasMore = true;
    int limit = 40; // Numero di transazioni per richiesta
    int Ntrans = 1;

    while (hasMore) {
        if(useTokenFilter) System.out.println("Scaricamento Transazioni TOKEN da "+Ntrans+" a "+(Ntrans+limit-1));
        else System.out.println("Scaricamento Transazioni SOL da "+Ntrans+" a "+(Ntrans+limit-1));
        Ntrans=Ntrans+limit;
        String url = HELIUS_RPC_URL + walletAddress + "/transactions?api-key=" + HELIUS_API_KEY + "&limit=" + limit;
        if (useTokenFilter) {
            url += "&token-accounts=balanceChanged";
        }

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
                if (VarCondivise.LogJsonDefi) {
                    System.out.println(jsonString);
                }
                if (Funzioni.isValidJSONArray(jsonString)&&!Principale.InterrompiCiclo) {
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
                    Principale.InterrompiCiclo=false;
                    allTransactions.clear();
                    hasMore = false;
                    break;
                }
            }
        }

        // Aggiunge un ritardo tra le richieste per evitare di sovraccaricare l'API
        try {
            Thread.sleep(1000); // 1000 ms di pausa tra una richiesta e l'altra
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    return allTransactions;
  }

  private static JSONArray getAllTransactions(String walletAddress, int afterBlock) throws IOException {
    HELIUS_API_KEY = DatabaseH2.Opzioni_Leggi("ApiKey_Helius");
    
    // Pass 1: Fetch transazioni generali (SOL, Swap native, ecc)
    JSONArray txs1 = fetchTransactions(walletAddress, afterBlock, false);
    
    // Pass 2: Fetch transazioni SPL Token (che altrimenti rischierebbero di essere ignorate)
    JSONArray txs2 = fetchTransactions(walletAddress, afterBlock, true);

    // Merge evitando i duplicati (basandosi sulla signature)
    Map<String, JSONObject> mergedMap = new HashMap<>();
    
    for (int i = 0; i < txs1.length(); i++) {
        JSONObject tx = txs1.getJSONObject(i);
        mergedMap.put(tx.getString("signature"), tx);
    }
    for (int i = 0; i < txs2.length(); i++) {
        JSONObject tx = txs2.getJSONObject(i);
        mergedMap.put(tx.getString("signature"), tx);
    }

    JSONArray mergedArray = new JSONArray();
    for (JSONObject tx : mergedMap.values()) {
        mergedArray.put(tx);
    }

    return sortTransactionsByTimestamp(mergedArray);
}


  /**
   * Verifica la validità di una API key Helius effettuando una chiamata JSON-RPC {@code getBalance} sull'indirizzo
   * di sistema Solana {@code 11111111111111111111111111111111}.
   * @param ApiKey API key da validare
   * @return {@code true} se la risposta è andata a buon fine e contiene il campo {@code "result"}, {@code false} altrimenti (inclusi gli errori di rete)
   */
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

        Map<String, String> tokenAccountOwnerBatch = CostruisciMappaTokenAccountOwner(transactions);

        //Prezzi di tutta l'importazione a gruppi (DefiLlama per i token SPL, exchange per SOL), prima che
        //InserisciMonete li chieda uno per uno
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliSolana(transactions, walletAddress), "import SOL", null);

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
            String formattedTimestamp = FunzioniDate.ConvertiDatadaLongAlSecondo(timestamp * 1000);
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
            if (VarCondivise.LogJsonDefi) {
                System.out.println("\nTimestamp: " + formattedTimestamp + " - Transazione #" + (i + 1) + " - Signature: " + signature);
            }
            trans.DataOra = formattedTimestamp;
            if (VarCondivise.LogJsonDefi) {
                System.out.println("Descrizione: " + description);
            }
            String AddressNoWallet = "";

            //SOL entrati o usciti dai conti token del wallet stesso (affitto alla creazione, rimborso alla chiusura):
            //restano del wallet, quindi non devono finire nella gamba SOL dello scambio
            BigDecimal SolContiPropri = VariazioneSolContiTokenPropri(tx, walletAddress);

            JSONArray AccountChanges = tx.optJSONArray("accountData");
            BigDecimal NativeSOL = new BigDecimal(0);
            if (AccountChanges != null && AccountChanges.length() > 0) {
                if (VarCondivise.LogJsonDefi) {
                    System.out.println("AccountChanges:");
                }
                for (int j = 0; j < AccountChanges.length(); j++) {
                    JSONObject AccountChange = AccountChanges.getJSONObject(j);
                    String account = AccountChange.optString("account", "N/A");
                    BigDecimal nativeSolAmount = AccountChange.optBigDecimal("nativeBalanceChange", BigDecimal.ZERO).divide(new BigDecimal(1_000_000_000));

                    if (account.equalsIgnoreCase(walletAddress) && nativeSolAmount.compareTo(BigDecimal.ZERO) != 0) {
                        //Se arrivo qua vuol dire che ho dei nativeSOL da gestire
                        //Il totale dei NativeSol Movimentati sarà quindi quello che vedo nel change meno le fee
                        NativeSOL = nativeSolAmount.add(fee).add(SolContiPropri);
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
                            if (userAccount.isBlank() || userAccount.equalsIgnoreCase("N/A")) {
                                String tokenAccount = tokenBalanceChange.optString("tokenAccount", "");
                                userAccount = RisolviOwnerTokenAccount(tokenAccount, signature, tokenAccountOwnerBatch);
                            }
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
                                        //Il wSOL non è un token a sé: quando paga il wallet i suoi lamport sono già in SolContiPropri,
                                        //il controllo sul mint copre anche il caso in cui getAsset non restituisca il simbolo
                                        if (!mint.equals(MINT_WSOL) && !tokenSymbol.equalsIgnoreCase("SOL")) {
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


    static final String MINT_WSOL = "So11111111111111111111111111111111111111112";
    private static final Set<String> PROGRAMMI_TOKEN = Set.of(
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",  //SPL Token
            "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"); //Token-2022
    private static final String PROGRAMMA_ATA = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";

    /**
     * Variazione, in SOL, dei conti token di proprietà del wallet in una transazione firmata dal wallet stesso.
     * <p>
     * Ogni conto token Solana trattiene un deposito in SOL (l'"affitto", 0,00203928 per un conto SPL): lo paga chi crea
     * il conto e lo restituisce la chiusura. Quando il conto è del wallet quel SOL resta del wallet, ma Helius lo riporta
     * dentro {@code nativeBalanceChange} del wallet, e finiva nella gamba SOL dello scambio (5,5 SOL ceduti diventavano
     * 5,50203928) o diventava da solo una gamba ceduta. Sommando alla variazione del wallet quella dei suoi conti token,
     * apertura e chiusura si annullano e resta solo il SOL uscito davvero verso altri, compreso l'affitto pagato per il
     * conto di un destinatario, che è perso.
     * <p>
     * Vale solo se il wallet paga la transazione: se il conto lo apre un altro (chi manda un token), l'affitto non è
     * uscito dal wallet e contarlo lo farebbe comparire come SOL ricevuto. I lamport di un conto wSOL del wallet sono
     * sommati allo stesso modo, ed è giusto, perché il wSOL è SOL.
     * @param tx transazione nel formato "enhanced" di Helius
     * @param walletAddress indirizzo del wallet
     * @return variazione in SOL (positiva se i conti propri hanno ricevuto lamport), zero se il wallet non paga la transazione
     */
    static BigDecimal VariazioneSolContiTokenPropri(JSONObject tx, String walletAddress) {
        if (!tx.optString("feePayer", "").equalsIgnoreCase(walletAddress)) return BigDecimal.ZERO;
        Set<String> conti = ContiTokenDelWallet(tx, walletAddress);
        JSONArray AccountChanges = tx.optJSONArray("accountData");
        if (conti.isEmpty() || AccountChanges == null) return BigDecimal.ZERO;
        BigDecimal lamport = BigDecimal.ZERO;
        for (int j = 0; j < AccountChanges.length(); j++) {
            JSONObject AccountChange = AccountChanges.getJSONObject(j);
            if (conti.contains(AccountChange.optString("account", ""))) {
                lamport = lamport.add(AccountChange.optBigDecimal("nativeBalanceChange", BigDecimal.ZERO));
            }
        }
        return lamport.divide(new BigDecimal(1_000_000_000));
    }

    /**
     * Conti token di cui il wallet è proprietario tra quelli toccati dalla transazione. Il proprietario di un conto token
     * non è chi lo paga: si legge dai saldi token ({@code userAccount}) e, per i conti senza variazione di saldo (un conto
     * aperto vuoto, o chiuso), dalle istruzioni del programma token e del programma ATA, interne comprese.
     */
    static Set<String> ContiTokenDelWallet(JSONObject tx, String walletAddress) {
        Set<String> conti = new HashSet<>();
        JSONArray AccountChanges = tx.optJSONArray("accountData");
        if (AccountChanges != null) {
            for (int j = 0; j < AccountChanges.length(); j++) {
                JSONArray tokenBalanceChanges = AccountChanges.getJSONObject(j).optJSONArray("tokenBalanceChanges");
                if (tokenBalanceChanges == null) continue;
                for (int k = 0; k < tokenBalanceChanges.length(); k++) {
                    JSONObject tokenBalanceChange = tokenBalanceChanges.getJSONObject(k);
                    if (tokenBalanceChange.optString("userAccount", "").equalsIgnoreCase(walletAddress)) {
                        conti.add(tokenBalanceChange.optString("tokenAccount", ""));
                    }
                }
            }
        }
        byte[] walletBytes = DecodificaBase58(walletAddress);
        JSONArray istruzioni = tx.optJSONArray("instructions");
        if (istruzioni != null) {
            for (int i = 0; i < istruzioni.length(); i++) {
                JSONObject istruzione = istruzioni.getJSONObject(i);
                AggiungiContoDaIstruzione(istruzione, walletAddress, walletBytes, conti);
                JSONArray interne = istruzione.optJSONArray("innerInstructions");
                if (interne == null) continue;
                for (int k = 0; k < interne.length(); k++) {
                    AggiungiContoDaIstruzione(interne.getJSONObject(k), walletAddress, walletBytes, conti);
                }
            }
        }
        conti.remove("");
        conti.removeIf(c -> c.equalsIgnoreCase(walletAddress));
        return conti;
    }

    private static void AggiungiContoDaIstruzione(JSONObject istruzione, String walletAddress, byte[] walletBytes, Set<String> conti) {
        String programma = istruzione.optString("programId", "");
        JSONArray account = istruzione.optJSONArray("accounts");
        if (account == null) return;
        if (programma.equals(PROGRAMMA_ATA)) {
            //create e createIdempotent: [pagatore, conto, proprietario, mint, ...]
            if (account.length() > 2 && account.optString(2).equalsIgnoreCase(walletAddress)) {
                conti.add(account.optString(1));
            }
            return;
        }
        if (!PROGRAMMI_TOKEN.contains(programma) || account.length() == 0) return;
        byte[] dati = DecodificaBase58(istruzione.optString("data", ""));
        if (dati == null || dati.length == 0) return;
        switch (dati[0]) {
            //initializeAccount [conto, mint, proprietario, rent] e closeAccount [conto, destinazione, proprietario]
            case 1, 9 -> {
                if (account.length() > 2 && account.optString(2).equalsIgnoreCase(walletAddress)) {
                    conti.add(account.optString(0));
                }
            }
            //initializeAccount2 e 3: il proprietario è nei dati, dopo il primo byte
            case 16, 18 -> {
                if (walletBytes != null && dati.length >= 33
                        && Arrays.equals(Arrays.copyOfRange(dati, 1, 33), walletBytes)) {
                    conti.add(account.optString(0));
                }
            }
            default -> { }
        }
    }

    private static final String ALFABETO_BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /** @return i byte rappresentati dalla stringa base58, {@code null} se contiene caratteri non base58 */
    static byte[] DecodificaBase58(String testo) {
        if (testo == null) return null;
        java.math.BigInteger valore = java.math.BigInteger.ZERO;
        java.math.BigInteger base = java.math.BigInteger.valueOf(58);
        int zeriIniziali = 0;
        boolean inizio = true;
        for (char c : testo.toCharArray()) {
            int cifra = ALFABETO_BASE58.indexOf(c);
            if (cifra < 0) return null;
            if (inizio && cifra == 0) zeriIniziali++;
            else inizio = false;
            valore = valore.multiply(base).add(java.math.BigInteger.valueOf(cifra));
        }
        byte[] corpo = valore.signum() == 0 ? new byte[0] : valore.toByteArray();
        if (corpo.length > 1 && corpo[0] == 0) corpo = Arrays.copyOfRange(corpo, 1, corpo.length);
        byte[] risultato = new byte[zeriIniziali + corpo.length];
        System.arraycopy(corpo, 0, risultato, zeriIniziali, corpo.length);
        return risultato;
    }

    private static Map<String, String> CostruisciMappaTokenAccountOwner(JSONArray transactions) {
        Map<String, String> mappa = new HashMap<>();
        for (int i = 0; i < transactions.length(); i++) {
            JSONArray AccountChanges = transactions.getJSONObject(i).optJSONArray("accountData");
            if (AccountChanges == null) continue;
            for (int j = 0; j < AccountChanges.length(); j++) {
                JSONArray tokenBalanceChanges = AccountChanges.getJSONObject(j).optJSONArray("tokenBalanceChanges");
                if (tokenBalanceChanges == null) continue;
                for (int k = 0; k < tokenBalanceChanges.length(); k++) {
                    JSONObject tokenBalanceChange = tokenBalanceChanges.getJSONObject(k);
                    String userAccount = tokenBalanceChange.optString("userAccount", "");
                    String tokenAccount = tokenBalanceChange.optString("tokenAccount", "");
                    if (!userAccount.isBlank() && !tokenAccount.isBlank()) {
                        mappa.put(tokenAccount, userAccount);
                    }
                }
            }
        }
        return mappa;
    }

    private static String RisolviOwnerTokenAccount(String tokenAccount, String signature, Map<String, String> tokenAccountOwnerBatch) {
        if (tokenAccount.isBlank()) return "";
        String owner = null;
        try {
            owner = getAccountOwner(tokenAccount);
        } catch (IOException ex) {
            Logger.getLogger(Trans_Solana.class.getName()).log(Level.WARNING, "Errore interrogando getAccountInfo per tokenAccount " + tokenAccount, ex);
        }
        if (owner == null) {
            owner = tokenAccountOwnerBatch.get(tokenAccount);
        }
        if (owner == null) {
            Logger.getLogger(Trans_Solana.class.getName()).log(Level.WARNING, "Owner non risolvibile per tokenAccount {0} nella transazione {1}: il movimento potrebbe essere classificato in modo errato", new Object[]{tokenAccount, signature});
            return "";
        }
        return owner;
    }

    private static final int MAX_RETRY_GET_ACCOUNT_OWNER = 3;

    private static String getAccountOwner(String tokenAccount) throws IOException {
        String jsonPayload = "{" +
                "\"jsonrpc\":\"2.0\"," +
                "\"id\":\"text\"," +
                "\"method\":\"getAccountInfo\"," +
                "\"params\": [\"" + tokenAccount + "\", {\"encoding\": \"jsonParsed\"}]}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        // Usa l'endpoint RPC Helius (già pagato con HELIUS_API_KEY) invece del nodo pubblico:
        // quest'ultimo è pesantemente rate-limitato e causava perdite silenziose di movimenti
        // sugli import massivi quando Helius non fornisce già "userAccount" in tokenBalanceChanges.
        Request request = new Request.Builder()
                .url(HELIUS_RPC_URL2 + "?api-key=" + HELIUS_API_KEY)
                .post(body)
                .build();

        IOException ultimoErrore = null;
        for (int tentativo = 1; tentativo <= MAX_RETRY_GET_ACCOUNT_OWNER; tentativo++) {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonString = response.body().string();
                    if (VarCondivise.LogJsonDefi) {
                        System.out.println(jsonString);
                    }
                    JSONObject jsonResponse = new JSONObject(jsonString);
                    JSONObject result = jsonResponse.optJSONObject("result");
                    JSONObject value = result != null ? result.optJSONObject("value") : null;
                    if (value != null) {
                        JSONObject data = value.optJSONObject("data");
                        JSONObject parsed = data != null ? data.optJSONObject("parsed") : null;
                        JSONObject info = parsed != null ? parsed.optJSONObject("info") : null;
                        if (info != null) {
                            return info.optString("owner", null);
                        }
                    }
                    return null;
                }
            } catch (IOException ex) {
                ultimoErrore = ex;
            }
            if (tentativo < MAX_RETRY_GET_ACCOUNT_OWNER) {
                try {
                    Thread.sleep(500L * tentativo);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (ultimoErrore != null) {
            throw ultimoErrore;
        }
        return null;
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
                if (VarCondivise.LogJsonDefi) {
                    System.out.println("Risposta JSON completa getTokenName: " + jsonString);
                }
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






