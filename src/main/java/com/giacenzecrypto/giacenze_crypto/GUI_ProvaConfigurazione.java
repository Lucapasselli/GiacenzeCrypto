package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Esito della prova di una configurazione di importazione ({@link ProvaConfigurazioneImport}): cosa produrrebbe il
 * CSV, cosa verrebbe scartato e perche', senza scrivere nulla nell'archivio.
 *
 * <p>Scritto a mano, senza {@code .form}: il contenuto e' tutto dinamico. "Ripeti la prova" rilegge configurazione e
 * CSV, per chi corregge il JSON in un editor (o con un'IA) e vuole vedere subito l'effetto. "Copia resoconto" mette
 * negli appunti {@link ProvaConfigurazioneImport.Esito#TestoPerIA()}.
 */
public class GUI_ProvaConfigurazione extends JDialog {

    private final File Json;
    private final File Csv;
    private ProvaConfigurazioneImport.Esito Esito;

    private final JLabel Intestazione = new JLabel();
    private final JTabbedPane Schede = new JTabbedPane();
    private final DefaultTableModel ModelloMovimenti = Modello("Data", "Categoria", "Tipo", "Uscita", "Entrata", "Wallet",
            "Classificazione", "Causali del CSV");
    private final DefaultTableModel ModelloRiepilogo = Modello("Causali del CSV (gruppo di righe)", "Tipi interni", "Gruppi",
            "Movimenti prodotti");
    private final DefaultTableModel ModelloSaldi = Modello("Wallet", "Moneta", "Entrate", "Uscite", "Saldo");
    private final DefaultTableModel ModelloScarti = Modello("Motivo", "Riga del CSV");
    private final JButton Ripeti = new JButton("Ripeti la prova");
    private final JButton Copia = new JButton("Copia resoconto");
    private final JButton Condividi = new JButton("Condividi con la community...");

    /**
     * Esegue la prova in background (cursore d'attesa sul proprietario) e ne mostra l'esito.
     * @param Proprietario la finestra da cui parte la prova
     * @param Json la configurazione da provare
     * @param Csv il file da leggere
     */
    public static void Avvia(Window Proprietario, File Json, File Csv) {
        Proprietario.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ProvaConfigurazioneImport.Esito, Void>() {
            /** Esegue la prova fuori dall'EDT. */
            @Override
            protected ProvaConfigurazioneImport.Esito doInBackground() {
                return ProvaConfigurazioneImport.Esegui(Json, Csv);
            }

            /** Mostra la finestra con l'esito. */
            @Override
            protected void done() {
                Proprietario.setCursor(Cursor.getDefaultCursor());
                try {
                    GUI_ProvaConfigurazione d = new GUI_ProvaConfigurazione(Proprietario, Json, Csv, get());
                    d.setLocationRelativeTo(Proprietario);
                    d.setVisible(true);
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    JOptionPane.showMessageDialog(Proprietario, "La prova non e' riuscita: " + ex.getMessage(),
                            "Prova della configurazione", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private GUI_ProvaConfigurazione(Window Proprietario, File Json, File Csv, ProvaConfigurazioneImport.Esito Esito) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        this.Json = Json;
        this.Csv = Csv;
        setTitle("Prova della configurazione di importazione");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Intestazione.setVerticalAlignment(JLabel.TOP);

        Schede.addTab("Movimenti", Scorrimento(ModelloMovimenti, 150, 60, 140, 170, 170, 110, 160, 260));
        Schede.addTab("Riepilogo per causale", Scorrimento(ModelloRiepilogo, 380, 300, 70, 270));
        Schede.addTab("Saldi per moneta", Scorrimento(ModelloSaldi, 200, 140, 200, 200, 200));
        Schede.addTab("Righe scartate", Scorrimento(ModelloScarti, 230, 790));

        Ripeti.setToolTipText("Rilegge la configurazione e il CSV: per vedere subito l'effetto di una correzione al JSON");
        Ripeti.addActionListener(e -> Ripeti());
        Copia.setToolTipText("<html>Copia il resoconto in testo semplice, da incollare in un chatbot insieme alla configurazione.<br>"
                + "Contiene le righe scartate cosi' come sono nel CSV: controlla che non ci siano dati che non vuoi condividere.</html>");
        Copia.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(this.Esito.TestoPerIA()), null);
            JOptionPane.showMessageDialog(this, "Resoconto copiato negli appunti.\n\n"
                    + "Contiene le righe scartate cosi' come sono nel CSV: prima di incollarlo controlla che non ci siano\n"
                    + "dati che non vuoi condividere, come identificativi o indirizzi.",
                    "Copia resoconto", JOptionPane.INFORMATION_MESSAGE);
        });
        Condividi.setToolTipText("<html>Propone la configurazione al catalogo della community, dopo una prova senza problemi.<br>"
                + "Diventa scaricabile dagli altri utenti solo dopo il controllo dell'autore del programma.</html>");
        Condividi.addActionListener(e -> GUI_CondividiConfigurazione.Mostra(this, this.Json, this.Esito));
        JButton Chiudi = new JButton("Chiudi");
        Chiudi.addActionListener(e -> dispose());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Ripeti);
        Pulsanti.add(Copia);
        Pulsanti.add(Condividi);
        Pulsanti.add(Chiudi);

        JPanel Contenuto = new JPanel(new BorderLayout());
        Contenuto.add(Intestazione, BorderLayout.NORTH);
        Contenuto.add(Schede, BorderLayout.CENTER);
        Contenuto.add(Pulsanti, BorderLayout.SOUTH);
        setContentPane(Contenuto);
        Mostra(Esito);
        pack();
    }

    private static DefaultTableModel Modello(String... colonne) {
        return new DefaultTableModel(colonne, 0) {
            /** @return sempre {@code false}: la finestra mostra, non modifica */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private static JScrollPane Scorrimento(DefaultTableModel Modello, int... larghezze) {
        JTable Tabella = new JTable(Modello);
        Tabella.setAutoCreateRowSorter(true);
        Tabella.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        for (int i = 0; i < larghezze.length; i++) {
            Tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        JScrollPane s = new JScrollPane(Tabella);
        s.setPreferredSize(new Dimension(1100, 380));
        return s;
    }

    /** Riempie intestazione e tabelle con l'esito */
    private void Mostra(ProvaConfigurazioneImport.Esito E) {
        Esito = E;
        StringBuilder t = new StringBuilder("<html><body style='width:1050px'>");
        t.append("Configurazione <b>").append(Html(E.fileConfigurazione.getName())).append("</b> sul file <b>")
                .append(Html(E.fileCSV == null ? "" : E.fileCSV.getName())).append("</b>. Nessun movimento e' stato scritto.<br>");
        if (E.errore != null) {
            t.append("<br><font color='#C0392B'><b>").append(Html(E.errore)).append("</b></font>");
        } else {
            t.append("Exchange <b>").append(Html(E.nomeExchange)).append("</b>, fuso <b>").append(Html(E.fuso))
                    .append("</b>: <b>").append(E.righeLette).append("</b> righe lette, <b>").append(E.movimenti.size())
                    .append("</b> movimenti, <b>").append(E.scarti.size()).append("</b> righe scartate.<br>");
            if (!E.chiaviSconosciute.isEmpty()) {
                t.append("<font color='#C0392B'><b>Opzioni non riconosciute, ignorate:</b> ")
                        .append(Html(String.join(", ", E.chiaviSconosciute))).append("</font><br>");
            }
            if (!E.tipiSconosciuti.isEmpty()) {
                t.append("<font color='#C0392B'><b>Valori che non sono tipi di movimento</b> (quelle righe diventano movimenti "
                        + "generici, classificati solo dalle monete): ").append(Html(String.join(", ", E.tipiSconosciuti)))
                        .append("</font><br>");
            }
            if (!E.ciSonoProblemi()) {
                t.append("Nessuna riga scartata e nessuna opzione sconosciuta. Controlla nel riepilogo che ogni causale "
                        + "produca i movimenti attesi.<br>");
            }
            for (String n : E.note) t.append("<font color='#7F8C8D'>").append(Html(n)).append("</font><br>");
        }
        t.append("</body></html>");
        Intestazione.setText(t.toString());

        ModelloMovimenti.setRowCount(0);
        for (ProvaConfigurazioneImport.Movimento m : E.movimenti) {
            String[] v = m.v();
            ModelloMovimenti.addRow(new Object[]{v[1], m.categoria(), v[5],
                ProvaConfigurazioneImport.Gamba(v[10], v[8]), ProvaConfigurazioneImport.Gamba(v[13], v[11]), v[4], v[18],
                m.causali()});
        }
        ModelloRiepilogo.setRowCount(0);
        for (ProvaConfigurazioneImport.Riepilogo r : E.riepilogo) {
            ModelloRiepilogo.addRow(new Object[]{r.causali(), r.tipi(), r.gruppi(), r.testoCategorie()});
        }
        ModelloSaldi.setRowCount(0);
        for (ProvaConfigurazioneImport.Saldo s : E.saldi) {
            ModelloSaldi.addRow(new Object[]{s.wallet(), s.moneta(), s.entrate().toPlainString(),
                s.uscite().toPlainString(), s.saldo().toPlainString()});
        }
        ModelloScarti.setRowCount(0);
        for (String r : E.scarti) {
            int i = r.indexOf(" | ");
            ModelloScarti.addRow(i < 0 ? new Object[]{r, ""} : new Object[]{r.substring(0, i), r.substring(i + 3)});
        }
        Schede.setTitleAt(0, "Movimenti (" + E.movimenti.size() + ")");
        Schede.setTitleAt(1, "Riepilogo per causale (" + E.riepilogo.size() + ")");
        Schede.setTitleAt(2, "Saldi per moneta (" + E.saldi.size() + ")");
        Schede.setTitleAt(3, "Righe scartate (" + E.scarti.size() + ")");
        if (!E.scarti.isEmpty() && E.movimenti.isEmpty()) Schede.setSelectedIndex(3);
        //Si condivide solo una configurazione provata senza problemi
        Condividi.setEnabled(CommunityConfigurazioni.ProvaSenzaProblemi(E));
    }

    /** Rilegge configurazione e CSV e aggiorna la finestra */
    private void Ripeti() {
        Ripeti.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ProvaConfigurazioneImport.Esito, Void>() {
            /** Esegue di nuovo la prova fuori dall'EDT. */
            @Override
            protected ProvaConfigurazioneImport.Esito doInBackground() {
                return ProvaConfigurazioneImport.Esegui(Json, Csv);
            }

            /** Aggiorna intestazione e tabelle. */
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                Ripeti.setEnabled(true);
                try {
                    Mostra(get());
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
            }
        }.execute();
    }

    private static String Html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
