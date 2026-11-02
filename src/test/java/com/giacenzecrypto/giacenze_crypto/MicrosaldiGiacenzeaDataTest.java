package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L'evidenziazione in giallo dei microsaldi di "Giacenze a data": le soglie
 * ({@link Principale_GiacenzeaData_Microsaldi.Soglie}) e come le due tabelle le applicano, compresa
 * l'eccezione del saldo che le API confermano.
 */
class MicrosaldiGiacenzeaDataTest {

    private static final Principale_GiacenzeaData_Microsaldi.Soglie SOLO_EURO =
            new Principale_GiacenzeaData_Microsaldi.Soglie(new BigDecimal("0.10"), null);
    private static final Principale_GiacenzeaData_Microsaldi.Soglie SOLO_QTA =
            new Principale_GiacenzeaData_Microsaldi.Soglie(null, new BigDecimal("0.001"));

    @BeforeEach
    void tema() {
        Principale.tema = "Chiaro";
    }

    // ---------- le soglie ----------

    @Test
    void sogliaInEuroSiApplicaSoloSeIlPrezzoCe() {
        assertTrue(SOLO_EURO.IsMicro("0.00000012", new BigDecimal("0.01")));
        assertTrue(SOLO_EURO.IsMicro("5", new BigDecimal("0.10")), "il valore uguale alla soglia è un microsaldo");
        assertFalse(SOLO_EURO.IsMicro("5", new BigDecimal("0.11")));
        assertFalse(SOLO_EURO.IsMicro("0.00000012", null), "senza prezzo il valore non si può giudicare");
    }

    @Test
    void sogliaInQuantitaValeAncheSenzaPrezzo() {
        assertTrue(SOLO_QTA.IsMicro("0.0005", null));
        assertFalse(SOLO_QTA.IsMicro("0.002", null));
    }

    @Test
    void bastaUnaDelleDueSoglie() {
        Principale_GiacenzeaData_Microsaldi.Soglie entrambe =
                new Principale_GiacenzeaData_Microsaldi.Soglie(new BigDecimal("0.10"), new BigDecimal("0.001"));
        assertTrue(entrambe.IsMicro("1000", new BigDecimal("0.05")), "vale poco anche se la quantità è alta (SHIB)");
        assertTrue(entrambe.IsMicro("0.0005", new BigDecimal("50")), "quantità minima anche se vale tanto");
        assertFalse(entrambe.IsMicro("1", new BigDecimal("50")));
    }

    @Test
    void soloLeQuantitaPositiveSonoMicrosaldi() {
        assertFalse(SOLO_EURO.IsMicro("0", BigDecimal.ZERO), "zero esatto non è un residuo");
        assertFalse(SOLO_EURO.IsMicro("-0.00000012", new BigDecimal("0.01")), "il negativo ha il suo rosso");
        assertFalse(SOLO_EURO.IsMicro("", null));
        assertFalse(SOLO_EURO.IsMicro("n.d.", null));
    }

    @Test
    void notazioneScientificaESimboloDecimaleItaliano() {
        // La stessa trappola del bug M7: 2.5E-9 è positivo
        assertTrue(SOLO_QTA.IsMicro("2.5E-9", null));
        assertEquals(new BigDecimal("0.5"), Principale_GiacenzeaData_Microsaldi.SogliaDaTesto("0,5"));
        assertNull(Principale_GiacenzeaData_Microsaldi.SogliaDaTesto("  "));
        assertThrows(IllegalArgumentException.class, () -> Principale_GiacenzeaData_Microsaldi.SogliaDaTesto("abc"));
        assertThrows(IllegalArgumentException.class, () -> Principale_GiacenzeaData_Microsaldi.SogliaDaTesto("0"));
        assertThrows(IllegalArgumentException.class, () -> Principale_GiacenzeaData_Microsaldi.SogliaDaTesto("-1"));
    }

    @Test
    void fiatENftSonoEsclusi() {
        assertTrue(Principale_GiacenzeaData_Microsaldi.TipoEscluso("FIAT"));
        assertTrue(Principale_GiacenzeaData_Microsaldi.TipoEscluso("nft"));
        assertTrue(Principale_GiacenzeaData_Microsaldi.TipoEscluso(""));
        assertFalse(Principale_GiacenzeaData_Microsaldi.TipoEscluso("Crypto"));
    }

    // ---------- tabella principale ----------

    /** Le 13 colonne della tabella principale: 3 Tipo, 4 Qta, 5 Valore, 7 Valore unitario, 12 Qta API. */
    private static JTable principale(Object tipo, String qta, Double valore, Double unitario, String qtaApi,
            Principale_GiacenzeaData_Microsaldi.Soglie soglie) {
        DefaultTableModel m = new DefaultTableModel(1, 13) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 5 || c == 6 || c == 7 || c == 8 || c == 9 ? Double.class : Object.class;
            }
        };
        m.setValueAt("TKN", 0, 0);
        m.setValueAt(tipo, 0, 3);
        m.setValueAt(qta, 0, 4);
        m.setValueAt(valore, 0, 5);
        m.setValueAt(unitario, 0, 7);
        m.setValueAt("", 0, 10);
        m.setValueAt(qtaApi, 0, 12);
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        Tabelle.ColoraRigheTabella0GiacenzeaData(t);
        t.putClientProperty(Tabelle.PROP_MICROSALDI, soglie);
        return t;
    }

    private static boolean giallo(JTable t, int colonna) {
        Component c = t.prepareRenderer(t.getCellRenderer(0, colonna), 0, colonna);
        return c.getBackground().equals(Tabelle.gialloMicrosaldo);
    }

    @Test
    void principaleColoraLaQuantitaDelMicrosaldo() {
        JTable t = principale("Crypto", "0.00000012", 0.0, 60000.0, "n.d.", SOLO_EURO);
        assertTrue(giallo(t, 4));
        assertFalse(giallo(t, 0), "solo la cella della quantità");
        Component c = t.prepareRenderer(t.getCellRenderer(0, 4), 0, 4);
        assertTrue(((JComponent) c).getToolTipText().contains("0.10"), "il tooltip dice la soglia");
        assertNull(((JComponent) t.prepareRenderer(t.getCellRenderer(0, 0), 0, 0)).getToolTipText(),
                "il tooltip non resta sulle altre celle");
    }

    @Test
    void principaleNonColoraSeLeApiConfermanoLaGiacenza() {
        assertFalse(giallo(principale("Crypto", "0.00000012", 0.0, 60000.0, "0.00000012", SOLO_EURO), 4));
        assertFalse(giallo(principale("Crypto", "1.2E-7", 0.0, 60000.0, "0.00000012", SOLO_EURO), 4),
                "confronto numerico, non testuale");
    }

    @Test
    void principaleColoraSeLeApiDicono() {
        // Il residuo c'è nell'app e sull'exchange c'è zero: è il caso da vedere
        assertTrue(giallo(principale("Crypto", "0.00000012", 0.0, 60000.0, "0", SOLO_EURO), 4));
    }

    @Test
    void principaleSenzaPrezzoSiGiudicaSoloPerQuantita() {
        assertFalse(giallo(principale("Crypto", "0.0005", 0.0, null, "", SOLO_EURO), 4),
                "token senza prezzo: ha già il suo avviso");
        assertTrue(giallo(principale("Crypto", "0.0005", 0.0, null, "", SOLO_QTA), 4));
    }

    @Test
    void principaleNonColoraFiatNftGrandiSaldiOSpento() {
        assertFalse(giallo(principale("FIAT", "0.05", 0.05, 1.0, "", SOLO_EURO), 4));
        assertFalse(giallo(principale("NFT", "1", 0.0, 1.0, "", SOLO_QTA), 4));
        assertFalse(giallo(principale("Crypto", "2", 120000.0, 60000.0, "", SOLO_EURO), 4));
        assertFalse(giallo(principale("Crypto", "0.00000012", 0.0, 60000.0, "", null), 4), "spento");
    }

    // ---------- tabella dettaglio ----------

    /** Colonne del dettaglio: 7 Qta Residua, 8 ID, 15 Valore Qta Residua, 18 Qta API. */
    private static JTable dettaglio(String qtaResidua, String valoreResiduo, String qtaApi, Object archivio,
            Principale_GiacenzeaData_Microsaldi.Soglie soglie) {
        DefaultTableModel m = new DefaultTableModel(1, 19) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        m.setValueAt("ID1", 0, 8);
        m.setValueAt(qtaResidua, 0, 7);
        m.setValueAt(valoreResiduo, 0, 15);
        m.setValueAt(qtaApi, 0, 18);
        JTable t = new JTable(m);
        Tabelle.ColoraRigheTabella1GiacenzeaData(t);
        t.putClientProperty(Tabelle.PROP_MICROSALDI, soglie);
        if (archivio != null) {
            t.putClientProperty(Tabelle.PROP_CONFRONTO_BLOCKCHAIN, Map.of("ID1", archivio));
        }
        return t;
    }

    @Test
    void dettaglioColoraLaQtaResiduaMicro() {
        assertTrue(giallo(dettaglio("0.00000012", "0.00", "", null, SOLO_EURO), 7));
    }

    @Test
    void dettaglioNonColoraSeIlPrezzoNonEArrivatoOLaQtaEGrande() {
        assertFalse(giallo(dettaglio("0.00000012", "", "", null, SOLO_EURO), 7), "valore ancora da completare");
        assertFalse(giallo(dettaglio("3", "180.00", "", null, SOLO_EURO), 7));
    }

    @Test
    void dettaglioNonColoraSeLApiConfermaLaRiga() {
        assertFalse(giallo(dettaglio("0.00000012", "0.00", "0.00000012", "0.00000012", SOLO_EURO), 7));
        assertTrue(giallo(dettaglio("0.00000012", "0.00", "0", "0.00000012", SOLO_EURO), 7),
                "l'API legge zero: il residuo va visto");
        assertTrue(giallo(dettaglio("0.00000012", "0.00", "n.d.", "0.00000012", SOLO_EURO), 7),
                "saldo non letto: non è una conferma");
    }

    @Test
    void dettaglioNegativoRestaRosso() {
        JTable t = dettaglio("-0.00000012", "0.00", "", null, SOLO_EURO);
        Component c = t.prepareRenderer(t.getCellRenderer(0, 7), 0, 7);
        assertNotEquals(Tabelle.gialloMicrosaldo, c.getBackground());
        assertTrue(c.getBackground().getRed() > c.getBackground().getGreen() + 50, "rosso");
    }

    @Test
    void temaScuroUsaUnGialloPiuCupo() {
        Principale.tema = "Scuro";
        JTable t = principale("Crypto", "0.00000012", 0.0, 60000.0, "", SOLO_EURO);
        Component c = t.prepareRenderer(t.getCellRenderer(0, 4), 0, 4);
        assertEquals(Tabelle.gialloMicrosaldoScuro, c.getBackground());
        assertEquals(Color.WHITE, c.getForeground());
    }
}
