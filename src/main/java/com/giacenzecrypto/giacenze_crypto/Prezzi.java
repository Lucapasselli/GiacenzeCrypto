/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * @author luca.passelli
 */
public class Prezzi {
    //static Map<String, String> MappaWallets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneUSDEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//    static Map<String, String> MappaCoppieBinance = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneSwapTransIDCoins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    //di seguito le coppie prioritarie ovvero quelle che hanno precedneza all'atto della ricerca dei prezzi rispetto alle altre
    static String[] SimboliPrioritari=new String []{"EURI","USDT","USDC","BUSD","USDE","DAI","TUSD","BTC",
        "ETH","BNB","SOL","LTC","ADA","XRP","XLM","PAX","TRX","ATOM","MATIC"};

    
    //Variabile che mi serve per capire se ho già interrorgato le api ad un certo timestamp
    //static Map<String, List<long>> richiestePerMoneta = new HashMap<>();
    static RangeRequestManager managerRichieste = new RangeRequestManager();


    
    
  //DA FARE : Recupero prezzi orari in base all'ora più vicina  


    /**
     * Questa funzione si occupa di fare 2 cose
     * 1 - Verifica se il movimento ha prezzo e se lo ha mette "SI" nel posto _32
     * 2 - Se il movimento ha prezzo o è scam ritorna true 
     * (Se il token è scam devo tornare che ha prezzo altrimenti il programma segnala errore su quel token e i token scam non mi interessa gestirli)
     *
     * @param Movimento
     * @return true se movimento prezzato o scam altrimenti false.
     */
    public static boolean isMovimentoPrezzato(String[] Movimento) {
        boolean prezzato;
        //se il movimento è già stato classificato ed ed ha prezzo ritorno subito true
        //in caso di no non ritorno false perchè prima devo vedere se coinvolge movimenti scam
        if (Movimento[32] != null && Movimento[32].equals("SI"))return true;
       /* else
            {System.out.println("ooorco "+Movimento[32]+" - "+Movimento[15]);}*/
        
        else if (Movimento[32] == null || Movimento[32].isBlank()) {//Questa cosa la faccio se il campo non è valorizzato o è valorizzato a NO
           // {System.out.println("ooorco2 "+Movimento[15]);}
            if (!Movimento[15].equals("0.00")) {
                Movimento[32] = "SI";
            } else if (DammiPrezzoDaTransazione(Movimento,2)==null) {               
                Movimento[32] = "NO";
            } else {
                Movimento[32] = "SI";
            }
        }
        //Questo secondo else serve per sistemare quei movimenti che hanno "Si" al posto di "SI"
        else if (Movimento[32].equalsIgnoreCase("SI"))
        {
            Movimento[32] = "SI";
            return true;
        }
        //Questa ultima parte server per sistemare eventuali errori nell'assegnazione del prezzo
      /*  else if (Movimento[32].equals("NO")&&!Movimento[15].equals("0.00"))
            {Movimento[32] = "SI";}
        else if (Movimento[32].equals("NO"))
            {System.out.println("ooorco "+Movimento[15]);}*/
        
        //Controllo l'esito dei movimenti sopra e setto prezzato di conseguenza
        prezzato=Movimento[32].equals("SI");
        //if (Movimento[32].equals("NO"))prezzato=false;
        
        //Se è un movimento di un token scam lo indico come prezzato anche se non ho il prezzo perchè non mi interessa averlo evidenziato come errore
        if (!Movimento[11].isBlank()&&Funzioni.isSCAM(Movimento[11]))prezzato=true;
        if (!Movimento[8].isBlank()&&Funzioni.isSCAM(Movimento[8]))prezzato=true;
        return prezzato;
    }
    
    public static String DammiPrezzoDaTransazione(String[] v,int decimali){
        //Questa funzione ritorna null in caso di mancanza di prezzo
        //Da sistemare recuperando la fonte corretta per i prezzi

            long data=FunzioniDate.ConvertiDatainLongMinuto(v[1]);
            String Rete = Funzioni.TrovaReteDaIMovimento(v);
            
            
            Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
            //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
            Monete[0] = new Moneta();
            Monete[1] = new Moneta();
            
            //Se la rete non è tra quelle supportate non la metto nella ricerca del prezzo e azzero anche gli address
            //Sono dati che non mi interessa avere infatti in questo caso perchè non posso recuperare i prezzi da coingecko
            if(Rete==null||Principale.MappaRetiSupportate.get(Rete)==null){
                Rete="";
                Monete[0].MonetaAddress="";
                Monete[1].MonetaAddress="";
            }else{
                Monete[0].MonetaAddress = v[26];
                Monete[1].MonetaAddress = v[28];
            }
            //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
            Monete[0].Moneta = v[8];
            Monete[0].Tipo = v[9];
            Monete[0].Qta = v[10];
            Monete[0].Rete = Rete;
            Monete[1].Moneta = v[11];
            Monete[1].Tipo = v[12];
            Monete[1].Qta = v[13];
            Monete[1].Rete = Rete;
            String Prezzo=DammiPrezzoTransazione(Monete[0],Monete[1],data,null, false, decimali, Rete,"");
            //System.out.println(Prezzo);
            return Prezzo;
            
    }
    
    
    
      public static void GeneraMappaConversioneSwapTransIDCoins(){
         try {
             File file=new File ("conversioneTransIDCoins.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("conversioneTransIDCoins.db");
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                     if (rigaSplittata.length==5)
                     {
                         MappaConversioneSwapTransIDCoins.put(rigaSplittata[0], rigaSplittata[1]+","+rigaSplittata[2]+","+rigaSplittata[3]+","+rigaSplittata[4]);
                     }
                 }
            //     bure.close();
            //     fire.close();        
             } catch (FileNotFoundException ex) {
                 LoggerGC.ScriviErrore(ex);
             } catch (IOException ex) {
                 LoggerGC.ScriviErrore(ex);
             }
             
         } catch (IOException ex) {        
            LoggerGC.ScriviErrore(ex);
        }

    }   
    
    
    
    
    
    public static void GeneraMappaCambioUSDEUR(){
         try {
             File file=new File (Statiche.getFileUSDEUR());
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader(Statiche.getFileUSDEUR());
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                     if (rigaSplittata.length==2)
                     {
                         MappaConversioneUSDEUR.put(rigaSplittata[0], rigaSplittata[1]);
                     }
                 }
             //    bure.close();
             //    fire.close();        
             } catch (FileNotFoundException ex) {
                 LoggerGC.ScriviErrore(ex);
             } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
             }
             
         } catch (IOException ex) {        
            LoggerGC.ScriviErrore(ex);
        }

    }  
    

    
    
    
/*     public static String DammiIDCoingeckodaAddress(String Address,String rete)   
        {
        if (MappaConversioneAddressCoin.isEmpty())
            {
                GeneraMappaConversioneAddressCoin();
                //qui andro' anche a popolare la mappa con la conversione della rete per coingecko
                MappaConversioneSimboloReteCoingecko.put("BSC", "binance-smart-chain");
            }
        if (MappaConversioneAddressCoin.get(Address+"_"+rete)!=null)
        {
            return MappaConversioneAddressCoin.get(Address+"_"+rete).split(",")[1];
        }
        else
            {
        try {
            TimeUnit.SECONDS.sleep(5);//il timeout serve per evitare di fare troppe richieste all'API
            String url = "https://api.coingecko.com/api/v3/coins/"+MappaConversioneSimboloReteCoingecko.get(rete)+ "/contract/" + Address;
            System.out.println(url);
            URL obj = new URI(url).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Gson gson = new Gson();
            JsonObject coinInfo = gson.fromJson(response.toString(), JsonObject.class);
            
           String Simbolo=coinInfo.get("symbol").toString().replace("\"", "");
           String ID=coinInfo.get("id").toString().replace("\"", "");
           MappaConversioneAddressCoin.put(Address.toUpperCase()+"_"+rete.toUpperCase(),Simbolo+","+ID);
           ScriviFileConversioneAddressCoin(Address.toUpperCase()+"_"+rete.toUpperCase()+","+Simbolo+","+ID);
        //   System.out.println(ID);
            return ID;
        } catch (Exception ex) {
          //  Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
          //  System.out.println(ex.getMessage());
/////////////////Se su coingecko non trovo nulla allora lancio la funzione che cerca il codice del token dal'html di bscscan
          //se anche li non trovo nulla allora ci rinuncio
 /*         if (rete.equalsIgnoreCase("BSC")){
              String Simbolo=RitornaNomeTokendadaBSCSCAN(Address);
              if (Simbolo!=null)
                  {
              MappaConversioneAddressCoin.put(Address.toUpperCase()+"_"+rete.toUpperCase(),Simbolo.toUpperCase()+",");
              }
              return Simbolo;
          }
          }
          MappaConversioneAddressCoin.put(Address.toUpperCase()+"_"+rete.toUpperCase(),"nulladifatto,nulladifatto");
          ScriviFileConversioneAddressCoin(Address.toUpperCase()+"_"+rete.toUpperCase()+",nulladifatto,nulladifatto");
     //     System.out.println("nulladifatto "+ex.getMessage());
          return null;
        }
       // return null;
//       return Simbolo;
        }}*/
     
 
     
   /*public static boolean RitornaTransazioniBSC_2di3(String walletAddress,String apiKey)
         {    
               try {

            URL url=new URI("https://api.bscscan.com/api?module=account&action=txlistinternal&address=" + walletAddress + "&startblock=0&sort=asc" +"&apikey=" + apiKey).toURL();
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
            
            JSONArray transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                String qta;
                String AddressNoWallet;
                JSONObject transaction = transactions.getJSONObject(i);
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(hash);
                    }

                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else{
                        AddressNoWallet=from;
                        qta=value;                      
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);//Da modificare con data e ora reale
                trans.HashTransazione=hash;
                trans.Rete="BSC";
                trans.MonetaCommissioni="BNB";  
                if (trans.QtaCommissioni!=null && new BigDecimal(trans.QtaCommissioni).abs().compareTo(new BigDecimal(qta).abs())==1)
                    {
                        // se il valore della commissione è maggiore del bnb di ritorno allora lo sottraggo dalle commissioni
                        //anzichè metterlo come importo dei trasferimenti
                     //   System.out.println("AAAA - "+trans.HashTransazione+ " - "+ qta);
                        trans.QtaCommissioni=new BigDecimal(trans.QtaCommissioni).subtract(new BigDecimal(qta)).toPlainString();
                    }
                else {
                    trans.InserisciMonete("BNB", "BNB", "BNB", AddressNoWallet, qta);
                 //   System.out.println(trans.HashTransazione+ " - "+ qta);
                }

                
              //  System.out.println(value+" - "+hash);

                
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
               
        return RitornaTransazioniBSC_3di3(walletAddress,apiKey);
         }     */
     
     
 /*   public static boolean RitornaTransazioniBSC_3di3(String walletAddress,String apiKey)
         {    
               try {
      
            URL url=new URI("https://api.bscscan.com/api?module=account&action=tokentx&address=" + walletAddress + "&startblock=0&sort=asc" +"&apikey="+apiKey).toURL();
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
            
            JSONArray transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
            //    System.out.println(transaction.toString());
                String tokenSymbol=transaction.getString("tokenSymbol");
                String tokenName=transaction.getString("tokenName");
                String tokenAddress=transaction.getString("contractAddress");
                String tokenDecimal=transaction.getString("tokenDecimal");
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(hash, trans);
                }else 
                    {
                   //     System.out.println("arghhhhh "+hash);
                    trans=MappaTransazioniDefi.get(hash);
                    }

                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);//Da modificare con data e ora reale
                trans.HashTransazione=hash;
                trans.Rete="BSC";
                trans.MonetaCommissioni="BNB";
               // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta);
                
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
               return true;
         }    */


     
     
  /*   public static void RecuperaDettagliTransazioneBSC(String transactionHash){
           String coinE=null;
           String qtaE=null;
           String coinU=null;
           String qtaU=null;
            if (MappaConversioneSwapTransIDCoins.isEmpty())
            {
                GeneraMappaConversioneSwapTransIDCoins();
            }
              String Moneta;
            if(MappaConversioneSwapTransIDCoins.get(transactionHash+"_BSC")!=null)
            {
                String splittata[]=MappaConversioneSwapTransIDCoins.get(transactionHash+"_BSC").split(",");
                coinU=splittata[0];
                qtaU=splittata[1];
                coinE=splittata[2];
                qtaE=splittata[3];  
            }
            else{
        try {
            TimeUnit.SECONDS.sleep(5);
            
            String url = "https://bscscan.com/tx/" + transactionHash;
            URL obj = new URI(url).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
           // con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            Moneta=response.toString();
            Moneta=Moneta.substring(Moneta.indexOf("Tokens Transferred:")).trim();
            Moneta=Moneta.substring(0,Moneta.indexOf("</a></div></li></ul></div></div>")).replaceAll("\\<.*?\\>", " ");
           // System.out.println(Moneta);

            String splittata[]=Moneta.split(" For ");
            if (splittata.length>=3)
                {
                    int i=0;
                for (String split:splittata)
                {
                    if (i==1){
                        String temp=split.trim().split("From")[0].trim();
                        String divisioneparentesi[]=temp.split("\\(");     
                        if (divisioneparentesi.length==3){
                            qtaU=temp.substring(0, temp.indexOf("(")).replaceAll(",","").trim();
                            coinU=temp.split("\\(")[2].replaceAll("\\)", "");
                        }else if (divisioneparentesi.length==2){
                            qtaU=temp.substring(0, temp.indexOf(" ")).replaceAll(",","").trim();
                            coinU=temp.split("\\(")[1].replaceAll("\\)", "");                            
                        }
                        if (coinU.equalsIgnoreCase("WBNB"))coinU="BNB";
                      // System.out.println("qta="+qta+" - coin="+coin); 
                    }
                    if (i==splittata.length-1)
                        {
                        String temp=split.trim().split("From")[0].trim();
                        String divisioneparentesi[]=temp.split("\\(");       
                        if (divisioneparentesi.length==3){
                            qtaE=temp.substring(0, temp.indexOf("(")).replaceAll(",", "").trim();
                            coinE=temp.split("\\(")[2].replaceAll("\\)", "");
                        }else if (divisioneparentesi.length==2){
                            qtaE=temp.substring(0, temp.indexOf(" ")).replaceAll(",","").trim();
                            coinE=temp.split("\\(")[1].replaceAll("\\)", "");                            
                        }
                        if (coinE.equalsIgnoreCase("WBNB"))coinE="BNB";
                    //    System.out.println("qta="+qtaE+" - coin="+coinE); 
                    //    System.out.println(""); 
                        }
                   // System.out.println(split.trim());
                    i++;
                }
            }
           
           String riga=transactionHash+"_BSC,"+coinU+","+qtaU+","+coinE+","+qtaE;
           ScriviFileConversioneSwapTransIDCoins(riga);
           MappaConversioneSwapTransIDCoins.put(transactionHash+"_BSC", coinU+","+qtaU+","+coinE+","+qtaE);
           
           
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
        }
            }
       // return null;
        System.out.println(transactionHash);
        System.out.println("qta="+qtaU+" - coin="+coinU);
        System.out.println("qta="+qtaE+" - coin="+coinE); 
        System.out.println("");
      
      }*/
     
 /*     public static String RitornaNomeTokendadaBSCSCAN(String Address)
          {
              String Moneta;
        try {
            String url = "https://bscscan.com/token/" + Address;
            URL obj = new URI(url).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
           // con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            Moneta=response.toString();
            Moneta=Moneta.substring(Moneta.indexOf("<title>")+8, Moneta.indexOf("</title>")).trim();
            if (!Moneta.contains(")")) { // se non contiene la parentesi significa che non ho trovato la moneta
                Moneta=null;
            }
            else
                {
            Moneta=Moneta.substring(Moneta.indexOf("(")+1, Moneta.indexOf(")")).trim();
            }
         //  System.out.println(Moneta);
           return Moneta;
           
           
           
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
      }*/
     
        
    public static String ConvertiUSDEUR(String Valore, String Data) {
        RecuperaTassiCambioEURUSD();
        //System.out.println(Valore+" - "+Data);
        
        
        String risultato = null;
        //come prima cosa devo controllare che la data analizzata sia nel range delle date di cui ho il cambio usd/eur
        Object dateDisponibili[] = MappaConversioneUSDEUR.keySet().toArray();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String PrimaData;//è la prima data disponibile nel file delle conversioni
        String UltimaData;//e' l'ultima data disponibile nel file delle conversioni
        String DatadiOggi = f.format(System.currentTimeMillis());                   
                    
        // se il file ha dei dati recupero prima e ultima data      
            PrimaData = (String) dateDisponibili[0];
            UltimaData = (String) dateDisponibili[dateDisponibili.length - 1];
            long DataOggiLong = FunzioniDate.ConvertiDatainLong(DatadiOggi);
            long DataUltimaLong = FunzioniDate.ConvertiDatainLong(UltimaData);
            long diffDate = DataOggiLong - DataUltimaLong;//86400000 di differenza significa 1 giorno
            //a questo punto siccome non posso ottenere i dati della giornata odierna
            //se sto cercando di ottenere quelli e l'ultimo dato che ho è inferiore a 10gg prendo quello come dato valido per la giornata
            //metto 10gg invece che 1 perchè potrebbero esserci delle feste e banchitalia non restituisce valori in quel caso
            if (FunzioniDate.ConvertiDatainLong(Data) >= FunzioniDate.ConvertiDatainLong("2015-01-01") && 
                    FunzioniDate.ConvertiDatainLong(Data) <= FunzioniDate.ConvertiDatainLong(DatadiOggi)) {
            risultato = MappaConversioneUSDEUR.get(Data);
            if (risultato == null) {
                //Se non trovo il tasso di cambio e comunque so che è compreso nel range che drovrebbe avere provo a trovarlo a ritroso
                //questo serve nel caso che to cercando di recuperare il tasso di una festività o della data attuale
                //in quel caso prendo il prezzo della giornata precedente o quello ancora prima
                    for (int i = 0; i < 10; i++) {
                        risultato = MappaConversioneUSDEUR.get(Data);
                        if (risultato == null) {
                            Data = FunzioniDate.GiornoMenoUno(Data);//questo appunto serve per andare a prendere i sabati e le domeniche dove non ho dati
                        } else {
                            break;
                        }
                    }
                }
            }
        

        if (risultato != null) {

            risultato = (new BigDecimal(Valore).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

          //  System.out.println(risultato);

            return risultato;
        }
        return risultato;
    }
    
    
    /*    public static String ConvertiUSDEUR(String Valore, String Data) {
        if (!fileConversioneUSDEURcaricato)
            {
                GeneraMappaCambioUSDEUR();
            }
        
        
        String risultato;// = null;

        risultato = MappaConversioneUSDEUR.get(Data);
      // SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH");
       // String DatadiOggi = f.format(System.currentTimeMillis());
        if (risultato == null) {
            //fare parte del range date
                String DataFinale = ConvertiDatadaLong(ConvertiDatainLong(Data) + Long.parseLong("15552000000"));
                String DataIniziale = ConvertiDatadaLong(ConvertiDatainLong(Data) - Long.parseLong("15552000000"));
                RecuperaTassidiCambio(DataIniziale,DataFinale);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
                risultato = MappaConversioneUSDEUR.get(Data);
            
                    } //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

        if (risultato != null) {
            risultato = (new BigDecimal(Valore).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
        }
        return risultato;
    }*/
    
    
 /*   public static String ConvertiUSDTEUR(String Qta, long Datalong) {
        String risultato;// = null;
        //come prima cosa devo decidere il formato data
        long adesso = System.currentTimeMillis();
        if (Datalong > adesso) {
            return null;//se la data è maggiore di quella attuale non recupero nessun prezzo
        }
        if (Datalong < 1483225200) {
            return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        }
        String DataOra = OperazioniSuDate.ConvertiDatadaLongallOra(Datalong);
        String DataGiorno = OperazioniSuDate.ConvertiDatadaLong(Datalong);

        //cerco il valore di USDT dal vecchio database
        risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        if (risultato!=null){
            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).stripTrailingZeros().toString();
            return risultato;
        }
        else {
            //Se non trovo il risultato nel vecchio database cerco nel nuovo
            InfoPrezzo IP=CambioXXXEUR("USDT",Qta,Datalong,"","","");
            if (IP!=null) return IP.RitornaPrezzoQta().toPlainString();
            //Se arrivo qua vuol dire che non ho ancora trovato il prezzo in quel caso prendo il prezzo dei dollari reali
            risultato = ConvertiUSDEUR(Qta, DataGiorno);
            return risultato;
        }

    }*/
    
    
    
    
    
    public static InfoPrezzo ConvertiAddressEUR(String Qta, long Datalong, String Address, String Rete, String Simbolo,String Fonte) {

        
        
        BigDecimal qta=new BigDecimal(Qta);
        //Se l'addess non contiene 0x significa che non posso recuperarlo da coingecko quindi lo recupero con il Simbolo
        //Se l'address non è valido allora recupero il prezzo dagli exchange
        if (!Funzioni_WalletDeFi.isValidAddress(Address, Rete) || Rete == null || Rete.isBlank()) {
            InfoPrezzo IP=CambioXXXEUR(Simbolo, Qta, Datalong,"","","",true);
            if (IP==null)return null;
            return IP;
        }
        //Se non l'ho ancora fatto recupero la lista dei token gestiti
        
        InfoPrezzo IPrezzo;
        
        
        //Come prima cosacontrollo se ho dei personalizzati nuovi, se non li ho allora cerco tra i prezzi vecchi e poi torno a cercare tra i nuovi
        //Come prima cosa cerco nei nuovi personalizzati        
        IPrezzo=DammiPrezzoDaDatabasePersonale("",Datalong,Fonte,Rete,Address,60,qta);
        if (IPrezzo!=null){
            IPrezzo.Moneta=Simbolo;
            return IPrezzo;
        }
        
        
        //Vedo se riesco a recuperare il prezzo dal vecchio database
        String DataOra = FunzioniDate.ConvertiDatadaLongallOra(Datalong);
        String DataGiorno = FunzioniDate.ConvertiDatadaLong(Datalong);
        String risultato = DatabaseH2.PrezzoAddressChain_Leggi(DataOra + "_" + Address + "_" + Rete);//Prezzo unitario
        if (risultato!=null && !risultato.equalsIgnoreCase("ND")){
            IPrezzo=new InfoPrezzo();
            IPrezzo.Moneta=Simbolo;
            IPrezzo.Qta=new BigDecimal(Qta);
            if(DatabaseH2.PrezzoAddressChainPers_Leggi(DataOra + "_" + Address + "_" + Rete)==null)IPrezzo.Fonte="coingecko (Old)";
            else IPrezzo.Fonte="Personalizzato (Old)";
            IPrezzo.timestamp=Datalong;
            IPrezzo.prezzoUnitario=new BigDecimal(risultato);
            return IPrezzo;
        }

       
        //Se non ci riesco cerco nel nuovo database
        if (IPrezzo==null){
            //Il nome della coin in questo caso non serve anzi non va messo proprio           
            IPrezzo=DammiPrezzoDaDatabase("",Datalong,"",Rete,Address,60,qta);
            if (IPrezzo!=null)
            {
                //Integro i dati mancanti (quelli che non posso avere dal database)
                IPrezzo.Moneta=Simbolo;
                //IPrezzo.exchange="coingecko";
                return IPrezzo;
            }
        }
        
        //Se ancora non trovo i prezzi vado a richiedere a coingecko i dati
        if (IPrezzo == null) {
            //se il token non è gestito da coingecko e non è già nel database ritorno null
            RecuperaCoinsCoingecko();
            String AddressNoPrezzo = DatabaseH2.GestitiCoingecko_Leggi(Address + "_" + Rete);
            if (AddressNoPrezzo == null) {
                return null;
            } else {
                //Se la moneta è codificata da coingecko (Quindi so che non è scam) e corrisponde ad una delle monete principali
                //Prendo il suo prezzo dagli exchange per risparmiare tempo e richieste.
                for (String SimboloP : SimboliPrioritari) {
                    if ((Simbolo).toUpperCase().equals(SimboloP)) {
                        IPrezzo=CambioXXXEUR(Simbolo, Qta, Datalong,"","","",true);
                        if (IPrezzo!=null)return IPrezzo;
                    }
                }
                long adesso = System.currentTimeMillis();
                if ((adesso - Datalong) > Long.parseLong("31536000000")) {
                    //Se arrivo qua il prezzo non è in memoria e 
                    //se cerco di recuperare prezzi più vecchi di 365gg li vado a cercare dagli exchange visto che coingecko non li fornisce
                    IPrezzo=CambioXXXEUR(Simbolo, Qta, Datalong,"","","",true);
                    if (IPrezzo!=null)return IPrezzo;
                    //Se il prezzo non lo trovo neanche dall'exchange ritorno null
                    return null;
                }
                //Se arrivo qua vuol dire che non è tra le coppie prioritarie e quindi vado a prendere il prezzo da coingecko
                RecuperaTassidiCambiodaAddress_Coingecko(DataGiorno, Address, Rete, Simbolo);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
                IPrezzo=DammiPrezzoDaDatabase("",Datalong,"",Rete,Address,60,qta);
                if (IPrezzo!=null)
                {
                    //Integro i dati mancanti (quelli che non posso avere dal database)
                    IPrezzo.Moneta=Simbolo;
                    //IPrezzo.exchange="coingecko";
                    return IPrezzo;
                }
                
                //Se ancora non trovo il prezzo allora provo a cercare per simbolo
                //Tanto parto dal presupposto che è un token che coingecko gestisce quindi non scam
                //QUESTO MOMENTANEAMENTE LO DISABILITO, DA CAPIRE SE RIABILITARLO O MENO
               // IPrezzo=CambioXXXEUR(Simbolo, Qta, Datalong,"","","");
                if (IPrezzo!=null)return IPrezzo;
            }
        }
        
        //Se arrivo qua vuol dire che le ho provate tutte ma non riesco a trovare il prezzo del token
        return null;
    }
    
    
    public static void CompilaMoneteStessoPrezzo(){
     Principale.Mappa_MoneteStessoPrezzo.put("WCRO", "CRO");
     Principale.Mappa_MoneteStessoPrezzo.put("WETH", "ETH");
    }
    
    
    
  /*  public static String ConvertiXXXEUR(String Crypto, String Qta, long Datalong) {

        String risultato;// = null;
        boolean mettereND = false;

        Crypto = CDC_Grafica.Mappa_MoneteStessoPrezzo.getOrDefault(Crypto, Crypto);
        long Anno2017 = Long.parseLong("1483225200000");
        long DataInizioBinance = Long.parseLong("1502942400000");

        long adesso = System.currentTimeMillis();
        if (Datalong > adesso) {
            return null;//se la data è maggiore di quella attuale allora ritrono subito null perchè non ho i prezzi
        }
        if (Datalong < Anno2017) {
            return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        }
        String DataOra = OperazioniSuDate.ConvertiDatadaLongallOra(Datalong);
        String DataGiorno = OperazioniSuDate.ConvertiDatadaLong(Datalong);       
        //System.out.println("RecuperoCoinCap");

        risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
        //se il risultato è null significa che non ho il prezzo specifico dell'ora
        if (risultato == null) {
            //a questo punto provo a recuperarlo da binance
            RecuperaCoppieBinance();//il test sulla data lo fà già il programma
            if (DatabaseH2.CoppieBinance_Leggi(Crypto + "USDT") != null && Datalong >= DataInizioBinance) {
                //scarico i prezzi da binance
                RecuperaTassidiCambioXXXUSDT_Binance(Crypto, DataGiorno, DataGiorno);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
            }
        }
        
        if (risultato == null) {
            //se non trovo un prezzo recupero le coppie gestite da binance e coincap
            RecuperaCoppieCoinbase();
            //System.out.println("RecuperoCoinCap");
            if (DatabaseH2.GestitiCoinbase_Leggi(Crypto) != null) {
                //Se gestito da CoinCap scarico i prezzi da CoinCap
                RecuperaTassidiCambiodaSimbolo_Coinbase(Crypto, DataGiorno);
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
            }
            //solo se arrivo fino a qua metto gli ND sulle ore che non sono riuscito a recuperare e per 10gg
            //che sono i giorni minimi per cui recupero i prezzi (Quelli di coinbase)
            //Questo perchè questo è l'ultimo provider da cui posso recuperare i dati quindi tutti i buchi di prezzo sono 
            //sicuro che non potrò farci nulla
            //mettereND = true;
            //in sostanza va messo sempre sull'ultimo provider

        }
        //se ancora non ho il prezzo recupero il prezzo dall'altro provider ovvero CryptoCompare
        
        if (risultato == null) {
            //se non trovo un prezzo recupero le coppie gestite da binance e coincap
            //Guardo le coins supportate da Coincap perchè Cryptocompare supporta troppe crypto e la maggior parte non hai dati storici
            //Decido quindi di usare la base di Coincap che terrò aggiornata manualmente
            RecuperaCoinsCoinCap();           
            if (DatabaseH2.GestitiCoinCap_Leggi(Crypto) != null) {
                //Se gestito da CoinCap scarico i prezzi da CoinCap
                RecuperaTassidiCambiodaSimbolo_CryptoCompare(Crypto, DataGiorno);
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
            }
            //solo se arrivo fino a qua metto gli ND sulle ore che non sono riuscito a recuperare e per 30gg
            //che sono i giorni per cui di solito recupero i prezzi
            //Questo perchè questo è l'ultimo providfer da cui posso recuperare i dati quindi tutti i buchi di prezzo sono 
            //sicuro che non potrò farci nulla
            //mettereND = true;
            //in sostanza va messo sempre sull'ultimo provider

        }
        
        //se ancora non ho il prezzo recupero il prezzo dall'altro provider ovvero CoinCap
        //Metto in pausa coincap momentaneamente perchè richiede APIKEY da Aprile
        
        if (risultato == null) {
            //se non trovo un prezzo recupero le coppie gestite da binance e coincap
            RecuperaCoinsCoinCap();
            //System.out.println("RecuperoCoinCap");
            if (DatabaseH2.GestitiCoinCap_Leggi(Crypto) != null) {
                //Se gestito da CoinCap scarico i prezzi da CoinCap
                RecuperaTassidiCambiodaSimbolo_CoinCap(Crypto, DataGiorno);
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
            }
            //solo se arrivo fino a qua metto gli ND sulle ore che non sono riuscito a recuperare e per 30gg
            //che sono i giorni per cui di solito recupero i prezzi
            //Questo perchè questo è l'ultimo providfer da cui posso recuperare i dati quindi tutti i buchi di prezzo sono 
            //sicuro che non potrò farci nulla
            mettereND = true;
            //in sostanza va messo sempre sull'ultimo provider

        }
        
        
        
        //Se il risultato è ancora null provo a recuperare il prezzo della giornata invece che quello orario
        if (risultato == null || risultato.equalsIgnoreCase("ND")) {
            //se non trovo un prezzo recupero le coppie gestite da binance e coincap
            risultato = DatabaseH2.XXXEUR_Leggi(DataGiorno + " " + Crypto);
        }

        if (mettereND) {
            //Adesso conto 30 gg dalla data in cui è partita la richiesta e per tutte le ore in cui non ho trovato prezzi metto degli ND
            //Questo serve per eveitare di fare altre richieste di prezzi che non posso recuperare
            //Gli ND poi vengono tolti al riavvio del programma
            long timestampIniziale = OperazioniSuDate.ConvertiDatainLong(DataGiorno);
            long timestampFinale = timestampIniziale + Long.parseLong("864000000");
            if (adesso < timestampFinale) {
                timestampFinale = adesso;
            }
            while (timestampIniziale < timestampFinale) {
                Date date = new java.util.Date(timestampIniziale);
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                String DataconOra = sdf.format(date);
                //Metto ND su tutte le ore senza valore
                if (DatabaseH2.XXXEUR_Leggi(DataconOra + " " + Crypto) == null) {
                    DatabaseH2.XXXEUR_Scrivi(DataconOra + " " + Crypto, "ND",false);
                }

                //Se le crypto senza prezzo sono le stable a questo punto prendo il prezzo del dollaro del giorno e le valorizzo così
                if (Crypto.equalsIgnoreCase("USDC")
                        || Crypto.equalsIgnoreCase("BUSD")
                        || Crypto.equalsIgnoreCase("DAI")) {
                    String Prezzo = ConvertiUSDTEUR("1", OperazioniSuDate.ConvertiDatainLongMinuto(DataconOra + ":01"));
                    if (Prezzo != null) {
                        DatabaseH2.XXXEUR_Scrivi(DataconOra + " " + Crypto, Prezzo,false);
                    }
                }
                timestampIniziale = timestampIniziale + 3600000;//aggiungo 1 ora
            }
        }

        if (risultato != null) {
            //infatti se ritorna zero vuol dire che per quella data binance o cryptohistory non forniscono nessun prezzo
            if (risultato.equalsIgnoreCase("ND") || risultato.equalsIgnoreCase("null")) {
                risultato = null;
            } else {
                //questa è la mappa che al termine della conversione devo scrivere nel file;
                //MappaConversioneXXXEUR.put(DataOra + " " + Crypto, risultato);
                risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(30, RoundingMode.HALF_UP).stripTrailingZeros().toString();
            }
        }
        //  System.out.println(risultato);
        return risultato;
    }*/
    
      public static InfoPrezzo CambioXXXEUR(String Crypto, String Qta, long Datalong,String Address,String Rete,String Fonte,boolean includiVecchi) {

        //in questo metodo manca solo la parte che salva le richieste già effettuate sul token per evitare di farne tante analoghe
        InfoPrezzo risultato;   
        BigDecimal qta=new BigDecimal(Qta);
       
        
        //1 - NORMALIZZO I DATI IN INGRESSO
        
        //Se i dati non sono completi ritorno null
        if(Crypto==null||Qta==null||Crypto.isBlank()||Qta.isBlank())return null;
        
        //Verifico se l'address è valido, se non lo è azzero Rete e address.
        //La ricerca in questo caso deve avvenire solo per Simbolo
        if (!Funzioni_WalletDeFi.isValidAddress(Address, Rete) || Rete.isBlank()) {
            Rete="";
            Address="";
        }
        else{
            //Se gli address sono validi mando la richiesta su coingecko
            //Sarà poi la funzione stessa a richiamare questa non dovesse trovare il prezzo
            return ConvertiAddressEUR(Qta,Datalong,Address,Rete,Crypto,Fonte);           
        }


        //2 - INTERROGO I DATI MEMORIZZATI NEL DATABASE INTERNO
        Crypto = Principale.Mappa_MoneteStessoPrezzo.getOrDefault(Crypto, Crypto);
        long Anno2017 = Long.parseLong("1483225200000");

        long adesso = System.currentTimeMillis();
        if (Datalong > adesso) {
            return null;//se la data è maggiore di quella attuale allora ritrono subito null perchè non ho i prezzi
        }
        if (Datalong < Anno2017) {
            return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        }
        String DataOra = FunzioniDate.ConvertiDatadaLongallOra(Datalong);
        
        //Come prima cosa cerco nei nuovi personalizzati        
        risultato=DammiPrezzoDaDatabasePersonale(Crypto,Datalong,Fonte,Rete,Address,60,qta);
        if (risultato!=null){
            return risultato;
        }
        
        //Vedo se ho i prezzi all'ora (ormai tenuti solo per valorizzare anche il pregresso allo stesso modo)
        if (includiVecchi){
        risultato=new InfoPrezzo();
        String PrezzoUnitario = DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto);
        if (PrezzoUnitario != null){
            risultato.Fonte="DB Interno (Old)";
            if (DatabaseH2.XXXEUR_LeggiPers(DataOra + " " + Crypto)!=null)risultato.Fonte="Personalizzato (Old)";
            risultato.timestamp=FunzioniDate.ConvertiDatainLongMinuto(DataOra+":00");
            risultato.prezzoUnitario=new BigDecimal(PrezzoUnitario);
            risultato.Qta=new BigDecimal(Qta);
            risultato.Moneta=Crypto;
            return risultato;
        }
        }
        //Se non ho i prezzi all'ora (mantenuti per non variare i prezzi delle vecchie valorizzazioni, verifico se ho prezzi precisi
        risultato=DammiPrezzoDaDatabase(Crypto,Datalong,Fonte,Rete,Address,60,qta);
        if (risultato!=null){
            return risultato;
        }
        
        
          //Se non ho neanche i prezzi all'ora provo a scaricarli
          //System.out.println("mi mancano i prezzi di "+Crypto+" - "+Address+" - "+Rete+" - "+Exchange+" - "+Datalong+" - Qta: "+Qta);
          RecuperaPrezziDaCCXT(Crypto, Datalong);
          risultato = DammiPrezzoDaDatabase(Crypto, Datalong, Fonte, Rete, Address,5,qta);
          if (risultato!=null){
            return risultato;
          }
          
          //Se non trovo i prezzi scaricandoli dagli exchange allora tiro in balo cryptocompare, lo tiro in ballo per ultimo prchè i prezzi hanno una precisione oraria          
          RecuperaPrezziDaCryptoCompare(Crypto, Datalong);
          //Cerco il risultato nei 60 minuti in questo caso
          risultato = DammiPrezzoDaDatabase(Crypto, Datalong, Fonte, Rete, Address,60,qta);
          if (risultato==null){
              System.out.println("Nessun prezzo trovato per "+Crypto+" in data "+FunzioniDate.ConvertiDatadaLongAlSecondo(Datalong));
          }
          return risultato;

    } 
      
      
      
          public static String ZZZ_RecuperaTassidiCambioUSDTEUR_Binance(String DataIniziale, String DataFinale) {
                      
        String ok = null;
        String CoppiaCrypto="EURUSDT";
        long dataIni = FunzioniDate.ConvertiDatainLong(DataIniziale) ;
        long dataFin = FunzioniDate.ConvertiDatainLong(DataFinale) + 86400000;
        
        //come prima cosa invididuo i vari intervalli di date da interrogare per riempire tutto l'intervallo
        long difData=dataFin-dataIni;
        ArrayList<Long> ArraydataIni = new ArrayList<>();
        ArrayList<Long> ArraydataFin = new ArrayList<>();
        //dataFin=dataIni+7776000 ;//questa fa si che mi dia i prezzi orari
        //binance ha la seguente peculiarità:
        //riesco ad avere i prezzi orari fino a 1000 righe quindi fino a circa 40gg
        //per cui per ogni richiesta devo gestire la cosa
        //quindi, se ho meno di 40gg porto il range a 40gg e prendo il primo valore di ogni giorno per quanto riaguarda il giornaliero
        //e tutti gli altri valori per gli orari.
        //se ho più di 40 giorni devo dividere le richieste in multipli di 40 giorni fino ad arrivare alla data iniziale che desidero.
        //i multipli devono partire dalla data finale e andare indietro.
        //3456000 secondi equivalgono a 40gg (40giorni per la precisione).
        //inoltre tra una richiesta e l'altra devo aspettare almeno 2 secondi per evitare problemidi blocco ip da parte di coingecko
      // System.out.println(dataIni+" - "+dataFin);
        while (difData>0){
            ArraydataIni.add(dataIni);
            ArraydataFin.add(dataIni+Long.parseLong("3456000000"));
            dataIni=dataIni+Long.parseLong("3456000000");
            difData=dataFin-dataIni;    
           // i++;
        }
        
        
        
        

        for (int i = 0; i < ArraydataIni.size(); i++) {

            try {
                String apiUrl = "https://api.binance.com/api/v3/klines?symbol=EURUSDT&interval=1h&startTime=" + ArraydataIni.get(i) + "&endTime=" + ArraydataFin.get(i) + "&limit=1000";
                URL url = new URI(apiUrl).toURL();
                //System.out.println(url);
                            //questo serve per non fare chiamate api doppie, se non va è inutile riprovare
            if (Principale.Mappa_RichiesteAPIGiaEffettuate.get(url.toString())!=null){
                return null;
            }
            Principale.Mappa_RichiesteAPIGiaEffettuate.put(url.toString(), "ok");
                URLConnection connection = url.openConnection();
                //System.out.println(url);
                System.out.println("Recupero prezzi "+CoppiaCrypto+" da Binance da data "+DataIniziale);
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);

                    }

                    Gson gson = new Gson();
                    JsonArray pricesArray = gson.fromJson(response.toString(), JsonArray.class);
                    if (pricesArray != null) {
                        for (JsonElement element : pricesArray) {
                            JsonArray priceArray = element.getAsJsonArray();
                            //System.out.println(priceArray);

                            if (priceArray.size() == 12) {
                                long timestamp = priceArray.get(0).getAsLong();
                                String price = priceArray.get(4).getAsString();
                                Date date = new java.util.Date(timestamp);
                                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                                sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                                String DataconOra = sdf.format(date);
                                //  System.out.println("DataAttuale : "+DataAttualeLong);
                                //  System.out.println("timestamp : "+timestamp);
                                //  System.out.println(CercaPrezziDataAttuale);
                                //Questo if server per evitare di cercare i dati della data odierna se non sono proprio strettamente necessari
                                //perchè i dati della data odierna non vengono salvati nel file di conversione dei valori di usdt
                                //e questo genererebbe una richiesta inutile su coingecko
                                String Prezzo;
                                    Prezzo = new BigDecimal(1).divide(new BigDecimal(price), 30, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
                                
                                String Data=DataconOra.split(" ")[0];
                                if (DatabaseH2.XXXEUR_Leggi(Data + " USDT")==null||DatabaseH2.XXXEUR_Leggi(Data + " USDT").equals("ND")) 
                                    DatabaseH2.OLD_XXXEUR_Scrivi(Data + " USDT", Prezzo,false);
                                if (DatabaseH2.XXXEUR_Leggi(DataconOra + " USDT")==null||DatabaseH2.XXXEUR_Leggi(DataconOra + " USDT").equals("ND")) 
                                    DatabaseH2.OLD_XXXEUR_Scrivi(DataconOra + " USDT", Prezzo,false);

                                
                                ok = "ok";
                                //il prezzo ovviamente indica quanti euro ci vogliono per acquistare 1 usdt ovvero usdt/euro
                                //In questo modo metto nella mappa l'ultimo valore della giornata per ogni data + il valore per ogni ora
                                //System.out.println(MappaConversioneUSDTEUR.get(DataconOra) + " - " + DataconOra);
                                //ora devo gestire l'inserimento nella mappa
                            }

                        }
                    } else {
                        ok = null;
                    }
                } catch (IOException ex) {
                    ok = null;
                }
                TimeUnit.SECONDS.sleep(1);
            } catch (MalformedURLException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (InterruptedException ex) {
                ok = null;
                LoggerGC.ScriviErrore(ex);
            } catch (URISyntaxException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
        return ok;
    }   

      
          public static String ZZZ_ConvertiUSDTEUR(String Qta, long Datalong) {
        //come prima cosa verifizo se ho caricato il file di conversione e in caso lo faccio
        /*      if (MappaConversioneUSDTEUR.isEmpty())
            {
                GeneraMappaCambioUSDTEUR();
            }*/
        boolean mettereND=false;
        String risultato;// = null;
        //come prima cosa devo decidere il formato data
        long adesso = System.currentTimeMillis();
        if (Datalong > adesso) {
            return null;//se la data è maggiore di quella attuale non recupero nessun prezzo
        }
        if (Datalong < 1483225200) {
            return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        }
        String DataOra = FunzioniDate.ConvertiDatadaLongallOra(Datalong);
        String DataGiorno = FunzioniDate.ConvertiDatadaLong(Datalong);

        //cerco il valore di USDT da Binance
        risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        if (risultato == null) {
            //RecuperaCoppieBinance();
            ZZZ_RecuperaTassidiCambioUSDTEUR_Binance(DataGiorno, DataGiorno);
            risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        }
        
        //se ancora non ho il prezzo recupero il prezzo dall'altro provider ovvero Coinbase
        if (risultato == null) {
                ZZZ_RecuperaTassidiCambiodaSimbolo_Coinbase("USDT", DataGiorno);
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        }
        
        //se ancora non ho il prezzo recupero il prezzo dall'altro provider ovvero CryptoCompare       
        if (risultato == null) {
              //  RecuperaPrezzidaCryptoCompare("USDT", DataGiorno);
                risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        }
        //Cerco su Coingecko
        //Escludo momentaneamente coingecko visto che ho tanti altri provider a cui chiedere in modo da diminuirne le richieste
      /*  if (risultato == null) {
            RecuperaTassidiCambioUSDT_Coingecko(DataGiorno, DataGiorno);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
            risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
        }*/
        //Cerco su  CoinCap
        if (risultato == null) {
            ZZZ_RecuperaTassidiCambiodaSimbolo_CoinCap("USDT",DataGiorno);
            risultato = DatabaseH2.XXXEUR_Leggi(DataOra + " " + "USDT");
            //solo se arrivo fino a qua metto gli ND sulle ore che non sono riuscito a recuperare e per 30gg
            //che sono i giorni per cui di solito recupero i prezzi
            //Questo perchè questo è l'ultimo providfer da cui posso recuperare i dati quindi tutti i buchi di prezzo sono 
            //sicuro che non potrò farci nulla
            mettereND=true;
        }
        //se arrivo qua cerco il prezzo del giorno invece che quello orario
        if (risultato == null) {
            risultato = DatabaseH2.XXXEUR_Leggi(DataGiorno + " " + "USDT");
        } 
        
        if (mettereND) {
            //Adesso metto ad ND tutte le ore per cui non sono riuscito a recuperare il prezzo di USDT per evitare mille richieste prima di arrivare al prezzo
            long timestampIniziale = FunzioniDate.ConvertiDatainLong(DataGiorno);
            System.out.println(timestampIniziale);
            long timestampFinale = timestampIniziale + Long.parseLong("2592000000");
            if (adesso < timestampFinale) {
                timestampFinale = adesso;
            }
            while (timestampIniziale < timestampFinale) {
                Date date = new java.util.Date(timestampIniziale);
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                String DataconOra = sdf.format(date);
                //Metto ND su tutte le ore senza valore
                if (DatabaseH2.XXXEUR_Leggi(DataconOra + " USDT") == null) {
                    DatabaseH2.OLD_XXXEUR_Scrivi(DataconOra + " USDT", "ND",false);
                }

                timestampIniziale = timestampIniziale + 3600000;//aggiungo 1 ora
            }
        }    
        
        //se il risultato è ancora recupero il prezzo dai dollari reali
        if (risultato == null || risultato.equalsIgnoreCase("ND")||risultato.equalsIgnoreCase("null")) {
            //questo risultato è già ponderato in base al valore
            risultato = ConvertiUSDEUR(Qta, DataGiorno);
        } //altrimenti calcolo il risultato in base alle qta
        else {

            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
        }
        return risultato;
    }
          
             public static String ZZZ_RecuperaTassidiCambiodaSimbolo_Coinbase(String Crypto, String DataIniziale) {
        String ok = null;
        //Aggiungo 10 giorni alla data iniziale per trovare la data di fine
        //non posso superarare infatti i 300 risultati in una singola query
        //il limite sarebbe quindi di 12,5 gg. mi fermo a 10 per sicurezza.
        long dataFin = FunzioniDate.ConvertiDatainLong(DataIniziale) + Long.parseLong("864000000");
        long timestampIniziale = FunzioniDate.ConvertiDatainLong(DataIniziale);

            String apiUrl = "https://api.exchange.coinbase.com/products/" + Crypto + "-USD/candles?granularity=3600&start=" + timestampIniziale + "&end=" + dataFin;
            //System.out.println(apiUrl);
            
            try {
                URL url = new URI(apiUrl).toURL();
                //questo serve per non fare chiamate api doppie, se non va è inutile riprovare
                if (Principale.Mappa_RichiesteAPIGiaEffettuate.get(url.toString()) != null) {
                    return null;
                }
                Principale.Mappa_RichiesteAPIGiaEffettuate.put(url.toString(), "ok");
                URLConnection connection = url.openConnection();
                // System.out.println(url);
                System.out.println("Recupero prezzi " + Crypto + " da Coinbase da data " + DataIniziale);
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);

                    }
                    //System.out.println(response);
                    Gson gson = new Gson();
                    JsonArray pricesArray = gson.fromJson(response.toString(), JsonArray.class);
                     // Creazione di un nuovo JSONArray per contenere gli elementi in ordine inverso
                     //Lo faccio perchè Coinbase restituisce i prezzi in ordine cronologico inverso (dal più recente)
                     //ho bisogno che invece il dato sia in ordine cronologico per via che poi salvo il primo prezzo disponibile della giornata
                    JsonArray inverso = new JsonArray();
                    for (int i = pricesArray.size() - 1; i >= 0; i--) {
                        inverso.add(pricesArray.get(i));
                    }
                    if (!inverso.isEmpty()) {
                        for (JsonElement element : inverso) {
                            JsonArray dettagliArray=element.getAsJsonArray();
                            long Unixtime = dettagliArray.get(0).getAsLong()*1000;
                            String Data = FunzioniDate.ConvertiDatadaLong(Unixtime);
                            String DataOra = FunzioniDate.ConvertiDatadaLongallOra(Unixtime);
                            String prezzoUSD = dettagliArray.get(3).getAsString();
                            String PrezzoEuro = ConvertiUSDEUR(prezzoUSD, Data);
                            //System.out.println(DataOra);
                            //Controllo ora se non ha il prezzo e in quel caso lo scrivo
                            if (DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto) == null||DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto).equals("ND")) 
                            {
                                DatabaseH2.OLD_XXXEUR_Scrivi(DataOra + " " + Crypto, PrezzoEuro,false);
                            }
                            if (DatabaseH2.XXXEUR_Leggi(Data + " " + Crypto) == null||DatabaseH2.XXXEUR_Leggi(Data + " " + Crypto).equals("ND")) 
                            {
                                DatabaseH2.OLD_XXXEUR_Scrivi(Data + " " + Crypto, PrezzoEuro,false);
                            }
                        
                        }

                    } else {
                        ok = null;
                    }

                } catch (IOException ex) {
                    ok = null;
                }
                TimeUnit.SECONDS.sleep(1);

            } catch (MalformedURLException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (InterruptedException ex) {
                ok = null;
                LoggerGC.ScriviErrore(ex);
            } catch (URISyntaxException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        
        return ok;
    }     
          
    public static void RecuperaPrezziDaCryptoCompare(String Crypto, long timestamp) {

        try {

            Crypto = Crypto.toUpperCase().replaceAll("\\*", "").trim();
            
            //Siccome timestamp nelle api di cryptocompare corrisponde alla data dell'ultimo dato che voglio ricevere
            //e ricevo 30 gg di dati aggiungo sempre 20gg al timestamp in modo da avere i dati 10gg prima e 20 dopo la richiesta fatta.
            long Until = timestamp + Long.parseLong("1728000000");           
            long Adesso = System.currentTimeMillis();
            if (Until > Adesso) {
                //return;
                Until = Adesso;
            }
            long Since = Until - Long.parseLong("2592000000");
            //se ho già fatto questa richiesta in questa sessione termino immediatamente il ciclo
            //per questa richiesta visto che la precisione è di 1 ora voglio avere almeno 1 ora di dati avanti e indietro di intervallo e controllo quelli
            long SinceVerifica = timestamp - 3600000;
            long UntilVerifica = timestamp + 3600000;
            if (UntilVerifica > Adesso) {
                UntilVerifica = Adesso;
            }
            if (SinceVerifica > Adesso) {
                SinceVerifica = Adesso - 3600000;
            }
            //Se ho già effettuato la richiesta esco dal ciclo
            if (managerRichieste.isAlreadyRequested("CryptoCompare_" + Crypto, SinceVerifica, UntilVerifica)) {
                return;
            }
            TimeUnit.SECONDS.sleep(1);
            long Fine = Until / 1000;

            String apiUrl = "https://min-api.cryptocompare.com/data/v2/histohour?fsym=" + Crypto + "&tsym=EUR&limit=720&toTs=" + Fine;
            // System.out.println(apiUrl);
            managerRichieste.addRange("CryptoCompare_" + Crypto, Since, Until);
            URL url = new URI(apiUrl).toURL();
            
            //questo serve per non fare chiamate api doppie, se non va è inutile riprovare
            URLConnection connection = url.openConnection();
            // System.out.println(url);
            System.out.println("Recupero prezzi " + Crypto + " da CryptoCompare per la data " + FunzioniDate.ConvertiDatadaLong(timestamp));
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);

                }
                //System.out.println(response);
                Gson gson = new Gson();
                JsonObject JsonObj = gson.fromJson(response.toString(), JsonObject.class);
                String Risposta = JsonObj.get("Response").getAsString();
                if (Risposta.equalsIgnoreCase("Success")) {
                    //Se la richiesta ha successo vado a leggere i dati delle crypto
                    JsonArray pricesArray = JsonObj.getAsJsonObject("Data").getAsJsonArray("Data");

                    //  List<PrezzoData> prezzoDataList = new ArrayList<>();
                    if (pricesArray != null && pricesArray.size() > 0) {
                        String mergeSql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, ?, ?)";

                        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(mergeSql)) {

                            for (JsonElement element : pricesArray) {
                               // JsonObject priceArray = element.getAsJsonObject();
                               /* if (priceArray.size() != 2) {
                                    continue;
                                }*/

                                long UnixTime = element.getAsJsonObject().get("time").getAsLong() * 1000;
                                double prezzo;
                                try {
                                    prezzo = element.getAsJsonObject().get("open").getAsDouble();
                                    if (prezzo==0)continue;//se il prezzo non è valorizzato non lo salvo
                                } catch (Exception ex) {
                                    continue; // skip valori non numerici
                                }

                                ps.setLong(1, UnixTime);
                                ps.setString(2, "CryptoCompare");
                                ps.setString(3, Crypto);
                                ps.setDouble(4, prezzo);
                                ps.setString(5, "");
                                ps.setString(6, "");
                                ps.addBatch();
                            }

                            ps.executeBatch();

                        } catch (SQLException ex) {
                            LoggerGC.ScriviErrore(ex);
                        }
                    }
                }
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }

        } catch (InterruptedException | URISyntaxException | MalformedURLException ex) {
            LoggerGC.ScriviErrore(ex);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }  

        public static String ZZZ_RecuperaTassidiCambiodaSimbolo_CoinCap(String Crypto, String DataIniziale) {
        String ApiKey = Funzioni.TrasformaNullinBlanc(DatabaseH2.Opzioni_Leggi("ApiKey_Coincap"));
        if (!ApiKey.isBlank()) {
            String ok = null;
            long dataFin = FunzioniDate.ConvertiDatainLong(DataIniziale) + Long.parseLong("864000000");
            long timestampIniziale = FunzioniDate.ConvertiDatainLong(DataIniziale);
            String ID = DatabaseH2.GestitiCoinCap_Leggi(Crypto);

            //String DataFinale = OperazioniSuDate.ConvertiDatadaLong(dataFin);

            String apiUrl = "https://rest.coincap.io/v3/assets/" + ID + "/history?interval=h1&start=" + timestampIniziale + "&end=" + dataFin;
            apiUrl=apiUrl+"&apiKey="+ApiKey;
            //System.out.println(apiUrl);

            try {
                URL url = new URI(apiUrl).toURL();
                //questo serve per non fare chiamate api doppie, se non va è inutile riprovare
                if (Principale.Mappa_RichiesteAPIGiaEffettuate.get(url.toString()) != null) {
                    return null;
                }
                Principale.Mappa_RichiesteAPIGiaEffettuate.put(url.toString(), "ok");
                URLConnection connection = url.openConnection();
                // System.out.println(url);
                System.out.println("Recupero prezzi " + Crypto + " da Coincap da data " + DataIniziale);
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);

                    }
                    //System.out.println(response);
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                    JsonArray pricesArray = jsonObject.getAsJsonArray("data");
                    //List<String> simboli = new ArrayList<>();
                    if (pricesArray != null || !pricesArray.isEmpty()) {
                        for (JsonElement element : pricesArray) {

                            JsonObject Coppie = element.getAsJsonObject();
                            String prezzoUSD = Coppie.get("priceUsd").getAsString();
                            long Unixtime = Coppie.get("time").getAsLong();
                            //System.out.println(prezzoUSD+" - "+Unixtime);
                            String Data = FunzioniDate.ConvertiDatadaLong(Unixtime);
                            String DataOra = FunzioniDate.ConvertiDatadaLongallOra(Unixtime);
                            String PrezzoEuro = ConvertiUSDEUR(prezzoUSD, Data);
                            //Controllo ora se non ha il prezzo e in quel caso lo scrivo
                            if (DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto) == null || DatabaseH2.XXXEUR_Leggi(DataOra + " " + Crypto).equals("ND")) {
                                DatabaseH2.OLD_XXXEUR_Scrivi(DataOra + " " + Crypto, PrezzoEuro, false);
                            }
                            if (DatabaseH2.XXXEUR_Leggi(Data + " " + Crypto) == null || DatabaseH2.XXXEUR_Leggi(Data + " " + Crypto).equals("ND")) {
                                DatabaseH2.OLD_XXXEUR_Scrivi(Data + " " + Crypto, PrezzoEuro, false);
                            }

                        }

                    } else {
                        ok = null;
                    }

                } catch (IOException ex) {
                    ok = null;
                }
                TimeUnit.SECONDS.sleep(1);

            } catch (MalformedURLException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (InterruptedException ex) {
                ok = null;
                LoggerGC.ScriviErrore(ex);
            } catch (URISyntaxException ex) {
                LoggerGC.ScriviErrore(ex);
            }

            return ok;
        } else {
            return null;
        }
    }  
      
      
    
    public static String RecuperaTassidiCambio(String DataIniziale,String DataFinale)  {      
        String ok="ok";
        try {     
            TimeUnit.SECONDS.sleep(1);
            URL url = new URI("https://tassidicambio.bancaditalia.it/terzevalute-wf-web/rest/v1.0/dailyTimeSeries?startDate="+DataIniziale+"&endDate="+DataFinale+"&baseCurrencyIsoCode=EUR&currencyIsoCode=USD").toURL();
                        //questo serve per non fare chiamate api doppie, se non va è inutile riprovare
            if (Principale.Mappa_RichiesteAPIGiaEffettuate.get(url.toString())!=null){
                return null;
            }
            Principale.Mappa_RichiesteAPIGiaEffettuate.put(url.toString(), "ok");
            URLConnection connection = url.openConnection();
           // System.out.println(url);
            System.out.println("Recupero tassi di cambio Euro-Dollaro da Bancaditalia da data "+DataIniziale);
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())))
            {
                String line;
                while ((line = in.readLine()) != null) {
                    String rigaSplittata[]=line.split(",");
                    if (rigaSplittata.length==6)
                    {
                       if (rigaSplittata[0].equalsIgnoreCase("Euro")) MappaConversioneUSDEUR.put(rigaSplittata[5], rigaSplittata[3]);
                    }
                }
                ScriviFileConversioneUSDEUR();

            } catch (IOException ex) {
              //  Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
                ok=null;
            } 
        }  catch (MalformedURLException ex) {
           // Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            ok=null;
        } catch (IOException ex) {
          //  Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            ok=null;
        } catch (URISyntaxException | InterruptedException ex) {
            LoggerGC.ScriviErrore(ex);
            //Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ok;
     }
 
    
    public static String RecuperaTassidiCambiodaAddress_Coingecko(String DataIniziale, String Address, String Rete, String Simbolo) {

        RecuperaCoinsCoingecko();
        String AddressNoPrezzo = DatabaseH2.GestitiCoingecko_Leggi(Address + "_" + Rete);
            if (AddressNoPrezzo == null) {
                return null;
            }
        //Se la moneta non è nelle liste coingecko annullo la richiesta
        
        //se ho già fatto questa richiesta in questa sessione termino immediatamente il ciclo
        //per questa richiesta volgio aver già fatto la richiesta almeno per 1h prima e 1h dopo
        long SinceVerifica=FunzioniDate.ConvertiDatainLong(DataIniziale)- 3600000;
        long UntilVerifica=FunzioniDate.ConvertiDatainLong(DataIniziale)+ 3600000;
        long dataAdesso1 = System.currentTimeMillis();
        if (dataAdesso1<UntilVerifica)UntilVerifica=dataAdesso1;
        //System.out.println("Cerco "+Address+"_"+Rete+" nel manager richieste ");
        //System.out.println("Since "+OperazioniSuDate.ConvertiDatadaLongAlSecondo(SinceVerifica)+" - Until "+OperazioniSuDate.ConvertiDatadaLongAlSecondo(UntilVerifica));
        if(managerRichieste.isAlreadyRequested(Address+"_"+Rete, SinceVerifica, UntilVerifica))return null;
        //else System.out.println("Non ho nulla nel manager proseguo con le richieste");
        //Se cerco di richiedere una data futura annullo
        if (dataAdesso1<SinceVerifica)return null;
        
        if (Principale.Mappa_ChainExplorer.get(Rete) == null) {
            return null;
        }
        //Verifico se ho le Api di coingecko
        String ApiKey = Funzioni.TrasformaNullinBlanc(DatabaseH2.Opzioni_Leggi("ApiKey_Coingecko"));

        long dataAdesso = System.currentTimeMillis() / 1000;

        long dataIni = (FunzioniDate.ConvertiDatainLong(DataIniziale) / 1000) - 86400;
        //Controllo la data di oggi e la confronto con la data iniziale, se la data iniziale è superiore a quella di oggi meno 90 gg allora la porto
        //a data di oggi meno 90gg, così sfrutto meglio le chiamate api e riesco a prendere i prezzi vecchi con la chiamata di una data recente
        //90gg sono 7776000 secondi
        if ((dataAdesso - dataIni) < 7776000) {
            dataIni = dataAdesso - 7776000;
        }
        long dataFin = dataIni + 7776000;
        //se la differenza tra la data iniziale e la data odierna è maggiore di 365 gg termino il ciclo in quanto non posso avere i prezzi
        if ((dataAdesso - dataIni) > Long.parseLong("31536000")) {
            return null;
        }
        //in questo caso ovviamente non vado avanti con la funzione che tanto non posso trovare i prezzi

        //come prima cosa invididuo i vari intervalli di date da interrogare per riempire tutto l'intervallo
        //dataFin=dataIni+7776000 ;//questa fa si che mi dia i prezzi orari
        //coingeko ha la seguente peculiarità:
        //se richiedo piu' di 90gg mi da i prezzi giornalieri
        //da 1 giorno a 90 da i prezzi ogni ora
        //se invece chiediamo meno di 1 giorno da i prezzi ogni 5 minuti
        //quello che interessa a me è avere i prezzi giornalieri (come backup) e quelli orari
        //per cui per ogni richiesta devo gestire la cosa
        //quindi, se ho meno di 3 mese porto il range a 3 mesi e prendo il primo valore di ogni giorno per quanto riaguarda il giornaliero
        //e tutti gli altri valori per gli orari.
        //se ho più di 3 mesi devo dividere le richieste in multipli di 3 mesi fino ad arrivare alla data iniziale che desidero.
        //i multipli devono partire dalla data finale e andare indietro.
        //7776000 secondi equivalgono a 3 mesi (90giorni per la precisione).
        //inoltre tra una richiesta e l'altra devo aspettare almeno 2 secondi per evitare problemidi blocco ip da parte di coingecko
        try {
            //System.out.println("Attendo 12 ");
            if (ApiKey.isBlank()) {
                TimeUnit.SECONDS.sleep(13);
            }//il timeout serve per evitare di fare troppe richieste all'API
            else {
                TimeUnit.SECONDS.sleep(3);
            }//Se ho le apikey posso fare molte più richieste quindi metto a 3 secondi il timeout

            URL url = new URI("https://api.coingecko.com/api/v3/coins/" + Principale.Mappa_ChainExplorer.get(Rete)[3] + "/contract/" + Address + "/market_chart/range?vs_currency=EUR&from=" + dataIni + "&to=" + dataFin).toURL();

           /* if (CDC_Grafica.Mappa_RichiesteAPIGiaEffettuate.get(url.toString()) != null) {
                return null;
            }
            CDC_Grafica.Mappa_RichiesteAPIGiaEffettuate.put(url.toString(), "ok");*/
            System.out.println("Recupero prezzi token " + Simbolo + " con Address " + Address + " da coingecko su rete " + Principale.Mappa_ChainExplorer.get(Rete)[3]
                    + " da data " + FunzioniDate.ConvertiDatadaLongAlSecondo(dataIni * 1000));
            OkHttpClient client = new OkHttpClient();
            Request request;
            if (ApiKey.isBlank()){ 
                request= new Request.Builder()
                            .url(url)
                            .build();
            }
            else {
                request= new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("accept", "application/json")
                            .addHeader("x-cg-demo-api-key", ApiKey)
                            .build();
            }
            try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Errore nel recupero dei prezzi del token "+Simbolo+" con Address "+Address+" su rete "+Principale.Mappa_ChainExplorer.get(Rete)[3]);
                return null; // Errore di connessione o di altro genere
            }
           
            String responseBody = response.body().string();

            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray pricesArray = json.getAsJsonArray("prices");
            //  List<PrezzoData> prezzoDataList = new ArrayList<>();
            if (pricesArray != null && pricesArray.size() > 0) {
                String mergeSql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                        + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(mergeSql)) {

                    for (JsonElement element : pricesArray) {
                        JsonArray priceArray = element.getAsJsonArray();
                        if (priceArray.size() != 2) {
                            continue;
                        }

                        long timestamp = priceArray.get(0).getAsLong();
                        double prezzo;
                        try {
                            prezzo = priceArray.get(1).getAsDouble();
                        } catch (Exception ex) {
                            continue; // skip valori non numerici
                        }

                        ps.setLong(1, timestamp);
                        ps.setString(2, "coingecko");
                        ps.setString(3, "");
                        ps.setDouble(4, prezzo);
                        ps.setString(5, Rete);
                        ps.setString(6, Address);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    
                    

                } catch (SQLException ex) {
                    LoggerGC.ScriviErrore(ex);
                    //Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, "Errore durante inserimento prezzi CoinGecko", ex);
                }
            } else {
                managerRichieste.addRange(Address+"_"+Rete, dataIni*1000, dataFin*1000);
                return null;
            }

              }
        } catch (JsonSyntaxException | IOException | InterruptedException | URISyntaxException ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
        
        //Se arrivo qua inserisco nel manager delle richieste il fatto che ho già richiseto il prezzo
        System.out.println("Inserisco "+Address+"_"+Rete+" nel manager richieste");
        System.out.println("Since "+FunzioniDate.ConvertiDatadaLongAlSecondo(dataIni*1000)+" - Until "+FunzioniDate.ConvertiDatadaLongAlSecondo(dataFin*1000));
        managerRichieste.addRange(Address+"_"+Rete, dataIni*1000, dataFin*1000);

        return "ok";
    }
    
    

    public static void RitornaPrezzoDaInfoPrezzo()
    {
    
    }
 


    public static String DammiPrezzoTransazioneSalvaInfoPrezzo(Moneta Moneta1a, Moneta Moneta2a, long Data, String Prezzo, boolean PrezzoZero, int Decimali, String Rete,String fonte,Map<String,InfoPrezzo> Mappa,String ChiaveMappa) {

        InfoPrezzo IP=DammiPrezzoInfoTransazione(Moneta1a, Moneta2a, Data, Rete,fonte);
        if (IP!=null){
            if (Moneta1a!=null&&Moneta1a.Moneta.equals(IP.Moneta)){
                IP.OggettoMoneta=Moneta1a;
            }else if (Moneta2a!=null&&Moneta2a.Moneta.equals(IP.Moneta)){
                IP.OggettoMoneta=Moneta2a;
            }
            Mappa.put(ChiaveMappa, IP);
           // if(IP.OggettoMoneta!=null) System.out.println(ChiaveMappa+IP.OggettoMoneta.Moneta);
            
        }
        
        
        if (IP!=null)
        {
            if (IP.prezzoQta==null)IP.prezzoQta=IP.Qta.multiply(IP.prezzoUnitario);
            return IP.prezzoQta.setScale(Decimali,RoundingMode.HALF_UP).toPlainString();
        }
        if (PrezzoZero) {
            return "0.00";
        } else {
            return Prezzo;   
        }
    }





    public static String DammiPrezzoTransazione(Moneta Moneta1a, Moneta Moneta2a, long Data, String Prezzo, boolean PrezzoZero, int Decimali, String Rete,String fonte) {

        InfoPrezzo IP=DammiPrezzoInfoTransazione(Moneta1a, Moneta2a, Data, Rete,fonte);
        if (IP!=null)
        {
            if (IP.prezzoQta==null)IP.prezzoQta=IP.Qta.multiply(IP.prezzoUnitario).abs();
            return IP.prezzoQta.setScale(Decimali,RoundingMode.HALF_UP).abs().toPlainString();
        }
        if (PrezzoZero) {
            return "0.00";
        } else {
            return Prezzo;
        }
    }
    
    public static InfoPrezzo DammiPrezzoInfoTransazione(Moneta Moneta1a, Moneta Moneta2a, long Data, String Rete,String fonte) {

        InfoPrezzo IP;

 /*Questa funzione si divide in 4 punti fondamentali:
        1 - Verifico che una delle 2 monete di scambio sia una Fiat e in quel caso prendo quello come prezzo della transazione anche perchè è il più affidabile
        2 - Verifico se una delle 2 monete è USDT in quel caso prendo quello come valore in quanto USDT è una moneta di cui mi salvo tutti i prezzi storici
        3 - Verifico se una delle 2 monete non faccia parte di uno specifico gruppo delle monete più capitalizzate presenti su binance, in quel caso prendo quello come
        prezzo della transazione in quanto il prezzo risulta sicuramente più preciso di quello di una shitcoin o comunque di una moneta con bassa liquidità
        4 - Prendo il prezzo della prima moneta disponibile essendo che l'affidabilità del prezzo è la stessa per entrambe le monete dello scambio      
         */

        Moneta MonOri[] = new Moneta[]{Moneta1a, Moneta2a};

        //PARTE 1 - ANALISI PRELIMINARE DATI
        
        //A - Clono le monete in quanto altrimenti potrei andare ad alterarle nel corso del ciclo per la richiesta dei prezzi
        Moneta mon[] = new Moneta[2];
        //Se la moneta in questione non è valorizzata correttamente non la clono e quindi resterà null
        for (int k = 0; k < 2; k++) {
            if (MonOri[k] != null
                    && MonOri[k].Moneta != null
                    && !MonOri[k].Moneta.isBlank()) {
                mon[k] = MonOri[k].ClonaMoneta();
            }
        }

        //B - Questa parte impone la ricerca su binance per determinati token salvati nella mappa
        //questo rende più veloce la ricerca e più affiabile
        //es. USDT su rete BSC o su rete CRO li cerco in ogni caso su Binance rendendo anche univoco il prezzo tra le varie chain
        for (int k = 0; k < 2; k++) {
            if (mon[k] != null && Principale.Mappa_AddressRete_Nome.get(mon[k].MonetaAddress + "_" + Rete) != null) {
                mon[k].Moneta = Principale.Mappa_AddressRete_Nome.get(mon[k].MonetaAddress + "_" + Rete);
                mon[k].MonetaAddress=null;

            }
        }

    
        //PARTE 2 - STABILISCO LA PRIORITA' DI ASSEGNAZIONE PREZZI SUI TOKEN
        //(In caso in cui la transazioni presenti 2 token scelgo quale token determinerà iol prezzo della transazione con queasta priorità:)
        //A - FIAT
        //B - STABLECOIN
        //C - Selezione di Crypto ad alta capitalizzazione (quindi con meno oscillazioni)
        
        //A - VERIFICO SE FIAT EURO (in quel caso prendo quel prezzo per la transazione che è il più accurato)
        for (int k = 0; k < 2; k++) {
            if (mon[k] != null && mon[k].Tipo.trim().equalsIgnoreCase("FIAT") && mon[k].Moneta.equalsIgnoreCase("EUR")) {
                if(mon[k].Qta!=null){
                    BigDecimal PrezzoTransazione = new BigDecimal(mon[k].Qta);
                    PrezzoTransazione = PrezzoTransazione.abs().stripTrailingZeros();
                    IP=new InfoPrezzo();
                    IP.Moneta=mon[k].Moneta;
                    IP.Qta=PrezzoTransazione;
                    IP.Fonte="";
                    IP.prezzoUnitario=new BigDecimal("1");
                    IP.prezzoQta=PrezzoTransazione;
                    IP.timestamp=Data;   
                    IP.OggettoMoneta=mon[k];
                    return IP;
                }
            }
        }
        
        //VERIFICO SE USD e prendo il prezzo da li
            for (int k=0;k<2;k++){
            if (mon[k] != null && mon[k].Moneta.equalsIgnoreCase("USD") && !mon[k].Tipo.trim().equalsIgnoreCase("NFT")&&mon[k].MonetaAddress == null) {
                //a seconda se ho l'address o meno recupero il suo prezzo in maniera diversa
                //anche perchè potrebbe essere che sia un token che si chiama usdt ma è scam
                String DataDollaro=FunzioniDate.ConvertiDatadaLong(Data);
                String PT = ConvertiUSDEUR("1", DataDollaro);                
                if (PT != null) {
                    BigDecimal PrezzoUnitario = new BigDecimal(PT).abs().stripTrailingZeros();
                    IP=new InfoPrezzo();
                    IP.Moneta=mon[k].Moneta;
                    IP.Qta=new BigDecimal(mon[k].Qta);
                    IP.Fonte="bancaditalia";
                    IP.prezzoUnitario=PrezzoUnitario;
                    IP.prezzoQta=PrezzoUnitario.multiply(new BigDecimal(mon[k].Qta).abs());
                    IP.timestamp=Data;  
                    IP.OggettoMoneta=mon[k];
                    return IP;
                }
            } 
            }

         //se non sono FIAT controllo se una delle coppie è USDT in quel caso prendo il prezzo di quello 
        
            //B e C - VERIFICO SE COPPIE PRIORITARIE
            //ora scorro le coin principali per vedere se trovo corrispondenze e in quel caso ritorno il prezzo
            //I simboli vengono interrogati per ordine di importanza ovvero nell'ordine in cui sono stati inseriti nella variabile
        
            for (String SimboloPrioritario : SimboliPrioritari) {
                for (int k = 0; k < 2; k++) {
                if (mon[k] != null && (mon[k].Moneta).toUpperCase().equals(SimboloPrioritario) && mon[k].Tipo.trim().equalsIgnoreCase("Crypto")) {
                    //come prima cosa provo a vedere se ho un prezzo personalizzato e uso quello
                            IP=CambioXXXEUR(mon[k].Moneta, mon[k].Qta, Data, mon[k].MonetaAddress, Rete,fonte,true);
                            if (IP!=null)
                            {
                                IP.OggettoMoneta=mon[k];
                                return IP;
                            }
                    }
                }
            }
        
            //Se arrivo qua vuol dire che non ho trovato il prezzo tra le coppie prioritarie
            //a questo punto controllo se ho l'address delle monetee controllo su coingecko.
            //a questo punto la cerco tra tutte le coppie che binance riconosce

            //PARTE 4 - Prendo il prezzo della prima moneta disponibile
            for (int k = 0; k < 2; k++) {
            if (mon[k] != null) {
                //Se non ho l'address cerco su binance altrimenti cerco su coingecko

                        IP=CambioXXXEUR(mon[k].Moneta, mon[k].Qta, Data, mon[k].MonetaAddress, Rete,fonte,true);
                        if (IP!=null)
                        {
                            IP.OggettoMoneta=mon[k];
                            return IP;
                        }
            }
        }

        //Se arrivo qua vuol dire che non ho trovato nessun prezzo quindi ritorno null
        return null;

    }

    
    //Recupero le coppie con il dollaro gestite da coinbase
    public static String RecuperaCoppieCoinbase() {
        String ok = "ok";
        //come prima cosa recupero l'ora atuale
        //poi la verifico con quella dell'ultimo scarico da binance e se sono passate almeno 24h allora richiedo la nuova lista
        //altrimenti tengo buona quella presente nel database
        long adesso = System.currentTimeMillis();
        String dataUltimoScaricoString = DatabaseH2.Opzioni_Leggi("Data_Lista_Coinbase");
        long dataUltimoScarico = 0;
        if (dataUltimoScaricoString != null) {
            dataUltimoScarico = Long.parseLong(dataUltimoScaricoString);
        }
        if (adesso > (dataUltimoScarico + 86400000)) {
            try {
                String apiUrl = "https://api.exchange.coinbase.com/products";
                URL url = new URI(apiUrl).toURL();
                URLConnection connection = url.openConnection();
                System.out.println("Recupero coppie gestite da Coinbase");
                //System.out.println(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    //System.out.println(line);
                }

                Gson gson = new Gson();
                JsonArray pricesArray = gson.fromJson(response.toString(), JsonArray.class);
               // System.out.println(pricesArray.size());
                //JsonArray pricesArray = jsonObject.getAsJsonArray();//.getAsJsonArray("symbols");
                List<String> simboli = new ArrayList<>();
                if (pricesArray != null) {
                    for (JsonElement element : pricesArray) {
                        
                        JsonObject Coppie = element.getAsJsonObject();
                        //String id = Coppie.get("id").getAsString();
                        String base_currency = Coppie.get("base_currency").getAsString();
                        String quote_currency = Coppie.get("quote_currency").getAsString();
                        //System.out.println(quote_currency);
                        if (quote_currency.equals("USD")) {
                            simboli.add(base_currency);
                        }
                    }
                    DatabaseH2.GestitiCoinbase_ScriviNuovaTabella(simboli);
                    DatabaseH2.Opzioni_Scrivi("Data_Lista_Coinbase", String.valueOf(adesso));
                } else {
                    ok = null;
                }

                TimeUnit.SECONDS.sleep(1);
            } catch (JsonSyntaxException | IOException | InterruptedException ex) {
                ok = null;
            } catch (URISyntaxException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
        return ok;
    }
    
    
    
    
    
    
    //questa funzione la chiamo sempre una sola volta per verificare quali sono le coppie di trading di cui binance mi fornisce i dati    
    public static String RecuperaTassiCambioEURUSD() {
        if (MappaConversioneUSDEUR.isEmpty())
            {
                GeneraMappaCambioUSDEUR();
            }
        Object dateDisponibili[] = MappaConversioneUSDEUR.keySet().toArray();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String PrimaData="0000-00-00";//è la prima data disponibile nel file delle conversioni
        String UltimaData="0000-00-00";//e' l'ultima data disponibile nel file delle conversioni                                 
        if (dateDisponibili.length > 0) {// se il file ha dei dati recupero prima e ultima data      
            PrimaData = (String) dateDisponibili[0];
            UltimaData = (String) dateDisponibili[dateDisponibili.length - 1];
        }
        String ok = "ok";
        //come prima cosa recupero l'ora atuale
        //poi la verifico con quella dell'ultimo scarico da binance e se sono passate almeno 24h allora richiedo la nuova lista
        //altrimenti tengo buona quella presente nel database
        long adesso = System.currentTimeMillis();
        String dataUltimoScaricoString = DatabaseH2.Opzioni_Leggi("Data_TassiCambio_USDEUR");
        long dataUltimoScarico = 0;
        if (dataUltimoScaricoString != null) {
            dataUltimoScarico = Long.parseLong(dataUltimoScaricoString);
        }
        if (adesso > (dataUltimoScarico + 86400000)) {
            String DataIniziale=UltimaData;
            String DataFinale=FunzioniDate.ConvertiDatadaLong(adesso);
            if (!PrimaData.equals("2015-01-02"))DataIniziale="2015-01-02";
            ok=RecuperaTassidiCambio(DataIniziale,FunzioniDate.ConvertiDatadaLong(adesso));
            if (ok!=null){
                DatabaseH2.Opzioni_Scrivi("Data_TassiCambio_USDEUR", String.valueOf(adesso));
                }
        }
        return ok;
    }
        

     
        public static String RecuperaCoinsCoinCap() {
        String ok = "ok";
        //come prima cosa recupero l'ora atuale
        //poi la verifico con quella dell'ultimo scarico da binance e se sono passate almeno 24h allora richiedo la nuova lista
        //altrimenti tengo buona quella presente nel database
        
        long adesso = System.currentTimeMillis();
        //String dataUltimoScaricoString = DatabaseH2.Opzioni_Leggi("Data_Lista_CryptoHistory");
        String dataUltimoScaricoString = DatabaseH2.Opzioni_Leggi("Data_Lista_CoinCap");
        long dataUltimoScarico = 0;
        if (dataUltimoScaricoString != null) {
            dataUltimoScarico = Long.parseLong(dataUltimoScaricoString);
        }
        if (adesso > (dataUltimoScarico + 86400000)) {
            System.out.println("Recupero gestiti Coincap+CryptoCompare");
            //ORA che coincap è a pagamento non posso usare più la richiesta api diretta perchè da sola farebbe fuori il 10% delle richieste mensili
            //al posto suo prenderò i dati da un file csv precedentemente preparato che vedrò come tenere aggiornato nel futuro
            
            String riga;
        try (FileReader fire = new FileReader(Statiche.getPathRisorse()+"Gestiticoincap.csv"); 
                BufferedReader bure = new BufferedReader(fire);) 
            {
                List<String[]> gestiti = new ArrayList<>();
                while((riga=bure.readLine())!=null)
                {
                    String rigaSplittata[]=riga.split(";");
                    gestiti.add(rigaSplittata);
                }
                DatabaseH2.GestitiCoinCap_ScriviNuovaTabella(gestiti);
                DatabaseH2.Opzioni_Scrivi("Data_Lista_CoinCap", String.valueOf(adesso));
            
        }   catch (FileNotFoundException ex) {
                LoggerGC.ScriviErrore(ex);
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }
           
        }
        return ok;
    }   
     
    
        public static String RecuperaCoinsCoingecko() {
        String ok = "ok";
        //come prima cosa recupero l'ora atuale
        //poi la verifico con quella dell'ultimo scarico da binance e se sono passate almeno 24h allora richiedo la nuova lista
        //altrimenti tengo buona quella presente nel database
        long adesso = System.currentTimeMillis();
        String dataUltimoScaricoString = DatabaseH2.Opzioni_Leggi("Data_Lista_Coingecko");
        long dataUltimoScarico = 0;
        if (dataUltimoScaricoString != null) {
            dataUltimoScarico = Long.parseLong(dataUltimoScaricoString);
        }
        if (adesso > (dataUltimoScarico + 86400000)) {
            try {
                String apiUrl = "https://api.coingecko.com/api/v3/coins/list?include_platform=true";
                URL url = new URI(apiUrl).toURL();
                URLConnection connection = url.openConnection();
                System.out.println("Recupero token gestiti da Coingecko");
               // System.out.println(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    //System.out.println(line);
                }
                List<String[]> gestiti = new ArrayList<>();
                String jsonString=response.toString();
               // String jsonString=in.readLine();
               JsonElement jsonElement = JsonParser.parseString(jsonString);

                if (jsonElement.isJsonArray()) {
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    for (JsonElement element : jsonArray) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        String Simbolo = jsonObject.get("symbol").getAsString();
                        String Nome = jsonObject.get("name").getAsString();
                        JsonObject platformsObject = jsonObject.getAsJsonObject("platforms");
                        String IndirizziCoins[]=new String[Principale.Mappa_ChainExplorer.size()];
                        int i=0;
                        for(String Rete : Principale.Mappa_ChainExplorer.keySet())
                        {
                            String nomeReteCoingecko=Principale.Mappa_ChainExplorer.get(Rete)[3];
                            IndirizziCoins[i]=platformsObject.has(nomeReteCoingecko) ? platformsObject.get(nomeReteCoingecko).getAsString() : null;
                            if (IndirizziCoins[i]!=null&&!IndirizziCoins[i].isEmpty()){
                                String Gestito[]=new String[3];
                                if (Rete.equals("SOL"))Gestito[0]=(IndirizziCoins[i]+"_"+Rete);
                                else Gestito[0]=(IndirizziCoins[i]+"_"+Rete).toUpperCase();
                                Gestito[1]=Simbolo;
                                Gestito[2]=Nome;
                                gestiti.add(Gestito);
                            }
                            i++;
                            
                        }
                      
                    }
                   // System.out.println("sono qui");
                   String Gestito[]=new String[3];
                   Gestito[0]="CRO_CRO";
                   Gestito[1]="CRO";
                   Gestito[2]="Crypto.com Coin";
                   gestiti.add(Gestito);
                    DatabaseH2.GestitiCoingecko_ScriviNuovaTabella(gestiti);
                    DatabaseH2.Opzioni_Scrivi("Data_Lista_Coingecko", String.valueOf(adesso));
                }else {
                    ok=null;
                        }
                TimeUnit.SECONDS.sleep(5);
            } catch (JsonSyntaxException | IOException | InterruptedException ex) {
                LoggerGC.ScriviErrore(ex);
                ok = null;
            } catch (URISyntaxException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
        return ok;
    }
    
    
     static void ScriviFileConversioneUSDEUR() { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
      // System.out.println("ScriviFileConversioneUSDEUR : "+Statiche.getFileUSDEUR());
       FileWriter w=new FileWriter(Statiche.getFileUSDEUR());
       BufferedWriter b=new BufferedWriter (w);
       
       Object DateCambi[]=MappaConversioneUSDEUR.keySet().toArray();
       
       for (int i=0;i<DateCambi.length;i++){
           b.write(DateCambi[i].toString()+","+MappaConversioneUSDEUR.get(DateCambi[i].toString())+"\n");
       }
       
      // b.write("DataIniziale="+CDC_DataIniziale+"\n");
       //System.out.println(CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
       b.close();
       w.close();

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }
     

     
     
     
/**
 * Recupera dal database il prezzo più vicino a un determinato timestamp per un dato token.
 * <p>
 * La funzione ricerca il valore nel database <b>PrezziNew</b> considerando le seguenti regole:
 * <ul>
 *   <li>Se viene specificato un exchange preferito, la ricerca viene effettuata prima su di esso.</li>
 *   <li>Se non viene trovato alcun risultato sull’exchange preferito (o se non specificato),
 *       la ricerca prosegue su tutti gli altri exchange.</li>
 *   <li>Il timestamp trovato non può differire di oltre ±5 minuti (300 secondi) dal timestamp richiesto.</li>
 *   <li>È possibile filtrare ulteriormente la ricerca per rete e address (facoltativi).</li>
 * </ul>
 *
 * @param symbol              il simbolo del token (es. "BTC")
 * @param timestampRiferimento il timestamp di riferimento (in millisecondi)
 * @param exchangePreferito   l’exchange da cui preferibilmente recuperare il prezzo
 *                            (può essere {@code null} o vuoto per cercare su tutti)
 * @param rete                la rete blockchain associata al token (facoltativa, può essere vuota)
 * @param address             l’indirizzo del token (facoltativo, può essere vuoto)
 * @param minuti              Precisione minima in minuti
     * @param Qta
 * @return un oggetto {@link PrezzoInfo} contenente il prezzo, l’exchange e il timestamp effettivo del prezzo,
 *         oppure {@code null} se non è stato trovato alcun prezzo valido entro 5 minuti dal timestamp richiesto
 *
 * @see PrezzoInfo
 * @implNote Il campo {@code timestamp} del database e del parametro di input devono essere espressi
 *           nella stessa unità temporale (millisecondi). Se il database usa secondi UNIX,
 *           adattare la logica di confronto di conseguenza.
 */
public static InfoPrezzo DammiPrezzoDaDatabase(
        String symbol,
        long timestampRiferimento,
        String exchangePreferito,
        String rete,
        String address,
        long minuti, 
        BigDecimal Qta) {
symbol=symbol.toUpperCase();
   // if (Qta==null)Qta="1";
    
    final long MAX_DIFF_MS = minuti * 60 * 1000; // 5 minuti in millisecondi
    long tsMin = timestampRiferimento - MAX_DIFF_MS;
    long tsMax = timestampRiferimento + MAX_DIFF_MS;

    
    
    

    // Se è specificato un exchange, prova prima con quello
    if (exchangePreferito != null && !exchangePreferito.isEmpty()) {
        //try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(baseQuery + " /* preferito */")) {
        //A1 - Prezzo da personalizzati
        
        //metto like nella fonte perchè nei personalizzati vado a cercare il gruppo wallet non la fonte in se
        //Quando creo i personalizzati infatti inserisco anche il gruppo wakllet nella fonte per individuarli
        //Nel caso non specifichi un grupo metto ALL quindi ad esempio la fonte sarà "personalizzato (ALL)" piuttosto che 
        //binance (Wallet 01) piuttosto che personalizzato (Wallet 02) etc....
        //nel caso sia messo binance, starà ad indicare che quello è il prezzo di binance che si è scelto che per quel gruppo
        //deve essere il predefinito.
        String baseQuery = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
          AND (exchange ILIKE ? OR ? = '')
        ORDER BY ABS(timestamp - ?) ASC
        LIMIT 1
    """;
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(baseQuery)) {
            ps.setString(1, symbol == null ? "" : symbol);
            ps.setString(2, symbol == null ? "" : symbol);
            ps.setLong(3, tsMin);
            ps.setLong(4, tsMax);
            ps.setString(5, rete == null ? "" : rete);
            ps.setString(6, rete == null ? "" : rete);
            ps.setString(7, address == null ? "" : address);
            ps.setString(8, address == null ? "" : address);
            ps.setString(9, "%" +exchangePreferito+"%");
            ps.setString(10, exchangePreferito);
            ps.setLong(11, timestampRiferimento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prezzo = rs.getBigDecimal("prezzo");
                    String exch = rs.getString("exchange");
                    long ts = rs.getLong("timestamp");
                    BigDecimal prezzoQta = prezzo.multiply(Qta.abs());
                    return new InfoPrezzo(prezzo, exch, ts, prezzoQta,Qta,symbol);
                    
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        
        //A2 - Prezzo da database prezzi
        baseQuery = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
          AND (exchange = ? OR ? = '')
        ORDER BY ABS(timestamp - ?) ASC
        LIMIT 1
    """;
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(baseQuery)) {
            ps.setString(1, symbol == null ? "" : symbol);
            ps.setString(2, symbol == null ? "" : symbol);
            ps.setLong(3, tsMin);
            ps.setLong(4, tsMax);
            ps.setString(5, rete == null ? "" : rete);
            ps.setString(6, rete == null ? "" : rete);
            ps.setString(7, address == null ? "" : address);
            ps.setString(8, address == null ? "" : address);
            ps.setString(9, exchangePreferito);
            ps.setString(10, exchangePreferito);
            ps.setLong(11, timestampRiferimento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prezzo = rs.getBigDecimal("prezzo");
                    String exch = rs.getString("exchange");
                    long ts = rs.getLong("timestamp");
                    BigDecimal prezzoQta = prezzo.multiply(Qta.abs());
                    return new InfoPrezzo(prezzo, exch, ts, prezzoQta,Qta,symbol);
                    
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        
        
    }
    

    // Se non trovato o exchange non specificato → cerca in tutti gli exchange
    String queryAll = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
        ORDER BY ABS(timestamp - ?) ASC
        LIMIT 1
    """;
    
    try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(queryAll)) {
        ps.setString(1, symbol == null ? "" : symbol);
        ps.setString(2, symbol == null ? "" : symbol);
        ps.setLong(3, tsMin);
        ps.setLong(4, tsMax);
        ps.setString(5, rete == null ? "" : rete);
        ps.setString(6, rete == null ? "" : rete);
        ps.setString(7, address == null ? "" : address);
        ps.setString(8, address == null ? "" : address);
        ps.setLong(9, timestampRiferimento);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal prezzo = rs.getBigDecimal("prezzo");
                String exch = rs.getString("exchange");
                long ts = rs.getLong("timestamp");
                BigDecimal prezzoQta = prezzo.multiply(Qta.abs());
                return new InfoPrezzo(prezzo, exch, ts, prezzoQta,Qta,symbol);
                //return new InfoPrezzo(prezzo, exch, ts);
            }
        }
    }   catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }

 // Nessun prezzo trovato entro 5 minuti
    return null;
}


/**
 * Recupera dal database il prezzo più vicino a un determinato timestamp per un dato token.
 * <p>
 * La funzione ricerca il valore nel database <b>PrezziNew</b> considerando le seguenti regole:
 * <ul>
 *   <li>Se viene specificato un exchange preferito, la ricerca viene effettuata prima su di esso.</li>
 *   <li>Se non viene trovato alcun risultato sull’exchange preferito (o se non specificato),
 *       la ricerca prosegue su tutti gli altri exchange.</li>
 *   <li>Il timestamp trovato non può differire di oltre ±5 minuti (300 secondi) dal timestamp richiesto.</li>
 *   <li>È possibile filtrare ulteriormente la ricerca per rete e address (facoltativi).</li>
 * </ul>
 *
 * @param symbol              il simbolo del token (es. "BTC")
 * @param timestampRiferimento il timestamp di riferimento (in millisecondi)
 * @param exchangePreferito   l’exchange da cui preferibilmente recuperare il prezzo
 *                            (può essere {@code null} o vuoto per cercare su tutti)
 * @param rete                la rete blockchain associata al token (facoltativa, può essere vuota)
 * @param address             l’indirizzo del token (facoltativo, può essere vuoto)
 * @param minuti              Precisione minima in minuti
     * @param Qta
 * @return un oggetto {@link PrezzoInfo} contenente il prezzo, l’exchange e il timestamp effettivo del prezzo,
 *         oppure {@code null} se non è stato trovato alcun prezzo valido entro 5 minuti dal timestamp richiesto
 *
 * @see PrezzoInfo
 * @implNote Il campo {@code timestamp} del database e del parametro di input devono essere espressi
 *           nella stessa unità temporale (millisecondi). Se il database usa secondi UNIX,
 *           adattare la logica di confronto di conseguenza.
 */
public static InfoPrezzo DammiPrezzoDaDatabasePersonale(
        String symbol,
        long timestampRiferimento,
        String exchangePreferito,
        String rete,
        String address,
        long minuti, 
        BigDecimal Qta) {
symbol=symbol.toUpperCase();
   // if (Qta==null)Qta="1";
    
    final long MAX_DIFF_MS = minuti * 60 * 1000; // 5 minuti in millisecondi
    long tsMin = timestampRiferimento - MAX_DIFF_MS;
    long tsMax = timestampRiferimento + MAX_DIFF_MS;

   // System.out.println(symbol+"-"+timestampRiferimento+"-"+rete+"-"+address+"-"+Qta+"-"+exchangePreferito);
    
    

    // Se è specificato un exchange, prova prima con quello
    if (exchangePreferito != null && !exchangePreferito.isEmpty()) {
        //try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(baseQuery + " /* preferito */")) {
        //A1 - Prezzo da personalizzati
        
        //metto like nella fonte perchè nei personalizzati vado a cercare il gruppo wallet non la fonte in se
        //Quando creo i personalizzati infatti inserisco anche il gruppo wakllet nella fonte per individuarli
        //Nel caso non specifichi un grupo metto ALL quindi ad esempio la fonte sarà "personalizzato (ALL)" piuttosto che 
        //binance (Wallet 01) piuttosto che personalizzato (Wallet 02) etc....
        //nel caso sia messo binance, starà ad indicare che quello è il prezzo di binance che si è scelto che per quel gruppo
        //deve essere il predefinito.
        String baseQuery = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
          AND (exchange ILIKE ? OR ? = '')
        ORDER BY ABS(timestamp - ?) ASC
        LIMIT 1
    """;
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(baseQuery)) {
            ps.setString(1, symbol == null ? "" : symbol);
            ps.setString(2, symbol == null ? "" : symbol);
            ps.setLong(3, tsMin);
            ps.setLong(4, tsMax);
            ps.setString(5, rete == null ? "" : rete);
            ps.setString(6, rete == null ? "" : rete);
            ps.setString(7, address == null ? "" : address);
            ps.setString(8, address == null ? "" : address);
            ps.setString(9, "%" +exchangePreferito+"%");
            ps.setString(10, exchangePreferito);
            ps.setLong(11, timestampRiferimento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prezzo = rs.getBigDecimal("prezzo");
                    String exch = rs.getString("exchange");
                    long ts = rs.getLong("timestamp");
                    BigDecimal prezzoQta = prezzo.multiply(Qta.abs());
                    return new InfoPrezzo(prezzo, exch, ts, prezzoQta,Qta,symbol);
                    
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        

        
    }else{
        // Se non trovato o exchange non specificato → cerca in tutti gli exchange
    String queryAll = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
        ORDER BY ABS(timestamp - ?) ASC
        LIMIT 1
    """;
    
    try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(queryAll)) {
        ps.setString(1, symbol == null ? "" : symbol);
        ps.setString(2, symbol == null ? "" : symbol);
        ps.setLong(3, tsMin);
        ps.setLong(4, tsMax);
        ps.setString(5, rete == null ? "" : rete);
        ps.setString(6, rete == null ? "" : rete);
        ps.setString(7, address == null ? "" : address);
        ps.setString(8, address == null ? "" : address);
        ps.setLong(9, timestampRiferimento);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal prezzo = rs.getBigDecimal("prezzo");
                String exch = rs.getString("exchange");
                long ts = rs.getLong("timestamp");
                BigDecimal prezzoQta = prezzo.multiply(Qta.abs());
                return new InfoPrezzo(prezzo, exch, ts, prezzoQta,Qta,symbol);
                //return new InfoPrezzo(prezzo, exch, ts);
            }
        }
    }   catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
   
    }
 // Nessun prezzo trovato entro 5 minuti
    return null;
}


public static List<InfoPrezzo> DammiListaPrezziDaDatabase(
        String symbol,
        long timestampRiferimento,
        String rete,
        String address,
        long minuti,
        BigDecimal Qta) {
    symbol=symbol.toUpperCase();

    List<InfoPrezzo> listaPrezzi = new ArrayList<>();

    final long MAX_DIFF_MS = minuti * 60 * 1000;
    long tsMin = timestampRiferimento - MAX_DIFF_MS;
    long tsMax = timestampRiferimento + MAX_DIFF_MS;

    // Recupera tutti i prezzi candidati entro la finestra
    String query = """
        SELECT prezzo, exchange, timestamp
        FROM PrezziNew
        WHERE (symbol = ? OR ? = '')
          AND timestamp BETWEEN ? AND ?
          AND (rete = ? OR ? = '')
          AND (address = ? OR ? = '')
        ORDER BY exchange ASC, ABS(timestamp - ?) ASC
    """;

    // Mappa per tenere solo il record più vicino per exchange
    Map<String, InfoPrezzo> miglioriPerExchange = new HashMap<>();

    //Interrogo i prezzi salvati nei personalizzati
    try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(query)) {
        ps.setString(1, symbol == null ? "" : symbol);
        ps.setString(2, symbol == null ? "" : symbol);
        ps.setLong(3, tsMin);
        ps.setLong(4, tsMax);
        ps.setString(5, rete == null ? "" : rete);
        ps.setString(6, rete == null ? "" : rete);
        ps.setString(7, address == null ? "" : address);
        ps.setString(8, address == null ? "" : address);
        ps.setLong(9, timestampRiferimento);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal prezzo = rs.getBigDecimal("prezzo");
                String exchange = rs.getString("exchange");
                long ts = rs.getLong("timestamp");

                BigDecimal prezzoQta = prezzo.multiply(Qta == null ? BigDecimal.ONE : Qta.abs());
                InfoPrezzo info = new InfoPrezzo(prezzo, exchange, ts, prezzoQta, Qta, symbol);

                // Mantieni solo quello più vicino per ogni exchange
                InfoPrezzo esistente = miglioriPerExchange.get(exchange);
                if (esistente == null ||
                    Math.abs(ts - timestampRiferimento) < Math.abs(esistente.timestamp - timestampRiferimento)) {
                    miglioriPerExchange.put(exchange, info);
                }
            }
        }
    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
    
    //Interrogo i prezzi nel database princiaple
    try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(query)) {
        ps.setString(1, symbol == null ? "" : symbol);
        ps.setString(2, symbol == null ? "" : symbol);
        ps.setLong(3, tsMin);
        ps.setLong(4, tsMax);
        ps.setString(5, rete == null ? "" : rete);
        ps.setString(6, rete == null ? "" : rete);
        ps.setString(7, address == null ? "" : address);
        ps.setString(8, address == null ? "" : address);
        ps.setLong(9, timestampRiferimento);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal prezzo = rs.getBigDecimal("prezzo");
                String exchange = rs.getString("exchange");
                long ts = rs.getLong("timestamp");

                BigDecimal prezzoQta = prezzo.multiply(Qta == null ? BigDecimal.ONE : Qta.abs());
                InfoPrezzo info = new InfoPrezzo(prezzo, exchange, ts, prezzoQta, Qta, symbol);

                // Mantieni solo quello più vicino per ogni exchange
                InfoPrezzo esistente = miglioriPerExchange.get(exchange);
                if (esistente == null ||
                    Math.abs(ts - timestampRiferimento) < Math.abs(esistente.timestamp - timestampRiferimento)) {
                    miglioriPerExchange.put(exchange, info);
                }
            }
        }
    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }

    listaPrezzi.addAll(miglioriPerExchange.values());
    return listaPrezzi;
}

 
public static long GUI_ModificaPrezzoConAttesa(Moneta M,String[] Ritorno,Component c,long dataL,String VecchioPrezzo){
                //In ritorno a questa funzione ricevo il timestamp del prezzo originale del token
                //Questo mi servirà per cancellare l'eventuale prezzo personalizzato.
    
                long timestampDaCancellare[]=new long[1];
                timestampDaCancellare[0]=0;
                Download progress = new Download();
                progress.MostraProgressAttesa("Scaricamento Prezzi", "Attendi scaricamento dei prezzi...");
                progress.setLocationRelativeTo(c);
                //Prezzi.InfoPrezzo IPt=IPr;//creo una nuova variabile da passare al thread perchè questa deve essere final
                Thread thread;
                thread = new Thread() {
                    public void run() {
                        Prezzi.InfoPrezzo IPr;
                IPr=Prezzi.DammiPrezzoInfoTransazione(M, null, dataL,M.Rete,"" );
                
                if(IPr!=null)
                {
                   if (IPr.prezzoQta==null) IPr.prezzoQta=new BigDecimal(M.Qta).multiply(IPr.prezzoUnitario).setScale(2,RoundingMode.HALF_UP).abs();
                   else IPr.prezzoQta=IPr.prezzoQta.setScale(2,RoundingMode.HALF_UP).abs();
                   if (IPr.prezzoQta.compareTo(new BigDecimal(VecchioPrezzo))!=0){
                       //Se arrivo qua significa che il prezzo calcolato è diverso dal prezzo mostrato
                       //Questo succede quando non vengono forniti tutti i dati alla funzione
                       //tipicamente non viene passato address o rete come fa la funzione dell'RW ad esempio
                       //In questo caso mostro solo i dati essenziali
                       IPr = new Prezzi.InfoPrezzo(null, "", 0, new BigDecimal(VecchioPrezzo), null, M.Moneta);
                   }
                   //IPr.prezzoQta=new BigDecimal(VecchioPrezzo);
                    timestampDaCancellare[0]=IPr.timestamp;
                  //  System.out.println(IPr.exchange);
                }
                else IPr = new Prezzi.InfoPrezzo(null, "", 0, new BigDecimal(VecchioPrezzo), null, M.Moneta);
                        
                        
                        GUI_ModificaPrezzo t = new GUI_ModificaPrezzo(M, null,IPr,dataL,M.Rete,Ritorno,progress);                   
                        t.setLocationRelativeTo(c);
                        t.setVisible(true);
                        //progress.ChiudiFinestra();
                    }
                };
                thread.start();
                progress.setVisible(true);
                //System.out.println("xxxxxxx"+timestampDaCancellare[0]);
                return timestampDaCancellare[0];
}

public static long GUI_ModificaPrezzoConAttesaTest(Moneta M,String[] Ritorno,Component c,long dataL,String VecchioPrezzo){
                //In ritorno a questa funzione ricevo il timestamp del prezzo originale del token
                //Questo mi servirà per cancellare l'eventuale prezzo personalizzato.
    
                Prezzi.InfoPrezzo IPr;
                IPr = new Prezzi.InfoPrezzo(null, "", 0, new BigDecimal(VecchioPrezzo), null, M.Moneta);
                Download progress = new Download();
                //progress.MostraProgressAttesa("Scaricamento Prezzi", "Attendi scaricamento dei prezzi...");
                progress.setLocationRelativeTo(c);
                Prezzi.InfoPrezzo IPt=IPr;//creo una nuova variabile da passare al thread perchè questa deve essere final
                Thread thread;
                thread = new Thread() {
                    public void run() {
                        GUI_ModificaPrezzo t = new GUI_ModificaPrezzo(M, null,IPt,dataL,M.Rete,Ritorno,progress);                   
                        t.setLocationRelativeTo(c);
                        t.setVisible(true);
                       // progress.ChiudiFinestra();
                    }
                };
                thread.start();
                progress.setVisible(true);
                return 0;
}


public static void RecuperaGiacenzeDaCCXT(String Exchange,String APIKey,String APISecret,String Giorno) {

        
         try {
            
             TimeUnit.SECONDS.sleep(1);
             
             Path nodePath = CcxtInterop.getNodeExePath();
             Path scriptPath = Paths.get(Statiche.getPathRisorse()
                     + "Scripts/"
                             + "Binance_SaldiGiornalieri"
                             + ".js");
             
             CcxtInterop.ensureNodeInstalled();
             CcxtInterop.installCcxt();
             if (!Files.exists(nodePath)) {
                 System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
                 return;
             }
             if (!Files.exists(scriptPath)) {
                 System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
                 return;
             }
             
             System.out.println("Scarico Giacenze da "+Exchange+" in data "+Giorno);
             // Parametri CLI da passare allo script
             List<String> command = new ArrayList<>();
             command.add(nodePath.toString());
             command.add(scriptPath.toAbsolutePath().toString());
             command.add(Exchange);
             command.add(APIKey);
             command.add(APISecret);
             command.add(Giorno);
             
             
             ProcessBuilder pb = new ProcessBuilder(command);
             
             pb.directory(scriptPath.getParent().toFile());
             Path nodeModulesPath = CcxtInterop.NODE_DIR.resolve("node_modules").toAbsolutePath();
             Map<String, String> env = pb.environment();
             // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
             String existingNodePath = env.get("NODE_PATH");
             String newNodePath = nodeModulesPath.toString();
             if (existingNodePath != null && !existingNodePath.isEmpty()) {
                 newNodePath += File.pathSeparator + existingNodePath;
             }
             env.put("NODE_PATH", newNodePath);
             pb.redirectErrorStream(true); // unisce stdout + stderr
             
             try {
                 pb.redirectErrorStream(false);
                 Process process = pb.start();
                 
                 // Legge l'output dello script (JSON stampato da console.log)
                 StringBuilder output = new StringBuilder();
                 try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                         BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                     String line;
                     while ((line = reader.readLine()) != null) {
                         output.append(line).append("\n");
                     }
                     while ((line = errReader.readLine()) != null) System.out.println("[NODE] " + line);
                 }
                 
                 // Attende che il processo finisca
                 int exitCode = process.waitFor();
                 //System.out.println("Exit code: " + exitCode);
                 if (exitCode != 0) {
                     System.err.println("Script Node fallito. Exit code: " + exitCode);
                     System.err.println(output);
                     return;
                 }
                 
                 // Parse JSON con Gson in modo sicuro (manteniamo precisione per i long)
                 // Gson gson = new Gson();
                 System.out.println("---"+output.toString()+"---");
                 JsonElement rootEl = JsonParser.parseString(output.toString());
                 if (!rootEl.isJsonArray()) {
                     System.err.println("Output non è un array JSON valido.");
                     return;
                 }
               /*  JsonArray rootArr = rootEl.getAsJsonArray();
                 
                 String mergeSql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo,rete,address) KEY (timestamp, exchange, symbol,rete,address) VALUES (?, ?, ?, ?, ?, ?)";
                 try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(mergeSql)) {
                     
                     for (JsonElement el : rootArr) {
                         if (!el.isJsonObject()) continue;
                         JsonObject obj = el.getAsJsonObject();
                         
                         if (!obj.has("timestamp")) continue;
                         long ts = obj.get("timestamp").getAsLong();
                         
                         JsonObject pricesObj = obj.has("prices") && obj.get("prices").isJsonObject()
                                 ? obj.getAsJsonObject("prices")
                                 : null;
                         if (pricesObj == null) continue;
                         
                         for (Map.Entry<String, JsonElement> entry : pricesObj.entrySet()) {
                             String exchang = entry.getKey();
                             JsonElement valEl = entry.getValue();
                             if (valEl == null || valEl.isJsonNull()) continue;
                             
                             double value;
                             try {
                                 value = valEl.getAsDouble();
                             } catch (Exception ex) {
                                 // valore non numerico: skip
                                 continue;
                             }
                             
                             ps.setLong(1, ts);
                             ps.setString(2, exchang);
                             ps.setString(3, Symbol);
                             ps.setDouble(4, value);
                             ps.setString(5, "");//Rete
                             ps.setString(6, "");//Address
                             ps.addBatch();
                         }
                     }
                     ps.executeBatch();
                 } catch (SQLException ex) {
                     //Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
                     LoggerGC.ScriviErrore(ex);
                 }*/
                 
                 //Se arrivo qua lo script è andato a buon fine quindi aggiungo il range richiesto alla lista di quelli già utilizzati pr la moneta
                 //in modo da non richiederlo più volte nell'arco della sessione nel caso in cui non trovi il prezzo
                // managerRichieste.addRange(Symbol, Since, Until);
                 
                 // Query di test: mostra i primi 50 record
                 /*    System.out.println("=== Sample from H2 (timestamp | exchange | symbol | prezzo) ===");
                 try (Statement st = DatabaseH2.connectionPrezzi.createStatement();
                 ResultSet rs = st.executeQuery(
                 "SELECT timestamp, exchange, symbol, prezzo FROM PrezziNew ORDER BY timestamp LIMIT 50")) {
                 
                 while (rs.next()) {
                 long ts = rs.getLong("timestamp");
                 String ex = rs.getString("exchange");
                 String sym = rs.getString("symbol");
                 double v = rs.getDouble("prezzo");
                 System.out.printf("%d | %s | %s = %.6f%n", ts, ex, sym, v);
                 }
                 }*/
                 //}
                 
             } catch (IOException | InterruptedException e) {
                 LoggerGC.ScriviErrore(e);
             }
             
             
         } catch (IOException | InterruptedException ex) {
            LoggerGC.ScriviErrore(ex);
        }


}


public static void RecuperaPrezziDaCCXT(String Symbol,long timestamp) {

        
         try {
             Symbol=Symbol.toUpperCase();
             
             //long timestampAttuale = System.currentTimeMillis();
             //Voglio reperire sempre almeno 1h di dati per cui prendo 1h prima e 1h dopo
             long Since=timestamp-7200000;//2h prima
             long Until=timestamp+21600000;//6h dopo
             long Adesso = System.currentTimeMillis();
             if (Until>Adesso)Until=Adesso;
             if (Since>Adesso)return;
             
             //se ho già fatto questa richiesta in questa sessione termino immediatamente il ciclo
             //per questa richiesta visto che la precisione è di 60 minuti mi basta che vi siano i 60 minuti prima e quelli dopo quindi
             long SinceVerifica=timestamp-300000;
             long UntilVerifica=timestamp+300000;
             if (UntilVerifica>Adesso)UntilVerifica=Adesso;
             if (SinceVerifica>Adesso)SinceVerifica=Adesso-300000;
             if(managerRichieste.isAlreadyRequested(Symbol, SinceVerifica, UntilVerifica))return;
             TimeUnit.SECONDS.sleep(1);
             //Lista degli exchange a cui richiedere il prezzo della cripto
             String exchanges="binance,cryptocom,bybit,okx,coinbase,bitstamp,kucoin";
             
             Path nodePath = CcxtInterop.getNodeExePath();
             Path scriptPath = Paths.get(Statiche.getPathRisorse()
                     + "Scripts/"
                             + "Historical_Multi_Eur"
                             + ".js");
             
             CcxtInterop.ensureNodeInstalled();
             CcxtInterop.installCcxt();
             if (!Files.exists(nodePath)) {
                 System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
                 return;
             }
             if (!Files.exists(scriptPath)) {
                 System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
                 return;
             }
             
             System.out.println("Scarico Prezzi di "+Symbol+" in data "+FunzioniDate.ConvertiDatadaLongAlSecondo(timestamp));
             // Parametri CLI da passare allo script
             List<String> command = new ArrayList<>();
             command.add(nodePath.toString());
             command.add(scriptPath.toAbsolutePath().toString());
             command.add("--since");
             command.add(String.valueOf(Since));
             command.add("--until");
             command.add(String.valueOf(Until));
             command.add("--exchanges");
             command.add(exchanges);
             command.add("--symbol");
             command.add(Symbol);
             command.add("--timeframe");
             command.add("1m");
             
             
             ProcessBuilder pb = new ProcessBuilder(command);
             
             pb.directory(scriptPath.getParent().toFile());
             Path nodeModulesPath = CcxtInterop.NODE_DIR.resolve("node_modules").toAbsolutePath();
             Map<String, String> env = pb.environment();
             // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
             String existingNodePath = env.get("NODE_PATH");
             String newNodePath = nodeModulesPath.toString();
             if (existingNodePath != null && !existingNodePath.isEmpty()) {
                 newNodePath += File.pathSeparator + existingNodePath;
             }
             env.put("NODE_PATH", newNodePath);
             pb.redirectErrorStream(true); // unisce stdout + stderr
             
             try {
                 pb.redirectErrorStream(false);
                 Process process = pb.start();
                 
                 // Leggi l'output dello script (JSON stampato da console.log)
                 StringBuilder output = new StringBuilder();
                 try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                         BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                     String line;
                     while ((line = reader.readLine()) != null) {
                         output.append(line).append("\n");
                     }
                     while ((line = errReader.readLine()) != null) System.out.println("[NODE] " + line);
                 }
                 
                 // Attendi che il processo finisca
                 int exitCode = process.waitFor();
                 //System.out.println("Exit code: " + exitCode);
                 if (exitCode != 0) {
                     System.err.println("Script Node fallito. Exit code: " + exitCode);
                     System.err.println(output);
                     return;
                 }
                 
                 // Parse JSON con Gson in modo sicuro (manteniamo precisione per i long)
                 // Gson gson = new Gson();
                 //System.out.println("---"+output.toString()+"---");
                 JsonElement rootEl = JsonParser.parseString(output.toString());
                 if (!rootEl.isJsonArray()) {
                     System.err.println("Output non è un array JSON valido.");
                     return;
                 }
                 JsonArray rootArr = rootEl.getAsJsonArray();
                 
                 String mergeSql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo,rete,address) KEY (timestamp, exchange, symbol,rete,address) VALUES (?, ?, ?, ?, ?, ?)";
                 try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(mergeSql)) {
                     
                     for (JsonElement el : rootArr) {
                         if (!el.isJsonObject()) continue;
                         JsonObject obj = el.getAsJsonObject();
                         
                         if (!obj.has("timestamp")) continue;
                         long ts = obj.get("timestamp").getAsLong();
                         
                         JsonObject pricesObj = obj.has("prices") && obj.get("prices").isJsonObject()
                                 ? obj.getAsJsonObject("prices")
                                 : null;
                         if (pricesObj == null) continue;
                         
                         for (Map.Entry<String, JsonElement> entry : pricesObj.entrySet()) {
                             String exchange = entry.getKey();
                             JsonElement valEl = entry.getValue();
                             if (valEl == null || valEl.isJsonNull()) continue;
                             
                             double value;
                             try {
                                 value = valEl.getAsDouble();
                             } catch (Exception ex) {
                                 // valore non numerico: skip
                                 continue;
                             }
                             
                             ps.setLong(1, ts);
                             ps.setString(2, exchange);
                             ps.setString(3, Symbol);
                             ps.setDouble(4, value);
                             ps.setString(5, "");//Rete
                             ps.setString(6, "");//Address
                             ps.addBatch();
                         }
                     }
                     ps.executeBatch();
                 } catch (SQLException ex) {
                     //Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null, ex);
                     LoggerGC.ScriviErrore(ex);
                 }
                 
                 //Se arrivo qua lo script è andato a buon fine quindi aggiungo il range richiesto alla lista di quelli già utilizzati pr la moneta
                 //in modo da non richiederlo più volte nell'arco della sessione nel caso in cui non trovi il prezzo
                 managerRichieste.addRange(Symbol, Since, Until);
                 
                 // Query di test: mostra i primi 50 record
                 /*    System.out.println("=== Sample from H2 (timestamp | exchange | symbol | prezzo) ===");
                 try (Statement st = DatabaseH2.connectionPrezzi.createStatement();
                 ResultSet rs = st.executeQuery(
                 "SELECT timestamp, exchange, symbol, prezzo FROM PrezziNew ORDER BY timestamp LIMIT 50")) {
                 
                 while (rs.next()) {
                 long ts = rs.getLong("timestamp");
                 String ex = rs.getString("exchange");
                 String sym = rs.getString("symbol");
                 double v = rs.getDouble("prezzo");
                 System.out.printf("%d | %s | %s = %.6f%n", ts, ex, sym, v);
                 }
                 }*/
                 //}
                 
             } catch (IOException | InterruptedException e) {
                 LoggerGC.ScriviErrore(e);
             }
             
             
         } catch (IOException | InterruptedException ex) {
            LoggerGC.ScriviErrore(ex);
        }


}


     
public static class InfoPrezzo {
    public BigDecimal prezzoUnitario;
    public BigDecimal prezzoQta;
    public String Fonte;
    public long timestamp;
    public BigDecimal Qta;
    public String Moneta;
    public Moneta OggettoMoneta;//Usato solo per salvasi i dati  dei valori delle monete di fine e inizio anno in RW (per la modifica prezzi)


   /* public InfoPrezzo(String Info40,BigDecimal Qta) {
        if (!Info40.isBlank()){
            String iPr[]=Info40.split("\\|",-1);
            if (iPr.length==4){
                this.prezzoUnitario = new BigDecimal(iPr[3]);
                this.exchange = iPr[4];
                this.timestamp = Long.parseLong(iPr[1]);
                this.prezzoQta = this.prezzoUnitario.multiply(Qta);
                this.Qta = Qta;
                this.Moneta = iPr[0];
            }
        }
        
    }*/
    
    public InfoPrezzo(String Info40) {
        if (!Info40.isBlank()){
            String iPr[]=Info40.split("\\|",-1);
            if (iPr.length==4){
                if (Principale.Funzioni_isNumeric(iPr[2], false))
                    this.prezzoUnitario = new BigDecimal(iPr[2]);
                this.Fonte = iPr[3];
                if (Principale.Funzioni_isNumeric(iPr[1], false))
                    this.timestamp = Long.parseLong(iPr[1]);
                this.Moneta = iPr[0];
            }
        }
        
    }
    
        /**
         * Converte la data da formato leggibile a timestamp ma restituisce una stringa e non un long
         * Restituisce null in caso di errori
         * @param Time
         * @return
         */
        public static String ConvertiDataATimestamp(String Time){       
        String ritorno=null;
        if (!Time.isBlank()){
            Time = Time.toUpperCase().replace("X", "0"); 
            ritorno=String.valueOf(FunzioniDate.ConvertiDatainLongSecondo(Time));
            }
        if(ritorno!=null&&!ritorno.equals("0"))return ritorno;
        return null;
    }
    
    public InfoPrezzo(BigDecimal prezzo, String exchange, long timestamp, BigDecimal prezzoQta, BigDecimal Qta,String Moneta) {
        this.prezzoUnitario = prezzo;
        this.Fonte = exchange;
        this.timestamp = timestamp;
        this.prezzoQta = prezzoQta;
        this.Qta = Qta;
        this.Moneta = Moneta;
    }
    public InfoPrezzo() {

    }
    
    public String RitornaStringData(){
        if (this.Fonte.toLowerCase().contains("db interno"))
                    {
                        return FunzioniDate.ConvertiDatadaLongallOra(this.timestamp)+":XX:XX";
                    }
        else return FunzioniDate.ConvertiDatadaLongAlSecondo(this.timestamp);
    }
    
    public String RitornaStringDiffData(long datalong) {

        if (this.Fonte.toLowerCase().contains("db interno")) {
            return "1 ora";
        } else {
            long DiffOrario = Math.abs(this.timestamp - datalong) / 1000;
            String unitaTempo;
            if (DiffOrario >= 60) {
                DiffOrario = DiffOrario / 60;  // converto in minuti
                unitaTempo = " min";
            } else {
                unitaTempo = " sec";
            }
            return String.valueOf(DiffOrario) + unitaTempo;
        }
    }
    
    public String Ritorna40(){
        return this.Moneta+"|"+this.timestamp+"|"+this.prezzoUnitario+"|"+this.Fonte;
    }
    }        
    


public static class RangeRequestManager {

    /** Classe che rappresenta un range temporale [start, end] */
    public static class TimeRange {
        public long start;
        public long end;

        public TimeRange(long start, long end) {
            if (end < start)
                throw new IllegalArgumentException("end < start");
            this.start = start;
            this.end = end;
        }

        /** Verifica se due range si sovrappongono o si toccano */
        public boolean overlaps(TimeRange other) {
            return this.start <= other.end && other.start <= this.end;
        }

        /** Fonde due range (presuppone che si sovrappongano) */
        public void merge(TimeRange other) {
            this.start = Math.min(this.start, other.start);
            this.end = Math.max(this.end, other.end);
        }

        @Override
        public String toString() {
            return "[" + start + "," + end + "]";
        }
    }

    // Mappa coin → lista di range già richiesti
    private final Map<String, List<TimeRange>> richiestePerMoneta = new ConcurrentHashMap<>();

    /**
     * Verifica se un range è già stato richiesto per una determinata moneta.
     * Restituisce true se si sovrappone con un range esistente.
     */
    public synchronized boolean isAlreadyRequested(String coin, long start, long end) {
        List<TimeRange> ranges = richiestePerMoneta.get(coin);
        if (ranges == null) return false;

        TimeRange newRange = new TimeRange(start, end);
        for (TimeRange r : ranges) {
            if (r.overlaps(newRange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aggiunge un nuovo range per una moneta. Se si sovrappone ad altri, li unisce.
     */
    public synchronized void addRange(String coin, long start, long end) {
        TimeRange newRange = new TimeRange(start, end);
        List<TimeRange> ranges = richiestePerMoneta.computeIfAbsent(coin, k -> new ArrayList<>());

        List<TimeRange> merged = new ArrayList<>();
        boolean added = false;

        for (TimeRange existing : ranges) {
            if (existing.overlaps(newRange)) {
                newRange.merge(existing);
                added = true;
            } else {
                merged.add(existing);
            }
        }

        merged.add(newRange);
        merged.sort(Comparator.comparingLong(r -> r.start));

        richiestePerMoneta.put(coin, merged);
    }

    /** Restituisce tutti i range registrati per una moneta */
    public synchronized List<TimeRange> getRanges(String coin) {
        return richiestePerMoneta.getOrDefault(coin, Collections.emptyList());
    }

    /** Stampa di debug */
    public synchronized void printState() {
        for (var entry : richiestePerMoneta.entrySet()) {
            System.out.println(entry.getKey() + " → " + entry.getValue());
        }
    }

    // Esempio d’uso
 
    /*    RangeRequestManager manager = new RangeRequestManager();

        manager.addRange("BTC", 1000, 2000);
        manager.addRange("BTC", 1500, 2500); // unisce con quello precedente
        manager.addRange("ETH", 500, 1000);

        System.out.println(manager.isAlreadyRequested("BTC", 1800, 1900)); // true
        System.out.println(manager.isAlreadyRequested("BTC", 2600, 2700)); // false

        manager.addRange("BTC", 2600, 2700); // nuovo range
        manager.printState();
*/
}


}


