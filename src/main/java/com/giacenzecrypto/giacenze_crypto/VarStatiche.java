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

    /** Valore dell'edizione destinata al Microsoft Store. Vedi {@link #EdizioneStore()}. */
    static final String EDIZIONE_STORE = "store";

    /**
     * Edizione con cui il jar è stato costruito, letta da {@code version.properties}: {@code "completa"}
     * (il default del pom) oppure {@code "store"}.
     */
    static String Edizione = leggiEdizione();

    /** Titolo mostrato nella barra della finestra, sul pulsante del titolo e nel piede delle stampe. */
    static String Titolo = componiTitolo(Versione);

    /**
     * Compone il titolo dell'applicazione.
     * <p>Fino al 18/08/2026 il titolo portava la dicitura <i>Beta</i> nell'edizione completa e non in
     * quella per il Microsoft Store (policy 10.1: un'applicazione che si annuncia incompleta attira
     * domande in certificazione). Uscito il programma dalla fase beta la dicitura non c'è più da
     * nessuna parte, e il titolo non dipende più dall'edizione.
     * <p>Sta in un metodo, e non nell'espressione del campo, perché il campo si calcola una volta sola
     * al caricamento della classe e non sarebbe verificabile.
     * @param versione la versione da mostrare
     * @return il titolo completo
     */
    static String componiTitolo(String versione) {
        return "Giacenze Crypto " + versione;
    }

    /**
     * Riferimento al sito del progetto per il piede delle stampe (Quadro W/RW e T/RT).
     * <p>Nell'edizione Store è <b>vuoto</b>: quell'indirizzo è la pagina da cui l'applicazione si
     * scarica fuori dallo Store, e in un documento fiscale che l'utente stampa e conserva non ha motivo
     * di comparire. Vedi {@code test/Documentazione/Pubblicazione_MicrosoftStore.md} §1.6.
     * @return l'indirizzo già preceduto da {@code " - "}, oppure stringa vuota nell'edizione Store
     */
    static String RiferimentoStampe() {
        return EdizioneStore() ? "" : " - https://sourceforge.net/projects/giacenze-crypto-com";
    }

    /**_Giacenzeadata
     * Legge la versione dell'applicazione da {@code version.properties}, generato da Maven
     * a partire dalla {@code <version>} del pom tramite resource filtering.
     * @return la versione del progetto, oppure {@code "sconosciuta"} se il file non è
     *         leggibile o il segnaposto non è stato sostituito
     */
    private static String leggiVersione() {
        return leggiProprietaBuild("versione", "sconosciuta");
    }

    /**
     * Legge l'edizione da {@code version.properties} (proprietà {@code giacenze.edizione} del pom).
     * @return l'edizione dichiarata, oppure {@code "completa"} se il file non è leggibile o il
     *         segnaposto non è stato sostituito: nel dubbio si assume il programma intero, perché
     *         è la build ordinaria e l'edizione ridotta va scelta apposta
     */
    private static String leggiEdizione() {
        return leggiProprietaBuild("edizione", "completa");
    }

    /**
     * Legge una proprietà da {@code version.properties}, il file che Maven riempie in fase di build
     * tramite resource filtering (vedi il blocco {@code <resources>} del pom).
     * @param Chiave nome della proprietà
     * @param Predefinito valore da usare se il file manca, non è leggibile o il segnaposto
     *        {@code ${...}} non è stato sostituito (cioè se il filtering non è stato eseguito)
     * @return il valore letto, oppure {@code Predefinito}
     */
    private static String leggiProprietaBuild(String Chiave, String Predefinito) {
        try (InputStream in = VarStatiche.class.getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty(Chiave, "").trim();
                //se il filtering di Maven non è stato eseguito il segnaposto arriva qui letterale
                if (!v.isEmpty() && !v.startsWith("${")) {
                    return v;
                }
            }
        } catch (Exception e) {
            System.out.println("Impossibile leggere " + Chiave + " da version.properties : " + e.getMessage());
        }
        return Predefinito;
    }

    /**
     * L'edizione destinata al Microsoft Store rinuncia a due cose, entrambe per motivi di policy e non
     * di funzionalità (vedi {@code test/Documentazione/Pubblicazione_MicrosoftStore.md}):
     * <ul>
     * <li>lo <b>scaricamento dei movimenti dai conti exchange</b>, perché l'accesso a un exchange e la
     *     conservazione di chiavi API sono "financial information" e imporrebbero un account Company
     *     (policy 10.8.3 e 10.2.6), che non è una strada percorribile;</li>
     * <li>i <b>loghi di exchange e blockchain</b> di {@code config/loghi/}, che sono immagini di terzi
     *     (policy 11.2 e termini CoinGecko, vedi {@code Analisi_API_Terze_Parti.md}).</li>
     * </ul>
     * Senza quelle due, il programma ricade nella casistica esplicitamente permessa dalla 10.2.6: legge
     * soltanto indirizzi pubblici e transazioni, e importa da file.
     *
     * @return {@code true} se il jar è stato costruito con {@code -Dgiacenze.edizione=store}
     */
    public static boolean EdizioneStore() {
        return EDIZIONE_STORE.equalsIgnoreCase(Edizione);
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


    /**
     * @return il percorso del file con i tempi delle fasi dell'ultimo avvio, usati da
     *         {@link SplashAvvio} per dare alla barra di caricamento pesi coerenti con questa
     *         installazione (un archivio da centomila movimenti non ha i tempi di uno vuoto)
     */
    public static String getFile_TempiAvvio() {
        return getWorkingDirectory() + "avvio.tempi.db";
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

    /**
     * @return il percorso della cartella {@code ArchivioBackup/}, dove {@link Backup_Restore} conserva
     *         gli archivi di backup completi (uno zip più il manifest json affiancato).
     *         <p>Deliberatamente <b>fuori</b> da {@code Backup/} : all'avvio
     *         {@link Funzioni#Files_CancellaOltreTOTh} pota quella cartella a 4320 h (180 giorni), e un
     *         backup dell'archivio fiscale non deve scadere. La ritenzione qui è eterna e la cancellazione
     *         solo manuale, dal pannello.
     */
    public static String getCartella_ArchivioBackup() {
        return getWorkingDirectory() + "ArchivioBackup/";
    }

    /**
     * @return il percorso della cartella {@code ImportConfig/} nella directory di lavoro.
     *         <p>Cartella storica delle configurazioni di import: non viene più sincronizzata con il
     *         repository (quel compito è passato a {@link #getCartella_ConfigImport()}) ed è mantenuta
     *         solo per retrocompatibilità con le installazioni precedenti. Da qui vanno letti soltanto
     *         i file NON marcati {@code "centralizzato": true}, cioè quelli scritti dall'utente.
     */
    public static String getCartella_ImportConfig() {
        return getWorkingDirectory() + "ImportConfig/";
    }

    /** @return il percorso della cartella {@code config/import/}, configurazioni di import complete */
    public static String getCartella_ConfigImport() {
        return getWorkingDirectory() + "config/import/";
    }

    /** @return il percorso della cartella {@code config/importmappe/}, sole mappe delle causali */
    public static String getCartella_ConfigImportMappe() {
        return getWorkingDirectory() + "config/importmappe/";
    }

    /** @return il percorso della cartella {@code config/loghi/}, loghi degli exchange/wallet */
    public static String getCartella_ConfigLoghi() {
        return getWorkingDirectory() + "config/loghi/";
    }

    /**
     * @return il percorso della cartella {@code DocumentiFonte/} nella directory di lavoro, dove vengono
     *         conservate compresse le copie dei file sorgente delle importazioni (CSV, JSON, NDJSON delle API).
     *         <p>Deliberatamente <b>fuori</b> da {@code Backup/} e {@code Temporanei/}: all'avvio
     *         {@link Funzioni#Files_CancellaOltreTOTh} pota quelle due cartelle (4320 h e 24 h) e un documento
     *         fiscale di origine non deve scadere.
     */
    public static String getCartella_DocumentiFonte() {
        return getWorkingDirectory() + "DocumentiFonte/";
    }

    /** @return il percorso del file di configurazione dei chatbot usati dalla funzione "Chiedi a IA" */
    public static String getFile_ChatbotIA() {
        return getWorkingDirectory() + "ChatbotIA.json";
    }
}
