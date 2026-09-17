package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Importazioni#SerializzaRiga} / {@link Importazioni#DeserializzaRiga}, la coppia
 * estratta da {@code Scrivi_Movimenti_Crypto} per poter riusare lo stesso formato nello storico delle
 * modifiche ai movimenti.
 *
 * <p>Il punto delicato, e il motivo per cui questi test esistono: <b>il giro non è l'identità</b>. Il
 * formato del file dei movimenti non ha escaping e i {@code ;} presenti nei valori vengono
 * <i>rimossi</i> in scrittura, non protetti. Un test scritto come "riscrivo e riottengo lo stesso
 * valore" fallirebbe su una funzione che si comporta esattamente come deve: qui si fissa la perdita
 * come comportamento atteso.</p>
 */
class ImportazioniSerializzaRigaTest {

    /** Riga piena di valori riconoscibili, per verificare che nessun campo si sposti nel giro. */
    private static String[] rigaDiProva() {
        String[] v = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < v.length; i++) {
            v[i] = "campo" + i;
        }
        return v;
    }

    @Test
    void rigaPiena_sopravviveAlGiroCompleto() {
        String[] v = rigaDiProva();

        String[] riletta = Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(v));

        assertEquals(Importazioni.ColonneTabella, riletta.length);
        assertArrayEquals(v, riletta, "nessun campo deve spostarsi nel giro di serializzazione");
    }

    @Test
    void separatoreDentroUnCampo_vienePersoEnonProtetto() {
        String[] v = rigaDiProva();
        v[21] = "nota; con punto e virgola";

        String Serializzata = Importazioni.SerializzaRiga(v);
        String[] riletta = Importazioni.DeserializzaRiga(Serializzata);

        //Il ';' sparisce: è il comportamento storico di Scrivi_Movimenti_Crypto, che lo rimuove invece
        //di escaparlo perché altrimenti romperebbe il file. Il giro NON è l'identità, e va bene così:
        //quello che conta è che non si sfasino i campi successivi.
        assertEquals("nota con punto e virgola", riletta[21]);
        assertEquals(Importazioni.ColonneTabella, riletta.length);
        assertEquals("campo22", riletta[22], "i campi dopo quello 'sporco' non devono sfasarsi");
        assertEquals("campo44", riletta[44]);
    }

    @Test
    void rigaPiuCortaDelPrevisto_vienePortataA45SenzaNull() {
        String[] corta = new String[]{"id", "2024-03-15 10:30", "1 di 1", "Wallet Test"};

        String[] riletta = Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(corta));

        assertEquals(Importazioni.ColonneTabella, riletta.length);
        assertEquals("id", riletta[0]);
        assertEquals("Wallet Test", riletta[3]);
        for (int i = 0; i < riletta.length; i++) {
            assertNotNull(riletta[i], "nessuna cella deve restare null: il campo " + i + " lo è");
        }
        assertEquals("", riletta[44]);
    }

    @Test
    void celleNulle_diventanoStringheVuote() {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = "id";

        String[] riletta = Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(v));

        assertEquals("id", riletta[0]);
        assertEquals("", riletta[1]);
        assertEquals("", riletta[44]);
    }

    @Test
    void rigaConPiuCampiDelModello_vieneTroncata() {
        String[] lunga = new String[Importazioni.ColonneTabella + 3];
        for (int i = 0; i < lunga.length; i++) {
            lunga[i] = "campo" + i;
        }

        String[] riletta = Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(lunga));

        assertEquals(Importazioni.ColonneTabella, riletta.length, "il modello resta a ColonneTabella campi");
        assertEquals("campo44", riletta[44]);
    }

    @Test
    void rigaNulla_nonFaEsplodereLaRilettura() {
        String[] riletta = Importazioni.DeserializzaRiga(null);

        assertEquals(Importazioni.ColonneTabella, riletta.length);
        assertEquals("", riletta[0]);
    }
}
