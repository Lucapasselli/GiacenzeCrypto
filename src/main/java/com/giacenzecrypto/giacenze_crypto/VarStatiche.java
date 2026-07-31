/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.io.InputStream;
import java.util.Properties;


/**
 *
 * @author luca
 */
public class VarStatiche {

    //NOME DEL PROGRAMMA
    static String Versione = leggiVersione();
    static String Titolo = "Giacenze Crypto " + VarStatiche.Versione + " Beta";

    /**_Giacenzeadata
     * Legge la versione dell'applicazione da {@code version.properties}, generato da Maven
     * a partire dalla {@code <version>} del pom tramite resource filtering.
     * @return la versione del progetto, oppure {@code "sconosciuta"} se il file non è
     *         leggibile o il segnaposto non è stato sostituito
     */
    private static String leggiVersione() {
        try (InputStream in = VarStatiche.class.getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("versione", "").trim();
                //se il filtering di Maven non è stato eseguito il segnaposto arriva qui letterale
                if (!v.isEmpty() && !v.startsWith("${")) {
                    return v;
                }
            }
        } catch (Exception e) {
            System.out.println("Impossibile leggere la versione da version.properties : " + e.getMessage());
        }
        return "sconosciuta";
    }

    //=== IMPOSTAZIONI GLOBALI RELATIVE AI CALCOLI ===
    static int DecimaliPlus=2;
    static int DecimaliCalcoli = 30;
    
    //=== IMPOSTAZIONI GLOBALI RELATIVE ALLE PATH DEL PROGRAMMA
    private static String pathRisorse="";
    private static String workingDirectory = System.getProperty("user.dir")+"/";       
    
    
    

    /** @param dir la nuova directory di lavoro (dove risiedono i database e i file runtime), deve terminare con {@code /} */
    public static void setWorkingDirectory(String dir) {
        workingDirectory = dir;
    }
    /** @param risorse il nuovo percorso delle risorse bundlate (immagini, ecc.), deve terminare con {@code /} */
    public static void setPathRisorse(String risorse){
        pathRisorse=risorse;
    }

    /** @return la directory di lavoro corrente (dove risiedono i database e i file runtime) */
    public static String getWorkingDirectory() {
        return workingDirectory;
    }

    /** @return il percorso della cartella {@code Immagini/} all'interno del percorso risorse */
    public static String getPathImmagini() {
        return pathRisorse+"Immagini/";
    }

    /** @return il percorso delle risorse bundlate (immagini, configurazioni) */
    public static String getPathRisorse() {
        return pathRisorse;
    }

    /** @return la URL di connessione JDBC H2 al database principale ({@code database.mv.db}) */
    public static String getDBPrincipale(){
        return "jdbc:h2:"+getWorkingDirectory()+"database";
    }

    /** @return la URL di connessione JDBC H2 al database personale ({@code personale.mv.db}) */
    public static String getDBPersonale(){
        return "jdbc:h2:"+getWorkingDirectory()+"personale";
    }

    /** @return la URL di connessione JDBC H2 al database dei prezzi ({@code prezzi.mv.db}) */
    public static String getDBPrezzi(){
        return "jdbc:h2:"+getWorkingDirectory()+"prezzi";
    }


    /** @return il percorso del file di cache dei tassi di cambio USD/EUR */
    public static String getFileUSDEUR() {
        return getWorkingDirectory() + "cambioUSDEUR.db";
    }

    /** @return il percorso del file dati del fiat wallet Crypto.com */
    public static String getFile_CDCFiatWallet() {
        return getWorkingDirectory() + "crypto.com.fiatwallet.db";
    }

    /** @return il percorso del file dati del card wallet Crypto.com */
    public static String getFile_CDCCardWallet() {
        return getWorkingDirectory() + "crypto.com.cardwallet.db";
    }

    /** @return il percorso del file dati generico Crypto.com */
    public static String getFile_CDCDatiDB() {
        return getWorkingDirectory() + "crypto.com.dati.db";
    }

    /** @return il percorso del file dei movimenti crypto */
    public static String getFile_CryptoWallet() {
        return getWorkingDirectory() + "movimenti.crypto.db";
    }


    /** @return il percorso del file dei tipi movimento personalizzati per il fiat wallet Crypto.com */
    public static String getFile_CDCFiatWallet_FileTipiMovimentiPers() {
        return getWorkingDirectory() + "crypto.com.fiatwallet.tipimovimentiPers.db";
    }

    /** @return il percorso della cartella {@code Temporanei/} nella directory di lavoro */
    public static String getCartella_Temporanei() {
        return getWorkingDirectory() + "Temporanei/";
    }

    /** @return il percorso della cartella {@code Backup/} nella directory di lavoro */
    public static String getCartella_Backup() {
        return getWorkingDirectory() + "Backup/";
    }

    /** @return il percorso della cartella {@code ImportConfig/} nella directory di lavoro */
    public static String getCartella_ImportConfig() {
        return getWorkingDirectory() + "ImportConfig/";
    }

    /** @return il percorso del file di configurazione dei chatbot usati dalla funzione "Chiedi a IA" */
    public static String getFile_ChatbotIA() {
        return getWorkingDirectory() + "ChatbotIA.json";
    }
}
