/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static com.giacenzecrypto.giacenze_crypto.Principale.MappaRetiSupportate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 *
 * @author Luca
 */
public class Funzioni {

    //A5: client OkHttp condiviso invece di uno nuovo per chiamata (evita di accumulare connection pool/dispatcher inutilizzati)
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private static final Set<String> PREFISSI_VALIDI_TrovaReteDaIMovimento = Set.of(
        "BC", "00BC", "01BC", "02BC", "03BC", "04BC", "ZZBC"
);

    
        /**
         * Rimuove da {@link Principale#MappaCryptoWallet} tutti i movimenti di un wallet compresi in un
         * intervallo di date, sistemando anche i movimenti correlati tramite {@link #RimuoviMovimentazioneXID}.
         * @param Wallet nome del wallet su cui operare, oppure {@code null} per operare su tutti i wallet
         * @param DataIniziale inizio dell'intervallo (incluso), millisecondi epoch; ignorato insieme a {@code DataFinale} se entrambi sono 0
         * @param DataFinale fine dell'intervallo (escluso), millisecondi epoch; ignorato insieme a {@code DataIniziale} se entrambi sono 0
         * @return il numero di movimenti cancellati
         */
        public static int CancellaMovimentazioniXWallet(String Wallet,long DataIniziale,long DataFinale){
        
        //Se Wallet=null  allora la pulizia la faccio su tutti i wallet
        int movimentiCancellati=0;
       //this.TransazioniCryptoFiltro_Text.setText("");
        //questo server per velocizzare la ricerca
        //disabilito il filtro e poi lo riabilito finito l'eleaborazione
        
        List<String> Cancellare=new ArrayList<>();
        

            for (String v : Principale.MappaCryptoWallet.keySet()) {
                String WalletRiga = Principale.MappaCryptoWallet.get(v)[3].trim();
                long DataMovimento=FunzioniDate.ConvertiDatainLong(Principale.MappaCryptoWallet.get(v)[1].split(" ")[0]);
                if (Wallet==null || WalletRiga.equalsIgnoreCase(Wallet.trim())) {
                    if ((DataMovimento >= DataIniziale
                            && DataMovimento < DataFinale) ||
                            (DataIniziale==0)&&(DataFinale==0)) {//Se data finale e iniziale sono a zero significa che non valgolo i limiti di date
                        Cancellare.add(v);
                        movimentiCancellati++;
                    }

                }
            }
        Iterator<String> I=Cancellare.iterator();
        while (I.hasNext()){
            String daRimuovere=I.next().toString();
            RimuoviMovimentazioneXID(daRimuovere);
        }
        
           // MappaCryptoWallet.clear();
   
        return movimentiCancellati;
    }
        
         /**
          * Rimuove da {@link Principale#MappaCryptoWallet} il movimento indicato, sistemando prima gli eventuali
          * movimenti correlati (es. se si rimuove un prelievo associato a un deposito, i riferimenti al prelievo
          * vengono tolti dal deposito) tramite {@link GUI_ClassificazioneMovimento#RiportaTransazioniASituazioneIniziale}.
          * @param ID identificativo del movimento da rimuovere
          */
         public static void RimuoviMovimentazioneXID(String ID){
             
            String Annessi[]=Principale.MappaCryptoWallet.get(ID);
            if (Annessi!=null){
            String PartiCoinvolte[]=(ID+","+Annessi[20]).split(",");
            if (Annessi[20]!=null && !Annessi[20].equalsIgnoreCase("")){
                //L'ID può infatti cambiare in fase di ripristino dei movimenti
                ID=GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(PartiCoinvolte,ID);
            }
            Principale.MappaCryptoWallet.remove(ID);
            } 
         }
        
        
         
        /**
         * @return la directory che contiene il JAR (o la classe, se eseguita non impacchettata) di questa applicazione
         * @throws RuntimeException se l'URI della sorgente del codice non è valido
         */
        public static Path getJarPath() {
        try {
            return new File(
                Funzioni.class.getProtectionDomain()
                           .getCodeSource()
                           .getLocation()
                           .toURI()
            ).getParentFile().toPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Impossibile ottenere il path del JAR", e);
        }
    } 
       
        

        
    static boolean ConnInternetAttiva=false;
    static long TimesTampUltimoControlloInternet=0;   
    static boolean CeConnessioneInternet() {
        String[] urls = {
            "https://www.google.com",
            "https://cloudflare.com",
            "https://www.microsoft.com"
        };

        //questa parte impedisce di fare test della connessione internet se non sono passati almeno 30 secondi dall'ultimo controllo
        long TO = System.currentTimeMillis();
        if (TO - TimesTampUltimoControlloInternet > 60000) {
            System.out.println("Testo connessione Internet...");
            for (String url : urls) {
                try {
                    HttpURLConnection conn
                            = (HttpURLConnection) new URL(url).openConnection();

                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(2500);
                    conn.setReadTimeout(2500);
                    conn.connect();

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 400) {
                        TimesTampUltimoControlloInternet = System.currentTimeMillis();
                        ConnInternetAttiva = true;
                        System.out.println("Internet Disponibile");
                      //LoggerGC.logInfo("Internet Disponibile");
                        return true;
                    }
                } catch (IOException ignored) {
                }
            }
            ConnInternetAttiva = false;
            TimesTampUltimoControlloInternet = System.currentTimeMillis();
            System.out.println("Internet NON Disponibile");
            return false;
        } else {
            return ConnInternetAttiva;
        }
    }
        
        
        
    /**
     * Restituisce il gruppo di prezzi personalizzati a cui appartiene un wallet. Attualmente ritorna sempre
     * {@code "TUTTI"} (i prezzi personalizzati non sono differenziati per wallet, per evitare confusione),
     * ma il parametro {@code Wallet} è mantenuto per un'eventuale differenziazione futura.
     * @param Wallet nome del wallet (attualmente non utilizzato)
     * @return sempre {@code "TUTTI"}
     */
    public static String getGruppoWalletXPrezzi(String Wallet){
        return "TUTTI";
        //Questa funzione servirà in futuro se vorro differenziare i prezi personalizzati per Wallet
        //per ora li metto tutti uguali, credo che altrimenti la cosa generi troppa confusione
        //
        
        
      /*  //Questa funzione è utile solo per i prezzi, torno ALL come nome Gruppo wallet nel caso in cui non trovo
        //il gruppo del wallet riferito al nome del wallet
        if (Wallet==null||Wallet.isBlank()||Wallet.toLowerCase().equals("tutti"))return "TUTTI";
        String Gruppo=DatabaseH2.Pers_GruppoWallet_Leggi(Wallet,false);
        if (Gruppo==null){
            //Se non trovo il gruppo verifico di non averlo già compreso nel nome
            //in sostanza è stato passato il nome del gruppo invece che il nome del wallet
            //il nome del gruppo è codì di solito Gruppo : Wallet 01 (Binance group)
            if (Wallet.toLowerCase().contains("gruppo :")){
                int start = Wallet.indexOf(':') + 1;
                int end = Wallet.indexOf('(');
                Gruppo = Wallet.substring(start, end).trim();
                return Gruppo;
            }
        }else return Gruppo;
        return "TUTTI";*/
    
    }    
        
        
    /**
     * Estrae data e ora dal prefisso timestamp di un ID movimento (formato {@code yyyyMMddHHmmss_...}).
     * Se il prefisso non è nel formato atteso, ripiega sulla data/ora già salvata nel movimento corrispondente
     * in {@link Principale#MappaCryptoWallet}, con secondi impostati a {@code :00}.
     * @param ID identificativo del movimento
     * @return la data/ora formattata come {@code yyyy-MM-dd HH:mm:ss}
     */
    public static String getOradaID(String ID) {
        String input = ID.split("_")[0];
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, inputFormatter);
            return dateTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            return Principale.MappaCryptoWallet.get(ID)[1]+":00";
        }
    }
    
        /**
         * Incrementa di 1 secondo un timestamp nel formato {@code yyyyMMddHHmmss} (usato come prefisso degli ID movimento).
         * @param input timestamp da incrementare, formato {@code yyyyMMddHHmmss}
         * @return il timestamp incrementato nello stesso formato, oppure {@code "nonOK"} se {@code input} non è parsabile
         */
        public static String DataIDaggiungiUnSecondo(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, formatter);
            LocalDateTime incrementata = dateTime.plusSeconds(1);
            return incrementata.format(formatter);
        } catch (DateTimeParseException e) {
            return "nonOK";
        }
    }
        
        /**
         * Decrementa di 1 secondo un timestamp nel formato {@code yyyyMMddHHmmss} (usato come prefisso degli ID movimento).
         * @param input timestamp da decrementare, formato {@code yyyyMMddHHmmss}
         * @return il timestamp decrementato nello stesso formato, oppure {@code "nonOK"} se {@code input} non è parsabile
         */
        public static String DataIDtogliUnSecondo(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, formatter);
            LocalDateTime incrementata = dateTime.minusSeconds(1);
            return incrementata.format(formatter);
        } catch (DateTimeParseException e) {
            return "nonOK";
        }
    }
    
        /**
         * Formatta un {@link BigDecimal} secondo le convenzioni italiane (punto come separatore delle migliaia,
         * virgola come separatore decimale).
         * @param numero valore da formattare
         * @param decimali se {@code true} mostra sempre 2 decimali (pattern {@code #,##0.00}), altrimenti nessun decimale (pattern {@code #,##0})
         * @return la stringa formattata
         */
        public static String formattaBigDecimal(BigDecimal numero,boolean decimali) {
        // Crea un'istanza di DecimalFormatSymbols per il locale italiano
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
        symbols.setGroupingSeparator('.'); // Separatore delle migliaia
        symbols.setDecimalSeparator(',');  // Separatore decimale

        // Definisce il pattern di formattazione
        DecimalFormat formatter;               
        if (decimali) formatter = new DecimalFormat("#,##0.00", symbols);
        else formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(numero);
    }
         
       /** Simula la pressione della combinazione di tasti Ctrl+C tramite {@link Robot} (copia negli appunti). */
       public static void simulaCtrlC() {
        try {
            Robot robot = new Robot();

            // Preme Ctrl
            robot.keyPress(KeyEvent.VK_CONTROL);
            // Preme C
            robot.keyPress(KeyEvent.VK_C);

            // Rilascia C
            robot.keyRelease(KeyEvent.VK_C);
            // Rilascia Ctrl
            robot.keyRelease(KeyEvent.VK_CONTROL);

           // System.out.println("Simulato Ctrl+C");

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }  
        
              /** Simula la pressione della combinazione di tasti Ctrl+V tramite {@link Robot} (incolla dagli appunti). */
              public static void simulaCtrlV() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }  
    
    /**
     * Verifica se un movimento di deposito/prelievo può essere classificato manualmente dall'utente: deve
     * essere di tipo {@code DC}/{@code PC} (crypto, con la relativa moneta non scam) oppure, se richiesto
     * tramite {@code VediFIAT}, di tipo {@code DF}/{@code PF} (fiat); in ogni caso non deve essere un
     * movimento generato automaticamente (campo 22 diverso da {@code "AU"}).
     * @param ID identificativo del movimento (se {@code null}, viene ricavato da {@code v[0]})
     * @param v riga di movimento grezza (se {@code null}, viene recuperata da {@link Principale#MappaCryptoWallet} tramite {@code ID})
     * @param VediFIAT se {@code true}, considera classificabili anche i movimenti fiat
     * @return {@code true} se il movimento è classificabile
     */
    public static boolean isDepositoPrelievoClassificabile(String ID,String v[],boolean VediFIAT){
        //posso passare alla funzione sia l'ID della transazione sia la transazione per intero o entrambe
        //I depositi classificabili sono quelli di tipo DC e PC che non sono movimenti artificiali
        //il movimento inoltre non deve contemplare token scam
        if (ID==null)ID=v[0];
        String TipoMovimento=ID.split("_")[4].trim();
        if(v==null) v=Principale.MappaCryptoWallet.get(ID);
        
        //In questa parte considero il movimento ok se è per crypto e se è di fiat ma solo se il booleano è a true
        //Set<String> movimentiCrypto = Set.of("DC", "PC");
        Set<String> movimentiFiat = Set.of("DF", "PF");
        boolean tipoMovimentoOK
                = TipoMovimento != null//il movimento non deve essere null
                &&
                (
                (TipoMovimento.equalsIgnoreCase("DC")&&!Funzioni.isSCAM(v[11]))//può essere DC e non scam
                || (TipoMovimento.equalsIgnoreCase("PC")&&!Funzioni.isSCAM(v[8]))//può essere PC e non scam
                || (VediFIAT && movimentiFiat.contains(TipoMovimento.toUpperCase()))//può essere fiat se espressamente richiesto
                )&& 
                v[22]!=null&&!v[22].equalsIgnoreCase("AU")//non deve essere un movimento automaticamente generato
                
                ;
        return tipoMovimentoOK;

    }         
              
              
    /**
     * Gestisce l'apertura del menu contestuale (tasto destro) su una tabella o campo di testo, abilitando o
     * disabilitando le singole voci in base al contesto: se non è passato un ID movimento disabilita tutte le
     * voci relative al movimento; altrimenti le abilita e regola "Classifica Movimento" in base a
     * {@link #isDepositoPrelievoClassificabile} e "Cambia Tipologia Reward" in base al tipo di movimento.
     * Distingue inoltre tra origine tabella (abilita "Esporta Tabella in Excel") e campo di testo (abilita "Incolla").
     * @param c componente rispetto a cui posizionare il menu
     * @param e evento del mouse che ha aperto il menu
     * @param pop il menu contestuale da configurare e mostrare
     * @param ID identificativo del movimento selezionato, oppure {@code null} se nessuno
     */
    public static void PopUpMenu(Component c, java.awt.event.MouseEvent e, JPopupMenu pop,String ID) {
        //if (e.isPopupTrigger()) {
            if (e.getButton() == MouseEvent.BUTTON3) {
            //Component focusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            //salvo l'ID passato dalla funzione, servirà nel caso in cui prema su alcune funzioni
            Principale.PopUp_IDTrans=ID;
            Principale.PopUp_Component=c;
            //System.out.println(CDC_Grafica.PopUp_Component);
            Component C_chiamante=e.getComponent();
            //C_chiamante
            Point Coordinata = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(Coordinata, c);
            //Se non passo l'id della transazione ingrigisco il tasto dettaglio movimento
            if (ID==null)
            {
                PopUp_disabilitaMenuDatesto(pop,"Dettagli Movimento");
                PopUp_disabilitaMenuDatesto(pop,"Modifica Movimento");
                PopUp_disabilitaMenuDatesto(pop,"Elimina Movimento");
                PopUp_disabilitaMenuDatesto(pop,"Classifica Movimento");
                PopUp_disabilitaMenuDatesto(pop,"Copia ID Transazione");
                PopUp_disabilitaMenuDatesto(pop,"Modifica Prezzo");
                PopUp_disabilitaMenuDatesto(pop,"Modifica Note");
                PopUp_disabilitaMenuDatesto(pop,"Cambia Tipologia Reward");
                PopUp_disabilitaMenuDatesto(pop,"Mostra LiFo Transazione");
                PopUp_disabilitaMenuDatesto(pop,"Separa in Deposito/Prelievo");
                PopUp_disabilitaMenuDatesto(pop,"Crea movimento di scambio da Deposito/Prelievo");
                PopUp_disabilitaMenuDatesto(pop,"Chiedi a IA");

            }else{
                PopUp_abilitaMenuDaTesto(pop,"Dettagli Movimento");
                PopUp_abilitaMenuDaTesto(pop,"Modifica Movimento");
                PopUp_abilitaMenuDaTesto(pop,"Elimina Movimento");
                PopUp_abilitaMenuDaTesto(pop,"Copia ID Transazione");
                PopUp_abilitaMenuDaTesto(pop,"Modifica Prezzo");
                PopUp_abilitaMenuDaTesto(pop,"Modifica Note");
                PopUp_abilitaMenuDaTesto(pop,"Mostra LiFo Transazione");

                //"Chiedi a IA" costruisce la domanda sui dati del movimento: serve che il movimento ci sia
                //davvero, perché il popup è condiviso con tabelle le cui righe non sono movimenti
                if (Principale.MappaCryptoWallet.get(ID)!=null){
                    PopUp_abilitaMenuDaTesto(pop,"Chiedi a IA");
                }else PopUp_disabilitaMenuDatesto(pop,"Chiedi a IA");

                if (isDepositoPrelievoClassificabile(ID, null,false)){
                   PopUp_abilitaMenuDaTesto(pop,"Classifica Movimento"); 
                }else PopUp_disabilitaMenuDatesto(pop,"Classifica Movimento");
                
                if(ID.split("_")[4].equals("RW")||Principale.MappaCryptoWallet.get(ID)[18].contains("DAI -"))
                    PopUp_abilitaMenuDaTesto(pop,"Cambia Tipologia Reward");
                else PopUp_disabilitaMenuDatesto(pop,"Cambia Tipologia Reward");

                //Separazione: solo su selezione singola di un movimento che coinvolge due monete
                if (Principale.PopUp_IDTransSelezionati.size()==1
                        && Principale_Movimenti_SeparaUnisci.isSeparabileInDepositoPrelievo(ID)){
                    PopUp_abilitaMenuDaTesto(pop,"Separa in Deposito/Prelievo");
                }else PopUp_disabilitaMenuDatesto(pop,"Separa in Deposito/Prelievo");

                //Fusione: solo su due movimenti non classificati (un deposito e un prelievo) fondibili
                if (Principale_Movimenti_SeparaUnisci.isUnibileInScambio(Principale.PopUp_IDTransSelezionati)){
                    PopUp_abilitaMenuDaTesto(pop,"Crea movimento di scambio da Deposito/Prelievo");
                }else PopUp_disabilitaMenuDatesto(pop,"Crea movimento di scambio da Deposito/Prelievo");
            }
            
            //Se è una tabella mi comporto in questo modo
            if (C_chiamante instanceof JTable table)
            {                
                int row = table.getSelectedRow();
                if (row == -1) return;
                PopUp_disabilitaMenuDatesto(pop,"Incolla");
                PopUp_abilitaMenuDaTesto(pop,"Esporta Tabella in Excel");
                Principale.PopUp_Tabella=table;
                pop.show(c, Coordinata.x, Coordinata.y);
            }            
            else if (C_chiamante instanceof JTextField)
            {
                PopUp_abilitaMenuDaTesto(pop,"Incolla");
                PopUp_disabilitaMenuDatesto(pop,"Esporta Tabella in Excel");
                pop.show(c, Coordinata.x, Coordinata.y);
                Principale.PopUp_Tabella=null;
            }
            //Se è un campo di testo in quest'altro
            
            
            
        }
    }
    
    
    /**
     * Verifica se una stringa rappresenta un valore decimale diverso da zero.
     * @param valore stringa da valutare
     * @return {@code true} se {@code valore} è un {@link BigDecimal} valido e diverso da zero, {@code false} altrimenti (incluso il caso di stringa non numerica)
     */
    public static boolean isBigDecimalNonZero(String valore) {
    try {
        return new BigDecimal(valore).compareTo(BigDecimal.ZERO) != 0;
    } catch (Exception e) {
        return false;
    }
}

    /**
     * Verifica se una quantità è negativa, cioè se rappresenta un movimento in <b>uscita</b>.
     *
     * <p>Nasce dalla correzione <b>M7</b> (vedi {@code Documentazione/Analisi_Bug_Criticita.md}): in tutto
     * il programma la direzione di un movimento veniva dedotta cercando un trattino <i>in qualunque
     * posizione</i> della stringa ({@code Qta.contains("-")}). Una quantità <b>positiva</b> scritta in
     * notazione scientifica con esponente negativo ({@code 2.5E-9}, tipica dei token con molti decimali e
     * prodotta spontaneamente da {@code BigDecimal.toString()}) contiene un trattino e veniva quindi
     * scambiata per una quantità in uscita, rovesciando la classificazione del movimento.</p>
     *
     * <p>Per i valori <b>non numerici</b> viene mantenuto il vecchio comportamento testuale, in modo che
     * la correzione cambi esito soltanto nel caso che intende correggere e non introduca differenze su
     * dati malformati.</p>
     *
     * @param valore quantità da valutare
     * @return {@code true} se la quantità è negativa; {@code false} se è positiva, nulla o {@code null}
     */
    public static boolean isNegativo(String valore) {
        if (valore == null) return false;
        if (!isNumeric(valore, false)) return valore.contains("-");
        return new BigDecimal(valore).signum() < 0;
    }

    /**
     * Riduce una credenziale (chiave API, secret, passphrase) alla forma abbreviata da usare nei log:
     * le prime quattro cifre seguite da puntini. Serve a poter riconoscere <i>quale</i> credenziale sia
     * in gioco senza scriverla per intero nei file di log, che possono essere allegati a una segnalazione.
     * @param credenziale credenziale da mascherare
     * @return la credenziale abbreviata, o {@code "(vuota)"} se non è valorizzata
     */
    public static String MascheraCredenziale(String credenziale) {
        if (credenziale == null || credenziale.isBlank()) return "(vuota)";
        if (credenziale.length() <= 4) return "…";
        return credenziale.substring(0, 4) + "…";
    }

    /**
     * Verifica se un clic del mouse su una tabella è avvenuto su una riga già presente nella selezione corrente
     * (utile per decidere se un click destro deve preservare una selezione multipla esistente).
     * @param table tabella su cui è avvenuto il clic
     * @param e evento del mouse
     * @return {@code true} se la riga cliccata è tra quelle selezionate, {@code false} se il clic è fuori da ogni riga o su una riga non selezionata
     */
    public static boolean PopUp_ClickInternoASelezione(JTable table,java.awt.event.MouseEvent e){
        int clickedRow = table.rowAtPoint(e.getPoint());
        if (clickedRow == -1) return false; // clic fuori da qualsiasi riga

        // Ottieni tutte le righe attualmente selezionate
        int[] selectedRows = table.getSelectedRows();

        // Verifica se la riga cliccata è tra quelle selezionate
        for (int row : selectedRows) {
            if (row == clickedRow) {
                //Trovato riga selezionata
                return true;
            }
        }
        return false;
    }
    
    
        /**
         * @param popupMenu il menu contestuale da esaminare
         * @return la lista di tutti i {@link JMenuItem} diretti contenuti in {@code popupMenu}
         */
        public static List<JMenuItem> PopUp_getAllMenuItems(JPopupMenu popupMenu) {
        List<JMenuItem> items = new ArrayList<>();
        for (Component comp : popupMenu.getComponents()) {
            if (comp instanceof JMenuItem) {
                items.add((JMenuItem) comp);
            }
        }
        return items;
    }
    
    
    /**
     * Disabilita, all'interno di un menu contestuale, la voce il cui testo corrisponde (case-insensitive) a quello indicato.
     * @param popupMenu il menu contestuale da modificare
     * @param textToDisable testo della voce da disabilitare
     */
    public static void PopUp_disabilitaMenuDatesto(JPopupMenu popupMenu, String textToDisable) {
        for (JMenuItem item : PopUp_getAllMenuItems(popupMenu)) {
            if (item.getText() != null && item.getText().equalsIgnoreCase(textToDisable)) {
                item.setEnabled(false);
            }
        }
    }
        /**
         * Abilita, all'interno di un menu contestuale, la voce il cui testo corrisponde (case-insensitive) a quello indicato.
         * @param popupMenu il menu contestuale da modificare
         * @param textToDisable testo della voce da abilitare
         */
        public static void PopUp_abilitaMenuDaTesto(JPopupMenu popupMenu, String textToDisable) {
        for (JMenuItem item : PopUp_getAllMenuItems(popupMenu)) {
            if (item.getText() != null && item.getText().equalsIgnoreCase(textToDisable)) {
                item.setEnabled(true);
            }
        }
    }
   






        
        
/**
 * Estrae la sottostringa compresa tra la prima occorrenza di {@code simboloIniziale} e la successiva
 * occorrenza di {@code simboloFinale}.
 * @param parola stringa da cui estrarre
 * @param simboloIniziale simbolo/sottostringa di apertura
 * @param simboloFinale simbolo/sottostringa di chiusura
 * @return la sottostringa (trimmata) tra i due simboli, oppure {@code ""} se uno dei due non è presente o sono in ordine invertito
 */
public static String getParolaTra2Simboli(String parola, String simboloIniziale, String simboloFinale) {

    int posIni = parola.indexOf(simboloIniziale);
    int posFin = parola.indexOf(simboloFinale);

    if (posIni == -1 || posFin == -1) return "";       // uno dei due simboli non esiste
    if (posFin <= posIni) return "";                  // simboli invertiti o malformati

    int start = posIni + simboloIniziale.length();
    return parola.substring(start, posFin).trim();
}
        


/**
 * Permette di modificare il prezzo di una transazione usando AppDialog al posto di JOptionPane.
 *
 * <p>Il flusso è composto da queste fasi:</p>
 * <ol>
 *   <li>Se la data del movimento è disponibile, chiede se il prezzo verrà inserito in euro o in dollari.</li>
 *   <li>Recupera e mostra un prezzo automatico di supporto calcolato dal programma.</li>
 *   <li>Chiede il nuovo prezzo, valida che sia numerico e lo converte eventualmente in euro.</li>
 *   <li>Se il valore finale è 0.00, richiede una conferma esplicita prima di salvare.</li>
 * </ol>
 *
 * <p>In caso di annullamento o chiusura di uno dei dialog, il metodo restituisce {@code false}.</p>
 *
 * @param c componente parent del dialog
 * @param ID identificativo del movimento da modificare
 * @return {@code true} se il prezzo viene modificato, {@code false} se l'operazione viene annullata
 *         o se non si arriva a un valore valido
 */
public static boolean GUIModificaPrezzo(Component c, String ID) {

    // Recupero la transazione da modificare.
    String[] trans = Principale.MappaCryptoWallet.get(ID);

    // Prezzo attualmente memorizzato sul movimento.
    String prezzo = trans[15];

    // Data del movimento, usata sia per la UI sia per eventuali conversioni USD -> EUR.
    long dataPrezzo = FunzioniDate.ConvertiDatainLongMinuto(trans[1]);

    // =========================
    // PARTE 1: valuta di input
    // =========================
    String monRiferimento = "EURO";

    if (dataPrezzo != 0) {
        AppDialog.DialogResult valutaResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Moneta di riferimento")
                .bodyTitle("Valuta del prezzo")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Indica se vuoi inserire il prezzo in EURO o in DOLLARI.")
                .details("""
                        Se scegli dollari, il prezzo verrà poi convertito in euro
                        usando il tasso di cambio della giornata di Banca d'Italia.
                        """)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("euro", "EURO")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .action(AppDialog.DialogAction.builder("dollari", "DOLLARI")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .showDialog();

        if (valutaResult == null) {
            return false;
        }

        String actionId = valutaResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return false;
        }

        if ("dollari".equals(actionId)) {
            monRiferimento = "DOLLARI";

            // Se il prezzo è già valorizzato, lo converto in dollari solo per
            // mostrarlo in modo coerente nel campo di input.
            if (prezzo != null) {
                String giorno = FunzioniDate.ConvertiDatadaLong(dataPrezzo);
                String val1Dollaro = Prezzi.CambioUSDEUR("1", giorno);

                prezzo = new BigDecimal(prezzo)
                        .divide(new BigDecimal(val1Dollaro), 2, RoundingMode.HALF_UP)
                        .toPlainString();
            }
        }
    }

    // ==============================================
    // PARTE 2: recupero del prezzo automatico
    // ==============================================
    c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    String prezzoAuto = Prezzi.DammiPrezzoDaTransazione(trans, 2);
    if (prezzoAuto == null) {
        prezzoAuto = "0.00";
    }
    c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    String messaggioInput = "Indica il prezzo in " + monRiferimento + " relativo alla transazione del " + trans[1] + ".";
    String dettagliInput = """
            Movimentazione: %s

            Il prezzo recuperato in automatico dal programma è pari a €%s.

            NB: il prezzo recuperato automaticamente potrebbe non coincidere con quello del CSV memorizzato nel programma.
            """.formatted(trans[6], prezzoAuto);

    // ======================================================
    // PARTE 3: input, validazione e conferma valore zero
    // ======================================================
    while (true) {
        AppDialog.DialogResult inputResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Modifica prezzo")
                .bodyTitle("Prezzo della transazione")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message(messaggioInput)
                .details(dettagliInput)
                .inputField("Prezzo attuale in Euro : "+prezzo,prezzo)
                .inputColumns(18)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("confirm", "Conferma")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (inputResult == null) {
            return false;
        }

        String actionId = inputResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return false;
        }

        String prezz = inputResult.getInputValue();
        if (prezz == null) {
            return false;
        }

        // Uniformo il formato decimale.
        prezz = prezz.replace(",", ".").trim();

        // Se il valore non è numerico, mostro l'errore e ripropongo il dialog.
        if (!Principale.Funzioni_isNumeric(prezz, false)) {
            AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                    .windowTitle("Valore non valido")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Attenzione, " + prezz + " non è un numero valido.")
                    .primaryAction("ok", "OK")
                    .showDialog();
            continue;
        }

        // Se l'utente ha inserito il valore in dollari, lo converto in euro.
        if (!"EURO".equals(monRiferimento)) {
            String giorno = FunzioniDate.ConvertiDatadaLong(dataPrezzo);
            prezz = Prezzi.CambioUSDEUR(prezz, giorno);
        }

        // Normalizzo il prezzo finale a due decimali.
        prezz = new BigDecimal(prezz)
                .setScale(2, RoundingMode.HALF_UP).abs()
                .toPlainString();

        // Se il prezzo finale è zero, richiedo conferma esplicita.
        if ("0.00".equals(prezz)) {
            AppDialog.DialogResult zeroResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                    .windowTitle("Conferma prezzo")
                    .bodyTitle("Prezzo a zero")
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Attenzione: il prezzo del movimento è valorizzato a 0.00.")
                    .details("Confermi questo valore? Il movimento verrà considerato come valorizzato a zero.")
                    .action(AppDialog.DialogAction.builder("no", "No")
                            .role(AppDialog.ActionRole.SECONDARY)
                            .build())
                    .action(AppDialog.DialogAction.builder("yes", "Sì")
                            .role(AppDialog.ActionRole.DANGER)
                            .build())
                    .showDialog();

            if (zeroResult == null) {
                AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                        .windowTitle("Operazione annullata")
                        .showTitleInBody(false)
                        .theme()
                        .type(AppDialog.DialogType.INFO)
                        .message("Operazione annullata.")
                        .primaryAction("ok", "OK")
                        .showDialog();
                return false;
            }

            String zeroActionId = zeroResult.getActionId();
            if (zeroActionId == null || "no".equals(zeroActionId)) {
                AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                        .windowTitle("Operazione annullata")
                        .showTitleInBody(false)
                        .theme()
                        .type(AppDialog.DialogType.INFO)
                        .message("Operazione annullata.")
                        .primaryAction("ok", "OK")
                        .showDialog();
                return false;
            }

            if ("yes".equals(zeroActionId)) {
                trans[15] = prezz;
                trans[32] = "SI";
                return true;
            }

            return false;
        }

        // Caso normale: salvo il prezzo e marco il movimento come modificato.
        trans[15] = prezz;
        trans[32] = "SI";
        return true;
    }
}

        /**
         * Versione legacy di {@link #GUIModificaPrezzo}, basata su {@link JOptionPane} invece che su {@code AppDialog}.
         * Segue lo stesso flusso: scelta valuta (EURO/DOLLARI), presentazione del prezzo automatico suggerito,
         * inserimento e validazione del nuovo prezzo, conferma esplicita se il valore finale è {@code "0.00"}.
         * @param c componente parent dei dialog
         * @param ID identificativo del movimento da modificare
         * @return {@code true} se il prezzo viene modificato, {@code false} se l'operazione viene annullata o il valore inserito non è valido
         */
        public static boolean ZZZ_GUIModificaPrezzo (Component c,String ID){
            
            //PARTE 1 -> Se conosco la data del movimento chiedo se voglio inserire il prezzo in dollari o in Euro
            //PARTE 2 -> Se specificato moneta e qta chiedo se voglio inserire il prezzo unitario o quello riferito al numero di token
            //PARTE 3 -> Chiedo di inserire l'importo e poi controllo che questo sia un numero 
            String trans[]=Principale.MappaCryptoWallet.get(ID);
            String Prezzo=trans[15];
            long DataPrezzo=FunzioniDate.ConvertiDatainLongMinuto(trans[1]);
            
            
            
            //PARTE 1
            int scelta=0;
            String MonRiferimento="EURO";
            String Testo;
            if (DataPrezzo!=0){
                Testo = "<html>Indica se vuoi imputare il prezzo in Euro o Dollari.<br><br>"
                            + "(Se scegli dollari il prezzo verrà poi convertito in Euro seguendo il tasso di cambio della giornata di bancha d'Italia)<br><br>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "EURO", "DOLLARI"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Moneta di riferimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                //EURO                           
                            }
                            case 2 -> {
                                //DOLLARO 
                                MonRiferimento="DOLLARI";
                                //Adesso trasformo il prezzo in dollari per presentarlo corretto nelle prossime schermate
                                if (Prezzo!=null){
                                    String Giorno=FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                                    String Val1Dollaro=Prezzi.CambioUSDEUR("1", Giorno);
                                    Prezzo=new BigDecimal(Prezzo).divide(new BigDecimal (Val1Dollaro), 2, RoundingMode.HALF_UP).toPlainString();
                                }
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return false;
                    }
            }
            
                    
            //PARTE 2    
            c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              String PrezzoAuto=Prezzi.DammiPrezzoDaTransazione(trans, 2);
              if (PrezzoAuto==null)PrezzoAuto="0.00";
              c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                Testo = "<html>Indicare il prezzo in <b>"+MonRiferimento+"</b> relativo alla transazione del <br><b>"
                        + trans[1]+"</b> relativo a questa movimentazione : (<b>"+trans[6]+"</b>)<br><br>"+
                        "Il prezzo recuperato in automatico dal programma è pari a <b>€"+PrezzoAuto+"</b><br>"+
                        "NB. Il prezzo recuperato in automatico potrebbe non essere uguale a quello del CSV memorizzato sul programma"
                            + "<br><br>"
                            + "</html>";
            
            
            String Prezz = JOptionPane.showInputDialog(c, Testo, Prezzo);
                if (Prezz != null) {
                    Prezz = Prezz.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (Principale.Funzioni_isNumeric(Prezz, false)) {
                        //Se dollari devo fare la conversione in euro
                        if (!MonRiferimento.equals("EURO")){
                            //devo fare la conversione da dollari a euro
                            String Giorno=FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                            Prezz=Prezzi.CambioUSDEUR(Prezz, Giorno);                           
                            //devo fare la conversione in dollari
                        }
                        Prezz=new BigDecimal(Prezz).setScale(2, RoundingMode.HALF_UP).toPlainString();
                        if (Prezz.equals("0.00")) {
                String Messaggio = "<html>Attenzione, il prezzo del movimento è valorizzato a '0.00'.<br>"
                        + "Si conferma questo valore? Il movimento verrà considerato come valorizzato a zero</html>";
                int risposta = JOptionPane.showOptionDialog(c, Messaggio, "Conferma Prezzo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Si", "No"}, "Si");
                //Si=0
                //No=1
                switch (risposta) {
                    case 0 -> {
                        trans[15]=Prezz;
                        trans[32]="SI";
                        return true;
                    }
                    case 1 -> {
                        JOptionPane.showConfirmDialog(c, "Operazione Annullata",
                                "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                    case -1 -> {
                        JOptionPane.showConfirmDialog(c, "Operazione Annullata",
                                "Operazione Annullata", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }

                }
            } else {
                trans[15]=Prezz;
                trans[32]="SI";
                return true;
            }

                    }else {
                        JOptionPane.showConfirmDialog(c, "Attenzione, " + Prezz + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                }
                return false;
        }
        
        
        
        
        /**
 * Mostra una sequenza di dialog per modificare o inserire un prezzo associato
 * a una coppia di monete, utilizzando AppDialog al posto di JOptionPane.
 *
 * <p>Il flusso è composto da queste fasi:</p>
 * <ol>
 *   <li>Se la data del prezzo è disponibile, chiede se l'importo verrà inserito in euro o in dollari.</li>
 *   <li>Recupera un prezzo automatico di supporto sulla base delle monete e della data.</li>
 *   <li>Chiede il prezzo, valida che sia numerico e restituisce il valore finale in euro.</li>
 * </ol>
 *
 * <p>Se l'utente annulla o chiude uno dei dialog, il metodo restituisce {@code null}.</p>
 *
 * @param c componente parent del dialog
 * @param MU moneta di uscita o principale, può essere null
 * @param ME moneta di entrata o secondaria, può essere null
 * @param Prezzo prezzo iniziale da proporre nel campo input
 * @param DataPrezzo data del prezzo in formato long
 * @param Rete rete di riferimento usata per il recupero automatico del prezzo
 * @return il prezzo finale in euro come stringa, oppure {@code null} se l'operazione viene annullata
 */
public static String GUIModificaPrezzo(Component c, Moneta MU, Moneta ME, String Prezzo, long DataPrezzo, String Rete) {

    // Converto la data in formato testuale per mostrarla nel dialog.
    String dataString = FunzioniDate.ConvertiDatadaLongAlSecondo(DataPrezzo);

    // =========================
    // PARTE 1: valuta di input
    // =========================
    String monRiferimento = "EURO";

    if (DataPrezzo != 0) {
        AppDialog.DialogResult valutaResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Moneta di riferimento")
                .bodyTitle("Valuta del prezzo")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Indica se vuoi inserire il prezzo in EURO o in DOLLARI.")
                .details("""
                        Se scegli dollari, il prezzo verrà poi convertito in euro
                        usando il tasso di cambio della giornata di Banca d'Italia.
                        """)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("euro", "EURO")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .action(AppDialog.DialogAction.builder("dollari", "DOLLARI")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .showDialog();

        if (valutaResult == null) {
            return null;
        }

        String actionId = valutaResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return null;
        }

        if ("dollari".equals(actionId)) {
            monRiferimento = "DOLLARI";

            // Se ho già un prezzo iniziale, lo converto in dollari solo per
            // mostrarlo correttamente nel campo input successivo.
            if (Prezzo != null) {
                String giorno = FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                String val1Dollaro = Prezzi.CambioUSDEUR("1", giorno);

                Prezzo = new BigDecimal(Prezzo)
                        .divide(new BigDecimal(val1Dollaro), 2, RoundingMode.HALF_UP)
                        .toPlainString();
            }
        }
    }

    // ==============================================
    // PARTE 2: recupero del prezzo automatico
    // ==============================================
    c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    Prezzi.InfoPrezzo IPT = Prezzi.DammiPrezzoInfoTransazione(ME, MU, DataPrezzo, Rete, "");

    if (ME == null) {
        ME = new Moneta();
        ME.Moneta = "";
    }

    if (MU == null) {
        MU = new Moneta();
        MU.Moneta = "";
    }

    BigDecimal bgPrezzoTot = new BigDecimal("0.00");
    if (IPT != null) {
        bgPrezzoTot = IPT.prezzoQta;
    }

    String prezzoAuto = "0.00";
    if (bgPrezzoTot != null) {
        prezzoAuto = bgPrezzoTot.toPlainString();
    }
    if (prezzoAuto == null) {
        prezzoAuto = "0.00";
    }

    c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    // ==========================
    // PARTE 3: inserimento input
    // ==========================
    String messaggioInput = "Indica il prezzo in " + monRiferimento + " relativo alla transazione del " + dataString + ".";
    String dettagliInput = """
            Movimentazioni coinvolte: %s %s

            Il prezzo recuperato in automatico dal programma è pari a €%s.

            NB: il prezzo recuperato automaticamente potrebbe non coincidere con quello del CSV memorizzato nel programma.
            """.formatted(MU.Moneta, ME.Moneta, prezzoAuto);

    // Ripeto il dialog finché l'utente inserisce un numero valido oppure annulla.
    while (true) {
        AppDialog.DialogResult inputResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Modifica prezzo")
                .bodyTitle("Prezzo della transazione")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message(messaggioInput)
                .details(dettagliInput)
                .inputField("Prezzo attuale in Euro : "+Prezzo,Prezzo)
                .inputColumns(18)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("confirm", "Conferma")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (inputResult == null) {
            return null;
        }

        String actionId = inputResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return null;
        }

        String prezz = inputResult.getInputValue();
        if (prezz == null) {
            return null;
        }

        // Uniformo il separatore decimale.
        prezz = prezz.replace(",", ".").trim();

        // Se il valore non è numerico, mostro l'errore e ripropongo l'input.
        if (!Principale.Funzioni_isNumeric(prezz, false)) {
            AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                    .windowTitle("Valore non valido")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Attenzione, " + prezz + " non è un numero valido.")
                    .primaryAction("ok", "OK")
                    .showDialog();
            continue;
        }

        // Se il prezzo è stato inserito in dollari, lo converto in euro.
        if (!"EURO".equals(monRiferimento)) {
            String giorno = FunzioniDate.ConvertiDatadaLong(DataPrezzo);
            prezz = Prezzi.CambioUSDEUR(prezz, giorno);
        }

        // Normalizzo il valore finale a due decimali.
        prezz = new BigDecimal(prezz)
                .setScale(2, RoundingMode.HALF_UP).abs()
                .toPlainString();

        return prezz;
    }
}
        
                /**
                 * Versione legacy di {@link #GUIModificaPrezzo(Component, Moneta, Moneta, String, long, String)},
                 * basata su {@link JOptionPane} invece che su {@code AppDialog}, per lo scambio tra due monete
                 * (usata ad esempio dal quadro RW). Stesso flusso: scelta valuta, prezzo automatico calcolato da
                 * {@link Prezzi#DammiPrezzoInfoTransazione}, inserimento e validazione del nuovo prezzo.
                 * @param c componente parent dei dialog
                 * @param MU moneta in uscita dello scambio
                 * @param ME moneta in entrata dello scambio
                 * @param Prezzo prezzo attuale mostrato come default nel campo di input
                 * @param DataPrezzo data/ora della transazione in millisecondi epoch
                 * @param Rete identificativo della blockchain/rete
                 * @return il nuovo prezzo in euro come stringa, oppure {@code null} se l'operazione viene annullata o il valore inserito non è valido
                 */
                public static String ZZZ_GUIModificaPrezzo (Component c,Moneta MU,Moneta ME,String Prezzo,long DataPrezzo,String Rete){
            
            //PARTE 1 -> Se conosco la data del movimento chiedo se voglio inserire il prezzo in dollari o in Euro
            //PARTE 2 -> Se specificato moneta e qta chiedo se voglio inserire il prezzo unitario o quello riferito al numero di token
            //PARTE 3 -> Chiedo di inserire l'importo e poi controllo che questo sia un numero 
           // String trans[]=CDC_Grafica.MappaCryptoWallet.get(ID);
           // String Prezzo=trans[15];
            //long DataPrezzo=OperazioniSuDate.ConvertiDatainLongMinuto(trans[1]);
            String DataString=FunzioniDate.ConvertiDatadaLongAlSecondo(DataPrezzo);
            
            
            //PARTE 1
            int scelta=0;
            String MonRiferimento="EURO";
            String Testo;
            if (DataPrezzo!=0){
                Testo = "<html>Indica se vuoi imputare il prezzo in Euro o Dollari.<br><br>"
                            + "(Se scegli dollari il prezzo verrà poi convertito in Euro seguendo il tasso di cambio della giornata di bancha d'Italia)<br><br>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "EURO", "DOLLARI"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Moneta di riferimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                //EURO                           
                            }
                            case 2 -> {
                                //DOLLARO 
                                MonRiferimento="DOLLARI";
                                //Adesso trasformo il prezzo in dollari per presentarlo corretto nelle prossime schermate
                                if (Prezzo!=null){
                                    String Giorno=FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                                    String Val1Dollaro=Prezzi.CambioUSDEUR("1", Giorno);
                                    Prezzo=new BigDecimal(Prezzo).divide(new BigDecimal (Val1Dollaro), 2, RoundingMode.HALF_UP).toPlainString();
                                }
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return null;
                    }
            }
            
                  
            //PARTE 2    
            c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Prezzi.InfoPrezzo IPT=Prezzi.DammiPrezzoInfoTransazione(ME, MU, DataPrezzo, Rete, "");
            if (ME==null) 
            {
                ME=new Moneta();
                ME.Moneta="";
            }
            if (MU==null) 
            {
                MU=new Moneta();
                MU.Moneta="";
            }
            BigDecimal BGPrezzoTot=new BigDecimal("0.00");
            if (IPT!=null) BGPrezzoTot=IPT.prezzoQta;
            String PrezzoAuto=("0.00");
            if (BGPrezzoTot!=null)PrezzoAuto=BGPrezzoTot.toPlainString();
              if (PrezzoAuto==null)PrezzoAuto="0.00";
              c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                Testo = "<html>Indicare il prezzo in <b>"+MonRiferimento+"</b> relativo alla transazione del <br><b>"
                        + DataString+"</b> contenente movimentazioni delle seguenti crypto : (<b>"+MU.Moneta+" "+ME.Moneta+"</b>)<br><br>"+
                        "Il prezzo recuperato in automatico dal programma è pari a <b>€"+PrezzoAuto+"</b><br>"+
                        "NB. Il prezzo recuperato in automatico potrebbe non essere uguale a quello del CSV memorizzato sul programma"
                            + "<br><br>"
                            + "</html>";
            
            
        String Prezz = JOptionPane.showInputDialog(c, Testo, Prezzo);
        if (Prezz != null) {
            Prezz = Prezz.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
            if (Principale.Funzioni_isNumeric(Prezz, false)) {
                //Se dollari devo fare la conversione in euro
                if (!MonRiferimento.equals("EURO")) {
                    //devo fare la conversione da dollari a euro
                    String Giorno = FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                    Prezz = Prezzi.CambioUSDEUR(Prezz, Giorno);
                    //devo fare la conversione in dollari
                }
                Prezz = new BigDecimal(Prezz).setScale(2, RoundingMode.HALF_UP).toPlainString();
                //System.out.println("Fine operazione");
                return Prezz;
                //return true;

            } else {
                JOptionPane.showConfirmDialog(c, "Attenzione, " + Prezz + " non è un numero valido!",
                        "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
            }
        }
        
        return null;
    }
        
        
        
       /**
        * Mostra un dialog con una {@link JTextArea} per modificare le note testuali di un movimento (campo 21).
        * I ritorni a capo inseriti vengono convertiti in {@code <br>} e i punto e virgola sostituiti con spazi
        * prima del salvataggio, per evitare di corrompere il formato interno del campo.
        * @param c componente parent del dialog (non utilizzato per il posizionamento, passato {@code null} come owner)
        * @param ID identificativo del movimento di cui modificare le note
        * @return {@code true} se le note sono state salvate, {@code false} se l'operazione è stata annullata
        */
       public static boolean GUIModificaNote(Component c,String ID) {
        // Crea una JTextArea
        String trans[]=Principale.MappaCryptoWallet.get(ID);
        String TestoArea=trans[21].replace("<br>", "\n");
        JTextArea textArea = new JTextArea(10, 30);  // 10 righe, 30 colonne
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(TestoArea);
        JScrollPane scrollPane = new JScrollPane(textArea);
        String[] options = {"Salva", "Annulla"};

        // Mostra un JOptionPane con la JTextArea
        int result = JOptionPane.showOptionDialog(
            null,
            scrollPane,
            "Note : ",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        // Gestione risultato
        if (result == 0) {
            String inputText = textArea.getText();
            inputText = inputText.replace(";", " ").replace("\n", "<br>");//Tolgo i caratteri che potrebbero dar fastidio alle note
            trans[21]=inputText;
            return true;
            //System.out.println("Hai scritto:\n" + inputText);
        } else {
           // System.out.println("Operazione annullata.");
        }
        return false;
    }
        
    /**
 * Mostra una sequenza di dialog per ottenere un prezzo da associare a un movimento,
 * utilizzando AppDialog al posto di JOptionPane.
 *
 * <p>Il flusso è composto da tre fasi:</p>
 * <ol>
 *   <li>Se la data del prezzo è disponibile, chiede se l'importo verrà inserito in euro o in dollari.</li>
 *   <li>Se sono disponibili nome moneta e quantità diversa da 1, chiede se il prezzo inserito
 *       sarà totale oppure unitario.</li>
 *   <li>Chiede l'importo, valida che sia numerico e restituisce sempre il prezzo finale in euro.
 *       Se il prezzo è unitario, calcola il totale moltiplicando per la quantità.</li>
 * </ol>
 *
 * <p>Se l'utente annulla in uno qualsiasi dei passaggi, il metodo restituisce {@code null}.</p>
 *
 * @param c componente parent del dialog
 * @param NomeMon nome della moneta/token, può essere null
 * @param DataPrezzo data del prezzo in formato long; se vale 0 non viene chiesta la valuta di riferimento
 * @param Qta quantità del token, può essere null
 * @param Prezzo prezzo iniziale da proporre nel campo input; se null viene usato "0"
 * @return il prezzo finale in euro come stringa, oppure {@code null} se l'operazione viene annullata
 */
public static String GUIDammiPrezzo(Component c, String NomeMon, long DataPrezzo, String Qta, String Prezzo) {

    // Se il prezzo iniziale non è valorizzato, propongo 0 come default.
    if (Prezzo == null) {
        Prezzo = "0";
    }

    // =========================
    // PARTE 1: valuta di input
    // =========================
    String monRiferimento = "EURO";

    if (DataPrezzo != 0) {
        AppDialog.DialogResult valutaResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Moneta di riferimento")
                .bodyTitle("Valuta del prezzo")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Indica se vuoi inserire il prezzo in EURO o in DOLLARI.")
                .details("""
                        Se scegli dollari, il prezzo verrà poi convertito in euro
                        usando il tasso di cambio della giornata di Banca d'Italia.
                        """)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("euro", "EURO")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .action(AppDialog.DialogAction.builder("dollari", "DOLLARI")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .showDialog();

        if (valutaResult == null) {
            return null;
        }

        String actionId = valutaResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return null;
        }

        if ("dollari".equals(actionId)) {
            monRiferimento = "DOLLARI";

            // Converto il prezzo iniziale da euro a dollari solo per mostrarlo
            // in modo coerente nel campo di input successivo.
            String giorno = FunzioniDate.ConvertiDatadaLong(DataPrezzo);
            String val1Dollaro = Prezzi.CambioUSDEUR("1", giorno);

            Prezzo = new BigDecimal(Prezzo)
                    .divide(new BigDecimal(val1Dollaro), 2, RoundingMode.HALF_UP)
                    .toPlainString();
        }
    }

    // ====================================
    // PARTE 2: prezzo totale o unitario
    // ====================================
    boolean prezzoUnitario = false;

    if (NomeMon != null && Qta != null && !Qta.equals("1")) {
        AppDialog.DialogResult tipoPrezzoResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Prezzo unitario o totale")
                .bodyTitle("Modalità di inserimento")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Per il token " + NomeMon + " vuoi indicare il prezzo totale o quello unitario?")
                .details("Se scegli il prezzo unitario, il totale verrà poi calcolato automaticamente dal programma.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("totale", "TOTALE")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .action(AppDialog.DialogAction.builder("unitario", "UNITARIO")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (tipoPrezzoResult == null) {
            return null;
        }

        String actionId = tipoPrezzoResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return null;
        }

        if ("unitario".equals(actionId)) {
            prezzoUnitario = true;
        }
    }

    // ==========================
    // PARTE 3: inserimento input
    // ==========================
    String messaggioInput;
    String dettagliInput = null;

    if (NomeMon != null && Qta != null) {
        if (prezzoUnitario) {
            messaggioInput = "Indica il prezzo unitario in " + monRiferimento + " relativo al token " + NomeMon + ".";
            dettagliInput = "Il prezzo totale verrà poi calcolato automaticamente dal programma.";
        } else {
            messaggioInput = "Indica il prezzo in " + monRiferimento + " relativo a " + Qta + " " + NomeMon + ".";
        }
    } else {
        messaggioInput = "Indica il prezzo in " + monRiferimento + ".";
    }

    // Ripeto il dialog finché l'utente inserisce un numero valido oppure annulla.
    while (true) {
        AppDialog.DialogResult inputResult = AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                .windowTitle("Inserimento prezzo")
                .bodyTitle("Valore del movimento")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message(messaggioInput)
                .details(dettagliInput)
                .inputField("Prezzo totale attuale in Euro : "+Prezzo,"")
                .inputColumns(18)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("confirm", "Conferma")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (inputResult == null) {
            return null;
        }

        String actionId = inputResult.getActionId();
        if (actionId == null || "cancel".equals(actionId)) {
            return null;
        }

        String prezz = inputResult.getInputValue();
        if (prezz == null) {
            return null;
        }

        // Uniformo i decimali sostituendo la virgola con il punto.
        prezz = prezz.replace(",", ".").trim();

        // Se non è un numero valido, mostro un messaggio e ripeto il dialog.
        if (!Principale.Funzioni_isNumeric(prezz, false)) {
            AppDialog.builder(SwingUtilities.getWindowAncestor(c))
                    .windowTitle("Valore non valido")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Attenzione, " + prezz + " non è un numero valido.")
                    .primaryAction("ok", "OK")
                    .showDialog();
            continue;
        }

        // Se l'importo è stato inserito in dollari, lo converto in euro.
        if (!"EURO".equals(monRiferimento)) {
            String giorno = FunzioniDate.ConvertiDatadaLong(DataPrezzo);
            prezz = Prezzi.CambioUSDEUR(prezz, giorno);
        }

        // Se il prezzo era unitario, lo trasformo in totale moltiplicando per la quantità.
        if (prezzoUnitario) {
            prezz = new BigDecimal(prezz)
                    .multiply(new BigDecimal(Qta))
                    .setScale(2, RoundingMode.HALF_UP).abs()
                    .toPlainString();
        }

        return prezz;
    }
}    
        
        /**
         * Versione legacy di {@link #GUIDammiPrezzo}, basata su {@link JOptionPane} invece che su {@code AppDialog}.
         * Stesso flusso: scelta valuta (se {@code DataPrezzo != 0}), scelta prezzo unitario/totale (se moneta e
         * quantità sono note e diverse da 1), inserimento e validazione dell'importo, con restituzione sempre in euro.
         * @param c componente parent dei dialog
         * @param NomeMon nome della moneta/token, può essere {@code null}
         * @param DataPrezzo data del prezzo in millisecondi epoch; se 0 non viene chiesta la valuta di riferimento
         * @param Qta quantità del token, può essere {@code null}
         * @param Prezzo prezzo iniziale da proporre nel campo input; se {@code null} viene usato {@code "0"}
         * @return il prezzo finale in euro come stringa, oppure {@code null} se l'operazione viene annullata
         */
        public static String ZZZGUIDammiPrezzo (Component c,String NomeMon,long DataPrezzo,String Qta,String Prezzo){
            
            //PARTE 1 -> Se conosco la data del movimento chiedo se voglio inserire il prezzo in dollari o in Euro
            //PARTE 2 -> Se specificato moneta e qta chiedo se voglio inserire il prezzo unitario o quello riferito al numero di token
            //PARTE 3 -> Chiedo di inserire l'importo e poi controllo che questo sia un numero 
            if (Prezzo==null)Prezzo="0";
           // String PRZ="0";
            
            
            
            //PARTE 1
            int scelta=0;
            String MonRiferimento="EURO";
            String Testo;
            if (DataPrezzo!=0){
                Testo = "<html>Indica se vuoi imputare il prezzo in Euro o Dollari.<br><br>"
                            + "(Se scegli dollari il prezzo verrà poi convertito in Euro seguendo il tasso di cambio della giornata di bancha d'Italia)<br><br>"
                            + "</html>";
                    Object[] Bottoni = {"Annulla", "EURO", "DOLLARI"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Moneta di riferimento",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                //EURO                           
                            }
                            case 2 -> {
                                //DOLLARO 
                                MonRiferimento="DOLLARI";
                                //Adesso trasformo il prezzo in dollari per presentarlo corretto nelle prossime schermate
                              //  if (Prezzo!=null){
                                    String Giorno=FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                                    String Val1Dollaro=Prezzi.CambioUSDEUR("1", Giorno);
                                    Prezzo=new BigDecimal(Prezzo).divide(new BigDecimal (Val1Dollaro), 2, RoundingMode.HALF_UP).toPlainString();
                              //  }
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return null;
                    }
            }
            
                    
            //PARTE 2
            boolean PrezzoUnitario=false;
            if (NomeMon!=null&&Qta!=null&&!Qta.equals("1")){
            Testo = "<html>Per il token "+NomeMon+" vuoi indicare il prezzo unitario o quello riferito al totale dei "+Qta+" pezzi?<br><br>"
                            + "(Se scegli di indicare il prezzo unitario il prezzo totale verrà poi calcolato dal programma)<br><br>"
                            + "</html>";
                    Object[] Bottoni2 = {"Annulla", "TOTALE", "UNITARIO"};
                    scelta = JOptionPane.showOptionDialog(c, Testo,
                            "Prezzo unitario o Totale",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            Bottoni2,
                            null);
                    //Adesso genero il movimento a seconda della scelta
                    //0 o 1 significa che non bisogna fare nulla
                    if (scelta != 0 && scelta != -1) {

                        switch (scelta) {
                            case 1 -> {
                                //TOTALE                           
                            }
                            case 2 -> {
                                //UNITARIO
                                PrezzoUnitario=true;
                            }
                            default -> {
                            }
                        }
                    }
                    else{
                        return null;
                    }
            }
            
            //PARTE 3
            if (NomeMon!=null&&Qta!=null){
                if (PrezzoUnitario)
                    Testo = "<html>indicare il prezzo <b>unitario</b> in "+MonRiferimento+" relativo al token "+NomeMon+"<br><br>"
                            + "(Il prezzo totale verrà poi calcolato dal programma)<br><br>"
                            + "</html>";
                else 
                    Testo = "<html>indicare il prezzo in "+MonRiferimento+" relativo a "+Qta+" "+NomeMon+"<br><br>"
                            + "<br><br>"
                            + "</html>";
            }
            else   
                Testo = "<html>indicare il prezzo in "+MonRiferimento+"<br><br>"
                            + "<br><br>"
                            + "</html>";
            
            
            String Prezz = JOptionPane.showInputDialog(c, Testo, Prezzo);
                if (Prezz != null) {
                    Prezz = Prezz.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                    if (Principale.Funzioni_isNumeric(Prezz, false)) {
                        //Se dollari devo fare la conversione in euro
                        if (!MonRiferimento.equals("EURO")){
                            //devo fare la conversione da dollari a euro
                            String Giorno=FunzioniDate.ConvertiDatadaLong(DataPrezzo);
                            Prezz=Prezzi.CambioUSDEUR(Prezz, Giorno);
                            //devo fare la conversione in dollari
                        }
                        //Se prezzo unitario poi devo moltiplicarlo per la quantità
                        if (PrezzoUnitario){
                            Prezz=new BigDecimal(Prezz).multiply(new BigDecimal(Qta)).setScale(2, RoundingMode.HALF_UP).toPlainString();
                        }                       
                        return Prezz;
                    }else {
                        JOptionPane.showConfirmDialog(c, "Attenzione, " + Prezz + " non è un numero valido!",
                            "Attenzione!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    }
                }
                
                return null;
        }
         
        
        /**
         * Verifica se la versione dell'applicazione salvata nell'opzione {@code Versione} è diversa da quella
         * indicata (o assente), e aggiorna comunque l'opzione con la versione corrente passata.
         * @param Versione versione corrente dell'applicazione
         * @return {@code true} se la versione salvata era assente o diversa da {@code Versione}
         */
        public static boolean CambiataVersione(String Versione){
            boolean VersioneCambiata=false;
            String Ver=DatabaseH2.Opzioni_Leggi("Versione");
            if (Ver==null){
                VersioneCambiata=true;
            }
            else {
                if (!Ver.equals(Versione)){
                    VersioneCambiata=true;
                }
            }
            DatabaseH2.Opzioni_Scrivi("Versione",Versione);
            return VersioneCambiata;
         }     
        
         
        
        /**
         * @param stringa stringa da normalizzare
         * @return {@code ""} se {@code stringa} è {@code null}, altrimenti {@code stringa} invariata
         */
        public static String TrasformaNullinBlanc(String stringa){
            if (stringa==null)return "";
            else return stringa;
        }
         
        /**
         * Rimuove da un nome i caratteri {@code ;} e {@code ,} che potrebbero interferire con la separazione
         * dei campi nei file CSV/interni.
         * @param Nome nome da normalizzare
         * @return il nome ripulito
         */
        public static String NormalizzaNome(String Nome){
            String NuovoNome=Nome.replace(";", "")
                   // .replace(" ", "")
                   // .replace(".", "")
                    .replace(",", "")
                   // .replace("_", "")
                   // .replace("\"", "")
                   // .replace("'", "")
                    ;    
            return NuovoNome;
         }
        
         /**
          * Come {@link #NormalizzaNome}, ma più aggressiva: rimuove anche {@code _} e le sequenze letterali
          * {@code \(} e {@code \)} (nota: essendo {@link String#replace(CharSequence, CharSequence)} non un
          * regex, le semplici parentesi {@code (}/{@code )} senza backslash non vengono rimosse).
          * @param Nome nome da normalizzare
          * @return il nome ripulito
          */
         public static String NormalizzaNomeStringente(String Nome){
            String NuovoNome=Nome.replace(";", "")
                   // .replace(" ", "")
                   // .replace(".", "")
                    .replace(",", "")
                    .replace("_", "")
                    .replace("\\(", "")
                    .replace("\\)", "")
                    ;    
            return NuovoNome;
         }
        
    /**
     * Duplica un movimento esistente in {@link Principale#MappaCryptoWallet}, assegnandogli un nuovo ID
     * ottenuto incrementando progressivamente il 4° segmento dell'ID originale (fino a 9 tentativi) finché
     * non se ne trova uno libero. Il duplicato viene marcato come aggiunto manualmente (campo 22 = {@code "M"}).
     * @param ID identificativo del movimento da duplicare
     * @return {@code true} se il duplicato è stato creato, {@code false} se l'ID non ha il formato atteso o non si trova un ID libero entro i tentativi previsti
     */
    public static boolean DuplicaMovimento(String ID){
        String riga[]=Principale.MappaCryptoWallet.get(ID);
        String nuovariga[]=riga.clone();
        String IDori=nuovariga[0];
        String idSplit[]=IDori.split("_");
        if (idSplit.length>4){
            for(int k=1;k<10;k++){
                String split3;
                if (Funzioni.isNumeric(idSplit[3], false)){
                    split3=String.valueOf((int)Double.parseDouble(idSplit[3])+k);
                    
                }
                else {split3=idSplit[3]+k;}
                String newID=idSplit[0]+"_"+idSplit[1]+"_"+idSplit[2]+"_"+split3+"_"+idSplit[4];
                if (Principale.MappaCryptoWallet.get(newID)==null){
                    //ho trovato un id libero, creo il nuovo movimento
                    nuovariga[0]=newID;//imposto il nuovo ID
                    nuovariga[22]="M";//Dico che il movimento è stato aggiunto manualmente
                    Principale.MappaCryptoWallet.put(newID, nuovariga);
                    return true;
                }
            }
        }
        return false;
    }   
        
    /**
     * Converte una stringa esadecimale (con o senza prefisso {@code 0x}) nel corrispondente {@link BigInteger}.
     * @param hexNumber stringa esadecimale da convertire
     * @return il valore decimale corrispondente
     * @throws IllegalArgumentException se {@code hexNumber} è {@code null} o vuota
     * @throws NumberFormatException se {@code hexNumber} non è una stringa esadecimale valida
     */
    public static BigInteger hexToDecimal(String hexNumber) {
        // Verifica se la stringa fornita è vuota o nulla
        if (hexNumber == null || hexNumber.isEmpty()) {
            throw new IllegalArgumentException("La stringa esadecimale non può essere vuota o nulla.");
        }

        // Rimuove il prefisso "0x" se presente
        if (hexNumber.startsWith("0x")) {
            hexNumber = hexNumber.substring(2);
        }

        // Converte la stringa esadecimale in decimale utilizzando il metodo parseLong della classe Long
        return new BigInteger(hexNumber , 16);
        
    }
        
    
        /**
     * Verifica se una stringa è un array JSON sintatticamente valido.
     * @param jsonString stringa da validare
     * @return {@code true} se {@code jsonString} è un {@link JSONArray} valido, {@code false} altrimenti
     */
    public static boolean isValidJSONArray(String jsonString) {
        try {
            new JSONArray(jsonString); // Prova a creare un JSONArray
            return true;
        } catch (JSONException e) {
            System.out.println("ERRORE : "+jsonString+" non è un array di stringhe valido");
            return false; // Se genera un'eccezione, non è un JSONArray valido
        }
    }
 
        
    /**
     * Esporta il contenuto di una {@link JTable} in un file Excel (.xlsx) nella cartella temporanei
     * ({@link VarStatiche#getCartella_Temporanei()}), nominato {@code Tabella_<timestamp>.xlsx}. Applica un
     * layout di colonne dedicato per {@code TabellaMovimentiCrypto} (35 colonne fisse, con la colonna 1
     * sostituita dalla data leggibile calcolata da {@link #getOradaID}) e per {@code RW_Tabella} (esclude la
     * colonna 6); per ogni altra tabella esporta tutte le colonne del model così come sono. L'HTML
     * eventualmente presente nelle celle/intestazioni viene ripulito tramite Jsoup prima della scrittura.
     * @param tabella la tabella da esportare
     */
    public static void Export_CreaExcelDaTabella(JTable tabella) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime now = LocalDateTime.now();
            String DataOra = now.format(formatter);

            File f = new File(VarStatiche.getCartella_Temporanei() + "Tabella_" + DataOra + ".xlsx");
            FileOutputStream fos = new FileOutputStream(f);
            Workbook wb = new Workbook(fos, "excel1", "1.0");
            Worksheet ws = wb.newWorksheet("Riepilogo Tabella ");

            TableModel model = tabella.getModel();
            //Scrivo l'intestazione della tabella riepilogo
            int NumColonne = tabella.getColumnCount();
            //System.out.println(model.getColumnCount());
            String NomeTabella = tabella.getName();
            if (NomeTabella != null && NomeTabella.equalsIgnoreCase("TabellaMovimentiCrypto")) {
                NumColonne = 35;
                String riga[] = new String[NumColonne];
                //Scrivo i titoli
                for (int i = 0; i < NumColonne; i++) {
                    String NomeColonna = model.getColumnName(i);
                    NomeColonna = Jsoup.parse(NomeColonna).text();
                    riga[i] = NomeColonna;
                }
                ScriviRigaExcel(riga, ws, 0);
                //Scrivo le righe
                for (int i = 0; i < tabella.getRowCount(); i++) {
                    int modelRow = tabella.convertRowIndexToModel(i);
                    riga = new String[NumColonne];
                    for (int k = 0; k < NumColonne; k++) {
                        if (k == 1) {
                            riga[k] = Funzioni.getOradaID(model.getValueAt(modelRow, 0).toString());
                            riga[k] = Jsoup.parse(riga[k]).text();
                        } else {
                            riga[k] = model.getValueAt(modelRow, k).toString();
                            riga[k] = Jsoup.parse(riga[k]).text();
                        }
                    }
                    ScriviRigaExcel(riga, ws, i + 1);

                }
                
            } 
            else if (NomeTabella != null && NomeTabella.equalsIgnoreCase("RW_Tabella")) {
                String riga[] = new String[NumColonne-1];
                //Scrivo i titoli
                int p=0;
                for (int i = 0; i < NumColonne; i++) {
                    if (i != 6) {
                        String NomeColonna = model.getColumnName(i);
                        NomeColonna = Jsoup.parse(NomeColonna).text();
                        riga[p] = NomeColonna;
                        p++;
                    }
                }
                ScriviRigaExcel(riga, ws, 0);
                //Scrivo le righe
                for (int i = 0; i < tabella.getRowCount(); i++) {
                    int modelRow = tabella.convertRowIndexToModel(i);
                    riga = new String[NumColonne-1];
                    p=0;
                    for (int k = 0; k < NumColonne; k++) {
                        if (k != 6) {
                            riga[p] = model.getValueAt(modelRow, k).toString();
                            riga[p] = Jsoup.parse(riga[p]).text();
                            p++;
                        }
                    }
                    ScriviRigaExcel(riga, ws, i + 1);

                }
            }
            else {
                String riga[] = new String[NumColonne];
                //Scrivo i titoli
                for (int i = 0; i < NumColonne; i++) {
                    String NomeColonna = model.getColumnName(i);
                    NomeColonna = Jsoup.parse(NomeColonna).text();
                    riga[i] = NomeColonna;
                }
                ScriviRigaExcel(riga, ws, 0);
                //Scrivo le righe
                for (int i = 0; i < tabella.getRowCount(); i++) {
                    int modelRow = tabella.convertRowIndexToModel(i);
                    riga = new String[NumColonne];
                    for (int k = 0; k < NumColonne; k++) {
                        riga[k] = model.getValueAt(modelRow, k).toString();
                        riga[k] = Jsoup.parse(riga[k]).text();
                    }
                    ScriviRigaExcel(riga, ws, i + 1);

                }
            }
            ws.finish();
            ws.close();
            wb.finish();
            wb.close();
            fos.close();
            Desktop desktop = Desktop.getDesktop();
            desktop.open(f);

        } catch (FileNotFoundException ex) {
            LoggerGC.ScriviErrore(ex);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
    
        /**
         * Verifica su SourceForge se esiste una release successiva alla versione indicata, incrementando di 1
         * il numero di patch (terzo segmento {@code X.Y.Z}) e controllando se la relativa pagina risponde 200 OK.
         * @param versione versione corrente, formato {@code X.Y.Z}
         * @return {@code true} se la pagina della versione successiva esiste, {@code false} altrimenti (inclusi gli errori di rete)
         */
        public static boolean isAggiornamentoDisponibile(String versione) {
        try {
            String split[]=versione.split("\\.");
            versione=split[0]+"."+split[1]+"."+String.valueOf(Integer.parseInt(split[2])+1);
            URL url = new URL("https://sourceforge.net/projects/giacenze-crypto-com/files/Giacenze_Crypto_"+versione+"/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Imposta il metodo e il timeout
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5 secondi
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            // Restituisce true solo se la risposta è 200 OK
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (IOException e) {
            // Qualsiasi eccezione indica che la pagina non è disponibile
            return false;
        }
    }    
        
        
    /**
     * Cancella tutti i file regolari in una directory la cui data di creazione risale a più di {@code Ore} ore fa.
     * Gli errori di accesso a singoli file o alla directory vengono loggati su console e non interrompono la scansione.
     * @param directory percorso della directory da ripulire
     * @param Ore soglia di età in ore oltre la quale un file viene cancellato
     */
    public static void Files_CancellaOltreTOTh(String directory, int Ore) {
        Path dir = Paths.get(directory);
        Instant now = Instant.now();
        System.out.println("Inizio procedura di pulizia file obsoleti in : "+directory);

        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        Instant creationTime = attrs.creationTime().toInstant();

                        Duration age = Duration.between(creationTime, now);
                        if (age.toHours() > Ore) {
                            Files.delete(path);
                            System.out.println("Cancellato file: " + path);
                        }
                    } catch (IOException e) {
                        System.err.println("Errore nella gestione del file: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Errore nell'accesso alla directory: " + directory + " - " + e.getMessage());
        }
    }    
        
        
        
        
    /**
     * Esporta in un file Excel (.xlsx, nella cartella temporanei) il dettaglio completo del quadro RW per un
     * anno: un foglio di riepilogo per gruppo wallet, e per ciascun gruppo un set di fogli con i calcoli RW
     * (da {@link Principale#Mappa_RW_ListeXGruppoWallet}), le giacenze di inizio/fine anno (da
     * {@link #RW_GiacenzeaData}, prezzate tramite {@link Prezzi#DammiPrezzoTransazione}) e l'elenco dei
     * movimenti dell'anno. I nomi dei gruppi vengono risolti tramite gli alias in
     * {@link DatabaseH2#Pers_GruppoAlias_LeggiTabella()}. Al termine apre il file con l'applicazione predefinita del sistema.
     * @param RW_Tabella la tabella RW da cui leggere il riepilogo per gruppo wallet
     * @param Anno anno di riferimento del quadro RW
     */
    public static void RW_CreaExcel(JTable RW_Tabella,String Anno){

        try {
            //Tabella Totali
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime now = LocalDateTime.now();
            String DataOra=now.format(formatter);  
            File f=new File (VarStatiche.getCartella_Temporanei()+"RW_"+Anno+"_"+DataOra+".xlsx");
            FileOutputStream fos = new FileOutputStream(f);
            Workbook wb = new Workbook(fos,"excel1","1.0");
            Worksheet wsrm=wb.newWorksheet("Riepilogo Anno "+Anno);
            wsrm.value(0, 0,"Riepilogo Anno "+Anno);
            String Intestazione[]=new String []{"RW","Valore Iniziale","Valore Finale","Giorni di detenzione","Errori",
                    "IC Dovuta","Bollo pagato"};
                ScriviRigaExcel(Intestazione,wsrm,1);
            
        // Per prima cosa creo la prima riga di intestazione
        Map<String, String[]> Mappa_Gruppo_Alias = DatabaseH2.Pers_GruppoAlias_LeggiTabella();
            Worksheet ws,wsI,wsF,wsM;
            TableModel model = RW_Tabella.getModel();
            //Scrivo l'intestazione della tabella riepilogo
            
            for (int i = 0; i < RW_Tabella.getRowCount(); i++) {
                //Popolo il primo worksheet con il riepilogo
                String RigaRiepilogo[]=new String[7];
                RigaRiepilogo[0]=model.getValueAt(i, 0).toString();
                RigaRiepilogo[1]=model.getValueAt(i, 1).toString();
                RigaRiepilogo[2]=model.getValueAt(i, 2).toString();
                RigaRiepilogo[3]=model.getValueAt(i, 3).toString();
                RigaRiepilogo[4]=model.getValueAt(i, 4).toString();
                RigaRiepilogo[5]=model.getValueAt(i, 5).toString();
                RigaRiepilogo[6]=model.getValueAt(i, 7).toString();
                ScriviRigaExcel(RigaRiepilogo,wsrm,i+2);
                
                
                //Creao i Worksheet relativi ai dettagli per il calcolo dell'RW
                String GruppoW=model.getValueAt(i, 0).toString();
                ws = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Calcoli RW");
                wsI = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Inizio "+Anno);
                wsF = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Fine "+Anno);
                wsM = wb.newWorksheet(GruppoW.split("\\(")[0].trim()+" - Movimenti "+Anno);
                String Gruppo = "Wallet " + model.getValueAt(i, 0).toString().split(" ")[0].trim();
                
                
                String Intestazioni[]=new String []{"Gruppo Iniziale","Moneta Iniziale","Qta Inizale","Data Iniziale","Valore Iniziale",
                    "Gruppo Finale","Moneta Finale","Qta Finale","Data Finale","Valore Finale","Giorni di detanzione","Motivo fine detenzione"};
                int colonna = 0;
                ScriviRigaExcel(Intestazioni,ws,0);
                int riga=1;
                for (String[] lista : Principale.Mappa_RW_ListeXGruppoWallet.get(Gruppo)) {
                    //Sistemo i nomi dei gruppi Wallet
                    if (Mappa_Gruppo_Alias.get(lista[1]) != null) {
                        lista[1] = lista[1].split(" ")[1].trim() + " ( " + Mappa_Gruppo_Alias.get(lista[1])[1] + " )";
                    }
                    if (Mappa_Gruppo_Alias.get(lista[6]) != null) {
                        lista[6] = lista[6].split(" ")[1].trim() + " ( " + Mappa_Gruppo_Alias.get(lista[6])[1] + " )";
                    }
                    //Scrivo i dati sull'excel
                    colonna = 0;
                    
                    for (String Valore : lista) {
                        if (colonna>0&&colonna<13){//Scrivo solo le colonne con idati che mi interessano
                        if (isNumeric(Valore,false)) {
                            double val = Double.parseDouble(Valore);
                            ws.value(riga, colonna-1, val);
                        }else{
                             ws.value(riga, colonna-1, Valore);
                        }}
                        colonna++;
                        
                    }
                    riga++;
                }
                //Adesso creao il worksheet relativo alle giacenze del wallet di inizio e fine anno
                wsI.value(0, 0, GruppoW+" - Giacenze Inizio "+Anno);
                wsF.value(0, 0, GruppoW+" - Giacenze Fine "+Anno);
                Intestazioni=new String []{"Nome","Rete","Address DeFi del Token","Tipo","Quantità",
                    "Valore in Euro","Valore Unirtario","Note"};
                ScriviRigaExcel(Intestazioni,wsI,1);
                ScriviRigaExcel(Intestazioni,wsF,1);
                //long DataRiferimento=0;
                String DataInizio=Anno+"-01-01";
                String DataFine=Anno+"-12-31";
                long lDataFine=FunzioniDate.ConvertiDatainLong(DataFine)+86400000;
                long lDataInizio=FunzioniDate.ConvertiDatainLong(DataInizio);
                String GruppoWNormalizzato="Wallet "+GruppoW.split(" ")[0];
                List<String[]> ListaSaldiIniziali=RW_GiacenzeaData(lDataInizio,GruppoWNormalizzato,"");
                List<String[]> ListaSaldiFinali=RW_GiacenzeaData(lDataFine,GruppoWNormalizzato,"");
                int r=2;
                //Iterator<String[]> Iniziali=ListaSaldiIniziali.iterator();
                //Iterator<String[]> Finali=ListaSaldiFinali.iterator();
                //Saldi Iniziali
                for (int ii=0;ii<ListaSaldiIniziali.size();ii++){
                    String valori[]=ListaSaldiIniziali.get(ii);
                        String[] copy = new String[valori.length + 2];
                        System.arraycopy(valori, 0, copy, 0, valori.length);
                        valori = copy;
                        String Rete=valori[1];
                        Moneta M1=new Moneta();
                        M1.Moneta=valori[0];
                        M1.MonetaAddress=valori[2];
                        M1.Qta="1";
                        M1.Tipo=valori[3];
                        M1.Rete=Rete;
                        valori[6]=Prezzi.DammiPrezzoTransazione(M1,null,lDataInizio, null,true,30,Rete,"");
                    if (valori[0].contains(" **"))
                    {
                        valori[7]="Token SCAM";
                    }
                    ScriviRigaExcel(valori,wsI,r);
                    r++;
                }
                r=2;
                //Saldi Finali
                for (int ii=0;ii<ListaSaldiFinali.size();ii++){
                    String valori[]=ListaSaldiFinali.get(ii);
                        String[] copy = new String[valori.length + 2];
                        System.arraycopy(valori, 0, copy, 0, valori.length);
                        valori = copy;
                        String Rete=valori[1];
                        Moneta M1=new Moneta();
                        M1.Moneta=valori[0];
                        M1.MonetaAddress=valori[2];
                        M1.Qta=new String("1");
                        M1.Tipo=valori[3];
                        M1.Rete=Rete;
                        valori[6]=Prezzi.DammiPrezzoTransazione(M1,null,lDataFine, null,true,30,Rete,"");
                    if (valori[0].contains(" **"))
                    {
                        valori[7]="Token SCAM";
                    }
                    ScriviRigaExcel(valori,wsF,r);
                    r++;
                }
                
                //Lista Movimenti
                Intestazioni = new String[]{
                    "Gruppo Wallet x RW",               //
                    "ID",                               //0
                    "Data",                             //1
                    "Wallet Principale/Exchange",       //3
                    "Dettaglio Wallet",                 //4
                    "Rete Wallet",                      //da calcolare
                    "Tipo Transazione",                 //5
                    "Token Venduto/Ceduto",             //8
                    "Tipo Token Venduto/Ceduto",        //9
                    "Address Token Venduto/Ceduto",     //26
                    "Quantità Venduta/Ceduta",          //10
                    "Token Acquistato/Ricevuto",        //11
                    "Tipo Token Acquistato/Ricevuto",   //12
                    "Address Token Acquistato/Ricevuto",//28
                    "Quantità Acquistata/Ricevuta",     //13
                    "Valore Transazione",               //15
                    "Riferimento Trasferimenti",        //20
                    "Note"                              //21
                };       
                ScriviRigaExcel(Intestazioni,wsM,0);
                int rli=1;
                for (String[] v : MappaCryptoWallet.values()) {
                    String AnnoRiga=v[1].split("-")[0];
                    String GruppoRiga=DatabaseH2.Pers_GruppoWallet_Leggi(v[3],true);
                    //se l'anno è quello di riferimento e il gruppo walle è quello analizzato allora scrivo i movimenti sull'excel
                    if(AnnoRiga.equals(Anno)&&
                            GruppoRiga.split(" ")[1].equals(GruppoW.split(" ")[0])){
                    String rigaT[]=new String[18];
                    rigaT[0]=GruppoW;
                    rigaT[1]=v[0];
                    rigaT[2]=v[1];
                    rigaT[3]=v[3];
                    rigaT[4]=v[4];
                    rigaT[5]=TrovaReteDaID(v[0]);
                    rigaT[6]=v[5];
                    rigaT[7]=v[8];
                    rigaT[8]=v[9];
                    rigaT[9]=v[26];
                    rigaT[10]=v[10];
                    rigaT[11]=v[11];
                    rigaT[12]=v[12];
                    rigaT[13]=v[28];
                    rigaT[14]=v[13];
                    rigaT[15]=v[15];
                    rigaT[16]=v[20];
                    rigaT[17]=v[21];
                    ScriviRigaExcel(rigaT,wsM,rli);
                    rli++;
                    }
                }
                
                
                wsI.finish();
                wsF.finish();
                ws.finish();
                wsI.close();
                wsF.close();
                ws.close();
            }
            wsrm.finish();
            wsrm.close();
            wb.finish();
            wb.close();
            fos.close();
            Desktop desktop = Desktop.getDesktop();
            desktop.open(f);
            
        } catch (FileNotFoundException ex) {
            LoggerGC.ScriviErrore(ex);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     * Costruisce una mappa gruppo wallet → ID del primo movimento incontrato per quel gruppo, scorrendo
     * {@link Principale#MappaCryptoWallet} nell'ordine di iterazione della mappa.
     * @return la mappa gruppo wallet → ID del primo movimento associato
     */
    public static Map<String, String>  MappaPrimoMovimentoXGruppoWallet() {
        Map<String, String> Mappa_Gruppi = new TreeMap<>();//la mappa è così composta, (Gruppo,ID Primo Movimento)
        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3],true);
            if (Mappa_Gruppi.get(GruppoWallet)==null)Mappa_Gruppi.put(GruppoWallet, v[0]);
        }
        return Mappa_Gruppi;
    }
    
    
    /**
     * Scrive un array di valori come riga di un {@link Worksheet}, convertendo automaticamente in numero
     * (double) le stringhe che rappresentano valori numerici e lasciando le altre come testo.
     * @param Valori valori da scrivere, una cella per elemento dell'array
     * @param ws foglio Excel su cui scrivere
     * @param riga indice della riga di destinazione (0-based)
     */
    public static void ScriviRigaExcel(String Valori[], Worksheet ws, int riga) {
        int colonna = 0;
        for (String Valore : Valori) {
            if (isNumeric(Valore, false)) {
                double val = Double.parseDouble(Valore);
                ws.value(riga, colonna, val);
            } else {
                ws.value(riga, colonna, Valore);
            }
            colonna++;
        }
    }
    
    
    

    
    
    
        /**
         * Calcola le giacenze per moneta di un wallet (o gruppo wallet) a una data di riferimento, sommando
         * tutti i movimenti precedenti a quella data che coinvolgono il wallet/sottowallet indicato (o l'intero
         * gruppo wallet a cui appartiene, secondo {@link DatabaseH2#Pers_GruppoWallet_Leggi}), e valorizzandole
         * in euro tramite {@link Prezzi#DammiPrezzoTransazione}. Le giacenze a zero vengono escluse dal risultato.
         * @param DataRiferimento data di riferimento (esclusiva: solo movimenti strettamente precedenti), millisecondi epoch
         * @param Wallet nome del wallet o del gruppo wallet, oppure {@code "tutti"} per includere tutti i wallet
         * @param SottoWallet nome del sottowallet, oppure {@code "tutti"} per includere tutti i sottowallet del wallet indicato
         * @return la lista delle giacenze, ciascuna come array {@code [moneta, rete, address, tipo, quantità, valore in euro]}
         */
        public static List<String[]> RW_GiacenzeaData(long DataRiferimento,String Wallet,String SottoWallet){
            //Nel wallet si può mettere il nome del gruppo Wallet
        
         List<String[]> ListaSaldi=new ArrayList<>();  
        //Compilo la mappa QtaCrypto con la somma dei movimenti divisa per crypto
        //in futuro dovrò mettere anche un limite per data e un limite per wallet
        //progress.RipristinaStdout();
        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
        Map<String, Moneta> QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto il bigdecimal con la qta
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaIMovimento(movimento);
                    long DataMovimento = FunzioniDate.ConvertiDatainLong(movimento[1]);
                    if (DataMovimento < DataRiferimento) {
                        if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                                || (Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase("tutti"))//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                                ||(Wallet.equalsIgnoreCase(movimento[3].trim())&&SottoWallet.equalsIgnoreCase(movimento[4].trim()))//Se wallet e sottowallet corrispondono a quelli analizzati proseguo
                                ||DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3],true).equals(Wallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                                ) {
                            // GiacenzeaData_Wallet_ComboBox.getSelectedItem()
                            //Faccio la somma dei movimenti in usicta
                            Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                            //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                            Monete[0] = new Moneta();
                            Monete[1] = new Moneta();
                            Monete[0].MonetaAddress = movimento[26];
                            Monete[1].MonetaAddress = movimento[28];
                            //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
                            Monete[0].Moneta = movimento[8];
                            Monete[0].Tipo = movimento[9];
                            Monete[0].Qta = movimento[10];
                            Monete[0].Rete=Rete;
                            Monete[1].Moneta = movimento[11];
                            Monete[1].Tipo = movimento[12];
                            Monete[1].Qta = movimento[13];
                            Monete[1].Rete=Rete;
                            //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                            for (int a = 0; a < 2; a++) {
                                //ANALIZZO MOVIMENTI
                                if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress)!=null) {
                                    //Movimento già presente da implementare
                                    Moneta M1 = QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress);
                                    M1.Qta = new BigDecimal(M1.Qta)
                                            .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                                } else if (!Monete[a].Moneta.isBlank()) {
                                    //Movimento Nuovo da inserire
                                    Moneta M1 = new Moneta();
                                    M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                    M1.Rete = Rete;
                                    QtaCrypto.put(Monete[a].Moneta+";"+Monete[a].Tipo+";"+Monete[a].MonetaAddress, M1);

                                }
                            }
                        }
                    }
                }
        
        //Adesso elenco tutte le monete e le metto il tutto in una lista   
        
        int i=0;
        BigDecimal TotEuro=new BigDecimal(0);
        for (String moneta :QtaCrypto.keySet()){
            i++;
            Moneta M1=QtaCrypto.get(moneta);
            String Rete=M1.Rete;
            String Address=M1.MonetaAddress;
            String riga[]=new String[6];
            riga[0]=M1.Moneta;
            riga[2]=Address;//qui ci va l'address della moneta se non sto analizzando i wallet nel complesso
            riga[3]=M1.Tipo;
            riga[4]=M1.Qta;
            riga[1]=M1.Rete;
            if (!M1.Qta.equals("0"))
            {
                if (M1.Qta.equals("0"))riga[5]="0.00";
                else riga[5]=Prezzi.DammiPrezzoTransazione(M1,null,DataRiferimento, null,true,2,Rete,"");
                if (riga[4].contains("-")&&!riga[5].equals("0.00"))riga[5]="-"+riga[5];
                ListaSaldi.add(riga);                
            }
            
        }       
return ListaSaldi;
}
    
      /**
       * Calcola, per ciascun gruppo wallet, le giacenze crypto di inizio e fine anno (con relativo prezzo in
       * euro tramite {@link Prezzi#DammiPrezzoTransazione}), producendo una riga per ogni moneta con
       * quantità/prezzo iniziali e finali, giorni di detenzione e causale (che distingue tra "Fine Anno" e
       * "Apertura Wallet/Fine Anno" per la prima moneta ricevuta dal wallet). Se il wallet ha ricevuto il suo
       * primo movimento durante l'anno stesso, la giacenza iniziale parte da quella data e non dal 1° gennaio.
       * @param Anno anno di riferimento, formato {@code yyyy}
       * @return mappa gruppo wallet → lista di righe di dettaglio (una per moneta con giacenza diversa da zero in almeno uno dei due estremi)
       */
      public static Map<String, List<String[]>> RW_GiacenzeInizioFineAnno(String Anno){
            //Nel wallet si può mettere il nome del gruppo Wallet
            /*
            0 -> Anno
            1 -> Gruppo Wallet Inizio Anno (Sarà sempre
            2 -> Nome Moneta
            */
        String DataInizio=Anno+"-01-01";
        String DataFine=Anno+"-12-31";
        
        Map<String, String[]> MappaDataPartenza= new TreeMap<>();
        long lDataFine=FunzioniDate.ConvertiDatainLong(DataFine)+86400000;
        long lDataInizio=FunzioniDate.ConvertiDatainLong(DataInizio);
          
        //Compilo la mappa QtaCrypto con la somma dei movimenti divisa per crypto
        //FASE 2 THREAD : CREO LA NUOVA MAPPA DI APPOGGIO PER L'ANALISI DEI TOKEN
        Map<String,Map<String, Moneta[]>> MappaCoinsWallet = new TreeMap<>();//Map<GruppoWallet,Map<NomeMoneta,Monete(Inizio e Fine anno)>>
        Map<String, Moneta[]> QtaCrypto;//nel primo oggetto metto l'ID, come secondo oggetto metto il bigdecimal con la qta
        //Moneta[0]->Qta Inizio Anno ----- Moneta[1]->Qta Fine Anno
            boolean PrimoMovimento;
                for (String[] movimento : MappaCryptoWallet.values()) {
                    //Prima cosa controllo che le date corrispondano all'ID
                    if(!movimento[0].substring(0, 4).equals(movimento[1].substring(0, 4))){
                        // LoggerGC.logInfo("ID diverso da data --- "+movimento[0]+ " --- "+movimento[1],"Funzioni.RW_GiacenzeInizioFineAnno");
                    }
                    
                    String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3],true);
                    //1 - Inizializzo le Mappe
                    if (MappaCoinsWallet.get(GruppoWallet)==null)
                    {
                       // LoggerGC.logInfo("Wallet - "+GruppoWallet+" - ID Iniziale --- "+movimento[0],"Funzioni.RW_GiacenzeInizioFineAnno");
                        QtaCrypto = new TreeMap<>();
                        MappaCoinsWallet.put(GruppoWallet, QtaCrypto);
                        String DataPartenza_Prezzo_ID[]=new String[]{movimento[1],movimento[15],movimento[0],movimento[11]};
                        MappaDataPartenza.put(GruppoWallet, DataPartenza_Prezzo_ID);
                        PrimoMovimento=true;
                    }else{
                        QtaCrypto = MappaCoinsWallet.get(GruppoWallet);
                        PrimoMovimento=false;
                    }
                    
                    
                    //2 - 
                    //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
                    //altrimenti non vado avanti
                    String Rete = Funzioni.TrovaReteDaIMovimento(movimento);
                    long DataMovimento = FunzioniDate.ConvertiDatainLong(movimento[1]);


                            // GiacenzeaData_Wallet_ComboBox.getSelectedItem()
                            //Faccio la somma dei movimenti in usicta
                           Moneta Monete[]=RitornaMoneteDaID(movimento[0]);
                            //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                            for (int a = 0; a < 2; a++) {
                                //ANALIZZO MOVIMENTI
                                if (!Monete[a].Moneta.isBlank() && Monete[a].Tipo.equalsIgnoreCase("Crypto") && QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo)!=null) {
                                    //Movimento già presente da implementare
                                    Moneta M[] = QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo);
                                    //if (Monete[a].Moneta.equals("ACE"))System.out.println("Aggiunta"+Monete[a].Qta);
                                    if (DataMovimento < lDataInizio) {
                                    M[0].Qta = new BigDecimal(M[0].Qta)
                                            .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();
                                    }if (DataMovimento < lDataFine) {
                                    M[1].Qta = new BigDecimal(M[1].Qta)
                                            .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();                                   
                                    }
                                    
                                    //Qui se nnon ho anora la moneta in pancia
                                } else if (!Monete[a].Moneta.isBlank() && Monete[a].Tipo.equalsIgnoreCase("Crypto")) {
                                    //if (Monete[a].Moneta.equals("ACE"))System.out.println("Inserimento"+Monete[a].Qta);
                                    //Movimento Nuovo da inserire
                                    Moneta M[]=new Moneta[2];
                                    M[0]=new Moneta();
                                    M[1]=new Moneta();
                                    if (DataMovimento < lDataInizio) {
                                        M[0].InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                        M[0].Rete= Rete;
                                        M[1].InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                        M[1].Rete= Rete;
                                    }
                                    if (DataMovimento < lDataFine && DataMovimento >= lDataInizio) {
                                        //Se è il primo movimento in assoluto il valore di inizio anno lo valorizzo
                                        //A questo movimento e non a zero
                                        if (PrimoMovimento){
                                            M[0].InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                            M[0].Rete= Rete;
                                        }
                                        else{
                                           M[0].InserisciValori(Monete[a].Moneta, "0", Monete[a].MonetaAddress, Monete[a].Tipo);
                                           M[0].Rete= Rete; 
                                        }
                                        M[1].InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                        M[1].Rete= Rete;
                                    }
                                    if (DataMovimento < lDataFine){
                                        QtaCrypto.put(Monete[a].Moneta+";"+Monete[a].Tipo, M);
                                    }

                                }
                            }
                         //  MappaCoinsWallet.put(Rete, QtaCrypto);
                        
                    
                }
        //LoggerGC.logInfo("-------------------------------------------------------------------","Funzioni.RW_GiacenzeInizioFineAnno");
        //Adesso elenco tutte le monete e le metto il tutto in una lista   
        Map<String, List<String[]>> MappaLista= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String[]> lista;
        
        
        for(String GWallet:MappaCoinsWallet.keySet()){
            int DiffDate=FunzioniDate.DifferenzaDate(DataInizio, DataFine)+1;
           // LoggerGC.logInfo("DiffDate1 - "+DataInizio+" - "+DataFine+" - "+DiffDate,"Funzioni.RW_GiacenzeInizioFineAnno");
            Map<String, Moneta[]> QtaCrypt=MappaCoinsWallet.get(GWallet);
            lista=RW_RitornaListaDaMappa(MappaLista,GWallet);
          /*  if(MappaLista.get(GWallet)==null){
                lista=new ArrayList<>();
                MappaLista.put(GWallet, lista);
            }else lista=MappaLista.get(GWallet);*/
            
            String DataInizioWallet=MappaDataPartenza.get(GWallet)[0];
            long lDataInizioWallet=FunzioniDate.ConvertiDatainLongMinuto(DataInizioWallet);
            long lDataInizio1=lDataInizio;
            String DataInizio1=DataInizio+" 00:00";
            if (lDataInizioWallet > lDataInizio) {
                DiffDate = FunzioniDate.DifferenzaDate(FunzioniDate.ConvertiDatadaLong(lDataInizioWallet), DataFine) + 1;
                //LoggerGC.logInfo("DiffDate2 - "+GWallet+" - "+lDataInizioWallet+" - "+DataFine+" - "+DiffDate,"Funzioni.RW_GiacenzeInizioFineAnno");
                //LoggerGC.logInfo("Wallet - "+GWallet+" - Data Inizio --- "+DataInizio1,"Funzioni.RW_GiacenzeInizioFineAnno");
                DataInizio1 = DataInizioWallet;
                lDataInizio1 = FunzioniDate.ConvertiDatainLongMinuto(DataInizio1);
            }           
            //LoggerGC.logInfo("Wallet - "+GWallet+" - Data Inizio --- "+DataInizio1,"Funzioni.RW_GiacenzeInizioFineAnno");
            for(Moneta m[]:QtaCrypt.values()){
                if(!(m[0].Qta.equals("0")&&m[1].Qta.equals("0")))
                {
                    String DicituraInizio="Giacenza Inizio Anno";
                    String Causale="Fine Anno";
                    if (m[0].Moneta.equals(MappaDataPartenza.get(GWallet)[3])) {                        
                        Causale = "Apertura Wallet/Fine Anno";
                        DicituraInizio = MappaDataPartenza.get(GWallet)[2];
                    }                   
                    if(m[0].Qta.equals("0"))m[0].Prezzo="0.0000";
                    else m[0].Prezzo=Prezzi.DammiPrezzoTransazione(m[0], null, lDataInizio1, null, false, 15, m[0].Rete,"");
                    if(m[1].Qta.equals("0"))m[1].Prezzo="0.0000";
                    else m[1].Prezzo=Prezzi.DammiPrezzoTransazione(m[1], null, lDataFine, null, false, 15, m[1].Rete,"");
                    String xlista[]=new String[17];
                    xlista[0]=Anno;                                             //Anno RW
                    xlista[1]=GWallet;                                          //Gruppo Wallet Inizio
                    xlista[2]=m[0].Moneta;                                      //Moneta Inizio
                    xlista[3]=m[0].Qta;                                         //Qta Inizio
                    xlista[4]=DataInizio1;                                      //Data Inizio
                    xlista[5]=m[0].Prezzo;                                      //Prezzo Inizio
                    xlista[6]=GWallet;                                          //GruppoWallet Fine
                    xlista[7]=m[1].Moneta;                                      //Moneta Fine
                    xlista[8]=m[1].Qta;                                         //Qta Fine
                    xlista[9]=DataFine+" 23:59";                                //Data Fine
                    xlista[10]=m[1].Prezzo;                                     //Prezzo Fine
                    xlista[11]=String.valueOf(DiffDate);                        //Giorni di Detenzione
                    xlista[12]=Causale;                                         //Causale
                    xlista[13]=DicituraInizio;                                  //ID Movimento Apertura (o segnalazione inizio anno)
                    xlista[14]="Giacenza Fine Anno";                            //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                    xlista[15]="";                                              //Tipo Errore
                    xlista[16]="";                                              //Lista ID coinvolti separati da virgola
                    lista.add(xlista);
                    
                    //System.out.println(GWallet+";"+m[0].Moneta+";"+m[0].Qta+";"+m[0].Prezzo+";"+m[1].Qta+";"+m[1].Prezzo); 
                }
            }
            
        }
        Calcoli_RW.SistemaErroriInListe(MappaLista);
return MappaLista;
}
      
    private static List<String[]> RW_RitornaListaDaMappa(Map<String, List<String[]>> MappaLista,String Key){
        List<String[]> lista;
            if(MappaLista.get(Key)==null){
                lista=new ArrayList<>();
                MappaLista.put(Key, lista);
            }else lista=MappaLista.get(Key);
        return lista;
    }
    
    
    /**
     * Individua le combinazioni exchange/wallet/token per cui il saldo netto delle sole uscite (movimenti in
     * cui il token compare come token ceduto) risulta negativo in un qualsiasi momento della cronologia,
     * segnale tipico di un movimento in entrata mancante o mal classificato.
     * @return la lista ordinata delle chiavi {@code exchange;wallet;token} segnalate come saldo negativo
     */
    public static List<String> ControllaSaldiNegativi(){
        return ControllaSaldiNegativi(MappaCryptoWallet.values());
    }

    /**
     * Come {@link #ControllaSaldiNegativi()}, ma sui movimenti passati invece che direttamente su
     * {@code MappaCryptoWallet}. Serve a poter eseguire il calcolo <b>fuori dall'EDT</b> su una
     * copia della collezione scattata sull'EDT: iterare la mappa viva da un altro thread mentre
     * l'EDT la modifica produrrebbe una {@code ConcurrentModificationException}.
     * <p>
     * Le righe restano condivise con la mappa (si copiano i riferimenti, non i contenuti): è
     * sicuro perché qui si leggono solo i campi 3, 4, 8, 10, 11 e 13, mentre il motore delle
     * plusvalenze scrive esclusivamente 16, 17, 19, 33 e 38.
     *
     * @param movimenti movimenti da esaminare
     * @return la lista ordinata delle chiavi {@code exchange;wallet;token} con saldo negativo
     */
    public static List<String> ControllaSaldiNegativi(java.util.Collection<String[]> movimenti){

         Map<String, BigDecimal> saldi = new HashMap<>();
        Set<String> segnalati = new HashSet<>();

        for (String[] movimento : movimenti) {
            String tokenUscita = movimento[8];
            String tokenEntrata = movimento[11];

            //Un movimento senza token né in uscita né in entrata non tocca alcun saldo: prima si
            //costruivano comunque le chiavi e si tentavano le due conversioni
            if (tokenUscita.isEmpty() && tokenEntrata.isEmpty()) continue;

            String exchange = movimento[3];
            String wallet = movimento[4];
            //Il prefisso della chiave di controllo è lo stesso per uscita ed entrata: prima veniva
            //ricostruito due volte, toLowerCase compresi.
            //Nota: il token NON viene abbassato di caso, solo exchange e wallet - comportamento
            //preesistente, cambiarlo unirebbe o spezzerebbe righe nella tabella dei saldi
            String prefissoChiave = exchange.toLowerCase() + ";" + wallet.toLowerCase() + ";";

            // Uscita
            if (!tokenUscita.isEmpty()) {
                //merge = una sola operazione sulla mappa al posto di getOrDefault + put + get
                BigDecimal saldo = saldi.merge(prefissoChiave + tokenUscita,
                        parseBigDecimalSafe(movimento[10]), BigDecimal::add);
                if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                    //La chiave che conserva le maiuscole serve solo quando c'è davvero da
                    //segnalare: prima veniva costruita per ogni movimento
                    segnalati.add(exchange + ";" + wallet + ";" + tokenUscita);
                }
            }

            // Entrata (non genera mai segnalazioni, solo saldo)
            if (!tokenEntrata.isEmpty()) {
                saldi.merge(prefissoChiave + tokenEntrata,
                        parseBigDecimalSafe(movimento[13]), BigDecimal::add);
            }
        }

        List<String> listaOrdinata = new ArrayList<>(segnalati);
        Collections.sort(listaOrdinata);
        return listaOrdinata; 
        
        
    }
    
    
    
    
    /**
     * Converte una quantità in {@link BigDecimal}, restituendo {@link BigDecimal#ZERO} se il valore
     * è {@code null}, vuoto o non numerico.
     * <p>
     * Passa da {@link #NumeroONull}, che scarta i valori non numerici <b>senza costruire
     * l'eccezione</b>: su {@link #ControllaSaldiNegativi} circa 62.000 delle 202.000 conversioni
     * riguardano quantità vuote (movimenti con la sola gamba in entrata o in uscita), e prima ognuna
     * costava una {@code NumberFormatException} completa di stack trace.
     * <p>
     * Il {@code trim()} viene fatto <b>prima</b> del controllo, come nella versione precedente:
     * invertirlo farebbe scartare valori validi con spazi attorno (es. {@code " 12"}).
     *
     * @param s quantità da convertire
     * @return il valore convertito, oppure {@link BigDecimal#ZERO} se non convertibile
     */
    private static BigDecimal parseBigDecimalSafe(String s) {
        if (s == null) return BigDecimal.ZERO;
        BigDecimal v = NumeroONull(s.trim());
        return v != null ? v : BigDecimal.ZERO;
    }
    
    
    /**
     * Apre un URL nel browser predefinito del sistema, usando {@link Desktop#browse} se disponibile, altrimenti
     * (solo su Linux, se il desktop non supporta l'apertura browser) tentando {@code xdg-open} come fallback.
     * Nota: nel ramo di fallback {@code xdg-open}, il metodo ritorna {@code true} anche se {@code exec} lancia
     * {@link IOException} (l'eccezione viene solo loggata).
     * @param Url indirizzo da aprire
     * @return {@code true} se l'apertura tramite {@link Desktop#browse} è riuscita, oppure se si è tentato il fallback {@code xdg-open}; {@code false} solo se {@link Desktop#browse} fallisce o se il sistema non è Linux e nessun metodo è supportato
     */
    public static boolean ApriWeb(String Url) {

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {


                Desktop.getDesktop().browse(new URI(Url));
                return true;

            } catch (URISyntaxException | IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        } else {
            String os = System.getProperty("os.name").toUpperCase();
            if (os.contains("LINUX")) {
                try {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", Url});

                } catch (IOException ex) {
                    LoggerGC.ScriviErrore(ex);
                    return true;
                }
            }
        }
        return false;
    }
    
    
        /**
         * Ricava il nome della rete DeFi di un movimento dal formato del nome wallet (es. {@code indirizzo (RETE)})
         * combinato con il prefisso {@code BC.} dell'ID, verificando che l'indirizzo sia valido su quella rete
         * tramite {@link Funzioni_WalletDeFi#isValidAddress}.
         * @param ID identificativo del movimento
         * @param k parametro non utilizzato nel corpo del metodo
         * @return il nome della rete DeFi individuata, oppure {@code ""} se il movimento non esiste o non è un wallet DeFi riconoscibile
         */
        public static String Deprecato_RitornaReteDefi(String ID,int k) {
        String Transazione[]=MappaCryptoWallet.get(ID);
        if (Transazione==null)return "";
        String Wallet=Transazione[3].trim();
        String appoggio[]=Wallet.split(" ");
        String Rete="";
        String ReteAppoggio;
        // Se soddisfa le seguenti condizioni significa che ho trovato un wallet in defi e posso tornare il nome della Rete DEFI
        // Quindi restituisco il nome della rete oltre le condizioni principali solo se hop la transaction hash
        if (appoggio.length==2&&appoggio[1].contains("(")&&appoggio[1].contains(")")&&ID.split("_")[1].startsWith("BC.")){
            ReteAppoggio=ID.split("_")[1].split("\\.")[1];
            if (Funzioni_WalletDeFi.isValidAddress(appoggio[0],ReteAppoggio)){
                Rete=ReteAppoggio;
            }
        }
        return Rete;
    }
    
    
        //Questa funzione è da lanciare al termine di un importazione dati DeFi e se imposto un token come scam
    //controlla se ci sono commissioni imputate a movimenti di prelievo defi scam
    //questi movimenti non sono veri movimenti ma vengono solo visti dagli explorer come tali
    //le commissioni non vanno quindi imputate al wallet e vanno quindi tolte.
    
    //Questa funzione che si occupa di:
                    //1 - Eliminae le commissioni fittizzie sui movimenti di prelievo scam
                    //2 - Cancellare i prelievi e le commissioni con quantità zero perchè anch'essi scam
                    //3 - Trasformare i prelievi fatti da se stessi per se stessi in scambio con la stessa moneta
                    //Se per sbaglio infatti invio cripto al mio stesso wallet questo viene identificato come prelievo
                    //ma non vi è nessun movimento di deposito, il risultato sarebbero delle giacenze errate
    
    //Da Lanciare a fine importazione wallets e dopo aver identificato un token come scam

    /**
     *Questa funzione non serve più e la rinomino
     */
    
    public static void Deprecato_ConvertiInvioSuStessoWallet(){
        Map<String,String> Mappa_CommissioniDaCancellare=new TreeMap<>();
        Map<String,String> Mappa_CommissioniPerHash=new TreeMap<>();
        Map<String,String> Mappa_MovimentiDaEliminare=new TreeMap<>();
        Map<String,String[]> Mappa_MovimentiDaCreare=new TreeMap<>();
        Map<String, String> Mappa_NomiTokenPersonalizzati = DatabaseH2.RinominaToken_LeggiTabella();
        for (String[] v : MappaCryptoWallet.values()) {
            
            
            
            //PASSO 1 - RINOMINO I TOKEN CHE DEVONO ESSERE RINOMINATI
            String Rete = Funzioni.TrovaReteDaIMovimento(v);
            String AddressU = v[26];
            String AddressE = v[28];
            //if (!Funzioni.noData(Rete)) {
                if (!Funzioni.noData(AddressU)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    if (Rete==null)Rete="";
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressU + "_" + Rete);
                    if (valore != null) {
                        v[8] = valore;
                    }
                }
                if (!Funzioni.noData(AddressE)) {
                    //Se ho dati allora verifico se ho nomitoken da cambiare e lo faccio
                    if (Rete==null)Rete="";
                    String valore = Mappa_NomiTokenPersonalizzati.get(AddressE + "_" + Rete);
                    if (valore != null) {
                        v[11] = valore;
                    }

                }
                      
            
            //PASSO 1A - SALVO UNA MAPPA tutti i movimenti con commissione con qta zero
            //questi infatti sono movimenti che non servono a nulla e gli eliminerò
            //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
            String TipoMovimento=v[0].split("_")[4].trim();
          /*   if ( (TipoMovimento.equalsIgnoreCase("CM")&&v[10].equalsIgnoreCase("-0"))
                     ||
                  (TipoMovimento.equalsIgnoreCase("PC")&&v[10].equalsIgnoreCase("-0"))){
                 //salvo nella mappa delle commissioni tutti gli id e come indice uso l'hash
                Mappa_MovimentiDaEliminare.put(v[0],"");
                
                //per i movimenti di prelievo mi salvo anchel'hash perchè dovrò andare a cancellare le commissioni
                if ( TipoMovimento.equalsIgnoreCase("PC")&&v[24]!=null&&!v[24].isBlank()){
                    //Salvo tutti gli hash delle commissioni che devo cancellare
                    Mappa_CommissioniDaCancellare.put(v[24], "");  
                    //System.out.println(v[24]);
            }
            }   */
             
            //PASSO 1B - Se ho un prelievo dove l'address controparte è uguale al mio wallet
            //vuol dire che mi sono autoinviato dei fondi, in quel caso il movimento va convertito in scambio
            if (TipoMovimento.equalsIgnoreCase("PC")&&v[3].split("\\(")[0].trim().equalsIgnoreCase(v[30])){
                String clone[]=v.clone();
                String partiID[]=clone[0].split("_");
                clone[0]=partiID[0]+"_"+partiID[1]+"_"+partiID[2]+"_"+partiID[3]+"_SC";
                clone[5]="SCAMBIO CRYPTO";
                clone[6]= clone[8]+" -> "+clone[8];
                clone[11]=clone[8];
                clone[12]=clone[9];
                clone[13]=clone[10].replace("-", "");
                clone[27]=clone[25];
                clone[28]=clone[26];
                //il movimento errato lo metto qua sotto in questa mappa perchè venga elimnato a fine ciclo
                Mappa_MovimentiDaEliminare.put(v[0],"");
                Mappa_MovimentiDaCreare.put(clone[0], clone);
            }
            
            
                
            //PASSO 2 - SALVO UNA MAPPA HASHCommissione_ID
            //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
         /*    if ( TipoMovimento.equalsIgnoreCase("CM")){
                 //salvo nella mappa delle commissioni tutti gli id e come indice uso l'hash
                Mappa_CommissioniPerHash.put(v[24],v[0]);
            }*/
             
             
             
             
             
             //PASSAO 3 - SALVO UNA MAPPA CON LA LISTA DELLE COMMISSIONI DA ELIMINARE per HASH
             //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
        /*    if ( TipoMovimento.equalsIgnoreCase("PC")&&Funzioni.isSCAM(v[8])&&v[24]!=null&&!v[24].isBlank()){
               //Salvo tutti gli hash delle commissioni che devo cancellare
               Mappa_CommissioniDaCancellare.put(v[24], "");  
               //System.out.println(v[24]);
            }*/
            
            
        }
        
        //Adesso cancello le commissioni imputate erroneamente  
        //QUESTA FUNZIONE NON SERVE PIU', VIENE SISTEMATO TUTTO IN FASE DI IMPORTAZIONE DATI
      /*  for (String hash : Mappa_CommissioniDaCancellare.keySet()) {
            //String TipoMovimento=v[0].split("_")[4].trim();
            String ID=Mappa_CommissioniPerHash.get(hash);
            if ( ID!=null){
                MappaCryptoWallet.remove(ID);
            }
        }*/
        
        //Adesso cancello i movimenti da eliminare     
        for (String ID : Mappa_MovimentiDaEliminare.keySet()) {

                MappaCryptoWallet.remove(ID);
            
        }
        
        //Adesso creo i movimenti da creare
        for (String ID : Mappa_MovimentiDaCreare.keySet()) {

                MappaCryptoWallet.put(ID, Mappa_MovimentiDaCreare.get(ID));
            
        }
        
    }
    
    
    
    /**
     * Determina se un movimento è fiscalmente rilevante ai fini del calcolo delle plusvalenze, in base al suo
     * tipo (5° segmento dell'ID) e, quando serve, alla sottoclassificazione nel campo 18: vendite/commissioni
     * ({@code VC}/{@code CM}) e acquisti ({@code AC}) sono sempre rilevanti; depositi/prelievi/scambi fiat e
     * trasferimenti interni non lo sono mai; reward ({@code RW}) e airdrop/costo-zero ({@code DAI}/{@code DCZ})
     * dipendono da {@link #RewardRilevante} e dalle opzioni {@code Plusvalenze_Pre2023EarnCostoZero} per i
     * movimenti antecedenti al 2023-01-01; scambi crypto ({@code SC}) sono rilevanti solo se le due monete
     * scambiate hanno tipo diverso secondo {@link #RitornaTipoCrypto} (a meno che l'opzione
     * {@code Plusvalenze_Pre2023ScambiRilevanti} non li renda sempre rilevanti prima del 2023); depositi/prelievi
     * crypto non classificati dipendono dall'opzione {@code PL_CosiderareMovimentiNC}. Un ID malformato (meno di
     * 5 segmenti) viene normalizzato e loggato come errore anziché causare un'eccezione.
     * @param Mov riga di movimento grezza
     * @return {@code true} se il movimento è fiscalmente rilevante
     */
    public static boolean MovimentoRilevante(String[] Mov){
        String ID = Mov[0];
        String IDTS[] = ID.split("_");
        //A3: un ID malformato (meno di 5 segmenti) non deve far crashare la valutazione:
        //normalizzo a 5 segmenti con tipo vuoto, così la classificazione ricade sul campo 18
        if (IDTS.length <= 4) {
            LoggerGC.ScriviErrore("MovimentoRilevante: ID transazione malformato \"" + ID + "\"");
            IDTS = java.util.Arrays.copyOf(IDTS, 5);
            for (int i = 0; i < IDTS.length; i++) if (IDTS[i] == null) IDTS[i] = "";
        }
        Moneta m[] = Moneta.RitornaMoneteDaMov(Mov);
        String Data = Mov[1];
        boolean Pre2023EarnCostoZero = false;
        boolean Pre2023ScambiRilevanti = false;
        long long2023 = FunzioniDate.ConvertiDatainLongMinuto("2023-01-01 00:00");
        long dataLong = FunzioniDate.ConvertiDatainLongMinuto(Data);
        boolean DataSuperiore2023 = true;
        if (dataLong < long2023) {
            DataSuperiore2023 = false;
        }
        String Plusvalenze_Pre2023EarnCostoZero = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023EarnCostoZero");
        if (Plusvalenze_Pre2023EarnCostoZero != null && Plusvalenze_Pre2023EarnCostoZero.equalsIgnoreCase("SI")) {
            Pre2023EarnCostoZero = true;
        }
        String Plusvalenze_Pre2023ScambiRilevanti = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023ScambiRilevanti");
        if (Plusvalenze_Pre2023ScambiRilevanti != null && Plusvalenze_Pre2023ScambiRilevanti.equalsIgnoreCase("SI")) {
            Pre2023ScambiRilevanti = true;
        }
        //Con questa opzione decido che fare in caso di movimenti non classificati, se conteggiarli o meno
       boolean ConsideraMovimentiNC=true;
       if(DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC","SI").equalsIgnoreCase("NO"))ConsideraMovimentiNC=false;
        
        
        boolean rilevante=true;
        boolean plusvalenza=true;
        
        if (IDTS[4].equals("VC")                //Vendita Cripto Rilevante
                || IDTS[4].equals("CM"))         //Commissione Rilevante
        {
            rilevante=true;
            plusvalenza=true;
        } else if (IDTS[4].equals("RW")) //Rewards Rilevante
        {
            //A seconda dei casi può generare plusvalenza o meno
            if ((DataSuperiore2023 && Funzioni.RewardRilevante(ID))
                    || (!DataSuperiore2023 && !Pre2023EarnCostoZero && Funzioni.RewardRilevante(ID))) {
                plusvalenza = true;

            } else {
                plusvalenza = false;
            }
            rilevante = true;
        }else if (IDTS[4].equals("AC"))//Acquisto Cripto Rilvente ma no Plusvalenza
                {
            rilevante=true;
            plusvalenza=false;
        }
            else if (IDTS[4].equals("DF")//deposito Fiat
                        || IDTS[4].equals("PF")//Prelievo Fiat
                        || IDTS[4].equals("SF")//Scambio Fiat
                        || IDTS[4].equals("TI"))//Trasferimento Interno
                {
            rilevante=false;
            plusvalenza=false;
        }
        else if (IDTS[4].equals("SC"))//scambio Crypto
        {
            String Tipo1 = RitornaTipoCrypto(m[0].Moneta, Data, m[0].Tipo);
            String Tipo2 = RitornaTipoCrypto(m[1].Moneta, Data, m[1].Tipo);
            if (DataSuperiore2023 || !Pre2023ScambiRilevanti) {
                if (Tipo1.equalsIgnoreCase(Tipo2)) {
                    rilevante = false;
                    plusvalenza = false;
                } else {
                    rilevante = true;
                    plusvalenza = true;
                }
            }else{
                rilevante = true;
                plusvalenza = true;
            }
        }
        else if (IDTS[4].equals("DC")//Deposito Crypto
                || IDTS[4].equals("PC"))//Prelievo Crypto
        {
            //Le tipologie possono essere le seguenti
            //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
            //PCO -> Cashout o similare (prelievo)
            //PTW -> Trasferimento tra Wallet (prelievo)
            //DTW -> Trasferimento tra Wallet (deposito)
            //DAI -> Airdrop o similare (deposito)
            //DCZ -> Costo di carico 0 (deposito)
            //DAC -> Acquisto Crypto (deposito)  
            if (Mov[18].isBlank()) {
                //Movimento non classificato
                if(ConsideraMovimentiNC){
                    rilevante = true;
                    plusvalenza = IDTS[4].equals("PC");
                    
                }
                else{
                    rilevante = false;
                    plusvalenza = false;
                }
            }
        } else if (Mov[18].contains("PWN")) {
            //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
            rilevante=false;
            plusvalenza=false;
        }  else if (Mov[18].contains("PCO")) {
            rilevante=true;
            plusvalenza=false;
        }else if (Mov[18].contains("DAI") || Mov[18].contains("DCZ")) {
            //A seconda dei casi può generare plusvalenza o meno
            //plusvalenza è true se rispetta queste condizioni altrimenti è false
            plusvalenza = (DataSuperiore2023 && Funzioni.RewardRilevante(ID))|| 
                          (!DataSuperiore2023 && !Pre2023EarnCostoZero && Funzioni.RewardRilevante(ID));
            rilevante = true;
        }
        else if (Mov[18].contains("PTW") || Mov[18].contains("DTW")) {
            //Trasferimento tra wallet
            rilevante=false;
            plusvalenza=false;
        }
        else if (Mov[18].contains("DAC")||Mov[18].contains("DDO")) {
            //Acquisto Crypto
            rilevante=true;
            plusvalenza=false;
        }
        return rilevante;
    }
        
    
    
    /**
     * Restituisce il tipo effettivo di un token alla data indicata: se il token è classificato come
     * {@code "Crypto"} ma è presente in {@link Principale#Mappa_EMoney} con una data di conversione a e-money
     * anteriore o uguale alla data dello scambio, il tipo restituito diventa {@code "EMoney"}.
     * @param Token simbolo del token
     * @param Data data dello scambio
     * @param Tipologia tipo dichiarato del token
     * @return {@code "EMoney"} se il token è diventato e-money entro la data indicata, altrimenti {@code Tipologia} invariata
     */
    public static String RitornaTipoCrypto(String Token,String Data,String Tipologia) {
       String Tipo=Tipologia;
       String DataEmoney=Principale.Mappa_EMoney.get(Token);
       if(Tipologia.equalsIgnoreCase("Crypto")&&DataEmoney!=null){
           long dataemoney=FunzioniDate.ConvertiDatainLong(DataEmoney);
           long datascambio=FunzioniDate.ConvertiDatainLong(Data);
           if (datascambio>=dataemoney) Tipo="EMoney";
       }
       return Tipo;
   }
    
    
      /**
       * Determina se una reward (staking, airdrop, cashback, earn, reward generico) genera plusvalenza,
       * in base alla sottostringa contenuta nella descrizione del movimento (campo 5) e alla relativa opzione
       * personale ({@code PDD_CashBack}, {@code PDD_Staking}, {@code PDD_Airdrop}, {@code PDD_Earn},
       * {@code PDD_Reward}, tutte con default {@code "SI"}). Si applica solo ai movimenti classificati come
       * reward alla fonte ({@code RW}) o riclassificati come tali ({@code DAI} nel campo 18); per ogni altro
       * movimento (o se il movimento non è trovato o l'ID è malformato) restituisce sempre {@code true} come
       * comportamento prudente di default.
       * @param ID identificativo del movimento
       * @return {@code true} se la reward è fiscalmente rilevante
       */
      public static boolean RewardRilevante(String ID) {

       String[] Mov=MappaCryptoWallet.get(ID);
       String IDTS[]=ID.split("_");
       //A2/A3: movimento assente dalla mappa o ID malformato (meno di 5 segmenti):
       //non posso stabilire il tipo, considero la reward fiscalmente rilevante (default prudente)
       if (Mov == null || IDTS.length <= 4) {
           LoggerGC.ScriviErrore("RewardRilevante: movimento \"" + ID + "\" " + (Mov == null ? "non trovato nella mappa" : "con ID malformato"));
           return true;
       }
       String TipoTrasf=Mov[18].split("-")[0].trim();

       //Perchè sia una reward devo verificare se è classificata come tale alla fonte (RW)
       //oppure se è stata classificata dopo quindi DAI
       if (IDTS[4].equals("RW")||
              TipoTrasf.equals("DAI") ){
           
           if (Mov[5].toUpperCase().contains("CASHBACK"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_CashBack","SI").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("STAKING"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Staking","SI").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("AIRDROP"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Airdrop","SI").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("EARN"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Earn","SI").equalsIgnoreCase("SI");
           }           
           else if (Mov[5].toUpperCase().contains("REWARD"))
           {               
               return DatabaseH2.Pers_Opzioni_Leggi("PDD_Reward","SI").equalsIgnoreCase("SI");
           }
           else return true;

       }
       //se non soddisfa nessuno dei casi sopra allora metto che è fiscalmente rilevante
       return true;
      

   }  
    
      /**
       * Determina se un movimento di cashback (classificato come reward {@code RW} o riclassificato {@code DAI})
       * deve essere trattato fiscalmente come fiat invece che come reward crypto, in base all'opzione personale
       * {@code CashBackComeFIAT} (default {@code "NO"}) e, se attiva, all'anno del movimento rispetto alla soglia
       * configurata in {@code CashBackComeFIATAnno} (default 2010, quindi applicata a tutti gli anni se non impostata).
       * Restituisce {@code false} se il movimento non è trovato, l'ID è malformato o non ha un anno iniziale valido,
       * o se non è classificato come cashback.
       * @param ID identificativo del movimento
       * @return {@code true} se il cashback va trattato come fiat
       */
      public static boolean CashbackComeFIAT(String ID) {

       String[] Mov=MappaCryptoWallet.get(ID);
       String IDTS[]=ID.split("_");
       //A2/A3: movimento assente dalla mappa o ID malformato (meno di 5 segmenti o senza
       //anno iniziale): non posso applicare l'assimilazione a fiat, la reward resta tassata
       if (Mov == null || IDTS.length <= 4) {
           LoggerGC.ScriviErrore("CashbackComeFIAT: movimento \"" + ID + "\" " + (Mov == null ? "non trovato nella mappa" : "con ID malformato"));
           return false;
       }
       String TipoTrasf=Mov[18].split("-")[0].trim();
       int AnnoInt;
       try {
           AnnoInt=Integer.parseInt(Mov[0].substring(0, 4));
       } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
           LoggerGC.ScriviErrore("CashbackComeFIAT: ID \"" + ID + "\" senza anno iniziale valido");
           return false;
       }
       
       boolean CashbackComeFiat=false;
       
       //Perchè sia una reward devo verificare se è classificata come tale alla fonte (RW) 
       //oppure se è stata classificata dopo quindi DAI
       if (IDTS[4].equals("RW")||
              TipoTrasf.equals("DAI") ){
           
           if (Mov[5].toUpperCase().contains("CASHBACK"))
           {               
               CashbackComeFiat = DatabaseH2.Pers_Opzioni_Leggi("CashBackComeFIAT","NO").equalsIgnoreCase("SI");
               if (CashbackComeFiat) {
                   String AnnoDB = DatabaseH2.Pers_Opzioni_Leggi("CashBackComeFIATAnno");
                   int AnnoDBInt = 2010;
                   if (AnnoDB != null && !AnnoDB.isBlank()) {
                       AnnoDBInt = Integer.parseInt(AnnoDB);
                   }
                   CashbackComeFiat=AnnoInt>=AnnoDBInt;
               }
           }           
           
           else return CashbackComeFiat;

       }
       //se non soddisfa nessuno dei casi sopra allora metto che è fiscalmente rilevante
       return CashbackComeFiat;
      

   }  
    
    
        /**
         * Costruisce le due monete (uscente ed entrante) coinvolte in un movimento, a partire dai suoi campi grezzi.
         * @param ID identificativo del movimento
         * @return array di 2 elementi: {@code [0]} moneta uscente, {@code [1]} moneta entrante
         */
        public static Moneta[] RitornaMoneteDaID(String ID){
            //Moneta[0] sarà la moneta uscente
            //Moneta[1] srà quella entrante
            Moneta m[]=new Moneta[2];
            String[] Mov=MappaCryptoWallet.get(ID);
            m[0]=new Moneta();
            m[0].Moneta=Mov[8];
            m[0].MonetaAddress=Mov[26];
            m[0].Prezzo=Mov[15];
            m[0].Qta=Mov[10];
            m[0].Rete=TrovaReteDaID(ID);
            m[0].Tipo=Mov[9];
            m[1]=new Moneta();
            m[1].Moneta=Mov[11];
            m[1].MonetaAddress=Mov[28];
            m[1].Prezzo=Mov[15];
            m[1].Qta=Mov[13];
            m[1].Rete=TrovaReteDaID(ID);
            m[1].Tipo=Mov[12];
            return m;
        }
        
        /**
         * Verifica se un nome di moneta è marcato come scam, riconoscendo la convenzione interna del suffisso {@code " **"}.
         * @param Nome nome della moneta da verificare
         * @return {@code true} se il nome termina con {@code " **"}
         */
        public static boolean isSCAM(String Nome){
            boolean SCAM=false;
            int Lnome=Nome.length();
            //verifico se la moneta è già considerata come scam
            if (Lnome>3 && " **".equals(Nome.substring(Lnome-3, Lnome))){
                SCAM=true;
            }
            return SCAM;
        }
        
        /**
         * Converte una riga di movimento da array di {@link String} ad array di {@link Object}, trasformando in
         * {@link BigDecimal} le colonne 15 (prezzo) e 19 (plusvalenza) e lasciando le altre colonne come stringa.
         * Se la colonna 19 è vuota viene trattata come {@code "0"}.
         * @param riga riga di movimento grezza
         * @return la riga convertita, con le colonne 15 e 19 come {@link BigDecimal}
         */
        public static Object[] Converti_String_Object(String[] riga){
            Object ritorno[]=new Object[riga.length];
           /* ritorno=riga.clone();
            ritorno[15]=new BigDecimal(riga[15]);
            ritorno[19]=new BigDecimal(riga[19]);*/
            for (int i=0;i<ritorno.length;i++){
                
                if (i==19||i==15){
                    //Questo imposta la plusvalenza a zero qualora non esista
                    if(i==19&&riga[i].isBlank())riga[i]="0";
                 //  if (!Funzioni.isNumeric(riga[i], false))System.out.println("a-"+riga[i]+"-a");
                    ritorno[i]=new BigDecimal(riga[i]);
                }else
                  {
                      ritorno[i]=riga[i];
                  }  
            }
            return ritorno;
        }
        
       /* public static boolean noData(String Valore){
            boolean noData=false;
            //verifico se la moneta è già considerata come scam
            if (Valore==null||Valore.trim().equals("")){
                noData=true;
            }
            return noData;
        }*/
        
    /**
     * @param valore stringa da verificare
     * @return {@code true} se {@code valore} è {@code null} o vuota/composta solo da spazi
     */
    public static boolean noData(String valore) {
        return valore == null || valore.isBlank();
    }

        
        /**
         * Determina la rete blockchain associata a un movimento a partire dal suo ID: se il movimento è già in
         * {@link Principale#MappaCryptoWallet}, delega a {@link #TrovaReteDaIMovimento}; altrimenti ricava la
         * rete direttamente dall'ID, riconoscendo il formato automatico {@code BC.<rete>....} (con uno dei
         * prefissi validi) o il formato manuale {@code ...(<rete>)} (solo se la rete è tra quelle supportate).
         * @param ID identificativo del movimento
         * @return il codice della rete individuata, oppure {@code null} se non determinabile
         */
        public static String TrovaReteDaID(String ID) {
        if (MappaCryptoWallet.get(ID) != null) {
            return TrovaReteDaIMovimento(Principale.MappaCryptoWallet.get(ID));
        } else {
            String Rete = null;
            //System.out.println(ID);
            //per trovare la rete devo scindere l'ID in più parti e verificarne alcune caratteristiche

            String IDSplittato[] = ID.split("_");
            String IDDettSplittato[] = IDSplittato[1].split("\\.");
            List<String> prefixValidi = Arrays.asList("BC", "00BC", "01BC", "02BC", "03BC", "04BC");
            if ((IDDettSplittato.length == 4 || IDDettSplittato.length == 5)
                    && prefixValidi.contains(IDDettSplittato[0].toUpperCase())) {//00BC viene usato negli scambi differiti automatici
                Rete = IDDettSplittato[1];
                return Rete;
            }

            //Se il primo if non trova la rete la cerco tra i movimenti manuali, a patto che la chain sia supportata
            if (IDSplittato[1].contains("(") && IDSplittato[1].contains(")") && IDSplittato[1].split("\\(").length > 1) {
                // String Mov[] = MappaCryptoWallet.get(ID);
                String ret = IDSplittato[1].split("\\(")[1].split("\\)")[0].trim();
                if (MappaRetiSupportate.get(ret) != null) {//se è una chain supportata allora la gestisco come tale
                    Rete = ret;
                    return Rete;
                }
            }

            return Rete;
        }
    }
        
        /**
         * Determina la rete blockchain di un movimento, usando la cache nel campo 34 se già valorizzata,
         * altrimenti ricavandola dall'ID: formato automatico {@code BC.<rete>....} (con uno dei prefissi validi
         * in {@link #PREFISSI_VALIDI_TrovaReteDaIMovimento}, la rete trovata viene poi salvata nel campo 34 per
         * le chiamate successive), o formato manuale {@code ...(<rete>)} (solo se la rete è tra quelle
         * supportate in {@link Principale#MappaRetiSupportate}, in questo caso non viene salvata in cache).
         * @param mov riga di movimento grezza
         * @return il codice della rete individuata, oppure {@code null} se non determinabile
         */
        public static String TrovaReteDaIMovimento(String[] mov){
        //boolean controllaAddress=false;
        //String Rete=null;
        String ID=mov[0];
        if (!mov[34].isBlank()) {
            //se è valorizzato a N ritorno null altrimenti ritorno il valore della rete NON PIU GESTITO COSI
           // return mov[34].equals("N") ? null : mov[34];
           //Se il boolean controllaAddress è true verifico anche gli address e se questi sono vuoti anche la rete la restituisco vuota
           //if(controllaAddress&&mov[28].isBlank()&&mov[26].isBlank())return "";
           //Altrimenti la valorizzo così com'è.
           //System.out.println(mov[34]);
           return mov[34];
        }
        // Se è blank, prosegue senza return
        
        //System.out.println(ID);
        //per trovare la rete devo scindere l'ID in più parti e verificarne alcune caratteristiche

            String IDSplittato[]=ID.split("_");
            String IDDettSplittato[]=IDSplittato[1].split("\\.");
           // List<String> prefixValidi = Arrays.asList("BC", "00BC", "01BC", "02BC", "03BC", "04BC", "ZZBC");
            if ((IDDettSplittato.length==4 ||IDDettSplittato.length==5) && 
                //    PREFISSI_VALIDI_TrovaReteDaIMovimento.contains(IDDettSplittato[0].toUpperCase())){//00BC viene usato negli scambi differiti automatici
                PREFISSI_VALIDI_TrovaReteDaIMovimento.contains(IDDettSplittato[0].toUpperCase())){//00BC viene usato negli scambi differiti automatici
                //Rete=IDDettSplittato[1];
                mov[34]=IDDettSplittato[1];
                //System.out.println(Rete);
                return mov[34];
            }

         
            //Se il primo if non trova la rete la cerco tra i movimenti manuali, a patto che la chain sia supportata
            if (IDSplittato[1].contains("(") && IDSplittato[1].contains(")")&& IDSplittato[1].split("\\(").length > 1) {
               // String Mov[] = MappaCryptoWallet.get(ID);
                String ret=IDSplittato[1].split("\\(")[1].split("\\)")[0].trim();
                if (MappaRetiSupportate.containsKey(ret)) {//se è una chain supportata allora la gestisco come tale
                   /* Rete = ret;
                    System.out.println(Rete);*/
                    return ret;
                }
            }
        
       // if (Rete==null)mov[34]="N";//N Significa che non ha reti, per ora non lo metto, preferisco che venga ogni volta controllato se la rete è valida
        return null;
           // return "";
        }
       
    /**
     * Versione ottimizzata di {@link #TrovaReteDaIMovimento} che evita lo split via regex, usando ricerche di
     * indice dirette ({@code indexOf}) sulla parte dell'ID successiva al primo underscore. Stessa logica:
     * usa la cache nel campo 34 se presente, altrimenti riconosce il formato automatico {@code BC.<rete>....}
     * (salvando il risultato in cache) o il formato manuale {@code ...(<rete>)} (solo se supportata).
     * @param mov riga di movimento grezza
     * @return il codice della rete individuata, oppure {@code null} se non determinabile
     */
    public static String TrovaReteDaIMovimentoNEW(String[] mov) {

        // Cache: se già calcolata, ritorna subito
        String reteCached = mov[34];
        if (reteCached != null && !reteCached.isBlank()) {
            return reteCached;
        }

        String id = mov[0];
       /* if (id == null) {
            return null;
        }*/

        // Split manuale (molto più veloce di split regex)
        int underscoreIdx = id.indexOf('_');
       /* if (underscoreIdx < 0 || underscoreIdx == id.length() - 1) {
            return null;
        }*/

        //Leggo la parte dopo il primo underscore che è quella che mi interessa per
        //trovare se è un dato derivante da blockchain quindi inizia per BC o simile
        String secondaParte = id.substring(underscoreIdx + 1);

        // ---- CASO 1: ID automatici BC ----
        int dot1 = secondaParte.indexOf('.');
        if (dot1 > 0) {

            String prefisso = secondaParte.substring(0, dot1);
            //Controllo se l'ID ha i prefissi corretti e in quel caso ritorno la rete
            if (PREFISSI_VALIDI_TrovaReteDaIMovimento.contains(prefisso)) {

                int dot2 = secondaParte.indexOf('.', dot1 + 1);
                if (dot2 > dot1) {
                    String rete = secondaParte.substring(dot1 + 1, dot2);
                    //la rete la trovo tra il primo e il secondo punto se il prefisso è corretto
                    //esempio di ID : 20251129003318_BC.SOL.5LPAkNQaz6pwpYFf5zfon9Lb7HYhEzwZJXpafH9U2Pjx.4NwvC7hq9wuB6N1wqjqwTZLDAp78XpT5dWgQpmVxZeH3X6k25c4iGZywgJJB3EXuLsXosREcrgsebzCQ7ynVk5T3_1_1_DC
                    mov[34] = rete; // cache
                    return rete;
                }
            }
        }

        // ---- CASO 2: movimenti manuali con (RETE) ----
        int open = secondaParte.indexOf('(');
        int close = secondaParte.indexOf(')', open + 1);

        if (open >= 0 && close > open) {
            String rete = secondaParte.substring(open + 1, close).trim();
            if (MappaRetiSupportate.containsKey(rete)) {
                return rete;
            }
        }

        return null;
    }

        
     
        
       /**
        * Sostituisce con {@code ""} tutti gli elementi {@code null} di un array di stringhe.
        * @param array array da normalizzare (modificato in place)
        * @return lo stesso array passato, con gli elementi {@code null} sostituiti da {@code ""}
        */
       public static String[] RiempiVuotiArray(String[] array){
            for (int i=0;i<array.length;i++) {
                if(array[i]==null){
                    array[i]="";
                }
            }
            return array;
        }
     
       
    
    /**
     * Verifica se una stringa rappresenta un numero decimale valido.
     * @param str stringa da verificare
     * @param CampoVuotoContacomeNumero se {@code true}, una stringa vuota/composta solo da spazi è considerata numerica
     * @return {@code true} se {@code str} è un {@link BigDecimal} valido (o vuota con {@code CampoVuotoContacomeNumero=true}), {@code false} se {@code null} o non numerica
     */
    public static boolean isNumeric(String str, boolean CampoVuotoContacomeNumero) {
        
        if (str==null)return false;
        //ritorna vero se il campo è vuoto oppure è un numero
        if (CampoVuotoContacomeNumero && str.isBlank()) {
            return true;
        }
        try {
            BigDecimal B = new BigDecimal(str);
        } catch (NumberFormatException nfe) {
            //System.out.println(nfe);
            return false;
        }
        return true;

    }

    /**
     * Filtro rapido che scarta i valori che {@link BigDecimal#BigDecimal(String)} rifiuterebbe
     * sicuramente, senza costruire l'eccezione. Riconosce la sola grammatica accettata da
     * {@code BigDecimal}: segno opzionale, cifre con eventuale punto decimale (almeno una cifra
     * nella mantissa) ed eventuale esponente {@code e/E} con segno opzionale e almeno una cifra.
     * <p>
     * <b>È volutamente asimmetrico</b>: non deve MAI scartare una stringa che {@code BigDecimal}
     * accetterebbe (un falso negativo cambierebbe i totali), mentre un falso positivo è innocuo
     * perché il chiamante tenta comunque la conversione dentro un {@code try/catch}. Per questo
     * accetta anche la notazione scientifica, che è il formato in cui {@code BigDecimal.toString()}
     * emette le quantità piccole (es. {@code 2.5E-9}).
     *
     * @param s stringa da esaminare (non {@code null})
     * @return {@code false} solo se la stringa non è certamente un numero
     */
    private static boolean PuoEssereNumero(String s) {
        int n = s.length();
        if (n == 0) return false;

        int i = 0;
        char c = s.charAt(i);
        if (c == '+' || c == '-') i++;

        //mantissa: cifre, al massimo un punto decimale, almeno una cifra in tutto
        boolean cifraMantissa = false;
        boolean punto = false;
        while (i < n) {
            c = s.charAt(i);
            if (c >= '0' && c <= '9') { cifraMantissa = true; i++; }
            else if (c == '.' && !punto) { punto = true; i++; }
            else break;
        }
        if (!cifraMantissa) return false;
        if (i == n) return true;

        //esponente: e/E, segno opzionale, almeno una cifra, poi deve finire la stringa
        c = s.charAt(i);
        if (c != 'e' && c != 'E') return false;
        i++;
        if (i < n) {
            c = s.charAt(i);
            if (c == '+' || c == '-') i++;
        }
        boolean cifraEsponente = false;
        while (i < n) {
            c = s.charAt(i);
            if (c < '0' || c > '9') return false;
            cifraEsponente = true;
            i++;
        }
        return cifraEsponente;
    }

    /**
     * Converte in {@link BigDecimal} il valore di una cella di tabella, restituendo {@code null}
     * se non è un numero. Pensata per i cicli su molte celle (vedi
     * {@link Tabelle#Tabelle_getSommeColonne}), dove il costo dominante non è la conversione ma
     * la costruzione della {@link NumberFormatException} sulle celle non numeriche (date, ID,
     * simboli token, indirizzi): la maggior parte delle celle viene scartata da
     * {@link #PuoEssereNumero} senza lanciare nulla.
     * <p>
     * Se il valore è già un {@code BigDecimal} (nella tabella principale le colonne 15 e 19 lo
     * sono, vedi {@link #Converti_String_Object}) viene restituito così com'è, senza passare da
     * {@code toString()}.
     *
     * @param valore contenuto della cella, tipicamente da {@code TableModel.getValueAt}
     * @return il valore convertito, oppure {@code null} se {@code null}, vuoto o non numerico
     */
    public static BigDecimal NumeroONull(Object valore) {
        if (valore == null) return null;
        if (valore instanceof BigDecimal bd) return bd;

        String s = valore.toString();
        if (!PuoEssereNumero(s)) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException nfe) {
            //rete di sicurezza: PuoEssereNumero è volutamente permissivo
            return null;
        }
    }

/**
 * Verifica la validità di una API key Moralis effettuando una richiesta minimale (numero ultimo blocco Ethereum).
 * @param apiKey API key da validare
 * @return {@code true} se la richiesta risponde con codice HTTP 200, {@code false} altrimenti (inclusi gli errori di rete)
 */
public static boolean isApiKeyValidaMoralis(String apiKey) {
    try {
        // SUPEREFFICIENTE - solo numero blocco!
        URL url = new URL("https://deep-index.moralis.io/api/v2.2/latestBlockNumber/eth");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-API-Key", apiKey);
        
        int status = con.getResponseCode();
        System.out.println("Stato Moralis (latestBlock): " + status);
        return status == 200;
    } catch (IOException e) {
        LoggerGC.ScriviErrore(e);
        return false;
    }
}







    
    /**
     * Verifica la validità di una API key Etherscan (v2, chain Ethereum) effettuando una richiesta {@code eth_blockNumber}.
     * @param ApiKey API key da validare
     * @return {@code true} se la risposta è una richiesta JSON-RPC 2.0 andata a buon fine, {@code false} altrimenti (inclusi gli errori di rete o parsing)
     */
    public static boolean isApiKeyValidaEtherscan(String ApiKey) {
       // return true;
        String ETHERSCAN_URL = "https://api.etherscan.io/v2/api?chainid=1&module=proxy&action=eth_blockNumber&apikey=";
        OkHttpClient client = HTTP_CLIENT;
        String url = ETHERSCAN_URL + ApiKey;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            //Risposta se ok -> {"jsonrpc":"2.0","id":83,"result":"0x14f857d"}
            //Risposta se non ok -> {"status": "0","message": "NOTOK","result": "Invalid API Key"}
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("jsonrpc") && "2.0".equals(json.get("jsonrpc").getAsString());
        } catch (Exception e) {
           // e.printStackTrace();
            LoggerGC.ScriviErrore(e);
            return false;
        }
    }
    
    /**
     * Verifica la validità di una API key per l'explorer Cronos effettuando una richiesta {@code eth_blockNumber}.
     * @param ApiKey API key da validare
     * @return {@code true} se la risposta è una richiesta JSON-RPC 2.0 andata a buon fine, {@code false} altrimenti (inclusi gli errori di rete o parsing)
     */
    public static boolean isApiKeyValidaCronos(String ApiKey) {
       // return true;
        String ETHERSCAN_URL = "https://explorer-api.cronos.org/mainnet/api/v2?module=proxy&action=eth_blockNumber&apikey=";
        OkHttpClient client = HTTP_CLIENT;
        String url = ETHERSCAN_URL + ApiKey;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            //Risposta se ok -> {"jsonrpc":"2.0","id":83,"result":"0x14f857d"}
            //Risposta se non ok -> {"status": "0","message": "NOTOK","result": "Invalid API Key"}
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("jsonrpc") && "2.0".equals(json.get("jsonrpc").getAsString());
        } catch (Exception e) {
           // e.printStackTrace();
            LoggerGC.ScriviErrore(e);
            return false;
        }
    }

    /**
     * Verifica la validità di una API key CoinMarketCap effettuando una richiesta minimale (mappa delle crypto attive, limite 1).
     * @param ApiKey API key da validare
     * @return {@code true} se la risposta ha {@code error_code} pari a {@code "0"}, {@code false} altrimenti (inclusi chiave assente/vuota, errori di rete o parsing)
     */
    public static boolean isApiKeyValidaCoinMarketCap(String ApiKey) {
        if (ApiKey == null || ApiKey.isBlank()) return false;
        OkHttpClient client = HTTP_CLIENT;
        Request request = new Request.Builder()
                .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/map?listing_status=active&limit=1")
                .header("X-CMC_PRO_API_KEY", ApiKey)
                .header("Accept", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return false;
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (!json.has("status")) return false;
            return "0".equals(json.getAsJsonObject("status").get("error_code").getAsString());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica la validità di una API key coingecko effettuando una richiesta all'endpoint {@code /ping}.
     * @param ApiKey API key da validare
     * @return {@code true} se la risposta contiene il campo {@code gecko_says}, {@code false} altrimenti (inclusi gli errori di rete o parsing)
     */
    public static boolean isApiKeyValidaCoingecko(String ApiKey) {
        OkHttpClient client = HTTP_CLIENT;
        Request request = new Request.Builder().url("https://api.coingecko.com/api/v3/ping").get().addHeader("accept", "application/json").addHeader("x-cg-demo-api-key", ApiKey).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false; // Errore di connessione o chiave non valida
            }
            String responseBody = response.body().string();
            //System.out.println(responseBody);
            // Parsing JSON con Gson
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("gecko_says");
        } catch (Exception e) {
            System.out.println("Trans_Solana.isApiKeyValidaCoingecko " + e.getMessage());
            return false;
        }
    }

    /**
     * Controlla i file di una cartella di configurazione locale rispetto alla corrispondente cartella
     * del repository GitHub e aggiorna quelli obsoleti o mancanti.
     * <p>Il confronto avviene sullo sha blob git, quindi non scarica nulla se il contenuto coincide.
     * Il download passa da {@code scaricaFileDaUrl}, che scrive i byte grezzi: va bene anche per i
     * file binari (loghi PNG).
     * <p><b>Nota di retrocompatibilità:</b> la cartella {@code ImportConfig/} del repository non va
     * svuotata né rimossa. Le versioni installate prima dell'introduzione di {@code config/} continuano
     * a sincronizzarsi da lì e cancellano i propri file marcati {@code "centralizzato": true} che non
     * trovano più nel repository.
     *
     * @param cartellaLocale path assoluta della cartella locale da allineare
     * @param pathRepo path della cartella nel repository (es. {@code "config/importmappe"})
     * @param estensione estensione dei file da considerare, minuscola e con il punto (es. {@code ".json"})
     * @param cancellaOrfaniCentralizzati se {@code true} cancella i file locali marcati
     *        {@code "centralizzato": true} non più presenti nel repository; da tenere a {@code false}
     *        per le cartelle che non contengono JSON (il parse fallirebbe su ogni file)
     * @return lista dei nomi dei file effettivamente aggiornati/scaricati
     */
    public static List<String> AggiornamentoConfigDaRepository(String cartellaLocale, String pathRepo,
            String estensione, boolean cancellaOrfaniCentralizzati) {
        List<String> fileAggiornati = new ArrayList<>();
        String apiUrl = "https://api.github.com/repos/Lucapasselli/GiacenzeCrypto/contents/" + pathRepo + "?ref=master";

        OkHttpClient client = HTTP_CLIENT;
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "GiacenzeCrypto")
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("AggiornamentoConfig [" + pathRepo + "]: risposta API GitHub non valida, codice " + response.code());
                return fileAggiornati;
            }

            JsonArray files = JsonParser.parseString(response.body().string()).getAsJsonArray();

            Set<String> nomiRemoti = new HashSet<>();
            for (JsonElement el : files) {
                JsonObject file = el.getAsJsonObject();
                if (!"file".equals(file.get("type").getAsString())) continue;

                String name = file.get("name").getAsString();
                if (!name.toLowerCase().endsWith(estensione)) continue;

                String remoteSha = file.get("sha").getAsString();
                String downloadUrl = file.get("download_url").getAsString();

                nomiRemoti.add(name);
                Path localPath = Paths.get(cartellaLocale, name);

                boolean daAggiornare;
                if (!Files.exists(localPath)) {
                    daAggiornare = true;
                    System.out.println("AggiornamentoConfig [" + pathRepo + "]: file non presente localmente: " + name);
                } else {
                    String localSha = calcolaBlobShaGit(localPath);
                    daAggiornare = !remoteSha.equalsIgnoreCase(localSha);
                    if (daAggiornare) {
                        System.out.println("AggiornamentoConfig [" + pathRepo + "]: file da aggiornare (sha diverso): " + name);
                    }
                }

                if (daAggiornare) {
                    Files.createDirectories(localPath.getParent());
                    scaricaFileDaUrl(client, downloadUrl, localPath);
                    fileAggiornati.add(name);
                    System.out.println("AggiornamentoConfig [" + pathRepo + "]: aggiornato " + name);
                }
            }

            // Cancella file locali centralizzati non più presenti nel repository
            if (cancellaOrfaniCentralizzati) {
                File[] locali = new File(cartellaLocale).listFiles((dir, n) -> n.toLowerCase().endsWith(estensione));
                if (locali != null) {
                    for (File f : locali) {
                        if (nomiRemoti.contains(f.getName())) continue;
                        try {
                            StringBuilder sb = new StringBuilder();
                            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                                String riga;
                                while ((riga = br.readLine()) != null) sb.append(riga);
                            }
                            if (new JSONObject(sb.toString()).optBoolean("centralizzato", false)) {
                                Files.delete(f.toPath());
                                System.out.println("AggiornamentoConfig [" + pathRepo + "]: rimosso file centralizzato non più nel repository: " + f.getName());
                            }
                        } catch (Exception ex) {
                            System.out.println("AggiornamentoConfig [" + pathRepo + "]: errore nella verifica di " + f.getName() + " - " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("AggiornamentoConfig [" + pathRepo + "]: errore durante il controllo - " + e.getMessage());
            LoggerGC.ScriviErrore(e);
        }

        return fileAggiornati;
    }

    private static String calcolaBlobShaGit(Path filePath) throws Exception {
        byte[] content = Files.readAllBytes(filePath);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
        byte[] header = ("blob " + content.length + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        digest.update(header);
        digest.update(content);
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void scaricaFileDaUrl(OkHttpClient client, String url, Path destination) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "GiacenzeCrypto")
                .build();
        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Download fallito per " + url + ", codice: " + resp.code());
            }
            Files.write(destination, resp.body().bytes());
        }
    }

    /**
     * Verifica se una stringa è JSON sintatticamente valido, provando prima come oggetto e poi come array.
     * @param test stringa da validare
     * @return {@code true} se {@code test} è un {@link JSONObject} o un {@link JSONArray} valido, {@code false} altrimenti
     */
    public static boolean isValidJSON(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                LoggerGC.ScriviErrore("JSON non valido : " + test);
                return false;
            }
        }
        return true;
    }
}
