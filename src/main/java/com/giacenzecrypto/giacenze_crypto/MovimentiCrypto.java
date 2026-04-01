/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Importazioni.ColonneTabella;
import static com.giacenzecrypto.giacenze_crypto.Importazioni.RiempiVuotiArray;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author lucap
 */
public class MovimentiCrypto {
    
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
    m.put("REWARD",  new String[]{"RW", "REWARD",""});
    m.put("CASHBACK",       new String[]{"RW", "CASHBACK",""});
    m.put("EARN",           new String[]{"RW", "EARN",""});
    m.put("STAKING REWARD",   new String[]{"RW", "STAKING REWARD",""});
    m.put("STAKING REWARDS",  new String[]{"RW", "STAKING REWARD",""});
    
   // m.put("SCAMBIO CRYPTO",       new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
   // m.put("SCAMBIO CRYPTO-CRYPTO",           new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
   // m.put("DUST-CONVERSION",           new String[]{"SC", "SCAMBIO CRYPTO",""});//Li faccio decidere al programma
    
   // m.put("ACQUISTO CRYPTO",           new String[]{"AC", "ACQUISTO CRYPTO",""});//Li faccio decidere al programma
   // m.put("VENDITA CRYPTO",           new String[]{"VC", "VENDITA CRYPTO",""});//Li faccio decidere al programma
    
    m.put("TRASFERIMENTO INTERNO",           new String[]{"TI", "TRASFERIMENTO INTERNO",""});
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
            String TipoTr,
            String IdentificazioneID    //E' il secondo campo dell'ID se null viene valorizzato con il nome del wallet altrimenti con quello passato in questo campo
    ) {
        //========== SISTEMO I NOMI IN MODO CHE SIANO SAFE ===========
        Wallet=normalizzaNome(Wallet);
        Wallet2=normalizzaNome(Wallet2);
        Nota=normalizzaNome(Nota);
        IDTransHash=normalizzaNome(IDTransHash);
        IdentificazioneID=normalizzaNome(IdentificazioneID);
        
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
            //La moneta deve :
            //1 - Esistere
            //2 - La sua qta deve essere un numero
            //3 - La sua qta deve essere diversa da 0
            if (MON != null &&
                Funzioni.isNumeric(MON.Qta, false)&&
                new BigDecimal(MON.Qta).abs().compareTo(BigDecimal.ZERO)!=0) {
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
                    if (!MIn.isBlank()){
                        LoggerGC.ScriviErrore("Movimento incoerente, ci sono due monete in ingresso : "+MIn+" e "+MON.Moneta);
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
        //Se nessuna moneta è valida termino la richiesta e ritorno null.
        if (MOut.isBlank()&&MIn.isBlank())
        {
            LoggerGC.ScriviErrore("Nessuna moneta è valida termino la richiesta");
            return null;
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
        Moneta MPR = DammiMonetaPrioritaria(Mon[0], Mon[1]);
        //Se passo il prezzo della transazione uso quello
        
        if (Prezzo != null && Funzioni.isNumeric(Prezzo, false)) {
            if (PrezzoPrezzato(Prezzo))RT2[32] = "SI";
            else RT2[32] = "NO";
            Prezzo = new BigDecimal(Prezzo).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();//Questo impedisce che il prezzo sia negativo
            if (FontePrezzo == null) {   
               // System.out.println("Personalizzato1");
                FontePrezzo = "Personalizzato";
            }
            RT2[40] = "|||" + FontePrezzo;
            
            //System.out.println("Prezzi presi dal csv");
        } //Se non passo il prezzo ma lo trovo nelle monete l'infoprezzo
        
        else if (MPR!=null&&MPR.InfoPrezzo != null && MPR.InfoPrezzo.Qta != null && MPR.InfoPrezzo.prezzoUnitario != null) {
            Prezzi.InfoPrezzo IP = MPR.InfoPrezzo;
            if (IP != null && IP.Qta != null && IP.prezzoUnitario != null) {
                //E' importante sempre fare il calcolo del prezzo e non prendere quello salvato in prezzoqta perchè potrebbe non essere giusto
                //se arriva da molteplici scambi ad esempio nella classe TransazioneDefi
                Prezzo = IP.Qta.multiply(IP.prezzoUnitario).setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
                RT2[40] = IP.Ritorna40();
                RT2[32] = "SI";
               // System.out.println("Prezzi presi dall'info prezzo singolo token");
            }
        } //se la moneta non ha infoprezzo ma ha prezzo prendo quello
        else if (MPR!=null&&MPR.Prezzo != null && Funzioni.isNumeric(MPR.Prezzo, false)) {
            if (PrezzoPrezzato(MPR.Prezzo))RT2[32] = "SI";
            else RT2[32] = "NO";
            Prezzo = new BigDecimal(MPR.Prezzo).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();//Questo impedisce che il prezzo sia negativo
            if (FontePrezzo == null) {
                if (MPR.InfoPrezzo!=null&&MPR.InfoPrezzo.Fonte!=null&&!MPR.InfoPrezzo.Fonte.isBlank())
                {
                    FontePrezzo=MPR.InfoPrezzo.Fonte;
                }
                else 
                {
                   // System.out.println("Personalizzato2");
                    FontePrezzo = "Personalizzato";
                }
            }
            RT2[40] = "|||" + FontePrezzo;
            
            //System.out.println("Prezzi presi dal prezzo singolo token");
        } //se non lo trovo neanche la allora lo calcolo
        else {
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(Mon[0], Mon[1], Timestamp, Rete, Wallet);
            if (IP != null) {
                Prezzo = IP.prezzoQta.setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
                RT2[40] = IP.Ritorna40();
                RT2[32] = "SI";
            } else {
                Prezzo = "0.00";
                RT2[32] = "NO";
            }
           // System.out.println("Prezzi calcolati");
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
                //Questa istruzione serve per modificare una copia dell'array estratto e non quello originale
                Tipologie = java.util.Arrays.copyOf(Tipologie, Tipologie.length);
                Tipologie[1]="RIMBORSO "+Tipologie[1];
                //In caso di Rimborso devo anche azzerare il prezzo
                Prezzo="0.00";
            }
            //Nel caso in cui recupero correttamente il movimento se è un movimento CM (Commissione) di ingresso devo inserirlo come cashback
            else if (Tipologie[0].equals("CM")&&MOut.isBlank()){
                //Questa istruzione serve per modificare una copia dell'array estratto e non quello originale
                Tipologie = java.util.Arrays.copyOf(Tipologie, Tipologie.length);
                Tipologie[1]="CASHBACK";
                Tipologie[0]="RW";
            }

        }
        
        
        //========== GESTISCO L'ID ELLA TRANSAZIONE =========== 
        
        if(IdentificazioneID.isBlank())IdentificazioneID=Wallet;//Non serve il test del null perchè il campo è già normalizzato
        
        //caso commissioni
        if (Tipologie[0].equals("CM")&&MIn.isBlank()){
                //Questa istruzione serve per modificare una copia dell'array estratto e non quello originale
                IdentificazioneID=IdentificazioneID+"C";
            }
        
        //Adesso controllo se è un movimento Interno se così fosse devo fare in modo che i prelievi siano sempre prima dei depositi
        //Questo perchè sono 2 movimenti contemporanei tolgo da una parte e metto dall'altra.
        //Per far questo sui depositi inserisco una A dopo il nome del Wallet. quindi dopo trunk 1
        if (Tipologie[0].equals("TI")&&MOut.isBlank()){
                //Questa istruzione serve per modificare una copia dell'array estratto e non quello originale
                IdentificazioneID=IdentificazioneID+"A";
            }
        
        RT2[0] = DataID + "_" + IdentificazioneID + "_" + numMovimento + "_" + numMovimento2 + "_" + Tipologie[0];
        if (IDMovimento != null) {
            //Nel caso in cui prendo l'id passato recupero però sempre il codice tipologia reale
            String IDMovimentoS[]=IDMovimento.split("_");
            if (IDMovimentoS.length==5){
                RT2[0] = IDMovimentoS[0]+"_"+IDMovimentoS[1]+"_"+IDMovimentoS[2]+"_"+IDMovimentoS[3]+"_"+Tipologie[0];
            }
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

//Controlla se il prezzo inserito è prezzato o meno, in sostanza se il prezzo non è un numero mette false
//se il prezzo è un numero pari a 0 ma non è espressamente 0.00 metto false
//altrimenti metto true
static boolean PrezzoPrezzato(String Prezzo) {
    if (!Funzioni.isNumeric(Prezzo, false))return false;
    else if (new BigDecimal(Prezzo).compareTo(BigDecimal.ZERO)==0 && !Prezzo.equals("0.00")){
        return false;
    }
    return true;
}    

    static Moneta DammiMonetaPrioritaria(Moneta Moneta1a, Moneta Moneta2a) {


 /*Questa funzione si divide in 4 punti fondamentali:
        1 - Verifico che una delle 2 monete di scambio sia una Fiat e in quel caso prendo quello come prezzo della transazione anche perchè è il più affidabile
        2 - Verifico se una delle 2 monete è USDT in quel caso prendo quello come valore in quanto USDT è una moneta di cui mi salvo tutti i prezzi storici
        3 - Verifico se una delle 2 monete non faccia parte di uno specifico gruppo delle monete più capitalizzate presenti su binance, in quel caso prendo quello come
        prezzo della transazione in quanto il prezzo risulta sicuramente più preciso di quello di una shitcoin o comunque di una moneta con bassa liquidità
        4 - Prendo il prezzo della prima moneta disponibile essendo che l'affidabilità del prezzo è la stessa per entrambe le monete dello scambio      
         */

        Moneta mon[] = new Moneta[]{Moneta1a, Moneta2a};

        //PARTE 1 - ANALISI PRELIMINARE DATI

    
        //PARTE 2 - STABILISCO LA PRIORITA' DI ASSEGNAZIONE PREZZI SUI TOKEN
        //(In caso in cui la transazioni presenti 2 token scelgo quale token determinerà iol prezzo della transazione con queasta priorità:)
        //A - FIAT
        //B - STABLECOIN
        //C - Selezione di Crypto ad alta capitalizzazione (quindi con meno oscillazioni)
        
        //A - VERIFICO SE FIAT EURO (in quel caso prendo quel prezzo per la transazione che è il più accurato)
        for (int k = 0; k < 2; k++) {
            if (mon[k] != null && mon[k].Tipo.trim().equalsIgnoreCase("FIAT") && mon[k].Moneta.equalsIgnoreCase("EUR")) {
                    return mon[k];
            }
        }
        
        //VERIFICO SE USD e prendo il prezzo da li
            for (int k=0;k<2;k++){
            if (mon[k] != null && mon[k].Moneta.equalsIgnoreCase("USD") && !mon[k].Tipo.trim().equalsIgnoreCase("NFT")&&mon[k].MonetaAddress == null) {
                //a seconda se ho l'address o meno recupero il suo prezzo in maniera diversa
                //anche perchè potrebbe essere che sia un token che si chiama usdt ma è scam              
                return mon[k];
            } 
            }

         //se non sono FIAT controllo se una delle coppie è USDT in quel caso prendo il prezzo di quello 
        
            //B e C - VERIFICO SE COPPIE PRIORITARIE
            //ora scorro le coin principali per vedere se trovo corrispondenze e in quel caso ritorno il prezzo
            //I simboli vengono interrogati per ordine di importanza ovvero nell'ordine in cui sono stati inseriti nella variabile
        
        for (String SimboloPrioritario : Prezzi.SimboliPrioritari) {
            for (int k = 0; k < 2; k++) {
                if (mon[k] != null && (mon[k].Moneta).toUpperCase().equals(SimboloPrioritario) && mon[k].Tipo.trim().equalsIgnoreCase("Crypto")) {
                    //come prima cosa provo a vedere se ho un prezzo personalizzato e uso quello
                    return mon[k];
                }
            }
        }
        


        //Se arrivo qua vuol dire che non ho trovato nessuna moneta prioritaria quindi prendo la prima di cui trovo il prezzo
        for (int k = 0; k < 2; k++) {
            if (mon[k] != null){
                if (mon[k].Prezzo!=null&&Funzioni.isNumeric(mon[k].Prezzo, false)) return mon[k];
                if (mon[k].InfoPrezzo!=null&&mon[k].InfoPrezzo.Qta!=null&&mon[k].InfoPrezzo.prezzoUnitario!=null) return mon[k];
            }
        }
        return null;

    }

    
    //La Funzione trova un id univoco sul movimento
    //Se va in errore per qualche motivo restituisce null altrimenti restituisce l'ID
     public static String getIDUnivoco(Map<String, String[]> map, String id) {
        String currentId = id;

        while (true) {
            if (map.get(currentId)==null){
                return currentId; 
            }
            currentId = incrementaQuartoCampoID(currentId);
            if (currentId==null)return null;
        }
    }

    private static String incrementaQuartoCampoID(String id) {
        String[] parts = id.split("_");

        if (parts.length < 5) {
           // throw new IllegalArgumentException("Formato ID non valido: " + id);
            LoggerGC.ScriviErrore("Formato ID non valido: " + id);
            return null;
        }
        if (Funzioni.isNumeric(parts[3], false)){
            LoggerGC.ScriviErrore("Quarto campo ID non valido: " + id);
            return null;
        }

        int fourthField = Integer.parseInt(parts[3]);
        parts[3] = String.valueOf(fourthField + 1);

        return String.join("_", parts);
    }
    
    
    
    
}
