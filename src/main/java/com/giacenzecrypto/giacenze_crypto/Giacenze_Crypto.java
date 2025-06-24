/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.io.File;
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
        
        
          Download progress = new Download();
            progress.Titolo("Finestra dei Log");
            progress.SetLabel("Finestra dei Log");
            progress.NascondiInterrompi();
            progress.NascondiBarra();
            progress.NoModale();
            Thread thread;
            thread = new Thread() {
                public void run() {
                
                   int k=0;
                   while (k<20){
                           try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Giacenze_Crypto.class.getName()).log(Level.SEVERE, null, ex);
        }
                   }
                    
                    
                }
            };
            thread.start();
            progress.setVisible(true);
        
        

        
        
        
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Giacenze_Crypto.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Directory di partenza : "+System.getProperty("user.dir"));
       //File workingDir = null;

        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            if (args[i].equals("--risorse") && i + 1 < args.length && args[i+1].charAt(args[i+1].length() - 1) == '/') {
                Statiche.setPathRisorse(args[i + 1]);
            }
            if (args[i].equals("--JarPath")) {
                Statiche.setPathRisorse(Funzioni.getJarPath().toString()+"/");
            }
            if (args[i].equalsIgnoreCase("--workdir") && i + 1 < args.length && args[i + 1].charAt(args[i + 1].length() - 1) == '/') {
                String workingDir = args[i + 1];
                

                // Esegui espansione variabili tipo $HOME o ~
                workingDir = workingDir.replaceFirst("^~", System.getProperty("user.home"));
                workingDir = workingDir.replace("$HOME", System.getProperty("user.home"));
                
                File dir = new File(workingDir);

                // Controlli di sicurezza
                if (dir.getPath().contains("..") || dir.getPath().contains(";") || dir.getPath().contains("|")) {
                    System.err.println("Percorso non valido: contiene caratteri non ammessi.");
                    System.exit(1);
                }

                if (dir.isFile()) {
                    System.err.println("Errore: il percorso specificato è un file, non una directory.");
                    System.exit(1);
                }

                // Se la directory non esiste, tenta di crearla
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        System.err.println("Impossibile creare la directory di lavoro: " + dir.getAbsolutePath());
                        System.exit(1);
                    }
                }

                // Verifica che sia scrivibile
                if (!dir.canWrite()) {
                    System.err.println("La directory specificata non è scrivibile: " + dir.getAbsolutePath());
                    System.exit(1);
                }

                // Se tutto è ok, assegna
                Statiche.setWorkingDirectory(dir.toString());
            }
        }
        
        System.out.println("Path Risorse : "+Statiche.getPathRisorse()); 
        System.out.println("Path Finale : "+Statiche.getWorkingDirectory());      
        
        if (!DatabaseH2.CreaoCollegaDatabase()){
         //   JOptionPane.showConfirmDialog(null, "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
         //   System.exit(0);
        }
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
    
}
