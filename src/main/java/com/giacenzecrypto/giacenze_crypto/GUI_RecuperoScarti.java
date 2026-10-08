package com.giacenzecrypto.giacenze_crypto;

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
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Conferma del recupero dei movimenti trovati nei documenti di origine ({@link ScartiImport}).
 *
 * <p>Scritto a mano, senza {@code .form}: il contenuto è tutto dinamico. Il recupero è <b>sempre</b> confermato
 * dall'utente (decisione del 2026-10-08), anche per i candidati certi, che sono solo spuntati in partenza. Gli
 * incerti partono non spuntati: possono essere movimenti che l'utente ha cancellato di proposito. I misti si
 * vedono ma non si possono scegliere.
 *
 * <p>Alla conferma, ciò che non è spuntato (misti compresi) viene segnato come ignorato e non verrà più
 * proposto; con "Chiedi più tardi" non si segna nulla.
 */
public class GUI_RecuperoScarti extends JDialog {

    /** Scelta dell'utente alla chiusura */
    public enum Scelta { RECUPERA, PIU_TARDI }

    private static final int COL_SCELTA = 0;

    private final List<ScartiImport.Candidato> Righe = new ArrayList<>();
    private final DefaultTableModel Modello;
    private Scelta SceltaFatta = Scelta.PIU_TARDI;

    /**
     * Mostra il dialogo e aspetta che venga chiuso.
     *
     * @param Proprietario finestra parent
     * @param A l'analisi da mostrare
     * @return il dialogo chiuso, da cui leggere {@link #Scelta()} e {@link #Selezionati()}
     */
    public static GUI_RecuperoScarti Mostra(Window Proprietario, ScartiImport.Analisi A) {
        GUI_RecuperoScarti d = new GUI_RecuperoScarti(Proprietario, A);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d;
    }

    private GUI_RecuperoScarti(Window Proprietario, ScartiImport.Analisi A) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Movimenti recuperabili dai documenti di origine");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Righe.addAll(A.Recuperabili);
        Righe.addAll(A.Misti);

        int certi = 0;
        for (ScartiImport.Candidato c : A.Recuperabili) {
            if (c.Certo) certi++;
        }
        int incerti = A.Recuperabili.size() - certi;

        StringBuilder testo = new StringBuilder("<html><body style='width:640px'>");
        testo.append("Nei documenti degli scaricamenti OKX ci sono movimenti che l'archivio non contiene e che le "
                + "mappe attuali sanno importare.<br><br>");
        if (certi > 0) {
            testo.append("<b>").append(certi).append("</b> erano stati scartati perché in quel momento la loro causale "
                    + "non era riconosciuta: gli scaricamenti successivi non li hanno più richiesti. Sono già spuntati.<br>");
        }
        if (incerti > 0) {
            testo.append("<b>").append(incerti).append("</b> non risultano in archivio, ma non c'è traccia che siano "
                    + "stati scartati: possono anche essere movimenti eliminati di proposito. Spuntali solo se li riconosci.<br>");
        }
        if (!A.Misti.isEmpty()) {
            testo.append("<b>").append(A.Misti.size()).append("</b> sono stati importati solo in parte e non si possono "
                    + "recuperare da qui: vanno sistemati a mano.<br>");
        }
        testo.append("<br>Quelli non spuntati non verranno più proposti. I movimenti recuperati vanno poi salvati "
                + "come dopo ogni importazione.</body></html>");
        JLabel Intestazione = new JLabel(testo.toString());
        Intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Modello = new DefaultTableModel(new Object[]{"Recupera", "Data", "Causale", "Movimento", "Documento", "Motivo"}, 0) {
            /** @return il tipo della colonna: la prima è una casella di spunta */
            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_SCELTA ? Boolean.class : String.class;
            }

            /** @return {@code true} solo per la spunta dei candidati recuperabili */
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_SCELTA && !Righe.get(r).Misto;
            }
        };
        for (ScartiImport.Candidato c : Righe) {
            String motivo = c.Misto ? "Importato solo in parte, da sistemare a mano"
                    : c.Certo ? "Scartato perché sconosciuto" : "Non presente in archivio";
            Modello.addRow(new Object[]{!c.Misto && c.Certo, c.Data(), c.Causali(), c.Movimento(),
                c.IdDocumento + " - " + c.NomeDocumento, motivo});
        }
        JTable Tabella = new JTable(Modello);
        Tabella.setAutoCreateRowSorter(true);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabella.getColumnModel().getColumn(COL_SCELTA).setPreferredWidth(70);
        Tabella.getColumnModel().getColumn(1).setPreferredWidth(140);
        Tabella.getColumnModel().getColumn(2).setPreferredWidth(180);
        Tabella.getColumnModel().getColumn(3).setPreferredWidth(220);
        Tabella.getColumnModel().getColumn(4).setPreferredWidth(220);
        Tabella.getColumnModel().getColumn(5).setPreferredWidth(240);
        JScrollPane Scorrimento = new JScrollPane(Tabella);
        Scorrimento.setPreferredSize(new Dimension(1000, 320));

        JButton Tutti = new JButton("Spunta tutti");
        Tutti.addActionListener(e -> Spunta(true));
        JButton Nessuno = new JButton("Togli tutte le spunte");
        Nessuno.addActionListener(e -> Spunta(false));
        JButton Dopo = new JButton("Chiedi più tardi");
        Dopo.setToolTipText("Chiude senza recuperare nulla e senza segnare nulla come ignorato");
        Dopo.addActionListener(e -> {
            SceltaFatta = Scelta.PIU_TARDI;
            dispose();
        });
        JButton Conferma = new JButton("Conferma");
        Conferma.setToolTipText("Recupera i movimenti spuntati e non propone più gli altri");
        Conferma.addActionListener(e -> {
            if (Tabella.isEditing()) Tabella.getCellEditor().stopCellEditing();
            SceltaFatta = Scelta.RECUPERA;
            dispose();
        });

        JPanel Sinistra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Sinistra.add(Tutti);
        Sinistra.add(Nessuno);
        JPanel Destra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Destra.add(Dopo);
        Destra.add(Conferma);
        JPanel Pulsanti = new JPanel(new BorderLayout());
        Pulsanti.add(Sinistra, BorderLayout.WEST);
        Pulsanti.add(Destra, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Intestazione, BorderLayout.NORTH);
        getContentPane().add(Scorrimento, BorderLayout.CENTER);
        getContentPane().add(Pulsanti, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(Conferma);
        pack();
        setMinimumSize(new Dimension(800, 400));
    }

    private void Spunta(boolean Valore) {
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (!Righe.get(r).Misto) Modello.setValueAt(Valore, r, COL_SCELTA);
        }
    }

    /** @return la scelta fatta; chiudere la finestra equivale a "Chiedi più tardi" */
    public Scelta Scelta() {
        return SceltaFatta;
    }

    /** @return i candidati spuntati */
    public List<ScartiImport.Candidato> Selezionati() {
        List<ScartiImport.Candidato> s = new ArrayList<>();
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (Boolean.TRUE.equals(Modello.getValueAt(r, COL_SCELTA))) s.add(Righe.get(r));
        }
        return s;
    }

    /** @return i candidati non spuntati, misti compresi: alla conferma vengono segnati come ignorati */
    public List<ScartiImport.Candidato> NonSelezionati() {
        List<ScartiImport.Candidato> s = new ArrayList<>();
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (!Boolean.TRUE.equals(Modello.getValueAt(r, COL_SCELTA))) s.add(Righe.get(r));
        }
        return s;
    }
}
