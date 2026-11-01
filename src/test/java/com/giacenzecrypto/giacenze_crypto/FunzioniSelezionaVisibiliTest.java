package com.giacenzecrypto.giacenze_crypto;

import java.util.Arrays;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Voce "Seleziona tutte le righe visibili" del menu contestuale delle tabelle: con un filtro attivo seleziona solo le righe
 * che la tabella mostra, e si propone solo dove ha senso (selezione multipla, almeno due righe nella vista).
 */
class FunzioniSelezionaVisibiliTest {

    private static JTable TabellaFiltrabile() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{
            {"BTC", "1"}, {"ETH", "2"}, {"BTC", "3"}, {"SOL", "4"}, {"BTC", "5"}}, new Object[]{"Moneta", "N"});
        JTable t = new JTable(m);
        t.setRowSorter(new TableRowSorter<>(m));
        return t;
    }

    @Test
    void conUnFiltroSelezionaSoloLeRigheVisibili() {
        JTable t = TabellaFiltrabile();
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) t.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("BTC", 0));

        assertTrue(Funzioni.PuoSelezionareTutteLeRighe(t));
        t.selectAll();

        assertEquals(3, t.getSelectedRowCount());
        int[] modello = Tabelle.Funzioni_getRigheSelezionate(t);
        Arrays.sort(modello);
        assertArrayEquals(new int[]{0, 2, 4}, modello, "le righe ETH e SOL, nascoste dal filtro, restano fuori");
    }

    @Test
    void nonSiProponeDoveNonHaSenso() {
        JTable t = TabellaFiltrabile();
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) t.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("SOL", 0));
        assertFalse(Funzioni.PuoSelezionareTutteLeRighe(t), "una sola riga visibile");

        sorter.setRowFilter(null);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertFalse(Funzioni.PuoSelezionareTutteLeRighe(t), "selezione singola");
    }

    @Test
    void laVoceHaLIconaNelJar() {
        assertNotNull(Funzioni.class.getResource("/Images/24_SelezionaTutto.png"));
    }
}
