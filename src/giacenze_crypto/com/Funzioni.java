/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Luca
 */
public class Funzioni {
        public static int CancellaMovimentazioniXWallet(String Wallet){
         
        int movimentiCancellati=0;
       //this.TransazioniCryptoFiltro_Text.setText("");
        //questo server per velocizzare la ricerca
        //disabilito il filtro e poi lo riabilito finito l'eleaborazione
        
        List<String> Cancellare=new ArrayList<>();
        

        for (String v : CDC_Grafica.MappaCryptoWallet.keySet()) {
            if (CDC_Grafica.MappaCryptoWallet.get(v)[3].trim().equalsIgnoreCase(Wallet.trim()))
            {
                //MappaCryptoWallet.remove(v);
                Cancellare.add(v);
                movimentiCancellati++;
            }
        }
        Iterator I=Cancellare.iterator();
        while (I.hasNext()){
            String daRimuovere=I.next().toString();
            RimuoviMovimentazioneXID(daRimuovere);
        }
        
           // MappaCryptoWallet.clear();
   
        return movimentiCancellati;
    }
        
        //Funzione che si occupa di rimuovere una movimentazione
        //eliminando o sistemando anche tutti i movimenti correlati.
        //ad esempio se devo rimuovere un movimento di prelievo che è associato ad un altro movimento di dieposito
        //prima di rimuovere il prelievo vado a torgliere dal deposito i riferimenti al prelievo che devo eliminare
         public static void RimuoviMovimentazioneXID(String ID){
            String Annessi[]=CDC_Grafica.MappaCryptoWallet.get(ID);
            String PartiCoinvolte[]=(ID+","+Annessi[20]).split(",");
            if (Annessi[20]!=null && !Annessi[20].equalsIgnoreCase("")){
                ClassificazioneTrasf_Modifica.RiportaTransazioniASituazioneIniziale(PartiCoinvolte);
            }
            CDC_Grafica.MappaCryptoWallet.remove(ID);
             
         }
        
         
        public static void CompilaMappaChain(){
            //indirizzoExplorer,api,coin commissioni,nomeEndpointCoingecko
            //System.out.println("Compilo Mappe integrate nel codice");
            String BSC[]=new String[]{"https://api.bscscan.com","6qoE9xw4fDYlEx4DSjgFN0+B5Bk8LCJ9/R+vNblrgiyVyJsMyAhhjPn8BWAi4LM6","BNB","binance-smart-chain"};
            String CRO[]=new String[]{"https://api.cronoscan.com","nYb1EJijpYUyiLKatxoMYI6TWXp+BpOG6hSuriJHVOG7exj5lMlMbw4lKAtdSHYc","CRO","Cronos"};
            CDC_Grafica.Mappa_ChainExplorer.put("CRO", CRO);
            CDC_Grafica.Mappa_ChainExplorer.put("BSC", BSC);             
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x66e428c3f67a68878562e79A0234c1F83c208770_CRO", "USDT");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xc21223249CA28397B4B6541dfFaEcC539BfF0c59_CRO", "USDC");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xC74D59A548ecf7fc1754bb7810D716E9Ac3e3AE5_CRO", "BUSD");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xF2001B145b43032AAF5Ee2884e456CCd805F677D_CRO", "DAI");
            
        }  
         
}
