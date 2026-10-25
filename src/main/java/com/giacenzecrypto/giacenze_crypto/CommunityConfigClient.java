package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client HTTP del catalogo delle configurazioni condivise ({@code /community/*} su giacenzecrypto.it, vedi
 * {@code ServizioPrezzi/routes/community.js}). Solo trasporto: i controlli sui file stanno in
 * {@link ValidazioneConfigCommunity}, il salvataggio in {@link CommunityConfigurazioni}.
 */
public final class CommunityConfigClient {

    private static final String URL_BASE = "https://giacenzecrypto.it/";

    /** Sovrascrivibile per sviluppo e test (es. {@code http://127.0.0.1:4173/}) */
    private static String urlBase() {
        return System.getProperty("community.servizio.urlbase", URL_BASE);
    }

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** Un file scaricato non puo' essere piu' grande di questo: il server ne accetta al massimo 64 KB */
    private static final int MAX_SCARICAMENTO = 2 * ValidazioneConfigCommunity.MAX_BYTE;

    private CommunityConfigClient() {
    }

    /** Esito di un invio: in caso di successo l'id della proposta e il codice per ritirarla */
    public record EsitoInvio(boolean ok, int id, String codiceRitiro, String messaggio, List<String> errori) {
    }

    /** Una configurazione pubblicata nel catalogo */
    public record VoceCatalogo(int id, String nomeFile, String fornitore, String estrazione, String nomeExchange,
            String descrizione, String versioneMinimaApp, String autore, long pubblicata, String sha256) {
    }

    /**
     * Propone una configurazione. Diventa scaricabile solo dopo l'approvazione dell'autore del programma.
     * @param testo il JSON, inviato cosi' com'e' (lo SHA-256 del catalogo sara' su questi byte)
     * @param riepilogo l'esito della prova, senza righe del CSV
     */
    public static EsitoInvio Invia(String testo, String nomeFile, String autore, JsonObject riepilogo) {
        JsonObject corpo = new JsonObject();
        corpo.addProperty("configurazione", testo);
        corpo.addProperty("nomeFile", nomeFile);
        if (autore != null && !autore.isBlank()) corpo.addProperty("autore", autore.trim());
        corpo.addProperty("versioneApp", VarStatiche.Versione);
        if (riepilogo != null) corpo.add("riepilogo", riepilogo);
        Request req = new Request.Builder().url(urlBase() + "community/proposte")
                .post(RequestBody.create(corpo.toString(), JSON)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            JsonObject j = LeggiOggetto(r.body());
            String esito = Testo(j, "esito");
            List<String> errori = new ArrayList<>();
            if (j.has("errori") && j.get("errori").isJsonArray()) {
                for (JsonElement e : j.getAsJsonArray("errori")) errori.add(e.getAsString());
            }
            if (r.code() == 201 && "ok".equals(esito)) {
                return new EsitoInvio(true, j.get("id").getAsInt(), Testo(j, "codiceRitiro"),
                        "Configurazione inviata. Sara' pubblicata dopo il controllo dell'autore del programma.", errori);
            }
            return new EsitoInvio(false, j.has("id") ? j.get("id").getAsInt() : 0, null,
                    MessaggioErrore(r.code(), esito, j), errori);
        } catch (IOException | RuntimeException ex) {
            return new EsitoInvio(false, 0, null,
                    "Servizio non raggiungibile. Controlla la connessione e riprova. (" + ex.getMessage() + ")", List.of());
        }
    }

    private static String MessaggioErrore(int codice, String esito, JsonObject j) {
        return switch (esito) {
            case "non-valida" -> "Il servizio ha rifiutato la configurazione:";
            case "gia-presente" -> "Questa configurazione e' gia' stata proposta (n. " + Testo(j, "id") + ", "
                    + ("pubblicata".equals(Testo(j, "stato")) ? "pubblicata" : "in attesa di controllo") + ").";
            case "quota-giornaliera" -> "Raggiunto il numero massimo di invii giornalieri. Riprova domani.";
            case "troppe-in-attesa" -> "Ci sono gia' troppe tue proposte in attesa di controllo: riprova quando saranno state esaminate.";
            case "coda-piena" -> "In questo momento le proposte in attesa sono troppe. Riprova fra qualche giorno.";
            case "troppe-richieste" -> "Troppi invii ravvicinati: attendi un minuto e riprova.";
            default -> "Il servizio ha risposto con un errore (codice " + codice + "). Riprova piu' tardi.";
        };
    }

    /** @return le configurazioni pubblicate */
    public static List<VoceCatalogo> Catalogo() throws IOException {
        Request req = new Request.Builder().url(urlBase() + "community/catalogo").get().build();
        try (Response r = HTTP.newCall(req).execute()) {
            if (!r.isSuccessful()) throw new IOException("catalogo non disponibile (codice " + r.code() + ")");
            JsonObject j = LeggiOggetto(r.body());
            List<VoceCatalogo> voci = new ArrayList<>();
            JsonArray a = j.has("configurazioni") && j.get("configurazioni").isJsonArray()
                    ? j.getAsJsonArray("configurazioni") : new JsonArray();
            for (JsonElement e : a) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                voci.add(new VoceCatalogo(o.get("id").getAsInt(), Testo(o, "nomeFile"), Testo(o, "fornitore"),
                        Testo(o, "estrazione"), Testo(o, "nomeExchange"), Testo(o, "descrizione"),
                        Testo(o, "versioneMinimaApp"), Testo(o, "autore"),
                        o.has("pubblicata") && !o.get("pubblicata").isJsonNull() ? o.get("pubblicata").getAsLong() : 0,
                        Testo(o, "sha256")));
            }
            return voci;
        } catch (RuntimeException ex) {
            throw new IOException("catalogo illeggibile: " + ex.getMessage(), ex);
        }
    }

    /** @return i byte della configurazione pubblicata, esattamente come sul server */
    public static byte[] Scarica(int id) throws IOException {
        Request req = new Request.Builder().url(urlBase() + "community/configurazioni/" + id).get().build();
        try (Response r = HTTP.newCall(req).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new IOException("configurazione non disponibile (codice " + r.code() + ")");
            //Lettura limitata: un file piu' grande del previsto non entra in memoria per intero
            byte[] dati = r.body().byteStream().readNBytes(MAX_SCARICAMENTO + 1);
            if (dati.length > MAX_SCARICAMENTO) throw new IOException("configurazione troppo grande");
            return dati;
        }
    }

    /** Ritira una propria proposta. @return vero se il servizio l'ha ritirata */
    public static boolean Ritira(int id, String codice) throws IOException {
        JsonObject corpo = new JsonObject();
        corpo.addProperty("codice", codice);
        Request req = new Request.Builder().url(urlBase() + "community/proposte/" + id + "/ritira")
                .post(RequestBody.create(corpo.toString(), JSON)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    private static JsonObject LeggiOggetto(ResponseBody corpo) throws IOException {
        String testo = corpo == null ? "" : corpo.string();
        try {
            JsonElement e = JsonParser.parseString(testo);
            return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ex) {
            return new JsonObject();
        }
    }

    private static String Testo(JsonObject o, String chiave) {
        return o.has(chiave) && !o.get(chiave).isJsonNull() ? o.get(chiave).getAsString() : "";
    }
}
