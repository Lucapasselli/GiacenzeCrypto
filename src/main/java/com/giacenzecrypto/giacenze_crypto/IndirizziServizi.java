package com.giacenzecrypto.giacenze_crypto;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Indirizzi base dei servizi pubblici interrogati <b>senza chiave</b> (DefiLlama, Banca d'Italia, mempool.space, candele
 * pubbliche di Coinbase e Binance, storico pubblico di CoinMarketCap, GoPlus, explorer di Cronos, indexer Algorand, nodo XRP Ledger,
 * TronGrid e TronScan), da
 * {@code config/varie/IndirizziServizi.json}: se un servizio cambia dominio basta il file, non una versione.
 *
 * <p>Gli indirizzi a cui va una ApiKey o una firma dell'utente (Etherscan, Blockscout, Moralis, Helius, NodeReal,
 * CoinGecko, l'API PRO di CoinMarketCap, UniSat, le chiamate firmate di Binance) restano nel codice di proposito: un file
 * aggiornato da GitHub senza una versione non deve poter mandare le chiavi altrove. Nessun ripiego nel codice: il default
 * e' la copia nel jar.</p>
 */
final class IndirizziServizi {

    private IndirizziServizi() {}

    static final String NOME = "IndirizziServizi";

    static final String DEFILLAMA = "defillama";
    static final String BANCA_ITALIA = "bancaditalia";
    static final String MEMPOOL = "mempool";
    static final String COINBASE = "coinbase";
    static final String COINMARKETCAP_DATI = "coinmarketcapDati";
    static final String GOPLUS = "goplus";
    static final String BINANCE_PUBBLICO = "binancePubblico";
    static final String CRONOS_EXPLORER = "cronosExplorer";
    static final String ALGORAND_INDEXER = "algorandIndexer";
    static final String XRPL_NODO = "xrplNodo";
    static final String TRONGRID = "tronGrid";
    static final String TRONSCAN = "tronScan";

    /** Solo https, solo host e percorso: niente parametri, niente credenziali nell'indirizzo, niente barra finale. */
    private static final String FORMATO = "https://[a-z0-9.-]+(:\\d{1,5})?(/[A-Za-z0-9._~-]+)*";

    private static volatile Map<String, String> indirizzi;

    /** L'indirizzo base del servizio, senza barra finale; vuoto (e scritto nel log) se il file non lo contiene. */
    static String Url(String chiave) {
        Map<String, String> m = indirizzi;
        if (m == null) {
            m = Carica();
            indirizzi = m;
        }
        String u = m.get(chiave);
        if (u == null) {
            LoggerGC.ScriviErrore("IndirizziServizi: manca l'indirizzo di " + chiave);
            return "";
        }
        return u;
    }

    static void Ricarica() {
        indirizzi = Carica();
    }

    private static Map<String, String> Carica() {
        Map<String, String> m = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, IndirizziServizi::Interpreta);
        return m != null ? m : Map.of();
    }

    /** @return gli indirizzi, oppure {@code null} se uno non e' valido (allora vale la copia nel jar) */
    static Map<String, String> Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject o = new JSONObject(contenuto).getJSONObject("indirizzi");
            Map<String, String> m = new HashMap<>();
            for (String k : o.keySet()) {
                String u = o.getString(k).trim();
                if (!u.matches(FORMATO)) {
                    LoggerGC.ScriviErrore("IndirizziServizi: indirizzo non valido in " + nome + ".json per " + k + ": " + u);
                    return null;
                }
                m.put(k, u);
            }
            return m.isEmpty() ? null : Map.copyOf(m);
        } catch (Exception e) {
            LoggerGC.ScriviErrore("IndirizziServizi: " + nome + ".json non interpretabile: " + e);
            return null;
        }
    }
}
