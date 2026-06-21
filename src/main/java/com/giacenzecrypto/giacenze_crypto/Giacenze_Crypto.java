/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

public class Giacenze_Crypto {

    private static final float DEFAULT_FONT_SIZE = 12f;
    private static final String DEFAULT_FONT_FAMILY = "Inter";

    public static void main(String[] args) throws UnsupportedLookAndFeelException {

        System.out.println("user.dir : " + System.getProperty("user.dir"));
        VarStatiche.setPathRisorse(getJarPath() + "/");

        float fontSize = DEFAULT_FONT_SIZE;
        String fontFamily = DEFAULT_FONT_FAMILY;

        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);

            if (args[i].equals("--debug")) {
                apriFinestraLog();
            }

            if (args[i].equals("--risorse") && i + 1 < args.length
                    && args[i + 1].charAt(args[i + 1].length() - 1) == '/') {
                VarStatiche.setPathRisorse(args[i + 1]);
            }

            if (args[i].equals("--NoJarPath")) {
                VarStatiche.setPathRisorse("./");
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

        if (!DatabaseH2.CreaoCollegaDatabase()) {
            JOptionPane.showConfirmDialog(
                    null,
                    "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!",
                    "Attenzione",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null
            );
            System.exit(0);
        }

        LoggerGC.init();

        Principale.tema = DatabaseH2.Opzioni_Leggi("Tema");
        if (Principale.tema == null) {
            Principale.tema = "Chiaro";
        }

        impostaFontGlobale(fontFamily, fontSize);

        if (Principale.tema.equalsIgnoreCase("Scuro")) {
            //FlatLaf.registerCustomDefaultsSource("Temi");
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

        Principale g = new Principale();
        g.pack();
        g.setLocationRelativeTo(null);
        g.setVisible(true);
    }

    private static void impostaFontGlobale(String fontFamily, float size) {
        try {
            Font uiFont = new Font(fontFamily, Font.PLAIN, Math.round(size));
            UIManager.put("defaultFont", new FontUIResource(uiFont));
            System.out.println("Font globale impostato: " + uiFont.getFontName() + " size=" + size);
        } catch (Exception ex) {
            ex.printStackTrace();
            Font fallback = new Font("SansSerif", Font.PLAIN, Math.round(size));
            UIManager.put("defaultFont", new FontUIResource(fallback));
            System.out.println("Errore impostazione font custom, uso fallback: " + fallback.getFontName());
        }
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

    private static void apriFinestraLog() {
        Download progress = new Download();
        progress.Titolo("Finestra dei Log");
        progress.SetLabel("Finestra dei Log");
        progress.NascondiInterrompi();
        progress.NascondiBarra();
        progress.NoModale();

        Thread thread = new Thread() {
            @Override
            public void run() {
            }
        };
        thread.start();

        progress.setVisible(true);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Giacenze_Crypto.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}