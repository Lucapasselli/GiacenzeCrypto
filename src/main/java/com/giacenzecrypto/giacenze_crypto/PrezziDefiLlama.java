package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Prezzi di DefiLlama per i token con address, chiesti <b>agli istanti esatti</b> in cui servono, molti
 * token e molti istanti per chiamata ({@code /batchHistorical}). E' l'unica strada verso DefiLlama: la
 * serie oraria {@code /chart} non si usa piu' (2026-10-09).
 *
 * <p><b>Perche' non {@code /chart}.</b> Per i token di pool e vault (GM di GMX, vault Beefy) quella serie
 * ha un punto ogni 3-5 ore, e la ricerca in cache a ±60 minuti non lo trova, mentre il dato all'istante e'
 * molto piu' fitto (stesso token, stessa sera: punti alle 19:02 e alle 02:02 in {@code /chart}, quotazioni
 * alle 20:19, 21:07, 22:14, 23:27 all'istante). In piu' {@code /chart} ha un tetto di 500 punti per
 * <i>chiamata</i>, quindi a gruppi copre poche ore per token, e il programma lo chiamava con 2 secondi di
 * pausa per token. Misure in {@code nocommit/Documentazione/Analisi_Prezzi_LP_DefiLlama.md}.
 *
 * <p>Regole (decisione dell'utente, 2026-10-09):
 * <ul>
 *   <li>un prezzo vale se l'affidabilita' ({@code confidence}) di DefiLlama supera
 *       {@link #SOGLIA_AFFIDABILITA}, altrimenti solo se il token e' nell'elenco CoinGecko
 *       ({@code GESTITICOINGECKO}), cioe' come fino a oggi, quando l'affidabilita' non era guardata;</li>
 *   <li>la quotazione deve stare entro {@link #FINESTRA_MS} da un istante chiesto, misurata sul
 *       timestamp che DefiLlama restituisce, che e' anche quello salvato in cache
 *       ({@link Prezzi#isPrezzoPreciso} misura la distanza dall'ora della quotazione). Il parametro
 *       {@code searchWidth=1h} cerca un'ora <b>per lato</b> (verificato: 30m non trova un punto a 33
 *       minuti, 1h si');</li>
 *   <li>i prezzi si salvano nella cache come prima (exchange {@code defillama}, simbolo vuoto, address
 *       scritto come lo passa il chiamante, perche' la lettura lo confronta esatto): il resto del programma
 *       li legge da li' senza sapere da dove arrivano;</li>
 *   <li>{@code PrezziKO} non si scrive qui: lo fa {@link Prezzi#CambioAddressEUR} come prima.</li>
 * </ul>
 *
 * <p>Ogni coppia (token, istante) si chiede una volta sola per sessione, riuscita o no
 * ({@link #GiaChiestoAllIstante}): e' cio' che rende i pre-scarichi a gruppi la strada normale e la
 * richiesta singola di {@link Prezzi#CambioAddressEUR} l'eccezione.
 */
public class PrezziDefiLlama {

    /** Affidabilita' minima, esclusa, per prendere il prezzo di un token fuori dall'elenco CoinGecko. */
    static final double SOGLIA_AFFIDABILITA = 0.9;

    /** Distanza massima fra la quotazione e l'istante chiesto: la stessa della lettura in cache. */
    static final long FINESTRA_MS = 3600000L;

    /**
     * Lunghezza massima dell'URL di una chiamata. Il server rifiuta (414) attorno ai 9.000 caratteri:
     * misurato il 2026-10-09, 8.581 accettati, 9.231 rifiutati.
     */
    static final int LUNGHEZZA_MAX_URL = 6000;

    static final String EXCHANGE = "defillama";

    private static final String URL_BASE = "https://coins.llama.fi/batchHistorical?coins=";

    /** Prima data per cui si cercano prezzi, come in {@link Prezzi#CambioXXXEUR} (01/01/2017). */
    private static final long ANNO_2017 = 1483225200000L;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS).build();

    /** Un prezzo che serve: il token (address come lo usera' la valorizzazione) a un istante, in millisecondi. */
    record Coppia(String address, String rete, long istante) {}

    /** Una quotazione letta dalla risposta, gia' accettata, ancora in dollari. */
    record Quotazione(String address, String rete, long timestamp, double prezzoUSD) {}

    /**
     * Nome della rete per DefiLlama, dalla colonna 4 di {@link Principale#Mappa_ChainExplorer}.
     * @return {@code null} se la rete e' vuota o DefiLlama non la conosce
     */
    static String NomeRete(String rete) {
        if (rete == null || rete.isBlank() || Principale.Mappa_ChainExplorer == null) return null;
        String[] info = Principale.Mappa_ChainExplorer.get(rete);
        if (info == null || info.length < 5 || info[4] == null || info[4].isBlank()) return null;
        return info[4];
    }

    /** Il token e' nell'elenco CoinGecko? (Scarica l'elenco se ha piu' di un giorno.) */
    static boolean InElencoCoinGecko(String address, String rete) {
        Prezzi.RecuperaCoinsCoingecko();
        return DatabaseH2.GestitiCoingecko_Leggi(address + "_" + rete) != null;
    }

    /**
     * Regola di accettazione di un prezzo DefiLlama.
     * @param affidabilita la {@code confidence} restituita, {@code null} se assente
     */
    static boolean Ammesso(Double affidabilita, boolean inElencoCoinGecko) {
        if (affidabilita != null && affidabilita > SOGLIA_AFFIDABILITA) return true;
        return inElencoCoinGecko;
    }

    /**
     * L'istante e' gia' stato chiesto a DefiLlama in questa sessione (di solito da un pre-scarico di gruppo)?
     * Allora la risposta entro l'ora c'e' gia' stata, e se in cache non c'e' un prezzo DefiLlama non lo ha.
     * Senza questo controllo ogni token senza prezzo rifaceva la sua chiamata: con {@code /chart}, che aveva
     * 2 secondi di pausa, un centinaio di token portava Giacenze a data da 26 secondi a quasi 5 minuti.
     */
    static boolean GiaChiestoAllIstante(String address, String rete, long istante) {
        return Prezzi.managerRichieste.isAlreadyRequested(ChiaveSessione(address, rete), istante, istante);
    }

    private static String ChiaveSessione(String address, String rete) {
        return "DLI_" + address.toUpperCase() + "_" + rete;
    }

    /**
     * Prezzo di un solo token a un istante, salvato in cache: la strada di {@link Prezzi#CambioAddressEUR}
     * quando nessun pre-scarico l'ha gia' chiesto. Il chiamante lo rilegge dalla cache.
     * @return {@code true} se e' stato scritto un prezzo
     */
    static boolean ScaricaIstante(long istante, String address, String rete) {
        List<Coppia> una = new ArrayList<>();
        una.add(new Coppia(address, rete, istante));
        return ScaricaCoppie(una, null) > 0;
    }

    /**
     * Scarica da {@code /batchHistorical} i prezzi delle coppie (token, istante) e li salva in cache, in
     * chiamate da al massimo {@link #LUNGHEZZA_MAX_URL} caratteri. Si saltano le reti senza nome DefiLlama,
     * gli istanti fuori dal 2017-oggi e le coppie gia' chieste nella sessione. Nessun filtro su prezzi locali
     * o KO: lo fanno i chiamanti, che sanno cosa la valorizzazione cerchera'.
     *
     * @param progress finestra di avanzamento, puo' essere {@code null}
     * @return quante quotazioni sono state salvate
     */
    static int ScaricaCoppie(Collection<Coppia> coppie, Download progress) {
        if (coppie == null || coppie.isEmpty()) return 0;
        //Stesso blocco di CambioXXXEUR e del pre-scarico CCXT: offline non si va in rete
        //(Calcoli_RW_GoldenMasterTest resta deterministico solo cosi').
        if (!Funzioni.CeConnessioneInternet() && !AttesaConnessione.Attendi()) return 0;

        long adesso = System.currentTimeMillis();
        //chiave DefiLlama in minuscolo -> grafie address/rete del chiamante, e istanti da chiedere
        Map<String, List<String[]>> destinazioni = new LinkedHashMap<>();
        Map<String, TreeSet<Long>> istanti = new LinkedHashMap<>();
        for (Coppia c : coppie) {
            if (c == null || c.address() == null || c.rete() == null) continue;
            if (c.istante() > adesso || c.istante() < ANNO_2017) continue;
            String nomeRete = NomeRete(c.rete());
            if (nomeRete == null) continue;
            if (GiaChiestoAllIstante(c.address(), c.rete(), c.istante())) continue;
            String chiave = (nomeRete + ":" + c.address()).toLowerCase();
            List<String[]> d = destinazioni.computeIfAbsent(chiave, k -> new ArrayList<>());
            boolean presente = false;
            for (String[] x : d) presente |= x[0].equals(c.address()) && x[1].equals(c.rete());
            if (!presente) d.add(new String[]{c.address(), c.rete()});
            istanti.computeIfAbsent(chiave, k -> new TreeSet<>()).add(c.istante());
        }
        if (istanti.isEmpty()) return 0;

        Prezzi.RecuperaTassiCambioEURUSD();
        if (Prezzi.MappaConversioneUSDEUR.isEmpty()) return 0;

        List<Map<String, List<Long>>> blocchi = Blocchi(istanti);
        if (progress != null) progress.SetLabel("Prezzi DefiLlama: " + istanti.size() + " token, " + blocchi.size() + " richieste...");
        int salvate = 0;
        for (Map<String, List<Long>> blocco : blocchi) {
            if (Interruzione.Richiesta()) break;
            String corpo;
            try (Response risposta = HTTP.newCall(new Request.Builder().url(UrlBlocco(blocco)).build()).execute()) {
                if (!risposta.isSuccessful() || risposta.body() == null) {
                    //Non si segna nulla: un errore del server non deve impedire di riprovare
                    System.out.println("DefiLlama, prezzi agli istanti: risposta " + risposta.code());
                    continue;
                }
                corpo = risposta.body().string();
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                continue;
            }
            if (VarCondivise.LogJsonPrezzi) System.out.println(corpo);

            List<Quotazione> accettate;
            try {
                accettate = Interpreta(corpo, blocco, destinazioni, t -> InElencoCoinGecko(t[0], t[1]));
            } catch (RuntimeException ex) {
                LoggerGC.ScriviErrore(ex);
                continue;
            }
            salvate += Salva(accettate);
            for (Map.Entry<String, List<Long>> e : blocco.entrySet()) {
                for (String[] d : destinazioni.get(e.getKey())) {
                    for (long i : e.getValue()) Prezzi.managerRichieste.addRange(ChiaveSessione(d[0], d[1]), i, i);
                }
            }
        }
        return salvate;
    }

    /**
     * Divide le richieste in blocchi che stanno nell'URL. La lunghezza si stima sulla forma codificata:
     * ogni token costa la sua chiave piu' ~20 caratteri di virgolette e parentesi, ogni istante 13 (dieci
     * cifre e la virgola). Un token con molti istanti puo' finire su piu' blocchi.
     */
    static List<Map<String, List<Long>>> Blocchi(Map<String, TreeSet<Long>> istanti) {
        int fisso = URL_BASE.length() + "&searchWidth=1h".length() + 6;
        List<Map<String, List<Long>>> blocchi = new ArrayList<>();
        Map<String, List<Long>> corrente = new LinkedHashMap<>();
        int lunghezza = fisso;
        for (Map.Entry<String, TreeSet<Long>> e : istanti.entrySet()) {
            for (long i : e.getValue()) {
                int costo = 13 + (corrente.containsKey(e.getKey()) ? 0 : e.getKey().length() + 20);
                if (lunghezza + costo > LUNGHEZZA_MAX_URL && !corrente.isEmpty()) {
                    blocchi.add(corrente);
                    corrente = new LinkedHashMap<>();
                    lunghezza = fisso;
                    costo = 13 + e.getKey().length() + 20;
                }
                corrente.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(i);
                lunghezza += costo;
            }
        }
        if (!corrente.isEmpty()) blocchi.add(corrente);
        return blocchi;
    }

    static String UrlBlocco(Map<String, List<Long>> blocco) {
        JsonObject coins = new JsonObject();
        for (Map.Entry<String, List<Long>> e : blocco.entrySet()) {
            JsonArray secondi = new JsonArray();
            for (long i : e.getValue()) secondi.add(i / 1000);
            coins.add(e.getKey(), secondi);
        }
        return URL_BASE + URLEncoder.encode(coins.toString(), StandardCharsets.UTF_8) + "&searchWidth=1h";
    }

    /**
     * Legge la risposta di {@code /batchHistorical} e tiene le quotazioni valide: entro {@link #FINESTRA_MS}
     * da uno degli istanti chiesti per quel token e ammesse da {@link #Ammesso}. Nessun accesso a rete o
     * database (l'elenco CoinGecko arriva da {@code inElenco}, interrogato solo se serve), per poterla
     * verificare nei test.
     *
     * @param istanti chiave DefiLlama in minuscolo ({@code rete:address}) -> istanti chiesti, in millisecondi
     * @param destinazioni chiave DefiLlama in minuscolo -> coppie {address, rete} da scrivere
     */
    static List<Quotazione> Interpreta(String json, Map<String, List<Long>> istanti,
            Map<String, List<String[]>> destinazioni, Predicate<String[]> inElenco) {
        List<Quotazione> accettate = new ArrayList<>();
        JsonObject coins = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("coins");
        if (coins == null) return accettate;
        for (Map.Entry<String, JsonElement> e : coins.entrySet()) {
            String chiave = e.getKey().toLowerCase();
            List<Long> chiesti = istanti.get(chiave);
            List<String[]> dest = destinazioni.get(chiave);
            if (chiesti == null || dest == null || !e.getValue().isJsonObject()) continue;
            JsonArray prezzi = e.getValue().getAsJsonObject().getAsJsonArray("prices");
            if (prezzi == null) continue;
            Boolean inElencoToken = null;
            Set<Long> visti = new HashSet<>();
            for (JsonElement el : prezzi) {
                if (!el.isJsonObject()) continue;
                JsonObject p = el.getAsJsonObject();
                if (!p.has("price") || !p.has("timestamp") || p.get("price").isJsonNull()) continue;
                double prezzo = p.get("price").getAsDouble();
                long ts = p.get("timestamp").getAsLong() * 1000;
                if (prezzo <= 0 || !visti.add(ts)) continue;
                boolean vicino = false;
                for (long i : chiesti) vicino |= Math.abs(ts - i) <= FINESTRA_MS;
                if (!vicino) continue;
                Double affidabilita = (p.has("confidence") && !p.get("confidence").isJsonNull())
                        ? p.get("confidence").getAsDouble() : null;
                if (affidabilita == null || affidabilita <= SOGLIA_AFFIDABILITA) {
                    if (inElencoToken == null) inElencoToken = inElenco.test(dest.get(0));
                }
                if (!Ammesso(affidabilita, inElencoToken != null && inElencoToken)) continue;
                for (String[] d : dest) accettate.add(new Quotazione(d[0], d[1], ts, prezzo));
            }
        }
        return accettate;
    }

    /**
     * Cambio USD->EUR del giorno della quotazione, o dei cinque giorni precedenti se manca
     * (fine settimana, festivi), come faceva la lettura di {@code /chart}.
     * @return {@code null} se non c'e'
     */
    static String TassoUSDEUR(long timestamp) {
        String giorno = FunzioniDate.ConvertiDatadaLong(timestamp);
        String tasso = Prezzi.MappaConversioneUSDEUR.get(giorno);
        for (int i = 0; i < 5 && tasso == null; i++) {
            giorno = FunzioniDate.GiornoMenoUno(giorno);
            tasso = Prezzi.MappaConversioneUSDEUR.get(giorno);
        }
        return tasso;
    }

    private static int Salva(List<Quotazione> quotazioni) {
        if (quotazioni.isEmpty()) return 0;
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, ?, ?)";
        int scritte = 0;
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (Quotazione q : quotazioni) {
                String tasso = TassoUSDEUR(q.timestamp());
                if (tasso == null) continue;
                ps.setLong(1, q.timestamp());
                ps.setString(2, EXCHANGE);
                ps.setString(3, "");
                ps.setDouble(4, q.prezzoUSD() * Double.parseDouble(tasso));
                ps.setString(5, q.rete());
                ps.setString(6, q.address());
                ps.addBatch();
                scritte++;
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            return 0;
        }
        return scritte;
    }

    /**
     * Pre-scarico di coppie che la valorizzazione cerchera' per address: tiene solo quelle per cui
     * {@link Prezzi#CambioAddressEUR} andrebbe davvero in rete, cioe' senza alias (quelle si prezzano per
     * simbolo), senza prezzo locale all'istante e senza KO all'istante. Un pre-scarico non deve mai
     * cambiare quale prezzo viene scelto, solo quanto costa averlo: in cache vince la quotazione piu' vicina
     * ({@link Prezzi#DammiPrezzoDaDatabase}), quindi una riga nuova accanto a un prezzo gia' usato,
     * per esempio in una dichiarazione, potrebbe soppiantarlo.
     *
     * @param origine etichetta per il log (es. "import BSC", "giacenze a data")
     * @return quante quotazioni sono state salvate
     */
    static int PreScaricaCoppie(Collection<Coppia> candidate, String origine, Download progress) {
        if (candidate == null || candidate.isEmpty()) return 0;
        long inizio = System.currentTimeMillis();
        Set<Coppia> uniche = new LinkedHashSet<>(candidate);
        List<Coppia> daChiedere = new ArrayList<>();
        for (Coppia c : uniche) {
            if (Interruzione.Richiesta()) return 0;
            if (c.address() == null || c.rete() == null || c.rete().isBlank()) continue;
            if (!Funzioni_WalletDeFi.isValidAddress(c.address(), c.rete())) continue;
            if (NomeRete(c.rete()) == null) continue;
            if (GiaChiestoAllIstante(c.address(), c.rete(), c.istante())) continue;
            if (AliasPrezziToken.Alias(c.address(), c.rete(), c.istante()) != null) continue;
            if (Prezzi.HaPrezzoLocaleAddress(c.istante(), c.address(), c.rete())) continue;
            if (Prezzi.PrezzoIrrecuperabileDaDB_Leggi("", c.istante(), c.rete(), c.address())) continue;
            daChiedere.add(c);
        }
        if (daChiedere.isEmpty()) return 0;
        int salvate = ScaricaCoppie(daChiedere, progress);
        System.out.println("Pre-scarico prezzi DefiLlama (" + origine + "): " + daChiedere.size() + " coppie token-istante senza prezzo locale, "
                + salvate + " quotazioni salvate, " + (System.currentTimeMillis() - inizio) + " ms");
        return salvate;
    }

    /**
     * Pre-scarico per una valorizzazione a una data (W/RW, T/RT, Giacenze a data): i token con address che
     * {@code PreScaricaPrezziMonete} non manda agli exchange, tutti allo stesso istante. Le esclusioni sono
     * quelle del pre-scarico CCXT, piu' gli NFT.
     *
     * @return quante quotazioni sono state salvate
     */
    static int PreScaricaMonete(Collection<Moneta> monete, long data, Download progress, String origine) {
        List<Coppia> candidate = new ArrayList<>();
        for (Moneta m : monete) {
            if (m == null || m.Moneta == null || m.Moneta.isBlank()) continue;
            if (m.Tipo != null && (m.Tipo.trim().equalsIgnoreCase("FIAT") || m.Tipo.trim().equalsIgnoreCase("NFT"))) continue;
            if (Funzioni.isSCAM(m.Moneta)) continue;
            if (Prezzi.EMoneyAncoratoAdEuro(m.Moneta, data)) continue;
            if (Prezzi.QtaZero(m)) continue;
            candidate.add(new Coppia(m.MonetaAddress, m.Rete == null ? "" : m.Rete, data));
        }
        return PreScaricaCoppie(candidate, origine, progress);
    }

    /**
     * Pre-scarico per movimenti gia' formati che stanno per essere riprezzati (Ricalcola prezzi, Trasla
     * orario): gli stessi bivi di {@link Prezzi#RaccogliRichiestePerMovimenti} per le gambe che la
     * valorizzazione cerchera' per address, all'istante che usa {@link Prezzi#DammiPrezzoDaTransazione}.
     */
    static int PreScaricaMovimenti(Collection<String[]> movimenti, int annoMinimo, Download progress) {
        if (movimenti == null || movimenti.isEmpty()) return 0;
        List<Coppia> candidate = new ArrayList<>();
        for (String[] v : movimenti) {
            if (v == null || v.length < 29) continue;
            if (annoMinimo > 0) {
                try {
                    if (v[0] == null || v[0].length() < 4 || Integer.parseInt(v[0].substring(0, 4)) < annoMinimo) continue;
                } catch (NumberFormatException ex) {
                    continue;
                }
            }
            if (v[14] != null && !v[14].isBlank()) continue;
            long data = FunzioniDate.ConvertiDatainLongMinuto(v[1]);
            if (data <= 0) continue;
            String rete;
            try {
                rete = Funzioni.TrovaReteDaIMovimento(v);
            } catch (RuntimeException ex) {
                continue;
            }
            if (rete == null || Principale.MappaRetiSupportate.get(rete) == null) continue;
            for (int k = 0; k < 2; k++) {
                String moneta = k == 0 ? v[8] : v[11];
                String tipo = k == 0 ? v[9] : v[12];
                String address = k == 0 ? v[26] : v[28];
                if (moneta == null || moneta.isBlank() || address == null || address.isBlank()) continue;
                if (tipo != null && (tipo.trim().equalsIgnoreCase("FIAT") || tipo.trim().equalsIgnoreCase("NFT"))) continue;
                if (Funzioni.isSCAM(moneta)) continue;
                candidate.add(new Coppia(address, rete, data));
            }
        }
        return PreScaricaCoppie(candidate, "movimenti", progress);
    }
}
