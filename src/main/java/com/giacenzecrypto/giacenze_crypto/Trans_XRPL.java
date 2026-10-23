package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Importazione di un wallet XRP Ledger da un nodo pubblico con la storia completa (Ripple, senza chiave: indirizzo in
 * {@code config/varie/IndirizziServizi.json}, voce {@code xrplNodo}).
 *
 * <p>Le variazioni del wallet non si ricavano dal tipo di transazione ma dai <b>metadati</b>
 * ({@code meta.AffectedNodes}), cioè da come la transazione ha cambiato il ledger: saldo XRP del conto
 * ({@code AccountRoot}), saldi delle linee di fiducia ({@code RippleState}, il saldo è dal punto di vista del conto
 * "basso" e si gira per quello "alto") e NFT nelle pagine del conto ({@code NFTokenPage}, il cui indice comincia con
 * l'id del proprietario). Così pagamenti parziali (si conta il consegnato, non l'importo dichiarato), ordini sul DEX
 * eseguiti da altri, AMM, assegni, escrow e transazioni {@code Batch} sono già giusti senza interpretarli uno per
 * uno. Verificato il 23/10/2026 su quattro wallet pubblici con 144-2.281 transazioni: saldo XRP, ogni linea di
 * fiducia e l'elenco degli NFT coincidono con quelli del conto.
 *
 * <p>Ogni transazione è un'operazione; la commissione la paga chi firma ({@code tx.Account}) ed è già dentro la
 * variazione del saldo XRP, quindi si toglie da lì. Una transazione fallita ({@code tec...}) è nel ledger e costa
 * solo la commissione.
 *
 * <p>I token (IOU) sono {@code VALUTA.emittente}, lo stesso formato di CoinGecko (piattaforma {@code xrp}) e
 * DefiLlama ({@code ripple:}). Lo stesso codice lo emette chiunque, e i falsi sono comuni: il calcolo LIFO identifica
 * le monete per simbolo, quindi il primo emittente incontrato tiene il codice e gli altri hanno l'emittente accanto
 * ({@link #Simboli}). I token LP delle AMM prendono il nome della coppia.
 *
 * <p>Dettagli e indirizzi di prova: {@code nocommit/Documentazione/Analisi_Import_XRP_ALGO_TRX.md}.
 */
final class Trans_XRPL {

    private Trans_XRPL() {}

    static final String RETE = "XRP";
    static final String MONETA = "XRP";
    static final String PROVIDER = "NODO XRPL";
    static final String NFT = "NFT";
    /** Primo ledger della storia pubblica (i precedenti sono andati persi nel 2013). */
    static final long PRIMO_LEDGER = 32570;
    /** Secondi fra l'epoca Unix e quella di Ripple (1/1/2000). */
    static final long EPOCA_RIPPLE = 946684800L;

    private static final int PAGINA = 400;
    private static final int TENTATIVI = 6;
    private static final BigDecimal DROPS = BigDecimal.valueOf(1_000_000);
    private static final String ALFABETO = "rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz";
    private static final MediaType JSON = MediaType.get("application/json");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    /**
     * Una transazione ridotta all'effetto sul wallet.
     *
     * @param variazioni chiave {@link #MONETA}, {@code VALUTA.emittente} o {@code NFT:<id>}, valore con segno (XRP in
     *                   XRP, commissione esclusa; un NFT vale 1), mai zero
     * @param commissione XRP pagati dal wallet per la transazione
     */
    record Operazione(String hash, long ledger, long timestamp, String tipo, String controparte, boolean riuscita,
            Map<String, BigDecimal> variazioni, BigDecimal commissione) {}

    /** @param simboli chiave del token → simbolo con cui compare nei movimenti */
    record Esito(List<Operazione> operazioni, Map<String, String> simboli, List<String> avvisi, boolean completo) {}

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /**
     * Scarica le transazioni del wallet dal ledger {@code ledgerDa} (incluso) all'ultimo validato. In una prima
     * importazione legge anche linee di fiducia e NFT allo stesso ledger, per confrontarli con la somma
     * ({@link #VerificaSaldi}).
     *
     * @param interrotto vero quando l'utente ha chiesto di fermarsi; può essere {@code null}
     * @param avanzamento riceve la riga di stato; può essere {@code null}
     * @return {@code completo} falso se lo scaricamento non è arrivato in fondo: allora non si importa nulla
     */
    static Esito Scarica(String wallet, long ledgerDa, BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        String nodo = IndirizziServizi.Url(IndirizziServizi.XRPL_NODO);
        try {
            Stato(avanzamento, "XRP Ledger: lettura del conto " + wallet + "...");
            JSONObject conto = Chiama(nodo, "account_info", new JSONObject().put("account", wallet)
                    .put("ledger_index", "validated"), interrotto);
            if ("actNotFound".equals(conto.optString("error"))) {
                System.out.println("XRP Ledger " + wallet + ": conto non attivato.");
                return new Esito(List.of(), Map.of(), avvisi, true);
            }
            Errore(conto);
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "XRPL", "account_info " + wallet, conto.toString());
            long ledgerA = conto.getLong("ledger_index");
            long da = Math.max(ledgerDa, 1);

            List<JSONObject> voci = new ArrayList<>();
            Object marker = null;
            int pagina = 0;
            do {
                if (Interrotto(interrotto)) return new Esito(List.of(), Map.of(), avvisi, false);
                pagina++;
                Stato(avanzamento, "XRP Ledger: pagina " + pagina + " delle transazioni (" + voci.size() + " lette)");
                JSONObject p = new JSONObject().put("account", wallet).put("ledger_index_min", da)
                        .put("ledger_index_max", ledgerA).put("forward", true).put("limit", PAGINA).put("api_version", 1);
                if (marker != null) p.put("marker", marker);
                JSONObject r = Chiama(nodo, "account_tx", p, interrotto);
                Errore(r);
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "XRPL", "account_tx " + wallet + " pagina " + pagina, r.toString());
                if (pagina == 1 && r.optLong("ledger_index_min", da) > Math.max(da, PRIMO_LEDGER)) {
                    //Un nodo senza tutta la storia risponde da dove ce l'ha: meglio niente che una storia a meta'
                    avvisi.add("Il nodo " + nodo + " ha la storia solo dal ledger " + r.optLong("ledger_index_min")
                            + ", ne serve dal " + da + ".");
                    return new Esito(List.of(), Map.of(), avvisi, false);
                }
                JSONArray tx = r.optJSONArray("transactions");
                for (int i = 0; tx != null && i < tx.length(); i++) voci.add(tx.getJSONObject(i));
                marker = r.opt("marker");
            } while (marker != null);
            System.out.println("XRP Ledger " + wallet + ": " + voci.size() + " transazioni dal ledger " + da + " al " + ledgerA + ".");

            List<Operazione> operazioni = Analizza(voci, wallet, avvisi);

            Map<String, String> nomiLP = new HashMap<>();
            for (Operazione o : operazioni) {
                for (String k : o.variazioni().keySet()) {
                    if (!isChiaveToken(k) || !Valuta(k).startsWith("03") || nomiLP.containsKey(Emittente(k))) continue;
                    if (Interrotto(interrotto)) return new Esito(List.of(), Map.of(), avvisi, false);
                    Stato(avanzamento, "XRP Ledger: coppia della AMM " + Emittente(k));
                    nomiLP.put(Emittente(k), NomeLP(nodo, Emittente(k), ledgerA, interrotto));
                }
            }
            Map<String, String> simboli = Simboli(operazioni, nomiLP, SimboliGiaUsati(Principale.MappaCryptoWallet));

            if (da <= PRIMO_LEDGER) {
                Stato(avanzamento, "XRP Ledger: confronto con i saldi del conto");
                JSONArray linee = Pagine(nodo, "account_lines", new JSONObject().put("account", wallet).put("ledger_index", ledgerA),
                        "lines", interrotto, idDocumento);
                JSONArray nft = Pagine(nodo, "account_nfts", new JSONObject().put("account", wallet).put("ledger_index", ledgerA),
                        "account_nfts", interrotto, idDocumento);
                avvisi.addAll(VerificaSaldi(operazioni, conto.getJSONObject("account_data"), linee, nft, simboli));
            }
            System.out.println("XRP Ledger " + wallet + ": " + operazioni.size() + " operazioni con effetto sul wallet.");
            return new Esito(operazioni, simboli, avvisi, true);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            avvisi.add("Nodo XRP Ledger: " + ex.getMessage());
            return new Esito(List.of(), Map.of(), avvisi, false);
        }
    }

    private static JSONArray Pagine(String nodo, String metodo, JSONObject parametri, String campo, BooleanSupplier interrotto,
            int idDocumento) throws IOException, InterruptedException {
        JSONArray tutto = new JSONArray();
        Object marker = null;
        do {
            JSONObject p = new JSONObject(parametri.toString()).put("limit", PAGINA);
            if (marker != null) p.put("marker", marker);
            JSONObject r = Chiama(nodo, metodo, p, interrotto);
            Errore(r);
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "XRPL", metodo + " " + parametri.optString("account"), r.toString());
            JSONArray a = r.optJSONArray(campo);
            for (int i = 0; a != null && i < a.length(); i++) tutto.put(a.get(i));
            marker = r.opt("marker");
        } while (marker != null);
        return tutto;
    }

    /** Il nome di un token LP: le due monete della AMM, {@code "LP XRP/SOLO"}; se la AMM non c'è più, il suo conto. */
    private static String NomeLP(String nodo, String conto, long ledger, BooleanSupplier interrotto) throws IOException, InterruptedException {
        JSONObject r = Chiama(nodo, "amm_info", new JSONObject().put("amm_account", conto).put("ledger_index", ledger), interrotto);
        JSONObject amm = r.optJSONObject("amm");
        if (amm == null) return "LP " + conto;
        return "LP " + CodiceImporto(amm.opt("amount")) + "/" + CodiceImporto(amm.opt("amount2"));
    }

    private static String CodiceImporto(Object importo) {
        return importo instanceof JSONObject o ? Codice(o.optString("currency")) : MONETA;
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================

    /**
     * Riduce le voci di {@code account_tx} (formato {@code api_version} 1: {@code tx} e {@code meta}) alle operazioni del
     * wallet, in ordine di ledger e di posizione nel ledger.
     *
     * @param avvisi riceve le cose che l'importazione non sa trattare (token MPT)
     */
    static List<Operazione> Analizza(List<JSONObject> voci, String wallet, List<String> avvisi) {
        List<JSONObject> ordinate = new ArrayList<>(voci);
        ordinate.sort(Comparator.comparingLong((JSONObject v) -> Tx(v).optLong("ledger_index", 0))
                .thenComparingLong(v -> v.optJSONObject("meta") == null ? 0 : v.getJSONObject("meta").optLong("TransactionIndex", 0)));
        String proprietario = IdConto(wallet);
        List<Operazione> operazioni = new ArrayList<>();
        Set<String> viste = new HashSet<>();
        boolean mpt = false;
        for (JSONObject v : ordinate) {
            JSONObject tx = Tx(v);
            JSONObject meta = v.optJSONObject("meta");
            if (meta == null || !viste.add(tx.optString("hash"))) continue;
            Map<String, BigDecimal> var = new LinkedHashMap<>();
            Set<String> nftPrima = new HashSet<>(), nftDopo = new HashSet<>();
            JSONArray nodi = meta.optJSONArray("AffectedNodes");
            for (int i = 0; nodi != null && i < nodi.length(); i++) {
                JSONObject contenitore = nodi.getJSONObject(i);
                String tipo = contenitore.keys().next();
                JSONObject n = contenitore.getJSONObject(tipo);
                JSONObject fin = n.optJSONObject("FinalFields");
                if (fin == null) fin = n.optJSONObject("NewFields");
                if (fin == null) fin = new JSONObject();
                JSONObject prev = n.optJSONObject("PreviousFields");
                if (prev == null) prev = new JSONObject();
                switch (n.optString("LedgerEntryType")) {
                    case "AccountRoot" -> {
                        if (!wallet.equals(fin.optString("Account"))) break;
                        if (tipo.equals("CreatedNode")) Somma(var, MONETA, Drops(fin.optString("Balance", "0")));
                        else if (prev.has("Balance")) {
                            Somma(var, MONETA, Drops(fin.getString("Balance")).subtract(Drops(prev.getString("Balance"))));
                        }
                    }
                    case "RippleState" -> {
                        String basso = fin.getJSONObject("LowLimit").getString("issuer");
                        String alto = fin.getJSONObject("HighLimit").getString("issuer");
                        if (!wallet.equals(basso) && !wallet.equals(alto)) break;
                        //Il saldo e' dal punto di vista del conto "basso": per quello "alto" si gira
                        BigDecimal segno = wallet.equals(basso) ? BigDecimal.ONE : BigDecimal.ONE.negate();
                        String chiave = fin.getJSONObject("Balance").getString("currency") + "." + (wallet.equals(basso) ? alto : basso);
                        BigDecimal dopo = new BigDecimal(fin.getJSONObject("Balance").getString("value"));
                        if (tipo.equals("CreatedNode")) Somma(var, chiave, dopo.multiply(segno));
                        else if (prev.has("Balance")) {
                            BigDecimal prima = new BigDecimal(prev.getJSONObject("Balance").getString("value"));
                            Somma(var, chiave, dopo.subtract(prima).multiply(segno));
                        }
                    }
                    case "NFTokenPage" -> {
                        //L'indice della pagina comincia con l'id del conto proprietario
                        if (!n.optString("LedgerIndex").toUpperCase().startsWith(proprietario)) break;
                        if (tipo.equals("CreatedNode")) nftDopo.addAll(IdNFT(fin.optJSONArray("NFTokens")));
                        else {
                            JSONArray primaDi = prev.has("NFTokens") ? prev.optJSONArray("NFTokens") : fin.optJSONArray("NFTokens");
                            nftPrima.addAll(IdNFT(primaDi));
                            if (!tipo.equals("DeletedNode")) nftDopo.addAll(IdNFT(fin.optJSONArray("NFTokens")));
                        }
                    }
                    case "MPToken" -> mpt |= wallet.equals(fin.optString("Account"));
                    default -> { }
                }
            }
            for (String id : nftDopo) if (!nftPrima.contains(id)) var.put(NFT + ":" + id, BigDecimal.ONE);
            for (String id : nftPrima) if (!nftDopo.contains(id)) var.put(NFT + ":" + id, BigDecimal.ONE.negate());

            boolean daWallet = wallet.equals(tx.optString("Account"));
            BigDecimal commissione = daWallet ? Drops(tx.optString("Fee", "0")) : BigDecimal.ZERO;
            //La commissione e' gia' nella variazione del saldo XRP: si toglie da li'
            if (daWallet) Somma(var, MONETA, commissione);
            var.values().removeIf(x -> x.signum() == 0);
            if (var.isEmpty() && commissione.signum() == 0) continue;
            String esito = meta.optString("TransactionResult");
            operazioni.add(new Operazione(tx.optString("hash"), tx.optLong("ledger_index"), tx.optLong("date") + EPOCA_RIPPLE,
                    tx.optString("TransactionType"), Controparte(tx, wallet), esito.startsWith("tes"), var, commissione));
        }
        if (mpt) avvisi.add("Il wallet ha movimenti di token MPT, che questa versione non importa.");
        return operazioni;
    }

    private static JSONObject Tx(JSONObject voce) {
        JSONObject t = voce.optJSONObject("tx");
        if (t == null) t = voce.optJSONObject("tx_json");
        return t == null ? new JSONObject() : t;
    }

    private static void Somma(Map<String, BigDecimal> m, String chiave, BigDecimal x) {
        m.merge(chiave, x, BigDecimal::add);
    }

    private static BigDecimal Drops(String drops) {
        return new BigDecimal(drops).divide(DROPS);
    }

    private static Set<String> IdNFT(JSONArray l) {
        Set<String> s = new HashSet<>();
        for (int i = 0; l != null && i < l.length(); i++) {
            JSONObject t = l.getJSONObject(i).optJSONObject("NFToken");
            if (t != null) s.add(t.getString("NFTokenID").toUpperCase());
        }
        return s;
    }

    /** L'altro conto della transazione: il destinatario se firma il wallet, altrimenti chi firma. */
    private static String Controparte(JSONObject tx, String wallet) {
        String firma = tx.optString("Account");
        if (!wallet.equals(firma)) return firma;
        String dest = tx.optString("Destination", "");
        return dest.equals(wallet) ? "" : dest;
    }

    /**
     * Confronta la somma delle operazioni con il conto letto allo stesso ledger: saldo XRP, ogni linea di fiducia, gli
     * NFT posseduti. Ha senso solo su tutta la storia.
     *
     * @param conto {@code account_data} di {@code account_info}
     * @param linee {@code lines} di {@code account_lines}
     * @param nft {@code account_nfts}
     */
    static List<String> VerificaSaldi(List<Operazione> operazioni, JSONObject conto, JSONArray linee, JSONArray nft,
            Map<String, String> simboli) {
        Map<String, BigDecimal> somma = new TreeMap<>();
        Set<String> nftCalcolati = new HashSet<>();
        for (Operazione o : operazioni) {
            for (Map.Entry<String, BigDecimal> e : o.variazioni().entrySet()) {
                if (e.getKey().startsWith(NFT + ":")) {
                    if (e.getValue().signum() > 0) nftCalcolati.add(e.getKey().substring(4));
                    else nftCalcolati.remove(e.getKey().substring(4));
                } else somma.merge(e.getKey(), e.getValue(), BigDecimal::add);
            }
            somma.merge(MONETA, o.commissione().negate(), BigDecimal::add);
        }
        Map<String, BigDecimal> saldo = new TreeMap<>();
        saldo.put(MONETA, Drops(conto.optString("Balance", "0")));
        for (int i = 0; linee != null && i < linee.length(); i++) {
            JSONObject l = linee.getJSONObject(i);
            saldo.put(l.getString("currency") + "." + l.getString("account"), new BigDecimal(l.getString("balance")));
        }
        List<String> avvisi = new ArrayList<>();
        Set<String> chiavi = new java.util.TreeSet<>(somma.keySet());
        chiavi.addAll(saldo.keySet());
        for (String k : chiavi) {
            BigDecimal calcolato = somma.getOrDefault(k, BigDecimal.ZERO);
            BigDecimal reale = saldo.getOrDefault(k, BigDecimal.ZERO);
            if (calcolato.compareTo(reale) == 0) continue;
            String nome = k.equals(MONETA) ? MONETA : simboli.getOrDefault(k, Codice(Valuta(k))) + " (" + Emittente(k) + ")";
            avvisi.add(nome + ": i movimenti importati danno " + calcolato.stripTrailingZeros().toPlainString()
                    + ", il saldo sulla blockchain è " + reale.stripTrailingZeros().toPlainString() + ".");
        }
        Set<String> nftReali = new HashSet<>();
        for (int i = 0; nft != null && i < nft.length(); i++) nftReali.add(nft.getJSONObject(i).getString("NFTokenID").toUpperCase());
        if (!nftReali.equals(nftCalcolati)) {
            avvisi.add("NFT: i movimenti importati ne danno " + nftCalcolati.size() + ", sulla blockchain il wallet ne ha "
                    + nftReali.size() + (nftReali.size() == nftCalcolati.size() ? " (diversi)." : "."));
        }
        return avvisi;
    }

    //=====================================================================================================
    //=== TOKEN E SIMBOLI
    //=====================================================================================================

    static boolean isChiaveToken(String chiave) {
        int p = chiave.indexOf('.');
        return p > 0 && !chiave.startsWith(NFT + ":");
    }

    static String Valuta(String chiave) {
        return chiave.substring(0, chiave.indexOf('.'));
    }

    static String Emittente(String chiave) {
        return chiave.substring(chiave.indexOf('.') + 1);
    }

    /**
     * Il codice leggibile di una valuta: quello di 3 caratteri così com'è, quello di 40 cifre esadecimali convertito in
     * testo se è testo (zeri finali tolti), altrimenti l'esadecimale. I codici che iniziano con {@code 03} sono token LP.
     */
    static String Codice(String valuta) {
        if (valuta.length() != 40 || !valuta.matches("[0-9A-Fa-f]{40}")) return valuta;
        byte[] b = new BigInteger("01" + valuta, 16).toByteArray();
        b = Arrays.copyOfRange(b, b.length - 20, b.length);
        int fine = b.length;
        while (fine > 0 && b[fine - 1] == 0) fine--;
        if (fine == 0) return valuta;
        for (int i = 0; i < fine; i++) {
            if (b[i] < 0x20 || b[i] > 0x7e) return valuta;
        }
        String s = new String(b, 0, fine, StandardCharsets.US_ASCII).replace(";", "").replace(",", "").trim();
        return s.isEmpty() ? valuta : s;
    }

    /**
     * I simboli dei token delle operazioni, in ordine di comparsa. Un simbolo già usato da un token diverso (in questa
     * importazione o in {@code giaUsati}, simbolo → chiave dei movimenti già in archivio) prende l'emittente fra
     * parentesi: il LIFO identifica le monete per simbolo, e un falso con lo stesso codice non deve mescolarsi.
     *
     * @param nomiLP conto della AMM → nome del suo token LP
     */
    static Map<String, String> Simboli(List<Operazione> operazioni, Map<String, String> nomiLP, Map<String, String> giaUsati) {
        Map<String, String> usati = new HashMap<>();
        giaUsati.forEach((s, k) -> usati.put(s.toUpperCase(), k));
        Map<String, String> simboli = new LinkedHashMap<>();
        for (Operazione o : operazioni) {
            for (String k : o.variazioni().keySet()) {
                if (!isChiaveToken(k) || simboli.containsKey(k)) continue;
                String base = Valuta(k).startsWith("03") ? nomiLP.getOrDefault(Emittente(k), "LP " + Emittente(k)) : Codice(Valuta(k));
                if (base.equalsIgnoreCase(MONETA)) base = base + " (" + Emittente(k) + ")";
                String gia = usati.get(base.toUpperCase());
                String s = gia == null || gia.equals(k) ? base : base + " (" + Emittente(k) + ")";
                usati.putIfAbsent(s.toUpperCase(), k);
                simboli.put(k, s);
            }
        }
        return simboli;
    }

    /** Simbolo → chiave dei token XRP Ledger già nei movimenti ({@code [8]}/{@code [26]} e {@code [11]}/{@code [28]}). */
    static Map<String, String> SimboliGiaUsati(Map<String, String[]> archivio) {
        Map<String, String> m = new HashMap<>();
        if (archivio == null) return m;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 34 || !RETE.equals(v[34])) continue;
            if (v[26] != null && isChiaveToken(v[26]) && v[8] != null) m.putIfAbsent(v[8], v[26]);
            if (v[28] != null && isChiaveToken(v[28]) && v[11] != null) m.putIfAbsent(v[11], v[28]);
        }
        return m;
    }

    //=====================================================================================================
    //=== MOVIMENTI
    //=====================================================================================================

    /** Come {@code Trans_Algorand.ComeTransazioniDefi}: prima i prezzi a gruppi, poi le righe solo quando servono. */
    static Map<String, TransazioneDefi> ComeTransazioniDefi(String wallet, Esito esito) {
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliXRPL(esito.operazioni()), "import XRP", null);
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Operazione o : esito.operazioni()) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = wallet;
            td.HashTransazione = o.hash();
            td.Blocco = String.valueOf(o.ledger());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
            td.RighePronte = () -> Righe(wallet, o, esito.simboli());
            mappa.put(wallet + "." + o.hash(), td);
        }
        return mappa;
    }

    /** Le righe di un'operazione: la classificazione la fa {@link TransazioneDefi} dalle variazioni nette. */
    static List<String[]> Righe(String wallet, Operazione o, Map<String, String> simboli) {
        TransazioneDefi t = new TransazioneDefi();
        t.Rete = RETE;
        t.Wallet = wallet;
        t.HashTransazione = o.hash();
        t.Blocco = String.valueOf(o.ledger());
        t.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
        t.TimeStamp = String.valueOf(o.timestamp());
        t.TipoTransazione = o.tipo().isEmpty() ? "XRPL" : o.tipo();
        t.TransazioneOK = o.riuscita();
        t.MonetaCommissioni = MONETA;
        if (o.commissione().signum() != 0) t.QtaCommissioni = "-" + o.commissione().stripTrailingZeros().toPlainString();
        for (Map.Entry<String, BigDecimal> v : o.variazioni().entrySet()) {
            String k = v.getKey();
            String qta = v.getValue().stripTrailingZeros().toPlainString();
            if (k.equals(MONETA)) {
                t.InserisciMonete(MONETA, "XRP", MONETA, o.controparte(), qta, "Crypto");
            } else if (k.startsWith(NFT + ":")) {
                t.InserisciMonete(NFT, "NFT XRP Ledger", k.substring(4), o.controparte(), qta, "NFT");
            } else {
                String s = simboli.getOrDefault(k, Codice(Valuta(k)));
                //Il nome esteso di un token LP e' la coppia, il codice esadecimale non dice nulla
                String nome = Valuta(k).startsWith("03") ? s : Codice(Valuta(k)) + " di " + Emittente(k);
                t.InserisciMonete(s, nome, k, o.controparte(), qta, "Crypto");
            }
        }
        return t.RitornaRigheTabella();
    }

    //=====================================================================================================
    //=== INDIRIZZI
    //=====================================================================================================

    /** Indirizzo di un conto: base58 con l'alfabeto di Ripple, versione 0, 20 byte e controllo doppio SHA-256. */
    static boolean isIndirizzoConto(String indirizzo) {
        byte[] b = Decodifica(indirizzo);
        if (b == null || b.length != 25 || b[0] != 0) return false;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] h = sha.digest(sha.digest(Arrays.copyOfRange(b, 0, 21)));
            return Arrays.equals(Arrays.copyOfRange(h, 0, 4), Arrays.copyOfRange(b, 21, 25));
        } catch (java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }

    /** Un token: {@code VALUTA.emittente}, valuta di 3 caratteri (non XRP) o di 40 cifre esadecimali. */
    static boolean isToken(String chiave) {
        if (chiave == null || !isChiaveToken(chiave)) return false;
        String v = Valuta(chiave);
        boolean valuta = v.matches("[0-9A-Fa-f]{40}") || (v.length() == 3 && !v.equalsIgnoreCase(MONETA) && v.matches("[A-Za-z0-9?!@#$%^&*<>(){}\\[\\]|]{3}"));
        return valuta && isIndirizzoConto(Emittente(chiave));
    }

    /**
     * L'id di un NFT (64 cifre esadecimali): è l'"indirizzo" con cui l'NFT entra nei movimenti. Deve valere come
     * indirizzo, altrimenti il prezzo si cerca per simbolo e "NFT" sugli exchange è un'altra moneta (APENFT), quasi a
     * zero: nell'acquisto la gamba in entrata ha la precedenza e l'NFT pagato 100 XRP varrebbe niente.
     */
    static boolean isIdNFT(String id) {
        return id != null && id.matches("[0-9A-Fa-f]{64}");
    }

    /** I 20 byte dell'id del conto in esadecimale maiuscolo, vuoto se l'indirizzo non è valido. */
    static String IdConto(String indirizzo) {
        byte[] b = Decodifica(indirizzo);
        if (b == null || b.length != 25) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 21; i++) sb.append(String.format("%02X", b[i]));
        return sb.toString();
    }

    private static byte[] Decodifica(String s) {
        if (s == null || s.length() < 25 || s.length() > 35) return null;
        BigInteger n = BigInteger.ZERO;
        for (char c : s.toCharArray()) {
            int v = ALFABETO.indexOf(c);
            if (v < 0) return null;
            n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(v));
        }
        byte[] b = n.toByteArray();
        int zeri = 0;
        while (zeri < s.length() && s.charAt(zeri) == ALFABETO.charAt(0)) zeri++;
        int inizio = b.length > 1 && b[0] == 0 ? 1 : 0;
        byte[] out = new byte[zeri + b.length - inizio];
        System.arraycopy(b, inizio, out, zeri, b.length - inizio);
        return out;
    }

    //=====================================================================================================
    //=== RETE
    //=====================================================================================================

    /** Una chiamata JSON-RPC, ripetuta se il nodo è occupato ({@code tooBusy}, {@code slowDown}, 429, 5xx). */
    private static JSONObject Chiama(String nodo, String metodo, JSONObject parametri, BooleanSupplier interrotto)
            throws IOException, InterruptedException {
        if (Funzioni.ReteDisabilitataPerTest) throw new IOException("rete disabilitata nei test");
        String corpo = new JSONObject().put("method", metodo).put("params", new JSONArray().put(parametri)).toString();
        IOException ultimo = null;
        for (int tentativo = 1; tentativo <= TENTATIVI; tentativo++) {
            if (Interrotto(interrotto)) throw new InterruptedException("interrotto");
            Request r = new Request.Builder().url(nodo).post(RequestBody.create(corpo, JSON)).build();
            int codice;
            String testo;
            try (Response risposta = HTTP.newCall(r).execute()) {
                codice = risposta.code();
                testo = risposta.body() == null ? "" : risposta.body().string();
            } catch (IOException e) {
                ultimo = e;
                System.out.println("XRP Ledger: tentativo " + tentativo + " non riuscito (" + e.getMessage() + "), riprovo.");
                Thread.sleep(3000L * tentativo);
                continue;
            }
            if (codice >= 200 && codice < 300) {
                JSONObject risultato = new JSONObject(testo).optJSONObject("result");
                if (risultato == null) throw new IOException("risposta senza result da " + metodo);
                String errore = risultato.optString("error", "");
                if (!errore.equals("tooBusy") && !errore.equals("slowDown")) return risultato;
                ultimo = new IOException(errore + ": " + risultato.optString("error_message"));
            } else {
                ultimo = new IOException("HTTP " + codice + " da " + metodo + ": " + testo.substring(0, Math.min(200, testo.length())));
                if (codice != 429 && codice < 500) throw ultimo;
            }
            System.out.println("XRP Ledger: nodo occupato (" + ultimo.getMessage() + "), riprovo.");
            Thread.sleep(10000L * tentativo);
        }
        throw ultimo;
    }

    /** Un errore del nodo diverso da quelli che si ripetono diventa un'eccezione: lo scaricamento non importa nulla. */
    private static void Errore(JSONObject risultato) throws IOException {
        if (risultato.has("error")) {
            throw new IOException(risultato.optString("error") + ": " + risultato.optString("error_message"));
        }
    }

    private static boolean Interrotto(BooleanSupplier interrotto) {
        return interrotto != null && interrotto.getAsBoolean();
    }

    private static void Stato(Consumer<String> avanzamento, String testo) {
        if (avanzamento != null) avanzamento.accept(testo);
    }
}
