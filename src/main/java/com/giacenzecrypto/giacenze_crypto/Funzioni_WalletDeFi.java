/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bitcoinj.core.Base58;

/**
 *
 * @author luca.passelli
 */
public class Funzioni_WalletDeFi {
    
    
    
    
     
     public static boolean ApriExplorer (String ID){
            

        if (Principale.MappaCryptoWallet.get(ID).length < 24) {
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

                    if (Rete.equalsIgnoreCase("BSC")) {
                        Funzioni.ApriWeb("https://bscscan.com/tx/" + IDTransazione);
                    } else if (Rete.equalsIgnoreCase("CRO")) {
                        Funzioni.ApriWeb("https://cronoscan.com/tx/" + IDTransazione);
                    } else if (Rete.equalsIgnoreCase("ETH")) {
                        Funzioni.ApriWeb("https://etherscan.io/tx/" + IDTransazione);
                    } else if (Rete.equalsIgnoreCase("BASE")){
                        Funzioni.ApriWeb("https://basescan.org/tx/" + IDTransazione);
                    } else if (Rete.equalsIgnoreCase("ARB")){
                        Funzioni.ApriWeb("https://arbiscan.io/tx/" + IDTransazione);
                    }else if (Rete.equalsIgnoreCase("SOL")){
                        Funzioni.ApriWeb("https://solscan.io/tx/" + IDTransazione);
                    }else if (Rete.equalsIgnoreCase("BERA")){
                        Funzioni.ApriWeb("https://berascan.com/tx/" + IDTransazione);
                    }else if (Rete.equalsIgnoreCase("AVAX")){
                        Funzioni.ApriWeb("https://avascan.info/blockchain/c/tx/" + IDTransazione);
                    }
                    
        }
        return true;

    }
     
     public static void ApriMovimentiWallet(String Wallet,String Address,String Rete){
           
            
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        if (Rete.equalsIgnoreCase("BSC")){
                            Desktop.getDesktop().browse(new URI("https://bscscan.com/token/"+Address +"?a="+ Wallet));
                           }
                        else if(Rete.equalsIgnoreCase("CRO")){
                           Desktop.getDesktop().browse(new URI("https://cronoscan.com/token/"+Address +"?a="+ Wallet)); 
                        }
                        else if(Rete.equalsIgnoreCase("ETH")){
                           Desktop.getDesktop().browse(new URI("https://etherscan.io/token/"+Address +"?a="+ Wallet)); 
                        }
                        else if(Rete.equalsIgnoreCase("BASE")){
                           Desktop.getDesktop().browse(new URI("https://basescan.org/token/"+Address +"?a="+ Wallet)); 
                        }
                        else if(Rete.equalsIgnoreCase("ARB")){
                           Desktop.getDesktop().browse(new URI("https://arbiscan.io/token/"+Address +"?a="+ Wallet)); 
                        }
                        else if(Rete.equalsIgnoreCase("SOL")){
                           Desktop.getDesktop().browse(new URI("https://solscan.io/token/"+Address +"?a="+ Wallet)); 
                        }
                        else if(Rete.equalsIgnoreCase("BERA")){
                           Desktop.getDesktop().browse(new URI("https://berascan.com/token/"+Address +"?a="+ Wallet)); 
                        }
                        
                    } catch (URISyntaxException | IOException ex) {
                        Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        
     }
     
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
                    }
        
     }
     
     
     public static boolean isValidDefiWallet(String wallet) {
        //Questa funzione serve per sapere se una stringa wallet presente nella colonna wallet es 0x3423432aff4545 (ETH)
        //può essere considerata un wallet valido, si controllerà quindi l'indirizzo e se la rete è supportata
        String RetiSupportate="||BSC||CRO||BASE||ARB||ETH||SOL||BERA||AVAX||";
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
    
        public static boolean isValidAddress(String address,String Rete) {
        if (Rete==null) return false;
        if (Rete.equalsIgnoreCase("SOL")){
           // String BASE58_REGEX = "^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+$";
           // return address != null && address.length() == 44 && Pattern.matches(BASE58_REGEX, address);
            return isValidSolanaAddress(address);
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
