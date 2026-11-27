package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Il <b>mark price</b> di un contratto derivato a un istante, per valutare il non realizzato delle posizioni aperte al 31/12 nel
 * quadro W/RW ({@link Calcoli_RW_Derivati}): è il prezzo con cui l'exchange stesso calcola il "PnL non realizzato", ed è
 * quello giusto da confrontare con il prezzo di esecuzione dello stesso exchange (lo spot di un'altra fonte porta dentro la base
 * del contratto). Un mark price è <b>di quel contratto su quell'exchange</b>: non si scambia fra exchange.
 * <ul>
 *   <li>OKX: {@code /api/v5/market/history-mark-price-candles}, candela a un minuto, per {@code instId}. I contratti scaduti non sono
 *       serviti (errori 51001 e 50047): resta il ripiego dello spot.</li>
 *   <li>Bybit: {@code /v5/market/mark-price-kline}, categoria {@code linear} o {@code inverse}, anche per i future scaduti.</li>
 *   <li>Binance: non c'è, il programma non conosce le posizioni dei suoi futures.</li>
 * </ul>
 * Endpoint pubblici, senza chiavi, indirizzi da {@link IndirizziServizi}. Una lettura riuscita si salva in {@code PrezziMarkDerivati}
 * ({@code prezzi.mv.db}), così il numero di una dichiarazione si ritrova uguale senza rete. Chi non risponde certo (rete assente, errore)
 * non si ricorda e si riprova; chi risponde "contratto inesistente" si ricorda solo per la sessione. Senza un mark price, chi chiama ripiega
 * sullo spot e lo dice. Logica senza Swing: la lettura dalla rete si sostituisce nei test con {@link #FONTE}.
 */
final class MarkPriceDerivati {

    private MarkPriceDerivati() {
    }

    /** La candela cercata comincia al massimo cosi' tanti millisecondi prima del minuto chiesto (lacune del feed). */
    private static final long TOLLERANZA_MS = 5 * 60_000L;

    /**
     * Esito di una lettura dalla rete.
     * @param prezzo il mark price, {@code null} se non c'è
     * @param certo  {@code true} se l'exchange ha risposto (anche "contratto inesistente"), {@code false} se non si è potuto chiedere
     */
    record Esito(BigDecimal prezzo, boolean certo) {
        static Esito trovato(BigDecimal p) {
            return new Esito(p, true);
        }

        static Esito assente() {
            return new Esito(null, true);
        }

        static Esito nonLetto() {
            return new Esito(null, false);
        }
    }

    /** Dove si legge il mark price: in produzione la rete, nei test un valore fisso. */
    interface Fonte {
        /**
         * @param piattaforma {@code OKX} o {@code BYBIT}
         * @param simbolo     simbolo nativo del contratto ({@code instId} su OKX, simbolo Bybit)
         * @param inverso     contratto regolato nel sottostante (categoria {@code inverse} di Bybit)
         * @param minuto      inizio della candela a un minuto, millisecondi
         */
        Esito leggi(String piattaforma, String simbolo, boolean inverso, long minuto);
    }

    static volatile Fonte FONTE = MarkPriceDerivati::DaRete;

    /** Contratti per cui l'exchange ha risposto che non li ha: solo per la sessione. */
    private static final Map<String, Boolean> ASSENTI = new ConcurrentHashMap<>();

    /** Solo per i test. */
    static void AzzeraMemoria() {
        ASSENTI.clear();
    }

    /** @return {@code true} se per quella piattaforma il programma sa leggere il mark price */
    static boolean Supportata(String piattaforma) {
        return Normalizza(piattaforma) != null;
    }

    /** {@code OKX} o {@code BYBIT} dal nome dell'exchange come sta nel campo dati {@code piattaforma}; {@code null} per gli altri. */
    static String Normalizza(String piattaforma) {
        if (piattaforma == null) return null;
        String p = piattaforma.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (p.startsWith("OKX")) return "OKX";
        if (p.startsWith("BYBIT")) return "BYBIT";
        return null;
    }

    /**
     * Il mark price del contratto all'istante: il valore del minuto che precede {@code istanteMs} (per il 31/12, l'ultimo minuto
     * dell'anno).
     *
     * @return il mark price, {@code null} se la piattaforma non è supportata o non si è trovato (chi chiama ripiega sullo spot)
     */
    static BigDecimal Mark(String piattaforma, String simbolo, boolean inverso, long istanteMs) {
        String p = Normalizza(piattaforma);
        if (p == null || simbolo == null || simbolo.isBlank()) return null;
        String s = simbolo.trim();
        long minuto = istanteMs - 60_000L;
        String chiave = p + "|" + s + "|" + minuto;
        if (ASSENTI.containsKey(chiave)) return null;
        BigDecimal salvato = Letto(p, s, minuto);
        if (salvato != null) return salvato;
        Esito e;
        try {
            e = FONTE.leggi(p, s, inverso, minuto);
        } catch (RuntimeException ex) {
            LoggerGC.logInfo("Mark price " + p + " " + s + ": non letto: " + ex.getMessage());
            return null;
        }
        if (e == null) return null;
        if (e.prezzo() != null && e.prezzo().signum() > 0) {
            Salva(p, s, minuto, e.prezzo());
            return e.prezzo();
        }
        if (e.certo()) ASSENTI.put(chiave, Boolean.TRUE);
        return null;
    }

    // =================================================================================================
    // CACHE
    // =================================================================================================

    private static BigDecimal Letto(String piattaforma, String simbolo, long minuto) {
        String sql = "SELECT prezzo FROM PrezziMarkDerivati WHERE piattaforma = ? AND simbolo = ? AND minuto = ?";
        //La connessione si prende a ogni uso: la compattazione del database la sostituisce
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setString(1, piattaforma);
            ps.setString(2, simbolo);
            ps.setLong(3, minuto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new BigDecimal(rs.getString(1));
            }
        } catch (SQLException | RuntimeException ex) {
            LoggerGC.logInfo("Mark price: cache non letta: " + ex.getMessage());
        }
        return null;
    }

    private static void Salva(String piattaforma, String simbolo, long minuto, BigDecimal prezzo) {
        String sql = "MERGE INTO PrezziMarkDerivati (piattaforma, simbolo, minuto, prezzo) KEY (piattaforma, simbolo, minuto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setString(1, piattaforma);
            ps.setString(2, simbolo);
            ps.setLong(3, minuto);
            ps.setString(4, prezzo.stripTrailingZeros().toPlainString());
            ps.executeUpdate();
        } catch (SQLException | RuntimeException ex) {
            LoggerGC.logInfo("Mark price: cache non scritta: " + ex.getMessage());
        }
    }

    // =================================================================================================
    // RETE
    // =================================================================================================

    private static Esito DaRete(String piattaforma, String simbolo, boolean inverso, long minuto) {
        if (Funzioni.ReteDisabilitataPerTest) return Esito.nonLetto();
        String s = URLEncoder.encode(simbolo, StandardCharsets.UTF_8);
        String url;
        if (piattaforma.equals("OKX")) {
            String base = IndirizziServizi.Url(IndirizziServizi.OKX_PUBBLICO);
            if (base.isEmpty()) return Esito.nonLetto();
            //"after" = candele piu' vecchie del tempo dato: con minuto + 1 minuto la prima e' quella del minuto cercato
            url = base + "/api/v5/market/history-mark-price-candles?instId=" + s + "&bar=1m&after=" + (minuto + 60_000L) + "&limit=5";
        } else {
            String base = IndirizziServizi.Url(IndirizziServizi.BYBIT_PUBBLICO);
            if (base.isEmpty()) return Esito.nonLetto();
            url = base + "/v5/market/mark-price-kline?category=" + (inverso ? "inverse" : "linear") + "&symbol=" + s
                    + "&interval=1&start=" + (minuto - TOLLERANZA_MS) + "&end=" + (minuto + 59_999L) + "&limit=5";
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            return piattaforma.equals("OKX") ? InterpretaOkx(r.statusCode(), r.body(), minuto)
                    : InterpretaBybit(r.statusCode(), r.body(), minuto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Esito.nonLetto();
        } catch (java.io.IOException | RuntimeException e) {
            return Esito.nonLetto();
        }
    }

    /**
     * Risposta di {@code history-mark-price-candles}: {@code data} è l'elenco delle candele dalla più recente, ognuna
     * {@code [ts, apertura, massimo, minimo, chiusura, confermata]}. Codice 51001 (contratto inesistente) e 50047 (già
     * regolato): il contratto non c'è più, esito certo.
     */
    static Esito InterpretaOkx(int stato, String corpo, long minuto) {
        if (stato != 200 || corpo == null) return Esito.nonLetto();
        try {
            JSONObject j = new JSONObject(corpo);
            String codice = j.optString("code", "");
            if (codice.equals("51001") || codice.equals("50047")) return Esito.assente();
            if (!codice.equals("0")) return Esito.nonLetto();
            return Candela(j.optJSONArray("data"), minuto);
        } catch (RuntimeException e) {
            return Esito.nonLetto();
        }
    }

    /**
     * Risposta di {@code mark-price-kline}: {@code result.list} dalla candela più recente, ognuna
     * {@code [inizio, apertura, massimo, minimo, chiusura]}. {@code retCode} 10001 è un parametro non valido (simbolo inesistente).
     */
    static Esito InterpretaBybit(int stato, String corpo, long minuto) {
        if (stato != 200 || corpo == null) return Esito.nonLetto();
        try {
            JSONObject j = new JSONObject(corpo);
            int codice = j.optInt("retCode", -1);
            if (codice == 10001) return Esito.assente();
            if (codice != 0) return Esito.nonLetto();
            JSONObject risultato = j.optJSONObject("result");
            return Candela(risultato == null ? null : risultato.optJSONArray("list"), minuto);
        } catch (RuntimeException e) {
            return Esito.nonLetto();
        }
    }

    /** La chiusura della candela più recente non dopo il minuto cercato e non più vecchia della tolleranza. */
    private static Esito Candela(JSONArray lista, long minuto) {
        if (lista == null) return Esito.nonLetto();
        for (int i = 0; i < lista.length(); i++) {
            JSONArray c = lista.optJSONArray(i);
            if (c == null || c.length() < 5) continue;
            long inizio;
            try {
                inizio = Long.parseLong(c.get(0).toString());
            } catch (NumberFormatException e) {
                continue;
            }
            if (inizio > minuto || inizio < minuto - TOLLERANZA_MS) continue;
            try {
                BigDecimal chiusura = new BigDecimal(c.get(4).toString());
                if (chiusura.signum() > 0) return Esito.trovato(chiusura);
            } catch (NumberFormatException e) {
                //candela illeggibile: si prova la successiva
            }
        }
        //Risposta valida ma senza una candela utile: il contratto non aveva un prezzo in quel momento
        return Esito.assente();
    }
}
