package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Chiavi API, secret e passphrase nascoste a prima vista: nelle caselle e nelle tabelle si vedono solo
 * dei pallini, e si leggono in chiaro solo su richiesta (l'occhio dentro la casella, il pulsante
 * <i>Mostra chiavi</i> per le tabelle).
 * <p>Non è una protezione crittografica: le chiavi restano in chiaro nel database personale. Serve solo
 * a non mostrarle a chi guarda lo schermo, o in uno screenshot.
 */
public final class CampiSegreti {

    /** Quanti pallini mostra una credenziale nascosta: sempre gli stessi, per non rivelarne la lunghezza. */
    static final int LUNGHEZZA_MASCHERA = 12;

    /** Il carattere di FlatLaf per le password (il pallino), se il tema non ne definisce un altro. */
    private static final char CARATTERE_MASCHERA = '•';

    private CampiSegreti() {
    }

    /**
     * Aggiunge alle caselle l'occhio di FlatLaf: un clic mostra in chiaro il contenuto, un altro lo
     * nasconde (disegnato dal tema stesso, quindi segue anche il tema scuro).
     * @param campi le caselle delle credenziali
     */
    public static void AggiungiOcchio(JPasswordField... campi) {
        for (JPasswordField campo : campi) {
            campo.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        }
    }

    /**
     * La forma nascosta di una credenziale da mostrare in una tabella.
     * @param valore la credenziale
     * @return stringa vuota se la credenziale non c'è (una passphrase non usata deve restare vuota anche
     *         a colpo d'occhio), altrimenti {@link #LUNGHEZZA_MASCHERA} pallini
     */
    public static String Mascherata(Object valore) {
        if (valore == null || valore.toString().isBlank()) return "";
        Object carattere = javax.swing.UIManager.get("PasswordField.echoChar");
        char c = carattere instanceof Character ch ? ch : CARATTERE_MASCHERA;
        return String.valueOf(c).repeat(LUNGHEZZA_MASCHERA);
    }

    /**
     * Nasconde le colonne indicate di una tabella, finché {@code mostra} non risponde {@code true}.
     * Cambia solo il disegno della cella: il modello continua a contenere le credenziali vere, che
     * servono agli scaricamenti. Riusa il renderer già installato per le stringhe, così la colonna
     * mantiene le righe alternate e i colori di selezione delle altre.
     * @param tabella la tabella
     * @param mostra interrogato a ogni disegno: {@code true} per mostrare le credenziali in chiaro
     * @param colonne gli indici di colonna (nel modello delle colonne della vista, come stanno nel .form)
     */
    public static void NascondiColonne(JTable tabella, java.util.function.BooleanSupplier mostra, int... colonne) {
        TableCellRenderer base = tabella.getDefaultRenderer(Object.class);
        TableCellRenderer nascosto = (JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) -> {
            Object testo = mostra.getAsBoolean() ? value : Mascherata(value);
            Component c = base.getTableCellRendererComponent(t, testo, isSelected, hasFocus, row, col);
            //Il suggerimento non deve rimettere in chiaro quello che la cella nasconde
            if (c instanceof javax.swing.JComponent j) j.setToolTipText(null);
            return c;
        };
        for (int colonna : colonne) {
            tabella.getColumnModel().getColumn(colonna).setCellRenderer(nascosto);
        }
    }
}
