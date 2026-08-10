package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Fissa il provider predefinito di ogni chain e l'endpoint Blockscout associato.
 * <p>Sono le due funzioni pure della catena provider: {@code DeFi_ProviderEffettivo} legge invece la
 * tabella PROVIDERDEFI e quindi qui non è verificabile senza database. Il caso che interessa di più è
 * GNOSIS, spostato su Blockscout perché dal 01/09/2026 esce dal piano gratuito di Etherscan V2.
 */
public class ProviderDefiDefaultTest {

    @Test
    public void gnosisUsaBlockscoutConIlSuoEndpointPubblico() {
        assertEquals("BLOCKSCOUT", Importazioni.DeFi_ProviderDefault("GNOSIS"));
        assertEquals("BLOCKSCOUT", Importazioni.DeFi_ProviderDefault("gnosis"));
    }

    @Test
    public void leChainNonGratuiteSuEtherscanNonRestanoSuEtherscan() {
        //BSC, Base e Avalanche sono fuori dal piano gratuito di Etherscan V2 e vanno su Moralis
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("BSC"));
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("BASE"));
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("AVAX"));
    }

    @Test
    public void leChainNonEvmHannoIlLoroProvider() {
        assertEquals("HELIUS", Importazioni.DeFi_ProviderDefault("SOL"));
        assertEquals("BITCOIN", Importazioni.DeFi_ProviderDefault("BTC"));
    }

    @Test
    public void cronosRestaSuBlockscoutEIlRestoSuEtherscan() {
        //Cronos usa Blockscout perché Cronoscan non accetta un blocco di partenza
        assertEquals("BLOCKSCOUT", Importazioni.DeFi_ProviderDefault("CRO"));
        assertEquals("ETHERSCAN", Importazioni.DeFi_ProviderDefault("ETH"));
        assertEquals("ETHERSCAN", Importazioni.DeFi_ProviderDefault("ARB"));
        assertEquals("ETHERSCAN", Importazioni.DeFi_ProviderDefault("POL"));
        assertEquals("ETHERSCAN", Importazioni.DeFi_ProviderDefault("BERA"));
        assertEquals("ETHERSCAN", Importazioni.DeFi_ProviderDefault("MONAD"));
    }

    @Test
    public void ogniChainConDefaultBlockscoutHaUnEndpointConfigurato() {
        //Senza URL l'importazione si ferma con un messaggio in log: un default BLOCKSCOUT senza
        //endpoint sarebbe una chain che non scarica più nulla
        for (String Rete : new String[]{"GNOSIS", "CRO"}) {
            String Url = Importazioni.DeFi_ProviderBlockscoutUrl(Rete);
            assertNotNull(Url, "manca l'URL Blockscout per " + Rete);
            assertTrue(Url.startsWith("https://"), "URL Blockscout non valido per " + Rete + ": " + Url);
        }
    }
}
