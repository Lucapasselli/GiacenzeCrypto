package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
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
        //Funding: 130 = From unified trading account, 131 = To unified, 76 = Simple Earn redemption
        for (String tipo : new String[]{"76", "130", "131"}) {
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
        assertEquals("Transfer out", CcxtInterop.causaleBillOKX("326", "-0.0000331", false));
        assertEquals("Transfer in", CcxtInterop.causaleBillOKX("327", "0.0000331", false));
        var mappa = Importazioni.Ex_OKX_MappaCausali();
        assertEquals("NON CONSIDERARE", mappa.get("Transfer out"));
        assertEquals("NON CONSIDERARE", mappa.get("Transfer in"));
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
        //Caso reale: sz 0.010711 - fee 0.000007229925 = balChg 0.010703770075
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"800","ccy":"ETH","balChg":"0.010703770075","type":"2","sz":"0.010711",
              "fee":"-0.000007229925","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        //[6] è il lordo, cioè esattamente sz
        assertEquals(0, new java.math.BigDecimal("0.010711").compareTo(new java.math.BigDecimal(righe.get(0)[6])));
        assertEquals("ETH", righe.get(0)[11]);
        //La fee è negativa: è un'uscita, e come tale la legge Ex_OKX_Consolida
        assertEquals(0, new java.math.BigDecimal("-0.000007229925").compareTo(new java.math.BigDecimal(righe.get(0)[12])));
    }

    /**
     * Il lordo si ricava da {@code balChg - fee} e non da {@code sz}, perché il segno di {@code sz} non è
     * affidabile su tutti i tipi di bill mentre {@code balChg} porta sempre il verso reale del movimento.
     * Sulla gamba in uscita OKX non addebita fee, quindi lordo e netto coincidono.
     */
    @Test
    void sullaGambaInUscitaSenzaFeeLaQuantitaRestaQuellaDiBalChg() {
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"801","ccy":"USDC","balChg":"-19.58924079","type":"2","sz":"19.58924079",
              "fee":"0","ts":"1700000000000"}]
            """), "Trading");

        assertEquals(1, righe.size());
        assertEquals(0, new java.math.BigDecimal("-19.58924079").compareTo(new java.math.BigDecimal(righe.get(0)[6])));
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

        //Riguarda solo OKX: gli altri exchange non hanno questo limite di storico
        assertTrue(CcxtInterop.ConfermaFinestraStoricoOKX("Binance", dueAnniFa, null));
        //Entro la finestra coperta dalle API si procede senza disturbare l'utente
        assertTrue(CcxtInterop.ConfermaFinestraStoricoOKX("OKX", ieri, null));
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
        //Tre accrediti ETH e due BTC nella stessa giornata (2026-08-01, ora italiana)
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"ETH","amt":"0.2","earnings":"0.00000136","rate":"0.06","ts":"1785535244000"},
             {"ccy":"ETH","amt":"0.2","earnings":"0.00000136","rate":"0.06","ts":"1785538844000"},
             {"ccy":"ETH","amt":"0.2","earnings":"0.00000100","rate":"0.06","ts":"1785542444000"},
             {"ccy":"BTC","amt":"0.01","earnings":"0.00000003","rate":"0.03","ts":"1785535227000"},
             {"ccy":"BTC","amt":"0.01","earnings":"0.00000004","rate":"0.03","ts":"1785538827000"}]
            """));

        assertEquals(2, righe.size(), "una riga per moneta, non una per accredito");

        String[] btc = righe.stream().filter(r -> r[5].equals("BTC")).findFirst().orElseThrow();
        String[] eth = righe.stream().filter(r -> r[5].equals("ETH")).findFirst().orElseThrow();

        assertEquals("0.00000007", btc[6]);
        assertEquals("0.00000372", eth[6]);
        //Causale già presente nella mappa condivisa: nessuna nuova categoria da introdurre
        assertEquals("Deposit yield", eth[4]);
        assertEquals("REWARD", Importazioni.Ex_OKX_MappaCausali().get("Deposit yield"));
        //ID deterministico: un secondo scaricamento sullo stesso periodo deve riconoscere la giornata
        assertEquals("EARN-ETH-20260801", eth[14]);
        assertEquals("EARN-BTC-20260801", btc[14]);
        //Il movimento porta la data dell'ultimo accredito della giornata
        assertEquals(FunzioniDate.ConvertiDatadaLongAlSecondo(1785542444000L), eth[0]);
    }

    @Test
    void giornateDiverseRestanoMovimentiDiversi() {
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"USDC","earnings":"0.001","ts":"1785535244000"},
             {"ccy":"USDC","earnings":"0.002","ts":"1785621644000"}]
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
                "[{\"ccy\":\"ETH\",\"earnings\":\"0.00000136\",\"ts\":\"" + ora + "\"}]"));

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
            [{"ccy":"ETH","earnings":"0.00000136","ts":"1785535244000"},
             {"ccy":"ETH","earnings":"0.00000136","ts":"1785535244000"},
             {"ccy":"ETH","earnings":"0.00000136","ts":"1785538844000"}]
            """));

        assertEquals(1, righe.size());
        assertEquals("0.00000272", righe.get(0)[6], "il doppione non deve entrare nella somma");
    }

    @Test
    void gliAccreditiNulliOMalformatiNonProduconoMovimenti() {
        List<String[]> righe = CcxtInterop.convertOKXEarn(bills("""
            [{"ccy":"ETH","earnings":"0","ts":"1785535244000"},
             {"ccy":"ETH","ts":"1785535244000"},
             {"ccy":"ETH","earnings":"non-numerico","ts":"1785535244000"},
             {"earnings":"0.001","ts":"1785535244000"}]
            """));

        assertTrue(righe.isEmpty());
    }
}
