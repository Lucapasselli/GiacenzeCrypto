package com.giacenzecrypto.giacenze_crypto;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Download#EseguiBloccante}: il dialogo modale di avanzamento si chiude <b>sempre</b>, anche se il lavoro lancia
 * un'eccezione, un {@link Error} o finisce prima che il dialogo compaia. Prima dell'helper, ogni punto dell'applicazione
 * chiudeva la finestra solo in coda al thread: un NullPointerException a meta' lasciava una finestra modale che nessuno
 * chiudeva piu' (applicazione congelata, traccia solo nel log). Ogni prova ha un timeout: un blocco e' un fallimento, non
 * un test che non finisce mai.
 *
 * <p>Serve uno schermo: saltato se headless, e saltato anche sul desktop Wayland ({@code WAYLAND_DISPLAY} presente), dove Java
 * passa da XWayland e ogni tanto si inceppa da solo (17/11/2026: tre prove bloccate nella suite completa, con un errore interno di
 * XToolkit "Window must not be zero" a meta' chiusura, mai riprodotte da sole), oltre a riempire il desktop di finestre. Si fa
 * girare sul display virtuale ({@code nocommit/StrumentiTest/avvia-display-virtuale.sh}, poi
 * {@code env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE DISPLAY=:99 ./mvnw test -Dtest=DownloadEseguiBloccanteTest}), oppure sul
 * desktop con {@code -Dgui.desktop=true}.
 */
class DownloadEseguiBloccanteTest {

    private static final Duration LIMITE = Duration.ofSeconds(30);

    private static String temaPrima;

    @BeforeAll
    static void serveLoSchermo() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Ambiente headless: prova del dialogo modale saltata");
        String wayland = System.getenv("WAYLAND_DISPLAY");
        Assumptions.assumeTrue(wayland == null || wayland.isBlank() || Boolean.getBoolean("gui.desktop"),
                "Desktop Wayland: prova del dialogo modale saltata, va lanciata sul display virtuale (vedi Javadoc)");
        //I messaggi leggono il tema da Principale.tema, che l'applicazione imposta all'avvio
        temaPrima = Principale.tema;
        if (Principale.tema == null) {
            Principale.tema = "chiaro";
        }
    }

    @AfterAll
    static void ripristinaIlTema() {
        Principale.tema = temaPrima;
    }

    /** Fa girare l'EDT a vuoto: le chiusure richieste con invokeLater sono arrivate. */
    private static void svuotaEDT() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    /** Un thread che chiude ogni dialogo visibile che non sia {@code escluso}, finche' non viene fermato. */
    private static class Chiusore extends Thread {

        private final Window escluso;
        volatile boolean fermo = false;
        final AtomicInteger chiusi = new AtomicInteger();

        Chiusore(Window escluso) {
            super("test-chiusore");
            this.escluso = escluso;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!fermo) {
                for (Window w : Window.getWindows()) {
                    if (w != escluso && w instanceof Dialog d && d.isShowing() && !(w instanceof Download)) {
                        chiusi.incrementAndGet();
                        SwingUtilities.invokeLater(d::dispose);
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }

    @Test
    void lavoroNormale_eseguito_dialogoChiuso_nessunErrore() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            Download dl = new Download();
            AtomicBoolean eseguito = new AtomicBoolean();
            Throwable errore = dl.EseguiBloccante(() -> {
                pausa(300); //abbastanza perche' il dialogo compaia davvero
                eseguito.set(true);
            });
            svuotaEDT();
            assertNull(errore);
            assertTrue(eseguito.get());
            assertFalse(dl.isDisplayable(), "il dialogo deve essere chiuso");
            assertFalse(Principale.InterrompiCiclo, "un fine lavoro normale non e' un'interruzione");
            assertFalse(Interruzione.Richiesta(), "un fine lavoro normale non e' un'interruzione");
        });
    }

    @Test
    void nullPointerException_dialogoChiusoComunque_erroreRestituito() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            Download dl = new Download();
            Throwable errore = dl.EseguiBloccante(() -> {
                pausa(200);
                String s = null;
                s.length(); //NullPointerException voluta
            });
            svuotaEDT();
            assertInstanceOf(NullPointerException.class, errore);
            assertFalse(dl.isDisplayable(), "con un'eccezione il dialogo non deve restare aperto");
        });
    }

    @Test
    void errorNonEccezione_dialogoChiusoComunque() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            Download dl = new Download();
            Throwable errore = dl.EseguiBloccante(() -> {
                pausa(200);
                throw new NoClassDefFoundError("classe di prova");
            });
            svuotaEDT();
            assertInstanceOf(NoClassDefFoundError.class, errore);
            assertFalse(dl.isDisplayable());
        });
    }

    @Test
    void lavoroIstantaneo_finisceInAnticipo_dialogoNonResta() throws Exception {
        //La gara: il thread finisce prima che l'EDT arrivi a setVisible(true). Con lo schema a mano (dispose a fine
        //thread) la finestra veniva mostrata dopo e nessuno la chiudeva.
        assertTimeoutPreemptively(LIMITE, () -> {
            for (int i = 0; i < 150; i++) {
                Download dl = new Download();
                Throwable errore = dl.EseguiBloccante(() -> {
                });
                assertNull(errore);
                svuotaEDT();
                assertFalse(dl.isDisplayable(), "giro " + i + ": dialogo rimasto aperto");
            }
        });
    }

    /** Un dialogo che si fa vedere solo dopo che il lavoro e' finito: la gara resa deterministica. */
    private static class DownloadInRitardo extends Download {

        private final boolean aspettaLaFine;

        DownloadInRitardo(boolean aspettaLaFine) {
            this.aspettaLaFine = aspettaLaFine;
        }

        @Override
        public void setVisible(boolean b) {
            if (b && aspettaLaFine) {
                try {
                    Thread t = thread;
                    if (t != null) {
                        t.join(5000);
                    }
                    Thread.sleep(100); //e il tempo che invokeLater/dispose arrivino prima della comparsa
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            super.setVisible(b);
        }
    }

    @Test
    void gara_ilDialogoCompareDopoLaFineDelLavoro_nonResta() throws Exception {
        //Lo schema a mano (new Thread + setVisible + dispose a fine thread) qui restava appeso per sempre: la finestra
        //modale compariva dopo il dispose e nessuno la chiudeva piu'
        assertTimeoutPreemptively(LIMITE, () -> {
            DownloadInRitardo dl = new DownloadInRitardo(true);
            Throwable errore = dl.EseguiBloccante(() -> pausa(150));
            svuotaEDT();
            assertNull(errore);
            assertFalse(dl.isDisplayable());
        });
    }

    @Test
    void gara_ilDialogoCompareDopoLErrore_nonResta() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            DownloadInRitardo dl = new DownloadInRitardo(true);
            Throwable errore = dl.EseguiBloccante(() -> {
                pausa(150);
                throw new IllegalStateException("prima della comparsa");
            });
            svuotaEDT();
            assertInstanceOf(IllegalStateException.class, errore);
            assertFalse(dl.isDisplayable());
        });
    }

    /** Un dialogo il cui primo dispose fallisce a meta', come l'errore interno di XToolkit visto il 17/11/2026. */
    private static class DownloadConDisposeCheFallisce extends Download {

        private final AtomicInteger tentativi = new AtomicInteger();

        @Override
        public void dispose() {
            if (tentativi.incrementAndGet() == 1) {
                throw new IllegalArgumentException("Window must not be zero");
            }
            super.dispose();
        }
    }

    @Test
    void unaChiusuraCheFallisceSiRiprova_ilDialogoNonResta() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            DownloadConDisposeCheFallisce dl = new DownloadConDisposeCheFallisce();
            Throwable errore = dl.EseguiBloccante(() -> pausa(200));
            svuotaEDT();
            assertNull(errore);
            assertFalse(dl.isDisplayable(), "il secondo tentativo deve chiudere il dialogo");
            assertTrue(dl.tentativi.get() >= 2);
        });
    }

    @Test
    void lavoroIstantaneoCheFallisce_dialogoNonResta() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            for (int i = 0; i < 150; i++) {
                Download dl = new Download();
                Throwable errore = dl.EseguiBloccante(() -> {
                    throw new IllegalStateException("subito");
                });
                assertInstanceOf(IllegalStateException.class, errore);
                svuotaEDT();
                assertFalse(dl.isDisplayable(), "giro " + i + ": dialogo rimasto aperto");
            }
        });
    }

    @Test
    void chiamatoDallEDT_comeNellApplicazione() throws Exception {
        //Nell'applicazione il gestore del pulsante gira sull'EDT: setVisible(true) modale fa girare un ciclo annidato
        assertTimeoutPreemptively(LIMITE, () -> {
            Throwable[] errore = new Throwable[1];
            boolean[] eseguito = new boolean[1];
            Download[] dl = new Download[1];
            SwingUtilities.invokeAndWait(() -> {
                dl[0] = new Download();
                errore[0] = dl[0].EseguiBloccante(() -> {
                    pausa(200);
                    eseguito[0] = true;
                    throw new UnsupportedOperationException("dopo il lavoro");
                });
            });
            svuotaEDT();
            assertInstanceOf(UnsupportedOperationException.class, errore[0]);
            assertTrue(eseguito[0]);
            assertFalse(dl[0].isDisplayable());
        });
    }

    @Test
    void dallEDT_lavoroIstantaneo_nessunBlocco() throws Exception {
        assertTimeoutPreemptively(LIMITE, () -> {
            for (int i = 0; i < 50; i++) {
                Download[] dl = new Download[1];
                SwingUtilities.invokeAndWait(() -> {
                    dl[0] = new Download();
                    dl[0].EseguiBloccante(() -> {
                    });
                });
                svuotaEDT();
                assertFalse(dl[0].isDisplayable(), "giro " + i);
            }
        });
    }

    @Test
    void dialogoChiusoDalLavoroStesso_poiUnSecondoDialogoModale_nessunDeadlock() throws Exception {
        //Il caso della scelta del prezzo: il lavoro chiude da se' l'attesa e apre un dialogo modale che ha bisogno
        //dell'EDT, e resta li' finche' l'utente non lo chiude. L'helper deve tornare comunque (l'attesa e' chiusa) e
        //non aspettare il thread sull'EDT: con un join l'EDT sarebbe bloccato e il secondo dialogo non comparirebbe mai.
        assertTimeoutPreemptively(LIMITE, () -> {
            Download dl = new Download();
            Throwable[] errore = new Throwable[1];
            SwingUtilities.invokeAndWait(() -> errore[0] = dl.EseguiBloccante(() -> {
                dl.ChiudiFinestra();
                JDialog secondo = new JDialog((Window) null, "secondo", Dialog.ModalityType.APPLICATION_MODAL);
                secondo.setSize(200, 100);
                secondo.setVisible(true); //torna quando qualcuno lo chiude
            }));
            //Siamo tornati sull'EDT libero, e il lavoro e' ancora dentro il secondo dialogo
            assertNull(errore[0]);
            assertTrue(dl.thread.isAlive(), "il lavoro e' ancora nel secondo dialogo modale");
            Chiusore chiusore = new Chiusore(null);
            chiusore.start();
            try {
                dl.thread.join(10000);
                assertFalse(dl.thread.isAlive(), "il secondo dialogo, servito dall'EDT, si e' chiuso e il lavoro e' finito");
                assertTrue(chiusore.chiusi.get() >= 1, "il secondo dialogo modale doveva comparire");
            } finally {
                chiusore.fermo = true;
            }
        });
    }

    @Test
    void conAvviso_erroreMostratoEDialogoChiuso() throws Exception {
        Chiusore chiusore = new Chiusore(null);
        chiusore.start();
        try {
            assertTimeoutPreemptively(LIMITE, () -> {
                Download dl = new Download();
                Throwable errore = dl.EseguiBloccante(() -> {
                    pausa(200);
                    throw new IllegalArgumentException("<b>dato non valido</b> & altro");
                }, "la prova", null);
                svuotaEDT();
                assertInstanceOf(IllegalArgumentException.class, errore);
                assertFalse(dl.isDisplayable());
                assertEquals(1, chiusore.chiusi.get(), "doveva comparire un solo messaggio d'errore");
            });
        } finally {
            chiusore.fermo = true;
        }
    }

    @Test
    void conAvviso_lavoroRiuscito_nessunMessaggio() throws Exception {
        Chiusore chiusore = new Chiusore(null);
        chiusore.start();
        try {
            assertTimeoutPreemptively(LIMITE, () -> {
                Download dl = new Download();
                Throwable errore = dl.EseguiBloccante(() -> pausa(200), "la prova", null);
                pausa(400);
                assertNull(errore);
                assertEquals(0, chiusore.chiusi.get(), "nessun avviso se il lavoro riesce");
            });
        } finally {
            chiusore.fermo = true;
        }
    }

    @Test
    void erroreScrittoNelLogConLoStackTrace() throws Exception {
        PrintStream vecchio = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, "UTF-8"));
        try {
            assertTimeoutPreemptively(LIMITE, () -> {
                new Download().EseguiBloccante(() -> {
                    throw new IllegalStateException("guasto di prova nel lavoro");
                });
            });
        } finally {
            System.setErr(vecchio);
        }
        String log = buffer.toString("UTF-8");
        assertTrue(log.contains("guasto di prova nel lavoro"), log);
        assertTrue(log.contains("IllegalStateException"), "serve la classe dell'errore");
        assertTrue(log.contains("at com.giacenzecrypto") || log.contains("\tat "), "serve lo stack trace, non solo il messaggio: " + log);
    }

    @Test
    void scriviErrore_conMessaggioNullo_citaLaClasse() throws Exception {
        //Una NullPointerException puo' non avere messaggio: il log diceva solo "null"
        PrintStream vecchio = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, "UTF-8"));
        try {
            LoggerGC.ScriviErrore(new RuntimeException((String) null));
        } finally {
            System.setErr(vecchio);
        }
        String log = buffer.toString("UTF-8");
        assertTrue(log.contains("RuntimeException"), log);
        assertFalse(log.trim().endsWith("\nnull"), log);
    }

    @Test
    void threadAssociatoPerFineThread() throws Exception {
        //FineThread() dei punti che interrogano il dialogo per sapere se il lavoro e' finito si appoggia al thread
        assertTimeoutPreemptively(LIMITE, () -> {
            Download dl = new Download();
            assertNull(dl.thread);
            dl.EseguiBloccante(() -> pausa(100));
            assertNotNull(dl.thread, "l'helper associa il thread, come SetThread");
            dl.thread.join(5000);
            assertFalse(dl.thread.isAlive());
        });
    }

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
