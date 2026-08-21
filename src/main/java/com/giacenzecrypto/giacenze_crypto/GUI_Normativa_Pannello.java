package com.giacenzecrypto.giacenze_crypto;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Parte grafica del tab "Normative": elenca i documenti di {@code Normativa/fonti.csv}, li filtra
 * per categoria/data/testo, ne mostra il dettaglio e apre una copia del file o la fonte ufficiale.
 *
 * <p>Sul modello di {@link GUI_DocumentiFonte_Pannello}: {@code JPanel} senza dipendenza da
 * {@link Principale}, logica operativa in {@link Principale_Normativa}. A differenza di quel
 * pannello questo non è condiviso con un dialogo, quindi non ha un pulsante "Chiudi" né bisogno di
 * risolvere il proprietario più di una volta per operazione.
 *
 * <p>I filtri sono tutti {@link RowFilter} lato client: l'intero catalogo (poche centinaia di righe)
 * sta in memoria, quindi non serve il meccanismo a due livelli load-filter/RowFilter usato per la
 * tabella dei movimenti, pensato per una scala e per query sul DB che qui non si pongono.
 */
public class GUI_Normativa_Pannello extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Il catalogo corrente, nello stesso ordine delle righe del modello (riga di vista → questa lista via convertRowIndexToModel). */
    private List<DocumentoNormativa> Catalogo = new ArrayList<>();

    /** Testo estratto dei documenti, popolato in background da {@link Principale_Normativa#PrecaricaTesti}. */
    private final ConcurrentHashMap<String, String> TestiCache = new ConcurrentHashMap<>();

    /** {@code true} mentre un thread di indicizzazione è in corso, per non farne partire due insieme. */
    private volatile boolean Indicizzazione = false;

    private TableRowSorter<DefaultTableModel> Sorter;

    public GUI_Normativa_Pannello() {
        initComponents();

        Icone.AdattaIconeAlTema(this);

        //Le due date scelte dal calendario non passano da un renderer che le ridipinge a ogni paint
        //(sono i selettori fissi della barra filtri, non una cella di tabella): il testo va reso
        //leggibile una volta sola, qui, sullo sfondo che il tema scuro darebbe altrimenti nero su nero.
        ((JTextFieldDateEditor) DataChooser_Dal.getDateEditor()).setBackground(Color.lightGray);
        ((JTextFieldDateEditor) DataChooser_Al.getDateEditor()).setBackground(Color.lightGray);

        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabelle.ColoraTabellaSemplice(Tabella);

        for (String etichetta : Principale_Normativa.EtichetteCategorie()) {
            ComboBox_Categoria.addItem(etichetta);
        }

        Sorter = new TableRowSorter<>(Modello());
        Tabella.setRowSorter(Sorter);

        Tabella.getSelectionModel().addListSelectionListener(e -> AggiornaDettaglio());

        ComboBox_Categoria.addActionListener(e -> AggiornaFiltro());
        CheckBox_CercaTesto.addItemListener(e -> AggiornaFiltro());
        TextField_Ricerca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { AggiornaFiltro(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { AggiornaFiltro(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { AggiornaFiltro(); }
        });
        DataChooser_Dal.addPropertyChangeListener("date", e -> AggiornaFiltro());
        DataChooser_Al.addPropertyChangeListener("date", e -> AggiornaFiltro());

        Ricarica();
    }

    private Window Proprietario() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private DefaultTableModel Modello() {
        return (DefaultTableModel) Tabella.getModel();
    }

    /** Ricarica il catalogo da {@code fonti.csv} e riavvia l'indicizzazione del testo. */
    public void Ricarica() {
        Component c = Proprietario() != null ? Proprietario() : this;
        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Catalogo = Principale_Normativa.LeggiCatalogo();
            TestiCache.clear();

            Modello().setRowCount(0);
            for (DocumentoNormativa d : Catalogo) {
                Modello().addRow(new Object[]{
                    d.titolo(), d.categoria(), d.autorita(), d.identificativo(),
                    d.ufficiale() ? "Ufficiale" : "Derivato",
                    d.dataDocumento() != null ? d.dataDocumento().format(FORMATO_DATA) : ""
                });
            }
            AdattaLarghezzaColonne();
        } finally {
            c.setCursor(Cursor.getDefaultCursor());
        }

        AggiornaFiltro();
        AggiornaDettaglio();
        AvviaIndicizzazione();
    }

    /**
     * Estrae e cachea il testo di ogni documento su un thread separato, aggiornando
     * {@link #Label_Stato} man mano. Con qualche centinaio di documenti di poche pagine il costo è
     * contenuto, ma non va comunque messo sull'EDT né prima del primo paint del tab.
     */
    private void AvviaIndicizzazione() {
        if (Catalogo.isEmpty()) {
            AggiornaLabelStato();
            return;
        }
        Indicizzazione = true;
        List<DocumentoNormativa> catalogoCorrente = Catalogo;
        AtomicInteger fatti = new AtomicInteger(0);
        Thread t = new Thread(() -> {
            Principale_Normativa.PrecaricaTesti(catalogoCorrente, TestiCache, () -> {
                int n = fatti.incrementAndGet();
                SwingUtilities.invokeLater(() -> {
                    if (catalogoCorrente != Catalogo) {
                        return; //un Ricarica() più recente ha già sostituito il catalogo: questo giro non conta più
                    }
                    if (n < catalogoCorrente.size()) {
                        Label_Stato.setText("Indicizzazione del testo: " + n + " di " + catalogoCorrente.size() + "...");
                        if (CheckBox_CercaTesto.isSelected() && !TextField_Ricerca.getText().isBlank()) {
                            AggiornaFiltro();
                        }
                    } else {
                        Indicizzazione = false;
                        AggiornaFiltro();
                    }
                });
            });
        }, "Normativa-Indicizzazione");
        t.setDaemon(true);
        t.start();
    }

    private void AggiornaLabelStato() {
        if (Catalogo.isEmpty()) {
            Label_Stato.setText(new java.io.File(VarStatiche.getCartella_Normativa()).exists()
                    ? "Nessun documento nell'archivio."
                    : "Archivio non ancora scaricato. Premi \"Aggiorna da GitHub\" per scaricarlo.");
            return;
        }
        int visualizzate = Sorter != null ? Sorter.getViewRowCount() : Modello().getRowCount();
        Label_Stato.setText(visualizzate == Catalogo.size()
                ? Catalogo.size() + " documenti"
                : visualizzate + " documenti su " + Catalogo.size());
    }

    // ------------------------------------------------------------------ filtro

    private void AggiornaFiltro() {
        String categoria = (String) ComboBox_Categoria.getSelectedItem();
        LocalDate dal = DataLocale(DataChooser_Dal.getDate());
        LocalDate al = DataLocale(DataChooser_Al.getDate());
        boolean filtraData = dal != null || al != null;
        String ricerca = TextField_Ricerca.getText().trim().toLowerCase(Locale.ITALIAN);
        boolean cercaTesto = CheckBox_CercaTesto.isSelected();

        Sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                DocumentoNormativa d = Catalogo.get(entry.getIdentifier());

                if (!Principale_Normativa.TUTTE_LE_CATEGORIE.equals(categoria)
                        && !categoria.equals(d.categoria())) {
                    return false;
                }

                if (filtraData) {
                    if (d.dataDocumento() == null) {
                        return false;
                    }
                    if (dal != null && d.dataDocumento().isBefore(dal)) {
                        return false;
                    }
                    if (al != null && d.dataDocumento().isAfter(al)) {
                        return false;
                    }
                }

                if (!ricerca.isEmpty()) {
                    //l'identificativo porta il numero/data ufficiali ("Circolare n. 30/E del 27
                    //ottobre 2023"), spesso il modo più naturale con cui si cerca un documento e
                    //quasi mai ripetuto nel titolo
                    boolean metadatiMatch = d.titolo().toLowerCase(Locale.ITALIAN).contains(ricerca)
                            || d.identificativo().toLowerCase(Locale.ITALIAN).contains(ricerca);
                    if (!metadatiMatch) {
                        if (!cercaTesto) {
                            return false;
                        }
                        String testo = TestiCache.get(d.percorso());
                        if (testo == null || !testo.toLowerCase(Locale.ITALIAN).contains(ricerca)) {
                            return false;
                        }
                    }
                }

                return true;
            }
        });

        AggiornaLabelStato();
    }

    private static LocalDate DataLocale(java.util.Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ------------------------------------------------------------------ dettaglio

    private DocumentoNormativa DocumentoSelezionato() {
        int riga = Tabella.getSelectedRow();
        if (riga < 0) {
            return null;
        }
        return Catalogo.get(Tabella.convertRowIndexToModel(riga));
    }

    private void AggiornaDettaglio() {
        DocumentoNormativa d = DocumentoSelezionato();
        Bottone_Apri.setEnabled(d != null);
        Bottone_ApriFonte.setEnabled(Principale_Normativa.UrlApribile(d));

        if (d == null) {
            TextArea_Dettaglio.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(d.autorita()).append('\n');
        sb.append(d.identificativo()).append('\n');
        if (d.dataDocumento() != null) {
            sb.append("Data: ").append(d.dataDocumento().format(FORMATO_DATA)).append('\n');
        }
        if (!d.argomenti().isEmpty()) {
            sb.append("Argomenti: ").append(String.join(", ", d.argomenti())).append('\n');
        }
        sb.append(Principale_Normativa.UrlApribile(d) ? d.url() : "").append('\n');

        String ricerca = TextField_Ricerca.getText().trim();
        if (CheckBox_CercaTesto.isSelected() && !ricerca.isEmpty()) {
            String estratto = Estratto(TestiCache.get(d.percorso()), ricerca);
            if (estratto != null) {
                sb.append("\n…").append(estratto).append("…");
            }
        }

        TextArea_Dettaglio.setText(sb.toString());
        TextArea_Dettaglio.setCaretPosition(0);
    }

    /** @return un breve contesto attorno alla prima occorrenza di {@code ricerca} in {@code testo}, o {@code null} */
    private static String Estratto(String testo, String ricerca) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        int pos = testo.toLowerCase(Locale.ITALIAN).indexOf(ricerca.toLowerCase(Locale.ITALIAN));
        if (pos < 0) {
            return null;
        }
        int inizio = Math.max(0, pos - 80);
        int fine = Math.min(testo.length(), pos + ricerca.length() + 80);
        return testo.substring(inizio, fine).replaceAll("\\s+", " ").trim();
    }

    private void AdattaLarghezzaColonne() {
        for (int c = 0; c < Tabella.getColumnCount(); c++) {
            int larghezza = Tabella.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(Tabella, Modello().getColumnName(c), false, false, 0, c)
                    .getPreferredSize().width;
            for (int r = 0; r < Tabella.getRowCount(); r++) {
                larghezza = Math.max(larghezza, Tabella.prepareRenderer(Tabella.getCellRenderer(r, c), r, c)
                        .getPreferredSize().width);
            }
            Tabella.getColumnModel().getColumn(c).setPreferredWidth(Math.min(larghezza + 20, 420));
        }
    }

    // ------------------------------------------------------------------ eventi

    private void Bottone_AggiornaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AggiornaActionPerformed
        Principale_Normativa.Aggiorna(Proprietario());
        Ricarica();
    }//GEN-LAST:event_Bottone_AggiornaActionPerformed

    private void Bottone_AzzeraFiltriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AzzeraFiltriActionPerformed
        ComboBox_Categoria.setSelectedItem(Principale_Normativa.TUTTE_LE_CATEGORIE);
        DataChooser_Dal.setDate(null);
        DataChooser_Al.setDate(null);
        TextField_Ricerca.setText("");
        CheckBox_CercaTesto.setSelected(false);
        AggiornaFiltro();
    }//GEN-LAST:event_Bottone_AzzeraFiltriActionPerformed

    private void Bottone_ApriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ApriActionPerformed
        Principale_Normativa.ApriCopia(DocumentoSelezionato(), Proprietario());
    }//GEN-LAST:event_Bottone_ApriActionPerformed

    private void Bottone_ApriFonteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ApriFonteActionPerformed
        Principale_Normativa.ApriFonteUfficiale(DocumentoSelezionato(), Proprietario());
    }//GEN-LAST:event_Bottone_ApriFonteActionPerformed

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Label_Categoria = new javax.swing.JLabel();
        ComboBox_Categoria = new javax.swing.JComboBox<>();
        Label_Dal = new javax.swing.JLabel();
        DataChooser_Dal = new com.toedter.calendar.JDateChooser();
        Label_Al = new javax.swing.JLabel();
        DataChooser_Al = new com.toedter.calendar.JDateChooser();
        TextField_Ricerca = new javax.swing.JTextField();
        CheckBox_CercaTesto = new javax.swing.JCheckBox();
        Bottone_AzzeraFiltri = new javax.swing.JButton();
        Bottone_Aggiorna = new javax.swing.JButton();
        ScrollPane_Tabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Label_Stato = new javax.swing.JLabel();
        ScrollPane_Dettaglio = new javax.swing.JScrollPane();
        TextArea_Dettaglio = new javax.swing.JTextArea();
        Bottone_Apri = new javax.swing.JButton();
        Bottone_ApriFonte = new javax.swing.JButton();

        Label_Categoria.setText("Categoria:");

        Label_Dal.setText("Dal:");

        DataChooser_Dal.setDateFormatString("yyyy-MM-dd");

        Label_Al.setText("Al:");

        DataChooser_Al.setDateFormatString("yyyy-MM-dd");

        CheckBox_CercaTesto.setText("Cerca anche nel testo");

        Bottone_AzzeraFiltri.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_ImbutoX.png"))); // NOI18N
        Bottone_AzzeraFiltri.setText("Azzera filtri");
        Bottone_AzzeraFiltri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AzzeraFiltriActionPerformed(evt);
            }
        });

        Bottone_Aggiorna.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Aggiorna.png"))); // NOI18N
        Bottone_Aggiorna.setText("Aggiorna da GitHub");
        Bottone_Aggiorna.setToolTipText("Scarica dal repository del progetto i documenti mancanti o cambiati");
        Bottone_Aggiorna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AggiornaActionPerformed(evt);
            }
        });

        Tabella.setAutoCreateRowSorter(false);
        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Titolo", "Categoria", "Autorità", "Identificativo", "Tipo", "Data"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Tabella.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        ScrollPane_Tabella.setViewportView(Tabella);

        TextArea_Dettaglio.setEditable(false);
        TextArea_Dettaglio.setColumns(20);
        TextArea_Dettaglio.setLineWrap(true);
        TextArea_Dettaglio.setRows(5);
        TextArea_Dettaglio.setWrapStyleWord(true);
        ScrollPane_Dettaglio.setViewportView(TextArea_Dettaglio);

        Bottone_Apri.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Documento.png"))); // NOI18N
        Bottone_Apri.setText("Apri una copia");
        Bottone_Apri.setToolTipText("Apre il documento selezionato con l'applicazione predefinita del sistema");
        Bottone_Apri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ApriActionPerformed(evt);
            }
        });

        Bottone_ApriFonte.setText("Apri fonte ufficiale");
        Bottone_ApriFonte.setToolTipText("Apre nel browser l'indirizzo da cui il documento è stato scaricato, per confrontarlo con l'originale");
        Bottone_ApriFonte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ApriFonteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Label_Categoria)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComboBox_Categoria, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Label_Dal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DataChooser_Dal, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Label_Al)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DataChooser_Al, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TextField_Ricerca, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CheckBox_CercaTesto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_AzzeraFiltri)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Aggiorna))
                    .addComponent(ScrollPane_Tabella, javax.swing.GroupLayout.DEFAULT_SIZE, 1160, Short.MAX_VALUE)
                    .addComponent(Label_Stato)
                    .addComponent(ScrollPane_Dettaglio, javax.swing.GroupLayout.DEFAULT_SIZE, 1160, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Apri, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_ApriFonte, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_Categoria)
                    .addComponent(ComboBox_Categoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Label_Dal)
                    .addComponent(DataChooser_Dal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Label_Al)
                    .addComponent(DataChooser_Al, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TextField_Ricerca, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CheckBox_CercaTesto)
                    .addComponent(Bottone_AzzeraFiltri)
                    .addComponent(Bottone_Aggiorna))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollPane_Tabella, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Label_Stato)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollPane_Dettaglio, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_Apri, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Bottone_ApriFonte, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Aggiorna;
    private javax.swing.JButton Bottone_Apri;
    private javax.swing.JButton Bottone_ApriFonte;
    private javax.swing.JButton Bottone_AzzeraFiltri;
    private javax.swing.JCheckBox CheckBox_CercaTesto;
    private javax.swing.JComboBox<String> ComboBox_Categoria;
    private com.toedter.calendar.JDateChooser DataChooser_Al;
    private com.toedter.calendar.JDateChooser DataChooser_Dal;
    private javax.swing.JLabel Label_Al;
    private javax.swing.JLabel Label_Categoria;
    private javax.swing.JLabel Label_Dal;
    private javax.swing.JLabel Label_Stato;
    private javax.swing.JScrollPane ScrollPane_Dettaglio;
    private javax.swing.JScrollPane ScrollPane_Tabella;
    private javax.swing.JTable Tabella;
    private javax.swing.JTextArea TextArea_Dettaglio;
    private javax.swing.JTextField TextField_Ricerca;
    // End of variables declaration//GEN-END:variables
}
