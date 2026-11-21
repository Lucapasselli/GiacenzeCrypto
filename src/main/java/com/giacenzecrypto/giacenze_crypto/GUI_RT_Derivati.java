package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Scheda «Redditi da derivati» del quadro RT (20/11/2026): per l'anno scelto nella tabella principale mostra i totali che vanno
 * nei righi dei derivati e, sotto, i movimenti che li compongono, uno per riga. Un riquadro a parte perché una riga o una colonna
 * nella tabella principale romperebbe la ricerca per anno. Scritta a mano, senza {@code .form}: la logica (testi e righe) sta in
 * {@link Principale_RT_Derivati}, qui c'è solo la parte Swing. Il menu dei movimenti lo aggancia {@code Principale}.
 */
public class GUI_RT_Derivati extends JPanel {

    private final JLabel riepilogo = new JLabel();
    private final JTable tabella;
    private final DefaultTableModel modello;

    public GUI_RT_Derivati() {
        super(new BorderLayout(0, 6));
        modello = new DefaultTableModel(new Object[0][0], Principale_RT_Derivati.COLONNE_RIQUADRO) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 4 ? BigDecimal.class : Object.class;
            }
        };
        tabella = new JTable(modello);
        tabella.setAutoCreateRowSorter(true);
        //L'ID serve solo al menu dei movimenti
        TableColumn id = tabella.getColumnModel().getColumn(Principale_RT_Derivati.COLONNA_ID_RIQUADRO);
        tabella.getColumnModel().removeColumn(id);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        riepilogo.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        riepilogo.setVerticalAlignment(JLabel.TOP);
        add(riepilogo, BorderLayout.NORTH);
        add(new JScrollPane(tabella), BorderLayout.CENTER);
        Pulisci();
    }

    /** La tabella dei movimenti, per agganciare il menu. */
    public JTable Tabella() {
        return tabella;
    }

    /** Nessun anno scelto. */
    public final void Pulisci() {
        Tabelle.Funzioni_PulisciTabella(modello);
        Colora();
        riepilogo.setText("<html>Scegliere un anno nella tabella in alto per vedere i redditi da derivati di quell'anno.</html>");
    }

    /** Mostra i redditi da derivati di un anno e le perdite dei quattro anni prima. */
    public void Mostra(Principale_RT_Derivati.Esito E, Map<String, BigDecimal> Perdite) {
        Tabelle.Funzioni_PulisciTabella(modello);
        if (E == null) {
            Pulisci();
            return;
        }
        StringBuilder sb = new StringBuilder("<html>");
        List<String> righe = Principale_RT_Derivati.RiepilogoRiquadro(E, Perdite);
        for (int i = 0; i < righe.size(); i++) {
            String r = righe.get(i).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            sb.append(i == 0 ? "<b>" + r + "</b>" : r).append("<br>");
        }
        sb.append("</html>");
        riepilogo.setText(sb.toString());
        for (Object[] r : Principale_RT_Derivati.RigheRiquadro(E)) modello.addRow(r);
        Colora();
    }

    /** Righe alternate come le altre tabelle, e il risultato in euro con due decimali, allineato a destra. */
    private void Colora() {
        Tabelle.ColoraTabellaSemplice(tabella);
        //Un solo componente disegna tutte le colonne: l'allineamento va reimpostato a ogni cella
        TableCellRenderer base = tabella.getDefaultRenderer(Object.class);
        TableCellRenderer celle = (t, valore, sel, fuoco, riga, col) -> {
            boolean importo = valore instanceof BigDecimal;
            Object testo = importo ? Funzioni.formattaBigDecimal((BigDecimal) valore, true) : valore;
            Component c = base.getTableCellRendererComponent(t, testo, sel, fuoco, riga, col);
            if (c instanceof JLabel l) l.setHorizontalAlignment(importo ? SwingConstants.RIGHT : SwingConstants.LEADING);
            return c;
        };
        tabella.setDefaultRenderer(Object.class, celle);
        tabella.setDefaultRenderer(BigDecimal.class, celle);
    }
}
