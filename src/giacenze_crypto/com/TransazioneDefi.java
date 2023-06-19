/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

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
 
  public void RitornaRigheTabella(){
      String RT[]=new String[23];
      String dataAlMinuto=DataOra.trim().substring(0, DataOra.length()-3);
      String PrimaParteID=DataOra.replaceAll(" |-|:", "")+"_BC."+Rete+"."+Wallet+"."+HashTransazione+"_1_1_";
      if (TipoTransazione.contains("approve"))
          {
              RT[0]=PrimaParteID+"CM";
              RT[1]=dataAlMinuto;
              RT[2]="1 di 1";
              RT[3]=Wallet+" ("+Rete+")";
              RT[4]=Rete+" Transaction";
              RT[5]="Approvazione Contratto "+Rete;
              RT[6]="Commissioni x Approvazione Contratto";
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
          }
      else{
          
      }
  }

}
