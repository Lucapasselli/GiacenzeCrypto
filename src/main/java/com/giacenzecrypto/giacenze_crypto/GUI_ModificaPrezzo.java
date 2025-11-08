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
    public Moneta MU;
    public Moneta ME;
    String PrezzoT;
    String Rete;
    public long TimeStamp;
    public boolean ModalitaRitorno=false;
    public String Ritorno[];

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
        PrezzoT=null;
        MU=null;
        ME=null;
        ModalitaRitorno=false;
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        this.setIconImage(icon.getImage());  
        
        CaricaTabellaPrezzoAttualedaID(ID); 
        CaricaTabellaPrezzi(ID);
        
    }
    
    public GUI_ModificaPrezzo(Moneta MU,Moneta ME, Prezzi.InfoPrezzo IPr, long TimeStamp,String Rete,String[] Ritorno) {
        this.MU=MU;
        this.ME=ME;
        this. TimeStamp=TimeStamp;
        this.Rete=Rete;
        PrezzoT=IPr.prezzoQta.toPlainString();
        
        initComponents();
        ModalitaRitorno=true;
        ImageIcon icon = new ImageIcon(Statiche.getPathRisorse()+"logo.png");
        this.setIconImage(icon.getImage());  
        
        CaricaTabellaPrezzoAttualedaID(IPr); 
        CaricaTabellaPrezzi();
        this.Ritorno=Ritorno;
        
    }
    
    private void CaricaTabellaPrezzoAttualedaID(Prezzi.InfoPrezzo IPr){
        DefaultTableModel ModTabPrezzoAttuale = (DefaultTableModel) Tabella_PrezzoAttuale.getModel();
        Tabelle.Funzioni_PulisciTabella(ModTabPrezzoAttuale);
        Tabelle.ColoraTabellaSemplice(Tabella_PrezzoAttuale);
        
        System.out.println(PrezzoT);
                
        

        //Compilo la tabella del prezzo attuale
        String PrezzoAttualeE[]=new String[8];
        String PrezzoAttualeU[]=new String[8];
        String ora=FunzioniDate.ConvertiDatadaLongAlSecondo(TimeStamp);
        if(MU!=null&&MU.Moneta!=null&&!MU.Moneta.isBlank()){
            PrezzoAttualeU[0]=MU.Moneta;
            PrezzoAttualeU[1]=ora;
            PrezzoAttualeU[2]=MU.Qta;
            if(IPr.Moneta.equalsIgnoreCase(MU.Moneta)){
                PrezzoAttualeU[3]=IPr.exchange;
                PrezzoAttualeU[4]=IPr.RitornaStringData();
                PrezzoAttualeU[5]=IPr.RitornaStringDiffData(TimeStamp);
                PrezzoAttualeU[6]=IPr.prezzoUnitario.toPlainString();
            }
            if (PrezzoT!=null)
                PrezzoAttualeU[7]=PrezzoT;
            else
                PrezzoAttualeU[7]="0.00";
            ModTabPrezzoAttuale.addRow(PrezzoAttualeU);
        }
        if(ME!=null&&ME.Moneta!=null&&!ME.Moneta.isBlank()){
            PrezzoAttualeE[0]=ME.Moneta;
            PrezzoAttualeE[1]=ora;
            PrezzoAttualeE[2]=ME.Qta;
            if(IPr.Moneta.equalsIgnoreCase(ME.Moneta)){
                PrezzoAttualeE[3]=IPr.exchange;
                PrezzoAttualeE[4]=IPr.RitornaStringData();
                PrezzoAttualeE[5]=IPr.RitornaStringDiffData(TimeStamp);
                PrezzoAttualeE[6]=IPr.prezzoUnitario.toPlainString();
            }
            if (PrezzoT!=null)
                PrezzoAttualeE[7]=PrezzoT;
            else
                PrezzoAttualeE[7]="0.00";
            ModTabPrezzoAttuale.addRow(PrezzoAttualeE);
            
        }
    }
    
    private void CaricaTabellaPrezzoAttualedaID(String ID){
        DefaultTableModel ModTabPrezzoAttuale = (DefaultTableModel) Tabella_PrezzoAttuale.getModel();
        Tabelle.Funzioni_PulisciTabella(ModTabPrezzoAttuale);
        Tabelle.ColoraTabellaSemplice(Tabella_PrezzoAttuale);
        this.ID=ID;
                
        
        String Movimento[]=Principale.MappaCryptoWallet.get(ID);
        String ora=Movimento[0].split("_")[0].substring(12);
        ora=Movimento[1]+":"+ora;
        long data=FunzioniDate.ConvertiDatainLongSecondo(ora);
        Prezzi.InfoPrezzo IPID=new Prezzi.InfoPrezzo(Movimento[40]);
        //Compilo la tabella del prezzo attuale
        String PrezzoAttualeE[]=new String[8];
        String PrezzoAttualeU[]=new String[8];
        //Prezzi.InfoPrezzo IPr=new Prezzi.InfoPrezzo(BigDecimal.ZERO, ora, data, BigDecimal.ZERO, BigDecimal.ONE, ora);
        if(!Movimento[8].isBlank()){
            PrezzoAttualeU[0]=Movimento[8];
            PrezzoAttualeU[1]=ora;
            PrezzoAttualeU[2]=Movimento[10];
            if(IPID.exchange!=null&&!IPID.exchange.isBlank()){
                PrezzoAttualeU[3]=IPID.exchange;
                if(IPID.Moneta.equalsIgnoreCase(Movimento[8])){                    
                    PrezzoAttualeU[4]=IPID.RitornaStringData();
                    PrezzoAttualeU[5]=IPID.RitornaStringDiffData(data);
                    PrezzoAttualeU[6]=IPID.prezzoUnitario.toPlainString();
                }
            }
            PrezzoAttualeU[7]=Movimento[15];
            ModTabPrezzoAttuale.addRow(PrezzoAttualeU);
        }
        if(!Movimento[11].isBlank()){
            PrezzoAttualeE[0]=Movimento[11];
            PrezzoAttualeE[1]=ora;
            PrezzoAttualeE[2]=Movimento[13];
            if(IPID.exchange!=null&&!IPID.exchange.isBlank()){
                PrezzoAttualeE[3]=IPID.exchange;
                if(IPID.Moneta.equalsIgnoreCase(Movimento[11])){                    
                    PrezzoAttualeE[4]=IPID.RitornaStringData();
                    PrezzoAttualeE[5]=IPID.RitornaStringDiffData(data);
                    PrezzoAttualeE[6]=IPID.prezzoUnitario.toPlainString();
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
                
        
        String Movimento[]=Principale.MappaCryptoWallet.get(ID);
        String ora=Movimento[0].split("_")[0].substring(12);
        ora=Movimento[1]+":"+ora;
        
        //PARTE 1 - Come prima cosa recupero i prezzi di entrambe le monete
        Moneta M[]=new Moneta[2];
        long data=FunzioniDate.ConvertiDatainLongSecondo(ora);
        Rete = Funzioni.TrovaReteDaIMovimento(Movimento);

        M[0] = new Moneta();
        M[1] = new Moneta();
        if(Rete==null||Principale.MappaRetiSupportate.get(Rete)==null){
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
            String NomeOriginale=M[i].Moneta;
        if (M[i].Moneta!=null&&!M[i].Moneta.isBlank()){
            //Prima di iniziare vedo se è una moneta codificata con address per cui devo cercare il prezzo da altre parti piuttosto che coingecko
            //in quel caso cambio anche il nome per trovare i prezzi es WBNB in BNB u USDT0 in USDT
            //ovviamente bisogna che si tratti di monete con lo stesso prezzo.
             if (M[i] != null && Principale.Mappa_AddressRete_Nome.get(M[i].MonetaAddress + "_" + M[i].Rete) != null) {
                M[i].Moneta = Principale.Mappa_AddressRete_Nome.get(M[i].MonetaAddress + "_" + M[i].Rete);
            }
            
            Prezzi.CambioXXXEUR(M[i].Moneta, M[i].Qta, data,M[i].MonetaAddress,M[i].Rete,"",false);
            
            
            //A - Vedo se è una fiat e in quel caso la tratto come tale
            if (M[i].Tipo.equalsIgnoreCase("FIAT")) {
                listaPrezziFIAT(M[i],Movimento[15],data,ModTabPrezzi);
                //Non vado a cercare le FIAT oltre, mi fermo qua dando solo questa possibilità
                continue;
            }
            
            //Cerco i prezzi da coingecko e li inserisco in tabella
            listaPrezziCoingecko(M[i],Movimento[15],data,NomeOriginale,ModTabPrezzi);
            
            //Adesso cerco i prezzi da ccxt ma solo se non ho address o comunque se è un address riconosciuto da coingecko
            listaPrezziCCXT(M[i],Movimento[15],data,ModTabPrezzi);
            
            
            //Adesso cerco i prezzi nel vecchio database
            listaPrezziVecchiDB(M[i],Movimento[15],data,NomeOriginale,ModTabPrezzi);
           
                
            }
        }
        
    }
    
    
        private void CaricaTabellaPrezzi(){
        DefaultTableModel ModTabPrezzi = (DefaultTableModel) Tabella_Prezzi.getModel();
        Tabelle.Funzioni_PulisciTabella(ModTabPrezzi);
        Tabelle.ColoraTabellaSemplice(Tabella_Prezzi);
                
        
        //String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
        String ora=FunzioniDate.ConvertiDatadaLongAlSecondo(TimeStamp);
        
        //PARTE 1 - Come prima cosa recupero i prezzi di entrambe le monete
        Moneta M[]=new Moneta[2];
        long data=FunzioniDate.ConvertiDatainLongSecondo(ora);

        M[0] = MU;
        M[1] = ME;
        if(Rete==null||Principale.MappaRetiSupportate.get(Rete)==null){
                Rete="";
                M[0].MonetaAddress="";
                M[1].MonetaAddress="";
            }
            M[0].Rete = Rete;
            M[1].Rete = Rete;
        //Forzo lo scaricamento dei prezzi di entrambe le monete
        for(int i=0;i<2;i++){
           // System.out.println(M[i].Moneta);
            String NomeOriginale=M[i].Moneta;
        if (M[i].Moneta!=null&&!M[i].Moneta.isBlank()){
            //Prima di iniziare vedo se è una moneta codificata con address per cui devo cercare il prezzo da altre parti piuttosto che coingecko
            //in quel caso cambio anche il nome per trovare i prezzi es WBNB in BNB u USDT0 in USDT
            //ovviamente bisogna che si tratti di monete con lo stesso prezzo.
             if (M[i] != null && Principale.Mappa_AddressRete_Nome.get(M[i].MonetaAddress + "_" + M[i].Rete) != null) {
                M[i].Moneta = Principale.Mappa_AddressRete_Nome.get(M[i].MonetaAddress + "_" + M[i].Rete);
            }
            
            Prezzi.CambioXXXEUR(M[i].Moneta, M[i].Qta, data,M[i].MonetaAddress,M[i].Rete,"",false);
            
            
            //A - Vedo se è una fiat e in quel caso la tratto come tale
            if (M[i].Tipo.equalsIgnoreCase("FIAT")) {
                listaPrezziFIAT(M[i],PrezzoT,data,ModTabPrezzi);
                //Non vado a cercare le FIAT oltre, mi fermo qua dando solo questa possibilità
                continue;
            }
            
            
            //Cerco i prezzi da Coingecko
            listaPrezziCoingecko(M[i],PrezzoT,data,NomeOriginale,ModTabPrezzi);
            
            //Cerco i prezzi da ccxt ma solo se non ho address o comunque se è un address riconosciuto da coingecko
            //Il tutto gestito dalla funzione
            listaPrezziCCXT(M[i],PrezzoT,data,ModTabPrezzi);
            
            //Cerco i prezzi nel vecchio database
            listaPrezziVecchiDB(M[i],PrezzoT,data,NomeOriginale,ModTabPrezzi);
                
            }
        }
        
    }
    
    
    private void listaPrezziCoingecko(Moneta M,String PrezzoRif,long data,String NomeOriginale,DefaultTableModel ModTabPrezzi){
    if (Funzioni_WalletDeFi.isValidAddress(M.MonetaAddress, M.Rete)) {
                    List<Prezzi.InfoPrezzo> ListaIP = Prezzi.DammiListaPrezziDaDatabase("", data, M.Rete, M.MonetaAddress, 60, new BigDecimal(M.Qta));
                    if (ListaIP.isEmpty()){
                        //Se non ho niente nel database preso da coingecko effettuo lo scaricamento
                        String DataIniziale=FunzioniDate.ConvertiDatadaLong(data);
                        Prezzi.RecuperaTassidiCambiodaAddress_Coingecko(DataIniziale, M.MonetaAddress, M.Rete, M.Moneta);
                        ListaIP = Prezzi.DammiListaPrezziDaDatabase("", data, M.Rete, M.MonetaAddress, 60, new BigDecimal(M.Qta));
                    }
                    for (Prezzi.InfoPrezzo IPl : ListaIP) {
                        String rigo[] = new String[9];
                        rigo[0] = NomeOriginale;
                        rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                        rigo[2] = M.Qta;
                        rigo[3] = IPl.exchange;
                        
                        rigo[4] = FunzioniDate.ConvertiDatadaLongAlSecondo(IPl.timestamp);
                        //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                       /* long DiffOrario=Math.abs(data-IPl.timestamp)/1000;
                        String unitaTempo;
                        if (DiffOrario >= 60) {
                            DiffOrario = DiffOrario / 60;  // converto in minuti
                            unitaTempo = " min";
                        } else {
                            unitaTempo = " sec";
                        }
                        
                        rigo[5] = String.valueOf(DiffOrario)+unitaTempo;*/
                        rigo[5] = IPl.RitornaStringDiffData(data);
                        rigo[6] = IPl.prezzoUnitario.toPlainString();
                        rigo[8] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                        rigo[7] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(PrezzoRif)).toPlainString();
                        ModTabPrezzi.addRow(rigo);
                    }
                }
    } 


    private void listaPrezziFIAT(Moneta M,String PrezzoRif,long data,DefaultTableModel ModTabPrezzi){
        Prezzi.InfoPrezzo IPF;
                String DataDollaro = FunzioniDate.ConvertiDatadaLong(data);
                if (M.Moneta.equalsIgnoreCase("EUR")) {
                    String rigo[] = new String[9];
                    rigo[0] = M.Moneta;
                    rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                    rigo[2] = M.Qta;
                    rigo[3] = "";
                    rigo[4] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                    rigo[5] = "0 sec";
                    rigo[6] = "1";
                    rigo[8] = new BigDecimal(M.Qta).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                    rigo[7] = new BigDecimal(M.Qta).abs().subtract(new BigDecimal(PrezzoRif)).toPlainString();
                    ModTabPrezzi.addRow(rigo);
                } 
                else if (M.Moneta.equalsIgnoreCase("USD")) {
                    String PT = Prezzi.ConvertiUSDEUR("1", DataDollaro);
                    if (PT != null) {
                        BigDecimal PrezzoTransazione = new BigDecimal(PT).abs().stripTrailingZeros();
                        IPF = new Prezzi.InfoPrezzo();
                        IPF.Moneta = M.Moneta;
                        IPF.Qta = new BigDecimal(M.Qta);
                        IPF.exchange = "bancaditalia";
                        IPF.prezzoUnitario = PrezzoTransazione;
                        IPF.timestamp = data;
                        IPF.prezzoQta=IPF.prezzoUnitario.multiply(IPF.Qta);
                        String rigo[] = new String[9];
                        rigo[0] = M.Moneta;
                        rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                        rigo[2] = M.Qta;
                        rigo[3] = IPF.exchange;
                        rigo[4] = FunzioniDate.ConvertiDatadaLong(IPF.timestamp);
                        rigo[5] = "1 giorno";
                        rigo[6] = IPF.prezzoUnitario.toPlainString();
                        rigo[8] = IPF.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                        rigo[7] = IPF.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(PrezzoRif)).toPlainString();
                        ModTabPrezzi.addRow(rigo);
                    }
                }
    }
    
     
    private void listaPrezziCCXT(Moneta M,String PrezzoRif,long data,DefaultTableModel ModTabPrezzi){
   String AddressNoPrezzo = DatabaseH2.GestitiCoingecko_Leggi(M.MonetaAddress + "_" + M.Rete);
            if (!Funzioni_WalletDeFi.isValidAddress(M.MonetaAddress, M.Rete) || AddressNoPrezzo != null) {

                List<Prezzi.InfoPrezzo> ListaIP = Prezzi.DammiListaPrezziDaDatabase(M.Moneta, data, "", "", 60, new BigDecimal(M.Qta));
                if (ListaIP.isEmpty()) {
                    //Se non ho niente nel database preso da coingecko effettuo lo scaricamento
                    Prezzi.RecuperaPrezziDaCCXT(M.Moneta, data);
                    ListaIP = Prezzi.DammiListaPrezziDaDatabase(M.Moneta, data, "", "", 60, new BigDecimal(M.Qta));
                }
                //Riempio la tabella con i prezzi
                for (Prezzi.InfoPrezzo IPl : ListaIP) {
                    String rigo[] = new String[9];
                    rigo[0] = M.Moneta;
                    rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                    rigo[2] = M.Qta;
                    rigo[3] = IPl.exchange;
                    rigo[4] = FunzioniDate.ConvertiDatadaLongAlSecondo(IPl.timestamp);
                    //Questa parte si occupa di calcolare la differenza tra il timestamp del movimento e quello del prezzo
                 /*   long DiffOrario = Math.abs(data - IPl.timestamp) / 1000;
                    String unitaTempo;
                    if (DiffOrario >= 60) {
                        DiffOrario = DiffOrario / 60;  // converto in minuti
                        unitaTempo = " min";
                    } else {
                        unitaTempo = " sec";
                    }
                    rigo[5] = String.valueOf(DiffOrario) + unitaTempo;*/

                    rigo[5] = IPl.RitornaStringDiffData(data);
                    rigo[6] = IPl.prezzoUnitario.toPlainString();
                    rigo[8] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                    rigo[7] = IPl.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(PrezzoRif)).toPlainString();
                    ModTabPrezzi.addRow(rigo);
                }
            }
    }     
    
    
     private void listaPrezziVecchiDB(Moneta M,String PrezzoRif,long data,String NomeOriginale,DefaultTableModel ModTabPrezzi){
         String DataOra = FunzioniDate.ConvertiDatadaLongallOra(data);
            String PrezzoUnitario = DatabaseH2.XXXEUR_Leggi(DataOra + " " + M.Moneta);
            Prezzi.InfoPrezzo IP = new Prezzi.InfoPrezzo();
            //System.out.println(PrezzoUnitario);
            if (PrezzoUnitario != null) {
                IP.exchange = "DB Interno (Old)";
                IP.timestamp = FunzioniDate.ConvertiDatainLongMinuto(DataOra + ":00");
                IP.prezzoUnitario = new BigDecimal(PrezzoUnitario);
                IP.Qta = new BigDecimal(M.Qta);
                IP.Moneta = M.Moneta;
                String rigo[] = new String[9];
                rigo[0] = M.Moneta;
                rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                rigo[2] = M.Qta;
                rigo[3] = IP.exchange;
                rigo[4] = DataOra + ":XX:XX";
                rigo[5] = "1 ora";

                rigo[6] = IP.prezzoUnitario.toPlainString();
                if (IP.prezzoQta==null)IP.prezzoQta=IP.prezzoUnitario.multiply(IP.Qta).abs();
                rigo[8] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                rigo[7] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(PrezzoRif)).toPlainString();
                ModTabPrezzi.addRow(rigo);
            }
            

            //Adesso cerco i prezzi nel vecchio database coingecko
            if (Funzioni_WalletDeFi.isValidAddress(M.MonetaAddress, M.Rete)) {
                PrezzoUnitario = DatabaseH2.PrezzoAddressChain_Leggi(DataOra + "_" + M.MonetaAddress + "_" + M.Rete);
                IP = new Prezzi.InfoPrezzo();
                //System.out.println(PrezzoUnitario);
                if (PrezzoUnitario != null) {
                    IP.exchange = "DB Interno (Coingecko)";
                    IP.timestamp = FunzioniDate.ConvertiDatainLongMinuto(DataOra + ":00");
                    IP.prezzoUnitario = new BigDecimal(PrezzoUnitario);
                    IP.Qta = new BigDecimal(M.Qta);
                    IP.Moneta = M.Moneta;
                    String rigo[] = new String[9];
                    rigo[0] = NomeOriginale;
                    rigo[1] = FunzioniDate.ConvertiDatadaLongAlSecondo(data);
                    rigo[2] = M.Qta;
                    rigo[3] = IP.exchange;
                    rigo[4] = DataOra + ":XX:XX";
                    rigo[5] = "1 ora";

                    rigo[6] = IP.prezzoUnitario.toPlainString();
                    if (IP.prezzoQta == null) {
                        IP.prezzoQta = IP.prezzoUnitario.multiply(IP.Qta).abs();
                    }
                    rigo[8] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).toPlainString();
                    rigo[7] = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal(PrezzoRif)).toPlainString();
                    ModTabPrezzi.addRow(rigo);
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
            String TimeStampa=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 4).toString();
            String Token=Tabella_Prezzi.getModel().getValueAt(rigaselezionata, 0).toString();
            String PrezzoPrecedente=Tabella_PrezzoAttuale.getModel().getValueAt(0, 7).toString();
            long timestamp;
            if(TimeStampa.contains("XX"))
            {
                TimeStampa=TimeStampa.substring(0, 13);
                timestamp=FunzioniDate.ConvertiDatainLongMinuto(TimeStampa+":00");
            }
            else timestamp=FunzioniDate.ConvertiDatainLongSecondo(TimeStampa);
            if (timestamp!=0){
                TimeStampa=String.valueOf(timestamp);
            }else TimeStampa="";
            
           // System.out.println("aaaa"+TimeStampa);
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
                    if (ID!=null){
                        Principale.MappaCryptoWallet.get(ID)[40]=Token+"|"+TimeStampa+"|"+PrezzoU+"|"+Fonte;
                        Principale.MappaCryptoWallet.get(ID)[15]=Prezzo;
                        Principale.TabellaCryptodaAggiornare=true;
                    }
                    
                    //Questo sarà il dato da leggere una volta chiusa la finestra con i dati
                    Ritorno[0]=Prezzo;
                    Ritorno[1]=Token+"|"+TimeStampa+"|"+PrezzoU+"|"+Fonte;
                
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
       if (ID!=null){
        if(Funzioni.GUIModificaPrezzo(this,ID))
        {
            Principale.MappaCryptoWallet.get(ID)[40]="|||Manuale";
            Principale.TabellaCryptodaAggiornare=true;
            this.dispose();
        }
        } else {
            String ritPrz = Funzioni.GUIModificaPrezzo(this, MU, ME, PrezzoT, TimeStamp, Rete);
            if (ritPrz != null) {
                Ritorno[0] = ritPrz;
                Ritorno[1] = "|||Manuale";
                this.dispose();
            }
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
