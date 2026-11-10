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

    // ---------------------------------------------------------------------------------------------
    // Ripristino delle modifiche manuali: data esplicita, doppioni, scarto con Annulla, lettura di tutto
    // ---------------------------------------------------------------------------------------------

    @Test
    void laModificaAccodataConUnaDataLaConservaFinoAlDatabase() {
        MovimentiStorico.AccodaModifica("L1", "B", "A", "riga", "TraslaOrario", 1234L);

        assertTrue(MovimentiStorico.ContieneVoce("L1", 1234L, "A"), "nel buffer");
        salvaSenzaMovimentiVivi();

        assertEquals(1, righeDi("L1"));
        assertEquals("1234", DatabaseH2.StoricoMovimenti_Leggi("L1").get(0)[1], "la data decisa dal chiamante, non l'istante del salvataggio");
        assertTrue(MovimentiStorico.ContieneVoce("L1", 1234L, "A"), "su disco");
    }

    @Test
    void ilControlloDeiDoppioniDistingueLIdDiPartenza() {
        //Le righe di un'unione nascono nello stesso millisecondo: differiscono solo per l'ID di partenza
        MovimentiStorico.AccodaModifica("L1", "N", "M1", "r1", "UnisciMovimenti", 1000L);

        assertTrue(MovimentiStorico.ContieneVoce("L1", 1000L, "M1"));
        assertFalse(MovimentiStorico.ContieneVoce("L1", 1000L, "M2"));
        assertFalse(MovimentiStorico.ContieneVoce("L1", 2000L, "M1"));
        assertFalse(MovimentiStorico.ContieneVoce("altro", 1000L, "M1"));
    }

    @Test
    void annullaScartaLeVociInAttesaEIlSalvataggioSuccessivoNonScriveNulla() {
        MovimentiStorico.AccodaModifica("L1", "B", "A", "riga", "ModificaMovimento", 1L);
        MovimentiStorico.AccodaCancellazione("L2");
        assertEquals(2, MovimentiStorico.VociInAttesa());

        MovimentiStorico.ScartaBuffer();
        salvaSenzaMovimentiVivi();

        assertEquals(0, MovimentiStorico.VociInAttesa());
        assertEquals(0, righeDi("L1"), "una modifica annullata non lascia storico orfano");
    }

    @Test
    void tutteLeVersioniUnisceDatabaseEBufferDallaPiuVecchia() {
        MovimentiStorico.AccodaModifica("L1", "B", "A", "r1", "ModificaMovimento", 3000L);
        salvaSenzaMovimentiVivi();
        MovimentiStorico.AccodaModifica("L1", "C", "B", "r2", "TraslaOrario", 2000L);
        MovimentiStorico.AccodaModifica("L2", "Y", "X", "r3", "ModificaMovimento", 1000L);

        Map<String, List<String[]>> tutte = MovimentiStorico.TutteLeVersioni();

        assertEquals(2, tutte.size());
        assertEquals(2, tutte.get("L1").size());
        assertEquals("2000", tutte.get("L1").get(0)[1], "dalla piu' vecchia");
        assertEquals("3000", tutte.get("L1").get(1)[1]);
        assertEquals("L2", tutte.keySet().iterator().next(), "i lignaggi seguono la prima modifica");
    }

    @Test
    void ilRipristinoRimetteLoStoricoVeroConLeDateOriginaliSenzaDoppioni() throws Exception {
        String[] prima = movimento("A_1", "");
        prima[15] = "10.00";
        String[] dopo = movimento("A_1", "L1");
        dopo[15] = "12.50";
        Map<String, List<String[]>> storico = Map.of("L1", new java.util.ArrayList<>(java.util.List.<String[]>of(
                new String[]{"A_1", "1000", Importazioni.SerializzaRiga(prima), "ModificaMovimento", "A_1"})));
        Principale_Opzioni_ModificheManuali_Ripristino.Contenuto c = Principale_Opzioni_ModificheManuali_Ripristino
                .Leggi(Principale_Opzioni_ModificheManuali.Testo(Principale_Opzioni_ModificheManuali.Costruisci(Map.of("A_1", dopo), storico)));
        String[] reimportato = movimento("A_1", "");
        reimportato[15] = "10.00";
        Map<String, String[]> archivio = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        archivio.put("A_1", reimportato);

        var piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, Principale_Opzioni_ModificheManuali_Ripristino.Storico.PREDEFINITO);

        assertEquals(1, MovimentiStorico.VociInAttesa());
        assertTrue(MovimentiStorico.ContieneVoce("L1", 1000L, "A_1"));
        assertEquals("L1", archivio.get("A_1")[MovimentiStorico.CAMPO_LIGNAGGIO]);
        //Un secondo ripristino sullo stesso archivio non aggiunge nulla
        var secondo = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        Principale_Opzioni_ModificheManuali_Ripristino.Applica(secondo, archivio, Principale_Opzioni_ModificheManuali_Ripristino.Storico.PREDEFINITO);
        assertEquals(1, MovimentiStorico.VociInAttesa());
    }

    @Test
    void accodaPrimaDiModificareTimbraIlLignaggioESalvaLaRigaDiPrima() {
        String[] m = movimento("A_1", "");
        m[15] = "10.00";

        MovimentiStorico.AccodaPrimaDiModificare(m, MovimentiStorico.OP_PREZZO);
        m[15] = "99.00";

        assertFalse(m[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank());
        List<String[]> versioni = MovimentiStorico.Versioni(m[MovimentiStorico.CAMPO_LIGNAGGIO]);
        assertEquals(1, versioni.size());
        assertEquals("10.00", Importazioni.DeserializzaRiga(versioni.get(0)[2])[15], "la riga com'era prima della scrittura");
        assertEquals(MovimentiStorico.OP_PREZZO, versioni.get(0)[3]);
    }

    @Test
    void unPrezzoCambiatoConModificaPrezzoSiRipristinaDopoLaReimportazione() throws Exception {
        String[] riga = movimento("20250101100000_Ex_1_1_AC", "");
        riga[1] = "2025-01-01 10:00";
        riga[3] = "Ex";
        riga[8] = "EUR";
        riga[10] = "50";
        riga[11] = "BTC";
        riga[13] = "0.001";
        riga[15] = "10.00";
        riga[32] = "SI";
        riga[40] = "BTC|1|10.00|Binance";
        MappaCryptoWallet.put(riga[0], riga);
        GUI_ModificaPrezzo.ApplicaPrezzoScelto(riga, "12.50", "BTC|2|12.50|Coingecko");
        var esportazione = Principale_Opzioni_ModificheManuali.Costruisci(MappaCryptoWallet, MovimentiStorico.TutteLeVersioni());
        var contenuto = Principale_Opzioni_ModificheManuali_Ripristino.Leggi(Principale_Opzioni_ModificheManuali.Testo(esportazione));

        //Reimportazione da zero: il movimento torna com'era, con il prezzo scaricato allora
        String[] reimportato = riga.clone();
        reimportato[15] = "10.00";
        reimportato[40] = "BTC|1|10.00|Binance";
        reimportato[MovimentiStorico.CAMPO_LIGNAGGIO] = "";
        Map<String, String[]> archivio = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        archivio.put(reimportato[0], reimportato);
        var piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(contenuto, archivio);
        var risultato = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio,
                Principale_Opzioni_ModificheManuali_Ripristino.Storico.PREDEFINITO);

        assertEquals(1, risultato.applicate());
        assertEquals("12.50", archivio.get(riga[0])[15]);
        assertEquals("BTC|2|12.50|Coingecko", archivio.get(riga[0])[40]);
        assertEquals(riga[MovimentiStorico.CAMPO_LIGNAGGIO], archivio.get(riga[0])[MovimentiStorico.CAMPO_LIGNAGGIO]);
        assertEquals(1, MovimentiStorico.VociInAttesa(), "lo storico c'e' gia' in buffer: non si raddoppia");
    }
}
