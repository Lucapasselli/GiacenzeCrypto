package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Dialogo (senza {@code .form}, contenuto dinamico) dei costi netti di "Giacenze a data": una riga per token, con tutti
 * i wallet insieme e i token dello stesso nome uniti anche se su reti o indirizzi diversi. È un'informazione
 * <b>separata</b> da quelle fiscali, e l'intestazione lo dice. Non calcola nulla: mostra le {@code Voce} che
 * {@link Principale_GiacenzeaData_CostoNetto#Voci} ha già preparato.
 */
public class GUI_CostiNetti extends JDialog {

    private static final int COL_NOME = 0;
    private static final int COL_TIPO = 1;
    private static final int COL_QTA = 2;
    private static final int COL_VALORE = 3;
    private static final int COL_COSTO = 4;
    private static final int COL_VENDITE = 5;
    private static final int COL_COSTO_MEDIO = 6;
    private static final int COL_PROFITTO = 7;
    private static final int COL_NOTE = 8;

    private final List<Principale_GiacenzeaData_CostoNetto.Voce> voci;
    private final DefaultTableModel modello;
    private final JLabel totali = new JLabel();
    private final JCheckBox nascondiZero = new JCheckBox("Nascondi i token con giacenza zero");

    public GUI_CostiNetti(Window owner, String data, List<Principale_GiacenzeaData_CostoNetto.Voce> voci) {
        super(owner, "Costi netti dei token", ModalityType.APPLICATION_MODAL);
        this.voci = voci;

        JLabel intestazione = new JLabel("<html><b>Costi netti dei token alla data " + data + "</b><br><br>"
                + "Informazioni separate dai dati fiscali, solo per consultazione. Considerano tutti i wallet insieme "
                + "e uniscono in una riga i token con lo stesso nome anche se su reti o indirizzi diversi.<br>"
                + "Costo totale e Vendite sono i valori in euro dei movimenti, compresi gli scambi cripto/cripto, che "
                + "per il calcolo delle plusvalenze possono non essere rilevanti. "
                + "<b>Profitti e perdite di questa tabella possono quindi non coincidere con quelli fiscali.</b><br>"
                + "Costo medio netto = (Costo totale - Vendite) / Qta.<br>"
                + "Profitti/Perdite = Valore + Vendite - Costo totale, e con giacenza zero Vendite - Costo totale.</html>");
        intestazione.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        modello = new DefaultTableModel(new String[]{"Nome", "Tipo", "Qta", "<html><center>Valore<br>(in Euro)</html>",
            "<html><center>Costo totale<br>(in Euro)</html>", "<html><center>Vendite<br>(in Euro)</html>",
            "<html><center>Costo medio<br>netto</html>", "<html><center>Profitti/Perdite<br>totali (in Euro)</html>",
            "Note"}, 0) {
            private final Class<?>[] tipi = {Object.class, Object.class, BigDecimal.class, Double.class, Double.class,
                Double.class, Double.class, Double.class, Object.class};

            @Override
            public Class<?> getColumnClass(int colonna) {
                return tipi[colonna];
            }

            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        JTable tabella = new JTable(modello);
        tabella.setRowSorter(new TableRowSorter<>(modello));
        tabella.setRowHeight(tabella.getRowHeight() + 4);
        tabella.setFillsViewportHeight(true);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        tabella.getTableHeader().setPreferredSize(new Dimension(tabella.getColumnModel().getTotalColumnWidth(), 42));
        int[] larghezze = {130, 60, 150, 110, 110, 110, 110, 140, 620};
        for (int c = 0; c < larghezze.length; c++) {
            tabella.getColumnModel().getColumn(c).setPreferredWidth(larghezze[c]);
        }
        tabella.setDefaultRenderer(Object.class, new Cella());
        tabella.setDefaultRenderer(Double.class, new Cella());
        tabella.setDefaultRenderer(BigDecimal.class, new Cella());
        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        nascondiZero.addActionListener(e -> riempi());
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel sotto = new JPanel(new BorderLayout());
        sotto.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        JPanel sinistra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sinistra.add(nascondiZero);
        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        destra.add(chiudi);
        totali.setFont(totali.getFont().deriveFont(Font.BOLD));
        totali.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        sotto.add(totali, BorderLayout.NORTH);
        sotto.add(sinistra, BorderLayout.WEST);
        sotto.add(destra, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(intestazione, BorderLayout.NORTH);
        getContentPane().add(scorri, BorderLayout.CENTER);
        getContentPane().add(sotto, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(chiudi);
        riempi();
        setSize(1500, 650);
        setLocationRelativeTo(owner);
    }

    private void riempi() {
        modello.setRowCount(0);
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal vendite = BigDecimal.ZERO;
        BigDecimal profitto = BigDecimal.ZERO;
        int senzaProfitto = 0;
        int righe = 0;
        for (Principale_GiacenzeaData_CostoNetto.Voce v : voci) {
            if (nascondiZero.isSelected() && v.Qta.signum() == 0) {
                continue;
            }
            Principale_GiacenzeaData_CostoNetto.Riga r = v.Colonne;
            modello.addRow(new Object[]{v.Moneta, v.Tipo, v.Qta, v.Valore == null ? null : due(v.Valore), r.Speso,
                r.Incassato, r.CostoMedioNetto, r.ProfittoPerdita, r.Note});
            righe++;
            costo = costo.add(BigDecimal.valueOf(r.Speso));
            vendite = vendite.add(BigDecimal.valueOf(r.Incassato));
            if (r.ProfittoPerdita == null) {
                senzaProfitto++;
            } else {
                profitto = profitto.add(BigDecimal.valueOf(r.ProfittoPerdita));
            }
        }
        totali.setText("<html>" + righe + " token. Costo totale " + testo(costo) + " EUR, Vendite "
                + testo(vendite) + " EUR, Profitti/Perdite totali " + testo(profitto)
                + " EUR" + (senzaProfitto == 0 ? "" : " (esclusi " + senzaProfitto + " token senza profitto/perdita calcolabile)")
                + "</html>");
    }

    private static String testo(BigDecimal valore) {
        return valore.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static Double due(BigDecimal valore) {
        return valore.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Righe alternate come le altre tabelle, numeri senza notazione scientifica, profitti verdi e perdite rosse. */
    private static final class Cella extends Tabelle.CellaConRipiegoFont {

        @Override
        public Component getTableCellRendererComponent(JTable tabella, Object valore, boolean selezionata,
                boolean fuoco, int riga, int colonna) {
            Component c = super.getTableCellRendererComponent(tabella, valore, selezionata, fuoco, riga, colonna);
            int col = tabella.convertColumnIndexToModel(colonna);
            boolean scuro = Principale.tema.equalsIgnoreCase("Scuro");
            Color sfondo = scuro ? (riga % 2 == 0 ? Tabelle.grigio : Tabelle.grigioScuro)
                    : (riga % 2 == 0 ? Tabelle.grigioChiaro : Tabelle.bianco);
            Color testo = scuro ? Color.lightGray : Color.BLACK;
            if (selezionata) {
                c.setBackground(Tabelle.SfondoSelezione(riga));
            } else {
                c.setBackground(sfondo);
                c.setForeground(testo);
            }
            setHorizontalAlignment(col == COL_NOME || col == COL_TIPO || col == COL_NOTE ? SwingConstants.LEFT
                    : SwingConstants.RIGHT);
            if (valore == null) {
                setText("");
            } else if (col == COL_QTA && valore instanceof BigDecimal) {
                BigDecimal q = (BigDecimal) valore;
                setText(q.signum() == 0 ? "0" : q.stripTrailingZeros().toPlainString());
            } else if (col == COL_COSTO_MEDIO && valore instanceof Double) {
                setText(Principale_GiacenzeaData.FormattaUnitario((Double) valore));
            } else if (valore instanceof Double) {
                //BigDecimal e non Double.toString, che sopra i dieci milioni scrive la notazione scientifica
                setText(BigDecimal.valueOf((Double) valore).setScale(2, RoundingMode.HALF_UP).toPlainString());
                if (col == COL_PROFITTO && !selezionata && ((Double) valore) != 0.0) {
                    c.setForeground(((Double) valore) > 0 ? Tabelle.verdeScuro : Tabelle.rosso);
                }
            }
            if (col == COL_NOTE) {
                //La nota puo' essere piu' lunga della colonna: il testo intero sta nel suggerimento
                setToolTipText(valore == null || valore.toString().isEmpty() ? null : valore.toString());
                if (!selezionata) {
                    c.setForeground(Tabelle.rosso);
                }
            }
            return c;
        }
    }
}
