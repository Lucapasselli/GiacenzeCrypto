package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Importazione di un wallet MiniPay su Celo e del conto della sua carta, dai log di Tenderly, senza chiavi.
 *
 * <p>MiniPay è un wallet normale (EOA) che paga le commissioni in stablecoin: le sue transazioni sono CIP-64 (tipo
 * {@code 0x7b}, campo {@code feeCurrency}) e il gas non esce in CELO. All'inizio della transazione il nodo preleva il
 * massimo della commissione con un {@code Transfer} del token dal wallet all'indirizzo di sistema {@code 0x…ce106a5}, alla
 * fine restituisce il non usato con un {@code Transfer} da quell'indirizzo al wallet: quei due log non sono movimenti, la
 * loro differenza è la
 * commissione ({@link #CommissioneCip64}). La strada DeFi generica li leggerebbe come un prelievo e un deposito e
 * metterebbe una commissione in CELO che il wallet non ha mai pagato.
 *
 * <p>La carta MiniPay (Visa, sull'infrastruttura di Gnosis Pay) è una Safe senza proprietari con i moduli Zodiac Roles e
 * Delay: le spese le manda l'operatore della carta al contratto di regolamento, i prelievi tornano al wallet MiniPay, che
 * è abilitato sul modulo Delay. Così la Safe si riconosce in modo esatto partendo dal wallet ({@link #isSafeCarta}): si
 * cerca fra le controparti dei trasferimenti del wallet e si salva ({@link #OPZIONE_SAFE}). La Safe si importa sempre
 * insieme al suo wallet, come wallet a sé ({@code safe (CELO)}), e ricariche e prelievi fra i due diventano
 * trasferimenti fra wallet propri ({@link #Abbina}, chiave l'hash della transazione).
 *
 * <p>Il CELO nativo non lascia log: si vedono solo la commissione e l'invio delle transazioni firmate dal wallet. Un CELO
 * ricevuto non si vede, e la verifica dei saldi ({@link #VerificaSaldi}) lo segnala. Le ricompense giornaliere MiniPay
 * Boost (USDT da un distributore della configurazione) sono REWARD. Decisioni dell'utente del
 * 2026-11-07 in {@code nocommit/Documentazione/Analisi_MiniPay_Celo.md}.
 */
final class Trans_MiniPay {

    private Trans_MiniPay() {}

    static final String RETE = "CELO";
    static final String PROVIDER = "TENDERLY";
    /** La voce di MiniPay nell'elenco delle reti di Gestione Wallet, accanto a Celo: il wallet si salva su CELO. */
    static final String ETICHETTA_WALLET = "MiniPay (" + RETE + ")";
    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "MiniPay";
    /** Opzione (personale.mv.db): le coppie wallet=Safe della carta riconosciute, indirizzi minuscoli separati da virgola. */
    static final String OPZIONE_SAFE = "MiniPay_Safe";
    /** Id dell'exchange in {@code RW_Predefiniti.json}: il suo gruppo wallet va al wallet MiniPay e alla Safe della carta. */
    static final String ID_GRUPPO = "minipay";

    static final String MONETA = "CELO";
    static final int DECIMALI_CELO = 18;
    static final String INDIRIZZO_NULLO = "0x0000000000000000000000000000000000000000";
    /** Indirizzo di sistema di Celo da cui passano prelievo e restituzione della commissione CIP-64 ("ce1o" in esadecimale). */
    static final String INDIRIZZO_COMMISSIONI = "0x000000000000000000000000000000000ce106a5";
    static final String SENTINELLA_MODULI = "0x0000000000000000000000000000000000000001";

    static final String TOPIC_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    static final String SEL_GET_MODULES_PAGINATED = "cc2f8452";
    static final String SEL_IS_MODULE_ENABLED = "2d9ad53d";
    static final String SEL_BALANCE_OF = "70a08231";

    static final String CAUSALE_RICARICA_CARTA = "Ricarica carta MiniPay";
    static final String CAUSALE_PRELIEVO_CARTA = "Prelievo dalla carta MiniPay";
    static final String CAUSALE_CARTA = "Pagamento con carta MiniPay";
    static final String CAUSALE_ACCREDITO_CARTA = "Accredito dalla carta MiniPay (rimborso o cashback)";
    static final String CAUSALE_DAIMO = "Ricarica Daimo Pay";
    static final String CAUSALE_BOOST = "MiniPay Boost";
    static final String CAUSALE_COMMISSIONE = "Commissione di rete";

    //=====================================================================================================
    //=== CONFIGURAZIONE
    //=====================================================================================================

    /**
     * Il contenuto di {@code config/varie/MiniPay.json}: indirizzi minuscoli.
     *
     * @param nodi nodi JSON-RPC di Celo, nell'ordine in cui provarli
     * @param masterCopyDelay implementazione del modulo Zodiac Delay: un modulo della Safe che la usa e ha il wallet
     *        abilitato dice che la Safe è del wallet
     * @param regolamentoCarta destinatari delle spese con la carta
     * @param accreditiCarta mittenti di rimborsi e accrediti della carta
     * @param ricaricheDaimo mittenti delle ricariche Daimo Pay (da un'altra catena o da un exchange)
     * @param boost mittenti delle ricompense giornaliere MiniPay Boost: REWARD (decisione dell'utente del 2026-11-07)
     * @param simboli indirizzo → simbolo da usare al posto di {@code symbol()} (USD₮: il font non ha il carattere)
     */
    record Config(List<String> nodi, long bloccoInizio, String masterCopyDelay, String masterCopyRoles,
            Set<String> regolamentoCarta, Set<String> accreditiCarta, Set<String> ricaricheDaimo, Set<String> boost,
            Map<String, String> simboli) {}

    /** @return la configurazione (disco, poi copia nel jar), oppure {@code null} se non è disponibile */
    static Config Configurazione() {
        return MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, Trans_MiniPay::Interpreta);
    }

    static Config Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject r = new JSONObject(contenuto);
            List<String> nodi = new ArrayList<>();
            for (int i = 0; i < r.getJSONArray("nodi").length(); i++) nodi.add(r.getJSONArray("nodi").getString(i).trim());
            Map<String, String> simboli = new HashMap<>();
            JSONObject s = r.optJSONObject("simboli");
            if (s != null) for (String k : s.keySet()) simboli.put(k.toLowerCase(), s.getString(k));
            Config c = new Config(Collections.unmodifiableList(nodi), r.optLong("bloccoInizio", 0),
                    r.getString("masterCopyDelay").toLowerCase(), r.optString("masterCopyRoles", "").toLowerCase(),
                    Insieme(r.optJSONArray("regolamentoCarta")), Insieme(r.optJSONArray("accreditiCarta")),
                    Insieme(r.optJSONArray("ricaricheDaimo")), Insieme(r.optJSONArray("boost")), Collections.unmodifiableMap(simboli));
            return nodi.isEmpty() ? null : c;
        } catch (Exception e) {
            LoggerGC.ScriviErrore("MiniPay: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }

    private static Set<String> Insieme(JSONArray a) {
        Set<String> s = new LinkedHashSet<>();
        if (a != null) for (int i = 0; i < a.length(); i++) s.add(a.getString(i).trim().toLowerCase());
        return Collections.unmodifiableSet(s);
    }

    //=====================================================================================================
    //=== RICONOSCIMENTO DELLA SAFE DELLA CARTA
    //=====================================================================================================

    /** @return wallet MiniPay → Safe della carta, indirizzi minuscoli */
    static Map<String, String> SafeRegistrate() {
        Map<String, String> m = new LinkedHashMap<>();
        if (DatabaseH2.connectionPersonale == null) return m;
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_SAFE);
        if (v == null) return m;
        for (String coppia : v.split(",")) {
            String[] p = coppia.split("=");
            if (p.length == 2 && !p[0].isBlank() && !p[1].isBlank()) m.put(p[0].trim().toLowerCase(), p[1].trim().toLowerCase());
        }
        return m;
    }

    /** @return {@code true} se l'indirizzo è la Safe della carta di un wallet MiniPay */
    static boolean isSafeRegistrata(String indirizzo) {
        return indirizzo != null && SafeRegistrate().containsValue(indirizzo.trim().toLowerCase());
    }

    /**
     * Salva la coppia wallet/Safe e mette i due wallet nel gruppo wallet di MiniPay ({@code RW_Predefiniti.json}) se l'utente
     * non gliene ha già dato uno. Se il wallet ha già un gruppo suo, la Safe prende quello: le ricariche restano interne.
     */
    static void Registra(String wallet, String safe) {
        if (wallet == null || safe == null || DatabaseH2.connectionPersonale == null) return;
        Map<String, String> m = SafeRegistrate();
        if (!safe.equalsIgnoreCase(m.get(wallet.toLowerCase()))) {
            m.put(wallet.toLowerCase(), safe.toLowerCase());
            List<String> coppie = new ArrayList<>();
            for (Map.Entry<String, String> e : m.entrySet()) coppie.add(e.getKey() + "=" + e.getValue());
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_SAFE, String.join(",", coppie));
        }
        String nomeWallet = wallet.trim() + " (" + RETE + ")", nomeSafe = safe.trim() + " (" + RETE + ")";
        String gruppoWallet = Gruppo(nomeWallet);
        if (gruppoWallet == null) {
            gruppoWallet = Principale_GruppiWalletRW.gruppoPreconfigurato(ID_GRUPPO);
            if (!gruppoWallet.isBlank()) DatabaseH2.Pers_GruppoWallet_Scrivi(nomeWallet, gruppoWallet);
        }
        if (gruppoWallet != null && !gruppoWallet.isBlank() && Gruppo(nomeSafe) == null) {
            DatabaseH2.Pers_GruppoWallet_Scrivi(nomeSafe, gruppoWallet);
        }
    }

    /**
     * @return {@code TRUE} se {@code candidato} è una Safe con un modulo Zodiac Delay (implementazione della configurazione)
     *         su cui {@code wallet} è abilitato, cioè la Safe della carta del wallet; {@code FALSE} se non lo è, {@code null}
     *         se il nodo non risponde
     */
    static Boolean isSafeCarta(String candidato, String wallet, OKX_WalletCarta.Rpc rpc, Config cfg) {
        try {
            if (!HaCodice(candidato, "latest", rpc)) return false;
            //Un contratto che non e' una Safe rifiuta la chiamata (revert): non e' la carta. Un altro errore e' del nodo
            String r = ChiamaSeEsiste(rpc, candidato, SEL_GET_MODULES_PAGINATED + Parola(SENTINELLA_MODULI) + Parola("a"));
            for (String modulo : Moduli(r)) {
                JsonArray par = new JsonArray();
                par.add(modulo);
                par.add("latest");
                String codice = rpc.chiama("eth_getCode", par).getAsString();
                if (!cfg.masterCopyDelay().equals(ImplementazioneEip1167(codice))) continue;
                String abilitato = ChiamaSeEsiste(rpc, modulo, SEL_IS_MODULE_ENABLED + Parola(wallet));
                if (abilitato != null && abilitato.length() > 2 && new BigInteger(abilitato.substring(2), 16).signum() != 0) return true;
            }
            return false;
        } catch (Exception e) {
            LoggerGC.ScriviErrore("MiniPay: verifica della Safe " + candidato + " non riuscita : " + e.getMessage());
            return null;
        }
    }

    /** I moduli restituiti da {@code getModulesPaginated} ({@code address[]} e indirizzo successivo); vuoto se non decodificabile. */
    static List<String> Moduli(String risultato) {
        List<String> out = new ArrayList<>();
        if (risultato == null || risultato.length() < 2 + 64 * 3) return out;
        try {
            String dati = risultato;
            int parola = Trans_XLayer.ParolaDati(dati, 0).intValueExact() / 32;
            int n = Trans_XLayer.ParolaDati(dati, parola).intValueExact();
            for (int k = 0; k < n && k < 50; k++) {
                out.add(Indirizzo(Trans_XLayer.Parola(Trans_XLayer.ParolaDati(dati, parola + 1 + k).toString(16))));
            }
        } catch (ArithmeticException | StringIndexOutOfBoundsException e) {
            //risposta di un contratto che non è una Safe
        }
        return out;
    }

    /** L'implementazione di un proxy minimo EIP-1167 dal suo codice, {@code null} se il codice non è quel proxy. */
    static String ImplementazioneEip1167(String codice) {
        if (codice == null) return null;
        String c = codice.toLowerCase();
        String inizio = "0x363d3d373d3d3d363d73";
        if (!c.startsWith(inizio) || c.length() < inizio.length() + 40) return null;
        return "0x" + c.substring(inizio.length(), inizio.length() + 40);
    }

    /** Testo dell'avviso quando la Safe della carta viene riconosciuta la prima volta. */
    static String TestoRiconosciuto(String wallet, String safe) {
        return "Il wallet MiniPay " + wallet + " ha la carta MiniPay, il cui conto e' la Safe " + safe + " su Celo.\n\n"
                + "I movimenti della carta (ricariche, spese, prelievi) si importano insieme al wallet, come wallet a parte,\n"
                + "e le ricariche fra i due diventano trasferimenti fra wallet propri. Il wallet e la Safe sono stati messi\n"
                + "nel gruppo wallet di MiniPay, se non ne avevano gia' uno.";
    }

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    record Token(String indirizzo, String simbolo, int decimali) {}

    /** Il CELO nativo: per gli importatori una moneta come le altre, con l'indirizzo uguale al simbolo. */
    static final Token CELO_NATIVO = new Token(MONETA, MONETA, DECIMALI_CELO);

    /**
     * Un movimento da scrivere. {@code tipo} è il {@code TipoTr} di {@code creaMovimento}, {@code null} per farlo decidere
     * dalle gambe.
     */
    record Movimento(String tipo, String causale, Token uscita, BigInteger qtaUscita, Token entrata, BigInteger qtaEntrata,
            String controparte) {}

    /** Una transazione di uno dei due wallet (il wallet MiniPay o la Safe della carta). */
    record Transazione(String wallet, String hash, long blocco, long timestamp, List<Movimento> movimenti) {}

    /**
     * @param safe la Safe della carta del wallet, {@code null} se non c'è (ancora nessuna ricarica)
     * @param safeNuova vero se la Safe è stata riconosciuta in questo scaricamento
     */
    record Esito(List<Transazione> transazioni, List<String> avvisi, boolean completo, String safe, boolean safeNuova) {}

    /** Dati letti sul nodo, separati perché i test li forniscono da file. */
    interface Fonte {
        Token token(String indirizzo) throws Exception;

        /** {@code eth_getTransactionByHash} */
        JsonObject transazione(String hash) throws Exception;

        /** {@code eth_getTransactionReceipt} */
        JsonObject ricevuta(String hash) throws Exception;

        /** istante del blocco in secondi, se il log non porta {@code blockTimestamp} */
        long tempo(long blocco) throws Exception;
    }

    /**
     * Scarica tutta la storia del wallet e della Safe della sua carta (riconosciuta qui la prima volta), e tiene per ogni
     * wallet le transazioni dopo il suo ultimo blocco importato. I nodi della configurazione si provano in ordine;
     * nessuna transazione se nessuno arriva in fondo.
     *
     * @param ultimoBlocco l'ultimo blocco già importato di un wallet (indirizzo minuscolo), 0 se nessuno
     */
    static Esito Scarica(String wallet, ToLongFunction<String> ultimoBlocco, BooleanSupplier interrotto, int idDocumento,
            Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        Config cfg = Configurazione();
        if (cfg == null) {
            avvisi.add("Configurazione MiniPay (config/varie/" + NOME + ".json) non disponibile.");
            return new Esito(List.of(), avvisi, false, null, false);
        }
        //La Safe riconosciuta su un nodo caduto subito dopo e' gia' salvata: per il nodo successivo non e' piu' nuova
        boolean conosciuta = SafeRegistrate().containsKey(wallet.trim().toLowerCase());
        for (String url : cfg.nodi()) {
            try {
                Esito e = Scarica(wallet, ultimoBlocco, OKX_WalletCarta.RpcNodo(url), cfg, interrotto, idDocumento, avanzamento);
                if (!conosciuta && e.safe() != null && !e.safeNuova()) {
                    e = new Esito(e.transazioni(), e.avvisi(), e.completo(), e.safe(), true);
                }
                return e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LoggerGC.ScriviErrore("MiniPay: nodo " + url + " : " + e.getMessage());
                avvisi.add("Nodo " + url + ": " + e.getMessage());
            }
            if (interrotto != null && interrotto.getAsBoolean()) break;
        }
        return new Esito(List.of(), avvisi, false, null, false);
    }

    /** Come {@link #Scarica(String, ToLongFunction, BooleanSupplier, int, Consumer)} su un nodo solo; un errore del nodo è un'eccezione. */
    static Esito Scarica(String wallet, ToLongFunction<String> ultimoBlocco, OKX_WalletCarta.Rpc rpc, Config cfg,
            BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento) throws Exception {
        String w = wallet.trim().toLowerCase();
        List<String> avvisi = new ArrayList<>();
        Stato(avanzamento, "Celo: lettura dell'ultimo blocco...");
        long ultimo = Long.decode(rpc.chiama("eth_blockNumber", new JsonArray()).getAsString());

        JsonArray par = new JsonArray();
        par.add(w);
        par.add("latest");
        String codice = rpc.chiama("eth_getCode", par).getAsString();
        //Un wallet con codice non firma: la Safe della carta si importa con il suo wallet, un altro contratto non si legge
        if (codice != null && codice.length() > 2 && !codice.toLowerCase().startsWith("0xef0100")) {
            if (SafeRegistrate().containsValue(w)) {
                //Inserita a mano in Gestione Wallet: nessun errore, i movimenti arrivano con il suo wallet
                avvisi.add("L'indirizzo e' il conto di una carta MiniPay: si importa insieme al suo wallet MiniPay, non da solo.");
                return new Esito(List.of(), avvisi, true, null, false);
            } else {
                avvisi.add("L'indirizzo e' un contratto: su Celo si importano solo wallet normali (come MiniPay) e la loro carta.");
            }
            return new Esito(List.of(), avvisi, false, null, false);
        }

        Fonte fonte = FonteNodo(rpc, cfg, idDocumento);
        Stato(avanzamento, "Celo: lettura dei trasferimenti del wallet...");
        List<JsonObject> logWallet = LogTransfer(w, cfg.bloccoInizio(), ultimo, rpc, interrotto, idDocumento);
        if (logWallet == null) return new Esito(List.of(), avvisi, false, null, false);

        //La Safe della carta: salvata, oppure fra le controparti contratto dei trasferimenti del wallet
        String safe = SafeRegistrate().get(w);
        boolean safeNuova = false;
        if (safe == null) {
            Stato(avanzamento, "Celo: ricerca del conto della carta MiniPay...");
            for (String candidato : Controparti(w, logWallet)) {
                if (interrotto != null && interrotto.getAsBoolean()) return new Esito(List.of(), avvisi, false, null, false);
                Boolean s = isSafeCarta(candidato, w, rpc, cfg);
                if (s == null) throw new Exception("verifica del conto della carta non riuscita");
                if (s) {
                    safe = candidato;
                    safeNuova = true;
                    Registra(w, safe);
                    break;
                }
            }
        }
        List<JsonObject> logSafe = List.of();
        if (safe != null) {
            Stato(avanzamento, "Celo: lettura dei movimenti della carta MiniPay...");
            logSafe = LogTransfer(safe, cfg.bloccoInizio(), ultimo, rpc, interrotto, idDocumento);
            if (logSafe == null) return new Esito(List.of(), avvisi, false, null, false);
        }

        Stato(avanzamento, "Celo: lettura delle transazioni...");
        Map<String, BigInteger> saldiWallet = new LinkedHashMap<>(), saldiSafe = new LinkedHashMap<>();
        int[] firmate = {0};
        List<Transazione> tutte = new ArrayList<>(Ricostruisci(w, safe, false, logWallet, fonte, cfg, saldiWallet, firmate, avvisi));
        if (safe != null) tutte.addAll(Ricostruisci(safe, w, true, logSafe, fonte, cfg, saldiSafe, null, avvisi));
        if (interrotto != null && interrotto.getAsBoolean()) return new Esito(List.of(), avvisi, false, null, false);

        Stato(avanzamento, "Celo: verifica dei saldi...");
        VerificaSaldi(w, saldiWallet, rpc, avvisi, "Wallet MiniPay");
        if (safe != null) VerificaSaldi(safe, saldiSafe, rpc, avvisi, "Carta MiniPay");
        BigInteger nonce = new BigInteger(rpc.chiama("eth_getTransactionCount", par).getAsString().substring(2), 16);
        if (nonce.compareTo(BigInteger.valueOf(firmate[0])) > 0) {
            avvisi.add("Il wallet ha firmato " + nonce + " transazioni, " + firmate[0] + " con trasferimenti di token: le altre"
                    + " (commissioni in CELO, invii di solo CELO) non si leggono dai log.");
        }

        List<Transazione> nuove = new ArrayList<>();
        Map<String, Long> daBlocco = new HashMap<>();
        for (Transazione t : tutte) {
            long da = daBlocco.computeIfAbsent(t.wallet(), x -> ultimoBlocco.applyAsLong(x) + 1);
            if (t.blocco() >= da) nuove.add(t);
        }
        System.out.println("Celo " + wallet + ": " + tutte.size() + " transazioni con movimenti"
                + (safe != null ? " (carta MiniPay compresa)" : "") + ", " + nuove.size() + " da importare.");
        return new Esito(nuove, avvisi, true, safe, safeNuova);
    }

    private static void Stato(Consumer<String> avanzamento, String testo) {
        if (avanzamento != null) avanzamento.accept(testo);
    }

    /** Le controparti contratto possibili della Safe: destinatari e mittenti dei trasferimenti, nell'ordine in cui compaiono. */
    static List<String> Controparti(String w, List<JsonObject> logs) {
        Set<String> s = new LinkedHashSet<>();
        for (JsonObject l : logs) {
            JsonArray tp = l.getAsJsonArray("topics");
            if (tp == null || tp.size() < 3) continue;
            String da = Indirizzo(Topic(tp, 1)), a = Indirizzo(Topic(tp, 2));
            String altro = da.equals(w) ? a : da;
            if (!altro.equals(w) && !altro.equals(INDIRIZZO_NULLO) && !altro.equals(INDIRIZZO_COMMISSIONI)) s.add(altro);
        }
        return new ArrayList<>(s);
    }

    /**
     * I {@code Transfer} con il wallet come mittente o destinatario, da {@code da} ad {@code a}, senza doppioni e in ordine di
     * blocco e indice. Il tratto si divide a metà se il nodo lo rifiuta (Tenderly oltre 20.000 risultati) e a tratti di
     * 5.000 blocchi se il nodo limita i blocchi (forno, drpc), fino a un massimo oltre il quale si passa al nodo successivo.
     *
     * @return i log, oppure {@code null} se l'utente ha interrotto
     */
    static List<JsonObject> LogTransfer(String w, long da, long a, OKX_WalletCarta.Rpc rpc, BooleanSupplier interrotto,
            int idDocumento) throws Exception {
        Map<String, JsonObject> unici = new TreeMap<>();
        String parola = "0x" + Parola(w);
        for (int pos = 1; pos <= 2; pos++) {
            JsonArray topics = new JsonArray();
            topics.add(TOPIC_TRANSFER);
            if (pos == 2) topics.add(JsonNull.INSTANCE);
            topics.add(parola);
            List<JsonObject> trovati = new ArrayList<>();
            Intervallo(rpc, topics, da, a, trovati, interrotto, idDocumento);
            if (interrotto != null && interrotto.getAsBoolean()) return null;
            for (JsonObject l : trovati) {
                if (l.has("removed") && !l.get("removed").isJsonNull() && l.get("removed").getAsBoolean()) continue;
                unici.put(ChiaveLog(l), l);
            }
        }
        return new ArrayList<>(unici.values());
    }

    /** Tratto dei nodi che limitano i blocchi di {@code eth_getLogs} (forno risponde fino a 5.000). */
    static final long TRATTO_RIPIEGO = 5_000;
    /** Oltre questi tratti (circa due mesi di Celo) il ripiego non conviene: meglio il nodo successivo. */
    static final long TRATTI_MASSIMI = 1_000;

    private static void Intervallo(OKX_WalletCarta.Rpc rpc, JsonArray topics, long da, long a, List<JsonObject> out,
            BooleanSupplier interrotto, int idDocumento) throws Exception {
        if (interrotto != null && interrotto.getAsBoolean()) return;
        JsonObject filtro = new JsonObject();
        filtro.addProperty("fromBlock", "0x" + Long.toHexString(da));
        //Sempre un numero: con "latest" Tenderly tronca in silenzio oltre 20.000 risultati
        filtro.addProperty("toBlock", "0x" + Long.toHexString(a));
        filtro.add("topics", topics);
        JsonArray par = new JsonArray();
        par.add(filtro);
        JsonElement r;
        try {
            r = rpc.chiama("eth_getLogs", par);
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            boolean risultati = m.contains("results");
            boolean blocchi = m.contains("range") || m.contains("too large") || m.contains("blocks");
            if ((!risultati && !blocchi) || a <= da) throw e;
            if (blocchi && a - da + 1 > TRATTO_RIPIEGO) {
                if (a - da + 1 > TRATTI_MASSIMI * TRATTO_RIPIEGO) throw e;
                for (long i = da; i <= a; i += TRATTO_RIPIEGO) {
                    Intervallo(rpc, topics, i, Math.min(a, i + TRATTO_RIPIEGO - 1), out, interrotto, idDocumento);
                }
            } else {
                long m2 = (da + a) >>> 1;
                Intervallo(rpc, topics, da, m2, out, interrotto, idDocumento);
                Intervallo(rpc, topics, m2 + 1, a, out, interrotto, idDocumento);
            }
            return;
        }
        DocumentiFonte.AggiungiAllaSessione(idDocumento, "MiniPay", "eth_getLogs " + filtro, r.toString());
        for (JsonElement el : r.getAsJsonArray()) out.add(el.getAsJsonObject());
    }

    /** La {@link Fonte} sul nodo, con le letture in memoria per la durata dello scaricamento. */
    static Fonte FonteNodo(OKX_WalletCarta.Rpc rpc, Config cfg, int idDocumento) {
        Map<String, Token> token = new HashMap<>();
        Map<String, JsonObject> tx = new HashMap<>(), ricevute = new HashMap<>();
        Map<Long, Long> tempi = new HashMap<>();
        return new Fonte() {
            @Override
            public Token token(String a) throws Exception {
                Token t = token.get(a);
                if (t != null) return t;
                String simbolo = cfg.simboli().get(a);
                if (simbolo == null) {
                    simbolo = Trans_XLayer.DecodificaStringa(ChiamaSicura(rpc, a, "95d89b41"));
                    if (simbolo.isBlank()) simbolo = a;
                }
                int decimali = 0;
                String d = ChiamaSicura(rpc, a, "313ce567");
                if (d != null && d.length() > 2) {
                    BigInteger v = new BigInteger(d.substring(2), 16);
                    if (v.compareTo(BigInteger.valueOf(36)) <= 0) decimali = v.intValue();
                }
                t = new Token(a, simbolo, decimali);
                token.put(a, t);
                return t;
            }

            @Override
            public JsonObject transazione(String hash) throws Exception {
                return Leggi(tx, "eth_getTransactionByHash", hash);
            }

            @Override
            public JsonObject ricevuta(String hash) throws Exception {
                return Leggi(ricevute, "eth_getTransactionReceipt", hash);
            }

            private JsonObject Leggi(Map<String, JsonObject> cache, String metodo, String hash) throws Exception {
                JsonObject o = cache.get(hash);
                if (o == null) {
                    JsonArray par = new JsonArray();
                    par.add(hash);
                    JsonElement r = rpc.chiama(metodo, par);
                    if (r == null || !r.isJsonObject()) throw new Exception(metodo + " " + hash + ": nessuna risposta");
                    o = r.getAsJsonObject();
                    DocumentiFonte.AggiungiAllaSessione(idDocumento, "MiniPay", metodo + " " + hash, o.toString());
                    cache.put(hash, o);
                }
                return o;
            }

            @Override
            public long tempo(long blocco) throws Exception {
                Long t = tempi.get(blocco);
                if (t == null) {
                    JsonArray par = new JsonArray();
                    par.add("0x" + Long.toHexString(blocco));
                    par.add(false);
                    t = Long.decode(rpc.chiama("eth_getBlockByNumber", par).getAsJsonObject().get("timestamp").getAsString());
                    tempi.put(blocco, t);
                }
                return t;
            }
        };
    }

    //=====================================================================================================
    //=== RICOSTRUZIONE DEI MOVIMENTI
    //=====================================================================================================

    /** Le voci speciali dei trasferimenti: tutto il resto si somma per moneta e segue la forma della transazione. */
    private enum Voce { GENERICA, SPESA_CARTA, ACCREDITO_CARTA, RICARICA_CARTA, PRELIEVO_CARTA, DAIMO, BOOST }

    /**
     * I movimenti di un wallet, dai suoi {@code Transfer} in ordine. Logica pura salvo le letture di {@code fonte}.
     *
     * @param altro per il wallet MiniPay la Safe della sua carta, per la Safe il wallet MiniPay; può essere {@code null}
     * @param isSafe vero per la Safe della carta: le uscite verso il regolamento sono spese, la Safe non firma mai
     * @param saldi riceve il saldo di ogni moneta che risulta dai movimenti (unità minime), CELO compreso
     * @param firmate se non {@code null}, riceve in [0] le transazioni firmate dal wallet
     */
    static List<Transazione> Ricostruisci(String w, String altro, boolean isSafe, List<JsonObject> logs, Fonte fonte, Config cfg,
            Map<String, BigInteger> saldi, int[] firmate, List<String> avvisi) throws Exception {
        Map<String, List<JsonObject>> perTx = new LinkedHashMap<>();
        for (JsonObject l : logs) perTx.computeIfAbsent(Testo(l, "transactionHash").toLowerCase(), k -> new ArrayList<>()).add(l);
        List<Transazione> transazioni = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> e : perTx.entrySet()) {
            String hash = e.getKey();
            List<JsonObject> tx = e.getValue();
            JsonObject primo = tx.get(0);
            long blocco = Long.decode(Testo(primo, "blockNumber"));
            String ts = Testo(primo, "blockTimestamp");
            long tempo = ts.isEmpty() ? fonte.tempo(blocco) : Long.decode(ts);

            List<Movimento> movimenti = new ArrayList<>();
            Set<String> esclusi = new HashSet<>();
            BigInteger valoreCeloUscito = BigInteger.ZERO;
            String destinatarioTx = "";
            if (!isSafe) {
                JsonObject t = fonte.transazione(hash);
                if (w.equalsIgnoreCase(Testo(t, "from"))) {
                    if (firmate != null) firmate[0]++;
                    JsonObject ric = fonte.ricevuta(hash);
                    destinatarioTx = Testo(t, "to").toLowerCase();
                    String feeCurrency = Testo(t, "feeCurrency");
                    if (!feeCurrency.isEmpty() && !feeCurrency.equals(INDIRIZZO_NULLO)) {
                        Commissione c = CommissioneCip64(w, ric);
                        if (c == null) {
                            avvisi.add("Transazione " + hash + ": commissione in token non riconosciuta, controllare a mano.");
                        } else {
                            esclusi.addAll(c.log());
                            Token tk = fonte.token(c.token());
                            if (c.qta().signum() > 0) {
                                movimenti.add(new Movimento("COMMISSIONE", CAUSALE_COMMISSIONE + " in " + tk.simbolo(), tk, c.qta(), null, null, ""));
                                saldi.merge(c.token(), c.qta().negate(), BigInteger::add);
                            }
                            String controllo = ControllaCommissione(c.qta(), tk.decimali(), ric);
                            if (controllo != null) avvisi.add("Transazione " + hash + ": " + controllo);
                        }
                    } else {
                        BigInteger fee = Numero(ric, "gasUsed").multiply(Numero(ric, "effectiveGasPrice")).add(Numero(ric, "l1Fee"));
                        if (fee.signum() > 0) {
                            movimenti.add(new Movimento("COMMISSIONE", CAUSALE_COMMISSIONE, CELO_NATIVO, fee, null, null, ""));
                            saldi.merge(MONETA, fee.negate(), BigInteger::add);
                        }
                    }
                    if ("0x1".equals(Testo(ric, "status"))) valoreCeloUscito = Numero(t, "value");
                }
            }

            //Somme per voce e moneta; le uscite negative
            Map<Voce, Map<String, BigInteger>> somme = new LinkedHashMap<>();
            Map<String, String> controparte = new HashMap<>();
            for (JsonObject l : tx) {
                if (esclusi.contains(ChiaveLog(l))) continue;
                JsonArray tp = l.getAsJsonArray("topics");
                if (tp == null || tp.size() != 3 || !Topic(tp, 0).equals(TOPIC_TRANSFER)) continue; //gli NFT ERC-721 hanno 4 topic
                String da = Indirizzo(Topic(tp, 1)), a = Indirizzo(Topic(tp, 2));
                if (da.equals(a)) continue;
                String token = Testo(l, "address").toLowerCase();
                BigInteger valore = Trans_XLayer.ParolaDati(Testo(l, "data"), 0);
                if (valore.signum() == 0) continue;
                boolean uscita = da.equals(w);
                String cp = uscita ? a : da;
                Voce voce = Voce.GENERICA;
                if (isSafe && uscita && cfg.regolamentoCarta().contains(cp)) voce = Voce.SPESA_CARTA;
                else if (isSafe && !uscita && (cfg.accreditiCarta().contains(cp) || cfg.regolamentoCarta().contains(cp))) voce = Voce.ACCREDITO_CARTA;
                else if (altro != null && cp.equals(altro)) voce = uscita == !isSafe ? Voce.RICARICA_CARTA : Voce.PRELIEVO_CARTA;
                else if (!uscita && cfg.ricaricheDaimo().contains(cp)) voce = Voce.DAIMO;
                else if (!uscita && cfg.boost().contains(cp)) voce = Voce.BOOST;
                somme.computeIfAbsent(voce, k -> new LinkedHashMap<>()).merge(token, uscita ? valore.negate() : valore, BigInteger::add);
                controparte.putIfAbsent(voce + token, cp);
                saldi.merge(token, uscita ? valore.negate() : valore, BigInteger::add);
            }
            if (valoreCeloUscito.signum() > 0) {
                somme.computeIfAbsent(Voce.GENERICA, k -> new LinkedHashMap<>()).merge(MONETA, valoreCeloUscito.negate(), BigInteger::add);
                controparte.putIfAbsent(Voce.GENERICA + MONETA, destinatarioTx);
                saldi.merge(MONETA, valoreCeloUscito.negate(), BigInteger::add);
            }

            for (Map.Entry<Voce, Map<String, BigInteger>> v : somme.entrySet()) {
                if (v.getKey() == Voce.GENERICA) continue;
                for (Map.Entry<String, BigInteger> m : v.getValue().entrySet()) {
                    if (m.getValue().signum() == 0) continue;
                    Token tk = TokenDi(m.getKey(), fonte);
                    String cp = controparte.getOrDefault(v.getKey() + m.getKey(), "");
                    BigInteger q = m.getValue().abs();
                    boolean uscita = m.getValue().signum() < 0;
                    switch (v.getKey()) {
                        case SPESA_CARTA -> movimenti.add(uscita
                                ? new Movimento("CASHOUT O SIMILARE", CAUSALE_CARTA, tk, q, null, null, cp)
                                : new Movimento(null, CAUSALE_ACCREDITO_CARTA, null, null, tk, q, cp));
                        case ACCREDITO_CARTA -> movimenti.add(uscita
                                ? new Movimento(null, "Prelievo", tk, q, null, null, cp)
                                : new Movimento(null, CAUSALE_ACCREDITO_CARTA, null, null, tk, q, cp));
                        case RICARICA_CARTA, PRELIEVO_CARTA -> {
                            String causale = v.getKey() == Voce.RICARICA_CARTA ? CAUSALE_RICARICA_CARTA : CAUSALE_PRELIEVO_CARTA;
                            movimenti.add(uscita ? new Movimento(null, causale, tk, q, null, null, cp)
                                    : new Movimento(null, causale, null, null, tk, q, cp));
                        }
                        case DAIMO -> movimenti.add(uscita ? new Movimento(null, "Prelievo", tk, q, null, null, cp)
                                : new Movimento(null, CAUSALE_DAIMO, null, null, tk, q, cp));
                        case BOOST -> movimenti.add(uscita ? new Movimento(null, "Prelievo", tk, q, null, null, cp)
                                : new Movimento("REWARD", CAUSALE_BOOST, null, null, tk, q, cp));
                        default -> { }
                    }
                }
            }

            Map<String, BigInteger> netto = somme.getOrDefault(Voce.GENERICA, Map.of());
            List<String> uscite = new ArrayList<>(), entrate = new ArrayList<>();
            for (Map.Entry<String, BigInteger> n : netto.entrySet()) {
                if (n.getValue().signum() < 0) uscite.add(n.getKey());
                else if (n.getValue().signum() > 0) entrate.add(n.getKey());
            }
            if (uscite.size() == 1 && entrate.size() == 1) {
                String u = uscite.get(0), en = entrate.get(0);
                movimenti.add(new Movimento(null, "Scambio", TokenDi(u, fonte), netto.get(u).negate(), TokenDi(en, fonte), netto.get(en),
                        controparte.getOrDefault(Voce.GENERICA + u, controparte.getOrDefault(Voce.GENERICA + en, ""))));
            } else {
                if (uscite.size() + entrate.size() > 1) {
                    avvisi.add("Transazione " + hash + " con piu' monete, importata come depositi e prelievi separati: da controllare.");
                }
                for (String u : uscite) {
                    movimenti.add(new Movimento(null, "Prelievo", TokenDi(u, fonte), netto.get(u).negate(), null, null,
                            controparte.getOrDefault(Voce.GENERICA + u, "")));
                }
                for (String en : entrate) {
                    movimenti.add(new Movimento(null, "Deposito", null, null, TokenDi(en, fonte), netto.get(en),
                            controparte.getOrDefault(Voce.GENERICA + en, "")));
                }
            }
            if (!movimenti.isEmpty()) transazioni.add(new Transazione(w, hash, blocco, tempo, movimenti));
        }
        return transazioni;
    }

    private static Token TokenDi(String indirizzo, Fonte fonte) throws Exception {
        return MONETA.equals(indirizzo) ? CELO_NATIVO : fonte.token(indirizzo);
    }

    /**
     * La commissione di una transazione CIP-64: il primo log della ricevuta è il prelievo del massimo, un {@code Transfer}
     * del token dal wallet a {@link #INDIRIZZO_COMMISSIONI}, l'ultimo log da quell'indirizzo al wallet, dello stesso token,
     * è la restituzione del non usato (il nodo li scrive prima e dopo l'esecuzione). Gli altri log da quell'indirizzo sono
     * la commissione che va al nodo e non toccano il wallet.
     *
     * @param log le chiavi ({@link #ChiaveLog}) dei due log, da non contare come movimenti
     */
    record Commissione(String token, BigInteger qta, Set<String> log) {}

    /** @return la commissione, oppure {@code null} se la ricevuta non ha la forma attesa */
    static Commissione CommissioneCip64(String w, JsonObject ricevuta) {
        JsonArray logs = ricevuta.getAsJsonArray("logs");
        if (logs == null || logs.isEmpty()) return null;
        JsonObject prelievo = logs.get(0).getAsJsonObject();
        JsonArray tp = prelievo.getAsJsonArray("topics");
        if (tp == null || tp.size() != 3 || !Topic(tp, 0).equals(TOPIC_TRANSFER) || !Indirizzo(Topic(tp, 1)).equals(w)
                || !Indirizzo(Topic(tp, 2)).equals(INDIRIZZO_COMMISSIONI)) return null;
        String token = Testo(prelievo, "address").toLowerCase();
        BigInteger qta = Trans_XLayer.ParolaDati(Testo(prelievo, "data"), 0);
        Set<String> chiavi = new HashSet<>();
        chiavi.add(ChiaveLog(prelievo));
        for (int i = logs.size() - 1; i > 0; i--) {
            JsonObject l = logs.get(i).getAsJsonObject();
            JsonArray t = l.getAsJsonArray("topics");
            if (t == null || t.size() != 3 || !Topic(t, 0).equals(TOPIC_TRANSFER) || !Testo(l, "address").equalsIgnoreCase(token)
                    || !Indirizzo(Topic(t, 1)).equals(INDIRIZZO_COMMISSIONI)) continue;
            if (Indirizzo(Topic(t, 2)).equals(w)) {
                qta = qta.subtract(Trans_XLayer.ParolaDati(Testo(l, "data"), 0));
                chiavi.add(ChiaveLog(l));
            }
            break;
        }
        return qta.signum() < 0 ? null : new Commissione(token, qta, chiavi);
    }

    /**
     * Confronta la commissione letta dai trasferimenti con {@code gasUsed × effectiveGasPrice}, che il nodo esprime in
     * unità a 18 decimali anche quando la moneta ne ha meno (l'adattatore di USDT e USDC): la differenza deve stare sotto
     * un'unità minima del token.
     *
     * @return {@code null} se torna, altrimenti il testo dell'avviso
     */
    static String ControllaCommissione(BigInteger qta, int decimali, JsonObject ricevuta) {
        BigInteger attesa = Numero(ricevuta, "gasUsed").multiply(Numero(ricevuta, "effectiveGasPrice"));
        if (attesa.signum() == 0 || decimali > 18) return null;
        BigInteger scala = BigInteger.TEN.pow(18 - decimali);
        BigInteger diff = qta.multiply(scala).subtract(attesa).abs();
        if (diff.compareTo(scala) <= 0) return null;
        return "commissione dai trasferimenti " + qta + ", dal gas " + attesa.divide(scala) + " (unita' minime): da controllare.";
    }

    /**
     * Confronta il saldo ricostruito di ogni moneta con quello sulla catena. Il CELO ricevuto non lascia log: se il saldo
     * vero è diverso, lo si dice.
     */
    static void VerificaSaldi(String w, Map<String, BigInteger> saldi, OKX_WalletCarta.Rpc rpc, List<String> avvisi, String chi) {
        Map<String, BigInteger> tutti = new LinkedHashMap<>(saldi);
        tutti.putIfAbsent(MONETA, BigInteger.ZERO);
        for (Map.Entry<String, BigInteger> e : tutti.entrySet()) {
            try {
                BigInteger vero;
                if (MONETA.equals(e.getKey())) {
                    JsonArray par = new JsonArray();
                    par.add(w);
                    par.add("latest");
                    vero = new BigInteger(rpc.chiama("eth_getBalance", par).getAsString().substring(2), 16);
                } else {
                    String b = Chiama(rpc, e.getKey(), SEL_BALANCE_OF + Parola(w));
                    vero = b == null || b.length() <= 2 ? BigInteger.ZERO : new BigInteger(b.substring(2), 16);
                }
                if (!vero.equals(e.getValue())) {
                    avvisi.add(chi + ", saldo di " + (MONETA.equals(e.getKey()) ? "CELO nativo" : e.getKey()) + ": dai movimenti "
                            + e.getValue() + ", sulla catena " + vero + " (unita' minime)"
                            + (MONETA.equals(e.getKey()) ? ". Il CELO ricevuto non si legge dai log" : "") + ", differenza da controllare.");
                }
            } catch (Exception ex) {
                avvisi.add(chi + ", saldo di " + e.getKey() + " non verificabile: " + ex.getMessage());
            }
        }
    }

    //=====================================================================================================
    //=== RIGHE DEI MOVIMENTI
    //=====================================================================================================

    static List<String[]> Righe(Transazione t) {
        return Righe(t, null);
    }

    /** Le righe della tabella movimenti di una transazione; con {@code prezzoFisso} non si cerca nessun prezzo (test). */
    static List<String[]> Righe(Transazione t, String prezzoFisso) {
        List<String[]> righe = new ArrayList<>();
        String nomeWallet = t.wallet() + " (" + RETE + ")";
        int n = t.movimenti().size();
        for (int i = 0; i < n; i++) {
            Movimento m = t.movimenti().get(i);
            String[] rt = MovimentiCrypto.creaMovimento(Moneta(m.uscita(), m.qtaUscita(), true), Moneta(m.entrata(), m.qtaEntrata(), false),
                    nomeWallet, "Wallet", t.timestamp() * 1000, prezzoFisso, null, i + 1, n, null, null, "A", t.hash(), m.tipo(), null);
            if (rt == null) continue;
            rt[7] = m.causale();
            //Il pagamento con la carta e' un cashout con il suo nome: campo 18 resta PCO, il calcolo non cambia
            if ("CASHOUT O SIMILARE".equals(m.tipo()) && CAUSALE_CARTA.equals(m.causale())) MovimentiCrypto.MarcaSpesaConCarta(rt);
            rt[23] = String.valueOf(t.blocco());
            if (rt.length > 30) rt[30] = m.controparte() == null ? "" : m.controparte();
            Importazioni.RiempiVuotiArray(rt);
            righe.add(rt);
        }
        return righe;
    }

    private static Moneta Moneta(Token t, BigInteger qta, boolean uscita) {
        if (t == null || qta == null) return null;
        BigDecimal q = new BigDecimal(qta).movePointLeft(t.decimali()).stripTrailingZeros();
        Moneta m = new Moneta();
        m.InserisciValori(t.simbolo(), (uscita ? q.negate() : q).toPlainString(), t.indirizzo(), "Crypto");
        m.SetNomeEsteso(t.simbolo());
        m.SetRete(RETE);
        return m;
    }

    /**
     * Le transazioni nella forma dell'importazione DeFi ({@code GUI_GestioneWallets.AggiornaWallets}), quelle della Safe
     * della carta con la Safe come wallet, con i prezzi scaricati prima a gruppi e le righe costruite solo quando
     * l'importazione le chiede.
     */
    static Map<String, TransazioneDefi> ComeTransazioniDefi(List<Transazione> transazioni) {
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliMiniPay(transazioni), "import MiniPay", null);
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Transazione t : transazioni) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = t.wallet();
            td.HashTransazione = t.hash();
            td.Blocco = String.valueOf(t.blocco());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(t.timestamp() * 1000);
            td.RighePronte = () -> Righe(t);
            mappa.put(t.wallet() + "." + t.hash(), td);
        }
        return mappa;
    }

    /** Il blocco più alto fra i movimenti già importati del wallet ({@code [23]}), 0 se nessuno. */
    static long UltimoBloccoImportato(Map<String, String[]> archivio, String wallet) {
        String nome = wallet.trim() + " (" + RETE + ")";
        long massimo = 0;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 23 || !nome.equalsIgnoreCase(v[3].trim())) continue;
            try {
                massimo = Math.max(massimo, Long.parseLong(v[23].trim()));
            } catch (NumberFormatException e) {
                //movimento senza blocco (inserito a mano): non dice da dove ripartire
            }
        }
        return massimo;
    }

    //=====================================================================================================
    //=== ABBINAMENTO DI RICARICHE E PRELIEVI DELLA CARTA
    //=====================================================================================================

    /**
     * Collega ogni ricarica della carta al prelievo dal wallet MiniPay, e ogni prelievo dalla carta al deposito sul wallet:
     * sono la stessa transazione ({@code [24]}), la stessa moneta e la stessa quantità, quindi il legame è esatto. Diventano
     * un trasferimento fra wallet propri (PTW/DTW). Rifatto dopo ogni importazione, non cambia nulla di già classificato.
     *
     * @return quante coppie ha abbinato
     */
    static int Abbina() {
        try {
            if (DatabaseH2.connectionPersonale == null) return 0;
            Map<String, String> coppie = SafeRegistrate();
            if (coppie.isEmpty()) return 0;
            int n = Abbina(Principale.MappaCryptoWallet, coppie);
            if (n > 0) Principale.TabellaCryptodaAggiornare = true;
            return n;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return 0;
        }
    }

    static int Abbina(Map<String, String[]> archivio, Map<String, String> coppie) {
        //nome del wallet (minuscolo) → nome dell'altro wallet della coppia
        Map<String, String> altro = new HashMap<>();
        for (Map.Entry<String, String> e : coppie.entrySet()) {
            String a = (e.getKey() + " (" + RETE + ")").toLowerCase(), b = (e.getValue() + " (" + RETE + ")").toLowerCase();
            altro.put(a, b);
            altro.put(b, a);
        }
        Map<String, List<String[]>> depositi = new HashMap<>();
        List<String[]> prelievi = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 30 || v[24] == null || v[24].isBlank() || !v[18].isBlank() || v[7] == null) continue;
            if (!v[7].equals(CAUSALE_RICARICA_CARTA) && !v[7].equals(CAUSALE_PRELIEVO_CARTA)) continue;
            String nome = v[3].trim().toLowerCase();
            if (!altro.containsKey(nome)) continue;
            String cat = Categoria(v);
            if ("PC".equals(cat)) prelievi.add(v);
            else if ("DC".equals(cat)) depositi.computeIfAbsent(v[24].trim().toLowerCase() + "|" + nome, k -> new ArrayList<>()).add(v);
        }
        prelievi.sort((a, b) -> a[0].compareTo(b[0]));
        int abbinati = 0;
        for (String[] p : prelievi) {
            List<String[]> candidati = depositi.get(p[24].trim().toLowerCase() + "|" + altro.get(p[3].trim().toLowerCase()));
            if (candidati == null) continue;
            String[] scelto = null;
            for (String[] d : candidati) {
                if (!d[18].isBlank() || !p[8].equalsIgnoreCase(d[11])) continue;
                try {
                    if (new BigDecimal(p[10]).abs().compareTo(new BigDecimal(d[13]).abs()) != 0) continue;
                } catch (NumberFormatException e) {
                    continue;
                }
                scelto = d;
                break;
            }
            if (scelto == null) continue;
            GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], scelto[0]);
            candidati.remove(scelto);
            abbinati++;
        }
        return abbinati;
    }

    /** Il gruppo wallet salvato per un wallet, {@code null} se non ne ha. */
    private static String Gruppo(String wallet) {
        if (DatabaseH2.connectionPersonale == null) return null;
        try {
            return DatabaseH2.Pers_GruppoWallet_Leggi(wallet.trim(), false);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String Categoria(String[] v) {
        String[] parti = v[0].split("_");
        return parti[parti.length - 1].toUpperCase();
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    private static boolean HaCodice(String a, String blocco, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonArray par = new JsonArray();
        par.add(a);
        par.add(blocco);
        String c = rpc.chiama("eth_getCode", par).getAsString();
        return c != null && c.length() > 2;
    }

    private static String Chiama(OKX_WalletCarta.Rpc rpc, String a, String dati) throws Exception {
        JsonObject c = new JsonObject();
        c.addProperty("to", a);
        c.addProperty("data", "0x" + dati);
        JsonArray par = new JsonArray();
        par.add(c);
        par.add("latest");
        return rpc.chiama("eth_call", par).getAsString();
    }

    /**
     * Come {@link #Chiama}, ma una chiamata che il contratto rifiuta ({@code revert}) restituisce {@code null}; un errore del
     * nodo resta un'eccezione, perche' "il nodo non risponde" non e' "non e' la carta".
     */
    private static String ChiamaSeEsiste(OKX_WalletCarta.Rpc rpc, String a, String dati) throws Exception {
        try {
            return Chiama(rpc, a, dati);
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (m.contains("revert")) return null;
            throw e;
        }
    }

    /** Come {@link #Chiama}, ma una chiamata che il contratto rifiuta restituisce {@code null}. */
    private static String ChiamaSicura(OKX_WalletCarta.Rpc rpc, String a, String dati) {
        try {
            return Chiama(rpc, a, dati);
        } catch (Exception e) {
            return null;
        }
    }

    static String ChiaveLog(JsonObject l) {
        return String.format("%016x", Long.decode(Testo(l, "blockNumber"))) + String.format("%08x", Long.decode(Testo(l, "logIndex")))
                + Testo(l, "transactionHash").toLowerCase();
    }

    /** Un numero esadecimale di un campo, zero se manca. */
    private static BigInteger Numero(JsonObject o, String campo) {
        String s = Testo(o, campo);
        if (s.length() <= 2) return BigInteger.ZERO;
        return new BigInteger(s.substring(2), 16);
    }

    static String Parola(String h) {
        return Trans_XLayer.Parola(h);
    }

    static String Indirizzo(String parola) {
        return Trans_XLayer.Indirizzo(parola);
    }

    private static String Topic(JsonArray tp, int i) {
        return tp.get(i).isJsonNull() ? "" : tp.get(i).getAsString().toLowerCase();
    }

    private static String Testo(JsonObject o, String campo) {
        return o != null && o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }
}
