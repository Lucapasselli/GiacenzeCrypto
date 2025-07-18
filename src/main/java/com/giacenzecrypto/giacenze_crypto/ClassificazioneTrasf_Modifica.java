/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.DecimaliCalcoli;
import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Cursor;
import java.awt.event.ItemEvent;
import java.math.RoundingMode;

/**
 *
 * @author luca.passelli
 */
public class ClassificazioneTrasf_Modifica extends javax.swing.JDialog {
    private static final long serialVersionUID = 1L;

    /*
     * Creates new form ClassificazioneTrasf_Modifica
     */
    //CLASSIFICAZIONE TRASFERIMENTI
    
    //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
    //PCO -> Cashout o similare (prelievo)
    //PTW -> Trasferimento tra Wallet (prelievo)
    //PSC -> Scambio Crypto Differito (Scambio crypto non simultaneo ma differito nel tempo) (Non Utilizzato per ora)
    //DTW -> Trasferimento tra Wallet (deposito)
    //DAI -> Airdrop o similare (deposito)
    //DCZ -> Costo di carico 0 (deposito)
    //DAC -> Acquisto Crypto (deposito)  
   //////////// //DSC -> Scambio Crypto Differito (Scambio crypto non simultaneo ma differito nel tempo) (Non Utilizzato per ora)

    
    static String IDTrans="";
    boolean ModificaEffettuata=false;
    @SuppressWarnings("unchecked")
    public ClassificazioneTrasf_Modifica(String ID) {
        ModificaEffettuata=false;
        IDTrans=ID;
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.jTable1.getModel();
        Tabelle.Funzioni_PulisciTabella(ModelloTabellaDepositiPrelievi);
        Tabelle.ColoraRigheTabellaCrypto(jTable1);
        String riga[]=DammiRigaTabellaDaID(ID);
        ModelloTabellaDepositiPrelievi.addRow(riga);
        //System.out.println(riga[6]);
        String tipomov=riga[6].split("-")[0].trim();
        int ntipo=0;//e' il numero di quello che deve essere evidenziato nella combobox
        if (tipomov.equalsIgnoreCase("PWN")) {
            ntipo = 2;
            TransferNO();
        } else if (tipomov.equalsIgnoreCase("PCO")) {
            ntipo = 1;
            TransferNO();
        } else if (tipomov.equalsIgnoreCase("PTW")) {
            ntipo = 3;
            if (riga[6].contains("Scambio"))ntipo=4;
            if (riga[6].contains("Rendita"))ntipo=5;//Prelievo per Piattoforma a rendita
            TransferSI();
        } else if (tipomov.equalsIgnoreCase("DTW")) {
            ntipo = 3;
            if (riga[6].contains("Scambio"))ntipo=5;//Deposito per Scambio Differito
            if (riga[6].contains("Rendita"))ntipo=6;//Deposito da Piattoforma a rendita
            TransferSI();
        } else if (tipomov.equalsIgnoreCase("DAI")) {
            ntipo = 1;
            TransferNO();
        } else if (tipomov.equalsIgnoreCase("DCZ")) {
            ntipo = 2;
            TransferNO();
        } else if (tipomov.equalsIgnoreCase("DAC")) {
            ntipo = 4;
            TransferNO();
        } else {
            TransferNO();
        }
        String papele[];
        if (ID.split("_")[4].equalsIgnoreCase("DC")){
            papele=new String[]{"- nessuna selezione -",
                "AIRDROP, CASHBACK, EARN etc...",
                "DEPOSITO CON COSTO DI CARICO A ZERO",
                "TRASFERIMENTO TRA WALLET DI PROPRIETA' (bisognerà selezionare il movimento di prelievo nella tabella sotto)",
                "ACQUISTO CRYPTO (Tramite contanti,servizi esterni etc...) o DONAZIONE",
                "SCAMBIO CRYPTO DIFFERITO",
                "TRASFERIMENTO DA VAULT/PIATTAFORMA A RENDITA"};

        }else
        {
            papele=new String[]{"- nessuna selezione -",
                "CASHOUT / COMMISSIONE (verrà calcolata la plusvalenza)",
                "DONAZIONE o FURTO Crypto-Attività",
                "TRASFERIMENTO TRA WALLET DI PROPRIETA' (bisognerà selezionare il movimento di deposito nella tabella sotto)",
                "SCAMBIO CRYPTO DIFFERITO",
                "TRASFERIMENTO A VAULT/PIATTAFORMA A RENDITA"};

        }
            ArrayList<String> elements = new ArrayList<>();
            elements.addAll(java.util.Arrays.asList(papele));
       
            //ComboBoxModel model = new DefaultComboBoxModel(elements.toArray());
            ComboBoxModel<String> model = new DefaultComboBoxModel<>(elements.toArray(String[]::new));
            ComboBox_TipoMovimento.setModel(model);
            this.ComboBox_TipoMovimento.setSelectedIndex(ntipo);
            String v[]=MappaCryptoWallet.get(ID);
            TextArea_Note.setText(v[21].replace("<br>" ,"\n"));
        //    CompilaTabellaMovimetiAssociabili(ID);

        
        
    }
  
    
    
    public boolean getModificaEffettuata(){
        return ModificaEffettuata;
    }
    public void setModificaEffettuata(boolean b){
        ModificaEffettuata=b;
    }
    
    public String[] DammiRigaTabellaDaID(String ID){
          String v[]=MappaCryptoWallet.get(ID);
          String TipoMovimento=v[0].split("_")[4].trim();

            String riga[]=new String[8];
            riga[0]=v[0];
            riga[1]=v[1];
            riga[2]=v[3];
            riga[3]=v[5];
            if (TipoMovimento.equalsIgnoreCase("PC"))
                {
                riga[4]=v[8];
                riga[5]=new BigDecimal(v[10]).stripTrailingZeros().toPlainString();
                }
            else
                {
                riga[4]=v[11];
                riga[5]=new BigDecimal(v[13]).stripTrailingZeros().toPlainString();
                }
            riga[6]=v[18];
            riga[7]=v[15];
           return riga;                                              
    }                

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        ComboBox_TipoMovimento = new javax.swing.JComboBox<>();
        Bottone_OK = new javax.swing.JButton();
        Bottone_Annulla = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        Tabella_MovimentiAbbinati = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TextArea_Note = new javax.swing.JTextArea();
        jLabel_EscursioneMassima = new javax.swing.JLabel();
        ComboBox_EscursioneMassimaPercentuale = new javax.swing.JComboBox<>();
        ComboBox_EscursioneMassimaGiorni = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Dettaglio Movimento:");

        jLabel2.setText("Scegli la tipologia di movimento dalla lista :");

        ComboBox_TipoMovimento.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ComboBox_TipoMovimento.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_TipoMovimentoItemStateChanged(evt);
            }
        });

        Bottone_OK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Salva.png"))); // NOI18N
        Bottone_OK.setText("OK");
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

        jLabel3.setText("Scegli movimento da abbinare qua sotto :");

        jTable1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Data e Ora", "Exchange/Wallet", "Tipo", "Moneta", "Qta", "null", "Prezzo"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setFocusable(false);
        jTable1.setRequestFocusEnabled(false);
        jTable1.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(1);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(1);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(1);
            jTable1.getColumnModel().getColumn(1).setMinWidth(120);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(120);
            jTable1.getColumnModel().getColumn(1).setMaxWidth(120);
            jTable1.getColumnModel().getColumn(2).setMinWidth(200);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(200);
            jTable1.getColumnModel().getColumn(2).setMaxWidth(200);
            jTable1.getColumnModel().getColumn(3).setMinWidth(200);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(200);
            jTable1.getColumnModel().getColumn(3).setMaxWidth(200);
            jTable1.getColumnModel().getColumn(4).setMinWidth(60);
            jTable1.getColumnModel().getColumn(4).setPreferredWidth(60);
            jTable1.getColumnModel().getColumn(4).setMaxWidth(100);
            jTable1.getColumnModel().getColumn(6).setMinWidth(0);
            jTable1.getColumnModel().getColumn(6).setPreferredWidth(0);
            jTable1.getColumnModel().getColumn(6).setMaxWidth(0);
        }

        Tabella_MovimentiAbbinati.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Data e Ora", "Exchange/Wallet", "Tipo", "Moneta", "Qta", "null", "Prezzo"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(Tabella_MovimentiAbbinati);
        if (Tabella_MovimentiAbbinati.getColumnModel().getColumnCount() > 0) {
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(0).setMinWidth(1);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(0).setPreferredWidth(1);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(0).setMaxWidth(1);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(1).setMinWidth(120);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(1).setPreferredWidth(120);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(1).setMaxWidth(120);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(2).setMinWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(2).setPreferredWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(2).setMaxWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(3).setMinWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(3).setPreferredWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(3).setMaxWidth(200);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(4).setMinWidth(60);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(4).setPreferredWidth(60);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(4).setMaxWidth(100);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(6).setMinWidth(0);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(6).setPreferredWidth(0);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(6).setMaxWidth(0);
        }

        jLabel4.setText("Note :");

        TextArea_Note.setColumns(20);
        TextArea_Note.setRows(5);
        jScrollPane1.setViewportView(TextArea_Note);

        jLabel_EscursioneMassima.setText("Escursione Massima per trasferimenti  (in percentule sul valore) :");

        ComboBox_EscursioneMassimaPercentuale.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100" }));
        ComboBox_EscursioneMassimaPercentuale.setSelectedIndex(3);
        ComboBox_EscursioneMassimaPercentuale.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_EscursioneMassimaPercentualeItemStateChanged(evt);
            }
        });

        ComboBox_EscursioneMassimaGiorni.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "15", "30", "45", "60", "90", "120", "240", "365" }));
        ComboBox_EscursioneMassimaGiorni.setSelectedIndex(1);
        ComboBox_EscursioneMassimaGiorni.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_EscursioneMassimaGiorniItemStateChanged(evt);
            }
        });

        jLabel5.setText("Escursione Massima per scambi differiti  (in giorni) :");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ComboBox_TipoMovimento, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel_EscursioneMassima, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ComboBox_EscursioneMassimaPercentuale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ComboBox_EscursioneMassimaGiorni, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_OK, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Annulla))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 854, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ComboBox_TipoMovimento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(ComboBox_EscursioneMassimaGiorni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_EscursioneMassima)
                            .addComponent(ComboBox_EscursioneMassimaPercentuale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(Bottone_OK, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Bottone_Annulla, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AnnullaActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_Bottone_AnnullaActionPerformed

    private void Bottone_OKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_OKActionPerformed
        // TODO add your handling code here:
        //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
        //PCO -> Cashout o similare (prelievo)
        //PTW -> Trasferimento tra Wallet (prelievo)
        //DTW -> Trasferimento tra Wallet (deposito)
        //DAI -> Airdrop o similare (deposito)
        //DCZ -> Costo di carico 0 (deposito)
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int scelta = this.ComboBox_TipoMovimento.getSelectedIndex();
        boolean completato = true;
        String descrizione;
        String dettaglio = "";
        //String Note=jTextField1.getText();
        String Note = TextArea_Note.getText().replace("\n", "<br>");
        String attuale[] = MappaCryptoWallet.get(IDTrans);
        String PartiCoinvolte[] = (IDTrans + "," + attuale[20]).split(",");
        String PrzCarico = "Da calcolare";
        String plusvalenza = "Da calcolare";
        String PrzVecchio ="";//Viene usato per spostare il prezzo orginale delle donazioni/acquisti
        boolean trasferimento = false;
        if (IDTrans.split("_")[4].equalsIgnoreCase("DC")) {
            //in questo caso sono in presenza di un movimento di deposito
            switch (scelta) {
                case 1 -> {
                    //Se scelgo il caso 1 faccio scegliere che tipo di reward voglio
                    descrizione = "REWARD";
                    dettaglio = "DAI - Airdrop,Cashback,Rewards etc.. ";
                    String Testo = "<html>Decidere il tipo di provento a cui appartiene il movimento di deposito.<br><br>"
                            + "<b>Come classifichiamo il movimento?<br><br><b>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "REWARD", "STAKING REWARD", "EARN", "CASHBACK", "AIRDROP"};
                    scelta = JOptionPane.showOptionDialog(this, Testo,
                            "Classificazione del movimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                descrizione = "REWARD";
                            }
                            case 2 -> {
                                descrizione = "STAKING REWARD";
                            }
                            case 3 -> {
                                descrizione = "EARN";
                            }
                            case 4 -> {
                                descrizione = "CASHBACK";
                            }
                            case 5 -> {
                                descrizione = "AIRDROP";
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        completato=false;
                    }
                    if (completato){
                        //Se effettuo una scelta valida controllo se è un movimento in defi
                        //Qualora lo fosse e trovo movimenti identici chiedo se si vuole classificare anche tutti gli altri movimenti allo stesso modo
                        //Se rispondo si li faccio tutti
                        //FASE 1: recupero tutti i movimenti con la stessa moneta e stesso contratto
                       // String Movimento[] = MappaCryptoWallet.get(ID);
                        String Moneta = attuale[11];
                        String AddressMoneta=attuale[28];
                        String AddressContratto = null;
                        if (attuale.length > 30) {
                            AddressContratto = attuale[30];
                        }
                        ArrayList<String> ListaIDMovimentiUguali = new ArrayList<>();
                        //for (String[] v : MappaCryptoWallet.values()) {
                        for (String IDnc : CDC_Grafica.DepositiPrelieviDaCategorizzare) {
                            String v[] = MappaCryptoWallet.get(IDnc);
                            //considero solo i movimenti che hanno l'address del contratto in memoria
                            //perchè a me interessa trovare i movimenti dello stesso tipo e l'unica è basarsi sul contratto
                            if (v.length > 30) {
                                String AddContratto = v[30];
                                if (AddressContratto != null && AddContratto != null
                                        && !AddressContratto.isBlank() && !AddContratto.isBlank()
                                        && AddressContratto.equalsIgnoreCase(AddContratto)
                                        && !v[0].equals(IDTrans)
                                        && v[0].split("_")[4].equalsIgnoreCase("DC")
                                        && v[11].equals(Moneta)
                                        && v[28].equals(AddressMoneta)
                                        && v[18].isBlank()) {//questo serve per trovare solo i movimenti non ancora classificati

                                    //Sotto questa if ci sono tutti i movimenti di deposito
                                    //che riguardano la stessa moneta e lo stesso contratto
                                    ListaIDMovimentiUguali.add(v[0]);
                                }

                            }
                        }
                        //boolean TuttiiMovimenti = false;
                        if (!ListaIDMovimentiUguali.isEmpty()){
                String Messaggio="Sono stati trovati altri "+ListaIDMovimentiUguali.size()+" movimenti analoghi non ancora classificati, vuoi considerarli allo stesso modo?";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Classificazione movimenti multipli", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        //modifico tutti i movimenti
                        //Salvo un booleano a true in mdo da sapere che ho fatto questa scelta per dopo

                        for (String IDMov:ListaIDMovimentiUguali){
                            String mov[]=MappaCryptoWallet.get(IDMov);
                                mov[5] = descrizione;
                                mov[18] = dettaglio;
                                mov[20] = "";
                                mov[21] = Note;
                                MappaCryptoWallet.put(IDMov, mov);
                        }
                        ModificaEffettuata=true;
                       // TuttiiMovimenti=true;
                    }
                    case 1 -> {
                        //modifico il solo movimento interessato
                        //in sostanza non faccio nulla di più perchè il movimento verrà modificato già dalla funzione stessa
                    }
                    case -1 -> {
                        completato=false;
                    }
                    default -> {
                    }
                }
               // System.out.println(risposta);
            }
                    }
                    //Adesso se trovo movimenti con le stesse caratteristiche chiedo se voglio assegnarli tutti allo stesso modo
                        
                       
                    
                   // plusvalenza = attuale[15];
                   // PrzCarico = attuale[15];
                }
                case 2 -> {
                    descrizione = "DEPOSITO CRYPTO (a costo zero)";
                    dettaglio = "DCZ - Deposito a costo zero (no plusvalenza)";
                    PrzCarico = "0.00";
                    plusvalenza = "0.00";
                }
                case 3 -> {
                    descrizione = "TRASFERIMENTO TRA WALLET";
                    dettaglio = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    plusvalenza = "0.00";
                    trasferimento = true;
                }
                case 4 -> {
                    descrizione = "ACQUISTO CRYPTO";
                    dettaglio = "DAC - Acquisto Crypto";
                    
                    String Testo = "<html>Decidere il tipo di provento a cui appartiene il movimento di deposito.<br><br>"
                            + "<b>Come classifichiamo il movimento?<br><br><b>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "ACQUISTO CRYPTO", "DONAZIONE"};
                    scelta = JOptionPane.showOptionDialog(this, Testo,
                            "Classificazione del movimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    String testo="";
                    if (scelta != 0 && scelta != -1) {
                        
                        switch (scelta) {
                            case 1 -> {
                               // descrizione = "ACQUISTO CRYPTO";
                               // dettaglio = "DAC - Acquisto Crypto";
                                testo="Indica il valore di acquisto corretto in Euro : ";
                            }
                            case 2 -> {
                                descrizione = "DONAZIONE";
                                dettaglio = "DAC - Donazione";
                                testo="Indica il costo di carico della donazione ricevuta : ";
                            }
                            default -> {
                            }
                        }
                        String m = JOptionPane.showInputDialog(this, testo, attuale[15]);
                        completato = m != null; //se premo annulla nel messaggio non devo poi chiudere la finestra, quindi metto completato=false
                        if (m != null) {
                            m = m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                            if (CDC_Grafica.Funzioni_isNumeric(m, false)) {
                                //Se trovo un prezzo vecchio in 35 significa che sto ricodificando lo stesso movimento
                                //A questo punto prima di andare avanti ripristino la situazione orginale
                                if (attuale[35]!=null&&!attuale[35].isBlank()){
                                    attuale[15]=attuale[35];
                                    attuale[35]="";
                                }
                                PrzVecchio = attuale[15];
                                attuale[15] = m;
                                PrzCarico = attuale[15];
                                plusvalenza = "0.00";
                            } else {
                                completato = false;
                                JOptionPane.showConfirmDialog(this, "Attenzione, " + m + " non è un numero valido!",
                                        "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            }
                        }
                    }else completato=false;
                    }
                    
                case 5 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    trasferimento = true;

                }
                case 6 -> {
                    descrizione = "TRASFERIMENTO DA PIATTAFORMA";
                    dettaglio = "DTW - Trasferimento da Vault/Piattaforma a Rendita";
                    trasferimento = false;
                }
                default -> {
                    /*   descrizione = "DEPOSITO CRYPTO";
                    System.out.println(attuale[9]);
                    System.out.println(attuale[12]);
                    System.out.println(Importazioni.RitornaTipologiaTransazione(attuale[9], attuale[12],1));*/
                    descrizione = Importazioni.RitornaTipologiaTransazione(null, attuale[12], 1);
                }
                //qui si va solo in caso la scelata sia nessuna
            }
        } else {
            //in questo caso sono in presenza di un movimento di prelievo
            switch (scelta) {
                case 1 -> {
                    descrizione = "CASHOUT o COMMISSIONI";
                    dettaglio = "PCO - Cashout, acquisti con crypto etc.. (plusvalenza)";
                    //Se scelgo il caso 1 faccio scegliere che tipo di reward voglio
                    String Testo = "<html>Decidere il tipo di provento a cui appartiene il movimento di deposito.<br><br>"
                            + "<b>Come classifichiamo il movimento?<br><br><b>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "CASH OUT", "COMMISSIONI"};
                    scelta = JOptionPane.showOptionDialog(this, Testo,
                            "Classificazione del movimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                descrizione = "CASH OUT";
                            }
                            case 2 -> {
                                descrizione = "COMMISSIONI";
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        completato=false;
                    }
                }
                case 2 -> {
                    //descrizione = "PRELIEVO CRYPTO (tolgo dai calcoli)";
                   // dettaglio = "PWN - Tolgo dai calcoli delle medie (no plusvalenza)";
                    descrizione = "FURTO o DONAZIONE";
                    dettaglio = "PWN - Tolgo dai calcoli delle medie (no plusvalenza)";
                    //Se scelgo il caso 1 faccio scegliere che tipo di reward voglio
                    String Testo = "<html>Furto o Donazione?<br><br>"
                            + "<b>Come classifichiamo il movimento?<br><br><b>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "FURTO", "DONAZIONE"};
                    scelta = JOptionPane.showOptionDialog(this, Testo,
                            "Classificazione del movimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                descrizione = "FURTO";
                                dettaglio = "PWN - Furto";
                            }
                            case 2 -> {
                                descrizione = "DONAZIONE";
                                dettaglio = "PWN - Donazione";
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        completato=false;
                    }
                    plusvalenza = "0";
                }
                case 3 -> {
                    descrizione = "TRASFERIMENTO TRA WALLET";
                    dettaglio = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    plusvalenza = "0";
                    trasferimento = true;
                }
                case 4 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    trasferimento = true;
                }
                case 5 -> {
                    descrizione = "TRASFERIMENTO A PIATTAFORMA";
                    dettaglio = "PTW - Trasferimento a Vault/Piattaforma a Rendita";
                    trasferimento = true;
                }
                default ->
                    //descrizione = "PRELIEVO CRYPTO";
                    descrizione = Importazioni.RitornaTipologiaTransazione(attuale[9], null, 1);
            }
        }

        if (completato) {
         if (PartiCoinvolte.length > 1) {
            //se controparte non è vuota vado ad eliminare l'associazione anche al movimento associato
            //a cancellare le eventuali commissioni e riportare i prezzi e qta allo stato originale
            IDTrans=RiportaTransazioniASituazioneIniziale(PartiCoinvolte,IDTrans);
        }
         //Adesso controllo se esiste ancora IDTrans e proseguio solo se esiste
         //if (MappaCryptoWallet.get(IDTrans)!=null){
            //System.out.println("Completato");
            if (descrizione.equalsIgnoreCase("TRASFERIMENTO A PIATTAFORMA")) {
                //creo movimento di deposito su Vault e movifico il movimento originale
                //in questa funzione non devo controllare nulla di particolare
                CreaMovimentiTrasferimentoAVault(IDTrans, descrizione, dettaglio);
                this.dispose();
            } else if (descrizione.equalsIgnoreCase("TRASFERIMENTO DA PIATTAFORMA")) {
                CreaMovimentiTrasferimentoDaVault(IDTrans, descrizione, dettaglio);
                this.dispose();
            } else if (!trasferimento) {
                attuale[5] = descrizione;
                attuale[17] = PrzCarico;
                attuale[18] = dettaglio;
                attuale[19] = plusvalenza;
                attuale[20] = "";
                attuale[21] = Note;
                //Se trovo un prezzo vecchio (originale) messo da parte, lo ripristino
                if (attuale[35]!=null&&!attuale[35].isBlank())attuale[15]=attuale[35];
                attuale[35] = PrzVecchio;
                
                //in teoria avendo preso l'oggetto e modificandone il contenuto non serve questa seconda parte
                MappaCryptoWallet.put(IDTrans, attuale);
                JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                        "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                ModificaEffettuata = true;
                this.dispose();
            } else {
                //Se arrivo qua sono in presenza di un trasferimento tra wallet o uno scambio differito
                if (Tabella_MovimentiAbbinati.getSelectedRow() >= 0) {
                    int rigaselezionata = Tabella_MovimentiAbbinati.getSelectedRow();
                    String IDTransazioneControparte = Tabella_MovimentiAbbinati.getValueAt(rigaselezionata, 0).toString();
                    //devo aggiungere che dettaglioTrasferimento deve essere vuoto!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    String attualeControparte[] = MappaCryptoWallet.get(IDTransazioneControparte);
                    String IDPrelievo;
                    String IDDeposito;
                    //se il movimento selezionato non è associato a nulla alloro lo associo, altrimenti faccio uscire un messaggio di errore
                    if (attualeControparte[20].equalsIgnoreCase("")) {

                        if (IDTrans.split("_")[4].equalsIgnoreCase("DC")) {
                            // tipoControparte = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                            IDDeposito = IDTrans;
                            IDPrelievo = IDTransazioneControparte;
                            //creo movimento di commissione etc
                            if (descrizione.equalsIgnoreCase("TRASFERIMENTO TRA WALLET")) {
                                CreaMovimentiTrasferimentosuWalletProprio(IDPrelievo, IDDeposito);
                            } else if (descrizione.equalsIgnoreCase("SCAMBIO CRYPTO DIFFERITO")) {
                                CreaMovimentiScambioCryptoDifferito(IDPrelievo, IDDeposito);
                              /*  JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                                        "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);*/
                            }
                        } else {
                            IDDeposito = IDTransazioneControparte;
                            IDPrelievo = IDTrans;
                            //creo movimento di commissione e valori
                            if (descrizione.equalsIgnoreCase("TRASFERIMENTO TRA WALLET")) {
                                CreaMovimentiTrasferimentosuWalletProprio(IDPrelievo, IDDeposito);
                            } else if (descrizione.equalsIgnoreCase("SCAMBIO CRYPTO DIFFERITO")) {
                                CreaMovimentiScambioCryptoDifferito(IDPrelievo, IDDeposito);
                               /* JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                                        "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);*/
                            }

                        }
                        attuale[21] = Note;

                        JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                                "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                        ModificaEffettuata = true;
                        this.dispose();

                    } else {
                        JOptionPane.showConfirmDialog(this, "Il movimento selezionato dalla tabella è già abbinato ad un altro movimento!\n"
                                + "Scegliere altro movimento oppure disabbinare dal movimento precedentemente associato e ripetere la procedura.",
                                "Attenzione richiesta!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
                    }
                } else {
                    //in questo caso mando fuori un messaggio che dice di selezionare un movimento di conroparte per andare avanti
                    JOptionPane.showConfirmDialog(this, "Per questo tipo di movimentazione è obbligatorio selezionare il movimento corrispondente nella tabella!",
                            "Attenzione richiesta!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
                }

            }
        // }
        }
this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    }//GEN-LAST:event_Bottone_OKActionPerformed

    
    public static void CreaMovimentiTrasferimentosuWalletProprio(String IDPrelievo,String IDDeposito){
        //come prima cosa devo generare un nuovo id per il prelievo
       // System.out.println("Creo commissione");
        String MovimentoPrelievo[]=MappaCryptoWallet.get(IDPrelievo);
        String MovimentoDeposito[]=MappaCryptoWallet.get(IDDeposito);
        
   /*     long dataPrelievo=Long.parseLong(IDPrelievo.split("_")[0]);
        long dataDeposito=Long.parseLong(IDDeposito.split("_")[0]);
        //affinchè i movimenti di deposito e prelievo risultino in ordine se trovo che un movimento di prelievo è successivo ad uno di posito vado a cambiargli 
        //l'id della transazione, salvo poi il vecchio id nel movimento in modo da poterlo ripristinare in caso io cancelli l'associazione in futuro
        if (dataPrelievo>dataDeposito){
    ///////////DA FARE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
            
            MovimentoPrelievo[31]=IDPrelievo;
            dataPrelievo=dataDeposito-1;
            String Prel[]=IDPrelievo.split("_");
            String NuovoIDPrelievo=String.valueOf(dataPrelievo)+"_"+Prel[1]+"_"+Prel[2]+"_"+Prel[3]+"_"+Prel[4];
            MovimentoPrelievo[0]=NuovoIDPrelievo;
            MappaCryptoWallet.put(NuovoIDPrelievo, MovimentoPrelievo);
            //finito la creazione del nuovo movimento cancello il vecchio
            MappaCryptoWallet.remove(IDPrelievo);
            //l'idprelievo adesso diventa quello nuovo
            IDPrelievo=NuovoIDPrelievo;
            
        }*/
        BigDecimal QtaPrelievoValoreAssoluto=new BigDecimal(MovimentoPrelievo[10]).stripTrailingZeros().abs();
        BigDecimal QtaDepositoValoreAssoluto=new BigDecimal(MovimentoDeposito[13]).stripTrailingZeros().abs();
        //Vado avanti solo se la qta prelevata è maggiore o uguale di quelòla ricevuta
  //      if (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)>=0){
            
        MovimentoPrelievo[5]="TRASFERIMENTO TRA WALLET";
        MovimentoDeposito[5]="TRASFERIMENTO TRA WALLET";
        MovimentoPrelievo[18]="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        MovimentoDeposito[18]="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)"; 
        

        
        String IDPrelivoSpezzato[]=IDPrelievo.split("_");
        
        
            //Se movimento di Prelievo è uguale a movimento di deposito allora non devo creare nessuna commissione
            switch (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)) {
                case 1 -> {
                    int numTransazione=Integer.parseInt(IDPrelivoSpezzato[2])+1;
                    String MovimentoCommissione[]=new String[Importazioni.ColonneTabella];
                    String IDCommissione = IDPrelivoSpezzato[0] + "_" + IDPrelivoSpezzato[1] + "_" + numTransazione + "_1_CM";
                    String QtaCommissione = new BigDecimal(MovimentoPrelievo[10]).abs().subtract(new BigDecimal(MovimentoDeposito[13]).abs()).toPlainString();
                    String ValoreCommissione = new BigDecimal(MovimentoPrelievo[15]).abs().divide(new BigDecimal(MovimentoPrelievo[10]).abs(), DecimaliCalcoli, RoundingMode.HALF_UP).multiply(new BigDecimal(QtaCommissione)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                    MovimentoCommissione[0] = IDCommissione;
                    MovimentoCommissione[1] = MovimentoPrelievo[1];
                    MovimentoCommissione[2] = "1 di 1";
                    MovimentoCommissione[3] = MovimentoPrelievo[3];
                    MovimentoCommissione[4] = MovimentoPrelievo[4];
                    MovimentoCommissione[5] = "COMMISSIONE";
                    MovimentoCommissione[6] = MovimentoPrelievo[6];
                    MovimentoCommissione[8] = MovimentoPrelievo[8];
                    MovimentoCommissione[9] = MovimentoPrelievo[9];
                    MovimentoCommissione[10] = "-" + QtaCommissione;
                    MovimentoCommissione[15] = ValoreCommissione;
                    MovimentoCommissione[20] = IDPrelievo + "," + IDDeposito;
                    MovimentoCommissione[22] = "AU"; //AU -> Significa che è un movimento di commissione automaticamente generato da una condizione successiva
                    //quindi se decadono le condizioni che lo hanno generato va eliminato
                    //ad esempio se uno dei movimenti padri cambiano tipo o vengono eliminati va eliminato anche il suddetto movimento
                    MovimentoCommissione[25] = MovimentoPrelievo[25];
                    MovimentoCommissione[26] = MovimentoPrelievo[26];
                    MovimentoCommissione[32] = MovimentoPrelievo[32];

                    Importazioni.RiempiVuotiArray(MovimentoCommissione);
                    //Se Qta prelievo maggiore di Qta Depositata
                    MappaCryptoWallet.put(IDCommissione, MovimentoCommissione);
                    MovimentoPrelievo[10]=new BigDecimal(MovimentoPrelievo[10]).subtract(new BigDecimal(MovimentoCommissione[10])).toPlainString();
                    MovimentoPrelievo[15]=new BigDecimal(MovimentoPrelievo[15]).subtract(new BigDecimal(MovimentoCommissione[15])).toPlainString();
                    MovimentoPrelievo[20]=IDDeposito+","+IDCommissione;
                    MovimentoDeposito[20]=IDPrelievo+","+IDCommissione;
                }
                case 0 -> {
                    //Se Qta Prelievo uguale a Qta depositata
                    MovimentoPrelievo[20]=IDDeposito;
                    MovimentoDeposito[20]=IDPrelievo;
                }
                default -> {
                    //Questo ultimo caso si verifica quando la Qta Prelievo è minore di quella depositata
                    String numTransazione="0"+IDPrelivoSpezzato[2];
                    String IDReward=IDPrelivoSpezzato[0]+"_"+IDPrelivoSpezzato[1]+"_"+numTransazione+"_1_RW";
                    String QtaReward=new BigDecimal(MovimentoDeposito[13]).abs().subtract(new BigDecimal(MovimentoPrelievo[10]).abs()).toPlainString();
                    String MovimentoReward[] = new String[Importazioni.ColonneTabella];
                    String ValoreReward=new BigDecimal(MovimentoPrelievo[15]).abs().divide(new BigDecimal(MovimentoPrelievo[10]).abs(),DecimaliCalcoli, RoundingMode.HALF_UP).multiply(new BigDecimal(QtaReward)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                    MovimentoReward[0] = IDReward;
                    MovimentoReward[1] = MovimentoPrelievo[1];
                    MovimentoReward[2] = "1 di 1";
                    MovimentoReward[3] = MovimentoPrelievo[3];
                    MovimentoReward[4] = MovimentoPrelievo[4];
                    MovimentoReward[5] = "REWARD";
                    MovimentoReward[6] = MovimentoDeposito[6];
                    MovimentoReward[11] = MovimentoPrelievo[8];
                    MovimentoReward[12] = MovimentoPrelievo[9];
                    MovimentoReward[13] = QtaReward;
                    MovimentoReward[15] = ValoreReward;
                    MovimentoReward[20] = IDPrelievo + "," + IDDeposito;
                    MovimentoReward[22] = "Rettifica su Trasferimento";
                    MovimentoReward[22] = "AU"; //AU -> Significa che è un movimento di commissione automaticamente generato da una condizione successiva
                    //quindi se decadono le condizioni che lo hanno generato va eliminato
                    //ad esempio se uno dei movimenti padri cambiano tipo o vengono eliminati va eliminato anche il suddetto movimento
                    MovimentoReward[25] = MovimentoPrelievo[25];
                    MovimentoReward[26] = MovimentoPrelievo[26];
                    MovimentoReward[32] = MovimentoPrelievo[32];

                    Importazioni.RiempiVuotiArray(MovimentoReward);
                    
                    MappaCryptoWallet.put(IDReward, MovimentoReward);
                    MovimentoPrelievo[10]=new BigDecimal(MovimentoPrelievo[10]).subtract(new BigDecimal(MovimentoReward[13])).toPlainString();
                    MovimentoPrelievo[15]=new BigDecimal(MovimentoPrelievo[15]).add(new BigDecimal(MovimentoReward[15])).toPlainString();
                    MovimentoPrelievo[20]=IDDeposito+","+IDReward;
                    MovimentoDeposito[20]=IDPrelievo+","+IDReward;
                }
            }
            //Parte che modifica i movimenti preesistenti
              
       //  }
        
        
        
                
    }
    
    private void CreaMovimentiTrasferimentoAVault(String ID,String Descrizione,String Dettaglio){
            //controllo se ho movimenti simili e chiedo se voglio classificarli nella stessa maniera
            //poi creo movimento di deposito su Vault e movifico il movimento originale

            
            //FASE 1: recupero tutti i movimenti con la stessa moneta e stesso contratto
            String Movimento[]=MappaCryptoWallet.get(ID);
            String Moneta=Movimento[8];
            String AddressContratto=null;
            if (Movimento.length>30)AddressContratto=Movimento[30];
            ArrayList<String> ListaIDMovimentiUguali = new ArrayList<>();
            //for (String[] v : MappaCryptoWallet.values()) {
            for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
                String v[]=MappaCryptoWallet.get(IDnc);
                //considero solo i movimenti che hanno l'address del contratto in memoria
                //perchè a me interessa trovare i movimenti dello stesso tipo e l'unica è basarsi sul contratto
                if (v.length>30){
                    String AddContratto=v[30];
                    if (AddressContratto!=null&&AddContratto!=null&&
                        !AddressContratto.isBlank()&&!AddContratto.isBlank()&&    
                        AddressContratto.equalsIgnoreCase(AddContratto)&&
                        !v[0].equals(ID)&&
                        v[0].split("_")[4].equalsIgnoreCase("PC")&&
                            v[8].equals(Moneta)&&
                            v[18].isBlank()){//questo serve per trovare solo i movimenti non ancora classificati
                        
                        //Sotto questa if ci sono tutti i movimenti di deposito
                        //che riguardano la stessa moneta e lo stesso contratto
                        ListaIDMovimentiUguali.add(v[0]);                       
                    }
                    
                }
            }
            boolean TuttiiMovimenti=false;
            //FASE 2: Se trovo movimenti identici chiedo se voglio che anche questi siano classificati come Vault
            if (!ListaIDMovimentiUguali.isEmpty()){
                String Messaggio="Sono stati trovati altri "+ListaIDMovimentiUguali.size()+" movimenti analoghi non ancora classificati, vuoi considerarli allo stesso modo?";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Classificazione movimenti multipli", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        //FASE 3:modifico tutti i movimenti
                        //Salvo un booleano a true in mdo da sapere che ho fatto questa scelta per dopo
                        CreaMovimentoTrasferimentoAVault(ID,Descrizione,Dettaglio);
                        for (String IDMov:ListaIDMovimentiUguali){
                            CreaMovimentoTrasferimentoAVault(IDMov,Descrizione,Dettaglio);
                        }
                        ModificaEffettuata=true;
                        TuttiiMovimenti=true;
                    }
                    case 1 -> {
                        //FASE 3:modifico il solo movimento interessato
                        CreaMovimentoTrasferimentoAVault(ID,Descrizione,Dettaglio);
                        ModificaEffettuata=true;
                    }
                    case -1 -> {
                        //FASE 3:non modifico nulla
                        JOptionPane.showConfirmDialog(this, "Operazione Annullata",
                    "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                    default -> {
                    }
                }
               // System.out.println(risposta);
            }else{
                //FASE 3:modifico il solo movimento interessato
                CreaMovimentoTrasferimentoAVault(ID,Descrizione,Dettaglio);
                ModificaEffettuata=true;  
                TuttiiMovimenti=true;
            }
            
            
            //Adesso verifico se ci sono movimenti di rientro di denaro dal vault e chiedo se voglio sistemare anche quelli
            //Questo lo faccio controllando contratto, moneta e tipo movimento, se contratto e moneta coincidono e tipo movimento è inverso allora ho 
            //trovato movimenti papaili per la richiesta
            //la richiesta la devo fare solo se alla domanda se volevo categorizzare tutti i movimenti è stato risposto si.
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            ListaIDMovimentiUguali = new ArrayList<>();
            if (TuttiiMovimenti)
            {
                //Verifico se trovo movimenti di rientro e eventualmente avviso se si vuole classificarli in automatico
               // for (String[] v : MappaCryptoWallet.values()) {
                for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
                String v[]=MappaCryptoWallet.get(IDnc);

                        String AddContratto = v[30];
                        if (AddressContratto != null && AddContratto != null
                                && !AddressContratto.isBlank() && !AddContratto.isBlank()
                                && AddressContratto.equalsIgnoreCase(AddContratto)
                                && !v[0].equals(ID)
                                && v[0].split("_")[4].equalsIgnoreCase("DC")
                                && v[11].equals(Moneta)
                                && v[18].isBlank()) {//questo serve per trovare solo i movimenti non ancora classificati
                            ListaIDMovimentiUguali.add(v[0]);
                        }

                    
                }
                
                
                
                
                
                
               if (!ListaIDMovimentiUguali.isEmpty()){
                    Descrizione = "TRASFERIMENTO DA PIATTAFORMA";
                    Dettaglio = "DTW - Trasferimento da Vault/Piattaforma a Rendita";
                 String Messaggio="<html>Sono stati trovati "+ListaIDMovimentiUguali.size()+" movimenti di rientro da questo contratto non ancora classificati.<br>"
                         + "Vuoi che vengano classificati automaticamente?</html>";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Classificazione movimenti multipli", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        //Classifico tutti i movimenti di rientro
                        for (String IDMov:ListaIDMovimentiUguali){
                            CreaMovimentoTrasferimentoDaVault(IDMov,Descrizione,Dettaglio);
                        }
                        ModificaEffettuata=true;

                    }
                    case 1 -> {
                        //non faccio nulla
                    }
                    case -1 -> {
                        //non faccio nulla
                    }
                    default -> {//non faccio nulla
                    } }
            }}
            if (ModificaEffettuata)            JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                    "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
    }
    
    
    
    
    
    
        public static int CreaMovimentiTrasferimentoAVaultNonPresidiati(String ID){
            //controllo se ho movimenti simili e chiedo se voglio classificarli nella stessa maniera
            //poi creo movimento di deposito su Vault e movifico il movimento originale
            int movimentimodificati=1;
            String Descrizione="TRASFERIMENTO A PIATTAFORMA";
            String Dettaglio="PTW - Trasferimento a Vault/Piattaforma a Rendita";
            
            //FASE 1: recupero tutti i movimenti con la stessa moneta e stesso contratto
            String Movimento[]=MappaCryptoWallet.get(ID);
            String Moneta=Movimento[8];
            String AddressContratto=null;
            if (Movimento.length>30)AddressContratto=Movimento[30];
            ArrayList<String> ListaIDMovimentiUguali = new ArrayList<>();
            for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
            String v[]=MappaCryptoWallet.get(IDnc);
            //for (String[] v : MappaCryptoWallet.values()) {
           // String v[]=MappaCryptoWallet.get(IDnc);
                //considero solo i movimenti che hanno l'address del contratto in memoria
                //perchè a me interessa trovare i movimenti dello stesso tipo e l'unica è basarsi sul contratto
                if (v.length>30){
                    String AddContratto=v[30];
                    if (AddressContratto!=null&&AddContratto!=null&&
                             !AddressContratto.isBlank() && !AddContratto.isBlank()&&
                        AddressContratto.equalsIgnoreCase(AddContratto)&&
                        !v[0].equals(ID)&&
                        v[0].split("_")[4].equalsIgnoreCase("PC")&&
                            v[8].equals(Moneta)&&
                            v[18].isBlank()){//questo serve per trovare solo i movimenti non ancora classificati
                        
                        //Sotto questa if ci sono tutti i movimenti di deposito
                        //che riguardano la stessa moneta e lo stesso contratto
                        ListaIDMovimentiUguali.add(v[0]); 
                        movimentimodificati++;
                    }
                    
                }
            }

            //FASE 2: Se trovo movimenti identici chiedo se voglio che anche questi siano classificati come Vault
            CreaMovimentoTrasferimentoAVault(ID,Descrizione,Dettaglio);
            if (!ListaIDMovimentiUguali.isEmpty()){
                        //FASE 3:modifico tutti i movimenti
                        for (String IDMov:ListaIDMovimentiUguali){
                            CreaMovimentoTrasferimentoAVault(IDMov,Descrizione,Dettaglio);
                        }
            }
            
            
            //Adesso verifico se ci sono movimenti di rientro di denaro dal vault e chiedo se voglio sistemare anche quelli
            //Questo lo faccio controllando contratto, moneta e tipo movimento, se contratto e moneta coincidono e tipo movimento è inverso allora ho 
            //trovato movimenti papaili per la richiesta
            //la richiesta la devo fare solo se alla domanda se volevo categorizzare tutti i movimenti è stato risposto si.
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            ListaIDMovimentiUguali = new ArrayList<>();

                //Verifico se trovo movimenti di rientro e eventualmente avviso se si vuole classificarli in automatico
               // for (String[] v : MappaCryptoWallet.values()) {
                for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
                    String v[]=MappaCryptoWallet.get(IDnc);
                    if (v.length > 30) {
                        String AddContratto = v[30];
                        if (AddressContratto != null && AddContratto != null
                                && !AddressContratto.isBlank() && !AddContratto.isBlank()
                                && AddressContratto.equalsIgnoreCase(AddContratto)
                                && !v[0].equals(ID)
                                && v[0].split("_")[4].equalsIgnoreCase("DC")
                                && v[11].equals(Moneta)
                                && v[18].isBlank()) {//questo serve per trovare solo i movimenti non ancora classificati
                            ListaIDMovimentiUguali.add(v[0]);
                            movimentimodificati++;
                        }

                    }
                
                
                
                
                
                
                
               if (!ListaIDMovimentiUguali.isEmpty()){
                    Descrizione = "TRASFERIMENTO DA PIATTAFORMA";
                    Dettaglio = "DTW - Trasferimento da Vault/Piattaforma a Rendita";

                        //Classifico tutti i movimenti di rientro
                        for (String IDMov:ListaIDMovimentiUguali){
                            CreaMovimentoTrasferimentoDaVault(IDMov,Descrizione,Dettaglio);
                        }


            }}
                return movimentimodificati;
    }
    
    public static void CreaMovimentoTrasferimentoAVault(String ID,String Descrizione,String Dettaglio){
        //Devo modificare il movimento originale + crearne uno nuovo
        String Movimento[]=MappaCryptoWallet.get(ID);
        //fase 1: creo il nuovo movimento
        String IDSpezzato[]=Movimento[0].split("_");
        String MonetaDettaglio=Movimento[8];
        if (!Movimento[25].isBlank()){
            MonetaDettaglio=Movimento[25];
        }
        String MT[]=new String[Importazioni.ColonneTabella];
        String IDNuovoMov=IDSpezzato[0]+"_"+IDSpezzato[1]+"_"+IDSpezzato[2]+"A_"+IDSpezzato[3]+"_DC";
        MT[0]=IDNuovoMov;
        MT[1]=Movimento[1];
        MT[2]="1 di 1";
        MT[3]=Movimento[3];
        MT[4]="Piattaforma/DeFi";
        MT[5]="TRASFERIMENTO A PIATTAFORMA";
        MT[6]="-> "+MonetaDettaglio;
        MT[11]=Movimento[8];
        MT[12]=Movimento[9];
        MT[13]=new BigDecimal(Movimento[10]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        MT[15]=Movimento[15];
        MT[18]="DTW - Trasferimento Interno";
        MT[20]=ID;
        MT[22]="AU";
        if (Movimento.length>29){
            MT[23]=Movimento[23];
            MT[24]=Movimento[24];
            MT[27]=Movimento[25];
            MT[28]=Movimento[26];
            MT[29]=Movimento[29];
        }
        Importazioni.RiempiVuotiArray(MT);
        MappaCryptoWallet.put(IDNuovoMov, MT);
        //fase 2: modifico il movimento originale aggiungendogli qualcosina
        Movimento[5]=Descrizione;
        Movimento[18]=Dettaglio;
        Movimento[20]=IDNuovoMov;

        
    }
    
    
    
    
    private void CreaMovimentiTrasferimentoDaVault(String ID,String Descrizione,String Dettaglio){
                   //controllo se ho movimenti simili e chiedo se voglio classificarli nella stessa maniera
            //poi creo movimento di deposito su Vault e movifico il movimento originale

            
            //FASE 1: recupero tutti i movimenti con la stessa moneta e stesso contratto
            String Movimento[]=MappaCryptoWallet.get(ID);
            long DataOraMovimento=OperazioniSuDate.ConvertiDatainLongMinuto(Movimento[1]);
            String Moneta=Movimento[11];
            String AddressContratto=null;
            long DataOra;
            if (Movimento.length>30)AddressContratto=Movimento[30];
            ArrayList<String> ListaIDMovimentiUguali = new ArrayList<>();
            boolean MovimentoOppostoNonClassificato=false;
            for (String[] v : MappaCryptoWallet.values()) {
                DataOra=OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
                if (v[0].split("_")[4].equalsIgnoreCase("PC")&&
                            v[8].equals(Moneta)&&
                            v[18].isBlank()&&
                        DataOraMovimento>=DataOra){
                        //se trovo movimenti di prelievo non classificati con data inferiore al movimento di deposito
                        //avviso e interrompo il ciclo, devo essere sicuro che tutti i movimenti precedenti siano stati inseriti
                        MovimentoOppostoNonClassificato=true; //Questo boolean serve appunto per interrompere il ciclo
                       // System.out.println("Arrivato Qua");
                        }
                if (v.length>30){
                    String AddContratto=v[30];
                    
                   /*     if (AddressContratto!=null&&AddContratto!=null&&
                        AddressContratto.equalsIgnoreCase(AddContratto)&&
                        !v[0].equals(ID)&&
                        v[0].split("_")[4].equalsIgnoreCase("DC")){
                               System.out.println("Arrivato Qua -"+AddressContratto+"-"+AddContratto);             
                                        }*/
                      // System.out.println(v[8]+"-"+Moneta);
                    if (AddressContratto!=null&&AddContratto!=null&&
                        !AddressContratto.isBlank()&&!AddContratto.isBlank()&&   
                        AddressContratto.equalsIgnoreCase(AddContratto)&&
                        !v[0].equals(ID)&&
                        v[0].split("_")[4].equalsIgnoreCase("DC")&&
                            v[11].equals(Moneta)&&
                        DataOraMovimento>=DataOra&&
                            v[18].length()<1){//questo serve per trovare solo i movimenti non ancora classificati
                        //Sotto questa if ci sono tutti i movimenti di deposito
                        //che riguardano la stessa moneta e lo stesso contratto
                        ListaIDMovimentiUguali.add(v[0]);                       
                    }
                    
                
                    
                    
                }
            }
            
            if (MovimentoOppostoNonClassificato){ 
                String Messaggio="Esistono movimenti di prelievo "+Moneta+" precedenti al movimento attuale non ancora classificati\n"
                        + "Procedere prima alla loro classificazione";
                JOptionPane.showConfirmDialog(this, Messaggio,
                    "Impossibile proseguire", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            }
            //FASE 2: Se trovo movimenti identici chiedo se voglio che anche questi siano classificati come Vault
            else if (!ListaIDMovimentiUguali.isEmpty()){
                String Messaggio="Sono stati trovati altri "+ListaIDMovimentiUguali.size()+" movimenti analoghi non ancora classificati, vuoi considerarli allo stesso modo?\n"
                        + "In caso di giacenza negativa sul Vault verrà creato un movimento correttivo per portare la giacenza a Zero\n"
                        + "(Questo movimento in positivo verrà considerato come fosse una reward)";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Movimenti analoghi", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        //FASE 3:modifico tutti i movimenti                        
                        for (String IDMov:ListaIDMovimentiUguali){
                            CreaMovimentoTrasferimentoDaVault(IDMov,Descrizione,Dettaglio);
                        }
                        CreaMovimentoTrasferimentoDaVault(ID,Descrizione,Dettaglio);
                        ModificaEffettuata=true;
                    }
                    case 1 -> {
                        //FASE 3:modifico il solo movimento interessato
                        CreaMovimentoTrasferimentoDaVault(ID,Descrizione,Dettaglio);
                        ModificaEffettuata=true;
                    }
                    case -1 -> {
                        //FASE 3:non modifico nulla
                        JOptionPane.showConfirmDialog(this, "Operazione Annullata",
                    "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                    default -> {
                    }
                }
               // System.out.println(risposta);
            }else{
                //FASE 3:modifico il solo movimento interessato
                CreaMovimentoTrasferimentoDaVault(ID,Descrizione,Dettaglio);
                ModificaEffettuata=true;  
            }
        if (ModificaEffettuata)            JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                    "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
               
    }
    
        public static void CreaMovimentoTrasferimentoDaVault(String ID,String Descrizione,String Dettaglio){
            

        //In questa funzione devo gestire le eventuali rewards aggiuntive ad esempio in altra moneta che rientrano assieme ai token di rientro
         //Per trovale e classificarle devo seguire le seguenti regole
         //1 - Cerco nella MappaCryptoWallet o meglio nella tabella depositiprelievi se ho movimenti con lo stesso hashtransazione del movimento analizzato
         //2 - Controllo se questo movimento è un movimento di deposito (DC)
         //3 - Controllo che questo movimento non sia già stato categorizzato
         //4 - Verifico che non si tratti lo stesso id del movimento che sto analizzando
         //Se soddisfa questi requisiti allora dovrò gestire il movimento come reward
         
         String Movimento[]=MappaCryptoWallet.get(ID);   
         String Moneta=Movimento[11];
         String HashT="";
         if (Movimento.length>29)HashT=Movimento[24];


        //In questa prima funzione devo gestire le eventuali rewards aggiuntive ad esempio in altra moneta che rientrano assieme ai token di rientro
         //Per trovale e classificarle devo seguire le seguenti regole
         //1 - Cerco nella MappaCryptoWallet o meglio nella tabella depositiprelievi se ho movimenti con lo stesso hashtransazione del movimento analizzato
         //2 - Controllo se questo movimento è un movimento di deposito (DC)
         //3 - Controllo che questo movimento non sia già stato categorizzato
         //4 - Verifico che non si tratti lo stesso id del movimento che sto analizzando
         //Se soddisfa questi requisiti allora dovrò gestire il movimento come reward
         
       for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
           String Mov[]=MappaCryptoWallet.get(IDnc);
           if (Mov.length>29){//Verifico che il movimento sia in defi
               String HashTnc=Mov[24];
               if (HashT.equalsIgnoreCase(HashTnc)&&!HashT.isBlank()&&//Verifico che abbiano lo stesso hash
                       Mov[0].split("_")[4].equals("DC")&&//Verifico che sia un movimento di deposito
                       !Mov[0].equals(Movimento[0])&&//Verifico che si tratti di un movimento diverso
                       Mov[18].isBlank())//Verifico che il movimento non sia già categorizzato
               {
                   //Se soddisfo tutti questi requisiti categorizzo il movimento come reward
                   Mov[5]="REWARD";
                   Mov[18]="DAI - Reward da Piattaforma Defi";
                   //Non lo lego ai movimenti originali ma lo lascio separato (Non compilo il campo [20])
               }
           }
           
       }// Fine prima funzione per la categorizzazione delle reward, ora passo alla successiva
         
         
        //Devo modificare il movimento originale + crearne uno nuovo
        
        //fase 1: creo il nuovo movimento
        String IDSpezzato[]=Movimento[0].split("_");
        String MonetaDettaglio=Movimento[11];
        if (!Movimento[27].isBlank()){
            MonetaDettaglio=Movimento[27];
        }
        String MT[]=new String[Importazioni.ColonneTabella];
        String IDNuovoMov=IDSpezzato[0]+"_"+IDSpezzato[1]+"_0"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_PC";
        MT[0]=IDNuovoMov;
        MT[1]=Movimento[1];
        MT[2]="1 di 1";
        MT[3]=Movimento[3];
        MT[4]="Piattaforma/DeFi";
        MT[5]="TRASFERIMENTO DA PIATTAFORMA";
        MT[6]=MonetaDettaglio+" ->";
        MT[8]=Movimento[11];
        MT[9]=Movimento[12];
        MT[10]=new BigDecimal(Movimento[13]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        MT[15]=Movimento[15];
        MT[18]="PTW - Trasferimento Interno";
        MT[22]="AU";
        if (Movimento.length>29){
            MT[23]=Movimento[23];
            MT[24]=Movimento[24];
            MT[25]=Movimento[27];
            MT[26]=Movimento[28];
            MT[29]=Movimento[29];
        }
        Importazioni.RiempiVuotiArray(MT);

        
        
         long DataOraMovimento=OperazioniSuDate.ConvertiDatainLongMinuto(Movimento[1]);
         long DataOra;
         BigDecimal QtaMovimentata=new BigDecimal(Movimento[13]);
         BigDecimal Somma=new BigDecimal(0);
        //faccio la somma dei movimenti del vault di questa moneta/wallet
        //Mi serve sapere la giacenza residua e vedere se sottraendo il movimento in uscita va o meno sotto zero
        //qualora vada sotto zero devo creare un movimento che compensi la cosa
        String CoppiaWalletVault=Movimento[3]+"Piattaforma/DeFi";   
        for (String[] v : MappaCryptoWallet.values()) {
            DataOra=OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
            String MonetaUscita=v[8];
            String QtaUscita=v[10];
            String MonetaEntrata=v[11];
            String QtaEntrata=v[13];
            //nella if devo mettere il limite sulla data
            if ((v[3]+v[4]).equals(CoppiaWalletVault)&&
                    DataOraMovimento>DataOra){
               if (MonetaUscita.equals(Moneta)){
                  Somma=Somma.add(new BigDecimal(QtaUscita));
               }
               if (MonetaEntrata.equals(Moneta)){
                  Somma=Somma.add(new BigDecimal(QtaEntrata)); 
               }
            }
        }
        BigDecimal QtaResiduaVault=Somma.subtract(QtaMovimentata);
        String IDMovCorrettivo="";
        if (QtaResiduaVault.compareTo(new BigDecimal(0))==-1){
            //!!!!Se la qta residua sul vault è minore di zero allora creo il nuovo movimento per sistemare la giacenza negativa del vault
        String MT2[]=new String[Importazioni.ColonneTabella];
        IDMovCorrettivo=IDSpezzato[0]+"_"+IDSpezzato[1]+"_00"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_DC";
        MT2[0]=IDMovCorrettivo;
        MT2[1]=Movimento[1];
        MT2[2]="1 di 1";
        MT2[3]=Movimento[3];
        MT2[4]="Piattaforma/DeFi";
        MT2[5]="REWARD";
        MT2[6]="-> "+MonetaDettaglio;
        MT2[11]=Movimento[11];
        MT2[12]=Movimento[12];
        MT2[13]=QtaResiduaVault.multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        MT2[15]=new BigDecimal(MT2[13]).divide(new BigDecimal(Movimento[13]),DecimaliCalcoli, RoundingMode.HALF_UP).multiply(new BigDecimal(Movimento[15])).setScale(2, RoundingMode.HALF_UP).toPlainString();
        MT2[18]="DAI - Reward";
        MT2[20]=ID+","+IDNuovoMov;
        MT2[22]="AU";
        if (Movimento.length>29){
            MT2[27]=Movimento[27];
            MT2[28]=Movimento[28];
            MT2[29]=Movimento[29];
        }
        Importazioni.RiempiVuotiArray(MT2);
        MappaCryptoWallet.put(IDMovCorrettivo, MT2);
        }
        MT[20]=ID+","+IDMovCorrettivo;
        MappaCryptoWallet.put(IDNuovoMov, MT);
        //fase 2: modifico il movimento originale aggiungendogli qualcosina
        Movimento[5]=Descrizione;
        Movimento[18]=Dettaglio;
        Movimento[20]=IDNuovoMov+","+IDMovCorrettivo;
        
    }
    
    public static void CreaMovimentiScambioCryptoDifferito(String IDPrelievo,String IDDeposito){
        //come prima cosa devo generare un nuovo id per il prelievo
        //System.out.println("Creo 3 movimenti");
        String MovimentoPrelievo[]=MappaCryptoWallet.get(IDPrelievo);
        String MovimentoDeposito[]=MappaCryptoWallet.get(IDDeposito);
                    //Rimuovo le movimentazioni perchè devo codificarle con altro id per metterle in ordine corretto
            //E' l'id infatti che da l'ordine alle transazioni
            //poi ricreo i movimenti ma con il nuovo ID
            Funzioni.RimuoviMovimentazioneXID(IDPrelievo);
            Funzioni.RimuoviMovimentazioneXID(IDDeposito);
        
       // BigDecimal QtaPrelievoValoreAssoluto=new BigDecimal(MovimentoPrelievo[10]).stripTrailingZeros().abs();
       // BigDecimal QtaDepositoValoreAssoluto=new BigDecimal(MovimentoDeposito[13]).stripTrailingZeros().abs();
        //Vado avanti solo se la qta prelevata è maggiore o uguale di quelòla ricevuta
     //   if (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)>=0){
            
        MovimentoPrelievo[5]="TRASFERIMENTO PER SCAMBIO";
        MovimentoDeposito[5]="TRASFERIMENTO PER SCAMBIO";

         
        //System.out.println(IDPrelievo);
        String IDTrasferimento1;
        String IDScambio;
        String IDTrasferimento2;
        String MT1[]=new String[Importazioni.ColonneTabella];
        String MS[]=new String[Importazioni.ColonneTabella];
        String MT2[]=new String[Importazioni.ColonneTabella];
        String IDSpezzato[]=IDPrelievo.split("_");
        MovimentoPrelievo[0]=IDSpezzato[0]+"_00"+IDSpezzato[1]+"_"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_"+IDSpezzato[4];
        IDTrasferimento1=IDSpezzato[0]+"_01"+IDSpezzato[1]+"_"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_DC";
        String IDSpezzatoDeposito[]=IDDeposito.split("_");
        IDScambio=IDSpezzatoDeposito[0]+"_02"+IDSpezzato[1]+"_"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_SC";
        IDTrasferimento2=IDSpezzatoDeposito[0]+"_03"+IDSpezzato[1]+"_"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_PC";
        MovimentoDeposito[0]=IDSpezzatoDeposito[0]+"_04"+IDSpezzatoDeposito[1]+"_"+IDSpezzatoDeposito[2]+"_"+IDSpezzatoDeposito[3]+"_"+IDSpezzatoDeposito[4];
        MT1[0]=IDTrasferimento1;
        MS[0]=IDScambio;
        MT2[0]=IDTrasferimento2;
        /*System.out.println(IDTrasferimento1);
        System.out.println(IDScambio);
        System.out.println(IDTrasferimento2);*/
        MT1[1]=MovimentoPrelievo[1];
        MS[1]=MovimentoDeposito[1];
        MT2[1]=MovimentoDeposito[1];
        MT1[2]="1 di 1";
        MS[2]="1 di 1";
        MT2[2]="1 di 1";
        MT1[3]=MovimentoPrelievo[3];
        MS[3]=MovimentoPrelievo[3];
        MT2[3]=MovimentoPrelievo[3];
        MT1[4]="Piattaforma di scambio";//da mettere defi se il movimento è in defi
        MS[4]="Piattaforma di scambio";//da mettere defi se il movimento è in defi
        MT2[4]="Piattaforma di scambio";//da mettere defi se il movimento è in defi
        MT1[5]="TRASFERIMENTO PER SCAMBIO";
        MS[5]="SCAMBIO CRYPTO";
        MT2[5]="TRASFERIMENTO PER SCAMBIO";
        String MonetaUscita=MovimentoPrelievo[8];
        if (!MovimentoPrelievo[25].isBlank())MonetaUscita=MovimentoPrelievo[25];
        String MonetaEntrata=MovimentoDeposito[11];
        if (!MovimentoDeposito[27].isBlank())MonetaEntrata=MovimentoDeposito[27];
        MT1[6]="-> "+MonetaUscita;
        MS[6]=MonetaUscita+" -> "+MonetaEntrata;
        MT2[6]=MonetaEntrata +" ->";
        
        MS[8]=MovimentoPrelievo[8];
        MT2[8]=MovimentoDeposito[11];        
        MS[9]=MovimentoPrelievo[9];
        MT2[9]=MovimentoDeposito[12];        
        MS[10]=MovimentoPrelievo[10];
        MT2[10]=new BigDecimal(MovimentoDeposito[13]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        
        MT1[11]=MovimentoPrelievo[8];
        MS[11]=MovimentoDeposito[11];
        MT1[12]=MovimentoPrelievo[9];
        MS[12]=MovimentoDeposito[12];
        MT1[13]=new BigDecimal(MovimentoPrelievo[10]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        MS[13]=MovimentoDeposito[13];

        MT1[15]=MovimentoPrelievo[15];
       // MS[15]=MovimentoDeposito[15];

            //Il prezzo del movimento di scambio deve essere uguale al valore della moneta ceduta nel caso sia passato tanto tempo
            //dal prelievo al deposito, mettiamo ad esempio il caso in cui metto degli USDT in una piattaforma in attesa che mi airdroppino
            //un nuovo token, nel momento in cui il token viene airdroppato viene calcolata l'eventuale plusvalenza sugli USDT ceduti
            //e il loro prezzo viene poi usato come costo di carico per il nuovo token acquistato.
            
            //Se è passato poco tempo invece significa che è semplicemente uno scambio che magari ci ha messo qualche minuto per arrivare,
            //in quel caso posso prendere il prezzo della transazione
            long DatalongDeposito=OperazioniSuDate.ConvertiDatainLongMinuto(MovimentoDeposito[1]);
            long DatalongPrelievo=OperazioniSuDate.ConvertiDatainLongMinuto(MovimentoPrelievo[1]);
            long DiffDate=DatalongDeposito-DatalongPrelievo;
            //System.out.println(DiffDate);
            //Moneta Uscita
            Moneta M1 = new Moneta();
            M1.InserisciValori(MovimentoPrelievo[8], MovimentoPrelievo[10], MovimentoPrelievo[26], MovimentoPrelievo[9]);
            //Moneta Entrata
            /*Moneta M2 = new Moneta();
            M2.InserisciValori(MovimentoDeposito[11], MovimentoDeposito[13], MovimentoPrelievo[28], MovimentoPrelievo[12]);*/
            String Prezzo;
            String Rete=Funzioni.TrovaReteDaID(MovimentoPrelievo[0]);
            Prezzo=Prezzi.DammiPrezzoTransazione(M1, null, DatalongDeposito, "0", true, 3, Rete);
            //Se la differenza è inferiore ai 5 minuti allora posso prendere sia il prezzo del token uscito che di quello entrato
            if (DiffDate<300000){
                // Se il prezzo è uguale a zero allora prendo il prezzo di deposito se esiste                
                if(Prezzo.equals("0.00")){
                    Prezzo=MovimentoDeposito[15];
                }
                /*else if(!MovimentoDeposito[32].equalsIgnoreCase("Si")){
                    MovimentoDeposito[15]=Prezzo;
                }*/
            }
            if(Prezzo.equals("0.00")){
                //Se il prezzo è 0.00 vuol dire che la crypto è senza prezzo
                MS[32]="No";
            }
            MS[15]=new BigDecimal(Prezzo).setScale(2,RoundingMode.HALF_UP).toPlainString();
        
        MT2[15]=MovimentoDeposito[15];
        
        MovimentoPrelievo[18]="PTW - Scambio Differito";
        MT1[18]="DTW - Trasferimento Interno";
        MT2[18]="PTW - Trasferimento Interno";
        MovimentoDeposito[18]="DTW - Scambio Differito"; 
        
        MovimentoPrelievo[20]=IDTrasferimento1+","+IDScambio+","+IDTrasferimento2+","+MovimentoDeposito[0];
        MT1[20]=MovimentoPrelievo[0]+","+IDScambio+","+IDTrasferimento2+","+MovimentoDeposito[0];
        MS[20]=MovimentoPrelievo[0]+","+IDTrasferimento1+","+IDTrasferimento2+","+MovimentoDeposito[0];
        MT2[20]=MovimentoPrelievo[0]+","+IDTrasferimento1+","+IDScambio+","+MovimentoDeposito[0];
        MovimentoDeposito[20]=MovimentoPrelievo[0]+","+IDTrasferimento1+","+IDScambio+","+IDTrasferimento2;
        
        MT1[22]="AU";
        MS[22]="AU";
        MT2[22]="AU";
        
        if (MovimentoDeposito.length>29){ 
            MS[28]=MovimentoDeposito[28];//Entra
            MS[27]=MovimentoDeposito[27];
            MT2[26]=MovimentoDeposito[28];//Esce
            MT2[25]=MovimentoDeposito[27];
            MS[29]=MovimentoDeposito[29];
            MT2[29]=MovimentoDeposito[29];     
          //  System.out.println(MovimentoDeposito[27]+" - "+MovimentoDeposito[28]);
        }
        if (MovimentoPrelievo.length>29){ 
            MT1[28]=MovimentoPrelievo[26];//Entra
            MT1[27]=MovimentoPrelievo[25];
            MS[26]=MovimentoPrelievo[26];//Esce
            MS[25]=MovimentoPrelievo[25];
            MS[29]=MovimentoPrelievo[29];
            MT1[29]=MovimentoPrelievo[29];  
        //    System.out.println(MovimentoPrelievo[25]+" - "+MovimentoPrelievo[26]);
        }

            Importazioni.RiempiVuotiArray(MT1);
            Importazioni.RiempiVuotiArray(MS);
            Importazioni.RiempiVuotiArray(MT2); 

            

            MappaCryptoWallet.put(MovimentoPrelievo[0], MovimentoPrelievo);
            MappaCryptoWallet.put(IDTrasferimento1, MT1);
            MappaCryptoWallet.put(IDScambio, MS);
            MappaCryptoWallet.put(IDTrasferimento2, MT2);
            MappaCryptoWallet.put(MovimentoDeposito[0], MovimentoDeposito);

      //   }

        
        
                
    }    
    
    
    private void ComboBox_TipoMovimentoItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_TipoMovimentoItemStateChanged
        // TODO add your handling code here:
        
       
        int scelta=ComboBox_TipoMovimento.getSelectedIndex();
        if (evt.getStateChange() == ItemEvent.SELECTED){
       // if (evt.getItem().toString().equals(ComboBox_TipoMovimento.getSelectedItem().toString())){
         //    System.out.println("cambio "+scelta);
        String descrizione,dettaglio;
                if (IDTrans.split("_")[4].equalsIgnoreCase("DC")){
          //in questo caso sono in presenza di un movimento di deposito
          switch(scelta){
              case 1 -> {
                  descrizione="AIRDROP o SIMILARE";
                  dettaglio="DAI - Airdrop,Cashback,Rewards etc..";
                  TransferNO();
                }
              case 2 -> {
                  descrizione="DEPOSITO CRYPTO";
                  dettaglio="DCZ - Deposito a costo zero (no plusvalenza)";
                  TransferNO();
                }
              case 3 -> {
                  descrizione="TRASFERIMENTO TRA WALLET";
                  dettaglio="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                  TransferSI();
                }
              case 4 -> {
                    descrizione = "ACQUISTO CRYPTO";
                    dettaglio = "DAC - Acquisto Crypto";
                    TransferNO();

                }
              case 5 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    dettaglio = "DSC - Scambio Crypto Differito";
                    TransferSI();
                 //   System.out.println("Scambio");

                }
              default -> {descrizione="DEPOSITO CRYPTO";
               TransferNO();}
                  //qui si va solo in caso la scelata sia nessuna
          }
        }else{
          //in questo caso sono in presenza di un movimento di prelievo
          switch(scelta){
              case 1 -> {
                  descrizione="CASHOUT o SIMILARI";
                  dettaglio="PCO - Cashout, acquisti con crypto etc.. (plusvalenza)";
                  TransferNO();
                }
              case 2 -> {
                  descrizione="PRELIEVO CRYPTO";
                  dettaglio="PWN - Tolgo dai calcoli delle medie (no plusvalenza)";
                  TransferNO();
                }
              case 3 -> {
                  descrizione="TRASFERIMENTO TRA WALLET";
                  dettaglio="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                  TransferSI();
                }
              case 4 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    dettaglio = "PSC - Scambio Crypto Differito";
                    TransferSI();
                   // System.out.println("Scambio");

                }
              default -> {descrizione="PRELIEVO CRYPTO";
              TransferNO();}
          }
        }
        }
    }//GEN-LAST:event_ComboBox_TipoMovimentoItemStateChanged

    private void ComboBox_EscursioneMassimaPercentualeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_EscursioneMassimaPercentualeItemStateChanged
        // TODO add your handling code here:
     if (evt.getStateChange() == ItemEvent.SELECTED) {
       // Object selectedItem = evt.getItem();
        ComboBox_TipoMovimentoItemStateChanged(evt);
      //  System.out.println("Elemento selezionato: " + selectedItem);
    }
           

    }//GEN-LAST:event_ComboBox_EscursioneMassimaPercentualeItemStateChanged

    private void ComboBox_EscursioneMassimaGiorniItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_EscursioneMassimaGiorniItemStateChanged
        // TODO add your handling code here:
        if (evt.getStateChange() == ItemEvent.SELECTED) {
       // Object selectedItem = evt.getItem();
        ComboBox_TipoMovimentoItemStateChanged(evt);
      //  System.out.println("Elemento selezionato: " + selectedItem);
    }
    }//GEN-LAST:event_ComboBox_EscursioneMassimaGiorniItemStateChanged
                                      
    private void TransferSI(){
                  Tabella_MovimentiAbbinati.setEnabled(true);
                  jLabel3.setEnabled(true);
                  Tabelle.ColoraRigheTabellaCrypto(Tabella_MovimentiAbbinati);
                  CompilaTabellaMovimetiAssociabili(IDTrans);
    }
    private void TransferNO(){
                  Tabella_MovimentiAbbinati.setEnabled(false);
                  jLabel3.setEnabled(false);
                  Tabelle.ColoraTabelladiGrigio(Tabella_MovimentiAbbinati);
    }
    
    public static String RiportaTransazioniASituazioneIniziale(String IDPartiConvolte[],String IDOri){
        String IDRitorno=IDOri;
        BigDecimal QtaCommissione=null;
        BigDecimal QtaReward=null;
        BigDecimal PrezzoCommissione=null;
        BigDecimal PrezzoReward=null;

        for (String ID:IDPartiConvolte){
           // System.out.println(ID);
            //come prima cosa cerco movimenti AU classificati come CM (Commissione)
            //Se lo trovo recupero i dati che mi servono e cancello la transazione
            //i dati sono la qta e il prezzo che dovrò andare a sommarli al movimento di prelievo per far tornare tutto alla situazione originale
            String attuale[]=MappaCryptoWallet.get(ID);
            if (ID.split("_")[4].equalsIgnoreCase("CM")&&attuale[22].equalsIgnoreCase("AU")){
                QtaCommissione=new BigDecimal(attuale[10]);
                PrezzoCommissione=new BigDecimal(attuale[15]);
                MappaCryptoWallet.remove(ID);
            }else if (ID.split("_")[4].equalsIgnoreCase("RW")&&attuale[22].equalsIgnoreCase("AU")){
                QtaReward=new BigDecimal(attuale[13]);
                PrezzoReward=new BigDecimal(attuale[15]);
                MappaCryptoWallet.remove(ID);
            }else if (attuale[22].equalsIgnoreCase("AU")){
                MappaCryptoWallet.remove(ID);
            }
        }

        for (String ID : IDPartiConvolte) {

            String attuale[] = MappaCryptoWallet.get(ID);
            if (attuale != null) {
                
                //Adesso restano i movimenti che non sono stati generati automaticamente
                if (ID.split("_")[4].equalsIgnoreCase("DC")) {

                    attuale[5] = "DEPOSITO CRYPTO";
                   // System.out.println("Deposito "+attuale[18]+" - "+attuale[19]);

                } else if (ID.split("_")[4].equalsIgnoreCase("PC")) {
                    //Essendo questo il prelievo devo farci delle modifiche sopra
                    //ovvero sistemare un pò di qta e prezzo transazione (posizione 10 e 15)

                    attuale[5] = "PRELIEVO CRYPTO";
                    
                   // System.out.println("Prelievo "+attuale[18]+" - "+attuale[19]);
                    if (QtaCommissione != null) {
                        attuale[10] = new BigDecimal(attuale[10]).add(QtaCommissione).toPlainString();
                        attuale[15] = new BigDecimal(attuale[15]).add(PrezzoCommissione).toPlainString();
                    }
                    if (QtaReward != null) {
                        attuale[10] = new BigDecimal(attuale[10]).add(QtaReward).toPlainString();
                        attuale[15] = new BigDecimal(attuale[15]).subtract(PrezzoReward).toPlainString();
                       // attuale[15] = new BigDecimal(attuale[15]).add(PrezzoCommissione).toPlainString();
                    }

                }
                String Old18=attuale[18].trim();
                attuale[18] = "";
                attuale[19] = "";
                attuale[20] = "";//Questi sono i movimenti associati che ovvimante vanno annullati
                attuale[31] = "";//data e ora di fine trasferimento
                
                //Adesso controllo se è un movimento di scambio differito fatto con la nuova funzione
                //Ovvero la seconda parte dell'ID deve iniziare con 00 o con 04
                String[] IDSplittato = attuale[0].split("_");
                if ((Old18.equalsIgnoreCase("PTW - Scambio Differito")
                        || Old18.equalsIgnoreCase("DTW - Scambio Differito"))
                        && (IDSplittato[1].startsWith("00") || IDSplittato[1].startsWith("04"))) 
                {
                    //Appurato che è uno scambio differito gestito con il nuovo metodo ripristo l'id originale al movimento
                    
                    //Recupero ID Originale
                    IDSplittato[1] = IDSplittato[1].substring(2);
                    attuale[0] = String.join("_", IDSplittato);
                    //Cancello movimento con id modificato
                    MappaCryptoWallet.remove(ID);
                    //Se questo è il movimento che sto analizzando cambio anche l'IDTrans che indica appunto il movimento analizzato
                    //non lo facessi poi la funzione andrebbe in errore perchè cercherebbe un movimento che non esiste più.
                    if (ID.equals(IDOri))IDRitorno=attuale[0];
                    //System.out.println("Rimuovo "+ID);
                    //Ricreo il movimenti con l'id Corretto
                    MappaCryptoWallet.put(attuale[0], attuale);

                }
            }
        }
        return IDRitorno;
    }

    
    

    public void CompilaTabellaMovimetiAssociabili(String ID) {
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.Tabella_MovimentiAbbinati.getModel();
        Tabelle.Funzioni_PulisciTabella(ModelloTabellaDepositiPrelievi);
        //Tabelle.ColoraRigheTabellaCrypto(jTable2);
        String attuale[] = MappaCryptoWallet.get(ID);
        long DataOraAttuale = OperazioniSuDate.ConvertiDatainLongMinuto(attuale[1]);
        String TipoMovimentoAttuale = attuale[0].split("_")[4].trim();
        String WalletAttuale=attuale[3]+attuale[4];
        String TipoMovimentoRichiesto;
        String MonetaAttuale;
        BigDecimal QtaAttuale;
        long Giorni=Long.parseLong(ComboBox_EscursioneMassimaGiorni.getSelectedItem().toString())*86400000;
        String Escursione=ComboBox_EscursioneMassimaPercentuale.getSelectedItem().toString();
        BigDecimal escursioneMassima = new BigDecimal(Escursione);
       // System.out.println(ComboBox_TipoMovimento);
       
       //1 - TRASMERIMENTI TRA WALLET
        if (ComboBox_TipoMovimento.getSelectedItem()!=null&&ComboBox_TipoMovimento.getSelectedItem().toString().contains("TRASFERIMENTO TRA WALLET")) {
            //Permetto di classificare movimenti in cui il deposito è più alto del prelievo solo se la differenza è inferiore allo 0,1%
            BigDecimal EscursioneContraria=new BigDecimal(0.1);
            BigDecimal QtaAttualeMax;
            BigDecimal QtaAttualeMin;
            if (TipoMovimentoAttuale.equalsIgnoreCase("PC")) {
                MonetaAttuale = attuale[8].trim();
                QtaAttuale = new BigDecimal(attuale[10]).stripTrailingZeros();
                TipoMovimentoRichiesto = "DC";
                QtaAttualeMin = QtaAttuale.subtract(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();
                QtaAttualeMax = QtaAttuale.add(QtaAttuale.multiply(EscursioneContraria).divide(new BigDecimal(100))).abs();
            } else {
                MonetaAttuale = attuale[11].trim();
                QtaAttuale = new BigDecimal(attuale[13]).stripTrailingZeros();
                TipoMovimentoRichiesto = "PC";
                QtaAttualeMin = QtaAttuale.subtract(QtaAttuale.multiply(EscursioneContraria).divide(new BigDecimal(100))).abs();
                QtaAttualeMax = QtaAttuale.add(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();
            }
            
          //  BigDecimal QtaAttualeMax = QtaAttuale.add(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();
          //  BigDecimal QtaAttualeMin = QtaAttuale.subtract(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();

            // a questo punto in tabella metto le righe che soddisfano le seguenti condizioni
            //1 - La moneta deve essere la stessa
            //2 - Il movimento deve essere opposto
            //3 - Qta deve essere compreso tra qtaMax e QtaMin che sono un 5%
            //4 - il movimento deve essere fatto nel giro di max 24 ore dopo quello analizzata e massimo 24 ore prima
            //5 - La qta uscita non deve essere ami inferiore alla qta ricevuta
            //6 - Exchange+Wallet non deve essere lo stesso (DA FARE!!!!!!!!!!!!!!!!)
            
            for (String[] v : MappaCryptoWallet.values()) {
                String TipoMovimento = v[0].split("_")[4].trim();
                if (v[22]!=null&&!v[22].equalsIgnoreCase("AU")&&TipoMovimento.equalsIgnoreCase(TipoMovimentoRichiesto)) {
                    String WalletRiferimento=v[3]+v[4];
                    BigDecimal Qta;
                    BigDecimal QtanoABS;
                    //BigDecimal DiffPrec;
                    String Moneta;
                    long DataOra = OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
                    long DataMinima=DataOraAttuale- 86400000;
                    long DataMassima=DataOraAttuale+ 86400000;
                    if (TipoMovimento.equalsIgnoreCase("DC"))
                    {
                        Qta = new BigDecimal(v[13]).abs();
                        QtanoABS = new BigDecimal(v[13]);
                       /* if (Funzioni.isBigDecimalNonZero(v[13])){
                            DiffPrec = QtaAttuale.divide(new BigDecimal(v[13]),10,RoundingMode.HALF_UP).subtract(BigDecimal.ONE).abs();
                        }else DiffPrec = null;*/
                        Moneta = v[11].trim();

                    } else// if (TipoMovimento.equalsIgnoreCase("PC"))
                    {                       
                        Qta = new BigDecimal(v[10]).abs();
                        QtanoABS = new BigDecimal(v[10]);
                       /* if (Funzioni.isBigDecimalNonZero(v[10])){
                            DiffPrec = QtaAttuale.divide(new BigDecimal(v[10]),10,RoundingMode.HALF_UP).subtract(BigDecimal.ONE).abs();
                        }
                        else DiffPrec = null;*/
                        Moneta = v[8].trim();
                    }

                    if (Qta != null &&/* DiffPrec != null && */Moneta != null
                            && MonetaAttuale.equals(Moneta)
                            && Qta.compareTo(QtaAttualeMax) == -1 && Qta.compareTo(QtaAttualeMin) == 1
                            && DataOra < (DataMassima)
                            && DataOra > (DataMinima)
                            && !WalletRiferimento.equals(WalletAttuale)
                           // && DiffPrec.compareTo(new BigDecimal(1)) <= 0
                            ) 
                    {
                        String riga[] = new String[8];
                        riga[0] = v[0];
                        riga[1] = v[1];
                        riga[2] = v[3];
                        riga[3] = v[5];
                        riga[4] = Moneta;
                        riga[5] = QtanoABS.stripTrailingZeros().toPlainString();
                        riga[6] = v[18];
                        riga[7] = v[15];
                        
                        ModelloTabellaDepositiPrelievi.addRow(riga);

                    }

                    //   ModelloTabellaDepositiPrelievi.addRow(riga); 
                }
            }

            
            //PARTE RELATIVA ASCAMBIO DIFFERITO
        }else if (ComboBox_TipoMovimento.getSelectedItem()!=null&&ComboBox_TipoMovimento.getSelectedItem().toString().contains("SCAMBIO CRYPTO DIFFERITO")) {
           // System.out.println("Differito");
           
           long DataMinima;
           long DataMassima;
           //long Mese=Long.parseLong("2592000000");
            if (TipoMovimentoAttuale.equalsIgnoreCase("PC")) {
                MonetaAttuale = attuale[8].trim();
              //  QtaAttuale = new BigDecimal(attuale[10]).stripTrailingZeros();
                TipoMovimentoRichiesto = "DC";
                    DataMinima=DataOraAttuale-Giorni;
                    DataMassima=DataOraAttuale+ 86400000;
            } else {
                MonetaAttuale = attuale[11].trim();
              //  QtaAttuale = new BigDecimal(attuale[13]).stripTrailingZeros();
                TipoMovimentoRichiesto = "PC";
                DataMinima=DataOraAttuale- 86400000;
                DataMassima=DataOraAttuale+Giorni;
            }
            //In questo caso i movimenti per essere accettati bisogna che abbiano le seguenti carattaristiche
            //1 - Devono essere del tipo movimento opposto a quello del movimento evidenziato
            //2 - Devono essere di una moneta con nome differente
            //3 - Non deve essere passato più di 1 mese 
            //(Metto un mese per gestire anche casi come GMR in cui i soldi li caricavi in una piattaforma e quando pronto il cambio di contratto ti giravano i nuovi token)
            for (String[] v : MappaCryptoWallet.values()) {
               // System.out.println("Differito");
                String TipoMovimento = v[0].split("_")[4].trim();
                if (v[22]!=null&&!v[22].equalsIgnoreCase("AU")&&TipoMovimento.equalsIgnoreCase(TipoMovimentoRichiesto)) {
                    
                    BigDecimal QtanoABS = null;
                    String Moneta = null;
                    long DataOra = 0;
                    
                    if (TipoMovimento.equalsIgnoreCase("DC"))
                    {
                        QtanoABS = new BigDecimal(v[13]);
                        Moneta = v[11].trim();
                        DataOra = OperazioniSuDate.ConvertiDatainLongMinuto(v[1])-Giorni;

                    } else if (TipoMovimento.equalsIgnoreCase("PC"))
                    {
                        QtanoABS = new BigDecimal(v[10]);
                        DataOra = OperazioniSuDate.ConvertiDatainLongMinuto(v[1])+Giorni;
                        Moneta = v[8].trim();
                    }
                    if (Moneta != null && QtanoABS!=null
                            && !MonetaAttuale.equalsIgnoreCase(Moneta)
                            && DataOra <= (DataMassima)
                            && DataOra >= (DataMinima)
                            ) 
                    {
                                            
                        String riga[] = new String[8];
                        riga[0] = v[0];
                        riga[1] = v[1];
                        riga[2] = v[3];
                        riga[3] = v[5];
                        riga[4] = Moneta;
                        riga[5] = QtanoABS.stripTrailingZeros().toPlainString();
                        riga[6] = v[18];
                        riga[7] = v[15];
                        ModelloTabellaDepositiPrelievi.addRow(riga);                 
                    
                    }
                }}
            
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
            java.util.logging.Logger.getLogger(ClassificazioneTrasf_Modifica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClassificazioneTrasf_Modifica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClassificazioneTrasf_Modifica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClassificazioneTrasf_Modifica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClassificazioneTrasf_Modifica(IDTrans).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Annulla;
    private javax.swing.JButton Bottone_OK;
    private javax.swing.JComboBox<String> ComboBox_EscursioneMassimaGiorni;
    private javax.swing.JComboBox<String> ComboBox_EscursioneMassimaPercentuale;
    private javax.swing.JComboBox<String> ComboBox_TipoMovimento;
    private javax.swing.JTable Tabella_MovimentiAbbinati;
    private javax.swing.JTextArea TextArea_Note;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel_EscursioneMassima;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
