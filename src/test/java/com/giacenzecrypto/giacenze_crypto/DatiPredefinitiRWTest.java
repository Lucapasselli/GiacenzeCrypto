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
    void reconcile_haSeminatoIGruppiComeSistema() {
        // Il reconcile è già girato in CreaoCollegaDatabase().
        assertEquals(Principale_GruppiWalletRW.ORIGINE_SISTEMA,
                DatabaseH2.Pers_GruppoAlias_Leggi("Wallet 101")[3]);
        // periodi di detenzione Binance predefiniti (bollo fino al 30/06/2026, poi NO)
        List<String[]> per = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 101");
        assertEquals(2, per.size());
        assertEquals(Principale_GruppiWalletRW.ORIGINE_SISTEMA, per.get(0)[13]);
        assertEquals("binance-crypto-bollo", per.get(0)[14]);
    }

    @Test
    void reconcile_iPeriodiFiscaliDellExchangeDiventanoRighiFiatDelSuoGruppo() {
        // Coinbase : due righi FIAT sul suo gruppo, con i dati fiscali delle due entità legali
        List<String[]> per = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 102");
        assertEquals(2, per.size());
        for (String[] r : per) {
            assertEquals("FIAT", r[2]);
        }
        // ordinati per progressivo : prima l'entità irlandese, poi quella lussemburghese
        assertEquals("2025-06-19", per.get(0)[5]);
        assertEquals("040", per.get(0)[15]);
        assertEquals("2025-06-20", per.get(1)[4]);
        assertEquals("092", per.get(1)[15]);
        assertEquals("LU36476644", per.get(1)[16]);
        assertEquals(Principale_GruppiWalletRW.MOD_SOLO_RESIDUO, per.get(1)[10]);
        assertNull(per.get(1)[19], "l'identificativo ISEE non si semina mai");
    }

    @Test
    void reconcile_nonToccaUnaRigaUtente() {
        // marco il periodo di Kraken come personalizzato, poi forzo un nuovo giro di reconcile.
        // Kraken e non Coinbase apposta : così questo test non tocca il gruppo su cui lavora
        // reconcile_iPeriodiFiscaliDellExchangeDiventanoRighiFiatDelSuoGruppo, qualunque sia
        // l'ordine con cui JUnit esegue i metodi.
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 103", "FIAT", 1, "2025-06-25", null,
                null, null, null, null, "SOLO_RESIDUO", "SOLO_RESIDUO", null,
                Principale_GruppiWalletRW.ORIGINE_UTENTE, "kraken-mica",
                "040", "P.IVA mia", "nota mia", "fonte mia", "E-mio-ISEE", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi(Principale_GruppiWalletRW.OPZIONE_HASH_PREDEFINITI, "hash-vecchio");
        Principale_GruppiWalletRW.Pers_RW_SeminaERiconcilia();

        String[] r = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 103").get(0);
        assertEquals("P.IVA mia", r[16], "una riga UTENTE non deve essere sovrascritta dal reconcile");
        assertEquals("E-mio-ISEE", r[19]);
        // e l'hash è stato riscritto a quello corrente
        assertEquals(DatiPredefinitiRW.Carica().hash(),
                DatabaseH2.Pers_Opzioni_Leggi(Principale_GruppiWalletRW.OPZIONE_HASH_PREDEFINITI));
    }
}
