package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Backup_Restore} : il giro completo backup → cancellazione → ripristino, più le tre
 * parti che si possono rompere in silenzio.
 *
 * <p>Gira su un database H2 e una working directory temporanei, come {@link DocumentiFonteTest}, quindi
 * non tocca i dati dell'utente.
 *
 * <p>Cosa difendono questi test, e perché ognuno è costato un ragionamento :
 * <ul>
 *   <li><b>il giro completo</b> — un backup che si scrive ma non si rilegge è peggio di nessun backup,
 *       perché uno lo scopre solo quando serve;</li>
 *   <li><b>{@code NULL} e stringa vuota restano distinti nel CSV</b> : su una colonna di chiave primaria
 *       la differenza cambia i dati, e un parser CSV ingenuo le confonde;</li>
 *   <li><b>l'estrazione dei prezzi copre gli istanti giusti</b> : è il test che si romperà il giorno in
 *       cui un motore fiscale introdurrà una lettura prezzo a un istante nuovo, che è esattamente il
 *       modo in cui questa funzione può smettere di garantire la ristampa identica senza che nessuno se
 *       ne accorga;</li>
 *   <li><b>l'impronta delle plusvalenze ignora {@code [38]} e {@code [31]}</b> : quei due campi sono
 *       portati e non ricalcolati, quindi includerli renderebbe la verifica post-ripristino un
 *       generatore di falsi allarmi.</li>
 * </ul>
 */
class Backup_RestoreTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
        new File(VarStatiche.getCartella_ArchivioBackup()).mkdirs();
        new File(VarStatiche.getCartella_DocumentiFonte()).mkdirs();
        new File(VarStatiche.getCartella_ConfigImport()).mkdirs();
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void puliscePartenza() throws Exception {
        Principale.MappaCryptoWallet.clear();
        for (File f : new File(VarStatiche.getCartella_ArchivioBackup()).listFiles()) {
            f.delete();
        }
        esegui(DatabaseH2.connectionPersonale, "DELETE FROM WALLETGRUPPO");
        esegui(DatabaseH2.connectionPersonale, "DELETE FROM EMONEY");
        esegui(DatabaseH2.connection, "DELETE FROM RINOMINATOKEN");
        esegui(DatabaseH2.connectionPrezzi, "DELETE FROM PrezziNew");
    }

    // =================================================================================================
    // GIRO COMPLETO
    // =================================================================================================
    @Test
    void backupERipristinoRimettonoTuttoAPosto() throws Exception {
        //--- dati di partenza
        aggiungi("20240115103000_Binance_1_1_AC", "2024-01-15 10:30",
                "Binance", "12.50");
        aggiungi("20240620141500_Kraken_1_1_RW", "2024-06-20 14:15",
                "Kraken", "0.00");
        Importazioni.Scrivi_Movimenti_Crypto(Principale.MappaCryptoWallet, false);

        esegui(DatabaseH2.connectionPersonale,
                "INSERT INTO WALLETGRUPPO (Wallet, Gruppo) VALUES ('Binance', 'Wallet 01')");
        esegui(DatabaseH2.connectionPersonale,
                "INSERT INTO EMONEY (Moneta, Data) VALUES ('USDC', '2023-06-30')");
        esegui(DatabaseH2.connection,
                "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome, NuovoNome) "
                + "VALUES ('0xabc_eth', 'PIPPO', 'PIPPO **')");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        //Un prezzo esattamente al confine d'anno: deve finire anche nel backup "essenziale"
        inserisciPrezzo(FunzioniDate.ConvertiDatainLongMinuto("2024-12-31 23:59"), "binance", "BTC", "82000");

        //--- backup
        Backup_Restore.OpzioniBackup opz = new Backup_Restore.OpzioniBackup();
        File zip = Backup_Restore.Esegui(opz, null);
        assertNotNull(zip, "il backup dovrebbe essere stato creato");
        assertTrue(zip.length() > 0, "l'archivio non dovrebbe essere vuoto");

        Backup_Restore.Manifest m = Backup_Restore.LeggiManifest(zip);
        assertNotNull(m, "il manifest dovrebbe essere leggibile");
        assertEquals(2, m.MovimentiTotali);
        assertEquals("2024-01-15 10:30", m.PrimoMovimento);
        assertEquals("2024-06-20 14:15", m.UltimoMovimento);
        assertEquals(Integer.valueOf(2), m.MovimentiPerAnno.get("2024"));
        assertEquals(Integer.valueOf(1), m.MovimentiPerWallet.get("Binance"));
        assertEquals(Importazioni.ColonneTabella, m.ColonneTabella);
        assertEquals("SI", m.Impostazioni.get("PlusXWallet"));
        assertFalse(m.ImprontaPlusvalenze.isEmpty(), "l'impronta dovrebbe essere calcolata");

        //--- si cancella tutto, come farebbe un'installazione nuova
        esegui(DatabaseH2.connectionPersonale, "DELETE FROM WALLETGRUPPO");
        esegui(DatabaseH2.connectionPersonale, "DELETE FROM EMONEY");
        esegui(DatabaseH2.connection, "DELETE FROM RINOMINATOKEN");
        esegui(DatabaseH2.connectionPrezzi, "DELETE FROM PrezziNew");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        new File(VarStatiche.getFile_CryptoWallet()).delete();

        //--- ripristino
        Set<Backup_Restore.Gruppo> tutti = EnumSet.allOf(Backup_Restore.Gruppo.class);
        Backup_Restore.EsitoRipristino esito = Backup_Restore.Ripristina(zip, tutti, null);
        assertTrue(esito.Riuscito, "il ripristino dovrebbe riuscire : " + esito.Avvisi);
        assertTrue(esito.MovimentiRipristinati, "il file dei movimenti dovrebbe essere stato riscritto");

        assertEquals("Wallet 01", unaStringa(DatabaseH2.connectionPersonale,
                "SELECT Gruppo FROM WALLETGRUPPO WHERE Wallet='Binance'"));
        assertEquals("2023-06-30", unaStringa(DatabaseH2.connectionPersonale,
                "SELECT Data FROM EMONEY WHERE Moneta='USDC'"));
        assertEquals("PIPPO **", unaStringa(DatabaseH2.connection,
                "SELECT NuovoNome FROM RINOMINATOKEN WHERE address_chain='0xabc_eth'"));
        assertEquals("SI", DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet"),
                "l'opzione di calcolo dovrebbe essere tornata quella del backup");
        assertEquals("82000.0", unaStringa(DatabaseH2.connectionPrezzi,
                "SELECT prezzo FROM PrezziNew WHERE symbol='BTC'"),
                "il prezzo di fine anno dovrebbe essere stato ripristinato");

        assertTrue(new File(VarStatiche.getFile_CryptoWallet()).isFile(),
                "il file dei movimenti dovrebbe essere stato riscritto");
        assertEquals(2, Files.readAllLines(new File(VarStatiche.getFile_CryptoWallet()).toPath()).size());
        assertEquals(m.ImprontaPlusvalenze, esito.ImprontaAttesa,
                "l'esito dovrebbe riportare l'impronta del backup, per la verifica post-ripristino");
        assertTrue(esito.VerificaMancanti.isEmpty(),
                "ripristinando tutto, la verifica delle plusvalenze dovrebbe essere possibile");
    }

    /**
     * Su un ripristino parziale il confronto delle impronte non va fatto : fallirebbe sempre e comunque,
     * perché il motore gira sugli ingressi di questa installazione e non su quelli del backup. Un
     * controllo che non può passare non è un controllo, e l'avviso rosso che ne segue insegna all'utente
     * a ignorarlo proprio quando invece conta.
     */
    @Test
    void laVerificaDellePlusvalenzeNonSiFaSuUnRipristinoParziale() {
        Set<Backup_Restore.Gruppo> tutti = EnumSet.allOf(Backup_Restore.Gruppo.class);

        assertTrue(Backup_Restore.GruppiMancantiPerVerifica(tutti, true).isEmpty(),
                "con tutti i gruppi e i movimenti riscritti la verifica è possibile");

        //Senza movimenti l'impronta descrive un altro insieme di righe : non è confrontabile nemmeno in
        //linea di principio. Il gate è sul flag e non sul gruppo, perché un archivio senza la voce dei
        //movimenti la salta in silenzio pur avendo il gruppo selezionato
        assertEquals(Set.of(Backup_Restore.Gruppo.MOVIMENTI),
                Backup_Restore.GruppiMancantiPerVerifica(tutti, false),
                "senza movimenti riscritti la verifica non è possibile, anche col gruppo selezionato");

        //Le impostazioni di calcolo sono ingressi del motore quanto i movimenti
        Set<Backup_Restore.Gruppo> senzaOpzioni = EnumSet.copyOf(tutti);
        senzaOpzioni.remove(Backup_Restore.Gruppo.OPZIONI_CALCOLO);
        assertEquals(Set.of(Backup_Restore.Gruppo.OPZIONI_CALCOLO),
                Backup_Restore.GruppiMancantiPerVerifica(senzaOpzioni, true));

        Set<Backup_Restore.Gruppo> senzaWallet = EnumSet.copyOf(tutti);
        senzaWallet.remove(Backup_Restore.Gruppo.GRUPPI_WALLET);
        assertEquals(Set.of(Backup_Restore.Gruppo.GRUPPI_WALLET),
                Backup_Restore.GruppiMancantiPerVerifica(senzaWallet, true));

        Set<Backup_Restore.Gruppo> senzaPrezziPers = EnumSet.copyOf(tutti);
        senzaPrezziPers.remove(Backup_Restore.Gruppo.PREZZI_PERSONALIZZATI);
        assertEquals(Set.of(Backup_Restore.Gruppo.PREZZI_PERSONALIZZATI),
                Backup_Restore.GruppiMancantiPerVerifica(senzaPrezziPers, true));

        //La cache dei prezzi di mercato invece NON blocca la verifica : un prezzo risolto diversamente è
        //esattamente il fallimento invisibile per cui l'impronta esiste
        Set<Backup_Restore.Gruppo> senzaPrezzi = EnumSet.copyOf(tutti);
        senzaPrezzi.remove(Backup_Restore.Gruppo.PREZZI);
        assertTrue(Backup_Restore.GruppiMancantiPerVerifica(senzaPrezzi, true).isEmpty(),
                "i prezzi di mercato sono cache riproducibile : la verifica resta sensata");

        //Nemmeno le chiavi API, le cache token, i dati Crypto.com e le marcature SCAM : non entrano nei
        //campi calcolati dal motore delle plusvalenze
        Set<Backup_Restore.Gruppo> minimo = EnumSet.of(Backup_Restore.Gruppo.MOVIMENTI,
                Backup_Restore.Gruppo.OPZIONI_CALCOLO, Backup_Restore.Gruppo.GRUPPI_WALLET,
                Backup_Restore.Gruppo.PREZZI_PERSONALIZZATI);
        assertTrue(Backup_Restore.GruppiMancantiPerVerifica(minimo, true).isEmpty(),
                "i quattro gruppi che entrano nel calcolo bastano");
    }

    /**
     * I tre gruppi richiesti dalla verifica devono essere <b>sempre selezionabili</b>.
     *
     * <p>{@code ChiediGruppi} disabilita le caselle dei gruppi che l'archivio non contiene, e solo le
     * caselle abilitate e spuntate arrivano al ripristino. Se un giorno {@code GruppiPresenti()}
     * derivasse la presenza dal contenuto — "nessun prezzo personalizzato inserito, quindi gruppo
     * assente" — quell'utente non potrebbe più selezionarlo, e la verifica delle plusvalenze
     * risulterebbe impossibile <b>per sempre</b> senza che nulla lo segnali. Il manifest qui è vuoto
     * apposta : è il caso peggiore.
     */
    @Test
    void iGruppiRichiestiDallaVerificaSonoSempreSelezionabili() {
        Backup_Restore.Manifest vuoto = new Backup_Restore.Manifest();
        Set<Backup_Restore.Gruppo> presenti = vuoto.GruppiPresenti();
        for (Backup_Restore.Gruppo g : Backup_Restore.GruppiMancantiPerVerifica(
                EnumSet.noneOf(Backup_Restore.Gruppo.class), false)) {
            if (g == Backup_Restore.Gruppo.MOVIMENTI) {
                //I movimenti sono l'eccezione voluta : un archivio senza movimenti non ha nulla da
                //verificare, e lì saltare il confronto è la risposta giusta
                continue;
            }
            assertTrue(presenti.contains(g),
                    "il gruppo " + g + " serve alla verifica e deve restare sempre selezionabile");
        }
    }

    @Test
    void ilBackupCompletoDeiPrezziAttraversaTuttaLaTabella() throws Exception {
        //Sull'archivio reale questo CSV è di 15 milioni di righe e 779 MB: né l'esportazione né il
        //ripristino possono tenerlo in memoria. Qui le righe sono molte meno, ma il percorso è lo
        //stesso — e questo test fallirebbe (per tempo o per memoria) se qualcuno rimettesse un buffer.
        final int RIGHE = 60000;
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "INSERT INTO PrezziNew (timestamp, exchange, symbol, rete, address, prezzo) VALUES (?,?,?,'','',?)")) {
            DatabaseH2.connectionPrezzi.setAutoCommit(false);
            for (int i = 0; i < RIGHE; i++) {
                //Timestamp sparsi su vent'anni: fuori da qualsiasi finestra "necessaria", così la
                //differenza fra i due livelli è netta
                ps.setLong(1, 1000000000000L + (long) i * 10_000_000L);
                ps.setString(2, "exch" + (i % 7));
                ps.setString(3, "TK" + (i % 50));
                ps.setString(4, String.valueOf(i / 100.0));
                ps.addBatch();
                if (i % 5000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            DatabaseH2.connectionPrezzi.commit();
            DatabaseH2.connectionPrezzi.setAutoCommit(true);
        }

        Backup_Restore.OpzioniBackup opz = new Backup_Restore.OpzioniBackup();
        opz.PrezziCompleti = true;
        File zip = Backup_Restore.Esegui(opz, null);
        assertNotNull(zip);
        Backup_Restore.Manifest m = Backup_Restore.LeggiManifest(zip);
        assertEquals("completi", m.LivelloPrezzi);
        assertEquals(RIGHE, m.RighePrezzi, "il backup completo deve contenere tutte le righe");

        esegui(DatabaseH2.connectionPrezzi, "DELETE FROM PrezziNew");
        Backup_Restore.EsitoRipristino esito = Backup_Restore.Ripristina(zip,
                EnumSet.of(Backup_Restore.Gruppo.PREZZI), null);
        assertTrue(esito.Riuscito, "" + esito.Avvisi);
        assertEquals(String.valueOf(RIGHE), unaStringa(DatabaseH2.connectionPrezzi,
                "SELECT COUNT(*) FROM PrezziNew"), "tutte le righe devono tornare");
    }

    @Test
    void ilRipristinoSuUnaInstallazioneVuotaFunziona() throws Exception {
        //Il caso d'uso dichiarato è proprio questo: ripristinare su una macchina nuova, dove le tabelle
        //esistono ma sono vuote e il file dei movimenti non c'è
        aggiungi("20240115103000_Binance_1_1_AC", "2024-01-15 10:30", "Binance", "12.50");
        Importazioni.Scrivi_Movimenti_Crypto(Principale.MappaCryptoWallet, false);
        esegui(DatabaseH2.connectionPersonale,
                "INSERT INTO WALLETGRUPPO (Wallet, Gruppo) VALUES ('Binance', 'Wallet 03')");
        File zip = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), null);
        assertNotNull(zip);

        //Installazione "nuova": tabelle vuote, nessun file dei movimenti, nessuna cartella dei documenti
        esegui(DatabaseH2.connectionPersonale, "DELETE FROM WALLETGRUPPO");
        new File(VarStatiche.getFile_CryptoWallet()).delete();
        Principale.MappaCryptoWallet.clear();

        Backup_Restore.EsitoRipristino esito = Backup_Restore.Ripristina(zip,
                EnumSet.allOf(Backup_Restore.Gruppo.class), null);
        assertTrue(esito.Riuscito, "" + esito.Avvisi);
        assertEquals("Wallet 03", unaStringa(DatabaseH2.connectionPersonale,
                "SELECT Gruppo FROM WALLETGRUPPO WHERE Wallet='Binance'"));
        assertTrue(new File(VarStatiche.getFile_CryptoWallet()).isFile());
        assertEquals(1, Files.readAllLines(new File(VarStatiche.getFile_CryptoWallet()).toPath()).size());
    }

    @Test
    void leChiaviApiRestanoFuoriSeNonRichieste() throws Exception {
        esegui(DatabaseH2.connectionPersonale,
                "MERGE INTO EXCHANGEAPI (Nome, Exchange, Chiave, Segreto, Opzionale) "
                + "VALUES ('prova', 'binance', 'CHIAVE-SEGRETISSIMA', 'SEGRETO', '')");
        DatabaseH2.Opzioni_Scrivi("ApiKey_Etherscan", "ABC123");

        File senza = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), null);
        assertNotNull(senza);
        assertFalse(contenuto(senza).contains("CHIAVE-SEGRETISSIMA"),
                "le chiavi API non devono finire in un backup che non le ha richieste");
        assertFalse(contenuto(senza).contains("ABC123"),
                "nemmeno le ApiKey_* delle opzioni devono finire nell'archivio");

        Backup_Restore.OpzioniBackup con = new Backup_Restore.OpzioniBackup();
        con.ChiaviApi = true;
        File conChiavi = Backup_Restore.Esegui(con, null);
        assertNotNull(conChiavi);
        assertTrue(contenuto(conChiavi).contains("CHIAVE-SEGRETISSIMA"),
                "richiedendole esplicitamente le chiavi devono esserci");
    }

    @Test
    void ilManifestEsternoEvitaDiApriteLoZip() {
        aggiungi("20240115103000_Binance_1_1_AC", "2024-01-15 10:30",
                "Binance", "12.50");
        File zip = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), null);
        assertNotNull(zip);
        String base = zip.getName().substring(0, zip.getName().lastIndexOf('.'));
        assertTrue(new File(zip.getParentFile(), base + ".json").isFile(),
                "accanto all'archivio deve esserci il manifest, così l'elenco non apre gli zip");

        List<Backup_Restore.Manifest> elenco = Backup_Restore.Elenco();
        assertEquals(1, elenco.size());
        assertEquals(1, elenco.get(0).MovimentiTotali);
        assertTrue(elenco.get(0).DimensioneCompressa > 0);
    }

    @Test
    void eliminaToglieAncheIlManifestAffiancato() {
        File zip = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), null);
        assertNotNull(zip);
        String base = zip.getName().substring(0, zip.getName().lastIndexOf('.'));
        File json = new File(zip.getParentFile(), base + ".json");
        assertTrue(json.isFile());

        assertTrue(Backup_Restore.Elimina(zip));
        assertFalse(zip.exists());
        assertFalse(json.exists(), "un manifest orfano ricomparirebbe come backup fantasma nell'elenco");
    }

    // =================================================================================================
    // PREZZI NECESSARI
    // =================================================================================================
    @Test
    void gliIntervalliCopronoConfiniDAnnoEIRewardAzzerati() {
        //Un reward con controvalore azzerato: è l'unico caso che fa chiedere un prezzo alla data del
        //movimento (Calcoli_RW:1297)
        aggiungi("20240620141500_Kraken_1_1_RW", "2024-06-20 14:15",
                "Kraken", "0.00");
        //Un reward già prezzato e un acquisto: nessuno dei due fa chiedere prezzi a quella data
        aggiungi("20240701090000_Kraken_1_1_RW", "2024-07-01 09:00",
                "Kraken", "31.20");
        aggiungi("20240815120000_Binance_1_1_AC", "2024-08-15 12:00",
                "Binance", "50.00");

        List<long[]> iv = Backup_Restore.IntervalliPrezziNecessari();
        assertTrue(coperto(iv, FunzioniDate.ConvertiDatainLongMinuto("2024-12-31 23:59")),
                "la fine dell'anno deve essere coperta: è dove si valorizzano le giacenze RW");
        assertTrue(coperto(iv, FunzioniDate.ConvertiDatainLongMinuto("2024-01-01 00:00")),
                "l'inizio dell'anno deve essere coperto");
        assertTrue(coperto(iv, FunzioniDate.ConvertiDatainLongMinuto("2025-01-01 00:00")),
                "l'anno successivo all'ultimo movimento serve: la chiusura di N è l'apertura di N+1");
        assertTrue(coperto(iv, FunzioniDate.ConvertiDatainLongMinuto("2024-06-20 14:15")),
                "il reward azzerato deve essere coperto");
        assertFalse(coperto(iv, FunzioniDate.ConvertiDatainLongMinuto("2024-08-15 12:00")),
                "un acquisto normale non fa chiedere prezzi: includerlo gonfierebbe l'archivio per nulla");
    }

    @Test
    void gliIntervalliSonoFusiEOrdinati() {
        aggiungi("20240620141500_Kraken_1_1_RW", "2024-06-20 14:15",
                "Kraken", "0.00");
        //A dieci minuti di distanza: le due finestre da un'ora si sovrappongono e devono fondersi, o le
        //righe verrebbero estratte due volte
        aggiungi("20240620142500_Kraken_1_1_RW", "2024-06-20 14:25",
                "Kraken", "0.00");

        List<long[]> iv = Backup_Restore.IntervalliPrezziNecessari();
        for (int i = 1; i < iv.size(); i++) {
            assertTrue(iv.get(i)[0] > iv.get(i - 1)[1],
                    "gli intervalli devono essere disgiunti e crescenti, altrimenti i prezzi si duplicano");
        }
        long a = FunzioniDate.ConvertiDatainLongMinuto("2024-06-20 14:15");
        long b = FunzioniDate.ConvertiDatainLongMinuto("2024-06-20 14:25");
        int quanti = 0;
        for (long[] x : iv) {
            if (a >= x[0] && a <= x[1]) {
                quanti++;
            }
        }
        assertEquals(1, quanti, "un istante deve stare in un solo intervallo");
        assertTrue(coperto(iv, b));
    }

    // =================================================================================================
    // IMPRONTA DELLE PLUSVALENZE
    // =================================================================================================
    @Test
    void limprontaCambiaSeCambiaUnCampoCalcolato() {
        String[] v = movimento("20240115103000_Binance_1_1_AC", "2024-01-15 10:30", "Binance", "12.50");
        Principale.MappaCryptoWallet.put(v[0], v);
        String prima = Backup_Restore.ImprontaPlusvalenze();

        v[19] = "999.99";  //la plusvalenza
        assertNotEquals(prima, Backup_Restore.ImprontaPlusvalenze(),
                "cambiare una plusvalenza deve cambiare l'impronta, altrimenti la verifica non verifica nulla");
    }

    @Test
    void limprontaIgnoraICampiPortati() {
        String[] v = movimento("20240115103000_Binance_1_1_AC", "2024-01-15 10:30", "Binance", "12.50");
        Principale.MappaCryptoWallet.put(v[0], v);
        String prima = Backup_Restore.ImprontaPlusvalenze();

        //[38] e [31] sono portati, non ricalcolati: se entrassero nell'impronta ogni ripristino
        //segnalerebbe una divergenza inesistente
        v[38] = "A";
        v[31] = "2024-01-15 10:30";
        assertEquals(prima, Backup_Restore.ImprontaPlusvalenze(),
                "i campi portati non devono entrare nell'impronta");
    }

    @Test
    void unMovimentoConUnCalcoloDiversoFaFallireLaVerifica() throws Exception {
        //È il test che dà valore alla verifica post-ripristino: se non si riesce a farla fallire, il
        //fatto che passi non dimostra nulla. Simula il caso reale — un prezzo risolto diversamente su
        //un'installazione nuova, che cambia la plusvalenza di un movimento.
        aggiungi("20240115103000_Binance_1_1_AC", "2024-01-15 10:30", "Binance", "12.50");
        aggiungi("20240620141500_Kraken_1_1_RW", "2024-06-20 14:15", "Kraken", "0.00");
        Importazioni.Scrivi_Movimenti_Crypto(Principale.MappaCryptoWallet, false);

        File zip = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), null);
        assertNotNull(zip);
        String improntaDelBackup = Backup_Restore.LeggiManifest(zip).ImprontaPlusvalenze;
        assertEquals(improntaDelBackup, Backup_Restore.ImprontaPlusvalenze(),
                "prima di toccare nulla le due impronte devono coincidere");

        //Un ricalcolo su un'altra installazione produce una plusvalenza diversa per un movimento
        Principale.MappaCryptoWallet.get("20240115103000_Binance_1_1_AC")[19] = "7.77";

        assertNotEquals(improntaDelBackup, Backup_Restore.ImprontaPlusvalenze(),
                "una plusvalenza diversa DEVE far divergere l'impronta, altrimenti la verifica è cieca");
    }

    // =================================================================================================
    // CSV
    // =================================================================================================
    @Test
    void ilCsvSiLeggeSenzaTenerloTuttoInMemoria() throws Exception {
        //Con l'opzione "tutti i prezzi" questo CSV arriva a 15 milioni di righe: il parser deve
        //consegnarle una alla volta, non costruire la lista completa
        StringBuilder sb = new StringBuilder();
        sb.append(Backup_Restore.RigaCsv(new String[]{"A", "B"}));
        for (int i = 0; i < 20000; i++) {
            sb.append(Backup_Restore.RigaCsv(new String[]{"riga" + i, i % 3 == 0 ? null : "v" + i}));
        }
        int[] viste = {0};
        String[] ultima = new String[1];
        Backup_Restore.LeggiCsvStream(new java.io.StringReader(sb.toString()), r -> {
            viste[0]++;
            ultima[0] = r[0];
        });
        assertEquals(20001, viste[0], "intestazione più 20000 righe");
        assertEquals("riga19999", ultima[0]);
    }

    @Test
    void ilCsvReggeUnCampoQuotatoSeguitoDaAltriCampi() throws Exception {
        //Il parser in streaming non può guardare il carattere successivo: distingue la virgoletta di
        //chiusura da quella raddoppiata rimandando la decisione al giro dopo. Questo è il caso che
        //quella logica deve reggere.
        String csv = Backup_Restore.RigaCsv(new String[]{"con\"dentro", "dopo", null, ""});
        List<String[]> letto = Backup_Restore.LeggiCsv(csv);
        assertEquals(1, letto.size());
        assertEquals("con\"dentro", letto.get(0)[0]);
        assertEquals("dopo", letto.get(0)[1]);
        assertNull(letto.get(0)[2]);
        assertEquals("", letto.get(0)[3]);
    }

    @Test
    void lElencoEOrdinatoPerDataNonPerNomeFile() throws Exception {
        //I backup automatici si chiamano "backup_auto_...": in ordine alfabetico finirebbero sopra
        //tutti i manuali qualunque sia la loro data
        File auto = new File(VarStatiche.getCartella_ArchivioBackup(), "backup_auto_20200101_000000.zip");
        File manuale = new File(VarStatiche.getCartella_ArchivioBackup(), "backup_20260101_000000.zip");
        Files.writeString(new File(VarStatiche.getCartella_ArchivioBackup(),
                "backup_auto_20200101_000000.json").toPath(),
                "{\"formatoBackup\":1,\"creato\":\"2020-01-01 00:00:00\"}");
        Files.writeString(new File(VarStatiche.getCartella_ArchivioBackup(),
                "backup_20260101_000000.json").toPath(),
                "{\"formatoBackup\":1,\"creato\":\"2026-01-01 00:00:00\"}");
        Files.write(auto.toPath(), new byte[]{1});
        Files.write(manuale.toPath(), new byte[]{1});

        List<Backup_Restore.Manifest> elenco = Backup_Restore.Elenco();
        assertEquals(2, elenco.size());
        assertEquals("2026-01-01 00:00:00", elenco.get(0).Creato,
                "in cima deve stare il più recente, non quello col nome alfabeticamente maggiore");
    }

    @Test
    void ilCsvDistingueNullDaStringaVuota() {
        String csv = Backup_Restore.RigaCsv(new String[]{"a", "", null, "con,virgola", "con\"virgolette"});
        List<String[]> letto = Backup_Restore.LeggiCsv(csv);
        assertEquals(1, letto.size());
        String[] r = letto.get(0);
        assertEquals("a", r[0]);
        assertEquals("", r[1], "la stringa vuota deve restare una stringa vuota");
        assertNull(r[2], "il NULL deve restare NULL : su una chiave primaria la differenza cambia i dati");
        assertEquals("con,virgola", r[3]);
        assertEquals("con\"virgolette", r[4]);
    }

    @Test
    void ilCsvReggeIlRitornoACapoDentroUnCampo() {
        String csv = Backup_Restore.RigaCsv(new String[]{"prima\nseconda", "x"})
                + Backup_Restore.RigaCsv(new String[]{"y", "z"});
        List<String[]> letto = Backup_Restore.LeggiCsv(csv);
        assertEquals(2, letto.size(), "un a capo dentro le virgolette non deve spezzare la riga");
        assertEquals("prima\nseconda", letto.get(0)[0]);
        assertEquals("z", letto.get(1)[1]);
    }

    // =================================================================================================
    // UTILITÀ
    // =================================================================================================
    /** Movimento minimo ma con la forma giusta: 45 campi, categoria nell'ID, controvalore in [15]. */
    private static String[] movimento(String Id, String Data, String Wallet, String Controvalore) {
        String[] v = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < v.length; i++) {
            v[i] = "";
        }
        v[0] = Id;
        v[1] = Data;
        v[3] = Wallet;
        v[15] = Controvalore;
        v[16] = "10.00";
        v[17] = "11.00";
        v[19] = "1.00";
        v[33] = "S";
        return v;
    }

    /** Mette in mappa un movimento appena costruito, con la propria chiave. */
    private static String[] aggiungi(String Id, String Data, String Wallet, String Controvalore) {
        String[] v = movimento(Id, Data, Wallet, Controvalore);
        Principale.MappaCryptoWallet.put(v[0], v);
        return v;
    }

    private static boolean coperto(List<long[]> Intervalli, long Istante) {
        for (long[] i : Intervalli) {
            if (Istante >= i[0] && Istante <= i[1]) {
                return true;
            }
        }
        return false;
    }

    private static void esegui(java.sql.Connection c, String Sql) throws Exception {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(Sql);
        }
    }

    private static void inserisciPrezzo(long Timestamp, String Exchange, String Symbol, String Prezzo)
            throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "MERGE INTO PrezziNew (timestamp, exchange, symbol, rete, address, prezzo) "
                + "VALUES (?,?,?,'','',?)")) {
            ps.setLong(1, Timestamp);
            ps.setString(2, Exchange);
            ps.setString(3, Symbol);
            ps.setString(4, Prezzo);
            ps.executeUpdate();
        }
    }

    private static String unaStringa(java.sql.Connection c, String Sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(Sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static String contenuto(File Zip) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(Zip)) {
            for (java.util.zip.ZipEntry e : java.util.Collections.list(z.entries())) {
                if (e.isDirectory()) {
                    continue;
                }
                try (java.io.InputStream in = z.getInputStream(e)) {
                    sb.append(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        return sb.toString();
    }
}
