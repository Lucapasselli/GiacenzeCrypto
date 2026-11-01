package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * La somma mostrata nell'intestazione delle colonne ({@link Tabelle#CalcolaSomme}): decimali, colonne escluse
 * e quantità di token diversi.
 */
class TabelleSommeColonneTest {

    private static DefaultTableModel modello(Object[]... righe) {
        return new DefaultTableModel(righe, new String[]{"Moneta", "Qta", "Valore"});
    }

    private static int[] tutte(DefaultTableModel m) {
        int[] r = new int[m.getRowCount()];
        for (int i = 0; i < r.length; i++) r[i] = i;
        return r;
    }

    private static Map<Integer, String> somme(DefaultTableModel m, Object senzaSomma, Object qtaPerToken) {
        return Tabelle.CalcolaSomme(m, m.getColumnCount(), tutte(m), senzaSomma, qtaPerToken, () -> false);
    }

    /** Il caso visto a video: 0,1 - 0,02 + 0,05 - 0,05 + 0,0499 + 0,04 - 0,06 = 0,1099 compariva come 0,11. */
    @Test
    void laQuantitaNonVieneArrotondataADueDecimali() {
        DefaultTableModel m = modello(
                new Object[]{"BTC", "0.1", "1"}, new Object[]{"BTC", "-0.02", "1"}, new Object[]{"BTC", "0.05", "1"},
                new Object[]{"BTC", "-0.05", "1"}, new Object[]{"BTC", "0.0499", "1"}, new Object[]{"BTC", "0.04", "1"},
                new Object[]{"BTC", "-0.06", "1"});
        assertEquals("0,1099", somme(m, null, null).get(1));
    }

    @Test
    void unaSommaPiccolaNonSparisce() {
        DefaultTableModel m = modello(new Object[]{"SHIB", "0.0004", "1"}, new Object[]{"SHIB", "0.0003", "1"});
        assertEquals("0,0007", somme(m, null, null).get(1));
    }

    @Test
    void gliImportiInEuroRestanoADueDecimali() {
        DefaultTableModel m = modello(new Object[]{"EUR", "1", "1234.5"}, new Object[]{"EUR", "1", "0.25"});
        assertEquals("1.234,75", somme(m, null, null).get(2));
        DefaultTableModel intero = modello(new Object[]{"EUR", "1", "10"}, new Object[]{"EUR", "1", "5"});
        assertEquals("15,00", somme(intero, null, null).get(2));
    }

    @Test
    void laSommaZeroRestaVuota() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.5", "1"}, new Object[]{"BTC", "-0.5", "-1"});
        assertEquals("", somme(m, null, null).get(1));
    }

    @Test
    void leColonneSenzaSommaNonHannoVoce() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.5", "7"});
        Map<Integer, String> s = somme(m, Set.of(1), null);
        assertFalse(s.containsKey(1));
        assertEquals("7,00", s.get(2));
    }

    @Test
    void laQuantitaDiTokenDiversiNonSiSomma() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.1", "1"}, new Object[]{"EUR", "5000", "1"});
        assertEquals("", somme(m, null, Map.of(1, 0)).get(1));
    }

    @Test
    void laQuantitaDelloStessoTokenSiSomma() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.1", "1"}, new Object[]{"BTC", "0.04", "1"});
        assertEquals("0,14", somme(m, null, Map.of(1, 0)).get(1));
    }

    /** Righe senza gamba (quantità vuota o zero) non devono far risultare "misto" un filtro su un solo token. */
    @Test
    void leRigheSenzaQuantitaNonContanoNelConfrontoDeiToken() {
        DefaultTableModel m = modello(new Object[]{"", "", "1"}, new Object[]{"BTC", "0.1", "1"},
                new Object[]{"ETH", "0", "1"}, new Object[]{"BTC", "-0.04", "1"});
        assertEquals("0,06", somme(m, null, Map.of(1, 0)).get(1));
    }

    /** La regola dei token vale solo per la colonna dichiarata: le altre si sommano come prima. */
    @Test
    void laRegolaDeiTokenNonToccaLeAltreColonne() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.1", "10"}, new Object[]{"EUR", "5", "20"});
        assertEquals("30,00", somme(m, null, Map.of(1, 0)).get(2));
    }

    @Test
    void unCalcoloSuperatoNonRestituisceNulla() {
        DefaultTableModel m = modello(new Object[]{"BTC", "0.1", "1"});
        assertNull(Tabelle.CalcolaSomme(m, m.getColumnCount(), tutte(m), null, null, () -> true));
    }

    @Test
    void soloLeRigheVisibiliContano() {
        DefaultTableModel m = modello(new Object[]{"BTC", "1", "1"}, new Object[]{"BTC", "2", "1"}, new Object[]{"BTC", "4", "1"});
        Map<Integer, String> s = Tabelle.CalcolaSomme(m, m.getColumnCount(), new int[]{0, 2}, null, null, () -> false);
        assertEquals("5,00", s.get(1));
    }
}
