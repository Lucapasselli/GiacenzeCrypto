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

    /**
     * Formattatori riusati, uno per pattern e per thread.
     * <p>
     * Prima ogni conversione costruiva un nuovo {@link SimpleDateFormat} (più la ricerca del
     * {@link TimeZone}), e queste funzioni sono chiamate <b>una volta per movimento</b> dal motore
     * delle plusvalenze e dal caricamento della tabella: su 101.000 movimenti la sola costruzione
     * dei formattatori pesava più del parsing. Riusarli taglia circa il 70% del costo
     * (vedi {@code Documentazione/Analisi_Performance_Caricamento.md}).
     * <p>
     * <b>Perché {@link ThreadLocal} e non un semplice {@code static final}</b>: {@code SimpleDateFormat}
     * <b>non è thread-safe</b> e queste funzioni sono invocate sia dall'EDT sia dai thread di import
     * e di ricalcolo. Un'istanza condivisa produrrebbe date sbagliate in modo intermittente e non
     * riproducibile — molto peggio del costo che elimina.
     * <p>
     * Tutti fissano <b>Europe/Rome</b>, come faceva il codice sostituito.
     */
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA = formattatoreRoma("yyyy-MM-dd");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA_MINUTO = formattatoreRoma("yyyy-MM-dd HH:mm");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA_SECONDO = formattatoreRoma("yyyy-MM-dd HH:mm:ss");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA_ORA = formattatoreRoma("yyyy-MM-dd HH");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA_ID = formattatoreRoma("yyyyMMddHHmmss");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATA_GIORNO = formattatoreRoma("yyyyMMdd");

    /**
     * @param Data1 data/ora in millisecondi epoch
     * @return il giorno di calendario di {@code Data1} come intero {@code yyyyMMdd}, fuso Europe/Rome
     */
    public static int GiornoIntGG(long Data1) {
        return Integer.parseInt(SDF_DATA_GIORNO.get().format(new Date(Data1)));
    }

    /**
     * @param Data1 data/ora in millisecondi epoch
     * @return i millisecondi epoch della mezzanotte (Europe/Rome) che apre il giorno di {@code Data1}
     */
    public static long InizioGiornoRoma(long Data1) {
        SimpleDateFormat f = SDF_DATA.get();
        try {
            return f.parse(f.format(new Date(Data1))).getTime();
        } catch (ParseException ex) {
            //format e parse usano lo stesso pattern: non dovrebbe mai accadere
            return Data1;
        }
    }

    /**
     * Crea il contenitore per thread di un {@link SimpleDateFormat} sul fuso Europe/Rome.
     * @param pattern pattern di formattazione/parsing
     * @return il formattatore, uno per thread
     */
    private static ThreadLocal<SimpleDateFormat> formattatoreRoma(String pattern) {
        return ThreadLocal.withInitial(() -> {
            SimpleDateFormat f = new SimpleDateFormat(pattern);
            f.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
            return f;
        });
    }

        /**
         * @param Data1 data/ora in millisecondi epoch
         * @return la data formattata come {@code yyyy-MM-dd}, nel fuso orario Europe/Rome
         */
        public static String ConvertiDatadaLong(long Data1) {

  
            SimpleDateFormat f = SDF_DATA.get();
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
    
        /**
         * @param Data1 data/ora in millisecondi epoch
         * @return la data/ora formattata come {@code yyyy-MM-dd HH:mm:ss}, nel fuso orario Europe/Rome
         */
        public static String ConvertiDatadaLongAlSecondo(long Data1) {

  
            SimpleDateFormat f = SDF_DATA_SECONDO.get();
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
        
        /**
         * @param unixTimestamp data/ora in millisecondi epoch
         * @return la data/ora formattata secondo lo standard ISO 8601 ({@link DateTimeFormatter#ISO_INSTANT})
         */
        public static String ConvertiUnixTimestampToIso(long unixTimestamp) {
            // Crea un oggetto Instant a partire dal timestamp fornito
            Instant instant = Instant.ofEpochMilli(unixTimestamp);
            // Format ISO 8601 utilizzando DateTimeFormatter.ISO_INSTANT
            System.out.print(DateTimeFormatter.ISO_INSTANT.format(instant));
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        }
    
            /**
             * Calcola la differenza in giorni interi tra due date (solo la parte data, l'eventuale ora viene ignorata).
             * @param DataInizio data iniziale, formato {@code yyyy-MM-dd} (con eventuale ora aggiuntiva, ignorata)
             * @param DataFine data finale, formato {@code yyyy-MM-dd} (con eventuale ora aggiuntiva, ignorata)
             * @return il numero di giorni tra {@code DataInizio} e {@code DataFine}
             */
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
    
        /**
         * @param Data1 data/ora in millisecondi epoch
         * @return la data/ora formattata come {@code yyyy-MM-dd HH} (precisione oraria), nel fuso orario Europe/Rome
         */
        public static String ConvertiDatadaLongallOra(long Data1) {

  
            SimpleDateFormat f = SDF_DATA_ORA.get();
            Date d = new Date(Data1);
            //d=f.format(d);
            String m1=f.format(d);
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni

        return m1;
    } 
             
        /**
         * @param Data1 data di riferimento, formato {@code yyyy-MM-dd}
         * @return la data del giorno precedente, formato {@code yyyy-MM-dd}, oppure {@code ""} se {@code Data1} non è parsabile
         */
        public static String GiornoMenoUno(String Data1) {
        String giorno="";
        try {
            SimpleDateFormat f = SDF_DATA_MINUTO.get();
            Date d = f.parse(Data1+" 00:00");
            long m1 = d.getTime();
            long giornomenouno=m1-86400000;
            SimpleDateFormat f2 = SDF_DATA.get();
            Date d1 = new Date(giornomenouno);
            giorno=f2.format(d1);
        } catch (ParseException ex) {
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
        }
        return giorno;
    }
        
            /**
             * @param Data1 data, formato {@code yyyy-MM-dd}
             * @return i millisecondi epoch (mezzanotte, fuso orario Europe/Rome) corrispondenti, oppure {@code 0} se {@code Data1} non è parsabile (errore loggato)
             */
            public static long ConvertiDatainLong(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = SDF_DATA.get();
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
            LoggerGC.ScriviErrore(Data1+" non è una data valida");
        }
        return m1;
    } 
     
        /**
         * @param Data1 data/ora, formato {@code yyyy-MM-dd HH:mm}
         * @return i millisecondi epoch (fuso orario Europe/Rome) corrispondenti, oppure {@code 0} se {@code Data1} non è parsabile (errore loggato)
         */
        public static long ConvertiDatainLongMinuto(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = SDF_DATA_MINUTO.get();
            Date d = f.parse(Data1);
            m1 = d.getTime();
            
            //System.out.println((m1-m2)/1000/3600/24);// questa è la differenza in giorni
        } catch (ParseException ex) {
           // Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println(Data1+" non è una data");
            LoggerGC.ScriviErrore(Data1+" non è una data valida");
        }
        return m1;
    } 
        
    /**
     * Converte una data/ora al secondo in millisecondi epoch (fuso orario Europe/Rome), normalizzando anche
     * anni a 2 cifre (es. {@code "25-..."} → {@code "2025-..."}) e gestendo automaticamente le ore inesistenti
     * per effetto del cambio ora legale/solare (spostate in avanti).
     * @param Data1 data/ora, formato {@code yyyy-MM-dd HH:mm:ss} (o {@code yy-MM-dd HH:mm:ss})
     * @return i millisecondi epoch corrispondenti, oppure {@code 0} se {@code Data1} non è parsabile
     */
    public static long ConvertiDatainLongSecondo(String Data1) {
    try {
        String dataDaParsare = Data1;
        if (Data1 != null && Data1.length() > 2 && Data1.charAt(2) == '-') {
            dataDaParsare = "20" + Data1;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId rome = ZoneId.of("Europe/Rome");

        LocalDateTime ldt = LocalDateTime.parse(dataDaParsare, fmt);

        // In caso di ora "inesistente" (gap DST), sposta in avanti automaticamente
        ZonedDateTime zdt = ldt.atZone(rome);

        return zdt.toInstant().toEpochMilli();
    } catch (Exception ex) {
       //System.out.println(ex.toString());
       //ritorna 0 in caso di data non valida
        return 0;
    }
}
        
    //l'offset indica il fuso orario rispetto a UTC es. offset 1 indica che sto utilizzando UTC+1 come fuso    
    /**
     * Converte una data/ora al secondo in millisecondi epoch, applicando un fuso orario UTC con offset esplicito
     * (invece del fuso Europe/Rome usato dagli altri metodi di conversione), normalizzando anche anni a 2 cifre.
     * @param Data1 data/ora, formato {@code yyyy-MM-dd HH:mm:ss} (o {@code yy-MM-dd HH:mm:ss})
     * @param offset offset del fuso rispetto a UTC in ore (es. {@code 1} per UTC+1)
     * @return i millisecondi epoch corrispondenti
     */
    public static long ConvertiDatainLongSecondoUTC2(String Data1,int offset) {
            // Rimuove il suffisso " UTC+2"
       DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

       String dataDaParsare = Data1;
       
       if (Data1 != null && Data1.length() > 2 && Data1.charAt(2) == '-') {
            dataDaParsare = "20" + Data1;
        }
       //System.out.println(dataDaParsare);
        // Parsa la data e applica l'offset UTC+2
        LocalDateTime ldt = LocalDateTime.parse(dataDaParsare, FORMATTER);

        return ldt.toEpochSecond(ZoneOffset.ofHours(offset))*1000;
    } 
        
    /**
     * @param Data1 timestamp nel formato usato come prefisso degli ID movimento, {@code yyyyMMddHHmmss}
     * @return i millisecondi epoch (fuso orario Europe/Rome) corrispondenti, oppure {@code 0} se {@code Data1} non è parsabile (errore loggato)
     */
    public static long ConvertiDataIDinLong(String Data1) {
           long m1=0;
        try {
            SimpleDateFormat f = SDF_DATA_ID.get();
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
        
        
        /**
         * @param isoDate data/ora in formato ISO 8601 (es. {@code "2024-07-22T01:53:29.000Z"})
         * @return i millisecondi epoch corrispondenti, oppure {@code 0L} se {@code isoDate} è {@code null}/vuota o non parsabile
         */
        public static long ConvertiISO8601toMillis(String isoDate) {
    if (isoDate == null || isoDate.isEmpty()) {
        return 0L;
    }

    try {
        // Parsing ISO 8601 (es: "2024-07-22T01:53:29.000Z")
        Instant instant = Instant.parse(isoDate);
        return instant.toEpochMilli();

    } catch (Exception e) {
      //  LoggerGC.ScriviErrore(isoDate+" non è una data valida");
        e.printStackTrace();
        return 0L;
    }
}
        
        /**
         * Converte una data/ora espressa in UTC (in uno dei formati con ora a una/due cifre, con o senza
         * millisecondi) nel fuso orario Europe/Rome, restituendola nel formato standard interno {@code yyyy-MM-dd HH:mm:ss}.
         * @param Data data/ora in UTC da convertire
         * @return la data/ora convertita, oppure {@code null} se non corrisponde a nessuno dei formati supportati
         */
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
        
    /**
     * Converte una data nel formato specifico usato dal Binance Tax Report (es. {@code "2023-01-01-01:00:00"},
     * fuso orario CET) in millisecondi epoch.
     * @param Data data nel formato Binance Tax Report ({@code yyyy-MM-dd-HH:mm:ss})
     * @return i millisecondi epoch corrispondenti, oppure {@code 0} se {@code Data} non è nel formato atteso
     */
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
            
        /**
         * Converte una data nel formato di export di CoinTracking ({@code dd.MM.yyyy HH:mm[:ss]}) nel formato
         * standard interno {@code yyyy-MM-dd HH:mm:ss}. Se {@code Data} contiene già più di 2 gruppi separati
         * da {@code :} e nessun punto (già nel formato con secondi, non CoinTracking), viene restituita invariata.
         * @param Data data nel formato CoinTracking (o già normalizzata)
         * @return la data convertita, oppure {@code ""} se {@code Data} non è nel formato atteso
         */
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
