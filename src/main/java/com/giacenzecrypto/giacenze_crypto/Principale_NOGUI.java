/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Importazioni.ColonneTabella;
import static com.giacenzecrypto.giacenze_crypto.Importazioni.RiempiVuotiArray;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author lucap
 */
public class Principale_NOGUI {
    
    /*
    

    
    -SCAMBIO CRYPTO-CRYPTO -> SC -> SCAMBIO CRYPTO
    -DUST-CONVERSION -> SC -> SCAMBIO CRYPTO
    -SCAMBIO CRYPTO -> SC -> SCAMBIO CRYPTO
    
    -STAKING REWARDS -> RW -> STAKING REWARD
    -STAKING REWARD -> RW -> STAKING REWARD
    -EARN -> RW -> EARN
    -CASHBACK -> RW -> CASHBACK
    -REWARD -> RW -> REWARD
    -ALTRE-REWARD -> RW -> REWARD
    -AIRDROP -> RW -> AIRDROP  
        
    -ACQUISTO CRYPTO -> AC -> ACQUISTO CRYPTO
    -VENDITA CRYPTO -> VC -> VENDITA CRYPTO
    
    -TRASFERIMENTO-CRYPTO-INTERNO -> TI -> TRASFERIMENTO INTERNO
    -TRASFERIMENTO INTERNO -> TI -> TRASFERIMENTO INTERNO
    
    TRASFERIMENTO-CRYPTO -> DC o PC a seconda se sono movimenti in uscita o entrata (Quinfi DEPOSITO CRYPTO o PRELIEVO CRYPTO)
    
    NON CONSIDERARE
    IGNORA
    
    DEPOSITO FIAT -> DF -> DEPOSITO FIAT
    PRELIEVO FIAT -> PF -> PRELIEVO FIAT
    
    -COMMISSIONI -> CM -> COMMISSIONI

    */

    //Questa mappa contiene gli abbinamenti che servono per assegnare correttamente tipo e nome della transazioni
static final Map<String, String[]> TipologieMovimento = Collections.unmodifiableMap(creaMappaTipologie());

private static Map<String, String[]> creaMappaTipologie() {
    Map<String, String[]> m = new TreeMap<>();
   // m.put("DEPOSITO FIAT",        new String[]{"DF", "DEPOSITO FIAT",""});//Li faccio decidere al programma
   // m.put("PRELIEVO FIAT",        new String[]{"PF", "PRELIEVO FIAT",""});//Li faccio decidere al programma
    
    m.put("COMMISSIONI",        new String[]{"CM", "COMMISSIONI",""});
    m.put("COMMISSIONE",        new String[]{"CM", "COMMISSIONI",""});
    
    m.put("AIRDROP",        new String[]{"RW", "AIRDROP",""});
    m.put("ALTRE-REWARD",   new String[]{"RW", "REWARD",""});
    m.put("AIRDREWARDROP",  new String[]{"RW", "REWARD",""});
    m.put("CASHBACK",       new String[]{"RW", "CASHBACK",""});
    m.put("EARN",           new String[]{"RW", "EARN",""});
    m.put("STAKING REWARD",   new String[]{"RW", "STAKING REWARD",""});
    m.put("STAKING REWARDS",  new String[]{"RW", "STAKING REWARD",""});
    
   // m.put("SCAMBIO CRYPTO",       new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
   // m.put("SCAMBIO CRYPTO-CRYPTO",           new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
   // m.put("DUST-CONVERSION",           new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
    
   // m.put("ACQUISTO CRYPTO",           new String[]{"AC", "ACQUISTO CRYPTO",""});//Li faccio decidere al programma
   // m.put("VENDITA CRYPTO",           new String[]{"VC", "VENDITA CRYPTO",""});//Li faccio decidere al programma
    
    m.put("TRASFERIMENTO INTERNO",           new String[]{"TI", "TASFERIMENTO INTERNO",""});
    m.put("TRASFERIMENTO-CRYPTO-INTERNO",           new String[]{"TI", "TRASFERIMENTO INTERNO",""});
    
    m.put("CASHOUT O SIMILARE",           new String[]{"PC", "CASHOUT O SIMILARE","PCO - CASHOUT O SIMILARE"});
    m.put("RETTIFICA GIACENZA",           new String[]{"PC", "RETTIFICA GIACENZA","PWN - RETTIFICA GIACENZA"});
    
    m.put("DEPOSITO A COSTO 0",           new String[]{"DC", "DEPOSITO A COSTO 0","DCZ - DEPOSITO A COSTO 0"});   

    return m;
}

    
    public static String[] creaMovimento(
            Moneta MonetaOUT, Moneta MonetaIN,
            String Wallet, String Wallet2,
            long Timestamp,
            String Prezzo, String FontePrezzo,
            int numMovimento, int numMovimento2,
            String IDMovimento,
            String Nota,
            String CarattereMovAutomatico, //Valorizzare ad A o a M
            String IDTransHash,
            String TipoTr
    ) {
        //========== SISTEMO I NOMI IN MODO CHE SIANO SAFE ===========
        Wallet=normalizzaNome(Wallet);
        Wallet2=normalizzaNome(Wallet2);
        Nota=normalizzaNome(Nota);
        IDTransHash=normalizzaNome(IDTransHash);
        
        //========== GESTISCO LE DATE ===========
        if (IDMovimento != null) {
            //Se mi forniscono l'id del movimento il timestamp lo prendo da li per coerenza
            String d = IDMovimento.split("_")[0];
            Timestamp = FunzioniDate.ConvertiDataIDinLong(d);
        }
        String data = FunzioniDate.ConvertiDatadaLongAlSecondo(Timestamp);
        String DataID = data.replaceAll(" |-|:", "");
        String dataa = data.trim().substring(0, data.length() - 3);

        //========== GESTISCO LE MONETE SCAMBIATE ===========   
        Moneta Mon[] = new Moneta[]{MonetaOUT, MonetaIN};
        String MOut = "";
        String TOut = "";
        String QOut = "";
        String AddressOut = "";
        String MIn = "";
        String TIn = "";
        String QIn = "";
        String AddressIn = "";
        String Rete = "";
        String RT2[] = new String[ColonneTabella];

        for (Moneta MON : Mon) {
            if (MON != null) {
                if (MON.Qta.contains("-")) {
                    if (!MOut.isBlank()){
                        LoggerGC.ScriviErrore("Movimento incoerente, ci sono due monete in uscita : "+MOut+" e "+MON.Moneta);
                        return null;
                    }
                    MOut = normalizzaMoneta(MON.Moneta);
                    TOut = MON.Tipo;
                    QOut = MON.Qta;
                    Rete = MON.Rete;
                    AddressOut = MON.MonetaAddress;
                } else {
                    if (!MOut.isBlank()){
                        LoggerGC.ScriviErrore("Movimento incoerente, ci sono due monete in ingresso : "+MOut+" e "+MON.Moneta);
                        return null;
                    }
                    MIn = normalizzaMoneta(MON.Moneta);
                    TIn = MON.Tipo;
                    QIn = MON.Qta;
                    Rete = MON.Rete;
                    AddressIn = MON.MonetaAddress;
                }
            }
        }

        //========== GESTISCO IL CARATTERE CHE IDENTIFICA SE IL MOVIMENTO E' DERIVANTE DA CSV/DEFI O MANUALE ===========
        if (CarattereMovAutomatico==null && 
                (!Arrays.asList("M", "A", "AU").contains(CarattereMovAutomatico))
                )
        {
            CarattereMovAutomatico="M";
        }
        
        //Se IDTrans è null la valorizzo a blanc
        if (IDTransHash == null) {
            IDTransHash = "";
        }

        //========== GESTISCO IL PREZZO DELLA TRANSAZIONE ===========
        if (Prezzo == null || !Funzioni.isNumeric(Prezzo, false)) {
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(Mon[0], Mon[1], Timestamp, Rete, Wallet);
            if (IP != null) {
                Prezzo = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
                RT2[40] = IP.Ritorna40();
                RT2[32] = "SI";
            }
            else{
                Prezzo="0.00";
                RT2[32] = "NO";
            }
        } else {
            Prezzo=Prezzo.replace("-","");//Questo impedisce che il prezzo sia negativo
            if (FontePrezzo == null) {
                FontePrezzo = "Personalizzato";
            }
            RT2[40] = "|||" + FontePrezzo;
            RT2[32] = "SI";
        }

        //========== GESTISCO IL TIPO DELLA TRANSAZIONE ===========  
        if (TipoTr == null)TipoTr="";
        //Tipologie[] ->   0-codiceTipo(nell'id),  1-Descrizione Tipo(posizione5),  2-Sottotipo(posizione18)
        String Tipologie[]=TipologieMovimento.get(TipoTr);
        if (Tipologie == null) {
                Tipologie=new String[3];
                Tipologie[0] = Importazioni.RitornaTipologiaTransazione(TOut, TIn, 0);
                Tipologie[1] = Importazioni.RitornaTipologiaTransazione(TOut, TIn, 1);
                Tipologie[2] = "";
            }
        else{
            //Nel caso in cui recupero correttamente il movimento se è un movimento RW di uscita devo inserire rimborso davanti alla descrizione
            if (Tipologie[0].equals("RW")&&MIn.isBlank()){
                Tipologie[1]="RIMBORSO "+Tipologie[1];
            }
        }
        
        
        //========== GESTISCO L'ID ELLA TRANSAZIONE =========== 
        
        RT2[0] = DataID + "_" + Wallet + "_" + numMovimento + "_" + numMovimento2 + "_" + Tipologie[0];
        if (IDMovimento != null) {
            //Nel caso in cui prendo l'id passato recupero però sempre il codice tipologia reale
            String IDMovimentoS[]=IDMovimento.split("_");
            if (IDMovimentoS.length==5){
                RT2[0] = IDMovimentoS[0]+"_"+IDMovimentoS[1]+"_"+IDMovimentoS[2]+"_"+IDMovimentoS[3]+"_"+Tipologie[0];
            }
        }
        //Adesso controllo se è un movimento Interno se così fosse devo fare in modo che i prelievi siano sempre prima dei depositi
        //Questo perchè sono 2 movimenti contemporanei tolgo da una parte e metto dall'altra.
        //Per far questo sui depositi inserisco una A dopo il nome del Wallet. quindi dopo trunk 1
        if (Tipologie[0].equals("TI")&&!MIn.isBlank()){
            String IDMovimentoS[]=RT2[0].split("_");
            RT2[0] = IDMovimentoS[0]+"_"+IDMovimentoS[1]+"A_"+IDMovimentoS[2]+"_"+IDMovimentoS[3]+"_"+IDMovimentoS[4];
        }
        

        //========== CREO IL MOVIMENTO ===========
        RT2[1] = dataa;
        RT2[3] = Wallet;
        RT2[4] = Wallet2;
        RT2[5] = Tipologie[1];
        RT2[6] = (MOut + " -> " + MIn).trim();
        RT2[8] = MOut;
        RT2[9] = TOut;
        RT2[10] = QOut;
        RT2[11] = MIn;
        RT2[12] = TIn;
        RT2[13] = QIn;
        RT2[15] = Prezzo;
        RT2[18] = Tipologie[2];
        RT2[21] = Nota;
        RT2[22] = CarattereMovAutomatico;
        RT2[24] = IDTransHash;
        RT2[26] = AddressOut;
        RT2[28] = AddressIn;
        RT2[29] = String.valueOf(Timestamp);
        RiempiVuotiArray(RT2);
        return RT2;
    }
    
    
    
static String normalizzaNome(String nome) {
    if (nome == null) return "";
    return nome.trim()
               .replaceAll("[;,_]", "")
               .replaceAll("\\s+", " ")  // Opzionale: normalizza spazi multipli
               ;
}
static String normalizzaMoneta(String nome) {
    if (nome == null) return "";
    return nome.trim()
               .replaceAll("[;,]", "")
               .replaceAll("\\s+", " ")  // Opzionale: normalizza spazi multipli
               ;
}    
    
}
