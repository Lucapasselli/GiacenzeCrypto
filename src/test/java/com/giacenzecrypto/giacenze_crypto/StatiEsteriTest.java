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
    void etichetteCombo_primaVoceVuota_poiItalia_eValoreLegacyAggiunto() {
        String[] senzaLegacy = StatiEsteri.etichetteCombo("");
        assertEquals(StatiEsteri.VOCE_VUOTA, senzaLegacy[0]);
        assertTrue(senzaLegacy[1].startsWith(StatiEsteri.CODICE_ITALIA), "la 2a voce e' l'Italia");
        assertEquals(StatiEsteri.ELENCO.length + 2, senzaLegacy.length); // vuota + Italia + elenco

        String[] conLegacy = StatiEsteri.etichetteCombo("999");
        assertEquals(StatiEsteri.ELENCO.length + 3, conLegacy.length);
        assertEquals("999 (non in elenco)", conLegacy[conLegacy.length - 1]);
    }

    @Test
    void italia_sentinella_roundTripEIsItalia() {
        assertTrue(StatiEsteri.isItalia(StatiEsteri.CODICE_ITALIA));
        assertTrue(StatiEsteri.isItalia(" " + StatiEsteri.CODICE_ITALIA + " "));
        assertFalse(StatiEsteri.isItalia("040"));
        assertFalse(StatiEsteri.isItalia(""));
        // la voce Italia sopravvive al giro etichetta -> codice
        assertEquals(StatiEsteri.CODICE_ITALIA,
                StatiEsteri.codiceDaEtichetta(StatiEsteri.etichetta(StatiEsteri.CODICE_ITALIA)));
    }

    // ------------------------------------------------------------------
    // Elenco del D.M. 4 maggio 1999 (fiscalità privilegiata)
    // ------------------------------------------------------------------

    /**
     * Un codice inventato o storpiato nell'elenco sarebbe invisibile: non fa fallire nulla, applica
     * semplicemente l'aliquota ordinaria a uno Stato che ne vorrebbe una doppia. Questo test è
     * l'unica difesa contro un refuso.
     */
    @Test
    void ogniCodicePrivilegiatoEsisteNellaTabella10() {
        java.util.Set<String> noti = new java.util.HashSet<>();
        for (String[] v : StatiEsteri.ELENCO) noti.add(v[0]);
        for (String c : StatiEsteri.codiciPrivilegiati()) {
            assertTrue(noti.contains(c), "codice " + c + " assente dalla Tabella 10");
        }
    }

    /**
     * L'aritmetica: 55 voci pubblicate, meno 3 senza codice nella Tabella 10 (Alderney, Sark,
     * Antille Olandesi) = 52 voci mappabili; gli Emirati Arabi Uniti valgono però 7 codici invece di
     * 1, quindi 52 - 1 + 7 = <b>58</b>.
     */
    @Test
    void elencoPrivilegiati_58CodiciPerLe55VociPubblicate() {
        assertEquals(58, StatiEsteri.codiciPrivilegiati().size());
    }

    /** Gli Emirati Arabi Uniti sono sette codici: dimenticarne uno dimezzerebbe l'imposta. */
    @Test
    void emiratiArabiUniti_tuttiESetteICodici() {
        for (String c : new String[] {"238", "239", "240", "241", "242", "243", "244"}) {
            assertTrue(StatiEsteri.isPrivilegiato(c), "emirato " + c + " non riconosciuto");
        }
    }

    @Test
    void isPrivilegiato_statiComuniNonSonoNellElenco() {
        assertFalse(StatiEsteri.isPrivilegiato("092"));   // LUSSEMBURGO
        assertFalse(StatiEsteri.isPrivilegiato("037"));   // SAN MARINO : tolto dall'elenco nel 2014
        assertFalse(StatiEsteri.isPrivilegiato("105"));   // MALTA
        assertFalse(StatiEsteri.isPrivilegiato(""));
        assertFalse(StatiEsteri.isPrivilegiato(null));
        assertFalse(StatiEsteri.isPrivilegiato(StatiEsteri.CODICE_ITALIA));
    }

    /** La Svizzera è uscita dall'elenco proprio dall'anno in cui la maggiorazione decorre. */
    @Test
    void svizzera_nonPiuNellElencoValidoDal2024() {
        assertFalse(StatiEsteri.isPrivilegiato("071"), "codice della Svizzera nella Tabella 10");
    }

    @Test
    void isPrivilegiato_ignoraGliSpaziAttorno() {
        assertTrue(StatiEsteri.isPrivilegiato(" 103 "));  // HONG KONG
    }

}
