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
import java.net.MalformedURLException;
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

/**
 *
 * @author luca.passelli
 */
public class Calcoli {
    static Map<String, String> MappaConversioneUSDEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneUSDTEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneXXXEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaConversioneXXXEUR_temp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> MappaCoppieBinance = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    //di seguito le coppie prioritarie ovvero quelle che hanno precedneza all'atto della ricerca dei prezzi rispetto alle altre
    static String CoppiePrioritarie[]=new String []{"USDCUSDT","USDCUSDT","BUSDUSDT","DAIUSDT","TUSDUSDT","BTCUSDT",
        "ETHUSDT","BNBUSDT","LTCUSDT","ADAUSDT","XRPUSDT","NEOUSDT",
        "IOTAUSDT","EOSUSDT","XLMUSDT","SOLUSDT","PAXUSDT","TRXUSDT","ATOMUSDT","MATICUSDT"};

    
  //DA FARE : Recupero prezzi orari in base all'ora più vicina  
    //DA FARE : se ci sono euro tenere buono quello
    
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
        
        
        
    public static String ConvertiUSDEUR(String Valore, String Data) {
        if (MappaConversioneUSDEUR.isEmpty())
            {
                GeneraMappaCambioUSDEUR();
            }
        
        
        String risultato = null;
        //come prima cosa devo controllare che la data analizzata sia nel range delle date di cui ho il cambio usd/eur
        Object dateDisponibili[] = MappaConversioneUSDEUR.keySet().toArray();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String PrimaData;
        String UltimaData;
        String DatadiOggi = f.format(System.currentTimeMillis());
        if (dateDisponibili.length > 0) {// se il file ha dei dati recupero prima e ultima data      
            PrimaData = (String) dateDisponibili[0];
            UltimaData = (String) dateDisponibili[dateDisponibili.length - 1];
            //se la data di cui cerco il valore è compreso tra i range cerco il tasso di cambio corretto
            if (ConvertiDatainLong(Data) >= ConvertiDatainLong(PrimaData) && ConvertiDatainLong(Data) <= ConvertiDatainLong(UltimaData)) {
                for (int i = 0; i < 10; i++) {
                    risultato = MappaConversioneUSDEUR.get(Data);
                    if (risultato == null) {
                        Data = GiornoMenoUno(Data);//questo appunto serve per andare a prendere i sabati e le domeniche dove non ho dati
                    } else {
                        break;
                    }
                }
            } else if (ConvertiDatainLong(Data) >= ConvertiDatainLong("2017-01-01") && ConvertiDatainLong(Data) <= ConvertiDatainLong(DatadiOggi)) {
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
                     risultato = MappaConversioneUSDTEUR.get(DataGiorno);
                 }//non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

                    } //non serve mettere nessun else in quanto se  non è null allora il valore è già stato recuperato sopra

        if (risultato != null) {
            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
        }
        return risultato;
    }
    
       public static String ConvertiXXXEUR(String Crypto,String Qta, long Datalong) {
        //come prima cosa verifizo se ho caricato il file di conversione e in caso lo faccio
        if (MappaConversioneXXXEUR.isEmpty())
            {
                GeneraMappaCambioXXXEUR();
            }
        if (MappaCoppieBinance.isEmpty())
        {
            RecuperaCoppieBinance();
        }
        String risultato;// = null;
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
        if (risultato != null) {
            //questa è la mappa che al termine della conversione devo scrivere nel file;
            MappaConversioneXXXEUR.put(DataOra+" "+Crypto, risultato);
            risultato = (new BigDecimal(Qta).multiply(new BigDecimal(risultato))).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toString();
        }
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
            URL url = new URL("https://tassidicambio.bancaditalia.it/terzevalute-wf-web/rest/v1.0/dailyTimeSeries?startDate="+DataIniziale+"&endDate="+DataFinale+"&baseCurrencyIsoCode=EUR&currencyIsoCode=USD");
            URLConnection connection = url.openConnection();
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
        }

        return ok;
     }
 
    
    
    
    public static String RecuperaTassidiCambioUSDT(String DataIniziale, String DataFinale) {
        String ok = "ok";
        long dataIni = ConvertiDatainLong(DataIniziale) / 1000;
        long dataFin = ConvertiDatainLong(DataFinale) / 1000 + 86400;
        
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
        while (difData>0){
            ArraydataIni.add(dataIni);
            ArraydataFin.add(dataIni+7776000);
            dataIni=dataIni+7776000;
            difData=dataFin-dataIni;    
           // i++;
        }
    /*    System.out.println(ArraydataIni.size());
        for (int i=0;i<ArraydataIni.size();i++){
            //qui ci metto il ciclo con le richieste e volendo anche la parte grafica con l'andamento
            System.out.println(ArraydataIni.get(i)+ " - "+ArraydataFin.get(i));
            
        }*/

for (int i=0;i<ArraydataIni.size();i++){
        try {
            URL url = new URL("https://api.coingecko.com/api/v3/coins/tether/market_chart/range?vs_currency=EUR&from=" + ArraydataIni.get(i) + "&to=" + ArraydataFin.get(i));
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
            TimeUnit.SECONDS.sleep(2);
        } 

        catch (MalformedURLException ex) {
            ok = null;
        } catch (IOException ex) {
            ok = null;
        } catch (InterruptedException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
       // ScriviFileConversioneUSDTEUR();
        return ok;
    }
    
  
    
    
    
    
        public static String RecuperaTassidiCambioXXXUSDT(String Crypto,String DataIniziale, String DataFinale) {
        String ok = "ok";
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
        //3456000 secondi equivalgono a 3 mesi (40giorni per la precisione).
        //inoltre tra una richiesta e l'altra devo aspettare almeno 2 secondi per evitare problemidi blocco ip da parte di coingecko
      // System.out.println(dataIni+" - "+dataFin);
        while (difData>0){
            ArraydataIni.add(dataIni);
            ArraydataFin.add(dataIni+Long.parseLong("3456000000"));
            dataIni=dataIni+Long.parseLong("3456000000");
            difData=dataFin-dataIni;    
           // i++;
        }

for (int i=0;i<ArraydataIni.size();i++){
        try {
            String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + CoppiaCrypto + "&interval=1h&startTime=" + ArraydataIni.get(i) + "&endTime=" + ArraydataFin.get(i)+ "&limit=1000";
            URL url = new URL(apiUrl);
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
                //System.out.println(response.toString());
               // JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                JsonArray pricesArray = gson.fromJson(response.toString(), JsonArray.class);
                //JsonArray pricesArray = jsonObject.getAsJsonArray("prices");
                //  List<PrezzoData> prezzoDataList = new ArrayList<>();
                if (pricesArray != null) {
                    for (JsonElement element : pricesArray) {
                        JsonArray priceArray = element.getAsJsonArray();
                        //System.out.println(priceArray);
                        if (priceArray.size()==12)
                    {
                        long timestamp = priceArray.get(0).getAsLong();
                        String price = priceArray.get(4).getAsString();
                        Date date = new java.util.Date(timestamp);
                        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
                        String DataconOra = sdf.format(date);
                        String Prezzo=ConvertiUSDTEUR(price,timestamp);
                       // System.out.println(DataconOra+" "+Crypto+" - "+Prezzo);
                       MappaConversioneXXXEUR_temp.put(DataconOra+" "+Crypto, Prezzo);
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
        } 

        catch (MalformedURLException ex) {
            ok = null;
        } catch (IOException ex) {
            ok = null;
        } catch (InterruptedException ex) {
            Logger.getLogger(Calcoli.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
     //   ScriviFileConversioneUSDTEUR();
        return ok;
    }
    
    
    public static String DammiPrezzoTransazione(String Moneta1,String Moneta2,String Qta1,String Qta2,long Data, String Prezzo) {
        String PrezzoTransazione;
        boolean trovato1=false;
        boolean trovato2=false;
        //come prima cosa controllo se sto scambiando usdt e prendo quel prezzo come valido
        if (Moneta1!=null && Moneta1.equalsIgnoreCase("EUR")){
            PrezzoTransazione=new BigDecimal(Qta1).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
            if (PrezzoTransazione!=null)return PrezzoTransazione;
        }
        else if  (Moneta2!=null && Moneta2.equalsIgnoreCase("EUR")){
            PrezzoTransazione=new BigDecimal(Qta2).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
             if (PrezzoTransazione!=null)return PrezzoTransazione;
        }
        else if (Moneta1!=null && Moneta1.equalsIgnoreCase("USDT")){
            PrezzoTransazione=new BigDecimal(ConvertiUSDTEUR(Qta1,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
            if (PrezzoTransazione!=null)return PrezzoTransazione;
        }
        else if  (Moneta2!=null && Moneta2.equalsIgnoreCase("USDT")){
            PrezzoTransazione=new BigDecimal(ConvertiUSDTEUR(Qta2,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
             if (PrezzoTransazione!=null)return PrezzoTransazione;
        }
        else
            {
        if (MappaCoppieBinance.isEmpty())
        {
            RecuperaCoppieBinance();
            //se non ho la mappa delle coppie di binance la recupero
        }
        //ora scorro le coppie principali per vedere se trovo corrispondenze e in quel caso ritorno il prezzo
        for (String CoppiePrioritarie1 : CoppiePrioritarie) {
            if (!trovato1 && Qta1!=null && Moneta1!=null && (Moneta1+"USDT").toUpperCase().equals(CoppiePrioritarie1)){
                trovato1=true;
                PrezzoTransazione=new BigDecimal(ConvertiXXXEUR(Moneta1,Qta1,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (PrezzoTransazione!=null)return PrezzoTransazione;//ovviamente se il prezzo è null vado a cercarlo sull'altra coppia
                //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
            if (!trovato2 && Qta2!=null && Moneta2!=null && (Moneta2+"USDT").toUpperCase().equals(CoppiePrioritarie1)){
                trovato2=true;
                PrezzoTransazione=new BigDecimal(ConvertiXXXEUR(Moneta2,Qta2,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (PrezzoTransazione!=null)return PrezzoTransazione;
               //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
        }
        //Se arrivo qua vuol dire che non ho trovato il prezzo tra le coppie prioritarie
        //a questo punto la cerco tra tutte le coppie che binance riconosce
        if (!trovato1 && Qta1!=null && Moneta1!=null && MappaCoppieBinance.get(Moneta1+"USDT")!=null){
                PrezzoTransazione=new BigDecimal(ConvertiXXXEUR(Moneta1,Qta1,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (PrezzoTransazione!=null)return PrezzoTransazione;
                //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
            if (!trovato2 && Qta2!=null && Moneta2!=null && MappaCoppieBinance.get(Moneta2+"USDT")!=null){
                PrezzoTransazione=new BigDecimal(ConvertiXXXEUR(Moneta2,Qta2,Data)).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (PrezzoTransazione!=null)return PrezzoTransazione;
               //se trovo la condizione ritorno il prezzo e interrnompo la funzione
            }
            }
        return Prezzo;
    } 
    
        
    //questa funzione la chiamo sempre una sola volta per verificare quali sono le coppie di trading di cui binance mi fornisce i dati    
    public static String RecuperaCoppieBinance() {
        String ok = "ok";

        try {
            String apiUrl = "https://api.binance.com/api/v3/exchangeInfo";
            URL url = new URL(apiUrl);
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
        }
        return ok;
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
    
         static void ScriviFileConversioneXXXEUR() { //CDC_FileDatiDB
   // Devo lanciare questa funzione alla fine di ogni conversione per aggiornare i dati sul database dei prezzi
   try { 
       FileWriter w=new FileWriter("cambioXXXEUR.db");
       BufferedWriter b=new BufferedWriter (w);
       
       Object DateCambi[]=MappaConversioneXXXEUR.keySet().toArray();
       
       for (int i=0;i<DateCambi.length;i++){
           b.write(DateCambi[i].toString()+","+MappaConversioneXXXEUR.get(DateCambi[i].toString())+"\n");
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

