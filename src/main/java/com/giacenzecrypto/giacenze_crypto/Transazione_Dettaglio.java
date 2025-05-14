/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import javax.swing.table.DefaultTableModel;

/**
 *
 * @author luca.passelli
 */
public class Transazione_Dettaglio extends javax.swing.JDialog {
private static final long serialVersionUID = 8L;
    /**
     * Creates new form Importazioni_Resoconto
     */
    
        public void TransazioniCrypto_CompilaTextPaneDatiMovimento(String IDTransazione) {


            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) Tabella.getModel();

        
        //come prima cosa mi occupo del pulsante defi, deve essere attivo se abbiamo movimenti in defi e disattivo in caso contrario 
        //per controllare verifico di avere il transaction hash e il nome della rete quindi
        String Transazione[]=CDC_Grafica.MappaCryptoWallet.get(IDTransazione);
        String ReteDefi=Funzioni.RitornaReteDefi(IDTransazione);
        //System.out.println("retedefi:"+ReteDefi);
        String THash=Transazione[24];
        //System.out.println("hash:"+THash);
            if(!THash.isEmpty()&&!ReteDefi.isEmpty()){
                Bottone_DeFi.setEnabled(true);
            }else{
                Bottone_DeFi.setEnabled(false);
            }
        
        String Valore;
        String Val[];
        

        
        Valore=Transazione[1];
        if (!Valore.isBlank()){
            Valore="<html><b>"+Valore+"</html>";
            Val=new String[]{"Data e Ora ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[3];
        if (!Valore.isBlank()){
            Val=new String[]{"Exchange/Wallet ",Valore+" ("+Transazione[4]+")"};
            ModelloTabellaCrypto.addRow(Val);
        }
        
            Valore = Transazione[5];
            if (!Valore.isBlank()) {
                if (Transazione[20].isBlank()) {
                    Val = new String[]{"Causale Movimento ", "<html><b>" + Valore + "</b> (" + Transazione[6] + ")</html>"};
                } else {
                    String WalletPrelievo = "";
                    String WalletDeposito = "";
                    String Movimenti[] = (Transazione[20]+","+Transazione[0]).split(",");
                    if (Movimenti.length < 3)//Sono in presenza di uno scambio differito
                    {
                        for (String IdM : Movimenti) {
                            String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IdM);
                            if (Mov[18].contains("PTW")) {
                                WalletPrelievo = Mov[3];
                            }
                            if (Mov[18].contains("DTW")) {
                                WalletDeposito = Mov[3];
                            }
                        }
                    }
                    Val = new String[]{"Causale Movimento ", "<html><b>" + Valore + "</b> (" + Transazione[6] + ")<br>"
                            +"Trasferimento da <b>"+ WalletPrelievo+"</b> a <b>"+WalletDeposito+"</html>"};
                }
                ModelloTabellaCrypto.addRow(Val);
            }
        
        Valore=Transazione[31];
        if (!Valore.isBlank()){
            Val=new String[]{"Data e Ora fine trasferimento",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
       /* Valore=Transazione[20];
        if (!Valore.isBlank()){
            Val=new String[]{"Movimenti Correlati ","<html>"+Valore.replaceAll(",", "<br>")+"</html>"};
            ModelloTabellaCrypto.addRow(Val);
        }*/
        Valore=Transazione[18];
        if (!Valore.isBlank()){
            Val=new String[]{"Dett. ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[7];
        if (!Valore.isBlank()){
            Val=new String[]{"Causale Originale ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[8];
        if (!Valore.isBlank()){
            String Testo="<html><b>"+Transazione[10]+ " " + Transazione[8].split("\\(")[0];
            if (!Transazione[25].isBlank()&&!Transazione[8].equalsIgnoreCase(Transazione[25])){
                Testo=Testo+" </b>("+Transazione[25]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Uscita: ",Testo};
            
            ModelloTabellaCrypto.addRow(Val);
        }
        Valore=Transazione[9];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Tipologia",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
      /*  Valore=Transazione[25];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Nome Completo",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } */
        
        Valore=Transazione[26];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Address Token",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[16];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Costo Carico","€ "+Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[11];
        if (!Valore.isBlank()){            
            String Testo="<html><b>"+Transazione[13]+ " " + Transazione[11].split("\\(")[0];
            if (!Transazione[27].isBlank()&&!Transazione[27].equalsIgnoreCase(Transazione[11])){
                Testo=Testo+" </b>("+Transazione[27]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Entrata: ",Testo};
            ModelloTabellaCrypto.addRow(Val);
        }
        Valore=Transazione[12];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Tipologia",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
      /*  Valore=Transazione[27];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Nome Completo",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } */
        
        Valore=Transazione[28];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Address Token",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[17];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Costo Carico","€ "+Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[15];
        if (!Valore.isBlank()){
            Valore="<html><b>€ "+Valore+"</html>";
            Val=new String[]{"Valore transazione ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[19];
        if (!Valore.isBlank()){
            Valore="<html><b>€ "+Valore+"</html>";
            Val=new String[]{"Plusvalenza ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        

        
        Valore=Transazione[29];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Timestamp",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[23];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Numero Blocco",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }        

        Valore=Transazione[24];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Hash Transazione",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 

        Valore=Transazione[30];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Address Controparte",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 


        Valore=Transazione[21];
        if (!Valore.isBlank()){
            Valore=("<html>"+Valore+"</html>");
            Val=new String[]{"Note ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[0];
        if (!Valore.isBlank()){
            Val=new String[]{"ID ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        
        Tabelle.ColoraTabellaSemplice(Tabella);
        Tabelle.updateRowHeights(Tabella);

    }
    
    public Transazione_Dettaglio() {
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();

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
        ScrollTabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_DeFi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        Label_Titolo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        Label_Titolo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Label_Titolo.setText("DETTAGLIO MOVIMENTO");

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome", "Valore"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ScrollTabella.setViewportView(Tabella);
        if (Tabella.getColumnModel().getColumnCount() > 0) {
            Tabella.getColumnModel().getColumn(0).setMinWidth(200);
            Tabella.getColumnModel().getColumn(0).setPreferredWidth(200);
            Tabella.getColumnModel().getColumn(0).setMaxWidth(200);
        }

        Bottone_DeFi.setText("Dettaglio DeFi");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Label_Titolo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 853, Short.MAX_VALUE)
                    .addComponent(ScrollTabella)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_DeFi, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Label_Titolo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollTabella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(Bottone_DeFi)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
            java.util.logging.Logger.getLogger(Transazione_Dettaglio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Transazione_Dettaglio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Transazione_Dettaglio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Transazione_Dettaglio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Transazione_Dettaglio().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_DeFi;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JScrollPane ScrollTabella;
    private javax.swing.JTable Tabella;
    // End of variables declaration//GEN-END:variables
}
