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
    
    
    
    
     public static void CompilaMappaChain(){
            //indirizzoExplorer,api,coin commissioni,nomeEndpointCoingecko
            //System.out.println("Compilo Mappe integrate nel codice");
            String BSC[]=new String[]{"https://api.etherscan.io/v2/api?chainid=56","","BNB","binance-smart-chain"};
            String CRO[]=new String[]{"https://api.etherscan.io/v2/api?chainid=25","","CRO","cronos"};
            String ETH[]=new String[]{"https://api.etherscan.io/v2/api?chainid=1","","ETH","ethereum"};           
            String BASE[]=new String[]{"https://api.etherscan.io/v2/api?chainid=8453","","ETH","base"};
            String ARB[]=new String[]{"https://api.etherscan.io/v2/api?chainid=42161","","ETH","arbitrum-one"};
            String BERA[]=new String[]{"https://api.etherscan.io/v2/api?chainid=80094","","BERA","berachain"};
            String AVAX[]=new String[]{"https://api.etherscan.io/v2/api?chainid=43114","","AVAX","avalanche"};
            
            String SOL[]=new String[]{"https://solscan.io/","","SOL","solana"};
    
            CDC_Grafica.Mappa_ChainExplorer.put("CRO", CRO);
            CDC_Grafica.Mappa_ChainExplorer.put("BSC", BSC);  
            CDC_Grafica.Mappa_ChainExplorer.put("ETH", ETH);
            CDC_Grafica.Mappa_ChainExplorer.put("BASE", BASE);
            CDC_Grafica.Mappa_ChainExplorer.put("ARB", ARB);
            CDC_Grafica.Mappa_ChainExplorer.put("SOL", SOL);
            CDC_Grafica.Mappa_ChainExplorer.put("BERA", BERA);
            CDC_Grafica.Mappa_ChainExplorer.put("AVAX", AVAX);
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x66e428c3f67a68878562e79A0234c1F83c208770_CRO", "USDT");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x55d398326f99059fF775485246999027B3197955_BSC", "USDT");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xc21223249CA28397B4B6541dfFaEcC539BfF0c59_CRO", "USDC");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xC74D59A548ecf7fc1754bb7810D716E9Ac3e3AE5_CRO", "BUSD");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x062E66477Faf219F25D27dCED647BF57C3107d52_CRO", "BTC");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xe44Fd7fCb2b1581822D0c862B68222998a0c299a_CRO", "ETH");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56_BSC", "BUSD");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xF2001B145b43032AAF5Ee2884e456CCd805F677D_CRO", "DAI");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x4200000000000000000000000000000000000006_BASE", "ETH");
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x6969696969696969696969696969696969696969_BERA", "BERA");//Sarebbe WBERA
            CDC_Grafica.Mappa_AddressRete_Nome.put("0x549943e04f40284185054145c6E4e9568C1D3241_BERA", "USDC");//Sarebbe USDC.e
            CDC_Grafica.Mappa_AddressRete_Nome.put("0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9_ARB", "USDT");//Sarebbe USDT0
            //0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9
            //0x549943e04f40284185054145c6E4e9568C1D3241
            //0x6969696969696969696969696969696969696969
            CDC_Grafica.Mappa_AddressRete_Nome.put("BNB_BSC", "BNB");
            
        }
     
     public static boolean ApriExplorer (String ID){
            

        if (CDC_Grafica.MappaCryptoWallet.get(ID).length < 24) {
            return false;
        }
        
        String IDTransazione = CDC_Grafica.MappaCryptoWallet.get(ID)[24];
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
                        Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
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
