package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test delle due operazioni del menu contestuale che trasformano la struttura di un movimento
 * ({@link Principale_Movimenti_SeparaUnisci}): la separazione di uno scambio nelle sue due gambe e la
 * fusione di un deposito e di un prelievo in un unico scambio.
 *
 * <p>Vengono verificate sia le condizioni di abilitazione delle voci di menu sia il risultato delle
 * trasformazioni, invocando i metodi {@code Esegui*} che concatenano i passi eseguiti dopo la conferma
 * dell'utente (la conferma e la mascherina di attesa vivono in {@link Principale}).</p>
 *
 * <p>I casi di separazione sono costruiti in modo da non richiedere mai una ricerca prezzi online: o la
 * fonte del prezzo è generica (e allora entrambe le gambe la ereditano) oppure la gamba da riprezzare è
 * in euro, il cui controvalore è la quantità stessa. Di conseguenza il round trip
 * separazione/fusione riporta al movimento di partenza solo con la fonte generica: quando il prezzo
 * proviene da una delle due monete la gamba non coperta viene riprezzata, ed è il comportamento voluto.</p>
 */
class Principale_Movimenti_SeparaUnisciTest {

    private static final String DATA_ID = "20240315103000";
    private static final String DATA = "2024-03-15 10:30";
    //Il campo 29 è il timestamp in millisecondi corrispondente alla data contenuta nell'ID
    private static final String TIMESTAMP = "1710495000000";

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /**
     * Costruisce un movimento grezzo minimale e lo inserisce in mappa.
     * @param ID identificativo completo del movimento
     * @param Campo5 descrizione del tipo di transazione
     * @param MonetaU simbolo della moneta in uscita (stringa vuota se assente)
     * @param TipoU tipologia della moneta in uscita
     * @param QtaU quantità in uscita (negativa)
     * @param MonetaE simbolo della moneta in entrata (stringa vuota se assente)
     * @param TipoE tipologia della moneta in entrata
     * @param QtaE quantità in entrata (positiva)
     * @param Prezzo controvalore in euro del movimento
     * @return il movimento inserito in mappa
     */
    private static String[] movimento(String ID, String Campo5,
            String MonetaU, String TipoU, String QtaU,
            String MonetaE, String TipoE, String QtaE, String Prezzo) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = DATA;
        v[2] = "1 di 1";
        v[3] = "Wallet Test";
        v[5] = Campo5;
        v[6] = (MonetaU + " -> " + MonetaE).trim();
        v[7] = "Causale originale";
        v[8] = MonetaU;
        v[9] = TipoU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = TipoE;
        v[13] = QtaE;
        v[15] = Prezzo;
        v[21] = "Nota di prova";
        v[22] = "M";
        v[24] = "0xHashDiProva";
        v[29] = TIMESTAMP;
        v[32] = "SI";
        v[40] = "|||Personalizzato";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    /** Uno scambio crypto/crypto, il caso tipico da separare. */
    private static String[] scambioCryptoCrypto() {
        return movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00");
    }

    // =============================================================================================
    // ABILITAZIONE DELLE VOCI DI MENU
    // =============================================================================================

    @Test
    void movimentoConDueMonete_eSeparabile() {
        String Scambio[] = scambioCryptoCrypto();
        assertTrue(Principale_Movimenti_SeparaUnisci.isSeparabileInDepositoPrelievo(Scambio[0]));
    }

    @Test
    void movimentoConUnaSolaMoneta_nonESeparabile() {
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        assertFalse(Principale_Movimenti_SeparaUnisci.isSeparabileInDepositoPrelievo(Deposito[0]));
    }

    @Test
    void movimentoConQuantitaAZero_nonESeparabile() {
        //Tipico dei movimenti su token scam azzerati: il simbolo c'è ma la quantità no
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "SCAM", "Crypto", "0", "20000.00");
        assertFalse(Principale_Movimenti_SeparaUnisci.isSeparabileInDepositoPrelievo(Scambio[0]));
    }

    @Test
    void depositoEPrelievoNonClassificatiContemporanei_sonoUnibili() {
        movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC", DATA_ID + "_WalletTest_002_001_DC")));
    }

    @Test
    void depositoFIATEPrelievoCrypto_sonoUnibili() {
        //Le due gambe possono avere tipologie di token diverse (Crypto/FIAT/NFT)
        movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        movimento(DATA_ID + "_WalletTest_002_001_DF", "DEPOSITO FIAT",
                "", "", "", "EUR", "FIAT", "20000", "20000.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC", DATA_ID + "_WalletTest_002_001_DF")));
    }

    @Test
    void movimentiSuWalletDiversi_nonSonoUnibili() {
        movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Deposito[3] = "Altro Wallet";

        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC", DATA_ID + "_WalletTest_002_001_DC")));
    }

    @Test
    void movimentiInIstantiDiversi_nonSonoUnibili() {
        movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Deposito[] = movimento("20240315103001_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Deposito[29] = "1710495001000";

        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC", "20240315103001_WalletTest_002_001_DC")));
    }

    @Test
    void movimentoGiaClassificato_nonEUnibile() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "CASH OUT",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        Prelievo[18] = "PCO - CASHOUT O SIMILARE";
        movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC", DATA_ID + "_WalletTest_002_001_DC")));
    }

    @Test
    void dueDepositi_nonSonoUnibili() {
        movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "BTC", "Crypto", "0.5", "20000.00");
        movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_DC", DATA_ID + "_WalletTest_002_001_DC")));
    }

    @Test
    void selezioneDiUnSoloMovimento_nonEUnibile() {
        movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");

        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(
                List.of(DATA_ID + "_WalletTest_001_001_PC")));
        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(List.of()));
        assertFalse(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(null));
    }

    // =============================================================================================
    // SEPARAZIONE IN DEPOSITO/PRELIEVO
    // =============================================================================================

    @Test
    void separazione_creaPrelievoEDepositoEdEliminaLOriginale() {
        String Scambio[] = scambioCryptoCrypto();
        String IDOriginale = Scambio[0];

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        assertNull(MappaCryptoWallet.get(IDOriginale), "il movimento originale deve essere eliminato");
        assertEquals(2, MappaCryptoWallet.size());

        String Prelievo[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC");
        String Deposito[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC");
        assertNotNull(Prelievo, "deve esistere la gamba di prelievo");
        assertNotNull(Deposito, "deve esistere la gamba di deposito");

        //La gamba in uscita porta solo la moneta uscita, quella in entrata solo la moneta entrata
        assertEquals("BTC", Prelievo[8]);
        assertEquals("-0.5", Prelievo[10]);
        assertEquals("", Prelievo[11]);
        assertEquals("", Deposito[8]);
        assertEquals("ETH", Deposito[11]);
        assertEquals("8", Deposito[13]);

        //Campo 5 e categoria ricalcolati dalla tabella di fallback, campo 18 vuoto (da riclassificare)
        assertEquals("PRELIEVO CRYPTO", Prelievo[5]);
        assertEquals("DEPOSITO CRYPTO", Deposito[5]);
        assertEquals("", Prelievo[18]);
        assertEquals("", Deposito[18]);
    }

    @Test
    void separazione_riportaIDatiDelMovimentoOriginale() {
        String Scambio[] = scambioCryptoCrypto();
        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        for (String Gamba[] : List.of(MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC"),
                                      MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC"))) {
            assertEquals("Wallet Test", Gamba[3]);
            assertEquals("Causale originale", Gamba[7]);
            assertEquals("Nota di prova", Gamba[21]);
            assertEquals("0xHashDiProva", Gamba[24]);
            assertEquals(TIMESTAMP, Gamba[29]);
            //Il controvalore dell'originale vale per entrambe le gambe, così come l'info prezzo
            assertEquals("20000.00", Gamba[15]);
            assertEquals("SI", Gamba[32]);
            assertEquals("|||Personalizzato", Gamba[40]);
            //Le gambe sono movimenti creati manualmente
            assertEquals("M", Gamba[22]);
        }
    }

    @Test
    void separazione_diUnAcquistoConFIAT_generaPrelievoFIATEDepositoCrypto() {
        String Acquisto[] = movimento(DATA_ID + "_WalletTest_001_001_AC", "ACQUISTO CRYPTO",
                "EUR", "FIAT", "-20000", "BTC", "Crypto", "0.5", "20000.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Acquisto));

        String Prelievo[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PF");
        String Deposito[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC");
        assertNotNull(Prelievo, "la gamba FIAT in uscita deve diventare un prelievo FIAT (PF)");
        assertNotNull(Deposito, "la gamba crypto in entrata deve diventare un deposito crypto (DC)");
        assertEquals("PRELIEVO FIAT", Prelievo[5]);
        assertEquals("DEPOSITO CRYPTO", Deposito[5]);
    }

    @Test
    void separazione_conQuantitaInNotazioneScientifica_nonAlteraLeQuantita() {
        //Il segno viene forzato manipolando solo il primo carattere: un replace("-","") indiscriminato
        //trasformerebbe 1.5E-8 in 1.5E8
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "SHIB", "Crypto", "-1.5E-8", "ETH", "Crypto", "2.5E-9", "100.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        //Entrambe le quantità restano invariate, esponenti negativi compresi: dopo la correzione M7
        //creaMovimento ricava la direzione dal segno numerico e non serve più riscriverle
        assertEquals("-1.5E-8", MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC")[10]);
        assertEquals("2.5E-9", MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC")[13]);
    }

    @Test
    void separazione_conPrezzoZeroNonCanonico_loNormalizzaANonCercaPrezziOnline() {
        //creaMovimento accetta il prezzo passato solo se supera PrezzoPrezzato, che rifiuta gli zeri
        //scritti diversamente da "0.00": senza normalizzazione si finirebbe nella ricerca prezzi online
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "0");
        Scambio[32] = "NO";

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        assertEquals("0.00", MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC")[15]);
        assertEquals("0.00", MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC")[15]);
    }

    @Test
    void fusione_conPrezzoZeroNonCanonico_loNormalizzaMantenendoLaValorizzazione() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "0");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "0");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Scambio[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC");
        assertEquals("0.00", Scambio[15]);
        //Le due gambe erano marcate come valorizzate: la normalizzazione del prezzo non lo cambia
        assertEquals("SI", Scambio[32]);
    }

    @Test
    void separazione_ilPrelievoPrecedeIlDepositoNellOrdinamento() {
        //L'ordine degli ID è quello con cui il motore LIFO elabora i movimenti: prima si toglie, poi si mette
        String Scambio[] = scambioCryptoCrypto();
        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        String IDInOrdine[] = MappaCryptoWallet.keySet().toArray(new String[0]);
        assertEquals(DATA_ID + "_WalletTest_001_001_PC", IDInOrdine[0]);
        assertEquals(DATA_ID + "_WalletTest_001A_001_DC", IDInOrdine[1]);
    }

    // =============================================================================================
    // PREZZO DELLE GAMBE SEPARATE
    // =============================================================================================

    /** Costruisce una moneta minimale, come quelle passate a creaMovimento dalla separazione. */
    private static Moneta moneta(String Simbolo, String Tipo, String Qta) {
        Moneta M = new Moneta();
        M.Moneta = Simbolo;
        M.Tipo = Tipo;
        M.Qta = Qta;
        return M;
    }

    @Test
    void tokenFontePrezzo_riconosceLeFontiGeneriche() {
        //Il primo segmento del campo 40 è il token su cui il prezzo è stato rilevato: se manca la fonte
        //non è legata a una moneta in particolare
        assertEquals("", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo(null));
        assertEquals("", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo(""));
        assertEquals("", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo("|||Personalizzato"));
        //Campo non nel formato atteso: trattato come fonte generica, non come errore
        assertEquals("", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo("valore inatteso"));

        assertEquals("BTC", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo("BTC|" + TIMESTAMP + "|40000|Binance"));
        //Il prezzo preso dalla gamba in euro ha la fonte vuota ma il token valorizzato: resta specifico
        assertEquals("EUR", Principale_Movimenti_SeparaUnisci.TokenFontePrezzo("EUR|" + TIMESTAMP + "|1|"));
    }

    @Test
    void gambaCopertaDallaFonte_soloSeLaFonteRiguardaQuellaMoneta() {
        Moneta BTC = moneta("BTC", "Crypto", "-0.5");
        Moneta ETH = moneta("ETH", "Crypto", "8");

        //Fonte generica: vale per il movimento nel suo complesso, quindi per entrambe le gambe
        assertTrue(Principale_Movimenti_SeparaUnisci.GambaCopertaDallaFonte("|||Personalizzato", BTC, ""));
        assertTrue(Principale_Movimenti_SeparaUnisci.GambaCopertaDallaFonte("|||Personalizzato", ETH, ""));

        //Fonte specifica: copre solo la moneta da cui il prezzo è stato rilevato
        String Fonte = "BTC|" + TIMESTAMP + "|40000|Binance";
        assertTrue(Principale_Movimenti_SeparaUnisci.GambaCopertaDallaFonte(Fonte, BTC, ""));
        assertFalse(Principale_Movimenti_SeparaUnisci.GambaCopertaDallaFonte(Fonte, ETH, ""));
    }

    @Test
    void separazione_conFonteGenerica_assegnaLoStessoPrezzoAEntrambeLeGambe() {
        //Prezzo imposto dall'utente: non deriva da uno dei due token, quindi vale per tutto il movimento
        //e nessuna delle due gambe va riprezzata
        String Scambio[] = scambioCryptoCrypto();
        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        String Prelievo[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC");
        String Deposito[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC");
        assertEquals("20000.00", Prelievo[15]);
        assertEquals("20000.00", Deposito[15]);
        assertEquals("|||Personalizzato", Prelievo[40]);
        assertEquals("|||Personalizzato", Deposito[40]);
    }

    @Test
    void separazione_conFonteSuUnaSolaMoneta_riprezzaLAltraGamba() {
        //Vendita di BTC contro euro: il controvalore del movimento è stato rilevato sul BTC, quindi
        //descrive la sola gamba in uscita. La gamba in euro non vale gli stessi 20000 e va riprezzata
        //(l'euro non richiede alcuna ricerca: il suo controvalore è la quantità stessa)
        String Vendita[] = movimento(DATA_ID + "_WalletTest_001_001_VC", "VENDITA CRYPTO",
                "BTC", "Crypto", "-0.5", "EUR", "FIAT", "19000", "20000.00");
        Vendita[40] = "BTC|" + TIMESTAMP + "|40000|Binance";

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Vendita));

        String Prelievo[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC");
        String Deposito[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DF");
        assertNotNull(Prelievo, "la gamba crypto in uscita deve diventare un prelievo crypto (PC)");
        assertNotNull(Deposito, "la gamba FIAT in entrata deve diventare un deposito FIAT (DF)");

        //La gamba coperta dalla fonte mantiene prezzo e fonte del movimento originale
        assertEquals("20000.00", Prelievo[15]);
        assertEquals("SI", Prelievo[32]);
        assertEquals("BTC|" + TIMESTAMP + "|40000|Binance", Prelievo[40]);

        //L'altra gamba viene riprezzata sulla propria moneta, con la fonte aggiornata
        assertEquals("19000.00", Deposito[15]);
        assertEquals("SI", Deposito[32]);
        assertTrue(Deposito[40].startsWith("EUR|"), "la fonte deve riferirsi alla moneta della gamba: " + Deposito[40]);
        assertTrue(Deposito[40].endsWith("|1|"), "il cambio dell'euro su sé stesso è 1: " + Deposito[40]);
    }

    @Test
    void separazione_diUnMovimentoNonValorizzato_nonCercaAlcunPrezzo() {
        //Senza prezzo di partenza non c'è nulla da ereditare né motivo di andare a cercarne uno:
        //entrambe le gambe restano non valorizzate come l'originale
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "0.00");
        Scambio[32] = "NO";
        Scambio[40] = "";

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        for (String Gamba[] : List.of(MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC"),
                                      MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC"))) {
            assertEquals("0.00", Gamba[15]);
            assertEquals("NO", Gamba[32]);
            assertEquals("", Gamba[40]);
        }
    }

    // =============================================================================================
    // FUSIONE IN MOVIMENTO DI SCAMBIO
    // =============================================================================================

    @Test
    void fusione_creaLoScambioEdEliminaIDueMovimentiDiPartenza() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        assertEquals(1, MappaCryptoWallet.size(), "i due movimenti di partenza devono essere eliminati");
        String Scambio[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC");
        assertNotNull(Scambio, "lo scambio nasce sull'ID del prelievo, con categoria ricalcolata a SC");

        assertEquals("SCAMBIO CRYPTO", Scambio[5]);
        assertEquals("BTC", Scambio[8]);
        assertEquals("-0.5", Scambio[10]);
        assertEquals("ETH", Scambio[11]);
        assertEquals("8", Scambio[13]);
        assertEquals("", Scambio[18]);
        assertEquals("M", Scambio[22]);
        assertEquals("20000.00", Scambio[15]);
        assertEquals("SI", Scambio[32]);
    }

    @Test
    void fusione_conFIATInEntrata_generaUnaVendita() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DF", "DEPOSITO FIAT",
                "", "", "", "EUR", "FIAT", "20000", "20000.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Vendita[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_VC");
        assertNotNull(Vendita, "crypto in uscita e FIAT in entrata devono dare una vendita (VC)");
        assertEquals("VENDITA CRYPTO", Vendita[5]);
    }

    @Test
    void fusione_seSoloUnaGambaEPrezzataUsaQuelPrezzo() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "0.00");
        Prelievo[32] = "NO";
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "19500.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Scambio[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC");
        assertEquals("19500.00", Scambio[15]);
        assertEquals("SI", Scambio[32]);
    }

    @Test
    void fusione_senzaGambePrezzate_lasciaIlMovimentoNonValorizzato() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "0.00");
        Prelievo[32] = "NO";
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "0.00");
        Deposito[32] = "NO";

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Scambio[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC");
        assertEquals("0.00", Scambio[15]);
        assertEquals("NO", Scambio[32]);
    }

    @Test
    void fusione_conEntrambeLeGambePrezzate_preferisceIlPrezzoDellaGambaFIAT() {
        //DammiMonetaPrioritaria considera il FIAT il riferimento più affidabile
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Deposito[] = movimento(DATA_ID + "_WalletTest_002_001_DF", "DEPOSITO FIAT",
                "", "", "", "EUR", "FIAT", "19500", "19500.00");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Vendita[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_VC");
        assertEquals("19500.00", Vendita[15]);
    }

    // =============================================================================================
    // ANDATA E RITORNO
    // =============================================================================================

    @Test
    void separazioneEFusione_riportanoAlMovimentoDiPartenza() {
        String Scambio[] = scambioCryptoCrypto();
        String IDOriginale = Scambio[0];

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiSeparazione(Scambio));

        String Prelievo[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_PC");
        String Deposito[] = MappaCryptoWallet.get(DATA_ID + "_WalletTest_001A_001_DC");
        assertTrue(Principale_Movimenti_SeparaUnisci.isUnibileInScambio(List.of(Prelievo[0], Deposito[0])),
                "le due gambe appena separate devono essere di nuovo fondibili");

        assertTrue(Principale_Movimenti_SeparaUnisci.EseguiFusione(Prelievo, Deposito, null));

        String Ricomposto[] = MappaCryptoWallet.get(IDOriginale);
        assertNotNull(Ricomposto, "la fusione deve riportare allo stesso ID di partenza");
        assertEquals(1, MappaCryptoWallet.size());
        assertEquals("BTC", Ricomposto[8]);
        assertEquals("-0.5", Ricomposto[10]);
        assertEquals("ETH", Ricomposto[11]);
        assertEquals("8", Ricomposto[13]);
        assertEquals("20000.00", Ricomposto[15]);
    }
}
