package com.giacenzecrypto.giacenze_crypto;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copre l'apertura della finestra dei log dell'opzione {@code --debug}, che
 * {@code Giacenze_Crypto.apriFinestraLog()} costruisce e mostra dentro un {@code invokeAndWait}
 * (residuo della voce A6 in Documentazione/Analisi_Bug_Criticita.md: era l'unico pezzo di GUI
 * rimasto fuori dall'Event Dispatch Thread). Nessun altro test tocca il percorso {@code --debug},
 * la cui unica verifica era finora l'avvio manuale dell'applicazione.
 *
 * Il rischio concreto è la modalità del dialogo: {@code Download} nasce <b>modale</b>, e senza la
 * chiamata a {@code NoModale()} il {@code setVisible} dentro l'{@code invokeAndWait} bloccherebbe
 * l'EDT a tempo indefinito, piantando l'avvio. È ciò che verifica il secondo test.
 *
 * Il primo test fotografa invece <i>quando</i> viene installato il dirottamento di
 * {@code System.out}/{@code System.err} sui pannelli della finestra: non lo fa {@code setVisible}
 * ma {@code Download.formWindowOpened}, cioè il gestore di {@code WINDOW_OPENED}. Il test lo
 * osserva tramite {@code Principale.InterrompiCiclo}, che quel gestore riporta a {@code false}.
 * <b>Non</b> dimostra la necessità di una barriera sulla coda dell'EDT: era stata aggiunta e poi
 * tolta, perché il dirottamento diventa effettivo solo quando {@code LoggerGC.init()} sostituisce
 * {@code System.out}, e quello viene eseguito comunque <i>dopo</i>, sul main thread.
 *
 * Entrambi aprono una finestra vera, quindi si auto-saltano su macchine senza display.
 */
class FinestraLogDebugTest {

    @BeforeEach
    void richiedeUnDisplay() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Serve un display: il test apre una finestra Swing reale");
    }

    @AfterEach
    void ripristinaGliStream() {
        // La finestra dirotta System.out/err su propri pannelli: se non lo si annulla, l'output
        // dei test successivi finirebbe su una finestra ormai chiusa.
        LoggerGC.disableTextPaneOut();
        LoggerGC.disableTextPaneErr();
    }

    @Test
    void allApertura_ilGestoreDellaFinestraInstallaIlDirottamentoDegliStream() throws Exception {
        Principale.InterrompiCiclo = true;   // sentinella: formWindowOpened la riporta a false

        Download[] finestra = new Download[1];
        SwingUtilities.invokeAndWait(() -> {
            Download d = new Download();
            d.Titolo("Test finestra log");
            d.NascondiInterrompi();
            d.NascondiBarra();
            d.NoModale();
            finestra[0] = d;
            d.setVisible(true);
        });


        try {
            assertFalse(Principale.InterrompiCiclo,
                    "il gestore di apertura della finestra deve aver girato: è lui a dirottare "
                    + "System.out/err sui pannelli della finestra dei log");
        } finally {
            SwingUtilities.invokeAndWait(() -> finestra[0].dispose());
        }
    }

    @Test
    void finestraNonModale_setVisibleNonBlocca() throws Exception {
        // Se NoModale() non venisse chiamata la finestra resterebbe modale (Download la crea
        // modale in initComponents) e setVisible bloccherebbe l'EDT per sempre: l'avvio con
        // --debug si pianterebbe. Il test fallisce per timeout se accadesse.
        Download[] finestra = new Download[1];
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(20), () -> {
            SwingUtilities.invokeAndWait(() -> {
                Download d = new Download();
                d.NascondiInterrompi();
                d.NascondiBarra();
                d.NoModale();
                finestra[0] = d;
                d.setVisible(true);
            });
        });
        SwingUtilities.invokeAndWait(() -> finestra[0].dispose());
    }
}
