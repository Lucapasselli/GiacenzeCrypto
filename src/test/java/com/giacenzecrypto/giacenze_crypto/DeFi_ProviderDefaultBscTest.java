package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fissa il default di BSC, l'unico ramo di {@link Importazioni#DeFi_ProviderDefault} che legge
 * un'opzione salvata (la ApiKey Moralis) e che quindi {@link ProviderDefiDefaultTest} non può
 * verificare — quel file resta apposta senza database, sul modello di
 * {@link DeFi_ProviderBlockscoutProUrlTest}.
 *
 * <p>Gira su un database H2 temporaneo, come {@link DocumentiFonteTest}.
 */
class DeFi_ProviderDefaultBscTest {

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
    void nessunaChiaveMoralisSalvata() {
        DatabaseH2.Opzioni_Scrivi("ApiKey_Moralis", "");
    }

    @Test
    void senzaChiaveMoralisIlDefaultEeNodeReal() {
        //chi installa il programma oggi non può più ottenere una chiave Moralis gratuita (vedi
        //nocommit/Documentazione/Analisi_Provider_BSC.md): NodeReal è l'unica alternativa gratuita
        assertEquals(NodeRealDefi.PROVIDER, Importazioni.DeFi_ProviderDefault("BSC"));
        assertEquals(NodeRealDefi.PROVIDER, Importazioni.DeFi_ProviderDefault("bsc"));
    }

    @Test
    void conUnaChiaveMoralisGiaCompilataIlDefaultRestaMoralis() {
        //chi ha già una chiave Moralis funzionante non deve smettere di scaricare BSC in silenzio
        //solo perché il default per i nuovi utenti è cambiato
        DatabaseH2.Opzioni_Scrivi("ApiKey_Moralis", "chiave_di_prova_123");
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("BSC"));
        assertEquals("MORALIS", Importazioni.DeFi_ProviderDefault("bsc"));
    }
}
