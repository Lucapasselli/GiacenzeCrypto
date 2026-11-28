package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link Funzioni#SenzaRete}: il lavoro della Dashboard sui valori dell'anno non deve far partire scaricamenti né aspettare il ritorno della
 * rete, e la cosa vale solo per il suo thread e solo finché dura.
 */
class FunzioniSenzaReteTest {

    @AfterEach
    void chiudeGliScopeRimasti() {
        while (AttesaConnessione.ScopeAperto()) AttesaConnessione.Chiudi();
    }

    @Test
    void dentroLaRetePareAssenteEFuoriTornaNormale() {
        assertFalse(Funzioni.SenzaReteSuQuestoThread());
        assertTrue(Funzioni.SenzaRete(Funzioni::SenzaReteSuQuestoThread));
        assertFalse(Funzioni.SenzaRete(Funzioni::CeConnessioneInternet));
        assertFalse(Funzioni.SenzaReteSuQuestoThread(), "alla fine il thread e' di nuovo normale");
    }

    @Test
    void ilRipristinoSopravvivealleEccezioniEAgliAnnidamenti() {
        assertThrows(IllegalStateException.class, () -> Funzioni.SenzaRete(() -> {
            throw new IllegalStateException("prova");
        }));
        assertFalse(Funzioni.SenzaReteSuQuestoThread());
        Funzioni.SenzaRete(() -> {
            Funzioni.SenzaRete(() -> null);
            assertTrue(Funzioni.SenzaReteSuQuestoThread(), "uscendo dal lavoro interno si resta nell'esterno");
            return null;
        });
        assertFalse(Funzioni.SenzaReteSuQuestoThread());
    }

    @Test
    void conUnaImportazioneApertaIlLavoroSenzaReteNonAspetta() {
        AttesaConnessione.Apri(null);
        long t = System.nanoTime();
        assertFalse(Funzioni.SenzaRete(AttesaConnessione::Attendi));
        assertTrue((System.nanoTime() - t) < 5_000_000_000L, "niente attesa: senza rete e' una scelta, non un guasto");
    }

    @Test
    void ilSenzaReteDiUnThreadNonToccaGliAltri() throws Exception {
        AtomicBoolean altro = new AtomicBoolean(true);
        Funzioni.SenzaRete(() -> {
            Thread t = new Thread(() -> altro.set(Funzioni.SenzaReteSuQuestoThread()));
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertEquals(false, altro.get());
    }
}
