package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Importazione di un wallet Tron, senza chiavi. Indirizzi in {@code config/varie/IndirizziServizi.json}
 * ({@code tronGrid}, {@code tronScan}).
 *
 * <p>Fonti, verificate il 23/10/2026 su quattro wallet pubblici (73-3.300 transazioni):
 * <ul>
 *   <li><b>TronGrid</b>, il servizio ufficiale: le transazioni del wallet (TRX, TRC10, staking, commissioni, TRX
 *       mossi dai contratti nelle sue transazioni) e i trasferimenti TRC20. Questi ultimi hanno solo l'orario: il
 *       blocco c'è per quelli firmati dal wallet (sono anche fra le transazioni), non per gli accrediti;</li>
 *   <li><b>TronScan</b> (endpoint ancora aperti senza chiave): il blocco degli accrediti TRC20, i TRX mossi verso il
 *       wallet dai contratti in transazioni <b>di altri</b> (che TronGrid non collega al wallet) e il livello dei token
 *       (4 = truffa). Se non risponde, il blocco si chiede al nodo TronGrid transazione per transazione, più lento.</li>
 * </ul>
 *
 * <p>Regole del saldo: la commissione è {@code ret[0].fee} di ogni transazione firmata dal wallet, riuscita o no
 * (comprende l'attivazione di conti, che {@code net_fee}+{@code energy_fee} non hanno). Congelare, scongelare,
 * delegare e votare non cambiano i TRX posseduti: la verifica conta liberi + congelati + in scongelamento + delegati.
 * Il ritiro delle ricompense di voto ({@code withdraw_amount}) è una riga a parte. Dei TRC20 contano solo gli eventi
 * {@code Transfer} (non gli {@code Approval}).
 *
 * <p>Truffe: i token che TronScan dà di livello 4 hanno il suffisso {@code " **"} (il marcatore SCAM del programma), e
 * i loro trasferimenti "dal wallet" in transazioni che il wallet non ha firmato si scartano: sono gli eventi
 * inventati dell'address poisoning. Restano fuori dalla verifica dei saldi; i token con commissione sul
 * trasferimento (che tolgono quantità senza evento) vengono segnalati da lì.
 *
 * <p>La ripresa riparte dall'orario dell'ultimo blocco importato ({@code /wallet/getblockbynum}). Dettagli e
 * indirizzi di prova: {@code nocommit/Documentazione/Analisi_Import_XRP_ALGO_TRX.md}.
 */
final class Trans_Tron {

    private Trans_Tron() {}

    static final String RETE = "TRX";
    static final String MONETA = "TRX";
    static final String PROVIDER = "TRONGRID";
    static final int DECIMALI_TRX = 6;
    static final String CAUSALE_RICOMPENSE = "Ricompense da voto Tron";
    /** Livello TronScan dei token truffa. */
    static final String LIVELLO_TRUFFA = "4";

    private static final int PAGINA = 200;
    private static final int PAGINA_TRONSCAN = 50;
    private static final int TENTATIVI = 6;
    /**
     * TronGrid senza chiave concede una richiesta al secondo e, se la si supera, sospende per 2-5 secondi (verificato il
     * 23/10/2026): meglio andare piano che sbattere sui 429.
     */
    private static final long PAUSA_TRONGRID_MS = 1100;
    private static long ultimaTronGrid;
    private static final MediaType JSON = MediaType.get("application/json");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    /** Un token: {@code chiave} è l'indirizzo del contratto TRC20 o l'id del TRC10. */
    record Token(String chiave, String simbolo, String nome, int decimali, boolean truffa) {}

    /**
     * Una transazione ridotta all'effetto sul wallet.
     *
     * @param variazioni chiave {@link #MONETA} o quella del token, in unità minime con segno; commissione e
     *                   ricompense escluse, mai zero
     * @param commissione sun pagati dal wallet
     * @param ricompense sun di ricompense di voto ritirate
     */
    record Operazione(String hash, long blocco, long timestamp, String tipo, String controparte, boolean riuscita,
            Map<String, BigInteger> variazioni, BigInteger commissione, BigInteger ricompense) {}

    /** @param token chiave → token, con il simbolo già scelto ({@link #Simboli}) */
    record Esito(List<Operazione> operazioni, Map<String, Token> token, List<String> avvisi, boolean completo) {}

    /** Quanto serve dei dati di TronScan: blocco dei trasferimenti TRC20, livello e simbolo dei token. */
    record DatiTronScan(Map<String, Long> blocchi, Map<String, String> livelli, Map<String, String> simboli,
            List<JSONObject> interne, boolean letti) {}

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /**
     * Scarica le transazioni del wallet dal blocco {@code bloccoDa} (incluso). In una prima importazione confronta poi
     * la somma con i saldi del conto ({@link #VerificaSaldi}).
     *
     * @return {@code completo} falso se lo scaricamento non è arrivato in fondo: allora non si importa nulla
     */
    static Esito Scarica(String wallet, long bloccoDa, BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        String grid = IndirizziServizi.Url(IndirizziServizi.TRONGRID);
        String scan = IndirizziServizi.Url(IndirizziServizi.TRONSCAN);
        try {
            Stato(avanzamento, "Tron: lettura del conto " + wallet + "...");
            String testoConto = Get(grid + "/v1/accounts/" + wallet, interrotto);
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "Tron", "accounts " + wallet, testoConto);
            JSONArray dati = new JSONObject(testoConto).optJSONArray("data");
            if (dati == null || dati.isEmpty()) {
                System.out.println("Tron " + wallet + ": conto non attivato.");
                return new Esito(List.of(), Map.of(), avvisi, true);
            }
            JSONObject conto = dati.getJSONObject(0);
            long fino = System.currentTimeMillis();
            //Ripresa: dall'orario dell'ultimo blocco importato (bloccoDa - 1). TronGrid non applica il filtro al
            //millisecondo (con min_timestamp = orario del blocco + 1 restituisce ancora le transazioni di quel blocco,
            //verificato il 23/10/2026): si chiede dal blocco stesso e si scarta per numero di blocco, piu' sotto
            long da = 0;
            if (bloccoDa > 1) {
                JSONObject b = new JSONObject(Post(grid + "/wallet/getblockbynum", new JSONObject().put("num", bloccoDa - 1), interrotto));
                da = b.getJSONObject("block_header").getJSONObject("raw_data").getLong("timestamp");
            }
            String intervallo = "&only_confirmed=true&min_timestamp=" + da + "&max_timestamp=" + fino;

            List<JSONObject> nat = PagineGrid(grid + "/v1/accounts/" + wallet + "/transactions?limit=" + PAGINA + intervallo,
                    "transazioni", interrotto, idDocumento, avanzamento);
            List<JSONObject> t20 = PagineGrid(grid + "/v1/accounts/" + wallet + "/transactions/trc20?limit=" + PAGINA + intervallo,
                    "trasferimenti TRC20", interrotto, idDocumento, avanzamento);
            if (nat == null || t20 == null) return new Esito(List.of(), Map.of(), avvisi, false);
            System.out.println("Tron " + wallet + ": " + nat.size() + " transazioni e " + t20.size() + " eventi TRC20.");

            DatiTronScan ts = LeggiTronScan(scan, wallet, da, fino, interrotto, idDocumento, avanzamento);
            if (!ts.letti()) {
                avvisi.add("TronScan non ha risposto: senza i TRX ricevuti dai contratti in transazioni di altri e senza il"
                        + " riconoscimento dei token truffa.");
            }

            //Blocco degli accrediti TRC20 che TronScan non ha dato: uno per uno dal nodo
            Set<String> nativi = new HashSet<>();
            for (JSONObject x : nat) nativi.add(x.optString("txID"));
            Map<String, Long> blocchi = new HashMap<>(ts.blocchi());
            List<String> mancanti = new ArrayList<>();
            for (JSONObject x : t20) {
                String id = x.optString("transaction_id");
                if ("Transfer".equals(x.optString("type")) && !nativi.contains(id) && !blocchi.containsKey(id) && !mancanti.contains(id)) {
                    mancanti.add(id);
                }
            }
            for (int i = 0; i < mancanti.size(); i++) {
                Stato(avanzamento, "Tron: blocco del trasferimento " + (i + 1) + " di " + mancanti.size());
                JSONObject info = new JSONObject(Post(grid + "/wallet/gettransactioninfobyid", new JSONObject().put("value", mancanti.get(i)), interrotto));
                if (info.has("blockNumber")) blocchi.put(mancanti.get(i), info.getLong("blockNumber"));
            }

            Set<String> truffe = new HashSet<>();
            ts.livelli().forEach((k, v) -> { if (LIVELLO_TRUFFA.equals(v)) truffe.add(k); });
            List<Operazione> operazioni = new ArrayList<>(Analizza(wallet, nat, t20, ts.interne(), blocchi, truffe, avvisi));
            operazioni = DalBlocco(operazioni, bloccoDa);

            Map<String, Token> token = new LinkedHashMap<>();
            Map<String, JSONObject> infoTRC20 = new HashMap<>();
            for (JSONObject x : t20) infoTRC20.putIfAbsent(x.optJSONObject("token_info") == null ? "" : x.getJSONObject("token_info").optString("address"), x.optJSONObject("token_info"));
            for (Operazione o : operazioni) {
                for (String k : o.variazioni().keySet()) {
                    if (k.equals(MONETA) || token.containsKey(k)) continue;
                    if (isIdTRC10(k)) token.put(k, LeggiTRC10(grid, k, interrotto));
                    else {
                        JSONObject ti = infoTRC20.get(k);
                        String simbolo = ti == null ? "" : ti.optString("symbol", "");
                        if (simbolo.isBlank()) simbolo = ts.simboli().getOrDefault(k, "");
                        token.put(k, new Token(k, simbolo, ti == null ? simbolo : ti.optString("name", simbolo),
                                ti == null ? 0 : ti.optInt("decimals", 0), truffe.contains(k)));
                    }
                }
            }
            Map<String, Token> conSimboli = Simboli(operazioni, token, ts.livelli(), SimboliGiaUsati(Principale.MappaCryptoWallet));
            if (bloccoDa <= 1) avvisi.addAll(VerificaSaldi(operazioni, conto, conSimboli));
            System.out.println("Tron " + wallet + ": " + operazioni.size() + " operazioni con effetto sul wallet.");
            return new Esito(operazioni, conSimboli, avvisi, true);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            avvisi.add("TronGrid: " + ex.getMessage());
            return new Esito(List.of(), Map.of(), avvisi, false);
        }
    }

    /** Tutte le pagine di un elenco TronGrid; {@code null} se interrotto. */
    private static List<JSONObject> PagineGrid(String url, String cosa, BooleanSupplier interrotto, int idDocumento,
            Consumer<String> avanzamento) throws IOException, InterruptedException {
        List<JSONObject> tutto = new ArrayList<>();
        int pagina = 0;
        while (url != null && !url.isBlank()) {
            if (Interrotto(interrotto)) return null;
            pagina++;
            Stato(avanzamento, "Tron: pagina " + pagina + " delle " + cosa + " (" + tutto.size() + " lette)");
            String testo = Get(url, interrotto);
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "Tron", cosa + " pagina " + pagina, testo);
            JSONObject r = new JSONObject(testo);
            if (!r.optBoolean("success", true)) throw new IOException("TronGrid: " + r.optString("error", testo));
            JSONArray d = r.optJSONArray("data");
            for (int i = 0; d != null && i < d.length(); i++) tutto.add(d.getJSONObject(i));
            JSONObject meta = r.optJSONObject("meta");
            JSONObject links = meta == null ? null : meta.optJSONObject("links");
            url = links == null ? null : links.optString("next", null);
        }
        return tutto;
    }

    /** Blocchi e livelli dei TRC20 e interne di terzi da TronScan; {@code letti} falso se non ha risposto. */
    private static DatiTronScan LeggiTronScan(String scan, String wallet, long da, long fino, BooleanSupplier interrotto,
            int idDocumento, Consumer<String> avanzamento) throws InterruptedException {
        Map<String, Long> blocchi = new HashMap<>();
        Map<String, String> livelli = new HashMap<>();
        Map<String, String> simboli = new HashMap<>();
        List<JSONObject> interne = new ArrayList<>();
        String finestra = "&start_timestamp=" + da + "&end_timestamp=" + fino;
        try {
            for (int start = 0; ; start += PAGINA_TRONSCAN) {
                Stato(avanzamento, "TronScan: trasferimenti TRC20 da " + start);
                String testo = Get(scan + "/token_trc20/transfers?relatedAddress=" + wallet + "&start=" + start
                        + "&limit=" + PAGINA_TRONSCAN + finestra, interrotto);
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "TronScan", "token_trc20/transfers da " + start, testo);
                JSONArray a = new JSONObject(testo).optJSONArray("token_transfers");
                for (int i = 0; a != null && i < a.length(); i++) {
                    JSONObject x = a.getJSONObject(i);
                    blocchi.put(x.optString("transaction_id"), x.optLong("block"));
                    JSONObject ti = x.optJSONObject("tokenInfo");
                    if (ti != null) {
                        livelli.put(x.optString("contract_address"), ti.optString("tokenLevel", ""));
                        simboli.put(x.optString("contract_address"), ti.optString("tokenAbbr", ""));
                    }
                }
                if (a == null || a.length() < PAGINA_TRONSCAN) break;
                Thread.sleep(600);
            }
            for (int start = 0; ; start += PAGINA_TRONSCAN) {
                Stato(avanzamento, "TronScan: transazioni interne da " + start);
                String testo = Get(scan + "/internal-transaction?address=" + wallet + "&start=" + start
                        + "&limit=" + PAGINA_TRONSCAN + finestra, interrotto);
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "TronScan", "internal-transaction da " + start, testo);
                JSONArray a = new JSONObject(testo).optJSONArray("data");
                for (int i = 0; a != null && i < a.length(); i++) interne.add(a.getJSONObject(i));
                if (a == null || a.length() < PAGINA_TRONSCAN) break;
                Thread.sleep(600);
            }
            return new DatiTronScan(blocchi, livelli, simboli, interne, true);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
            return new DatiTronScan(Map.of(), Map.of(), Map.of(), List.of(), false);
        }
    }

    private static Token LeggiTRC10(String grid, String id, BooleanSupplier interrotto) throws IOException, InterruptedException {
        JSONArray d = new JSONObject(Get(grid + "/v1/assets/" + id, interrotto)).optJSONArray("data");
        JSONObject a = d == null || d.isEmpty() ? new JSONObject() : d.getJSONObject(0);
        String simbolo = a.optString("abbr", "").trim();
        String nome = a.optString("name", "").trim();
        return new Token(id, simbolo.isEmpty() ? nome : simbolo, nome.isEmpty() ? simbolo : nome, a.optInt("precision", 0), false);
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================

    private static final class Accumulo {
        long blocco;
        long timestamp;
        boolean riuscita = true;
        String controparte = "";
        final Set<String> tipi = new LinkedHashSet<>();
        final Map<String, BigInteger> variazioni = new LinkedHashMap<>();
        BigInteger commissione = BigInteger.ZERO;
        BigInteger ricompense = BigInteger.ZERO;

        void Aggiungi(String k, BigInteger x) {
            if (x.signum() != 0) variazioni.merge(k, x, BigInteger::add);
        }

        void Controparte(String c, String w) {
            if (controparte.isEmpty() && c != null && !c.isEmpty() && !c.equals(w)) controparte = c;
        }
    }

    /**
     * Riduce le tre fonti alle operazioni del wallet, una per transazione, in ordine di blocco.
     *
     * @param nat transazioni di TronGrid ({@code /v1/accounts/{a}/transactions})
     * @param t20 eventi TRC20 di TronGrid
     * @param interne transazioni interne di TronScan: si usano solo quelle di transazioni che non sono in {@code nat}
     * @param blocchi blocco delle transazioni che non sono in {@code nat}
     * @param truffe contratti TRC20 di livello truffa
     */
    static List<Operazione> Analizza(String w, List<JSONObject> nat, List<JSONObject> t20, List<JSONObject> interne,
            Map<String, Long> blocchi, Set<String> truffe, List<String> avvisi) {
        Map<String, Accumulo> per = new LinkedHashMap<>();
        Set<String> firmate = new HashSet<>();
        Set<String> nativi = new HashSet<>();
        for (JSONObject tx : nat) {
            String id = tx.optString("txID");
            if (!nativi.add(id)) continue;
            JSONObject c = tx.getJSONObject("raw_data").getJSONArray("contract").getJSONObject(0);
            String tipo = c.optString("type");
            JSONObject v = c.getJSONObject("parameter").getJSONObject("value");
            String firma = Base58(v.optString("owner_address"));
            JSONObject ret = tx.optJSONArray("ret") == null || tx.getJSONArray("ret").isEmpty() ? new JSONObject() : tx.getJSONArray("ret").getJSONObject(0);
            boolean ok = ret.optString("contractRet", "SUCCESS").equals("SUCCESS");
            Accumulo a = per.computeIfAbsent(id, k -> new Accumulo());
            a.blocco = tx.optLong("blockNumber");
            a.timestamp = tx.optLong("block_timestamp") / 1000;
            boolean mio = firma.equals(w);
            if (mio) {
                firmate.add(id);
                a.commissione = a.commissione.add(Numero(ret, "fee"));
                if (!ok) a.riuscita = false;
            }
            a.tipi.add(tipo.endsWith("Contract") ? tipo.substring(0, tipo.length() - 8) : tipo);
            if (ok) {
                switch (tipo) {
                    case "TransferContract" -> {
                        String a2 = Base58(v.optString("to_address"));
                        if (mio) { a.Aggiungi(MONETA, Numero(v, "amount").negate()); a.Controparte(a2, w); }
                        if (a2.equals(w)) { a.Aggiungi(MONETA, Numero(v, "amount")); a.Controparte(firma, w); }
                    }
                    case "TransferAssetContract" -> {
                        String k = AssetTRC10(v.optString("asset_name"));
                        String a2 = Base58(v.optString("to_address"));
                        if (mio) { a.Aggiungi(k, Numero(v, "amount").negate()); a.Controparte(a2, w); }
                        if (a2.equals(w)) { a.Aggiungi(k, Numero(v, "amount")); a.Controparte(firma, w); }
                    }
                    case "TriggerSmartContract" -> {
                        if (mio) {
                            a.Aggiungi(MONETA, Numero(v, "call_value").negate());
                            a.Controparte(Base58(v.optString("contract_address")), w);
                        }
                    }
                    case "WithdrawBalanceContract" -> {
                        if (mio) a.ricompense = a.ricompense.add(Numero(tx, "withdraw_amount"));
                    }
                    default -> { }
                }
            }
            JSONArray it = tx.optJSONArray("internal_transactions");
            for (int i = 0; it != null && i < it.length(); i++) {
                JSONObject x = it.getJSONObject(i);
                JSONObject d = x.optJSONObject("data");
                if (d == null || d.optBoolean("rejected", false)) continue;
                JSONObject cv = d.optJSONObject("call_value");
                if (cv == null) continue;
                String da = Base58(x.optString("from_address")), a2 = Base58(x.optString("to_address"));
                for (String t : cv.keySet()) {
                    String k = t.equals("_") ? MONETA : AssetTRC10(t);
                    BigInteger q = cv.getBigInteger(t);
                    if (a2.equals(w)) a.Aggiungi(k, q);
                    if (da.equals(w)) a.Aggiungi(k, q.negate());
                }
            }
        }
        int inventati = 0;
        for (JSONObject x : t20) {
            if (!"Transfer".equals(x.optString("type"))) continue;
            String id = x.optString("transaction_id");
            JSONObject ti = x.optJSONObject("token_info");
            if (ti == null) continue;
            String k = ti.optString("address");
            BigInteger q = new BigInteger(x.optString("value", "0"));
            String da = x.optString("from"), a2 = x.optString("to");
            if (q.signum() == 0) continue;
            //Address poisoning: un token truffa "esce" dal wallet in una transazione che il wallet non ha firmato
            if (da.equals(w) && truffe.contains(k) && !firmate.contains(id)) { inventati++; continue; }
            Accumulo a = per.get(id);
            if (a == null) {
                a = new Accumulo();
                a.blocco = blocchi.getOrDefault(id, 0L);
                a.timestamp = x.optLong("block_timestamp") / 1000;
                a.tipi.add("TRC20");
                per.put(id, a);
            }
            if (da.equals(w)) { a.Aggiungi(k, q.negate()); a.Controparte(a2, w); }
            if (a2.equals(w)) { a.Aggiungi(k, q); a.Controparte(da, w); }
        }
        for (JSONObject x : interne) {
            String id = x.optString("hash");
            if (nativi.contains(id) || x.optBoolean("rejected", false) || x.optBoolean("revert", false)) continue;
            JSONObject tl = x.optJSONObject("token_list");
            if (tl == null) continue;
            String t = tl.optString("token_id", "_");
            String k = t.equals("_") ? MONETA : AssetTRC10(t);
            BigInteger q = Numero(tl, "call_value");
            String da = x.optString("from"), a2 = x.optString("to");
            Accumulo a = per.get(id);
            if (a == null) {
                a = new Accumulo();
                a.blocco = x.optLong("block");
                a.timestamp = x.optLong("timestamp") / 1000;
                per.put(id, a);
            }
            a.tipi.add("Interna");
            if (a2.equals(w)) { a.Aggiungi(k, q); a.Controparte(da, w); }
            if (da.equals(w)) { a.Aggiungi(k, q.negate()); a.Controparte(a2, w); }
        }
        //Solo nel log: sui wallet USDT attivi l'address poisoning e' continuo, una finestra a ogni scaricamento non servirebbe
        if (inventati > 0) System.out.println("Tron " + w + ": scartati " + inventati + " trasferimenti di token truffa che il wallet non ha firmato.");
        List<Operazione> ops = new ArrayList<>();
        int senzaBlocco = 0;
        for (Map.Entry<String, Accumulo> e : per.entrySet()) {
            Accumulo a = e.getValue();
            a.variazioni.values().removeIf(x -> x.signum() == 0);
            if (a.variazioni.isEmpty() && a.commissione.signum() == 0 && a.ricompense.signum() == 0) continue;
            if (a.blocco <= 0) senzaBlocco++;
            ops.add(new Operazione(e.getKey(), a.blocco, a.timestamp, String.join(", ", a.tipi), a.controparte, a.riuscita,
                    a.variazioni, a.commissione, a.ricompense));
        }
        if (senzaBlocco > 0) avvisi.add(senzaBlocco + " operazioni senza numero di blocco.");
        ops.sort(Comparator.comparingLong(Operazione::blocco).thenComparingLong(Operazione::timestamp));
        return ops;
    }

    /** Le operazioni dal blocco {@code bloccoDa} in poi: la ripresa scarica anche il blocco già importato. */
    static List<Operazione> DalBlocco(List<Operazione> operazioni, long bloccoDa) {
        List<Operazione> l = new ArrayList<>(operazioni);
        if (bloccoDa > 1) l.removeIf(o -> o.blocco() > 0 && o.blocco() < bloccoDa);
        return l;
    }

    private static BigInteger Numero(JSONObject o, String campo) {
        if (o == null || !o.has(campo)) return BigInteger.ZERO;
        Object v = o.get(campo);
        return v instanceof Number n ? new BigInteger(n.toString()) : new BigInteger(o.optString(campo, "0"));
    }

    /** La chiave di un TRC10: l'id; i TRC10 antichi avevano il nome in esadecimale al posto dell'id. */
    static String AssetTRC10(String nome) {
        if (nome.matches("\\d+")) return nome;
        try {
            return new String(new BigInteger(nome, 16).toByteArray(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (NumberFormatException e) {
            return nome;
        }
    }

    /**
     * I TRX posseduti: liberi, congelati (stake 1.0 e 2.0), in scongelamento, delegati ad altri. Le ricompense non
     * ancora ritirate ({@code allowance}) non ci sono.
     */
    static BigInteger Posseduti(JSONObject c) {
        BigInteger t = Numero(c, "balance");
        JSONArray f2 = c.optJSONArray("frozenV2");
        for (int i = 0; f2 != null && i < f2.length(); i++) t = t.add(Numero(f2.getJSONObject(i), "amount"));
        JSONArray u2 = c.optJSONArray("unfrozenV2");
        for (int i = 0; u2 != null && i < u2.length(); i++) t = t.add(Numero(u2.getJSONObject(i), "unfreeze_amount"));
        JSONArray f1 = c.optJSONArray("frozen");
        for (int i = 0; f1 != null && i < f1.length(); i++) t = t.add(Numero(f1.getJSONObject(i), "frozen_balance"));
        JSONObject r = c.optJSONObject("account_resource");
        if (r != null) {
            t = t.add(Numero(r.optJSONObject("frozen_balance_for_energy"), "frozen_balance"));
            t = t.add(Numero(r, "delegated_frozenV2_balance_for_energy")).add(Numero(r, "delegated_frozen_balance_for_energy"));
        }
        return t.add(Numero(c, "delegated_frozenV2_balance_for_bandwidth")).add(Numero(c, "delegated_frozen_balance_for_bandwidth"));
    }

    /**
     * Confronta la somma con il conto: TRX posseduti, TRC10, TRC20 escluse le truffe. Un token che toglie quantità
     * senza evento (commissione sul trasferimento) compare qui.
     *
     * @param conto il conto di {@code /v1/accounts/{a}} ({@code data[0]})
     */
    static List<String> VerificaSaldi(List<Operazione> operazioni, JSONObject conto, Map<String, Token> token) {
        Map<String, BigInteger> somma = new TreeMap<>();
        for (Operazione o : operazioni) {
            o.variazioni().forEach((k, v) -> somma.merge(k, v, BigInteger::add));
            somma.merge(MONETA, o.ricompense().subtract(o.commissione()), BigInteger::add);
        }
        Map<String, BigInteger> saldo = new TreeMap<>();
        saldo.put(MONETA, Posseduti(conto));
        JSONArray a10 = conto.optJSONArray("assetV2");
        for (int i = 0; a10 != null && i < a10.length(); i++) {
            saldo.put(a10.getJSONObject(i).optString("key"), Numero(a10.getJSONObject(i), "value"));
        }
        JSONArray a20 = conto.optJSONArray("trc20");
        for (int i = 0; a20 != null && i < a20.length(); i++) {
            JSONObject x = a20.getJSONObject(i);
            for (String k : x.keySet()) saldo.put(k, new BigInteger(x.getString(k)));
        }
        List<String> avvisi = new ArrayList<>();
        Set<String> chiavi = new java.util.TreeSet<>(somma.keySet());
        chiavi.addAll(saldo.keySet());
        for (String k : chiavi) {
            Token t = token.get(k);
            //Token mai mossi nell'intervallo (spam con saldo finto nell'elenco del conto) e truffe: fuori dal confronto
            if (!k.equals(MONETA) && (t == null || t.truffa())) continue;
            BigInteger calcolato = somma.getOrDefault(k, BigInteger.ZERO);
            BigInteger reale = saldo.getOrDefault(k, BigInteger.ZERO);
            if (calcolato.equals(reale)) continue;
            int decimali = k.equals(MONETA) ? DECIMALI_TRX : t.decimali();
            String nome = k.equals(MONETA) ? MONETA : t.simbolo() + " (" + k + ")";
            avvisi.add(nome + ": i movimenti importati danno " + Decimale(calcolato, decimali).toPlainString()
                    + ", il saldo sulla blockchain è " + Decimale(reale, decimali).toPlainString() + ".");
        }
        return avvisi;
    }

    static BigDecimal Decimale(BigInteger unita, int decimali) {
        return new BigDecimal(unita).movePointLeft(decimali).stripTrailingZeros();
    }

    //=====================================================================================================
    //=== SIMBOLI
    //=====================================================================================================

    /**
     * Il simbolo di ogni token. I token che TronScan dà come truffa hanno il marcatore SCAM {@code " **"}; fra gli
     * altri, il simbolo libero va ai token verificati da TronScan (livello 1 o 2) prima che agli altri, e chi trova il
     * simbolo già preso (in questa importazione o nell'archivio, {@code giaUsati}) ha l'indirizzo fra parentesi: il
     * calcolo LIFO identifica le monete per simbolo.
     */
    static Map<String, Token> Simboli(List<Operazione> operazioni, Map<String, Token> token, Map<String, String> livelli,
            Map<String, String> giaUsati) {
        List<String> ordine = new ArrayList<>();
        for (Operazione o : operazioni) for (String k : o.variazioni().keySet()) if (token.containsKey(k) && !ordine.contains(k)) ordine.add(k);
        List<String> prima = new ArrayList<>(), dopo = new ArrayList<>();
        for (String k : ordine) {
            String l = livelli.getOrDefault(k, "");
            (l.equals("1") || l.equals("2") ? prima : dopo).add(k);
        }
        prima.addAll(dopo);
        Map<String, String> usati = new HashMap<>();
        giaUsati.forEach((s, k) -> usati.put(s.toUpperCase(), k));
        Map<String, Token> out = new LinkedHashMap<>();
        for (String k : prima) {
            Token t = token.get(k);
            String base = t.simbolo().replace(";", "").replace(",", "").trim();
            if (base.isEmpty()) base = isIdTRC10(k) ? "TRC10" : "TRC20";
            String s;
            if (t.truffa()) s = base + " (" + k + ") **";
            else {
                if (base.equalsIgnoreCase(MONETA)) base = base + " (" + k + ")";
                String gia = usati.get(base.toUpperCase());
                s = gia == null || gia.equals(k) ? base : base + " (" + k + ")";
                usati.putIfAbsent(s.toUpperCase(), k);
            }
            out.put(k, new Token(k, s, t.nome().isBlank() ? base : t.nome(), t.decimali(), t.truffa()));
        }
        return out;
    }

    /** Simbolo → chiave dei token Tron già nei movimenti ({@code [8]}/{@code [26]} e {@code [11]}/{@code [28]}, rete {@code [34]}). */
    static Map<String, String> SimboliGiaUsati(Map<String, String[]> archivio) {
        Map<String, String> m = new HashMap<>();
        if (archivio == null) return m;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 34 || !RETE.equals(v[34])) continue;
            if (v[26] != null && (isIndirizzo(v[26]) || isIdTRC10(v[26])) && v[8] != null) m.putIfAbsent(v[8], v[26]);
            if (v[28] != null && (isIndirizzo(v[28]) || isIdTRC10(v[28])) && v[11] != null) m.putIfAbsent(v[11], v[28]);
        }
        return m;
    }

    //=====================================================================================================
    //=== MOVIMENTI
    //=====================================================================================================

    static Map<String, TransazioneDefi> ComeTransazioniDefi(String wallet, Esito esito) {
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliTron(esito.operazioni(), esito.token()), "import TRX", null);
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Operazione o : esito.operazioni()) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = wallet;
            td.HashTransazione = o.hash();
            td.Blocco = String.valueOf(o.blocco());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
            td.RighePronte = () -> Righe(wallet, o, esito.token());
            mappa.put(wallet + "." + o.hash(), td);
        }
        return mappa;
    }

    /** Le righe di un'operazione; le ricompense di voto sono una riga STAKING REWARD a parte. */
    static List<String[]> Righe(String wallet, Operazione o, Map<String, Token> token) {
        List<String[]> righe = new ArrayList<>();
        if (!o.variazioni().isEmpty() || o.commissione().signum() != 0) {
            TransazioneDefi t = new TransazioneDefi();
            t.Rete = RETE;
            t.Wallet = wallet;
            t.HashTransazione = o.hash();
            t.Blocco = String.valueOf(o.blocco());
            t.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
            t.TimeStamp = String.valueOf(o.timestamp());
            t.TipoTransazione = o.tipo().isEmpty() ? "Tron" : o.tipo();
            t.TransazioneOK = o.riuscita();
            t.MonetaCommissioni = MONETA;
            if (o.commissione().signum() != 0) t.QtaCommissioni = "-" + Decimale(o.commissione(), DECIMALI_TRX).toPlainString();
            for (Map.Entry<String, BigInteger> v : o.variazioni().entrySet()) {
                if (v.getKey().equals(MONETA)) {
                    t.InserisciMonete(MONETA, "TRON", MONETA, o.controparte(), Decimale(v.getValue(), DECIMALI_TRX).toPlainString(), "Crypto");
                } else {
                    Token tk = token.get(v.getKey());
                    if (tk == null) tk = new Token(v.getKey(), "TRC20 (" + v.getKey() + ")", "", 0, false);
                    t.InserisciMonete(tk.simbolo(), tk.nome(), v.getKey(), o.controparte(),
                            Decimale(v.getValue(), tk.decimali()).toPlainString(), "Crypto");
                }
            }
            righe.addAll(t.RitornaRigheTabella());
        }
        if (o.ricompense().signum() != 0) {
            Moneta m = new Moneta();
            m.InserisciValori(MONETA, Decimale(o.ricompense(), DECIMALI_TRX).toPlainString(), MONETA, "Crypto");
            m.SetNomeEsteso("TRON");
            m.SetRete(RETE);
            String[] rt = MovimentiCrypto.creaMovimento(null, m, wallet + " (" + RETE + ")", "Wallet", o.timestamp() * 1000,
                    null, null, 1, 1, null, null, "A", o.hash(), "STAKING REWARD", null);
            if (rt != null) {
                rt[7] = CAUSALE_RICOMPENSE;
                rt[23] = String.valueOf(o.blocco());
                Importazioni.RiempiVuotiArray(rt);
                righe.add(rt);
            }
        }
        return righe;
    }

    //=====================================================================================================
    //=== INDIRIZZI
    //=====================================================================================================

    /** Indirizzo Tron: Base58Check di 21 byte che iniziano con 0x41 ({@code T...}). Vale per conti e contratti. */
    static boolean isIndirizzo(String s) {
        if (s == null || s.length() != 34 || !s.startsWith("T")) return false;
        try {
            byte[] b = IndirizziBitcoin.DecodificaBase58Verificata(s);
            return b.length == 21 && b[0] == 0x41;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Id di un token TRC10 (sette cifre, dal 1000001). */
    static boolean isIdTRC10(String s) {
        return s != null && s.matches("1\\d{6}");
    }

    /** {@code 41...} esadecimale → {@code T...}; una stringa che non è esadecimale resta com'è. */
    static String Base58(String hex) {
        if (hex == null || !hex.matches("41[0-9a-fA-F]{40}")) return hex == null ? "" : hex;
        byte[] b = new BigInteger("01" + hex, 16).toByteArray();
        return IndirizziBitcoin.CodificaBase58Verificata(Arrays.copyOfRange(b, b.length - 21, b.length));
    }

    //=====================================================================================================
    //=== RETE
    //=====================================================================================================

    private static String Get(String url, BooleanSupplier interrotto) throws IOException, InterruptedException {
        return Chiama(new Request.Builder().url(url).header("Accept", "application/json").get().build(), interrotto);
    }

    private static String Post(String url, JSONObject corpo, BooleanSupplier interrotto) throws IOException, InterruptedException {
        return Chiama(new Request.Builder().url(url).post(RequestBody.create(corpo.toString(), JSON)).build(), interrotto);
    }

    /** Ripetuta su 429 (TronGrid senza chiave regge poche richieste al secondo), 5xx ed errori di connessione. */
    private static String Chiama(Request r, BooleanSupplier interrotto) throws IOException, InterruptedException {
        if (Funzioni.ReteDisabilitataPerTest) throw new IOException("rete disabilitata nei test");
        IOException ultimo = null;
        for (int tentativo = 1; tentativo <= TENTATIVI; tentativo++) {
            if (Interrotto(interrotto)) throw new InterruptedException("interrotto");
            if (r.url().host().contains("trongrid")) Ritmo();
            int codice;
            String testo;
            try (Response risposta = HTTP.newCall(r).execute()) {
                codice = risposta.code();
                testo = risposta.body() == null ? "" : risposta.body().string();
            } catch (IOException e) {
                ultimo = e;
                Thread.sleep(3000L * tentativo);
                continue;
            }
            if (codice >= 200 && codice < 300) return testo;
            ultimo = new IOException("HTTP " + codice + " da " + r.url().host() + ": " + testo.substring(0, Math.min(200, testo.length())));
            if (codice != 429 && codice < 500) throw ultimo;
            System.out.println("Tron: " + ultimo.getMessage() + ", riprovo.");
            Thread.sleep(3000L * tentativo);
        }
        throw ultimo;
    }

    private static synchronized void Ritmo() throws InterruptedException {
        long attesa = ultimaTronGrid + PAUSA_TRONGRID_MS - System.currentTimeMillis();
        if (attesa > 0) Thread.sleep(attesa);
        ultimaTronGrid = System.currentTimeMillis();
    }

    private static boolean Interrotto(BooleanSupplier interrotto) {
        return interrotto != null && interrotto.getAsBoolean();
    }

    private static void Stato(Consumer<String> avanzamento, String testo) {
        if (avanzamento != null) avanzamento.accept(testo);
    }
}
