/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author lucap
 */
public class VarCondivise {

    //In questa mappa verranno memorizzati le info sui prezzi relative alle crypto del quadro RW
    //La chiave sarà Gruppo_Moneta_Timestamp
    static Map<String, Prezzi.InfoPrezzo> RW_MappaInfoPrezzo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    //=== VARIABILI RELATIVE AL FIAT E CARD WALLET DI CRYPTO.COM CONDIVISA
    static Map<String, String> CDC_FiatWallet_MappaTipiMovimenti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    //=== OPZIONI DI LOG VERBOSO (checkbox in Opzioni -> Varie, persistite in personale.mv.db)
    //Se true le risposte JSON scaricate dagli explorer durante l'importazione dei wallet DeFi
    //vengono stampate su System.out e quindi finiscono in GiacenzeCrypto.log tramite LoggerGC.
    public static boolean LogJsonDefi = false;
    //Come sopra, ma per le risposte JSON dei servizi di quotazione usati dal recupero prezzi.
    public static boolean LogJsonPrezzi = false;
    

    
    
    

    /**
     * Popola {@link Principale#Mappa_ChainExplorer} con la configurazione di ciascuna blockchain EVM/non-EVM
     * supportata (endpoint explorer, coin di commissione, nomi endpoint coingecko/DefiLlama), e
     * {@link Principale#Mappa_AddressRete_Nome} con gli indirizzi di contratto noti dei principali
     * stablecoin/token wrapped per rete, in modo da poterli identificare senza interrogare coingecko.
     * Da chiamare una volta all'avvio dell'applicazione. Per aggiungere una nuova chain EVM vedi
     * {@code Documentazione/IstruzioniVarie.txt}.
     */
    public static void CompilaMappaChain() {
        //indirizzoExplorer,api,coin commissioni,nomeEndpointCoingecko,nomeEndpointDefiLlama
        //System.out.println("Compilo Mappe integrate nel codice");
        String[] BSC = new String[]{"https://api.etherscan.io/v2/api?chainid=56", "", "BNB", "binance-smart-chain", "bsc"};
        String[] CRO = new String[]{"https://explorer-api.cronos.org/mainnet/api/v2", "", "CRO", "cronos", "cronos"};
        String[] ETH = new String[]{"https://api.etherscan.io/v2/api?chainid=1", "", "ETH", "ethereum", "ethereum"};
        String[] BASE = new String[]{"https://api.etherscan.io/v2/api?chainid=8453", "", "ETH", "base", "base"};
        String[] ARB = new String[]{"https://api.etherscan.io/v2/api?chainid=42161", "", "ETH", "arbitrum-one", "arbitrum"};
        String[] BERA = new String[]{"https://api.etherscan.io/v2/api?chainid=80094", "", "BERA", "berachain", "berachain"};
        String[] AVAX = new String[]{"https://api.etherscan.io/v2/api?chainid=43114", "", "AVAX", "avalanche", "avax"};
        String[] SOL = new String[]{"https://solscan.io/", "", "SOL", "solana", "solana"};
        String[] POL = new String[]{"https://api.etherscan.io/v2/api?chainid=137", "", "POL", "polygon-pos", "polygon"};
        String[] MONAD = new String[]{"https://api.etherscan.io/v2/api?chainid=143", "", "MON", "monad", "monad"};
        String[] BTC = new String[]{"https://mempool.space/api", "", "BTC", "bitcoin", "bitcoin"};
        String[] GNOSIS = new String[]{"https://api.etherscan.io/v2/api?chainid=100", "", "XDAI", "xdai", "xdai"};
        //Chain L2 con ETH come moneta di gas: nessuna moneta nuova da prezzare, vedi test/Documentazione/Analisi_Chain_Aggiungibili.md
        String[] LINEA = new String[]{"https://api.etherscan.io/v2/api?chainid=59144", "", "ETH", "linea", "linea"};
        String[] BLAST = new String[]{"https://api.etherscan.io/v2/api?chainid=81457", "", "ETH", "blast", "blast"};
        String[] UNICHAIN = new String[]{"https://api.etherscan.io/v2/api?chainid=130", "", "ETH", "unichain", "unichain"};
        String[] WORLD = new String[]{"https://api.etherscan.io/v2/api?chainid=480", "", "ETH", "world-chain", "world-chain"};
        String[] TAIKO = new String[]{"https://api.etherscan.io/v2/api?chainid=167000", "", "ETH", "taiko", "taiko"};
        String[] ABSTRACT = new String[]{"https://api.etherscan.io/v2/api?chainid=2741", "", "ETH", "abstract", "abstract"};
        String[] KATANA = new String[]{"https://api.etherscan.io/v2/api?chainid=747474", "", "ETH", "katana", "katana"};
        //Chain con moneta di gas propria: il prezzo di S e MNT passa dal percorso "simbolo" (exchange/CoinGecko)
        //e non da quello "address su chain", perché la moneta nativa non ha un indirizzo di contratto
        String[] SONIC = new String[]{"https://api.etherscan.io/v2/api?chainid=146", "", "S", "sonic", "sonic"};
        String[] MANTLE = new String[]{"https://api.etherscan.io/v2/api?chainid=5000", "", "MNT", "mantle", "mantle"};
        //OP non è nel piano gratuito di Etherscan V2, quindi il provider predefinito è Blockscout
        //(DeFi_ProviderDefault). Qui resta comunque l'endpoint Etherscan, come per GNOSIS: chi ha una
        //chiave a pagamento può riportare la chain su ETHERSCAN dalle preferenze e continua a funzionare
        String[] OP = new String[]{"https://api.etherscan.io/v2/api?chainid=10", "", "ETH", "optimistic-ethereum", "optimism"};
        Principale.Mappa_ChainExplorer.put("CRO", CRO);
        Principale.Mappa_ChainExplorer.put("BSC", BSC);
        Principale.Mappa_ChainExplorer.put("ETH", ETH);
        Principale.Mappa_ChainExplorer.put("BASE", BASE);
        Principale.Mappa_ChainExplorer.put("ARB", ARB);
        Principale.Mappa_ChainExplorer.put("SOL", SOL);
        Principale.Mappa_ChainExplorer.put("BTC", BTC);
        Principale.Mappa_ChainExplorer.put("BERA", BERA);
        Principale.Mappa_ChainExplorer.put("AVAX", AVAX);
        Principale.Mappa_ChainExplorer.put("POL", POL);
        Principale.Mappa_ChainExplorer.put("MONAD", MONAD);
        Principale.Mappa_ChainExplorer.put("GNOSIS", GNOSIS);
        Principale.Mappa_ChainExplorer.put("LINEA", LINEA);
        Principale.Mappa_ChainExplorer.put("BLAST", BLAST);
        Principale.Mappa_ChainExplorer.put("UNICHAIN", UNICHAIN);
        Principale.Mappa_ChainExplorer.put("WORLD", WORLD);
        Principale.Mappa_ChainExplorer.put("TAIKO", TAIKO);
        Principale.Mappa_ChainExplorer.put("ABSTRACT", ABSTRACT);
        Principale.Mappa_ChainExplorer.put("KATANA", KATANA);
        Principale.Mappa_ChainExplorer.put("SONIC", SONIC);
        Principale.Mappa_ChainExplorer.put("MANTLE", MANTLE);
        Principale.Mappa_ChainExplorer.put("OP", OP);
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

    /**
     * Popola {@link Principale#MappaRetiSupportate} con l'insieme dei codici rete/blockchain riconosciuti
     * dall'applicazione (incluse reti per cui non è disponibile un explorer DeFi integrato, come DASH o EOS).
     * Da chiamare una volta all'avvio dell'applicazione.
     */
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
        Principale.MappaRetiSupportate.put("GNOSIS", "");
        Principale.MappaRetiSupportate.put("LTC", "");
        Principale.MappaRetiSupportate.put("LUNA", "");
        Principale.MappaRetiSupportate.put("POL", "");
        Principale.MappaRetiSupportate.put("MONAD", "");
        Principale.MappaRetiSupportate.put("LINEA", "");
        Principale.MappaRetiSupportate.put("BLAST", "");
        Principale.MappaRetiSupportate.put("UNICHAIN", "");
        Principale.MappaRetiSupportate.put("WORLD", "");
        Principale.MappaRetiSupportate.put("TAIKO", "");
        Principale.MappaRetiSupportate.put("ABSTRACT", "");
        Principale.MappaRetiSupportate.put("KATANA", "");
        Principale.MappaRetiSupportate.put("SONIC", "");
        Principale.MappaRetiSupportate.put("MANTLE", "");
        Principale.MappaRetiSupportate.put("OP", "");
        Principale.MappaRetiSupportate.put("TRX", "");
        Principale.MappaRetiSupportate.put("SOL", "");
        Principale.MappaRetiSupportate.put("XLM", "");
        Principale.MappaRetiSupportate.put("XRP", "");
        Principale.MappaRetiSupportate.put("ZEC", "");
        //Funzione da scrivere
    }
   
    
    
    
}
