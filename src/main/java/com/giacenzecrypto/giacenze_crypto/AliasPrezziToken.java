package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Token che, su una rete, si prezzano come un'altra moneta quotata sugli exchange: WETH su Base come
 * ETH, USDC.e su Arbitrum come USDC, WBNB come BNB. Il prezzo arriva così da CCXT (candele da un
 * minuto) invece che per address da DefiLlama/CoinGecko, che danno quotazioni orarie.
 * <p>L'elenco vive in {@code config/varie/AliasPrezziToken.json} e segue la strada di
 * {@link NoteCompilazione}: copia di default nel jar sotto {@code /Varie/}, installata al primo avvio da
 * {@link MappeCausali#InstallaDefaultSeMancanti()}, riallineata dal repository a ogni apertura. Un
 * aggiornamento scaricato vale dall'avvio <b>successivo</b>: la lettura avviene all'avvio, mentre
 * l'allineamento gira in background.
 * <p><b>Attenzione:</b> a differenza delle note di compilazione, questo file <b>cambia numeri fiscali</b> senza un cambio
 * di versione: il quadro RW valorizza al 31/12 dal vivo, quindi un alias nuovo cambia il valore di fine
 * anno di chi detiene quel token. Ogni modifica al file va trattata come una modifica fiscale.
 * <p>Riempie due mappe già lette da tutto il resto del programma, che non sa da dove arrivano:
 * {@link Principale#Mappa_AddressRete_Nome} (voci {@code alias}) e {@link Principale#Mappa_MoneteStessoPrezzo}
 * (voce {@code stessoPrezzo}, per simbolo, per le monete senza address valido). Le voci con
 * {@code riferimento} alimentano anche il controllo anti-impersonazione ({@link #MotivoImpersonazione}).
 * <p><b>Le dichiarazioni già fatte non devono cambiare.</b> Una voce può avere una data {@code dal}: fino a
 * quella data l'alias non vale e il token si prezza come prima (per address). Le voci aggiunte nell'anno N
 * nascono con {@code dal = 01/01/N}, così un aggiornamento del file non tocca mai un anno che qualcuno
 * può aver già dichiarato. L'opzione {@link #OPZIONE_ANCHE_ANNI_PASSATI} ("usa l'elenco completo anche per
 * gli anni passati") fa ignorare tutte le date: accesa di default solo su un'installazione nuova, dove non
 * c'è nessuna dichiarazione da proteggere ({@link #ValoreInizialeOpzione}). Per questo chi legge un alias
 * per decidere un prezzo deve passare da {@link #Alias} e {@link #StessoPrezzo}, con l'istante del prezzo,
 * e non dalle due mappe, che contengono tutte le voci senza data.
 * <p>Analisi completa in {@code nocommit/Documentazione/Analisi_Alias_Prezzi_Token_CCXT.md}.
 */
public class AliasPrezziToken {

    public static final String NOME = "AliasPrezziToken";

    /**
     * Voci di riferimento per chiave {@code address_RETE}: {@code [simboloToken, prezzoDa]}. Sono il token
     * che su quella rete "è" quella moneta: il canonico dell'emittente, oppure la versione bridged dove un
     * canonico non c'è (USDT su BSC e su CRO, WETH su Base). Solo contro queste ha senso cercare
     * un'imitazione.
     */
    static Map<String, String[]> Riferimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Da quando vale ciascun alias ({@code address_RETE} → epoch ms); una voce assente vale sempre. */
    static Map<String, Long> DalAlias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    /** Da quando vale ciascuna voce di {@code stessoPrezzo}; una voce assente vale sempre. */
    static Map<String, Long> DalStessoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Chiave in {@code personale.mv.db}: "SI" = ignora le date {@code dal}, usa l'elenco completo per ogni anno. */
    public static final String OPZIONE_ANCHE_ANNI_PASSATI = "AliasPrezzi_AncheAnniPassati";
    /** Valore corrente dell'opzione, letto all'avvio e aggiornato dalla casella in Opzioni. */
    static volatile boolean AncheAnniPassati = false;

    /** Le date {@code dal} sono giorni di calendario italiani, come tutte le date del programma. */
    private static final ZoneId FUSO = ZoneId.of("Europe/Rome");

    /** Il contenuto del file, già interpretato. */
    record Tabelle(Map<String, String> alias, Map<String, String[]> riferimenti, Map<String, String> stessoPrezzo,
                   Map<String, Long> dalAlias, Map<String, Long> dalStessoPrezzo) {
    }

    /**
     * Legge il file (disco, poi copia nel jar) e riempie le mappe. Se non è disponibile da nessuna
     * parte usa l'elenco minimo scritto qui sotto, che è quello che il programma aveva prima del file:
     * senza alias i token noti tornerebbero a DefiLlama in silenzio, e il ripiego va almeno scritto nel log.
     */
    public static void Carica() {
        Tabelle t = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, AliasPrezziToken::Interpreta);
        if (t == null) {
            LoggerGC.ScriviErrore("AliasPrezziToken: " + NOME + ".json non disponibile, uso l'elenco minimo interno");
            t = Predefinite();
        }
        Applica(t);
    }

    /**
     * Rende attive le tabelle indicate. Separato da {@link #Carica()} perché i test devono poter mettere
     * a confronto il file con l'elenco storico ({@link #Predefinite()}) senza passare dal disco.
     */
    static void Applica(Tabelle t) {
        Principale.Mappa_AddressRete_Nome.clear();
        Principale.Mappa_AddressRete_Nome.putAll(t.alias());
        Principale.Mappa_MoneteStessoPrezzo.clear();
        Principale.Mappa_MoneteStessoPrezzo.putAll(t.stessoPrezzo());
        Map<String, String[]> riferimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        riferimenti.putAll(t.riferimenti());
        Riferimenti = riferimenti;
        Map<String, Long> dalAlias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        dalAlias.putAll(t.dalAlias());
        DalAlias = dalAlias;
        Map<String, Long> dalStessoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        dalStessoPrezzo.putAll(t.dalStessoPrezzo());
        DalStessoPrezzo = dalStessoPrezzo;
    }

    /**
     * Il primo istante in cui una voce datata comincia a valere, oppure {@code null} se nessuna voce ha una
     * data. Ogni anno d'imposta il cui fine anno (valorizzato all'01/01 successivo alle 00:00) non lo supera
     * deve dare, con l'opzione spenta, gli stessi valori dell'elenco storico.
     */
    static Long PrimoDal() {
        Long primo = null;
        for (Long d : DalAlias.values()) if (primo == null || d < primo) primo = d;
        for (Long d : DalStessoPrezzo.values()) if (primo == null || d < primo) primo = d;
        return primo;
    }

    /**
     * Il simbolo con cui prezzare un token con address alla data indicata, oppure {@code null} se il token
     * non ha alias <b>a quella data</b> (non in elenco, oppure {@code istante} precedente al suo {@code dal}
     * con l'opzione spenta): in quel caso il prezzo si cerca per address, come prima dell'alias.
     */
    static String Alias(String address, String rete, long istante) {
        String chiave = address + "_" + rete;
        String alias = Principale.Mappa_AddressRete_Nome.get(chiave);
        if (alias == null) return null;
        return ValeAllaData(DalAlias.get(chiave), istante) ? alias : null;
    }

    /**
     * Il simbolo da cercare al posto di {@code simbolo} alla data indicata (WETH → ETH), oppure
     * {@code simbolo} stesso se non ha una voce {@code stessoPrezzo} valida a quella data.
     */
    static String StessoPrezzo(String simbolo, long istante) {
        if (simbolo == null) return null;
        String altro = Principale.Mappa_MoneteStessoPrezzo.get(simbolo);
        if (altro == null) return simbolo;
        return ValeAllaData(DalStessoPrezzo.get(simbolo), istante) ? altro : simbolo;
    }

    /**
     * Strettamente <b>dopo</b> l'inizio del giorno {@code dal}, non da quell'istante compreso: il quadro RW
     * valorizza la fine dell'anno N all'istante {@code 01/01/N+1 00:00} (Calcoli_RW, {@code fine}), che è
     * anche l'istante del valore iniziale dell'anno N+1. Con {@code dal = 2026-01-01} il valore finale 2025
     * resta quindi quello di prima, e l'iniziale 2026 coincide con lui invece di differire di qualche
     * centesimo.
     */
    private static boolean ValeAllaData(Long dal, long istante) {
        return dal == null || AncheAnniPassati || istante > dal;
    }

    /**
     * Valore iniziale dell'opzione {@link #OPZIONE_ANCHE_ANNI_PASSATI}, da usare solo quando nel database
     * personale non c'è ancora: accesa se l'installazione è nuova, cioè non esiste ancora un archivio di
     * movimenti (file assente o vuoto). Chi aggiorna ha dei movimenti e la trova spenta: i valori degli anni
     * già dichiarati restano quelli di prima.
     */
    static boolean ValoreInizialeOpzione(Path fileMovimenti) {
        try {
            return !Files.exists(fileMovimenti) || Files.size(fileMovimenti) == 0;
        } catch (java.io.IOException ex) {
            return false;
        }
    }

    /**
     * Legge l'opzione dal database personale; se manca (primo avvio di una versione che la conosce) la
     * scrive col valore di {@link #ValoreInizialeOpzione}. Da chiamare all'avvio, dopo l'apertura dei database.
     */
    public static boolean InizializzaOpzione() {
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ANCHE_ANNI_PASSATI);
        if (v == null) {
            v = ValoreInizialeOpzione(Path.of(VarStatiche.getFile_CryptoWallet())) ? "SI" : "NO";
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ANCHE_ANNI_PASSATI, v);
            System.out.println("AliasPrezziToken: opzione " + OPZIONE_ANCHE_ANNI_PASSATI + " inizializzata a " + v);
        }
        AncheAnniPassati = "SI".equalsIgnoreCase(v);
        return AncheAnniPassati;
    }

    /** Cambia l'opzione da interfaccia e la salva. */
    public static void ImpostaAncheAnniPassati(boolean attiva) {
        AncheAnniPassati = attiva;
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ANCHE_ANNI_PASSATI, attiva ? "SI" : "NO");
    }

    /** {@code "2026-01-01"} → inizio di quel giorno a Roma, in epoch ms; {@code null} se il campo manca. */
    private static Long Dal(JSONObject v) {
        String dal = v.optString("dal", "").trim();
        if (dal.isEmpty()) return null;
        return LocalDate.parse(dal).atStartOfDay(FUSO).toInstant().toEpochMilli();
    }

    /**
     * Interpreta il JSON. Una voce con rete, address o {@code prezzoDa} mancanti, o con un address non
     * valido per la sua rete, viene saltata e segnalata: una riga sbagliata non deve far cadere le altre.
     *
     * @return le tabelle, oppure {@code null} se il contenuto non è interpretabile o non ha nessuna voce valida
     */
    static Tabelle Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) {
            return null;
        }
        try {
            JSONObject radice = new JSONObject(contenuto);
            Map<String, String> alias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, String[]> riferimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, Long> dalAlias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, Long> dalStessoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            JSONArray voci = radice.getJSONArray("alias");
            for (int i = 0; i < voci.length(); i++) {
                JSONObject v = voci.getJSONObject(i);
                String rete = v.optString("rete", "").trim();
                String address = v.optString("address", "").trim();
                String prezzoDa = v.optString("prezzoDa", "").trim();
                if (rete.isEmpty() || prezzoDa.isEmpty() || !Funzioni_WalletDeFi.isValidAddress(address, rete)) {
                    LoggerGC.ScriviErrore("AliasPrezziToken: voce " + i + " di " + nome + ".json non valida, saltata");
                    continue;
                }
                Long dal;
                try {
                    dal = Dal(v);
                } catch (RuntimeException ex) {
                    //Una data illeggibile non deve diventare "vale sempre": la voce si salta
                    LoggerGC.ScriviErrore("AliasPrezziToken: voce " + i + " di " + nome + ".json con data 'dal' non valida, saltata");
                    continue;
                }
                String chiave = address + "_" + rete;
                alias.put(chiave, prezzoDa);
                if (dal != null) dalAlias.put(chiave, dal);
                //Senza il campo vale il tipo: un canonico e' sempre il riferimento della sua moneta
                if (v.optBoolean("riferimento", v.optString("tipo", "").equalsIgnoreCase("canonico"))) {
                    riferimenti.put(chiave, new String[]{v.optString("simboloToken", "").trim(), prezzoDa});
                }
            }
            Map<String, String> stessoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            JSONObject sp = radice.optJSONObject("stessoPrezzo");
            if (sp != null) {
                //Il valore e' il simbolo di prezzo, oppure {"prezzoDa": ..., "dal": ...} per una voce con data
                for (String simbolo : sp.keySet()) {
                    JSONObject voce = sp.optJSONObject(simbolo);
                    if (voce == null) {
                        stessoPrezzo.put(simbolo, sp.getString(simbolo));
                        continue;
                    }
                    String prezzoDa = voce.optString("prezzoDa", "").trim();
                    Long dal;
                    try {
                        dal = Dal(voce);
                    } catch (RuntimeException ex) {
                        dal = null;
                        prezzoDa = "";
                    }
                    if (prezzoDa.isEmpty()) {
                        LoggerGC.ScriviErrore("AliasPrezziToken: stessoPrezzo '" + simbolo + "' di " + nome + ".json non valido, saltato");
                        continue;
                    }
                    stessoPrezzo.put(simbolo, prezzoDa);
                    if (dal != null) dalStessoPrezzo.put(simbolo, dal);
                }
            }
            return alias.isEmpty() ? null : new Tabelle(alias, riferimenti, stessoPrezzo, dalAlias, dalStessoPrezzo);
        } catch (RuntimeException ex) {
            LoggerGC.ScriviErrore("AliasPrezziToken: " + nome + ".json non interpretabile : " + ex);
            return null;
        }
    }

    /** L'elenco che era scritto in {@code VarCondivise}/{@code Prezzi} prima del file: solo ripiego. */
    static Tabelle Predefinite() {
        Map<String, String> alias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        alias.put("0x66e428c3f67a68878562e79A0234c1F83c208770_CRO", "USDT");
        alias.put("0x55d398326f99059fF775485246999027B3197955_BSC", "USDT");
        alias.put("0xc21223249CA28397B4B6541dfFaEcC539BfF0c59_CRO", "USDC");
        alias.put("0xC74D59A548ecf7fc1754bb7810D716E9Ac3e3AE5_CRO", "BUSD");
        alias.put("0x062E66477Faf219F25D27dCED647BF57C3107d52_CRO", "BTC");
        alias.put("0xe44Fd7fCb2b1581822D0c862B68222998a0c299a_CRO", "ETH");
        alias.put("0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56_BSC", "BUSD");
        alias.put("0xF2001B145b43032AAF5Ee2884e456CCd805F677D_CRO", "DAI");
        alias.put("0x4200000000000000000000000000000000000006_BASE", "ETH");
        alias.put("0x6969696969696969696969696969696969696969_BERA", "BERA");
        alias.put("0x549943e04f40284185054145c6E4e9568C1D3241_BERA", "USDC");
        alias.put("0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9_ARB", "USDT");
        Map<String, String> stessoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        stessoPrezzo.put("WCRO", "CRO");
        stessoPrezzo.put("WETH", "ETH");
        stessoPrezzo.put("XDAI", "DAI");
        //Tutte di riferimento, col simbolo di prezzo come simbolo on-chain: e' esattamente il confronto che
        //faceva il controllo anti-impersonazione prima del file, che nel ripiego non deve spegnersi
        Map<String, String[]> riferimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> e : alias.entrySet()) {
            riferimenti.put(e.getKey(), new String[]{e.getValue(), e.getValue()});
        }
        return new Tabelle(alias, riferimenti, stessoPrezzo, new TreeMap<>(), new TreeMap<>());
    }

    /**
     * Come {@link #MotivoImpersonazione}, ma un token il cui address+rete è censito da CoinGecko
     * ({@code GESTITICOINGECKO}, la lista scaricata una volta al giorno) non è mai un'imitazione: esiste un
     * progetto vero dietro quel contratto. È il caso di un bridged legittimo che l'elenco non contiene, come
     * l'ETH di Wormhole su BSC, che altrimenti verrebbe marcato SCAM perché si chiama come il riferimento.
     * <p>Il confronto è per <b>address</b>, mai per simbolo: per simbolo la lista contiene sempre un "USDT", e
     * l'eccezione coprirebbe proprio le imitazioni. Vale solo per questa regola: il controllo sul nome e
     * GoPlus restano in vigore anche per i token censiti, perché essere su CoinGecko dice che il token
     * esiste, non che è sicuro. Con la lista vuota (primo avvio senza rete) non esclude nulla.
     * <p>Sui dati reali (2026-09-26) dei 124 token marcati SCAM a mano solo 2 sono nella lista, e nessuno
     * dei due imita una moneta dell'elenco.
     */
    static String MotivoImpersonazioneEsclusiCensiti(String nomeMoneta, String address, String rete) {
        if (address != null && rete != null && DatabaseH2.GestitiCoingecko_Leggi(address + "_" + rete) != null) {
            return null;
        }
        return MotivoImpersonazione(nomeMoneta, address, rete);
    }

    /**
     * Verifica gratuita (nessuna chiamata API) di impersonazione: un token con lo stesso simbolo del token
     * <b>di riferimento</b> della stessa rete, ma con un address diverso, è quasi certamente contraffatto.
     * <p>Tre restrizioni, tutte volute, perché il chiamante marca SCAM in automatico e un token marcato
     * esce dai calcoli:
     * <ul>
     *   <li>solo le voci {@code riferimento}: dove esiste il canonico, una versione bridged legittima
     *       (USDC.e) ha spesso il simbolo della moneta vera, e confrontarsi con quella produrrebbe falsi
     *       positivi. Dove il canonico non c'è, il riferimento è il bridged che tutti usano (USDT su BSC),
     *       altrimenti il controllo si spegnerebbe proprio sulle imitazioni più comuni;</li>
     *   <li>un token che è a sua volta nell'elenco (anche come bridged) non è mai un'imitazione;</li>
     *   <li>l'address deve essere valido per la rete: la moneta nativa non ha un address di contratto.</li>
     * </ul>
     * Si confronta il nome con il simbolo on-chain del riferimento <b>e</b> con quello della moneta di
     * prezzo: un finto "USDT" su una rete dove il vero si chiama USDT0 è l'imitazione più comune, e
     * senza il secondo confronto sfuggirebbe.
     *
     * @return il motivo, oppure {@code null} se non c'è impersonazione
     */
    static String MotivoImpersonazione(String nomeMoneta, String address, String rete) {
        if (nomeMoneta == null || address == null || rete == null) return null;
        if (!Funzioni_WalletDeFi.isValidAddress(address, rete)) return null;
        String chiave = address + "_" + rete;
        if (Principale.Mappa_AddressRete_Nome.containsKey(chiave)) return null;
        for (Map.Entry<String, String[]> entry : Riferimenti.entrySet()) {
            String chiaveNota = entry.getKey();
            String reteNota = chiaveNota.substring(chiaveNota.lastIndexOf('_') + 1);
            if (!reteNota.equalsIgnoreCase(rete)) continue;
            String simboloToken = entry.getValue()[0];
            String prezzoDa = entry.getValue()[1];
            if (nomeMoneta.equalsIgnoreCase(simboloToken) || nomeMoneta.equalsIgnoreCase(prezzoDa)) {
                return "simbolo \"" + nomeMoneta + "\" coincide con il token noto " + simboloToken
                        + " sulla stessa rete (" + chiaveNota + ") ma con address diverso: possibile impersonazione";
            }
        }
        return null;
    }
}
