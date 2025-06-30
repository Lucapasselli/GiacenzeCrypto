/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;


import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.DecimaliCalcoli;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author lucap
 */
public class GUI_LiFoTransazione extends javax.swing.JFrame {

    /**
     * Creates new form Gui_LiFo_Transazione
     */
    private static String ID="";
    private String Movimento[];
    public MultiSelectPopup popup = new MultiSelectPopup(this);
    
    public GUI_LiFoTransazione(String IDtr) {
        ID=IDtr;
        Movimento=CDC_Grafica.MappaCryptoWallet.get(IDtr);       
        initComponents();
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        setIconImage(icon.getImage());
        setTitle("LiFo Transazione : "+ID);
        InizializzaOggetti();        
    }
    
    private void InizializzaOggetti(){
         //Inizializza Label e Bottoni
        if (!Movimento[8].isBlank()){
            LabelLifoMU.setText("<html>L.i.F.o. Moneta Uscita : <b>"+Movimento[10]+" "+Movimento[8]+"</b></html>");
            this.Bottone_MU_FrecciaDestra.setEnabled(true);
            this.Bottone_MU_FrecciaDestra.setToolTipText("Vedi stack relativo al movimento successivo di : "+Movimento[8]);
            this.Bottone_MU_FrecciaSinistra.setEnabled(true);
            this.Bottone_MU_FrecciaSinistra.setToolTipText("Vedi stack relativo al movimento precedente di : "+Movimento[8]);
        }else
        {
            this.Bottone_MU_FrecciaDestra.setEnabled(false);
            this.Bottone_MU_FrecciaDestra.setToolTipText("");
            this.Bottone_MU_FrecciaSinistra.setEnabled(false);
            this.Bottone_MU_FrecciaSinistra.setToolTipText("");
        }
        if (!Movimento[11].isBlank()){
            LabelLifoME.setText("<html>L.i.F.o. Moneta Entrata : <b>"+Movimento[13]+" "+Movimento[11]+"</b></html>");
            this.Bottone_ME_FrecciaDestra.setEnabled(true);
            this.Bottone_ME_FrecciaDestra.setToolTipText("Vedi stack relativo al movimento successivo di : "+Movimento[11]);
            this.Bottone_ME_FrecciaSinistra.setEnabled(true);
            this.Bottone_ME_FrecciaSinistra.setToolTipText("Vedi stack relativo al movimento precedente di : "+Movimento[11]);           
        }else
        {
            this.Bottone_ME_FrecciaDestra.setEnabled(false);
            this.Bottone_ME_FrecciaDestra.setToolTipText("");
            this.Bottone_ME_FrecciaSinistra.setEnabled(false);
            this.Bottone_ME_FrecciaSinistra.setToolTipText("");
        }
        
        //Compilo le tabelle
        Calcoli_PlusvalenzeNew.LifoXID lifoID=Calcoli_PlusvalenzeNew.getIDLiFo(ID);
        if (lifoID!=null){
        ArrayDeque<String[]> StackEntrato=lifoID.Get_CryptoStackEntrato();
        //Stack della moneta entrata prima el movimento
        ArrayDeque<String[]> StackEntratoPreMovimento=lifoID.Get_CryptoStackEntratoPreMovimento();
        //Parte dello stack della moneta uscita relativo alla transazione
        ArrayDeque<String[]> StackUscito=lifoID.Get_CryptoStackUscito();
        //Rimanenze dello stack della moneta uscita dopo la transazione
        ArrayDeque<String[]> StackUscitoRimanenze=lifoID.Get_CryptoStackUscitoRimanenze();
        
        Tabelle.Funzioni_Tabelle_PulisciTabella((DefaultTableModel)Tabella_Lifo_Entrata.getModel());
        Tabelle.Funzioni_Tabelle_PulisciTabella((DefaultTableModel)Tabella_Lifo_Uscita.getModel());
        int ordinamento =PopolaTabella(Tabella_Lifo_Entrata,StackEntrato,true,0);
        PopolaTabella(Tabella_Lifo_Entrata,StackEntratoPreMovimento,false,ordinamento);
        ordinamento =PopolaTabella(Tabella_Lifo_Uscita,StackUscito,true,0);
        PopolaTabella(Tabella_Lifo_Uscita,StackUscitoRimanenze,false,ordinamento);
        }
        
    }

    
    
    private int PopolaTabella(JTable table,ArrayDeque<String[]> Stack,boolean RealtivoAMovimento,int ordinamento){
        DefaultTableModel ModelloTabella;
        if (ordinamento==0)ModelloTabella = inizializzaTabella(table);
        else ModelloTabella = (DefaultTableModel) table.getModel();
        ArrayDeque<String[]> stack=Stack.clone();
        int i=ordinamento;
        while (!stack.isEmpty()) {
          //  String ordine=String.valueOf(i);
            String[] ultimoRecupero = stack.pop();
            Object[] riga = new Object[9];
            riga[0]=i;
            riga[1]=ultimoRecupero[3];
            riga[2]=CDC_Grafica.MappaCryptoWallet.get(ultimoRecupero[3])[1];
            riga[3]=CDC_Grafica.MappaCryptoWallet.get(ultimoRecupero[3])[6];
            riga[4]=ultimoRecupero[0];
            riga[5]=ultimoRecupero[1];
            riga[6]=formattaNumero(ultimoRecupero[2]);
            riga[7]=RealtivoAMovimento;
            if(!ultimoRecupero[1].isBlank()&&new BigDecimal(ultimoRecupero[1]).compareTo(BigDecimal.ZERO)!=0)
                riga[8]=formattaNumero(new BigDecimal(ultimoRecupero[2]).divide(new BigDecimal(ultimoRecupero[1]), DecimaliCalcoli, RoundingMode.HALF_UP).abs().stripTrailingZeros().toPlainString());
            ModelloTabella.addRow(riga);
            i++;
        }
    return i;
    }
    
    
    private String formattaNumero(String Sinput) {
        
        /*
         Massimo 10 cifre significative, sempre.
         Almeno 2 decimali, anche se le cifre intere sono molte.        
        */
        BigDecimal input=new BigDecimal(Sinput);
        
        if (input == null) return null;

        // Rimuove zeri inutili per contare correttamente le cifre
        input = input.stripTrailingZeros();

        // Applica massimo 10 cifre significative
        BigDecimal rounded = input.round(new MathContext(10, RoundingMode.HALF_UP));

        // Assicura almeno 2 decimali
        if (rounded.scale() < 2) {
            rounded = rounded.setScale(2, RoundingMode.HALF_UP);
        }

        return rounded.stripTrailingZeros().toPlainString();
    }
    
    
    
    
    private DefaultTableModel inizializzaTabella(JTable table){
        DefaultTableModel ModelloTabella = (DefaultTableModel) table.getModel();
        Tabelle.Funzioni_Tabelle_PulisciTabella(ModelloTabella);
        Tabelle.ColoraTabellaLiFoTransazione(table);
        Tabelle.Tabelle_FiltroColonne(table,null,popup);
        return ModelloTabella;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        LabelLifoMU = new javax.swing.JLabel();
        LabelLifoME = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabella_Lifo_Uscita = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        Tabella_Lifo_Entrata = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        Bottone_MU_FrecciaSinistra = new javax.swing.JButton();
        Bottone_MU_FrecciaDestra = new javax.swing.JButton();
        Bottone_ME_FrecciaDestra = new javax.swing.JButton();
        Bottone_ME_FrecciaSinistra = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        LabelLifoMU.setText("L.i.F.o. Moneta Uscita");

        LabelLifoME.setText("L.i.F.o. Moneta Entrata");

        Tabella_Lifo_Uscita.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Ordine", "ID", "Data Origine", "Movimento Origine", "Moneta", "Qta", "Costo di Carico", "Relativo a Movimento", "Costo Unitario"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Tabella_Lifo_Uscita.setName("Uscita"); // NOI18N
        jScrollPane1.setViewportView(Tabella_Lifo_Uscita);
        if (Tabella_Lifo_Uscita.getColumnModel().getColumnCount() > 0) {
            Tabella_Lifo_Uscita.getColumnModel().getColumn(0).setMinWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(0).setPreferredWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(0).setMaxWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(1).setMinWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(1).setPreferredWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(1).setMaxWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(7).setMinWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(7).setPreferredWidth(0);
            Tabella_Lifo_Uscita.getColumnModel().getColumn(7).setMaxWidth(0);
        }

        Tabella_Lifo_Entrata.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Ordine", "ID", "Data Origine", "Movimento Origine", "Moneta", "Qta", "Costo di Carico", "Relativo a Movimento", "Costo Unitario"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Tabella_Lifo_Entrata.setName("Entrata"); // NOI18N
        jScrollPane2.setViewportView(Tabella_Lifo_Entrata);
        if (Tabella_Lifo_Entrata.getColumnModel().getColumnCount() > 0) {
            Tabella_Lifo_Entrata.getColumnModel().getColumn(0).setMinWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(0).setPreferredWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(0).setMaxWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(1).setMinWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(1).setPreferredWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(1).setMaxWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(7).setMinWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(7).setPreferredWidth(0);
            Tabella_Lifo_Entrata.getColumnModel().getColumn(7).setMaxWidth(0);
        }

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/48_FrecciaSotto.png"))); // NOI18N

        Bottone_MU_FrecciaSinistra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N

        Bottone_MU_FrecciaDestra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N

        Bottone_ME_FrecciaDestra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N

        Bottone_ME_FrecciaSinistra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 912, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(LabelLifoMU, javax.swing.GroupLayout.PREFERRED_SIZE, 546, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_MU_FrecciaSinistra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_MU_FrecciaDestra))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(LabelLifoME, javax.swing.GroupLayout.PREFERRED_SIZE, 546, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_ME_FrecciaSinistra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_ME_FrecciaDestra))
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(LabelLifoMU, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Bottone_MU_FrecciaSinistra)
                            .addComponent(Bottone_MU_FrecciaDestra))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(LabelLifoME, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Bottone_ME_FrecciaDestra)
                    .addComponent(Bottone_ME_FrecciaSinistra))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE)
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
            java.util.logging.Logger.getLogger(GUI_LiFoTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI_LiFoTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI_LiFoTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI_LiFoTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI_LiFoTransazione(ID).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_ME_FrecciaDestra;
    private javax.swing.JButton Bottone_ME_FrecciaSinistra;
    private javax.swing.JButton Bottone_MU_FrecciaDestra;
    private javax.swing.JButton Bottone_MU_FrecciaSinistra;
    private javax.swing.JLabel LabelLifoME;
    private javax.swing.JLabel LabelLifoMU;
    private javax.swing.JTable Tabella_Lifo_Entrata;
    private javax.swing.JTable Tabella_Lifo_Uscita;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
