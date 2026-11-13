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
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Le plusvalenze della tabella dei movimenti divise per aliquota (26% e 33%), con vendite e costi di carico di ogni
 * gruppo, per anno e per gruppo. Si apre cliccando il riquadro della plusvalenza nella maschera dei movimenti e
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

        JLabel testo = new JLabel("<html><div style='width:780px'>"
                + "Le plusvalenze delle righe mostrate nella tabella divise per aliquota dell'imposta "
                + "sostitutiva. Fino al 31/12/2025 tutto al 26%. Dal 01/01/2026 al 33%, tranne quelle che nascono da un token di "
                + "moneta elettronica in euro, che restano al 26%. Il programma attribuisce un movimento al 26% se la gamba da cui "
                + "nasce la plusvalenza (quella uscita, altrimenti quella entrata) è uno di questi token. Vendite e costi contano "
                + "come nei riquadri della tabella."
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

        JPanel centro = new JPanel();
        centro.setLayout(new javax.swing.BoxLayout(centro, javax.swing.BoxLayout.Y_AXIS));
        centro.add(titolo("Per aliquota"));
        centro.add(riquadro(tabellaPerAliquota(r)));
        centro.add(titolo("Per anno e aliquota"));
        centro.add(riquadro(tabellaPerAnno(r)));
        centro.add(titolo("Token di moneta elettronica nelle plusvalenze dal 2026"));
        centro.add(testoToken(r));

        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(chiudi);

        setLayout(new BorderLayout());
        add(intestazione, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(pulsanti, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private static final String[] COLONNE = {"Movimenti", "Vendite (euro)", "Costi di carico (euro)", "Plusvalenza (euro)"};

    private static JTable tabellaPerAliquota(Principale_PlusvalenzeAliquote.Risultato r) {
        DefaultTableModel m = modello("Gruppo");
        for (Principale_PlusvalenzeAliquote.Categoria c : Principale_PlusvalenzeAliquote.Categoria.values()) {
            m.addRow(valori(c.etichetta, c.aliquota + "%", r.perCategoria.get(c)));
        }
        m.addRow(valori("Totale", "", r.totale));
        return tabella(m, true);
    }

    private static JTable tabellaPerAnno(Principale_PlusvalenzeAliquote.Risultato r) {
        DefaultTableModel m = modello("Anno e gruppo");
        for (Map.Entry<String, Map<Principale_PlusvalenzeAliquote.Categoria, Principale_PlusvalenzeAliquote.Riga>> anno : r.perAnno.entrySet()) {
            for (Principale_PlusvalenzeAliquote.Categoria c : Principale_PlusvalenzeAliquote.Categoria.values()) {
                Principale_PlusvalenzeAliquote.Riga riga = anno.getValue().get(c);
                if (riga == null) continue;
                m.addRow(valori(anno.getKey() + (c == Principale_PlusvalenzeAliquote.Categoria.EMT_EURO
                        ? ", token di moneta elettronica in euro" : ""), c.aliquota + "%", riga));
            }
        }
        if (m.getRowCount() == 0) {
            m.addRow(new Object[] {"Nessuna plusvalenza nelle righe mostrate", "", "", "", "", ""});
        }
        return tabella(m, false);
    }

    private static DefaultTableModel modello(String primaColonna) {
        DefaultTableModel m = new DefaultTableModel(new Object[] {primaColonna, "Aliquota", COLONNE[0], COLONNE[1], COLONNE[2], COLONNE[3]}, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        return m;
    }

    private static Object[] valori(String etichetta, String aliquota, Principale_PlusvalenzeAliquote.Riga riga) {
        if (riga == null) return new Object[] {etichetta, aliquota, "0", euro(BigDecimal.ZERO), euro(BigDecimal.ZERO), euro(BigDecimal.ZERO)};
        return new Object[] {etichetta, aliquota, String.valueOf(riga.movimenti), euro(riga.vendite), euro(riga.costi), euro(riga.plusvalenza)};
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
        sb.append("<br><br>Il programma non calcola l'imposta: queste sono somme informative, e non tengono conto di compensazioni "
                + "fra minusvalenze e plusvalenze né di altri elementi della dichiarazione.</div></html>");
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
