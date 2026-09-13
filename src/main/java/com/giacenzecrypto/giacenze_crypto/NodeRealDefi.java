package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Provider DeFi NodeReal (MegaNode) per BSC, usato al posto di Moralis su una chain che non ha nessun
 * explorer compatibile Etherscan gratuito.
 *
 * <p><b>Non e' un secondo importatore: e' un adattatore.</b> {@link #Scarica} restituisce lo stesso
 * {@code Object[]{numero, JSONArray}} di {@link Importazioni#DeFi_RitornaTransazioniEtherscan}, e le righe
 * dentro quell'array hanno i <b>nomi di campo di Etherscan</b>. Tutto cio' che sta a valle — le cinque fasi
 * di {@code DeFi_RitornaTransazioni}, la costruzione delle {@link TransazioneDefi}, i prezzi — non sa che
 * esiste NodeReal e non va toccato. Era il punto che contava: un secondo percorso con regole proprie
 * sarebbe un secondo posto in cui possono nascondersi i difetti sottili che finora sono stati tutti nella
 * strada Moralis.
 *
 * <p>Il protocollo e' JSON-RPC 2.0 in POST su {@code https://bsc-mainnet.nodereal.io/v1/<chiave>}, quindi
 * <b>la chiave sta in un segmento di percorso</b> e non in un parametro: vedi
 * {@link DocumentiFonte#UrlSenzaChiave}, che ha una regola apposta, senza la quale la chiave finirebbe in
 * chiaro nel documento di origine.
 *
 * <p>Cose misurate sul campo (13/09/2026, chiave reale, piano Free) e su cui il codice si appoggia:
 * <ul>
 *   <li>la finestra massima e' <b>2.000.000 di blocchi</b> e non i 100.000 dichiarati dalla
 *       documentazione; il confronto del server e' stretto, quindi si usa {@link #AMPIEZZA_FINESTRA}
 *       = 1.999.999;</li>
 *   <li>{@code fromBlock} senza {@code toBlock} viene rifiutato ({@code toBlock param missing}), ma
 *       <b>omettendoli entrambi</b> si scorre l'intera storia col solo {@code pageKey}: e' la strada usata
 *       alla prima importazione, dove una finestratura da blocco 1 sprecherebbe decine di richieste su
 *       intervalli vuoti;</li>
 *   <li>{@code nr_getTransactionByAddress} prende un solo {@code address} e copre <b>entrambe le
 *       direzioni nella stessa risposta</b> (verificato: 505 uscite e 495 entrate in un'unica pagina),
 *       mentre {@code nr_getAssetTransfers} vuole {@code fromAddress} oppure {@code toAddress} e costa
 *       quindi il doppio delle chiamate a parita' di CU;</li>
 *   <li>il {@code pageKey} e' base64 di {@code <categoria>:<blocco>_<indice>}: avanza per categoria oltre
 *       che per blocco, quindi <b>si pagina fino al pageKey vuoto</b> e non ci si puo' fermare al primo
 *       blocco gia' noto.</li>
 * </ul>
 */
public class NodeRealDefi {

    /** Valore del provider in {@code PROVIDERDEFI} e in {@code DeFi_ProviderEffettivo}. */
    public static final String PROVIDER = "NODEREAL";

    /** Chiave dell'opzione in {@code database.mv.db} che contiene la API key NodeReal. */
    public static final String OPZIONE_APIKEY = "ApiKey_NodeReal";

    /**
     * Ampiezza massima della finestra di blocchi per richiesta. Il server rifiuta con
     * {@code range must be less than 2000000}, e il confronto e' stretto: 2.000.000 esatti vengono
     * rifiutati, 1.999.999 no.
     */
    static final int AMPIEZZA_FINESTRA = 1999999;

    /** Record per pagina, il massimo accettato ({@code maxCount} = {@code 0x3e8}). */
    static final int RECORD_PER_PAGINA = 1000;

    /**
     * Intervallo minimo fra due richieste, come cortesia verso il servizio e non come limite misurato.
     * <p>Sulla carta il piano Free concede 150 CUPS e ogni chiamata costa 250 CU, cioe' una richiesta
     * ogni 1,7 s in regime sostenuto; <b>nella prova quel tetto non e' stato applicato</b> — 12 richieste
     * consecutive senza nessuna pausa hanno risposto tutte {@code 200}, e la storia intera di un wallet
     * da 762 transazioni si e' scaricata in 16 s. Una pausa da 1,7 s aggiungerebbe minuti a una prima
     * importazione per difendersi da un limite mai incontrato, quindi si tiene una pausa breve: non
     * martella il servizio e non paga un costo che non serve. Se un giorno comparissero dei 429, e'
     * questo il numero da alzare a 1700.
     */
    static final long INTERVALLO_MINIMO_MS = 300;

    /** Numero massimo di pagine per finestra, guardia contro un {@code pageKey} che non avanza. */
    static final int MAX_PAGINE = 2000;

    /**
     * Traduce l'azione Etherscan nella categoria NodeReal corrispondente.
     * @return la categoria, oppure {@code null} se l'azione non ha corrispondente
     */
    static String Categoria(String Tipo) {
        if (Tipo == null) return null;
        switch (Tipo.toLowerCase()) {
            case "txlist": return "external";
            case "txlistinternal": return "internal";
            case "tokentx": return "20";
            case "tokennfttx": return "721";
            case "token1155tx": return "1155";
            default: return null;
        }
    }

    /**
     * Endpoint della rete. Oggi solo BSC: {@code nr_getTransactionByAddress} esiste su BSC ed Ethereum
     * mainnet, ma su Ethereum il piano gratuito di Etherscan copre gia' tutto e un provider in piu' da
     * mantenere non porterebbe niente.
     * @return l'URL completo di chiave, oppure {@code null} se la rete non e' gestita
     */
    static String Endpoint(String Rete, String apiKey) {
        if (Rete == null || apiKey == null || apiKey.isBlank()) return null;
        if (Rete.equalsIgnoreCase("BSC")) return "https://bsc-mainnet.nodereal.io/v1/" + apiKey.trim();
        return null;
    }

    /** @return {@code true} se la rete puo' essere scaricata da NodeReal */
    public static boolean ReteSupportata(String Rete) {
        return Rete != null && Rete.equalsIgnoreCase("BSC");
    }

    //=====================================================================================================
    //=== CONVERSIONE : da record NodeReal a riga con i nomi di campo di Etherscan
    //=====================================================================================================

    /**
     * Converte un valore esadecimale NodeReal ({@code 0x…}) nella stringa decimale che il parser di
     * {@code Importazioni} si aspetta: quel codice fa {@code new BigDecimal(getString("value"))}, che su
     * un {@code 0x…} lancerebbe.
     * @return la cifra in base 10, {@code "0"} se il campo manca o non e' leggibile
     */
    static String Decimale(Object Grezzo) {
        if (Grezzo == null || Grezzo == JSONObject.NULL) return "0";
        String v = String.valueOf(Grezzo).trim();
        if (v.isEmpty()) return "0";
        try {
            if (v.toLowerCase().startsWith("0x")) {
                String cifre = v.substring(2);
                if (cifre.isEmpty()) return "0";
                return new BigInteger(cifre, 16).toString();
            }
            return new BigInteger(v).toString();
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    /** Legge un campo trattando allo stesso modo assente, {@code null} JSON e stringa vuota. */
    static String Campo(JSONObject R, String Nome) {
        if (R == null || !R.has(Nome) || R.isNull(Nome)) return "";
        return String.valueOf(R.get(Nome)).trim();
    }

    /**
     * Converte un record NodeReal nelle righe con i nomi di campo di Etherscan attese dalla fase
     * corrispondente di {@code DeFi_RitornaTransazioni}.
     *
     * <p>Un record puo' produrre <b>piu' di una riga</b>: un trasferimento ERC-1155 porta
     * {@code erc1155Metadata} come elenco di coppie {@code tokenId}/{@code value}, mentre Etherscan emette
     * una riga per ogni tokenId. Le altre categorie producono sempre una riga sola.
     *
     * <p>Tre conversioni sono quelle in cui un errore non si vedrebbe:
     * <ul>
     *   <li><b>{@code isError} e' invertito</b> rispetto a {@code receiptsStatus}: per Etherscan
     *       {@code "0"} vuol dire riuscita, per NodeReal e' {@code receiptsStatus == 1}. Scambiarli
     *       marcherebbe come fallita ogni transazione riuscita, senza nessun errore da nessuna parte;</li>
     *   <li><b>i valori sono esadecimali</b> e vanno emessi in base 10 ({@link #Decimale});</li>
     *   <li><b>{@code blockTimeStamp} e' un numero</b> e va emesso come stringa, perche' il parser fa
     *       {@code Long.parseLong(getString("timeStamp"))} e {@code getString} su un numero lancia.</li>
     * </ul>
     */
    static JSONArray ConvertiRecord(JSONObject R, String Tipo) {
        JSONArray Righe = new JSONArray();
        if (R == null) return Righe;
        String Base_hash = Campo(R, "hash");
        if (Base_hash.isEmpty()) return Righe;   //senza hash la riga non e' accorpabile a nulla

        if ("token1155tx".equalsIgnoreCase(Tipo)) {
            JSONArray Meta = R.optJSONArray("erc1155Metadata");
            if (Meta == null || Meta.isEmpty()) {
                Righe.put(RigaBase(R, Tipo, "", ""));
            } else {
                for (int i = 0; i < Meta.length(); i++) {
                    JSONObject M = Meta.optJSONObject(i);
                    if (M == null) continue;
                    Righe.put(RigaBase(R, Tipo, Decimale(M.opt("tokenId")), Decimale(M.opt("value"))));
                }
            }
            return Righe;
        }
        Righe.put(RigaBase(R, Tipo, Decimale(R.opt("erc721TokenId")), ""));
        return Righe;
    }

    /**
     * Costruisce la singola riga in formato Etherscan. {@code TokenId} e {@code TokenValue} valgono solo
     * per le categorie NFT ed ERC-1155 e vengono ignorati altrove.
     */
    private static JSONObject RigaBase(JSONObject R, String Tipo, String TokenId, String TokenValue) {
        JSONObject T = new JSONObject();
        T.put("hash", Campo(R, "hash"));
        T.put("blockNumber", Decimale(R.opt("blockNum")));
        //blockTimeStamp arriva come numero: il parser fa Long.parseLong(getString(...)), quindi stringa
        T.put("timeStamp", String.valueOf(R.optLong("blockTimeStamp", 0L)));
        T.put("from", Campo(R, "from"));
        //"to" manca sulle creazioni di contratto: il parser fa getString("to"), che su un campo assente
        //lancerebbe, quindi va sempre emesso
        T.put("to", Campo(R, "to"));
        //Etherscan: "0" = riuscita. NodeReal: receiptsStatus 1 = riuscita. La negazione e' voluta.
        T.put("isError", R.optInt("receiptsStatus", 1) == 1 ? "0" : "1");
        T.put("gasUsed", Decimale(R.opt("gasUsed")));
        T.put("gasPrice", Decimale(R.opt("gasPrice")));
        //NodeReal non espone functionName; il parser lo legge gia' con optString perche' nemmeno
        //Blockscout lo manda sempre
        T.put("functionName", "");
        T.put("contractAddress", Campo(R, "contractAddress"));
        T.put("tokenSymbol", Campo(R, "asset"));
        T.put("tokenName", Campo(R, "name"));
        T.put("tokenDecimal", Campo(R, "decimal"));
        if ("txlist".equalsIgnoreCase(Tipo) || "txlistinternal".equalsIgnoreCase(Tipo)) {
            T.put("value", Decimale(R.opt("value")));
        } else if ("token1155tx".equalsIgnoreCase(Tipo)) {
            T.put("tokenID", TokenId);
            T.put("tokenValue", TokenValue);
            T.put("value", TokenValue.isEmpty() ? "0" : TokenValue);
        } else if ("tokennfttx".equalsIgnoreCase(Tipo)) {
            T.put("tokenID", TokenId);
            T.put("value", "1");
        } else {
            T.put("value", Decimale(R.opt("value")));
        }
        return T;
    }

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /**
     * Scarica da NodeReal le transazioni di una categoria, restituendole con i nomi di campo di Etherscan.
     *
     * <p>Firma e valore di ritorno sono identici a
     * {@link Importazioni#DeFi_RitornaTransazioniEtherscan}, cosi' il chiamante cambia solo la riga che
     * sceglie il fetcher.
     *
     * @param apiKey chiave NodeReal (gia' decifrata)
     * @param Rete identificativo della rete (oggi solo {@code BSC})
     * @param walletAddress indirizzo da interrogare
     * @param Tipo azione in stile Etherscan ({@code txlist}, {@code tokentx}, …)
     * @param BloccoIniziale blocco da cui partire; {@code <= 1} significa "tutta la storia"
     * @param ccc componente parent per gli eventuali dialog di errore
     * @param progressb finestra di avanzamento, usata anche per l'annullamento
     * @return {@code [0]} numero di righe, {@code [1]} {@link JSONArray} delle righe; {@code null} in caso
     *         di errore o annullamento
     */
    public static Object[] Scarica(String apiKey, String Rete, String walletAddress, String Tipo,
            String BloccoIniziale, Component ccc, Download progressb) {
        String Url = Endpoint(Rete, apiKey);
        String Cat = Categoria(Tipo);
        if (Url == null || Cat == null) {
            LoggerGC.ScriviErrore("NodeReal non gestisce la combinazione rete=" + Rete + " azione=" + Tipo);
            return null;
        }
        JSONArray Righe = new JSONArray();
        try {
            long Da = 1;
            try {
                Da = Long.parseLong(BloccoIniziale.trim());
            } catch (Exception ignore) {
                //blocco non numerico: si riparte da tutta la storia, che e' il caso peggiore ma mai errato
            }
            boolean StoriaIntera = Da <= 1;
            long Ultimo = StoriaIntera ? 0 : BloccoCorrente(Url);
            if (!StoriaIntera && Ultimo <= 0) {
                //senza il blocco corrente non si puo' chiudere la finestra: meglio la storia intera che
                //una finestra sbagliata, che scarterebbe in silenzio i movimenti piu' recenti
                StoriaIntera = true;
            }
            long Inizio = Da;
            while (true) {
                long Fine = Math.min(Inizio + AMPIEZZA_FINESTRA, Ultimo);
                String PageKey = "";
                int Pagina = 0;
                while (true) {
                    if (progressb != null && progressb.FineThread()) return null;
                    JSONObject Richiesta = ComponiRichiesta(walletAddress, Cat, StoriaIntera, Inizio, Fine, PageKey);
                    String Risposta = Chiama(Url, Richiesta, Tipo, ccc, progressb);
                    if (Risposta == null) return null;
                    JSONObject Json = new JSONObject(Risposta);
                    if (Json.has("error")) {
                        Errore(ccc, progressb, "NodeReal ha risposto con un errore per '" + Tipo + "' ("
                                + walletAddress + ") :\n" + Json.getJSONObject("error").optString("message", Risposta));
                        return null;
                    }
                    JSONObject Ris = Json.optJSONObject("result");
                    JSONArray Trasferimenti = (Ris == null) ? null : Ris.optJSONArray("transfers");
                    if (Trasferimenti != null) {
                        for (int i = 0; i < Trasferimenti.length(); i++) {
                            JSONObject Rec = Trasferimenti.optJSONObject(i);
                            JSONArray Convertite = ConvertiRecord(Rec, Tipo);
                            for (int k = 0; k < Convertite.length(); k++) Righe.put(Convertite.get(k));
                        }
                    }
                    String Prossima = (Ris == null) ? "" : Ris.optString("pageKey", "");
                    Pagina++;
                    if (Prossima.isEmpty() || Prossima.equals(PageKey) || Pagina >= MAX_PAGINE) break;
                    PageKey = Prossima;
                }
                if (StoriaIntera || Fine >= Ultimo) break;
                Inizio = Fine + 1;
            }
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
            Errore(ccc, progressb, "Errore durante lo scaricamento da NodeReal per la tipologia " + Tipo
                    + "\n" + e);
            return null;
        }
        Object Ritorno[] = new Object[2];
        Ritorno[0] = Righe.length();
        Ritorno[1] = Righe;
        return Ritorno;
    }

    /** Compone il corpo JSON-RPC della richiesta. */
    static JSONObject ComponiRichiesta(String walletAddress, String Categoria, boolean StoriaIntera,
            long Inizio, long Fine, String PageKey) {
        JSONObject P = new JSONObject();
        P.put("address", walletAddress);
        P.put("category", new JSONArray().put(Categoria));
        P.put("order", "asc");
        P.put("maxCount", "0x" + Integer.toHexString(RECORD_PER_PAGINA));
        if (!StoriaIntera) {
            //fromBlock senza toBlock viene rifiutato: vanno sempre insieme
            P.put("fromBlock", "0x" + Long.toHexString(Inizio));
            P.put("toBlock", "0x" + Long.toHexString(Fine));
        }
        if (PageKey != null && !PageKey.isEmpty()) P.put("pageKey", PageKey);
        JSONObject Richiesta = new JSONObject();
        Richiesta.put("jsonrpc", "2.0");
        Richiesta.put("id", 1);
        Richiesta.put("method", "nr_getTransactionByAddress");
        Richiesta.put("params", new JSONArray().put(P));
        return Richiesta;
    }

    /** @return il numero dell'ultimo blocco, {@code 0} se non e' stato possibile leggerlo */
    static long BloccoCorrente(String Url) {
        try {
            JSONObject R = new JSONObject();
            R.put("jsonrpc", "2.0");
            R.put("id", 1);
            R.put("method", "eth_blockNumber");
            R.put("params", new JSONArray());
            String Risposta = Chiama(Url, R, "eth_blockNumber", null, null);
            if (Risposta == null) return 0;
            return Long.parseLong(Decimale(new JSONObject(Risposta).optString("result", "0x0")));
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
            return 0;
        }
    }

    private static long UltimaChiamata = 0;

    /** Esegue una POST JSON-RPC, rispettando l'intervallo minimo fra richieste. */
    private static String Chiama(String Url, JSONObject Corpo, String Tipo, Component ccc, Download progressb)
            throws Exception {
        long Attesa = INTERVALLO_MINIMO_MS - (System.currentTimeMillis() - UltimaChiamata);
        if (Attesa > 0 && Attesa <= INTERVALLO_MINIMO_MS) TimeUnit.MILLISECONDS.sleep(Attesa);
        UltimaChiamata = System.currentTimeMillis();
        URL url = new URI(Url).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        byte[] dati = Corpo.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = con.getOutputStream()) {
            os.write(dati);
        }
        int codice = con.getResponseCode();
        InputStream stream = (codice >= 400) ? con.getErrorStream() : con.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String riga;
                while ((riga = in.readLine()) != null) sb.append(riga);
            }
        }
        String Risposta = sb.toString();
        if (VarCondivise.LogJsonDefi) System.out.println(Risposta);
        //La chiave e' dentro l'URL: DocumentiFonte.UrlSenzaChiave ha la regola per il segmento di percorso
        DocumentiFonte.AggiungiRispostaWeb(Importazioni.DocumentoFonteCorrente, Url, Risposta);
        if (codice >= 400) {
            Errore(ccc, progressb, "Errore HTTP " + codice + " da NodeReal per '" + Tipo + "'\n"
                    + (Risposta.isBlank() ? "(nessun dettaglio nella risposta)" : Risposta));
            return null;
        }
        return Risposta;
    }

    /** Chiude l'avanzamento e mostra il messaggio, come fanno gli altri percorsi explorer. */
    private static void Errore(Component ccc, Download progressb, String Messaggio) {
        LoggerGC.ScriviErrore(Messaggio);
        if (progressb != null) progressb.ChiudiFinestra();
        if (ccc != null) {
            JOptionPane.showConfirmDialog(ccc, Messaggio + "\nRiprovare in un secondo momento.",
                    "Errore", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null);
        }
    }
}
