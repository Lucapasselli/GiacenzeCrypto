package com.giacenzecrypto.giacenze_crypto;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * La riga "Somma" sotto il titolo di una colonna non deve essere tagliata: l'altezza dell'intestazione cresce quando
 * arriva la somma ({@link Tabelle#AdattaAltezzaIntestazione}), solo in crescita.
 */
class TabelleAltezzaIntestazioneTest {

    private static JTable tabella() {
        JTable t = new JTable(new DefaultTableModel(new Object[][]{{"1"}}, new String[]{"Valore"}));
        t.getTableHeader().setDefaultRenderer(Tabelle.Tabelle_creaNuovoHeaderRenderer(t, new HashMap<>(), null));
        return t;
    }

    /** Il caso visto a video nelle finestre LiFo e Deposito: altezza fissa di 25 e una somma sotto il titolo. */
    @Test
    void unaAltezzaFissaCresceQuandoArrivaLaSomma() {
        JTable t = tabella();
        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(200, 25));
        try {
            Tabelle.AdattaAltezzaIntestazione(t);
            assertEquals(25, h.getPreferredSize().height, "senza somme non cambia nulla");

            Tabelle.SommaColonne.put(t, Map.of(0, "10,00"));
            Tabelle.AdattaAltezzaIntestazione(t);
            assertTrue(h.getPreferredSize().height > 25, "con la somma serve una riga in più");
            assertEquals(t.getColumnModel().getTotalColumnWidth(), h.getPreferredSize().width, "la larghezza resta quella delle colonne");
        } finally {
            Tabelle.SommaColonne.remove(t);
        }
    }

    @Test
    void unaSommaVuotaNonFaCrescere() {
        JTable t = tabella();
        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(200, 25));
        try {
            Tabelle.SommaColonne.put(t, Map.of(0, ""));
            Tabelle.AdattaAltezzaIntestazione(t);
            assertEquals(25, h.getPreferredSize().height);
        } finally {
            Tabelle.SommaColonne.remove(t);
        }
    }

    /** Un'altezza già abbondante (la tabella movimenti ha 100) non viene mai ridotta. */
    @Test
    void unAltezzaGiaAbbondanteNonSiRiduce() {
        JTable t = tabella();
        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(200, 100));
        try {
            Tabelle.SommaColonne.put(t, Map.of(0, "10,00"));
            Tabelle.AdattaAltezzaIntestazione(t);
            assertEquals(100, h.getPreferredSize().height);
        } finally {
            Tabelle.SommaColonne.remove(t);
        }
    }

    /**
     * Senza altezza fissata Swing la ricava dalla sola prima colonna: una somma sulla seconda (il caso del dialogo Deposito,
     * dove "Valore" e' l'ultima colonna) non la alzerebbe da sola.
     */
    @Test
    void unaAltezzaAutomaticaCresceSeLaSommaNonEQuellaDellaPrimaColonna() {
        JTable t = new JTable(new DefaultTableModel(new Object[][]{{"a", "1"}}, new String[]{"Data", "Valore"}));
        t.getTableHeader().setDefaultRenderer(Tabelle.Tabelle_creaNuovoHeaderRenderer(t, new HashMap<>(), null));
        JTableHeader h = t.getTableHeader();
        try {
            int prima = h.getPreferredSize().height;
            Tabelle.SommaColonne.put(t, Map.of(0, "", 1, "10,00"));
            assertEquals(prima, h.getPreferredSize().height, "da sola Swing non la alza");
            Tabelle.AdattaAltezzaIntestazione(t);
            assertTrue(h.getPreferredSize().height > prima);
        } finally {
            Tabelle.SommaColonne.remove(t);
        }
    }

    /**
     * Il caso della finestra LiFo: la somma e' sulla prima colonna, quindi l'altezza automatica ricalcolata adesso la contiene
     * gia', ma l'intestazione e' ancora disegnata alta come prima.
     */
    @Test
    void siConfrontaConLAltezzaDisegnataNonConLaPreferitaDiAdesso() {
        JTable t = tabella();
        JTableHeader h = t.getTableHeader();
        try {
            int prima = h.getPreferredSize().height;
            h.setSize(100, prima);
            Tabelle.SommaColonne.put(t, Map.of(0, "10,00"));
            Tabelle.AdattaAltezzaIntestazione(t);
            assertTrue(h.getPreferredSize().height > prima);
            assertTrue(h.isPreferredSizeSet());
        } finally {
            Tabelle.SommaColonne.remove(t);
        }
    }
}
