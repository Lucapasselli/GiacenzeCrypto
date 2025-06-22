/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author luca.passelli
 */
public class Giacenze_Crypto {
    

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        //String PathRisorse="";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--risorse") && i + 1 < args.length) {
                Statiche.setPathRisorse(args[i + 1]);
            }
        }
        System.out.println("Path risorse : " + Statiche.pathRisorse);
                
        
        if (!DatabaseH2.CreaoCollegaDatabase()){
            JOptionPane.showConfirmDialog(null, "Attenzione, è già aperta un'altra sessione del programma, questa verrà terminata!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            System.exit(0);
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
