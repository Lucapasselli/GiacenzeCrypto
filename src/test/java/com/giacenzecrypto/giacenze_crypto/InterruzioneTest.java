package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Interruzione}, cioè della proprietà per cui esiste: la richiesta di interruzione
 * <b>non è appiccicosa</b>.
 *
 * <p>È la differenza con {@link Principale#InterrompiCiclo}, che dopo un'importazione andata a buon fine
 * resta acceso — {@code Download.formWindowClosed} scatta su ogni chiusura della finestra di avanzamento,
 * anche quella normale di fine lavoro — e che quindi non può essere controllato dentro la fase di
 * scaricamento prezzi senza disattivarla in silenzio per tutta la sessione.
 */
class InterruzioneTest {

    @BeforeEach
    @AfterEach
    void pulisci() {
        Interruzione.Azzera();
    }

    /** Fuori da un'operazione aperta una richiesta non vale nulla: è tutta la ragione d'essere della classe. */
    @Test
    void fuoriDaUnOperazioneLaRichiestaNonVale() {
        Interruzione.Chiedi();
        assertFalse(Interruzione.Richiesta());
        assertFalse(Interruzione.OperazioneAperta());
    }

    @Test
    void dentroUnOperazioneLaRichiestaVale() {
        Interruzione.Apri();
        assertFalse(Interruzione.Richiesta());
        Interruzione.Chiedi();
        assertTrue(Interruzione.Richiesta());
        Interruzione.Chiudi();
        assertFalse(Interruzione.Richiesta());
    }

    /**
     * Il caso che il vecchio flag sbagliava: un'operazione interrotta non deve lasciare nulla acceso per
     * quella dopo, altrimenti la seconda importazione della sessione non scaricherebbe alcun prezzo.
     */
    @Test
    void unaRichiestaNonSopravviveAllOperazioneCheLHaRicevuta() {
        Interruzione.Apri();
        Interruzione.Chiedi();
        assertTrue(Interruzione.Richiesta());
        Interruzione.Chiudi();

        Interruzione.Apri();
        assertFalse(Interruzione.Richiesta(), "la richiesta della corsa precedente non deve valere qui");
        Interruzione.Chiudi();
    }

    /** Un'operazione che ne contiene un'altra non deve chiudere lo scope esterno prima del tempo. */
    @Test
    void loScopeSiAnnida() {
        Interruzione.Apri();
        Interruzione.Apri();
        Interruzione.Chiedi();
        Interruzione.Chiudi();
        assertTrue(Interruzione.Richiesta(), "lo scope esterno è ancora aperto");
        Interruzione.Chiudi();
        assertFalse(Interruzione.Richiesta());
    }

    /** Una Chiudi() di troppo non deve mandare il conteggio sotto zero e rendere impossibile riaprire. */
    @Test
    void unaChiusuraDiTroppoNonSbilanciaIlConteggio() {
        Interruzione.Chiudi();
        Interruzione.Chiudi();
        assertFalse(Interruzione.OperazioneAperta());

        Interruzione.Apri();
        Interruzione.Chiedi();
        assertTrue(Interruzione.Richiesta());
        Interruzione.Chiudi();
        assertFalse(Interruzione.OperazioneAperta());
    }
}
