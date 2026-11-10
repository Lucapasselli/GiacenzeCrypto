package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Esito;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Voce;
import java.util.List;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

/** Il modello della finestra di scelta del ripristino, provato senza display. */
class GUI_RipristinoModificheTest {

    private static final List<Voce> VOCI = List.of(
            new Voce("Modifica puntuale", "A_1", Esito.APPLICABILE, "[15] Valore in euro: 10 -> 12", "2025/01/01 10:00", "Ex: 1 BTC"),
            new Voce("Rinomina", "B_1", Esito.CONFLITTO, "[15] diverso", "2025/01/02 10:00", "Ex: 2 ETH"),
            new Voce("Classificazione", "C_1", Esito.APPLICABILE, "2 originali", "2025/01/03 10:00", "Ex: 3 XRP"),
            new Voce("Unione", "D_1", Esito.GIA_APPLICATA, "gia' fatta"));

    private static boolean eseguibile(int i) {
        return VOCI.get(i).esito() == Esito.APPLICABILE;
    }

    @Test
    void diPartenzaSonoSpunteTutteLeApplicabiliEBasta() {
        DefaultTableModel m = GUI_RipristinoModifiche.CreaModello(VOCI, GUI_RipristinoModificheTest::eseguibile);

        assertEquals(4, m.getRowCount(), "tutte le voci sono nel modello");
        assertEquals(Set.of(0, 2), GUI_RipristinoModifiche.Selezionati(m));
    }

    @Test
    void sipuoSpuntareSoloLeApplicabili() {
        DefaultTableModel m = GUI_RipristinoModifiche.CreaModello(VOCI, GUI_RipristinoModificheTest::eseguibile);

        assertTrue(m.isCellEditable(0, GUI_RipristinoModifiche.COL_SCELTA));
        assertFalse(m.isCellEditable(1, GUI_RipristinoModifiche.COL_SCELTA), "un conflitto non si puo' applicare");
        assertFalse(m.isCellEditable(3, GUI_RipristinoModifiche.COL_SCELTA));
        assertFalse(m.isCellEditable(0, GUI_RipristinoModifiche.COL_TIPO), "il resto della riga non si modifica");
        assertEquals(Boolean.class, m.getColumnClass(GUI_RipristinoModifiche.COL_SCELTA));
    }

    @Test
    void togliendoUnaSpuntaLaSceltaCambia() {
        DefaultTableModel m = GUI_RipristinoModifiche.CreaModello(VOCI, GUI_RipristinoModificheTest::eseguibile);

        m.setValueAt(false, 2, GUI_RipristinoModifiche.COL_SCELTA);

        assertEquals(Set.of(0), GUI_RipristinoModifiche.Selezionati(m));
    }

    @Test
    void ogniRigaDiceQualeMovimentoEPerche() {
        DefaultTableModel m = GUI_RipristinoModifiche.CreaModello(VOCI, GUI_RipristinoModificheTest::eseguibile);

        assertEquals("2025/01/01 10:00", m.getValueAt(0, GUI_RipristinoModifiche.COL_DATA));
        assertEquals("Ex: 1 BTC  [A_1]", m.getValueAt(0, GUI_RipristinoModifiche.COL_MOVIMENTO));
        assertEquals("conflitto", m.getValueAt(1, GUI_RipristinoModifiche.COL_ESITO));
        assertEquals("[D_1]", m.getValueAt(3, GUI_RipristinoModifiche.COL_MOVIMENTO), "senza descrizione resta l'ID");
        assertEquals("già applicata", m.getValueAt(3, GUI_RipristinoModifiche.COL_ESITO));
    }

    @Test
    void ilPannelloDiDettaglioDiceChiEE_poiIlCompleto() {
        Voce v = new Voce("Modifica puntuale", "A_1", Esito.CONFLITTO, "breve", "2025-01-01 10:00", "Ex: 1 BTC",
                "[15] Valore\n    ora: 1\n    diventa: 2");

        String testo = GUI_RipristinoModifiche.TestoDettaglio(v);

        assertTrue(testo.contains("Tipo: Modifica puntuale"));
        assertTrue(testo.contains("Esito: conflitto"));
        assertTrue(testo.contains("Movimento: Ex: 1 BTC"));
        assertTrue(testo.contains("Data: 2025-01-01 10:00"));
        assertTrue(testo.contains("ID: A_1"));
        assertTrue(testo.endsWith("[15] Valore\n    ora: 1\n    diventa: 2"), "in fondo il dettaglio completo, non quello breve");
        assertFalse(testo.contains("breve"));
    }

    @Test
    void ilPannelloSaltaLeRigheNonNoteESenzaCompletoUsaIlDettaglio() {
        String testo = GUI_RipristinoModifiche.TestoDettaglio(new Voce("Unione", "D_1", Esito.GIA_APPLICATA, "gia' fatta"));

        assertFalse(testo.contains("Movimento:"));
        assertFalse(testo.contains("Data:"));
        assertTrue(testo.endsWith("gia' fatta"));
    }
}
