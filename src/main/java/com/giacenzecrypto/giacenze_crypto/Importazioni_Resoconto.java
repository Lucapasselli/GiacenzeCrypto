/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;


import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;




/**
 *
 * @author luca.passelli
 */
public class Importazioni_Resoconto extends javax.swing.JDialog {
private static final long serialVersionUID = 8L;
    /**
     * Creates new form Importazioni_Resoconto
     */
    
    /*    int Transazioni=0;
        int TransazioniAggiunte=0;
        int TrasazioniScartate=0;
        String movimentiSconosciuti="";*/
    
    // Contesto dell'importazione appena eseguita, per il pulsante "Invia segnalazione errori":
    // dai soli righi d'errore (movimenti sconosciuti) non si capirebbe a quale import si
    // riferiscono. Catturati alla costruzione perché il resoconto viene aperto subito dopo
    // l'import (vedi DocumentiFonte.UltimaDescrizioneImport).
    private String contestoDescrizione = "";
    private String contestoFile = "";
    private int cTotali;
    private int cAggiunte;
    private int cScartate;
    private int cSconosciute;
    private String movimentiSconosciuti = "";

    public Importazioni_Resoconto() {
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        //Nascondo gli scrollpane e non solo i TextPane che contengono: nascondendo solo questi ultimi
        //resterebbero visibili due riquadri vuoti anche quando non c'è nulla da segnalare.
        jScrollPane1.setVisible(false);
        jScrollPane2.setVisible(false);
        TextPane_Attenzione.setVisible(false);
        TextPane_Errori.setVisible(false);

        //Il vecchio pulsante "Copia errori negli appunti" diventa l'invio diretto della segnalazione
        //(il .form resta invariato: qui si cambia solo l'etichetta e, nell'handler, il comportamento).
        Bottone_CopiaAppunti.setText("Invia segnalazione errori");

        contestoDescrizione = DocumentiFonte.UltimaDescrizioneImport == null ? "" : DocumentiFonte.UltimaDescrizioneImport;
        contestoFile = DocumentiFonte.UltimoFileImport == null ? "" : DocumentiFonte.UltimoFileImport;
    }

    /**
     * Popola il riepilogo a partire dall'esito di un'importazione, intestandolo con la sua origine.
     * <p>È la forma usata dallo scaricamento via API degli exchange, dove più importazioni possono essere
     * sommate in un unico esito e mostrate in una sola finestra.
     * @param E esito dell'importazione (o somma degli esiti di più exchange)
     */
    public void ImpostaValori(Importazioni.Esito E) {
        if (!E.Origine.isBlank()) {
            Label_Titolo.setText("RESOCONTO IMPORTAZIONE - " + E.Origine.toUpperCase());
            //L'origine dell'esito (scaricamento API di più exchange sommati) è più precisa della
            //descrizione dell'ultimo documento: la si preferisce quando c'è.
            contestoDescrizione = E.Origine;
        }
        ImpostaValori(E.Transazioni, E.Aggiunte, E.Scartate, E.Sconosciute, E.MovimentiSconosciuti);
    }

    /**
     * Popola i campi del riepilogo con i conteggi dell'importazione appena eseguita, evidenziando in
     * rosso le transazioni scartate/sconosciute quando presenti e abilitando il pulsante di copia degli
     * errori se sono presenti movimenti sconosciuti.
     * @param T numero totale di transazioni elaborate
     * @param TAggiunte numero di transazioni importate con successo
     * @param TScartate numero di transazioni scartate perché già esistenti
     * @param TSconosciute numero di transazioni scartate perché di tipo sconosciuto
     * @param movScon testo con l'elenco dei movimenti sconosciuti, vuoto se nessuno
     */
    public void ImpostaValori(int T,int TAggiunte,int TScartate,int TSconosciute,String movScon){

        this.Text_TransTotali.setText(String.valueOf(T));
        this.Text_TransImportate.setText(String.valueOf(TAggiunte));
        this.Text_TransScartate.setText(String.valueOf(TScartate));
        this.Text_TransSconosciute.setText(String.valueOf(TSconosciute));
        this.cTotali = T;
        this.cAggiunte = TAggiunte;
        this.cScartate = TScartate;
        this.cSconosciute = TSconosciute;
        this.movimentiSconosciuti = movScon == null ? "" : movScon;
        if (TSconosciute==0) Text_TransSconosciute.setForeground(Color.BLACK); else Text_TransSconosciute.setForeground(Color.RED);
        if (TScartate==0) this.Text_TransScartate.setForeground(Color.BLACK); else Text_TransScartate.setForeground(Color.RED);
        if (!movScon.trim().equalsIgnoreCase("")){
            this.Bottone_CopiaAppunti.setEnabled(true);
            this.jScrollPane1.setVisible(true);
            this.jScrollPane2.setVisible(true);
            this.TextPane_Attenzione.setVisible(true);
            this.TextPane_Errori.setVisible(true);
            this.TextPane_Errori.setText(movScon);
            //Il riquadro d'avviso non manda più a una mail: c'è il pulsante "Invia segnalazione errori".
            this.TextPane_Attenzione.setText("<html><body><p style=\"margin-top:0\">"
                    + "<b><center>ATTENZIONE: alcune transazioni non sono state importate.</b><br><br>"
                    + "<center>I movimenti elencati qui sotto non sono riconosciuti dall'import del programma.<br>"
                    + "<center>Premi il pulsante <b>Invia segnalazione errori</b> per mandarli all'autore:"
                    + " verranno spediti solo queste righe, il tipo di importazione e la versione del programma."
                    + "</p></body></html>");
        }
        pack();
              
        
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
        Label_TransTotali = new javax.swing.JLabel();
        Label_TransImportate = new javax.swing.JLabel();
        Label_TransScartate = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TextPane_Attenzione = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        TextPane_Errori = new javax.swing.JTextPane();
        Text_TransTotali = new javax.swing.JTextField();
        Text_TransImportate = new javax.swing.JTextField();
        Text_TransScartate = new javax.swing.JTextField();
        Bottone_Ok = new javax.swing.JButton();
        Bottone_CopiaAppunti = new javax.swing.JButton();
        Label_TransSconosciute = new javax.swing.JLabel();
        Text_TransSconosciute = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        Label_Titolo.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        Label_Titolo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Label_Titolo.setText("RESOCONTO IMPORTAZIONE");

        Label_TransTotali.setText("Transazioni Totali :");

        Label_TransImportate.setText("Transazioni Importate :");

        Label_TransScartate.setText("Transazioni Scartate perchè già esistenti :");

        TextPane_Attenzione.setEditable(false);
        TextPane_Attenzione.setContentType("text/html"); // NOI18N
        TextPane_Attenzione.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \r<b><center>ATTENZIONE!!! &#9    ALCUNE TRANSAZIONI SONO STATE SCARTATE!</b><br><br>\n<center> Le transazioni di seguito elencate non sono contemplate dall'import del programma<br>\n<center>e' quindi importate segnalare la cosa affinchè possano essere aggiunte all'import<br><br>\n<center>Mandare una mail all'indirizzo <b>giacenzecrypto@gmail.com</b> con il dettaglio dei movimenti presenti <br>\n<center>qua sotto se si vuole che vengano implementate nelle versioni successive del programma<br>\n    </p>\r\n  </body>\r\n</html>\r\n");
        jScrollPane1.setViewportView(TextPane_Attenzione);

        TextPane_Errori.setEditable(false);
        jScrollPane2.setViewportView(TextPane_Errori);

        Text_TransTotali.setEditable(false);
        Text_TransTotali.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N

        Text_TransImportate.setEditable(false);
        Text_TransImportate.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N

        Text_TransScartate.setEditable(false);
        Text_TransScartate.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N
        Text_TransScartate.setForeground(new java.awt.Color(204, 0, 0));

        Bottone_Ok.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N
        Bottone_Ok.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Salva.png"))); // NOI18N
        Bottone_Ok.setText("OK");
        Bottone_Ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_OkActionPerformed(evt);
            }
        });

        Bottone_CopiaAppunti.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Copia.png"))); // NOI18N
        Bottone_CopiaAppunti.setText("Copia errori negli appunti");
        Bottone_CopiaAppunti.setEnabled(false);
        Bottone_CopiaAppunti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_CopiaAppuntiActionPerformed(evt);
            }
        });

        Label_TransSconosciute.setText("Transazioni Scartate perchè sconosciute :");

        Text_TransSconosciute.setEditable(false);
        Text_TransSconosciute.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N
        Text_TransSconosciute.setForeground(new java.awt.Color(255, 51, 51));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(Label_Titolo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_CopiaAppunti, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Label_TransSconosciute, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Label_TransImportate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Label_TransScartate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Label_TransTotali, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Text_TransTotali)
                            .addComponent(Text_TransImportate)
                            .addComponent(Text_TransScartate)
                            .addComponent(Text_TransSconosciute))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Label_Titolo)
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_TransTotali)
                    .addComponent(Text_TransTotali, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_TransImportate)
                    .addComponent(Text_TransImportate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_TransScartate)
                    .addComponent(Text_TransScartate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Text_TransSconosciute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Label_TransSconosciute))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Bottone_CopiaAppunti, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                    .addComponent(Bottone_Ok, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_OkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_OkActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_Bottone_OkActionPerformed

    private void Bottone_CopiaAppuntiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_CopiaAppuntiActionPerformed
        final String corpo = TextPane_Errori.getText();
        if (corpo == null || corpo.isBlank()) {
            return;
        }
        String tipo = contestoDescrizione.isBlank() ? "(sconosciuto)" : contestoDescrizione;

        AppDialog.DialogResult r = AppDialog.builder(this)
                .windowTitle("Invia segnalazione errori")
                .bodyTitle("Inviare la segnalazione?")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Verranno inviati all'autore: le righe dei movimenti sconosciuti mostrate qui sopra, "
                        + "il tipo di importazione (<b>" + escapeHtml(tipo) + "</b>) e la versione del programma. "
                        + "Nessun altro dato.")
                .details("Il servizio conserva la segnalazione al massimo 30 giorni, solo per la diagnosi.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("send", "Invia")
                        .role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (r == null || !r.isAction("send")) {
            return;
        }

        SegnalazioniClient.DescrittoreImport d = new SegnalazioniClient.DescrittoreImport(
                contestoDescrizione, contestoFile, cTotali, cAggiunte, cScartate, cSconosciute);

        Download dow = new Download();
        dow.NascondiInterrompi();
        dow.MostraProgressAttesa("Invio segnalazione", "Invio in corso...");
        dow.SetLabel("Invio in corso, attendere...");
        dow.setLocationRelativeTo(this);

        final SegnalazioniClient.Esito[] esito = new SegnalazioniClient.Esito[1];
        Thread t = new Thread(() -> {
            try {
                esito[0] = SegnalazioniClient.inviaErroreImport(corpo, d);
            } catch (RuntimeException ex) {
                LoggerGC.ScriviErrore(ex);
                esito[0] = new SegnalazioniClient.Esito(false, 0, "Errore imprevisto: " + ex.getMessage());
            } finally {
                dow.ChiudiFinestra();
            }
        });
        t.start();
        dow.setVisible(true);

        SegnalazioniClient.Esito e = esito[0];
        if (e != null && e.ok()) {
            Messaggi.SuccessMessage("Inviata", e.messaggio(), this);
            Bottone_CopiaAppunti.setEnabled(false);
        } else {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(corpo), null);
            Messaggi.WarningMessage("Invio non riuscito",
                    (e != null ? e.messaggio() : "Nessuna risposta dal servizio.")
                    + " Gli errori sono stati copiati negli appunti: puoi incollarli in una mail all'autore.",
                    this);
        }
    }//GEN-LAST:event_Bottone_CopiaAppuntiActionPerformed

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Resoconto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Resoconto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Resoconto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Resoconto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Importazioni_Resoconto().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_CopiaAppunti;
    private javax.swing.JButton Bottone_Ok;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JLabel Label_TransImportate;
    private javax.swing.JLabel Label_TransScartate;
    private javax.swing.JLabel Label_TransSconosciute;
    private javax.swing.JLabel Label_TransTotali;
    private javax.swing.JTextPane TextPane_Attenzione;
    private javax.swing.JTextPane TextPane_Errori;
    private javax.swing.JTextField Text_TransImportate;
    private javax.swing.JTextField Text_TransScartate;
    private javax.swing.JTextField Text_TransSconosciute;
    private javax.swing.JTextField Text_TransTotali;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
