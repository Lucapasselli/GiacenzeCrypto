package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.Calcoli_RT.AnalisiPlus;
import com.giacenzecrypto.giacenze_crypto.Calcoli_RT.PlusXMoneta;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Plusvalenze per gruppo wallet del quadro RT: la somma per gruppo ({@code RitornaTabellaGruppiAnno}), la colonna
 * dell'alias nell'Excel ({@code ConColonnaAlias}) e l'etichetta dei valori nel popup del filtro di colonna.
 */
class RTGruppiWalletTest {

    private static PlusXMoneta plus(String qta, String prezzo, boolean movimentata, String vendite, String costi,
            String realizzata, String latente) {
        PlusXMoneta p = new PlusXMoneta();
        p.Mon = new Moneta();
        p.Mon.Qta = qta;
        p.Mon.Prezzo = prezzo;
        p.Mon.Tipo = "Crypto";
        p.movimentatoAnno = movimentata;
        p.ValVendita = vendite;
        p.CostoVendite = costi;
        p.PlusRealizzata = realizzata;
        p.PlusLatente = latente;
        p.PMC = "0.00";
        return p;
    }

    private static AnalisiPlus analisi(Map<String, Map<String, PlusXMoneta>> gruppi) {
        AnalisiPlus a = new AnalisiPlus();
        a.Put_MappaCompleta(Map.of("2025", gruppi));
        return a;
    }

    @Test
    void sommaPerGruppo_tornaConLaTabellaPerToken() {
        Map<String, Map<String, PlusXMoneta>> g = new LinkedHashMap<>();
        Map<String, PlusXMoneta> w1 = new LinkedHashMap<>();
        w1.put("BTC", plus("2", "100", true, "300.00", "200.00", "100.00", "50.00"));
        w1.put("ETH", plus("10", "5", true, "40.00", "50.00", "-10.00", "0.00"));
        w1.put("ZERO", plus("0", "1", false, "999", "999", "999", "999")); // niente movimenti e giacenza zero: fuori
        w1.put("BAD **", plus("1", "1", true, "999", "999", "999", "999")); // SCAM: fuori
        g.put("Wallet 101", w1);
        Map<String, PlusXMoneta> w2 = new LinkedHashMap<>();
        w2.put("BTC", plus("1", "100", true, "10.00", "4.00", "6.00", "1.50"));
        g.put("Wallet 102", w2);
        AnalisiPlus a = analisi(g);

        List<Object[]> gruppi = a.RitornaTabellaGruppiAnno("2025");
        assertEquals(2, gruppi.size());
        Object[] r1 = gruppi.get(0);
        assertEquals("Wallet 101", r1[0]);
        assertEquals(340.0, (double) r1[1], 1e-9, "vendite");
        assertEquals(250.0, (double) r1[2], 1e-9, "costi");
        assertEquals(90.0, (double) r1[3], 1e-9, "plusvalenze realizzate");
        assertEquals(50.0, (double) r1[4], 1e-9, "plusvalenze latenti");
        assertEquals(105.0, (double) r1[5], 1e-9, "rimanenze = 100 + 5: Mon.Prezzo e' il valore dell'intera giacenza");

        // i totali per gruppo sono la somma delle righe per token degli stessi gruppi
        double venditeToken = 0;
        for (Object[] t : a.RitornaTabellaAnno("2025")) {
            venditeToken += (double) t[3];
        }
        double venditeGruppi = 0;
        for (Object[] r : gruppi) {
            venditeGruppi += (double) r[1];
        }
        assertEquals(venditeToken, venditeGruppi, 1e-9);
    }

    @Test
    void errori_giacenzaNegativaESenzaPrezzo_ancheConNotazioneScientifica() {
        Map<String, Map<String, PlusXMoneta>> g = new LinkedHashMap<>();
        Map<String, PlusXMoneta> w = new LinkedHashMap<>();
        w.put("NEG", plus("-3", "1", true, "0", "0", "0", "0"));
        w.put("NOPRICE", plus("4", "0", true, "0", "0", "0", "0"));
        w.put("TINY", plus("2.5E-9", "1", true, "0", "0", "0", "0")); // positiva: M7
        g.put("Wallet 101", w);
        String errori = (String) analisi(g).RitornaTabellaGruppiAnno("2025").get(0)[6];
        assertTrue(errori.contains("1 token con giacenza negativa"), errori);
        assertTrue(errori.contains("1 token senza prezzo"), errori);
    }

    @Test
    void gruppoSenzaMonetePresenti_nonHaRiga() {
        Map<String, Map<String, PlusXMoneta>> g = new LinkedHashMap<>();
        g.put("Wallet 101", new LinkedHashMap<>(Map.of("ZERO", plus("0", "1", false, "0", "0", "0", "0"))));
        assertTrue(analisi(g).RitornaTabellaGruppiAnno("2025").isEmpty());
    }

    @Test
    void liFoGlobale_laTabellaPerGruppoRipartisceSenzaCambiareITotali() {
        // pila globale: BTC giacenza 3 a valore 300 (latente 60), ETH giacenza 10 a valore 50
        Map<String, Map<String, PlusXMoneta>> g = new LinkedHashMap<>();
        Map<String, PlusXMoneta> glob = new LinkedHashMap<>();
        glob.put("BTC", plus("3", "300.00", true, "90.00", "40.00", "50.00", "60.00"));
        glob.put("ETH", plus("10", "50.00", true, "0", "0", "0", "0"));
        glob.put("BAD **", plus("1", "1", true, "0", "0", "0", "0"));
        g.put("Globale", glob);
        AnalisiPlus a = analisi(g);
        a.GlobaleDivisoPerGruppo = true;
        // Wallet 101 detiene 2 BTC e 10 ETH, Wallet 102 detiene 1 BTC; lo SCAM non conta
        a.AggiungiQtaGruppo("Wallet 101", "BTC", "2");
        a.AggiungiQtaGruppo("Wallet 101", "ETH", "10");
        a.AggiungiQtaGruppo("Wallet 102", "BTC", "1");
        a.AggiungiQtaGruppo("Wallet 102", "BAD **", "1");
        a.FotografaQtaGruppi("2025");
        // il realizzato e' del gruppo del movimento: 70/30 vendite, 40/10 costi, 30/20 plus
        a.AccumulaRealizzatoGruppo("2025", "Wallet 101", "BTC", new java.math.BigDecimal("70"), new java.math.BigDecimal("40"), new java.math.BigDecimal("30"));
        a.AccumulaRealizzatoGruppo("2025", "Wallet 102", "BTC", new java.math.BigDecimal("20"), new java.math.BigDecimal("0"), new java.math.BigDecimal("20"));
        a.AccumulaRealizzatoGruppo("2025", "Wallet 102", "BAD **", new java.math.BigDecimal("999"), new java.math.BigDecimal("999"), new java.math.BigDecimal("999"));

        List<Object[]> gruppi = a.RitornaTabellaGruppiAnno("2025");
        assertEquals(2, gruppi.size());
        Object[] r1 = gruppi.get(0);
        assertEquals("Wallet 101", r1[0]);
        assertEquals(70.0, (double) r1[1], 1e-9);
        assertEquals(40.0, (double) r1[2], 1e-9);
        assertEquals(30.0, (double) r1[3], 1e-9);
        assertEquals(40.0, (double) r1[4], 1e-9, "latente: 2/3 di 60 su BTC");
        assertEquals(250.0, (double) r1[5], 1e-9, "rimanenze: 2/3 di 300 + tutti i 50 di ETH");
        Object[] r2 = gruppi.get(1);
        assertEquals("Wallet 102", r2[0]);
        assertEquals(20.0, (double) r2[1], 1e-9, "lo SCAM non entra nel realizzato");
        assertEquals(20.0, (double) r2[4], 1e-9);
        assertEquals(100.0, (double) r2[5], 1e-9);

        // i totali dei gruppi sono quelli della tabella per token
        double latente = 0, rimanenze = 0, realizzata = 0;
        for (Object[] r : gruppi) {
            latente += (double) r[4];
            rimanenze += (double) r[5];
            realizzata += (double) r[3];
        }
        assertEquals(60.0, latente, 1e-9);
        assertEquals(350.0, rimanenze, 1e-9);
        assertEquals(50.0, realizzata, 1e-9);
    }

    @Test
    void liFoGlobale_annoSenzaMovimentiUsaLaFotografiaPrecedente() {
        Map<String, Map<String, PlusXMoneta>> g = new LinkedHashMap<>();
        g.put("Globale", new LinkedHashMap<>(Map.of("BTC", plus("1", "100.00", false, "0", "0", "0", "10.00"))));
        AnalisiPlus a = new AnalisiPlus();
        a.Put_MappaCompleta(Map.of("2025", g, "2026", g));
        a.GlobaleDivisoPerGruppo = true;
        a.AggiungiQtaGruppo("Wallet 101", "BTC", "1");
        a.FotografaQtaGruppi("2025");
        List<Object[]> gruppi = a.RitornaTabellaGruppiAnno("2026");
        assertEquals(1, gruppi.size());
        assertEquals(100.0, (double) gruppi.get(0)[5], 1e-9);
    }

    @Test
    void excel_gruppoEAliasDopoLaColonnaWallet() {
        Map<String, String[]> alias = Map.of("Wallet 101", new String[]{"Wallet 101", "Binance"});
        Object[] intestazione = Principale_Dichiarazioni.ConColonneGruppoWallet(new Object[]{"Data", "Tipo", "Wallet", "Moneta"}, 2, null, alias);
        assertArrayEquals(new Object[]{"Data", "Tipo", "Wallet", "Gruppo wallet", "Alias gruppo wallet", "Moneta"}, intestazione);
        Object[] riga = Principale_Dichiarazioni.ConColonneGruppoWallet(new Object[]{"d", "t", "Binance", "BTC"}, 2, "Wallet 101", alias);
        assertArrayEquals(new Object[]{"d", "t", "Binance", "Wallet 101", "Binance", "BTC"}, riga);
        Object[] senza = Principale_Dichiarazioni.ConColonneGruppoWallet(new Object[]{"d", "t", "X", "BTC"}, 2, "Wallet 7", alias);
        assertArrayEquals(new Object[]{"d", "t", "X", "Wallet 7", "", "BTC"}, senza);
    }

    @Test
    void excel_colonnaAliasDopoIlGruppo() {
        Map<String, String[]> alias = Map.of("Wallet 101", new String[]{"Wallet 101", "Binance"});
        Object[] intestazione = Principale_Dichiarazioni.ConColonnaAlias(new Object[]{"Wallet", "Moneta"}, null, alias);
        assertArrayEquals(new Object[]{"Wallet", "Alias gruppo wallet", "Moneta"}, intestazione);
        Object[] riga = Principale_Dichiarazioni.ConColonnaAlias(new Object[]{"Wallet 101", "BTC"}, "Wallet 101", alias);
        assertArrayEquals(new Object[]{"Wallet 101", "Binance", "BTC"}, riga);
        Object[] senza = Principale_Dichiarazioni.ConColonnaAlias(new Object[]{"Wallet 7", "BTC"}, "Wallet 7", alias);
        assertArrayEquals(new Object[]{"Wallet 7", "", "BTC"}, senza, "gruppo senza alias: colonna vuota");
    }

    @Test
    void popupFiltro_etichettaSoloMostrata_laSelezioneRestaSulValore() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "Ambiente headless: il popup e' una finestra");
        Tabelle_PopupSelezioneMultipla popup = new Tabelle_PopupSelezioneMultipla((java.awt.Window) null);
        List<String[]> valori = new ArrayList<>(List.of(new String[]{"Wallet 101", "1"}, new String[]{"Wallet 102", "0"}));
        popup.updateOptions(valori, v -> v.equals("Wallet 101") ? "101 ( Binance )" : v);
        assertEquals(List.of("Wallet 101"), popup.getSelectedOptions(), "il filtro lavora sul valore del model");
    }

    @Test
    void impostaEtichettaFiltro_nonCambiaIlModel() {
        JTable t = new JTable(new DefaultTableModel(new Object[][]{{"Wallet 101"}}, new Object[]{"Wallet"}));
        Tabelle.Tabelle_ImpostaEtichettaFiltro(t, 0, v -> "alias di " + v);
        assertEquals("Wallet 101", t.getModel().getValueAt(0, 0));
    }
}
