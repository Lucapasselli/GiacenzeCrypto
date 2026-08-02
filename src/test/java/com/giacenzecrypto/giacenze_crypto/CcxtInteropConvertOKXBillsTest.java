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

    @Test
    void leFeeNonVengonoRiportateCosiDaNonContarleDueVolte() {
        //balChg è già la variazione netta di saldo e comprende la commissione
        List<String[]> righe = CcxtInterop.convertOKXBills(bills("""
            [{"billId":"800","ccy":"BTC","balChg":"0.01","type":"2","fee":"-0.00001","ts":"1700000000000"}]
            """), "Trading");

        assertEquals("", righe.get(0)[11].trim());
        assertEquals("", righe.get(0)[12].trim());
    }

    @Test
    void unArrayNulloProduceUnaListaVuota() {
        assertTrue(CcxtInterop.convertOKXBills(null, "Funding").isEmpty());
    }
}
