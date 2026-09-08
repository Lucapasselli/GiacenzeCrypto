/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_CHIAVE_DEFAULT;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_DATA_FINE;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_DATA_INIZIO;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_FONTE;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_IDENT;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_NOME;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_NOTE;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_ORIGINE;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_PROGRESSIVO;
import static com.giacenzecrypto.giacenze_crypto.Principale_PeriodiExchange.COL_STATO;

/**
 * Periodi fiscali di un exchange ({@code EXCHANGE_PERIODO}) : un rigo per ogni entità legale / Stato
 * estero nel tempo. Colonna <b>Origine</b> (Predefinito / Modificato / Manuale).
 *
 * <p>La tabella è di sola lettura ; si aggiunge / modifica una riga dal dialogo
 * {@link GUI_ModificaPeriodoExchange} (date da calendario). "Ripristina riga / tutti al default"
 * riportano ai valori di {@code RW_Predefiniti.json}. Il modello di verità è la lista {@link #righe} ;
 * la {@code Tabella} è solo la vista, ricostruita dopo ogni operazione.</p>
 */
public class GUI_PeriodiExchange extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final String exchangeId;
    private final List<String[]> righe = new ArrayList<>();

    /** {@code true} se l'utente ha salvato almeno una volta. */
    public boolean salvato = false;

    public GUI_PeriodiExchange(String exchangeId) {
        this(exchangeId, exchangeId);
    }

    public GUI_PeriodiExchange(String exchangeId, String nomeExchange) {
        this.exchangeId = exchangeId;
        try {
            setIconImage(new ImageIcon(VarStatiche.getPathRisorse() + "logo.png").getImage());
        } catch (RuntimeException ignore) {
        }
        initComponents();
        // Stile di default delle tabelle dell'applicazione (righe alternate a tema, header in grassetto).
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabelle.ColoraTabellaSemplice(Tabella);

        Tabella.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        Tabella.setShowGrid(false); // niente righe fra celle : come le altre tabelle dell'app (default FlatLaf)
        Tabella.setPreferredScrollableViewportSize(new java.awt.Dimension(900, 260));
        int[] larghezze = {50, 90, 90, 190, 80, 150, 300, 240, 110};
        for (int i = 0; i < larghezze.length && i < Tabella.getColumnModel().getColumnCount(); i++) {
            Tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        Tabella.getColumnModel().getColumn(COL_NOME).setCellRenderer(new Tabelle.WrapCellRenderer());
        Tabella.getColumnModel().getColumn(COL_NOTE).setCellRenderer(new Tabelle.WrapCellRenderer());
        Tabella.getColumnModel().getColumn(COL_FONTE).setCellRenderer(new Tabelle.WrapCellRenderer());
        Tabelle.TooltipHeaderColonne(Tabella,
                "Numero d'ordine del periodo (cronologico)",
                "Inizio validità di questa entità / Stato (vuoto = da sempre)",
                "Fine validità (vuoto = periodo ancora in corso)",
                "Denominazione legale dell'operatore in questo periodo",
                "Codice dello Stato estero — Tabella 10 delle istruzioni Redditi PF",
                "P.IVA o numero di registro imprese (max 15 caratteri, limite modulo FC.1 ISEE)",
                "Note libere (es. riferimenti dell'autorizzazione, LEI)",
                "Da dove viene il dato (fonte ufficiale)",
                "Provenienza della riga : Predefinito / Modificato / Manuale");

        String etichetta = nomeExchange == null || nomeExchange.isBlank() ? exchangeId : nomeExchange;
        setTitle("Periodi fiscali — " + etichetta);
        Label_Titolo.setText("Periodi fiscali dell'exchange \"" + etichetta + "\" (" + exchangeId + ")");

        // L'altezza-riga dipende dalla larghezza reale delle colonne, nota solo a finestra disegnata.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                Tabelle.AdattaAltezzaRighe(Tabella, 24);
            }
        });

        righe.addAll(Principale_PeriodiExchange.caricaPeriodi(exchangeId));
        ricostruisciVista();
        setLocationRelativeTo(null);
    }

    private void ricostruisciVista() {
        DefaultTableModel m = (DefaultTableModel) Tabella.getModel();
        m.setRowCount(0);
        for (String[] r : righe) {
            m.addRow(new Object[]{
                v(r, COL_PROGRESSIVO), v(r, COL_DATA_INIZIO), v(r, COL_DATA_FINE), v(r, COL_NOME),
                v(r, COL_STATO), v(r, COL_IDENT), v(r, COL_NOTE), v(r, COL_FONTE),
                Principale_PeriodiExchange.descrizioneOrigineRiga(v(r, COL_ORIGINE), v(r, COL_CHIAVE_DEFAULT))});
        }
        Tabelle.AdattaAltezzaRighe(Tabella, 24);
    }

    private int rigaSelezionata() {
        int v = Tabella.getSelectedRow();
        return v < 0 ? -1 : Tabella.convertRowIndexToModel(v);
    }

    private void selezionaUltima() {
        int n = Tabella.getRowCount();
        if (n > 0) {
            Tabella.setRowSelectionInterval(n - 1, n - 1);
            Tabella.scrollRectToVisible(Tabella.getCellRect(n - 1, 0, true));
        }
    }

    private static String v(String[] r, int i) {
        return r != null && i >= 0 && i < r.length && r[i] != null ? r[i] : "";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Label_Titolo = new javax.swing.JLabel();
        Label_Info = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_Aggiungi = new javax.swing.JButton();
        Bottone_Modifica = new javax.swing.JButton();
        Bottone_Rimuovi = new javax.swing.JButton();
        Bottone_RipristinaRiga = new javax.swing.JButton();
        Bottone_RipristinaTutti = new javax.swing.JButton();
        Bottone_Salva = new javax.swing.JButton();
        Bottone_Chiudi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Periodi fiscali dell'exchange");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Label_Titolo.setText("Periodi fiscali dell'exchange");

        Label_Info.setText("<html><div style='width: 900px'>"
                + "<b>A cosa serve.</b> La successione delle entità legali / Stati esteri sotto cui "
                + "l'exchange ha operato nel tempo: ogni migrazione MiCA (cambio di Paese o P.IVA) è "
                + "un rigo diverso nel Quadro W/RW e nell'ISEE."
                + "<br><b>Colonne</b>"
                + "<ul style='margin-top:2px; margin-bottom:2px'>"
                + "<li><b>Data inizio</b> / <b>Data fine</b> vuote = \"da sempre\" / \"ancora in corso\"</li>"
                + "<li><b>Nome entità</b> = denominazione legale dell'operatore in quel periodo</li>"
                + "<li><b>Stato estero</b> = codice della Tabella 10 delle istruzioni Redditi PF (si sceglie da un elenco nel dialogo)</li>"
                + "<li><b>Identificativo fiscale</b> = P.IVA o n. registro imprese (max 15 caratteri, limite del modulo FC.1 ISEE)</li>"
                + "<li><b>Origine</b> = \"Predefinito\" (dal programma) / \"Modificato\" / \"Manuale\"</li>"
                + "</ul>"
                + "<b>Pulsanti.</b> \"Aggiungi\" e \"Modifica\" aprono un dialogo con il calendario per "
                + "le date; \"Ripristina riga\" e \"Ripristina tutto\" riportano ai valori predefiniti. "
                + "Colonne larghe: scorri la tabella in orizzontale."
                + "</div></html>");

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Progr.", "Data inizio", "Data fine", "Nome entita'", "Stato estero", "Identificativo fiscale", "Note", "Fonte", "Origine"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(Tabella);

        Bottone_Aggiungi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        Bottone_Aggiungi.setText("Aggiungi...");
        Bottone_Aggiungi.setToolTipText("Aggiunge un nuovo periodo fiscale (entita' / Stato estero)");
        Bottone_Aggiungi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AggiungiActionPerformed(evt);
            }
        });

        Bottone_Modifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        Bottone_Modifica.setText("Modifica...");
        Bottone_Modifica.setToolTipText("Modifica il periodo selezionato nel dialogo dedicato (con calendario)");
        Bottone_Modifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ModificaActionPerformed(evt);
            }
        });

        Bottone_Rimuovi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Cestino.png"))); // NOI18N
        Bottone_Rimuovi.setText("Rimuovi");
        Bottone_Rimuovi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RimuoviActionPerformed(evt);
            }
        });

        Bottone_RipristinaRiga.setText("Ripristina riga");
        Bottone_RipristinaRiga.setToolTipText("Riporta la riga selezionata ai valori predefiniti del programma");
        Bottone_RipristinaRiga.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RipristinaRigaActionPerformed(evt);
            }
        });

        Bottone_RipristinaTutti.setText("Ripristina tutto");
        Bottone_RipristinaTutti.setToolTipText("Scarta le personalizzazioni e riapplica i valori predefiniti del programma");
        Bottone_RipristinaTutti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RipristinaTuttiActionPerformed(evt);
            }
        });

        Bottone_Salva.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Salva.png"))); // NOI18N
        Bottone_Salva.setText("Salva");
        Bottone_Salva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_SalvaActionPerformed(evt);
            }
        });

        Bottone_Chiudi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_Chiudi.setText("Chiudi");
        Bottone_Chiudi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ChiudiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Label_Titolo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Label_Info, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Aggiungi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Modifica)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Rimuovi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_RipristinaRiga)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_RipristinaTutti)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Salva)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Chiudi)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Label_Titolo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Label_Info)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_Aggiungi)
                    .addComponent(Bottone_Modifica)
                    .addComponent(Bottone_Rimuovi)
                    .addComponent(Bottone_RipristinaRiga)
                    .addComponent(Bottone_RipristinaTutti)
                    .addComponent(Bottone_Salva)
                    .addComponent(Bottone_Chiudi))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_AggiungiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AggiungiActionPerformed
        int prog = Principale_PeriodiExchange.prossimoProgressivo(righe);
        GUI_ModificaPeriodoExchange d = new GUI_ModificaPeriodoExchange(this, null, prog);
        d.setVisible(true);
        if (d.confermato) {
            righe.add(d.risultato);
            ricostruisciVista();
            selezionaUltima();
        }
    }//GEN-LAST:event_Bottone_AggiungiActionPerformed

    private void Bottone_ModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ModificaActionPerformed
        int i = rigaSelezionata();
        if (i < 0) {
            Messaggi.WarningMessage("Nessuna selezione", "Seleziona prima un periodo.", this);
            return;
        }
        GUI_ModificaPeriodoExchange d = new GUI_ModificaPeriodoExchange(this, righe.get(i), 0);
        d.setVisible(true);
        if (d.confermato) {
            righe.set(i, d.risultato);
            ricostruisciVista();
        }
    }//GEN-LAST:event_Bottone_ModificaActionPerformed

    private void Bottone_RimuoviActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RimuoviActionPerformed
        int i = rigaSelezionata();
        if (i >= 0) {
            righe.remove(i);
            ricostruisciVista();
        }
    }//GEN-LAST:event_Bottone_RimuoviActionPerformed

    private void Bottone_RipristinaRigaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RipristinaRigaActionPerformed
        int i = rigaSelezionata();
        if (i < 0) {
            Messaggi.WarningMessage("Nessuna selezione", "Seleziona prima un periodo.", this);
            return;
        }
        if (Principale_PeriodiExchange.ripristinaRigaAlDefault(exchangeId, righe, i)) {
            ricostruisciVista();
        } else {
            Messaggi.InfoMessage("Nessun default", "Questo periodo non ha un corrispondente nei dati "
                    + "predefiniti : non c'è nulla a cui ripristinarlo.", this);
        }
    }//GEN-LAST:event_Bottone_RipristinaRigaActionPerformed

    private void Bottone_RipristinaTuttiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RipristinaTuttiActionPerformed
        int scelta = JOptionPane.showConfirmDialog(this,
                "Rimuovere le personalizzazioni e riportare tutti i periodi fiscali di questo exchange\n"
                + "ai valori predefiniti del programma?", "Ripristina tutti al default",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (scelta != JOptionPane.YES_OPTION) {
            return;
        }
        List<String[]> def = Principale_PeriodiExchange.ripristinaTuttiAlDefault(exchangeId);
        if (def == null) {
            Messaggi.WarningMessage("Dati predefiniti non disponibili",
                    "Non è stato possibile leggere RW_Predefiniti.json.", this);
            return;
        }
        righe.clear();
        righe.addAll(def);
        ricostruisciVista();
    }//GEN-LAST:event_Bottone_RipristinaTuttiActionPerformed

    private void Bottone_SalvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_SalvaActionPerformed
        List<String> errori = Principale_PeriodiExchange.salvaPeriodi(exchangeId, righe);
        if (!errori.isEmpty()) {
            Messaggi.WarningMessage("Dati non validi", String.join("\n", errori), this);
            return;
        }
        salvato = true;
        righe.clear();
        righe.addAll(Principale_PeriodiExchange.caricaPeriodi(exchangeId));
        ricostruisciVista();
        List<String> avvisi = Principale_PeriodiExchange.avvisiPeriodi(righe);
        if (avvisi.isEmpty()) {
            Messaggi.InfoMessage("Salvato", "Periodi fiscali salvati.", this);
        } else {
            Messaggi.InfoMessage("Salvato (con avvisi)",
                    "Periodi fiscali salvati.\n\nAvvisi (non bloccanti) :\n- " + String.join("\n- ", avvisi), this);
        }
    }//GEN-LAST:event_Bottone_SalvaActionPerformed

    private void Bottone_ChiudiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ChiudiActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_ChiudiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Aggiungi;
    private javax.swing.JButton Bottone_Chiudi;
    private javax.swing.JButton Bottone_Modifica;
    private javax.swing.JButton Bottone_Rimuovi;
    private javax.swing.JButton Bottone_RipristinaRiga;
    private javax.swing.JButton Bottone_RipristinaTutti;
    private javax.swing.JButton Bottone_Salva;
    private javax.swing.JLabel Label_Info;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable Tabella;
    // End of variables declaration//GEN-END:variables
}
