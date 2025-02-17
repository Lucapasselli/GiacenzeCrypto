package giacenze_crypto.com;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
public class OperazioniSuDate {
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
        
                public static long ConvertiDatainLongSecondo(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
        }
        return m1;
    } 
}
