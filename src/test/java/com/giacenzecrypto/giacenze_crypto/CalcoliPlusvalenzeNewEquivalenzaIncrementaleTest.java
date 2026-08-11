package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica di EQUIVALENZA fra la passata unica di {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenze()}
 * e una ripartenza da checkpoint, cioè il presupposto del ricalcolo incrementale descritto in
 * {@code test/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md} (paragrafo 7).
 *
 * <p>Il motore incrementale non esiste ancora: qui si verifica il <b>meccanismo</b> su cui si
 * baserà, usando {@link Calcoli_PlusvalenzeNew#ElaboraMovimento} (estratto apposta) per
 * rielaborare solo una coda della mappa a partire da uno stato LIFO salvato.
 *
 * <p>Lo scenario è quello che il golden master NON riesce a coprire: il dataset reale ha
 * {@code PlusXWallet = NO}, quindi il percorso della controparte fra gruppi wallet diversi — il
 * più delicato del motore — non viene mai eseguito da nessun test. Qui viene costruito a mano:
 * due gruppi wallet, un trasferimento PTW→DTW fra i due, e la <b>dipendenza all'indietro</b> del
 * paragrafo 5.1 (il PTW legge campi del DTW, che nell'ordine della mappa viene <i>dopo</i>).
 *
 * <p>Il test più importante della classe è
 * {@link #ripartenzaDallaChiaveSporcaMinima_senzaCollegati_lasciaUnRisultatoStantio()}: dimostra
 * che la regola ingenua "riparto dalla chiave modificata più piccola" produce un dato fiscale
 * sbagliato, ed è la ragione per cui l'impronta deve includere un digest dei movimenti collegati.
 * Asserisce quindi un comportamento <b>indesiderato</b>, di proposito.
 */
class CalcoliPlusvalenzeNewEquivalenzaIncrementaleTest {

    @TempDir
    static Path tempDir;

    private static final String WALLET_A = "WalletA";   // Gruppo 01
    private static final String WALLET_B = "WalletB";   // Gruppo 02

    // ID costruiti a mano (niente contatori globali): lo scenario viene ricostruito più volte
    // nello stesso test e gli ID devono restare identici fra una ricostruzione e l'altra.
    private static final String ID_ACQUISTO = "2024-01-05 10.00_TEST_001_001_AC";
    private static final String ID_DONAZIONE = "2024-01-20 11.00_TEST_002_001_DC";
    private static final String ID_PRELIEVO = "2024-02-10 09.00_TEST_003_001_TI";
    private static final String ID_DEPOSITO = "2024-02-10 09.30_TEST_004_001_TI";
    private static final String ID_PENZOLANTE = "2024-02-15 12.00_TEST_005_001_TI";
    private static final String ID_VENDITA_B = "2024-03-01 10.00_TEST_006_001_VC";
    private static final String ID_VENDITA_DONO = "2024-03-05 10.00_TEST_007_001_VC";

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = 5000;
        DatabaseH2.Pers_Opzioni_Scrivi(Calcoli_PlusvalenzeNew.OPZIONE_INCREMENTALE, "SI");
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        //IL punto dello scenario: senza PlusXWallet=SI il percorso della controparte non gira mai
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("CashBackComeFIAT", "NO");
        DatabaseH2.Pers_GruppoWallet_Scrivi(WALLET_A, "Gruppo 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi(WALLET_B, "Gruppo 02");
        costruisciScenario();
    }

    // ------------------------------------------------------------------
    // Scenario
    // ------------------------------------------------------------------

    /**
     * Ricostruisce da zero i movimenti dello scenario. Va richiamato prima di ogni passata da
     * confrontare: il motore riscrive i campi 16/17/19/33/38 in place, quindi due passate diverse
     * devono partire dagli stessi identici dati di ingresso.
     *
     * <ul>
     *   <li>acquisto di 10 BTC a 1000 EUR sul WalletA (Gruppo 01);</li>
     *   <li>donazione ricevuta di 2 ETH con costo di carico 250 inserito a mano in v[17];</li>
     *   <li>prelievo PTW dal WalletA collegato al deposito DTW sul WalletB (Gruppo 02): il
     *       collegato viene <b>dopo</b> nell'ordine della mappa;</li>
     *   <li>un DTW con un collegato inesistente (v[20] penzolante);</li>
     *   <li>vendita dei 10 BTC dal WalletB a 1500: il costo di carico deve essere arrivato lì
     *       attraverso il trasferimento;</li>
     *   <li>vendita dei 2 ETH donati a 400.</li>
     * </ul>
     */
    private static void costruisciScenario() {
        Principale.MappaCryptoWallet.clear();

        movimento(ID_ACQUISTO, "2024-01-05 10:00", WALLET_A, "", "ACQUISTO",
                "EUR", "FIAT", "1000", "BTC", "Crypto", "10", "1000", "", "M");

        String[] dono = movimento(ID_DONAZIONE, "2024-01-20 11:00", WALLET_A,
                "DDO - Donazione ricevuta", "DONAZIONE",
                "", "", "", "ETH", "Crypto", "2", "300", "", "M");
        dono[17] = "250.00";   //costo di carico della donazione: INGRESSO del motore, non uscita

        movimento(ID_PRELIEVO, "2024-02-10 09:00", WALLET_A,
                "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", "TRASFERIMENTO",
                "BTC", "Crypto", "10", "", "", "", "0", ID_DEPOSITO, "M");

        movimento(ID_DEPOSITO, "2024-02-10 09:30", WALLET_B,
                "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", "TRASFERIMENTO",
                "", "", "", "BTC", "Crypto", "10", "0", ID_PRELIEVO, "M");

        movimento(ID_PENZOLANTE, "2024-02-15 12:00", WALLET_B,
                "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", "TRASFERIMENTO",
                "", "", "", "SOL", "Crypto", "5", "100", "2023-01-01 00.00_NONESISTE_999_001_TI", "M");

        movimento(ID_VENDITA_B, "2024-03-01 10:00", WALLET_B, "", "VENDITA",
                "BTC", "Crypto", "10", "EUR", "FIAT", "1500", "1500", "", "M");

        movimento(ID_VENDITA_DONO, "2024-03-05 10:00", WALLET_A, "", "VENDITA",
                "ETH", "Crypto", "2", "EUR", "FIAT", "400", "400", "", "M");
    }

    private static String[] movimento(String id, String data, String wallet, String classificazione,
            String descrizione, String monetaU, String tipoU, String qtaU,
            String monetaE, String tipoE, String qtaE, String valore,
            String collegati, String marcatore) {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = wallet;
        m[5] = descrizione;
        m[8] = monetaU;
        m[9] = tipoU;
        m[10] = qtaU;
        m[11] = monetaE;
        m[12] = tipoE;
        m[13] = qtaE;
        m[15] = valore;
        m[18] = classificazione;
        m[20] = collegati;
        m[22] = marcatore;
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    // ------------------------------------------------------------------
    // Strumenti: passate parziali, checkpoint, fotografie del risultato
    // ------------------------------------------------------------------

    /** Elabora i movimenti da {@code dallaChiave} (inclusa) in poi, con lo stato LIFO passato. */
    private static void elaboraDa(String dallaChiave, Map<String, Map<String, ArrayDeque<String[]>>> stato) {
        Calcoli_PlusvalenzeNew.OpzioniRicalcolo opzioni = new Calcoli_PlusvalenzeNew.OpzioniRicalcolo();
        for (String[] v : Principale.MappaCryptoWallet.tailMap(dallaChiave, true).values()) {
            Calcoli_PlusvalenzeNew.ElaboraMovimento(v, stato, opzioni);
        }
    }

    /**
     * Esegue la passata completa movimento per movimento, restituendo per ogni ID lo stato LIFO
     * <b>precedente</b> alla sua elaborazione: sono i checkpoint da cui si può ripartire.
     */
    private static Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> eseguiRaccogliendoCheckpoint() {
        Calcoli_PlusvalenzeNew.OpzioniRicalcolo opzioni = new Calcoli_PlusvalenzeNew.OpzioniRicalcolo();
        Map<String, Map<String, ArrayDeque<String[]>>> stato = new TreeMap<>();
        Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> checkpoint = new LinkedHashMap<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            checkpoint.put(v[0], copiaStato(stato));
            Calcoli_PlusvalenzeNew.ElaboraMovimento(v, stato, opzioni);
        }
        return checkpoint;
    }

    /**
     * Copia di uno stato LIFO: duplica l'ossatura delle mappe e delle pile ma <b>condivide gli
     * array dei lotti</b>, perché il motore non li modifica mai in place (il consumo parziale
     * alloca un array nuovo). È esattamente la copia prevista dal paragrafo 3.1 dell'analisi, e
     * il paragrafo 14.4 avverte che quell'invariante non è difesa dal compilatore: se un domani
     * {@code StackLIFO_TogliQta} scrivesse dentro i lotti, i test di questa classe fallirebbero.
     */
    private static Map<String, Map<String, ArrayDeque<String[]>>> copiaStato(
            Map<String, Map<String, ArrayDeque<String[]>>> stato) {
        Map<String, Map<String, ArrayDeque<String[]>>> copia = new TreeMap<>();
        for (Map.Entry<String, Map<String, ArrayDeque<String[]>>> gruppo : stato.entrySet()) {
            Map<String, ArrayDeque<String[]>> perMoneta = new TreeMap<>();
            for (Map.Entry<String, ArrayDeque<String[]>> moneta : gruppo.getValue().entrySet()) {
                perMoneta.put(moneta.getKey(), moneta.getValue().clone());
            }
            copia.put(gruppo.getKey(), perMoneta);
        }
        return copia;
    }

    /**
     * Sporca i campi di uscita dei movimenti da {@code dallaChiave} in poi, prima di una
     * ripartenza. Senza questo, un test di ripartenza <b>non dimostra nulla</b>: la passata che ha
     * raccolto i checkpoint ha già lasciato i valori giusti dappertutto, quindi il confronto
     * finale passerebbe anche se la rielaborazione non facesse assolutamente niente. Con il
     * marcatore, una ripartenza che salta del lavoro lascia "SPORCO" in fotografia e fallisce.
     * <p>
     * Due campi restano volutamente fuori, per ragioni diverse:
     * <ul>
     *   <li><b>{@code v[17]}</b>: per i movimenti DDO è un <i>ingresso</i> del motore (il costo di
     *       carico della donazione, inserito dall'utente), non un'uscita — è il doppio ruolo del
     *       paragrafo 4.3. Sporcarlo cambierebbe i dati in ingresso.</li>
     *   <li><b>{@code v[38]}</b>: il motore non lo riscrive mai, lo <i>modifica</i>. Toglie le
     *       lettere "E" e "M" all'inizio di ogni movimento e aggiunge "A" quando manca lo stack
     *       LIFO; qualunque altro contenuto sopravvive intatto a un ricalcolo, anche completo.
     *       È un campo portato avanti, non ricalcolato — e si comporta allo stesso modo nella
     *       passata unica e nella ripartenza, che è ciò che qui interessa.</li>
     * </ul>
     */
    private static void sporcaLeUsciteDa(String dallaChiave) {
        for (String[] v : Principale.MappaCryptoWallet.tailMap(dallaChiave, true).values()) {
            v[16] = "SPORCO";
            v[19] = "SPORCO";
            v[33] = "SPORCO";
        }
    }

    /**
     * Fotografia dei campi scritti dal motore, nell'ordine della mappa.
     * <p>
     * Comprende {@code v[31]} anche se è un campo di sola visualizzazione: è l'unico che il motore
     * scrive <b>dentro un altro movimento</b> (riga 742, il DTW che timbra la data sulla sua
     * controparte), quindi l'unico che una passata incrementale può toccare <i>prima</i> del punto
     * di ripartenza. Se incrementale e completo dovessero divergere lì, si vedrebbe qui.
     */
    private static List<String> fotografia() {
        List<String> righe = new ArrayList<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            righe.add(v[0] + " | 16=" + v[16] + " | 17=" + v[17] + " | 19=" + v[19]
                    + " | 31=" + v[31] + " | 33=" + v[33] + " | 38=" + v[38]);
        }
        return righe;
    }

    private static List<String> fotografiaDopoPassataCompleta() {
        costruisciScenario();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        return fotografia();
    }

    private static String[] movimentoDi(String id) {
        return Principale.MappaCryptoWallet.get(id);
    }

    // ------------------------------------------------------------------
    // 1 - Lo scenario esercita davvero il percorso fra gruppi wallet
    // ------------------------------------------------------------------

    @Test
    void loScenarioEsercitaIlTrasferimentoFraGruppiDiversi() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Il PTW legge il costo di carico dal Gruppo 01 senza toglierlo (ultimo parametro false)
        assertEquals("1000.00", movimentoDi(ID_PRELIEVO)[16],
                "Il PTW verso un altro gruppo deve riportare il costo di carico della moneta uscita");
        //Il DTW lo toglie dal Gruppo 01 e lo inserisce nel Gruppo 02
        assertEquals("1000.00", movimentoDi(ID_DEPOSITO)[17],
                "Il DTW deve ricevere il costo di carico prelevato dallo stack del gruppo di origine");
        //E la vendita sul WalletB lo ritrova: plusvalenza 1500-1000
        assertEquals("1000.00", movimentoDi(ID_VENDITA_B)[16],
                "La vendita dal secondo gruppo deve trovare il costo di carico arrivato col trasferimento");
        assertEquals("500.00", movimentoDi(ID_VENDITA_B)[19],
                "Plusvalenza attesa: 1500 di ricavo meno 1000 di costo di carico");
        assertEquals("S", movimentoDi(ID_VENDITA_B)[33]);
    }

    @Test
    void ilCollegatoInesistenteNonInterrompeIlRicalcolo() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //v[20] punta a un movimento cancellato: il motore lo tratta come non collegato.
        //Nota di onestà: il valore "" è lo stesso che si otterrebbe se la controparte esistesse ma
        //fosse nello stesso gruppo, quindi da solo non prova quale ramo sia stato percorso. Ciò che
        //il test garantisce è il confronto con ID_DEPOSITO — riga di forma identica (DTW su
        //WalletB, un solo collegato) che il costo di carico invece lo riceve — e la non
        //interruzione del ricalcolo, che è il rischio vero.
        assertEquals("", movimentoDi(ID_PENZOLANTE)[17],
                "Un DTW con collegato inesistente non riceve costo di carico");
        assertEquals("N", movimentoDi(ID_PENZOLANTE)[33]);
        assertEquals("1000.00", movimentoDi(ID_DEPOSITO)[17],
                "La riga gemella col collegato esistente deve invece ricevere il costo di carico");
        //e soprattutto i movimenti successivi sono stati elaborati lo stesso
        assertEquals("S", movimentoDi(ID_VENDITA_B)[33],
                "Il ricalcolo non deve essersi interrotto sul collegato mancante");
    }

    @Test
    void ilCostoDiCaricoDelleDonazioniSopravviveAlRicalcolo() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //v[17] è INGRESSO per i DDO: il motore lo rilegge e lo riscrive uguale (idempotente).
        //È la proprietà che rende l'impronta stabile pur includendo v[17] (paragrafo 4.3).
        assertEquals("250.00", movimentoDi(ID_DONAZIONE)[17],
                "Il costo di carico inserito a mano sulla donazione non deve essere sovrascritto");
        assertEquals("250.00", movimentoDi(ID_VENDITA_DONO)[16],
                "La vendita della donazione deve usare il costo di carico dichiarato");

        List<String> dopoUnaPassata = fotografia();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals(dopoUnaPassata, fotografia(),
                "Due ricalcoli consecutivi sugli stessi dati devono dare lo stesso risultato");
    }

    // ------------------------------------------------------------------
    // 2 - Equivalenza: ciclo manuale e ripartenza da checkpoint
    // ------------------------------------------------------------------

    @Test
    void ilCicloManualeEquivaleAllaPassataDelMotore() {
        List<String> completa = fotografiaDopoPassataCompleta();

        costruisciScenario();
        elaboraDa(Principale.MappaCryptoWallet.firstKey(), new TreeMap<>());

        assertEquals(completa, fotografia(),
                "Elaborare i movimenti uno a uno deve dare lo stesso risultato di AggiornaPlusvalenze()");
    }

    @Test
    void ripartireDaOgniCheckpointEquivaleAllaPassataUnica() {
        List<String> completa = fotografiaDopoPassataCompleta();

        //Nessun movimento è stato modificato: ripartire da un checkpoint qualsiasi deve ricostruire
        //esattamente lo stesso risultato. È la proprietà su cui si regge tutto il ricalcolo
        //incrementale, e vale per OGNI punto di ripartenza, non solo per uno scelto bene.
        //Nota sul protocollo: NON si ricostruisce lo scenario prima della ripartenza. In un
        //ricalcolo incrementale vero i movimenti che precedono il punto di ripartenza conservano i
        //valori scritti dalla passata precedente; azzerarli renderebbe il confronto impossibile da
        //superare per un motivo che con l'incrementale non c'entra nulla.
        for (String chiave : new ArrayList<>(Principale.MappaCryptoWallet.keySet())) {
            costruisciScenario();
            Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> checkpoint = eseguiRaccogliendoCheckpoint();

            sporcaLeUsciteDa(chiave);
            elaboraDa(chiave, copiaStato(checkpoint.get(chiave)));

            assertEquals(completa, fotografia(),
                    "Ripartenza dal checkpoint di \"" + chiave + "\" diversa dalla passata unica");
        }
    }

    @Test
    void ilCheckpointNonVieneAlteratoDallaRielaborazioneSuccessiva() {
        //I lotti sono condivisi fra stato e copia (paragrafo 14.4): questo test fallisce se un
        //domani il motore comincia a modificarli in place, perché il checkpoint verrebbe sporcato
        //dalla passata successiva e la seconda ripartenza darebbe un risultato diverso.
        List<String> completa = fotografiaDopoPassataCompleta();

        costruisciScenario();
        Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> checkpoint = eseguiRaccogliendoCheckpoint();
        Map<String, Map<String, ArrayDeque<String[]>>> salvato = checkpoint.get(ID_DEPOSITO);

        sporcaLeUsciteDa(ID_DEPOSITO);
        elaboraDa(ID_DEPOSITO, copiaStato(salvato));
        assertEquals(completa, fotografia(), "Prima ripartenza dal checkpoint");

        //Riuso LO STESSO checkpoint dopo che una rielaborazione lo ha attraversato
        sporcaLeUsciteDa(ID_DEPOSITO);
        elaboraDa(ID_DEPOSITO, copiaStato(salvato));
        assertEquals(completa, fotografia(),
                "Il checkpoint è stato alterato dalla rielaborazione precedente: i lotti non sono più condivisibili");
    }

    // ------------------------------------------------------------------
    // 3 - La dipendenza all'indietro (paragrafo 5.1): il cuore del problema
    // ------------------------------------------------------------------

    /**
     * Modifica applicata al DTW: il wallet passa da WalletB (Gruppo 02) a WalletA (Gruppo 01),
     * cioè lo stesso gruppo del PTW. Da quel momento il trasferimento è interno a un gruppo e il
     * PTW non deve più riportare alcun costo di carico.
     */
    private static void modificaIlDeposito() {
        movimentoDi(ID_DEPOSITO)[3] = WALLET_A;
    }

    @Test
    void modificareIlDepositoCambiaAncheIlPrelievoCheLoPrecede() {
        costruisciScenario();
        modificaIlDeposito();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals("", movimentoDi(ID_PRELIEVO)[16],
                "Con la controparte nello stesso gruppo il PTW non deve riportare costo di carico");
        //E il PTW sta PRIMA del DTW nell'ordine della mappa: la dipendenza va all'indietro
        assertTrue(ID_PRELIEVO.compareTo(ID_DEPOSITO) < 0,
                "Lo scenario ha senso solo se il movimento modificato viene dopo quello che ne dipende");
    }

    /**
     * Il test che giustifica il digest dei collegati. Riparte dalla chiave modificata più piccola
     * — che è quella del DTW, perché è l'unico movimento toccato — e mostra che il PTW, essendo
     * più indietro, <b>non viene rielaborato e conserva un valore ormai sbagliato</b>.
     * Nessuna eccezione, nessun log: solo un dato fiscale errato.
     */
    /**
     * Riferimento per i due test qui sotto: prima passata sui dati originali, poi la modifica, poi
     * una passata completa — <b>senza mai ricostruire lo scenario</b>. La simmetria conta: i campi
     * "portati avanti" (v[31] e v[38], che il motore scrive ma non azzera mai) valgono diversamente
     * su dati appena caricati da file e su dati già passati sotto il motore, e confrontare i due
     * casi farebbe fallire il test per un motivo che non c'entra con la ripartenza.
     */
    private static List<String> riferimentoConModifica() {
        costruisciScenario();
        eseguiRaccogliendoCheckpoint();
        modificaIlDeposito();
        elaboraDa(Principale.MappaCryptoWallet.firstKey(), new TreeMap<>());
        return fotografia();
    }

    @Test
    void ripartenzaDallaChiaveSporcaMinima_senzaCollegati_lasciaUnRisultatoStantio() {
        List<String> corretta = riferimentoConModifica();
        assertEquals("", movimentoDi(ID_PRELIEVO)[16]);

        //Ricostruisco: passata completa sui dati ORIGINALI, checkpoint, poi la modifica
        costruisciScenario();
        Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> checkpoint = eseguiRaccogliendoCheckpoint();
        modificaIlDeposito();

        //Ripartenza dalla sola riga modificata: è la regola ingenua "min(chiave sporca)"
        elaboraDa(ID_DEPOSITO, copiaStato(checkpoint.get(ID_DEPOSITO)));

        assertEquals("1000.00", movimentoDi(ID_PRELIEVO)[16],
                "Atteso il valore STANTIO: il PTW non è stato rielaborato perché precede la modifica");
        assertNotEquals(corretta, fotografia(),
                "Se questo confronto non fallisse, il digest dei movimenti collegati sarebbe inutile");
    }

    /**
     * La stessa modifica, ripartendo dal PTW: è il punto che l'impronta col digest dei collegati
     * individua, perché la modifica del DTW cambia anche l'impronta del PTW. Il risultato torna
     * identico alla passata completa.
     */
    @Test
    void ripartenzaDalMovimentoCollegatoPiuVecchio_riproduceLaPassataCompleta() {
        List<String> corretta = riferimentoConModifica();

        costruisciScenario();
        Map<String, Map<String, Map<String, ArrayDeque<String[]>>>> checkpoint = eseguiRaccogliendoCheckpoint();
        modificaIlDeposito();

        elaboraDa(ID_PRELIEVO, copiaStato(checkpoint.get(ID_PRELIEVO)));

        assertEquals(corretta, fotografia(),
                "Ripartendo dal collegato più vecchio il risultato deve coincidere con la passata completa");
    }

    // ------------------------------------------------------------------
    // 4 - Il motore incrementale vero, attraverso l'API pubblica
    // ------------------------------------------------------------------

    /** Ricalcolo completo forzato, buttando via impronte e checkpoint. */
    private static List<String> ricalcoloCompletoForzato() {
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "Questa passata doveva essere completa");
        return fotografia();
    }

    /**
     * La proprietà centrale di tutto il lavoro: dopo una modifica, il ricalcolo incrementale deve
     * dare <b>lo stesso identico risultato</b> di quello completo. Il test verifica anche che il
     * percorso incrementale sia stato davvero preso: senza quel controllo passerebbe anche se ogni
     * passata fosse completa, cioè anche se l'ottimizzazione non esistesse.
     */
    @Test
    void dopoUnaModificaIlRicalcoloIncrementaleCoincideConQuelloCompleto() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Modifica di un movimento in mezzo allo storico: cambia il valore della donazione
        movimentoDi(ID_DONAZIONE)[17] = "180.00";

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "La modifica di un solo movimento non deve far ripartire tutto da capo");
        List<String> incrementale = fotografia();

        assertEquals(ricalcoloCompletoForzato(), incrementale,
                "Incrementale e completo devono coincidere dopo una modifica");
        assertEquals("180.00", movimentoDi(ID_VENDITA_DONO)[16],
                "La vendita successiva deve usare il nuovo costo di carico della donazione");
    }

    /**
     * Lo stesso confronto sul caso che il paragrafo 5.1 indica come il più pericoloso: il movimento
     * modificato (DTW) viene <b>dopo</b> quello che ne dipende (PTW). Se il digest dei collegati
     * non funzionasse, qui l'incrementale divergerebbe dal completo.
     */
    @Test
    void laDipendenzaAllIndietroEGestitaDalMotoreIncrementale() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("1000.00", movimentoDi(ID_PRELIEVO)[16]);

        modificaIlDeposito();

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa);
        List<String> incrementale = fotografia();

        assertEquals("", movimentoDi(ID_PRELIEVO)[16],
                "Il PTW che PRECEDE il movimento modificato deve essere stato rielaborato");
        assertEquals(ricalcoloCompletoForzato(), incrementale,
                "Incrementale e completo devono coincidere anche con la dipendenza all'indietro");
    }

    @Test
    void aggiungereERimuovereMovimentiVieneRilevato() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Aggiunta in mezzo allo storico
        movimento("2024-01-10 10.00_TEST_008_001_AC", "2024-01-10 10:00", WALLET_A, "", "ACQUISTO",
                "EUR", "FIAT", "500", "BTC", "Crypto", "5", "500", "", "M");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa);
        List<String> dopoAggiunta = fotografia();
        assertEquals(ricalcoloCompletoForzato(), dopoAggiunta, "Aggiunta di un movimento");

        //Rimozione dello stesso
        Principale.MappaCryptoWallet.remove("2024-01-10 10.00_TEST_008_001_AC");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa);
        List<String> dopoRimozione = fotografia();
        assertEquals(ricalcoloCompletoForzato(), dopoRimozione, "Rimozione di un movimento");
    }

    @Test
    void senzaModificheNonVieneRielaboratoNulla() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String> prima = fotografia();

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertEquals(0, Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati,
                "Senza modifiche non va rielaborato alcun movimento");
        assertEquals(prima, fotografia(), "E i valori in mappa devono restare quelli giusti");
    }

    /**
     * Un'opzione che cambia il verdetto su tutto lo storico senza che nessun movimento risulti
     * modificato: deve far tornare il ricalcolo completo, ed è il compito di
     * {@code OpzioniRicalcolo.Epoca()}.
     */
    @Test
    void cambiareUnOpzioneFaTornareIlRicalcoloCompleto() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "SI");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "Il cambio di un'opzione deve invalidare tutto lo storico");
        assertEquals(Principale.MappaCryptoWallet.size(), Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati);
    }

    /** Anche lo spostamento di un wallet in un altro gruppo cambia l'epoca: nessuna riga si modifica. */
    @Test
    void cambiareIlGruppoDiUnWalletFaTornareIlRicalcoloCompleto() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        DatabaseH2.Pers_GruppoWallet_Scrivi(WALLET_B, "Gruppo 03");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "Il cambio di gruppo wallet deve invalidare tutto lo storico");
        //Ripristino per non lasciare il database temporaneo in uno stato inatteso
        DatabaseH2.Pers_GruppoWallet_Scrivi(WALLET_B, "Gruppo 02");
    }

    @Test
    void disattivareLOpzioneRiportaAllaPassataCompleta() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        DatabaseH2.Pers_Opzioni_Scrivi(Calcoli_PlusvalenzeNew.OPZIONE_INCREMENTALE, "NO");

        movimentoDi(ID_DONAZIONE)[17] = "180.00";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "Con l'opzione a NO ogni ricalcolo deve essere completo");
    }

    /**
     * Con un checkpoint ogni 2 movimenti la ripartenza usa davvero un checkpoint <i>intermedio</i>
     * invece di ricominciare dal primo movimento: è il meccanismo che sul dataset reale limita il
     * lavoro a un blocco da 5.000.
     */
    @Test
    void laRipartenzaUsaUnCheckpointIntermedio() {
        Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = 2;
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //Modifico l'ultima vendita: la ripartenza deve toccare pochi movimenti, non tutti e 7
        movimentoDi(ID_VENDITA_DONO)[15] = "500";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String> incrementale = fotografia();

        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa);
        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati < Principale.MappaCryptoWallet.size(),
                "La ripartenza doveva usare un checkpoint intermedio, invece ha rielaborato tutto ("
                        + Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati + " movimenti)");
        assertEquals(ricalcoloCompletoForzato(), incrementale);
    }

    /**
     * I dettagli LIFO per movimento vengono accumulati con {@code push} su un {@code LifoXID}
     * recuperato con {@code computeIfAbsent}: se una passata incrementale rielaborasse un movimento
     * senza prima togliere la sua voce, le pile di dettaglio raddoppierebbero a ogni ricalcolo.
     * Non se ne accorgerebbe nessun confronto sui campi 16/17/19/33.
     */
    @Test
    void iDettagliLifoNonSiAccumulanoTraUnaPassataELaltra() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        int lottiUsciti = Calcoli_PlusvalenzeNew.getIDLiFo(ID_VENDITA_B).StackUscito.size();
        assertTrue(lottiUsciti > 0, "La vendita deve avere consumato almeno un lotto");

        //Modifica prima della vendita: la vendita viene rielaborata dalla passata incrementale
        movimentoDi(ID_DONAZIONE)[17] = "180.00";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa);

        assertEquals(lottiUsciti, Calcoli_PlusvalenzeNew.getIDLiFo(ID_VENDITA_B).StackUscito.size(),
                "I lotti del dettaglio LIFO si sono accumulati invece di essere ricalcolati");
    }

    /**
     * I movimenti che precedono il punto di ripartenza non vengono rielaborati, quindi il loro
     * dettaglio LIFO deve restare quello della passata precedente e non sparire: la maschera di
     * dettaglio usa {@code getIDLiFo(ID) != null} per decidere quali movimenti mostrare.
     */
    /**
     * §14.1 — la proprietà di sicurezza dell'intero lavoro. Se il motore esplode a metà, la mappa
     * resta scritta a metà: senza il {@code try/finally} che azzera impronte e checkpoint, quel
     * prefisso corrotto risulterebbe "già verificato" e nessuna passata successiva lo toccherebbe
     * più. Un errore transitorio diventerebbe un dato fiscale sbagliato permanente.
     * <p>
     * Il movimento velenoso ha il wallet vuoto, che fa lanciare
     * {@code IllegalArgumentException} a {@code Pers_GruppoWallet_Leggi} sulla prima riga di
     * {@code ElaboraMovimento}.
     */
    @Test
    void unEccezioneAMetaPassataRiportaIlRicalcoloSuccessivoACompleto() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        String idVelenoso = "2024-02-20 10.00_TEST_009_001_AC";
        String[] velenoso = movimento(idVelenoso, "2024-02-20 10:00", "", "", "ACQUISTO",
                "EUR", "FIAT", "100", "BTC", "Crypto", "1", "100", "", "M");

        assertThrows(IllegalArgumentException.class, Calcoli_PlusvalenzeNew::AggiornaPlusvalenze,
                "Il movimento con wallet vuoto doveva far fallire il ricalcolo");

        //Rimedio al dato sbagliato e ricalcolo: DEVE essere una passata completa
        velenoso[3] = WALLET_A;
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                "Dopo un'eccezione lo stato incrementale va buttato: la passata dopo deve essere completa");

        List<String> dopoErrore = fotografia();
        assertEquals(ricalcoloCompletoForzato(), dopoErrore,
                "E il risultato deve essere quello del ricalcolo completo");
    }

    @Test
    void iDettagliLifoDeiMovimentiNonRielaboratiRestanoDisponibili() {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        movimentoDi(ID_VENDITA_DONO)[15] = "500";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        assertNotNull(Calcoli_PlusvalenzeNew.getIDLiFo(ID_ACQUISTO),
                "Il dettaglio LIFO di un movimento non rielaborato non deve essere stato buttato");
    }
}
