package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Il dialogo del pulsante <b>Errori</b>: un elenco con una riga per ogni tipo di errore presente (problema, quanti, effetto sui
 * calcoli) e sotto la spiegazione di quello selezionato. Costruito a codice, senza {@code .form}, perché {@link AppDialog} accetta solo
 * pulsanti e testo: con un pulsante per tipo di errore la riga non entrava più, ora un tipo nuovo è una riga in più.
 *
 * <p>Le voci e i testi vengono da {@link Principale_Errori}. Il dialogo non corregge nulla: restituisce l'identificativo della voce
 * scelta (doppio clic, Invio o <b>Correggi</b>) e la correzione la decide il chiamante. Chiudendo senza scegliere restituisce
 * {@code null}.</p>
 */
public class GUI_Errori extends JDialog {

    private final List<Principale_Errori.Voce> voci;
    private final JTable tabella;
    private final JLabel dettaglio;
    private final JButton correggi;
    private String scelta = null;

    /**
     * Mostra il dialogo (modale) e restituisce la voce scelta.
     *
     * @param owner finestra proprietaria
     * @param voci le voci da mostrare, già nell'ordine voluto ({@link Principale_Errori#Voci})
     * @param titoliSenzaProblemi i tipi controllati senza nessun errore, per dirlo in fondo (può essere vuoto)
     * @param notaFuoriPeriodo nota sui movimenti in errore fuori dal periodo selezionato (può essere vuota)
     * @return l'identificativo della voce scelta, {@code null} se si è chiuso senza scegliere
     */
    public static String Mostra(Window owner, List<Principale_Errori.Voce> voci, List<String> titoliSenzaProblemi, String notaFuoriPeriodo) {
        GUI_Errori g = new GUI_Errori(owner, voci, titoliSenzaProblemi, notaFuoriPeriodo);
        g.setVisible(true);
        return g.scelta;
    }

    private GUI_Errori(Window owner, List<Principale_Errori.Voce> voci, List<String> titoliSenzaProblemi, String notaFuoriPeriodo) {
        super(owner, "Correzione errori", ModalityType.APPLICATION_MODAL);
        this.voci = voci;

        int totale = 0;
        for (Principale_Errori.Voce v : voci) totale += v.quanti();
        JLabel intestazione = new JLabel("<html><body style='width:720px'><b>Errori da correggere: " + totale + "</b><br>"
                + "Seleziona un tipo di errore per leggerne l'effetto sui calcoli, poi premi <b>Correggi</b>.</body></html>");
        intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        DefaultTableModel modello = new DefaultTableModel(new Object[] {"Problema", "Quanti", "Effetto sui calcoli"}, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        for (Principale_Errori.Voce v : voci) modello.addRow(new Object[] {v.titolo(), v.quanti(), v.effetto()});
        tabella = new JTable(modello);
        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.setRowHeight(Math.max(tabella.getRowHeight(), 26));
        tabella.setFillsViewportHeight(true);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        //Righe alternate e colore di selezione come nelle altre tabelle del programma (anche nel tema scuro)
        Tabelle.ColoraTabellaSemplice(tabella);
        //Il numero al centro: a destra si attaccava al testo della colonna accanto. Si avvolge il renderer standard invece di
        //sostituirlo, altrimenti la colonna perde le righe alternate. L'allineamento si imposta su ogni colonna, perche' il
        //renderer standard e' lo stesso componente per tutte e porterebbe il centro anche alle altre
        final TableCellRenderer standard = tabella.getDefaultRenderer(Object.class);
        final int[] allineamenti = {SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.LEFT};
        for (int i = 0; i < allineamenti.length; i++) {
            final int allineamento = allineamenti[i];
            tabella.getColumnModel().getColumn(i).setCellRenderer((t, valore, selezionata, fuoco, riga, colonna) -> {
                Component c = standard.getTableCellRendererComponent(t, valore, selezionata, fuoco, riga, colonna);
                if (c instanceof JLabel l) l.setHorizontalAlignment(allineamento);
                return c;
            });
        }
        int[] larghezze = {260, 70, 390};
        for (int i = 0; i < larghezze.length; i++) tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setPreferredSize(new Dimension(720, Math.min(8, Math.max(3, voci.size())) * tabella.getRowHeight() + 32));

        dettaglio = new JLabel();
        dettaglio.setVerticalAlignment(SwingConstants.TOP);
        JPanel pannelloDettaglio = new JPanel(new BorderLayout());
        pannelloDettaglio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 10, 4, 10),
                BorderFactory.createTitledBorder("Di cosa si tratta")));
        pannelloDettaglio.add(dettaglio, BorderLayout.CENTER);
        pannelloDettaglio.setPreferredSize(new Dimension(720, 150));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centro.add(scorri, BorderLayout.CENTER);

        correggi = new JButton("Correggi");
        correggi.addActionListener(e -> Conferma());
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(correggi);
        pulsanti.add(chiudi);

        StringBuilder piede = new StringBuilder();
        if (titoliSenzaProblemi != null && !titoliSenzaProblemi.isEmpty()) {
            piede.append("Nessun problema su: ").append(String.join(", ", titoliSenzaProblemi)).append(".");
        }
        if (notaFuoriPeriodo != null && !notaFuoriPeriodo.isBlank()) {
            if (piede.length() > 0) piede.append("<br>");
            piede.append(notaFuoriPeriodo);
        }
        JPanel sud = new JPanel(new BorderLayout());
        if (piede.length() > 0) {
            JLabel nota = new JLabel("<html><body style='width:720px'>" + piede + "</body></html>");
            nota.setEnabled(false);
            nota.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            sud.add(nota, BorderLayout.NORTH);
        }
        sud.add(pulsanti, BorderLayout.SOUTH);

        JPanel sotto = new JPanel(new BorderLayout());
        sotto.add(pannelloDettaglio, BorderLayout.CENTER);
        sotto.add(sud, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(intestazione, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(sotto, BorderLayout.SOUTH);

        tabella.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) AggiornaDettaglio();
        });
        tabella.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabella.rowAtPoint(e.getPoint()) >= 0) Conferma();
            }
        });
        //Invio sulla tabella conferma la voce (di suo sposterebbe la selezione alla riga dopo)
        tabella.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "correggiVoce");
        tabella.getActionMap().put("correggiVoce", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Conferma();
            }
        });
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(correggi);

        if (!voci.isEmpty()) tabella.setRowSelectionInterval(0, 0);
        AggiornaDettaglio();
        pack();
        setMinimumSize(new Dimension(760, getHeight()));
        setLocationRelativeTo(owner);
    }

    private Principale_Errori.Voce Selezionata() {
        int riga = tabella.getSelectedRow();
        return riga < 0 ? null : voci.get(tabella.convertRowIndexToModel(riga));
    }

    private void AggiornaDettaglio() {
        Principale_Errori.Voce v = Selezionata();
        correggi.setEnabled(v != null);
        if (v == null) {
            dettaglio.setText("");
            return;
        }
        dettaglio.setText("<html><body style='width:690px'><b>" + v.titolo() + "</b> (" + v.quanti() + ")<br><br>"
                + v.descrizione() + "</body></html>");
    }

    private void Conferma() {
        Principale_Errori.Voce v = Selezionata();
        if (v == null) return;
        scelta = v.id();
        dispose();
    }
}
