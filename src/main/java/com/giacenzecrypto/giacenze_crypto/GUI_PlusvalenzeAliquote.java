package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
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
 * Le plusvalenze della tabella dei movimenti divise per aliquota (26% e 33%), con vendite, costi di carico e imposta
 * di ogni aliquota in una tabella sola (nessuna divisione per anno). Scegliendo una riga, una seconda tabella sotto
 * elenca i movimenti che hanno determinato quelle somme. Si apre cliccando il riquadro della plusvalenza nella maschera dei movimenti e
 * riguarda le righe mostrate in quel momento: periodo, filtri di caricamento e filtri di riga. Il dialogo dice
 * <b>quali</b> sono, in un riquadro in testa, perché le somme valgono solo per quelle righe. Solo lettura, a codice,
 * senza {@code .form}. Il calcolo è in {@link Principale_PlusvalenzeAliquote}.
 */
public class GUI_PlusvalenzeAliquote extends JDialog {

    /**
     * Mostra la finestra (modale).
     *
     * @param periodo il periodo scelto in alto, già in testo (vuoto se non c'è)
     * @param filtriCaricamento i criteri attivi della finestra "Filtri" (wallet, moneta, documento...), uno per voce
     * @param filtriRiga il campo di ricerca e i filtri per colonna attivi, uno per voce
     */
    public static void Mostra(Window owner, Principale_PlusvalenzeAliquote.Risultato risultato, String periodo,
            java.util.List<String> filtriCaricamento, java.util.List<String> filtriRiga) {
        new GUI_PlusvalenzeAliquote(owner, risultato, periodo, filtriCaricamento, filtriRiga).setVisible(true);
    }

    /** Il testo del riquadro "righe considerate": periodo e filtri attivi, oppure "nessuno" dove non ce ne sono. */
    static String TestoRigheConsiderate(String periodo, java.util.List<String> filtriCaricamento,
            java.util.List<String> filtriRiga) {
        StringBuilder h = new StringBuilder("<html><div style='width:780px'><b>Righe considerate</b> (quelle che la tabella dei movimenti mostra adesso)<br>");
        h.append("Periodo: ").append(periodo == null || periodo.isBlank() ? "non impostato" : Principale_FiltriMovimenti.EscapeHtml(periodo)).append("<br>");
        h.append("Filtri della finestra Filtri: ").append(elenco(filtriCaricamento)).append("<br>");
        h.append("Ricerca e filtri di colonna: ").append(elenco(filtriRiga));
        return h.append("</div></html>").toString();
    }

    private static String elenco(java.util.List<String> voci) {
        if (voci == null || voci.isEmpty()) return "nessuno";
        StringBuilder s = new StringBuilder();
        for (String v : voci) {
            if (s.length() > 0) s.append("; ");
            s.append(Principale_FiltriMovimenti.EscapeHtml(v));
        }
        return s.toString();
    }

    private GUI_PlusvalenzeAliquote(Window owner, Principale_PlusvalenzeAliquote.Risultato r, String periodo,
            java.util.List<String> filtriCaricamento, java.util.List<String> filtriRiga) {
        super(owner, "Plusvalenze per aliquota", ModalityType.APPLICATION_MODAL);

        JLabel testo = new JLabel("<html><div style='width:900px'>"
                + "Le plusvalenze delle righe mostrate nella tabella divise per aliquota dell'imposta "
                + "sostitutiva. Fino al 31/12/2025 tutto al 26%. Dal 01/01/2026 al 33%, tranne quelle che nascono da un token di "
                + "moneta elettronica in euro, che restano al 26%. Seleziona una riga della tabella in alto per vedere sotto i "
                + "movimenti che hanno determinato quel calcolo."
                + "</div></html>");
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        boolean filtrato = (filtriCaricamento != null && !filtriCaricamento.isEmpty())
                || (filtriRiga != null && !filtriRiga.isEmpty());
        JLabel righe = new JLabel(TestoRigheConsiderate(periodo, filtriCaricamento, filtriRiga));
        righe.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 10, 6, 10),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(filtrato ? Tabelle.ambra : java.awt.Color.GRAY, filtrato ? 2 : 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8))));
        JPanel intestazione = new JPanel(new BorderLayout());
        intestazione.add(testo, BorderLayout.NORTH);
        intestazione.add(righe, BorderLayout.CENTER);

        // Tabella in alto: una riga per aliquota. Quella in basso si riempie con i movimenti della riga scelta.
        JTable perAliquota = tabellaPerAliquota(r);
        DefaultTableModel modelloMovimenti = modelloMovimenti();
        JTable movimenti = tabellaMovimenti(modelloMovimenti);
        JLabel titoloMovimenti = titolo("Movimenti");
        perAliquota.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int riga = perAliquota.getSelectedRow();
            if (riga < 0) return;
            int aliquota = riga < Principale_PlusvalenzeAliquote.ALIQUOTE.length ? Principale_PlusvalenzeAliquote.ALIQUOTE[riga] : 0;
            riempiMovimenti(modelloMovimenti, titoloMovimenti, r, aliquota);
        });

        JPanel alto = new JPanel(new BorderLayout());
        alto.add(titolo("Per aliquota"), BorderLayout.NORTH);
        alto.add(riquadro(perAliquota), BorderLayout.CENTER);
        JPanel basso = new JPanel(new BorderLayout());
        basso.add(titoloMovimenti, BorderLayout.NORTH);
        basso.add(new JScrollPane(movimenti), BorderLayout.CENTER);
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, alto, basso);
        centro.setBorder(null);
        centro.setResizeWeight(0.0);

        JScrollPane note = new JScrollPane(testoToken(r));
        note.setBorder(BorderFactory.createEmptyBorder());
        note.setPreferredSize(new Dimension(940, 110));

        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(chiudi);
        JPanel sud = new JPanel(new BorderLayout());
        sud.add(note, BorderLayout.CENTER);
        sud.add(pulsanti, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(intestazione, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(sud, BorderLayout.SOUTH);
        pack();
        // Il dialogo non esce dallo schermo: i movimenti stanno in una tabella a scorrimento
        java.awt.Rectangle schermo = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(Math.max(getWidth(), 980), schermo.width - 40), Math.min(Math.max(getHeight(), 760), schermo.height - 40));
        setMinimumSize(new Dimension(700, 480));
        setResizable(true);
        setLocationRelativeTo(owner);

        // Apre sulla prima aliquota che ha movimenti (altrimenti sul totale)
        int iniziale = perAliquota.getRowCount() - 1;
        for (int i = 0; i < Principale_PlusvalenzeAliquote.ALIQUOTE.length; i++) {
            if (!r.voci(Principale_PlusvalenzeAliquote.ALIQUOTE[i]).isEmpty()) { iniziale = i; break; }
        }
        perAliquota.setRowSelectionInterval(iniziale, iniziale);
        centro.setDividerLocation(alto.getPreferredSize().height);
    }

    private static final String[] COLONNE = {"Aliquota", "Movimenti", "Vendite (euro)", "Costi di carico (euro)", "Plusvalenza (euro)", "Imposta (euro)"};

    private static JTable tabellaPerAliquota(Principale_PlusvalenzeAliquote.Risultato r) {
        DefaultTableModel m = new DefaultTableModel(COLONNE, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        for (int aliquota : Principale_PlusvalenzeAliquote.ALIQUOTE) {
            Principale_PlusvalenzeAliquote.Riga riga = r.perAliquota.get(aliquota);
            m.addRow(valori(aliquota + "%", riga, Principale_PlusvalenzeAliquote.Imposta(riga == null ? null : riga.plusvalenza, aliquota)));
        }
        m.addRow(valori("Totale", r.totale, r.impostaTotale()));
        JTable t = tabella(m, true);
        t.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        t.getColumnModel().getColumn(0).setPreferredWidth(120);
        return t;
    }

    private static Object[] valori(String etichetta, Principale_PlusvalenzeAliquote.Riga riga, BigDecimal imposta) {
        if (riga == null) return new Object[] {etichetta, "0", euro(BigDecimal.ZERO), euro(BigDecimal.ZERO), euro(BigDecimal.ZERO), euro(imposta)};
        return new Object[] {etichetta, String.valueOf(riga.movimenti), euro(riga.vendite), euro(riga.costi), euro(riga.plusvalenza), euro(imposta)};
    }

    private static final String[] COLONNE_MOVIMENTI = {"Data e Ora", "Exchange / Wallet", "Tipologia", "Moneta Uscita", "Qta Uscita",
        "Moneta Entrata", "Qta Entrata", "Corrispettivo", "Costo di Carico", "Plusvalenza", "Gruppo"};

    private static DefaultTableModel modelloMovimenti() {
        return new DefaultTableModel(COLONNE_MOVIMENTI, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
    }

    private static JTable tabellaMovimenti(DefaultTableModel m) {
        JTable t = new JTable(m);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(t);
        DefaultTableCellRenderer cella = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tab, Object valore, boolean sel, boolean fuoco, int riga, int colonna) {
                java.awt.Component c = super.getTableCellRendererComponent(tab, valore, sel, fuoco, riga, colonna);
                boolean numerica = colonna == 4 || colonna == 6 || colonna == 7 || colonna == 8 || colonna == 9;
                setHorizontalAlignment(numerica ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        };
        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(cella);
        }
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] larghezze = {125, 130, 60, 70, 70, 70, 70, 90, 90, 90, 260};
        for (int c = 0; c < larghezze.length; c++) t.getColumnModel().getColumn(c).setPreferredWidth(larghezze[c]);
        t.setAutoCreateRowSorter(false);
        return t;
    }

    private static void riempiMovimenti(DefaultTableModel m, JLabel titolo, Principale_PlusvalenzeAliquote.Risultato r, int aliquota) {
        m.setRowCount(0);
        java.util.List<Principale_PlusvalenzeAliquote.Voce> voci = r.voci(aliquota);
        for (Principale_PlusvalenzeAliquote.Voce voce : voci) {
            String[] v = voce.movimento();
            boolean calcolata = "S".equals(v[33]);
            m.addRow(new Object[] {
                FunzioniDate.DataOraAlSecondo(v), (v[3] + " " + v[4]).trim(), v[5],
                v[8], v[10], v[11], v[13],
                calcolata ? v[15] : "", calcolata ? v[16] : "", v[19],
                voce.categoria().etichetta});
        }
        titolo.setText((aliquota > 0 ? "Movimenti al " + aliquota + "%" : "Tutti i movimenti") + ": " + voci.size());
    }

    private static String euro(BigDecimal valore) {
        return Funzioni.formattaBigDecimal(valore, true);
    }

    private static JTable tabella(DefaultTableModel modello, boolean ultimaInGrassetto) {
        JTable t = new JTable(modello);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(t);
        DefaultTableCellRenderer cella = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tab, Object valore, boolean sel, boolean fuoco, int riga, int colonna) {
                java.awt.Component c = super.getTableCellRendererComponent(tab, valore, sel, fuoco, riga, colonna);
                setHorizontalAlignment(colonna == 0 ? SwingConstants.LEFT : colonna == 1 ? SwingConstants.CENTER : SwingConstants.RIGHT);
                boolean grassetto = ultimaInGrassetto && riga == tab.getRowCount() - 1;
                setFont(tab.getFont().deriveFont(grassetto ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
                return c;
            }
        };
        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(cella);
        }
        t.getColumnModel().getColumn(0).setPreferredWidth(340);
        t.getColumnModel().getColumn(1).setPreferredWidth(70);
        return t;
    }

    private static JScrollPane riquadro(JTable tabella) {
        JScrollPane s = new JScrollPane(tabella);
        int altezza = tabella.getTableHeader().getPreferredSize().height + tabella.getRowCount() * tabella.getRowHeight() + 4;
        s.setPreferredSize(new Dimension(880, altezza));
        s.setAlignmentX(LEFT_ALIGNMENT);
        return s;
    }

    private static JLabel titolo(String testo) {
        JLabel l = new JLabel(testo);
        l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
        l.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel testoToken(Principale_PlusvalenzeAliquote.Risultato r) {
        StringBuilder sb = new StringBuilder("<html><div style='width:780px'>");
        if (!r.elencoDisponibile) {
            sb.append("L'elenco dei token di moneta elettronica (EmoneyToken.json) non è disponibile: nessun token è risultato in euro.");
        } else {
            sb.append("Al 26%: ").append(r.tokenEuro.isEmpty() ? "nessuno." : elenco(r.tokenEuro)).append("<br>");
            sb.append("Token di moneta elettronica ma non in euro (33%): ")
                    .append(r.tokenEmtNonEuro.isEmpty() ? "nessuno." : elenco(r.tokenEmtNonEuro)).append("<br><br>");
            sb.append("Un token conta come in euro solo se è E-Money nelle opzioni alla data del movimento e l'elenco dei token "
                    + "lo dichiara denominato in euro. Il nome del token non basta.");
        }
        sb.append("<br><br>L'imposta mostrata è un'indicazione: plusvalenza di ogni aliquota per l'aliquota stessa, con tutti gli anni "
                + "insieme, e zero dove la somma è negativa. Non tiene conto di compensazioni fra le due aliquote o con "
                + "minusvalenze di altri anni, né delle regole del quadro RT dei singoli anni (per esempio la soglia dei "
                + "51.645,69 euro per gli anni fino al 2022).</div></html>");
        JLabel l = new JLabel(sb.toString());
        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static String elenco(Map<String, Integer> token) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : token.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("<b>").append(e.getKey().replace("<", "&lt;")).append("</b> (").append(e.getValue())
                    .append(e.getValue() == 1 ? " movimento)" : " movimenti)");
        }
        return sb.append('.').toString();
    }
}
