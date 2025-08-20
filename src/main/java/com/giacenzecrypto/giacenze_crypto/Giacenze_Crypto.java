/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author luca.passelli
 */
public class Giacenze_Crypto {
    

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        
        
         
       System.out.println("user.dir : "+System.getProperty("user.dir"));
       Statiche.setPathRisorse(getJarPath()+"/");

        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            if (args[i].equals("--debug")) {
                apriFinestraLog();
            }
            if (args[i].equals("--risorse") && i + 1 < args.length && args[i+1].charAt(args[i+1].length() - 1) == '/') {
                Statiche.setPathRisorse(args[i + 1]);
            }
            if (args[i].equals("--NoJarPath")) {
                Statiche.setPathRisorse("./");
            }
            if (args[i].equalsIgnoreCase("--workdir") && i + 1 < args.length && args[i + 1].charAt(args[i + 1].length() - 1) == '/') {
                setWorkDir(args[i + 1]);               
            }
            if (args[i].equalsIgnoreCase("--workInRisorse")) {
                setWorkDir(Statiche.getPathRisorse());               
            }
        }
        System.out.println("Path Risorse : "+Statiche.getPathRisorse()); 
        System.out.println("Working Directory : "+Statiche.getWorkingDirectory());      
        
        if (!DatabaseH2.CreaoCollegaDatabase()){
            JOptionPane.showConfirmDialog(null, "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            System.exit(0);
        }
        LoggerGC.init(); 
        // Flusso su console (System.err originale)
       /* PrintStream consoleErr = System.err;

        OutputStream logStream = new OutputStream() {
            @Override
            public void write(int b) {
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String msg = new String(b, off, len);
                LoggerGC.logError(msg, null);
            }
        };
        System.setErr(new PrintStream(new OutputStreamLog(consoleErr, logStream), true));*/
        
        CDC_Grafica.tema=DatabaseH2.Opzioni_Leggi("Tema");
        if (CDC_Grafica.tema==null)CDC_Grafica.tema="Chiaro";
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro"))
        {
            UIManager.setLookAndFeel( new FlatDarkLaf() );
            Tabelle.verdeScuro=new Color (145, 255, 143);
            Tabelle.rosso=new Color(255, 133, 133);
            Tabelle.Rosso="FFA07A";
            Tabelle.Verde="9ACD32";
        }else 
            {        
            UIManager.setLookAndFeel( new FlatLightLaf() );
            Tabelle.verdeScuro=new Color (23, 114, 69);
           // Tabelle.verde=Tabelle.verdeScuro;
            //Tabelle.rosso=Color.RED;
            Tabelle.rosso=new Color(255, 100, 100);
       }

       // UIManager.setLookAndFeel( new FlatDarkLaf() );
        CDC_Grafica g=new CDC_Grafica();
        //SwingUtilities.updateComponentTreeUI(g);   //Questo è molto utile per aggiornare il look and feel al volo 
        g.setVisible(true);
    }
    
private static String getHomeUtente(){
        String home = System.getProperty("user.home");
        System.out.println("Trovata Home user.home : "+home);
        if (home == null || home.isBlank()) {
            // Prova a usare la variabile d’ambiente HOME
            home = System.getenv("HOME");
            System.out.println("Trovata Home HOME : "+home);
        }
        if (home == null || home.isBlank()) {
            // Ultimo fallback: directory corrente
            home = new File(".").getAbsolutePath();
            System.out.println("Home non trovata : "+home);
        }
        return home;
}

private static void setWorkDir(String workingDir){
               
        //workingDir = workingDir.replaceFirst("^~", getHomeUtente());
        workingDir = workingDir.replace("HOME", getHomeUtente());

        File dir = new File(workingDir);

        // Controlli di sicurezza
        if (dir.getPath().contains("..") || dir.getPath().contains(";") || dir.getPath().contains("|")) {
            JOptionPane.showConfirmDialog(null, "Attenzione! il parametro passato a --workdir '" + workingDir + "' non è valido!\n "
                    + " Il Programma verrà terminato.", "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            System.exit(1);
        }

        if (dir.isFile()) {
            JOptionPane.showConfirmDialog(null, "Attenzione! il parametro passato a --workdir '" + workingDir + "' non è un file e non una directory!\n "
                    + " Il Programma verrà terminato.", "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            System.exit(1);
        }

        // Se la directory non esiste, tenta di crearla
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                JOptionPane.showConfirmDialog(null, "Attenzione! non è possibile creare la directory specificata : '" + workingDir + "'\n "
                        + " Il Programma verrà terminato.", "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                System.exit(1);
            }
        }

        // Verifica che sia scrivibile
        if (!dir.canWrite()) {
            System.err.println("La directory specificata non è scrivibile: " + dir.getAbsolutePath());
            JOptionPane.showConfirmDialog(null, "Attenzione! La directory specificata : '" + workingDir + "' non è scrivibile\n "
                    + " Il Programma verrà terminato.", "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            System.exit(1);
        }

        // Se tutto è ok, assegna
        Statiche.setWorkingDirectory(workingDir);
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
            JOptionPane.showConfirmDialog(null, "Attenzione! non è possibile recuperare la path del file Jar principale!\n "
                    + " Il Programma verrà terminato.", "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
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
        Thread thread;
        thread = new Thread() {
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
