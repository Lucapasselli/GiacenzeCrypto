package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client per l'invio delle segnalazioni utente al servizio remoto (endpoint pubblici
 * {@code /segnalazioni/*} su {@code giacenzecrypto.it} — vedi
 * nocommit/Documentazione/Analisi_Segnalazioni_Log.md e API_ServizioPrezzi.md).
 *
 * <p>A differenza di {@link ServizioPrezziClient} <b>non ha interruttore di sessione</b>: qui
 * l'invio &egrave; un'azione manuale e rara (un click dell'utente, con anteprima e conferma),
 * quindi disabilitarla dopo un fallimento transitorio sarebbe solo dannoso — si ritorna un esito
 * chiaro e l'utente ritenta.
 *
 * <p>Due forme:
 * <ul>
 *   <li>{@link #inviaErroreImport} — JSON piccolo: le righe dei movimenti sconosciuti + il
 *       descrittore dell'importazione che le ha prodotte;</li>
 *   <li>{@link #inviaLog} — corpo grezzo = il {@code .gz} del log (documento di testo unico gi&agrave;
 *       redatto lato client), i metadati in un header {@code X-Segnalazione-Meta} base64.</li>
 * </ul>
 */
public final class SegnalazioniClient {

    private static final String URL_BASE = "https://giacenzecrypto.it/";

    /** Sovrascrivibile per sviluppo/test (es. {@code http://127.0.0.1:4173/}). */
    private static String urlBase() {
        return System.getProperty("segnalazioni.servizio.urlbase", URL_BASE);
    }

    // Scrittura pi&ugrave; larga della lettura: l'upload del .gz pu&ograve; essere di qualche MB.
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType GZIP = MediaType.get("application/gzip");

    private SegnalazioniClient() {
    }

    /** Esito di un invio, gi&agrave; tradotto in un messaggio mostrabile all'utente. */
    public record Esito(boolean ok, int codice, String messaggio) {
    }

    /**
     * Contesto dell'importazione che ha generato una segnalazione di errore: serve a capire, dai
     * soli righi d'errore, a quale import si riferiscono.
     */
    public record DescrittoreImport(String tipoImport, String file,
            int totali, int aggiunte, int scartate, int sconosciute) {

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            if (tipoImport != null && !tipoImport.isBlank()) {
                o.addProperty("tipoImport", tipoImport);
            }
            if (file != null && !file.isBlank()) {
                o.addProperty("file", file);
            }
            JsonObject c = new JsonObject();
            c.addProperty("totali", totali);
            c.addProperty("aggiunte", aggiunte);
            c.addProperty("scartate", scartate);
            c.addProperty("sconosciute", sconosciute);
            o.add("conteggi", c);
            return o;
        }
    }

    /** @return una stringa {@code "Nome versione (arch)"} del sistema operativo. */
    private static String infoOs() {
        return System.getProperty("os.name", "?") + " "
                + System.getProperty("os.version", "?") + " ("
                + System.getProperty("os.arch", "?") + ")";
    }

    private static void aggiungiInfoComuni(JsonObject o) {
        o.addProperty("versioneApp", VarStatiche.Versione);
        o.addProperty("os", infoOs());
        o.addProperty("java", System.getProperty("java.version", "?"));
    }

    /**
     * Invia una segnalazione di errore di importazione.
     * @param corpo le righe dei movimenti sconosciuti (gi&agrave; visibili nel pannello all'utente)
     * @param d contesto dell'import, pu&ograve; essere {@code null}
     */
    public static Esito inviaErroreImport(String corpo, DescrittoreImport d) {
        JsonObject body = new JsonObject();
        body.addProperty("corpo", corpo == null ? "" : corpo);
        aggiungiInfoComuni(body);
        if (d != null) {
            body.add("descrittore", d.toJson());
        }
        Request req = new Request.Builder()
                .url(urlBase() + "segnalazioni/errore-import")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        return esegui(req, Contesto.ERRORE_IMPORT);
    }

    /**
     * Invia un log gi&agrave; compresso.
     * @param gz byte del {@code .gz} (documento di testo unico gi&agrave; redatto e compresso)
     * @param modalita {@code "bundle"} o {@code "log-completo"} per un vero invio di log,
     *        {@code "errore-import-grande"} quando questo metodo è usato per una segnalazione di
     *        errore import troppo grande per il JSON diretto (vedi {@link Importazioni_Resoconto})
     *        — solo informativo lato server, finisce nei metadati, ma qui sceglie anche il testo
     *        del messaggio in caso di errore "troppo-grande"
     * @param d contesto dell'import se l'invio parte dal resoconto di importazione, altrimenti {@code null}
     */
    public static Esito inviaLog(byte[] gz, String modalita, DescrittoreImport d) {
        JsonObject meta = new JsonObject();
        aggiungiInfoComuni(meta);
        if (modalita != null) {
            meta.addProperty("modalita", modalita);
        }
        if (d != null) {
            meta.add("descrittore", d.toJson());
        }
        String header = Base64.getEncoder()
                .encodeToString(meta.toString().getBytes(StandardCharsets.UTF_8));
        Request req = new Request.Builder()
                .url(urlBase() + "segnalazioni/log")
                .addHeader("X-Segnalazione-Meta", header)
                .post(RequestBody.create(gz, GZIP))
                .build();
        Contesto ctx = "errore-import-grande".equals(modalita) ? Contesto.ERRORE_IMPORT_GRANDE : Contesto.LOG;
        return esegui(req, ctx);
    }

    /**
     * Distingue, solo ai fini del messaggio mostrato all'utente in caso di errore "troppo-grande",
     * fra i tre punti da cui {@link #esegui} può essere chiamato — lo stesso {@code esito} del
     * server ("troppo-grande") ha un rimedio diverso a seconda di chi ha inviato la richiesta:
     * {@link #inviaErroreImport} (JSON diretto, limite 256 KB), un vero {@link #inviaLog} di log
     * (dove "usa il bundle diagnostico" è un consiglio sensato, essendo l'altra modalità offerta
     * dalla stessa maschera), o {@link #inviaLog} riusato per un errore import troppo grande anche
     * compresso (dove "bundle diagnostico" non esiste e non significherebbe nulla).
     */
    private enum Contesto { ERRORE_IMPORT, ERRORE_IMPORT_GRANDE, LOG }

    private static Esito esegui(Request req, Contesto ctx) {
        try (Response r = HTTP.newCall(req).execute()) {
            String corpo = r.body() != null ? r.body().string() : "";
            String esito = "";
            try {
                var root = JsonParser.parseString(corpo);
                if (root.isJsonObject() && root.getAsJsonObject().has("esito")) {
                    esito = root.getAsJsonObject().get("esito").getAsString();
                }
            } catch (RuntimeException ignora) {
                // corpo non JSON: si ripiega sul solo codice HTTP
            }
            return new Esito(r.isSuccessful(), r.code(), messaggioUtente(r.code(), esito, ctx));
        } catch (IOException ex) {
            return new Esito(false, 0,
                    "Servizio non raggiungibile. Controlla la connessione e riprova. (" + ex.getMessage() + ")");
        }
    }

    private static String messaggioUtente(int codice, String esito, Contesto ctx) {
        if ("troppo-grande".equals(esito)) {
            return switch (ctx) {
                case LOG ->
                    "Il file supera i 10 MB compressi: usa il \"bundle diagnostico\" o riduci i log allegati.";
                case ERRORE_IMPORT_GRANDE ->
                    "L'elenco delle transazioni scartate supera i 10 MB anche compresso: "
                    + "è un caso eccezionale, contatta l'autore in altro modo (es. allegando un estratto).";
                case ERRORE_IMPORT ->
                    "Il testo è troppo lungo per essere inviato in un'unica segnalazione: riprova, "
                    + "verrà inviato automaticamente in forma compressa.";
            };
        }
        return switch (esito) {
            case "ok" ->
                "Segnalazione inviata. Grazie!";
            case "troppe-richieste" ->
                "Troppi invii ravvicinati: attendi un minuto e riprova.";
            case "quota-giornaliera" ->
                "Raggiunto il numero massimo di invii giornalieri da questo indirizzo. Riprova domani.";
            case "payload-non-valido" ->
                "Il contenuto inviato non è un archivio valido.";
            case "corpo-mancante" ->
                "Non c'è nulla da inviare.";
            default ->
                (codice >= 200 && codice < 300)
                        ? "Segnalazione inviata."
                        : "Il servizio ha risposto con un errore (codice " + codice + "). Riprova più tardi.";
        };
    }
}
