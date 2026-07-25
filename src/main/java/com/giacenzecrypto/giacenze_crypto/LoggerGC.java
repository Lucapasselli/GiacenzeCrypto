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

    /**
     * Inizializza il logger applicativo: rimuove gli handler di default della console, configura un
     * {@link FileHandler} rotativo su {@code GiacenzeCrypto.log} (5MB max, 3 backup, in append), e reindirizza
     * {@link System#out}/{@link System#err} verso il file di log (oltre che verso i loro stream originali).
     */
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
                    VarStatiche.getWorkingDirectory() + "GiacenzeCrypto.log",
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

    /**
     * Attiva l'inoltro di {@link System#out} verso un {@link JTextPane} (usato dalla finestra di log a video).
     * @param textPane componente su cui mostrare l'output
     */
    public static void enableTextPaneOut(JTextPane textPane) {
        textPaneOutStream = new TextPaneOutputStream(textPane);
    }
    /** Disattiva l'inoltro di {@link System#out} verso il {@link JTextPane} eventualmente attivato con {@link #enableTextPaneOut}. */
    public static void disableTextPaneOut() {
        textPaneOutStream = null;
    }

    /**
     * Attiva l'inoltro di {@link System#err} verso un {@link JTextPane} (usato dalla finestra di log a video).
     * @param textPane componente su cui mostrare l'output
     */
    public static void enableTextPaneErr(JTextPane textPane) {
        textPaneErrStream = new TextPaneOutputStream(textPane);
    }
    /** Disattiva l'inoltro di {@link System#err} verso il {@link JTextPane} eventualmente attivato con {@link #enableTextPaneErr}. */
    public static void disableTextPaneErr() {
        textPaneErrStream = null;
    }

    private static void redirectSystemStreams() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // STDOUT
        System.setOut(new PrintStream(new OutputStream() {
            /** Inoltra il byte allo stream originale e, se attivo, al {@link JTextPane} di output. */
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);
                //logger.info(String.valueOf((char) b));
                if (textPaneOutStream != null) textPaneOutStream.write(b);
            }
            /** Inoltra i byte allo stream originale, li registra nel log come {@code INFO} e, se attivo, li inoltra al {@link JTextPane} di output. */
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
            /** Inoltra il byte allo stream originale e, se attivo, al {@link JTextPane} di errore. */
            @Override
            public void write(int b) throws IOException {
                originalErr.write(b);
                //logger.severe(String.valueOf((char) b));
                if (textPaneErrStream != null) textPaneErrStream.write(b);
            }
            /** Inoltra i byte allo stream originale, li registra nel log come {@code SEVERE} e, se attivo, li inoltra al {@link JTextPane} di errore. */
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalErr.write(b, off, len);
                String msg = new String(b, off, len);
                logger.severe(msg.trim());
                if (textPaneErrStream != null) textPaneErrStream.write(b, off, len);
            }
        }, true));
    }

    /**
     * Registra un messaggio informativo, prefissato dal nome della funzione chiamante.
     * @param message messaggio da registrare
     * @param funzione nome della funzione/contesto chiamante, mostrato tra parentesi quadre
     */
    public static void logInfo(String message, String funzione) {
        logger.log(Level.INFO, "[{0}] {1}", new Object[]{funzione, message});
    }
     /** @param message messaggio informativo da registrare */
     public static void logInfo(String message) {
        logger.info(message);
    }

    /**
     * Registra un messaggio di errore con la relativa eccezione.
     * @param message messaggio descrittivo dell'errore
     * @param t eccezione associata
     */
    public static void logError(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    /**
     * Ricava nome classe e metodo a un dato livello dello stack trace del thread corrente, utile per
     * identificare il chiamante effettivo nei messaggi di errore generati da {@link #ScriviErrore}.
     * @param livello indice nello stack trace (0 = questo metodo, 1 = chiamante diretto, e così via)
     * @return la stringa {@code NomeClasse.nomeMetodo} corrispondente al livello indicato
     */
    public static String getCurrentClassAndMethod(int livello) {
    StackTraceElement element = Thread.currentThread().getStackTrace()[livello];
    return element.getClassName() + "." + element.getMethodName();
}
    /**
     * Stampa su {@link System#err} un messaggio di errore testuale, prefissato da classe/metodo del chiamante
     * e del chiamante del chiamante (utile per risalire rapidamente all'origine dell'errore nei log).
     * @param ex testo dell'errore da registrare
     */
    public static void ScriviErrore(String ex) {
        System.err.println("ERRORE in "+LoggerGC.getCurrentClassAndMethod(3)+" chiamato da "+getCurrentClassAndMethod(4)+"\n"+ex);
}
    /**
     * Come {@link #ScriviErrore(String)}, ma a partire da un'eccezione (ne stampa il solo messaggio, non lo stack trace).
     * @param ex eccezione da registrare
     */
    public static void ScriviErrore(Throwable ex) {
        System.err.println("ERRORE in "+LoggerGC.getCurrentClassAndMethod(3)+" chiamato da "+getCurrentClassAndMethod(4)+"\n"+ex.getMessage());
}


    /** Chiude il {@link FileHandler} del log, se inizializzato. */
    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }

    public static class SimpleMessageFormatter extends Formatter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /** @return la riga di log formattata come {@code [yyyy-MM-dd HH:mm:ss] [LIVELLO] messaggio} */
        @Override
        public String format(LogRecord record) {
            String timestamp = sdf.format(new Date(record.getMillis()));
            return "[" + timestamp + "] [" + record.getLevel() + "] " + record.getMessage() + System.lineSeparator();
        }
    }




}





