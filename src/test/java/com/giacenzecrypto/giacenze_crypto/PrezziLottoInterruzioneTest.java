package com.giacenzecrypto.giacenze_crypto;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test di {@link Prezzi#avviaSorveglianzaInterruzione}: il lotto prezzi in processo singolo deve fermarsi
 * entro un paio di secondi dal tasto Interrompi, invece di arrivare in fondo al lotto.
 *
 * <p>Il processo sorvegliato e' un {@code sleep}, non Node: quello che si verifica e' la sorveglianza, e
 * cosi' il test non dipende ne' da Node ne' dalla rete.
 */
class PrezziLottoInterruzioneTest {

    @BeforeEach
    @AfterEach
    void pulisci() {
        Interruzione.Azzera();
    }

    private static Process avviaSleep() throws Exception {
        assumeTrue(!System.getProperty("os.name", "").toLowerCase().contains("win"), "serve il comando sleep");
        return new ProcessBuilder("sleep", "30").start();
    }

    @Test
    void unInterrompiTerminaIlProcessoEntroPochiSecondi() throws Exception {
        Process p = avviaSleep();
        try {
            Interruzione.Apri();
            AtomicBoolean interrotto = Prezzi.avviaSorveglianzaInterruzione(p);
            Interruzione.Chiedi();
            assertTrue(p.waitFor(5, TimeUnit.SECONDS), "il processo doveva essere terminato");
            assertTrue(interrotto.get());
        } finally {
            p.destroyForcibly();
        }
    }

    /** Senza richiesta di interruzione il processo resta vivo: la sorveglianza non deve fare altro. */
    @Test
    void senzaInterrompiIlProcessoNonVieneToccato() throws Exception {
        Process p = avviaSleep();
        try {
            Interruzione.Apri();
            AtomicBoolean interrotto = Prezzi.avviaSorveglianzaInterruzione(p);
            assertFalse(p.waitFor(2500, TimeUnit.MILLISECONDS), "il processo non doveva essere terminato");
            assertFalse(interrotto.get());
        } finally {
            p.destroyForcibly();
        }
    }
}
