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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Importazione di un conto ether.fi Cash (carta e conto su Optimism) dai log dei contratti del protocollo, senza chiavi.
 *
 * <p>Il "wallet" dell'utente è un contratto ({@code EtherFiSafe}) che non firma mai transazioni: le manda un relayer di
 * ether.fi ai moduli del protocollo, che fanno eseguire le chiamate al Safe. Il saldo poi non sta nel Safe ma nel
 * {@code LendGateway}, come collaterale di una struttura hub/spoke in stile Aave v4, e una spesa con la carta esce dal
 * LendGateway senza nessun trasferimento del Safe. Il calcolo per variazione netta dei trasferimenti del wallet (la
 * strada DeFi generica) vede quindi quasi niente: niente ricariche, niente scambi, niente spese, giacenza zero.
 *
 * <p>Qui il conto è <b>Safe + collaterale</b>: la variazione di una moneta in una transazione è la somma dei
 * {@code Transfer} del Safe e dei {@code Supplied}/{@code Withdrawn} del LendGateway. Gli eventi dei moduli dicono solo
 * di che cosa si tratta ({@code TopUp} ricarica, {@code Spend} spesa con la carta, {@code Deposit}/{@code LiquidDeposit}/
 * {@code SwapExecuted} scambio), così una funzione nuova di ether.fi non sparisce: diventa un deposito o un prelievo da
 * classificare, e la verifica dei saldi ({@link #VerificaSaldi}) lo segnala se qualcosa non torna.
 *
 * <p>Gli interessi delle riserve a tasso (USDC, EURC) non generano trasferimenti: lo spoke registra a ogni movimento
 * quote e importo, quindi il valore di una quota è noto a ogni movimento e l'interesse maturato fino a lì diventa una
 * REWARD ordinata prima del movimento. Fra un movimento e l'altro (e al 31/12) non si legge nulla, come per la carta
 * OKX. Decisioni dell'utente del 2026-10-24 in {@code nocommit/Documentazione/Analisi_EtherFi_Cash.md} § 10.
 *
 * <p>Fonte: {@code eth_getLogs} con il Safe in una delle tre posizioni indicizzate, tre richieste per tutta la storia sul
 * nodo Tenderly di Optimism. Sempre con un {@code toBlock} numerico: con {@code "latest"} Tenderly tronca in silenzio
 * oltre 20.000 risultati, con un numero risponde con un errore, e il tratto si divide ({@link #Intervallo}). Ogni volta si
 * rilegge tutta la storia (costa pochi secondi) e si tengono le transazioni dopo l'ultimo blocco importato: così uno
 * swap richiesto in un'importazione ed eseguito nella successiva, o il prezzo della quota al movimento precedente,
 * non dipendono da quello che c'era in archivio.
 */
final class Trans_EtherFi {

    private Trans_EtherFi() {}

    static final String RETE = "OP";
    /**
     * La voce di ether.fi nell'elenco delle reti di Gestione Wallet, accanto a Optimism: chi ha un conto ether.fi non deve
     * sapere che sta su Optimism. Il codice fra parentesi è la rete con cui il wallet si salva; il logo è
     * {@code config/loghi/ether-fi-cash-op.png} (lo slug dell'etichetta).
     */
    static final String ETICHETTA_WALLET = "ether.fi Cash (" + RETE + ")";
    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "EtherFi";
    /** Opzione (personale.mv.db): i Safe riconosciuti, indirizzi minuscoli separati da virgola. */
    static final String OPZIONE_SAFE = "EtherFi_Safe";
    /** Id dell'exchange in {@code RW_Predefiniti.json}: il suo gruppo wallet va ai Safe riconosciuti. */
    static final String ID_GRUPPO = "etherfi";

    static final String INDIRIZZO_NULLO = "0x0000000000000000000000000000000000000000";

    static final String TOPIC_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    static final String TOPIC_TRANSFER_SINGLE = "0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62";
    static final String TOPIC_TRANSFER_BATCH = "0x4a39dc06d4c0dbc64b70af90fd698a233a518aa5d07e595d983b8c0526c8f7fb";
    /** {@code Supplied(address indexed safe, address indexed asset, uint256 amount)} del LendGateway */
    static final String TOPIC_SUPPLIED = "0x523322d7afb07cee28d04498651096af062004e1f14315eff2e4a60290dfadda";
    /** {@code Withdrawn(address indexed safe, address indexed asset, uint256 amount, address indexed to)} del LendGateway */
    static final String TOPIC_WITHDRAWN = "0x889d364b5102aee044ff370e2e6a836584e5618a16dce4f5b191459cf3903e58";
    /** {@code Supply(uint256 indexed reserveId, address indexed caller, address indexed user, uint256 shares, uint256 amount)} dello spoke */
    static final String TOPIC_SUPPLY = "0xd986db228cb1fe8392c5f45ff5f2c639b7db6cbd9ca7d1fe70b2de90c2c8c961";
    /** {@code Withdraw(uint256 indexed reserveId, address indexed caller, address indexed user, uint256 shares, uint256 amount)} dello spoke */
    static final String TOPIC_WITHDRAW = "0xfe7813e2866053d5c3938554e517b554fce6666a6561bed9eaa7419b29fa9b68";
    /** {@code TopUp(bytes32 indexed txId, address indexed user, address indexed token, bytes32 sourceTxHash, uint256 chainId, uint256 amount)} */
    static final String TOPIC_TOPUP = "0x2ff66106aff37c72533c0686de51adb6d1b86dd1709407dc6a2236533ebe3c19";
    /** {@code Spend(address indexed safe, bytes32 indexed txId, uint8 indexed binSponsor, address[] tokens, uint256[] amounts, uint256[] amountInUsd, uint256 totalUsdAmt, uint8 mode)} */
    static final String TOPIC_SPEND = "0x244f4cc0665ad7ee4709aa59b30d3ea581cecde1b0430a3f23a5dc609d4890fc";

    static final String SEL_IS_ETHERFI_SAFE = "b7ca418b";
    static final String SEL_SUPPLIED_OF = "05a5b843";
    static final String SEL_DEBT_OF = "4c4ab169";
    static final String SEL_RESERVE_ID_OF = "d5794425";
    static final String SEL_BALANCE_OF = "70a08231";
    static final String SEL_USER_SUPPLIED_SHARES = "2fa08544";

    static final String CAUSALE_RICARICA = "Ricarica ether.fi";
    static final String CAUSALE_CARTA = "Pagamento con carta ether.fi";
    static final String CAUSALE_INTERESSI = "Interessi ether.fi";
    static final String CAUSALE_SCAMBIO = "Scambio ether.fi";

    private static final Pattern HASH = Pattern.compile("0x[0-9a-fA-F]{64}");

    //=====================================================================================================
    //=== CONFIGURAZIONE
    //=====================================================================================================

    /**
     * Il contenuto di {@code config/varie/EtherFi.json}: indirizzi minuscoli.
     *
     * @param nodi nodi JSON-RPC di Optimism, nell'ordine in cui provarli
     * @param bloccoInizio primo blocco da leggere: la creazione della factory dei Safe
     * @param bloccoLendGateway creazione del LendGateway attuale: un Safe più vecchio può avere movimenti sui contratti di prima
     * @param moduliScambio indirizzo del modulo → nome, per la causale dello scambio
     * @param simboli indirizzo → simbolo da usare al posto di {@code symbol()} (caratteri che il font non ha)
     */
    record Config(List<String> nodi, long bloccoInizio, String dataProvider, String lendGateway, long bloccoLendGateway,
            String spoke, String topUpDest, String cashEventEmitter, Map<String, String> moduliScambio, Map<String, String> simboli) {}

    /** @return la configurazione (disco, poi copia nel jar), oppure {@code null} se non è disponibile */
    static Config Configurazione() {
        return MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, Trans_EtherFi::Interpreta);
    }

    static Config Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject r = new JSONObject(contenuto);
            List<String> nodi = new ArrayList<>();
            for (int i = 0; i < r.getJSONArray("nodi").length(); i++) nodi.add(r.getJSONArray("nodi").getString(i).trim());
            Map<String, String> moduli = new LinkedHashMap<>();
            JSONObject m = r.optJSONObject("moduliScambio");
            if (m != null) for (String k : m.keySet()) moduli.put(k.toLowerCase(), m.getString(k));
            Map<String, String> simboli = new HashMap<>();
            JSONObject s = r.optJSONObject("simboli");
            if (s != null) for (String k : s.keySet()) simboli.put(k.toLowerCase(), s.getString(k));
            Config c = new Config(Collections.unmodifiableList(nodi), r.getLong("bloccoInizio"),
                    r.getString("dataProvider").toLowerCase(), r.getString("lendGateway").toLowerCase(),
                    r.optLong("bloccoLendGateway", 0), r.getString("spoke").toLowerCase(), r.getString("topUpDest").toLowerCase(),
                    r.getString("cashEventEmitter").toLowerCase(), Collections.unmodifiableMap(moduli), Collections.unmodifiableMap(simboli));
            return nodi.isEmpty() ? null : c;
        } catch (Exception e) {
            LoggerGC.ScriviErrore("EtherFi: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }

    //=====================================================================================================
    //=== RICONOSCIMENTO DEL SAFE
    //=====================================================================================================

    /** Che strada prende l'importazione di un wallet su Optimism. */
    enum Strada {
        /** Safe già riconosciuto in passato */
        ETHERFI,
        /** Safe riconosciuto adesso per la prima volta (e registrato) */
        ETHERFI_NUOVO,
        /** non è un Safe ether.fi: strada DeFi generica */
        GENERICA,
        /** non si è potuto verificare: per non scrivere movimenti sbagliati non si importa nulla */
        NON_VERIFICABILE
    }

    /** @return {@code true} se l'indirizzo è fra i Safe già riconosciuti */
    static boolean isRegistrato(String wallet) {
        return wallet != null && SafeRegistrati().contains(wallet.trim().toLowerCase());
    }

    static Set<String> SafeRegistrati() {
        return Elenco(OPZIONE_SAFE);
    }

    /**
     * La strada di un wallet di Optimism. Un Safe riconosciuto resta tale (il riconoscimento è salvato), così un nodo
     * irraggiungibile non lo manda mai sulla strada generica, che scriverebbe righe sbagliate e sposterebbe avanti
     * l'ultimo blocco importato, perdendo per sempre la storia vera.
     */
    static Strada Strada(String wallet) {
        if (isRegistrato(wallet)) return Strada.ETHERFI;
        Config cfg = Configurazione();
        if (cfg == null) return Strada.GENERICA;
        Boolean safe = null;
        for (String url : cfg.nodi()) {
            safe = Riconosci(wallet, OKX_WalletCarta.RpcNodo(url), cfg);
            if (safe != null) break;
        }
        if (safe == null) return Strada.NON_VERIFICABILE;
        if (!safe) return Strada.GENERICA;
        Registra(wallet);
        return Strada.ETHERFI_NUOVO;
    }

    /**
     * @return {@code TRUE} se l'indirizzo è un Safe ether.fi ({@code EtherFiDataProvider.isEtherFiSafe}), {@code FALSE}
     *         se non lo è (anche: nessun codice), {@code null} se il nodo non risponde
     */
    static Boolean Riconosci(String wallet, OKX_WalletCarta.Rpc rpc, Config cfg) {
        try {
            JsonArray par = new JsonArray();
            par.add(wallet.toLowerCase());
            par.add("latest");
            String codice = rpc.chiama("eth_getCode", par).getAsString();
            if (codice == null || codice.length() <= 2) return false;
            String r = Chiama(rpc, cfg.dataProvider(), SEL_IS_ETHERFI_SAFE + Parola(wallet));
            return r != null && r.length() > 2 && new BigInteger(r.substring(2), 16).signum() != 0;
        } catch (Exception e) {
            LoggerGC.ScriviErrore("EtherFi: riconoscimento di " + wallet + " non riuscito : " + e.getMessage());
            return null;
        }
    }

    /**
     * Salva il Safe fra quelli riconosciuti e lo mette nel gruppo wallet di ether.fi ({@code RW_Predefiniti.json}) se
     * l'utente non gliene ha già dato uno.
     */
    static void Registra(String wallet) {
        if (wallet == null || wallet.isBlank() || DatabaseH2.connectionPersonale == null) return;
        Set<String> s = SafeRegistrati();
        if (s.add(wallet.trim().toLowerCase())) DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_SAFE, String.join(",", s));
        String nome = wallet.trim() + " (" + RETE + ")";
        String gruppo = Principale_GruppiWalletRW.gruppoPreconfigurato(ID_GRUPPO);
        if (!gruppo.isBlank() && DatabaseH2.Pers_GruppoWallet_Leggi(nome, false) == null) {
            DatabaseH2.Pers_GruppoWallet_Scrivi(nome, gruppo);
        }
    }

    /** Opzione (personale.mv.db): i Safe già importati almeno una volta da qui, indirizzi minuscoli separati da virgola. */
    static final String OPZIONE_IMPORTATI = "EtherFi_SafeImportati";

    /**
     * @return {@code true} se il Safe non è mai stato importato da qui: i suoi movimenti in archivio, se ce ne sono,
     *         vengono dalla strada generica (anche se il riconoscimento è avvenuto prima, all'inserimento del wallet)
     */
    static boolean isPrimaImportazione(String wallet) {
        return !Elenco(OPZIONE_IMPORTATI).contains(wallet.trim().toLowerCase());
    }

    /** Segna il Safe come importato da qui, dopo uno scaricamento completo. */
    static void SegnaImportato(String wallet) {
        if (DatabaseH2.connectionPersonale == null) return;
        Set<String> s = Elenco(OPZIONE_IMPORTATI);
        if (s.add(wallet.trim().toLowerCase())) DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_IMPORTATI, String.join(",", s));
    }

    private static Set<String> Elenco(String opzione) {
        Set<String> s = new LinkedHashSet<>();
        if (DatabaseH2.connectionPersonale == null) return s;
        String v = DatabaseH2.Pers_Opzioni_Leggi(opzione);
        if (v != null) for (String x : v.split(",")) if (!x.isBlank()) s.add(x.trim().toLowerCase());
        return s;
    }

    /** Testo dell'avviso quando un Safe viene riconosciuto la prima volta. */
    static String TestoRiconosciuto(String wallet) {
        return "Il wallet " + wallet + " su Optimism è un conto ether.fi Cash.\n\n"
                + "I suoi movimenti (ricariche, scambi, spese con la carta, interessi) si leggono dagli eventi del\n"
                + "protocollo ether.fi sui nodi pubblici di Optimism, senza nessuna chiave. Il wallet è stato messo nel\n"
                + "gruppo wallet di ether.fi, se non ne aveva già uno.";
    }

    /** I movimenti già in archivio del wallet, cioè quelli scritti prima del riconoscimento dalla strada generica. */
    static List<String> MovimentiPrecedenti(Map<String, String[]> archivio, String wallet) {
        String nome = wallet.trim() + " (" + RETE + ")";
        List<String> id = new ArrayList<>();
        for (Map.Entry<String, String[]> e : archivio.entrySet()) {
            String[] v = e.getValue();
            if (v != null && v.length > 3 && nome.equalsIgnoreCase(v[3].trim())) id.add(e.getKey());
        }
        return id;
    }

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /** Un token: simbolo e decimali letti sul nodo (o dalla configurazione). */
    record Token(String indirizzo, String simbolo, int decimali) {}

    /**
     * Un movimento da scrivere. {@code tipo} è il {@code TipoTr} di {@code creaMovimento}, {@code null} per farlo decidere
     * dalle gambe. Gli NFT hanno quantità intera e {@code nft} vero, {@code scam} se arrivati senza un'operazione ether.fi.
     */
    record Movimento(String tipo, String causale, Token uscita, BigInteger qtaUscita, Token entrata, BigInteger qtaEntrata,
            String controparte, boolean nft, boolean scam) {}

    record Transazione(String hash, long blocco, long timestamp, List<Movimento> movimenti) {}

    record Esito(List<Transazione> transazioni, List<String> avvisi, boolean completo) {}

    /**
     * Il risultato della ricostruzione: le transazioni di tutta la storia, il saldo di ogni moneta che ne risulta e, per le
     * monete passate dallo spoke, la riserva e le quote del Safe (che non crescono con gli interessi: si confrontano esatte).
     */
    record Ricostruzione(List<Transazione> transazioni, Map<String, BigInteger> saldi, Map<String, BigInteger> riserva,
            Map<String, BigInteger> quote) {}

    /** Dati letti sul nodo durante la ricostruzione, separati perché i test li forniscono da file. */
    interface Fonte {
        Token token(String indirizzo) throws Exception;

        /** {@code reserveIdOf(asset)} del LendGateway */
        BigInteger riserva(String asset) throws Exception;

        /** istante del blocco in secondi, se il log non porta {@code blockTimestamp} */
        long tempo(long blocco) throws Exception;
    }

    /**
     * Scarica tutta la storia del Safe e tiene le transazioni dal blocco {@code bloccoDa} (incluso). I nodi della
     * configurazione si provano in ordine; nessuna transazione se nessuno arriva in fondo.
     */
    static Esito Scarica(String wallet, long bloccoDa, BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        Config cfg = Configurazione();
        if (cfg == null) {
            avvisi.add("Configurazione ether.fi (config/varie/" + NOME + ".json) non disponibile.");
            return new Esito(List.of(), avvisi, false);
        }
        for (String url : cfg.nodi()) {
            try {
                return Scarica(wallet, bloccoDa, OKX_WalletCarta.RpcNodo(url), cfg, interrotto, idDocumento, avanzamento);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LoggerGC.ScriviErrore("EtherFi: nodo " + url + " : " + e.getMessage());
                avvisi.add("Nodo " + url + ": " + e.getMessage());
            }
            if (interrotto != null && interrotto.getAsBoolean()) break;
        }
        return new Esito(List.of(), avvisi, false);
    }

    /** Come {@link #Scarica(String, long, BooleanSupplier, int, Consumer)} su un nodo solo; un errore del nodo è un'eccezione. */
    static Esito Scarica(String wallet, long bloccoDa, OKX_WalletCarta.Rpc rpc, Config cfg, BooleanSupplier interrotto,
            int idDocumento, Consumer<String> avanzamento) throws Exception {
        String w = wallet.toLowerCase();
        List<String> avvisi = new ArrayList<>();
        Stato(avanzamento, "ether.fi: lettura dell'ultimo blocco...");
        long ultimo = Long.decode(rpc.chiama("eth_blockNumber", new JsonArray()).getAsString());
        List<JsonObject> log = Log(w, cfg.bloccoInizio(), ultimo, rpc, interrotto, idDocumento, avanzamento);
        if (log == null) return new Esito(List.of(), avvisi, false);
        System.out.println("ether.fi " + wallet + ": " + log.size() + " eventi fino al blocco " + ultimo + ".");
        Ricostruzione r = Ricostruisci(w, log, cfg, FonteNodo(rpc, cfg), avvisi);
        if (!log.isEmpty() && Blocco(log.get(0)) < cfg.bloccoLendGateway()) {
            avvisi.add("Il conto ha eventi precedenti al LendGateway attuale (blocco " + cfg.bloccoLendGateway()
                    + "): i movimenti sui contratti di allora possono essere incompleti, controllare i saldi.");
        }
        Stato(avanzamento, "ether.fi: verifica dei saldi...");
        VerificaSaldi(w, r, rpc, cfg, avvisi);
        List<Transazione> nuove = new ArrayList<>();
        for (Transazione t : r.transazioni()) if (t.blocco() >= bloccoDa) nuove.add(t);
        System.out.println("ether.fi " + wallet + ": " + r.transazioni().size() + " transazioni con movimenti, "
                + nuove.size() + " dal blocco " + bloccoDa + ".");
        return new Esito(nuove, avvisi, true);
    }

    private static void Stato(Consumer<String> avanzamento, String testo) {
        if (avanzamento != null) avanzamento.accept(testo);
    }

    /**
     * Tutti i log con il Safe come argomento indicizzato in posizione 1, 2 o 3, da {@code da} ad {@code a}, senza doppioni
     * e in ordine di blocco e indice. Un log può comparire in più posizioni (un trasferimento del Safe a se stesso).
     *
     * @return i log, oppure {@code null} se l'utente ha interrotto
     */
    static List<JsonObject> Log(String w, long da, long a, OKX_WalletCarta.Rpc rpc, BooleanSupplier interrotto, int idDocumento,
            Consumer<String> avanzamento) throws Exception {
        Map<String, JsonObject> unici = new TreeMap<>();
        String parola = "0x" + Parola(w);
        long inizio = da;
        for (int pos = 1; pos <= 3; pos++) {
            JsonArray topics = new JsonArray();
            for (int i = 0; i < pos; i++) topics.add(JsonNull.INSTANCE);
            topics.add(parola);
            Stato(avanzamento, "ether.fi: lettura degli eventi (" + pos + " di 3)...");
            List<JsonObject> trovati = new ArrayList<>();
            try {
                //La prima richiesta non si spezza a tratti: su un nodo che limita i blocchi conviene partire dalla
                //creazione del Safe invece che da quella della factory (mesi di tratti vuoti)
                Intervallo(rpc, topics, inizio, a, trovati, interrotto, idDocumento, avanzamento, pos > 1 || inizio != da);
            } catch (TroppoGrande e) {
                if (pos != 1 || inizio != da) throw e;
                inizio = Creazione(w, da, a, rpc);
                Stato(avanzamento, "ether.fi: il Safe esiste dal blocco " + inizio + ", lettura a tratti...");
                trovati.clear();
                Intervallo(rpc, topics, inizio, a, trovati, interrotto, idDocumento, avanzamento, true);
            }
            if (interrotto != null && interrotto.getAsBoolean()) return null;
            for (JsonObject l : trovati) {
                if (l.has("removed") && !l.get("removed").isJsonNull() && l.get("removed").getAsBoolean()) continue;
                unici.put(String.format("%016x", Blocco(l)) + String.format("%08x", Long.decode(Testo(l, "logIndex")))
                        + Testo(l, "transactionHash").toLowerCase(), l);
            }
        }
        return new ArrayList<>(unici.values());
    }

    /** Il nodo ha rifiutato il tratto perché troppo lungo o con troppi risultati. */
    static final class TroppoGrande extends Exception {
        TroppoGrande(String m) {
            super(m);
        }
    }

    /**
     * {@code eth_getLogs} su ({@code da}, {@code a}) inclusi, dividendo il tratto finché il nodo lo accetta: a metà per
     * troppi risultati (Tenderly oltre 20.000), a tratti di 10.000 blocchi per un limite di blocchi (nodo ufficiale, drpc).
     *
     * @param aTratti falso: un limite di blocchi diventa {@link TroppoGrande} invece di una lettura a tratti
     */
    private static void Intervallo(OKX_WalletCarta.Rpc rpc, JsonArray topics, long da, long a, List<JsonObject> out,
            BooleanSupplier interrotto, int idDocumento, Consumer<String> avanzamento, boolean aTratti) throws Exception {
        if (interrotto != null && interrotto.getAsBoolean()) return;
        JsonObject filtro = new JsonObject();
        filtro.addProperty("fromBlock", "0x" + Long.toHexString(da));
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
            boolean blocchi = m.contains("range") || m.contains("too large");
            if (!risultati && !blocchi) throw e;
            if (a <= da) throw new TroppoGrande(e.getMessage());
            if (blocchi && a - da + 1 > TRATTO_RIPIEGO) {
                if (!aTratti || a - da + 1 > TRATTI_MASSIMI * TRATTO_RIPIEGO) throw new TroppoGrande(e.getMessage());
                for (long i = da; i <= a; i += TRATTO_RIPIEGO) {
                    Stato(avanzamento, "ether.fi: lettura degli eventi, blocco " + i + " di " + a);
                    Intervallo(rpc, topics, i, Math.min(a, i + TRATTO_RIPIEGO - 1), out, interrotto, idDocumento, avanzamento, true);
                }
            } else {
                long m2 = (da + a) >>> 1;
                Intervallo(rpc, topics, da, m2, out, interrotto, idDocumento, avanzamento, aTratti);
                Intervallo(rpc, topics, m2 + 1, a, out, interrotto, idDocumento, avanzamento, aTratti);
            }
            return;
        }
        DocumentiFonte.AggiungiAllaSessione(idDocumento, "EtherFi", "eth_getLogs " + filtro, r.toString());
        for (JsonElement el : r.getAsJsonArray()) out.add(el.getAsJsonObject());
    }

    /** Tratto dei nodi che limitano i blocchi di {@code eth_getLogs} (nodo ufficiale di Optimism e drpc). */
    static final long TRATTO_RIPIEGO = 10_000;
    /** Oltre questi tratti (circa 7 mesi di Optimism) il ripiego non conviene: meglio aspettare il nodo principale. */
    static final long TRATTI_MASSIMI = 1_000;

    /** Il blocco in cui il Safe ha codice per la prima volta (bisezione su {@code eth_getCode}, nodo d'archivio). */
    static long Creazione(String w, long da, long a, OKX_WalletCarta.Rpc rpc) throws Exception {
        long lo = da, hi = a;
        if (!HaCodice(w, hi, rpc)) return hi;
        while (hi - lo > 1) {
            long m = (lo + hi) >>> 1;
            if (HaCodice(w, m, rpc)) hi = m;
            else lo = m;
        }
        return hi;
    }

    private static boolean HaCodice(String w, long blocco, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonArray par = new JsonArray();
        par.add(w);
        par.add("0x" + Long.toHexString(blocco));
        String c = rpc.chiama("eth_getCode", par).getAsString();
        return c != null && c.length() > 2;
    }

    /** La {@link Fonte} sul nodo, con le letture in memoria per la durata dello scaricamento. */
    static Fonte FonteNodo(OKX_WalletCarta.Rpc rpc, Config cfg) {
        Map<String, Token> token = new HashMap<>();
        Map<String, BigInteger> riserve = new HashMap<>();
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
            public BigInteger riserva(String asset) throws Exception {
                BigInteger r = riserve.get(asset);
                if (r == null) {
                    r = new BigInteger(Chiama(rpc, cfg.lendGateway(), SEL_RESERVE_ID_OF + Parola(asset)).substring(2), 16);
                    riserve.put(asset, r);
                }
                return r;
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

    /** Una riserva dello spoke: quote del Safe e importo già contabilizzato (movimenti e interessi). */
    private static final class Riserva {
        BigInteger quote = BigInteger.ZERO;
        BigInteger contabile = BigInteger.ZERO;
    }

    /** Uno NFT entrato o uscito dal Safe in una transazione. */
    private record Nft(String contratto, String id, BigInteger qta, boolean entrata, String controparte) {}

    /**
     * I movimenti di tutta la storia, dai log in ordine. Logica pura salvo le letture di {@code fonte} (simboli,
     * riserve, istanti), che nei test vengono da file.
     */
    static Ricostruzione Ricostruisci(String w, List<JsonObject> logs, Config cfg, Fonte fonte, List<String> avvisi) throws Exception {
        //Le transazioni, in ordine: i log arrivano già ordinati per blocco e indice
        Map<String, List<JsonObject>> perTx = new LinkedHashMap<>();
        for (JsonObject l : logs) perTx.computeIfAbsent(Testo(l, "transactionHash").toLowerCase(), k -> new ArrayList<>()).add(l);

        //Riserve dello spoke: l'id di ogni moneta passata dal LendGateway
        Map<BigInteger, String> monetaDiRiserva = new HashMap<>();
        for (JsonObject l : logs) {
            JsonArray tp = l.getAsJsonArray("topics");
            if (Testo(l, "address").equalsIgnoreCase(cfg.lendGateway()) && tp.size() >= 3
                    && (Topic(tp, 0).equals(TOPIC_SUPPLIED) || Topic(tp, 0).equals(TOPIC_WITHDRAWN))) {
                String asset = Indirizzo(Topic(tp, 2));
                if (!monetaDiRiserva.containsValue(asset)) monetaDiRiserva.put(fonte.riserva(asset), asset);
            }
        }

        Map<BigInteger, Riserva> riserve = new HashMap<>();
        Map<String, BigInteger> saldi = new LinkedHashMap<>();
        Set<BigInteger> riserveIgnote = new HashSet<>();
        List<Transazione> transazioni = new ArrayList<>();

        for (Map.Entry<String, List<JsonObject>> e : perTx.entrySet()) {
            List<JsonObject> tx = e.getValue();
            Map<String, BigInteger> netto = new LinkedHashMap<>();
            Map<String, String> controparte = new HashMap<>();
            Map<String, BigInteger> interessi = new LinkedHashMap<>();
            List<String[]> ricariche = new ArrayList<>();     //token, quantità, chain, hash d'origine
            List<String[]> spese = new ArrayList<>();         //token, quantità, USD
            List<Nft> nft = new ArrayList<>();
            boolean etherfi = false;
            String modulo = null;
            for (JsonObject l : tx) {
                JsonArray tp = l.getAsJsonArray("topics");
                if (tp == null || tp.size() == 0) continue;
                String a = Testo(l, "address").toLowerCase();
                String t0 = Topic(tp, 0);
                String dati = Testo(l, "data");
                if (t0.equals(TOPIC_TRANSFER) && tp.size() == 3) {
                    String da = Indirizzo(Topic(tp, 1)), aa = Indirizzo(Topic(tp, 2));
                    BigInteger v = Trans_XLayer.ParolaDati(dati, 0);
                    //La controparte è chi sta fuori dal conto: il LendGateway è il conto stesso (il collaterale)
                    if (da.equals(w) && !aa.equals(w)) {
                        netto.merge(a, v.negate(), BigInteger::add);
                        if (!aa.equals(cfg.lendGateway())) controparte.put(a, aa);
                    } else if (aa.equals(w) && !da.equals(w)) {
                        netto.merge(a, v, BigInteger::add);
                        if (!da.equals(cfg.lendGateway())) controparte.putIfAbsent(a, da);
                    }
                } else if (t0.equals(TOPIC_TRANSFER) && tp.size() == 4) {
                    String da = Indirizzo(Topic(tp, 1)), aa = Indirizzo(Topic(tp, 2));
                    String id = new BigInteger(Topic(tp, 3).substring(2), 16).toString();
                    if (aa.equals(w) && !da.equals(w)) nft.add(new Nft(a, id, BigInteger.ONE, true, da));
                    else if (da.equals(w) && !aa.equals(w)) nft.add(new Nft(a, id, BigInteger.ONE, false, aa));
                } else if (t0.equals(TOPIC_TRANSFER_SINGLE) && tp.size() == 4) {
                    String da = Indirizzo(Topic(tp, 2)), aa = Indirizzo(Topic(tp, 3));
                    String id = Trans_XLayer.ParolaDati(dati, 0).toString();
                    BigInteger q = Trans_XLayer.ParolaDati(dati, 1);
                    if (aa.equals(w) && !da.equals(w)) nft.add(new Nft(a, id, q, true, da));
                    else if (da.equals(w) && !aa.equals(w)) nft.add(new Nft(a, id, q, false, aa));
                } else if (t0.equals(TOPIC_TRANSFER_BATCH) && tp.size() == 4) {
                    String da = Indirizzo(Topic(tp, 2)), aa = Indirizzo(Topic(tp, 3));
                    List<BigInteger> ids = Array(dati, 0), qta = Array(dati, 1);
                    for (int i = 0; i < Math.min(ids.size(), qta.size()); i++) {
                        if (aa.equals(w) && !da.equals(w)) nft.add(new Nft(a, ids.get(i).toString(), qta.get(i), true, da));
                        else if (da.equals(w) && !aa.equals(w)) nft.add(new Nft(a, ids.get(i).toString(), qta.get(i), false, aa));
                    }
                } else if (a.equals(cfg.lendGateway())) {
                    etherfi = true;
                    if (tp.size() >= 3 && Indirizzo(Topic(tp, 1)).equals(w)) {
                        String asset = Indirizzo(Topic(tp, 2));
                        BigInteger v = Trans_XLayer.ParolaDati(dati, 0);
                        if (t0.equals(TOPIC_SUPPLIED)) netto.merge(asset, v, BigInteger::add);
                        else if (t0.equals(TOPIC_WITHDRAWN) && tp.size() >= 4) {
                            netto.merge(asset, v.negate(), BigInteger::add);
                            String verso = Indirizzo(Topic(tp, 3));
                            if (!verso.equals(w)) controparte.put(asset, verso);
                        }
                    }
                } else if (a.equals(cfg.spoke())) {
                    etherfi = true;
                    if ((t0.equals(TOPIC_SUPPLY) || t0.equals(TOPIC_WITHDRAW)) && tp.size() >= 4 && Indirizzo(Topic(tp, 3)).equals(w)) {
                        BigInteger id = new BigInteger(Topic(tp, 1).substring(2), 16);
                        BigInteger quote = Trans_XLayer.ParolaDati(dati, 0), importo = Trans_XLayer.ParolaDati(dati, 1);
                        String asset = monetaDiRiserva.get(id);
                        if (asset == null) {
                            if (riserveIgnote.add(id)) avvisi.add("Riserva " + id + " dello spoke senza moneta nota: interessi non calcolati.");
                            continue;
                        }
                        Riserva r = riserve.computeIfAbsent(id, k -> new Riserva());
                        //Interesse maturato dal movimento precedente: valore attuale delle quote meno quanto già contato
                        if (r.quote.signum() > 0 && quote.signum() > 0) {
                            BigInteger interesse = r.quote.multiply(importo).divide(quote).subtract(r.contabile);
                            if (interesse.signum() > 0) {
                                interessi.merge(asset, interesse, BigInteger::add);
                                r.contabile = r.contabile.add(interesse);
                            }
                        }
                        if (t0.equals(TOPIC_SUPPLY)) {
                            r.quote = r.quote.add(quote);
                            r.contabile = r.contabile.add(importo);
                        } else {
                            r.quote = r.quote.subtract(quote).max(BigInteger.ZERO);
                            r.contabile = r.contabile.subtract(importo).max(BigInteger.ZERO);
                        }
                    }
                } else if (a.equals(cfg.topUpDest())) {
                    etherfi = true;
                    if (t0.equals(TOPIC_TOPUP) && tp.size() >= 4 && Indirizzo(Topic(tp, 2)).equals(w)) {
                        String sorgente = "0x" + Trans_XLayer.Parola(Trans_XLayer.ParolaDati(dati, 0).toString(16));
                        ricariche.add(new String[]{Indirizzo(Topic(tp, 3)), Trans_XLayer.ParolaDati(dati, 2).toString(),
                            Trans_XLayer.ParolaDati(dati, 1).toString(), sorgente});
                    }
                } else if (a.equals(cfg.cashEventEmitter())) {
                    etherfi = true;
                    if (t0.equals(TOPIC_SPEND) && tp.size() >= 2 && Indirizzo(Topic(tp, 1)).equals(w)) {
                        List<BigInteger> token = Array(dati, 0), qta = Array(dati, 1), usd = Array(dati, 2);
                        for (int i = 0; i < Math.min(token.size(), qta.size()); i++) {
                            spese.add(new String[]{Indirizzo(Trans_XLayer.Parola(token.get(i).toString(16))), qta.get(i).toString(),
                                i < usd.size() ? usd.get(i).toString() : ""});
                        }
                    }
                } else if (cfg.moduliScambio().containsKey(a)) {
                    etherfi = true;
                    modulo = cfg.moduliScambio().get(a);
                }
            }
            long blocco = Blocco(tx.get(0));
            long ts = tx.get(0).has("blockTimestamp") && !tx.get(0).get("blockTimestamp").isJsonNull()
                    ? Long.decode(Testo(tx.get(0), "blockTimestamp")) : fonte.tempo(blocco);
            List<Movimento> movimenti = new ArrayList<>();

            //1 - interessi, prima di tutto: il movimento della stessa transazione li consuma per LIFO
            for (Map.Entry<String, BigInteger> i : interessi.entrySet()) {
                movimenti.add(new Movimento("REWARD", CAUSALE_INTERESSI, null, null, fonte.token(i.getKey()), i.getValue(),
                        cfg.lendGateway(), false, false));
            }
            //2 - ricariche
            for (String[] r : ricariche) {
                BigInteger q = new BigInteger(r[1]);
                movimenti.add(new Movimento(null, CAUSALE_RICARICA + " da " + NomeChain(r[2]) + ", transazione " + r[3],
                        null, null, fonte.token(r[0]), q, cfg.topUpDest(), false, false));
                netto.merge(r[0], q.negate(), BigInteger::add);
            }
            //3 - spese con la carta
            for (String[] s : spese) {
                BigInteger q = new BigInteger(s[1]);
                String usd = s[2].isEmpty() ? "" : " (USD " + new BigDecimal(new BigInteger(s[2])).movePointLeft(6).stripTrailingZeros().toPlainString() + ")";
                movimenti.add(new Movimento("CASHOUT O SIMILARE", CAUSALE_CARTA + usd, fonte.token(s[0]), q, null, null,
                        controparte.getOrDefault(s[0], ""), false, false));
                netto.merge(s[0], q, BigInteger::add);
            }
            //4 - quello che resta: uno scambio se è una moneta che esce e una che entra, altrimenti depositi e prelievi
            List<String> uscite = new ArrayList<>(), entrate = new ArrayList<>();
            for (Map.Entry<String, BigInteger> n : netto.entrySet()) {
                if (n.getValue().signum() < 0) uscite.add(n.getKey());
                else if (n.getValue().signum() > 0) entrate.add(n.getKey());
            }
            if (uscite.size() == 1 && entrate.size() == 1) {
                String u = uscite.get(0), en = entrate.get(0);
                movimenti.add(new Movimento(null, etherfi ? CAUSALE_SCAMBIO + (modulo == null ? "" : " " + modulo) : "Scambio",
                        fonte.token(u), netto.get(u).negate(), fonte.token(en), netto.get(en), controparte.getOrDefault(u, ""), false, false));
            } else {
                if (uscite.size() + entrate.size() > 1) {
                    avvisi.add("Transazione " + e.getKey() + " con piu' monete, importata come depositi e prelievi separati: da controllare.");
                }
                for (String u : uscite) {
                    movimenti.add(new Movimento(null, "Prelievo", fonte.token(u), netto.get(u).negate(), null, null,
                            controparte.getOrDefault(u, ""), false, false));
                }
                for (String en : entrate) {
                    movimenti.add(new Movimento(null, "Deposito", null, null, fonte.token(en), netto.get(en),
                            controparte.getOrDefault(en, ""), false, false));
                }
            }
            //5 - NFT: arrivati senza un'operazione ether.fi nella stessa transazione sono SCAM (decisione dell'utente)
            for (Nft n : nft) {
                Token t = fonte.token(n.contratto());
                Token id = new Token(n.contratto(), t.simbolo(), 0);
                if (n.entrata()) movimenti.add(new Movimento(null, "Deposito NFT", null, null, id, n.qta(), n.controparte(), true, !etherfi));
                else movimenti.add(new Movimento(null, "Prelievo NFT", id, n.qta(), null, null, n.controparte(), true, false));
            }

            for (Movimento m : movimenti) {
                if (m.nft()) continue;
                if (m.entrata() != null) saldi.merge(m.entrata().indirizzo(), m.qtaEntrata(), BigInteger::add);
                if (m.uscita() != null) saldi.merge(m.uscita().indirizzo(), m.qtaUscita().negate(), BigInteger::add);
            }
            if (!movimenti.isEmpty()) transazioni.add(new Transazione(e.getKey(), blocco, ts, movimenti));
        }
        Map<String, BigInteger> riservaDi = new HashMap<>(), quote = new HashMap<>();
        for (Map.Entry<BigInteger, String> m : monetaDiRiserva.entrySet()) {
            riservaDi.put(m.getValue(), m.getKey());
            Riserva r = riserve.get(m.getKey());
            quote.put(m.getValue(), r == null ? BigInteger.ZERO : r.quote);
        }
        return new Ricostruzione(transazioni, saldi, riservaDi, quote);
    }

    /** Nome della rete d'origine di una ricarica, dal chain id dell'evento {@code TopUp}. */
    static String NomeChain(String chainId) {
        for (Reti.Rete r : Reti.Elenco()) {
            if (r.chainidEtherscan() > 0 && String.valueOf(r.chainidEtherscan()).equals(chainId)) return r.id();
        }
        return "chain " + chainId;
    }

    /**
     * Confronta il saldo ricostruito di ogni moneta con quello del conto (collaterale nel LendGateway più saldo del Safe).
     * Per le monete dello spoke si confrontano prima le quote, che non crescono con gli interessi: se sono le stesse, il
     * saldo vero può superare quello ricostruito (interessi maturati dopo l'ultimo movimento, che non si leggono) ma non
     * essere più basso. Un debito (modalità credito) non è gestito e va segnalato.
     */
    static void VerificaSaldi(String w, Ricostruzione r, OKX_WalletCarta.Rpc rpc, Config cfg, List<String> avvisi) {
        for (Map.Entry<String, BigInteger> e : r.saldi().entrySet()) {
            String a = e.getKey();
            try {
                BigInteger vero = new BigInteger(Chiama(rpc, cfg.lendGateway(), SEL_SUPPLIED_OF + Parola(w) + Parola(a)).substring(2), 16);
                String b = ChiamaSicura(rpc, a, SEL_BALANCE_OF + Parola(w));
                if (b != null && b.length() > 2) vero = vero.add(new BigInteger(b.substring(2), 16));
                String debito = ChiamaSicura(rpc, cfg.lendGateway(), SEL_DEBT_OF + Parola(w) + Parola(a));
                if (debito != null && debito.length() > 2 && new BigInteger(debito.substring(2), 16).signum() > 0) {
                    avvisi.add("Il conto ha un debito in " + a + " (modalita' credito): non e' gestito dall'importazione.");
                }
                Boolean quoteUguali = null;
                BigInteger riserva = r.riserva().get(a);
                if (riserva != null) {
                    String q = Chiama(rpc, cfg.spoke(), SEL_USER_SUPPLIED_SHARES + Parola(riserva.toString(16)) + Parola(w));
                    quoteUguali = new BigInteger(q.substring(2), 16).equals(r.quote().get(a));
                }
                String esito = EsitoSaldo(e.getValue(), vero, quoteUguali);
                if (esito != null) avvisi.add("Saldo di " + a + ": " + esito);
            } catch (Exception ex) {
                avvisi.add("Saldo di " + a + " non verificabile: " + ex.getMessage());
            }
        }
    }

    /**
     * @param quoteUguali se le quote dello spoke ricostruite sono quelle del conto, {@code null} per una moneta fuori
     *        dallo spoke
     * @return {@code null} se il saldo ricostruito torna con quello vero (uguale a meno di un'unità minima, o più basso
     *         per i soli interessi quando le quote coincidono), altrimenti il testo dell'avviso
     */
    static String EsitoSaldo(BigInteger ricostruito, BigInteger vero, Boolean quoteUguali) {
        BigInteger diff = vero.subtract(ricostruito);
        if (Boolean.FALSE.equals(quoteUguali)) {
            return "le quote depositate non coincidono con quelle del conto (sul conto " + vero + ", dai movimenti " + ricostruito
                    + " in unita' minime): movimenti da controllare.";
        }
        if (diff.abs().compareTo(BigInteger.ONE) <= 0) return null;
        if (Boolean.TRUE.equals(quoteUguali) && diff.signum() > 0) return null;
        return "dai movimenti " + ricostruito + ", sul conto " + vero + " (unita' minime): differenza da controllare.";
    }

    //=====================================================================================================
    //=== RIGHE DEI MOVIMENTI
    //=====================================================================================================

    static List<String[]> Righe(String wallet, Transazione t) {
        return Righe(wallet, t, null);
    }

    /** Le righe della tabella movimenti di una transazione; con {@code prezzoFisso} non si cerca nessun prezzo (test). */
    static List<String[]> Righe(String wallet, Transazione t, String prezzoFisso) {
        List<String[]> righe = new ArrayList<>();
        String nomeWallet = wallet + " (" + RETE + ")";
        int n = t.movimenti().size();
        for (int i = 0; i < n; i++) {
            Movimento m = t.movimenti().get(i);
            String prezzo = m.nft() ? "0" : prezzoFisso;
            String[] rt = MovimentiCrypto.creaMovimento(Moneta(m.uscita(), m.qtaUscita(), true, m.nft(), false),
                    Moneta(m.entrata(), m.qtaEntrata(), false, m.nft(), m.scam()), nomeWallet, "Wallet", t.timestamp() * 1000,
                    prezzo, null, i + 1, n, null, null, "A", t.hash(), m.tipo(), null);
            if (rt == null) continue;
            rt[7] = m.causale();
            rt[23] = String.valueOf(t.blocco());
            if (rt.length > 30) rt[30] = m.controparte() == null ? "" : m.controparte();
            Importazioni.RiempiVuotiArray(rt);
            righe.add(rt);
        }
        return righe;
    }

    private static Moneta Moneta(Token t, BigInteger qta, boolean uscita, boolean nft, boolean scam) {
        if (t == null || qta == null) return null;
        BigDecimal q = new BigDecimal(qta).movePointLeft(t.decimali()).stripTrailingZeros();
        Moneta m = new Moneta();
        String simbolo = nft ? t.simbolo() + " (" + t.indirizzo() + ")" : t.simbolo();
        if (scam) simbolo = simbolo + " **";
        m.InserisciValori(simbolo, (uscita ? q.negate() : q).toPlainString(), t.indirizzo(), nft ? "NFT" : "Crypto");
        m.SetNomeEsteso(t.simbolo());
        m.SetRete(RETE);
        return m;
    }

    /**
     * Le transazioni nella forma dell'importazione DeFi ({@code GUI_GestioneWallets.AggiornaWallets}), con i prezzi
     * DefiLlama scaricati prima a gruppi e le righe costruite solo quando l'importazione le chiede.
     */
    static Map<String, TransazioneDefi> ComeTransazioniDefi(String wallet, List<Transazione> transazioni) {
        PrezziImportDeFi.PreScarica(PrezziImportDeFi.RaccogliEtherFi(transazioni), "import ether.fi", null);
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Transazione t : transazioni) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = wallet;
            td.HashTransazione = t.hash();
            td.Blocco = String.valueOf(t.blocco());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(t.timestamp() * 1000);
            td.RighePronte = () -> Righe(wallet, t);
            mappa.put(wallet + "." + t.hash(), td);
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
    //=== ABBINAMENTO DELLE RICARICHE
    //=====================================================================================================

    /**
     * Collega ogni ricarica di un Safe al prelievo da cui parte (dal wallet dell'utente o da un exchange), che ha come
     * hash ({@code [24]}) quello indicato dall'evento {@code TopUp}: diventano un trasferimento fra wallet propri
     * (PTW/DTW, {@code [20]} scritto su tutti e due). Passata unica su tutto l'archivio, rifatta dopo ogni importazione:
     * il prelievo d'origine può arrivare dopo la ricarica. Una ricarica senza prelievo resta un deposito da classificare.
     *
     * @return quante ricariche ha abbinato
     */
    static int Abbina() {
        try {
            if (DatabaseH2.connectionPersonale == null || SafeRegistrati().isEmpty()) return 0;
            int n = Abbina(Principale.MappaCryptoWallet, SafeRegistrati());
            if (n > 0) Principale.TabellaCryptodaAggiornare = true;
            return n;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return 0;
        }
    }

    static int Abbina(Map<String, String[]> archivio, Set<String> safe) {
        Set<String> nomi = new HashSet<>();
        for (String s : safe) nomi.add((s + " (" + RETE + ")").toLowerCase());
        Map<String, List<String[]>> prelievi = new HashMap<>();
        List<String[]> ricariche = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 30) continue;
            String cat = Categoria(v);
            if (nomi.contains(v[3].trim().toLowerCase())) {
                if ("DC".equals(cat) && v[18].isBlank() && v[7].startsWith(CAUSALE_RICARICA)) ricariche.add(v);
            } else if ("PC".equals(cat) && v[18].isBlank() && v[24] != null && !v[24].isBlank()) {
                prelievi.computeIfAbsent(v[24].trim().toLowerCase(), k -> new ArrayList<>()).add(v);
            }
        }
        int abbinate = 0;
        for (String[] d : ricariche) {
            Matcher m = HASH.matcher(d[7]);
            if (!m.find()) continue;
            List<String[]> candidati = prelievi.get(m.group().toLowerCase());
            if (candidati == null) continue;
            String[] scelto = null;
            for (String[] p : candidati) {
                if (p[18].isBlank() && p[8].equalsIgnoreCase(d[11])) {
                    scelto = p;
                    break;
                }
            }
            if (scelto == null) continue;
            GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(scelto[0], d[0]);
            abbinate++;
        }
        return abbinate;
    }

    private static String Categoria(String[] v) {
        String[] parti = v[0].split("_");
        return parti[parti.length - 1].toUpperCase();
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    private static String Chiama(OKX_WalletCarta.Rpc rpc, String a, String dati) throws Exception {
        JsonObject c = new JsonObject();
        c.addProperty("to", a);
        c.addProperty("data", "0x" + dati);
        JsonArray par = new JsonArray();
        par.add(c);
        par.add("latest");
        return rpc.chiama("eth_call", par).getAsString();
    }

    /** Come {@link #Chiama}, ma una chiamata che il contratto rifiuta restituisce {@code null}. */
    private static String ChiamaSicura(OKX_WalletCarta.Rpc rpc, String a, String dati) {
        try {
            return Chiama(rpc, a, dati);
        } catch (Exception e) {
            return null;
        }
    }

    /** Un {@code uint256[]} (o {@code address[]}) dinamico del campo {@code data}, il cui offset è la parola {@code i}. */
    static List<BigInteger> Array(String dati, int i) {
        List<BigInteger> out = new ArrayList<>();
        try {
            int parola = Trans_XLayer.ParolaDati(dati, i).intValueExact() / 32;
            int n = Trans_XLayer.ParolaDati(dati, parola).intValueExact();
            for (int k = 0; k < n; k++) out.add(Trans_XLayer.ParolaDati(dati, parola + 1 + k));
        } catch (ArithmeticException e) {
            //offset o lunghezza fuori misura: dati non decodificabili
        }
        return out;
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

    static long Blocco(JsonObject l) {
        return Long.decode(Testo(l, "blockNumber"));
    }

    private static String Testo(JsonObject o, String campo) {
        return o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }
}
