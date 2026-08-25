package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client per il servizio prezzi remoto (cache pull-through condivisa fra tutte le installazioni,
 * vedi nocommit/Documentazione/API_ServizioPrezzi.md e Analisi_VPS_Prezzi_Sito.md). Fase 1,
 * pensata come **puramente additiva**: {@link Prezzi#CambioXXXEUR} lo interroga PRIMA del
 * recupero locale via CCXT ({@link Prezzi#RecuperaPrezziDaCCXT}); se il servizio risponde con un
 * esito autorevole (prezzi trovati o assenza confermata), i punti vengono scritti nella stessa
 * cache locale (tabella {@code PrezziNew}, tramite {@link Prezzi#ScriviPuntiPrezzoInCache}) e il
 * recupero locale viene saltato. In ogni altro caso — servizio irraggiungibile, occupato, moneta
 * fuori dalla lista che copre, interruttore di sessione attivo — si ripiega
 * **silenziosamente** sul meccanismo locale, identico a prima: l'app non deve mai bloccarsi o
 * comportarsi diversamente se la VPS è irraggiungibile.
 *
 * Nessuna lista delle monete coperte è tenuta qui: {@code config/monete.json} vive solo sul
 * server (può cambiare senza redeploy del client) e la risposta {@code 400 "moneta non gestita"}
 * viene semplicemente ricordata per sessione, così da non richiedere in rete due volte lo stesso
 * simbolo non coperto — niente elenco duplicato da tenere allineato a mano.
 */
public class ServizioPrezziClient {

    /**
     * Interruttore temporaneo — non l'interruttore di sessione, quello si riapre da solo dopo un
     * riavvio. Questo resta finché non lo si rimette a {@code false} a mano. Attivato il
     * 2026-08-25 su richiesta esplicita dell'utente: alcuni degli exchange configurati non hanno
     * ancora i termini contrattuali verificati (vedi Analisi_VPS_Prezzi_Sito.md, "Vincoli
     * contrattuali degli exchange sui Market Data") e nel frattempo il client deve tornare a
     * comportarsi esattamente come prima di questa integrazione — nessuna chiamata al servizio
     * remoto, in nessun caso. Sovrascrivibile per i test con la proprietà di sistema
     * {@code prezzi.servizio.abilitato=true}.
     */
    private static final boolean ABILITATO_DI_DEFAULT = false;

    private static boolean abilitato() {
        String override = System.getProperty("prezzi.servizio.abilitato");
        return override != null ? Boolean.parseBoolean(override) : ABILITATO_DI_DEFAULT;
    }

    private static final String URL_BASE = "https://giacenzecrypto.it/v1/";

    /**
     * Nome della risorsa sul classpath che contiene la chiave applicativa anti-abuso del servizio
     * prezzi (non un vero segreto in senso stretto: viaggia comunque dentro il programma
     * distribuito ed è estraibile — vedi Analisi_VPS_Prezzi_Sito.md, "API key applicativa"). Il
     * <b>valore non sta nel codice sorgente</b>: il file {@code src/main/resources/ServizioPrezzi_ApiKey.txt}
     * è deliberatamente ignorato da git (vedi {@code .gitignore}) e va popolato a mano, una riga,
     * solo sulla macchina che produce una build da distribuire — la procedura per recuperare il
     * valore reale è nella documentazione interna del progetto, non pubblica. Senza quel file il
     * servizio remoto resta semplicemente sempre inattivo: nessun malfunzionamento, solo fallback
     * locale su ogni chiamata (vedi {@link #apiKey()}).
     */
    private static final String RISORSA_API_KEY = "/ServizioPrezzi_ApiKey.txt";

    /** Legge la chiave dalla risorsa sul classpath, {@code null} se assente/vuota. */
    private static String chiaveDaRisorsa() {
        try (InputStream in = ServizioPrezziClient.class.getResourceAsStream(RISORSA_API_KEY)) {
            if (in == null) return null;
            String valore = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return valore.isEmpty() ? null : valore;
        } catch (IOException ex) {
            return null;
        }
    }

    /** Sovrascrivibile per sviluppo/test (es. contro un'istanza locale del servizio). */
    private static String apiKey() {
        // Una proprietà impostata ma vuota forza "nessuna chiave", indipendentemente da cosa c'è
        // sul classpath — è quello che serve ai test per simulare "nessuna chiave configurata" a
        // prescindere dal fatto che chi esegue la build abbia già creato in locale il proprio
        // ServizioPrezzi_ApiKey.txt reale (vedi ServizioPrezziClientTest).
        String override = System.getProperty("prezzi.servizio.apikey");
        if (override != null) return override.isEmpty() ? null : override;
        return chiaveDaRisorsa();
    }

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

    private ServizioPrezziClient() {
    }

    /**
     * Prova a risolvere il prezzo di {@code symbol} al tempo {@code timestampMs} tramite il
     * servizio remoto. Se ottiene un esito autorevole (prezzi trovati o assenza confermata),
     * scrive i punti nella cache locale e ritorna {@code true}: il chiamante non deve fare altro.
     * Ritorna {@code false} in ogni altro caso — irraggiungibile, occupato/parziale, moneta non
     * gestita, interruttore di sessione attivo — e il chiamante deve procedere esattamente come
     * se il servizio non esistesse.
     */
    public static boolean tentaRecupero(String symbol, long timestampMs) {
        if (!abilitato()) return false;
        if (sessioneInterrotta) return false;
        String chiave = apiKey();
        // Nessuna chiave configurata (build senza src/main/resources/ServizioPrezzi_ApiKey.txt,
        // es. una build da sorgente non ufficiale): il servizio remoto è sempre e comunque
        // inattivo, niente da guadagnare interrogandolo — zero chiamate di rete inutili.
        if (chiave == null) return false;
        String simbolo = symbol.toUpperCase();
        if (SIMBOLI_NON_SUPPORTATI.contains(simbolo)) return false;

        Request richiesta = new Request.Builder()
                .url(urlBase() + "prezzo?symbol=" + simbolo + "&timestamp=" + timestampMs)
                .header("X-Api-Key", chiave)
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
                // 401 (chiave sbagliata/non ancora impostata), 429 (rate limit), 500: mai
                // un'eccezione visibile, solo un log leggero e il fallback.
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
            // "punti: []" è una risposta valida e definitiva (assenza di prezzo confermata): si
            // scrive comunque (un batch vuoto non fa nulla) e si ritorna true, senza che il
            // chiamante debba distinguere i due casi.
            Prezzi.ScriviPuntiPrezzoInCache(simbolo, punti);
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
                        + " (assenza confermata dal server, non verrà ricercato anche in locale).");
            }
            return true;

        } catch (IOException ex) {
            // Servizio irraggiungibile (rete, DNS, timeout): mai un errore visibile, solo fallback.
            registraFallimento();
            return false;
        }
    }

    private static void registraFallimento() {
        if (FALLIMENTI_CONSECUTIVI.incrementAndGet() >= SOGLIA_INTERRUZIONE_SESSIONE && !sessioneInterrotta) {
            sessioneInterrotta = true;
            System.out.println("Servizio prezzi condiviso (giacenzecrypto.it) non risponde: per il resto di "
                    + "questa sessione i prezzi verranno scaricati come sempre in locale, direttamente dagli exchange.");
        }
    }

    /** Solo per i test: azzera lo stato di sessione (interruttore, fallimenti, simboli non supportati). */
    static void ReimpostaStatoSessione_PerTest() {
        SIMBOLI_NON_SUPPORTATI.clear();
        FALLIMENTI_CONSECUTIVI.set(0);
        sessioneInterrotta = false;
    }
}
