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
 * Smoke test dei due dialoghi per-gruppo della Fase 2 ({@link GUI_RiferimentoEsteroGruppo},
 * {@link GUI_PeriodiDetenzioneRW}): si costruiscono (coerenza {@code .form} /
 * {@code initComponents()}) e ricaricano i dati del gruppo. Saltato se headless.
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
    void riferimentoEsteroGruppo_siCostruisceECaricaIValoriCorrenti() throws Exception {
        DatabaseH2.Pers_GruppoRiferimento_Scrivi("Wallet 01",
                Principale_GruppiWalletRW.RIFERIMENTO_STATO, "092", "LU12345678", null);

        GUI_RiferimentoEsteroGruppo[] d = new GUI_RiferimentoEsteroGruppo[1];
        SwingUtilities.invokeAndWait(() -> d[0] = new GUI_RiferimentoEsteroGruppo("Wallet 01"));
        try {
            assertFalse(d[0].salvato);
            assertTrue(d[0].getTitle().contains("Wallet 01"));
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
    }

    @Test
    void periodiDetenzioneRW_siCostruisceECaricaIPeriodi() throws Exception {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 02", java.util.Arrays.asList(
                new String[]{"FIAT", "1", "2024-01-01", "2024-12-31", "", "", "", "", "", ""},
                new String[]{"CRYPTO", "1", "", "", "", "", "", "", "", ""}));

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
            assertEquals(10, m.getColumnCount());
            assertEquals(2, m.getRowCount(), "i due periodi salvati devono comparire");
        } finally {
            SwingUtilities.invokeAndWait(d[0]::dispose);
        }
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
