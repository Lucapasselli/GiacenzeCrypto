package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * La giacenza di ogni giorno lavorativo dell'anno, per capire perché la soglia dei 51.645,69 euro è superata o no
 * ({@link SogliaGiacenzaAnte2023}): data, giorno della settimana, valore al prezzo del 1° gennaio, se sta sopra soglia
 * e da quanti giorni lavorativi continui. I sette giorni che hanno fatto scattare la condizione sono evidenziati.
 * Solo lettura, costruita a codice senza {@code .form}. In testa la nota dell'esito ({@link Principale_RT_Soglia}).
 */
public class GUI_DettaglioSoglia extends JDialog {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Mostra la finestra (modale). */
    public static void Mostra(Window owner, SogliaGiacenzaAnte2023.Esito esito, Principale_RT_Soglia.Risultato risultato) {
        new GUI_DettaglioSoglia(owner, esito, risultato).setVisible(true);
    }

    private GUI_DettaglioSoglia(Window owner, SogliaGiacenzaAnte2023.Esito esito, Principale_RT_Soglia.Risultato risultato) {
        super(owner, "Soglia dei 51.645,69 euro nel " + esito.anno, ModalityType.APPLICATION_MODAL);

        String riassunto = risultato == null || risultato.nota.isEmpty() ? "" : Principale_RT_Soglia.Html(risultato.nota);
        JLabel testo = new JLabel("<html><div style='width:820px'>" + riassunto + "<br><br>Giacenza a fine di ogni giorno lavorativo (lunedì-venerdì senza festività nazionali), "
                + "al prezzo delle 00:00 del 1° gennaio, di tutti i wallet. I giorni sopra soglia di una serie lunga almeno "
                + SogliaGiacenzaAnte2023.GIORNI_LAVORATIVI_CONTINUI + " giorni sono evidenziati.</div></html>");
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel modello = new DefaultTableModel(
                new Object[] {"Data", "Giorno", "Giacenza (euro)", "Sopra soglia", "Giorni continui sopra soglia"}, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        int serie = 0;
        boolean[] evidenziata = new boolean[esito.giorni.size()];
        int i = 0;
        for (SogliaGiacenzaAnte2023.Giorno g : esito.giorni) {
            serie = g.sopraSoglia ? serie + 1 : 0;
            LocalDate d = g.data;
            evidenziata[i++] = esito.superata && !d.isBefore(esito.inizioPeriodo) && !d.isAfter(esito.finePeriodo);
            modello.addRow(new Object[] {DATA.format(d), d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ITALY),
                String.format(Locale.ITALY, "%,.2f", g.valore), g.sopraSoglia ? "Sì" : "No", serie == 0 ? "" : String.valueOf(serie)});
        }
        JTable tabella = new JTable(modello);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        DefaultTableCellRenderer cella = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valore, boolean sel, boolean fuoco, int riga, int colonna) {
                Component c = super.getTableCellRendererComponent(t, valore, sel, fuoco, riga, colonna);
                setHorizontalAlignment(colonna == 2 || colonna >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                int m = t.convertRowIndexToModel(riga);
                boolean ev = evidenziata[m];
                setFont(t.getFont().deriveFont(ev ? Font.BOLD : Font.PLAIN));
                if (!sel) {
                    //Un velo del colore di selezione sullo sfondo della tabella: si legge anche nel tema scuro
                    setBackground(ev ? velo(t.getSelectionBackground(), t.getBackground(), 0.35f) : t.getBackground());
                }
                return c;
            }
        };
        for (int c = 0; c < tabella.getColumnCount(); c++) {
            tabella.getColumnModel().getColumn(c).setCellRenderer(cella);
        }
        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setPreferredSize(new Dimension(880, 420));

        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(chiudi);

        setLayout(new BorderLayout());
        add(testo, BorderLayout.NORTH);
        add(scorri, BorderLayout.CENTER);
        add(pulsanti, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);

        // Si apre sul giorno più interessante: l'inizio del periodo scattato, altrimenti il giorno di massimo
        LocalDate dove = esito.superata ? esito.inizioPeriodo : esito.giornoMassimo;
        if (dove != null) {
            for (int r = 0; r < esito.giorni.size(); r++) {
                if (esito.giorni.get(r).data.equals(dove)) {
                    final int riga = r;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        tabella.scrollRectToVisible(tabella.getCellRect(Math.min(riga + 6, tabella.getRowCount() - 1), 0, true));
                        tabella.scrollRectToVisible(tabella.getCellRect(riga, 0, true));
                        tabella.setRowSelectionInterval(riga, riga);
                    });
                    break;
                }
            }
        }
    }

    private static Color velo(Color sopra, Color sotto, float peso) {
        return new Color(
                Math.round(sopra.getRed() * peso + sotto.getRed() * (1 - peso)),
                Math.round(sopra.getGreen() * peso + sotto.getGreen() * (1 - peso)),
                Math.round(sopra.getBlue() * peso + sotto.getBlue() * (1 - peso)));
    }
}
