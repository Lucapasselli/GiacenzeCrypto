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
    public static String pathImmagini="Immagini/";
    public static String pathRisorse="";
    private static String workingDirectory = System.getProperty("user.dir");

    public static void setWorkingDirectory(String dir) {
        workingDirectory = dir;
    }

    public static String getWorkingDirectory() {
        return workingDirectory;
    }

    public static String getPathImmagini() {
        return pathImmagini;
    }
    
    public static void setPathRisorse(String risorse){
        pathRisorse=risorse;
        pathImmagini=pathRisorse+"Immagini/";
    }
    
    public static void setPathImmagini(String Immagini){
        pathImmagini=pathRisorse+Immagini;
    }

    
}
