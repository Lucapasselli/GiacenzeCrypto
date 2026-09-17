package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link MovimentiStorico}: il buffer differito, il lignaggio e la pulizia alla cancellazione.
 *
 * <p>Gira su un database H2 temporaneo, come {@code DocumentiFonteTest}, quindi non tocca i dati
 * dell'utente.</p>
 *
 * <p>Cosa difende ciascun gruppo, e perché:</p>
 * <ul>
 *   <li><b>la catena sopravvive a un cambio di ID</b>: è la proprietà per cui il lignaggio esiste. Con
 *       la chiave sull'ID del movimento, una versione salvata prima che l'ID cambiasse restava
 *       agganciata a un ID non più esistente e l'utente non la vedeva più;</li>
 *   <li><b>la cancellazione ripulisce tutto</b>, così lo storico non accumula righe orfane;</li>
 *   <li><b>ma non ripulisce un lignaggio ancora vivo</b>: separando un movimento le due gambe
 *       ereditano lo stesso lignaggio, e cancellarne una non deve portarsi via la storia dell'altra.</li>
 * </ul>
 */
class MovimentiStoricoTest {

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

    @BeforeEach
    void puliscePartenza() throws Exception {
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.executeUpdate("DELETE FROM MOVIMENTI_STORICO");
        }
        MovimentiStorico.AzzeraBuffer();
        MappaCryptoWallet.clear();
    }

    // ---------------------------------------------------------------------------------------------
    // Aiutanti
    // ---------------------------------------------------------------------------------------------

    /** Una riga di movimento minima, con l'ID e il lignaggio indicati. */
    private static String[] movimento(String ID, String Lignaggio) {
        String[] v = new String[Importazioni.ColonneTabella];
        Importazioni.RiempiVuotiArray(v);
        v[0] = ID;
        v[MovimentiStorico.CAMPO_LIGNAGGIO] = Lignaggio;
        return v;
    }

    /** Salva il buffer dichiarando che nessun movimento è vivo. */
    private static void salvaSenzaMovimentiVivi() {
        MovimentiStorico.SalvaBuffer(Map.of());
    }

    private static int righeDi(String Lignaggio) {
        return DatabaseH2.StoricoMovimenti_Leggi(Lignaggio).size();
    }

    // ---------------------------------------------------------------------------------------------
    // LIGNAGGIO
    // ---------------------------------------------------------------------------------------------

    @Test
    void assicuraLignaggio_timbraIlCampoVuotoEPoiRiusaLoStesso() {
        String[] mov = movimento("id1", "");

        String Primo = MovimentiStorico.AssicuraLignaggio(mov);

        assertFalse(Primo.isBlank(), "la prima modifica deve generare un lignaggio");
        assertEquals(Primo, mov[MovimentiStorico.CAMPO_LIGNAGGIO], "il lignaggio va timbrato sulla riga");
        //Idempotenza: è così che la seconda modifica dello stesso movimento finisce nella stessa catena
        assertEquals(Primo, MovimentiStorico.AssicuraLignaggio(mov),
                "una seconda modifica deve riusare il lignaggio esistente, non generarne un altro");
    }

    @Test
    void assicuraLignaggio_dueMovimentiDiversiNonCondividonoLaCatena() {
        assertNotEquals(MovimentiStorico.AssicuraLignaggio(movimento("id1", "")),
                MovimentiStorico.AssicuraLignaggio(movimento("id2", "")));
    }

    @Test
    void lignaggioDi_movimentoNonInMappaOMaiModificato_tornaStringaVuota() {
        MappaCryptoWallet.put("id1", movimento("id1", ""));

        assertEquals("", MovimentiStorico.LignaggioDi("id1"), "un movimento mai modificato non ha catena");
        assertEquals("", MovimentiStorico.LignaggioDi("inesistente"));
        assertEquals("", MovimentiStorico.LignaggioDi(null));
    }

    // ---------------------------------------------------------------------------------------------
    // BUFFER DIFFERITO
    // ---------------------------------------------------------------------------------------------

    @Test
    void salvaBuffer_riversaLeVociAccodate() {
        MovimentiStorico.AccodaModifica("LIG-1", "id2", "id1", "riga originale", "ModificaMovimento");
        assertEquals(1, MovimentiStorico.VociInAttesa(), "prima del salvataggio la voce resta in memoria");
        assertEquals(0, righeDi("LIG-1"), "niente deve essere scritto prima del salvataggio");

        salvaSenzaMovimentiVivi();

        assertEquals(0, MovimentiStorico.VociInAttesa());
        List<String[]> Versioni = DatabaseH2.StoricoMovimenti_Leggi("LIG-1");
        assertEquals(1, Versioni.size());
        assertEquals("id1", Versioni.get(0)[0], "l'ID consumato resta leggibile, per mostrarlo all'utente");
        assertEquals("riga originale", Versioni.get(0)[2]);
        assertEquals("id2", Versioni.get(0)[4]);
    }

    @Test
    void vocePrivaDiLignaggio_nonVieneAccodata() {
        //Una riga senza lignaggio non sarebbe né leggibile né cancellabile: meglio non scriverla
        MovimentiStorico.AccodaModifica("", "id2", "id1", "riga", "ModificaMovimento");

        assertEquals(0, MovimentiStorico.VociInAttesa());
    }

    @Test
    void creaECancellaNellaStessaSessione_nonLasciaRighe() {
        MovimentiStorico.AccodaModifica("LIG-1", "id2", "id1", "riga originale", "ModificaMovimento");
        MovimentiStorico.AccodaCancellazione("LIG-1");

        salvaSenzaMovimentiVivi();

        //Le cancellazioni girano dopo le modifiche proprio per questo: la voce viene scritta e subito
        //ripulita, invece di restare a metà
        assertEquals(0, righeDi("LIG-1"));
        assertEquals(0, MovimentiStorico.VociInAttesa());
    }

    // ---------------------------------------------------------------------------------------------
    // LA CATENA SOPRAVVIVE A UN CAMBIO DI ID (il motivo per cui il lignaggio esiste)
    // ---------------------------------------------------------------------------------------------

    @Test
    void modificaInPlaceSeguitaDaCambioDiId_restaLeggibileTuttaInsieme() {
        //Sequenza reale: modifica in place quando il movimento si chiamava ..._AC, poi una modifica che
        //ricalcola l'ID in ..._SF. Con la chiave sull'ID la prima riga restava agganciata a _AC, che non
        //esiste più come movimento, e il visualizzatore ne mostrava una sola.
        MovimentiStorico.AccodaModifica("LIG-1", "mov_AC", "mov_AC", "riga;versione;uno",
                MovimentiStorico.OP_IN_PLACE);
        MovimentiStorico.AccodaModifica("LIG-1", "mov_SF", "mov_AC", "riga;versione;due",
                "ModificaMovimento");

        salvaSenzaMovimentiVivi();

        assertEquals(2, righeDi("LIG-1"),
                "entrambe le versioni devono essere leggibili dal movimento vivo, che ora si chiama _SF");
    }

    @Test
    void ilVisualizzatoreLeggeDalMovimentoVivo_ancheDopoIlCambioDiId() {
        //La stessa cosa dal punto di vista di chi apre il dialogo: si parte dall'ID vivo, non dal lignaggio
        MovimentiStorico.AccodaModifica("LIG-1", "mov_AC", "mov_AC", "riga;uno", MovimentiStorico.OP_IN_PLACE);
        MovimentiStorico.AccodaModifica("LIG-1", "mov_SF", "mov_AC", "riga;due", "ModificaMovimento");
        salvaSenzaMovimentiVivi();
        MappaCryptoWallet.put("mov_SF", movimento("mov_SF", "LIG-1"));

        assertTrue(DatabaseH2.StoricoMovimenti_Esiste(MovimentiStorico.LignaggioDi("mov_SF")),
                "il pulsante delle versioni precedenti deve accendersi sul movimento vivo");
        assertEquals(2, righeDi(MovimentiStorico.LignaggioDi("mov_SF")));
    }

    // ---------------------------------------------------------------------------------------------
    // CANCELLAZIONE
    // ---------------------------------------------------------------------------------------------

    @Test
    void cancellazione_ripuliscelInteraCatenaInUnColpoSolo() {
        MovimentiStorico.AccodaModifica("LIG-1", "id2", "id1", "riga;uno", "ModificaMovimento");
        MovimentiStorico.AccodaModifica("LIG-1", "id3", "id2", "riga;due", "TraslaOrario");
        salvaSenzaMovimentiVivi();
        assertEquals(2, righeDi("LIG-1"));

        MovimentiStorico.AccodaCancellazione("LIG-1");
        salvaSenzaMovimentiVivi();

        assertEquals(0, righeDi("LIG-1"), "nessuna riga orfana deve restare dopo la cancellazione");
    }

    @Test
    void cancellazioneDiUnaGambaSeparata_nonToccaLaStoriaCheLAltraUsaAncora() {
        //Separando un movimento le due gambe ereditano lo stesso lignaggio (CampiDaRiportare contiene il
        //42): cancellarne una non deve portarsi via la storia condivisa.
        MovimentiStorico.AccodaModifica("LIG-1", "id2", "id1", "riga;uno", "ModificaMovimento");
        salvaSenzaMovimentiVivi();
        MappaCryptoWallet.put("gamba2", movimento("gamba2", "LIG-1"));

        MovimentiStorico.AccodaCancellazione("LIG-1");
        MovimentiStorico.SalvaBuffer(MappaCryptoWallet);

        assertEquals(1, righeDi("LIG-1"), "la gamba superstite porta ancora quel lignaggio");

        //Cancellata anche l'altra gamba, non resta nessuno a usare quella storia
        MappaCryptoWallet.clear();
        MovimentiStorico.AccodaCancellazione("LIG-1");
        MovimentiStorico.SalvaBuffer(MappaCryptoWallet);

        assertEquals(0, righeDi("LIG-1"), "sparita l'ultima gamba, la catena va ripulita");
    }

    @Test
    void cancellazioneDiUnMovimentoMaiModificato_nonAccodaNulla() {
        MovimentiStorico.AccodaCancellazione("");

        assertEquals(0, MovimentiStorico.VociInAttesa());
    }

    @Test
    void catenaDiUnAltroMovimento_nonVieneToccata() {
        MovimentiStorico.AccodaModifica("LIG-1", "id2", "id1", "riga;uno", "ModificaMovimento");
        MovimentiStorico.AccodaModifica("LIG-2", "altro2", "altro1", "riga;due", "ModificaMovimento");
        salvaSenzaMovimentiVivi();

        MovimentiStorico.AccodaCancellazione("LIG-1");
        salvaSenzaMovimentiVivi();

        assertEquals(0, righeDi("LIG-1"));
        assertEquals(1, righeDi("LIG-2"), "i lignaggi sono separati per costruzione");
    }
}
