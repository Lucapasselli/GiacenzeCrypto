package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Il giro di un movimento ({@link Principale_GiroMovimento}), dalla voce <i>Mostra il giro del movimento</i> del menu dei movimenti. Dialogo a
 * codice, senza {@code .form}: una riga per movimento con la sua spiegazione, il movimento scelto selezionato, i totali sotto. Dalle righe si
 * apre il menu dei movimenti in sola consultazione.
 */
public class GUI_GiroMovimento extends JDialog {

    private static final int COL_ID = 11;

    /** Mostra la finestra (modale) con il giro del movimento. */
    public static void Mostra(Window owner, String id) {
        new GUI_GiroMovimento(owner, Principale_GiroMovimento.Calcola(id)).setVisible(true);
    }

    private GUI_GiroMovimento(Window owner, Principale_GiroMovimento.Giro g) {
        super(owner, "Giro del movimento", ModalityType.APPLICATION_MODAL);
        String[] scelto = MappaCryptoWallet.get(g.id);

        StringBuilder t = new StringBuilder();
        t.append(g.voci.size() <= 1 ? "Nessun altro movimento legato a quello scelto"
                : (g.voci.size() - 1) + (g.voci.size() == 2 ? " movimento legato" : " movimenti legati") + " a quello scelto")
                .append(scelto == null ? "" : " (" + FunzioniDate.DataOraAlSecondo(scelto) + ", " + scelto[5] + ")").append(".\n");
        if (!g.contratto.isEmpty()) t.append("Contratto: ").append(g.contratto).append(".\n");
        t.append("Sono nel giro i movimenti della stessa operazione, quelli della stessa posizione su un contratto derivato (da quando i contratti "
                + "aperti passano da zero a quando tornano a zero, con margine, profitti, commissioni e funding) e quelli collegati dalla "
                + "classificazione. La spiegazione di ogni riga è calcolata dai dati del movimento.");
        if (g.escluse > 0) t.append("\nAttenzione: ").append(g.escluse).append(" movimenti oltre il limite di ")
                .append(Principale_GiroMovimento.MASSIMO).append(" righe non sono mostrati.");
        JTextArea testo = new JTextArea(t.toString());
        testo.setEditable(false);
        testo.setLineWrap(true);
        testo.setWrapStyleWord(true);
        testo.setOpaque(false);
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        DefaultTableModel modello = new DefaultTableModel(new Object[0][0], new Object[]{"Data e ora", "Spiegazione", "Uscita",
            "Entrata", "Valore (euro)", "Plusvalenza", "Risultato derivati", "Legame", "Exchange", "Wallet", "Movimento", "ID"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 4 || c == 5 || c == 6 ? BigDecimal.class : Object.class;
            }
        };
        int rigaScelta = -1;
        for (Principale_GiroMovimento.Voce x : g.voci) {
            String[] v = MappaCryptoWallet.get(x.id);
            if (v == null) continue;
            if (x.legame == Principale_GiroMovimento.Legame.SCELTO) rigaScelta = modello.getRowCount();
            BigDecimal plus = "S".equalsIgnoreCase(v[33]) && Funzioni.isNumeric(v[19], false) ? new BigDecimal(v[19]) : null;
            String legame = x.legame == Principale_GiroMovimento.Legame.POSIZIONE && !g.contratto.isEmpty()
                    ? x.legame.testo + " su " + g.contratto : x.legame.testo;
            modello.addRow(new Object[]{FunzioniDate.DataOraAlSecondo(v), x.spiegazione, Gamba(v[10], v[8]), Gamba(v[13], v[11]),
                Funzioni.isNumeric(v[15], false) ? new BigDecimal(v[15]) : null, plus, Derivati.Risultato(v), legame, v[3], v[4], v[5], x.id});
        }
        JTable tabella = new JTable(modello) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int r = rowAtPoint(e.getPoint());
                if (r < 0) return null;
                return (String) getModel().getValueAt(convertRowIndexToModel(r), 1);
            }
        };
        tabella.setAutoCreateRowSorter(true);
        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.getColumnModel().removeColumn(tabella.getColumnModel().getColumn(COL_ID));
        int[] larghezze = {130, 520, 130, 130, 80, 80, 100, 200, 70, 90, 180};
        for (int c = 0; c < larghezze.length; c++) tabella.getColumnModel().getColumn(c).setPreferredWidth(larghezze[c]);
        tabella.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        Tabelle.ColoraTabellaSemplice(tabella);
        //I numeri per intero e a destra, il testo a sinistra: un solo componente disegna tutte le colonne
        TableCellRenderer base = tabella.getDefaultRenderer(Object.class);
        tabella.setDefaultRenderer(BigDecimal.class, (tb, valore, sel, fuoco, riga, col) -> {
            Object numero = valore instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString().replace('.', ',') : valore;
            Component c = base.getTableCellRendererComponent(tb, numero, sel, fuoco, riga, col);
            if (c instanceof JLabel l) l.setHorizontalAlignment(SwingConstants.RIGHT);
            return c;
        });
        tabella.setDefaultRenderer(Object.class, (tb, valore, sel, fuoco, riga, col) -> {
            Component c = base.getTableCellRendererComponent(tb, valore, sel, fuoco, riga, col);
            if (c instanceof JLabel l) l.setHorizontalAlignment(SwingConstants.LEADING);
            return c;
        });
        if (rigaScelta >= 0) tabella.setRowSelectionInterval(rigaScelta, rigaScelta);
        tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                Principale.ApriMenuMovimenti(tabella, evt, COL_ID, Funzioni.ProfiloMenuMovimenti.CONSULTAZIONE, null);
            }
        });

        JLabel totali = new JLabel("Totali delle righe: plusvalenze " + Euro(g.Plusvalenze(MappaCryptoWallet))
                + " euro, risultato dei derivati " + Euro(g.RisultatoDerivati(MappaCryptoWallet)) + " euro");
        totali.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel sotto = new JPanel(new BorderLayout());
        sotto.add(totali, BorderLayout.WEST);
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(chiudi);
        sotto.add(pulsanti, BorderLayout.EAST);

        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10), scorri.getBorder()));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(testo, BorderLayout.NORTH);
        getContentPane().add(scorri, BorderLayout.CENTER);
        getContentPane().add(sotto, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(1200, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    /** Quantità con la moneta, vuoto se la gamba non c'è. */
    private static String Gamba(String qta, String moneta) {
        if (qta == null || qta.isBlank() || moneta == null || moneta.isBlank()) return "";
        String q = Funzioni.isNumeric(qta, false) ? new BigDecimal(qta).stripTrailingZeros().toPlainString().replace('.', ',') : qta;
        return q + " " + moneta;
    }

    private static String Euro(BigDecimal b) {
        return b.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
