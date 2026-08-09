package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa il comportamento della modifica note su selezione multipla: la composizione della nota
 * (accoda / sostituisci) e la scrittura sui movimenti.
 *
 * <p>La parte interattiva ({@code GUIModificaNote}) non è verificabile senza schermo, ma la scelta
 * accoda/sostituisci si limita a passare un booleano a queste funzioni: è qui che sta la logica che
 * può sbagliare, cioè perdere una nota esistente o lasciare un {@code <br>} appeso in coda.</p>
 */
public class ModificaNoteMultiplaTest {

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /** Inserisce in mappa un movimento con la sola nota valorizzata: il resto non serve a queste funzioni. */
    private static void movimentoConNota(String ID, String Nota) {
        String v[] = new String[Importazioni.ColonneTabella];
        Importazioni.RiempiVuotiArray(v);
        v[0] = ID;
        v[21] = Nota;
        MappaCryptoWallet.put(ID, v);
    }

    @Test
    public void sostituireScriveLaNuovaNotaSuTutti() {
        movimentoConNota("A", "vecchia");
        movimentoConNota("B", "");

        assertEquals(2, Funzioni.ApplicaNotaAiMovimenti(List.of("A", "B"), "nuova", false));

        assertEquals("nuova", MappaCryptoWallet.get("A")[21]);
        assertEquals("nuova", MappaCryptoWallet.get("B")[21]);
    }

    @Test
    public void accodareVaACapoSoloDoveCeraGiaUnaNota() {
        movimentoConNota("A", "vecchia");
        movimentoConNota("B", "");

        assertEquals(2, Funzioni.ApplicaNotaAiMovimenti(List.of("A", "B"), "nuova", true));

        //Il ritorno a capo nel formato interno del campo 21 è <br>, non \n
        assertEquals("vecchia<br>nuova", MappaCryptoWallet.get("A")[21]);
        assertEquals("nuova", MappaCryptoWallet.get("B")[21],
                "su un movimento senza nota accodare deve dare lo stesso risultato di sostituire");
    }

    @Test
    public void accodareDueVolteImpilaLeNoteSenzaPerdereNulla() {
        movimentoConNota("A", "prima");

        Funzioni.ApplicaNotaAiMovimenti(List.of("A"), "seconda", true);
        Funzioni.ApplicaNotaAiMovimenti(List.of("A"), "terza", true);

        assertEquals("prima<br>seconda<br>terza", MappaCryptoWallet.get("A")[21]);
    }

    @Test
    public void accodareIlVuotoNonLasciaUnRitornoACapoAppeso() {
        movimentoConNota("A", "vecchia");

        assertEquals(0, Funzioni.ApplicaNotaAiMovimenti(List.of("A"), "", true),
                "senza testo nuovo non c'è nessuna modifica da contare");
        assertEquals("vecchia", MappaCryptoWallet.get("A")[21]);
    }

    @Test
    public void sostituireConIlVuotoCancellaLaNota() {
        //È l'unico modo di svuotare le note di più movimenti in un colpo, e l'utente lo chiede
        //esplicitamente scegliendo "Sostituisci" con l'area di testo vuota
        movimentoConNota("A", "vecchia");

        assertEquals(1, Funzioni.ApplicaNotaAiMovimenti(List.of("A"), "", false));
        assertEquals("", MappaCryptoWallet.get("A")[21]);
    }

    @Test
    public void ilConteggioIgnoraIMovimentiCheNonCambianoENonEsistono() {
        movimentoConNota("A", "identica");

        assertEquals(0, Funzioni.ApplicaNotaAiMovimenti(List.of("A", "IDinesistente"), "identica", false),
                "riscrivere la stessa nota non è una modifica, e un ID assente non deve far esplodere nulla");
    }

    @Test
    public void laDomandaAccodaSostituisciSiPoneSoloSeQualcunoHaGiaUnaNota() {
        movimentoConNota("A", "");
        movimentoConNota("B", "   ");
        //Un campo con soli separatori è quello che si ottiene salvando un'area con righe bianche:
        //non è testo dell'utente e accodarci sotto lascerebbe uno stacco casuale
        movimentoConNota("C", "<br><br>");

        assertEquals(0, Funzioni.ConteggioNoteNonVuote(List.of("A", "B", "C")));

        movimentoConNota("D", "questa c'è");
        movimentoConNota("E", "e anche questa");
        //Il conteggio, non solo il sì/no: è il numero mostrato nella domanda, e sostituire 1 nota su 5
        //o 5 su 5 sono decisioni diverse
        assertEquals(2, Funzioni.ConteggioNoteNonVuote(List.of("A", "B", "C", "D", "E")));
    }

    @Test
    public void notaVuotaRiconosceIlCampoAssente() {
        assertTrue(Funzioni.NotaVuota(null));
        assertTrue(Funzioni.NotaVuota(""));
        assertTrue(Funzioni.NotaVuota("<br>"));
        assertFalse(Funzioni.NotaVuota("x"));
    }

    @Test
    public void componiNotaNonDipendeDallaMappa() {
        assertEquals("nuova", Funzioni.ComponiNota("", "nuova", true));
        assertEquals("nuova", Funzioni.ComponiNota(null, "nuova", true));
        assertEquals("nuova", Funzioni.ComponiNota("vecchia", "nuova", false));
        assertEquals("vecchia<br>nuova", Funzioni.ComponiNota("vecchia", "nuova", true));
        assertEquals("", Funzioni.ComponiNota("vecchia", null, false),
                "una nota nulla in sostituzione diventa campo vuoto, non la stringa \"null\"");
    }
}
