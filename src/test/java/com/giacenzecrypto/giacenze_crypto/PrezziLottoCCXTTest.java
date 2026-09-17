package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova end-to-end del percorso a lotti: {@link Prezzi#RecuperaPrezziDaCCXTLotto} deve lanciare
 * <b>una sola</b> invocazione di {@code Historical_Multi_Eur.js} per molte coppie (moneta, finestra)
 * e scriverne i punti in {@code PrezziNew}.
 *
 * <p><b>Perché è un test di integrazione e non una prova manuale.</b> Il percorso ha tre pezzi che
 * si rompono solo insieme: il JSON scritto su stdin da un thread dedicato, il parsing dell'array di
 * esiti, e la scrittura in cache. Compilare non dimostra nessuno dei tre. Il tutto passa da un
 * processo Node vero, quindi il test <b>si auto-salta</b> quando Node o lo script non ci sono (come
 * il golden master fa col dataset reale): un salto su una macchina senza Node o senza rete è
 * l'esito previsto, non un fallimento.
 *
 * <p>Nota sul perché il lotto esiste, misurato: le stesse 6 richieste costano ~3.300 ms l'una a
 * invocazioni separate e ~650 ms dentro un lotto, e nel lotto interrogare tutti e otto gli exchange
 * non costa più che interrogarne uno.
 */
class PrezziLottoCCXTTest {

    @TempDir
    static Path tempDir;

    /** Finestra di un'ora nel passato: 2025-02-04, la stessa su cui è stato misurato il bug A9. */
    private static final long SINCE = 1738635180000L;
    private static final long UNTIL = 1738638780000L;

    private static String risorseOriginali;

    @BeforeAll
    static void apre() {
        System.setProperty("prezzi.servizio.abilitato", "false");
        //I due percorsi sono distinti e devono restare tali: la working directory e' la @TempDir (ci
        //vanno i database), mentre pathRisorse deve puntare alla radice del progetto, dove sta
        //Scripts/. Di norma lo imposta l'avvio dell'applicazione; nei test nessuno lo fa e resta
        //vuoto, quindi getPathRisorse()+"Scripts/..." non trova lo script e il percorso a lotti non
        //parte affatto - il test si auto-salterebbe dando l'impressione di passare.
        risorseOriginali = VarStatiche.getPathRisorse();
        VarStatiche.setPathRisorse(System.getProperty("user.dir") + "/");

        //La working directory NON va puntata a un archivio reale: CreaoCollegaDatabase apre le tre
        //connessioni in scrittura ed esegue le migrazioni di schema (inclusa quella con DROP/ADD
        //PRIMARY KEY su PrezziNew), quindi lo farebbe sui dati veri dell'utente durante una suite di
        //test. Resta percio' la @TempDir, dove i database nascono vuoti.
        //Node pero' vive sotto la working directory (tools/node) e in una @TempDir non c'e'. Invece
        //di rinunciare alla prova end-to-end, si collega `tools` all'archivio con un SYMLINK: Node
        //viene letto da li' senza che nessun database reale venga mai aperto.
        try {
            for (String archivio : new String[]{"test/Dichiarazione 2025", "test/2025", "test/2025.test"}) {
                Path tools = Path.of(System.getProperty("user.dir"), archivio, "tools");
                if (Files.exists(tools.resolve("node"))) {
                    Files.createSymbolicLink(tempDir.resolve("tools"), tools);
                    break;
                }
            }
        } catch (Exception ex) {
            //symlink non supportato o archivio assente: i test end-to-end si auto-salteranno
        }
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
        System.clearProperty("prezzi.servizio.abilitato");
        //Ripristino: pathRisorse e' statico e condiviso, lasciarlo sporco romperebbe classi lontane.
        VarStatiche.setPathRisorse(risorseOriginali);
    }

    /**
     * Salta il test se l'ambiente non può eseguire lo script.
     *
     * <p><b>Il vincolo vero è Node</b>, non lo script: {@code getNodeExePath()} lo cerca sotto la
     * <i>working directory</i> (in {@code tools/node}, dove viene scaricato al primo uso), e una
     * {@code @TempDir} appena creata non lo contiene. Sul computer di un utente il percorso è
     * corretto — è l'ambiente di test a essere particolare, non il codice di produzione. Per questo
     * {@link #apre()} punta la working directory a un archivio che Node ce l'ha già, e qui si salta
     * quando quell'archivio non esiste: su una macchina pulita o in CI la classe si salta invece di
     * fallire, esattamente come fa il golden master col dataset privato.
     */
    private static void richiedeNodeEScript() {
        Assumptions.assumeTrue(Files.exists(CcxtInterop.getNodeExePath()),
                "Node non installato in questo ambiente: prova saltata");
        Assumptions.assumeTrue(
                Files.exists(Path.of(VarStatiche.getPathRisorse() + "Scripts/Historical_Multi_Eur.js")),
                "Script dei prezzi non trovato: prova saltata");
    }

    private static long righeInCache(String simbolo) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "SELECT COUNT(*) FROM PrezziNew WHERE symbol = ?")) {
            ps.setString(1, simbolo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    @Test
    void unLottoScaricaPiuMoneteInUnaSolaInvocazioneEScriveInCache() throws Exception {
        richiedeNodeEScript();

        List<Prezzi.RichiestaPrezzo> richieste = new ArrayList<>();
        richieste.add(new Prezzi.RichiestaPrezzo("BTC", SINCE, UNTIL));
        richieste.add(new Prezzi.RichiestaPrezzo("ETH", SINCE, UNTIL));

        List<Prezzi.EsitoLotto> esiti = Prezzi.RecuperaPrezziDaCCXTLotto(richieste, "binance");

        assertEquals(2, esiti.size(), "un esito per ogni richiesta ricevuta");
        //Se la rete non risponde gli esiti sono tutti non-risposti: e' un ambiente senza connessione,
        //non un difetto del codice, quindi il test si ferma qui invece di fallire.
        Assumptions.assumeTrue(esiti.stream().allMatch(e -> e.risposto),
                "lo script non ha risposto (rete assente?): prova saltata");

        for (Prezzi.EsitoLotto e : esiti) {
            assertTrue(e.punti > 0, e.simbolo + " deve aver restituito punti");
            assertTrue(e.falliti.isEmpty(), e.simbolo + " non deve avere exchange falliti: " + e.falliti);
        }

        assertTrue(righeInCache("BTC") > 0, "i punti di BTC devono essere finiti in PrezziNew");
        assertTrue(righeInCache("ETH") > 0, "i punti di ETH devono essere finiti in PrezziNew");
    }

    @Test
    void unaRichiestaGiaCopertaInSessioneNonVieneRispedita() throws Exception {
        richiedeNodeEScript();

        List<Prezzi.RichiestaPrezzo> prima = List.of(new Prezzi.RichiestaPrezzo("SOL", SINCE, UNTIL));
        List<Prezzi.EsitoLotto> uno = Prezzi.RecuperaPrezziDaCCXTLotto(prima, "binance");
        Assumptions.assumeTrue(uno.size() == 1 && uno.get(0).risposto,
                "lo script non ha risposto (rete assente?): prova saltata");

        //Seconda richiesta identica: il dedup di sessione la copre, quindi niente processo Node e
        //zero punti nuovi, ma per il chiamante resta "risposta" (i dati sono gia' in cache).
        long t0 = System.currentTimeMillis();
        List<Prezzi.EsitoLotto> due = Prezzi.RecuperaPrezziDaCCXTLotto(prima, "binance");
        long durata = System.currentTimeMillis() - t0;

        assertEquals(1, due.size());
        assertTrue(due.get(0).risposto, "una richiesta gia' coperta conta come risposta");
        assertEquals(0, due.get(0).punti, "non deve aver riscaricato nulla");
        assertTrue(durata < 1000,
                "senza lanciare Node deve tornare subito, invece ha impiegato " + durata + " ms");
    }

    @Test
    void dopoUnoScaricoLOraRisultaMarcataPerGliExchangeCheHannoRisposto() throws Exception {
        richiedeNodeEScript();

        long oraInt = FunzioniDate.OraIntYYYYMMDDHH(SINCE);
        //Nessun exchange deve risultare marcato prima: la tabella nasce vuota nella @TempDir.
        for (String ex : Prezzi.EXCHANGE_CCXT) {
            assertFalse(Prezzi.OraCCXT_Leggi("XLM", oraInt, ex),
                    ex + " non puo' risultare gia' interrogato su un database appena creato");
        }

        List<Prezzi.EsitoLotto> esiti = Prezzi.RecuperaPrezziDaCCXTLotto(
                List.of(new Prezzi.RichiestaPrezzo("XLM", SINCE, UNTIL)), "binance");
        Assumptions.assumeTrue(esiti.size() == 1 && esiti.get(0).risposto,
                "lo script non ha risposto (rete assente?): prova saltata");

        //Si marca chi ha risposto, e solo lui: e' la regola che impedisce a un blip di rete di
        //congelare un buco per sempre.
        Prezzi.EsitoLotto e = esiti.get(0);
        for (String ex : Prezzi.EXCHANGE_CCXT) {
            if (e.falliti.contains(ex)) continue;
            Prezzi.OraCCXT_Scrivi("XLM", oraInt, ex);
        }

        assertTrue(Prezzi.OraCCXT_Leggi("XLM", oraInt, "binance"),
                "binance ha risposto, quindi l'ora deve risultare interrogata per binance");
        assertFalse(Prezzi.OraCCXT_Leggi("XLM", oraInt + 1, "binance"),
                "il marcatore e' per ora: un'ora diversa resta non interrogata");
        assertFalse(Prezzi.OraCCXT_Leggi("ETH", oraInt, "binance"),
                "il marcatore e' per simbolo: un'altra moneta resta non interrogata");
    }

    @Test
    void ilMarcatoreOrarioDistingueGliExchangeFraLoro() {
        long oraInt = FunzioniDate.OraIntYYYYMMDDHH(SINCE);
        Prezzi.OraCCXT_Scrivi("TESTMARK", oraInt, "binance");

        assertTrue(Prezzi.OraCCXT_Leggi("TESTMARK", oraInt, "binance"));
        assertFalse(Prezzi.OraCCXT_Leggi("TESTMARK", oraInt, "okx"),
                "marcare binance non deve far credere che sia stato interrogato anche okx: e' "
                + "esattamente il difetto del vecchio marcatore a giornata, che non aveva l'exchange in chiave");
    }

    @Test
    void unaListaVuotaNonLanciaNulla() {
        assertTrue(Prezzi.RecuperaPrezziDaCCXTLotto(List.of(), "binance").isEmpty());
        assertTrue(Prezzi.RecuperaPrezziDaCCXTLotto(null, "binance").isEmpty());
    }

    /** Scrive un prezzo nella cache ({@code prezzi.mv.db}) come farebbe uno scaricamento riuscito. */
    private static void prezzoInCache(String simbolo, long timestamp, String exchange) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "MERGE INTO PrezziNew (timestamp, exchange, symbol, rete, address, prezzo) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, '', '', ?)")) {
            ps.setLong(1, timestamp);
            ps.setString(2, exchange);
            ps.setString(3, simbolo);
            ps.setDouble(4, 1.23);
            ps.executeUpdate();
        }
    }

    /**
     * Il difetto che questi tre casi fissano: il pre-scarico raccoglieva le coppie (moneta, ora) che
     * servivano <b>senza guardare se fossero già risolte</b>, e le rispediva tutte a Node — su un
     * archivio maturo, o semplicemente alla seconda corsa, quasi lavoro interamente inutile.
     *
     * <p>A differenza degli altri test di questa classe <b>non servono né Node né rete</b>: provano il
     * filtro, non lo scaricamento, quindi girano davvero ovunque invece di auto-saltarsi.
     */
    @Test
    void unOraGiaInterrogataDaTuttiGliExchangeNonVieneRichiesta() {
        long oraInt = FunzioniDate.OraIntYYYYMMDDHH(SINCE);
        for (String ex : Prezzi.EXCHANGE_CCXT) {
            Prezzi.OraCCXT_Scrivi("FILTROMARK", oraInt, ex);
        }

        List<Prezzi.RichiestaPrezzo> rimaste = Prezzi.FiltraRichiesteGiaCoperte(List.of(
                new Prezzi.RichiestaPrezzo("FILTROMARK", SINCE, UNTIL),
                new Prezzi.RichiestaPrezzo("FILTROIGNOTA", SINCE, UNTIL)), null);

        assertEquals(1, rimaste.size(), "la moneta con tutti gli exchange già interrogati va scartata");
        assertEquals("FILTROIGNOTA", rimaste.get(0).simbolo);
    }

    /**
     * È il caso dell'utente che ha già l'archivio pieno: i prezzi stanno in {@code PrezziNew} perché
     * li ha scaricati il vecchio percorso a giornata, e {@code PrezziOraCCXT} è vuoto. Senza questo
     * secondo controllo il marcatore non filtrerebbe nulla e si riscaricherebbe tutto.
     */
    @Test
    void unPrezzoGiaInCacheDaUnExchangeCCXTNonVieneRichiesto() throws Exception {
        prezzoInCache("FILTROCACHE", SINCE + 600000L, "binance");

        //L'istante coincide col prezzo in cache: è il caso in cui anche CambioXXXEUR rinuncia a
        //scaricare, quindi chiederlo sarebbe lavoro certamente inutile.
        List<Prezzi.RichiestaPrezzo> rimaste = Prezzi.FiltraRichiesteGiaCoperte(
                List.of(new Prezzi.RichiestaPrezzo("FILTROCACHE", SINCE, UNTIL, SINCE + 600000L)), null);

        assertTrue(rimaste.isEmpty(),
                "con un prezzo CCXT entro ±5 minuti dall'istante non c'è nulla da scaricare");
    }

    /**
     * La correzione del 2026-09-17 al filtro introdotto lo stesso giorno: prima bastava un prezzo
     * <i>qualsiasi</i> dentro l'ora (±30 minuti) per considerarla coperta, una tolleranza <b>più
     * larga</b> di quella con cui {@code CambioXXXEUR} rinuncia a scaricare (±5 minuti dall'istante).
     * Risultato: il pre-scarico saltava la moneta, il consumatore la scaricava lo stesso, e il lotto
     * perdeva il suo guadagno proprio dove serve di più — sull'ora corrente, che non viene mai
     * marcata e quindi non è mai coperta dall'altra condizione.
     */
    @Test
    void unPrezzoNellaStessaOraMaLontanoDallIstanteNonBastaAConsiderarlaCoperta() throws Exception {
        prezzoInCache("FILTROLONTANO", SINCE + 600000L, "binance");

        //Prezzo al minuto 10 dell'ora, istante al minuto 50: stessa ora, ma 40 minuti di distanza.
        List<Prezzi.RichiestaPrezzo> rimaste = Prezzi.FiltraRichiesteGiaCoperte(
                List.of(new Prezzi.RichiestaPrezzo("FILTROLONTANO", SINCE, UNTIL, SINCE + 3000000L)), null);

        assertEquals(1, rimaste.size(),
                "a 40 minuti di distanza il consumatore scaricherebbe comunque: la richiesta deve restare");
    }

    /**
     * Il contrario del precedente, ed è la ragione per cui il filtro guarda la <b>fonte</b> e non si
     * accontenta di "esiste una riga": davanti a un prezzo di ripiego {@code CambioXXXEUR} scarica
     * comunque, quindi scartare qui lascerebbe quel movimento al percorso lento di un processo Node
     * per volta — cioè il filtro si mangerebbe il guadagno che deve produrre.
     */
    @Test
    void unPrezzoDiRipiegoNonBastaAConsiderareLOraCoperta() throws Exception {
        prezzoInCache("FILTRORIPIEGO", SINCE + 600000L, "GC");

        //Istante sul prezzo in cache: così a decidere è la FONTE e non la distanza, che è ciò che
        //questo test dichiara di verificare.
        List<Prezzi.RichiestaPrezzo> rimaste = Prezzi.FiltraRichiesteGiaCoperte(
                List.of(new Prezzi.RichiestaPrezzo("FILTRORIPIEGO", SINCE, UNTIL, SINCE + 600000L)), null);

        assertEquals(1, rimaste.size(),
                "un prezzo non-CCXT è un ripiego: gli exchange vanno interrogati lo stesso");
    }

    @Test
    void unaFinestraNelFuturoVieneScartataSenzaLanciareNulla() {
        long futuro = System.currentTimeMillis() + 86400000L;
        List<Prezzi.EsitoLotto> esiti = Prezzi.RecuperaPrezziDaCCXTLotto(
                List.of(new Prezzi.RichiestaPrezzo("BTC", futuro, futuro + 3600000L)), "binance");

        assertTrue(esiti.isEmpty(), "una finestra interamente nel futuro non ha prezzi da scaricare");
    }
}
