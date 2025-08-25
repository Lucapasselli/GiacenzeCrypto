/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;

/**
 *
 * @author lucap
 */
public class Icone {
    public static Icon FrecciaDestra = new FlatSVGIcon("Images/FrecciaDestra.svg", 40, 40);
    public static Icon FrecciaSinistra = new FlatSVGIcon("Images/FrecciaSinistra.svg", 40, 40);
    public static Icon Modifica = new FlatSVGIcon("Images/Modifica.svg", 24, 24);
    public static Icon Carica = new FlatSVGIcon("Images/Carica.svg", 24, 24);
    public static Icon Chiave = new FlatSVGIcon("Images/Chiave.svg", 24, 24);
    public static Icon Catena = new FlatSVGIcon("Images/Catena.svg", 24, 24);
    public static Icon Banana = new FlatSVGIcon("Images/Banana.svg", 24, 24);
    public static Icon Imbuto = new FlatSVGIcon("Images/Imbuto.svg", 24, 24);
    public static Icon Wallet = new FlatSVGIcon("Images/Wallet.svg", 24, 24);
    public static Icon ImbutoX = new FlatSVGIcon("Images/ImbutoX.svg", 24, 24);
    public static Icon Annulla = new FlatSVGIcon("Images/Annulla.svg", 24, 24);
    public static Icon Salva = new FlatSVGIcon("Images/Salva.svg", 24, 24);
    public static Icon Stack = new FlatSVGIcon("Images/Stack.svg", 24, 24);
    public static Icon Euro = new FlatSVGIcon("Images/Euro.svg", 24, 24);
    public static Icon Attenzione = new FlatSVGIcon("Images/Attenzione.svg", 24, 24);
    public static Icon Unlock = new FlatSVGIcon("Images/Unlock.svg", 24, 24);
    public static Icon Cestino = new FlatSVGIcon("Images/Cestino.svg", 24, 24);
    public static Icon AssegnazioneAutomatica = new FlatSVGIcon("Images/AssegnazioneAutomatica.svg", 24, 24);
    
   // public static FlatSVGIcon svgImbuto = new FlatSVGIcon("Images/Imbuto.svg", 12, 12);
    
    public static Icon getAlert(int Dimensione){
        return new FlatSVGIcon("Images/Alert.svg", Dimensione, Dimensione);
    }
}
