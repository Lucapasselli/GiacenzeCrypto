package com.giacenzecrypto.giacenze_crypto;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author luca.passelli
 */
public class FunzioniDate {
        public static String ConvertiDatadaLong(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
    
        public static String ConvertiDatadaLongAlSecondo(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
        
        public static String ConvertiUnixTimestampToIso(long unixTimestamp) {
            // Crea un oggetto Instant a partire dal timestamp fornito
            Instant instant = Instant.ofEpochMilli(unixTimestamp);
            // Format ISO 8601 utilizzando DateTimeFormatter.ISO_INSTANT
            System.out.print(DateTimeFormatter.ISO_INSTANT.format(instant));
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        }
    
            public static int DifferenzaDate(String DataInizio,String DataFine)   {
                //Il Formato della data deve essere es. 2023-02-15
                //System.out.println(DataInizio);
                //System.out.println(DataFine);
                BigDecimal DataInizioBigD=new BigDecimal(ConvertiDatainLong(DataInizio.split(" ")[0]));
                BigDecimal DataFineBigD=new BigDecimal(ConvertiDatainLong(DataFine.split(" ")[0]));
                String DiffData = (DataFineBigD.subtract(DataInizioBigD)).divide(new BigDecimal(86400000),0,RoundingMode.HALF_UP).toPlainString();
                //System.out.println(DiffData);
                //System.out.println("--------");
                return Integer.parseInt(DiffData);
 }  
    
        public static String ConvertiDatadaLongallOra(long Data1) {

  
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH");
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
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
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d = f.parse(Data1+" 00:00");
            long m1 = d.getTime();
            long giornomenouno=m1-86400000;            
            SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd");
            f2.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d1 = new Date(giornomenouno);
            giorno=f2.format(d1);
        } catch (ParseException ex) {
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
        }
        return giorno;
    }
        
            public static long ConvertiDatainLong(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
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
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
        }
        return m1;
    } 
        
        public static long ConvertiDatainLongSecondo(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
            LoggerGC.ScriviErrore(Data1+" non è una data valida");
           // return 0;
        }
        return m1;
    } 
        
        
        
        
        public static long ConvertiISO8601toMillis(String isoDate) {
    if (isoDate == null || isoDate.isEmpty()) {
        return 0L;
    }

    try {
        // Parsing ISO 8601 (es: "2024-07-22T01:53:29.000Z")
        Instant instant = Instant.parse(isoDate);
        return instant.toEpochMilli();

    } catch (Exception e) {
        e.printStackTrace();
        return 0L;
    }
}
        
        public static String Formatta_Data_UTC(String Data) {

            //come prima cosa controllo che l'ora abbia effettivamente 2 caratteri per quanto riguarda le ore
            //può capitare infatti che l'ra sia 9:36:11 al posto di 09:36:11
            // Elenco di formati possibili
            String[] FormatiPossibili = {
            "yyyy-MM-dd HH:mm:ss",  // Formato con ora a una cifra
            "yyyy-MM-dd H:mm:ss" , // Formato con ora a due cifre
            "yyyy-MM-dd H:mm:ss.SSS"  // Formato con i millisecondi
            };
            LocalDateTime localDateTime = null;

        // Prova ciascun formato fino a trovare quello giusto
        for (String format : FormatiPossibili) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                localDateTime = LocalDateTime.parse(Data, formatter);
                break; // Se il parsing riesce, esci dal ciclo
            } catch (DateTimeParseException e) {
                // Ignora e prova il prossimo formato
            }
        }
        if (localDateTime != null) {
            DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return localDateTime
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.of("Europe/Rome"))
            .format(formatterOutput);
        }else return null;
        
    }    
        
    public static long ConvertiDataBinanceTaxReportinLong(String Data) {
        //La data di Binance Tax Report è in questo formato 2023-01-01-01:00:00
        //ed è in orario CET, devo convertirla nel formato standard ovvero 2023-01-01 01:00:00
        //in più devo fare in modo che l'orario ia quello di Roma quindi deve comprendere anche il duso orario
        try {
        Data=Data + " CET";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss z", Locale.ENGLISH);

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(Data, formatter);

        // Ottieni il timestamp (UTC)
        long unixTimestamp = zonedDateTime.toInstant().getEpochSecond()*1000;
        return unixTimestamp;
        }catch (Exception ex) {
            //ritorna 0 se il formato della data è errato
            return 0;
        }
    }    
            
        public static String Formatta_Data_CoinTracking(String Data) {

        if (Data.split(":").length>2&&!Data.contains(".")) return Data;
            String DataFormattata="";
            try {
            SimpleDateFormat originale = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            Date d;
            if (Data.split(":").length<3){//Quindi se gli mancano i secondi
                d = originale.parse(Data+":00");
            }else d=originale.parse(Data);
            originale.applyPattern("yyyy-MM-dd HH:mm:ss");
            DataFormattata = originale.format(d);
        } catch (ParseException ex) {
           // LoggerGC.ScriviErrore(ex);
          //  Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            return DataFormattata;
        }
           // System.out.println(newDateString);
            return DataFormattata;
    }
        
        
}
