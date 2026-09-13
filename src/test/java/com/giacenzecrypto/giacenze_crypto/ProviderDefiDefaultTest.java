package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        //BSC, BASE, AVAX e OP sono tutte fuori dal piano gratuito di Etherscan V2. Due delle quattro
        //hanno un explorer Etherscan-compatibile pubblico e usabile senza chiave.
        assertEquals("BLOCKSCOUT", Importazioni.DeFi_ProviderDefault("OP"));
        assertEquals("https://optimism.blockscout.com/api", Importazioni.DeFi_ProviderBlockscoutUrl("OP"));
        //BASE ha l'istanza Blockscout ufficiale ma NON e' un default: dal 2026 le istanze ospitate su
        //blockscout.com danno 10 richieste all'ora per IP senza chiave, e un solo wallet ne consuma
        //almeno cinque. L'endpoint resta configurato, cosi' la scelta manuale (con la ApiKey
        //Blockscout gratuita) funziona.
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("BASE"));
        assertEquals("https://base.blockscout.com/api", Importazioni.DeFi_ProviderBlockscoutUrl("BASE"));
        //AVAX non ha un'istanza Blockscout (la chain 43114 non è nel registro chains.blockscout.com):
        //l'endpoint Etherscan-compatibile è Routescan. Il nome del provider resta "BLOCKSCOUT" perché
        //nel programma vuol dire "explorer Etherscan-compatibile diverso da Etherscan".
        assertEquals("BLOCKSCOUT", Importazioni.DeFi_ProviderDefault("AVAX"));
        assertEquals("https://api.routescan.io/v2/network/mainnet/evm/43114/etherscan/api",
                Importazioni.DeFi_ProviderBlockscoutUrl("AVAX"));
    }

    @Test
    public void bscRestaSuMoralisPerchePerLa56NonCeNienteDiGratuito() {
        //Verificato il 13/09/2026: nessuna istanza Blockscout pubblica per la chain 56, Routescan
        //risponde "chain not supported", Etherscan V2 risponde "Free API access is not supported for
        //this chain". Se un giorno ne comparisse una, questo test è il punto da cui accorgersi che la
        //riga su Moralis non serve più.
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("BSC"));
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("bsc"));
        assertNull(Importazioni.DeFi_ProviderBlockscoutUrl("BSC"));
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
    public void laFinestraSenzaChiaveSiRiconosceDalResetLungo() {
        //Misurato il 13/09/2026: senza chiave la finestra è di almeno 44 minuti (2.647.368 ms
        //osservati), con la ApiKey Blockscout sulla PRO API è di circa un secondo. La soglia sta
        //comodamente fra i due.
        assertTrue(Importazioni.Explorer_FinestraSenzaChiaveEsaurita("2647368", false),
                "44 minuti senza chiave devono essere riconosciuti come finestra lunga");
        assertFalse(Importazioni.Explorer_FinestraSenzaChiaveEsaurita("973", false),
                "meno di un secondo (PRO API) non deve fermare l'importazione");
        //stessa finestra lunga, ma la chiave è già in uso per questa richiesta: non ha senso
        //suggerire di inserirla, e in pratica con la chiave la PRO API non dà mai un reset così lungo
        assertFalse(Importazioni.Explorer_FinestraSenzaChiaveEsaurita("2647368", true),
                "con una chiave già usata il messaggio \"inseriscila\" non avrebbe senso");
        //nessun header, o un valore non numerico: nessuna base per decidere, si ricade sul retry normale
        assertFalse(Importazioni.Explorer_FinestraSenzaChiaveEsaurita(null, false));
        assertFalse(Importazioni.Explorer_FinestraSenzaChiaveEsaurita("non un numero", false));
        assertFalse(Importazioni.Explorer_FinestraSenzaChiaveEsaurita("", false));
    }

    @Test
    public void ogniChainConDefaultBlockscoutHaUnEndpointConfigurato() {
        //Senza URL l'importazione si ferma con un messaggio in log: un default BLOCKSCOUT senza
        //endpoint sarebbe una chain che non scarica più nulla.
        //L'elenco non è scritto a mano ma ricavato dalle chain configurate, così una chain futura
        //messa su Blockscout e dimenticata qui viene trovata da questo test invece che dall'utente
        VarCondivise.CompilaMappaChain();
        int Controllate = 0;
        for (String Rete : Principale.Mappa_ChainExplorer.keySet()) {
            if (!"BLOCKSCOUT".equals(Importazioni.DeFi_ProviderDefault(Rete))) continue;
            Controllate++;
            String Url = Importazioni.DeFi_ProviderBlockscoutUrl(Rete);
            assertNotNull(Url, "manca l'URL Blockscout per " + Rete);
            assertTrue(Url.startsWith("https://"), "URL Blockscout non valido per " + Rete + ": " + Url);
        }
        assertTrue(Controllate >= 4, "attese almeno CRO, GNOSIS, OP e AVAX con default Blockscout, trovate " + Controllate);
    }
}
