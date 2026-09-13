package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Importazioni#F_ritornaSoloElementiNuovi}, che internamente costruisce la chiave di dedup
 * con {@code F_buildKeyMovimento} (privato, quindi verificato solo attraverso questo metodo pubblico).
 *
 * <p>Da quando le quantità dei token vengono sempre normalizzate con {@code stripTrailingZeros} in
 * ingresso, un movimento già presente in mappa (importato prima di quella normalizzazione, o comunque con
 * zeri non significativi nella stringa) e lo stesso movimento appena ricostruito devono continuare a
 * essere riconosciuti come lo stesso movimento: il confronto della chiave va fatto sul valore numerico
 * ({@link java.math.BigDecimal}), non sulla stringa.</p>
 */
class ImportazioniDedupQtaTest {

    private static final String DATA_ID = "20240315103000";
    private static final String TIMESTAMP = "1710495000000";

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /**
     * Costruisce una riga di movimento minimale con solo i campi che {@code F_buildKeyMovimento} legge.
     * @param ID identificativo completo del movimento
     * @param Exchange campo 3, exchange/wallet principale
     * @param MonetaU simbolo della moneta in uscita (stringa vuota se assente)
     * @param QtaU quantità in uscita, come scritta nel campo (segno e zeri compresi)
     * @param MonetaE simbolo della moneta in entrata (stringa vuota se assente)
     * @param QtaE quantità in entrata, come scritta nel campo (segno e zeri compresi)
     * @return la riga di movimento
     */
    private static String[] movimento(String ID, String Exchange,
            String MonetaU, String QtaU, String MonetaE, String QtaE) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[3] = Exchange;
        v[8] = MonetaU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[13] = QtaE;
        v[29] = TIMESTAMP;
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    @Test
    void movimentoConZeriNonSignificativiRiconosciutoComeGiaPresente() {
        // In mappa un movimento già importato con qta "1.50" (es. prima di stripTrailingZeros)
        MappaCryptoWallet.put(DATA_ID + "_001", movimento(DATA_ID + "_001", "Binance", "", "", "BTC", "1.50"));

        // Lo stesso movimento, ma con qta normalizzata a "1.5"
        List<String[]> nuovi = new ArrayList<>();
        nuovi.add(movimento(DATA_ID + "_999", "Binance", "", "", "BTC", "1.5"));

        List<String[]> risultato = Importazioni.F_ritornaSoloElementiNuovi(nuovi);

        assertTrue(risultato.isEmpty(),
                "\"1.50\" e \"1.5\" sono lo stesso numero: il movimento non deve essere reimportato");
    }

    @Test
    void notazioneScientificaEquivalenteRiconosciutaComeGiaPresente() {
        // Stesso caso del bug M7: una quantità piccola può arrivare in notazione scientifica
        MappaCryptoWallet.put(DATA_ID + "_001", movimento(DATA_ID + "_001", "Kraken", "", "", "SHIB", "2.5E-9"));

        List<String[]> nuovi = new ArrayList<>();
        nuovi.add(movimento(DATA_ID + "_999", "Kraken", "", "", "SHIB", "0.0000000025"));

        List<String[]> risultato = Importazioni.F_ritornaSoloElementiNuovi(nuovi);

        assertTrue(risultato.isEmpty(),
                "\"2.5E-9\" e \"0.0000000025\" sono lo stesso numero: il movimento non deve essere reimportato");
    }

    @Test
    void movimentoConQtaDiversaRestaNuovo() {
        MappaCryptoWallet.put(DATA_ID + "_001", movimento(DATA_ID + "_001", "Binance", "", "", "BTC", "1.5"));

        List<String[]> nuovi = new ArrayList<>();
        nuovi.add(movimento(DATA_ID + "_999", "Binance", "", "", "BTC", "1.6"));

        List<String[]> risultato = Importazioni.F_ritornaSoloElementiNuovi(nuovi);

        assertEquals(1, risultato.size(), "Una quantità realmente diversa deve restare un movimento nuovo");
    }

    @Test
    void qtaNonNumericaNonFaFallireIlConfronto() {
        // "NaN": F_isNumeroNonZero (Double.parseDouble) la considera un numero diverso da zero e la fa
        // entrare nella chiave, ma Funzioni.isNumeric (BigDecimal) la rifiuta. La normalizzazione deve
        // limitarsi a lasciarla invariata invece di lanciare un'eccezione che blocca l'intero import.
        MappaCryptoWallet.put(DATA_ID + "_001", movimento(DATA_ID + "_001", "Binance", "", "", "BTC", "NaN"));

        List<String[]> nuovi = new ArrayList<>();
        nuovi.add(movimento(DATA_ID + "_999", "Binance", "", "", "BTC", "1.5"));

        List<String[]> risultato = assertDoesNotThrow(() -> Importazioni.F_ritornaSoloElementiNuovi(nuovi));

        assertEquals(1, risultato.size(), "Una qta non numerica in mappa non deve far scomparire il nuovo movimento");
    }
}
