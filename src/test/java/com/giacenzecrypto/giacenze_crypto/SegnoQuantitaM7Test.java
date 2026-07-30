package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della correzione <b>M7</b> fuori da {@code creaMovimento} (vedi
 * {@code Documentazione/Analisi_Bug_Criticita.md}): la direzione di un movimento veniva dedotta cercando
 * un trattino <i>in qualunque posizione</i> della quantità, così una quantità <b>positiva</b> in notazione
 * scientifica con esponente negativo ({@code 2.5E-9}) veniva scambiata per una quantità in uscita.
 *
 * <p>Copre il nuovo helper condiviso {@link Funzioni#isNegativo}, l'inversione di segno
 * {@link Moneta#InvertiQta} e la classificazione entrata/uscita di
 * {@link TransazioneDefi#IdentificaTipoTransazioneCEX}.</p>
 *
 * <p>La variante DeFi {@code IdentificaTipoTransazione} non è esercitabile in un test perché l'unico modo
 * di popolarne la mappa, {@code InserisciMonete}, chiama {@code Prezzi.DammiPrezzoTransazione} (rete e
 * database prezzi). Usa però lo stesso {@code Funzioni.isNegativo} della variante CEX qui testata.</p>
 */
class SegnoQuantitaM7Test {

    // =============================================================================================
    // Funzioni.isNegativo
    // =============================================================================================

    @Test
    void quantitaPositivaInNotazioneScientifica_nonEUnUscita_correzioneM7() {
        //Il caso che il vecchio contains("-") sbagliava
        assertFalse(Funzioni.isNegativo("2.5E-9"));
        assertFalse(Funzioni.isNegativo("1.5E-18"));
        assertFalse(Funzioni.isNegativo("2.5e-9"));
    }

    @Test
    void quantitaNegativaInNotazioneScientifica_eUnUscita() {
        assertTrue(Funzioni.isNegativo("-2.5E-9"));
        assertTrue(Funzioni.isNegativo("-1.5E+8"));
    }

    @Test
    void quantitaInNotazioneDecimale_segnoRiconosciutoComePrima() {
        assertTrue(Funzioni.isNegativo("-0.5"));
        assertTrue(Funzioni.isNegativo("-1000"));
        assertFalse(Funzioni.isNegativo("0.5"));
        assertFalse(Funzioni.isNegativo("1000"));
    }

    @Test
    void quantitaNulla_nonEConsiderataUscita() {
        assertFalse(Funzioni.isNegativo("0"));
        assertFalse(Funzioni.isNegativo("0.00"));
        //"-0" è numericamente zero: prima veniva letto come uscita, ora no
        assertFalse(Funzioni.isNegativo("-0"));
    }

    @Test
    void valoriNonNumerici_mantengonoIlVecchioComportamentoTestuale() {
        //Scelta voluta: sui dati malformati la correzione non deve cambiare nulla
        assertTrue(Funzioni.isNegativo("abc-def"));
        assertFalse(Funzioni.isNegativo("abc"));
        assertFalse(Funzioni.isNegativo(""));
        assertFalse(Funzioni.isNegativo(null));
    }

    // =============================================================================================
    // Moneta.InvertiQta
    // =============================================================================================

    /**
     * @param Qta quantità iniziale della moneta
     * @return la quantità dopo l'inversione di segno
     */
    private static String invertita(String Qta) {
        Moneta Mon = new Moneta();
        Mon.Qta = Qta;
        Mon.InvertiQta();
        return Mon.Qta;
    }

    @Test
    void inversioneDiSegnoInNotazioneScientifica_nonToccaLEsponente_correzioneM7() {
        //Prima: replace("-","") cancellava anche il segno dell'esponente, quindi 1.5E-8 diventava 1.5E8,
        //cioè un valore moltiplicato per 10^16
        assertEquals("-1.5E-8", invertita("1.5E-8"));
        assertEquals("1.5E-8", invertita("-1.5E-8"));
    }

    @Test
    void inversioneDiSegnoInNotazioneDecimale_comePrima() {
        assertEquals("-0.5", invertita("0.5"));
        assertEquals("0.5", invertita("-0.5"));
        assertEquals("-1000", invertita("1000"));
    }

    @Test
    void inversioneDiQuantitaVuotaONulla_nonFaNulla() {
        assertNull(invertita(null));
        assertEquals("", invertita(""));
    }

    // =============================================================================================
    // TransazioneDefi.IdentificaTipoTransazioneCEX
    // =============================================================================================

    /**
     * @param Simbolo simbolo della moneta
     * @param Qta quantità, con il segno che ne determina la direzione
     * @return la moneta corrispondente, con un prezzo valorizzato
     */
    private static Moneta moneta(String Simbolo, String Qta) {
        Moneta Mon = new Moneta();
        Mon.Moneta = Simbolo;
        Mon.Tipo = "Crypto";
        Mon.Qta = Qta;
        Mon.Prezzo = "100.00";
        return Mon;
    }

    @Test
    void cexTokenPositivoInNotazioneScientifica_eUnDeposito_correzioneM7() {
        //Prima della correzione il token finiva fra le uscite e la transazione diventava un "Prelievo"
        TransazioneDefi Trans = new TransazioneDefi();
        Trans.InserisciMoneteCEX(moneta("SHIB", "2.5E-9"), "Principale", "", "");

        assertEquals("Deposito", Trans.IdentificaTipoTransazioneCEX());
        assertEquals(1, Trans.RitornaNumeroTokenentrata());
        assertEquals(0, Trans.RitornaNumeroTokenUscita());
    }

    @Test
    void cexScambioConNotazioneScientifica_riconosceEntrambeLeDirezioni_correzioneM7() {
        //Prima della correzione entrambi i token finivano fra le uscite: la transazione, che è uno
        //scambio, veniva classificata come "Prelievo"
        TransazioneDefi Trans = new TransazioneDefi();
        Trans.InserisciMoneteCEX(moneta("BTC", "-1.5E-8"), "Principale", "", "");
        Trans.InserisciMoneteCEX(moneta("SHIB", "2.5E-9"), "Principale", "", "");

        assertEquals("Scambio", Trans.IdentificaTipoTransazioneCEX());
        assertEquals(1, Trans.RitornaNumeroTokenentrata());
        assertEquals(1, Trans.RitornaNumeroTokenUscita());
    }

    @Test
    void cexScambioInNotazioneDecimale_classificazioneInvariata() {
        TransazioneDefi Trans = new TransazioneDefi();
        Trans.InserisciMoneteCEX(moneta("BTC", "-0.5"), "Principale", "", "");
        Trans.InserisciMoneteCEX(moneta("ETH", "8"), "Principale", "", "");

        assertEquals("Scambio", Trans.IdentificaTipoTransazioneCEX());
    }

    @Test
    void cexSoloUscite_restaUnPrelievo() {
        TransazioneDefi Trans = new TransazioneDefi();
        Trans.InserisciMoneteCEX(moneta("BTC", "-0.5"), "Principale", "", "");

        assertEquals("Prelievo", Trans.IdentificaTipoTransazioneCEX());
    }

    @Test
    void cexSommaDiDueMovimentiSulloStessoToken_usaIlSegnoDellaSomma() {
        //Il ramo di somma allinea il segno del prezzo a quello della quantità: verifico che la quantità
        //risultante (positiva) faccia classificare la transazione come deposito
        TransazioneDefi Trans = new TransazioneDefi();
        Trans.InserisciMoneteCEX(moneta("ETH", "-2"), "Principale", "", "");
        Trans.InserisciMoneteCEX(moneta("ETH", "8"), "Principale", "", "");

        assertEquals("Deposito", Trans.IdentificaTipoTransazioneCEX());
    }
}
