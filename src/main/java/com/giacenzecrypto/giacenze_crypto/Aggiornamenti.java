package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Il controllo delle nuove versioni del programma, sui due canali:
 * <ul>
 * <li><b>stabile</b>: l'ultima release pubblicata del repository GitHub ({@code releases/latest}, che per
 *     definizione esclude prerelease e bozze);</li>
 * <li><b>beta</b>: anche le prerelease, cioè le versioni intermedie a quattro cifre ({@code 1.0.65.07}). Il canale
 *     vede anche le stabili, così chi è sulla {@code 1.0.65.07} riceve la {@code 1.0.66} appena esce.</li>
 * </ul>
 * Le versioni si confrontano con {@link Funzioni#ConfrontaVersioni}, lo stesso comparatore di
 * {@code versioneMinimaApp}: perché una beta venga prima della stabile che prepara, le intermedie si numerano
 * {@code <ultima stabile>.NN} e mai {@code <prossima stabile>.NN}.
 *
 * <p>Logica senza Swing: la finestra è {@link GUI_Aggiornamento}, il pulsante rosso è di {@link Principale}.
 * L'edizione Store si aggiorna dallo Store e non interroga nulla (policy 10.8.3 e 10.2.2).
 * Analisi completa: {@code nocommit/Documentazione/Analisi_Aggiornamenti_Automatici.md}.
 */
public final class Aggiornamenti {

    private Aggiornamenti() {}

    /** Prefisso dei tag delle release: {@code Giacenze_Crypto_<versione>}, lo stesso da cui dipendono AUR e flatpak. */
    static final String PREFISSO_TAG = "Giacenze_Crypto_";

    /** Opzioni personali ({@code personale.mv.db}). Non sono opzioni del motore: fuori da {@code OpzioniRicalcolo.Epoca()}. */
    static final String OPZIONE_CANALE = "Aggiornamenti_Canale";
    static final String OPZIONE_CONTROLLA_ALL_AVVIO = "Aggiornamenti_ControllaAllAvvio";
    static final String OPZIONE_IGNORATA = "Aggiornamenti_VersioneIgnorata";

    private static final int TIMEOUT_MS = 8000;

    /** I due canali di aggiornamento. */
    public enum Canale {
        STABILE, BETA
    }

    /** Un file allegato a una release. {@code Sha256} è vuoto se GitHub non lo fornisce. */
    public record Allegato(String Nome, String Url, String Sha256, long Dimensione) {}

    /** Una release pubblicata, con la versione già ricavata dal tag. */
    public record Rilascio(String Versione, boolean Beta, String Pagina, List<Allegato> Allegati) {}

    /** Esito di un controllo. */
    public enum Stato {
        /** C'è una versione più recente da proporre. */
        DISPONIBILE,
        /** La versione in uso è la più recente del canale (o quella più recente è stata ignorata). */
        AGGIORNATO,
        /** Non si sa: rete assente, limite dell'API, risposta illeggibile. Non equivale ad "aggiornato". */
        SCONOSCIUTO
    }

    /** Risultato del controllo: {@code Rilascio} è valorizzato quando lo stato è {@link Stato#DISPONIBILE}. */
    public record Risultato(Stato Stato, Rilascio Rilascio, String Errore) {}

    private static volatile Risultato UltimoRisultato;
    private static volatile Runnable Osservatore;

    /** @return l'esito dell'ultimo controllo eseguito in questa sessione, o {@code null} se non ce n'è stato uno */
    public static Risultato UltimoRisultato() {
        return UltimoRisultato;
    }

    /**
     * Registra chi deve sapere di ogni nuovo esito (il pulsante rosso di {@link Principale}, che va mostrato o
     * nascosto anche quando il controllo lo rifà la finestra degli aggiornamenti). Viene chiamato sul thread del
     * controllo, non sull'EDT.
     */
    public static void ImpostaOsservatore(Runnable r) {
        Osservatore = r;
    }

    private static volatile Predicate<BooleanSupplier> Chiusura;

    /**
     * Registra la chiusura ordinata del programma (quella della X della finestra principale): l'installazione di un
     * aggiornamento la usa per salvare, fare il backup, avviare lo script e uscire. La funzione riceve l'azione da
     * eseguire prima dell'uscita e restituisce {@code false} se la chiusura è stata annullata.
     */
    public static void ImpostaChiusura(Predicate<BooleanSupplier> c) {
        Chiusura = c;
    }

    /** @return la chiusura registrata, o {@code null} (finestra aperta fuori dal programma completo) */
    static Predicate<BooleanSupplier> Chiusura() {
        return Chiusura;
    }

    /** @return {@code false} per l'edizione Store, che non deve interrogare altre vie di distribuzione */
    public static boolean Abilitati() {
        return !VarStatiche.EdizioneStore();
    }

    //=== OPZIONI ===

    /**
     * @return il canale scelto, o quello predefinito per la versione in uso se l'utente non ha mai scelto. Un beta
     *         dedotto si salva subito: altrimenti, passato alla stabile successiva (tre segmenti), il predefinito
     *         tornerebbe stabile e l'utente non vedrebbe più le intermedie che finora riceveva.
     */
    public static Canale CanaleScelto() {
        String salvato = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CANALE);
        Canale c = CanalePredefinito(VarStatiche.Versione, salvato);
        if ((salvato == null || salvato.isBlank()) && c == Canale.BETA) ScriviCanale(c);
        return c;
    }

    public static void ScriviCanale(Canale c) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CANALE, c.name());
    }

    /**
     * Il canale effettivo dati la versione in uso e l'opzione salvata. Chi non ha mai scelto e sta già usando una
     * versione intermedia (quattro segmenti) è di fatto sul canale beta: lo si lascia lì (e {@link #CanaleScelto}
     * lo salva), altrimenti non vedrebbe più le intermedie successive.
     */
    static Canale CanalePredefinito(String VersioneInUso, String Opzione) {
        if (Opzione != null && !Opzione.isBlank()) {
            try {
                return Canale.valueOf(Opzione.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                //valore non riconosciuto: si ricade sul predefinito
            }
        }
        return isVersioneIntermedia(VersioneInUso) ? Canale.BETA : Canale.STABILE;
    }

    /** @return {@code true} per una versione con più di tre segmenti ({@code 1.0.65.07}) */
    static boolean isVersioneIntermedia(String Versione) {
        return isVersioneValida(Versione) && Versione.trim().split("\\.").length > 3;
    }

    public static boolean ControllaAllAvvio() {
        return !"NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CONTROLLA_ALL_AVVIO));
    }

    public static void ScriviControllaAllAvvio(boolean si) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CONTROLLA_ALL_AVVIO, si ? "SI" : "NO");
    }

    /** Non riproporre più questa versione (ne verrà proposta una successiva). */
    public static void Ignora(String Versione) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_IGNORATA, Versione == null ? "" : Versione);
        Notifica();
    }

    /**
     * @return {@code true} se l'esito va segnalato col pulsante rosso: c'è una versione nuova e non è quella
     *         ignorata (un controllo chiesto dall'utente la mostra comunque nella finestra)
     */
    public static boolean DaSegnalare(Risultato r) {
        return r != null && r.Stato() == Stato.DISPONIBILE
                && !r.Rilascio().Versione().equals(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_IGNORATA, "").trim());
    }

    //=== CONTROLLO ===

    /**
     * Interroga GitHub sul canale scelto e ricorda l'esito. Non lancia eccezioni: ogni problema diventa
     * {@link Stato#SCONOSCIUTO}.
     */
    public static Risultato Controlla() {
        return Controlla(CanaleScelto(), true);
    }

    /**
     * @param Canale canale da interrogare
     * @param RispettaIgnorata {@code false} per un controllo chiesto dall'utente, che mostra anche la versione
     *        ignorata
     */
    public static Risultato Controlla(Canale Canale, boolean RispettaIgnorata) {
        Risultato r;
        if (!Abilitati()) {
            r = new Risultato(Stato.SCONOSCIUTO, null, "Nell'edizione Store gli aggiornamenti arrivano dallo Store.");
        } else if (Funzioni.ReteDisabilitataPerTest) {
            r = new Risultato(Stato.SCONOSCIUTO, null, "Rete disabilitata.");
        } else {
            try {
                List<Rilascio> rilasci = Scarica(Canale);
                String ignorata = RispettaIgnorata ? DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_IGNORATA, "") : "";
                r = Valuta(VarStatiche.Versione, Migliore(rilasci, Canale), ignorata);
            } catch (IOException | JSONException ex) {
                r = new Risultato(Stato.SCONOSCIUTO, null, ex.getMessage());
            }
        }
        System.out.println("Aggiornamenti: canale " + Canale + ", esito " + r.Stato()
                + (r.Rilascio() != null ? " (" + r.Rilascio().Versione() + ")" : "")
                + (r.Errore() != null ? " - " + r.Errore() : ""));
        UltimoRisultato = r;
        Notifica();
        return r;
    }

    private static void Notifica() {
        Runnable o = Osservatore;
        if (o != null) o.run();
    }

    /**
     * Stabile: una sola chiamata a {@code releases/latest}. Beta: l'elenco delle ultime release, prerelease
     * comprese. In entrambi i casi una sola richiesta all'API (60 l'ora per indirizzo, senza chiave).
     */
    private static List<Rilascio> Scarica(Canale Canale) throws IOException {
        String base = "https://api.github.com/repos/" + Funzioni.REPO_GITHUB + "/releases";
        List<Rilascio> rilasci = new ArrayList<>();
        if (Canale == Aggiornamenti.Canale.STABILE) {
            Rilascio r = LeggiRilascio(new JSONObject(Get(base + "/latest")));
            if (r != null) rilasci.add(r);
        } else {
            rilasci.addAll(LeggiRilasci(new JSONArray(Get(base + "?per_page=20"))));
        }
        return rilasci;
    }

    private static String Get(String Url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(Url).toURL().openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "GiacenzeCrypto/" + VarStatiche.Versione);
            int codice = conn.getResponseCode();
            //403/429 = limite di richieste superato, 404 = nessuna release: in ogni caso "non so"
            if (codice != HttpURLConnection.HTTP_OK) throw new IOException("GitHub ha risposto " + codice);
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }

    //=== PARTE PURA (testata senza rete) ===

    /** @return le release valide dell'elenco restituito da {@code /releases}, bozze escluse */
    static List<Rilascio> LeggiRilasci(JSONArray Elenco) {
        List<Rilascio> rilasci = new ArrayList<>();
        for (int i = 0; i < Elenco.length(); i++) {
            JSONObject o = Elenco.optJSONObject(i);
            Rilascio r = o == null ? null : LeggiRilascio(o);
            if (r != null) rilasci.add(r);
        }
        return rilasci;
    }

    /**
     * @return la release, o {@code null} se è una bozza o se il tag non è {@code Giacenze_Crypto_<versione numerica>}
     */
    static Rilascio LeggiRilascio(JSONObject o) {
        if (o.optBoolean("draft", false)) return null;
        String tag = o.optString("tag_name", "");
        if (!tag.startsWith(PREFISSO_TAG)) return null;
        String versione = tag.substring(PREFISSO_TAG.length()).trim();
        if (!versione.matches("\\d+(\\.\\d+)*")) return null;
        List<Allegato> allegati = new ArrayList<>();
        JSONArray a = o.optJSONArray("assets");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject f = a.optJSONObject(i);
                if (f == null) continue;
                String digest = f.optString("digest", "");
                String sha = digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : "";
                allegati.add(new Allegato(f.optString("name", ""), f.optString("browser_download_url", ""),
                        sha, f.optLong("size", 0)));
            }
        }
        return new Rilascio(versione, o.optBoolean("prerelease", false), o.optString("html_url", ""), allegati);
    }

    /**
     * @return la release più recente fra quelle che il canale vede (lo stabile scarta le prerelease), o {@code null}
     */
    static Rilascio Migliore(List<Rilascio> Rilasci, Canale Canale) {
        Rilascio migliore = null;
        for (Rilascio r : Rilasci) {
            if (Canale == Aggiornamenti.Canale.STABILE && r.Beta()) continue;
            if (migliore == null || Funzioni.ConfrontaVersioni(r.Versione(), migliore.Versione()) > 0) migliore = r;
        }
        return migliore;
    }

    /**
     * Decide se proporre {@code Candidato}. Una versione in uso non interpretabile ({@code "sconosciuta"}, build
     * senza il filtering Maven) non propone nulla: non si sa da dove si parte.
     */
    static Risultato Valuta(String VersioneInUso, Rilascio Candidato, String Ignorata) {
        if (!isVersioneValida(VersioneInUso)) {
            return new Risultato(Stato.SCONOSCIUTO, null, "Versione in uso non riconosciuta: " + VersioneInUso);
        }
        if (Candidato == null) return new Risultato(Stato.SCONOSCIUTO, null, "Nessuna release trovata.");
        if (Funzioni.ConfrontaVersioni(Candidato.Versione(), VersioneInUso) <= 0) {
            return new Risultato(Stato.AGGIORNATO, null, null);
        }
        if (Ignorata != null && Candidato.Versione().equals(Ignorata.trim())) {
            return new Risultato(Stato.AGGIORNATO, null, null);
        }
        return new Risultato(Stato.DISPONIBILE, Candidato, null);
    }

    static boolean isVersioneValida(String Versione) {
        return Versione != null && Versione.trim().matches("\\d+(\\.\\d+)*");
    }
}
