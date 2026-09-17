package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copre la migrazione della tabella dei prezzi personalizzati che separa il <b>gruppo wallet</b>
 * dalla <b>fonte</b>: fino al 2026-09-17 i due stavano incollati in una stringa sola dentro la
 * colonna {@code exchange} ({@code "Personalizzato (TUTTI)"}), adesso il gruppo ha una colonna sua e
 * fa parte della chiave primaria.
 *
 * <p><b>Perché serve un test dedicato.</b> Il resto della suite crea sempre database nuovi, quindi
 * esercita solo il ramo {@code CREATE TABLE} con lo schema già aggiornato. Il codice che girerà sulle
 * macchine degli utenti è l'altro: l'{@code ALTER} su una tabella che esiste già e contiene i loro
 * prezzi di fine anno, cioè il dato su cui si regge il quadro RW. Qui la tabella viene quindi creata
 * <b>nella forma vecchia</b>, riempita con righe nel vecchio formato, e solo dopo si lascia partire
 * {@link DatabaseH2#CreaoCollegaDatabase()}.
 *
 * <p>L'ordine dei passi della migrazione non è arbitrario e questo test lo protegge: H2 pretende che
 * le colonne di chiave siano {@code NOT NULL} (errore 90023) mentre {@code ADD COLUMN ... DEFAULT}
 * crea una colonna nullable, quindi il {@code SET NOT NULL} deve stare fra il riempimento e la nuova
 * chiave. Invertendoli la migrazione fallisce all'avvio, e fallirebbe in silenzio nel log.
 */
class PrezziPersonalizzatiMigrazioneGruppoTest {

    @TempDir
    static Path tempDir;

    private static final long FINE_2024 = 1735686000000L;
    private static final long FINE_2025 = 1767222000000L;

    @BeforeAll
    static void creaDatabaseVecchioPoiMigra() throws Exception {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");

        //Tabella nella forma PRECEDENTE alla migrazione: niente colonna `gruppo`, chiave a cinque
        //colonne, e il gruppo incollato dentro `exchange`. E' lo stato in cui si trova il database
        //di un utente che aggiorna il programma.
        try (Connection c = DriverManager.getConnection(VarStatiche.getDBPersonale(),
                DatabaseH2.usernameH2, DatabaseH2.passwordH2);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE PrezziNew ("
                    + "timestamp BIGINT NOT NULL, "
                    + "exchange VARCHAR(100) NOT NULL, "
                    + "symbol VARCHAR(100) NOT NULL, "
                    + "rete VARCHAR(100) NOT NULL, "
                    + "address VARCHAR(255) NOT NULL, "
                    + "prezzo DOUBLE, "
                    + "PRIMARY KEY (timestamp, exchange, symbol, rete, address))");
            //prezzo di fine anno per simbolo, nel vecchio formato composito
            st.execute("INSERT INTO PrezziNew VALUES (" + FINE_2024
                    + ", 'Personalizzato (TUTTI)', 'BTC', '', '', 90211.125)");
            //prezzo per indirizzo/rete a zero: e' il caso piu' frequente nell'uso reale
            st.execute("INSERT INTO PrezziNew VALUES (" + FINE_2025
                    + ", 'Personalizzato (TUTTI)', '', 'CRO', '0xabc', 0)");
            //fonte senza parentesi: non deve essere toccata dallo spacchettamento
            st.execute("INSERT INTO PrezziNew VALUES (" + FINE_2025
                    + ", 'binance', 'ETH', '', '', 2519.27)");
        }

        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "la connessione deve riuscire: e' durante questa che gira la migrazione");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static List<String[]> leggi(String sql) throws Exception {
        List<String[]> righe = new ArrayList<>();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                String[] r = new String[n];
                for (int i = 1; i <= n; i++) r[i - 1] = rs.getString(i);
                righe.add(r);
            }
        }
        return righe;
    }

    @Test
    void ilCompositoVieneSpacchettatoInFonteEGruppo() throws Exception {
        List<String[]> r = leggi("SELECT exchange, gruppo FROM PrezziNew WHERE symbol = 'BTC'");

        assertEquals(1, r.size(), "la riga deve esserci ancora: la migrazione non perde dati");
        assertEquals("Personalizzato", r.get(0)[0], "da `exchange` sparisce la parte fra parentesi");
        assertEquals("TUTTI", r.get(0)[1], "e finisce nella colonna `gruppo`");
    }

    @Test
    void loSpacchettamentoVienefattoAncheSulleRighePerIndirizzo() throws Exception {
        List<String[]> r = leggi("SELECT exchange, gruppo, prezzo FROM PrezziNew WHERE address = '0xabc'");

        assertEquals(1, r.size());
        assertEquals("Personalizzato", r.get(0)[0]);
        assertEquals("TUTTI", r.get(0)[1]);
        assertEquals(0.0, Double.parseDouble(r.get(0)[2]), 0.000001,
                "il prezzo a zero e' un valore voluto (token che a fine anno non vale nulla), non un dato mancante");
    }

    @Test
    void unaFonteSenzaParentesiRestaIntatta() throws Exception {
        List<String[]> r = leggi("SELECT exchange, gruppo FROM PrezziNew WHERE symbol = 'ETH'");

        assertEquals(1, r.size());
        assertEquals("binance", r.get(0)[0], "senza parentesi non c'e' nulla da spacchettare");
        assertEquals("TUTTI", r.get(0)[1], "e il gruppo prende il valore di default");
    }

    @Test
    void nessunaRigaVienePersa() throws Exception {
        assertEquals("3", leggi("SELECT COUNT(*) FROM PrezziNew").get(0)[0]);
    }

    @Test
    void laChiavePrimariaComprendeIlGruppo() throws Exception {
        List<String[]> r = leggi("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                + "WHERE TABLE_NAME = 'PREZZINEW' ORDER BY ORDINAL_POSITION");

        List<String> colonne = new ArrayList<>();
        for (String[] riga : r) colonne.add(riga[0]);

        assertEquals(List.of("TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS", "GRUPPO"), colonne,
                "senza GRUPPO in chiave, due prezzi dello stesso istante per gruppi diversi si "
                + "sovrascriverebbero a vicenda");
    }

    @Test
    void ilGruppoNonPuoEssereNullo() throws Exception {
        //E' il vincolo che H2 pretende per una colonna di chiave, ed e' anche quello che garantisce
        //che una riga scritta senza gruppo esplicito finisca in 'TUTTI' invece che in un limbo.
        assertEquals("NO", leggi("SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = 'PREZZINEW' AND COLUMN_NAME = 'GRUPPO'").get(0)[0]);
    }

    @Test
    void ilWriterScriveFonteEGruppoSeparati() throws Exception {
        //Giro completo sul writer vero: quello che arriva come "Personalizzato (TUTTI)" dalla GUI
        //non deve piu' essere ricomposto in una stringa sola.
        assertTrue(DatabaseH2.InserisciPrezzoPresonalizzato(FINE_2025, "Personalizzato (TUTTI)",
                "SOL", "105.64", "", "", "TUTTI", FINE_2025));

        List<String[]> r = leggi("SELECT exchange, gruppo, prezzo FROM PrezziNew WHERE symbol = 'SOL'");
        assertEquals(1, r.size());
        assertEquals("Personalizzato", r.get(0)[0]);
        assertEquals("TUTTI", r.get(0)[1]);
        assertEquals(105.64, Double.parseDouble(r.get(0)[2]), 0.000001);
    }
}
