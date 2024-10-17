 /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;





import static giacenze_crypto.com.CDC_Grafica.DecimaliCalcoli;
import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */


public class Calcoli_RW {
    
       static String AnnoR;
       static Map<String, String> MappaGruppo_IDPrimoMovimento = new TreeMap<>();//la mappa è così composta, (Gruppo,ID Primo Movimento)
       
       
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
       
    public static boolean ScambioRilevante(Moneta[] m,String Data,String Rilevanza){

        //Rilevanza A = Solo Valori iniziali e finali (ancora da implementare)!!!!!
        //Rilevanza B = Solo scambi con FIAT
        //Rilevanza C = Solo scambi rilevanti fiscalmente
        //Rilevanza D = Tutti gli scambi 
        if (Rilevanza.equalsIgnoreCase("D")) {
            return true;
        } else if (Rilevanza.equalsIgnoreCase("B")) {
            return false;
        } else {
            String Tipo1 = RitornaTipoCrypto(m[0].Moneta, Data, m[0].Tipo);
            String Tipo2 = RitornaTipoCrypto(m[1].Moneta, Data, m[1].Tipo);
            //Se Tipo1 è uguale a Tipo2 ritorna Falso altrimenti vero
            return !Tipo1.equalsIgnoreCase(Tipo2);
        }
    }
   
       
    
public static void StackLIFO_InserisciValoreFR(Map<String, ArrayDeque> CryptoStack,String GruppoWallet,ElementiStack el) {
    
    //System.out.println(Moneta+" <> "+Qta);

    ArrayDeque<ElementiStack> stack;
    
   // if (el.QtaOri.isBlank())System.out.println("Trovato inghippo");

    if(!el.Qta.contains("-")){//Inserisco nello stack solo valori positivi, i token con giacenze negative ovviamente non li inserisco
        if (CryptoStack.get(el.Moneta)==null){
            stack = new ArrayDeque<ElementiStack>();
            stack.push(el);
            CryptoStack.put(el.Moneta, stack);
        }else{
            stack=CryptoStack.get(el.Moneta);
            stack.push(el);
            CryptoStack.put(el.Moneta, stack);
        }
    }
    else{// se invece contiene una giacenza negativa la inserisco subito in lista per far verificare gli erorri
            
                
        
                String xlista[]=new String[17];
                
                xlista[0]=AnnoR;                    //Anno RW
                xlista[1]=GruppoWallet;             //Gruppo Wallet Inizio
                xlista[2]=el.Moneta;                   //Moneta Inizio
                xlista[3]=el.Qta;                      //Qta Inizio
                xlista[4]=el.DataOri;               //Data Inizio
                xlista[5]="0.000";                  //Prezzo Inizio
                xlista[6]=GruppoWallet;             //GruppoWallet Fine
                xlista[7]=el.Moneta;                   //Moneta Fine
                xlista[8]=el.Qta;                      //Qta Fine
                xlista[9]="0000-00-00 00:00";       //Data Fine
                xlista[10]="0.000";                 //Prezzo Fine
                xlista[11]="365";                     //Giorni di Detenzione
                xlista[12]="Inizio Periodo";        //Causale
                xlista[13]=el.IDOri;                //ID Movimento Apertura (o segnalazione inizio anno)
                xlista[14]="";                     //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                xlista[15]="Errore (Giacenza Negativa)";                      //Tipo Errore
                xlista[16]="";                               //Lista ID coinvolti separati da virgola

                
                List<String[]> ListaRW;
                ListaRW=CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                ListaRW.add(xlista);
                //Verifico Ora se esistono i prezzi unitari del token con data iniziale e del token con data finale.
                //Se esistono li scrivo nella lista come campo 13 e 14
                
                //Qui gestisco gli errori
                //Gli errori non li segnalo se il token in questione è stato identificato come Scam
                
                }
}
       
       

    
      public static ArrayDeque<ElementiStack> StackLIFO_TogliQtaFR (Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String GruppoWallet,boolean toglidaStack) {
    
    //in ritorno devo avere la lista delle qta estratte  e valore con relative date
    //es. ListaRitorno[0]=1,025;15550;2023-01-01 00:00   (qta iniziale;valore iniziale;data iniziale)

    ArrayDeque<ElementiStack> stackRitorno = new ArrayDeque<ElementiStack>();//è lo stack con le nuove movimentazioni da inserire
   // String ritorno="0.00";
    if (!Qta.isBlank()&&!Moneta.isBlank()){//non faccio nulla se la momenta o la qta non è valorizzata
    ArrayDeque<ElementiStack> stack;
    
    BigDecimal qtaRimanente=new BigDecimal(Qta).abs();
   // BigDecimal costoTransazione=new BigDecimal("0");
    //prima cosa individuo la moneta e prendo lo stack corrispondente
    if (CryptoStack.get(Moneta)!=null){   
        if (toglidaStack)   stack=CryptoStack.get(Moneta);
        else{
            ArrayDeque<ElementiStack> stack2=CryptoStack.get(Moneta);
            stack=stack2.clone();
        }
 
        while (qtaRimanente.compareTo(new BigDecimal ("0"))>0 && !stack.isEmpty()){ //in sostanza fino a che la qta rimanente è maggiore di zero oppure ho finito lo stack
           //System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
          
           //if (Qta.equals("6.584998"))System.out.println("porcaletta");
            ElementiStack ultimoRecupero;
           //System.out.println(stack.size());
           //System.out.println(Moneta);
            ultimoRecupero=stack.pop();
           // System.out.println(Moneta);
           // System.out.println("-----------");
            
            BigDecimal qtaEstratta=new BigDecimal(ultimoRecupero.Qta).abs();
          //  BigDecimal costoEstratta=new BigDecimal(ultimoRecupero.Valore).abs();
            String dataOrigine=ultimoRecupero.DataOri;
            String MonetaOrigine=ultimoRecupero.MonOri;
            //System.out.println("a-"+ultimoRecupero.QtaOri+"b-"+ultimoRecupero.MonOri);
           // if(ultimoRecupero.QtaOri.isBlank())System.out.println("---"+ultimoRecupero.Moneta+ultimoRecupero.Data);
            BigDecimal QtaOrigine;
            if (!ultimoRecupero.QtaOri.isBlank())QtaOrigine=new BigDecimal(ultimoRecupero.QtaOri).abs();//Questo può essere non valorizzato nel caso in cui ci siano delle giacenze negative
            else QtaOrigine=new BigDecimal(0);
            BigDecimal CostoOrigine=new BigDecimal(ultimoRecupero.CostoOri).abs();//Questo può essere non valorizzato nel caso in cui ci siano delle giacenze negative
            String IDOrigine=ultimoRecupero.IDOri;
            String GW=ultimoRecupero.GruppoWalletOri;
            String listaID=ultimoRecupero.ListaIDcoinvolti;
            String TipoOri=ultimoRecupero.TipoMonetaOri;


            //QtaEstratta è la qta estratta dallo stack non è la qta che voglio estrarre
            //la qta che voglio estrarre è la QtaRimanente
            if (qtaEstratta.compareTo(qtaRimanente)<=0)//se qta estratta è minore o uguale alla qta rimanente allora
                {
                //if (Qta.equals("6.584998"))System.out.println("porcaletta2");
                //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
                qtaRimanente=qtaRimanente.subtract(qtaEstratta);
                //if (Qta.equals("6.584998"))System.out.println("porcaletta2 "+qtaRimanente);
               // System.out.println(ultimoRecupero.Moneta + " .....qta.... "+ultimoRecupero.Qta);
     /*          //System.out.println("Moneta Riferimento: "+Moneta);
            //System.out.println("Qta Estratta: "+qtaEstratta);
           // System.out.println("Completo MON: "+Moneta+" QTA Rimanente: "+qtaRimanente);
            //System.out.println("ID: "+ultimoRecupero.IDOri);
            //System.out.println("QTA: "+ultimoRecupero.QtaOri+" MON: "+ultimoRecupero.MonOri); 
            //System.out.println("-");*/
            //    if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) SpostaQta(GruppoWallet,ultimoRecupero);
                //Aggiorno il Gruppo Wallet con quello attuale solo se è attiva l'opzione per toglierlo dallo stack
                if (toglidaStack)ultimoRecupero.GruppoWallet=GruppoWallet;
                stackRitorno.push(ultimoRecupero);

            }else{
                //if (Qta.equals("6.584998"))System.out.println("porcaletta3");
                //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
                //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
                //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
                //non ho più qta rimanente
                String qtaRimanenteStack=qtaEstratta.subtract(qtaRimanente).toPlainString();
               // System.out.println(dataOrigine+" - "+MonetaOrigine+" - "+qtaRimanenteStack);
                BigDecimal qtaRimanenteStackBD=new BigDecimal(qtaRimanenteStack);
                //adesso devo trovare anche la QtaRimanenteOrigine = QtaRimanente / QtaEstratta x QtaOrigine
                //Il cambio di scala nei calcoli dopo la divisione serve a togliere eventuali errori di approssimazione dovuti alla moltiplicazione successiva
                String qtaRimanenteOrigine=qtaRimanenteStackBD.divide(qtaEstratta,DecimaliCalcoli+10,RoundingMode.HALF_UP).multiply(QtaOrigine).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).stripTrailingZeros().abs().toPlainString();
                //System.out.println(qtaRimanenteStackBD+" / "+qtaEstratta+" x "+QtaOrigine);
                //System.out.println(dataOrigine+" - "+MonetaOrigine+" - "+qtaRimanenteOrigine);
                //Il cambio di scala nei calcoli dopo la divisione serve a togliere eventuali errori di approssimazione dovuti alla moltiplicazione successiva
                String valoreRimanenteOrigine=CostoOrigine.divide(QtaOrigine,DecimaliCalcoli+10,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteOrigine)).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).abs().toPlainString();
                // System.out.println("ValRimanenteOrigine " +valoreRimanenteOrigine);
            //    String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,30,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).abs().toPlainString();
                
                //Ora i valori avanzati li rimetto nello stack precedente
                ElementiStack el = new ElementiStack();
                    el.IDOri = IDOrigine;//ID del movimento da cui tutto ha avuto origine
                    if (IDOrigine.equalsIgnoreCase("Errore (Giacenza Negativa)"))
                    {
                        el.CostoOri="0.00";
                        el.QtaOri="";
                    }
                    else
                    { 
                        el.CostoOri = valoreRimanenteOrigine;//Costo di partenza della moneta originale
                        el.QtaOri = qtaRimanenteOrigine;//Qta di partenza della moneta originale  
                    }
                    el.MonOri = MonetaOrigine;//Moneta di partenza di tutto il giro del Lifo
                   /* if (IDOrigine.equalsIgnoreCase("Errore (Giacenza Negativa)"))
                        el.QtaOri="";
                    else 
                        el.QtaOri = qtaRimanenteOrigine;//Qta di partenza della moneta originale  */                  
                    el.DataOri = dataOrigine;//Data di partenza
                    el.GruppoWalletOri =GW;

                    el.Moneta = Moneta; //Moneta di riferimento
                    el.Qta = qtaRimanenteStack; //Qta di riferimento
                    el.ListaIDcoinvolti=listaID;
                    el.TipoMonetaOri=TipoOri;
                    el.Tipo=ultimoRecupero.Tipo;
                    el.GruppoWallet=ultimoRecupero.GruppoWallet;
                    el.Data=ultimoRecupero.Data;
                    
                    //System.out.println(el.Moneta + " qta "+el.Qta);
                stack.push(el);
                
                BigDecimal costoOrigine=CostoOrigine.subtract(new BigDecimal(valoreRimanenteOrigine));
               // BigDecimal costoTransazione=costoEstratta.subtract(new BigDecimal(valoreRimanenteSatck));
                BigDecimal qtaOrigine=QtaOrigine.subtract(new BigDecimal(qtaRimanenteOrigine));
                
                //I nuovi valori li metto nel nuovo stack che poi dovrò gestire nella funzione preposta
                el = new ElementiStack();
                    el.IDOri = IDOrigine;//ID del movimento da cui tutto ha avuto origine
                    if (IDOrigine.equalsIgnoreCase("Errore (Giacenza Negativa)")){
                        el.CostoOri="0.00";
                        el.QtaOri="";
                    }
                    else{ 
                        el.CostoOri = costoOrigine.toPlainString();//Costo di partenza della moneta originale
                        el.QtaOri = qtaOrigine.toPlainString();//Qta di partenza della moneta originale
                    }
                    el.MonOri = MonetaOrigine;//Moneta di partenza di tutto il giro del Lifo
                   /* if (IDOrigine.equalsIgnoreCase("Errore (Giacenza Negativa)"))
                        el.QtaOri="";
                    else 
                        el.QtaOri = qtaOrigine.toPlainString();//Qta di partenza della moneta originale*/
                    el.DataOri = dataOrigine;//Data di partenza
                    el.GruppoWalletOri =GW;

                    el.Moneta = Moneta; //Moneta di riferimento
                    el.Qta = qtaRimanente.toPlainString(); //Qta di riferimento
                    el.ListaIDcoinvolti=listaID;
                    el.TipoMonetaOri=TipoOri;
                    el.GruppoWallet=GruppoWallet;
                    
                   // el.Data=ultimoRecupero.Data;//messi in data 24/08/2024 per problemi di dati mancanti
                   // el.Tipo=ultimoRecupero.Tipo;//messi in data 24/08/2024 per problemi di dati mancanti
                    if (!toglidaStack)el.GruppoWallet=ultimoRecupero.GruppoWallet;
                    
                    
                    //System.out.println(el.Moneta + " qta "+el.Qta);
                   // System.out.println(el.Moneta + " .....elqta.... "+el.Qta);
         /*  // System.out.println("Parziale MON: "+Moneta+" QTA: "+el.Qta);
           // System.out.println("ID: "+el.IDOri);
            //System.out.println("QTA: "+el.QtaOri+" MON: "+el.MonOri);*/
    //     if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) SpostaQta(GruppoWallet,ultimoRecupero);
                stackRitorno.push(el);              
                qtaRimanente=new BigDecimal("0");//non ho più qta rimanente
            }
            
        }
        //pop -> toglie dello stack l'ultimo e recupera il dato
        //peek - > recupera solo il dato
        //System.out.println("RIMANENTE : "+qtaRimanente);
         if (qtaRimanente.compareTo(new BigDecimal(0))==1){
             //Se resta ancora della qta rimanente da scaricare significa che sto vendendo crypto che non posseggo, ergo mancano dei movimenti
             //in questo caso lo segnalo mettendo la data e prezzo a zero
             //Rettifico la data non la metto a zero ma al primo dell'anno
                    ElementiStack el = new ElementiStack();
                    el.IDOri = "Errore (Giacenza Negativa)";//ID del movimento da cui tutto ha avuto origine
                    el.CostoOri = "0.00";//Costo di partenza della moneta originale
                    el.MonOri = "";//Moneta di partenza di tutto il giro del Lifo
                    el.QtaOri = "";//Qta di partenza della moneta originale
                    el.DataOri = AnnoR+"-01-01 00:00";//Data di partenza
                    el.TipoMonetaOri="";
                    el.GruppoWalletOri ="";

                    el.Moneta = Moneta; //Moneta di riferimento
                    el.Qta = "-"+qtaRimanente.toPlainString(); //Qta di riferimento
                    el.GruppoWallet = GruppoWallet;
                //    el.GruppoWallet = "parsimonia";
                //  System.out.println(el.Moneta);
                    el.ListaIDcoinvolti="";
       /*           // System.out.println("Errore MON: "+Moneta+" QTA: "+qtaRimanente);
           // System.out.println("ID: "+el.IDOri);
            //System.out.println("QTA: "+el.QtaOri+" MON: "+el.MonOri);*/
            
                   // System.out.println(el.Moneta + " .....finqta.... "+el.Qta);
                
                stackRitorno.push(el); 
         }
    }
//System.out.println("-------------------");
    }
    if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoSubMovimenti").equalsIgnoreCase("NO"))
    {
        return stackRitorno;
    }
    else
    {
        //In questo caso devo invertire lo stack
        ArrayDeque<ElementiStack> stackRitornoGirato = new ArrayDeque<ElementiStack>();
        while (!stackRitorno.isEmpty()){
            stackRitornoGirato.push(stackRitorno.pop());           
        }
        return stackRitornoGirato; 
    }
}      
      
  /*  public static void DAELIMINARE_SpostaQta(String GruppoWallet,ElementiStack el,String GruppoWalletOrigine) {

        Map<String, Moneta> QtaCrypto = MappaGrWallet_QtaCrypto.get(GruppoWallet);
        if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) {
            if (!GruppoWalletOrigine.equalsIgnoreCase(GruppoWallet)) {
                //aggiungo la qta al gruppoWallet
                if (QtaCrypto.get(el.Moneta + ";" + el.TipoMonetaOri) != null) {
                    //Aggiungo la qta a quella di questo wallet
                    Moneta M1 = QtaCrypto.get(el.Moneta + ";" + el.TipoMonetaOri);
                    M1.Qta = new BigDecimal(M1.Qta)
                            .add(new BigDecimal(el.Qta)).stripTrailingZeros().toPlainString();

                } else {
                    //Movimento Nuovo da inserire
                    Moneta M1 = new Moneta();
                    M1.InserisciValori(el.Moneta, el.Qta, "", el.TipoMonetaOri);
                    //M1.Rete = Rete;
                    //da sistemare questa cosa che non riesco a recuperare rete e address
                    QtaCrypto.put(el.Moneta + ";" + el.TipoMonetaOri, M1);

                }
                //Tolgo la qta dal vecchio wallet
                Map<String, Moneta> QtaCryptoTemp = MappaGrWallet_QtaCrypto.get(GruppoWalletOrigine);
                Moneta M2 = QtaCryptoTemp.get(el.Moneta + ";" + el.TipoMonetaOri);
                //if(M2!=null)
                M2.Qta = new BigDecimal(M2.Qta)
                        .subtract(new BigDecimal(el.Qta)).stripTrailingZeros().toPlainString();
                //else QtaCryptoTemp.remove(el.MonOri + ";" + el.TipoMonetaOri);

            }
        }
        /*        if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) {
            if (!el.GruppoWalletOri.equalsIgnoreCase(GruppoWallet)) {
                //aggiungo la qta al gruppoWallet
                if (QtaCrypto.get(el.MonOri + ";" + el.TipoMonetaOri) != null) {
                    //Aggiungo la qta a quella di questo wallet
                    Moneta M1 = QtaCrypto.get(el.MonOri + ";" + el.TipoMonetaOri);
                    M1.Qta = new BigDecimal(M1.Qta)
                            .add(new BigDecimal(el.QtaOri)).stripTrailingZeros().toPlainString();

                } else {
                    //Movimento Nuovo da inserire
                    Moneta M1 = new Moneta();
                    M1.InserisciValori(el.MonOri, el.QtaOri, "", el.TipoMonetaOri);
                    //M1.Rete = Rete;
                    //da sistemare questa cosa che non riesco a recuperare rete e address
                    QtaCrypto.put(el.MonOri + ";" + el.TipoMonetaOri, M1);

                }
                //Tolgo la qta dal vecchio wallet
                Map<String, Moneta> QtaCryptoTemp = MappaGrWallet_QtaCrypto.get(el.GruppoWalletOri);
                Moneta M2 = QtaCryptoTemp.get(el.MonOri + ";" + el.TipoMonetaOri);
                //if(M2!=null)
                M2.Qta = new BigDecimal(M2.Qta)
                        .subtract(new BigDecimal(el.QtaOri)).stripTrailingZeros().toPlainString();
                //else QtaCryptoTemp.remove(el.MonOri + ";" + el.TipoMonetaOri);

            }
        }*/
    //}
      
        public static boolean StessoGruppoWalletContropate(String ID){
            boolean stessoGruppo=true;
            String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            String GruppoWalletOrigine=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
            String MovimentiCorrelati[] = Movimento[20].split(",");
            for (String IdM : MovimentiCorrelati) {
                String gruppo=DatabaseH2.Pers_GruppoWallet_Leggi(CDC_Grafica.MappaCryptoWallet.get(IdM)[3]);
                //per ogni movimento verifico se fa parte di un gruppo diverso e nel qual caso significa che la transazione riguarda wallet di gruppi diversi
                //per cui metto a false il boolean stessoGruppo
                if (!gruppo.equals(GruppoWalletOrigine))stessoGruppo=false;
            }
            return stessoGruppo;
        }
        
        public static String RitornaGruppoWalletControparte(String ID){
            String Gruppo;
            String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            String GruppoWalletOrigine=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
            Gruppo = GruppoWalletOrigine;
            String MovimentiCorrelati[] = Movimento[20].split(",");
            for (String IdM : MovimentiCorrelati) {
                String gruppo=DatabaseH2.Pers_GruppoWallet_Leggi(CDC_Grafica.MappaCryptoWallet.get(IdM)[3]);
                //per ogni movimento verifico se fa parte di un gruppo diverso e nel qual caso significa che la transazione riguarda wallet di gruppi diversi
                //per cui metto a false il boolean stessoGruppo
                if (!gruppo.equals(GruppoWalletOrigine)){
                    Gruppo=gruppo;
                }
            }
            return Gruppo;
        }
        
        public static String RitornaIDControparte(String ID){
            //Ritorna l'id della controparte se fa parte di gruppi wallet diversi
            //In futuro sarà da sistemare per farla funzionare anche se i gruppi 
            String IDC=ID;
            String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
           // String GruppoWalletOrigine=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
            String MovimentiCorrelati[] = Movimento[20].split(",");
            for (String IdM : MovimentiCorrelati) {
                String MovimentoControparte[]=CDC_Grafica.MappaCryptoWallet.get(IdM);
                //per ogni movimento verifico se fa parte di un gruppo diverso e nel qual caso significa che la transazione riguarda wallet di gruppi diversi
                //per cui metto a false il boolean stessoGruppo
                if (!Movimento[3].equals(MovimentoControparte[3])&&
                        (MovimentoControparte[22].equals("A")||MovimentoControparte[22].equals("M"))){
                    IDC=CDC_Grafica.MappaCryptoWallet.get(IdM)[0];
                }
            }
            return IDC;
        }
        
    
        
      public static void CreaPrimiMovimenti(Map<String, Map<String, Moneta>> MappaGrWallet_QtaCryptoInizio,Map<String,
              Map<String, ArrayDeque>> MappaGrWallet_CryptoStack, long inizio, String DataInizioAnno) {

        for (String key : MappaGrWallet_QtaCryptoInizio.keySet()) {
            Map<String, Moneta> a = MappaGrWallet_QtaCryptoInizio.get(key);
            for (Moneta m : a.values()) {
                if (!m.Tipo.equalsIgnoreCase("FIAT") && new BigDecimal(m.Qta).compareTo(new BigDecimal(0)) != 0) {
                    //long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);
                    m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, inizio, null, true, 15, m.Rete);
                    //System.out.println(m.Moneta+" - "+m.Qta+" - "+inizio+" - "+m.Prezzo);
                    //System.out.println(m.Prezzo);
                    //System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo);
                    Map<String, ArrayDeque> CryptoStackTemp;
                    String WR = key;
                    if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) {
                        WR = "Unico 01";
                    }
                    CryptoStackTemp = MappaGrWallet_CryptoStack.get(WR);

                    ElementiStack el = new ElementiStack();

                    el.IDOri = "Giacenza Inizio Anno";//ID del movimento da cui tutto ha avuto origine
                    el.CostoOri = m.Prezzo;//Costo di partenza della moneta originale
                    el.MonOri = m.Moneta;//Moneta di partenza di tutto il giro del Lifo
                    el.QtaOri = m.Qta;//Qta di partenza della moneta originale
                    el.DataOri = DataInizioAnno;//Data di partenza
                    el.GruppoWalletOri = key;//Gruppo Wallet di partenza

                    el.Moneta = m.Moneta; //Moneta di riferimento
                    el.Qta = m.Qta; //Qta di riferimento    
                    el.TipoMonetaOri = m.Tipo;
                   // el.Tipo = Monete[1].Tipo;
                    el.Tipo = m.Tipo;
                    el.GruppoWallet = key;
                    //el.Data = Data;
                    el.Data =DataInizioAnno;
                    //System.out.println(DataInizioAnno);
                    //if (el.QtaOri.isBlank())System.out.println("Errore in Crea Primi movimenti");
                    StackLIFO_InserisciValoreFR(CryptoStackTemp, key, el);
                    if (CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key) == null) {
                        List<Moneta> li = new ArrayList<>();
                        Moneta mo = m.ClonaMoneta();
                        li.add(mo);
                        CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.put(key, li);
                    } else {
                        List<Moneta> li = CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key);
                        Moneta mo = m.ClonaMoneta();
                        li.add(mo);
                    }
                }
            }
        }
    }
      
      
        public static void ChiudiRWFR (Moneta Monete,Map<String, ArrayDeque> CryptoStack,String GruppoWallet,String Data,String Valore,String Causale,String IDt) {
        //System.out.println(Data+ " - "+Monete.Moneta+" - "+Monete.Qta+" - "+Monete.Prezzo+" - "+Valore+" - "+Monete.Rete+" - "+Monete.MonetaAddress);
        List<String[]> ListaRW;
        String DataDaScrivere=Data;
                        
        //System.out.println(Monete.Qta);
        //tolgo dal lifo solo se non è fiat, sulle fiat non mi interessa fare nulla attualmente
        if (!Monete.Moneta.isBlank() && !Monete.Tipo.equalsIgnoreCase("FIAT")) {//tolgo dal lifo solo se non è fiat, sulle fiat non mi interessa fare nulla attualmente
            ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStack, Monete.Moneta, Monete.Qta,GruppoWallet, true);
           // System.out.println(Monete.Moneta+" p "+Monete.Prezzo+" q "+Monete.Qta+" v "+Valore);
            //creo la riga per i quadri RW
            
            //Se lo stack è vuoto vuol dire che non ho movimenti tracciati della moneta in questione ma ho una giacenza finale
            //Questo caso si verifica se ho delle giacenze negative a fine anno oppure non ho tracciato correttamente i movimenti
            //nel qual caso mi compare l'errore "Attenzione movimento XY non considerato in Calcoli RW
         //   System.out.println(Monete.Moneta+"-"+Valore);
            if (StackRitorno.isEmpty()){
               // System.out.println("Sono io "+Monete.Moneta);
                String xlista[]=new String[17];
                xlista[0]=AnnoR;                                                //Anno RW
                xlista[1]="";                                                   //Gruppo Wallet Inizio
                xlista[2]="";                                                   //Moneta Inizio
                xlista[3]="";                                                   //Qta Inizio
                xlista[4]="";                                                   //Data Inizio
                xlista[5]="0.00";                                               //Prezzo Inizio
                xlista[6]=GruppoWallet;                                         //GruppoWallet Fine
                xlista[7]=Monete.Moneta;                                        //Moneta Fine
                xlista[8]=Monete.Qta;                                           //Qta Fine
                xlista[9]=DataDaScrivere;                                       //Data Fine
                xlista[10]=Valore;                                              //Prezzo Fine
                xlista[11]="365";                                            //Giorni di Detenzione
                xlista[12]=Causale;                                             //Causale
                xlista[13]="";                                                  //ID Movimento Apertura (o segnalazione inizio anno)
                xlista[14]=IDt;                                                 //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                xlista[15]="Errore (Giacenza Negativa)";                        //Tipo Errore
                xlista[16]="";                                                  //Lista ID coinvolti separati da virgola
                ListaRW=CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                ListaRW.add(xlista);
            }
            while (!StackRitorno.isEmpty()) {

                ElementiStack el = StackRitorno.pop();
            //    if (Causale.contains("Fine Anno")) GruppoWallet=el.GruppoWalletOri; 
                //Elementi è così composta Qta;Prezzo;Data  
                //Se la data del movimento è uguale a quella di creazione (al minuto) metto GG di detenzione zero altrimenti anche se posseggo la moneta per un solo minuto metto 1
                long DiffData = OperazioniSuDate.ConvertiDatainLongMinuto(Data)-OperazioniSuDate.ConvertiDatainLongMinuto(el.DataOri);
                if (DiffData!=0){
                   // DiffData = (OperazioniSuDate.ConvertiDatainLong(Data.split(" ")[0]) - OperazioniSuDate.ConvertiDatainLong(Elementi[2].split(" ")[0]) + 86400000) / 86400000;
                //    String RWgiorno1 = DatabaseH2.Pers_Opzioni_Leggi("RW_DiffDateMatematica");
                  //  if (RWgiorno1 != null && RWgiorno1.equalsIgnoreCase("SI")) {
                  //      DiffData = OperazioniSuDate.DifferenzaDate(el.DataOri, Data);
                  //  } else {
                        DiffData = OperazioniSuDate.DifferenzaDate(el.DataOri, Data) + 1;
                  //  }
                }
                
                //adesso controllo se la data fine corrisponde con la data di inizio anno a mezzanotte, se così tolgo 1 minuto per scrivere il valore del wallet a fine anno
                if (Data.equals(String.valueOf(Integer.parseInt(AnnoR)+1)+"-01-01 00:00")){
                    DataDaScrivere=AnnoR+"-12-31 23:59";
                }

                String Prz;
                if (new BigDecimal(Valore).compareTo(new BigDecimal("0"))==0) Prz=Valore;
                else Prz = new BigDecimal(Valore).divide(new BigDecimal(Monete.Qta), DecimaliCalcoli+10, RoundingMode.HALF_UP).multiply(new BigDecimal(el.Qta)).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).abs().toPlainString();
                //System.out.println(Monete.Moneta +" + "+Monete.Qta);
             /*   String GruppoInizio="";
                if (CDC_Grafica.MappaCryptoWallet.get(el.IDOri) != null)
                {    
                    GruppoInizio = DatabaseH2.Pers_GruppoWallet_Leggi(CDC_Grafica.MappaCryptoWallet.get(el.IDOri)[3]);
                }*/
                String xlista[]=new String[17];
                xlista[0]=AnnoR;                    //Anno RW
                xlista[1]=el.GruppoWalletOri;             //Gruppo Wallet Inizio
                xlista[2]=el.MonOri;                //Moneta Inizio
                xlista[3]=el.QtaOri;                //Qta Inizio
                xlista[4]=el.DataOri;               //Data Inizio
                xlista[5]=el.CostoOri;              //Prezzo Inizio
                
                xlista[6]=GruppoWallet;         //GruppoWallet Fine
                xlista[7]=Monete.Moneta;            //Moneta Fine
                xlista[8]=new BigDecimal(el.Qta).stripTrailingZeros().toPlainString(); //Qta Fine
                xlista[9]=DataDaScrivere;           //Data Fine
                xlista[10]=Prz;                     //Prezzo Fine
                xlista[11]=String.valueOf(DiffData);//Giorni di Detenzione
                xlista[12]=Causale;                 //Causale
                xlista[13]=el.IDOri;                //ID Movimento Apertura (o segnalazione inizio anno)
                xlista[14]=IDt;                     //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                xlista[15]="";                      //Tipo Errore
                xlista[16]=el.ListaIDcoinvolti;     //Lista ID coinvolti separati da virgola
                
                //Verifico Ora se esistono i prezzi unitari del token con data iniziale e del token con data finale.
                //Se esistono li scrivo nella lista come campo 13 e 14
                
                //Qui gestisco gli errori
                //Gli errori non li segnalo se il token in questione è stato identificato come Scam
                if (el.IDOri.contains("Errore")) {
                    xlista[15] = el.IDOri;
                }
                if (!xlista[11].equals("0")){//Solo se i giorni di detenzione sono diversi da zero compilo la lista altrimenti resta tutto così com'è.
                    if(CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet)==null)
                        {
                            ListaRW=new ArrayList<>();
                            CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(GruppoWallet, ListaRW);
                        } 
                    ListaRW=CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                    ListaRW.add(xlista);
                }
                }
        }
    }
        
        
    public static void ChiudiRWGiacenzeFinali(String GruppoWallet) {
        //System.out.println(Data+ " - "+Monete.Moneta+" - "+Monete.Qta+" - "+Monete.Prezzo+" - "+Valore+" - "+Monete.Rete+" - "+Monete.MonetaAddress);
        //pulizia vecchia lista, tanto devo ricrearla da capo in questo caso perchè devo prendere solo i valori iniziali e finali
        List<String[]> ListaRW=new ArrayList<>();
        CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(GruppoWallet, ListaRW);
        
        String GGDetenzione="365";
        String MotivoInizio="Giacenza Inizio Anno";
        String DataFineAnno = AnnoR + "-12-31 23:59";
        String DataInizioAnno = AnnoR + "-01-01 00:00";
        
        //Verifico se il primo movimento assoluto per il gruppo wallet + avvenuto nel periodo di competenza
        //Se così fisso quella data come data di inizio detenzione per tutte le cripto di quel gruppo wallet
        String IDPrimoMovimento = MappaGruppo_IDPrimoMovimento.get(GruppoWallet);
        String PrimoMovimento[] = MappaCryptoWallet.get(IDPrimoMovimento);
        String DataPrimoMovimento = PrimoMovimento[1];
        String AnnoPrimoMovimento = DataPrimoMovimento.split("-")[0];
        if (AnnoPrimoMovimento.equals(AnnoR))
        {
            DataInizioAnno=DataPrimoMovimento;
            MotivoInizio="Primo Movimento del Wallet";
            long DiffData = OperazioniSuDate.DifferenzaDate(DataInizioAnno.split(" ")[0], DataFineAnno.split(" ")[0]) + 1;
            GGDetenzione=String.valueOf(DiffData);
            Moneta PrimaMoneta[] = Moneta.RitornaMoneteDaMov(PrimoMovimento);
            PrimaMoneta[1].Prezzo = PrimoMovimento[15];
            if (!PrimaMoneta[1].Moneta.isBlank()) {//la moneta che sta entrando deve contenere qualcosa altrimenti non può essere il primo movimento e c'è un errore
                if (CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(GruppoWallet) != null) {
                    CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(GruppoWallet).add(PrimaMoneta[1]);
                }else{
                    List<Moneta> lf=new ArrayList<Moneta>();
                    lf.add(PrimaMoneta[1]);
                    CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.put(GruppoWallet, lf);
                }
            }
            //Mappa_RW_GiacenzeInizioPeriodo
                
        }
        
        
        //System.out.println(MappaGruppo_IDPrimoMovimento.get(GruppoWallet));
        //Moneta Miniziale = null;
        Map<String, Moneta[]> MappaDoppia = new TreeMap<>();//Moneta[0]=monetaIniziale, Moneta[1]=monetaFinale
        if (CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(GruppoWallet) != null) {
            List<Moneta> lf = CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(GruppoWallet);
            Iterator<Moneta> it = lf.iterator();
            while (it.hasNext()) {
                Moneta Mfinale = it.next();
                Moneta Miniziale=new Moneta();
                Miniziale.Moneta=Mfinale.Moneta;
                Miniziale.GruppoRW=Mfinale.GruppoRW;
                Miniziale.Prezzo="0.000";
                Miniziale.Qta="0";
                Miniziale.Tipo=Mfinale.Tipo;
                Moneta Mdoppia[] =new Moneta[2];
                Mdoppia[0]=Miniziale;
                Mdoppia[1]=Mfinale;
                //Azzero il prezzo se la giacenza è negativa perchè appunto significa che non ce l'ho
                //la voglio far vedere ma non deve incidere sul''rw
                if (Mfinale.Qta.contains("-"))Mfinale.Prezzo="0.000";
                MappaDoppia.put(Mfinale.Moneta, Mdoppia);
            }
        }
        if (CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(GruppoWallet) != null) {
            List<Moneta> li = CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(GruppoWallet);
            Iterator<Moneta> iti = li.iterator();
            while (iti.hasNext()) {
                Moneta Miniziale = iti.next();
                if (MappaDoppia.get(Miniziale.Moneta) == null) {
                    Moneta Mfinale = new Moneta();
                    Mfinale.Moneta = Miniziale.Moneta;
                    Mfinale.GruppoRW = Miniziale.GruppoRW;
                    Mfinale.Prezzo = "0.000";
                    Mfinale.Qta = "0";
                    Mfinale.Tipo = Miniziale.Tipo;
                    Moneta Mdoppia[] = new Moneta[2];
                    Mdoppia[0] = Miniziale;
                    Mdoppia[1] = Mfinale; 
                    MappaDoppia.put(Miniziale.Moneta, Mdoppia);
                }else{
                    //Azzero il prezzo se la giacenza è negativa perchè appunto significa che non ce l'ho
                    //la voglio far vedere ma non deve incidere sul''rw
                    if (Miniziale.Qta.contains("-"))Miniziale.Prezzo="0.000";
                    Moneta Mdoppia[] = MappaDoppia.get(Miniziale.Moneta);
                    Mdoppia[0] = Miniziale;
                }
            }
        }   
        for(Moneta m[]:MappaDoppia.values()){

                               //Trovato moneta con giacenza iniziale e finale
                                //Compilo la lista
                                String xlista[] = new String[17];
                                xlista[0] = AnnoR;                                                //Anno RW
                                xlista[1] = GruppoWallet;                                         //Gruppo Wallet Inizio
                                xlista[2] = m[0].Moneta;                                     //Moneta Inizio
                                xlista[3] = m[0].Qta;                                        //Qta Inizio
                                xlista[4] = DataInizioAnno;                                       //Data Inizio
                                xlista[5] = m[0].Prezzo;                                     //Prezzo Inizio
                                xlista[6] = GruppoWallet;                                         //GruppoWallet Fine
                                xlista[7] = m[1].Moneta;                                        //Moneta Fine
                                xlista[8] = m[1].Qta;                                           //Qta Fine
                                xlista[9] = DataFineAnno;                                         //Data Fine
                                xlista[10] = m[1].Prezzo;                                              //Prezzo Fine
                                xlista[11] = GGDetenzione;                                               //Giorni di Detenzione
                                xlista[12] = "Fine Anno";                                             //Causale
                                xlista[13] = MotivoInizio;                                    //ID Movimento Apertura (o segnalazione inizio anno)
                                xlista[14] = "Giacenza Fine Anno";                                                 //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                                xlista[15] = "";                                                  //Tipo Errore
                                xlista[16] = "";                                                  //Lista ID coinvolti separati da virgola

                                
                              /*  if (CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet) == null) {
                                    ListaRW = new ArrayList<>();
                                    CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(GruppoWallet, ListaRW);
                                }*/
                                ListaRW = CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                                ListaRW.add(xlista);
                            }
                  

    }
    
    
    public static void SistemaErroriInListe(){
        
        //Se prezzo = 0.00 significa che non esiste il prezzo della moneta
        //Lo segnalo nelgi errori
        //Poi scalo tutti i prezzi ai 2 centesimi
        for (String key : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.keySet()) {
            //if (CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(key)!=null)
            for (String[] lista : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(key)) {
                Moneta mi[]=new Moneta[2];
                Moneta mf[]=new Moneta[2];
                if (MappaCryptoWallet.get(lista[13])!=null) mi=Funzioni.RitornaMoneteDaID(lista[13]);
                if (MappaCryptoWallet.get(lista[14])!=null) mf=Funzioni.RitornaMoneteDaID(lista[14]);
                //Prima di aggiungere alla tabella la riga relativa al movimento controllo se il valore è a zero
                //se il valore è zero e non esiste un prezzo per quel token a quella data allora metto errore
                String PrezzoInizio = lista[5];
                String PrezzoFine = lista[10];
                if (lista[15].isBlank()) lista[15]="<html>";
                else lista[15]="<html>"+lista[15].trim()+"<br>";
                //Se
                //A - id non reale
                //B - PrezzoInizio=0.00
                //C - Token non scam
                //Oppure
                //Se 
                //1 - ho id iniziale reale (non fine o inizio anno)
                //2 - il prezzo è zero (in qualsiasi forma)
                //3 - il token non è scam e il token non è vuoto (è vuoto in caso di giacenze negative)
                //4 - il prezzo cercato nel database è zero
                //5 - non esiste un prezzo sul csv
                //Allora Faccio uscire l'errore Valore Iniziale non valorizzato
               // System.out.println("ID Iniziale :"+MappaCryptoWallet.get(lista[13]));
             //   System.out.println("ID Finale :"+MappaCryptoWallet.get(lista[14]));
                if( (MappaCryptoWallet.get(lista[13])==null                     //A
                        &&PrezzoInizio.equals("0.00")                           //B
                        &&!lista[2].contains(" **")                             //C
                        &&!lista[2].isBlank()
                     )                            
                        ||
                    (MappaCryptoWallet.get(lista[13])!=null                     //1
                        &&new BigDecimal(PrezzoInizio).compareTo(new BigDecimal(0))==0 //2
                        &&!lista[2].contains(" **")                             //3
                        &&!MappaCryptoWallet.get(lista[13])[32].equals("SI")                 //4
                      //  &&!TokenConPrezzo(mi[1],"0.00",lista[4])                 //4                     
                        &&MappaCryptoWallet.get(lista[13])[14].isBlank()        //5
                        &&!lista[2].isBlank()
                     )
                    )
                {
                    lista[5] = "0";
                    lista[15] = lista[15]+"Errore (Valore Iniziale non Valorizzato) <br>";
                    lista[15] = lista[15]+"Bottone '<b>Modifica Valore Iniziale</b>' per correggere<br><br>";
                }
                
                //System.out.println(mf[0]);
                //Stessa cosa però con il valore finale
                if( (MappaCryptoWallet.get(lista[14])==null                     //A
                        &&PrezzoFine.equals("0.00")                           //B
                        &&!lista[7].contains(" **")                             //C
                     )                            
                        ||
                    (MappaCryptoWallet.get(lista[14])!=null                     //1
                        &&new BigDecimal(PrezzoFine).compareTo(new BigDecimal(0))==0 //2
                        &&!lista[7].contains(" **")                             //3
                        &&!MappaCryptoWallet.get(lista[14])[32].equals("SI")                 //4
                       // &&!TokenConPrezzo(mf[0],"0.00",lista[9])                //4
                        &&MappaCryptoWallet.get(lista[14])[14].isBlank()        //5
                     )
                    )
                {
                   // System.out.println("Valore Finale non valorizzato");
                    lista[10]="0";
                    lista[15] = lista[15]+"Errore (Valore Finale non Valorizzato) <br>";
                    lista[15] = lista[15]+"Bottone '<b>Modifica Valore Finale</b>' per correggere<br><br>";
                }
                                
                                
             /*   if (PrezzoInizio.equals("0.00")&&!lista[2].contains(" **")) {//token non valorizzato e non scam
                    lista[5] = "0";
                    lista[12] = lista[12]+"Errore (Valore Iniziale non Valorizzato) <br>";
                }*/
                /*if (PrezzoFine.equals("0.00")&&!lista[2].contains(" **")) {//token non valorizzato e non scam
                    lista[7]="0";
                    lista[12] = lista[12]+"Errore (Valore Finale non Valorizzato) <br>";
                }*/
                //lista[3]=new BigDecimal(lista[3]).setScale(10, RoundingMode.HALF_UP).toPlainString();
                if (!lista[5].equals("0"))lista[5]=new BigDecimal(lista[5]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (!lista[10].equals("0"))lista[10]=new BigDecimal(lista[10]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                
                //Manca da sistemare la parte relativa al fatto di non segnalare i token scam
                //Poi sarebbe anche da verificare le giacenze negative
                //e fare la modifica dei prezzi sui token con id

                if (Funzioni.isSCAM(lista[2])){
                    lista[15] = lista[15]+"Avviso (Token iniziale SCAM)<br>";//Se Token SCAM non verrà considerato in nessun calcolo dell'RW, verrà solo mostrato
                }
                if (Funzioni.isSCAM(lista[7])){
                    lista[15] = lista[15]+"Avviso (Token finale SCAM)<br>";//Se Token SCAM non verrà considerato in nessun calcolo dell'RW, verrà solo mostrato
                }
                
                //Se ID di apertura corrisponde a movimento non classificato aggiungo anche quell'errore
                if(MappaCryptoWallet.get(lista[13])!=null
                        &&MappaCryptoWallet.get(lista[13])[18].isBlank()
                        &&lista[13].split("_")[4].equals("DC")&&!Funzioni.isSCAM(lista[2])) {
                    lista[15] = lista[15]+"Errore (Movimento di apertura non Classificato) <br>";
                }
                if(MappaCryptoWallet.get(lista[14])!=null
                        &&MappaCryptoWallet.get(lista[14])[18].isBlank()
                        &&lista[14].split("_")[4].equals("PC")) {
                    lista[15] = lista[15]+"Errore (Movimento di chiusura non Classificato) <br>";
                }
                //Se il valore iniziale o finale è negativo e non ho ancora segnalato il fatto che la giacenza è negativa allora aggiungo la segnalazione
                if ((lista[8].contains("-")|lista[3].contains("-"))&&!lista[15].contains("Giacenza Negativa")) 
                {
                    lista[15]=lista[15]+"Errore (Giacenza Negativa) <br>";
                }
                lista[15]=lista[15]+"</html>";
               /* else if (Elementi[3].contains("Errore") && Valore.equals("0.00")) {
                    xlista[12] = "Errore ("+Elementi[3].split("\\(")[1].replace(")", "")+" e Token non Valorizzato)";
                } else if (Valore.equals("0.00")) {
                    xlista[12] = "Errore (Token non Valorizzato)";
                } else */
            }
        }
    }

    public static boolean TokenConPrezzo(Moneta m,String Valore,String Data){
        boolean TokenConPrezzo=true;
                        //Prima di inserire il token nello stack verifico se è valorizzato a 0.00
                        //e se il token è effettivamente senza prezzo
                        //se così è allora lascio Valore a 0.00 (Significa che è un tokenm senza prezzo)
                        //Altrimenti lo valorizzo a 0.000 che significa che il suo valore è zero ma il token ha un valore intrinseco
                        //Dovrò anche verificare se per caso il token ha già prezzi che arrivano dal csv
                        if (Valore.equals("0.00")){
                            long DataRif=OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                            String Prezzo = Prezzi.DammiPrezzoTransazione(m, null, DataRif, null, true, 15, m.Rete);
                           //System.out.println("Funzione CalcoliRW TokenConPrezzo "+m.Moneta+" - "+Prezzo);
                            if (Prezzo.equals("0.00"))TokenConPrezzo=false;
                        }
        return TokenConPrezzo;
    }
    
   
    
   
        public static void AggiornaRWFR(String AnnoRif) {
        
        MappaGruppo_IDPrimoMovimento=Funzioni.MappaPrimoMovimentoXGruppoWallet();
        String Rilevanza=DatabaseH2.Pers_Opzioni_Leggi("RW_Rilevanza");
        //Chiusura conseguente apertura di un nuovo rigo segue queste roegole
        //Rilevanza A = Solo Valori iniziali e finali (ancora da implementare)!!!!!
        //Rilevanza B = Solo scambi con FIAT
        //Rilevanza C = Solo scambi rilevanti fiscalmente
        //Rilevanza D = Tutti gli scambi 
        //Assegno ad un booleano le opzioni di calcolo per l'RW
        boolean ChiudiRWsuTrasferimento=false;
        if (DatabaseH2.Pers_Opzioni_Leggi("RW_ChiudiRWsuTrasferimento").equals("SI")) ChiudiRWsuTrasferimento=true;
        boolean StakingZero = false;
        if (DatabaseH2.Pers_Opzioni_Leggi("RW_StakingZero").equals("SI")) StakingZero=true;
        boolean LiFoComplessivo = false;
        if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) LiFoComplessivo=true;
        
        CDC_Grafica.Mappa_RW_ListeXGruppoWallet.clear();
        CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.clear();
        CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.clear();
        AnnoR=AnnoRif;
        String AnnoSuccessivo=String.valueOf(Integer.parseInt(AnnoRif)+1);

        //PARTE 1 : Calcolo delle Giacenze iniziali e inserimento nello stack
        int AnnoRiferimento = Integer.parseInt(AnnoRif);
        String DataInizioAnno = AnnoRif+"-01-01 00:00";
        String DataFineAnno = AnnoRif+"-12-31 23:59";
      //  String RWgiorno1 = DatabaseH2.Pers_Opzioni_Leggi("RW_DiffDateMatematica");
      //  if (RWgiorno1 != null && RWgiorno1.equalsIgnoreCase("SI")) {
      //      DataFineAnno=AnnoSuccessivo+"-01-01 00:00";      
      //  }
        long fine = OperazioniSuDate.ConvertiDatainLongMinuto(AnnoSuccessivo+"-01-01 00:00");//Data Fine anno in long per calcolo prezzi
        
       // long inizio = OperazioniSuDate.ConvertiDatainLongSecondo(AnnoPrecendente+"-12-31 23:59:59");//Data Fine anno in long per calcolo prezzi
        long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);//Data inizio anno in long per calcolo prezzi
        boolean PrimoMovimentoAnno = true;

////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere
        // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();
        Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto = new TreeMap<>();
        Map<String, Map<String, Moneta>> MappaGrWallet_QtaCryptoInizio = new TreeMap<>();
        //Map<String, Map<String, Moneta>> MappaGrWallet_QtaCryptoTOT = new TreeMap<>();
        Map<String, ArrayDeque> CryptoStack;// = new TreeMap<>();
        Map<String, Moneta> QtaCrypto;
        Map<String, Moneta> QtaCryptoInizio;
       // Map<String, Moneta> QtaCryptoTOT;
        List<String[]> ListaRW;

        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
            String GW = GruppoWallet;
            if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) GW = "Unico 01";
            if (CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet) == null) {
                //se non esiste ancora lo stack lo creo e lo associo alla mappa
                //stessa cosa faccio per la lista per l'rw
                //stessa cosa faccio per il gruppo delle qta
                QtaCryptoInizio = new TreeMap<>();
                MappaGrWallet_QtaCryptoInizio.put(GruppoWallet, QtaCryptoInizio);
                ListaRW=new ArrayList<>();
                CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(GruppoWallet, ListaRW);
            }
             else {
                //altrimenti lo recupero per i calcoli
                QtaCryptoInizio = MappaGrWallet_QtaCryptoInizio.get(GruppoWallet);
                //Le giacenze di inizio anno vengono sempre separate da quell globali perchè mi serve sapere di che wallet fanno parte
                //le giacenze conteggiate fino alla fine dell'anno
               
            }
           
            if (MappaGrWallet_CryptoStack.get(GW) == null) {
                //se non esiste ancora lo stack lo creo e lo associo alla mappa
                //stessa cosa faccio per la lista per l'rw
                //stessa cosa faccio per il gruppo delle qta
                CryptoStack = new TreeMap<>();
                MappaGrWallet_CryptoStack.put(GW, CryptoStack);
                QtaCrypto = new TreeMap<>();
                MappaGrWallet_QtaCrypto.put(GW, QtaCrypto);
            } else {
                //altrimenti lo recupero per i calcoli
                //QtaCrypto serve per trovare le rimanenze di ogni crypto e compilare la giacenza di fine o inizio anno.
                CryptoStack = MappaGrWallet_CryptoStack.get(GW);
                QtaCrypto = MappaGrWallet_QtaCrypto.get(GW);
               
            }

            String IDTransazione = v[0];
            String Data = v[1];
            String IDTS[] = IDTransazione.split("_");
            String Valore = v[15];
            String Rete = Funzioni.TrovaReteDaID(v[0]);
            Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
            //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
            Monete[0] = new Moneta();
            Monete[1] = new Moneta();
            Monete[0].MonetaAddress = v[26];
            Monete[1].MonetaAddress = v[28];
            //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
            Monete[0].Moneta = v[8];
            Monete[0].Tipo = v[9];
            Monete[0].Qta = v[10];
            Monete[0].Rete = Rete;
            Monete[1].Moneta = v[11];
            Monete[1].Tipo = v[12];
            Monete[1].Qta = v[13];
            Monete[1].Rete = Rete;

            int Anno = Integer.parseInt(Data.split("-")[0]);

            //PARTE 1
            if (Anno < AnnoRiferimento) {
                //Faccio i conti per i valori iniziali

                //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);
                    }
                    if (!Monete[a].Moneta.isBlank() && QtaCryptoInizio.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCryptoInizio.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCryptoInizio.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);
                    }
                }

                //PARTE 2    
            } else if (Anno == AnnoRiferimento) {
                //al primo movimento dell'anno successivo faccio questo:
                //1 - Inseirsco nello stack tutti i valori iniziali precedentemente trovati
                //2 - Uso il lifo per il calcolo dei valori RW
                if (PrimoMovimentoAnno) {
                    
                    CreaPrimiMovimenti(MappaGrWallet_QtaCryptoInizio,MappaGrWallet_CryptoStack,inizio,DataInizioAnno);
                    PrimoMovimentoAnno = false;
                   /* for (String key : MappaGrWallet_QtaCryptoInizio.keySet()) {
                        Map<String, Moneta> a = MappaGrWallet_QtaCryptoInizio.get(key);
                        for (Moneta m : a.values()) {
                            if (!m.Tipo.equalsIgnoreCase("FIAT")&&new BigDecimal(m.Qta).compareTo(new BigDecimal(0))!=0) {
                                //long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);
                                m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, inizio, null, true, 15, m.Rete);
                                //System.out.println(m.Prezzo);
                                //System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo);
                                Map<String, ArrayDeque> CryptoStackTemp;
                                String WR=key;
                                if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) WR = "Unico 01";
                                CryptoStackTemp = MappaGrWallet_CryptoStack.get(WR);

                                ElementiStack el = new ElementiStack();

                                el.IDOri = "Giacenza Inizio Anno";//ID del movimento da cui tutto ha avuto origine
                                el.CostoOri = m.Prezzo;//Costo di partenza della moneta originale
                                el.MonOri = m.Moneta;//Moneta di partenza di tutto il giro del Lifo
                                el.QtaOri = m.Qta;//Qta di partenza della moneta originale
                                el.DataOri = DataInizioAnno;//Data di partenza
                                el.GruppoWalletOri = key;//Gruppo Wallet di partenza

                                el.Moneta = m.Moneta; //Moneta di riferimento
                                el.Qta = m.Qta; //Qta di riferimento    
                                el.TipoMonetaOri=m.Tipo;
                                el.Tipo=Monete[1].Tipo;
                                el.GruppoWallet=key;
                                el.Data =Data;

                                StackLIFO_InserisciValoreFR(CryptoStackTemp,key,el);
                                if (CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key)==null){
                                    List<Moneta> li=new ArrayList<>();
                                    Moneta mo=m.ClonaMoneta();
                                    li.add(mo);
                                    CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.put(key, li);
                                }else{
                                    List<Moneta> li=CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key);
                                    Moneta mo=m.ClonaMoneta();
                                    li.add(mo);
                                }
                            }
                        }
                    }*/
                    
                }

                //Continuo comunque a fare la somma della qta delle crypto che servirà dopo per chiudere gli RW di fine anno
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                    }
                    if (!Monete[a].Moneta.isBlank() && QtaCryptoInizio.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCryptoInizio.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCryptoInizio.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);
                    }
                                      
                }

                //Solo se rilvenza è di diverso da A proseguo con i calcoli
                //Infatti se è uguale ad A devo solo leggere le giacenze iniziali e finali
                if (!Rilevanza.equalsIgnoreCase("A")){                 
                String Causale = "Vendita";
                    switch (IDTS[4]) {
                        case "VC" ->
                            Causale = "Vendita";//Fiscalmente Rilevante
                        case "SC" ->
                            Causale = "Scambio";
                        case "CM" ->
                            Causale = "Commissione";//Fiscalmente Rilevante
                        case "RW" ->
                            Causale = "Rimborso Ricompensa";//Fiscalmente Rilevante
                        default -> {
                        }
                    }

                //Adesso a seconda del tipo movimento devo comportarmi in maniera diversa
                //TIPOLOGIA = 0 (Vendita Crypto)
                if (IDTS[4].equals("VC") //Vendita Cripto - MU=crypto ME=FIAT
                        || IDTS[4].equals("CM")//Commissione - MU=crypto ME=null
                        || IDTS[4].equals("RW")//Reward - MU=null ME=crypto
                        || IDTS[4].equals("AC")) //Acquisto Crypto - MU=FIAT ME=Crypto
                {
                    //tolgo dal Lifo della moneta venduta e prendo la lista delle varie movimentazione
                    if (!Monete[0].Moneta.isBlank() && !Monete[0].Tipo.equalsIgnoreCase("FIAT")) {
                        //Chiudo RW se ho una moneta in uscita che è diversa da una FIAT (le FIAT non le considero per l'RW)
                        //Se è un movimento RW() quindi un rimborso di un RW se è qua e il prezzo è zero lo ricalcolo perchè viene messo a zero dal sistema per il calcolo delle plusvalenze
                        //ma non è corretto per il calcolo del quadro RW
                       // System.out.println("Monete coinvolte :"+Monete[0].Moneta+"/"+Monete[0].Tipo+" - "+Monete[1].Moneta+"/"+Monete[1].Tipo+"\n");
                        long d = OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                        if (IDTS[4].equals("RW") && Valore.equals("0.00")) {
                            Valore = Prezzi.DammiPrezzoTransazione(Monete[0], null, d, null, true, 15, Monete[0].Rete);                            
                        }
                      //  System.out.println(Monete[0].Prezzo+" m "+Monete[0].Moneta + " v "+Valore);
                        ChiudiRWFR(Monete[0], CryptoStack, GruppoWallet, Data, Valore, Causale, IDTransazione);
                    }
                    if (!Monete[1].Moneta.isBlank() && !Monete[1].Tipo.equalsIgnoreCase("FIAT")) {
                        //Apro RW se ho una moneta in ingresso diversa da una FIAT (le FIAT non le considero per l'RW) 
                        ElementiStack el = new ElementiStack();

                        el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                        if(IDTS[4].equals("RW")&&
                                (StakingZero||!Funzioni.RewardRilevante(IDTransazione))) 
                            {
                                el.CostoOri="0.00";
                            }
                        else  el.CostoOri = Valore;//Costo di partenza della moneta originale
                        el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                        el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                        el.DataOri = Data;//Data di partenza
                        el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza

                        el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                        el.Qta = Monete[1].Qta; //Qta di riferimento
                        el.TipoMonetaOri=Monete[1].Tipo;  
                        el.Tipo=Monete[1].Tipo;
                        el.GruppoWallet=GruppoWallet;
                        el.Data =Data;
                        //if (el.QtaOri.isBlank())System.out.println("VC-CM-RW-AC : "+el.Data);
                        StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                    }

                } else if (IDTS[4].equals("DF")//deposito Fiat
                        || IDTS[4].equals("PF")//Prelievo Fiat
                        || IDTS[4].equals("SF")//Scambio Fiat
                        || IDTS[4].equals("TI"))//Trasferimento Interno all'interno dello stesso Wallet
                {
                    //Queste sono categorie per cui non va fatto rw quindi le escludo

                }else if (IDTS[4].equals("SC"))//deposito Fiat

                {
                    /*    if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) {
                        ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStack, Monete[0].Moneta, Monete[0].Qta, false);
                        while (!StackRitorno.isEmpty()) {
                            ElementiStack ELem = StackRitorno.pop();
                            SpostaQta(GruppoWallet,ELem,ELem.GruppoWalletOri);
                        }}*/
                    //Adesso distinguo 2 casi, se scambio fiscalmente rilevante oppure no
                    if (ScambioRilevante(Monete,Data,Rilevanza)){
                        //Chiudo RW moneta uscente
                        //prima di chiudere RW recupero i movimenti e li sposto sul wallet di destinazione

                        
                        ChiudiRWFR(Monete[0], CryptoStack, GruppoWallet, Data, Valore, Causale, IDTransazione);
                        
                        //Apro RW Moneta entrante
                        ElementiStack el = new ElementiStack();
                        el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                        el.CostoOri = Valore;//Costo di partenza della moneta originale
                        el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                        el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                        el.DataOri = Data;//Data di partenza
                        el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza
                        el.Moneta = Monete[1].Moneta; //Moneta di riferimento per lo stack
                        el.Qta = Monete[1].Qta; //Qta di riferimento
                        el.TipoMonetaOri=Monete[1].Tipo;
                        el.Tipo=Monete[1].Tipo;
                        el.GruppoWallet=GruppoWallet;
                        el.Data =Data;
                        //if (el.QtaOri.isBlank())System.out.println("SC : "+el.Data);
                        StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                        

                    }else{
                        //In questo caso devo spostare solo lo stack da una moneta all'altra
                        //visto che non è fiscalmente rilevante
                        
                        ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStack, Monete[0].Moneta, Monete[0].Qta,GruppoWallet, true);
                        BigDecimal qtaUscenteTotale=new BigDecimal(Monete[0].Qta).abs();
                        BigDecimal qtaEntranteTotale=new BigDecimal(Monete[1].Qta).abs();
                        BigDecimal qtaEntranteRimanente=new BigDecimal(Monete[1].Qta).abs();
                        while (!StackRitorno.isEmpty()) {
                            //per ogni elemento trovato devo inserire il giusto quantitativo nello stack della moneta entrante
                            ElementiStack el = StackRitorno.pop();
                            el.Moneta=Monete[1].Moneta;
                            BigDecimal qtaEstratta=new BigDecimal(el.Qta);
                            BigDecimal qtaEntrante;
                            if (StackRitorno.isEmpty()) {
                                //Se è l'ultimo rigo dello stack allora la qtaEntranteRimanente è quella che c'è
                                el.Qta=qtaEntranteRimanente.stripTrailingZeros().abs().toPlainString();
                            }
                            else{
                                //Altrimenti è quella calcolata
                               // System.out.println (qtaEstratta.toPlainString()+ " - "+qtaUscenteTotale.toPlainString()+" - "+qtaEntranteTotale);
                                qtaEntrante=qtaEstratta.divide(qtaUscenteTotale,DecimaliCalcoli+10,RoundingMode.HALF_UP).multiply(qtaEntranteTotale).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).stripTrailingZeros();
                                el.Qta=qtaEntrante.stripTrailingZeros().abs().toPlainString();
                                qtaEntranteRimanente=qtaEntranteRimanente.subtract(qtaEntrante);
                            }
                            if (!el.IDOri.contains("Errore")){
                                //Se lo scambio non è rilevante fiscalmente allora aggiungo
                                //all'elenco degli id coinvolti nel movimento anche questo id
                                //fosse stato rilvenate fiscalmente non serviva perchè in quel caso il movimento sarebbe stato chiuso e il movimento di chiusura è sempre salvato
                                el.AggiungiID(IDTransazione);
                                //if (el.QtaOri.isBlank())System.out.println("Scambio non rilevante : "+el.Data);
                                StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                                }
                            //Nel caso in cui il lifo venga applicato alla totalità dei wallet devo anche spostare la qta
                            //dal wallet di origine a quello di destinazione (questo per ricalcare quello che fa tatax)

                            //System.out.println("Dentro RW MON:"+el.Moneta+" QTA:"+el.Qta+" MonOri:"+el.MonOri+" QtaOri:"+el.QtaOri);
                          //  if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) SpostaQta(GruppoWallet,el,el.GruppoWalletOri);
                          //Se non è rilevante non faccio nulla perchè
                        }
                    }
                    
                    
                    //Queste sono categorie per cui non va fatto rw quindi le escludo

                } else if (IDTS[4].equals("DC")//Deposito Crypto
                        || IDTS[4].equals("PC"))//Prelievo Crypto
                {
                    //Le tipologie possono essere le seguenti
                    //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
                    //PCO -> Cashout o similare (prelievo)
                    //PTW -> Trasferimento tra Wallet (prelievo)
                    //DTW -> Trasferimento tra Wallet (deposito)
                    //DAI -> Airdrop o similare (deposito)
                    //DCZ -> Costo di carico 0 (deposito)
                    if (v[18].isBlank()) {
                        //Se i movimenti non sono classificati identifico come Cashout i movimenti in uscita
                        //e come reward i movimenti in ingresso
                        //Devo anche emettere qualche sorta di avviso
                        //System.out.println("Movimento non classificato");
                        if (IDTS[4].equals("PC")) {
                            //System.out.println("Movimento non classificato Prelievo");
                            //Lo considero come cash out e chiudo l'RW
                            ChiudiRWFR(Monete[0], CryptoStack, GruppoWallet, Data, Valore, "Prelievo Sconosciuto",IDTransazione);
                        } else {
                        //System.out.println("Movimento non classificato Deposito");
                        //Lo considero come un acquisto e apro un nuovo RW
                        //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
                        //PCO -> Cashout o similare (prelievo)
                        ElementiStack el = new ElementiStack();
                        el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                        el.CostoOri = Valore;//Costo di partenza della moneta originale
                        el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                        el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                        el.DataOri = Data;//Data di partenza
                        el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza
                        el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                        el.Qta = Monete[1].Qta; //Qta di riferimento
                        el.TipoMonetaOri=Monete[1].Tipo;
                        el.Tipo=Monete[1].Tipo;
                        el.GruppoWallet=GruppoWallet;
                        el.Data =Data;
                        //if (el.QtaOri.isBlank())System.out.println("DC-PC : "+el.Data);
                        StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                           // StackLIFO_InserisciValoreFR(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data, IDTransazione, GruppoWallet);
                        }
                    } else if (v[18].contains("PWN") || v[18].contains("PCO")) {
                        //Chiudo RW
                        ChiudiRWFR(Monete[0], CryptoStack, GruppoWallet, Data, Valore, "Cashout o Similare", IDTransazione);
                    } else if (v[18].contains("DAI") || v[18].contains("DCZ")) {
                        //Apro nuovo RW
                        //DAI -> Airdrop o similare (deposito)
                        //DCZ -> Costo di carico 0 (deposito)
                        ElementiStack el = new ElementiStack();
                        el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                        if (v[18].contains("DAI")&&
                               (StakingZero||!Funzioni.RewardRilevante(IDTransazione)) )
                        { 
                            el.CostoOri="0.00";
                            }
                        else  el.CostoOri = Valore;//Costo di partenza della moneta originale
                        el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                        el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                        el.DataOri = Data;//Data di partenza
                        el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza
                        el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                        el.Qta = Monete[1].Qta; //Qta di riferimento
                        el.TipoMonetaOri=Monete[1].Tipo;
                        el.Tipo=Monete[1].Tipo;
                        el.GruppoWallet=GruppoWallet;
                        el.Data =Data;
                        //if (el.QtaOri.isBlank())System.out.println("DAI-DCZ : "+el.Data);
                        StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                    } else if (v[18].contains("PTW")) {
                        // System.out.println(StessoGruppoWalletContropate(IDTransazione) + " : " + IDTransazione);
                        //Se è un trasferimento tra wallet dello stesso gruppo non faccio nulla
                        //Se è un trasferimento tra wallet di gruppi diversi chiudo l'RW
                        //Se è trasferimento per scambio non faccio nulla perchè viene tutto gestito nel momento del deposito sul wallet di destinazione
                        //Se è un trasferimento a vault non faccio nulla perchè ritengo che il valut faccia sempre riferimento al mio wallet quindi di conseguenza i token non sono mai usciti
               
               //SE E' UN PRELIEVO NON FACCIO NULLA AL MASSIMO POTREI CHIUDERE L'RW PER POI APRIRNE UNO DOPO
               //MA IN LINEA DI MASSIMA LA LOGICA è QUELLA DI NON CONSIDERARE GLI SCAMBI TRA WALLET DI PROPRIETA'
               
               
                        if (v[18].contains("PTW - Trasferimento tra Wallet")//E' un trasferimento tra wallet (Prelievo)
                                &&
                                (//Rilevanza.equalsIgnoreCase("D") || 
                                ChiudiRWsuTrasferimento)//decido di chiudere rw su traferimento o su ogni movimento 
                                && !StessoGruppoWalletContropate(IDTransazione)//Trasferimento tra gruppi diversi
                                // &&!LiFoComplessivo
                                ) {
                            ChiudiRWFR(Monete[0], CryptoStack, GruppoWallet, Data, Valore, "Trasferimento su altro Wallet", IDTransazione);

                        }

                    }else if (v[18].contains("DAC")) {

                        //Apro RW nel caso di un acquisto Crypto da piattaforma esterna
                        ElementiStack el = new ElementiStack();

                        el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                        el.CostoOri = Valore;//Costo di partenza della moneta originale
                        el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                        el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                        el.DataOri = Data;//Data di partenza
                        el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza

                        el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                        el.Qta = Monete[1].Qta; //Qta di riferimento
                        el.TipoMonetaOri=Monete[1].Tipo;  
                        el.Tipo=Monete[1].Tipo;
                        el.GruppoWallet=GruppoWallet;
                        el.Data =Data;
                        //if (el.QtaOri.isBlank())System.out.println("DAC : "+el.Data);
                        StackLIFO_InserisciValoreFR(CryptoStack,GruppoWallet,el);
                    } 
                    else if (v[18].contains("DTW")) {
                        //Se è un trasferimento tra wallet dello stesso gruppo non faccio nulla
                        //Se è un trasferimento tra wallet di gruppi diversi apro l'RW
                        //Se è un deposito per scambio differito nello stesso gruppo wallet non faccio nulla
                        //Se è un deposito per scambio differito da un diverso gruppo wallet chiudo RW vecchio Wallet apro RW nuovo Wallet
                        //Da ricordare che scambio, invio e ricezione del token vengono generati nello stesso istante
                        //E lo scambio già di per se va a chiudere un rw e aprirne uno nuovo
                        if (v[18].contains("DTW - Trasferimento tra Wallet")) {
                            //IN QUESTO CASO CHIUDO SPOSTO IL MOVIMENTO DAL GRUPPO WALLET DI ORIGINE A QUELLO DI DESTINAZIONE
                    /*         if (!StessoGruppoWalletContropate(IDTransazione)&&DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) 
                                {
                                    //Recupero i movimenti dallo stack e li sposto sul wallet di destinazione per quanto riguarda la conta delle qta
                                    //faccio riferimento alla moneta
                                    ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStack, Monete[1].Moneta, Monete[1].Qta, false);
                                    while (!StackRitorno.isEmpty()) {
                                        //per ogni elemento trovato devo inserire il giusto quantitativo nello stack della moneta entrante
                                        String gruppoControparte = RitornaGruppoWalletControparte(IDTransazione);
                                        ElementiStack el = StackRitorno.pop();
                                        //Metto nel wallet attuale di destinazione e aggiungo ai movimenti gestiti
                                        SpostaQta(GruppoWallet,el,gruppoControparte);

                                    }
                                    
                                    
                                } */
                           
                            //if (!StessoGruppoWalletContropate(IDTransazione)&&(DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("NO")))
                            if (!StessoGruppoWalletContropate(IDTransazione)
                                  //  &&!LiFoComplessivo
                                    )
                            {//Controllo se fanno parte dello stesso gruppo
                                //Se non fanno parte dello stesso gruppo controllo se voglio generare un nuovo rigo ad ogni transazione
                                //se non è così sposto solo i valori tra un gruppo ad un altro, altrimenti greo un nuovo rigo sul wallet
                                if (//Rilevanza.equalsIgnoreCase("D")||
                                    ChiudiRWsuTrasferimento) 
                                {
                                    //Qua ci vado solo quando il trasferimento voglio che sia fiscalmente rilevante
                                    //o devido che devo aprire un nuovo rw quando scambio le crypto tra i wallet
                                    ElementiStack el = new ElementiStack();
                                    el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                                    el.CostoOri = Valore;//Costo di partenza della moneta originale
                                    el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                                    el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                                    el.DataOri = Data;//Data di partenza
                                    el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza
                                    el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                                    el.Qta = Monete[1].Qta; //Qta di riferimento
                                    el.TipoMonetaOri=Monete[1].Tipo;
                                    el.Tipo=Monete[1].Tipo;
                                    el.GruppoWallet=GruppoWallet;
                                    el.Data =Data;
                                  //  if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) SpostaQta(GruppoWallet,el);
                                  //if (el.QtaOri.isBlank())System.out.println("DTW : "+el.Data);
                                    StackLIFO_InserisciValoreFR(CryptoStack, GruppoWallet, el);
                                }
                                else if (!LiFoComplessivo){//solo se LiFo complessivo è false allora sposto lo stack da un wallet all'altro
                                                            //appunto perchè decido di non spezzare l'rw sugli scambi
                                                            //Se Lifo complessivo è true non serve spostare nulla perchè lo stack è lo stesso
                                    //Se non esistono le mappe per il wallet controparte le genero
                                    String gruppoControparte = RitornaGruppoWalletControparte(IDTransazione);
                                    Map<String, ArrayDeque> CryptoStackControparte;
                                  //  Map<String, Moneta> QtaCryptoControparte;
                                    if (MappaGrWallet_CryptoStack.get(gruppoControparte) == null) {
                                        //se non esiste ancora lo stack lo creo e lo associo alla mappa
                                        //stessa cosa faccio per la lista per l'rw
                                        //stessa cosa faccio per il gruppo delle qta
                                        ListaRW = new ArrayList<>();
                                        CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(gruppoControparte, ListaRW);
                                        CryptoStackControparte = new TreeMap<>();
                                      //  QtaCryptoControparte = new TreeMap<>();

                                        MappaGrWallet_CryptoStack.put(gruppoControparte, CryptoStackControparte);
                                        MappaGrWallet_QtaCrypto.put(gruppoControparte, QtaCrypto);
                                    } else {
                                        //altrimenti lo recupero per i calcoli
                                        CryptoStackControparte = MappaGrWallet_CryptoStack.get(gruppoControparte);
                                        //QtaCrypto serve per trovare le rimanenze di ogni crypto e compilare la giacenza di fine o inizio anno.
                                     //   QtaCryptoControparte = MappaGrWallet_QtaCrypto.get(gruppoControparte);
                                    }

                                    //Tolgo dal wallet di origine
                                    ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStackControparte, Monete[1].Moneta, Monete[1].Qta,GruppoWallet, true);
                                    //System.out.println(Monete[1].Qta);
                                    while (!StackRitorno.isEmpty()) {
                                        //per ogni elemento trovato devo inserire il giusto quantitativo nello stack della moneta entrante
                                        ElementiStack el = StackRitorno.pop();
                                        //Metto nel wallet attuale di destinazione e aggiungo ai movimenti gestiti
                                        el.AggiungiID(RitornaIDControparte(IDTransazione));
                                        el.AggiungiID(IDTransazione);
                                        //System.out.println(el.ListaIDcoinvolti);
                                        //if (el.QtaOri.isBlank())System.out.println(el.Moneta+" DTW2 : "+el.MonOri+" - "+el.ListaIDcoinvolti);
                                        StackLIFO_InserisciValoreFR(CryptoStack, GruppoWallet, el);

                                    }
                                } 
                            }

                            //se soddisfa questa condizione sono in presenza di un trasferimento tra wallet
                            //adesso devo verificare se il Gruppo wallet della controparte è lo stesso del mio o meno
                         /*   if (StessoGruppoWalletContropate(IDTransazione)) {
                                //Non faccio nulla se sono nello stesso gruppo
                            } else {
                                //Se è un trasferimento tra wallet di gruppi diversi apro il nuovo RW
                                StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data, IDTransazione, GruppoWallet);
                            }*/
                        } else if (v[18].contains("DTW - Scambio Differito")) {
                            //Es. Scambio differito
                            //Mov. 1 - Wallet 1 - Invia 1 ETH a piattaforma di scambio 
                            //Mov. 2 - Wallet 1 - La piattaforma di scambio riceve l'ETH (DTW - Trasferimento Interno)
                            //Mov. 3 - Wallet 1 - La piattaforma di scambio scambia ETH con BTC
                            //Mov. 4 - Wallet 1 - La piattaforma di scambio invia BTC al Wallet 2
                            //Mov. 5 - Wallet 2 - La piattaforma di scambio riceve i BTC (DTW - Scambio Differito)
                            //Se è l'ultimo movimento di uno scambio differito allora chiudo l'rw del vecchio wallet e apro l'RW del nuovo
                            //infatti la conversione tra le monete è già stata classificata
                            //l'RW chiuso non verrà considerato in quanto avrà zero come lasso di tempo ovvero GG, scambio token invio e ricezione avvengono infatti nello stesso momento.
                           /*   if (!StessoGruppoWalletContropate(IDTransazione)&&DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) 
                                {
                                    //Recupero i movimenti dallo stack e li sposto sul wallet di destinazione per quanto riguarda la conta delle qta
                                    //faccio riferimento alla moneta
                                    ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStack, Monete[1].Moneta, Monete[1].Qta, false);
                                    while (!StackRitorno.isEmpty()) {
                                        //per ogni elemento trovato devo inserire il giusto quantitativo nello stack della moneta entrante
                                        String gruppoControparte = RitornaGruppoWalletControparte(IDTransazione);
                                        ElementiStack el = StackRitorno.pop();
                                        //Metto nel wallet attuale di destinazione e aggiungo ai movimenti gestiti
                                        SpostaQta(GruppoWallet,el,gruppoControparte);

                                    }
                                    
                                    
                                } */
                            //!LiFoComplessivo
                            if (!StessoGruppoWalletContropate(IDTransazione)) //Gruppi diversi
                            {
                         //   if (!StessoGruppoWalletContropate(IDTransazione)) {
                                //Se non esistono le mappe per il wallet controparte le genero
                                String gruppoControparte;
                                if(LiFoComplessivo)gruppoControparte=GruppoWallet;//il gruppoControparte nel caso di LiFo complessivo è uguale al gruppo originale
                                else gruppoControparte=RitornaGruppoWalletControparte(IDTransazione);
                                Map<String, ArrayDeque> CryptoStackControparte;  
                             //   Map<String, Moneta> QtaCryptoControparte;
                                //Se non ho lifo complessivo e non esiste wallet controparte lo creao vuoto
                                if (MappaGrWallet_CryptoStack.get(gruppoControparte) == null) {
                                    ListaRW = new ArrayList<>();
                                    CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(gruppoControparte, ListaRW);
                                    CryptoStackControparte = new TreeMap<>();
                                  //  QtaCryptoControparte = new TreeMap<>();
                                    MappaGrWallet_CryptoStack.put(gruppoControparte, CryptoStackControparte);
                                    MappaGrWallet_QtaCrypto.put(gruppoControparte, QtaCrypto);
                                } else {
                                    //Altrimenti mi prendo la qta della controparte
                                    CryptoStackControparte = MappaGrWallet_CryptoStack.get(gruppoControparte);
                                 //   QtaCryptoControparte = MappaGrWallet_QtaCrypto.get(gruppoControparte);
                                }

                                if (//Rilevanza.equalsIgnoreCase("D")||
                                    ChiudiRWsuTrasferimento) 
                                {
                                    
                                    //Questo nel caso in cui non chiudo l'rw nel momento dello scambio
                                    //Apro RW con la moneta che ho ricevuto e chiudo l'RW dell'altro Wallet
                                    //Infatti il momento in cui ricevo la moneta coincide con il momento in cui l'altro wallet mi spedisce i token
                                    ElementiStack el = new ElementiStack();
                                    el.IDOri = IDTransazione;//ID del movimento da cui tutto ha avuto origine
                                    el.CostoOri = Valore;//Costo di partenza della moneta originale
                                    el.MonOri = Monete[1].Moneta;//Moneta di partenza di tutto il giro del Lifo
                                    el.QtaOri = Monete[1].Qta;//Qta di partenza della moneta originale
                                    el.DataOri = Data;//Data di partenza
                                    el.GruppoWalletOri = GruppoWallet;//Gruppo Wallet di partenza
                                    el.Moneta = Monete[1].Moneta; //Moneta di riferimento
                                    el.Qta = Monete[1].Qta; //Qta di riferimento
                                    el.TipoMonetaOri=Monete[1].Tipo;
                                    el.Tipo=Monete[1].Tipo;
                                    el.GruppoWallet=GruppoWallet;
                                    el.Data =Data;
                                  //  if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) SpostaQta(GruppoWallet,el);
                                  //if (el.QtaOri.isBlank())System.out.println("scambio differito : "+el.Data);
                                    StackLIFO_InserisciValoreFR(CryptoStack, GruppoWallet, el);
                                    
                                    ChiudiRWFR(Monete[1], CryptoStackControparte, gruppoControparte, Data, Valore, "Trasferimento su altro Wallet", "Giorni Detenzione Zero");
                                }
                                
                                else if (!LiFoComplessivo) {//se arrivo qua significa che decido di non spezzare l'rw sui trasfeirmenti
                                    //e solo se LiFo complessivo è false allora sposto lo stack da un wallet all'altro
                                    //Se Lifo complessivo è true non serve spostare nulla perchè lo stack è lo stesso

                                    //Tolgo dal wallet di origine
                                    ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStackControparte, Monete[1].Moneta, Monete[1].Qta, GruppoWallet, true);
                                    while (!StackRitorno.isEmpty()) {
                                        //per ogni elemento trovato devo inserire il giusto quantitativo nello stack della moneta entrante
                                        ElementiStack el = StackRitorno.pop();
                                        //Metto nel wallet attuale di destinazione
                                        //if (el.QtaOri.isBlank())System.out.println("scambio differito 2 : "+el.Data);
                                        StackLIFO_InserisciValoreFR(CryptoStack, GruppoWallet, el);

                                    }
                                }
                            }

                        }
                    } else {                       
                        System.out.println("Attenzione movimento di deposito o prelievo non classificato nella funzione AggiornaRW() in CalcoliRW");
                        System.out.println("Movimento non Gestito : " +v[18]);
                    }
                } else {
                    System.out.println("Attenzione movimento " + IDTS[4] + " non considerato in CalcoliRW");
                }

                //PARTE 3
           } } else if (Anno > AnnoRiferimento) {
                //Non faccio nulla

            }
            

        }
                //Se arrivato a questo punto non ho ancora creato i primi movimenti li creo ora
                //1 - Inseirsco nello stack tutti i valori iniziali precedentemente trovati
                //2 - Uso il lifo per il calcolo dei valori RW
                if (PrimoMovimentoAnno) {                   
                    CreaPrimiMovimenti(MappaGrWallet_QtaCryptoInizio,MappaGrWallet_CryptoStack,inizio,DataInizioAnno);
                   // System.out.println("Secondo Primo Movimento");
                    //PrimoMovimentoAnno = false;
                 }  
 
                //finito il ciclo
                //1 - Trovo il valore di fine anno di riferimento relativo a tutti i token e chiudo tutti i conti aperti
                //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                //Se voglio avere il lifo complessivo allora considero le giacenze totali e non quelle per singolo wallet
               // if(DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SI")) MappaGrWallet_QtaCrypto=MappaGrWallet_QtaCryptoTOT;
             //  Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto2=new TreeMap<>();
             //   MappaGrWallet_QtaCrypto2 .putAll(MappaGrWallet_QtaCrypto);
              
               
                    for (String key : MappaGrWallet_QtaCryptoInizio.keySet()) {
                        //Key è il Gruppo Wallet
                        String Valori[]=DatabaseH2.Pers_GruppoAlias_Leggi(key);
                        String RW_MostraGiacenzeSePagaBollo=DatabaseH2.Pers_Opzioni_Leggi("RW_MostraGiacenzeSePagaBollo");
                        //Leggo se il gruppo ha pagato o meno e a seconda del flag RW_MostraGiacenzeSePagaBollo
                        //aggiorno il boolean che dice se devo o meno mostrare la giacenza inizio e fine anno al posto dei calcoli
                        boolean MostraGiacenzeSePagaBollo=false;
                        if (Valori[2].equals("S")&&RW_MostraGiacenzeSePagaBollo.equals("SI"))MostraGiacenzeSePagaBollo=true;
                       // System.out.println(key+" _ "+RW_MostraGiacenzeSePagaBollo+" _ "+MostraGiacenzeSePagaBollo);
                        
                    Map<String, Moneta> a = MappaGrWallet_QtaCryptoInizio.get(key);
                   // System.out.println(key);
                    for (Moneta m : a.values()) {
                        if (!m.Tipo.equalsIgnoreCase("FIAT")&&new BigDecimal(m.Qta).compareTo(new BigDecimal(0))!=0) {
                           // long fine = OperazioniSuDate.ConvertiDatainLongMinuto(DataFineAnnoCalcoloPrezzi);
                          // m.Moneta="BTC";
                           //if (m.Moneta.equals("NEXO"))System.out.println(m.Qta);
                            m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, fine, null, true, 15, m.Rete);
                            //System.out.println(m.Moneta+" - "+m.Qta+" - "+fine+" - "+m.Prezzo);
                            
                           // System.out.println(m.Moneta+"-"+m.Prezzo);
                            //System.out.println(Prezzi.DammiPrezzoTransazione(m, null,fine, null, true, 15, m.Rete));
                            //System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo+ " - "+m.MonetaAddress+ " - "+ m.Rete);
                            Map<String, ArrayDeque> CryptoStackTemp;
                            String GRWallet=key;
                            if (LiFoComplessivo)GRWallet="Unico 01";
                            CryptoStackTemp = MappaGrWallet_CryptoStack.get(GRWallet);
                            
                            //Questa parte sotto è momentaneamente disabilitata con quell'if perchè non mi sembra corretto concettualmente
                            //anche se ho riscontrato lo stesso comportamento in altri software
                            //con questa opzione si rischia di avere valori di chiusura rw di un wallet più alti di quelli reali
                            if (DatabaseH2.Pers_Opzioni_Leggi("RW_LiFoComplessivo").equals("SIs")) {
                             //   ChiudiRWFR(m, CryptoStackTemp, "null", DataFineAnno, m.Prezzo, "Fine Anno", "Giacenza Fine Anno");

                                ArrayDeque<ElementiStack> StackRitorno = StackLIFO_TogliQtaFR(CryptoStackTemp, m.Moneta, m.Qta, "ininfluente", false);
                               // List<Moneta> listaDaConsolidare = new ArrayList<>();
                                Map<String, Moneta> listaDaConsolidare=new TreeMap<>();
                                while (!StackRitorno.isEmpty()) {
                                    ElementiStack el = StackRitorno.pop();
                                    Moneta m2=m.ClonaMoneta();
                                    //if (!m2.Moneta.equals(el.Moneta))System.out.println("AGGIORNARWFR - Errore moneta diversa");
                                    m2.Prezzo=new BigDecimal(m2.Prezzo).divide(new BigDecimal(m2.Qta),DecimaliCalcoli+10,RoundingMode.HALF_UP).multiply(new BigDecimal(el.Qta)).setScale(DecimaliCalcoli,RoundingMode.HALF_UP).toPlainString();
                                    m2.Moneta=el.Moneta;
                                    m2.Qta=el.Qta;
                                    m2.GruppoRW=el.GruppoWallet;
                                    if (listaDaConsolidare.get(el.GruppoWallet)==null){
                                        listaDaConsolidare.put(el.GruppoWallet, m2);
                                    }
                                    else{
                                        Moneta m3=listaDaConsolidare.get(el.GruppoWallet);
                                        //ora sommo la qta di m2 con quella di m3
                                        //tanto so già che la moneta che sto analizzando è la stessa
                                        m3.Qta=new BigDecimal(m2.Qta).add(new BigDecimal(m3.Qta)).stripTrailingZeros().toPlainString();
                                        m3.Prezzo=new BigDecimal(m2.Prezzo).add(new BigDecimal(m3.Prezzo)).toPlainString();
                                    }
                                    }
                                int i=0;
                                for(String GRWAL : listaDaConsolidare.keySet()){
                                    Moneta m2=listaDaConsolidare.get(GRWAL);
                                    ChiudiRWFR(m2, CryptoStackTemp, GRWAL, DataFineAnno, m2.Prezzo, "Fine Anno", "Giacenza Fine Anno");
                                    if (m2.Qta.contains("-"))System.out.println(m2.Qta);
                                    if (CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(m2.GruppoRW) == null) {
                                        List<Moneta> li = new ArrayList<>();
                                        li.add(m2);
                                        CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.put(m2.GruppoRW, li);
                                    } else {
                                        List<Moneta> li = CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(m2.GruppoRW);
                                        li.add(m2);
                                    }
                                }
                             
                                
                            } else {
                                //Solo se rilvenza è di diverso da A proseguo con i calcoli
                                //nel caso sia uguale ad A viene gestito a fine ciclo
                                if (!(Rilevanza.equalsIgnoreCase("A") || MostraGiacenzeSePagaBollo)) {
                                    //Lancio la funzione solo se la rimanenza è positiva, se è negativa non devo chiudere nulla
                                    //anzi se lo facessi creerei solo problemi in caso di giacenze negative
                                    if (!m.Qta.contains("-")){
                                    ChiudiRWFR(m, CryptoStackTemp, key, DataFineAnno, m.Prezzo, "Fine Anno", "Giacenza Fine Anno");
                                    }
                                }
                               /* if (m.Qta.contains("-")){
                                    System.out.println(m.Moneta+" - "+m.Qta);
                                }*/
                            //Questo qua sotto popola una lista per ogni gruppo wallet contenente la giacenza di ciascuna moneta ad inizio anno
                            
                            if (CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(key)==null){
                                    List<Moneta> li=new ArrayList<>();
                                    li.add(m);
                                    CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.put(key, li);
                                }else{
                                    List<Moneta> li=CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(key);
                                    li.add(m);
                                }
                         }
                        }
                    }
                    
                    //se la rilevanza è uguale ad A significa che voglio vedere solo le giacenze iniziali e finali e quelle andrò a calcolare
                    if (Rilevanza.equalsIgnoreCase("A")|| MostraGiacenzeSePagaBollo){ 
                        ChiudiRWGiacenzeFinali (key);
                    }
                }
        
        SistemaErroriInListe();
        
    }
    
    
    
    
  
        
        
  public static class ElementiStack {

  public String MonOri;
  public String QtaOri;
  public String CostoOri;
  public String IDOri;
  public String DataOri;
  public String GruppoWalletOri;
  public String TipoMonetaOri;
  
  
  public String Moneta;
  public String Qta;
  public String Tipo; //Ancora non correttamente gestiti nelle funzioni
  public String GruppoWallet;
  public String Data; //Ancora non correttamente gestiti nelle funzioni, in particolare in togliQta
  
  public String ListaIDcoinvolti="";
  
  public void AggiungiID(String ID){
      if (ListaIDcoinvolti.isBlank())
        ListaIDcoinvolti=ID;
      else ListaIDcoinvolti=ListaIDcoinvolti+","+ID;
  }

}
        
        
        
        
        
        
        
        
        
        
        
        
        
}

 
