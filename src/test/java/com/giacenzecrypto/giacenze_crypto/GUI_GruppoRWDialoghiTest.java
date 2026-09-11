package com.giacenzecrypto.giacenze_crypto;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test di {@link GUI_PeriodiDetenzioneRW} : si costruisce (coerenza {@code .form} /
 * {@code initComponents()}) e ricarica i periodi del gruppo. Saltato se headless.
 */
class GUI_GruppoRWDialoghiTest {

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
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @Test
    void periodiDetenzioneRW_siCostruisceECaricaIPeriodi() throws Exception {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 02", java.util.Arrays.asList(
                riga("FIAT", "1", "2024-01-01", "2024-12-31"),
                riga("CRYPTO", "1", "", "")));

        GUI_PeriodiDetenzioneRW[] d = new GUI_PeriodiDetenzioneRW[1];
        SwingUtilities.invokeAndWait(() -> d[0] = new GUI_PeriodiDetenzioneRW("Wallet 02"));
        try {
            DefaultTableModel m = null;
            for (java.awt.Component c : figli(d[0])) {
                if (c instanceof JTable t) {
                    m = (DefaultTableModel) t.getModel();
                }
            }
            assertNotNull(m);
            assertEquals(14, m.getColumnCount());
            assertEquals(2, m.getRowCount(), "i due periodi salvati devono comparire");
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
    }

    @Test
    void modificaPeriodoDetenzione_siCostruisceECaricaLaRiga() throws Exception {
        String[] fiat = riga("FIAT", "2", "2025-06-20", "");
        fiat[Principale_GruppiWalletRW.COL_STATO_ESTERO] = "092";
        fiat[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = "LU36476644";
        fiat[Principale_GruppiWalletRW.COL_IDENT_ISEE] = "E12345";
        fiat[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = Principale_GruppiWalletRW.MOD_SOLO_RESIDUO;

        GUI_ModificaPeriodoDetenzione[] d = new GUI_ModificaPeriodoDetenzione[1];
        SwingUtilities.invokeAndWait(() -> d[0] = new GUI_ModificaPeriodoDetenzione(null, fiat, 0,
                Principale_GruppiWalletRW.BOLLO_NO));
        try {
            assertFalse(d[0].confermato);
            assertTrue(d[0].getTitle().toLowerCase().contains("periodo"));
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
    }

    /**
     * Costruttore con contesto (elenco + indice della riga in modifica) : è quello che il dialogo dei
     * periodi usa da quando la validazione è incrociata. Se il tipo cambia e il progressivo collide,
     * il campo si sposta da solo su uno libero.
     */
    @Test
    void modificaPeriodoDetenzione_conContesto_siCostruisce() throws Exception {
        java.util.List<String[]> tutti = java.util.Arrays.asList(
                riga("CRYPTO", "1", "", ""),
                riga("FIAT", "1", "2024-01-01", "2024-12-31"));

        GUI_ModificaPeriodoDetenzione[] d = new GUI_ModificaPeriodoDetenzione[1];
        SwingUtilities.invokeAndWait(() -> d[0] = new GUI_ModificaPeriodoDetenzione(null, null, 2,
                Principale_GruppiWalletRW.BOLLO_NO, tutti, -1));
        try {
            assertFalse(d[0].confermato);
            assertTrue(d[0].getTitle().toLowerCase().contains("nuovo"));
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
    }

    private static String[] riga(String tipo, String prog, String di, String df) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = tipo;
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = prog;
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = di;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = df;
        return r;
    }

    private static java.util.List<java.awt.Component> figli(java.awt.Container root) {
        java.util.List<java.awt.Component> out = new java.util.ArrayList<>();
        for (java.awt.Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof java.awt.Container cont) {
                out.addAll(figli(cont));
            }
        }
        return out;
    }
}
