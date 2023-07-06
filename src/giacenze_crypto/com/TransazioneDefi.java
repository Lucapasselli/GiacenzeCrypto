/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.Calcoli.CoppiePrioritarie;
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
  public String TipoTransazione;
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
    
    public void InserisciMonete(String Moneta,String MonetaName,String MonetaAddress,String AddressNoWallet,String Qta){
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
            if (Moneta.trim().equalsIgnoreCase("cake-lp")){
                Moneta=Moneta+" ("+MonetaAddress+")";
                MonetaName=MonetaName+" ("+MonetaAddress+")";
            }
            monete.Moneta=Moneta;
            monete.MonetaAddress=MonetaAddress;
            monete.MonetaName=MonetaName;
            monete.Qta=Qta;
            MappaToken.put(MonetaAddress,monete);
            //System.out.println(dataAlMinuto+" - "+MonetaAddress);
            monete.Prezzo=Calcoli.DammiPrezzoTransazione(Moneta,null,Qta,null,Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,6,monete.MonetaAddress,null,Rete);
            if (Calcoli.MappaConversioneAddressCoin.get(MonetaAddress+"_"+Rete)!=null) {          
                for (String Coppia : CoppiePrioritarie) {
                    if ((monete.Moneta+"USDT").equalsIgnoreCase(Coppia)||monete.Moneta.equalsIgnoreCase("USDT"))
                        monete.Moneta=monete.Moneta+" **";
                }                
            }

            }
        else 
            {
                //altrimenti faccio la somma
            monete=MappaToken.get(MonetaAddress);
            monete.Qta=new BigDecimal(Qta).add(new BigDecimal(monete.Qta)).stripTrailingZeros().toPlainString();
            //System.out.println(dataAlMinuto+" - "+Moneta);
            monete.Prezzo=Calcoli.DammiPrezzoTransazione(Moneta,null,monete.Qta,null,Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,6,monete.MonetaAddress,null,Rete);
            }
    }
    

      public String IdentificaTipoTransazione(){
          String Tipo=null;
          boolean trovataEntrata=false;
          boolean trovataUscita=false;
          List<String> daEliminare=new ArrayList<>();
          for(ValoriToken token : MappaToken.values())
              {
                //  if (new BigDecimal(token.Qta).compareTo(new BigDecimal("0"))==1)
                  if (!token.Qta.contains("-"))
                   {
                       
                       if (token.IndirizzoNoWallet.equalsIgnoreCase("0x0000000000000000000000000000000000000000")&&token.Moneta.toUpperCase().contains("DIVIDEND_TRACKER")){
                            //se trovo un movimento tecnico lo tolgo dalla lista generica e lo metto nelle movimentazioni specifiche tecniche
                            MappaTokenTecniciEntrata.put(token.MonetaAddress, token);
                           // MappaToken.remove(token.MonetaAddress);
                            daEliminare.add(token.MonetaAddress);
                       }
                       else{
                            MappaTokenEntrata.put(token.MonetaAddress, token);
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
          if(trovataEntrata&&!trovataUscita)Tipo="Deposito";
          if(!trovataEntrata&&trovataUscita)Tipo="Prelievo";
       //   System.out.println(Tipo);
       //   System.out.println("-----------------------------------------");
       //Elimino ora dalla mappa le transazioni interne
       for (String add : daEliminare){
           MappaToken.remove(add);
       }
       
       
          return Tipo;
    }   
     
      
      
      
    public void AssegnaPesiaPartiTransazione() {

        //Prima di tutto scorro le 2 mappe e controllo se ce n'è almeno una di cui riesco a trovare il prezzo completo della transazione
        boolean trovatoValoreTransazione = true;
        BigDecimal ValoreTransazioneEntrata = new BigDecimal("0");
        BigDecimal ValoreTransazioneUscita = new BigDecimal("0");
        BigDecimal ValoreTransazione = new BigDecimal("0");
        for (ValoriToken a : MappaTokenEntrata.values()) {
            if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))==0) {
                //Se trovo un prezzo a zero valorizzo il booleano a false
                trovatoValoreTransazione = false;
                // numEntrateSenzaPrezzo++;
                ValoreTransazioneEntrata = ValoreTransazioneEntrata.add(new BigDecimal(a.Prezzo));
            }
            ValoreTransazioneEntrata = ValoreTransazioneEntrata.add(new BigDecimal(a.Prezzo));
        }
        if (trovatoValoreTransazione)// se trovo il totale allora vuol dire che ho trovato il valore della transazione lo salvo
        {
            ValoreTransazione = ValoreTransazioneEntrata;

        } //se non trovo il valore della transazione allora lo cerco nei valori in entrata
        else {
            for (ValoriToken a : MappaTokenUscita.values()) {
                if (new BigDecimal(a.Prezzo).compareTo(new BigDecimal("0"))==0) {
                    //Se trovo un prezzo a zero valorizzo il booleano a false
                    trovatoValoreTransazione = false;
                    // numUsciteSenzaPrezzo++;
                    ValoreTransazioneUscita = ValoreTransazioneUscita.add(new BigDecimal(a.Prezzo));
                }
                ValoreTransazioneUscita = ValoreTransazioneUscita.add(new BigDecimal(a.Prezzo));
            }
            if (trovatoValoreTransazione) {
                ValoreTransazione = ValoreTransazioneUscita;
            }
        }

        //Ora calcolo i pesi dei vari token
        //La funzione si divide in 2, se non trovo il valore transazione allora do lo stesso peso ai vari token
        if (!trovatoValoreTransazione)// se trovo il totale allora vuol dire che ho trovato il valore della transazione lo salvo
        {
            for (ValoriToken a : MappaTokenEntrata.values()) {
                a.Peso = new BigDecimal(1).divide(new BigDecimal(MappaTokenEntrata.size()), 10, RoundingMode.HALF_UP).toPlainString();
            }
            for (ValoriToken a : MappaTokenUscita.values()) {
                a.Peso = new BigDecimal(1).divide(new BigDecimal(MappaTokenUscita.size()), 10, RoundingMode.HALF_UP).toPlainString();
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
                    a.Peso = new BigDecimal(a.Prezzo).divide(ValoreTransazione, 15, RoundingMode.HALF_UP).toPlainString();
                    PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
                }
            }
            //secondo ciclo do i pesi ai token senza prezzo
            for (ValoriToken a : MappaTokenEntrata.values()) {
                // New BigDecimal pesi;
                if (a.Peso==null||new BigDecimal(a.Peso).compareTo(new BigDecimal("0"))==0) {//se non ha peso vuol dire che non l'ho ancora conteggiato
                    // a questo punto calcolo il peso dei token che è peso rimanente/nomTokenRimenti
                    a.Peso = PesoRimanente.divide(new BigDecimal(numTokenRimenenti), 15, RoundingMode.HALF_UP).toPlainString();
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
                    a.Peso = new BigDecimal(a.Prezzo).divide(ValoreTransazione, 15, RoundingMode.HALF_UP).toPlainString();
                    PesoRimanente = PesoRimanente.subtract(new BigDecimal(a.Peso));
                }
            }
            //secondo ciclo do i pesi ai token senza prezzo
            for (ValoriToken a : MappaTokenUscita.values()) {
                // New BigDecimal pesi;
                if (a.Peso==null||new BigDecimal(a.Peso).compareTo(new BigDecimal("0"))==0) {//se non ha peso vuol dire che non l'ho ancora conteggiato
                    // a questo punto calcolo il peso dei token che è peso rimanente/nomTokenRimenti
                    a.Peso = PesoRimanente.divide(new BigDecimal(numTokenRimenenti), 15, RoundingMode.HALF_UP).toPlainString();
                }
            }
        }
    }

  
 public void IdentificaScam(List<String[]> Lista){
     
 }   
    
     
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
              RT[4]=Rete+" Transaction";
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
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
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
              RT[27]="";
              RT[28]="";
              RT[29]=TimeStamp;
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              //chiudo il cilco perchè questa è una transazione unica
              return righe;
       }
      else if (TipoTransazione!=null&&TipoTransazione.contains("approve"))
          {
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_1_1_CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="COMMISSIONE";
              RT[6]="Per Approvazione Contratto";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
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
         int numeroDepositi=MappaToken.size();
         int i=1;
         for(ValoriToken token : MappaToken.values()){
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_DC";
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroDepositi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="DEPOSITO CRYPTO";
              RT[6]="-> "+token.MonetaName;
              RT[7]=TipoTransazione;
              RT[8]="";
              RT[9]="";
              RT[10]="";
              RT[11]=token.Moneta;
              RT[12]="Crypto";
              RT[13]=token.Qta;
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,token.MonetaAddress,Rete);//calcolare con numero contratto
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
              RT[30]="";
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
              RT[4]=Rete+" Transaction";
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
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
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
              RT[0]=PrimaParteID+"_"+i+"_1_PC";
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+numeroPrelievi;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="PRELIEVO CRYPTO";
              RT[6]=token.MonetaName+" ->";
              RT[7]=TipoTransazione;
              RT[8]=token.Moneta;
              RT[9]="Crypto";
              RT[10]=token.Qta;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,token.MonetaAddress,null,Rete);//calcolare con numero contratto
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
              RT[30]="";
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
              RT[4]=Rete+" Transaction";
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
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,null,Rete);//calcolare con numero contratto
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
          for (ValoriToken tokenE : MappaTokenEntrata.values()) {
              for (ValoriToken tokenU : MappaTokenUscita.values()) {
                  //peso transazione                  
              BigDecimal PesoTransazione=new BigDecimal(tokenE.Peso).multiply(new BigDecimal(tokenU.Peso));
              if (MappaTokenEntrata.size()==1&&MappaTokenUscita.size()==1) PesoTransazione=new BigDecimal(1);
             // System.out.println(PesoTransazione + " - "+HashTransazione);
              BigDecimal PrezzoTransazione=new BigDecimal (Calcoli.DammiPrezzoTransazione(tokenU.Moneta,tokenE.Moneta,tokenU.Qta,tokenE.Qta,Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,6,tokenU.MonetaAddress,tokenE.MonetaAddress,Rete))
                .multiply(PesoTransazione);
              RT=new String[Importazioni.ColonneTabella];
              RT[0]=PrimaParteID+"_"+i+"_1_SC";
              RT[1]=dataAlMinuto;
              RT[2]=i+" di "+totMov;
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="SCAMBIO CRYPTO";
              RT[6]=tokenU.MonetaName+" -> "+tokenE.MonetaName;
              RT[7]=TipoTransazione;
              RT[8]=tokenU.Moneta;
              RT[9]="Crypto";
              RT[10]=new BigDecimal(tokenU.Qta).multiply(PesoTransazione).stripTrailingZeros().toPlainString();
              RT[11]=tokenE.Moneta;
              RT[12]="Crypto";
              RT[13]=new BigDecimal(tokenE.Qta).multiply(PesoTransazione).stripTrailingZeros().toPlainString();
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
              RT[30]="";
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
              RT[4]=Rete+" Transaction";
              RT[5]="TRANSAZIONE TECNICA INTERNA";
              RT[6]="-> "+token.MonetaName;
              RT[7]=TipoTransazione;
              RT[8]="";
              RT[9]="";
              RT[10]="";
              RT[11]=token.Moneta;
              RT[12]="Crypto";
              RT[13]=token.Qta;
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,null,token.MonetaAddress,Rete);//calcolare con numero contratto
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
              RT[30]="";
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
              RT[4]=Rete+" Transaction";
              RT[5]="TRANSAZIONE TECNICA INTERNA";
              RT[6]=token.MonetaName+" ->";
              RT[7]=TipoTransazione;
              RT[8]=token.Moneta;
              RT[9]="Crypto";
              RT[10]=token.Qta;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true,2,token.MonetaAddress,null,Rete);//calcolare con numero contratto
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
              RT[30]="";
              Importazioni.RiempiVuotiArray(RT);
              righe.add(RT);
              i++;
      }
      
      
      return righe;
  }

  
  
 private class ValoriToken {

  public String Moneta;
  public String Qta;
  public String MonetaAddress;
  public String MonetaName;
  public String IndirizzoNoWallet;
  public String Prezzo;
  public String Peso;
  
}
 
}


