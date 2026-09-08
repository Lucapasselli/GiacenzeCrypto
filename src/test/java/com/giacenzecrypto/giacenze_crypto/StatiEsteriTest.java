package com.giacenzecrypto.giacenze_crypto;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La tabella dei codici Stato estero ({@link StatiEsteri}) : coerenza interna (codici a 3 cifre,
 * niente duplicati) e round-trip codice ⇄ etichetta della combo.
 */
class StatiEsteriTest {

    @Test
    void elenco_codiciATreCifreSenzaDuplicati() {
        Set<String> codici = new HashSet<>();
        Set<String> nomi = new HashSet<>();
        for (String[] r : StatiEsteri.ELENCO) {
            assertEquals(2, r.length);
            assertTrue(r[0].matches("\\d{3}"), "codice non a 3 cifre: " + r[0]);
            assertFalse(r[1].isBlank(), "nome vuoto per " + r[0]);
            assertTrue(codici.add(r[0]), "codice duplicato: " + r[0]);
            assertTrue(nomi.add(r[1]), "nome duplicato: " + r[1]);
        }
        assertTrue(StatiEsteri.ELENCO.length > 240, "elenco troppo corto: " + StatiEsteri.ELENCO.length);
        // alcuni riferimenti noti della Tabella 10
        assertEquals("LUSSEMBURGO", StatiEsteri.nome("092"));
        assertEquals("MALTA", StatiEsteri.nome("105"));
        assertEquals("IRLANDA", StatiEsteri.nome("040"));
        assertEquals("AUSTRIA", StatiEsteri.nome("008"));
    }

    @Test
    void etichetta_eCodiceDaEtichetta_sonoInverse() {
        for (String[] r : StatiEsteri.ELENCO) {
            assertEquals(r[0], StatiEsteri.codiceDaEtichetta(StatiEsteri.etichetta(r[0])));
        }
        assertEquals("", StatiEsteri.codiceDaEtichetta(StatiEsteri.VOCE_VUOTA));
        assertEquals("", StatiEsteri.codiceDaEtichetta(""));
        // valore legacy non in tabella : sopravvive
        assertEquals("999", StatiEsteri.codiceDaEtichetta(StatiEsteri.etichetta("999")));
    }

    @Test
    void etichetteCombo_primaVoceVuota_eValoreLegacyAggiunto() {
        String[] senzaLegacy = StatiEsteri.etichetteCombo("");
        assertEquals(StatiEsteri.VOCE_VUOTA, senzaLegacy[0]);
        assertEquals(StatiEsteri.ELENCO.length + 1, senzaLegacy.length);

        String[] conLegacy = StatiEsteri.etichetteCombo("999");
        assertEquals(StatiEsteri.ELENCO.length + 2, conLegacy.length);
        assertEquals("999 (non in elenco)", conLegacy[conLegacy.length - 1]);
    }
}
