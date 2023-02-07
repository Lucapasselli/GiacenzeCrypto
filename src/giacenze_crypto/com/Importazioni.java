/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import static giacenze_crypto.com.CDC_Grafica.ConvertiDatainLong;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca.passelli
 */
public class Importazioni {
    
    public static void Importa_Crypto_CDCApp() {

        Map<String, String> Mappa_Conversione_Causali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //Faccio una lista di causali per la conversione dei dati del csv
        Mappa_Conversione_Causali.put("card_cashback_reverted", "CASHBACK");              //Cashback ripristinato
        Mappa_Conversione_Causali.put("referral_card_cashback", "CASHBACK");              //Cashback della Carta MCO
        Mappa_Conversione_Causali.put("crypto_earn_interest_paid", "EARN");               //Interessi maturati da una Crypto in Earn
        Mappa_Conversione_Causali.put("crypto_earn_program_created", "TRASFERIMENTO-CRYPTO-INTERNO-EARN");//Inserimento di una Crypto in Earn
        Mappa_Conversione_Causali.put("crypto_earn_program_withdrawn", "TRASFERIMENTO-CRYPTO-INTERNO-EARN");//Prelievo di una Crypto dall'Earn
        Mappa_Conversione_Causali.put("crypto_exchange", "SCAMBIO-CRYPTO-CRYPTO");        //Scambio di una Crypto per un'altra Crypto
        Mappa_Conversione_Causali.put("crypto_deposit", "TRASFERIMENTO-CRYPTO");          //Deposito di Crypto provenienti da wallet esterno
        Mappa_Conversione_Causali.put("crypto_payment", "PAGAMENTO-CRYPTO");              //Pagamento in Crypto (Es.: Crypto Pay in CRO)
        Mappa_Conversione_Causali.put("crypto_purchase", "ACQUISTO CRYPTO");          //Acquisto di Crypto da Carta di Credito
        Mappa_Conversione_Causali.put("crypto_to_exchange_transfer", "TRASFERIMENTO-CRYPTO");//Trasferimento di una Crypto dall'App verso l'Exchange
        Mappa_Conversione_Causali.put("crypto_viban_exchange", "VENDITA CRYPTO");    //Vendita di una Crypto verso il portafoglio EUR
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_credited", fileDaImportare);    //Scambio MCO in CRO (MCO liberi nel portafoglio). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("crypto_wallet_swap_debited", fileDaImportare);     //Scambio MCO in CRO (MCO liberi nel portafoglio). Vendita degli MCO
        Mappa_Conversione_Causali.put("crypto_withdrawal", "TRASFERIMENTO-CRYPTO");       //Prelievo di una Crypto verso portafogli esterni
        Mappa_Conversione_Causali.put("dust_conversion_credited", "SCAMBIO-CRYPTO-CRYPTO");//Conversione di Crypto in CRO. CRO Ricevuti dalla conversione.
        Mappa_Conversione_Causali.put("dust_conversion_debited", "SCAMBIO-CRYPTO-CRYPTO");//Conversione di Crypto in CRO. Crypto da convertire in CRO.
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_bonus_exchange_deposit", fileDaImportare);//Bonus Swap MCO/CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_credited", fileDaImportare);     //Scambio MCO in CRO (MCO in Earn). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("dynamic_coin_swap_debited", fileDaImportare);      //Scambio MCO in CRO (MCO in Earn). Vendita degli MCO
        Mappa_Conversione_Causali.put("exchange_to_crypto_transfer", "TRASFERIMENTO-CRYPTO-STAKE");    //Trasferimenti dall'Exchange verso l'App
        Mappa_Conversione_Causali.put("lockup_lock", "TRASFERIMENTO-CRYPTO-INTERNO");          //CRO Stake per la MCO Card. Nuovo Stake
//        Mappa_Conversione_Causali.put("lockup_swap_credited", fileDaImportare);         //Scambio MCO in CRO (MCO in Stake per la Carta). Acquisto dei CRO
//        Mappa_Conversione_Causali.put("lockup_swap_debited", fileDaImportare);          //Scambio MCO in CRO (MCO in Stake per la Carta). Vendita degli MCO
        Mappa_Conversione_Causali.put("lockup_upgrade", "TRASFERIMENTO-CRYPTO-INTERNO-STAKE");       //CRO Stake per la MCO Card. (Upgrade)
        Mappa_Conversione_Causali.put("mco_stake_reward", "STAKING");                       //Interessi che la MCO Card matura. Da (Jade in su)
        Mappa_Conversione_Causali.put("pay_checkout_reward", "REWARD");                   //Ricompesa di Crypto.com Pay
        Mappa_Conversione_Causali.put("referral_gift", "REWARD");                         //Bonus di iscrizione sbloccato
        Mappa_Conversione_Causali.put("reimbursement", "REWARD");                         //Rimborsi (Es. Netflix, Promozioni)
        Mappa_Conversione_Causali.put("reimbursement_reverted", "REWARD");                //Annullamento di un rimborso (o parte)
        Mappa_Conversione_Causali.put("supercharger_deposit", "TRASFERIMENTO-CRYPTO-INTERNO-SUPERCHARGER"); //Deposito dei CRO nel supercharger
        Mappa_Conversione_Causali.put("supercharger_withdrawal", "TRASFERIMENTO-CRYPTO-INTERNO-SUPERCHARGER");//Prelievo dei CRO dal supercharger
        Mappa_Conversione_Causali.put("viban_purchase", "ACQUISTO CRYPTO");           //Acquisto di Crypto dal portafoglio EUR 
//        Mappa_Conversione_Causali.put("nft_payout_credited", fileDaImportare);            //Vendita NFT 
        Mappa_Conversione_Causali.put("staking_reward", "STAKING");                       //Reward (Es. NEO Gas) 
        Mappa_Conversione_Causali.put("campaign_reward", "REWARD");                       //Vincita di una campagna (Es.: Telegram Madness
        Mappa_Conversione_Causali.put("crypto_payment_refund", "REWARD");                 //Rimborso in Crypto. (Es. Rimborso Offerta per un NFT)
        Mappa_Conversione_Causali.put("referral_bonus", "REWARD");                        //Bonus Referral 
        Mappa_Conversione_Causali.put("crypto_earn_extra_interest_paid", "REWARD");//Earn Extra Reward 
        Mappa_Conversione_Causali.put("supercharger_reward_to_app_credited", "REWARD");//Supercharger Reward in App
        Mappa_Conversione_Causali.put("rewards_platform_deposit_credited", "REWARD");//Mission Reward
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_fund_lock", "TRASFERIMENTO-CRYPTO-INTERNO-BLOCCO");//Blocca i fondi destinati ad un'ordine Limit 
        Mappa_Conversione_Causali.put("trading_limit_order_crypto_wallet_exchange", "SCAMBIO-CRYPTO-CRYPTO");//Ordine Limite Eseguito         
//        Mappa_Conversione_Causali.put("crypto_credit_withdrawal_created", fileDaImportare);//Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_repayment_created", fileDaImportare);//Crypto Loan        
//        Mappa_Conversione_Causali.put("crypto_credit_loan_credited", fileDaImportare);    //Crypto Loan
//        Mappa_Conversione_Causali.put("crypto_credit_program_created", fileDaImportare);  //Crypto Loan 
        Mappa_Conversione_Causali.put("admin_wallet_credited", "ALTRE-REWARD");//es. aggiustamenti luna  

        String fileDaImportare = "testCDCApp.csv";
        //come prima cosa leggo il file csv e lo ordino in maniera corretta (dal più recente)
        //se ci sono movimenti con la stessa ora devo mantenere l'ordine inverso del file.
        //ad esempio questo succede per i dust conversion etc....
        Map<String, String[]> Mappa_Movimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String riga;
        String ultimaData = "";
        List<String> listaMovimentidaConsolidare = new ArrayList<>();
        try ( FileReader fire = new FileReader(fileDaImportare);  BufferedReader bure = new BufferedReader(fire);) {
            while ((riga = bure.readLine()) != null) {
                String splittata[] = riga.split(",");
                if (ConvertiDatainLong(splittata[0]) != 0)// se la riga riporta una data valida allora proseguo con l'importazione
                {
                    //se trovo movimento con stessa data e ora lo aggiungo alla lista che compone il movimento e vado avanti
                    if (splittata[0].equalsIgnoreCase(ultimaData)) {
                        listaMovimentidaConsolidare.add(riga);
                    } else //altrimenti consolido il movimento precedente
                    {
                       // System.out.println(riga);
                        List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
                        int nElementi = listaConsolidata.size();
                        for (int i = 0; i < nElementi; i++) {
                            String consolidata[] = listaConsolidata.get(i);
                            Mappa_Movimenti.put(consolidata[0] + consolidata[6].split(" di ")[0].trim(), consolidata);
                        }

                        //una volta fatto tutto svuoto la lista movimenti e la preparo per il prossimo
                        listaMovimentidaConsolidare = new ArrayList<>();
                        listaMovimentidaConsolidare.add(riga);
                    }
                    ultimaData = splittata[0];

                }

            }
            List<String[]> listaConsolidata = ConsolidaMovimenti_CDCAPP(listaMovimentidaConsolidare, Mappa_Conversione_Causali);
            int nElementi = listaConsolidata.size();
            for (int i = 0; i < nElementi; i++) {
                String consolidata[] = listaConsolidata.get(i);
                Mappa_Movimenti.put(consolidata[0] + consolidata[6].split(" di ")[0].trim(), consolidata);
            }

            bure.close();
            fire.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }     

        
        Scrivi_Movimenti_Crypto(Mappa_Movimenti);
        
    }
    
    
        private static void Scrivi_Movimenti_Crypto(Map<String, String[]> Mappa_Movimenti) {
         try { 
            FileWriter w=new FileWriter("movimenti.crypto.csv");
            BufferedWriter b=new BufferedWriter (w);
       for (String[] v : Mappa_Movimenti.values()) {
          String riga=v[0]+";"+v[1]+";"+v[2]+";"+v[3]+";"+v[4]+";"+v[5]+";"+v[6]+";"+v[7]+";"+v[8]+";"+v[9]+";"+v[10]+";"+v[11]+";"+v[12]+";"+v[13]+";"+v[14]+";"+v[15]+";"+v[16]+";"+v[17]+";"+v[18]+";"+v[19]+";"+v[20]+";"+v[21]+";"+v[22];
         //  System.out.println(riga);
           b.write(riga+"\n");
          // System.out.println(value);
       }
       b.close();
       w.close();
    }catch (IOException ex) {
                 //  Logger.getLogger(AWS.class.getName()).log(Level.SEVERE, null, ex);
               }
    }
    
    
     public static List<String[]> ConsolidaMovimenti_CDCAPP(List<String> listaMovimentidaConsolidare,Map<String, String> Mappa_Conversione_Causali){
         List<String[]> lista=new ArrayList<>();
         int numMovimenti=listaMovimentidaConsolidare.size();
                        for (int k=0;k<numMovimenti;k++){
                            String RT[]=new String[23];
                            String movimento=listaMovimentidaConsolidare.get(k);
                            String movimentoSplittato[]=movimento.split(",");
                            String movimentoConvertito=Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                           // System.out.println(movimentoSplittato[9]);
                           if (movimentoConvertito==null)
                                {
                                   System.out.println("Errore in importazione da CDCAPP csv: "+movimento);
                                }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("CASHBACK")||
                                    movimentoConvertito.trim().equalsIgnoreCase("STAKING")||
                                    movimentoConvertito.trim().equalsIgnoreCase("EARN")||
                                    movimentoConvertito.trim().equalsIgnoreCase("REWARD")||
                                    movimentoConvertito.trim().equalsIgnoreCase("ALTRE-REWARD"))
                            {

                                RT[0] = movimentoSplittato[0] + "_CDCAPP_" + Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1] = movimentoSplittato[0];
                                RT[2] = "Crypto.com App";
                                RT[3] = "Crypto Wallet";
                                RT[4] = Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[5] = movimentoSplittato[9];
                                RT[6] = k + 1 + " di " + numMovimenti;
                                RT[7] = movimentoSplittato[9] + "(" + movimentoSplittato[1] + ")";
                                
                                if (movimentoSplittato[3].contains("-")) {
                                    RT[4] = "RIMBORSO " + Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                    RT[8] = movimentoSplittato[2];
                                    RT[9] = "Crypto";
                                    RT[10] = movimentoSplittato[3];
                                    RT[11] = "";
                                    RT[12] = "";
                                    RT[13] = "";
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                                    RT[15] = "0";
                                    RT[16] = "0";
                                    RT[17] = "Da Calcolare";//verrà calcolato con il metodo lifo
                                    RT[18] = "Da Calcolare";//verrà calcolato con il metodo lifo
                                    RT[19] = "Da Calcolare";//verrà calcolato con il metodo lifo sarà (0 - prezzo di carico)
                                } 
                                else 
                                {
                                    RT[8] = "";
                                    RT[9] = "";
                                    RT[10] = "";
                                    RT[11] = movimentoSplittato[2];
                                    RT[12] = "Crypto";
                                    RT[13] = movimentoSplittato[3];
                                    RT[14] = movimentoSplittato[6] + " " + movimentoSplittato[7];
                                    String valoreEuro = "";
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("EUR")) {
                                        valoreEuro = movimentoSplittato[7];
                                    }
                                    if (movimentoSplittato[6].trim().equalsIgnoreCase("USD")) {
                                        valoreEuro = Calcoli.ConvertiUSDEUR(movimentoSplittato[7], movimentoSplittato[0].split(" ")[0]);
                                    }
                                    RT[15] = valoreEuro;
                                    BigDecimal Dividendo = new BigDecimal(valoreEuro).setScale(8, RoundingMode.HALF_UP);
                                    BigDecimal QTA = new BigDecimal(movimentoSplittato[3]);
                                    BigDecimal risultato = Dividendo.divide(QTA, RoundingMode.HALF_UP).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
                                    String UnitarioMercato = risultato.abs().toString();
                                    String plus;
                                    if (QTA.toString().contains("-")) {
                                        plus = "-" + valoreEuro;
                                    } else {
                                        plus = valoreEuro;
                                    }
                                    RT[16] = UnitarioMercato;
                                    RT[17] = valoreEuro;
                                    RT[18] = UnitarioMercato;
                                    RT[19] = new BigDecimal(plus).setScale(2, RoundingMode.HALF_UP).toString();
                                }

                                RT[20] = "";
                                RT[21] = "";
                                RT[22] = "A";
                                lista.add(RT);
                                // Mappa_Movimenti.put(RT[0]+RT[1].split(" di ")[0].trim(), RT);
                            }
                           else if (movimentoConvertito.trim().equalsIgnoreCase("ACQUISTO CRYPTO"))
                            {
                                //trasferimento FIAT
                                RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=movimentoSplittato[0];
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]="ACQUISTO CRYPTO";
                                RT[5]="TRASFERIMENTO FIAT";
                                RT[6]=1+" di "+2;
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]="";
                                RT[9]="";
                                RT[10]="";
                                RT[11]=movimentoSplittato[2];                                                               
                                RT[12]="FIAT";
                                RT[13]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                RT[14]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                String valoreEuro="";
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();                                
                                RT[15]=valoreEuro;
                                BigDecimal Dividendo=new BigDecimal(valoreEuro).setScale(8, RoundingMode.HALF_UP);
                                BigDecimal QTA=new BigDecimal(movimentoSplittato[3]).abs();
                                
                                BigDecimal risultato=Dividendo.divide(QTA, RoundingMode.HALF_UP).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
                                String UnitarioMercato=risultato.toString();
                                RT[16]=UnitarioMercato;
                                RT[17]=valoreEuro;
                                RT[18]=UnitarioMercato;
                                RT[19]="Non applicabile";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                lista.add(RT);
                                
                                //Vendita Euro x Crypto
                                RT=new String[23];
                                RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=movimentoSplittato[0];
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]="ACQUISTO CRYPTO";
                                RT[5]="SCAMBIO FIAT/CRYPTO";//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                RT[6]=2+" di "+2;
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="FIAT";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                valoreEuro="";
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                RT[15]=valoreEuro;
                                Dividendo=new BigDecimal(valoreEuro).setScale(8, RoundingMode.HALF_UP);
                                QTA=new BigDecimal(movimentoSplittato[5]).abs();
                                risultato=Dividendo.divide(QTA,RoundingMode.HALF_UP).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
                                //System.out.println(Dividendo+ "/"+QTA+"="+risultato);
                                UnitarioMercato=risultato.toString();
                                RT[16]=UnitarioMercato;
                                RT[17]=valoreEuro;
                                RT[18]=UnitarioMercato;
                                RT[19]="Non applicabile";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                lista.add(RT); 
                                
                            }
                            else if (movimentoConvertito.trim().equalsIgnoreCase("VENDITA CRYPTO"))
                            {
                                //Vendita Crypto x Euro
                                RT=new String[23];
                                RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=movimentoSplittato[0];
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]="ACQUISTO CRYPTO";
                                RT[5]="SCAMBIO FIAT/CRYPTO";//da sistemare con ulteriore dettaglio specificando le monete trattate                                
                                RT[6]=2+" di "+2;
                                RT[7]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="FIAT";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).toString();                                 
                                RT[11]=movimentoSplittato[4];
                                RT[12]="Crypto";
                                RT[13]=new BigDecimal(movimentoSplittato[5]).abs().toString();                                                                                            
                                RT[14]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                String valoreEuro="";
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                RT[15]=valoreEuro;
                                BigDecimal Dividendo=new BigDecimal(valoreEuro).setScale(8, RoundingMode.HALF_UP);
                                BigDecimal QTA=new BigDecimal(movimentoSplittato[5]).abs();
                                BigDecimal risultato=Dividendo.divide(QTA,RoundingMode.HALF_UP).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
                                //System.out.println(Dividendo+ "/"+QTA+"="+risultato);
                                String UnitarioMercato=risultato.toString();
                                RT[16]=UnitarioMercato;
                                RT[17]=valoreEuro;
                                RT[18]=UnitarioMercato;
                                RT[19]="Non applicabile";
                                RT[20]="";
                                RT[21]="";
                                RT[22]="A";
                                lista.add(RT);                                 
                                //trasferimento FIAT
                          /*      RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=1+" di "+3;
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]=movimentoSplittato[0];
                                RT[5]="SCAMBIO-FIAT-CRYPTO";
                                RT[6]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                              
                                RT[7]="Uscita";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                RT[11]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                String valoreEuro="";
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[5];
                                if (movimentoSplittato[4].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[5], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                RT[12]=valoreEuro;
                                BigDecimal moltiplicatore=new BigDecimal(1);
                                BigDecimal Dividendo=new BigDecimal(valoreEuro);
                                BigDecimal QTA=new BigDecimal(movimentoSplittato[3]);
                                if (movimentoSplittato[3].contains("-")) moltiplicatore=new BigDecimal(-1);
                                BigDecimal risultato=Dividendo.divide(QTA,RoundingMode.CEILING).setScale(8, RoundingMode.CEILING).multiply(moltiplicatore).stripTrailingZeros();
                                String UnitarioMercato=risultato.toString();
                                //BigDecimal plus=new BigDecimal(valoreEuro).multiply(moltiplicatore);
                                RT[13]=UnitarioMercato;
                                RT[14]="NA";
                                RT[15]="NA";
                                RT[16]="NA";
                                RT[17]="";
                                RT[18]="";
                                RT[19]="A";
                                lista.add(RT);
                                
                                //Vendita Euro
                                RT=new String[20];
                                RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=2+" di "+3;
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]=movimentoSplittato[0];
                                RT[5]="SCAMBIO-FIAT-CRYPTO";
                                RT[6]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                              
                                RT[7]="Entrata";
                                RT[8]=movimentoSplittato[2];
                                RT[9]="FIAT";
                                RT[10]=new BigDecimal(movimentoSplittato[3]).abs().toString();
                                RT[11]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                valoreEuro="";
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                RT[12]=valoreEuro;
                                moltiplicatore=new BigDecimal(1);
                                Dividendo=new BigDecimal(valoreEuro);
                                QTA=new BigDecimal(movimentoSplittato[3]);
                                if (movimentoSplittato[3].contains("-")) moltiplicatore=new BigDecimal(-1);
                                risultato=Dividendo.divide(QTA,RoundingMode.CEILING).setScale(8, RoundingMode.CEILING).multiply(moltiplicatore).stripTrailingZeros();
                                UnitarioMercato=risultato.toString();
                                //BigDecimal plus=new BigDecimal(valoreEuro).multiply(moltiplicatore);
                                RT[13]=UnitarioMercato;
                                RT[14]=valoreEuro;
                                RT[15]=UnitarioMercato;
                                RT[16]="0";
                                RT[17]="";
                                RT[18]="";
                                RT[19]="A";
                                lista.add(RT); 
                                
                                //Acquisto Crypto
                                RT=new String[20];
                                RT[0]=movimentoSplittato[0]+"_CDCAPP_"+Mappa_Conversione_Causali.get(movimentoSplittato[9]);
                                RT[1]=3+" di "+3;
                                RT[2]="Crypto.com App";
                                RT[3]="Crypto Wallet";
                                RT[4]=movimentoSplittato[0];
                                RT[5]="SCAMBIO-FIAT-CRYPTO";
                                RT[6]=movimentoSplittato[9]+"("+movimentoSplittato[1]+")";                              
                                RT[7]="Entrata";
                                RT[8]=movimentoSplittato[4];
                                RT[9]="Crypto";
                                RT[10]=new BigDecimal(movimentoSplittato[5]).abs().toString();
                                RT[11]=movimentoSplittato[2]+" "+movimentoSplittato[3];///////
                                valoreEuro="";
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("EUR"))valoreEuro=movimentoSplittato[3];
                                if (movimentoSplittato[2].trim().equalsIgnoreCase("USD"))
                                    {
                                        valoreEuro=Calcoli.ConvertiUSDEUR(movimentoSplittato[3], movimentoSplittato[0].split(" ")[0]);
                                    }
                                valoreEuro=new BigDecimal(valoreEuro).abs().toString();
                                RT[12]=valoreEuro;
                                moltiplicatore=new BigDecimal(1);
                                Dividendo=new BigDecimal(valoreEuro);
                                QTA=new BigDecimal(movimentoSplittato[5]);
                                if (movimentoSplittato[3].contains("-")) moltiplicatore=new BigDecimal(-1);
                                risultato=Dividendo.divide(QTA,RoundingMode.CEILING).setScale(8, RoundingMode.CEILING).multiply(moltiplicatore).stripTrailingZeros();
                                UnitarioMercato=risultato.abs().toString();
                                //BigDecimal plus=new BigDecimal(valoreEuro).multiply(moltiplicatore);
                                RT[13]=UnitarioMercato;
                                RT[14]=valoreEuro;
                                RT[15]=UnitarioMercato;
                                RT[16]="";
                                RT[17]="";
                                RT[18]="";
                                RT[19]="A";
                                lista.add(RT); */                  
                            }
                        }
        return lista;
    }   
    
    
    
    public void Importa_Crypto_CDCExchange(){
        
    }
    
    public void Importa_Crypto_Binance(){
        
    }
    
    public void Importa_Crypto_OKX(){
        
    }
    
}
