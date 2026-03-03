/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Importazioni.ColonneTabella;
import static com.giacenzecrypto.giacenze_crypto.Importazioni.RiempiVuotiArray;
import static com.giacenzecrypto.giacenze_crypto.Principale.DecimaliCalcoli;
import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Component;
import java.awt.Cursor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author lucap
 */
public class Principale_NOGUI {
    
    
    public static void GiacenzeaData_Funzione_SistemaQtanew(Component posizione,JTable TabMovimenti,String Wallet) {
        int rigaselezionata=Tabelle.Funzioni_getRigaSelezionata(TabMovimenti);
        if (rigaselezionata >= 0) {
            int scelta;
            String Moneta = TabMovimenti.getModel().getValueAt(rigaselezionata, 2).toString();
            String IDTrans = TabMovimenti.getModel().getValueAt(rigaselezionata, 8).toString();
            //Adesso recupero tipo moneta e address dalla transazione
            String mov[]=Principale.MappaCryptoWallet.get(IDTrans);
            String TipoMoneta;
            String AddressMoneta;
            if(Moneta.equals(mov[8]))
            {
                TipoMoneta=mov[9];
                AddressMoneta=mov[26];
            }
            else
            {
                //se non è il token in uscita allora è quello in ingresso
                TipoMoneta=mov[12];
                AddressMoneta=mov[28];
            }
            
            
            
            String GiacenzaAttualeS = TabMovimenti.getModel().getValueAt(rigaselezionata, 7).toString();
            String GiacNegativaPrecedente = TabMovimenti.getModel().getValueAt(rigaselezionata, 9).toString();

            long DataRiferimento;
            BigDecimal GiacenzaAttuale = new BigDecimal(GiacenzaAttualeS);
            BigDecimal GiacenzaVoluta = new BigDecimal(0);
            BigDecimal QtaNuovoMovimento;

            
            
            if (Wallet==null || !Wallet.equalsIgnoreCase("tutti")){
            if (TipoMoneta.equalsIgnoreCase("Crypto")){
            
            String m = JOptionPane.showInputDialog(posizione, "<html>Il saldo alla data selezionata è : <b>" + GiacenzaAttuale.toPlainString() + "</b> <br>"
                    + "Indicare nel riquadro sottostante la giacenza che il token <b>" + Moneta + "</b> dovrà avere al termine dell'operazione: </html>", GiacenzaVoluta);
            //  completato = m!=null; //se premo annulla nel messaggio non devo poi chiudere la finestra, quindi metto completato=false
            if (m != null) {
                m = m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                if (Principale.Funzioni_isNumeric(m, false)) {
                    GiacenzaVoluta = new BigDecimal(m);
                    if (GiacNegativaPrecedente.equals("S")){
                //Se arrivo qua vuol dire che sto cercando di modificare la giacenza di un token che ha saldi negativi precedenti
                //In questo caso emetto un messaggio di alert che avvisa che sarebbe meglio correggere queste giacenze in ordine.
                String testo = """
                <html>
                <b>Attenzione:</b><br><br>
                    &nbsp;&nbsp;&nbsp;&nbsp;
                               Stai tentando di <b>cambiare la giacenza</b> di un token che ha avuto <b>saldi negativi <u>in passato</u></b>.<br>
                    &nbsp;&nbsp;&nbsp;&nbsp;
                               Sarebbe consigliabile <u>correggere i movimenti in ordine cronologico</u> per evitare discrepanze nei calcoli.<br><br>
                    <b>Vuoi comunque proseguire con la modifica?</b>
                    </html>
                """;
                JLabel labelTesto = new JLabel(testo);
                labelTesto.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
               int risposta=JOptionPane.showOptionDialog(posizione,labelTesto, "Avviso", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Si", "No"}, "No");
            if (risposta!=0){
                return;
            } 

            }
                    
                    QtaNuovoMovimento = GiacenzaVoluta.subtract(GiacenzaAttuale);
                    String SQta = QtaNuovoMovimento.toPlainString();
                    BigDecimal ValoreMovOrigine=new BigDecimal(TabMovimenti.getModel().getValueAt(rigaselezionata, 6).toString());
                    BigDecimal QtaMovOrigine=new BigDecimal(TabMovimenti.getModel().getValueAt(rigaselezionata, 5).toString());
                    if (QtaMovOrigine.compareTo(BigDecimal.ZERO)==0)
                    {
                        LoggerGC.ScriviErrore("QtaMovOrigine=0, esco dalla funzione");
                        return;
                    }
                        BigDecimal ValoreUnitarioToken=ValoreMovOrigine.divide(QtaMovOrigine,DecimaliCalcoli+10, RoundingMode.HALF_UP).abs();
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
                        scelta = JOptionPane.showOptionDialog(posizione, Testo,
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
                            String Nota = JOptionPane.showInputDialog(posizione,
                                    "<html>Inserire un eventuale nota sul movimento :</html>", "Rettifica di Giacenza");
                            if (Nota != null) {
                                posizione.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                //adesso compilo la parte comune del movimento
                                String RTOri[] = MappaCryptoWallet.get(IDTrans);
                                DataRiferimento = FunzioniDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi aggiungo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                String RT2[] = new String[ColonneTabella];
                                RT2[0] = "";//questo può variare in caso di movimento di commissione per cui lo metto nel capitolo successivo
                                RT2[1] = RTOri[1];
                                RT2[2] = "1 di 1";
                                RT2[3] = RTOri[3];
                                RT2[4] = RTOri[4];
                                RT2[6] = Moneta + " ->";
                                RT2[8] = Moneta;
                                RT2[9] = TipoMoneta;//da prendere dalla tabella prima
                                RT2[10] = SQta;
                                Moneta M1 = new Moneta();
                                M1.Moneta = Moneta;
                                M1.MonetaAddress = AddressMoneta;
                                M1.Qta = SQta;
                                M1.Tipo = TipoMoneta;
                                M1.Rete = Funzioni.TrovaReteDaIMovimento(RTOri);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete,""));
                                Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(M1, null, DataRiferimento, M1.Rete, "");
                                if (IP!=null)RT2[40] = IP.Ritorna40();
                                if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(SQta)).setScale(2,RoundingMode.HALF_UP).abs();
                                }
                                RT2[15] = Prezzo.toPlainString();
                                RT2[21] = Nota;
                                RT2[22] = "M";
                                RT2[26] = AddressMoneta;
                                RT2[29] = RTOri[29];
                                RiempiVuotiArray(RT2);

                                String IDOriSplittato[] = RTOri[0].split("_");
                                switch (scelta) {
                                    case 1 -> {
                                        //Non Classifico Movimento 
                                        //Ciclo per creare un movimento con il primo ID libero
                                        for(int ki=1;ki<30;ki++){
                                            if (!IDOriSplittato[1].contains(".Rettifica"))
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_PC";
                                            else
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + "_1_"+ki+"_PC";
                                            if(MappaCryptoWallet.get(RT2[0])==null){
                                               break;
                                            }
                                        }
                                        RT2[5] = "PRELIEVO "+TipoMoneta.toUpperCase();
                                        RT2[18] = "";
                                    }
                                    case 2 -> {
                                        //CashOut
                                        for(int ki=1;ki<30;ki++){
                                            if (!IDOriSplittato[1].contains(".Rettifica"))
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_PC";
                                            else
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + "_1_"+ki+"_PC";
                                            if(MappaCryptoWallet.get(RT2[0])==null){                                              
                                               break;
                                            }
                                        }
                                        RT2[5] = "CASHOUT O SIMILARE";
                                        RT2[18] = "PCO - CASHOUT O SIMILARE";
                                    }
                                    case 3 -> {
                                        //Commissione                                        
                                        for(int ki=1;ki<30;ki++){
                                            if (!IDOriSplittato[1].contains(".Rettifica"))
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_CM";
                                            else
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + "_1_"+ki+"_CM";
                                            if(MappaCryptoWallet.get(RT2[0])==null){
                                               break;
                                            }
                                        }
                                        RT2[5] = "COMMISSIONE";
                                        RT2[18] = "";
                                    }
                                    case 4 -> {
                                        //Rettifica Giacenza                                       
                                        for(int ki=1;ki<30;ki++){
                                            if (!IDOriSplittato[1].contains(".Rettifica"))
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_PC";
                                            else
                                                RT2[0] = IDOriSplittato[0] + "_ZZ" + IDOriSplittato[1] + "_1_"+ki+"_PC";
                                            if(MappaCryptoWallet.get(RT2[0])==null){
                                               break;
                                            }
                                        }
                                        RT2[5] = "RETTIFICA GIACENZA";
                                        RT2[18] = "PWN - RETTIFICA GIACENZA";
                                    }
                                    default -> {
                                    }
                                }
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT2[0], RT2);

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
                        scelta = JOptionPane.showOptionDialog(posizione, Testo,
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
                            String Nota = JOptionPane.showInputDialog(posizione,
                                    "<html>Inserire un eventuale nota sul movimento :</html>", "Rettifica di Giacenza");
                            if (Nota != null) {
                                posizione.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                //adesso compilo la parte comune del movimento
                                String RTOri[] = MappaCryptoWallet.get(IDTrans);
                                String IDOriSplittato[] = RTOri[0].split("_");
                                DataRiferimento = FunzioniDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi tolgo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                //String NuovoOrario=Funzioni.DataIDtogliUnSecondo(RTOri[0].split("_")[0]);
                                String RT1[]= new String[ColonneTabella];
                                for(int ki=1;ki<30;ki++){
                                            if (!IDOriSplittato[1].contains(".Rettifica"))
                                                RT1[0] = IDOriSplittato[0] + "_00" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_DC";
                                            else
                                                RT1[0] = IDOriSplittato[0] + "_00" + IDOriSplittato[1] + "_1_"+ki+"_DC";
                                            if(MappaCryptoWallet.get(RT1[0])==null){
                                               break;
                                            }
                                        }
                                RT1[1] = RTOri[1];
                                RT1[2] = "1 di 1";
                                RT1[3] = RTOri[3];
                                RT1[4] = RTOri[4];
                                RT1[6] = " ->" + Moneta;
                                RT1[11] = Moneta;
                                RT1[12] = TipoMoneta;
                                RT1[13] = SQta;
                                Moneta M1 = new Moneta();
                                M1.Moneta = Moneta;
                                M1.MonetaAddress = AddressMoneta;
                                M1.Qta = SQta;
                                M1.Tipo = TipoMoneta;
                                M1.Rete = Funzioni.TrovaReteDaIMovimento(RTOri);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete,""));
                                Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(M1, null, DataRiferimento, M1.Rete, "");
                                if (IP!=null)RT1[40] = IP.Ritorna40();
                                if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(SQta)).setScale(2,RoundingMode.HALF_UP).abs();
                                }
                                RT1[15] = Prezzo.toPlainString();
                                RT1[21] = Nota;
                                RT1[22] = "M";
                                RT1[28] = AddressMoneta;
                                RT1[29] = RTOri[29];
                                RiempiVuotiArray(RT1);

                                switch (scelta) {
                                    case 1 -> {
                                        //Non Classifico Movimento                                
                                        RT1[5] = "DEPOSITO "+TipoMoneta.toUpperCase();
                                        RT1[18] = "";
                                    }
                                    case 2 -> {
                                        //Rendita da Capitale
                                        RT1[5] = "EARN";
                                        RT1[18] = "DAI - Provento da Detenzione";
                                    }
                                    case 3 -> {
                                        //Costo di carico 0
                                        RT1[5] = "DEPOSITO A COSTO 0";
                                        RT1[18] = "DCZ - DEPOSITO A COSTO 0";
                                    }
                                    default -> {
                                    }
                                }
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT1[0], RT1);

                            }
                        }


                    }
                    
                    //Avviso il programma che devo anche aggiornare la tabella crypto e ricalcolare le plusvalenze
                        
                    //Aggiorno tutto in un thread separato così viene fatto tutto in backgroud intanto che 
                    //Viene premuto sul messaggio di conferma
                 //   new Thread(() -> {
                        Principale.TabellaCryptodaAggiornare=true;
                  //  }).start();
                    //Adesso avviso che il movimento è inserito e ricarico l'intera pagina
                    posizione.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    if (scelta != 0 && scelta != -1) {
                        JOptionPane.showConfirmDialog(posizione, """
                                                            Movimento di rettifica generato con successo!
                                                            Ricordarsi di salvare i movimenti nella sezione 'Transazioni Crypto'.""",
                                "Movimento Creato", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                        

                        
                        //DA FARE!!!!!!
                        //Ora sistemo i valori sulla tabella principale
                        //Devo ricalcolare la tabella principale
                        //Ricaricare i dettagli e riposizionare il tutto
                        //E ricarico la tabella secondaria

                        // GiacenzeaData_CompilaTabellaToken();
                        
                        //Tra le altre cose devo anche ricalcolare l'RW qualora sia stato già calcolato
                    }
                } else {
                    //  completato=false;
                    JOptionPane.showConfirmDialog(posizione, "Attenzione, " + m + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
            }
        }else{
                    //  completato=false;
                    JOptionPane.showConfirmDialog(posizione, """
                                                        Questo tipo di operazione \u00e8 consentita solo per le Crypto.
                                                        Per NFT e FIAT utilizzare l'inserimento manuale.""",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
            }else{
                    //  completato=false;
                    JOptionPane.showConfirmDialog(posizione, """
                                                        Questo tipo di operazione \u00e8 consentita solo sui singoli Wallet.
                                                        Selezionare un sigolo Wallet dal men\u00f9 a tendina in alto.""",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                }
        }
        posizione.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
}
