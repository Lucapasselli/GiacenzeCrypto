package com.giacenzecrypto.giacenze_crypto;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test di {@link GUI_AnagraficaExchange}: il dialog si costruisce (coerenza
 * {@code .form} / {@code initComponents()}), la tabella si popola dal seed di
 * {@code EXCHANGE_ANAGRAFICA} e ha le 7 colonne attese. Saltato se headless.
 */
class GUI_AnagraficaExchangeTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Ambiente headless: smoke test GUI saltato");
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        if (DatabaseH2.connectionPersonale != null) {
            DatabaseH2.connection.close();
            DatabaseH2.connectionPersonale.close();
            DatabaseH2.connectionPrezzi.close();
        }
    }

    @Test
    void siCostruisceEMostraIlSeed() throws Exception {
        GUI_AnagraficaExchange[] d = new GUI_AnagraficaExchange[1];
        SwingUtilities.invokeAndWait(() -> d[0] = new GUI_AnagraficaExchange());
        try {
            DefaultTableModel m = null;
            for (java.awt.Component c : trovaTabelle(d[0])) {
                if (c instanceof javax.swing.JTable t) {
                    m = (DefaultTableModel) t.getModel();
                }
            }
            assertNotNull(m, "la JTable del dialog non è stata trovata");
            assertEquals(7, m.getColumnCount());
            assertEquals(DatabaseH2.Pers_ExchangeAnagrafica_LeggiTabella().size(), m.getRowCount());
            assertTrue(m.getRowCount() >= 10, "il seed dei nomi noti deve essere visibile");
            assertFalse(m.isCellEditable(0, 0), "la colonna id non è editabile");
            assertTrue(m.isCellEditable(0, 2), "la colonna stato estero è editabile");
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
    }

    private static java.util.List<java.awt.Component> trovaTabelle(java.awt.Container root) {
        java.util.List<java.awt.Component> out = new java.util.ArrayList<>();
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JTable) {
                out.add(c);
            }
            if (c instanceof java.awt.Container cont) {
                out.addAll(trovaTabelle(cont));
            }
        }
        return out;
    }
}
