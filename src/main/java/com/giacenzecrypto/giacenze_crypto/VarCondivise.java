/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

/**
 *
 * @author lucap
 */
public class VarCondivise {

    public static void CompilaMappaChain() {
        //indirizzoExplorer,api,coin commissioni,nomeEndpointCoingecko
        //System.out.println("Compilo Mappe integrate nel codice");
        String[] BSC = new String[]{"https://api.etherscan.io/v2/api?chainid=56", "", "BNB", "binance-smart-chain"};
        String[] CRO = new String[]{"https://api.etherscan.io/v2/api?chainid=25", "", "CRO", "cronos"};
        String[] ETH = new String[]{"https://api.etherscan.io/v2/api?chainid=1", "", "ETH", "ethereum"};
        String[] BASE = new String[]{"https://api.etherscan.io/v2/api?chainid=8453", "", "ETH", "base"};
        String[] ARB = new String[]{"https://api.etherscan.io/v2/api?chainid=42161", "", "ETH", "arbitrum-one"};
        String[] BERA = new String[]{"https://api.etherscan.io/v2/api?chainid=80094", "", "BERA", "berachain"};
        String[] AVAX = new String[]{"https://api.etherscan.io/v2/api?chainid=43114", "", "AVAX", "avalanche"};
        String[] SOL = new String[]{"https://solscan.io/", "", "SOL", "solana"};
        Principale.Mappa_ChainExplorer.put("CRO", CRO);
        Principale.Mappa_ChainExplorer.put("BSC", BSC);
        Principale.Mappa_ChainExplorer.put("ETH", ETH);
        Principale.Mappa_ChainExplorer.put("BASE", BASE);
        Principale.Mappa_ChainExplorer.put("ARB", ARB);
        Principale.Mappa_ChainExplorer.put("SOL", SOL);
        Principale.Mappa_ChainExplorer.put("BERA", BERA);
        Principale.Mappa_ChainExplorer.put("AVAX", AVAX);
        Principale.Mappa_AddressRete_Nome.put("0x66e428c3f67a68878562e79A0234c1F83c208770_CRO", "USDT");
        Principale.Mappa_AddressRete_Nome.put("0x55d398326f99059fF775485246999027B3197955_BSC", "USDT");
        Principale.Mappa_AddressRete_Nome.put("0xc21223249CA28397B4B6541dfFaEcC539BfF0c59_CRO", "USDC");
        Principale.Mappa_AddressRete_Nome.put("0xC74D59A548ecf7fc1754bb7810D716E9Ac3e3AE5_CRO", "BUSD");
        Principale.Mappa_AddressRete_Nome.put("0x062E66477Faf219F25D27dCED647BF57C3107d52_CRO", "BTC");
        Principale.Mappa_AddressRete_Nome.put("0xe44Fd7fCb2b1581822D0c862B68222998a0c299a_CRO", "ETH");
        Principale.Mappa_AddressRete_Nome.put("0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56_BSC", "BUSD");
        Principale.Mappa_AddressRete_Nome.put("0xF2001B145b43032AAF5Ee2884e456CCd805F677D_CRO", "DAI");
        Principale.Mappa_AddressRete_Nome.put("0x4200000000000000000000000000000000000006_BASE", "ETH");
        Principale.Mappa_AddressRete_Nome.put("0x6969696969696969696969696969696969696969_BERA", "BERA"); //Sarebbe WBERA
        Principale.Mappa_AddressRete_Nome.put("0x549943e04f40284185054145c6E4e9568C1D3241_BERA", "USDC"); //Sarebbe USDC.e
        Principale.Mappa_AddressRete_Nome.put("0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9_ARB", "USDT"); //Sarebbe USDT0
        //0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9
        //0x549943e04f40284185054145c6E4e9568C1D3241
        //0x6969696969696969696969696969696969696969
        //Principale.Mappa_AddressRete_Nome.put("BNB_BSC", "BNB");
    }

    public static void CompilaMappaRetiSupportate() {
        Principale.MappaRetiSupportate.put("ARB", "");
        Principale.MappaRetiSupportate.put("AVAX", "");
        Principale.MappaRetiSupportate.put("BASE", "");
        Principale.MappaRetiSupportate.put("BERA", "");
        Principale.MappaRetiSupportate.put("ADA", "");
        Principale.MappaRetiSupportate.put("BNB", "");
        Principale.MappaRetiSupportate.put("BSC", "");
        Principale.MappaRetiSupportate.put("BTC", "");
        Principale.MappaRetiSupportate.put("CRO", "");
        Principale.MappaRetiSupportate.put("DASH", "");
        Principale.MappaRetiSupportate.put("DOGE", "");
        Principale.MappaRetiSupportate.put("DOT", "");
        Principale.MappaRetiSupportate.put("EOS", "");
        Principale.MappaRetiSupportate.put("ETH", "");
        Principale.MappaRetiSupportate.put("FTM", "");
        Principale.MappaRetiSupportate.put("LTC", "");
        Principale.MappaRetiSupportate.put("LUNA", "");
        Principale.MappaRetiSupportate.put("MATIC", "");
        Principale.MappaRetiSupportate.put("TRX", "");
        Principale.MappaRetiSupportate.put("SOL", "");
        Principale.MappaRetiSupportate.put("XLM", "");
        Principale.MappaRetiSupportate.put("XRP", "");
        Principale.MappaRetiSupportate.put("ZEC", "");
        //Funzione da scrivere
    }
   
    
    
    
}
