/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

public class Giacenze_Crypto {

    private static final float DEFAULT_FONT_SIZE = 12f;
    /** Il font incluso nel jar: vedi {@link FontApplicazione} per il perché non è un nome di sistema. */
    private static final String DEFAULT_FONT_FAMILY = FontApplicazione.FAMIGLIA;

    /**
     * Punto di ingresso dell'applicazione: interpreta gli argomenti a riga di comando (vedi {@code CLAUDE.md}
     * per l'elenco completo — {@code --NoJarPath}, {@code --workdir}, {@code --workInRisorse}, {@code --risorse},
     * {@code --debug}, {@code --fontSize}, {@code --fontFamily}), imposta i percorsi in {@link VarStatiche},
     * applica il tema FlatLaf e le impostazioni di font globali, quindi apre la finestra {@link Principale}.
     * @param args argomenti a riga di comando
     */
    public static void main(String[] args) {

        System.out.println("user.dir : " + System.getProperty("user.dir"));
        VarStatiche.setPathRisorse(getJarPath() + "/");

        //Prima passata: solo gli argomenti che servono a localizzare il logo. Tutto il resto (--debug,
        //--workdir, i font...) è rimandato alla seconda passata, perché lo splash deve comparire il
        //prima possibile e queste opzioni costano tempo (attese, creazione di cartelle, dialoghi).
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--risorse") && i + 1 < args.length
                    && args[i + 1].charAt(args[i + 1].length() - 1) == '/') {
                VarStatiche.setPathRisorse(args[i + 1]);
            }

            if (args[i].equals("--NoJarPath")) {
                VarStatiche.setPathRisorse("./");
            }
        }

        //Prima di qualunque cosa vada a video: lo splash disegna il proprio testo da sé, con il font
        //dell'applicazione, e la finestra principale lo eredita dal Look&Feel più avanti.
        FontApplicazione.Registra();

        //Prima operazione visibile del programma: da qui in poi l'utente ha qualcosa a video mentre
        //proseguono l'apertura del database e la costruzione della finestra principale.
        SplashAvvio.mostra();

        float fontSize = DEFAULT_FONT_SIZE;
        String fontFamily = DEFAULT_FONT_FAMILY;

        //Seconda passata: tutti gli altri argomenti.
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);

            if (args[i].equals("--debug")) {
                apriFinestraLog();
            }

            if (args[i].equalsIgnoreCase("--workdir") && i + 1 < args.length
                    && args[i + 1].charAt(args[i + 1].length() - 1) == '/') {
                setWorkDir(args[i + 1]);
            }

            if (args[i].equalsIgnoreCase("--workInRisorse")) {
                setWorkDir(VarStatiche.getPathRisorse());
            }

            if (args[i].equalsIgnoreCase("--fontSize") && i + 1 < args.length) {
                try {
                    fontSize = Float.parseFloat(args[i + 1].replace(",", "."));
                } catch (NumberFormatException ex) {
                    System.err.println("Valore non valido per --fontSize: " + args[i + 1]);
                }
            }

            if (args[i].equalsIgnoreCase("--fontFamily") && i + 1 < args.length) {
                fontFamily = args[i + 1];
            }
        }

        System.out.println("Path Risorse : " + VarStatiche.getPathRisorse());
        System.out.println("Working Directory : " + VarStatiche.getWorkingDirectory());

        //Da qui in poi la barra dello splash segue le fasi dell'avvio. La prima fase va dichiarata
        //dopo la lettura degli argomenti: solo adesso la directory di lavoro è quella definitiva, ed
        //è da lì che SplashAvvio legge i tempi dell'avvio precedente per pesare la barra.
        SplashAvvio.fase(SplashAvvio.Fase.DATABASE);

        if (!DatabaseH2.CreaoCollegaDatabase()) {
            //Lo splash va chiuso prima del dialogo, altrimenti resterebbe sospeso sopra di esso.
            SplashAvvio.chiudi();
            //C5: distingue il lock da un'altra sessione (H2 error code 90020) da qualsiasi altro
            //errore di connessione (DB corrotto, disco pieno, versione H2 incompatibile...), che
            //altrimenti veniva mascherato dallo stesso messaggio fuorviante
            String messaggio;
            if (DatabaseH2.isErroreDatabaseGiaAperto()) {
                messaggio = "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!";
            } else {
                messaggio = "Attenzione, non è stato possibile aprire il database, questa sessione verrà terminata!!\n"
                        + "Dettaglio errore: " + DatabaseH2.getUltimaEccezioneConnessione();
            }
            JOptionPane.showConfirmDialog(
                    null,
                    messaggio,
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(0);
        }

        //Da qui in poi lo splash è a video: se il main thread muore per un'eccezione non c'è più nessuno
        //che possa chiuderlo e, siccome una JWindow visibile tiene viva la JVM, il programma resterebbe
        //appeso su uno splash non chiudibile. Prima di questa modifica non esisteva ancora nessuna
        //finestra e un'eccezione qui terminava semplicemente il processo.
        try {
            SplashAvvio.fase(SplashAvvio.Fase.AMBIENTE);
            LoggerGC.init();

            Principale.tema = DatabaseH2.Opzioni_Leggi("Tema");
            if (Principale.tema == null) {
                Principale.tema = "Chiaro";
            }

            impostaFontGlobale(fontFamily, fontSize);
        } catch (Throwable t) {
            SplashAvvio.chiudi();
            t.printStackTrace();
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione, errore durante l'avvio del programma, questa sessione verrà terminata!!\n"
                            + "Dettaglio errore: " + t,
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(1);
        }

        //A6: la costruzione della GUI Swing va fatta sull'Event Dispatch Thread, non sul main thread
        SwingUtilities.invokeLater(() -> {
            try {
                //Va registrata PRIMA di installare il Look&Feel, e una volta sola: è un registro
                //statico globale di FlatLaf, quindi vale anche per il cambio tema a caldo di
                //Principale. Carica src/main/resources/Temi/FlatDarkLaf.properties, che alza il
                //contrasto rispetto al tema di serie (sfondo #3c3f41 e testo #bbb, grigio su grigio).
                FlatLaf.registerCustomDefaultsSource("Temi");
                if (Principale.tema.equalsIgnoreCase("Scuro")) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    Tabelle.verdeScuro = new Color(145, 255, 143);
                    Tabelle.rosso = new Color(255, 133, 133);
                    Tabelle.Rosso = "FFA07A";
                    Tabelle.Verde = "9ACD32";
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    Tabelle.verdeScuro = new Color(23, 114, 69);
                    Tabelle.rosso = new Color(255, 100, 100);
                }
                //Le icone dell'applicazione sono nere: sullo sfondo scuro vanno ridisegnate in chiaro
                Icone.InizializzaTema(Principale.tema.equalsIgnoreCase("Scuro"));
            } catch (UnsupportedLookAndFeelException ex) {
                LoggerGC.ScriviErrore(ex);
            }

            //Il finally garantisce che lo splash sparisca anche se la costruzione della finestra
            //principale fallisce (il costruttore di Principale intercetta e ingoia le eccezioni):
            //senza, resterebbe a video uno splash impossibile da chiudere.
            try {
                Principale g = new Principale();

                String finestraLarghezza = DatabaseH2.Opzioni_Leggi("Finestra_Larghezza");
                String finestraAltezza = DatabaseH2.Opzioni_Leggi("Finestra_Altezza");
                String finestraMassimizzata = DatabaseH2.Opzioni_Leggi("Finestra_Massimizzata");

                if (finestraLarghezza != null && finestraAltezza != null) {
                    try {
                        g.setSize(Integer.parseInt(finestraLarghezza), Integer.parseInt(finestraAltezza));
                    } catch (NumberFormatException ex) {
                        g.pack();
                    }
                } else {
                    g.pack();
                }

                g.setLocationRelativeTo(null);

                if ("true".equalsIgnoreCase(finestraMassimizzata)) {
                    g.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
                }

                g.setVisible(true);
            } finally {
                SplashAvvio.chiudi();
            }
        });
    }

    private static void impostaFontGlobale(String fontFamily, float size) {
        Font uiFont = new Font(fontFamily, Font.PLAIN, Math.round(size));

        //new Font() su una famiglia assente non lancia niente: ripiega in silenzio sul font logico
        //Dialog, che il sistema risolve come gli pare. Era esattamente il difetto che FontApplicazione
        //è nata per chiudere, quindi qui va reso visibile invece che intercettato con un catch che
        //non scatta mai. Non è un errore fatale: succede solo con un --fontFamily inesistente.
        if (!fontFamily.equalsIgnoreCase(uiFont.getFamily(java.util.Locale.ROOT))) {
            System.err.println("Attenzione: il font '" + fontFamily + "' non è disponibile, "
                    + "il sistema userà al suo posto '" + uiFont.getFamily(java.util.Locale.ROOT) + "'");
        }

        UIManager.put("defaultFont", new FontUIResource(uiFont));
        System.out.println("Font globale impostato: " + uiFont.getFontName() + " size=" + size);
    }

    private static String getHomeUtente() {
        String home = System.getProperty("user.home");
        System.out.println("Trovata Home user.home : " + home);
        if (home == null || home.isBlank()) {
            home = System.getenv("HOME");
            System.out.println("Trovata Home HOME : " + home);
        }
        if (home == null || home.isBlank()) {
            home = new File(".").getAbsolutePath();
            System.out.println("Home non trovata : " + home);
        }
        return home;
    }

    private static void setWorkDir(String workingDir) {

        workingDir = workingDir.replace("HOME", getHomeUtente());

        File dir = new File(workingDir);

        if (dir.getPath().contains("..") || dir.getPath().contains(";") || dir.getPath().contains("|")) {
            SplashAvvio.chiudi();
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione! il parametro passato a --workdir '" + workingDir + "' non è valido!\n Il Programma verrà terminato.",
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(1);
        }

        if (dir.isFile()) {
            SplashAvvio.chiudi();
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione! il parametro passato a --workdir '" + workingDir + "' non è un file e non una directory!\n Il Programma verrà terminato.",
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(1);
        }

        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                SplashAvvio.chiudi();
                JOptionPane.showConfirmDialog(
                        null,
                        "Attenzione! non è possibile creare la directory specificata : '" + workingDir + "'\n Il Programma verrà terminato.",
                        "Attenzione",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null
                );
                System.exit(1);
            }
        }

        if (!dir.canWrite()) {
            System.err.println("La directory specificata non è scrivibile: " + dir.getAbsolutePath());
            SplashAvvio.chiudi();
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione! La directory specificata : '" + workingDir + "' non è scrivibile\n Il Programma verrà terminato.",
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(1);
        }

        VarStatiche.setWorkingDirectory(workingDir);
    }

    private static String getJarPath() {
        try {
            return new File(
                    Funzioni.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParentFile().toPath().toString();
        } catch (URISyntaxException e) {
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione! non è possibile recuperare la path del file Jar principale!\n Il Programma verrà terminato.",
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(1);
            return null;
        }
    }

    /**
     * Apre la finestra dei log dell'opzione {@code --debug}, e ritorna solo quando è a video:
     * quello che l'opzione serve a mostrare è il log dell'avvio che segue (connessione al
     * database, tema, font, costruzione della finestra principale).
     */
    private static void apriFinestraLog() {
        try {
            //A6: Download è un JDialog, quindi va costruito e mostrato sull'Event Dispatch Thread.
            //Qui siamo nel ciclo di lettura degli argomenti di main(), cioè sul main thread e
            //prima dell'invokeLater che avvolge il resto della GUI: senza questo blocco l'intera
            //costruzione avveniva fuori dall'EDT. Residuo sfuggito alla correzione A6 originale.
            //
            //invokeAndWait e non invokeLater: con invokeLater main() proseguirebbe subito e la
            //finestra potrebbe aprirsi dopo che l'avvio ha già prodotto il log da mostrare.
            //Non blocca a lungo perché NoModale() rende il dialogo MODELESS: se non lo fosse
            //(Download nasce modale) setVisible bloccherebbe l'EDT e l'avvio si pianterebbe.
            SwingUtilities.invokeAndWait(() -> {
                Download progress = new Download();
                progress.Titolo("Finestra dei Log");
                progress.SetLabel("Finestra dei Log");
                progress.NascondiInterrompi();
                progress.NascondiBarra();
                progress.NoModale();
                progress.setVisible(true);
            });
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LoggerGC.ScriviErrore(ex);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            //LoggerGC.init() non è ancora stato chiamato: ScriviErrore scrive comunque su System.err
            LoggerGC.ScriviErrore(ex);
        }
    }
}