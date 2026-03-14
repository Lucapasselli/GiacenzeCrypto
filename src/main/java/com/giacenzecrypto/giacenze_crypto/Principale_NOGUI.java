/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Importazioni.ColonneTabella;
import static com.giacenzecrypto.giacenze_crypto.Importazioni.RiempiVuotiArray;
import java.math.RoundingMode;

/**
 *
 * @author lucap
 */
public class Principale_NOGUI {
        

public static String[] creaMovimento(
        Moneta MonetaOUT, Moneta MonetaIN,
        String Wallet, String Wallet2,
        long Timestamp,
        String Prezzo,String FontePrezzo,
        int numMovimento,int numMovimento2,
        String IDMovimento,
        String Nota,
        String CarattereMovAutomatico,   //Valorizzare ad A o a M
        String IDTransHash,
        String CodTipoTr,String TipoTr
){
                        //========== GESTISCO LE DATE ===========
                                if (IDMovimento!=null){
                                    //Se mi forniscono l'id del movimento il timestamp lo prendo da li per coerenza
                                    String d=IDMovimento.split("_")[0];
                                    Timestamp=FunzioniDate.ConvertiDataIDinLong(d);
                                }
                                String data=FunzioniDate.ConvertiDatadaLongAlSecondo(Timestamp);
                                String DataID=data.replaceAll(" |-|:", "");
                                String dataa=data.trim().substring(0, data.length()-3);
                                
                                
                        //========== GESTISCO LE MONETE SCAMBIATE ===========        
                                String MOut="";
                                String TOut="";
                                String QOut="";
                                String AddressOut="";
                                String MIn="";
                                String TIn="";
                                String QIn="";
                                String AddressIn="";
                                String Rete="";
                                String RT2[] = new String[ColonneTabella];
                                
                                
                                if (MonetaOUT!=null)
                                {
                                    if(MonetaOUT.Qta.contains("-")){
                                        MOut=MonetaOUT.Moneta;
                                        TOut=MonetaOUT.Tipo;
                                        QOut=MonetaOUT.Qta;                                    
                                        Rete=MonetaOUT.Rete;
                                        AddressOut=MonetaOUT.MonetaAddress;
                                    }
                                    else{
                                        MIn=MonetaOUT.Moneta;
                                        TIn=MonetaOUT.Tipo;
                                        QIn=MonetaOUT.Qta;                                    
                                        Rete=MonetaOUT.Rete;
                                        AddressIn=MonetaOUT.MonetaAddress;                                           
                                            }
                                }
                                if (MonetaIN!=null)
                                {
                                    if(!MonetaIN.Qta.contains("-")){
                                        MIn=MonetaIN.Moneta;
                                        TIn=MonetaIN.Tipo;
                                        QIn=MonetaIN.Qta;
                                        Rete=MonetaIN.Rete;
                                        AddressIn=MonetaIN.MonetaAddress;
                                    }
                                    else{
                                        MOut=MonetaIN.Moneta;
                                        TOut=MonetaIN.Tipo;
                                        QOut=MonetaIN.Qta;
                                        Rete=MonetaIN.Rete;
                                        AddressOut=MonetaIN.MonetaAddress;                                    
                                    }
                                }
                                
                                //Se IDTrans è null la valorizzo a blanc
                                if (IDTransHash==null)IDTransHash="";
                                
                         //========== GESTISCO IL PREZZO DELLA TRANSAZIONE ===========
                                
                                if (Prezzo==null||!Funzioni.isNumeric(Prezzo, false))
                                {
                                    Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(MonetaOUT, MonetaIN, Timestamp, Rete, Wallet);
                                    if (IP!=null){
                                        Prezzo=IP.prezzoQta.setScale(2,RoundingMode.HALF_UP).toPlainString();
                                        RT2[40] = IP.Ritorna40();
                                    }
                                }else{
                                    if (FontePrezzo==null)FontePrezzo="Personalizzato";
                                    RT2[40] = "|||"+FontePrezzo;
                                }
                                
                         //========== GESTISCO L'ID ELLA TRANSAZIONE ===========       
                                if (IDMovimento==null){
                                    if (CodTipoTr==null)CodTipoTr=Importazioni.RitornaTipologiaTransazione(TOut,TIn,0);
                                    RT2[0] = DataID+"_"+Wallet+"_"+numMovimento+"_"+numMovimento2+"_"+CodTipoTr;
                                }
                                else RT2[0] = IDMovimento;
                                
                           
                         //========== CREO IL MOVIMENTO ===========
                                if (TipoTr==null)TipoTr=Importazioni.RitornaTipologiaTransazione(TOut,TIn,1);
                                RT2[1] = dataa;
                                RT2[3] = Wallet;
                                RT2[4] = Wallet2;
                                RT2[5] = TipoTr;
                                RT2[6] = (MOut+" -> "+MIn).trim();
                                RT2[8] = MOut;
                                RT2[9] = TOut;
                                RT2[10] = QOut;
                                RT2[11] = MIn;
                                RT2[12] = TIn;
                                RT2[13] = QIn;
                                RT2[15] = Prezzo;
                                RT2[21] = Nota;
                                RT2[22] = "M";
                                RT2[24] = IDTransHash;
                                RT2[26] = AddressOut;
                                RT2[28] = AddressIn;
                                RT2[29] = String.valueOf(Timestamp);
                                RiempiVuotiArray(RT2);
                                return RT2;
}
    
}
