/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_Plusvalenze {
    
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
    
    public static int CategorizzaTransazione(String[] movimento){
//Questa prima funzione si occupa di categorizzare le movimentazioni in 10 Categorie
//Leggi la documentazione in fondo alla classe per le spiegazioni del caso

           // String TipoMU = movimento[9].trim();
            //String TipoME = movimento[12].trim();
            String TipoMU = RitornaTipoCrypto(movimento[8].trim(),movimento[1].trim(),movimento[9].trim());
            String TipoME = RitornaTipoCrypto(movimento[11].trim(),movimento[1].trim(),movimento[12].trim());
          /*  System.out.println(TipoMU);
            System.out.println(TipoMU);
            System.out.println("------");*/
            String IDTransazione=movimento[0];
            String IDTS[]=IDTransazione.split("_");
            int Tipologia = 0;
         /*   int TipologiaPlus=0;
            int TipologiaNCC=0;//Valore nuovo costo di carico
            int TipologiaStackLIFOVecchioCosto=0;
            int TipologiaStackLIFONuovoCosto=0;*/
            
            //TIPOLOGIA = 1 
            if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")//non devono essere fiata
                    && TipoMU.equalsIgnoreCase(TipoME)&&//moneta uscita e entrata dello stesso tipo
                    !TipoMU.isBlank() && !TipoME.isBlank()) //non devno essere campi nulli (senza scambi)
            {
                Tipologia = 1; //ScambioCryptoAttività medesime caratteristiche
            } 
            
            //TIPOLOGIA = 2
          /*  else if ((TipoMU.equalsIgnoreCase("NFT") && TipoME.equalsIgnoreCase("Crypto"))
                    || (TipoMU.equalsIgnoreCase("Crypto") && TipoME.equalsIgnoreCase("NFT"))) */
            else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")
                    && !TipoMU.equalsIgnoreCase(TipoME)&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                Tipologia = 2; //Scambio CryptoAttività diverse Caratteristiche 
            }
            
            //TIPOLOGIA = 3
            else if (TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                Tipologia = 3; //Acquisto CriptoAttività
            }
            
            //TIPOLOGIA = 4
            else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                Tipologia = 4; //Vendita CryptoAttività
            } 
            
            
            //TIPOLOGIA = 5 , 7 e 9 -> Deposito Criptoattività di vario tipo
            else if (TipoMU.isBlank() && !TipoME.equalsIgnoreCase("FIAT")) 
            {
                //Se arrivo qua vuol dire che questo è un deposito, poi a secondo di che tipo di deposito è
                //valorizzo la tipologia corretta
                if(IDTS[4].equalsIgnoreCase("RW")||movimento[18].contains("DAI")) Tipologia = 7;//Deposito Criptoattività x rewards, stacking,cashback etc...
                else if (IDTS[4].equalsIgnoreCase("TI")||movimento[18].isBlank()||movimento[18].contains("DTW")) Tipologia = 5; //Deposito Criptoattività x spostamento tra wallet
                else if(movimento[18].contains("DCZ")) Tipologia = 9;//Deposito a costo di carico zero
                else if(movimento[18].contains("DAC")) Tipologia = 3;//Acquisto Crypto
            } 
            
            //TIPOLOGIA = 6 , 8 e 10 -> Prelievo Criptoattività di vario tipo
            else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.isBlank()) 
            {
                //Se arrivo qua vuol dire che questo è un Prelievo, poi a secondo di che tipo di deposito è
                //valorizzo la tipologia corretta
                Tipologia = 6; //Deposito CriptoAttività x spostamento tra wallet
                if(IDTS[4].equalsIgnoreCase("RW"))Tipologia = 4;//Significa che sto facendo il rimborso di un cashback o altro quindi lo considero come vendita
                else if(IDTS[4].equalsIgnoreCase("CM")||movimento[18].contains("PCO")) Tipologia = 8;//Prelievo Criptoattività x servizi, acquisto beni etc...
                else if (IDTS[4].equalsIgnoreCase("TI")||movimento[18].isBlank()||movimento[18].contains("PTW")) Tipologia = 6; //Prelievo Criptoattività x spostamento tra wallet
                else if(movimento[18].contains("PWN")) Tipologia = 10;//(Prelievo a plusvalenza Zero ma toglie dal Lifo) 
            } 
            //TIPOLOGIA = 11 -> Deposito FIAT o Prelievo FIAT
            else if ((TipoMU.isBlank() && TipoME.equalsIgnoreCase("FIAT"))||(TipoME.isBlank() && TipoMU.equalsIgnoreCase("FIAT"))) 
            {
                Tipologia = 11;
            }
            else {
                System.out.println("Classe:Plusvalenze - Funzione:CategorizzaTransazione - Nessuna Tipologia Individuata");
                System.out.println(TipoMU+" - "+TipoME);
            }
//System.out.println(Tipologia);
       return Tipologia;
       }
 
    
    
    
    /**
     *
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
    public static int[] RitornaTipologieCalcoli(int Tipologia) {
        int tipo[] = new int[5];
        int tipoPlus=0;
        int tipoNCC=0;//Nuovo Costo di Carico
        int tipoRimuovoStack=0;
        int tipoInseriscoStack=0;
        int tipoVecchioCostoCarico=0;
        switch (Tipologia) {
            case 1 -> {
                tipoPlus = 0;
                tipoNCC = 2;
                tipoRimuovoStack = 1;
                tipoInseriscoStack = 2;
                tipoVecchioCostoCarico=2;
            }
            case 2 -> {
                tipoPlus = 2;
                tipoNCC = 3;
                tipoRimuovoStack = 1;
                tipoInseriscoStack = 3; 
                tipoVecchioCostoCarico=2;
            }
            case 3 -> {
                tipoPlus = 0;
                tipoNCC = 3;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 3;
                tipoVecchioCostoCarico=1;
            }
            case 4 -> {
                tipoPlus = 2;
                tipoNCC = 1;
                tipoRimuovoStack = 1;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=2;
            }
            case 5 -> {
                tipoPlus = 0;
                tipoNCC = 1;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=1;
            }
            case 6 -> {
                tipoPlus = 0;
                tipoNCC = 1;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=1;
            }
            case 7 -> {
                tipoPlus = 1;
                tipoNCC = 3;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 3;
                tipoVecchioCostoCarico=1;
            }
            case 8 -> {
                tipoPlus = 2;
                tipoNCC = 1;
                tipoRimuovoStack = 1;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=2;
            }
            case 9 -> {
                tipoPlus = 0;
                tipoNCC = 0;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 0;
                tipoVecchioCostoCarico=1;
            }
            case 10 -> {
                tipoPlus = 0;
                tipoNCC = 1;
                tipoRimuovoStack = 1;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=2;
            }
            case 11 -> {
                tipoPlus = 0;
                tipoNCC = 1;
                tipoRimuovoStack = 0;
                tipoInseriscoStack = 1;
                tipoVecchioCostoCarico=1;
            }
            default -> {
               
            }
            //qui si va solo in caso la scelata sia nessuna
            }
        tipo[0]=tipoPlus;
        tipo[1]=tipoNCC;
        tipo[2]=tipoRimuovoStack;
        tipo[3]=tipoInseriscoStack;
        tipo[4]=tipoVecchioCostoCarico;
        return tipo;
    }
    
 public static String StackLIFO_TogliQta(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,boolean toglidaStack) {
    
    //come ritorno ci invio il valore della movimentazione
    String ritorno="0.00";
    if (!Qta.isBlank()&&!Moneta.isBlank()){//non faccio nulla se la momenta o la qta non è valorizzata
    ArrayDeque<String[]> stack;

    BigDecimal qtaRimanente=new BigDecimal(Qta).abs();
    BigDecimal costoTransazione=new BigDecimal("0");
    //prima cosa individuo la moneta e prendo lo stack corrispondente
    if (CryptoStack.get(Moneta)==null){
        //ritorno="0";
    }else{
          //  System.out.println("OKokokokoko");
            
        if (toglidaStack)stack=CryptoStack.get(Moneta);
        else{
            ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
            stack=stack2.clone();
        }
        /*ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
        stack=stack2.clone();*/
        //System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
        while (qtaRimanente.compareTo(new BigDecimal ("0"))>0 && stack.size()>0){ //in sostanza fino a che la qta rimanente è maggiore di zero oppure ho finito lo stack
           // System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
            String ultimoRecupero[];
            ultimoRecupero=stack.pop();
            BigDecimal qtaEstratta=new BigDecimal(ultimoRecupero[1]).abs();
            BigDecimal costoEstratta=new BigDecimal(ultimoRecupero[2]).abs();
         /*  if (Moneta.equalsIgnoreCase("usdt")){ 
                System.out.println(ultimoRecupero[1]+" - "+ultimoRecupero[2]+" - "+stack.size());
                System.out.println(qtaRimanente);
                }*/
            //System.out.println(qtaEstratta+" - "+costoEstratta);
            if (qtaEstratta.compareTo(qtaRimanente)<=0)//se qta estratta è minore o uguale alla qta rimanente allora
                {
                //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
                qtaRimanente=qtaRimanente.subtract(qtaEstratta);
                //System.out.println(qtaRimanente);
                //recupero il valore di quella transazione e la aggiungo al costoTransazione
                costoTransazione=costoTransazione.add(costoEstratta);
            }else{
                //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
                //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
                //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
                //non ho più qta rimanente
                String qtaRimanenteStack=qtaEstratta.subtract(qtaRimanente).toPlainString();
                //System.out.println(qtaRimanenteStack);
               // System.out.println(qtaEstratta+" - "+qtaRimanente+"- "+qtaRimanenteStack);
                String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,30,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).stripTrailingZeros().toPlainString();
                String valori[]=new String[]{Moneta,qtaRimanenteStack,valoreRimanenteSatck};
                stack.push(valori);
                costoTransazione=costoTransazione.add(costoEstratta.subtract(new BigDecimal(valoreRimanenteSatck)));
                qtaRimanente=new BigDecimal("0");//non ho più qta rimanente
            }
            
        }
        //pop -> toglie dello stack l'ultimo e recupera il dato
        //peek - > recupera solo il dato

    }
    ritorno=costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }else return "";
   // System.out.println(ritorno);
    return ritorno;
   // System.out.println(Moneta +" - "+stack.size());
}      
    
   public static void StackLIFO_InserisciValore(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String Valore) {
    
    ArrayDeque<String[]> stack;
    String valori[]=new String[3];
    valori[0]=Moneta;
    valori[1]=new BigDecimal(Qta).abs().toPlainString();
    valori[2]=Valore;
    if (CryptoStack.get(Moneta)==null){
        stack = new ArrayDeque<String[]>();
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }else{
        stack=CryptoStack.get(Moneta);
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }
   // System.out.println(Moneta +" - "+stack.size());
}
    
     public static void AggiornaPlusvalenze(){
////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere

       // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();
        Map<String, ArrayDeque> CryptoStack;// = new TreeMap<>();
        
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
                if (MappaGrWallet_CryptoStack.get(GruppoWallet) == null) {
                    //se non esiste ancora lo stack lo creo e lo associo alla mappa
                    CryptoStack = new TreeMap<>();
                    MappaGrWallet_CryptoStack.put(GruppoWallet, CryptoStack);
                } else {
                    //altrimenti lo recupero per i calcoli
                    CryptoStack = MappaGrWallet_CryptoStack.get(GruppoWallet);
                }
            String TipoMU = RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
            String TipoME = RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
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
            long long2023=OperazioniSuDate.ConvertiDatainLongMinuto("2023-01-01 00:00");
            long dataLong=OperazioniSuDate.ConvertiDatainLongMinuto(v[1]);
            boolean DataSuperiore2023=true;
            if (dataLong<long2023)DataSuperiore2023=false;
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
            if (IDTS[4].equals("VC")){
                //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                
                //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                NuovoPrezzoCarico="";
                
                //Calcolo la plusvalenza
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString(); 
            }           
            //TIPOLOGIA = 1  (Scambio Cripto Attività medesime Caratteristiche)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")//non devono essere fiata
                    && TipoMU.equalsIgnoreCase(TipoME)&&//moneta uscita e entrata dello stesso tipo
                    !TipoMU.isBlank() && !TipoME.isBlank()) //non devno essere campi nulli (senza scambi)
            {
                
                if (DataSuperiore2023||!Pre2023ScambiRilevanti){//se la data è superiore al 2023 oppure gli scambi pre 2023 non voglio renderli rilvenati
                    //Tolgo dallo stack il costo di carico della cripèto uscita
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                    
                    //Inserisco il costo di carico nello stack della cripto entrata
                    NuovoPrezzoCarico=VecchioPrezzoCarico;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    //La plusvalenza va valorizzata a zero
                    Plusvalenza="0.00";
                 }else {//altrimenti calcolo la plusvalenza
                    //Tolgo dallo stack il vecchio costo di carico
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                    
                    //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                    //lo aggiungo quindi allo stack del lifo
                    NuovoPrezzoCarico=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                }                                      
            } 
            
            
            //TIPOLOGIA = 2 (Scambio Cripto Attività Diverse Caratteristiche)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")
                    && !TipoMU.equalsIgnoreCase(TipoME)&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                    //Tolgo dallo stack il vecchio costo di carico
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                    
                    //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                    //lo aggiungo quindi allo stack del lifo
                    NuovoPrezzoCarico=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                                       
            }
            
            
            //TIPOLOGIA = 3 (Acquisto di Cripto attività tramite FIAT)
            else if (TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                
                    NuovoPrezzoCarico=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    Plusvalenza="0.00";
                                         
                    VecchioPrezzoCarico=""; 
                    
                    
                    
            }
            
            //TIPOLOGIA = 4 (Vendita Criptoattività per FIAT)
            else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.equalsIgnoreCase("FIAT")&&
                    !TipoMU.isBlank() && !TipoME.isBlank())  
            {
                //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                
                //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                NuovoPrezzoCarico="";
                
                //Calcolo la plusvalenza
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();                
                 
            } 
            
            
            //TIPOLOGIA = 5 , 7 e 9 -> Deposito Criptoattività di vario tipo
            else if (TipoMU.isBlank() && !TipoME.equalsIgnoreCase("FIAT")) 
            {
                //Se arrivo qua vuol dire che questo è un deposito, poi a secondo di che tipo di deposito è
                //valorizzo la tipologia corretta
                
                //TIPOLOGIA = 7; ( Deposito Criptoattività x rewards, stacking,cashback etc... - Plusvalenza immediata)
                if (IDTS[4].equalsIgnoreCase("RW") || v[18].contains("DAI")) {

                    if (DataSuperiore2023 || !Pre2023EarnCostoZero) {
                        NuovoPrezzoCarico = Valore;

                        Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico);

                        Plusvalenza = Valore;

                        VecchioPrezzoCarico = "";
                    } else {
                        NuovoPrezzoCarico = "0.00";
                        Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico);

                        Plusvalenza = "0.00";

                        VecchioPrezzoCarico = "";
                    }

                }

                //Tipologia = 5; (Deposito Criptoattività x spostamento tra wallet)
                else if (IDTS[4].equalsIgnoreCase("TI") || v[18].isBlank() || v[18].contains("DTW")) {

                    //il compito è trovare la controparte del movimento qualora questa si riferisse ad un diverso gruppo wallet
                    //e da li spostare il costo di carico
                    String IDControparte = null;
                    String GruppoWalletControparte = null;
                    //comincio impostando le prime condizioni
                    //v[20] non deve essere nullo ovvero devo avere transazioni allegate
                    //v[18] deve essere un deposito derivante da trasferimenti e deve essere o un movimento importato o uno manuale (v[22]=ad A o M)
                    if (!v[20].isBlank() && v[18].contains("DTW") && (v[22].equals("A") || v[22].equals("M"))) {

                        //Se è un movimento di Trasferimento tra wallet (2 o 3 movimenti a seconda se ci sono le commissioni) il movimento controparte è l'unico PTW
                        //Se è un movimento di scambio differito (5 movimenti) il movimento controparte è un PTW classificato come AU (posizione 22)
                        //Tutto questo lo faccio però solo se il movimento di controparte PTW fa parte di un altro gruppo di wallet, altrimentio non faccio nulla.
                        String Movimenti[] = v[20].split(",");

                        if (Movimenti.length > 3)//Sono in presenza di uno scambio differito
                        {
                            for (String IdM : Movimenti) {
                                String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IdM);
                                //devo trovare la controparte che in questo caso è il movimento di prelievo creato automaticamente dal sistema
                                //inoltre vedo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                                //perchè se fanno parte dello stesso gruppo non devo fare nulla

                                if (Mov[18].contains("PTW") && Mov[22].contains("AU")
                                        && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]))) {
                                    IDControparte = IdM;
                                    GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]);
                                }
                            }
                        } else {//Scambio tra wallet

                            for (String IdM : Movimenti) {
                                String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IdM);
                                //devo trovare la controparte che in questo caso è l'unico movimento di prelievo
                                //inoltre vedo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                                //perchè se fanno parte dello stesso gruppo non devo fare nulla
                                if (Mov[18].contains("PTW")
                                        && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]))) {
                                    IDControparte = IdM;
                                    GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3]);
                                }

                            }

                        }

                    }
                    //Se ID controparte è diverso da null vuol dire che devo gestire il calcolo delle plusvalenze, altrimenti no
                    if (IDControparte != null) {
                        Plusvalenza = "0.00";
                        VecchioPrezzoCarico = "";
                        
                        //DA VEDERE PERCHE' IL CRYPTO STACK E' DIVERSO
                        String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IDControparte);
                    Map<String, ArrayDeque> CryptoStack2=MappaGrWallet_CryptoStack.get(GruppoWalletControparte);// = new TreeMap<>();
                    Mov[31]=v[1];
                    if (CryptoStack2==null){
                        NuovoPrezzoCarico="0.00";
                        } else {
                        NuovoPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack2,Mov[8],Mov[10],true);
                        Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    }

                    } else {
                        Plusvalenza = "0.00";

                        NuovoPrezzoCarico = "";

                        VecchioPrezzoCarico = "";
                    }

                }
                
                //Tipologia = 9; (Deposito a costo di carico zero)
                else if(v[18].contains("DCZ")){
                     
                     NuovoPrezzoCarico="0.00";
                     Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                     
                     Plusvalenza="0.00";
                     
                     VecchioPrezzoCarico="";
                }
                
                //Tipologia = 3; (Acquisto Crypto)
                else if(v[18].contains("DAC")){
                    
                    NuovoPrezzoCarico=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    Plusvalenza="0.00";
                                         
                    VecchioPrezzoCarico=""; 
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
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                
                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico="";
                
                    //Calcolo la plusvalenza
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();  
                }
                //Tipologia = 8;//Prelievo Criptoattività x servizi, acquisto beni etc... //per ora uguale alla tipologia 4
                else if(IDTS[4].equalsIgnoreCase("CM")||v[18].contains("PCO")){
                    
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                
                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico="";
                
                    //Calcolo la plusvalenza
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString(); 
                }
                //Tipologia = 6;//Prelievo Criptoattività x spostamento tra wallet
                else if (IDTS[4].equalsIgnoreCase("TI")||v[18].isBlank()||v[18].contains("PTW")) {
                                        
                     Plusvalenza="0.00";
                     
                     NuovoPrezzoCarico="";
                     
                     VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,false);
                } 
                
                //Tipologia = 10;//(Prelievo a plusvalenza Zero ma toglie dal Lifo)
                else if(v[18].contains("PWN")){
                    
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                    
                    NuovoPrezzoCarico="";
                    
                    Plusvalenza="0.00";                    
                    
                }
            } 
            //TIPOLOGIA = 11 -> Deposito FIAT o Prelievo FIAT
            else if ((TipoMU.isBlank() && TipoME.equalsIgnoreCase("FIAT"))||(TipoME.isBlank() && TipoMU.equalsIgnoreCase("FIAT"))) 
            {
                    
                    NuovoPrezzoCarico="";
                    
                    Plusvalenza="0.00";
                                           
                    VecchioPrezzoCarico="";
                    
            }
            else {
                System.out.println("Classe:Plusvalenze - Funzione:CategorizzaTransazione - Nessuna Tipologia Individuata");
                System.out.println(TipoMU+" - "+TipoME);
            }
            
            

                    v[16]=VecchioPrezzoCarico;
                    v[17]=NuovoPrezzoCarico;
                    v[19]=Plusvalenza;


        }
    }

    
       
         public static void AggiornaPlusvalenzeOLD(){
////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere

       // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, ArrayDeque> CryptoStack = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            int TipoMovimento=Calcoli_Plusvalenze.CategorizzaTransazione(v);
            int TipologieCalcoli[]=Calcoli_Plusvalenze.RitornaTipologieCalcoli(TipoMovimento);
            String MonetaU=v[8];
            String QtaU=v[10];
            String MonetaE=v[11];
            String QtaE=v[13];
            String Valore=v[15];
            String VecchioPrezzoCarico="0.00";
            String NuovoPrezzoCarico="0.00";
            String Plusvalenza="0.00";
            

            switch (TipologieCalcoli[2]) {//Qui analizzo se devo o meno cancellare dallo stack il vecchio costo
                case 0 -> {//Non tolgo dallo stack il vecchio costo di carico
                   
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,false);
                    //questa seconda casistica succede solo in presenza di depositi
                    if (VecchioPrezzoCarico.isBlank())VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaE,QtaE,false);
                }
                case 1 -> {//Tolgo dallo stack il vecchio costo di carico
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                }

            }
            switch (TipologieCalcoli[3]) {//Qui analizzo se devo e che valore devo inserire nello stack come nuovo costo di carico
                case 0 -> {
                    //il nuovo prezzo di carico ovviamente è valorizzato a Zero
                    NuovoPrezzoCarico="0.00";
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                }
                case 1 -> {
                    NuovoPrezzoCarico="";
                }
                case 2 -> {
                    NuovoPrezzoCarico=VecchioPrezzoCarico;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                }
                case 3 -> {
                    NuovoPrezzoCarico=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                }

            }
            switch (TipologieCalcoli[0]) {//Qui analizzo il calcolo della plusvalenza e mi comportio di conseguenza
                case 0 -> {
                    Plusvalenza="0.00";
                }
                case 1 -> {
                    Plusvalenza=Valore;
                }
                case 2 -> {
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(VecchioPrezzoCarico)).toPlainString();
                }
            }
            switch (TipologieCalcoli[1]) {//Qui analizzo il calcolo del costo di carico e mi comporto di conseguenza
                case 0 -> {
                    NuovoPrezzoCarico="0.00";
                }
                case 1 -> {
                    NuovoPrezzoCarico="";
                }
                case 2 -> {
                    NuovoPrezzoCarico=VecchioPrezzoCarico;
                }
                case 3 -> {
                    NuovoPrezzoCarico=Valore;
                }
            }
            switch (TipologieCalcoli[4]) {//Qui decido il Vecchio Costo di carico
                case 0 -> {
                    VecchioPrezzoCarico="0.00";
                }
                case 1 -> {
                    VecchioPrezzoCarico="";
                }
                case 2 -> {
                    //non faccio nulla resta valorizzato così com'è
                }

            }
                   // System.out.println("-"+VecchioPrezzoCarico+"-"+TipologieCalcoli[4]);
                    v[16]=VecchioPrezzoCarico;
                    v[17]=NuovoPrezzoCarico;
                    v[19]=Plusvalenza;
                    //System.out.println("--------------------------");

        }
    }
    
 
public void TransazioniCrypto_Funzioni_CategorizzaTransazionixPlusOld(){
     /* Questa funzione si occupa di categorizzare i movimenti in 6 macrocategorie che ne identificano il modo in cui :
        - Verrà calcolata la plusvalenza
        - Verrà calcolato il nuovo costo di carico
        - verrà gestito lo stack per il calcolo del lifo
        
        Le categorie in questione saranno le seguenti e saranno valorizzate con un numero:
        1 - Scambio tra Criptoattività Omogenee (medesime caratteristiche e funzioni)
            Mebri del gruppo:
                - Scambio NFT -> NFT
                - Scambio Crypto -> Crypto
            Caratteristiche : 
                - Plusvalenza = 0
                - Stack lifo : tolgo valore da moneta venduta e lo aggiungo a moneta entrata
                - Nuovo Costo di carico : Riporto costo di carico calcolato con il lifo dalla moneta uscita sulla moneta entrata
        2 - Scambio tra CriptoAttività non Omogenee
            Membri del gruppo:
                - Scambio NFT -> Crypto
                - Scambio Crypto -> NFT
            Caratteristiche :
                - Plusvalenza=Valore Transazione - Costo di carico moneta uscente calcolato con Lifo
                - Stack Lifo : - Rimuovo da stack costo moneta uscente
                               - Inserisco nello stack della moneta entrante il valore della transazione
                - Nuovo Costo di Carico = Valore della Transazione
        3 - Spostamenti tra wallet di proprietà
            Membri del gruppo:
                - NFT ->                (Spostamento verso wallet proprio)
                - Crypto ->             (Spostamento verso wallet proprio)
                -     -> NFT            (Spostamento verso wallet proprio)
                -     -> Crypto         (Spostamento verso wallet proprio)
            Caratteristiche :
                - Plusvalenza = 0
                - Stack Lifo : Non tocco nulla
                - Nuovo Costo di Carico = Vecchio costo di carico preso con il lifo 
        4 - Vendita CriptoAttività per FIAT o Servizi
            Membri del gruppo:
                - Scambio NFT -> FIAT
                - Scambio Crypto -> FIAT
                - NFT -> Servizio/Wallet non nostro
                - Crypto -> Servizio/Wallet non nostro
            Caratteristiche :
                - Plusvalenza=Valore Transazione - Costo di carico moneta uscente calcolato con Lifo
                - Stack Lifo : - Rimuovo da stack costo moneta uscente
                - Nuovo Costo di Carico = 0
        5 - Acquisto Crypto con Fiat
            Membri del gruppo:
                - Scambio FIAT -> NFT
                - Scambio FIAT -> Crypto 
            Caratteristiche :
                - Plusvalenza=0
                - Stack Lifo :  - Inserisco nello stack della moneta entrante il valore della transazione
                - Nuovo Costo di Carico = Valore FIAT o Transazione (Dovrebbero sempre equivalersi)
        5 - Proventi da detenzione
            Membri del gruppo:
                -      -> NFT           (Mining,Staking,rewards,cashback etc...)
                -      -> Crypto        (Mining,Staking,rewards,cashback etc...)
            Caratteristiche :
                - Plusvalenza = Valore Transazione
                - Stack Lifo :  - Inserisco nello stack della moneta entrante il Valore Transazione
                - Nuovo Costo di Carico = Valore Transazione
        */ 
     for (String[] v : MappaCryptoWallet.values()) {
         String TipoMU=v[9].trim();
         String TipoME=v[12].trim();
         int Tipologia=0;
         if((TipoMU.equalsIgnoreCase("Crypto")&&TipoME.equalsIgnoreCase("Crypto")) || 
                 (TipoMU.equalsIgnoreCase("NFT")&&TipoME.equalsIgnoreCase("NFT"))){
            Tipologia=1; //ScambioCryptoAttività medesime caratteristiche
         }
         else if((TipoMU.equalsIgnoreCase("NFT")&&TipoME.equalsIgnoreCase("Crypto")) || 
                 (TipoMU.equalsIgnoreCase("Crypto")&&TipoME.equalsIgnoreCase("NFT"))){
            Tipologia=2; //Scambio CryptoAttività dicverse Caratteristiche
         }
         else if(((TipoMU.equalsIgnoreCase("NFT")&&TipoME.equalsIgnoreCase("")) || 
                 (TipoMU.equalsIgnoreCase("Crypto")&&TipoME.equalsIgnoreCase("")) ||
                 (TipoMU.equalsIgnoreCase("")&&TipoME.equalsIgnoreCase("NFT")) ||
                 (TipoMU.equalsIgnoreCase("")&&TipoME.equalsIgnoreCase("Crypto"))) 
                 &&
                 ((v[18].contains("DTW")||v[18].contains("PTW"))||v[18].isBlank())){  
              Tipologia=3; //Spostamenti tra Wallet Propri
         }
         else if((TipoMU.equalsIgnoreCase("NFT")&&TipoME.equalsIgnoreCase("FIAT")) 
                 || 
                 (TipoMU.equalsIgnoreCase("Crypto")&&TipoME.equalsIgnoreCase("FIAT")) 
                 ||
                 (((TipoMU.equalsIgnoreCase("Crypto")&&TipoME.equalsIgnoreCase("")) || (TipoMU.equalsIgnoreCase("NFT")&&TipoME.equalsIgnoreCase(""))) && v[18].contains("PCO"))){  
              Tipologia=4; //Vendita per Fiat o Servizi
         }
         else if((TipoMU.equalsIgnoreCase("FIAT")&&TipoME.equalsIgnoreCase("Crypto")) 
                 || 
                 (TipoMU.equalsIgnoreCase("FIAT")&&TipoME.equalsIgnoreCase("NFT")) )
         {
             
             Tipologia=5; //Acquisto Crypto con Fiat
         }else if(((TipoMU.equalsIgnoreCase("")&&TipoME.equalsIgnoreCase("Crypto")) || (TipoMU.equalsIgnoreCase("")&&TipoME.equalsIgnoreCase("NFT")))
                 &&
                 v[18].contains("DAI"))
         {
             
             Tipologia=6; //Acquisto Crypto con Fiat
         }else System.out.println("Nessuna Tipologia Individuata");
         
         System.out.println("Tipologia Transazione : "+Tipologia);
           
         }
     
     
    }


   public void TransazioniCrypto_Funzioni_AggiornaPlusvalenzeOLDest(){
      //il comando seguente è da eliminare una volta appurato che la categorizzazione funziona
      //per ora mancano ancora delle cosucce da sistemare
        
        
            Deque<String[]> stack = new ArrayDeque<String[]>();
       // stack.push("a");
       // stack.push("b");
       // System.out.println(stack.size());
       // System.out.println(stack.pop());
       // System.out.println(stack.pop());
        Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            Calcoli_Plusvalenze.CategorizzaTransazione(v);
            
            //Plusvalenze.
            //Se deposito crypto non associato o prelievo crypto non associato non lo considero e lo salto - DC-PC()
            //Se è un trasferimento interno lo salto - TI
            //se è un trasferimento tra wallet lo salto - PC-DC(PTW-DTW)
            //se deposito airdrop lo considero - DC(DAI)
            //se deposito a zero lo considero - DC(DCZ)
            //se cashout lo levo e calcolo la plusvalenza - PC(PCO)
            //se è un prelievo sconosciuto lo tolgo dallo stack ma non calcolo plusvalenza. - PC(PWN)
            //se è uno scambio crypto tolgo la moneta venduta, aggiungo moneta acquistata e riporto valore LIFO su moneta acquistata - SC
            //se è deposito fiat lo salto - DF
            //se è acquisto crypto aggiungo a stack - AC
            //se è vendita crypto tolgo da stack e calcolo plus in base al lifo - VC
            //da completare con altre casistiche........................................................................
            String IDTransazione=v[0];
            String IDTS[]=IDTransazione.split("_");
            //per questa prima fase intanto ignoro tutti i prelievi, una volta finito di scrivere la funzione
            //riprenderò in mano questa parte per aggiungere tutte le casistiche
            if (IDTS[4].equalsIgnoreCase("DC")||IDTS[4].equalsIgnoreCase("DN")){//questo vale sia per deposito crypto che per nft
                //QUI VANNO GESTITI I DEPOSITI CRYPTO IN BASE ALLA TIPOLOGIA
                //Le tipologie sono:
                //DTW -> Trasferimento tra Wallet (deposito) (plusvalenza 0 e solo calcolo costo carico, nessun movimento sullo stack)
                //DAI -> Airdrop o similare (deposito) (plusvalenza=valore transazione, prezzo di carico = valore transazione)
                //DCZ -> Costo di carico 0 (deposito)  (no plusvalenza, costo di carico=0 aggiunto allo stack)
                //e senza classificazione che va trattato come fosse un DTW
                String Moneta=v[11];
                String Qta=v[13];
                String Valore=v[15];
                if (v[18].contains("DTW")||v[18].equalsIgnoreCase("")){
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                    v[19]="0.00";
                }else if(v[18].contains("DAI")){
                    v[17]=Valore;
                    v[19]=Valore;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                }else if(v[18].contains("DCZ")){
                    v[17]="0.00";
                    v[19]="0.00";
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta,Qta,"0.00");                    
                }
                //System.out.println(v[18]);
            }else if (IDTS[4].equalsIgnoreCase("PC")||IDTS[4].equalsIgnoreCase("PN")){ //questo vale sia per crypto che nft
             //QUI VANNO GESTITI I PRELIEVI CRYPTO IN BASE ALLA TIPOLOGIA  
                //Le tipologie sono:
                //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo) (lo elimino dai calcoli e non genero plusvalenze)
                //PCO -> Cashout o similare (prelievo) (plusvalenza calcolata + tolto valore dallo stack)
                //PTW -> Trasferimento tra Wallet (prelievo) (plusvalenza 0 e solo calcolo costo carico, nessun movimento sullo stack)
                //e senza classificazione che va trattato come fosse un PTW
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                String Valore=v[15];
                if (v[18].contains("PTW")||v[18].equalsIgnoreCase("")){
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                    v[19]="0.00";
                }else if(v[18].contains("PCO")){
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                    String Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                }else if(v[18].contains("PWN")){
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                    v[17]=PrzCarico;
                    v[19]="0.00";                    
                }
               // System.out.println(v[18]);

            }else if (IDTS[4].equalsIgnoreCase("DF")){ //Deposito Fiat
             //IN QUESTO CASO NON DEVO FARE NULLA
            }else if (IDTS[4].equalsIgnoreCase("PF")){ //Prelievo Fiat
             //IN QUESTO CASO NON DEVO FARE NULLA   
            }else if (IDTS[4].equalsIgnoreCase("AC")||IDTS[4].equalsIgnoreCase("AN")){ //Acquisto Crypto o NFT
                //se compro tramite FIAT non genero plusvalenza
                if (v[9].trim().equalsIgnoreCase("FIAT")){
                    String Moneta=v[11];
                    String Qta=v[13];
                    String Valore=v[15];
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                    v[17]=v[15];
                    v[19]="0.00";
                }else{
                   //in questo caso vuol dire che l'acquisto in realtà è uno scambio eterogeneo che genera plusvalenza
                   //e devo gestirlo
                    String Moneta=v[8];
                    String Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico;
                    String Plusvalenza;
                    String Valore=v[15];
                    PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true); 
                    Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                    String Moneta2=v[11];
                    String Qta2=v[13];
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta2,Qta2,PrzCarico);
                }
                
            }else if (IDTS[4].equalsIgnoreCase("VC")||IDTS[4].equalsIgnoreCase("VN")){ //Vendita Crypto o NFT
                String Moneta=v[8];
                String Qta=v[10];
                String Valore=v[15];
                String PrzCarico;
                String Plusvalenza;
                PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]=Plusvalenza;
                
            }else if (IDTS[4].equalsIgnoreCase("SC")||IDTS[4].equalsIgnoreCase("SN")){//Scambio Crypto o NFT
                //nel caso degli scambi, prima recupero il valore dallo stack per la moneta venduta
                //poi quel valore lo metto nello stack per la moneta acquistata
                //e quindi lo stesso vaolre lo scrivo sulla riga dello scambio
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                //System.out.println(Moneta+" - "+Qta);
                //String Valore=v[15];
                String PrzCarico;
                //String Plusvalenza;
                PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                //Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]="0.00";  
                String Moneta2=v[11];
                String Qta2=v[13];
                //System.out.println(Moneta2+" - "+Qta2+" - "+PrzCarico);
                Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta2,Qta2,PrzCarico);
                
            }else if (IDTS[4].equalsIgnoreCase("RW")){ //Reward varie
                //IN QUESTO CASO DEVO SOLO INSERIRE IL DATO NELLO STACK
                String Moneta=v[11];
                String Qta=v[13];
               // if (Qta.equalsIgnoreCase("")) System.out.println(v[13]+" - "+v[0]);
                String Valore=v[15];
                //se esiste la qta in quella possizione vuol dire che è una rewad, altrimenti è un rimborso di una reward
                if (!Qta.equalsIgnoreCase(""))
                    {
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, Moneta,Qta,Valore);
                    }
                else{
                    Moneta=v[8];
                    Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                    String Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                    v[17]=PrzCarico;
                    v[19]=Plusvalenza;
                }
                
            }else if (IDTS[4].equalsIgnoreCase("TI")){ //Trasferimento interno
                //IN QUESTO CASO dovrei solo calcolare il prezzo di carico ma senza togliere nulla dallo stack
                String Moneta=v[11];
                String Qta=v[13];
                if (!Qta.equalsIgnoreCase("")){
                String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,false);
                v[17]=PrzCarico;
                }
                else {
                    Moneta=v[8];
                    Qta=new BigDecimal(v[10]).abs().toPlainString();
                    String PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,false);
                    v[17]=PrzCarico;
                }
            }else if (IDTS[4].equalsIgnoreCase("CM")){ //Commissioni
                String Moneta=v[8];
                String Qta=new BigDecimal(v[10]).abs().toPlainString();
                String Valore=v[15];
                String PrzCarico;
                String Plusvalenza;
                PrzCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,Moneta,Qta,true);
                Plusvalenza=new BigDecimal(Valore).subtract(new BigDecimal(PrzCarico)).toPlainString();
                v[17]=PrzCarico;
                v[19]=Plusvalenza;                
            }else { //Qualcosa di non contemplato
                System.out.println(IDTransazione);
            }

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
   public static String RitornaTipoCrypto(String Token,String Data,String Tipologia) {
       String Tipo=Tipologia;
       String DataEmoney=CDC_Grafica.Mappa_EMoney.get(Token);
       if(Tipologia.equalsIgnoreCase("Crypto")&&DataEmoney!=null){
           long dataemoney=OperazioniSuDate.ConvertiDatainLong(DataEmoney);
           long datascambio=OperazioniSuDate.ConvertiDatainLong(Data);
           if (datascambio>=dataemoney) Tipo="EMoney";
       }
       return Tipo;
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