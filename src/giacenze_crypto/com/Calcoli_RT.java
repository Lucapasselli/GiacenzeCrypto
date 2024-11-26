/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.DecimaliCalcoli;
import static giacenze_crypto.com.CDC_Grafica.Funzioni_isNumeric;
import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
import static giacenze_crypto.com.Calcoli_Plusvalenze.RitornaTipoCrypto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_RT {
    
    
    public static AnalisiPlus CalcoliPlusvalenzeXAnno_OLD(){
        // DefaultTableModel ModelloTabellaRT = (DefaultTableModel) RT_Tabella_Principale.getModel();
      //  Funzioni_Tabelle_PulisciTabella(ModelloTabellaRT);
        AnalisiPlus ritorno=new AnalisiPlus();
        
        //ANNO,GRUPPOWALLET,MONETA,STACK della moneta
        Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta = new TreeMap<>();
        Map<String, Map<String, PlusXMoneta>> MappaGrWallet_MappaMoneta_PlusXMoneta= new TreeMap<>();
        Map<String,PlusXMoneta> MappaMoneta_PlusXMoneta;
        
        Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();//Wallet - Mappa(Moneta - Stack)
        Map<String, ArrayDeque> CryptoStack;//  - Stack
       // Map<String, ArrayDeque> CryptoStack2;//  - Stack
        
        //Questa la mappa che momorizzerà la Quantità di ogni singola moneta che poi dovrò sottrarre dallo stack per i calcoli
        //GRUPPO WALLET,MONETA,QTA
        Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto = new TreeMap<>();
        Map<String, Moneta> QtaCrypto;
        
        //controllo se devo o meno prendere in considerazione i gruppi wallet per il calcolo della plusvalenza
        boolean PlusXWallet=false;
        String PlusXW=DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if(PlusXW!=null && PlusXW.equalsIgnoreCase("SI")){
            PlusXWallet=true;
        }
        String Anno=null;
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
            if (Anno!=null&&!Anno.equals(v[1].split("-")[0])){
                //Questa funzione va poi replicata a fine ciclo
                //Se sto facendo un cambio anno allora devo prendere l'intero stack e calcolare la pluvalenza latente per ogni crypto quindi fare le somme
                //inoltre mi copio lo stack che poi mi servirà in futuro
               
                //Questa mappa sarà quela che poi andrò a popolare e terrò da parte per analisi future
                //Per ora non la utilizzerò
                //ANNO,GRUPPOWALLET,MONETA,STACK della moneta
              //  Map<String, Map<String, Map<String, ArrayDeque>>> MappaAnno_MappaGrWallet_CryptoStack = new TreeMap<>();

                ChiudiAnno_OLD(PlusvalenzeXAnno,Anno,MappaGrWallet_CryptoStack,MappaGrWallet_QtaCrypto,MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta);               
            }
            
            Anno=v[1].split("-")[0];
            //Identifico e creo una o più mappe per il cryptostack a seconda che sia o meno gestiti gli stack divisi per Wallet
            
            
            
            
           //A1 - INIZIALIZZA MAPPE PER GRUPPO WALLET
            
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
                if(!PlusXWallet)GruppoWallet="Wallet 01";
                if (MappaGrWallet_CryptoStack.get(GruppoWallet) == null) {
                    //se non esiste ancora lo stack lo creo e lo associo alla mappa
                    CryptoStack = new TreeMap<>();                              //Nuova Mappa degli Stack (Per Moneta)
                    MappaGrWallet_CryptoStack.put(GruppoWallet, CryptoStack);   //Mappa delle mappe degli Stack (Per Wallet)
                            
                    QtaCrypto = new TreeMap<>();                                //Nuova Mappa delle Qta (Per Moneta)
                    MappaGrWallet_QtaCrypto.put(GruppoWallet, QtaCrypto);       //Mappa delle mappe degli Qta (Per Wallet)            
                } else {
                    //altrimenti lo recupero per i calcoli
                    CryptoStack = MappaGrWallet_CryptoStack.get(GruppoWallet);  //Mappa delle mappe degli Stack (Per Wallet)
                    QtaCrypto = MappaGrWallet_QtaCrypto.get(GruppoWallet);      //Mappa delle mappe degli Qta (Per Wallet) 
                }
            
            //A2 - INIZIALIZZA MAPPE PER ANNO

            if (MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno)==null){
                //Se non esiste la mappa per l'anno controllo se esiste già una mappa che arriva dagli anni passati
                //Se non esiste ne creo una nuova
                //Altrimenti duplico quella precedente e la aggiungo alla mappa
                //La mappa es. del 2022 deve infatti contenere tutti i dati degli anni passati + quelli del 2022 e così via per gli anni successivi
                if (MappaGrWallet_MappaMoneta_PlusXMoneta.isEmpty())
                   { 
                       MappaGrWallet_MappaMoneta_PlusXMoneta= new TreeMap<>();
                   }
                else{
                    MappaGrWallet_MappaMoneta_PlusXMoneta=new TreeMap<>(MappaGrWallet_MappaMoneta_PlusXMoneta);
                }
                MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.put(Anno, MappaGrWallet_MappaMoneta_PlusXMoneta);
                }
            else {
                //Altrimenti la recupero
                MappaGrWallet_MappaMoneta_PlusXMoneta=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno);
            }
            // Se adesso non esiste la mappa per wallet la creo
            if (MappaGrWallet_MappaMoneta_PlusXMoneta.get(GruppoWallet)==null){
               // CryptoStack2 = new TreeMap<>();
                MappaMoneta_PlusXMoneta= new TreeMap<>();
                MappaGrWallet_MappaMoneta_PlusXMoneta.put(GruppoWallet, MappaMoneta_PlusXMoneta);
                }
            else {
                //Altrimenti la recupero
                MappaMoneta_PlusXMoneta=MappaGrWallet_MappaMoneta_PlusXMoneta.get(GruppoWallet);
            }
            
            
                //ANNO,GRUPPOWALLET,MONETA,STACK della moneta
        //Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta = new TreeMap<>();
       // Map<String, Map<String, PlusXMoneta>> MappaGrWallet_MappaMoneta_PlusXMoneta;
        //Map<String,PlusXMoneta> MappaMoneta_PlusXMoneta;
                
            
            
            
            
            //String AnnoTrans =v[0].split("-")[0];
            String TipoMU = RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
            String TipoME = RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
            //String IDTransazione=v[0];
           // String IDTS[]=IDTransazione.split("_");
            String MonetaU=v[8];
            String QtaU=v[10];
            String CostoCaricoU=v[16];
            String AddressU=v[26];
            String MonetaE=v[11];
            String QtaE=v[13];
            String CostoCaricoE=v[17];
            String AddressE=v[28];
            String Valore=v[15];
            String Rete=Funzioni.TrovaReteDaID(v[0]);
            


            
                
               
            
            //Se ci sono token in uscita, non sono FIAT e hanno un costo di carico gli tolgo dallo stack (costo di carico e prezzo li leggo dalla mappa)
            //Se non hanno costo di carico significa infatti che sono movimenti irrilevanti quali traferimenti interni o tra wallet dello stesso gruppo
            if (!TipoMU.isBlank()&&!TipoMU.equalsIgnoreCase("FIAT")){
                
                //PARTE 1
                //Faccio la somma delle qta e le scrivo nella mappa
                //Questa parte va fatta senza filtri perchè per le quantità conto tutte le uscite e le entrate
                if (QtaCrypto.get(MonetaU)==null){
                    //Se non ho ancora codificato la moneta nella mappa delle qta la inserisco
                    Moneta mon=new Moneta();
                    mon.Moneta=MonetaU;
                    mon.Qta=QtaU;
                    mon.MonetaAddress=AddressU;
                    mon.Rete=Rete;
                    mon.Tipo=TipoMU;
                    QtaCrypto.put(MonetaU, mon);
                }
                else{
                //adesso faccio la somma della qta nuova sulla vecchia
                Moneta mon=QtaCrypto.get(MonetaU);
                BigDecimal Qta=new BigDecimal(mon.Qta);
                Qta=Qta.add(new BigDecimal(QtaU));
                mon.Qta=Qta.toPlainString();
                }
                //Stessa cosa la inserisco nella mappa dei dettagli delle moneta        
                if (MappaMoneta_PlusXMoneta.get(MonetaU)==null){
                    //Se non ho ancora codificato la moneta nella mappa delle qta la inserisco
                    Moneta mon=new Moneta();
                    mon.Moneta=MonetaU;
                    mon.Qta=QtaU;
                    mon.MonetaAddress=AddressU;
                    mon.Rete=Rete;
                    mon.Tipo=TipoMU;
                    
                    //Stessa cosa la inserisco nella mappa dei dettagli delle moneta
                    PlusXMoneta PlusXm=new PlusXMoneta();
                    PlusXm.CompilaCampiDaMoneta(mon);
                    PlusXm.Put_Anno(Anno);
                    PlusXm.Put_Wallet(GruppoWallet);                     
                    MappaMoneta_PlusXMoneta.put(MonetaU, PlusXm);
                }
                else{
                //adesso faccio la somma della qta nuova sulla vecchia                
                PlusXMoneta PlusXm=MappaMoneta_PlusXMoneta.get(MonetaU);
                BigDecimal Qta=PlusXm.Get_Giacenza();
                Qta=Qta.add(new BigDecimal(QtaU));
                PlusXm.Put_Giacenza(Qta.toPlainString());
                
                }

                
                
                
                
                //PARTE 2
                //Tolgo il token dallo stack se non sono PTW, i PTW infatti vengono scaricati nel momento in cui arrivano a detinazione
                //e solo se si è deciso di distinguere le plusvalenze per gruppo wallet altrimenti vengono considerati alla stregua di un trasferimento interno
                //PTW sono prelievi che vanno ad altro wallet di proprietà
                //in ogni caso non vanno trasferiti in questa sezione
                if (!CostoCaricoU.isBlank()&&!v[18].contains("PTW")) StackLIFO_TogliQta_OLD(CryptoStack,MonetaU,QtaU,true);
            }

            
            //Se ci sono token in entrata gli aggiungo allo stack (costo di carico e prezzo li leggo dalla mappa che ha già tutti i dati)
            if (!TipoME.isBlank()&&!TipoME.equalsIgnoreCase("FIAT")){
                
                //PARTE 1
                //Faccio la somma delle qta e le scrivo nella mappa
                //Questa parte va fatta senza filtri perchè per le quantità conto tutte le uscite e le entrate
                if (QtaCrypto.get(MonetaE)==null){
                    //Se non ho ancora codificato la moneta la inserisco nella mappa
                    Moneta mon=new Moneta();
                    mon.Moneta=MonetaE;
                    mon.Qta=QtaE;
                    mon.MonetaAddress=AddressE;
                    mon.Rete=Rete;
                    mon.Tipo=TipoME;
                    QtaCrypto.put(MonetaE, mon);
                }
                else{
                //adesso faccio la somma della qta nuova sulla vecchia
                Moneta mon=QtaCrypto.get(MonetaE);
                BigDecimal Qta=new BigDecimal(mon.Qta);
                Qta=Qta.add(new BigDecimal(QtaE));
                mon.Qta=Qta.toPlainString();
                }
                
                if (MappaMoneta_PlusXMoneta.get(MonetaE)==null){
                    //Se non ho ancora codificato la moneta la inserisco nella mappa
                    Moneta mon=new Moneta();
                    mon.Moneta=MonetaE;
                    mon.Qta=QtaE;
                    mon.MonetaAddress=AddressE;
                    mon.Rete=Rete;
                    mon.Tipo=TipoME;                   
                    //Stessa cosa la inserisco nella mappa dei dettagli delle moneta
                    PlusXMoneta PlusXm=new PlusXMoneta();
                    PlusXm.CompilaCampiDaMoneta(mon);
                    PlusXm.Put_Anno(Anno);
                    PlusXm.Put_Wallet(GruppoWallet);                  
                    MappaMoneta_PlusXMoneta.put(mon.Moneta, PlusXm);
                }
                else{
                //adesso faccio la somma della qta nuova sulla vecchia               
                PlusXMoneta PlusXm=MappaMoneta_PlusXMoneta.get(MonetaE);
                BigDecimal Qta=PlusXm.Get_Giacenza();
                Qta=Qta.add(new BigDecimal(QtaE));
                PlusXm.Put_Giacenza(Qta.toPlainString());
                }
                
                
                
                //PARTE 2
                  if(!CostoCaricoE.isBlank()){
                  StackLIFO_InserisciValore_OLD(CryptoStack,MonetaE,QtaE,CostoCaricoE);
                  
                  if (v[18].contains("DTW")){
                      //Se è un DTW devo quindi anche scaricare il movimento PTW contrario
                      //se è attiva la funzione delle divisione delle plusvalenze per gruppo
                      //e se lo scambio avviene tra wallet di gruppi diversi
                      //Recupero quindi il tutto dalla funzione che c'è nel calcolo delle pluvalenze
                      String ris[]=Calcoli_Plusvalenze.RitornaIDeGruppoControparteSeGruppoDiverso(v);
                      String IDControparte=ris[0];
                      String WalletControparte=ris[1];
                      //IDControparte è null e i wallet di origine e destinazione sono uguali o se
                      //non è attiva la funzione per separare le plusvalenze per wallet
                      if (IDControparte!=null){
                          Map<String, ArrayDeque> CryptoStack2=MappaGrWallet_CryptoStack.get(WalletControparte);
                          String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IDControparte);
                          StackLIFO_TogliQta_OLD(CryptoStack2,Mov[8],Mov[10],true);
                      }
                      
                  }
               // Se non c'è costo di carico significa che sono trasferimenti non contabilizzati per cui li escludo dai calcoli
               //da controllare bene la cosa
                }
            }
            
            
            
            //PARTE B
            //ANALISI MOVIMENTI RILEVANTI
            
            
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
                    //adesso solo per le moete uscite salvo la pluvalenza per moneta
                    //DA FARE!!!!!
                }
                if (!v[15].isEmpty()) {
                    Vendite = Vendite.add(new BigDecimal(v[15]));
                    PlusAnno[2] = PlusAnno[2].add(new BigDecimal(v[15]));
                    //adesso solo per le moete uscite salvo il valore della vendita per moneta
                    //DA FARE!!!
                }
                if (!v[16].isEmpty()) {
                    CostiCarico = CostiCarico.add(new BigDecimal(v[16]));
                    PlusAnno[1] = PlusAnno[1].add(new BigDecimal(v[16]));
                    //adesso solo per le moete uscite salvo il costo di carico per moneta
                    //DA FARE!!!!!
                }
            }
            

                                            
        }
         ChiudiAnno_OLD(PlusvalenzeXAnno,Anno,MappaGrWallet_CryptoStack,MappaGrWallet_QtaCrypto,MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta);
      // return PlusvalenzeXAnno;
       ritorno.Put_PluvalenzeXAnno(PlusvalenzeXAnno);
       return ritorno;
    }
    
    public static AnalisiPlus CalcoliPlusvalenzeXAnno(){
        // DefaultTableModel ModelloTabellaRT = (DefaultTableModel) RT_Tabella_Principale.getModel();
      //  Funzioni_Tabelle_PulisciTabella(ModelloTabellaRT);
        AnalisiPlus ritorno=new AnalisiPlus();
        
        //ANNO,GRUPPOWALLET,MONETA,STACK della moneta
        Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta = new TreeMap<>();
        Map<String, Map<String, PlusXMoneta>> MappaGrWallet_MappaMoneta_PlusXMoneta= new TreeMap<>();
        Map<String,PlusXMoneta> MappaMoneta_PlusXMoneta;
        
        //controllo se devo o meno prendere in considerazione i gruppi wallet per il calcolo della plusvalenza
        boolean PlusXWallet=false;
        String PlusXW=DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if(PlusXW!=null && PlusXW.equalsIgnoreCase("SI")){
            PlusXWallet=true;
        }
        String Anno=null;
        Map<String,BigDecimal[]> PlusvalenzeXAnno = new TreeMap<>(); 
        //BigDecimal[] così composto
        //0 - Anno
        //1 - Tot. Costi di Carico
        //2 - Tot. Realizzato
        //3 - Tot. Plusvalenza
        //4 - Tot. Plusvalenza non realizzata/latente
        //5 - Valore di Fine Anno
        //6 - Errori
        //String = Anno
        BigDecimal Plusvalenza = new BigDecimal("0");
        BigDecimal CostiCarico = new BigDecimal("0");
        BigDecimal Vendite = new BigDecimal("0");
     //   setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        for (String[] v : MappaCryptoWallet.values()) {
            //if (Anno.isBlank())
            if (Anno!=null&&!Anno.equals(v[1].split("-")[0])){
                //Questa funzione va poi replicata a fine ciclo
                //Se sto facendo un cambio anno allora devo prendere l'intero stack e calcolare la pluvalenza latente per ogni crypto quindi fare le somme
                //inoltre mi copio lo stack che poi mi servirà in futuro
               
                //Questa mappa sarà quela che poi andrò a popolare e terrò da parte per analisi future
                //Per ora non la utilizzerò
                //ANNO,GRUPPOWALLET,MONETA,STACK della moneta
              //  Map<String, Map<String, Map<String, ArrayDeque>>> MappaAnno_MappaGrWallet_CryptoStack = new TreeMap<>();

                ChiudiAnno(PlusvalenzeXAnno,Anno,MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta);               
            }
            
            Anno=v[1].split("-")[0];
            //Identifico e creo una o più mappe per il cryptostack a seconda che sia o meno gestiti gli stack divisi per Wallet
            
            
            

            
            String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
                if(!PlusXWallet)GruppoWallet="Wallet 01";

            //A - INIZIALIZZA MAPPE PER ANNO

            if (MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno)==null){
                //Se non esiste la mappa per l'anno controllo se esiste già una mappa che arriva dagli anni passati
                //Se non esiste ne creo una nuova
                //Altrimenti duplico quella precedente e la aggiungo alla mappa
                //La mappa es. del 2022 deve infatti contenere tutti i dati degli anni passati + quelli del 2022 e così via per gli anni successivi
                if (MappaGrWallet_MappaMoneta_PlusXMoneta.isEmpty())
                   { 
                       MappaGrWallet_MappaMoneta_PlusXMoneta= new TreeMap<>();
                   }
                else{
                    //in questo caso devo clonare la mappa e tutti i suoi contenuti
                    //Questo perchè devo partire da dove ero arrivato ma con dei nuovi oggetti per il nuovo anno
                    MappaGrWallet_MappaMoneta_PlusXMoneta=new TreeMap<>(MappaGrWallet_MappaMoneta_PlusXMoneta);
                    for(String Wallet:MappaGrWallet_MappaMoneta_PlusXMoneta.keySet()){
                        MappaMoneta_PlusXMoneta=new TreeMap<>(MappaGrWallet_MappaMoneta_PlusXMoneta.get(Wallet));
                        MappaGrWallet_MappaMoneta_PlusXMoneta.put(Wallet, MappaMoneta_PlusXMoneta);
                        for(String Monetaa:MappaMoneta_PlusXMoneta.keySet()){
                            PlusXMoneta a = MappaMoneta_PlusXMoneta.get(Monetaa);
                            PlusXMoneta b = new PlusXMoneta();
                            b.Mon=a.Mon.ClonaMoneta();
                            b.CostoVendite="0.00";
                            b.ValVendita="0.00";
                            b.PlusLatente="0.00";
                            b.PlusRealizzata="0.00";
                            b.Mon.Prezzo="0.00";
                           // b.CompilaCampiDaMoneta(b.Mon);
                           //Adesso clono lo stack
                            if (a.Stack!=null){
                                b.Stack=a.Stack.clone();
                                }
                            MappaMoneta_PlusXMoneta.put(Monetaa, b);
                        }
                    }
                }
                MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.put(Anno, MappaGrWallet_MappaMoneta_PlusXMoneta);
                }
            else {
                //Altrimenti la recupero
                MappaGrWallet_MappaMoneta_PlusXMoneta=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno);
            }
            // Se adesso non esiste la mappa per wallet la creo
            if (MappaGrWallet_MappaMoneta_PlusXMoneta.get(GruppoWallet)==null){
               // CryptoStack2 = new TreeMap<>();
                MappaMoneta_PlusXMoneta= new TreeMap<>();
                MappaGrWallet_MappaMoneta_PlusXMoneta.put(GruppoWallet, MappaMoneta_PlusXMoneta);
                }
            else {
                //Altrimenti la recupero
                MappaMoneta_PlusXMoneta=MappaGrWallet_MappaMoneta_PlusXMoneta.get(GruppoWallet);
            }
            
   
            
            //B - Gestisco lo stack LiFo e aggiorno le quantità per tutti i movimenti
            //Dovrò escludere movimenti interni e spostamenti tra wallet (DA FARE!!!!)
            int Uscita=0;
            int Entrata=1;
            String IDTS[]=v[0].split("_");
            Moneta Monete[]=Moneta.RitornaMoneteDaMov(v);
            

            for (int i = 0; i < 2; i++) {
                //Se l moneata ha un tipo e quel tipo è divers da fiat vado avanti
                if (!Monete[i].Tipo.isBlank() && !Monete[i].Tipo.equalsIgnoreCase("FIAT")) {
                    //PARTE 1
                    //Faccio la somma delle qta e le scrivo nella mappa
                    //Questa parte va fatta senza filtri perchè per le quantità conto tutte le uscite e le entrate      
                    if (MappaMoneta_PlusXMoneta.get(Monete[i].Moneta) == null) {
                        //Se non ho ancora codificato la moneta nella mappa delle qta la inserisco
                        //e la inserisco nella mappa dei dettagli delle moneta
                        PlusXMoneta PlusXm = new PlusXMoneta();
                        PlusXm.CompilaCampiDaMoneta(Monete[i]);
                        PlusXm.Put_Anno(Anno);
                        PlusXm.Put_Wallet(GruppoWallet);
                        MappaMoneta_PlusXMoneta.put(Monete[i].Moneta, PlusXm);
                    } else {
                        //adesso faccio la somma della qta nuova sulla vecchia                
                        PlusXMoneta PlusXm = MappaMoneta_PlusXMoneta.get(Monete[i].Moneta);
                        BigDecimal Qta = PlusXm.Get_Giacenza();
                        Qta = Qta.add(new BigDecimal(Monete[i].Qta));
                        PlusXm.Put_Giacenza(Qta.toPlainString());

                    }
                    
                    
                    //PARTE 2
                    //SISTEMO GLI SCAMBI TRA WALLET
                    if (i==Uscita&&!Monete[Uscita].CostoCarico.isBlank()&&!v[18].contains("PTW")&&!IDTS[4].equalsIgnoreCase("TI")){
                        //I movimenti interni (TI) non voglio vengano considerati
                        //Tolgo il token dallo stack se non sono PTW, i PTW infatti vengono scaricati nel momento in cui arrivano a detinazione
                        //e solo se si è deciso di distinguere le plusvalenze per gruppo wallet altrimenti vengono considerati alla stregua di un trasferimento interno
                        //PTW sono prelievi che vanno ad altro wallet di proprietà
                        //in ogni caso non vanno trasferiti in questa sezione
                        StackLIFO_TogliQta(MappaMoneta_PlusXMoneta,Monete[Uscita].Moneta,Monete[Uscita].Qta,true);
                    }
                    
                    if (i == Entrata && !Monete[Entrata].CostoCarico.isBlank()) {
                        if (!IDTS[4].equalsIgnoreCase("TI")) {
                            if (!v[18].contains("DTW")) {
                                StackLIFO_InserisciValore(MappaMoneta_PlusXMoneta, Monete[Entrata].Moneta, Monete[Entrata].Qta, Monete[Entrata].CostoCarico);
                            } else {
                                //Se è un DTW devo quindi anche scaricare il movimento PTW contrario
                                //se è attiva la funzione delle divisione delle plusvalenze per gruppo
                                //e se lo scambio avviene tra wallet di gruppi diversi
                                //Recupero quindi il tutto dalla funzione che c'è nel calcolo delle pluvalenze
                                String ris[] = Calcoli_Plusvalenze.RitornaIDeGruppoControparteSeGruppoDiverso(v);
                                String IDControparte = ris[0];
                                String WalletControparte = ris[1];
                                //IDControparte è null se i wallet di origine e destinazione sono uguali o se
                                //non è attiva la funzione per separare le plusvalenze per wallet
                                if (IDControparte != null) {
                                    StackLIFO_InserisciValore(MappaMoneta_PlusXMoneta, Monete[Entrata].Moneta, Monete[Entrata].Qta, Monete[Entrata].CostoCarico);
                                    Map<String, PlusXMoneta> CryptoStack2 = MappaGrWallet_MappaMoneta_PlusXMoneta.get(WalletControparte);
                                    String Mov[] = CDC_Grafica.MappaCryptoWallet.get(IDControparte);
                                    StackLIFO_TogliQta(CryptoStack2, Mov[8], Mov[10], true);
                                }

                            }
                        }
                    }
                }
            }
            
            
            
            
            //PARTE D
            //ANALISI DEI SOLI MOVIMENTI RILEVANTI E INSERIMENTO DEI DATI IN TABELLA
            
            
            //Se inoltre è un movimento fiscalmente rilevante mi salvo costo di carico, valore transazione e plusvalenza divisa per anno
            //dobbiamo controllare i cashback come vengono gestiti.
            if (v[33].equals("S")) {
                //Per prima cosa verifico se l'anno è vuoto e in quel caso lo valorizzo
                BigDecimal PlusAnno[];
                if (PlusvalenzeXAnno.get(Anno)==null)
                {
                    PlusAnno=new BigDecimal[7];   
                    //System.out.println(Anno);
                    PlusAnno[0]=new BigDecimal(Anno);//Anno
                    PlusAnno[1]=new BigDecimal(0);//Costo Carico
                    PlusAnno[2]=new BigDecimal(0);//Realizzato
                    PlusAnno[3]=new BigDecimal(0);//Plusvalenza
                    PlusAnno[4]=new BigDecimal(0);//Plusvalenza Latente
                    PlusAnno[5]=new BigDecimal(0);//Valore Rimanenze
                    PlusAnno[6]=new BigDecimal(0);//Errori
                    PlusvalenzeXAnno.put(Anno, PlusAnno);
                    
                    
                }
                else
                {
                    PlusAnno=PlusvalenzeXAnno.get(Anno);
                }
                if (Funzioni_isNumeric(v[19], false)) {
                    Plusvalenza = Plusvalenza.add(new BigDecimal(v[19]));
                    PlusAnno[3] = PlusAnno[3].add(new BigDecimal(v[19]));
                    //La plusvalenza la realizzo o vendendo oppure se ho un earn
                    //Quindi adesso vedo se il movimento contiene solo un token in ingresso e ha relizzato pluvalenza
                    //vuol dire che devo mettere su quel token la plusvalenza
                    //in alternativa lo devo mettere sul token in uscita
                    BigDecimal PlusVal;
                    if (Monete[Uscita].Moneta.isBlank()){
                        PlusVal=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Get_PlusRealizzata().add(new BigDecimal(v[19]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Put_PlusRealizzata(PlusVal.toPlainString());
                    }
                    else{
                        PlusVal=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Get_PlusRealizzata().add(new BigDecimal(v[19]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Put_PlusRealizzata(PlusVal.toPlainString());
                    //adesso solo per le moete uscite salvo la pluvalenza per moneta
                    //DA FARE!!!!!
                    }
                }
                if (!v[15].isEmpty()) {
                    Vendite = Vendite.add(new BigDecimal(v[15]));
                    PlusAnno[2] = PlusAnno[2].add(new BigDecimal(v[15]));
                    //adesso solo per le moete uscite salvo il valore della vendita per moneta
                    //DA FARE!!!
                    //Il Valore Vendita lo realizzo o vendendo oppure se ho un earn
                    //Quindi adesso vedo se il movimento contiene solo un token in ingresso e ha relizzato pluvalenza
                    //vuol dire che devo mettere su quel token la plusvalenza
                    //in alternativa lo devo mettere sul token in uscita
                    BigDecimal ValVen;
                    if (Monete[Uscita].Moneta.isBlank()){
                        ValVen=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Get_ValVendite().add(new BigDecimal(v[15]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Put_ValVendite(ValVen.toPlainString());
                    }
                    else{
                        ValVen=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Get_ValVendite().add(new BigDecimal(v[15]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Put_ValVendite(ValVen.toPlainString());
                    }
                }
                if (!v[16].isEmpty()) {
                    CostiCarico = CostiCarico.add(new BigDecimal(v[16]));
                    PlusAnno[1] = PlusAnno[1].add(new BigDecimal(v[16]));
                    //adesso solo per le moete uscite salvo il costo di carico per moneta
                    //Il Valore Vendita lo realizzo o vendendo oppure se ho un earn
                    //Quindi adesso vedo se il movimento contiene solo un token in ingresso e ha relizzato pluvalenza
                    //vuol dire che devo mettere su quel token la plusvalenza
                    //in alternativa lo devo mettere sul token in uscita
                    BigDecimal ValCosto;
                    if (Monete[Uscita].Moneta.isBlank()){
                        ValCosto=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Get_ValCosto().add(new BigDecimal(v[16]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Entrata].Moneta)
                            .Put_ValCosto(ValCosto.toPlainString());
                    }
                    else{
                        ValCosto=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Get_ValCosto().add(new BigDecimal(v[16]));
                        MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta
                            .get(Anno)
                            .get(GruppoWallet)
                            .get(Monete[Uscita].Moneta)
                            .Put_ValCosto(ValCosto.toPlainString());
                    }
                }
            }
            

                                            
        }
         ChiudiAnno(PlusvalenzeXAnno,Anno,MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta);
      // return PlusvalenzeXAnno;
       ritorno.Put_PluvalenzeXAnno(PlusvalenzeXAnno);
       ritorno.Put_MappaCompleta(MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta);
       return ritorno;
    }
    
    
    public static void ChiudiAnno_OLD(Map<String,BigDecimal[]> PlusvalenzeXAnno,
            String Anno,
            Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack,
            Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto,
            Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta){
        
        
            //1 - Prendo ogni wallet
            //2 - Per ogni wallet estraggo tutte le monete che lo compongono
            //3 - Per ogni moneta trovo la plusvalenza sottraendo le rimanenze (QTA)
            //Recupero i dati dell'anno da analizzare
                BigDecimal PlusAnno[]=PlusvalenzeXAnno.get(Anno);
                //BigDecimal PluvalenzaLatente=PlusAnno[4];
                long d=Long.parseLong("1731667965000");
              //  long d=System.currentTimeMillis();//DA SISTEMARE CON DATA CORRETTA
                String DataAttualeAllOra=OperazioniSuDate.ConvertiDatadaLong(d)+" 00:00";
               String AnnoAttuale=DataAttualeAllOra.split("-")[0];
               d=OperazioniSuDate.ConvertiDatainLongMinuto(DataAttualeAllOra);
                if (!AnnoAttuale.equals(Anno)){
                    String Data=String.valueOf(Integer.parseInt(Anno)+1)+"-01-01 00:00";
                    d= OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                }
                
                for (String Wallet : MappaGrWallet_QtaCrypto.keySet()){
                    Map<String, Moneta> Mappa_Qta=MappaGrWallet_QtaCrypto.get(Wallet);
                    Map<String, ArrayDeque> Crypto_Stack=MappaGrWallet_CryptoStack.get(Wallet);
                    for (String Moneta : Mappa_Qta.keySet()){
                        Moneta mon=Mappa_Qta.get(Moneta);
                        BigDecimal qta = new BigDecimal(mon.Qta);
                        //Se la qyìta è minoreo uguale a zeno non faccio nulla
                        if (qta.compareTo(new BigDecimal(0)) > 0) {
                            //Recupero lo stack e lo passo alla Mappa con la suddivisione per anno
                            ArrayDeque stack = Crypto_Stack.get(Moneta);
                            MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Put_CryptoStack(stack);
                            

                            //questa funzione ritorna il valore al costo di carico della moneta appena levata dallo stack
                            BigDecimal CostoCarico = new BigDecimal(StackLIFO_TogliQta_OLD(Crypto_Stack, Moneta, qta.toPlainString(), false));

                            //Adesso devo trovare il Valore di Vendita della Moneta
                            BigDecimal PrezzoV = new BigDecimal(Prezzi.DammiPrezzoTransazione(mon, null, d, null, true, 15, mon.Rete));

                            //per trovare la plusvalenza devo quindi prima trovare il prezzo a fine anno e fare la sottrazione
                            BigDecimal PluvalenzaLatente = PrezzoV.subtract(CostoCarico);
                            MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Put_PlusLatente(PluvalenzaLatente.toPlainString());
                            PlusAnno[4] = PlusAnno[4].add(PluvalenzaLatente);
                        }
                    }
                }
    }
    
       public static void ChiudiAnno(Map<String,BigDecimal[]> PlusvalenzeXAnno,
            String Anno,
            Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta){
        
        
            //1 - Prendo ogni wallet
            //2 - Per ogni wallet estraggo tutte le monete che lo compongono
            //3 - Per ogni moneta trovo la plusvalenza sottraendo le rimanenze (QTA)
            //Recupero i dati dell'anno da analizzare
                BigDecimal PlusAnno[]=PlusvalenzeXAnno.get(Anno);
                //BigDecimal PluvalenzaLatente=PlusAnno[4];
                long d=Long.parseLong("1731667965000");
                //long d=System.currentTimeMillis();
                String DataAttualeAllOra=OperazioniSuDate.ConvertiDatadaLong(d)+" 00:00";
               String AnnoAttuale=DataAttualeAllOra.split("-")[0];
               d=OperazioniSuDate.ConvertiDatainLongMinuto(DataAttualeAllOra);
                if (!AnnoAttuale.equals(Anno)){
                    String Data=String.valueOf(Integer.parseInt(Anno)+1)+"-01-01 00:00";
                    //System.out.println(Data);
                    d= OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                }
                Map<String, Map<String, PlusXMoneta>> MappaGrWallet_MappaMoneta_PlusXMoneta=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno);
                for (String Wallet : MappaGrWallet_MappaMoneta_PlusXMoneta.keySet()){
                    Map<String, PlusXMoneta> MappaMoneta_PlusXMoneta=MappaGrWallet_MappaMoneta_PlusXMoneta.get(Wallet);
                    for (String Moneta : MappaMoneta_PlusXMoneta.keySet()){
                        
                        Moneta mon=MappaMoneta_PlusXMoneta.get(Moneta).Get_Moneta();
                        BigDecimal qta = new BigDecimal(mon.Qta);
                        //Se la qyìta è minoreo uguale a zeno non faccio nulla
                        if (qta.compareTo(new BigDecimal(0)) > 0) {

                            //questa funzione ritorna il valore al costo di carico della moneta appena levata dallo stack
                            BigDecimal CostoCarico = new BigDecimal(StackLIFO_TogliQta(MappaMoneta_PlusXMoneta, Moneta, qta.toPlainString(), false));
                            //MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Put_ValCosto(CostoCarico.toPlainString());
                           // MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Mon.Qta="100";
                            //Adesso devo trovare il Valore di Vendita della Moneta
                            //mon.MonetaAddress="";
                            BigDecimal PrezzoV = new BigDecimal(Prezzi.DammiPrezzoTransazione(mon, null, d, null, true, 2, mon.Rete));
                            MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Mon.Prezzo=PrezzoV.toPlainString();
                            //BigDecimal PrezzoV = new BigDecimal(Prezzi.DammiPrezzoTransazione(mon, null, d, null, true, 15, null));
                            PlusAnno[5] = PlusAnno[5].add(PrezzoV);
                            if (Anno.equals("2023")&&Moneta.equalsIgnoreCase("ETH")) {
                                System.out.println(mon.Moneta + " - " + Wallet+" - "+mon.Qta+" - "+Anno+" - "+PrezzoV);
                                System.out.println(mon.Rete + " - " + Wallet+" - "+mon.MonetaAddress+" - "+Anno+" - "+PrezzoV);
                                System.out.println(d);
                                }
                            //per trovare la plusvalenza devo quindi prima trovare il prezzo a fine anno e fare la sottrazione
                            BigDecimal PluvalenzaLatente = PrezzoV.subtract(CostoCarico);
                            MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno).get(Wallet).get(Moneta).Put_PlusLatente(PluvalenzaLatente.toPlainString());
                            PlusAnno[4] = PlusAnno[4].add(PluvalenzaLatente);
                            /*if (Moneta.equalsIgnoreCase("CRO")){
                            System.out.println(Wallet+" - "+qta+" - "+Anno+" - "+PluvalenzaLatente);
                        }*/
                        }
                    }
                }
    } 
    
     public static void StackLIFO_InserisciValore(Map<String, PlusXMoneta> MappaMoneta_PlusXMoneta, String Moneta,String Qta,String Valore) {
   // MappaMoneta_PlusXMoneta
           // Map<String, ArrayDeque> CryptoStack
    ArrayDeque<String[]> stack;
    String valori[]=new String[3];
    valori[0]=Moneta;
    valori[1]=new BigDecimal(Qta).abs().toPlainString();
    valori[2]=Valore;

        if(MappaMoneta_PlusXMoneta.get(Moneta).Get_CryptoStack()==null){
           stack = new ArrayDeque<>(); 
        }else{
            stack=MappaMoneta_PlusXMoneta.get(Moneta).Get_CryptoStack();
        }
        stack.push(valori);
        MappaMoneta_PlusXMoneta.get(Moneta).Put_CryptoStack(stack);
        //Adesso dovrei ributtare nella mappa il valore ma non credo che serva
        //Da testare

}  
     
       public static void StackLIFO_InserisciValore_OLD(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String Valore) {
    
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
     
  
      public static String StackLIFO_TogliQta(Map<String, PlusXMoneta> MappaMoneta_PlusXMoneta, String Moneta,String Qta,boolean toglidaStack) {
    
    //come ritorno ci invio il valore della movimentazione
    String ritorno="0.00";
    if (!Qta.isBlank()&&!Moneta.isBlank()){//non faccio nulla se la momenta o la qta non è valorizzata
    ArrayDeque<String[]> stack;

    BigDecimal qtaRimanente=new BigDecimal(Qta).abs();
    BigDecimal costoTransazione=new BigDecimal("0");
    //prima cosa individuo la moneta e prendo lo stack corrispondente
    if(MappaMoneta_PlusXMoneta.get(Moneta).Get_CryptoStack()==null){
        //ritorno="0";
    }else{
          //  System.out.println("OKokokokoko");
            
        if (toglidaStack)stack=MappaMoneta_PlusXMoneta.get(Moneta).Get_CryptoStack();
        else{
            ArrayDeque<String[]> stack2=MappaMoneta_PlusXMoneta.get(Moneta).Get_CryptoStack();
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
     
        public static String StackLIFO_TogliQta_OLD(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,boolean toglidaStack) {
    
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
     
     
 

      
      
      
  public static class AnalisiPlus {


  Map<String,BigDecimal[]> PlusvalenzeXAnno;
  Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta;
  
  
  public String ListaIDcoinvolti="";
  
  public void Put_PluvalenzeXAnno(Map<String,BigDecimal[]> PlusXAnno){
      PlusvalenzeXAnno=PlusXAnno;
  }
  
  public Map<String,BigDecimal[]>  Get_TabellaPlusXAnno(){
      return PlusvalenzeXAnno;
  }
  
  public void Put_MappaCompleta(Map<String, Map<String, Map<String, PlusXMoneta>>> MappaAnno_MappaGrWallet_MappaMoneta_PlusXMonetaA){
     MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMonetaA; 
  }
  
  public List<Object[]> RitornaTabellaAnno(String Anno){
      Map<String, Map<String, PlusXMoneta>> MappaGrWallet_MappaMoneta_PlusXMoneta=MappaAnno_MappaGrWallet_MappaMoneta_PlusXMoneta.get(Anno);
      List<Object[]> Tabella=new ArrayList<>();
      Object rigaTabella[];
      for(String Wallet : MappaGrWallet_MappaMoneta_PlusXMoneta.keySet()){
          Map<String, PlusXMoneta> MappaMoneta_PlusXMoneta=MappaGrWallet_MappaMoneta_PlusXMoneta.get(Wallet);
          for(String Moneta : MappaMoneta_PlusXMoneta.keySet()){ 
              PlusXMoneta plus=MappaMoneta_PlusXMoneta.get(Moneta);
              rigaTabella=new Object[9];
              rigaTabella[0]=Wallet;
              rigaTabella[1]=Moneta;
              rigaTabella[2]=plus.Mon.Tipo;
              rigaTabella[3]=Double.valueOf(plus.ValVendita);
              rigaTabella[4]=Double.valueOf(plus.CostoVendite);
              rigaTabella[5]=Double.valueOf(plus.PlusRealizzata);
              rigaTabella[6]=Double.valueOf(plus.PlusLatente);
              rigaTabella[7]=new BigDecimal(plus.Mon.Qta).stripTrailingZeros().toPlainString();
              rigaTabella[8]=plus.Mon.Prezzo;  
              Tabella.add(rigaTabella);
          }
      }
      return Tabella;
  }
  
 }
  
      public static class PlusXMoneta {

          String Anno;
          String Wallet;
          String ValVendita="0.00";
          String CostoVendite="0.00";
          String PlusRealizzata="0.00";
          String PlusLatente="0.00";
          String Errori;
          Moneta Mon;
          ArrayDeque<String[]> Stack;
          
          
          public void Put_CryptoStack(ArrayDeque<String[]> PMStack)
          {
            Stack=PMStack;
          }
          public ArrayDeque<String[]> Get_CryptoStack()
          {
            return Stack;
          }  
          public void Put_Anno(String PMAnno)
          {
            Anno=PMAnno;
          }  
          public void Put_Wallet(String PMWallet)
          {
            Wallet=PMWallet;
          } 
          public void Put_Moneta(String PMMoneta)
          {
            Mon.Moneta=PMMoneta;
          } 
          public void Put_Tipo(String PMTipo)
          {
            Mon.Tipo=PMTipo;
          } 
          public void Put_ValVendite(String PMValVendite)
          {
            ValVendita=PMValVendite;
          } 
          public BigDecimal Get_ValVendite()
          {
            return new BigDecimal(ValVendita);
          } 
          public void Put_ValCosto(String Put_ValCosto)
          {
            CostoVendite=Put_ValCosto;
          } 
          public BigDecimal Get_ValCosto()
          {
            return new BigDecimal(CostoVendite);
          } 
          public void Put_PlusRealizzata(String PMPlusRealizzata)
          {
            PlusRealizzata=PMPlusRealizzata;
          }
          public BigDecimal Get_PlusRealizzata()
          {
            return new BigDecimal(PlusRealizzata);
          } 
          public void Put_PlusLatente(String PMPlusLatente)
          {
            PlusLatente=PMPlusLatente;
          } 
          public void Put_Giacenza(String PMGiacenza)
          {
            Mon.Qta=PMGiacenza;
          } 
          public BigDecimal Get_Giacenza()
          {
            return new BigDecimal(Mon.Qta);
          } 
          public void Put_Errori(String PMErrori)
          {
            Errori=PMErrori;
          } 
          public void CompilaCampiDaMoneta(Moneta monet){
              Mon=monet;
          }
          public Moneta Get_Moneta(){
              return Mon;
          }
      }     
      
      
      
      
}
