package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Le reti blockchain riconosciute dal programma, da {@code config/varie/Reti.json}: etichetta, explorer da aprire nel
 * browser, moneta della rete, nomi CoinGecko/DefiLlama, nodi pubblici con lo stato storico, nodi e token dell'import dal
 * nodo pubblico (BASE) e token della carta OKX (XLAYER). Prima erano scritti in sei posti del codice; una rete EVM nuova
 * coperta da Etherscan V2 ora e' una voce del file.
 *
 * <p>Restano nel codice di proposito: l'indirizzo delle API degli explorer (si costruisce da {@code chainidEtherscan},
 * e per CRO/SOL/BTC/XLAYER e' in {@link #API_NON_ETHERSCAN}), perche' a quelle API va la ApiKey dell'utente e un file
 * aggiornato da GitHub senza una versione non deve poterla mandare altrove; i provider e gli indirizzi Blockscout
 * ({@code Importazioni.DeFi_ProviderDefault}, {@code DeFi_ProviderBlockscoutUrl}), che sono comportamento; GoPlus.
 * Nessuna lista di ripiego nel codice: il default e' la copia nel jar.</p>
 */
final class Reti {

    private Reti() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "Reti";

    static final String VOCE_VUOTA_WALLET = "--- nessuna selezione ---";
    static final String VOCE_VUOTA_IMPORT = "----------";

    /**
     * Reti importate da una classe propria. Nel file sono {@code "importNativo": true} invece di {@code "defi": true}:
     * le versioni senza la classe ignorano la chiave e le vedono solo riconosciute, invece di mandarne i wallet a
     * Etherscan. Cosi' la rete nuova non costa un {@code versioneMinimaApp} su tutto il file.
     */
    static final java.util.Set<String> IMPORT_NATIVI = java.util.Set.of(Trans_Algorand.RETE, Trans_XRPL.RETE, Trans_Tron.RETE);

    /** API degli explorer che non sono Etherscan V2: restano nel codice perche' ricevono la ApiKey. */
    private static final Map<String, String> API_NON_ETHERSCAN = Map.of(
            "CRO", "https://explorer-api.cronos.org/mainnet/api/v2",
            "SOL", "https://solscan.io/",
            "BTC", "https://mempool.space/api",
            "XLAYER", OKX_WalletCarta.RPC_XLAYER);

    /**
     * Una rete.
     *
     * @param defi rete con explorer integrato: wallet DeFi inseribili, voce di {@code Mappa_ChainExplorer}
     * @param chainidEtherscan chain id per l'API Etherscan V2, {@code 0} se la rete non e' su Etherscan
     * @param nodoPubblico nodi e token dell'importazione dai nodi pubblici ({@link NodoPubblicoDefi}), o {@code null}
     * @param tokenNodo monete lette nello stato del wallet della carta OKX ({@link Trans_XLayer}), vuoto per le altre reti
     */
    record Rete(String id, String nome, boolean defi, long chainidEtherscan, String simbolo, String coingecko,
            String defillama, String urlTransazione, String urlTokenWallet, String urlWallet, List<String> nodiStorico,
            boolean senzaStorico, NodoPubblicoDefi.Configurazione nodoPubblico, List<Trans_XLayer.Token> tokenNodo) {

        /** {@code "Arbitrum (ARB)"}: e' anche la chiave dei loghi, non solo una didascalia. */
        String etichetta() {
            return nome + " (" + id + ")";
        }

        /** L'indirizzo delle API dell'explorer ({@code Mappa_ChainExplorer[0]}), vuoto se la rete non ne ha. */
        String api() {
            if (chainidEtherscan > 0) return "https://api.etherscan.io/v2/api?chainid=" + chainidEtherscan;
            return API_NON_ETHERSCAN.getOrDefault(id, "");
        }
    }

    record Dati(List<Rete> elenco, Map<String, Rete> perId) {}

    private static volatile Dati dati;

    private static Dati dati() {
        Dati d = dati;
        if (d == null) {
            d = Carica();
            dati = d;
        }
        return d;
    }

    /** Tutte le reti, nell'ordine del file. */
    static List<Rete> Elenco() {
        return dati().elenco();
    }

    /** @return la rete, o {@code null} se sconosciuta (maiuscole e minuscole indifferenti) */
    static Rete Get(String id) {
        return id == null ? null : dati().perId().get(id.trim().toUpperCase());
    }

    /**
     * Rilegge il file e rifa' le due mappe di {@code Principale}: chiamata quando l'aggiornamento in background porta
     * una copia nuova. Le mappe si sostituiscono, non si svuotano, cosi' chi le sta leggendo non le vede mai a meta'.
     */
    static void Ricarica() {
        dati = Carica();
        CompilaMappaChain();
        CompilaMappaRetiSupportate();
    }

    /** Riempie {@code Principale.Mappa_ChainExplorer}: {api, "", moneta della rete, nome CoinGecko, nome DefiLlama}. */
    static void CompilaMappaChain() {
        Map<String, String[]> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Rete r : Elenco()) {
            if (r.defi()) m.put(r.id(), new String[]{r.api(), "", r.simbolo(), r.coingecko(), r.defillama()});
        }
        Principale.Mappa_ChainExplorer = m;
    }

    /** Riempie {@code Principale.MappaRetiSupportate} con tutte le reti del file. */
    static void CompilaMappaRetiSupportate() {
        Map<String, String> m = new TreeMap<>();
        for (Rete r : Elenco()) m.put(r.id(), "");
        Principale.MappaRetiSupportate = m;
    }

    /** Le voci della tendina di Gestione Wallet: le reti con explorer, nell'ordine del file. */
    static String[] EtichetteComboWallet() {
        List<String> v = new ArrayList<>();
        v.add(VOCE_VUOTA_WALLET);
        for (Rete r : Elenco()) if (r.defi()) v.add(r.etichetta());
        return v.toArray(new String[0]);
    }

    /** Le voci della finestra di importazione: tutte le reti (la finestra le ordina per nome). */
    static String[] EtichetteImport() {
        List<String> v = new ArrayList<>();
        v.add(VOCE_VUOTA_IMPORT);
        for (Rete r : Elenco()) v.add(r.etichetta());
        return v.toArray(new String[0]);
    }

    /** {@code true} se la rete ha un explorer integrato (i wallet DeFi si possono inserire). */
    static boolean isDefi(String id) {
        Rete r = Get(id);
        return r != null && r.defi();
    }

    /** Sostituisce i segnaposto {@code {hash}}, {@code {wallet}}, {@code {token}}; {@code null} se il modello manca. */
    static String Url(String modello, String hash, String wallet, String token) {
        if (modello == null || modello.isBlank()) return null;
        return modello.replace("{hash}", hash == null ? "" : hash)
                .replace("{wallet}", wallet == null ? "" : wallet)
                .replace("{token}", token == null ? "" : token);
    }

    private static Dati Carica() {
        Dati d = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, Reti::Interpreta);
        return d != null ? d : new Dati(List.of(), Map.of());
    }

    /** @return i dati, oppure {@code null} se il contenuto non e' valido: allora vale la copia nel jar */
    static Dati Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONArray voci = new JSONObject(contenuto).getJSONArray("reti");
            List<Rete> elenco = new ArrayList<>();
            Map<String, Rete> perId = new LinkedHashMap<>();
            for (int i = 0; i < voci.length(); i++) {
                JSONObject v = voci.getJSONObject(i);
                String id = v.getString("id").trim();
                String etichetta = v.getString("nome").trim();
                if (!id.matches("[A-Z0-9]{2,12}") || etichetta.isEmpty() || etichetta.contains("(") || perId.containsKey(id)) {
                    return Errore(nome, "rete non valida o doppia: " + id);
                }
                boolean defi = v.optBoolean("defi", false) || (v.optBoolean("importNativo", false) && IMPORT_NATIVI.contains(id));
                long chainid = v.optLong("chainidEtherscan", 0);
                String[] url = {v.optString("urlTransazione", ""), v.optString("urlTokenWallet", ""), v.optString("urlWallet", "")};
                for (String u : url) {
                    if (!u.isEmpty() && !u.startsWith("https://")) return Errore(nome, "indirizzo non https per " + id + ": " + u);
                }
                if (defi && url[0].isEmpty()) return Errore(nome, "manca urlTransazione per " + id);
                List<String> nodi = Https(v.optJSONArray("nodiStorico"));
                if (nodi == null) return Errore(nome, "nodiStorico non valido per " + id);
                NodoPubblicoDefi.Configurazione conf = null;
                JSONObject np = v.optJSONObject("nodoPubblico");
                if (np != null) {
                    List<String> stato = Https(np.optJSONArray("nodiStato")), log = Https(np.optJSONArray("nodiLog")),
                            tracce = Https(np.optJSONArray("nodiTracce"));
                    List<String> noti = Indirizzi(np.optJSONArray("tokenNoti")), avvolti = Indirizzi(np.optJSONArray("tokenAvvolti"));
                    if (stato == null || stato.isEmpty() || log == null || tracce == null || noti == null || avvolti == null) {
                        return Errore(nome, "nodoPubblico non valido per " + id);
                    }
                    conf = new NodoPubblicoDefi.Configurazione(id, stato, log, tracce, noti, avvolti);
                }
                List<Trans_XLayer.Token> token = new ArrayList<>();
                JSONArray tk = v.optJSONArray("tokenNodo");
                if (tk != null) {
                    for (int j = 0; j < tk.length(); j++) {
                        JSONObject t = tk.getJSONObject(j);
                        String ind = t.getString("indirizzo").trim().toLowerCase();
                        int dec = t.getInt("decimali");
                        if (!ind.matches("0x[0-9a-f]{40}") || dec < 0 || dec > 36 || t.getString("simbolo").isBlank()) {
                            return Errore(nome, "token non valido per " + id + ": " + ind);
                        }
                        token.add(new Trans_XLayer.Token(ind, t.getString("simbolo").trim(), t.optString("nome", "").trim(), dec,
                                t.optBoolean("aToken", false)));
                    }
                }
                Rete r = new Rete(id, etichetta, defi, chainid, v.optString("simbolo", "").trim(), v.optString("coingecko", "").trim(),
                        v.optString("defillama", "").trim(), url[0], url[1], url[2], nodi, v.optBoolean("senzaStorico", false),
                        conf, List.copyOf(token));
                elenco.add(r);
                perId.put(id, r);
            }
            if (elenco.isEmpty()) return null;
            return new Dati(Collections.unmodifiableList(elenco), Collections.unmodifiableMap(perId));
        } catch (Exception e) {
            return Errore(nome, "non interpretabile: " + e);
        }
    }

    private static Dati Errore(String nome, String messaggio) {
        LoggerGC.ScriviErrore("Reti: " + nome + ".json, " + messaggio);
        return null;
    }

    /** Elenco di indirizzi https, vuoto se manca; {@code null} se una voce non e' https. */
    private static List<String> Https(JSONArray a) {
        List<String> l = new ArrayList<>();
        if (a == null) return l;
        for (int i = 0; i < a.length(); i++) {
            String s = a.getString(i).trim();
            if (!s.startsWith("https://")) return null;
            l.add(s);
        }
        return List.copyOf(l);
    }

    /** Elenco di indirizzi di contratto (minuscoli), vuoto se manca; {@code null} se uno non e' valido. */
    private static List<String> Indirizzi(JSONArray a) {
        List<String> l = new ArrayList<>();
        if (a == null) return l;
        for (int i = 0; i < a.length(); i++) {
            String s = a.getString(i).trim().toLowerCase();
            if (!s.matches("0x[0-9a-f]{40}")) return null;
            l.add(s);
        }
        return List.copyOf(l);
    }
}
