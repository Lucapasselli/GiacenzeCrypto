package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Importazione di un wallet Algorand dall'indexer pubblico (Nodely, senza chiave: indirizzo in
 * {@code config/varie/IndirizziServizi.json}, voce {@code algorandIndexer}).
 *
 * <p>Ogni transazione, o gruppo atomico, diventa la <b>variazione netta</b> del saldo del wallet per moneta, e la
 * classificazione (deposito, prelievo, scambio, sola commissione) la fa {@link TransazioneDefi} come per le altre
 * reti. Così uno swap su un DEX, che on-chain è un gruppo di 2-4 transazioni più quelle interne dell'applicazione,
 * resta un movimento solo. Regole del saldo (verificate il 23/10/2026 su tre wallet pubblici con 900-2.700
 * transazioni: la somma ricostruita coincide al microalgo con il saldo del conto):
 * <ul>
 *   <li>{@code pay}: l'importo esce dal mittente ed entra nel destinatario; con {@code close-remainder-to} il resto
 *       ({@code closing-amount}) esce dal mittente ed entra nel conto di chiusura;</li>
 *   <li>{@code axfer}: come sopra per l'asset ({@code close-amount}, {@code close-to}); nel clawback il conto da cui
 *       si toglie è {@code asset-transfer-transaction.sender}, non chi firma;</li>
 *   <li>{@code acfg}: alla creazione il creatore riceve l'intera emissione, alla distruzione la perde;</li>
 *   <li>la commissione la paga chi firma; le transazioni interne delle applicazioni si visitano ricorsivamente
 *       (l'indexer restituisce la transazione radice anche quando il wallet compare solo in quelle interne);</li>
 *   <li>{@code sender-rewards}, {@code receiver-rewards}, {@code close-rewards}: ricompense di partecipazione
 *       (finite nel 2022), una riga REWARD a parte.</li>
 * </ul>
 * Le quantità sono uint64: sempre {@link BigInteger}, l'emissione di molti asset è 2^64-1.
 *
 * <p>Gli asset cancellati tornano dall'indexer con i parametri vuoti: nome, decimali ed emissione si prendono
 * dalla loro transazione di creazione ({@link #RisolviAsset}). I simboli non sono unici (33 pool Tinyman diverse
 * si chiamano tutte {@code TMPOOL2}) e il calcolo LIFO identifica le monete per simbolo: per i nomi condivisi noti
 * il simbolo diventa {@code "NOME (id)"} ({@link #Simbolo}).
 *
 * <p>Dettagli e indirizzi di prova: {@code nocommit/Documentazione/Analisi_Import_XRP_ALGO_TRX.md}.
 */
final class Trans_Algorand {

    private Trans_Algorand() {}

    static final String RETE = "ALGO";
    static final String MONETA = "ALGO";
    static final int DECIMALI_ALGO = 6;
    static final String PROVIDER = "INDEXER PUBBLICO";
    static final String CAUSALE_RICOMPENSE = "Ricompense di partecipazione Algorand";

    private static final int PAGINA = 1000;
    private static final int TENTATIVI = 4;

    /**
     * Simboli che molti asset diversi hanno in comune: pool Tinyman v1/v1.1/v2, pool Pact, quote dei mercati Alpha.
     * Con il solo simbolo il LIFO mescolerebbe i costi di pool o mercati diversi.
     */
    private static final Pattern SIMBOLI_CONDIVISI = Pattern.compile("(?i)TM1POOL|TMPOOL\\d*|PLP|ALPHA-[YN]");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    /** Un asset (ASA) con i dati che servono ai movimenti. */
    record Asset(long id, String simbolo, String nome, int decimali, BigInteger totale, String creatore) {
        /** Emissione di una sola unità indivisibile: un NFT (ARC-3, ARC-19, ARC-69). */
        boolean nft() {
            return decimali == 0 && BigInteger.ONE.equals(totale);
        }
    }

    /**
     * Una transazione, o un gruppo atomico, ridotta all'effetto sul wallet.
     *
     * @param variazioni chiave {@link #MONETA} o l'id dell'asset, valore in unità minime con segno; commissione e
     *                   ricompense escluse, mai valori zero
     * @param commissione microalgo pagati dal wallet
     * @param ricompense microalgo di ricompense di partecipazione accreditati al wallet
     */
    record Operazione(String hash, long round, long timestamp, String tipo, String controparte,
            Map<String, BigInteger> variazioni, BigInteger commissione, BigInteger ricompense) {}

    record Esito(List<Operazione> operazioni, Map<Long, Asset> asset, List<String> avvisi, boolean completo) {}

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /**
     * Scarica dall'indexer le transazioni del wallet dal round {@code roundDa} (incluso) all'ultimo. Prima si legge il
     * conto, e la storia si ferma al suo round: così, in una prima importazione, la somma dei movimenti si può
     * confrontare con il saldo ({@link #VerificaSaldi}).
     *
     * @param interrotto vero quando l'utente ha chiesto di fermarsi; può essere {@code null}
     * @param avanzamento riceve la riga di stato; può essere {@code null}
     * @return {@code completo} falso se lo scaricamento non è arrivato in fondo: allora non si importa nulla
     */
    static Esito Scarica(String wallet, long roundDa, BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        String base = IndirizziServizi.Url(IndirizziServizi.ALGORAND_INDEXER);
        try {
            Stato(avanzamento, "Algorand: lettura del conto " + wallet + "...");
            String testoConto = Chiama(base + "/v2/accounts/" + wallet + "?include-all=true", interrotto);
            if (testoConto == null) {
                //404: l'indexer non conosce il conto, cioè non ha mai ricevuto nulla
                System.out.println("Algorand " + wallet + ": conto senza transazioni.");
                return new Esito(List.of(), Map.of(), avvisi, true);
            }
            DocumentiFonte.AggiungiAllaSessione(idDocumento, "Algorand", "accounts " + wallet, testoConto);
            JSONObject conto = new JSONObject(testoConto);
            long roundA = conto.getLong("current-round");

            List<JSONObject> transazioni = new ArrayList<>();
            String prossima = null;
            int pagina = 0;
            do {
                if (Interrotto(interrotto)) return new Esito(List.of(), Map.of(), avvisi, false);
                pagina++;
                Stato(avanzamento, "Algorand: pagina " + pagina + " delle transazioni (" + transazioni.size() + " lette)");
                String url = base + "/v2/accounts/" + wallet + "/transactions?limit=" + PAGINA
                        + "&min-round=" + Math.max(0, roundDa) + "&max-round=" + roundA
                        + (prossima == null ? "" : "&next=" + prossima);
                String testo = Chiama(url, interrotto);
                if (testo == null) break;
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "Algorand", "transactions " + wallet + " pagina " + pagina, testo);
                JSONObject risposta = new JSONObject(testo);
                JSONArray tx = risposta.optJSONArray("transactions");
                if (tx == null || tx.isEmpty()) break;
                for (int i = 0; i < tx.length(); i++) transazioni.add(tx.getJSONObject(i));
                prossima = risposta.optString("next-token", null);
            } while (prossima != null && !prossima.isBlank());
            System.out.println("Algorand " + wallet + ": " + transazioni.size() + " transazioni dal round " + roundDa
                    + " al " + roundA + ".");

            Set<Long> ids = new LinkedHashSet<>();
            for (JSONObject t : transazioni) IdAsset(t, ids);
            Map<Long, Asset> asset = new HashMap<>();
            int n = 0;
            for (long id : ids) {
                if (Interrotto(interrotto)) return new Esito(List.of(), Map.of(), avvisi, false);
                n++;
                Stato(avanzamento, "Algorand: dati dell'asset " + n + " di " + ids.size());
                asset.put(id, LeggiAsset(base, id, transazioni, interrotto, idDocumento));
            }

            List<Operazione> operazioni = Analizza(transazioni, wallet, asset);
            if (roundDa <= 1) avvisi.addAll(VerificaSaldi(operazioni, conto, asset));
            System.out.println("Algorand " + wallet + ": " + operazioni.size() + " operazioni con effetto sul wallet.");
            return new Esito(operazioni, asset, avvisi, true);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            avvisi.add("Indexer Algorand: " + ex.getMessage());
            return new Esito(List.of(), Map.of(), avvisi, false);
        }
    }

    /** I dati di un asset: dall'indexer, e per un asset cancellato dalla sua transazione di creazione. */
    private static Asset LeggiAsset(String base, long id, List<JSONObject> transazioni, BooleanSupplier interrotto,
            int idDocumento) throws IOException, InterruptedException {
        String testo = Chiama(base + "/v2/assets/" + id + "?include-all=true", interrotto);
        if (testo == null) return RisolviAsset(id, null, null);
        DocumentiFonte.AggiungiAllaSessione(idDocumento, "Algorand", "assets " + id, testo);
        JSONObject a = new JSONObject(testo).optJSONObject("asset");
        if (a == null || !a.optBoolean("deleted", false)) return RisolviAsset(id, a, null);
        JSONObject creazione = null;
        for (JSONObject t : transazioni) {
            creazione = Creazione(t, id);
            if (creazione != null) break;
        }
        if (creazione == null) {
            //Creato da un altro conto, o prima del round di partenza: la creazione è la prima transazione dell'asset
            String storia = Chiama(base + "/v2/transactions?asset-id=" + id + "&limit=20", interrotto);
            if (storia != null) {
                DocumentiFonte.AggiungiAllaSessione(idDocumento, "Algorand", "transactions asset " + id, storia);
                JSONArray tx = new JSONObject(storia).optJSONArray("transactions");
                for (int i = 0; tx != null && i < tx.length() && creazione == null; i++) {
                    creazione = Creazione(tx.getJSONObject(i), id);
                }
            }
        }
        return RisolviAsset(id, a, creazione);
    }

    /** La transazione (anche interna) che ha creato l'asset {@code id}, o {@code null}. */
    static JSONObject Creazione(JSONObject tx, long id) {
        if (tx.optLong("created-asset-index", 0) == id) return tx;
        JSONArray interne = tx.optJSONArray("inner-txns");
        for (int i = 0; interne != null && i < interne.length(); i++) {
            JSONObject c = Creazione(interne.getJSONObject(i), id);
            if (c != null) return c;
        }
        return null;
    }

    /**
     * Costruisce l'{@link Asset} dalla risposta {@code /v2/assets/{id}} (il campo {@code asset}). Un asset cancellato
     * ha i parametri vuoti: valgono quelli di {@code creazione}, se c'è.
     */
    static Asset RisolviAsset(long id, JSONObject asset, JSONObject creazione) {
        JSONObject p = asset == null ? null : asset.optJSONObject("params");
        boolean cancellato = asset != null && asset.optBoolean("deleted", false);
        if ((p == null || cancellato) && creazione != null) {
            JSONObject acfg = creazione.optJSONObject("asset-config-transaction");
            if (acfg != null && acfg.optJSONObject("params") != null) p = acfg.getJSONObject("params");
        }
        if (p == null) p = new JSONObject();
        String unita = p.optString("unit-name", "").trim();
        String nome = p.optString("name", "").trim();
        int decimali = p.optInt("decimals", 0);
        BigInteger totale = p.has("total") ? p.getBigInteger("total") : BigInteger.ZERO;
        String creatore = p.optString("creator", "");
        boolean nft = decimali == 0 && BigInteger.ONE.equals(totale);
        return new Asset(id, Simbolo(id, unita, nome, nft), nome.isEmpty() ? unita : nome, decimali, totale, creatore);
    }

    /**
     * Il simbolo con cui l'asset compare nei movimenti: il nome dell'unità, altrimenti il nome, altrimenti
     * {@code ASA}. Per i simboli condivisi da asset diversi ({@link #SIMBOLI_CONDIVISI}), o uguali alla moneta della
     * rete, si aggiunge l'id. Gli NFT no: lo aggiunge già {@code TransazioneDefi.ValoriToken.RitornaIDToken}.
     */
    static String Simbolo(long id, String unita, String nome, boolean nft) {
        String s = !unita.isEmpty() ? unita : !nome.isEmpty() ? nome : "ASA";
        s = s.replace(";", "").trim();
        if (s.isEmpty()) s = "ASA";
        if (!nft && (s.equals("ASA") || s.equalsIgnoreCase(MONETA) || SIMBOLI_CONDIVISI.matcher(s).matches())) {
            s = s + " (" + id + ")";
        }
        return s;
    }

    /** Gli id degli asset toccati dalla transazione e da quelle interne. */
    static void IdAsset(JSONObject tx, Set<Long> ids) {
        JSONObject ax = tx.optJSONObject("asset-transfer-transaction");
        if (ax != null) ids.add(ax.getLong("asset-id"));
        if (tx.optLong("created-asset-index", 0) != 0) ids.add(tx.getLong("created-asset-index"));
        JSONObject ac = tx.optJSONObject("asset-config-transaction");
        if (ac != null && ac.optLong("asset-id", 0) != 0) ids.add(ac.getLong("asset-id"));
        JSONArray interne = tx.optJSONArray("inner-txns");
        for (int i = 0; interne != null && i < interne.length(); i++) IdAsset(interne.getJSONObject(i), ids);
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================

    /** Somme di una transazione o di un gruppo, mentre si visitano. */
    private static final class Accumulo {
        final Map<String, BigInteger> variazioni = new LinkedHashMap<>();
        BigInteger commissione = BigInteger.ZERO;
        BigInteger ricompense = BigInteger.ZERO;
        final Set<String> tipi = new LinkedHashSet<>();
        String controparte = "";

        void Aggiungi(String chiave, BigInteger qta) {
            if (qta.signum() != 0) variazioni.merge(chiave, qta, BigInteger::add);
        }

        void Controparte(String indirizzo, String wallet) {
            if (controparte.isEmpty() && indirizzo != null && !indirizzo.isEmpty() && !indirizzo.equals(wallet)) {
                controparte = indirizzo;
            }
        }
    }

    /**
     * Riduce le transazioni dell'indexer alle operazioni del wallet, in ordine di round. Le transazioni di uno stesso
     * gruppo atomico diventano un'operazione sola, con l'hash della prima del gruppo.
     *
     * @param asset i dati degli asset toccati, servono per la distruzione di un asset creato dal wallet
     */
    static List<Operazione> Analizza(List<JSONObject> transazioni, String wallet, Map<Long, Asset> asset) {
        List<JSONObject> ordinate = new ArrayList<>(transazioni);
        ordinate.sort(Comparator.comparingLong((JSONObject t) -> t.optLong("confirmed-round", 0))
                .thenComparingLong(t -> t.optLong("intra-round-offset", 0)));
        Map<String, List<JSONObject>> gruppi = new LinkedHashMap<>();
        Set<String> visti = new java.util.HashSet<>();
        for (JSONObject t : ordinate) {
            //La stessa transazione può tornare due volte da due pagine se la storia cresce durante lo scaricamento
            if (!visti.add(t.optString("id"))) continue;
            String g = t.optString("group", "");
            String chiave = g.isEmpty() ? "tx:" + t.optString("id") : "gr:" + t.optLong("confirmed-round") + ":" + g;
            gruppi.computeIfAbsent(chiave, k -> new ArrayList<>()).add(t);
        }
        List<Operazione> operazioni = new ArrayList<>();
        for (List<JSONObject> gruppo : gruppi.values()) {
            Accumulo a = new Accumulo();
            for (JSONObject t : gruppo) Applica(t, wallet, asset, a, true);
            a.variazioni.values().removeIf(v -> v.signum() == 0);
            if (a.variazioni.isEmpty() && a.commissione.signum() == 0 && a.ricompense.signum() == 0) continue;
            JSONObject primo = gruppo.get(0);
            operazioni.add(new Operazione(primo.optString("id"), primo.optLong("confirmed-round"), primo.optLong("round-time"),
                    String.join(", ", a.tipi), a.controparte, a.variazioni, a.commissione, a.ricompense));
        }
        return operazioni;
    }

    /**
     * Effetto sul wallet di una transazione e, ricorsivamente, delle sue transazioni interne. Il tipo (la causale
     * del movimento) lo danno solo le transazioni radice: quelle interne sono il meccanismo dell'applicazione.
     */
    private static void Applica(JSONObject tx, String w, Map<Long, Asset> asset, Accumulo a, boolean radice) {
        Set<String> tipi = radice ? a.tipi : new LinkedHashSet<>();
        String tipo = tx.optString("tx-type", "");
        String mittente = tx.optString("sender", "");
        boolean daWallet = mittente.equals(w);
        if (daWallet) {
            a.commissione = a.commissione.add(Numero(tx, "fee"));
            a.ricompense = a.ricompense.add(Numero(tx, "sender-rewards"));
        }
        switch (tipo) {
            case "pay" -> {
                JSONObject p = tx.getJSONObject("payment-transaction");
                BigInteger importo = Numero(p, "amount");
                BigInteger chiusura = Numero(tx, "closing-amount");
                String destinatario = p.optString("receiver", "");
                String conto = p.optString("close-remainder-to", "");
                if (daWallet) {
                    a.Aggiungi(MONETA, importo.add(chiusura).negate());
                    a.Controparte(destinatario, w);
                }
                if (destinatario.equals(w)) {
                    a.Aggiungi(MONETA, importo);
                    a.ricompense = a.ricompense.add(Numero(tx, "receiver-rewards"));
                    a.Controparte(mittente, w);
                }
                if (conto.equals(w)) {
                    a.Aggiungi(MONETA, chiusura);
                    a.ricompense = a.ricompense.add(Numero(tx, "close-rewards"));
                    a.Controparte(mittente, w);
                }
                if (daWallet || destinatario.equals(w) || conto.equals(w)) tipi.add("pay");
            }
            case "axfer" -> {
                JSONObject x = tx.getJSONObject("asset-transfer-transaction");
                String chiave = String.valueOf(x.getLong("asset-id"));
                BigInteger importo = Numero(x, "amount");
                BigInteger chiusura = Numero(x, "close-amount");
                //Nel clawback il conto da cui si toglie e' "sender" dentro il trasferimento, non chi firma
                String da = x.optString("sender", "");
                if (da.isEmpty()) da = mittente;
                String destinatario = x.optString("receiver", "");
                String conto = x.optString("close-to", "");
                if (da.equals(w)) {
                    a.Aggiungi(chiave, importo.add(chiusura).negate());
                    a.Controparte(destinatario, w);
                }
                if (destinatario.equals(w)) {
                    a.Aggiungi(chiave, importo);
                    a.Controparte(da, w);
                }
                if (conto.equals(w)) a.Aggiungi(chiave, chiusura);
                if (daWallet || da.equals(w) || destinatario.equals(w) || conto.equals(w)) tipi.add("axfer");
            }
            case "acfg" -> {
                JSONObject c = tx.optJSONObject("asset-config-transaction");
                long creato = tx.optLong("created-asset-index", 0);
                long modificato = c == null ? 0 : c.optLong("asset-id", 0);
                if (creato != 0) {
                    //L'emissione va al creatore dichiarato nei parametri, che per un'applicazione e' il suo conto
                    JSONObject p = c == null ? null : c.optJSONObject("params");
                    String creatore = p == null ? mittente : p.optString("creator", mittente);
                    if (creatore.equals(w) && p != null) {
                        a.Aggiungi(String.valueOf(creato), Numero(p, "total"));
                        tipi.add("acfg");
                    }
                } else if (modificato != 0 && (c.optJSONObject("params") == null || c.getJSONObject("params").isEmpty())) {
                    //Distruzione: possibile solo con l'intera emissione nel conto del creatore, che la perde
                    Asset as = asset.get(modificato);
                    if (as != null && w.equals(as.creatore())) {
                        a.Aggiungi(String.valueOf(modificato), as.totale().negate());
                        tipi.add("acfg");
                    }
                }
                if (daWallet) tipi.add("acfg");
            }
            case "appl" -> {
                if (daWallet) {
                    JSONObject ap = tx.optJSONObject("application-transaction");
                    tipi.add("appl " + (ap == null ? "" : ap.optLong("application-id")));
                }
            }
            default -> {
                if (daWallet) tipi.add(tipo);
            }
        }
        JSONArray interne = tx.optJSONArray("inner-txns");
        for (int i = 0; interne != null && i < interne.length(); i++) {
            Applica(interne.getJSONObject(i), w, asset, a, false);
        }
    }

    private static BigInteger Numero(JSONObject o, String campo) {
        return o != null && o.has(campo) ? o.getBigInteger(campo) : BigInteger.ZERO;
    }

    /**
     * Confronta la somma delle operazioni (variazioni, commissioni e ricompense) con i saldi del conto letti allo
     * stesso round. Ha senso solo su tutta la storia: una differenza vuol dire una regola di saldo che manca.
     *
     * @param conto la risposta {@code /v2/accounts/{indirizzo}}
     * @return un avviso per ogni moneta che non torna, vuoto se tutto coincide
     */
    static List<String> VerificaSaldi(List<Operazione> operazioni, JSONObject conto, Map<Long, Asset> asset) {
        Map<String, BigInteger> somma = new TreeMap<>();
        for (Operazione o : operazioni) {
            o.variazioni().forEach((k, v) -> somma.merge(k, v, BigInteger::add));
            somma.merge(MONETA, o.ricompense().subtract(o.commissione()), BigInteger::add);
        }
        JSONObject c = conto.optJSONObject("account");
        if (c == null) c = conto;
        Map<String, BigInteger> saldo = new TreeMap<>();
        saldo.put(MONETA, Numero(c, "amount-without-pending-rewards"));
        JSONArray detenuti = c.optJSONArray("assets");
        for (int i = 0; detenuti != null && i < detenuti.length(); i++) {
            JSONObject h = detenuti.getJSONObject(i);
            saldo.put(String.valueOf(h.getLong("asset-id")), Numero(h, "amount"));
        }
        List<String> avvisi = new ArrayList<>();
        Set<String> chiavi = new java.util.TreeSet<>(somma.keySet());
        chiavi.addAll(saldo.keySet());
        for (String k : chiavi) {
            BigInteger calcolato = somma.getOrDefault(k, BigInteger.ZERO);
            BigInteger reale = saldo.getOrDefault(k, BigInteger.ZERO);
            if (calcolato.equals(reale)) continue;
            int decimali = DECIMALI_ALGO;
            String nome = MONETA;
            if (!k.equals(MONETA)) {
                Asset as = asset.get(Long.parseLong(k));
                decimali = as == null ? 0 : as.decimali();
                nome = as == null ? "asset " + k : as.simbolo() + " (asset " + k + ")";
            }
            avvisi.add(nome + ": i movimenti importati danno " + Decimale(calcolato, decimali).toPlainString()
                    + ", il saldo sulla blockchain è " + Decimale(reale, decimali).toPlainString() + ".");
        }
        return avvisi;
    }

    static BigDecimal Decimale(BigInteger unita, int decimali) {
        return new BigDecimal(unita).movePointLeft(decimali).stripTrailingZeros();
    }

    //=====================================================================================================
    //=== MOVIMENTI
    //=====================================================================================================

    /**
     * Le operazioni nella forma dell'importazione DeFi ({@code GUI_GestioneWallets.AggiornaWallets}). Le righe, e
     * con loro i prezzi, si costruiscono solo quando l'importazione le chiede: lì si controlla "Interrompi". Prima
     * si scaricano a gruppi i prezzi che serviranno ({@link PrezziImportDeFi#RaccogliAlgorand}).
     */
    static Map<String, TransazioneDefi> ComeTransazioniDefi(String wallet, Esito esito) {
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliAlgorand(esito.operazioni()), "import ALGO", null);
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Operazione o : esito.operazioni()) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = wallet;
            td.HashTransazione = o.hash();
            td.Blocco = String.valueOf(o.round());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
            td.RighePronte = () -> Righe(wallet, o, esito.asset());
            mappa.put(wallet + "." + o.hash(), td);
        }
        return mappa;
    }

    /**
     * Le righe di un'operazione: deposito, prelievo, scambio o commissione li decide {@link TransazioneDefi} dalle
     * variazioni nette; le ricompense di partecipazione sono una riga REWARD a parte.
     */
    static List<String[]> Righe(String wallet, Operazione o, Map<Long, Asset> asset) {
        List<String[]> righe = new ArrayList<>();
        if (!o.variazioni().isEmpty() || o.commissione().signum() != 0) {
            TransazioneDefi t = new TransazioneDefi();
            t.Rete = RETE;
            t.Wallet = wallet;
            t.HashTransazione = o.hash();
            t.Blocco = String.valueOf(o.round());
            t.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(o.timestamp() * 1000);
            t.TimeStamp = String.valueOf(o.timestamp());
            t.TipoTransazione = o.tipo().isEmpty() ? "Algorand" : o.tipo();
            t.MonetaCommissioni = MONETA;
            if (o.commissione().signum() != 0) {
                t.QtaCommissioni = "-" + Decimale(o.commissione(), DECIMALI_ALGO).toPlainString();
            }
            for (Map.Entry<String, BigInteger> v : o.variazioni().entrySet()) {
                if (v.getKey().equals(MONETA)) {
                    t.InserisciMonete(MONETA, "Algorand", MONETA, o.controparte(),
                            Decimale(v.getValue(), DECIMALI_ALGO).toPlainString(), "Crypto");
                } else {
                    long id = Long.parseLong(v.getKey());
                    Asset as = asset.get(id);
                    if (as == null) as = RisolviAsset(id, null, null);
                    t.InserisciMonete(as.simbolo(), as.nome(), v.getKey(), o.controparte(),
                            Decimale(v.getValue(), as.decimali()).toPlainString(), as.nft() ? "NFT" : "Crypto");
                }
            }
            righe.addAll(t.RitornaRigheTabella());
        }
        if (o.ricompense().signum() != 0) {
            Moneta m = new Moneta();
            m.InserisciValori(MONETA, Decimale(o.ricompense(), DECIMALI_ALGO).toPlainString(), MONETA, "Crypto");
            m.SetNomeEsteso("Algorand");
            m.SetRete(RETE);
            String[] rt = MovimentiCrypto.creaMovimento(null, m, wallet + " (" + RETE + ")", "Wallet", o.timestamp() * 1000,
                    null, null, 1, 1, null, null, "A", o.hash(), "REWARD", null);
            if (rt != null) {
                rt[7] = CAUSALE_RICOMPENSE;
                rt[23] = String.valueOf(o.round());
                Importazioni.RiempiVuotiArray(rt);
                righe.add(rt);
            }
        }
        return righe;
    }

    //=====================================================================================================
    //=== INDIRIZZI
    //=====================================================================================================

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** Indirizzo di un conto Algorand: 58 caratteri base32, chiave pubblica di 32 byte più 4 byte di controllo. */
    static boolean isIndirizzoConto(String indirizzo) {
        if (indirizzo == null || indirizzo.length() != 58) return false;
        byte[] b = DecodificaBase32(indirizzo);
        if (b == null || b.length < 36) return false;
        try {
            byte[] h = MessageDigest.getInstance("SHA-512/256").digest(Arrays.copyOfRange(b, 0, 32));
            return Arrays.equals(Arrays.copyOfRange(h, h.length - 4, h.length), Arrays.copyOfRange(b, 32, 36));
        } catch (java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }

    /** Id di un asset (ASA): l'"indirizzo" dei token Algorand, un intero positivo a 64 bit senza segno. */
    static boolean isIdAsset(String id) {
        if (id == null || !id.matches("[1-9][0-9]{0,19}")) return false;
        return new BigInteger(id).bitLength() <= 64;
    }

    private static byte[] DecodificaBase32(String s) {
        byte[] out = new byte[s.length() * 5 / 8];
        int buffer = 0, bit = 0, n = 0;
        for (char ch : s.toCharArray()) {
            int v = BASE32.indexOf(ch);
            if (v < 0) return null;
            buffer = (buffer << 5) | v;
            bit += 5;
            if (bit >= 8) {
                bit -= 8;
                out[n++] = (byte) (buffer >> bit);
            }
        }
        return out;
    }

    //=====================================================================================================
    //=== RETE
    //=====================================================================================================

    /**
     * Una GET all'indexer, ripetuta su 429, errori 5xx e di connessione. {@code null} su 404 (conto o asset che
     * l'indexer non conosce); un altro errore diventa un'eccezione, e lo scaricamento non importa nulla.
     */
    private static String Chiama(String url, BooleanSupplier interrotto) throws IOException, InterruptedException {
        if (Funzioni.ReteDisabilitataPerTest) throw new IOException("rete disabilitata nei test");
        IOException ultimo = null;
        for (int tentativo = 1; tentativo <= TENTATIVI; tentativo++) {
            if (Interrotto(interrotto)) throw new InterruptedException("interrotto");
            Request r = new Request.Builder().url(url).header("Accept", "application/json").get().build();
            int codice;
            String corpo;
            try (Response risposta = HTTP.newCall(r).execute()) {
                codice = risposta.code();
                corpo = risposta.body() == null ? "" : risposta.body().string();
            } catch (IOException e) {
                ultimo = e;
                System.out.println("Algorand: tentativo " + tentativo + " non riuscito (" + e.getMessage() + "), riprovo.");
                Thread.sleep(2000L * tentativo);
                continue;
            }
            if (codice == 404) return null;
            if (codice >= 200 && codice < 300) return corpo;
            ultimo = new IOException("HTTP " + codice + " da " + url + ": " + corpo.substring(0, Math.min(200, corpo.length())));
            if (codice != 429 && codice < 500) throw ultimo;
            System.out.println("Algorand: tentativo " + tentativo + " non riuscito (HTTP " + codice + "), riprovo.");
            Thread.sleep(2000L * tentativo);
        }
        throw ultimo;
    }

    private static boolean Interrotto(BooleanSupplier interrotto) {
        return interrotto != null && interrotto.getAsBoolean();
    }

    private static void Stato(Consumer<String> avanzamento, String testo) {
        if (avanzamento != null) avanzamento.accept(testo);
    }
}
