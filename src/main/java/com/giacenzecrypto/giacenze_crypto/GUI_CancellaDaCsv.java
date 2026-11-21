package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Anteprima della cancellazione da CSV ({@link Principale_Opzioni_CancellaDaCsv}): come è stato letto il file, quali
 * righe non trovano nulla, l'effetto sui gruppi collegati e l'elenco dei movimenti trovati.
 *
 * <p>Scritto a mano, senza {@code .form}, come {@link GUI_RipristinoModifiche}. Il pulsante si chiama «Prosegui» e
 * non «Elimina»: dopo viene la conferma di sempre della cancellazione, che chiede anche delle commissioni collegate.</p>
 */
public class GUI_CancellaDaCsv extends JDialog {

    /** Quante righe senza corrispondenza si elencano nell'intestazione: le altre si contano */
    private static final int RIGHE_ELENCATE = 15;

    private boolean Confermato = false;

    /**
     * Mostra l'anteprima e aspetta che venga chiusa.
     * @return {@code true} se l'utente vuole proseguire con la cancellazione
     */
    public static boolean Mostra(Window Proprietario, String NomeFile, Principale_Opzioni_CancellaDaCsv.Criteri Criteri,
            Principale_Opzioni_CancellaDaCsv.Esito Esito, Map<String, String[]> Archivio) {
        GUI_CancellaDaCsv d = new GUI_CancellaDaCsv(Proprietario, NomeFile, Criteri, Esito, Archivio);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d.Confermato;
    }

    /** Il testo sopra la tabella, separato dalla finestra per poterlo provare senza display */
    static String TestoIntestazione(String NomeFile, Principale_Opzioni_CancellaDaCsv.Criteri Criteri,
            Principale_Opzioni_CancellaDaCsv.Esito Esito) {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(NomeFile).append("\n");
        int n = Criteri.criteri().size();
        sb.append(Criteri.conIntestazione()
                ? "Righe lette: " + n + ", campi confrontati: " + Campi(Criteri.colonne()) + "."
                : "Elenco di " + n + " ID senza intestazione.");
        if (Criteri.righeVuote() > 0) {
            sb.append(" Righe vuote saltate: ").append(Criteri.righeVuote()).append(".");
        }
        sb.append("\n");
        int t = Esito.ids().size();
        sb.append(t == 1 ? "Trovato 1 movimento." : "Trovati " + t + " movimenti.").append("\n");
        List<Principale_Opzioni_CancellaDaCsv.Criterio> senza = Esito.senzaCorrispondenza();
        if (!senza.isEmpty()) {
            sb.append("\n").append(senza.size() == 1 ? "1 riga del file non trova nessun movimento:"
                    : senza.size() + " righe del file non trovano nessun movimento:").append("\n");
            for (int i = 0; i < Math.min(senza.size(), RIGHE_ELENCATE); i++) {
                sb.append("   ").append(senza.get(i).descrizione()).append("\n");
            }
            if (senza.size() > RIGHE_ELENCATE) {
                sb.append("   ... e altre ").append(senza.size() - RIGHE_ELENCATE).append(".\n");
            }
        }
        if (Esito.conGruppoIncompleto() > 0) {
            sb.append("\n").append(Esito.conGruppoIncompleto() == 1
                    ? "1 movimento trovato fa parte di una classificazione (trasferimento, scambio differito o simili) con movimenti che non sono nell'elenco."
                    : Esito.conGruppoIncompleto() + " movimenti trovati fanno parte di una classificazione (trasferimento, scambio differito o simili) con movimenti che non sono nell'elenco.")
                    .append(" Cancellandoli si annulla la classificazione e quegli altri movimenti tornano da classificare.\n");
        }
        if (Esito.generati() > 0) {
            sb.append("\n").append(Esito.generati() == 1 ? "1 movimento trovato è stato generato da una classificazione."
                    : Esito.generati() + " movimenti trovati sono stati generati da una classificazione.")
                    .append(" Cancellandoli si annulla la classificazione da cui vengono.\n");
        }
        sb.append("\nLa cancellazione resta in memoria finché non premi Salva in Transazioni Crypto, con Annulla viene scartata.");
        return sb.toString();
    }

    private static String Campi(List<Integer> Colonne) {
        StringBuilder sb = new StringBuilder();
        for (int c : Colonne) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c == 0 ? "ID" : "[" + c + "]");
        }
        return sb.toString();
    }

    /** Il modello della tabella dei movimenti trovati, separato dalla finestra per poterlo provare senza display */
    static DefaultTableModel CreaModello(Principale_Opzioni_CancellaDaCsv.Esito Esito, Map<String, String[]> Archivio) {
        DefaultTableModel m = new DefaultTableModel(
                new Object[]{"Riga del file", "Data e ora", "Exchange", "Wallet", "Tipo", "Uscita", "Entrata", "ID"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : String.class;
            }
        };
        for (String id : Esito.ids()) {
            String[] v = Archivio.get(id);
            if (v == null) continue;
            m.addRow(new Object[]{Esito.rigaDi().get(v[0]), FunzioniDate.DataOraAlSecondo(v), v[3], v[4], v[5],
                Gamba(v[10], v[8]), Gamba(v[13], v[11]), v[0]});
        }
        return m;
    }

    private static String Gamba(String Qta, String Moneta) {
        if (Qta == null || Qta.isBlank()) return "";
        return Qta + " " + (Moneta == null ? "" : Moneta);
    }

    private GUI_CancellaDaCsv(Window Proprietario, String NomeFile, Principale_Opzioni_CancellaDaCsv.Criteri Criteri,
            Principale_Opzioni_CancellaDaCsv.Esito Esito, Map<String, String[]> Archivio) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Cancellazione di movimenti da CSV");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String testo = TestoIntestazione(NomeFile, Criteri, Esito);
        JLabel Intestazione = new JLabel("<html><body style='width:820px'>"
                + testo.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</body></html>");
        Intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel Modello = CreaModello(Esito, Archivio);
        JTable Tabella = new JTable(Modello);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabella.setRowSorter(new TableRowSorter<>(Modello));
        int[] larghezze = {80, 130, 100, 100, 190, 170, 170, 300};
        for (int i = 0; i < larghezze.length; i++) {
            Tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        JScrollPane Scorrimento = new JScrollPane(Tabella);
        Scorrimento.setPreferredSize(new Dimension(1100, 380));

        int t = Esito.ids().size();
        JButton Prosegui = new JButton(t == 1 ? "Prosegui con 1 movimento..." : "Prosegui con " + t + " movimenti...");
        Prosegui.addActionListener(e -> {
            Confermato = true;
            dispose();
        });
        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Annulla);
        Pulsanti.add(Prosegui);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Intestazione, BorderLayout.NORTH);
        getContentPane().add(Scorrimento, BorderLayout.CENTER);
        getContentPane().add(Pulsanti, BorderLayout.SOUTH);
        //Annulla è il pulsante di Invio: una cancellazione non parte per un tasto premuto per abitudine
        getRootPane().setDefaultButton(Annulla);
        pack();
        setMinimumSize(new Dimension(900, 500));
    }
}
