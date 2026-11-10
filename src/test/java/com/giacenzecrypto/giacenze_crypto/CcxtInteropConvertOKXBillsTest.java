package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link CcxtInterop#convertOKXBills} — la conversione dei "bills" grezzi restituiti dall'API di
 * OKX ({@code Scripts/OKX_Bills.js}) nel formato intermedio a 19 campi che alimenta
 * {@link Importazioni#Ex_OKX_RaggruppaEConsolida}, lo stesso consolidamento usato dall'import del CSV.
 *
 * <p>Sono test caratterizzanti sulla parte <b>deterministica e priva di dipendenze</b> del ramo OKX via
 * API: nessun accesso a database, nessuna ricerca prezzi. Il consolidamento vero e proprio non è coperto
 * qui perché {@code Ex_OKX_Consolida} richiede la ricerca prezzi.</p>
 *
 * <p>Pinnano in particolare le scelte descritte nel javadoc di {@code convertOKXBills}: sono mappate solo
 * le causali verificabili, mentre ogni altro codice {@code type} resta <b>non mappato</b> e viene esposto
 * come {@code "OKX type N"} così da finire nel riepilogo dei movimenti sconosciuti invece di essere
 * classificato a caso.</p>
 */
class CcxtInteropConvertOKXBillsTest {

    /** @return l'array JSON dei bill a partire dalla sua rappresentazione testuale */
    private static JsonArray bills(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    @Test
    void fundingDistingueDepositoEPrelievoDalCodiceType() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"100","ccy":"BTC","balChg":"0.5","type":"1","ts":"1700000000000"},
             {"billId":"101","ccy":"BTC","balChg":"-0.25","type":"2","ts":"1700000001000"}]
            """), "Funding");

        assertEquals(2, righe.size());
        assertEquals("deposit", righe.get(0)[4]);
        assertEquals("withdrawal", righe.get(1)[4]);
        //Il conto di provenienza è noto senza euristiche, a differenza del CSV
        assertEquals("Funding", righe.get(0)[2]);
        assertEquals("OKX", righe.get(0)[1]);
        assertEquals("100", righe.get(0)[14]);
    }

    @Test
    void tradingRicavaBuyESellDalSegnoDiBalChg() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"200","ccy":"BTC","balChg":"0.01","type":"2","subType":"1","ts":"1700000000000"},
             {"billId":"201","ccy":"USDT","balChg":"-350.5","type":"2","subType":"2","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(2, righe.size());
        //Buy/Sell si ricavano dal segno di balChg, non dal subType
        assertEquals("Buy", righe.get(0)[4]);
        assertEquals("Sell", righe.get(1)[4]);
        assertEquals("Trading", righe.get(0)[2]);
        //Le due gambe dello swap condividono l'orario: è ciò su cui si basa il raggruppamento
        assertEquals(righe.get(0)[0], righe.get(1)[0]);
    }

    /**
     * Il {@code type} 30 del conto Trading è uno scambio spot registrato con un codice diverso dal 2:
     * fino al 20/08/2026 restava non mappato e le sue righe venivano scartate dall'import, che così non
     * consolidava alcuno scambio. Le due gambe arrivano come sempre a coppie di segno opposto sullo
     * stesso istante — qui un esempio, SOL comprato con EUR.
     */
    @Test
    void ilCodice30DelTradingEUnoScambioComeIlCodice2() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"500","ccy":"SOL","balChg":"0.25","type":"30","ordId":"900","ts":"1700000000000"},
             {"billId":"501","ccy":"EUR","balChg":"-20","type":"30","ordId":"900","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(2, righe.size());
        assertEquals("Buy", righe.get(0)[4]);
        assertEquals("Sell", righe.get(1)[4]);
        //Le due gambe condividono orario e ordine: è ciò su cui si basano raggruppamento e suddivisione
        assertEquals(righe.get(0)[0], righe.get(1)[0]);
        assertEquals("900", righe.get(0)[13]);
        //Le etichette prodotte devono essere chiavi valide della mappa condivisa con l'import da CSV
        var mappa = Importazioni.Ex_OKX_MappaCausali();
        assertNotNull(mappa.get("Buy"));
        assertNotNull(mappa.get("Sell"));
    }

    /**
     * Lo scaricamento incompleto (difetto <b>C13</b>) non importa nulla, ma il resoconto delle causali
     * non riconosciute deve essere prodotto lo stesso: è l'unico modo in cui i codici {@code type} ancora
     * da decodificare vengono alla luce. {@code Ex_OKX_SoloCausaliSconosciute} è il percorso che lo
     * produce, e non deve toccare i movimenti.
     */
    @Test
    void loScaricamentoIncompletoProduceIlResocontoSenzaImportareNulla() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"600","ccy":"BTC","balChg":"0.5","type":"1","ts":"1700000000000"},
             {"billId":"601","ccy":"ETH","balChg":"1","type":"8","ts":"1700000001000"}]
            """), "Trading");

        int movimentiPrima = Principale.MappaCryptoWallet.size();
        Importazioni.Esito E = Importazioni.Ex_OKX_SoloCausaliSconosciute(righe, "OKX");

        assertNotNull(E, "con una causale non mappata il resoconto deve aprirsi");
        assertEquals(1, E.Sconosciute);
        assertTrue(E.MovimentiSconosciuti.contains("OKX type 8"));
        //Nessuna riga importata: il resoconto deve dirlo, non nasconderlo
        assertEquals(0, E.Aggiunte);
        assertEquals(0, E.Transazioni);
        assertEquals(0, E.Scartate);
        assertEquals(movimentiPrima, Principale.MappaCryptoWallet.size(),
                "la classificazione non deve scrivere nessun movimento");
        //La riga riconosciuta resta classificata, quella ignota no: è la stessa regola dell'import completo
        assertEquals("", righe.get(1)[3]);
        assertFalse(righe.get(0)[3].isEmpty());
    }

    /**
     * Senza causali sconosciute non si apre nessun resoconto: dopo l'avviso di scaricamento incompleto
     * una finestra con tutti zeri sarebbe solo rumore. {@code MostraResoconto} ignora il {@code null}.
     */
    @Test
    void senzaCausaliSconosciuteIlResocontoNonSiApre() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"700","ccy":"BTC","balChg":"0.5","type":"1","ts":"1700000000000"}]
            """), "Funding");

        assertNull(Importazioni.Ex_OKX_SoloCausaliSconosciute(righe, "OKX"));
    }

    /**
     * Il prodotto "ETH Staking" (funding {@code type} 137/138, non l'on-chain staking del 328): la
     * sottoscrizione (ETH in uscita) e la "swapping" di ritorno (BETH in entrata) arrivano sullo stesso
     * istante, come le gambe di uno scambio, e vanno entrambe classificate SCAMBIO CRYPTO-CRYPTO — è una
     * permuta ETH->BETH, non un giroconto interno.
     */
    @Test
    void ethStakingSubscriptionESwappingSonoUnoScambioEthBeth() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"800","ccy":"ETH","balChg":"-0.5","type":"137","ts":"1700000000000"},
             {"billId":"801","ccy":"BETH","balChg":"0.5","type":"138","ts":"1700000000000"}]
            """), "Funding");

        assertEquals(2, righe.size());
        assertEquals("ETH Staking subscription", righe.get(0)[4]);
        assertEquals("ETH Staking swapping", righe.get(1)[4]);
        assertEquals(righe.get(0)[0], righe.get(1)[0]);

        var mappa = Importazioni.Ex_OKX_MappaCausali();
        assertEquals("SCAMBIO CRYPTO-CRYPTO", mappa.get("ETH Staking subscription"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", mappa.get("ETH Staking swapping"));
    }

    /**
     * Il rendimento giornaliero dello stesso prodotto (funding {@code type} 139) riusa l'etichetta generica
     * "Staking earnings" già mappata a REWARD dal codice 328 (on-chain), invece di crearne una coppia
     * dedicata: è lo stesso trattamento fiscale, solo un prodotto OKX diverso.
     */
    @Test
    void ethStakingEarningsUsaLaStessaEtichettaDelloStakingOnChain() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"802","ccy":"BETH","balChg":"0.001","type":"139","ts":"1700000000000"}]
            """), "Funding");

        assertEquals(1, righe.size());
        assertEquals("Staking earnings", righe.get(0)[4]);
        assertEquals("REWARD", Importazioni.Ex_OKX_MappaCausali().get("Staking earnings"));
    }

    /** Le due posizioni "Spark USDC" chiuse da OKX nello stesso istante, come le restituisce OKX_Earn.js */
    private static final String STORICO_SPARK = """
        [{"ordId":"1001","ccy":"USDC","state":"3","protocol":"Spark USDC",
          "investData":[{"ccy":"USDC","amt":"100"}],
          "earningData":[{"ccy":"USDC","earningType":"0","realizedEarnings":"0.5"}],
          "purchasedTime":"1699900000000","redeemedTime":"1700000000000"},
         {"ordId":"1002","ccy":"USDC","state":"3","protocol":"Spark USDC",
          "investData":[{"ccy":"USDC","amt":"800"}],
          "earningData":[{"ccy":"USDC","earningType":"0","realizedEarnings":"3"}],
          "purchasedTime":"1699800000000","redeemedTime":"1700000000000"}]
        """;

    /** Il bill 330 dello stesso giorno: 900 LYUSDC accreditati un secondo prima del riscatto */
    private static final String BILL_LYUSDC = """
        [{"billId":"900001","ccy":"LYUSDC","balChg":"900","type":"330",
          "notes":"Liquid Staking swapping","ts":"1699999999000"}]
        """;

    /**
     * La conversione di una posizione On-chain Earn in un token di Liquid Staking e' una permuta come ETH ->
     * BETH, ma OKX registra solo il bill in entrata: il capitale era uscito dal Funding con il type 80, non
     * considerato. La gamba in uscita si ricostruisce dalle posizioni chiuse in quel momento, una riga per
     * posizione, sullo stesso istante del bill perche' lo scambio si raggruppa al secondo esatto.
     */
    @Test
    void laConversioneDiUnaPosizioneOnChainEarnDiventaUnoScambioCompleto() {
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        assertEquals("Liquid Staking swapping", righe.get(0)[4]);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", Importazioni.Ex_OKX_MappaCausali().get("Liquid Staking swapping"));

        CcxtInterop.AbbinaLiquidStakingOnChain(righe, bills(STORICO_SPARK));

        assertEquals(3, righe.size());
        java.math.BigDecimal uscita = java.math.BigDecimal.ZERO;
        for (String[] r : righe.subList(1, 3)) {
            assertEquals("USDC", r[5]);
            assertEquals("Liquid Staking swapping", r[4]);
            assertEquals(righe.get(0)[0], r[0], "stesso istante del bill, altrimenti lo scambio non si forma");
            assertTrue(r[14].startsWith("ONCHAIN-"));
            uscita = uscita.add(new java.math.BigDecimal(r[6]));
        }
        assertEquals(0, uscita.compareTo(new java.math.BigDecimal("-900")),
                "esce il capitale delle posizioni, non i rendimenti: quelli arrivano con il bill 328");
        assertEquals(java.util.Set.of("ONCHAIN-1001", "ONCHAIN-1002"),
                java.util.Set.of(righe.get(1)[14], righe.get(2)[14]));
    }

    /** Gli ID delle gambe ricostruite sono deterministici: uno scaricamento ripetuto produce lo stesso scambio */
    @Test
    void lAbbinamentoRipetutoDaGliStessiIdentificativi() {
        List<String[]> prima = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        List<String[]> dopo = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        CcxtInterop.AbbinaLiquidStakingOnChain(prima, bills(STORICO_SPARK));
        CcxtInterop.AbbinaLiquidStakingOnChain(dopo, bills(STORICO_SPARK));
        assertEquals(prima.size(), dopo.size());
        for (int i = 0; i < prima.size(); i++) {
            assertArrayEquals(prima.get(i), dopo.get(i));
        }
    }

    /**
     * Senza le posizioni di origine il token non deve entrare come
     * accredito senza contropartita: resta fra gli sconosciuti del resoconto.
     */
    @Test
    void senzaPosizioneDiOrigineIlTokenRestaFraGliSconosciuti() {
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        CcxtInterop.AbbinaLiquidStakingOnChain(righe, null);

        assertEquals(1, righe.size());
        assertEquals(CcxtInterop.CAUSALE_LIQUID_STAKING_SENZA_ORIGINE, righe.get(0)[4]);
        assertNull(Importazioni.Ex_OKX_MappaCausali().get(righe.get(0)[4]));
        Importazioni.Esito E = Importazioni.Ex_OKX_SoloCausaliSconosciute(righe, "OKX");
        assertNotNull(E);
        assertEquals(1, E.Sconosciute);
    }

    /** Sottoscrizione diretta di Liquid Staking: 329 in uscita e 330 in entrata nello stesso secondo */
    private static final String BILL_SOTTOSCRIZIONE_OKSOL = """
        [{"billId":"900002","ccy":"SOL","balChg":"-0.5","type":"329",
          "notes":"Liquid Staking subscription","ts":"1700000000000"},
         {"billId":"900003","ccy":"OKSOL","balChg":"0.5","type":"330",
          "notes":"Liquid Staking swapping","ts":"1700000000000"}]
        """;

    /**
     * La sottoscrizione diretta di un prodotto di Liquid Staking porta <b>tutte e due</b> le gambe: la moneta in
     * uscita e' il bill 329. Non va cercata nessuna posizione On-chain Earn (non ce n'e', e prima della mappatura
     * del 329 il 330 finiva fra gli sconosciuti insieme a lui): le due righe sono gia' uno scambio SOL -> OKSOL.
     */
    @Test
    void laSottoscrizioneDirettaDiLiquidStakingPortaGiaLaGambaInUscita() {
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_SOTTOSCRIZIONE_OKSOL), "Funding"));
        assertEquals(2, righe.size());
        for (String[] r : righe) {
            assertEquals("Liquid Staking swapping", r[4]);
        }
        List<String[]> nonAbbinati = CcxtInterop.AbbinaLiquidStakingOnChain(righe, null);
        assertTrue(nonAbbinati.isEmpty(), "la gamba in uscita c'e' gia', nessuna ricostruzione e nessun avviso");
        assertEquals(2, righe.size(), "nessuna gamba ricostruita in piu'");

        Map<String, String> mappa = Importazioni.Ex_OKX_MappaCausali();
        List<List<String[]>> gruppi = Importazioni.Ex_OKX_Raggruppa(Importazioni.Ex_OKX_Ordina(righe));
        assertEquals(1, gruppi.size(), "stesso istante: un solo scambio");
        for (String[] r : gruppi.get(0)) {
            assertEquals("SCAMBIO CRYPTO-CRYPTO", mappa.get(r[4]));
        }
    }

    /**
     * Lo storico completo degli ordini On-chain Earn si chiede solo se c'e' una conversione da abbinare: senza,
     * lo script fa una sola chiamata come prima della paginazione.
     */
    @Test
    void loStoricoOrdiniCompletoSiChiedeSoloConUnaConversioneDaAbbinare() {
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        assertTrue(CcxtInterop.ContieneLiquidStaking(righe));

        //Gia' abbinata: la gamba in uscita c'e', lo storico non serve piu' ma il bill in entrata resta
        CcxtInterop.AbbinaLiquidStakingOnChain(righe, bills(STORICO_SPARK));
        assertTrue(CcxtInterop.ContieneLiquidStaking(righe));

        //Nessuna conversione fra i bill
        List<String[]> senza = new java.util.ArrayList<>(righe);
        senza.removeIf(r -> CcxtInterop.CAUSALE_LIQUID_STAKING_SWAPPING.equals(r[4]));
        assertFalse(CcxtInterop.ContieneLiquidStaking(senza));
        assertFalse(CcxtInterop.ContieneLiquidStaking(null));
    }

    /**
     * Le conversioni senza posizione di origine tornano al chiamante, che avvisa l'utente: l'avviso cita data,
     * quantita' e token, e dice se lo storico ordini era completo, perche' la causa cambia.
     */
    @Test
    void leConversioniNonAbbinateVengonoSegnalate() {
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        List<String[]> abbinate = CcxtInterop.AbbinaLiquidStakingOnChain(
                new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding")), bills(STORICO_SPARK));
        assertTrue(abbinate.isEmpty());

        List<String[]> nonAbbinati = CcxtInterop.AbbinaLiquidStakingOnChain(righe, null);
        assertEquals(1, nonAbbinati.size());
        assertSame(righe.get(0), nonAbbinati.get(0));

        String completo = CcxtInterop.TestoAvvisoLiquidStakingNonAbbinati(nonAbbinati, true);
        String interrotto = CcxtInterop.TestoAvvisoLiquidStakingNonAbbinati(nonAbbinati, false);
        for (String testo : List.of(completo, interrotto)) {
            assertTrue(testo.contains(righe.get(0)[0]), testo);
            assertTrue(testo.contains("900 LYUSDC"), testo);
            assertFalse(testo.contains(";"), testo);
        }
        assertTrue(completo.contains("scaricato per intero"));
        assertTrue(interrotto.contains("non è stato scaricato fino in fondo"));
    }

    /** Posizioni che non tornano esattamente con la quantita' ricevuta, o chiuse in un altro momento, non si abbinano */
    @Test
    void lePosizioniCheNonTornanoNonVengonoAbbinate() {
        //Solo una delle due posizioni: la somma non torna
        String unaSola = STORICO_SPARK.substring(0, STORICO_SPARK.indexOf("},\n") + 1) + "]";
        List<String[]> righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        CcxtInterop.AbbinaLiquidStakingOnChain(righe, bills(unaSola));
        assertEquals(1, righe.size());
        assertEquals(CcxtInterop.CAUSALE_LIQUID_STAKING_SENZA_ORIGINE, righe.get(0)[4]);

        //Riscattate un'ora dopo
        String unOraDopo = STORICO_SPARK.replace("1700000000000", "1700003600000");
        righe = new java.util.ArrayList<>(CcxtInterop.convertOKXBills(bills(BILL_LYUSDC), "Funding"));
        CcxtInterop.AbbinaLiquidStakingOnChain(righe, bills(unOraDopo));
        assertEquals(1, righe.size());
        assertEquals(CcxtInterop.CAUSALE_LIQUID_STAKING_SENZA_ORIGINE, righe.get(0)[4]);
    }

    /**
     * Il 30 vale solo sul conto Trading. Sul Funding i codici bassi hanno tutt'altro significato — il 2 è
     * un prelievo, non una vendita — quindi un 30 che arrivasse di là deve restare non mappato e finire
     * nel riepilogo dei movimenti sconosciuti invece di essere scambiato per un acquisto.
     */
    @Test
    void ilCodice30NonVieneInterpretatoSulContoFunding() {
        assertEquals("OKX type 30", CcxtInterop.causaleBillOKX("30", "0.25", false));
    }

    /**
     * Il 27 del conto Trading è la conversione rapida (USDT -> USDC): due bill di segno opposto sullo stesso
     * istante, senza ordine, che devono avere l'etichetta {@code Convert} (scambio cripto/cripto) e non
     * restare fra i movimenti sconosciuti. Sul Funding il 27 non è elencato e resta non mappato.
     */
    @Test
    void ilCodice27DelContoTradingEUnaConversione() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"700","ccy":"USDT","balChg":"-500","type":"27","ts":"1700000000000"},
             {"billId":"701","ccy":"USDC","balChg":"500","type":"27","ts":"1700000000000"}]
            """), "Trading");
        assertEquals(2, righe.size());
        assertEquals("Convert", righe.get(0)[4]);
        assertEquals("Convert", righe.get(1)[4]);
        assertEquals("-500", righe.get(0)[6]);
        assertEquals("500", righe.get(1)[6]);
        assertEquals("OKX type 27", CcxtInterop.causaleBillOKX("27", "1", false));
    }

    /**
     * Regressione della classe di bug <b>M7</b>: una quantità positiva molto piccola viene stampata da
     * {@code BigDecimal} in notazione scientifica ({@code 2.5E-9}) e contiene quindi un trattino. Deve
     * comunque essere riconosciuta come <b>entrata</b>, altrimenti la classificazione fiscale si inverte.
     */
    @Test
    void quantitaPositivaInNotazioneScientificaRestaUnAcquisto() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"300","ccy":"SHIB","balChg":"2.5E-9","type":"2","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        assertEquals("Buy", righe.get(0)[4], "una quantità positiva in notazione scientifica non è una vendita");
    }

    @Test
    void quantitaNegativaInNotazioneScientificaRestaUnaVendita() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"301","ccy":"SHIB","balChg":"-2.5E-9","type":"2","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        assertEquals("Sell", righe.get(0)[4]);
    }

    /**
     * I codici dei giroconti interni a OKX, decodificati confrontando i bill scaricati via API con le
     * righe degli export CSV dello stesso periodo. Devono finire tutti su {@code NON CONSIDERARE}: sono
     * spostamenti fra conti dello stesso utente e non vanno importati.
     */
    @Test
    void iGirocontiInterniNonVengonoImportati() {
        var mappa = Importazioni.Ex_OKX_MappaCausali();

        //Trading: 1 = Transfer in/out fra Funding e Trading, 12 = Transfer senza azione
        for (String tipo : new String[]{"1", "12"}) {
            assertEquals("Transfer out", CcxtInterop.causaleBillOKX(tipo, "-1", true), "Trading type " + tipo);
            assertEquals("Transfer in",  CcxtInterop.causaleBillOKX(tipo, "1",  true), "Trading type " + tipo);
        }
        //Funding: 130 = From unified trading account, 131 = To unified, 76 = Simple Earn redemption,
        //80/82 = sottoscrizione e riscatto dei Flash Deals, 311 = accredito dal conto Trading
        for (String tipo : new String[]{"76", "80", "82", "130", "131", "311"}) {
            assertEquals("Transfer out", CcxtInterop.causaleBillOKX(tipo, "-1", false), "Funding type " + tipo);
            assertEquals("Transfer in",  CcxtInterop.causaleBillOKX(tipo, "1",  false), "Funding type " + tipo);
        }
        assertEquals("NON CONSIDERARE", mappa.get("Transfer in"));
        assertEquals("NON CONSIDERARE", mappa.get("Transfer out"));
    }

    /**
     * {@code Received} (Funding, codice 48) è un accredito che arriva da un altro account OKX: crypto che
     * entra davvero nel wallet, quindi vale come un deposito e non come un giroconto interno.
     */
    @Test
    void lAccreditoDaAltroAccountValeComeDeposito() {
        assertEquals("Received", CcxtInterop.causaleBillOKX("48", "10.31", false));
        assertEquals("TRASFERIMENTO-CRYPTO", Importazioni.Ex_OKX_MappaCausali().get("Received"));
    }

    /**
     * I codici 326/327 compaiono solo a coppie di segno opposto sulla stessa moneta e sullo stesso istante,
     * senza riga corrispondente in alcun export CSV: sono spostamenti fra wallet dello stesso utente e
     * vanno quindi esclusi dall'importazione come gli altri giroconti interni.
     */
    @Test
    void iCodiciDelloSpostamentoFraWalletSonoGirocontiInterni() {
        assertEquals("Transfer out", CcxtInterop.causaleBillOKX("326", "-0.001", false));
        assertEquals("Transfer in", CcxtInterop.causaleBillOKX("327", "0.001", false));
        var mappa = Importazioni.Ex_OKX_MappaCausali();
        assertEquals("NON CONSIDERARE", mappa.get("Transfer out"));
        assertEquals("NON CONSIDERARE", mappa.get("Transfer in"));
    }

    /**
     * La conversione rapida di OKX (codice 61) è uno scambio fra due monete, non un movimento a sé: le due
     * gambe arrivano sullo stesso istante con segno opposto, quindi devono portare l'etichetta che
     * l'import del CSV già conosce, altrimenti il raggruppamento per orario non le riconoscerebbe.
     */
    @Test
    void laConversioneRapidaEUnoScambioFraDueMonete() {
        //Le due gambe: SHIB in uscita, USDT in entrata
        assertEquals("Convert", CcxtInterop.causaleBillOKX("61", "-10000", false));
        assertEquals("Convert", CcxtInterop.causaleBillOKX("61", "0.07", false));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", Importazioni.Ex_OKX_MappaCausali().get("Convert"));
    }

    /**
     * Gli accrediti gratuiti del Funding: il rendimento dei Flash Deals (89), che arriva in una moneta
     * diversa da quella investita, e l'omaggio promozionale della mystery box (189). Sono crypto che entra
     * senza contropartita e vanno trattati come gli interessi di Simple Earn, non come giroconti.
     */
    @Test
    void iRendimentiEGliOmaggiSonoReward() {
        var mappa = Importazioni.Ex_OKX_MappaCausali();

        assertEquals("Flash Deals Earnings", CcxtInterop.causaleBillOKX("89", "10", false));
        assertEquals("REWARD", mappa.get("Flash Deals Earnings"));

        assertEquals("Mystery box bonus", CcxtInterop.causaleBillOKX("189", "5000", false));
        assertEquals("REWARD", mappa.get("Mystery box bonus"));
    }

    @Test
    void codiciNonMappatiRestanoNonClassificatiEVisibili() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"400","ccy":"ETH","balChg":"1","type":"8","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        //Non viene inventata una causale: il codice grezzo resta visibile e finirà tra i movimenti sconosciuti
        assertEquals("OKX type 8", righe.get(0)[4]);
        assertNull(Importazioni.Ex_OKX_MappaCausali().get(righe.get(0)[4]),
                "una causale non mappata non deve corrispondere ad alcuna categoria interna");
    }

    @Test
    void leCausaliProdotteSonoQuelleDellaMappaCondivisaConIlCSV() {
        //È il punto del riuso: le etichette emesse dal ramo API devono essere chiavi valide della
        //stessa mappa causali usata dall'import del CSV, altrimenti le due strade divergerebbero.
        var mappa = Importazioni.Ex_OKX_MappaCausali();
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.get("deposit"));
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.get("withdrawal"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", mappa.get("Buy"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", mappa.get("Sell"));
    }

    @Test
    void iBillSenzaVariazioneDiSaldoNonSonoMovimenti() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"500","ccy":"ETH","balChg":"0","type":"1","ts":"1700000000000"},
             {"billId":"501","ccy":"ETH","balChg":"0.000","type":"1","ts":"1700000000000"}]
            """), "Funding");

        assertTrue(righe.isEmpty(), "un bill che non muove il saldo non deve produrre un movimento");
    }

    @Test
    void leRigheIncompleteVengonoScartateSenzaEccezioni() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"600","balChg":"1","type":"1","ts":"1700000000000"},
             {"billId":"601","ccy":"ETH","type":"1","ts":"1700000000000"},
             {"billId":"602","ccy":"ETH","balChg":"non-numerico","type":"1","ts":"1700000000000"},
             {"billId":"603","ccy":"ETH","balChg":"1","type":"1","ts":"1700000000000"}]
            """), "Funding");

        assertEquals(1, righe.size());
        assertEquals("603", righe.get(0)[14]);
    }

    @Test
    void ilMovimentoOppostoNonVieneGeneratoSuiTrasferimentiInterni() {
        //Funding e Trading sono scaricati nella stessa esecuzione, quindi un trasferimento interno
        //compare già come bill su entrambi i conti: generare anche l'opposto lo conterebbe due volte.
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"700","ccy":"ETH","balChg":"1","type":"1","ts":"1700000000000"}]
            """), "Funding");

        assertEquals("NO", righe.get(0)[15]);
    }

    /**
     * Le commissioni vanno <b>scorporate</b>: {@code balChg} è il netto, e importarlo così com'è
     * equivarrebbe a dedurre la commissione dal costo di carico, cosa che l'art. 68 c.9-bis del TUIR non
     * consente per le cripto-attività. La quantità deve quindi tornare al lordo e la fee deve viaggiare nei
     * campi {@code [11]}/{@code [12]}, da cui {@code Ex_OKX_Consolida} genera il movimento COMMISSIONI.
     */
    @Test
    void laFeeVieneScorporataELaQuantitaTornaAlLordo() {
        //Esempio: sz 0.01 - fee 0.00001 = balChg 0.00999
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"800","ccy":"ETH","balChg":"0.00999","type":"2","sz":"0.01",
              "fee":"-0.00001","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        //[6] è il lordo, cioè esattamente sz
        assertEquals(0, new java.math.BigDecimal("0.01").compareTo(new java.math.BigDecimal(righe.get(0)[6])));
        assertEquals("ETH", righe.get(0)[11]);
        //La fee è negativa: è un'uscita, e come tale la legge Ex_OKX_Consolida
        assertEquals(0, new java.math.BigDecimal("-0.00001").compareTo(new java.math.BigDecimal(righe.get(0)[12])));
    }

    /**
     * Il lordo si ricava da {@code balChg - fee} e non da {@code sz}, perché il segno di {@code sz} non è
     * affidabile su tutti i tipi di bill mentre {@code balChg} porta sempre il verso reale del movimento.
     * Sulla gamba in uscita OKX non addebita fee, quindi lordo e netto coincidono.
     */
    @Test
    void sullaGambaInUscitaSenzaFeeLaQuantitaRestaQuellaDiBalChg() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"801","ccy":"USDC","balChg":"-20","type":"2","sz":"20",
              "fee":"0","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        assertEquals(0, new java.math.BigDecimal("-20").compareTo(new java.math.BigDecimal(righe.get(0)[6])));
        //Fee nulla: i campi restano vuoti, altrimenti nascerebbe un movimento di commissione fantasma
        assertEquals("", righe.get(0)[11].trim());
        assertEquals("", righe.get(0)[12].trim());
    }

    /** I bill del Funding non hanno affatto il campo {@code fee}: la sua assenza non deve rompere nulla. */
    @Test
    void unBillSenzaCampoFeeVieneImportatoComunque() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"802","ccy":"BTC","balChg":"0.5","type":"1","ts":"1700000000000"}]
            """), "Funding");

        assertEquals(1, righe.size());
        assertEquals(0, new java.math.BigDecimal("0.5").compareTo(new java.math.BigDecimal(righe.get(0)[6])));
        assertEquals("", righe.get(0)[12].trim());
    }

    /**
     * L'avviso sullo storico troppo vecchio non deve scattare quando non serve. Sono coperti solo i due casi
     * che ritornano prima di aprire la finestra: quelli che la aprono non sono verificabili senza interfaccia.
     */
    @Test
    void lAvvisoSulloStoricoNonScattaQuandoNonServe() {
        long ieri = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
        long dueAnniFa = System.currentTimeMillis() - 730L * 24 * 60 * 60 * 1000;

        long adesso = System.currentTimeMillis();

        //Riguarda solo OKX: gli altri exchange non hanno questo limite di storico
        assertFalse(CcxtInterop.serveDialogoStoricoOKX("Binance", dueAnniFa, adesso, false, false));
        assertFalse(CcxtInterop.serveDialogoStoricoOKX(null, dueAnniFa, adesso, false, true));
        //Entro la finestra coperta dalle API, e con la scelta gia' proposta, non si disturba l'utente
        assertFalse(CcxtInterop.serveDialogoStoricoOKX("OKX", ieri, adesso, true, false));
        //Ma la prima volta la scelta va offerta anche con movimenti recenti: e' il caso di chi ha
        //importato solo una parte della propria storia e ha un buco dietro
        assertTrue(CcxtInterop.serveDialogoStoricoOKX("OKX", ieri, adesso, false, false));
        //Da lontano si chiede comunque, anche se e' gia' stata proposta
        assertTrue(CcxtInterop.serveDialogoStoricoOKX("OKX", dueAnniFa, adesso, true, false));
        //E con dei trimestri ancora da ritirare si torna a chiedere anche da vicino: e' l'unica via
        //rimasta per completare un recupero parziale, da quando la data di partenza non si sceglie piu'
        assertTrue(CcxtInterop.serveDialogoStoricoOKX("OKX", ieri, adesso, true, true));

        //Quando il dialogo non serve, la data proposta arriva intatta al chiamante
        assertEquals(CcxtInterop.SceltaStorico.PROCEDI,
                CcxtInterop.SceltaStoricoOKX("Binance", dueAnniFa, null).scelta());
        assertEquals(dueAnniFa, CcxtInterop.SceltaStoricoOKX("Binance", dueAnniFa, null).startDate());
    }

    /**
     * Il tetto di pagine di {@code OKX_Bills.js} non è un guasto ma un budget di tempo (~12 minuti per
     * conto), e da quando uno scaricamento incompleto non importa più nulla era diventato un vicolo cieco:
     * non importando, la data di partenza non avanza e la corsa dopo trova lo stesso muro. Il cursore di
     * ripresa è la via d'uscita — questi test coprono la parte pura, cioè come viene ricordato.
     */
    @Test
    void ilCursoreDiRipresaFaAndataERitornoSenzaPerdereNulla() {
        String salvato = "Funding=1700000000000|1600000000000;Trading=123456789|1780000000000";
        var riprese = CcxtInterop.ripreseOKX(salvato);

        assertEquals(2, riprese.size());
        //L'after non viene interpretato: sul Funding è un timestamp, sul Trading un billId (difetto C8)
        assertEquals("1700000000000", riprese.get("Funding").after());
        assertEquals(1600000000000L, riprese.get("Funding").limite());
        assertEquals("123456789", riprese.get("Trading").after());

        assertEquals(salvato, CcxtInterop.ripreseOKX(riprese));
    }

    @Test
    void unaRipresaMalformataVieneIgnorataInveceDiSporcareLaRichiesta() {
        //L'opzione è testo su un file dell'utente: una riga sporca non deve tradursi in una richiesta
        //assurda a OKX, e soprattutto non deve far saltare le voci buone che le stanno accanto
        var riprese = CcxtInterop.ripreseOKX("Funding=|123;=1|2;Trading=abc|nonUnNumero;Trading=999|5;spazzatura");

        assertEquals(1, riprese.size());
        assertEquals("999", riprese.get("Trading").after());
        assertTrue(CcxtInterop.ripreseOKX((String) null).isEmpty());
        assertEquals("", CcxtInterop.ripreseOKX(new java.util.TreeMap<String, CcxtInterop.RipresaOKX>()));
    }

    @Test
    void laRipresaVieneLettaDallaRispostaDelloScript() {
        var json = com.google.gson.JsonParser.parseString("""
            {"okx_completo":false,
             "okx_motivo":{"Funding":"tetto_pagine","Trading":""},
             "okx_ripresa":{"Funding":{"after":"1700000000000","limite":1600000000000}}}
            """).getAsJsonObject();

        var riprese = CcxtInterop.ripreseOKX(json);
        assertEquals(1, riprese.size());
        assertEquals("1700000000000", riprese.get("Funding").after());
        //Solo il tetto: si importa comunque e si ricorda dove si era arrivati
        assertTrue(CcxtInterop.soloTettoPagineOKX(json));
    }

    /**
     * Il tetto è l'unica incompletezza da cui si sa ripartire. Ogni altro motivo dice che manca
     * <i>qualcosa</i> senza dire <i>che cosa</i>, e lì deve restare il blocco di C13.
     */
    @Test
    void soloIlTettoDiPagineFaImportareUnoScaricamentoIncompleto() {
        assertFalse(CcxtInterop.soloTettoPagineOKX(com.google.gson.JsonParser.parseString("""
            {"okx_motivo":{"Funding":"tetto_pagine","Trading":"paginazione_ferma"}}
            """).getAsJsonObject()), "un'anomalia accanto al tetto deve continuare a bloccare");

        assertFalse(CcxtInterop.soloTettoPagineOKX(com.google.gson.JsonParser.parseString("""
            {"okx_motivo":{"Funding":"","Trading":""}}
            """).getAsJsonObject()), "senza alcun motivo non c'è nessun tetto da riprendere");

        //Una risposta di uno script più vecchio non porta il campo: non va scambiata per un tetto
        assertFalse(CcxtInterop.soloTettoPagineOKX(com.google.gson.JsonParser.parseString(
                "{\"okx_completo\":false}").getAsJsonObject()));
        assertFalse(CcxtInterop.soloTettoPagineOKX(null));
        assertTrue(CcxtInterop.ripreseOKX((com.google.gson.JsonObject) null).isEmpty());
    }

    @Test
    void unArrayNulloProduceUnaListaVuota() {
        assertTrue(CcxtInterop.convertOKXBills(null, "Funding").isEmpty());
        assertTrue(CcxtInterop.convertOKXEarn(null).isEmpty());
    }

    /**
     * Gli interessi di OKX Simple Earn sono accreditati <b>ogni ora</b>, una riga per moneta: vanno sommati per
     * giornata, altrimenti si otterrebbero circa 72 movimenti al giorno. I record qui sotto hanno la stessa
     * forma di quelli restituiti davvero da {@code finance/savings/lending-history}.
     */
    @Test
    void gliInteressiOrariVengonoSommatiPerGiornataEMoneta() {
        //Tre accrediti ETH e due BTC nella stessa giornata (2023-11-14, ora italiana)
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"ETH","amt":"0.2","earnings":"0.001","rate":"0.06","ts":"1699963200000"},
             {"ccy":"ETH","amt":"0.2","earnings":"0.001","rate":"0.06","ts":"1699966800000"},
             {"ccy":"ETH","amt":"0.2","earnings":"0.002","rate":"0.06","ts":"1699970400000"},
             {"ccy":"BTC","amt":"0.01","earnings":"0.0001","rate":"0.03","ts":"1699963183000"},
             {"ccy":"BTC","amt":"0.01","earnings":"0.0002","rate":"0.03","ts":"1699966783000"}]
            """));

        assertEquals(2, righe.size(), "una riga per moneta, non una per accredito");

        String[] btc = righe.stream().filter(r -> r[5].equals("BTC")).findFirst().orElseThrow();
        String[] eth = righe.stream().filter(r -> r[5].equals("ETH")).findFirst().orElseThrow();

        assertEquals("0.0003", btc[6]);
        assertEquals("0.004", eth[6]);
        //Causale già presente nella mappa condivisa: nessuna nuova categoria da introdurre
        assertEquals("Deposit yield", eth[4]);
        assertEquals("REWARD", Importazioni.Ex_OKX_MappaCausali().get("Deposit yield"));
        //ID deterministico: un secondo scaricamento sullo stesso periodo deve riconoscere la giornata
        assertEquals("EARN-ETH-20231114", eth[14]);
        assertEquals("EARN-BTC-20231114", btc[14]);
        //Il movimento porta la data dell'ultimo accredito della giornata
        assertEquals(FunzioniDate.ConvertiDatadaLongAlSecondo(1699970400000L), eth[0]);
    }

    @Test
    void giornateDiverseRestanoMovimentiDiversi() {
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"USDC","earnings":"0.001","ts":"1699963200000"},
             {"ccy":"USDC","earnings":"0.002","ts":"1700049600000"}]
            """));

        assertEquals(2, righe.size());
        assertNotEquals(righe.get(0)[14], righe.get(1)[14]);
    }

    /**
     * La giornata in corso è ancora incompleta: importarla adesso significherebbe congelarne una parte,
     * perché l'ID è deterministico e la deduplica impedirebbe poi di completarla.
     */
    @Test
    void laGiornataInCorsoNonVieneImportata() {
        long ora = System.currentTimeMillis();
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills(
                "[{\"ccy\":\"ETH\",\"earnings\":\"0.001\",\"ts\":\"" + ora + "\"}]"));

        assertTrue(righe.isEmpty(), "la giornata odierna va importata solo quando è completa");
    }

    /**
     * I record degli interessi non hanno un id, e non è verificato che l'endpoint onori la paginazione: se
     * una pagina venisse restituita due volte, la somma della giornata risulterebbe gonfiata senza che nulla
     * lo segnali. Moneta + timestamp è l'unica chiave disponibile per accorgersene.
     */
    @Test
    void unAccreditoRipetutoNonVieneContatoDueVolte() {
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"ETH","earnings":"0.001","ts":"1699963200000"},
             {"ccy":"ETH","earnings":"0.001","ts":"1699963200000"},
             {"ccy":"ETH","earnings":"0.001","ts":"1699966800000"}]
            """));

        assertEquals(1, righe.size());
        assertEquals("0.002", righe.get(0)[6], "il doppione non deve entrare nella somma");
    }

    @Test
    void gliAccreditiNulliOMalformatiNonProduconoMovimenti() {
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"ETH","earnings":"0","ts":"1699963200000"},
             {"ccy":"ETH","ts":"1699963200000"},
             {"ccy":"ETH","earnings":"non-numerico","ts":"1699963200000"},
             {"earnings":"0.001","ts":"1699963200000"}]
            """));

        assertTrue(righe.isEmpty());
    }

    /**
     * Deduplica delle righe grezze: è il rimedio al difetto C6 di
     * {@code Documentazione/Analisi_Bug_Criticita.md}. Lo scaricamento con archivio storico unisce due
     * finestre che si sovrappongono ({@code account/bills-archive} copre ~3 mesi, l'archivio copre il
     * trimestre intero), quindi le operazioni comuni arrivano due volte con lo stesso {@code billId}.
     */
    @Test
    void loStessoBillArrivatoDaDueFinestreEntraUnaVoltaSola() {
        //Le due gambe di uno scambio spot, prese una volta da bills-archive e una dall'archivio trimestrale
        String[] usdcUscita = new String[19];
        usdcUscita[2] = "Trading"; usdcUscita[5] = "USDC"; usdcUscita[6] = "-30"; usdcUscita[14] = "900100";
        String[] btcEntrata = new String[19];
        btcEntrata[2] = "Trading"; btcEntrata[5] = "BTC"; btcEntrata[6] = "0.0005"; btcEntrata[14] = "900101";

        List<String[]> righe = CcxtInterop.deduplicaBillOKX(List.of(
                usdcUscita, btcEntrata, usdcUscita.clone(), btcEntrata.clone()));

        assertEquals(2, righe.size(), "ogni bill deve restare una riga sola");
        //Si tiene la prima occorrenza, cioè quella dello scaricamento ordinario, e l'ordine non cambia
        assertSame(usdcUscita, righe.get(0));
        assertSame(btcEntrata, righe.get(1));
    }

    @Test
    void bilanciConIDugualeMaContiDiversiRestanoDistinti() {
        //I due endpoint numerano i bill in modo indipendente: la collisione fra conti non è una duplicazione
        String[] funding = new String[19];
        funding[2] = "Funding"; funding[14] = "900200";
        String[] trading = new String[19];
        trading[2] = "Trading"; trading[14] = "900200";

        assertEquals(2, CcxtInterop.deduplicaBillOKX(List.of(funding, trading)).size());
    }

    @Test
    void leRigheSenzaIDvengonoSempreTenute() {
        //Senza identificativo un doppione non è riconoscibile: scartarle perderebbe movimenti veri
        String[] senzaId1 = new String[19];
        senzaId1[2] = "Trading"; senzaId1[5] = "BTC"; senzaId1[14] = "";
        String[] senzaId2 = new String[19];
        senzaId2[2] = "Trading"; senzaId2[5] = "ETH"; senzaId2[14] = "";

        assertEquals(2, CcxtInterop.deduplicaBillOKX(List.of(senzaId1, senzaId2)).size());
        assertTrue(CcxtInterop.deduplicaBillOKX(List.of()).isEmpty());
    }
}
