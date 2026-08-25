package com.giacenzecrypto.giacenze_crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

/**
 * Motore di backup e ripristino dell'intero archivio: produce un file zip autosufficiente da cui è
 * possibile ricostruire, su un'installazione nuova, tutto ciò che serve a ristampare un quadro RW/RT
 * identico all'originale.
 *
 * <p>Segue lo schema delle altre estrazioni ({@code DocumentiFonte}, {@code Principale_GiacenzeaData}) :
 * metodi {@code public static}, nessun campo Swing, nessun riferimento a {@link Principale}. La parte
 * grafica sta in {@link GUI_Backup_Pannello}, l'orchestrazione dei pulsanti in {@link Principale_Backup},
 * il controllo di compatibilità in {@link Backup_Compatibilita}.
 *
 * <h2>Cosa entra nel backup, e perché proprio questo</h2>
 *
 * L'elenco non è "tutto quello che c'è": è stato ricavato censendo che cosa i motori fiscali leggono
 * davvero. Tre conseguenze non ovvie, documentate per esteso in
 * {@code nocommit/Documentazione/Analisi_Backup_Ripristino.md} :
 *
 * <ul>
 *   <li><b>Il DB prezzi non serve tutto.</b> {@code Calcoli_PlusvalenzeNew} non richiama {@code Prezzi}
 *       da nessuna parte — la plusvalenza realizzata esce dai controvalori già scritti nelle righe dei
 *       movimenti. Le uniche letture vive sono ai <b>confini d'anno</b> ({@code Calcoli_RW:540} e
 *       {@code :1794}, {@code Calcoli_RT:909/1344/1377}) e alla <b>data dei soli movimenti reward con
 *       controvalore azzerato</b> ({@code Calcoli_RW:1297}). Da qui {@link #IntervalliPrezziNecessari()},
 *       che su un archivio da 100.000 movimenti riduce 15 milioni di righe a circa un milione.</li>
 *   <li><b>Le tabelle si esportano, non si copiano i file.</b> Le tre connessioni H2 restano aperte e
 *       tengono il lock di istanza singola, quindi i {@code .mv.db} non sono né copiabili né
 *       sostituibili a caldo. In più {@code connectionPrezzi} è aperta con {@code AUTO_COMPACT_FILL_RATE=0}
 *       e {@code RETENTION_TIME=0} : il file cresce senza mai compattarsi e contiene molti più byte dei
 *       dati che rappresenta (misurati 2,8 GB di file per 86 MB di dati compressi).</li>
 *   <li><b>{@code database.mv.db} non è solo cache.</b> {@code RINOMINATOKEN} contiene le marcature SCAM
 *       e {@code PROVIDERDEFI} le preferenze per rete : sono dati dell'utente e vanno sempre salvati.
 *       {@code GOPLUSSECURITY} e {@code TOKENSOLANA} sono cache, ma accumulate un token alla volta sullo
 *       storico dell'utente : stanno in un gruppo opzionale ({@link Gruppo#CACHE_TOKEN}), acceso per
 *       default. I registri {@code GESTITI*} sono invece <b>fuori dal backup</b> : sono la fotografia di
 *       un listino che cambia di giorno in giorno, e ripristinarli da un archivio vecchio rimetterebbe
 *       in circolo un elenco superato.</li>
 * </ul>
 *
 * <h2>Le credenziali</h2>
 *
 * {@code EXCHANGEAPI} e le opzioni {@code ApiKey_*} contengono chiavi in chiaro : sono un gruppo a sé
 * ({@link Gruppo#CHIAVI_API}), <b>escluso per default</b>. Un backup che le includesse diventerebbe, una
 * volta esportato su una chiavetta o su un cloud, un singolo punto di compromissione degli account —
 * lo stesso motivo per cui {@code DocumentiFonte.UrlSenzaChiave} le redige dagli NDJSON. Non servono a
 * ristampare RW/RT : servono solo a riscaricare i movimenti.
 */
public class Backup_Restore {

    /**
     * Versione del formato dell'archivio. Va incrementata solo se cambia la <b>struttura</b> dello zip
     * (nomi delle cartelle interne, semantica del manifest), non quando cambiano le tabelle: quelle si
     * confrontano una per una in {@link Backup_Compatibilita}.
     */
    public static final int FORMATO_BACKUP = 1;

    /**
     * Semiampiezza della finestra temporale intorno a ogni istante utile, per l'estrazione dei prezzi
     * necessari.
     *
     * <p>Un'ora, non i 15 minuti della tolleranza massima di {@code Prezzi.DammiPrezzoTransazione}
     * (censite tutte le chiamate: 2, 3, 10, 15 e nulla oltre). Il margine non è decorativo: gli istanti
     * di confine d'anno stanno a cavallo della mezzanotte del 31/12 — l'inizio dell'anno N è un minuto
     * dopo la fine dell'anno N−1 — e il database dei prezzi contiene istanti in UTC accanto a istanti
     * convertiti con {@code Europe/Rome}, dove un'ora è esattamente l'ordine di grandezza dell'offset.
     * Sul set necessario il margine costa pochi MB.
     */
    static final long FINESTRA_PREZZI_MS = 60L * 60L * 1000L;

    /** Nome della voce del manifest dentro lo zip, e suffisso del file affiancato. */
    static final String NOME_MANIFEST = "manifest.json";

    private static final DateTimeFormatter FMT_NOME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FMT_LEGGIBILE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Campi della riga di movimento che il motore delle plusvalenze <b>ricalcola</b>, e su cui si basa la
     * verifica post-ripristino ({@link #ImprontaPlusvalenze()}).
     *
     * <p>Sono i quattro scritti insieme in {@code Calcoli_PlusvalenzeNew} (vecchio e nuovo prezzo di
     * carico, plusvalenza, flag di calcolo). <b>Non</b> comprendono {@code v[38]} e {@code v[31]}, che
     * sono <i>portati</i> e non ricalcolati — il motore su {@code [38]} toglie solo {@code E}/{@code M} e
     * aggiunge {@code A}, e {@code [31]} viene solo scritto, mai azzerato. Includerli darebbe falsi
     * positivi a ogni verifica, per lo stesso motivo per cui esiste {@code riferimentoConModifica()} nel
     * test di equivalenza incrementale.
     */
    static final int[] CAMPI_CALCOLATI = {16, 17, 19, 33};

    /**
     * Gruppi che devono essere ripristinati <b>insieme ai movimenti</b> perché il confronto delle
     * impronte significhi qualcosa (vedi {@link #GruppiMancantiPerVerifica}).
     *
     * <p>Il discrimine non è "questo gruppo entra nel calcolo" — ci entrano tutti — ma <b>dato
     * dell'utente contro cache riproducibile</b>:
     * <ul>
     *   <li>Opzioni di calcolo (comprese le personalizzazioni e i tassi USD/EUR), gruppi wallet e token
     *       EMoney, prezzi personalizzati: sono <i>impostazioni dell'utente</i>. Non ripristinarle
     *       significa far girare il motore su <b>ingressi diversi</b> da quelli del backup, quindi una
     *       divergenza non dimostra niente — è la conseguenza voluta della scelta fatta.</li>
     *   <li>Cache dei prezzi di mercato ({@link Gruppo#PREZZI}): si sovrappone invece di cancellare, e un
     *       prezzo risolto diversamente è <b>esattamente</b> il fallimento invisibile per cui l'impronta
     *       esiste. Lì la verifica resta, e la sua eventuale diagnosi cambia soltanto di testo.</li>
     * </ul>
     * Le marcature SCAM non sono nell'elenco di proposito: {@code Funzioni.isSCAM} legge il nome della
     * moneta <i>nella riga del movimento</i>, quindi lo stato arriva già con i movimenti.
     */
    private static final Set<Gruppo> GRUPPI_VERIFICA = EnumSet.of(
            Gruppo.OPZIONI_CALCOLO, Gruppo.GRUPPI_WALLET, Gruppo.PREZZI_PERSONALIZZATI);

    /**
     * Cosa manca perché il confronto delle impronte delle plusvalenze sia sensato.
     *
     * <p>Predicato puro, così è verificabile senza schermo. Insieme vuoto = la verifica può essere fatta.
     *
     * <p>Il primo requisito è che i <b>movimenti</b> siano stati davvero riscritti, e per questo il
     * parametro è il flag e non l'appartenenza al gruppo: se l'archivio non contiene la voce, il
     * ripristino la salta senza errore e {@code Principale} non ricarica {@code MappaCryptoWallet}, per
     * cui si confronterebbe l'impronta del backup con quella dei movimenti locali — falso allarme
     * garantito. Senza movimenti ripristinati l'impronta non è nemmeno confrontabile in linea di
     * principio: descrive un altro insieme di movimenti.
     *
     * @param Gruppi i gruppi effettivamente ripristinati
     * @param MovimentiRipristinati {@code true} se il file dei movimenti è stato riscritto
     * @return i gruppi mancanti, in ordine di dichiarazione; vuoto se la verifica è possibile
     */
    public static Set<Gruppo> GruppiMancantiPerVerifica(Set<Gruppo> Gruppi, boolean MovimentiRipristinati) {
        Set<Gruppo> mancanti = EnumSet.noneOf(Gruppo.class);
        if (!MovimentiRipristinati) {
            mancanti.add(Gruppo.MOVIMENTI);
        }
        for (Gruppo g : GRUPPI_VERIFICA) {
            if (Gruppi == null || !Gruppi.contains(g)) {
                mancanti.add(g);
            }
        }
        return mancanti;
    }

    /** Opzioni che non hanno senso su un'altra installazione, o che sono pura cache di sessione. */
    private static final Set<String> OPZIONI_ESCLUSE = Set.of(
            "Tema", "Finestra_Larghezza", "Finestra_Altezza", "Finestra_Massimizzata",
            "Directory_ImportazioniGestione", "Versione", "GiacenzeAData_Data",
            "Data_Lista_Coinbase", "Data_Lista_Coingecko", "Data_Lista_CoinMarketCap");

    /** Prefisso delle opzioni che contengono credenziali: seguono il gruppo {@link Gruppo#CHIAVI_API}. */
    private static final String PREFISSO_CHIAVI = "ApiKey_";

    // =================================================================================================
    // GRUPPI DI CONTENUTO
    // =================================================================================================
    /**
     * Unità selezionabili in fase di ripristino. Sono <b>gruppi coerenti</b> e non singole tabelle: alcune
     * cose non hanno senso separate (i movimenti senza il registro dei documenti lascerebbero il campo
     * {@code [41]} a puntare nel vuoto).
     */
    public enum Gruppo {
        /** Movimenti crypto, registro {@code DOCUMENTIFONTE} e copie dei file di origine: unità inscindibile. */
        MOVIMENTI("Movimenti crypto, documenti di origine e registro"),
        /** Fiat wallet, card wallet e causali personalizzate di Crypto.com. */
        CDC("Dati Crypto.com (fiat wallet, card wallet, causali)"),
        /** Opzioni di calcolo e personalizzazioni, tassi USD/EUR, configurazioni di import. */
        OPZIONI_CALCOLO("Opzioni di calcolo e personalizzazioni"),
        /** Wallet, gruppi wallet, alias, token EMoney: cambiano direttamente i risultati RW. */
        GRUPPI_WALLET("Wallet, gruppi wallet, alias e token EMT"),
        /** Prezzi inseriti a mano dall'utente. */
        PREZZI_PERSONALIZZATI("Prezzi personalizzati"),
        /** Cache dei prezzi di mercato, al livello contenuto nel backup, più i prezzi irrecuperabili. */
        PREZZI("Prezzi di mercato"),
        /** Marcature SCAM ({@code RINOMINATOKEN}) e preferenze provider DeFi. */
        SCAM_DEFI("Marcature SCAM e preferenze provider DeFi"),
        /** Chiavi API degli exchange e degli explorer. */
        CHIAVI_API("Chiavi API degli exchange e degli explorer"),
        /**
         * Cache accumulate <b>un token alla volta</b> sullo storico dell'utente: l'analisi di sicurezza
         * GoPlusLabs ({@code GOPLUSSECURITY}) e l'identità dei mint Solana ({@code TOKENSOLANA}).
         *
         * <p>Non vanno confuse con i registri {@code GESTITI*} (Binance, Coinbase, Coingecko,
         * CoinMarketCap, CryptoHistory), che sono <b>fuori dal backup</b>: quelli sono la fotografia di
         * un listino, riscritti in blocco da {@code …ScriviNuovaTabella()} e cambiano di giorno in
         * giorno, quindi ripristinarli da un archivio vecchio rimetterebbe in circolo un elenco
         * superato. Queste due invece crescono per incontro — solo i token che l'utente ha davvero
         * attraversato — e non invecchiano: il simbolo di un mint non cambia domani. Ricostruirle costa
         * una interrogazione per token a GoPlusLabs e a Helius, quest'ultima a carico della API key
         * dell'utente, e se la risposta non arriva il token resta {@code N/A}.
         */
        CACHE_TOKEN("Cache dei token analizzati (GoPlus, Solana)");

        /** Etichetta dell'edizione completa. Si legge con {@link #Etichetta()}, non direttamente. */
        private final String Etichetta;

        Gruppo(String Etichetta) {
            this.Etichetta = Etichetta;
        }

        /**
         * Etichetta da mostrare nelle caselle del pannello.
         *
         * <p>Coincide con quella dichiarata tranne che per {@link #CHIAVI_API} nell'edizione Store, dove
         * le chiavi degli exchange non esistono ({@link VarStatiche#EdizioneStore()}) e annunciarle
         * sarebbe falso: lì restano solo quelle degli explorer e dei fornitori di dati. È un metodo e non
         * un campo proprio perché nessuno possa aggirare questa distinzione leggendo l'etichetta grezza.
         */
        public String Etichetta() {
            if (this == CHIAVI_API && VarStatiche.EdizioneStore()) {
                return "Chiavi API degli explorer e dei fornitori di dati";
            }
            return Etichetta;
        }
    }

    /** Un database fra i tre aperti, indicato per nome così da poter finire dentro il manifest. */
    private enum DB {
        PERSONALE, PRINCIPALE, PREZZI;

        Connection Connessione() {
            switch (this) {
                case PERSONALE:
                    return DatabaseH2.connectionPersonale;
                case PRINCIPALE:
                    return DatabaseH2.connection;
                default:
                    return DatabaseH2.connectionPrezzi;
            }
        }

        String Cartella() {
            return name().toLowerCase();
        }
    }

    /** Descrittore di una tabella da salvare: dove sta, in che gruppo, e con quale eventuale filtro di riga. */
    private static final class Tabella {

        final String Nome;
        final DB Db;
        final Gruppo Gr;
        /** Filtro applicato alla riga in uscita; {@code null} per esportarla intera. */
        final Predicate<String[]> Filtro;

        Tabella(String Nome, DB Db, Gruppo Gr) {
            this(Nome, Db, Gr, null);
        }

        Tabella(String Nome, DB Db, Gruppo Gr, Predicate<String[]> Filtro) {
            this.Nome = Nome;
            this.Db = Db;
            this.Gr = Gr;
            this.Filtro = Filtro;
        }

        /** Chiave con cui la tabella compare nel manifest e nello zip, es. {@code personale/WALLETGRUPPO}. */
        String Chiave() {
            return Db.Cartella() + "/" + Nome;
        }
    }

    /**
     * Le tabelle salvate, con il gruppo di appartenenza.
     *
     * <p>{@code OPZIONI} compare due volte, una per database, e non è una svista: quella di
     * {@code personale.mv.db} contiene le opzioni di calcolo, quella di {@code database.mv.db} i duplicati
     * storici più le {@code ApiKey_*} degli explorer. Nello zip finiscono in cartelle diverse, quindi non
     * si sovrappongono.
     */
    private static List<Tabella> Tabelle(boolean ChiaviApi) {
        //Le OPZIONI si filtrano per riga: fuori quelle legate alla macchina, e fuori le credenziali
        //quando il gruppo CHIAVI_API non è stato richiesto.
        Predicate<String[]> filtroOpzioni = r -> {
            String k = r.length > 0 && r[0] != null ? r[0] : "";
            if (OPZIONI_ESCLUSE.contains(k)) {
                return false;
            }
            return ChiaviApi || !k.startsWith(PREFISSO_CHIAVI);
        };

        List<Tabella> t = new ArrayList<>();
        //--- personale.mv.db : è tutto dato dell'utente
        t.add(new Tabella("OPZIONI", DB.PERSONALE, Gruppo.OPZIONI_CALCOLO, filtroOpzioni));
        t.add(new Tabella("PrezziNew", DB.PERSONALE, Gruppo.PREZZI_PERSONALIZZATI));
        t.add(new Tabella("XXXEUR", DB.PERSONALE, Gruppo.PREZZI_PERSONALIZZATI));
        t.add(new Tabella("Prezzo_ora_Address_Chain", DB.PERSONALE, Gruppo.PREZZI_PERSONALIZZATI));
        t.add(new Tabella("WALLETS", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        t.add(new Tabella("WALLETGRUPPO", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        t.add(new Tabella("GRUPPO_ALIAS", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        t.add(new Tabella("EMONEY", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        t.add(new Tabella("GIACENZEBLOCKCHAIN", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        t.add(new Tabella("EXCHANGETOKENS", DB.PERSONALE, Gruppo.GRUPPI_WALLET));
        //Il registro dei documenti sta nel gruppo dei movimenti: il campo [41] di ogni movimento ne
        //referenzia l'Id, separarli lascerebbe quei riferimenti appesi
        t.add(new Tabella("DOCUMENTIFONTE", DB.PERSONALE, Gruppo.MOVIMENTI));
        //Nell'edizione Store la tabella non esiste (vedi VarStatiche.EdizioneStore()): fuori da questo
        //elenco vuol dire che non viene salvata e che un archivio prodotto dall'edizione completa la
        //salta al ripristino, con l'avviso già previsto da Backup_Compatibilita. Il gruppo CHIAVI_API
        //resta comunque, perché contiene anche le opzioni ApiKey_* di explorer e fornitori di dati.
        if (!VarStatiche.EdizioneStore()) {
            t.add(new Tabella("EXCHANGEAPI", DB.PERSONALE, Gruppo.CHIAVI_API));
        }

        //--- database.mv.db : in gran parte cache, ma non tutto
        t.add(new Tabella("OPZIONI", DB.PRINCIPALE, Gruppo.OPZIONI_CALCOLO, filtroOpzioni));
        t.add(new Tabella("RINOMINATOKEN", DB.PRINCIPALE, Gruppo.SCAM_DEFI));
        t.add(new Tabella("PROVIDERDEFI", DB.PRINCIPALE, Gruppo.SCAM_DEFI));
        t.add(new Tabella("XXXEUR", DB.PRINCIPALE, Gruppo.PREZZI_PERSONALIZZATI));
        t.add(new Tabella("Prezzo_ora_Address_Chain", DB.PRINCIPALE, Gruppo.PREZZI_PERSONALIZZATI));
        t.add(new Tabella("GOPLUSSECURITY", DB.PRINCIPALE, Gruppo.CACHE_TOKEN));
        t.add(new Tabella("TOKENSOLANA", DB.PRINCIPALE, Gruppo.CACHE_TOKEN));
        //I registri GESTITIBINANCE, GESTITICOINBASE, GESTITICOINGECKO, GESTITICOINMARKETCAP e
        //GESTITICRYPTOHISTORY non sono nell'elenco apposta: vedi Gruppo.CACHE_TOKEN. Un archivio
        //prodotto da una versione precedente li contiene ancora, e il ripristino li ignora da solo,
        //perché scorre questo elenco e cerca la voce nello zip, non il contrario

        //--- prezzi.mv.db
        //PrezziKO è sempre inclusa: dichiara quali prezzi sono irrecuperabili, e questo cambia il
        //comportamento del calcolo, non è cache
        t.add(new Tabella("PrezziKO", DB.PREZZI, Gruppo.PREZZI));
        //PrezziNew della cache di mercato ha un trattamento a parte (vedi EsportaPrezzi)
        return t;
    }

    /** File piatti della directory di lavoro salvati nell'archivio, con il gruppo di appartenenza. */
    private static Map<String, Gruppo> FilePiatti() {
        Map<String, Gruppo> m = new LinkedHashMap<>();
        m.put(VarStatiche.getFile_CryptoWallet(), Gruppo.MOVIMENTI);
        m.put(VarStatiche.getFile_CDCFiatWallet(), Gruppo.CDC);
        m.put(VarStatiche.getFile_CDCCardWallet(), Gruppo.CDC);
        m.put(VarStatiche.getFile_CDCDatiDB(), Gruppo.CDC);
        m.put(VarStatiche.getFile_CDCFiatWallet_FileTipiMovimentiPers(), Gruppo.CDC);
        //I tassi USD/EUR entrano nei calcoli: senza, la conversione su una macchina nuova può divergere
        m.put(VarStatiche.getFileUSDEUR(), Gruppo.OPZIONI_CALCOLO);
        m.put(VarStatiche.getFile_ChatbotIA(), Gruppo.OPZIONI_CALCOLO);
        return m;
    }

    /** Cartelle di configurazione salvate nell'archivio. */
    private static Map<String, String> CartelleConfig() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ImportConfig", VarStatiche.getCartella_ImportConfig());
        m.put("config/import", VarStatiche.getCartella_ConfigImport());
        m.put("config/importmappe", VarStatiche.getCartella_ConfigImportMappe());
        m.put("config/loghi", VarStatiche.getCartella_ConfigLoghi());
        return m;
    }

    // =================================================================================================
    // OPZIONI DI BACKUP E MANIFEST
    // =================================================================================================
    /** Le tre scelte offerte all'utente al momento di creare un backup. */
    public static final class OpzioniBackup {

        /** Se {@code true} salva tutta la tabella dei prezzi anziché il solo sottoinsieme necessario. */
        public boolean PrezziCompleti = false;
        /**
         * Se {@code true} include le opzioni {@code ApiKey_*} e, nell'edizione completa, la tabella
         * {@code EXCHANGEAPI} (che l'edizione Store non ha). Default: no.
         */
        public boolean ChiaviApi = false;
        /** Se {@code true} include le cache token accumulate ({@link Gruppo#CACHE_TOKEN}). Default: sì. */
        public boolean CacheToken = true;
        /** {@code true} per i backup di sicurezza creati automaticamente prima di un ripristino. */
        public boolean Automatico = false;
    }

    /**
     * Contenuto del file {@code manifest.json}: tutto ciò che si vuole poter vedere <b>prima</b> di
     * ripristinare, più il contratto di compatibilità verificato da {@link Backup_Compatibilita}.
     */
    public static final class Manifest {

        /** Il file zip da cui il manifest è stato letto; {@code null} per un manifest appena costruito. */
        public File Archivio;
        public int FormatoBackup;
        public String Creato = "";
        public String Versione = "";
        /** Valore di {@code Importazioni.ColonneTabella} al momento del backup. */
        public int ColonneTabella;
        public int MovimentiTotali;
        public final Map<String, Integer> MovimentiPerAnno = new TreeMap<>();
        public final Map<String, Integer> MovimentiPerWallet = new TreeMap<>();
        public String PrimoMovimento = "";
        public String UltimoMovimento = "";
        public int Documenti;
        public long DocumentiByte;
        public int FileSupporto;
        public final Map<String, String> Impostazioni = new TreeMap<>();
        /** Valore di {@code Calcoli_PlusvalenzeNew.OpzioniRicalcolo.Epoca()} al momento del backup. */
        public long EpocaRicalcolo;
        /** Digest dei campi calcolati dei movimenti, per la verifica post-ripristino. */
        public String ImprontaPlusvalenze = "";
        /** {@code "necessari"} oppure {@code "completi"}; {@code "assenti"} se non ci sono prezzi. */
        public String LivelloPrezzi = "";
        public long RighePrezzi;
        public boolean ChiaviApi;
        public boolean CacheToken;
        public boolean Automatico;
        /** {@code true} se l'archivio è stato importato da fuori anziché prodotto qui. */
        public boolean Importato;
        /** Per ogni tabella salvata: righe e nomi di colonna, nell'ordine in cui sono nel CSV. */
        public final Map<String, TabellaSalvata> Tabelle = new LinkedHashMap<>();
        public long DimensioneCompressa;
        public long DimensioneOriginale;

        /** @return i gruppi effettivamente presenti in questo archivio */
        public Set<Gruppo> GruppiPresenti() {
            Set<Gruppo> g = EnumSet.noneOf(Gruppo.class);
            if (MovimentiTotali > 0 || Tabelle.containsKey("personale/DOCUMENTIFONTE")) {
                g.add(Gruppo.MOVIMENTI);
            }
            g.add(Gruppo.CDC);
            g.add(Gruppo.OPZIONI_CALCOLO);
            g.add(Gruppo.GRUPPI_WALLET);
            g.add(Gruppo.PREZZI_PERSONALIZZATI);
            g.add(Gruppo.SCAM_DEFI);
            if (!"assenti".equals(LivelloPrezzi)) {
                g.add(Gruppo.PREZZI);
            }
            if (ChiaviApi) {
                g.add(Gruppo.CHIAVI_API);
            }
            if (CacheToken) {
                g.add(Gruppo.CACHE_TOKEN);
            }
            return g;
        }
    }

    /** Una tabella dentro il manifest: quante righe e con quali colonne è stata scritta. */
    public static final class TabellaSalvata {

        public int Righe;
        public List<String> Colonne = new ArrayList<>();
    }

    // =================================================================================================
    // CREAZIONE DEL BACKUP
    // =================================================================================================
    /**
     * Crea un nuovo archivio di backup in {@code ArchivioBackup/}.
     *
     * <p>Scrive due file: lo zip e, affiancato, una copia del manifest con lo stesso nome base ed
     * estensione {@code .json}. La copia esterna non è ridondanza inutile — è ciò che permette a
     * {@link #Elenco()} di popolare la tabella del pannello senza aprire, uno per uno, archivi che
     * possono pesare decine di MB.
     *
     * @param Opzioni le scelte dell'utente
     * @param progress finestra di avanzamento da aggiornare, può essere {@code null}
     * @return il file zip creato, {@code null} se l'operazione è fallita o è stata interrotta
     */
    public static File Esegui(OpzioniBackup Opzioni, Download progress) {
        if (Opzioni == null) {
            Opzioni = new OpzioniBackup();
        }
        File cartella = new File(VarStatiche.getCartella_ArchivioBackup());
        if (!cartella.exists() && !cartella.mkdirs()) {
            System.out.println("Backup_Restore.Esegui : impossibile creare " + cartella);
            return null;
        }
        LocalDateTime adesso = LocalDateTime.now();
        String nomeBase = (Opzioni.Automatico ? "backup_auto_" : "backup_") + adesso.format(FMT_NOME);
        File zip = new File(cartella, nomeBase + ".zip");
        File json = new File(cartella, nomeBase + ".json");

        Manifest m = new Manifest();
        m.FormatoBackup = FORMATO_BACKUP;
        m.Creato = adesso.format(FMT_LEGGIBILE);
        m.Versione = VarStatiche.Versione;
        m.ColonneTabella = Importazioni.ColonneTabella;
        m.ChiaviApi = Opzioni.ChiaviApi;
        m.CacheToken = Opzioni.CacheToken;
        m.Automatico = Opzioni.Automatico;

        boolean completato = false;
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.setLevel(Deflater.BEST_SPEED);

            Fase(progress, "Riepilogo dei movimenti...", 1, 6);
            RiempiRiepiloghi(m);
            if (Interrotto(progress)) {
                return null;
            }

            Fase(progress, "Copia dei movimenti e dei file di supporto...", 2, 6);
            m.DimensioneOriginale += ScriviFilePiatti(out);
            m.FileSupporto = ScriviCartelleConfig(out);
            if (Interrotto(progress)) {
                return null;
            }

            Fase(progress, "Copia dei documenti di origine...", 3, 6);
            long[] doc = ScriviDocumentiFonte(out);
            m.Documenti = (int) doc[0];
            m.DocumentiByte = doc[1];
            m.DimensioneOriginale += doc[1];
            if (Interrotto(progress)) {
                return null;
            }

            Fase(progress, "Esportazione delle tabelle...", 4, 6);
            for (Tabella t : Tabelle(Opzioni.ChiaviApi)) {
                if (t.Gr == Gruppo.CACHE_TOKEN && !Opzioni.CacheToken) {
                    continue;
                }
                if (t.Gr == Gruppo.CHIAVI_API && !Opzioni.ChiaviApi) {
                    continue;
                }
                TabellaSalvata ts = EsportaTabella(out, t);
                if (ts != null) {
                    m.Tabelle.put(t.Chiave(), ts);
                }
                if (Interrotto(progress)) {
                    return null;
                }
            }
            RiempiImpostazioni(m);

            Fase(progress, Opzioni.PrezziCompleti
                    ? "Esportazione di tutti i prezzi (può richiedere qualche minuto)..."
                    : "Esportazione dei prezzi necessari...", 5, 6);
            m.RighePrezzi = EsportaPrezzi(out, Opzioni.PrezziCompleti, progress);
            m.LivelloPrezzi = m.RighePrezzi < 0 ? "assenti" : (Opzioni.PrezziCompleti ? "completi" : "necessari");
            if (m.RighePrezzi < 0) {
                m.RighePrezzi = 0;
            }
            if (Interrotto(progress)) {
                return null;
            }

            Fase(progress, "Scrittura del manifest...", 6, 6);
            byte[] manifest = ManifestAJson(m).toString(2).getBytes(StandardCharsets.UTF_8);
            ScriviVoce(out, NOME_MANIFEST, manifest);
            completato = true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            System.out.println("Backup_Restore.Esegui : " + e.getMessage());
        } finally {
            if (!completato) {
                //Uno zip troncato è peggio di nessuno zip: comparirebbe nell'elenco come backup valido
                zip.delete();
            }
        }
        if (!completato) {
            return null;
        }

        m.Archivio = zip;
        m.DimensioneCompressa = zip.length();
        //Il manifest interno è già scritto: quello esterno riporta anche la dimensione finale, che si
        //conosce solo a zip chiuso
        try {
            Files.write(json.toPath(), ManifestAJson(m).toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Backup_Restore.Esegui : manifest esterno non scritto : " + e.getMessage());
        }
        return zip;
    }

    /**
     * Percorre {@code MappaCryptoWallet} <b>una volta sola</b> e riempie tutti i conteggi del manifest.
     *
     * <p>Le date si confrontano come stringhe: il prefisso dell'ID è {@code yyyyMMddHHmmss}, quindi
     * l'ordine lessicografico è già cronologico e non serve convertire nulla. È lo stesso trucco di
     * {@code DocumentiFonte.Riepiloghi()}.
     */
    private static void RiempiRiepiloghi(Manifest m) {
        for (String[] v : MappaCryptoWallet.values()) {
            if (v == null || v.length < 4) {
                continue;
            }
            m.MovimentiTotali++;
            String data = v[1] == null ? "" : v[1].trim();
            if (data.length() >= 4) {
                m.MovimentiPerAnno.merge(data.substring(0, 4), 1, Integer::sum);
            }
            String wallet = v[3] == null || v[3].isBlank() ? "(senza wallet)" : v[3].trim();
            m.MovimentiPerWallet.merge(wallet, 1, Integer::sum);
            if (!data.isEmpty()) {
                if (m.PrimoMovimento.isEmpty() || data.compareTo(m.PrimoMovimento) < 0) {
                    m.PrimoMovimento = data;
                }
                if (data.compareTo(m.UltimoMovimento) > 0) {
                    m.UltimoMovimento = data;
                }
            }
        }
        m.EpocaRicalcolo = Calcoli_PlusvalenzeNew.EpocaCorrente();
        m.ImprontaPlusvalenze = ImprontaPlusvalenze();
    }

    /** Copia nel manifest le opzioni salvate, così sono leggibili prima del ripristino. */
    private static void RiempiImpostazioni(Manifest m) {
        for (String[] r : LeggiTabella(DB.PERSONALE.Connessione(), "OPZIONI")) {
            if (r.length >= 2 && r[0] != null && !OPZIONI_ESCLUSE.contains(r[0])
                    && !r[0].startsWith(PREFISSO_CHIAVI)) {
                m.Impostazioni.put(r[0], r[1] == null ? "" : r[1]);
            }
        }
    }

    /** @return i byte originali dei file piatti copiati */
    private static long ScriviFilePiatti(ZipOutputStream out) throws IOException {
        long byteTotali = 0;
        for (Map.Entry<String, Gruppo> e : FilePiatti().entrySet()) {
            File f = new File(e.getKey());
            if (!f.isFile()) {
                continue;
            }
            ScriviVoce(out, "movimenti/" + f.getName(), Files.readAllBytes(f.toPath()));
            byteTotali += f.length();
        }
        return byteTotali;
    }

    /**
     * Copia le configurazioni di import dell'<b>utente</b>.
     *
     * <p>I file marcati {@code "centralizzato": true} sono quelli distribuiti con l'applicazione e si
     * risincronizzano da soli: ripristinarli da un backup vecchio riporterebbe indietro configurazioni che
     * nel frattempo sono state corrette a monte.
     *
     * @return quanti file sono stati salvati
     */
    private static int ScriviCartelleConfig(ZipOutputStream out) throws IOException {
        int salvati = 0;
        for (Map.Entry<String, String> e : CartelleConfig().entrySet()) {
            File dir = new File(e.getValue());
            File[] elenco = dir.listFiles();
            if (elenco == null) {
                continue;
            }
            for (File f : elenco) {
                if (!f.isFile() || Centralizzato(f)) {
                    continue;
                }
                ScriviVoce(out, "config/" + e.getKey() + "/" + f.getName(), Files.readAllBytes(f.toPath()));
                salvati++;
            }
        }
        return salvati;
    }

    /** @return {@code true} se il file è una configurazione distribuita con l'applicazione */
    private static boolean Centralizzato(File f) {
        if (!f.getName().toLowerCase().endsWith(".json")) {
            return false;
        }
        try {
            JSONObject o = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            return o.optBoolean("centralizzato", false);
        } catch (IOException | RuntimeException e) {
            //Un file illeggibile o non-JSON è dell'utente per definizione: meglio salvarlo
            return false;
        }
    }

    /**
     * Copia le copie compresse dei file di origine.
     *
     * <p>Sono già gzip, quindi la voce si scrive <b>senza ricomprimere</b>: deflate su dati già compressi
     * costa tempo e non guadagna nulla.
     *
     * @return {@code {numero di file, byte occupati}}
     */
    private static long[] ScriviDocumentiFonte(ZipOutputStream out) throws IOException {
        File dir = new File(VarStatiche.getCartella_DocumentiFonte());
        File[] elenco = dir.listFiles();
        if (elenco == null) {
            return new long[]{0, 0};
        }
        long n = 0, byteTotali = 0;
        out.setLevel(Deflater.NO_COMPRESSION);
        try {
            for (File f : elenco) {
                if (!f.isFile()) {
                    continue;
                }
                ScriviVoce(out, "documenti/" + f.getName(), Files.readAllBytes(f.toPath()));
                n++;
                byteTotali += f.length();
            }
        } finally {
            out.setLevel(Deflater.BEST_SPEED);
        }
        return new long[]{n, byteTotali};
    }

    // =================================================================================================
    // TABELLE: ESPORTAZIONE E REIMPORTAZIONE
    // =================================================================================================
    /**
     * Scrive una tabella come CSV dentro lo zip, con i nomi di colonna in testa.
     *
     * <p>L'intestazione non è cosmetica: è il <b>contratto di compatibilità</b>. Al ripristino le colonne
     * si riassociano per nome, non per posizione, così l'aggiunta di una colonna in una versione
     * successiva non disallinea i dati di un backup vecchio.
     *
     * @return il descrittore da mettere nel manifest, {@code null} se la tabella non esiste
     */
    private static TabellaSalvata EsportaTabella(ZipOutputStream out, Tabella t) throws IOException {
        Connection c = t.Db.Connessione();
        if (c == null) {
            return null;
        }
        //Le colonne si leggono prima di aprire la voce dello zip: servono per l'intestazione e per il
        //manifest, e una voce aperta su una tabella inesistente resterebbe vuota nell'archivio
        List<String> colonne = ColonneDi(c, t.Nome);
        if (colonne.isEmpty()) {
            System.out.println("Backup_Restore : tabella " + t.Chiave() + " assente, non esportata");
            return null;
        }
        TabellaSalvata ts = new TabellaSalvata();
        ts.Colonne.addAll(colonne);

        out.putNextEntry(new ZipEntry("tabelle/" + t.Chiave() + ".csv"));
        Writer w = new OutputStreamWriter(new NonChiudere(out), StandardCharsets.UTF_8);
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + t.Nome)) {
            int n = colonne.size();
            w.write(RigaCsv(colonne.toArray(new String[0])));
            while (rs.next()) {
                String[] r = new String[n];
                for (int i = 1; i <= n; i++) {
                    r[i - 1] = rs.getString(i);
                }
                if (t.Filtro != null && !t.Filtro.test(r)) {
                    continue;
                }
                w.write(RigaCsv(r));
                ts.Righe++;
            }
        } catch (SQLException e) {
            System.out.println("Backup_Restore : tabella " + t.Chiave() + " non esportata (" + e.getMessage() + ")");
        }
        w.flush();
        out.closeEntry();
        return ts;
    }

    /**
     * Svuota una tabella e vi rimette il contenuto del backup, in una sola transazione.
     *
     * <p>Le connessioni H2 restano <b>aperte</b> per tutta la vita dell'applicazione (tengono il lock di
     * istanza singola), quindi non si possono sostituire i file dei database: l'unica strada è
     * {@code DELETE} + {@code INSERT} sulla connessione viva. Le colonne si riassociano per <b>nome</b>:
     * quelle presenti nel backup ma non più nella tabella vengono scartate, quelle nuove restano al
     * default.
     *
     * @param Svuota se {@code false} le righe si sovrappongono a quelle esistenti ({@code MERGE})
     * @return quante righe sono state scritte, {@code -1} in caso di errore
     */
    private static int ImportaTabella(Connection c, String Nome, InputStream Csv, boolean Svuota) {
        List<String> colonneDb = ColonneDi(c, Nome);
        if (colonneDb.isEmpty()) {
            return -1;
        }
        boolean autoCommit = true;
        try (Reader r = new InputStreamReader(new java.io.BufferedInputStream(Csv, 1 << 16),
                StandardCharsets.UTF_8)) {
            autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            //Lo stato vive fuori dal consumatore perché la prima riga è l'intestazione: è lei a
            //decidere quali colonne del CSV esistono ancora e quindi la forma dell'INSERT
            final List<Integer> usabili = new ArrayList<>();
            final PreparedStatement[] ps = new PreparedStatement[1];
            final int[] scritte = {0};
            final SQLException[] errore = new SQLException[1];

            LeggiCsvStream(r, riga -> {
                if (errore[0] != null) {
                    return;
                }
                try {
                    if (ps[0] == null) {
                        List<String> nomi = new ArrayList<>();
                        for (int i = 0; i < riga.length; i++) {
                            for (String cd : colonneDb) {
                                if (cd.equalsIgnoreCase(riga[i])) {
                                    usabili.add(i);
                                    nomi.add(cd);
                                    break;
                                }
                            }
                        }
                        if (nomi.isEmpty()) {
                            errore[0] = new SQLException("nessuna colonna riconosciuta");
                            return;
                        }
                        if (Svuota) {
                            try (Statement st = c.createStatement()) {
                                st.executeUpdate("DELETE FROM " + Nome);
                            }
                        }
                        StringBuilder sql = new StringBuilder(Svuota ? "INSERT INTO " : "MERGE INTO ");
                        sql.append(Nome).append(" (").append(String.join(",", nomi)).append(") VALUES (");
                        for (int i = 0; i < nomi.size(); i++) {
                            sql.append(i == 0 ? "?" : ",?");
                        }
                        ps[0] = c.prepareStatement(sql.append(")").toString());
                        return;
                    }
                    for (int i = 0; i < usabili.size(); i++) {
                        int idx = usabili.get(i);
                        ps[0].setString(i + 1, idx < riga.length ? riga[idx] : null);
                    }
                    ps[0].addBatch();
                    scritte[0]++;
                    if (scritte[0] % 5000 == 0) {
                        ps[0].executeBatch();
                    }
                } catch (SQLException e) {
                    errore[0] = e;
                }
            });

            if (errore[0] != null) {
                throw errore[0];
            }
            if (ps[0] == null) {
                //CSV vuoto: nessuna intestazione, niente da fare
                c.commit();
                return 0;
            }
            ps[0].executeBatch();
            ps[0].close();
            c.commit();
            return scritte[0];
        } catch (SQLException | IOException e) {
            LoggerGC.ScriviErrore(e);
            System.out.println("Backup_Restore.ImportaTabella " + Nome + " : " + e.getMessage());
            try {
                c.rollback();
            } catch (SQLException ignora) {
            }
            return -1;
        } finally {
            try {
                c.setAutoCommit(autoCommit);
            } catch (SQLException ignora) {
            }
        }
    }

    /** @return i nomi di colonna della tabella, lista vuota se la tabella non esiste */
    static List<String> ColonneDi(Connection c, String Nome) {
        List<String> l = new ArrayList<>();
        if (c == null) {
            return l;
        }
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + Nome + " WHERE 1=0")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                l.add(md.getColumnLabel(i));
            }
        } catch (SQLException e) {
            //tabella assente: lista vuota, il chiamante decide
        }
        return l;
    }

    /** Legge una tabella intera come righe di stringhe. */
    private static List<String[]> LeggiTabella(Connection c, String Nome) {
        List<String[]> righe = new ArrayList<>();
        if (c == null) {
            return righe;
        }
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + Nome)) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                String[] r = new String[n];
                for (int i = 1; i <= n; i++) {
                    r[i - 1] = rs.getString(i);
                }
                righe.add(r);
            }
        } catch (SQLException e) {
            System.out.println("Backup_Restore.LeggiTabella " + Nome + " : " + e.getMessage());
        }
        return righe;
    }

    // =================================================================================================
    // PREZZI
    // =================================================================================================
    /**
     * Scrive {@code prezzi/PrezziNew.csv} con tutta la tabella o con il solo sottoinsieme necessario.
     *
     * @return il numero di righe scritte, {@code -1} se il database prezzi non è disponibile
     */
    private static long EsportaPrezzi(ZipOutputStream out, boolean Completi, Download progress) throws IOException {
        Connection c = DatabaseH2.connectionPrezzi;
        if (c == null) {
            return -1;
        }
        List<long[]> intervalli = Completi ? null : IntervalliPrezziNecessari();
        //Si scrive **direttamente dentro la voce dello zip**, riga per riga, senza accumulare in
        //memoria: con l'opzione "tutti i prezzi" questo CSV misura 779 MB sull'archivio reale, e
        //bufferizzarlo (come faceva la prima versione) significherebbe tenerne in RAM il doppio prima
        //di scrivere il primo byte
        out.putNextEntry(new ZipEntry("prezzi/PrezziNew.csv"));
        Writer w = new OutputStreamWriter(new java.io.BufferedOutputStream(new NonChiudere(out), 1 << 16),
                StandardCharsets.UTF_8);
        long righe = 0;
        try {
            w.write(RigaCsv(new String[]{"TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS", "PREZZO"}));
            if (Completi) {
                righe = ScorriPrezzi(c, null, w, righe, progress);
            } else {
                int i = 0;
                if (progress != null) {
                    progress.SetMassimo(intervalli.size());
                }
                for (long[] iv : intervalli) {
                    righe = ScorriPrezzi(c, iv, w, righe, progress);
                    i++;
                    if (progress != null) {
                        progress.SetAvanzamento(i);
                        if (progress.FineThread()) {
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LoggerGC.ScriviErrore(e);
            System.out.println("Backup_Restore.EsportaPrezzi : " + e.getMessage());
            w.flush();
            out.closeEntry();
            return -1;
        }
        w.flush();
        out.closeEntry();
        return righe;
    }

    /** Scorre le righe di {@code PrezziNew} (tutte, o quelle di un intervallo) scrivendole subito. */
    private static long ScorriPrezzi(Connection c, long[] Intervallo, Writer w, long righe,
            Download progress) throws SQLException, IOException {
        String sql = "SELECT timestamp, exchange, symbol, rete, address, prezzo FROM PrezziNew"
                + (Intervallo == null ? "" : " WHERE timestamp BETWEEN ? AND ?");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            //Senza questo H2 materializza l'intero ResultSet lato client: con 15 milioni di righe è
            //il secondo modo di far esplodere la memoria dopo il buffer che non c'è più
            ps.setFetchSize(5000);
            if (Intervallo != null) {
                ps.setLong(1, Intervallo[0]);
                ps.setLong(2, Intervallo[1]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    w.write(RigaCsv(new String[]{
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)}));
                    righe++;
                    if ((righe & 0xFFFF) == 0 && progress != null && progress.FineThread()) {
                        break;
                    }
                }
            }
        }
        return righe;
    }

    /**
     * Gli intervalli temporali che contengono <b>tutti</b> i prezzi che RW e RT possono leggere.
     *
     * <p>Il set di istanti è ricavato dal censimento delle letture prezzo nei motori fiscali:
     * <ul>
     *   <li>i <b>confini di ogni anno</b> presente nei movimenti — {@code 01-01 00:00} e
     *       {@code 12-31 23:59} — usati da {@code Calcoli_RW:540/1794} per valorizzare le giacenze di
     *       inizio e fine periodo e da {@code Calcoli_RT:909/1344/1377} per la plusvalenza latente;</li>
     *   <li>la <b>mezzanotte di oggi</b>, che è l'istante usato per la latente dell'anno in corso;</li>
     *   <li>la data dei movimenti di categoria {@code RW} (reward) con controvalore {@code "0.00"}, unico
     *       caso rimasto in cui si chiede un prezzo alla data del movimento ({@code Calcoli_RW:1297}: il
     *       motore delle plusvalenze azzera quel controvalore e per l'RW va rimesso).</li>
     * </ul>
     *
     * <p>Gli intervalli tornano <b>fusi e ordinati</b>, così le righe estratte non si ripetono e non
     * serve deduplicare a valle.
     *
     * @return coppie {@code {da, a}} in millisecondi epoch, senza sovrapposizioni
     */
    static List<long[]> IntervalliPrezziNecessari() {
        Set<Long> istanti = new LinkedHashSet<>();
        Set<String> anni = new LinkedHashSet<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (v == null || v.length < 16 || v[1] == null || v[1].trim().length() < 16) {
                continue;
            }
            String data = v[1].trim();
            anni.add(data.substring(0, 4));
            //Solo i reward azzerati: sono gli unici a far chiedere un prezzo alla data del movimento
            if ("RW".equals(Categoria(v[0])) && "0.00".equals(v[15])) {
                long t = FunzioniDate.ConvertiDatainLongMinuto(data.substring(0, 16));
                if (t > 0) {
                    istanti.add(t);
                }
            }
        }
        //I confini d'anno servono anche per l'anno successivo all'ultimo movimento: la giacenza di fine
        //anno di N è l'apertura di N+1
        List<String> ordinati = new ArrayList<>(anni);
        Collections.sort(ordinati);
        if (!ordinati.isEmpty()) {
            int primo = Integer.parseInt(ordinati.get(0));
            int ultimo = Integer.parseInt(ordinati.get(ordinati.size() - 1));
            for (int a = primo - 1; a <= ultimo + 1; a++) {
                long i = FunzioniDate.ConvertiDatainLongMinuto(a + "-01-01 00:00");
                long f = FunzioniDate.ConvertiDatainLongMinuto(a + "-12-31 23:59");
                if (i > 0) {
                    istanti.add(i);
                }
                if (f > 0) {
                    istanti.add(f);
                }
            }
        }
        long oggi = FunzioniDate.ConvertiDatainLongMinuto(LocalDate.now() + " 00:00");
        if (oggi > 0) {
            istanti.add(oggi);
        }

        List<Long> lista = new ArrayList<>(istanti);
        Collections.sort(lista);
        List<long[]> intervalli = new ArrayList<>();
        for (long t : lista) {
            long da = t - FINESTRA_PREZZI_MS;
            long a = t + FINESTRA_PREZZI_MS;
            if (!intervalli.isEmpty() && da <= intervalli.get(intervalli.size() - 1)[1]) {
                intervalli.get(intervalli.size() - 1)[1] = Math.max(intervalli.get(intervalli.size() - 1)[1], a);
            } else {
                intervalli.add(new long[]{da, a});
            }
        }
        return intervalli;
    }

    /** @return la categoria di un movimento, cioè l'ultimo segmento {@code _XX} del suo ID */
    private static String Categoria(String Id) {
        if (Id == null) {
            return "";
        }
        int i = Id.lastIndexOf('_');
        return i < 0 ? "" : Id.substring(i + 1);
    }

    // =================================================================================================
    // IMPRONTA DI VERIFICA
    // =================================================================================================
    /**
     * Digest dei campi che il motore delle plusvalenze <b>ricalcola</b>, per ogni movimento.
     *
     * <p>È l'unica cosa che trasforma "spero che la ristampa sia identica" in "è identica": dopo un
     * ripristino il ricalcolo completo rigenera quei campi, e se il digest non torna significa che
     * qualche prezzo è stato risolto diversamente. È un fallimento altrimenti <b>invisibile</b>, perché
     * su cache mancante {@code Prezzi.DammiPrezzoTransazione} scarica dalla rete e un prezzo assente
     * verrebbe recuperato da un altro exchange o da un'altra candela senza che nulla segnali niente.
     *
     * @return il digest esadecimale, stringa vuota se non calcolabile
     */
    public static String ImprontaPlusvalenze() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //L'ordine deve essere stabile fra due esecuzioni: MappaCryptoWallet è ordinata per ID, ma
            //ordinare esplicitamente rende l'impronta indipendente dal tipo di mappa
            List<String> ids = new ArrayList<>(MappaCryptoWallet.keySet());
            Collections.sort(ids);
            for (String id : ids) {
                String[] v = MappaCryptoWallet.get(id);
                if (v == null) {
                    continue;
                }
                md.update(id.getBytes(StandardCharsets.UTF_8));
                for (int i : CAMPI_CALCOLATI) {
                    String s = i < v.length && v[i] != null ? v[i] : "";
                    //La lunghezza entra prima del contenuto: senza, "AB"+"C" e "A"+"BC" darebbero lo
                    //stesso digest pur essendo due movimenti diversi
                    md.update((s.length() + ":" + s).getBytes(StandardCharsets.UTF_8));
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println("Backup_Restore.ImprontaPlusvalenze : " + e.getMessage());
            return "";
        }
    }

    // =================================================================================================
    // ELENCO, LETTURA, IMPORT/EXPORT, CANCELLAZIONE
    // =================================================================================================
    /**
     * Gli archivi presenti in {@code ArchivioBackup/}, dal più recente.
     *
     * <p>Legge il manifest <b>affiancato</b> quando c'è, e apre lo zip solo per gli archivi che non ce
     * l'hanno (tipicamente quelli appena importati da fuori, per i quali il file esterno viene poi
     * rigenerato). È il motivo per cui il manifest viene scritto due volte.
     *
     * @return un manifest per archivio leggibile; gli archivi corrotti vengono saltati
     */
    public static List<Manifest> Elenco() {
        List<Manifest> l = new ArrayList<>();
        File[] file = new File(VarStatiche.getCartella_ArchivioBackup())
                .listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));
        if (file == null) {
            return l;
        }
        for (File f : file) {
            Manifest m = null;
            File json = new File(f.getParentFile(), NomeBase(f) + ".json");
            if (json.isFile()) {
                try {
                    m = JsonAManifest(new JSONObject(
                            new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8)));
                } catch (IOException | RuntimeException e) {
                    m = null;
                }
            }
            if (m == null) {
                m = LeggiManifest(f);
                if (m != null) {
                    RiscriviManifestEsterno(f, m);
                }
            }
            if (m != null) {
                m.Archivio = f;
                m.DimensioneCompressa = f.length();
                l.add(m);
            }
        }
        //Ordinati per data di creazione, dal più recente. **Non** per nome: i backup automatici si
        //chiamano "backup_auto_…" e in ordine alfabetico finirebbero tutti sopra i manuali qualunque
        //sia la loro data, che è l'esatto contrario di ciò per cui serve questa tabella.
        l.sort((a, b) -> b.Creato.compareTo(a.Creato));
        return l;
    }

    /**
     * Legge il manifest dall'interno di un archivio.
     * @return {@code null} se lo zip non è leggibile o non contiene un manifest
     */
    public static Manifest LeggiManifest(File Zip) {
        if (Zip == null || !Zip.isFile()) {
            return null;
        }
        try (ZipFile z = new ZipFile(Zip)) {
            ZipEntry e = z.getEntry(NOME_MANIFEST);
            if (e == null) {
                return null;
            }
            try (InputStream in = z.getInputStream(e)) {
                Manifest m = JsonAManifest(new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                m.Archivio = Zip;
                m.DimensioneCompressa = Zip.length();
                return m;
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Backup_Restore.LeggiManifest " + Zip.getName() + " : " + e.getMessage());
            return null;
        }
    }

    /** Elimina archivio e manifest affiancato. */
    public static boolean Elimina(File Zip) {
        if (Zip == null || !Zip.isFile()) {
            return false;
        }
        File json = new File(Zip.getParentFile(), NomeBase(Zip) + ".json");
        json.delete();
        return Zip.delete();
    }

    /**
     * Copia un archivio in una cartella a scelta dell'utente.
     * @return il file scritto, {@code null} in caso di errore
     */
    public static File Esporta(File Zip, File CartellaDestinazione) {
        if (Zip == null || !Zip.isFile() || CartellaDestinazione == null) {
            return null;
        }
        File dest = new File(CartellaDestinazione, Zip.getName());
        try {
            Files.copy(Zip.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            //Il manifest affiancato viaggia insieme: senza, chi reimporta l'archivio lo rigenererebbe
            //aprendo lo zip, che è esattamente il costo che si vuole evitare
            File json = new File(Zip.getParentFile(), NomeBase(Zip) + ".json");
            if (json.isFile()) {
                Files.copy(json.toPath(), new File(CartellaDestinazione, json.getName()).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        } catch (IOException e) {
            System.out.println("Backup_Restore.Esporta : " + e.getMessage());
            return null;
        }
    }

    /**
     * Copia un archivio esterno dentro {@code ArchivioBackup/}, verificandolo.
     *
     * <p>Se in cartella esiste già un archivio con quel nome ne sceglie uno libero aggiungendo un
     * suffisso: due backup fatti su macchine diverse nello stesso secondo sono improbabili ma non
     * impossibili, e sovrascrivere sarebbe una perdita di dati silenziosa.
     *
     * @return il file importato, {@code null} se non è un archivio valido
     */
    public static File Importa(File ZipEsterno) {
        if (ZipEsterno == null || !ZipEsterno.isFile()) {
            return null;
        }
        Manifest m = LeggiManifest(ZipEsterno);
        if (m == null) {
            return null;
        }
        File cartella = new File(VarStatiche.getCartella_ArchivioBackup());
        if (!cartella.exists() && !cartella.mkdirs()) {
            return null;
        }
        File dest = new File(cartella, ZipEsterno.getName());
        int n = 1;
        while (dest.exists()) {
            dest = new File(cartella, NomeBase(ZipEsterno) + "_importato" + (n == 1 ? "" : "_" + n) + ".zip");
            n++;
        }
        try {
            Files.copy(ZipEsterno.toPath(), dest.toPath());
        } catch (IOException e) {
            System.out.println("Backup_Restore.Importa : " + e.getMessage());
            return null;
        }
        m.Importato = true;
        RiscriviManifestEsterno(dest, m);
        return dest;
    }

    /** Riscrive il manifest affiancato a un archivio. */
    private static void RiscriviManifestEsterno(File Zip, Manifest m) {
        File json = new File(Zip.getParentFile(), NomeBase(Zip) + ".json");
        try {
            Files.write(json.toPath(), ManifestAJson(m).toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Backup_Restore : manifest esterno non riscritto : " + e.getMessage());
        }
    }

    /** @return il nome del file senza estensione */
    private static String NomeBase(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return i < 0 ? n : n.substring(0, i);
    }

    // =================================================================================================
    // RIPRISTINO
    // =================================================================================================
    /** Esito di un ripristino, da mostrare all'utente. */
    public static final class EsitoRipristino {

        /** {@code true} se il ripristino è arrivato in fondo. */
        public boolean Riuscito;
        /** Righe scritte per tabella e file ripristinati, in ordine di esecuzione. */
        public final List<String> Dettaglio = new ArrayList<>();
        /** Problemi non bloccanti (tabelle assenti, colonne scartate). */
        public final List<String> Avvisi = new ArrayList<>();
        /** {@code true} se i movimenti sono stati riscritti e vanno ricaricati dal chiamante. */
        public boolean MovimentiRipristinati;
        /** Impronta delle plusvalenze contenuta nel backup, da confrontare dopo il ricalcolo. */
        public String ImprontaAttesa = "";
        /**
         * Gruppi la cui mancanza rende il confronto delle impronte privo di senso; vuoto se la verifica
         * si può fare. Riempito da {@link #GruppiMancantiPerVerifica}.
         */
        public final Set<Gruppo> VerificaMancanti = EnumSet.noneOf(Gruppo.class);
        /** {@code true} se anche la cache dei prezzi di mercato è stata ripristinata. */
        public boolean PrezziRipristinati;
        /**
         * {@code true} se l'archivio conteneva prezzi di mercato. Serve a non consigliare all'utente di
         * riselezionare una casella che era disabilitata.
         */
        public boolean PrezziNellArchivio;
    }

    /**
     * Ripristina i gruppi indicati dall'archivio.
     *
     * <p><b>Non ricarica nulla</b>: al ritorno tocca a {@code Principale} rileggere i movimenti,
     * invalidare le cache e rifare i calcoli. È la stessa divisione delle altre estrazioni — la logica
     * operativa qui, il refresh della GUI di là — e qui è anche una necessità, perché la funzione che
     * ricarica {@code MappaCryptoWallet} è privata di {@code Principale}.
     *
     * @param Zip archivio da cui ripristinare
     * @param Gruppi i gruppi selezionati dall'utente
     * @param progress finestra di avanzamento, può essere {@code null}
     * @return l'esito, mai {@code null}
     */
    public static EsitoRipristino Ripristina(File Zip, Set<Gruppo> Gruppi, Download progress) {
        EsitoRipristino esito = new EsitoRipristino();
        if (Zip == null || !Zip.isFile() || Gruppi == null || Gruppi.isEmpty()) {
            esito.Avvisi.add("Nessun archivio o nessun gruppo selezionato.");
            return esito;
        }
        Manifest m = LeggiManifest(Zip);
        if (m == null) {
            esito.Avvisi.add("L'archivio non contiene un manifest leggibile.");
            return esito;
        }
        esito.ImprontaAttesa = m.ImprontaPlusvalenze;
        esito.PrezziNellArchivio = m.GruppiPresenti().contains(Gruppo.PREZZI);

        try (ZipFile z = new ZipFile(Zip)) {
            int passo = 0;
            int totale = Gruppi.size() + 2;

            //--- tabelle
            Fase(progress, "Ripristino delle tabelle...", ++passo, totale);
            for (Tabella t : Tabelle(true)) {
                if (!Gruppi.contains(t.Gr)) {
                    continue;
                }
                ZipEntry e = z.getEntry("tabelle/" + t.Chiave() + ".csv");
                if (e == null) {
                    continue;
                }
                int n;
                try (InputStream in = z.getInputStream(e)) {
                    n = ImportaTabella(t.Db.Connessione(), t.Nome, in, true);
                }
                if (n < 0) {
                    esito.Avvisi.add("Tabella " + t.Chiave() + " non ripristinata.");
                } else {
                    esito.Dettaglio.add(t.Chiave() + " : " + n + " righe");
                }
            }

            //--- prezzi di mercato
            if (Gruppi.contains(Gruppo.PREZZI)) {
                Fase(progress, "Ripristino dei prezzi...", ++passo, totale);
                ZipEntry e = z.getEntry("prezzi/PrezziNew.csv");
                if (e != null) {
                    //I prezzi di mercato si sovrappongono ({@code MERGE}) invece di cancellare: la cache
                    //locale può contenere prezzi più recenti o token che il backup non aveva, e buttarli
                    //via costringerebbe a riscaricarli senza alcun vantaggio
                    int n;
                    try (InputStream in = z.getInputStream(e)) {
                        n = ImportaTabella(DatabaseH2.connectionPrezzi, "PrezziNew", in, false);
                    }
                    if (n < 0) {
                        esito.Avvisi.add("Prezzi di mercato non ripristinati.");
                    } else {
                        esito.Dettaglio.add("prezzi/PrezziNew : " + n + " righe sovrascritte");
                    }
                }
            }

            //--- file piatti
            Fase(progress, "Ripristino dei file...", ++passo, totale);
            for (Map.Entry<String, Gruppo> fp : FilePiatti().entrySet()) {
                if (!Gruppi.contains(fp.getValue())) {
                    continue;
                }
                File dest = new File(fp.getKey());
                ZipEntry e = z.getEntry("movimenti/" + dest.getName());
                if (e == null) {
                    continue;
                }
                Files.write(dest.toPath(), Leggi(z, e));
                esito.Dettaglio.add(dest.getName() + " ripristinato");
                if (fp.getValue() == Gruppo.MOVIMENTI) {
                    esito.MovimentiRipristinati = true;
                }
            }

            //--- documenti di origine e configurazioni
            if (Gruppi.contains(Gruppo.MOVIMENTI)) {
                int n = EstraiCartella(z, "documenti/", new File(VarStatiche.getCartella_DocumentiFonte()));
                esito.Dettaglio.add(n + " documenti di origine ripristinati");
            }
            if (Gruppi.contains(Gruppo.OPZIONI_CALCOLO)) {
                int n = 0;
                for (Map.Entry<String, String> cc : CartelleConfig().entrySet()) {
                    n += EstraiCartella(z, "config/" + cc.getKey() + "/", new File(cc.getValue()));
                }
                esito.Dettaglio.add(n + " file di configurazione ripristinati");
            }
            esito.Riuscito = true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            esito.Avvisi.add("Errore durante il ripristino : " + e.getMessage());
        } finally {
            //Sempre, anche dopo un errore: se una parte delle tabelle è stata riscritta, le cache in
            //memoria descrivono già uno stato che non esiste più
            InvalidaCacheInMemoria();
            //Anche questo va calcolato sempre: un ripristino interrotto a metà è, semmai, ancora meno
            //verificabile di uno completo
            esito.PrezziRipristinati = Gruppi.contains(Gruppo.PREZZI);
            esito.VerificaMancanti.addAll(
                    GruppiMancantiPerVerifica(Gruppi, esito.MovimentiRipristinati));
        }
        return esito;
    }

    /**
     * Butta via tutto ciò che, dopo aver riscritto le tabelle, resterebbe in memoria a descrivere lo
     * stato precedente.
     *
     * <p>Sta <b>qui</b> e non nel chiamante di proposito : sono cache del livello dati, e chi riscrive
     * quelle tabelle è questa classe. Lasciarle al chiamante significherebbe che un secondo punto di
     * ripristino — un test, un futuro comando da riga di comando — le dimenticherebbe, e il risultato
     * sarebbe <b>sbagliato senza alcun errore visibile</b>: le opzioni e i gruppi wallet entrano in
     * {@code OpzioniRicalcolo.Epoca()}, quindi il motore continuerebbe a calcolare con i valori di
     * prima credendo che nulla sia cambiato. È esattamente il caso che il test di round-trip ha colto.
     *
     * <p>Quello che resta al chiamante è il <b>ricaricamento</b>: rileggere {@code MappaCryptoWallet} dal
     * file e rinfrescare le tabelle richiede metodi privati di {@code Principale}.
     */
    private static void InvalidaCacheInMemoria() {
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        DatabaseH2.Pers_Opzioni_InvalidaCache();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();
    }

    /** Estrae in una cartella tutte le voci con un dato prefisso. */
    private static int EstraiCartella(ZipFile z, String Prefisso, File Destinazione) throws IOException {
        if (!Destinazione.exists() && !Destinazione.mkdirs()) {
            return 0;
        }
        int n = 0;
        for (ZipEntry e : Collections.list(z.entries())) {
            if (e.isDirectory() || !e.getName().startsWith(Prefisso)) {
                continue;
            }
            String nome = e.getName().substring(Prefisso.length());
            //Nessun percorso relativo può uscire dalla cartella di destinazione
            if (nome.isEmpty() || nome.contains("/") || nome.contains("\\") || nome.contains("..")) {
                continue;
            }
            Files.write(new File(Destinazione, nome).toPath(), Leggi(z, e));
            n++;
        }
        return n;
    }

    private static byte[] Leggi(ZipFile z, ZipEntry e) throws IOException {
        try (InputStream in = z.getInputStream(e)) {
            return in.readAllBytes();
        }
    }

    // =================================================================================================
    // MANIFEST <-> JSON
    // =================================================================================================
    static JSONObject ManifestAJson(Manifest m) {
        JSONObject o = new JSONObject();
        o.put("formatoBackup", m.FormatoBackup);
        o.put("creato", m.Creato);
        o.put("programma", new JSONObject()
                .put("versione", m.Versione)
                .put("colonneTabella", m.ColonneTabella));
        o.put("movimenti", new JSONObject()
                .put("totale", m.MovimentiTotali)
                .put("perAnno", new JSONObject(m.MovimentiPerAnno))
                .put("perWallet", new JSONObject(m.MovimentiPerWallet))
                .put("primo", m.PrimoMovimento)
                .put("ultimo", m.UltimoMovimento));
        o.put("documenti", new JSONObject().put("numero", m.Documenti).put("byte", m.DocumentiByte));
        o.put("fileSupporto", m.FileSupporto);
        o.put("impostazioni", new JSONObject(m.Impostazioni));
        o.put("epocaRicalcolo", m.EpocaRicalcolo);
        o.put("improntaPlusvalenze", m.ImprontaPlusvalenze);
        o.put("contenuto", new JSONObject()
                .put("prezzi", m.LivelloPrezzi)
                .put("righePrezzi", m.RighePrezzi)
                .put("chiaviApi", m.ChiaviApi)
                .put("cacheToken", m.CacheToken)
                .put("automatico", m.Automatico)
                .put("importato", m.Importato));
        JSONObject tab = new JSONObject();
        for (Map.Entry<String, TabellaSalvata> e : m.Tabelle.entrySet()) {
            tab.put(e.getKey(), new JSONObject()
                    .put("righe", e.getValue().Righe)
                    .put("colonne", new JSONArray(e.getValue().Colonne)));
        }
        o.put("tabelle", tab);
        o.put("dimensione", new JSONObject()
                .put("compresso", m.DimensioneCompressa)
                .put("originale", m.DimensioneOriginale));
        return o;
    }

    static Manifest JsonAManifest(JSONObject o) {
        Manifest m = new Manifest();
        m.FormatoBackup = o.optInt("formatoBackup", 0);
        m.Creato = o.optString("creato", "");
        JSONObject p = o.optJSONObject("programma");
        if (p != null) {
            m.Versione = p.optString("versione", "");
            m.ColonneTabella = p.optInt("colonneTabella", 0);
        }
        JSONObject mv = o.optJSONObject("movimenti");
        if (mv != null) {
            m.MovimentiTotali = mv.optInt("totale", 0);
            m.PrimoMovimento = mv.optString("primo", "");
            m.UltimoMovimento = mv.optString("ultimo", "");
            CopiaConteggi(mv.optJSONObject("perAnno"), m.MovimentiPerAnno);
            CopiaConteggi(mv.optJSONObject("perWallet"), m.MovimentiPerWallet);
        }
        JSONObject d = o.optJSONObject("documenti");
        if (d != null) {
            m.Documenti = d.optInt("numero", 0);
            m.DocumentiByte = d.optLong("byte", 0);
        }
        m.FileSupporto = o.optInt("fileSupporto", 0);
        JSONObject imp = o.optJSONObject("impostazioni");
        if (imp != null) {
            for (String k : imp.keySet()) {
                m.Impostazioni.put(k, imp.optString(k, ""));
            }
        }
        m.EpocaRicalcolo = o.optLong("epocaRicalcolo", 0);
        m.ImprontaPlusvalenze = o.optString("improntaPlusvalenze", "");
        JSONObject c = o.optJSONObject("contenuto");
        if (c != null) {
            m.LivelloPrezzi = c.optString("prezzi", "");
            m.RighePrezzi = c.optLong("righePrezzi", 0);
            m.ChiaviApi = c.optBoolean("chiaviApi", false);
            //"cacheRegistri" e' il nome che la chiave aveva quando il gruppo comprendeva anche i
            //registri GESTITI*: un archivio prodotto allora contiene comunque GOPLUSSECURITY e
            //TOKENSOLANA, quindi va letto come se dichiarasse il gruppo di oggi
            m.CacheToken = c.optBoolean("cacheToken", c.optBoolean("cacheRegistri", false));
            m.Automatico = c.optBoolean("automatico", false);
            m.Importato = c.optBoolean("importato", false);
        }
        JSONObject t = o.optJSONObject("tabelle");
        if (t != null) {
            for (String k : t.keySet()) {
                JSONObject v = t.optJSONObject(k);
                if (v == null) {
                    continue;
                }
                TabellaSalvata ts = new TabellaSalvata();
                ts.Righe = v.optInt("righe", 0);
                JSONArray col = v.optJSONArray("colonne");
                if (col != null) {
                    for (int i = 0; i < col.length(); i++) {
                        ts.Colonne.add(col.optString(i, ""));
                    }
                }
                m.Tabelle.put(k, ts);
            }
        }
        JSONObject dim = o.optJSONObject("dimensione");
        if (dim != null) {
            m.DimensioneCompressa = dim.optLong("compresso", 0);
            m.DimensioneOriginale = dim.optLong("originale", 0);
        }
        return m;
    }

    private static void CopiaConteggi(JSONObject o, Map<String, Integer> destinazione) {
        if (o == null) {
            return;
        }
        for (String k : o.keySet()) {
            destinazione.put(k, o.optInt(k, 0));
        }
    }

    // =================================================================================================
    // UTILITÀ
    // =================================================================================================
    private static void ScriviVoce(ZipOutputStream out, String Nome, byte[] Contenuto) throws IOException {
        out.putNextEntry(new ZipEntry(Nome));
        out.write(Contenuto);
        out.closeEntry();
    }

    private static void Fase(Download progress, String Testo, int Passo, int Totale) {
        if (progress == null) {
            return;
        }
        progress.SetLabel(Testo);
        progress.SetMassimo(Totale);
        progress.SetAvanzamento(Passo);
    }

    private static boolean Interrotto(Download progress) {
        return progress != null && progress.FineThread();
    }

    /** Una riga CSV con virgolette raddoppiate, terminata da newline. */
    static String RigaCsv(String[] Valori) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Valori.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String v = Valori[i];
            if (v == null) {
                //Un campo vuoto e un NULL vanno distinti: il primo è "" fra virgolette, il secondo niente
                continue;
            }
            sb.append('"').append(v.replace("\"", "\"\"")).append('"');
        }
        return sb.append('\n').toString();
    }

    /**
     * Parser CSV minimale, speculare a {@link #RigaCsv(String[])}, che consegna <b>una riga alla
     * volta</b> senza mai tenerle tutte in memoria.
     *
     * <p>Lo streaming non è un vezzo: con l'opzione "tutti i prezzi" il CSV è di 15 milioni di righe e
     * 779 MB sull'archivio reale. Leggerlo in una {@code String} ne costerebbe il doppio in RAM (UTF-16)
     * e costruirne la {@code List<String[]>} molto di più, tutto prima del primo {@code INSERT}.
     *
     * <p>Un campo senza virgolette è un {@code NULL}, uno fra virgolette è una stringa (anche vuota):
     * è la distinzione che permette di riscrivere una tabella senza trasformare i {@code NULL} in
     * stringhe vuote, cosa che su una colonna di chiave primaria cambierebbe i dati.
     */
    static void LeggiCsvStream(Reader r, java.util.function.Consumer<String[]> PerRiga) throws IOException {
        List<String> campi = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean fraVirgolette = false, valorizzato = false, virgolettaSospesa = false;
        int letto;
        while ((letto = r.read()) != -1) {
            char c = (char) letto;
            if (virgolettaSospesa) {
                //Chiusa una virgoletta dentro un campo quotato: se ne segue un'altra è un " letterale,
                //altrimenti il campo è finito e questo carattere va rielaborato fuori dalle virgolette
                virgolettaSospesa = false;
                if (c == '"') {
                    campo.append('"');
                    fraVirgolette = true;
                    continue;
                }
                fraVirgolette = false;
            }
            if (fraVirgolette) {
                if (c == '"') {
                    virgolettaSospesa = true;
                } else {
                    campo.append(c);
                }
                continue;
            }
            if (c == '"') {
                fraVirgolette = true;
                valorizzato = true;
            } else if (c == ',') {
                campi.add(valorizzato ? campo.toString() : null);
                campo.setLength(0);
                valorizzato = false;
            } else if (c == '\n') {
                campi.add(valorizzato ? campo.toString() : null);
                PerRiga.accept(campi.toArray(new String[0]));
                campi.clear();
                campo.setLength(0);
                valorizzato = false;
            } else if (c != '\r') {
                campo.append(c);
                valorizzato = true;
            }
        }
        if (valorizzato || !campi.isEmpty()) {
            campi.add(valorizzato ? campo.toString() : null);
            PerRiga.accept(campi.toArray(new String[0]));
        }
    }

    /**
     * Versione in memoria di {@link #LeggiCsvStream}, per i test e per i testi piccoli.
     * @param Testo il CSV per intero
     * @return le righe lette
     */
    static List<String[]> LeggiCsv(String Testo) {
        List<String[]> righe = new ArrayList<>();
        try {
            LeggiCsvStream(new java.io.StringReader(Testo), righe::add);
        } catch (IOException e) {
            //Uno StringReader non fallisce in lettura
        }
        return righe;
    }

    /**
     * Filtro che lascia passare tutto tranne la {@code close()}.
     *
     * <p>Serve per poter avvolgere lo {@link ZipOutputStream} in un {@link Writer} senza che la chiusura
     * del writer chiuda l'intero archivio : un {@code OutputStreamWriter} deve poter essere svuotato per
     * voce, ma lo zip resta aperto per quelle successive.
     */
    private static final class NonChiudere extends java.io.FilterOutputStream {

        NonChiudere(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            //FilterOutputStream scriverebbe byte per byte: su centinaia di MB è insostenibile
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }

    /** @return la dimensione in byte resa leggibile */
    public static String DimensioneLeggibile(long Byte) {
        if (Byte <= 0) {
            return "-";
        }
        if (Byte < 1024) {
            return Byte + " B";
        }
        if (Byte < 1024 * 1024) {
            return String.format("%.1f KB", Byte / 1024.0);
        }
        if (Byte < 1024L * 1024L * 1024L) {
            return String.format("%.1f MB", Byte / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", Byte / (1024.0 * 1024.0 * 1024.0));
    }
}
