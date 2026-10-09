package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Prezzi di DefiLlama per i token con address, chiesti <b>a un istante preciso</b>
 * ({@code /prices/historical}), anche per molti token in una sola chiamata.
 *
 * <p>Serve ai token che la serie oraria {@code /chart} non copre: per i token di pool e vault
 * (GM di GMX, vault Beefy) quella serie ha un punto ogni 3-5 ore, e la ricerca in cache a ±60
 * minuti non lo trova. Chiesto per l'istante, DefiLlama risponde con una quotazione entro circa
 * mezz'ora. Analisi completa in {@code nocommit/Documentazione/Analisi_Prezzi_LP_DefiLlama.md}.
 *
 * <p>Regole (decisione dell'utente, 2026-10-09):
 * <ul>
 *   <li>un prezzo vale se l'affidabilita' ({@code confidence}) di DefiLlama supera
 *       {@link #SOGLIA_AFFIDABILITA}, altrimenti solo se il token e' nell'elenco CoinGecko
 *       ({@code GESTITICOINGECKO}), cioe' come fino a oggi, quando l'affidabilita' non era guardata;</li>
 *   <li>la quotazione deve stare entro {@link #FINESTRA_MS} dall'istante chiesto, misurata sul
 *       timestamp che DefiLlama restituisce, che e' anche quello salvato in cache
 *       ({@link Prezzi#isPrezzoPreciso} misura la distanza dall'ora della quotazione);</li>
 *   <li>i prezzi si salvano nella cache come quelli di {@code /chart} (exchange {@code defillama},
 *       simbolo vuoto, address scritto come lo passa il chiamante, perche' la lettura lo confronta
 *       esatto): il resto del programma li legge da li' senza sapere da dove arrivano;</li>
 *   <li>{@code PrezziKO} non si legge e non si scrive qui: lo fa {@link Prezzi#CambioAddressEUR}
 *       come prima. Il pre-scarico scrive in cache, che e' letta prima dei KO.</li>
 * </ul>
 */
public class PrezziDefiLlama {

    /** Affidabilita' minima, esclusa, per prendere il prezzo di un token fuori dall'elenco CoinGecko. */
    static final double SOGLIA_AFFIDABILITA = 0.9;

    /** Distanza massima fra la quotazione e l'istante chiesto: la stessa della lettura in cache. */
    static final long FINESTRA_MS = 3600000L;

    /** Token per chiamata: oltre, l'URL diventa troppo lungo. */
    static final int TOKEN_PER_RICHIESTA = 80;

    static final String EXCHANGE = "defillama";

    /** Prima data per cui si cercano prezzi, come in {@link Prezzi#CambioXXXEUR} (01/01/2017). */
    private static final long ANNO_2017 = 1483225200000L;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS).build();

    /**
     * Esito di {@code /chart} per token ({@code address_rete}), nella sessione: se DefiLlama non
     * conosce il token, o lo conosce ma con un'affidabilita' insufficiente, la richiesta all'istante
     * non darebbe di piu' e non si fa. Un token mai chiesto a {@code /chart} non e' in mappa.
     */
    private static final Map<String, Boolean> EsitoSerie = new ConcurrentHashMap<>();

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
     * Regola di accettazione di un prezzo DefiLlama, unica per {@code /chart} e per l'istante preciso.
     * @param affidabilita la {@code confidence} restituita, {@code null} se assente
     */
    static boolean Ammesso(Double affidabilita, boolean inElencoCoinGecko) {
        if (affidabilita != null && affidabilita > SOGLIA_AFFIDABILITA) return true;
        return inElencoCoinGecko;
    }

    /** Registra l'esito di {@code /chart} per un token (vedi {@link #EsitoSerie}). */
    static void RegistraEsitoSerie(String address, String rete, boolean ammesso) {
        EsitoSerie.put(Chiave(address, rete), ammesso);
    }

    /**
     * Vale la pena chiedere l'istante preciso dopo che {@code /chart} non ha dato un punto entro l'ora?
     * Si', salvo che {@code /chart} abbia gia' detto che il token non c'e' o non e' ammesso.
     */
    static boolean DaChiedereAllIstante(String address, String rete) {
        return !Boolean.FALSE.equals(EsitoSerie.get(Chiave(address, rete)));
    }

    /**
     * L'istante e' gia' stato chiesto a DefiLlama in questa sessione (di solito dal pre-scarico di gruppo)?
     * Allora la risposta entro l'ora c'e' gia' stata, e se in cache non c'e' un prezzo DefiLlama non lo ha:
     * richiederlo a {@code /chart} costerebbe una chiamata con 2 secondi di pausa per ogni token, che su un
     * archivio con un centinaio di token senza prezzo portava Giacenze a data da 26 secondi a quasi 5 minuti.
     */
    static boolean GiaChiestoAllIstante(String address, String rete, long istante) {
        return Prezzi.managerRichieste.isAlreadyRequested("DLI_" + Chiave(address, rete), istante, istante);
    }

    private static String Chiave(String address, String rete) {
        return address.toUpperCase() + "_" + rete;
    }

    /**
     * Prezzo di un solo token all'istante, salvato in cache. Il chiamante lo rilegge da li'.
     * @return {@code true} se e' stato scritto un prezzo
     */
    static boolean ScaricaIstante(long istante, String address, String rete) {
        List<String[]> uno = new ArrayList<>();
        uno.add(new String[]{address, rete});
        return ScaricaIstante(istante, uno, null) > 0;
    }

    /**
     * Prezzi di molti token allo stesso istante, a gruppi di {@link #TOKEN_PER_RICHIESTA}, salvati in cache.
     * Ogni token si chiede una volta sola per istante nella sessione, riuscito o no: un token che
     * DefiLlama non prezza non fa ripetere la chiamata a ogni ricalcolo.
     *
     * @param token coppie {address, rete}; quelle su reti senza nome DefiLlama si saltano
     * @param progress finestra di avanzamento, puo' essere {@code null}
     * @return quanti token hanno avuto un prezzo
     */
    static int ScaricaIstante(long istante, Collection<String[]> token, Download progress) {
        if (token == null || token.isEmpty()) return 0;
        if (istante > System.currentTimeMillis() || istante < ANNO_2017) return 0;
        //Stesso blocco di CambioXXXEUR e del pre-scarico CCXT: offline non si va in rete
        //(Calcoli_RW_GoldenMasterTest resta deterministico solo cosi').
        if (!Funzioni.CeConnessioneInternet() && !AttesaConnessione.Attendi()) return 0;

        //chiave DefiLlama in minuscolo -> address/rete come li passa il chiamante (anche piu' grafie)
        Map<String, List<String[]>> perChiave = new LinkedHashMap<>();
        for (String[] t : token) {
            if (t == null || t[0] == null || t[1] == null) continue;
            String nomeRete = NomeRete(t[1]);
            if (nomeRete == null) continue;
            String chiaveSessione = "DLI_" + Chiave(t[0], t[1]);
            if (Prezzi.managerRichieste.isAlreadyRequested(chiaveSessione, istante, istante)) continue;
            perChiave.computeIfAbsent((nomeRete + ":" + t[0]).toLowerCase(), k -> new ArrayList<>()).add(t);
        }
        if (perChiave.isEmpty()) return 0;

        Prezzi.RecuperaTassiCambioEURUSD();
        if (Prezzi.MappaConversioneUSDEUR.isEmpty()) return 0;

        List<String> chiavi = new ArrayList<>(perChiave.keySet());
        if (progress != null) progress.SetLabel("Prezzi DefiLlama: " + chiavi.size() + " token...");
        int prezzati = 0;
        for (int i = 0; i < chiavi.size(); i += TOKEN_PER_RICHIESTA) {
            if (Interruzione.Richiesta()) break;
            List<String> blocco = chiavi.subList(i, Math.min(i + TOKEN_PER_RICHIESTA, chiavi.size()));
            String url = "https://coins.llama.fi/prices/historical/" + (istante / 1000) + "/"
                    + String.join(",", blocco) + "?searchWidth=1h";
            String corpo;
            try (Response risposta = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
                if (!risposta.isSuccessful() || risposta.body() == null) {
                    //Non si segna nulla: un errore del server non deve impedire di riprovare
                    System.out.println("DefiLlama, prezzi all'istante: risposta " + risposta.code());
                    continue;
                }
                corpo = risposta.body().string();
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                continue;
            }
            if (VarCondivise.LogJsonPrezzi) System.out.println(corpo);

            Map<String, List<String[]>> sottoinsieme = new LinkedHashMap<>();
            for (String k : blocco) sottoinsieme.put(k, perChiave.get(k));
            List<Quotazione> accettate;
            try {
                accettate = Interpreta(corpo, istante, sottoinsieme, t -> InElencoCoinGecko(t[0], t[1]));
            } catch (RuntimeException ex) {
                LoggerGC.ScriviErrore(ex);
                continue;
            }
            prezzati += Salva(accettate);
            for (String k : blocco) {
                for (String[] t : perChiave.get(k)) {
                    Prezzi.managerRichieste.addRange("DLI_" + Chiave(t[0], t[1]), istante, istante);
                }
            }
        }
        System.out.println("DefiLlama, prezzi all'istante " + FunzioniDate.ConvertiDatadaLongAlSecondo(istante)
                + ": " + prezzati + " su " + chiavi.size() + " token");
        return prezzati;
    }

    /**
     * Legge la risposta di {@code /prices/historical} e tiene le quotazioni valide: entro
     * {@link #FINESTRA_MS} dall'istante e ammesse da {@link #Ammesso}. Nessun accesso a rete o database
     * (l'elenco CoinGecko arriva da {@code inElenco}), per poterla verificare nei test.
     *
     * @param perChiave chiave DefiLlama in minuscolo ({@code rete:address}) -> coppie {address, rete} da scrivere
     */
    static List<Quotazione> Interpreta(String json, long istante, Map<String, List<String[]>> perChiave,
            Predicate<String[]> inElenco) {
        List<Quotazione> accettate = new ArrayList<>();
        JsonObject radice = JsonParser.parseString(json).getAsJsonObject();
        JsonObject coins = radice.getAsJsonObject("coins");
        if (coins == null) return accettate;
        for (Map.Entry<String, JsonElement> e : coins.entrySet()) {
            List<String[]> destinazioni = perChiave.get(e.getKey().toLowerCase());
            if (destinazioni == null || !e.getValue().isJsonObject()) continue;
            JsonObject c = e.getValue().getAsJsonObject();
            if (!c.has("price") || !c.has("timestamp") || c.get("price").isJsonNull()) continue;
            double prezzo = c.get("price").getAsDouble();
            long ts = c.get("timestamp").getAsLong() * 1000;
            if (prezzo <= 0 || Math.abs(ts - istante) > FINESTRA_MS) continue;
            Double affidabilita = (c.has("confidence") && !c.get("confidence").isJsonNull())
                    ? c.get("confidence").getAsDouble() : null;
            if (!Ammesso(affidabilita, inElenco.test(destinazioni.get(0)))) continue;
            for (String[] d : destinazioni) accettate.add(new Quotazione(d[0], d[1], ts, prezzo));
        }
        return accettate;
    }

    /**
     * Cambio USD->EUR del giorno della quotazione, o dei cinque giorni precedenti se manca
     * (fine settimana, festivi), come per {@code /chart}.
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
     * Pre-scarico per una valorizzazione a una data (W/RW, T/RT, Giacenze a data): chiede a DefiLlama,
     * in gruppo, i token con address che <b>non</b> hanno gia' un prezzo locale all'istante.
     *
     * <p>Il "solo se manca" non e' un'ottimizzazione: in cache vince la quotazione piu' vicina
     * all'istante ({@link Prezzi#DammiPrezzoDaDatabase}), quindi una riga DefiLlama nuova potrebbe
     * soppiantare quella con cui un anno e' gia' stato dichiarato. Le esclusioni sono quelle del
     * pre-scarico CCXT, piu' gli NFT e i token con un alias, che si prezzano per simbolo.
     *
     * @return quanti token hanno avuto un prezzo
     */
    static int PreScaricaMonete(Collection<Moneta> monete, long data, Download progress, String origine) {
        long inizio = System.currentTimeMillis();
        Map<String, String[]> daChiedere = new LinkedHashMap<>();
        for (Moneta m : monete) {
            if (Interruzione.Richiesta()) return 0;
            if (m == null || m.Moneta == null || m.Moneta.isBlank()) continue;
            if (m.Tipo != null && (m.Tipo.trim().equalsIgnoreCase("FIAT") || m.Tipo.trim().equalsIgnoreCase("NFT"))) continue;
            if (Funzioni.isSCAM(m.Moneta)) continue;
            if (Prezzi.EMoneyAncoratoAdEuro(m.Moneta, data)) continue;
            if (Prezzi.QtaZero(m)) continue;
            String rete = m.Rete == null ? "" : m.Rete;
            String address = m.MonetaAddress;
            if (rete.isBlank() || !Funzioni_WalletDeFi.isValidAddress(address, rete)) continue;
            if (NomeRete(rete) == null) continue;
            if (AliasPrezziToken.Alias(address, rete, data) != null) continue;
            String k = address + "_" + rete;
            if (daChiedere.containsKey(k)) continue;
            if (Prezzi.HaPrezzoLocaleAddress(data, address, rete)) continue;
            daChiedere.put(k, new String[]{address, rete});
        }
        if (daChiedere.isEmpty()) return 0;
        int prezzati = ScaricaIstante(data, daChiedere.values(), progress);
        System.out.println("Pre-scarico prezzi DefiLlama (" + origine + "): " + daChiedere.size() + " token senza prezzo locale, "
                + prezzati + " prezzati, " + (System.currentTimeMillis() - inizio) + " ms");
        return prezzati;
    }
}
