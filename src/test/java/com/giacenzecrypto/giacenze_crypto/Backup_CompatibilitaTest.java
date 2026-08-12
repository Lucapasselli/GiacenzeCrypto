package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Backup_Compatibilita}, cioè della domanda "questo archivio si può ancora ripristinare
 * su questa versione?".
 *
 * <p>La regola che questi test difendono è che la risposta <b>non</b> si dà confrontando numeri di
 * versione — fra due build può non essere cambiato niente di rilevante, oppure può essere cambiato tutto
 * — ma confrontando la <i>forma dei dati</i> dichiarata nel manifest: quanti campi ha una riga di
 * movimento e con quali colonne è stata scritta ogni tabella.
 *
 * <p>L'asimmetria fra "meno colonne" e "più colonne" è il punto: un backup più vecchio si ripristina
 * riempiendo di vuoto i campi che non aveva, esattamente come già succede caricando un file di movimenti
 * di una versione precedente; un backup più <i>nuovo</i> no, perché i campi in eccesso sparirebbero senza
 * che nessuno possa dire cosa contenevano.
 */
class Backup_CompatibilitaTest {

    @TempDir
    static Path tempDir;

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

    // =================================================================================================
    // FORMATO DELL'ARCHIVIO
    // =================================================================================================
    @Test
    void unManifestAssenteEIncompatibile() {
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(null);
        assertEquals(Backup_Compatibilita.Esito.INCOMPATIBILE, v.Esito);
        assertFalse(v.Ripristinabile());
    }

    @Test
    void unArchivioDaUnaVersioneFuturaVieneRifiutato() {
        Backup_Restore.Manifest m = manifestBase();
        m.FormatoBackup = Backup_Restore.FORMATO_BACKUP + 1;

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.INCOMPATIBILE, v.Esito);
        assertTrue(v.Descrizione().contains("versione più recente"),
                "il messaggio deve dire all'utente che cosa fare, cioè aggiornare");
    }

    @Test
    void unArchivioDelFormatoCorrenteVaBene() {
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(manifestBase());
        assertEquals(Backup_Compatibilita.Esito.COMPATIBILE, v.Esito);
        assertTrue(v.Ripristinabile());
        assertTrue(v.Errori.isEmpty());
        assertTrue(v.Avvisi.isEmpty());
    }

    // =================================================================================================
    // NUMERO DI CAMPI DELLA RIGA DI MOVIMENTO
    // =================================================================================================
    @Test
    void menoCampiSiRipristinaConUnAvviso() {
        Backup_Restore.Manifest m = manifestBase();
        m.ColonneTabella = Importazioni.ColonneTabella - 1;

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.CON_AVVISI, v.Esito);
        assertTrue(v.Ripristinabile(), "un backup più vecchio deve restare ripristinabile");
        assertTrue(v.Errori.isEmpty());
        assertEquals(1, v.Avvisi.size());
    }

    @Test
    void piuCampiVieneRifiutato() {
        Backup_Restore.Manifest m = manifestBase();
        m.ColonneTabella = Importazioni.ColonneTabella + 1;

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.INCOMPATIBILE, v.Esito);
        assertFalse(v.Ripristinabile(),
                "i campi in più andrebbero persi in silenzio: meglio rifiutare");
    }

    @Test
    void stessoNumeroDiCampiNonProduceNulla() {
        Backup_Restore.Manifest m = manifestBase();
        m.ColonneTabella = Importazioni.ColonneTabella;

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.COMPATIBILE, v.Esito);
    }

    // =================================================================================================
    // FORMA DELLE TABELLE
    // =================================================================================================
    @Test
    void unaTabellaConLeStesseColonneVaBene() {
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/WALLETGRUPPO", tabella(1, "WALLET", "GRUPPO"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.COMPATIBILE, v.Esito);
    }

    @Test
    void unaColonnaInMenoNelBackupNonEUnProblema() {
        //Il backup è più vecchio e non ha una colonna aggiunta dopo: quella resterà al proprio default
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/GRUPPO_ALIAS", tabella(41, "GRUPPO", "ALIAS"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.COMPATIBILE, v.Esito,
                "le colonne aggiunte dopo restano al default, non è un avviso");
    }

    @Test
    void unaColonnaScomparsaProduceUnAvviso() {
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/WALLETGRUPPO", tabella(1, "WALLET", "GRUPPO", "COLONNA_SPARITA"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.CON_AVVISI, v.Esito);
        assertTrue(v.Ripristinabile());
        assertTrue(v.Avvisi.get(0).contains("COLONNA_SPARITA"));
    }

    @Test
    void unaColonnaDiChiaveScomparsaVieneRifiutata() {
        //Senza la chiave, righe distinte del backup diventerebbero la stessa riga
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/PrezziNew",
                tabella(10, "TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS", "PREZZO", "SCADUTA"));
        //Si finge che ADDRESS non esista più togliendola dall'elenco delle attuali non si può fare:
        //si usa invece una chiave che nella tabella non c'è di sicuro
        m.Tabelle.put("personale/WALLETGRUPPO", tabella(1, "WALLET", "GRUPPO"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        //PrezziNew ha davvero tutte quelle colonne tranne SCADUTA, che non è una chiave: quindi avviso
        assertEquals(Backup_Compatibilita.Esito.CON_AVVISI, v.Esito);

        //Adesso una tabella la cui chiave dichiarata non esiste più nel database
        Backup_Restore.Manifest m2 = manifestBase();
        m2.Tabelle.put("personale/EMONEY", tabella(3, "MONETA_RINOMINATA", "DATA"));
        Backup_Compatibilita.Verdetto v2 = Backup_Compatibilita.Verifica(m2);
        assertEquals(Backup_Compatibilita.Esito.CON_AVVISI, v2.Esito,
                "MONETA_RINOMINATA non è fra le chiavi note di EMONEY: resta un avviso");
    }

    @Test
    void unaTabellaCheNonEsistePiuVieneSaltata() {
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/TABELLA_CHE_NON_ESISTE", tabella(7, "A", "B"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(Backup_Compatibilita.Esito.CON_AVVISI, v.Esito);
        assertTrue(v.Ripristinabile(), "una tabella sparita non impedisce di ripristinare il resto");
        assertTrue(v.Avvisi.get(0).contains("7 righe"),
                "l'avviso deve dire quante righe si perdono, altrimenti non è decidibile");
    }

    @Test
    void piuProblemiInsiemeVengonoElencatiTutti() {
        Backup_Restore.Manifest m = manifestBase();
        m.Tabelle.put("personale/WALLETGRUPPO", tabella(1, "WALLET", "GRUPPO", "SPARITA_UNO"));
        m.Tabelle.put("personale/EMONEY", tabella(2, "MONETA", "DATA", "SPARITA_DUE"));

        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        assertEquals(2, v.Avvisi.size(), "l'utente deve vederli tutti, non solo il primo");
        assertTrue(v.Descrizione().contains("SPARITA_UNO"));
        assertTrue(v.Descrizione().contains("SPARITA_DUE"));
    }

    // =================================================================================================
    // UTILITÀ
    // =================================================================================================
    /** Un manifest minimo e valido, dal quale partire per rompere una cosa alla volta. */
    private static Backup_Restore.Manifest manifestBase() {
        Backup_Restore.Manifest m = new Backup_Restore.Manifest();
        m.FormatoBackup = Backup_Restore.FORMATO_BACKUP;
        m.Creato = "2026-08-12 18:30:12";
        m.Versione = VarStatiche.Versione;
        m.ColonneTabella = Importazioni.ColonneTabella;
        return m;
    }

    private static Backup_Restore.TabellaSalvata tabella(int Righe, String... Colonne) {
        Backup_Restore.TabellaSalvata t = new Backup_Restore.TabellaSalvata();
        t.Righe = Righe;
        for (String c : Colonne) {
            t.Colonne.add(c);
        }
        return t;
    }
}
