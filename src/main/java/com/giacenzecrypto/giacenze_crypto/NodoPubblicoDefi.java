package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Component;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Provider DeFi senza chiavi API per le reti EVM che non hanno piu' nessun explorer gratuito: si legge dai nodi
 * pubblici. Oggi solo BASE (dal 10/10/2026 Blockscout la da' solo a pagamento, Etherscan anche, Routescan non la
 * copre: misure in {@code nocommit/Documentazione/Analisi_Provider_BSC.md}). Le reti stanno in {@link #RETI}: una
 * rete nuova e' una {@link Configurazione} in piu', non una classe in piu'.
 *
 * <p><b>Come {@link NodeRealDefi}, e' un adattatore</b>: {@link #Scarica} restituisce le righe di ciascuna delle
 * cinque azioni Etherscan ({@code txlist}, {@code tokentx}, {@code tokennfttx}, {@code token1155tx},
 * {@code txlistinternal}) coi nomi di campo di Etherscan, e tutto cio' che sta a valle in
 * {@code Importazioni.DeFi_RitornaTransazioni} non sa da dove vengano.
 *
 * <p><b>Come {@link Trans_XLayer}, legge lo stato e non lo storico.</b> Nessun nodo gratuito da' i log su tratti
 * lunghi (il nodo ufficiale di Base mai, Tenderly 1.000 blocchi, drpc 100, Blast 10) e la rete ha decine di
 * milioni di blocchi. Si legge invece lo stato del wallet a un blocco (nonce del conto, saldo della moneta della
 * rete, saldi dei token seguiti) con una richiesta sola, e dove lo stato e' uguale ai due estremi di un tratto non
 * e' successo nulla, altrimenti il tratto si divide a meta' fino al singolo blocco. Del blocco cambiato si leggono
 * le transazioni, i log del wallet e le ricevute.
 *
 * <p>Cosa garantisce e cosa no:
 * <ul>
 *   <li><b>ogni transazione inviata dal wallet</b> (approve e fallite comprese, commissione compresa la quota L1):
 *       il nonce cambia sempre. Per questo un wallet con codice (smart wallet, delega EIP-7702) viene rifiutato:
 *       il suo nonce non conta le operazioni;</li>
 *   <li><b>ogni cambio della moneta della rete</b>: il saldo e' nello stato. Quello che le transazioni del blocco
 *       non spiegano e' ETH interno: se nel blocco c'e' una sola transazione candidata gli si attribuisce la
 *       differenza esatta, altrimenti si chiede la traccia. Una differenza che resta senza transazione diventa un
 *       avviso, mai un movimento senza hash;</li>
 *   <li><b>i token seguiti</b>: l'elenco della rete, quelli gia' nell'archivio per quel wallet (non SCAM) e ogni
 *       token comparso nei log, che si ricerca poi da capo su tutto il tratto. <b>Resta fuori un token ricevuto
 *       senza fare nulla, fuori elenco e mai spostato</b>: e' il limite del metodo, accettato dall'utente il
 *       10/10/2026;</li>
 *   <li>un token il cui saldo cambia senza trasferimenti (interessi, ribasamento) esce dallo stato al primo cambio
 *       senza log, altrimenti la bisezione scenderebbe a ogni blocco.</li>
 * </ul>
 *
 * <p>Una scansione fallita non restituisce nulla di quel wallet (gli altri si importano lo stesso): il blocco di
 * partenza della volta dopo e' l'ultimo movimento scritto, e una parte importata lascerebbe indietro il resto per
 * sempre.
 */
public final class NodoPubblicoDefi {

    private NodoPubblicoDefi() {}

    /** Valore del provider in {@code PROVIDERDEFI} e in {@code DeFi_ProviderEffettivo} (lo stesso di X Layer). */
    public static final String PROVIDER = "NODO PUBBLICO";

    static final String TOPIC_TRANSFER = OKX_WalletCarta.TOPIC_TRANSFER;
    /** {@code TransferSingle(address operator, address from, address to, uint256 id, uint256 value)} */
    static final String TOPIC_TRANSFER_SINGLE = "0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62";
    /** {@code TransferBatch(address operator, address from, address to, uint256[] ids, uint256[] values)} */
    static final String TOPIC_TRANSFER_BATCH = "0x4a39dc06d4c0dbc64b70af90fd698a233a518aa5d07e595d983b8c0526c8f7fb";

    /** {@code Deposit(address indexed dst, uint256 wad)} del token avvolto (WETH9): ETH avvolto, nessun Transfer */
    static final String TOPIC_DEPOSITO_AVVOLTO = "0xe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c";
    /** {@code Withdrawal(address indexed src, uint256 wad)} del token avvolto: ETH svolto, nessun Transfer */
    static final String TOPIC_PRELIEVO_AVVOLTO = "0x7fcf532c15f0a6db0bd6d0e038bea71d30d808c7d98cb3bf7268a95bf5081b65";

    static final String SEL_NAME = "06fdde03";

    /** Oltre questo numero di blocchi cambiati la scansione si ferma: un wallet cosi' non e' da nodo pubblico. */
    static final int MASSIMO_BLOCCHI = 20000;
    /** Giri di ricerca dei token scoperti nei log (ogni giro puo' scoprirne altri). */
    static final int MASSIMO_GIRI_TOKEN = 10;

    /** A che cosa serve una richiesta: ogni nodo pubblico sa fare cose diverse. */
    enum Uso { STATO, LOG, TRACCE }

    /**
     * Una rete servita dai nodi pubblici.
     *
     * @param nodiStato nodi con lo stato storico completo (archivio), per lo stato, i blocchi e le ricevute
     * @param nodiLog nodi che rispondono a {@code eth_getLogs} almeno su un blocco
     * @param nodiTracce nodi che danno {@code debug_traceTransaction}
     * @param tokenNoti token sempre seguiti nello stato (indirizzi minuscoli)
     * @param tokenAvvolti token avvolti della moneta della rete (WETH9): avvolgere e svolgere non emettono
     *        {@code Transfer} ma {@code Deposit}/{@code Withdrawal}, che si leggono solo da questi contratti, perche'
     *        altri contratti usano la stessa firma per cose che non sono token
     */
    record Configurazione(String rete, List<String> nodiStato, List<String> nodiLog, List<String> nodiTracce,
            List<String> tokenNoti, List<String> tokenAvvolti) {}

    /**
     * Le reti. BASE, misure del 10/10/2026: stato storico su base.org, Tenderly e Blast (40 letture su 40 ciascuno),
     * drpc limita (28 su 40) e resta di riserva; {@code eth_getLogs} il nodo ufficiale non lo da' mai; le tracce solo
     * drpc, una transazione per volta. I token sono i principali della rete, simbolo e decimali letti sul nodo.
     */
    static final Map<String, Configurazione> RETI = Map.of(
            "BASE", new Configurazione("BASE",
                    List.of("https://mainnet.base.org", "https://base.gateway.tenderly.co",
                            "https://base-mainnet.public.blastapi.io", "https://base.drpc.org"),
                    List.of("https://base.gateway.tenderly.co", "https://base.drpc.org",
                            "https://base-mainnet.public.blastapi.io"),
                    List.of("https://base.drpc.org"),
                    List.of("0x833589fcd6edb6e08f4c7c32d4f71b54bda02913", //USDC
                            "0xd9aaec86b65d86f6a7b5b1b0c42ffa531710b6ca", //USDbC
                            "0x4200000000000000000000000000000000000006", //WETH
                            "0x2ae3f1ec7f1f5012cfeab0185bfc7aa3cf0dec22", //cbETH
                            "0xcbb7c0000ab88b473b1f5afd9ef808440eed33bf", //cbBTC
                            "0x50c5725949a6f0c72e6c4a641f24049a917db0cb", //DAI
                            "0x940181a94a35a4569e4529a3cdfb74e38fd98631", //AERO
                            "0xfde4c96c8593536e31f229ea8f37b2ada2699bb2", //USDT
                            "0x60a3e35cc302bfa44cb288bc5a4f316fdb1adb42", //EURC
                            "0xc1cba3fcea344f92d9239c08c0568f6f2f0ee452", //wstETH
                            "0x04c0599ae5a44757c0af6f9ec3b93da8976c150a", //weETH
                            "0xb6fe221fe9eef5aba221c348ba20a1bf5e73624c", //rETH
                            "0x4ed4e862860bed51a9570b96d89af5e1b0efefed", //DEGEN
                            "0x532f27101965dd16442e59d40670faf5ebb142e4", //BRETT
                            "0x0b3e328455c4059eeb9e3f84b5543f74e24e7e1b", //VIRTUAL
                            "0x820c137fa70c8691f0e44dc420a5e53c168921dc", //USDS
                            "0xecac9c5f704e954931349da37f60e39f515c11c1", //LBTC
                            "0x1111111111166b7fe7bd91427724b487980afc69", //ZORA
                            "0x236aa50979d5f3de3bd1eeb40e81137f22ab794b", //tBTC
                            "0x22e6966b799c4d5b13be962e1d117b56327fda66"), //SNX
                    List.of("0x4200000000000000000000000000000000000006"))); //WETH

    /** @return {@code true} se la rete si scarica dai nodi pubblici */
    public static boolean ReteSupportata(String Rete) {
        return Rete != null && RETI.containsKey(Rete.toUpperCase());
    }

    //=====================================================================================================
    //=== ADATTATORE PER DeFi_RitornaTransazioni
    //=====================================================================================================

    /** Risultato della scansione di un wallet: le cinque liste Etherscan, gli avvisi, se e' completa. */
    record Esito(JSONArray txlist, JSONArray tokentx, JSONArray tokennfttx, JSONArray token1155tx,
            JSONArray txlistinternal, List<String> avvisi, boolean completo, int letture, int blocchi) {

        JSONArray Righe(String Tipo) {
            return switch (Tipo.toLowerCase()) {
                case "txlist" -> txlist;
                case "tokentx" -> tokentx;
                case "tokennfttx" -> tokennfttx;
                case "token1155tx" -> token1155tx;
                case "txlistinternal" -> txlistinternal;
                default -> null;
            };
        }
    }

    private static String ChiaveUltima = null;
    private static Esito Ultimo = null;

    /**
     * Le righe di un'azione Etherscan per il wallet, con la stessa firma e lo stesso ritorno di
     * {@link NodeRealDefi#Scarica}. La scansione legge tutte e cinque le azioni insieme: la fa la prima chiamata
     * ({@code txlist}, che il chiamante chiede sempre per prima) e le altre quattro leggono il risultato tenuto da
     * parte per lo stesso documento, wallet e blocco.
     *
     * @return {@code [0]} numero di righe, {@code [1]} {@link JSONArray}; nessuna riga se la scansione di questo
     *         wallet non e' completa (errore dei nodi, wallet con codice), {@code null} solo se l'utente ha interrotto
     */
    public static Object[] Scarica(String Rete, String walletAddress, String Tipo, String BloccoIniziale,
            Component ccc, Download progressb) {
        long Da = 1;
        try {
            Da = Long.parseLong(BloccoIniziale.trim());
        } catch (Exception ignore) {
            //blocco non numerico: tutta la storia, il caso piu' lungo ma mai sbagliato
        }
        String Chiave = Importazioni.DocumentoFonteCorrente + "|" + Rete.toUpperCase() + "|"
                + walletAddress.toLowerCase() + "|" + Da;
        synchronized (NodoPubblicoDefi.class) {
            if ("txlist".equalsIgnoreCase(Tipo) || Ultimo == null || !Chiave.equals(ChiaveUltima)) {
                Ultimo = null;
                ChiaveUltima = null;
                if (progressb != null) progressb.setIndeterminate(true);
                Esito e = Scansiona(Rete, walletAddress, Da, true,
                        progressb == null ? null : progressb::FineThread,
                        progressb == null ? null : progressb::SetMessaggioAvanzamento,
                        Importazioni.DocumentoFonteCorrente);
                if (progressb != null) progressb.setIndeterminate(false);
                for (String a : e.avvisi()) LoggerGC.ScriviErrore("Nodo pubblico " + Rete + " " + walletAddress + ": " + a);
                if (!e.completo()) {
                    //Interrompi: si ferma tutta l'importazione, come per gli altri provider
                    if (progressb != null && progressb.FineThread()) return null;
                    //Altrimenti si salta solo questo wallet, come fa X Layer: un wallet rifiutato per sempre (smart
                    //wallet) non deve impedire l'importazione degli altri. Nulla va perso, il suo ultimo blocco
                    //importato non si sposta e la volta dopo si riparte da li'.
                    String Messaggio = "Scansione del wallet " + walletAddress + " (" + Rete + ") dai nodi pubblici "
                            + "non completata, nessun movimento importato per questo wallet.\n" + String.join("\n", e.avvisi());
                    if (ccc != null) JOptionPane.showConfirmDialog(ccc, Messaggio, "Nodo pubblico " + Rete,
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
                    e = new Esito(new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(),
                            e.avvisi(), false, e.letture(), 0);
                } else if (ccc != null && !e.avvisi().isEmpty()) {
                    JOptionPane.showConfirmDialog(ccc, "Wallet " + walletAddress + " (" + Rete + "):\n"
                            + String.join("\n", e.avvisi()), "Nodo pubblico " + Rete, JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null);
                }
                Ultimo = e;
                ChiaveUltima = Chiave;
            }
            JSONArray Righe = Ultimo.Righe(Tipo);
            if (Righe == null) return null;
            return new Object[]{Righe.length(), Righe};
        }
    }

    /**
     * Scansione di un wallet coi nodi pubblici della rete.
     *
     * @param bloccoDa primo blocco da leggere ({@code <= 1}: tutta la storia)
     * @param tokenDallArchivio se seguire anche i token che l'archivio ha gia' per quel wallet
     */
    static Esito Scansiona(String Rete, String wallet, long bloccoDa, boolean tokenDallArchivio,
            BooleanSupplier interrotto, Consumer<String> avanzamento, int idDocumento) {
        Configurazione conf = RETI.get(Rete.toUpperCase());
        if (conf == null) {
            return new Esito(new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(),
                    List.of("Rete " + Rete + " non gestita dai nodi pubblici."), false, 0, 0);
        }
        Set<String> token = new LinkedHashSet<>(conf.tokenNoti());
        if (tokenDallArchivio) token.addAll(TokenDallArchivio(wallet, Rete));
        return new Scansione(conf, new NodiPubblici(conf), wallet, interrotto, avanzamento, idDocumento)
                .Esegui(bloccoDa, token);
    }

    /**
     * I token che l'archivio ha gia' per un wallet su una rete, esclusi gli SCAM: il loro {@code balanceOf} puo'
     * essere finto o cambiare da solo, e farebbe scendere la bisezione dove non succede nulla.
     */
    static Set<String> TokenDallArchivio(String wallet, String Rete) {
        Set<String> token = new LinkedHashSet<>();
        if (Principale.MappaCryptoWallet == null) return token;
        String w = wallet.toLowerCase();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v.length < 35 || !v[3].toLowerCase().startsWith(w) || !Rete.equalsIgnoreCase(v[34])) continue;
            AggiungiTokenArchivio(token, v[8], v[9], v[26]);
            AggiungiTokenArchivio(token, v[11], v[12], v[28]);
        }
        return token;
    }

    private static void AggiungiTokenArchivio(Set<String> token, String Simbolo, String Tipo, String Address) {
        if (Address == null || Simbolo == null || Simbolo.isBlank()) return;
        String a = Address.trim().toLowerCase();
        if (!a.matches("0x[0-9a-f]{40}") || Funzioni.isSCAM(Simbolo) || !"Crypto".equalsIgnoreCase(Tipo)) return;
        token.add(a);
    }

    //=====================================================================================================
    //=== ACCESSO AI NODI
    //=====================================================================================================

    /** Accesso ai nodi, separato perche' i test lo sostituiscono. */
    interface Nodo {
        JsonElement chiama(Uso uso, String metodo, JsonArray parametri) throws Exception;

        /** Piu' chiamate in una richiesta sola; i risultati nell'ordine delle chiamate. */
        default List<JsonElement> insieme(Uso uso, List<String> metodi, List<JsonArray> parametri) throws Exception {
            List<JsonElement> r = new ArrayList<>();
            for (int i = 0; i < metodi.size(); i++) r.add(chiama(uso, metodi.get(i), parametri.get(i)));
            return r;
        }
    }

    /**
     * I nodi pubblici di una rete, scelti per uso. Una richiesta che fallisce (limite di richieste, errore, risposta
     * non JSON) passa al nodo successivo, e dopo un giro completo si aspetta e si riprova: il nodo che ha risposto
     * resta il primo per quell'uso. Le richieste allo stesso nodo sono distanziate di {@link #INTERVALLO_MS}.
     */
    static final class NodiPubblici implements Nodo {
        static final long INTERVALLO_MS = 120;
        static final int GIRI = 4;
        private final Map<Uso, List<String>> nodi = new EnumMap<>(Uso.class);
        private final Map<Uso, Integer> preferito = new EnumMap<>(Uso.class);
        private final Map<String, Long> ultimaRichiesta = new HashMap<>();
        private final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build();
        private static final MediaType JSON = MediaType.parse("application/json");

        NodiPubblici(Configurazione c) {
            nodi.put(Uso.STATO, c.nodiStato());
            nodi.put(Uso.LOG, c.nodiLog());
            nodi.put(Uso.TRACCE, c.nodiTracce());
        }

        @Override
        public JsonElement chiama(Uso uso, String metodo, JsonArray parametri) throws Exception {
            return insieme(uso, List.of(metodo), List.of(parametri)).get(0);
        }

        @Override
        public List<JsonElement> insieme(Uso uso, List<String> metodi, List<JsonArray> parametri) throws Exception {
            List<String> elenco = nodi.get(uso);
            Exception ultimo = null;
            int inizio = preferito.getOrDefault(uso, 0);
            for (int giro = 0; giro < GIRI; giro++) {
                for (int k = 0; k < elenco.size(); k++) {
                    int i = (inizio + k) % elenco.size();
                    try {
                        List<JsonElement> r = Invia(elenco.get(i), metodi, parametri);
                        preferito.put(uso, i);
                        return r;
                    } catch (ErroreDefinitivo e) {
                        throw e;
                    } catch (Exception e) {
                        ultimo = e;
                        LoggerGC.logInfo("Nodo pubblico " + elenco.get(i) + " (" + metodi.get(0) + "): " + e.getMessage());
                    }
                }
                Thread.sleep(2000L * (giro + 1));
            }
            throw new Exception("nessun nodo pubblico ha risposto a " + metodi.get(0) + " ("
                    + (ultimo == null ? "" : ultimo.getMessage()) + ")");
        }

        private List<JsonElement> Invia(String url, List<String> metodi, List<JsonArray> parametri) throws Exception {
            synchronized (ultimaRichiesta) {
                long attesa = ultimaRichiesta.getOrDefault(url, 0L) + INTERVALLO_MS - System.currentTimeMillis();
                if (attesa > 0) Thread.sleep(attesa);
                ultimaRichiesta.put(url, System.currentTimeMillis());
            }
            JsonArray lotto = new JsonArray();
            for (int i = 0; i < metodi.size(); i++) {
                JsonObject r = new JsonObject();
                r.addProperty("jsonrpc", "2.0");
                r.addProperty("id", i);
                r.addProperty("method", metodi.get(i));
                r.add("params", parametri.get(i));
                lotto.add(r);
            }
            String corpoRichiesta = metodi.size() == 1 ? lotto.get(0).toString() : lotto.toString();
            Request req = new Request.Builder().url(url).post(RequestBody.create(corpoRichiesta, JSON)).build();
            try (Response risposta = client.newCall(req).execute()) {
                String corpo = risposta.body() != null ? risposta.body().string() : "";
                if (!risposta.isSuccessful()) throw new Exception("HTTP " + risposta.code() + " " + Breve(corpo));
                JsonElement j;
                try {
                    j = JsonParser.parseString(corpo);
                } catch (Exception e) {
                    throw new Exception("risposta non JSON: " + Breve(corpo));
                }
                JsonElement[] ris = new JsonElement[metodi.size()];
                List<JsonElement> elementi = new ArrayList<>();
                if (j.isJsonArray()) j.getAsJsonArray().forEach(elementi::add);
                else elementi.add(j);
                for (JsonElement el : elementi) {
                    if (!el.isJsonObject()) throw new Exception("risposta inattesa: " + Breve(corpo));
                    JsonObject o = el.getAsJsonObject();
                    if (o.has("error") && !o.get("error").isJsonNull()) {
                        String errore = o.get("error").toString();
                        //Un revert e' la risposta del contratto, non un guasto del nodo: un altro nodo darebbe la stessa
                        if (errore.toLowerCase().contains("revert")) throw new ErroreDefinitivo(Breve(errore));
                        throw new Exception(Breve(errore));
                    }
                    int id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsInt() : 0;
                    if (id < 0 || id >= ris.length) throw new Exception("id inatteso nella risposta");
                    ris[id] = o.has("result") ? o.get("result") : JsonNull.INSTANCE;
                }
                for (JsonElement r : ris) if (r == null) throw new Exception("risposta incompleta");
                return List.of(ris);
            }
        }

        /** Errore che un altro nodo ripeterebbe uguale (revert): non si riprova. */
        static final class ErroreDefinitivo extends Exception {
            ErroreDefinitivo(String m) {
                super(m);
            }
        }

        private static String Breve(String s) {
            return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) : s);
        }
    }

    //=====================================================================================================
    //=== SCANSIONE
    //=====================================================================================================

    /** Stato del wallet dopo un blocco: nonce e moneta della rete (se letti) e i saldi dei token letti. */
    record Stato(BigInteger nonce, BigInteger moneta, Map<String, BigInteger> token) {}

    /** Una scansione: tiene i token seguiti, quelli esclusi, i blocchi gia' letti e le righe prodotte. */
    static final class Scansione {
        private final Configurazione conf;
        private final Nodo nodo;
        private final String w;
        private final String parolaW;
        private final BooleanSupplier interrotto;
        private final Consumer<String> avanzamento;
        private final int idDocumento;

        final List<String> avvisi = new ArrayList<>();
        final JSONArray txlist = new JSONArray(), tokentx = new JSONArray(), tokennfttx = new JSONArray(),
                token1155tx = new JSONArray(), txlistinternal = new JSONArray();
        final Set<String> esclusi = new HashSet<>();
        final Set<String> conosciuti = new HashSet<>();
        final Set<String> nuovi = new LinkedHashSet<>();
        final Set<Long> elaborati = new HashSet<>();
        final Map<String, String[]> metadati = new HashMap<>();
        int letture = 0;
        private long inizio, fine;
        private int ultimaDecina = -1;
        private String fase = "";

        Scansione(Configurazione conf, Nodo nodo, String wallet, BooleanSupplier interrotto,
                Consumer<String> avanzamento, int idDocumento) {
            this.conf = conf;
            this.nodo = nodo;
            this.w = wallet.toLowerCase();
            this.parolaW = "0x" + Trans_XLayer.Parola(w);
            this.interrotto = interrotto;
            this.avanzamento = avanzamento;
            this.idDocumento = idDocumento;
        }

        private boolean Fermo() {
            return interrotto != null && interrotto.getAsBoolean();
        }

        private void log(String testo) {
            System.out.println(testo);
            stato(testo);
        }

        private void stato(String testo) {
            if (avanzamento != null) avanzamento.accept(testo);
        }

        private Esito Fine(boolean completo) {
            return new Esito(txlist, tokentx, tokennfttx, token1155tx, txlistinternal, avvisi, completo, letture,
                    elaborati.size());
        }

        Esito Esegui(long bloccoDa, Set<String> tokenIniziali) {
            String r = conf.rete();
            try {
                stato(r + ": controllo del wallet sul nodo pubblico...");
                JsonArray par = new JsonArray();
                par.add(w);
                par.add("latest");
                String codice = nodo.chiama(Uso.STATO, "eth_getCode", par).getAsString();
                if (codice != null && codice.length() > 2) {
                    //Smart wallet (ERC-4337) o EOA con delega EIP-7702: le sue operazioni non muovono il nonce del
                    //conto, quindi la bisezione non le vedrebbe. Meglio rifiutare che importare una parte.
                    avvisi.add("Il wallet ha del codice (smart wallet o delega EIP-7702): la lettura dai nodi pubblici "
                            + "vede solo i conti normali, e importerebbe una parte dei movimenti.");
                    return Fine(false);
                }
                long ultimo = Long.decode(nodo.chiama(Uso.STATO, "eth_blockNumber", new JsonArray()).getAsString()) - 3;
                if (bloccoDa > ultimo) {
                    log(r + " " + w + ": nessun blocco nuovo dopo il " + (bloccoDa - 1) + ".");
                    return Fine(true);
                }
                long da = Math.max(0, bloccoDa - 1);
                List<String> seguiti = new ArrayList<>(tokenIniziali);
                conosciuti.addAll(seguiti);
                log(r + " " + w + ": scansione dal blocco " + (da + 1) + " al " + ultimo + " (" + (ultimo - da)
                        + " blocchi), " + seguiti.size() + " token seguiti.");
                fase = "Scansione degli stati";
                if (!Bisezione(da, ultimo, true, seguiti)) return Fine(false);
                int giro = 0;
                while (!nuovi.isEmpty()) {
                    if (++giro > MASSIMO_GIRI_TOKEN) {
                        avvisi.add("Troppi token nuovi scoperti di seguito: la ricerca dei loro movimenti si e' fermata "
                                + "al giro " + MASSIMO_GIRI_TOKEN + ".");
                        break;
                    }
                    List<String> lotto = new ArrayList<>(nuovi);
                    nuovi.clear();
                    log(r + ": ricerca dei movimenti di " + lotto.size() + " token scoperti nei log (giro " + giro + ").");
                    fase = "Token scoperti, giro " + giro;
                    if (!Bisezione(da, ultimo, false, lotto)) return Fine(false);
                }
                log(r + " " + w + ": scansione finita, " + letture + " letture dello stato, " + elaborati.size()
                        + " blocchi con movimenti, " + (txlist.length() + tokentx.length() + tokennfttx.length()
                        + token1155tx.length() + txlistinternal.length()) + " righe.");
                return Fine(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Fine(false);
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
                avvisi.add("Nodi pubblici di " + r + ": " + ex.getMessage());
                return Fine(false);
            }
        }

        /** Una bisezione completa su ({@code da}, {@code a}]. @return falso se interrotta o fermata */
        private boolean Bisezione(long da, long a, boolean base, List<String> token) throws Exception {
            inizio = da;
            fine = a;
            ultimaDecina = -1;
            Stato sDa = LeggiStato(da, base, token);
            Stato sA = LeggiStato(a, base, token);
            Dividi(da, a, sDa, sA, base, token);
            if (Fermo()) {
                log(conf.rete() + ": scansione interrotta.");
                return false;
            }
            if (elaborati.size() > MASSIMO_BLOCCHI) {
                avvisi.add("Oltre " + MASSIMO_BLOCCHI + " blocchi con movimenti: il wallet e' troppo attivo per i nodi pubblici.");
                return false;
            }
            return true;
        }

        /**
         * Divide ({@code da}, {@code a}] finche' lo stato cambia, fino al singolo blocco, che si legge subito: un
         * token escluso li' (saldo che cambia da solo) non conta piu' nei confronti dei tratti che restano.
         */
        private void Dividi(long da, long a, Stato sDa, Stato sA, boolean base, List<String> token) throws Exception {
            if (Fermo() || elaborati.size() > MASSIMO_BLOCCHI) return;
            if (Uguali(sDa, sA, base, token)) {
                Coperto(a);
                return;
            }
            if (a - da == 1) {
                if (!elaborati.contains(a)) ElaboraBlocco(a, sDa, sA, base, token);
                Coperto(a);
                return;
            }
            long m = (da + a) >>> 1;
            Stato sM = LeggiStato(m, base, token);
            Dividi(da, m, sDa, sM, base, token);
            Dividi(m, a, sM, sA, base, token);
        }

        private boolean Uguali(Stato x, Stato y, boolean base, List<String> token) {
            if (base && (!x.nonce().equals(y.nonce()) || !x.moneta().equals(y.moneta()))) return false;
            for (String t : token) {
                if (esclusi.contains(t)) continue;
                if (!x.token().getOrDefault(t, BigInteger.ZERO).equals(y.token().getOrDefault(t, BigInteger.ZERO))) return false;
            }
            return true;
        }

        private void Coperto(long blocco) {
            int perc = fine > inizio ? (int) ((blocco - inizio) * 100 / (fine - inizio)) : 100;
            String testo = fase + ": " + perc + "% (blocco " + blocco + " di " + fine + "), " + letture + " letture, "
                    + elaborati.size() + " blocchi con movimenti";
            if (perc / 10 > ultimaDecina) {
                ultimaDecina = perc / 10;
                System.out.println(conf.rete() + ": " + testo);
            }
            stato(testo);
        }

        /** Stato dopo il blocco: nonce e moneta della rete se {@code base}, poi i saldi dei token. */
        Stato LeggiStato(long blocco, boolean base, List<String> token) throws Exception {
            letture++;
            String hex = "0x" + Long.toHexString(blocco);
            List<String[]> chiamate = new ArrayList<>();
            if (base) chiamate.add(new String[]{Trans_XLayer.MULTICALL3, Trans_XLayer.SEL_GET_ETH_BALANCE + Trans_XLayer.Parola(w)});
            for (String t : token) chiamate.add(new String[]{t, Trans_XLayer.SEL_BALANCE_OF + Trans_XLayer.Parola(w)});
            JsonObject call = new JsonObject();
            call.addProperty("to", Trans_XLayer.MULTICALL3);
            call.addProperty("data", Trans_XLayer.Aggregate3(chiamate));
            JsonArray pCall = new JsonArray();
            pCall.add(call);
            pCall.add(hex);
            List<BigInteger> valori;
            BigInteger nonce = BigInteger.ZERO;
            if (base) {
                JsonArray pNonce = new JsonArray();
                pNonce.add(w);
                pNonce.add(hex);
                List<JsonElement> r = nodo.insieme(Uso.STATO, List.of("eth_getTransactionCount", "eth_call"), List.of(pNonce, pCall));
                nonce = new BigInteger(r.get(0).getAsString().substring(2), 16);
                valori = Trans_XLayer.DecodificaAggregate3(r.get(1).getAsString());
            } else {
                valori = Trans_XLayer.DecodificaAggregate3(nodo.chiama(Uso.STATO, "eth_call", pCall).getAsString());
            }
            //Prima che Multicall3 esistesse la chiamata restituisce 0x: tutto a zero, come un wallet vuoto
            while (valori.size() < chiamate.size()) valori.add(BigInteger.ZERO);
            int i = 0;
            BigInteger moneta = base ? valori.get(i++) : BigInteger.ZERO;
            Map<String, BigInteger> saldi = new HashMap<>();
            for (String t : token) saldi.put(t, valori.get(i++));
            return new Stato(nonce, moneta, saldi);
        }

        //---------------------------------------------------------------------------------------------------
        //--- LETTURA DI UN BLOCCO CAMBIATO
        //---------------------------------------------------------------------------------------------------

        /** Legge il blocco in cui lo stato e' cambiato e ne scrive le righe. */
        void ElaboraBlocco(long b, Stato prima, Stato dopo, boolean base, List<String> token) throws Exception {
            elaborati.add(b);
            String hex = "0x" + Long.toHexString(b);
            stato(fase + ": lettura del blocco " + b);
            JsonArray pBlocco = new JsonArray();
            pBlocco.add(hex);
            pBlocco.add(true);
            JsonObject blk = nodo.chiama(Uso.STATO, "eth_getBlockByNumber", pBlocco).getAsJsonObject();
            long ts = Long.decode(Testo(blk, "timestamp"));
            String tsTesto = String.valueOf(ts);
            Map<String, JsonObject> txBlocco = new LinkedHashMap<>();
            List<JsonObject> rilevanti = new ArrayList<>();
            for (JsonElement el : blk.getAsJsonArray("transactions")) {
                JsonObject tx = el.getAsJsonObject();
                String hash = Testo(tx, "hash").toLowerCase();
                txBlocco.put(hash, tx);
                String da = Testo(tx, "from").toLowerCase(), a = Testo(tx, "to").toLowerCase();
                if (da.equals(w) || (a.equals(w) && Esadecimale(Testo(tx, "value")).signum() > 0)) rilevanti.add(tx);
            }

            //Log del wallet nel blocco: Transfer (ERC-20 ed ERC-721) da e verso, TransferSingle/Batch da e verso
            //e Deposit/Withdrawal del token avvolto, che avvolgendo e svolgendo non emette Transfer
            List<String> metodiLog = new ArrayList<>(List.of("eth_getLogs", "eth_getLogs", "eth_getLogs", "eth_getLogs"));
            List<JsonArray> filtriLog = new ArrayList<>(List.of(FiltroLog(hex, new String[]{TOPIC_TRANSFER}, 1),
                    FiltroLog(hex, new String[]{TOPIC_TRANSFER}, 2),
                    FiltroLog(hex, new String[]{TOPIC_TRANSFER_SINGLE, TOPIC_TRANSFER_BATCH}, 2),
                    FiltroLog(hex, new String[]{TOPIC_TRANSFER_SINGLE, TOPIC_TRANSFER_BATCH}, 3)));
            if (!conf.tokenAvvolti().isEmpty()) {
                JsonArray f = FiltroLog(hex, new String[]{TOPIC_DEPOSITO_AVVOLTO, TOPIC_PRELIEVO_AVVOLTO}, 1);
                JsonArray indirizzi = new JsonArray();
                conf.tokenAvvolti().forEach(indirizzi::add);
                f.get(0).getAsJsonObject().add("address", indirizzi);
                metodiLog.add("eth_getLogs");
                filtriLog.add(f);
            }
            List<JsonElement> risLog = nodo.insieme(Uso.LOG, metodiLog, filtriLog);
            Map<String, JsonObject> logs = new TreeMap<>();
            for (JsonElement lista : risLog) {
                for (JsonElement el : lista.getAsJsonArray()) {
                    JsonObject l = el.getAsJsonObject();
                    if (l.has("removed") && l.get("removed").getAsBoolean()) continue;
                    long idx = Long.decode(Testo(l, "logIndex"));
                    logs.put(String.format("%08d", idx), l);
                }
            }

            //Ricevute delle transazioni del wallet e dei trasferimenti diretti verso il wallet
            Map<String, JsonObject> ricevute = new HashMap<>();
            if (!rilevanti.isEmpty()) {
                List<String> metodi = new ArrayList<>();
                List<JsonArray> par = new ArrayList<>();
                for (JsonObject tx : rilevanti) {
                    metodi.add("eth_getTransactionReceipt");
                    JsonArray p = new JsonArray();
                    p.add(Testo(tx, "hash"));
                    par.add(p);
                }
                List<JsonElement> r = nodo.insieme(Uso.STATO, metodi, par);
                for (int i = 0; i < r.size(); i++) {
                    if (r.get(i).isJsonObject()) ricevute.put(Testo(rilevanti.get(i), "hash").toLowerCase(), r.get(i).getAsJsonObject());
                }
            }

            //Righe "txlist" e conto della moneta della rete spiegata dalle transazioni
            BigInteger spiegato = BigInteger.ZERO;
            Set<String> candidati = new LinkedHashSet<>();
            for (JsonObject tx : rilevanti) {
                String hash = Testo(tx, "hash").toLowerCase();
                JsonObject ric = ricevute.get(hash);
                if (ric == null) throw new Exception("ricevuta mancante per una transazione del blocco " + b);
                boolean ok = "0x1".equals(Testo(ric, "status"));
                String da = Testo(tx, "from").toLowerCase(), a = Testo(tx, "to").toLowerCase();
                BigInteger valore = Esadecimale(Testo(tx, "value"));
                BigInteger gasUsato = Esadecimale(Testo(ric, "gasUsed"));
                BigInteger prezzoGas = Esadecimale(Testo(ric, "effectiveGasPrice"));
                BigInteger commissione = Commissione(ric);
                JSONObject riga = new JSONObject();
                riga.put("hash", hash);
                riga.put("blockNumber", String.valueOf(b));
                riga.put("timeStamp", tsTesto);
                riga.put("from", da);
                riga.put("to", a);
                riga.put("value", valore.toString());
                riga.put("isError", ok ? "0" : "1");
                riga.put("txreceipt_status", ok ? "1" : "0");
                riga.put("gasUsed", gasUsato.toString());
                riga.put("gasPrice", prezzoGas.toString());
                //Commissione intera, quota L1 compresa: sulle reti OP-stack gasUsed x gasPrice ne e' solo una parte
                riga.put("commissioneWei", commissione.toString());
                riga.put("functionName", "");
                riga.put("contractAddress", Testo(ric, "contractAddress").toLowerCase());
                txlist.put(riga);
                if (da.equals(w)) {
                    spiegato = spiegato.subtract(commissione);
                    if (ok && !a.equals(w)) spiegato = spiegato.subtract(valore);
                    if (ok) candidati.add(hash);
                } else if (ok) {
                    spiegato = spiegato.add(valore);
                }
            }

            //Righe dei token dai log, e movimento netto di ogni token nel blocco
            Map<String, BigInteger> nettoToken = new HashMap<>();
            for (JsonObject l : logs.values()) {
                String hash = Testo(l, "transactionHash").toLowerCase();
                candidati.add(hash);
                RigheLog(l, hash, b, tsTesto, nettoToken);
            }

            //Moneta della rete: quello che le transazioni non spiegano e' ETH interno
            BigInteger delta;
            if (base) {
                delta = dopo.moneta().subtract(prima.moneta());
            } else {
                delta = SaldoMoneta(b).subtract(SaldoMoneta(b - 1));
            }
            BigInteger residuo = delta.subtract(spiegato);
            if (residuo.signum() != 0) Interne(b, tsTesto, residuo, candidati, txBlocco);

            //Token: un saldo che cambia senza trasferimenti non si puo' seguire con la bisezione
            for (String t : token) {
                if (esclusi.contains(t)) continue;
                BigInteger d = dopo.token().getOrDefault(t, BigInteger.ZERO).subtract(prima.token().getOrDefault(t, BigInteger.ZERO));
                if (d.signum() == 0) continue;
                BigInteger n = nettoToken.get(t);
                if (n == null) {
                    esclusi.add(t);
                    avvisi.add("Il saldo del token " + Simbolo(t) + " (" + t + ") cambia senza trasferimenti (interessi o "
                            + "ribasamento, blocco " + b + "): da li' in poi i suoi movimenti si leggono solo dove il wallet "
                            + "fa altre operazioni.");
                } else if (!n.equals(d)) {
                    avvisi.add("Blocco " + b + ", token " + Simbolo(t) + ": i trasferimenti (" + n + ") non spiegano il cambio "
                            + "di saldo (" + d + ").");
                }
            }
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "Nodo pubblico " + conf.rete(), "blocco " + b,
                    Documento(b, ts, rilevanti, ricevute, logs.values()));
        }

        /** Commissione pagata: gas L2 al prezzo effettivo, piu' la quota L1 e la quota operatore se la ricevuta le ha. */
        static BigInteger Commissione(JsonObject ric) {
            BigInteger gas = Esadecimale(Testo(ric, "gasUsed"));
            BigInteger c = gas.multiply(Esadecimale(Testo(ric, "effectiveGasPrice")));
            c = c.add(Esadecimale(Testo(ric, "l1Fee")));
            if (ric.has("operatorFeeScalar") || ric.has("operatorFeeConstant")) {
                //Isthmus: gasUsed x operatorFeeScalar / 1e6 + operatorFeeConstant
                c = c.add(gas.multiply(Esadecimale(Testo(ric, "operatorFeeScalar"))).divide(BigInteger.valueOf(1_000_000)))
                        .add(Esadecimale(Testo(ric, "operatorFeeConstant")));
            }
            return c;
        }

        private JsonArray FiltroLog(String hex, String[] eventi, int posizioneWallet) {
            JsonObject f = new JsonObject();
            f.addProperty("fromBlock", hex);
            f.addProperty("toBlock", hex);
            JsonArray topics = new JsonArray();
            JsonArray ev = new JsonArray();
            for (String e : eventi) ev.add(e);
            topics.add(ev);
            for (int i = 1; i < posizioneWallet; i++) topics.add(JsonNull.INSTANCE);
            topics.add(parolaW);
            f.add("topics", topics);
            JsonArray p = new JsonArray();
            p.add(f);
            return p;
        }

        /** Le righe di un log di trasferimento che riguarda il wallet. */
        private void RigheLog(JsonObject l, String hash, long b, String ts, Map<String, BigInteger> nettoToken) throws Exception {
            JsonArray tp = l.getAsJsonArray("topics");
            if (tp == null || tp.size() == 0) return;
            String evento = tp.get(0).getAsString().toLowerCase();
            String contratto = Testo(l, "address").toLowerCase();
            String dati = Testo(l, "data");
            if (evento.equals(TOPIC_TRANSFER) && tp.size() >= 3) {
                String da = Trans_XLayer.Indirizzo(tp.get(1).getAsString()), a = Trans_XLayer.Indirizzo(tp.get(2).getAsString());
                //Un trasferimento del wallet a se stesso non sposta nulla
                if (da.equals(w) && a.equals(w)) return;
                String[] m = Metadati(contratto);
                Scoperto(contratto);
                JSONObject riga = RigaToken(hash, b, ts, da, a, contratto, m);
                if (tp.size() >= 4) {
                    //ERC-721: l'id e' il quarto topic, ogni trasferimento sposta un pezzo
                    riga.put("tokenID", new BigInteger(Trans_XLayer.Parola(tp.get(3).getAsString()), 16).toString());
                    riga.put("value", "1");
                    tokennfttx.put(riga);
                    nettoToken.merge(contratto, a.equals(w) ? BigInteger.ONE : BigInteger.ONE.negate(), BigInteger::add);
                } else {
                    BigInteger valore = Trans_XLayer.ParolaDati(dati, 0);
                    riga.put("value", valore.toString());
                    riga.put("tokenDecimal", m[2]);
                    tokentx.put(riga);
                    nettoToken.merge(contratto, a.equals(w) ? valore : valore.negate(), BigInteger::add);
                }
            } else if ((evento.equals(TOPIC_DEPOSITO_AVVOLTO) || evento.equals(TOPIC_PRELIEVO_AVVOLTO)) && tp.size() == 2
                    && conf.tokenAvvolti().contains(contratto) && Trans_XLayer.Indirizzo(tp.get(1).getAsString()).equals(w)) {
                //Come un Transfer fra il contratto e il wallet: e' cosi' che gli explorer lo mostravano
                boolean entrata = evento.equals(TOPIC_DEPOSITO_AVVOLTO);
                String[] m = Metadati(contratto);
                BigInteger valore = Trans_XLayer.ParolaDati(dati, 0);
                JSONObject riga = RigaToken(hash, b, ts, entrata ? contratto : w, entrata ? w : contratto, contratto, m);
                riga.put("value", valore.toString());
                riga.put("tokenDecimal", m[2]);
                tokentx.put(riga);
                nettoToken.merge(contratto, entrata ? valore : valore.negate(), BigInteger::add);
            } else if ((evento.equals(TOPIC_TRANSFER_SINGLE) || evento.equals(TOPIC_TRANSFER_BATCH)) && tp.size() >= 4) {
                String da = Trans_XLayer.Indirizzo(tp.get(2).getAsString()), a = Trans_XLayer.Indirizzo(tp.get(3).getAsString());
                if (da.equals(w) && a.equals(w)) return;
                String[] m = Metadati(contratto);
                List<BigInteger[]> pezzi = new ArrayList<>();
                if (evento.equals(TOPIC_TRANSFER_SINGLE)) {
                    pezzi.add(new BigInteger[]{Trans_XLayer.ParolaDati(dati, 0), Trans_XLayer.ParolaDati(dati, 1)});
                } else {
                    pezzi.addAll(Lotto1155(dati));
                }
                for (BigInteger[] p : pezzi) {
                    //Come Etherscan: una riga per id, quantita' in tokenValue, niente tokenDecimal
                    JSONObject riga = RigaToken(hash, b, ts, da, a, contratto, m);
                    riga.put("tokenID", p[0].toString());
                    riga.put("tokenValue", p[1].toString());
                    token1155tx.put(riga);
                }
            }
        }

        private JSONObject RigaToken(String hash, long b, String ts, String da, String a, String contratto, String[] m) {
            JSONObject riga = new JSONObject();
            riga.put("hash", hash);
            riga.put("blockNumber", String.valueOf(b));
            riga.put("timeStamp", ts);
            riga.put("from", da);
            riga.put("to", a);
            riga.put("contractAddress", contratto);
            riga.put("tokenName", m[0]);
            riga.put("tokenSymbol", m[1]);
            return riga;
        }

        /** Un token visto nei log e non ancora seguito: va cercato da capo su tutto il tratto. */
        private void Scoperto(String contratto) {
            if (conosciuti.add(contratto)) nuovi.add(contratto);
        }

        /** Saldo della moneta della rete dopo un blocco. */
        private BigInteger SaldoMoneta(long blocco) throws Exception {
            JsonArray p = new JsonArray();
            p.add(w);
            p.add("0x" + Long.toHexString(blocco));
            return Esadecimale(nodo.chiama(Uso.STATO, "eth_getBalance", p).getAsString());
        }

        /**
         * La moneta della rete entrata o uscita nel blocco senza una transazione che la porti: ETH interno. Con una
         * sola transazione candidata la differenza e' sua, esatta. Con piu' candidate (o nessuna, e allora si cercano
         * nelle ricevute del blocco quelle che nominano il wallet) si chiede la traccia di ognuna.
         */
        private void Interne(long b, String ts, BigInteger residuo, Set<String> candidati, Map<String, JsonObject> txBlocco)
                throws Exception {
            if (candidati.isEmpty()) {
                JsonArray p = new JsonArray();
                p.add("0x" + Long.toHexString(b));
                JsonElement r = nodo.chiama(Uso.STATO, "eth_getBlockReceipts", p);
                String parola = Trans_XLayer.Parola(w);
                for (JsonElement el : r.getAsJsonArray()) {
                    JsonObject ric = el.getAsJsonObject();
                    for (JsonElement le : ric.getAsJsonArray("logs")) {
                        if (le.toString().toLowerCase().contains(parola)) {
                            candidati.add(Testo(ric, "transactionHash").toLowerCase());
                            break;
                        }
                    }
                }
            }
            if (candidati.size() == 1) {
                String hash = candidati.iterator().next();
                JsonObject tx = txBlocco.get(hash);
                String controparte = tx == null ? "" : Testo(tx, "to").toLowerCase();
                if (controparte.equals(w)) controparte = tx == null ? "" : Testo(tx, "from").toLowerCase();
                txlistinternal.put(RigaInterna(hash, b, ts, residuo.signum() > 0 ? controparte : w,
                        residuo.signum() > 0 ? w : controparte, residuo.abs()));
                return;
            }
            BigInteger tracciato = BigInteger.ZERO;
            for (String hash : candidati) {
                JsonArray p = new JsonArray();
                p.add(hash);
                JsonObject opz = new JsonObject();
                opz.addProperty("tracer", "callTracer");
                p.add(opz);
                JsonObject traccia;
                try {
                    traccia = nodo.chiama(Uso.TRACCE, "debug_traceTransaction", p).getAsJsonObject();
                } catch (Exception e) {
                    avvisi.add("Blocco " + b + ": traccia della transazione " + hash + " non disponibile (" + e.getMessage() + ").");
                    continue;
                }
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "Nodo pubblico " + conf.rete(), "traccia " + hash, traccia.toString());
                List<String[]> chiamate = new ArrayList<>();
                ChiamateConValore(traccia, true, chiamate);
                for (String[] c : chiamate) {
                    BigInteger v = new BigInteger(c[2]);
                    if (c[1].equals(w) && !c[0].equals(w)) tracciato = tracciato.add(v);
                    else if (c[0].equals(w) && !c[1].equals(w)) tracciato = tracciato.subtract(v);
                    else continue;
                    txlistinternal.put(RigaInterna(hash, b, ts, c[0], c[1], v));
                }
            }
            BigInteger resta = residuo.subtract(tracciato);
            if (resta.signum() != 0) {
                avvisi.add("Blocco " + b + ": " + new java.math.BigDecimal(resta, 18).stripTrailingZeros().toPlainString() + " "
                        + MonetaRete() + " entrati o usciti senza una transazione individuata: non importati.");
            }
        }

        /** Le chiamate interne riuscite che spostano moneta, escluso il livello principale (la transazione stessa). */
        private void ChiamateConValore(JsonObject frame, boolean principale, List<String[]> uscita) {
            if (frame.has("error") && !frame.get("error").isJsonNull()) return;
            String tipo = Testo(frame, "type").toUpperCase();
            BigInteger v = Esadecimale(Testo(frame, "value"));
            if (!principale && v.signum() > 0 && !tipo.equals("DELEGATECALL") && !tipo.equals("STATICCALL")) {
                uscita.add(new String[]{Testo(frame, "from").toLowerCase(), Testo(frame, "to").toLowerCase(), v.toString()});
            }
            if (frame.has("calls") && frame.get("calls").isJsonArray()) {
                for (JsonElement c : frame.getAsJsonArray("calls")) ChiamateConValore(c.getAsJsonObject(), false, uscita);
            }
        }

        private JSONObject RigaInterna(String hash, long b, String ts, String da, String a, BigInteger valore) {
            JSONObject riga = new JSONObject();
            riga.put("hash", hash);
            riga.put("blockNumber", String.valueOf(b));
            riga.put("timeStamp", ts);
            riga.put("from", da);
            riga.put("to", a);
            riga.put("value", valore.toString());
            riga.put("isError", "0");
            riga.put("contractAddress", "");
            return riga;
        }

        private String MonetaRete() {
            String[] e = Principale.Mappa_ChainExplorer == null ? null : Principale.Mappa_ChainExplorer.get(conf.rete());
            return e != null && e.length > 2 ? e[2] : "ETH";
        }

        /** Nome, simbolo e decimali di un token letti sul nodo; vuoti se il contratto non li espone. */
        String[] Metadati(String contratto) throws Exception {
            String[] m = metadati.get(contratto);
            if (m != null) return m;
            List<String> metodi = List.of("eth_call", "eth_call", "eth_call");
            List<JsonArray> par = new ArrayList<>();
            for (String sel : new String[]{SEL_NAME, Trans_XLayer.SEL_SYMBOL, Trans_XLayer.SEL_DECIMALS}) {
                JsonObject c = new JsonObject();
                c.addProperty("to", contratto);
                c.addProperty("data", "0x" + sel);
                JsonArray p = new JsonArray();
                p.add(c);
                p.add("latest");
                par.add(p);
            }
            String[] ris = new String[3];
            for (int i = 0; i < 3; i++) {
                //Un contratto che non implementa il metodo fa fallire la chiamata: si legge una per volta,
                //cosi' il fallimento di una non perde le altre
                try {
                    ris[i] = nodo.chiama(Uso.STATO, metodi.get(i), par.get(i)).getAsString();
                } catch (Exception e) {
                    ris[i] = "0x";
                }
            }
            String nome = Stringa(ris[0]), simbolo = Stringa(ris[1]);
            String decimali = "";
            try {
                if (ris[2] != null && ris[2].length() > 2) decimali = new BigInteger(ris[2].substring(2, Math.min(ris[2].length(), 66)), 16).toString();
            } catch (Exception ignore) {
                //decimali non leggibili: vuoto, come Blockscout
            }
            m = new String[]{nome, simbolo, decimali};
            metadati.put(contratto, m);
            return m;
        }

        private String Simbolo(String contratto) {
            String[] m = metadati.get(contratto);
            return m != null && !m[1].isBlank() ? m[1] : contratto;
        }

        private String Documento(long b, long ts, List<JsonObject> rilevanti, Map<String, JsonObject> ricevute,
                java.util.Collection<JsonObject> logs) {
            JsonObject d = new JsonObject();
            d.addProperty("blocco", b);
            d.addProperty("timestamp", ts);
            JsonArray tx = new JsonArray();
            rilevanti.forEach(tx::add);
            d.add("transazioni", tx);
            JsonArray ric = new JsonArray();
            ricevute.values().forEach(ric::add);
            d.add("ricevute", ric);
            JsonArray lg = new JsonArray();
            logs.forEach(lg::add);
            d.add("log", lg);
            return d.toString();
        }
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    /**
     * Stringa ABI restituita da {@code name()}/{@code symbol()}: dinamica, oppure {@code bytes32} nei contratti
     * vecchi. Il testo resta com'e' (spazi, due punti, link): il riconoscimento degli SCAM legge proprio quei nomi,
     * come arrivavano da Blockscout e Moralis. Si tolgono solo i caratteri di controllo.
     */
    static String Stringa(String r) {
        if (r == null || r.length() <= 2) return "";
        String h = r.substring(2);
        byte[] b = null;
        try {
            if (h.length() >= 128) {
                int off = new BigInteger(h.substring(0, 64), 16).intValueExact() * 2;
                int len = new BigInteger(h.substring(off, off + 64), 16).intValueExact();
                if (off + 64 + len * 2 <= h.length()) b = Byte(h.substring(off + 64, off + 64 + len * 2));
            }
        } catch (Exception ignore) {
            b = null;
        }
        if (b == null && h.length() == 64) {
            //bytes32: fino al primo zero
            byte[] tutto = Byte(h);
            int n = 0;
            while (n < tutto.length && tutto[n] != 0) n++;
            b = java.util.Arrays.copyOf(tutto, n);
        }
        if (b == null) return "";
        return new String(b, StandardCharsets.UTF_8).replaceAll("[\\p{Cntrl}\\x{FFFD}]", "").trim();
    }

    private static byte[] Byte(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return b;
    }

    /** Coppie id/quantita' di un {@code TransferBatch}: i dati sono due array dinamici. */
    static List<BigInteger[]> Lotto1155(String dati) {
        List<BigInteger[]> r = new ArrayList<>();
        try {
            String h = dati.substring(2);
            int offId = new BigInteger(h.substring(0, 64), 16).intValueExact() * 2;
            int offVal = new BigInteger(h.substring(64, 128), 16).intValueExact() * 2;
            int n = new BigInteger(h.substring(offId, offId + 64), 16).intValueExact();
            for (int i = 0; i < n; i++) {
                BigInteger id = new BigInteger(h.substring(offId + 64 + i * 64, offId + 128 + i * 64), 16);
                BigInteger v = new BigInteger(h.substring(offVal + 64 + i * 64, offVal + 128 + i * 64), 16);
                r.add(new BigInteger[]{id, v});
            }
        } catch (Exception ignore) {
            //dati malformati: nessuna riga
        }
        return r;
    }

    static BigInteger Esadecimale(String v) {
        if (v == null || v.isBlank()) return BigInteger.ZERO;
        String s = v.trim().toLowerCase();
        if (s.startsWith("0x")) s = s.substring(2);
        return s.isEmpty() ? BigInteger.ZERO : new BigInteger(s, 16);
    }

    static String Testo(JsonObject o, String campo) {
        return o != null && o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }
}
