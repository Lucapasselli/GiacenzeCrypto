/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author luca.passelli
 */
public class GUI_DettaglioTransazione extends javax.swing.JDialog {
//private static final long serialVersionUID = 8L;

    /**
     * Creates new form Importazioni_Resoconto
     * @param IDTransazione
     */
    
     private static final Map<Integer,String> mappa_ID=new TreeMap<>(); 
     private static int Riferimento=0;


        public void TransazioniCrypto_CompilaTextPaneDatiMovimento(String IDTransazione) {
            SerializzaIDMovimenti(IDTransazione);
            this.setTitle("Dettaglio Movimento");
            
            

            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) Tabella.getModel();
            Tabelle.Funzioni_PulisciTabella(ModelloTabellaCrypto);

        
        //come prima cosa mi occupo del pulsante defi, deve essere attivo se abbiamo movimenti in defi e disattivo in caso contrario 
        //per controllare verifico di avere il transaction hash e il nome della rete quindi
      //  System.out.println(IDTransazione);
      //  System.out.println(CDC_Grafica.MappaCryptoWallet.size());
        String Transazione[]=Principale.MappaCryptoWallet.get(IDTransazione);
        
        //System.out.println(IDTransazione);
        //System.out.println(CDC_Grafica.MappaCryptoWallet.get(IDTransazione));
        //String Titolo="<html><p align=\"center\">"+"Pippo<br>"+"Pluto</html>";
       // String secondi=Transazione[0].split("_")[0].substring(12);
       // Funzioni.getOradaID(Transazione[0]);
        String Titolo="<html>"+Funzioni.getOradaID(Transazione[0])+"<br>"+Transazione[5]+"</html>";
        TextPane_Titolo.setText(Titolo);
        //Accenstro il testo
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        StyledDocument doc = TextPane_Titolo.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
       // TextArea_Titolo.setText(Transazione[1]+"\n"+Transazione[5]);
        String ReteDefi=Funzioni.TrovaReteDaID(IDTransazione);
        //System.out.println("retedefi:"+ReteDefi);
        String THash=Transazione[24];
        //System.out.println("hash:"+THash);
            if(!THash.isEmpty()&&ReteDefi!=null){
                Bottone_DeFi.setEnabled(true);
            }else{
                Bottone_DeFi.setEnabled(false);
            }
        
        String Valore;
        String Val[];
        
        //Parte per la verifica dei prezzi dalle fonti e il recupero dei prezzi unitari precisi
            BigDecimal PrzTotaleNonArrotondato = null;
            if (!Transazione[40].isBlank()) {
                String VSplit[] = Transazione[40].split("\\|");
                String MonRif = VSplit[0];
                String PrzUnitarioMonRif = VSplit[2];

                if (!Transazione[8].isBlank() && Transazione[8].equalsIgnoreCase(MonRif)) {
                    PrzTotaleNonArrotondato = new BigDecimal(PrzUnitarioMonRif).multiply(new BigDecimal(Transazione[10]));
                }
                if (!Transazione[11].isBlank() && Transazione[11].equalsIgnoreCase(MonRif)) {
                    PrzTotaleNonArrotondato = new BigDecimal(PrzUnitarioMonRif).multiply(new BigDecimal(Transazione[13]));
                }
            }
        
        Valore=Transazione[1];
        if (!Valore.isBlank()){
            Valore="<html><b>"+Funzioni.getOradaID(Transazione[0])+"</html>";
            Val=new String[]{"Data e Ora ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[3];
        if (!Valore.isBlank()){
            Val=new String[]{"Exchange/Wallet ",Valore+" ("+Transazione[4]+")"};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Funzioni.TrovaReteDaID(Transazione[0]);
        if (Valore!=null&&!Valore.isBlank()){
            Val=new String[]{"Rete ",Valore};
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
                            String Mov[] = Principale.MappaCryptoWallet.get(IdM);
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
            String Testo="<html><p style=\"color:"+Tabelle.Rosso+";\"><b>"+Transazione[10]+ " " + Transazione[8].split("\\(")[0];
            if (!Transazione[25].isBlank()&&!Transazione[8].equalsIgnoreCase(Transazione[25])){
                Testo=Testo+" </b></p>("+Transazione[25]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Uscita: ",Testo};
            
            ModelloTabellaCrypto.addRow(Val);
            //Adesso Aggiungo anche il Valore Unitario in euro della moneta in ingresso e quella in uscita
            if (!Transazione[15].isBlank()){
                BigDecimal ValUnitario=new BigDecimal(0);
                if (new BigDecimal(Transazione[10]).compareTo(BigDecimal.ZERO)!=0)
                {
                    ValUnitario=new BigDecimal(Transazione[15]).divide(new BigDecimal(Transazione[10]),10, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                }
                Valore="<html>€ "+ValUnitario.toPlainString()+"</html>";
                Val=new String[]{"Valore Unitario "+Transazione[8]+" (Calcolato)",Valore};
                if (PrzTotaleNonArrotondato!=null) {
                        ValUnitario = PrzTotaleNonArrotondato.divide(new BigDecimal(Transazione[10]), 20, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                        Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                        Val = new String[]{"Valore Unitario " + Transazione[8], Valore};
                    }
                ModelloTabellaCrypto.addRow(Val);
            }
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
            String Testo="<html><p style=\"color:"+Tabelle.Verde+";\"><b>"+Transazione[13]+ " " + Transazione[11].split("\\(")[0];
            if (!Transazione[27].isBlank()&&!Transazione[27].equalsIgnoreCase(Transazione[11])){
                Testo=Testo+" </b></p>("+Transazione[27]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Entrata: ",Testo};
            ModelloTabellaCrypto.addRow(Val);
            
            if (!Transazione[15].isBlank()) {
                if (new BigDecimal(Transazione[13]).compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ValUnitario;
                Val=null;
                if (new BigDecimal(Transazione[15]).compareTo(BigDecimal.ZERO) != 0) {
                    ValUnitario = new BigDecimal(Transazione[15]).divide(new BigDecimal(Transazione[13]), 10, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                    Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                    Val = new String[]{"Valore Unitario " + Transazione[11] + " (Calcolato)", Valore};

                }
                if (PrzTotaleNonArrotondato != null) {
                    ValUnitario = PrzTotaleNonArrotondato.divide(new BigDecimal(Transazione[13]), 20, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                    Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                    Val = new String[]{"Valore Unitario " + Transazione[11], Valore};
                }
                if(Val!=null)ModelloTabellaCrypto.addRow(Val);
                }
            }
        }
        Valore=Transazione[12];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Tipologia",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
       /* Valore=Transazione[27];
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
            Valore="<html>€ "+Valore+"</html>";
            Val=new String[]{"Valore transazione ",Valore};
            ModelloTabellaCrypto.addRow(Val);      
        }
        Valore=Transazione[14];
        if (!Valore.isBlank()){
            Valore="<html>"+Valore+"</html>";
            Val=new String[]{"Valore transazione da CSV",Valore};
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
        
        String Valori[]=Transazione[20].split(",");
         for (String Valori1 : Valori) {
             Valore = Valori1;
             if (!Valore.isBlank()){
                 Valore=("<html>"+Valore+"</html>");
                 Val=new String[]{"Movimenti Correlati ",Valore};
                 ModelloTabellaCrypto.addRow(Val);
             }
         }
        Valore=Transazione[0];
        if (!Valore.isBlank()){
            Val=new String[]{"ID ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
                Valore=Transazione[40];
        if (!Valore.isBlank()){
            String VSplit[]=Valore.split("\\|");        
            Val=new String[]{"Info Prezzo : Fonte ",VSplit[3]};
            ModelloTabellaCrypto.addRow(Val);
            if (Funzioni.isNumeric(VSplit[1], false)){
                Val=new String[]{"Info Prezzo : Orario Fonte ",FunzioniDate.ConvertiDatadaLongAlSecondo(Long.parseLong(VSplit[1]))};
                ModelloTabellaCrypto.addRow(Val); 
            }                    
            if (VSplit[0]!=null&&!VSplit[0].isBlank()){
                Val=new String[]{"Info Prezzo : Moneta di riferimento transazione",VSplit[0]};
                ModelloTabellaCrypto.addRow(Val);
            }
            if (VSplit[2]!=null&&!VSplit[2].isBlank()){
                Val=new String[]{"Info Prezzo : Prezzo unitario ","€ "+VSplit[2]};
                ModelloTabellaCrypto.addRow(Val);
            }
        }
        
        
        Tabelle.ColoraTabellaSemplice(Tabella);
        Tabelle.updateRowHeights(Tabella);

    }
    
    public GUI_DettaglioTransazione() {
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

        ScrollTabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_DeFi = new javax.swing.JButton();
        Bottone_MovPrecedente = new javax.swing.JButton();
        Bottone_MovSuccessivo = new javax.swing.JButton();
        TextPane_Titolo = new javax.swing.JTextPane();
        Bottone_Modifica = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

        Bottone_DeFi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Catena.png"))); // NOI18N
        Bottone_DeFi.setText("Dettaglio DeFi");
        Bottone_DeFi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_DeFiActionPerformed(evt);
            }
        });

        Bottone_MovPrecedente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N
        Bottone_MovPrecedente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MovPrecedenteActionPerformed(evt);
            }
        });

        Bottone_MovSuccessivo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N
        Bottone_MovSuccessivo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MovSuccessivoActionPerformed(evt);
            }
        });

        TextPane_Titolo.setEditable(false);
        TextPane_Titolo.setContentType("text/html"); // NOI18N
        TextPane_Titolo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

        Bottone_Modifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        Bottone_Modifica.setText("Modifica Movimento");
        Bottone_Modifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ModificaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ScrollTabella)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_MovPrecedente)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 226, Short.MAX_VALUE)
                        .addComponent(TextPane_Titolo, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(237, 237, 237)
                        .addComponent(Bottone_MovSuccessivo))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Modifica, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_DeFi, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TextPane_Titolo, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(Bottone_MovPrecedente)
                        .addComponent(Bottone_MovSuccessivo)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollTabella, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_DeFi)
                    .addComponent(Bottone_Modifica))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void AzzeraMap(){
        mappa_ID.clear();
    }
    
    private void SerializzaIDMovimenti(String IDTransazione){
        int i=0;
        //Metto in questa mappa tutti i movimenti ordinati per ID
        //Poi salvo anche il numero del movimento visualizzato
        if (mappa_ID.isEmpty())
            for (String[] movimento: Principale.MappaCryptoWallet.values()){
                mappa_ID.put(i, movimento[0]);
                if (movimento[0].equals(IDTransazione))Riferimento=i;
                i++;
            }
    }
    
    private void Bottone_MovSuccessivoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MovSuccessivoActionPerformed
        // TODO add your handling code here:
        if (mappa_ID.get(Riferimento+1)!=null){
            Riferimento=Riferimento+1;
            String IDTransazione=mappa_ID.get(Riferimento);
            TransazioniCrypto_CompilaTextPaneDatiMovimento(IDTransazione);
        }
    }//GEN-LAST:event_Bottone_MovSuccessivoActionPerformed

    private void Bottone_MovPrecedenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MovPrecedenteActionPerformed
        // TODO add your handling code here:
        if (mappa_ID.get(Riferimento-1)!=null){
            Riferimento=Riferimento-1;
            String IDTransazione=mappa_ID.get(Riferimento);
            TransazioniCrypto_CompilaTextPaneDatiMovimento(IDTransazione);
        }
    }//GEN-LAST:event_Bottone_MovPrecedenteActionPerformed

    private void Bottone_DeFiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_DeFiActionPerformed
        // TODO add your handling code here:
        Funzioni_WalletDeFi.ApriExplorer(mappa_ID.get(Riferimento));
    }//GEN-LAST:event_Bottone_DeFiActionPerformed

    private void Bottone_ModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ModificaActionPerformed
        // TODO add your handling code here:
        Funzione_ModificaMovimento(mappa_ID.get(Riferimento),this);
    }//GEN-LAST:event_Bottone_ModificaActionPerformed

      public void Funzione_ModificaMovimento(String ID,Component c){
            GUI_ModificaMovimento a = new GUI_ModificaMovimento();
            String riga[]=Principale.MappaCryptoWallet.get(ID);

            String PartiCoinvolte[] = (riga[0] + "," + riga[20]).split(",");
            if (PartiCoinvolte.length > 1 && !riga[22].equalsIgnoreCase("AU")) {//devo permettere di modificare i movimenti automatici generati dagli scambi per poter cambiare eventualmente il prezzo
                String Messaggio = "Attenzione, il movimento è associato ad un altro movimento.\n"
                        + "se si prosegue l'associazione verrà rimossa, si vuole continuare?";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Conferma modifica", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        ID=ClassificazioneTrasf_Modifica.RiportaTransazioniASituazioneIniziale(PartiCoinvolte,ID); 

                        //String id=mappa_ID.get(Riferimento);
                Point p = this.getLocation();
                this.dispose();
                a.CompilaCampidaID(ID);
                a.setLocation(p);               
                a.setVisible(true);
                if (!a.IDNuovo.isEmpty())ID=a.IDNuovo;
                
                //Quando finisco la modifica apro di nuovo la maschera con il movimento
                
               // CDC_Grafica.
                GUI_DettaglioTransazione t =new GUI_DettaglioTransazione();
                t.AzzeraMap();
                t.TransazioniCrypto_CompilaTextPaneDatiMovimento(ID);
                t.setLocation(p);
                t.setVisible(true);
                        
                    }
                    case 1 -> {
                        JOptionPane.showConfirmDialog(this, "Operazione Annullata",
                                "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                        
                    }
                    case -1 -> {
                        JOptionPane.showConfirmDialog(this, "Operazione Annullata",
                                "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                        
                    }

                }
            }else{ 
                //String id=mappa_ID.get(Riferimento);
                Point p = this.getLocation();
                this.dispose();
                a.CompilaCampidaID(ID);
                a.setLocation(p);               
                a.setVisible(true);
                if (!a.IDNuovo.isEmpty())ID=a.IDNuovo;
                
                //Quando finisco la modifica apro di nuovo la maschera con il movimento
                
               // CDC_Grafica.
                GUI_DettaglioTransazione t =new GUI_DettaglioTransazione();
                t.AzzeraMap();
                t.TransazioniCrypto_CompilaTextPaneDatiMovimento(ID);
                t.setLocation(p);
                t.setVisible(true);
                
                
                
               // this.dispose();

            }
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
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI_DettaglioTransazione().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_DeFi;
    private javax.swing.JButton Bottone_Modifica;
    private javax.swing.JButton Bottone_MovPrecedente;
    private javax.swing.JButton Bottone_MovSuccessivo;
    private javax.swing.JScrollPane ScrollTabella;
    private javax.swing.JTable Tabella;
    private javax.swing.JTextPane TextPane_Titolo;
    // End of variables declaration//GEN-END:variables
}
