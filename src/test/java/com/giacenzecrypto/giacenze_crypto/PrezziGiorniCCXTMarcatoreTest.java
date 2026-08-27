package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copre il marcatore persistente {@code PrezziGiorniCCXT} (tabella in {@code prezzi.mv.db}) e la
 * classificazione delle fonti prezzo, introdotti il 2026-08-27 in {@link Prezzi} per non riscaricare
 * l'intera giornata dagli exchange piu' di una volta per coppia (moneta, giorno).
 *
 * <p>Il rischio specifico coperto: {@link Prezzi#GiornoCCXT_Leggi}/{@link Prezzi#GiornoCCXT_Scrivi}
 * non vengono mai eseguiti dal resto della suite (ogni percorso di prezzo ritorna prima, e lo
 * scaricamento vero richiede Node, assente nei test). Un errore nella {@code MERGE} o un disallineamento
 * di case sul simbolo si manifesterebbe solo a runtime come "riscarica la stessa giornata all'infinito",
 * silenzioso e costoso.
 */
class PrezziGiorniCCXTMarcatoreTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @Test
    void marcatore_scritturaERilettura_roundTrip() {
        assertFalse(Prezzi.GiornoCCXT_Leggi("BTC", 20260215),
                "un giorno mai scritto non deve risultare marcato");

        Prezzi.GiornoCCXT_Scrivi("BTC", 20260215);

        assertTrue(Prezzi.GiornoCCXT_Leggi("BTC", 20260215),
                "dopo Scrivi il giorno deve risultare marcato");
        assertFalse(Prezzi.GiornoCCXT_Leggi("BTC", 20260216),
                "il marcatore e' per giorno: un altro giorno resta non marcato");
        assertFalse(Prezzi.GiornoCCXT_Leggi("ETH", 20260215),
                "il marcatore e' per simbolo: un altro simbolo resta non marcato");
    }

    @Test
    void marcatore_scritturaRipetuta_nonEsplode() {
        Prezzi.GiornoCCXT_Scrivi("SOL", 20250101);
        Prezzi.GiornoCCXT_Scrivi("SOL", 20250101);//MERGE: la seconda non deve fallire sulla PK
        assertTrue(Prezzi.GiornoCCXT_Leggi("SOL", 20250101));
    }

    @Test
    void marcatore_simboloCaseInsensitive() {
        Prezzi.GiornoCCXT_Scrivi("doge", 20250630);
        assertTrue(Prezzi.GiornoCCXT_Leggi("DOGE", 20250630),
                "Scrivi e Leggi normalizzano il simbolo in maiuscolo: minuscolo e maiuscolo coincidono");
    }

    @Test
    void fonteEDaExchangeCCXT_riconosceI7ExchangeENonLeFontiDiRipiego() {
        for (String ex : Prezzi.EXCHANGES_CCXT.split(",")) {
            assertTrue(Prezzi.fonteEDaExchangeCCXT(ex), ex + " deve contare come exchange CCXT");
            assertTrue(Prezzi.fonteEDaExchangeCCXT(ex.toUpperCase()), ex + " (maiuscolo) idem");
        }
        assertFalse(Prezzi.fonteEDaExchangeCCXT("CoinMarketCap"));
        assertFalse(Prezzi.fonteEDaExchangeCCXT("GC"));
        assertFalse(Prezzi.fonteEDaExchangeCCXT("giacenzecrypto.it"));
        assertFalse(Prezzi.fonteEDaExchangeCCXT("DB Interno (Old)"));
        assertFalse(Prezzi.fonteEDaExchangeCCXT("bancaditalia"));
        assertFalse(Prezzi.fonteEDaExchangeCCXT(""));
        assertFalse(Prezzi.fonteEDaExchangeCCXT(null));
        assertFalse(Prezzi.fonteEDaExchangeCCXT("okx (Wallet 01)"),
                "una fonte di gruppo wallet non e' un id exchange puro");
    }
}
