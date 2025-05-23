/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.MappaCryptoWallet;
import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.MappaRetiSupportate;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;

/**
 *
 * @author Luca
 */
public class Funzioni {
    
        public static int CancellaMovimentazioniXWallet(String Wallet,long DataIniziale,long DataFinale){
        
        //Se Wallet=null  allora la pulizia la faccio su tutti i wallet
        int movimentiCancellati=0;
       //this.TransazioniCryptoFiltro_Text.setText("");
        //questo server per velocizzare la ricerca
        //disabilito il filtro e poi lo riabilito finito l'eleaborazione
        
        List<String> Cancellare=new ArrayList<>();
        

            for (String v : CDC_Grafica.MappaCryptoWallet.keySet()) {
                String WalletRiga = CDC_Grafica.MappaCryptoWallet.get(v)[3].trim();
                long DataMovimento=OperazioniSuDate.ConvertiDatainLong(CDC_Grafica.MappaCryptoWallet.get(v)[1].split(" ")[0]);
                if (Wallet==null || WalletRiga.equalsIgnoreCase(Wallet.trim())) {
                    if ((DataMovimento >= DataIniziale
                            && DataMovimento < DataFinale) ||
                            (DataIniziale==0)&&(DataFinale==0)) {//Se data finale e iniziale sono a zero significa che non valgolo i limiti di date
                        Cancellare.add(v);
                        movimentiCancellati++;
                    }

                }
            }
        Iterator<String> I=Cancellare.iterator();
        while (I.hasNext()){
            String daRimuovere=I.next().toString();
            RimuoviMovimentazioneXID(daRimuovere);
        }
        
           // MappaCryptoWallet.clear();
   
        return movimentiCancellati;
    }
        
        //Funzione che si occupa di rimuovere una movimentazione
        //eliminando o sistemando anche tutti i movimenti correlati.
        //ad esempio se devo rimuovere un movimento di prelievo che è associato ad un altro movimento di dieposito
        //prima di rimuovere il prelievo vado a torgliere dal deposito i riferimenti al prelievo che devo eliminare
         public static void RimuoviMovimentazioneXID(String ID){
             
            String Annessi[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            if (Annessi!=null){
            String PartiCoinvolte[]=(ID+","+Annessi[20]).split(",");
            if (Annessi[20]!=null && !Annessi[20].equalsIgnoreCase("")){
                ClassificazioneTrasf_Modifica.RiportaTransazioniASituazioneIniziale(PartiCoinvolte);
            }
            CDC_Grafica.MappaCryptoWallet.remove(ID);
            } 
         }
        
        
        public static String formattaBigDecimal(BigDecimal numero,boolean decimali) {
        // Crea un'istanza di DecimalFormatSymbols per il locale italiano
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
        symbols.setGroupingSeparator('.'); // Separatore delle migliaia
        symbols.setDecimalSeparator(',');  // Separatore decimale

        // Definisce il pattern di formattazione
        DecimalFormat formatter;               
        if (decimali) formatter = new DecimalFormat("#,##0.00", symbols);
        else formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(numero);
    }
         
       public static void simulaCtrlC() {
        try {
            Robot robot = new Robot();

            // Preme Ctrl
            robot.keyPress(KeyEvent.VK_CONTROL);
            // Preme C
            robot.keyPress(KeyEvent.VK_C);

            // Rilascia C
            robot.keyRelease(KeyEvent.VK_C);
            // Rilascia Ctrl
            robot.keyRelease(KeyEvent.VK_CONTROL);

           // System.out.println("Simulato Ctrl+C");

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }  
        
              public static void simulaCtrlV() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }  
              
    public static void PopUpMenu(Component c, java.awt.event.MouseEvent e, JPopupMenu pop,String ID) {
        if (e.isPopupTrigger()) {
            //Component focusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            //salvo l'ID passato dalla funzione, servirà nel caso in cui prema su alcune funzioni
            CDC_Grafica.PopUp_IDTrans=ID;
            CDC_Grafica.PopUp_Component=c;
            //System.out.println(CDC_Grafica.PopUp_Component);
            Component C_chiamante=e.getComponent();
            //C_chiamante
            Point Coordinata = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(Coordinata, c);
            //Se non passo l'id della transazione ingrigisco il tasto dettaglio movimento
            if (ID==null)
            {
                PopUp_disableMenuItemByText(pop,"Dettagli Movimento");
            }else{
                PopUp_enableMenuItemByText(pop,"Dettagli Movimento");
            }
            
            //Se è una tabella mi comporto in questo modo
            if (C_chiamante instanceof JTable table)
            {                
                int row = table.getSelectedRow();
                if (row == -1) return;
                PopUp_disableMenuItemByText(pop,"Incolla");
                PopUp_enableMenuItemByText(pop,"Esporta Tabella in Excel");
                CDC_Grafica.PopUp_Tabella=table;
                pop.show(c, Coordinata.x, Coordinata.y);
            }            
            else if (C_chiamante instanceof JTextField)
            {
                PopUp_enableMenuItemByText(pop,"Incolla");
                PopUp_disableMenuItemByText(pop,"Esporta Tabella in Excel");
                pop.show(c, Coordinata.x, Coordinata.y);
                CDC_Grafica.PopUp_Tabella=null;
            }
            //Se è un campo di testo in quest'altro
            
            
            
        }
    }
    
    public static boolean PopUp_ClickInternoASelezione(JTable table,java.awt.event.MouseEvent e){
        int clickedRow = table.rowAtPoint(e.getPoint());
        if (clickedRow == -1) return false; // clic fuori da qualsiasi riga

        // Ottieni tutte le righe attualmente selezionate
        int[] selectedRows = table.getSelectedRows();

        // Verifica se la riga cliccata è tra quelle selezionate
        for (int row : selectedRows) {
            if (row == clickedRow) {
                //Trovato riga selezionata
                return true;
            }
        }
        return false;
    }
    
    
        public static List<JMenuItem> PopUp_getAllMenuItems(JPopupMenu popupMenu) {
        List<JMenuItem> items = new ArrayList<>();
        for (Component comp : popupMenu.getComponents()) {
            if (comp instanceof JMenuItem) {
                items.add((JMenuItem) comp);
            }
        }
        return items;
    }
    
    
    public static void PopUp_disableMenuItemByText(JPopupMenu popupMenu, String textToDisable) {
        for (JMenuItem item : PopUp_getAllMenuItems(popupMenu)) {
            if (item.getText() != null && item.getText().equalsIgnoreCase(textToDisable)) {
                item.setEnabled(false);
            }
        }
    }
        public static void PopUp_enableMenuItemByText(JPopupMenu popupMenu, String textToDisable) {
        for (JMenuItem item : PopUp_getAllMenuItems(popupMenu)) {
            if (item.getText() != null && item.getText().equalsIgnoreCase(textToDisable)) {
                item.setEnabled(true);
            }
        }
    }
       
        public static String GUIDammiPrezzo (Component c,String NomeMon,long DataPrezzo,String Qta,String Prezzo){
            
            //PARTE 1 -> Se conosco la data del movimento chiedo se voglio inserire il prezzo in dollari o in Euro
            //PARTE 2 -> Se specificato moneta e qta chiedo se voglio inserire il prezzo unitario o quello riferito al numero di token
            //PARTE 3 -> Chiedo di inserire l'importo e poi controllo che questo sia un numero 
            if (Prezzo==null)Prezzo="0";
           // String PRZ="0";
            
            
            
            //PARTE 1
            int scelta=0;
            String MonRiferimento="EURO";
            String Testo;
            if (DataPrezzo!=0){
                Testo = "<html>Indica se vuoi imputare il prezzo in Euro o Dollari.<br><br>"
                            + "(Se scegli dollari il prezzo verrà poi convertito in Euro seguendo il tasso di cambio della giornata di bancha d'Italia)<br><br>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "EURO", "DOLLARI"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Moneta di riferimento",
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
                                //EURO                           
                            }
                            case 2 -> {
                                //DOLLARO 
                                MonRiferimento="DOLLARI";
                                //Adesso trasformo il prezzo in dollari per presentarlo corretto nelle prossime schermate
                                if (Prezzo!=null){
                                    String Giorno=OperazioniSuDate.ConvertiDatadaLong(DataPrezzo);
                                    String Val1Dollaro=Prezzi.ConvertiUSDEUR("1", Giorno);
                                    Prezzo=new BigDecimal(Prezzo).divide(new BigDecimal (Val1Dollaro), 2, RoundingMode.HALF_UP).toPlainString();
                                }
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return null;
                    }
            }
            
                    
            //PARTE 2
            boolean PrezzoUnitario=false;
            if (NomeMon!=null&&Qta!=null&&!Qta.equals("1")){
            Testo = "<html>Per il token "+NomeMon+" vuoi indicare il prezzo unitario o quello riferito al totale dei "+Qta+" pezzi?<br><br>"
                            + "(Se scegli di indicare il prezzo unitario il prezzo totale verrà poi calcolato dal programma)<br><br>"
                            + "</html>";
                    Object[] Bottoni2 = {"Annulla", "TOTALE", "UNITARIO"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Prezzo unitario o Totale",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni2,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                //TOTALE                           
                            }
                            case 2 -> {
                                //UNITARIO
                                PrezzoUnitario=true;
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return null;
                    }
            }
            
            //PARTE 3
            if (NomeMon!=null&&Qta!=null){
                if (PrezzoUnitario)
                    Testo = "<html>indicare il prezzo <b>unitario</b> in "+MonRiferimento+" relativo al token "+NomeMon+"<br><br>"
                            + "(Il prezzo totale verrà poi calcolato dal programma)<br><br>"
                            + "</html>";
                else 
                    Testo = "<html>indicare il prezzo in "+MonRiferimento+" relativo a "+Qta+" "+NomeMon+"<br><br>"
                            + "<br><br>"
                            + "</html>";
            }
            else   
                Testo = "<html>indicare il prezzo in "+MonRiferimento+"<br><br>"
                            + "<br><br>"
                            + "</html>";
            
            
            String Prezz = JOptionPane.showInputDialog(c, Testo, Prezzo);
                if (Prezz != null) {
                    Prezz = Prezz.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (CDC_Grafica.Funzioni_isNumeric(Prezz, false)) {
                        //Se dollari devo fare la conversione in euro
                        if (!MonRiferimento.equals("EURO")){
                            //devo fare la conversione da dollari a euro
                            String Giorno=OperazioniSuDate.ConvertiDatadaLong(DataPrezzo);
                            Prezz=Prezzi.ConvertiUSDEUR(Prezz, Giorno);
                            //devo fare la conversione in dollari
                        }
                        //Se prezzo unitario poi devo moltiplicarlo per la quantità
                        if (PrezzoUnitario){
                            Prezz=new BigDecimal(Prezz).multiply(new BigDecimal(Qta)).setScale(2, RoundingMode.HALF_UP).toPlainString();
                        }                       
                        return Prezz;
                    }else {
                        JOptionPane.showConfirmDialog(c, "Attenzione, " + Prezz + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                }
                
                return null;
        }
         
        
        public static boolean CambiataVersione(String Versione){
            boolean VersioneCambiata=false;
            String Ver=DatabaseH2.Opzioni_Leggi("Versione");
            if (Ver==null){
                VersioneCambiata=true;
            }
            else {
                if (!Ver.equals(Versione)){
                    VersioneCambiata=true;
                }
            }
            DatabaseH2.Opzioni_Scrivi("Versione",Versione);
            return VersioneCambiata;
         }     
        
         
        
        public static String TrasformaNullinBlanc(String stringa){
            if (stringa==null)return "";
            else return stringa;
        }
         
        public static String NormalizzaNome(String Nome){
            String NuovoNome=Nome.replace(";", "")
                   // .replace(" ", "")
                   // .replace(".", "")
                    .replace(",", "")
                   // .replace("_", "")
                   // .replace("\"", "")
                   // .replace("'", "")
                    ;    
            return NuovoNome;
         }
        
         public static String NormalizzaNomeStringente(String Nome){
            String NuovoNome=Nome.replace(";", "")
                   // .replace(" ", "")
                   // .replace(".", "")
                    .replace(",", "")
                    .replace("_", "")
                    .replace("\\(", "")
                    .replace("\\)", "")
                    ;    
            return NuovoNome;
         }
        
    public static boolean DuplicaMovimento(String ID){
        String riga[]=CDC_Grafica.MappaCryptoWallet.get(ID);
        String nuovariga[]=riga.clone();
        String IDori=nuovariga[0];
        String idSplit[]=IDori.split("_");
        if (idSplit.length>4){
            for(int k=1;k<10;k++){
                String split3;
                if (Funzioni.Funzioni_isNumeric(idSplit[3], false)){
                    split3=String.valueOf((int)Double.parseDouble(idSplit[3])+k);
                    
                }
                else {split3=idSplit[3]+k;}
                String newID=idSplit[0]+"_"+idSplit[1]+"_"+idSplit[2]+"_"+split3+"_"+idSplit[4];
                if (CDC_Grafica.MappaCryptoWallet.get(newID)==null){
                    //ho trovato un id libero, creo il nuovo movimento
                    nuovariga[0]=newID;//imposto il nuovo ID
                    nuovariga[22]="M";//Dico che il movimento è stato aggiunto manualmente
                    CDC_Grafica.MappaCryptoWallet.put(newID, nuovariga);
                    return true;
                }
            }
        }
        return false;
    }   
        
    public static BigInteger hexToDecimal(String hexNumber) {
        // Verifica se la stringa fornita è vuota o nulla
        if (hexNumber == null || hexNumber.isEmpty()) {
            throw new IllegalArgumentException("La stringa esadecimale non può essere vuota o nulla.");
        }

        // Rimuove il prefisso "0x" se presente
        if (hexNumber.startsWith("0x")) {
            hexNumber = hexNumber.substring(2);
        }

        // Converte la stringa esadecimale in decimale utilizzando il metodo parseLong della classe Long
        return new BigInteger(hexNumber , 16);
        
    }
        
    
        public static boolean isValidJSONArray(String jsonString) {
        try {
            new JSONArray(jsonString); // Prova a creare un JSONArray
            return true;
        } catch (JSONException e) {
            System.out.println("ERRORE : "+jsonString+" non è un array di stringhe valido");
            return false; // Se genera un'eccezione, non è un JSONArray valido
        }
    }
 
        
        public static void Export_CreaExcelDaTabella(JTable tabella){

        try {
            //System.out.println("orcapaletta");
            File f=new File ("temp.xlsx");
            FileOutputStream fos = new FileOutputStream(f);
            Workbook wb = new Workbook(fos,"excel1","1.0");
            Worksheet ws=wb.newWorksheet("Riepilogo Tabella ");
           // ws.value(0, 0,"Riepilogo Tabella Sheet ");

            TableModel model = tabella.getModel();
            //Scrivo l'intestazione della tabella riepilogo
            
           // if (tabella.getName().equalsIgnoreCase("TabellaMovimentiCrypto")){}
            int NumColonne=tabella.getColumnCount();
            //System.out.println(model.getColumnCount());
            String NomeTabella=tabella.getName();
            if (NomeTabella!=null&&NomeTabella.equalsIgnoreCase("TabellaMovimentiCrypto")){
               NumColonne=35;
              /* int i=0;
               for (String riga[] : CDC_Grafica.MappaCryptoWallet.values()){
                    for (int k = 0; k < NumColonne; k++) {
                        //if (riga[k]==null)riga[k]="";
                        riga[k] = Jsoup.parse(riga[k]).text();
                    }
                    ScriviRigaExcel(riga, ws, i + 1);
                    i++;
               }*/
            }
           // else{
                
                String riga[]=new String[NumColonne];
                for (int i = 0; i < NumColonne; i++) {
                    String NomeColonna = model.getColumnName(i);
                    NomeColonna = Jsoup.parse(NomeColonna).text();
                    riga[i] = NomeColonna;
                }
                ScriviRigaExcel(riga, ws, 0);
                for (int i = 0; i < tabella.getRowCount(); i++) {
                    int modelRow = tabella.convertRowIndexToModel(i);
                    riga = new String[NumColonne];
                    for (int k = 0; k < NumColonne; k++) {
                        riga[k] = model.getValueAt(modelRow, k).toString();
                        riga[k] = Jsoup.parse(riga[k]).text();
                    }
                    ScriviRigaExcel(riga, ws, i + 1);

                }
           // }
            ws.finish();
            ws.close();
            wb.finish();
            wb.close();
            fos.close();
            Desktop desktop = Desktop.getDesktop();
            desktop.open(f);
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
        
        
    public static void RW_CreaExcel(JTable RW_Tabella,String Anno){

        try {
            //System.out.println("orcapaletta");
            File f=new File ("RW.xlsx");
            FileOutputStream fos = new FileOutputStream(f);
            Workbook wb = new Workbook(fos,"excel1","1.0");
            Worksheet wsrm=wb.newWorksheet("Riepilogo Anno "+Anno);
            wsrm.value(0, 0,"Riepilogo Anno "+Anno);
            String Intestazione[]=new String []{"RW","Valore Iniziale","Valore Finale","Giorni di detenzione","Errori",
                    "IC Dovuta","Bollo pagato"};
                ScriviRigaExcel(Intestazione,wsrm,1);
            
        // Per prima cosa creo la prima riga di intestazione
        Map<String, String[]> Mappa_Gruppo_Alias = DatabaseH2.Pers_GruppoAlias_LeggiTabella();
            Worksheet ws,wsI,wsF,wsM;
            TableModel model = RW_Tabella.getModel();
            //Scrivo l'intestazione della tabella riepilogo
            
            for (int i = 0; i < RW_Tabella.getRowCount(); i++) {
                //Popolo il primo worksheet con il riepilogo
                String RigaRiepilogo[]=new String[7];
                RigaRiepilogo[0]=model.getValueAt(i, 0).toString();
                RigaRiepilogo[1]=model.getValueAt(i, 1).toString();
                RigaRiepilogo[2]=model.getValueAt(i, 2).toString();
                RigaRiepilogo[3]=model.getValueAt(i, 3).toString();
                RigaRiepilogo[4]=model.getValueAt(i, 4).toString();
                RigaRiepilogo[5]=model.getValueAt(i, 5).toString();
                RigaRiepilogo[6]=model.getValueAt(i, 7).toString();
                ScriviRigaExcel(RigaRiepilogo,wsrm,i+2);
                
                
                //Creao i Worksheet relativi ai dettagli per il calcolo dell'RW
                String GruppoW=model.getValueAt(i, 0).toString();
                ws = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Calcoli RW");
                wsI = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Inizio "+Anno);
                wsF = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Fine "+Anno);
                wsM = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Movimenti "+Anno);
                String Gruppo = "Wallet " + model.getValueAt(i, 0).toString().split(" ")[0].trim();
                
                
                String Intestazioni[]=new String []{"Gruppo Iniziale","Moneta Iniziale","Qta Inizale","Data Iniziale","Valore Iniziale",
                    "Gruppo Finale","Moneta Finale","Qta Finale","Data Finale","Valore Finale","Giorni di detanzione","Motivo fine detenzione"};
                int colonna = 0;
                ScriviRigaExcel(Intestazioni,ws,0);
                int riga=1;
                for (String[] lista : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(Gruppo)) {
                    //Sistemo i nomi dei gruppi Wallet
                    if (Mappa_Gruppo_Alias.get(lista[1]) != null) {
                        lista[1] = lista[1].split(" ")[1].trim() + " ( " + Mappa_Gruppo_Alias.get(lista[1])[1] + " )";
                    }
                    if (Mappa_Gruppo_Alias.get(lista[6]) != null) {
                        lista[6] = lista[6].split(" ")[1].trim() + " ( " + Mappa_Gruppo_Alias.get(lista[6])[1] + " )";
                    }
                    //Scrivo i dati sull'excel
                    colonna = 0;
                    
                    for (String Valore : lista) {
                        if (colonna>0&&colonna<13){//Scrivo solo le colonne con idati che mi interessano
                        if (Funzioni_isNumeric(Valore,false)) {
                            double val = Double.parseDouble(Valore);
                            ws.value(riga, colonna-1, val);
                        }else{
                             ws.value(riga, colonna-1, Valore);
                        }}
                        colonna++;
                        
                    }
                    riga++;
                }
                //Adesso creao il worksheet relativo alle giacenze del wallet di inizio e fine anno
                wsI.value(0, 0, GruppoW+" - Giacenze Inizio "+Anno);
                wsF.value(0, 0, GruppoW+" - Giacenze Fine "+Anno);
                Intestazioni=new String []{"Nome","Rete","Address DeFi del Token","Tipo","Quantità",
                    "Valore in Euro","Valore Unirtario","Note"};
                ScriviRigaExcel(Intestazioni,wsI,1);
                ScriviRigaExcel(Intestazioni,wsF,1);
                //long DataRiferimento=0;
                String DataInizio=Anno+"-01-01";
                String DataFine=Anno+"-12-31";
                long lDataFine=OperazioniSuDate.ConvertiDatainLong(DataFine)+86400000;
                long lDataInizio=OperazioniSuDate.ConvertiDatainLong(DataInizio);
                String GruppoWNormalizzato="Wallet "+GruppoW.split(" ")[0];
                List<String[]> ListaSaldiIniziali=RW_GiacenzeaData(lDataInizio,GruppoWNormalizzato,"");
                List<String[]> ListaSaldiFinali=RW_GiacenzeaData(lDataFine,GruppoWNormalizzato,"");
                int r=2;
                //Iterator<String[]> Iniziali=ListaSaldiIniziali.iterator();
                //Iterator<String[]> Finali=ListaSaldiFinali.iterator();
                //Saldi Iniziali
                for (int ii=0;ii<ListaSaldiIniziali.size();ii++){
                    String valori[]=ListaSaldiIniziali.get(ii);
                        String[] copy = new String[valori.length + 2];
                        System.arraycopy(valori, 0, copy, 0, valori.length);
                        valori = copy;
                        String Rete=valori[1];
                        Moneta M1=new Moneta();
                        M1.Moneta=valori[0];
                        M1.MonetaAddress=valori[2];
                        M1.Qta="1";
                        M1.Tipo=valori[3];
                        M1.Rete=Rete;
                        valori[6]=Prezzi.DammiPrezzoTransazione(M1,null,lDataInizio, null,true,30,Rete);
                    if (valori[0].contains(" **"))
                    {
                        valori[7]="Token SCAM";
                    }
                    ScriviRigaExcel(valori,wsI,r);
                    r++;
                }
                r=2;
                //Saldi Finali
                for (int ii=0;ii<ListaSaldiFinali.size();ii++){
                    String valori[]=ListaSaldiFinali.get(ii);
                        String[] copy = new String[valori.length + 2];
                        System.arraycopy(valori, 0, copy, 0, valori.length);
                        valori = copy;
                        String Rete=valori[1];
                        Moneta M1=new Moneta();
                        M1.Moneta=valori[0];
                        M1.MonetaAddress=valori[2];
                        M1.Qta=new String("1");
                        M1.Tipo=valori[3];
                        M1.Rete=Rete;
                        valori[6]=Prezzi.DammiPrezzoTransazione(M1,null,lDataFine, null,true,30,Rete);
                    if (valori[0].contains(" **"))
                    {
                        valori[7]="Token SCAM";
                    }
                    ScriviRigaExcel(valori,wsF,r);
                    r++;
                }
                
                //Lista Movimenti
                Intestazioni = new String[]{
                    "Gruppo Wallet x RW",               //
                    "ID",                               //0
                    "Data",                             //1
                    "Wallet Principale/Exchange",       //3
                    "Dettaglio Wallet",                 //4
                    "Rete Wallet",                      //da calcolare
                    "Tipo Transazione",                 //5
                    "Token Venduto/Ceduto",             //8
                    "Tipo Token Venduto/Ceduto",        //9
                    "Address Token Venduto/Ceduto",     //26
                    "Quantità Venduta/Ceduta",          //10
                    "Token Acquistato/Ricevuto",        //11
                    "Tipo Token Acquistato/Ricevuto",   //12
                    "Address Token Acquistato/Ricevuto",//28
                    "Quantità Acquistata/Ricevuta",     //13
                    "Valore Transazione",               //15
                    "Riferimento Trasferimenti",        //20
                    "Note"                              //21
                };       
                ScriviRigaExcel(Intestazioni,wsM,0);
                int rli=1;
                for (String[] v : MappaCryptoWallet.values()) {
                    String AnnoRiga=v[1].split("-")[0];
                    String GruppoRiga=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
                    //se l'anno è quello di riferimento e il gruppo walle è quello analizzato allora scrivo i movimenti sull'excel
                    if(AnnoRiga.equals(Anno)&&
                            GruppoRiga.split(" ")[1].equals(GruppoW.split(" ")[0])){
                    String rigaT[]=new String[18];
                    rigaT[0]=GruppoW;
                    rigaT[1]=v[0];
                    rigaT[2]=v[1];
                    rigaT[3]=v[3];
                    rigaT[4]=v[4];
                    rigaT[5]=TrovaReteDaID(v[0]);
                    rigaT[6]=v[5];
                    rigaT[7]=v[8];
                    rigaT[8]=v[9];
                    rigaT[9]=v[26];
                    rigaT[10]=v[10];
                    rigaT[11]=v[11];
                    rigaT[12]=v[12];
                    rigaT[13]=v[28];
                    rigaT[14]=v[13];
                    rigaT[15]=v[15];
                    rigaT[16]=v[20];
                    rigaT[17]=v[21];
                    ScriviRigaExcel(rigaT,wsM,rli);
                    rli++;
                    }
                }
                
                
                wsI.finish();
                wsF.finish();
                ws.finish();
                wsI.close();
                wsF.close();
                ws.close();
            }
            wsrm.finish();
            wsrm.close();
            wb.finish();
            wb.close();
            fos.close();
            Desktop desktop = Desktop.getDesktop();
            desktop.open(f);
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Map<String, String>  MappaPrimoMovimentoXGruppoWallet() {
        Map<String, String> Mappa_Gruppi = new TreeMap<>();//la mappa è così composta, (Gruppo,ID Primo Movimento)
        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
            if (Mappa_Gruppi.get(GruppoWallet)==null)Mappa_Gruppi.put(GruppoWallet, v[0]);
        }
        return Mappa_Gruppi;
    }
    
    
    public static void ScriviRigaExcel(String Valori[], Worksheet ws, int riga) {
        int colonna = 0;
        for (String Valore : Valori) {
            if (Funzioni_isNumeric(Valore, false)) {
                double val = Double.parseDouble(Valore);
                ws.value(riga, colonna, val);
            } else {
                ws.value(riga, colonna, Valore);
            }
            colonna++;
        }
    }
    
    
    

    
    
    
        public static List<String[]> RW_GiacenzeaData(long DataRiferimento,String Wallet,String SottoWallet){
            //Nel wallet si può mettere il nome del gruppo Wallet
        
         List<String[]> ListaSaldi=new ArrayList<>();  
        //Compilo la mappa QtaCrypto con la somma dei movimenti divisa per crypto
        //in futuro dovrò mettere anche un limite per data e un limite per wallet
        //progress.RipristinaStdout();
        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
        Map<String, Moneta> QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto il bigdecimal con la qta
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaID(movimento[0]);
                    long DataMovimento = OperazioniSuDate.ConvertiDatainLong(movimento[1]);
                    if (DataMovimento < DataRiferimento) {
                        if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                || (Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase("tutti"))//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||(Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase(movimento[4].trim()))//Se wallet e sottowallet corrispondono a quelli analizzati proseguo
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3]).equals(Wallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
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
        
        //Adesso elenco tutte le monete e le metto il tutto in una lista   
        
        int i=0;
        BigDecimal TotEuro=new BigDecimal(0);
        for (String moneta :QtaCrypto.keySet()){
            i++;
            Moneta M1=QtaCrypto.get(moneta);
            String Rete=M1.Rete;
            String Address=M1.MonetaAddress;
            String riga[]=new String[6];
            riga[0]=M1.Moneta;
            riga[2]=Address;//qui ci va l'address della moneta se non sto analizzando i wallet nel complesso
            riga[3]=M1.Tipo;
            riga[4]=M1.Qta;
            riga[1]=M1.Rete;
            if (!M1.Qta.equals("0"))
            {
                if (M1.Qta.equals("0"))riga[5]="0.00";
                else riga[5]=Prezzi.DammiPrezzoTransazione(M1,null,DataRiferimento, null,true,2,Rete);
                if (riga[4].contains("-")&&!riga[5].equals("0.00"))riga[5]="-"+riga[5];
                ListaSaldi.add(riga);                
            }
            
        }       
return ListaSaldi;
}
    
    
    
    
    public static boolean ApriWeb(String Url) {

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {


                Desktop.getDesktop().browse(new URI(Url));
                return true;

            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            String os = System.getProperty("os.name").toUpperCase();
            if (os.contains("LINUX")) {
                try {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", Url});

                } catch (IOException ex) {
                    Logger.getLogger(Funzioni.class.getName()).log(Level.SEVERE, null, ex);
                    return true;
                }
            }
        }
        return false;
    }
    
    
        public static String RitornaReteDefi(String ID) {
        String Transazione[]=MappaCryptoWallet.get(ID);
        String Wallet=Transazione[3].trim();
        String appoggio[]=Wallet.split(" ");
        String Rete="";
        String ReteAppoggio;
        // Se soddisfa le seguenti condizioni significa che ho trovato un wallet in defi e posso tornare il nome della Rete DEFI
        // Quindi restituisco il nome della rete oltre le condizioni principali solo se hop la transaction hash
        if (appoggio.length==2&&appoggio[1].contains("(")&&appoggio[1].contains(")")&&ID.split("_")[1].startsWith("BC.")){
            ReteAppoggio=ID.split("_")[1].split("\\.")[1];
            if (Funzioni_WalletDeFi.isValidAddress(appoggio[0],ReteAppoggio)){
                Rete=ReteAppoggio;
            }
        }
        return Rete;
    }
    
    
        //Questa funzione è da lanciare al termine di un importazione dati DeFi e se imposto un token come scam
    //controlla se ci sono commissioni imputate a movimenti di prelievo defi scam
    //questi movimenti non sono veri movimenti ma vengono solo visti dagli explorer come tali
    //le commissioni non vanno quindi imputate al wallet e vanno quindi tolte.
    
    //Questa funzione che si occupa di:
                    //1 - Eliminae le commissioni fittizzie sui movimenti di prelievo scam
                    //2 - Cancellare i prelievi e le commissioni con quantità zero perchè anch'essi scam
                    //3 - Trasformare i prelievi fatti da se stessi per se stessi in scambio con la stessa moneta
                    //Se per sbaglio infatti invio cripto al mio stesso wallet questo viene identificato come prelievo
                    //ma non vi è nessun movimento di deposito, il risultato sarebbero delle giacenze errate
    
    //Da Lanciare a fine importazione wallets e dopo aver identificato un token come scam

    /**
     *Questa funzione non serve più e la rinomino
     */
    
    public static void Dismessa_ConvertiInvioSuStessoWallet(){
        Map<String,String> Mappa_CommissioniDaCancellare=new TreeMap<>();
        Map<String,String> Mappa_CommissioniPerHash=new TreeMap<>();
        Map<String,String> Mappa_MovimentiDaEliminare=new TreeMap<>();
        Map<String,String[]> Mappa_MovimentiDaCreare=new TreeMap<>();
        Map<String, String> Mappa_NomiTokenPersonalizzati = DatabaseH2.RinominaToken_LeggiTabella();
        for (String[] v : MappaCryptoWallet.values()) {
            
            
            
            //PASSO 1 - RINOMINO I TOKEN CHE DEVONO ESSERE RINOMINATI
            String Rete = Funzioni.TrovaReteDaID(v[0]);
            String AddressU = v[26];
            String AddressE = v[28];
            //if (!Funzioni.noData(Rete)) {
                if (!Funzioni.noData(AddressU)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    if (Rete==null)Rete="";
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressU + "_" + Rete);
                    if (valore != null) {
                        v[8] = valore;
                    }
                }
                if (!Funzioni.noData(AddressE)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    if (Rete==null)Rete="";
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressE + "_" + Rete);
                    if (valore != null) {
                        v[11] = valore;
                    }

                }
                      
            
            //PASSO 1A - SALVO UNA MAPPA tutti i movimenti con commissione con qta zero
            //questi infatti sono movimenti che non servono a nulla e gli eliminerò
            //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
            String TipoMovimento=v[0].split("_")[4].trim();
          /*   if ( (TipoMovimento.equalsIgnoreCase("CM")&&v[10].equalsIgnoreCase("-0"))
                     ||
                  (TipoMovimento.equalsIgnoreCase("PC")&&v[10].equalsIgnoreCase("-0"))){
                 //salvo nella mappa delle commissioni tutti gli id e come indice uso l'hash
                Mappa_MovimentiDaEliminare.put(v[0],"");
                
                //per i movimenti di prelievo mi salvo anchel'hash perchè dovrò andare a cancellare le commissioni
                if ( TipoMovimento.equalsIgnoreCase("PC")&&v[24]!=null&&!v[24].isBlank()){
                    //Salvo tutti gli hash delle commissioni che devo cancellare
                    Mappa_CommissioniDaCancellare.put(v[24], "");  
                    //System.out.println(v[24]);
            }
            }   */
             
            //PASSO 1B - Se ho un prelievo dove l'address controparte è uguale al mio wallet
            //vuol dire che mi sono autoinviato dei fondi, in quel caso il movimento va convertito in scambio
            if (TipoMovimento.equalsIgnoreCase("PC")&&v[3].split("\\(")[0].trim().equalsIgnoreCase(v[30])){
                String clone[]=v.clone();
                String partiID[]=clone[0].split("_");
                clone[0]=partiID[0]+"_"+partiID[1]+"_"+partiID[2]+"_"+partiID[3]+"_SC";
                clone[5]="SCAMBIO CRYPTO";
                clone[6]= clone[8]+" -> "+clone[8];
                clone[11]=clone[8];
                clone[12]=clone[9];
                clone[13]=clone[10].replace("-", "");
                clone[27]=clone[25];
                clone[28]=clone[26];
                //il movimento errato lo metto qua sotto in questa mappa perchè venga elimnato a fine ciclo
                Mappa_MovimentiDaEliminare.put(v[0],"");
                Mappa_MovimentiDaCreare.put(clone[0], clone);
            }
            
            
                
            //PASSO 2 - SALVO UNA MAPPA HASHCommissione_ID
            //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
         /*    if ( TipoMovimento.equalsIgnoreCase("CM")){
                 //salvo nella mappa delle commissioni tutti gli id e come indice uso l'hash
                Mappa_CommissioniPerHash.put(v[24],v[0]);
            }*/
             
             
             
             
             
             //PASSAO 3 - SALVO UNA MAPPA CON LA LISTA DELLE COMMISSIONI DA ELIMINARE per HASH
             //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
        /*    if ( TipoMovimento.equalsIgnoreCase("PC")&&Funzioni.isSCAM(v[8])&&v[24]!=null&&!v[24].isBlank()){
               //Salvo tutti gli hash delle commissioni che devo cancellare
               Mappa_CommissioniDaCancellare.put(v[24], "");  
               //System.out.println(v[24]);
            }*/
            
            
        }
        
        //Adesso cancello le commissioni imputate erroneamente  
        //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
      /*  for (String hash : Mappa_CommissioniDaCancellare.keySet()) {
            //String TipoMovimento=v[0].split("_")[4].trim();
            String ID=Mappa_CommissioniPerHash.get(hash);
            if ( ID!=null){
                MappaCryptoWallet.remove(ID);
            }
        }*/
        
        //Adesso cancello i movimenti da eliminare     
        for (String ID : Mappa_MovimentiDaEliminare.keySet()) {

                MappaCryptoWallet.remove(ID);
            
        }
        
        //Adesso creo i movimenti da creare
        for (String ID : Mappa_MovimentiDaCreare.keySet()) {

                MappaCryptoWallet.put(ID, Mappa_MovimentiDaCreare.get(ID));
            
        }
        
    }
    
    
    
    public static boolean MovimentoRilevante(String[] Mov){
        String ID=Mov[0];
        String IDTS[]=ID.split("_");
        Moneta m[]=Moneta.RitornaMoneteDaMov(Mov);
        String Data = Mov[1];
        boolean rilevante=true;
        boolean plusvalenza=true;
        
        if (IDTS[4].equals("VC")                //Vendita Cripto Rilevante
                || IDTS[4].equals("CM")         //Commissione Rilevante
                || IDTS[4].equals("RW"))        //Rewards rilevante
        {
            rilevante=true;
            plusvalenza=true;
        }else if (IDTS[4].equals("AC"))//Acquisto Cripto Rilvente ma no Plusvalenza
                {
            rilevante=true;
            plusvalenza=false;
        }
            else if (IDTS[4].equals("DF")//deposito Fiat
                        || IDTS[4].equals("PF")//Prelievo Fiat
                        || IDTS[4].equals("SF")//Scambio Fiat
                        || IDTS[4].equals("TI"))//Trasferimento Interno
                {
            rilevante=false;
            plusvalenza=false;
        }
        else if (IDTS[4].equals("SC"))//deposito Fiat
        {
            String Tipo1 = RitornaTipoCrypto(m[0].Moneta, Data, m[0].Tipo);
            String Tipo2 = RitornaTipoCrypto(m[1].Moneta, Data, m[1].Tipo);
            if (Tipo1.equalsIgnoreCase(Tipo2)) {
                rilevante = false;
                plusvalenza=false;
            }else
                {
                rilevante = true;
                plusvalenza=true;
            }
        }
        else if (IDTS[4].equals("DC")//Deposito Crypto
                        || IDTS[4].equals("PC"))//Prelievo Crypto
                {
                    //Le tipologie possono essere le seguenti
                    //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
                    //PCO -> Cashout o similare (prelievo)
                    //PTW -> Trasferimento tra Wallet (prelievo)
                    //DTW -> Trasferimento tra Wallet (deposito)
                    //DAI -> Airdrop o similare (deposito)
                    //DCZ -> Costo di carico 0 (deposito)
                    if (Mov[18].isBlank()) {
                        //Se Mov[18] è vuoto significa che non ho classificato il movimento
                        //a questo punto devo decidere come comportarmi, se considerare il movimento rilevante o se invece non gestirlo
                        //perora lo considero rilvente quindi tratterò i prelievi come cashout e i depositi come provento da detenzione es. Staking
                        rilevante = true;
                    }
                }else if (Mov[18].contains("PWN") || Mov[18].contains("PCO")) {

                    } else if (Mov[18].contains("DAI") || Mov[18].contains("DCZ")) {
                        
                    }
        return rilevante;
    }
        
    
    
    public static String RitornaTipoCrypto(String Token,String Data,String Tipologia) {
       String Tipo=Tipologia;
       String DataEmoney=CDC_Grafica.Mappa_EMoney.get(Token);
       if(Tipologia.equalsIgnoreCase("Crypto")&&DataEmoney!=null){
           long dataemoney=OperazioniSuDate.ConvertiDatainLong(DataEmoney);
           long datascambio=OperazioniSuDate.ConvertiDatainLong(Data);
           if (datascambio>=dataemoney) Tipo="EMoney";
       }
       return Tipo;
   }
    
      public static boolean RewardRilevante(String ID) {

       String[] Mov=MappaCryptoWallet.get(ID);
       String IDTS[]=ID.split("_");
       String TipoTrasf=Mov[18].split("-")[0].trim();
       
       //Perchè sia una reward devo verificare se è classificata come tale alla fonte (RW) 
       //oppure se è stata classificata dopo quindi DAI
       if (IDTS[4].equals("RW")||
              TipoTrasf.equals("DAI") ){
           
           if (Mov[5].toUpperCase().contains("CASHBACK"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_CashBack").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("STAKING"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Staking").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("AIRDROP"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Airdrop").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("EARN"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Earn").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("REWARD"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Reward").equalsIgnoreCase("SI");
           }
           else return true;

       }
       //se non soddisfa nessuno dei casi sopra allora metto che è fiscalmente rilevante
       return true;
      

   }  
    
    
    
        public static Moneta[] RitornaMoneteDaID(String ID){
            //Moneta[0] sarà la moneta uscente
            //Moneta[1] srà quella entrante
            Moneta m[]=new Moneta[2];
            String[] Mov=MappaCryptoWallet.get(ID);
            m[0]=new Moneta();
            m[0].Moneta=Mov[8];
            m[0].MonetaAddress=Mov[26];
            m[0].Prezzo=Mov[15];
            m[0].Qta=Mov[10];
            m[0].Rete=TrovaReteDaID(ID);
            m[0].Tipo=Mov[9];
            m[1]=new Moneta();
            m[1].Moneta=Mov[11];
            m[1].MonetaAddress=Mov[28];
            m[1].Prezzo=Mov[15];
            m[1].Qta=Mov[13];
            m[1].Rete=TrovaReteDaID(ID);
            m[1].Tipo=Mov[12];
            return m;
        }
        
        public static boolean isSCAM(String Nome){
            boolean SCAM=false;
            int Lnome=Nome.length();
            //verifico se la moneta è già considerata come scam
            if (Lnome>3 && " **".equals(Nome.substring(Lnome-3, Lnome))){
                SCAM=true;
            }
            return SCAM;
        }
        
        public static Object[] Converti_String_Object(String[] riga){
            Object ritorno[]=new Object[riga.length];
           /* ritorno=riga.clone();
            ritorno[15]=new BigDecimal(riga[15]);
            ritorno[19]=new BigDecimal(riga[19]);*/
            for (int i=0;i<ritorno.length;i++){
                
                if (i==19||i==15){
                    ritorno[i]=new BigDecimal(riga[i]);
                }else
                  {
                      ritorno[i]=riga[i];
                  }  
            }
            return ritorno;
        }
        
        public static boolean noData(String Valore){
            boolean noData=false;
            //verifico se la moneta è già considerata come scam
            if (Valore==null||Valore.trim().equals("")){
                noData=true;
            }
            return noData;
        }
        
        public static String TrovaReteDaID(String ID){


        String Rete=null;
        //System.out.println(ID);
        //per trovare la rete devo scindere l'ID in più parti e verificarne alcune caratteristiche

            String IDSplittato[]=ID.split("_");
            String IDDettSplittato[]=IDSplittato[1].split("\\.");
            if ((IDDettSplittato.length==4 ||IDDettSplittato.length==5) && 
                    (IDDettSplittato[0].equalsIgnoreCase("BC")||IDDettSplittato[0].equalsIgnoreCase("00BC"))){//00BC viene usato negli scambi differiti automatici
                Rete=IDDettSplittato[1];
                return Rete;
            }
            
            //Se il primo if non trova la rete la cerco tra i movimenti manuali, a patto che venga inserito il contratto del token
         /*   if (IDSplittato[1].contains("(") && IDSplittato[1].contains(")")&& IDSplittato[1].split("\\(").length > 1) {
                String Mov[] = MappaCryptoWallet.get(ID);
                if (Mov!=null&&(!Mov[26].isEmpty() || !Mov[28].isEmpty())) {
                    Rete = IDSplittato[1].split("\\(")[1].split("\\)")[0];
                    return Rete;
                }
            }*/
         
            //Se il primo if non trova la rete la cerco tra i movimenti manuali, a patto che la chain sia supportata
            if (IDSplittato[1].contains("(") && IDSplittato[1].contains(")")&& IDSplittato[1].split("\\(").length > 1) {
               // String Mov[] = MappaCryptoWallet.get(ID);
                String ret=IDSplittato[1].split("\\(")[1].split("\\)")[0].trim();
                if (MappaRetiSupportate.get(ret)!=null) {//se è una chain supportata allra la gestisco come tale
                    Rete = ret;
                    return Rete;
                }
            }
            
        return Rete;
        }
       
        public static void CompilaMappaRetiSupportate(){
            
            MappaRetiSupportate.put("ARB", "");
            MappaRetiSupportate.put("AVAX", "");
            MappaRetiSupportate.put("BASE", "");
            MappaRetiSupportate.put("ADA", "");
            MappaRetiSupportate.put("BNB", "");
            MappaRetiSupportate.put("BSC", "");
            MappaRetiSupportate.put("BTC", "");
            MappaRetiSupportate.put("CRO", "");
            MappaRetiSupportate.put("DASH", "");
            MappaRetiSupportate.put("DOGE", "");
            MappaRetiSupportate.put("DOT", "");
            MappaRetiSupportate.put("EOS", "");
            MappaRetiSupportate.put("ETH", "");
            MappaRetiSupportate.put("FTM", "");
            MappaRetiSupportate.put("LTC", "");
            MappaRetiSupportate.put("LUNA", "");
            MappaRetiSupportate.put("MATIC", "");
            MappaRetiSupportate.put("TRX", "");
            MappaRetiSupportate.put("SOL", "");
            MappaRetiSupportate.put("XLM", "");
            MappaRetiSupportate.put("XRP", "");
            MappaRetiSupportate.put("ZEC", "");
            
            //Funzione da scrivere
        }
     
        
       public static String[] RiempiVuotiArray(String[] array){
            for (int i=0;i<array.length;i++) {
                if(array[i]==null){
                    array[i]="";
                }
            }
            return array;
        }
     
       
    
    public static boolean Funzioni_isNumeric(String str, boolean CampoVuotoContacomeNumero) {
        
        if (str==null)return false;
        //ritorna vero se il campo è vuoto oppure è un numero
        if (CampoVuotoContacomeNumero && str.isBlank()) {
            return true;
        }
        try {
            BigDecimal B = new BigDecimal(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;

    }

    public static boolean isApiKeyValidaCoincap(String ApiKey) {
        //String apiUrl = "rest.coincap.io/v3/assets?apiKey=YourApiKey (New Api)" + ID + "/history?interval=h1&start=" + timestampIniziale + "&end=" + dataFin;
        String COINCAP_URL = "https://rest.coincap.io/v3/assets/bitcoin?apiKey=";
        OkHttpClient client = new OkHttpClient();
        String url = COINCAP_URL + ApiKey;
        //System.out.println(url);
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return !json.has("error");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isApiKeyValidaEtherscan(String ApiKey) {
        String ETHERSCAN_URL = "https://api.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=";
        OkHttpClient client = new OkHttpClient();
        String url = ETHERSCAN_URL + ApiKey;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            //Risposta se ok -> {"jsonrpc":"2.0","id":83,"result":"0x14f857d"}
            //Risposta se non ok -> {"status": "0","message": "NOTOK","result": "Invalid API Key"}
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("jsonrpc") && "2.0".equals(json.get("jsonrpc").getAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isApiKeyValidaCoingecko(String ApiKey) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://api.coingecko.com/api/v3/ping").get().addHeader("accept", "application/json").addHeader("x-cg-demo-api-key", ApiKey).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("gecko_says");
        } catch (Exception e) {
            System.out.println("Trans_Solana.isApiKeyValidaCoingecko " + e.getMessage());
            return false;
        }
    }
}
