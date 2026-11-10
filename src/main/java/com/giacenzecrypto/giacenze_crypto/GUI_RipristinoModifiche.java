package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Scelta delle modifiche manuali da ripristinare ({@link Principale_Opzioni_ModificheManuali_Ripristino}): una riga per
 * modifica, spuntata in partenza se si può applicare, da togliere se non la si vuole riapplicare.
 *
 * <p>Scritto a mano, senza {@code .form}, come {@link GUI_RecuperoScarti}: il contenuto è tutto dinamico. Le righe che
 * non si possono applicare (conflitti, non ritrovate, già applicate...) hanno la spunta spenta e non modificabile, e
 * si vedono solo con «Mostra anche le righe senza nulla da applicare»: servono a capire perché una modifica non c'è.
 * La spunta riguarda la voce del piano così com'è: un'unione o una classificazione si applica o no per intero.</p>
 *
 * <p>Le colonne dicono quale movimento è e in breve cosa succede, ma i testi lunghi non ci stanno: selezionando una
 * riga, il pannello sotto la tabella ne mostra tutto il dettaglio, su più righe e senza abbreviazioni
 * ({@link #TestoDettaglio}). Si è scelto il pannello e non le righe alte perché con centinaia di modifiche un elenco
 * compatto si scorre, uno a righe di altezza diversa no.</p>
 */
public class GUI_RipristinoModifiche extends JDialog {

    static final int COL_SCELTA = 0;
    static final int COL_TIPO = 1;
    static final int COL_DATA = 2;
    static final int COL_MOVIMENTO = 3;
    static final int COL_ESITO = 4;
    static final int COL_DETTAGLIO = 5;

    private final DefaultTableModel Modello;
    private final JButton Applica = new JButton();
    private boolean Confermato = false;

    /**
     * Mostra il dialogo e aspetta che venga chiuso.
     *
     * @param Proprietario finestra parent
     * @param Riepilogo testo di riepilogo da mostrare in alto (righe separate da a capo)
     * @param Voci le voci del piano, nell'ordine del piano
     * @param Eseguibile dice se la voce {@code i} si può scegliere di applicare
     * @return gli indici delle voci scelte, {@code null} se l'utente annulla o non sceglie nulla
     */
    public static Set<Integer> Mostra(Window Proprietario, String Riepilogo,
            List<Principale_Opzioni_ModificheManuali_Ripristino.Voce> Voci, IntPredicate Eseguibile) {
        GUI_RipristinoModifiche d = new GUI_RipristinoModifiche(Proprietario, Riepilogo, Voci, Eseguibile);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        if (!d.Confermato) {
            return null;
        }
        Set<Integer> scelte = Selezionati(d.Modello);
        return scelte.isEmpty() ? null : scelte;
    }

    /**
     * Il modello della tabella: una riga per voce, spuntate quelle eseguibili, modificabile solo la spunta delle
     * eseguibili. Separato dalla finestra per poterlo provare senza display.
     */
    static DefaultTableModel CreaModello(List<Principale_Opzioni_ModificheManuali_Ripristino.Voce> Voci, IntPredicate Eseguibile) {
        DefaultTableModel m = new DefaultTableModel(
                new Object[]{"Applica", "Tipo", "Data", "Movimento", "Esito", "Cosa cambia / perché"}, 0) {
            /** @return il tipo della colonna: la prima è una casella di spunta */
            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_SCELTA ? Boolean.class : String.class;
            }

            /** @return {@code true} solo per la spunta delle voci eseguibili */
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_SCELTA && Eseguibile.test(r);
            }
        };
        for (int i = 0; i < Voci.size(); i++) {
            Principale_Opzioni_ModificheManuali_Ripristino.Voce v = Voci.get(i);
            m.addRow(new Object[]{Eseguibile.test(i), v.tipo(), v.quando(), v.movimento() + (v.movimento().isEmpty() ? "" : "  ") + "[" + v.chiave() + "]",
                v.esito().testo(), v.dettaglio()});
        }
        return m;
    }

    /**
     * Il testo del pannello di dettaglio per una voce: chi è (tipo, esito, movimento, data, ID) e il dettaglio completo.
     * Separato dalla finestra per poterlo provare senza display.
     */
    static String TestoDettaglio(Principale_Opzioni_ModificheManuali_Ripristino.Voce v) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tipo: ").append(v.tipo()).append("\n");
        sb.append("Esito: ").append(v.esito().testo()).append("\n");
        if (!v.movimento().isEmpty()) {
            sb.append("Movimento: ").append(v.movimento()).append("\n");
        }
        if (!v.quando().isEmpty()) {
            sb.append("Data: ").append(v.quando()).append("\n");
        }
        sb.append("ID: ").append(v.chiave()).append("\n");
        if (!v.completo().isBlank()) {
            sb.append("\n").append(v.completo());
        }
        return sb.toString();
    }

    private static final String DETTAGLIO_VUOTO = "Seleziona una riga per vederne il dettaglio completo.";

    /** @return gli indici (righe del modello) delle voci spuntate */
    static Set<Integer> Selezionati(TableModel Modello) {
        Set<Integer> s = new LinkedHashSet<>();
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (Boolean.TRUE.equals(Modello.getValueAt(r, COL_SCELTA))) {
                s.add(r);
            }
        }
        return s;
    }

    private GUI_RipristinoModifiche(Window Proprietario, String Riepilogo,
            List<Principale_Opzioni_ModificheManuali_Ripristino.Voce> Voci, IntPredicate Eseguibile) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Ripristino delle modifiche manuali");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        StringBuilder testo = new StringBuilder("<html><body style='width:780px'>");
        testo.append("Scegli quali modifiche riapplicare: quelle spuntate vengono eseguite, le altre restano com'erano. "
                + "Conflitti e movimenti non ritrovati non si possono applicare.<br><br>");
        testo.append(Riepilogo.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>"));
        testo.append("<br>Le modifiche restano in memoria finché non premi Salva in Transazioni Crypto, con Annulla "
                + "vengono scartate, storico compreso. I documenti di supporto collegati dalle classificazioni "
                + "(Dual Investment, Subscription...) non si ripristinano.</body></html>");
        JLabel Intestazione = new JLabel(testo.toString());
        Intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Modello = CreaModello(Voci, Eseguibile);
        //Movimento e dettaglio possono essere più lunghi della colonna: il testo intero compare come suggerimento
        JTable Tabella = new JTable(Modello) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int r = rowAtPoint(e.getPoint());
                int c = columnAtPoint(e.getPoint());
                if (r < 0 || c <= COL_SCELTA) {
                    return null;
                }
                Object v = getValueAt(r, c);
                return v == null || v.toString().isBlank() ? null
                        : "<html><body style='width:500px'>" + v.toString().replace("&", "&amp;").replace("<", "&lt;") + "</body></html>";
            }
        };
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabella.getColumnModel().getColumn(COL_SCELTA).setPreferredWidth(60);
        Tabella.getColumnModel().getColumn(COL_TIPO).setPreferredWidth(130);
        Tabella.getColumnModel().getColumn(COL_DATA).setPreferredWidth(120);
        Tabella.getColumnModel().getColumn(COL_MOVIMENTO).setPreferredWidth(330);
        Tabella.getColumnModel().getColumn(COL_ESITO).setPreferredWidth(100);
        Tabella.getColumnModel().getColumn(COL_DETTAGLIO).setPreferredWidth(420);
        TableRowSorter<DefaultTableModel> Ordinatore = new TableRowSorter<>(Modello);
        Tabella.setRowSorter(Ordinatore);
        //In partenza si vedono solo le modifiche che si possono scegliere
        RowFilter<DefaultTableModel, Integer> SoloEseguibili = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                return Eseguibile.test(e.getIdentifier());
            }
        };
        Ordinatore.setRowFilter(SoloEseguibili);
        JScrollPane Scorrimento = new JScrollPane(Tabella);
        Scorrimento.setPreferredSize(new Dimension(1100, 190));

        //Il dettaglio della riga selezionata, per intero
        JTextArea Dettaglio = new JTextArea(DETTAGLIO_VUOTO);
        Dettaglio.setEditable(false);
        Dettaglio.setLineWrap(true);
        Dettaglio.setWrapStyleWord(true);
        Dettaglio.setMargin(new java.awt.Insets(6, 8, 6, 8));
        JScrollPane ScorrimentoDettaglio = new JScrollPane(Dettaglio);
        ScorrimentoDettaglio.setBorder(BorderFactory.createTitledBorder("Dettaglio della riga selezionata"));
        ScorrimentoDettaglio.setPreferredSize(new Dimension(1100, 300));
        Tabella.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int r = Tabella.getSelectedRow();
            Dettaglio.setText(r < 0 ? DETTAGLIO_VUOTO : TestoDettaglio(Voci.get(Tabella.convertRowIndexToModel(r))));
            Dettaglio.setCaretPosition(0);
        });
        //Si parte dalla prima riga, così il pannello non è mai vuoto davanti a un elenco
        if (Tabella.getRowCount() > 0) {
            Tabella.setRowSelectionInterval(0, 0);
        }
        JSplitPane Diviso = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Scorrimento, ScorrimentoDettaglio);
        //Il dettaglio è il testo lungo: lo spazio in più va a lui, l'elenco compatto ne ha poco bisogno
        Diviso.setResizeWeight(0.3);
        Diviso.setContinuousLayout(true);

        JCheckBox TutteLeRighe = new JCheckBox("Mostra anche le righe senza nulla da applicare");
        TutteLeRighe.addActionListener(e -> Ordinatore.setRowFilter(TutteLeRighe.isSelected() ? null : SoloEseguibili));
        JButton Tutte = new JButton("Spunta tutte");
        Tutte.addActionListener(e -> Spunta(true, Eseguibile));
        JButton Nessuna = new JButton("Togli tutte le spunte");
        Nessuna.addActionListener(e -> Spunta(false, Eseguibile));
        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());
        Applica.addActionListener(e -> {
            if (Tabella.isEditing()) {
                Tabella.getCellEditor().stopCellEditing();
            }
            Confermato = true;
            dispose();
        });
        Modello.addTableModelListener(e -> AggiornaPulsante());
        AggiornaPulsante();

        JPanel Sinistra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Sinistra.add(Tutte);
        Sinistra.add(Nessuna);
        Sinistra.add(TutteLeRighe);
        JPanel Destra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Destra.add(Annulla);
        Destra.add(Applica);
        JPanel Pulsanti = new JPanel(new BorderLayout());
        Pulsanti.add(Sinistra, BorderLayout.WEST);
        Pulsanti.add(Destra, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Intestazione, BorderLayout.NORTH);
        getContentPane().add(Diviso, BorderLayout.CENTER);
        getContentPane().add(Pulsanti, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(Applica);
        pack();
        setMinimumSize(new Dimension(900, 620));
    }

    private void Spunta(boolean Valore, IntPredicate Eseguibile) {
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (Eseguibile.test(r)) {
                Modello.setValueAt(Valore, r, COL_SCELTA);
            }
        }
    }

    private void AggiornaPulsante() {
        int n = Selezionati(Modello).size();
        Applica.setText(n == 0 ? "Applica" : "Applica " + n + (n == 1 ? " modifica" : " modifiche"));
        Applica.setEnabled(n > 0);
    }
}
