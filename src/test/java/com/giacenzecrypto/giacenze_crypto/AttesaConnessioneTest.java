package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa il comportamento di {@link AttesaConnessione} e del rifiuto di scrivere che ne consegue.
 *
 * <p>Il difetto che questo meccanismo chiude, osservato il 12/09/2026 su un archivio Binance reale: la
 * connessione è caduta a metà importazione, {@code Prezzi.CambioXXXEUR} ha restituito il ripiego (nullo)
 * per ogni movimento successivo e l'importazione è arrivata in fondo scrivendo <b>15.158 movimenti su
 * 19.166 senza prezzo</b>. La regola concordata è: prima si riprova, poi si abbandona tutto.
 *
 * <p>Qui non si simula la caduta della rete — servirebbe staccarla davvero — ma si fissano le tre
 * proprietà da cui dipende la correttezza: lo scope, la resa, e il fatto che una resa impedisca la
 * scrittura. L'attesa vera (tre tentativi da tre minuti) non è esercitabile in un test e resta
 * verificata a mano.
 */
class AttesaConnessioneTest {

    @AfterEach
    void chiudeGliScopeRimasti() {
        //Un test che fallisce a metà non deve lasciare uno scope aperto agli altri
        while (AttesaConnessione.ScopeAperto()) AttesaConnessione.Chiudi();
    }

    // ---- lo scope ------------------------------------------------------------------------------

    /**
     * È la proprietà che rende il meccanismo innocuo per tutto il resto del programma: la
     * valorizzazione di un singolo movimento, il ricalcolo RW e l'aggiornamento delle configurazioni
     * continuano a mollare subito quando manca la rete, invece di bloccarsi nove minuti.
     */
    @Test
    void fuoriDaUnoScopeNonSiAspettaENonSiAbortisce() {
        assertFalse(AttesaConnessione.ScopeAperto());
        assertFalse(AttesaConnessione.Attendi(), "fuori da un'importazione Attendi() non deve fare nulla");
        assertFalse(AttesaConnessione.Abortita());
    }

    @Test
    void loScopeSiAnnidaESiChiudeSoloAllUltimoLivello() {
        AttesaConnessione.Apri(null);
        AttesaConnessione.Apri(null);
        assertTrue(AttesaConnessione.ScopeAperto());
        AttesaConnessione.Chiudi();
        assertTrue(AttesaConnessione.ScopeAperto(), "un'operazione annidata non chiude lo scope di chi la contiene");
        AttesaConnessione.Chiudi();
        assertFalse(AttesaConnessione.ScopeAperto());
    }

    /**
     * Una resa non deve sopravvivere all'importazione che l'ha subita: se lo facesse, la prossima
     * importazione della stessa sessione non scriverebbe nulla e nessuno capirebbe perché.
     */
    @Test
    void laResaNonSopravviveAllaChiusuraDelloScope() {
        AttesaConnessione.Apri(null);
        forzaResa();
        assertTrue(AttesaConnessione.Abortita());
        AttesaConnessione.Chiudi();

        assertFalse(AttesaConnessione.Abortita(), "chiusa l'importazione la resa deve sparire");
        AttesaConnessione.Apri(null);
        assertFalse(AttesaConnessione.Abortita(), "la nuova importazione parte pulita");
        AttesaConnessione.Chiudi();
    }

    /**
     * Sull'EDT non si dorme: bloccherebbe la finestra che deve mostrare l'attesa e il pulsante che deve
     * annullarla. Si preferisce arrendersi subito — l'importazione non scrive, che è comunque l'esito
     * corretto — invece di congelare il programma per nove minuti.
     */
    @Test
    void sullEdtNonSiAspettaMaSiArrende() throws Exception {
        AttesaConnessione.Apri(null);
        final boolean[] esito = new boolean[1];
        long t = System.currentTimeMillis();
        javax.swing.SwingUtilities.invokeAndWait(() -> esito[0] = AttesaConnessione.Attendi());
        assertFalse(esito[0]);
        assertTrue(System.currentTimeMillis() - t < 5000, "non deve aver dormito");
        assertTrue(AttesaConnessione.Abortita(), "saltare l'attesa non vuol dire proseguire l'importazione");
        AttesaConnessione.Chiudi();
    }

    // ---- la scrittura --------------------------------------------------------------------------

    /**
     * Il punto di strozzatura attraversato da tutti gli importatori da file e dalle due strade CCXT.
     * Con la resa in corso non deve scrivere niente: non importare è anche ciò che rende l'abbandono
     * ripetibile, perché la prossima esecuzione riparte dallo stesso punto (stesso principio del bug
     * C13 sull'import OKX incompleto).
     */
    @Test
    void conLaResaScriviListaSuMappaCryptoNonScriveNulla() {
        int primaInMappa = Principale.MappaCryptoWallet.size();
        String avvisiPrima = Importazioni.movimentiSconosciuti;

        AttesaConnessione.Apri(null);
        forzaResa();
        int[] esito = Importazioni.ScriviListaSuMappaCrypto(movimentiFinti(), false, 0);
        AttesaConnessione.Chiudi();

        assertArrayEquals(new int[]{0, 0}, esito, "nessun movimento aggiunto e nessuno scartato");
        assertEquals(primaInMappa, Principale.MappaCryptoWallet.size(), "la mappa non deve essere toccata");
        assertTrue(Importazioni.movimentiSconosciuti.startsWith(Importazioni.AVVISO_IMPORT_ABBANDONATO),
                "l'avviso deve finire nel riquadro errori del resoconto, o l'utente vede solo \"0 importati\"");

        Importazioni.movimentiSconosciuti = avvisiPrima;
    }

    /** Senza resa la scrittura resta quella di sempre: la guardia non deve cambiare il caso normale. */
    @Test
    void senzaResaLaScritturaAvvieneNormalmente() {
        List<String[]> lista = movimentiFinti();
        String id = lista.get(0)[0];
        try {
            int[] esito = Importazioni.ScriviListaSuMappaCrypto(lista, false, 0);
            assertEquals(1, esito[0], "il movimento deve essere stato scritto");
            assertNotNull(Principale.MappaCryptoWallet.get(id));
        } finally {
            Principale.MappaCryptoWallet.remove(id);
        }
    }

    /**
     * Gli scambi differiti girano PRIMA della scrittura in {@code ImportazioneGenerica}: con la resa in
     * corso accoppierebbero movimenti che in {@code MappaCryptoWallet} non finiranno mai.
     */
    @Test
    void conLaResaGliScambiDifferitiNonVengonoAccoppiati() {
        int primaInMappa = Principale.MappaCryptoWallet.size();
        AttesaConnessione.Apri(null);
        forzaResa();
        //Se non uscisse subito, questa riga farebbe da "PRELIEVO" e cercherebbe la controparte
        Importazioni.ConsolidaMovimentiDifferiti(movimentiFinti(), false);
        AttesaConnessione.Chiudi();
        assertEquals(primaInMappa, Principale.MappaCryptoWallet.size());
    }

    // ---- utilità -------------------------------------------------------------------------------

    /**
     * Provoca la resa senza aspettare i nove minuti reali, passando dall'unica strada che la produce:
     * {@link AttesaConnessione#Attendi()} chiamato sull'EDT, che rinuncia all'attesa e si arrende.
     */
    private static void forzaResa() {
        try {
            javax.swing.SwingUtilities.invokeAndWait(AttesaConnessione::Attendi);
        } catch (Exception e) {
            fail("impossibile forzare la resa: " + e);
        }
        assertTrue(AttesaConnessione.Abortita());
    }

    /** Un movimento minimo, con l'ID nel formato a 5 segmenti che il resto del programma si aspetta. */
    private static List<String[]> movimentiFinti() {
        String[] m = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < m.length; i++) m[i] = "";
        m[0] = "20220512062450_TestAttesa_001_001_PC";
        m[1] = "2022-05-12 06:24";
        m[3] = "TestAttesa";
        m[4] = "Principale";
        m[5] = "PRELIEVO CRYPTO";
        m[8] = "USDT";
        m[10] = "-100";
        m[15] = "100.00";
        List<String[]> lista = new ArrayList<>();
        lista.add(m);
        return lista;
    }
}
