package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

/**
 * Abbinamento dei rientri del margine di una posizione aperta con la versione 1.0.66 ai prelievi del margine di allora
 * ({@link Principale_RientroMargine}). Costruita a codice, senza {@code .form}: in alto la posizione, al centro i prelievi da
 * spuntare, sotto cosa diventano i movimenti con la scelta fatta. La logica e' tutta nella classe di logica: qui solo la scelta.
 */
public class GUI_RientroMargine extends JDialog {

    private static final int COL_SCELTA = 0;
    private static final int COL_ID = 4;

    private final Principale_RientroMargine.Posizione posizione;
    private final DefaultTableModel modello;
    private final JTextArea anteprima = new JTextArea();
    private final JButton abbina = new JButton("Abbina");
    private boolean applicato = false;

    /**
     * Mostra la finestra (modale) per la posizione del rientro indicato. Se la posizione non si puo' abbinare lo dice e basta.
     *
     * @return {@code true} se l'abbinamento e' stato fatto (chi chiama fa ricalcolare)
     */
    public static boolean Mostra(Window owner, String idRientro) {
        Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(idRientro);
        if (p.motivo != null) {
            Messaggi.WarningMessage("Abbinamento del rientro del margine", p.motivo, owner);
            return false;
        }
        GUI_RientroMargine g = new GUI_RientroMargine(owner, p);
        g.setVisible(true);
        return g.applicato;
    }

    private GUI_RientroMargine(Window owner, Principale_RientroMargine.Posizione p) {
        super(owner, "Abbinamento del rientro del margine", ModalityType.APPLICATION_MODAL);
        this.posizione = p;

        StringBuilder rientri = new StringBuilder();
        for (String id : p.rientri) {
            String[] v = MappaCryptoWallet.get(id);
            rientri.append("<br>&nbsp;&nbsp;").append(FunzioniDate.DataOraAlSecondo(v)).append(": ")
                    .append(v[13]).append(' ').append(v[11]);
        }
        JLabel testo = new JLabel("<html><body style='width:600px'>La posizione su <b>" + Html(p.contratto) + "</b> è stata aperta con "
                + "la versione 1.0.66, che registrava il margine come un prelievo da classificare, ed è stata chiusa dopo "
                + "l'aggiornamento. Il margine è rientrato così:" + rientri + "<br><br>"
                + "Spuntare i prelievi del margine di questa posizione. I prelievi di quella versione non dicono il contratto: "
                + (p.proposti.isEmpty() ? "ce n'è più di uno possibile, quindi la scelta va fatta qui." : "il programma ha spuntato l'unico "
                + "possibile, da controllare.") + " Sono elencati solo quelli dopo l'ultima chiusura registrata da quella versione."
                + "</body></html>");
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        modello = new DefaultTableModel(new Object[0][0], new Object[]{"Abbina", "Data e ora", "Quantità", "Moneta", "ID"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_SCELTA;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_SCELTA ? Boolean.class : Object.class;
            }
        };
        for (String id : p.candidati) {
            String[] v = MappaCryptoWallet.get(id);
            modello.addRow(new Object[]{p.proposti.contains(id), FunzioniDate.DataOraAlSecondo(v), v[10], v[8], id});
        }
        JTable tabella = new JTable(modello);
        tabella.getColumnModel().removeColumn(tabella.getColumnModel().getColumn(COL_ID));
        tabella.getColumnModel().getColumn(COL_SCELTA).setMaxWidth(70);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        modello.addTableModelListener(e -> Aggiorna());

        anteprima.setEditable(false);
        anteprima.setLineWrap(true);
        anteprima.setWrapStyleWord(true);
        anteprima.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabella), new JScrollPane(anteprima));
        centro.setResizeWeight(0.45);
        centro.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JButton annulla = new JButton("Annulla");
        annulla.addActionListener(e -> dispose());
        abbina.addActionListener(e -> Applica());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(annulla);
        pulsanti.add(abbina);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(testo, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(pulsanti, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(820, 600));
        pack();
        setLocationRelativeTo(owner);
        Aggiorna();
    }

    private List<String> Scelti() {
        List<String> ids = new ArrayList<>();
        for (int r = 0; r < modello.getRowCount(); r++) {
            if (Boolean.TRUE.equals(modello.getValueAt(r, COL_SCELTA))) ids.add((String) modello.getValueAt(r, COL_ID));
        }
        return ids;
    }

    private void Aggiorna() {
        List<String> scelti = Scelti();
        abbina.setEnabled(!scelti.isEmpty());
        if (scelti.isEmpty()) {
            anteprima.setText("Nessun prelievo scelto.");
        } else {
            anteprima.setText(Principale_RientroMargine.TestoAnteprima(posizione, Principale_RientroMargine.Calcola(posizione, scelti)));
        }
        anteprima.setCaretPosition(0);
    }

    private void Applica() {
        List<String> scelti = Scelti();
        if (scelti.isEmpty()) return;
        if (Principale_RientroMargine.Applica(posizione, scelti)) {
            applicato = true;
            dispose();
        } else {
            Messaggi.WarningMessage("Abbinamento del rientro del margine", "L'abbinamento non è stato fatto: i movimenti sono rimasti "
                    + "come prima. Il motivo è nel log.", this);
        }
    }

    private static String Html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
