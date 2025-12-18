/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.TabellaCryptodaAggiornare;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
     
     private static String PopUp_IDTrans=null;
     private static Component PopUp_Component=null;
     private static JTable PopUp_Tabella=null;


        public void TransazioniCrypto_CompilaTextPaneDatiMovimento(String IDTransazione) {
            SerializzaIDMovimenti(IDTransazione);
            this.setTitle("Dettaglio Movimento");
            
            

            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) Tabella.getModel();
            Tabelle.Funzioni_PulisciTabella(ModelloTabellaCrypto);
            Tabelle.CopiaPulitadaTAG(Tabella);

        
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

        PopupMenu = new javax.swing.JPopupMenu();
        MenuItem_CopiaID = new javax.swing.JMenuItem();
        MenuItem_Copia = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        MenuItem_LiFoTransazione = new javax.swing.JMenuItem();
        MenuItem_DettagliMovimento = new javax.swing.JMenuItem();
        MenuItem_ModificaMovimento = new javax.swing.JMenuItem();
        MenuItem_EliminaMovimento = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        MenuItem_ClassificaMovimento = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        MenuItem_ModificaPrezzo = new javax.swing.JMenuItem();
        MenuItem_ModificaNote = new javax.swing.JMenuItem();
        MenuItem_ModificaReward = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        MenuItem_EsportaTabella = new javax.swing.JMenuItem();
        ScrollTabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_DeFi = new javax.swing.JButton();
        Bottone_MovPrecedente = new javax.swing.JButton();
        Bottone_MovSuccessivo = new javax.swing.JButton();
        TextPane_Titolo = new javax.swing.JTextPane();
        Bottone_Modifica = new javax.swing.JButton();

        MenuItem_CopiaID.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Copia.png"))); // NOI18N
        MenuItem_CopiaID.setText("Copia ID Transazione");
        MenuItem_CopiaID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_CopiaIDActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_CopiaID);

        MenuItem_Copia.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Copia.png"))); // NOI18N
        MenuItem_Copia.setText("Copia selezione");
        MenuItem_Copia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_CopiaActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_Copia);
        PopupMenu.add(jSeparator4);

        MenuItem_LiFoTransazione.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Stack.png"))); // NOI18N
        MenuItem_LiFoTransazione.setText("Mostra LiFo Transazione");
        MenuItem_LiFoTransazione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_LiFoTransazioneActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_LiFoTransazione);

        MenuItem_DettagliMovimento.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Dettagli.png"))); // NOI18N
        MenuItem_DettagliMovimento.setText("Dettagli Movimento");
        MenuItem_DettagliMovimento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_DettagliMovimentoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_DettagliMovimento);

        MenuItem_ModificaMovimento.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        MenuItem_ModificaMovimento.setText("Modifica Movimento");
        MenuItem_ModificaMovimento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaMovimentoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaMovimento);

        MenuItem_EliminaMovimento.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Cestino.png"))); // NOI18N
        MenuItem_EliminaMovimento.setText("Elimina Movimento");
        MenuItem_EliminaMovimento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_EliminaMovimentoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_EliminaMovimento);
        PopupMenu.add(jSeparator8);

        MenuItem_ClassificaMovimento.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Classifica.png"))); // NOI18N
        MenuItem_ClassificaMovimento.setText("Classifica Movimento");
        MenuItem_ClassificaMovimento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ClassificaMovimentoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ClassificaMovimento);
        PopupMenu.add(jSeparator6);

        MenuItem_ModificaPrezzo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Prezzo.png"))); // NOI18N
        MenuItem_ModificaPrezzo.setText("Modifica Prezzo");
        MenuItem_ModificaPrezzo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                MenuItem_ModificaPrezzoMouseReleased(evt);
            }
        });
        MenuItem_ModificaPrezzo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaPrezzoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaPrezzo);

        MenuItem_ModificaNote.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        MenuItem_ModificaNote.setText("Modifica Note");
        MenuItem_ModificaNote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaNoteActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaNote);

        MenuItem_ModificaReward.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Reward.png"))); // NOI18N
        MenuItem_ModificaReward.setText("Cambia Tipologia Reward");
        MenuItem_ModificaReward.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaRewardActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaReward);
        PopupMenu.add(jSeparator7);

        MenuItem_EsportaTabella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Tabella.png"))); // NOI18N
        MenuItem_EsportaTabella.setText("Esporta Tabella in Excel");
        MenuItem_EsportaTabella.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_EsportaTabellaActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_EsportaTabella);

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
        Tabella.setCellSelectionEnabled(true);
        Tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                TabellaMouseReleased(evt);
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

    private void TabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TabellaMouseReleased
        // TODO add your handling code here:
        Funzioni_RichiamaPopUpdaTabella(Tabella,evt,-1);
    }//GEN-LAST:event_TabellaMouseReleased

    private void MenuItem_CopiaIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_CopiaIDActionPerformed
        // TODO add your handling code here:
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(PopUp_IDTrans);
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_MenuItem_CopiaIDActionPerformed

    private void MenuItem_CopiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_CopiaActionPerformed
        // TODO add your handling code here:
        Funzioni.simulaCtrlC();
    }//GEN-LAST:event_MenuItem_CopiaActionPerformed

    private void MenuItem_LiFoTransazioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_LiFoTransazioneActionPerformed
        // TODO add your handling code here:
        if (PopUp_IDTrans!=null){
            //Calcoli_PlusvalenzeNew.LifoXID lifoID=Calcoli_PlusvalenzeNew.getIDLiFo(PopUp_IDTrans);
            GUI_LiFoTransazione t =new GUI_LiFoTransazione(PopUp_IDTrans);
            t.setLocationRelativeTo(PopUp_Component);
            t.setVisible(true);
        }
    }//GEN-LAST:event_MenuItem_LiFoTransazioneActionPerformed

    private void MenuItem_DettagliMovimentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_DettagliMovimentoActionPerformed
        // TODO add your handling code here:
        if (PopUp_IDTrans!=null){
            GUI_DettaglioTransazione t =new GUI_DettaglioTransazione();
            t.AzzeraMap();
            t.TransazioniCrypto_CompilaTextPaneDatiMovimento(PopUp_IDTrans);
            t.setLocationRelativeTo(PopUp_Component);
            t.setVisible(true);
        }
    }//GEN-LAST:event_MenuItem_DettagliMovimentoActionPerformed

    private void MenuItem_ModificaMovimentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaMovimentoActionPerformed
        // TODO add your handling code here:
        if (PopUp_IDTrans!=null){
            Funzione_ModificaMovimento(PopUp_IDTrans,PopUp_Component);
        }
    }//GEN-LAST:event_MenuItem_ModificaMovimentoActionPerformed

    private void MenuItem_EliminaMovimentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_EliminaMovimentoActionPerformed
        // TODO add your handling code here:
        if (PopUp_IDTrans != null) {

            Funzione_EliminaMovimento(PopUp_IDTrans, PopUp_Component);

            // if (PopUp_Tabella.getName()!=null&&PopUp_Tabella.getName().equalsIgnoreCase("DepositiPrelievi"))DepositiPrelievi_Caricatabella();
        }
    }//GEN-LAST:event_MenuItem_EliminaMovimentoActionPerformed

     public void Funzione_EliminaMovimento(String ID,Component c){
    int risposta=JOptionPane.showOptionDialog(c,"Sicuro di voler cancellare la transazione con ID "+ID+" ?", "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
            if (risposta==0){
                //controllo se quel movimento è associato ad altri e nel qual caso lo sbianco e sbianco i movimenti associati a lui
                Funzioni.RimuoviMovimentazioneXID(ID);
                TabellaCryptodaAggiornare=true;
                SwingUtilities.invokeLater(() -> {
                JOptionPane.showConfirmDialog(c, "Transazione con ID"+ID+" eliminata correttamente.\nPremere sul Bottone Salva per rendere permanente la cancellazione fatta.",
                    "Eliminazione riuscita",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                });
            }
    }
    
    
    private void MenuItem_ClassificaMovimentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ClassificaMovimentoActionPerformed
        // TODO add your handling code here:
       /* if (Principale.PopUp_IDTrans != null) {

            ClassificazioneTrasf_Modifica mod = new ClassificazioneTrasf_Modifica(Principale.PopUp_IDTrans);
            mod.setLocationRelativeTo(Principale.PopUp_Component);
            mod.setVisible(true);

            if (mod.getModificaEffettuata()) {
                Principale.Funzioni_AggiornaTutto();
                Principale.DepositiPrelievi_Caricatabella();
            }
        }*/
    }//GEN-LAST:event_MenuItem_ClassificaMovimentoActionPerformed

    private void MenuItem_ModificaPrezzoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MenuItem_ModificaPrezzoMouseReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_MenuItem_ModificaPrezzoMouseReleased

    private void MenuItem_ModificaPrezzoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaPrezzoActionPerformed
        // TODO add your handling code here:
        //   if(Funzioni.GUIModificaPrezzo(PopUp_Component,PopUp_IDTrans))Funzioni_AggiornaTutto();
        Component c=this;
        Download progress=new Download();
        progress.MostraProgressAttesa("Scaricamento Prezzi", "Attendi scaricamento dei prezzi...");
        progress.setLocationRelativeTo(PopUp_Component);

        Thread thread;
        thread = new Thread() {
            public void run() {
                GUI_ModificaPrezzo t =new GUI_ModificaPrezzo(PopUp_IDTrans,progress);
                t.setLocationRelativeTo(c);
                t.setVisible(true);
                //progress.ChiudiFinestra();
            }
        };
        thread.start();
        progress.setVisible(true);

    }//GEN-LAST:event_MenuItem_ModificaPrezzoActionPerformed

    private void MenuItem_ModificaNoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaNoteActionPerformed
        // TODO add your handling code here:
        if (Funzioni.GUIModificaNote(PopUp_Component, PopUp_IDTrans))TabellaCryptodaAggiornare=true;

    }//GEN-LAST:event_MenuItem_ModificaNoteActionPerformed

    private void MenuItem_ModificaRewardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaRewardActionPerformed
        // TODO add your handling code here:
  /*      String Testo = "<html>Decidere il tipo di provento a cui appartiene il movimento di deposito.<br><br>"
        + "<b>Come classifichiamo il movimento?<br><br><b>"
        + "</html>";
        Object[] Bottoni = {"Annulla", "REWARD", "STAKING REWARD", "EARN", "CASHBACK", "AIRDROP"};
        int scelta = JOptionPane.showOptionDialog(this, Testo,
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
                    Principale.MappaCryptoWallet.get(Principale.PopUp_IDTrans)[5]= "REWARD";
                }
                case 2 -> {
                    Principale.MappaCryptoWallet.get(Principale.PopUp_IDTrans)[5]= "STAKING REWARD";
                }
                case 3 -> {
                    Principale.MappaCryptoWallet.get(Principale.PopUp_IDTrans)[5]= "EARN";
                }
                case 4 -> {
                    Principale.MappaCryptoWallet.get(Principale.PopUp_IDTrans)[5]= "CASHBACK";
                }
                case 5 -> {
                    Principale.MappaCryptoWallet.get(Principale.PopUp_IDTrans)[5]= "AIRDROP";
                }
                default -> {
                }
            }
            Funzioni_AggiornaTutto();
            if (Principale.PopUp_Tabella.getName()!=null&&Principale.PopUp_Tabella.getName().equalsIgnoreCase("DepositiPrelievi"))DepositiPrelievi_Caricatabella();
        }*/
    }//GEN-LAST:event_MenuItem_ModificaRewardActionPerformed

    private void MenuItem_EsportaTabellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_EsportaTabellaActionPerformed

        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Download progress = new Download();
        progress.MostraProgressAttesa("Export in Excel", "Esportazione in corso...");
        progress.setLocationRelativeTo(this);

        // Esegui l'export in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (PopUp_Tabella != null) {
                    Funzioni.Export_CreaExcelDaTabella(PopUp_Tabella);
                }
                return null;
            }

            @Override
            protected void done() {
                progress.dispose();
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        };

        worker.execute();
        progress.setVisible(true);// Questo blocca finché done() non chiama dispose()

    }//GEN-LAST:event_MenuItem_EsportaTabellaActionPerformed

    
    private void Funzioni_RichiamaPopUpdaTabella(JTable tabella, java.awt.event.MouseEvent evt, int posizioneID) {

        int rigaSelezionata = tabella.getSelectedRow();
        if (!Funzioni.PopUp_ClickInternoASelezione(tabella, evt)) {
            tabella.requestFocusInWindow();
            int row = tabella.rowAtPoint(evt.getPoint());
            int col = tabella.columnAtPoint(evt.getPoint());

            if (row != -1 && col != -1) {
                tabella.setRowSelectionInterval(row, row);
                tabella.setColumnSelectionInterval(col, col);
                tabella.changeSelection(row, col, false, false);
            }

        }
        if (rigaSelezionata != -1) {
            int rigaselezionata;
            if (tabella.getRowSorter() != null) {
                rigaselezionata = tabella.getRowSorter().convertRowIndexToModel(tabella.getSelectedRow());
            } else {
                rigaselezionata = tabella.convertRowIndexToModel(tabella.getSelectedRow());
            }
            String IDTransazione = null;
            if (posizioneID != -1) {

                IDTransazione = tabella.getModel().getValueAt(rigaselezionata, posizioneID).toString();
                if (Principale.MappaCryptoWallet.get(IDTransazione) == null) {
                    IDTransazione = null;
                }
            }
            Funzioni.PopUpMenu(this, evt, PopupMenu, IDTransazione);
            // TransazioniCrypto_CompilaTextPaneDatiMovimento();

        }
    }
    
    
    
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
    private javax.swing.JMenuItem MenuItem_ClassificaMovimento;
    private javax.swing.JMenuItem MenuItem_Copia;
    private javax.swing.JMenuItem MenuItem_CopiaID;
    private javax.swing.JMenuItem MenuItem_DettagliMovimento;
    private javax.swing.JMenuItem MenuItem_EliminaMovimento;
    private javax.swing.JMenuItem MenuItem_EsportaTabella;
    private javax.swing.JMenuItem MenuItem_LiFoTransazione;
    private javax.swing.JMenuItem MenuItem_ModificaMovimento;
    private javax.swing.JMenuItem MenuItem_ModificaNote;
    private javax.swing.JMenuItem MenuItem_ModificaPrezzo;
    private javax.swing.JMenuItem MenuItem_ModificaReward;
    private javax.swing.JPopupMenu PopupMenu;
    private javax.swing.JScrollPane ScrollTabella;
    private javax.swing.JTable Tabella;
    private javax.swing.JTextPane TextPane_Titolo;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    // End of variables declaration//GEN-END:variables
}
