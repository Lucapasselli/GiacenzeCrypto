package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Fissa il passaggio automatico dalla vecchia istanza Blockscout per-chain alla PRO API quando l'utente
 * ha salvato una ApiKey Blockscout, cosa che {@link ProviderDefiDefaultTest} non può verificare — quel
 * file resta apposta senza database.
 *
 * <p>Gira su un database H2 temporaneo, come {@link DocumentiFonteTest}.
 */
class DeFi_ProviderBlockscoutProUrlTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertEquals(true, DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void nessunaChiaveESSuNessunUrlPersonalizzato() {
        DatabaseH2.Opzioni_Scrivi("ApiKey_Blockscout", "");
        //senza azzerare anche PROVIDERDEFI, l'URL personalizzato scritto da
        //unUrlPersonalizzatoVinceAncheSullaProApi resterebbe per i test successivi: l'ordine dei
        //metodi @Test non è quello di dichiarazione
        DatabaseH2.ProviderDefi_CancellaTutti();
    }

    @Test
    void senzaChiaveRestaSullIstanzaPerChain() {
        assertEquals("https://eth.blockscout.com/api", Importazioni.DeFi_ProviderBlockscoutUrl("ETH"));
        assertEquals("https://base.blockscout.com/api", Importazioni.DeFi_ProviderBlockscoutUrl("BASE"));
        assertNull(Importazioni.DeFi_ProviderBlockscoutProUrl("ETH"));
    }

    @Test
    void conLaChiavePassaAllaProApiSulleChainVerificate() {
        DatabaseH2.Opzioni_Scrivi("ApiKey_Blockscout", "proapi_test123");
        //chain_id presi da Mappa_ChainExplorer/etherscan v2, verificati uno per uno con una chiave
        //reale il 13/09/2026
        assertEquals("https://api.blockscout.com/v2/api?chain_id=1", Importazioni.DeFi_ProviderBlockscoutUrl("ETH"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=42161", Importazioni.DeFi_ProviderBlockscoutUrl("ARB"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=8453", Importazioni.DeFi_ProviderBlockscoutUrl("BASE"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=137", Importazioni.DeFi_ProviderBlockscoutUrl("POL"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=100", Importazioni.DeFi_ProviderBlockscoutUrl("GNOSIS"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=10", Importazioni.DeFi_ProviderBlockscoutUrl("OP"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=57073", Importazioni.DeFi_ProviderBlockscoutUrl("INK"));
        assertEquals("https://api.blockscout.com/v2/api?chain_id=4663", Importazioni.DeFi_ProviderBlockscoutUrl("ROBINHOOD"));
    }

    @Test
    void conLaChiaveLeChainNonVerificateRestanoSulPerIstanza() {
        //la chiave Blockscout esiste, ma AVAX/CRO/BERA hanno risposto "Network not supported" sulla
        //PRO API: devono restare sui loro endpoint di sempre, non su un chain_id inventato
        DatabaseH2.Opzioni_Scrivi("ApiKey_Blockscout", "proapi_test123");
        assertEquals("https://api.routescan.io/v2/network/mainnet/evm/43114/etherscan/api",
                Importazioni.DeFi_ProviderBlockscoutUrl("AVAX"));
        assertEquals("https://explorer-api.cronos.org/mainnet/api/v2", Importazioni.DeFi_ProviderBlockscoutUrl("CRO"));
        assertNull(Importazioni.DeFi_ProviderBlockscoutProUrl("AVAX"));
        assertNull(Importazioni.DeFi_ProviderBlockscoutProUrl("CRO"));
        assertNull(Importazioni.DeFi_ProviderBlockscoutProUrl("BERA"));
        //BSC non è mai stata una chain Blockscout: né per-istanza né PRO
        assertNull(Importazioni.DeFi_ProviderBlockscoutUrl("BSC"));
    }

    @Test
    void unUrlPersonalizzatoVinceAncheSullaProApi() {
        //l'utente ha scelto un'istanza propria per questa chain: deve restare quella, chiave o no
        DatabaseH2.Opzioni_Scrivi("ApiKey_Blockscout", "proapi_test123");
        DatabaseH2.ProviderDefi_Scrivi("ETH", "BLOCKSCOUT", "https://mio-blockscout-privato.esempio/api");
        assertEquals("https://mio-blockscout-privato.esempio/api", Importazioni.DeFi_ProviderBlockscoutUrl("ETH"));
    }
}
