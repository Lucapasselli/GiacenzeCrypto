package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link RecuperoDatabase}: un file {@code .mv.db} danneggiato si sposta (mai si cancella) e se ne crea uno nuovo
 * oppure si ripristina la copia fatta prima della compattazione, ma solo con il consenso dell'utente e solo per una
 * corruzione vera. File di prova inventati, mai l'archivio reale.
 */
class RecuperoDatabaseTest {

    @TempDir
    Path cartella;

    @AfterEach
    void chiudiConnessioni() {
        DatabaseH2.ChiudiConnessioniAperte();
    }

    /** Decide in base alla domanda e ricorda cosa gli è stato chiesto. */
    private static final class SceltaFinta implements RecuperoDatabase.Scelta {

        final Function<Boolean, RecuperoDatabase.Azione> decisione;
        final List<String> file = new ArrayList<>();
        final List<String> testi = new ArrayList<>();
        final List<Boolean> conCopia = new ArrayList<>();
        final List<String> errori = new ArrayList<>();

        SceltaFinta(RecuperoDatabase.Azione sempre) {
            this(c -> sempre);
        }

        SceltaFinta(Function<Boolean, RecuperoDatabase.Azione> decisione) {
            this.decisione = decisione;
        }

        @Override
        public RecuperoDatabase.Azione scegli(String f, String testo, boolean copia) {
            file.add(f);
            testi.add(testo);
            conCopia.add(copia);
            return decisione.apply(copia);
        }

        @Override
        public void errore(String testo) {
            errori.add(testo);
        }
    }

    /** Un file che H2 non riconosce come suo: stessa famiglia di errore di un MVStore interrotto a metà scrittura. */
    private void scriviDanneggiato(String nomeFile) throws IOException {
        byte[] rumore = new byte[64 * 1024];
        new Random(42).nextBytes(rumore);
        Files.write(cartella.resolve(nomeFile), rumore);
    }

    private List<String> nomiNellaCartella() throws IOException {
        try (Stream<Path> s = Files.list(cartella)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private void usaCartella() {
        VarStatiche.setWorkingDirectory(cartella.toString() + "/");
    }

    private static void esegui(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static int conta(Connection c, String tabella) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabella)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Apre i tre database nella cartella di prova e vi scrive una riga riconoscibile in prezzi e nel principale. */
    private void apriConDati() throws SQLException {
        usaCartella();
        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(new SceltaFinta(RecuperoDatabase.Azione.CHIUDI)));
        esegui(DatabaseH2.connectionPrezzi,
                "INSERT INTO PrezziNew (timestamp, exchange, symbol, rete, address, prezzo) VALUES (1, 'X', 'BTC', '', '', 1.5)");
        esegui(DatabaseH2.connection,
                "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome, NuovoNome) VALUES ('a', 'b', 'c')");
    }

    private List<String> copieDiSicurezza() throws IOException {
        return nomiNellaCartella().stream().filter(n -> n.contains(RecuperoDatabase.SUFFISSO_COPIA)).toList();
    }

    /** Come a fine avvio: legge per intero i file con una copia e, se è tutto a posto, la cancella. */
    private static int verificaEElimina() {
        return RecuperoDatabase.verificaEEliminaCopie(DatabaseH2::NuovaConnessione, f -> { });
    }

    /** Chiude e riapre come un secondo avvio: i file ora esistono, quindi le loro copie non sono "da tenere". */
    private void riapriComeSecondoAvvio() {
        DatabaseH2.ChiudiConnessioniAperte();
        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(new SceltaFinta(RecuperoDatabase.Azione.CHIUDI)));
    }

    // ---- riconoscimento ----

    @Test
    void riconosceLaCorruzioneDalCodiceOdalTesto() {
        assertTrue(RecuperoDatabase.eCorruzione(new SQLException("File corrupted while reading record", "90030", 90030)));
        assertTrue(RecuperoDatabase.eCorruzione(new SQLException("General error",
                new IllegalStateException("File is corrupted - unable to recover a valid set of chunks [2.2.224/6]"))),
                "il codice 50000 non nomina il file ma la causa sì");
    }

    @Test
    void nonScambiaPerCorruzioneGliAltriErrori() {
        assertFalse(RecuperoDatabase.eCorruzione(null));
        assertFalse(RecuperoDatabase.eCorruzione(new SQLException("Database may be already in use", "90020", 90020)),
                "il lock di un'altra sessione non si risolve spostando il file");
        assertFalse(RecuperoDatabase.eCorruzione(new SQLException("Unsupported database file version", "90048", 90048)),
                "un formato più nuovo non è un file rotto");
        assertFalse(RecuperoDatabase.eCorruzione(new SQLException("Wrong user name or password", "28000", 28000)));
    }

    // ---- spostamento ----

    @Test
    void spostaIlFileSenzaCancellarloENonSovrascriveUnoSpostatoPrima() throws IOException {
        Path file = cartella.resolve("prezzi.mv.db");
        Files.writeString(file, "contenuto");
        LocalDateTime ora = LocalDateTime.of(2026, 11, 9, 19, 45, 30);

        Path primo = RecuperoDatabase.spostaDaParte(cartella + "/", RecuperoDatabase.FILE_PREZZI, ora);

        assertFalse(Files.exists(file), "il file originale non deve restare al suo posto");
        assertEquals("prezzi.mv.db.danneggiato-20261109-194530", primo.getFileName().toString());
        assertEquals("contenuto", Files.readString(primo), "il contenuto deve restare intatto");

        Files.writeString(file, "altro");
        Path secondo = RecuperoDatabase.spostaDaParte(cartella + "/", RecuperoDatabase.FILE_PREZZI, ora);
        assertEquals("prezzi.mv.db.danneggiato-20261109-194530-2", secondo.getFileName().toString());
        assertEquals("contenuto", Files.readString(primo), "il primo non va toccato dal secondo spostamento");
        assertEquals("altro", Files.readString(secondo));
    }

    // ---- apertura ----

    @Test
    void cartellaSanaSiApreSenzaChiedereNulla() {
        usaCartella();
        SceltaFinta scelta = new SceltaFinta(RecuperoDatabase.Azione.NUOVO);

        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(scelta));
        assertTrue(scelta.file.isEmpty(), "nessuna domanda se i file sono integri");
    }

    @Test
    void fileDanneggiatoConConsensoVieneSpostatoELaCartellaRipartePulita() throws IOException {
        usaCartella();
        scriviDanneggiato("prezzi.mv.db");
        SceltaFinta scelta = new SceltaFinta(RecuperoDatabase.Azione.NUOVO);

        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(scelta));

        assertEquals(List.of("prezzi"), scelta.file, "chiede solo del file rotto");
        assertEquals(List.of(false), scelta.conCopia, "senza copia di sicurezza non offre il ripristino");
        assertTrue(scelta.testi.get(0).contains("prezzi.mv.db"), "il testo nomina il file");
        assertTrue(scelta.testi.get(0).contains("non viene cancellato"));
        List<String> nomi = nomiNellaCartella();
        assertTrue(nomi.stream().anyMatch(n -> n.startsWith("prezzi.mv.db.danneggiato-")), nomi.toString());
        assertTrue(nomi.contains("prezzi.mv.db"), "ne esiste uno nuovo");
        assertNotNull(DatabaseH2.connectionPrezzi);
    }

    @Test
    void dueFileDanneggiatiSiSpostanoUnoAllaVolta() throws IOException {
        usaCartella();
        scriviDanneggiato("database.mv.db");
        scriviDanneggiato("prezzi.mv.db");
        SceltaFinta scelta = new SceltaFinta(RecuperoDatabase.Azione.NUOVO);

        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(scelta));

        assertEquals(List.of("database", "prezzi"), scelta.file);
        List<String> nomi = nomiNellaCartella();
        assertTrue(nomi.stream().anyMatch(n -> n.startsWith("database.mv.db.danneggiato-")), nomi.toString());
        assertTrue(nomi.stream().anyMatch(n -> n.startsWith("prezzi.mv.db.danneggiato-")), nomi.toString());
    }

    @Test
    void senzaConsensoNonSiTocchiNullaESiChiude() throws IOException {
        usaCartella();
        scriviDanneggiato("database.mv.db");
        byte[] prima = Files.readAllBytes(cartella.resolve("database.mv.db"));
        SceltaFinta scelta = new SceltaFinta(RecuperoDatabase.Azione.CHIUDI);

        assertEquals(RecuperoDatabase.Esito.RIFIUTATO, RecuperoDatabase.apri(scelta));

        assertEquals(List.of("database"), scelta.file);
        assertTrue(nomiNellaCartella().stream().noneMatch(n -> n.contains("danneggiato")), "nessun file rinominato");
        assertArrayEquals(prima, Files.readAllBytes(cartella.resolve("database.mv.db")),
                "il file non deve essere modificato dal tentativo di apertura");
    }

    // ---- copia di sicurezza ----

    @Test
    void ilControlloDelloSpazioLasciaUnMargine() {
        long mb = 1024L * 1024L;
        assertFalse(RecuperoDatabase.spazioSufficiente(400 * mb, 400 * mb), "lo spazio pari al file non basta");
        assertFalse(RecuperoDatabase.spazioSufficiente(450 * mb, 400 * mb), "il 10% senza il margine fisso non basta");
        assertTrue(RecuperoDatabase.spazioSufficiente(470 * mb, 400 * mb));
    }

    @Test
    void compattareCreaUnaSolaCopiaPerDatabaseECiSiRiapreSopra() throws Exception {
        apriConDati();

        assertTrue(DatabaseH2.CompattaPrezzi(true));
        assertTrue(DatabaseH2.CompattaPrincipale(true));
        assertEquals(List.of("database.mv.db.precompattazione", "prezzi.mv.db.precompattazione"), copieDiSicurezza());

        //La seconda compattazione sostituisce la copia, non ne aggiunge una: una per database, mai di più
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        assertEquals(List.of("database.mv.db.precompattazione", "prezzi.mv.db.precompattazione"), copieDiSicurezza());

        assertEquals(1, conta(DatabaseH2.connectionPrezzi, "PrezziNew"), "il contenuto è lo stesso dopo la compattazione");
        assertEquals(1, conta(DatabaseH2.connection, "RINOMINATOKEN"));
    }

    @Test
    void laCopiaDiSicurezzaEUnDatabaseValidoConLoStatoPrecedente() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        DatabaseH2.ChiudiConnessioniAperte();

        Path altra = Files.createDirectory(cartella.resolve("verifica"));
        Files.copy(cartella.resolve("prezzi.mv.db.precompattazione"), altra.resolve("prezzi.mv.db"));
        try (Connection c = DriverManager.getConnection("jdbc:h2:" + altra + "/prezzi", "sa", "")) {
            assertEquals(1, conta(c, "PrezziNew"), "la copia si apre da sola e contiene la riga scritta prima");
        }
    }

    @Test
    void compattareSenzaRiaprireLasciaLaCopiaEIlDatabaseChiuso() throws Exception {
        apriConDati();

        assertTrue(DatabaseH2.CompattaPrezzi(false));

        assertTrue(DatabaseH2.connectionPrezzi.isClosed(), "chiudendo l'applicazione non si riapre");
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza());
    }

    @Test
    void seLaCopiaNonRiesceNonSiCompattaELaConnessioneResta() throws Exception {
        apriConDati();
        //Un .tmp che è una cartella piena: la copia non può scriverci sopra
        Path ostacolo = Files.createDirectory(cartella.resolve("prezzi.mv.db.precompattazione.tmp"));
        Files.writeString(ostacolo.resolve("x"), "x");

        assertFalse(DatabaseH2.CompattaPrezzi(true), "senza rete di sicurezza non si compatta");

        assertFalse(DatabaseH2.connectionPrezzi.isClosed(), "la sessione deve poter continuare");
        assertEquals(1, conta(DatabaseH2.connectionPrezzi, "PrezziNew"), "nessun dato toccato");
        assertTrue(copieDiSicurezza().stream().noneMatch(n -> n.equals("prezzi.mv.db.precompattazione")));
    }

    // ---- ripristino e cancellazione ----

    @Test
    void conUnaCopiaSiPuoRipristinareELaCopiaRestaFinoAFineAvvio() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        DatabaseH2.ChiudiConnessioniAperte();
        scriviDanneggiato("prezzi.mv.db");
        SceltaFinta scelta = new SceltaFinta(RecuperoDatabase.Azione.RIPRISTINA);

        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(scelta));

        assertEquals(List.of("prezzi"), scelta.file);
        assertEquals(List.of(true), scelta.conCopia, "con la copia offre il ripristino");
        assertTrue(scelta.testi.get(0).contains("copia di sicurezza fatta prima dell'ultima compattazione"));
        assertEquals(1, conta(DatabaseH2.connectionPrezzi, "PrezziNew"), "i dati sono quelli della copia");
        assertTrue(nomiNellaCartella().stream().anyMatch(n -> n.startsWith("prezzi.mv.db.danneggiato-")),
                "il file danneggiato resta, rinominato");
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza(),
                "finché l'avvio non è completo la copia non si cancella");

        assertEquals(1, verificaEElimina(), "a fine avvio completo sì");
        assertTrue(copieDiSicurezza().isEmpty());
    }

    @Test
    void scegliendoUnFileNuovoLaCopiaNonVieneCancellataAFineAvvio() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        DatabaseH2.ChiudiConnessioniAperte();
        scriviDanneggiato("prezzi.mv.db");

        assertEquals(RecuperoDatabase.Esito.APERTO,
                RecuperoDatabase.apri(new SceltaFinta(RecuperoDatabase.Azione.NUOVO)));

        assertEquals(0, conta(DatabaseH2.connectionPrezzi, "PrezziNew"), "il file nuovo è vuoto");
        assertEquals(0, verificaEElimina(), "l'utente non ha ripristinato: la copia è ancora l'unica");
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza());
    }

    @Test
    void unaCopiaIllegibileNonSiRipropone() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        DatabaseH2.ChiudiConnessioniAperte();
        scriviDanneggiato("prezzi.mv.db");
        scriviDanneggiato("prezzi.mv.db.precompattazione");
        SceltaFinta scelta = new SceltaFinta(copia -> copia ? RecuperoDatabase.Azione.RIPRISTINA : RecuperoDatabase.Azione.NUOVO);

        assertEquals(RecuperoDatabase.Esito.APERTO, RecuperoDatabase.apri(scelta));

        assertEquals(List.of(true, false), scelta.conCopia,
                "la copia rimessa al suo posto e ancora rotta non viene offerta due volte");
        assertEquals(0, conta(DatabaseH2.connectionPrezzi, "PrezziNew"));
        //Scegliere il file nuovo dopo un ripristino fallito non dimostra niente sulla copia: a fine avvio non si cancella
        assertEquals(0, verificaEElimina());
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza());
    }

    @Test
    void aFineAvvioSiCancellanoLeCopieDeiFileSaniMaNonQuelleDiFileAssenti() throws Exception {
        apriConDati();
        DatabaseH2.ChiudiConnessioniAperte();
        Files.writeString(cartella.resolve("database.mv.db.precompattazione"), "copia");
        Files.writeString(cartella.resolve("prezzi.mv.db.precompattazione"), "copia");
        Files.writeString(cartella.resolve("prezzi.mv.db.precompattazione.tmp"), "a meta");
        Files.delete(cartella.resolve("personale.mv.db"));
        Files.writeString(cartella.resolve("personale.mv.db.precompattazione"), "copia");

        //personale.mv.db era assente all'apertura: l'apertura lo crea vuoto, e questo non dimostra niente sulla sua copia
        assertEquals(RecuperoDatabase.Esito.APERTO,
                RecuperoDatabase.apri(new SceltaFinta(RecuperoDatabase.Azione.CHIUDI)));

        assertEquals(2, verificaEElimina());
        assertEquals(List.of("personale.mv.db.precompattazione"), copieDiSicurezza(),
                "restano la copia del file assente e nessun .tmp");
    }

    // ---- lettura completa e impronta ----

    @Test
    void lImprontaNonCambiaConLaCompattazioneECambiaSeUnaRigaCambia() throws Exception {
        apriConDati();
        String prima = RecuperoDatabase.impronta(DatabaseH2.connectionPrezzi);

        assertTrue(DatabaseH2.CompattaPrezzi(true));

        assertEquals(prima, RecuperoDatabase.impronta(DatabaseH2.connectionPrezzi),
                "la compattazione cambia dove stanno le righe, non quali sono");
        esegui(DatabaseH2.connectionPrezzi, "UPDATE PrezziNew SET prezzo = 1.6");
        assertFalse(prima.equals(RecuperoDatabase.impronta(DatabaseH2.connectionPrezzi)), "un valore diverso cambia l'impronta");
    }

    @Test
    void compattarePrincipaleConfrontaLaCompattazioneConLaCopia() throws Exception {
        apriConDati();

        assertTrue(DatabaseH2.CompattaPrincipale(true), "il confronto con la copia deve riuscire su un file sano");

        assertEquals(1, conta(DatabaseH2.connection, "RINOMINATOKEN"));
        assertTrue(copieDiSicurezza().contains("database.mv.db.precompattazione"));
        assertTrue(nomiNellaCartella().stream().noneMatch(n -> n.startsWith(".verifica-")),
                "la cartella temporanea del confronto non deve restare");
    }

    @Test
    void seIlFileCompattatoNonCoincideConLaCopiaSiRimetteLaCopia() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrincipale(true));
        DatabaseH2.ChiudiConnessioniAperte();
        //Il file ora differisce dalla copia (una riga in più): come un compattato che avesse perso o cambiato dati
        try (Connection c = DriverManager.getConnection("jdbc:h2:" + cartella + "/database", "sa", "")) {
            esegui(c, "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome, NuovoNome) VALUES ('x', 'y', 'z')");
        }
        String cartellaConBarra = cartella + "/";
        String improntaCopia = RecuperoDatabase.improntaDellaCopia(cartellaConBarra, "database");

        assertFalse(RecuperoDatabase.confermaCompattazione(cartellaConBarra, "database", improntaCopia));

        assertArrayEquals(Files.readAllBytes(cartella.resolve("database.mv.db.precompattazione")),
                Files.readAllBytes(cartella.resolve("database.mv.db")), "il file è tornato quello della copia");
        try (Connection c = DriverManager.getConnection("jdbc:h2:" + cartella + "/database", "sa", "")) {
            assertEquals(1, conta(c, "RINOMINATOKEN"), "la riga in più non c'è più");
        }
    }

    @Test
    void unaCopiaDiversaDallOriginaleVieneScartataENonSiCompatta() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrincipale(true));
        DatabaseH2.ChiudiConnessioniAperte();
        String cartellaConBarra = cartella + "/";
        //La copia coincide con l'originale: l'impronta si restituisce
        assertEquals(RecuperoDatabase.improntaDellaCopia(cartellaConBarra, "database"),
                RecuperoDatabase.improntaCopiaVerificata(cartellaConBarra, "database"));

        //Ora l'originale cambia (una riga in più) e la copia no: come una copia con un valore cambiato in silenzio
        try (Connection c = DriverManager.getConnection("jdbc:h2:" + cartella + "/database", "sa", "")) {
            esegui(c, "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome, NuovoNome) VALUES ('x', 'y', 'z')");
        }
        byte[] originale = Files.readAllBytes(cartella.resolve("database.mv.db"));

        assertThrows(IOException.class, () -> RecuperoDatabase.improntaCopiaVerificata(cartellaConBarra, "database"));

        assertFalse(copieDiSicurezza().contains("database.mv.db.precompattazione"),
                "la copia sbagliata non deve restare: un domani si userebbe per ripristinare");
        assertArrayEquals(originale, Files.readAllBytes(cartella.resolve("database.mv.db")), "l'originale non va toccato");
    }

    @Test
    void laCopiaSiCancellaSoloSeIlFileSiLeggePerIntero() throws Exception {
        apriConDati();
        assertTrue(DatabaseH2.CompattaPrezzi(true));
        riapriComeSecondoAvvio();
        List<String> avvisi = new ArrayList<>();

        //Un errore di corruzione: la copia resta e l'utente viene avvisato
        assertEquals(0, RecuperoDatabase.verificaEEliminaCopie(
                f -> { throw new SQLException("File corrupted while reading record", "90030", 90030); }, avvisi::add));
        assertEquals(List.of("prezzi"), avvisi);
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza());

        //Un altro errore (il programma che si chiude a metà scansione): la copia resta, senza allarmi
        avvisi.clear();
        assertEquals(0, RecuperoDatabase.verificaEEliminaCopie(
                f -> { throw new SQLException("Database is closed", "90098", 90098); }, avvisi::add));
        assertTrue(avvisi.isEmpty());
        assertEquals(List.of("prezzi.mv.db.precompattazione"), copieDiSicurezza());

        //Lettura riuscita: ora sì
        assertEquals(1, RecuperoDatabase.verificaEEliminaCopie(DatabaseH2::NuovaConnessione, avvisi::add));
        assertTrue(copieDiSicurezza().isEmpty());
        assertTrue(avvisi.isEmpty());
    }

    // ---- testi ----

    @Test
    void ilTestoDicePerOgniFileCosaSiPerdeESenzaPuntoEVirgola() {
        usaCartella();
        assertTrue(RecuperoDatabase.testoAvviso("database").contains("SCAM"));
        assertTrue(RecuperoDatabase.testoAvviso("personale").contains("prezzi inseriti a mano"));
        assertTrue(RecuperoDatabase.testoAvviso("prezzi").contains("riscaricati"));
        assertTrue(RecuperoDatabase.testoAvviso("prezzi").contains("fine anno"), "i prezzi di fine anno sono ciò che vale in quel file");
        assertTrue(RecuperoDatabase.testoAvviso("prezzi").contains("backup"));
        LocalDateTime copiaDel = LocalDateTime.of(2026, 6, 6, 13, 27);
        for (String f : new String[]{"database", "personale", "prezzi"}) {
            assertFalse(RecuperoDatabase.testoAvviso(f).contains(";"), "niente punto e virgola nei testi per l'utente");
            assertFalse(RecuperoDatabase.testoAvviso(f, copiaDel).contains(";"));
        }
        assertTrue(RecuperoDatabase.testoAvviso("prezzi", copiaDel).contains("06/06/2026 alle 13:27"));
        assertFalse(RecuperoDatabase.testoAvviso("prezzi").contains("copia di sicurezza"));
    }
}
