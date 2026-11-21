package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Riepilogo di una posizione su un contratto derivato: ogni chiusura con i lotti consumati, il differenziale in LIFO
 * ({@link DerivatiLifo}) e il profitto riconosciuto dall'exchange, poi i totali per anno. Solo consultazione: nessun
 * movimento si modifica e nessun totale fiscale cambia. Dialogo a codice, senza {@code .form}.
 */
public class GUI_RiepilogoContratto extends JDialog {

    private static String fmt(BigDecimal v) {
        if (v == null) return "n.d.";
        BigDecimal r = v.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
        return r.signum() == 0 ? "0" : r.toPlainString();
    }

    private static String giorno(String data) {
        return data == null ? "" : data;
    }

    public GUI_RiepilogoContratto(Window owner, DerivatiLifo.Posizione p) {
        super(owner, "Riepilogo contratto derivato", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String verso = p.lungo ? "long" : "short";
        String tipo = p.inverso ? "inverso, regolato in " + p.moneta : "lineare, regolato in " + p.moneta;
        JLabel titolo = new JLabel("<html><b>" + esc(p.contratto) + "</b> posizione " + verso + " (" + tipo + ")<br>"
                + "Le quantità sono in contratti. Contratti ancora aperti: <b>" + fmt(p.contrattiAperti()) + "</b>"
                + (p.datiIncompleti ? "<br><font color='#b00020'>In almeno una posizione mancano prezzi o il valore del contratto: per le sue chiusure il differenziale in LIFO non è calcolato.</font>" : "")
                + (p.verificataInParte()
                    ? "<br><font color='#b00020'>In alcune posizioni (da un'apertura al ritorno a zero contratti) il profitto di OKX non si ricostruisce dai prezzi di esecuzione: dall'anno della prima chiusura che non torna il reddito resta quello dell'exchange.</font>"
                    : !p.datiIncompleti && !p.verificata() && !p.chiusure.isEmpty()
                    ? "<br><font color='#b00020'>Il profitto di OKX non si ricostruisce dai prezzi di esecuzione (con il suo prezzo medio): dall'anno della prima chiusura che non torna il reddito resta quello dell'exchange.</font>" : "")
                + "</html>");
        add(titolo, BorderLayout.NORTH);

        DefaultTableModel mc = new DefaultTableModel(new Object[]{"Data chiusura", "Contratti", "Prezzo chiusura", "Lotti chiusi (LIFO)",
            "Diff. LIFO", "Profitto exchange", "Differenza", "Reddito (euro)"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        StringBuilder conguagli = new StringBuilder();
        for (DerivatiLifo.Chiusura c : p.chiusure) {
            StringBuilder lotti = new StringBuilder();
            for (DerivatiLifo.Pezzo z : c.pezzi) {
                if (lotti.length() > 0) lotti.append(", ");
                lotti.append(fmt(z.contratti)).append(" a ").append(fmt(z.prezzoApertura)).append(" del ").append(giorno(z.dataApertura));
            }
            if (c.scoperti.signum() > 0) lotti.append(lotti.length() > 0 ? ", " : "").append(fmt(c.scoperti)).append(" senza apertura");
            String[] riga = Principale.MappaCryptoWallet.get(c.id);
            BigDecimal euro = riga == null ? null : Derivati.Risultato(riga);
            String conguaglio = riga == null ? "" : DerivatiLifo.TestoConguaglio(riga);
            String reddito = euro == null ? "dal profitto exchange"
                    : !conguaglio.isEmpty() ? "profitto exchange + conguaglio " + euro.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    : euro.setScale(2, RoundingMode.HALF_UP).toPlainString();
            if (!conguaglio.isEmpty()) conguagli.append("<br><b>Chiusura del ").append(esc(c.data)).append(".</b> ").append(esc(conguaglio));
            mc.addRow(new Object[]{c.data, fmt(c.contratti), fmt(c.prezzo), lotti.toString(), fmt(c.differenzialeLifo), fmt(c.pnlBorsa), fmt(c.differenza()),
                reddito});
        }
        JTable tc = tabella(mc);

        DefaultTableModel ma = new DefaultTableModel(new Object[]{"Anno", "Differenziale LIFO", "Profitto exchange", "Differenza"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Map.Entry<String, BigDecimal[]> e : p.perAnno().entrySet()) {
            ma.addRow(new Object[]{e.getKey(), fmt(e.getValue()[0]), fmt(e.getValue()[1]), fmt(e.getValue()[2])});
        }
        JTable ta = tabella(ma);

        JPanel su = new JPanel(new BorderLayout(0, 4));
        su.add(new JLabel("Chiusure"), BorderLayout.NORTH);
        su.add(new JScrollPane(tc), BorderLayout.CENTER);
        JPanel giu = new JPanel(new BorderLayout(0, 4));
        giu.add(new JLabel("Totali per anno di chiusura"), BorderLayout.NORTH);
        giu.add(new JScrollPane(ta), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, su, giu);
        split.setResizeWeight(0.7);
        add(split, BorderLayout.CENTER);

        String esito = p.datiIncompleti ? "" : (p.DivergePerAnno()
                ? "In almeno un anno i due metodi danno un totale diverso: la posizione si è chiusa in più riprese a cavallo d'anno. "
                : "I due metodi danno lo stesso totale in ogni anno. ");
        JLabel nota = new JLabel("<html><div style='width:820px'>" + esito
                + "Il reddito dei derivati di una chiusura è il differenziale in LIFO (art. 67 comma 1-bis del TUIR) per il prezzo della moneta, "
                + "scritto sulla chiusura del token. Il profitto dell'exchange, che usa il prezzo medio, resta il movimento di denaro e conta zero come reddito. "
                + "Dove il LIFO non si calcola il reddito resta quello dell'exchange." + conguagli + "</div></html>");
        JPanel basso = new JPanel(new BorderLayout());
        basso.add(nota, BorderLayout.CENTER);
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(chiudi);
        basso.add(pulsanti, BorderLayout.SOUTH);
        add(basso, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1100, 520));
        pack();
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(chiudi);
    }

    private static JTable tabella(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(false);
        t.setFillsViewportHeight(true);
        DefaultTableCellRenderer dx = new DefaultTableCellRenderer();
        dx.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 0; c < m.getColumnCount(); c++) {
            String n = m.getColumnName(c);
            if (n.contains("Contratti") || n.contains("Prezzo") || n.contains("Differenz") || n.contains("Profitto") || n.contains("Reddito")) t.getColumnModel().getColumn(c).setCellRenderer(dx);
        }
        if (m.getColumnCount() == 8) {
            t.getColumnModel().getColumn(3).setPreferredWidth(360);
            t.getColumnModel().getColumn(0).setPreferredWidth(120);
        }
        return t;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
