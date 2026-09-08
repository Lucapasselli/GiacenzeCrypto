package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DatiPredefinitiRW} (parse del JSON dei predefiniti, copia nel jar) e il reconcile
 * {@link Principale_GruppiWalletRW#Pers_RW_SeminaERiconcilia()} : semina su DB vuoto, rispetto delle
 * righe {@code UTENTE}, hash stabile. DB H2 temporaneo come {@link DatabaseH2_RwConfigTest}.
 */
class DatiPredefinitiRWTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @Test
    void carica_dalJar_haI14ExchangeENoteBinance() {
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        assertNotNull(p, "RW_Predefiniti.json deve essere leggibile (copia nel jar)");
        assertEquals(14, p.exchange().size());
        assertNotNull(p.exchangePerId("BINANCE"), "lookup case-insensitive");
        assertEquals("Wallet 101", p.exchangePerId("binance").gruppo);
        // hash stabile fra due letture
        assertEquals(p.hash(), DatiPredefinitiRW.Carica().hash());
        assertFalse(p.hash().isBlank());
    }

    @Test
    void interpreta_jsonNonValido_ritornaNull() {
        assertNull(DatiPredefinitiRW.Interpreta("non e' json", "prova"));
        assertNull(DatiPredefinitiRW.Interpreta("{\"versione\":\"x\"}", "prova"), "senza exchange = non valido");
    }

    @Test
    void reconcile_haSeminatoLAnagraficaComeSistema() {
        // Il reconcile è già girato in CreaoCollegaDatabase().
        String[] binance = DatabaseH2.Pers_ExchangeAnagrafica_Leggi("binance");
        assertEquals("binance", binance[0]);
        assertEquals(Principale_GruppiWalletRW.ORIGINE_SISTEMA, binance[7]);
        assertEquals(Principale_GruppiWalletRW.ORIGINE_SISTEMA,
                DatabaseH2.Pers_GruppoAlias_Leggi("Wallet 101")[3]);
        // periodi di detenzione Binance predefiniti (bollo fino al 30/06/2026, poi NO)
        List<String[]> per = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 101");
        assertEquals(2, per.size());
        assertEquals(Principale_GruppiWalletRW.ORIGINE_SISTEMA, per.get(0)[13]);
        assertEquals("binance-crypto-bollo", per.get(0)[14]);
    }

    @Test
    void reconcile_nonToccaUnaRigaUtente() {
        // marco Coinbase come personalizzato, poi forzo un nuovo giro di reconcile
        DatabaseH2.Pers_ExchangeAnagrafica_ScriviNome("coinbase", "Coinbase (mio nome)",
                Principale_GruppiWalletRW.ORIGINE_UTENTE);
        DatabaseH2.Pers_Opzioni_Scrivi(Principale_GruppiWalletRW.OPZIONE_HASH_PREDEFINITI, "hash-vecchio");
        Principale_GruppiWalletRW.Pers_RW_SeminaERiconcilia();

        assertEquals("Coinbase (mio nome)", DatabaseH2.Pers_ExchangeAnagrafica_Leggi("coinbase")[1],
                "una riga UTENTE non deve essere sovrascritta dal reconcile");
        // e l'hash è stato riscritto a quello corrente
        assertEquals(DatiPredefinitiRW.Carica().hash(),
                DatabaseH2.Pers_Opzioni_Leggi(Principale_GruppiWalletRW.OPZIONE_HASH_PREDEFINITI));
    }
}
