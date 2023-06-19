/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luca
 */

public class TransazioneDefi {

  public String Wallet;
  public String DataOra;
  public String HashTransazione; 
  public String Rete;
  public boolean TransazioneOK;
  public String TipoTransazione;
  public String MonetaCommissioni;
  public String QtaCommissioni;
  public String MonetaEntrata;
  public String MonetaEntrataAddress;
  public String MonetaEntrataName;
  public String QtaEntrata;
  public String MonetaUscita;
  public String MonetaUscitaAddress;
  public String MonetaUscitaName;
  public String QtaUscita;

    public TransazioneDefi() {
        this.TransazioneOK = true;
    }
 
  public List<String[]> RitornaRigheTabella(){
      String RT[]=new String[23];
      List<String[]> righe=new ArrayList<>();
      String dataAlMinuto=DataOra.trim().substring(0, DataOra.length()-3);
      String PrimaParteID=DataOra.replaceAll(" |-|:", "")+"_BC."+Rete+"."+Wallet+"."+HashTransazione+"_1_1_";
      if (TipoTransazione!=null&&TipoTransazione.contains("approve"))
          {
              RT[0]=PrimaParteID+"CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="COMMISSIONE";
              RT[6]="COMMISSIONE PER APPROVAZIONE CONTRATTO";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              return righe;
          }
       else if(!TransazioneOK){
           //Transazione non andata a buon fine
           //Considero solo le commisioni
              RT[0]=PrimaParteID+"CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="COMMISSIONE";
              RT[6]="COMMISSIONE SU OPERAZIONE FALLITA";
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              return righe;
       }
      else if(MonetaEntrata!=null && MonetaUscita==null){
         //Deposito (No commissioni)
              RT[0]=PrimaParteID+"DC";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="DEPOSITO CRYPTO";
              RT[6]="DEPOSITO "+MonetaEntrata;
              RT[7]=TipoTransazione;
              RT[8]="";
              RT[9]="";
              RT[10]="";
              RT[11]=MonetaEntrata;
              RT[12]="Crypto";
              RT[13]=QtaEntrata;
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              return righe;
      }else if(MonetaEntrata==null && MonetaUscita!=null){
         //Prelievo (considero le commissioni) 
              RT[0]=PrimaParteID+"PC";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="PRELIEVO CRYPTO";
              RT[6]="PRELIEVO "+MonetaUscita;
              RT[7]=TipoTransazione;
              RT[8]=MonetaUscita;
              RT[9]="Crypto";
              RT[10]=QtaUscita;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              
              RT=new String[23];
              RT[0]=PrimaParteID+"CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="COMMISSIONE";
              RT[6]="COMMISSIONE SU PRELIEVO "+MonetaUscita;
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              return righe;
      }else if(MonetaEntrata!=null && MonetaUscita!=null){
          //scambio crypto crypto
       //   System.out.println(MonetaUscita+" - "+MonetaEntrata);
              RT[0]=PrimaParteID+"SC";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="SCAMBIO CRYPTO";
              RT[6]="SCAMBIO CRYPTO "+MonetaUscita+" -> "+MonetaEntrata;
              RT[7]=TipoTransazione;
              RT[8]=MonetaUscita;
              RT[9]="Crypto";
              RT[10]=QtaUscita;
              RT[11]=MonetaEntrata;
              RT[12]="Crypto";
              RT[13]=QtaEntrata;
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              
              RT=new String[23];
              RT[0]=PrimaParteID+"CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="COMMISSIONE";
              RT[6]="COMMISSIONE SCAMBIO "+MonetaUscita+" -> "+MonetaEntrata;
              RT[7]=TipoTransazione;
              RT[8]=MonetaCommissioni;
              RT[9]="Crypto";
              RT[10]=QtaCommissioni;
              RT[11]="";
              RT[12]="";
              RT[13]="";
              RT[14]="";
              RT[15]=Calcoli.DammiPrezzoTransazione(RT[8],RT[11],RT[10],RT[13],Calcoli.ConvertiDatainLongMinuto(dataAlMinuto), "0",true);//calcolare con numero contratto
              RT[16]="";//Da definire cosa mettere
              RT[17]="Da calcolare";
              RT[18]="";
              RT[19]="Da calcolare";
              RT[20]="";
              RT[21]="";
              RT[22]="A";
              righe.add(RT);
              return righe;
      }else{
          System.out.println("Transazione non contemplata "+HashTransazione);
          return null;
      }
  }

}
