/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
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
        this.ID=ID;
                
        
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
                String VSplit[]=Movimento[40].split("\\|",-1); 
                if(VSplit[0].equalsIgnoreCase(Movimento[8])){
                    PrezzoAttualeU[3]=VSplit[3];
                    if (PrezzoAttualeU[3].toLowerCase().contains("db interno"))
                    {
                        PrezzoAttualeU[4]=OperazioniSuDate.ConvertiDatadaLongallOra(data)+":XX:XX";
                        PrezzoAttualeU[5]="1 ora";
                    }
                    if (!VSplit[1].isBlank()){
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
                    }
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
                String VSplit[]=Movimento[40].split("\\|",-1); 
                if(VSplit[0].equalsIgnoreCase(Movimento[11])){
                    PrezzoAttualeE[3]=VSplit[3];
                    if (PrezzoAttualeE[3].toLowerCase().contains("db interno"))
                        {
                        PrezzoAttualeE[4]=OperazioniSuDate.ConvertiDatadaLongallOra(data)+":XX:XX";
                        PrezzoAttualeE[5]="1 ora";
                    }
                    if (!VSplit[1].isBlank()){
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
                    }
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
            Prezzi.CambioXXXEUR(M[i].Moneta, M[i].Qta, data,M[i].MonetaAddress,M[i].Rete,"",false);
            
            
            //A - Vedo se è una fiat e in quel caso la tratto come tale
            if (M[i].Tipo.equalsIgnoreCase("FIAT")) {
                Prezzi.InfoPrezzo IPF;
                String DataDollaro = OperazioniSuDate.ConvertiDatadaLong(data);
                if (M[i].Moneta.equalsIgnoreCase("EUR")) {
                    String rigo[] = new String[9];
                    rigo[0] = M[i].Moneta;
                    rigo[1] = ora;
                    rigo[2] = M[i].Qta;
                    rigo[3] = "";
                    rigo[4] = OperazioniSuDate.ConvertiDatadaLongAlSecondo(data);
                    rigo[5] = "0 sec";
                    rigo[6] = "1";
                    rigo[8] = new BigDecimal(M[i].Qta).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                    rigo[7] = new BigDecimal(M[i].Qta).abs().subtract(new BigDecimal(Movimento[15])).toPlainString();
                    ModTabPrezzi.addRow(rigo);
                } 
                else if (M[i].Moneta.equalsIgnoreCase("USD")) {
                    String PT = Prezzi.ConvertiUSDEUR("1", DataDollaro);
                    if (PT != null) {
                        BigDecimal PrezzoTransazione = new BigDecimal(PT).abs().stripTrailingZeros();
                        IPF = new Prezzi.InfoPrezzo();
                        IPF.Moneta = M[i].Moneta;
                        IPF.Qta = new BigDecimal(M[i].Qta);
                        IPF.exchange = "bancaditalia";
                        IPF.prezzo = PrezzoTransazione;
                        IPF.timestamp = data;
                        IPF.prezzoQta=IPF.prezzo.multiply(IPF.Qta);
                        String rigo[] = new String[9];
                        rigo[0] = M[i].Moneta;
                        rigo[1] = ora;
                        rigo[2] = M[i].Qta;
                        rigo[3] = IPF.exchange;
                        rigo[4] = OperazioniSuDate.ConvertiDatadaLong(IPF.timestamp);
                        rigo[5] = "1 giorno";
                        rigo[6] = IPF.prezzo.toPlainString();
                        rigo[8] = IPF.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                        rigo[7] = IPF.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
                        ModTabPrezzi.addRow(rigo);
                    }
                }
                //Non vado a cercare le FIAT oltre, mi fermo qua dando solo questa possibilità
                continue;
            }
            
            
            if (!M[i].MonetaAddress.isBlank() && !M[i].Rete.isBlank()) {
                    List<Prezzi.InfoPrezzo> ListaIP = Prezzi.DammiListaPrezziDaDatabase("", data, M[i].Rete, M[i].MonetaAddress, 60, new BigDecimal(M[i].Qta));
                    for (Prezzi.InfoPrezzo IPl : ListaIP) {
                        String rigo[] = new String[9];
                        rigo[0] = M[i].Moneta;
                        rigo[1] = ora;
                        rigo[2] = M[i].Qta;
                        rigo[3] = IPl.exchange;
                        rigo[4] = OperazioniSuDate.ConvertiDatadaLongAlSecondo(IPl.timestamp);
                        //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                        long DiffOrario=Math.abs(data-IPl.timestamp)/1000;
                        String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }
                        rigo[5] = String.valueOf(DiffOrario)+unitaTempo;
                        rigo[6] = IPl.prezzo.toPlainString();
                        rigo[8] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                        rigo[7] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
                        ModTabPrezzi.addRow(rigo);
                    }
                }
            //Prezzi.DammiPrezzoInfoTransazione(M1, null, data, Rete,"");
             List<Prezzi.InfoPrezzo> ListaIP=Prezzi.DammiListaPrezziDaDatabase(M[i].Moneta,data,"","",60,new BigDecimal(M[i].Qta));
            //Riempio la tabella con i prezzi
            for(Prezzi.InfoPrezzo IPl:ListaIP){
                String rigo[]=new String[9];
                    rigo[0] = M[i].Moneta;
                    rigo[1] = ora;
                    rigo[2] = M[i].Qta;                  
                    rigo[3] = IPl.exchange;
                    rigo[4] = OperazioniSuDate.ConvertiDatadaLongAlSecondo(IPl.timestamp);
                    //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                    long DiffOrario=Math.abs(data-IPl.timestamp)/1000;
                    String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }                       
                    rigo[5] = String.valueOf(DiffOrario)+unitaTempo;
                    
                    rigo[6] = IPl.prezzo.toPlainString();
                    rigo[8] = IPl.prezzoQta.setScale(2,RoundingMode.HALF_UP).toPlainString();
                    rigo[7] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
                    ModTabPrezzi.addRow(rigo);                
            }
            
            //Adesso cerco i prezzi nel vecchio database
            String DataOra = OperazioniSuDate.ConvertiDatadaLongallOra(data);
            String PrezzoUnitario = DatabaseH2.XXXEUR_Leggi(DataOra + " " + M[i].Moneta);
            Prezzi.InfoPrezzo IP = new Prezzi.InfoPrezzo();
            System.out.println(PrezzoUnitario);
            if (PrezzoUnitario != null) {
                IP.exchange = "DB Interno (Old)";
                IP.timestamp = OperazioniSuDate.ConvertiDatainLongMinuto(DataOra + ":00");
                IP.prezzo = new BigDecimal(PrezzoUnitario);
                IP.Qta = new BigDecimal(M[i].Qta);
                IP.Moneta = M[i].Moneta;
                String rigo[] = new String[9];
                rigo[0] = M[i].Moneta;
                rigo[1] = ora;
                rigo[2] = M[i].Qta;
                rigo[3] = IP.exchange;
                rigo[4] = DataOra + ":XX:XX";
                rigo[5] = "1 ora";

                rigo[6] = IP.prezzo.toPlainString();
                if (IP.prezzoQta==null)IP.prezzoQta=IP.prezzo.multiply(IP.Qta).abs();
                rigo[8] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                rigo[7] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(Movimento[15])).toPlainString();
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
        Tabella_PrezzoAttuale.setEnabled(false);
        jScrollPane1.setViewportView(Tabella_PrezzoAttuale);

        Bottone_OK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Prezzo.png"))); // NOI18N
        Bottone_OK.setText("Applica prezzo selezionato");
        Bottone_OK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_OKActionPerformed(evt);
            }
        });

        Bottone_Annulla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_Annulla.setText("Annulla");
        Bottone_Annulla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AnnullaActionPerformed(evt);
            }
        });

        Tabella_Prezzi.setAutoCreateRowSorter(true);
        Tabella_Prezzi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Moneta", "Orario", "Qta", "Fonte", "Orario Fonte", "Precisione", "Prezzo Unitario", "Diff.Prezzo", "Prezzo Totale"
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
            Tabella_Prezzi.getColumnModel().getColumn(8).setMinWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(8).setPreferredWidth(100);
            Tabella_Prezzi.getColumnModel().getColumn(8).setMaxWidth(100);
        }

        jLabel1.setText("Prezzo Attuale");

        jLabel2.setText("Prezzi Disponibili piu' vicini");

        jLabel3.setText("Fai click sulla riga corrispndente al prezzo voluto e conferma con il tasto \"Applica prezzo selezionato\" per cambiare il prezzo alla transazione.");

        jLabel4.setText("In alternativa premere sul tasto \"Prezzo Personalizzato\" per inserire un prezzo personalizzato per il token/transazione.");

        Bottone_Personalizzato.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        Bottone_Personalizzato.setText("Prezzo Personalizzato");
        Bottone_Personalizzato.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_PersonalizzatoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_OK, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(Bottone_Personalizzato, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Bottone_Annulla)
                            .addComponent(Bottone_Personalizzato)))
                    .addComponent(Bottone_OK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_OKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_OKActionPerformed
        // TODO add your handling code here:
          if (Tabella_Prezzi.getSelectedRow() >= 0) {
            int rigaselezionata = Tabella_Prezzi.getRowSorter().convertRowIndexToModel(Tabella_Prezzi.getSelectedRow());
            
            String Fonte=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 3).toString();
            String Prezzo=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 8).toString();
            String PrezzoU=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 6).toString();
            String TimeStamp=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 4).toString();
            String Token=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 0).toString();
            String PrezzoPrecedente=Tabella_PrezzoAttuale.getModel().getValueAt(0, 7).toString();
            long timestamp=OperazioniSuDate.ConvertiDatainLongSecondo(TimeStamp);
            if (timestamp!=0){
                TimeStamp=String.valueOf(timestamp);
            }else TimeStamp="";
            /*String domanda="<html>Sicuro di voler applicare il prezzo di <b>€"+Prezzo+"</b> alla transazione?<br>";
            if (!Fonte.isBlank()){
                domanda=domanda+"Il prezzo è preso dalla seguente fonte : <b>"+Fonte+"</b><br>";
            }
            domanda=domanda+"<br>Il prezzo attualmente memorizzato e' di <b>€"+PrezzoPrecedente+"</b><br></html>";*/
              String domanda = """
<html>
  <body style='font-family: Segoe UI, sans-serif; font-size: 13pt;'>
    <div style='text-align: left;'>
      <p>Vuoi applicare il prezzo di <b>€%s</b> a questa transazione?</p>
      <div style='text-align: center;'>                         
        %s
      </div>                         
      <p><br>Il prezzo attualmente memorizzato è di <b>€%s</b></p>
    </div>
  </body>
</html>
""".formatted(Prezzo,
                      Fonte.isBlank() ? "" : "<p>(Prezzo ottenuto da: <b>" + Fonte + "</b>)</p>",
                      PrezzoPrecedente);
            int risposta=JOptionPane.showOptionDialog(this,domanda, "Cambio Prezzo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
            if (risposta==0){
            
                CDC_Grafica.MappaCryptoWallet.get(ID)[40]=Token+"|"+TimeStamp+"|"+PrezzoU+"|"+Fonte;
                CDC_Grafica.MappaCryptoWallet.get(ID)[15]=Prezzo;
                CDC_Grafica.TabellaCryptodaAggiornare=true;
                this.dispose();
            }
          }
    }//GEN-LAST:event_Bottone_OKActionPerformed

    private void Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AnnullaActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_Bottone_AnnullaActionPerformed

    private void Bottone_PersonalizzatoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_PersonalizzatoActionPerformed
        // TODO add your handling code here:
       // System.out.println(ID);
        if(Funzioni.GUIModificaPrezzo(this,ID))
        {
            CDC_Grafica.MappaCryptoWallet.get(ID)[40]="|||Manuale";
            CDC_Grafica.TabellaCryptodaAggiornare=true;
            this.dispose();
        }
    }//GEN-LAST:event_Bottone_PersonalizzatoActionPerformed

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
