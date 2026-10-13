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
     * {@link Principale#Mappa_AddressRete_Nome} (e {@link Principale#Mappa_MoneteStessoPrezzo}) con i token
     * noti da prezzare sugli exchange, letti da {@code config/varie/AliasPrezziToken.json} tramite
     * {@link AliasPrezziToken#Carica()}.
     * Da chiamare una volta all'avvio dell'applicazione. Una nuova chain EVM coperta da Etherscan V2 e' una
     * voce di {@code config/varie/Reti.json} (vedi {@link Reti}).
     */
    public static void CompilaMappaChain() {
        //Le reti stanno in config/varie/Reti.json (vedi Reti): una rete Etherscan V2 nuova non tocca piu' il codice
        Reti.CompilaMappaChain();
        AliasPrezziToken.Carica();
    }

    /**
     * Popola {@link Principale#MappaRetiSupportate} con l'insieme dei codici rete/blockchain riconosciuti
     * dall'applicazione (incluse reti per cui non è disponibile un explorer DeFi integrato, come DASH o EOS).
     * Da chiamare una volta all'avvio dell'applicazione.
     */
    public static void CompilaMappaRetiSupportate() {
        Reti.CompilaMappaRetiSupportate();
    }
   
    
    
    
}
