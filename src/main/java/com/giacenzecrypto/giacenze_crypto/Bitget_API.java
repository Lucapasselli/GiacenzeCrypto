package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Importazione dei movimenti Bitget tramite l'<b>API fiscale</b> ({@code /api/v2/tax/*}), quella che
 * Bitget mette a disposizione con il permesso "Tax" in sola lettura ({@code taxr}).
 *
 * <p><b>Ogni record diventa una riga uguale a quella dell'export CSV</b> ({@code order, Date, Coin, Type,
 * Amount, Fee, Available}) e passa dalla stessa configurazione {@code config/import/Bitget CSV.json} e dalla
 * stessa pipeline ({@link ImportazioneGenerica#importaRighe}): causali, consolidamento degli interessi per
 * giorno, scambi, commissioni e deduplica sono quelli del CSV. Verificato su tre export reali (56.507
 * righe): l'id del record e' la colonna {@code order} del CSV, importo e fee coincidono. Cambiano solo
 * alcuni nomi di tipo, che {@code causaliAPI} della configurazione riporta all'etichetta del CSV.
 *
 * <p>Fatti dell'API che il codice da' per assodati (verificati il 17/10/2026):
 * <ul>
 *   <li>la finestra {@code startTime}-{@code endTime} non puo' superare 30 giorni;</li>
 *   <li>i record arrivano in ordine <b>crescente</b> di tempo, al piu' 500 per pagina, e {@code idLessThan}
 *       non ha effetto: si pagina spostando {@code startTime} sull'ultimo istante ricevuto, togliendo i
 *       doppioni per id;</li>
 *   <li>oltre qualche richiesta al secondo risponde 429: si rallenta e si riprova;</li>
 *   <li>la passphrase serve solo alle chiavi create con una passphrase: e' facoltativa.</li>
 * </ul>
 *
 * <p><b>Cosa l'API non contiene</b>: gli scambi interni dei trading bot (nessun endpoint Tax li espone).
 * Come nel CSV il giro fondi verso il bot e' IGNORA, quindi la giacenza conta i fondi del bot al valore di
 * ingresso. Conti futures, margin e P2P vengono solo contati e segnalati nel resoconto, non importati.
 */
final class Bitget_API {

    private Bitget_API() { }

    /** Nome dell'exchange nella finestra Exchange API e campo [3] dei movimenti (il {@code nomeExchange} della configurazione). */
    static final String NOME_EXCHANGE = "Bitget";
    /** Configurazione dell'import CSV, riusata per intero. */
    static final String FILE_CONFIGURAZIONE = "Bitget CSV.json";
    /** Permesso della chiave che apre gli endpoint {@code /api/v2/tax/*}. */
    static final String PERMESSO_TAX = "taxr";

    //Endpoint firmato con la chiave dell'utente: resta nel codice come gli altri che portano una firma
    private static final String BASE = "https://api.bitget.com";
    private static final long FINESTRA_MS = 30L * 24 * 60 * 60 * 1000 - 1000;
    private static final int LIMITE_PAGINA = 500;
    private static final long PAUSA_MS = 350;
    private static final int TENTATIVI_429 = 10;
    /**
     * Lo scaricamento si ferma un minuto prima di adesso: il prossimo riparte dall'ultimo movimento piu' un
     * secondo, e un record dello stesso secondo arrivato dopo la richiesta andrebbe perso.
     */
    private static final long MARGINE_FINE_MS = 60_000;

    /** Conti che vengono solo contati: nessuna configurazione li descrive. */
    private static final String[][] CONTI_SOLO_CONTATI = {
        {"future-record", "futures"}, {"margin-record", "margin"}, {"p2p-record", "P2P"}};

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    static boolean isBitget(String exchangeId) {
        return exchangeId != null && exchangeId.trim().equalsIgnoreCase(NOME_EXCHANGE);
    }

    /** Risposta di errore dell'API (codice diverso da {@code 00000}), con il messaggio di Bitget. */
    static final class ErroreAPI extends Exception {
        ErroreAPI(String messaggio) {
            super(messaggio);
        }
    }

    /** Dati dell'account letti da {@code /api/v2/spot/account/info}, l'unico endpoint non Tax aperto a una chiave {@code taxr}. */
    record Account(long Registrazione, List<String> Permessi) {
        boolean HaPermessoTax() {
            return Permessi.contains(PERMESSO_TAX);
        }
    }

    //=====================================================================================================
    //=== FIRMA E CHIAMATE
    //=====================================================================================================

    /** Firma Bitget: base64 di HMAC-SHA256 su {@code timestamp + METODO + percorso?query + corpo}. */
    static String Firma(String Secret, String Timestamp, String Metodo, String PercorsoEQuery, String Corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal((Timestamp + Metodo + PercorsoEQuery + Corpo).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Query string nell'ordine dei parametri: la firma va calcolata sulla stessa stringa che parte. */
    static String Query(Map<String, String> Parametri) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : Parametri.entrySet()) {
            sb.append(sb.length() == 0 ? "?" : "&")
              .append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * GET firmata. Riprova sui 429; una caduta di linea passa da {@link AttesaConnessione}. La risposta entra
     * nel documento di origine dello scaricamento: le credenziali stanno negli header, non nell'URL, quindi
     * percorso e query si possono registrare cosi' come sono.
     */
    static JSONObject Chiama(String ApiKey, String Secret, String Passphrase, String Percorso,
            Map<String, String> Parametri) throws IOException, ErroreAPI {
        if (Funzioni.ReteDisabilitataPerTest) throw new IOException("rete disabilitata per i test");
        String pq = Percorso + Query(Parametri);
        for (int tentativo = 1; ; tentativo++) {
            String ts = String.valueOf(System.currentTimeMillis());
            Request req = new Request.Builder().url(BASE + pq).get()
                    .header("ACCESS-KEY", ApiKey)
                    .header("ACCESS-SIGN", Firma(Secret, ts, "GET", pq, ""))
                    .header("ACCESS-TIMESTAMP", ts)
                    .header("ACCESS-PASSPHRASE", Passphrase == null ? "" : Passphrase)
                    .header("locale", "en-US")
                    .header("Content-Type", "application/json")
                    .build();
            int codice;
            String corpo;
            try (Response r = httpClient.newCall(req).execute()) {
                codice = r.code();
                corpo = r.body() != null ? r.body().string() : "";
            } catch (IOException e) {
                if (AttesaConnessione.Attendi()) continue;
                throw e;
            }
            if (codice == 429 && tentativo < TENTATIVI_429) {
                Dormi(2000L * tentativo);
                continue;
            }
            DocumentiFonte.AggiungiAllaSessione(Importazioni.DocumentoFonteCorrente, "bitget" + Percorso,
                    Query(Parametri), corpo);
            JSONObject j;
            try {
                j = new JSONObject(corpo);
            } catch (Exception e) {
                throw new ErroreAPI("risposta non valida (HTTP " + codice + ")");
            }
            if (!"00000".equals(j.optString("code"))) {
                throw new ErroreAPI(j.optString("msg", "errore") + " (codice " + j.optString("code") + ")");
            }
            return j;
        }
    }

    private static void Dormi(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static Account LeggiAccount(String ApiKey, String Secret, String Passphrase) throws IOException, ErroreAPI {
        JSONObject d = Chiama(ApiKey, Secret, Passphrase, "/api/v2/spot/account/info", new LinkedHashMap<>())
                .getJSONObject("data");
        List<String> permessi = new ArrayList<>();
        JSONArray a = d.optJSONArray("authorities");
        if (a != null) for (int i = 0; i < a.length(); i++) permessi.add(a.getString(i));
        return new Account(d.optLong("regisTime", 0), permessi);
    }

    /** @return {@code true} se le credenziali sono valide e la chiave ha il permesso Tax. */
    static boolean ChiaveValida(String ApiKey, String Secret, String Passphrase) {
        try {
            return LeggiAccount(ApiKey, Secret, Passphrase).HaPermessoTax();
        } catch (Exception e) {
            System.err.println("Bitget, verifica della chiave non riuscita: " + e.getMessage());
            return false;
        }
    }

    private static boolean Interrotto(Download progress) {
        return Principale.InterrompiCiclo || Interruzione.Richiesta() || (progress != null && progress.FineThread());
    }

    /**
     * Tutti i record di un conto fra {@code Da} e {@code A}, a finestre di 30 giorni, senza doppioni.
     * @param Tipo endpoint sotto {@code /api/v2/tax/}, es. {@code spot-record}
     * @return i record in ordine di tempo, {@code null} se l'utente ha interrotto
     */
    static List<JSONObject> ScaricaRecord(String ApiKey, String Secret, String Passphrase, String Tipo,
            long Da, long A, Download progress, String Etichetta) throws IOException, ErroreAPI {
        Map<String, JSONObject> perId = new LinkedHashMap<>();
        for (long t = Da; t < A; t += FINESTRA_MS + 1) {
            long fine = Math.min(t + FINESTRA_MS, A);
            if (progress != null) {
                progress.SetMessaggioAvanzamento(Etichetta + ": " + FunzioniDate.ConvertiDatadaLongAlSecondo(t).substring(0, 10)
                        + " (" + perId.size() + " movimenti)");
            }
            long inizio = t;
            while (true) {
                if (Interrotto(progress)) return null;
                Map<String, String> p = new LinkedHashMap<>();
                p.put("startTime", String.valueOf(inizio));
                p.put("endTime", String.valueOf(fine));
                p.put("limit", String.valueOf(LIMITE_PAGINA));
                JSONArray pagina = Chiama(ApiKey, Secret, Passphrase, "/api/v2/tax/" + Tipo, p).optJSONArray("data");
                Dormi(PAUSA_MS);
                if (pagina == null) break;
                int nuovi = 0;
                long ultimo = inizio;
                for (int i = 0; i < pagina.length(); i++) {
                    JSONObject r = pagina.getJSONObject(i);
                    if (perId.putIfAbsent(r.getString("id"), r) == null) nuovi++;
                    ultimo = Math.max(ultimo, r.optLong("ts", ultimo));
                }
                if (pagina.length() < LIMITE_PAGINA) break;
                //Pagina piena: si riparte dall'ultimo istante ricevuto (puo' avere altri record dopo di se'),
                //oppure dal millisecondo dopo se la pagina non ha portato nulla di nuovo
                inizio = nuovi > 0 ? ultimo : ultimo + 1;
            }
        }
        List<JSONObject> out = new ArrayList<>(perId.values());
        out.sort((x, y) -> {
            int c = Long.compare(x.optLong("ts"), y.optLong("ts"));
            return c != 0 ? c : x.optString("id").compareTo(y.optString("id"));
        });
        return out;
    }

    //=====================================================================================================
    //=== CONVERSIONE IN RIGHE DEL CSV
    //=====================================================================================================

    /** Etichetta mostrata per un record con il tipo vuoto che non e' una coppia LaunchX. */
    static final String TIPO_VUOTO = "(tipo vuoto)";

    /**
     * Record di {@code tax/spot-record} -> righe dell'export CSV: {@code order, Date (UTC), Coin, Type,
     * Amount, Fee, Available}.
     *
     * <p>Il tipo passa da {@code causaliAPI}. La coppia LaunchX arriva con il tipo <b>vuoto</b> (BGB impegnato
     * e token ricevuto, stesso {@code bizOrderId}): la si ricostruisce dal segno solo quando ci sono entrambe
     * le gambe, ogni altro tipo vuoto resta riconoscibile come tale e finisce fra le sconosciute.
     */
    static List<String[]> InRighe(List<JSONObject> Record, Map<String, String> CausaliAPI) {
        Map<String, int[]> segniVuoti = new HashMap<>();   //bizOrderId -> {uscite, entrate}
        for (JSONObject r : Record) {
            if (!r.optString("spotTaxType").isBlank()) continue;
            int[] s = segniVuoti.computeIfAbsent(r.optString("bizOrderId"), k -> new int[2]);
            s[Funzioni.isNegativo(r.optString("amount")) ? 0 : 1]++;
        }
        List<String[]> righe = new ArrayList<>();
        for (JSONObject r : Record) {
            String tipo = r.optString("spotTaxType").trim();
            if (tipo.isEmpty()) {
                int[] s = segniVuoti.get(r.optString("bizOrderId"));
                boolean coppia = !r.optString("bizOrderId").isBlank() && s != null && s[0] > 0 && s[1] > 0;
                tipo = !coppia ? TIPO_VUOTO
                        : Funzioni.isNegativo(r.optString("amount")) ? "LaunchX expense" : "LaunchX income";
            } else if (CausaliAPI.containsKey(tipo)) {
                tipo = CausaliAPI.get(tipo);
            }
            righe.add(new String[]{
                r.optString("id"),
                FORMATO_DATA.format(Instant.ofEpochMilli(r.optLong("ts"))),
                r.optString("coin"),
                tipo,
                r.optString("amount"),
                r.optString("fee", "0").isBlank() ? "0" : r.optString("fee", "0"),
                r.optString("balance")});
        }
        return righe;
    }

    /** Tipi delle righe che la configurazione non conosce, con il numero di righe. */
    static Map<String, Integer> TipiSconosciuti(List<String[]> Righe, ImportazioneGenerica.ConfigurazioneImport cfg) {
        Map<String, Integer> out = new TreeMap<>();
        for (String[] r : Righe) {
            if (!cfg.mappaCausali.containsKey(r[3])) out.merge(r[3], 1, Integer::sum);
        }
        return out;
    }

    //=====================================================================================================
    //=== MOVIMENTI GIA' IN ARCHIVIO
    //=====================================================================================================

    /**
     * Movimenti Bitget gia' in archivio che non vengono da uno scaricamento via API (documento di origine
     * CSV, o nessun documento).
     *
     * <p>Contano perche' l'export CSV scrive gli orari con uno scostamento fisso diverso da export a export
     * (UTC+1, UTC+2, verificato confrontando gli id) mentre la configurazione li legge come UTC: gli stessi
     * movimenti scaricati via API cadono un'ora o due piu' in la' e la deduplica, che confronta l'istante al
     * secondo, non li riconosce.
     */
    static int MovimentiNonDaAPI() {
        Map<Integer, Boolean> daAPI = new HashMap<>();
        int n = 0;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!isBitget(v[3])) continue;
            int id = DocumentiFonte.IdDaCampo41(v.length > 41 ? v[41] : null);
            boolean api = id > 0 && daAPI.computeIfAbsent(id, k -> {
                DocumentiFonte.Documento d = DocumentiFonte.Leggi(k);
                return d != null && DocumentiFonte.TIPO_NDJSON.equals(d.Tipo);
            });
            if (!api) n++;
        }
        return n;
    }

    /**
     * Avvisa, prima di scaricare, se l'archivio ha gia' movimenti Bitget non scaricati via API.
     * @return {@code true} per procedere
     */
    static boolean ConfermaConMovimentiNonDaAPI(Component c) {
        int n = MovimentiNonDaAPI();
        if (n == 0) return true;
        Object[] scelte = {"Scarica comunque", "Annulla"};
        int r = JOptionPane.showOptionDialog(c,
                "<html><div style='width:420px'>In archivio ci sono gia' <b>" + n + "</b> movimenti Bitget che non vengono"
                + " da uno scaricamento via API (importati da CSV o inseriti a mano).<br><br>"
                + "L'export CSV di Bitget scrive gli orari con uno scostamento fisso che cambia da un export all'altro"
                + " (un'ora o due rispetto all'ora UTC), mentre l'API da' l'orario esatto: gli stessi movimenti"
                + " scaricati via API cadrebbero in un orario diverso e non verrebbero riconosciuti come gia' presenti,"
                + " cioe' entrerebbero due volte.<br><br>"
                + "Per avere tutto lo storico dall'API vanno prima eliminati i movimenti Bitget importati da CSV"
                + " (dalla finestra dei documenti di origine). Lo scaricamento parte dall'ultimo movimento Bitget in archivio.</div></html>",
                "Movimenti Bitget gia' presenti", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                scelte, scelte[1]);
        return r == 0;
    }

    //=====================================================================================================
    //=== IMPORTAZIONE
    //=====================================================================================================

    /**
     * Scarica e importa i movimenti spot dal {@code DataInizio} a un minuto fa.
     * Va chiamata dentro la sessione di documento aperta da {@link CcxtInterop#fetchMovimenti}.
     *
     * @return l'esito da mostrare nel resoconto, {@code null} se non si e' importato nulla (errore o
     *         rinuncia gia' segnalati all'utente)
     */
    static Importazioni.Esito Importa(String ApiKey, String Secret, String Passphrase, long DataInizio,
            Download progress, Component c) {
        ImportazioneGenerica.ConfigurazioneImport cfg;
        try {
            cfg = ImportazioneGenerica.ConfigurazioneImport.carica(VarStatiche.getCartella_ConfigImport() + FILE_CONFIGURAZIONE);
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
            Avviso(c, "Configurazione " + FILE_CONFIGURAZIONE + " non disponibile: importazione non eseguita.");
            return null;
        }
        //Senza causaliAPI (configurazione installata precedente alla 1.005) i codici interni dell'API, interessi
        //compresi, sarebbero tutti sconosciuti: importare senza di loro li perderebbe per sempre, perche' il
        //prossimo scaricamento riparte dall'ultimo movimento importato
        if (cfg.causaliAPI.isEmpty()) {
            Avviso(c, "La configurazione " + FILE_CONFIGURAZIONE + " installata non e' ancora quella per le API di Bitget:"
                    + "\nimportazione non eseguita. Riprovare dopo l'aggiornamento delle configurazioni.");
            return null;
        }
        progress.setTitle("Scaricamento dei dati di " + NOME_EXCHANGE + " tramite API");
        progress.SetMessaggioAvanzamento("Comunicazione con Bitget in corso...");

        List<JSONObject> spot;
        StringBuilder altriConti = new StringBuilder();
        try {
            Account acc = LeggiAccount(ApiKey, Secret, Passphrase);
            if (!acc.HaPermessoTax()) {
                Avviso(c, "La chiave API di Bitget non ha il permesso \"Tax\" in lettura, l'unico con cui si leggono"
                        + " i movimenti.\nVa creata (o modificata) su Bitget abilitando quel permesso.");
                return null;
            }
            //Prima della registrazione dell'account non c'e' nulla: si risparmiano anni di finestre vuote
            long da = Math.max(DataInizio, acc.Registrazione());
            long a = System.currentTimeMillis() - MARGINE_FINE_MS;
            spot = ScaricaRecord(ApiKey, Secret, Passphrase, "spot-record", da, a, progress, "Conto spot");
            for (String[] conto : CONTI_SOLO_CONTATI) {
                if (spot == null) break;
                List<JSONObject> r = ScaricaRecord(ApiKey, Secret, Passphrase, conto[0], da, a, progress, "Conto " + conto[1]);
                if (r == null) {
                    spot = null;
                    break;
                }
                if (!r.isEmpty()) {
                    altriConti.append("=== CONTO ").append(conto[1].toUpperCase()).append(" BITGET: ").append(r.size())
                            .append(" movimenti NON importati (l'import via API legge solo il conto spot) ===\n");
                    System.out.println("Bitget, conto " + conto[1] + ": " + r.size() + " movimenti non importati");
                }
            }
        } catch (ErroreAPI e) {
            Avviso(c, "Errore nella comunicazione con Bitget:\n" + e.getMessage());
            return null;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Avviso(c, "Bitget non raggiungibile: importazione non eseguita.\n" + e.getMessage());
            return null;
        }
        if (spot == null) {
            Avviso(c, "Scaricamento da " + NOME_EXCHANGE + " interrotto prima della fine: nessun movimento importato.");
            return null;
        }

        List<String[]> righe = InRighe(spot, cfg.causaliAPI);
        Map<String, Integer> sconosciuti = TipiSconosciuti(righe, cfg);
        if (!sconosciuti.isEmpty() && !ConfermaTipiSconosciuti(c, sconosciuti)) return null;

        progress.SetMessaggioAvanzamento("Importazione di " + righe.size() + " movimenti Bitget...");
        Importazioni.AzzeraContatori();
        ImportazioneGenerica.ordinaRighe(righe, cfg);
        //Data dell'ultima riga per gli interessi sommati per giorno: il prossimo scaricamento riparte
        //dall'ultimo movimento piu' un secondo (vedi ImportazioneGenerica.consolidaCausaliPerGiorno)
        if (!ImportazioneGenerica.importaRighe(righe, cfg, false, progress, true)) return null;
        Importazioni.Esito esito = Importazioni.Esito.daiContatori(NOME_EXCHANGE);
        if (altriConti.length() > 0) esito.MovimentiSconosciuti = altriConti + esito.MovimentiSconosciuti;
        return esito;
    }

    private static boolean ConfermaTipiSconosciuti(Component c, Map<String, Integer> Tipi) {
        StringBuilder elenco = new StringBuilder();
        for (Map.Entry<String, Integer> e : Tipi.entrySet()) {
            elenco.append("<br>&nbsp;&nbsp;").append(e.getKey()).append(" (").append(e.getValue()).append(")");
        }
        Object[] scelte = {"Annulla", "Importa senza questi movimenti"};
        int r = JOptionPane.showOptionDialog(null,
                "<html><div style='width:420px'>Bitget ha restituito movimenti di tipo non riconosciuto:" + elenco
                + "<br><br>Se si importa lo stesso, quei movimenti restano fuori, e i prossimi scaricamenti partono"
                + " dall'ultimo movimento importato, quindi non li richiederanno piu'. Annullando non viene scritto"
                + " nulla: si puo' riprovare dopo un aggiornamento della configurazione.</div></html>",
                "Tipi di movimento sconosciuti", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                scelte, scelte[0]);
        return r == 1;
    }

    /**
     * Avviso durante lo scaricamento. Senza parent, come per OKX: la finestra da cui si e' partiti e' bloccata
     * dalla finestra modale di avanzamento.
     */
    private static void Avviso(Component c, String testo) {
        JOptionPane.showConfirmDialog(null, testo, "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
    }
}
