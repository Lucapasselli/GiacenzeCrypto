package com.giacenzecrypto.giacenze_crypto;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;
import javax.swing.JTextPane;

public class LoggerGC {
    private static final Logger logger = Logger.getLogger(LoggerGC.class.getName());
    private static FileHandler fileHandler;

    // Stream opzionali separati per out e err
    private static OutputStream textPaneOutStream = null;
    private static OutputStream textPaneErrStream = null;

    public static void init() {
        try {
            // Rimuove gli handler predefiniti della console di java.util.logging
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }

            // Log rotativo: 5 MB max, 3 file di backup, append attivo
            fileHandler = new FileHandler(
                    Statiche.getWorkingDirectory() + "GiacenzeCrypto.log",
                    5 * 1024 * 1024,
                    3,
                    true
            );
            fileHandler.setFormatter(new SimpleMessageFormatter());
            fileHandler.setLevel(Level.ALL);

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);

            // Reindirizza System.out e System.err
            redirectSystemStreams();

        } catch (IOException e) {
            System.err.println("Errore nell'inizializzazione del logger: " + e.getMessage());
        }
    }

    // Attiva/disattiva TextPane per System.out
    public static void enableTextPaneOut(JTextPane textPane) {
        textPaneOutStream = new TextPaneOutputStream(textPane);
    }
    public static void disableTextPaneOut() {
        textPaneOutStream = null;
    }

    // Attiva/disattiva TextPane per System.err
    public static void enableTextPaneErr(JTextPane textPane) {
        textPaneErrStream = new TextPaneOutputStream(textPane);
    }
    public static void disableTextPaneErr() {
        textPaneErrStream = null;
    }

    private static void redirectSystemStreams() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // STDOUT
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);
                //logger.info(String.valueOf((char) b));
                if (textPaneOutStream != null) textPaneOutStream.write(b);
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalOut.write(b, off, len);
                String msg = new String(b, off, len);
                logger.info(msg.trim());
                if (textPaneOutStream != null) textPaneOutStream.write(b, off, len);
            }
        }, true));

        // STDERR
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalErr.write(b);
                //logger.severe(String.valueOf((char) b));
                if (textPaneErrStream != null) textPaneErrStream.write(b);
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalErr.write(b, off, len);
                String msg = new String(b, off, len);
                logger.severe(msg.trim());
                if (textPaneErrStream != null) textPaneErrStream.write(b, off, len);
            }
        }, true));
    }

    public static void logInfo(String message, String funzione) {
        logger.log(Level.INFO, "[{0}] {1}", new Object[]{funzione, message});
    }
     public static void logInfo(String message) {
        logger.info(message);
    }

    public static void logError(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }
    
    public static String getCurrentClassAndMethod(int livello) {
    StackTraceElement element = Thread.currentThread().getStackTrace()[livello];
    return element.getClassName() + "." + element.getMethodName();
}
    public static void ScriviErrore(String ex) {
        System.err.println("ERRORE in "+LoggerGC.getCurrentClassAndMethod(3)+" chiamato da "+getCurrentClassAndMethod(4)+"\n"+ex);
}
    public static void ScriviErrore(Throwable ex) {
        System.err.println("ERRORE in "+LoggerGC.getCurrentClassAndMethod(3)+" chiamato da "+getCurrentClassAndMethod(4)+"\n"+ex.getMessage());
}


    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }

    public static class SimpleMessageFormatter extends Formatter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @Override
        public String format(LogRecord record) {
            String timestamp = sdf.format(new Date(record.getMillis()));
            return "[" + timestamp + "] [" + record.getLevel() + "] " + record.getMessage() + System.lineSeparator();
        }
    }




}





