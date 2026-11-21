package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Posizioni su perpetui/futures aperte con un voucher OKX: {@link OKXVoucher} le riconosce con un'identita'
 * esatta (somma dei {@code balChg} dell'episodio uguale al bill {@code type 1 / subType 257}) e
 * {@link CcxtInterop#convertOKXBills} importa soltanto quel bill.
 *
 * <p>Dati inventati, nella forma reale dei bill: margine isolato, un fill di apertura in tre pezzi, un fill di
 * chiusura, {@code posBal} che torna a zero e il bill 257 senza {@code from}/{@code to}/{@code notes}.
 * Importi: margine 40 USDC, commissioni di apertura 0,03, pnl 1,50, commissione di chiusura 0,04, quindi
 * profitto netto 1,50 - 0,03 - 0,04 = 1,43.</p>
 */
class OKXVoucherTest {

    /** Un'interruzione richiesta da un altro test lascia il flag acceso e convertOKXBills risponderebbe null. */
    @org.junit.jupiter.api.BeforeEach
    void nessunaInterruzione() {
        Principale.InterrompiCiclo = false;
    }

    private static JsonArray bills(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    /** Un fill type 2 di un derivato. */
    private static String fill(String id, String ts, String sub, String balChg, String fee, String pnl,
                               String posBal, String posBalChg, String instId) {
        return "{\"billId\":\"" + id + "\",\"ts\":\"" + ts + "\",\"ccy\":\"USDC\",\"type\":\"2\",\"subType\":\"" + sub
                + "\",\"instType\":\"FUTURES\",\"instId\":\"" + instId + "\",\"mgnMode\":\"isolated\","
                + "\"balChg\":\"" + balChg + "\",\"fee\":\"" + fee + "\",\"pnl\":\"" + pnl + "\","
                + "\"posBal\":\"" + posBal + "\",\"posBalChg\":\"" + posBalChg + "\",\"ordId\":\"ORD" + id + "\"}";
    }

    private static String b257(String id, String ts, String balChg) {
        return "{\"billId\":\"" + id + "\",\"ts\":\"" + ts + "\",\"ccy\":\"USDC\",\"type\":\"1\",\"subType\":\"257\","
                + "\"balChg\":\"" + balChg + "\",\"fee\":\"0\",\"pnl\":\"0\",\"posBal\":\"0\",\"posBalChg\":\"0\"}";
    }

    private static final String XP = "TESTCOIN-USD_UM_XPERP-999999";
    private static final long T0 = 1_800_000_000_000L;

    @Test
    void unaPosizioneAVoucherDiventaLaSola257() {
        //Gli id sono numerici come quelli veri: servono a ordinare i fill con lo stesso istante
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-15.01", "-0.01", "0", "15", "15", XP) + ","
                + fill("4002", String.valueOf(T0), "1", "-25.02", "-0.02", "0", "40", "25", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.43") + "]";
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");

        assertEquals(1, righe.size(), "margine e commissioni della posizione non entrano, resta la 257");
        String[] r = righe.get(0);
        assertEquals("Utilizzo di voucher|FUTURES", r[4]);
        assertEquals("4004", r[14]);
        assertEquals("1.43", r[6]);
        assertEquals(Derivati.PNL, CcxtInterop.TipoDerivatoDaCausaleOKX(r[4]));
        assertTrue(r[12].isEmpty(), "nessuna commissione: e' gia' dentro il netto accreditato");
    }

    @Test
    void senzaLa257LaPosizioneRestaCom_eOggi() {
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + "]";
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");

        //Il margine (40) non e' un movimento: dell'apertura resta la commissione, della chiusura il profitto e la sua commissione
        assertEquals(2, righe.size());
        assertEquals("Fee|FUTURES", righe.get(0)[4]);
        assertEquals("-0.03", righe.get(0)[6]);
        assertTrue(righe.get(0)[12].isEmpty(), "la commissione e' la riga stessa, non una seconda");
        assertEquals("Buy|FUTURES", righe.get(1)[4]);
        assertEquals("1.5", righe.get(1)[6]);
        assertEquals("-0.04", righe.get(1)[12]);
        assertEquals(Derivati.COMMISSIONE, CcxtInterop.TipoDerivatoDaCausaleOKX(righe.get(0)[4]));
        assertEquals(Derivati.PNL, CcxtInterop.TipoDerivatoDaCausaleOKX(righe.get(1)[4]));
    }

    @Test
    void unaChiusuraInPerditaDiventaUnAddebitoDelSoloRisultato() {
        //Fondi propri: margine 40, perdita 2, commissione 0,04. Rientrano 37,96 ma il movimento e' la perdita, non il margine
        String json = "[" + fill("4003", String.valueOf(T0 + 600_000), "2", "37.96", "-0.04", "-2.00", "0", "-40", XP) + "]";
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");

        assertEquals(1, righe.size());
        assertEquals("Sell|FUTURES", righe.get(0)[4]);
        assertEquals("-2", righe.get(0)[6]);
        assertEquals("-0.04", righe.get(0)[12]);
    }

    @Test
    void unaChiusuraParzialeNonPortaFuoriIlMargineRimasto() {
        //Aperti 2 contratti (margine 13), chiuso 1 in pari perdita: fra i movimenti non compare ne' il 13 ne' il 6,5
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-13.006", "-0.006", "0", "13", "13", XP) + ","
                + fill("4002", String.valueOf(T0 + 60_000), "2", "6.494", "-0.003", "-0.003", "6.5", "-6.5", XP) + "]";
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");

        assertEquals(2, righe.size());
        assertEquals("-0.006", righe.get(0)[6]);
        assertEquals("-0.003", righe.get(1)[6]);
        java.math.BigDecimal totale = java.math.BigDecimal.ZERO;
        for (String[] r : righe) {
            totale = totale.add(new java.math.BigDecimal(r[6]));
            if (!r[12].isEmpty()) totale = totale.add(new java.math.BigDecimal(r[12]));
        }
        //-0,006 (commissione di apertura) - 0,003 (perdita) - 0,003 (commissione di chiusura)
        assertEquals(0, totale.compareTo(new java.math.BigDecimal("-0.012")));
    }

    @Test
    void ilContrattoViaggiaNelCampo7DiOgniRigaDeiDerivatiEdellAccreditoDelVoucher() {
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.43") + "]";
        //Senza abbinamento (qui la 257 e' di un altro importo) le righe dei derivati portano comunque il contratto
        List<String[]> senza = CcxtInterop.convertOKXBills(bills(json.replace("\"1.43\"", "\"9.99\"")), "Trading");
        assertEquals(3, senza.size());
        assertEquals(XP, senza.get(0)[7]);
        assertEquals(XP, senza.get(1)[7]);
        assertEquals("", senza.get(2)[7], "il Transfer in non abbinato non e' un derivato");
        //Con l'abbinamento resta la sola 257, che prende il contratto dalla posizione
        List<String[]> con = CcxtInterop.convertOKXBills(bills(json), "Trading");
        assertEquals(1, con.size());
        assertEquals(XP, con.get(0)[7]);
    }

    @Test
    void unaPosizioneInPerditaNonHaLa257EResta() {
        //Chiusura in perdita: 40 di margine meno 2 di pnl negativo meno 0,04 di fee torna 37,96
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "37.96", "-0.04", "-2.00", "0", "-40", XP) + "]";
        OKXVoucher.Esito e = OKXVoucher.Analizza(bills(json));
        assertTrue(e.omessi.isEmpty());
        assertTrue(e.voucher.isEmpty());
        assertEquals(2, CcxtInterop.convertOKXBills(bills(json), "Trading").size());
    }

    @Test
    void unaDifferenzaDiImportoNonAbbina() {
        //La 257 e' di 1,44 invece di 1,43: non e' l'identita' esatta, quindi non e' di questa posizione
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-15.01", "-0.01", "0", "15", "15", XP) + ","
                + fill("4002", String.valueOf(T0), "1", "-25.02", "-0.02", "0", "40", "25", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.44") + "]";
        assertTrue(OKXVoucher.Analizza(bills(json)).voucher.isEmpty());
    }

    @Test
    void laFundingFeeDentroLEpisodioEntraNellaSomma() {
        //Funding pagato 0,10 durante la posizione: il netto accreditato e' 1,33
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + "{\"billId\":\"4002\",\"ts\":\"" + (T0 + 300_000) + "\",\"ccy\":\"USDC\",\"type\":\"8\",\"instType\":\"FUTURES\","
                + "\"instId\":\"" + XP + "\",\"balChg\":\"-0.10\"},"
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 603_000), "1.33") + "]";
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");

        assertEquals(1, righe.size());
        assertEquals("1.33", righe.get(0)[6]);
    }

    @Test
    void unEpisodioIniziatoPrimaDeiBillScaricatiNonSiAbbina() {
        //Il primo fill non apre da zero (posBal 40 ma posBalChg 10): l'apertura e' in uno scaricamento precedente
        String json = "[" + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.46") + "]";
        OKXVoucher.Esito e = OKXVoucher.Analizza(bills(json));
        assertTrue(e.voucher.isEmpty());
        assertEquals(0, e.episodiAperti);
    }

    @Test
    void unaPosizioneAncoraApertaVieneContataEnonOmessa() {
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + "]";
        OKXVoucher.Esito e = OKXVoucher.Analizza(bills(json));
        assertEquals(1, e.episodiAperti);
        assertTrue(e.omessi.isEmpty());
    }

    @Test
    void unaStessa257UgualeADuePosizioniNonAbbinaNessuna() {
        String XP2 = "OTHER-USD_UM_XPERP-111111";
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4002", String.valueOf(T0 + 1000), "1", "-40.03", "-0.03", "0", "40", "40", XP2) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + fill("4005", String.valueOf(T0 + 601_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP2) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.43") + "]";
        OKXVoucher.Esito e = OKXVoucher.Analizza(bills(json));
        assertTrue(e.voucher.isEmpty(), "ambigua: in dubbio non si toglie nulla");
        assertTrue(e.omessi.isEmpty());
        List<String[]> righe = CcxtInterop.convertOKXBills(bills(json), "Trading");
        assertEquals(5, righe.size(), "i quattro fill restano e la 257 e' il solito Transfer in");
        assertTrue(righe.stream().anyMatch(r -> r[4].equals("Transfer in")));
        assertTrue(righe.stream().noneMatch(r -> r[4].startsWith("Utilizzo di voucher")));
    }

    @Test
    void dueVoucherConImportiDiversiSiAbbinanoCiascunoAlSuo() {
        String XP2 = "OTHER-USD_UM_XPERP-111111";
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4002", String.valueOf(T0 + 1000), "1", "-40.03", "-0.03", "0", "40", "40", XP2) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + fill("4005", String.valueOf(T0 + 601_000), "2", "42.46", "-0.04", "2.50", "0", "-40", XP2) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.43") + ","
                + b257("4006", String.valueOf(T0 + 605_000), "2.43") + "]";
        OKXVoucher.Esito e = OKXVoucher.Analizza(bills(json));
        assertEquals(2, e.voucher.size());
        assertEquals(java.util.Set.of("4001", "4003", "4002", "4005"), e.omessi);
    }

    @Test
    void lArchivioSenzaPosBalNonAbbinaENonTogliePiuDelNecessario() {
        //Righe nella forma del CSV dell'archivio: niente posBal, il tipo e' sintetizzato dal subType
        String json = "[{\"billId\":\"4001\",\"ts\":\"" + T0 + "\",\"ccy\":\"USDC\",\"type\":\"2\",\"subType\":\"1\",\"instType\":\"FUTURES\","
                + "\"instId\":\"" + XP + "\",\"balChg\":\"-40.03\",\"fee\":\"-0.03\"}]";
        assertTrue(OKXVoucher.Analizza(bills(json)).omessi.isEmpty());
        assertEquals(1, CcxtInterop.convertOKXBills(bills(json), "Trading").size());
    }

    @Test
    void ilContoFundingNonSiToccaMai() {
        String json = "[" + fill("4001", String.valueOf(T0), "1", "-40.03", "-0.03", "0", "40", "40", XP) + ","
                + fill("4003", String.valueOf(T0 + 600_000), "2", "41.46", "-0.04", "1.50", "0", "-40", XP) + ","
                + b257("4004", String.valueOf(T0 + 604_000), "1.43") + "]";
        //Stessa lista passata come Funding: nessun abbinamento, le righe seguono la mappa del Funding
        assertTrue(CcxtInterop.convertOKXBills(bills(json), "Funding").stream().noneMatch(r -> r[4].startsWith("Utilizzo di voucher")));
    }

    @Test
    void laMappaCausaliMandaLaCausaleDelVoucherFraIDepositiDaClassificare() throws Exception {
        org.json.JSONObject mappa = new org.json.JSONObject(java.nio.file.Files.readString(
                java.nio.file.Path.of("config/importmappe/OKX.json"))).getJSONObject("mappaCausali");
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.getString("Utilizzo di voucher|FUTURES"));
        assertEquals("TRASFERIMENTO-CRYPTO", mappa.getString("Utilizzo di voucher|SWAP"));
        //Senza abbinamento la 257 resta un Transfer in non considerato: non si inventa un deposito
        assertEquals("NON CONSIDERARE", mappa.getString("Transfer in"));
    }
}
