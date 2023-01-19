/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca.passelli
 */
public class Calcoli {
    static Map<String, String> MappaConversioneUSDEUR = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    
    public static void GeneraMappaCambioUSDEUR(){
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
    }  
    
    
    public static String ConvertiUSDEUR(String Valore, String Data) {
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
                        Data = GiornoMenoUno(Data);
                    } else {
                        break;
                    }
                }
            } else if (ConvertiDatainLong(Data) > ConvertiDatainLong("2019-01-01") && ConvertiDatainLong(Data) < ConvertiDatainLong(DatadiOggi)) {
                if (ConvertiDatainLong(Data) < ConvertiDatainLong(PrimaData)) {
                    //in questo caso richiedo i 10 gg precedenti la data richiesta
                    //anche perchè in questo modo comincio a compilare la tabella dei cambi
                    String DataMeno10 = ConvertiDatadaLong(ConvertiDatainLong(Data) - 864000000);
                    if(RecuperaTassidiCambio(DataMeno10, PrimaData)!=null)risultato = ConvertiUSDEUR("1", Data);
                } else if (ConvertiDatainLong(Data) > ConvertiDatainLong(UltimaData)) {
                    //in questo caso richiedo i 180 gg successivi la data richiesta
                    //anche perchè in questo modo comincio a compilare la tabella dei cambi
                    String DataPiu10 = ConvertiDatadaLong(ConvertiDatainLong(Data) + 864000000);
                    if(RecuperaTassidiCambio(UltimaData, DataPiu10)!=null)risultato = ConvertiUSDEUR("1", Data);
                }
                //  risultato = "Fuori range di date";
            } else {
                risultato = null;//avviene solo quando la data non è compresa tra oggi e il primo gennaio 2018
            }
        } else {
            if (dateDisponibili.length == 0) {
                PrimaData = "2019-01-01";
                UltimaData = DatadiOggi;
                if(RecuperaTassidiCambio(PrimaData, UltimaData)!=null)risultato = ConvertiUSDEUR("1", Data);
            }
        }

        if (risultato != null) {
            risultato = String.valueOf(Double.parseDouble(Valore) * Double.parseDouble(risultato));
            return risultato;
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
    
    
    
    
    public static String ConvertiDatadaLong(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
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

