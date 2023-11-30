/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import static giacenze_crypto.com.CDC_Grafica.Funzioni_Tabelle_PulisciTabella;
import java.math.RoundingMode;

/**
 *
 * @author luca.passelli
 */
public class ClassificazioneTrasf_Modifica extends javax.swing.JDialog {

    /**
     * Creates new form ClassificazioneTrasf_Modifica
     */
    //CLASSIFICAZIONE TRASFERIMENTI
    
    //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
    //PCO -> Cashout o similare (prelievo)
    //PTW -> Trasferimento tra Wallet (prelievo)
    /////////////PSC -> Scambio Crypto Differito (Scambio crypto non simultaneo ma differito nel tempo) (Non Utilizzato per ora)
    //DTW -> Trasferimento tra Wallet (deposito)
    //DAI -> Airdrop o similare (deposito)
    //DCZ -> Costo di carico 0 (deposito)
    //DAC -> Acquisto Crypto (deposito)   
   //////////// //DSC -> Scambio Crypto Differito (Scambio crypto non simultaneo ma differito nel tempo) (Non Utilizzato per ora)

    
    static String IDTrans="";
    boolean ModificaEffettuata=false;
    public ClassificazioneTrasf_Modifica(String ID) {
        ModificaEffettuata=false;
        IDTrans=ID;
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.jTable1.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaDepositiPrelievi);
        Tabelle.ColoraRigheTabellaCrypto(jTable1);
        String riga[]=DammiRigaTabellaDaID(ID);
        ModelloTabellaDepositiPrelievi.addRow(riga);
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
            TransferSI();
        } else if (tipomov.equalsIgnoreCase("DTW")) {
            ntipo = 3;
            if (riga[6].contains("Scambio"))ntipo=5;
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
                "AIRDROP O SIMILARI (verrà calcolata la plusvalenza)",
                "DEPOSITO CON COSTO DI CARICO A ZERO",
                "TRASFERIMENTO TRA WALLET DI PROPRIETA' (bisognerà selezionare il movimento di prelievo nella tabella sotto)",
                "ACQUISTO CRYPTO (Tramite contanti,servizi esterni etc...)",
                "SCAMBIO CRYPTO DIFFERITO"};

        }else
        {
            papele=new String[]{"- nessuna selezione -",
                "CASHOUT O SIMILARE (verrà calcolata la plusvalenza)",
                "PRELIEVO SCONOSCIUTO (qta e valore verrà tolta dal calcolo della Plus con LIFO)",
                "TRASFERIMENTO TRA WALLET DI PROPRIETA' (bisognerà selezionare il movimento di deposito nella tabella sotto)",
                "SCAMBIO CRYPTO DIFFERITO"};

        }
            ArrayList<String> elements = new ArrayList<>();
            elements.addAll(java.util.Arrays.asList(papele));
            ComboBoxModel model = new DefaultComboBoxModel(elements.toArray());
            this.ComboBox_TipoMovimento.setModel(model);
            this.ComboBox_TipoMovimento.setSelectedIndex(ntipo);
            String v[]=MappaCryptoWallet.get(ID);
            TextArea_Note.setText(v[21].replace("<br>" ,"\n"));
        //    CompilaTabellaMovimetiAssociabili(ID);

        
        
    }
  
    
    
    public boolean getModificaEffettuata(){
        return ModificaEffettuata;
    }
    
    public String[] DammiRigaTabellaDaID(String ID){
          String v[]=MappaCryptoWallet.get(ID);
          String TipoMovimento=v[0].split("_")[4].trim();

            String riga[]=new String[7];
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Dettaglio Movimento:");

        jLabel2.setText("Scegli la tipologia di movimento dalla lista :");

        ComboBox_TipoMovimento.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ComboBox_TipoMovimento.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_TipoMovimentoItemStateChanged(evt);
            }
        });

        Bottone_OK.setText("OK");
        Bottone_OK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_OKActionPerformed(evt);
            }
        });

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
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Data e Ora", "Exchange/Wallet", "Tipo", "Moneta", "Qta"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
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
            jTable1.getColumnModel().getColumn(4).setMaxWidth(60);
            jTable1.getColumnModel().getColumn(5).setMinWidth(100);
            jTable1.getColumnModel().getColumn(5).setPreferredWidth(100);
            jTable1.getColumnModel().getColumn(5).setMaxWidth(100);
        }

        Tabella_MovimentiAbbinati.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Data e Ora", "Exchange/Wallet", "Tipo", "Moneta", "Qta"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
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
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(4).setMaxWidth(60);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(5).setMinWidth(100);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(5).setPreferredWidth(100);
            Tabella_MovimentiAbbinati.getColumnModel().getColumn(5).setMaxWidth(100);
        }

        jLabel4.setText("Note :");

        TextArea_Note.setColumns(20);
        TextArea_Note.setRows(5);
        jScrollPane1.setViewportView(TextArea_Note);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ComboBox_TipoMovimento, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(Bottone_OK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Annulla))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 705, Short.MAX_VALUE)
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_Annulla)
                    .addComponent(Bottone_OK))
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
        int scelta = this.ComboBox_TipoMovimento.getSelectedIndex();
        boolean completato=true;
        String descrizione = "";
        String dettaglio = "";
        //String Note=jTextField1.getText();
        String Note=TextArea_Note.getText().replace("\n", "<br>");
        String attuale[] = MappaCryptoWallet.get(IDTrans);
        String PartiCoinvolte[] = (IDTrans+","+attuale[20]).split(",");
        String PrzCarico="Da calcolare";
        String plusvalenza="Da calcolare";
        boolean trasferimento = false;
        if (IDTrans.split("_")[4].equalsIgnoreCase("DC")) {
            //in questo caso sono in presenza di un movimento di deposito
            switch (scelta) {
                case 1 -> {
                    descrizione = "AIRDROP o SIMILARE";
                    dettaglio = "DAI - Airdrop,Cashback,Rewards etc.. (plusvalenza)";
                    plusvalenza=attuale[15];
                    PrzCarico=attuale[15];
                }
                case 2 -> {
                    descrizione = "DEPOSITO CRYPTO (a costo zero)";
                    dettaglio = "DCZ - Deposito a costo zero (no plusvalenza)";
                    PrzCarico="0.00";
                    plusvalenza="0.00";
                }
                case 3 -> {
                    descrizione = "TRASFERIMENTO TRA WALLET";
                    dettaglio = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    plusvalenza="0.00";
                    trasferimento = true;
                }
                case 4 -> {
                    descrizione = "ACQUISTO CRYPTO";
                    dettaglio = "DAC - Acquisto Crypto";
                    String m = JOptionPane.showInputDialog(this,"Indica il valore di acquisto corretto in Euro : ",attuale[15]);
                    completato = m!=null; //se premo annulla nel messaggio non devo poi chiudere la finestra, quindi metto completato=false
                    if (m!=null){
                        m=m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (CDC_Grafica.Funzioni_isNumeric(m, false))
                    {
                        attuale[15]=m;
                        PrzCarico=attuale[15];
                        plusvalenza="0.00";
                    }else
                    {
                        completato=false;
                        JOptionPane.showConfirmDialog(this, "Attenzione, "+m+" non è un numero valido!",
                    "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                    }
                    

                }
                case 5 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    trasferimento = true;

                }
                default ->{
                 /*   descrizione = "DEPOSITO CRYPTO";
                    System.out.println(attuale[9]);
                    System.out.println(attuale[12]);
                    System.out.println(Importazioni.RitornaTipologiaTransazione(attuale[9], attuale[12],1));*/
                    descrizione=Importazioni.RitornaTipologiaTransazione(null, attuale[12],1);
                    }
                //qui si va solo in caso la scelata sia nessuna
            }
        } else {
            //in questo caso sono in presenza di un movimento di prelievo
            switch (scelta) {
                case 1 -> {
                    descrizione = "CASHOUT o SIMILARI";
                    dettaglio = "PCO - Cashout, acquisti con crypto etc.. (plusvalenza)";
                }
                case 2 -> {
                    descrizione = "PRELIEVO CRYPTO (tolgo dai calcoli)";
                    dettaglio = "PWN - Tolgo dai calcoli delle medie (no plusvalenza)";
                    plusvalenza="0";
                }
                case 3 -> {
                    descrizione = "TRASFERIMENTO TRA WALLET";
                    dettaglio = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    plusvalenza="0";
                    trasferimento = true;
                }
                case 4 -> {
                    descrizione = "SCAMBIO CRYPTO DIFFERITO";
                    trasferimento = true;
                }
                default ->
                    //descrizione = "PRELIEVO CRYPTO";
                    descrizione=Importazioni.RitornaTipologiaTransazione(attuale[9], null,1);
            }
        }

        if (PartiCoinvolte.length>1) {
            //se controparte non è vuota vado ad eliminare l'associazione anche al movimento associato
            //a cancellare le eventuali commissioni e riportare i prezzi e qta allo stato originale
            RiportaTransazioniASituazioneIniziale(PartiCoinvolte);
        }
        if (completato)
        if (!trasferimento) {
            attuale[5] = descrizione;
            attuale[17] = PrzCarico;
            attuale[18] = dettaglio;
            attuale[19] = plusvalenza;
            attuale[20] = "";
            attuale[21] = Note;
            //in teoria avendo preso l'oggetto e modificandone il contenuto non serve questa seconda parte
            MappaCryptoWallet.put(IDTrans, attuale);
            JOptionPane.showConfirmDialog(this, "Modifiche effettuate, ricordarsi di Salvare!! (sezione Transazioni Crypto)",
                    "Modifiche fatte!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            ModificaEffettuata = true;
            this.dispose();
            } else {

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
                            }
                        } else {
                            IDDeposito = IDTransazioneControparte;
                            IDPrelievo = IDTrans;
                            //creo movimento di commissione e valori
                            if (descrizione.equalsIgnoreCase("TRASFERIMENTO TRA WALLET")) {
                                CreaMovimentiTrasferimentosuWalletProprio(IDPrelievo, IDDeposito);
                            } else if (descrizione.equalsIgnoreCase("SCAMBIO CRYPTO DIFFERITO")) {
                                CreaMovimentiScambioCryptoDifferito(IDPrelievo, IDDeposito);
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


    }//GEN-LAST:event_Bottone_OKActionPerformed

    
    public static void CreaMovimentiTrasferimentosuWalletProprio(String IDPrelievo,String IDDeposito){
        //come prima cosa devo generare un nuovo id per il prelievo
       // System.out.println("Creo commissione");
        String MovimentoPrelievo[]=MappaCryptoWallet.get(IDPrelievo);
        String MovimentoDeposito[]=MappaCryptoWallet.get(IDDeposito);
        BigDecimal QtaPrelievoValoreAssoluto=new BigDecimal(MovimentoPrelievo[10]).stripTrailingZeros().abs();
        BigDecimal QtaDepositoValoreAssoluto=new BigDecimal(MovimentoDeposito[13]).stripTrailingZeros().abs();
        //Vado avanti solo se la qta prelevata è maggiore o uguale di quelòla ricevuta
        if (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)>=0){
            
        MovimentoPrelievo[5]="TRASFERIMENTO TRA WALLET";
        MovimentoDeposito[5]="TRASFERIMENTO TRA WALLET";
        MovimentoPrelievo[18]="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        MovimentoDeposito[18]="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)"; 
        
        String IDCommissione;
        String QtaCommissione;
        String ValoreTransazione;
        String MovimentoCommissione[]=new String[Importazioni.ColonneTabella];
        String IDPrelivoSpezzato[]=IDPrelievo.split("_");
        int numTransazione=Integer.parseInt(IDPrelivoSpezzato[2])+1;
        IDCommissione=IDPrelivoSpezzato[0]+"_"+IDPrelivoSpezzato[1]+"_"+numTransazione+"_1_CM";
        QtaCommissione=new BigDecimal(MovimentoPrelievo[10]).abs().subtract(new BigDecimal(MovimentoDeposito[13]).abs()).toPlainString();
        ValoreTransazione=new BigDecimal(MovimentoPrelievo[15]).abs().divide(new BigDecimal(MovimentoPrelievo[10]).abs(),15, RoundingMode.HALF_UP).multiply(new BigDecimal(QtaCommissione)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
        MovimentoCommissione[0]=IDCommissione;
        MovimentoCommissione[1]=MovimentoPrelievo[1];
        MovimentoCommissione[2]="1 di 1";
        MovimentoCommissione[3]=MovimentoPrelievo[3];
        MovimentoCommissione[4]=MovimentoPrelievo[4];
        MovimentoCommissione[5]="COMMISSIONE";
        MovimentoCommissione[6]=MovimentoPrelievo[6];
        MovimentoCommissione[8]=MovimentoPrelievo[8];
        MovimentoCommissione[9]=MovimentoPrelievo[9];
        MovimentoCommissione[10]="-"+QtaCommissione;
        MovimentoCommissione[15]=ValoreTransazione;
        MovimentoCommissione[20]=IDPrelievo+","+IDDeposito;
        MovimentoCommissione[22]="AU"; //AU -> Significa che è un movimento di commissione automaticamente generato da una condizione successiva
        //quindi se decadono le condizioni che lo hanno generato va eliminato
        //ad esempio se uno dei movimenti padri cambiano tipo o vengono eliminati va eliminato anche il suddetto movimento
        Importazioni.RiempiVuotiArray(MovimentoCommissione);
        //Se movimento di Prelievo è uguale a movimento di deposito allora non devo creare nessuna commissione
        if (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)==1){           
            MappaCryptoWallet.put(IDCommissione, MovimentoCommissione);
            MovimentoPrelievo[10]=new BigDecimal(MovimentoPrelievo[10]).subtract(new BigDecimal(MovimentoCommissione[10])).toPlainString();
            MovimentoPrelievo[15]=new BigDecimal(MovimentoPrelievo[15]).subtract(new BigDecimal(MovimentoCommissione[15])).toPlainString();
            MovimentoPrelievo[20]=IDDeposito+","+IDCommissione;
            MovimentoDeposito[20]=IDPrelievo+","+IDCommissione; 
        }else
            {
            MovimentoPrelievo[20]=IDDeposito;
            MovimentoDeposito[20]=IDPrelievo;
            }
        //Parte che modifica i movimenti preesistenti
  
         }
        
        
        
                
    }
    
    public static void CreaMovimentiScambioCryptoDifferito(String IDPrelievo,String IDDeposito){
        //come prima cosa devo generare un nuovo id per il prelievo
        System.out.println("Creo 3 movimenti");
        String MovimentoPrelievo[]=MappaCryptoWallet.get(IDPrelievo);
        String MovimentoDeposito[]=MappaCryptoWallet.get(IDDeposito);
       // BigDecimal QtaPrelievoValoreAssoluto=new BigDecimal(MovimentoPrelievo[10]).stripTrailingZeros().abs();
       // BigDecimal QtaDepositoValoreAssoluto=new BigDecimal(MovimentoDeposito[13]).stripTrailingZeros().abs();
        //Vado avanti solo se la qta prelevata è maggiore o uguale di quelòla ricevuta
     //   if (QtaPrelievoValoreAssoluto.compareTo(QtaDepositoValoreAssoluto)>=0){
            
        MovimentoPrelievo[5]="TRASFERIMENTO INTERNO";
        MovimentoDeposito[5]="TRASFERIMENTO INTERNO";

         
        
        String IDTrasferimento1;
        String IDScambio;
        String IDTrasferimento2;
        String MT1[]=new String[Importazioni.ColonneTabella];
        String MS[]=new String[Importazioni.ColonneTabella];
        String MT2[]=new String[Importazioni.ColonneTabella];
        String IDSpezzato[]=IDPrelievo.split("_");
        IDTrasferimento1=IDSpezzato[0]+"_"+IDSpezzato[1]+"_"+IDSpezzato[2]+"A_"+IDSpezzato[3]+"_DC";
        IDSpezzato=IDDeposito.split("_");
        IDScambio=IDSpezzato[0]+"_"+IDSpezzato[1]+"_00"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_SC";
        IDTrasferimento2=IDSpezzato[0]+"_"+IDSpezzato[1]+"_0"+IDSpezzato[2]+"_"+IDSpezzato[3]+"_PC";
        MT1[0]=IDTrasferimento1;
        MS[0]=IDScambio;
        MT2[0]=IDTrasferimento2;
        MT1[1]=MovimentoPrelievo[1];
        MS[1]=MovimentoDeposito[1];
        MT2[1]=MovimentoDeposito[1];
        MT1[2]="1 di 1";
        MS[2]="1 di 1";
        MT2[2]="1 di 1";
        MT1[3]=MovimentoPrelievo[3];
        MS[3]=MovimentoPrelievo[3];
        MT2[3]=MovimentoPrelievo[3];
        MT1[4]="In fase di Scambio";
        MS[4]="In fase di Scambio";
        MT2[4]="In fase di Scambio";
        MT1[5]="TRASFERIMENTO INTERNO";
        MS[5]="SCAMBIO CRYPTO";
        MT2[5]="TRASFERIMENTO INTERNO";
        MT1[6]="-> "+MovimentoPrelievo[8];
        MS[6]=MovimentoPrelievo[8]+" -> "+MovimentoDeposito[11];
        MT2[6]=MovimentoDeposito[11] +" ->";
        
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
        MS[15]=MovimentoDeposito[15];
        MT2[15]=MovimentoDeposito[15];
        
        MovimentoPrelievo[18]="PTW - Scambio Differito";
        MT1[18]="DTW - Trasferimento Interno";
        MT2[18]="PTW - Trasferimento Interno";
        MovimentoDeposito[18]="DTW - Scambio Differito"; 
        
        MovimentoPrelievo[20]=IDTrasferimento1+","+IDScambio+","+IDTrasferimento2+","+IDDeposito;
        MT1[20]=IDPrelievo+","+IDScambio+","+IDTrasferimento2+","+IDDeposito;
        MS[20]=IDPrelievo+","+IDTrasferimento1+","+IDTrasferimento2+","+IDDeposito;
        MT2[20]=IDPrelievo+","+IDTrasferimento1+","+IDScambio+","+IDDeposito;
        MovimentoDeposito[20]=IDPrelievo+","+IDTrasferimento1+","+IDScambio+","+IDTrasferimento2;
        
        MT1[22]="AU";
        MS[22]="AU";
        MT2[22]="AU";
        
        if (MovimentoDeposito.length>27){ 
            MS[28]=MovimentoDeposito[28];//Entra
            MT2[26]=MovimentoDeposito[28];//Esce
        }
        if (MovimentoPrelievo.length>25){ 
            MT1[28]=MovimentoPrelievo[26];//Entra
            MS[26]=MovimentoPrelievo[26];//Esce
        }

            Importazioni.RiempiVuotiArray(MT1);
            Importazioni.RiempiVuotiArray(MS);
            Importazioni.RiempiVuotiArray(MT2); 
          //  MappaCryptoWallet.put(IDPrelievo, MovimentoPrelievo);
            MappaCryptoWallet.put(IDTrasferimento1, MT1);
            MappaCryptoWallet.put(IDScambio, MS);
            MappaCryptoWallet.put(IDTrasferimento2, MT2);
          //  MappaCryptoWallet.put(IDDeposito, MovimentoDeposito);
      //   }
        
        
        
                
    }    
    
    
    private void ComboBox_TipoMovimentoItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_TipoMovimentoItemStateChanged
        // TODO add your handling code here:
        
       
        int scelta=this.ComboBox_TipoMovimento.getSelectedIndex();
        if (evt.getItem().toString().equals(ComboBox_TipoMovimento.getSelectedItem().toString())){
         //    System.out.println("cambio "+scelta);
        String descrizione,dettaglio;
                if (IDTrans.split("_")[4].equalsIgnoreCase("DC")){
          //in questo caso sono in presenza di un movimento di deposito
          switch(scelta){
              case 1 -> {
                  descrizione="AIRDROP o SIMILARE";
                  dettaglio="DAI - Airdrop,Cashback,Rewards etc.. (plusvalenza)";
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
        }}
    }//GEN-LAST:event_ComboBox_TipoMovimentoItemStateChanged

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
    
    public static void RiportaTransazioniASituazioneIniziale(String IDPartiConvolte[]){
        BigDecimal QtaCommissione=null;
        BigDecimal PrezzoCommissione=null;

        for (String ID:IDPartiConvolte){
           // System.out.println(ID);
            //come prima cosa cerco movimenti AC con e classificati come CM (Commissione)
            //Se lo trovo recupero i dati che mi servono e cancello la transazione
            //i dati sono la qta e il prezzo che dovrò andare a sommarli al movimento di prelievo per far tornare tutto alla situazione originale
            String attuale[]=MappaCryptoWallet.get(ID);
            if (ID.split("_")[4].equalsIgnoreCase("CM")&&attuale[22].equalsIgnoreCase("AU")){
                QtaCommissione=new BigDecimal(attuale[10]);
                PrezzoCommissione=new BigDecimal(attuale[15]);
                MappaCryptoWallet.remove(ID);
            }else if (attuale[22].equalsIgnoreCase("AU")){
                MappaCryptoWallet.remove(ID);
            }
        }

        for (String ID : IDPartiConvolte) {

            String attuale[] = MappaCryptoWallet.get(ID);
            if (attuale != null) {
                if (ID.split("_")[4].equalsIgnoreCase("DC")) {

                    attuale[5] = "DEPOSITO CRYPTO";

                } else if (ID.split("_")[4].equalsIgnoreCase("PC")) {
                    //Essendo questo il prelievo devo farci delle modifiche sopra
                    //ovvero sistemare un pò di qta e prezzo transazione (posizione 10 e 15)

                    attuale[5] = "PRELIEVO CRYPTO";
                    if (QtaCommissione != null) {
                        attuale[10] = new BigDecimal(attuale[10]).add(QtaCommissione).toPlainString();
                    }
                    if (QtaCommissione != null) {
                        attuale[15] = new BigDecimal(attuale[15]).add(PrezzoCommissione).toPlainString();
                    }

                }
                attuale[18] = "";
                attuale[19] = "";
                attuale[20] = "";
            }
        }
    }


    public void CompilaTabellaMovimetiAssociabili(String ID) {
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.Tabella_MovimentiAbbinati.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaDepositiPrelievi);
        //Tabelle.ColoraRigheTabellaCrypto(jTable2);
        String attuale[] = MappaCryptoWallet.get(ID);
        long DataOraAttuale = OperazioniSuDate.ConvertiDatainLong(attuale[1]);
        String TipoMovimentoAttuale = attuale[0].split("_")[4].trim();
        String WalletAttuale=attuale[3]+attuale[4];
        String TipoMovimentoRichiesto;
        String MonetaAttuale;
        BigDecimal QtaAttuale;
       // System.out.println(ComboBox_TipoMovimento);
        if (ComboBox_TipoMovimento.getSelectedItem()!=null&&ComboBox_TipoMovimento.getSelectedItem().toString().contains("TRASFERIMENTO TRA WALLET")) {
            if (TipoMovimentoAttuale.equalsIgnoreCase("PC")) {
                MonetaAttuale = attuale[8].trim();
                QtaAttuale = new BigDecimal(attuale[10]).stripTrailingZeros();
                TipoMovimentoRichiesto = "DC";
            } else {
                MonetaAttuale = attuale[11].trim();
                QtaAttuale = new BigDecimal(attuale[13]).stripTrailingZeros();
                TipoMovimentoRichiesto = "PC";
            }
            BigDecimal escursioneMassima = new BigDecimal(5);
            BigDecimal QtaAttualeMax = QtaAttuale.add(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();
            BigDecimal QtaAttualeMin = QtaAttuale.subtract(QtaAttuale.multiply(escursioneMassima).divide(new BigDecimal(100))).abs();

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
                    BigDecimal Qta = null;
                    BigDecimal QtanoABS = null;
                    BigDecimal SommaQta = null;
                    String Moneta = null;
                    long DataOra = 0;
                    if (TipoMovimento.equalsIgnoreCase("DC"))//manca la parte pc + questa neanche funziona//da rivedere completamente
                    {
                        Qta = new BigDecimal(v[13]).abs();
                        QtanoABS = new BigDecimal(v[13]);
                        SommaQta = QtaAttuale.add(new BigDecimal(v[13]));
                        Moneta = v[11].trim();
                        DataOra = OperazioniSuDate.ConvertiDatainLong(v[1]);

                    } else if (TipoMovimento.equalsIgnoreCase("PC"))//manca la parte pc + questa neanche funziona//da rivedere completamente
                    {
                        Qta = new BigDecimal(v[10]).abs();
                        QtanoABS = new BigDecimal(v[10]);
                        SommaQta = QtaAttuale.add(new BigDecimal(v[10]));
                        DataOra = OperazioniSuDate.ConvertiDatainLong(v[1]);
                        Moneta = v[8].trim();
                    }

                    if (QtanoABS != null && Qta != null && SommaQta != null && Moneta != null
                            && MonetaAttuale.equals(Moneta)
                            && Qta.compareTo(QtaAttualeMax) == -1 && Qta.compareTo(QtaAttualeMin) == 1
                            && DataOra < (DataOraAttuale + 86400000)
                            && DataOra > (DataOraAttuale - 86400000)
                            && !WalletRiferimento.equals(WalletAttuale)
                            && SommaQta.compareTo(new BigDecimal(0)) <= 0) {
                        String riga[] = new String[7];
                        riga[0] = v[0];
                        riga[1] = v[1];
                        riga[2] = v[3];
                        riga[3] = v[5];
                        riga[4] = Moneta;
                        riga[5] = QtanoABS.stripTrailingZeros().toPlainString();
                        riga[6] = v[18];
                        ModelloTabellaDepositiPrelievi.addRow(riga);

                    }

                    //   ModelloTabellaDepositiPrelievi.addRow(riga); 
                }
            }

        }else if (ComboBox_TipoMovimento.getSelectedItem()!=null&&ComboBox_TipoMovimento.getSelectedItem().toString().contains("SCAMBIO CRYPTO DIFFERITO")) {
           // System.out.println("Differito");
           long DataMinima=0;
           long DataMassima=0;
           long Mese=Long.parseLong("2592000000");
            if (TipoMovimentoAttuale.equalsIgnoreCase("PC")) {
                MonetaAttuale = attuale[8].trim();
              //  QtaAttuale = new BigDecimal(attuale[10]).stripTrailingZeros();
                TipoMovimentoRichiesto = "DC";
                    DataMinima=DataOraAttuale-Mese;
                    DataMassima=DataOraAttuale+ 86400000;
            } else {
                MonetaAttuale = attuale[11].trim();
              //  QtaAttuale = new BigDecimal(attuale[13]).stripTrailingZeros();
                TipoMovimentoRichiesto = "PC";
                DataMinima=DataOraAttuale- 86400000;
                DataMassima=DataOraAttuale+Mese;
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
                    
                    if (TipoMovimento.equalsIgnoreCase("DC"))//manca la parte pc + questa neanche funziona//da rivedere completamente
                    {
                        QtanoABS = new BigDecimal(v[13]);
                        Moneta = v[11].trim();
                        DataOra = OperazioniSuDate.ConvertiDatainLong(v[1])-Mese;

                    } else if (TipoMovimento.equalsIgnoreCase("PC"))//manca la parte pc + questa neanche funziona//da rivedere completamente
                    {
                        QtanoABS = new BigDecimal(v[10]);
                        DataOra = OperazioniSuDate.ConvertiDatainLong(v[1])+Mese;
                        Moneta = v[8].trim();
                    }
                    if (Moneta != null && QtanoABS!=null
                            && !MonetaAttuale.equalsIgnoreCase(Moneta)
                            && DataOra < (DataMassima)
                            && DataOra > (DataMinima)
                            ) 
                    {
                                            
                        String riga[] = new String[7];
                        riga[0] = v[0];
                        riga[1] = v[1];
                        riga[2] = v[3];
                        riga[3] = v[5];
                        riga[4] = Moneta;
                        riga[5] = QtanoABS.stripTrailingZeros().toPlainString();
                        riga[6] = v[18];
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
    private javax.swing.JComboBox<String> ComboBox_TipoMovimento;
    private javax.swing.JTable Tabella_MovimentiAbbinati;
    private javax.swing.JTextArea TextArea_Note;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
