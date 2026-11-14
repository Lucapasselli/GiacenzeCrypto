package com.giacenzecrypto.giacenze_crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.json.JSONObject;

/**
 * Verifica della disponibilità dei servizi di cui il programma ha bisogno, con i tempi di risposta e una diagnosi in
 * parole semplici (pulsante <i>Verifica la disponibilità dei servizi</i> in Opzioni - Varie, finestra
 * {@link GUI_VerificaServizi}). Nata dalla segnalazione di un'installazione Windows in cui il servizio prezzi restava
 * muto per 300 s senza che il log dicesse perché (2026-11-13).
 *
 * <p>Nessuna parte di Swing: questa classe produce {@link Esito} e testo, la finestra li mostra. Ciò che si controlla:</p>
 * <ul>
 *   <li><b>Ambiente locale</b>: Node.js, ccxt e gli script presenti, cartella di lavoro scrivibile, IPv6 e proxy. Se Node
 *       o ccxt mancano si dice "non installato": non si scarica mai nulla da una diagnostica.</li>
 *   <li><b>Servizi pubblici senza chiave</b>, raggiunti da Java: prezzi e cambi, explorer, aggiornamenti, nodi pubblici.
 *       Per ciascuno il tempo del DNS, della connessione TCP e della risposta intera sono separati, perché è quel
 *       confronto che distingue una rete lenta da una senza IPv6 da un host bloccato.</li>
 *   <li><b>Exchange da due strade</b>: da Java (HTTPS diretto) e da Node con ccxt ({@code Scripts/Verifica_Rete.js}, che usa
 *       lo stesso stack dei prezzi, con la correzione {@code Ccxt_Rete.js}). Se Java arriva e Node no, la causa non e'
 *       la rete ma Node, un antivirus/firewall o la configurazione IPv6: la diagnosi lo dice.</li>
 *   <li>L'<b>orologio</b> del computer confrontato con quello di Binance: uno sfasamento di pochi secondi fa fallire le
 *       chiamate firmate di Binance e OKX.</li>
 * </ul>
 *
 * <p>Le richieste sono generiche e innocue: nessun dato dell'utente negli indirizzi, nessuna chiave a meno che l'utente
 * ne abbia impostata una (Etherscan), e allora non compare mai nel rapporto né nel log ({@link #senzaChiavi}). La
 * sonda HTTP è iniettabile ({@link Sonda}): i test girano contro un server locale.</p>
 */
final class DiagnosiServizi {

    private DiagnosiServizi() {}

    // ---------------------------------------------------------------- modello

    enum Stato {
        OK("OK"), LENTO("Lento"), AVVISO("Attenzione"), ERRORE("Problema"), INFO("Info");

        private final String etichetta;

        Stato(String etichetta) {
            this.etichetta = etichetta;
        }

        String etichetta() {
            return etichetta;
        }
    }

    /** Che peso ha il fallimento di una voce: {@code ESSENZIALE} è un problema, {@code SECONDARIO} un'attenzione. */
    enum Gravita { ESSENZIALE, SECONDARIO, SPENTO }

    /** @param ms tempo totale in millisecondi, {@code -1} se non applicabile */
    record Esito(String categoria, String servizio, Stato stato, long ms, String dettaglio) {}

    record Richiesta(String metodo, String url, String corpo, Map<String, String> intestazioni) {
        static Richiesta get(String url) {
            return new Richiesta("GET", url, null, Map.of());
        }

        static Richiesta rpc(String url, String metodo) {
            return new Richiesta("POST", url, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + metodo + "\",\"params\":[]}",
                    Map.of("Content-Type", "application/json"));
        }
    }

    /** @param connessioneMs {@code -1} se non misurata (proxy) */
    record Risposta(int codice, String corpo, long dnsMs, int indirizzi4, int indirizzi6, long connessioneMs,
            long totaleMs, long istanteMs) {}

    /** Esito del controllo del contenuto: {@code problema} vuoto se la risposta è quella attesa, {@code nota} è un di più da mostrare. */
    record Verdetto(String problema, String nota) {
        static Verdetto ok(String nota) {
            return new Verdetto("", nota == null ? "" : nota);
        }

        static Verdetto problema(String problema) {
            return new Verdetto(problema, "");
        }
    }

    /** Un'operazione che può fallire in una fase precisa (DNS, connessione, TLS, risposta). */
    static final class ErroreRete extends IOException {
        private static final long serialVersionUID = 1L;
        final String fase;
        final long ms;

        ErroreRete(String fase, String messaggio, long ms) {
            super(messaggio);
            this.fase = fase;
            this.ms = ms;
        }
    }

    interface Sonda {
        Risposta esegui(Richiesta richiesta, int timeoutConnessioneMs, int timeoutLetturaMs) throws IOException;
    }

    /**
     * Una voce da verificare.
     *
     * @param accettaErroriClient vero per le sonde di sola raggiungibilita' (basta che l'host risponda, anche con 403 o 404)
     * @param sogliaLentoMs oltre questo tempo totale l'esito è "Lento"
     * @param controllo verifica il contenuto della risposta 2xx, {@code null} se basta il codice
     */
    record Voce(String categoria, String servizio, Richiesta richiesta, Gravita gravita, boolean accettaErroriClient,
            long sogliaLentoMs, Function<Risposta, Verdetto> controllo) {

        Voce(String categoria, String servizio, Richiesta richiesta, Gravita gravita) {
            this(categoria, servizio, richiesta, gravita, false, SOGLIA_LENTO_MS, null);
        }

        Voce conControllo(Function<Risposta, Verdetto> c) {
            return new Voce(categoria, servizio, richiesta, gravita, accettaErroriClient, sogliaLentoMs, c);
        }

        Voce soloRaggiungibilita() {
            return new Voce(categoria, servizio, richiesta, gravita, true, sogliaLentoMs, controllo);
        }
    }

    /**
     * Impostazioni dell'utente che cambiano cosa si controlla. La chiave Etherscan è tenuta qui solo per comporre la
     * richiesta: {@code toString} non la stampa.
     */
    record Impostazioni(String chiaveEtherscan, boolean servizioPrezziAttivo, boolean tuttiINodi) {
        @Override
        public String toString() {
            return "Impostazioni[chiaveEtherscan=" + (chiaveEtherscan == null || chiaveEtherscan.isBlank() ? "no" : "si")
                    + ", servizioPrezziAttivo=" + servizioPrezziAttivo + ", tuttiINodi=" + tuttiINodi + "]";
        }
    }

    // ---------------------------------------------------------------- costanti

    static final long SOGLIA_LENTO_MS = 2500;
    /** Oltre questo sfasamento rispetto all'orologio di Binance le chiamate firmate falliscono. */
    static final long SFASAMENTO_MASSIMO_MS = 2000;
    static final int TIMEOUT_CONNESSIONE_MS = 5000;
    static final int TIMEOUT_LETTURA_MS = 10000;
    private static final int LIMITE_CORPO = 64 * 1024;
    private static final long TIMEOUT_SCRIPT_NODE_MS = 35000;

    static final String CAT_AMBIENTE = "Ambiente";
    static final String CAT_BASE = "Connessione di base";
    static final String CAT_PREZZI = "Prezzi e cambi";
    static final String CAT_EXCHANGE_JAVA = "Exchange (da Java)";
    static final String CAT_EXCHANGE_NODE = "Exchange (da Node.js)";
    static final String CAT_BLOCKCHAIN = "Blockchain (explorer)";
    static final String CAT_NODI = "Nodi pubblici";
    static final String CAT_AGGIORNAMENTI = "Aggiornamenti e configurazione";
    static final String CAT_AUTORE = "Servizio dell'autore";

    /** Ordine in cui la finestra e il rapporto mostrano le categorie. */
    static final List<String> CATEGORIE = List.of(CAT_AMBIENTE, CAT_BASE, CAT_PREZZI, CAT_EXCHANGE_JAVA, CAT_EXCHANGE_NODE,
            CAT_BLOCKCHAIN, CAT_NODI, CAT_AGGIORNAMENTI, CAT_AUTORE);

    /**
     * Endpoint pubblici leggeri degli exchange usati per i prezzi, identici a quelli di {@code Scripts/Verifica_Rete.js}
     * (un test li confronta). Stessa lista di {@link Prezzi#EXCHANGES_CCXT}.
     */
    static final Map<String, String> ENDPOINT_EXCHANGE = endpointExchange();

    private static Map<String, String> endpointExchange() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("binance", "https://api.binance.com/api/v3/time");
        m.put("cryptocom", "https://api.crypto.com/exchange/v1/public/get-tickers?instrument_name=BTC_USDT");
        m.put("bybit", "https://api.bybit.com/v5/market/time");
        m.put("okx", "https://www.okx.com/api/v5/public/time");
        m.put("coinbase", "https://api.exchange.coinbase.com/time");
        m.put("bitstamp", "https://www.bitstamp.net/api/v2/ticker/btceur/");
        m.put("kucoin", "https://api.kucoin.com/api/v1/timestamp");
        m.put("bitget", "https://api.bitget.com/api/v2/public/time");
        return java.util.Collections.unmodifiableMap(m);
    }

    // ---------------------------------------------------------------- elenco delle voci

    /** Le voci verificate con una richiesta HTTP da Java, nell'ordine in cui compaiono. Nessuna rete e nessun database: solo composizione. */
    static List<Voce> Elenco(Impostazioni imp) {
        List<Voce> v = new ArrayList<>();

        v.add(new Voce(CAT_BASE, "Internet (cloudflare.com)", Richiesta.get("https://cloudflare.com"), Gravita.ESSENZIALE).soloRaggiungibilita());

        v.add(new Voce(CAT_PREZZI, "DefiLlama (prezzi dei token)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.DEFILLAMA) + "/prices/current/coingecko:bitcoin"), Gravita.ESSENZIALE)
                .conControllo(r -> r.corpo().contains("coins") ? Verdetto.ok("") : Verdetto.problema("risposta senza il campo coins")));
        v.add(new Voce(CAT_PREZZI, "Banca d'Italia (cambio euro-dollaro)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.BANCA_ITALIA) + "/"), Gravita.ESSENZIALE).soloRaggiungibilita());
        v.add(new Voce(CAT_PREZZI, "CoinGecko (elenco dei token)",
                Richiesta.get("https://api.coingecko.com/api/v3/ping"), Gravita.ESSENZIALE)
                .conControllo(r -> r.corpo().contains("gecko_says") ? Verdetto.ok("") : Verdetto.problema("risposta inattesa")));
        v.add(new Voce(CAT_PREZZI, "CoinMarketCap (storico pubblico)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.COINMARKETCAP_DATI) + "/"), Gravita.SECONDARIO).soloRaggiungibilita());

        // Binance prima fra gli exchange: dal suo orologio si ricava anche lo sfasamento di quello del computer.
        for (Map.Entry<String, String> e : ENDPOINT_EXCHANGE.entrySet()) {
            Voce voce = new Voce(CAT_EXCHANGE_JAVA, e.getKey(), Richiesta.get(e.getValue()), Gravita.SECONDARIO).soloRaggiungibilita();
            if (e.getKey().equals("binance")) voce = voce.conControllo(DiagnosiServizi::ControllaOrologioBinance);
            v.add(voce);
        }

        if (imp.chiaveEtherscan() != null && !imp.chiaveEtherscan().isBlank()) {
            v.add(new Voce(CAT_BLOCKCHAIN, "Etherscan (con la tua chiave)",
                    Richiesta.get("https://api.etherscan.io/v2/api?chainid=1&module=proxy&action=eth_blockNumber&apikey=" + imp.chiaveEtherscan().trim()),
                    Gravita.ESSENZIALE).conControllo(DiagnosiServizi::ControllaEtherscan));
        } else {
            v.add(new Voce(CAT_BLOCKCHAIN, "Etherscan (senza chiave)",
                    Richiesta.get("https://api.etherscan.io/v2/api?chainid=1&module=proxy&action=eth_blockNumber"), Gravita.SECONDARIO)
                    .soloRaggiungibilita());
        }
        v.add(new Voce(CAT_BLOCKCHAIN, "mempool.space (Bitcoin)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.MEMPOOL) + "/blocks/tip/height"), Gravita.SECONDARIO)
                .conControllo(r -> r.corpo().trim().matches("\\d+") ? Verdetto.ok("altezza " + r.corpo().trim()) : Verdetto.problema("risposta inattesa")));
        v.add(new Voce(CAT_BLOCKCHAIN, "GoPlus (sicurezza dei token)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.GOPLUS) + "/"), Gravita.SECONDARIO).soloRaggiungibilita());
        v.add(new Voce(CAT_BLOCKCHAIN, "Cronos (explorer)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.CRONOS_EXPLORER)), Gravita.SECONDARIO).soloRaggiungibilita());
        v.add(new Voce(CAT_BLOCKCHAIN, "Algorand (indexer)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.ALGORAND_INDEXER) + "/health"), Gravita.SECONDARIO).soloRaggiungibilita());
        v.add(new Voce(CAT_BLOCKCHAIN, "XRP Ledger (nodo)",
                new Richiesta("POST", IndirizziServizi.Url(IndirizziServizi.XRPL_NODO), "{\"method\":\"server_info\",\"params\":[{}]}",
                        Map.of("Content-Type", "application/json")), Gravita.SECONDARIO)
                .conControllo(r -> r.corpo().contains("result") ? Verdetto.ok("") : Verdetto.problema("risposta inattesa")));
        v.add(new Voce(CAT_BLOCKCHAIN, "TronGrid (Tron)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.TRONGRID)), Gravita.SECONDARIO).soloRaggiungibilita());
        v.add(new Voce(CAT_BLOCKCHAIN, "TronScan (Tron)",
                Richiesta.get(IndirizziServizi.Url(IndirizziServizi.TRONSCAN)), Gravita.SECONDARIO).soloRaggiungibilita());

        for (String[] nodo : NodiDaVerificare(imp.tuttiINodi())) {
            v.add(new Voce(CAT_NODI, nodo[0] + " - " + nodo[1], Richiesta.rpc(nodo[1], "eth_blockNumber"), Gravita.SECONDARIO)
                    .conControllo(DiagnosiServizi::ControllaNodoRpc));
        }

        v.add(new Voce(CAT_AGGIORNAMENTI, "GitHub (versioni del programma)",
                Richiesta.get("https://api.github.com/rate_limit"), Gravita.SECONDARIO).conControllo(DiagnosiServizi::ControllaGitHub));
        v.add(new Voce(CAT_AGGIORNAMENTI, "GitHub raw (configurazioni)",
                Richiesta.get("https://raw.githubusercontent.com/" + Funzioni.REPO_GITHUB + "/" + Funzioni.BRANCH_GITHUB + "/config/varie/Runtime.json"),
                Gravita.SECONDARIO).conControllo(r -> r.corpo().contains("node") ? Verdetto.ok("") : Verdetto.problema("file di configurazione non letto")));
        if (!CcxtInterop.nodeFornitoDalPacchetto()) {
            v.add(new Voce(CAT_AGGIORNAMENTI, "nodejs.org (scaricamento di Node)",
                    Richiesta.get("https://nodejs.org/dist/" + CcxtInterop.nodeVersion() + "/SHASUMS256.txt"), Gravita.SECONDARIO)
                    .conControllo(r -> r.corpo().contains("node-") ? Verdetto.ok("") : Verdetto.problema("elenco dei file non letto")));
            v.add(new Voce(CAT_AGGIORNAMENTI, "npm (scaricamento di ccxt)",
                    Richiesta.get("https://registry.npmjs.org/-/ping"), Gravita.SECONDARIO).soloRaggiungibilita());
        }

        v.add(new Voce(CAT_AUTORE, "giacenzecrypto.it (prezzi on-chain)",
                Richiesta.get("https://giacenzecrypto.it/v1/monete"), imp.servizioPrezziAttivo() ? Gravita.SECONDARIO : Gravita.SPENTO)
                .conControllo(r -> r.corpo().contains("[") || r.corpo().contains("{") ? Verdetto.ok(imp.servizioPrezziAttivo() ? "" : "servizio facoltativo, disattivato nelle opzioni")
                        : Verdetto.problema("risposta inattesa")));
        return v;
    }

    /** Coppie {rete, indirizzo} dei nodi pubblici di ETH e BASE, o di tutte le reti con un nodo di storico se {@code tutti}. */
    static List<String[]> NodiDaVerificare(boolean tutti) {
        List<String[]> nodi = new ArrayList<>();
        java.util.Set<String> visti = new java.util.HashSet<>();
        for (Reti.Rete r : Reti.Elenco()) {
            boolean principale = r.nodoPubblico() != null;
            if (!tutti && !principale) continue;
            List<String> indirizzi = new ArrayList<>();
            if (r.nodoPubblico() != null) indirizzi.addAll(r.nodoPubblico().nodiStato());
            if (r.nodiStorico() != null) indirizzi.addAll(r.nodiStorico());
            for (String u : indirizzi) {
                if (visti.add(r.id() + "|" + u)) nodi.add(new String[]{r.id(), u});
            }
        }
        return nodi;
    }

    // ---------------------------------------------------------------- controlli sul contenuto

    static Verdetto ControllaOrologioBinance(Risposta r) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"serverTime\"\\s*:\\s*(\\d+)").matcher(r.corpo());
        if (!m.find()) return Verdetto.problema("risposta senza l'ora del server");
        long server = Long.parseLong(m.group(1));
        long sfasamento = r.istanteMs() - server; //positivo: il computer è avanti
        String nota = "orologio del computer " + (sfasamento >= 0 ? "avanti" : "indietro") + " di " + Math.abs(sfasamento) + " ms rispetto a Binance";
        if (Math.abs(sfasamento) > SFASAMENTO_MASSIMO_MS) {
            return Verdetto.problema(nota + ": le chiamate firmate di Binance e OKX falliscono con un orologio sfasato di oltre qualche secondo");
        }
        return Verdetto.ok(nota);
    }

    static Verdetto ControllaEtherscan(Risposta r) {
        if (r.corpo().contains("\"jsonrpc\"")) return Verdetto.ok("");
        if (r.corpo().toLowerCase(Locale.ROOT).contains("invalid api key")) return Verdetto.problema("la chiave Etherscan è stata rifiutata");
        return Verdetto.problema("risposta inattesa");
    }

    static Verdetto ControllaNodoRpc(Risposta r) {
        try {
            JSONObject o = new JSONObject(r.corpo());
            if (o.has("error")) {
                Object e = o.get("error");
                return Verdetto.problema("il nodo risponde con un errore" + (e instanceof JSONObject j && j.has("message") ? ": " + j.optString("message") : ""));
            }
            String res = o.optString("result", "");
            if (!res.startsWith("0x")) return Verdetto.problema("risposta inattesa");
            if (res.equals("0x0") || res.equals("0x")) return Verdetto.problema("il nodo risponde 0x0 invece dell'altezza dell'ultimo blocco: non è affidabile");
            return Verdetto.ok("blocco " + Long.parseLong(res.substring(2), 16));
        } catch (RuntimeException e) {
            return Verdetto.problema("risposta non valida");
        }
    }

    static Verdetto ControllaGitHub(Risposta r) {
        try {
            JSONObject core = new JSONObject(r.corpo()).getJSONObject("resources").getJSONObject("core");
            int resta = core.getInt("remaining");
            int limite = core.getInt("limit");
            String nota = resta + " richieste rimaste su " + limite + " nell'ora";
            if (resta == 0) {
                String quando = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochSecond(core.optLong("reset")));
                return Verdetto.problema("limite di richieste raggiunto, si sblocca alle " + quando
                        + " (aggiornamenti e configurazioni non si scaricano fino ad allora)");
            }
            return Verdetto.ok(nota);
        } catch (RuntimeException e) {
            return Verdetto.problema("risposta inattesa");
        }
    }

    // ---------------------------------------------------------------- classificazione (pura)

    private static final java.util.regex.Pattern CHIAVE = java.util.regex.Pattern.compile("(?i)(api[_-]?key|key|token|secret)=[^&\\s\"']+");

    /** Toglie dai testi qualunque {@code apikey=...}: nessuna chiave deve arrivare al rapporto o al log. */
    static String senzaChiavi(String testo) {
        return testo == null ? "" : CHIAVE.matcher(testo).replaceAll("$1=***");
    }

    static String tempi(Risposta r) {
        StringBuilder sb = new StringBuilder();
        sb.append("DNS ").append(r.dnsMs()).append(" ms (").append(r.indirizzi4()).append(" IPv4, ").append(r.indirizzi6()).append(" IPv6)");
        if (r.connessioneMs() >= 0) sb.append(", connessione ").append(r.connessioneMs()).append(" ms");
        sb.append(", risposta ").append(r.totaleMs()).append(" ms");
        return sb.toString();
    }

    /**
     * Trasforma la risposta (o l'errore) di una voce nel suo esito. Il guasto di una voce non essenziale è un'attenzione,
     * non un problema: il programma ha di norma un'alternativa (un altro exchange, un altro nodo).
     */
    static Esito Classifica(Voce v, Risposta r, IOException errore) {
        Stato guasto = v.gravita() == Gravita.ESSENZIALE ? Stato.ERRORE : v.gravita() == Gravita.SECONDARIO ? Stato.AVVISO : Stato.INFO;
        if (errore != null) {
            String fase = errore instanceof ErroreRete e ? e.fase : "richiesta";
            long ms = errore instanceof ErroreRete e ? e.ms : -1;
            String msg = senzaChiavi(errore.getMessage());
            return new Esito(v.categoria(), v.servizio(), guasto, ms, "non raggiungibile (" + fase + "): " + msg);
        }
        int c = r.codice();
        String tempi = tempi(r);
        String problema = null;
        String nota = "";
        if (c == 429) {
            problema = "il servizio risponde ma ha raggiunto il limite di richieste (429)";
        } else if (c == 451) {
            problema = "il servizio risponde ma blocca la tua area geografica (451)";
        } else if (c >= 500) {
            problema = "il servizio risponde con un errore del server (" + c + ")";
        } else if (c >= 400 && !v.accettaErroriClient()) {
            problema = c == 401 || c == 403
                    ? "il servizio risponde con accesso negato (" + c + "): può essere un blocco della tua rete o una chiave non valida"
                    : "il servizio risponde con l'errore " + c;
        } else if (c < 400 && v.controllo() != null) {
            Verdetto d = v.controllo().apply(r);
            if (!d.problema().isEmpty()) problema = d.problema();
            nota = d.nota();
        } else if (c >= 400) {
            nota = "risponde (" + c + ")";
        }
        if (problema != null) {
            Stato s = c == 429 || c == 451 ? Stato.AVVISO : guasto;
            // Un controllo sul contenuto fallito su un servizio raggiunto è sempre solo un'attenzione, tranne i servizi essenziali.
            return new Esito(v.categoria(), v.servizio(), s, r.totaleMs(), senzaChiavi(problema) + ". " + tempi);
        }
        Stato s = r.totaleMs() > v.sogliaLentoMs() ? Stato.LENTO : Stato.OK;
        String dettaglio = (nota.isEmpty() ? "" : nota + ". ") + tempi + (s == Stato.LENTO ? ". Risposta lenta (oltre " + v.sogliaLentoMs() / 1000.0 + " s)" : "");
        return new Esito(v.categoria(), v.servizio(), s, r.totaleMs(), senzaChiavi(dettaglio));
    }

    static Esito Esegui(Voce v, Sonda sonda) {
        try {
            Risposta r = sonda.esegui(v.richiesta(), TIMEOUT_CONNESSIONE_MS, TIMEOUT_LETTURA_MS);
            return Classifica(v, r, null);
        } catch (IOException e) {
            return Classifica(v, null, e);
        } catch (RuntimeException e) {
            return Classifica(v, null, new IOException(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- la sonda HTTP vera

    /** La sonda che usa il JDK: DNS, connessione TCP e risposta misurati a parte, nel rispetto del proxy di sistema. */
    static final class SondaHttp implements Sonda {
        @Override
        public Risposta esegui(Richiesta rich, int timeoutConnessioneMs, int timeoutLetturaMs) throws IOException {
            URI uri = URI.create(rich.url());
            String host = uri.getHost();
            int porta = uri.getPort() > 0 ? uri.getPort() : "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
            long t0 = System.nanoTime();

            InetAddress[] indirizzi;
            try {
                indirizzi = InetAddress.getAllByName(host);
            } catch (UnknownHostException e) {
                throw new ErroreRete("DNS", "nome non risolto: " + host, ms(t0));
            }
            long dnsMs = ms(t0);
            int v4 = 0;
            int v6 = 0;
            for (InetAddress a : indirizzi) {
                if (a instanceof java.net.Inet6Address) v6++;
                else v4++;
            }

            long connessioneMs = -1;
            boolean conProxy = false;
            try {
                for (Proxy p : ProxySelector.getDefault().select(uri)) {
                    if (p.type() != Proxy.Type.DIRECT) conProxy = true;
                }
            } catch (RuntimeException ignorata) {
                //selettore di proxy anomalo: si misura comunque
            }
            if (!conProxy && indirizzi.length > 0) {
                long t1 = System.nanoTime();
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(indirizzi[0], porta), timeoutConnessioneMs);
                    connessioneMs = ms(t1);
                } catch (SocketTimeoutException e) {
                    throw new ErroreRete("connessione", "nessuna risposta entro " + timeoutConnessioneMs / 1000 + " s verso "
                            + indirizzi[0].getHostAddress(), ms(t0));
                } catch (IOException e) {
                    throw new ErroreRete("connessione", e.getClass().getSimpleName() + " verso " + indirizzi[0].getHostAddress()
                            + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")"), ms(t0));
                }
            }

            long t2 = System.nanoTime();
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            try {
                conn.setConnectTimeout(timeoutConnessioneMs);
                conn.setReadTimeout(timeoutLetturaMs);
                conn.setRequestMethod(rich.metodo());
                conn.setRequestProperty("User-Agent", "GiacenzeCrypto-Diagnostica");
                conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                for (Map.Entry<String, String> h : rich.intestazioni().entrySet()) conn.setRequestProperty(h.getKey(), h.getValue());
                if (rich.corpo() != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(rich.corpo().getBytes(StandardCharsets.UTF_8));
                    }
                }
                int codice = conn.getResponseCode();
                long istante = System.currentTimeMillis();
                InputStream is = codice >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String corpo = is == null ? "" : leggi(is);
                long totale = dnsMs + (connessioneMs > 0 ? connessioneMs : 0) + ms(t2);
                //Per l'orologio: l'istante a meta' della risposta è l'approssimazione migliore di quando il server ha letto l'ora.
                long meta = istante - ms(t2) / 2;
                return new Risposta(codice, corpo, dnsMs, v4, v6, connessioneMs, totale, meta);
            } catch (SocketTimeoutException e) {
                throw new ErroreRete("risposta", "nessuna risposta entro " + timeoutLetturaMs / 1000 + " s", ms(t0));
            } catch (SSLException e) {
                throw new ErroreRete("TLS", "errore di sicurezza (" + e.getClass().getSimpleName()
                        + "): controlla data e ora del computer, un antivirus o un proxy che intercetta i certificati", ms(t0));
            } catch (IOException e) {
                throw new ErroreRete("risposta", e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()), ms(t0));
            } finally {
                conn.disconnect();
            }
        }

        private static long ms(long daNano) {
            return (System.nanoTime() - daNano) / 1_000_000;
        }

        private static String leggi(InputStream is) throws IOException {
            try (is) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0 && out.size() < LIMITE_CORPO) out.write(buf, 0, n);
                return out.toString(StandardCharsets.UTF_8);
            }
        }
    }

    // ---------------------------------------------------------------- ambiente locale e Node

    /** Le righe dell'ambiente locale: veloci, nessuna rete. */
    static List<Esito> VerificheLocali() {
        List<Esito> e = new ArrayList<>();
        e.add(new Esito(CAT_AMBIENTE, "Sistema", Stato.INFO, -1, System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + "), Java " + System.getProperty("java.version")
                + ", programma " + VarStatiche.Versione));
        e.add(new Esito(CAT_AMBIENTE, "IPv6 sul computer", Stato.INFO, -1, HaIpv6Globale()
                ? "presente: i siti a doppio stack si raggiungono anche in IPv6"
                : "assente: si usa solo IPv4 (normale su molte reti domestiche e aziendali)"));
        boolean proxy = ProxyConfigurato();
        e.add(new Esito(CAT_AMBIENTE, "Proxy di Java", Stato.INFO, -1, proxy ? "configurato: le misure di connessione TCP non sono possibili" : "nessuno"));

        try {
            Path cartella = Paths.get(VarStatiche.getWorkingDirectory()).toAbsolutePath();
            long liberi = Files.getFileStore(cartella).getUsableSpace() / (1024 * 1024);
            boolean scrivibile = Files.isWritable(cartella);
            Stato s = !scrivibile ? Stato.ERRORE : liberi < 500 ? Stato.AVVISO : Stato.OK;
            e.add(new Esito(CAT_AMBIENTE, "Cartella di lavoro", s, -1,
                    (scrivibile ? "scrivibile" : "NON scrivibile") + ", " + liberi + " MB liberi"));
        } catch (IOException | RuntimeException ex) {
            e.add(new Esito(CAT_AMBIENTE, "Cartella di lavoro", Stato.AVVISO, -1, "non verificabile: " + ex.getMessage()));
        }

        Path script = Paths.get(VarStatiche.getPathRisorse() + "Scripts/Historical_Multi_Eur.js");
        Path rete = script.resolveSibling(ServizioNodePrezzi.SCRIPT_RETE_CCXT);
        List<String> mancanti = new ArrayList<>();
        if (!Files.isRegularFile(script)) mancanti.add("Historical_Multi_Eur.js");
        if (!Files.isRegularFile(rete)) mancanti.add(ServizioNodePrezzi.SCRIPT_RETE_CCXT);
        e.add(new Esito(CAT_AMBIENTE, "Script di Node", mancanti.isEmpty() ? Stato.OK : Stato.ERRORE, -1,
                mancanti.isEmpty() ? "Historical_Multi_Eur.js e " + ServizioNodePrezzi.SCRIPT_RETE_CCXT + " presenti"
                        : "mancano: " + String.join(", ", mancanti) + " (i prezzi da exchange non funzionano o mancano della correzione di rete)"));

        Path node = CcxtInterop.getNodeExePath();
        if (!Files.isRegularFile(node)) {
            e.add(new Esito(CAT_AMBIENTE, "Node.js", Stato.INFO, -1, "non installato: si scarica al primo uso delle importazioni da exchange o dei prezzi"));
        } else {
            e.add(VersioneNode(node));
        }
        Path ccxt = CcxtInterop.getNodeDir().resolve("node_modules").resolve("ccxt");
        String installata = CcxtInterop.versioneCcxtInstallata(ccxt);
        if (installata == null) {
            e.add(new Esito(CAT_AMBIENTE, "ccxt", Stato.INFO, -1, "non installato: si installa al primo uso"));
        } else if (!installata.equals(CcxtInterop.ccxtVersion())) {
            e.add(new Esito(CAT_AMBIENTE, "ccxt", Stato.INFO, -1, "versione " + installata + " installata, " + CcxtInterop.ccxtVersion()
                    + " richiesta: si aggiorna al primo uso"));
        } else {
            e.add(new Esito(CAT_AMBIENTE, "ccxt", Stato.OK, -1, "versione " + installata));
        }
        return e;
    }

    /** Lancia {@code node --version}: il tempo di avvio è il primo sintomo di un antivirus che scansiona ogni esecuzione. */
    private static Esito VersioneNode(Path node) {
        long t0 = System.nanoTime();
        try {
            Process p = new ProcessBuilder(node.toString(), "--version").redirectErrorStream(true).start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return new Esito(CAT_AMBIENTE, "Node.js", Stato.ERRORE, 15000, "non risponde a node --version entro 15 s: un antivirus o un firewall potrebbe bloccarlo");
            }
            String versione = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            boolean diversa = !CcxtInterop.nodeFornitoDalPacchetto() && !versione.equals(CcxtInterop.nodeVersion());
            Stato s = p.exitValue() != 0 ? Stato.ERRORE : ms > 3000 ? Stato.LENTO : Stato.OK;
            return new Esito(CAT_AMBIENTE, "Node.js", s, ms, versione + (diversa ? " (richiesta " + CcxtInterop.nodeVersion() + ")" : "")
                    + ", avvio in " + ms + " ms" + (s == Stato.LENTO ? " (lento: possibile scansione dell'antivirus)" : ""));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Esito(CAT_AMBIENTE, "Node.js", Stato.ERRORE, -1, "non si avvia: " + senzaChiavi(e.getMessage()));
        }
    }

    static boolean HaIpv6Globale() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : java.util.Collections.list(ni.getInetAddresses())) {
                    if (a instanceof java.net.Inet6Address && !a.isLinkLocalAddress() && !a.isLoopbackAddress()) return true;
                }
            }
        } catch (IOException | RuntimeException ignorata) {
            //solo informativo
        }
        return false;
    }

    static boolean ProxyConfigurato() {
        try {
            for (Proxy p : ProxySelector.getDefault().select(URI.create("https://cloudflare.com"))) {
                if (p.type() != Proxy.Type.DIRECT) return true;
            }
        } catch (RuntimeException ignorata) {
            //solo informativo
        }
        return false;
    }

    /**
     * Interroga gli exchange dallo stack di Node + ccxt con {@code Scripts/Verifica_Rete.js}, in un processo a parte (non
     * si tocca il servizio dei prezzi) e con un limite di tempo. {@code null} come {@code interrotto} vale "mai".
     */
    static List<Esito> VerificaExchangeDaNode(Consumer<Esito> perOgniEsito, BooleanSupplier interrotto) {
        List<Esito> esiti = new ArrayList<>();
        Consumer<Esito> emetti = x -> {
            esiti.add(x);
            if (perOgniEsito != null) perOgniEsito.accept(x);
        };
        Path node = CcxtInterop.getNodeExePath();
        Path script = Paths.get(VarStatiche.getPathRisorse() + "Scripts/Verifica_Rete.js");
        Path ccxt = CcxtInterop.getNodeDir().resolve("node_modules").resolve("ccxt");
        if (!Files.isRegularFile(node) || !Files.isDirectory(ccxt)) {
            emetti.accept(new Esito(CAT_EXCHANGE_NODE, "Node.js e ccxt", Stato.INFO, -1,
                    "non installati: la prova da Node si fa dopo il primo uso delle importazioni da exchange o dei prezzi"));
            return esiti;
        }
        if (!Files.isRegularFile(script)) {
            emetti.accept(new Esito(CAT_EXCHANGE_NODE, "Script di prova", Stato.INFO, -1,
                    "Scripts/Verifica_Rete.js non presente in questa installazione: prova da Node saltata"));
            return esiti;
        }
        Process p = null;
        try {
            p = ServizioNodePrezzi.ProcessoScript(node, script, List.of()).start();
            final Process processo = p;
            Thread err = new Thread(() -> {
                try (InputStream es = processo.getErrorStream()) {
                    es.transferTo(OutputStream.nullOutputStream());
                } catch (IOException ignorata) {
                    //processo terminato
                }
            }, "verifica-rete-stderr");
            err.setDaemon(true);
            err.start();
            Thread guardia = new Thread(() -> {
                long fine = System.currentTimeMillis() + TIMEOUT_SCRIPT_NODE_MS;
                while (processo.isAlive() && System.currentTimeMillis() < fine) {
                    if (interrotto != null && interrotto.getAsBoolean()) break;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (processo.isAlive()) processo.destroyForcibly();
            }, "verifica-rete-guardia");
            guardia.setDaemon(true);
            guardia.start();

            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String riga;
                while ((riga = r.readLine()) != null) {
                    Esito x = EsitoDaRigaNode(riga);
                    if (x != null) emetti.accept(x);
                }
            }
            if (esiti.isEmpty()) {
                emetti.accept(new Esito(CAT_EXCHANGE_NODE, "Script di prova", Stato.ERRORE, -1,
                        "nessun risultato da Node entro " + TIMEOUT_SCRIPT_NODE_MS / 1000 + " s: il processo non risponde (antivirus, firewall o rete)"));
            }
        } catch (IOException e) {
            emetti.accept(new Esito(CAT_EXCHANGE_NODE, "Script di prova", Stato.ERRORE, -1, "Node non si avvia: " + senzaChiavi(e.getMessage())));
        } finally {
            if (p != null && p.isAlive()) p.destroyForcibly();
        }
        return esiti;
    }

    /** Una riga JSON di {@code Verifica_Rete.js} nell'esito corrispondente, {@code null} se non è una riga che interessa. */
    static Esito EsitoDaRigaNode(String riga) {
        try {
            JSONObject o = new JSONObject(riga);
            String tipo = o.optString("tipo");
            if (tipo.equals("ambiente")) {
                return new Esito(CAT_AMBIENTE, "Node.js (rete)", Stato.INFO, -1, "Node " + o.optString("node") + " " + o.optString("piattaforma")
                        + ", ccxt " + o.optString("ccxt") + ", IPv6 globale: " + (o.optBoolean("ipv6") ? "si" : "no")
                        + ", variabili proxy: " + (o.optBoolean("proxy") ? "si" : "no")
                        + ", correzione di rete (Ccxt_Rete.js): " + (o.optBoolean("correzione") ? "attiva" : "assente"));
            }
            if (tipo.equals("errore")) {
                return new Esito(CAT_EXCHANGE_NODE, "ccxt", Stato.ERRORE, -1, senzaChiavi(o.optString("errore")));
            }
            if (!tipo.equals("exchange")) return null;
            String id = o.optString("exchange");
            long ms = o.optLong("ms", -1);
            if (!o.optBoolean("ok")) {
                return new Esito(CAT_EXCHANGE_NODE, id, Stato.AVVISO, ms, "non raggiungibile da Node: " + senzaChiavi(o.optString("errore")));
            }
            String nota = o.optString("nota");
            boolean lento = ms > 2 * SOGLIA_LENTO_MS;
            return new Esito(CAT_EXCHANGE_NODE, id, lento ? Stato.LENTO : Stato.OK, ms,
                    (nota.isEmpty() ? "" : nota + ". ") + "risposta in " + ms + " ms" + (lento ? " (lenta)" : ""));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- orchestrazione

    /**
     * Esegue tutta la verifica: ambiente locale, richieste HTTP in parallelo e prova da Node nello stesso tempo. Ogni esito
     * è consegnato a {@code perOgniEsito} appena pronto (da thread diversi: il chiamante sincronizza). Al massimo mezzo
     * minuto in condizioni normali.
     *
     * @return tutti gli esiti, ordinati per categoria nell'ordine di {@link #CATEGORIE} e poi come sono stati prodotti
     */
    static List<Esito> EseguiTutto(Impostazioni imp, Sonda sonda, Consumer<Esito> perOgniEsito, BooleanSupplier interrotto) {
        List<Esito> tutti = java.util.Collections.synchronizedList(new ArrayList<>());
        Consumer<Esito> emetti = x -> {
            tutti.add(x);
            if (perOgniEsito != null) perOgniEsito.accept(x);
        };
        ExecutorService pool = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "verifica-servizi");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> attivita = new ArrayList<>();
            attivita.add(pool.submit(() -> VerificheLocali().forEach(emetti)));
            attivita.add(pool.submit(() -> VerificaExchangeDaNode(emetti, interrotto)));
            for (Voce v : Elenco(imp)) {
                attivita.add(pool.submit(() -> {
                    if (interrotto != null && interrotto.getAsBoolean()) return;
                    emetti.accept(Esegui(v, sonda));
                }));
            }
            for (Future<?> f : attivita) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (java.util.concurrent.ExecutionException e) {
                    LoggerGC.ScriviErrore(e);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return Ordina(new ArrayList<>(tutti));
    }

    /** Raggruppa per categoria nell'ordine di {@link #CATEGORIE}, tenendo l'ordine di arrivo dentro la categoria. */
    static List<Esito> Ordina(List<Esito> esiti) {
        List<Esito> out = new ArrayList<>(esiti);
        out.sort(java.util.Comparator.comparingInt(x -> {
            int i = CATEGORIE.indexOf(x.categoria());
            return i < 0 ? CATEGORIE.size() : i;
        }));
        return out;
    }

    // ---------------------------------------------------------------- diagnosi e rapporto (pure)

    private static boolean guasto(Esito e) {
        return e.stato() == Stato.ERRORE || e.stato() == Stato.AVVISO;
    }

    /**
     * La diagnosi in frasi: cosa non va e quale può essere la causa, ricavata dal confronto fra le righe. Mai istruzioni
     * di tipo fiscale, solo lo stato dei servizi.
     */
    static List<String> Conclusioni(List<Esito> esiti) {
        List<String> c = new ArrayList<>();
        List<Esito> base = di(esiti, CAT_BASE);
        if (!base.isEmpty() && base.stream().allMatch(DiagnosiServizi::guasto)) {
            c.add("Il programma non raggiunge Internet (" + base.get(0).dettaglio() + "). Gli altri esiti negativi ne sono una conseguenza: "
                    + "controlla la connessione, il firewall o il proxy.");
            return c;
        }

        Map<String, Esito> java = perNome(di(esiti, CAT_EXCHANGE_JAVA));
        Map<String, Esito> node = perNome(di(esiti, CAT_EXCHANGE_NODE));
        List<String> soloNode = new ArrayList<>();
        List<String> entrambi = new ArrayList<>();
        for (Map.Entry<String, Esito> e : node.entrySet()) {
            Esito j = java.get(e.getKey());
            if (j == null || !guasto(e.getValue())) continue;
            if (guasto(j)) entrambi.add(e.getKey());
            else soloNode.add(e.getKey());
        }
        long provatiDaNode = node.values().stream().filter(x -> x.stato() != Stato.INFO).count();
        if (!soloNode.isEmpty()) {
            if (soloNode.size() == provatiDaNode && provatiDaNode > 1) {
                c.add("Java raggiunge gli exchange ma Node.js no, su nessuno: la rete in sé funziona, il problema è nel percorso di Node. "
                        + "Cause tipiche: un antivirus o un firewall che blocca node.exe (cartella tools/node), un proxy che Node non usa, oppure una rete senza IPv6 "
                        + "con il difetto di connessione di ccxt (vedi la riga Node.js (rete): se la correzione di rete è assente, serve una versione del programma che la contiene). "
                        + "I prezzi da exchange e le importazioni via API non funzionano finché non si risolve.");
            } else {
                c.add("Node.js non raggiunge " + String.join(", ", soloNode) + " mentre Java sì: il percorso di Node ha un problema su questi exchange "
                        + "(spesso un host a doppio stack su una rete senza IPv6, o un blocco selettivo).");
            }
        }
        if (!entrambi.isEmpty()) {
            c.add(String.join(", ", entrambi) + (entrambi.size() == 1 ? " non è raggiungibile" : " non sono raggiungibili")
                    + " né da Java né da Node: blocco della rete, dell'area geografica o servizio fuori uso. I prezzi proseguono con gli altri exchange.");
        }

        for (Esito e : esiti) {
            if (e.categoria().equals(CAT_EXCHANGE_JAVA) || e.categoria().equals(CAT_EXCHANGE_NODE)) continue;
            if (e.stato() == Stato.ERRORE) c.add(e.servizio() + ": " + e.dettaglio());
        }
        for (Esito e : esiti) {
            if (e.categoria().equals(CAT_EXCHANGE_JAVA) || e.categoria().equals(CAT_EXCHANGE_NODE)) continue;
            if (e.stato() == Stato.AVVISO) c.add(e.servizio() + ": " + e.dettaglio());
        }
        List<String> lenti = new ArrayList<>();
        for (Esito e : esiti) if (e.stato() == Stato.LENTO) lenti.add(e.servizio() + " (" + e.ms() + " ms)");
        if (!lenti.isEmpty()) c.add("Risposte lente: " + String.join(", ", lenti) + ". Una rete lenta allunga importazioni e calcolo dei prezzi, non li impedisce.");

        if (c.isEmpty()) c.add("Tutto a posto: tutti i servizi controllati rispondono nei tempi normali.");
        return c;
    }

    private static List<Esito> di(List<Esito> esiti, String categoria) {
        List<Esito> out = new ArrayList<>();
        for (Esito e : esiti) if (e.categoria().equals(categoria)) out.add(e);
        return out;
    }

    private static Map<String, Esito> perNome(List<Esito> esiti) {
        Map<String, Esito> m = new LinkedHashMap<>();
        for (Esito e : esiti) m.put(e.servizio(), e);
        return m;
    }

    /** Il testo del rapporto, per gli appunti e per il log: date, versione, tutte le righe e la diagnosi. Mai chiavi. */
    static String Rapporto(List<Esito> esiti, String quando) {
        StringBuilder sb = new StringBuilder();
        sb.append("Verifica dei servizi - ").append(quando).append(" - programma ").append(VarStatiche.Versione).append('\n');
        String cat = null;
        for (Esito e : Ordina(esiti)) {
            if (!e.categoria().equals(cat)) {
                cat = e.categoria();
                sb.append('\n').append(cat).append('\n');
            }
            sb.append("  [").append(e.stato().etichetta()).append("] ").append(e.servizio());
            if (e.ms() >= 0) sb.append(" - ").append(e.ms()).append(" ms");
            sb.append(" - ").append(e.dettaglio()).append('\n');
        }
        sb.append("\nDiagnosi\n");
        for (String s : Conclusioni(esiti)) sb.append("  - ").append(s).append('\n');
        return senzaChiavi(sb.toString());
    }

    /** Insieme di tutti gli id exchange verificabili: usato dai test per tenere allineati Java e lo script Node. */
    static Set<String> IdExchange() {
        return ENDPOINT_EXCHANGE.keySet();
    }
}
