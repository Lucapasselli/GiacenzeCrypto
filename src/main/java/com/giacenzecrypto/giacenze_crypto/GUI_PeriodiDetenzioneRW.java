/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.table.DefaultTableModel;

import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_CRYPTO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_FIAT;

/**
 * Periodi di detenzione di un gruppo wallet per il quadro W/RW : righi CRYPTO / FIAT con
 * date di inizio/fine, valori campo 7-8 inseriti a mano (+ nota) e modalità di calcolo.
 *
 * <p>Scrive su {@code GRUPPO_PERIODO_RW} tramite {@link Principale_GruppiWalletRW#salvaPeriodi}
 * (validazione solo strutturale in Fase 2). Nelle colonne "Calcolo iniziale/finale" la
 * tabella mostra le etichette leggibili, convertite in codici al salvataggio.</p>
 */
public class GUI_PeriodiDetenzioneRW extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final String gruppo;

    /** {@code true} se l'utente ha salvato almeno una volta. */
    public boolean salvato = false;

    public GUI_PeriodiDetenzioneRW(String gruppo) {
        this.gruppo = gruppo;
        ImageIcon icon = new ImageIcon(VarStatiche.getPathRisorse() + "logo.png");
        this.setIconImage(icon.getImage());
        initComponents();
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);

        setTitle("Periodi di detenzione — " + gruppo);
        Label_Titolo.setText("Periodi di detenzione del gruppo \"" + gruppo + "\"");

        Tabella.getColumnModel().getColumn(0).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{TIPO_CRYPTO, TIPO_FIAT})));
        Tabella.getColumnModel().getColumn(8).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(etichette(MODALITA_INIZIALE))));
        Tabella.getColumnModel().getColumn(9).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(etichette(MODALITA_FINALE))));

        caricaTabella();
        setLocationRelativeTo(null);
    }

    private static String[] etichette(String[][] tabella) {
        String[] e = new String[tabella.length];
        for (int i = 0; i < tabella.length; i++) {
            e[i] = tabella[i][1];
        }
        return e;
    }

    private void caricaTabella() {
        DefaultTableModel m = (DefaultTableModel) Tabella.getModel();
        m.setRowCount(0);
        for (String[] r : Principale_GruppiWalletRW.caricaPeriodi(gruppo)) {
            m.addRow(new Object[]{
                r[0], r[1], nz(r[2]), nz(r[3]), nz(r[4]), nz(r[5]), nz(r[6]), nz(r[7]),
                Principale_GruppiWalletRW.etichettaModalita(r[8], MODALITA_INIZIALE),
                Principale_GruppiWalletRW.etichettaModalita(r[9], MODALITA_FINALE)
            });
        }
    }

    /** Righe della tabella in forma GUI a 10 colonne (codici, non etichette, per le colonne 8/9). */
    private List<String[]> raccogliRighe() {
        if (Tabella.isEditing()) {
            Tabella.getCellEditor().stopCellEditing();
        }
        DefaultTableModel m = (DefaultTableModel) Tabella.getModel();
        List<String[]> righe = new ArrayList<>();
        for (int i = 0; i < m.getRowCount(); i++) {
            righe.add(new String[]{
                cella(m.getValueAt(i, 0)), cella(m.getValueAt(i, 1)),
                cella(m.getValueAt(i, 2)), cella(m.getValueAt(i, 3)),
                cella(m.getValueAt(i, 4)), cella(m.getValueAt(i, 5)),
                cella(m.getValueAt(i, 6)), cella(m.getValueAt(i, 7)),
                Principale_GruppiWalletRW.codiceModalita(cella(m.getValueAt(i, 8)), MODALITA_INIZIALE),
                Principale_GruppiWalletRW.codiceModalita(cella(m.getValueAt(i, 9)), MODALITA_FINALE)
            });
        }
        return righe;
    }

    private static String cella(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void aggiungi(String tipo) {
        int p = Principale_GruppiWalletRW.prossimoProgressivo(raccogliRighe(), tipo);
        ((DefaultTableModel) Tabella.getModel()).addRow(new Object[]{
            tipo, String.valueOf(p), "", "", "", "", "", "",
            MODALITA_INIZIALE[0][1], MODALITA_FINALE[0][1]
        });
        int r = Tabella.getRowCount() - 1;
        Tabella.setRowSelectionInterval(r, r);
        Tabella.scrollRectToVisible(Tabella.getCellRect(r, 0, true));
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
        Bottone_AggiungiCrypto = new javax.swing.JButton();
        Bottone_AggiungiFiat = new javax.swing.JButton();
        Bottone_Rimuovi = new javax.swing.JButton();
        Bottone_Salva = new javax.swing.JButton();
        Bottone_Chiudi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Periodi di detenzione");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Label_Titolo.setText("Periodi di detenzione");

        Label_Info.setText("<html>Un rigo per periodo, distinto CRYPTO / FIAT. Data inizio vuota = dedotta dal primo movimento (in una fase successiva). Data fine obbligatoria solo se il conto e' stato chiuso. Un valore iniziale/finale inserito a mano prevale sul calcolo automatico : la nota serve a spiegarne il motivo.</html>");

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Tipo", "Progr.", "Data inizio", "Data fine", "Val. iniziale", "Nota iniziale", "Val. finale", "Nota finale", "Calcolo iniziale", "Calcolo finale"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, true, true, true, true, true, true, true, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(Tabella);

        Bottone_AggiungiCrypto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        Bottone_AggiungiCrypto.setText("Aggiungi rigo CRYPTO");
        Bottone_AggiungiCrypto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AggiungiCryptoActionPerformed(evt);
            }
        });

        Bottone_AggiungiFiat.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        Bottone_AggiungiFiat.setText("Aggiungi rigo FIAT");
        Bottone_AggiungiFiat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AggiungiFiatActionPerformed(evt);
            }
        });

        Bottone_Rimuovi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Cestino.png"))); // NOI18N
        Bottone_Rimuovi.setText("Rimuovi rigo");
        Bottone_Rimuovi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RimuoviActionPerformed(evt);
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1040, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_AggiungiCrypto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_AggiungiFiat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Rimuovi)
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_AggiungiCrypto)
                    .addComponent(Bottone_AggiungiFiat)
                    .addComponent(Bottone_Rimuovi)
                    .addComponent(Bottone_Salva)
                    .addComponent(Bottone_Chiudi))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_AggiungiCryptoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AggiungiCryptoActionPerformed
        aggiungi(TIPO_CRYPTO);
    }//GEN-LAST:event_Bottone_AggiungiCryptoActionPerformed

    private void Bottone_AggiungiFiatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AggiungiFiatActionPerformed
        aggiungi(TIPO_FIAT);
    }//GEN-LAST:event_Bottone_AggiungiFiatActionPerformed

    private void Bottone_RimuoviActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RimuoviActionPerformed
        int riga = Tabella.getSelectedRow();
        if (riga < 0) {
            return;
        }
        if (Tabella.isEditing()) {
            Tabella.getCellEditor().cancelCellEditing();
        }
        ((DefaultTableModel) Tabella.getModel()).removeRow(Tabella.convertRowIndexToModel(riga));
    }//GEN-LAST:event_Bottone_RimuoviActionPerformed

    private void Bottone_SalvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_SalvaActionPerformed
        List<String> errori = Principale_GruppiWalletRW.salvaPeriodi(gruppo, raccogliRighe());
        if (!errori.isEmpty()) {
            Messaggi.WarningMessage("Dati non validi", String.join("\n", errori), this);
            return;
        }
        salvato = true;
        caricaTabella();
        Messaggi.InfoMessage("Salvato", "Periodi di detenzione salvati.", this);
    }//GEN-LAST:event_Bottone_SalvaActionPerformed

    private void Bottone_ChiudiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ChiudiActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_ChiudiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_AggiungiCrypto;
    private javax.swing.JButton Bottone_AggiungiFiat;
    private javax.swing.JButton Bottone_Chiudi;
    private javax.swing.JButton Bottone_Rimuovi;
    private javax.swing.JButton Bottone_Salva;
    private javax.swing.JLabel Label_Info;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable Tabella;
    // End of variables declaration//GEN-END:variables
}
