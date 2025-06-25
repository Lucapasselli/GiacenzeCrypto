/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;


/**
 *
 * @author luca
 */
public class Statiche {
    private static String pathRisorse="";
    private static String workingDirectory = System.getProperty("user.dir")+"/";

    public static void setWorkingDirectory(String dir) {
        workingDirectory = dir;
    }
    public static void setPathRisorse(String risorse){
        pathRisorse=risorse;
    }

    public static String getWorkingDirectory() {
        return workingDirectory;
    }

    public static String getPathImmagini() {
        return pathRisorse+"Immagini/";
    }
    
    public static String getPathRisorse() {
        return pathRisorse;
    }

    public static String getDBPrincipale(){
        return "jdbc:h2:"+getWorkingDirectory()+"database";
    }
    
    public static String getDBPersonale(){
        return "jdbc:h2:"+getWorkingDirectory()+"personale";
    }
    
    public static String getFileUSDEUR() {
        return getWorkingDirectory() + "cambioUSDEUR.db";
    }

    public static String getFile_CDCFiatWallet() {
        return getWorkingDirectory() + "crypto.com.fiatwallet.db";
    }

    public static String getFile_CDCCardWallet() {
        return getWorkingDirectory() + "crypto.com.cardwallet.db";
    }

    public static String getFile_CDCDatiDB() {
        return getWorkingDirectory() + "crypto.com.dati.db";
    }

    public static String getFile_CryptoWallet() {
        return getWorkingDirectory() + "movimenti.crypto.db";
    }
    

    public static String getFile_CDCFiatWallet_FileTipiMovimentiPers() {
        return getWorkingDirectory() + "crypto.com.fiatwallet.tipimovimentiPers.db";
    }
    
    public static String getCartella_Temporanei() {
        return getWorkingDirectory() + "Temporanei/";
    }
    
    public static String getCartella_Backup() {
        return getWorkingDirectory() + "Backup/";
    }
}
