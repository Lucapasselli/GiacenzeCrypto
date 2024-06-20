/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package giacenze_crypto.com;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.lowagie.text.Font;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JDateChooserCellEditor;
import static giacenze_crypto.com.ClassificazioneTrasf_Modifica.RiportaTransazioniASituazioneIniziale;
import static giacenze_crypto.com.Importazioni.ColonneTabella;
import static giacenze_crypto.com.Importazioni.RiempiVuotiArray;
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
import java.util.ArrayList;
import java.util.Date;
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
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
//import org.apache.commons.codec.binary.Hex;


/**
 *
 * @author luca.passelli
 */
public class CDC_Grafica extends javax.swing.JFrame {

    /**
     * Creates new form com
     */
   
    static Map<String, String> CDC_FiatWallet_Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, String> CDC_FiatWallet_MappaTipiMovimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> CDC_FiatWallet_MappaErrori = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> CDC_CardWallet_Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, String> Mappa_EMoney = new TreeMap<>();//Mapa dei token considerati emoney, deve essere case sensitive perchè in alcuni casi dei token si differenziano solo dalle minuscole o maiuscole
    static public Map<String, String> Mappa_RichiesteAPIGiaEffettuate = new TreeMap<>();
    static public Map<String, List<String>> Mappa_Wallets_e_Dettagli = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> Mappa_Wallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, String[]> MappaCryptoWallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);//mappa principale che tiene tutte le movimentazioni crypto
    static public Map<String, String[]> Mappa_ChainExplorer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);//Mappa delle chain per la defi
    static public Map<String, String> Mappa_AddressRete_Nome = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);//Mappa che converte gli address di una rete in nome moneta per binance, serve per l'acquisizione dei prezzi in maniera più precisa
    static public String CDC_FiatWallet_FileDB="crypto.com.fiatwallet.db";
    static String CDC_CardWallet_FileDB="crypto.com.cardwallet.db";
    static String CDC_FileDatiDB="crypto.com.dati.db";
//    static String CDC_FiatWallet_FileTipiMovimentiDB="crypto.com.fiatwallet.tipimovimenti.db";
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
    static public Map<String, List<String[]>> Mappa_RW_ListeXGruppoWallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, List<Moneta>> Mappa_RW_GiacenzeInizioPeriodo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static public Map<String, List<Moneta>> Mappa_RW_GiacenzeFinePeriodo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static List<String> DepositiPrelieviDaCategorizzare;//viene salvata la lista degli id dei depositi e prelievi ancora da categorizzare
    static public List<String>[] CDC_FiatWallet_ListaSaldi;
    static public boolean TabellaCryptodaAggiornare=false;
    static public boolean TransazioniCrypto_DaSalvare=false;//implementata per uso futuro attualmente non ancora utilizzata
    public boolean tabDepositiPrelieviCaricataprimavolta=false;
    public static Object JDialog_Ritorno;
    public boolean VersioneCambiata=false;
    public boolean FineCaricamentoDati=false;
    
    //static String Appoggio="";
    
    
    public CDC_Grafica() {
       
    try {
        
            this.setTitle("Giacenze_Crypto 1.22 Beta");
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
        if (!DatabaseH2.CreaoCollegaDatabase()){
            JOptionPane.showConfirmDialog(null, "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            System.exit(0);
        }
        
        if (Funzioni.CambiataVersione(this.getTitle())) {
            System.out.println("Versione Cambiata");
            VersioneCambiata = true;
        } else {
            VersioneCambiata = false;//intanto così poi verrà utilizzata per altre cose in futuro
        }
        if (VersioneCambiata) {
            DatabaseH2.Opzioni_Scrivi("Data_Lista_Coingecko", "1000000000000");
            Download progress = new Download();
            progress.setLocationRelativeTo(this);
            progress.Titolo("Sistemazione dati per cambio versione... ATTENDERE...");
            progress.SetLabel("Caricamento e sistemazione Dati per cambio versione...");
            progress.NascondiInterrompi();
            progress.NascondiBarra();
            progress.NoModale();
            Thread thread;
            thread = new Thread() {
                public void run() {

                    while (!FineCaricamentoDati) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    progress.ChiudiFinestra();
                }
            };
            thread.start();
            progress.setVisible(true);
        }
        
        Funzioni.CompilaMappaChain();
        this.CDC_FiatWallet_Label_Errore1.setVisible(false);
        this.CDC_FiatWallet_Label_Errore2.setVisible(false);
        this.CDC_FiatWallet_Bottone_Errore.setVisible(false);
        TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(false);
        //Optioni_Export_Wallets_Combobox=GiacenzeaData_Wallet_ComboBox;
        DatabaseH2.CancellaPrezziVuoti();//pulisce il database dai prezzi vuoti delle precedenti sessioni
                                        //in modo tale che se i prezzi tornano ad essere disponibili questi vengono riscaricati
        
        CDC_LeggiFileDatiDB();
        TransazioniCrypto_Funzioni_NascondiColonneTabellaCrypto();
        CDC_FiatWallet_Funzione_ImportaWallet(CDC_FiatWallet_FileDB); 
        CDC_CardWallet_Funzione_ImportaWallet(CDC_CardWallet_FileDB);
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();//Popolo la mappa delle emoneytoken prima di proseguire
        

        

        //boolean successo=DatabaseH2.CreaoCollegaDatabase();
        
        //Aggiorno lo stato del checkbox relativo al calcolo delle plusvalenze
        String PlusXWallet=DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if(PlusXWallet!=null && PlusXWallet.equalsIgnoreCase("SI")){
            Opzioni_GruppoWallet_CheckBox_PlusXWallet.setSelected(true);
        }
        String Plusvalenze_Pre2023EarnCostoZero=DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023EarnCostoZero");
        if(Plusvalenze_Pre2023EarnCostoZero!=null && Plusvalenze_Pre2023EarnCostoZero.equalsIgnoreCase("SI")){
            this.Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero.setSelected(true);
        }
        String Plusvalenze_Pre2023ScambiRilevanti=DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023ScambiRilevanti");
        if(Plusvalenze_Pre2023ScambiRilevanti!=null && Plusvalenze_Pre2023ScambiRilevanti.equalsIgnoreCase("SI")){
            this.Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti.setSelected(true);
        }
        String RW_1RigoXOperazione=DatabaseH2.Pers_Opzioni_Leggi("RW_1RigoXOperazione");
        if(RW_1RigoXOperazione!=null && RW_1RigoXOperazione.equalsIgnoreCase("SI")){
            this.RW_Opzioni_CheckBox_1RigoXOperazione.setSelected(true);
        }else DatabaseH2.Pers_Opzioni_Scrivi("RW_1RigoXOperazione","NO");
        
        
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        CDC_AggiornaGui();
        FineCaricamentoDati=true;

}  catch( Exception ex ) {
             Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
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
        TransazioniCrypto_Bottone_MovimentoNuovo = new javax.swing.JButton();
        TransazioniCrypto_Bottone_MovimentoElimina = new javax.swing.JButton();
        TransazioniCrypto_Bottone_MovimentoModifica = new javax.swing.JButton();
        TransazioniCrypto_TabbedPane = new javax.swing.JTabbedPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        TransazioniCrypto_Tabella_Dettagli = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        Analisi_Crypto = new javax.swing.JPanel();
        AnalisiCrypto = new javax.swing.JTabbedPane();
        DepositiPrelievi = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        DepositiPrelievi_Tabella = new javax.swing.JTable();
        DepositiPrelievi_Bottone_AssegnazioneAutomatica = new javax.swing.JButton();
        DepositiPrelievi_Bottone_AssegnazioneManuale = new javax.swing.JButton();
        DepositiPrelievi_CheckBox_movimentiClassificati = new javax.swing.JCheckBox();
        DepositiPrelievi_Bottone_DettaglioDefi = new javax.swing.JButton();
        DepositiPrelievi_Bottone_CreaMovOpposto = new javax.swing.JButton();
        SituazioneImport = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        SituazioneImport_Tabella1 = new javax.swing.JTable();
        GiacenzeaData = new javax.swing.JPanel();
        GiacenzeaData_Wallet_Label = new javax.swing.JLabel();
        GiacenzeaData_Wallet_ComboBox = new javax.swing.JComboBox<>();
        GiacenzeaData_Data_Label = new javax.swing.JLabel();
        GiacenzeaData_Data_DataChooser = new com.toedter.calendar.JDateChooser();
        GiacenzeaData_ScrollPane = new javax.swing.JScrollPane();
        GiacenzeaData_Tabella = new javax.swing.JTable();
        GiacenzeaData_Totali_Label = new javax.swing.JLabel();
        GiacenzeaData_Totali_TextField = new javax.swing.JTextField();
        GiacenzeaData_Bottone_Calcola = new javax.swing.JButton();
        GiacenzeaData_ScrollPaneDettaglioMovimenti = new javax.swing.JScrollPane();
        GiacenzeaData_TabellaDettaglioMovimenti = new javax.swing.JTable();
        Giacenzeadata_Dettaglio_Label = new javax.swing.JLabel();
        Giacenzeadata_Walleta_Label = new javax.swing.JLabel();
        GiacenzeaData_WalletEsame_Label = new javax.swing.JLabel();
        GiacenzeaData_Bottone_ModificaValore = new javax.swing.JButton();
        GiacenzeaData_Bottone_MovimentiDefi = new javax.swing.JButton();
        GiacenzeaData_Bottone_GiacenzeExplorer = new javax.swing.JButton();
        GiacenzeaData_Bottone_RettificaQta = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        GiacenzeaData_CheckBox_MostraQtaZero = new javax.swing.JCheckBox();
        GiacenzeaData_Bottone_Scam = new javax.swing.JButton();
        GiacenzeaData_Bottone_CambiaNomeToken = new javax.swing.JButton();
        GiacenzeaData_Wallet2_Label = new javax.swing.JLabel();
        GiacenzeaData_Wallet2_ComboBox = new javax.swing.JComboBox<>();
        Giacenzeadata_Walletb_Label = new javax.swing.JLabel();
        GiacenzeaData_CheckBox_NascondiScam = new javax.swing.JCheckBox();
        RW = new javax.swing.JPanel();
        RW_Anno_ComboBox = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        RW_Tabella = new javax.swing.JTable();
        jScrollPane8 = new javax.swing.JScrollPane();
        RW_Tabella_Dettagli = new javax.swing.JTable();
        RW_Bottone_Calcola = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        RW_Tabella_DettaglioMovimenti = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        RW_Bottone_CorreggiErrore = new javax.swing.JButton();
        RW_Bottone_IdentificaScam = new javax.swing.JButton();
        RW_Bottone_ModificaVFinale = new javax.swing.JButton();
        RW_Bottone_ModificaVIniziale = new javax.swing.JButton();
        RW_CheckBox_VediSoloErrori = new javax.swing.JCheckBox();
        RW_Label_SegnalaErrori = new javax.swing.JLabel();
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
        Opzioni = new javax.swing.JPanel();
        Opzioni_TabbedPane = new javax.swing.JTabbedPane();
        Opzioni_CardWallet_Pannello = new javax.swing.JPanel();
        CDC_Opzioni_Bottone_CancellaCardWallet = new javax.swing.JButton();
        Opzioni_FiatWallet_Pannello = new javax.swing.JPanel();
        CDC_Opzioni_Bottone_CancellaFiatWallet = new javax.swing.JButton();
        CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet = new javax.swing.JButton();
        Opzioni_Crypto_Pannello = new javax.swing.JPanel();
        Opzioni_Bottone_CancellaTransazioniCrypto = new javax.swing.JButton();
        Opzioni_Bottone_CancellaTransazioniCryptoXwallet = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet = new javax.swing.JComboBox<>();
        Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti = new javax.swing.JCheckBox();
        Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero = new javax.swing.JCheckBox();
        RW_Opzioni_CheckBox_1RigoXOperazione = new javax.swing.JCheckBox();
        Opzioni_GruppoWallet_Pannello = new javax.swing.JPanel();
        Opzioni_GruppoWallet_ScrollTabella = new javax.swing.JScrollPane();
        Opzioni_GruppoWallet_Tabella = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        Opzioni_GruppoWallet_CheckBox_PlusXWallet = new javax.swing.JCheckBox();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        Opzioni_Emoney_Pannello = new javax.swing.JPanel();
        Opzioni_Emoney_ScrollPane = new javax.swing.JScrollPane();
        Opzioni_Emoney_Tabella = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        Opzioni_Emoney_Bottone_Rimuovi = new javax.swing.JButton();
        Opzioni_Emoney_Bottone_Aggiungi = new javax.swing.JButton();
        Opzioni_Export_Pannello = new javax.swing.JPanel();
        Opzioni_Export_Wallets_Combobox = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        Opzioni_Export_Tatax_Bottone = new javax.swing.JButton();
        Opzioni_Export_EsportaPrezzi_CheckBox = new javax.swing.JCheckBox();
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
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        TransazioniCryptoTabella.setAutoCreateRowSorter(true);
        TransazioniCryptoTabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "<html><center>ID<br>Transazione</html>", "Data e Ora", "<html><center>Numero<br>movimento<br>su Totale<br>movimenti</html>", "<html><center>Exchange<br>/<br>Wallet</html>", "<html><center>Dettaglio<br>Wallet</html>", "<html><center>Tipo<br>Transazione<br></html>", "<html><center>Dettaglio<br>Movimento<br></html>", "<html><center>Causale<br>originale<br></html>", "<html><center>Moneta<br>Ven./Trasf.</html>", "<html><center>Tipo<br>Moneta<br>Ven./Trasf.</html>", "<html><center>Qta<br>Ven./Trasf.</html>", "<html><center>Moneta<br>Acq./Ric.</html>", "<html><center>Tipo<br>Moneta<br>Acq./Ric.</html>", "<html><center>Qta<br>Acq./Ric.</html>", "<html><center>Valore <br>transazione<br>come da CSV</html>", "<html><center>Valore<br>transazione<br>in EURO</html>", "<html><center>Non Utilizzata</html>", "<html><center>Nuovo<br>Costo di Carico<br>in EURO</html>", "<html><center><html><center>Non Utilizzata</html></html>", "<html><center>Plusvalenza<br>in EURO</html>", "<html><center>Riferimento<br>Trasferimento</html>", "Note", "Auto", "ND", "ND", "ND", "ND", "ND", "ND", "ND", "ND"
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
        TransazioniCrypto_Bottone_DettaglioDefi.setEnabled(false);
        TransazioniCrypto_Bottone_DettaglioDefi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_DettaglioDefiActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_MovimentoNuovo.setText("Nuovo Movimento");
        TransazioniCrypto_Bottone_MovimentoNuovo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_MovimentoNuovoActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_MovimentoElimina.setText("Elimina Movimento Selezionato");
        TransazioniCrypto_Bottone_MovimentoElimina.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_MovimentoEliminaActionPerformed(evt);
            }
        });

        TransazioniCrypto_Bottone_MovimentoModifica.setText("Modifica Movimento Selezionato");
        TransazioniCrypto_Bottone_MovimentoModifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransazioniCrypto_Bottone_MovimentoModificaActionPerformed(evt);
            }
        });

        TransazioniCrypto_TabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        TransazioniCrypto_Tabella_Dettagli.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome", "Valore"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
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
        TransazioniCrypto_Tabella_Dettagli.setColumnSelectionAllowed(true);
        jScrollPane4.setViewportView(TransazioniCrypto_Tabella_Dettagli);
        if (TransazioniCrypto_Tabella_Dettagli.getColumnModel().getColumnCount() > 0) {
            TransazioniCrypto_Tabella_Dettagli.getColumnModel().getColumn(0).setMinWidth(160);
            TransazioniCrypto_Tabella_Dettagli.getColumnModel().getColumn(0).setPreferredWidth(160);
            TransazioniCrypto_Tabella_Dettagli.getColumnModel().getColumn(0).setMaxWidth(160);
        }

        TransazioniCrypto_TabbedPane.addTab("Dettagli Riga", jScrollPane4);

        jButton1.setText("Esporta Tabella in CSV");
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
                    .addComponent(TransazioniCrypto_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1079, Short.MAX_VALUE)
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addComponent(TransazioniCrypto_Bottone_Importa)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_InserisciWallet)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 284, Short.MAX_VALUE)
                        .addComponent(TransazioniCrypto_Label_MovimentiNonSalvati, javax.swing.GroupLayout.PREFERRED_SIZE, 508, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_Salva, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TransazioniCrypto_Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6))
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                                .addComponent(TransazioniCrypto_Bottone_MovimentoNuovo)
                                .addGap(18, 18, 18)
                                .addComponent(TransazioniCrypto_Bottone_MovimentoModifica)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 238, Short.MAX_VALUE)
                                .addComponent(TransazioniCrypto_Bottone_MovimentoElimina))
                            .addComponent(TransazioniCrypto_TabbedPane))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TransazioniCrypto_Text_Plusvalenza)
                                    .addComponent(TransazioniCrypto_Label_Plusvalenza, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)))
                            .addComponent(TransazioniCrypto_CheckBox_EscludiTI, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TransazioniCryptoLayout.createSequentialGroup()
                                .addComponent(TransazioniCrypto_Label_Filtro)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TransazioniCryptoFiltro_Text, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                                .addComponent(TransazioniCrypto_Bottone_DettaglioDefi)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1)))
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
                .addComponent(TransazioniCrypto_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TransazioniCrypto_Label_Plusvalenza)
                    .addComponent(TransazioniCrypto_Bottone_MovimentoNuovo)
                    .addComponent(TransazioniCrypto_Bottone_MovimentoElimina)
                    .addComponent(TransazioniCrypto_Bottone_MovimentoModifica))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(TransazioniCryptoLayout.createSequentialGroup()
                        .addComponent(TransazioniCrypto_Text_Plusvalenza, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TransazioniCrypto_Bottone_DettaglioDefi)
                            .addComponent(jButton1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TransazioniCrypto_CheckBox_EscludiTI)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(TransazioniCryptoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TransazioniCryptoFiltro_Text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TransazioniCrypto_Label_Filtro)))
                    .addComponent(TransazioniCrypto_TabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
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

            },
            new String [] {
                "ID_Transazione", "Data e Ora", "Exchange / Wallet", "Tipo Transazione", "Moneta", "Qta", "Dettaglio Trasferimento", "Prezzo", "Dett. Defi/CSV", "Controparte"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
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
            DepositiPrelievi_Tabella.getColumnModel().getColumn(2).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(2).setMaxWidth(400);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(3).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(3).setMaxWidth(200);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setMinWidth(60);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setPreferredWidth(60);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(4).setMaxWidth(240);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setPreferredWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(5).setMaxWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(6).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(7).setMinWidth(70);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(7).setMaxWidth(70);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(8).setMinWidth(100);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(8).setMaxWidth(200);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(9).setMinWidth(50);
            DepositiPrelievi_Tabella.getColumnModel().getColumn(9).setMaxWidth(400);
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

        DepositiPrelievi_Bottone_DettaglioDefi.setText("Dettaglio Defi");
        DepositiPrelievi_Bottone_DettaglioDefi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DepositiPrelievi_Bottone_DettaglioDefiActionPerformed(evt);
            }
        });

        DepositiPrelievi_Bottone_CreaMovOpposto.setText("Crea Movimento Opposto su altro Wallet");
        DepositiPrelievi_Bottone_CreaMovOpposto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DepositiPrelievi_Bottone_CreaMovOppostoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout DepositiPrelieviLayout = new javax.swing.GroupLayout(DepositiPrelievi);
        DepositiPrelievi.setLayout(DepositiPrelieviLayout);
        DepositiPrelieviLayout.setHorizontalGroup(
            DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1003, Short.MAX_VALUE)
                    .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                        .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                                .addComponent(DepositiPrelievi_Bottone_AssegnazioneAutomatica)
                                .addGap(252, 252, 252)
                                .addComponent(DepositiPrelievi_Bottone_CreaMovOpposto))
                            .addComponent(DepositiPrelievi_CheckBox_movimentiClassificati, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 196, Short.MAX_VALUE)
                        .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(DepositiPrelievi_Bottone_AssegnazioneManuale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(DepositiPrelievi_Bottone_DettaglioDefi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        DepositiPrelieviLayout.setVerticalGroup(
            DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DepositiPrelieviLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DepositiPrelievi_CheckBox_movimentiClassificati)
                    .addComponent(DepositiPrelievi_Bottone_DettaglioDefi))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(DepositiPrelieviLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DepositiPrelievi_Bottone_AssegnazioneAutomatica)
                    .addComponent(DepositiPrelievi_Bottone_AssegnazioneManuale)
                    .addComponent(DepositiPrelievi_Bottone_CreaMovOpposto)))
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
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1015, Short.MAX_VALUE)
        );
        SituazioneImportLayout.setVerticalGroup(
            SituazioneImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SituazioneImportLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap())
        );

        AnalisiCrypto.addTab("Sitazione Import Crypto", SituazioneImport);

        GiacenzeaData.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                GiacenzeaDataComponentShown(evt);
            }
        });

        GiacenzeaData_Wallet_Label.setText("Wallet : ");

        GiacenzeaData_Wallet_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tutti" }));
        GiacenzeaData_Wallet_ComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                GiacenzeaData_Wallet_ComboBoxItemStateChanged(evt);
            }
        });
        GiacenzeaData_Wallet_ComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                GiacenzeaData_Wallet_ComboBoxMouseClicked(evt);
            }
        });

        GiacenzeaData_Data_Label.setText("Data : ");

        GiacenzeaData_Data_DataChooser.setDateFormatString("yyyy-MM-dd");

        GiacenzeaData_Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome", "Rete", "Address Defi", "Tipo", "Qta", "<html><center>Valore<br>(in Euro)</html>"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        GiacenzeaData_Tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                GiacenzeaData_TabellaMouseReleased(evt);
            }
        });
        GiacenzeaData_Tabella.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                GiacenzeaData_TabellaKeyReleased(evt);
            }
        });
        GiacenzeaData_ScrollPane.setViewportView(GiacenzeaData_Tabella);
        if (GiacenzeaData_Tabella.getColumnModel().getColumnCount() > 0) {
            GiacenzeaData_Tabella.getColumnModel().getColumn(1).setMinWidth(40);
            GiacenzeaData_Tabella.getColumnModel().getColumn(1).setPreferredWidth(40);
            GiacenzeaData_Tabella.getColumnModel().getColumn(1).setMaxWidth(40);
            GiacenzeaData_Tabella.getColumnModel().getColumn(3).setMinWidth(50);
            GiacenzeaData_Tabella.getColumnModel().getColumn(3).setPreferredWidth(50);
            GiacenzeaData_Tabella.getColumnModel().getColumn(3).setMaxWidth(50);
            GiacenzeaData_Tabella.getColumnModel().getColumn(5).setMinWidth(100);
            GiacenzeaData_Tabella.getColumnModel().getColumn(5).setPreferredWidth(100);
            GiacenzeaData_Tabella.getColumnModel().getColumn(5).setMaxWidth(100);
        }
        GiacenzeaData_Tabella.getTableHeader().setPreferredSize(new Dimension(TransazioniCryptoTabella.getColumnModel().getTotalColumnWidth(), 32));

        GiacenzeaData_Totali_Label.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        GiacenzeaData_Totali_Label.setText("TOTALE in EURO : ");

        GiacenzeaData_Totali_TextField.setEditable(false);

        GiacenzeaData_Bottone_Calcola.setBackground(new java.awt.Color(255, 240, 195));
        GiacenzeaData_Bottone_Calcola.setText("Calcola");
        GiacenzeaData_Bottone_Calcola.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_CalcolaActionPerformed(evt);
            }
        });

        GiacenzeaData_TabellaDettaglioMovimenti.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Data", "Wallet", "Moneta", "Address Moneta", "Tipo Movimento", "Quantita'", "Valore in Euro", "Qta Residua", "ID"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        GiacenzeaData_TabellaDettaglioMovimenti.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                GiacenzeaData_TabellaDettaglioMovimentiMouseReleased(evt);
            }
        });
        GiacenzeaData_TabellaDettaglioMovimenti.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                GiacenzeaData_TabellaDettaglioMovimentiKeyReleased(evt);
            }
        });
        GiacenzeaData_ScrollPaneDettaglioMovimenti.setViewportView(GiacenzeaData_TabellaDettaglioMovimenti);
        if (GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumnCount() > 0) {
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(0).setMinWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(0).setPreferredWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(0).setMaxWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(2).setPreferredWidth(50);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(6).setMinWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(6).setPreferredWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(6).setMaxWidth(100);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(8).setMinWidth(1);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(8).setPreferredWidth(1);
            GiacenzeaData_TabellaDettaglioMovimenti.getColumnModel().getColumn(8).setMaxWidth(1);
        }

        Giacenzeadata_Dettaglio_Label.setText("Tabella dettaglio movimenti :");

        Giacenzeadata_Walleta_Label.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Giacenzeadata_Walleta_Label.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                Giacenzeadata_Walleta_LabelPropertyChange(evt);
            }
        });

        GiacenzeaData_WalletEsame_Label.setText("Wallet in Esame :");

        GiacenzeaData_Bottone_ModificaValore.setText("Modifica Valore");
        GiacenzeaData_Bottone_ModificaValore.setToolTipText("<html>Modifica il valore globale della giacenza del token evidenziato<br><\\html>");
        GiacenzeaData_Bottone_ModificaValore.setMaximumSize(new java.awt.Dimension(144, 22));
        GiacenzeaData_Bottone_ModificaValore.setMinimumSize(new java.awt.Dimension(144, 22));
        GiacenzeaData_Bottone_ModificaValore.setPreferredSize(new java.awt.Dimension(144, 22));
        GiacenzeaData_Bottone_ModificaValore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                GiacenzeaData_Bottone_ModificaValoreMouseClicked(evt);
            }
        });
        GiacenzeaData_Bottone_ModificaValore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_ModificaValoreActionPerformed(evt);
            }
        });

        GiacenzeaData_Bottone_MovimentiDefi.setText("Movimenti Defi");
        GiacenzeaData_Bottone_MovimentiDefi.setEnabled(false);
        GiacenzeaData_Bottone_MovimentiDefi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_MovimentiDefiActionPerformed(evt);
            }
        });

        GiacenzeaData_Bottone_GiacenzeExplorer.setText("<html>\nVedi situazione <br>\nWallet ad Oggi\n</html>");
        GiacenzeaData_Bottone_GiacenzeExplorer.setEnabled(false);
        GiacenzeaData_Bottone_GiacenzeExplorer.setMaximumSize(new java.awt.Dimension(72, 23));
        GiacenzeaData_Bottone_GiacenzeExplorer.setMinimumSize(new java.awt.Dimension(72, 23));
        GiacenzeaData_Bottone_GiacenzeExplorer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                GiacenzeaData_Bottone_GiacenzeExplorerMouseClicked(evt);
            }
        });

        GiacenzeaData_Bottone_RettificaQta.setText("Sistema Qta Residua");
        GiacenzeaData_Bottone_RettificaQta.setEnabled(false);
        GiacenzeaData_Bottone_RettificaQta.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                GiacenzeaData_Bottone_RettificaQtaMouseReleased(evt);
            }
        });
        GiacenzeaData_Bottone_RettificaQta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_RettificaQtaActionPerformed(evt);
            }
        });

        GiacenzeaData_CheckBox_MostraQtaZero.setText("Mostra Giacenze a Zero");
        GiacenzeaData_CheckBox_MostraQtaZero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_CheckBox_MostraQtaZeroActionPerformed(evt);
            }
        });

        GiacenzeaData_Bottone_Scam.setText("Identifica come SCAM");
        GiacenzeaData_Bottone_Scam.setEnabled(false);
        GiacenzeaData_Bottone_Scam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_ScamActionPerformed(evt);
            }
        });

        GiacenzeaData_Bottone_CambiaNomeToken.setText("Cambia Nome Token");
        GiacenzeaData_Bottone_CambiaNomeToken.setEnabled(false);
        GiacenzeaData_Bottone_CambiaNomeToken.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_Bottone_CambiaNomeTokenActionPerformed(evt);
            }
        });

        GiacenzeaData_Wallet2_Label.setText("Dett.Wallet :");

        GiacenzeaData_Wallet2_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tutti" }));

        GiacenzeaData_CheckBox_NascondiScam.setText("Nascondi Token Scam");
        GiacenzeaData_CheckBox_NascondiScam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiacenzeaData_CheckBox_NascondiScamActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout GiacenzeaDataLayout = new javax.swing.GroupLayout(GiacenzeaData);
        GiacenzeaData.setLayout(GiacenzeaDataLayout);
        GiacenzeaDataLayout.setHorizontalGroup(
            GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GiacenzeaData_ScrollPane)
                    .addComponent(GiacenzeaData_ScrollPaneDettaglioMovimenti)
                    .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                        .addComponent(GiacenzeaData_Totali_Label)
                        .addGap(1, 1, 1)
                        .addComponent(GiacenzeaData_Totali_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(GiacenzeaData_Bottone_RettificaQta))
                    .addComponent(jSeparator5)
                    .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                        .addComponent(GiacenzeaData_Bottone_MovimentiDefi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(GiacenzeaData_CheckBox_MostraQtaZero)
                        .addGap(18, 18, 18)
                        .addComponent(GiacenzeaData_CheckBox_NascondiScam, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(GiacenzeaData_Bottone_CambiaNomeToken, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(GiacenzeaData_Bottone_Scam)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(GiacenzeaData_Bottone_ModificaValore, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                        .addComponent(Giacenzeadata_Dettaglio_Label, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, GiacenzeaDataLayout.createSequentialGroup()
                        .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                                .addComponent(GiacenzeaData_Wallet_Label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(GiacenzeaData_Wallet_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 428, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 59, Short.MAX_VALUE)
                                .addComponent(GiacenzeaData_Wallet2_Label, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                                .addComponent(GiacenzeaData_WalletEsame_Label, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Giacenzeadata_Walleta_Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(Giacenzeadata_Walletb_Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addComponent(GiacenzeaData_Data_Label)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                                .addComponent(GiacenzeaData_Data_DataChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(GiacenzeaData_Bottone_GiacenzeExplorer, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(GiacenzeaData_Bottone_Calcola, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(GiacenzeaData_Wallet2_ComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
        );
        GiacenzeaDataLayout.setVerticalGroup(
            GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GiacenzeaData_Wallet_Label)
                    .addComponent(GiacenzeaData_Wallet_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GiacenzeaData_Wallet2_Label)
                    .addComponent(GiacenzeaData_Wallet2_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GiacenzeaData_Data_Label)
                            .addComponent(GiacenzeaData_Data_DataChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(GiacenzeaData_Bottone_Calcola, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                    .addComponent(GiacenzeaData_WalletEsame_Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GiacenzeaData_Bottone_GiacenzeExplorer, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(GiacenzeaDataLayout.createSequentialGroup()
                        .addComponent(Giacenzeadata_Walleta_Label, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Giacenzeadata_Walletb_Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(GiacenzeaData_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(GiacenzeaData_Bottone_ModificaValore, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GiacenzeaData_Bottone_Scam, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GiacenzeaData_Bottone_MovimentiDefi, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(GiacenzeaData_CheckBox_MostraQtaZero)
                        .addComponent(GiacenzeaData_CheckBox_NascondiScam))
                    .addComponent(GiacenzeaData_Bottone_CambiaNomeToken, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Giacenzeadata_Dettaglio_Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(GiacenzeaData_ScrollPaneDettaglioMovimenti, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GiacenzeaDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GiacenzeaData_Totali_Label)
                    .addComponent(GiacenzeaData_Totali_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GiacenzeaData_Bottone_RettificaQta)))
        );

        AnalisiCrypto.addTab("Giacenze a Data", GiacenzeaData);

        RW_Anno_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2023" }));

        jLabel4.setText("Anno :");

        RW_Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "RW", "Val. Iniziale", "Val. Finale", "Giorni di Detenzione", "Errori"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        RW_Tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                RW_TabellaMouseReleased(evt);
            }
        });
        RW_Tabella.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                RW_TabellaKeyReleased(evt);
            }
        });
        jScrollPane7.setViewportView(RW_Tabella);

        RW_Tabella_Dettagli.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Anno", "Gr. Inizio", "Mon. Inizio", "Qta Inizio", "Data Inizio", "Val. Inizio", "Gr. Fine", "Mon. Fine", "Qta Fine", "Data Fine", "Val. Finale", "Giorni", "Causale", "IDApertura", "IDChiusura", "Errore / Avvisi", "IDMovimentati"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        RW_Tabella_Dettagli.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                RW_Tabella_DettagliMouseReleased(evt);
            }
        });
        RW_Tabella_Dettagli.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                RW_Tabella_DettagliKeyReleased(evt);
            }
        });
        jScrollPane8.setViewportView(RW_Tabella_Dettagli);
        if (RW_Tabella_Dettagli.getColumnModel().getColumnCount() > 0) {
            RW_Tabella_Dettagli.getColumnModel().getColumn(0).setMinWidth(40);
            RW_Tabella_Dettagli.getColumnModel().getColumn(0).setPreferredWidth(40);
            RW_Tabella_Dettagli.getColumnModel().getColumn(0).setMaxWidth(40);
            RW_Tabella_Dettagli.getColumnModel().getColumn(1).setMinWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(1).setPreferredWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(1).setMaxWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(2).setMinWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(2).setPreferredWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(2).setMaxWidth(150);
            RW_Tabella_Dettagli.getColumnModel().getColumn(3).setMinWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(3).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(3).setMaxWidth(150);
            RW_Tabella_Dettagli.getColumnModel().getColumn(4).setMinWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(4).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(4).setMaxWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(5).setMinWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(5).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(5).setMaxWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(6).setMinWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(6).setPreferredWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(6).setMaxWidth(65);
            RW_Tabella_Dettagli.getColumnModel().getColumn(7).setMinWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(7).setPreferredWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(7).setMaxWidth(150);
            RW_Tabella_Dettagli.getColumnModel().getColumn(8).setMinWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(8).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(8).setMaxWidth(150);
            RW_Tabella_Dettagli.getColumnModel().getColumn(9).setMinWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(9).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(9).setMaxWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(10).setMinWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(10).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(10).setMaxWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(11).setMinWidth(25);
            RW_Tabella_Dettagli.getColumnModel().getColumn(11).setPreferredWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(11).setMaxWidth(50);
            RW_Tabella_Dettagli.getColumnModel().getColumn(12).setMinWidth(25);
            RW_Tabella_Dettagli.getColumnModel().getColumn(12).setPreferredWidth(100);
            RW_Tabella_Dettagli.getColumnModel().getColumn(12).setMaxWidth(200);
            RW_Tabella_Dettagli.getColumnModel().getColumn(13).setMinWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(13).setPreferredWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(13).setMaxWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(14).setMinWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(14).setPreferredWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(14).setMaxWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(16).setMinWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(16).setPreferredWidth(0);
            RW_Tabella_Dettagli.getColumnModel().getColumn(16).setMaxWidth(0);
        }

        RW_Bottone_Calcola.setText("Calcola");
        RW_Bottone_Calcola.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Bottone_CalcolaActionPerformed(evt);
            }
        });

        RW_Tabella_DettaglioMovimenti.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Data", "Wallet", "Tipo Movimento", "Mon.Uscita/Fine Anno", "Mon.Entrata/Iniz.Anno", "Valore", "ID Transazione"
            }
        ));
        jScrollPane9.setViewportView(RW_Tabella_DettaglioMovimenti);
        if (RW_Tabella_DettaglioMovimenti.getColumnModel().getColumnCount() > 0) {
            RW_Tabella_DettaglioMovimenti.getColumnModel().getColumn(5).setMinWidth(100);
            RW_Tabella_DettaglioMovimenti.getColumnModel().getColumn(5).setPreferredWidth(100);
            RW_Tabella_DettaglioMovimenti.getColumnModel().getColumn(5).setMaxWidth(100);
        }

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("Dettaglio RW");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Movimenti dell'anno coinvolti");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("RW Aggregato");

        RW_Bottone_CorreggiErrore.setText("Corrreggi Errore");
        RW_Bottone_CorreggiErrore.setEnabled(false);
        RW_Bottone_CorreggiErrore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Bottone_CorreggiErroreActionPerformed(evt);
            }
        });

        RW_Bottone_IdentificaScam.setText("Identifica come SPAM");
        RW_Bottone_IdentificaScam.setEnabled(false);
        RW_Bottone_IdentificaScam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Bottone_IdentificaScamActionPerformed(evt);
            }
        });

        RW_Bottone_ModificaVFinale.setText("Modifica Valore Finale");
        RW_Bottone_ModificaVFinale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Bottone_ModificaVFinaleActionPerformed(evt);
            }
        });

        RW_Bottone_ModificaVIniziale.setText("Modifica Valore Iniziale");
        RW_Bottone_ModificaVIniziale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Bottone_ModificaVInizialeActionPerformed(evt);
            }
        });

        RW_CheckBox_VediSoloErrori.setText("Vedi solo movimenti con errori");
        RW_CheckBox_VediSoloErrori.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_CheckBox_VediSoloErroriActionPerformed(evt);
            }
        });

        RW_Label_SegnalaErrori.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        RW_Label_SegnalaErrori.setForeground(new java.awt.Color(255, 0, 51));

        javax.swing.GroupLayout RWLayout = new javax.swing.GroupLayout(RW);
        RW.setLayout(RWLayout);
        RWLayout.setHorizontalGroup(
            RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RWLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 1003, Short.MAX_VALUE)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane9)
                    .addGroup(RWLayout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RW_Anno_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RW_Bottone_Calcola))
                    .addGroup(RWLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(RW_Label_SegnalaErrori, javax.swing.GroupLayout.PREFERRED_SIZE, 628, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(RW_CheckBox_VediSoloErrori))
                    .addGroup(RWLayout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(RW_Bottone_ModificaVIniziale)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RW_Bottone_ModificaVFinale)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RW_Bottone_IdentificaScam)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RW_Bottone_CorreggiErrore, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        RWLayout.setVerticalGroup(
            RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RWLayout.createSequentialGroup()
                .addGroup(RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RW_Anno_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(RW_Bottone_Calcola)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RW_Label_SegnalaErrori, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(RW_CheckBox_VediSoloErrori)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                .addGap(1, 1, 1)
                .addGroup(RWLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(RW_Bottone_CorreggiErrore)
                    .addComponent(RW_Bottone_IdentificaScam)
                    .addComponent(RW_Bottone_ModificaVFinale)
                    .addComponent(RW_Bottone_ModificaVIniziale))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addContainerGap())
        );

        AnalisiCrypto.addTab("RW/W (Met.LIFO vers. di test)", RW);

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
                            .addComponent(CDC_CardWallet_Label_Tabella1, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                            .addComponent(CDC_CardWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CDC_CardWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
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
                            .addComponent(CDC_CardWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                            .addComponent(CDC_CardWallet_Tabella1Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_CardWallet_Label_FiltroTabelle)
                    .addComponent(CDC_CardWallet_Text_FiltroTabelle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        CDC.addTab("Carta CdC", CDC_CardWallet_Pannello);

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
                                .addGap(0, 320, Short.MAX_VALUE))
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
                    .addComponent(CDC_FiatWallet_Tabella2Scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                    .addComponent(CDC_FiatWallet_Tabella3Scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CDC_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CDC_FiatWallet_Label_FiltroTabella)
                    .addComponent(CDC_FiatWallet_Text_FiltroTabella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        CDC.addTab("Fiat Wallet CdC", CDC_FiatWallet_Pannello);

        Opzioni.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                OpzioniComponentShown(evt);
            }
        });

        Opzioni_TabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        CDC_Opzioni_Bottone_CancellaCardWallet.setText("Elimina tutti i dati dal Card Wallet");
        CDC_Opzioni_Bottone_CancellaCardWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CDC_Opzioni_Bottone_CancellaCardWalletActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout Opzioni_CardWallet_PannelloLayout = new javax.swing.GroupLayout(Opzioni_CardWallet_Pannello);
        Opzioni_CardWallet_Pannello.setLayout(Opzioni_CardWallet_PannelloLayout);
        Opzioni_CardWallet_PannelloLayout.setHorizontalGroup(
            Opzioni_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_CardWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CDC_Opzioni_Bottone_CancellaCardWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 931, Short.MAX_VALUE)
                .addContainerGap())
        );
        Opzioni_CardWallet_PannelloLayout.setVerticalGroup(
            Opzioni_CardWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_CardWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CDC_Opzioni_Bottone_CancellaCardWallet)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Opzioni_TabbedPane.addTab("CDC Card Wallet", Opzioni_CardWallet_Pannello);

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

        javax.swing.GroupLayout Opzioni_FiatWallet_PannelloLayout = new javax.swing.GroupLayout(Opzioni_FiatWallet_Pannello);
        Opzioni_FiatWallet_Pannello.setLayout(Opzioni_FiatWallet_PannelloLayout);
        Opzioni_FiatWallet_PannelloLayout.setHorizontalGroup(
            Opzioni_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_FiatWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Opzioni_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CDC_Opzioni_Bottone_CancellaFiatWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 931, Short.MAX_VALUE)
                    .addComponent(CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet, javax.swing.GroupLayout.DEFAULT_SIZE, 931, Short.MAX_VALUE))
                .addContainerGap())
        );
        Opzioni_FiatWallet_PannelloLayout.setVerticalGroup(
            Opzioni_FiatWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_FiatWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CDC_Opzioni_Bottone_CancellaFiatWallet)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Opzioni_TabbedPane.addTab("CDC Fiat Wallet", Opzioni_FiatWallet_Pannello);

        Opzioni_Bottone_CancellaTransazioniCrypto.setText("Elimina tutte le transazioni Crypto");
        Opzioni_Bottone_CancellaTransazioniCrypto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed(evt);
            }
        });

        Opzioni_Bottone_CancellaTransazioniCryptoXwallet.setText("Elimina Dati singolo Wallet/Exchange");
        Opzioni_Bottone_CancellaTransazioniCryptoXwallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed(evt);
            }
        });

        jLabel8.setText("->");

        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "----------" }));

        Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti.setText("Fino al 31-12-2022 considera tutti gli scambi crypto-crypto fiscalmente rilevanti (calcola plusvalenza e nuovo costo di carico)");
        Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevantiActionPerformed(evt);
            }
        });

        Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero.setText("Fino al 31-12-2022 considera tutti gli earn,cashback,staking,airdrop etc... come token a costo di carico zero");
        Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZeroActionPerformed(evt);
            }
        });

        RW_Opzioni_CheckBox_1RigoXOperazione.setText("<html><b>Quadro RW :</b> Ogni operazione crea un nuovo rigo RW (Diversamente viene creato un nuovo riso solo sulle operazioni fiscalmente rilevanti)</html>");
        RW_Opzioni_CheckBox_1RigoXOperazione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RW_Opzioni_CheckBox_1RigoXOperazioneActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout Opzioni_Crypto_PannelloLayout = new javax.swing.GroupLayout(Opzioni_Crypto_Pannello);
        Opzioni_Crypto_Pannello.setLayout(Opzioni_Crypto_PannelloLayout);
        Opzioni_Crypto_PannelloLayout.setHorizontalGroup(
            Opzioni_Crypto_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_Crypto_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Opzioni_Crypto_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Opzioni_Bottone_CancellaTransazioniCrypto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(Opzioni_Crypto_PannelloLayout.createSequentialGroup()
                        .addComponent(Opzioni_Bottone_CancellaTransazioniCryptoXwallet, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Opzioni_Combobox_CancellaTransazioniCryptoXwallet, 0, 748, Short.MAX_VALUE))
                    .addGroup(Opzioni_Crypto_PannelloLayout.createSequentialGroup()
                        .addGroup(Opzioni_Crypto_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti)
                            .addComponent(Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero)
                            .addComponent(RW_Opzioni_CheckBox_1RigoXOperazione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Opzioni_Crypto_PannelloLayout.setVerticalGroup(
            Opzioni_Crypto_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_Crypto_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Opzioni_Bottone_CancellaTransazioniCrypto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Opzioni_Crypto_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Opzioni_Bottone_CancellaTransazioniCryptoXwallet)
                    .addComponent(jLabel8)
                    .addComponent(Opzioni_Combobox_CancellaTransazioniCryptoXwallet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(85, 85, 85)
                .addComponent(RW_Opzioni_CheckBox_1RigoXOperazione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(44, 44, 44)
                .addComponent(Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero)
                .addContainerGap(358, Short.MAX_VALUE))
        );

        Opzioni_TabbedPane.addTab("Crypto", Opzioni_Crypto_Pannello);

        Opzioni_GruppoWallet_Tabella.setAutoCreateRowSorter(true);
        Opzioni_GruppoWallet_Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome Wallet", "Gruppo Wallet"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Opzioni_GruppoWallet_Tabella.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                Opzioni_GruppoWallet_TabellaFocusGained(evt);
            }
        });
        Opzioni_GruppoWallet_ScrollTabella.setViewportView(Opzioni_GruppoWallet_Tabella);

        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTextArea1.setRows(3);
        jTextArea1.setText("Per ogni Wallet selezionare un gruppo di appartenenza (più Nomi Wallet possono essere associati allo stesso Gruppo Wallet).\nVerrà generato un Quadro RW per ogni Gruppo.");
        jTextArea1.setPreferredSize(new java.awt.Dimension(774, 44));
        jScrollPane1.setViewportView(jTextArea1);

        Opzioni_GruppoWallet_CheckBox_PlusXWallet.setText("Abilita Calcolo Plusvalenze per Gruppo Wallet");
        Opzioni_GruppoWallet_CheckBox_PlusXWallet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_GruppoWallet_CheckBox_PlusXWalletActionPerformed(evt);
            }
        });

        jScrollPane6.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setText("Se biffata questa opzione i gruppi wallet verranno utilizzati anche per il calcolo delle plusvalenze.\nQuesto significa che, ad esempio, il costo dei BTC del gruppo \"Wallet 1\" non verranno considerati nel calcolo delle Plusvalenze di un BTC venduto nel gruppo \"Wallet 2\".\nViceversa tutti i BTC dei vari wallet verranno considerati come facenti parte di un unico grande portafoglio per il calcolo delle Plusvalenze.");
        jTextArea2.setMinimumSize(new java.awt.Dimension(897, 45));
        jTextArea2.setPreferredSize(new java.awt.Dimension(897, 70));
        jScrollPane6.setViewportView(jTextArea2);

        javax.swing.GroupLayout Opzioni_GruppoWallet_PannelloLayout = new javax.swing.GroupLayout(Opzioni_GruppoWallet_Pannello);
        Opzioni_GruppoWallet_Pannello.setLayout(Opzioni_GruppoWallet_PannelloLayout);
        Opzioni_GruppoWallet_PannelloLayout.setHorizontalGroup(
            Opzioni_GruppoWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_GruppoWallet_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Opzioni_GruppoWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Opzioni_GruppoWallet_ScrollTabella, javax.swing.GroupLayout.DEFAULT_SIZE, 937, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addGroup(Opzioni_GruppoWallet_PannelloLayout.createSequentialGroup()
                        .addComponent(Opzioni_GruppoWallet_CheckBox_PlusXWallet)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(Opzioni_GruppoWallet_PannelloLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jScrollPane6))))
        );
        Opzioni_GruppoWallet_PannelloLayout.setVerticalGroup(
            Opzioni_GruppoWallet_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Opzioni_GruppoWallet_PannelloLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Opzioni_GruppoWallet_ScrollTabella, javax.swing.GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Opzioni_GruppoWallet_CheckBox_PlusXWallet)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7))
        );

        Opzioni_TabbedPane.addTab("Gruppi Wallet Crypto", Opzioni_GruppoWallet_Pannello);

        Opzioni_Emoney_Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Token", "Data Inizio classificazione e-money token"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Opzioni_Emoney_Tabella.setColumnSelectionAllowed(true);
        Opzioni_Emoney_Tabella.setRowHeight(30);
        Opzioni_Emoney_Tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Opzioni_Emoney_TabellaMouseClicked(evt);
            }
        });
        Opzioni_Emoney_ScrollPane.setViewportView(Opzioni_Emoney_Tabella);
        Opzioni_Emoney_Tabella.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jTextPane1.setEditable(false);
        jTextPane1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTextPane1.setText("Nella Tabella sottostante aggiungere il nome di tutti i token che si vogliono classificare come E-Money Token (es. USDC).\nLo scambio tra Crypto ed E-Money Token verrà considerato fiscalmente rilevante e genererà quindi un eventuale plusvalenza o minusvalenza.\n\nPer casistiche particolari, ad esempio un  token che diventa E-Money solo da un certa data, è possibile indicare quest'ultima direttamente nella tabella.\nIn questi casi gli scambi tra Crypto e queste E-Money saranno fiscalmente rilevanti solo alla data indicata in poi.");
        jScrollPane5.setViewportView(jTextPane1);

        Opzioni_Emoney_Bottone_Rimuovi.setText("Rimuovi Token");
        Opzioni_Emoney_Bottone_Rimuovi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Emoney_Bottone_RimuoviActionPerformed(evt);
            }
        });

        Opzioni_Emoney_Bottone_Aggiungi.setText("Aggiungi Token");
        Opzioni_Emoney_Bottone_Aggiungi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Emoney_Bottone_AggiungiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout Opzioni_Emoney_PannelloLayout = new javax.swing.GroupLayout(Opzioni_Emoney_Pannello);
        Opzioni_Emoney_Pannello.setLayout(Opzioni_Emoney_PannelloLayout);
        Opzioni_Emoney_PannelloLayout.setHorizontalGroup(
            Opzioni_Emoney_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_Emoney_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Opzioni_Emoney_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 937, Short.MAX_VALUE)
                    .addComponent(Opzioni_Emoney_ScrollPane)
                    .addGroup(Opzioni_Emoney_PannelloLayout.createSequentialGroup()
                        .addComponent(Opzioni_Emoney_Bottone_Aggiungi, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Opzioni_Emoney_Bottone_Rimuovi, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        Opzioni_Emoney_PannelloLayout.setVerticalGroup(
            Opzioni_Emoney_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Opzioni_Emoney_PannelloLayout.createSequentialGroup()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Opzioni_Emoney_ScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Opzioni_Emoney_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Opzioni_Emoney_Bottone_Aggiungi)
                    .addComponent(Opzioni_Emoney_Bottone_Rimuovi))
                .addContainerGap())
        );

        Opzioni_TabbedPane.addTab("E-Money Token (EMT)", Opzioni_Emoney_Pannello);

        Opzioni_Export_Wallets_Combobox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel9.setText("Scegli il wallet da esportare : ");

        Opzioni_Export_Tatax_Bottone.setText("Genera File per Tatax");
        Opzioni_Export_Tatax_Bottone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Opzioni_Export_Tatax_BottoneActionPerformed(evt);
            }
        });

        Opzioni_Export_EsportaPrezzi_CheckBox.setText("Esporta Prezzi se presenti");

        javax.swing.GroupLayout Opzioni_Export_PannelloLayout = new javax.swing.GroupLayout(Opzioni_Export_Pannello);
        Opzioni_Export_Pannello.setLayout(Opzioni_Export_PannelloLayout);
        Opzioni_Export_PannelloLayout.setHorizontalGroup(
            Opzioni_Export_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_Export_PannelloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Opzioni_Export_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Opzioni_Export_PannelloLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Opzioni_Export_Wallets_Combobox, javax.swing.GroupLayout.PREFERRED_SIZE, 419, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Opzioni_Export_Tatax_Bottone))
                    .addComponent(Opzioni_Export_EsportaPrezzi_CheckBox))
                .addContainerGap(437, Short.MAX_VALUE))
        );
        Opzioni_Export_PannelloLayout.setVerticalGroup(
            Opzioni_Export_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Opzioni_Export_PannelloLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(Opzioni_Export_PannelloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(Opzioni_Export_Wallets_Combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Opzioni_Export_Tatax_Bottone))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Opzioni_Export_EsportaPrezzi_CheckBox)
                .addContainerGap(572, Short.MAX_VALUE))
        );

        Opzioni_TabbedPane.addTab("Export", Opzioni_Export_Pannello);

        javax.swing.GroupLayout OpzioniLayout = new javax.swing.GroupLayout(Opzioni);
        Opzioni.setLayout(OpzioniLayout);
        OpzioniLayout.setHorizontalGroup(
            OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Opzioni_TabbedPane)
        );
        OpzioniLayout.setVerticalGroup(
            OpzioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(OpzioniLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Opzioni_TabbedPane)
                .addContainerGap())
        );

        CDC.addTab("Opzioni", Opzioni);

        jLabel1.setText("Seleziona data inizio e fine per i calcoli ->");

        jLabel2.setText("Data Inizio :");

        CDC_DataChooser_Iniziale.setDateFormatString("yyyy-MM-dd");
        CDC_DataChooser_Iniziale.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                CDC_DataChooser_InizialeFocusLost(evt);
            }
        });
        CDC_DataChooser_Iniziale.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                CDC_DataChooser_InizialePropertyChange(evt);
            }
        });
        CDC_DataChooser_Iniziale.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                CDC_DataChooser_InizialeKeyPressed(evt);
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
                .addComponent(CDC))
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
            this.GiacenzeaData_Data_DataChooser.setDate(d);
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
          //  System.out.println (apiUrl);
           // System.out.println (response.toString());
           // System.out.println (data[4]); //questo è il valore sulla coppia con usdt
            
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
        try ( FileReader fire = new FileReader(fiatwallet);  BufferedReader bure = new BufferedReader(fire);) {
            while ((riga = bure.readLine()) != null) {
                String splittata[] = riga.split(",");
                if (splittata.length == 10)// se non è esattamente uguale a 10 significa che il file non è corretto
                {
                    //le transazioni qua sotto non devo considerarle
                    if (!splittata[9].equals("trading.limit_order.fiat_wallet.purchase_unlock")
                            && !splittata[9].equals("trading.limit_order.fiat_wallet.purchase_lock")) {
                        if (Funzioni_Date_ConvertiDatainLong(splittata[0]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                        {
                            //CDC_FiatWallet_Mappa.put(splittata[0], riga);
                            String idRiga;
                            int Colonna = CDC_Funzione_trovaColonnaEuro(riga);
                            if (Colonna == 999) {
                                idRiga = splittata[0] + splittata[1] + splittata[9];
                            } else {
                                idRiga = splittata[0] + splittata[1] + splittata[9] + splittata[Colonna];
                            }
                            String rigasistemata = "";
                            splittata[3] = splittata[3].replace("-", ""); //questo toglie i valori negativi dalla colonna 3 che deve essere sempre positiva
                            for (String composta : splittata) {
                                rigasistemata = rigasistemata + composta + ",";
                            }
                            //System.out.println(rigasistemata);
                            CDC_FiatWallet_Mappa.put(idRiga, rigasistemata);
                        }
                    }
                }
            }
            //   bure.close();
            //  fire.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }

        CDC_FiatWallet_MappaTipiMovimenti.clear();
        //Ora importo i tipi movimento del FiatWallet
        CDC_FiatWallet_MappaTipiMovimenti.put("crypto_viban", "crypto_viban;+;default;Vendita Crypto");
        CDC_FiatWallet_MappaTipiMovimenti.put("viban_card_top_up", "viban_card_top_up;-;default;TopUp Carta");
        CDC_FiatWallet_MappaTipiMovimenti.put("viban_deposit", "viban_deposit;+;default;Bonifico in Ingresso");
        CDC_FiatWallet_MappaTipiMovimenti.put("viban_purchase", "viban_purchase;-;default;Acquisto Crypto");
        CDC_FiatWallet_MappaTipiMovimenti.put("viban_withdrawal", "viban_withdrawal;+;default;Bonifico su Conto Corrente");
        CDC_FiatWallet_MappaTipiMovimenti.put("trading.limit_order.fiat_wallet.purchase_commit", "trading.limit_order.fiat_wallet.purchase_commit;-;default;Acquisto Crypto");
        CDC_FiatWallet_MappaTipiMovimenti.put("trading.limit_order.fiat_wallet.sell_commit", "trading.limit_order.fiat_wallet.sell_commit;+;default;Vendita Crypto");

        try {
            File movPers = new File(CDC_FiatWallet_FileTipiMovimentiDBPers);
            if (!movPers.exists()) {
                movPers.createNewFile();
            }
            FileReader fires = new FileReader(CDC_FiatWallet_FileTipiMovimentiDBPers);
            BufferedReader bures = new BufferedReader(fires);

            while ((riga = bures.readLine()) != null) {
                String splittata[] = riga.split(";");
                if (splittata.length == 4)// se non è esattamente uguale a 4 significa che il file non è corretto
                {
                    CDC_FiatWallet_MappaTipiMovimenti.put(splittata[0], riga);
                }
            }
            bures.close();
            fires.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }

        CDC_FiatWallet_ListaSaldi = CDC_FiatWallet_Funzione_CalcolaListaSaldi();

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
    //                   //     System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
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
    //                  //      System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
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
                                
    //                  //      System.out.println("----------------------------"+UltimaData+","+totale+"---------------------------");
    
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
        //String SaldoIniziale=this.CDC_FiatWallet_Text_GiacenzaIniziale.getText().replace("€", "").trim();
       // BigDecimal Totale=new BigDecimal(SaldoIniziale);
        BigDecimal Totale=new BigDecimal (CDC_FiatWallet_Text_SaldoIniziale.getText().substring(2));
       // Totale=Totale.add(SaldoInizioPeriodo);
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
                        //System.out.println("-"+splittata[Colonna]+"-");
                        //System.out.println(tempo.split(";")[1]+splittata[Colonna]);
                        CDC_FiatWallet_RigaTabella2[3]=Double.valueOf(splittata[Colonna]);
                        String numString=(tempo.split(";")[1]+splittata[Colonna]);
                        BigDecimal Adde2 = new BigDecimal(numString);
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
      //  String SaldoIniziale=this.CDC_CardWallet_Text_GiacenzaIniziale.getText().replace("€", "").trim();
       // BigDecimal Totale=new BigDecimal(SaldoIniziale);
        BigDecimal Totale=new BigDecimal (CDC_CardWallet_Text_SaldoIniziale.getText().substring(2));
       // Totale=Totale.add(SaldoInizioPeriodo);
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
       //System.out.println(CDC_DataChooser_Iniziale.getDate());
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

        if (!CDC_FiatWallet_SaldoIniziale.equalsIgnoreCase(this.CDC_FiatWallet_Text_GiacenzaIniziale.getText())&&Funzioni_isNumeric(this.CDC_FiatWallet_Text_GiacenzaIniziale.getText(),true)){
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
        if (!CDC_CardWallet_SaldoIniziale.equalsIgnoreCase(this.CDC_CardWallet_Text_GiacenzaIniziale.getText())&&Funzioni_isNumeric(this.CDC_CardWallet_Text_GiacenzaIniziale.getText(),true)){
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

    private void GiacenzeaData_CompilaTabellaMovimenti() {
        
        //Gestisco i bottoni
        GiacenzeaData_Bottone_RettificaQta.setEnabled(false);
       

        //PULIZIA TABELLA
        DefaultTableModel GiacenzeaData_ModelloTabella = (DefaultTableModel) this.GiacenzeaData_TabellaDettaglioMovimenti.getModel();
        Funzioni_Tabelle_PulisciTabella(GiacenzeaData_ModelloTabella);
        
        //ANALISI E PROPOSTA
        if (GiacenzeaData_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
            String mon = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            //Cambio il nome sul Bottone SCAM a seconda se il token è scam o meno
            if (Funzioni.isSCAM(mon))
                GiacenzeaData_Bottone_Scam.setText("Rimuovi da SCAM");
            else
                GiacenzeaData_Bottone_Scam.setText("Identifica come SCAM");
            String Address = "";
            if (GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2) != null) {
                Address = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2).toString();
                
            }
            //gestione bottone scam token da abilitare solo in presenza di un token in defi
            if (Address.contains("0x"))
                {
                GiacenzeaData_Bottone_Scam.setEnabled(true);
                GiacenzeaData_Bottone_CambiaNomeToken.setEnabled(true);
                }
            else
            {
                GiacenzeaData_Bottone_Scam.setEnabled(false);
                GiacenzeaData_Bottone_CambiaNomeToken.setEnabled(false);
                }
            
                    //ABILITO BOTTONE DEFI SE CI SONO LE CONDIZIONI
        String Wallet=Giacenzeadata_Walleta_Label.getText().trim(); 
        String SottoWallet=Giacenzeadata_Walletb_Label.getText().trim();
        if (Wallet.contains("0x")&&Wallet.contains("(")&&Wallet.contains(")")&&!Address.equalsIgnoreCase("")) {
                this.GiacenzeaData_Bottone_MovimentiDefi.setEnabled(true);
            }
        else{
            this.GiacenzeaData_Bottone_MovimentiDefi.setEnabled(false);
        }

            long DataRiferimento = 0;
            if (GiacenzeaData_Data_DataChooser.getDate() != null) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                String Data = f.format(GiacenzeaData_Data_DataChooser.getDate());
                DataRiferimento = OperazioniSuDate.ConvertiDatainLong(Data) + 86400000;
                long DatadiOggi = System.currentTimeMillis();
                if (DatadiOggi < DataRiferimento) {
                    DataRiferimento = DatadiOggi;
                }
            }
            //Adesso compilo i movimenti
            BigDecimal TotaleQta = new BigDecimal(0);
            for (String[] movimento : MappaCryptoWallet.values()) {
                long DataMovimento = OperazioniSuDate.ConvertiDatainLong(movimento[1]);
                if (DataMovimento < DataRiferimento) {
                        String gruppoWallet="";
                        if (Wallet.contains("Gruppo :"))gruppoWallet=Wallet.split(" : ")[1].trim();
                        String AddressU = movimento[26];
                        String AddressE = movimento[28];
                    // adesso verifico il wallet
                    if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                || (Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase("tutti"))//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||(Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase(movimento[4].trim()))//Se wallet e sottowasllet corrispondono a quelli analizzati proseguo
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3]).equals(gruppoWallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                                ) { 
                        if (movimento[8].equals(mon) && AddressU.equalsIgnoreCase(Address)) {
                            TotaleQta = TotaleQta.add(new BigDecimal(movimento[10])).stripTrailingZeros();
                            String riga[] = new String[9];
                            riga[0] = movimento[1];
                            riga[1] = movimento[3];
                            riga[2] = movimento[8];
                            riga[3] = AddressU;
                            riga[4] = movimento[5];
                            riga[5] = movimento[10];
                            riga[6] = movimento[15];
                            riga[7] = TotaleQta.toPlainString();
                            riga[8] = movimento[0];
                            GiacenzeaData_ModelloTabella.addRow(riga);
                        }
                        if (movimento[11].equals(mon) && AddressE.equalsIgnoreCase(Address)) {
                            TotaleQta = TotaleQta.add(new BigDecimal(movimento[13])).stripTrailingZeros();
                            String riga[] = new String[9];
                            riga[0] = movimento[1];
                            riga[1] = movimento[3];
                            riga[2] = movimento[11];
                            riga[3] = AddressE;
                            riga[4] = movimento[5];
                            riga[5] = movimento[13];
                            riga[6] = movimento[15];
                            riga[7] = TotaleQta.toPlainString();
                            riga[8] = movimento[0];
                            GiacenzeaData_ModelloTabella.addRow(riga);
                        }
                    }
                }
            }
            //coloro la tabella
            Tabelle.ColoraRigheTabella1GiacenzeaData(GiacenzeaData_TabellaDettaglioMovimenti);
        }
    }
    
    

    
    private void Opzioni_Emoney_CaricaTabellaEmoney(){
        DefaultTableModel Emoney_ModelloTabella = (DefaultTableModel) this.Opzioni_Emoney_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(Emoney_ModelloTabella);
        TableColumn column1 = Opzioni_Emoney_Tabella.getColumnModel().getColumn(1);
        JDateChooser DataChooser = new com.toedter.calendar.JDateChooser();
        
        //DataChooser.get
        column1.setCellRenderer(new JDateChooserRenderer());
        column1.setCellEditor(new JDateChooserCellEditor());
        //testColumn.setCellEditor(new JDateChooser());
        for (String a: Mappa_EMoney.keySet()){
            try {
                Object rigaTabella[]=new Object[2];
                rigaTabella[0]=a;
                String Data=Mappa_EMoney.get(a);
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                Date d = f.parse(Data);
                rigaTabella[1]=d;
                Emoney_ModelloTabella.addRow(rigaTabella);
                // System.out.println(a);
            } catch (ParseException ex) {
                Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void Opzioni_GruppoWallet_CaricaGruppiWallet(){
        DefaultTableModel GruppoWallet_ModelloTabella = (DefaultTableModel) this.Opzioni_GruppoWallet_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(GruppoWallet_ModelloTabella);
   JComboBox<String> comboBox = new JComboBox<>();
    comboBox.addItem("Wallet 01");
    comboBox.addItem("Wallet 02");
    comboBox.addItem("Wallet 03");
    comboBox.addItem("Wallet 04");
    comboBox.addItem("Wallet 05");
    comboBox.addItem("Wallet 06");
    comboBox.addItem("Wallet 07");
    comboBox.addItem("Wallet 08");
    comboBox.addItem("Wallet 09");
    comboBox.addItem("Wallet 10");
    comboBox.addItem("Wallet 11");
    comboBox.addItem("Wallet 12");
    comboBox.addItem("Wallet 13");
    comboBox.addItem("Wallet 14");
    comboBox.addItem("Wallet 15");
    comboBox.addItem("Wallet 16");
    comboBox.addItem("Wallet 17");
    comboBox.addItem("Wallet 18");   
    comboBox.addItem("Wallet 19");
    comboBox.addItem("Wallet 20");
    
TableColumn testColumn = Opzioni_GruppoWallet_Tabella.getColumnModel().getColumn(1);
testColumn.setCellEditor(new DefaultCellEditor(comboBox));
        
        for (String a: Mappa_Wallet.keySet()){
            Object rigaTabella[]=new Object[2];
         //   TableCellEditor comboBoxEditor = new DefaultCellEditor(gruppo);
          //  Opzioni_GruppoWallet_Tabella.getColumnModel().getColumn(1).setCellEditor(comboBoxEditor);
            rigaTabella[0]=a;
            rigaTabella[1]=DatabaseH2.Pers_GruppoWallet_Leggi(a);
            GruppoWallet_ModelloTabella.addRow(rigaTabella);
            
          //  System.out.println("aaaa");
        }
    }
    
    public static void Funzione_AggiornaMappaWallets(String[] v){
                  Mappa_Wallet.put(v[3], v[1]);
                  
                  if(Mappa_Wallets_e_Dettagli.get(v[3])==null)
                  {
                      List<String> Lista=new ArrayList();
                      Lista.add(v[4]);
                      Mappa_Wallets_e_Dettagli.put(v[3], Lista);
                  }
              else{
                  List<String> Lista;
                  Lista=Mappa_Wallets_e_Dettagli.get(v[3]);
                  if (!Lista.contains(v[4]))Lista.add(v[4]);
              }
    }
    
    public void Opzioni_RicreaListaWalletDisponibili(){ 
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.removeAllItems();
        Opzioni_Combobox_CancellaTransazioniCryptoXwallet.addItem("----------");
          Mappa_Wallet.clear();
          Mappa_Wallets_e_Dettagli.clear();
          for (String[] v : MappaCryptoWallet.values()) {
                Funzione_AggiornaMappaWallets(v);
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

        if (mod.getModificaEffettuata()){
            Funzioni_AggiornaTutto();
            DepositiPrelievi_Caricatabella();
        }
             
      //  DepositiPrelievi_Caricatabella();
        
        
        
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

    private void Funzioni_AggiornaTutto() {
        TransazioniCrypto_DaSalvare = true;
        TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
        Calcoli_Plusvalenze.AggiornaPlusvalenze();
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        RW_RicalcolaRWseEsiste();
        //DepositiPrelievi_Caricatabella();
    }
    
    private void DepositiPrelievi_Caricatabella()
            {
                
                if (!tabDepositiPrelieviCaricataprimavolta){
                    tabDepositiPrelieviCaricataprimavolta=true;
                    //Questo serve per sistemare il pregresso prima della versione 1.15
                    Importazioni.ConvertiScambiLPinDepositiPrelievi();
                }
        DepositiPrelieviDaCategorizzare=new ArrayList<>();
        DefaultTableModel ModelloTabellaDepositiPrelievi = (DefaultTableModel) this.DepositiPrelievi_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaDepositiPrelievi);
        Tabelle.ColoraRigheTabellaCrypto(DepositiPrelievi_Tabella);
        for (String[] v : MappaCryptoWallet.values()) {
          String TipoMovimento=v[0].split("_")[4].trim();
          //AU sono equiparati a dei trasferimenti interni, da verifixcare accuratamente perchè così rischio di fare casino nelle esportazioni
          if ((TipoMovimento.equalsIgnoreCase("DC")||TipoMovimento.equalsIgnoreCase("PC"))&&v[22]!=null&&!v[22].equalsIgnoreCase("AU"))
          {
            //Adesso controllo se il token è scam e in quel caso non lo faccio vedere che creo solo confusione
              if (TipoMovimento.equalsIgnoreCase("DC")&&!Funzioni.isSCAM(v[11])
                      ||
                  TipoMovimento.equalsIgnoreCase("PC"))
              {
            //if (this.DepositiPrelievi_CheckBox_movimentiClassificati.isSelected())
            if (v[18].trim().equalsIgnoreCase("")||this.DepositiPrelievi_CheckBox_movimentiClassificati.isSelected())
              {
            String riga[]=new String[10];
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
            riga[8]=v[7];
            riga[9]=v[30];
            Funzioni.RiempiVuotiArray(riga);             
            ModelloTabellaDepositiPrelievi.addRow(riga);
            //Se il movimento non è ancora categorizzato lo metto nella lista dei movimenti ancora non categorizzati
            if (v[18].trim().equalsIgnoreCase(""))DepositiPrelieviDaCategorizzare.add(v[0]);
           // System.out.println("a");
            }
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
        DepositiPrelievi_Caricatabella();
      //System.out.println("mi vedi");
    }//GEN-LAST:event_DepositiPrelieviComponentShown

    private void DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed
        DepositiPrelievi_AssegnazioneAutomatica();
        RW_RicalcolaRWseEsiste();
    }//GEN-LAST:event_DepositiPrelievi_Bottone_AssegnazioneAutomaticaActionPerformed

    

    
    private void DepositiPrelievi_AssegnazioneAutomatica(){
                // TODO add your handling code here:
        //qua devo fare le verifiche sui numeri e assegnare le unioni correttamente
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       // Importazioni.ConvertiScambiLPinDepositiPrelievi();
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
            //String wallet=ModelloTabella1DepositiPrelievi.getValueAt(i, 2).toString();
            String wallet=MappaCryptoWallet.get(id)[3]+MappaCryptoWallet.get(id)[4];
            //come prima cosa verifico che il movimento non sia già abbinato/assegnato
        if (MappaCryptoWallet.get(id)!=null && MappaCryptoWallet.get(id)[18].equalsIgnoreCase(""))
            for (int k=i+1;k<numeroRighe;k++){ 
                String id2=ModelloTabella1DepositiPrelievi.getValueAt(k, 0).toString();
                String data2=ModelloTabella1DepositiPrelievi.getValueAt(k, 1).toString();
                String moneta2=ModelloTabella1DepositiPrelievi.getValueAt(k, 4).toString();
                String qta2=ModelloTabella1DepositiPrelievi.getValueAt(k, 5).toString();
                //String wallet2=ModelloTabella1DepositiPrelievi.getValueAt(k, 2).toString();
                String wallet2=MappaCryptoWallet.get(id2)[3]+MappaCryptoWallet.get(id2)[4];
                //le condizioni affinchè avvenga l'abbinamento automatico devono essere               
                //1- il movimento non deve risultarte già abbinato
                //2- differenza tra le date minore di 1 ora
                //3- stessa moneta
                //4- exchange diverso
                //5- importo uguale o comunque non deve differire di più del 2% ma uno deve essere un deposito e l'altro un prelievo
                //6- un movimento deve essere in negativo e l'altro in positivo                
                //7 - La qta uscita deve essere sempre maggiore o uguale di quella ricevuta
                BigDecimal Sommaqta2=new BigDecimal(qta).add(new BigDecimal (qta2)).stripTrailingZeros();
                //Se sommaQta è maggiore di zero significa che sono entrati più soldi di quelli usciti e questo è impossibile
                //per cui non posso eseguire il movimento
                //vado avanti solo se sommaqta è minore o uguale a zero
                BigDecimal Sommaqta=Sommaqta2.abs();
                BigDecimal PercentualeDifferenza=new BigDecimal(100);                
                if (Double.parseDouble(qta)!=0){
                    PercentualeDifferenza=Sommaqta.divide(new BigDecimal(qta),4,RoundingMode.HALF_UP).multiply(new BigDecimal(100)).abs(); 
                    }
                if (MappaCryptoWallet.get(id2)[18].equalsIgnoreCase("")&&//1
                        Funzioni_Date_DifferenzaDateSecondi(data2,data)<3600 &&//2
                        moneta.equals(moneta2)&&//3
                        !wallet.equalsIgnoreCase(wallet2)&&//4
                        PercentualeDifferenza.compareTo(new BigDecimal(2))==-1 &&//5
                        Sommaqta2.compareTo(new BigDecimal(0))<=0//7
                        )     //6  
                
                {
                    String IDDeposito=null;
                    String IDPrelievo=null;
                    if(id.split("_")[4].equalsIgnoreCase("DC")){
                        IDDeposito=id;
                    }
                    else if(id.split("_")[4].equalsIgnoreCase("PC")){
                        IDPrelievo=id;
                    }
                    if(id2.split("_")[4].equalsIgnoreCase("DC")){
                        IDDeposito=id2;
                    }
                    else if(id2.split("_")[4].equalsIgnoreCase("PC")){
                        IDPrelievo=id2;
                    }
                    if (IDPrelievo!=null && IDDeposito!=null)
                        {
                    ClassificazioneTrasf_Modifica.CreaMovimentiTrasferimentosuWalletProprio(IDPrelievo,IDDeposito);
                    numeromodifiche++;
                    break;
                    }
                }
            }
        }

        
        //FASE 2 : Adesso gestisco tutta la parte delle reward da Defi
        for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
            String Movimento[]=MappaCryptoWallet.get(IDnc);
            String Rete=Funzioni.TrovaReteDaID(IDnc);
            if(Movimento[18].equalsIgnoreCase("")&&
                  Rete!=null&&
                  Movimento[0].split("_")[4].equalsIgnoreCase("DC")&&
                  Movimento[7].trim().equalsIgnoreCase("getReward")){
                
                    Movimento[5] = "REWARD";
                    Movimento[18] = "DAI - Airdrop,Cashback,Rewards etc.. (plusvalenza)";
                    numeromodifiche++;
            }
        }
        
        //FASE 3 : Cerco di Classificare i movimenti che entrano ed escono dalle piattaforme DEFI e le categorizzo
        
        //Se è un prelievo di un token LP lo classifico come mandato in un vault e poi di conseguenza vado a classificare anche tutti i rientri, calcolo le reward etc...
        for (String IDnc:CDC_Grafica.DepositiPrelieviDaCategorizzare){
            //ad uno ad uno controllo tutti i movimenti non ancora categorizzati
            String Movimento[]=MappaCryptoWallet.get(IDnc);
            String Rete=Funzioni.TrovaReteDaID(IDnc);
            if (Rete!=null&&Movimento[18].equalsIgnoreCase("")&&//Verifico che il movimento sia in defi e non sia già classificato
                    Movimento[0].split("_")[4].equals("PC")&&//che sia un movimento di prelievo
                    Movimento[8].contains("-LP"))//che sia di un Token LP
            {
                //la funzione si occupa di trovare tutti i movimenti analoghi e classificarli
                //nonchè classifica tutti i movimenti di rientro
                numeromodifiche=numeromodifiche+ClassificazioneTrasf_Modifica.CreaMovimentiTrasferimentoAVaultNonPresidiati(IDnc);
            }
                
            
        }
        
        //FASE 4 : Cerco di classificare i movimenti delle piattoferme defi che riconosco come tali dai contratti (Completamente da vedere come fare la gestione)
        
        //FASE 5 : Categorizzo gli scambi differiti su stesso wallet in automatico
        //Condizioni:
            //-Movimento eseguito nello stesso secondo
            //-almeno uno dei due movimenti contine swap nella sua causale
            //-movimento non ancora categorizzato
        for (String IDnc : CDC_Grafica.DepositiPrelieviDaCategorizzare) {

            String Movimento[] = MappaCryptoWallet.get(IDnc);
            String Rete=Funzioni.TrovaReteDaID(IDnc);
            //Se non è un movimento in defi non gestisco nulla perchè non ho le basi per farlo e devo gestirlo a mano
            if (Rete!=null && Movimento[18].equalsIgnoreCase("")) {

                String DataConfronto1 = IDnc.split("_")[0];
                for (String IDnc2 : CDC_Grafica.DepositiPrelieviDaCategorizzare) {

                    String Movimento2[] = MappaCryptoWallet.get(IDnc2);
                    String Rete2=Funzioni.TrovaReteDaID(IDnc2);
                    String DataConfronto2 = IDnc2.split("_")[0];
                    if (DataConfronto1.equals(DataConfronto2)&&
                            Rete2!=null&& Movimento2[18].equalsIgnoreCase("")&&
                            (Movimento2[7].contains("swapExactTokens")||Movimento[7].contains("swapExactTokens"))&&
                            IDnc.split("_")[4].equals("PC")&&
                            IDnc2.split("_")[4].equals("DC")) {
                        //Se arrivo qua ho i due dovimenti di cui devo creare lo scambio
                        ClassificazioneTrasf_Modifica.CreaMovimentiScambioCryptoDifferito(IDnc,IDnc2);
                        //mando avanti di 2 le modifiche perchè ne ho classificati 2
                        numeromodifiche++;
                        numeromodifiche++;
                        
                    }
                }
            }
        }
        
        //FASE 6: Trasforma i movimenti di deposito di CRO che arrivano da un certo indirizzo in scambio tra WCRO e CRO
        
            for (String IDnc : CDC_Grafica.DepositiPrelieviDaCategorizzare) {

            String Movimento[] = MappaCryptoWallet.get(IDnc);
            
            //Se non è un movimento in defi non gestisco nulla perchè non ho le bsi per farlo e devo gestirlo a mano
            if (Movimento[18].equalsIgnoreCase("")
                    &&//movimento non classificato e in defi
                    IDnc.split("_")[4].equals("DC") && Movimento[11].equals("CRO")
                    &&//movimento di deposito di CRO
                    Movimento[7].trim().equalsIgnoreCase("withdraw")
                    &&//Classificato come withdraw
                    Movimento[30].equalsIgnoreCase("0x5c7f8a570d578ed84e63fdfa7b1ee72deae1ae23")
                    &&//Arriva da contratto WCRO
                    Funzioni.TrovaReteDaID(IDnc).equalsIgnoreCase("CRO")) //Rete Cronos
            {
                //Creo un movimento di uscita di WCRO che poi verrà trasformato in scambio differito dal sistema
                String MT[] = new String[Importazioni.ColonneTabella];
                String IDSpezzato[]=IDnc.split("_");
                String IDNuovoMov = IDSpezzato[0] + "_" + IDSpezzato[1] + "_0."+IDSpezzato[2]+"_" + IDSpezzato[3] + "_PC";
                MT[0] = IDNuovoMov;
                MT[1] = Movimento[1];
                MT[2] = "1 di 1";
                MT[3] = Movimento[3];
                MT[4] = Movimento[4];
                MT[5] = "PRELIEVO CRYPTO";
                MT[6] = "WCRO ->";
                MT[8] = "WCRO";
                MT[9] = Movimento[12];
                MT[10] = new BigDecimal(Movimento[13]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
                MT[15] = Movimento[15];
                MT[22] = "A";
                MT[23] = Movimento[23];
                MT[24] = Movimento[24];
                MT[25] = "WCRO";
                MT[26] = "0x5c7f8a570d578ed84e63fdfa7b1ee72deae1ae23";
                MT[29] = Movimento[29];
                Importazioni.RiempiVuotiArray(MT);
                MappaCryptoWallet.put(IDNuovoMov, MT);
                ClassificazioneTrasf_Modifica.CreaMovimentiScambioCryptoDifferito(IDNuovoMov,IDnc);
               // System.out.println("Trovato scambio con WCRO");
                numeromodifiche++;
            }
        }

          //FASE 7 : Classificare o eliminare i movimenti che passano da CRO a CRO su diversa chain  
            //NON DEVO FAR NULLA MA SEMPLICEMENTE GESTIRE LA CRONOS POS CHAIN CON IL WALLET CORRETTO NELLE IMPORTAZIONI
        
        
       // this.CDC.setSelectedIndex(0);
        if (numeromodifiche>0){
        JOptionPane.showConfirmDialog(this, "Sono stati individuati e aggiornati "+numeromodifiche+" coppie di transazioni, ricordarsi di salvare le modifiche!!",
        "Resoconto",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
       
                    TransazioniCrypto_DaSalvare=true;
            TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
        }
        else{
        JOptionPane.showConfirmDialog(this, "Non sono state trovare nuove coppie di transazioni da abbinare automaticamente",
        "Resoconto",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);   
        }
        
        Calcoli_Plusvalenze.AggiornaPlusvalenze();
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        DepositiPrelievi_Caricatabella();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    private void SituazioneImportComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_SituazioneImportComponentShown
        // TODO add your handling code here:
        SituazioneImport_Caricatabella1();
    }//GEN-LAST:event_SituazioneImportComponentShown

    private void DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased
        // TODO add your handling code here:
       // System.out.println("cambio");

       DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_DepositiPrelievi_CheckBox_movimentiClassificatiMouseReleased

    

    
    
    

    
    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        // TODO add your handling code here:
        System.out.println("Focus");      
        TransazioniCrypto_Funzione_VerificaeAggiornaTabellaCrypto();
    }//GEN-LAST:event_formWindowGainedFocus

    private void TransazioniCrypto_Funzione_VerificaeAggiornaTabellaCrypto(){
                if (TabellaCryptodaAggiornare) {
          //  System.out.println("AggiornoTabella");
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            TabellaCryptodaAggiornare = false;
            TransazioniCrypto_DaSalvare = true;
            Calcoli_Plusvalenze.AggiornaPlusvalenze();
            TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
            RW_RicalcolaRWseEsiste();
            //GiacenzeaData_AggiornaComboBoxWallet();
           /* TransazioniCrypto_Bottone_Annulla.setEnabled(true);
            TransazioniCrypto_Bottone_Salva.setEnabled(true);
            TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(true);*/
            this.setCursor(Cursor.getDefaultCursor());
        }
    }
    
    private void Analisi_CryptoComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_Analisi_CryptoComponentShown
        // TODO add your handling code here:
        //System.out.println("CaricaTabella");
        DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_Analisi_CryptoComponentShown

    private void GiacenzeaDataComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_GiacenzeaDataComponentShown
        // TODO add your handling code here:
      //  GiacenzeaData_Funzione_AggiornaComboBoxWallet();
    }//GEN-LAST:event_GiacenzeaDataComponentShown

    private void GiacenzeaData_Bottone_ModificaValoreMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_ModificaValoreMouseClicked
        // TODO add your handling code here:

    }//GEN-LAST:event_GiacenzeaData_Bottone_ModificaValoreMouseClicked

    private void GiacenzeaData_Funzione_ModificaValore() {  
        
        if (GiacenzeaData_Tabella.getSelectedRow() >= 0) {
            long DataRiferimento = 0;
            if (GiacenzeaData_Data_DataChooser.getDate() != null) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                String Data = f.format(GiacenzeaData_Data_DataChooser.getDate());
                DataRiferimento = OperazioniSuDate.ConvertiDatainLong(Data) + 86400000;
                long DatadiOggi = System.currentTimeMillis();
                if (DatadiOggi < DataRiferimento) {
                    DataRiferimento = DatadiOggi;
                }
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                String DataconOra = sdf.format(DataRiferimento);

                int rigaselezionata = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
                String mon = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
                String Rete = null;
                if (GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1) != null) {
                    Rete = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1).toString();
                }
                String Address = null;
                if (GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2) != null) {
                    Address = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2).toString().toUpperCase();
                }
                BigDecimal Qta = new BigDecimal(GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 4).toString());
                String Prezzo = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 5).toString();
                String m = JOptionPane.showInputDialog(this, "Indica il valore in Euro per " + Qta + " " + mon + " : ", Prezzo);
                if (m != null) {
                    m = m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (CDC_Grafica.Funzioni_isNumeric(m, false)) {
                        //Se è un numero inserisco il prezzo e lo salvo a sistema
                        BigDecimal PrezzoUnitario = new BigDecimal(m).divide(Qta, 30, RoundingMode.HALF_UP).stripTrailingZeros();
                        if (Address != null && Rete != null) {
                            DatabaseH2.PrezzoAddressChain_Scrivi(DataconOra + "_" + Address + "_" + Rete, PrezzoUnitario.toPlainString());
                           // System.out.println(DataconOra + "_" + Address + "_" + Rete +" - "+ PrezzoUnitario.toPlainString());
                        } else {
                            DatabaseH2.XXXEUR_Scrivi(DataconOra + " " + mon, PrezzoUnitario.toPlainString());
                        }
                    } else {
                        JOptionPane.showConfirmDialog(this, "Attenzione, " + m + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                }
                //Una volta cambiato il prezzo aggiorno la tabella
                GiacenzeaData_CompilaTabellaToken();
            }
        }
    }
    
        private void RW_Funzione_ModificaValore(int InizioFine) {
            //InizioFine=0 -> Prezzo Iniziale
            //InizioFine=1 -> Prezzo Finale
            //se ID è nullo significa che è un prezzo di inizio io fine anno e mi comporto di conseguenza
            //Altrimenti cambio il prezzo sulla transazione
            String ID;
        if (RW_Tabella_Dettagli.getSelectedRow() >= 0) {
            int rigaselezionata = RW_Tabella_Dettagli.getSelectedRow();
            //String GruppoWalletInizio=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 1).toString();
            //String GruppoWalletFine=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 6).toString();
            String GruppoWallet;
            String DataPrezzo;
            String Prezzo;
            String mon;
            BigDecimal Qta;
            if (InizioFine==0){
                    //Prezzo Iniziale
                    GruppoWallet=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 1).toString();
                    mon = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 2).toString();
                    Qta = new BigDecimal(RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 3).toString());
                    DataPrezzo = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 4).toString();
                    Prezzo = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 5).toString();
                    ID= RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 13).toString();
            }else{
                    //Prezzo Finale
                    GruppoWallet=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 6).toString();
                    mon = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 7).toString();
                    Qta = new BigDecimal(RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 8).toString());
                    DataPrezzo = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 9).toString();
                    Prezzo = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 10).toString();
                    ID= RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 14).toString();
            }
           // System.out.println("RW_Funzione_ModificaValore  - ID : "+ID);
            int rigaTabellaPrincipale=RW_Tabella.getSelectedRow();
            if (MappaCryptoWallet.get(ID)==null){
                //Se entro qua dentro significa che il valore che voglio modificare è quello di inizio o fine anno
                //Adesso verifico se è una data iniziale o finale che voglio modificare
                //Se è fine anno devo invece aggiungere 60 secondi
                //System.out.println("modfffff");
                long DataCalcoli = 0;
                if (DataPrezzo.contains("00:00")) DataCalcoli=OperazioniSuDate.ConvertiDatainLongMinuto(DataPrezzo);
                if (DataPrezzo.contains("23:59")) DataCalcoli=OperazioniSuDate.ConvertiDatainLongMinuto(DataPrezzo)+60000;
                String DataconOra=OperazioniSuDate.ConvertiDatadaLongallOra(DataCalcoli);
            //long DataRiferimento = 0;
                
                
               // BigDecimal Qta = new BigDecimal(RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 3).toString());
                

                String Prezz = JOptionPane.showInputDialog(this, "Indica il valore in Euro per " + Qta + " " + mon + " in data "+DataPrezzo+" : ", Prezzo);
                if (Prezz != null) {
                    Prezz = Prezz.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (CDC_Grafica.Funzioni_isNumeric(Prezz, false)) {
                        //Adesso devo cercare tutte le movimentazioni di questa moneta e visto che non ho l'id della transazione
                        //recuperare tutti gli address, poi dovrò modificare il prezzo su tutti questi.(per la defi sono obbligato ad usare gli address)
                        //Scansiono la tabella della movimentazioni e salvo in una mappa Monete tutte le monete che trovo con Address_Nome come key
                        //ovvimente solo quelle facente parti del Gruppo wallet analizzato
                        //Devo scansionare tutto perchè potrei trovarmi con monete con lo stesso nome ma address diversi
                        //Siccome l'RW tiene conto solo del NomeToken, per essere sicuro che quel prezzo sia associato a quel nome token in tutte le sue derivazioni
                        //e poi per scrivere il prezzo ho bisogno dell'address
                        String Address;
                        String Rete;
                        //cd
                        Map<String, Moneta> MappaAddressNomeMoneta = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);        
                        for (String[] v : MappaCryptoWallet.values()) {
                            Moneta a[]=Funzioni.RitornaMoneteDaID(v[0]);
                            //Controllo se la data della transazione è inferiore o uguale a quella a cui devo arrivare
                            //E se il wallet fa parte del gruppo wallet di riferimento
                            String GruppoWalletMovimento = DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
                            long dataTransazione=OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
                            if (dataTransazione<=DataCalcoli&&GruppoWallet.equals(GruppoWalletMovimento)){
                            //A questo punto controllo se la moneta è quella che sto cercando
                            for (Moneta MonTransazione : a){
                                //controllo sia la moneta in uscita che quella in ingresso, nel caso trovi una corrispondenza inserisco la moneta nella mappa
                                
                                if (MonTransazione.Moneta.equals(mon)){
                                    MappaAddressNomeMoneta.put(MonTransazione.MonetaAddress+"_"+MonTransazione.Moneta, MonTransazione);
                                }
                            }
                            }else  {
                                //in caso devo mettere un break se la funzione risulta troppo lenta
                            }
                        }
                        
                        for (Moneta Mone : MappaAddressNomeMoneta.values()){
                            Address=Mone.MonetaAddress.toUpperCase();//è importante sia maiuscolo per la corretta imputazione del prezzo
                            Rete=Mone.Rete;
                           // System.out.println("RW_Funzione_ModificaValore : "+Mone.Moneta+ " - "+Mone.MonetaAddress+" - "+Mone.Rete);
                           // System.out.println(DataconOra);
                        //Se è un numero inserisco il prezzo e lo salvo a sistema
                        BigDecimal PrezzoUnitario = new BigDecimal(Prezz).divide(Qta, 30, RoundingMode.HALF_UP).stripTrailingZeros();
                      //  System.out.println(DataconOra+"-"+mon+"-"+PrezzoUnitario);
                        if (Address != null && Rete != null) {
                          //  System.out.println("Scrivo prezzo per Address");
                            DatabaseH2.PrezzoAddressChain_Scrivi(DataconOra + "_" + Address + "_" + Rete, PrezzoUnitario.toPlainString());
                           // System.out.println(DataconOra + "_" + Address + "_" + Rete +" - "+ PrezzoUnitario.toPlainString());
                        } else {
                            DatabaseH2.XXXEUR_Scrivi(DataconOra + " " + mon, PrezzoUnitario.toPlainString());
                        }
                        } 
                    } else {
                        JOptionPane.showConfirmDialog(this, "Attenzione, " + Prezz + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                }

         }else{
                JOptionPane.showConfirmDialog(this, "<html>Attenzione<br>"
                        + "Siccome il valore è legato ad un movimento specifico verra' proposto la modifica dello stesso.<br>"
                        + "Questo potrebbe inficiare anche sul valore di altri righi che fanno riferimento allo stesso movimento.</html>",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                MovimentoManuale_GUI a = new MovimentoManuale_GUI();
                            a.CompilaCampidaID(ID);
            a.setLocationRelativeTo(this);
            a.setVisible(true);
            
            }
            
                         //Una volta cambiato il prezzo aggiorno la tabella
                this.RW_CalcolaRW();
                RW_Tabella.setRowSelectionInterval(rigaTabellaPrincipale, rigaTabellaPrincipale);
                RW_CompilaTabellaDettagli();
                if(RW_Tabella_Dettagli.getRowCount()>rigaselezionata)RW_Tabella_Dettagli.setRowSelectionInterval(rigaselezionata, rigaselezionata);
                RW_CompilaTabellaDettagliXID();
                RW_Tabella_Dettagli.requestFocus();
                //Una volta aggiornata la tabella ricreao la tabella dettagli e mi posiziono sulla riga di prima   
            
        }
    }
    
    
    private void GiacenzeaData_Bottone_CalcolaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_CalcolaActionPerformed
        // TODO add your handling code here:
        GiacenzeaData_CompilaTabellaToken();
    }//GEN-LAST:event_GiacenzeaData_Bottone_CalcolaActionPerformed

    private void GiacenzeaData_TabellaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_GiacenzeaData_TabellaKeyReleased
        // TODO add your handling code here:
        this.GiacenzeaData_CompilaTabellaMovimenti();
    }//GEN-LAST:event_GiacenzeaData_TabellaKeyReleased

    private void GiacenzeaData_TabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_TabellaMouseReleased
        // TODO add your handling code here:
        this.GiacenzeaData_CompilaTabellaMovimenti();
    }//GEN-LAST:event_GiacenzeaData_TabellaMouseReleased

    private void GiacenzeaData_Wallet_ComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_Wallet_ComboBoxMouseClicked
        // TODO add your handling code here:
        // this.GiacenzeaData_AggiornaComboBoxWallet();
      //  System.out.println("prrrrrrrrrrrrroroppopero");
    }//GEN-LAST:event_GiacenzeaData_Wallet_ComboBoxMouseClicked

    private void GiacenzeaData_Wallet_ComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_GiacenzeaData_Wallet_ComboBoxItemStateChanged
        // TODO add your handling code here:
         if(evt.getStateChange() == ItemEvent.SELECTED && GiacenzeaData_Wallet_ComboBox.isShowing()) {
             
          // System.out.println("poroppopero");
            GiacenzeaData_Funzione_AggiornaComboBoxWallet2();
           
           
           }
          // GiacenzeaData_Wallet_ComboBox.getSelectedItem()
    }//GEN-LAST:event_GiacenzeaData_Wallet_ComboBoxItemStateChanged

    private void GiacenzeaData_Bottone_MovimentiDefiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_MovimentiDefiActionPerformed
        // TODO add your handling code here:
                if (GiacenzeaData_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
            String Rete = null;
            if (GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1)!=null)
                Rete=GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1).toString();
            String Address = null;
            if (GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2)!=null)
                Address=GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2).toString();            
            String Wallet=Giacenzeadata_Walleta_Label.getText();
            
            if (Wallet.contains("0x")&&Wallet.contains("(")&&Wallet.contains(")")&&Address!=null&&Rete!=null) {
                Wallet=Wallet.split("\\(")[0].trim();
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        if (Rete.equalsIgnoreCase("BSC")){
                            Desktop.getDesktop().browse(new URI("https://bscscan.com/token/"+Address +"?a="+ Wallet));
                           }
                        else if(Rete.equalsIgnoreCase("CRO")){
                           Desktop.getDesktop().browse(new URI("https://cronoscan.com/token/"+Address +"?a="+ Wallet)); 
                        }
                    } catch (URISyntaxException | IOException ex) {
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else
              {
                JOptionPane.showConfirmDialog(this, "Per vedere i dettagli dei movimenti in explorer \nselezionare un singolo Wallet",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
              }  
        }
    }//GEN-LAST:event_GiacenzeaData_Bottone_MovimentiDefiActionPerformed

    private void Giacenzeadata_Walleta_LabelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_Giacenzeadata_Walleta_LabelPropertyChange
        // TODO add your handling code here:
        //System.out.println("Cambiato Wallet");
                    String Wallet=Giacenzeadata_Walleta_Label.getText();
            
        if (Wallet.contains("0x")&&Wallet.contains("(")&&Wallet.contains(")")) {
                this.GiacenzeaData_Bottone_GiacenzeExplorer.setEnabled(true);
            }
        else{
            this.GiacenzeaData_Bottone_GiacenzeExplorer.setEnabled(false);
            this.GiacenzeaData_Bottone_MovimentiDefi.setEnabled(false);
        }
    }//GEN-LAST:event_Giacenzeadata_Walleta_LabelPropertyChange

    private void GiacenzeaData_Bottone_GiacenzeExplorerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_GiacenzeExplorerMouseClicked
        // TODO add your handling code here:

            String Rete;        
            String Wallet=Giacenzeadata_Walleta_Label.getText().trim();
            
            if (Wallet.contains("0x")&&Wallet.contains("(")&&Wallet.contains(")")) {
                Rete=Wallet.split("\\(")[1].split("\\)")[0];
                Wallet=Wallet.split("\\(")[0].trim();
                
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        if (Rete.equalsIgnoreCase("BSC")){
                            Desktop.getDesktop().browse(new URI("https://bscscan.com/tokenholdings?a="+ Wallet));
                           }
                        else if(Rete.equalsIgnoreCase("CRO")){
                           Desktop.getDesktop().browse(new URI("https://cronoscan.com/tokenholdings?a="+ Wallet)); 
                        }
                    } catch (URISyntaxException | IOException ex) {
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else
              {
                JOptionPane.showConfirmDialog(this, "Per vedere i dettagli dei movimenti in explorer \nselezionare un singolo Wallet",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
              }  
        
    }//GEN-LAST:event_GiacenzeaData_Bottone_GiacenzeExplorerMouseClicked

    private void GiacenzeaData_Bottone_RettificaQtaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_RettificaQtaMouseReleased
        // TODO add your handling code here:
        //  boolean completato;
        GiacenzeaData_Funzione_SistemaQta();
    }//GEN-LAST:event_GiacenzeaData_Bottone_RettificaQtaMouseReleased

    private void GiacenzeaData_Funzione_SistemaQta() {
        if (GiacenzeaData_TabellaDettaglioMovimenti.getSelectedRow() >= 0) {
            int scelta=0;
            int rigaselezionataTabPrincipale = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
            String TipoMoneta = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionataTabPrincipale, 3).toString();
            int rigaselezionata = GiacenzeaData_TabellaDettaglioMovimenti.getSelectedRow();
            String IDTrans = GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 8).toString();
            String GiacenzaAttualeS = GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 7).toString();
            String Moneta = GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 2).toString();
            String AddressMoneta = null;
            if (GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 3) != null) {
                AddressMoneta = GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 3).toString();
            }
            long DataRiferimento = 0;
            BigDecimal GiacenzaAttuale = new BigDecimal(GiacenzaAttualeS);
            BigDecimal GiacenzaVoluta = new BigDecimal(0);
            BigDecimal QtaNuovoMovimento;
            String Wallet=Giacenzeadata_Walleta_Label.getText().trim();
            if (!Wallet.equalsIgnoreCase("tutti")){
            if (TipoMoneta.equalsIgnoreCase("Crypto")){
            
            String m = JOptionPane.showInputDialog(this, "<html>Il saldo alla data selezionata è : <b>" + GiacenzaAttuale.toPlainString() + "</b> <br>"
                    + "Indicare nel riquadro sottostante la giacenza che il token <b>" + Moneta + "</b> dovrà avere al termine dell'operazione: </html>", GiacenzaVoluta);
            //  completato = m!=null; //se premo annulla nel messaggio non devo poi chiudere la finestra, quindi metto completato=false
            if (m != null) {
                m = m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                if (CDC_Grafica.Funzioni_isNumeric(m, false)) {
                    GiacenzaVoluta = new BigDecimal(m);
                    QtaNuovoMovimento = GiacenzaVoluta.subtract(GiacenzaAttuale);
                    String SQta = QtaNuovoMovimento.toPlainString();
                    BigDecimal ValoreMovOrigine=new BigDecimal(GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 6).toString());
                    BigDecimal QtaMovOrigine=new BigDecimal(GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(rigaselezionata, 5).toString());
                    BigDecimal ValoreUnitarioToken=ValoreMovOrigine.divide(QtaMovOrigine,30, RoundingMode.HALF_UP).abs();
                    if (SQta.contains("-")) {
                        //Gestisco i movimenti di scarico (Prelievi)
                        String Testo = "<html>Per raggiungere la giacenza desiderata devo generare un movimento<br>"
                                + "di prelievo di <b>" + SQta.replace("-", "") + "</b> unità<br><br>"
                                + "Come classifichiamo il movimento?<br><br>"
                                + "<b>1</b> - <b>Non classifico il movimento</b>, dovrò gestirlo successivamente<br>"
                                + "    nella sezione 'Classificazione Trasferimenti Crypto'<br><br>"
                                + "<b>2</b> - Lo considero alla stregua di un <b>CashOut</b> <br>"
                                + "    (Verrà generata l'eventuale plusvalenza sul movimento)<br><br>"
                                + "<b>3</b> - Lo considero alla stregua di una <b>Commissione</b><br><br>"
                                + "<b>4</b> - Lo inserisco con descrizione <b>Rettifica Giacenza</b><br>"
                                + "    (Non verranno calcolate le eventuali plusvalenze)<br><br></html>";
                        Object[] Bottoni = {"Annulla", "1", "2", "3", "4"};
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

                            //ora chiedo di inserire una nota
                            String Nota = JOptionPane.showInputDialog(this,
                                    "<html>Inserire un eventuale nota sul movimento :</html>", "Rettifica di Giacenza");
                            if (Nota != null) {
                                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                //adesso compilo la parte comune del movimento
                                String RTOri[] = MappaCryptoWallet.get(IDTrans);
                                DataRiferimento = OperazioniSuDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi aggiungo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                long NuovoOrario = Long.parseLong(RTOri[0].split("_")[0]) + 1;
                                String RT[] = new String[ColonneTabella];
                                RT[0] = "";//questo può variare in caso di movimento di commissione per cui lo metto nel capitolo successivo
                                RT[1] = RTOri[1];
                                RT[2] = "1 di 1";
                                RT[3] = RTOri[3];
                                RT[4] = RTOri[4];
                                RT[6] = Moneta + " ->";
                                RT[8] = Moneta;
                                RT[9] = TipoMoneta;//da prendere dalla tabella prima
                                RT[10] = SQta;
                                Moneta M1 = new Moneta();
                                M1.Moneta = Moneta;
                                M1.MonetaAddress = AddressMoneta;
                                M1.Qta = SQta;
                                M1.Tipo = TipoMoneta;
                                M1.Rete = Funzioni.TrovaReteDaID(RTOri[0]);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete));
                                if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(SQta)).setScale(2,RoundingMode.HALF_UP).abs();
                                }
                                RT[15] = Prezzo.toPlainString();
                                RT[21] = Nota;
                                RT[22] = "M";
                                RT[26] = AddressMoneta;
                                RT[29] = RTOri[29];
                                RiempiVuotiArray(RT);

                                String IDOriSplittato[] = RTOri[0].split("_");
                                switch (scelta) {
                                    case 1 -> {
                                        //Non Classifico Movimento                                
                                        RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_1_PC";
                                        RT[5] = "PRELIEVO "+TipoMoneta.toUpperCase();
                                        RT[18] = "";
                                    }
                                    case 2 -> {
                                        //CashOut
                                        RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_1_PC";
                                        RT[5] = "CASHOUT O SIMILARE";
                                        RT[18] = "PCO - CASHOUT O SIMILARE";
                                    }
                                    case 3 -> {
                                        //Commissione
                                        RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_1_CM";
                                        RT[5] = "COMMISSIONE";
                                        RT[18] = "";
                                    }
                                    case 4 -> {
                                        //Rettifica Giacenza
                                        RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_1_PC";
                                        RT[5] = "RETTIFICA GIACENZA";
                                        RT[18] = "PWN - RETTIFICA GIACENZA";
                                    }
                                    default -> {
                                    }
                                }
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT[0], RT);

                            }
                        }
                    } else {
                        //Gestisco i movimenti di Carico (Depositi)
                        //Gestisco i movimenti di scarico (Prelievi)
                        String Testo = "<html>Per raggiungere la giacenza desiderata devo generare un movimento<br>"
                                + "di deposito di <b>" + SQta.replace("-", "") + "</b> unità<br><br>"
                                + "Come classifichiamo il movimento?<br><br>"
                                + "<b>1</b> - <b>Non classifico il movimento</b>, dovrò gestirlo successivamente<br>"
                                + "    nella sezione 'Classificazione Trasferimenti Crypto'<br><br>"
                                + "<b>2</b> - Lo considero alla stregua di un <b>Provento da detenzione</b> <br>"
                                + "    (Verrà generata una plusvalenza sul movimento pari al suo valore)<br><br>"
                                + "<b>3</b> - Carico il movimento con <b>Costo di carico = 0</b><br><br>"
                                + "</html>";
                        Object[] Bottoni = {"Annulla", "1", "2", "3"};
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

                            //ora chiedo di inserire una nota
                            String Nota = JOptionPane.showInputDialog(this,
                                    "<html>Inserire un eventuale nota sul movimento :</html>", "Rettifica di Giacenza");
                            if (Nota != null) {

                                //adesso compilo la parte comune del movimento
                                String RTOri[] = MappaCryptoWallet.get(IDTrans);
                                String IDOriSplittato[] = RTOri[0].split("_");
                                DataRiferimento = OperazioniSuDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi tolgo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                long NuovoOrario = Long.parseLong(RTOri[0].split("_")[0]) - 1;
                                String RT[] = new String[ColonneTabella];
                                RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_1_DC";
                                RT[1] = RTOri[1];
                                RT[2] = "1 di 1";
                                RT[3] = RTOri[3];
                                RT[4] = RTOri[4];
                                RT[6] = " ->" + Moneta;
                                RT[11] = Moneta;
                                RT[12] = TipoMoneta;
                                RT[13] = SQta;
                                Moneta M1 = new Moneta();
                                M1.Moneta = Moneta;
                                M1.MonetaAddress = AddressMoneta;
                                M1.Qta = SQta;
                                M1.Tipo = TipoMoneta;
                                M1.Rete = Funzioni.TrovaReteDaID(RTOri[0]);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete));
                                if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(SQta)).setScale(2,RoundingMode.HALF_UP).abs();
                                }
                                RT[15] = Prezzo.toPlainString();
                                RT[21] = Nota;
                                RT[22] = "M";
                                RT[28] = AddressMoneta;
                                RT[29] = RTOri[29];
                                RiempiVuotiArray(RT);

                                switch (scelta) {
                                    case 1 -> {
                                        //Non Classifico Movimento                                
                                        RT[5] = "DEPOSITO "+TipoMoneta.toUpperCase();
                                        RT[18] = "";
                                    }
                                    case 2 -> {
                                        //Rendita da Capitale
                                        RT[5] = "EARN";
                                        RT[18] = "DAI - Provento da Detenzione";
                                    }
                                    case 3 -> {
                                        //Costo di carico 0
                                        RT[5] = "DEPOSITO A COSTO 0";
                                        RT[18] = "DCZ - DEPOSITO A COSTO 0";
                                    }
                                    default -> {
                                    }
                                }
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT[0], RT);

                            }
                        }


                    }
                    //Adesso avviso che il movimento è inserito e ricarico l'intera pagina
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    if (scelta != 0 && scelta != -1) {
                        JOptionPane.showConfirmDialog(this, """
                                                            Movimento di rettifica generato con successo!
                                                            Ricordarsi di salvare i movimenti nella sezione 'Transazioni Crypto'.""",
                                "Movimento Creato", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                        //Ora sistemo i valori sulla tabella principale
                        GiacenzeaData_Tabella.getModel().setValueAt(GiacenzaVoluta, rigaselezionataTabPrincipale, 4);
                        GiacenzeaData_CompilaTabellaMovimenti();
                        //E ricarico la tabella secondaria

                        // GiacenzeaData_CompilaTabellaToken();
                        //Avviso il programma che devo anche aggiornare la tabella crypto e ricalcolare le plusvalenze
                        TabellaCryptodaAggiornare = true;
                        //Tra le altre cose devo anche ricalcolare l'RW qualora sia stato già calcolato
                    }
                } else {
                    //  completato=false;
                    JOptionPane.showConfirmDialog(this, "Attenzione, " + m + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
            }
        }else{
                    //  completato=false;
                    JOptionPane.showConfirmDialog(this, """
                                                        Questo tipo di operazione \u00e8 consentita solo per le Crypto.
                                                        Per NFT e FIAT utilizzare l'inserimento manuale.""",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
            }else{
                    //  completato=false;
                    JOptionPane.showConfirmDialog(this, """
                                                        Questo tipo di operazione \u00e8 consentita solo sui singoli Wallet.
                                                        Selezionare un sigolo Wallet dal men\u00f9 a tendina in alto.""",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    private void GiacenzeaData_Bottone_RettificaQtaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_RettificaQtaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_GiacenzeaData_Bottone_RettificaQtaActionPerformed

    private void GiacenzeaData_TabellaDettaglioMovimentiKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_GiacenzeaData_TabellaDettaglioMovimentiKeyReleased
        // TODO add your handling code here:
        GiacenzeaData_Bottone_RettificaQta.setEnabled(true);
    }//GEN-LAST:event_GiacenzeaData_TabellaDettaglioMovimentiKeyReleased

    private void GiacenzeaData_TabellaDettaglioMovimentiMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GiacenzeaData_TabellaDettaglioMovimentiMouseReleased
        // TODO add your handling code here:
        GiacenzeaData_Bottone_RettificaQta.setEnabled(true);
    }//GEN-LAST:event_GiacenzeaData_TabellaDettaglioMovimentiMouseReleased

    private void GiacenzeaData_CheckBox_MostraQtaZeroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_CheckBox_MostraQtaZeroActionPerformed
        // TODO add your handling code here:
        if(GiacenzeaData_Tabella.getRowCount()!=0)
            GiacenzeaData_CompilaTabellaToken();
    }//GEN-LAST:event_GiacenzeaData_CheckBox_MostraQtaZeroActionPerformed

    private void GiacenzeaData_Bottone_ModificaValoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_ModificaValoreActionPerformed
        // TODO add your handling code here:
        GiacenzeaData_Funzione_ModificaValore();
    }//GEN-LAST:event_GiacenzeaData_Bottone_ModificaValoreActionPerformed

    private void GiacenzeaData_Bottone_ScamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_ScamActionPerformed
        // TODO add your handling code here:
        GiacenzeaData_Funzione_IdentificaComeScam();
    }//GEN-LAST:event_GiacenzeaData_Bottone_ScamActionPerformed

    private void BinanceApi()
         {   
                    BufferedReader reader = null;
        HttpURLConnection connection = null;
        try {
            // TODO add your handling code here:
            String timestamp = Long.toString(System.currentTimeMillis());
            String data = "recvWindow=60000&timestamp="+timestamp;
            String signature = generateSignature(data, "");
            String richiesta="https://api.binance.com/sapi/v1/asset/assetDividend?"+data+ "&signature=" + signature;
         //   System.out.println(richiesta);
            URL apiUrl = new URL(richiesta);
            connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-MBX-APIKEY", "");
                        int responseCode = connection.getResponseCode();
         //   System.out.println("Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

            //    System.out.println(response.toString());
            } else {
            //    System.out.println("Request failed. Response Code: " + responseCode);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
            //System.out.println(signature);
         //   System.out.println(timestamp);
        }
 }
    
    private void GiacenzeaData_Bottone_CambiaNomeTokenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_Bottone_CambiaNomeTokenActionPerformed
        // TODO add your handling code here:
            
            GiacenzeaData_Funzione_CambiaNomeToken();

    }//GEN-LAST:event_GiacenzeaData_Bottone_CambiaNomeTokenActionPerformed

    private void DepositiPrelievi_Bottone_DettaglioDefiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DepositiPrelievi_Bottone_DettaglioDefiActionPerformed
        // TODO add your handling code here:
        if (DepositiPrelievi_Tabella.getSelectedRow()>=0){
        int rigaselezionata = DepositiPrelievi_Tabella.getSelectedRow();        
        String ID = DepositiPrelievi_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
        if (!Funzioni.ApriExplorer(ID)){
            JOptionPane.showConfirmDialog(this, """
                                                Non è possibile aprire l'explorer per questa transazione.
                                                        """,
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
        }
        }
        
    }//GEN-LAST:event_DepositiPrelievi_Bottone_DettaglioDefiActionPerformed

    private void TransazioniCrypto_Bottone_MovimentoModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_MovimentoModificaActionPerformed
        // TODO add your handling code here:

        if (TransazioniCryptoTabella.getSelectedRow() >= 0) {
            MovimentoManuale_GUI a = new MovimentoManuale_GUI();
            int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());
            String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
            String riga[]=CDC_Grafica.MappaCryptoWallet.get(IDTransazione);

            String PartiCoinvolte[] = (riga[0] + "," + riga[20]).split(",");
            if (PartiCoinvolte.length > 1 && !riga[22].equalsIgnoreCase("AU")) {//devo permettere di modificare i movimenti automatici generati dagli scambi per poter cambiare eventualmente il prezzo
                String Messaggio = "Attenzione, il movimento è associato ad un altro movimento.\n"
                        + "se si prosegue l'associazione verrà rimossa, si vuole continuare?";
                int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Conferma modifica", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        RiportaTransazioniASituazioneIniziale(PartiCoinvolte); 

                            a.CompilaCampidaID(IDTransazione);
                            a.setLocationRelativeTo(this);
                            a.setVisible(true);
                            CDC_Grafica.TabellaCryptodaAggiornare=true;
                        
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
                a.CompilaCampidaID(IDTransazione);
                a.setLocationRelativeTo(this);
                a.setVisible(true);
            }
        }
    }//GEN-LAST:event_TransazioniCrypto_Bottone_MovimentoModificaActionPerformed

    private void TransazioniCrypto_Bottone_MovimentoEliminaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_MovimentoEliminaActionPerformed
        // TODO add your handling code here:
        if (TransazioniCryptoTabella.getSelectedRow()>=0){
            int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());
            String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
            int risposta=JOptionPane.showOptionDialog(this,"Sicuro di voler cancellare la transazione con ID "+IDTransazione+" ?", "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
            if (risposta==0){
                //controllo se quel movimento è associato ad altri e nel qual caso lo sbianco e sbianco i movimenti associati a lui
                Funzioni.RimuoviMovimentazioneXID(IDTransazione);
                TabellaCryptodaAggiornare=true;
                // TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
                // TransazioniCrypto_Funzioni_AggiornaPlusvalenze();
                // TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                JOptionPane.showConfirmDialog(this, "Transazione con ID"+IDTransazione+" eliminata correttamente.\nPremere sul Bottone Salva per rendere permanente la cancellazione fatta.",
                    "Eliminazione riuscita",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            }
        }

    }//GEN-LAST:event_TransazioniCrypto_Bottone_MovimentoEliminaActionPerformed

    private void TransazioniCrypto_Bottone_MovimentoNuovoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_MovimentoNuovoActionPerformed
        // TODO add your handling code here:
        
        if (TransazioniCryptoTabella.getSelectedRow() >= 0) {
            MovimentoManuale_GUI a = new MovimentoManuale_GUI();
            int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());
            String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
            a.CompilaCampiPrincipalidaID(IDTransazione);
            a.setLocationRelativeTo(this);
            a.setVisible(true);
        }else
        {
        MovimentoManuale_GUI a= new MovimentoManuale_GUI();
        a.setLocationRelativeTo(this);
        a.setVisible(true);
        }
    }//GEN-LAST:event_TransazioniCrypto_Bottone_MovimentoNuovoActionPerformed

    private void TransazioniCrypto_Bottone_DettaglioDefiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_DettaglioDefiActionPerformed
        // TODO add your handling code here:

        if (TransazioniCryptoTabella.getSelectedRow() >= 0) {
            int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());

            //String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 24).toString();
            String ID=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
            /* String Rete=Funzioni.TrovaReteDaID(ID);
            if (IDTransazione != null) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        if (Rete.equalsIgnoreCase("BSC")){
                            Desktop.getDesktop().browse(new URI("https://bscscan.com/tx/" + IDTransazione));
                        }
                        else if(Rete.equalsIgnoreCase("CRO")){
                            Desktop.getDesktop().browse(new URI("https://cronoscan.com//tx/" + IDTransazione));
                        }
                    } catch (URISyntaxException | IOException ex) {
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }*/
            Funzioni.ApriExplorer(ID);
        }

    }//GEN-LAST:event_TransazioniCrypto_Bottone_DettaglioDefiActionPerformed

    private void TransazioniCrypto_Bottone_InserisciWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_InserisciWalletActionPerformed
        // TODO add your handling code here:
        GestioneWallets a =new GestioneWallets();
        a.setLocationRelativeTo(this);
        a.setTitle("Gestione dei Wallet Defi");
        a.setVisible(true);
        //   System.out.println("cavolo");
    }//GEN-LAST:event_TransazioniCrypto_Bottone_InserisciWalletActionPerformed

    private void TransazioniCrypto_Text_PlusvalenzaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Text_PlusvalenzaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TransazioniCrypto_Text_PlusvalenzaActionPerformed

    private void TransazioniCrypto_Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_AnnullaActionPerformed
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        TransazioniCrypto_DaSalvare=false;
        try {

            // TODO add your handling code here:
            this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.setCursor(Cursor.getDefaultCursor());

    }//GEN-LAST:event_TransazioniCrypto_Bottone_AnnullaActionPerformed

    private void TransazioniCrypto_CheckBox_EscludiTIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_CheckBox_EscludiTIActionPerformed
        // TODO add your handling code here:
        //disabilito il filtro prima dell'eleaborazioneper velocizzare il tutto
        //il filtro altrimenti viene applicato ogni volta che aggiungo una riga in tabella e rallenta tantissimo
        //     this.FiltraTabella(TransazioniCryptoTabella, "", 999);
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        //     this.FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }//GEN-LAST:event_TransazioniCrypto_CheckBox_EscludiTIActionPerformed

    private void TransazioniCryptoFiltro_TextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TransazioniCryptoFiltro_TextKeyReleased
        // TODO add your handling code here:
        this.Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
    }//GEN-LAST:event_TransazioniCryptoFiltro_TextKeyReleased

    private void TransazioniCrypto_Bottone_SalvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_SalvaActionPerformed
        // TODO add your handling code here:
        Importazioni.Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Importazioni.TransazioniAggiunte=0;
        TransazioniCrypto_DaSalvare=false;
        TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
    }//GEN-LAST:event_TransazioniCrypto_Bottone_SalvaActionPerformed

    private void TransazioniCrypto_Bottone_ImportaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransazioniCrypto_Bottone_ImportaActionPerformed
        // TODO add your handling code here:
        // Calcoli.GeneraMappaCambioUSDEUR();
        // Importazioni.Formatta_Data_CoinTracking("11.07.2022 20:12");

        ///////       Importazioni.Importa_Crypto_CDCApp();
        ///////       CaricaTabellaCrypto();

        Importazioni_Gestione gest = new Importazioni_Gestione();
        gest.setLocationRelativeTo(this);
        gest.setVisible(true);

        //   TransazioniCrypto_Funzioni_PulisciMovimentiAssociatinonEsistenti();
        Calcoli_Plusvalenze.AggiornaPlusvalenze();
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());

        //questo sotto serve per aumentare la diomensione dell'header della tabella
        //Calcoli.RecuperaTassidiCambio();
    }//GEN-LAST:event_TransazioniCrypto_Bottone_ImportaActionPerformed

    private void TransazioniCryptoTabellaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TransazioniCryptoTabellaKeyReleased
        // TODO add your handling code here:
        TransazioniCrypto_CompilaTextPaneDatiMovimento();
    }//GEN-LAST:event_TransazioniCryptoTabellaKeyReleased

    private void TransazioniCryptoTabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TransazioniCryptoTabellaMouseReleased
        // TODO add your handling code here:
        TransazioniCrypto_CompilaTextPaneDatiMovimento();
    }//GEN-LAST:event_TransazioniCryptoTabellaMouseReleased

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            // TODO add your handling code here:

            //il modo più rapido per esportare la tabella è prendere tutta la mappa ed esportare solo quello che c'è tra le date e che ha
            //nel suo contenuto il filtro in basso
            
            
            File export = new File("temp.csv");
            FileWriter w = new FileWriter(export);
            BufferedWriter b = new BufferedWriter(w);
            b.write("ID,Data,nMov,Exchange/Wallet,Dett.Wallet,TipoTransazione,Dettaglio Movimento,Causale Originale,Crypto Uscita,Tipo Crypto Uscita,Qta Uscita,"+
                    "Crypto Entrata,Tipo Crypto Entrata,Qta Entrata,Valore Transazione (da CSV),Valore Transazione in Euro,Costo di Carico Uscente,Nuovo Costo di Carico,"+
                    "Tipo Trasferimento,Plusvalenza generata in Euro,Rif. per Trasferimenti,Note,Auto,Blocco Transazione,Hash Transazione,Nome Token Uscita,"+
                    "Address Token Uscita,Nome Token Entrata,Address Token Entrata,Timestamp,Address Controparte, \n");
            for (String[] R : MappaCryptoWallet.values()) {
                if (Funzioni_Date_ConvertiDatainLong(R[1]) >= Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale)
                        && Funzioni_Date_ConvertiDatainLong(R[1]) <= Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {
                    String Stringa="";
                    for (String prov : R) {
                        Stringa=Stringa+prov+",";
                        
                    }
                     Stringa=Stringa+"\n";
                        b.write(Stringa);
                        
                }
                   
            }
            b.close();
            w.close();
            Desktop desktop = Desktop.getDesktop();  
            desktop.open(export); 
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }

        

    }//GEN-LAST:event_jButton1ActionPerformed

    private void CDC_DataChooser_InizialeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_CDC_DataChooser_InizialeKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_CDC_DataChooser_InizialeKeyPressed

    private void CDC_DataChooser_InizialeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_CDC_DataChooser_InizialeFocusLost
        // TODO add your handling code here:
       // System.out.println("lost");
    }//GEN-LAST:event_CDC_DataChooser_InizialeFocusLost

    private void OpzioniComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_OpzioniComponentShown
        // TODO add your handling code here:
        Opzioni_RicreaListaWalletDisponibili();
        Opzioni_GruppoWallet_CaricaGruppiWallet();
        Opzioni_Emoney_CaricaTabellaEmoney();
    }//GEN-LAST:event_OpzioniComponentShown

    private void Opzioni_Emoney_TabellaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Opzioni_Emoney_TabellaMouseClicked
        //  try {
            // TODO add your handling code here:
            if(Opzioni_Emoney_Tabella.getSelectedColumn()==1){
                String token=Opzioni_Emoney_Tabella.getModel().getValueAt(Opzioni_Emoney_Tabella.getSelectedRow(), 0).toString();
                Date data=(Date)Opzioni_Emoney_Tabella.getModel().getValueAt(Opzioni_Emoney_Tabella.getSelectedRow(), 1);
                String oldData=data.toString();
                GUI_ScegliData a = new GUI_ScegliData();
                a.setLocationRelativeTo(this);
                a.ImpostaData(data);
                a.ImpostaTitolo("Imposta la data dal quale il token "+token+" deve essere considerato E-Money");
                a.setVisible(true);
                if (JDialog_Ritorno!=null){
                    Date nuovaData=(Date)JDialog_Ritorno;
                    Opzioni_Emoney_Tabella.getModel().setValueAt(nuovaData, Opzioni_Emoney_Tabella.getSelectedRow(), 1);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String DataStringata=dateFormat.format(nuovaData);
                    DatabaseH2.Pers_Emoney_Scrivi(token, DataStringata);
                    // TransazioniCrypto_DaSalvare=true;
                    if(!oldData.equalsIgnoreCase(nuovaData.toString()))
                    {
                        //TabellaCryptodaAggiornare=true;
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Calcoli_Plusvalenze.AggiornaPlusvalenze();
                        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
                //System.out.println(JDialog_Ritorno);
            }
    }//GEN-LAST:event_Opzioni_Emoney_TabellaMouseClicked

    private void Opzioni_Emoney_Bottone_RimuoviActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Emoney_Bottone_RimuoviActionPerformed
        // TODO add your handling code here:
        if (Opzioni_Emoney_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = Opzioni_Emoney_Tabella.getSelectedRow();
            String Moneta = Opzioni_Emoney_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            String Testo = "<html>Vuoi calncellare il Token <b>" + Moneta + "</b> dalla lista degli EMoney Token?<br><br>"
            + "</html>";
            Object[] Bottoni = {"Si", "No"};
            int scelta = JOptionPane.showOptionDialog(this, Testo,
                "Classificazione del Token",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                Bottoni,
                null);
            if (scelta == 0) {
                DatabaseH2.Pers_Emoney_Cancella(Moneta);
                Opzioni_Emoney_CaricaTabellaEmoney();
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                Calcoli_Plusvalenze.AggiornaPlusvalenze();
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                //TabellaCryptodaAggiornare=true;
            } else {

            }

        }
    }//GEN-LAST:event_Opzioni_Emoney_Bottone_RimuoviActionPerformed

    private void Opzioni_Emoney_Bottone_AggiungiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Emoney_Bottone_AggiungiActionPerformed
        // TODO add your handling code here:
        String Testo="<html>Digita il nome della moneta da aggiungere alla lista delle E-Money Token (es. USDC)<br>";
        Testo = Testo + "<b>Attenzione :</b> I nomi dei token sono CaseSensitive quindi, ad esempio, BTC è diverso da Btc o btc<br><br></html>";
        String m = JOptionPane.showInputDialog(this, Testo, "");
        if (m!=null){
            m=m.trim();
            if (DatabaseH2.Pers_Emoney_Leggi(m)==null){
                //System.out.println("Aggiunto "+m+" al databse");
                DatabaseH2.Pers_Emoney_Scrivi(m, "2000-01-01");
                Opzioni_Emoney_CaricaTabellaEmoney();
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                Calcoli_Plusvalenze.AggiornaPlusvalenze();
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                // TabellaCryptodaAggiornare=true;
            }else{
                JOptionPane.showConfirmDialog(this, "Il token è già presente in tabella.",
                    "Token già esistente",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            }
            // System.out.println("Trovato moneta: "+m);
            // System.out.println(DatabaseH2.Pers_Emoney_Leggi(m));
        }
    }//GEN-LAST:event_Opzioni_Emoney_Bottone_AggiungiActionPerformed

    private void Opzioni_GruppoWallet_TabellaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_Opzioni_GruppoWallet_TabellaFocusGained
        // TODO add your handling code here:
        //System.out.println("Componente cambiato");
        if (Opzioni_GruppoWallet_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = Opzioni_GruppoWallet_Tabella.getRowSorter().convertRowIndexToModel(Opzioni_GruppoWallet_Tabella.getSelectedRow());
            String Gruppo=Opzioni_GruppoWallet_Tabella.getModel().getValueAt(rigaselezionata, 1).toString();
            String Wallet=Opzioni_GruppoWallet_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            //Se viene modificato un gruppo wallet
            //1 - Scrivo il nuovo gruppo nel dabase
            //2 - Ricalcolo i dati dell'RW se presenti
            //3 - Aggiorno le plusvalenze
            //4 - Ricarico la tabella crypto
            if (!DatabaseH2.Pers_GruppoWallet_Leggi(Wallet).equals(Gruppo)){
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                //1 - Scrivo il nuovo gruppo nel dabase
                DatabaseH2.Pers_GruppoWallet_Scrivi(Wallet, Gruppo);
                //2 - Ricalcolo i dati dell'RW se presenti
                RW_RicalcolaRWseEsiste();
                //3 - Aggiorno le plusvalenze
                Calcoli_Plusvalenze.AggiornaPlusvalenze();
                //4 - Ricarico la tabella crypt
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_Opzioni_GruppoWallet_TabellaFocusGained

    private void RW_RicalcolaRWseEsiste() {
        //Questa funzione serve per ricalcolare l'RW qualora vi siano state delle modifiche
        //Va a verificare se l'RW è stato generato e in quel caso lo ricalcola altrimenti non fa nulla
        if (Mappa_RW_ListeXGruppoWallet != null && !Mappa_RW_ListeXGruppoWallet.isEmpty()) {
            RW_CalcolaRW();
        }
    }
    
    private void Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed
        // TODO add your handling code here:
        if(Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedIndex()!=0) {

            String Messaggio="Sicuro di voler cancellare tutti i dati delle Transazioni Crypto del Wallet "+Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedItem().toString()+"?";
            int risposta=JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
            if (risposta==0)
            {
                Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, "", 999);
                int movimentiCancellati=Funzioni.CancellaMovimentazioniXWallet(Opzioni_Combobox_CancellaTransazioniCryptoXwallet.getSelectedItem().toString());
                if (movimentiCancellati>0){
                    Opzioni_RicreaListaWalletDisponibili();
                    Calcoli_Plusvalenze.AggiornaPlusvalenze();
                    TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                    TransazioniCrypto_DaSalvare=true;
                    TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
                }
                Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
                Messaggio="Numero movimenti cancellati : "+movimentiCancellati+ "\n Ricordarsi di Salvare per non perdere le modifiche fatte.";
                JOptionPane.showOptionDialog(this,Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK"}, "OK");

            }
        }
    }//GEN-LAST:event_Opzioni_Bottone_CancellaTransazioniCryptoXwalletActionPerformed

    private void Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed
        // TODO add your handling code here:
        // TODO add your handling code here:
        String Messaggio = "Sicuro di voler cancellare tutti i dati delle Transazioni Crypto?";
        int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
        if (risposta == 0) {
            MappaCryptoWallet.clear();
            TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
            TransazioniCrypto_DaSalvare=true;
            TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
            Messaggio = "Sono state cancellate tutte le movimentazioni crypto \nRicordarsi di Salvare per non perdere le modifiche fatte.";
            JOptionPane.showOptionDialog(this, Messaggio, "Cancellazione Transazioni Crypto", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK"}, "OK");
        }
    }//GEN-LAST:event_Opzioni_Bottone_CancellaTransazioniCryptoActionPerformed

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

    private void Opzioni_GruppoWallet_CheckBox_PlusXWalletActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_GruppoWallet_CheckBox_PlusXWalletActionPerformed
        // TODO add your handling code here:
        // System.out.println(Opzioni_GruppoWallet_CheckBox_PlusXWallet.isSelected());
        if (Opzioni_GruppoWallet_CheckBox_PlusXWallet.isSelected()) {
            //scrivo nelle Opzioni del DB che voglio il calcolo delle plus X Gruppo Wallet
            DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "SI");
        } else {
            //scrivo nelle Opzioni del DB che nel calcolo delle plus non considero la suddivisione per wallet
            DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Calcoli_Plusvalenze.AggiornaPlusvalenze();
        TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_Opzioni_GruppoWallet_CheckBox_PlusXWalletActionPerformed

    private void RW_Bottone_CalcolaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Bottone_CalcolaActionPerformed
        // TODO add your handling code here:
        RW_CalcolaRW();
        
       
    }//GEN-LAST:event_RW_Bottone_CalcolaActionPerformed

    private void RW_CalcolaRW(){
                // TODO add your handling code here:
        //Come prima cosa faccio un pò di pulizia
        System.out.println("RW_CalcoloRW");
        DefaultTableModel ModelloTabella = (DefaultTableModel) this.RW_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabella);
        DefaultTableModel ModelloTabella2 = (DefaultTableModel) RW_Tabella_Dettagli.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabella2);
        DefaultTableModel ModelloTabella3 = (DefaultTableModel) RW_Tabella_DettaglioMovimenti.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabella3);
        RW_Bottone_CorreggiErrore.setEnabled(false);
        RW_Bottone_IdentificaScam.setEnabled(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //Array Lista RW così composta
        // 0 - Anno
        // 1 - Gruppo Wallet
        // 2 - Moneta
        // 3 - Qta
        // 4 - Data Inizio
        // 5 - Prezzo Inizio
        // 6 - Data Fine
        // 7 - Prezzo Fine
        // 8 - Giorni di detenzione
        // 9 - Causale
                Download progress = new Download();
        progress.setLocationRelativeTo(this);
                Thread thread;
        thread = new Thread() {
            public void run() {

        //Compilo la mappa QtaCrypto con la somma dei movimenti divisa per crypto
        //in futuro dovrò mettere anche un limite per data e un limite per wallet
        progress.Titolo("Calcolo RW in corso.... Attendere");
        progress.SetLabel("Calcolo RW in corso.... Attendere");
        progress.NascondiBarra();
        progress.NascondiInterrompi();
        Calcoli_RW.AggiornaRWFR(RW_Anno_ComboBox.getSelectedItem().toString());// Questa Funzione va a popolare Mappa_RW_ListeXGruppoWallet che contiene una la lista degli RW per ogni wallet
        //Poi utilizzerò questa lista per fare la media ponderata e popolare la tabella
        for (String key : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.keySet()) {
            String Errore="";
            BigDecimal ValIniziale = new BigDecimal(0);
            BigDecimal ValFinale = new BigDecimal(0);
            BigDecimal ValFinalexggTOT = new BigDecimal(0);
            String RW1[] = new String[5];
            for (String[] lista : Mappa_RW_ListeXGruppoWallet.get(key)) {
              //  System.out.println(lista[1]);
                if (lista[4].equals("0000-00-00 00:00"))Errore="ERRORI";
                if (lista[15].toLowerCase().contains("error"))Errore="ERRORI";
                ValIniziale = new BigDecimal(lista[5]).add(ValIniziale);
                ValFinale = new BigDecimal(lista[10]).add(ValFinale);
                ValFinalexggTOT = new BigDecimal(lista[10]).multiply(new BigDecimal(lista[11])).add(ValFinalexggTOT);
            }
            BigDecimal GGPonderati;//=new BigDecimal(999999);
            if (ValFinale.compareTo(new BigDecimal(0))!=0) {
                GGPonderati = ValFinalexggTOT.divide(ValFinale, 2, RoundingMode.HALF_UP);
            }else{ 
                GGPonderati=new BigDecimal("365.00").setScale(2, RoundingMode.HALF_UP);
                    Errore="Wallet vuoto o senza valore";
                }
            if (Errore.equalsIgnoreCase("ERRORI"))GGPonderati=new BigDecimal(999999);
            RW1[0] = key.split(" ")[1] + " (" + key + ")";
            RW1[1] = ValIniziale.toPlainString();
            RW1[2] = ValFinale.toPlainString();
            RW1[3] = GGPonderati.toPlainString();
            RW1[4] = Errore;
            ModelloTabella.addRow(RW1);
            
        }
        Tabelle.ColoraTabellaEvidenzaRigheErrore(RW_Tabella);
        progress.ChiudiFinestra();
        }
            };
        thread.start();
        progress.setVisible(true);
       // Tabelle.ColoraTabellaEvidenzaRigheErrore(RW_Tabella);
        RW_Tabella.requestFocus();
        
        
        
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        //Adesso Calcolo la media ponderata e genero gli RW dalla lista appena creata
    }
    
    private void RW_CompilaTabellaDettagli(){
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (RW_Tabella.getSelectedRow()>=0){
            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabella = (DefaultTableModel) RW_Tabella_Dettagli.getModel();
            Funzioni_Tabelle_PulisciTabella(ModelloTabella);
            DefaultTableModel ModelloTabella3 = (DefaultTableModel) RW_Tabella_DettaglioMovimenti.getModel();
            Funzioni_Tabelle_PulisciTabella(ModelloTabella3);
            
           // ATTENZIONE : Correggere gli errori per calcolare i totali corretti
           int rigaselezionata = RW_Tabella.getSelectedRow();
            if (RW_Tabella.getModel().getValueAt(rigaselezionata, 4).toString().toLowerCase().contains("errori"))
                this.RW_Label_SegnalaErrori.setText("ATTENZIONE : Correggere gli errori per calcolare i totali corretti");
            else
                this.RW_Label_SegnalaErrori.setText("");   
            
            String Gruppo = RW_Tabella.getModel().getValueAt(rigaselezionata, 0).toString().split("\\(")[1].replaceAll("\\)", "").trim();
            //System.out.println(Gruppo);
            for (String[] lista : Mappa_RW_ListeXGruppoWallet.get(Gruppo)) {
                if (RW_CheckBox_VediSoloErrori.isSelected())
                    {
                        if(lista[15].toLowerCase().contains("errore")) ModelloTabella.addRow(lista);
                    }
                else 
                    ModelloTabella.addRow(lista);
            }
          //  ModelloTabella.addRow(Mappa_RW_ListeXGruppoWallet);
            
            Tabelle.ColoraTabellaEvidenzaRigheErrore(RW_Tabella_Dettagli);
            Tabelle.updateRowHeights(RW_Tabella_Dettagli);
           } 
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    private void RW_CompilaTabellaDettagliXID(){
        if (RW_Tabella_Dettagli.getSelectedRow()>=0){
            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabella3 = (DefaultTableModel) RW_Tabella_DettaglioMovimenti.getModel();
            Funzioni_Tabelle_PulisciTabella(ModelloTabella3);
            int rigaselezionata = RW_Tabella_Dettagli.getSelectedRow();
            String Errore = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 15).toString();
            String MonetaTabIni = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 2).toString().trim();
            String MonetaTabFin = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 7).toString().trim();
            
            if (Errore.toLowerCase().contains("scam")){
                RW_Bottone_IdentificaScam.setText("Rimuovi da SCAM");
                RW_Bottone_IdentificaScam.setEnabled(true);
                }
            else{
                RW_Bottone_IdentificaScam.setText("Identifica come SCAM");
                }
            if (Errore.toLowerCase().contains("errore")) {
                //se c'è un errore sulla riga abilito il pulsante di correzione altrimenti lo disabilito
                RW_Bottone_CorreggiErrore.setEnabled(true);
                if (Errore.toLowerCase().contains("non valorizzato")){
                    RW_Bottone_IdentificaScam.setEnabled(true);
                }
            }else{
                RW_Bottone_CorreggiErrore.setEnabled(false);
                RW_Bottone_IdentificaScam.setEnabled(false);
            }
            //IDIniziale è l'id del movimento che ha fatto partire l'RW
            //IDFinale è l'id del movimento che ha chiuso l'RW
            String IDMovimentati[]=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 16).toString().split(",");
            String IDIniziale = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 13).toString();
            String IDFinale = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 14).toString();
            String GruppoWalletIni = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 1).toString().trim();
            String GruppoWalletFin = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 6).toString().trim();
            
           // System.out.println(IDIniziale + " - " + IDFinale);
            String[] Movimento=MappaCryptoWallet.get(IDIniziale);
            if (Movimento!=null){
                String Mov[]=new String[7];
                Mov[0]=Movimento[1];
                Mov[1]=GruppoWalletIni+" ("+Movimento[3]+")";
                Mov[2]=Movimento[5];
                Mov[3]="";
                Mov[4]="";
                if (!Movimento[10].isBlank())Mov[3]=Movimento[10]+" "+Movimento[8]+" ("+Movimento[9]+")";
                if (!Movimento[13].isBlank())Mov[4]=Movimento[13]+" "+Movimento[11]+" ("+Movimento[12]+")";
                Mov[5]=Movimento[15];
                Mov[6]=IDIniziale; 
                ModelloTabella3.addRow(Mov);
            }else{
                String Mov[]=new String[7];
                //Se arrivo qua significa che il movimento è in realtà la giacenza iniziale
                //devo quindi trovare a quanto ammonta e scriverlo
                List<Moneta> listaIniziale=Mappa_RW_GiacenzeInizioPeriodo.get(GruppoWalletIni);
                String MonNome="";
                String MonQta="";
                String MonPrz="";
                String MonTipo="";
                if(listaIniziale!=null){
                Iterator<Moneta> it=listaIniziale.iterator();
                Moneta Mon;
                while (it.hasNext()){
                    Mon=it.next();
                    if (Mon.Moneta.equals(MonetaTabIni)){
                        MonNome=MonetaTabIni;
                        MonQta=Mon.Qta;
                        MonPrz=new BigDecimal(Mon.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                        MonTipo=Mon.Tipo;
                    }
                }
                }
                Mov[0]=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 4).toString();
                Mov[1]=GruppoWalletIni;
                Mov[2]=IDIniziale;
                Mov[3]="";
                Mov[4]=MonQta+" "+MonNome+" ("+MonTipo+")";
                Mov[5]=MonPrz;
                Mov[6]=""; 
                ModelloTabella3.addRow(Mov);
            }
            for (String id : IDMovimentati) {
                Movimento = MappaCryptoWallet.get(id);
                if (Movimento != null) {
                    String gwallet=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
                    String Mov[] = new String[7];
                    Mov[0] = Movimento[1];
                    Mov[1] = gwallet + " (" + Movimento[3] + ")";//da sistemare gruppo wallet
                    Mov[2] = Movimento[5];
                    Mov[3] = "";
                    Mov[4] = "";
                    if (!Movimento[10].isBlank()) {
                        Mov[3] = Movimento[10] + " " + Movimento[8] + " (" + Movimento[9] + ")";
                    }
                    if (!Movimento[13].isBlank()) {
                        Mov[4] = Movimento[13] + " " + Movimento[11] + " (" + Movimento[12] + ")";
                    }
                    Mov[5] = Movimento[15];
                    Mov[6] = id;
                    ModelloTabella3.addRow(Mov);
                }
            }
            
            
            Movimento=MappaCryptoWallet.get(IDFinale);
            if (Movimento!=null){
                String Mov[]=new String[7];
                Mov[0]=Movimento[1];
                Mov[1]=GruppoWalletFin+" ("+Movimento[3]+")";
                Mov[2]=Movimento[5];
                Mov[3]="";
                Mov[4]="";
                if (!Movimento[10].isBlank())Mov[3]=Movimento[10]+" "+Movimento[8]+" ("+Movimento[9]+")";
                if (!Movimento[13].isBlank())Mov[4]=Movimento[13]+" "+Movimento[11]+" ("+Movimento[12]+")";
                Mov[5]=Movimento[15];
                Mov[6]=IDFinale; 
                ModelloTabella3.addRow(Mov);
            }else{
                String Mov[]=new String[7];
                List<Moneta> listaFinale=Mappa_RW_GiacenzeFinePeriodo.get(GruppoWalletFin);
                String MonNome="";
                String MonQta="";
                String MonPrz="";
                String MonTipo="";
                if(listaFinale!=null){
                Iterator<Moneta> it=listaFinale.iterator();
                Moneta Mon;
                while (it.hasNext()){
                    Mon=it.next();
                    if (Mon.Moneta.equals(MonetaTabFin)){
                        MonNome=MonetaTabFin;
                        MonQta=Mon.Qta;
                        MonPrz=new BigDecimal(Mon.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                        MonTipo=Mon.Tipo;
                    }
                }}
                Mov[0]=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 9).toString();
                Mov[1]=GruppoWalletFin;
                Mov[2]=IDFinale;
                Mov[3]=MonQta+" "+MonNome+" ("+MonTipo+")";
                Mov[4]="";
                Mov[5]=MonPrz;
                Mov[6]=""; 
                ModelloTabella3.addRow(Mov);
            }
            Tabelle.ColoraTabellaEvidenzaRigheErrore(RW_Tabella_DettaglioMovimenti);
           } 
    }
    
    private void RW_TabellaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_RW_TabellaKeyReleased
        // TODO add your handling code here:
        RW_CompilaTabellaDettagli();
    }//GEN-LAST:event_RW_TabellaKeyReleased

    private void RW_TabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RW_TabellaMouseReleased
        // TODO add your handling code here:
        RW_CompilaTabellaDettagli();
    }//GEN-LAST:event_RW_TabellaMouseReleased

    private void RW_Tabella_DettagliKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_RW_Tabella_DettagliKeyReleased
        // TODO add your handling code here:
        RW_CompilaTabellaDettagliXID();
    }//GEN-LAST:event_RW_Tabella_DettagliKeyReleased

    private void RW_Tabella_DettagliMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RW_Tabella_DettagliMouseReleased
        // TODO add your handling code here:
        RW_CompilaTabellaDettagliXID();
    }//GEN-LAST:event_RW_Tabella_DettagliMouseReleased

    private void RW_Bottone_CorreggiErroreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Bottone_CorreggiErroreActionPerformed
        // TODO add your handling code here:
        //Quello che devo fare sono le seguenti cose:
        //1-Individuare il tipo di errore
        //2-Se è un errore di giacenza negativa :
            //2a-Posizionare il focus su GiacenzeaData
            //2b-Cambiare la data di riferimento con quella di fine anno RW
            //2c-Cambiare la label del wallet selezionato con quella del gruppo
            //2d-Compilare le 2 tabelle, la prima con le giacenze del wallet e la seconda tabella con le movimentazioni della coin con errore
            //2e-Far uscire un messaggio chiedendo di correggere la giacenza negativa
        //3-Se è un errore di prezzo non disponibile a sistema:
            //3a-Dare la possibilità di correggerlo o eventualmente identificarlo come token scam, in quel caso varrà sempre zero ma non verrà segnalato come errore
        
         //Punto 1   
           if (RW_Tabella_Dettagli.getSelectedRow() >= 0) {

            int rigaselezionata = RW_Tabella_Dettagli.getSelectedRow();
            String Errore = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 15).toString();
            //Punto 2
            if (Errore.toLowerCase().contains("giacenza negativa")) {
                try {
                    JOptionPane.showConfirmDialog(this, "Si verrà ora reindirizzati alla funzione GiacenzeaData\nSistemare le giacenze negative per correggere l'RW!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    //Punto 2a
                    AnalisiCrypto.setSelectedComponent(GiacenzeaData);
                    //Punto 2b
                    String DataFineRW=RW_Anno_ComboBox.getSelectedItem().toString()+"-12-31";
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                    Date d = f.parse(DataFineRW);                    
                    GiacenzeaData_Data_DataChooser.setDate(d);
                    //Punto 2c
                    String GWallet=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 1).toString();
                    GiacenzeaData_Wallet_ComboBox.setSelectedItem("Gruppo : "+GWallet);
                    //Punto 2d
                    GiacenzeaData_CompilaTabellaToken();
                    //Punto 2e
                    int righeTabella=GiacenzeaData_Tabella.getModel().getRowCount();
                    //Certo la riga della tabella con la moneta incriminata
                    int rigaTabellaMoneta=0;
                    String MonetaCercata=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 2).toString();
                    for (int i=0;i<righeTabella;i++){
                        if (GiacenzeaData_Tabella.getModel().getValueAt(i, 0).toString().equals(MonetaCercata)){
                            rigaTabellaMoneta=i;
                        }
                    }
                    GiacenzeaData_Tabella.setRowSelectionInterval(rigaTabellaMoneta, rigaTabellaMoneta);
                    GiacenzeaData_CompilaTabellaMovimenti();
                    //Adesso mi posiziono nel primo movimento con giacenza negativa che trovo
                    righeTabella=GiacenzeaData_TabellaDettaglioMovimenti.getModel().getRowCount();
                    int rigaconGiacNegativa=0;
                    for (int i=0;i<righeTabella;i++){
                        if (GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(i, 7).toString().contains("-")){
                            rigaconGiacNegativa=i;
                            GiacenzeaData_Bottone_RettificaQta.setEnabled(true);
                        }
                    }
                    GiacenzeaData_TabellaDettaglioMovimenti.setRowSelectionInterval(rigaconGiacNegativa, rigaconGiacNegativa);
                    GiacenzeaData.requestFocus();
                    GiacenzeaData_TabellaDettaglioMovimenti.requestFocus();
                    
                } catch (ParseException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }

              
               } else if (Errore.toLowerCase().contains("apertura non classificato")) {
                   String IDTransazione = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 13).toString();
                   // System.out.println(IDTransazione);
                   ClassificazioneTrasf_Modifica mod = new ClassificazioneTrasf_Modifica(IDTransazione);
                   mod.setLocationRelativeTo(this);
                   mod.setVisible(true);

                   if (mod.getModificaEffettuata()) {
                       int riga = RW_Tabella.getSelectedRow();
                       TransazioniCrypto_DaSalvare = true;
                       TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);

                       Calcoli_Plusvalenze.AggiornaPlusvalenze();
                       this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                       DepositiPrelievi_Caricatabella();
                       RW_CalcolaRW();
                       RW_Tabella.setRowSelectionInterval(riga, riga);
                       RW_CompilaTabellaDettagli();
                   }
                   //Messaggio nessun errore da correggere sulla riga selezionata
               } else if (Errore.toLowerCase().contains("chiusura non classificato")) {
                   //Messaggio nessun errore da correggere sulla riga selezionata
                   String IDTransazione = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 14).toString();
                   // System.out.println(IDTransazione);
                   ClassificazioneTrasf_Modifica mod = new ClassificazioneTrasf_Modifica(IDTransazione);
                   mod.setLocationRelativeTo(this);
                   mod.setVisible(true);

                   if (mod.getModificaEffettuata()) {
                       int riga = RW_Tabella.getSelectedRow();
                       TransazioniCrypto_DaSalvare = true;
                       TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);

                       Calcoli_Plusvalenze.AggiornaPlusvalenze();
                       this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                       DepositiPrelievi_Caricatabella();
                       RW_CalcolaRW();
                       RW_Tabella.setRowSelectionInterval(riga, riga);
                       RW_CompilaTabellaDettagli();
                   }
               } else {
                   //Messaggio nessun errore da correggere sulla riga selezionata
               }
        }
    }//GEN-LAST:event_RW_Bottone_CorreggiErroreActionPerformed

    private void RW_Bottone_IdentificaScamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Bottone_IdentificaScamActionPerformed
        try {
            // TODO add your handling code here:
            int rigaselezionata = RW_Tabella_Dettagli.getSelectedRow();
            //   String Errore = RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 12).toString();
            JOptionPane.showConfirmDialog(this, "Si verrà ora reindirizzati alla funzione GiacenzeaData\nIdentificare i token SCAM per correggere l'RW!",
                    "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            //Punto 2a
            AnalisiCrypto.setSelectedComponent(GiacenzeaData);
            //Punto 2b
            String DataFineRW=RW_Anno_ComboBox.getSelectedItem().toString()+"-12-31";
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            Date d = f.parse(DataFineRW);
            GiacenzeaData_Data_DataChooser.setDate(d);
            //Punto 2c
            String GWallet=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 1).toString();
            GiacenzeaData_Wallet_ComboBox.setSelectedItem("Gruppo : "+GWallet);
            //Punto 2d
            GiacenzeaData_CompilaTabellaToken();
            //Punto 2e
            int righeTabella=GiacenzeaData_Tabella.getModel().getRowCount();
            //Certo la riga della tabella con la moneta incriminata
            int rigaTabellaMoneta=0;
            String MonetaCercata=RW_Tabella_Dettagli.getModel().getValueAt(rigaselezionata, 6).toString();
            for (int i=0;i<righeTabella;i++){
                if (GiacenzeaData_Tabella.getModel().getValueAt(i, 0).toString().equals(MonetaCercata)){
                    rigaTabellaMoneta=i;
                }
            }
            GiacenzeaData_Tabella.setRowSelectionInterval(rigaTabellaMoneta, rigaTabellaMoneta);
            GiacenzeaData_CompilaTabellaMovimenti();
            GiacenzeaData.requestFocus();
            GiacenzeaData_Tabella.requestFocus();
            //GiacenzeaData_TabellaDettaglioMovimenti.requestFocus();
        } catch (ParseException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
                    
        
    }//GEN-LAST:event_RW_Bottone_IdentificaScamActionPerformed

    private void RW_Bottone_ModificaVInizialeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Bottone_ModificaVInizialeActionPerformed
        // TODO add your handling code here:
        RW_Funzione_ModificaValore(0);
    }//GEN-LAST:event_RW_Bottone_ModificaVInizialeActionPerformed

    private void RW_Bottone_ModificaVFinaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Bottone_ModificaVFinaleActionPerformed
        // TODO add your handling code here:
        RW_Funzione_ModificaValore(1);

    }//GEN-LAST:event_RW_Bottone_ModificaVFinaleActionPerformed

    private void RW_CheckBox_VediSoloErroriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_CheckBox_VediSoloErroriActionPerformed
        // TODO add your handling code here:
        RW_CompilaTabellaDettagli();
    }//GEN-LAST:event_RW_CheckBox_VediSoloErroriActionPerformed

    private void Opzioni_Export_Tatax_BottoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Opzioni_Export_Tatax_BottoneActionPerformed
        // TODO add your handling code here:
        Opzioni_Export_Tatax();
    }//GEN-LAST:event_Opzioni_Export_Tatax_BottoneActionPerformed

    private void Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevantiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevantiActionPerformed
        // TODO add your handling code here:
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti.isSelected()) {
            //scrivo nelle Opzioni del DB che voglio il calcolo delle plus X Gruppo Wallet
            DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "SI");
        } else {
            //scrivo nelle Opzioni del DB che nel calcolo delle plus non considero la suddivisione per wallet
            DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023ScambiRilevanti", "NO");
        }
        //TabellaCryptodaAggiornare=true;
        //Adesso dovrei ricalcolare le plusvalenze ed aggiornare la tabella crypto
        Funzioni_AggiornaTutto();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
       // Calcoli_Plusvalenze.AggiornaPlusvalenze();
    }//GEN-LAST:event_Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevantiActionPerformed

    private void Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZeroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZeroActionPerformed
        // TODO add your handling code here:
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero.isSelected()) {
            //scrivo nelle Opzioni del DB che voglio il calcolo delle plus X Gruppo Wallet
            DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "SI");
        } else {
            //scrivo nelle Opzioni del DB che nel calcolo delle plus non considero la suddivisione per wallet
            DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_Pre2023EarnCostoZero", "NO");
        }
        //TabellaCryptodaAggiornare=true;
       // Calcoli_Plusvalenze.AggiornaPlusvalenze();
       Funzioni_AggiornaTutto();
       this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        //Adesso dovrei ricalcolare le plusvalenze ed aggiornare la tabella crypto
    }//GEN-LAST:event_Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZeroActionPerformed

    private void DepositiPrelievi_Bottone_CreaMovOppostoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DepositiPrelievi_Bottone_CreaMovOppostoActionPerformed
        // TODO add your handling code here:
        if (DepositiPrelievi_Tabella.getSelectedRow() >= 0) {
            MovimentoManuale_GUI a = new MovimentoManuale_GUI();
        int rigaselezionata = DepositiPrelievi_Tabella.getSelectedRow();        
        String IDTransazione = DepositiPrelievi_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            a.CompilaMovimentoOppostoID(IDTransazione);
            a.setLocationRelativeTo(this);
            a.setVisible(true);
        }else
        {
        MovimentoManuale_GUI a= new MovimentoManuale_GUI();
        a.setLocationRelativeTo(this);
        a.setVisible(true);
        }
                  //  Funzioni_AggiornaTutto();
            DepositiPrelievi_Caricatabella();
    }//GEN-LAST:event_DepositiPrelievi_Bottone_CreaMovOppostoActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        System.out.println("Chiusura");
        
        if(TransazioniCrypto_Bottone_Salva.isEnabled()){
                     String Messaggio="Attenzione, ci sono movimenti non salvati.\n"
                     + "Si vuole salvare prima di chiudere?";
             int risposta = JOptionPane.showOptionDialog(this, Messaggio, "Movimenti non salvati", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        Importazioni.Scrivi_Movimenti_Crypto(MappaCryptoWallet);
                    }

                } 
        }
  
    }//GEN-LAST:event_formWindowClosing

    private void GiacenzeaData_CheckBox_NascondiScamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiacenzeaData_CheckBox_NascondiScamActionPerformed
        // TODO add your handling code here:
                if(GiacenzeaData_Tabella.getRowCount()!=0)
            GiacenzeaData_CompilaTabellaToken();
    }//GEN-LAST:event_GiacenzeaData_CheckBox_NascondiScamActionPerformed

    private void RW_Opzioni_CheckBox_1RigoXOperazioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RW_Opzioni_CheckBox_1RigoXOperazioneActionPerformed
        // TODO add your handling code here:
                // TODO add your handling code here:
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (RW_Opzioni_CheckBox_1RigoXOperazione.isSelected()) {
            //scrivo nelle Opzioni del DB che voglio il calcolo delle plus X Gruppo Wallet
            DatabaseH2.Pers_Opzioni_Scrivi("RW_1RigoXOperazione", "SI");
        } else {
            //scrivo nelle Opzioni del DB che nel calcolo delle plus non considero la suddivisione per wallet
            DatabaseH2.Pers_Opzioni_Scrivi("RW_1RigoXOperazione", "NO");
        }
        //TabellaCryptodaAggiornare=true;
        //Adesso dovrei ricalcolare le plusvalenze ed aggiornare la tabella crypto
        Funzioni_AggiornaTutto();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
       // Calcoli_Plusvalenze.AggiornaPlusvalenze();
    }//GEN-LAST:event_RW_Opzioni_CheckBox_1RigoXOperazioneActionPerformed
    
    private void GiacenzeaData_Funzione_IdentificaComeScam() {
                //Recupero Address e Nome Moneta attuale tanto so già che se arrivo qua significa che i dati li ho
        if (GiacenzeaData_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
            String NomeMoneta = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            String Address = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2).toString();
            String Rete= GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1).toString();
            String Testo;
            //Come prima cosa controllo nella tabella del dettaglio se ho solo movimenti di deposito
            //solo in quel caso permetto di identificare il token come scam
            //il tipo movimento è il 4
            boolean SoloDepositi=true;
            for (int i=0;i<GiacenzeaData_TabellaDettaglioMovimenti.getRowCount();i++){
               String ID = GiacenzeaData_TabellaDettaglioMovimenti.getModel().getValueAt(i, 8).toString();
               if (!ID.split("_")[4].equalsIgnoreCase("DC"))SoloDepositi=false;
            }
            
            
            if (!Funzioni.isSCAM(NomeMoneta)){
                Testo = "<html>Vuoi identificare il Token <b>"+NomeMoneta+"</b> con Address <b>"+Address+"</b> come SCAM?<br><br>"
                                + "(Nelle varie funzioni del programma verrà data la possibilità di nascondere tali asset<br>"
                                + "e quando mostrati verranno identificati con un doppio asterisco (**) alla fine del nome)<br><br></html>";
                }
            else {
                Testo = "<html>Vuoi che il Token <b>"+NomeMoneta+"</b> non venga più considerato SCAM?<br><br>"
                + "</html>";
            }
                        Object[] Bottoni = {"Si", "No"};
                        int scelta = JOptionPane.showOptionDialog(this, Testo,
                                "Classificazione del movimento",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                Bottoni,
                                null);
                        if (scelta == 0 && !Funzioni.isSCAM(NomeMoneta)) {
                            if (SoloDepositi){//proseguo solo se il token ha solo movimenti di diposito
                            String nomi[]=DatabaseH2.RinominaToken_Leggi(Address+"_"+Rete);
                            //Se nomi[0] è null vuol dire che questo token non ha mai neanche subito una rinomina
                            //altrimenti vuol dire che è stato rinominato quindi prima di considerarlo come scam
                            //e aggiungergli gli asterisci recupero il suo nome originale
                            //gli asterischi gli aggiungo al nome orioginale del token e non al nome rinominato
                            if (nomi[0] == null)
                                {
                                DatabaseH2.RinominaToken_Scrivi(Address + "_" + Rete, NomeMoneta, NomeMoneta + " **");
                                //il comando sotto indica al database che quel token essendo scam non avrà mai prezzo
                                //ed eviterà verifiche sul prezzo e perdite di tempo in fase di calcolo
                             //   DatabaseH2.AddressSenzaPrezzo_Scrivi(Address + "_" + Rete, "9999999999");
                                GiacenzeaData_Tabella.getModel().setValueAt(NomeMoneta + " **", rigaselezionata, 0);
                                }
                            else
                                {
                                DatabaseH2.RinominaToken_Scrivi(Address + "_" + Rete, nomi[0], nomi[0] + " **");
                                //il comando sotto indica al database che quel token essendo scam non avrà mai prezzo
                                //ed eviterà verifiche sul prezzo e perdite di tempo in fase di calcolo
                              //  DatabaseH2.AddressSenzaPrezzo_Scrivi(Address + "_" + Rete, "9999999999");
                                GiacenzeaData_Tabella.getModel().setValueAt(nomi[0] + " **", rigaselezionata, 0);
                                }
                            //A questo punto devo cambiare il nome a tutti i token dello stesso tipo che trovo nelle transazioni
                            //Lancio la funzione rinomina token
                            TabellaCryptodaAggiornare = true;
                           }
                            else{
                                //Se ci sono altri movimenti emetto un messaggio di avviso
                                JOptionPane.showConfirmDialog(this, "<html>Attenzione! Possono essere considerati SCAM solo i token con solo movimenti di deposito<br>"
                                        + "L'operazione verrà annullata!<br></html>",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                                }
                        }
                        else if (scelta == 0 && Funzioni.isSCAM(NomeMoneta)) {
                            //Da gestire parte di descammizzazione
                            //Metto a zero il time nella tabella addressSenzaPrezzo in modo che la volta successiva il programma
                            //vada nuovamente a cercare il prezzo del token
                            //RinominaToken_LeggiTabellaDatabaseH2.AddressSenzaPrezzo_Scrivi(Address + "_" + Rete, "9999999999");
                            //leggo la riga per individuare il nome originale del token
                            String nomi[]=DatabaseH2.RinominaToken_Leggi(Address+"_"+Rete);
                            //lo scrivo nel database
                            DatabaseH2.RinominaToken_Scrivi(Address + "_" + Rete, nomi[0], nomi[0]);
                            GiacenzeaData_Tabella.getModel().setValueAt(nomi[0], rigaselezionata, 0);
                            //aggiorno l'intera tabella crypto
                            TabellaCryptodaAggiornare = true;
                         //   DatabaseH2.RinominaToken_Scrivi(Address+"_"+Rete, NomeMoneta,NomeMoneta+" **"); 
                        }
                                    NomeMoneta = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            if (Funzioni.isSCAM(NomeMoneta)) {
                GiacenzeaData_Bottone_Scam.setText("Rimuovi da SCAM");
            } else {
                GiacenzeaData_Bottone_Scam.setText("Identifica come SCAM");
            }
            }
    }
    
    private void GiacenzeaData_Funzione_CambiaNomeToken() {
        //Recupero Address e Nome Moneta attuale tanto so già che se arrivo qua significa che i dati li ho
        if (GiacenzeaData_Tabella.getSelectedRow() >= 0) {
            int rigaselezionata = GiacenzeaData_Tabella.getRowSorter().convertRowIndexToModel(GiacenzeaData_Tabella.getSelectedRow());
            String NomeMoneta = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            String Address = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 2).toString();
            String Rete = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 1).toString();
            String Testo;
            String nomi[] = DatabaseH2.RinominaToken_Leggi(Address + "_" + Rete);
            Testo = "<html>Indica il nuovo nome della Moneta<b>" + NomeMoneta + "</b> con Address <b>" + Address + "</b><br>";
            if (nomi[0] != null && !nomi[0].equalsIgnoreCase(NomeMoneta)) {
                Testo = Testo
                        + "Il nome Originale da prima importazione era : <b>" + nomi[0] + "</b><br>";
            }
            Testo = Testo + "<b>Attenzione :</b> I nomi dei token sono CaseSensitive quindi ad esempio BTC è diverso da Btc o btc<br><br></html>";
            String m = JOptionPane.showInputDialog(this, Testo, NomeMoneta).trim();
            if (m != null) {
                m = Funzioni.NormalizzaNome(m);//sostituisco le virgole con i punti per la separazione corretta dei decimali
                //Se il nome toke è di almeno 3 caratteri allora proseguo
                if (m.length() > 2) {
                    if (nomi[0] == null) {
                        DatabaseH2.RinominaToken_Scrivi(Address + "_" + Rete, NomeMoneta, m);
                        GiacenzeaData_Tabella.getModel().setValueAt(m, rigaselezionata, 0);
                    } else {
                        DatabaseH2.RinominaToken_Scrivi(Address + "_" + Rete, nomi[0], m);
                        GiacenzeaData_Tabella.getModel().setValueAt(m, rigaselezionata, 0);
                    }
                    TabellaCryptodaAggiornare = true;
                } else {
                    JOptionPane.showConfirmDialog(this, "Attenzione, " + m + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
            }
            NomeMoneta = GiacenzeaData_Tabella.getModel().getValueAt(rigaselezionata, 0).toString();
            if (Funzioni.isSCAM(NomeMoneta)) {
                GiacenzeaData_Bottone_Scam.setText("Rimuovi da SCAM");
            } else {
                GiacenzeaData_Bottone_Scam.setText("Identifica come SCAM");
            }
        }

    }
    
    public static String generateSignature(String data, String apiSecret) {
 /*       try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] encodedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(encodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }*/
 return null;
    }
    
    
        private void Opzioni_Export_Tatax(){


        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
try {
            // TODO add your handling code here:

            //il modo più rapido per esportare la tabella è prendere tutta la mappa ed esportare solo quello che c'è tra le date e che ha
            //nel suo contenuto il filtro in basso
                    JFileChooser fc = new JFileChooser();

        //In response to a button click:
        //fc.showSaveDialog(Opzioni);
       // System.out.println(CDC_FiatWallet_Pannello);
        int returnVal = fc.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {           
            FileWriter w = new FileWriter(fc.getSelectedFile()+".csv");
            FileWriter wDEFI = new FileWriter(fc.getSelectedFile()+"_DEFI.csv");
            //File export = new File("temp.csv");
            //FileWriter w = new FileWriter(export);
            BufferedWriter b = new BufferedWriter(w);
            BufferedWriter bDEFI = new BufferedWriter(wDEFI);
            b.write("\"Symbol\",\"TokenAddress\",\"TimeStamp\",\"MovementType\",\"Quantity\",\"Countervalue\",\"SymbolCountervalue\",\"UserCountervalue\",\"UserSymbolCountervalue\",\"SourceCountervalue\",\"SourceSymbolCountervalue\"\n");
            bDEFI.write("\"Symbol\",\"TokenAddress\",\"TimeStamp\",\"MovementType\",\"Quantity\",\"Countervalue\",\"SymbolCountervalue\",\"UserCountervalue\",\"UserSymbolCountervalue\",\"SourceCountervalue\",\"SourceSymbolCountervalue\"\n");            
            String Wallet = Opzioni_Export_Wallets_Combobox.getSelectedItem().toString().trim();
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaID(movimento[0]);
                    String IDTS[] = movimento[0].split("_");
                    String secondi=IDTS[0].substring(12, 14);
                    String DataMovimento = movimento[1]+":"+secondi;
                    String TokenU=movimento[8];
                    String TokenE=movimento[11];
                    String Prezzo="";
                    String Simbolo="";
                    boolean haprezzo=false;
                    if (movimento[32].equalsIgnoreCase("SI"))haprezzo=true;
                    boolean EstraiPrezzi=this.Opzioni_Export_EsportaPrezzi_CheckBox.isSelected();
                    if (haprezzo&&EstraiPrezzi)
                    {
                        Prezzo=movimento[15];
                        Simbolo="EUR";
                        }
                    //Ora tolgo le parentesi dai nomi dei token
                    TokenE=TokenE.split("\\(")[0].trim();
                    TokenU=TokenU.split("\\(")[0].trim();
                        // adesso verifico il wallet
                        String gruppoWallet="";
                        if (Wallet.contains("Gruppo :"))gruppoWallet=Wallet.split(" : ")[1].trim();
                        if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                ||Wallet.equalsIgnoreCase(movimento[3].trim())//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3]).equals(gruppoWallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                                ) 
                        {
                            
                            if (IDTS[4].equals("VC")
                                    || IDTS[4].equals("SC")
                                    || IDTS[4].equals("AC")) {
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        "DEBIT\",\""+movimento[10]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa);
                                
                                
                                Stringa="";
                                Stringa =Stringa+"\""+TokenE+"\",\""+movimento[28]+"\",\""+
                                DataMovimento+"\",\""+
                                        "CREDIT\",\""+movimento[13]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                //Prima di scrivere il movimento verifico se è un movimento della defi lo salvo nel file a parte
                                //altrimenti lo metto nel file principale
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa);
                            }
                            else if (IDTS[4].equals("CM")) {
                                String TipoCommissione="EXCHANGE_FEE";
                                if (Rete!=null)TipoCommissione="BLOCKCHAIN_FEE";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        TipoCommissione+"\",\""+movimento[10]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                            }
                            else if (IDTS[4].equals("RW")&&!movimento[13].isBlank()) {
                                String Tipo="EARN";
                                if (movimento[5].equalsIgnoreCase("CASHBACK"))Tipo="CASHBACK";
                                else if (movimento[5].equalsIgnoreCase("STAKING REWARDS"))Tipo="STAKING";
                                if (movimento[5].equalsIgnoreCase("AIRDROP"))Tipo="AIRDROP";
                                if (movimento[5].equalsIgnoreCase("EARN"))Tipo="EARN";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenE+"\",\""+movimento[28]+"\",\""+
                                        DataMovimento+"\",\""+
                                        Tipo+"\",\""+movimento[13]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                            }
                            else if (IDTS[4].equals("DC") || IDTS[4].equals("DF")) {
                                //Siccome i token scam hanno solamente depositi metto qua la verifica se il token è scam e se lo è non lo esporto
                                if (!Funzioni.isSCAM(TokenE)) {
                                    String Stringa = "";
                                    if (movimento[18].contains("DAI")) {
                                        String Tipo = "EARN";
                                        if (movimento[5].equalsIgnoreCase("CASHBACK")) {
                                            Tipo = "CASHBACK";
                                        } else if (movimento[5].equalsIgnoreCase("STAKING REWARDS")) {
                                            Tipo = "STAKING";
                                        }
                                        if (movimento[5].equalsIgnoreCase("AIRDROP")) {
                                            Tipo = "AIRDROP";
                                        }
                                        if (movimento[5].equalsIgnoreCase("EARN")) {
                                            Tipo = "EARN";
                                        }
                                        Stringa = Stringa + "\"" + TokenE + "\",\"" + movimento[28] + "\",\""
                                                + DataMovimento + "\",\""
                                                + Tipo + "\",\"" + movimento[13] +"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                    } else {
                                        String Tipo = "DEPOSIT";

                                        Stringa = Stringa + "\"" + TokenE + "\",\"" + movimento[28] + "\",\""
                                                + DataMovimento + "\",\""
                                                + Tipo + "\",\"" + movimento[13] +"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                    }
                                    if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi") || movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio")) {
                                        bDEFI.append(Stringa);
                                    } else {
                                        b.append(Stringa);
                                    }
                                }
                            }
                            else if (IDTS[4].equals("PC")||IDTS[4].equals("PF")||(IDTS[4].equals("RW")&&movimento[13].isBlank())) {
                                
                                if(movimento[5].equalsIgnoreCase("COMMISSIONI")){
                                String TipoCommissione="EXCHANGE_FEE";
                                if (Rete!=null)TipoCommissione="BLOCKCHAIN_FEE";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        TipoCommissione+"\",\""+movimento[10]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                                }else{
                                String Tipo="WITHDRAWAL";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        Tipo+"\",\""+movimento[10]+"\",\"\",\"\","+"\"\""+","+"\"\""+",\""+Prezzo+"\",\""+Simbolo+"\"\n";
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    //System.out.println(movimento[4]);
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                                }
                            }
                            else if (IDTS[4].equals("TI")) {
                                //TI non fà nulla
                            }
                            else {
                                System.out.println("Movimento "+movimento[0]+" sconosciuto e non esportato");
                            }

                        }
                    
                }
            bDEFI.close();
            wDEFI.close();
            b.close();
            w.close();
            //File a=fc.getSelectedFile();
            JOptionPane.showConfirmDialog(null, "<html><b>Elaborazione Terminata</b><br>"
                    + "File Salvato in "+fc.getSelectedFile().getAbsolutePath(),
                            "Fine Esportazione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
          //  Desktop desktop = Desktop.getDesktop();  
         //   desktop.open(export);
} 
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        }
    
      private void Opzioni_Export_Tatax_OLD(){


        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
try {
            // TODO add your handling code here:

            //il modo più rapido per esportare la tabella è prendere tutta la mappa ed esportare solo quello che c'è tra le date e che ha
            //nel suo contenuto il filtro in basso
                    JFileChooser fc = new JFileChooser();

        //In response to a button click:
        //fc.showSaveDialog(Opzioni);
       // System.out.println(CDC_FiatWallet_Pannello);
        int returnVal = fc.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {           
            FileWriter w = new FileWriter(fc.getSelectedFile()+".csv");
            FileWriter wDEFI = new FileWriter(fc.getSelectedFile()+"_DEFI.csv");
            //File export = new File("temp.csv");
            //FileWriter w = new FileWriter(export);
            BufferedWriter b = new BufferedWriter(w);
            BufferedWriter bDEFI = new BufferedWriter(wDEFI);
            b.write("\"Symbol\",\"TokenAddress\",\"TimeStamp\",\"MovementType\",\"Quantity\",\"Countervalue\",\"SymbolCountervalue\",\"UserCountervalue\",\"UserSymbolCountervalue\",\"SourceCountervalue\",\"SourceSymbolCountervalue\"\n");
            bDEFI.write("\"Symbol\",\"TokenAddress\",\"TimeStamp\",\"MovementType\",\"Quantity\",\"Countervalue\",\"SymbolCountervalue\",\"UserCountervalue\",\"UserSymbolCountervalue\",\"SourceCountervalue\",\"SourceSymbolCountervalue\"\n");            
            String Wallet = Opzioni_Export_Wallets_Combobox.getSelectedItem().toString().trim();
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaID(movimento[0]);
                    String IDTS[] = movimento[0].split("_");
                    String secondi=IDTS[0].substring(12, 14);
                    String DataMovimento = movimento[1]+":"+secondi;
                    String TokenU=movimento[8];
                    String TokenE=movimento[11];
                    String Prezzo="";
                    String Simbolo="";
                    boolean haprezzo=false;
                    if (movimento[32].equalsIgnoreCase("SI"))haprezzo=true;
                    boolean EstraiPrezzi=this.Opzioni_Export_EsportaPrezzi_CheckBox.isSelected();
                    if (haprezzo&&EstraiPrezzi)
                    {
                        Prezzo=movimento[15];
                        Simbolo="EUR";
                        }
                    //Ora tolgo le parentesi dai nomi dei token
                    TokenE=TokenE.split("\\(")[0].trim();
                    TokenU=TokenU.split("\\(")[0].trim();
                        // adesso verifico il wallet
                        String gruppoWallet="";
                        if (Wallet.contains("Gruppo :"))gruppoWallet=Wallet.split(" : ")[1].trim();
                        if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                ||Wallet.equalsIgnoreCase(movimento[3].trim())//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3]).equals(gruppoWallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                                ) 
                        {
                            
                            if (IDTS[4].equals("VC")
                                    || IDTS[4].equals("SC")
                                    || IDTS[4].equals("AC")) {
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        "DEBIT\",\""+movimento[10]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n";
                                
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa);
                                
                                
                                Stringa="";
                                Stringa =Stringa+"\""+TokenE+"\",\""+movimento[28]+"\",\""+
                                DataMovimento+"\",\""+
                                        "CREDIT\",\""+movimento[13]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n";
                                //Prima di scrivere il movimento verifico se è un movimento della defi lo salvo nel file a parte
                                //altrimenti lo metto nel file principale
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa);
                            }
                            else if (IDTS[4].equals("CM")) {
                                String TipoCommissione="EXCHANGE_FEE";
                                if (Rete!=null)TipoCommissione="BLOCKCHAIN_FEE";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        TipoCommissione+"\",\""+movimento[10]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                            }
                            else if (IDTS[4].equals("RW")&&!movimento[13].isBlank()) {
                                String Tipo="EARN";
                                if (movimento[5].equalsIgnoreCase("CASHBACK"))Tipo="CASHBACK";
                                else if (movimento[5].equalsIgnoreCase("STAKING REWARDS"))Tipo="STAKING";
                                if (movimento[5].equalsIgnoreCase("AIRDROP"))Tipo="AIRDROP";
                                if (movimento[5].equalsIgnoreCase("EARN"))Tipo="EARN";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenE+"\",\""+movimento[28]+"\",\""+
                                        DataMovimento+"\",\""+
                                        Tipo+"\",\""+movimento[13]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                            }
                            else if (IDTS[4].equals("DC") || IDTS[4].equals("DF")) {
                                //Siccome i token scam hanno solamente depositi metto qua la verifica se il token è scam e se lo è non lo esporto
                                if (!Funzioni.isSCAM(TokenE)) {
                                    String Stringa = "";
                                    if (movimento[18].contains("DAI")) {
                                        String Tipo = "EARN";
                                        if (movimento[5].equalsIgnoreCase("CASHBACK")) {
                                            Tipo = "CASHBACK";
                                        } else if (movimento[5].equalsIgnoreCase("STAKING REWARDS")) {
                                            Tipo = "STAKING";
                                        }
                                        if (movimento[5].equalsIgnoreCase("AIRDROP")) {
                                            Tipo = "AIRDROP";
                                        }
                                        if (movimento[5].equalsIgnoreCase("EARN")) {
                                            Tipo = "EARN";
                                        }
                                        Stringa = Stringa + "\"" + TokenE + "\",\"" + movimento[28] + "\",\""
                                                + DataMovimento + "\",\""
                                                + Tipo + "\",\"" + movimento[13] +"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n";
                                    } else {
                                        String Tipo = "DEPOSIT";

                                        Stringa = Stringa + "\"" + TokenE + "\",\"" + movimento[28] + "\",\""
                                                + DataMovimento + "\",\""
                                                + Tipo + "\",\"" + movimento[13] +"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n";
                                    }
                                    if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi") || movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio")) {
                                        bDEFI.append(Stringa);
                                    } else {
                                        b.append(Stringa);
                                    }
                                }
                            }
                            else if (IDTS[4].equals("PC")||IDTS[4].equals("PF")||(IDTS[4].equals("RW")&&movimento[13].isBlank())) {
                                
                                if(movimento[5].equalsIgnoreCase("COMMISSIONI")){
                                String TipoCommissione="EXCHANGE_FEE";
                                if (Rete!=null)TipoCommissione="BLOCKCHAIN_FEE";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        TipoCommissione+"\",\""+movimento[10]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                                }else{
                                String Tipo="WITHDRAWAL";
                                String Stringa="";
                                Stringa =Stringa+"\""+TokenU+"\",\""+movimento[26]+"\",\""+
                                        DataMovimento+"\",\""+
                                        Tipo+"\",\""+movimento[10]+"\",\"\",\"\","+Prezzo+","+Simbolo+",\"\",\"\"\n"; 
                                if (movimento[4].trim().equalsIgnoreCase("Piattaforma/defi")||movimento[4].trim().equalsIgnoreCase("Piattaforma di scambio"))
                                    {
                                    //System.out.println(movimento[4]);
                                    bDEFI.append(Stringa);   
                                    }
                                else b.append(Stringa); 
                                }
                            }
                            else if (IDTS[4].equals("TI")) {
                                //TI non fà nulla
                            }
                            else {
                                System.out.println("Movimento "+movimento[0]+" sconosciuto e non esportato");
                            }

                        }
                    
                }
            bDEFI.close();
            wDEFI.close();
            b.close();
            w.close();
            //File a=fc.getSelectedFile();
            JOptionPane.showConfirmDialog(null, "<html><b>Elaborazione Terminata</b><br>"
                    + "File Salvato in "+fc.getSelectedFile().getAbsolutePath(),
                            "Fine Esportazione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
          //  Desktop desktop = Desktop.getDesktop();  
         //   desktop.open(export);
} 
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        }
      
      
    private void GiacenzeaData_CompilaTabellaToken(){
        
        //Gestisco i bottoni
        GiacenzeaData_Bottone_RettificaQta.setEnabled(false);

            
        //FASE 1 PULIZIA TABELLA
        //questo serve per evitare errori di sorting nel thread
        //azzera il sort ogni volta in sostanza ed evita errori
        DefaultTableModel GiacenzeaData_ModelloTabellaDettagli = (DefaultTableModel) this.GiacenzeaData_TabellaDettaglioMovimenti.getModel(); 
        Funzioni_Tabelle_PulisciTabella(GiacenzeaData_ModelloTabellaDettagli);
        
        DefaultTableModel GiacenzeaData_ModelloTabella = (DefaultTableModel) this.GiacenzeaData_Tabella.getModel();
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(GiacenzeaData_Tabella.getModel());
        GiacenzeaData_Tabella.setRowSorter(sorter);
        Funzioni_Tabelle_PulisciTabella(GiacenzeaData_ModelloTabella); 
       /* sorter = new TableRowSorter<>(GiacenzeaData_Tabella.getModel());
        GiacenzeaData_Tabella.setRowSorter(sorter);*/
        //Fase 2 Preparazione thead
        Download progress = new Download();
        progress.setLocationRelativeTo(this);

        Thread thread;
        thread = new Thread() {
            public void run() {

        //Compilo la mappa QtaCrypto con la somma dei movimenti divisa per crypto
        //in futuro dovrò mettere anche un limite per data e un limite per wallet
        progress.Titolo("Calcolo Giazenze e  Prezzi in corso....");
        progress.SetLabel("Calcolo Giazenze e  Prezzi in corso....");
        long DataRiferimento=0;
        //FASE 1 THREAD : RECUPERO LA DATA DI RIFERIMENTO
        if (GiacenzeaData_Data_DataChooser.getDate()!=null){
            //DA FARE : IMPEDIRE DI METTERE DATE FUTURE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            String Data=f.format(GiacenzeaData_Data_DataChooser.getDate());
            DataRiferimento=OperazioniSuDate.ConvertiDatainLong(Data)+86400000;
           // DataRiferimento=OperazioniSuDate.ConvertiDatainLong(Data)+86399000;
            //DataRiferimento=OperazioniSuDate.ConvertiDatainLong(Data)+86340000;//Riferito alle 23:59 del giorno
            long DatadiOggi = System.currentTimeMillis();
            if (DatadiOggi<DataRiferimento) DataRiferimento=DatadiOggi;
        } 
        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
        Map<String, Moneta> QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto il bigdecimal con la qta

        String Wallet = GiacenzeaData_Wallet_ComboBox.getSelectedItem().toString().trim();
        String SottoWallet = GiacenzeaData_Wallet2_ComboBox.getSelectedItem().toString().trim();
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaID(movimento[0]);
                    long DataMovimento = OperazioniSuDate.ConvertiDatainLong(movimento[1]);
                    if (DataMovimento < DataRiferimento) {
                        // adesso verifico il wallet
                        String gruppoWallet="";
                        if (Wallet.contains("Gruppo :"))gruppoWallet=Wallet.split(" : ")[1].trim();
                        if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                || (Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase("tutti"))//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||(Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase(movimento[4].trim()))//Se wallet e sottowallet corrispondono a quelli analizzati proseguo
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3]).equals(gruppoWallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                                ) {
                            // GiacenzeaData_Wallet_ComboBox.getSelectedItem()
                            //Faccio la somma dei movimenti in usicta
                            Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                            //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                            Monete[0] = new Moneta();
                            Monete[1] = new Moneta();
                            Monete[0].MonetaAddress = movimento[26];
                            Monete[1].MonetaAddress = movimento[28];
                            //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
                            Monete[0].Moneta = movimento[8];
                            Monete[0].Tipo = movimento[9];
                            Monete[0].Qta = movimento[10];
                            Monete[0].Rete=Rete;
                            Monete[1].Moneta = movimento[11];
                            Monete[1].Tipo = movimento[12];
                            Monete[1].Qta = movimento[13];
                            Monete[1].Rete=Rete;
                            //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                            for (int a = 0; a < 2; a++) {
                                //ANALIZZO MOVIMENTI
                                if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress)!=null) {
                                    //Movimento già presente da implementare
                                    Moneta M1 = QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress);
                                    M1.Qta = new BigDecimal(M1.Qta)
                                            .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                                } else if (!Monete[a].Moneta.isBlank()) {
                                    //Movimento Nuovo da inserire
                                    Moneta M1 = new Moneta();
                                    M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                    M1.Rete = Rete;
                                    QtaCrypto.put(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress, M1);

                                }
                            }
                        }
                    }
                }
        
        //Adesso elenco tutte le monete e le metto in tabella
        progress.SetMassimo(QtaCrypto.size());       
        DefaultTableModel GiacenzeaData_ModelloTabella = (DefaultTableModel) GiacenzeaData_Tabella.getModel();
        Funzioni_Tabelle_PulisciTabella(GiacenzeaData_ModelloTabella);
        int i=0;
        BigDecimal TotEuro=new BigDecimal(0);
        for (String moneta :QtaCrypto.keySet()){
            i++;
            if (progress.FineThread())
                {
                    
                    Funzioni_Tabelle_PulisciTabella(GiacenzeaData_ModelloTabella);                    
                    TableRowSorter<TableModel> sorter = new TableRowSorter<>(GiacenzeaData_Tabella.getModel());
                    GiacenzeaData_Tabella.setRowSorter(sorter);
                    GiacenzeaData_Totali_TextField.setText("");
                    JOptionPane.showConfirmDialog(progress, "Elaborazione Interrotta!",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                    progress.ChiudiFinestra();
                try {
                    this.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }
                }
            if (i<=progress.Massimo) progress.SetAvanzamento(i);
            Moneta M1=QtaCrypto.get(moneta);
            String Rete=M1.Rete;
            String Address=M1.MonetaAddress;
            String riga[]=new String[6];
            riga[0]=M1.Moneta;
            riga[2]=Address;//qui ci va l'address della moneta se non sto analizzando i wallet nel complesso
            riga[3]=M1.Tipo;
            riga[4]=M1.Qta;
            riga[1]=M1.Rete;
            if ((GiacenzeaData_CheckBox_MostraQtaZero.isSelected()||!M1.Qta.equals("0"))
                    &&(!GiacenzeaData_CheckBox_NascondiScam.isSelected()||!Funzioni.isSCAM(M1.Moneta))
                    ){
                if (M1.Qta.equals("0"))riga[5]="0.00";
                else riga[5]=Prezzi.DammiPrezzoTransazione(M1,null,DataRiferimento, null,true,2,Rete);
                if (riga[4].contains("-")&&!riga[5].equals("0.00"))riga[5]="-"+riga[5];
                GiacenzeaData_ModelloTabella.addRow(riga);
                TotEuro=TotEuro.add(new BigDecimal(riga[5]));
                GiacenzeaData_Totali_TextField.setText(TotEuro.toString());                
            }
            
        }
        Tabelle.ColoraRigheTabella0GiacenzeaData(GiacenzeaData_Tabella);
        Giacenzeadata_Walleta_Label.setText(Wallet);
        Giacenzeadata_Walletb_Label.setText(SottoWallet);
        progress.ChiudiFinestra();
        
                        }
            };
        
        thread.start();
        progress.setVisible(true);}
    
    private void GiacenzeaData_Funzione_AggiornaComboBoxWallet() {
       // int Selezionata=GiacenzeaData_Wallet_ComboBox.getSelectedIndex();
        String VecchioValore=GiacenzeaData_Wallet_ComboBox.getSelectedItem().toString();
        Map<String, String> MappaGruppiWalletUtilizzati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        boolean VecchioTrovato=false;
       // int Selezionata2=GiacenzeaData_Wallet2_ComboBox.getSelectedIndex();
        GiacenzeaData_Wallet_ComboBox.removeAllItems();
        Opzioni_Export_Wallets_Combobox.removeAllItems();
        GiacenzeaData_Wallet_ComboBox.addItem("Tutti");
        Opzioni_Export_Wallets_Combobox.addItem("Tutti");
       /* Mappa_Wallet.clear();
        for (String[] v : MappaCryptoWallet.values()) {
            Funzione_AggiornaMappaWallets(v);
        }*/
        for (String v : Mappa_Wallet.keySet()) {
            this.GiacenzeaData_Wallet_ComboBox.addItem(v);
            Opzioni_Export_Wallets_Combobox.addItem(v);
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v);
            MappaGruppiWalletUtilizzati.put(GruppoWallet, GruppoWallet);
            if (v.equals(VecchioValore)) {
                VecchioTrovato=true;
            }
        }
        for (String v : MappaGruppiWalletUtilizzati.keySet()) {
            String nome="Gruppo : "+v;
            this.GiacenzeaData_Wallet_ComboBox.addItem(nome);
            Opzioni_Export_Wallets_Combobox.addItem(nome);
            if (nome.equals(VecchioValore)) {
                VecchioTrovato=true;
            }
        }
        if (VecchioTrovato)
            GiacenzeaData_Wallet_ComboBox.setSelectedItem(VecchioValore);
        GiacenzeaData_Funzione_AggiornaComboBoxWallet2();
    }
     private void GiacenzeaData_Funzione_AggiornaComboBoxWallet2() {
         String VecchioValore=GiacenzeaData_Wallet2_ComboBox.getSelectedItem().toString();
         boolean VecchioTrovato=false;
           List<String> Lista=Mappa_Wallets_e_Dettagli.get(GiacenzeaData_Wallet_ComboBox.getSelectedItem().toString());
           GiacenzeaData_Wallet2_ComboBox.removeAllItems();
           GiacenzeaData_Wallet2_ComboBox.addItem("Tutti");
           if (Lista!=null){
               for (String SottoWallet:Lista){
                   GiacenzeaData_Wallet2_ComboBox.addItem(SottoWallet);
               if (SottoWallet.equals(VecchioValore)) {
                VecchioTrovato=true;
            }
               }
                       if (VecchioTrovato)
            GiacenzeaData_Wallet2_ComboBox.setSelectedItem(VecchioValore);
           }
     }
/*    public void TransazioniCrypto_Funzioni_AggiornaDefi(List<String> Portafogli,String apiKey) {
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
           
            Plusvalenze.AggiornaPlusvalenze();
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
    }*/
            
    
    
    
/*    public void TransazioniCrypto_Funzioni_PulisciMovimentiAssociatinonEsistenti(){
        //questa funziona va lanciata ad ogni fine importazione per verificare non vi siano modifiche
        //su movimenti già associati
        for (String[] v : MappaCryptoWallet.values()) {
          if (v[18]!=null&&(v[18].contains("DTW")||v[18].contains("PTW")))
          {
              String ID=v[0];
              String riferimento=v[20];   
              //FUNZIONE COMPLETAMENTE DA RIVEDERE, SERVE PER TROVARE MOVIMENTI ASSOCIATI NON PIU TALI E CANCELLARE LE ANOMALIE
              //QUESTO PUò SUCCEDERE SE SI UTILIZZA LA FUNZIONE SOVRASCRIVI IN FASE DI IMPORTAZIONE DATI
              //IN SOSTANZA SE NON TROVO TUTTI I RIFERIMENTI SU TUTTI I MOVIMENTI ASSOCIATI DEVO RIPORTARE IL TUTTO
              //ALLA CONDIZIONE INIZIALE
   /*           String movimentiOpposti[]=MappaCryptoWallet.get(riferimento)[20].split(",");
              for (String IDmovimentoOpposto:movimentiOpposti){
              //String movimentoOpposto
              String movimentoOpposto[]=MappaCryptoWallet.get(IDmovimentoOpposto);
              
              
              if (movimentoOpposto==null || !movimentoOpposto[20].equalsIgnoreCase(ID)){
                //se il movimento opposto non esiste oppure se sul movimento opposto non trovo l'id di questo movimento allora pulisco le righe
                if (v[18].contains("DTW"))v[5]="DEPOSITO CRYPTO"; else v[5]="PRELIEVO CRYPTO";
                v[18]="";
                }
                  
              }
            
            }
          }
    }*/
    

  
    
    public String RitornaReteDefi(String ID) {
        String Transazione[]=MappaCryptoWallet.get(ID);
        String Wallet=Transazione[3].trim();
        String appoggio[]=Wallet.split(" ");
        String Rete="";
        // Se soddisfa le seguenti condizioni significa che ho trovato un wallet in defi e posso tornare il nome della Rete DEFI
        // Quindi restituisco il nome della rete oltre le condizioni principali solo se hop la transaction hash
        if (appoggio.length==2&&appoggio[1].contains("(")&&
                appoggio[1].contains(")")&&
                appoggio[0].contains("0x")&&
                ID.split("_")[1].startsWith("BC.")){
            Rete=ID.split("_")[1].split("\\.")[1];
        }
        return Rete;
    }
    
    
        public void TransazioniCrypto_CompilaTextPaneDatiMovimento() {
        if (TransazioniCryptoTabella.getSelectedRow()>=0){
            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) this.TransazioniCrypto_Tabella_Dettagli.getModel();
            Funzioni_Tabelle_PulisciTabella(ModelloTabellaCrypto);
            
            
            
            
        int rigaselezionata = TransazioniCryptoTabella.getRowSorter().convertRowIndexToModel(TransazioniCryptoTabella.getSelectedRow());
        String IDTransazione = TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
        
        //come prima cosa mi occupo del pulsante defi, deve essere attivo se abbiamo movimenti in defi e disattivo in caso contrario 
        //per controllare verifico di avere il transaction hash e il nome della rete quindi
        String Transazione[]=MappaCryptoWallet.get(IDTransazione);
        String ReteDefi=RitornaReteDefi(IDTransazione);
        String THash=Transazione[24];
            if(!THash.isEmpty()&&!ReteDefi.isEmpty()){
                this.TransazioniCrypto_Bottone_DettaglioDefi.setEnabled(true);
            }else{
                this.TransazioniCrypto_Bottone_DettaglioDefi.setEnabled(false);
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
        
        
        Tabelle.ColoraTabellaSemplice(TransazioniCrypto_Tabella_Dettagli);
        Tabelle.updateRowHeights(TransazioniCrypto_Tabella_Dettagli);
}
    }
    
    
    
       private void PulisciTabella(JTable T){
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) T.getModel();
            Funzioni_Tabelle_PulisciTabella(ModelloTabellaCrypto);
       }
    
    
    private void TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile(boolean EscludiTI) throws IOException {

                try {                    
             /*       File file = new File(CryptoWallet_FileDB);
                    LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
                    lineNumberReader.skip(Long.MAX_VALUE);
                    int lines = lineNumberReader.getLineNumber();
                    lineNumberReader.close();*/
                  

                    String fileDaImportare = CryptoWallet_FileDB;
                    MappaCryptoWallet.clear();
                    Mappa_Wallet.clear();

                    //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
                    //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
                    //ad esempio questo succede per i dust conversion etc....
                    File TransazioniCrypto1 = new File(fileDaImportare);
                    if (!TransazioniCrypto1.exists()) {
                        TransazioniCrypto1.createNewFile();
                    }
                    String riga;
                    Mappa_Wallet.clear();
                    Mappa_Wallets_e_Dettagli.clear();
                    try (FileReader fire = new FileReader(fileDaImportare); BufferedReader bure = new BufferedReader(fire);) {
                        while ((riga = bure.readLine()) != null) {
                            String splittata1[] = riga.split(";");
                            String splittata[] = new String[ColonneTabella];
                            System.arraycopy(splittata1, 0, splittata, 0, splittata1.length);
                            //questo serve affinchè ogni movimento abbia sempre un numero di colonne pari a ColonneTabella
                            //serve affinchè possa incrementare a piacimento il numero di colonne senza avere problemi poi
                            //----------------------------------------------------------------------------------------
                            //Adesso faccio in modo che che i sottowallet CRO Transaction, BSC transaction etc.... vengano convertiti in
                            if (VersioneCambiata){
                            if (Funzioni.TrovaReteDaID(splittata[0]) != null && !Funzioni.TrovaReteDaID(splittata[0]).isBlank()) {
                                if (splittata[4].split(" ").length > 1 && splittata[4].contains("Transaction")) {
                                    splittata[4] = "Wallet";
                                }
                                if (splittata[4].equals("PIATTAFORMA DI SCAMBIO")) {
                                    splittata[4] = "Piattaforma di scambio";
                                }
                                if (splittata[4].equals("PIATTAFORMA/VAULT")) {
                                    splittata[4] = "Piattaforma/DeFi";
                                }
                            }
                            //questo serve solo per eliminare i null che erano finiti per sbaglio
                            //dopo un errore di programmazione
                            
                            
                                //Queste cose le faccio solo se mi accorcgo che la versione del software è cambiata
                            //Direi che si può tranquillamente togliere tra qualche versione, mettiamo ad esempio dalla 1.15
                            for (int kj = 0; kj < splittata.length; kj++) {
                                //questo invece inizializza tutti i campi nulli a campo vuoto per non avere problemi con gli if futuri
                                if (splittata[kj] == null || splittata[kj].equals("null")) {
                                    splittata[kj] = "";
                                }
                            }

                            //Adesso verifico se ho prezzi a zero non perchè valgano zero ma perchè non è presente un prezzo sul movimento e li segnalo
                            //col 32 a SI se il movimento è senza prezzo invece a NO se ha prezzo
                            Prezzi.IndicaMovimentoPrezzato(splittata);
                            }
                            MappaCryptoWallet.put(splittata[0], splittata);

                        }

                    } catch (IOException ex) {
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } catch (IOException ex) {
                    Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
                }


        Calcoli_Plusvalenze.AggiornaPlusvalenze();
        
        if (VersioneCambiata){
            //Se c'è un cambio versione può essere che vi sia anche una modifica del file
            //per questo salverei una copia di backup del vecchio file e ne creerei uno nuovo con le modifiche

            Importazioni.Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        }
        
        this.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(EscludiTI);
        /*    this.TransazioniCrypto_Text_Plusvalenza.setText("€ "+Plusvalenza.toPlainString());
        Color verde=new Color (45, 155, 103);
        Color rosso=new Color(166,16,34);
        if (!TransazioniCrypto_Text_Plusvalenza.getText().contains("-"))TransazioniCrypto_Text_Plusvalenza.setForeground(verde);else TransazioniCrypto_Text_Plusvalenza.setForeground(rosso);
        this.Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
        GiacenzeaData_Funzione_AggiornaComboBoxWallet();*/
    } 
    
       
       
    private void TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(boolean EscludiTI) {
        Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, "", 999);
        PulisciTabella(TransazioniCrypto_Tabella_Dettagli);
        Mappa_Wallet.clear();

        //da verificare se va bene, serve per evitare problemi di sorting nel caso in cui la richiesta arrivi da un thread
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(TransazioniCryptoTabella.getModel());
        TransazioniCryptoTabella.setRowSorter(sorter);

        Map<String, String> Mappa_NomiTokenPersonalizzati = DatabaseH2.RinominaToken_LeggiTabella();

        DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) TransazioniCryptoTabella.getModel();
        Funzioni_Tabelle_PulisciTabella(ModelloTabellaCrypto);
        BigDecimal Plusvalenza = new BigDecimal("0");
        Tabelle.ColoraRigheTabellaCrypto(TransazioniCryptoTabella);
        Mappa_Wallet.clear();
        Mappa_Wallets_e_Dettagli.clear();
        for (String[] v : MappaCryptoWallet.values()) {
            Funzione_AggiornaMappaWallets(v);

            //questo rinomina i token con nomi personalizzati
            //Solo in caso di defi
            String Rete = Funzioni.TrovaReteDaID(v[0]);
            String AddressU = v[26];
            String AddressE = v[28];
            if (!Funzioni.noData(Rete)) {
                if (!Funzioni.noData(AddressU)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressU + "_" + Rete);
                    if (valore != null) {
                        v[8] = valore;
                    }
                }
                if (!Funzioni.noData(AddressE)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressE + "_" + Rete);
                    if (valore != null) {
                        v[11] = valore;
                    }

                }
            }
            //Questo indica nella colonna 32 se il movimento è provvisto o meno di prezzo.
            Prezzi.IndicaMovimentoPrezzato(v);

            //questo scrive i dati sulla mappa ed esclude i trasferimenti esterni se specificato
            if (EscludiTI == true && !v[5].trim().equalsIgnoreCase("Trasferimento Interno") || EscludiTI == false) {
                if (Funzioni_Date_ConvertiDatainLong(v[1]) >= Funzioni_Date_ConvertiDatainLong(CDC_DataIniziale) && Funzioni_Date_ConvertiDatainLong(v[1]) <= Funzioni_Date_ConvertiDatainLong(CDC_DataFinale)) {
                    ModelloTabellaCrypto.addRow(v);
                    if (Funzioni_isNumeric(v[19], false)) {
                        Plusvalenza = Plusvalenza.add(new BigDecimal(v[19]));
                    }
                }
            }

        }
        TransazioniCrypto_Funzioni_AbilitaBottoneSalva(TransazioniCrypto_DaSalvare);
        TransazioniCrypto_Text_Plusvalenza.setText("€ " + Plusvalenza.toPlainString());
        Color verde = new Color(45, 155, 103);
        Color rosso = new Color(166, 16, 34);
        if (!TransazioniCrypto_Text_Plusvalenza.getText().contains("-")) {
            TransazioniCrypto_Text_Plusvalenza.setForeground(verde);
        } else {
            TransazioniCrypto_Text_Plusvalenza.setForeground(rosso);
        }
        Funzioni_Tabelle_FiltraTabella(TransazioniCryptoTabella, TransazioniCryptoFiltro_Text.getText(), 999);
        //Adesso aggiorno i componenti delle funzioni secondarie
        GiacenzeaData_Funzione_AggiornaComboBoxWallet();
    }    
    
        
    private void TransazioniCrypto_Funzioni_AbilitaBottoneSalva(boolean Attivo) { 
         TransazioniCrypto_Bottone_Salva.setEnabled(Attivo);
         TransazioniCrypto_Bottone_Annulla.setEnabled(Attivo);
         TransazioniCrypto_Label_MovimentiNonSalvati.setVisible(Attivo);
         
        
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
     
    
    
    public static boolean Funzioni_isNumeric(String str,boolean CampoVuotoContacomeNumero) {
        //ritorna vero se il campo è vuoto oppure è un numero
  if(CampoVuotoContacomeNumero&&str.isBlank()) return true;
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
                  //   System.out.println("Errore, bisogna mettere una data inferiore o uguale a "+splittata[0]);
                        break;
                    }*/
            if (longDatainiziale > Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                UltimoValore = new BigDecimal(splittata[1]);
                SaldoInizialeT=splittata[1];
                //  System.out.println("SaldoIniziale="+UltimoValore+" , "+splittata[0]);
            }
            if (longDatainiziale <= Funzioni_Date_ConvertiDatainLong(splittata[0]) && longDataFinale >= Funzioni_Date_ConvertiDatainLong(splittata[0])) {
                //diffdate = Funzioni_Date_DifferenzaDate(splittata[0], DataIniziale);
                diffdate = OperazioniSuDate.DifferenzaDate(DataIniziale, splittata[0]);
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
                    //diffdate = Funzioni_Date_DifferenzaDate(splittata[0], DataIniziale);
                    diffdate = OperazioniSuDate.DifferenzaDate(DataIniziale, splittata[0]);
                    contatore = contatore + Integer.parseInt(String.valueOf(diffdate));
                    SaldoIniziale = UltimoValore.multiply(new BigDecimal(diffdate)).add(SaldoIniziale);
                    DataIniziale = splittata[0];
                    UltimoValore = new BigDecimal(splittata[1]);
                }

            }
        }

        //diffdate = Funzioni_Date_DifferenzaDate(DataFinale, DataIniziale) + 1;
        diffdate = OperazioniSuDate.DifferenzaDate(DataIniziale, DataFinale)+1;
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
    
    public void test(java.beans.PropertyChangeEvent evt){
       // System.out.println(((JDateChooser)(evt.getSource())).getDate());
    //   System.out.println(evt.getNewValue());
     // Opzioni_Emoney_Tabella.requestFocus();
  /*    ((JDateChooser)(evt.getSource())).setDateFormatString("yyyy-MM-dd");
            if (((JDateChooserRenderer)(evt.getSource())).getDate()!=null){
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            String Data=f.format(((JDateChooserRenderer)(evt.getSource())).getDate());
        //    System.out.println(Data);
        }*/
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
    
    
    public class JDateChooserRenderer extends JDateChooser implements TableCellRenderer{

  //  Date inDate;
        
       /*CDC_DataChooser_Iniziale.setDateFormatString("yyyy-MM-dd");
        CDC_DataChooser_Iniziale.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                CDC_DataChooser_InizialePropertyChange(evt);
            }
        });*/
    @Override
    
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        // TODO Auto-generated method stub
     //   this.setDateFormatString("yyyy-MM-dd");
    /*   this.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                test(evt);
            }});*/
    /*   this.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                test2(evt);
            }
        });*/
        if (value instanceof Date date){
               // DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
               // Date newDate = new Date();
               // String newDate=dateFormat.format(date);
               // this.setDateFormatString("yyyy-MM-dd");
                this.setDate(date);
                
                
                //this.setDateFormatString("AA");

        }else if (value instanceof Calendar calendar){
            this.setCalendar(calendar);
          //  this.setDateFormatString("AA");
        }
        return this;
    }
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
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaCardWallet;
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaFiatWallet;
    private javax.swing.JButton CDC_Opzioni_Bottone_CancellaPersonalizzazioniFiatWallet;
    private javax.swing.JTextField CDC_Text_Giorni;
    private javax.swing.JPanel DepositiPrelievi;
    private javax.swing.JButton DepositiPrelievi_Bottone_AssegnazioneAutomatica;
    private javax.swing.JButton DepositiPrelievi_Bottone_AssegnazioneManuale;
    private javax.swing.JButton DepositiPrelievi_Bottone_CreaMovOpposto;
    private javax.swing.JButton DepositiPrelievi_Bottone_DettaglioDefi;
    private javax.swing.JCheckBox DepositiPrelievi_CheckBox_movimentiClassificati;
    private javax.swing.JTable DepositiPrelievi_Tabella;
    private javax.swing.JPanel GiacenzeaData;
    private javax.swing.JButton GiacenzeaData_Bottone_Calcola;
    private javax.swing.JButton GiacenzeaData_Bottone_CambiaNomeToken;
    private javax.swing.JButton GiacenzeaData_Bottone_GiacenzeExplorer;
    private javax.swing.JButton GiacenzeaData_Bottone_ModificaValore;
    private javax.swing.JButton GiacenzeaData_Bottone_MovimentiDefi;
    private javax.swing.JButton GiacenzeaData_Bottone_RettificaQta;
    private javax.swing.JButton GiacenzeaData_Bottone_Scam;
    private javax.swing.JCheckBox GiacenzeaData_CheckBox_MostraQtaZero;
    private javax.swing.JCheckBox GiacenzeaData_CheckBox_NascondiScam;
    private com.toedter.calendar.JDateChooser GiacenzeaData_Data_DataChooser;
    private javax.swing.JLabel GiacenzeaData_Data_Label;
    private javax.swing.JScrollPane GiacenzeaData_ScrollPane;
    private javax.swing.JScrollPane GiacenzeaData_ScrollPaneDettaglioMovimenti;
    private javax.swing.JTable GiacenzeaData_Tabella;
    private javax.swing.JTable GiacenzeaData_TabellaDettaglioMovimenti;
    private javax.swing.JLabel GiacenzeaData_Totali_Label;
    private javax.swing.JTextField GiacenzeaData_Totali_TextField;
    private javax.swing.JComboBox<String> GiacenzeaData_Wallet2_ComboBox;
    private javax.swing.JLabel GiacenzeaData_Wallet2_Label;
    private javax.swing.JLabel GiacenzeaData_WalletEsame_Label;
    private javax.swing.JComboBox<String> GiacenzeaData_Wallet_ComboBox;
    private javax.swing.JLabel GiacenzeaData_Wallet_Label;
    private javax.swing.JLabel Giacenzeadata_Dettaglio_Label;
    private javax.swing.JLabel Giacenzeadata_Walleta_Label;
    private javax.swing.JLabel Giacenzeadata_Walletb_Label;
    private javax.swing.JPanel Opzioni;
    private javax.swing.JButton Opzioni_Bottone_CancellaTransazioniCrypto;
    private javax.swing.JButton Opzioni_Bottone_CancellaTransazioniCryptoXwallet;
    private javax.swing.JPanel Opzioni_CardWallet_Pannello;
    private javax.swing.JComboBox<String> Opzioni_Combobox_CancellaTransazioniCryptoXwallet;
    private javax.swing.JPanel Opzioni_Crypto_Pannello;
    private javax.swing.JButton Opzioni_Emoney_Bottone_Aggiungi;
    private javax.swing.JButton Opzioni_Emoney_Bottone_Rimuovi;
    private javax.swing.JPanel Opzioni_Emoney_Pannello;
    private javax.swing.JScrollPane Opzioni_Emoney_ScrollPane;
    private javax.swing.JTable Opzioni_Emoney_Tabella;
    private javax.swing.JCheckBox Opzioni_Export_EsportaPrezzi_CheckBox;
    private javax.swing.JPanel Opzioni_Export_Pannello;
    private javax.swing.JButton Opzioni_Export_Tatax_Bottone;
    private javax.swing.JComboBox<String> Opzioni_Export_Wallets_Combobox;
    private javax.swing.JPanel Opzioni_FiatWallet_Pannello;
    private javax.swing.JCheckBox Opzioni_GruppoWallet_CheckBox_PlusXWallet;
    private javax.swing.JPanel Opzioni_GruppoWallet_Pannello;
    private javax.swing.JScrollPane Opzioni_GruppoWallet_ScrollTabella;
    private javax.swing.JTable Opzioni_GruppoWallet_Tabella;
    private javax.swing.JTabbedPane Opzioni_TabbedPane;
    private javax.swing.JCheckBox Plusvalenze_Opzioni_CheckBox_Pre2023EarnCostoZero;
    private javax.swing.JCheckBox Plusvalenze_Opzioni_CheckBox_Pre2023ScambiRilevanti;
    private javax.swing.JPanel RW;
    private javax.swing.JComboBox<String> RW_Anno_ComboBox;
    private javax.swing.JButton RW_Bottone_Calcola;
    private javax.swing.JButton RW_Bottone_CorreggiErrore;
    private javax.swing.JButton RW_Bottone_IdentificaScam;
    private javax.swing.JButton RW_Bottone_ModificaVFinale;
    private javax.swing.JButton RW_Bottone_ModificaVIniziale;
    private javax.swing.JCheckBox RW_CheckBox_VediSoloErrori;
    private javax.swing.JLabel RW_Label_SegnalaErrori;
    private javax.swing.JCheckBox RW_Opzioni_CheckBox_1RigoXOperazione;
    private javax.swing.JTable RW_Tabella;
    private javax.swing.JTable RW_Tabella_Dettagli;
    private javax.swing.JTable RW_Tabella_DettaglioMovimenti;
    private javax.swing.JPanel SituazioneImport;
    private javax.swing.JTable SituazioneImport_Tabella1;
    private javax.swing.JPanel TransazioniCrypto;
    private javax.swing.JTextField TransazioniCryptoFiltro_Text;
    private javax.swing.JTable TransazioniCryptoTabella;
    private javax.swing.JButton TransazioniCrypto_Bottone_Annulla;
    private javax.swing.JButton TransazioniCrypto_Bottone_DettaglioDefi;
    private javax.swing.JButton TransazioniCrypto_Bottone_Importa;
    private javax.swing.JButton TransazioniCrypto_Bottone_InserisciWallet;
    private javax.swing.JButton TransazioniCrypto_Bottone_MovimentoElimina;
    private javax.swing.JButton TransazioniCrypto_Bottone_MovimentoModifica;
    private javax.swing.JButton TransazioniCrypto_Bottone_MovimentoNuovo;
    private javax.swing.JButton TransazioniCrypto_Bottone_Salva;
    private javax.swing.JCheckBox TransazioniCrypto_CheckBox_EscludiTI;
    private javax.swing.JLabel TransazioniCrypto_Label_Filtro;
    private javax.swing.JLabel TransazioniCrypto_Label_MovimentiNonSalvati;
    private javax.swing.JLabel TransazioniCrypto_Label_Plusvalenza;
    private javax.swing.JScrollPane TransazioniCrypto_ScrollPane;
    private javax.swing.JTabbedPane TransazioniCrypto_TabbedPane;
    private javax.swing.JTable TransazioniCrypto_Tabella_Dettagli;
    private javax.swing.JTextField TransazioniCrypto_Text_Plusvalenza;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}

