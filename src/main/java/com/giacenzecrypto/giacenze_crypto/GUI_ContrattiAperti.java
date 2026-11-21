package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Le posizioni su derivati aperte a una data ({@link Principale_ContrattiAperti}), dal pulsante <i>Contratti aperti</i> della scheda
 * <i>Giacenze a data</i>. Dialogo a codice, senza {@code .form}: una tabella e il riepilogo del contratto della riga scelta
 * (doppio clic o pulsante). Solo consultazione.
 */
public class GUI_ContrattiAperti extends JDialog {

    private static final int COL_ID = 8;

    private final DefaultTableModel modello;
    private final JTable tabella;

    /** Mostra la finestra (modale) con le posizioni aperte prima di {@code limite}. */
    public static void Mostra(Window owner, String data, long limite) {
        new GUI_ContrattiAperti(owner, data, Principale_ContrattiAperti.Calcola(limite)).setVisible(true);
    }

    private GUI_ContrattiAperti(Window owner, String data, List<Principale_ContrattiAperti.Riga> righe) {
        super(owner, "Contratti derivati aperti al " + data, ModalityType.APPLICATION_MODAL);

        JTextArea testo = new JTextArea(Principale_ContrattiAperti.TestoIntestazione(data, righe.size()));
        testo.setEditable(false);
        testo.setLineWrap(true);
        testo.setWrapStyleWord(true);
        testo.setOpaque(false);
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        modello = new DefaultTableModel(new Object[0][0], new Object[]{"Exchange", "Contratto", "Verso", "Contratti aperti",
            "Aperta dal", "Prezzo medio", "Margine", "Moneta", "ID"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 3 || c == 5 || c == 6 ? BigDecimal.class : Object.class;
            }
        };
        for (Principale_ContrattiAperti.Riga r : righe) {
            modello.addRow(new Object[]{r.exchange, r.contratto, r.lungo ? "Long" : "Short", r.contratti, r.apertaDal,
                r.prezzoMedio, r.margine.stripTrailingZeros(), r.moneta, r.idMovimento});
        }
        tabella = new JTable(modello);
        tabella.setAutoCreateRowSorter(true);
        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.getColumnModel().removeColumn(tabella.getColumnModel().getColumn(COL_ID));
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        Tabelle.ColoraTabellaSemplice(tabella);
        //I numeri per intero (quantita' e margini hanno molti decimali): il renderer standard dei Number ne tiene tre
        javax.swing.table.TableCellRenderer base = tabella.getDefaultRenderer(Object.class);
        tabella.setDefaultRenderer(BigDecimal.class, (t, valore, sel, fuoco, riga, col) -> {
            Object numero = valore instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString().replace('.', ',') : valore;
            java.awt.Component c = base.getTableCellRendererComponent(t, numero, sel, fuoco, riga, col);
            if (c instanceof javax.swing.JLabel l) l.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            return c;
        });
        //Un solo componente disegna tutte le colonne: il testo torna a sinistra nelle altre
        tabella.setDefaultRenderer(Object.class, (t, valore, sel, fuoco, riga, col) -> {
            java.awt.Component c = base.getTableCellRendererComponent(t, valore, sel, fuoco, riga, col);
            if (c instanceof javax.swing.JLabel l) l.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            return c;
        });
        tabella.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) ApriRiepilogo();
            }
        });

        JButton riepilogo = new JButton("Riepilogo del contratto");
        riepilogo.setEnabled(!righe.isEmpty());
        riepilogo.addActionListener(e -> ApriRiepilogo());
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(riepilogo);
        pulsanti.add(chiudi);

        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10), scorri.getBorder()));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(testo, BorderLayout.NORTH);
        getContentPane().add(scorri, BorderLayout.CENTER);
        getContentPane().add(pulsanti, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(980, 420));
        pack();
        setLocationRelativeTo(owner);
    }

    /** Il riepilogo della posizione della riga scelta (la prima, se non ne e' scelta nessuna). */
    private void ApriRiepilogo() {
        if (modello.getRowCount() == 0) return;
        int vista = tabella.getSelectedRow() < 0 ? 0 : tabella.getSelectedRow();
        String id = (String) modello.getValueAt(tabella.convertRowIndexToModel(vista), COL_ID);
        if (id != null && !id.isBlank()) Principale_Derivati.ApriRiepilogo(this, id);
    }
}
