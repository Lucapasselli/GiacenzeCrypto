package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Riconosce da solo il wallet della carta OKX, senza chiederlo all'utente.
 *
 * <p>La carta OKX spende da un "OKX Pay Wallet", un wallet a contratto (account abstraction ERC-4337) sulla rete
 * X Layer. L'exchange lo alimenta con il bill Funding {@code type} 325 "Transfer from exchange to smart wallet",
 * che non porta ne' indirizzo ne' hash, e non compare nello storico prelievi ({@code asset/withdrawal-history},
 * verificato il 07/10/2026: nessun movimento USDG). L'indirizzo si ricava quindi dalla blockchain: nei minuti
 * successivi al bill si cerca, sul nodo pubblico di X Layer, il trasferimento della stessa moneta per la quantita'
 * esatta, e il destinatario deve essere un wallet ERC-4337 ({@code entryPoint()} restituisce un EntryPoint
 * noto). Sul caso reale il trasferimento e' arrivato 34 secondi dopo il bill, da un wallet caldo di OKX.
 *
 * <p>Con piu' bill 325 tutti devono portare allo stesso indirizzo: se non tornano, o se un bill ha piu' candidati,
 * non si sceglie nulla. Il nodo pubblico non chiede chiavi; {@code eth_getLogs} accetta al massimo 100 blocchi per
 * richiesta e X Layer fa circa un blocco al secondo, quindi un bill costa una ventina di richieste.
 */
final class OKX_WalletCarta {

    private OKX_WalletCarta() {}

    /** Nodo pubblico ufficiale di X Layer (chain id 196). */
    static final String RPC_XLAYER = "https://rpc.xlayer.tech";

    /** Opzione (personale.mv.db): indirizzo del wallet della carta individuato. */
    static final String OPZIONE_WALLET = "OKX_WalletCarta_XLayer";

    /** Bill Funding del trasferimento dall'exchange al wallet della carta. */
    static final String TIPO_VERSO_CARTA = "325";

    static final String TOPIC_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    /** EntryPoint ERC-4337 v0.7 e v0.6: il wallet della carta risponde a {@code entryPoint()} con uno dei due. */
    static final Set<String> ENTRYPOINT = Set.of(
            "0x0000000071727de22e5e9d8baf0edac6f37da032",
            "0x5ff137d4b0fdcd49dca30c7cf57e578a026d2789");

    /** Contratti su X Layer delle monete che la carta usa, tutte a 6 decimali (verificato con decimals()). */
    static final Map<String, List<String>> TOKEN_XLAYER = Map.of(
            "USDG", List.of("0x4ae46a509f6b1d9056937ba4500cb143933d2dc8"),
            "USDC", List.of("0x74b7f16337b8972027f6196a17a631ac6de26d22"),
            "USDT", List.of("0x1e4a5963abfd975d8c9021ce480b42188849d41d", "0x779ded0c9e1022225f8e0630b35a9b54be713736"));
    static final int DECIMALI = 6;

    /** Finestra di ricerca attorno al bill: un po' prima (orologi diversi) e fino a 15 minuti dopo. */
    static final long ANTICIPO_S = 120;
    static final long RITARDO_S = 900;
    static final int BLOCCHI_PER_RICHIESTA = 100;

    /** Accesso al nodo, separato perche' i test lo sostituiscono. */
    interface Rpc {
        JsonElement chiama(String metodo, JsonArray parametri) throws Exception;
    }

    record Trasferimento(long ts, String moneta, BigDecimal qta) {}

    record Esito(String indirizzo, List<String> note) {}

    /** Bill 325 dello scaricamento: istante, moneta e quantita' uscita dall'exchange. */
    static List<Trasferimento> TrasferimentiVersoCarta(JsonArray fundingBills) {
        List<Trasferimento> lista = new ArrayList<>();
        if (fundingBills == null) return lista;
        for (JsonElement el : fundingBills) {
            if (!el.isJsonObject()) continue;
            JsonObject b = el.getAsJsonObject();
            if (!TIPO_VERSO_CARTA.equals(Testo(b, "type"))) continue;
            String ts = Testo(b, "ts"), balChg = Testo(b, "balChg"), ccy = Testo(b, "ccy").toUpperCase();
            if (!Funzioni.isNumeric(ts, false) || !Funzioni.isNumeric(balChg, false) || ccy.isEmpty()) continue;
            BigDecimal qta = new BigDecimal(balChg).negate();
            if (qta.signum() > 0) lista.add(new Trasferimento(Long.parseLong(ts), ccy, qta));
        }
        return lista;
    }

    /**
     * Cerca il wallet della carta. Restituisce l'indirizzo (minuscolo) solo se tutti i trasferimenti riconosciuti
     * portano allo stesso wallet ERC-4337, altrimenti {@code null}; le note dicono perche'.
     */
    static Esito Trova(List<Trasferimento> trasferimenti, Rpc rpc) {
        List<String> note = new ArrayList<>();
        Set<String> trovati = new LinkedHashSet<>();
        try {
            for (Trasferimento t : trasferimenti) {
                List<String> token = TOKEN_XLAYER.get(t.moneta());
                if (token == null) {
                    note.add(t.moneta() + ": moneta non prevista sul wallet della carta, trasferimento ignorato.");
                    continue;
                }
                Set<String> candidati = Candidati(t, token, rpc);
                if (candidati.size() == 1) trovati.addAll(candidati);
                else if (candidati.isEmpty()) note.add("Nessun trasferimento su X Layer per " + Testo(t.qta()) + " " + t.moneta() + " del " + Data(t.ts()) + ".");
                else note.add("Piu' wallet candidati per " + Testo(t.qta()) + " " + t.moneta() + " del " + Data(t.ts()) + ".");
            }
        } catch (Exception e) {
            note.add("Nodo di X Layer non raggiungibile: " + e.getMessage());
            return new Esito(null, note);
        }
        if (trovati.size() > 1) {
            note.add("I trasferimenti verso la carta portano a wallet diversi, nessuno scelto.");
            return new Esito(null, note);
        }
        return new Esito(trovati.isEmpty() ? null : trovati.iterator().next(), note);
    }

    private static Set<String> Candidati(Trasferimento t, List<String> token, Rpc rpc) throws Exception {
        Set<String> candidati = new LinkedHashSet<>();
        BigInteger atteso;
        try {
            atteso = t.qta().movePointRight(DECIMALI).toBigIntegerExact();
        } catch (ArithmeticException e) {
            return candidati;   //piu' decimali di quelli del token: non puo' essere quel trasferimento
        }
        long secondo = t.ts() / 1000;
        long da = BloccoAlTempo(secondo - ANTICIPO_S, rpc);
        long a = BloccoAlTempo(secondo + RITARDO_S, rpc);
        Set<String> destinatari = new LinkedHashSet<>();
        for (long inizio = da; inizio <= a; inizio += BLOCCHI_PER_RICHIESTA) {
            long fine = Math.min(a, inizio + BLOCCHI_PER_RICHIESTA - 1);
            JsonObject filtro = new JsonObject();
            filtro.addProperty("fromBlock", "0x" + Long.toHexString(inizio));
            filtro.addProperty("toBlock", "0x" + Long.toHexString(fine));
            JsonArray indirizzi = new JsonArray();
            token.forEach(indirizzi::add);
            filtro.add("address", indirizzi);
            JsonArray topics = new JsonArray();
            topics.add(TOPIC_TRANSFER);
            filtro.add("topics", topics);
            JsonArray parametri = new JsonArray();
            parametri.add(filtro);
            for (JsonElement el : rpc.chiama("eth_getLogs", parametri).getAsJsonArray()) {
                JsonObject log = el.getAsJsonObject();
                JsonArray tp = log.getAsJsonArray("topics");
                if (tp.size() < 3) continue;
                if (Esadecimale(Testo(log, "data")).equals(atteso)) destinatari.add(Indirizzo(tp.get(2).getAsString()));
            }
        }
        for (String d : destinatari) {
            if (ENTRYPOINT.contains(EntryPoint(d, rpc))) candidati.add(d);
        }
        return candidati;
    }

    /** {@code entryPoint()} del contratto, oppure stringa vuota se non risponde (non e' un wallet ERC-4337). */
    private static String EntryPoint(String indirizzo, Rpc rpc) {
        try {
            JsonObject chiamata = new JsonObject();
            chiamata.addProperty("to", indirizzo);
            chiamata.addProperty("data", "0xb0d691fe");
            JsonArray parametri = new JsonArray();
            parametri.add(chiamata);
            parametri.add("latest");
            String r = rpc.chiama("eth_call", parametri).getAsString();
            return r.length() >= 42 ? Indirizzo(r) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Primo blocco con timestamp maggiore o uguale a {@code secondo} (l'ultimo, se nel futuro). */
    static long BloccoAlTempo(long secondo, Rpc rpc) throws Exception {
        long ultimo = Long.decode(rpc.chiama("eth_blockNumber", new JsonArray()).getAsString());
        long tempoUltimo = TempoBlocco(ultimo, rpc);
        if (tempoUltimo < secondo) return ultimo;
        //Stima a un blocco al secondo e ricerca binaria attorno: se la stima sbaglia si allarga a tutta la catena
        long stima = Math.max(0, ultimo - (tempoUltimo - secondo));
        long basso = Math.max(0, stima - 100_000), alto = Math.min(ultimo, stima + 100_000);
        if (basso > 0 && TempoBlocco(basso, rpc) >= secondo) basso = 0;
        if (alto < ultimo && TempoBlocco(alto, rpc) < secondo) alto = ultimo;
        while (basso < alto) {
            long medio = (basso + alto) >>> 1;
            if (TempoBlocco(medio, rpc) < secondo) basso = medio + 1;
            else alto = medio;
        }
        return basso;
    }

    private static long TempoBlocco(long numero, Rpc rpc) throws Exception {
        JsonArray parametri = new JsonArray();
        parametri.add("0x" + Long.toHexString(numero));
        parametri.add(false);
        return Long.decode(rpc.chiama("eth_getBlockByNumber", parametri).getAsJsonObject().get("timestamp").getAsString());
    }

    /** Nodo pubblico di X Layer via JSON-RPC. */
    static Rpc RpcXLayer() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();
        MediaType json = MediaType.parse("application/json");
        return (metodo, parametri) -> {
            JsonObject richiesta = new JsonObject();
            richiesta.addProperty("jsonrpc", "2.0");
            richiesta.addProperty("id", 1);
            richiesta.addProperty("method", metodo);
            richiesta.add("params", parametri);
            Request r = new Request.Builder().url(RPC_XLAYER).post(RequestBody.create(richiesta.toString(), json)).build();
            try (Response risposta = client.newCall(r).execute()) {
                String corpo = risposta.body() != null ? risposta.body().string() : "";
                if (!risposta.isSuccessful()) throw new Exception("HTTP " + risposta.code() + " " + corpo);
                JsonObject j = JsonParser.parseString(corpo).getAsJsonObject();
                if (j.has("error")) throw new Exception(j.get("error").toString());
                return j.get("result");
            }
        };
    }

    /** Testo dell'avviso quando il wallet viene individuato per la prima volta (o cambia). */
    static String TestoAvviso(String indirizzo) {
        return "Individuato il wallet della carta OKX sulla rete X Layer:\n\n   " + indirizzo + "\n\n"
                + "È il wallet a cui vanno i trasferimenti \"Transfer from exchange to smart wallet\".\n"
                + "Il programma non importa ancora i movimenti di X Layer: l'indirizzo è salvato\n"
                + "e lo trovi in Exchange API, scheda Particolarità OKX.";
    }

    private static BigInteger Esadecimale(String h) {
        if (h == null || !h.startsWith("0x") || h.length() <= 2) return BigInteger.valueOf(-1);
        try {
            return new BigInteger(h.substring(2), 16);
        } catch (NumberFormatException e) {
            return BigInteger.valueOf(-1);
        }
    }

    /** Ultimi 20 byte di una parola a 32 byte (topic o risultato di eth_call), in minuscolo. */
    private static String Indirizzo(String parola) {
        String h = parola.toLowerCase();
        return "0x" + h.substring(h.length() - 40);
    }

    private static String Data(long ts) {
        return FunzioniDate.ConvertiDatadaLongAlSecondo(ts);
    }

    private static String Testo(JsonObject o, String campo) {
        return o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }

    private static String Testo(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
