package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le righe OKX di perpetui, futures e opzioni (causale {@code Sell|SWAP}...) escono da
 * {@link Importazioni#Ex_OKX_Consolida} come deposito o prelievo da classificare, marcate come derivati e con
 * la commissione marcata a parte. Uno scambio spot resta com'era, senza marcatore.
 *
 * <p>Dati inventati: la forma e' quella di un trade su perpetuo a margine isolato, dove ogni bill porta una
 * sola moneta (il margine piu' o meno il profitto) e non due gambe.</p>
 */
class ImportOKXDerivatiTest {

    private static final Map<String, String> MAPPA = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        MAPPA.put("Sell|SWAP", "TRASFERIMENTO-CRYPTO");
        MAPPA.put("Buy|SWAP", "TRASFERIMENTO-CRYPTO");
    }

    private static String[] riga(String orario, String causale, String moneta, String qta,
                                 String monetaFee, String qtaFee, String billId) {
        String[] r = new String[19];
        r[0] = orario;
        r[1] = "OKX";
        r[2] = "Principale";
        r[3] = MAPPA.getOrDefault(causale, "");
        r[4] = causale;
        r[5] = moneta;
        r[6] = qta;
        r[11] = monetaFee;
        r[12] = qtaFee;
        r[13] = "ORD-" + billId;
        r[14] = billId;
        r[15] = "NO";
        Importazioni.RiempiVuotiArray(r);
        return r;
    }

    @Test
    void ilContrattoDelDerivatoVaNelleNoteDelMovimentoEDellaCommissioneConITrattiniBassi() {
        Importazioni.AzzeraContatori();
        String[] r = riga("2026-11-13 14:54:08", "Sell|SWAP", "USDC", "-18.3", "USDC", "-0.027", "7201");
        r[7] = "BTC-USD_UM_XPERP-991231";
        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(List.<String[]>of(r), MAPPA, new ArrayList<>()));

        assertEquals(2, out.size());
        for (String[] m : out) assertEquals("Contratto BTC-USD_UM_XPERP-991231", m[21], m[0]);
    }

    @Test
    void unMovimentoSpotNonPortaIlContratto() {
        Importazioni.AzzeraContatori();
        String[] acquisto = riga("2026-11-13 14:54:08", "Deposit yield", "USDC", "1", "", "", "7301");
        acquisto[3] = "REWARD";
        acquisto[7] = "BTC-USD_UM_XPERP-991231";   //anche se per errore fosse valorizzato: senza causale di derivato non conta
        java.util.Map<String, String> mappa = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        mappa.put("Deposit yield", "REWARD");
        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(List.<String[]>of(acquisto), mappa, new ArrayList<>()));

        assertEquals(1, out.size());
        assertEquals("", out.get(0)[21]);
    }

    @Test
    void ilFillDiAperturaDiventaLaSolaCommissioneMarcata() {
        Importazioni.AzzeraContatori();
        java.util.Map<String, String> mappa = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        mappa.put("Fee|FUTURES", "COMMISSIONI");
        String[] r = riga("2026-11-13 14:54:08", "Fee|FUTURES", "USDC", "-0.0065", "", "", "7101");
        r[3] = "COMMISSIONI";

        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(List.<String[]>of(r), mappa, new ArrayList<>()));

        assertEquals(1, out.size(), "una commissione sola, nessun prelievo di margine");
        assertTrue(CommissioniCollegate.isCommissione(out.get(0)), out.get(0)[0]);
        assertEquals(Derivati.COMMISSIONE, Derivati.Tipo(out.get(0)));
        assertEquals("-0.0065", out.get(0)[10]);
        assertEquals("Fee|FUTURES", out.get(0)[7]);
    }

    @Test
    void unTradeSuPerpetuoDiventaPrelievoMarcatoConLaCommissioneMarcata() {
        Importazioni.AzzeraContatori();
        List<String[]> righe = List.<String[]>of(
                riga("2026-11-13 14:54:08", "Sell|SWAP", "USDC", "-18.30455333", "USDC", "-0.02745683", "7001"));

        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(righe, MAPPA, new ArrayList<>()));

        assertEquals(2, out.size(), "il movimento e la sua commissione");
        String[] commissione = out.stream().filter(CommissioniCollegate::isCommissione).findFirst().orElseThrow();
        String[] prelievo = out.stream().filter(v -> !CommissioniCollegate.isCommissione(v)).findFirst().orElseThrow();
        assertEquals("PRELIEVO CRYPTO", prelievo[5]);
        assertEquals(Derivati.PNL, Derivati.Tipo(prelievo));
        assertEquals(Derivati.COMMISSIONE, Derivati.Tipo(commissione));
        assertEquals("Sell|SWAP", prelievo[7]);
        assertTrue(Importazioni.CausaliDerivatiSegnalate.contains("Sell|SWAP"), "l'avviso del resoconto");
        assertFalse(Importazioni.TestoAvvisoDerivati().isEmpty());
    }

    @Test
    void laChiusuraDelloShortEUnDepositoMarcato() {
        Importazioni.AzzeraContatori();
        List<String[]> righe = List.<String[]>of(
                riga("2026-11-13 15:06:03", "Buy|SWAP", "USDC", "18.45519333", "USDC", "-0.02738151", "7002"));

        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(righe, MAPPA, new ArrayList<>()));

        String[] deposito = out.stream().filter(v -> !CommissioniCollegate.isCommissione(v)).findFirst().orElseThrow();
        assertEquals("DEPOSITO CRYPTO", deposito[5]);
        assertEquals(Derivati.PNL, Derivati.Tipo(deposito));
    }

    @Test
    void unaRigaSenzaSuffissoNonViene_marcata() {
        Importazioni.AzzeraContatori();
        MAPPA.put("withdrawal", "TRASFERIMENTO-CRYPTO");
        List<String[]> righe = List.<String[]>of(
                riga("2026-11-13 10:00:00", "withdrawal", "USDC", "-5", "", "", "7003"));

        List<String[]> out = Prezzi.SenzaValorizzare(() ->
                Importazioni.Ex_OKX_Consolida(righe, MAPPA, new ArrayList<>()));

        assertEquals(1, out.size());
        assertFalse(Derivati.isDerivato(out.get(0)));
        assertTrue(Importazioni.CausaliDerivatiSegnalate.isEmpty());
    }
}
