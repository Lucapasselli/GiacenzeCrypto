/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.DecimaliCalcoli;
import static giacenze_crypto.com.CDC_Grafica.Funzioni_Tabelle_PulisciTabella;
import static giacenze_crypto.com.CDC_Grafica.Funzioni_isNumeric;
import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import static giacenze_crypto.com.Calcoli_Plusvalenze.RitornaTipoCrypto;
import java.awt.Cursor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_RT {
    
    
    public static Map<String,BigDecimal[]> CalcoliPlusvalenzeXAnno(){
        // DefaultTableModel ModelloTabellaRT = (DefaultTableModel) RT_Tabella_Principale.getModel();
      //  Funzioni_Tabelle_PulisciTabella(ModelloTabellaRT);
      Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();//Wallet - Mappa(Moneta - Stack)
        Map<String, ArrayDeque> CryptoStack;// = new TreeMap<>(); Moneta - Stack
        
        //controllo se devo o meno prendere in considerazione i gruppi wallet per il calcolo della plusvalenza
        boolean PlusXWallet=false;
        String PlusXW=DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if(PlusXW!=null && PlusXW.equalsIgnoreCase("SI")){
            PlusXWallet=true;
        }
        String Anno;
        Map<String,BigDecimal[]> PlusvalenzeXAnno = new TreeMap<>(); 
        //BigDecimal[] così composto
        //0 - Anno
        //1 - Tot. Costi di Carico
        //2 - Tot. Realizzato
        //3 - Tot. Plusvalenza
        //4 - Tot. Plusvalenza non realizzata/latente
        //5 - Errori?
        //String = Anno
        BigDecimal Plusvalenza = new BigDecimal("0");
        BigDecimal CostiCarico = new BigDecimal("0");
        BigDecimal Vendite = new BigDecimal("0");
     //   setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        for (String[] v : MappaCryptoWallet.values()) {
            //if (Anno.isBlank())
            Anno=v[1].split("-")[0];
            
            //String AnnoTrans =v[0].split("-")[0];
            String TipoMU = RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
            String TipoME = RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
            //String IDTransazione=v[0];
           // String IDTS[]=IDTransazione.split("_");
            String MonetaU=v[8];
            String QtaU=v[10];
            String CostoCaricoU=v[16];
            String MonetaE=v[11];
            String QtaE=v[13];
            String CostoCaricoE=v[17];
            String Valore=v[15];
            

            //Identifico e creo una o più mappe per il cryptostack a seconda che sia o meno gestiti gli stack divisi per Wallet
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
                if(!PlusXWallet)GruppoWallet="Wallet 01";
                if (MappaGrWallet_CryptoStack.get(GruppoWallet) == null) {
                    //se non esiste ancora lo stack lo creo e lo associo alla mappa
                    CryptoStack = new TreeMap<>();
                    MappaGrWallet_CryptoStack.put(GruppoWallet, CryptoStack);
                } else {
                    //altrimenti lo recupero per i calcoli
                    CryptoStack = MappaGrWallet_CryptoStack.get(GruppoWallet);
                }
            
                
                
            //ATTENZIONE : vedere come gestire i trasferimenti
            
            //Se ci sono token in uscita, non sono FIAT e hanno un costo di carico gli tolgo dallo stack (costo di carico e prezzo li leggo dalla mappa)
            //Se non hanno costo di carico significa infatti che sono movimenti irrilevanti quali traferimenti interni o tra wallet dello stesso gruppo
            if (!TipoMU.isBlank()&&!TipoMU.equalsIgnoreCase("FIAT")&&!CostoCaricoU.isBlank()){
                //Tolgo il token dallo stack se non sono PTW, i PTW infatti vengono scaricati nel momento in cui arrivano a detinazione
                //e solo se si è deciso di distinguere le plusvalenze per gruppo wallet altrimenti vengono considerati alla stregua di un trasferimento interno
                //PTW sono prelievi che vanno ad altro wallet di proprietà
                //in ogni caso non vanno trasferiti in questa sezione
                if (!v[18].contains("PTW")) StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
            }
            
            //Se ci sono token in entrata gli aggiungo allo stack (costo di carico e prezzo li leggo dalla mappa che ha già tutti i dati)
            if (!TipoME.isBlank()&&!TipoME.equalsIgnoreCase("FIAT")&&!CostoCaricoE.isBlank()){

                  StackLIFO_InserisciValore(CryptoStack,MonetaE,QtaE,CostoCaricoE);
                  
                  if (v[18].contains("DTW")){
                      //Se è un DTW devo quindi anche scaricare il movimento PTW contrario
                      //se è attiva la funzione delle divisione delle plusvalenze per gruppo
                      //e se lo scambio avviene tra wallet di gruppi diversi
                      //Recupero quindi il tutto dalla funzione che c'è nel calcolo delle pluvalenze
                      String ris[]=Calcoli_Plusvalenze.RitornaIDeGruppoControparteSeGruppoDiverso(v);
                      String IDControparte=ris[0];
                      String WalletControparte=ris[0];
                      //IDControparte è null e i wallet di origine e destinazione sono uguali o se
                      //non è attiva la funzione per separare le plusvalenze per wallet
                      if (IDControparte!=null){
                          Map<String, ArrayDeque> CryptoStack2=MappaGrWallet_CryptoStack.get(WalletControparte);
                          String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IDControparte);
                          StackLIFO_TogliQta(CryptoStack2,Mov[8],Mov[10],true);
                      }
                      
                  }
               // Se non c'è costo di carico significa che sono trasferimenti non contabilizzati per cui li escludo dai calcoli
               //da controllare bene la cosa

            }
            
            
            
            //Se inoltre è un movimento fiscalmente rilevante mi salvo costo di carico, valore transazione e plusvalenza divisa per anno
            //dobbiamo controllare i cashback come vengono gestiti.
            if (v[33].equals("S")) {
                //Per prima cosa verifico se l'anno è vuoto e in quel caso lo valorizzo
                BigDecimal PlusAnno[];
                if (PlusvalenzeXAnno.get(Anno)==null)
                {
                    PlusAnno=new BigDecimal[6];   
                    //System.out.println(Anno);
                    PlusAnno[0]=new BigDecimal(Anno);//Anno
                    PlusAnno[1]=new BigDecimal(0);//Costo Carico
                    PlusAnno[2]=new BigDecimal(0);//Realizzato
                    PlusAnno[3]=new BigDecimal(0);//Plusvalenza
                    PlusAnno[4]=new BigDecimal(0);//Plusvalenza Latente
                    PlusAnno[5]=new BigDecimal(0);//Errori
                    PlusvalenzeXAnno.put(Anno, PlusAnno);
                    
                    
                }
                else
                {
                    PlusAnno=PlusvalenzeXAnno.get(Anno);
                }
                if (Funzioni_isNumeric(v[19], false)) {
                    Plusvalenza = Plusvalenza.add(new BigDecimal(v[19]));
                    PlusAnno[3] = PlusAnno[3].add(new BigDecimal(v[19]));
                }
                if (!v[15].isEmpty()) {
                    Vendite = Vendite.add(new BigDecimal(v[15]));
                    PlusAnno[2] = PlusAnno[2].add(new BigDecimal(v[15]));
                }
                if (!v[16].isEmpty()) {
                    CostiCarico = CostiCarico.add(new BigDecimal(v[16]));
                    PlusAnno[1] = PlusAnno[1].add(new BigDecimal(v[16]));
                }
            }
            

                                            
        }
       return PlusvalenzeXAnno;
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
        while (qtaRimanente.compareTo(new BigDecimal ("0"))>0 && !stack.isEmpty()){ //in sostanza fino a che la qta rimanente è maggiore di zero oppure ho finito lo stack
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
                String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,DecimaliCalcoli+10,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
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
     
     
}
