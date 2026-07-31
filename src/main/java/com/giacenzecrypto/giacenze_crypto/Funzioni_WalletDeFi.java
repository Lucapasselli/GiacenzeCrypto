/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Desktop;
import java.util.regex.Pattern;
import org.bitcoinj.core.Base58;

/**
 *
 * @author luca.passelli
 */
public class Funzioni_WalletDeFi {
    
    
    
    
     
     /**
      * Apre nel browser predefinito la pagina dell'explorer blockchain relativa alla transazione di un
      * movimento (usando {@link #ApriExplorer}), in base alla rete determinata da {@link Funzioni#TrovaReteDaID}.
      * @param ID identificativo del movimento
      * @return {@code true} se è stato possibile determinare rete e transazione (l'apertura del browser non ne condiziona il ritorno), {@code false} se il movimento non ha i campi attesi o la rete non è determinabile
      */
     public static boolean ApriExplorer (String ID){
            

        if (Principale.MappaCryptoWallet.get(ID).length < 25) {
            return false;
        }

        String IDTransazione = Principale.MappaCryptoWallet.get(ID)[24];
        // String ID=TransazioniCryptoTabella.getModel().getValueAt(rigaselezionata, 0).toString();
        String Rete = Funzioni.TrovaReteDaID(ID);
        //System.out.println(IDTransazione+"-"+Rete);
        if (Rete == null) {
            return false;
        }
        if (IDTransazione != null) {
            String Url = UrlExplorerTx(Rete, IDTransazione);
            if (Url != null) Funzioni.ApriWeb(Url);
        }
        return true;

    }

     /**
      * Costruisce l'indirizzo della pagina dell'explorer che mostra una transazione, senza aprirla: serve
      * sia ad {@link #ApriExplorer} sia alla funzione "Chiedi a IA", che include il link nella domanda
      * rivolta al chatbot.
      * @param Rete codice della rete blockchain
      * @param Hash hash della transazione
      * @return l'URL della transazione sull'explorer della rete, oppure {@code null} se la rete non &egrave;
      *         tra quelle note o se mancano rete o hash
      */
     public static String UrlExplorerTx(String Rete, String Hash) {
        if (Rete == null || Hash == null || Hash.isBlank()) return null;

        if (Rete.equalsIgnoreCase("BSC")) return "https://bscscan.com/tx/" + Hash;
        if (Rete.equalsIgnoreCase("CRO")) return "https://cronoscan.com/tx/" + Hash;
        if (Rete.equalsIgnoreCase("ETH")) return "https://etherscan.io/tx/" + Hash;
        if (Rete.equalsIgnoreCase("BASE")) return "https://basescan.org/tx/" + Hash;
        if (Rete.equalsIgnoreCase("ARB")) return "https://arbiscan.io/tx/" + Hash;
        if (Rete.equalsIgnoreCase("SOL")) return "https://solscan.io/tx/" + Hash;
        if (Rete.equalsIgnoreCase("BERA")) return "https://berascan.com/tx/" + Hash;
        if (Rete.equalsIgnoreCase("AVAX")) return "https://avascan.info/blockchain/c/tx/" + Hash;
        if (Rete.equalsIgnoreCase("BTC")) return "https://mempool.space/tx/" + Hash;
        if (Rete.equalsIgnoreCase("POL")) return "https://polygonscan.com/tx/" + Hash;
        if (Rete.equalsIgnoreCase("MONAD")) return "https://monadscan.com/tx/" + Hash;
        if (Rete.equalsIgnoreCase("GNOSIS")) return "https://gnosisscan.io/tx/" + Hash;

        return null;
    }
     
     /**
      * Apre nel browser predefinito la pagina dei movimenti di un token per un wallet sull'explorer della rete
      * indicata (per Bitcoin apre semplicemente la pagina dell'indirizzo, non essendo un token).
      * @param Wallet indirizzo del wallet
      * @param Address indirizzo di contratto del token (ignorato per Bitcoin)
      * @param Rete identificativo della blockchain/rete
      */
     public static void ApriMovimentiWallet(String Wallet,String Address,String Rete){
           
            
                
                        if (Rete.equalsIgnoreCase("BSC")){
                            Funzioni.ApriWeb("https://bscscan.com/token/"+Address +"?a="+ Wallet);
                           }
                        else if(Rete.equalsIgnoreCase("CRO")){
                           Funzioni.ApriWeb("https://cronoscan.com/token/"+Address +"?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("ETH")){
                           Funzioni.ApriWeb("https://etherscan.io/token/"+Address +"?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("BASE")){
                           Funzioni.ApriWeb("https://basescan.org/token/"+Address +"?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("ARB")){
                           Funzioni.ApriWeb("https://arbiscan.io/token/"+Address +"?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("SOL")){
                           Funzioni.ApriWeb("https://solscan.io/token/"+Address +"?a="+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("BERA")){
                           Funzioni.ApriWeb("https://berascan.com/token/"+Address +"?a="+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("BTC")){
                           Funzioni.ApriWeb("https://mempool.space/address/"+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("POL")){
                           Funzioni.ApriWeb("https://polygonscan.com/token/"+Address +"?a="+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("MONAD")){
                           Funzioni.ApriWeb("https://monadscan.com/token/"+Address +"?a="+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("GNOSIS")){
                           Funzioni.ApriWeb("https://gnosisscan.io/token/"+Address +"?a="+ Wallet);
                        }


     }
     
          /**
           * Apre nel browser predefinito la pagina di riepilogo delle giacenze (token holdings/portfolio) di un
           * wallet sull'explorer della rete indicata, se il sistema supporta {@link Desktop}.
           * @param Wallet indirizzo del wallet
           * @param Rete identificativo della blockchain/rete
           */
          public static void ApriSituazioneWallet(String Wallet,String Rete){
           
            
               if (Desktop.isDesktopSupported()) {
                        if (Rete.equalsIgnoreCase("BSC")){
                            Funzioni.ApriWeb("https://bscscan.com/tokenholdings?a="+ Wallet);
                           }
                        else if(Rete.equalsIgnoreCase("CRO")){
                           Funzioni.ApriWeb("https://cronoscan.com/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("ETH")){
                           Funzioni.ApriWeb("https://etherscan.io/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("BASE")){
                           Funzioni.ApriWeb("https://basescan.org/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("ARB")){
                           Funzioni.ApriWeb("https://arbiscan.io/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("BERA")){
                           Funzioni.ApriWeb("https://berascan.com/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("SOL")){
                           Funzioni.ApriWeb("https://solscan.io/account/"+ Wallet+"#portfolio");
                        }
                        else if(Rete.equalsIgnoreCase("BTC")){
                           Funzioni.ApriWeb("https://mempool.space/address/"+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("POL")){
                           Funzioni.ApriWeb("https://polygonscan.com/tokenholdings?a="+ Wallet); 
                        }
                        else if(Rete.equalsIgnoreCase("MONAD")){
                           Funzioni.ApriWeb("https://monadscan.com/tokenholdings?a="+ Wallet);
                        }
                        else if(Rete.equalsIgnoreCase("GNOSIS")){
                           Funzioni.ApriWeb("https://gnosisscan.io/tokenholdings?a="+ Wallet);
                        }
                    }

     }
     
     
     /**
      * Verifica se una stringa nel formato {@code indirizzo (RETE)} rappresenta un wallet DeFi valido: la rete
      * deve essere tra quelle con explorer DeFi integrato e l'indirizzo deve essere valido per quella rete
      * secondo {@link #isValidAddress}.
      * @param wallet stringa wallet nel formato {@code indirizzo (RETE)}
      * @return {@code true} se rete e indirizzo sono validi
      */
     public static boolean isValidDefiWallet(String wallet) {
        //Questa funzione serve per sapere se una stringa wallet presente nella colonna wallet es 0x3423432aff4545 (ETH)
        //può essere considerata un wallet valido, si controllerà quindi l'indirizzo e se la rete è supportata
        String RetiSupportate="||BSC||CRO||BASE||ARB||ETH||SOL||BERA||AVAX||POL||MONAD||BTC||GNOSIS||";
        String sWallet[]=wallet.split("\\(");
        String address;
        String Rete;
        if (sWallet.length==2){
            address=sWallet[0].trim();
            Rete=sWallet[1].replace(")", "").trim();
            if (RetiSupportate.contains("||"+Rete+"||"))return isValidAddress(address, Rete);
        }
        return false;
    }
    
        /**
         * Verifica se un indirizzo è sintatticamente valido per la rete indicata: formato Base58 a 32 byte per
         * Solana ({@link #isValidSolanaAddress}), formato specifico per Bitcoin ({@link Trans_Bitcoin#isValidBitcoinAddress}),
         * altrimenti formato esadecimale EVM standard ({@code 0x} seguito da 40 caratteri esadecimali).
         * @param address indirizzo da validare
         * @param Rete identificativo della blockchain/rete
         * @return {@code true} se l'indirizzo è valido per la rete indicata, {@code false} anche se {@code Rete} è {@code null}
         */
        public static boolean isValidAddress(String address,String Rete) {
        if (Rete==null) return false;
        if (Rete.equalsIgnoreCase("SOL")){
           // String BASE58_REGEX = "^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+$";
           // return address != null && address.length() == 44 && Pattern.matches(BASE58_REGEX, address);
            return isValidSolanaAddress(address);
        }
        else if (Rete.equalsIgnoreCase("BTC")){
            return Trans_Bitcoin.isValidBitcoinAddress(address);
        }
        else{
            Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
            return address != null && ETH_ADDRESS_PATTERN.matcher(address).matches();
        }
    }
        
            private static boolean isValidSolanaAddress(String address) {
        try {
            byte[] decoded = Base58.decode(address);
            return decoded.length == 32; // Gli indirizzi Solana devono essere di 32 byte decodificati
        } catch (Exception e) {
            return false;
        }
    }
}
