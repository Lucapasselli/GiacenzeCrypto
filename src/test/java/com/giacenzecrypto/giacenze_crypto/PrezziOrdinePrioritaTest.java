package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa l'<b>ordine di priorità</b> con cui {@link Prezzi#CambioXXXEUR} interroga il DB interno.
 *
 * <p>Perché esiste: chi ha già presentato una dichiarazione ha anche già sistemato i suoi prezzi, che
 * sono quindi presenti nel DB interno. Finché l'ordine con cui li si interroga resta quello, un
 * ricalcolo di RW/RT di annualità passate restituisce gli stessi valori — ed è questa, non una soglia
 * sulle date, l'unica garanzia che regge su <b>qualunque</b> archivio. I quadri W/RW chiedono i prezzi
 * di inizio e fine periodo da {@code Calcoli_RW:535} e {@code :1790}, entrambi con fonte vuota e
 * {@code includiVecchi=true}: è esattamente lo scenario coperto qui.
 *
 * <p>Il test è <b>sintetico di proposito</b>: costruisce le tre fonti a mano su un database
 * temporaneo e verifica <i>quale gradino vince</i>, non quale numero esce. Un test che fissasse i
 * valori di fine anno di un archivio reale proteggerebbe quell'archivio e nessun altro, perché la
 * distribuzione delle fonti cambia da utente a utente.
 *
 * <p>I tre gradini, nell'ordine che questo test blocca:
 * <ol>
 *   <li>prezzi personalizzati (±60 min), {@code PrezziNew} di {@code personale.mv.db}</li>
 *   <li>archivio orario {@code XXXEUR} all'ora esatta, solo con {@code includiVecchi=true}</li>
 *   <li>cache prezzi (±5 min), {@code PrezziNew} di {@code prezzi.mv.db}</li>
 * </ol>
 * Il gradino 2 è invariante per costruzione (una riga per chiave primaria); i gradini 1 e 3 scelgono
 * fra righe concorrenti nella finestra, quindi dipendono dall'ordinamento — vedi
 * {@link #aParitaDiDistanzaVinceLExchangeAlfabeticamentePrimo()}.
 */
class PrezziOrdinePrioritaTest {

    @TempDir
    static Path tempDir;

    /** 2024-06-01 12:00 UTC: nel passato e dopo il 2017, le due soglie di {@code CambioXXXEUR}. */
    private static final long ISTANTE = 1717243200000L;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        //Il servizio remoto è opt-in e qui va spento comunque: una risposta di rete renderebbe
        //l'esito dipendente dalla connessione invece che dall'ordine dei gradini.
        System.setProperty("prezzi.servizio.abilitato", "false");
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
        System.clearProperty("prezzi.servizio.abilitato");
    }

    @AfterEach
    void ripulisceInterruzione() {
        Interruzione.Azzera();
    }

    // ---------------------------------------------------------------- helper

    private static void scriviRiga(java.sql.Connection conn, String simbolo, long ts,
            String exchange, String prezzo) throws Exception {
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, exchange);
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal(prezzo));
            ps.executeUpdate();
        }
    }

    /** Prezzo personalizzato nella forma di oggi: exchange "Personalizzato" e gruppo "TUTTI" in colonne separate. */
    private static void personalizzato(String simbolo, String prezzo) throws Exception {
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address, gruppo) "
                + "KEY (timestamp, exchange, symbol, rete, address, gruppo) VALUES (?, ?, ?, ?, '', '', 'TUTTI')";
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(sql)) {
            ps.setLong(1, ISTANTE);
            ps.setString(2, "Personalizzato");
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal(prezzo));
            ps.executeUpdate();
        }
    }

    private static void cache(String simbolo, String exchange, String prezzo) throws Exception {
        scriviRiga(DatabaseH2.connectionPrezzi, simbolo, ISTANTE, exchange, prezzo);
    }

    /** Gradino 2. {@code personalizzato=false} → {@code database.mv.db}, cioè "DB Interno (Old)". */
    private static void legacyOrario(String simbolo, String prezzo) {
        DatabaseH2.OLD_XXXEUR_Scrivi(
                FunzioniDate.ConvertiDatadaLongallOra(ISTANTE) + " " + simbolo, prezzo, false);
    }

    private static Prezzi.InfoPrezzo chiedi(String simbolo, boolean includiVecchi) {
        return Prezzi.CambioXXXEUR(simbolo, "1", ISTANTE, "", "", "", includiVecchi);
    }

    // ----------------------------------------------------------------- test

    @Test
    void ilPersonalizzatoVinceSuArchivioOrarioECache() throws Exception {
        personalizzato("TSTA", "111");
        legacyOrario("TSTA", "222");
        cache("TSTA", "binance", "333");

        Prezzi.InfoPrezzo ip = chiedi("TSTA", true);

        assertNotNull(ip, "con tutte e tre le fonti presenti un prezzo va restituito");
        assertEquals(new BigDecimal("111").compareTo(ip.prezzoUnitario), 0,
                "il gradino 1 (personalizzati) deve vincere sugli altri due");
        assertEquals("Personalizzato", ip.Fonte);
    }

    @Test
    void senzaPersonalizzatoVinceLArchivioOrarioSullaCache() throws Exception {
        legacyOrario("TSTB", "222");
        cache("TSTB", "binance", "333");

        Prezzi.InfoPrezzo ip = chiedi("TSTB", true);

        assertNotNull(ip);
        assertEquals(new BigDecimal("222").compareTo(ip.prezzoUnitario), 0,
                "il gradino 2 (XXXEUR) deve vincere sulla cache prezzi");
        assertEquals("DB Interno (Old)", ip.Fonte);
    }

    @Test
    void senzaIncludiVecchiLArchivioOrarioVieneIgnorato() throws Exception {
        legacyOrario("TSTC", "222");
        cache("TSTC", "binance", "333");

        Prezzi.InfoPrezzo ip = chiedi("TSTC", false);

        assertNotNull(ip);
        assertEquals(new BigDecimal("333").compareTo(ip.prezzoUnitario), 0,
                "con includiVecchi=false il gradino 2 è saltato e vince la cache");
        assertEquals("binance", ip.Fonte);
    }

    @Test
    void conSolaCacheVinceLaCache() throws Exception {
        cache("TSTD", "binance", "333");

        Prezzi.InfoPrezzo ip = chiedi("TSTD", true);

        assertNotNull(ip);
        assertEquals(new BigDecimal("333").compareTo(ip.prezzoUnitario), 0);
        assertEquals("binance", ip.Fonte);
    }

    @Test
    void ilRipiegoNonCCXTVieneConservatoENonPerso() throws Exception {
        //Una fonte di ripiego (CoinMarketCap, GC, DB orario) non chiude la ricerca: viene tenuta da
        //parte e restituita se più avanti non si trova nulla di più preciso. Qui si interrompe prima
        //della fase di rete, che è il punto in cui il ripiego viene restituito in modo deterministico
        //e senza dipendere dalla connessione della macchina che esegue i test.
        cache("TSTE", "CoinMarketCap", "444");

        Interruzione.Apri();
        Interruzione.Chiedi();
        Prezzi.InfoPrezzo ip = chiedi("TSTE", true);
        Interruzione.Chiudi();

        assertNotNull(ip, "il ripiego già in cache non deve andare perso quando si esce prima della rete");
        assertEquals(new BigDecimal("444").compareTo(ip.prezzoUnitario), 0);
        assertEquals("CoinMarketCap", ip.Fonte);
    }

    @Test
    void aParitaDiDistanzaVinceLExchangeAlfabeticamentePrimo() throws Exception {
        //Oggi l'ordinamento è "ORDER BY ABS(timestamp - ?)" senza tie-break, e fra righe alla stessa
        //distanza vince quella che H2 restituisce scandendo in ordine di chiave primaria
        //(timestamp, exchange, ...), cioè l'exchange primo in alfabeto fra quelli presenti.
        //Questo test fissa quell'esito: è il valore già finito nelle dichiarazioni, e va riprodotto
        //identico quando l'ordinamento verrà reso esplicito.
        cache("TSTF", "kucoin", "555");
        cache("TSTF", "binance", "666");

        Prezzi.InfoPrezzo ip = chiedi("TSTF", true);

        assertNotNull(ip);
        assertEquals("binance", ip.Fonte,
                "a parità di distanza temporale deve vincere l'exchange primo in ordine alfabetico");
        assertEquals(new BigDecimal("666").compareTo(ip.prezzoUnitario), 0);
    }

    @Test
    void ilTieBreakNonDipendeDallOrdineDiInserimento() throws Exception {
        //Controprova del test precedente, che inserisce kucoin per primo: se a decidere fosse
        //l'ordine di scrittura invece della chiave primaria, invertendolo cambierebbe il vincitore.
        cache("TSTH", "binance", "666");
        cache("TSTH", "kucoin", "555");

        Prezzi.InfoPrezzo ip = chiedi("TSTH", true);

        assertNotNull(ip);
        assertEquals("binance", ip.Fonte,
                "il vincitore non deve dipendere dall'ordine in cui le righe sono state scritte");
    }

    @Test
    void laSceltaAParitaDiDistanzaSegueLOrdineAlfabeticoDellExchange() throws Exception {
        //Controprova della regola dichiarata nel piano: non vince "binance" per privilegio, vince il
        //primo in ordine alfabetico fra quelli presenti — e fra gli otto id CCXT binance è già il
        //primo, il che rende i due comportamenti indistinguibili passando da CambioXXXEUR. Qui si
        //interroga direttamente la query (la stessa che il passo 2 renderà esplicita), con un nome
        //che ordina prima: se la regola vera fosse "binance sempre", un ORDER BY per exchange
        //cambierebbe i prezzi già dichiarati invece di riprodurli.
        cache("TSTI", "binance", "666");
        cache("TSTI", "ascendex", "111");

        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoDaDatabase("TSTI", ISTANTE, "", "", "", 5, BigDecimal.ONE);

        assertNotNull(ip);
        assertEquals("ascendex", ip.Fonte,
                "a parità di distanza deve vincere l'exchange primo in alfabeto, non binance per privilegio");
    }

    @Test
    void fonteSpecificaAssenteNonRipiegaSuiPersonalizzatiDiAltreFonti() throws Exception {
        //Asimmetria reale fra i due lettori, da non "uniformare" per distrazione:
        //DammiPrezzoDaDatabasePersonale ha un vero else (Prezzi.java:2993), quindi con una fonte
        //specificata che non trova nulla NON prova tutte le fonti; DammiPrezzoDaDatabase invece
        //esegue comunque la queryAll come ripiego.
        personalizzato("TSTG", "777");

        assertNull(Prezzi.DammiPrezzoDaDatabasePersonale("TSTG", ISTANTE, "okx", "", "", 60, BigDecimal.ONE),
                "con fonte 'okx' il lettore dei personalizzati non deve ripiegare su una fonte diversa");
        assertNotNull(Prezzi.DammiPrezzoDaDatabasePersonale("TSTG", ISTANTE, "", "", "", 60, BigDecimal.ONE),
                "con fonte vuota (il caso di RW) deve invece trovare la riga personalizzata");
    }

    @Test
    void ilGruppoWalletComeFonteTrovaIlPersonalizzato() throws Exception {
        //Regressione del 2026-09-17: "Giacenze a data" passa come fonte il gruppo ("TUTTI") e il lettore lo
        //cercava dentro `exchange`; da quando il gruppo e' una colonna sua ("Personalizzato" + "TUTTI") non
        //trovava piu' nulla, mentre RW (fonte vuota) il prezzo lo trovava: stesso token, valori diversi.
        personalizzato("TSTZ", "888");

        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoDaDatabasePersonale("TSTZ", ISTANTE, "TUTTI", "", "", 60, BigDecimal.ONE);

        assertNotNull(ip, "il gruppo TUTTI deve trovare il prezzo personalizzato salvato con gruppo TUTTI");
        assertEquals(0, new BigDecimal("888").compareTo(ip.prezzoUnitario));
        assertNotNull(Prezzi.DammiPrezzoDaDatabasePersonale("TSTZ", ISTANTE, "tutti", "", "", 60, BigDecimal.ONE),
                "il confronto sul gruppo e' case-insensitive");
        assertNull(Prezzi.DammiPrezzoDaDatabasePersonale("TSTZ", ISTANTE, "Wallet 01", "", "", 60, BigDecimal.ONE),
                "un altro gruppo non deve vedere il prezzo del gruppo TUTTI");
    }
}
