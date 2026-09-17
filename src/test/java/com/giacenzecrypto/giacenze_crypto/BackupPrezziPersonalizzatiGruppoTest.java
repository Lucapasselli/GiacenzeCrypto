package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Revisione di backup/ripristino dopo la separazione del gruppo dalla fonte nei prezzi personalizzati
 * (2026-09-17): `personale.mv.db`/`PrezziNew` ha una colonna `gruppo` in più e una chiave primaria a
 * sei colonne.
 *
 * <p>Copre i due casi che contano e che nessun test toccava:
 * <ul>
 *   <li><b>archivio nuovo</b>: una riga con fonte e gruppo separati deve sopravvivere al giro completo
 *       senza che i due valori si rimescolino;</li>
 *   <li><b>archivio vecchio</b>: un CSV prodotto prima della migrazione non ha la colonna `gruppo`, e
 *       al ripristino le righe devono prendere il {@code DEFAULT 'TUTTI'} — che è esattamente il
 *       valore che avevano quando il gruppo stava incollato dentro `exchange`.</li>
 * </ul>
 *
 * <p>Il secondo caso è quello che rende la migrazione sicura per chi aggiorna il programma con un
 * backup vecchio in mano, ed era stato affermato leggendo il codice ({@code ImportaTabella} associa
 * le colonne per nome) senza mai provarlo.
 */
class BackupPrezziPersonalizzatiGruppoTest {

    @TempDir
    static Path tempDir;

    private static final long ISTANTE = 1767222000000L;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static void esegui(String sql) throws Exception {
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute(sql);
        }
    }

    private static String[] leggiRiga(String simbolo) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT exchange, gruppo, prezzo FROM PrezziNew WHERE symbol = ?")) {
            ps.setString(1, simbolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new String[]{rs.getString(1), rs.getString(2), rs.getString(3)};
            }
        }
    }

    @Test
    void laColonnaGruppoEsisteEDEDichiarataNellaChiave() throws Exception {
        //Se queste due cose non valgono, tutto il resto della revisione non ha senso.
        try (Statement st = DatabaseH2.connectionPersonale.createStatement();
             ResultSet rs = st.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                     + "WHERE TABLE_NAME = 'PREZZINEW' ORDER BY ORDINAL_POSITION")) {
            StringBuilder chiave = new StringBuilder();
            while (rs.next()) chiave.append(rs.getString(1)).append(" ");
            assertEquals("TIMESTAMP EXCHANGE SYMBOL RETE ADDRESS GRUPPO ", chiave.toString());
        }
    }

    @Test
    void fonteEGruppoRestanoSeparatiDopoUnGiroInTabella() throws Exception {
        //Una riga con gruppo NON di default: e' il caso che la vecchia stringa composita non sapeva
        //rappresentare senza ambiguita', e quello su cui si innestera' la preferenza per gruppo.
        esegui("DELETE FROM PrezziNew WHERE symbol = 'TSTBK'");
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "MERGE INTO PrezziNew (timestamp, exchange, symbol, rete, address, gruppo, prezzo) "
                + "KEY (timestamp, exchange, symbol, rete, address, gruppo) VALUES (?,?,?,'','',?,?)")) {
            ps.setLong(1, ISTANTE);
            ps.setString(2, "binance");
            ps.setString(3, "TSTBK");
            ps.setString(4, "Wallet 01");
            ps.setDouble(5, 123.45);
            ps.executeUpdate();
        }

        String[] r = leggiRiga("TSTBK");
        assertNotNull(r);
        assertEquals("binance", r[0], "la fonte resta nella sua colonna");
        assertEquals("Wallet 01", r[1], "e il gruppo nella sua, senza rimescolarsi");
    }

    @Test
    void unaRigaSenzaGruppoEsplicitoPrendeIlDefault() throws Exception {
        //E' il caso "archivio vecchio ripristinato sulla versione nuova": il CSV non porta la colonna,
        //l'INSERT non la valorizza, e la riga deve finire in 'TUTTI' invece che in NULL o in errore.
        esegui("DELETE FROM PrezziNew WHERE symbol = 'TSTOLD'");
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "INSERT INTO PrezziNew (timestamp, exchange, symbol, rete, address, prezzo) "
                + "VALUES (?,?,?,'','',?)")) {
            ps.setLong(1, ISTANTE);
            ps.setString(2, "Personalizzato");
            ps.setString(3, "TSTOLD");
            ps.setDouble(4, 99.9);
            ps.executeUpdate();
        }

        String[] r = leggiRiga("TSTOLD");
        assertNotNull(r, "la riga senza gruppo deve entrare, non fallire");
        assertEquals("TUTTI", r[1],
                "senza colonna gruppo la riga prende il DEFAULT: e' il valore che aveva quando il "
                + "gruppo stava incollato dentro exchange");
    }

    /** Manifest minimo con una sola tabella salvata, nella forma che {@code Verifica} si aspetta. */
    private static Backup_Restore.Manifest manifestCon(java.util.List<String> colonne) {
        Backup_Restore.Manifest m = new Backup_Restore.Manifest();
        m.FormatoBackup = Backup_Restore.FORMATO_BACKUP;
        m.ColonneTabella = Importazioni.ColonneTabella;
        Backup_Restore.TabellaSalvata ts = new Backup_Restore.TabellaSalvata();
        ts.Righe = 1;
        ts.Colonne = colonne;
        //La chiave porta il prefisso del database: ConnessioneDi lo usa per scegliere la connessione
        //e NomeDi prende cio' che segue la barra. Con un prefisso sbagliato il confronto non
        //arriverebbe alla tabella e si fermerebbe all'avviso "non esiste piu'".
        m.Tabelle.put("personale/PrezziNew", ts);
        return m;
    }

    @Test
    void unArchivioConLaColonnaGruppoSiRipristinaSenzaErrori() {
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(manifestCon(java.util.List.of(
                "TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS", "GRUPPO", "PREZZO")));

        assertTrue(v.Errori.isEmpty(),
                "un archivio prodotto da questa versione deve essere ripristinabile: " + v.Errori);
    }

    @Test
    void unArchivioVecchioSenzaGruppoDaAlPiuUnAvvisoNonUnErrore() {
        //E' il caso di chi aggiorna il programma con un backup fatto prima della migrazione: la
        //colonna manca, e al ripristino le righe prendono il DEFAULT 'TUTTI'. Deve passare: bloccarlo
        //renderebbe irripristinabili tutti gli archivi esistenti.
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(manifestCon(java.util.List.of(
                "TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS", "PREZZO")));

        assertTrue(v.Errori.isEmpty(),
                "un archivio precedente alla colonna gruppo deve restare ripristinabile: " + v.Errori);
    }
}
