/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;



import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;

/**
 *
 * @author luca.passelli
 */
public class Calcoli {
    static Map<String, String> MappaWallets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneUSDEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneUSDTEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneAddressEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneAddressEURtemp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);//mappa di appoggio per scrivere il file correttamente
    static Map<String, String> MappaConversioneXXXEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneXXXEUR_temp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaCoppieBinance = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaSimboliCoingecko = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneAddressCoin = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  //  static Map<String, String> MappaConversioneSimboloReteCoingecko = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneSwapTransIDCoins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    //di seguito le coppie prioritarie ovvero quelle che hanno precedneza all'atto della ricerca dei prezzi rispetto alle altre
    static String CoppiePrioritarie[]=new String []{"USDCUSDT","BUSDUSDT","DAIUSDT","TUSDUSDT","BTCUSDT",
        "ETHUSDT","BNBUSDT","LTCUSDT","ADAUSDT","XRPUSDT","NEOUSDT",
        "IOTAUSDT","EOSUSDT","XLMUSDT","SOLUSDT","PAXUSDT","TRXUSDT","ATOMUSDT","MATICUSDT"};

    
  //DA FARE : Recupero prezzi orari in base all'ora più vicina  

  
    
    
    
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
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }
             
         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }

    }   
    
    
    
    
    
    public static void GeneraMappaCambioUSDEUR(){
         try {
             File file=new File ("cambioUSDEUR.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("cambioUSDEUR.db");
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
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }
             
         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }

    }  
    
        public static void GeneraMappaCambioUSDTEUR(){
         try {
             File file=new File ("cambioUSDTEUR.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("cambioUSDTEUR.db");
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                     if (rigaSplittata.length==2)
                     {
                         MappaConversioneUSDTEUR.put(rigaSplittata[0], rigaSplittata[1]);
                     }
                 }
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }

         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
        
        
        
        
        
        
             public static void GeneraMappaCambioAddressEUR(){
         try {
             File file=new File ("cambioAddressEUR.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("cambioAddressEUR.db");
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                     if (rigaSplittata.length==2)
                     {
                         MappaConversioneAddressEUR.put(rigaSplittata[0], rigaSplittata[1]);
                         MappaConversioneAddressEURtemp.put(rigaSplittata[0], rigaSplittata[1]);
                     }
                 }
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }

         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
        
        
        
        
        
        
        
        
        
        
        public static void GeneraMappaCambioXXXEUR(){
         try {
             File file=new File ("cambioXXXEUR.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("cambioXXXEUR.db");
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                     if (rigaSplittata.length==2)
                     {
                         MappaConversioneXXXEUR.put(rigaSplittata[0], rigaSplittata[1]);
                         MappaConversioneXXXEUR_temp.put(rigaSplittata[0], rigaSplittata[1]);
                     }
                 }
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }

         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
        
    public static void GeneraMappaConversioneAddressCoin(){
         try {
             File file=new File ("conversioneAddressCoin.db");
             if (!file.exists()) file.createNewFile();
             String riga;
             try (FileReader fire = new FileReader("conversioneAddressCoin.db");
                     BufferedReader bure = new BufferedReader(fire);)
             {
                 while((riga=bure.readLine())!=null)
                 {
                     String rigaSplittata[]=riga.split(",");
                    // System.out.println(riga);
                     if (rigaSplittata.length==2&&CDC_Grafica.Mappa_AddressRete_Nome.get(rigaSplittata[0])==null)
                         //se lunghezza=2 significa che non ho l'id di coingecko della moneta
                         //inserisco il dato anche solo se lo stesso non è presente nella tabella delle coin listate su Binance
                         //in quel caso non mi interessa inserirlo qua perchè il prezzo lo prenderò direttamente da binance
                         //e quelle coin sono sicuramente legali e non serve contraddesgnare con gli sterischi
                     {
                        // System.out.println(riga);
                         MappaConversioneAddressCoin.put(rigaSplittata[0], rigaSplittata[1]);
                     }
                     else if (rigaSplittata.length==3&&CDC_Grafica.Mappa_AddressRete_Nome.get(rigaSplittata[0])==null)
                         //se lunghezza=3 significa che ho anche l'id di coingecko della moneta
                         //utile nel caso mi servissero i prezzi di una shitcoin listata da coingecko
                     {
                         MappaConversioneAddressCoin.put(rigaSplittata[0], rigaSplittata[1]+","+rigaSplittata[2]);
                     }
                 }
                 bure.close();
                 fire.close();        
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             } catch (IOException ex) {
                 Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
             }

         } catch (IOException ex) {        
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
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


     
     
     public static void RecuperaDettagliTransazioneBSC(String transactionHash){
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
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
            }
       // return null;
        System.out.println(transactionHash);
        System.out.println("qta="+qtaU+" - coin="+coinU);
        System.out.println("qta="+qtaE+" - coin="+coinE); 
        System.out.println("");
      
      }
     
      public static String RitornaNomeTokendadaBSCSCAN(String Address)
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
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
      }
     
        
    public static String ConvertiUSDEUR(String Valore, String Data) {
        if (MappaConversioneUSDEUR.isEmpty())
            {
                GeneraMappaCambioUSDEUR();
            }
        
        
        String risultato = null;
        //come prima cosa devo controllare che la data analizzata sia nel range delle date di cui ho il cambio usd/eur
        Object dateDisponibili[] = MappaConversioneUSDEUR.keySet().toArray();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String PrimaData;//è la prima data disponibile nel file delle conversioni
        String UltimaData;//e' l'ultima data disponibile nel file delle conversioni
        String DatadiOggi = f.format(System.currentTimeMillis());                   
                    
                    
        if (dateDisponibili.length > 0) {// se il file ha dei dati recupero prima e ultima data      
            PrimaData = (String) dateDisponibili[0];
            UltimaData = (String) dateDisponibili[dateDisponibili.length - 1];
            long DataOggiLong=ConvertiDatainLong(DatadiOggi);
            long DataUltimaLong=ConvertiDatainLong(UltimaData);
            long diffDate=DataOggiLong-DataUltimaLong;//86400000 di differenza significa 1 giorno
            //a questo punto siccome non posso ottenere i dati della giornata odierna
            //se sto cercando di ottenere quelli e l'ultimo dato che ho è inferiore a 10gg prendo quello come dato valido per la giornata
            //metto 10gg invece che 1 perchè potrebbero esserci delle feste e banchitalia non restituisce valori in quel caso
            if(ConvertiDatainLong(Data)==ConvertiDatainLong(DatadiOggi)&&diffDate<864000000){
                risultato=MappaConversioneUSDEUR.get(UltimaData);
            }
            //se la data di cui cerco il valore è compreso tra i range cerco il tasso di cambio corretto
            else if (ConvertiDatainLong(Data) >= ConvertiDatainLong(PrimaData) && ConvertiDatainLong(Data) <= ConvertiDatainLong(UltimaData)) {
                for (int i = 0; i < 10; i++) {
                    risultato = MappaConversioneUSDEUR.get(Data);
                    if (risultato == null) {
                        Data = GiornoMenoUno(Data);//questo appunto serve per andare a prendere i sabati e le domeniche dove non ho dati
                    } else {
                        break;
                    }
                }
            }
            else if (ConvertiDatainLong(Data) >= ConvertiDatainLong("2017-01-01") && ConvertiDatainLong(Data) <= ConvertiDatainLong(DatadiOggi)) {
                if (ConvertiDatainLong(Data) < ConvertiDatainLong(PrimaData)) {
                    //in questo caso richiedo i 90 gg precedenti la data richiesta
                    //anche perchè in questo modo comincio a compilare la tabella dei cambi
                    String DataMeno10 = ConvertiDatadaLong(ConvertiDatainLong(Data) - Long.parseLong("7776000000"));
                    if(RecuperaTassidiCambio(DataMeno10, PrimaData)!=null)risultato = ConvertiUSDEUR("1", Data);
                } else if (ConvertiDatainLong(Data) > ConvertiDatainLong(UltimaData)) {
                    //in questo caso richiedo i 90 gg successivi la data richiesta
                    //anche perchè in questo modo comincio a compilare la tabella dei cambi
                    String DataPiu10 = ConvertiDatadaLong(ConvertiDatainLong(Data) + Long.parseLong("7776000000"));
                    if(RecuperaTassidiCambio(UltimaData, DataPiu10)!=null)risultato = ConvertiUSDEUR("1", Data);
                }
                //  risultato = "Fuori range di date";
            } else {
                risultato = null;//avviene solo quando la data non è compresa tra oggi e il primo gennaio 2018
            }
        } else {
            if (dateDisponibili.length == 0) {
                PrimaData = "2017-01-01";
                UltimaData = DatadiOggi;
                if(RecuperaTassidiCambio(PrimaData, UltimaData)!=null)risultato = ConvertiUSDEUR("1", Data);
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
    
    
    public static String ConvertiUSDTEUR(String Qta, long Datalong) {
        //come prima cosa verifizo se ho caricato il file di conversione e in caso lo faccio
        if (MappaConversioneUSDTEUR.isEmpty())
            {
                GeneraMappaCambioUSDTEUR();
            }
        
        String risultato;// = null;
        //come prima cosa devo decidere il formato data
       // long adesso=System.currentTimeMillis();
        //long inizio2019=ConvertiDatainLong("2019-01-01");
        long adesso=System.currentTimeMillis();
        if (Datalong>adesso) return null;//se la data è maggiore di quella attuale non recupero nessun prezzo
        if (Datalong<1483225200)return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        String DataOra=ConvertiDatadaLongallOra(Datalong);
        String DataGiorno=ConvertiDatadaLong(Datalong);
        //String DataInizio=ConvertiDatadaLong(Datalong-Long.parseLong("3888000000"));//datainizio=la data-45gg
        //String DataFine=ConvertiDatadaLong(Datalong-Long.parseLong("3888000000"));//datafine=la data+45gg
        risultato = MappaConversioneUSDTEUR.get(DataOra);
       /* SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH");
        String DatadiOggi = f.format(System.currentTimeMillis());*/
        if (risultato == null) {

                //solo in questo caso vado a prendere il valore del giorno e non quello orario
                risultato = MappaConversioneUSDTEUR.get(DataGiorno);
                 if (risultato==null)  //se non trovo nenache il valore del giorno allora richiamo l'api coingecko per l'aggiornamento dei prezzi
                {
                     RecuperaTassidiCambioUSDT(DataGiorno,DataGiorno);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
                     risultato = MappaConversioneUSDTEUR.get(DataOra);
                     if (risultato == null) {
                         risultato = MappaConversioneUSDTEUR.get(DataGiorno);
                     }
                 }//non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

                    } //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

        if (risultato != null) {
            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
        }
        return risultato;
    }
    
    
    
    
    
    public static String ConvertiAddressEUR(String Qta, long Datalong, String Address, String Rete, String Simbolo) {
        //come prima cosa verifizo se ho caricato il file di conversione e in caso lo faccio
        if (MappaConversioneAddressEUR.isEmpty()) {
            GeneraMappaCambioAddressEUR();
           // MappaConversioneSimboloReteCoingecko.put("BSC", "binance-smart-chain");
        }
        if (MappaConversioneAddressCoin.isEmpty()) {
            GeneraMappaConversioneAddressCoin();
        }
       /* if (MappaSimboliCoingecko.isEmpty()) {
            RecuperaCoinsCoingecko();
        }
        if (Simbolo != null && MappaSimboliCoingecko.get(Simbolo.toUpperCase().trim()) == null) {
            //Se ho un simbolo e questo non è nella lista allora termino subito il ciclo che tanto mi restituirebbe null lo stesso
            return null;
        }*/
        if (MappaConversioneAddressCoin.get(Address + "_" + Rete) != null && MappaConversioneAddressCoin.get(Address + "_" + Rete).equalsIgnoreCase("nullo")) {
            //se il token non è gestito da coingecko ritorno null immediatamente
            //non ha senso andare avanti con le richieste
            return null;
        }

        
        
        
        
        
        
        Address = Address.toUpperCase();
        String risultato;// = null;
        //come prima cosa devo decidere il formato data
        String DataOra = ConvertiDatadaLongallOra(Datalong);
        String DataGiorno = ConvertiDatadaLong(Datalong);
        risultato = MappaConversioneAddressEUR.get(DataOra + "_" + Address + "_" + Rete);

        if (risultato == null) {

            //solo in questo caso vado a prendere il valore del giorno e non quello orario
            RecuperaTassidiCambiodaAddress(DataGiorno, DataGiorno, Address, Rete ,Simbolo);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
            risultato = MappaConversioneAddressEUR.get(DataOra + "_" + Address + "_" + Rete);
            if (risultato == null) {
                risultato = MappaConversioneAddressEUR.get(DataGiorno + "_" + Address + "_" + Rete);
            }
        } //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra
//ora controllo che l'indirizzo sia gestito, in caso contrario termino il ciclo
//questo perchè con la richiesta che ho appena fatto potrebbe essersi generata una nuova voce nella mappa
        if (MappaConversioneAddressCoin.get(Address+"_"+Rete)!=null&&MappaConversioneAddressCoin.get(Address+"_"+Rete).equalsIgnoreCase("nullo"))
        {
            return null;
        }
        
  //se è gestito controllo se lo avevo già nel file ein caso contrario lo inserisco      
        if (MappaConversioneAddressEURtemp.get(DataOra + "_" + Address + "_" + Rete) == null) {
           // if (risultato!=null) MappaConversioneAddressEUR.put(DataOra + "_" + Address + "_" + Rete, risultato);
           // if (risultato==null) risultato="nullo";
            MappaConversioneAddressEURtemp.put(DataOra + "_" + Address + "_" + Rete, risultato);
       
            ScriviFileConversioneAddressEUR(DataOra + "_" + Address + "_" + Rete + "," + risultato);
        }
        //quindi se il risultato non è nullo faccio i calcoli
        if (risultato != null && risultato.equalsIgnoreCase("nullo")) risultato=null;
        else if (risultato != null) {
            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(25, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            }
        return risultato;
    }
    
    
    
    
    
    
       public static String ConvertiXXXEUR(String Crypto,String Qta, long Datalong) {
        //come prima cosa verifizo se ho caricato il file di conversione e in caso lo faccio
        if (MappaConversioneXXXEUR.isEmpty())
            {
                GeneraMappaCambioXXXEUR();
            }
     /*   if (MappaCoppieBinance.isEmpty())
        {
            RecuperaCoppieBinance();
        }*/
        String risultato;// = null;
        long adesso=System.currentTimeMillis();
        if (Datalong>adesso) return null;//se la data è maggiore di quella attuale allora ritrono subito null perchè non ho i prezzi
        if (Datalong<1483225200)return null;//se la data è inferioe al 2017 non recupero nessun prezzo
        String DataOra=ConvertiDatadaLongallOra(Datalong);
        String DataGiorno=ConvertiDatadaLong(Datalong);
        //risultato = MappaConversioneXXXEUR.get(DataOra+" "+Crypto);
        risultato = MappaConversioneXXXEUR_temp.get(DataOra+" "+Crypto);
        if (risultato == null) {

                     RecuperaTassidiCambioXXXUSDT(Crypto,DataGiorno,DataGiorno);//in automatico questa routine da i dati di 90gg a partire dalla data iniziale
                     risultato = MappaConversioneXXXEUR_temp.get(DataOra+" "+Crypto);

                 //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

                     //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra
        }
        //se il risultato è zero devo equipararlo ad un risultato nullo

           if (risultato != null) {
               //infatti se ritorna zero vuol dire che per quella data binance non mi fornisce nessun prezzo
               if (risultato.equalsIgnoreCase("0")) {
                   risultato = null;
               } else {
                   //questa è la mappa che al termine della conversione devo scrivere nel file;
                   MappaConversioneXXXEUR.put(DataOra + " " + Crypto, risultato);
                   risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
               }
           }
      //  System.out.println(risultato);
        return risultato;
    } 
    
    
     
    public static long ConvertiDatainLong(String Data1) {
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
     
        public static long ConvertiDatainLongMinuto(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
        }
        return m1;
    } 
    
    public static String RecuperaTassidiCambio(String DataIniziale,String DataFinale)  {      
        String ok="ok";
        try {     
            TimeUnit.SECONDS.sleep(1);
            URL url = new URI("https://tassidicambio.bancaditalia.it/terzevalute-wf-web/rest/v1.0/dailyTimeSeries?startDate="+DataIniziale+"&endDate="+DataFinale+"&baseCurrencyIsoCode=EUR&currencyIsoCode=USD").toURL();
            URLConnection connection = url.openConnection();
            System.out.println(url);
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
        } catch (URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ok;
     }
 
    
 
        public static String RecuperaTassidiCambiodaAddress(String DataIniziale, String DataFinale,String Address,String Rete,String Simbolo) {
        
        
    /*    if(MappaSimboliCoingecko.isEmpty())    {
            RecuperaCoinsCoingecko();
        }
        if (Simbolo!=null && MappaSimboliCoingecko.get(Simbolo.toUpperCase().trim())==null){
            //Se ho un simbolo e questo non è nella lista allora termino subito il ciclo che tanto mi restituirebbe null lo stesso
            return null;
        }*/
            
        if (MappaConversioneAddressEUR.isEmpty()) {
            GeneraMappaCambioAddressEUR();
         //   MappaConversioneSimboloReteCoingecko.put("BSC", "binance-smart-chain");
        }
        if (MappaConversioneAddressCoin.isEmpty())
            {
                GeneraMappaConversioneAddressCoin();               
            }
        
        //come prima cosa vedo se la rete è gestita altrimenti chiudo immediatamente il ciclo    
         if (CDC_Grafica.Mappa_ChainExplorer.get(Rete)==null)   {
             return null;
         }
        
        long dataAdesso= System.currentTimeMillis() / 1000;  
        long dataIni = ( ConvertiDatainLong(DataIniziale) / 1000 ) - 86400;
        long dataFin = ConvertiDatainLong(DataFinale) / 1000 + 86400;
        if (dataFin>dataAdesso) dataFin=dataAdesso;
      //  String ID=DammiIDCoingeckodaAddress(Address,Rete);
        
   //     if (ID!=null&&!ID.equalsIgnoreCase("nulladifatto")){//quando non trovo nulla potrei aver restituito null o nulladifatto
            //in questo caso ovviamente non vado avanti con la funzione che tanto non posso trovare i prezzi
        
        //come prima cosa invididuo i vari intervalli di date da interrogare per riempire tutto l'intervallo
        long difData=dataFin-dataIni;
        ArrayList<Long> ArraydataIni = new ArrayList<>();
        ArrayList<Long> ArraydataFin = new ArrayList<>();
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
      // System.out.println(dataIni+" - "+dataFin);
        long temp;
        while (difData>0){
            ArraydataIni.add(dataIni);
            temp=dataIni+7776000;
            if (temp>dataAdesso) temp=dataAdesso;
            ArraydataFin.add(temp);
            dataIni=dataIni+7776000;
            difData=dataFin-dataIni;    
           // i++;
        }
//MappaConversioneSimboloReteCoingecko.put("BSC", "binance-smart-chain");
      
  //    MappaConversioneAddressCoin.isEmpty()
            

for (int i=0;i<ArraydataIni.size();i++){
        try {
            TimeUnit.SECONDS.sleep(7);//il timeout serve per evitare di fare troppe richieste all'API
            URL url;
            //DA RIVEDERE!!!!!!!!!!!!!!
           // https://api.coingecko.com/api/v3/coins/crypto-com-chain/market_chart/range?vs_currency=eur&from=1644879600&to=1648335600
            if (!Address.equalsIgnoreCase("CRO"))
                url = new URI("https://api.coingecko.com/api/v3/coins/"+CDC_Grafica.Mappa_ChainExplorer.get(Rete)[3]+"/contract/"+Address+"/market_chart/range?vs_currency=EUR&from=" + ArraydataIni.get(i) + "&to=" + ArraydataFin.get(i)).toURL();
            else
                url = new URI("https://api.coingecko.com/api/v3/coins/crypto-com-chain/market_chart/range?vs_currency=eur&from=" + ArraydataIni.get(i) + "&to=" + ArraydataFin.get(i)).toURL();               
            System.out.println(url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();
             StringBuilder response;
           //  System.out.println(statusCode);
            if (statusCode >= 200 && statusCode <= 299) {
                //come prima cosa se la richiesta ha avuto successo riempio tutte le ore in tutte le date con prezzo a zero
                //poi verranno sostituiti dai valori reali nel momento in cui leggerò la risposta
                //questo mi serve per avere sempre una risposta anche per le coin senza prezzi
                long DataProggressiva=ArraydataIni.get(i)*1000;
                Date data;
                String Data;
                SimpleDateFormat sdfx = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
             //    System.out.println(DataProggressiva);
              //   System.out.println(ArraydataFin.get(i));
                 while (DataProggressiva < ArraydataFin.get(i)*1000) {                    
                    data = new java.util.Date(DataProggressiva);                   
                    sdfx.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                    Data = sdfx.format(data);
                    MappaConversioneAddressEUR.put(Data+"_"+Address+"_"+Rete, "nullo");
                    DataProggressiva=DataProggressiva+3600000;
                }
                 
                response = new StringBuilder();
                // La chiamata API ha avuto successo, leggi la risposta
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
             }
            else {
                // Si è verificato un errore nella chiamata API
                response = new StringBuilder();
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    response.append(errorLine);
                }
                errorReader.close();
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                JsonElement errore=jsonObject.get("error");
                if (errore!=null && errore.toString().contains("coin not found"))
                    {
                    //Se arrivo qua vuol dire che la coin non è gestita da coingecko e la salvo nella lista degli address esclusi
                    MappaConversioneAddressCoin.put(Address+"_"+Rete, "nullo");
                    ScriviFileConversioneAddressCoin(Address+"_"+Rete+",nullo");
                    return null;  
                    }
            }
             
            
            
                //se nella risposta ho questo genere di errore significa che quell'address non è gestito
                //a questo punto lo escludo dalle ulteriori ricerche mettendolo in una tabella apposita
                //e ovviamente chiudo il ciclo
                

                System.out.println(response.toString());
/////System.out.println(response);
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                JsonArray pricesArray = jsonObject.getAsJsonArray("prices");
                //  List<PrezzoData> prezzoDataList = new ArrayList<>();
                if (pricesArray != null) {
                    for (JsonElement element : pricesArray) {
                        JsonArray priceArray = element.getAsJsonArray();
                        if (priceArray.size()==2)
                    {
                    //   if (rigaSplittata[0].equalsIgnoreCase("Euro")) MappaConversioneUSDEUR.put(rigaSplittata[5], rigaSplittata[3]);
                    
                        long timestamp = priceArray.get(0).getAsLong();
                        String price = priceArray.get(1).getAsString();
                        Date date = new java.util.Date(timestamp);
                        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                        SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                        sdf2.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                        String DataconOra = sdf.format(date);
                        String Data = sdf2.format(date);
                        if (MappaConversioneAddressEUR.get(Data)==null) MappaConversioneAddressEUR.put(Data+"_"+Address+"_"+Rete, price);
                        MappaConversioneAddressEUR.put(DataconOra+"_"+Address+"_"+Rete, price);
                        //il prezzo ovviamente indica quanti euro ci vogliono per acquistare 1 usdt ovvero usdt/euro
                        //In questo modo metto nella mappa l'ultimo valore della giornata per ogni data + il valore per ogni ora
                        //System.out.println(MappaConversioneUSDTEUR.get(DataconOra) + " - " + DataconOra);
                        //ora devo gestire l'inserimento nella mappa
                        }
                    }
                } else {
                    connection.disconnect();
                    return null;
                }
         connection.disconnect();
        } 

        catch (MalformedURLException ex) {
            return null;
        } catch (IOException | URISyntaxException | InterruptedException ex) {
            return null;
        }
        }

        return "ok";

    }
    
    
    
    
    
    public static String RecuperaTassidiCambioUSDT(String DataIniziale, String DataFinale) {
        
        
             //  Object DateCambi[]=MappaConversioneUSDTEUR.keySet().toArray();
   /*     long adesso=System.currentTimeMillis();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
        String DataAttuale = sdf.format(adesso).trim();*/
      //  if(DataAttuale.equalsIgnoreCase(DataIniziale))
        
      // for (int i=0;i<DateCambi.length;i++){
           //devo fare in modo di non scrivere mai i dati della data odierna sul file dei cambi perchè potrebbero essere incompleti
         //  String Giorno=DateCambi[i].toString().split(" ")[0].trim();
          // if (!Giorno.equalsIgnoreCase(DataAttuale)){
                
        //   }
     //  }
        
        
        String ok = "ok";
        long dataIni = ConvertiDatainLong(DataIniziale) / 1000;
        long dataFin = ConvertiDatainLong(DataFinale) / 1000 + 86400;
        long adesso=System.currentTimeMillis()/1000;
        //    if(adesso<timestampFinale)timestampFinale=adesso;
        
        //come prima cosa invididuo i vari intervalli di date da interrogare per riempire tutto l'intervallo
        long difData=dataFin-dataIni;
        ArrayList<Long> ArraydataIni = new ArrayList<>();
        ArrayList<Long> ArraydataFin = new ArrayList<>();
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
      // System.out.println(dataIni+" - "+dataFin);
        while (difData>0&&dataIni<adesso){//se la fifferenza tra le date è <0 e la data iniziale è minore della data attuale allora creo l'array
            ArraydataIni.add(dataIni);
            ArraydataFin.add(dataIni+7776000);
            dataIni=dataIni+7776000;
            difData=dataFin-dataIni;    
           // i++;
        }


for (int i=0;i<ArraydataIni.size();i++){
   // if(DataAttuale.equalsIgnoreCase(DataIniziale))
        try {
            
            TimeUnit.SECONDS.sleep(3);//il timeout serve per evitare di fare troppe richieste all'API
            URL url = new URI("https://api.coingecko.com/api/v3/coins/tether/market_chart/range?vs_currency=EUR&from=" + ArraydataIni.get(i) + "&to=" + ArraydataFin.get(i)).toURL();
            URLConnection connection = url.openConnection();
            System.out.println(url);
            try ( BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);

                }

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                JsonArray pricesArray = jsonObject.getAsJsonArray("prices");
                //  List<PrezzoData> prezzoDataList = new ArrayList<>();
                if (pricesArray != null) {
                    for (JsonElement element : pricesArray) {
                        JsonArray priceArray = element.getAsJsonArray();
                        if (priceArray.size()==2)
                    {
                    //   if (rigaSplittata[0].equalsIgnoreCase("Euro")) MappaConversioneUSDEUR.put(rigaSplittata[5], rigaSplittata[3]);
                    
                        long timestamp = priceArray.get(0).getAsLong();
                        String price = priceArray.get(1).getAsString();
                        Date date = new java.util.Date(timestamp);
                        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                        SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                        sdf2.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                        String DataconOra = sdf.format(date);
                        String Data = sdf2.format(date);
                        if (MappaConversioneUSDTEUR.get(Data)==null) MappaConversioneUSDTEUR.put(Data, price);
                        MappaConversioneUSDTEUR.put(DataconOra, price);
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
            ScriviFileConversioneUSDTEUR();
           // TimeUnit.SECONDS.sleep(2);
        } //}

        catch (MalformedURLException ex) {
            ok = null;
        } catch (IOException ex) {
            ok = null;
        } catch (InterruptedException | URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
       // ScriviFileConversioneUSDTEUR();
        return ok;
    }
    
  
    
    
    
    
        public static String RecuperaTassidiCambioXXXUSDT(String Crypto,String DataIniziale, String DataFinale) {
                      
        String ok = null;
        String CoppiaCrypto=Crypto+"USDT";
        long dataIni = ConvertiDatainLong(DataIniziale) ;
        long dataFin = ConvertiDatainLong(DataFinale) + 86400000;
        
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

            //come prima cosa imposto il valore di tutte le ore a zero
            //poi quelle che verranno invece realmente valorizzate sovrasscriveranno questo valore ocn quello reale
            //quaesto serve per avere un valore anche se dalle api non dovesse risultare
            //e per evitare che future richieste api vengano ripetute a vuoto
            long timestampIniziale = ArraydataIni.get(i);
            long timestampFinale = ArraydataFin.get(i);
            long adesso=System.currentTimeMillis();
            ConvertiUSDTEUR("1", timestampIniziale);//Questo serve solo per generare la tabella con i prezzi di USDT qualora non vi dovessero essere
            if(adesso<timestampFinale)timestampFinale=adesso;
            while (timestampIniziale < timestampFinale) {
                Date date = new java.util.Date(timestampIniziale);
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                String DataconOra = sdf.format(date);
                //nel caso in cui non trovo il prezzo di usdc lo stesso lo equiparo a usdt
                //questo perchè per un periodo binance non ha fornito i prezzi di usdc
                MappaConversioneXXXEUR_temp.put(DataconOra + " " + Crypto, "0");
                if (CoppiaCrypto.equalsIgnoreCase("USDCUSDT")
                        || CoppiaCrypto.equalsIgnoreCase("BUSDUSDT")
                        || CoppiaCrypto.equalsIgnoreCase("DAIUSDT")) 
                {
                        String Prezzo=MappaConversioneUSDTEUR.get(DataconOra);
                        if(MappaConversioneUSDTEUR.get(DataconOra)!=null)
                            {
                          //  String Prezzo = ConvertiUSDTEUR("1", timestampIniziale);
                            MappaConversioneXXXEUR_temp.put(DataconOra + " " + Crypto, Prezzo);
                            }
                }
                timestampIniziale = timestampIniziale + 3600000;//aggiungo 1 ora
            }

            try {
                String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + CoppiaCrypto + "&interval=1h&startTime=" + ArraydataIni.get(i) + "&endTime=" + ArraydataFin.get(i) + "&limit=1000";
                URL url = new URI(apiUrl).toURL();
                URLConnection connection = url.openConnection();
                System.out.println(url);
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);

                    }

                    Gson gson = new Gson();
                    //System.out.println(response.toString());
                    // JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                    JsonArray pricesArray = gson.fromJson(response.toString(), JsonArray.class);
                    //JsonArray pricesArray = jsonObject.getAsJsonArray("prices");
                    //  List<PrezzoData> prezzoDataList = new ArrayList<>();
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
                                //perchè i dati della data odfierna non vengono salvati nel file di conversione dei valori di usdt
                                //e questo genererebbe una richiesta inutile su coingecko
                              
                                    String Prezzo = ConvertiUSDTEUR(price, timestamp);
                                    // System.out.println(DataconOra+" "+Crypto+" - "+Prezzo);
                                    MappaConversioneXXXEUR_temp.put(DataconOra + " " + Crypto, Prezzo);
                                
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
                Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
                ok = null;
            } catch (IOException ex) {
                Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
                ok = null;
            } catch (InterruptedException ex) {
                ok = null;
                Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            } catch (URISyntaxException ex) {
                Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //   ScriviFileConversioneUSDTEUR();
        //System.out.println(ok);
        return ok;
    }
    
    
    public static String DammiPrezzoTransazioneOLD(String Moneta1, String Moneta2, String Qta1, String Qta2,
            long Data, String Prezzo, boolean PrezzoZero, int Decimali, String Address1, String Address2, String Rete) {
        
        
        String PrezzoTransazione;
     /*   System.out.println(Moneta1);
        System.out.println(Moneta2);
        System.out.println(Qta1);
        System.out.println(Qta2);
        System.out.println(Data);0xEadAa45fC7e8912d8AabF415205830f6b567610b
        System.out.println(Prezzo);
        System.out.println(PrezzoZero);
        System.out.println(Decimali);
        System.out.println(Address1);
        System.out.println(Address2);
        System.out.println(Rete);
        System.out.println("-------");*/
        Map<String, String> MappaReteCoin = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        MappaReteCoin.put("BSC", "BNB");
        // boolean trovato1=false;
        // boolean trovato2=false;
        //come prima cosa controllo se sto scambiando usdt e prendo quel prezzo come valido
        //metto anche la condizione che address sia null perchè ci sono delle monete che hanno simbolo eur ma non lo sono
        //e se hanno un address sicuramente non sono fiat
        if (Moneta1 != null && Moneta1.equalsIgnoreCase("EUR") && Address1 == null) {
            PrezzoTransazione = Qta1;
            if (PrezzoTransazione != null) {
                PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                return PrezzoTransazione;
            }
        } else if (Moneta2 != null && Moneta2.equalsIgnoreCase("EUR") && Address2 == null) {
            PrezzoTransazione = Qta2;
            if (PrezzoTransazione != null) {
                PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                return PrezzoTransazione;
            }
        } else if (Moneta1 != null && Moneta1.equalsIgnoreCase("USDT")) {
            //se l'address è uguale alla moneta di riferimento della rete vuol dire che sto chiedendo il prezzo della coin nativa
            if (Address1==null){
                PrezzoTransazione = ConvertiUSDTEUR(Qta1, Data);
            }                    
            else
                {
                PrezzoTransazione = ConvertiAddressEUR(Qta1, Data, Address1, Rete,Moneta1);
                } 
            if (PrezzoTransazione != null) {
                PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                return PrezzoTransazione;
            }
        } else if (Moneta2 != null && Moneta2.equalsIgnoreCase("USDT")) {
            if (Address2==null){
                PrezzoTransazione = ConvertiUSDTEUR(Qta2, Data);
                 }                    
            else
                {
                PrezzoTransazione = ConvertiAddressEUR(Qta2, Data, Address2, Rete,Moneta2);
                } 
            if (PrezzoTransazione != null) {
                PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                return PrezzoTransazione;
            }
        } else {

            //ora scorro le coppie principali per vedere se trovo corrispondenze e in quel caso ritorno il prezzo
            for (String CoppiePrioritarie1 : CoppiePrioritarie) {
                if (Qta1 != null && Moneta1 != null && (Moneta1 + "USDT").toUpperCase().equals(CoppiePrioritarie1)) {
                    // trovato1=true;
                    if (Address1==null  || MappaReteCoin.get(Rete).equalsIgnoreCase(Address1)){
                        PrezzoTransazione = ConvertiXXXEUR(Moneta1, Qta1, Data);
                    }
                    else
                       {
                        PrezzoTransazione = ConvertiAddressEUR(Qta1, Data, Address1, Rete,Moneta1);
                       } 
                    if (PrezzoTransazione != null) {
                        PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                        return PrezzoTransazione;
                    }//ovviamente se il prezzo è null vado a cercarlo sull'altra coppia
                    //se trovo la condizione ritorno il prezzo e interrnompo la funzione
                    
                }
                if (Qta2 != null && Moneta2 != null && (Moneta2 + "USDT").toUpperCase().equals(CoppiePrioritarie1)) {
                    // trovato2=true;
                    if (Address2==null  || MappaReteCoin.get(Rete).equalsIgnoreCase(Address2)){
                        PrezzoTransazione = ConvertiXXXEUR(Moneta2, Qta2, Data);
                    }
                    else
                       {
                        PrezzoTransazione = ConvertiAddressEUR(Qta2, Data, Address2, Rete,Moneta2);
                       } 
                    if (PrezzoTransazione != null) {
                        PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                        return PrezzoTransazione;
                    }
                    
                    //se trovo la condizione ritorno il prezzo e interrnompo la funzione
                }
            }
            //Se arrivo qua vuol dire che non ho trovato il prezzo tra le coppie prioritarie
            //a questo punto controllo se ho l'address delle monetee controllo su coingecko.

            if (Qta1 != null && Moneta1 != null && Address1 != null && Rete != null) {
                PrezzoTransazione = ConvertiAddressEUR(Qta1, Data, Address1, Rete,Moneta1);
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    //   trovato1=true;
                    return PrezzoTransazione;
                }
            }
            if (Qta2 != null && Moneta2 != null && Address2 != null && Rete != null) {
                PrezzoTransazione = ConvertiAddressEUR(Qta2, Data, Address2, Rete,Moneta2);
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
            }

            //a questo punto la cerco tra tutte le coppie che binance riconosce
            if (MappaCoppieBinance.isEmpty()) {
                RecuperaCoppieBinance();
                //se non ho la mappa delle coppie di binance la recupero
            }
            //Se ho gli address non cerco in binance i dati che potrei incorrere in errori di omonimia
            //utilizzo l'address che è molto più preciso
            if (Qta1 != null && Moneta1 != null && MappaCoppieBinance.get(Moneta1 + "USDT") != null && Address1==null) {
                PrezzoTransazione = ConvertiXXXEUR(Moneta1, Qta1, Data);
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
                //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
            if (Qta2 != null && Moneta2 != null && MappaCoppieBinance.get(Moneta2 + "USDT") != null && Address2==null) {
                PrezzoTransazione = ConvertiXXXEUR(Moneta2, Qta2, Data);
                // System.out.println("prezzo.."+PrezzoTransazione);
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
                //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
        }
        if (PrezzoZero) {
            return "0.00";
        } else {
            return Prezzo;
        }
    }
 
    
    
    public static String DammiPrezzoTransazione(Moneta Moneta1, Moneta Moneta2, long Data, String Prezzo, boolean PrezzoZero, int Decimali, String Rete) {

        /*Questa funzione si divide in 4 punti fondamentali:
        1 - Verifico che una delle 2 monete di scambio sia una Fiat e in quel caso prendo quello come prezzo della transazione anche perchè è il più affidabile
        2 - Verifico se una delle 2 monete è USDT in quel caso prendo quello come valore in quanto USDT è una moneta di cui mi salvo tutti i prezzi storici
        3 - Verifico se una delle 2 monete non faccia parte di uno specifico gruppo delle monete più capitalizzate presenti su binance, in quel caso prendo quello come
        prezzo della transazione in quanto il prezzo risulta sicuramente più preciso di quello di una shitcoin o comunque di una moneta con bassa liquidità
        4 - Prendo il prezzo della prima moneta disponibile essendo che l'affidabilità del prezzo è la stessa per entrambe le monete dello scambio      
        */
        String PrezzoTransazione;
        
        //come prima cosa prima di iniziare controllo che la moneta in questione non sia già una di quelle in lista
        //tra quelle in defi importanti di cui conosco l'address e listate da binance, in quel caso il prezzo lo prenderò da li e non da coingecko
        //dato le limitazioni che quest'ultimo comporta
        //per far questo se trovo le suddette monete nella lista elimino address per farle prendere da binance
        String AddressMoneta1=null;
                if(Moneta1!=null)AddressMoneta1=Moneta1.MonetaAddress;
        String AddressMoneta2=null;
                if(Moneta2!=null)AddressMoneta2=Moneta2.MonetaAddress;
        if (Moneta1!=null&&CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta1.MonetaAddress+"_"+Rete)!=null){
            //Rete=null;
            AddressMoneta1=null;
            //System.out.println(CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta1.MonetaAddress+"_"+Rete));
            Moneta1.Moneta=CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta1.MonetaAddress+"_"+Rete);
        }
        if (Moneta2!=null&&CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta2.MonetaAddress+"_"+Rete)!=null){
           // Rete=null;
            AddressMoneta2=null;
            //System.out.println(CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta2.MonetaAddress+"_"+Rete));
            Moneta2.Moneta=CDC_Grafica.Mappa_AddressRete_Nome.get(Moneta2.MonetaAddress+"_"+Rete);
        }
        
    String MonetaRete=null;
    if (Rete!=null)MonetaRete=CDC_Grafica.Mappa_ChainExplorer.get(Rete)[2];
        // boolean trovato1=false;
        // boolean trovato2=false;
        //come prima cosa controllo se sto scambiando usdt e prendo quel prezzo come valido
        //metto anche la condizione che address sia null perchè ci sono delle monete che hanno simbolo eur ma non lo sono
        //e se hanno un address sicuramente non sono fiat
        //se almeno una delle 2 monete è una FIAT prendo il prezzo da quella
        
        //PARTE 1 - VERIFICO SE FIAT
        if ((Moneta1 != null && Moneta1.Tipo.trim().equalsIgnoreCase("FIAT")) || (Moneta2 != null && Moneta2.Tipo.trim().equalsIgnoreCase("FIAT"))) {
            //per ora gestisco solo eruo ma sarà da aggiungere anche la parte USD
            if (Moneta1 != null && Moneta1.Moneta.equalsIgnoreCase("EUR")) {
                PrezzoTransazione = Moneta1.Qta;
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
            } else if (Moneta2 != null && Moneta2.Moneta.equalsIgnoreCase("EUR")) {
                PrezzoTransazione = Moneta2.Qta;
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
            }

        } //se non sono FIAT controllo se una delle coppie è USDT in quel caso prendo il prezzo di quello 

        //PARTE 2 VERIFICO SE USDT
        else if (Moneta1 != null && Moneta1.Moneta.equalsIgnoreCase("USDT") && Moneta1.Tipo.trim().equalsIgnoreCase("Crypto")) {
                //a seconda se ho l'address o meno recupero il suo prezzo in maniera diversa
                //anche perchè potrebbe essere che sia un token che si chiama usdt ma è scam
                if (AddressMoneta1 == null) {
                    PrezzoTransazione = ConvertiUSDTEUR(Moneta1.Qta, Data);
                } else {
                    PrezzoTransazione = ConvertiAddressEUR(Moneta1.Qta, Data, AddressMoneta1, Rete, Moneta1.Moneta);
                }
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
            } else if (Moneta2 != null && Moneta2.Moneta.equalsIgnoreCase("USDT") && Moneta2.Tipo.trim().equalsIgnoreCase("Crypto")) {
                if (AddressMoneta2 == null) {
                    PrezzoTransazione = ConvertiUSDTEUR(Moneta2.Qta, Data);
                } else {
                    PrezzoTransazione = ConvertiAddressEUR(Moneta2.Qta, Data, AddressMoneta2, Rete, Moneta2.Moneta);
                }
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                    return PrezzoTransazione;
                }
            }
         else {
            //PARTE 3 - VERIFICO SE COPPIE PRIORITARIE
            //ora scorro le coppie principali per vedere se trovo corrispondenze e in quel caso ritorno il prezzo
            for (String CoppiePrioritarie1 : CoppiePrioritarie) {
                if (Moneta1 != null &&  (Moneta1.Moneta + "USDT").toUpperCase().equals(CoppiePrioritarie1) && Moneta1.Tipo.trim().equalsIgnoreCase("Crypto")) {
                    // trovato1=true;
                  //  System.out.println("aaa"+Moneta1.MonetaAddress);
                    if (AddressMoneta1 == null || MonetaRete.equalsIgnoreCase(AddressMoneta1)) {
                        PrezzoTransazione = ConvertiXXXEUR(Moneta1.Moneta, Moneta1.Qta, Data);
                    } else {
                        PrezzoTransazione = ConvertiAddressEUR(Moneta1.Qta, Data, AddressMoneta1, Rete, Moneta1.Moneta);
                        
                    }
                    if (PrezzoTransazione != null) {
                        PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                        return PrezzoTransazione;
                    }//ovviamente se il prezzo è null vado a cercarlo sull'altra coppia
                    //se trovo la condizione ritorno il prezzo e interrnompo la funzione

                }
                if (Moneta2 != null && (Moneta2.Moneta + "USDT").toUpperCase().equals(CoppiePrioritarie1)&& Moneta2.Tipo.trim().equalsIgnoreCase("Crypto")) {
                    // trovato2=true;
                    if (AddressMoneta2 == null || MonetaRete.equalsIgnoreCase(AddressMoneta2)) {
                        PrezzoTransazione = ConvertiXXXEUR(Moneta2.Moneta, Moneta2.Qta, Data);
                    } else {
                        PrezzoTransazione = ConvertiAddressEUR(Moneta2.Qta, Data, AddressMoneta2, Rete, Moneta2.Moneta);
                    }
                    if (PrezzoTransazione != null) {
                        PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                        return PrezzoTransazione;
                    }

                    //se trovo la condizione ritorno il prezzo e interrnompo la funzione
                }
            }
            //Se arrivo qua vuol dire che non ho trovato il prezzo tra le coppie prioritarie
            //a questo punto controllo se ho l'address delle monetee controllo su coingecko.
            //a questo punto la cerco tra tutte le coppie che binance riconosce
            if (MappaCoppieBinance.isEmpty()) {
                RecuperaCoppieBinance();
                
                //se non ho la mappa delle coppie di binance la recupero
            }
            //PARTE 4 - Prendo il prezzo della prima moneta disponibile
            if (Moneta1 != null && Moneta1.Tipo.trim().equalsIgnoreCase("Crypto")) {
                //se trovo la moneta su binance e non ho l'address cerco il prezzo su binance
                //altrimenti lo prendo da coingecko se ho l'address e ho anche la rete
                //in alternativa restituisco null
                if(MappaCoppieBinance.get(Moneta1 + "USDT") != null && AddressMoneta1 == null){
                    PrezzoTransazione = ConvertiXXXEUR(Moneta1.Moneta, Moneta1.Qta, Data);
                  //  System.out.println("ConvertiXXXEUR "+Moneta1.Moneta+" - "+Data);
                }
                else if(AddressMoneta1!= null && Rete != null)
                  {
                      PrezzoTransazione = ConvertiAddressEUR(Moneta1.Qta, Data, AddressMoneta1, Rete, Moneta1.Moneta);
                    //  System.out.println("ConvertiAddressEUR "+Moneta1.Moneta+" - "+Data+" - "+PrezzoTransazione);
                  }  
                else
                   { 
                    PrezzoTransazione=null;
                    }
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                  //  System.out.println(Moneta1.Moneta+" - "+Data+" - "+PrezzoTransazione);
                    //   trovato1=true;
                    return PrezzoTransazione;
                }
            }
            if (Moneta2 != null && Moneta2.Tipo.trim().equalsIgnoreCase("Crypto")) {
                
                if(MappaCoppieBinance.get(Moneta2 + "USDT") != null && AddressMoneta2 == null){
                    PrezzoTransazione = ConvertiXXXEUR(Moneta2.Moneta, Moneta2.Qta, Data);
                   // System.out.println("ConvertiXXXEUR "+Moneta2.Moneta+" - "+Data);
                }
                else if(AddressMoneta2!= null && Rete != null)
                  {
                      PrezzoTransazione = ConvertiAddressEUR(Moneta2.Qta, Data, AddressMoneta2, Rete, Moneta2.Moneta);
                    //  System.out.println(Moneta2.Moneta+" - "+Data+" - "+PrezzoTransazione);
                  }  
                else
                   { 
                    PrezzoTransazione=null;
                    }
                
                if (PrezzoTransazione != null) {
                    PrezzoTransazione = new BigDecimal(PrezzoTransazione).abs().setScale(Decimali, RoundingMode.HALF_UP).toPlainString();
                  //  System.out.println(Moneta2.Moneta+" - "+Data+" - "+PrezzoTransazione);
                    return PrezzoTransazione;
                }
            }

        }
        if (PrezzoZero) {
            return "0.00";
        } else {
            return Prezzo;
        }
    }

      
    //questa funzione la chiamo sempre una sola volta per verificare quali sono le coppie di trading di cui binance mi fornisce i dati    
    public static String RecuperaCoppieBinance() {
        String ok = "ok";

        try {
            String apiUrl = "https://api.binance.com/api/v3/exchangeInfo";
            URL url = new URI(apiUrl).toURL();
            URLConnection connection = url.openConnection();
            System.out.println(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
            JsonArray pricesArray = jsonObject.getAsJsonArray("symbols");
            if (pricesArray != null) {
                for (JsonElement element : pricesArray) {

                    JsonObject Coppie = element.getAsJsonObject();
                    String symbol = Coppie.get("symbol").getAsString();
                    if (symbol.substring(symbol.length()-4).equals("USDT")) 
                        {
                        //System.out.println(symbol);
                        MappaCoppieBinance.put(symbol, symbol);
                        }
                }
            } else {
                ok = null;
            }

            TimeUnit.SECONDS.sleep(1);
        } catch (JsonSyntaxException | IOException | InterruptedException ex) {
            ok = null;
        } catch (URISyntaxException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ok;
    }
        
    //questa funzione la chiamo sempre una sola volta per verificare quali sono le coin gestite da coingecko    
    public static String RecuperaCoinsCoingecko() {

        try {
            TimeUnit.SECONDS.sleep(7);
            URL url = new URI("https://api.coingecko.com/api/v3/coins/list").toURL();
            System.out.println(url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();
            StringBuilder response;
            
            if (statusCode >= 200 && statusCode <= 299) {
                response = new StringBuilder();
                // La chiamata API ha avuto successo, leggi la risposta
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                JSONArray pricesArray = new JSONArray(response.toString());

                for (int i = 0; i < pricesArray.length(); i++) {
                    JSONObject coinObject = pricesArray.getJSONObject(i);
                    String coinSymbol = coinObject.getString("symbol").toUpperCase().trim();
                    //System.out.println(coinSymbol);
                    MappaSimboliCoingecko.put(coinSymbol, coinSymbol);
                }
                connection.disconnect();

            } else {
                
                // Si è verificato un errore nella chiamata API
                response = new StringBuilder();
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    response.append(errorLine);
                }
                errorReader.close();
                connection.disconnect();
                return null;

            }

        } catch (JsonSyntaxException | IOException | InterruptedException | URISyntaxException ex) {
            return null;
        }

        return "ok";
        //solo se il comando va a buon fine ritorno ok altrimenti ritorno null
    }
    
    
    
     static void ScriviFileConversioneUSDEUR() { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
       FileWriter w=new FileWriter("cambioUSDEUR.db");
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
     
     
        static void ScriviFileConversioneAddressCoin(String Riga) { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
       FileWriter w=new FileWriter("conversioneAddressCoin.db",true);
       BufferedWriter b=new BufferedWriter (w);
       b.append(Riga+"\n");
       b.close();
       w.close();
       
      //il file è cosi composto
      //Address_rete,Simbolo,ID Coingecko
      //Es. 0x1456345343737364_BSC,BUSD,Binance USD
      //Simboli rete usati per ora
      //CRO=Cronos chain
      //BSC=Binance Smart Chain
      //ETH=Ethereum

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }
   
        
   
        
    static void ScriviFileConversioneAddressEUR(String Riga) { 

   try { 
       FileWriter w=new FileWriter("cambioAddressEUR.db",true);
       BufferedWriter b=new BufferedWriter (w);
       b.append(Riga+"\n");
       b.close();
       w.close();
       
      //il file è cosi composto
      //Data_Address_rete,Prezzo
      //Es. 2023-01-01 01_0x1456345343737364_BSC,BUSD,Binance USD
      //Simboli rete usati per ora
      //CRO=Cronos chain
      //BSC=Binance Smart Chain
      //ETH=Ethereum

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }      
        
        
        
        
   static void ScriviFileConversioneSwapTransIDCoins(String riga) { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
       FileWriter w=new FileWriter("conversioneTransIDCoins.db",true);
       BufferedWriter b=new BufferedWriter (w);
       b.append(riga+"\n");
       b.close();
       w.close();
             
      //il file è cosi composto
      //TransID_rete,coin uscita,qta uscita,coin entrata,qta entrata
      //Es. 0x1456345343737364_BSC,BUSD,10,eth,0.005
      //Simboli rete usati per ora
      //CRO=Cronos chain
      //BSC=Binance Smart Chain
      //ETH=Ethereum

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }
    
   static void ScriviFileConversioneXXXEUR() { //CDC_FileDatiDB
   // Devo lanciare questa funzione alla fine di ogni conversione per aggiornare i dati sul database dei prezzi
   //questo serve per recuperare prima sempre tutti i dati della mappa
   GeneraMappaCambioXXXEUR();
   try { 
       FileWriter w=new FileWriter("cambioXXXEUR.db");
       BufferedWriter b=new BufferedWriter (w);
       Object DateCambi[]=MappaConversioneXXXEUR.keySet().toArray();
       
       for (Object DateCambi1 : DateCambi) {
           b.write(DateCambi1.toString() + "," + MappaConversioneXXXEUR.get(DateCambi1.toString()) + "\n");
       }
       
      // b.write("DataIniziale="+CDC_DataIniziale+"\n");
       //System.out.println(CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
       b.close();
       w.close();

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   } 
     
     
     
        static void ScriviFileConversioneUSDTEUR() { //CDC_FileDatiDB
   // CDC_FileDatiDB
   try { 
//devo fare in modo di non scrivere mai i dati della data odierna sul file dei cambi perchè potrebbero essere incompleti
       FileWriter w=new FileWriter("cambioUSDTEUR.db");
       BufferedWriter b=new BufferedWriter (w);
       
       Object DateCambi[]=MappaConversioneUSDTEUR.keySet().toArray();
        long adesso=System.currentTimeMillis();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
        String DataAttuale = sdf.format(adesso).trim();
        
       for (int i=0;i<DateCambi.length;i++){
           //devo fare in modo di non scrivere mai i dati della data odierna sul file dei cambi perchè potrebbero essere incompleti
           String Giorno=DateCambi[i].toString().split(" ")[0].trim();
           if (!Giorno.equalsIgnoreCase(DataAttuale)){
                b.write(DateCambi[i].toString()+","+MappaConversioneUSDTEUR.get(DateCambi[i].toString())+"\n");
           }
       }
       
      // b.write("DataIniziale="+CDC_DataIniziale+"\n");
       //System.out.println(CDC_FiatWallet_ConsideroValoreMassimoGiornaliero);
       b.close();
       w.close();

    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
   
   }
     
    
    
    public static String ConvertiDatadaLong(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
    
        public static String ConvertiDatadaLongAlSecondo(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
    
    
        public static String ConvertiDatadaLongallOra(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH");
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
             
        public static String GiornoMenoUno(String Data1) {
        String giorno="";
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date d = f.parse(Data1+" 00:00");
            long m1 = d.getTime();
            long giornomenouno=m1-86400000;            
            SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd");
            Date d1 = new Date(giornomenouno);
            giorno=f2.format(d1);
        } catch (ParseException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        return giorno;
    }
    

}

