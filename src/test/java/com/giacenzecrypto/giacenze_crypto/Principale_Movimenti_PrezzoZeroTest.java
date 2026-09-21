package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abilitazione della voce "Conferma che il token non ha prezzo (valorizza a Zero)": scatta solo se
 * <b>tutti</b> i movimenti selezionati sono senza prezzo, e non deve mai cercare prezzi in rete.
 */
class Principale_Movimenti_PrezzoZeroTest {

    private static final String ID_SENZA = "20240101120000_1_Kraken_1_DC";
    private static final String ID_CON = "20240101130000_1_Kraken_1_DC";
    private static final String ID_VUOTO = "20240101140000_1_Kraken_1_DC";

    private static String[] riga(String id, String prezzo, String flag32) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[15] = prezzo;
        v[32] = flag32;
        return v;
    }

    @BeforeEach
    void preparaMappa() {
        Principale.MappaCryptoWallet.clear();
        Principale.MappaCryptoWallet.put(ID_SENZA, riga(ID_SENZA, "0.00", "NO"));
        Principale.MappaCryptoWallet.put(ID_CON, riga(ID_CON, "12.50", "SI"));
        //[32] vuoto = non ancora valutato dal caricamento tabella: mai un motivo per cercare un prezzo in rete
        Principale.MappaCryptoWallet.put(ID_VUOTO, riga(ID_VUOTO, "0.00", ""));
    }

    @AfterEach
    void pulisciMappa() {
        Principale.MappaCryptoWallet.clear();
    }

    @Test
    void unMovimentoNoConPrezzoZeroESenzaPrezzo() {
        assertTrue(Principale_Movimenti_PrezzoZero.isSenzaPrezzo(Principale.MappaCryptoWallet.get(ID_SENZA)));
    }

    @Test
    void unMovimentoGiaPrezzatoNonLoE() {
        assertFalse(Principale_Movimenti_PrezzoZero.isSenzaPrezzo(Principale.MappaCryptoWallet.get(ID_CON)));
    }

    @Test
    void ilCampoVuotoNonVieneValutatoNeModificato() {
        String[] v = Principale.MappaCryptoWallet.get(ID_VUOTO);
        assertFalse(Principale_Movimenti_PrezzoZero.isSenzaPrezzo(v));
        assertTrue(v[32].isEmpty());
    }

    @Test
    void ilControlloNonModificaLaRiga() {
        String[] v = Principale.MappaCryptoWallet.get(ID_SENZA);
        String[] prima = v.clone();
        Principale_Movimenti_PrezzoZero.isSenzaPrezzo(v);
        assertTrue(Arrays.equals(prima, v));
    }

    @Test
    void laVoceSiAbilitaSoloSeTuttaLaSelezioneESenzaPrezzo() {
        assertTrue(Principale_Movimenti_PrezzoZero.isConfermabile(List.of(ID_SENZA)));
        assertFalse(Principale_Movimenti_PrezzoZero.isConfermabile(List.of(ID_SENZA, ID_CON)));
        assertFalse(Principale_Movimenti_PrezzoZero.isConfermabile(List.of(ID_SENZA, "IDinesistente")));
        assertFalse(Principale_Movimenti_PrezzoZero.isConfermabile(List.of()));
    }
}
