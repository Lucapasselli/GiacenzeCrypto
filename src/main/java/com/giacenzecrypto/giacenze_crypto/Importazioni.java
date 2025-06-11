/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */





/*Dati che compongono il nuovo database una volta importati i dati

0 - TrasID -> es. 202112031045_Binance_ScambioCryptoCrypto
1 - Data e ora -> es. 2021/12/03 10:45
2 - Numero di movimenti che compongono la transazione -> es. 1 di 3 (ovvero questa transazione è composta da 3 movimenti e questo è il primo movimento)
3 - Exchange -> es. Crypto.com
4 - Wallet -> es. EARN - FINANZIAMENTO oppure indirizzo wallet
5 - Tipo Transazione -> es. Scambio Crypto/Crypto (descrizione generica del tipo di transazione)
6 - Dettaglio Movimento -> es. Trasferimento Fiat - scambio CRO/USD - pool liquidità (descrizione dettagliata)
7 - Causale originale -> Causale originale come da CSV (es. Crypto_exchange)
8 - Moneta Venduta/Trasferita -> es. CRO oppure ETH oppure EUR
9 - Tipo Moneta Venduta/Trasferita -> es. Crypto oppure FIAT
10 - Quantità Venduta/Trasferita -> es. 10
11 - Moneta Acq/Ricevuta -> es. CRO oppure ETH oppure EUR
12 - Tipo Moneta Acq./Ricevuta -> es. Crypto oppure FIAT
13 - Quantità Acq./Ricevuta -> es. 10
14 - Valore di Mercato transazione (come da csv) -> es. 10 USD
15 - Valore di Mercato Transazione in EURO -> es. 9
16 - Costo di Carico Moneta Uscente
17 - Costo di carico Moneta Entrante
18 - Tipo Trasferimento
19 - Plusvalenza in EUR della Transazione -> es 3
20 - Riferimento x Trasferimenti -> Se è un traferimento si mette il riferimento al wallet/ transazione che l'ha generato es. 202112031045_Crypto.com_TraferimentoCrypto
21 - Note -> Eventuali note sulla transazione o sulla singola parte della transazione.
22 - Auto -> Segno con M se i valori sono stati modificati manualmente o con A se sono stati presi in automatico o AU se movimenti generati in automatico dal programma per trasferimenti interni
23 - [DEFI] Blocco Transazione
24 - [DEFI] Hash Transazione o ID Transazione Exchange come da CSV
25 - [DEFI] Nome Token Uscita
26 - [DEFI] Address Token Uscita
27 - [DEFI] Nome Token Entrata
28 - [DEFI] Address Token Entrata
29 - [DEFI (si può utilizzare anche per altro)] Timestamp
30 - [DEFI (si può utilizzare anche per altro)] Address Controparte
31 - Data Fine trasferimento crypto (viene anche utilizzata come data per lo spostamento del costo di carico tra wallet)
32 - Movimento ha prezzo (Valorizzato a Si o No)  //Serve per sapere se l'eventuale prezzo a zero è voluto o semplicemente non ho trovato i prezzi sul movimento
33 - Movimento che genera plusvalenza (Valorizzato a S o N)
34 - Rete (Attualmente solo BSC,ETH,CRO,ARB,BASE)//da implementare, per ora non gestito
35 - Campo di appoggio per prezzo transazione originale quando si classifica il movimento di deposito come Acquisto o Donazione
36 - Address di Provenienza (Attualmente implementato solo in BinanceTaxReport)
37 - Address di Destinazione (Attualmente Implementato solo in BananceTaxReport)
38 - Campo Libero per implementazioni future
39 - Campo Libero per implementazioni future
*/

         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees






package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.MappaCryptoWallet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.Funzioni_Date_ConvertiDatainLong;
import static com.giacenzecrypto.giacenze_crypto.ClassificazioneTrasf_Modifica.RiportaTransazioniASituazioneIniziale;
import com.giacenzecrypto.giacenze_crypto.TransazioneDefi.ValoriToken;
import java.awt.Component;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Collections;
import java.util.Set;


/**
 *
 * @author luca.passelli
 */
public class Importazioni {
    
    public static int Transazioni=0;
    public static int TransazioniAggiunte=0;
    public static int TrasazioniScartate=0;
    public static int TrasazioniSconosciute=0;
    public static int ColonneTabella=40;
    //La mappa delle chain conterrà per ogni chain l'indirizzo del chain explorer e relativa api
    public static String movimentiSconosciuti="";
    
    public static void AzzeraContatori()
            {           
                Transazioni=0;
                TransazioniAggiunte=0;
                TrasazioniScartate=0;
                TrasazioniSconosciute=0;
                movimentiSconosciuti="";
            }
           

    //23->Blocco Transazione
    //24->Hash Transazione
    //25->Nome Moneta Uscita
    //26->Contratto Moneta Uscita
    //27->Nome Moneta Entrata
    //28->Contratto Moneta Entrata
    //29->Timestamp
    //
    
        public static boolean Importa_Crypto_OKX(String fileOKX,boolean SovrascriEsistenti,Component c,Download progressb) {
        //Da sistemare problema su prezzi della giornata odierna/precendere che vanno in loop
        //Da sistemare problema con conversione dust su secondi diversi che da problemi
        //Da sistemare problema con il nuovo stakin che non viene conteggiato (FATTO MA NON SO IL RITIRO DALLO STAKING con che causale sarà segnalato) bisognerà fare delle prove
        //mettere almeno 1 secondo di tempo tra una richiesta e l'altra verso banchitalia
        
        AzzeraContatori();
        List<String[]> listaScambiDifferiti=new ArrayList<>();
        
        
        //Siccome OKX cambia le ore sul file csv a seconda della data in cui questo viene scaricato per prima cosa bisogna trovare tutte le transazioni di okx che ho in memoria
        //e recuparare tutti gli id, se ho transazioni con lo stesso id di transazioni in memoria allora non le devo importare.
        Map<String, String> MappaIDOKX=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String Trans[]:CDC_Grafica.MappaCryptoWallet.values()){
            if (Trans[3].equalsIgnoreCase("OKX")){

                    MappaIDOKX.put(Trans[24], "");
                
            }
        }
        
        String fileDaImportare = fileOKX;
       // System.out.println(fileBinance);
        Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //fee,sell,buy,Transaction Fee,Transaction Spend,Transaction Buy,Transaction Revenue,Transaction Sold
        //Sono tutte descrizioni che fanno parte di uno scambio tra crypto
        //in quel caso il movimento si compone di tre parti principali che però possono essee multiple e sono
        //le fee, la moneta venduta e la moneta acquistata
        //per gestire la transazione dovremmo prendere tutte le righe con lo stesso orario e sommarle per ottenere
        //i dati della transazione di scambio
        //le tipologie sono alla posizione 3

        
        Mappa_Conversione_Causali.put("Savings subscription",                   "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Savings redemption",                     "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Stake",                                  "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Redeem staking",                         "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("From unified trading account",           "TRASFERIMENTO-CRYPTO-INTERNO");      
        Mappa_Conversione_Causali.put("To unified trading account",             "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Simple Earn subscription",               "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Simple Earn redemption",                 "TRASFERIMENTO-CRYPTO-INTERNO");
        
        Mappa_Conversione_Causali.put("withdrawal",                             "TRASFERIMENTO-CRYPTO");
        Mappa_Conversione_Causali.put("deposit",                                "TRASFERIMENTO-CRYPTO");
        
        Mappa_Conversione_Causali.put("Convert",                                "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Buy",                                    "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Sell",                                   "SCAMBIO CRYPTO-CRYPTO");
        
        Mappa_Conversione_Causali.put("Deposit yield",                          "REWARD");
        Mappa_Conversione_Causali.put("Crypto dust auto-transfer in",           "REWARD");
        
        Mappa_Conversione_Causali.put("Transfer in",                            "NON CONSIDERARE");
        Mappa_Conversione_Causali.put("Transfer out",                           "NON CONSIDERARE");
        Mappa_Conversione_Causali.put("",                                       "NON CONSIDERARE");


        //il movimento che devo comporre per poi mandare al consolidamento deve avere le seguenti caratteristiche
        /*
        0 - Timestamp               (es. 2023-01-01 22:30:54)
        1 - Nome Exchange           (es. OKX)
        2 - Wallet exchange         (es. Principale)
        3 - Tipo Movimento Generico (es. SCAMBIO CRYPTO-CRYPTO)
        4 - Tipo Movimento da CSV   (es.redeem staking)
        5 - Moneta                  (es. BTC)
        6 - Qta                     (es. 1)
        7 - Tipo Moneta             (es. CRYPTO)
        8 - Moneta quotazione scambio (es. USD)
        9 - Quotazione scambio      (es. 25000)
        10 - Tipo Moneta Quotazione (es. FIAT)
        11 - Moneta Fee             (es. BTC)
        12 - Qta Fee                (es 0.0001) 
        13 - Tipo Moneta Fee        (es. CRYPTO)
        14 - Trasaction ID da CSV   (es. aadre111)
        15 - Doppio movimento Trasferimento interno (es. SI o NO)
                    se SI creo sia movimento di uscita che di entrata su nuovo wallet
                    se NO creo solo il movimento di entrata
        16 - Wallet di destinazione (nel caso in cui stia trasferendo qualcosa in un wallet conosciuto o da un wallet conosciuto)
        17 - Giacenza inizio movimento da CSV
        18 - Giacenza fine movimento da CSV
         */
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
       // Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String riga;
        String ultimaData = "";
        List<String[]> listaMovimentidaConsolidare = new ArrayList<>();
        List<String> righeFile = new ArrayList<>();
        int ColID=99,ColTime=99,ColTipoTrans=99,ColMoneta=99,ColQta=99,ColGiacIni=99,ColGiacFin=99,ColFee=99,ColMonFee=99;
            try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {

                //in questo modo butto tutto il in un array
                while ((riga = bure.readLine()) != null) {
                    //Primo giro del file recupero gli ID delle colonne e metto il tutto in ordine di ID che per fortuna è la prima colonna
                    //Quindi mi basta metterlo in un array ordinato e poi scorrere l'array
                    //devo metterli in ordine di id perchè è proprio quello che stabilisce l'ordine cronologico
                    //movimenti con la stessa data vengono infatti messi in ordine casuale e non corretto
                    riga=riga.replace("\"", "");//rimuovo le virgolette dal file
                    riga=riga.replaceAll("\\p{C}", ""); //rimuovo i carattere speciali ad inizio csv               
                    
                    //Trovo ora dove sono i campi del csv
                    String splittata[] = riga.split(",");
                    boolean SoloTesto=true;
                    for (String a :splittata){
                        if (Funzioni.Funzioni_isNumeric(a, false))SoloTesto=false;
                        
                    }
                    if (SoloTesto){
                        //Qui devo identificare le colonne e assegnarle il valore corretto
                        //System.out.println("Solo testo");
                        for (int i=0;i<splittata.length;i++){  
                           // System.out.println("."+splittata[i]+".");
                            if (splittata[i].trim().equalsIgnoreCase("Time")) ColTime=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("Type")) ColTipoTrans=i;
                            else if (splittata[i].trim().equalsIgnoreCase("Action")) ColTipoTrans=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("Amount")) ColQta=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("Before Balance")) ColGiacIni=i;
                            else if (splittata[i].trim().equalsIgnoreCase("After Balance")) ColGiacFin=i;
                            else if (splittata[i].trim().equalsIgnoreCase("Balance")) ColGiacFin=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("Symbol")) ColMoneta=i;                             
                            else if (splittata[i].trim().equalsIgnoreCase("Balance Unit")) ColMoneta=i;
                            else if (splittata[i].trim().equalsIgnoreCase("Unit")) ColMoneta=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("Fee Unit")) ColMonFee=i;
                            else if (splittata[i].trim().equalsIgnoreCase("Fee")) ColFee=i;
                            
                            else if (splittata[i].trim().equalsIgnoreCase("id")) ColID=i;
                        }
                        if (ColMonFee==99)ColMonFee=ColMoneta;
                    }
                    else {
                        righeFile.add(riga);
                    }
                }
             //   bure.close();
              //  fire.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            }
            Collections.sort(righeFile);
            progressb.SetMassimo(righeFile.size());
            progressb.SetAvanzamento(0);
            //System.out.println(righeFile.size());

            //Primo giro del file recupero gli ID delle colonne e metto il tutto in ordine di ID che per fortuna è la prima colonna
            //Quindi mi basta metterlo in un array ordinato e poi scorrere l'array
            //devo metterli in ordine di id perchè è proprio quello che stabilisce l'ordine cronologico
            //movimenti con la stessa data vengono infatti messi in ordine casuale e non corretto
            
            for (int w=0;w<righeFile.size();w++){
                progressb.avanzamento++;
                progressb.SetAvanzamento(progressb.avanzamento);
                if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                    return false;
                }
                riga=righeFile.get(w);
                //System.out.println(riga);
               
                String splittata[] = riga.split(",");
                //Se splittata è minore di 6 vuol dire che ci troviamo di fronte alla prima riga del file, in quel caso la salto
                //li infatti ci sono solo i dati cliente

                if (splittata.length>6){
                    //Adesso verifico se tra i campi c'è almeno un campo numerico
                    //cosi non fosse quella è l'intestazione del file e vado a leggere le colonne per capire che cosa sto importando
                if (OperazioniSuDate.ConvertiDatainLongSecondo(splittata[ColTime]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                {
                   // System.out.println("sono qua");
                    //se trovo movimento con stessa data oppure la data differisce di un solo secondosolo se è un dust conversion allora lo aggiungo alla lista che compone il movimento e vado avanti
                    //ho dovuto aggiungere la parte del secondo perchè quando fa i dust conversion può capitare che ci metta 1 secondo a fare tutti i movimenti
                    String MovGenerico=Mappa_Conversione_Causali.get(splittata[ColTipoTrans]);
                    if (MovGenerico==null)
                        {
                                //   System.out.println("Errore in importazione da CDCAPP csv: "+movimento);
                                   movimentiSconosciuti=movimentiSconosciuti+riga+"\n";
                                   TrasazioniSconosciute++;
                        }
                    String WalletDestinazione="";
                    if (splittata[ColTipoTrans].equalsIgnoreCase("Savings subscription"))WalletDestinazione="Savings";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("Savings redemption"))WalletDestinazione="Savings";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("Stake"))WalletDestinazione="Stake";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("Redeem staking"))WalletDestinazione="Stake";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("From unified trading account"))WalletDestinazione="Trading";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("To unified trading account"))WalletDestinazione="Trading";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("Simple Earn subscription"))WalletDestinazione="Earn";
                    else if (splittata[ColTipoTrans].equalsIgnoreCase("Simple Earn redemption"))WalletDestinazione="Earn";
                    //System.out.println(splittata[ColID]+"-"+splittata[ColTipoTrans]+"-"+splittata[ColTime]);
                    
                  
                    String DatoRiga[]=new String[19];
                    String NomeWallet="Funding";
                    if (ColTime!=1)NomeWallet="Trading";
                    if (ColTime!=99) DatoRiga[0]=splittata[ColTime];//Timestamp
                    DatoRiga[1]="OKX";//exchange
                    DatoRiga[2]=NomeWallet;//Wallet   ----> Da Sistemare in base al tipo di import
                    DatoRiga[3]="";//conversione tipo movimento
                    if (MovGenerico!=null)DatoRiga[3]=MovGenerico;//conversione tipo movimento
                    if (ColTipoTrans!=99) DatoRiga[4]=splittata[ColTipoTrans];//Tipo Movimento csv
                    if (ColMoneta!=99) DatoRiga[5]=splittata[ColMoneta];//Moneta
                    if (ColQta!=99) DatoRiga[6]=splittata[ColQta];//Qta.... se tipo =sell allora devo mettere un meno davanti
                    if (splittata[ColTipoTrans].equalsIgnoreCase("sell"))DatoRiga[6]="-"+DatoRiga[6];
                    DatoRiga[7]="";
                    DatoRiga[8]="";
                    DatoRiga[9]="";
                    DatoRiga[10]="";
                    if (ColMonFee!=99) DatoRiga[11]=splittata[ColMonFee];//Moneta fee
                    if (ColFee!=99) DatoRiga[12]=new BigDecimal(splittata[ColFee]).stripTrailingZeros().toPlainString();//Qta fee
                    if (Funzioni.Funzioni_isNumeric(DatoRiga[12], false)&&
                            new BigDecimal(DatoRiga[12]).compareTo(new BigDecimal(0))==0){
                        //al posto di lasciare il campo vuoto mette 0-E8 ogni tanto per cui devo individuarlo e valorizzare il campo a vuoto dovessi individuarlo
                        DatoRiga[12]="";
                    }
                    //System.out.println(DatoRiga[12]);
                    DatoRiga[13]="";
                    if (ColID!=99) DatoRiga[14]=splittata[ColID];//ID                    
                    DatoRiga[15]="SI";
                    DatoRiga[16]=WalletDestinazione;
                    if (ColGiacIni!=99) DatoRiga[17]=splittata[ColGiacIni];
                    if (ColGiacFin!=99) DatoRiga[18]=splittata[ColGiacFin];
                    RiempiVuotiArray(DatoRiga);
                    
                    String secondo=splittata[ColTime].split(":")[2];
                    int secondoInt=Integer.parseInt(secondo)-1;
                    secondo=String.valueOf(secondoInt);//secondo è secondo meno 1
                   /* System.out.println(splittata[ColTime]);
                   // System.out.println(ultimaData);
                   // System.out.println("-------");*/
                    if (secondo.length()==1)secondo="0"+secondo;
                    String DataMeno1Secondo = splittata[ColTime].split(":")[0] + ":" + splittata[ColTime].split(":")[1] + ":" + secondo;
                    if (splittata[ColTime].equalsIgnoreCase(ultimaData)) {
                        listaMovimentidaConsolidare.add(DatoRiga);
                        
                        if (listaMovimentidaConsolidare.size() > 1) {
                            //Scorro la lista se esiste un movimento di sell e uno di buy consolido il movimento
                            boolean trovatoBuy=false;
                            boolean trovatoSell=false;
                            for (String rigaLista[]:listaMovimentidaConsolidare){
                                if (rigaLista[4].equalsIgnoreCase("Buy"))  trovatoBuy=true;
                                if (rigaLista[4].equalsIgnoreCase("Sell")) trovatoSell=true;  
                            }
                            if (trovatoSell&&trovatoBuy){
                                //Consolido il movimento
                                //System.out.println("consolido il movimento");
                                List<String[]> listaConsolidata = ConsolidaMovimentiGenerica(listaMovimentidaConsolidare, Mappa_Conversione_Causali, listaScambiDifferiti);
                                int nElementi = listaConsolidata.size();
                                for (int i = 0; i < nElementi; i++) {
                                    String consolidata[] = listaConsolidata.get(i);
                                    Mappa_Movimenti.put(consolidata[0], consolidata);
                                }
                                //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                                listaMovimentidaConsolidare = new ArrayList<>();

                            }
                        }
                    } else if (DataMeno1Secondo.equalsIgnoreCase(ultimaData)
                            && splittata[ColTipoTrans].contains("Convert")) {//intercetto i movimenti che avvengono ad 1 secondo di distanza
                        listaMovimentidaConsolidare.add(DatoRiga);

                    }

                    else //altrimenti consolido il movimento precedente
                    {

                        List<String[]> listaConsolidata = ConsolidaMovimentiGenerica(listaMovimentidaConsolidare, Mappa_Conversione_Causali,listaScambiDifferiti);
                        int nElementi = listaConsolidata.size();
                        for (int i = 0; i < nElementi; i++) {
                            String consolidata[] = listaConsolidata.get(i);
                            Mappa_Movimenti.put(consolidata[0], consolidata);
                        }

                        //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                        listaMovimentidaConsolidare = new ArrayList<>();
                        listaMovimentidaConsolidare.add(DatoRiga);
                    }
                    ultimaData = splittata[ColTime];

                }
                
                }
            }
            List<String[]> listaConsolidata = ConsolidaMovimentiGenerica(listaMovimentidaConsolidare, Mappa_Conversione_Causali,listaScambiDifferiti);
          //  List<String> listaAutoinvestimenti=new ArrayList()<>;
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                //System.out.println(consolidata[2].split(" di ")[0].trim());               
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }
            

         //   bure.close();
          //  fire.close();
     

        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           String IDdaCSV=Mappa_Movimenti.get(v)[24];
         /*  System.out.println(IDdaCSV);
         //  System.out.println();*/
           numeromov++;
           if (!(MappaCryptoWallet.get(v)!=null||//se trovo la stessa id transazione
                    MappaIDOKX.get(IDdaCSV)!=null)//o se trovo lo stesso id dal csv scarto il movimento (la mappa degli id la genero prima di far partire l'importazione)
                   ||SovrascriEsistenti)
           {

             //  MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
       //questo lo faccio alla fine perchè vado ad agire direttamente sulla mappa già compilata
       //assengnado i movimenti aggiuntivi
       ConsolidaMovimentiDifferiti(listaScambiDifferiti,SovrascriEsistenti);
    //   System.out.println(listaScambiDifferiti.get(0)[5]);
    //   System.out.println(MappaCryptoWallet.get(listaScambiDifferiti.get(0)[0])[5]);
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        if (TransazioniAggiunte>0)CDC_Grafica.TransazioniCrypto_DaSalvare=true;
        
        
    return true;    
    }
    
    
   

        

        
/*public static void Importa_Solana_OKHTTP(){
            String RPC_URL = "https://api.mainnet-beta.solana.com";
            String WALLET_ADDRESS = "E2ebWw5CLJGbgDLvnEK19ES6QceouQLMbxHJRzieM6FL";
            OkHttpClient client = new OkHttpClient();

        // Passaggio 1: Ottieni la lista delle transazioni
        String jsonPayload = "{"
                + "\"jsonrpc\": \"2.0\","
                + "\"id\": 1,"
                + "\"method\": \"getSignaturesForAddress\","
                + "\"params\": [\"" + WALLET_ADDRESS + "\", {\"limit\": 5}]"
                + "}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));


        Request request = new Request.Builder().url(RPC_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                System.out.println("Sono qui");
                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONArray transactions = jsonResponse.getJSONArray("result");
System.out.println(response.body().string());
                for (int i = 0; i < transactions.length(); i++) {
                    JSONObject tx = transactions.getJSONObject(i);
                    String signature = tx.getString("signature");
                    System.out.println (signature);
                    // Passaggio 2: Ottieni i dettagli della transazione
                    getTransactionDetails(client, signature,RPC_URL);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        }
*/


        
 /*      private static void getTransactionDetails(OkHttpClient client, String signature,String RPC_URL) throws IOException {
        String jsonPayload = "{"
                + "\"jsonrpc\": \"2.0\","
                + "\"id\": 1,"
                + "\"method\": \"getTransaction\","
                + "\"params\": [\"" + signature + "\", \"json\"]"
                + "}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        Request request = new Request.Builder().url(RPC_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONObject txDetails = jsonResponse.getJSONObject("result");

                double fee = txDetails.getJSONObject("meta").getDouble("fee") / 1_000_000_000;
                JSONArray postTokenBalances = txDetails.getJSONObject("meta").getJSONArray("postTokenBalances");
                JSONArray preTokenBalances = txDetails.getJSONObject("meta").getJSONArray("preTokenBalances");

                System.out.println("Tx Signature: " + signature);
                System.out.println("Fee: " + fee + " SOL");

                // Analizza token in entrata e uscita
                for (int i = 0; i < postTokenBalances.length(); i++) {
                    JSONObject postBalance = postTokenBalances.getJSONObject(i);
                    JSONObject preBalance = preTokenBalances.getJSONObject(i);

                    String mint = postBalance.getString("mint");
                    double preAmount = preBalance.getDouble("uiTokenAmount");
                    double postAmount = postBalance.getDouble("uiTokenAmount");
                    double change = postAmount - preAmount;

                    System.out.println("Token: " + mint);
                    System.out.println("Quantità: " + change);
                }
                System.out.println("--------------------");
            }
        }
       }
    
    */
    
        public static boolean Importa_Crypto_Binance(String fileBinance,boolean SovrascriEsistenti,Component c,Download progressb) {
        //Da sistemare problema su prezzi della giornata odierna/precendere che vanno in loop
        //Da sistemare problema con conversione dust su secondi diversi che da problemi
        //Da sistemare problema con il nuovo stakin che non viene conteggiato (FATTO MA NON SO IL RITIRO DALLO STAKING con che causale sarà segnalato) bisognerà fare delle prove
        //mettere almeno 1 secondo di tempo tra una richiesta e l'altra verso banchitalia
        
        AzzeraContatori();
        List<String[]> listaScambiDifferiti=new ArrayList<>();
        
        String fileDaImportare = fileBinance;
       // System.out.println(fileBinance);
        Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //fee,sell,buy,Transaction Fee,Transaction Spend,Transaction Buy,Transaction Revenue,Transaction Sold
        //Sono tutte descrizioni che fanno parte di uno scambio tra crypto
        //in quel caso il movimento si compone di tre parti principali che però possono essee multiple e sono
        //le fee, la moneta venduta e la moneta acquistata
        //per gestire la transazione dovremmo prendere tutte le righe con lo stesso orario e sommarle per ottenere
        //i dati della transazione di scambio
        //le tipologie sono alla posizione 3
        Mappa_Conversione_Causali.put("Binance Card Cashback",                      "CASHBACK");              //Cashback
        Mappa_Conversione_Causali.put("Card Cashback",                              "CASHBACK");              //Cashback        
        Mappa_Conversione_Causali.put("Simple Earn Flexible Interest",              "EARN");  
        Mappa_Conversione_Causali.put("Simple Earn Locked Rewards",                 "EARN");//
        Mappa_Conversione_Causali.put("Launchpool Earnings Withdrawal",             "EARN");//
        Mappa_Conversione_Causali.put("Cash Voucher Distribution",                  "REWARD");//
        Mappa_Conversione_Causali.put("Airdrop Assets",                             "REWARD");//
        Mappa_Conversione_Causali.put("Launchpool Airdrop",                         "REWARD");//non li metto in airdrop perchè sono comunque rewards date per detenzione
        Mappa_Conversione_Causali.put("Cashback Voucher",                           "CASHBACK");
        Mappa_Conversione_Causali.put("Megadrop Rewards",                           "REWARD");
        Mappa_Conversione_Causali.put("Staking Rewards",                            "STAKING REWARDS");//
        Mappa_Conversione_Causali.put("Distribution",                               "REWARD");//
        Mappa_Conversione_Causali.put("BNB Vault Rewards",                          "REWARD");//
        Mappa_Conversione_Causali.put("ETH 2.0 Staking Rewards",                    "STAKING REWARDS");//
        Mappa_Conversione_Causali.put("Simple Earn Flexible Subscription",          "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Simple Earn Flexible Redemption",            "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Simple Earn Locked Subscription",            "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Simple Earn Locked Redemption",              "TRASFERIMENTO-CRYPTO-INTERNO");//
        Mappa_Conversione_Causali.put("Transfer Between Main and Funding Wallet",   "TRASFERIMENTO-CRYPTO-INTERNO");      
        Mappa_Conversione_Causali.put("Staking Purchase",                           "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Staking Redemption",                         "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Main and Funding Account Transfer",          "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("transfer_in",                                "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("transfer_out",                               "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Transfer Between Spot and Strategy Account", "TRASFERIMENTO-CRYPTO-INTERNO");
        Mappa_Conversione_Causali.put("Launchpool Subscription/Redemption",         "TRASFERIMENTO-CRYPTO-INTERNO");

        Mappa_Conversione_Causali.put("withdraw",                                   "TRASFERIMENTO-CRYPTO");
        Mappa_Conversione_Causali.put("deposit",                                    "TRASFERIMENTO-CRYPTO");
        Mappa_Conversione_Causali.put("Fiat OCBS - Add Fiat and Fees",              "TRASFERIMENTO-CRYPTO");
        //Fiat OCBS - Add Fiat and Fees
        //Buy Crypto With Card
        // La causale di autoinvestimento la dovrò poi convertire in Scambio Crypto Differito
        // Possono passare infatti anche diversi minuti tra il movimento di uscita e quello di entrata
        Mappa_Conversione_Causali.put("Auto-Invest Transaction",                    "SCAMBIO DIFFERITO");
        Mappa_Conversione_Causali.put("Asset Recovery",                             "SCAMBIO DIFFERITO");//08-01-2025
        Mappa_Conversione_Causali.put("Token Swap - Distribution",                  "SCAMBIO DIFFERITO");//08-01-2025
        Mappa_Conversione_Causali.put("Small Assets Exchange BNB (Spot)",           "DUST-CONVERSION");
        Mappa_Conversione_Causali.put("Small Assets Exchange BNB",                  "DUST-CONVERSION");
        Mappa_Conversione_Causali.put("Buy Crypto With Card",                       "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Transaction Buy",                            "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Transaction Sold",                           "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Transaction Spend",                          "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Transaction Revenue",                        "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Binance Convert",                            "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Sell Crypto To Fiat",                        "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Buy",                                        "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Sell",                                       "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("Transaction Related",                        "SCAMBIO CRYPTO-CRYPTO");
        Mappa_Conversione_Causali.put("ETH 2.0 Staking",                            "SCAMBIO CRYPTO-CRYPTO");//
        Mappa_Conversione_Causali.put("ETH 2.0 Staking Withdrawals",                "SCAMBIO CRYPTO-CRYPTO");//
        
        Mappa_Conversione_Causali.put("Transaction Fee",                            "COMMISSIONI");
        Mappa_Conversione_Causali.put("Fee",                                        "COMMISSIONI");
        Mappa_Conversione_Causali.put("Strategy Trading Fee Rebate",                "COMMISSIONI");//Da Verificare
        
        
        Mappa_Conversione_Causali.put("Fiat Deposit",                               "DEPOSITO FIAT");
        Mappa_Conversione_Causali.put("Binance Card Spending",                      "PRELIEVO FIAT");
        Mappa_Conversione_Causali.put("Fund Recovery",                              "PRELIEVO FIAT");
        Mappa_Conversione_Causali.put("Fiat Withdraw",                              "PRELIEVO FIAT");
        
        Mappa_Conversione_Causali.put("Buy Crypto",                                 "ACQUISTO CRYPTO");
        Mappa_Conversione_Causali.put("Buy Crypto With Fiat",                       "ACQUISTO CRYPTO");//Inserito il 11/12/2024
        Mappa_Conversione_Causali.put("Convert Fiat to Stablecoin Paysafe",         "ACQUISTO CRYPTO");//Inserito il 21/07/2024
        Mappa_Conversione_Causali.put("Tax Liquidation",                            "VENDITA CRYPTO");//Inserito il 08/01/2025
        
        Mappa_Conversione_Causali.put("Referral Commission",                        "REWARD");//Inserito il 21/07/2024
        Mappa_Conversione_Causali.put("Crypto Box",                                 "REWARD"); // Red carpet Binance rewards, Inserito il 21/07/2024
        
        //,Auto-Invest Transaction
        //Binance Card Spending
      /*  Mappa_Conversione_Causali.put("Fiat Deposit", "DEPOSITO FIAT");        //Scambio di una Crypto per un'altra Crypto*/
        
         
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
       // Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String riga;
        String ultimaData = "";
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
        List<String> righeFile = new ArrayList<>();
            try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {

                //in questo modo butto tutto il in un array
                while ((riga = bure.readLine()) != null) {
                    riga=riga.replace("\"", "");//rimuovo le virgolette dal file
                    righeFile.add(riga);
                }
              //  bure.close();
              //  fire.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
            }
            progressb.SetMassimo(righeFile.size());
            progressb.SetAvanzamento(0);
      /*  int avanzamento=0;
        for (String str : Mappa_MovimentiTemporanea.keySet()) {
            if (progressb.FineThread()){
            //se è stato interrotta la finestra di progresso interrompo il ciclo
                return false;
                }*/
            //System.out.println(righeFile.size());
            for (int w=0;w<righeFile.size();w++){
                progressb.avanzamento++;
                progressb.SetAvanzamento(progressb.avanzamento);
                if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                    return false;
                }
                riga=righeFile.get(w);
               
                String splittata[] = riga.split(",");
              //   System.out.println(splittata[2]);
                if (OperazioniSuDate.ConvertiDatainLongSecondo(splittata[1]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                {
                   // System.out.println("sono qua");
                    //se trovo movimento con stessa data oppure la data differisce di un solo secondosolo se è un dust conversion allora lo aggiungo alla lista che compone il movimento e vado avanti
                    //ho dovuto aggiungere la parte del secondo perchè quando fa i dust conversion può capitare che ci metta 1 secondo a fare tutti i movimenti
                    String secondo=splittata[1].split(":")[2];
                    int secondoInt=Integer.parseInt(secondo)-1;
                    secondo=String.valueOf(secondoInt);//secondo è secondo meno 1
                    if (secondo.length()==1)secondo="0"+secondo;
                    String DataMeno1Secondo=splittata[1].split(":")[0]+":"+splittata[1].split(":")[1]+":"+secondo;
                    if (splittata[1].equalsIgnoreCase(ultimaData)) {
                        listaMovimentidaConsolidare.add(riga);
                    }else if(DataMeno1Secondo.equalsIgnoreCase(ultimaData)&&splittata[3].contains("Small Assets Exchange BNB")||
                            DataMeno1Secondo.equalsIgnoreCase(ultimaData)&&splittata[3].contains("Binance Convert")||
                            DataMeno1Secondo.equalsIgnoreCase(ultimaData)&&splittata[3].contains("Transaction Related")){//SOLO per i dust conversion,binance convert e transaction related
                        listaMovimentidaConsolidare.add(riga);
                        }
                    else //altrimenti consolido il movimento precedente
                    {
                     //   System.out.println(riga);
                        List<String[]> listaConsolidata = ConsolidaMovimenti_Binance(listaMovimentidaConsolidare, Mappa_Conversione_Causali,listaScambiDifferiti);
                        int nElementi = listaConsolidata.size();
                        for (int i = 0; i < nElementi; i++) {
                            String consolidata[] = listaConsolidata.get(i);
                            Mappa_Movimenti.put(consolidata[0], consolidata);
                        }

                        //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                        listaMovimentidaConsolidare = new ArrayList<>();
                        listaMovimentidaConsolidare.add(riga);
                    }
                    ultimaData = splittata[1];

                }

            }
            List<String[]> listaConsolidata = ConsolidaMovimenti_Binance(listaMovimentidaConsolidare, Mappa_Conversione_Causali,listaScambiDifferiti);
          //  List<String> listaAutoinvestimenti=new ArrayList()<>;
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                //System.out.println(consolidata[2].split(" di ")[0].trim());               
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }
            

         //   bure.close();
          //  fire.close();
     

        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {

             //  MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
       //questo lo faccio alla fine perchè vado ad agire direttamente sulla mappa già compilata
       //assengnado i movimenti aggiuntivi
       ConsolidaMovimentiDifferiti(listaScambiDifferiti,SovrascriEsistenti);
    //   System.out.println(listaScambiDifferiti.get(0)[5]);
    //   System.out.println(MappaCryptoWallet.get(listaScambiDifferiti.get(0)[0])[5]);
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        if (TransazioniAggiunte>0)CDC_Grafica.TransazioniCrypto_DaSalvare=true;
        
        
    return true;    
    }
    
    
    public static void InserisciMovimentosuMappaCryptoWallet(String Chiave, String[] Valore) {
       //QUA DOVRO' INSERIRE ANCHE L'EVENTUALE CAMBIO NOME DEL TOKEN
       //PER EVITARE DI FARLO AD OGNI CARICAMENTO DI TABELLA E AUMENTARE LA VELOCITA' MA LO VEDRO' CON CALMA PIU' AVANTI  
       //POSSO ANCHE GESTIRE L'EVENTUALE IDENTIFICAZIONE DI UN TOKEN SCAM (DA VEDERE CON CALMA)
      /*  String AddressUscita=Valore[25];
        String AddressEntrata=Valore[27];
        String Rete = Funzioni.TrovaReteDaID(Chiave);*/



        //Questa funzione inserisce il movimento in mappa ma prima di fare ciò elimina eventuali associazioni sul movimento precedente
        //FASE 1 : VERIFICO SE IL MOVIMENTO ESISTE
        if (MappaCryptoWallet.get(Chiave) != null) {
            //FASE 2 : SE ESISTE IL MOVIMENTO ELIMINA EVENTUALI ASSOCIAZIONI VECCHIE SUL MOVIMENTO DA SOSTITUIRE
            String PartiCoinvolte[] = (Chiave + "," + MappaCryptoWallet.get(Chiave)[20]).split(",");
            RiportaTransazioniASituazioneIniziale(PartiCoinvolte);
        }
        //FASE 3: Inserisocil movimento in mappa
        MappaCryptoWallet.put(Chiave, Valore);
    }
    
    
    public static void Importa_Crypto_CDCApp(String fileCDCapp,boolean SovrascriEsistenti) {
        //Da sistemare problema su prezzi della giornata odierna/precendere che vanno in loop
        //Da sistemare problema con conversione dust su secondi diversi che da problemi
        //Da sistemare problema con il nuovo stakin che non viene conteggiato (FATTO MA NON SO IL RITIRO DALLO STAKING con che causale sarà segnalato) bisognerà fare delle prove
        //mettere almeno 1 secondo di tempo tra una richiesta e l'altra verso banchitalia
        
        AzzeraContatori();
        
        String fileDaImportare = fileCDCapp;
        Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //Faccio una lista di causali per la conversione dei dati del csv
        Mappa_Conversione_Causali.put("card_cashback_reverted", "CASHBACK");              //Cashback ripristinato
        Mappa_Conversione_Causali.put("referral_card_cashback", "CASHBACK");              //Cashback della Carta MCO
        Mappa_Conversione_Causali.put("crypto_earn_interest_paid", "EARN");               //Interessi maturati da una Crypto in Earn

        Mappa_Conversione_Causali.put("crypto_exchange", "SCAMBIO CRYPTO-CRYPTO");        //Scambio di una Crypto per un'altra Crypto
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_exchange", "SCAMBIO CRYPTO-CRYPTO");//Ordine Limite Eseguito 
        Mappa_Conversione_Causali.put("trading.limit_order.crypto_wallet.exchange", "SCAMBIO CRYPTO-CRYPTO");//Ordine Limite Eseguito
        
        Mappa_Conversione_Causali.put("crypto_deposit", "TRASFERIMENTO-CRYPTO");          //Deposito di Crypto provenienti da wallet esterno
        Mappa_Conversione_Causali.put("crypto_withdrawal", "TRASFERIMENTO-CRYPTO");       //Prelievo di una Crypto verso portafogli esterni
        Mappa_Conversione_Causali.put("crypto_to_exchange_transfer", "TRASFERIMENTO-CRYPTO");//Trasferimento di una Crypto dall'App verso l'Exchange
        Mappa_Conversione_Causali.put("crypto_transfer", "TRASFERIMENTO-CRYPTO");       //Trasferimento verso o da altro portafoglio crypto.com tramite app 
        Mappa_Conversione_Causali.put("exchange_to_crypto_transfer", "TRASFERIMENTO-CRYPTO");    //Trasferimenti dall'Exchange verso l'App
        
        Mappa_Conversione_Causali.put("crypto_purchase", "ACQUISTO CRYPTO");          //Acquisto di Crypto da Carta di Credito
        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.purchase_commit", "ACQUISTO CRYPTO");//Acquisto crypto da fill order limit
        Mappa_Conversione_Causali.put("trading.crypto_purchase.google_pay", "ACQUISTO CRYPTO");//Acquisto crypto da fill order limit
        Mappa_Conversione_Causali.put("viban_purchase", "ACQUISTO CRYPTO");           //Acquisto di Crypto dal portafoglio EUR 
        Mappa_Conversione_Causali.put("recurring_buy_order", "ACQUISTO CRYPTO");//Acquisto Crypto tramite acquisti ricorrenti
        
        Mappa_Conversione_Causali.put("crypto_viban_exchange", "VENDITA CRYPTO");    //Vendita di una Crypto verso il portafoglio EUR
        Mappa_Conversione_Causali.put("card_top_up", "VENDITA CRYPTO");    //Vendita di una Crypto verso il portafoglio EUR
        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.sell_commit", "VENDITA CRYPTO");//Vendita crypto da fill order limit
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_credited", fileDaImportare);    //Scambio MCO in CRO (MCO liberi nel portafoglio). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_debited", fileDaImportare);     //Scambio MCO in CRO (MCO liberi nel portafoglio). Vendita degli MCO

        Mappa_Conversione_Causali.put("dust_conversion_credited", "DUST-CONVERSION");//Conversione di Crypto in CRO. CRO Ricevuti dalla conversione.
        Mappa_Conversione_Causali.put("dust_conversion_debited", "DUST-CONVERSION");//Conversione di Crypto in CRO. Crypto da convertire in CRO.
        Mappa_Conversione_Causali.put("crypto_wallet_swap_credited", "DUST-CONVERSION");//Conversione di monete
        Mappa_Conversione_Causali.put("crypto_wallet_swap_debited", "DUST-CONVERSION");//Conversione di monete       
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_bonus_exchange_deposit", fileDaImportare);//Bonus Swap MCO/CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_credited", fileDaImportare);     //Scambio MCO in CRO (MCO in Earn). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_debited", fileDaImportare);      //Scambio MCO in CRO (MCO in Earn). Vendita degli MCO


        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.purchase_unlock", "IGNORA"); //Ignoro il movimento in quanto sto bloccando Euro del Fiat Wallet
        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.purchase_lock", "IGNORA"); //Ignoro il movimento in quanto sto bloccando fondi del Fiat Wallet
        Mappa_Conversione_Causali.put("trading.limit_order.crypto_wallet.fund_lock", "IGNORA");          //Limit order
        Mappa_Conversione_Causali.put("trading.limit_order.crypto_wallet.fund_unlock", "IGNORA");          //Limit order     
        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.sell_unlock", "IGNORA");          //Limit order
        Mappa_Conversione_Causali.put("trading.limit_order.fiat_wallet.sell_lock", "IGNORA");          //Limit order  
        
        Mappa_Conversione_Causali.put("lockup_lock", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo Stake
        Mappa_Conversione_Causali.put("lockup_unlock", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo unStake
        Mappa_Conversione_Causali.put("finance.lockup.dpos_lock.crypto_wallet", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo Stake
        Mappa_Conversione_Causali.put("finance.lockup.dpos_unlock.crypto_wallet", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo unStake
        Mappa_Conversione_Causali.put("crypto_earn_program_created", "TRASFERIMENTO-CRYPTO-INTERNO");//Inserimento di una Crypto in Earn
        Mappa_Conversione_Causali.put("crypto_earn_program_withdrawn", "TRASFERIMENTO-CRYPTO-INTERNO");//Prelievo di una Crypto dall'Earn
        Mappa_Conversione_Causali.put("finance.dpos.staking.crypto_wallet", "TRASFERIMENTO-CRYPTO-INTERNO");    //Nuovo Staking di Crypto.com
        Mappa_Conversione_Causali.put("finance.dpos.unstaking.crypto_wallet", "TRASFERIMENTO-CRYPTO-INTERNO");      //unstake
        Mappa_Conversione_Causali.put("lockup_upgrade", "TRASFERIMENTO-CRYPTO-INTERNO");       //CRO Stake per la MCO Card. (Upgrade)
        Mappa_Conversione_Causali.put("supercharger_deposit", "TRASFERIMENTO-CRYPTO-INTERNO"); //Deposito dei CRO nel supercharger
        Mappa_Conversione_Causali.put("supercharger_withdrawal", "TRASFERIMENTO-CRYPTO-INTERNO");//Prelievo dei CRO dal supercharger
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_fund_lock", "TRASFERIMENTO-CRYPTO-INTERNO");//Blocca i fondi destinati ad un'ordine Limit 

//        Mappa_Conversione_Causali.put("lockup_swap_credited", fileDaImportare);         //Scambio MCO in CRO (MCO in Stake per la Carta). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("lockup_swap_debited", fileDaImportare);          //Scambio MCO in CRO (MCO in Stake per la Carta). Vendita degli MCO


        Mappa_Conversione_Causali.put("mco_stake_reward", "STAKING REWARD");                       //Interessi che la MCO Card matura. Da (Jade in su)
        Mappa_Conversione_Causali.put("finance.dpos.non_compound_interest.crypto_wallet", "STAKING REWARD");    //Nuovo Staking di Crypto.com
        Mappa_Conversione_Causali.put("finance.dpos.compound_interest.crypto_wallet", "STAKING REWARD");
        Mappa_Conversione_Causali.put("staking_reward", "STAKING REWARD");                       //Reward (Es. NEO Gas) 
        Mappa_Conversione_Causali.put("finance.lockup.dpos_compound_interest.crypto_wallet", "STAKING REWARD");//Reward da staking con Carta 20/07/2024

        Mappa_Conversione_Causali.put("pay_checkout_reward", "REWARD");                   //Ricompesa di Crypto.com Pay
        Mappa_Conversione_Causali.put("finance.crypto_earn.loyalty_program_extra_interest_paid.crypto_wallet", "REWARD"); //12-01-2024
        Mappa_Conversione_Causali.put("referral_gift", "REWARD");                         //Bonus di iscrizione sbloccato
        Mappa_Conversione_Causali.put("reimbursement", "REWARD");                         //Rimborsi (Es. Netflix, Promozioni)
        Mappa_Conversione_Causali.put("reimbursement_reverted", "REWARD");                //Annullamento di un rimborso (o parte)
        Mappa_Conversione_Causali.put("reward.loyalty_program.trading_rebate.crypto_wallet", "REWARD");                //Altre reward
        Mappa_Conversione_Causali.put("campaign_reward", "REWARD");                       //Vincita di una campagna (Es.: Telegram Madness
        Mappa_Conversione_Causali.put("crypto_payment_refund", "REWARD");                 //Rimborso in Crypto. (Es. Rimborso Offerta per un NFT)
        Mappa_Conversione_Causali.put("referral_bonus", "REWARD");                        //Bonus Referral 
        Mappa_Conversione_Causali.put("crypto_earn_extra_interest_paid", "REWARD");//Earn Extra Reward 
        Mappa_Conversione_Causali.put("supercharger_reward_to_app_credited", "REWARD");//Supercharger Reward in App
        Mappa_Conversione_Causali.put("rewards_platform_deposit_credited", "REWARD");//Mission Reward

        


//        Mappa_Conversione_Causali.put("nft_payout_credited", fileDaImportare);            //Vendita NFT 


      
//        Mappa_Conversione_Causali.put("crypto_credit_withdrawal_created", fileDaImportare);//Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_repayment_created", fileDaImportare);//Crypto Loan        
//        Mappa_Conversione_Causali.put("crypto_credit_loan_credited", fileDaImportare);    //Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_program_created", fileDaImportare);  //Crypto Loan 
        Mappa_Conversione_Causali.put("admin_wallet_credited", "ALTRE-REWARD");//es. aggiustamenti luna 
        Mappa_Conversione_Causali.put("transfer_cashback", "CASHBACK");              //Cashback su trasferimento crypto tra portafogli

        //QUESTE 2 SOTTO SONO ANCORA DA GESTIRE
        Mappa_Conversione_Causali.put("crypto_payment", "VENDITA CRYPTO");              //Pagamento in Crypto (Es.: Crypto Pay in CRO)

        
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
       // Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String riga;
        String ultimaData = "";
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
        List<String> righeFile = new ArrayList<>();

            //in questo modo butto tutto il file in un array che poi verrà riordinato in modo da avere i movimenti in ordine di data
            while ((riga = bure.readLine()) != null) {
                righeFile.add(riga);
                }
            Collections.sort(righeFile);
            Collections.sort(righeFile);
            
            for (int w=0;w<righeFile.size();w++){
                
                riga=righeFile.get(w);
                //System.out.println(riga);
                String splittata[] = riga.split(",");
                String TipologiaOperazione=Mappa_Conversione_Causali.get(splittata[9]);
                if (Funzioni_Date_ConvertiDatainLong(splittata[0]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                {
                    //se trovo movimento con stessa data oppure la data differisce di un solo secondosolo se è un dust conversion allora lo aggiungo alla lista che compone il movimento e vado avanti
                    //ho dovuto aggiungere la parte del secondo perchè quando fa i dust conversion può capitare che ci metta 1 secondo a fare tutti i movimenti
                    String data_1=OperazioniSuDate.ConvertiDatadaLongAlSecondo(OperazioniSuDate.ConvertiDatainLongSecondo(splittata[0])-1000);
                   /* String secondo=splittata[0].split(":")[2];
                    int secondoInt=Integer.parseInt(secondo)-1;
                    secondo=String.valueOf(secondoInt);
                    if (secondo.length()==1)secondo="0"+secondo;
                    String DataMeno1Secondo=splittata[0].split(":")[0]+":"+splittata[0].split(":")[1]+":"+secondo;*/
                    if (splittata[0].equalsIgnoreCase(ultimaData)) {
                        listaMovimentidaConsolidare.add(riga);
                    }else if(data_1.equalsIgnoreCase(ultimaData)&&TipologiaOperazione!=null&&TipologiaOperazione.contains("DUST")){//SOLO per i dust conversion
                        listaMovimentidaConsolidare.add(riga);
                        }
                    else //altrimenti consolido il movimento precedente
                    {
                       // System.out.println(riga);
                        List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
                        int nElementi = listaConsolidata.size();
                        for (int i = 0; i < nElementi; i++) {
                            String consolidata[] = listaConsolidata.get(i);
                            Mappa_Movimenti.put(consolidata[0], consolidata);
                        }

                        //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                        listaMovimentidaConsolidare = new ArrayList<>();
                        listaMovimentidaConsolidare.add(riga);
                    }
                    ultimaData = splittata[0];

                }

            }
            List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                //System.out.println(consolidata[2].split(" di ")[0].trim());               
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }

         //   bure.close();
          //  fire.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }     

        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {

               //MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        if (TransazioniAggiunte>0)CDC_Grafica.TransazioniCrypto_DaSalvare=true;
        
    }
    
    

        
        
        
    
    
        public String createMD5Hash(final String input)
           throws NoSuchAlgorithmException {

      String hashtext = null;
      MessageDigest md = MessageDigest.getInstance("MD5");

      // Compute message digest of the input
      byte[] messageDigest = md.digest(input.getBytes());

      hashtext = convertToHex(messageDigest);

      return hashtext;
   }

   private String convertToHex(final byte[] messageDigest) {
      BigInteger bigint = new BigInteger(1, messageDigest);
      String hexText = bigint.toString(16);
      while (hexText.length() < 32) {
         hexText = "0".concat(hexText);
      }
      return hexText;
   }
        
        
public static boolean Importa_Crypto_CoinTracking(String fileCoinTracking,boolean SovrascriEsistenti,String Exchange,Component c,boolean PrezzoZero,Download progressb ) {
        

         //   Download progressb=new Download();
  /*                 SwingUtilities.invokeLater(() -> {
           // ProgressBarExample example = new ProgressBarExample();
  Download progressb=new Download();
  progressb.SetMassimo(100);
            // Esempio di aggiornamento della progress bar dalla funzione main
            for (int i = 0; i <= 100; i++) {
                final int progress = i;
                SwingUtilities.invokeLater(() -> progressb.updateProgress(progress));
                try {
                    Thread.sleep(100); // Aggiorna la progress bar ogni 100 millisecondi
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });*/
    
  
 AzzeraContatori();        
        String fileDaImportare = fileCoinTracking;

        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        
         String riga;
         boolean OrdineInverso=false;
         long primaData=0;
         long UltimaData=0;
        // 1- come prima cosa metto in una lista il file
        // e creo una mappa che mi permetterà di analizzare il file ed eliminare le righe doppie
        List<String> lista=new ArrayList<>();
        Map<String, String> Mappa_EliminaDoppioni = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
                while ((riga = bure.readLine()) != null) {
                    //System.out.println(riga);
                    riga=riga.replaceAll("\"", "");//toglie le barre, dovrebbero esistere solo nelle date
                    String splittata[] = riga.split(","); 
                    if (splittata.length==13){
                        String data = OperazioniSuDate.Formatta_Data_CoinTracking(splittata[12]);
                        if (!data.equalsIgnoreCase("")&&Mappa_EliminaDoppioni.get(riga)==null) {
                            Mappa_EliminaDoppioni.put(riga, "");
                            lista.add(riga);
                            if (primaData==0){
                                primaData=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                            }
                            UltimaData=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                       } 
                        }
                }
             //   bure.close();
             //   fire.close();
              } catch (FileNotFoundException ex) {  
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //2 - adesso verifico se l'ordine della lista è decrescente o crescente, quindi se parte dalla data più prossima o da quella più lontana
        //    e ordino la lista nel caso in cui abbia l'ordine inverso ovvero in cui la data più recente sia la prima
        //voglio che come primo record ci sia sempre la dfata più vecchia
        if (primaData>UltimaData) OrdineInverso=true;
        List<String> listaProv=new ArrayList<>();
        if (OrdineInverso){
            for (int i = lista.size(); i-- > 0; ) {
                listaProv.add(lista.get(i));
                //System.out.println(lista.get(i));
                }
            lista=listaProv;
        }
        
        //3 - Adesso scorro la lista e se trovo date identiche incremento di un secondo la data del movimento
        Map<String, String> Mappa_MovimentiTemporanea = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        long DataUltima=0;
        long DataUltimaRiferimento=0;
        for (Object rigas : lista.toArray() ) {
            riga=rigas.toString();
            String splittata[] = riga.split(",");            
            //controllo se la data è uguiale a quella del movimento precedente, così fosse agiungo 1 secondo al movimento
            String data = OperazioniSuDate.Formatta_Data_CoinTracking(splittata[12]);
            long DataAttuale=OperazioniSuDate.ConvertiDatainLongSecondo(data);
            if (DataAttuale==DataUltimaRiferimento){
                DataAttuale=DataUltima+1000;
            }
            else{
                DataUltimaRiferimento=DataAttuale;
            }
            DataUltima=DataAttuale;
            data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(DataAttuale);
            
            //adesso ricreo la riga che andrà sulla mappa
            riga="";
            for(int i=0;i<13;i++){
                if(i==12){
                    riga=riga+data;
                    
                }
                else{
                riga=riga+splittata[i]+",";
                        }
            }
            //metto la data nel keyset in modo da mettere in ordine cronologico qualora ancora non lo fossero, i movimenti
            //System.out.println(riga);
            Mappa_MovimentiTemporanea.put(data+" "+riga, riga);
           // System.out.println(rigas);
        }
    
        
        
        
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //devo prima formattare le date affinche risultino nel formato corretto
        
      //  String riga;
        String ultimaData = "";
        //Iterator<String> iteratore=Mappa_MovimentiTemporanea.keySet().iterator();
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
      progressb.SetMassimo(Mappa_MovimentiTemporanea.size());
        int avanzamento=0;
        for (String str : Mappa_MovimentiTemporanea.keySet()) {
            if (progressb.FineThread()){
            //se è stato interrotta la finestra di progresso interrompo il ciclo
                return false;
                }
         //   System.out.println("aa");
            riga=Mappa_MovimentiTemporanea.get(str);
           // System.out.println(riga);
            String splittata[] = riga.split(",");
            String data=splittata[12];
            //System.out.println(data+" "+ultimaData);

            if (OperazioniSuDate.ConvertiDatainLongSecondo(data) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
            {
               // System.out.println(riga);
                //se trovo movimento con stessa data e ora lo aggiungo alla lista che compone il movimento e vado avanti
                if (data.equalsIgnoreCase(ultimaData)) {
                    listaMovimentidaConsolidare.add(riga);
                    //System.out.println(splittata[12]);
                } else //altrimenti consolido il movimento precedente
                {
                     //System.out.println(listaMovimentidaConsolidare.size());
                    List<String[]> listaConsolidata = ConsolidaMovimenti_CoinTracking(listaMovimentidaConsolidare,Exchange,PrezzoZero);
                    int nElementi = listaConsolidata.size();
                    for (int i = 0; i < nElementi; i++) {
                    String consolidata[] = listaConsolidata.get(i);
                    Mappa_Movimenti.put(consolidata[0], consolidata);
                    }
                    
                    //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                    listaMovimentidaConsolidare = new ArrayList<>();
                    listaMovimentidaConsolidare.add(riga);
                }
                ultimaData = splittata[12];
                
            }
            avanzamento++;
             progressb.SetAvanzamento(avanzamento);
        }
            
            List<String[]> listaConsolidata = ConsolidaMovimenti_CoinTracking(listaMovimentidaConsolidare,Exchange,PrezzoZero);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);             
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }




        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {
               //questa funzione prima di inserire i movimenti nuovi pulisce quelli vecchi e le associazioni
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
              // MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
     //   Prezzi.ScriviFileConversioneXXXEUR();
        if (TransazioniAggiunte>0) 
            CDC_Grafica.TransazioniCrypto_DaSalvare=true;
            
        
        return true;
       
                   
                 
            
        
       
        
    }


public static boolean Importa_Crypto_BinanceTaxReport(String fileBinanceTaxReport,boolean SovrascriEsistenti,Component c,Download progressb ) {
        

   /*
    QUI SOTTO I CAPITESTA DEL FILE FINANCIAL REPORT di BINANCE
    
    0 - ID Movimento
    1 - Data in formato CET es : 2021-01-01-02:00:50
    2 - Tipo generico di movimento (es. Trade, Receive, Send)
    3 - Label (Es. Reward)
    4 - Dettaglio Transazione es. (EARN,SPOT,CRYPTO_DEPOSIT etc....)
    5 - Order Type (Es. LIMIT)
    6 - Qta Uscita
    7 - Moneta Inviata
    8 - Valore in Euro Moneta Inviata
    9 - Indirizzo di Invio
    10 - Qta Ricevuta
    11 - Moneta Ricevuta
    12 - Valore in Euro Monete ricevute
    13 - Indirizzo di ricezione delle Coin
    14 - Qta Fee
    15 - Moneta con cui vengono pagate le fee
    16 - Valore Fee in Euro    
    */
  
   Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //La tipologia di movimento la individuo dall'insieme delle colonne 2,3 e 4
        

        
        Mappa_Conversione_Causali.put("RECEIVE.REWARD.EARN",                    "EARN");//
        Mappa_Conversione_Causali.put("RECEIVE.AIRDROP.EARN",                   "EARN");//
        Mappa_Conversione_Causali.put("RECEIVE.REBATE.SPOT",                    "CASHBACK");//
        Mappa_Conversione_Causali.put("BUY..CONVERT",                           "ACQUISTO CRYPTO");//
        Mappa_Conversione_Causali.put("BUY..SPOT",                              "ACQUISTO CRYPTO");//
        Mappa_Conversione_Causali.put("BUY..OCBS",                              "ACQUISTO CRYPTO");//
        Mappa_Conversione_Causali.put("SELL..SPOT",                             "VENDITA CRYPTO");//
        Mappa_Conversione_Causali.put("SELL..CONVERT",                          "VENDITA CRYPTO");//
        Mappa_Conversione_Causali.put("DEPOSIT..FIAT",                          "DEPOSITO FIAT");//
        Mappa_Conversione_Causali.put("DEPOSIT..OCBS",                          "DEPOSITO FIAT");//
        Mappa_Conversione_Causali.put("TRADE..SPOT",                            "SCAMBIO CRYPTO");//
        Mappa_Conversione_Causali.put("TRADE..CONVERT",                         "SCAMBIO CRYPTO");//
        Mappa_Conversione_Causali.put("TRADE..EARN",                            "SCAMBIO CRYPTO");//
        Mappa_Conversione_Causali.put("RECEIVE.PAYMENT.PAY",                    "TRASFERIMENTO-CRYPTO");//
        Mappa_Conversione_Causali.put("RECEIVE..CRYPTO_DEPOSIT",                "TRASFERIMENTO-CRYPTO");//
        Mappa_Conversione_Causali.put("SEND..CRYPTO_WITHDRAWAL",                "TRASFERIMENTO-CRYPTO");//
   
   
 AzzeraContatori();        
        String fileDaImportare = fileBinanceTaxReport;
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //come prima cosa salvo il file in un array per conoscerne la lunghezza
        //mi servirà poi per gestire la barra di scorrimento
         String riga;
         List<String> file=new ArrayList<>();
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
                while ((riga = bure.readLine()) != null) {
                    file.add(riga);
                }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        }
        int NumeroRighe = file.size();
        progressb.SetMassimo(NumeroRighe);
        for (int i = 0; i < NumeroRighe; i++) {
            progressb.SetAvanzamento(i+1);
            
            riga=file.get(i);
            String splittata[] = riga.split(",",-1); 
                   // System.out.println(splittata.length);
                    if (splittata.length==17){
                        long DataLong=OperazioniSuDate.ConvertiDataBinanceTaxReportinLong(splittata[1]);
                       // System.out.println(DataLong);
                        //Se DataLong è uguale a zero significa che non è una data valida quindi salto la riga che molto probabilmente è l'intestazione
                        if (DataLong != 0) {
                            //Prima di consolidare mi accerto che il movimento sia gestito nella mappa, se non lo è lo aggiungo ai movimenti
                            //da segnalare come errore (questa cosa è da vedere se è meglio farla in fare da analisi del movimento oppure già ora
                            List<String[]> listaConsolidata = ConsolidaMovimenti_BinanceTaxReport(riga, Mappa_Conversione_Causali);
                            int nElementi = listaConsolidata.size();
                            for (int k = 0; k < nElementi; k++) {
                                String consolidata[] = listaConsolidata.get(k);
                                Mappa_Movimenti.put(consolidata[0], consolidata);
                            }
                        }else if (i!=0)
                    {
                        movimentiSconosciuti=movimentiSconosciuti+"FORMATO DATA ERRATO : "+riga+"\n";
                        TrasazioniSconosciute++;
                    }
                    }else
                    {
                        movimentiSconosciuti=movimentiSconosciuti+"LUNGHEZZA RIGA ERRATA : "+riga+"\n";
                        TrasazioniSconosciute++;
                    }
        }






        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {
               //questa funzione prima di inserire i movimenti nuovi pulisce quelli vecchi e le associazioni
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
              // MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
//////////////////////////////////////////////////////       Scrivi_Movimenti_Crypto(MappaCryptoWallet);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
     //   Prezzi.ScriviFileConversioneXXXEUR();
        if (TransazioniAggiunte>0) 
            CDC_Grafica.TransazioniCrypto_DaSalvare=true;
            
        
        return true;
       
                   
                 
            
        
       
        
    }
    
    public static boolean Importa_Crypto_Tatax(String fileTatax,boolean SovrascriEsistenti,String Exchange,Component c,boolean PrezzoZero,Download progressb ) {
       
    
  
        AzzeraContatori();        
        String fileDaImportare = fileTatax;
        
         Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //Faccio una lista di causali per la conversione dei dati del csv
        Mappa_Conversione_Causali.put("CASHBACK", "CASHBACK");  
        Mappa_Conversione_Causali.put("STAKING", "STAKING REWARDS");
        Mappa_Conversione_Causali.put("EARN", "EARN");              
        Mappa_Conversione_Causali.put("AIRDROP", "AIRDROP");            
        Mappa_Conversione_Causali.put("CREDIT", "SCAMBIO CRYPTO-CRYPTO");        
        Mappa_Conversione_Causali.put("DEBIT", "SCAMBIO CRYPTO-CRYPTO");          
        Mappa_Conversione_Causali.put("EXCHANGE_FEE", "COMMISSIONI");          
        Mappa_Conversione_Causali.put("BLOCKCHAIN_FEE", "COMMISSIONI");         
        Mappa_Conversione_Causali.put("FEE", "COMMISSIONI");          
        Mappa_Conversione_Causali.put("FEE", "COMMISSIONI");          
        Mappa_Conversione_Causali.put("FEE", "COMMISSIONI");          
        Mappa_Conversione_Causali.put("FEE", "COMMISSIONI");          
        Mappa_Conversione_Causali.put("DEPOSIT", "TRASFERIMENTO-CRYPTO");//Deposito di Crypto o FIAT provenienti da wallet esterno
        Mappa_Conversione_Causali.put("WITHDRAWAL", "TRASFERIMENTO-CRYPTO");//Prelievo di Crypto o FIAT su wallet esterno
        Mappa_Conversione_Causali.put("CREDIT_FIX", "TRASFERIMENTO-CRYPTO");//Correzione Giacenza
        Mappa_Conversione_Causali.put("DEBIT_FIX", "TRASFERIMENTO-CRYPTO");//Correzione Giacenza

        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        
         String riga;
        // 1- come prima cosa metto in una mappa il file, in questo modo viene anche ordinato
        Map<String, String[]> Mappa_MovimentiTemporanea = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
                while ((riga = bure.readLine()) != null) {
                    String riga2=riga;
                    riga=riga.replaceAll("\"", "");//toglie le barre, dovrebbero esistere solo nelle date
                    String splittata[] = riga.split(",",-1);                     
                    if (splittata.length==11&&Funzioni.Funzioni_isNumeric(splittata[4], false)){
                        // Definisci il formato della data
                       // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String utcDateStr = splittata[2];
                        String Data=OperazioniSuDate.Formatta_Data_UTC(utcDateStr);
                        if (Data==null) {
                            //In questo caso verrà segnalato lo scarto a fine importazione
                            Data="2021-01-01 00:00:00";
                            splittata[3]="FORMATO DATA ERRATO : "+riga2;
                        }

                            String Movimento[]=new String[20];
                            Movimento[0]=Data;//Data
                            Movimento[1]=Exchange;//Wallet Principale
                            Movimento[2]="Principale";//Wallet Secondario                            
                            Movimento[3]=splittata[3];//Tipologia di Movimento
                            Movimento[4]=splittata[3];//Causale Originale
                            Movimento[5]=splittata[0];//Moneta
                            //Dalla moneta tolgo la tipologia di tatax, voglio vedere solo il nome
                            if (Movimento[5].contains(".STAKING@"))Movimento[5]=Movimento[5].split(".STAKING@")[0];
                            if (Movimento[5].contains(".LENDING@"))Movimento[5]=Movimento[5].split(".LENDING@")[0];
                            Movimento[6]=splittata[4];//Qta
                            Movimento[7]=splittata[1];//Address Moneta
                            if (Funzioni.Funzioni_isNumeric(splittata[5], false)&&!splittata[5].equals("0")) {
                                Movimento[8]=new BigDecimal(splittata[5]).toPlainString().replace("-", "");//Valore Originale Euro
                            }
                            else Movimento[8]="";
                        
                            Movimento[9]="";//ID Originale
                            Movimento[10]="";//Rete 
                            
                            //Metto il tutto in una mappa in modo che venga anche già ordinato in base alla data
                            if (Mappa_MovimentiTemporanea.get(Data+" "+riga)==null){
                                Mappa_MovimentiTemporanea.put(Data+" "+riga, Movimento);
                                }
                            else{
                                //se arrivo qua vuol dire che ho trovato un movimento doppio
                                //per cui non devo far altro che sommare le quantità e i prezzi
                                String Mov[]=Mappa_MovimentiTemporanea.get(Data+" "+riga);
                                Movimento[6]=new BigDecimal(Mov[6]).add(new BigDecimal(Movimento[6])).toPlainString();
                                if (!Movimento[8].isBlank()) Movimento[8]=new BigDecimal(Mov[8]).add(new BigDecimal(Movimento[8])).toPlainString();
                                Mappa_MovimentiTemporanea.put(Data+" "+riga, Movimento);
                            }
                            //lista.add(riga);
                        }
                }
            //    bure.close();
            //    fire.close();
              } catch (FileNotFoundException ex) {  
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
       // System.out.println(Mappa_MovimentiTemporanea.size());
        
        
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        
      //  String riga;
        String ultimaData = "";
        //Iterator<String> iteratore=Mappa_MovimentiTemporanea.keySet().iterator();
        List<String[]> listaMovimentidaConsolidare = new ArrayList<>();
      progressb.SetMassimo(Mappa_MovimentiTemporanea.size());
        int avanzamento=0;
        for (String splittata[] : Mappa_MovimentiTemporanea.values()) {
            if (progressb.FineThread()){
            //se è stato interrotta la finestra di progresso interrompo il ciclo
                return false;
                }

            String data=splittata[0];

            if (OperazioniSuDate.ConvertiDatainLongSecondo(data) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
            {
               // System.out.println(riga);
                //se trovo movimento con stessa data e ora lo aggiungo alla lista che compone il movimento e vado avanti
                if (data.equalsIgnoreCase(ultimaData)) {
                    listaMovimentidaConsolidare.add(splittata);
                } else //altrimenti consolido il movimento precedente
                {
                     //System.out.println(listaMovimentidaConsolidare.size());
                    List<String[]> listaConsolidata = ConsolidaMovimentiSingolaRiga(listaMovimentidaConsolidare,Mappa_Conversione_Causali);
                    int nElementi = listaConsolidata.size();
                    for (int i = 0; i < nElementi; i++) {
                        String consolidata[] = listaConsolidata.get(i);
                        Mappa_Movimenti.put(consolidata[0], consolidata);
                    }
                    
                    //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                    listaMovimentidaConsolidare = new ArrayList<>();
                    listaMovimentidaConsolidare.add(splittata);
                }
                ultimaData = splittata[0];
                
            }
            avanzamento++;
             progressb.SetAvanzamento(avanzamento);
        }
            
            List<String[]> listaConsolidata = ConsolidaMovimentiSingolaRiga(listaMovimentidaConsolidare,Mappa_Conversione_Causali);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);             
                Mappa_Movimenti.put(consolidata[0], consolidata);
               // 
            }




        
       int numeromov=0; 
       int numeroscartati=0;
       int numeroaggiunti=0;
       for (String v : Mappa_Movimenti.keySet()) {
           numeromov++;
           if (MappaCryptoWallet.get(v)==null||SovrascriEsistenti)
           {
               //questa funzione prima di inserire i movimenti nuovi pulisce quelli vecchi e le associazioni
               InserisciMovimentosuMappaCryptoWallet(v, Mappa_Movimenti.get(v));
              // MappaCryptoWallet.put(v, Mappa_Movimenti.get(v));
               numeroaggiunti++;
           }else {
            //   System.out.println("Movimento Duplicato " + v);
               numeroscartati++;
           }
       }
     //  System.out.println("TotaleMovimenti="+numeromov);
     //  System.out.println("TotaleScartati="+numeroscartati);
        Transazioni=numeromov;
        TransazioniAggiunte=numeroaggiunti;
        TrasazioniScartate=numeroscartati;
        if (TransazioniAggiunte>0) 
            CDC_Grafica.TransazioniCrypto_DaSalvare=true;
            
        
        return true;
       
                   
                 
            
        
       
        
    }
    
        public static String[] RiempiVuotiArray(String[] array){
            for (int i=0;i<array.length;i++) {
                if(array[i]==null){
                    array[i]="";
                }
            }
            return array;
        }
        
        
    
    
    
        public static void Scrivi_Movimenti_Crypto(Map<String, String[]> Mappa_Movimenti,boolean SalvataggioPermanente) {
        File f = new File("movimenti.crypto.db");
        File f2 = new File("movimenti.crypto.backup");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime now = LocalDateTime.now();
            String DataOra=now.format(formatter);
        if (SalvataggioPermanente)f2 = new File("Backup/movimenti.crypto.backup."+DataOra);
    if(f.exists()){
        if(f2.exists())f2.delete();
        f.renameTo(f2);
    }
         try { 
            FileWriter w=new FileWriter("movimenti.crypto.db");
            BufferedWriter b=new BufferedWriter (w);
       for (String[] v : Mappa_Movimenti.values()) {
           String riga="";
                for (String v1 : v) {
                    if(v1==null)v1="";
                    riga = riga + v1 + ";";
                }
           //Questa serve per togliere l'ultimo ";" dalla stringa in quanto superfluo
           riga=riga.substring(0,riga.length()-1);
          // System.out.println(riga);
           b.write(riga+"\n");

       }
       b.close();
       w.close();
    }catch (IOException ex) {
              //     Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
    }
        
        
  /*  private static void Leggi_Movimenti_Crypto() {///da finireeeeeeeeeeeeeeeee
        try {
            FileReader w = new FileReader("movimenti.crypto.csv");
            BufferedReader bure = new BufferedReader(w);
            String riga;
            while ((riga = bure.readLine()) != null) {
                String splittata[] = riga.split(";");
            }
            bure.close();
            w.close();
        } catch (IOException ex) {
            //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
        
        
         public static List<String[]> ConsolidaMovimenti_CDCAPP(List<String> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
         String dust_accreditati="";
         String dust_addebitati[]=new String[1];
         if (numMovimenti>1) {dust_addebitati=new String[numMovimenti-1];}
         String dust_sommaaddebiti="0";
         int numeroAddebiti=0;
                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                            String data=movimentoSplittato[0];
                            data=OperazioniSuDate.Formatta_Data_UTC(data);
                            String dataa="";
                            String movimentoConvertito=Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                            if (data==null) {
                                movimentoConvertito=null;
                                movimento="FORMATO DATA ERRATO : "+movimento;
                            }
                            else dataa=data.trim().substring(0, data.length()-3);
                             //   String inputValue = "2012-08-15T22:56:02.038Z";

                            
                           // System.out.println(movimentoSplittato[9]);
                           if (movimentoConvertito==null)
                                {
                                //   System.out.println("Errore in importazione da CDCAPP csv: "+movimento);
                                   movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                   TrasazioniSconosciute++;
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD"))
                            {

                                //System.out.println(movimentoSplittato[0].replaceAll(" |-|:", ""));
                                RT[0] = data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_RW";
           // System.out.println(RT[0]);
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = "Crypto.com App";
                                RT[4] = "Crypto Wallet";
                                RT[5] = Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[9] + "(" + movimentoSplittato[1] + ")";
                                String valoreEuro = "0";
                                    
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) {
                                        valoreEuro = movimentoSplittato[7];
                                    }
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD")) {
                                        valoreEuro = Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);                                        
                                    }
                                    
                                    valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).toString();
                                
                                if (movimentoSplittato[3].contains("-")) {
                                    RT[5] = "RIMBORSO " + Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                    RT[6] = movimentoSplittato[2]+" ->"; 
                                    RT[8] = movimentoSplittato[2];
                                    RT[9] = "Crypto";
                                    RT[10] = movimentoSplittato[3];
                                    RT[11] = "";
                                    RT[12] = "";
                                    RT[13] = "";
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                                    RT[15] = "0.00";
                                    RT[16] = "";
                                    RT[17] = "Da Calcolare";//verrà calcolato con il metodo lifo
                                    RT[18] = "";//verrà calcolato con il metodo lifo
                                    RT[19] = "Da Calcolare";//verrà calcolato con il metodo lifo sarà (0 - prezzo di carico)
                                } 
                                else 
                                {
                                    RT[8] = "";
                                    RT[9] = "";
                                    RT[10] = "";
                                    RT[11] = movimentoSplittato[2];
                                    RT[12] = "Crypto";
                                    RT[13] = movimentoSplittato[3];
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                             /*       String valoreEuro = "0";
                                    
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) {
                                        valoreEuro = movimentoSplittato[7];
                                    }
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD")) {
                                        valoreEuro = Calcoli.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);                                        
                                    }
                                    //System.out.println("-"+valoreEuro+"-");
                                    //valoreEuro=new BigDecimal(1).toString();
                                 //   if (valoreEuro!=null){
                                    valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).toString();*/

                                    RT[15] = valoreEuro;
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[3]);
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                 //   RT[16] = "";
                                    RT[17] = valoreEuro;
                                 //   RT[18] = "";
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                               /* RT[20] = "";
                                RT[21] = "";*/
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO"))
                            {
                                //trasferimento FIAT                              
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_DF";
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";                                
                                RT[5]="DEPOSITO FIAT";
                                String valoreEuro="0";
                                if (movimentoSplittato[9].trim().equalsIgnoreCase("viban_purchase")){
                                    if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                    if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[3], data.split(" ")[0]);
                                    }
                                    valoreEuro=new BigDecimal(valoreEuro).abs().toString(); 
                                    
                                    RT[6]="-> "+movimentoSplittato[2]; 
                                    RT[11]=movimentoSplittato[2]; 
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                    RT[14]=movimentoSplittato[2]+" "+movimentoSplittato[3];
                                }
                                else 
                                    {
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                    valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                    
                                    RT[6]="-> EUR";
                                    RT[11]="EUR";
                                    RT[13]=valoreEuro;
                                    RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                    
                                    }
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                                                   
                                RT[12]="FIAT";
                                RT[15]=valoreEuro;
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[22]="A";                              
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                                //Vendita Euro x Crypto
                                //movimentoSplittato[0].replaceAll(" |-|:", "") +"_"+String.valueOf(k+1)+ "_CDCAPP_AC_*_"+movimentoSplittato[2].trim();
                                RT=new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_AC";                              
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="ACQUISTO CRYPTO";
                                //recurring_buy_order e viban_purcase vanno gestite diversamente
                                if (movimentoSplittato[9].trim().equalsIgnoreCase("viban_purchase")||
                                        movimentoSplittato[9].trim().equalsIgnoreCase("recurring_buy_order")||
                                        movimentoSplittato[9].trim().equalsIgnoreCase("trading.limit_order.fiat_wallet.purchase_commit"))
                                {
                                    RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];
                                    RT[8]=movimentoSplittato[2];
                                    RT[10]="-"+(new BigDecimal(movimentoSplittato[3]).abs().toString());
                                    RT[11]=movimentoSplittato[4];
                                    RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();
                                    
                                }
                                else
                                {
                                    RT[6]="EUR -> "+movimentoSplittato[2];
                                    RT[8]="EUR";
                                    RT[10]="-"+valoreEuro;
                                    RT[11]=movimentoSplittato[2];
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                    RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                }
                                                              
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                               
                                RT[9]="FIAT";                                                                                              
                                RT[12]="Crypto";
                                valoreEuro=new BigDecimal(valoreEuro).abs().setScale(2, RoundingMode.HALF_UP).toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("VENDITA CRYPTO"))
                            {
                                //Vendita Crypto x Servizio
                                if (movimentoSplittato[9].equalsIgnoreCase("crypto_payment")||movimentoSplittato[9].equalsIgnoreCase("card_top_up")){
                                //if (movimentoSplittato[9].equalsIgnoreCase("crypto_payment")){   
                                RT=new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_VC"; 
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="VENDITA CRYPTO";
                                if (movimentoSplittato[9].equalsIgnoreCase("crypto_payment"))RT[6]=movimentoSplittato[2]+" -> (Acquisto con Crypto)";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                               
                                else if (movimentoSplittato[9].equalsIgnoreCase("card_top_up"))RT[6]=movimentoSplittato[2]+" -> (Top Up Carta)";
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];///////
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                else if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                else
                                    {
                                        valoreEuro=Prezzi.ConvertiXXXEUR(movimentoSplittato[6],movimentoSplittato[7], OperazioniSuDate.ConvertiDatainLongMinuto(data));
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);   
                                }
                                else
                                {//Vendita Crypto x Euro
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_VC"; 
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="VENDITA CRYPTO";
                                RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="FIAT";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];///////
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);   
                                
                                
                                //trasferimento FIAT
                                RT=new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_PF"; 
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="PRELIEVO FIAT";
                                RT[6]=movimentoSplittato[4]+" ->";
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[4];
                                RT[9]="FIAT";
                                RT[10]="-"+new BigDecimal(movimentoSplittato[5]).abs().toString();
                                RT[11]="";                                                               
                                RT[12]=""; 
                                RT[13]=""; 
                                RT[14]=movimentoSplittato[4]+" "+movimentoSplittato[5];///////
                                valoreEuro="";
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[5];
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[5], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();                                
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                }
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO-CRYPTO"))
                            {
                                //Scambio Crypto Crypto
                                
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_SC";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                                RT[5]="SCAMBIO CRYPTO";
                                RT[6]=movimentoSplittato[2]+" -> "+movimentoSplittato[4];//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];///////
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().setScale(2, RoundingMode.HALF_UP).toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);     
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DUST-CONVERSION"))
                            {
                                // serve solo per il calcolo della percentuale di cro da attivare

                                    // se è un movimento negativo lo inserisco tra gli addebiti
                                    if (movimentoSplittato[3].contains("-")){
                                        dust_addebitati[numeroAddebiti]=movimento;
                                        dust_sommaaddebiti=new BigDecimal(dust_sommaaddebiti).abs().add(new BigDecimal(movimentoSplittato[7])).abs().toString();
                                       // System.out.println(dust_sommaaddebiti+ " "+movimentoSplittato[7]);
                                        numeroAddebiti++;
                                       // System.out.println("ADDEBITI : "+movimento);
                                    }
                                    else
                                    {
                                       //System.out.println("ACCREDITI : "+movimento);
                                       dust_accreditati=movimento;
                                    }   
                                
                           // se è l'ultimo movimento allora creo anche le righe
                                if (k==numMovimenti-1){
                                  // System.out.println(movimento);
                                 //  System.out.println(numeroAddebiti);
                                    for (int w = 0; w < numeroAddebiti; w++) {
                                        String splittata[]=dust_addebitati[w].split(",");
                                        RT = new String[ColonneTabella];
                                        RT[0] = data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_"+String.valueOf(w+1)+"_SC";
                                        RT[1] = dataa;
                                        RT[2] = w+1 + " di " + numeroAddebiti;
                                        RT[3] = "Crypto.com App";
                                        RT[4] = "Crypto Wallet";
                                        RT[5] = "SCAMBIO CRYPTO";
                                        //System.out.println(splittata[0]);
                                       /* System.out.println(splittata.length);
                                        System.out.println(splittata[2]);
                                        System.out.println(dust_accreditati.length());
                                        System.out.println(dust_accreditati);
                                        System.out.println(numeroAddebiti);
                                        System.out.println(dataa);
                                        System.out.println("-------");*/
                                      /* if (dust_accreditati.split(",").length<3){
                                           System.out.println("ERRORE - "+dust_accreditati);
                                           System.out.println("ERRORE - "+splittata[0]);
                                       }*/
                                        RT[6] = splittata[2]+" -> "+dust_accreditati.split(",")[2];//da sistemare con ulteriore dettaglio specificando le monete trattate                                        
                                        RT[7] = splittata[9] + "(" + splittata[1] + ")";
                                        RT[8] = splittata[2];
                                        RT[9] = "Crypto";
                                        RT[10] = splittata[3];
                                        RT[11] = dust_accreditati.split(",")[2];
                                        RT[12] = "Crypto";
                                        BigDecimal valoreTrans=new BigDecimal(splittata[7]);
                                        BigDecimal sumAddebiti;
                                        //System.out.println(dust_sommaaddebiti);
                                        if (new BigDecimal(dust_sommaaddebiti).compareTo(new BigDecimal("0"))!=0){
                                            sumAddebiti=new BigDecimal(dust_sommaaddebiti);
                                        }else
                                            {
                                               sumAddebiti=new BigDecimal("0.000000001"); 
                                            }
                                        BigDecimal totCRO=new BigDecimal(dust_accreditati.split(",")[3]);
                                        BigDecimal operazione;                                        
                                        operazione=(valoreTrans.divide(sumAddebiti,30, RoundingMode.HALF_UP));
                                        String numCRO=operazione.multiply(totCRO).stripTrailingZeros().abs().toString();//da sistemare calcolo errato
                                        RT[13] =numCRO;//dust_accreditati.split(",")[3];//bisogna fare i calcoli
                                        RT[14] = splittata[6] + " " + splittata[7];///////
                                        String valoreEuro = "";
                                        if (splittata[6].trim().equalsIgnoreCase("EUR")) {
                                            valoreEuro = splittata[7];
                                        }
                                        if (splittata[6].trim().equalsIgnoreCase("USD")) {
                                            valoreEuro = Prezzi.ConvertiUSDEUR(splittata[7], splittata[0].split(" ")[0]);
                                        }
                                        valoreEuro = new BigDecimal(valoreEuro).abs().setScale(2, RoundingMode.HALF_UP).toString();
                                        RT[15] = valoreEuro;
                                        RT[16] = "";
                                        RT[17] = "Da calcolare";
                                        RT[18] = "";
                                        RT[19] = "0.00";
                                        RT[20] = "";
                                        RT[21] = "";
                                        RT[22] = "A";
                                        RiempiVuotiArray(RT);
                                        lista.add(RT);
                                    }
                                }
                                }
                                else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO-INTERNO"))
                            {
                               
                                //come prima cosa devo individuare il portafoglio nel quale vanno i token
                                String WalletPartenza="";
                                String WalletDestinazione="";
                                if (movimentoSplittato[9].toLowerCase().contains("supercharger"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Supercharger";
                                    }
                                    else
                                    {
                                        WalletPartenza="Supercharger";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }else if (movimentoSplittato[9].toLowerCase().contains("staking"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Staking";
                                    }
                                    else
                                    {
                                        WalletPartenza="Staking";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }else if (movimentoSplittato[9].toLowerCase().contains("earn"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Earn";
                                    }
                                    else
                                    {
                                        WalletPartenza="Earn";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }else if (movimentoSplittato[9].toLowerCase().contains("lock"))
                                {
                                    if (movimentoSplittato[3].contains("-"))
                                    {
                                        WalletPartenza="Crypto Wallet";
                                        WalletDestinazione="Fondi Bloccati";
                                    }
                                    else
                                    {
                                        WalletPartenza="Fondi Bloccati";
                                        WalletDestinazione="Crypto Wallet";                                        
                                    }
                                }
                                else {
                                    System.out.println(movimento);
                                }
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]=WalletPartenza;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[6]=movimentoSplittato[2]+" -> ";                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                if (movimentoSplittato[3].contains("-")) RT[10]=movimentoSplittato[3]; else RT[10]="-"+movimentoSplittato[3];                                 
                                RT[11]="";
                                RT[12]="";
                                RT[13]="";                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                                
                                
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_2_TI";
                                RT[1]=dataa;
                                RT[2]=2+" di "+2;
                                RT[3]="Crypto.com App";
                                RT[4]=WalletDestinazione;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[6]=" -> "+movimentoSplittato[2];                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]="";
                                RT[9]="";
                                RT[10]="";                                 
                                RT[11]=movimentoSplittato[2];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="0.00";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                                
                                
                                
                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO"))
                            {
                                RT = new String[ColonneTabella];
                            //    RT[0]=movimentoSplittato[0].replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_TC";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Crypto.com App";
                                RT[4]="Crypto Wallet";
                             //   RT[5]="TRASFERIMENTO CRYPTO";
                            //    RT[6]="TRASFERIMENTO CRYPTO";                                
                                
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                if (movimentoSplittato[3].contains("-")) {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_PC";
                                    RT[5]="PRELIEVO CRYPTO";
                                    RT[6]=movimentoSplittato[2]+" ->";
                                    RT[8]=movimentoSplittato[2];
                                    RT[9]="Crypto";
                                    RT[10]=movimentoSplittato[3];
                                    RT[11]="";
                                    RT[12]="";
                                    RT[13]="";
                                } else {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_CDCAPP_"+String.valueOf(k+1)+ "_1_DC";
                                    RT[5]="DEPOSITO CRYPTO";
                                    RT[6]="-> "+movimentoSplittato[2];
                                    RT[8]="";
                                    RT[9]="";
                                    RT[10]="";
                                    RT[11]=movimentoSplittato[2];
                                    RT[12]="Crypto";
                                    RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();                               
                                }                                                                                            
                                RT[14]=movimentoSplittato[6]+" "+movimentoSplittato[7];
                                String valoreEuro="";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[7];
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Prezzi.ConvertiUSDEUR(movimentoSplittato[7], data.split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                if (movimentoSplittato.length>10) RT[21]="Trans ID : "+movimentoSplittato[10];else RT[21]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("IGNORA"))
                            {
                                System.out.println("Movimento ignorato : "+movimento);
                            }
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                        TrasazioniSconosciute++;
                                    }

                           
                        }
        return lista;
    }      
        
        
     public static List<String[]> ConsolidaMovimenti_BinanceTaxReport(String movimento,Map<String, String> Mappa_Conversione_Causali){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
         List<String[]> lista=new ArrayList<>();
                            String movimentoSplittato[]=movimento.split(",",-1);
                            String RT[]=new String[ColonneTabella];
                            String TipoMovimento=movimentoSplittato[2].toUpperCase()+"."+movimentoSplittato[3].toUpperCase()+"."+movimentoSplittato[4].toUpperCase();
                            String movimentoConvertito=Mappa_Conversione_Causali.get(TipoMovimento);
                            
                            String idBinance=movimentoSplittato[0].replace("_", "");
                            long DataLong=OperazioniSuDate.ConvertiDataBinanceTaxReportinLong(movimentoSplittato[1]);
                            String data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(DataLong);
                            String dataa=data.trim().substring(0, data.length()-3);
                            if (movimentoConvertito==null){
                                movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                TrasazioniSconosciute++;
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("CASHBACK"))
                            {
                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance."+idBinance+ "_1_1_RW";
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";
                                RT[5] = Mappa_Conversione_Causali.get(TipoMovimento);
                                RT[6] = "-> "+movimentoSplittato[11];                                
                                RT[7] = TipoMovimento;
                                String valoreEuro;
                                RT[11] = movimentoSplittato[11];
                                String TipoCrypto;
                                if (RT[11].equals("EUR"))
                                {
                                    TipoCrypto="FIAT";
                                    valoreEuro=new BigDecimal(movimentoSplittato[10]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                }
                                else{                                     
                                    TipoCrypto="Crypto";
                                    valoreEuro=new BigDecimal(movimentoSplittato[12]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                }
                                RT[12] = TipoCrypto;
                                RT[13] = movimentoSplittato[10];
                                RT[14] = "€ "+movimentoSplittato[12];
                                RT[15] = valoreEuro;
                                RT[17] = valoreEuro;
                                RT[19] = valoreEuro;
                                RT[22] = "A";
                                RT[32] = "SI";
                                RT[36] = movimentoSplittato[9];
                                RT[37] = movimentoSplittato[13];
                                RiempiVuotiArray(RT);
                                //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                                lista.add(RT);
                                
                                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DEPOSITO FIAT"))
                            {
                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance."+idBinance+ "_1_1_DF";
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";
                                BigDecimal TotRicevuto=new BigDecimal(movimentoSplittato[10]);
                                BigDecimal ValoreTot=new BigDecimal(movimentoSplittato[12]);
                                
                                //imposto il valore della transazione come da file con la differenza che se sono euro
                                //il valore è il quantitativo in euro
                                //se non sono euro prendo invece il controvalore che mi da binance
                                String valoreEuro;                               
                                //Se la moneta delle fee è uguale a quella ricevuta sommo i valori
                                if(movimentoSplittato[11].equals(movimentoSplittato[15])){
                                    //Se la moneta delle fee è uguale a quella ricevuta allora devo sommare le fee alla moneta ricevuta per trovare il totale
                                    TotRicevuto=TotRicevuto.add(new BigDecimal(movimentoSplittato[14])); 
                                    ValoreTot=ValoreTot.add(new BigDecimal(movimentoSplittato[16]));
                                }
                                if (movimentoSplittato[11].equalsIgnoreCase("EUR")){
                                    valoreEuro=TotRicevuto.setScale(2, RoundingMode.HALF_UP).abs().toString();
                                    } 
                                else {
                                    valoreEuro=ValoreTot.setScale(2, RoundingMode.HALF_UP).abs().toString();
                                }
                                RT[5] = Mappa_Conversione_Causali.get(TipoMovimento);
                                RT[6] = "-> "+movimentoSplittato[11];                                
                                RT[7] = TipoMovimento;
                                if (!movimentoSplittato[11].equalsIgnoreCase("EUR")){
                                    valoreEuro=new BigDecimal(movimentoSplittato[12]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                }
                                //String valoreEuro=new BigDecimal(TotRicevuto).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[11] = movimentoSplittato[11];
                                RT[12] = "FIAT";
                                RT[13] = TotRicevuto.toPlainString();
                                RT[14] = "€ "+movimentoSplittato[12];
                                RT[15] = valoreEuro;
                                RT[17] = valoreEuro;
                                RT[22] = "A";
                                RT[32] = "SI";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO"))
                            {                               
                                //Scambio Crypto Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance."+idBinance+ "_1_1_SC";
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";
                                RT[5] = Mappa_Conversione_Causali.get(TipoMovimento);
                                RT[6] = movimentoSplittato[7]+" -> "+movimentoSplittato[11];                                
                                RT[7] = TipoMovimento;
                                String valoreEuro=new BigDecimal(movimentoSplittato[12]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[8] = movimentoSplittato[7];
                                RT[9] = "Crypto";
                                RT[10] = movimentoSplittato[6];
                                if (!RT[10].contains("-"))RT[10]="-"+RT[10];
                                RT[11] = movimentoSplittato[11];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[10];
                                RT[14] = "€ "+movimentoSplittato[12];
                                RT[15] = valoreEuro;
                                RT[17] = valoreEuro;
                                RT[19] = valoreEuro;
                                RT[22] = "A";
                                RT[32] = "SI";
                                RT[36] = movimentoSplittato[9];
                                RT[37] = movimentoSplittato[13];
                                
                                RiempiVuotiArray(RT);
                                //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                                lista.add(RT);    

                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO"))
                            {                               
                                //Scambio Crypto Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance."+idBinance+ "_1_1_AC";
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";
                                RT[5] = Mappa_Conversione_Causali.get(TipoMovimento);
                                RT[6] = movimentoSplittato[7]+" -> "+movimentoSplittato[11];                                
                                RT[7] = TipoMovimento;                               
                                RT[8] = movimentoSplittato[7];
                                RT[9] = "FIAT";
                                RT[10] = movimentoSplittato[6];
                                if (!RT[10].contains("-"))RT[10]="-"+RT[10];
                                RT[11] = movimentoSplittato[11];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[10];
                                RT[14] = "€ "+movimentoSplittato[12];
                                String valoreEuro;
                                if (movimentoSplittato[11].equals("EUR"))
                                    valoreEuro=new BigDecimal(RT[10]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                else
                                    valoreEuro=new BigDecimal(movimentoSplittato[8]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15] = valoreEuro;
                                RT[17] = valoreEuro;
                                RT[19] = valoreEuro;
                                RT[22] = "A";
                                RT[32] = "SI";
                                RT[36] = movimentoSplittato[9];
                                RT[37] = movimentoSplittato[13];
                                
                                RiempiVuotiArray(RT);
                                //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                                lista.add(RT);    

                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("VENDITA CRYPTO"))
                            {                               
                                //Scambio Crypto Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance."+idBinance+ "_1_1_VC";
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";
                                RT[5] = Mappa_Conversione_Causali.get(TipoMovimento);
                                RT[6] = movimentoSplittato[7]+" -> "+movimentoSplittato[11];                                
                                RT[7] = TipoMovimento;                               
                                RT[8] = movimentoSplittato[7];
                                RT[9] = "Crypto";
                                RT[10] = movimentoSplittato[6];
                                if (!RT[10].contains("-"))RT[10]="-"+RT[10];
                                RT[11] = movimentoSplittato[11];
                                RT[12] = "FIAT";
                                RT[13] = movimentoSplittato[10];
                                RT[14] = "€ "+movimentoSplittato[12];
                                String valoreEuro;
                                if (movimentoSplittato[11].equals("EUR"))
                                    valoreEuro=new BigDecimal(RT[13]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                else
                                    valoreEuro=new BigDecimal(movimentoSplittato[12]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                RT[15] = valoreEuro;
                                RT[17] = valoreEuro;
                                RT[19] = valoreEuro;
                                RT[22] = "A";
                                RT[32] = "SI";
                                RT[36] = movimentoSplittato[9];
                                RT[37] = movimentoSplittato[13];
                                
                                RiempiVuotiArray(RT);
                                //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                                lista.add(RT);    

                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO"))
                            {
                                RT[1] = dataa;
                                RT[2] = 1 + " di " + 1;
                                RT[3] = "Binance";
                                RT[4] = "Principale";                                                          
                                RT[7]=TipoMovimento;
                                String valoreEuro;
                                if (TipoMovimento.contains("SEND.")) {
                                    RT[0] = data.replaceAll(" |-|:", "") + "_Binance." + idBinance + "_1_1_PC";
                                    RT[5] = "PRELIEVO CRYPTO";
                                    RT[6] = movimentoSplittato[7] + " ->";
                                    RT[8] = movimentoSplittato[7];
                                    RT[9] = "Crypto";
                                    RT[10] = movimentoSplittato[6];
                                    if (!RT[10].contains("-"))RT[10]="-"+RT[10];
                                    RT[14] = "€ " + movimentoSplittato[8];
                                    valoreEuro=new BigDecimal(movimentoSplittato[8]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                } else {
                                    RT[0] = data.replaceAll(" |-|:", "") + "_Binance." + idBinance + "_1_1_DC";
                                    RT[5] = "DEPOSITO CRYPTO";
                                    RT[6] = "-> " + movimentoSplittato[11];
                                    RT[11] = movimentoSplittato[11];
                                    RT[12] = "Crypto";
                                    RT[13] = movimentoSplittato[10];
                                    RT[14] = "€ " + movimentoSplittato[12];
                                    valoreEuro=new BigDecimal(movimentoSplittato[12]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                                }                                                                                          
                                RT[15]=valoreEuro;
                                RT[22]="A";
                                RT[32] = "SI";
                                RT[36] = movimentoSplittato[9];
                                RT[37] = movimentoSplittato[13];
                                RiempiVuotiArray(RT);
                                //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                                lista.add(RT); 
                                
                                                                
;
                                
                            }

                           //Per ultimo aggiungo i movimenti di fee se questo non sono zero
                           if (!movimentoSplittato[15].isBlank()){
                               RT = new String[ColonneTabella];
                               RT[0] = data.replaceAll(" |-|:", "") + "_Binance." + idBinance + "_1_2_CM";                              
                               RT[1] = dataa;
                               RT[2] = 1 + " di " + 1;
                               RT[3] = "Binance";
                               RT[4] = "Principale";
                               RT[5] = "COMMISSIONI";
                               RT[6] = movimentoSplittato[15] + " ->";
                               RT[7] = TipoMovimento;
                               RT[8] = movimentoSplittato[15];
                               String valoreEuro = new BigDecimal(movimentoSplittato[16]).setScale(2, RoundingMode.HALF_UP).abs().toString();
                               Set<String> valute = Set.of("EUR", "USD", "TRY");
                                if (valute.contains(movimentoSplittato[15])) {
                                    
                                    RT[9] = "FIAT";
                               }
                               else{
                                   RT[9] = "Crypto";                              
                               }
                               RT[10] = movimentoSplittato[14];
                               if (!RT[10].contains("-"))RT[10]="-"+RT[10];
                               RT[14] = "€ " + movimentoSplittato[16];
                               RT[15] = valoreEuro;
                               RT[22] = "A";
                               RT[32] = "SI";
                               RiempiVuotiArray(RT);
                               //Prezzi.DammiPrezzoDaTransazione(RT, 4);
                               lista.add(RT);

                           }
                           
                        
        return lista;
    }   
  
        public static void ConsolidaMovimentiDifferiti(List<String[]> listaMovimentidaConsolidare,boolean SovrascrivoEsistenti){
            Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            int nElementi = listaMovimentidaConsolidare.size();
            //Con questo ordino i movimenti
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaMovimentidaConsolidare.get(i); 
                if (SovrascrivoEsistenti||MappaCryptoWallet.get(consolidata[0])==null){ 
                    //Aggiungo alla mappa da verificare solo i movimenti nuovi o se è attiva la spunta di sovrascrivere i movimenti esistenti
                    Mappa_Movimenti.put(consolidata[0], consolidata);
                }
            }
            
            for(String[] riga:Mappa_Movimenti.values()){
                //Se trovo un prelievo devo vedere se nei 10 minuti successivi c'è stato un deposito e lo associo
               // System.out.println(riga[5]);
                if (riga[5].contains("PRELIEVO")){
                    //leggo l'ora
                    long timestampPrelievo=OperazioniSuDate.ConvertiDatainLongMinuto(riga[1]);
                    // una volta letto l'ora vado a vedere se nei 15 minuti successivi c'è un deposito, in quel caso
                    // lo associo a questo movimenti di prelievo solo se il prezzo tra le due monete non si discosta di più del 10%
                    // oppure se non riesco a trovare il prezzo
                    // altrimenti lascio tutto com'è
                    for(String[] rigaConfronto:Mappa_Movimenti.values()){
                        if (rigaConfronto[5].contains("DEPOSITO")){                            
                            BigDecimal PrezzoPrelievo=new BigDecimal(riga[15]);
                            BigDecimal PrezzoDeposito=new BigDecimal(rigaConfronto[15]);
                            BigDecimal Diecipercento=PrezzoPrelievo.divide(new BigDecimal(10));
                            long timestampDeposito=OperazioniSuDate.ConvertiDatainLongMinuto(rigaConfronto[1]);
                          /*  System.out.println(PrezzoPrelievo);
                           // System.out.println(PrezzoDeposito);
                          //  System.out.println(Diecipercento);
                           // System.out.println(timestampPrelievo);
                           // System.out.println(timestampDeposito);*/
                            //Se il tempo intercorso tra deposito e prelievo è inferiore di 15 minuti controllo il prezzo
                            if (timestampDeposito-timestampPrelievo>0 && timestampDeposito-timestampPrelievo<900000&&
                                    PrezzoPrelievo.subtract(PrezzoDeposito).abs().compareTo(Diecipercento)==-1){
                                //Se la differenza tra il prezzo di prelievo e deposito è inferiore al 10% allora proseguo
                                //e associo i movimenti e poi termino il ciclo
                                    ClassificazioneTrasf_Modifica.CreaMovimentiScambioCryptoDifferito(riga[0], rigaConfronto[0]);
                                    
                                                               
                            }
                        }
                    }
                    
                }
            }
                   
        }
     
        
          public static List<String[]> ConsolidaMovimenti_Binance(List<String> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali,List<String[]> listaAutoinvest){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
         String dataa="";
         long Datalong=0;
         String data="";
         TransazioneDefi Scambio=new TransazioneDefi();         
                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                            data=movimentoSplittato[1];
                            data=OperazioniSuDate.Formatta_Data_UTC(data);
                            Moneta Mon=new Moneta();
                            Mon.Moneta=movimentoSplittato[4];
                            Mon.Qta=new BigDecimal(movimentoSplittato[5]).toPlainString();
                            Datalong=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                            
                            if (Mon.Moneta.equalsIgnoreCase("EUR")||Mon.Moneta.equalsIgnoreCase("USD"))
                                Mon.Tipo="FIAT";
                            else
                                Mon.Tipo="Crypto";
                           
                            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, Datalong, null, true, 10, null);
                            String valoreEuro = new BigDecimal(Mon.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                           // String WalletSecondario=movimentoSplittato[2];
                           // WalletSecondario="Principale";
                            String CausaleOriginale=movimentoSplittato[3];
                              
                                                  
                            String movimentoConvertito=Mappa_Conversione_Causali.get(movimentoSplittato[3]);
                            if (data==null) {
                                movimentoConvertito=null;
                                movimento="FORMATO DATA ERRATO : "+movimento;
                            }
                            else dataa=data.trim().substring(0, data.length()-3);
                           
                           if (movimentoConvertito==null)
                                {
                                //   System.out.println("Errore in importazione da CDCAPP csv: "+movimento);
                                   movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                   TrasazioniSconosciute++;
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING REWARDS")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD"))
                            {

                                RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = "Binance";
                                //RT[4] = WalletSecondario;
                                RT[4] = "Principale";
                                RT[5] = movimentoConvertito;
                                RT[6] = "-> "+Mon.Moneta;                                
                                RT[7] = CausaleOriginale;
                                if (Mon.Qta.contains("-")) {
                                    RT[5] = "RIMBORSO " + movimentoConvertito;
                                    RT[6] = Mon.Moneta+" ->"; 
                                    RT[8] = Mon.Moneta;
                                    RT[9] = Mon.Tipo;
                                    RT[10] = Mon.Qta;
                                    RT[15] = "0.00";
                                    RT[17] = "Da Calcolare";//verrà calcolato con il metodo lifo
                                    RT[19] = "Da Calcolare";//verrà calcolato con il metodo lifo sarà (0 - prezzo di carico)
                                } 
                                else 
                                {

                                    RT[11] = Mon.Moneta;
                                    RT[12] = Mon.Tipo;
                                    RT[13] = Mon.Qta;
                                    RT[14] = "";

                                    RT[15] = valoreEuro;
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[5]);
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                    RT[17] = valoreEuro;
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("DEPOSITO FIAT")||
                                   (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&Mon.Tipo.equals("FIAT")&&!Mon.Qta.contains("-"))
                                   )
                            {
                                                                //trasferimento FIAT                              
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_DF";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                               // RT[4]=WalletSecondario;
                                RT[4] = "Principale";
                                RT[5]="DEPOSITO FIAT";
                                RT[6]="-> "+Mon.Moneta;
                                RT[7]=CausaleOriginale;
                                RT[11]=Mon.Moneta;
                                RT[12]=Mon.Tipo;
                                RT[13]=Mon.Qta;
                                RT[15]=valoreEuro;
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[22]="A";                              
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("PRELIEVO FIAT"))
                            {
                                //trasferimento FIAT
                                String Codice;
                                String Descrizione;
                                if (Mon.Qta.contains("-")) {
                                    Codice=RitornaTipologiaTransazione(Mon.Tipo,null,0);
                                    Descrizione=RitornaTipologiaTransazione(Mon.Tipo,null,1);
                                   } 
                                else {
                                    Codice=RitornaTipologiaTransazione(null,Mon.Tipo,0);
                                    Descrizione=RitornaTipologiaTransazione(null,Mon.Tipo,1);
                                }
                                    
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_"+Codice;
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                //RT[4]=WalletSecondario;
                                RT[4] = "Principale";                                
                                RT[5]=Descrizione;
                                RT[7]=CausaleOriginale; 
                                if (Mon.Qta.contains("-")) {
                                if (CausaleOriginale.trim().equalsIgnoreCase("Fiat Withdraw"))RT[5]="PRELIEVO FIAT";
                                else RT[5]="SPESA CON CARTA";
                                RT[6]=Mon.Moneta+"-> ";
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;
                                }
                                else{
                                RT[5]="RIMBORSO SU CARTA";
                                RT[6]="-> "+Mon.Moneta;
                                RT[11]=Mon.Moneta;
                                RT[12]=Mon.Tipo;
                                RT[13]=Mon.Qta;    
                                }
                                RT[15]=valoreEuro;
                                RT[19]="0.00";
                                RT[22]="A";                              
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("VENDITA CRYPTO"))
                            {
                                //Vendita Crypto, ad esempio per pagamento tasse
                                String Codice;
                                String Descrizione;  
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_VC";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                RT[4] = "Principale";                                
                                RT[5]="VENDITA CRYPTO";
                                RT[7]=CausaleOriginale; 
                                RT[6]=Mon.Moneta+"-> ";
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;                          
                                RT[15]=valoreEuro;
                                RT[19]="0.00";
                                RT[22]="A";                              
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("COMMISSIONI"))
                            {
                                //Scambio Crypto Crypto
                                //il C dopo binance mi serve per far si che le commissioni le metta per ultime
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_C"+String.valueOf(k+1)+ "_1_CM";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                //RT[4]=WalletSecondario;
                                RT[4] = "Principale";
                                if (Mon.Qta.contains("-")){
                                    RT[0]=data.replaceAll(" |-|:", "") +"_Binance_C"+String.valueOf(k+1)+ "_1_CM";
                                    RT[5]="COMMISSIONI";                                
                                    RT[6]=Mon.Moneta+" -> ";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                                
                                    RT[7]=CausaleOriginale;
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;   
                                }else
                                {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_Binance_C"+String.valueOf(k+1)+ "_1_RW";
                                    RT[5]="CASHBACK";//IL RIMBORSO DELLE COMMISSIONI LO TRATTO COME UN CASHBACK
                                    RT[6]=" -> "+Mon.Moneta;//da sistemare con ulteriore dettaglio specificando le monete trattate                                                                
                                    RT[7]=CausaleOriginale;
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta; 
                                }
                                RT[15]=valoreEuro;
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);     
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DUST-CONVERSION")||
                                    movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO-CRYPTO")||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&Mon.Tipo.equals("FIAT")&&Mon.Qta.contains("-"))||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&!Mon.Tipo.equals("FIAT"))
                                    )
                            {
                               // System.out.println(movimentoConvertito);
                               // System.out.println(Mon.Moneta);
                                // serve solo per il calcolo della percentuale di cro da attivare
                                    Scambio.InserisciMoneteCEX(Mon,"Principale",CausaleOriginale,"");
                                   // System.out.println(CausaleOriginale+" - "+dataa+ " - "+Mon.Moneta+" _ "+Mon.Qta);

                           // se è l'ultimo movimento allora creo anche le righe

                                }
                                else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO-INTERNO"))
                            {
                               
                            /*    //come prima cosa devo individuare il portafoglio nel quale vanno i token
                                String Wallet=movimentoSplittato[2];
                                
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                RT[4]=Wallet;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=movimentoSplittato[3];
                                if (movimentoSplittato[5].contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;
                                }else{
                                    // i movimenti di rientro vanno sempre dopo e li distinguo con la A
                                    RT[0]=data.replaceAll(" |-|:", "") +"_Binance_A"+String.valueOf(k+1)+ "_1_TI";
                                    RT[6]="-> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;
                                }                     
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                                
                                if (movimentoSplittato[3].equalsIgnoreCase("Simple Earn Flexible Subscription")||
                                        movimentoSplittato[3].equalsIgnoreCase("Simple Earn Flexible Redemption")||
                                        movimentoSplittato[3].equalsIgnoreCase("Simple Earn Locked Subscription")||
                                        movimentoSplittato[3].equalsIgnoreCase("Simple Earn Locked Redemption")||
                                        movimentoSplittato[3].equalsIgnoreCase("Staking Purchase")||
                                        movimentoSplittato[3].equalsIgnoreCase("Staking Redemption"))
                                {
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_2_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                if (movimentoSplittato[3].contains("Staking"))RT[4]="Staking";
                                //else if (movimentoSplittato[3].contains("Earn"))RT[4]="EARN";
                                else if (movimentoSplittato.length>6)RT[4]=movimentoSplittato[6];
                                else RT[4]=WalletSecondario;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=movimentoSplittato[3];
                                if (!movimentoSplittato[5].contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]="-"+Mon.Qta;
                                }else{
                                    RT[6]=" -> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=new BigDecimal(Mon.Qta).abs().toPlainString();
                                }                     
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                                }
                                
                              */  
                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO")||
                                       movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO DIFFERITO"))
                            {
                                RT = new String[ColonneTabella];
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]="Binance";
                                //RT[4]=movimentoSplittato[2];
                                RT[4] = "Principale";
                                RT[7]=movimentoSplittato[3];
                                if (Mon.Qta.contains("-")) {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_PC";
                                    RT[5]="PRELIEVO CRYPTO";
                                    RT[6]=Mon.Moneta+" ->";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;

                                } else {
                                    if (Mon.Tipo.equalsIgnoreCase("FIAT")){
                                        RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_DF";
                                        RT[5]="DEPOSITO FIAT";
                                        }
                                    else 
                                        {
                                        RT[0]=data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_1_DC";
                                        RT[5]="DEPOSITO CRYPTO";
                                        }
                                    RT[6]="-> "+Mon.Moneta;
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;                              
                                }                                                                                            
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[22]="A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO DIFFERITO"))
                                {
                                    //Gli scambi differiti li analizzarò poi alla fine di tutto
                                  //  String RTcopy[]=new String[ColonneTabella];
                                   // System.arraycopy(RT, 0, RTcopy, 0, ColonneTabella);
                                listaAutoinvest.add(RT);
                                }//else
                                 // {
                                      
                                 // }  
                                //La lista autoinvest serve per individuare i movimenti di autoinvest che sono degli scambi differiti e successivamente analizzarla e sistemarli
                               // else if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO DIFFERITO"))listaAutoinvest.add(RT);
                            }
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
                              
                        //Mancano le transazioni di solo acquisto o solo vendita!!!!!!!
                        
                            // A fine ciclo verifico se ho degli scambi da inserire e li inserisco
                            // Li inserisco alla fine perchè non so quando teminino
                        //  if (k == numMovimenti - 1) {
                        
                                    if(Scambio.lenght()>0){
                                    String TipoScambio=Scambio.IdentificaTipoTransazioneCEX();
                                    Scambio.AssegnaPesiaPartiTransazione();
                                    Map<String, ValoriToken> MappaTokenUscita=Scambio.RitornaMappaTokenUscita();
                                    Map<String, ValoriToken> MappaTokenEntrata=Scambio.RitornaMappaTokenEntrata();
                                    int i = 1;
                                    int totMov = MappaTokenEntrata.size() * MappaTokenUscita.size();
                                    //  RT = new String[ColonneTabella];
                                    //  RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_"+String.valueOf(w+1)+"_SC";
                                    if(TipoScambio.equalsIgnoreCase("Scambio")){
                                    for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                        for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                          /*  if (new BigDecimal(tokenU.Peso).compareTo(new BigDecimal(1)) != 0 || new BigDecimal(tokenE.Peso).compareTo(new BigDecimal(1)) != 0) {
                                              //  System.out.print(tokenU.Moneta + " - " + tokenU.Peso + " - " + tokenU.Qta + " _____ ");
                                              //  System.out.println(tokenE.Moneta + " - " + tokenE.Peso + " - " + tokenE.Qta+ " - "+dataa);
                                            }*/
                                            //peso transazione                  
                                            String QuantitaEntrata = new BigDecimal(tokenE.Qta).multiply(new BigDecimal(tokenU.Peso)).stripTrailingZeros().toPlainString();
                                            String QuantitaUscita = new BigDecimal(tokenU.Qta).multiply(new BigDecimal(tokenE.Peso)).stripTrailingZeros().toPlainString();
                                            Moneta M1 = new Moneta();
                                            M1.InserisciValori(tokenU.Moneta, QuantitaUscita, tokenU.MonetaAddress, tokenU.Tipo);
                                            Moneta M2 = new Moneta();
                                            M2.InserisciValori(tokenE.Moneta, QuantitaEntrata, tokenE.MonetaAddress, tokenE.Tipo);
                                            BigDecimal PrezzoTransazione = new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, M2, Datalong, "0", true, 2, null));
                                            String TipoTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 1);
                                            String CodiceTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 2);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+totMov+ "_"+i+"_"+CodiceTransazione;
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = "Binance";
                                            RT[4] = "Principale";
                                            RT[5] = TipoTransazione;
                                            RT[6] = tokenU.RitornaNomeToken() + " -> " + tokenE.RitornaNomeToken();
                                            RT[7] = tokenU.CausaleOriginale+" - "+tokenE.CausaleOriginale;
                                            RT[8] = tokenU.Moneta;
                                            RT[9] = tokenU.Tipo;
                                            RT[10] = QuantitaUscita;
                                            RT[11] = tokenE.Moneta;
                                            RT[12] = tokenE.Tipo;
                                            RT[13] = QuantitaEntrata;
                                            RT[14] = "";
                                            RT[15] = PrezzoTransazione.toPlainString();
                                            RT[22] = "A";
                                            Importazioni.RiempiVuotiArray(RT);
                                            lista.add(RT);
                                            i++;
                                       // }
                                   }   
                                }
                                    }else if(TipoScambio.equalsIgnoreCase("Deposito")){
                                        for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                            Moneta M2 = new Moneta();
                                            M2.InserisciValori(tokenE.Moneta, tokenE.Qta, tokenE.MonetaAddress, tokenE.Tipo);
                                            BigDecimal PrezzoTransazione;
                                            if (tokenE.Prezzo.isBlank())
                                                PrezzoTransazione = new BigDecimal(Prezzi.DammiPrezzoTransazione(M2,null, Datalong, "0", true, 2, null));
                                            else
                                                PrezzoTransazione=new BigDecimal(tokenE.Prezzo).setScale(2, RoundingMode.HALF_UP);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+totMov+ "_"+i+"_DC";
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = "Binance";
                                            RT[4] = "Principale";
                                            RT[5] = "Deposito";
                                            RT[6] = " -> " + tokenE.RitornaNomeToken();
                                            RT[7] = tokenE.CausaleOriginale;
                                            RT[11] = tokenE.Moneta;
                                            RT[12] = tokenE.Tipo;
                                            RT[13] = tokenE.Qta;
                                            RT[14] = "";
                                            RT[15] = PrezzoTransazione.toPlainString();
                                            RT[22] = "A";
                                            Importazioni.RiempiVuotiArray(RT);
                                            lista.add(RT);
                                            i++;
                                        }
                                     
                                
                                    }
                                    else if(TipoScambio.equalsIgnoreCase("Prelievo")){                                       
                                        for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                            Moneta M2 = new Moneta();
                                            M2.InserisciValori(tokenU.Moneta, tokenU.Qta, tokenU.MonetaAddress, tokenU.Tipo);
                                            BigDecimal PrezzoTransazione;
                                            if (tokenU.Prezzo!=null&&tokenU.Prezzo.isBlank())
                                                PrezzoTransazione = new BigDecimal(Prezzi.DammiPrezzoTransazione(M2,null, Datalong, "0", true, 2, null));
                                            else
                                                PrezzoTransazione=new BigDecimal(tokenU.Prezzo).setScale(2, RoundingMode.HALF_UP);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+totMov+ "_"+i+"_PC";
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = "Binance";
                                            RT[4] = "Principale";
                                            RT[5] = "Prelievo";
                                            RT[6] = tokenU.RitornaNomeToken()+" ->";
                                            RT[7] = tokenU.CausaleOriginale;
                                            RT[8] = tokenU.Moneta;
                                            RT[9] = tokenU.Tipo;
                                            RT[10] = tokenU.Qta;
                                            RT[14] = "";
                                            RT[15] = PrezzoTransazione.toPlainString();
                                            RT[22] = "A";
                                            Importazioni.RiempiVuotiArray(RT);
                                            lista.add(RT);
                                            i++;
                                        }
                                    
                                    }else{System.out.println("Errore assegnazione tipologia (ConsolidaMovimenti_Binance)");}
                                    }
        return lista;
    }   
     
          public static List<String[]> ConsolidaMovimentiGenerica(List<String[]> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali,List<String[]> listaAutoinvest){
         
         //int mov è un indice che viene aggiunto all'ID per permettere a più transazioni con lo stesso id di coesistere
         //es. ho sue scambi identici alla stessa ora su 2 movimenti consolidati diversi, in questo caso idichero a un movimento Mov=1 e all'altro Mov=2
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
              /*ListaMovimentidaConsolidare ->
            [0]=Data
            [1]=Wallet Principale
            [2]=Wallet Secondario
            [3]=Tipologia di Movimento 
         (CASHBACK,STAKING REWARDS,EARN,REWARD,ALTRE-REWARD,DEPOSITO FIAT,ACQUISTO CRYPTO,PRELIEVO FIAT,COMMISSIONI,DUST-CONVERSION,
         SCAMBIO CRYPTO-CRYPTO,ACQUISTO CRYPTO,TRASFERIMENTO-CRYPTO-INTERNO,TRASFERIMENTO-CRYPTO,SCAMBIO DIFFERITO,NON CONSIDERARE)
            [4]=Causale Originale
            [5]=Moneta
            [6]=Qta
            [7]=
            [8]=
            [9]=
            [10]=
            [11]=Moneta fee
            [12]=Qta fee
            [13]=
            [14]=ID Originale
            [15]=Se "SI" creo un movimento opposto sul Wallet di Destinazione 
            [16]=Wallet Destinazione
            [17]=Giacenza Iniziale
            [18]=Giacenza Finale
         
               */

         
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
         String dataa="";
         long Datalong=0;
         String data="";
         String WalletPrincipale="";
         String IDOriginale="";
         TransazioneDefi Scambio=new TransazioneDefi();         
                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimentoSplittato[]=listaMovimentidaConsolidare.get(k);
                            data=movimentoSplittato[0];
                            Moneta Mon=new Moneta();
                            Mon.Moneta=movimentoSplittato[5];
                            Mon.Qta=new BigDecimal(movimentoSplittato[6]).toPlainString();
                            Datalong=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                            
                            if (Mon.Moneta.equalsIgnoreCase("EUR")||Mon.Moneta.equalsIgnoreCase("USD"))
                                Mon.Tipo="FIAT";
                            else
                                Mon.Tipo="Crypto";
                           //Questa parte del prezzo va rivista se i prezzi li prendo dal csv
                            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, Datalong, null, true, 10, null);
                            String valoreEuro = new BigDecimal(Mon.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                            WalletPrincipale=movimentoSplittato[1];
                            String WalletSecondario=movimentoSplittato[2];
                            String CausaleOriginale=movimentoSplittato[4];
                            IDOriginale=movimentoSplittato[14];
                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);

                            dataa=data.trim().substring(0, data.length()-3);

                            String movimentoConvertito=movimentoSplittato[3];
                            //Prima di tutto controllo se il movimento ha delle fee e qualora dovesse averle creo il movimento
                            if (!movimentoSplittato[12].isEmpty()){
                                String Tipo="Crypto";
                                
                                if (movimentoSplittato[11].equalsIgnoreCase("EUR")||movimentoSplittato[11].equalsIgnoreCase("USD")){
                                    Tipo="FIAT";
                                    }
                                Moneta m=new Moneta();
                                m.Moneta=movimentoSplittato[11];
                                m.Qta=movimentoSplittato[12];
                                m.Tipo=Tipo;
                                m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, Datalong, null, true, 10, null);
                                //System.out.println(m.Moneta+"-"+m.Qta+"-"+m.Tipo+"-"+m.Prezzo);
                                String val = new BigDecimal(m.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"C_"+String.valueOf(k+1)+"_1_CM";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;  
                                RT[5]="COMMISSIONI";
                                RT[6]=m.Moneta+" -> ";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                                
                                RT[7]="";
                                RT[8]=m.Moneta;
                                RT[9]=Tipo;
                                RT[10]=m.Qta;                                                                                                                            
                                RT[15]=val;
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                            }
                            
                            
                           if (movimentoSplittato[3].isBlank())
                                {
                                    //non faccio nulla tanto ho già salvato il movimento nel primo ciclo
                                    //altrimenti vado a vanti
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING REWARDS")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD"))
                            {

                                RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;
                                RT[5] = movimentoConvertito;
                                RT[6] = "-> "+Mon.Moneta;                                
                                RT[7] = CausaleOriginale;
                                if (Mon.Qta.contains("-")) {
                                    RT[5] = "RIMBORSO " + movimentoConvertito;
                                    RT[6] = Mon.Moneta+" ->"; 
                                    RT[8] = Mon.Moneta;
                                    RT[9] = Mon.Tipo;
                                    RT[10] = Mon.Qta;
                                    RT[15] = "0.00";
                                } 
                                else 
                                {

                                    RT[11] = Mon.Moneta;
                                    RT[12] = Mon.Tipo;
                                    RT[13] = Mon.Qta;
                                    RT[14] = "";

                                    RT[15] = valoreEuro;
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[6]);
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                    RT[17] = valoreEuro;
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                                RT[22] = "A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("DEPOSITO FIAT")||
                                   (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&Mon.Tipo.equals("FIAT")&&!Mon.Qta.contains("-"))
                                   )
                            {
                                                                //trasferimento FIAT                              
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+ "_1_DF";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;
                                RT[5]="DEPOSITO FIAT";
                                RT[6]="-> "+Mon.Moneta;
                                RT[7]=CausaleOriginale;
                                RT[11]=Mon.Moneta;
                                RT[12]=Mon.Tipo;
                                RT[13]=Mon.Qta;
                                RT[15]=valoreEuro;
                                RT[17]=valoreEuro;
                                RT[18]="";
                                RT[19]="0.00";
                                RT[22]="A";   
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("PRELIEVO FIAT"))
                            {
                                //trasferimento FIAT
                                String Codice;
                                String Descrizione;
                                if (Mon.Qta.contains("-")) {
                                    Codice=RitornaTipologiaTransazione(Mon.Tipo,null,0);
                                    Descrizione=RitornaTipologiaTransazione(Mon.Tipo,null,1);
                                   } 
                                else {
                                    Codice=RitornaTipologiaTransazione(null,Mon.Tipo,0);
                                    Descrizione=RitornaTipologiaTransazione(null,Mon.Tipo,1);
                                }
                                    
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+ "_1_"+Codice;
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;                               
                                RT[5]=Descrizione;
                                RT[7]=CausaleOriginale; 
                                if (Mon.Qta.contains("-")) {
                                RT[5]="SPESA CON CARTA";
                                RT[6]=Mon.Moneta+"-> ";
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;
                                }
                                else{
                                RT[5]="RIMBORSO SU CARTA";
                                RT[6]="-> "+Mon.Moneta;
                                RT[11]=Mon.Moneta;
                                RT[12]=Mon.Tipo;
                                RT[13]=Mon.Qta;    
                                }
                                RT[15]=valoreEuro;
                                RT[19]="0.00";
                                RT[22]="A";   
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("COMMISSIONI"))
                            {
                                //Scambio Crypto Crypto
                                //il C dopo il nome dell'exchange mi serve per far si che le commissioni le metta per ultime
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"C_"+String.valueOf(k+1)+"_1_CM";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;  
                                RT[5]="COMMISSIONI";
                                RT[6]=Mon.Moneta+" -> ";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                                
                                RT[7]=CausaleOriginale;
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;                                                                                                                            
                                RT[15]=valoreEuro;
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);     
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DUST-CONVERSION")||
                                    movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO-CRYPTO")||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&Mon.Tipo.equals("FIAT")&&Mon.Qta.contains("-"))||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&!Mon.Tipo.equals("FIAT"))
                                    )
                            {
                                
                               // System.out.println(Mon.Moneta);
                                // serve solo per il calcolo della percentuale di cro da attivare
                                    Scambio.InserisciMoneteCEX(Mon,WalletSecondario,CausaleOriginale,IDOriginale);
                                   // System.out.println(CausaleOriginale+" - "+dataa+ " - "+Mon.Moneta+" _ "+Mon.Qta);
                                   
                           // se è l'ultimo movimento allora creo anche le righe

                                }
                                else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO-INTERNO"))
                            {
                               
                                //come prima cosa devo individuare il portafoglio nel quale vanno i token
                               // String Wallet=movimentoSplittato[16];
                                
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]=WalletPrincipale;
                                RT[4]=WalletSecondario;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=CausaleOriginale;
                                if (Mon.Qta.contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;
                                }else{
                                    // i movimenti di rientro vanno sempre dopo e li distinguo con la A
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"A_"+String.valueOf(k+1)+ "_1_TI";
                                    RT[6]="-> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;
                                }                     
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);  
                                
                                //Se si creo anche il movimento opposto che arriva a destinazione
                                //oppure parte dall'origine
                                if(movimentoSplittato[15].equalsIgnoreCase("SI")){
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]=WalletPrincipale;
                                RT[4]=movimentoSplittato[16];
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=CausaleOriginale;
                                if (!Mon.Qta.contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]="-"+Mon.Qta;
                                }else{
                                    // i movimenti di rientro vanno sempre dopo e li distinguo con la A
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+movimentoSplittato[1]+IDOriginale+"A_"+String.valueOf(k+1)+ "_1_TI";
                                    RT[6]="-> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta.replace("-", "");
                                }                     
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT); 
                                }
                               
                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO")||
                                       movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO DIFFERITO"))
                            {
                                RT = new String[ColonneTabella];
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;  
                                RT[7] = CausaleOriginale;
                                if (Mon.Qta.contains("-")) {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_PC";
                                    RT[5]="PRELIEVO CRYPTO";
                                    RT[6]=Mon.Moneta+" ->";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;

                                } else {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_DC";
                                    RT[5]="DEPOSITO CRYPTO";
                                    RT[6]="-> "+Mon.Moneta;
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;                              
                                }                                                                                            
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                if (movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO DIFFERITO"))
                                {
                                    //Gli scambi differiti li analizzarò poi alla fine di tutto
                                  //  String RTcopy[]=new String[ColonneTabella];
                                   // System.arraycopy(RT, 0, RTcopy, 0, ColonneTabella);
                                    listaAutoinvest.add(RT);
                                } 
                                //La lista autoinvest serve per individuare i movimenti di autoinvest che sono degli scambi differiti e successivamente analizzarla e sistemarli
                            }else if (movimentoConvertito.trim().equalsIgnoreCase("NON CONSIDERARE")){
                                
                            }
                                    
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimentoSplittato[3]+" SCONOSCIUTO\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
                              

                            // A fine ciclo verifico se ho degli scambi da inserire e li inserisco
                            // Li inserisco alla fine perchè non so quando teminino
                        //  if (k == numMovimenti - 1) {
                        if (!Scambio.isEmpty()){
                                    String TipoScambio=Scambio.IdentificaTipoTransazioneCEX();
                                    Scambio.AssegnaPesiaPartiTransazione();
                                    Map<String, ValoriToken> MappaTokenUscita=Scambio.RitornaMappaTokenUscita();
                                    Map<String, ValoriToken> MappaTokenEntrata=Scambio.RitornaMappaTokenEntrata();
                                    //Se il tipo scambio è disverso da scambio vuol dire che c'è un errore e lo segnalo
                                    if (!TipoScambio.equalsIgnoreCase("Scambio")){
                                        movimentiSconosciuti=movimentiSconosciuti+"Errore su un movimento del "+data+" \n";
                                        TrasazioniSconosciute++;
                                    }

                                  /*  for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                      //  System.out.println(tokenE.Moneta);
                                    }
                                    for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                      //  System.out.println(tokenU.Moneta);
                                    }
                                    //System.out.println("------");*/
                                    int i = 1;
                                    int totMov = MappaTokenEntrata.size() * MappaTokenUscita.size();
                                    //  RT = new String[ColonneTabella];
                                    //  RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_"+String.valueOf(w+1)+"_SC";
                                    for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                        for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                           // System.out.println(tokenU);
                                          /*  if (new BigDecimal(tokenU.Peso).compareTo(new BigDecimal(1)) != 0 || new BigDecimal(tokenE.Peso).compareTo(new BigDecimal(1)) != 0) {
                                              //  System.out.print(tokenU.Moneta + " - " + tokenU.Peso + " - " + tokenU.Qta + " _____ ");
                                              //  System.out.println(tokenE.Moneta + " - " + tokenE.Peso + " - " + tokenE.Qta+ " - "+dataa);
                                            }*/
                                            //peso transazione                  
                                            String QuantitaEntrata = new BigDecimal(tokenE.Qta).multiply(new BigDecimal(tokenU.Peso)).stripTrailingZeros().toPlainString();
                                            String QuantitaUscita = new BigDecimal(tokenU.Qta).multiply(new BigDecimal(tokenE.Peso)).stripTrailingZeros().toPlainString();
                                            Moneta M1 = new Moneta();
                                            M1.InserisciValori(tokenU.Moneta, QuantitaUscita, tokenU.MonetaAddress, tokenU.Tipo);
                                            Moneta M2 = new Moneta();
                                            M2.InserisciValori(tokenE.Moneta, QuantitaEntrata, tokenE.MonetaAddress, tokenE.Tipo);
                                            BigDecimal PrezzoTransazione = new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, M2, Datalong, "0", true, 2, null));
                                            String TipoTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 1);
                                            String CodiceTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 2);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+tokenE.IDTransazione+"_"+totMov+ "_"+i+"_"+CodiceTransazione;
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = WalletPrincipale;
                                            RT[4] = tokenU.WalletSecondario;
                                            RT[5] = TipoTransazione;
                                            RT[6] = tokenU.RitornaNomeToken() + " -> " + tokenE.RitornaNomeToken();
                                            RT[7] = tokenU.CausaleOriginale+" - "+tokenE.CausaleOriginale;
                                            RT[8] = tokenU.Moneta;
                                            RT[9] = tokenU.Tipo;
                                            RT[10] = QuantitaUscita;
                                            RT[11] = tokenE.Moneta;
                                            RT[12] = tokenE.Tipo;
                                            RT[13] = QuantitaEntrata;
                                            RT[14] = "";
                                            RT[15] = PrezzoTransazione.toPlainString();
                                            RT[22] = "A";
                                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                                            RT[29] = String.valueOf(TimeStamp);
                                            RT[24] = tokenU.IDTransazione+"_"+tokenE.IDTransazione;
                                            Importazioni.RiempiVuotiArray(RT);
                                            lista.add(RT);
                                            i++;
                                       // }
                                   }    }
                                }
        return lista;
    }   
    
         
        public static List<String[]> ConsolidaMovimentiSingolaRiga(List<String[]> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali){
         
         //int mov è un indice che viene aggiunto all'ID per permettere a più transazioni con lo stesso id di coesistere
         //es. ho sue scambi identici alla stessa ora su 2 movimenti consolidati diversi, in questo caso idichero a un movimento Mov=1 e all'altro Mov=2
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
            /*ListaMovimentidaConsolidare ->
            [0]=Data
            [1]=Wallet Principale
            [2]=Wallet Secondario
            [3]=Tipologia di Movimento 
         (CASHBACK,STAKING REWARDS,EARN,REWARD,ALTRE-REWARD,DEPOSITO FIAT,ACQUISTO CRYPTO,PRELIEVO FIAT,COMMISSIONI,DUST-CONVERSION,
         SCAMBIO CRYPTO-CRYPTO,ACQUISTO CRYPTO,TRASFERIMENTO-CRYPTO-INTERNO,TRASFERIMENTO-CRYPTO,SCAMBIO DIFFERITO,NON CONSIDERARE)
            [4]=Causale Originale
            [5]=Moneta
            [6]=Qta
            [7]=Address Moneta
            [8]=Valore Originale in EURO
            [9]=ID Originale
            [10]=Rete
         
               */

         
        // System.out.println("sono qui");
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
         //System.out.println(numMovimenti);
         String dataa="";
         long Datalong=0;
         String data="";
         String WalletPrincipale="";
         String IDOriginale="";
         TransazioneDefi Scambio=new TransazioneDefi();         
                        for (int k=0;k<numMovimenti;k++){
                            //System.out.println("sono qui");
                            String RT[]=new String[ColonneTabella];
                            String movimentoSplittato[]=listaMovimentidaConsolidare.get(k);
                            data=movimentoSplittato[0];
                            Moneta Mon=new Moneta();
                            Mon.Moneta=movimentoSplittato[5];
                            Mon.Qta=new BigDecimal(movimentoSplittato[6]).toPlainString();
                            Mon.MonetaAddress=movimentoSplittato[7];
                            Datalong=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                            
                            if (Mon.Moneta.equalsIgnoreCase("EUR")||Mon.Moneta.equalsIgnoreCase("USD"))
                                Mon.Tipo="FIAT";
                            else
                                Mon.Tipo="Crypto";
                            
                           //Questa parte del prezzo va rivista se i prezzi li prendo dal csv
                            if (movimentoSplittato[8].isBlank()){
                                Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, Datalong, null, true, 10, null);
                            }
                            else{
                                Mon.Prezzo = movimentoSplittato[8];
                            }
                            
                            String valoreEuro = new BigDecimal(Mon.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                            
                            WalletPrincipale=movimentoSplittato[1];
                            String WalletSecondario=movimentoSplittato[2];
                            String CausaleOriginale=movimentoSplittato[4];
                            IDOriginale=movimentoSplittato[9];
                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);

                            dataa=data.trim().substring(0, data.length()-3);

                            String movimentoConvertito=Mappa_Conversione_Causali.get(movimentoSplittato[3]);
                            if (movimentoConvertito==null)movimentoConvertito=movimentoSplittato[3];
                            //System.out.println(movimentoSplittato[3]);
                            
                            
                            
                           if (movimentoSplittato[3].isBlank())
                                {
                                    //non faccio nulla tanto ho già salvato il movimento nel primo ciclo
                                    //altrimenti vado a vanti
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING REWARDS")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("AIRDROP"))
                            {

                                RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;
                                RT[5] = movimentoConvertito;
                                RT[6] = "-> "+Mon.Moneta;                                
                                RT[7] = CausaleOriginale;
                                if (Mon.Qta.contains("-")) {
                                    RT[5] = "RIMBORSO " + movimentoConvertito;
                                    RT[6] = Mon.Moneta+" ->"; 
                                    RT[8] = Mon.Moneta;
                                    RT[9] = Mon.Tipo;
                                    RT[10] = Mon.Qta;
                                    RT[14] = movimentoSplittato[8];
                                    RT[15] = "0.00";
                                } 
                                else 
                                {

                                    RT[11] = Mon.Moneta;
                                    RT[12] = Mon.Tipo;
                                    RT[13] = Mon.Qta;
                                    RT[14] = movimentoSplittato[8];

                                    RT[15] = valoreEuro;
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[6]);
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                    RT[17] = valoreEuro;
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                                RT[22] = "A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RT[28] = Mon.MonetaAddress;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT);
                                
                            }                           
                            else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO")&&Mon.Tipo.equals("FIAT"))
                            {
                                //trasferimento FIAT
                                String Codice;
                                String Descrizione;
                                if (Mon.Qta.contains("-")) {
                                    Codice=RitornaTipologiaTransazione(Mon.Tipo,null,0);
                                    Descrizione=RitornaTipologiaTransazione(Mon.Tipo,null,1);
                                   } 
                                else {
                                    Codice=RitornaTipologiaTransazione(null,Mon.Tipo,0);
                                    Descrizione=RitornaTipologiaTransazione(null,Mon.Tipo,1);
                                }
                                    
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+ "_1_"+Codice;
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;                               
                                RT[5]=Descrizione;
                                RT[7]=CausaleOriginale; 
                                if (Mon.Qta.contains("-")) {
                                RT[5]="PRELIEVO FIAT";
                                RT[6]=Mon.Moneta+"-> ";
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;
                                }
                                else{
                                //Nel caso di depositi metto uno zero davanti al numero di movimento perchè in caso di date coincidenti
                                //il deposito sarà sempre il primo movimento da considerare
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_0"+String.valueOf(k+1)+ "_1_"+Codice;    
                                RT[5]="DEPOSITO FIAT";
                                RT[6]="-> "+Mon.Moneta;
                                RT[11]=Mon.Moneta;
                                RT[12]=Mon.Tipo;
                                RT[13]=Mon.Qta;    
                                }
                                RT[14] = movimentoSplittato[8];
                                RT[15]=valoreEuro;
                                RT[19]="0.00";
                                RT[22]="A";   
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT);
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("COMMISSIONI"))
                            {
                                //Scambio Crypto Crypto
                                //il C dopo il nome dell'exchange mi serve per far si che le commissioni le metta per ultime
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"C_"+String.valueOf(k+1)+"_1_CM";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;  
                                RT[5]="COMMISSIONI";
                                RT[6]=Mon.Moneta+" -> ";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                                
                                RT[7]=CausaleOriginale;
                                RT[8]=Mon.Moneta;
                                RT[9]=Mon.Tipo;
                                RT[10]=Mon.Qta;    
                                RT[14] = movimentoSplittato[8];
                                RT[15]=valoreEuro;
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RT[26] = Mon.MonetaAddress;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT);     
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("DUST-CONVERSION")||
                                    movimentoConvertito.trim().equalsIgnoreCase("SCAMBIO CRYPTO-CRYPTO")||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&Mon.Tipo.equals("FIAT")&&Mon.Qta.contains("-"))||
                                    (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO")&&!Mon.Tipo.equals("FIAT"))
                                    )
                            {
                                
                              //  System.out.println(Mon.Moneta);
                                // serve solo per il calcolo della percentuale di cro da attivare
                                    Scambio.InserisciMoneteCEX(Mon,WalletSecondario,CausaleOriginale,IDOriginale);
                                   // System.out.println(CausaleOriginale+" - "+dataa+ " - "+Mon.Moneta+" _ "+Mon.Qta);
                                   
                           // se è l'ultimo movimento allora creo anche le righe

                                }
                                else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO-INTERNO"))
                            {
                               
                                //come prima cosa devo individuare il portafoglio nel quale vanno i token
                               // String Wallet=movimentoSplittato[16];
                                
                                
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]=WalletPrincipale;
                                RT[4]=WalletSecondario;
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=CausaleOriginale;
                                if (Mon.Qta.contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;
                                    RT[26] = Mon.MonetaAddress;
                                }else{
                                    // i movimenti di rientro vanno sempre dopo e li distinguo con la A
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"A_"+String.valueOf(k+1)+ "_1_TI";
                                    RT[6]="-> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;
                                    RT[28] = Mon.MonetaAddress;
                                }
                                RT[14] = movimentoSplittato[8];
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT);  
                                
                                //Se si creo anche il movimento opposto che arriva a destinazione
                                //oppure parte dall'origine
                                if(movimentoSplittato[15].equalsIgnoreCase("SI")){
                                RT = new String[ColonneTabella];
                                RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_TI";
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3]=WalletPrincipale;
                                RT[4]=movimentoSplittato[16];
                                RT[5]="TRASFERIMENTO INTERNO";
                                RT[7]=CausaleOriginale;
                                if (!Mon.Qta.contains("-")){
                                    RT[6]=Mon.Moneta+" -> ";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]="-"+Mon.Qta;
                                    RT[26] = Mon.MonetaAddress;
                                }else{
                                    // i movimenti di rientro vanno sempre dopo e li distinguo con la A
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+movimentoSplittato[1]+IDOriginale+"A_"+String.valueOf(k+1)+ "_1_TI";
                                    RT[6]="-> "+Mon.Moneta;                                   
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta.replace("-", "");
                                    RT[28] = Mon.MonetaAddress;
                                }
                                RT[14] = movimentoSplittato[8];
                                RT[15]=valoreEuro;                                
                                RT[19]="0.00";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT); 
                                }
                               
                            }
                               else if (movimentoConvertito.trim().equalsIgnoreCase("TRASFERIMENTO-CRYPTO")&&!Mon.Tipo.equals("FIAT"))
                            {
                                RT = new String[ColonneTabella];
                                RT[1]=dataa;
                                RT[2]=1+" di "+1;
                                RT[3] = WalletPrincipale;
                                RT[4] = WalletSecondario;  
                                RT[7] = CausaleOriginale;
                                if (Mon.Qta.contains("-")) {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_PC";
                                    RT[5]="PRELIEVO CRYPTO";
                                    RT[6]=Mon.Moneta+" ->";
                                    RT[8]=Mon.Moneta;
                                    RT[9]=Mon.Tipo;
                                    RT[10]=Mon.Qta;
                                    RT[26] = Mon.MonetaAddress;

                                } else {
                                    RT[0]=data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+IDOriginale+"_"+String.valueOf(k+1)+"_1_DC";
                                    RT[5]="DEPOSITO CRYPTO";
                                    RT[6]="-> "+Mon.Moneta;
                                    RT[11]=Mon.Moneta;
                                    RT[12]=Mon.Tipo;
                                    RT[13]=Mon.Qta;
                                    RT[28] = Mon.MonetaAddress;
                                }
                                RT[14] = movimentoSplittato[8];
                                RT[15]=valoreEuro;
                                RT[16]="";
                                RT[17]="Da calcolare";
                                RT[18]="";
                                RT[19]="Da calcolare";
                                RT[20]="";
                                RT[22]="A";
                                RT[29] = String.valueOf(TimeStamp);
                                RT[24] = IDOriginale;
                                RiempiVuotiArray(RT);
                                Prezzi.IndicaMovimentoPrezzato(RT);
                                lista.add(RT);
                                //La lista autoinvest serve per individuare i movimenti di autoinvest che sono degli scambi differiti e successivamente analizzarla e sistemarli
                            }else if (movimentoConvertito.trim().equalsIgnoreCase("NON CONSIDERARE")){
                                
                            }
                                    
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimentoSplittato[3]+" SCONOSCIUTO\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
                              

                            // A fine ciclo verifico se ho degli scambi da inserire e li inserisco
                            // Li inserisco alla fine perchè non so quando teminino
                        //  if (k == numMovimenti - 1) {
                        if (!Scambio.isEmpty()){
                            //System.out.println("scambioooooooo");
                                    String TipoScambio=Scambio.IdentificaTipoTransazioneCEX();
                                    Scambio.AssegnaPesiaPartiTransazione();
                                    Map<String, ValoriToken> MappaTokenUscita=Scambio.RitornaMappaTokenUscita();
                                    Map<String, ValoriToken> MappaTokenEntrata=Scambio.RitornaMappaTokenEntrata();
                                    //Se il tipo scambio è disverso da scambio vuol dire che c'è un errore e lo segnalo
                                    
                                   
                                    if (TipoScambio.equalsIgnoreCase("Deposito")){
                                        int i = 1;
                                        int totMov = MappaTokenEntrata.size();
                                        for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                            String TipoTransazione=Importazioni.RitornaTipologiaTransazione(null, tokenE.Tipo, 1);
                                            String CodiceTransazione=Importazioni.RitornaTipologiaTransazione(null, tokenE.Tipo, 2);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+tokenE.IDTransazione+"_"+totMov+ "_"+i+"_"+CodiceTransazione;
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = WalletPrincipale;
                                            RT[4] = tokenE.WalletSecondario;
                                            RT[5] = TipoTransazione;
                                            RT[6] = "-> " + tokenE.RitornaNomeToken();
                                            RT[7] = tokenE.CausaleOriginale;
                                            RT[11] = tokenE.Moneta;
                                            RT[12] = tokenE.Tipo;
                                            RT[13] = tokenE.Qta;
                                            RT[14] = "";
                                            RT[15] = new BigDecimal(tokenE.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                                            RT[22] = "A";
                                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                                            RT[29] = String.valueOf(TimeStamp);
                                            RT[24] = tokenE.IDTransazione;
                                            RT[26] = "";
                                            RT[28] = tokenE.MonetaAddress;
                                            Importazioni.RiempiVuotiArray(RT);
                                            Prezzi.IndicaMovimentoPrezzato(RT);
                                            if (!(RT[13].equals("0")))
                                            {
                                                lista.add(RT);
                                            }
                                            i++;    
                                        }
                                    }
                                    
                                    else if (TipoScambio.equalsIgnoreCase("Prelievo")){
                                        int i = 1;
                                         int totMov = MappaTokenUscita.size();
                                        for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                            String TipoTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo,null, 1);
                                            String CodiceTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo,null, 2);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+tokenU.IDTransazione+"_"+totMov+ "_"+i+"_"+CodiceTransazione;
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = WalletPrincipale;
                                            RT[4] = tokenU.WalletSecondario;
                                            RT[5] = TipoTransazione;
                                            RT[6] = tokenU.RitornaNomeToken() + " ->";
                                            RT[7] = tokenU.CausaleOriginale;
                                            RT[8] = tokenU.Moneta;
                                            RT[9] = tokenU.Tipo;
                                            RT[10] = tokenU.Qta;
                                            RT[14] = "";
                                            RT[15] = new BigDecimal(tokenU.Prezzo).setScale(2, RoundingMode.HALF_UP).toPlainString();
                                            RT[22] = "A";
                                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                                            RT[29] = String.valueOf(TimeStamp);
                                            RT[24] = tokenU.IDTransazione;
                                            RT[26] = tokenU.MonetaAddress;
                                            RT[28] = "";
                                            Importazioni.RiempiVuotiArray(RT);
                                            Prezzi.IndicaMovimentoPrezzato(RT);
                                            if (!(RT[10].equals("0")))
                                            {
                                                lista.add(RT);
                                            }
                                            i++; 
                                            
                                        }
                                    }

                                 /*   for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                        System.out.println(tokenE.Moneta+" - "+tokenE.Qta+" - "+tokenE.Peso+" - "+tokenE.Prezzo);
                                    }
                                    for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                        System.out.println(tokenU.Moneta+" - "+tokenU.Qta+" - "+tokenU.Peso+" - "+tokenU.Prezzo);
                                    }
                                    System.out.println("------");*/
                                    else if (TipoScambio.equalsIgnoreCase("Scambio")){
                                    int i = 1;
                                    int totMov = MappaTokenEntrata.size() * MappaTokenUscita.size();
                                    //  RT = new String[ColonneTabella];
                                    //  RT[0] = data.replaceAll(" |-|:", "") +"_Binance_"+String.valueOf(k+1)+ "_"+String.valueOf(w+1)+"_SC";
                                    for (ValoriToken tokenE : MappaTokenEntrata.values()) {
                                        for (ValoriToken tokenU : MappaTokenUscita.values()) {
                                           // System.out.println(tokenU);
                                          /*  if (new BigDecimal(tokenU.Peso).compareTo(new BigDecimal(1)) != 0 || new BigDecimal(tokenE.Peso).compareTo(new BigDecimal(1)) != 0) {
                                              //  System.out.print(tokenU.Moneta + " - " + tokenU.Peso + " - " + tokenU.Qta + " _____ ");
                                              //  System.out.println(tokenE.Moneta + " - " + tokenE.Peso + " - " + tokenE.Qta+ " - "+dataa);
                                            }*/
                                            //peso transazione                  
                                            String QuantitaEntrata = new BigDecimal(tokenE.Qta).multiply(new BigDecimal(tokenU.Peso)).stripTrailingZeros().toPlainString();
                                            String PrezzoEntrata = new BigDecimal(tokenE.Prezzo).multiply(new BigDecimal(tokenU.Peso)).stripTrailingZeros().toPlainString();
                                            String QuantitaUscita = new BigDecimal(tokenU.Qta).multiply(new BigDecimal(tokenE.Peso)).stripTrailingZeros().toPlainString();
                                            String PrezzoUscita = new BigDecimal(tokenU.Prezzo).multiply(new BigDecimal(tokenE.Peso)).stripTrailingZeros().toPlainString();
                                            Moneta M1 = new Moneta();
                                            M1.InserisciValori(tokenU.Moneta, QuantitaUscita, tokenU.MonetaAddress, tokenU.Tipo);
                                            Moneta M2 = new Moneta();
                                            M2.InserisciValori(tokenE.Moneta, QuantitaEntrata, tokenE.MonetaAddress, tokenE.Tipo);
                                            BigDecimal PrezzoTransazione;
                                            PrezzoTransazione=new BigDecimal(PrezzoEntrata);
                                            if (PrezzoTransazione.compareTo(new BigDecimal(0))==0) PrezzoTransazione=new BigDecimal(PrezzoUscita);
                                            if (PrezzoTransazione.compareTo(new BigDecimal(0))==0) PrezzoTransazione = new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, M2, Datalong, "0", true, 2, null));
                                            PrezzoTransazione=PrezzoTransazione.setScale(2,RoundingMode.HALF_UP);
                                            String TipoTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 1);
                                            String CodiceTransazione=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo, 2);
                                            String RT[] = new String[ColonneTabella];
                                            RT[0] = data.replaceAll(" |-|:", "") +"_"+WalletPrincipale+tokenE.IDTransazione+"_"+totMov+ "_"+i+"_"+CodiceTransazione;
                                            RT[1] = dataa;
                                            RT[2] = i + " di " + totMov;
                                            RT[3] = WalletPrincipale;
                                            RT[4] = tokenU.WalletSecondario;
                                            RT[5] = TipoTransazione;
                                            RT[6] = tokenU.RitornaNomeToken() + " -> " + tokenE.RitornaNomeToken();
                                            RT[7] = tokenU.CausaleOriginale+" - "+tokenE.CausaleOriginale;
                                            RT[8] = tokenU.Moneta;
                                            RT[9] = tokenU.Tipo;
                                            RT[10] = QuantitaUscita;
                                            RT[11] = tokenE.Moneta;
                                            RT[12] = tokenE.Tipo;
                                            RT[13] = QuantitaEntrata;
                                            RT[14] = "";
                                            RT[15] = PrezzoTransazione.toPlainString();
                                            RT[22] = "A";
                                            long TimeStamp=OperazioniSuDate.ConvertiDatainLongSecondo(data);
                                            RT[29] = String.valueOf(TimeStamp);
                                            RT[24] = tokenU.IDTransazione+"_"+tokenE.IDTransazione;
                                            RT[26] = tokenU.MonetaAddress;
                                            RT[28] = tokenE.MonetaAddress;
                                           // System.out.println(tokenU.MonetaAddress+" - "+tokenE.MonetaAddress);
                                            Importazioni.RiempiVuotiArray(RT);
                                            Prezzi.IndicaMovimentoPrezzato(RT);
                                            
                                            //se scambio la stessa moneta e stessa qta non genero il movimento
                                            if (!(RT[8].equals(RT[11])&&RT[10].equals(RT[13])))
                                            {
                                                lista.add(RT);
                                            }
                                            i++;
                                       // }
                                   }    }
                                } 
                                    else {
                                        //se arrivo qua non so che movimento sia
                                        movimentiSconosciuti=movimentiSconosciuti+"Errore su un movimento del "+data+" - "+TipoScambio+"\n";
                                        TrasazioniSconosciute++;
                                    }
                        }
        return lista;
    }   
    
           
          
          
        public static List<String[]> ConsolidaMovimenti_CoinTracking(List<String> listaMovimentidaConsolidare,String Exchange,boolean PrezzoZero){
         //PER ID TRANSAZIONE QUESTI SONO GLI ACRONIMI
         //TI=Trasferimento Interno
         //TC=Trasferimento Criptoattività          -> non dovrebbe essere utilizzato
         //DC=Deposito Criptoattività
         //PC=Prelievo Criptoattività
         //DF=Deposito Fiat
         //PF=Prelievo Fiat
         //SF=Scambio Fiat                          -> Non utilizzato
         //TF=Trasferimento Fiat                    -> non dovrebbe essere utilizzato
         //AC=Acquisto Criptoattività (con FIAT)
         //VC=Vendita Criptoattività (per FIAT)
         //SC=Scambio Criptoattività
         //AN=Acquisto NFT (con fiat o crypto)  //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //VN=Vendita NFT (per Fiat o Crypto)   //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //SN=Scambio NFT                       //non più utilizzata, viene utilizzata la stessa nomenclatura delle crypto essendo entrambe criptoattività
         //RW=Staking/caschback/airdrop etc....
         //CM=Commissioni/Fees
         
         
       /*              String splittata[] = riga.split(",");
            String data=Formatta_Data_CoinTracking(splittata[12]);*/
         
         
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();


                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[ColonneTabella];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                           // System.out.println(movimentoSplittato[9]);
                            String data=movimentoSplittato[12];
                            String dataa=data.substring(0, data.length()-3).trim();
                            if (movimentoSplittato[0].trim().equalsIgnoreCase("Trade")||movimentoSplittato[0].trim().equalsIgnoreCase("Operazione"))
                            {
                                if (!movimentoSplittato[6].trim().equalsIgnoreCase("EUR")&&!movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))
                                    {
                                        // in Questo caso si tratta di uno scambio Crypto-Crypto
                                //System.out.println(movimentoSplittato[0].replaceAll(" |-|:", ""));
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_SC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "SCAMBIO CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "Crypto";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                String prezzoTrans;
                                if (new BigDecimal(movimentoSplittato[4]).compareTo(new BigDecimal("0"))==0) prezzoTrans=movimentoSplittato[8];
                                else prezzoTrans=movimentoSplittato[4];
                                RT[14] = "EUR "+prezzoTrans;
                                //questo è il primo movimento fatto, mancano gli altri
                                Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";//verrà calcolato con il metodo lifo
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                
                            }else if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))
                                    {
                                        //in questo caso abbiamo un Acquisto Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_AC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "ACQUISTO CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "FIAT";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                String prezzoTrans=new BigDecimal(movimentoSplittato[5]).setScale(2, RoundingMode.HALF_UP).toString();
                                RT[14] = "EUR "+movimentoSplittato[5];
                                Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                              //  RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                        
                                    }
                                
                            else if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))
                                    {
                                        //in questo caso abbiamo una Vendita Crypto
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_VC";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "VENDITA CRYPTO";
                                RT[6] = movimentoSplittato[6]+" -> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "Crypto";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "FIAT";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[1];
                                String prezzoTrans=new BigDecimal(movimentoSplittato[1]).setScale(2, RoundingMode.HALF_UP).toString();
                                Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                               // RT[15] =  Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";//verrà calcolato con il metodo lifo
                                RT[18] = "";
                                RT[19] = "Da calcolare";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                    }
                            }
                           else if (movimentoSplittato[0].trim().equalsIgnoreCase("Other Income")
                                   ||movimentoSplittato[0].trim().equalsIgnoreCase("Altri redditi")||
                                   movimentoSplittato[0].trim().equalsIgnoreCase("Reddito da interessi")||
                                   movimentoSplittato[0].trim().equalsIgnoreCase("Ricompensa / Bonus"))
                            {
                                //Rewards di vario tipo
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "ALTRE-REWARD";
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = "";
                                RT[9] = "";
                                RT[10] = "";
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[4];
                                String prezzoTrans= movimentoSplittato[4];
                              //  Moneta M1=new Moneta();
                             //   M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(null,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                               // RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = RT[15];
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Staking"))
                            {
                                //Rewards di vario tipo
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_RW";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5] = "STAKING";
                                RT[6] = "-> "+movimentoSplittato[2];                                
                                RT[7] = movimentoSplittato[0];
                                RT[8] = "";
                                RT[9] = "";
                                RT[10] = "";
                                RT[11] = movimentoSplittato[2];
                                RT[12] = "Crypto";
                                RT[13] = movimentoSplittato[1];
                                RT[14] = "EUR "+movimentoSplittato[4];
                                String prezzoTrans= movimentoSplittato[4];
                                //Moneta M1=new Moneta();
                                //M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(null,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                                //RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = RT[15];
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Other Fee") 
                                    || movimentoSplittato[0].trim().equalsIgnoreCase("Altra commissione"))
                            {
                                //Commissioni
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_CM";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5]="COMMISSIONI";
                                RT[6]="Commissione in "+movimentoSplittato[6];//da sistemare con ulteriore dettaglio specificando le monete trattate                                                               
                                RT[7] = movimentoSplittato[0];                                
                                RT[8] = movimentoSplittato[6];
                                String TipoMoneta="Crypto";
                                if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR"))TipoMoneta="FIAT";
                                RT[9] = TipoMoneta;
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = "";
                                RT[12] = "";
                                RT[13] = "";
                                RT[14] = "EUR "+movimentoSplittato[8];
                                String prezzoTrans= movimentoSplittato[8];
                                Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                //Moneta M2=new Moneta();
                                //M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                               // RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = "Da calcolare";
                                RT[18] = "";
                                RT[19] = "Da calcolare";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                                              
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Spesa"))
                            {
                                //Commissioni
                                
                                RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PF";
                                RT[1] = dataa;
                                RT[2] = k + 1 + " di " + numMovimenti;
                                RT[3] = Exchange;
                                RT[4] = movimentoSplittato[10];
                                RT[5]="PRELIEVO FIAT";
                                RT[6]=movimentoSplittato[6]+" ->";//da sistemare con ulteriore dettaglio specificando le monete trattate                                                               
                                RT[7] = movimentoSplittato[0];                                
                                RT[8] = movimentoSplittato[6];
                                RT[9] = "FIAT";
                                RT[10] = "-"+movimentoSplittato[5];
                                RT[11] = "";
                                RT[12] = "";
                                RT[13] = "";
                                RT[14] = "EUR "+movimentoSplittato[5];
                                String prezzoTrans= new BigDecimal(movimentoSplittato[5]).setScale(2, RoundingMode.HALF_UP).toString();
                                Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                //Moneta M2=new Moneta();
                                //M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                                //RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                RT[16] = "";
                                RT[17] = RT[15];
                                RT[18] = "";
                                RT[19] = "0.00";
                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                RiempiVuotiArray(RT);
                                lista.add(RT);
                                                              
                            }
                            else if (movimentoSplittato[0].trim().equalsIgnoreCase("Prelievo")||movimentoSplittato[0].trim().equalsIgnoreCase("Deposito"))
                            {
                                //Trasferimenti Crypto, ora bisognerà eliminare quelli doppi (prelievo e deposito stessa cifra e stesso wallet) 
                                //e discernere i trasferimenti tra wallet da quelli interni all'exchange
                                
 
                                // serve solo per il calcolo della percentuale di cro da attivare
                       //  for (int k=0;k<numMovimenti;k++){
                       //     String RT[]=new String[23];
                       //     String movimento=listaMovimentidaConsolidare.get(k);
                                
                           // se è l'ultimo movimento allora creo anche le righe
                                
                                  // System.out.println(movimento);
                                 //  System.out.println(numeroAddebiti);
                                String TipoMov="TRASFERIMENTO CRYPTO";
                                String TipoMovAbbreviato="TC";
                                boolean trovatacorrispondenza=false;
                                boolean annullato=false;
                                for (int w = 0; w < numMovimenti; w++) {
                                    String mov=listaMovimentidaConsolidare.get(w);
                                    String movSplit[]=mov.split(",");
                                 //   System.out.println(movSplit[0]+" "+numMovimenti);
                                    //controllo se trovo un movimento di deposito e una controparte di prelievo
                                    if ((movimentoSplittato[0].trim().equalsIgnoreCase("Deposito")&&movSplit[0].trim().equalsIgnoreCase("Prelievo"))||
                                            (movimentoSplittato[0].trim().equalsIgnoreCase("Prelievo")&&movSplit[0].trim().equalsIgnoreCase("Deposito")))
                                    {
                                        //adesso controllo se i valori e valute di deposito e prelievo sono identici
                                        //System.out.println("Trovato Deposito e prelievo");
                                        //System.out.println(movimentoSplittato[1]+" "+movSplit[5]);
                                       if (movimentoSplittato[1].trim().equalsIgnoreCase(movSplit[5].trim())&&
                                               movimentoSplittato[5].trim().equalsIgnoreCase(movSplit[1].trim())&&
                                               movimentoSplittato[2].trim().equalsIgnoreCase(movSplit[6].trim())&&
                                               movimentoSplittato[6].trim().equalsIgnoreCase(movSplit[2].trim()))
                                           {
                                             //  System.out.println("Stesso Wallet");
                                               //adesso controllo se sono relativi allo stesso wallet, in quel caso non scrivo nulla in caso contrario lo tratto come trasferimento interno
                                               if (movimentoSplittato[10].trim().equalsIgnoreCase(movSplit[10].trim()))                                                   
                                                {
                                                    //Se è uguale anche il wallet i movimenti si annullano e non scrivo nulla
                                                    //System.out.println("Stesso Wallet");
                                                    annullato=true;
                                                }
                                               else //altrimenti lo salvo come movimento interno
                                                {
                                                    trovatacorrispondenza=true;
                                                    TipoMov="TRASFERIMENTO INTERNO";
                                                    TipoMovAbbreviato="TI";
                                                  //  System.out.println("ok");
                                                }   
                                           }
                                    }
                                    }
                                //se dopo il ciclo trovo che non è ne un traferimento interno ne un movimento da non importare allora
                                //significa che è un traferimento Crypto o traferimento FIAT
                                //a questo punto verifico che tipo di movimento è e imposto le variabili di conseguenza
                                if (trovatacorrispondenza==false && annullato==false && 
                                        (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")||movimentoSplittato[2].trim().equalsIgnoreCase("EUR")||
                                        movimentoSplittato[6].trim().equalsIgnoreCase("USD")||movimentoSplittato[2].trim().equalsIgnoreCase("USD")))
                                {
                                        TipoMov="TRASFERIMENTO FIAT";
                                        TipoMovAbbreviato="TF";
                                }
                                if (annullato==false)   
                                    {
                                    RT=new String[ColonneTabella];
                                    RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_"+TipoMovAbbreviato;
                                    RT[1] = dataa;
                                    RT[2] = k + 1 + " di " + numMovimenti;
                                    RT[3] = Exchange;
                                    RT[4] = movimentoSplittato[10];
                                    RT[5] = TipoMov;
                                    //parte sotto da sistemare in base se è un prelievo o un deposito

                                    String TipoMovimento=movimentoSplittato[0].trim();

                                                                                                 
                                    
                                    if (TipoMovimento.equalsIgnoreCase("Deposito"))
                                        {
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                        RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_DC";
                                        RT[5] = "DEPOSITO CRYPTO";
                                        }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                              RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_DF";
                                              RT[5] = "DEPOSITO FIAT";  
                                            }
                                        RT[6] ="-> "+movimentoSplittato[2];
                                        RT[7] = TipoMovimento;
                                        RT[8] = "";
                                        RT[9] = "";
                                        RT[10] = "";
                                        RT[11] = movimentoSplittato[2];
                                        if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR")) RT[12] = "FIAT";else RT[12] = "Crypto";
                                        RT[13] = movimentoSplittato[1];
                                        String valoreEur;
                                        if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR")) valoreEur = movimentoSplittato[1];else valoreEur =movimentoSplittato[4];
                                        RT[14] = "EUR "+valoreEur;
                                        String prezzoTrans=new BigDecimal(valoreEur).setScale(2, RoundingMode.HALF_UP).toString();
                                      //  Moneta M1=new Moneta();
                               // M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                                Moneta M2=new Moneta();
                                M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(null,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                                       // RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                        RT[16] = "";
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                                RT[17]="Da calcolare";
                                                //se sono qua significa che c'è in piedi un deposito crypto
                                            }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TI")) RT[17] = "Da calcolare";else RT[17]=RT[15];
                                        RT[18] = "";
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                                RT[19]="Da calcolare";
                                                //se sono qua significa che c'è in piedi un deposito crypto
                                            }
                                        else RT[19] = "0.00";
                                        }
                                    if (TipoMovimento.equalsIgnoreCase("Prelievo"))
                                        {
                                        if (!TipoMovAbbreviato.equalsIgnoreCase("TI")&&!TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                        RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PC";
                                        RT[5] = "PRELIEVO CRYPTO";
                                        }
                                        else if (TipoMovAbbreviato.equalsIgnoreCase("TF"))
                                            {
                                              RT[0] = data.replaceAll(" |-|:", "") +"_"+Exchange.replaceAll(" ", "")+"_"+String.valueOf(k+1)+ "_1_PF";
                                              RT[5] = "PRELIEVO FIAT";  
                                            }
                                        RT[6] = movimentoSplittato[6]+" ->";
                                        RT[7] = TipoMovimento;
                                        RT[8] = movimentoSplittato[6];
                                        if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) RT[9] = "FIAT";else RT[9] = "Crypto";
                                        RT[10] = "-"+movimentoSplittato[5];
                                        RT[11] = "";
                                        RT[12] = "";
                                        RT[13] = "";
                                        String valoreEur;
                                        if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) valoreEur = movimentoSplittato[5];else valoreEur =movimentoSplittato[8];
                                        RT[14] = "EUR "+valoreEur;
                                        String prezzoTrans=new BigDecimal(valoreEur).setScale(2, RoundingMode.HALF_UP).toString();
                                        Moneta M1=new Moneta();
                                M1.InserisciValori(RT[8],RT[10],null,RT[9]);
                               // Moneta M2=new Moneta();
                               // M2.InserisciValori(RT[11],RT[13],null,RT[12]);
                                RT[15] = Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null);
                                        //RT[15] = Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataa), prezzoTrans,PrezzoZero,2,null,null,null);
                                        RT[16] = "";
                                        RT[17] = "Da Calcolare";
                                        RT[18] = "";
                                        if (TipoMovAbbreviato.equalsIgnoreCase("TI"))RT[19] = "0.00";else RT[19] = "Da calcolare";

                                        }
                                    RT[20] = "";
                                    RT[21] = "";
                                    RT[22] = "A";
                                    RiempiVuotiArray(RT);
                                    lista.add(RT);
                                  }   
                                }else if (movimentoSplittato[0].trim().equalsIgnoreCase("Reddito (non imponibile)")||
                                    movimentoSplittato[0].trim().equalsIgnoreCase("Spese (non imponibili)"))
                            {
                                System.out.println("Movimento di entrata e uscita da earn scartato dai movimenti");
                            }
                                
                               
                             
                           else
                                    {
                                        //qui ci saranno tutti i movimenti scartati
                                    //    System.out.println(movimento);
                                        movimentiSconosciuti=movimentiSconosciuti+movimento+"\n";
                                        TrasazioniSconosciute++;
                                    }
                           
                           
                        }
        
        return lista;
    }   
    
        
    public static String vespa(String strToDecrypt, String secret) 
    {
        try
        {
            SecretKeySpec secretKey=setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } 
        catch (Exception e) 
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }  
        
    
    public static SecretKeySpec setKey(String myKey) 
    {
        MessageDigest sha = null;
        SecretKeySpec secretKey=null;
        try {
            byte[] key;
            
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); 
            secretKey = new SecretKeySpec(key, "AES");
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return secretKey;
    }   
     
    
    //se int=1 ritorna tipologia altrimenti ritorna codice
     public static String RitornaTipologiaTransazione(String TipoUscita,String TipoEntrata,int valore)
         {   
             String Tipologia=null;
             String CodiceTipologia=null;
             if(TipoUscita==null){
                 //ora gestisco le varie casistiche se M1 è una crypto
                 if(TipoEntrata==null){
                     Tipologia=null;
                     CodiceTipologia=null;
                 }else if (TipoEntrata.trim().equalsIgnoreCase("Crypto")){
                     Tipologia="DEPOSITO CRYPTO";
                     CodiceTipologia="DC";
                 }else if (TipoEntrata.trim().equalsIgnoreCase("NFT")){
                     Tipologia="DEPOSITO NFT";
                     CodiceTipologia="DC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("FIAT")){
                     Tipologia="DEPOSITO FIAT";
                     CodiceTipologia="DF";
                    }
                }
             else if(TipoUscita.trim().equalsIgnoreCase("Crypto")){
                 //ora gestisco le varie casistiche se M1 è una crypto
                 if(TipoEntrata==null){
                     Tipologia="PRELIEVO CRYPTO";
                     CodiceTipologia="PC";
                 }else if (TipoEntrata.trim().equalsIgnoreCase("Crypto")){
                     Tipologia="SCAMBIO CRYPTO";
                     CodiceTipologia="SC";
                 }else if (TipoEntrata.trim().equalsIgnoreCase("NFT")){
                     Tipologia="ACQUISTO NFT";
                     CodiceTipologia="SC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("FIAT")){
                     Tipologia="VENDITA CRYPTO";
                     CodiceTipologia="VC";
                    }
                }
             else if(TipoUscita.trim().equalsIgnoreCase("NFT")){
                 //ora gestisco le varie casistiche se M1 è una crypto
                 if(TipoEntrata==null){
                     Tipologia="PRELIEVO NFT";
                     CodiceTipologia="PC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("Crypto")){
                     Tipologia="VENDITA NFT";
                     CodiceTipologia="SC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("NFT")){
                     Tipologia="SCAMBIO NFT";
                     CodiceTipologia="SC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("FIAT")){
                     Tipologia="VENDITA NFT";
                     CodiceTipologia="VC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                    }
                }
             else if(TipoUscita.trim().equalsIgnoreCase("FIAT")){
                 //ora gestisco le varie casistiche se M1 è una crypto
                 if(TipoEntrata==null){
                     Tipologia="PRELIEVO FIAT";
                     CodiceTipologia="PF";
                 }else if (TipoEntrata.trim().equalsIgnoreCase("Crypto")){
                     Tipologia="ACQUISTO CRYPTO";
                     CodiceTipologia="AC";
                 }else if (TipoEntrata.trim().equalsIgnoreCase("NFT")){
                     Tipologia="ACQUISTO NFT";
                     CodiceTipologia="AC";//Mantengo la stessa nomenclatura che per i depositi crypto altrimenti ho troppe varianti da gestire
                 }else if (TipoEntrata.trim().equalsIgnoreCase("FIAT")){
                     Tipologia="SCAMBIO FIAT";
                     CodiceTipologia="SF";
                    }
                }
             if (valore==1){
             return Tipologia;}
             else return CodiceTipologia;
         }
    
    
    
    
        
    /* public static Map<String,TransazioneDefi> RitornaTransazioniBSCold( List<String> Portafogli,Component c,Download progressb)
         {   
            String apiKey="6qoE9xw4fDYlEx4DSjgFN0+B5Bk8LCJ9/R+vNblrgiyVyJsMyAhhjPn8BWAi4LM6";
            String vespa=vespa(apiKey,"paperino");
            progressb.setDefaultCloseOperation(0);            
            progressb.Titolo("Importazione da rete BSC");
            progressb.SetMassimo(Portafogli.size()*4);
            AzzeraContatori(); 
            Map<String, TransazioneDefi> MappaTransazioniDefi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            int ava=0;
            for (String wallets:Portafogli){
                String walletAddress=wallets.split(";")[0];
                String Blocco=wallets.split(";")[1];
                Blocco = String.valueOf(Integer.parseInt(Blocco)+1);
 
            progressb.SetLabel("Scaricamento transazioni da "+walletAddress+" in corso...");
            

        try {
            
           // MappaTransazioniDefi.clear();
            
            //PARTE 1 : Recupero la lista delle transazioni
            
            if (progressb.FineThread()) {
                return null;
            }
            URL url = new URI("https://api.bscscan.com/api?module=account&action=txlist&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey=" + vespa).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            int status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
                              if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
           // System.out.println(response);
         
            JSONArray transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
               // System.out.println(transaction.toString());
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String Data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                   //     System.out.println("arghhhhh "+hash);
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                trans.Rete="BSC";
                trans.Blocco=transaction.getString("blockNumber");
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;                
                trans.MonetaCommissioni="BNB";
                trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                trans.Wallet=walletAddress;
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                trans.TipoTransazione=transaction.getString("functionName");
                if (!value.equalsIgnoreCase("0")){
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete("BNB", "BNB", "BNB", AddressNoWallet, qta,"Crypto");
               
                }
            }
            ava++;
             progressb.SetAvanzamento(ava);
            TimeUnit.SECONDS.sleep(3);
  
            
          
                        //PARTE 2: Recupero la lista delle transazioni dei token bsc20   
                           if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=tokentx&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey="+vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
               if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                //System.out.println("sono qui");
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
            //    System.out.println(transaction.toString());
                String tokenSymbol=transaction.getString("tokenSymbol");
                String tokenName=transaction.getString("tokenName");
                String Data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String tokenAddress=transaction.getString("contractAddress");
                String tokenDecimal=transaction.getString("tokenDecimal");
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                    trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";
               // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta,"Crypto");                   
         }             
            ava++;
             progressb.SetAvanzamento(ava);  
          TimeUnit.SECONDS.sleep(3); 
            
            
          
          
           //PARTE 3: Recupero la lista delle transazioni dei token erc721 (NFT)  
                           if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=tokennfttx&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey="+vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
               if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
                            if (progressb.FineThread()) {
                return null;
            }
                //System.out.println("sono qui");
                String AddressNoWallet;
                String qta;
                JSONObject transaction = transactions.getJSONObject(i);
            //    System.out.println(transaction.toString());
                String tokenSymbol=transaction.getString("tokenID");
                String tokenName=transaction.getString("tokenName");
                String Data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String tokenAddress=transaction.getString("contractAddress");
               // String tokenDecimal=transaction.getString("tokenDecimal");
                String hash = transaction.getString("hash");
                String from = transaction.getString("from");
                String to = transaction.getString("to");
              //  String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                 String value = "1";
                TransazioneDefi trans;
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                    trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else {
                        AddressNoWallet=from;
                        qta=value;
                    }
                
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;//Da modificare con data e ora reale
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";
               // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                BigDecimal gasUsed=new BigDecimal (transaction.getString("gasUsed"));
                BigDecimal gasPrice=new BigDecimal (transaction.getString("gasPrice"));
                String qtaCommissione=gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                trans.QtaCommissioni="-"+qtaCommissione;
                progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta,"NFT");                   
         }             
            ava++;
             progressb.SetAvanzamento(ava);  
          TimeUnit.SECONDS.sleep(3); 
          
          
            
            
            //PARTE 4: Recupero delle transazioni interne
                            if (progressb.FineThread()){
                    //se è stato interrotta la finestra di progresso interrompo il ciclo
                 //   progressb.ChiudiFinestra();
                    return null;
                }
            url=new URI("https://api.bscscan.com/api?module=account&action=txlistinternal&address=" + walletAddress + "&startblock="+Blocco+"&sort=asc" +"&apikey=" + vespa).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            inputLine="";
            response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            jsonObject = new JSONObject(response.toString());
            status = Integer.parseInt(jsonObject.getString("status"));
            if (status==0){
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
               // System.out.println(jsonObject.getString("result"));
                                             if (!jsonObject.getString("message").trim().equalsIgnoreCase("No transactions found"))
                   {
                progressb.ChiudiFinestra();
                JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+jsonObject.getString("message"),
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                return null;
                }
            }
            
            transactions = jsonObject.getJSONArray("result");
            for (int i = 0; i < transactions.length(); i++) {
            if (progressb.FineThread()) {
                return null;
            }
                
                String qta;
                String AddressNoWallet;
                JSONObject transaction = transactions.getJSONObject(i);
                String hash = transaction.getString("hash");
                String Data=OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp"))*1000);
                String from = transaction.getString("from");
                String to = transaction.getString("to");
                String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                TransazioneDefi trans;
                
                if (MappaTransazioniDefi.get(walletAddress+"."+hash)==null){
                    trans=new TransazioneDefi();
                    MappaTransazioniDefi.put(walletAddress+"."+hash, trans);
                }else 
                    {
                    trans=MappaTransazioniDefi.get(walletAddress+"."+hash);
                    }
                trans.Rete="BSC";
                    if (from.equalsIgnoreCase(walletAddress)){
                        AddressNoWallet=to;
                        qta="-"+value;
                    }else{
                        AddressNoWallet=from;
                        qta=value;                      
                    }
                trans.Blocco=transaction.getString("blockNumber");
                trans.Wallet=walletAddress;
                trans.DataOra=Data;
                trans.TimeStamp=transaction.getString("timeStamp");
                trans.HashTransazione=hash;
                
                trans.MonetaCommissioni="BNB";  
                if (trans.QtaCommissioni!=null && new BigDecimal(trans.QtaCommissioni).abs().compareTo(new BigDecimal(qta).abs())==1 &&
                        !(trans.TipoTransazione!=null && trans.TipoTransazione.toLowerCase().contains("swap")&&(trans.RitornaNumeroTokenUscita()==0||trans.RitornaNumeroTokenentrata()==0)))
                    {
                        // se il valore della commissione è maggiore del bnb di ritorno allora lo sottraggo dalle commissioni
                        //anzichè metterlo come importo dei trasferimenti
                        // questo non deve essere fatto però se è uno swap di cui questi bnb sono gli unici in ritorno
                     //questa cosa la devo gestire
                        trans.QtaCommissioni=new BigDecimal(trans.QtaCommissioni).subtract(new BigDecimal(qta)).toPlainString();
                    }
                else {
                    progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del "+Data.split(" ")[0]+" in corso");
                    trans.InserisciMonete("BNB", "BNB", "BNB", AddressNoWallet, qta,"Crypto");
                 //   System.out.println(trans.HashTransazione+ " - "+ qta);
                }

                
              //  System.out.println(value+" - "+hash);

                
            }           
            ava++;
             progressb.SetAvanzamento(ava);
            TimeUnit.SECONDS.sleep(3);
            
            
            
            
                

         //   TimeUnit.SECONDS.sleep(1);
                    
        } catch (MalformedURLException ex) {
            Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null,"AAA"+ ex);
            progressb.dispose();
            Prezzi.ScriviFileConversioneXXXEUR();
            JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(Prezzi.class.getName()).log(Level.SEVERE, null,"AAA"+  ex);
            progressb.dispose();
            Prezzi.ScriviFileConversioneXXXEUR();
                        JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        } catch (InterruptedException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null,"AAA"+  ex);
            progressb.dispose();
            Prezzi.ScriviFileConversioneXXXEUR();
                        JOptionPane.showConfirmDialog(c, "Errore durante l'importazione dei dati\n"+ex,
                    "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
            return null;
        }
        
        }
        Prezzi.ScriviFileConversioneXXXEUR();
        return MappaTransazioniDefi;
        }    
    */

     public static Object[] DeFi_RitornaArrayJson(String Dominio,String walletAddress,String Tipo,String BloccoIniziale,String vespa,Component ccc,Download progressb){
         //L'oogetto in ritorno è un array di 2 oggetti
         //il primo è un int che indica il numero di transazioni
         //il secondo è un JsonArray con tutte le transazioni
        JSONArray transactionsArray=new JSONArray();
        Object ritorno[]=new Object[2];
        int numeroTrans=0;
        String BloccoTemp=BloccoIniziale;
        boolean finito=false;

         try {
             while (!finito){//Siccome il limite è di 10000 movimenti se supero quel limite continuo le richieste dall'ultima arrivata
            if (progressb!=null&&progressb.FineThread()) {
                return null;
            }
            //String urls=Dominio+"/api?module=account&action="+Tipo+"&address=" + walletAddress + "&startblock=" + BloccoTemp + "&sort=asc" + "&apikey=" + vespa;
            String urls=Dominio+"&module=account&action="+Tipo+"&address=" + walletAddress + "&startblock=" + BloccoTemp + "&sort=asc" + "&apikey=" + vespa;

            //if (Dominio.contains("cronos.org"))urls=Dominio+"/api?module=account&action="+Tipo+"&address=" + walletAddress + "&startblock=" + BloccoTemp + "&sort=asc";
            //System.out.println(urls);
            System.out.println("Recupero informazioni da Explorer "+Dominio+" relativamente a wallet "+ walletAddress);
            System.out.println("da Blocco : "+BloccoTemp+" relativi a tipologia : "+Tipo);
            URL url = new URI(urls).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder responseTxlist = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseTxlist.append(inputLine);
            }
            in.close();
            String Risposta=responseTxlist.toString();
            //System.out.println(Risposta);
            //Se Risposta contiene "Query Timeout occurred." e la richiesta è per un erc1155
            //significa che l'explorer non lo supporta quindi ritorno il campo vuoto
            if (Tipo.equalsIgnoreCase("token1155tx") && Risposta.contains("Query Timeout occured.")){
                ritorno[0]=0;
                return ritorno;
            }
            //System.out.println(Risposta);
            JSONObject jsonObjectTxlist = new JSONObject(Risposta);
            int status = Integer.parseInt(jsonObjectTxlist.getString("status"));
            //verifico che questa non sia andata in errore, in caso contratrio interrompo l'importazione
            if (status == 0) {
                //in questo caso la richiesta è anda in errore
                //scrivo il messaggio, e chiudo la progress bar
                if (!jsonObjectTxlist.getString("message").trim().equalsIgnoreCase("No transactions found")) {
                    if (progressb!=null)progressb.ChiudiFinestra();
                    JOptionPane.showConfirmDialog(ccc, "Errore durante l'importazione dei dati\n" + jsonObjectTxlist.getString("message")+
                            "\n"+Risposta+"\n"+"Riprovare in un secondo momento, le API dell'Explorer non rispondono correttamente.",
                            "Errore", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null);
                    return null;
                }
            }
            //conto le transazioni
            int numeroTransTemp=0;
            JSONArray transactionsArrayTemp = jsonObjectTxlist.getJSONArray("result");
            for (int i = 0; i < transactionsArrayTemp.length(); i++) {
                JSONObject transaction = transactionsArrayTemp.getJSONObject(i);
                   BloccoTemp=transaction.getString("blockNumber");
                   numeroTrans++;
                   numeroTransTemp++;
            }
            transactionsArray.putAll(transactionsArrayTemp);           
            TimeUnit.SECONDS.sleep(2);
            if (progressb!=null&&progressb.FineThread()) {
                return null;
            }
            if (numeroTransTemp<10000)finito=true;
          }   
        } catch (InterruptedException | URISyntaxException | IOException ex) {
            Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
        }
           ritorno[0]=numeroTrans;
           System.out.println(numeroTrans);
           ritorno[1]=transactionsArray;
           return ritorno; 
     }

     
    public static String GiacenzeCRO_RimanzeBlocco(String Blocco, String walletAddress) {
        //In questa funzione dovrò recuperare le rimanenze CRO del wallet ad un determinato Blocco
        //Questo ci permetterà di sistemare le giacenze dei CRO in maniera esatta anche se porterà via molto tempo.
        String Valore = DatabaseH2.GiacenzeWalletMonetaBlockchain_Leggi(walletAddress + "_CRO_CRO_" + Blocco);
        if (Valore == null)
         try {
            String urls = "https://cronos.org/explorer/api?module=account&action=eth_get_balance&address=" + walletAddress + "&block=" + Blocco;

           // System.out.println(urls);
            System.out.println("Controllo giacenze CRO su blocco "+ Blocco + " per il Wallet "+ walletAddress);
            URL url = new URI(urls).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder responseTxlist = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseTxlist.append(inputLine);
            }
            in.close();
            JSONObject jsonObjectTxlist = new JSONObject(responseTxlist.toString());
            Valore = jsonObjectTxlist.getString("result");
            Valore = (Funzioni.hexToDecimal(Valore)).toString();
            Valore = new BigDecimal(Valore).divide(new BigDecimal("1000000000000000000")).stripTrailingZeros().toPlainString();
            DatabaseH2.GiacenzeWalletMonetaBlockchain_Scrivi(walletAddress + "_CRO_CRO_" + Blocco, Valore);
            TimeUnit.SECONDS.sleep(2);

        } catch (InterruptedException | URISyntaxException | IOException ex) {
            // Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        return Valore;
    }
    
    public static String DeFi_GiacenzeL1_Rimanze(String walletAddress,String Rete) {
        //In questa funzione dovrò recuperare le rimanenze CRO del wallet ad un determinato Blocco
        //Questo ci permetterà di sistemare le giacenze dei CRO in maniera esatta anche se porterà via molto tempo.
        String Valore = null;
         try {
             if (Funzioni.isApiKeyValidaEtherscan(DatabaseH2.Opzioni_Leggi("ApiKey_Etherscan"))) {
            String apiKey = DatabaseH2.Opzioni_Leggi("ApiKey_Etherscan");
            String Indirizzo = CDC_Grafica.Mappa_ChainExplorer.get(Rete)[0];
            String MonetaRete = CDC_Grafica.Mappa_ChainExplorer.get(Rete)[2];
            //String vespa = vespa(apiKey, "paperino");
             
           // String urls = Indirizzo+"/api?module=account&action=balance&address="+walletAddress+"&apikey="+vespa;
            String urls = Indirizzo+"&module=account&action=balance&address="+walletAddress+"&apikey="+apiKey;
            
            System.out.println("Controllo giacenze "+MonetaRete+" per il Wallet "+ walletAddress);
            URL url = new URI(urls).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder responseTxlist = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseTxlist.append(inputLine);
            }
            in.close();
            JSONObject jsonObjectTxlist = new JSONObject(responseTxlist.toString());
            Valore = jsonObjectTxlist.getString("result");
            if (!Funzioni.Funzioni_isNumeric(Valore, false))
                {
                    System.out.println("Controllo giacenze non riuscito per il token "+MonetaRete+" sul Wallet "+walletAddress+ ". Riprovare in un secondo momento");
                    System.out.println("Errore : '"+Valore+"'");
                    return null;
                }
            //Valore = (Funzioni.hexToDecimal(Valore)).toString();
            //System.out.println(Valore);
            Valore = new BigDecimal(Valore).divide(new BigDecimal("1000000000000000000")).stripTrailingZeros().toPlainString();
            TimeUnit.SECONDS.sleep(2);
             }

        } catch (InterruptedException | URISyntaxException | IOException ex) {
            // Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        return Valore;
    }
     
     public static String[] GiacenzeCRO_CreaMovCorretivo(String IDrif,BigDecimal QtaTot,BigDecimal QtaVoluta) {
         BigDecimal differenzaQta=QtaVoluta.subtract(QtaTot).stripTrailingZeros();
            
            String MV[]=MappaCryptoWallet.get(IDrif);
            String IDSplit[]=MV[0].split("_");
            String RT[]=null;
         if (differenzaQta.compareTo(new BigDecimal(0))==1){
             //devo creare un movimento di deposito peri a differenzaQta 
             RT=new String[ColonneTabella];
             String ID=IDSplit[0]+"_"+IDSplit[1]+"_."+IDSplit[2]+"_"+IDSplit[3]+"_DC";
             RT[0]=ID;
             RT[1]=MV[1];
             RT[2]="1 di 1";
             RT[3]=MV[3];
             RT[4]=MV[4];
             RT[5]="DEPOSITO CRYPTO";
             RT[11]="CRO";
             RT[12]="Crypto";
             RT[13]=differenzaQta.toPlainString();
             Moneta mon=new Moneta();
             mon.Moneta="CRO";
             mon.MonetaAddress="CRO";
             mon.Qta=differenzaQta.abs().toPlainString();
            // mon.Rete=Funzioni.TrovaReteDaID(IDrif);
             mon.Tipo="Crypto";
             RT[15]=Prezzi.DammiPrezzoTransazione(mon, null, OperazioniSuDate.ConvertiDatainLongMinuto(MV[1]), null, true, 2, "CRO");
             RT[7]="Rettifica Automatica";
             RT[22]="A";
                RT[27]="CRO";
                RT[28]="CRO";
                RT[29]=MV[29];
             Funzioni.RiempiVuotiArray(RT);
  //           MappaCryptoWallet.put(ID, RT);
         }else if (differenzaQta.compareTo(new BigDecimal(0))==-1){
             //devo creare un movimento di prelievo pari a differenzaQta
             RT=new String[ColonneTabella];
             String ID=IDSplit[0]+"_"+IDSplit[1]+"_."+IDSplit[2]+"_"+IDSplit[3]+"_PC";
             RT[0]=ID;
             RT[1]=MV[1];
             RT[2]="1 di 1";
             RT[3]=MV[3];
             RT[4]=MV[4];
             RT[5]="PRELIEVO CRYPTO";
             RT[8]="CRO";
             RT[9]="Crypto";
             RT[10]=differenzaQta.toPlainString();
             Moneta mon=new Moneta();
             mon.Moneta="CRO";
             mon.MonetaAddress="CRO";
             mon.Qta=differenzaQta.abs().toPlainString();
             //mon.Rete="CRO";
             mon.Tipo="Crypto";
             RT[15]=Prezzi.DammiPrezzoTransazione(mon, null, OperazioniSuDate.ConvertiDatainLongMinuto(MV[1]), null, true, 2, "CRO");
             RT[7]="Rettifica Automatica";
             RT[22]="A";
                RT[25]="CRO";
                RT[26]="CRO";
                RT[29]=MV[29];
                
            if (differenzaQta.abs().compareTo(new BigDecimal(2))==-1){
            //se la differenza è inferiore a 2 CRO considero il movimento come una commissione
                RT[18]="PCO - Commissione";
                RT[5]="COMMISSIONI";
                }
             Funzioni.RiempiVuotiArray(RT);
 //            MappaCryptoWallet.put(ID, RT);
             
         }
         //System.out.println(RT[0]);
         return RT;
     }
        public static String GiacenzeCRO_Sistema(String Wallet,Component ccc,Download progressb) {
        
            progressb.setDefaultCloseOperation(0);
            progressb.Titolo("Sistemazione Giacenze CRO");
            progressb.SetLabel("La funzione potrebbe durare parecchi minuti");
            progressb.SetMassimo(MappaCryptoWallet.size());
            progressb.avanzamento=0;
            int avanzamento=0;
            BigDecimal TotaleQta = new BigDecimal(0);
            String UltimoBlocco="";
            String PrimaTransBlocco="";
            List<String[]> RigheTabella=new ArrayList<>();
            List<String[]> MovDaAggiungere=new ArrayList<>();
            List<String> MovDaEliminare=new ArrayList<>();
            for (String[] movimento : MappaCryptoWallet.values()) {
                progressb.SetAvanzamento(avanzamento);
                avanzamento++;
                if (progressb.FineThread()) {
                        //thread.join();
                        return null;

                }
                
               // long DataMovimento = OperazioniSuDate.ConvertiDatainLong(movimento[1]);
                        String AddressU = movimento[26];
                        String AddressE = movimento[28];
                        String WalletRiga=movimento[3];
                      //  System.out.println(movimento[3].trim());
                    // adesso verifico il wallet
                    //Deve essere lo stesso wallet
                    //Deve chiamarsi CRO Transaction, per lo meno per il momento
                    //La moneta movimentata deve essere CRO
                    //Devo avere il numero di blocco
                    //poi penso cambierò il nome del wallet per la defi, credo li chiamerò Wallet o qualcosa del tipo DEFI Principale
                    if (Wallet.equalsIgnoreCase(WalletRiga) && movimento[4].trim().equalsIgnoreCase("Wallet")) {
                       // System.out.println("Sono qui");
                        if (AddressU.equalsIgnoreCase("CRO")||AddressE.equalsIgnoreCase("CRO")) {
                            
                            //Prima di iniziare il controllo delle giacenze e sistemara la qta di CRO verifico se trovo dei trasferimenti di CRO da un certo Address
                            //e li trasformo in uno scambio WCRO - CRO
                           

                               

                                //Se non è un movimento in defi non gestisco nulla perchè non ho le bsi per farlo e devo gestirlo a mano
                                if (movimento[18].equalsIgnoreCase("")
                                        &&//movimento non classificato e in defi
                                        movimento[0].split("_")[4].equals("DC") && movimento[11].equals("CRO")
                                        &&//movimento di deposito di CRO
                                        movimento[7].trim().equalsIgnoreCase("withdraw")
                                        &&//Classificato come withdraw
                                        movimento[30].equalsIgnoreCase("0x5c7f8a570d578ed84e63fdfa7b1ee72deae1ae23")
                                        &&//Arriva da contratto WCRO
                                        Funzioni.TrovaReteDaID(movimento[0]).equalsIgnoreCase("CRO")) //Rete Cronos
                                {
                                    //Creo un movimento di uscita di WCRO che poi verrà trasformato in scambio differito dal sistema
                                    String MT[] = new String[Importazioni.ColonneTabella];
                                    String IDSpezzato[] = movimento[0].split("_");
                                    String IDNuovoMov = IDSpezzato[0] + "_" + IDSpezzato[1] + "_" + IDSpezzato[2] + "_" + IDSpezzato[3] + "_SC";
                                    MT[0] = IDNuovoMov;
                                    MT[1] = movimento[1];
                                    MT[2] = "1 di 1";
                                    MT[3] = movimento[3];
                                    MT[4] = movimento[4];
                                    MT[5] = "SCAMBIO CRYPTO";
                                    MT[6] = "WCRO -> CRO";
                                    MT[8] = "WCRO";
                                    MT[9] = movimento[12];
                                    MT[10] = new BigDecimal(movimento[13]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
                                    MT[11] = movimento[11];
                                    MT[12] = movimento[12];
                                    MT[13] = movimento[13];
                                    MT[15] = movimento[15];
                                    MT[22] = "A";
                                    MT[23] = movimento[23];
                                    MT[24] = movimento[24];
                                    MT[25] = "WCRO";
                                    MT[26] = "0x5c7f8a570d578ed84e63fdfa7b1ee72deae1ae23";
                                    MT[27] = movimento[27];
                                    MT[28] = movimento[28];
                                    MT[29] = movimento[29];
                                    MT[30] = movimento[30];
                                    Importazioni.RiempiVuotiArray(MT);
                                    MovDaAggiungere.add(MT);
                                    MovDaEliminare.add(movimento[0]);
                                   // MappaCryptoWallet.put(IDNuovoMov, MT);
                                   // ClassificazioneTrasf_Modifica.CreaMovimentiScambioCryptoDifferito(IDNuovoMov, movimento[0]);
                                    // System.out.println("Trovato scambio con WCRO");
                                
                                }
                            
                            
                            
                            
                            
                            
                            
                            
                            
                            
                            
                            if (!movimento[23].equals(UltimoBlocco)&&!UltimoBlocco.isBlank()){
                                //Se il blocco che sto analizzando è diverso dal blocco precedente allora posso fare le verifiche sulla giacenza del blocco precedente e sistemare le cose
                                
                                // System.out.println("Sono qui");
                                String rima=GiacenzeCRO_RimanzeBlocco(UltimoBlocco,Wallet.split(" ")[0]);
                                //a questo punto faccio il check e se il risultato è null annullare tutto
                                //questo perchè altrimenti rischio di mandare avanti un conto sbagliato
                                //Ovviamente annullo mandando fuori un errore
                                if (rima==null){
                                    String testoMessaggio="""
                                                       Errore nello scaricamento delle rimanenze dei singoli blocchi di CRO
                                                       Verra' interrotta l'analisi, riprovare pi\u00f9 tardi""";
                                    JOptionPane.showConfirmDialog(ccc,testoMessaggio,"Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
                                    return null;
                                    
                                }

                                BigDecimal TotaleVoluto=new BigDecimal(rima);
                                if (TotaleVoluto.compareTo(TotaleQta)!=0){//se i 2 totali non corrispondono creo il movimento che sistema le cose                                
                                    String RT[]=GiacenzeCRO_CreaMovCorretivo(PrimaTransBlocco,TotaleQta,TotaleVoluto);
                                    if(RT!=null)RigheTabella.add(RT);
                                    TotaleQta=TotaleVoluto;//A Questo punto il nuovo totale dovrà essere quello voluto
                                    //break;
                                }
     
                                
                               
                               
                                
                            }
                            if (!movimento[23].equals(UltimoBlocco)){
                                PrimaTransBlocco=movimento[0];
                            }
                            //Finite le varie verifiche procedo con la somma e incremento la voce ultimo blocco
                            if (AddressU.equalsIgnoreCase("CRO"))TotaleQta = TotaleQta.add(new BigDecimal(movimento[10])).stripTrailingZeros();
                            if (AddressE.equalsIgnoreCase("CRO")) TotaleQta = TotaleQta.add(new BigDecimal(movimento[13])).stripTrailingZeros();
                            UltimoBlocco=movimento[23];
                        }
                    }
                
            }
            
            //Ora creo tutti i movimenti correttivi che ho messo da parte nella lista
            for (String RT[]:RigheTabella){
                MappaCryptoWallet.put(RT[0], RT);
               // System.out.println("Sto correggendo");
            }
            for (String RT[]:MovDaAggiungere){
                MappaCryptoWallet.put(RT[0], RT);
            }
            for (String id:MovDaEliminare){
                MappaCryptoWallet.remove(id);
            }
            if (!RigheTabella.isEmpty())CDC_Grafica.TabellaCryptodaAggiornare=true;
            if (!MovDaAggiungere.isEmpty())CDC_Grafica.TabellaCryptodaAggiornare=true;
            if (!MovDaEliminare.isEmpty())CDC_Grafica.TabellaCryptodaAggiornare=true;
    return "Ok";
        
    }
        
    public static String DeFi_GiacenzeL1_Sistema(String Wallet, String Rete, Component ccc, Download progressb) {
        //sistemo le giacenze sulle rete ethereum compatibili
        if (!Rete.equals("SOL")){
        progressb.setDefaultCloseOperation(0);
        progressb.Titolo("Sistemazione Giacenze moneta di Scambio su Wallet " + Wallet);
        progressb.SetLabel("Sistemazione Giacenze");
        progressb.SetMassimo(MappaCryptoWallet.size());
        progressb.avanzamento = 0;
        int avanzamento = 0;
        BigDecimal TotaleQta = new BigDecimal(0);
        String IDUltimoMovimento=null;
        List<String[]> RigheTabella = new ArrayList<>();
        String MonetaRete = CDC_Grafica.Mappa_ChainExplorer.get(Rete)[2];
        for (String[] movimento : MappaCryptoWallet.values()) {
            progressb.SetAvanzamento(avanzamento);
            avanzamento++;
            if (progressb.FineThread()) {
                //thread.join();
                return null;

            }

            // long DataMovimento = OperazioniSuDate.ConvertiDatainLong(movimento[1]);
            String AddressU = movimento[26];
            String AddressE = movimento[28];
            String WalletRiga = movimento[3].split("\\(")[0].trim();
            String ReteMov=Funzioni.TrovaReteDaID(movimento[0]);           
            if (Wallet.equalsIgnoreCase(WalletRiga) && movimento[4].trim().equalsIgnoreCase("Wallet")){
                //if (ReteMov==null)System.out.println("ERRORE NEL RECUPERO DELLA RETE : "+movimento[0]);
                if (ReteMov!=null&&ReteMov.equalsIgnoreCase(Rete)) {
                    //Finite le varie verifiche procedo con la somma e incremento la voce ultimo blocco
                    if (AddressU.equalsIgnoreCase(MonetaRete)&&!movimento[8].isBlank()) {
                       /* System.out.println(movimento[0]);
                        System.out.println(movimento[3]+" - " +AddressU+" - "+MonetaRete);
                        System.out.println(movimento[10]+" - "+TotaleQta);
                        System.out.println("----");*/
                        TotaleQta = TotaleQta.add(new BigDecimal(movimento[10])).stripTrailingZeros();
                        IDUltimoMovimento = movimento[0];
                    }
                    if (AddressE.equalsIgnoreCase(MonetaRete)&&!movimento[11].isBlank()) {
                        TotaleQta = TotaleQta.add(new BigDecimal(movimento[13])).stripTrailingZeros();
                        IDUltimoMovimento = movimento[0];
                    }
                }
            }
        }
        String GiacenzaReale=DeFi_GiacenzeL1_Rimanze(Wallet.split(" ")[0],Rete);
        String QtaNuovoMovimento = null;
        if (GiacenzaReale!=null) QtaNuovoMovimento = new BigDecimal(GiacenzaReale).subtract(TotaleQta).stripTrailingZeros().toPlainString();       
        if (QtaNuovoMovimento!=null && IDUltimoMovimento!=null && !QtaNuovoMovimento.equals("0") && QtaNuovoMovimento.contains("-")) {
                        
                                //adesso compilo la parte comune del movimento
                                String RTOri[] = MappaCryptoWallet.get(IDUltimoMovimento);
                                long DataRiferimento = OperazioniSuDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi aggiungo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                long NuovoOrario = Long.parseLong(RTOri[0].split("_")[0]) + 1;
                                String RT[] = new String[ColonneTabella];
                                RT[0] = "";//questo può variare in caso di movimento di commissione per cui lo metto nel capitolo successivo
                                String IDOriSplittato[] = RTOri[0].split("_");
                                for(int ki=1;ki<30;ki++){
                                            RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_PC";
                                            if(MappaCryptoWallet.get(RT[0])==null){
                                               break;
                                            }
                                        }
                                RT[1] = RTOri[1];
                                RT[2] = "1 di 1";
                                RT[3] = RTOri[3];
                                RT[4] = RTOri[4];
                                RT[5] = "COMMISSIONI";
                                RT[18] = "PCO - COMMISSIONE";
                                RT[6] = MonetaRete + " ->";
                                RT[8] = MonetaRete;
                                RT[9] = "Crypto";//da prendere dalla tabella prima
                                RT[10] = QtaNuovoMovimento;
                                Moneta M1 = new Moneta();
                                M1.Moneta = MonetaRete;
                                M1.MonetaAddress = MonetaRete;
                                M1.Qta = QtaNuovoMovimento;
                                M1.Tipo = "Crypto";
                                M1.Rete = Funzioni.TrovaReteDaID(RTOri[0]);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete));
                                /*if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(QtaNuovoMovimento)).setScale(2,RoundingMode.HALF_UP).abs();
                                }*/
                                RT[15] = Prezzo.toPlainString();
                                RT[21] = "Rettifica Giacenza";
                                RT[22] = "M";
                                RT[26] = MonetaRete;
                                RT[29] = RTOri[29];
                                RiempiVuotiArray(RT);
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT[0], RT);
                                CDC_Grafica.TabellaCryptodaAggiornare = true;        
                            
                        
                    } else if (QtaNuovoMovimento!=null && IDUltimoMovimento!=null && !QtaNuovoMovimento.equals("0")){
                        //Gestisco i movimenti di Carico (Depositi)
                                //adesso compilo la parte comune del movimento
                               //System.out.println(IDUltimoMovimento);
                                String RTOri[] = MappaCryptoWallet.get(IDUltimoMovimento);
                                String IDOriSplittato[] = RTOri[0].split("_");
                                long DataRiferimento = OperazioniSuDate.ConvertiDatainLongMinuto(RTOri[1]);
                                //il movimento in questo caso deve finire successivamente a quello selezionato
                                //quindi tolgo 1 secondo al tempo del movimento originale per trovare quello da mettere
                                long NuovoOrario = Long.parseLong(RTOri[0].split("_")[0]) - 1;
                                String RT[] = new String[ColonneTabella];
                                for(int ki=1;ki<30;ki++){
                                            RT[0] = NuovoOrario + "_" + IDOriSplittato[1] + ".Rettifica_1_"+ki+"_DC";
                                            if(MappaCryptoWallet.get(RT[0])==null){
                                               break;
                                            }
                                        }
                                RT[1] = RTOri[1];
                                RT[2] = "1 di 1";
                                RT[3] = RTOri[3];
                                RT[4] = RTOri[4];
                                RT[5] = "DEPOSITO A COSTO 0";
                                RT[18] = "DCZ - DEPOSITO A COSTO 0";
                                RT[6] = " ->" + MonetaRete;
                                RT[11] = MonetaRete;
                                RT[12] = "Crypto";
                                RT[13] = QtaNuovoMovimento;
                                Moneta M1 = new Moneta();
                                M1.Moneta = MonetaRete;
                                M1.MonetaAddress = MonetaRete;
                                M1.Qta = QtaNuovoMovimento;
                                M1.Tipo = "Crypto";
                                M1.Rete = Funzioni.TrovaReteDaID(RTOri[0]);
                                BigDecimal Prezzo=new BigDecimal(Prezzi.DammiPrezzoTransazione(M1, null, DataRiferimento, null, true, 2, M1.Rete));
                                /*if (Prezzo.compareTo(new BigDecimal(0))==0){
                                    Prezzo=ValoreUnitarioToken.multiply(new BigDecimal(SQta)).setScale(2,RoundingMode.HALF_UP).abs();
                                }*/
                                RT[15] = Prezzo.toPlainString();
                                RT[21] = "Rettifica Giacenza";
                                RT[22] = "M";
                                RT[28] = MonetaRete;
                                RT[29] = RTOri[29];
                                RiempiVuotiArray(RT);
                                //Adesso scrivo il movimento
                                MappaCryptoWallet.put(RT[0], RT);
                                CDC_Grafica.TabellaCryptodaAggiornare = true;
                    }
        }
        return "Ok";

    }
      
      
    public static Map<String, TransazioneDefi> DeFi_RitornaTransazioni(List<String> Portafogli, Component ccc, Download progressb) {
        //Portafigli contiene la lista dei portafogli da analizzare e comprende indirizzo,ultimoblocco e rete
        //la mappa seguente va popolata per ogni chain explorer che viene implementato a programma 

        // String apiKey="6qoE9xw4fDYlEx4DSjgFN0+B5Bk8LCJ9/R+vNblrgiyVyJsMyAhhjPn8BWAi4LM6";
        progressb.setDefaultCloseOperation(0);
        progressb.Titolo("Importazione dati da explorer");
//  progressb.RipristinaStdout();
        AzzeraContatori();
        Map<String, TransazioneDefi> MappaTransazioniDefi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int avaTot=0;
        for (String wallets : Portafogli) {
            avaTot++;
            int ava = 0;
            String walletAddress = wallets.split(";")[0];
            progressb.Titolo(avaTot+" di "+Portafogli.size()+" Importazione di " + walletAddress + "da explorer");
            String Blocco = wallets.split(";")[1];
            String Rete = wallets.split(";")[2];
            //In caso di SOL devo passargli l'ultimo blocco disponibile, non il blocco+1
            if (!Rete.equalsIgnoreCase("SOL")) Blocco = String.valueOf(Integer.parseInt(Blocco) + 1);            
            if (Rete.equalsIgnoreCase("SOL")) {

                try {
                    if (Trans_Solana.isApiKeyValida(DatabaseH2.Opzioni_Leggi("ApiKey_Helius"))) {
                        int BloccoSol = Integer.parseInt(Blocco);
                        Map<String, TransazioneDefi> MappaTransazioniDefiSol = Trans_Solana.fetchAndParseTransactions(walletAddress, BloccoSol);
                        if (MappaTransazioniDefiSol != null) {
                            for (TransazioneDefi T : MappaTransazioniDefiSol.values()) {
                                MappaTransazioniDefi.put(walletAddress + "." + T.HashTransazione, T);
                            }
                        }
                    } else {
                        System.out.println("Non possono essere scaricate le transazioni del Wallet Solana " + walletAddress + " per mancaza di ApiKey");
                        System.out.println("Andare nella sezione 'Opzioni' - 'ApiKey' per inserire l'apiKey relativa ad Helius");
                        TimeUnit.SECONDS.sleep(5);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                
                //Inizio
                if (Funzioni.isApiKeyValidaEtherscan(DatabaseH2.Opzioni_Leggi("ApiKey_Etherscan"))) {
                
                
                String apiKey = DatabaseH2.Opzioni_Leggi("ApiKey_Etherscan");
                String Indirizzo = CDC_Grafica.Mappa_ChainExplorer.get(Rete)[0];
                String MonetaRete = CDC_Grafica.Mappa_ChainExplorer.get(Rete)[2];
                //String vespa = vespa(apiKey, "paperino");
                progressb.SetLabel("Scaricamento da " + walletAddress + " ("+Rete+") in corso...");

                //Come prima cosa recupero tutte le risposte per riuscire poi ad avere il numero totale di transazioni da elaborare
                //e popolare correttamente la progressbar
                progressb.SetAvanzamento(0);
                progressb.SetMassimo(4);
                int numeroTrans = 0;

                //PARTE 1 : Recupero la lista delle transazioni
                progressb.SetMessaggioAvanzamento("Preparazione fase 1 di 5");
                Object Risposta[] = DeFi_RitornaArrayJson(Indirizzo, walletAddress, "txlist", Blocco, apiKey, ccc, progressb);
                if (Risposta == null) {
                    return null;//se in errore termino il ciclo
                }
                JSONArray transactionsTxlist = (JSONArray) Risposta[1];
                numeroTrans = numeroTrans + (int) Risposta[0];

                //PARTE 2  : Recupero la lista delle transazioni dei token bsc20 
                progressb.SetMessaggioAvanzamento("Preparazione fase 2 di 5");
                Risposta = DeFi_RitornaArrayJson(Indirizzo, walletAddress, "tokentx", Blocco, apiKey, ccc, progressb);
                if (Risposta == null) {
                    return null;//se in errore termino il ciclo
                }
                JSONArray transactionsTokentx = (JSONArray) Risposta[1];
                numeroTrans = numeroTrans + (int) Risposta[0];

                //PARTE 3: Recupero la lista delle transazioni dei token erc721 (NFT) 
                progressb.SetMessaggioAvanzamento("Preparazione fase 3 di 5");
                Risposta = DeFi_RitornaArrayJson(Indirizzo, walletAddress, "tokennfttx", Blocco, apiKey, ccc, progressb);
                if (Risposta == null) {
                    return null;//se in errore termino il ciclo
                }
                JSONArray transactionsTokenntfttx = (JSONArray) Risposta[1];
                numeroTrans = numeroTrans + (int) Risposta[0];
                
                //PARTE 4: Recupero la lista delle transazioni dei token erc1155 
                progressb.SetMessaggioAvanzamento("Preparazione fase 4 di 5");
                Risposta = DeFi_RitornaArrayJson(Indirizzo, walletAddress, "token1155tx", Blocco, apiKey, ccc, progressb);
                if (Risposta == null) {
                    return null;//se in errore termino il ciclo
                }
                JSONArray transactionsTokenERC1155 = (JSONArray) Risposta[1];
                numeroTrans = numeroTrans + (int) Risposta[0];

                //PARTE 5: Recupero delle transazioni interne
                progressb.SetMessaggioAvanzamento("Preparazione fase 5 di 5");
                Risposta = DeFi_RitornaArrayJson(Indirizzo, walletAddress, "txlistinternal", Blocco, apiKey, ccc, progressb);
                if (Risposta == null) {
                    return null;//se in errore termino il ciclo
                }
                JSONArray transactionsTxlistinternal = (JSONArray) Risposta[1];
                numeroTrans = numeroTrans + (int) Risposta[0];

                //   System.out.println(numeroTransTemp);
                progressb.SetMassimo(numeroTrans);
                // return null;

// PARTE B1:  ANALIZZO I RISULTATO DELLA PARTE 1 E SCRIVO I DATI 
                for (int i = 0; i < transactionsTxlist.length(); i++) {
                    if (progressb.FineThread()) {
                        return null;
                    }
                    String AddressNoWallet;
                    String qta;
                    JSONObject transaction = transactionsTxlist.getJSONObject(i);
                    // System.out.println(transaction.toString());
                    String hash = transaction.getString("hash");
                    String from = transaction.getString("from");

                    String to = transaction.getString("to");
                    String Data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp")) * 1000);
                    String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                    TransazioneDefi trans;
                    if (MappaTransazioniDefi.get(walletAddress + "." + hash) == null) {
                        trans = new TransazioneDefi();
                        MappaTransazioniDefi.put(walletAddress + "." + hash, trans);
                    } else {
                        trans = MappaTransazioniDefi.get(walletAddress + "." + hash);
                    }
                    trans.Rete = Rete;
                    trans.Blocco = transaction.getString("blockNumber");
                    trans.DataOra = Data;//Da modificare con data e ora reale
                    trans.TimeStamp = transaction.getString("timeStamp");
                    trans.HashTransazione = hash;
                    trans.MonetaCommissioni = MonetaRete;
                    trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                    trans.Wallet = walletAddress;
                    BigDecimal gasUsed = new BigDecimal(transaction.getString("gasUsed"));
                    BigDecimal gasPrice = new BigDecimal(transaction.getString("gasPrice"));
                    String qtaCommissione = gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                    trans.QtaCommissioni = null;
                    //trans.QtaCommissioni = "-" + qtaCommissione;
                    trans.TipoTransazione = transaction.getString("functionName");
                    if (!value.equalsIgnoreCase("0")) {
                        if (from.equalsIgnoreCase(walletAddress)) {
                            AddressNoWallet = to;
                            qta = "-" + value;
                        } else {
                            AddressNoWallet = from;
                            qta = value;
                        }
                        progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del " + Data.split(" ")[0] + " in corso");
                        trans.InserisciMonete(MonetaRete, MonetaRete, MonetaRete, AddressNoWallet, qta, "Crypto");

                    }
                    if (from.equalsIgnoreCase(walletAddress)) {
                        //le commissioni le ho solo quando è il mio wallet che chiama la transazione
                        trans.QtaCommissioni = "-" + qtaCommissione;
                    }

                    //System.out.println(from + " - "+hash+" - B1 - "+trans.QtaCommissioni );
                    ava++;
                    progressb.SetAvanzamento(ava);
                }

                
                //PARTE B2: Recupero la lista delle transazioni dei token bsc20   
                for (int i = 0; i < transactionsTokentx.length(); i++) {
                    if (progressb.FineThread()) {
                        return null;
                    }
                    //System.out.println("sono qui");
                    String AddressNoWallet;
                    String qta;
                    JSONObject transaction = transactionsTokentx.getJSONObject(i);
                    //    System.out.println(transaction.toString());
                    String tokenSymbol = transaction.getString("tokenSymbol");
                    String tokenName = transaction.getString("tokenName");
                    String Data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp")) * 1000);
                    String tokenAddress = transaction.getString("contractAddress");
                    String tokenDecimal = transaction.getString("tokenDecimal");
                    String hash = transaction.getString("hash");
                    String from = transaction.getString("from");
                    //System.out.println(from + " - "+hash+" B2");
                    String to = transaction.getString("to");
                    String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-" + tokenDecimal)).stripTrailingZeros().toPlainString();
                    TransazioneDefi trans;
                    if (MappaTransazioniDefi.get(walletAddress + "." + hash) == null) {
                        trans = new TransazioneDefi();
                        MappaTransazioniDefi.put(walletAddress + "." + hash, trans);
                    } else {
                        trans = MappaTransazioniDefi.get(walletAddress + "." + hash);
                    }
                    trans.Rete = Rete;

                    trans.Blocco = transaction.getString("blockNumber");
                    trans.Wallet = walletAddress;
                    trans.DataOra = Data;//Da modificare con data e ora reale
                    trans.TimeStamp = transaction.getString("timeStamp");
                    trans.HashTransazione = hash;

                    trans.MonetaCommissioni = MonetaRete;
                    // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
    //                BigDecimal gasUsed = new BigDecimal(transaction.getString("gasUsed"));
    //                BigDecimal gasPrice = new BigDecimal(transaction.getString("gasPrice"));
                    //in teoria le commissioni non serve prenderle da qua perchè le ho già prese al punto B1
                    //String qtaCommissione = gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();

                    if (from.equalsIgnoreCase(walletAddress)) {
                        //trans.QtaCommissioni = "-" + qtaCommissione;
                        AddressNoWallet = to;
                        qta = "-" + value;
                    } else {
                        AddressNoWallet = from;
                        qta = value;
                    }
                    progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del " + Data.split(" ")[0] + " in corso");
                    trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta, "Crypto");

                    // System.out.println(tokenSymbol+" - "+qta);
                    //System.out.println(from + " - "+hash+" - B2 - "+trans.QtaCommissioni );
                    ava++;
                    progressb.SetAvanzamento(ava);
                }
                
                

                //PARTE B3: Recupero la lista delle transazioni dei token erc721 (NFT)  
                for (int i = 0; i < transactionsTokenntfttx.length(); i++) {
                    if (progressb.FineThread()) {
                        return null;
                    }
                    //System.out.println("sono qui");
                    String AddressNoWallet;
                    String qta;
                    JSONObject transaction = transactionsTokenntfttx.getJSONObject(i);
                    //    System.out.println(transaction.toString());
                    String tokenSymbol = transaction.getString("tokenID");
                    String tokenName = transaction.getString("tokenName");
                    String Data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp")) * 1000);
                    String tokenAddress = transaction.getString("contractAddress");
                    // String tokenDecimal=transaction.getString("tokenDecimal");
                    String hash = transaction.getString("hash");
                    String from = transaction.getString("from");
                    //System.out.println(from + " - "+hash+" B3");
                    String to = transaction.getString("to");
                    //  String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-"+tokenDecimal)).stripTrailingZeros().toPlainString();
                    String value = "1";
                    TransazioneDefi trans;
                    if (MappaTransazioniDefi.get(walletAddress + "." + hash) == null) {
                        trans = new TransazioneDefi();
                        MappaTransazioniDefi.put(walletAddress + "." + hash, trans);
                    } else {
                        trans = MappaTransazioniDefi.get(walletAddress + "." + hash);
                    }
                    trans.Rete = Rete;

                    trans.Blocco = transaction.getString("blockNumber");
                    trans.Wallet = walletAddress;
                    trans.DataOra = Data;//Da modificare con data e ora reale
                    trans.TimeStamp = transaction.getString("timeStamp");
                    trans.HashTransazione = hash;

                    trans.MonetaCommissioni = MonetaRete;
                    // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
    //                BigDecimal gasUsed = new BigDecimal(transaction.getString("gasUsed"));
    //                BigDecimal gasPrice = new BigDecimal(transaction.getString("gasPrice"));
                    //in teoria le commissioni non serve prenderle da qua perchè le ho già prese al punto B1
                    //String qtaCommissione = gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                    //trans.QtaCommissioni=null;
                    if (from.equalsIgnoreCase(walletAddress)) {
                        //trans.QtaCommissioni = "-" + qtaCommissione;
                        AddressNoWallet = to;
                        qta = "-" + value;
                    } else {
                        AddressNoWallet = from;
                        qta = value;
                    }
                    progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del " + Data.split(" ")[0] + " in corso");
                    trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta, "NFT");
                    // System.out.println(from + " - "+hash+" - B3 - "+trans.QtaCommissioni );
                    ava++;
                    progressb.SetAvanzamento(ava);
                }
                
                //PARTE B4: Recupero la lista delle transazioni dei token ERC1155
                //transactionsTokenERC1155 potrebbe essere null in caso non sia supportato dall'explorer
                //in quel caso mi faccio spedire un json null
                    if (transactionsTokenERC1155 != null) {
                        for (int i = 0; i < transactionsTokenERC1155.length(); i++) {
                            if (progressb.FineThread()) {
                                return null;
                            }
                            //System.out.println("sono qui");
                            String AddressNoWallet;
                            String qta;
                            JSONObject transaction = transactionsTokenERC1155.getJSONObject(i);
                            //    System.out.println(transaction.toString());
                            String tokenSymbol = transaction.getString("tokenSymbol");
                            String tokenName = transaction.getString("tokenName");
                            String Data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp")) * 1000);
                            String tokenAddress = transaction.getString("contractAddress");
                            String tokenDecimal;
                            String value = "0";
                            String Tipo = "Crypto";
                            if (transaction.has("tokenDecimal")) {
                                tokenDecimal = transaction.getString("tokenDecimal");
                                value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-" + tokenDecimal)).stripTrailingZeros().toPlainString();

                            }
                            String tokenValue;
                            if (transaction.has("tokenValue")) {
                                tokenValue = transaction.getString("tokenValue");
                                value = tokenValue;
                                Tipo = "NFT";
                            }
                            String hash = transaction.getString("hash");
                            String from = transaction.getString("from");
                            //System.out.println(from + " - "+hash+" B2");
                            String to = transaction.getString("to");
                            TransazioneDefi trans;
                            if (MappaTransazioniDefi.get(walletAddress + "." + hash) == null) {
                                trans = new TransazioneDefi();
                                MappaTransazioniDefi.put(walletAddress + "." + hash, trans);
                            } else {
                                trans = MappaTransazioniDefi.get(walletAddress + "." + hash);
                            }
                            trans.Rete = Rete;

                            trans.Blocco = transaction.getString("blockNumber");
                            trans.Wallet = walletAddress;
                            trans.DataOra = Data;//Da modificare con data e ora reale
                            trans.TimeStamp = transaction.getString("timeStamp");
                            trans.HashTransazione = hash;

                            trans.MonetaCommissioni = MonetaRete;
                            // trans.TransazioneOK = transaction.getString("isError").equalsIgnoreCase("0");
                            //                BigDecimal gasUsed = new BigDecimal(transaction.getString("gasUsed"));
                            //                BigDecimal gasPrice = new BigDecimal(transaction.getString("gasPrice"));
                            //in teoria le commissioni non serve prenderle da qua perchè le ho già prese al punto B1
                            //String qtaCommissione = gasUsed.multiply(gasPrice).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();

                            if (from.equalsIgnoreCase(walletAddress)) {
                                //trans.QtaCommissioni = "-" + qtaCommissione;
                                AddressNoWallet = to;
                                qta = "-" + value;
                            } else {
                                AddressNoWallet = from;
                                qta = value;
                            }
                            progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del " + Data.split(" ")[0] + " in corso");
                            trans.InserisciMonete(tokenSymbol, tokenName, tokenAddress, AddressNoWallet, qta, Tipo);

                            // System.out.println(tokenSymbol+" - "+qta);
                            //System.out.println(from + " - "+hash+" - B2 - "+trans.QtaCommissioni );
                            ava++;
                            progressb.SetAvanzamento(ava);
                        }
                    }
                
                

                //PARTE B5: Recupero delle transazioni interne
                for (int i = 0; i < transactionsTxlistinternal.length(); i++) {
                    if (progressb.FineThread()) {
                        return null;
                    }

                    String qta;
                    String AddressNoWallet;
                    JSONObject transaction = transactionsTxlistinternal.getJSONObject(i);
                    String hash = transaction.getString("hash");
                    //      String hash =transaction.getString("transactionHash");
                    String Data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(Long.parseLong(transaction.getString("timeStamp")) * 1000);
                    String from = transaction.getString("from");
                    //System.out.println(from + " - "+hash+" B4");
                    String to = transaction.getString("to");
                    String value = new BigDecimal(transaction.getString("value")).multiply(new BigDecimal("1e-18")).stripTrailingZeros().toPlainString();
                    TransazioneDefi trans;

                    if (MappaTransazioniDefi.get(walletAddress + "." + hash) == null) {
                        trans = new TransazioneDefi();
                        MappaTransazioniDefi.put(walletAddress + "." + hash, trans);
                    } else {
                        trans = MappaTransazioniDefi.get(walletAddress + "." + hash);
                    }
                    trans.Rete = Rete;
                    if (from.equalsIgnoreCase(walletAddress)) {
                        AddressNoWallet = to;
                        qta = "-" + value;
                    } else {
                        AddressNoWallet = from;
                        qta = value;
                    }
                    trans.Blocco = transaction.getString("blockNumber");
                    trans.Wallet = walletAddress;
                    trans.DataOra = Data;
                    trans.TimeStamp = transaction.getString("timeStamp");
                    trans.HashTransazione = hash;

                    trans.MonetaCommissioni = MonetaRete;
                    if (trans.QtaCommissioni != null && new BigDecimal(trans.QtaCommissioni).abs().compareTo(new BigDecimal(qta).abs()) == 1
                            && !(trans.TipoTransazione != null && trans.TipoTransazione.toLowerCase().contains("swap") && (trans.RitornaNumeroTokenUscita() > 0 && trans.RitornaNumeroTokenentrata() > 0))) {
                        // se il valore della commissione è maggiore del bnb di ritorno allora lo sottraggo dalle commissioni
                        //anzichè metterlo come importo dei trasferimenti
                        // questo non deve essere fatto però se è uno swap di cui questi bnb sono gli unici in ritorno
                        //questa cosa la devo gestire
                        //System.out.println(trans.QtaCommissioni +" ---- "+qta);
                        //faccio somma e non sottrazione perchè le commissioni sono già negative
                        trans.QtaCommissioni = new BigDecimal(trans.QtaCommissioni).add(new BigDecimal(qta)).toPlainString();
                    } else {
                        progressb.SetMessaggioAvanzamento("Scaricamento Prezzi del " + Data.split(" ")[0] + " in corso");
                        trans.InserisciMonete(MonetaRete, MonetaRete, MonetaRete, AddressNoWallet, qta, "Crypto");
                        //   System.out.println(trans.HashTransazione+ " - "+ qta);
                    }

                    //  System.out.println(value+" - "+hash);
                    //System.out.println(from + " - "+hash+" - B4 - "+trans.QtaCommissioni );
                    ava++;
                    progressb.SetAvanzamento(ava);
                }

                //   TimeUnit.SECONDS.sleep(1);
                //Fine
                } else {
                        System.out.println("Non possono essere scaricate le transazioni del Wallet " + walletAddress + " per mancaza di ApiKey");
                        System.out.println("Andare nella sezione 'Opzioni' - 'ApiKey' per inserire l'apiKey relativa ad Etherscan");
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Importazioni.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    }
            }
        }
//        Prezzi.ScriviFileConversioneXXXEUR();
        return MappaTransazioniDefi;
    }

    
    
    
        public static void ConvertiScambiLPinDepositiPrelievi(){
        //Ci sono alcuni depositi su piattaforma defi che nel momento in cui fai il deposito
        //ad esempio di un token lp sulla piattaforma poi ti restituiscono le reward accumulate fino a quel momento in altra moneta
        //Questo fa si che il programma identifichi il movimento come uno scambio invece che come un deposito con una reward in rientro
        //Questa funzione cancellerà il movimento di scambio e lo trasformerà in un prelievo/deposito
        //Dove il prelievo viene fatto dal wallet verso una piattaforma defi
        //mentre il deposito sul wallet verrà fatto figurare come una reward come è giusto che sia
        
        //Le conzioni perchè questo avvenga sono:
        //Trovo il movimento di scambio crypto-crypto
        //il token in uscita è un token LP
        //il movimento viene classificato come "deposit" sulla defi
        
        //Cosa devo fare se le condizioni sono soddisfatte:
        //Genero un movimento di prelievo del token LP
        //Genero movimento di deposito dell'altro token
        //Classifico il movimento di deposito come reward
        //Cancello il vecchio movimento
        
        //Ovviamente tutto questo lo devo fare su tutto il database
        //
        //Questa funzione bisogna che vengaeseguita ad ogni fine importazione da defi
        //Per Sistemare il pregresso invece bisognerà per il momento almeno prima del primo caricamento della tabella DepositiPrelievi
        //Non lo farei fare ad ogni caricmaneto perchè diventa pesante
        List<String> Trovati = new ArrayList<>();
        
        for (String Movimento[]:MappaCryptoWallet.values()){
            String Rete=Funzioni.TrovaReteDaID(Movimento[0]);
            if(Movimento[0].split("_")[4].equalsIgnoreCase("SC")&&
                    Movimento[8].contains("-LP")&&
                    Rete!=null&&
                    Movimento[7].trim().equalsIgnoreCase("deposit")){
                //Condizione tovata
                //Faccio la lista degli id da modificare
                Trovati.add(Movimento[0]);
                }}
            for(String idmov:Trovati){
//1 - Genero il movimento di Prelievo
                String Movimento[]=MappaCryptoWallet.get(idmov);
                String RT[]=new String[ColonneTabella];
                System.arraycopy(Movimento, 0, RT, 0, ColonneTabella); //ricopio tutti i movimenti
                //adesso modifico quelli che mi interessano
                String PartiID[];
                PartiID=Movimento[0].split("_");
                String ID=PartiID[0]+"_"+PartiID[1]+"_"+PartiID[2]+"_0"+PartiID[3]+"_PC";
                RT[0]=ID;
                RT[5]="PRELIEVO CRYPTO";
                RT[6]=Movimento[8]+" ->";
                RT[11]="";
                RT[12]="";
                RT[13]="";
                RT[27]="";
                RT[28]="";
                Funzioni.RiempiVuotiArray(RT);
                MappaCryptoWallet.put(ID, RT);
                
                //2 - Genero il movimento di Deposito e lo classifico come rimborso
                RT=new String[ColonneTabella];
                System.arraycopy(Movimento, 0, RT, 0, ColonneTabella); //ricopio tutti i movimenti
                //adesso modifico quelli che mi interessano
                PartiID=Movimento[0].split("_");
                ID=PartiID[0]+"_"+PartiID[1]+"_"+PartiID[2]+"_"+PartiID[3]+"_DC";
                RT[0]=ID;
                RT[5]="REWARD";
                RT[6]="-> "+Movimento[11];
                RT[8]="";
                RT[9]="";
                RT[10]="";
                RT[18]="DAI - Reward";
                RT[25]="";
                RT[26]="";
                Funzioni.RiempiVuotiArray(RT);
                MappaCryptoWallet.put(ID, RT);
                
                //3 - Cancello il vecchi movimento
                MappaCryptoWallet.remove(Movimento[0]);
                CDC_Grafica.TabellaCryptodaAggiornare=true;
              /*  Plusvalenze.AggiornaPlusvalenze();
                TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa(this.TransazioniCrypto_CheckBox_EscludiTI.isSelected());
                DepositiPrelievi_Caricatabella();*/
                
                
            
        }
        
    }
    
    
    
    
    
    
}