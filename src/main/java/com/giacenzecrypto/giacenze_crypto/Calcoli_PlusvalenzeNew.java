/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.DecimaliCalcoli;
import static com.giacenzecrypto.giacenze_crypto.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_PlusvalenzeNew {
    
    /**
     *
     * @param movimento
     * @return  Funzione che si occupa di categorizzare le transazioni in 10 tipologie
     * che corrispondono poi a 10 calcoli diversi per Plusvalenza, Lifo e costi di carico
     * di ritorno a questa funzione viene tornato un numero che identifica la categoria<br>
     * Queste sono le categorie:<br>
     * <br>
     *      1 - Scambio tra Criptoattività Omogenee<br>
     *           Comprende:  NFT -> NFT<br>
     *                       Crypto -> Crypto     <br>
     *       2 - Scambio tra Criptoattività non omogenee<br>
     *           Comprende:  Crypto -> NFT<br>
     *                       NFT -> Crypto       <br>
     *       3 - Acquisto Criptoattività<br>
     *           Comprende:  FIAT -> NFT<br>
     *                       FIAT -> Crypto         <br>
     *       4 - Vendita Criptoattività<br>
     *           Comprende:  NFT -> FIAT<br>
     *                       Crypto -> FIAT          <br>       
     *       5 - Deposito Criptoattività x spostamento tra wallet<br>
     *           Comprende:  -> NFT          (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione) <br>                        
     *       6 - Prelievo Criptoattività x spostamento tra wallet<br>
     *           Comprende:  NFT ->          (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione) <br>
     *       7 - Deposito Criptoattività x rewards, stacking,cashback etc...<br>
     *           Comprende:  -> NFT          (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)<br>
     *       8 - Prelievo Criptoattività x servizi, acquisto beni etc...<br>
     *           Comprende:  NFT ->          (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)<br>
     *       9 - Deposito Criptoattività DCZ         (Deposito a costo di carico zero)<br>
     *           Comprende:  -> NFT          (Tipologia DCZ su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia DCZ su quella della Transazione)<br>
     *       10 - Prelievo Criptoattività PWN        (Prelievo a plusvalenza Zero ma toglie dal Lifo) <br>
     *           Comprende:  NFT ->          (Tipologia PWN su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia PWN su quella della Transazione)<br>
     *       11 - Deposito FIAT
     *           Comprende:    -> FIAT
     */
    
 
    
    
    /**
     *
     * @param CryptoStack
     * @param Tipologia
     * @return  In base alla Tipologia di movimento<br>
     * ritorna la tipologia di plusvalenza, costo di carico, rimonozione dallo stack LIFO, inserimento nello stack Lifo<br>
     * in un array di int<br><br>
     * dove:<br>
     * int[0]=Tipologia Plusvalenza<br>
     * int[1]=Tipologia Calcolo Costo di Carico<br>
     * int[2]=Tipologia Eliminazione vecchio costo di carico da stack LIFO<br>
     * int[3]=Tipologia inserimento nuovo costo di carico da stack LIFO<br><br><br>
     * Con la seguente logica:<br>
     * <br>
     * int[0]=0 :   Il campo plusvalenza va compilato con valore Zero <br>
     * int[0]=1 :   Il campo plusvalenza va compilato con il ValoreTransazione<br>
     * int[0]=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)<br><br>
     * int[1]=0 :   Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero <br>
     * int[1]=1 :   Nuovo Costo di Carico= "" <br>
     * int[1]=2 :   Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)<br>
     * int[1]=3 :   Nuovo Costo di Carico = Valore Transazione<br><br>
     * int[2]=0 :   Non tolgo dallo stack il vecchio costo di carico <br>
     * int[2]=1 :   Tolgo dallo stack il vecchio costo di carico<br><br>
     * int[3]=0 :   Costo Lifo Moneta Entrante = Zero <br><
     * int[3]=1 :   Non faccio nulla (non inserisco nessun valore)<br>
     * int[3]=2 :   Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente<br>
     * int[3]=3 :   Costo Lifo Moneta Entrante = Valore Transazione<br>
     * int[4]=0 :   Vecchio Costo di Carico=0<br>
     * int[4]=1 :   Vecchio Costo di carico=""<br>
     * int[4]=2 :   Vecchio Costo di carico=preso da lifo moneta<br>
     */
    
    
private static Map<String, LifoXID> MappaIDTrans_LifoxID = new TreeMap<>();
    
public static LifoXID getIDLiFo(String id){
    return MappaIDTrans_LifoxID.get(id);
}
    
/* public static String StackLIFO_TogliQtaNEW(Map<String, ArrayDeque<String[]>> CryptoStack, String Moneta,String Qta,boolean toglidaStack,String IDTransazione) {
    
    LifoXID lifoID=MappaIDTrans_LifoxID.computeIfAbsent(IDTransazione, k -> new LifoXID());
   // lifoID.StackEntrato.push(valori);
     
    //Se la qta o la moneta sono vuoti non ritorno nulla, quei campi devono essere obbligatoriamente valorizzati 
    if (Moneta.isBlank() || Qta.isBlank()) return "";
    
    ArrayDeque<String[]> originalStack = CryptoStack.get(Moneta);
    if (originalStack == null) return "0.00";
    
    // Se non devo togliere dallo stack originale, lo clono
    ArrayDeque<String[]> stack = toglidaStack ? originalStack : originalStack.clone();

    BigDecimal qtaRimanente = new BigDecimal(Qta).abs();
    BigDecimal costoTransazione = BigDecimal.ZERO;

    //prima cosa individuo la moneta e prendo lo stack corrispondente
   /* if (CryptoStack.get(Moneta)==null){
        //ritorno="0";
    }else{*/

/*while (qtaRimanente.compareTo(BigDecimal.ZERO) > 0 && !stack.isEmpty()) {
        String[] ultimoRecupero = stack.pop();
        BigDecimal qtaEstratta = new BigDecimal(ultimoRecupero[1]).abs();
        BigDecimal costoEstratto = new BigDecimal(ultimoRecupero[2]).abs();

        if (qtaEstratta.compareTo(qtaRimanente) <= 0) {
            // Caso semplice: uso tutta la quantità
            //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
            qtaRimanente = qtaRimanente.subtract(qtaEstratta);
            //recupero il valore di quella transazione e la aggiungo al costoTransazione
            costoTransazione = costoTransazione.add(costoEstratto);
            
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            if (toglidaStack){
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;
            valoriDaTogliere[1]=qtaEstratta.abs().toPlainString();
            valoriDaTogliere[2]=costoEstratto.toPlainString();
            valoriDaTogliere[3]=ultimoRecupero[3];
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            }
        } else {
            // Caso in cui la quantità richiesta è inferiore a quella in stack
            //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
            //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
            //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
            //non ho più qta rimanente
                        
            BigDecimal qtaRimanenteStack = qtaEstratta.subtract(qtaRimanente);

            BigDecimal costoUnitario = costoEstratto
                .divide(qtaEstratta, DecimaliCalcoli + 10, RoundingMode.HALF_UP);

            //Il valore lo arrotondo al secondo decimale per coerenza tanto poi il restante viene calcolato tramite sottrazione
            BigDecimal valoreRimanenteStack = costoUnitario
                .multiply(qtaRimanenteStack)
               // .setScale(DecimaliCalcoli, RoundingMode.HALF_UP)
                .setScale(Statiche.DecimaliPlus, RoundingMode.HALF_UP)
                .stripTrailingZeros();

            String[] valori = new String[] {
                Moneta,
                qtaRimanenteStack.toPlainString(),
                valoreRimanenteStack.toPlainString(),
                ultimoRecupero[3]
            };
           // lifoID.StackEntrato.push(valori);
            stack.push(valori);

            BigDecimal valoreUsato = costoEstratto.subtract(valoreRimanenteStack);
            costoTransazione = costoTransazione.add(valoreUsato);

           
            //Questa cosa la faccio solo se il flag toglidastack è attivo il che significa solo se è un movimento
            //che realmente movimenta lo stack
            if (toglidaStack){
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;                                         //Moneta di riferimento
            valoriDaTogliere[1]=qtaRimanente.abs().toPlainString();             //qta tolta dallo stack
            valoriDaTogliere[2]=valoreUsato.toPlainString();                    //costo della qta tolra
            valoriDaTogliere[3]=ultimoRecupero[3];                              //ID della Transazione
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            //Stack Uscito Rimanenze sono appunto quello che rimane delle stack dopo il movimento
            
            }
             qtaRimanente = BigDecimal.ZERO;
           // 
        }
        
    }
if (toglidaStack){
    lifoID.StackUscitoRimanenze=stack.clone();
}
    //return costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();
    return costoTransazione.toPlainString();
}      */
 
public static String StackLIFO_TogliQta(Map<String, ArrayDeque<String[]>> CryptoStack, String Moneta,String Qta,boolean toglidaStack,String IDTransazione) {
    
    LifoXID lifoID=MappaIDTrans_LifoxID.computeIfAbsent(IDTransazione, k -> new LifoXID());
   // lifoID.StackEntrato.push(valori);
   
    // if (Moneta.equals("APE"))System.out.println("APE - "+Qta);
    //Se la qta o la moneta sono vuoti non ritorno nulla, quei campi devono essere obbligatoriamente valorizzati 
    if (Moneta.isBlank() || Qta.isBlank()) return "";
    
    //Se lo stack è vuoto salvo l'errore e ritorno 0.00 come costo di carico
    ArrayDeque<String[]> originalStack = CryptoStack.get(Moneta);
    if (originalStack == null) 
    {
       originalStack=new ArrayDeque<>();
       // return "0.00";
    }
    // Se non devo togliere dallo stack originale, lo clono
    ArrayDeque<String[]> stack = toglidaStack ? originalStack : originalStack.clone();

    BigDecimal qtaRimanente = new BigDecimal(Qta).abs();
    BigDecimal costoTransazione = BigDecimal.ZERO;

    //if (Moneta.equals("APE")) System.out.println("APE " + stack.size()+ " - "+qtaRimanente);
    //prima cosa individuo la moneta e prendo lo stack corrispondente
   /* if (CryptoStack.get(Moneta)==null){
        //ritorno="0";
    }else{*/

while (qtaRimanente.compareTo(BigDecimal.ZERO) > 0 && !stack.isEmpty()) {
        String[] ultimoRecupero = stack.pop();
        BigDecimal qtaEstratta = new BigDecimal(ultimoRecupero[1]).abs();
        BigDecimal costoEstratto = new BigDecimal(ultimoRecupero[2]).abs();

        if (qtaEstratta.compareTo(qtaRimanente) <= 0) {
            // Caso semplice: uso tutta la quantità
            //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
            qtaRimanente = qtaRimanente.subtract(qtaEstratta);
            //recupero il valore di quella transazione e la aggiungo al costoTransazione
            costoTransazione = costoTransazione.add(costoEstratto);
            
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            if (toglidaStack){
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;
            valoriDaTogliere[1]=qtaEstratta.abs().toPlainString();
            valoriDaTogliere[2]=costoEstratto.toPlainString();
            valoriDaTogliere[3]=ultimoRecupero[3];
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            }
        } else {
            // Caso in cui la quantità richiesta è inferiore a quella in stack
            //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
            //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
            //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
            //non ho più qta rimanente
                        
            BigDecimal qtaRimanenteStack = qtaEstratta.subtract(qtaRimanente);

            BigDecimal costoUnitario = costoEstratto
                .divide(qtaEstratta, DecimaliCalcoli + 10, RoundingMode.HALF_UP);

            BigDecimal valoreRimanenteStack = costoUnitario
                .multiply(qtaRimanenteStack)
                //.setScale(2, RoundingMode.HALF_UP)
                .setScale(DecimaliCalcoli, RoundingMode.HALF_UP)
                .stripTrailingZeros();

            String[] valori = new String[] {
                Moneta,
                qtaRimanenteStack.toPlainString(),
                valoreRimanenteStack.toPlainString(),
                ultimoRecupero[3]
            };
           // lifoID.StackEntrato.push(valori);
            stack.push(valori);

            BigDecimal valoreUsato = costoEstratto.subtract(valoreRimanenteStack);
            costoTransazione = costoTransazione.add(valoreUsato);

           
            //Questa cosa la faccio solo se il flag toglidastack è attivo il che significa solo se è un movimento
            //che realmente movimenta lo stack
            if (toglidaStack){
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;                                         //Moneta di riferimento
            valoriDaTogliere[1]=qtaRimanente.abs().toPlainString();             //qta tolta dallo stack
            valoriDaTogliere[2]=valoreUsato.toPlainString();                    //costo della qta tolra
            valoriDaTogliere[3]=ultimoRecupero[3];                              //ID della Transazione
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            //Stack Uscito Rimanenze sono appunto quello che rimane delle stack dopo il movimento
           // lifoID.StackUscitoRimanenze=stack.clone();
            }
             qtaRimanente = BigDecimal.ZERO;
           // 
        }
    }
    if (toglidaStack) {
        lifoID.StackUscitoRimanenze = stack.clone();
    }

    if (qtaRimanente.compareTo(BigDecimal.ZERO) > 0 && stack.isEmpty()) {
//Adesso verifico se sono rimaste ancora parti da togliere di cui non ho però l'equivalente nello stack e lo segnalo
        String valoriDaTogliere[] = new String[4];
        valoriDaTogliere[0] = Moneta;                                         //Moneta di riferimento
        valoriDaTogliere[1] = qtaRimanente.abs().toPlainString();             //qta tolta dallo stack
        valoriDaTogliere[2] = "0";                                            //costo della qta tolra
        valoriDaTogliere[3] = "";                                             //ID della Transazione
        lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)

//Segnalo l'errore anche direttamente sul movimento
        //System.out.println("Errore");
        CDC_Grafica.MappaCryptoWallet.get(IDTransazione)[38]="A";
       // System.out.println("Errore "+IDTransazione);
    }

    return costoTransazione.setScale(Statiche.DecimaliPlus, RoundingMode.HALF_UP).toPlainString();
   // return costoTransazione.setScale(4, RoundingMode.HALF_UP).toPlainString();
}       
 
 
    
   public static void StackLIFO_InserisciValore(Map<String, ArrayDeque<String[]>> CryptoStack, String Moneta,String Qta,String Valore,String IDTransazione) {
    
   // ArrayDeque<String[]> stack;
    String valori[]=new String[4];
    valori[0]=Moneta;
    valori[1]=new BigDecimal(Qta).abs().toPlainString();
    valori[2]=Valore;
    valori[3]=IDTransazione;
   /* if (CryptoStack.get(Moneta)==null){
        stack = new ArrayDeque<>();
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }else{
        stack=CryptoStack.get(Moneta);
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }*/
    // Funzione equivalente e semplificata di quella sopra    
    //Aggiungo allo stack della moneta anche questo valore
    ArrayDeque<String[]> stack = CryptoStack.computeIfAbsent(Moneta, k -> new ArrayDeque<>());
    LifoXID lifoID=MappaIDTrans_LifoxID.computeIfAbsent(IDTransazione, k -> new LifoXID());
    lifoID.StackEntratoPreMovimento=stack.clone();
    stack.push(valori);
    
    //Aggiungo allo stack relativo all'id anche questo valore relativamente a quello che è entrato
    //Questo mi servirà per poi visualizzare per ogni transazione lo stack della stessa
    lifoID.StackEntrato.push(valori);

}
    
     public static void AggiornaPlusvalenze(){
         
      //   System.out.println("Aggiornamento Plusvalenze");
////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere


        MappaIDTrans_LifoxID.clear();

       //Con questa opzione decido che fare in caso di movimenti non classificati, se conteggiarli o meno
       boolean ConsideraMovimentiNC=true;
       if(DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC").equalsIgnoreCase("NO"))ConsideraMovimentiNC=false;
       
       // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, ArrayDeque<String[]>>> MappaGrWallet_CryptoStack = new TreeMap<>();
       // Map<String, ArrayDeque<String[]>> CryptoStack;// = new TreeMap<>();
        
    //    Map<String, String[]> MappaCryptoWalletTemp=MappaCryptoWallet;
        
        //controllo se devo o meno prendere in considerazione i gruppi wallet per il calcolo della plusvalenza
        boolean PlusXWallet=false;
        String PlusXW=DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if(PlusXW!=null && PlusXW.equalsIgnoreCase("SI")){
            PlusXWallet=true;
        }
        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
               // System.out.println(GruppoWallet);
            if(!PlusXWallet)GruppoWallet="Wallet 01";
                
            //Questa funzione inizializza la mappa CryptoStack nel caso non esista già nella mappa MappaGrWallet_CryptoStack, nel qual caso recupera il suo valore         
            Map<String, ArrayDeque<String[]>> CryptoStack = MappaGrWallet_CryptoStack.computeIfAbsent(GruppoWallet, k -> new TreeMap<>());

            String TipoMU = Funzioni.RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
           // if (v[12]==null)System.out.println(v[11]+"_"+v[1]+"_"+v[12]);
            String TipoME = Funzioni.RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
            String IDTransazione=v[0];
            String IDTS[]=IDTransazione.split("_");
            String MonetaU=v[8];
            String QtaU=v[10];
            String MonetaE=v[11];
            String QtaE=v[13];
            String Valore=v[15];
            String VecchioPrezzoCarico="0.00";
            String NuovoPrezzoCarico="0.00";
            String Plusvalenza="0.00";
            String CalcoloPlusvalenza="N";
            long long2023=OperazioniSuDate.ConvertiDatainLongMinuto("2023-01-01 00:00");
            long dataLong=OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
            boolean DataSuperiore2023=true;
            if (dataLong<long2023){DataSuperiore2023=false;}
            boolean Pre2023EarnCostoZero = false;
            boolean Pre2023ScambiRilevanti = false;
            String Plusvalenze_Pre2023EarnCostoZero = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023EarnCostoZero");
            if (Plusvalenze_Pre2023EarnCostoZero != null && Plusvalenze_Pre2023EarnCostoZero.equalsIgnoreCase("SI")) {
                Pre2023EarnCostoZero=true;
            }
            String Plusvalenze_Pre2023ScambiRilevanti = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023ScambiRilevanti");
            if (Plusvalenze_Pre2023ScambiRilevanti != null && Plusvalenze_Pre2023ScambiRilevanti.equalsIgnoreCase("SI")) {
                Pre2023ScambiRilevanti=true;
            }
            
            
            //TIPOLOGIA = 0 (Vendita Crypto)
            //System.out.println("aaa "+IDTransazione);
            if (IDTS[4].equals("VC")){
                //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                NuovoPrezzoCarico="";
                
                //Calcolo la plusvalenza
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString(); 
                CalcoloPlusvalenza="S";
            }           
            //TIPOLOGIA = 1  (Scambio Cripto Attività medesime Caratteristiche)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")//non devono essere fiata
                    && TipoMU.equalsIgnoreCase(TipoME)&&//moneta uscita e entrata dello stesso tipo
                    !TipoMU.isBlank() && !TipoME.isBlank()) //non devno essere campi nulli (senza scambi)
            {
                
                if (DataSuperiore2023||!Pre2023ScambiRilevanti){//se la data è superiore al 2023 oppure gli scambi pre 2023 non voglio renderli rilvenati
                    //Tolgo dallo stack il costo di carico della cripèto uscita
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                    
                    //Inserisco il costo di carico nello stack della cripto entrata
                    NuovoPrezzoCarico=VecchioPrezzoCarico;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                    
                    //La plusvalenza va valorizzata a zero
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";
                 }else {//altrimenti calcolo la plusvalenza
                    //Tolgo dallo stack il vecchio costo di carico
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                    
                    //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                    //lo aggiungo quindi allo stack del lifo
                    NuovoPrezzoCarico=Valore;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                    
                    //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                    CalcoloPlusvalenza="S";
                }                                      
            } 
            
            
            //TIPOLOGIA = 2 (Scambio Cripto Attività Diverse Caratteristiche)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")
                    && !TipoMU.equalsIgnoreCase(TipoME)&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                    //Tolgo dallo stack il vecchio costo di carico
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                    
                    //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                    //lo aggiungo quindi allo stack del lifo
                    NuovoPrezzoCarico=Valore;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                    
                    //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                    CalcoloPlusvalenza="S";
                                       
            }
            
            
            //TIPOLOGIA = 3 (Acquisto di Cripto attività tramite FIAT)
            else if (TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                
                    NuovoPrezzoCarico=Valore;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                    
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";
                                         
                    VecchioPrezzoCarico=""; 
                    
                    
                    
            }
            
            //TIPOLOGIA = 4 (Vendita Criptoattività per FIAT)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                NuovoPrezzoCarico="";
                
                //Calcolo la plusvalenza
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                CalcoloPlusvalenza="S";                
                 
            } 
            
            
            //TIPOLOGIA = 5 , 7 e 9 -> Deposito Criptoattività di vario tipo
            else if (TipoMU.isBlank() && !TipoME.equalsIgnoreCase("FIAT")) 
            {
                //Se arrivo qua vuol dire che questo è un deposito, poi a secondo di che tipo di deposito è
                //valorizzo la tipologia corretta
                
                //TIPOLOGIA = 7; ( Deposito Criptoattività x rewards, stacking,cashback etc... - Plusvalenza immediata)
                if (IDTS[4].equalsIgnoreCase("RW") || v[18].contains("DAI")) {
                   // Funzioni.RewardRilevante(IDTransazione);
                    //Se data superiore a 2023 e la reward è fiscalmente rilvente oppure se
                    //la data è inferiore al 2023, la reward è rilevante e non è attiva l'opzione per cui tutte le reward pre2023 sono da mettere a costo carico a zero
                    //allore considero la reward rilevante
                    //altrimenti non rilevante
                    if ((DataSuperiore2023&&Funzioni.RewardRilevante(IDTransazione)) || 
                            (!DataSuperiore2023&&!Pre2023EarnCostoZero&&Funzioni.RewardRilevante(IDTransazione))
                            ) 
                    {
                        NuovoPrezzoCarico = Valore;

                        StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                        Plusvalenza = Valore;
                        CalcoloPlusvalenza="S";

                        VecchioPrezzoCarico = "";
                    } else {
                        NuovoPrezzoCarico = "0.00";
                        StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza="N";

                        VecchioPrezzoCarico = "";
                    }

                }

                //Tipologia = 5; (Deposito Criptoattività x spostamento tra wallet)
                else if (IDTS[4].equalsIgnoreCase("TI") || v[18].contains("DTW")) {
                    
                    
                    
                    
                //else if (TipoMU.isBlank()&&(IDTS[4].equalsIgnoreCase("TI") || v[18].isBlank() || v[18].contains("DTW"))) {
                    //il compito è trovare la controparte del movimento qualora questa si riferisse ad un diverso gruppo wallet
                    //e da li spostare il costo di carico
                   // String IDControparte = null;
                   // String GruppoWalletControparte = null;
                    String temp[]=RitornaIDeGruppoControparteSeGruppoDiverso(v);
                    //questa funzione mi torna dei valori diversi da null se
                    //il wallet controparte è diverso da quello originale e se la plusvalenza va calcolata divisa per gruppo wallet
                    String IDControparte=temp[0];
                    String GruppoWalletControparte = temp[1];
                    
               
                    //Se ID controparte è diverso da null vuol dire che devo gestire il calcolo delle plusvalenze, altrimenti no
                    if (IDControparte != null) {
                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza="N";
                        VecchioPrezzoCarico = "";
                        
                        //DA VEDERE PERCHE' IL CRYPTO STACK E' DIVERSO
                    String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IDControparte);
                    Map<String, ArrayDeque<String[]>> CryptoStack2=MappaGrWallet_CryptoStack.get(GruppoWalletControparte);// = new TreeMap<>();
                    Mov[31]=v[1];
                        if (CryptoStack2 == null) {
                            //In teoria qua non ci dovrei mai entrare
                            NuovoPrezzoCarico = "";
                        } else {
                            NuovoPrezzoCarico = StackLIFO_TogliQta(CryptoStack2, Mov[8], Mov[10], true,IDTransazione);
                            StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);
                        }

                    } else {
                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza="N";

                        NuovoPrezzoCarico = "";

                        VecchioPrezzoCarico = "";
                    }

                }
                
                //Tipologia = 9; (Deposito a costo di carico zero)
                else if(v[18].contains("DCZ")){
                     
                     NuovoPrezzoCarico="0.00";
                     StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                     
                     Plusvalenza="0.00";
                     CalcoloPlusvalenza="N";
                     
                     VecchioPrezzoCarico="";
                }
                
                //Tipologia = 3; (Acquisto Crypto)
                else if(v[18].contains("DAC")){
                    
                    NuovoPrezzoCarico=Valore;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                    
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";
                                         
                    VecchioPrezzoCarico=""; 
                }
                //Tipologia = XY; (Deposito non categorizzato) -> Vengono caricati sul LiFo a costo di carico Zero
                else if(v[18].isBlank()){
                    // nel caso la variabile considera movimenti non classficati sia a trueconsidero il movimento come deposito a zero
                     if (ConsideraMovimentiNC) {
                        NuovoPrezzoCarico = "0.00";
                        StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza = "N";

                        VecchioPrezzoCarico = "";
                    } else {
                        //altrimenti non lo considero
                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza = "N";

                        NuovoPrezzoCarico = "";

                        VecchioPrezzoCarico = "";
                    }
                }
            } 
            
            //TIPOLOGIA = 6 , 8 e 10 -> Prelievo Criptoattività di vario tipo
            else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.isBlank()) 
            {
                //Se arrivo qua vuol dire che questo è un Prelievo, poi a secondo di che tipo di deposito è
                //valorizzo la tipologia corretta                         
                
                //Tipologia = 4 Sto facendo il rimborso di un cashback o altro quindi lo considero come vendita
                if(IDTS[4].equalsIgnoreCase("RW")){
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico="";
                
                    //Calcolo la plusvalenza
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                    CalcoloPlusvalenza="S";
                }
                //Tipologia = 8;//Prelievo Criptoattività x servizi, acquisto beni etc... //per ora uguale alla tipologia 4
                else if(IDTS[4].equalsIgnoreCase("CM")||v[18].contains("PCO")){
                    
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico="";
                
                    //Calcolo la plusvalenza
                  //  if (Funzioni.Funzioni_isNumeric(Valore, false)&&Funzioni.Funzioni_isNumeric(VecchioPrezzoCarico, false))
                        Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                        CalcoloPlusvalenza="S";
                   // else Plusvalenza="ERRORE";
                }
                //Tipologia = 6;//Prelievo Criptoattività x spostamento tra wallet
                else if (IDTS[4].equalsIgnoreCase("TI")||v[18].contains("PTW")) {
                                        
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";
                     
                    NuovoPrezzoCarico="";
                     
                    String temp[]=RitornaIDeGruppoControparteSeGruppoDiverso(v);
                    String GruppoWalletControparte = temp[1];
                     
                    if (v[18].contains("PTW") && GruppoWalletControparte!=null &&!GruppoWallet.equalsIgnoreCase(GruppoWalletControparte)) {
                        //Inserisco il prezzo di carico del token in uscita solo se va poi a finire su un gruppoWallet diverso
                        //e solo se ho attiva l'opzione che vuole il calcolo delle plusvalenze divise per wallet
                        //altrimenti lo tratto alla stregua di un trasferimento interno e non metto nulla, tanto è un movimento completamente irrilevante
                        //In ogni caso non lo tolgo dal LiFo perchè lo toglierò dal LiFo nel momento in cui c'è il deposito nel nuovo wallet
                        VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, false,IDTransazione);
                    } else
                        VecchioPrezzoCarico = "";

                } 
                
                //Tipologia = 10;//(Prelievo a plusvalenza Zero ma toglie dal Lifo) FURTO o DONAZIONE
                else if(v[18].contains("PWN")){
                    
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                    
                    NuovoPrezzoCarico="";
                    
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";                    
                    
                }
                //Tipologia = XY;//(Movimento non categorizzato) - Lo Considero come un cashOut
                else if(v[18].isBlank()){
                    if (ConsideraMovimentiNC) {
                        //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                        VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, true,IDTransazione);

                        //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                        NuovoPrezzoCarico = "";

                        //Calcolo la plusvalenza
                        Plusvalenza = new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                        CalcoloPlusvalenza = "S";
                    } else {
                        Plusvalenza = "0.00";
                        CalcoloPlusvalenza = "N";
                        NuovoPrezzoCarico = "";
                        VecchioPrezzoCarico = "";
                    }
                }
            } 
            //TIPOLOGIA = 11 -> Deposito FIAT o Prelievo FIAT
            else if ((TipoMU.isBlank() && TipoME.equalsIgnoreCase("FIAT"))||(TipoME.isBlank() && TipoMU.equalsIgnoreCase("FIAT"))) 
            {
                    
                    NuovoPrezzoCarico="";
                    
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";
                                           
                    VecchioPrezzoCarico="";
                    
            }
            else {
                System.out.println("Classe:Plusvalenze - Funzione:CategorizzaTransazione - Nessuna Tipologia Individuata");
                System.out.println(TipoMU+" - "+TipoME);
            }
            
            

                    v[16]=VecchioPrezzoCarico;
                    v[17]=NuovoPrezzoCarico;
                    v[19]=Plusvalenza;
                    v[33]=CalcoloPlusvalenza;


        }
    }




   

   /**
     *
     * @param Tipologia
     * @param Data
     * @param Token
     * @return  In base alla Tipologia di movimento<br>
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     */
  /* public static String RitornaTipoCrypto(String Token,String Data,String Tipologia) {
       String Tipo=Tipologia;
       String DataEmoney=CDC_Grafica.Mappa_EMoney.get(Token);
       if(Tipologia.equalsIgnoreCase("Crypto")&&DataEmoney!=null){
           long dataemoney=OperazioniSuDate.ConvertiDatainLong(DataEmoney);
           long datascambio=OperazioniSuDate.ConvertiDatainLong(Data);
           if (datascambio>=dataemoney) Tipo="EMoney";
       }
       return Tipo;
   }*/
   
    public static String[] RitornaIDeGruppoControparteSeGruppoDiverso(String v[]) {
        
        String IDeGruppo[] = new String[2];

        boolean PlusXWallet = false;
        String PlusXW = DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if (PlusXW != null && PlusXW.equalsIgnoreCase("SI")) {
            PlusXWallet = true;
        }
        if (!PlusXWallet) return IDeGruppo;
        //Se non voglio la distinzione per wallet ritorno subito la stringa nulla
        
        
        //il compito è trovare la controparte del movimento qualora questa si riferisse ad un diverso gruppo wallet
        //e da li spostare il costo di carico
        String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
        String IDControparte = null;
        String GruppoWalletControparte = null;
        //comincio impostando le prime condizioni
        //v[20] non deve essere nullo ovvero devo avere transazioni allegate
        //v[18] deve essere un deposito derivante da trasferimenti e deve essere o un movimento importato o uno manuale (v[22]=ad A o M)
        if (!v[20].isBlank() && (v[18].contains("DTW") || v[18].contains("PTW")) && (v[22].equals("A") || v[22].equals("M"))) {

            //Se è un movimento di Trasferimento tra wallet (2 o 3 movimenti a seconda se ci sono le commissioni) il movimento controparte è l'unico PTW
            //Se è un movimento di scambio differito (5 movimenti) il movimento controparte è un PTW classificato come AU (posizione 22)
            //Tutto questo lo faccio però solo se il movimento di controparte PTW fa parte di un altro gruppo di wallet, altrimentio non faccio nulla.
            String Movimenti[] = v[20].split(",");

            if (Movimenti.length > 3)//Sono in presenza di uno scambio differito
            {
                for (String IdM : Movimenti) {
                    String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IdM);
                    //devo trovare la controparte che in questo caso è il movimento di prelievo creato automaticamente dal sistema
                    //inoltre devo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                    //perchè se fanno parte dello stesso gruppo non devo fare nulla
                    //se fanno parte dello stesso gruppo infatti è lo stesso movimento di scambio a spostare il costo di carico

                    if (v[18].contains("DTW") && Mov[18].contains("PTW") && Mov[22].contains("AU")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]);
                    }
                    //non faccio niente in caso di prelievo PTW perchè quello non è mai rilevante essendo un mero trasferimento interno
                }
            } else {//Scambio tra wallet

                for (String IdM : Movimenti) {
                    String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IdM);
                    //devo trovare la controparte che in questo caso è l'unico movimento di prelievo
                    //inoltre vedo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                    //perchè se fanno parte dello stesso gruppo non devo fare nulla
                    if (v[18].contains("DTW") && Mov[18].contains("PTW")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]);
                    } else if (v[18].contains("PTW") && Mov[18].contains("DTW")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]);
                    }

                }

            }

        }
        
        IDeGruppo[0] = IDControparte;
        IDeGruppo[1] = GruppoWalletControparte;
        return IDeGruppo;
    }
    
    
    
    
    
    
          public static class LifoXID {

          

          ArrayDeque<String[]> StackEntrato=new ArrayDeque<>();
          ArrayDeque<String[]> StackEntratoPreMovimento=new ArrayDeque<>();
          ArrayDeque<String[]> StackUscito=new ArrayDeque<>();
          ArrayDeque<String[]> StackUscitoRimanenze=new ArrayDeque<>();
          
          
          
          public void sostituisci_CryptoStackEntrato(ArrayDeque<String[]> PMStack)
          {
            StackEntrato=PMStack;
          }
          public void aggiungi_Entrato_Dettagli(String Dettaglio[])
          {
            StackEntrato.add(Dettaglio);
          }
          
          public ArrayDeque<String[]> Get_CryptoStackEntrato()
          {
            return StackEntrato;
          }  
          public ArrayDeque<String[]> Get_CryptoStackEntratoPreMovimento()
          {
            return StackEntratoPreMovimento;
          } 
          
          public ArrayDeque<String[]> Get_CryptoStackUscito()
          {
            return StackUscito;
          }  
          
          public ArrayDeque<String[]> Get_CryptoStackUscitoRimanenze()
          {
            return StackUscitoRimanenze;
          }  
      
      }     

}   
    
    



        /* SEZIONE SPIEGAZIONI
        In sezione funzione suddivido gli scambi crypto per categorie in modo da poter più facilmente poi gestire il calcolo della plusvalenza etc...
        Per Ogni tipologia gli andrò a dire come comportarsi con le seguenti situazioni:
            - Calcolo Plusvalenza
            - Valore del Nuovo Costo di Carico
            - Se Togliere o meno dallo stack del lifo della moneta uscente il vecchio costo di carico
            - Se mettere e con che valore mettere nello stack lifo relativo alla moneta entrante il valore del nuovo costo di carico
        
        In questa funzione in particolare divito gli scambi in 10 categorie:
            1 - Scambio tra Criptoattività Omogenee
                Comprende:  NFT -> NFT
                            Crypto -> Crypto     
            2 - Scambio tra Criptoattività non omogenee
                Comprende:  Crypto -> NFT
                            NFT -> Crypto       
            3 - Acquisto Criptoattività
                Comprende:  FIAT -> NFT
                            FIAT -> Crypto         
            4 - Vendita Criptoattività
                Comprende:  NFT -> FIAT
                            Crypto -> FIAT                 
            5 - Deposito Criptoattività x spostamento tra wallet
                Comprende:  -> NFT          (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)
                            -> Crypto       (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)                         
            6 - Prelievo Criptoattività x spostamento tra wallet
                Comprende:  NFT ->          (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione)
                            Crypto ->       (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione) 
            7 - Deposito Criptoattività x rewards, stacking,cashback etc...
                Comprende:  -> NFT          (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)
                            -> Crypto       (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)
            8 - Prelievo Criptoattività x servizi, acquisto beni etc...
                Comprende:  NFT ->          (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)
                            Crypto ->       (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)
            9 - Deposito Criptoattività DCZ         (Deposito a costo di carico zero)
                Comprende:  -> NFT          (Tipologia DCZ su quella della Transazione)
                            -> Crypto       (Tipologia DCZ su quella della Transazione)
            10 - Prelievo Criptoattività PWN        (Prelievo a plusvalenza Zero ma toglie dal Lifo) 
                Comprende:  NFT ->          (Tipologia PWN su quella della Transazione)
                            Crypto ->       (Tipologia PWN su quella della Transazione)
            11 - Deposito FIAT
                Comprende:    -> FIAT
        
        
        
        Per ogni tipologia trovata devo indicare le seguenti caratteristiche:
        
        TipologiaPlus -> Indica come va calcolata la Plusvalenza per ogni tipologia di scambio (Va messo poi nel Campo 19 della tabella)
            TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero 
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
            TipologiaPlus=1 :   Il campo plusvalenza va compilato con il ValoreTransazione
                    Cosa rientra :  Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
            TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 4 (Vendita criptoattività)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
        
        TipologiaNCC -> Indica come va calcolato il nuovo costo di carico per ogni tipologia di scambio (Va messo poi nel Campo 17 della tabella)
            TipologiaNCC=0 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero    
                    Cosa rientra :  Tipologia 4 (Vendita criptoattività)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
            TipologiaNCC=1 :    Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)   
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
            TipologiaNCC=2 :    Nuovo Costo di Carico = Valore Transazione   
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
        
        TipologiaStackLIFOVecchioCosto -> Indica se devo togliero o meno dallo stack il vecchio costo di carico della moneta uscente
            TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                    Cosa rientra :  Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
            TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 4 (Vendita criptoattività)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
        
        TipologiaStackLIFONuovoCosto -> Indica che valore devo o se devo inserire nello stack del Lifo sulla moneta entrante
            TipologiaStackLIFONuovoCosto=0 :   Costo Lifo Moneta Entrante = Zero
                    Cosa rientra :  Tipologia 9 (Deposito forzato a costo di carico zero)
            TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
                    Cosa rientra :  Tipologia 4 (Vendita criptoattività)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare))
            TipologiaStackLIFONuovoCosto=2 :   Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
            TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
        
        
        Quindi ricapitolanto per tipologia abbiamo le seguenti caratteristiche
        Tipologia 1 (Scambio criptoatt. omogenee) :
                TipologiaPlus=0 (Plusvalenza=0)
                TipologiaNCC=2 (Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita)
                TipologiaStackLIFOVecchioCosto=1  (Tolgo dallo stack il vecchio costo di carico)
                TipologiaStackLIFONuovoCosto=2  (Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente)
        Tipologia 2 (Scambio CriptoAttività non omogeneo) :
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 3 (Acquisto criptoattivita)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione 
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 4 (Vendita criptoattività)
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a "" 
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 5 o 6 (Deposito o Prelievo x spostamento tra Wallet di Proprietà)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=2 :    Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
                TipologiaPlus=1 :   Il campo plusvalenza va compilato con il ValoreTransazione
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione 
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a ""  
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 9 (Deposito forzato a costo di carico zero)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=0 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=0 :   Costo Lifo Moneta Entrante = Zero
        Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare))
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a "" 
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        
        
        */