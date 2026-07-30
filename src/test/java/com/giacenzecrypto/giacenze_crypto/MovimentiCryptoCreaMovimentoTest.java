package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link MovimentiCrypto#creaMovimento} sul riconoscimento della direzione delle monete
 * (quale sia quella in uscita e quale quella in entrata), che determina campo 5, campo 18 e categoria
 * del movimento prodotto.
 *
 * <p>Pin della correzione <b>M7</b> (vedi {@code Documentazione/Analisi_Bug_Criticita.md}): la direzione
 * si ricava dal <b>segno numerico</b> della quantità e non più dalla presenza di un trattino nella
 * stringa, che classificava come uscita anche le quantità positive in notazione scientifica con
 * esponente negativo ({@code 2.5E-9}).</p>
 *
 * <p>A tutte le chiamate viene passato un prezzo esplicito accettabile da {@code PrezzoPrezzato}: senza,
 * {@code creaMovimento} tenterebbe una ricerca prezzi online.</p>
 */
class MovimentiCryptoCreaMovimentoTest {

    private static final long TIMESTAMP = 1710495000000L;

    /**
     * @param Simbolo simbolo della moneta
     * @param Qta quantità, con il segno che ne determina la direzione
     * @return la moneta di tipo Crypto corrispondente
     */
    private static Moneta moneta(String Simbolo, String Qta) {
        Moneta Mon = new Moneta();
        Mon.Moneta = Simbolo;
        Mon.Tipo = "Crypto";
        Mon.Qta = Qta;
        return Mon;
    }

    /**
     * @param Mon1 prima moneta passata a creaMovimento
     * @param Mon2 seconda moneta passata a creaMovimento
     * @return il movimento generato
     */
    private static String[] crea(Moneta Mon1, Moneta Mon2) {
        return MovimentiCrypto.creaMovimento(Mon1, Mon2, "Wallet Test", "", TIMESTAMP,
                "100.00", null, 1, 1, null, "", "M", "", null, null);
    }

    @Test
    void quantitaPositivaInNotazioneScientificaConEsponenteNegativo_eUnDeposito_correzioneM7() {
        //Prima della correzione "2.5E-9" conteneva un trattino e veniva trattata come quantità in uscita,
        //producendo un PRELIEVO CRYPTO al posto di un DEPOSITO CRYPTO
        String Mov[] = crea(null, moneta("SHIB", "2.5E-9"));

        assertNotNull(Mov);
        assertEquals("DEPOSITO CRYPTO", Mov[5]);
        assertEquals("DC", Mov[0].split("_")[4]);
        assertEquals("SHIB", Mov[11], "la moneta deve finire fra quelle in entrata");
        assertEquals("2.5E-9", Mov[13]);
        assertEquals("", Mov[8], "non deve esserci alcuna moneta in uscita");
    }

    @Test
    void scambioConQuantitaInNotazioneScientifica_riconosceEntrambeLeDirezioni_correzioneM7() {
        //Prima della correzione entrambe le monete finivano in uscita e creaMovimento ritornava null
        //("Movimento incoerente, ci sono due monete in uscita")
        String Mov[] = crea(moneta("BTC", "-1.5E-8"), moneta("SHIB", "2.5E-9"));

        assertNotNull(Mov, "il movimento non deve essere scartato come incoerente");
        assertEquals("SCAMBIO CRYPTO", Mov[5]);
        assertEquals("SC", Mov[0].split("_")[4]);
        assertEquals("BTC", Mov[8]);
        assertEquals("-1.5E-8", Mov[10]);
        assertEquals("SHIB", Mov[11]);
        assertEquals("2.5E-9", Mov[13]);
    }

    @Test
    void quantitaNegativaInNotazioneScientifica_restaUnPrelievo() {
        String Mov[] = crea(moneta("BTC", "-1.5E-8"), null);

        assertNotNull(Mov);
        assertEquals("PRELIEVO CRYPTO", Mov[5]);
        assertEquals("PC", Mov[0].split("_")[4]);
        assertEquals("BTC", Mov[8]);
        assertEquals("-1.5E-8", Mov[10]);
    }

    @Test
    void quantitaInNotazioneDecimale_direzioniInvariate() {
        //Comportamento identico a prima della correzione: è il caso normale, di gran lunga il più frequente
        String Mov[] = crea(moneta("BTC", "-0.5"), moneta("ETH", "8"));

        assertNotNull(Mov);
        assertEquals("SCAMBIO CRYPTO", Mov[5]);
        assertEquals("BTC", Mov[8]);
        assertEquals("-0.5", Mov[10]);
        assertEquals("ETH", Mov[11]);
        assertEquals("8", Mov[13]);
    }

    @Test
    void dueMoneteInUscita_restaUnMovimentoIncoerente() {
        //La guardia sulle direzioni incoerenti continua a valere: la correzione cambia solo COME viene
        //stabilita la direzione, non il rifiuto dei movimenti malformati
        assertNull(crea(moneta("BTC", "-0.5"), moneta("ETH", "-8")));
    }

    @Test
    void quantitaZeroScrittaComeNegativa_vieneIgnorata() {
        //"-0" è numericamente zero: la moneta non è valida e non entra né in uscita né in entrata
        assertNull(crea(moneta("BTC", "-0"), null));
    }
}
