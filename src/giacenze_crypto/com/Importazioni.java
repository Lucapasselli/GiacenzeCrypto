/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static giacenze_crypto.com.CDC_Grafica.Funzioni_Date_ConvertiDatainLong;
import java.util.Collections;
import java.awt.Component;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author luca.passelli
 */
public class Importazioni {
    
    public static int Transazioni=0;
    public static int TransazioniAggiunte=0;
    public static int TrasazioniScartate=0;
    public static int TrasazioniSconosciute=0;
    public static int ColonneTabella=31;
    
    
    public static String movimentiSconosciuti="";
    
    public static void AzzeraContatori()
            {
                Transazioni=0;
                TransazioniAggiunte=0;
                TrasazioniScartate=0;
                TrasazioniSconosciute=0;
                movimentiSconosciuti="";
            }
           
    
    //23->Blocco Transazione
    //24->Hash Transazione
    //25->Nome Moneta Uscita
    //26->Contratto Moneta Uscita
    //27->Nome Moneta Entrata
    //28->Contratto Moneta Entrata
    //29->Timestamp
    //
    
    
    public static void Importa_Crypto_CDCApp(String fileCDCapp,boolean SovrascriEsistenti) {
        
        AzzeraContatori();
        
        String fileDaImportare = fileCDCapp;
        Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //Faccio una lista di causali per la conversione dei dati del csv
        Mappa_Conversione_Causali.put("card_cashback_reverted", "CASHBACK");              //Cashback ripristinato
        Mappa_Conversione_Causali.put("referral_card_cashback", "CASHBACK");              //Cashback della Carta MCO
        Mappa_Conversione_Causali.put("crypto_earn_interest_paid", "EARN");               //Interessi maturati da una Crypto in Earn
        Mappa_Conversione_Causali.put("crypto_earn_program_created", "TRASFERIMENTO-CRYPTO-INTERNO");//Inserimento di una Crypto in Earn
        Mappa_Conversione_Causali.put("crypto_earn_program_withdrawn", "TRASFERIMENTO-CRYPTO-INTERNO");//Prelievo di una Crypto dall'Earn
        Mappa_Conversione_Causali.put("crypto_exchange", "SCAMBIO CRYPTO-CRYPTO");        //Scambio di una Crypto per un'altra Crypto
        Mappa_Conversione_Causali.put("crypto_deposit", "TRASFERIMENTO-CRYPTO");          //Deposito di Crypto provenienti da wallet esterno
        Mappa_Conversione_Causali.put("crypto_payment", "PAGAMENTO-CRYPTO");              //Pagamento in Crypto (Es.: Crypto Pay in CRO)
        Mappa_Conversione_Causali.put("crypto_purchase", "ACQUISTO CRYPTO");          //Acquisto di Crypto da Carta di Credito
        Mappa_Conversione_Causali.put("crypto_to_exchange_transfer", "TRASFERIMENTO-CRYPTO");//Trasferimento di una Crypto dall'App verso l'Exchange
        Mappa_Conversione_Causali.put("crypto_viban_exchange", "VENDITA CRYPTO");    //Vendita di una Crypto verso il portafoglio EUR
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_credited", fileDaImportare);    //Scambio MCO in CRO (MCO liberi nel portafoglio). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_debited", fileDaImportare);     //Scambio MCO in CRO (MCO liberi nel portafoglio). Vendita degli MCO
        Mappa_Conversione_Causali.put("crypto_withdrawal", "TRASFERIMENTO-CRYPTO");       //Prelievo di una Crypto verso portafogli esterni
        Mappa_Conversione_Causali.put("dust_conversion_credited", "DUST-CONVERSION");//Conversione di Crypto in CRO. CRO Ricevuti dalla conversione.
        Mappa_Conversione_Causali.put("dust_conversion_debited", "DUST-CONVERSION");//Conversione di Crypto in CRO. Crypto da convertire in CRO.
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_bonus_exchange_deposit", fileDaImportare);//Bonus Swap MCO/CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_credited", fileDaImportare);     //Scambio MCO in CRO (MCO in Earn). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_debited", fileDaImportare);      //Scambio MCO in CRO (MCO in Earn). Vendita degli MCO
        Mappa_Conversione_Causali.put("exchange_to_crypto_transfer", "TRASFERIMENTO-CRYPTO");    //Trasferimenti dall'Exchange verso l'App
        Mappa_Conversione_Causali.put("lockup_lock", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo Stake
//        Mappa_Conversione_Causali.put("lockup_swap_credited", fileDaImportare);         //Scambio MCO in CRO (MCO in Stake per la Carta). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("lockup_swap_debited", fileDaImportare);          //Scambio MCO in CRO (MCO in Stake per la Carta). Vendita degli MCO
        Mappa_Conversione_Causali.put("lockup_upgrade", "TRASFERIMENTO-CRYPTO-INTERNO");       //CRO Stake per la MCO Card. (Upgrade)
        Mappa_Conversione_Causali.put("mco_stake_reward", "STAKING");                       //Interessi che la MCO Card matura. Da (Jade in su)
        Mappa_Conversione_Causali.put("pay_checkout_reward", "REWARD");                   //Ricompesa di Crypto.com Pay
        Mappa_Conversione_Causali.put("referral_gift", "REWARD");                         //Bonus di iscrizione sbloccato
        Mappa_Conversione_Causali.put("reimbursement", "REWARD");                         //Rimborsi (Es. Netflix, Promozioni)
        Mappa_Conversione_Causali.put("reimbursement_reverted", "REWARD");                //Annullamento di un rimborso (o parte)
        Mappa_Conversione_Causali.put("supercharger_deposit", "TRASFERIMENTO-CRYPTO-INTERNO"); //Deposito dei CRO nel supercharger
        Mappa_Conversione_Causali.put("supercharger_withdrawal", "TRASFERIMENTO-CRYPTO-INTERNO");//Prelievo dei CRO dal supercharger
        Mappa_Conversione_Causali.put("viban_purchase", "ACQUISTO CRYPTO");           //Acquisto di Crypto dal portafoglio EUR 
//        Mappa_Conversione_Causali.put("nft_payout_credited", fileDaImportare);            //Vendita NFT 
        Mappa_Conversione_Causali.put("staking_reward", "STAKING");                       //Reward (Es. NEO Gas) 
        Mappa_Conversione_Causali.put("campaign_reward", "REWARD");                       //Vincita di una campagna (Es.: Telegram Madness
        Mappa_Conversione_Causali.put("crypto_payment_refund", "REWARD");                 //Rimborso in Crypto. (Es. Rimborso Offerta per un NFT)
        Mappa_Conversione_Causali.put("referral_bonus", "REWARD");                        //Bonus Referral 
        Mappa_Conversione_Causali.put("crypto_earn_extra_interest_paid", "REWARD");//Earn Extra Reward 
        Mappa_Conversione_Causali.put("supercharger_reward_to_app_credited", "REWARD");//Supercharger Reward in App
        Mappa_Conversione_Causali.put("rewards_platform_deposit_credited", "REWARD");//Mission Reward
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_fund_lock", "TRASFERIMENTO-CRYPTO-INTERNO");//Blocca i fondi destinati ad un'ordine Limit 
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_exchange", "SCAMBIO CRYPTO-CRYPTO");//Ordine Limite Eseguito         
//        Mappa_Conversione_Causali.put("crypto_credit_withdrawal_created", fileDaImportare);//Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_repayment_created", fileDaImportare);//Crypto Loan        
//        Mappa_Conversione_Causali.put("crypto_credit_loan_credited", fileDaImportare);    //Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_program_created", fileDaImportare);  //Crypto Loan 
        Mappa_Conversione_Causali.put("admin_wallet_credited", "ALTRE-REWARD");//es. aggiustamenti luna  

        
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
       // Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String riga;
        String ultimaData = "";
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
        List<String> righeFile = new ArrayList<>();

            //in questo modo butto tutto il file in un array che poi verrà riordinato in modo da avere i movimenti in ordine di data
            while ((riga = bure.readLine()) != null) {
                righeFile.add(riga);
                }
            Collections.sort(righeFile);
            Collections.sort(righeFile);
            
            for (int w=0;w<righeFile.size();w++){
                
                riga=righeFile.get(w);
                //System.out.println(riga);
                String splittata[] = riga.split(",");
                if (Funzioni_Date_ConvertiDatainLong(splittata[0]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                {
                    //se trovo movimento con stessa data e ora lo aggiungo alla lista che compone il movimento e vado avanti
                    if (splittata[0].equalsIgnoreCase(ultimaData)) {
                        listaMovimentidaConsolidare.add(riga);
                    } else //altrimenti consolido il movimento precedente
                    {
                       // System.out.println(riga);
                        List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
                        int nElementi = listaConsolidata.size();
                        for (int i = 0; i < nElementi; i++) {
                            String consolidata[] = listaConsolidata.get(i);
                            Mappa_Movimenti.put(consolidata[0], consolidata);
                        }

                        //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                        listaMovimentidaConsolidare = new ArrayList<>();
                        listaMovimentidaConsolidare.add(riga);
                    }
                    ultimaData = splittata[0];

                }

            }
            List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                //System.out.println(consolidata[2].split(" di ")[0].trim());               
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }

         //   bure.close();
          //  fire.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }     

        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {

               MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        
    }
    
    
    
        public static String Formatta_Data_CoinTracking(String Data) {

        String DataFormattata="";
            try {
            SimpleDateFormat originale = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            
            Date d = originale.parse(Data+":30");
            originale.applyPattern("yyyy-MM-dd HH:mm:ss");
            DataFormattata = originale.format(d);
        } catch (ParseException ex) {
          //  Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            return DataFormattata;
        }
           // System.out.println(newDateString);
            return DataFormattata;
    }
        
        public static String Formatta_Data_UTC(String Data) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(Data, formatter)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.of("Europe/Rome"))
            .format(formatter);

    }    
        
    
    
        public String createMD5Hash(final String input)
           throws NoSuchAlgorithmException {

      String hashtext = null;
      MessageDigest md = MessageDigest.getInstance("MD5");

      // Compute message digest of the input
      byte[] messageDigest = md.digest(input.getBytes());

      hashtext = convertToHex(messageDigest);

      return hashtext;
   }

   private String convertToHex(final byte[] messageDigest) {
      BigInteger bigint = new BigInteger(1, messageDigest);
      String hexText = bigint.toString(16);
      while (hexText.length() < 32) {
         hexText = "0".concat(hexText);
      }
      return hexText;
   }
        
        
    
public static boolean Importa_Crypto_CoinTracking(String fileCoinTracking,boolean SovrascriEsistenti,String Exchange,Component c,boolean PrezzoZero,Download progressb ) {
        

         //   Download progressb=new Download();
  /*                 SwingUtilities.invokeLater(() -> {
           // ProgressBarExample example = new ProgressBarExample();
  Download progressb=new Download();
  progressb.SetMassimo(100);
            // Esempio di aggiornamento della progress bar dalla funzione main
            for (int i = 0; i <= 100; i++) {
                final int progress = i;
                SwingUtilities.invokeLater(() -> progressb.updateProgress(progress));
                try {
                    Thread.sleep(100); // Aggiorna la progress bar ogni 100 millisecondi
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });*/
    
  
 AzzeraContatori();        
        String fileDaImportare = fileCoinTracking;

        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        
        //come prima cosa creo una mappa con i movimenti per poi averli in ordine di data e così elimino anche i movimenti doppi
        //ho infatti notato che importando i dati ad esempio da binance, cointracking crea movimenti doppi
        Map<String, String> Mappa_MovimentiTemporanea = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
               String riga;


            try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
                while ((riga = bure.readLine()) != null) {
                    riga=riga.replaceAll("\"", "");
                    String splittata[] = riga.split(",");
                    if (splittata.length==13){
                        String data = Formatta_Data_CoinTracking(splittata[12]);
                        if (!data.equalsIgnoreCase("")) {
                            if (Mappa_MovimentiTemporanea.get(riga) == null) {
                                Mappa_MovimentiTemporanea.put(data+" "+riga, riga);
                                //System.out.println(riga);
                            } else {
                            //System.out.println("Movimento doppio - " + riga);
                            }
                        }
                    }
                }
           //     bure.close();
           // fire.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }     
        
        
        
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //devo prima formattare le date affinche risultino nel formato corretto
        
      //  String riga;
        String ultimaData = "";
        //Iterator<String> iteratore=Mappa_MovimentiTemporanea.keySet().iterator();
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
      progressb.SetMassimo(Mappa_MovimentiTemporanea.size());
        int avanzamento=0;
        for (String str : Mappa_MovimentiTemporanea.keySet()) {
            if (progressb.FineThread()){
            //se è stato interrotta la finestra di progresso interrompo il ciclo
                return false;
                }
            riga=Mappa_MovimentiTemporanea.get(str);
           // System.out.println(riga);
            String splittata[] = riga.split(",");
            String data=Formatta_Data_CoinTracking(splittata[12]);
            //System.out.println(data+" "+ultimaData);
            if (Funzioni_Date_ConvertiDatainLong(data) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
            {
                //se trovo movimento con stessa data e ora lo aggiungo alla lista che compone il movimento e vado avanti
                if (data.equalsIgnoreCase(ultimaData)) {
                    listaMovimentidaConsolidare.add(riga);
                    //System.out.println(splittata[12]);
                } else //altrimenti consolido il movimento precedente
                {
                     //System.out.println(listaMovimentidaConsolidare.size());
                    
                    List<String[]> listaConsolidata = ConsolidaMovimenti_CoinTracking(listaMovimentidaConsolidare,Exchange,PrezzoZero);
                    int nElementi = listaConsolidata.size();
                    for (int i = 0; i < nElementi; i++) {
                    String consolidata[] = listaConsolidata.get(i);
                    Mappa_Movimenti.put(consolidata[0], consolidata);
                    }
                    
                    //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                    listaMovimentidaConsolidare = new ArrayList<>();
                    listaMovimentidaConsolidare.add(riga);
                }
                ultimaData = Formatta_Data_CoinTracking(splittata[12]);
                
            }
            avanzamento++;
             progressb.SetAvanzamento(avanzamento);
        }
            
            List<String[]> listaConsolidata = ConsolidaMovimenti_CoinTracking(listaMovimentidaConsolidare,Exchange,PrezzoZero);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                //System.out.println(consolidata[2].split(" di ")[0].trim());               
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }




        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {

               MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        Calcoli.ScriviFileConversioneXXXEUR();
        
        return true;
       
                   
                 
            
        
       
        
    }
    
    
    
        public static String[] RiempiVuotiArray(String[] array){
            for (String v : array) {
                if(v==null){
                    v="";
                }
            }
            return array;
        }
        
        
    
    
    
        public static void Scrivi_Movimenti_Crypto(Map<String, String[]> Mappa_Movimenti) {
         try { 
            FileWriter w=new FileWriter("movimenti.crypto.db");
            BufferedWriter b=new BufferedWriter (w);
       for (String[] v : Mappa_Movimenti.values()) {
           String riga="";
                for (String v1 : v) {
                    riga = riga + v1 + ";";
                }
       //   riga=v[0]+";"+v[1]+";"+v[2]+";"+v[3]+";"+v[4]+";"+v[5]+";"+v[6]+";"+v[7]+";"+v[8]+";"+v[9]+";"+v[10]+";"+v[11]+";"+v[12]+";"+v[13]+";"+v[14]+";"+v[15]+";"+v[16]+";"+v[17]+";"+v[18]+";"+v[19]+";"+v[20]+";"+v[21]+";"+v[22];

           b.write(riga+"\n");

       }
       b.close();
       w.close();
    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
    }
        
        
  /*  private static void Leggi_Movimenti_Crypto() {///da finireeeeeeeeeeeeeeeee
        try {
            FileReader w = new FileReader("movimenti.crypto.csv");
            BufferedReader bure = new BufferedReader(w);
            String riga;
            while ((riga = bure.readLine()) != null) {
                String splittata[] = riga.split(";");
            }
            bure.close();
            w.close();
        } catch (IOException ex) {
            //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
     public static List<String[]> ConsolidaMovimenti_CDCAPP(List<String> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Crypto
         //DC=Deposito Crypto
         //PC=Prelievo Crypto
         //TF=Trasferimento Fiat
         //AC=Acquisto Crypto (con FIAT)
         //VC=Vendita Crypto (per FIAT)
         //SC=Scambio Crypto
         //RW=Staking/caschback/airdrop etc....
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
         String dust_accreditati="";
         String dust_addebitati[]=new String[1];
         if (numMovimenti>1) {dust_addebitati=new String[numMovimenti-1];}
         String dust_sommaaddebiti="0";
         int numeroAddebiti=0;
                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                            String data=movimentoSplittato[0];
                            data=Formatta_Data_UTC(data);
                            String dataa=data.trim().substring(0, data.length()-3);
                             //   String inputValue = "2012-08-15T22:56:02.038Z";

                            String movimentoConvertito=Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                           // System.out.println(movimentoSplittato[9]);
                           if (movimentoConvertito==null)
                                {
                                //   System.out.println("Errore in importazione da CDCAPP csv: "+movimento);
                                   movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                   TrasazioniSconosciute++;
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD"))
                            {

                                //System.out.println(movimentoSplittato[0].replaceAll(" |-|:", ""));
                                RT[0] = data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_RW";
           // System.out.println(RT[0]);
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = "Crypto.com App";
                                RT[4] = "Crypto Wallet";
                                RT[5] = Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[9] + "(" + movimentoSplittato[1] + ")";
                                
                                if (movimentoSplittato[3].contains("-")) {
                                    RT[5] = "RIMBORSO " + Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                    RT[6] = movimentoSplittato[2]+" ->"; 
                                    RT[8] = movimentoSplittato[2];
                                    RT[9] = "Crypto";
                                    RT[10] = movimentoSplittato[3];
                                    RT[11] = "";
                                    RT[12] = "";
                                    RT[13] = "";
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                                    RT[15] = "0.00";
                                    RT[16] = "";
                                    RT[17] = "Da Calcolare";//verrà calcolato con il metodo lifo
                                    RT[18] = "";//verrà calcolato con il metodo lifo
                                    RT[19] = "Da Calcolare";//verrà calcolato con il metodo lifo sarà (0 - prezzo di carico)
                                } 
                                else 
                                {
                                    RT[8] = "";
                                    RT[9] = "";
                                    RT[10] = "";
                                    RT[11] = movimentoSplittato[2];
                                    RT[12] = "Crypto";
                                    RT[13] = movimentoSplittato[3];
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                                    String valoreEuro = "0";
                                    
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) {
                                        valoreEuro = movimentoSplittato[7];
                                    }
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD")) {
                                        valoreEuro = Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);                                        
                                    }
                                    //System.out.println("-"+valoreEuro+"-");
                                    //valoreEuro=new BigDecimal(1).toString();
                                 //   if (valoreEuro!=null){
                                    valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).toString();
                                 //   }
                                 //   else {
                                  //      valoreEuro="0";
                                     //   System.out.println(movimentoSplittato[7]);
                                     //   System.out.println(data.split(" ")[0]);
                                      //  System.out.println("------");
                                //    }
                                    RT[15] = valoreEuro;
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[3]);
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                 //   RT[16] = "";
                                    RT[17] = valoreEuro;
                                 //   RT[18] = "";
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                               /* RT[20] = "";
                                RT[21] = "";*/
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO"))
                            {
                                //trasferimento FIAT
                                
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_DF";
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";                                
                                RT[5]="DEPOSITO FIAT";
                                String valoreEuro="0";
                                if (movimentoSplittato[9].trim().equalsIgnoreCase("viban_purchase")){
                                    if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                    if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], data.split(" ")[0]);
                                    }
                                    valoreEuro=new BigDecimal(valoreEuro).abs().toString(); 
                                    
                                    RT[6]="-> "+movimentoSplittato[2]; 
                                    RT[11]=movimentoSplittato[2]; 
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                    RT[14]=movimentoSplittato[2]+" "+movimentoSplittato[3];
                                }
                                else 
                                    {
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                    valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                    
                                    RT[6]="-> EUR";
                                    RT[11]="EUR";
                                    RT[13]=valoreEuro;
                                    RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                    
                                    }
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                                                   
                                RT[12]="FIAT";
                                RT[15]=valoreEuro;
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[22]="A";                              
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                                //Vendita Euro x Crypto
                                //movimentoSplittato[0].replaceAll(" |-|:", "") +"_"+String.valueOf(k+1)+ "_CDCAPP_AC_*_"+movimentoSplittato[2].trim();
                                RT=new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_AC";                              
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="ACQUISTO CRYPTO";
                                if (movimentoSplittato[9].trim().equalsIgnoreCase("viban_purchase"))
                                {
                                    RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];
                                    RT[8]=movimentoSplittato[2];
                                    RT[10]=new BigDecimal(movimentoSplittato[3]).toString();
                                    RT[11]=movimentoSplittato[4];
                                    RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();
                                    
                                }
                                else
                                {
                                    RT[6]="EUR -> "+movimentoSplittato[2];
                                    RT[8]="EUR";
                                    RT[10]="-"+valoreEuro;
                                    RT[11]=movimentoSplittato[2];
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                    RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                }
                                                              
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                               
                                RT[9]="FIAT";                                                                                              
                                RT[12]="Crypto";
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("VENDITA CRYPTO"))
                            {
                                //Vendita Crypto x Euro
                                
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_VC"; 
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="VENDITA CRYPTO";
                                RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="FIAT";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];///////
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);   
                                
                                
                                //trasferimento FIAT
                                RT=new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_PF"; 
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="PRELIEVO FIAT";
                                RT[6]=movimentoSplittato[4]+" ->";
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[4];
                                RT[9]="FIAT";
                                RT[10]="-"+new BigDecimal(movimentoSplittato[5]).abs().toString();
                                RT[11]="";                                                               
                                RT[12]=""; 
                                RT[13]=""; 
                                RT[14]=movimentoSplittato[4]+" "+movimentoSplittato[5];///////
                                valoreEuro="";
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[5];
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[5], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();                                
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO-CRYPTO"))
                            {
                                //Scambio Crypto Crypto
                                
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_SC";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="SCAMBIO CRYPTO";
                                RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];///////
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().setScale(2, RoundingMode.HALF_UP).toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);     
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DUST-CONVERSION"))
                            {
                                // serve solo per il calcolo della percentuale di cro da attivare

                                    // se è un movimento negativo lo inserisco tra gli addebiti
                                    if (movimentoSplittato[3].contains("-")){
                                        dust_addebitati[numeroAddebiti]=movimento;
                                        dust_sommaaddebiti=new BigDecimal(dust_sommaaddebiti).abs().add(new BigDecimal(movimentoSplittato[7])).abs().toString();
                                       // System.out.println(dust_sommaaddebiti+ " "+movimentoSplittato[7]);
                                        numeroAddebiti++;
                                    }
                                    else
                                    {
                                       dust_accreditati=movimento;
                                    }   
                                
                           // se è l'ultimo movimento allora creo anche le righe
                                if (k==numMovimenti-1){
                                  // System.out.println(movimento);
                                 //  System.out.println(numeroAddebiti);
                                    for (int w = 0; w < numeroAddebiti; w++) {
                                        String splittata[]=dust_addebitati[w].split(",");
                                        RT = new String[ColonneTabella];
                                        RT[0] = data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_"+String.valueOf(w+1)+"_SC";
                                        RT[1] = dataa;
                                        RT[2] = w+1 + " di " + numeroAddebiti;
                                        RT[3] = "Crypto.com App";
                                        RT[4] = "Crypto Wallet";
                                        RT[5] = "SCAMBIO CRYPTO";
                                        RT[6] = splittata[2]+" -> "+dust_accreditati.split(",")[2];//da sistemare con ulteriore dettaglio specificando le monete trattate                                        
                                        RT[7] = splittata[9] + "(" + splittata[1] + ")";
                                        RT[8] = splittata[2];
                                        RT[9] = "Crypto";
                                        RT[10] = splittata[3];
                                        RT[11] = dust_accreditati.split(",")[2];
                                        RT[12] = "Crypto";
                                        BigDecimal valoreTrans=new BigDecimal(splittata[7]);
                                        BigDecimal sumAddebiti;
                                        //System.out.println(dust_sommaaddebiti);
                                        if (new BigDecimal(dust_sommaaddebiti).compareTo(new BigDecimal("0"))!=0){
                                            sumAddebiti=new BigDecimal(dust_sommaaddebiti);
                                        }else
                                            {
                                               sumAddebiti=new BigDecimal("0.000000001"); 
                                            }
                                        BigDecimal totCRO=new BigDecimal(dust_accreditati.split(",")[3]);
                                        BigDecimal operazione;                                        
                                        operazione=(valoreTrans.divide(sumAddebiti,8, RoundingMode.HALF_UP));
                                        String numCRO=operazione.multiply(totCRO).stripTrailingZeros().abs().toString();//da sistemare calcolo errato
                                        RT[13] =numCRO;//dust_accreditati.split(",")[3];//bisogna fare i calcoli
                                        RT[14] = splittata[6] + " " + splittata[7];///////
                                        String valoreEuro = "";
                                        if (splittata[6].trim().equalsIgnoreCase("EUR")) {
                                            valoreEuro = splittata[7];
                                        }
                                        if (splittata[6].trim().equalsIgnoreCase("USD")) {
                                            valoreEuro = Calcoli.ConvertiUSDEUR(splittata[7], splittata[0].split(" ")[0]);
                                        }
                                        valoreEuro = new BigDecimal(valoreEuro).abs().setScale(2, RoundingMode.HALF_UP).toString();
                                        RT[15] = valoreEuro;
                                        RT[16] = "";
                                        RT[17] = "Da calcolare";
                                        RT[18] = "";
                                        RT[19] = "0.00";
                                        RT[20] = "";
                                        RT[21] = "";
                                        RT[22] = "A";
                                        RiempiVuotiArray(RT);
                                        lista.add(RT);
                                    }
                                }
                                }
                                else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO-INTERNO"))
                            {
                               
                                //come prima cosa devo individuare il portafoglio nel quale vanno i token
                                String WalletPartenza="";
                                String WalletDestinazione="";
                                if (movimentoSplittato[9].toLowerCase().contains("supercharger"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Supercharger";
                                    }
                                    else
                                    {
                                        WalletPartenza="Supercharger";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }else if (movimentoSplittato[9].toLowerCase().contains("earn"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Earn";
                                    }
                                    else
                                    {
                                        WalletPartenza="Earn";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }else if (movimentoSplittato[9].toLowerCase().contains("lock"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Fondi Bloccati";
                                    }
                                    else
                                    {
                                        WalletPartenza="Fondi Bloccati";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }
                                else {
                                    System.out.println(movimento);
                                }
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]=WalletPartenza;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[6]=movimentoSplittato[2]+" -> ";                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                if (movimentoSplittato[3].contains("-")) RT[10]=movimentoSplittato[3]; else RT[10]="-"+movimentoSplittato[3];                                 
                                RT[11]="";
                                RT[12]="";
                                RT[13]="";                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                                
                                
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_TI";
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]=WalletDestinazione;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[6]=" -> "+movimentoSplittato[2];                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]="";
                                RT[9]="";
                                RT[10]="";                                 
                                RT[11]=movimentoSplittato[2];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                                
                                
                                
                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO"))
                            {
                                RT = new String[ColonneTabella];
                            //    RT[0]=movimentoSplittato[0].replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_TC";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                             //   RT[5]="TRASFERIMENTO CRYPTO";
                            //    RT[6]="TRASFERIMENTO CRYPTO";                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                if (movimentoSplittato[3].contains("-")) {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_PC";
                                    RT[5]="PRELIEVO CRYPTO";
                                    RT[6]=movimentoSplittato[2]+" ->";
                                    RT[8]=movimentoSplittato[2];
                                    RT[9]="Crypto";
                                    RT[10]=movimentoSplittato[3];
                                    RT[11]="";
                                    RT[12]="";
                                    RT[13]="";
                                } else {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_DC";
                                    RT[5]="DEPOSITO CRYPTO";
                                    RT[6]="-> "+movimentoSplittato[2];
                                    RT[8]="";
                                    RT[9]="";
                                    RT[10]="";
                                    RT[11]=movimentoSplittato[2];
                                    RT[12]="Crypto";
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();                               
                                }                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                if (movimentoSplittato.length>10) RT[21]="Trans ID : "+movimentoSplittato[10];else RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                            }
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
        return lista;
    }   
    
        public static List<String[]> ConsolidaMovimenti_CoinTracking(List<String> listaMovimentidaConsolidare,String Exchange,boolean PrezzoZero){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Crypto
         //DC=Deposito Crypto
         //PC=Prelievo Crypto
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //TF=Trasferimento Fiat
         //AC=Acquisto Crypto (con FIAT)
         //VC=Vendita Crypto (per FIAT)
         //SC=Scambio Crypto
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
         
       /*              String splittata[] = riga.split(",");
            String data=Formatta_Data_CoinTracking(splittata[12]);*/
         
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();


                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                           // System.out.println(movimentoSplittato[9]);
                            String data=Formatta_Data_CoinTracking(movimentoSplittato[12]);
                            String dataa=data.substring(0, data.length()-3).trim();
                            if (movimentoSplittato[0].trim().equalsIgnoreCase("Trade"))
                            {
                                if (!movimentoSplittato[6].trim().equalsIgnoreCase("EUR")&&!movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))
                                    {
                                        // in Questo caso si tratta di uno scambio Crypto-Crypto
                                //System.out.println(movimentoSplittato[0].replaceAll(" |-|:", ""));
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_SC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "SCAMBIO CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "Crypto";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                String prezzoTrans;
                                if (new BigDecimal(movimentoSplittato[4]).compareTo(new BigDecimal("0"))==0) prezzoTrans=movimentoSplittato[8];
                                else prezzoTrans=movimentoSplittato[4];
                                RT[14] = "EUR "+prezzoTrans;
                                //questo è il primo movimento fatto, mancano gli altri
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";//verrà calcolato con il metodo lifo
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }else if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))
                                    {
                                        //in questo caso abbiamo un Acquisto Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_AC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "ACQUISTO CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "FIAT";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                String prezzoTrans=new BigDecimal(movimentoSplittato[5]).setScale(2, RoundingMode.HALF_UP).toString();
                                RT[14] = "EUR "+movimentoSplittato[5];
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                        
                                    }
                                
                            else if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))
                                    {
                                        //in questo caso abbiamo una Vendita Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_VC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "VENDITA CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "Crypto";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "FIAT";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[1];
                                String prezzoTrans=new BigDecimal(movimentoSplittato[1]).setScale(2, RoundingMode.HALF_UP).toString();
                                RT[15] =  Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";//verrà calcolato con il metodo lifo
                                RT[18] = "";
                                RT[19] = "Da calcolare";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                    }
                            }
                           else if (movimentoSplittato[0].trim().equalsIgnoreCase("Other Income"))
                            {
                                //Rewards di vario tipo
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "ALTRE-REWARD";
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = "";
                                RT[9] = "";
                                RT[10] = "";
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[4];
                                String prezzoTrans= movimentoSplittato[4];
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = RT[15];
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Staking"))
                            {
                                //Rewards di vario tipo
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "STAKING";
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = "";
                                RT[9] = "";
                                RT[10] = "";
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[4];
                                String prezzoTrans= movimentoSplittato[4];
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = RT[15];
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Other Fee"))
                            {
                                //Commissioni
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_CM";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5]="COMMISSIONE";
                                RT[6]="Commissione in "+movimentoSplittato[6];//da sistemare con ulteriore dettaglio specificando le monete trattate                                                               
                                RT[7] = movimentoSplittato[0];                                
                                RT[8] = movimentoSplittato[6];
                                String TipoMoneta="Crypto";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))TipoMoneta="FIAT";
                                RT[9] = TipoMoneta;
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = "";
                                RT[12] = "";
                                RT[13] = "";
                                RT[14] = "EUR "+movimentoSplittato[8];
                                String prezzoTrans= movimentoSplittato[8];
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";
                                RT[18] = "";
                                RT[19] = "Da calcolare";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                                              
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Spesa"))
                            {
                                //Commissioni
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PF";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5]="PRELIEVO FIAT";
                                RT[6]=movimentoSplittato[6]+" ->";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                               
                                RT[7] = movimentoSplittato[0];                                
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "FIAT";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = "";
                                RT[12] = "";
                                RT[13] = "";
                                RT[14] = "EUR "+movimentoSplittato[5];
                                String prezzoTrans= new BigDecimal(movimentoSplittato[5]).setScale(2, RoundingMode.HALF_UP).toString();
                                RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                                              
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Prelievo")||movimentoSplittato[0].trim().equalsIgnoreCase("Deposito"))
                            {
                                //Trasferimenti Crypto, ora bisognerà eliminare quelli doppi (prelievo e deposito stessa cifra e stesso wallet) 
                                //e discernere i trasferimenti tra wallet da quelli interni all'exchange
                                
 
                                // serve solo per il calcolo della percentuale di cro da attivare
                       //  for (int k=0;k<numMovimenti;k++){
                       //     String RT[]=new String[23];
                       //     String movimento=listaMovimentidaConsolidare.get(k);
                                
                           // se è l'ultimo movimento allora creo anche le righe
                                
                                  // System.out.println(movimento);
                                 //  System.out.println(numeroAddebiti);
                                String TipoMov="TRASFERIMENTO CRYPTO";
                                String TipoMovAbbreviato="TC";
                                boolean trovatacorrispondenza=false;
                                boolean annullato=false;
                                for (int w = 0; w < numMovimenti; w++) {
                                    String mov=listaMovimentidaConsolidare.get(w);
                                    String movSplit[]=mov.split(",");
                                 //   System.out.println(movSplit[0]+" "+numMovimenti);
                                    //controllo se trovo un movimento di deposito e una controparte di prelievo
                                    if ((movimentoSplittato[0].trim().equalsIgnoreCase("Deposito")&&movSplit[0].trim().equalsIgnoreCase("Prelievo"))||
                                            (movimentoSplittato[0].trim().equalsIgnoreCase("Prelievo")&&movSplit[0].trim().equalsIgnoreCase("Deposito")))
                                    {
                                        //adesso controllo se i valori e valute di deposito e prelievo sono identici
                                        //System.out.println("Trovato Deposito e prelievo");
                                        //System.out.println(movimentoSplittato[1]+" "+movSplit[5]);
                                       if (movimentoSplittato[1].trim().equalsIgnoreCase(movSplit[5].trim())&&
                                               movimentoSplittato[5].trim().equalsIgnoreCase(movSplit[1].trim())&&
                                               movimentoSplittato[2].trim().equalsIgnoreCase(movSplit[6].trim())&&
                                               movimentoSplittato[6].trim().equalsIgnoreCase(movSplit[2].trim()))
                                           {
                                             //  System.out.println("Stesso Wallet");
                                               //adesso controllo se sono relativi allo stesso wallet, in quel caso non scrivo nulla in caso contrario lo tratto come trasferimento interno
                                               if (movimentoSplittato[10].trim().equalsIgnoreCase(movSplit[10].trim()))                                                   
                                                {
                                                    //Se è uguale anche il wallet i movimenti si annullano e non scrivo nulla
                                                    //System.out.println("Stesso Wallet");
                                                    annullato=true;
                                                }
                                               else //altrimenti lo salvo come movimento interno
                                                {
                                                    trovatacorrispondenza=true;
                                                    TipoMov="TRASFERIMENTO INTERNO";
                                                    TipoMovAbbreviato="TI";
                                                  //  System.out.println("ok");
                                                }   
                                           }
                                    }
                                    }
                                //se dopo il ciclo trovo che non è ne un traferimento interno ne un movimento da non importare allora
                                //significa che è un traferimento Crypto o traferimento FIAT
                                //a questo punto verifico che tipo di movimento è e imposto le variabili di conseguenza
                                if (trovatacorrispondenza==false && annullato==false && 
                                        (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")||movimentoSplittato[2].trim().equalsIgnoreCase("EUR")||
                                        movimentoSplittato[6].trim().equalsIgnoreCase("USD")||movimentoSplittato[2].trim().equalsIgnoreCase("USD")))
                                {
                                        TipoMov="TRASFERIMENTO FIAT";
                                        TipoMovAbbreviato="TF";
                                }
                                if (annullato==false)   
                                    {
                                    RT=new String[ColonneTabella];
                                    RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_"+TipoMovAbbreviato;
                                    RT[1] = dataa;
                                    RT[2] = k + 1 + " di " + numMovimenti;
                                    RT[3] = Exchange;
                                    RT[4] = movimentoSplittato[10];
                                    RT[5] = TipoMov;
                                    //parte sotto da sistemare in base se è un prelievo o un deposito

                                    String TipoMovimento=movimentoSplittato[0].trim();

                                                                                                 
                                    
                                    if (TipoMovimento.equalsIgnoreCase("Deposito"))
                                        {
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                        RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_DC";
                                        RT[5] = "DEPOSITO CRYPTO";
                                        }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                              RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_DF";
                                              RT[5] = "DEPOSITO FIAT";  
                                            }
                                        RT[6] ="-> "+movimentoSplittato[2];
                                        RT[7] = TipoMovimento;
                                        RT[8] = "";
                                        RT[9] = "";
                                        RT[10] = "";
                                        RT[11] = movimentoSplittato[2];
                                        if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR")) RT[12] = "FIAT";else RT[12] = "Crypto";
                                        RT[13] = movimentoSplittato[1];
                                        String valoreEur;
                                        if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR")) valoreEur = movimentoSplittato[1];else valoreEur =movimentoSplittato[4];
                                        RT[14] = "EUR "+valoreEur;
                                        String prezzoTrans=new BigDecimal(valoreEur).setScale(2, RoundingMode.HALF_UP).toString();
                                        
                                        RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                        RT[16] = "";
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                                RT[17]="Da calcolare";
                                                //se sono qua significa che c'è in piedi un deposito crypto
                                            }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TI")) RT[17] = "Da calcolare";else RT[17]=RT[15];
                                        RT[18] = "";
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                                RT[19]="Da calcolare";
                                                //se sono qua significa che c'è in piedi un deposito crypto
                                            }
                                        else RT[19] = "0.00";
                                        }
                                    if (TipoMovimento.equalsIgnoreCase("Prelievo"))
                                        {
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                        RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PC";
                                        RT[5] = "PRELIEVO CRYPTO";
                                        }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                              RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PF";
                                              RT[5] = "PRELIEVO FIAT";  
                                            }
                                        RT[6] = movimentoSplittato[6]+" ->";
                                        RT[7] = TipoMovimento;
                                        RT[8] = movimentoSplittato[6];
                                        if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) RT[9] = "FIAT";else RT[9] = "Crypto";
                                        RT[10] = "-"+movimentoSplittato[5];
                                        RT[11] = "";
                                        RT[12] = "";
                                        RT[13] = "";
                                        String valoreEur;
                                        if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) valoreEur = movimentoSplittato[5];else valoreEur =movimentoSplittato[8];
                                        RT[14] = "EUR "+valoreEur;
                                        String prezzoTrans=new BigDecimal(valoreEur).setScale(2, RoundingMode.HALF_UP).toString();
                                        RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                        RT[16] = "";
                                        RT[17] = "Da Calcolare";
                                        RT[18] = "";
                                        if (TipoMovAbbreviato.equalsIgnoreCase("TI"))RT[19] = "0.00";else RT[19] = "Da calcolare";

                                        }
                                    RT[20] = "";
                                    RT[21] = "";
                                    RT[22] = "A";
                                    RiempiVuotiArray(RT);
                                    lista.add(RT);
                                  }   
                                }
                                
                               
                             
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
        
        return lista;
    }   
    
        
    public static String vespa(String strToDecrypt, String secret) 
    {
        try
        {
            SecretKeySpec secretKey=setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } 
        catch (Exception e) 
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }  
        
    
    public static SecretKeySpec setKey(String myKey) 
    {
        MessageDigest sha = null;
        SecretKeySpec secretKey=null;
        try {
            byte[] key;
            
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); 
            secretKey = new SecretKeySpec(key, "AES");
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return secretKey;
    }   
        
        
     public static Map<String,TransazioneDefi> RitornaTransazioniBSC( List<String> Portafogli,Component c,Download progressb)
         {   
            String apiKey="6qoE9xw4fDYlEx4DSjgFN0+B5Bk8LCJ9/R+vNblrgiyVyJsMyAhhjPn8BWAi4LM6";
            String vespa=vespa(apiKey,"paperino");
            progressb.setDefaultCloseOperation(0);            
            progressb.Titolo("Importazione da rete BSC");
            progressb.SetMassimo(Portafogli.size()*4);
            AzzeraContatori(); 
            Map<String, TransazioneDefi> MappaTransazioniDefi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            int ava=0;
            for (String wallets:Portafogli){
                String walletAddress=wallets.split(";")[0];
                String Blocco=wallets.split(";")[1];
                Blocco = String.valueOf(Integer.parseInt(Blocco)+1);
 
            progressb.SetLabel("Scaricamento transazioni da "+walletAddress+" in corso...");
            

        try {
            
           // MappaTransazioniDefi.clear();
            
            //PARTE 1 : Recupero la lista delle transazioni
            
            if (progressb.FineThread()) {
                return null;
            }
            URL url = new URI("https://api.bscscan.com/api?module=account&action=txlist&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey=" + vespa).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            int status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
                              if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
           // System.out.println(response);
         
            JSONArray transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
               // System.out.println(transaction.toString());
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String Data=Calcoli.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                   //     System.out.println("arghhhhh "+hash);
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                trans.Rete="BSC";
                trans.Blocco=transaction.getString("blockNumber");
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;                
                trans.MonetaCommissioni="BNB";
                trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                trans.Wallet=walletAddress;
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                trans.TipoTransazione=transaction.getString("functionName");
                if (!value.equalsIgnoreCase("0")){
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete("BNB", "BNB", "BNB", AddressNoWallet, qta,"Crypto");
               
                }
            }
            ava++;
             progressb.SetAvanzamento(ava);
            TimeUnit.SECONDS.sleep(3);
  
            
          
                        //PARTE 2: Recupero la lista delle transazioni dei token bsc20   
                           if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=tokentx&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey="+vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
               if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                //System.out.println("sono qui");
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
            //    System.out.println(transaction.toString());
                String tokenSymbol=transaction.getString("tokenSymbol");
                String tokenName=transaction.getString("tokenName");
                String Data=Calcoli.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String tokenAddress=transaction.getString("contractAddress");
                String tokenDecimal=transaction.getString("tokenDecimal");
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                    trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";
               // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta,"Crypto");                   
         }             
            ava++;
             progressb.SetAvanzamento(ava);  
          TimeUnit.SECONDS.sleep(3); 
            
            
          
          
           //PARTE 3: Recupero la lista delle transazioni dei token erc721 (NFT)  
                           if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=tokennfttx&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey="+vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
               if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                //System.out.println("sono qui");
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
            //    System.out.println(transaction.toString());
                String tokenSymbol=transaction.getString("tokenID");
                String tokenName=transaction.getString("tokenName");
                String Data=Calcoli.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String tokenAddress=transaction.getString("contractAddress");
               // String tokenDecimal=transaction.getString("tokenDecimal");
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
              //  String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                 String value = "1";
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                    trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";
               // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta,"NFT");                   
         }             
            ava++;
             progressb.SetAvanzamento(ava);  
          TimeUnit.SECONDS.sleep(3); 
          
          
            
            
            //PARTE 4: Recupero delle transazioni interne
                            if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=txlistinternal&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey=" + vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
                                             if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
            if (progressb.FineThread()) {
                return null;
            }
                
                String qta;
                String AddressNoWallet;
                JSONObject transaction = transactions.getJSONObject(i);
                String hash = transaction.getString("hash");
                String Data=Calcoli.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else{
                        AddressNoWallet=from;
                        qta=value;                      
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";  
                if (trans.QtaCommissioni!=null && new BigDecimal(trans.QtaCommissioni).abs().compareTo(new BigDecimal(qta).abs())==1 &&
                        !(trans.TipoTransazione.toLowerCase().contains("swap")&&(trans.RitornaNumeroTokenUscita()==0||trans.RitornaNumeroTokenentrata()==0)))
                    {
                        // se il valore della commissione è maggiore del bnb di ritorno allora lo sottraggo dalle commissioni
                        //anzichè metterlo come importo dei trasferimenti
                        // questo non deve essere fatto però se è uno swap di cui questi bnb sono gli unici in ritorno
                     //questa cosa la devo gestire
                        trans.QtaCommissioni=new BigDecimal(trans.QtaCommissioni).subtract(new BigDecimal(qta)).toPlainString();
                    }
                else {
                    progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                    trans.InserisciMonete("BNB", "BNB", "BNB", AddressNoWallet, qta,"Crypto");
                 //   System.out.println(trans.HashTransazione+ " - "+ qta);
                }

                
              //  System.out.println(value+" - "+hash);

                
            }           
            ava++;
             progressb.SetAvanzamento(ava);
            TimeUnit.SECONDS.sleep(3);
            
            
            
            
                

         //   TimeUnit.SECONDS.sleep(1);
                    
        } catch (MalformedURLException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null,"AAA"+ ex);
            progressb.dispose();
            Calcoli.ScriviFileConversioneXXXEUR();
            JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null,"AAA"+  ex);
            progressb.dispose();
            Calcoli.ScriviFileConversioneXXXEUR();
                        JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        } catch (InterruptedException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null,"AAA"+  ex);
            progressb.dispose();
            Calcoli.ScriviFileConversioneXXXEUR();
                        JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        }
        
        }
        Calcoli.ScriviFileConversioneXXXEUR();
        return MappaTransazioniDefi;
        }    
    
}
