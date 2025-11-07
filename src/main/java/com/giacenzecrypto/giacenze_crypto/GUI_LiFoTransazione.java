/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;


import static com.giacenzecrypto.giacenze_crypto.Principale.DecimaliCalcoli;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Map;
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
   // private String Movimento[];
    public Tabelle_PopupSelezioneMultipla popup = new Tabelle_PopupSelezioneMultipla(this);
    
    public GUI_LiFoTransazione(String IDtr) {
      //  ID=IDtr;              
        initComponents();
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        this.Bottone_ME_FrecciaDestra.setIcon(Icone.FrecciaDestra);
        this.Bottone_MU_FrecciaDestra.setIcon(Icone.FrecciaDestra);
        this.Bottone_ME_FrecciaSinistra.setIcon(Icone.FrecciaSinistra);
        this.Bottone_MU_FrecciaSinistra.setIcon(Icone.FrecciaSinistra);
        setIconImage(icon.getImage());       
        InizializzaOggetti(IDtr);        
    }
    
    private void InizializzaOggetti(String IDtr){
        ID=IDtr;
        String Movimento[]=Principale.MappaCryptoWallet.get(IDtr);
        setTitle("LiFo Transazione : "+IDtr);
        LabelMovimento.setText("<html><center><b><span style=\"font-size: 18px;\">"+Movimento[5]+" ("+Movimento[6]+")</span></b></html>");
         //Inizializza Label e Bottoni
        if (!Movimento[8].isBlank()){
            LabelLifoMU.setText("<html>L.i.F.o. Moneta Uscita : <b><span style=\"font-size: 18px;\">"+Movimento[10]+" "+Movimento[8]+"</span></b></html>");
            this.Bottone_MU_FrecciaDestra.setEnabled(true);
            this.Bottone_MU_FrecciaDestra.setToolTipText("<html>Vedi stack relativo al movimento successivo di : <b>"+Movimento[8]+"</b></html>");
            this.Bottone_MU_FrecciaSinistra.setEnabled(true);
            this.Bottone_MU_FrecciaSinistra.setToolTipText("<html>Vedi stack relativo al movimento precedente di : <b>"+Movimento[8]+"</b></html>");
        }else
        {
            LabelLifoMU.setText("<html>L.i.F.o. Moneta Uscita</html>");
            this.Bottone_MU_FrecciaDestra.setEnabled(false);
            this.Bottone_MU_FrecciaDestra.setToolTipText("");
            this.Bottone_MU_FrecciaSinistra.setEnabled(false);
            this.Bottone_MU_FrecciaSinistra.setToolTipText("");
        }
        if (!Movimento[11].isBlank()){
            LabelLifoME.setText("<html>L.i.F.o. Moneta Entrata : <b><span style=\"font-size: 18px;\">"+Movimento[13]+" "+Movimento[11]+"</span></b></html>");
            this.Bottone_ME_FrecciaDestra.setEnabled(true);
            this.Bottone_ME_FrecciaDestra.setToolTipText("<html>Vedi stack relativo al movimento successivo di : <b>"+Movimento[11]+"</b></html>");
            this.Bottone_ME_FrecciaSinistra.setEnabled(true);
            this.Bottone_ME_FrecciaSinistra.setToolTipText("<html>Vedi stack relativo al movimento precedente di : <b>"+Movimento[11]+"</b></html>");           
        }else
        {
            LabelLifoME.setText("<html>L.i.F.o. Moneta Entrata</html>");
            this.Bottone_ME_FrecciaDestra.setEnabled(false);
            this.Bottone_ME_FrecciaDestra.setToolTipText("");
            this.Bottone_ME_FrecciaSinistra.setEnabled(false);
            this.Bottone_ME_FrecciaSinistra.setToolTipText("");
        }
        
        
        if (Funzioni.MovimentoRilevante(Movimento)) {
            //Movimento Rilevante
            String text = "<html>"
                    + "<div style='font-family: sans-serif; font-size: 11px;'>"
                    + "<b>Il movimento è <span style='color: #FF6600;'>RILEVANTE</span></b>, e significa che:"
                    + "<br>"
                    + "<ul style='margin-left: 15px;'>"
                    + "<li>Se esistono movimenti evidenziati in <b style='color:red;'>rosso</b> nella parte alta, "
                    + "questi genereranno una <b>plusvalenza</b> e verranno rimossi dallo stack <i>LIFO</i> relativo al token in uscita.</li>"
                    + "<li>Se esiste un movimento evidenziato in <b style='color:green;'>verde</b> nella parte bassa, "
                    + "indicherà il nuovo <b>costo di carico</b> per il token in entrata, pari al valore della transazione. "
                    + "Questo verrà aggiunto allo stack <i>LIFO</i> del token entrante.<br>"
                    + "<b>Nota:</b> in caso di <b>EARN</b> o altre forme di <i><b>rendita da detenzione</b></i>, "
                    + "verrà generata una <b>plusvalenza</b> pari al valore della transazione.<br>"
                    + "<b>Nota 2:</b> Nel caso di altri proventi e a seconda delle opzioni scelte potrebbe essere che il costo di carico sia pari a Zero"
                    + "</li>"
                    + "</ul>"
                    + "</div>"
                    + "</html>";
            Label_Informazioni.setText(text);

        }else{
            //Movimento non Rilevante
            String text = "<html>"
                    + "<div style='font-family: sans-serif; font-size: 11px;'>"
                    + "<b>Il movimento è <span style='color: #FF6600;'> NON RILEVANTE</span></b>, e significa che:"
                    + "<br>"
                    + "<ul style='margin-left: 15px;'>"
                    + "<li>Se esistono movimenti evidenziati in <b style='color:red;'>rosso</b> nella parte alta, "
                    + "questi verranno rimosso dallo stack <i>LIFO</i> relativo al token in uscita.</li>"
                    + "<li>Se esiste un movimento evidenziato in <b style='color:green;'>verde</b> nella parte bassa, "
                    + "indicherà il nuovo <b>costo di carico</b> per il token in entrata, pari al valore della somma dei movimenti rimossi al primo punto. "
                    + "Questo verrà aggiunto allo stack <i>LIFO</i> del token entrante.<br>"
                    + "</li>"
                    + "</ul>"
                    + "</div>"
                    + "</html>";
            Label_Informazioni.setText(text);
        }
        //Compilo le tabelle
        Tabelle.Funzioni_PulisciTabella((DefaultTableModel)Tabella_Lifo_Entrata.getModel());
        Tabelle.Funzioni_PulisciTabella((DefaultTableModel)Tabella_Lifo_Uscita.getModel());
        Tabelle.Funzioni_PulisciTabella((DefaultTableModel)jTable1.getModel());
        String rigaTab[]=new String[]{Movimento[0],Movimento[1],Movimento[5],Movimento[6],Movimento[8],Movimento[10],Movimento[11],Movimento[13],Movimento[15],Movimento[19]};
        DefaultTableModel ModelloTabella=(DefaultTableModel)jTable1.getModel();
        ModelloTabella.addRow(rigaTab);
        
        Calcoli_PlusvalenzeNew.LifoXID lifoID=Calcoli_PlusvalenzeNew.getIDLiFo(IDtr);
        if (lifoID!=null){
        ArrayDeque<String[]> StackEntrato=lifoID.Get_CryptoStackEntrato();
        //Stack della moneta entrata prima el movimento
        ArrayDeque<String[]> StackEntratoPreMovimento=lifoID.Get_CryptoStackEntratoPreMovimento();
        //Parte dello stack della moneta uscita relativo alla transazione
        ArrayDeque<String[]> StackUscito=lifoID.Get_CryptoStackUscito();
        //Rimanenze dello stack della moneta uscita dopo la transazione
        ArrayDeque<String[]> StackUscitoRimanenze=lifoID.Get_CryptoStackUscitoRimanenze();       
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
            String mov[]=Principale.MappaCryptoWallet.get(ultimoRecupero[3]);
            Object[] riga = new Object[9];
            riga[0]=i;
            riga[1]=ultimoRecupero[3];
            if (mov!=null){
                riga[2]=mov[1];
                riga[3]=mov[6];
            }else{
                riga[2]="Giacenza negativa";
                riga[3]="Manca movimento in ingresso";
            }
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
    
    private void CaricaMovimentoSuccessivo(String IDtr, String Moneta) {
        String IDCiclo = IDtr;
        Map.Entry<String, String[]> suc;
        while (true) {
            suc = Principale.MappaCryptoWallet.higherEntry(IDCiclo);
            if (suc != null) {
                IDCiclo = suc.getKey();
                if (suc.getValue()[8].equals(Moneta) || suc.getValue()[11].equals(Moneta)) {
                    if (Calcoli_PlusvalenzeNew.getIDLiFo(IDCiclo)!=null){
                        InizializzaOggetti(IDCiclo);
                        break;
                    }
                }
            } else {
                break;
            }

        }
    }
    private void CaricaMovimentoPrecedente(String IDtr, String Moneta){
            String IDCiclo = IDtr;
        Map.Entry<String, String[]> suc;
        while (true) {
            suc = Principale.MappaCryptoWallet.lowerEntry(IDCiclo);
            if (suc != null) {
                IDCiclo = suc.getKey();
                if (suc.getValue()[8].equals(Moneta) || suc.getValue()[11].equals(Moneta)) {
                    if (Calcoli_PlusvalenzeNew.getIDLiFo(IDCiclo)!=null){
                        InizializzaOggetti(IDCiclo);
                        break;
                    }
                }
            } else {
                break;
            }

        }
    
    }
    
    
    private DefaultTableModel inizializzaTabella(JTable table){
        DefaultTableModel ModelloTabella = (DefaultTableModel) table.getModel();
        Tabelle.Funzioni_PulisciTabella(ModelloTabella);
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
        Bottone_MU_FrecciaSinistra = new javax.swing.JButton();
        Bottone_MU_FrecciaDestra = new javax.swing.JButton();
        Bottone_ME_FrecciaDestra = new javax.swing.JButton();
        Bottone_ME_FrecciaSinistra = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        LabelMovimento = new javax.swing.JLabel();
        Label_Informazioni = new javax.swing.JLabel();

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

        Bottone_MU_FrecciaSinistra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N
        Bottone_MU_FrecciaSinistra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MU_FrecciaSinistraActionPerformed(evt);
            }
        });

        Bottone_MU_FrecciaDestra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N
        Bottone_MU_FrecciaDestra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MU_FrecciaDestraActionPerformed(evt);
            }
        });

        Bottone_ME_FrecciaDestra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N
        Bottone_ME_FrecciaDestra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ME_FrecciaDestraActionPerformed(evt);
            }
        });

        Bottone_ME_FrecciaSinistra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N
        Bottone_ME_FrecciaSinistra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ME_FrecciaSinistraActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Data", "Tipo Movimento", "Title 4", "Moneta Uscita", "Qta Uscita", "Moneta Entrata", "Qta Entrata", "Valore Transazione", "Plusvalenza"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(0);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(0);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(0);
        }

        LabelMovimento.setText("Movimento di riferimento :");

        Label_Informazioni.setText("Testo Informativo");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Label_Informazioni, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane4)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1012, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(LabelLifoME, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(Bottone_ME_FrecciaSinistra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_ME_FrecciaDestra))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(LabelLifoMU, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(Bottone_MU_FrecciaSinistra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_MU_FrecciaDestra))
                    .addComponent(LabelMovimento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(LabelMovimento, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Label_Informazioni, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(Bottone_MU_FrecciaDestra, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Bottone_MU_FrecciaSinistra, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LabelLifoMU, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Bottone_ME_FrecciaDestra, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Bottone_ME_FrecciaSinistra, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LabelLifoME, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_MU_FrecciaDestraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MU_FrecciaDestraActionPerformed
        // TODO add your handling code here:
        String MU=Principale.MappaCryptoWallet.get(ID)[8];
        CaricaMovimentoSuccessivo(ID, MU);
    }//GEN-LAST:event_Bottone_MU_FrecciaDestraActionPerformed

    private void Bottone_ME_FrecciaDestraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ME_FrecciaDestraActionPerformed
        // TODO add your handling code here:
        String ME=Principale.MappaCryptoWallet.get(ID)[11];
        CaricaMovimentoSuccessivo(ID, ME);
    }//GEN-LAST:event_Bottone_ME_FrecciaDestraActionPerformed

    private void Bottone_ME_FrecciaSinistraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ME_FrecciaSinistraActionPerformed
        // TODO add your handling code here:
        String ME=Principale.MappaCryptoWallet.get(ID)[11];
        CaricaMovimentoPrecedente(ID, ME);
    }//GEN-LAST:event_Bottone_ME_FrecciaSinistraActionPerformed

    private void Bottone_MU_FrecciaSinistraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MU_FrecciaSinistraActionPerformed
        // TODO add your handling code here:
        String MU=Principale.MappaCryptoWallet.get(ID)[8];
        CaricaMovimentoPrecedente(ID, MU);
    }//GEN-LAST:event_Bottone_MU_FrecciaSinistraActionPerformed

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
    private javax.swing.JLabel LabelMovimento;
    private javax.swing.JLabel Label_Informazioni;
    private javax.swing.JTable Tabella_Lifo_Entrata;
    private javax.swing.JTable Tabella_Lifo_Uscita;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
