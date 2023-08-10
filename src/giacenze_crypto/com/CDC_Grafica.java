/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package giacenze_crypto.com;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.lowagie.text.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


/**
 *
 * @author luca.passelli
 */
public class CDC_Grafica extends javax.swing.JFrame {

    /**
     * Creates new form com
     */
    static public boolean TabellaCryptodaAggiornare=false;
    static Map<String, String> CDC_FiatWallet_Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, String> CDC_FiatWallet_MappaTipiMovimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> CDC_FiatWallet_MappaErrori = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> CDC_CardWallet_Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> Mappa_Wallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, String[]> MappaCryptoWallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public String CDC_FiatWallet_FileDB="crypto.com.fiatwallet.db";
    static String CDC_CardWallet_FileDB="crypto.com.cardwallet.db";
    static String CDC_FileDatiDB="crypto.com.dati.db";
    static String CDC_FiatWallet_FileTipiMovimentiDB="crypto.com.fiatwallet.tipimovimenti.db";
    static String CryptoWallet_FileDB="movimenti.crypto.db";
    static public String CDC_FiatWallet_FileTipiMovimentiDBPers="crypto.com.fiatwallet.tipimovimentiPers.db";
    static String CDC_DataIniziale="";
    static String CDC_DataFinale="";
    static String CDC_FiatWallet_SaldoIniziale="0";
    static String CDC_CardWallet_SaldoIniziale="0";
    static String CDC_FiatWallet_DataSaldoIniziale="";
    static String CDC_CardWallet_DataSaldoIniziale="";
    static boolean CDC_FiatWallet_ConsideroValoreMassimoGiornaliero=false;
    static boolean CDC_CardWallet_ConsideroValoreMassimoGiornaliero=false;
    static List<String>[] CDC_CardWallet_ListaSaldi;
    static public List<String>[] CDC_FiatWallet_ListaSaldi;
    static public boolean TransazioniCrypto_DaSalvare=false;//implementata per uso futuro attualmente non ancora utilizzata
    
    //static String Appoggio="";
    
    
    public CDC_Grafica() {
       
    try {
        
            this.setTitle("Giacenze_Crypto.com 1.09 Beta");
            ImageIcon icon = new ImageIcon("logo.png");
            this.setIconImage(icon.getImage());
            File fiatwallet=new File (CDC_FiatWallet_FileDB);
            if (!fiatwallet.exists()) fiatwallet.createNewFile();

            File cardwallet=new File (CDC_CardWallet_FileDB);
            if (!cardwallet.exists()) cardwallet.createNewFile();
            
            File filedati=new File (CDC_FileDatiDB);
            if (!filedati.exists()) filedati.createNewFile();
                        
            File cryptowallet=new File (CryptoWallet_FileDB);
            if (!cryptowallet.exists()) cryptowallet.createNewFile();
            
            UIManager.setLookAndFeel( new FlatIntelliJLaf() );
        
        initComponents();
        this.CDC_FiatWallet_Label_Errore1.setVisible(false);
        this.CDC_FiatWallet_Label_Errore2.setVisible(false);
        this.CDC_FiatWallet_Bottone_Errore.setVisible(false);
        TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(false);
        
        CDC_LeggiFileDatiDB();
        TransazioniCrypto_Funzioni_NascondiColonneTabellaCrypto();
        CDC_FiatWallet_Funzione_ImportaWallet(CDC_FiatWallet_FileDB); 
        CDC_CardWallet_Funzione_ImportaWallet(CDC_CardWallet_FileDB);
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        //CDC_LeggiFileDatiDB();

        CDC_AggiornaGui();
       /* Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"),Locale.ITALY);
        long today = calendar.getTimeInMillis();
        Date currentDate = new Date(today);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Date d = f.parse(f.format(currentDate));
        System.out.println(f.format(currentDate)+"aa");
        //m1 = d.getTime();
        this.CDC_DataChooser_Iniziale.setDate(d);*/
}  catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }
        
         
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        CDC = new javax.swing.JTabbedPane();
        TransazioniCrypto = new javax.swing.JPanel();
        TransazioniCrypto_ScrollPane = new javax.swing.JScrollPane();
        TransazioniCryptoTabella = new javax.swing.JTable();
        TransazioniCrypto_Bottone_Importa = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TransazioniCryptoTextPane = new javax.swing.JTextPane();
        TransazioniCrypto_Label_MovimentiNonSalvati = new javax.swing.JLabel();
        TransazioniCrypto_Bottone_Salva = new javax.swing.JButton();
        TransazioniCrypto_Label_Filtro = new javax.swing.JLabel();
        TransazioniCryptoFiltro_Text = new javax.swing.JTextField();
        TransazioniCrypto_CheckBox_EscludiTI = new javax.swing.JCheckBox();
        TransazioniCrypto_Bottone_Annulla = new javax.swing.JButton();
        TransazioniCrypto_Label_Plusvalenza = new javax.swing.JLabel();
        TransazioniCrypto_Text_Plusvalenza = new javax.swing.JTextField();
        TransazioniCrypto_Bottone_InserisciWallet = new javax.swing.JButton();
        TransazioniCrypto_Bottone_DettaglioDefi = new javax.swing.JButton();
        TransazioniCrypto_Bottone_NuovoMovimento = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        Analisi_Crypto = new javax.swing.JPanel();
        AnalisiCrypto = new javax.swing.JTabbedPane();
        DepositiPrelievi = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        DepositiPrelievi_Tabella = new javax.swing.JTable();
        DepositiPrelievi_Bottone_AssegnazioneAutomatica = new javax.swing.JButton();
        DepositiPrelievi_Bottone_AssegnazioneManuale = new javax.swing.JButton();
        DepositiPrelievi_CheckBox_movimentiClassificati = new javax.swing.JCheckBox();
        SituazioneImport = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        SituazioneImport_Tabella1 = new javax.swing.JTable();
        CDC_CardWallet_Pannello = new javax.swing.JPanel();
        CDC_CardWallet_Bottone_CaricaCSV = new javax.swing.JButton();
        CDC_CardWallet_Label_PrimaData = new javax.swing.JLabel();
        CDC_CardWallet_Text_PrimaData = new javax.swing.JTextField();
        CDC_CardWallet_Label_UltimaData = new javax.swing.JLabel();
        CDC_CardWallet_Text_UltimaData = new javax.swing.JTextField();
        CDC_CardWallet_Label_GiacenzaIniziale = new javax.swing.JLabel();
        CDC_CardWallet_Text_GiacenzaIniziale = new javax.swing.JTextField();
        CDC_CardWallet_Checkbox_ConsideraValoreMaggiore = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        CDC_CardWallet_Label_GiacenzaMedia = new javax.swing.JLabel();
        CDC_CardWallet_Text_GiacenzaMedia = new javax.swing.JTextField();
        CDC_CardWallet_Label_Spese = new javax.swing.JLabel();
        CDC_CardWallet_Label_Entrate = new javax.swing.JLabel();
        CDC_CardWallet_Text_Entrate = new javax.swing.JTextField();
        CDC_CardWallet_Text_Spese = new javax.swing.JTextField();
        CDC_CardWallet_Label_SaldoIniziale = new javax.swing.JLabel();
        CDC_CardWallet_Text_SaldoIniziale = new javax.swing.JTextField();
        CDC_CardWallet_Label_SaldoFinale = new javax.swing.JLabel();
        CDC_CardWallet_Text_SaldoFinale = new javax.swing.JTextField();
        CDC_CardWallet_Tabella1Scroll = new javax.swing.JScrollPane();
        CDC_CardWallet_Tabella1 = new javax.swing.JTable();
        CDC_CardWallet_Tabella2Scroll = new javax.swing.JScrollPane();
        CDC_CardWallet_Tabella2 = new javax.swing.JTable();
        CDC_CardWallet_Label_Tabella1 = new javax.swing.JLabel();
        CDC_CardWallet_Label_Tabella2 = new javax.swing.JLabel();
        CDC_CardWallet_Label_FiltroTabelle = new javax.swing.JLabel();
        CDC_CardWallet_Text_FiltroTabelle = new javax.swing.JTextField();
        CDC_CardWallet_Bottone_StampaRapporto = new javax.swing.JButton();
        CDC_FiatWallet_Pannello = new javax.swing.JPanel();
        CDC_FiatWallet_Bottone_CaricaCSV = new javax.swing.JButton();
        CDC_FiatWallet_Label_PrimaData = new javax.swing.JLabel();
        CDC_FiatWallet_Label_UltimaData = new javax.swing.JLabel();
        CDC_FiatWallet_Text_PrimaData = new javax.swing.JTextField();
        CDC_FiatWallet_Text_UltimaData = new javax.swing.JTextField();
        CDC_FiatWallet_Label_GiacenzaIniziale = new javax.swing.JLabel();
        CDC_FiatWallet_Text_GiacenzaIniziale = new javax.swing.JTextField();
        CDC_FiatWallet_Label_GiacenzaMedia = new javax.swing.JLabel();
        CDC_FiatWallet_Text_GiacenzaMedia = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore = new javax.swing.JCheckBox();
        CDC_FiatWallet_Text_SaldoIniziale = new javax.swing.JTextField();
        CDC_FiatWallet_Label_SaldoIniziale = new javax.swing.JLabel();
        CDC_FiatWallet_Label_SaldoFinale = new javax.swing.JLabel();
        CDC_FiatWallet_Text_SaldoFinale = new javax.swing.JTextField();
        CDC_FiatWallet_Tabella1Scroll = new javax.swing.JScrollPane();
        CDC_FiatWallet_Tabella1 = new javax.swing.JTable();
        CDC_FiatWallet_Tabella2Scroll = new javax.swing.JScrollPane();
        CDC_FiatWallet_Tabella2 = new javax.swing.JTable();
        CDC_FiatWallet_Label_Tabella2 = new javax.swing.JLabel();
        CDC_FiatWallet_Label_Errore1 = new javax.swing.JLabel();
        CDC_FiatWallet_Label_Errore2 = new javax.swing.JLabel();
        CDC_FiatWallet_Bottone_Errore = new javax.swing.JButton();
        CDC_FiatWallet_Label_FiltroTabella = new javax.swing.JLabel();
        CDC_FiatWallet_Text_FiltroTabella = new javax.swing.JTextField();
        CDC_FiatWallet_Tabella3Scroll = new javax.swing.JScrollPane();
        CDC_FiatWallet_Tabella3 = new javax.swing.JTable();
        CDC_FiatWallet_Label_Tabella3 = new javax.swing.JLabel();
        CDC_FiatWallet_Bottone_StampaRapporto = new javax.swing.JButton();
        CDC_Opzioni = new javax.swing.JPanel();
        CDC_Opzioni_Bottone_CancellaFiatWallet = new javax.swing.JButton();
        CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet = new javax.swing.JButton();
        CDC_Opzioni_Bottone_CancellaCardWallet = new javax.swing.JButton();
        Opzioni_Bottone_CancellaTransazioniCrypto = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        Opzioni_Bottone_CancellaTransazioniCryptoXwallet = new javax.swing.JButton();
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        CDC_DataChooser_Iniziale = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        CDC_DataChooser_Finale = new com.toedter.calendar.JDateChooser();
        CDC_Label_Giorni = new javax.swing.JLabel();
        CDC_Text_Giorni = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });

        TransazioniCryptoTabella.setAutoCreateRowSorter(true);
        TransazioniCryptoTabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "<html><center>ID<br>Transazione</html>", "Data e Ora", "<html><center>Numero<br>movimento<br>su Totale<br>movimenti</html>", "<html><center>Exchange<br>/<br>Wallet</html>", "<html><center>Dettaglio<br>Wallet</html>", "<html><center>Tipo<br>Transazione<br></html>", "<html><center>Dettaglio<br>Movimento<br></html>", "<html><center>Causale<br>originale<br></html>", "<html><center>Moneta<br>Ven./Trasf.</html>", "<html><center>Tipo<br>Moneta<br>Ven./Trasf.</html>", "<html><center>Qta<br>Ven./Trasf.</html>", "<html><center>Moneta<br>Acq./Ric.</html>", "<html><center>Tipo<br>Moneta<br>Acq./Ric.</html>", "<html><center>Qta<br>Acq./Ric.</html>", "<html><center>Valore <br>transazione<br>come da CSV</html>", "<html><center>Valore<br>transazione<br>in EURO</html>", "<html><center>Non Utilizzata</html>", "<html><center>Costo Acquisto<br>al Prz di Carico<br>in EURO</html>", "<html><center><html><center>Non Utilizzata</html></html>", "<html><center>Plusvalenza<br>in EURO</html>", "<html><center>Riferimento<br>Trasferimento</html>", "Note", "Auto", "ND", "ND", "ND", "ND", "ND", "ND", "ND", "ND"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        TransazioniCryptoTabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                TransazioniCryptoTabellaMouseReleased(evt);
            }
        });
        TransazioniCryptoTabella.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TransazioniCryptoTabellaKeyReleased(evt);
            }
        });
        TransazioniCrypto_ScrollPane.setViewportView(TransazioniCryptoTabella);
        if (TransazioniCryptoTabella.getColumnModel().getColumnCount() > 0) {
            TransazioniCryptoTabella.getColumnModel().getColumn(1).setMinWidth(120);
            TransazioniCryptoTabella.getColumnModel().getColumn(1).setPreferredWidth(120);
            TransazioniCryptoTabella.getColumnModel().getColumn(1).setMaxWidth(120);
            TransazioniCryptoTabella.getColumnModel().getColumn(2).setMinWidth(70);
            TransazioniCryptoTabella.getColumnModel().getColumn(2).setPreferredWidth(70);
            TransazioniCryptoTabella.getColumnModel().getColumn(2).setMaxWidth(70);
            TransazioniCryptoTabella.getColumnModel().getColumn(8).setMinWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(8).setPreferredWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(8).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(11).setMinWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(11).setPreferredWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(11).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(12).setMinWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(12).setPreferredWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(12).setMaxWidth(60);
            TransazioniCryptoTabella.getColumnModel().getColumn(15).setMinWidth(80);
            TransazioniCryptoTabella.getColumnModel().getColumn(15).setPreferredWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(15).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(16).setMinWidth(80);
            TransazioniCryptoTabella.getColumnModel().getColumn(16).setPreferredWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(16).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(17).setMinWidth(80);
            TransazioniCryptoTabella.getColumnModel().getColumn(17).setPreferredWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(17).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(18).setMinWidth(80);
            TransazioniCryptoTabella.getColumnModel().getColumn(18).setPreferredWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(18).setMaxWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(19).setMinWidth(80);
            TransazioniCryptoTabella.getColumnModel().getColumn(19).setPreferredWidth(100);
            TransazioniCryptoTabella.getColumnModel().getColumn(19).setMaxWidth(100);
        }
        TransazioniCryptoTabella.getTableHeader().setPreferredSize(new Dimension(TransazioniCryptoTabella.getColumnModel().getTotalColumnWidth(), 64));

        TransazioniCrypto_Bottone_Importa.setText("Carica CSV");
        TransazioniCrypto_Bottone_Importa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_ImportaActionPerformed(evt);
            }
        });

        TransazioniCryptoTextPane.setContentType("text/html"); // NOI18N
        jScrollPane1.setViewportView(TransazioniCryptoTextPane);

        TransazioniCrypto_Label_MovimentiNonSalvati.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        TransazioniCrypto_Label_MovimentiNonSalvati.setForeground(new java.awt.Color(255, 51, 51));
        TransazioniCrypto_Label_MovimentiNonSalvati.setText("Attenzione ci sono dei movimenti non salvati, ricordarsi di farlo o all'uscita verranno persi!");

        TransazioniCrypto_Bottone_Salva.setText("Salva");
        TransazioniCrypto_Bottone_Salva.setEnabled(false);
        TransazioniCrypto_Bottone_Salva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_SalvaActionPerformed(evt);
            }
        });

        TransazioniCrypto_Label_Filtro.setText("Filtro : ");

        TransazioniCryptoFiltro_Text.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TransazioniCryptoFiltro_TextKeyReleased(evt);
            }
        });

        TransazioniCrypto_CheckBox_EscludiTI.setText("Nascondi Traferimenti Interni");
        TransazioniCrypto_CheckBox_EscludiTI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_CheckBox_EscludiTIActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_Annulla.setText("Annulla");
        TransazioniCrypto_Bottone_Annulla.setEnabled(false);
        TransazioniCrypto_Bottone_Annulla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_AnnullaActionPerformed(evt);
            }
        });

        TransazioniCrypto_Label_Plusvalenza.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        TransazioniCrypto_Label_Plusvalenza.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TransazioniCrypto_Label_Plusvalenza.setText("Plusvalenze del periodo selezionato : ");

        TransazioniCrypto_Text_Plusvalenza.setEditable(false);
        TransazioniCrypto_Text_Plusvalenza.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        TransazioniCrypto_Text_Plusvalenza.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        TransazioniCrypto_Text_Plusvalenza.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Text_PlusvalenzaActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_InserisciWallet.setText("Inserisci Wallet");
        TransazioniCrypto_Bottone_InserisciWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_InserisciWalletActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_DettaglioDefi.setText("DettaglioDefi");
        TransazioniCrypto_Bottone_DettaglioDefi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_DettaglioDefiActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_NuovoMovimento.setText("Nuovo Movimento");

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout TransazioniCryptoLayout = new javax.swing.GroupLayout(TransazioniCrypto);
        TransazioniCrypto.setLayout(TransazioniCryptoLayout);
        TransazioniCryptoLayout.setHorizontalGroup(
            TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TransazioniCrypto_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1054, Short.MAX_VALUE)
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addComponent(TransazioniCrypto_Bottone_Importa)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_InserisciWallet)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(TransazioniCrypto_Label_MovimentiNonSalvati, javax.swing.GroupLayout.PREFERRED_SIZE, 508, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_Salva, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6))
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                                .addComponent(TransazioniCrypto_Bottone_NuovoMovimento)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                                    .addComponent(TransazioniCrypto_Label_Filtro)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(TransazioniCryptoFiltro_Text, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TransazioniCrypto_Text_Plusvalenza)
                                    .addComponent(TransazioniCrypto_Label_Plusvalenza, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)))
                            .addComponent(TransazioniCrypto_CheckBox_EscludiTI, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TransazioniCrypto_Bottone_DettaglioDefi, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        TransazioniCryptoLayout.setVerticalGroup(
            TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TransazioniCryptoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TransazioniCrypto_Bottone_Importa)
                    .addComponent(TransazioniCrypto_Label_MovimentiNonSalvati)
                    .addComponent(TransazioniCrypto_Bottone_Salva)
                    .addComponent(TransazioniCrypto_Bottone_Annulla)
                    .addComponent(TransazioniCrypto_Bottone_InserisciWallet))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TransazioniCrypto_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TransazioniCrypto_Label_Plusvalenza)
                    .addComponent(TransazioniCrypto_Bottone_NuovoMovimento))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addComponent(TransazioniCrypto_Text_Plusvalenza, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(44, 44, 44)
                        .addComponent(jButton1)
                        .addGap(39, 39, 39)
                        .addComponent(TransazioniCrypto_Bottone_DettaglioDefi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_CheckBox_EscludiTI)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TransazioniCrypto_Label_Filtro)
                            .addComponent(TransazioniCryptoFiltro_Text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addComponent(jScrollPane1)))
        );

        CDC.addTab("Transazioni Crypto", TransazioniCrypto);

        Analisi_Crypto.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                Analisi_CryptoComponentShown(evt);
            }
        });

        AnalisiCrypto.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        DepositiPrelievi.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                DepositiPrelieviComponentShown(evt);
            }
        });

        DepositiPrelievi_Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "ID_Transazione", "Data e Ora", "Exchange / Wallet", "Tipo Transazione", "Moneta", "Qta", "Dettaglio Trasferimento"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(DepositiPrelievi_Tabella);
        if (DepositiPrelievi_Tabella.getColumnModel().getColumnCount() > 0) {
            DepositiPrelievi_Tabella.getColumnModel().getColumn(0).setMinWidth(1);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(0).setPreferredWidth(1);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(0).setMaxWidth(1);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(1).setMinWidth(120);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(1).setPreferredWidth(120);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(1).setMaxWidth(120);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(2).setMinWidth(200);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(2).setMaxWidth(400);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(3).setMinWidth(200);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(3).setMaxWidth(200);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setMinWidth(60);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setPreferredWidth(60);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setMaxWidth(120);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setPreferredWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setMaxWidth(100);
        }

        DepositiPrelievi_Bottone_AssegnazioneAutomatica.setText("Assegnazione Automatica");
        DepositiPrelievi_Bottone_AssegnazioneAutomatica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed(evt);
            }
        });

        DepositiPrelievi_Bottone_AssegnazioneManuale.setText("Modifica movimento");
        DepositiPrelievi_Bottone_AssegnazioneManuale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DepositiPrelievi_Bottone_AssegnazioneManualeActionPerformed(evt);
            }
        });

        DepositiPrelievi_CheckBox_movimentiClassificati.setSelected(true);
        DepositiPrelievi_CheckBox_movimentiClassificati.setText("Mostra movimenti già classificati");
        DepositiPrelievi_CheckBox_movimentiClassificati.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout DepositiPrelieviLayout = new javax.swing.GroupLayout(DepositiPrelievi);
        DepositiPrelievi.setLayout(DepositiPrelieviLayout);
        DepositiPrelieviLayout.setHorizontalGroup(
            DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 835, Short.MAX_VALUE)
                    .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                        .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(DepositiPrelievi_Bottone_AssegnazioneAutomatica)
                            .addComponent(DepositiPrelievi_CheckBox_movimentiClassificati, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(DepositiPrelievi_Bottone_AssegnazioneManuale)))
                .addContainerGap())
        );
        DepositiPrelieviLayout.setVerticalGroup(
            DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DepositiPrelievi_CheckBox_movimentiClassificati)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DepositiPrelievi_Bottone_AssegnazioneAutomatica)
                    .addComponent(DepositiPrelievi_Bottone_AssegnazioneManuale)))
        );

        AnalisiCrypto.addTab("Calssificazione Trasferimenti Crypto", DepositiPrelievi);

        SituazioneImport.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                SituazioneImportComponentShown(evt);
            }
        });

        SituazioneImport_Tabella1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Exchange / Wallet", "Data Primo Movimento", "Data Ultimo Movimento", "Numero Movimenti"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(SituazioneImport_Tabella1);
        if (SituazioneImport_Tabella1.getColumnModel().getColumnCount() > 0) {
            SituazioneImport_Tabella1.getColumnModel().getColumn(1).setPreferredWidth(150);
            SituazioneImport_Tabella1.getColumnModel().getColumn(1).setMaxWidth(150);
            SituazioneImport_Tabella1.getColumnModel().getColumn(2).setPreferredWidth(150);
            SituazioneImport_Tabella1.getColumnModel().getColumn(2).setMaxWidth(150);
            SituazioneImport_Tabella1.getColumnModel().getColumn(3).setPreferredWidth(150);
            SituazioneImport_Tabella1.getColumnModel().getColumn(3).setMaxWidth(150);
        }

        javax.swing.GroupLayout SituazioneImportLayout = new javax.swing.GroupLayout(SituazioneImport);
        SituazioneImport.setLayout(SituazioneImportLayout);
        SituazioneImportLayout.setHorizontalGroup(
            SituazioneImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 847, Short.MAX_VALUE)
        );
        SituazioneImportLayout.setVerticalGroup(
            SituazioneImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SituazioneImportLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addContainerGap())
        );

        AnalisiCrypto.addTab("Sitazione Import Crypto", SituazioneImport);

        javax.swing.GroupLayout Analisi_CryptoLayout = new javax.swing.GroupLayout(Analisi_Crypto);
        Analisi_Crypto.setLayout(Analisi_CryptoLayout);
        Analisi_CryptoLayout.setHorizontalGroup(
            Analisi_CryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(AnalisiCrypto)
        );
        Analisi_CryptoLayout.setVerticalGroup(
            Analisi_CryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Analisi_CryptoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(AnalisiCrypto))
        );

        CDC.addTab("Analisi Crypto", Analisi_Crypto);

        CDC_CardWallet_Bottone_CaricaCSV.setText("Carica Dati Carta");
        CDC_CardWallet_Bottone_CaricaCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_CardWallet_Bottone_CaricaCSVActionPerformed(evt);
            }
        });

        CDC_CardWallet_Label_PrimaData.setText("Data prima transazione disponibile : ");

        CDC_CardWallet_Text_PrimaData.setEditable(false);

        CDC_CardWallet_Label_UltimaData.setText("Data ultima transazione disponibile : ");

        CDC_CardWallet_Text_UltimaData.setEditable(false);
        CDC_CardWallet_Text_UltimaData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_CardWallet_Text_UltimaDataActionPerformed(evt);
            }
        });

        CDC_CardWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in euro al : ");

        CDC_CardWallet_Text_GiacenzaIniziale.setEditable(false);
        CDC_CardWallet_Text_GiacenzaIniziale.setText("0");
        CDC_CardWallet_Text_GiacenzaIniziale.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                CDC_CardWallet_Text_GiacenzaInizialeKeyReleased(evt);
            }
        });

        CDC_CardWallet_Checkbox_ConsideraValoreMaggiore.setText("Per il calcolo della giacenza media considera il valore più alto della giornata");
        CDC_CardWallet_Checkbox_ConsideraValoreMaggiore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CDC_CardWallet_Checkbox_ConsideraValoreMaggioreMouseClicked(evt);
            }
        });

        CDC_CardWallet_Label_GiacenzaMedia.setText("GiacenzaMedia : ");

        CDC_CardWallet_Text_GiacenzaMedia.setEditable(false);
        CDC_CardWallet_Text_GiacenzaMedia.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        CDC_CardWallet_Label_Spese.setText("Totale Spese :");

        CDC_CardWallet_Label_Entrate.setText("Totale Entrate :");

        CDC_CardWallet_Text_Entrate.setEditable(false);

        CDC_CardWallet_Text_Spese.setEditable(false);

        CDC_CardWallet_Label_SaldoIniziale.setText("Saldo Inizio Periodo :");

        CDC_CardWallet_Text_SaldoIniziale.setEditable(false);
        CDC_CardWallet_Text_SaldoIniziale.setToolTipText("Saldo ad Inizio Giornata");

        CDC_CardWallet_Label_SaldoFinale.setText("Saldo Fine Periodo :");

        CDC_CardWallet_Text_SaldoFinale.setEditable(false);
        CDC_CardWallet_Text_SaldoFinale.setToolTipText("Saldo a Fine Giornata");

        CDC_CardWallet_Tabella1.setAutoCreateRowSorter(true);
        CDC_CardWallet_Tabella1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Causale", "Ammontare"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        CDC_CardWallet_Tabella1Scroll.setViewportView(CDC_CardWallet_Tabella1);
        if (CDC_CardWallet_Tabella1.getColumnModel().getColumnCount() > 0) {
            CDC_CardWallet_Tabella1.getColumnModel().getColumn(1).setPreferredWidth(100);
            CDC_CardWallet_Tabella1.getColumnModel().getColumn(1).setMaxWidth(100);
        }

        CDC_CardWallet_Tabella2.setAutoCreateRowSorter(true);
        CDC_CardWallet_Tabella2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Data", "Controparte", "Valore", "Rimanenze"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        CDC_CardWallet_Tabella2Scroll.setViewportView(CDC_CardWallet_Tabella2);
        if (CDC_CardWallet_Tabella2.getColumnModel().getColumnCount() > 0) {
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(0).setPreferredWidth(200);
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(0).setMaxWidth(200);
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(2).setPreferredWidth(100);
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(2).setMaxWidth(100);
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(3).setPreferredWidth(100);
            CDC_CardWallet_Tabella2.getColumnModel().getColumn(3).setMaxWidth(100);
        }

        CDC_CardWallet_Label_Tabella1.setBackground(new java.awt.Color(255, 255, 255));
        CDC_CardWallet_Label_Tabella1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_CardWallet_Label_Tabella1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CDC_CardWallet_Label_Tabella1.setText("TABELLA MOVIMENTI RAGRUPPATI PER CAUSALE");

        CDC_CardWallet_Label_Tabella2.setBackground(new java.awt.Color(255, 255, 255));
        CDC_CardWallet_Label_Tabella2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_CardWallet_Label_Tabella2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CDC_CardWallet_Label_Tabella2.setText("TABELLA MOVIMENTI");

        CDC_CardWallet_Label_FiltroTabelle.setText("Filtro Tabelle : ");

        CDC_CardWallet_Text_FiltroTabelle.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                CDC_CardWallet_Text_FiltroTabelleKeyReleased(evt);
            }
        });

        CDC_CardWallet_Bottone_StampaRapporto.setText("Stampa Rapporto PDF");
        CDC_CardWallet_Bottone_StampaRapporto.setMaximumSize(new java.awt.Dimension(150, 23));
        CDC_CardWallet_Bottone_StampaRapporto.setMinimumSize(new java.awt.Dimension(150, 23));
        CDC_CardWallet_Bottone_StampaRapporto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_CardWallet_Bottone_StampaRapportoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout CDC_CardWallet_PannelloLayout = new javax.swing.GroupLayout(CDC_CardWallet_Pannello);
        CDC_CardWallet_Pannello.setLayout(CDC_CardWallet_PannelloLayout);
        CDC_CardWallet_PannelloLayout.setHorizontalGroup(
            CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator2)
            .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addComponent(CDC_CardWallet_Label_FiltroTabelle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CDC_CardWallet_Text_FiltroTabelle))
                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_CardWallet_Label_Tabella1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                            .addComponent(CDC_CardWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_CardWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                            .addComponent(CDC_CardWallet_Label_Tabella2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(CDC_CardWallet_Label_Spese)
                                    .addComponent(CDC_CardWallet_Label_Entrate))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(CDC_CardWallet_Text_Spese)
                                    .addComponent(CDC_CardWallet_Text_Entrate, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(140, 140, 140)
                                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                        .addComponent(CDC_CardWallet_Label_GiacenzaMedia)
                                        .addGap(26, 26, 26)
                                        .addComponent(CDC_CardWallet_Text_GiacenzaMedia))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                        .addComponent(CDC_CardWallet_Label_SaldoFinale)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(CDC_CardWallet_Text_SaldoFinale))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                        .addComponent(CDC_CardWallet_Label_SaldoIniziale)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(CDC_CardWallet_Text_SaldoIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                .addComponent(CDC_CardWallet_Label_GiacenzaIniziale)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_CardWallet_Text_GiacenzaIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                                .addComponent(CDC_CardWallet_Bottone_CaricaCSV, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(CDC_CardWallet_Label_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_CardWallet_Text_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(CDC_CardWallet_Label_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_CardWallet_Text_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(CDC_CardWallet_Checkbox_ConsideraValoreMaggiore, javax.swing.GroupLayout.PREFERRED_SIZE, 455, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(CDC_CardWallet_Bottone_StampaRapporto, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        CDC_CardWallet_PannelloLayout.setVerticalGroup(
            CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_CardWallet_Label_PrimaData)
                    .addComponent(CDC_CardWallet_Text_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CDC_CardWallet_Label_UltimaData)
                    .addComponent(CDC_CardWallet_Text_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CDC_CardWallet_Bottone_CaricaCSV))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CDC_CardWallet_Checkbox_ConsideraValoreMaggiore)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_CardWallet_Label_GiacenzaIniziale)
                    .addComponent(CDC_CardWallet_Text_GiacenzaIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Label_SaldoIniziale)
                            .addComponent(CDC_CardWallet_Text_SaldoIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CDC_CardWallet_Bottone_StampaRapporto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Label_SaldoFinale)
                            .addComponent(CDC_CardWallet_Text_SaldoFinale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Text_GiacenzaMedia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CDC_CardWallet_Label_GiacenzaMedia)))
                    .addGroup(CDC_CardWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Label_Spese)
                            .addComponent(CDC_CardWallet_Text_Spese, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(7, 7, 7)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Text_Entrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CDC_CardWallet_Label_Entrate))
                        .addGap(82, 82, 82)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_CardWallet_Label_Tabella1)
                            .addComponent(CDC_CardWallet_Label_Tabella2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_CardWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                            .addComponent(CDC_CardWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_CardWallet_Label_FiltroTabelle)
                    .addComponent(CDC_CardWallet_Text_FiltroTabelle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        CDC.addTab("Carta", CDC_CardWallet_Pannello);

        CDC_FiatWallet_Bottone_CaricaCSV.setText("Carica Dati Fiat Wallet");
        CDC_FiatWallet_Bottone_CaricaCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_FiatWallet_Bottone_CaricaCSVActionPerformed(evt);
            }
        });

        CDC_FiatWallet_Label_PrimaData.setText("Data prima transazione disponibile : ");

        CDC_FiatWallet_Label_UltimaData.setText("Data ultima transazione disponibile : ");

        CDC_FiatWallet_Text_PrimaData.setEditable(false);

        CDC_FiatWallet_Text_UltimaData.setEditable(false);
        CDC_FiatWallet_Text_UltimaData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_FiatWallet_Text_UltimaDataActionPerformed(evt);
            }
        });

        CDC_FiatWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in euro al : ");

        CDC_FiatWallet_Text_GiacenzaIniziale.setEditable(false);
        CDC_FiatWallet_Text_GiacenzaIniziale.setText("0");
        CDC_FiatWallet_Text_GiacenzaIniziale.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                CDC_FiatWallet_Text_GiacenzaInizialeKeyReleased(evt);
            }
        });

        CDC_FiatWallet_Label_GiacenzaMedia.setText("GiacenzaMedia : ");

        CDC_FiatWallet_Text_GiacenzaMedia.setEditable(false);
        CDC_FiatWallet_Text_GiacenzaMedia.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore.setText("Per il calcolo della giacenza media considera il valore più alto della giornata");
        CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CDC_FiatWallet_Checkbox_ConsideraValoreMaggioreMouseClicked(evt);
            }
        });

        CDC_FiatWallet_Text_SaldoIniziale.setEditable(false);
        CDC_FiatWallet_Text_SaldoIniziale.setToolTipText("Saldo ad Inizio Giornata");

        CDC_FiatWallet_Label_SaldoIniziale.setText("Saldo Inizio Periodo :");

        CDC_FiatWallet_Label_SaldoFinale.setText("Saldo Fine Periodo :");

        CDC_FiatWallet_Text_SaldoFinale.setEditable(false);
        CDC_FiatWallet_Text_SaldoFinale.setToolTipText("Saldo a Fine Giornata");

        CDC_FiatWallet_Tabella1.setAutoCreateRowSorter(true);
        CDC_FiatWallet_Tabella1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Causale", "Ammontare"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        CDC_FiatWallet_Tabella1Scroll.setViewportView(CDC_FiatWallet_Tabella1);
        if (CDC_FiatWallet_Tabella1.getColumnModel().getColumnCount() > 0) {
            CDC_FiatWallet_Tabella1.getColumnModel().getColumn(1).setPreferredWidth(100);
            CDC_FiatWallet_Tabella1.getColumnModel().getColumn(1).setMaxWidth(100);
        }

        CDC_FiatWallet_Tabella2.setAutoCreateRowSorter(true);
        CDC_FiatWallet_Tabella2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Data", "Causale", "Dettaglio", "Valore", "Rimanenze"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        CDC_FiatWallet_Tabella2Scroll.setViewportView(CDC_FiatWallet_Tabella2);
        if (CDC_FiatWallet_Tabella2.getColumnModel().getColumnCount() > 0) {
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(0).setPreferredWidth(130);
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(0).setMaxWidth(130);
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(3).setPreferredWidth(100);
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(3).setMaxWidth(100);
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(4).setPreferredWidth(100);
            CDC_FiatWallet_Tabella2.getColumnModel().getColumn(4).setMaxWidth(100);
        }

        CDC_FiatWallet_Label_Tabella2.setBackground(new java.awt.Color(255, 255, 255));
        CDC_FiatWallet_Label_Tabella2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_FiatWallet_Label_Tabella2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CDC_FiatWallet_Label_Tabella2.setText("TABELLA MOVIMENTI");

        CDC_FiatWallet_Label_Errore1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_FiatWallet_Label_Errore1.setForeground(new java.awt.Color(255, 0, 51));
        CDC_FiatWallet_Label_Errore1.setText("Attenzione ci sono dei movimenti non contabilizzati!");

        CDC_FiatWallet_Label_Errore2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_FiatWallet_Label_Errore2.setForeground(new java.awt.Color(255, 0, 0));
        CDC_FiatWallet_Label_Errore2.setText("Premere sul pulsante qui sotto per visualizzarli!");

        CDC_FiatWallet_Bottone_Errore.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_FiatWallet_Bottone_Errore.setText("Vedi Errori");
        CDC_FiatWallet_Bottone_Errore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_FiatWallet_Bottone_ErroreActionPerformed(evt);
            }
        });

        CDC_FiatWallet_Label_FiltroTabella.setText("Filtro Tabella Movimenti : ");

        CDC_FiatWallet_Text_FiltroTabella.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                CDC_FiatWallet_Text_FiltroTabellaKeyReleased(evt);
            }
        });

        CDC_FiatWallet_Tabella3.setAutoCreateRowSorter(true);
        CDC_FiatWallet_Tabella3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Causale", "Ammontare"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        CDC_FiatWallet_Tabella3Scroll.setViewportView(CDC_FiatWallet_Tabella3);
        if (CDC_FiatWallet_Tabella3.getColumnModel().getColumnCount() > 0) {
            CDC_FiatWallet_Tabella3.getColumnModel().getColumn(1).setPreferredWidth(100);
            CDC_FiatWallet_Tabella3.getColumnModel().getColumn(1).setMaxWidth(100);
        }

        CDC_FiatWallet_Label_Tabella3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CDC_FiatWallet_Label_Tabella3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CDC_FiatWallet_Label_Tabella3.setText("Tabella Dettagli Aggregati");

        CDC_FiatWallet_Bottone_StampaRapporto.setText("Stampa Rapporto PDF");
        CDC_FiatWallet_Bottone_StampaRapporto.setMaximumSize(new java.awt.Dimension(150, 23));
        CDC_FiatWallet_Bottone_StampaRapporto.setMinimumSize(new java.awt.Dimension(150, 23));
        CDC_FiatWallet_Bottone_StampaRapporto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_FiatWallet_Bottone_StampaRapportoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout CDC_FiatWallet_PannelloLayout = new javax.swing.GroupLayout(CDC_FiatWallet_Pannello);
        CDC_FiatWallet_Pannello.setLayout(CDC_FiatWallet_PannelloLayout);
        CDC_FiatWallet_PannelloLayout.setHorizontalGroup(
            CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                .addComponent(CDC_FiatWallet_Label_FiltroTabella)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_FiatWallet_Text_FiltroTabella))))
                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                        .addComponent(CDC_FiatWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(46, 46, 46)
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_FiatWallet_Label_Errore1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(CDC_FiatWallet_Label_Errore2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                        .addComponent(CDC_FiatWallet_Label_SaldoIniziale)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(CDC_FiatWallet_Text_SaldoIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(CDC_FiatWallet_Label_SaldoFinale)
                                            .addComponent(CDC_FiatWallet_Label_GiacenzaMedia))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(CDC_FiatWallet_Text_GiacenzaMedia, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                                            .addComponent(CDC_FiatWallet_Text_SaldoFinale)))
                                    .addComponent(CDC_FiatWallet_Bottone_Errore, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 295, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(CDC_FiatWallet_Bottone_StampaRapporto, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(CDC_FiatWallet_Tabella3Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
                            .addComponent(CDC_FiatWallet_Label_Tabella3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_FiatWallet_Label_Tabella2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(CDC_FiatWallet_Tabella2Scroll)))
                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(CDC_FiatWallet_Label_GiacenzaIniziale)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_FiatWallet_Text_GiacenzaIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore, javax.swing.GroupLayout.PREFERRED_SIZE, 455, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                                .addComponent(CDC_FiatWallet_Bottone_CaricaCSV, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(CDC_FiatWallet_Label_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_FiatWallet_Text_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_FiatWallet_Label_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_FiatWallet_Text_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        CDC_FiatWallet_PannelloLayout.setVerticalGroup(
            CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_FiatWallet_Label_PrimaData)
                    .addComponent(CDC_FiatWallet_Text_PrimaData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CDC_FiatWallet_Label_UltimaData)
                    .addComponent(CDC_FiatWallet_Text_UltimaData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CDC_FiatWallet_Bottone_CaricaCSV))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_FiatWallet_Label_GiacenzaIniziale)
                    .addComponent(CDC_FiatWallet_Text_GiacenzaIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_FiatWallet_PannelloLayout.createSequentialGroup()
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_FiatWallet_Label_SaldoIniziale)
                            .addComponent(CDC_FiatWallet_Text_SaldoIniziale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CDC_FiatWallet_Bottone_StampaRapporto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_FiatWallet_Label_SaldoFinale)
                            .addComponent(CDC_FiatWallet_Text_SaldoFinale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CDC_FiatWallet_Label_GiacenzaMedia)
                            .addComponent(CDC_FiatWallet_Text_GiacenzaMedia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(CDC_FiatWallet_Label_Errore1)
                        .addGap(2, 2, 2)
                        .addComponent(CDC_FiatWallet_Label_Errore2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CDC_FiatWallet_Bottone_Errore))
                    .addComponent(CDC_FiatWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_FiatWallet_Label_Tabella2)
                    .addComponent(CDC_FiatWallet_Label_Tabella3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CDC_FiatWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(CDC_FiatWallet_Tabella3Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_FiatWallet_Label_FiltroTabella)
                    .addComponent(CDC_FiatWallet_Text_FiltroTabella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        CDC.addTab("Fiat Wallet", CDC_FiatWallet_Pannello);

        CDC_Opzioni.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                CDC_OpzioniComponentShown(evt);
            }
        });

        CDC_Opzioni_Bottone_CancellaFiatWallet.setText("Elimina tutti i dati dal Fiat Wallet");
        CDC_Opzioni_Bottone_CancellaFiatWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_Opzioni_Bottone_CancellaFiatWalletActionPerformed(evt);
            }
        });

        CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet.setText("Elimina personalizzazioni movimenti Fiat Wallet");
        CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWalletActionPerformed(evt);
            }
        });

        CDC_Opzioni_Bottone_CancellaCardWallet.setText("Elimina tutti i dati dal Card Wallet");
        CDC_Opzioni_Bottone_CancellaCardWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_Opzioni_Bottone_CancellaCardWalletActionPerformed(evt);
            }
        });

        Opzioni_Bottone_CancellaTransazioniCrypto.setText("Elimina tutte le transazioni Crypto");
        Opzioni_Bottone_CancellaTransazioniCrypto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed(evt);
            }
        });

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("FIAT WALLET");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("CRYPTO");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("CARD WALLET");

        Opzioni_Bottone_CancellaTransazioniCryptoXwallet.setText("Elimina Dati singolo Wallet/Exchange");
        Opzioni_Bottone_CancellaTransazioniCryptoXwallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed(evt);
            }
        });

        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "----------" }));

        jLabel8.setText("->");

        javax.swing.GroupLayout CDC_OpzioniLayout = new javax.swing.GroupLayout(CDC_Opzioni);
        CDC_Opzioni.setLayout(CDC_OpzioniLayout);
        CDC_OpzioniLayout.setHorizontalGroup(
            CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                        .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 671, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                                    .addComponent(CDC_Opzioni_Bottone_CancellaFiatWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(36, 36, 36)
                                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, Short.MAX_VALUE)
                                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(CDC_Opzioni_Bottone_CancellaCardWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addContainerGap(383, Short.MAX_VALUE))
                    .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                        .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 303, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(Opzioni_Bottone_CancellaTransazioniCryptoXwallet, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(Opzioni_Bottone_CancellaTransazioniCrypto, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
                                .addGap(11, 11, 11)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(Opzioni_Combobox_CancellaTransazioniCryptoXwallet, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        CDC_OpzioniLayout.setVerticalGroup(
            CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addComponent(CDC_Opzioni_Bottone_CancellaFiatWallet)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet))
                            .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(18, 18, 18)
                                .addComponent(CDC_Opzioni_Bottone_CancellaCardWallet)))
                        .addGap(19, 19, 19)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(CDC_OpzioniLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Opzioni_Bottone_CancellaTransazioniCrypto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Opzioni_Bottone_CancellaTransazioniCryptoXwallet)
                    .addComponent(Opzioni_Combobox_CancellaTransazioniCryptoXwallet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(315, Short.MAX_VALUE))
        );

        CDC.addTab("Opzioni", CDC_Opzioni);

        jLabel1.setText("Seleziona data inizio e fine per i calcoli ->");

        jLabel2.setText("Data Inizio :");

        CDC_DataChooser_Iniziale.setDateFormatString("yyyy-MM-dd");
        CDC_DataChooser_Iniziale.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                CDC_DataChooser_InizialePropertyChange(evt);
            }
        });

        jLabel3.setText("Data Fine :");

        CDC_DataChooser_Finale.setDateFormatString("yyyy-MM-dd");
        CDC_DataChooser_Finale.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                CDC_DataChooser_FinalePropertyChange(evt);
            }
        });

        CDC_Label_Giorni.setText("Giorni : ");

        CDC_Text_Giorni.setEditable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CDC_DataChooser_Iniziale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(41, 41, 41)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CDC_DataChooser_Finale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(124, 124, 124)
                        .addComponent(CDC_Label_Giorni, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CDC_Text_Giorni))
                    .addComponent(CDC))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CDC_DataChooser_Iniziale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CDC_DataChooser_Finale, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(CDC_Label_Giorni)
                        .addComponent(CDC_Text_Giorni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(CDC)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    public void CDC_AggiornaGui() {
        CDC_FiatWallet_AggiornaDatisuGUI();
        CDC_CardWallet_AggiornaDatisuGUI();
    }
    
    
    private void CDC_LeggiFileDatiDB() { //CDC_FileDatiDB
   // CDC_FileDatiDB
   String riga;
        try (FileReader fire = new FileReader(CDC_FileDatiDB); 
                BufferedReader bure = new BufferedReader(fire);) 
        {
                while((riga=bure.readLine())!=null)
                {
                    String splittata[]=riga.split("=");
                    if (splittata.length==2)
                        {
                            if (splittata[0].equalsIgnoreCase("DataIniziale")){CDC_DataIniziale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("DataFinale")){CDC_DataFinale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("CDC_FiatWallet_SaldoIniziale")){CDC_FiatWallet_SaldoIniziale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("CDC_FiatWallet_DataSaldoIniziale")){CDC_FiatWallet_DataSaldoIniziale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("CDC_FiatWallet_ConsideraValoreMassimoGiornaliero")){CDC_FiatWallet_ConsideroValoreMassimoGiornaliero=Boolean.parseBoolean(splittata[1]);}
                            if (splittata[0].equalsIgnoreCase("CDC_CardWallet_SaldoIniziale")){CDC_CardWallet_SaldoIniziale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("CDC_CardWallet_DataSaldoIniziale")){CDC_CardWallet_DataSaldoIniziale=splittata[1];}
                            if (splittata[0].equalsIgnoreCase("CDC_CardWallet_ConsideraValoreMassimoGiornaliero")){CDC_CardWallet_ConsideroValoreMassimoGiornaliero=Boolean.parseBoolean(splittata[1]);}
                        }
                }
                
      // bure.close();
      // fire.close();

        
        if (CDC_DataIniziale.equalsIgnoreCase("")||CDC_DataFinale.equalsIgnoreCase("")){
           LocalDate current_date = LocalDate.now();
            //System.out.println("Current date: "+current_date);

            //getting the current year from the current_date
            int current_Year = current_date.getYear();
          //  System.out.println(Calendar);
           CDC_DataIniziale=current_Year-1+"-01-01";
           CDC_DataFinale=current_Year-1+"-12-31";
        }
      
           }   catch (FileNotFoundException ex) {     
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        
   }catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        
        
        
        
        
        
       CDC_ScriviDatesuGUI(); 
     }   
        
    
    
    
    public void TransazioniCrypto_Funzioni_NascondiColonneTabellaCrypto(){
       // this.CDC.remove(this.TransazioniCrypto);
        //per nascondere devo farlo al contrario
        
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(30));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(29));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(28));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(27));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(26));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(25));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(24));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(23));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(22));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(21));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(20));
       // TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(19));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(18));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(17));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(16));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(15));        
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(14));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(13));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(12));        
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(11));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(10));        
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(9));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(8));        
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(7));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(6));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(5));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(4));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(3));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(2));
        //TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(1));
        TransazioniCryptoTabella.getColumnModel().removeColumn(TransazioniCryptoTabella.getColumnModel().getColumn(0));
        //Elenco Colonne
        /*
        0 - TrasID -> es. 202112031045_Binance_ScambioCryptoCrypto
        1 - Numero di movimenti che compongono la transazione -> es- 1 di 3 (ovvero questa transazione è composta da 3 movimenti e questo è il primo movimento)
        2 - Exchange/Wallet -> es. Crypto.com
        3 - Data e ora -> es. 2021/12/03 10:45
        4 - Tipo Movimento -> es. Scambio Crypto/Crypto
        5 - Causale originale -> Causale originale come da CSV (es. Crypto_exchange)
        6 - Uscita/Entrata -> es. Uscita
        7 - Moneta -> es. CRO oppure ETH oppure EUR
        8 - Tipo Moneta -> es. Crypto oppure FIAT
        9 - Quantità -> es. 10
        10 - Valore di Mercato transazione (come da csv) -> es. 10 USD
        11 - Valore di Mercato Transazione in EURO -> es. 9
        12 - Valore di Mercato unitario in EUR (singolo pezzo) -> es 0,9
        13 - Prezzo di Carico Totale Transazione in EUR -> es. 8
        14 - Prezzo di Carico in EUR (Unitario) -> es. 0,8
        15 - Plusvalenza in EUR della Transazione -> es 3
        16 - Riferimento x Trasferimenti -> Se è un traferimento si mette il riferimento al wallet/ transaziojne che l'ha generato es. 202112031045_Crypto.com_TraferimentoCrypto
        17 - Note -> Eventuali note sulla transazione o sulla singola parte della transazione.
        
        */
    }
    

    
    
   private void CDC_ScriviDatesuGUI() {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            Date d = f.parse(CDC_DataIniziale);
            this.CDC_DataChooser_Iniziale.setDate(d);
            d= f.parse(CDC_DataFinale);
            this.CDC_DataChooser_Finale.setDate(d);
            this.CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore.setSelected(CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
            this.CDC_CardWallet_Checkbox_ConsideraValoreMaggiore.setSelected(CDC_CardWallet_ConsideroValoreMassimoGiornaliero);
        } catch (ParseException ex) {
         //   Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        //CDC_ScriviFileDatiDB();
   }
    
   private void CDC_ScriviFileDatiDB() { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
       FileWriter w=new FileWriter(CDC_FileDatiDB);
       BufferedWriter b=new BufferedWriter (w);
       b.write("DataIniziale="+CDC_DataIniziale+"\n");
       b.write("DataFinale="+CDC_DataFinale+"\n");
       if (CDC_FiatWallet_SaldoIniziale.equalsIgnoreCase(""))CDC_FiatWallet_SaldoIniziale="0";
       b.write("CDC_FiatWallet_SaldoIniziale="+CDC_FiatWallet_SaldoIniziale+"\n"); 
       b.write("CDC_FiatWallet_DataSaldoIniziale="+CDC_FiatWallet_DataSaldoIniziale+"\n");
       b.write("CDC_FiatWallet_ConsideraValoreMassimoGiornaliero="+CDC_FiatWallet_ConsideroValoreMassimoGiornaliero+"\n");
       b.write("CDC_CardWallet_SaldoIniziale="+CDC_CardWallet_SaldoIniziale+"\n"); 
       b.write("CDC_CardWallet_DataSaldoIniziale="+CDC_CardWallet_DataSaldoIniziale+"\n");       
       b.write("CDC_CardWallet_ConsideraValoreMassimoGiornaliero="+CDC_CardWallet_ConsideroValoreMassimoGiornaliero+"\n");
       //System.out.println(CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
       b.close();
       w.close();

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }
            
   /* public static void getBitcoinPrice(String symbol, long timestamp) throws IOException {
        try {
            String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1m&startTime=" + timestamp + "&endTime=" + timestamp;
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parsing del JSON di risposta per ottenere il prezzo
            String json = response.toString();
            String[] data = json.substring(1, json.length() - 1).split(",");
            //double price = Double.parseDouble(data[4]);
            System.out.println (apiUrl);
            System.out.println (response.toString());
            System.out.println (data[4]); //questo è il valore sulla coppia con usdt
            
            // return price;
        } catch (URISyntaxException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
    public void CDC_FiatWallet_Funzione_ImportaWallet(String fiatwallet) {                                          
        // TODO add your handling code here:
        
        //Questa funzione importa i dati del wallet, presi dal file csv o dal database interno e li mette nelle mappe
        //questo per rendere le operazioni molto più veloci visto che il tutto viene gestito in ram
        //non vengono utilizzati database visto che i dati sono relativamente pochi
       // CDC_FiatWallet_Mappa.clear();
        String riga;
        try (FileReader fire = new FileReader(fiatwallet); 
                BufferedReader bure = new BufferedReader(fire);) 
        {
                while((riga=bure.readLine())!=null)
                {
                    String splittata[]=riga.split(",");
                    if (splittata.length==10)// se non è esattamente uguale a 10 significa che il file non è corretto
                    {
                        if(Funzioni_Date_ConvertiDatainLong(splittata[0])!=0)// se la riga riporta una data valida allora proseguo con l'importazione
                        {
                            //CDC_FiatWallet_Mappa.put(splittata[0], riga);
                            String idRiga;
                            int Colonna=CDC_Funzione_trovaColonnaEuro(riga);
                            if (Colonna==999)
                            {
                                idRiga=splittata[0]+splittata[1]+splittata[9];
                            }
                            else
                            {
                                idRiga=splittata[0]+splittata[1]+splittata[9]+splittata[Colonna];
                            }
                            CDC_FiatWallet_Mappa.put(idRiga, riga);
                        }
                    }
                }
    //   bure.close();
     //  fire.close();
    }   catch (FileNotFoundException ex) {     
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        CDC_FiatWallet_MappaTipiMovimenti.clear();
        //Ora importo i tipi movimento del FiatWallet
        
                try (FileReader fire = new FileReader(CDC_FiatWallet_FileTipiMovimentiDB); 
                BufferedReader bure = new BufferedReader(fire);) 
        {
                while((riga=bure.readLine())!=null)
                {
                    String splittata[]=riga.split(";");
                    if (splittata.length==4)// se non è esattamente uguale a 4 significa che il file non è corretto
                    {

                            CDC_FiatWallet_MappaTipiMovimenti.put(splittata[0], riga);
                           // System.out.println("aaa");

                    }
                }
     //  bure.close();
     //  fire.close();
                File movPers=new File (CDC_FiatWallet_FileTipiMovimentiDBPers);
        if (!movPers.exists()) movPers.createNewFile();
               FileReader fires = new FileReader(CDC_FiatWallet_FileTipiMovimentiDBPers); 
               BufferedReader bures = new BufferedReader(fires);
        
                while((riga=bures.readLine())!=null)
                {
                    String splittata[]=riga.split(";");
                    if (splittata.length==4)// se non è esattamente uguale a 4 significa che il file non è corretto
                    {

                            CDC_FiatWallet_MappaTipiMovimenti.put(splittata[0], riga);
                            //System.out.println("aaa");

                    }
                }
       bures.close();
       fires.close();
       
    }   catch (FileNotFoundException ex) {     
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
                
                
                
                CDC_FiatWallet_ListaSaldi=CDC_FiatWallet_Funzione_CalcolaListaSaldi();
        
   } 
    
        public void CDC_CardWallet_Funzione_ImportaWallet(String cardwallet) {                                          
        String riga;
        try (FileReader fire = new FileReader(cardwallet); 
                BufferedReader bure = new BufferedReader(fire);) 
        {
                while((riga=bure.readLine())!=null)
                {
                    String splittata[]=riga.split(",");
                    if (splittata.length>=9)// se non è esattamente uguale a 10 significa che il file non è corretto
                    {
                        if(Funzioni_Date_ConvertiDatainLong(splittata[0])!=0)// se la riga riporta una data valida allora proseguo con l'importazione
                        {
                            int Colonna=CDC_Funzione_trovaColonnaEuro(riga);
                            String idRiga;
                            if (Colonna==999)
                            {
                                idRiga=splittata[0]+splittata[1];
                            }
                            else
                            {
                                idRiga=splittata[0]+splittata[1]+splittata[Colonna];
                            }
                            CDC_CardWallet_Mappa.put(idRiga, riga);
                        }
                    }
                }
     //  bure.close();
     //  fire.close();
    }   catch (FileNotFoundException ex) {     
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        
      CDC_CardWallet_ListaSaldi=CDC_CardWallet_Funzione_CalcolaListaSaldi();  
 
        
   } 
    
    
    
   private void CDC_FiatWallet_AggiornaDatisuGUI() {
       
//       CDC_FiatWallet_Text_PeriodoRiferimento.setText(CDC_DataIniziale+"     ->     "+CDC_DataFinale);
       //scrivo le date relative a tutto quello che ho in pancia come dati
       //In questa prima parte recupero i dati essenziali che mi servono poi per i calcoli
       CDC_FiatWallet_Mappa.size();
       int i=0;
       for (String value : CDC_FiatWallet_Mappa.values()) {
           if (i==0) {
               this.CDC_FiatWallet_Text_PrimaData.setText(value.split(",")[0]);
               //verifico ora che la primadata corrisponda con quella del file db salvato
               if (Funzioni_Date_ConvertiDatainLong(value.split(",")[0].split(" ")[0])<Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale))
                   {
               this.CDC_FiatWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in Euro al  "+value.split(",")[0].split(" ")[0]+" (Data Primo Movimento Importato) : ");
               }
               else
                   {
                   this.CDC_FiatWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in Euro al  "+CDC_DataIniziale+" : ");    
                   }
               CDC_FiatWallet_Text_GiacenzaIniziale.setEditable(true);
               if(CDC_FiatWallet_DataSaldoIniziale.equalsIgnoreCase(value.split(",")[0].split(" ")[0]))
                   {
                       //se corrisponde allora prendo il saldo iniziale e lo imposto nella gui
                       this.CDC_FiatWallet_Text_GiacenzaIniziale.setText(CDC_FiatWallet_SaldoIniziale);
                   }
               else
               {
                   //se non corrisponde lascio il saldo iniziale a zero
                   //e aggiorno i dati sul file di configurazione
                   CDC_FiatWallet_SaldoIniziale="0";
                   CDC_FiatWallet_DataSaldoIniziale=value.split(",")[0].split(" ")[0];
                   this.CDC_FiatWallet_Text_GiacenzaIniziale.setText("0");
                   CDC_ScriviFileDatiDB();
                   
               }
                       }
           if (i==CDC_FiatWallet_Mappa.size()-1) this.CDC_FiatWallet_Text_UltimaData.setText(value.split(",")[0]);
           i++;
       }
       
       
       
       
       //adesso trovo la lista dei saldi completa
       
     //    CDC_FiatWallet_ListaSaldi=CDC_FiatWallet_Funzione_CalcolaListaSaldi();
          
                            
           //TROVO GIACENZA MEDIA

           //String DataIniziale="2021-11-23";
           //BigDecimal SaldoIniziale= new BigDecimal(CDC_FiatWallet_SaldoIniziale);
           String Saldi[]=Funzioni_Calcolo_SaldieMedie(CDC_FiatWallet_ListaSaldi,CDC_DataIniziale,CDC_DataFinale,CDC_FiatWallet_SaldoIniziale,CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
           this.CDC_FiatWallet_Text_GiacenzaMedia.setText("€ "+Saldi[2]);
           this.CDC_FiatWallet_Text_SaldoIniziale.setText("€ "+Saldi[0]);
           this.CDC_FiatWallet_Text_SaldoFinale.setText("€ "+Saldi[1]);
           CDC_FiatWallet_Funzione_Totali_per_tipo_movimento();

   } 
    
    private void CDC_CardWallet_AggiornaDatisuGUI() {
       
//       CDC_CardWallet_Text_PeriodoRiferimento.setText(CDC_DataIniziale+"     ->     "+CDC_DataFinale);
       //scrivo le date relative a tutto quello che ho in pancia come dati
       //In questa prima parte recupero i dati essenziali che mi servono poi per i calcoli
       CDC_CardWallet_Mappa.size();
       int i=0;
       for (String value : CDC_CardWallet_Mappa.values()) {
           if (i==0) {
               this.CDC_CardWallet_Text_PrimaData.setText(value.split(",")[0]);
               //verifico ora che la primadata corrisponda con quella del file db salvato
               if (Funzioni_Date_ConvertiDatainLong(value.split(",")[0].split(" ")[0])<Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale))
                   {
               this.CDC_CardWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in Euro al  "+value.split(",")[0].split(" ")[0]+" (Data Primo Movimento Importato) : ");
               }
               else
                   {
                   this.CDC_CardWallet_Label_GiacenzaIniziale.setText("Inserire la giacenza inziale in Euro al  "+CDC_DataIniziale+" : ");    
                   }
               CDC_CardWallet_Text_GiacenzaIniziale.setEditable(true);
               if(CDC_CardWallet_DataSaldoIniziale.equalsIgnoreCase(value.split(",")[0].split(" ")[0]))
                   {
                       //se corrisponde allora prendo il saldo iniziale e lo imposto nella gui
                       this.CDC_CardWallet_Text_GiacenzaIniziale.setText(CDC_CardWallet_SaldoIniziale);
                   }
               else
               {
                   //se non corrisponde lascio il saldo iniziale a zero
                   //e aggiorno i dati sul file di configurazione
                   CDC_CardWallet_SaldoIniziale="0";
                   CDC_CardWallet_DataSaldoIniziale=value.split(",")[0].split(" ")[0];
                   this.CDC_CardWallet_Text_GiacenzaIniziale.setText("0");
                   CDC_ScriviFileDatiDB();
                   
               }
                       }
           if (i==CDC_CardWallet_Mappa.size()-1) this.CDC_CardWallet_Text_UltimaData.setText(value.split(",")[0]);
           i++;
       }
       
       
       
       
       //adesso trovo la lista dei saldi completa
       
        // CDC_CardWallet_ListaSaldi=CDC_CardWallet_Funzione_CalcolaListaSaldi();
          
                            
           //TROVO GIACENZA MEDIA

           //String DataIniziale="2021-11-23";
           //BigDecimal SaldoIniziale= new BigDecimal(CDC_FiatWallet_SaldoIniziale);
           String Saldi[]=Funzioni_Calcolo_SaldieMedie(CDC_CardWallet_ListaSaldi,CDC_DataIniziale,CDC_DataFinale,CDC_CardWallet_SaldoIniziale,CDC_CardWallet_ConsideroValoreMassimoGiornaliero);
           this.CDC_CardWallet_Text_GiacenzaMedia.setText("€ "+Saldi[2]);
           this.CDC_CardWallet_Text_SaldoIniziale.setText("€ "+Saldi[0]);
           this.CDC_CardWallet_Text_SaldoFinale.setText("€ "+Saldi[1]);
           CDC_CardWallet_Funzione_Totali_per_tipo_movimento();

   } 
    
   public List<String>[] CDC_FiatWallet_Funzione_CalcolaListaSaldi() {
       
        this.CDC_FiatWallet_Label_Errore1.setVisible(false);
        this.CDC_FiatWallet_Label_Errore2.setVisible(false);
        this.CDC_FiatWallet_Bottone_Errore.setVisible(false);
        CDC_FiatWallet_MappaErrori.clear();
        int errori=0;
       
       boolean TrovataCorrispondenzaTipo=false;
       List<String> listaSaldi=new ArrayList<>();
       List<String> listaSaldiconMassimoGiornaliero=new ArrayList<>();
            String UltimaData="";
            BigDecimal totale= new BigDecimal("0");
            BigDecimal piccoGiornata=new BigDecimal("0");
            for (String value : CDC_FiatWallet_Mappa.values())
            {
                TrovataCorrispondenzaTipo=false;
                String splittata[]=value.split(",");
                String Data=splittata[0].split(" ")[0];//prendo solo la data e non l'ora
                if (!Data.equalsIgnoreCase(UltimaData)&&!UltimaData.equalsIgnoreCase(""))
                        {                            
                            
                                listaSaldiconMassimoGiornaliero.add(UltimaData+","+piccoGiornata);
                               // System.out.println(UltimaData+","+piccoGiornata);
                                if (Funzioni_Date_ConvertiDatainLong(Data)-Funzioni_Date_ConvertiDatainLong(UltimaData)!=86400000){
                 //QUESTO SERVE PER AGGIUNGERE UNA RIGA CON IL VALORE CORRETTO DEL GIORNO DOPO QUALORA NON VI SIA GIà UN VALORE DA CONSIDERARE
                 //INFATTI SE CONSIDERO IL PICCO MASSIMO , IL PRIMO GIORNO METTO IL PICCO MASSIMO MA QUELLO DOPO DEVO CONSIDERARE IL VALORE NORMALE
                                    listaSaldiconMassimoGiornaliero.add(Funzioni_Date_ConvertiDatadaLong(Funzioni_Date_ConvertiDatainLong(UltimaData)+86400000)+","+totale);

                                }

                                 
                            
                                listaSaldi.add(UltimaData+","+totale);
                               // System.out.println(UltimaData+","+totale);
                                
                            piccoGiornata=totale;
    //                        System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
                        }
                //CDC_FiatWallet_FileTipiMovimentiDB
            int Colonna=CDC_Funzione_trovaColonnaEuro(value);
            if (Colonna!=999){
            for (String tempo : CDC_FiatWallet_MappaTipiMovimenti.values())
            {
                if (splittata[9].trim().equalsIgnoreCase(tempo.split(";")[0].trim()))
                        {
                            TrovataCorrispondenzaTipo=true;
                            if (tempo.split(";")[1].equalsIgnoreCase("+")){
                                
                                totale=totale.add(new BigDecimal(splittata[Colonna]));
                                
                                //System.out.println(totale+" , "+piccoGiornata+" , "+ totale.compareTo(piccoGiornata));
                                if (totale.compareTo(piccoGiornata)>0) piccoGiornata=totale;
                            }
                            else
                             {
                                 totale=totale.subtract(new BigDecimal(splittata[Colonna]));
                             }   
                        }
            }
            }
            if(!TrovataCorrispondenzaTipo)
                {
                    this.CDC_FiatWallet_Label_Errore1.setVisible(true);
                    this.CDC_FiatWallet_Label_Errore2.setVisible(true);
                    this.CDC_FiatWallet_Bottone_Errore.setVisible(true);
                    String TipoErrore;
                    if (Colonna==999){
                        TipoErrore="1";//Movimento non in Euro, non viene contabilizzato
                    }else
                    {
                        TipoErrore="2";//Movimento sconosciuto, non viene contabilizzato per la giacenza media
                    }
                    CDC_FiatWallet_MappaErrori.put(String.valueOf(errori), value+","+TipoErrore);
                    //System.out.println(value+","+TipoErrore);
                    errori++;
                }
 

                    UltimaData=Data;
            }
                          
                                
                                listaSaldiconMassimoGiornaliero.add(UltimaData+","+piccoGiornata);
                                listaSaldiconMassimoGiornaliero.add(Funzioni_Date_ConvertiDatadaLong(Funzioni_Date_ConvertiDatainLong(UltimaData)+86400000)+","+totale);
                            
                                listaSaldi.add(UltimaData+","+totale);
                                
                   List<String>[] group2 = (ArrayList<String>[]) new ArrayList[2];
    group2[0]=listaSaldi;
    group2[1]=listaSaldiconMassimoGiornaliero;
    return group2;
   }
   
    
     private List<String>[] CDC_CardWallet_Funzione_CalcolaListaSaldi() {
      //da rivedere le doppie liste
            
       List<String> listaSaldi=new ArrayList<>();
       List<String> listaSaldiconMassimoGiornaliero=new ArrayList<>();
            String UltimaData="";
            BigDecimal totale= new BigDecimal("0");
            BigDecimal piccoGiornata=new BigDecimal("0");
            for (String value : CDC_CardWallet_Mappa.values())
            {

                
                String splittata[]=value.split(",");
                String Data=splittata[0].split(" ")[0];//prendo solo la data e non l'ora
                if (!Data.equalsIgnoreCase(UltimaData)&&!UltimaData.equalsIgnoreCase(""))
                        {                            
                            
                                listaSaldiconMassimoGiornaliero.add(UltimaData+","+piccoGiornata);
                               // System.out.println(UltimaData+","+piccoGiornata);
                                if (Funzioni_Date_ConvertiDatainLong(Data)-Funzioni_Date_ConvertiDatainLong(UltimaData)!=86400000){
                 //QUESTO SERVE PER AGGIUNGERE UNA RIGA CON IL VALORE CORRETTO DEL GIORNO DOPO QUALORA NON VI SIA GIà UN VALORE DA CONSIDERARE
                 //INFATTI SE CONSIDERO IL PICCO MASSIMO , IL PRIMO GIORNO METTO IL PICCO MASSIMO MA QUELLO DOPO DEVO CONSIDERARE IL VALORE NORMALE
                                    listaSaldiconMassimoGiornaliero.add(Funzioni_Date_ConvertiDatadaLong(Funzioni_Date_ConvertiDatainLong(UltimaData)+86400000)+","+totale);
                                    //System.out.println(ConvertiDatadaLong(ConvertiDatainLong(UltimaData)+86400000)+","+totale);

                                }

                                 
                            
                                listaSaldi.add(UltimaData+","+totale);
                                //System.out.println(UltimaData+","+totale);
                                
                            piccoGiornata=totale;
    //                        System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
                        }
                int Colonna=CDC_Funzione_trovaColonnaEuro(value);
                if (Colonna!=999){
                //se è un valore positivo lo salvo nel picco giornata
                if ((new BigDecimal(splittata[Colonna])).compareTo(BigDecimal.ZERO) > 0)
               // if (splittata[1].contains("EUR Deposit"))
                        {
                            
                                
                                totale=totale.add(new BigDecimal(splittata[Colonna]));
                                
                                //System.out.println(totale+" , "+piccoGiornata+" , "+ totale.compareTo(piccoGiornata));
                                if (totale.compareTo(piccoGiornata)>0) piccoGiornata=totale;
                         }   
                            else
                             {
                                 totale=totale.add(new BigDecimal(splittata[Colonna]));
                                
                             }   
                        
                         }
 

                    UltimaData=Data;
                   
            }
            

          
                                listaSaldiconMassimoGiornaliero.add(UltimaData+","+piccoGiornata);
                                //System.out.println(UltimaData+","+piccoGiornata);
                                listaSaldiconMassimoGiornaliero.add(Funzioni_Date_ConvertiDatadaLong(Funzioni_Date_ConvertiDatainLong(UltimaData)+86400000)+","+totale);
                                //System.out.println(ConvertiDatadaLong(ConvertiDatainLong(UltimaData)+86400000)+","+totale);
                              
                                listaSaldi.add(UltimaData+","+totale);
                                //System.out.println(UltimaData+","+totale);
                                
    //                        System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
    
    List<String>[] group2 = (ArrayList<String>[]) new ArrayList[2];
    group2[0]=listaSaldi;
    group2[1]=listaSaldiconMassimoGiornaliero;
    return group2;
    /*if (conPicco)
    return listaSaldiconMassimoGiornaliero;
    else return listaSaldi;*/
    
   }
   
     private int CDC_Funzione_trovaColonnaEuro(String riga) {
       int colonna=999;
       String splittata[] = riga.split(",");
       //System.out.println(splittata.length);
       if (splittata.length==10 || splittata.length==9) {
           if (splittata[2].trim().equalsIgnoreCase("EUR")){
               colonna=3;
           }else if (splittata[4].trim().equalsIgnoreCase("EUR")){
               colonna=5;
           }else if (splittata[6].trim().equalsIgnoreCase("EUR")){
               colonna=7;
           }
       }
       return colonna;
   }
   
    private void CDC_FiatWallet_Funzione_Totali_per_tipo_movimento() {
        //calcola i totali sui bonifici, topupcarta e acquisti crypto passati per il fiat wallet

        DefaultTableModel CDC_FiatWallet_ModelloTabella1 = (DefaultTableModel) CDC_FiatWallet_Tabella1.getModel();
        DefaultTableModel CDC_FiatWallet_ModelloTabella2 = (DefaultTableModel) CDC_FiatWallet_Tabella2.getModel();
        DefaultTableModel CDC_FiatWallet_ModelloTabella3 = (DefaultTableModel) CDC_FiatWallet_Tabella3.getModel();
        Funzioni_Tabelle_PulisciTabella(CDC_FiatWallet_ModelloTabella1);
        Funzioni_Tabelle_PulisciTabella(CDC_FiatWallet_ModelloTabella2);
        Funzioni_Tabelle_PulisciTabella(CDC_FiatWallet_ModelloTabella3);

        Map<String, String> CDC_FiatWallet_MappaCausali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> CDC_FiatWallet_Descrizioni = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String SaldoIniziale=this.CDC_FiatWallet_Text_GiacenzaIniziale.getText().replace("€", "").trim();
        BigDecimal Totale=new BigDecimal(SaldoIniziale);
        for (String value : CDC_FiatWallet_Mappa.values()) {

            String splittata[] = value.split(",");
            String Data = splittata[0].split(" ")[0];//prendo solo la data e non l'ora
            
            if (Funzioni_Date_ConvertiDatainLong(Data) >= Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale) && Funzioni_Date_ConvertiDatainLong(Data) <= Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {    //CDC_FiatWallet_FileTipiMovimentiDB
                boolean trovato = false;
                Object CDC_FiatWallet_RigaTabella2[]=new Object[5];
                int Colonna=CDC_Funzione_trovaColonnaEuro(value);
                if (Colonna!=999){
                for (String tempo : CDC_FiatWallet_MappaTipiMovimenti.values()) {   
                   // String Colonna = tempo.split(";")[2];
                   // String Segno = tempo.split(";")[1];
                    String Descrizione = tempo.split(";")[3];
                    String DescrizioneOriginale = tempo.split(";")[0];
                    if (splittata[9].trim().equalsIgnoreCase(DescrizioneOriginale.trim())) {
                        trovato = true;
                        CDC_FiatWallet_RigaTabella2[0]=splittata[0];
                        CDC_FiatWallet_RigaTabella2[1]=Descrizione;
                        CDC_FiatWallet_RigaTabella2[2]=splittata[1];
                        CDC_FiatWallet_RigaTabella2[3]=Double.valueOf(splittata[Colonna]);
                        BigDecimal Adde2 = new BigDecimal(tempo.split(";")[1]+splittata[Colonna]);
                        Totale=Totale.add(Adde2);
                        CDC_FiatWallet_RigaTabella2[4]=Double.valueOf(Totale.toString());
                        if (CDC_FiatWallet_MappaCausali.get(Descrizione) == null) {
                            // se è una controparte nuova allora la aggiungo alla mappa
                            // altrimenti la aggiorno con il valore corretto
                            CDC_FiatWallet_MappaCausali.put(Descrizione, splittata[Colonna]);
                           // CDC_FiatWallet_RigaTabella2[4]=Double.parseDouble(splittata[Colonna]);
                        } else {
                            BigDecimal Addendo1 = new BigDecimal(CDC_FiatWallet_MappaCausali.get(Descrizione));
                            BigDecimal Addendo2 = new BigDecimal(splittata[Colonna]);
                            String Somma = Addendo1.add(Addendo2).toString();
                            //CDC_FiatWallet_RigaTabella2[4]=Double.parseDouble(Somma);
                            CDC_FiatWallet_MappaCausali.put(Descrizione, Somma);
                        }

                    }
                }
                // se non trovo la decodifica faccio le somme tenendo come descrizione la colonna 9
                if (!trovato) {
                    CDC_FiatWallet_RigaTabella2[0]=splittata[0];
                    CDC_FiatWallet_RigaTabella2[1]=splittata[9];
                    CDC_FiatWallet_RigaTabella2[2]=splittata[1];
                    CDC_FiatWallet_RigaTabella2[3]=Double.valueOf(splittata[Colonna]);
                    if (CDC_FiatWallet_MappaCausali.get(splittata[9]) == null) {
                        // se è una controparte nuova allora la aggiungo alla mappa
                        // altrimenti la aggiorno con il valore corretto
                        CDC_FiatWallet_MappaCausali.put(splittata[9], splittata[Colonna]);
                        //CDC_FiatWallet_RigaTabella2[4]=Double.parseDouble(splittata[Colonna]);
                    } else {
                        BigDecimal Addendo1 = new BigDecimal(CDC_FiatWallet_MappaCausali.get(splittata[9]));
                        BigDecimal Addendo2 = new BigDecimal(splittata[Colonna]);
                        String Somma = Addendo1.add(Addendo2).toString();
                       // CDC_FiatWallet_RigaTabella2[4]=Double.parseDouble(Somma);
                        CDC_FiatWallet_MappaCausali.put(splittata[9], Somma);
                    }
                }
                CDC_FiatWallet_ModelloTabella2.addRow(CDC_FiatWallet_RigaTabella2);
                //creo movimenti tabella 3
                if (CDC_FiatWallet_Descrizioni.get(splittata[1]) == null) {
                        // se è una controparte nuova allora la aggiungo alla mappa
                        // altrimenti la aggiorno con il valore corretto
                        CDC_FiatWallet_Descrizioni.put(splittata[1], splittata[Colonna]);
                    } else {
                        BigDecimal Addendo1 = new BigDecimal(CDC_FiatWallet_Descrizioni.get(splittata[1]));
                        BigDecimal Addendo2 = new BigDecimal(splittata[Colonna]);
                        String Somma = Addendo1.add(Addendo2).toString();
                        CDC_FiatWallet_Descrizioni.put(splittata[1], Somma);
                    }
                
                }else{
                //errore, non ho trovato movimentazioni in euro
                    
                }
                    
               // CDC_FiatWallet_ModelloTabella2.addRow(CDC_FiatWallet_RigaTabella2);
            }
        }

        for (String key : CDC_FiatWallet_MappaCausali.keySet()) {
            Object CDC_FiatWallet_RigaTabella1[] = new Object[2];
            CDC_FiatWallet_RigaTabella1[0] = key;
            CDC_FiatWallet_RigaTabella1[1] = Double.valueOf(CDC_FiatWallet_MappaCausali.get(key));
            CDC_FiatWallet_ModelloTabella1.addRow(CDC_FiatWallet_RigaTabella1);
        }
        for (String key : CDC_FiatWallet_Descrizioni.keySet()) {
           // System.out.println(Double.valueOf(CDC_FiatWallet_Descrizioni.get(key)));
            Object CDC_FiatWallet_RigaTabella3[] = new Object[2];
            CDC_FiatWallet_RigaTabella3[0] = key;
            CDC_FiatWallet_RigaTabella3[1] = Double.valueOf(CDC_FiatWallet_Descrizioni.get(key));
            CDC_FiatWallet_ModelloTabella3.addRow(CDC_FiatWallet_RigaTabella3);
        }

    }
   
   
     public static void Funzioni_Tabelle_PulisciTabella(DefaultTableModel modello) {
           int z=modello.getRowCount();
        // System.out.println(modelProblemi.getRowCount());
         while (z!=0){
             modello.removeRow(0);
             z=modello.getRowCount();
         }
         
  }
   
     

     
 
     
     
        public void Funzioni_Tabelle_FiltraTabella(JTable Tabella, String filtro, int colonna) {

        TableRowSorter<TableModel> sorter = new TableRowSorter<>((DefaultTableModel) Tabella.getModel());
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filtro));
        //se metto 999 significa che non voglio venga riordinato niente
        if (colonna != 999) {
            sorter.toggleSortOrder(colonna);
        }
        Tabella.setRowSorter(sorter);

    }
     
     
   
      private void CDC_CardWallet_Funzione_Totali_per_tipo_movimento() {
          
        

        DefaultTableModel CDC_CardWallet_ModelloTabella1 = (DefaultTableModel) CDC_CardWallet_Tabella1.getModel();
        DefaultTableModel CDC_CardWallet_ModelloTabella2 = (DefaultTableModel) CDC_CardWallet_Tabella2.getModel();
      //  DefaultTableModel CDC_CardWallet_ModelloTabella2 = (DefaultTableModel) model;
      //  CDC_CardWallet_Tabella2.setModel(model);
        
        Funzioni_Tabelle_PulisciTabella(CDC_CardWallet_ModelloTabella2);
        Funzioni_Tabelle_PulisciTabella(CDC_CardWallet_ModelloTabella1);
          
       //calcola i totali sui bonifici, topupcarta e acquisti crypto passati per il fiat wallet
        BigDecimal TotaleSpese= new BigDecimal("0");
        BigDecimal TotaleTopUpCarta= new BigDecimal("0"); 
        String SaldoIniziale=this.CDC_CardWallet_Text_GiacenzaIniziale.getText().replace("€", "").trim();
        BigDecimal Totale=new BigDecimal(SaldoIniziale);
        Map<String, String> CDC_CardWallet_MappaCausali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String value : CDC_CardWallet_Mappa.values())
            {
                
                String splittata[]=value.split(",");
                String Data=splittata[0].split(" ")[0];//prendo solo la data e non l'ora
                if (Funzioni_Date_ConvertiDatainLong(Data)>=Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale)&&Funzioni_Date_ConvertiDatainLong(Data)<=Funzioni_Date_ConvertiDatainLong(CDC_DataFinale))

                {    
                    int Colonna=CDC_Funzione_trovaColonnaEuro(value);
                    if (Colonna!=999){
                    //CDC_FiatWallet_FileTipiMovimentiDB
                   // System.out.println(CDC_CardWallet_MappaCausali.get(splittata[1]));
                    if (CDC_CardWallet_MappaCausali.get(splittata[1])==null) {
                        // se è una controparte nuova allora la aggiungo alla mappa
                        // altrimenti la aggiorno con il valore corretto
                        CDC_CardWallet_MappaCausali.put(splittata[1], splittata[Colonna]);
                    }
                    else
                    {
                        BigDecimal Addendo1= new BigDecimal(CDC_CardWallet_MappaCausali.get(splittata[1]));
                        BigDecimal Addendo2= new BigDecimal(splittata[Colonna]);
                        String Somma=Addendo1.add(Addendo2).toString();
                        CDC_CardWallet_MappaCausali.put(splittata[1], Somma);
                    }
                    Object CDC_CardWallet_RigaTabella2[]=new Object[4];
                    CDC_CardWallet_RigaTabella2[0]=splittata[0];
                    CDC_CardWallet_RigaTabella2[1]=splittata[1];
                    CDC_CardWallet_RigaTabella2[2]=Double.valueOf(splittata[Colonna]);
                    BigDecimal Adde2 = new BigDecimal(splittata[Colonna]);
                    Totale=Totale.add(Adde2);
                    CDC_CardWallet_RigaTabella2[3]=Double.valueOf(Totale.toString());
                    CDC_CardWallet_ModelloTabella2.addRow(CDC_CardWallet_RigaTabella2);

                    if ((new BigDecimal(splittata[3])).compareTo(BigDecimal.ZERO) > 0)
                    
                            {
                                TotaleTopUpCarta=TotaleTopUpCarta.add(new BigDecimal(splittata[Colonna]));
                            }
                    else
                            {
                                TotaleSpese=TotaleSpese.add(new BigDecimal(splittata[Colonna]));
                            }       
  }
                }
            }
            for (String key : CDC_CardWallet_MappaCausali.keySet())
            {
                Object CDC_CardWallet_RigaTabella1[]=new Object[2];
                CDC_CardWallet_RigaTabella1[0]=key;
                CDC_CardWallet_RigaTabella1[1]=Double.parseDouble(CDC_CardWallet_MappaCausali.get(key));
                CDC_CardWallet_ModelloTabella1.addRow(CDC_CardWallet_RigaTabella1);
            }
            this.CDC_CardWallet_Text_Spese.setText("€ "+TotaleSpese.multiply(new BigDecimal ("-1")).toString());
            this.CDC_CardWallet_Text_Entrate.setText("€ "+TotaleTopUpCarta.toString());
            //CDC_CardWallet_Tabella1.getRowSorter().toggleSortOrder(1);
            //CDC_CardWallet_Tabella1.getRowSorter().toggleSortOrder(1);
           // ColoraRigaTabellaShadow(CDC_CardWallet_Tabella2);
                                  
   }
      

   
    private void CDC_FiatWallet_Funzione_Scrivi() {
         try { 
       FileWriter w=new FileWriter(CDC_FiatWallet_FileDB);
       BufferedWriter b=new BufferedWriter (w);

       for (String value : CDC_FiatWallet_Mappa.values()) {
           b.write(value+"\n");
          // System.out.println(value);
       }
       b.close();
       w.close();
    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
    }
    
        private void CDC_CardWallet_Funzione_Scrivi() {
         try { 
       FileWriter w=new FileWriter(CDC_CardWallet_FileDB);
       BufferedWriter b=new BufferedWriter (w);

       for (String value : CDC_CardWallet_Mappa.values()) {
           b.write(value+"\n");
          // System.out.println(value);
       }
       b.close();
       w.close();
    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
    }
    
    
    private void CDC_DataChooser_InizialePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_CDC_DataChooser_InizialePropertyChange
        // TODO add your handling code here:
       // System.out.println(CDC_DataChooser_Iniziale.getDate());
        if (CDC_DataChooser_Iniziale.getDate()!=null){
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            String Data=f.format(CDC_DataChooser_Iniziale.getDate());
            if (!Data.equalsIgnoreCase(CDC_DataIniziale)&&Funzioni_Date_ConvertiDatainLong(Data)<=Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)){
                CDC_DataIniziale=Data;
                this.CDC_ScriviFileDatiDB();
                CDC_AggiornaGui(); 
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            }
            else if(Funzioni_Date_ConvertiDatainLong(Data)>Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {
                try {
                    //f.parse(CDC_DataIniziale)
                    CDC_DataChooser_Iniziale.setDate(f.parse(CDC_DataIniziale));
                    JOptionPane.showConfirmDialog(this, "Attenzione, la data iniziale non può essere maggiore della data finale!",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                } catch (ParseException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
                    

       // CDC_DataChooser_Iniziale.getDate()
    }//GEN-LAST:event_CDC_DataChooser_InizialePropertyChange

    private void CDC_DataChooser_FinalePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_CDC_DataChooser_FinalePropertyChange
        // TODO add your handling code here:
                if (CDC_DataChooser_Finale.getDate()!=null){
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            String Data=f.format(CDC_DataChooser_Finale.getDate());
            if (!Data.equalsIgnoreCase(CDC_DataFinale)&&Funzioni_Date_ConvertiDatainLong(Data)>=Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale)){
                CDC_DataFinale=Data;
                this.CDC_ScriviFileDatiDB();
                CDC_AggiornaGui();
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                
              
            }
            else if(Funzioni_Date_ConvertiDatainLong(Data)<Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale)) {
                try {
                    //f.parse(CDC_DataIniziale)
                    CDC_DataChooser_Finale.setDate(f.parse(CDC_DataFinale));
                    JOptionPane.showConfirmDialog(this, "Attenzione, la data finale non può essere minore della data iniziale!",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                } catch (ParseException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_CDC_DataChooser_FinalePropertyChange

    private void CDC_FiatWallet_Checkbox_ConsideraValoreMaggioreMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Checkbox_ConsideraValoreMaggioreMouseClicked
        // TODO add your handling code here:
        CDC_FiatWallet_ConsideroValoreMassimoGiornaliero=this.CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore.isSelected();
        CDC_ScriviFileDatiDB();
        CDC_FiatWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_FiatWallet_Checkbox_ConsideraValoreMaggioreMouseClicked

    private void CDC_FiatWallet_Text_GiacenzaInizialeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Text_GiacenzaInizialeKeyReleased
        // TODO add your handling code here:

        if (!CDC_FiatWallet_SaldoIniziale.equalsIgnoreCase(this.CDC_FiatWallet_Text_GiacenzaIniziale.getText())&&Funzioni_isNumeric(this.CDC_FiatWallet_Text_GiacenzaIniziale.getText())){
            CDC_FiatWallet_SaldoIniziale=this.CDC_FiatWallet_Text_GiacenzaIniziale.getText().replaceFirst("^0+(?!$)", "");

            //if (CDC_FiatWallet_Text_GiacenzaIniziale.getText().equalsIgnoreCase("")) CDC_FiatWallet_SaldoIniziale="0";
            this.CDC_ScriviFileDatiDB();

        }
        else CDC_FiatWallet_Text_GiacenzaIniziale.setText(CDC_FiatWallet_SaldoIniziale.replaceFirst("^0+(?!$)", ""));

        CDC_FiatWallet_AggiornaDatisuGUI();

    }//GEN-LAST:event_CDC_FiatWallet_Text_GiacenzaInizialeKeyReleased

    private void CDC_FiatWallet_Text_UltimaDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Text_UltimaDataActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CDC_FiatWallet_Text_UltimaDataActionPerformed

    private void CDC_FiatWallet_Bottone_CaricaCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Bottone_CaricaCSVActionPerformed
        // TODO add your handling code here:
        //Create a file chooser
        JFileChooser fc = new JFileChooser();

        //In response to a button click:
        int returnVal = fc.showOpenDialog(CDC_FiatWallet_Pannello);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            String fiatwallet=fc.getSelectedFile().getAbsolutePath();
            CDC_FiatWallet_Funzione_ImportaWallet(fiatwallet);

            //ImportaFiatWallet("C:\\Users\\luca.passelli\\Desktop\\fiat_transactions_record_20220110_144004.csv");
            CDC_FiatWallet_Funzione_Scrivi();
            CDC_FiatWallet_AggiornaDatisuGUI();

            //   TextFiatWallet.setText(fc.getSelectedFile().getAbsolutePath());

        } else {
        }
    }//GEN-LAST:event_CDC_FiatWallet_Bottone_CaricaCSVActionPerformed

    private void CDC_CardWallet_Text_FiltroTabelleKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Text_FiltroTabelleKeyReleased
        // TODO add your handling code here:
        this.Funzioni_Tabelle_FiltraTabella(CDC_CardWallet_Tabella1, CDC_CardWallet_Text_FiltroTabelle.getText(), 999);
        this.Funzioni_Tabelle_FiltraTabella(CDC_CardWallet_Tabella2, CDC_CardWallet_Text_FiltroTabelle.getText(), 999);
    }//GEN-LAST:event_CDC_CardWallet_Text_FiltroTabelleKeyReleased

    private void CDC_CardWallet_Checkbox_ConsideraValoreMaggioreMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Checkbox_ConsideraValoreMaggioreMouseClicked
        // TODO add your handling code here:
        CDC_CardWallet_ConsideroValoreMassimoGiornaliero=this.CDC_CardWallet_Checkbox_ConsideraValoreMaggiore.isSelected();
        CDC_ScriviFileDatiDB();
        CDC_CardWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_CardWallet_Checkbox_ConsideraValoreMaggioreMouseClicked

    private void CDC_CardWallet_Text_GiacenzaInizialeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Text_GiacenzaInizialeKeyReleased
        // TODO add your handling code here:
        if (!CDC_CardWallet_SaldoIniziale.equalsIgnoreCase(this.CDC_CardWallet_Text_GiacenzaIniziale.getText())&&Funzioni_isNumeric(this.CDC_CardWallet_Text_GiacenzaIniziale.getText())){
            if (CDC_CardWallet_Text_GiacenzaIniziale.getText().equalsIgnoreCase(""))CDC_CardWallet_Text_GiacenzaIniziale.setText("0");
            CDC_CardWallet_SaldoIniziale=this.CDC_CardWallet_Text_GiacenzaIniziale.getText().replaceFirst("^0+(?!$)", "");

            //if (CDC_FiatWallet_Text_GiacenzaIniziale.getText().equalsIgnoreCase("")) CDC_FiatWallet_SaldoIniziale="0";
            this.CDC_ScriviFileDatiDB();

        }
        else CDC_CardWallet_Text_GiacenzaIniziale.setText(CDC_CardWallet_SaldoIniziale.replaceFirst("^0+(?!$)", ""));

        CDC_CardWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_CardWallet_Text_GiacenzaInizialeKeyReleased

    private void CDC_CardWallet_Text_UltimaDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Text_UltimaDataActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CDC_CardWallet_Text_UltimaDataActionPerformed

    private void CDC_CardWallet_Bottone_CaricaCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Bottone_CaricaCSVActionPerformed
        // TODO add your handling code here:
        // TODO add your handling code here:
        //Create a file chooser
        JFileChooser fc = new JFileChooser();

        //In response to a button click:
        int returnVal = fc.showOpenDialog(CDC_CardWallet_Pannello);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            String cardwallet=fc.getSelectedFile().getAbsolutePath();
            CDC_CardWallet_Funzione_ImportaWallet(cardwallet);

            //DA INSERIRE LE 2 RIGHE SOTTO
            CDC_CardWallet_Funzione_Scrivi();
            CDC_CardWallet_AggiornaDatisuGUI();

            //   TextFiatWallet.setText(fc.getSelectedFile().getAbsolutePath());

        } else {
        }
    }//GEN-LAST:event_CDC_CardWallet_Bottone_CaricaCSVActionPerformed

    private void CDC_FiatWallet_Text_FiltroTabellaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Text_FiltroTabellaKeyReleased
        // TODO add your handling code here:
        this.Funzioni_Tabelle_FiltraTabella(CDC_FiatWallet_Tabella2, CDC_FiatWallet_Text_FiltroTabella.getText(), 999);
        this.Funzioni_Tabelle_FiltraTabella(CDC_FiatWallet_Tabella3, CDC_FiatWallet_Text_FiltroTabella.getText(), 999);
    }//GEN-LAST:event_CDC_FiatWallet_Text_FiltroTabellaKeyReleased

    private void CDC_FiatWallet_Bottone_ErroreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Bottone_ErroreActionPerformed
        // TODO add your handling code here:
                Gestione_Errori mod = new Gestione_Errori();
                mod.CompilaTabellaErrori(CDC_FiatWallet_MappaErrori);
                mod.setLocationRelativeTo(this);
                mod.setVisible(true);
                CDC_FiatWallet_Funzione_ImportaWallet(CDC_FiatWallet_FileDB);
                CDC_FiatWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_FiatWallet_Bottone_ErroreActionPerformed

    private void CDC_FiatWallet_Bottone_StampaRapportoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_FiatWallet_Bottone_StampaRapportoActionPerformed
        try {
            // TODO add your handling code here:
           
            
            
            //Tabella Totali
            Stampe stampa=new Stampe("temp.pdf");
            String DataIniziale=CDC_DataIniziale.split("-")[2]+"/"+CDC_DataIniziale.split("-")[1]+"/"+CDC_DataIniziale.split("-")[0];
            String DataFinale=CDC_DataFinale.split("-")[2]+"/"+CDC_DataFinale.split("-")[1]+"/"+CDC_DataFinale.split("-")[0];
            //String piede="Stampa generata da "+this.getTitle()+"  - https://sourceforge.net/projects/giacenze-crypto-com";
            String piede="REPORT FIAT WALLET  -  Periodi di competenza : "+DataIniziale+" - "+DataFinale+" ";
            stampa.Piede(piede);
            stampa.ApriDocumento();
            stampa.AggiungiTesto("     REPORT FIAT WALLET di Crypto.com\n\n",Font.BOLD,20);
            stampa.AggiungiTesto("Periodo di riferimento        : "+DataIniziale+" - "+DataFinale+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Numero di giorni del periodo  : "+this.CDC_Text_Giorni.getText()+"\n\n",Font.BOLD,12);
            stampa.AggiungiTesto("Saldo Iniziale al "+DataIniziale.trim()+"        :  "+this.CDC_FiatWallet_Text_SaldoIniziale.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Saldo Finale al "+DataFinale.trim()+"          :  "+this.CDC_FiatWallet_Text_SaldoFinale.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Giacenza media del periodo          :  "+this.CDC_FiatWallet_Text_GiacenzaMedia.getText()+"\n\n\n",Font.BOLD,12);
            stampa.NuovaPagina();
            stampa.AggiungiTesto("TABELLA TOTALI\n",Font.UNDERLINE,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            List<String[]> tabella1=Funzioni_Tabelle_ListaTabella(CDC_FiatWallet_Tabella1);
            String Titoli1[]=new String[]{"Tipologia","Valore in Euro"};
            stampa.AggiungiTabella(Titoli1,tabella1);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.NuovaPagina();
            stampa.AggiungiTesto("TABELLA TOTALI PER DETTAGLIO\n",Font.UNDERLINE,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            tabella1=Funzioni_Tabelle_ListaTabella(CDC_FiatWallet_Tabella3);
            Titoli1=new String[]{"Tipologia","Valore in Euro"};
            stampa.AggiungiTabella(Titoli1,tabella1);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.NuovaPagina();
            stampa.AggiungiTesto("TABELLA MOVIMENTI\n",Font.UNDERLINE,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            tabella1=Funzioni_Tabelle_ListaTabella(CDC_FiatWallet_Tabella2);
            Titoli1=new String[]{"Data","Causale","Dettaglio","Valore in Euro","Rimanenze"};
            stampa.AggiungiTabella(Titoli1,tabella1);
            stampa.ScriviPDF();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_CDC_FiatWallet_Bottone_StampaRapportoActionPerformed

    private void CDC_CardWallet_Bottone_StampaRapportoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_CardWallet_Bottone_StampaRapportoActionPerformed
        // TODO add your handling code here:
                try {
            // TODO add your handling code here:
           
            
            
            //Tabella Totali
            Stampe stampa=new Stampe("temp.pdf");
            String DataIniziale=CDC_DataIniziale.split("-")[2]+"/"+CDC_DataIniziale.split("-")[1]+"/"+CDC_DataIniziale.split("-")[0];
            String DataFinale=CDC_DataFinale.split("-")[2]+"/"+CDC_DataFinale.split("-")[1]+"/"+CDC_DataFinale.split("-")[0];
            //String piede="Stampa generata da "+this.getTitle()+"  - https://sourceforge.net/projects/giacenze-crypto-com";
            String piede="REPORT CARD WALLET  -  Periodi di competenza : "+DataIniziale+" - "+DataFinale+" ";
            stampa.Piede(piede);
            stampa.ApriDocumento();
           // stampa.AggiungiTitolo("Titolo");
            stampa.AggiungiTesto("      REPORT CARD WALLET di Crypto.com\n\n",Font.BOLD,20);
            stampa.AggiungiTesto("Periodo di riferimento        : "+DataIniziale+" - "+DataFinale+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Numero di giorni del periodo  : "+this.CDC_Text_Giorni.getText()+"\n\n",Font.BOLD,12);
            stampa.AggiungiTesto("Saldo Iniziale al "+DataIniziale.trim()+"         :  "+this.CDC_CardWallet_Text_SaldoIniziale.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Saldo Finale al "+DataFinale.trim()+"           :  "+this.CDC_CardWallet_Text_SaldoFinale.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Giacenza media del periodo           :  "+this.CDC_CardWallet_Text_GiacenzaMedia.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Totale Spese (Uscite)                :  "+this.CDC_CardWallet_Text_Spese.getText()+"\n",Font.BOLD,12);
            stampa.AggiungiTesto("Totale Ricariche/Rimborsi (Entrate)  :  "+this.CDC_CardWallet_Text_Entrate.getText()+"\n\n\n",Font.BOLD,12);
            stampa.NuovaPagina();
            stampa.AggiungiTesto("TABELLA TOTALI RAGGRUPPATA PER CONTROPARTE/CAUSALE IN ORDINE ALFABETICO\n",Font.UNDERLINE,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            List<String[]> tabella1=Funzioni_Tabelle_ListaTabella(CDC_CardWallet_Tabella1);
            String Titoli1[]=new String[]{"Controparte/Causale","Valore in Euro"};
            stampa.AggiungiTabella(Titoli1,tabella1);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            stampa.NuovaPagina();
            stampa.AggiungiTesto("TABELLA DETTAGLIO IN ORDINE DI DATA\n",Font.UNDERLINE,12);
            stampa.AggiungiTesto("\n",Font.NORMAL,12);
            tabella1=Funzioni_Tabelle_ListaTabella(CDC_CardWallet_Tabella2);
            Titoli1=new String[]{"Data","Controparte/Causale","Valore in Euro","Rimanenze in Euro"};
            stampa.AggiungiTabella(Titoli1,tabella1);
            stampa.ScriviPDF();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_CDC_CardWallet_Bottone_StampaRapportoActionPerformed

    private void TransazioniCrypto_Bottone_ImportaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_ImportaActionPerformed
        // TODO add your handling code here:
       // Calcoli.GeneraMappaCambioUSDEUR();
       // Importazioni.Formatta_Data_CoinTracking("11.07.2022 20:12");

///////       Importazioni.Importa_Crypto_CDCApp();
///////       CaricaTabellaCrypto();
       
        Importazioni_Gestione gest = new Importazioni_Gestione();
        gest.setLocationRelativeTo(this);
        gest.setVisible(true);
        
        TransazioniCrypto_Funzioni_PulisciMovimentiAssociatinonEsistenti();
        TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());

       
        //questo sotto serve per aumentare la diomensione dell'header della tabella
        //Calcoli.RecuperaTassidiCambio();
    }//GEN-LAST:event_TransazioniCrypto_Bottone_ImportaActionPerformed

    private void TransazioniCryptoTabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TransazioniCryptoTabellaMouseReleased
        // TODO add your handling code here:
        TransazioniCrypto_CompilaTextPaneDatiMovimento();
    }//GEN-LAST:event_TransazioniCryptoTabellaMouseReleased

    private void TransazioniCryptoTabellaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TransazioniCryptoTabellaKeyReleased
        // TODO add your handling code here:
        TransazioniCrypto_CompilaTextPaneDatiMovimento();
    }//GEN-LAST:event_TransazioniCryptoTabellaKeyReleased

    private void TransazioniCrypto_Bottone_SalvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_SalvaActionPerformed
        // TODO add your handling code here:
        Importazioni.Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        this.TransazioniCrypto_Bottone_Salva.setEnabled(false);
        TransazioniCrypto_Bottone_Annulla.setEnabled(false);
        this.TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(false);
        Importazioni.TransazioniAggiunte=0;

    }//GEN-LAST:event_TransazioniCrypto_Bottone_SalvaActionPerformed

    private void Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed
        // TODO add your handling code here:
        // TODO add your handling code here:
        String Messaggio = "Sicuro di voler cancellare tutti i dati delle Transazioni Crypto?";
        int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
        if (risposta == 0) {
            MappaCryptoWallet.clear();
            TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_Bottone_Annulla.setEnabled(true);
            TransazioniCrypto_Bottone_Salva.setEnabled(true);
            TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
            Messaggio = "Sono state cancellate tutte le movimentazioni crypto \nRicordarsi di Salvare per non perdere le modifiche fatte.";
            JOptionPane.showOptionDialog(this, Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK"}, "OK");
        }

    }//GEN-LAST:event_Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed

    private void CDC_Opzioni_Bottone_CancellaCardWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_Opzioni_Bottone_CancellaCardWalletActionPerformed
        // TODO add your handling code here:
        String Messaggio="Sicuro di voler cancellare tutti i dati del Card Wallet?";
        int risposta=JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione CardWallet", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
        if (risposta==0){
            try
            {
                FileWriter w=new FileWriter(CDC_Grafica.CDC_CardWallet_FileDB);
                BufferedWriter b=new BufferedWriter (w);
                b.write("");
                b.close();
                w.close();
            }catch (IOException ex)
            {

            }    }
            CDC_CardWallet_Mappa.clear();
            CDC_CardWallet_Funzione_ImportaWallet(CDC_CardWallet_FileDB);
            CDC_CardWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_Opzioni_Bottone_CancellaCardWalletActionPerformed

    private void CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWalletActionPerformed
        // TODO add your handling code here:
        String Messaggio="Sicuro di voler cancellare le personalizzazione sui movimenti del Fiat Wallet?";
        int risposta=JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione personalizzazioni FiatWallet", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
        if (risposta==0){
            try
            {
                FileWriter w=new FileWriter(CDC_Grafica.CDC_FiatWallet_FileTipiMovimentiDBPers);
                BufferedWriter b=new BufferedWriter (w);
                for (String value : CDC_Grafica.CDC_FiatWallet_MappaTipiMovimenti.values())
                {
                    if (!value.toUpperCase().contains(";Personalizzato;".toUpperCase()))
                    {
                        b.write(value+"\n");
                    }
                }
                b.close();
                w.close();
            }catch (IOException ex)
            {

            }    }
            CDC_FiatWallet_Funzione_ImportaWallet(CDC_FiatWallet_FileDB);
            CDC_FiatWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWalletActionPerformed

    private void CDC_Opzioni_Bottone_CancellaFiatWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CDC_Opzioni_Bottone_CancellaFiatWalletActionPerformed
        // TODO add your handling code here:
        String Messaggio="Sicuro di voler cancellare tutti i dati del Fiat Wallet?";
        int risposta=JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione FiatWallet", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
        if (risposta==0){
            try
            {
                FileWriter w=new FileWriter(CDC_Grafica.CDC_FiatWallet_FileDB);
                BufferedWriter b=new BufferedWriter (w);
                b.write("");
                b.close();
                w.close();
            }catch (IOException ex)
            {

            }    }
            CDC_FiatWallet_Mappa.clear();
            CDC_FiatWallet_Funzione_ImportaWallet(CDC_FiatWallet_FileDB);
            CDC_FiatWallet_AggiornaDatisuGUI();
    }//GEN-LAST:event_CDC_Opzioni_Bottone_CancellaFiatWalletActionPerformed

    private void TransazioniCryptoFiltro_TextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TransazioniCryptoFiltro_TextKeyReleased
        // TODO add your handling code here:
        this.Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }//GEN-LAST:event_TransazioniCryptoFiltro_TextKeyReleased

    private void TransazioniCrypto_CheckBox_EscludiTIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_CheckBox_EscludiTIActionPerformed
        // TODO add your handling code here:
        //disabilito il filtro prima dell'eleaborazioneper velocizzare il tutto
        //il filtro altrimenti viene applicato ogni volta che aggiungo una riga in tabella e rallenta tantissimo
   //     this.FiltraTabella(TransazioniCryptoTabella, "", 999);
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
   //     this.FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }//GEN-LAST:event_TransazioniCrypto_CheckBox_EscludiTIActionPerformed

    private void TransazioniCrypto_Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_AnnullaActionPerformed
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {

            // TODO add your handling code here:
            this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_Bottone_Salva.setEnabled(false);
            TransazioniCrypto_Bottone_Annulla.setEnabled(false);
            TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(false);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.setCursor(Cursor.getDefaultCursor());
    }//GEN-LAST:event_TransazioniCrypto_Bottone_AnnullaActionPerformed

    private void Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed
        // TODO add your handling code here:
        if(Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedIndex()!=0) {
            
                    String Messaggio="Sicuro di voler cancellare tutti i dati delle Transazioni Crypto del Wallet "+Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedItem().toString()+"?";
        int risposta=JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
         if (risposta==0)
        {
            Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, "", 999);
            int movimentiCancellati=Funzioni_CancellaMovimentazioniWallet(Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedItem().toString());
            if (movimentiCancellati>0){
               Opzioni_RicreaListaWalletDisponibili();
                TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                TransazioniCrypto_Bottone_Annulla.setEnabled(true);
                TransazioniCrypto_Bottone_Salva.setEnabled(true);
                TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
                }
        Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
        Messaggio="Numero movimenti cancellati : "+movimentiCancellati+ "\n Ricordarsi di Salvare per non perdere le modifiche fatte.";
        JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK"}, "OK");
     
        
        }
            }
    }//GEN-LAST:event_Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed

    
    public static int Funzioni_CancellaMovimentazioniWallet(String Wallet){
         
        int movimentiCancellati=0;
       //this.TransazioniCryptoFiltro_Text.setText("");
        //questo server per velocizzare la ricerca
        //disabilito il filtro e poi lo riabilito finito l'eleaborazione
        
        List<String> Cancellare=new ArrayList<>();
        

        for (String v : MappaCryptoWallet.keySet()) {
            if (MappaCryptoWallet.get(v)[3].trim().equalsIgnoreCase(Wallet.trim()))
            {
                //MappaCryptoWallet.remove(v);
                Cancellare.add(v);
                movimentiCancellati++;
            }
        }
        Iterator I=Cancellare.iterator();
        while (I.hasNext()){
            String daRimuovere=I.next().toString();
            String controparte[]=MappaCryptoWallet.get(daRimuovere);
            if (controparte[20]!=null && !controparte[20].equalsIgnoreCase("")){
                ClassificazioneTrasf_Modifica.RiportaIDTransaSituazioneIniziale(controparte[20]);
            }
            MappaCryptoWallet.remove(daRimuovere);
        }
        
           // MappaCryptoWallet.clear();
   
        return movimentiCancellati;
    }
    
    private void CDC_OpzioniComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_CDC_OpzioniComponentShown
        // TODO add your handling code here:
        Opzioni_RicreaListaWalletDisponibili();

    }//GEN-LAST:event_CDC_OpzioniComponentShown

    public void Opzioni_RicreaListaWalletDisponibili(){ 
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.removeAllItems();
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.addItem("----------");
          Mappa_Wallet.clear();
          for (String[] v : MappaCryptoWallet.values()) {
                Mappa_Wallet.put(v[3], v[1]);
          }
          for (String v : Mappa_Wallet.keySet()) {
              this.Opzioni_Combobox_CancellaTransazioniCryptoXwallet.addItem(v);
          }
    }
    
    
    private void DepositiPrelievi_Bottone_AssegnazioneManualeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DepositiPrelievi_Bottone_AssegnazioneManualeActionPerformed
        // TODO add your handling code here:

        if (DepositiPrelievi_Tabella.getSelectedRow()>=0){
        int rigaselezionata = DepositiPrelievi_Tabella.getSelectedRow();
        
        String IDTransazione = DepositiPrelievi_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
       // System.out.println(IDTransazione);
        ClassificazioneTrasf_Modifica mod=new ClassificazioneTrasf_Modifica(IDTransazione);
        mod.setLocationRelativeTo(this);
        mod.setVisible(true);
        //System.out.println(mod.getModificaEffettuata());
        if (mod.getModificaEffettuata()){
         this.TransazioniCrypto_Bottone_Salva.setEnabled(true);
         this.TransazioniCrypto_Bottone_Annulla.setEnabled(true);
         this.TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
         
         TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
         this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        }
       // System.out.println(mod.getModificaEffettuata());        
        DepositiPrelievi_Caricatabella();
        
        
        
      /*            String tipo;
                    String aggiornata2[]=MappaCryptoWallet.get(id2);
                    if(id2.split("_")[4].equalsIgnoreCase("DC")) tipo="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";else tipo="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    aggiornata2[18]=tipo;
                    aggiornata2[5]="TRASFERIMENTO TRA WALLET";
                    aggiornata2[19]="0";
                    aggiornata2[20]=id;
                    String aggiornata[]=MappaCryptoWallet.get(id);
                    if(id.split("_")[4].equalsIgnoreCase("DC")) tipo="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";else tipo="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    aggiornata[18]=tipo;
                    aggiornata[5]="TRASFERIMENTO TRA WALLET";
                    aggiornata[19]="0";
                    aggiornata[20]=id2;
                    MappaCryptoWallet.put(id, aggiornata);
                    MappaCryptoWallet.put(id2, aggiornata2);*/
        
        
        }

        
        // adesso devo caricarci i dettagli del movimento selezionato etc...
    }//GEN-LAST:event_DepositiPrelievi_Bottone_AssegnazioneManualeActionPerformed

    private void DepositiPrelievi_Caricatabella()
            {
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.DepositiPrelievi_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaDepositiPrelievi);
        Tabelle.ColoraRigheTabellaCrypto(DepositiPrelievi_Tabella);
        for (String[] v : MappaCryptoWallet.values()) {
          String TipoMovimento=v[0].split("_")[4].trim();
          if (TipoMovimento.equalsIgnoreCase("DC")||TipoMovimento.equalsIgnoreCase("PC"))
          {
            //if (this.DepositiPrelievi_CheckBox_movimentiClassificati.isSelected())
            if (v[18].trim().equalsIgnoreCase("")||this.DepositiPrelievi_CheckBox_movimentiClassificati.isSelected())
              {
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
            ModelloTabellaDepositiPrelievi.addRow(riga);
            }
          }
                  
       }
    }
    
        private void SituazioneImport_Caricatabella1()
            {
        Map<String, String[]> SituazioneImport_Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        DefaultTableModel ModelloTabella1SituazioneImport = (DefaultTableModel) this.SituazioneImport_Tabella1.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabella1SituazioneImport);
        Tabelle.ColoraRigheTabellaCrypto(SituazioneImport_Tabella1);
        for (String[] v : MappaCryptoWallet.values()) {
            
          String riga[]=new String[4];
        if (SituazioneImport_Mappa.get(v[3])==null)
        {
            riga[0]=v[3];
            riga[1]=v[1];
            riga[2]=v[1];
            riga[3]="1";
            SituazioneImport_Mappa.put(v[3], riga);
        }
        else
          {
             riga= SituazioneImport_Mappa.get(v[3]);
             riga[2]=v[1];
             riga[3]=String.valueOf(Integer.parseInt(riga[3])+1);
             SituazioneImport_Mappa.put(v[3], riga);
          }

          //  ModelloTabella1SituazioneImport.addRow(riga);                 
       }
        for (String[] v : SituazioneImport_Mappa.values()) {
            ModelloTabella1SituazioneImport.addRow(v); 
            
        }
        
    }
    
    private void DepositiPrelieviComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_DepositiPrelieviComponentShown
        // TODO add your handling code here:
      //  DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_DepositiPrelieviComponentShown

    private void DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed
        // TODO add your handling code here:
        //qua devo fare le verifiche sui numeri e assegnare le unioni correttamente
        int numeromodifiche=0;
        DefaultTableModel ModelloTabella1DepositiPrelievi = (DefaultTableModel) this.DepositiPrelievi_Tabella.getModel();
        //SituazioneImport_Tabella1.
        int numeroRighe=ModelloTabella1DepositiPrelievi.getRowCount();
        for (int i=0;i<numeroRighe;i++){
            //System.out.println(ModelloTabella1DepositiPrelievi.getValueAt(i, 1));
            String id=ModelloTabella1DepositiPrelievi.getValueAt(i, 0).toString();
            String data=ModelloTabella1DepositiPrelievi.getValueAt(i, 1).toString();
            String moneta=ModelloTabella1DepositiPrelievi.getValueAt(i, 4).toString();
            String qta=ModelloTabella1DepositiPrelievi.getValueAt(i, 5).toString();
            String wallet=ModelloTabella1DepositiPrelievi.getValueAt(i, 2).toString();
            //come prima cosa verifico che il movimento non sia già abbinato/assegnato
        if (MappaCryptoWallet.get(id)!=null && MappaCryptoWallet.get(id)[18].equalsIgnoreCase(""))
            for (int k=i+1;k<numeroRighe;k++){ 
                String id2=ModelloTabella1DepositiPrelievi.getValueAt(k, 0).toString();
                String data2=ModelloTabella1DepositiPrelievi.getValueAt(k, 1).toString();
                String moneta2=ModelloTabella1DepositiPrelievi.getValueAt(k, 4).toString();
                String qta2=ModelloTabella1DepositiPrelievi.getValueAt(k, 5).toString();
                String wallet2=ModelloTabella1DepositiPrelievi.getValueAt(k, 2).toString();
                //le condizioni affinchè avvenga l'abbinamento automatico devono essere               
                //1- il movimento non deve risultarte già abbinato
                //2- differenza tra le date minore di 1 ora
                //3- stessa moneta
                //4- exchange diverso
                //5- importo uguale o comunque non deve differire di più del 2% ma uno deve essere un deposito e l'altro un prelievo
                //6- un movimento deve essere in negativo e l'altro in positivo
                BigDecimal Sommaqta=new BigDecimal(qta).add(new BigDecimal (qta2)).stripTrailingZeros().abs();
                BigDecimal PercentualeDifferenza=new BigDecimal(100);                
                if (Double.parseDouble(qta)!=0){
                    PercentualeDifferenza=Sommaqta.divide(new BigDecimal(qta),4,RoundingMode.HALF_UP).multiply(new BigDecimal(100)).abs(); 
                    }
                if (MappaCryptoWallet.get(id2)[18].equalsIgnoreCase("")&&//1
                        Funzioni_Date_DifferenzaDateSecondi(data2,data)<3600 &&//2
                        moneta.equalsIgnoreCase(moneta2)&&//3
                        !wallet.equalsIgnoreCase(wallet2)&&//4
                        PercentualeDifferenza.compareTo(new BigDecimal(2))==-1 //5
                        )     //6  
                
                {
                    String tipo;
                    String aggiornata2[]=MappaCryptoWallet.get(id2);
                    if(id2.split("_")[4].equalsIgnoreCase("DC")) tipo="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";else tipo="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    aggiornata2[18]=tipo;
                    if(id2.split("_")[4].equalsIgnoreCase("DC")) aggiornata2[5]="TRASFERIMENTO TRA WALLET";else aggiornata2[5]="TRASFERIMENTO TRA WALLET";
                    aggiornata2[19]="0";
                    aggiornata2[20]=id;
                    String aggiornata[]=MappaCryptoWallet.get(id);
                    if(id.split("_")[4].equalsIgnoreCase("DC")) tipo="DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";else tipo="PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
                    aggiornata[18]=tipo;
                    if(id.split("_")[4].equalsIgnoreCase("DC")) aggiornata[5]="TRASFERIMENTO TRA WALLET";else aggiornata[5]="TRASFERIMENTO TRA WALLET";
                    aggiornata[19]="0";
                    aggiornata[20]=id2;
                    MappaCryptoWallet.put(id, aggiornata);
                    MappaCryptoWallet.put(id2, aggiornata2);
                    numeromodifiche++;
                   // System.out.println(data+" ; "+data2+" ; "+Differenza_Date_secondi(data2,data));
                    break;
                }
            }
        }

       // this.CDC.setSelectedIndex(0);
        if (numeromodifiche>0){
        JOptionPane.showConfirmDialog(this, "Sono stati individuati e aggiornati "+numeromodifiche+" coppie di transazioni, ricordarsi di salvare le modifiche!!",
        "Resoconto",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
        this.TransazioniCrypto_Bottone_Salva.setEnabled(true);
        this.TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
        this.TransazioniCrypto_Bottone_Annulla.setEnabled(true);
        }
        else{
        JOptionPane.showConfirmDialog(this, "Non sono state trovare nuove coppie di transazioni da abbinare automaticamente",
        "Resoconto",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);   
        }
        
        TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed

    private void SituazioneImportComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_SituazioneImportComponentShown
        // TODO add your handling code here:
        SituazioneImport_Caricatabella1();
    }//GEN-LAST:event_SituazioneImportComponentShown

    private void DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased
        // TODO add your handling code here:
       // System.out.println("cambio");

       DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased

    private void TransazioniCrypto_Text_PlusvalenzaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Text_PlusvalenzaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TransazioniCrypto_Text_PlusvalenzaActionPerformed

    
         private static boolean eseguiOperazioneConProgressbar() {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(250, 30));
        progressBar.setStringPainted(true);

        JFrame frame = new JFrame("ProgressBar Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());
        frame.add(progressBar);
        frame.setVisible(true);

        SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(50);
                    setProgress(i);
                }
                return true;
            }

            @Override
            protected void done() {
                progressBar.setString("Completato!");
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (int) evt.getNewValue();
                progressBar.setValue(progress);
            }
        });

        worker.execute();

        try {
            worker.get();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    
    private void TransazioniCrypto_Bottone_InserisciWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_InserisciWalletActionPerformed
        // TODO add your handling code here:
        GestioneWallets a =new GestioneWallets();
        a.setLocationRelativeTo(this);
        a.setTitle("Gestione dei Wallet Defi");
        a.setVisible(true);
     //   System.out.println("cavolo");

    }//GEN-LAST:event_TransazioniCrypto_Bottone_InserisciWalletActionPerformed

    private void TransazioniCrypto_Bottone_DettaglioDefiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_DettaglioDefiActionPerformed
        // TODO add your handling code here:
        
        if (TransazioniCryptoTabella.getSelectedRow() >= 0) {
            int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());

            String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 24).toString();
            if (IDTransazione != null) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://bscscan.com/tx/" + IDTransazione));
                    } catch (URISyntaxException | IOException ex) {
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }


    }//GEN-LAST:event_TransazioniCrypto_Bottone_DettaglioDefiActionPerformed

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        // TODO add your handling code here:
      //  System.out.println("Focus");
        if (TabellaCryptodaAggiornare) {
          //  System.out.println("AggiornoTabella");
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            TabellaCryptodaAggiornare = false;
            TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
            TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_Bottone_Annulla.setEnabled(true);
            TransazioniCrypto_Bottone_Salva.setEnabled(true);
            TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
            this.setCursor(Cursor.getDefaultCursor());
        }
    }//GEN-LAST:event_formWindowGainedFocus

    private void Analisi_CryptoComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_Analisi_CryptoComponentShown
        // TODO add your handling code here:
        DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_Analisi_CryptoComponentShown

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        //Creo una lista con i Wallet disponibili
          Map<String, List<String>> Wallets_e_Dettagli = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
          for (String[] v : MappaCryptoWallet.values()) {
              String Wallet=v[3];
              String WalletDettaglio=v[4];
              if(Wallets_e_Dettagli.get(v[3])==null)
                  {
                      List<String> Lista=new ArrayList();
                      Lista.add(WalletDettaglio);
                      Wallets_e_Dettagli.put(Wallet, Lista);
                  }
              else{
                  List<String> Lista;
                  Lista=Wallets_e_Dettagli.get(Wallet);
                  if (!Lista.contains(WalletDettaglio))Lista.add(WalletDettaglio);
              }
          }
          //Creo una lista con i dettagli dei wallet disponibili
      /*    Mappa_Wallet.clear();
          for (String[] v : MappaCryptoWallet.values()) {
                Mappa_Wallet.put(v[3], v[1]);
          }*/
        
        MovimentoManuale_GUI a= new MovimentoManuale_GUI();
        a.CompilaComboBoxWallet(Wallets_e_Dettagli);//questo compila il combobox principale e popola la mappa wallet e dettagli della classe
        a.setVisible(true);
        //Mappa_Wallet
    }//GEN-LAST:event_jButton1ActionPerformed

 
    public void TransazioniCrypto_Funzioni_AggiornaDefi(List<String> Portafogli,String apiKey) {
        Component c=this;
        Download progress=new Download();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progress.setLocationRelativeTo(this);
        
        Thread thread;
            thread = new Thread() {
            public void run() {
        Map<String, TransazioneDefi> MappaTransazioniDefi = Importazioni.RitornaTransazioniBSC(Portafogli,c,progress);
        if (MappaTransazioniDefi != null) {

            int i=0;
            for (TransazioneDefi v : MappaTransazioniDefi.values()) {
                for (String[] st : v.RitornaRigheTabella()) {
                    MappaCryptoWallet.put(st[0], st);
                    i++;
                }
            }
            Calcoli.ScriviFileConversioneXXXEUR();
           
            TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
            Importazioni.TransazioniAggiunte=i;
            progress.dispose();
                       
        }       
        }
            };
        thread.start();  
        progress.setVisible(true);
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        JOptionPane.showConfirmDialog(this, "Importazione Terminata \nSono stati inseriti "+Importazioni.TransazioniAggiunte+" nuovi movimenti",
                "Importazione Terminata",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
        this.setCursor(Cursor.getDefaultCursor());
    }
            
    
    
    
    public void TransazioniCrypto_Funzioni_PulisciMovimentiAssociatinonEsistenti(){
        //questa funziona va lanciata ad ogni fine importazione per verificare non vi siano modifiche
        //su movimenti già associati
        for (String[] v : MappaCryptoWallet.values()) {
          if (v[18]!=null&&(v[18].contains("DTW")||v[18].contains("PTW")))
          {
              String ID=v[0];
              String riferimento=v[20];
              String movimentoOpposto[]=MappaCryptoWallet.get(riferimento);
              if (movimentoOpposto==null || !movimentoOpposto[20].equalsIgnoreCase(ID)){
                //se il movimento opposto non esiste oppure se sul movimento opposto non trovo l'id di questo movimento allora pulisco le righe
                if (v[18].contains("DTW"))v[5]="DEPOSITO CRYPTO"; else v[5]="PRELIEVO CRYPTO";
                v[18]="";
                
                  
              }
            
            }
          }
    }
    
public void TransazioniCrypto_Funzioni_AggiornaPlusvalenze(){
            Deque<String[]> stack = new ArrayDeque<String[]>();
       // stack.push("a");
       // stack.push("b");
       // System.out.println(stack.size());
       // System.out.println(stack.pop());
       // System.out.println(stack.pop());
        Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            //Se deposito crypto non associato o prelievo crypto non associato non lo considero e lo salto - DC-PC()
            //Se è un trasferimento interno lo salto - TI
            //se è un trasferimento tra wallet lo salto - PC-DC(PTW-DTW)
            //se deposito airdrop lo considero - DC(DAI)
            //se deposito a zero lo considero - DC(DCZ)
            //se cashout lo levo e calcolo la plusvalenza - PC(PCO)
            //se è un prelievo sconosciuto lo tolgo dallo stack ma non calcolo plusvalenza. - PC(PWN)
            //se è uno scambio crypto tolgo la moneta venduta, aggiungo moneta acquistata e riporto valore LIFO su moneta acquistata - SC
            //se è deposito fiat lo salto - DF
            //se è acquisto crypto aggiungo a stack - AC
            //se è vendita crypto tolgo da stack e calcolo plus in base al lifo - VC
            //da completare con altre casistiche........................................................................
            String IDTransazione=v[0];
            String IDTS[]=IDTransazione.split("_");
            //per questa prima fase intanto ignoro tutti i prelievi, una volta finito di scrivere la funzione
            //riprenderò in mano questa parte per aggiungere tutte le casistiche
            if (IDTS[4].equalsIgnoreCase("DC")||IDTS[4].equalsIgnoreCase("DN")){//questo vale sia per deposito crypto che per nft
                //QUI VANNO GESTITI I DEPOSITI CRYPTO IN BASE ALLA TIPOLOGIA
                //Le tipologie sono:
                //DTW -> Trasferimento tra Wallet (deposito) (plusvalenza 0 e solo calcolo costo carico, nessun movimento sullo stack)
                //DAI -> Airdrop o similare (deposito) (plusvalenza=valore transazione, prezzo di carico = valore transazione)
                //DCZ -> Costo di carico 0 (deposito)  (no plusvalenza, costo di carico=0 aggiunto allo stack)
                //e senza classificazione che va trattato come fosse un DTW
                String Moneta=v[11];
                String Qta=v[13];
                String Valore=v[15];
                if (v[18].contains("DTW")||v[18].equalsIgnoreCase("")){
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                    v[19]="0.00";
                }else if(v[18].contains("DAI")){
                    v[17]=Valore;
                    v[19]=Valore;
                    TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                }else if(v[18].contains("DCZ")){
                    v[17]="0.00";
                    v[19]="0.00";
                    TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta,Qta,"0.00");                    
                }
                //System.out.println(v[18]);
            }else if (IDTS[4].equalsIgnoreCase("PC")||IDTS[4].equalsIgnoreCase("PN")){ //questo vale sia per crypto che nft
             //QUI VANNO GESTITI I PRELIEVI CRYPTO IN BASE ALLA TIPOLOGIA  
                //Le tipologie sono:
                //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo) (lo elimino dai calcoli e non genero plusvalenze)
                //PCO -> Cashout o similare (prelievo) (plusvalenza calcolata + tolto valore dallo stack)
                //PTW -> Trasferimento tra Wallet (prelievo) (plusvalenza 0 e solo calcolo costo carico, nessun movimento sullo stack)
                //e senza classificazione che va trattato come fosse un PTW
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                String Valore=v[15];
                if (v[18].contains("PTW")||v[18].equalsIgnoreCase("")){
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                    v[19]="0.00";
                }else if(v[18].contains("PCO")){
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                    String Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                }else if(v[18].contains("PWN")){
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                    v[17]=PrzCarico;
                    v[19]="0.00";                    
                }
               // System.out.println(v[18]);

            }else if (IDTS[4].equalsIgnoreCase("DF")){ //Deposito Fiat
             //IN QUESTO CASO NON DEVO FARE NULLA
            }else if (IDTS[4].equalsIgnoreCase("PF")){ //Prelievo Fiat
             //IN QUESTO CASO NON DEVO FARE NULLA   
            }else if (IDTS[4].equalsIgnoreCase("AC")||IDTS[4].equalsIgnoreCase("AN")){ //Acquisto Crypto o NFT
                //se compro tramite FIAT non genero plusvalenza
                if (v[9].trim().equalsIgnoreCase("FIAT")){
                    String Moneta=v[11];
                    String Qta=v[13];
                    String Valore=v[15];
                    TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                    v[17]=v[15];
                    v[19]="0.00";
                }else{
                   //in questo caso vuol dire che l'acquisto in realtà è uno scambio eterogeneo che genera plusvalenza
                   //e devo gestirlo
                    String Moneta=v[8];
                    String Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico;
                    String Plusvalenza;
                    String Valore=v[15];
                    PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true); 
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                    String Moneta2=v[11];
                    String Qta2=v[13];
                    TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta2,Qta2,PrzCarico);
                }
                
            }else if (IDTS[4].equalsIgnoreCase("VC")||IDTS[4].equalsIgnoreCase("VN")){ //Vendita Crypto o NFT
                String Moneta=v[8];
                String Qta=v[10];
                String Valore=v[15];
                String PrzCarico;
                String Plusvalenza;
                PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]=Plusvalenza;
                
            }else if (IDTS[4].equalsIgnoreCase("SC")||IDTS[4].equalsIgnoreCase("SN")){//Scambio Crypto o NFT
                //nel caso degli scambi, prima recupero il valore dallo stack per la moneta venduta
                //poi quel valore lo metto nello stack per la moneta acquistata
                //e quindi lo stesso vaolre lo scrivo sulla riga dello scambio
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                //System.out.println(Moneta+" - "+Qta);
                //String Valore=v[15];
                String PrzCarico;
                //String Plusvalenza;
                PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                //Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]="0.00";  
                String Moneta2=v[11];
                String Qta2=v[13];
                //System.out.println(Moneta2+" - "+Qta2+" - "+PrzCarico);
                TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta2,Qta2,PrzCarico);
                
            }else if (IDTS[4].equalsIgnoreCase("RW")){ //Reward varie
                //IN QUESTO CASO DEVO SOLO INSERIRE IL DATO NELLO STACK
                String Moneta=v[11];
                String Qta=v[13];
               // if (Qta.equalsIgnoreCase("")) System.out.println(v[13]+" - "+v[0]);
                String Valore=v[15];
                //se esiste la qta in quella possizione vuol dire che è una rewad, altrimenti è un rimborso di una reward
                if (!Qta.equalsIgnoreCase(""))
                    {
                    TransazioniCrypto_Stack_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                    }
                else{
                    Moneta=v[8];
                    Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                    String Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                }
                
            }else if (IDTS[4].equalsIgnoreCase("TI")){ //Trasferimento interno
                //IN QUESTO CASO dovrei solo calcolare il prezzo di carico ma senza togliere nulla dallo stack
                String Moneta=v[11];
                String Qta=v[13];
                if (!Qta.equalsIgnoreCase("")){
                String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,false);
                v[17]=PrzCarico;
                }
                else {
                    Moneta=v[8];
                    Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                }
            }else if (IDTS[4].equalsIgnoreCase("CM")){ //Commissioni
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                String Valore=v[15];
                String PrzCarico;
                String Plusvalenza;
                PrzCarico=TransazioniCrypto_Stack_TogliQta(CryptoStack,Moneta,Qta,true);
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]=Plusvalenza;                
            }else { //Qualcosa di non contemplato
                System.out.println(IDTransazione);
            }

        }
}
    
public String TransazioniCrypto_Stack_TogliQta(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,boolean toglidaStack) {
    
    //come ritorno ci invio il valore della movimentazione
    String ritorno="";
    ArrayDeque<String[]> stack;
    BigDecimal qtaRimanente=new BigDecimal(Qta).abs();
    BigDecimal costoTransazione=new BigDecimal("0");
    //prima cosa individuo la moneta e prendo lo stack corrispondente
    if (CryptoStack.get(Moneta)==null){
        //ritorno="0";
    }else{
        if (toglidaStack)stack=CryptoStack.get(Moneta);
        else{
            ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
            stack=stack2.clone();
        }
        /*ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
        stack=stack2.clone();*/
        //System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
        while (qtaRimanente.compareTo(new BigDecimal ("0"))>0 && stack.size()>0){ //in sostanza fino a che la qta rimanente è maggiore di zero oppure ho finito lo stack
           // System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
            String ultimoRecupero[];
            ultimoRecupero=stack.pop();
            BigDecimal qtaEstratta=new BigDecimal(ultimoRecupero[1]);
            BigDecimal costoEstratta=new BigDecimal(ultimoRecupero[2]);
        /*   if (Moneta.equalsIgnoreCase("usdt")){ 
                System.out.println(ultimoRecupero[1]+" - "+ultimoRecupero[2]+" - "+stack.size());
                System.out.println(qtaRimanente);
                }*/
            //System.out.println(qtaEstratta+" - "+costoEstratta);
            if (qtaEstratta.compareTo(qtaRimanente)<=0)//se qta estratta e minore o uguale alla qta rimanente allore
                {
                //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
                qtaRimanente=qtaRimanente.subtract(qtaEstratta);
                //System.out.println(qtaRimanente);
                //recupero il valore di quella transazione e la aggiungo al costoTransazione
                costoTransazione=costoTransazione.add(costoEstratta);
            }else{
                //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
                //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
                //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
                //non ho più qta rimanente
                String qtaRimanenteStack=qtaEstratta.subtract(qtaRimanente).toPlainString();
               // System.out.println(qtaEstratta+" - "+qtaRimanente+"- "+qtaRimanenteStack);
                String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).toPlainString();
                String valori[]=new String[]{Moneta,qtaRimanenteStack,valoreRimanenteSatck};
                stack.push(valori);
                costoTransazione=costoTransazione.add(costoEstratta.subtract(new BigDecimal(valoreRimanenteSatck)));
                qtaRimanente=new BigDecimal("0");//non ho più qta rimanente
            }
            
        }
        //pop -> toglie dello stack l'ultimo e recupera il dato
        //peek - > recupera solo il dato

    }
    ritorno=costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();
    
    
    return ritorno;
   // System.out.println(Moneta +" - "+stack.size());
}   
    
    public void TransazioniCrypto_Stack_InserisciValore(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String Valore) {
    
    ArrayDeque<String[]> stack;
    String valori[]=new String[3];
    valori[0]=Moneta;
    valori[1]=Qta;
    valori[2]=Valore;
    if (CryptoStack.get(Moneta)==null){
        stack = new ArrayDeque<String[]>();
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }else{
        stack=CryptoStack.get(Moneta);
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }
   // System.out.println(Moneta +" - "+stack.size());
}
    
        public void TransazioniCrypto_CompilaTextPaneDatiMovimento() {
        if (TransazioniCryptoTabella.getSelectedRow()>=0){
        int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());
        String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
        
        String DataOra = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 1).toString();
        
        String ExchangeWallet=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 3).toString()+
                " ("+TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 4).toString()+")";
        
        String CausaleMovimento=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 5).toString()+
                " ("+TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 6).toString()+")";
        
        String CausaleOriginale="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 7)!=null)
            CausaleOriginale=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 7).toString();
        
        String ValoreTransazione=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 15).toString();
        String ValoreTransazionePrezzoCarico=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 17).toString();
        String QTARic="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 13)!=null)
            QTARic=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 13).toString();
        String QTAUsc="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 10)!=null)
            QTAUsc=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 10).toString();
        String unitarioValoreRic="";
        String unitarioPrzCaricoRic="";
        String unitarioValoreUsc="";
        String unitarioPrzCaricoUsc="";
        String MonRic="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 11)!=null)
            MonRic=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 11).toString().trim();
        String MonUsc="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 8)!=null)
            MonUsc=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 8).toString().trim();
        if (Funzioni_isNumeric(ValoreTransazione)&&Funzioni_isNumeric(QTARic)&&!QTARic.equalsIgnoreCase("")&&!MonRic.equalsIgnoreCase("EUR")){
            if (new BigDecimal(QTARic).compareTo(new BigDecimal(0))!=0)
            unitarioValoreRic="</b>&#9("+new BigDecimal(ValoreTransazione).divide(new BigDecimal(QTARic).abs(),12, RoundingMode.HALF_UP).stripTrailingZeros().toString()+"€ V.M. cad)";
        }
        if (Funzioni_isNumeric(ValoreTransazionePrezzoCarico)&&Funzioni_isNumeric(QTARic)&&!QTARic.equalsIgnoreCase("")&&!MonRic.equalsIgnoreCase("EUR")){
            if (new BigDecimal(QTARic).compareTo(new BigDecimal(0))!=0)
            unitarioPrzCaricoRic="</b>  ("+new BigDecimal(ValoreTransazionePrezzoCarico).divide(new BigDecimal(QTARic).abs(),12, RoundingMode.HALF_UP).stripTrailingZeros().toString()+"€ PdC cad)";
        }
        if (Funzioni_isNumeric(ValoreTransazione)&&Funzioni_isNumeric(QTAUsc)&&!QTAUsc.equalsIgnoreCase("")&&!MonUsc.equalsIgnoreCase("EUR")){
            if (new BigDecimal(QTAUsc).compareTo(new BigDecimal(0))!=0)
            unitarioValoreUsc="</b>&#9("+new BigDecimal(ValoreTransazione).divide(new BigDecimal(QTAUsc).abs(),12, RoundingMode.HALF_UP).stripTrailingZeros().toString()+"€ V.M. cad)";
        }
        if (Funzioni_isNumeric(ValoreTransazionePrezzoCarico)&&Funzioni_isNumeric(QTAUsc)&&!QTAUsc.equalsIgnoreCase("")&&!MonUsc.equalsIgnoreCase("EUR")){
            if (new BigDecimal(QTAUsc).compareTo(new BigDecimal(0))!=0)
            unitarioPrzCaricoUsc="</b>  ("+new BigDecimal(ValoreTransazionePrezzoCarico).divide(new BigDecimal(QTAUsc).abs(),12, RoundingMode.HALF_UP).stripTrailingZeros().toString()+"€ PdC cad)";
        }
        String MonetaRicevuta="";
        String MonetaUscita="";
        if (Funzioni_isNumeric(ValoreTransazione)&&Funzioni_isNumeric(QTARic)){
        MonetaRicevuta=QTARic+
                " "+MonRic+
                "  "+unitarioValoreRic+" "+
                "  "+unitarioPrzCaricoRic;
        }
        if (Funzioni_isNumeric(ValoreTransazione)&&Funzioni_isNumeric(QTAUsc)){
        MonetaUscita=QTAUsc+
                " "+MonUsc+
                "  "+unitarioValoreUsc+" "+
                "  "+unitarioPrzCaricoUsc;
        

        }
        String ValoreTransazioneCSV=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 14).toString();
        String Plusvalenza=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 19).toString();
        String Note="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 21)!=null)
            Note=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 21).toString().replace("|&£|","<br>&#9");
        String Riferimenti="";
        if (TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 20)!=null)
            Riferimenti=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 20).toString();
      //  String MacAddress = TransazioniCryptoTabella.getModel().getValueAt(TransazioniCryptoTabella.getSelectedRow(), 1).toString();
        
        String daAppendere="ID TRANSAZIONE :&#9&#9&#9<b>"+IDTransazione+"</b><br>"+
                "DATA e ORA :&#9&#9&#9<b>"+DataOra+"</b><br>"+
                "EXCHANGE/WALLET :&#9&#9&#9<b>"+ExchangeWallet+"</b><br>"+
                "CAUSALE MOVIMENTO :&#9&#9&#9<b>"+CausaleMovimento+"</b><br>"+
                "CAUSALE DA CSV :&#9&#9&#9<b>"+CausaleOriginale+"</b><br>"+
                "MONETA USCITA :&#9&#9&#9<b>"+MonetaUscita+"</b><br>"+
                "MONETA RICEVUTA :&#9&#9&#9<b>"+MonetaRicevuta+"</b><br>"+
                "VALORE TRANSAZIONE da CSV :&#9&#9<b>"+ValoreTransazioneCSV+"</b><br>"+
                "VALORE TRANSAZIONE :&#9&#9&#9<b>"+ValoreTransazione+"</b><br>"+
                "VALORE TRANSAZIONE AL PREZZO DI CARICO :&#9<b>"+ValoreTransazionePrezzoCarico+"</b><br>"+
                "PLUSVALENZA :&#9&#9&#9<b>"+Plusvalenza+"</b><br>"+
                "RIFERIMENTI TRASFERIMENTO :&#9&#9<b>"+Riferimenti+"</b><br>"+
                "NOTE :&#9<b>"+Note+"</b><br>";
        
        this.TransazioniCryptoTextPane.setText(daAppendere);
}
    }
    
    
    
    
    
    
       private void TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(boolean EscludiTI) throws IOException { 
        this.Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, "", 999);
        this.TransazioniCryptoTextPane.setText("");
        String fileDaImportare = CryptoWallet_FileDB;
        MappaCryptoWallet.clear();
        Mappa_Wallet.clear();
        BigDecimal Plusvalenza=new BigDecimal("0");
        
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) this.TransazioniCryptoTabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaCrypto);
        Tabelle.ColoraRigheTabellaCrypto(TransazioniCryptoTabella);
        File TransazioniCrypto1=new File (fileDaImportare);
        if (!TransazioniCrypto1.exists()) TransazioniCrypto1.createNewFile();
        String riga;
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
            while ((riga = bure.readLine()) != null) {
                String splittata[] = riga.split(";");
                Mappa_Wallet.put(splittata[3], splittata[1]);
                MappaCryptoWallet.put(splittata[0], splittata);
              //  this.TransazioniCryptoTabella.add(splittata);
              if (EscludiTI==true&&!splittata[5].trim().equalsIgnoreCase("Trasferimento Interno")||EscludiTI==false){
                  if (Funzioni_Date_ConvertiDatainLong(splittata[1]) >= Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale) && Funzioni_Date_ConvertiDatainLong(splittata[1]) <= Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {
                     ModelloTabellaCrypto.addRow(splittata);
                                     if (Funzioni_isNumeric(splittata[19]))
                {
                    Plusvalenza=Plusvalenza.add(new BigDecimal(splittata[19]));
                }
              }
                  }
             //   MappaCryptoWallet.put(splittata[0], splittata);
                
            }
            
    }   catch (IOException ex) {   
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.TransazioniCrypto_Text_Plusvalenza.setText("€ "+Plusvalenza.toPlainString());
        Color verde=new Color (45, 155, 103);
        Color rosso=new Color(166,16,34);
        if (!TransazioniCrypto_Text_Plusvalenza.getText().contains("-"))TransazioniCrypto_Text_Plusvalenza.setForeground(verde);else TransazioniCrypto_Text_Plusvalenza.setForeground(rosso);
        this.Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }    
    
       
       
        private void TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(boolean EscludiTI) { 
        Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, "", 999);
        TransazioniCryptoTextPane.setText("");
        Mappa_Wallet.clear();
        
       //da verificare se va bene, serve per evitare problemi di sorting nel caso in cui la richiesta arrivi da un thread
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(TransazioniCryptoTabella.getModel());
        TransazioniCryptoTabella.setRowSorter(sorter);
        
        
        DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) TransazioniCryptoTabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaCrypto);
        BigDecimal Plusvalenza=new BigDecimal("0");
        Tabelle.ColoraRigheTabellaCrypto(TransazioniCryptoTabella);
         for (String[] v : MappaCryptoWallet.values()) {
          Mappa_Wallet.put(v[3], v[1]);
          if (EscludiTI==true&&!v[5].trim().equalsIgnoreCase("Trasferimento Interno")||EscludiTI==false){
                if (Funzioni_Date_ConvertiDatainLong(v[1]) >= Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale) && Funzioni_Date_ConvertiDatainLong(v[1]) <= Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {
                ModelloTabellaCrypto.addRow(v);
                if (Funzioni_isNumeric(v[19]))
                {
                    Plusvalenza=Plusvalenza.add(new BigDecimal(v[19]));
                }
                }
            }
       }
         if (Importazioni.TransazioniAggiunte!=0){
         TransazioniCrypto_Bottone_Salva.setEnabled(true);
         TransazioniCrypto_Bottone_Annulla.setEnabled(true);
         TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);
        }
         TransazioniCrypto_Text_Plusvalenza.setText("€ "+Plusvalenza.toPlainString());
         Color verde=new Color (45, 155, 103);
        Color rosso=new Color(166,16,34);
        if (!TransazioniCrypto_Text_Plusvalenza.getText().contains("-"))TransazioniCrypto_Text_Plusvalenza.setForeground(verde);else TransazioniCrypto_Text_Plusvalenza.setForeground(rosso);
         Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }       
    
    public static List<String[]> Funzioni_Tabelle_ListaTabella(JTable tabella) {
            int numeroRighe=tabella.getModel().getRowCount();
            int numeroColonne=tabella.getModel().getColumnCount();
            String dati[];
            List<String[]> tabella1=new ArrayList<>();
            for (int i=0;i<numeroRighe;i++){
                dati=new String[numeroColonne];
                for (int h=0;h<numeroColonne;h++){
                    if (tabella.getModel().getValueAt(i, h)!=null)
                    dati[h]=tabella.getModel().getValueAt(i, h).toString();
                    else dati[h]="movimento non conteggiato";
                }
                tabella1.add(dati);
            }
            return tabella1;
    }
     
    
    
    public static boolean Funzioni_isNumeric(String str) {
        //ritorna vero se il campo è vuoto oppure è un numero
     if (str.isBlank()) return true;
        try  
  {  
    double d = Double.parseDouble(str);  
  }  
  catch(NumberFormatException nfe)  
  {  
    return false;  
  }  
  return !str.matches("^.*[a-zA-Z].*$");  

}
    
    public String[] Funzioni_Calcolo_SaldieMedie(List<String>[] listaSaldi, String DataInizialeS, String DataFinaleS, String SaldoInizioPeriodo, boolean MediaconPicchi) {

        //  System.out.println(DataInizialeS+" "+DataFinaleS);
        //  System.out.println (listaSaldi.length);
//viene tornato come primo valore il saldo iniziale
//come secondo valore il saldo finale
//come terso la giacenza media
// String SaldoPartenza=SaldoIniziale.toString();
        String ritorno[] = new String[3];
        String SaldoInizialeT = "0";
        boolean TrovatoSaldoIniziale = false;
        String SaldoFinaleT = "0";
        String DataIniziale = DataInizialeS;
        String DataFinale = DataFinaleS;
        BigDecimal SaldoIniziale = new BigDecimal("0");
        BigDecimal UltimoValore = new BigDecimal("0");
        // System.out.println(DataIniziale);
        //System.out.println(DataFinale);
        long diffdate;
        long longDatainiziale = Funzioni_Date_ConvertiDatainLong(DataIniziale);
        long longDataFinale = Funzioni_Date_ConvertiDatainLong(DataFinale);
        int contatore = 0;//il numero di giorni che serviranno per il calcolo della giacenza media
        for (String valori : listaSaldi[0]) {
            String splittata[] = valori.split(",");
            // long longDatainiziale=ConvertiDatainLong(DataIniziale);

            /*   if (longDatainiziale>ConvertiDatainLong(splittata[0]))
                    {
                        System.out.println("Errore, bisogna mettere una data inferiore o uguale a "+splittata[0]);
                        break;
                    }*/
            if (longDatainiziale > Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                UltimoValore = new BigDecimal(splittata[1]);
                SaldoInizialeT=splittata[1];
                //  System.out.println("SaldoIniziale="+UltimoValore+" , "+splittata[0]);
            }
            if (longDatainiziale <= Funzioni_Date_ConvertiDatainLong(splittata[0]) && longDataFinale >= Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                diffdate = Funzioni_Date_DifferenzaDate(splittata[0], DataIniziale);
                contatore = contatore + Integer.parseInt(String.valueOf(diffdate));
                SaldoIniziale = UltimoValore.multiply(new BigDecimal(diffdate)).add(SaldoIniziale);
                if (!TrovatoSaldoIniziale) {
                    SaldoInizialeT = UltimoValore.toString();
                }
                TrovatoSaldoIniziale = true;
                DataIniziale = splittata[0];
                UltimoValore = new BigDecimal(splittata[1]);

            }
        }

        SaldoFinaleT = UltimoValore.toString();

        if (MediaconPicchi) {
            DataIniziale = DataInizialeS;
            SaldoIniziale = new BigDecimal("0");
            UltimoValore = new BigDecimal("0");
            contatore = 0;//il numero di giorni che serviranno per il calcolo della giacenza media
            for (String valori : listaSaldi[1]) {
                String splittata[] = valori.split(",");
                if (longDatainiziale > Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                    UltimoValore = new BigDecimal(splittata[1]);
                }
                if (longDatainiziale <= Funzioni_Date_ConvertiDatainLong(splittata[0]) && longDataFinale >= Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                    diffdate = Funzioni_Date_DifferenzaDate(splittata[0], DataIniziale);
                    contatore = contatore + Integer.parseInt(String.valueOf(diffdate));
                    SaldoIniziale = UltimoValore.multiply(new BigDecimal(diffdate)).add(SaldoIniziale);
                    DataIniziale = splittata[0];
                    UltimoValore = new BigDecimal(splittata[1]);
                }

            }
        }

        diffdate = Funzioni_Date_DifferenzaDate(DataFinale, DataIniziale) + 1;
        contatore = contatore + Integer.parseInt(String.valueOf(diffdate));
        SaldoIniziale = UltimoValore.multiply(new BigDecimal(diffdate)).add(SaldoIniziale);
        BigDecimal GiacenzaMedia = SaldoIniziale.divide(new BigDecimal(contatore), 2, RoundingMode.HALF_UP).add(new BigDecimal(SaldoInizioPeriodo));
        //  System.out.println("Giacenza media "+GiacenzaMedia+";"+SaldoInizialeT+";"+SaldoFinaleT);
        this.CDC_Text_Giorni.setText("" + contatore);

        SaldoInizialeT = new BigDecimal(SaldoInizialeT).add(new BigDecimal(SaldoInizioPeriodo)).toString();
        SaldoFinaleT = new BigDecimal(SaldoFinaleT).add(new BigDecimal(SaldoInizioPeriodo)).toString();
        // System.out.println("Giacenza media "+GiacenzaMedia+";"+SaldoInizialeT+";"+SaldoFinaleT);
        ritorno[0] = SaldoInizialeT;
        ritorno[1] = SaldoFinaleT;
        ritorno[2] = GiacenzaMedia.toString();

        return ritorno;

    }
    
    public static long Funzioni_Date_DifferenzaDate(String Data1, String Data2) {
        long differenza=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date d = f.parse(Data1+" 00:00");
            long m1 = d.getTime();
            d = f.parse(Data2+" 00:00");
            long m2 = d.getTime();
            double diffe=(double)(m1-m2)/1000/3600/24;//Ritorno la differenza in giorni che poi mi servirà per il calcolo della giacenza media
            differenza=Math.round(diffe);//Ritorno la differenza in giorni che poi mi servirà per il calcolo della giacenza media
            //System.out.println (Data1+" - "+Data2+" - "+differenza+" - "+a);
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        return differenza;
    }
    
    public static long Funzioni_Date_DifferenzaDateSecondi(String Data1, String Data2) {
        long differenza=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date d = f.parse(Data1+" 00:00");
            long m1 = d.getTime();
            d = f.parse(Data2+" 00:00");
            long m2 = d.getTime();
            double diffe=(double)(m1-m2)/1000;//Ritorno la differenza in giorni che poi mi servirà per il calcolo della giacenza media
            differenza=Math.round(diffe);//Ritorno la differenza in giorni che poi mi servirà per il calcolo della giacenza media
            //System.out.println (Data1+" - "+Data2+" - "+differenza+" - "+a);
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        return differenza;
    }
    
       public static long Funzioni_Date_ConvertiDatainLong(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
        }
        return m1;
    } 
    
        public static String Funzioni_Date_ConvertiDatadaLong(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
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
            java.util.logging.Logger.getLogger(CDC_Grafica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CDC_Grafica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CDC_Grafica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CDC_Grafica.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CDC_Grafica().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane AnalisiCrypto;
    private javax.swing.JPanel Analisi_Crypto;
    private javax.swing.JTabbedPane CDC;
    private javax.swing.JButton CDC_CardWallet_Bottone_CaricaCSV;
    private javax.swing.JButton CDC_CardWallet_Bottone_StampaRapporto;
    private javax.swing.JCheckBox CDC_CardWallet_Checkbox_ConsideraValoreMaggiore;
    private javax.swing.JLabel CDC_CardWallet_Label_Entrate;
    private javax.swing.JLabel CDC_CardWallet_Label_FiltroTabelle;
    private javax.swing.JLabel CDC_CardWallet_Label_GiacenzaIniziale;
    private javax.swing.JLabel CDC_CardWallet_Label_GiacenzaMedia;
    private javax.swing.JLabel CDC_CardWallet_Label_PrimaData;
    private javax.swing.JLabel CDC_CardWallet_Label_SaldoFinale;
    private javax.swing.JLabel CDC_CardWallet_Label_SaldoIniziale;
    private javax.swing.JLabel CDC_CardWallet_Label_Spese;
    private javax.swing.JLabel CDC_CardWallet_Label_Tabella1;
    private javax.swing.JLabel CDC_CardWallet_Label_Tabella2;
    private javax.swing.JLabel CDC_CardWallet_Label_UltimaData;
    private javax.swing.JPanel CDC_CardWallet_Pannello;
    private javax.swing.JTable CDC_CardWallet_Tabella1;
    private javax.swing.JScrollPane CDC_CardWallet_Tabella1Scroll;
    private javax.swing.JTable CDC_CardWallet_Tabella2;
    private javax.swing.JScrollPane CDC_CardWallet_Tabella2Scroll;
    private javax.swing.JTextField CDC_CardWallet_Text_Entrate;
    private javax.swing.JTextField CDC_CardWallet_Text_FiltroTabelle;
    private javax.swing.JTextField CDC_CardWallet_Text_GiacenzaIniziale;
    private javax.swing.JTextField CDC_CardWallet_Text_GiacenzaMedia;
    private javax.swing.JTextField CDC_CardWallet_Text_PrimaData;
    private javax.swing.JTextField CDC_CardWallet_Text_SaldoFinale;
    private javax.swing.JTextField CDC_CardWallet_Text_SaldoIniziale;
    private javax.swing.JTextField CDC_CardWallet_Text_Spese;
    private javax.swing.JTextField CDC_CardWallet_Text_UltimaData;
    private com.toedter.calendar.JDateChooser CDC_DataChooser_Finale;
    private com.toedter.calendar.JDateChooser CDC_DataChooser_Iniziale;
    private javax.swing.JButton CDC_FiatWallet_Bottone_CaricaCSV;
    private javax.swing.JButton CDC_FiatWallet_Bottone_Errore;
    private javax.swing.JButton CDC_FiatWallet_Bottone_StampaRapporto;
    private javax.swing.JCheckBox CDC_FiatWallet_Checkbox_ConsideraValoreMaggiore;
    private javax.swing.JLabel CDC_FiatWallet_Label_Errore1;
    private javax.swing.JLabel CDC_FiatWallet_Label_Errore2;
    private javax.swing.JLabel CDC_FiatWallet_Label_FiltroTabella;
    private javax.swing.JLabel CDC_FiatWallet_Label_GiacenzaIniziale;
    private javax.swing.JLabel CDC_FiatWallet_Label_GiacenzaMedia;
    private javax.swing.JLabel CDC_FiatWallet_Label_PrimaData;
    private javax.swing.JLabel CDC_FiatWallet_Label_SaldoFinale;
    private javax.swing.JLabel CDC_FiatWallet_Label_SaldoIniziale;
    private javax.swing.JLabel CDC_FiatWallet_Label_Tabella2;
    private javax.swing.JLabel CDC_FiatWallet_Label_Tabella3;
    private javax.swing.JLabel CDC_FiatWallet_Label_UltimaData;
    private javax.swing.JPanel CDC_FiatWallet_Pannello;
    private javax.swing.JTable CDC_FiatWallet_Tabella1;
    private javax.swing.JScrollPane CDC_FiatWallet_Tabella1Scroll;
    private javax.swing.JTable CDC_FiatWallet_Tabella2;
    private javax.swing.JScrollPane CDC_FiatWallet_Tabella2Scroll;
    private javax.swing.JTable CDC_FiatWallet_Tabella3;
    private javax.swing.JScrollPane CDC_FiatWallet_Tabella3Scroll;
    private javax.swing.JTextField CDC_FiatWallet_Text_FiltroTabella;
    private javax.swing.JTextField CDC_FiatWallet_Text_GiacenzaIniziale;
    private javax.swing.JTextField CDC_FiatWallet_Text_GiacenzaMedia;
    private javax.swing.JTextField CDC_FiatWallet_Text_PrimaData;
    private javax.swing.JTextField CDC_FiatWallet_Text_SaldoFinale;
    private javax.swing.JTextField CDC_FiatWallet_Text_SaldoIniziale;
    private javax.swing.JTextField CDC_FiatWallet_Text_UltimaData;
    private javax.swing.JLabel CDC_Label_Giorni;
    private javax.swing.JPanel CDC_Opzioni;
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaCardWallet;
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaFiatWallet;
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet;
    private javax.swing.JTextField CDC_Text_Giorni;
    private javax.swing.JPanel DepositiPrelievi;
    private javax.swing.JButton DepositiPrelievi_Bottone_AssegnazioneAutomatica;
    private javax.swing.JButton DepositiPrelievi_Bottone_AssegnazioneManuale;
    private javax.swing.JCheckBox DepositiPrelievi_CheckBox_movimentiClassificati;
    private javax.swing.JTable DepositiPrelievi_Tabella;
    private javax.swing.JButton Opzioni_Bottone_CancellaTransazioniCrypto;
    private javax.swing.JButton Opzioni_Bottone_CancellaTransazioniCryptoXwallet;
    private javax.swing.JComboBox<String> Opzioni_Combobox_CancellaTransazioniCryptoXwallet;
    private javax.swing.JPanel SituazioneImport;
    private javax.swing.JTable SituazioneImport_Tabella1;
    private javax.swing.JPanel TransazioniCrypto;
    private javax.swing.JTextField TransazioniCryptoFiltro_Text;
    private javax.swing.JTable TransazioniCryptoTabella;
    private javax.swing.JTextPane TransazioniCryptoTextPane;
    private javax.swing.JButton TransazioniCrypto_Bottone_Annulla;
    private javax.swing.JButton TransazioniCrypto_Bottone_DettaglioDefi;
    private javax.swing.JButton TransazioniCrypto_Bottone_Importa;
    private javax.swing.JButton TransazioniCrypto_Bottone_InserisciWallet;
    private javax.swing.JButton TransazioniCrypto_Bottone_NuovoMovimento;
    private javax.swing.JButton TransazioniCrypto_Bottone_Salva;
    private javax.swing.JCheckBox TransazioniCrypto_CheckBox_EscludiTI;
    private javax.swing.JLabel TransazioniCrypto_Label_Filtro;
    private javax.swing.JLabel TransazioniCrypto_Label_MovimentiNonSalvati;
    private javax.swing.JLabel TransazioniCrypto_Label_Plusvalenza;
    private javax.swing.JScrollPane TransazioniCrypto_ScrollPane;
    private javax.swing.JTextField TransazioniCrypto_Text_Plusvalenza;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    // End of variables declaration//GEN-END:variables
}
