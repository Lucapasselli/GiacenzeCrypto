package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Finestra "Verifica la disponibilità dei servizi" (Opzioni - Varie): lancia {@link DiagnosiServizi}, mostra una riga
 * per servizio appena arriva il suo esito (categoria, nome, esito colorato, tempo, dettaglio), poi la diagnosi in parole
 * semplici e un pulsante per copiare tutto il rapporto, da incollare in una segnalazione. Costruita a codice, senza
 * {@code .form}. Il lavoro gira su un thread a parte: la finestra non si blocca e <b>Interrompi</b> ferma la prova.
 *
 * <p>Il rapporto finisce anche nel log del programma (nessuna chiave dentro: {@link DiagnosiServizi#senzaChiavi}), cosi'
 * la prossima segnalazione con "Invia log" lo contiene già.</p>
 */
public class GUI_VerificaServizi extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int COL_ESITO = 2;
    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final DefaultTableModel modello = new DefaultTableModel(
            new Object[]{"Categoria", "Servizio", "Esito", "Tempo (ms)", "Dettaglio"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int riga, int colonna) {
            return false;
        }
    };
    private final JTable tabella = new JTable(modello);
    private final JTextArea diagnosi = new JTextArea(6, 60);
    private final JProgressBar barra = new JProgressBar();
    private final JLabel stato = new JLabel(" ");
    private final JButton avvia = new JButton("Avvia la verifica");
    private final JButton interrompi = new JButton("Interrompi");
    private final JButton copia = new JButton("Copia il rapporto");
    private final JCheckBox tuttiINodi = new JCheckBox("Controlla anche i nodi pubblici di tutte le altre reti (circa 50 richieste in più)");

    private final List<DiagnosiServizi.Esito> esiti = new ArrayList<>();
    private volatile boolean interrotto = false;
    private volatile boolean inCorso = false;
    private String ultimoRapporto = "";

    public static void Mostra(Window owner) {
        GUI_VerificaServizi g = new GUI_VerificaServizi(owner);
        g.setVisible(true);
    }

    private GUI_VerificaServizi(Window owner) {
        super(owner, "Verifica la disponibilità dei servizi", ModalityType.MODELESS);

        JLabel testo = new JLabel("<html><body style='width:880px'>Controlla che i servizi esterni usati dal programma siano raggiungibili e misura quanto "
                + "ci mettono a rispondere: prezzi e cambi, exchange (da Java e da Node.js), explorer e nodi delle blockchain, aggiornamenti. "
                + "Le richieste sono generiche, senza dati tuoi. Alla fine un riepilogo dice cosa non va e quale può essere la causa. "
                + "<b>Copia il rapporto</b> per incollarlo in una segnalazione.</body></html>");
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

        tuttiINodi.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));
        javax.swing.JPanel nord = new javax.swing.JPanel(new BorderLayout());
        nord.add(testo, BorderLayout.NORTH);
        nord.add(tuttiINodi, BorderLayout.SOUTH);

        tabella.setAutoCreateRowSorter(false);
        tabella.setRowHeight(tabella.getRowHeight() + 4);
        tabella.getColumnModel().getColumn(0).setPreferredWidth(190);
        tabella.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabella.getColumnModel().getColumn(COL_ESITO).setPreferredWidth(90);
        tabella.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabella.getColumnModel().getColumn(4).setPreferredWidth(620);
        tabella.getColumnModel().getColumn(COL_ESITO).setCellRenderer(new RendererEsito());
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setPreferredSize(new Dimension(1100, 440));

        diagnosi.setEditable(false);
        diagnosi.setLineWrap(true);
        diagnosi.setWrapStyleWord(true);
        diagnosi.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane scorriDiagnosi = new JScrollPane(diagnosi);
        scorriDiagnosi.setBorder(BorderFactory.createTitledBorder("Diagnosi"));
        scorriDiagnosi.setPreferredSize(new Dimension(1100, 150));

        javax.swing.JPanel centro = new javax.swing.JPanel(new BorderLayout(0, 6));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centro.add(scorri, BorderLayout.CENTER);
        centro.add(scorriDiagnosi, BorderLayout.SOUTH);

        JButton chiudi = new JButton("Chiudi");
        interrompi.setEnabled(false);
        copia.setEnabled(false);
        barra.setIndeterminate(false);
        barra.setPreferredSize(new Dimension(160, 18));
        javax.swing.JPanel pulsanti = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        pulsanti.add(stato);
        pulsanti.add(barra);
        pulsanti.add(avvia);
        pulsanti.add(interrompi);
        pulsanti.add(copia);
        pulsanti.add(chiudi);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(nord, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(pulsanti, BorderLayout.SOUTH);

        avvia.addActionListener(e -> Avvia());
        interrompi.addActionListener(e -> {
            interrotto = true;
            stato.setText("Interruzione in corso...");
            interrompi.setEnabled(false);
        });
        copia.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ultimoRapporto), null);
            stato.setText("Rapporto copiato negli appunti");
        });
        chiudi.addActionListener(e -> Chiudi());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                interrotto = true;
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    private void Chiudi() {
        interrotto = true;
        dispose();
    }

    private void Avvia() {
        if (inCorso) return;
        inCorso = true;
        interrotto = false;
        esiti.clear();
        modello.setRowCount(0);
        diagnosi.setText("");
        avvia.setEnabled(false);
        interrompi.setEnabled(true);
        copia.setEnabled(false);
        barra.setIndeterminate(true);
        stato.setText("Verifica in corso...");

        //Le impostazioni si leggono qui, sul thread della finestra: il database non è fatto per essere letto da un thread qualsiasi.
        String chiave = DatabaseH2.Opzioni_Leggi("ApiKey_Etherscan");
        boolean servizioPrezzi = "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(ServizioPrezziClient.OPZIONE_ABILITATO,
                ServizioPrezziClient.OPZIONE_ABILITATO_DEFAULT));
        DiagnosiServizi.Impostazioni imp = new DiagnosiServizi.Impostazioni(Funzioni.TrasformaNullinBlanc(chiave), servizioPrezzi, tuttiINodi.isSelected());

        Thread t = new Thread(() -> {
            List<DiagnosiServizi.Esito> finali;
            try {
                finali = DiagnosiServizi.EseguiTutto(imp, new DiagnosiServizi.SondaHttp(),
                        x -> SwingUtilities.invokeLater(() -> AggiungiRiga(x)), () -> interrotto);
            } catch (RuntimeException ex) {
                LoggerGC.ScriviErrore(ex);
                finali = new ArrayList<>();
            }
            final List<DiagnosiServizi.Esito> risultato = finali;
            SwingUtilities.invokeLater(() -> Concludi(risultato));
        }, "verifica-servizi-orchestratore");
        t.setDaemon(true);
        t.start();
    }

    private void AggiungiRiga(DiagnosiServizi.Esito x) {
        esiti.add(x);
        modello.addRow(new Object[]{x.categoria(), x.servizio(), x.stato().etichetta(), x.ms() >= 0 ? x.ms() : "", x.dettaglio()});
    }

    private void Concludi(List<DiagnosiServizi.Esito> finali) {
        barra.setIndeterminate(false);
        inCorso = false;
        avvia.setEnabled(true);
        interrompi.setEnabled(false);
        if (finali.isEmpty()) {
            stato.setText(interrotto ? "Verifica interrotta" : "Verifica non riuscita (vedi il log)");
            return;
        }
        //Le righe arrivano in ordine di completamento: a fine prova si raggruppano per categoria.
        List<DiagnosiServizi.Esito> ordinati = DiagnosiServizi.Ordina(finali);
        modello.setRowCount(0);
        for (DiagnosiServizi.Esito x : ordinati) {
            modello.addRow(new Object[]{x.categoria(), x.servizio(), x.stato().etichetta(), x.ms() >= 0 ? x.ms() : "", x.dettaglio()});
        }
        String quando = LocalDateTime.now().format(ORA);
        ultimoRapporto = DiagnosiServizi.Rapporto(ordinati, quando);
        StringBuilder sb = new StringBuilder();
        for (String c : DiagnosiServizi.Conclusioni(ordinati)) sb.append("- ").append(c).append("\n\n");
        diagnosi.setText(sb.toString().trim());
        diagnosi.setCaretPosition(0);
        copia.setEnabled(true);
        stato.setText(interrotto ? "Verifica interrotta (esiti parziali)" : "Verifica completata");
        //Nel log, cosi' una segnalazione fatta dopo lo contiene senza altro.
        System.out.println("=== " + ultimoRapporto.trim().replace("\n", "\n    ") + "\n=== fine verifica servizi");
    }

    /** Colora la cella dell'esito. Tinte chiare con testo scuro: leggibili nel tema chiaro e in quello scuro. */
    private static final class RendererEsito extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        RendererEsito() {
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valore, boolean selezionata, boolean fuoco, int riga, int colonna) {
            Component c = super.getTableCellRendererComponent(t, valore, selezionata, fuoco, riga, colonna);
            Color sfondo = null;
            String v = String.valueOf(valore);
            if (v.equals(DiagnosiServizi.Stato.OK.etichetta())) sfondo = new Color(0xC8E6C9);
            else if (v.equals(DiagnosiServizi.Stato.LENTO.etichetta())) sfondo = new Color(0xFFF59D);
            else if (v.equals(DiagnosiServizi.Stato.AVVISO.etichetta())) sfondo = new Color(0xFFCC80);
            else if (v.equals(DiagnosiServizi.Stato.ERRORE.etichetta())) sfondo = new Color(0xEF9A9A);
            else if (v.equals(DiagnosiServizi.Stato.INFO.etichetta())) sfondo = new Color(0xE0E0E0);
            if (sfondo != null && !selezionata) {
                c.setBackground(sfondo);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
