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
     * @param Simbolo simbolo della moneta FIAT
     * @param Qta quantità, con il segno che ne determina la direzione
     * @return la moneta di tipo FIAT corrispondente
     */
    private static Moneta fiat(String Simbolo, String Qta) {
        Moneta Mon = new Moneta();
        Mon.Moneta = Simbolo;
        Mon.Tipo = "FIAT";
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

    @Test
    void depositoFiatPuro_haNm1Zero_eOrdinaPrimaDegliAltriMovimentiDelloStessoSecondo() {
        //A parità di secondo e di wallet l'ordinamento di MappaCryptoWallet (TreeMap sull'ID) cade sul
        //codice categoria: "AC" < "DF", quindi un acquisto Nexo fatto con euro appena depositati veniva
        //memorizzato PRIMA del deposito, con una giacenza fiat momentaneamente negativa. Il deposito fiat
        //puro deve ora avere nm1 = "000" e precedere l'acquisto dello stesso istante.
        String Deposito[] = MovimentiCrypto.creaMovimento(null, fiat("EUR", "50"), "Nexo", "Principale",
                TIMESTAMP, "50.00", null, 1, 1, null, "", "A", "", null, "Nexo");
        String Acquisto[] = MovimentiCrypto.creaMovimento(fiat("EUR", "-50"), moneta("USDT", "53"), "Nexo",
                "Principale", TIMESTAMP, "50.00", null, 1, 1, null, "", "A", "", null, "Nexo");

        assertNotNull(Deposito);
        assertNotNull(Acquisto);
        assertEquals("DF", Deposito[0].split("_")[4]);
        assertEquals("AC", Acquisto[0].split("_")[4]);
        assertEquals("000", Deposito[0].split("_")[2], "il deposito fiat puro deve avere nm1 = 000");
        assertEquals("001", Acquisto[0].split("_")[2], "gli altri movimenti restano a nm1 = 001");
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(Deposito[0], Acquisto[0]) < 0,
                "l'ID del deposito fiat deve ordinarsi prima di quello dell'acquisto dello stesso secondo");
    }

    @Test
    void venditaCryptoPerFiat_nonToccata_nm1Uno() {
        //La forzatura di nm1 riguarda solo la categoria "DF": una vendita di cripto per euro (uscita
        //cripto + entrata fiat, categoria "VC") non deve essere spostata.
        String Mov[] = MovimentiCrypto.creaMovimento(moneta("USDT", "-53"), fiat("EUR", "50"), "Nexo",
                "Principale", TIMESTAMP, "50.00", null, 1, 1, null, "", "A", "", null, "Nexo");

        assertNotNull(Mov);
        assertEquals("VC", Mov[0].split("_")[4]);
        assertEquals("001", Mov[0].split("_")[2]);
    }
}
