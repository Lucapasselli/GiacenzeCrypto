/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;


//ATTENZIONE : PREZZO CRO DA RIVEDERE!!!!!!


import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_RW {
    
    
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
   
       
    public static void StackLIFO_InserisciValore(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String Valore, String Data) {
    
    ArrayDeque<String[]> stack;
    String valori[]=new String[4];
    valori[0]=Moneta;
    valori[1]=new BigDecimal(Qta).abs().toPlainString();
    valori[2]=Valore;
    valori[3]=Data;
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
       
       
  public static List<String> StackLIFO_TogliQta(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,boolean toglidaStack) {
    
    //in ritorno devo avere la lista delle qta estratte  e valore con relative date
    //es. ListaRitorno[0]=1,025;15550;2023-01-01 00:00   (qta iniziale;valore iniziale;data iniziale)
    List<String> ListaRitorno=new ArrayList<>();
   // String ritorno="0.00";
    if (!Qta.isBlank()&&!Moneta.isBlank()){//non faccio nulla se la momenta o la qta non è valorizzata
    ArrayDeque<String[]> stack;

    BigDecimal qtaRimanente=new BigDecimal(Qta).abs();
   // BigDecimal costoTransazione=new BigDecimal("0");
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
            String dataEstratta=ultimoRecupero[3];
         /*  if (Moneta.equalsIgnoreCase("usdt")){ 
                System.out.println(ultimoRecupero[1]+" - "+ultimoRecupero[2]+" - "+stack.size());
                System.out.println(qtaRimanente);
                }*/
            //System.out.println(qtaEstratta+" - "+costoEstratta);
            if (qtaEstratta.compareTo(qtaRimanente)<=0)//se qta estratta è minore o uguale alla qta rimanente allora
                {
                //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
                qtaRimanente=qtaRimanente.subtract(qtaEstratta);
                //poi inserisco nella lista i dati che mi servono per chiudere le righe dell'rw
                ListaRitorno.add(qtaEstratta.toPlainString()+";"+costoEstratta.toPlainString()+";"+dataEstratta);
            }else{
                //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
                //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
                //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
                //non ho più qta rimanente
                String qtaRimanenteStack=qtaEstratta.subtract(qtaRimanente).toPlainString();
                //System.out.println(qtaRimanenteStack);
               // System.out.println(qtaEstratta+" - "+qtaRimanente+"- "+qtaRimanenteStack);
                String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,30,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).stripTrailingZeros().toPlainString();
                String valori[]=new String[]{Moneta,qtaRimanenteStack,valoreRimanenteSatck,dataEstratta};
                stack.push(valori);
                BigDecimal costoTransazione=costoEstratta.subtract(new BigDecimal(valoreRimanenteSatck));
                ListaRitorno.add(qtaRimanente.toPlainString()+";"+costoTransazione.toPlainString()+";"+dataEstratta);
                
                qtaRimanente=new BigDecimal("0");//non ho più qta rimanente
            }
            
        }
        //pop -> toglie dello stack l'ultimo e recupera il dato
        //peek - > recupera solo il dato

    }
   // ritorno=costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    return ListaRitorno;
    //ogni singolo elemento di listaRitorno è così composto     1,025;15550;2023-01-01 00:00   (qta iniziale;valore iniziale;data iniziale)
   // System.out.println(Moneta +" - "+stack.size());
}      
    
    
    
    
    
    
    
    
         public static void AggiornaRW(){
             
        //PARTE 1 : Calcolo delle Giacenze iniziali e inserimento nello stack
             
         int AnnoRiferimento=Integer.parseInt("2023");  
         String DataInizioAnno="2023-01-01 00:00";
         String DataFineAnno="2023-12-31 23:59";
         boolean PrimoMovimentoAnno=true;
         boolean UltimoMovimentoAnno=true;
             
             
             
////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere

       // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();
        Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto = new TreeMap<>();
        Map<String, ArrayDeque> CryptoStack;// = new TreeMap<>();
        Map<String, Moneta> QtaCrypto = new TreeMap<>();
        
      

        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
               // System.out.println(GruppoWallet);

                if (MappaGrWallet_CryptoStack.get(GruppoWallet) == null) {
                    //se non esiste ancora lo stack lo creo e lo associo alla mappa
                    CryptoStack = new TreeMap<>();
                    QtaCrypto = new TreeMap<>();
                    MappaGrWallet_CryptoStack.put(GruppoWallet, CryptoStack);
                    MappaGrWallet_QtaCrypto.put(GruppoWallet, QtaCrypto);
                } else {
                    //altrimenti lo recupero per i calcoli
                    CryptoStack = MappaGrWallet_CryptoStack.get(GruppoWallet);
                    QtaCrypto = MappaGrWallet_QtaCrypto.get(GruppoWallet);
                }
            String TipoMU = RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
            String TipoME = RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
            String IDTransazione=v[0];
            String Data=v[1];
            String IDTS[]=IDTransazione.split("_");
            String MonetaU=v[8];
            String QtaU=v[10];
            String MonetaE=v[11];
            String QtaE=v[13];
            String Valore=v[15];
            String VecchioPrezzoCarico="0.00";
            String NuovoPrezzoCarico="0.00";
            String Plusvalenza="0.00";
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
                            Monete[0].Rete=Rete;
                            Monete[1].Moneta = v[11];
                            Monete[1].Tipo = v[12];
                            Monete[1].Qta = v[13];
                            Monete[1].Rete=Rete;
            
            int Anno=Integer.parseInt(Data.split("-")[0]); 
            if (Anno<AnnoRiferimento){
                //Faccio i conti per i valori iniziali

                            //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                            for (int a = 0; a < 2; a++) {
                                //ANALIZZO MOVIMENTI
                                if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo)!=null) {
                                    //Movimento già presente da implementare
                                    Moneta M1 = QtaCrypto.get(Monete[a].Moneta+";"+Monete[a].Tipo);
                                    M1.Qta = new BigDecimal(M1.Qta)
                                            .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                                } else if (!Monete[a].Moneta.isBlank()) {
                                    //Movimento Nuovo da inserire
                                    Moneta M1 = new Moneta();
                                    M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                                    M1.Rete = Rete;
                                    QtaCrypto.put(Monete[a].Moneta+";"+Monete[a].Tipo, M1);

                                }
                            }
            }else if (Anno==AnnoRiferimento){
                //al primo movimento dell'anno successivo faccio questo:
                //1 - Inseirsco nello stack tutti i valori iniziali precedentemente trovati
                //2 - Uso il lifo per il calcolo dei valori RW
                if (PrimoMovimentoAnno) {
                      for (String key : MappaGrWallet_QtaCrypto.keySet()) {  
                          Map<String, Moneta> a=MappaGrWallet_QtaCrypto.get(key);
                        for (Moneta m : a.values()) {
                            if (!m.Tipo.equalsIgnoreCase("FIAT")) {
                                long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);
                                m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, inizio, null, true, 10, null);
                               // System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo);
                                Map<String, ArrayDeque> CryptoStackTemp = MappaGrWallet_CryptoStack.get(key);
                                StackLIFO_InserisciValore(CryptoStackTemp,m.Moneta,m.Qta,m.Prezzo,DataInizioAnno);
                                
                            }
                        }
                    }
                    PrimoMovimentoAnno = false;
                }
                //Adesso a seconda del tipo movimento devo comportarmi in maniera diversa
            //TIPOLOGIA = 0 (Vendita Crypto)
            if (IDTS[4].equals("VC")){
                //tolgo dal Lifo della moneta venduta e prendo la lista delle varie movimentazione
                List<String> ListaRitorno=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                //creo la riga per i quadri RW
                for (String elemento:ListaRitorno){
                    String Elementi[]=elemento.split(";");
                    //Elementi è così composta Qta;Prezzo;Data                    
                    System.out.println("Moneta="+MonetaU);
                    System.out.println("Qta="+Elementi[0]);
                    System.out.println("Data inizio periodo di detenzione="+Elementi[2]);
                    System.out.println("Prezzo a inizio periodo di detenzione="+Elementi[1]);
                    System.out.println("Data fine periodo di detenzione="+Data);
                    long DataLong=OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                    System.out.println("Prezzo a fine periodo di dentenzione="+Prezzi.DammiPrezzoTransazione(Monete[0],Monete[1], DataLong, "0.00", true, 2, Rete));
                    System.out.println("---------------------------------");
                }
               // Sysyem.out.println("Qta="+)
                
            } 
                
            }else if (Anno>AnnoRiferimento){
                //al primo movimento dell'anno successivo faccio questo:
                //1 - Trovo il valore di fine anno di riferimento relativo a tutti i token e chiudo tutti i conti aperti
                //2 - Termino il ciclo
            }
            
            
            
     /*       
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
                    //Tolgo dallo stack il costo di carico della cripèto uscita
                    VecchioPrezzoCarico=Calcoli_Plusvalenze.StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true);
                    
                    //Inserisco il costo di carico nello stack della cripto entrata
                    NuovoPrezzoCarico=VecchioPrezzoCarico;
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    //La plusvalenza va valorizzata a zero
                    Plusvalenza="0.00";
                                                        
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
                if(IDTS[4].equalsIgnoreCase("RW")||v[18].contains("DAI")) {
                                       
                    NuovoPrezzoCarico=Valore;
                    
                    Calcoli_Plusvalenze.StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico);
                    
                    Plusvalenza=Valore;
                    
                    VecchioPrezzoCarico="";                                
                
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
*/

        }
    }

    
    
}
