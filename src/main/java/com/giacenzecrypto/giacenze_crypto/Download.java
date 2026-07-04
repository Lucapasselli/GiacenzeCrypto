/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.giacenzecrypto.giacenze_crypto;



import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.Timer;



/**
 * Finestra di dialogo modale per il monitoraggio delle operazioni asincrone
 * (importazioni, download, elaborazioni batch).
 * <p>
 * Mostra una barra di avanzamento, un contatore testuale, un pannello di log
 * dei dettagli e un pannello dedicato agli errori bloccanti. Supporta sia la
 * modalità deterministica (con massimo e avanzamento espliciti) sia quella
 * indeterministica (spinner) tramite {@link #setIndeterminate(boolean)}.
 * </p>
 *
 * @author luca.passelli
 */
public class Download extends javax.swing.JDialog {
     private static final long serialVersionUID = 4L;

/** Valore massimo della barra di avanzamento, impostato da {@link #SetMassimo(int)}. */
public int Massimo;

/** Valore corrente della barra di avanzamento, aggiornato da {@link #SetAvanzamento(int)}. */
public int avanzamento;

/** Thread associato all'operazione monitorata; usato per rilevare la fine automatica. */
public transient Thread thread;

/**
 * Flag statico che segnala la terminazione (normale o forzata) del thread.
 * Impostato a {@code true} dalla chiusura della finestra o dal pulsante "Interrompi".
 */
public static boolean FineThread = false;

/** Se {@code true}, i pannelli di log non vengono collegati allo stdout/stderr. */
public boolean nascondiLog = false;

private Timer timer = new Timer(1000, new ActionListener() {
        private int counter = 1;
        @Override
        public void actionPerformed(ActionEvent ae) {
            counter++;
            if (counter > 1000) {
                timer.stop();
            }
        }
    });


    /**
     * Costruisce il dialogo, inizializza i componenti grafici e applica le
     * personalizzazioni post-designer (font monospaziato per i log, dimensione minima).
     */
    public Download() {
        ImageIcon icon = new ImageIcon(VarStatiche.getPathRisorse() + "logo.png");
        this.setIconImage(icon.getImage());
        initComponents();
        Download.FineThread = false;
        Principale.InterrompiCiclo = false;

        // Font derivati dal tema corrente: bold per i label di sezione, +2pt per il titolo
        LabelScaricamento.setFont(LabelScaricamento.getFont().deriveFont(
                LabelScaricamento.getFont().getStyle() | Font.BOLD,
                LabelScaricamento.getFont().getSize() + 2f));
        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD));
        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | Font.BOLD));

        // Font monospaziato per i pannelli di log: leggibile e indipendente dal tema
        Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        textPane.setFont(monoFont);
        textPaneErrori.setFont(monoFont);
    }

    /**
     * Configura il dialogo per la modalità di attesa indeterministica, nascondendo
     * tutti i controlli non rilevanti e impostando la progress bar come spinner.
     * Utile per operazioni di durata sconosciuta (es. connessioni di rete).
     *
     * @param Titolo    testo visualizzato nella barra del titolo del dialogo
     * @param Messaggio testo mostrato all'interno della progress bar
     */
    public void MostraProgressAttesa(String Titolo, String Messaggio) {
        Bottone_Interrompi.setEnabled(false);
        Bottone_Interrompi.setVisible(false);
        LabelScaricamento.setEnabled(false);
        LabelScaricamento.setVisible(false);
        textPane.setEnabled(false);
        textPane.setVisible(false);
        jScrollPane3.setEnabled(false);
        jScrollPane3.setVisible(false);
        jScrollPane4.setEnabled(false);
        jScrollPane4.setVisible(false);
        jLabel1.setEnabled(false);
        jLabel1.setVisible(false);
        jLabel2.setEnabled(false);
        jLabel2.setVisible(false);
        jSeparator1.setEnabled(false);
        jSeparator1.setVisible(false);
        LabelAvanzamento.setEnabled(false);
        LabelAvanzamento.setVisible(false);
        ProgressBarDownload.setIndeterminate(true);
        ProgressBarDownload.setString(Messaggio);
        ProgressBarDownload.setStringPainted(true);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setTitle(Titolo);
        this.setSize(new Dimension(300,80));
       /* pack();
        if (getWidth() < 300) setSize(300, getHeight());*/
    }

    /**
     * Attiva o disattiva la modalità indeterministica della progress bar.
     * Quando indeterministica, la barra mostra uno spinner animato invece di un valore.
     *
     * @param b {@code true} per attivare lo spinner, {@code false} per tornare alla
     *          modalità deterministica
     */
    public void setIndeterminate(boolean b) {
        ProgressBarDownload.setIndeterminate(b);
        ProgressBarDownload.setString("");
    }

    /**
     * Associa il thread dell'operazione monitorata a questo dialogo.
     * Il thread viene controllato da {@link #FineThread()} per rilevare
     * la terminazione automatica.
     *
     * @param T thread da monitorare
     */
    public void SetThread(Thread T) {
        thread = T;
    }

    /**
     * Nasconde la progress bar e l'etichetta di avanzamento.
     * Utile quando si vuole mostrare solo i pannelli di log.
     */
    public void NascondiBarra() {
        ProgressBarDownload.setVisible(false);
        LabelAvanzamento.setVisible(false);
    }

    /**
     * Disabilita e nasconde il pulsante "Interrompi".
     * Chiamare questo metodo per operazioni che non devono essere interrotte dall'utente.
     */
    public void NascondiInterrompi() {
        Bottone_Interrompi.setEnabled(false);
        Bottone_Interrompi.setVisible(false);
    }

    /**
     * Verifica se l'operazione è terminata e, in caso affermativo, chiude il dialogo.
     * Reimposta anche {@link Principale#InterrompiCiclo} a {@code false}.
     *
     * @return {@code true} se il flag {@link #FineThread} è attivo (operazione terminata
     *         o interrotta), {@code false} altrimenti
     */
    public Boolean FineThread() {
        if (thread != null && !thread.isAlive()) {
            this.dispose();
        }
        Principale.InterrompiCiclo = false;
        return Download.FineThread;
    }

    /**
     * Imposta il dialogo come non modale, permettendo all'utente di interagire
     * con le altre finestre dell'applicazione durante l'operazione.
     */
    public void NoModale() {
        this.setModalityType(ModalityType.MODELESS);
    }

    /**
     * Avvia il timer interno (usato internamente per operazioni di pausa).
     */
    public void Pausa() {
        timer.start();
    }

    /**
     * Aggiorna il testo dell'etichetta di stato principale.
     *
     * @param testo nuovo testo da mostrare (es. "Importazione in corso…")
     */
    public void SetLabel(String testo) {
        this.LabelScaricamento.setText(testo);
    }

    /**
     * Imposta il testo nella barra del titolo del dialogo.
     *
     * @param testo titolo da visualizzare
     */
    public void Titolo(String testo) {
        this.setTitle(testo);
    }

    /**
     * Imposta il valore massimo della progress bar.
     * Deve essere chiamato prima di {@link #SetAvanzamento(int)}.
     *
     * @param Max numero totale di step dell'operazione
     */
    public void SetMassimo(int Max) {
        Massimo = Max;
        ProgressBarDownload.setMaximum(Massimo);
    }

    /**
     * Aggiorna la progress bar e l'etichetta di avanzamento con il valore corrente.
     *
     * @param Avanzamento step corrente (deve essere compreso tra 0 e {@link #Massimo})
     */
    public void SetAvanzamento(int Avanzamento) {
        avanzamento = Avanzamento;
        ProgressBarDownload.setValue(Avanzamento);
        LabelAvanzamento.setText("Elaborazione " + Avanzamento + " di " + Massimo);
    }

    /**
     * Scollega i pannelli di log dallo stdout e dallo stderr, ripristinando
     * l'output standard della JVM. Imposta anche {@link #nascondiLog} a {@code true}
     * per evitare di ricollegare i pannelli alla riapertura.
     */
    public void RipristinaStdout() {
        nascondiLog = true;
        LoggerGC.disableTextPaneOut();
        LoggerGC.disableTextPaneErr();
    }

    /**
     * Imposta un messaggio testuale personalizzato nell'etichetta di avanzamento,
     * in alternativa al formato "Elaborazione N di M" usato da {@link #SetAvanzamento(int)}.
     *
     * @param Messaggio testo libero da mostrare
     */
    public void SetMessaggioAvanzamento(String Messaggio) {
        LabelAvanzamento.setText(Messaggio);
    }

    /**
     * Scollega i log, chiude e distrugge il dialogo.
     * Equivalente a chiamare {@link #RipristinaStdout()} seguito da {@code dispose()}.
     */
    public void ChiudiFinestra() {
        RipristinaStdout();
        this.dispose();
    }

    /**
     * Aggiorna direttamente il valore della progress bar senza aggiornare l'etichetta.
     * Preferire {@link #SetAvanzamento(int)} per aggiornamenti completi.
     *
     * @param value valore da impostare sulla progress bar
     */
    public void updateProgress(int value) {
        ProgressBarDownload.setValue(value);
    }

    /**
     * Verifica se il pannello degli errori contiene messaggi relativi a una
     * terminazione anomala di Node.js (codice di uscita 1).
     *
     * @return {@code true} se il testo degli errori contiene la stringa
     *         "Node.js terminato con codice 1", {@code false} altrimenti
     */
    public boolean ErroriNodeJS() {
        return textPaneErrori.getText().toLowerCase().contains("Node.js terminato con codice 1".toLowerCase());
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        LabelScaricamento = new javax.swing.JLabel();
        ProgressBarDownload = new javax.swing.JProgressBar();
        LabelAvanzamento = new javax.swing.JLabel();
        Bottone_Interrompi = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        textPane = new javax.swing.JTextPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        textPaneErrori = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        LabelScaricamento.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabelScaricamento.setText("Importazione del file in corso....");

        ProgressBarDownload.setStringPainted(true);

        LabelAvanzamento.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabelAvanzamento.setText("      ");

        Bottone_Interrompi.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Bottone_Interrompi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_ImbutoX.png"))); // NOI18N
        Bottone_Interrompi.setText("<html><h2>Interrompi Elaborazione</h2></html>");
        Bottone_Interrompi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_InterrompiActionPerformed(evt);
            }
        });

        jLabel1.setText("Dettaglio:");

        jLabel2.setText("Errori :");

        jScrollPane3.setViewportView(textPane);

        jScrollPane4.setViewportView(textPaneErrori);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LabelScaricamento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ProgressBarDownload, javax.swing.GroupLayout.DEFAULT_SIZE, 975, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addComponent(jScrollPane3)
                    .addComponent(jScrollPane4)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(Bottone_Interrompi)
                    .addComponent(LabelAvanzamento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(LabelScaricamento, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ProgressBarDownload, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(LabelAvanzamento)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Bottone_Interrompi, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleParent(null);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        Principale.InterrompiCiclo = true;
        Download.FineThread = true;
        LoggerGC.disableTextPaneOut();
        LoggerGC.disableTextPaneErr();
    }//GEN-LAST:event_formWindowClosed

    private void Bottone_InterrompiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_InterrompiActionPerformed
        Principale.InterrompiCiclo = true;
        Download.FineThread = true;
        Bottone_Interrompi.setEnabled(false);
        LabelAvanzamento.setText("Interruzione in corso...");
        //Bottone_Interrompi.setText("Interruzione in corso...");
        
        System.out.println("Premuto tasto interrompi sull'operazione in corso");
        if (!textPaneErrori.getText().isBlank()) {
            this.dispose();
        }
    }//GEN-LAST:event_Bottone_InterrompiActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        Principale.InterrompiCiclo = false;
        Download.FineThread = false;
        if (!nascondiLog) {
            LoggerGC.enableTextPaneOut(textPane);
            LoggerGC.enableTextPaneErr(textPaneErrori);
        }
    }//GEN-LAST:event_formWindowOpened


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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Download.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Interrompi;
    private javax.swing.JLabel LabelAvanzamento;
    private javax.swing.JLabel LabelScaricamento;
    private javax.swing.JProgressBar ProgressBarDownload;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextPane textPane;
    private javax.swing.JTextPane textPaneErrori;
    // End of variables declaration//GEN-END:variables

}
