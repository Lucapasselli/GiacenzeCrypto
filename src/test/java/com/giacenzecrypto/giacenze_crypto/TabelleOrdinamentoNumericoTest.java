package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.List;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Le colonne numeriche di un model {@code Object} (tabella movimenti di "Dettagli x Tipologia") si ordinano per valore. */
class TabelleOrdinamentoNumericoTest {

    private static JTable tabella(Object... valori) {
        DefaultTableModel m = new DefaultTableModel(new String[]{"N"}, 0);
        for (Object v : valori) {
            m.addRow(new Object[]{v});
        }
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        return t;
    }

    private static String ordinata(JTable t, SortOrder o) {
        t.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, o)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.getRowCount(); i++) {
            sb.append(t.getValueAt(i, 0)).append(' ');
        }
        return sb.toString().trim();
    }

    @Test
    void senzaComparatore_ordinaComeTesto() {
        JTable t = tabella(new BigDecimal("9"), new BigDecimal("100"), new BigDecimal("-5"));
        assertEquals("100 -5 9", ordinata(t, SortOrder.ASCENDING), "per testo \"100\" precede \"9\"");
    }

    @Test
    void conComparatore_ordinaPerValore_ancheStringheENotazioneScientifica() {
        JTable t = tabella(new BigDecimal("9"), "100", new BigDecimal("-5"), "2.5E-9", null, "n.d.");
        Tabelle.Tabelle_OrdinamentoNumerico(t, 0);
        // i null li mette per primi lo sorter di Swing, prima del comparatore
        assertEquals("null -5 2.5E-9 9 100 n.d.", ordinata(t, SortOrder.ASCENDING));
    }
}
