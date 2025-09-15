/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.giacenzecrypto.giacenze_crypto;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.Timer;



/**
 *
 * @author luca.passelli
 */
public class Download extends javax.swing.JDialog {
     private static final long serialVersionUID = 4L;

    /**
     * Creates new form Attesa
     */
public int Massimo;
public int avanzamento;
public transient Thread thread;
public static boolean FineThread=false;
public boolean nascondiLog=false;

//    static boolean DownloadTerminato=false;
   // static boolean finito=false;

private Timer timer = new Timer(1000, new ActionListener() {

        private int counter = 1;

        @Override
        public void actionPerformed(ActionEvent ae) {
           // SetAvanzamento(counter);
           // bar.setValue(++counter);
           counter++;
            if (counter > 1000) {
                timer.stop();
            }
        }
    });





    public Download() {

     //   finestra=c;
      //  finestra.setEnabled(false);
         ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
         this.setIconImage(icon.getImage());
         initComponents();
         Download.FineThread=false;
         CDC_Grafica.InterrompiCiclo=false;
     
        
//RipristinaStdout();

     

    }
    
       public void MostraProgressAttesa(String Titolo,String Messaggio){
       // ProgressBarDownload.setVisible(false);
       // LabelAvanzamento.setVisible(false);
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
        LabelAvanzamento.setEnabled(false);
        LabelAvanzamento.setVisible(false);
        this.setSize(new Dimension(300,80));
        ProgressBarDownload.setIndeterminate(true);
        ProgressBarDownload.setString(Messaggio);
        ProgressBarDownload.setStringPainted(true);
        //ProgressBarDownload.setSize(300, 70);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setTitle(Titolo);
    } 
    
    
    public void setIndeterminate(boolean b){
        ProgressBarDownload.setIndeterminate(b);
        ProgressBarDownload.setString("");
    }
    
    public void SetThread(Thread T){
        thread=T;
    }
    
    public void NascondiBarra(){
        ProgressBarDownload.setVisible(false);
        LabelAvanzamento.setVisible(false);
    }
    
    public void NascondiInterrompi(){
        Bottone_Interrompi.setEnabled(false);
        Bottone_Interrompi.setVisible(false);
    }
    
    public Boolean FineThread(){
        
        if (thread!=null&&!thread.isAlive()){
            this.dispose();
        }
        CDC_Grafica.InterrompiCiclo=false;
        return Download.FineThread;
    }
    
    public void NoModale(){
    this.setModalityType(ModalityType.MODELESS);
}
     public void Pausa(){
         timer.start();
     }
     
     public void SetLabel(String testo){
         this.LabelScaricamento.setText(testo);
     }
     
     public void Titolo(String testo){
         this.setTitle(testo);
     }
    
     public void SetMassimo(int Max){

                     Massimo=Max;
         ProgressBarDownload.setMaximum(Massimo);


     }
    
     public void SetAvanzamento (int Avanzamento) {
 //Thread thread = new Thread() {
 //           public void run() {
         avanzamento=Avanzamento;
         ProgressBarDownload.setValue(Avanzamento);
        // ProgressBarDownload.setStringPainted(true);
         LabelAvanzamento.setText("Elaborazione "+Avanzamento+" di "+Massimo);
 //}      };
// thread.start();

     }
     
     public void RipristinaStdout()
             {
                 nascondiLog=true;
        LoggerGC.disableTextPaneOut();
        LoggerGC.disableTextPaneErr();
     }
             
    public void SetMessaggioAvanzamento (String Messaggio) {

         LabelAvanzamento.setText(Messaggio);
     }
     
     public void ChiudiFinestra (){
        RipristinaStdout();
        this.dispose();
     }
     
     
        public void updateProgress(int value) {
        ProgressBarDownload.setValue(value);
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
        LabelAvanzamento.setText("Avanzamento");

        Bottone_Interrompi.setText("Interrompi");
        Bottone_Interrompi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_InterrompiActionPerformed(evt);
            }
        });

        jLabel1.setText("Dettaglio :");

        jLabel2.setText("Errori Bloccanti :");

        jScrollPane3.setViewportView(textPane);

        jScrollPane4.setViewportView(textPaneErrori);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(244, 244, 244)
                                .addComponent(Bottone_Interrompi, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(ProgressBarDownload, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE)
                            .addComponent(LabelAvanzamento, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(LabelScaricamento, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(LabelScaricamento, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ProgressBarDownload, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(LabelAvanzamento)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Bottone_Interrompi)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleParent(null);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        CDC_Grafica.InterrompiCiclo=true;       
        Download.FineThread=true;

               
       // CDC_Grafica.InterrompiCiclo=false;
        LoggerGC.disableTextPaneOut();
        LoggerGC.disableTextPaneErr();

    }//GEN-LAST:event_formWindowClosed

    
    private void Bottone_InterrompiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_InterrompiActionPerformed
        // TODO add your handling code here:
        //this.dispose();
        CDC_Grafica.InterrompiCiclo=true;       
        Download.FineThread=true;
        Bottone_Interrompi.setBackground(Color.red);
        Bottone_Interrompi.setText("Interruzione in corso ...");
        if (!textPaneErrori.getText().isBlank())
            {
            this.dispose();
            }
    }//GEN-LAST:event_Bottone_InterrompiActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
     //   System.out.println("Finestra Attesa Aperta");
     CDC_Grafica.InterrompiCiclo=false;
     Download.FineThread=false;
     if (!nascondiLog) {
       /* PrintStream printStream = new PrintStream(new CustomOutputStream(textPane));
        System.setOut(printStream);
        PrintStream printStreamErr = new PrintStream(new CustomOutputStream(textPaneErrori));
        System.setErr(printStreamErr);*/
        LoggerGC.enableTextPaneOut(textPane);
        LoggerGC.enableTextPaneErr(textPaneErrori);
        }
        //System.out.println("Finestra Attesa Aperta");
    }//GEN-LAST:event_formWindowOpened

    
    public boolean ErroriNodeJS(){
         return textPaneErrori.getText().toLowerCase().contains("Node.js terminato con codice 1".toLowerCase());
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Download.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
         //</editor-fold>
         //</editor-fold>
         //</editor-fold>
         //</editor-fold>
         /* Create and display the form */
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */

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
    private javax.swing.JTextPane textPane;
    private javax.swing.JTextPane textPaneErrori;
    // End of variables declaration//GEN-END:variables




 /*  public class CustomOutputStream extends OutputStream {
  
       

    private final JTextPane textPane;

    public CustomOutputStream(JTextPane textPane) {
        this.textPane = textPane;
    }

    @Override
    public void write(int b) throws IOException {
        // redirects data to the text area
        //textPane.setText(textPane.getText() + String.valueOf((char)b));
        
        StyledDocument document = (StyledDocument) textPane.getDocument();
        try {
            document.insertString(document.getLength(), String.valueOf((char)b), null);
        } catch (BadLocationException ex) {
            Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
        }
     //   textArea.append(String.valueOf((char)b));
        // scrolls the text area to the end of data
        textPane.setCaretPosition(textPane.getDocument().getLength());
        // keeps the textArea up to date
    //    textArea.update(textArea.getGraphics());
    }
    
    
} */
    
    
    
    


}
