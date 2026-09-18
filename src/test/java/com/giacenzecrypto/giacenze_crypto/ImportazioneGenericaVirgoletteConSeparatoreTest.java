package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Fissa {@code ImportazioneGenerica.splitCsvRispettoVirgolette} (bug C16,
 * {@code nocommit/Documentazione/Analisi_Bug_Criticita.md}): un campo quotato che contiene il
 * separatore (es. la virgola delle migliaia di un importo, {@code "$2,270.52"} in un export Nexo)
 * non deve sfasare le colonne successive. Il vecchio comportamento faceva
 * {@code riga.replace("\"","")} PRIMA dello split: la virgola interna restava e veniva scambiata
 * per un separatore di colonna.
 */
class ImportazioneGenericaVirgoletteConSeparatoreTest {

    @Test
    void importoQuotatoConVirgolaDelleMigliaia_nonSfasaLeColonneSuccessive() {
        String riga = "NXT1,Locking Term Deposit,BTC,-0.024,BTC,0.024,\"$2,270.52\",-,-,nota,2025-12-10 7:37:19";
        String[] campi = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");

        assertEquals(11, campi.length, "l'importo quotato conta come UN solo campo");
        assertEquals("$2,270.52", campi[6], "le virgolette sono tolte ma la virgola interna resta");
        assertEquals("-", campi[7], "Fee non deve slittare");
        assertEquals("-", campi[8], "Fee Currency non deve slittare");
        assertEquals("nota", campi[9], "Details non deve slittare");
        assertEquals("2025-12-10 7:37:19", campi[10], "Date non deve slittare");
    }

    @Test
    void campoSenzaVirgolette_siComportaComePrima() {
        String riga = "a,b,c,d";
        String[] campi = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, campi);
    }

    @Test
    void campoQuotatoSenzaSeparatoreInterno_perdeSoloLeVirgolette() {
        String riga = "a,\"testo semplice\",c";
        String[] campi = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");
        assertArrayEquals(new String[]{"a", "testo semplice", "c"}, campi);
    }

    @Test
    void piuCampiQuotatiConVirgoleInterneNellaStessaRiga() {
        String riga = "\"1,000\",x,\"2,500.75\",y";
        String[] campi = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ",");
        assertArrayEquals(new String[]{"1,000", "x", "2,500.75", "y"}, campi);
    }

    @Test
    void separatorePuntoEVirgola_funzionaAllostessoModo() {
        String riga = "a;\"1,5\";c";
        String[] campi = ImportazioneGenerica.splitCsvRispettoVirgolette(riga, ";");
        assertArrayEquals(new String[]{"a", "1,5", "c"}, campi);
    }
}
