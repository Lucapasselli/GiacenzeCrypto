/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author lucap
 */
public class LoggerGC {
    private static final Logger logger = Logger.getLogger(LoggerGC.class.getName());
     private static FileHandler fileHandler;

    // Inizializzazione una sola volta all'avvio
    public static void init() {
        try {
            // Rimuove eventuali handler predefiniti
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }

            // Log rotativo: 5 MB max, 3 file di backup, append attivo
            fileHandler = new FileHandler(Statiche.getWorkingDirectory()+"GiacenzeCrypto.log", 5 * 1024 * 1024, 3, true);
            fileHandler.setFormatter(new SimpleMessageFormatter());
            //fileHandler.setFormatter(new SimpleFormatter()); // oppure custom Formatter
            fileHandler.setLevel(Level.ALL);

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false); // evita doppia stampa su console
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Errore nell'inizializzazione del logger: " + e.getMessage());
        }
    }

    // Metodo per loggare ovunque nel programma
    public static void logInfo(String message,String funzione) {
        logger.log(Level.INFO, "[{0}] {1}", new Object[]{funzione, message});
    }
    
    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
    
    public static void logError(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }
    
    public static class SimpleMessageFormatter extends Formatter {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        String timestamp = sdf.format(new Date(record.getMillis()));
        return "[" + timestamp + "] " + record.getMessage() + System.lineSeparator();
    }
}
}
