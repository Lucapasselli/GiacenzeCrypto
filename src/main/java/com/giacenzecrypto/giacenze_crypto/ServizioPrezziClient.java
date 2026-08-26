package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client per il servizio prezzi remoto (cache condivisa fra tutte le installazioni, prezzi letti
 * direttamente dallo stato delle pool DEX on-chain — vedi
 * nocommit/Documentazione/API_ServizioPrezzi.md e Analisi_VPS_Prezzi_Sito.md, "Fase 1-bis").
 * Pensato come **puramente additivo**: {@link Prezzi#CambioXXXEUR} lo interroga PRIMA del
 * recupero locale via CCXT ({@link Prezzi#RecuperaPrezziDaCCXT}), e solo quando non è stata
 * richiesta una fonte specifica ({@code Fonte} vuota); se il servizio trova il prezzo, i punti —
 * già in **EUR** (il server converte lato suo, con il cambio EUR/USD letto anch'esso on-chain da
 * una pool EURC/USDC, più preciso del cambio giornaliero di Banca d'Italia — vedi
 * Analisi_VPS_Prezzi_Sito.md, "Fase 1-ter") — vengono scritti nella stessa cache locale (tabella
 * {@code PrezziNew}, tramite {@link Prezzi#ScriviPuntiPrezzoInCache}) e il recupero locale viene
 * saltato. In ogni altro caso — servizio irraggiungibile, occupato, moneta fuori dalla lista che
 * copre, interruttore di sessione attivo, opzione disattivata, **oppure esito "assenza
 * confermata"** (il servizio copre solo poche decine di monete on-chain: non trovarcela non
 * significa che CCXT non la trovi altrove) — si ripiega comunque sul recupero locale via CCXT,
 * identico a prima: l'app non deve mai perdere un prezzo solo perché la cache remota non lo ha.
 *
 * Nessuna lista delle monete coperte è tenuta **fissa** qui: {@code config/pool_onchain.json} vive
 * solo sul server (può cambiare senza redeploy del client). {@link #tentaRecupero} interroga però
 * {@code GET /v1/monete} una volta per sessione (vedi {@link #moneteEsposte()}) per sapere quali
 * simboli sono davvero esposti prima di provare {@code /v1/prezzo} — evita un round-trip a vuoto
 * per ogni moneta non coperta, invece di scoprirlo un simbolo alla volta dal primo {@code 400}
 * come accadeva prima. Se quella chiamata fallisce (servizio giù, rete assente) non succede
 * nulla di diverso da prima: si ripiega sulla scoperta reattiva via {@code 400}, ricordata in
 * {@link #SIMBOLI_NON_SUPPORTATI} esattamente come oggi — nessun elenco duplicato da tenere
 * allineato a mano, solo una richiesta in meno quando il servizio risponde.
 */
public class ServizioPrezziClient {

    /**
     * Chiave dell'opzione persistita (tabella {@code OPZIONI} di {@code personale.mv.db}) che
     * abilita il servizio — casella in Opzioni, letta da {@link Principale#AggiornaSpunte()} e
     * scritta dal relativo {@code ActionListener}. <b>Disattivata di default</b> ("NO" se
     * l'opzione non è mai stata scritta, vedi {@link #abilitato()}) — riportata a opt-in il
     * 2026-08-26 su richiesta dell'utente, per fare altri test prima di riattivarla di default;
     * era stata attivata di default lo stesso giorno. Quando abilitata, i prezzi arrivano dallo
     * stato delle pool DEX letto direttamente on-chain (vedi Analisi_VPS_Prezzi_Sito.md),
     * verificato in produzione e reso trasparente/verificabile da {@code GET /v1/monete} (monete
     * gestite, da quando ci sono dati, quali pool le alimentano). Sovrascrivibile per i test con
     * la proprietà di sistema {@code prezzi.servizio.abilitato}.
     */
    public static final String OPZIONE_ABILITATO = "ServizioPrezziOnchain_Abilitato";

    /** Valore di default di {@link #OPZIONE_ABILITATO} quando l'opzione non è mai stata scritta. */
    public static final String OPZIONE_ABILITATO_DEFAULT = "NO";

    /**
     * Codice breve scritto in {@code PrezziNew.exchange} per i prezzi arrivati da questo servizio,
     * al posto del nome interno che il server usa lato suo ({@code "onchain"}, vedi
     * {@link #rinominaFonte}): la cache locale può accumulare milioni di righe (vedi CLAUDE.md,
     * manutenzione del DB prezzi) e non ha senso ripetere un nome lungo su ognuna. Il nome per
     * esteso, mostrato all'utente, è {@link #NOME_FONTE} — tradotto una sola volta, alla lettura,
     * in {@link Prezzi.InfoPrezzo#InfoPrezzo(java.math.BigDecimal, String, long, java.math.BigDecimal, java.math.BigDecimal, String)}.
     */
    public static final String CODICE_FONTE = "GC";

    /** Nome per esteso di {@link #CODICE_FONTE}, mostrato all'utente al posto del codice breve. */
    public static final String NOME_FONTE = "giacenzecrypto.it";

    private static boolean abilitato() {
        String override = System.getProperty("prezzi.servizio.abilitato");
        if (override != null) return Boolean.parseBoolean(override);
        return "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ABILITATO, OPZIONE_ABILITATO_DEFAULT));
    }

    private static final String URL_BASE = "https://giacenzecrypto.it/v1/";

    /** Sovrascrivibile per sviluppo/test (es. {@code http://127.0.0.1:4173/v1/}). */
    private static String urlBase() {
        return System.getProperty("prezzi.servizio.urlbase", URL_BASE);
    }

    // Timeout differenziati: connessione rapida (il servizio deve risultare irraggiungibile
    // presto se la VPS è giù), lettura più larga (un fetch dal vivo lato server può richiedere
    // qualche secondo — misurato empiricamente 1-3s, vedi Analisi_VPS_Prezzi_Sito.md).
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    /**
     * Simboli per cui il server ha già risposto "moneta non gestita" in questa sessione: evita di
     * richiedere in rete, ripetutamente, ogni simbolo fuori dalla lista ristretta del servizio
     * (la maggioranza delle monete, oggi ~20 coperte su un archivio che ne contiene molte di più).
     */
    private static final Set<String> SIMBOLI_NON_SUPPORTATI = ConcurrentHashMap.newKeySet();

    /**
     * Interruttore di sessione: dopo troppi esiti "occupato"/errore consecutivi, il client smette
     * di provare il servizio per il resto della sessione (disegno in Analisi_VPS_Prezzi_Sito.md,
     * "Interruttore di sessione lato client") — evita di pagare un round-trip a vuoto a ogni
     * movimento se il server è sotto carico o irraggiungibile per un periodo lungo.
     */
    private static final int SOGLIA_INTERRUZIONE_SESSIONE = 5;
    private static final AtomicInteger FALLIMENTI_CONSECUTIVI = new AtomicInteger(0);
    private static volatile boolean sessioneInterrotta = false;

    /**
     * Cache, per l'intera sessione, dei simboli con {@code esposta:true} in {@code GET /v1/monete}
     * — {@code null} finché non è stata tentata una prima volta o se quel tentativo è fallito (in
     * quel caso {@link #moneteEsposte()} non filtra nulla: si ripiega sulla scoperta reattiva via
     * {@code 400} di {@link #SIMBOLI_NON_SUPPORTATI}, esattamente come prima che questo metodo
     * esistesse). Non è mai ritentata nella stessa sessione anche se fallisce, per lo stesso motivo
     * per cui {@link #SIMBOLI_NON_SUPPORTATI} non lo è: un servizio giù resta giù per un po'.
     */
    private static volatile Set<String> moneteEsposteCache;
    private static volatile boolean moneteEsposteTentato = false;
    private static final Object LOCK_MONETE_ESPOSTE = new Object();

    private ServizioPrezziClient() {
    }

    /** @return i simboli esposti secondo l'ultima {@code GET /v1/monete} riuscita in questa sessione, o {@code null} se non è mai riuscita. */
    private static Set<String> moneteEsposte() {
        if (moneteEsposteTentato) return moneteEsposteCache;
        synchronized (LOCK_MONETE_ESPOSTE) {
            if (moneteEsposteTentato) return moneteEsposteCache;
            moneteEsposteCache = recuperaMoneteEsposte();
            moneteEsposteTentato = true;
        }
        return moneteEsposteCache;
    }

    /**
     * Interroga {@code GET /v1/monete} una volta e ne estrae i soli simboli con {@code esposta:true}
     * (gli unici che {@code /v1/prezzo} può davvero risolvere, vedi {@code monetaAbilitata} lato
     * server). Nessun'eccezione visibile: qualunque problema (rete, JSON inatteso, campo mancante)
     * fa tornare {@code null}, trattato da {@link #moneteEsposte()} come "non so", non come "nessuna
     * moneta esposta" — un {@code null} non filtra, un insieme vuoto filtrerebbe tutto per errore.
     */
    private static Set<String> recuperaMoneteEsposte() {
        Request richiesta = new Request.Builder().url(urlBase() + "monete").get().build();
        try (Response risposta = HTTP_CLIENT.newCall(richiesta).execute()) {
            if (!risposta.isSuccessful()) return null;
            String corpo = risposta.body() != null ? risposta.body().string() : "";
            JsonElement root = JsonParser.parseString(corpo);
            if (!root.isJsonObject()) return null;
            JsonElement moneteEl = root.getAsJsonObject().get("monete");
            if (moneteEl == null || !moneteEl.isJsonArray()) return null;

            Set<String> risultato = new HashSet<>();
            for (JsonElement el : moneteEl.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject m = el.getAsJsonObject();
                if (!m.has("symbol") || !m.has("esposta")) continue;
                if (m.get("esposta").getAsBoolean()) {
                    risultato.add(m.get("symbol").getAsString().toUpperCase());
                }
            }
            return risultato;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Prova a risolvere il prezzo di {@code symbol} al tempo {@code timestampMs} tramite il
     * servizio remoto. Se ottiene un esito autorevole (prezzi trovati o assenza confermata),
     * scrive i punti nella cache locale (un batch vuoto per l'assenza confermata non scrive
     * nulla) e ritorna {@code true}. Ritorna {@code false} in ogni altro caso — irraggiungibile,
     * occupato/parziale, moneta non gestita, interruttore di sessione attivo. In entrambi i casi
     * il valore di ritorno non basta al chiamante per sapere se il prezzo è stato trovato: un
     * {@code true} copre anche l'assenza confermata, quindi {@link Prezzi#CambioXXXEUR} verifica
     * sempre la cache locale dopo la chiamata e ripiega su CCXT se il prezzo non c'è ancora.
     */
    public static boolean tentaRecupero(String symbol, long timestampMs) {
        if (!abilitato()) return false;
        if (sessioneInterrotta) return false;
        String simbolo = symbol.toUpperCase();
        if (SIMBOLI_NON_SUPPORTATI.contains(simbolo)) return false;

        // Filtro proattivo da GET /v1/monete (una volta per sessione, vedi moneteEsposte()): se
        // sappiamo già che il simbolo non è esposto risparmiamo l'intero round-trip su /prezzo,
        // anziché scoprirlo solo al primo 400. Se non lo sappiamo (mai riuscito a leggerlo, o
        // lettura fallita) non filtriamo nulla: si prova comunque, esattamente come prima.
        Set<String> esposte = moneteEsposte();
        if (esposte != null && !esposte.contains(simbolo)) {
            SIMBOLI_NON_SUPPORTATI.add(simbolo);
            return false;
        }

        // Nessuna chiave applicativa: il servizio è protetto solo dal rate limit per IP lato
        // server (vedi Analisi_VPS_Prezzi_Sito.md) — una chiave qui non aggiungeva altro, dato
        // che sarebbe comunque distribuita dentro il programma ed estraibile.
        Request richiesta = new Request.Builder()
                .url(urlBase() + "prezzo?symbol=" + simbolo + "&timestamp=" + timestampMs)
                .get()
                .build();

        try (Response risposta = HTTP_CLIENT.newCall(richiesta).execute()) {
            // 400: qui symbol e timestamp sono sempre validi (Prezzi.CambioXXXEUR li ha già
            // verificati prima di questa chiamata), quindi un 400 può significare solo "moneta
            // non gestita" — vedi API_ServizioPrezzi.md.
            if (risposta.code() == 400) {
                SIMBOLI_NON_SUPPORTATI.add(simbolo);
                return false;
            }
            // 503: sia "occupato" che "parziale" vanno trattati identicamente (vedi doc) — nessun
            // dato, il server stesso non ha toccato le proprie coperture in questo caso.
            if (risposta.code() == 503) {
                registraFallimento();
                return false;
            }
            if (!risposta.isSuccessful()) {
                // 429 (rate limit per IP), 500: mai un'eccezione visibile, solo un log leggero
                // e il fallback.
                System.err.println("Servizio prezzi: risposta " + risposta.code() + " per " + simbolo);
                registraFallimento();
                return false;
            }

            String corpo = risposta.body() != null ? risposta.body().string() : "";
            JsonElement root = JsonParser.parseString(corpo);
            if (!root.isJsonObject()) {
                registraFallimento();
                return false;
            }
            JsonObject obj = root.getAsJsonObject();
            String esito = obj.has("esito") ? obj.get("esito").getAsString() : "";
            if (!"risolto".equals(esito)) {
                registraFallimento();
                return false;
            }

            JsonArray punti = obj.has("punti") && obj.get("punti").isJsonArray()
                    ? obj.getAsJsonArray("punti")
                    : new JsonArray();
            // Il servizio remoto converte già in EUR lato server (cambio EUR/USD letto dalla
            // stessa pool on-chain EURC/USDC, vedi Analisi_VPS_Prezzi_Sito.md, "Fase 1-ter") —
            // qui non serve nessuna conversione, esattamente come il vecchio percorso CCXT.
            // "punti: []" è una risposta valida e definitiva (assenza di prezzo confermata): si
            // scrive comunque (un batch vuoto non fa nulla) e si ritorna true, senza che il
            // chiamante debba distinguere i due casi.
            Prezzi.ScriviPuntiPrezzoInCache(simbolo, rinominaFonte(punti));
            FALLIMENTI_CONSECUTIVI.set(0);
            // Un semplice System.out arriva già dove serve, senza dover passare una finestra
            // Download attraverso Prezzi.CambioXXXEUR: LoggerGC lo scrive su GiacenzeCrypto.log,
            // lo mostra nella finestra di log (--debug) e, quando è aperta, nel pannello di log
            // di Download.java (stesso meccanismo già usato da Prezzi.RecuperaPrezziDaCCXT).
            if (punti.size() > 0) {
                System.out.println("Servizio prezzi condiviso (giacenzecrypto.it): prezzo di " + simbolo
                        + " in data " + FunzioniDate.ConvertiDatadaLongAlSecondo(timestampMs)
                        + " recuperato dalla cache remota, nessun download diretto dagli exchange necessario.");
            } else {
                System.out.println("Servizio prezzi condiviso (giacenzecrypto.it): nessun prezzo di " + simbolo
                        + " in data " + FunzioniDate.ConvertiDatadaLongAlSecondo(timestampMs)
                        + " (assenza confermata dal server, verrà comunque ricercato anche in locale).");
            }
            return true;

        } catch (IOException ex) {
            // Servizio irraggiungibile (rete, DNS, timeout): mai un errore visibile, solo fallback.
            registraFallimento();
            return false;
        }
    }

    /**
     * Rinomina la fonte di ogni punto in {@link #CODICE_FONTE}, scartando il nome interno che il
     * server usa lato suo ({@code "onchain"} oggi — vedi API_ServizioPrezzi.md). Il servizio
     * risponde sempre con un'unica fonte per punto, quindi il nome originale non serve a niente:
     * si prende il (solo) valore presente, se c'è. Un punto senza prezzo ({@code "prices": {}},
     * assenza confermata) resta senza prezzo, non un errore.
     */
    private static JsonArray rinominaFonte(JsonArray punti) {
        JsonArray risultato = new JsonArray();
        for (JsonElement el : punti) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            JsonObject nuovoPunto = new JsonObject();
            if (obj.has("timestamp")) nuovoPunto.add("timestamp", obj.get("timestamp"));

            JsonObject prices = obj.has("prices") && obj.get("prices").isJsonObject()
                    ? obj.getAsJsonObject("prices")
                    : null;
            JsonObject nuovePrezzi = new JsonObject();
            if (prices != null && prices.size() > 0) {
                JsonElement valore = prices.entrySet().iterator().next().getValue();
                nuovePrezzi.add(CODICE_FONTE, valore);
            }
            nuovoPunto.add("prices", nuovePrezzi);
            risultato.add(nuovoPunto);
        }
        return risultato;
    }

    private static void registraFallimento() {
        if (FALLIMENTI_CONSECUTIVI.incrementAndGet() >= SOGLIA_INTERRUZIONE_SESSIONE && !sessioneInterrotta) {
            sessioneInterrotta = true;
            System.out.println("Servizio prezzi condiviso (giacenzecrypto.it) non risponde: per il resto di "
                    + "questa sessione i prezzi verranno scaricati come sempre in locale, direttamente dagli exchange.");
        }
    }

    /** Solo per i test: azzera lo stato di sessione (interruttore, fallimenti, simboli non supportati, cache di /v1/monete). */
    static void ReimpostaStatoSessione_PerTest() {
        SIMBOLI_NON_SUPPORTATI.clear();
        FALLIMENTI_CONSECUTIVI.set(0);
        sessioneInterrotta = false;
        moneteEsposteCache = null;
        moneteEsposteTentato = false;
    }
}
