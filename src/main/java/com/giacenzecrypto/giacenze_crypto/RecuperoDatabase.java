package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Apertura dei tre database H2 con una via d'uscita quando uno dei file è danneggiato, e copia di
 * sicurezza che precede la compattazione.
 *
 * <p>Prima di questa classe un file {@code .mv.db} corrotto (tipicamente un'interruzione durante la
 * scrittura: chiusura forzata, spegnimento, sincronizzazione della cartella) faceva comparire un
 * dialogo che terminava la sessione, e l'utente doveva scoprire da solo che bastava spostare il file.
 * Ora il programma chiede, e se l'utente accetta <b>rinomina</b> il file (mai cancellarlo: resta
 * analizzabile) e ne crea uno nuovo, oppure, se esiste, ripristina la copia di sicurezza.
 *
 * <p>Non è innocuo e il testo del dialogo lo dice: {@code database.mv.db} e {@code personale.mv.db}
 * contengono dati dell'utente (marcature SCAM, wallet, prezzi inseriti a mano...), che un file nuovo
 * non ha. Per questo non c'è mai uno spostamento automatico, e il pulsante predefinito è chiudere.
 *
 * <p>Si interviene solo su una corruzione riconosciuta con certezza ({@link #eCorruzione(Throwable)})
 * e di cui si sa il file ({@link DatabaseH2#getFileDatabaseInErrore()}): un lock di un'altra sessione,
 * un formato più nuovo o un errore nella creazione delle tabelle restano il dialogo di prima.
 *
 * <h2>La copia di sicurezza</h2>
 *
 * Il {@code SHUTDOWN DEFRAG} della compattazione riscrive il file sul posto: un'interruzione lì in mezzo
 * lascia un file che nessuno strumento di H2 sa riparare. Prima di compattare, {@code DatabaseH2} chiude il
 * database, ne copia il file in {@code <nome>.mv.db.precompattazione} (<b>una sola copia per database</b>, la
 * nuova sostituisce la precedente) e solo allora compatta. La copia si fa a file chiuso perché i
 * {@code .mv.db} aperti tengono un lock di H2 e non sono copiabili a caldo (vedi {@code Backup_Restore}).
 *
 * <p>La copia si cancella <b>soltanto alla fine del primo avvio completo</b> successivo e solo dopo una lettura
 * completa del file ({@link #VerificaEEliminaCopieInBackground()}, chiamata da {@code Principale}): finché il
 * programma non ha dimostrato di ripartire e il file di leggersi per intero, è l'unica cosa che resta della
 * versione precedente. Non si cancella la copia di un file che in quell'avvio è stato sostituito o non esisteva,
 * perché lì non ha dimostrato niente.
 *
 * <p>Per {@code database.mv.db}, che ha dati dell'utente e costa meno di un secondo, la compattazione è anche
 * <b>confrontata</b>: l'impronta del contenuto (righe e hash per tabella) del file compattato deve coincidere
 * con quella della copia, altrimenti si rimette la copia ({@link #confermaCompattazione}). MVStore non ha un
 * checksum sul contenuto delle pagine: solo il confronto con un'altra copia vede un'alterazione. Per
 * {@code prezzi.mv.db} (8 s per 8,8 milioni di righe, <b>20 s</b> contando due impronte) non si fa: è una cache,
 * e quello che vale, i prezzi di fine anno, si ripristina dal backup del programma.
 */
public final class RecuperoDatabase {

    static final String FILE_PRINCIPALE = "database";
    static final String FILE_PERSONALE = "personale";
    static final String FILE_PREZZI = "prezzi";
    private static final String[] TUTTI_I_FILE = {FILE_PRINCIPALE, FILE_PERSONALE, FILE_PREZZI};

    /** Suffisso della copia di sicurezza: {@code prezzi.mv.db.precompattazione}. */
    static final String SUFFISSO_COPIA = ".precompattazione";

    /** Massimo di file spostati in un avvio: i database sono tre. */
    private static final int MAX_SPOSTAMENTI = 3;

    /** Margine sopra la dimensione del file per decidere che c'è spazio per la copia: 10% più 16 MB. */
    private static final long MARGINE_SPAZIO_MINIMO = 16L * 1024L * 1024L;

    /**
     * File la cui copia di sicurezza <b>non</b> va cancellata a fine avvio: sostituiti dall'utente in questo
     * avvio, o assenti all'apertura. Si azzera a ogni {@link #apri(Scelta)}: vale per un avvio.
     */
    private static final Set<String> COPIE_DA_TENERE = ConcurrentHashMap.newKeySet();

    /** Come finisce l'apertura dei database. */
    enum Esito {
        /** I database sono aperti (eventualmente dopo aver spostato uno o più file). */
        APERTO,
        /** L'utente ha scelto di non toccare il file: non c'è altro da dire, si chiude. */
        RIFIUTATO,
        /** Lo spostamento o il ripristino sono falliti e l'utente ne è già stato informato: si chiude senza un secondo dialogo. */
        SEGNALATO,
        /** Non aperti: il chiamante mostra il dialogo di errore con il dettaglio. */
        FALLITO
    }

    /** Cosa vuole fare l'utente con un file danneggiato. */
    enum Azione {
        /** Rimettere al suo posto la copia fatta prima dell'ultima compattazione. */
        RIPRISTINA,
        /** Spostare il file danneggiato e ripartire con uno nuovo e vuoto. */
        NUOVO,
        /** Non toccare niente e chiudere il programma. */
        CHIUDI
    }

    /** Le due domande che il recupero rivolge all'utente: iniettabili per non aprire finestre nei test. */
    interface Scelta {
        /**
         * @param conCopia {@code true} se esiste una copia di sicurezza da ripristinare (altrimenti
         *                 {@link Azione#RIPRISTINA} non è tra le scelte)
         */
        Azione scegli(String file, String testo, boolean conCopia);

        /** Segnala che lo spostamento o il ripristino non sono riusciti. */
        void errore(String testo);
    }

    private RecuperoDatabase() {
    }

    /** Apre i database con il dialogo vero (Swing). */
    static Esito apri() {
        return apri(new SceltaSwing());
    }

    /**
     * Apre i tre database e, se uno è danneggiato, propone di ripristinare la copia o di spostarlo e ripartire.
     *
     * <p>Lo splash viene chiuso prima del primo dialogo (altrimenti resterebbe sospeso sopra di esso)
     * e riaperto se alla fine i database si aprono. I tempi di quell'avvio non descrivono quanto
     * costano le fasi, perché dentro c'è l'attesa dell'utente: non vanno salvati.
     */
    static Esito apri(Scelta scelta) {
        COPIE_DA_TENERE.clear();
        String cartella = VarStatiche.getWorkingDirectory();
        for (String f : TUTTI_I_FILE) {
            //Un file assente viene creato vuoto dall'apertura: non dimostra niente sulla copia della versione precedente
            if (!Files.exists(Paths.get(cartella + f + ".mv.db"))) {
                COPIE_DA_TENERE.add(f);
            }
        }
        boolean aperto = DatabaseH2.CreaoCollegaDatabase();
        boolean splashChiuso = false;
        Set<String> copiaGiaRipristinata = new HashSet<>();
        for (int giro = 0; !aperto && giro < MAX_SPOSTAMENTI && DatabaseH2.isErroreDatabaseCorrotto(); giro++) {
            String file = DatabaseH2.getFileDatabaseInErrore();
            if (!splashChiuso && SplashAvvio.attivo()) {
                SplashAvvio.chiudi();
                splashChiuso = true;
            }
            //Una copia già rimessa al suo posto e ancora illeggibile non si ripropone: sarebbe un giro a vuoto
            Path copia = percorsoCopia(cartella, file);
            boolean haCopia = Files.isRegularFile(copia) && !copiaGiaRipristinata.contains(file);
            Azione azione = scelta.scegli(file, testoAvviso(file, haCopia ? dataCopia(copia) : null), haCopia);
            if (azione == Azione.CHIUDI) {
                return Esito.RIFIUTATO;
            }
            //Su Windows un file aperto non si rinomina: prima si chiude quello che l'apertura fallita ha lasciato
            DatabaseH2.ChiudiConnessioniAperte();
            Path spostato = null;
            try {
                spostato = spostaDaParte(cartella, file, LocalDateTime.now());
                System.out.println("Database danneggiato spostato in " + spostato);
                if (azione == Azione.RIPRISTINA && haCopia) {
                    ripristinaCopia(cartella, file);
                    copiaGiaRipristinata.add(file);
                    System.out.println("Ripristinata la copia di sicurezza di " + file + ".mv.db");
                } else if (Files.isRegularFile(copia)) {
                    //Anche una copia già rimessa al suo posto e ancora illeggibile: scegliere il file nuovo non dimostra
                    //niente sulla copia, che resta l'unica traccia della versione precedente
                    COPIE_DA_TENERE.add(file);
                }
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                COPIE_DA_TENERE.add(file);
                rimettiAlSuoPosto(cartella, file, spostato);
                scelta.errore("Non è stato possibile completare l'operazione sul file " + file + ".mv.db.\n"
                        + "Dettaglio errore: " + ex);
                return Esito.SEGNALATO;
            }
            aperto = DatabaseH2.CreaoCollegaDatabase();
        }
        if (aperto && splashChiuso) {
            SplashAvvio.tempiNonAttendibili();
            SplashAvvio.mostra();
            SplashAvvio.fase(SplashAvvio.Fase.DATABASE);
        }
        return aperto ? Esito.APERTO : Esito.FALLITO;
    }

    /**
     * @return {@code true} se l'eccezione, o una delle sue cause, dice che il file H2 è danneggiato:
     * codice 90030 oppure il testo {@code File is corrupted} di MVStore, che con il codice 50000 non
     * porta il nome del file. Volutamente stretto: spostare un file è un'azione vera.
     */
    static boolean eCorruzione(Throwable t) {
        int profondita = 0;
        for (Throwable c = t; c != null && profondita < 10; c = c.getCause(), profondita++) {
            if (c instanceof SQLException se && se.getErrorCode() == 90030) {
                return true;
            }
            String m = c.getMessage();
            if (m != null && m.contains("File is corrupted")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rinomina {@code <cartella><file>.mv.db} in {@code <file>.mv.db.danneggiato-AAAAMMGG-HHMMSS}
     * nella stessa cartella. Non sovrascrive niente: se il nome esiste aggiunge un numero.
     *
     * @param cartella la directory di lavoro, con la barra finale
     * @param file     {@link #FILE_PRINCIPALE}, {@link #FILE_PERSONALE} o {@link #FILE_PREZZI}
     * @return il nuovo percorso del file danneggiato
     */
    static Path spostaDaParte(String cartella, String file, LocalDateTime ora) throws IOException {
        Path origine = Paths.get(cartella + file + ".mv.db");
        String base = origine.getFileName() + ".danneggiato-" + ora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path destinazione = origine.resolveSibling(base);
        for (int n = 2; Files.exists(destinazione); n++) {
            destinazione = origine.resolveSibling(base + "-" + n);
        }
        return Files.move(origine, destinazione);
    }

    // ---- copia di sicurezza ----

    /** @return il percorso della copia di sicurezza del file indicato (esista o no) */
    static Path percorsoCopia(String cartella, String file) {
        return Paths.get(cartella + file + ".mv.db" + SUFFISSO_COPIA);
    }

    /** @return {@code true} se in una cartella con {@code liberi} byte disponibili ci sta una copia di {@code dimensione} byte */
    static boolean spazioSufficiente(long liberi, long dimensione) {
        return liberi >= dimensione + dimensione / 10 + MARGINE_SPAZIO_MINIMO;
    }

    /**
     * Copia {@code <file>.mv.db} in {@code <file>.mv.db.precompattazione}, sostituendo quella che c'era.
     *
     * <p><b>Il file deve essere chiuso</b> (il database dopo un {@code SHUTDOWN}): aperto tiene il lock di H2.
     * La copia passa da un {@code .tmp} e si rinomina solo a copia finita e di dimensione uguale, così
     * quella precedente non si perde se la nuova non finisce e una copia a metà non si scambia per valida.
     *
     * @throws IOException se manca lo spazio, la copia fallisce o la dimensione non torna: il chiamante non
     *                     deve compattare senza questa rete di sicurezza
     */
    static void creaCopiaPrecompattazione(String cartella, String file) throws IOException {
        Path origine = Paths.get(cartella + file + ".mv.db");
        long dimensione = Files.size(origine);
        long liberi = Files.getFileStore(origine.toAbsolutePath().getParent()).getUsableSpace();
        if (!spazioSufficiente(liberi, dimensione)) {
            throw new IOException("spazio libero insufficiente per la copia di sicurezza di " + file + ".mv.db (servono circa "
                    + (dimensione + dimensione / 10 + MARGINE_SPAZIO_MINIMO) / (1024 * 1024) + " MB)");
        }
        Path finale = percorsoCopia(cartella, file);
        Path temporanea = finale.resolveSibling(finale.getFileName() + ".tmp");
        try {
            Files.copy(origine, temporanea, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(temporanea) != dimensione) {
                throw new IOException("la copia di sicurezza di " + file + ".mv.db non ha la dimensione dell'originale");
            }
            try {
                Files.move(temporanea, finale, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporanea, finale, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(temporanea);
            throw ex;
        }
    }

    /**
     * Rimette la copia di sicurezza al posto del file (che il chiamante ha già spostato da parte). È una
     * <b>copia</b>: la copia di sicurezza resta fino alla fine dell'avvio completo.
     */
    static void ripristinaCopia(String cartella, String file) throws IOException {
        Files.copy(percorsoCopia(cartella, file), Paths.get(cartella + file + ".mv.db"));
    }

    /** Se un ripristino a metà è fallito, riporta il file danneggiato dov'era, così il file non risulta assente. */
    private static void rimettiAlSuoPosto(String cartella, String file, Path spostato) {
        if (spostato == null) {
            return;
        }
        Path origine = Paths.get(cartella + file + ".mv.db");
        try {
            Files.deleteIfExists(origine);
            Files.move(spostato, origine);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /** Apre una connessione di sola lettura al database indicato, per il controllo che precede la cancellazione della copia. */
    interface ApriLettura {
        Connection apri(String file) throws SQLException;
    }

    /**
     * Da chiamare <b>una volta sola, alla fine di un avvio completo</b>, cioè quando i database si sono aperti e
     * il programma ha caricato i dati: legge per intero i file che hanno una copia di sicurezza e, se non trova
     * errori, cancella la copia. Gira su un thread a parte a bassa priorità, con una <b>seconda</b> connessione
     * (quella dell'applicazione sarebbe bloccata per la durata della scansione).
     *
     * <p>Se l'applicazione si chiude prima della fine la copia resta e il controllo si ripete al prossimo avvio.
     */
    static void VerificaEEliminaCopieInBackground() {
        Thread t = new Thread(() -> verificaEEliminaCopie(DatabaseH2::NuovaConnessione, RecuperoDatabase::avvisaCopiaConservata),
                "verifica-copie-sicurezza");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * Il cuore sincrono di {@link #VerificaEEliminaCopieInBackground()}. Per ogni file con una copia: se non è in
     * {@link #COPIE_DA_TENERE} lo legge per intero ({@link #letturaCompleta}), e solo se la lettura riesce cancella la
     * copia. Un errore di corruzione (come quello dell'apertura) la conserva e chiama {@code avviso}, qualunque
     * altro errore (tipicamente la chiusura del programma a metà scansione) la conserva senza dire niente. Toglie
     * sempre i {@code .tmp} rimasti da una copia interrotta.
     *
     * @return quante copie sono state cancellate
     */
    static int verificaEEliminaCopie(ApriLettura apri, Consumer<String> avviso) {
        String cartella = VarStatiche.getWorkingDirectory();
        int cancellate = 0;
        for (String f : TUTTI_I_FILE) {
            Path copia = percorsoCopia(cartella, f);
            try {
                Files.deleteIfExists(copia.resolveSibling(copia.getFileName() + ".tmp"));
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }
            if (COPIE_DA_TENERE.contains(f) || !Files.isRegularFile(copia)) {
                continue;
            }
            try (Connection c = apri.apri(f)) {
                letturaCompleta(c);
            } catch (SQLException ex) {
                LoggerGC.ScriviErrore(ex);
                if (eCorruzione(ex)) {
                    avviso.accept(f);
                }
                continue;
            }
            try {
                if (Files.deleteIfExists(copia)) {
                    cancellate++;
                    System.out.println("Cancellata la copia di sicurezza di " + f + ".mv.db: avvio completo e file letto per intero");
                }
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
        return cancellate;
    }

    private static void avvisaCopiaConservata(String file) {
        SwingUtilities.invokeLater(() -> Messaggi.WarningMessage("Controllo del database",
                "Il controllo completo di " + file + ".mv.db ha trovato un errore. La copia di sicurezza fatta prima "
                + "dell'ultima compattazione è stata conservata nella cartella di lavoro (" + file + ".mv.db"
                + SUFFISSO_COPIA + "). Il dettaglio è nel log.", null));
    }

    // ---- lettura completa e impronta ----

    /** Nomi delle tabelle del database in ordine alfabetico, per un risultato che non dipende dall'ordine di creazione. */
    private static List<String> tabelle(Connection c) throws SQLException {
        List<String> nomi = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {
            while (rs.next()) {
                nomi.add(rs.getString(1));
            }
        }
        nomi.sort(Comparator.naturalOrder());
        return nomi;
    }

    /** Tutte le colonne di una tabella concatenate in un'espressione SQL sola, con il NULL reso visibile. */
    private static String colonneConcatenate(Connection c, String tabella) throws SQLException {
        StringBuilder e = new StringBuilder();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                     + "AND TABLE_NAME = '" + tabella + "' ORDER BY ORDINAL_POSITION")) {
            while (rs.next()) {
                if (e.length() > 0) {
                    e.append("||'|'||");
                }
                e.append("COALESCE(CAST(\"").append(rs.getString(1)).append("\" AS VARCHAR),'~')");
            }
        }
        return e.toString();
    }

    /**
     * Legge ogni riga di ogni tabella (tutte le colonne). Serve a far leggere a H2 ogni pagina del file: lo scopo è
     * l'eccezione, non il risultato. Misurato su 8,8 milioni di righe: 8 s, uguali a cache fredda e a cache calda,
     * cioè limitati dal processore e non dal disco.
     *
     * @throws SQLException se una pagina non si legge
     */
    static void letturaCompleta(Connection c) throws SQLException {
        for (String t : tabelle(c)) {
            String espressione = colonneConcatenate(c, t);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*), MAX(LENGTH(" + espressione + ")) FROM \"" + t + "\"")) {
                rs.next();
            }
        }
    }

    /**
     * Impronta del contenuto: per ogni tabella il numero di righe e la somma degli hash delle righe. La somma non
     * dipende dall'ordine, quindi due file con lo stesso contenuto disposto in modo diverso (prima e dopo una
     * compattazione) hanno la stessa impronta. Legge ogni riga, quindi vale anche come {@link #letturaCompleta}.
     */
    static String impronta(Connection c) throws SQLException {
        StringBuilder s = new StringBuilder();
        for (String t : tabelle(c)) {
            String espressione = colonneConcatenate(c, t);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*), SUM(ORA_HASH(" + espressione + ")) FROM \"" + t + "\"")) {
                rs.next();
                s.append(t).append(':').append(rs.getLong(1)).append(':').append(rs.getLong(2)).append('|');
            }
        }
        return s.toString();
    }

    /** Impronta di un file {@code .mv.db} <b>chiuso</b> (il cui URL JDBC è dato senza il {@code .mv.db}), aperto in sola lettura. */
    private static String improntaDaUrl(String urlSenzaOpzioni) throws SQLException {
        try (Connection c = DriverManager.getConnection(urlSenzaOpzioni + ";ACCESS_MODE_DATA=r", DatabaseH2.usernameH2, DatabaseH2.passwordH2)) {
            return impronta(c);
        }
    }

    /**
     * Impronta della copia di sicurezza di {@code file}, che prima si <b>collega</b> (o copia, se il collegamento
     * non è possibile) in una cartella temporanea come {@code <file>.mv.db}: H2 apre solo file con quel nome.
     * La cartella sta dentro la cartella di lavoro e non in {@code /tmp}, che può essere in RAM e piccola.
     */
    static String improntaDellaCopia(String cartella, String file) throws IOException, SQLException {
        Path temporanea = Files.createTempDirectory(Paths.get(cartella), ".verifica-");
        try {
            Path destinazione = temporanea.resolve(file + ".mv.db");
            try {
                Files.createLink(destinazione, percorsoCopia(cartella, file));
            } catch (IOException | UnsupportedOperationException ex) {
                Files.copy(percorsoCopia(cartella, file), destinazione);
            }
            return improntaDaUrl("jdbc:h2:" + temporanea + "/" + file);
        } finally {
            try (java.util.stream.Stream<Path> contenuto = Files.walk(temporanea)) {
                contenuto.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    /**
     * Impronta della copia appena fatta, dopo aver controllato che sia <b>uguale al file originale</b>: l'originale
     * (chiuso, nessuno ci scrive) si legge per intero e se la sua impronta differisce da quella della copia, la copia
     * viene <b>cancellata</b> e si lancia {@link IOException}, così non si compatta e non si userà mai come ripristino.
     *
     * <p>Serve perché {@link #confermaCompattazione} si fida della copia: il solo controllo della dimensione fatto da
     * {@link #creaCopiaPrecompattazione} non vede un valore cambiato in silenzio durante la copia, e una copia così
     * sostituirebbe un file compattato corretto. Il riferimento non si prende dal database aperto (un thread che
     * scrive in quel momento darebbe un falso allarme) ma dal file chiuso.
     *
     * @return l'impronta della copia, da passare a {@link #confermaCompattazione}
     * @throws IOException se la copia non coincide con l'originale
     */
    static String improntaCopiaVerificata(String cartella, String file) throws IOException, SQLException {
        String improntaOriginale = improntaDaUrl("jdbc:h2:" + cartella + file);
        String improntaCopia = improntaDellaCopia(cartella, file);
        if (!improntaOriginale.equals(improntaCopia)) {
            Files.deleteIfExists(percorsoCopia(cartella, file));
            throw new IOException("la copia di sicurezza di " + file + ".mv.db non coincide con l'originale: scartata, non si compatta");
        }
        return improntaCopia;
    }

    /**
     * Controlla che il file appena compattato abbia lo stesso contenuto della copia di sicurezza. Se l'impronta
     * non coincide, o il file non si apre o non si legge, <b>rimette la copia</b> al suo posto: la copia è stata
     * letta per intero e coincide con l'originale ({@link #improntaCopiaVerificata}), quindi è lo stato valido di prima.
     *
     * @param improntaCopia il risultato di {@link #improntaCopiaVerificata}
     * @return {@code true} se la compattazione è confermata, {@code false} se si è tornati alla copia
     */
    static boolean confermaCompattazione(String cartella, String file, String improntaCopia) throws IOException {
        boolean uguale;
        try {
            uguale = improntaCopia.equals(improntaDaUrl("jdbc:h2:" + cartella + file));
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            uguale = false;
        }
        if (!uguale) {
            Files.copy(percorsoCopia(cartella, file), Paths.get(cartella + file + ".mv.db"), StandardCopyOption.REPLACE_EXISTING);
        }
        return uguale;
    }

    private static LocalDateTime dataCopia(Path copia) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Files.getLastModifiedTime(copia).toMillis()), ZoneId.systemDefault());
        } catch (IOException ex) {
            return null;
        }
    }

    // ---- testi ----

    /** Cosa contiene il file e cosa succede ricreandolo vuoto: l'effetto, senza dire cosa fare dei risultati. */
    static String cosaContiene(String file) {
        switch (file) {
            case FILE_PRINCIPALE:
                return "Contiene le marcature SCAM dei token, le preferenze dei provider DeFi,\n"
                        + "le chiavi API degli explorer, alcune opzioni e la cache dei token.\n"
                        + "Un file nuovo parte vuoto: questi dati vanno ripristinati da un backup\n"
                        + "del programma (Opzioni, Backup), finché non lo sono i risultati dei calcoli possono cambiare.";
            case FILE_PERSONALE:
                return "Contiene i wallet, i gruppi wallet, i prezzi inseriti a mano, i periodi\n"
                        + "di detenzione del quadro RW, i documenti di origine, lo storico delle modifiche\n"
                        + "e le opzioni di calcolo. Un file nuovo parte vuoto: questi dati vanno\n"
                        + "ripristinati da un backup del programma (Opzioni, Backup).";
            default:
                return "Contiene la cache dei prezzi scaricati, compresi quelli di fine anno usati dai quadri,\n"
                        + "e l'elenco dei prezzi non recuperabili. Un file nuovo parte vuoto: i prezzi vengono\n"
                        + "riscaricati quando servono, quelli di fine anno si possono ripristinare da un backup\n"
                        + "del programma (Opzioni, Backup).";
        }
    }

    /** Testo del dialogo per il file indicato, senza copia di sicurezza. */
    static String testoAvviso(String file) {
        return testoAvviso(file, null);
    }

    /**
     * Testo del dialogo per il file indicato.
     *
     * @param copiaDel quando è stata fatta la copia di sicurezza, o {@code null} se non ce n'è una da offrire
     */
    static String testoAvviso(String file, LocalDateTime copiaDel) {
        String testo = "Il file " + file + ".mv.db è danneggiato e il programma non riesce ad aprirlo.\n"
                + "Cartella: " + VarStatiche.getWorkingDirectory() + "\n\n"
                + cosaContiene(file) + "\n\n";
        if (copiaDel != null) {
            testo += "Esiste una copia di sicurezza fatta prima dell'ultima compattazione, il "
                    + copiaDel.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm")) + ".\n"
                    + "Ripristinandola il file torna com'era prima di quella compattazione.\n\n";
        }
        testo += "Il file danneggiato non viene cancellato: viene rinominato e resta nella stessa\n"
                + "cartella (" + file + ".mv.db.danneggiato-...), così si può ancora analizzare o recuperare.\n\n"
                + (copiaDel != null ? "Cosa si vuole fare?" : "Spostarlo e continuare con un file nuovo?");
        return testo;
    }

    /** Dialogo vero: a questo punto dell'avvio il Look&Feel non è ancora installato, quindi JOptionPane semplice. */
    private static final class SceltaSwing implements Scelta {

        @Override
        public Azione scegli(String file, String testo, boolean conCopia) {
            String[] pulsanti = conCopia
                    ? new String[]{"Ripristina la copia", "Nuovo file vuoto", "Chiudi il programma"}
                    : new String[]{"Sposta e continua", "Chiudi il programma"};
            int scelta = JOptionPane.showOptionDialog(
                    null,
                    testo,
                    "Database danneggiato",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    pulsanti,
                    pulsanti[pulsanti.length - 1]
            );
            if (conCopia) {
                return scelta == 0 ? Azione.RIPRISTINA : scelta == 1 ? Azione.NUOVO : Azione.CHIUDI;
            }
            return scelta == 0 ? Azione.NUOVO : Azione.CHIUDI;
        }

        @Override
        public void errore(String testo) {
            JOptionPane.showMessageDialog(null, testo, "Attenzione", JOptionPane.ERROR_MESSAGE);
        }
    }
}
