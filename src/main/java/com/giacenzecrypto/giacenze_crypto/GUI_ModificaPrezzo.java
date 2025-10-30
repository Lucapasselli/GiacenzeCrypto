/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author luca.passelli
 */
public class GUI_ModificaPrezzo extends javax.swing.JDialog {
    
    //private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GUI_ModificaPrezzo.class.getName());
    public String ID;

    /**
     * Creates new form GUI_ModificaPrezzo
     */
    public GUI_ModificaPrezzo() {
        initComponents();
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        this.setIconImage(icon.getImage());
        
    }
    public GUI_ModificaPrezzo(String ID) {
        initComponents();
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        this.setIconImage(icon.getImage());  
        
        CaricaTabellaPrezzoAttualedaID(ID); 
        CaricaTabellaPrezzi(ID);
        
    }
    
    private void CaricaTabellaPrezzoAttualedaID(String ID){
        DefaultTableModel ModTabPrezzoAttuale = (DefaultTableModel) Tabella_PrezzoAttuale.getModel();
        Tabelle.Funzioni_PulisciTabella(ModTabPrezzoAttuale);
        Tabelle.ColoraTabellaSemplice(Tabella_PrezzoAttuale);
                
        
        String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
        String ora=Movimento[0].split("_")[0].substring(12);
        ora=Movimento[1]+":"+ora;
        long data=OperazioniSuDate.ConvertiDatainLongSecondo(ora);
        //Compilo la tabella del prezzo attuale
        String PrezzoAttualeE[]=new String[8];
        String PrezzoAttualeU[]=new String[8];
        if(!Movimento[8].isBlank()){
            PrezzoAttualeU[0]=Movimento[8];
            PrezzoAttualeU[1]=ora;
            PrezzoAttualeU[2]=Movimento[10];
            if(!Movimento[40].isBlank()){
                String VSplit[]=Movimento[40].split("\\|"); 
                if(VSplit[0].equalsIgnoreCase(Movimento[8])){
                    PrezzoAttualeU[3]=VSplit[3];
                    PrezzoAttualeU[4]=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(VSplit[1]));
                    long DiffOrario=Math.abs(data-Long.parseLong(VSplit[1]))/1000;
                    String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }    
                    PrezzoAttualeU[5]=String.valueOf(DiffOrario)+unitaTempo;
                    PrezzoAttualeU[6]=VSplit[2];
                }
            }
            PrezzoAttualeU[7]=Movimento[15];
            ModTabPrezzoAttuale.addRow(PrezzoAttualeU);
        }
        if(!Movimento[11].isBlank()){
            PrezzoAttualeE[0]=Movimento[11];
            PrezzoAttualeE[1]=ora;
            PrezzoAttualeE[2]=Movimento[13];
            if(!Movimento[40].isBlank()){
                String VSplit[]=Movimento[40].split("\\|"); 
                if(VSplit[0].equalsIgnoreCase(Movimento[11])){
                    PrezzoAttualeE[3]=VSplit[3];
                    PrezzoAttualeE[4]=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(VSplit[1]));
                    long DiffOrario=Math.abs(data-Long.parseLong(VSplit[1]))/1000;
                    String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        } 
                    PrezzoAttualeE[5]=String.valueOf(DiffOrario)+unitaTempo;
                    PrezzoAttualeE[6]=VSplit[2];
                }
            }
            PrezzoAttualeE[7]=Movimento[15];
            ModTabPrezzoAttuale.addRow(PrezzoAttualeE);
            
        }
    }
    
    private void CaricaTabellaPrezzi(String ID){
        DefaultTableModel ModTabPrezzi = (DefaultTableModel) Tabella_Prezzi.getModel();
        Tabelle.Funzioni_PulisciTabella(ModTabPrezzi);
        Tabelle.ColoraTabellaSemplice(Tabella_Prezzi);
                
        
        String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
        String ora=Movimento[0].split("_")[0].substring(12);
        ora=Movimento[1]+":"+ora;
        
        //PARTE 1 - Come prima cosa recupero i prezzi di entrambe le monete
        Moneta M[]=new Moneta[2];
        long data=OperazioniSuDate.ConvertiDatainLongSecondo(ora);
        String Rete = Funzioni.TrovaReteDaIMovimento(Movimento);

        M[0] = new Moneta();
        M[1] = new Moneta();
        if(Rete==null||CDC_Grafica.MappaRetiSupportate.get(Rete)==null){
                Rete="";
                M[0].MonetaAddress="";
                M[1].MonetaAddress="";
            }else{
                M[0].MonetaAddress = Movimento[26];
                M[1].MonetaAddress = Movimento[28];
            }
            M[0].Moneta = Movimento[8];
            M[0].Tipo = Movimento[9];
            M[0].Qta = Movimento[10];
            M[0].Rete = Rete;
            M[1].Moneta = Movimento[11];
            M[1].Tipo = Movimento[12];
            M[1].Qta = Movimento[13];
            M[1].Rete = Rete;
        //Forzo lo scaricamento dei prezzi di entrambe le monete
        for(int i=0;i<2;i++){
        if (M[i].Moneta!=null&&!M[i].Moneta.isBlank()){
            Prezzi.CambioXXXEUR(M[i].Moneta, M[i].Qta, data,M[i].MonetaAddress,M[i].Rete,"");
            
            if (!M[i].MonetaAddress.isBlank() && !M[i].Rete.isBlank()) {
                    List<Prezzi.InfoPrezzo> ListaIP = Prezzi.DammiListaPrezziDaDatabase("", data, M[i].Rete, M[i].MonetaAddress, 60, new BigDecimal(M[i].Qta));
                    for (Prezzi.InfoPrezzo IP : ListaIP) {
                        String rigo[] = new String[9];
                        rigo[0] = M[i].Moneta;
                        rigo[1] = ora;
                        rigo[2] = M[i].Qta;
                        rigo[3] = IP.exchange;
                        rigo[4] = OperazioniSuDate.ConvertiDatadaLongAlSecondo(IP.timestamp);
                        //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                        long DiffOrario=Math.abs(data-IP.timestamp)/1000;
                        String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }
                        rigo[5] = String.valueOf(DiffOrario)+unitaTempo;
                        rigo[6] = IP.prezzo.toPlainString();
                        rigo[7] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                        rigo[8] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
                        ModTabPrezzi.addRow(rigo);
                    }
                }
            //Prezzi.DammiPrezzoInfoTransazione(M1, null, data, Rete,"");
             List<Prezzi.InfoPrezzo> ListaIP=Prezzi.DammiListaPrezziDaDatabase(M[i].Moneta,data,"","",60,new BigDecimal(M[i].Qta));
            //Riempio la tabella con i prezzi
            for(Prezzi.InfoPrezzo IP:ListaIP){
                String rigo[]=new String[9];
                    rigo[0] = M[i].Moneta;
                    rigo[1] = ora;
                    rigo[2] = M[i].Qta;
                    rigo[3] = IP.exchange;
                    rigo[4] = OperazioniSuDate.ConvertiDatadaLongAlSecondo(IP.timestamp);
                    //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                    long DiffOrario=Math.abs(data-IP.timestamp)/1000;
                    String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }                       
                    rigo[5] = String.valueOf(DiffOrario)+unitaTempo;
                    
                    rigo[6] = IP.prezzo.toPlainString();
                    rigo[7] = IP.prezzoQta.setScale(2,RoundingMode.HALF_UP).toPlainString();
                    rigo[8] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
                    ModTabPrezzi.addRow(rigo);                
            }
                
            }
        }
        
    }
    
    
    public void GUI_SetID(String ID){
        this.ID=ID;
        jLabel1.setText(ID);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        Tabella_PrezzoAttuale = new javax.swing.JTable();
        Bottone_OK = new javax.swing.JButton();
        Bottone_Annulla = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        Tabella_Prezzi = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        Bottone_Personalizzato = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gestione Prezzo");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Tabella_PrezzoAttuale.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Moneta", "Orario", "Qta", "Fonte", "Orario Fonte", "Precisione", "Prezzo Unitario", "Prezzo Totale"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(Tabella_PrezzoAttuale);

        Bottone_OK.setText("OK");

        Bottone_Annulla.setText("Annulla");

        Tabella_Prezzi.setAutoCreateRowSorter(true);
        Tabella_Prezzi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Moneta", "Orario", "Qta", "Fonte", "Orario Fonte", "Precisione", "Prezzo Unitario", "Prezzo Totale", "Diff.Prezzo"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(Tabella_Prezzi);
        if (Tabella_Prezzi.getColumnModel().getColumnCount() > 0) {
            Tabella_Prezzi.getColumnModel().getColumn(5).setMinWidth(80);
            Tabella_Prezzi.getColumnModel().getColumn(5).setPreferredWidth(80);
            Tabella_Prezzi.getColumnModel().getColumn(5).setMaxWidth(80);
            Tabella_Prezzi.getColumnModel().getColumn(6).setMinWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(6).setPreferredWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(6).setMaxWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(7).setMinWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(7).setPreferredWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(7).setMaxWidth(100);
        }

        jLabel1.setText("Prezzo Attuale");

        jLabel2.setText("Prezzi Disponibili piu' vicini");

        jLabel3.setText("Fare doppio click sulla riga corrispndente al prezzo scelto o singolo click e conferma con il tasto ok per cambiare il prezzo alla transazione.");

        jLabel4.setText("In alternativa premere sul tasto \"Prezzo Personalizzato\" per inserire un prezzo personalizzato per il token/transazione.");

        Bottone_Personalizzato.setText("Prezzo Personalizzato");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Personalizzato)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_OK, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 975, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_OK)
                    .addComponent(Bottone_Annulla)
                    .addComponent(Bottone_Personalizzato))
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
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new GUI_ModificaPrezzo().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Annulla;
    private javax.swing.JButton Bottone_OK;
    private javax.swing.JButton Bottone_Personalizzato;
    private javax.swing.JTable Tabella_Prezzi;
    private javax.swing.JTable Tabella_PrezzoAttuale;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
