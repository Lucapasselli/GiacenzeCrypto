/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;





import static giacenze_crypto.com.CDC_Grafica.MappaCryptoWallet;
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


public class Calcoli_RW {
    
       static String AnnoR;
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
   
       
    public static void StackLIFO_InserisciValore(Map<String, ArrayDeque> CryptoStack, String Moneta,String Qta,String Valore, String Data,String ID,String GruppoWallet) {
    
    //System.out.println(Moneta+" <> "+Qta);
    ArrayDeque<String[]> stack;
    String valori[]=new String[5];
    valori[0]=Moneta;
    //valori[1]=new BigDecimal(Qta).abs().toPlainString();
    valori[1]=Qta;
    valori[2]=Valore;
    valori[3]=Data;
    valori[4]=ID;
    if(!Qta.contains("-")){//Inserisco nello stack solo valori positivi, i token con giacenze negative ovviamente non li inserisco
        if (CryptoStack.get(Moneta)==null){
            stack = new ArrayDeque<String[]>();
            stack.push(valori);
            CryptoStack.put(Moneta, stack);
        }else{
            stack=CryptoStack.get(Moneta);
            stack.push(valori);
            CryptoStack.put(Moneta, stack);
        }
    }
    else{// se invece contiene una giacenza negativa la inserisco subito in lista per far verificare gli erorri
            
        
        
                String xlista[]=new String[13];
                xlista[0]=AnnoR;                    //Anno RW
                xlista[1]=GruppoWallet;             //GruppoWallet
                xlista[2]=Moneta;                   //Moneta
                xlista[3]=Qta;                      //Qta
                xlista[4]=Data;                     //Data Inizio
                xlista[5]="0.000";                  //Prezzo Inizio
                xlista[6]="0000-00-00 00:00";       //Data Fine
                xlista[7]="0.000";                  //Prezzo Fine
                xlista[8]="0";                      //Giorni di Detenzione
                xlista[9]="Inizio Periodo";         //Causale
                xlista[10]=ID;                      //ID Movimento Apertura (o segnalazione inizio anno)
                xlista[11]="";                     //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                xlista[12]="Errore (Giacenza Negativa)";//Tipo Errore
                
                List<String[]> ListaRW;
                ListaRW=CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                ListaRW.add(xlista);
                //Verifico Ora se esistono i prezzi unitari del token con data iniziale e del token con data finale.
                //Se esistono li scrivo nella lista come campo 13 e 14
                
                //Qui gestisco gli errori
                //Gli errori non li segnalo se il token in questione è stato identificato come Scam
                
                }
  //System.out.println(Moneta +" - "+Valore);
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
    
        if (toglidaStack)stack=CryptoStack.get(Moneta);
        else{
            ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
            stack=stack2.clone();
        }
        /*ArrayDeque<String[]> stack2=CryptoStack.get(Moneta);
        stack=stack2.clone();*/
        //System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
        while (qtaRimanente.compareTo(new BigDecimal ("0"))>0 && !stack.isEmpty()){ //in sostanza fino a che la qta rimanente è maggiore di zero oppure ho finito lo stack
           //System.out.println(Moneta+" - "+stack.size()+" - "+qtaRimanente.compareTo(new BigDecimal ("0")));
            String ultimoRecupero[];
           //System.out.println(stack.size());
            ultimoRecupero=stack.pop();
            BigDecimal qtaEstratta=new BigDecimal(ultimoRecupero[1]).abs();
            BigDecimal costoEstratta=new BigDecimal(ultimoRecupero[2]).abs();
            String dataEstratta=ultimoRecupero[3];
            String IDEstratto=ultimoRecupero[4];
         /*  if (Moneta.equalsIgnoreCase("usdt")){ 
                //System.out.println(ultimoRecupero[1]+" - "+ultimoRecupero[2]+" - "+stack.size());
                //System.out.println(qtaRimanente);
                }*/
            //System.out.println(qtaEstratta+" - "+costoEstratta);
            if (qtaEstratta.compareTo(qtaRimanente)<=0)//se qta estratta è minore o uguale alla qta rimanente allora
                {
                //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
                qtaRimanente=qtaRimanente.subtract(qtaEstratta);
                               
                //poi inserisco nella lista i dati che mi servono per chiudere le righe dell'rw
               // ListaRitorno.add(qtaEstratta.toPlainString()+";"+costoEstratta.setScale(2, RoundingMode.HALF_UP).toPlainString()+";"+dataEstratta+";"+IDEstratto);
                ListaRitorno.add(qtaEstratta.toPlainString()+";"+costoEstratta.toPlainString()+";"+dataEstratta+";"+IDEstratto);
            }else{
                //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
                //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
                //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
                //non ho più qta rimanente
                String qtaRimanenteStack=qtaEstratta.subtract(qtaRimanente).toPlainString();
                //System.out.println(qtaRimanenteStack);
               //System.out.println(qtaEstratta+" - "+qtaRimanente+"- "+qtaRimanenteStack);
                String valoreRimanenteSatck=costoEstratta.divide(qtaEstratta,30,RoundingMode.HALF_UP).multiply(new BigDecimal(qtaRimanenteStack)).abs().toPlainString();
                String valori[]=new String[]{Moneta,qtaRimanenteStack,valoreRimanenteSatck,dataEstratta,IDEstratto};
                stack.push(valori);
                BigDecimal costoTransazione=costoEstratta.subtract(new BigDecimal(valoreRimanenteSatck));
               // ListaRitorno.add(qtaRimanente.toPlainString()+";"+costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString()+";"+dataEstratta+";"+IDEstratto);
                ListaRitorno.add(qtaRimanente.toPlainString()+";"+costoTransazione.toPlainString()+";"+dataEstratta+";"+IDEstratto);
                

                qtaRimanente=new BigDecimal("0");//non ho più qta rimanente
            }
            
        }
        //pop -> toglie dello stack l'ultimo e recupera il dato
        //peek - > recupera solo il dato
        //System.out.println("RIMANENTE : "+qtaRimanente);
         if (qtaRimanente.compareTo(new BigDecimal(0))==1){
             //Se resta ancora della qta rimanente da scaricare significa che sto vendendo crypto che non posseggo, ergo mancano dei movimenti
             //in questo caso lo segnalo mettendo la data e prezzo a zero
             ListaRitorno.add(qtaRimanente.toPlainString()+";0.00;0000-00-00 00:00;Errore (Giacenza Negativa)");
         }

    }
   // ritorno=costoTransazione.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
  /* for (String elemento : ListaRitorno) {
      //System.out.println(elemento);
    }*/
    return ListaRitorno;
    //ogni singolo elemento di listaRitorno è così composto     1,025;15550;2023-01-01 00:00   (qta iniziale;valore iniziale;data iniziale)
  //System.out.println(Moneta +" - "+stack.size());
}      
    
    
    
        public static boolean StessoGruppoWalletContropate(String ID){
            boolean stessoGruppo=true;
            String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            String GruppoWalletOrigine=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
            String MovimentiCorrelati[] = Movimento[20].split(",");
            for (String IdM : MovimentiCorrelati) {
                String gruppo=DatabaseH2.Pers_GruppoWallet_Leggi(CDC_Grafica.MappaCryptoWallet.get(IdM)[3]);
                //per ogni movimento verifico se fa parte di un gruppo diverso e nel qual caso significa che la transazione riguarda wallet di gruppi diversi
                //per cui metto a false il boolean stessoGruppo
                if (!gruppo.equals(GruppoWalletOrigine))stessoGruppo=false;
            }
            return stessoGruppo;
        }
        
        public static String RitornaGruppoWalletContropate(String ID){
            String Gruppo;
            String Movimento[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            String GruppoWalletOrigine=DatabaseH2.Pers_GruppoWallet_Leggi(Movimento[3]);
            Gruppo = GruppoWalletOrigine;
            String MovimentiCorrelati[] = Movimento[20].split(",");
            for (String IdM : MovimentiCorrelati) {
                String gruppo=DatabaseH2.Pers_GruppoWallet_Leggi(CDC_Grafica.MappaCryptoWallet.get(IdM)[3]);
                //per ogni movimento verifico se fa parte di un gruppo diverso e nel qual caso significa che la transazione riguarda wallet di gruppi diversi
                //per cui metto a false il boolean stessoGruppo
                if (!gruppo.equals(GruppoWalletOrigine)){
                    Gruppo=gruppo;
                }
            }
            return Gruppo;
        }
        
    public static void ChiudiRW(Moneta Monete,Map<String, ArrayDeque> CryptoStack,String GruppoWallet,String Data,String Valore,String Causale,String IDt) {
        //System.out.println(Data+ " - "+Monete.Moneta+" - "+Monete.Qta+" - "+Monete.Prezzo+" - "+Valore+" - "+Monete.Rete+" - "+Monete.MonetaAddress);
        List<String[]> ListaRW;
        //System.out.println(Monete.Qta);
        if (!Monete.Moneta.isBlank() && !Monete.Tipo.equalsIgnoreCase("FIAT")) {//tolgo dal lifo solo se non è fiat, sulle fiat non mi interessa fare nulla attualmente
            List<String> ListaRitorno = StackLIFO_TogliQta(CryptoStack, Monete.Moneta, Monete.Qta, true);
            //creo la riga per i quadri RW
            for (String elemento : ListaRitorno) {
                String Elementi[] = elemento.split(";");
                //Elementi è così composta Qta;Prezzo;Data  
                //Se la data del movimento è uguale a quella di creazione (al minuto) metto GG di detenzione zero altrimenti anche se posseggo la moneta per un solo minuto metto 1
                long DiffData = OperazioniSuDate.ConvertiDatainLongMinuto(Data)-OperazioniSuDate.ConvertiDatainLongMinuto(Elementi[2]);
                if (DiffData!=0){
                   // DiffData = (OperazioniSuDate.ConvertiDatainLong(Data.split(" ")[0]) - OperazioniSuDate.ConvertiDatainLong(Elementi[2].split(" ")[0]) + 86400000) / 86400000;
                    DiffData = OperazioniSuDate.DifferenzaDate(Elementi[2], Data)+1;
                  //  DiffData = (OperazioniSuDate.ConvertiDatainLong(Data.split(" ")[0]) - OperazioniSuDate.ConvertiDatainLong(Elementi[2].split(" ")[0])) / 86400000;
                }
                //System.out.println(DiffData);

                String Prz;
               // if (!Valore.equals("0.00")){
                    Prz = new BigDecimal(Valore).divide(new BigDecimal(Monete.Qta), 30, RoundingMode.HALF_UP).multiply(new BigDecimal(Elementi[0])).abs().toPlainString();
               // }
                String xlista[]=new String[13];
                xlista[0]=AnnoR;                    //Anno RW
                xlista[1]=GruppoWallet;             //GruppoWallet
                xlista[2]=Monete.Moneta;            //Moneta
                xlista[3]=Elementi[0];              //Qta
                xlista[4]=Elementi[2];              //Data Inizio
                xlista[5]=Elementi[1];              //Prezzo Inizio
                xlista[6]=Data;                     //Data Fine
                xlista[7]=Prz;                      //Prezzo Fine
                xlista[8]=String.valueOf(DiffData); //Giorni di Detenzione
                xlista[9]=Causale;                  //Causale
                xlista[10]=Elementi[3];             //ID Movimento Apertura (o segnalazione inizio anno)
                xlista[11]=IDt;                     //ID Movimento Chiusura (o segnalazione fine anno o segnalazione errore)
                xlista[12]="";                      //Tipo Errore
                
                //Verifico Ora se esistono i prezzi unitari del token con data iniziale e del token con data finale.
                //Se esistono li scrivo nella lista come campo 13 e 14
                
                //Qui gestisco gli errori
                //Gli errori non li segnalo se il token in questione è stato identificato come Scam
                if (Elementi[3].contains("Errore")) {
                    xlista[12] = Elementi[3];
                }
                if (!xlista[8].equals("0")){//Solo se i giorni di detenzione sono diversi da zero compilo la lista altrimenti resta tutto così com'è.
                    ListaRW=CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(GruppoWallet);
                    ListaRW.add(xlista);
                }
            }
        }
    }
    
    public static void SistemaErroriInListe(){
        
        //Se prezzo = 0.00 significa che non esiste il prezzo della moneta
        //Lo segnalo nelgi errori
        //Poi scalo tutti i prezzi ai 2 centesimi
        for (String key : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.keySet()) {
            for (String[] lista : CDC_Grafica.Mappa_RW_ListeXGruppoWallet.get(key)) {
                Moneta mi[]=new Moneta[2];
                Moneta mf[]=new Moneta[2];
                if (MappaCryptoWallet.get(lista[10])!=null) mi=Funzioni.RitornaMoneteDaID(lista[10]);
                if (MappaCryptoWallet.get(lista[11])!=null) mf=Funzioni.RitornaMoneteDaID(lista[11]);
                //Prima di aggiungere alla tabella la riga relativa al movimento controllo se il valore è a zero
                //se il valore è zero e non esiste un prezzo per quel token a quella data allora metto errore
                String PrezzoInizio = lista[5];
                String PrezzoFine = lista[7];
                if (lista[12].isBlank()) lista[12]="<html>";
                else lista[12]="<html>"+lista[12].trim()+"<br>";
                //Se
                //A - id non reale
                //B - PrezzoInizio=0.00
                //C - Token non scam
                //Oppure
                //Se 
                //1 - ho id iniziale reale (non fine o inizio anno)
                //2 - il prezzo è zero (in qualsiasi forma)
                //3 - il token non è scam
                //4 - il prezzo cercato nel database è zero
                //5 - non esiste un prezzo sul csv
                //Allora Faccio uscire l'errore Valore Iniziale non valorizzato
                if( (MappaCryptoWallet.get(lista[10])==null                     //A
                        &&PrezzoInizio.equals("0.00")                           //B
                        &&!lista[2].contains(" **")                             //C
                     )                            
                        ||
                    (MappaCryptoWallet.get(lista[10])!=null                     //1
                        &&new BigDecimal(PrezzoInizio).compareTo(new BigDecimal(0))==0 //2
                        &&!lista[2].contains(" **")                             //3
                        &&!TokenConPrezzo(mi[1],"0.00",lista[4])                 //4
                        &&MappaCryptoWallet.get(lista[10])[14].isBlank()        //5
                     )
                    )
                {
                    lista[5] = "0";
                    lista[12] = lista[12]+"Errore (Valore Iniziale non Valorizzato) <br>";
                }
                
                
                //Stessa cosa però con il valore finale
                if( (MappaCryptoWallet.get(lista[11])==null                     //A
                        &&PrezzoFine.equals("0.00")                           //B
                        &&!lista[2].contains(" **")                             //C
                     )                            
                        ||
                    (MappaCryptoWallet.get(lista[11])!=null                     //1
                        &&new BigDecimal(PrezzoFine).compareTo(new BigDecimal(0))==0 //2
                        &&!lista[2].contains(" **")                             //3
                        &&!TokenConPrezzo(mf[0],"0.00",lista[6])                //4
                        &&MappaCryptoWallet.get(lista[11])[14].isBlank()        //5
                     )
                    )
                {
                    lista[7]="0";
                    lista[12] = lista[12]+"Errore (Valore Finale non Valorizzato) <br>";
                }
                                
                                
             /*   if (PrezzoInizio.equals("0.00")&&!lista[2].contains(" **")) {//token non valorizzato e non scam
                    lista[5] = "0";
                    lista[12] = lista[12]+"Errore (Valore Iniziale non Valorizzato) <br>";
                }*/
                /*if (PrezzoFine.equals("0.00")&&!lista[2].contains(" **")) {//token non valorizzato e non scam
                    lista[7]="0";
                    lista[12] = lista[12]+"Errore (Valore Finale non Valorizzato) <br>";
                }*/
                //lista[3]=new BigDecimal(lista[3]).setScale(10, RoundingMode.HALF_UP).toPlainString();
                if (!lista[5].equals("0"))lista[5]=new BigDecimal(lista[5]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                if (!lista[7].equals("0"))lista[7]=new BigDecimal(lista[7]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                
                //Manca da sistemare la parte relativa al fatto di non segnalare i token scam
                //Poi sarebbe anche da verificare le giacenze negative
                //e fare la modifica dei prezzi sui token con id

                if (lista[2].contains(" **")){
                    lista[12] = lista[12]+"Avviso (Token SCAM)<br>";//Se Token SCAM non verrà considerato in nessun calcolo dell'RW, verrà solo mostrato
                }
                
                //Se ID di apertura corrisponde a movimento non classificato aggiungo anche quell'errore
                if(MappaCryptoWallet.get(lista[10])!=null
                        &&MappaCryptoWallet.get(lista[10])[18].isBlank()
                        &&lista[10].split("_")[4].equals("DC")) {
                    lista[12] = lista[12]+"Errore (Movimento di apertura non Classificato) <br>";
                }
                if(MappaCryptoWallet.get(lista[11])!=null
                        &&MappaCryptoWallet.get(lista[11])[18].isBlank()
                        &&lista[11].split("_")[4].equals("PC")) {
                    lista[12] = lista[12]+"Errore (Movimento di chiusura non Classificato) <br>";
                }

                
                lista[12]=lista[12]+"</html>";
               /* else if (Elementi[3].contains("Errore") && Valore.equals("0.00")) {
                    xlista[12] = "Errore ("+Elementi[3].split("\\(")[1].replace(")", "")+" e Token non Valorizzato)";
                } else if (Valore.equals("0.00")) {
                    xlista[12] = "Errore (Token non Valorizzato)";
                } else */
            }
        }
    }

    public static boolean TokenConPrezzo(Moneta m,String Valore,String Data){
        boolean TokenConPrezzo=true;
                        //Prima di inserire il token nello stack verifico se è valorizzato a 0.00
                        //e se il token è effettivamente senza prezzo
                        //se così è allora lascio Valore a 0.00 (Significa che è un tokenm senza prezzo)
                        //Altrimenti lo valorizzo a 0.000 che significa che il suo valore è zero ma il token ha un valore intrinseco
                        //Dovrò anche verificare se per caso il token ha già prezzi che arrivano dal csv
                        if (Valore.equals("0.00")){
                            long DataRif=OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                            String Prezzo = Prezzi.DammiPrezzoTransazione(m, null, DataRif, null, true, 15, m.Rete);
                           //System.out.println("Funzione CalcoliRW TokenConPrezzo "+m.Moneta+" - "+Prezzo);
                            if (Prezzo.equals("0.00"))TokenConPrezzo=false;
                        }
        return TokenConPrezzo;
    }
    
    public static void AggiornaRW(String AnnoRif) {
        
        CDC_Grafica.Mappa_RW_ListeXGruppoWallet.clear();
        CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.clear();
        CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.clear();
        AnnoR=AnnoRif;
        String AnnoSuccessivo=String.valueOf(Integer.parseInt(AnnoRif)+1);

        //PARTE 1 : Calcolo delle Giacenze iniziali e inserimento nello stack
        int AnnoRiferimento = Integer.parseInt(AnnoRif);
        String DataInizioAnno = AnnoRif+"-01-01 00:00";
        String DataFineAnno = AnnoRif+"-12-31 23:59";
        long fine = OperazioniSuDate.ConvertiDatainLongMinuto(AnnoSuccessivo+"-01-01 00:00");//Data Fine anno in long per calcolo prezzi
        
       // long inizio = OperazioniSuDate.ConvertiDatainLongSecondo(AnnoPrecendente+"-12-31 23:59:59");//Data Fine anno in long per calcolo prezzi
        long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);//Data inizio anno in long per calcolo prezzi
        boolean PrimoMovimentoAnno = true;

////////    Deque<String[]> stack = new ArrayDeque<String[]>(); Forse questo è da mettere
        // Map<String, ArrayDeque> CryptoStack = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, ArrayDeque>> MappaGrWallet_CryptoStack = new TreeMap<>();
        Map<String, Map<String, Moneta>> MappaGrWallet_QtaCrypto = new TreeMap<>();
        Map<String, ArrayDeque> CryptoStack;// = new TreeMap<>();
        Map<String, Moneta> QtaCrypto;
        List<String[]> ListaRW;

        for (String[] v : MappaCryptoWallet.values()) {
            String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3]);
            //System.out.println(GruppoWallet);

            if (MappaGrWallet_CryptoStack.get(GruppoWallet) == null) {
                //se non esiste ancora lo stack lo creo e lo associo alla mappa
                //stessa cosa faccio per la lista per l'rw
                //stessa cosa faccio per il gruppo delle qta
                ListaRW=new ArrayList<>();
                CDC_Grafica.Mappa_RW_ListeXGruppoWallet.put(GruppoWallet, ListaRW);
                CryptoStack = new TreeMap<>();
                QtaCrypto = new TreeMap<>();
                MappaGrWallet_CryptoStack.put(GruppoWallet, CryptoStack);
                MappaGrWallet_QtaCrypto.put(GruppoWallet, QtaCrypto);
            } else {
                //altrimenti lo recupero per i calcoli
                CryptoStack = MappaGrWallet_CryptoStack.get(GruppoWallet);
                QtaCrypto = MappaGrWallet_QtaCrypto.get(GruppoWallet);
            }

            String IDTransazione = v[0];
            String Data = v[1];
            String IDTS[] = IDTransazione.split("_");
            String Valore = v[15];
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
            Monete[0].Rete = Rete;
            Monete[1].Moneta = v[11];
            Monete[1].Tipo = v[12];
            Monete[1].Qta = v[13];
            Monete[1].Rete = Rete;

            int Anno = Integer.parseInt(Data.split("-")[0]);

            //PARTE 1
            if (Anno < AnnoRiferimento) {
                //Faccio i conti per i valori iniziali

                //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                    }
                }

                //PARTE 2    
            } else if (Anno == AnnoRiferimento) {
                //al primo movimento dell'anno successivo faccio questo:
                //1 - Inseirsco nello stack tutti i valori iniziali precedentemente trovati
                //2 - Uso il lifo per il calcolo dei valori RW
                if (PrimoMovimentoAnno) {
                    for (String key : MappaGrWallet_QtaCrypto.keySet()) {
                        Map<String, Moneta> a = MappaGrWallet_QtaCrypto.get(key);
                        for (Moneta m : a.values()) {
                            if (!m.Tipo.equalsIgnoreCase("FIAT")&&new BigDecimal(m.Qta).compareTo(new BigDecimal(0))!=0) {
                                //long inizio = OperazioniSuDate.ConvertiDatainLongMinuto(DataInizioAnno);
                                m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, inizio, null, true, 15, m.Rete);
                                //System.out.println(m.Prezzo);
                                //System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo);
                                Map<String, ArrayDeque> CryptoStackTemp = MappaGrWallet_CryptoStack.get(key);
                                StackLIFO_InserisciValore(CryptoStackTemp, m.Moneta, m.Qta, m.Prezzo, DataInizioAnno,"Giacenza Inizio Anno",key);
                                if (CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key)==null){
                                    List<Moneta> li=new ArrayList<>();
                                    Moneta mo=m.ClonaMoneta();
                                    li.add(mo);
                                    CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.put(key, li);
                                }else{
                                    List<Moneta> li=CDC_Grafica.Mappa_RW_GiacenzeInizioPeriodo.get(key);
                                    Moneta mo=m.ClonaMoneta();
                                    li.add(mo);
                                }
                            }
                        }
                    }
                    PrimoMovimentoAnno = false;
                }

                //Continuo comunque a fare la somma della qta delle crypto che servirà dopo per chiudere gli RW di fine anno
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                        M1.Rete = Rete;
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                    }
                }

                //Adesso a seconda del tipo movimento devo comportarmi in maniera diversa
                //TIPOLOGIA = 0 (Vendita Crypto)
                if (IDTS[4].equals("VC")
                        || IDTS[4].equals("SC")
                        || IDTS[4].equals("AC")
                        //La Reward va qua solo se è un rimborso della reward per qualche motivo, altrimenti va cosiderata come un acquisto
                        || IDTS[4].equals("RW")//Se Monete[1].Moneta è vuota vuol dire che è un rimborso
                        || IDTS[4].equals("CM")) {

                    //tolgo dal Lifo della moneta venduta e prendo la lista delle varie movimentazione
                    String Causale="Vendita";
                    switch (IDTS[4]) {
                        case "VC" -> Causale="Vendita";
                        case "SC" -> Causale="Scambio";
                        case "CM" -> Causale="Commissione";
                        case "RW" -> Causale="Rimborso Ricompensa";
                        default -> {
                        }
                    }
                    if (!Monete[0].Moneta.isBlank() && !Monete[0].Tipo.equalsIgnoreCase("FIAT")) {
                        //Chiudo RW se ho una moneta in uscita che è diversa da una FIAT (le FIAT non le considero per l'RW)
                        //Se è un movimento RW ()quindi un rimborso di un RW se è qua) e il prezzo è zero lo ricalcolo perchè viene messo a zero dal sistema per il calcolo delle plusvalenze
                        //ma non è corretto per il calcolo del quadro RW
                        long d=OperazioniSuDate.ConvertiDatainLongMinuto(Data);
                        if(IDTS[4].equals("RW")&&Valore.equals("0.00"))Valore=Prezzi.DammiPrezzoTransazione(Monete[0], null, d, null, true, 15, Monete[0].Rete);
                        ChiudiRW(Monete[0], CryptoStack, GruppoWallet, Data, Valore, Causale,IDTransazione);
                    }
                    if (!Monete[1].Moneta.isBlank() && !Monete[1].Tipo.equalsIgnoreCase("FIAT")) {
                        //Apro RW se ho una moneta in ingresso diversa da una FIAT (le FIAT non le considero per l'RW)  
                        StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data,IDTransazione,GruppoWallet);
                    }

                } else if (IDTS[4].equals("DF")//deposito Fiat
                        || IDTS[4].equals("PF")//Prelievo Fiat
                        || IDTS[4].equals("SF")//Scambio Fiat
                        || IDTS[4].equals("TI"))//Trasferimento Interno
                {
                    //Queste sono categorie per cui non va fatto rw quindi le escludo

                } else if (IDTS[4].equals("DC")//Deposito Crypto
                        || IDTS[4].equals("PC"))//Prelievo Crypto
                {
                    //Le tipologie possono essere le seguenti
                    //PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
                    //PCO -> Cashout o similare (prelievo)
                    //PTW -> Trasferimento tra Wallet (prelievo)
                    //DTW -> Trasferimento tra Wallet (deposito)
                    //DAI -> Airdrop o similare (deposito)
                    //DCZ -> Costo di carico 0 (deposito)
                    if (v[18].isBlank()) {
                        //Se i movimenti non sono classificati identifico come Cashout i movimenti in uscita
                        //e come reward i movimenti in ingresso
                        //Devo anche emettere qualche sorta di avviso
                        //System.out.println("Movimento non classificato");
                        if(IDTS[4].equals("PC"))
                        {    
                            //System.out.println("Movimento non classificato Prelievo");
                            ChiudiRW(Monete[0], CryptoStack, GruppoWallet, Data,Valore, "Prelievo Sconosciuto",IDTransazione);
                        }
                        else 
                        {   
                            //System.out.println("Movimento non classificato Deposito");
                            StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data,IDTransazione,GruppoWallet);
                        }
                    } else if (v[18].contains("PWN") || v[18].contains("PCO")) {
                        //Chiudo RW
                        ChiudiRW(Monete[0], CryptoStack, GruppoWallet, Data,Valore, "Cashout o Similare",IDTransazione);
                    } else if (v[18].contains("DAI") || v[18].contains("DCZ")) {
                        //Apro nuovo RW
                        StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data,IDTransazione,GruppoWallet);
                    } else if (v[18].contains("PTW")) {
                       // System.out.println(StessoGruppoWalletContropate(IDTransazione) + " : " + IDTransazione);
                        //Se è un trasferimento tra wallet dello stesso gruppo non faccio nulla
                        //Se è un trasferimento tra wallet di gruppi diversi chiudo l'RW
                        //Se è trasferimento per scambio non faccio nulla perchè viene tutto gestito nel momento del deposito sul wallet di destinazione
                        //Se è un trasferimento a vault non faccio nulla perchè ritengo che il valut faccia sempre riferimento al mio wallet quindi di conseguenza i token non sono mai usciti
                        if (v[18].contains("PTW - Trasferimento tra Wallet")) {
                            //se soddisfa questa condizione sono in presenza di un trasferimento tra wallet
                            //adesso devo verificare se il Gruppo wallet della controparte è lo stesso del mio o meno

                            if (StessoGruppoWalletContropate(IDTransazione)) {

                                //Non faccio nulla se sono nello stesso gruppo
                            } else {
                                //Se è un trasferimento tra wallet di gruppi diversi chiudo l'RW
                                ChiudiRW(Monete[0], CryptoStack, GruppoWallet, Data, Valore,"Trasferimento su altro Wallet",IDTransazione);
                            }
                        }

                    } else if (v[18].contains("DTW")) {
                        //Se è un trasferimento tra wallet dello stesso gruppo non faccio nulla
                        //Se è un trasferimento tra wallet di gruppi diversi apro l'RW
                        //Se è un deposito per scambio differito nello stesso gruppo wallet non faccio nulla
                        //Se è un deposito per scambio differito da un diverso gruppo wallet chiudo RW vecchio Wallet apro RW nuovo Wallet
                        //Da ricordare che scambio, invio e ricezione del token vengono generati nello stesso istante
                        //E lo scambio già di per se va a chiudere un rw e aprirne uno nuovo
                        if (v[18].contains("DTW - Trasferimento tra Wallet")) {
                            //se soddisfa questa condizione sono in presenza di un trasferimento tra wallet
                            //adesso devo verificare se il Gruppo wallet della controparte è lo stesso del mio o meno
                            if (StessoGruppoWalletContropate(IDTransazione)) {
                                //Non faccio nulla se sono nello stesso gruppo
                            } else {
                                //Se è un trasferimento tra wallet di gruppi diversi apro il nuovo RW
                                StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data,IDTransazione,GruppoWallet);
                            }
                        } else if (v[18].contains("DTW - Scambio Differito")) {
                            //Es. Scambio differito
                            //Mov. 1 - Wallet 1 - Invia 1 ETH a piattaforma di scambio
                            //Mov. 2 - Wallet 1 - La piattaforma di scambio riceve l'ETH
                            //Mov. 3 - Wallet 1 - La piattaforma di scambio scambia ETH con BTC
                            //Mov. 4 - Wallet 1 - La piattaforma di scambio invia BTC al Wallet 2
                            //Mov. 5 - Wallet 2 - La piattaforma di scambio riceve i BTC
                            //Se è l'ultimo movimento di uno scambio differito allora chiudo l'rw del vecchio wallet e apro l'RW del nuovo
                            //infatti la conversione tra le monete è già stata classificata
                            //l'RW chiuso non verrà considerato in quanto avrà zero come lasso di tempo ovvero GG, scambio token invio e ricezione avvengono infatti nello stesso momento.
                            if (StessoGruppoWalletContropate(IDTransazione)) {
                                //Non faccio nulla se sono nello stesso gruppo
                            } else {
                                //Se è un trasferimento tra wallet di gruppi diversi apro il nuovo RW
                                StackLIFO_InserisciValore(CryptoStack, Monete[1].Moneta, Monete[1].Qta, Valore, Data,IDTransazione,GruppoWallet);
                                //Adesso devo recuperare il CryptoStack dell'altro wallet e togliere dall'RW la moneta
                                String gruppoControparte = RitornaGruppoWalletContropate(IDTransazione);
                                //gruppoControparte non sarà mai nulla perchè l'ho già verificato nelle righe sopra quindi evito di fare un altro IF
                                Map<String, ArrayDeque> CryptoStackControparte = MappaGrWallet_CryptoStack.get(gruppoControparte);
                                //Chiudo l'RW della controparte
                                ChiudiRW(Monete[1], CryptoStackControparte, gruppoControparte, Data, Valore,"Trasferimento su altro Wallet","Giorni Detenzione Zero");

                            }

                        }
                    } else {
                        System.out.println("Attenzione movimento di deposito o prelievo non classificato nella funzione AggiornaRW() in CalcoliRW");
                    }
                }else {
                        System.out.println("Attenzione movimento "+IDTS[4]+" non considerato in CalcoliRW");
                    }

                //PARTE 3
            } else if (Anno > AnnoRiferimento) {
                //Non faccio nulla

            }
        }
        
                //finito il ciclo
                //1 - Trovo il valore di fine anno di riferimento relativo a tutti i token e chiudo tutti i conti aperti
                //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                        for (String key : MappaGrWallet_QtaCrypto.keySet()) {
                    Map<String, Moneta> a = MappaGrWallet_QtaCrypto.get(key);
                    for (Moneta m : a.values()) {
                        if (!m.Tipo.equalsIgnoreCase("FIAT")&&new BigDecimal(m.Qta).compareTo(new BigDecimal(0))!=0) {
                           // long fine = OperazioniSuDate.ConvertiDatainLongMinuto(DataFineAnnoCalcoloPrezzi);
                          // m.Moneta="BTC";
                            m.Prezzo = Prezzi.DammiPrezzoTransazione(m, null, fine, null, true, 15, m.Rete); 
                            //System.out.println(Prezzi.DammiPrezzoTransazione(m, null,fine, null, true, 15, m.Rete));
                            //System.out.println(key+" - "+m.Moneta + " - " + m.Qta + " - " + m.Prezzo+ " - "+m.MonetaAddress+ " - "+ m.Rete);
                            Map<String, ArrayDeque> CryptoStackTemp = MappaGrWallet_CryptoStack.get(key);
                           // StackLIFO_InserisciValore(CryptoStackTemp, m.Moneta, m.Qta, m.Prezzo, DataFineAnno);
                            ChiudiRW(m, CryptoStackTemp, key, DataFineAnno, m.Prezzo,"Fine Anno","Giacenza Fine Anno");
                            //Questo qua sotto popola una lista per ogni gruppo wallet contenente la giacenza di ciascuna moneta ad inizio anno
                            
                            if (CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(key)==null){
                                    List<Moneta> li=new ArrayList<>();
                                    li.add(m);
                                    CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.put(key, li);
                                }else{
                                    List<Moneta> li=CDC_Grafica.Mappa_RW_GiacenzeFinePeriodo.get(key);
                                    li.add(m);
                                }

                        }
                    }
                }
        
        SistemaErroriInListe();
        
    }
    
    
    
}
