/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;

/**
 *
 * @author Luca
 */

public class TransazioneDefi {

  public String Wallet;
  public String DataOra;
  public String HashTransazione;
  public String Blocco;
  public String Rete;
  public boolean TransazioneOK;
  public String TipoTransazione; //E' il Tipo Transazione segnata sulla blockchain ovvero il nome delle funzione sostanzialmente
  public String TipologiaTrans;//es. Scambio Crypto,Deposito Crypto, Prelievo Crypto, Acquisto NFT, Vendita NFT, Deposito NFT, Prelievo NFT,Scambio NFT
  public String MonetaCommissioni;
  public String QtaCommissioni;
  public String TimeStamp;
  private final Map<String, ValoriToken> MappaToken;
  private final Map<String, ValoriToken> MappaTokenUscita;
  private final Map<String, ValoriToken> MappaTokenEntrata;
  private final Map<String, ValoriToken> MappaTokenTecniciEntrata;
  private final Map<String, ValoriToken> MappaTokenTecniciUscita;

    public TransazioneDefi() {
        this.MappaToken = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.MappaTokenEntrata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.MappaTokenUscita = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.MappaTokenTecniciEntrata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.MappaTokenTecniciUscita = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.TransazioneOK = true;
    }
    
    
   public int RitornaNumeroTokenUscita() {
       return MappaTokenUscita.size();
   }
      public int RitornaNumeroTokenentrata() {
       return MappaTokenEntrata.size();
   }
   public Map<String,ValoriToken> RitornaMappaTokenEntrata(){
       return MappaTokenEntrata;
   }
   public Map<String,ValoriToken> RitornaMappaTokenUscita(){
       return MappaTokenUscita;
   }
    
    public void InserisciMonete(String Moneta,String MonetaName,String MonetaAddress,String AddressNoWallet,String Qta, String Tipologia){
        ValoriToken monete;
        //le monete in uscita avranno il meno davanti alla qta mentre quelle in ingresso no
//DA FARE!!!!!!Se i BNB arrivano dai movimenti interni sarebbe da verificarli prima di inserirli qua dentro
//DA FARE!!!!!!infatti da un rapido controllo sembrerebbe che mi risultino in giacenza più bnb di quelli reali
//DA FARE!!!!!!E' una cosa che posso gestire alla fine, una volta generato tutti gli algoritmi di calcolo
//conviene forse verificare la cosa già prima di cercare di buttare i token qua dentro
        String dataAlMinuto=DataOra.trim().substring(0, DataOra.length()-3);
        
        if (MappaToken.get(MonetaAddress)==null)
            {
                //se non esiste lo inserisco
            monete=new ValoriToken();
            monete.IndirizzoNoWallet=AddressNoWallet;
         /*   if (Moneta.trim().equalsIgnoreCase("cake-lp")){
                Moneta=Moneta+" ("+MonetaAddress+")";
                MonetaName=MonetaName+" ("+MonetaAddress+")";
            }*/
            monete.Tipo=Tipologia;
            monete.Moneta=Moneta;
            monete.MonetaAddress=MonetaAddress;
            monete.MonetaName=MonetaName;
            monete.Qta=Qta;
            MappaToken.put(MonetaAddress,monete);
            //System.out.println(dataAlMinuto+" - "+MonetaAddress);
            Moneta M1=new Moneta();
            M1.InserisciValori(Moneta,Qta,MonetaAddress,Tipologia);
            monete.Prezzo=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,30,Rete);
            //System.out.println("Import - "+Moneta+" - "+MonetaAddress+" - "+monete.Prezzo);
            //Se trovo l'indirizzo nella mappa significa che non è gestito da coingecko
            
           

           
           //Adesso verifico se il nome della coin è nelle coppie prioritarie ma non è gestita da coingecko la identifico come scam
           // long DataRiferimento=OperazioniSuDate.ConvertiDatainLong(DataOra.split(" ")[0])/1000;
          /*  if (DatabaseH2.AddressSenzaPrezzo_Leggi(MonetaAddress+"_"+Rete)!=null&&
                (DataRiferimento<Long.parseLong(DatabaseH2.AddressSenzaPrezzo_Leggi(MonetaAddress+"_"+Rete)))) {          
                for (String Coppia : CoppiePrioritarie) {
                    //Se non è gestito da coingecko ma è il nome di una coin importante significa che probabilmente è scam e quindi
                    //dopo il nome della moneta ci metto 2 asterischi
                    if ((monete.Moneta+"USDT").equalsIgnoreCase(Coppia)||monete.Moneta.equalsIgnoreCase("USDT"))
                        monete.Moneta=monete.Moneta+" **";
                }                
            }*/

            }
        else 
            {
                //altrimenti faccio la somma
            monete=MappaToken.get(MonetaAddress);
            monete.Qta=new BigDecimal(Qta).add(new BigDecimal(monete.Qta)).stripTrailingZeros().toPlainString();
            Moneta M1=new Moneta();
            M1.InserisciValori(Moneta,monete.Qta,MonetaAddress,Tipologia);
            monete.Prezzo=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,30,Rete);
            }
    }
    
     //FUNZIONE TUTTA DA VERIFICARE   
    public void InserisciMoneteCEX(Moneta Moneta,String Wallet,String CausaleOriginale,String IDT){
        ValoriToken monete;
        //le monete in uscita avranno il meno davanti alla qta mentre quelle in ingresso no
        //conviene forse verificare la cosa già prima di cercare di buttare i token qua dentro
        
        if (MappaToken.get(Moneta.Moneta)==null)
            {
            //se non esiste lo inserisco
            monete=new ValoriToken();
            monete.Tipo=Moneta.Tipo;
            monete.Moneta=Moneta.Moneta;
            monete.Qta=Moneta.Qta;            
            monete.Prezzo=Moneta.Prezzo;
            monete.WalletSecondario=Wallet;//poi sarà da vedere se esistono casi di monete identiche presi da wallet dioversi, in quel caso bisognerà differenziarli
            monete.CausaleOriginale=CausaleOriginale;
            monete.IDTransazione=IDT;
            MappaToken.put(Moneta.Moneta,monete);
            }
        else 
            {
            //altrimenti faccio la somma
            monete=MappaToken.get(Moneta.Moneta);
            monete.Qta=new BigDecimal(Moneta.Qta).add(new BigDecimal(monete.Qta)).stripTrailingZeros().toPlainString();
            monete.Prezzo=new BigDecimal(Moneta.Prezzo).add(new BigDecimal(monete.Prezzo)).stripTrailingZeros().toPlainString();
            }
    }
        

      public String IdentificaTipoTransazione(){
          String Tipo;
          boolean trovataEntrata=false;
          boolean trovataUscita=false;
          List<String> daEliminare=new ArrayList<>();
        //  System.out.println();
          for(ValoriToken token : MappaToken.values())
              {
               //   System.out.print(token.MonetaName+" _ "+token.Qta+" - ");
                //  if (new BigDecimal(token.Qta).compareTo(new BigDecimal("0"))==1)
                  if (!token.Qta.contains("-"))
                   {
                       
                       if (token.IndirizzoNoWallet!=null&&token.IndirizzoNoWallet.equalsIgnoreCase("0x0000000000000000000000000000000000000000")&&token.Moneta.toUpperCase().contains("DIVIDEND_TRACKER")){
                            //se trovo un movimento tecnico lo tolgo dalla lista generica e lo metto nelle movimentazioni specifiche tecniche
                            MappaTokenTecniciEntrata.put(token.MonetaAddress, token);
                            //System.out.println(token.MonetaAddress);
                           // MappaToken.remove(token.MonetaAddress);
                            daEliminare.add(token.MonetaAddress);
                       }
                       else{
                            MappaTokenEntrata.put(token.MonetaAddress, token);
                           // System.out.println(token.Moneta+" - "+token.Qta);
                            trovataEntrata=true;
                       }
                   }  
                  //if (new BigDecimal(token.Qta).compareTo(new BigDecimal("0"))==-1)
                  if (token.Qta.contains("-"))
                   {
                       
                       if (token.IndirizzoNoWallet.equalsIgnoreCase("0x0000000000000000000000000000000000000000")&&token.Moneta.toUpperCase().contains("DIVIDEND_TRACKER")){
                            //se trovo un movimento tecnico lo tolgo dalla lista generica e lo metto nelle movimentazioni specifiche tecniche
                            MappaTokenTecniciUscita.put(token.MonetaAddress, token);
                            daEliminare.add(token.MonetaAddress);
                            //System.out.println(token.MonetaAddress);
                           // MappaToken.remove(token.MonetaAddress);
                       }
                       else{
                            MappaTokenUscita.put(token.MonetaAddress, token);
                            trovataUscita=true;
                       }
                   } 
             //     System.out.println(token.Moneta+" - "+token.Qta);
              }
          if(trovataEntrata&&trovataUscita)Tipo="Scambio";
          else if(trovataEntrata&&!trovataUscita)Tipo="Deposito";
          else if(!trovataEntrata&&trovataUscita)Tipo="Prelievo";
          else Tipo="Commissioni";
       //   System.out.println(Tipo);
       //   System.out.println("-----------------------------------------");
       //Elimino ora dalla mappa le transazioni interne
       for (String add : daEliminare){
           MappaToken.remove(add);
       }
       
       
          return Tipo;
    }   
     
      public boolean isEmpty(){
          int i=0;
        for(ValoriToken token : MappaToken.values())
              {
                  i++;
              }
      return i==0;
      }
      
      
          public String IdentificaTipoTransazioneCEX(){
          String Tipo;
          boolean trovataEntrata=false;
          boolean trovataUscita=false;
          for(ValoriToken token : MappaToken.values())
              {
               //   System.out.print(token.MonetaName+" _ "+token.Qta+" - ");
                //  if (new BigDecimal(token.Qta).compareTo(new BigDecimal("0"))==1)
                 /// System.out.println(token.Moneta);
                  if (!token.Qta.contains("-"))
                   {
                     
                            MappaTokenEntrata.put(token.Moneta, token);
                            //System.out.println("Moneta Entrata : "+token.Moneta+" - "+token.Qta);
                            trovataEntrata=true;
                       
                   }  
                  //if (new BigDecimal(token.Qta).compareTo(new BigDecimal("0"))==-1)
                  if (token.Qta.contains("-"))
                   {
                     
                            MappaTokenUscita.put(token.Moneta, token);
                            //System.out.println("Moneta Uscita : "+token.Moneta+" - "+token.Qta);
                            trovataUscita=true;
                       
                   } 
             //     System.out.println(token.Moneta+" - "+token.Qta);
              }
          if(trovataEntrata&&trovataUscita)Tipo="Scambio";
          else if(trovataEntrata&&!trovataUscita)Tipo="Deposito";
          else if(!trovataEntrata&&trovataUscita)Tipo="Prelievo";
          else Tipo="Commissioni";
          //System.out.println(Tipo);
          return Tipo;
    }    
      
      
    public void AssegnaPesiaPartiTransazione() {

        //Prima di tutto scorro le 2 mappe e controllo se ce n'è almeno una di cui riesco a trovare il prezzo completo della transazione
        boolean trovatoValoreTransazione = true;
        BigDecimal ValoreTransazioneEntrata = new BigDecimal("0");
        BigDecimal ValoreTransazioneUscita = new BigDecimal("0");
        BigDecimal ValoreTransazione = new BigDecimal("0");
        for (ValoriToken a : MappaTokenEntrata.values()) {
           // System.out.println(a.Moneta);
            if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))==0) {
                //Se trovo un prezzo a zero valorizzo il booleano a false
                trovatoValoreTransazione = false;
                

             //   ValoreTransazioneEntrata = ValoreTransazioneEntrata.add(new BigDecimal(a.Prezzo));
            }
          //  System.out.println(a.Qta+" - "+a.Moneta+" - Euro "+a.Prezzo);
            ValoreTransazioneEntrata = ValoreTransazioneEntrata.add(new BigDecimal(a.Prezzo));
        }
        if (trovatoValoreTransazione)// se trovo il totale allora vuol dire che ho trovato il valore della transazione lo salvo
        {
            ValoreTransazione = ValoreTransazioneEntrata;

        } //se non trovo il valore della transazione allora lo cerco nei valori in uscita
        else {
            trovatoValoreTransazione = true;
            for (ValoriToken a : MappaTokenUscita.values()) {
                if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))==0) {
                    //Se trovo un prezzo a zero valorizzo il booleano a false
                    trovatoValoreTransazione = false;
            //        ValoreTransazioneUscita = ValoreTransazioneUscita.add(new BigDecimal(a.Prezzo));
                }
                ValoreTransazioneUscita = ValoreTransazioneUscita.add(new BigDecimal(a.Prezzo));
             //   System.out.println(a.Qta+" - "+a.Moneta+" - Euro "+a.Prezzo);
            }
            if (trovatoValoreTransazione) {
                ValoreTransazione = ValoreTransazioneUscita;
            }
        }
        //System.out.println("APPT "+MappaTokenUscita.size());
        //System.out.println("APPT "+MappaTokenEntrata.size());
        //Ora calcolo i pesi dei vari token
        //La funzione si divide in 2, se non trovo il valore transazione allora do lo stesso peso ai vari token
        if (!trovatoValoreTransazione)// se trovo il totale allora vuol dire che ho trovato il valore della transazione lo salvo
        {
            int numTokenRimenenti = MappaTokenEntrata.size();
            BigDecimal PesoRimanente = new BigDecimal(1);
            for (ValoriToken a : MappaTokenEntrata.values()) {
                numTokenRimenenti--;
                if (numTokenRimenenti==0){
                    a.Peso=PesoRimanente.toPlainString(); 
                }
                else{
                    a.Peso = new BigDecimal(1).divide(new BigDecimal(MappaTokenEntrata.size()), 10, RoundingMode.HALF_UP).toPlainString();
                }
                PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
            }
            numTokenRimenenti = MappaTokenUscita.size();
            PesoRimanente = new BigDecimal(1);
            for (ValoriToken a : MappaTokenUscita.values()) {
                numTokenRimenenti--;
                if (numTokenRimenenti==0){
                    a.Peso=PesoRimanente.toPlainString();
                }
                else{
                    a.Peso = new BigDecimal(1).divide(new BigDecimal(MappaTokenUscita.size()), 10, RoundingMode.HALF_UP).toPlainString();
                }
                PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
            }
        } else // se trovo il totale allora vuol dire che ho trovato il valore della transazione lo salvo e rifaccio i pesi in base ai valori
        //bisogna capire ora come sviluppare questo ciclo
        {
           // System.out.println(ValoreTransazione + " - " + HashTransazione);
            
            int numTokenRimenenti = MappaTokenEntrata.size();
            BigDecimal PesoRimanente = new BigDecimal(1);

            //primo ciclo do i pesi ai token con i prezzi
            for (ValoriToken a : MappaTokenEntrata.values()) {
                //Se ha prezzo riduco di 1 il numero dei token rimanenti e faccio il calcolo del peso
                //il peso lo calcolo dividendo il prezzo per il valore della transazione

                if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))!=0) {

                    numTokenRimenenti--;
                    if (numTokenRimenenti==0){
                        a.Peso=PesoRimanente.toPlainString();

                    }else   
                    {                 
                        a.Peso = new BigDecimal(a.Prezzo).divide(ValoreTransazione, 20, RoundingMode.HALF_UP).toPlainString();

                        //Se a.peso è maggiore di pesoRimanente allora a.Peso=Pesorimanente
                        if (new BigDecimal(a.Peso).compareTo(PesoRimanente)==1){
                            a.Peso=PesoRimanente.toPlainString();
                        }
                    }
                    
                    PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
                }
            }
            //secondo ciclo do i pesi ai token senza prezzo
            for (ValoriToken a : MappaTokenEntrata.values()) {
                // New BigDecimal pesi;
                if (a.Peso==null||new BigDecimal(a.Peso).compareTo(new BigDecimal("0"))==0) {//se non ha peso vuol dire che non l'ho ancora conteggiato
                    // a questo punto calcolo il peso dei token che è peso rimanente/nomTokenRimenti
                    if(numTokenRimenenti==0) System.out.println(a.Moneta);
                    else a.Peso = PesoRimanente.divide(new BigDecimal(numTokenRimenenti), 10, RoundingMode.HALF_UP).toPlainString();
                   // System.out.println("Siamo a zero"+a.Moneta+" - "+a.Peso);
                }
            }
            
            numTokenRimenenti = MappaTokenUscita.size();
            PesoRimanente = new BigDecimal(1);
            //primo ciclo do i pesi ai token con i prezzi
            for (ValoriToken a : MappaTokenUscita.values()) {
                //Se ha prezzo riduco di 1 il numero dei token rimanenti e faccio il calcolo del peso
                //il peso lo calcolo dividendo il prezzo per il valore della transazione
                if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))!=0) {
                    numTokenRimenenti--;
                    if (numTokenRimenenti==0){
                        a.Peso=PesoRimanente.toPlainString();
                       // System.out.println("Siamo a zero"+a.Moneta+" - "+a.Peso);
                    }
                    else   
                    {                 
                        a.Peso = new BigDecimal(a.Prezzo).divide(ValoreTransazione, 10, RoundingMode.HALF_UP).toPlainString();
                        if (new BigDecimal(a.Peso).compareTo(PesoRimanente)==1){
                            a.Peso=PesoRimanente.toPlainString();
                        }
                        //System.out.println(a.Moneta+" - "+a.Peso);
                    }
                    PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
               }
            }
            //secondo ciclo do i pesi ai token senza prezzo
            for (ValoriToken a : MappaTokenUscita.values()) {
                // New BigDecimal pesi;
                if (a.Peso==null||new BigDecimal(a.Peso).compareTo(new BigDecimal("0"))==0) {//se non ha peso vuol dire che non l'ho ancora conteggiato
                    // a questo punto calcolo il peso dei token che è peso rimanente/nomTokenRimenti
                    if(numTokenRimenenti==0) System.out.println(a.Moneta +"-"+ ValoreTransazione);
                    else a.Peso = PesoRimanente.divide(new BigDecimal(numTokenRimenenti), 10, RoundingMode.HALF_UP).toPlainString();
                  //  System.out.println("Siamo a zero"+a.Moneta+" - "+a.Peso);
                }
            }
        }
      /*  for (ValoriToken a : MappaTokenEntrata.values()) {
         //   System.out.println(a.Qta+" - "+a.Moneta+" - Euro "+a.Prezzo+" - Peso : "+a.Peso);
        }*/
    }

  
 public void IdentificaScam(List<String[]> Lista){
     
 }   
  //Parte nuova per la Defi
  // RT[23]=Blocco Transazione
  // RT[24]=Hash Transazione
  // RT[25]=Nome Token Uscita
  // RT[26]=Address Token Uscita
  // RT[27]=Nome Token Entrata
  // RT[28]=Address Token Entrata
  // RT[29]=Timestamp
  // RT[30]=Address Controparte
  public List<String[]> RitornaRigheTabella(){
      String RT[];
      List<String[]> righe=new ArrayList<>();
      String dataAlMinuto=DataOra.trim().substring(0, DataOra.length()-3);
      String PrimaParteID=DataOra.replaceAll(" |-|:", "")+"_BC."+Rete+"."+Wallet+"."+HashTransazione;
      if (TipoTransazione!=null) TipoTransazione=TipoTransazione.split("\\(")[0].trim();
      if(!TransazioneOK){
           //Transazione non andata a buon fine
           //Considero solo le commisioni
           RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_1_1_CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="COMMISSIONE";
              RT[6]="Per Operazione Fallita";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              Moneta M1=new Moneta();
           //   if(RT[8].equalsIgnoreCase("CRO")){
                M1.InserisciValori(RT[8],RT[10],RT[8],RT[9]);
             // }else M1.InserisciValori(RT[8],RT[10],null,RT[9]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]=MonetaCommissioni;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              //chiudo il cilco perchè questa è una transazione unica
              return righe;
       }
      else if (TipoTransazione!=null&&IdentificaTipoTransazione().equalsIgnoreCase("commissioni"))
          {
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_1_1_CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="COMMISSIONE";
              RT[6]="Per "+TipoTransazione;
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              Moneta M1=new Moneta();
           //   if(RT[8].equalsIgnoreCase("CRO")){
                M1.InserisciValori(RT[8],RT[10],RT[8],RT[9]);
            //  }else M1.InserisciValori(RT[8],RT[10],null,RT[9]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
             // System.out.println(M1.Moneta+" - "+M1.MonetaAddress+" - "+RT[15]);
             // RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]=MonetaCommissioni;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              //chiudo il ciclo perchè questa è una transazione unica
              return righe;
          }
       else if(IdentificaTipoTransazione()!=null && IdentificaTipoTransazione().equalsIgnoreCase("deposito")){
         //Deposito (No commissioni)
         //Potrebbero esserci commissioni se è un deposito da piattaforma defi
         //Quindi controllo se c'è la causale prima di applicare la commissione
         //Se non c'è la causale probabilmente arriva da exchange
         // System.out.println("---" + QtaCommissioni + "---");
          if (QtaCommissioni != null && TipoTransazione!=null && !TipoTransazione.isBlank()) {
              RT = new String[Importazioni.ColonneTabella];
              RT[0] = PrimaParteID + "_1_1_CM";
              RT[1] = dataAlMinuto;
              RT[2] = "1 di 1";
              RT[3] = Wallet + " (" + Rete + ")";
              RT[4] = "Wallet";
              RT[5] = "COMMISSIONE";
              RT[6] = "Per Deposito";
              RT[7] = TipoTransazione;
              RT[8] = MonetaCommissioni;
              RT[9] = "Crypto";
              RT[10] = QtaCommissioni;
              RT[11] = "";
              RT[12] = "";
              RT[13] = "";
              RT[14] = "";
              Moneta M1 = new Moneta();
              //             if(RT[8].equalsIgnoreCase("CRO")){
              M1.InserisciValori(RT[8], RT[10], RT[8], RT[9]);
              //   }else M1.InserisciValori(RT[8],RT[10],null,RT[9]);

              RT[15] = Prezzi.DammiPrezzoTransazione(M1, null, OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0", true, 2, Rete);//calcolare con numero contratto              
              //RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
              RT[16] = "";//Da definire cosa mettere
              RT[17] = "Da calcolare";
              RT[18] = "";
              RT[19] = "Da calcolare";
              RT[20] = "";
              RT[21] = "";
              RT[22] = "A";
              RT[23] = Blocco;
              RT[24] = HashTransazione;
              RT[25] = "";
              RT[26] = MonetaCommissioni;
              RT[27] = "";
              RT[28] = "";
              RT[29] = TimeStamp;
              RT[30] = "";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
          }
         
         
         int numeroDepositi=MappaToken.size();
         int i=1;
         for(ValoriToken token : MappaToken.values()){
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_"+Importazioni.RitornaTipologiaTransazione(null, token.Tipo,0);
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroDepositi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]=Importazioni.RitornaTipologiaTransazione(null, token.Tipo,1);
              RT[6]="-> "+token.RitornaNomeToken();
              RT[7]=TipoTransazione;
              RT[8]="";
              RT[9]="";
              RT[10]="";
              RT[11]=token.RitornaIDToken();
              RT[12]=token.Tipo;
              RT[13]=token.Qta;
              RT[14]="";
            //  Moneta M1=new Moneta();
             // M1.InserisciValori(RT[8],RT[10],null,RT[9]);
              Moneta M2=new Moneta();
              M2.InserisciValori(RT[11],RT[13],token.MonetaAddress,RT[12]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M2,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
             // RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,token.MonetaAddress,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]="";
              RT[27]=token.MonetaName;
              RT[28]=token.MonetaAddress;
              RT[29]=TimeStamp;
              RT[30]=token.IndirizzoNoWallet;
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
              }
             
      }else if(IdentificaTipoTransazione()!=null && IdentificaTipoTransazione().equalsIgnoreCase("prelievo")){
         //Prelievo (considero le commissioni) 
              
         RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_1_1_CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="COMMISSIONE";
              RT[6]="Per Prelievo";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              Moneta M1=new Moneta();
 //             if(RT[8].equalsIgnoreCase("CRO")){
                M1.InserisciValori(RT[8],RT[10],RT[8],RT[9]);
           //   }else M1.InserisciValori(RT[8],RT[10],null,RT[9]);

              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto              
              //RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]=MonetaCommissioni;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
            
              int numeroPrelievi=MappaToken.size();
              int i=1;
              for(ValoriToken token : MappaToken.values()){
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_"+Importazioni.RitornaTipologiaTransazione(token.Tipo, null,0);
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroPrelievi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]=Importazioni.RitornaTipologiaTransazione(token.Tipo, null,1);
              RT[6]=token.RitornaNomeToken()+" ->";
              RT[7]=TipoTransazione;
              RT[8]=token.RitornaIDToken();
              RT[9]=token.Tipo;
              RT[10]=token.Qta;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              M1=new Moneta();
              M1.InserisciValori(RT[8],RT[10],token.MonetaAddress,RT[9]);
             // Moneta M2=new Moneta();
             // M2.InserisciValori(RT[11],RT[13],null,RT[12]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto              
              //RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,token.MonetaAddress,null,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]=token.MonetaName;
              RT[26]=token.MonetaAddress;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]=token.IndirizzoNoWallet;
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
              }
              
      }else if(IdentificaTipoTransazione()!=null && IdentificaTipoTransazione().equalsIgnoreCase("scambio")){
          //prima di tutto genero il movimento di commissione
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_1_1_CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="COMMISSIONE";
              RT[6]="Per Scambio";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              Moneta M1=new Moneta();
         //     if(RT[8].equalsIgnoreCase("CRO")){
                M1.InserisciValori(RT[8],RT[10],RT[8],RT[9]);
          //    }else M1.InserisciValori(RT[8],RT[10],null,RT[9]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
             // RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]=MonetaCommissioni;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
          
            AssegnaPesiaPartiTransazione();  
          // in seconda istanza a seconda del numero di token che compongono la transazione creo i vari scambi 
          int i=1;
          int totMov=MappaTokenEntrata.size()*MappaTokenUscita.size();
       //   System.out.println("Numero Token in Entrata = "+MappaTokenEntrata.size()+" - Numero Token in uscita = "+MappaTokenUscita.size());
          for (ValoriToken tokenE : MappaTokenEntrata.values()) {
              for (ValoriToken tokenU : MappaTokenUscita.values()) {
                  //PESOOOOOOOOOOOOOOOOOOOOO
                  if (new BigDecimal(tokenU.Peso).compareTo(new BigDecimal(1))!=0||new BigDecimal(tokenE.Peso).compareTo(new BigDecimal(1))!=0){
                //  System.out.print(tokenU.Moneta+" - "+tokenU.Peso+" - "+tokenU.Qta+" _____ ");
                 // System.out.println(tokenE.Moneta+" - "+tokenE.Peso+" - "+tokenE.Qta);
                  }
                  //peso transazione                  
             /* BigDecimal PesoTransazione=new BigDecimal(tokenE.Peso).multiply(new BigDecimal(tokenU.Peso));
              if (MappaTokenEntrata.size()==1&&MappaTokenUscita.size()==1) PesoTransazione=new BigDecimal(1);*/
             // System.out.println(PesoTransazione + " - "+HashTransazione);
              String QuantitaEntrata=new BigDecimal(tokenE.Qta).multiply(new BigDecimal(tokenU.Peso)).stripTrailingZeros().toPlainString();
              String QuantitaUscita=new BigDecimal(tokenU.Qta).multiply(new BigDecimal(tokenE.Peso)).stripTrailingZeros().toPlainString();
              M1=new Moneta();
              M1.InserisciValori(tokenU.Moneta,QuantitaUscita,tokenU.MonetaAddress,tokenU.Tipo);
              Moneta M2=new Moneta();
              M2.InserisciValori(tokenE.Moneta,QuantitaEntrata,tokenE.MonetaAddress,tokenE.Tipo);
              BigDecimal PrezzoTransazione=new BigDecimal (Prezzi.DammiPrezzoTransazione(M1,M2,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete));
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_"+Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo,0);
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+totMov;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]=Importazioni.RitornaTipologiaTransazione(tokenU.Tipo, tokenE.Tipo,1);
              RT[6]=tokenU.RitornaNomeToken()+" -> "+tokenE.RitornaNomeToken();
              RT[7]=TipoTransazione;
              RT[8]=tokenU.RitornaIDToken();
              RT[9]=tokenU.Tipo;
              RT[10]=QuantitaUscita;
              RT[11]=tokenE.RitornaIDToken();
              RT[12]=tokenE.Tipo;
              RT[13]=QuantitaEntrata;
              RT[14]="";
              RT[15]=PrezzoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]=tokenU.MonetaName;
              RT[26]=tokenU.MonetaAddress;
              RT[27]=tokenE.MonetaName;
              RT[28]=tokenE.MonetaAddress;
              RT[29]=TimeStamp;
              RT[30]=tokenU.IndirizzoNoWallet;
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
              }
          }
          //scambio crypto crypto
 

              
      }else{
          System.out.println("Transazione non contemplata "+HashTransazione);
                      JOptionPane.showConfirmDialog(null, "Transazione non contemplata "+HashTransazione,
                    "Transazione non contemplata",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null);
          
      }
      
      //transazioni interne di deposito
      int numeroDepositi=MappaTokenTecniciEntrata.size();
      int i=1;
      //mi occupo ora di inserire le transazioni tecniche
      for(ValoriToken token : MappaTokenTecniciEntrata.values()){
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_TI";
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroDepositi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="TRANSAZIONE TECNICA INTERNA";
              RT[6]="-> "+token.MonetaName;
              RT[7]=TipoTransazione;
              RT[8]="";
              RT[9]="";
              RT[10]="";
              RT[11]=token.Moneta;
              RT[12]=token.Tipo;
              RT[13]=token.Qta;
              RT[14]="";
           //   Moneta M1=new Moneta();
            //  M1.InserisciValori(RT[8],RT[10],null,RT[9]);
              Moneta M2=new Moneta();
              M2.InserisciValori(RT[11],RT[13],token.MonetaAddress,RT[12]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M2,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="0.00";
              RT[18]="";
              RT[19]="0.00";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]="";
              RT[26]="";
              RT[27]=token.MonetaName;
              RT[28]=token.MonetaAddress;
              RT[29]=TimeStamp;
              RT[30]=token.IndirizzoNoWallet;
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
      }
      
      //Transazioni interne di prelievo
      int numeroPrelievi=MappaTokenTecniciUscita.size();
      i=1;     
      for(ValoriToken token : MappaTokenTecniciUscita.values()){
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_TI";
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroDepositi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]="Wallet";
              RT[5]="TRANSAZIONE TECNICA INTERNA";
              RT[6]=token.MonetaName+" ->";
              RT[7]=TipoTransazione;
              RT[8]=token.Moneta;
              RT[9]=token.Tipo;
              RT[10]=token.Qta;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              Moneta M1=new Moneta();
              M1.InserisciValori(RT[8],RT[10],token.MonetaAddress,RT[9]);
             // Moneta M2=new Moneta();
            //  M2.InserisciValori(RT[11],RT[13],token.MonetaAddress,RT[12]);
              RT[15]=Prezzi.DammiPrezzoTransazione(M1,null,OperazioniSuDate.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,Rete);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="0.00";
              RT[18]="";
              RT[19]="0.00";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              RT[23]=Blocco;
              RT[24]=HashTransazione;
              RT[25]=token.MonetaName;
              RT[26]=token.MonetaAddress;
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]=token.IndirizzoNoWallet;
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
      }
      
      
      return righe;
  }

  
  
 public class ValoriToken {

  public String WalletSecondario;//utilizzato solo per i cex per identificare da dove movimentano i token
  public String CausaleOriginale;//utilizzato solo per i cex per identificare da dove movimentano i token
  public String Moneta;
  public String Qta;
  public String MonetaAddress;
  public String MonetaName;
  public String IndirizzoNoWallet;
  public String Prezzo;
  public String Peso;
  public String Tipo; //NFT, FIAT o CRYPTO
  public String IDTransazione;
  
  public String RitornaNomeToken(){
      String nome;
     // System.out.println("aa-"+Moneta+"-aa");
      if (MonetaName==null || MonetaName.trim().equalsIgnoreCase("")){
          nome=Moneta;
      }
      else if (Moneta.trim().length()>2&&Moneta.trim().substring(Moneta.trim().length()-3, Moneta.trim().length()).equals("-LP")){
      //    else if (Moneta.trim().contains("-LP")){
                nome=MonetaName+" ("+MonetaAddress+")";
            }
      else nome=MonetaName;
      //.out.println(Moneta.trim().substring(Moneta.trim().length()-3, Moneta.trim().length())+" ("+MonetaAddress+")");
      //System.out.println(nome);
      return nome;
  }
  
    public String RitornaIDToken(){
      String nome;
      if (Tipo.equalsIgnoreCase("NFT")){
          nome=Moneta+" ("+MonetaAddress+")";
      }
      else if (Moneta.trim().length()>2&&Moneta.trim().substring(Moneta.trim().length()-3, Moneta.trim().length()).equals("-LP")){
                nome=Moneta+" ("+MonetaAddress+")";
            }
      else nome=Moneta;
      return nome;
  }
}
 
}


