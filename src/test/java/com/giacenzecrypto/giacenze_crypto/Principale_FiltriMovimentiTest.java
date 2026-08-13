package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.FiltriMovimenti;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test dei criteri di filtro della tabella movimenti.
 *
 * <p>Fino al 2026-08-13 questa logica era un {@code if} a sette condizioni dentro un metodo da 300 righe
 * che tocca Swing, quindi <b>non era coperta da un solo test</b> pur decidendo quali movimenti l'utente
 * vede. I casi qui sotto fissano il comportamento com'era prima dell'estrazione, più il criterio nuovo
 * sul documento di origine.
 */
class Principale_FiltriMovimentiTest {

    private static final long GEN = 20240101L;
    private static final long DIC = 20241231L;

    /** @return un movimento minimo, con i soli campi che i filtri guardano */
    private static String[] mov(String wallet, String tipo, String monetaOut, String monetaIn, String documento) {
        String[] v = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < v.length; i++) v[i] = "";
        v[3] = wallet;
        v[5] = tipo;
        v[8] = monetaOut;
        v[11] = monetaIn;
        v[41] = documento;
        return v;
    }

    private static FiltriMovimenti nessuno() {
        return Nessuno(0, 99999999L);
    }

    // ==================== documento di origine (criterio nuovo) ====================

    @Test
    void ilFiltroSulDocumentoDistingueITreCasi() {
        assertTrue(PassaDocumento(DOC_TUTTI, ""), "nessun filtro: passa tutto");
        assertTrue(PassaDocumento(DOC_TUTTI, "3"));

        assertTrue(PassaDocumento(DOC_CON, "3"));
        assertFalse(PassaDocumento(DOC_CON, ""));
        assertFalse(PassaDocumento(DOC_CON, "  "), "un campo di soli spazi è 'nessun documento'");

        assertFalse(PassaDocumento(DOC_SENZA, "3"));
        assertTrue(PassaDocumento(DOC_SENZA, ""));
    }

    @Test
    void unIdPreciso() {
        assertTrue(PassaDocumento("3", "3"));
        assertFalse(PassaDocumento("3", "13"), "il confronto è secco, non 'contiene'");
        assertFalse(PassaDocumento("3", "30"));
        assertFalse(PassaDocumento("3", ""));
        assertTrue(PassaDocumento("3", " 3 "), "gli spazi attorno all'id non contano");
    }

    @Test
    void ilCriterioDocumentoAgisceSulMovimentoIntero() {
        String[] conDoc = mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "3");
        String[] senzaDoc = mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "");

        FiltriMovimenti soloConDoc = new FiltriMovimenti(TUTTI, TUTTI, DOC_CON, 0, 99999999L,
                false, false, false, false);

        assertTrue(soloConDoc.Passa(conDoc, 20240315L, "", true, false));
        assertFalse(soloConDoc.Passa(senzaDoc, 20240315L, "", true, false));
    }

    // ==================== i criteri preesistenti ====================

    @Test
    void senzaCriteriPassaTutto() {
        assertTrue(nessuno().Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", ""),
                20240315L, "", true, false));
        assertTrue(nessuno().Passa(mov("OKX", "Trasferimento Interno", "ETH", "", "7"),
                20200101L, "Gruppo1", false, true));
    }

    @Test
    void lIntervalloDiDateEInclusivoSuEntrambiGliEstremi() {
        FiltriMovimenti f = new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, GEN, DIC, false, false, false, false);
        String[] v = mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "");

        assertTrue(f.Passa(v, GEN, "", true, false));
        assertTrue(f.Passa(v, DIC, "", true, false));
        assertFalse(f.Passa(v, GEN - 1, "", true, false));
        assertFalse(f.Passa(v, DIC + 1, "", true, false));
    }

    @Test
    void ilWalletCombaciaAnchePerGruppo() {
        //La combo espone sia i singoli wallet sia le voci "Wallet : Gruppo (n)": scegliendo la seconda
        //compaiono i movimenti di tutti i wallet del gruppo, non solo di quello nominato.
        FiltriMovimenti perGruppo = new FiltriMovimenti("Ledger : Personali (4)", TUTTI, DOC_TUTTI,
                0, 99999999L, false, false, false, false);

        assertTrue(perGruppo.Passa(mov("Metamask", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "Personali", true, false),
                "un altro wallet dello stesso gruppo passa");
        assertTrue(perGruppo.Passa(mov("Ledger", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "Personali", true, false),
                "e il wallet nominato, che al gruppo appartiene");
        assertFalse(perGruppo.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "Exchange", true, false));
    }

    @Test
    void scegliendoUnSingoloWalletPassanoAncheIMovimentiDeiWalletSenzaGruppo() {
        //CARATTERIZZAZIONE DI UN DIFETTO, non una regola voluta: scegliendo un singolo wallet il gruppo
        //cercato e' la stringa vuota, e la condizione "gruppo del movimento == gruppo cercato" e' quindi
        //vera per OGNI movimento di un wallet senza gruppo. Il comportamento e' identico a quello che
        //c'era prima dell'estrazione ed e' stato riportato tale e quale; non si vede su un archivio in
        //cui tutti i wallet hanno un gruppo, come richiede il quadro RW.
        FiltriMovimenti soloBinance = new FiltriMovimenti("Binance", TUTTI, DOC_TUTTI, 0, 99999999L,
                false, false, false, false);

        assertTrue(soloBinance.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "Exchange", true, false),
                "il wallet scelto passa, ovviamente");
        assertTrue(soloBinance.Passa(mov("OKX", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "", true, false),
                "ma passa anche un altro wallet, se non ha gruppo");
        assertFalse(soloBinance.Passa(mov("OKX", "SCAMBIO CRYPTO", "BTC", "", ""), 20240315L, "Exchange", true, false),
                "mentre un altro wallet CON gruppo viene correttamente escluso");
    }

    @Test
    void ilGruppoSiRicavaDallaVoceDellaCombo() {
        assertEquals("Personali", GruppoDaVoceWallet("Ledger : Personali (4)"));
        assertEquals("Personali", GruppoDaVoceWallet("Ledger : Personali"));
        assertEquals("", GruppoDaVoceWallet("Ledger"));
        assertEquals("", GruppoDaVoceWallet(null));
    }

    @Test
    void ilTokenCombaciaSiaInEntrataSiaInUscita() {
        FiltriMovimenti soloBTC = new FiltriMovimenti(TUTTI, "BTC", DOC_TUTTI, 0, 99999999L,
                false, false, false, false);

        assertTrue(soloBTC.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", ""), 20240315L, "", true, false));
        assertTrue(soloBTC.Passa(mov("Binance", "SCAMBIO CRYPTO", "USDC", "BTC", ""), 20240315L, "", true, false));
        assertFalse(soloBTC.Passa(mov("Binance", "SCAMBIO CRYPTO", "ETH", "USDC", ""), 20240315L, "", true, false));
    }

    @Test
    void nascondiTrasferimentiInterni() {
        FiltriMovimenti f = new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, 0, 99999999L, true, false, false, false);

        assertFalse(f.Passa(mov("OKX", "Trasferimento Interno", "ETH", "", ""), 20240315L, "", true, false));
        assertFalse(f.Passa(mov("OKX", "  trasferimento interno  ", "ETH", "", ""), 20240315L, "", true, false),
                "il confronto ignora spazi e maiuscole, come prima");
        assertTrue(f.Passa(mov("OKX", "SCAMBIO CRYPTO", "ETH", "USDC", ""), 20240315L, "", true, false));
    }

    @Test
    void unoScambioConUnSoloTokenScamRestaVisibile() {
        //Regola sottile e voluta: si nasconde solo il movimento in cui il token marcato è l'unico che
        //muove. In uno scambio fra un token buono e uno SCAM sparirebbe anche la gamba buona.
        FiltriMovimenti f = new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, 0, 99999999L, false, true, false, false);
        String scam = "TRUFFA **";

        assertFalse(f.Passa(mov("Wallet", "DEPOSITO CRYPTO", "", scam, ""), 20240315L, "", true, false),
                "un deposito del solo token scam si nasconde");
        assertFalse(f.Passa(mov("Wallet", "PRELIEVO CRYPTO", scam, "", ""), 20240315L, "", true, false),
                "e anche il prelievo");
        assertTrue(f.Passa(mov("Wallet", "SCAMBIO CRYPTO", "BTC", scam, ""), 20240315L, "", true, false),
                "lo scambio no: nasconderlo farebbe sparire anche il BTC uscito");
    }

    @Test
    void soloSenzaPrezzoESoloLifoMancante() {
        String[] v = mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "");

        FiltriMovimenti senzaPrezzo = new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, 0, 99999999L,
                false, false, true, false);
        assertTrue(senzaPrezzo.Passa(v, 20240315L, "", false, false));
        assertFalse(senzaPrezzo.Passa(v, 20240315L, "", true, false));

        FiltriMovimenti lifo = new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, 0, 99999999L,
                false, false, false, true);
        assertTrue(lifo.Passa(v, 20240315L, "", true, true));
        assertFalse(lifo.Passa(v, 20240315L, "", true, false));
    }

    @Test
    void iCriteriSiCombinanoInAnd() {
        FiltriMovimenti f = new FiltriMovimenti("Binance", "BTC", DOC_CON, GEN, DIC, true, false, false, false);

        assertTrue(f.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "3"), 20240315L, "", true, false));
        assertFalse(f.Passa(mov("OKX", "SCAMBIO CRYPTO", "BTC", "USDC", "3"), 20240315L, "Exchange", true, false),
                "wallet sbagliato (con gruppo: vedi il difetto caratterizzato sopra)");
        assertFalse(f.Passa(mov("Binance", "SCAMBIO CRYPTO", "ETH", "USDC", "3"), 20240315L, "", true, false),
                "token sbagliato");
        assertFalse(f.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", ""), 20240315L, "", true, false),
                "documento mancante");
        assertFalse(f.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", "3"), 20230315L, "", true, false),
                "fuori intervallo");
    }

    // ==================== contatore per il pulsante ====================

    @Test
    void ilContatoreDeiCriteriAttivi() {
        assertEquals(0, nessuno().Attivi(), "nessun filtro: il pulsante non mostra numeri");

        //Le date non contano: sono sempre valorizzate, e conteggiarle mostrerebbe un filtro attivo
        //anche a chi non ha filtrato nulla.
        assertEquals(0, new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, GEN, DIC, false, false, false, false).Attivi());

        assertEquals(3, new FiltriMovimenti("Binance", "BTC", DOC_CON, GEN, DIC,
                false, false, false, false).Attivi());
        assertEquals(7, new FiltriMovimenti("Binance", "BTC", "3", GEN, DIC,
                true, true, true, true).Attivi());
    }

    // ==================== elenco per il tooltip del pulsante ====================

    @Test
    void lElencoDelTooltipHaSempreTantiElementiQuantiIFiltriAttivi() {
        //Il pulsante mostra Attivi(), il tooltip mostra Descrizione(): se i due divergessero il tooltip
        //direbbe "(3)" ed elencherebbe quattro criteri, cioè sarebbe peggio di nessun tooltip.
        assertEquals(0, nessuno().Descrizione().size(), "nessun filtro: niente da elencare");

        FiltriMovimenti tutti = new FiltriMovimenti("Binance", "BTC", "3", GEN, DIC, true, true, true, true);
        assertEquals(tutti.Attivi(), tutti.Descrizione().size());
        assertEquals(7, tutti.Descrizione().size());

        FiltriMovimenti parziali = new FiltriMovimenti("Binance", TUTTI, DOC_CON, GEN, DIC,
                false, true, false, false);
        assertEquals(parziali.Attivi(), parziali.Descrizione().size());
    }

    @Test
    void lElencoDistingueUnWalletDaUnGruppo() {
        assertEquals("Wallet : Binance",
                new FiltriMovimenti("Binance", TUTTI, DOC_TUTTI, GEN, DIC, false, false, false, false)
                        .Descrizione().get(0));

        //La voce "Wallet : Gruppo (n)" della combo seleziona il gruppo: ripeterla tale e quale nel
        //tooltip mostrerebbe all'utente la forma interna invece del criterio.
        assertEquals("Gruppo di wallet : Exchange",
                new FiltriMovimenti("Binance : Exchange (12)", TUTTI, DOC_TUTTI, GEN, DIC, false, false, false, false)
                        .Descrizione().get(0));
    }

    @Test
    void ilDocumentoEDescrittoNeiQuattroCasi() {
        assertEquals("tutti", DescrizioneDocumento(DOC_TUTTI, null));
        assertEquals("solo i movimenti che ne hanno uno", DescrizioneDocumento(DOC_CON, null));
        assertEquals("solo i movimenti che non ne hanno", DescrizioneDocumento(DOC_SENZA, null));

        assertEquals("n. 3", DescrizioneDocumento("3", null), "senza risolutore resta il solo id");
        assertEquals("n. 3 — estratto.csv", DescrizioneDocumento("3", id -> "estratto.csv"));
        assertEquals("n. 3", DescrizioneDocumento("3", id -> ""), "documento non più nel registro");

        //Un registro illeggibile non deve far saltare il tooltip: il criterio resta descritto per id.
        assertEquals("n. 3", DescrizioneDocumento("3", id -> { throw new RuntimeException("db chiuso"); }));
    }

    @Test
    void ilTooltipEHtmlEProteggeIValori() {
        String vuoto = Tooltip("Apre i filtri", nessuno(), null, "", null);
        assertTrue(vuoto.startsWith("<html>") && vuoto.endsWith("</html>"));
        assertTrue(vuoto.contains("Nessun filtro di caricamento attivo"));

        String pieno = Tooltip("Apre i filtri",
                new FiltriMovimenti("Wallet & Co.", "BTC", DOC_TUTTI, GEN, DIC, false, false, false, true),
                null, "2024-01-01  →  2024-12-31", null);

        assertTrue(pieno.contains("Filtri attivi (3)"));
        assertTrue(pieno.contains("<li>Wallet : Wallet &amp; Co.</li>"), "la & va neutralizzata o il tooltip si tronca");
        assertTrue(pieno.contains("<li>Moneta : BTC</li>"));
        assertTrue(pieno.contains("<li>Solo i movimenti con LiFo mancante</li>"));
        assertTrue(pieno.contains("Periodo : 2024-01-01"), "le date si mostrano ma fuori dall'elenco");
        assertFalse(pieno.contains("<li>Periodo"));
    }

    @Test
    void iFiltriDiRigaCompaionoInUnaSezioneAParte() {
        //Con il record neutro ma una colonna filtrata la tabella mostra meno righe: dire "nessun filtro"
        //e basta sarebbe falso, e "Azzera Filtri" spegne entrambe le famiglie.
        String t = Tooltip("Apre i filtri", nessuno(), null, "",
                java.util.List.of("Ricerca nel testo : btc", "Filtri per colonna : Data, Wallet"));

        assertTrue(t.contains("Nessun filtro di caricamento attivo"));
        assertTrue(t.contains("<li>Ricerca nel testo : btc</li>"));
        assertTrue(t.contains("<li>Filtri per colonna : Data, Wallet</li>"));
        assertFalse(t.contains("Filtri attivi ("), "non entrano nel conteggio del pulsante");
    }

    @Test
    void ilRecordNormalizzaINull() {
        //Le combo possono restituire stringhe vuote nei primi istanti di vita della finestra: senza
        //normalizzazione un filtro vuoto nasconderebbe ogni movimento invece di non filtrare nulla.
        FiltriMovimenti f = new FiltriMovimenti(null, "", null, 0, 99999999L, false, false, false, false);

        assertEquals(TUTTI, f.Wallet());
        assertEquals(TUTTI, f.Token());
        assertEquals(DOC_TUTTI, f.Documento());
        assertTrue(f.Passa(mov("Binance", "SCAMBIO CRYPTO", "BTC", "USDC", ""), 20240315L, "", true, false));
    }
}
