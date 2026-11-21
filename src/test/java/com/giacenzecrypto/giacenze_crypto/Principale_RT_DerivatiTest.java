package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redditi da derivati per RT11/T11 movimento per movimento ({@link Principale_RT_Derivati}, decisione D2 del 19/11/2026).
 * Dati inventati.
 */
class Principale_RT_DerivatiTest {

    private static int n = 0;

    private static String[] mov(String data, String exchange, String tipo, String nota, String risultato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = data.replace("-", "").replace(" ", "").replace(":", "") + "00_" + exchange + "_" + String.format("%03d", ++n) + "_001_DC";
        v[1] = data;
        v[3] = exchange;
        v[21] = nota;
        v[Derivati.CAMPO] = tipo;
        v[Derivati.CAMPO_RISULTATO] = risultato;
        return v;
    }

    /** L'esempio dell'analisi: due contratti OKX e un CSV Bybit senza contratto, totale −27. */
    private static List<String[]> esempio2025() {
        List<String[]> l = new ArrayList<>();
        l.add(mov("2025-03-01 10:00", "OKX", Derivati.CONTRATTO, "", "300.00"));
        l.add(mov("2025-03-02 10:00", "OKX", Derivati.CONTRATTO, "", "-100.00"));
        l.add(mov("2025-03-03 10:00", "OKX", Derivati.FUNDING, "Contratto BTC-USDT-SWAP", "-15.00"));
        l.add(mov("2025-03-04 10:00", "OKX", Derivati.COMMISSIONE, "Contratto BTC-USDT-SWAP", "-10.00"));
        l.add(mov("2025-04-01 10:00", "OKX", Derivati.CONTRATTO, "", "-250.00"));
        l.add(mov("2025-04-02 10:00", "OKX", Derivati.FUNDING, "Contratto ETH-USDT-SWAP", "5.00"));
        l.add(mov("2025-04-03 10:00", "OKX", Derivati.COMMISSIONE, "Contratto ETH-USDT-SWAP", "-5.00"));
        l.add(mov("2025-05-01 10:00", "Bybit", Derivati.PNL, "", "80.00"));
        l.add(mov("2025-05-02 10:00", "Bybit", Derivati.PNL, "", "-30.00"));
        l.add(mov("2025-05-03 10:00", "Bybit", Derivati.FUNDING, "", "-2.00"));
        //la riga del profitto di OKX di una chiusura in LIFO vale zero e non conta
        l.add(mov("2025-03-01 10:00", "OKX", Derivati.PNL, "Contratto BTC-USDT-SWAP", "0.00"));
        //un movimento senza risultato (non classificato) e uno di un altro anno non contano
        l.add(mov("2025-06-01 10:00", "OKX", Derivati.PNL, "", ""));
        l.add(mov("2024-12-31 23:59", "OKX", Derivati.PNL, "", "999.00"));
        return l;
    }

    @Test
    void movimentoPerMovimentoIPositiviInColonna1INegativiInColonna2() {
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2025", esempio2025());
        assertTrue(e.presente());
        assertEquals(10, e.voci.size());
        assertEquals(0, new BigDecimal("385").compareTo(e.colonna1()));
        assertEquals(0, new BigDecimal("412").compareTo(e.colonna2()));
        assertEquals(0, new BigDecimal("-27").compareTo(e.risultato()));
        assertEquals(0, new BigDecimal("-27.00").compareTo(e.risultatoCentesimi()));
        assertEquals(3, e.voci.stream().filter(v -> v.colonna == 1).count());
        assertEquals(7, e.voci.stream().filter(v -> v.colonna == 2).count());
        assertEquals("BTC-USDT-SWAP", e.voci.stream().filter(v -> v.tipo.equals(Derivati.FUNDING) && v.valore.signum() < 0).findFirst().orElseThrow().contratto);
        assertEquals("", e.voci.stream().filter(v -> "Bybit".equals(v.exchange)).findFirst().orElseThrow().contratto, "i CSV non hanno il contratto");
    }

    @Test
    void siArrotondaIlTotaleNonIlMovimentoEIlRisultatoVieneDalleColonneArrotondate() {
        List<String[]> l = List.of(
                mov("2025-01-01 10:00", "OKX", Derivati.PNL, "", "10.40"),
                mov("2025-01-02 10:00", "OKX", Derivati.PNL, "", "10.40"),
                mov("2025-01-03 10:00", "OKX", Derivati.COMMISSIONE, "", "-0.49"));
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2025", l);
        //movimento per movimento sarebbero 10 + 10 = 20: sul totale 20,80 diventa 21
        assertEquals(0, new BigDecimal("21").compareTo(e.colonna1()));
        assertEquals(0, new BigDecimal("0").compareTo(e.colonna2()));
        assertEquals(0, e.colonna1().subtract(e.colonna2()).compareTo(e.risultato()));
    }

    @Test
    void ilConguaglioEUnMovimentoComeGliAltri() {
        String[] conguaglio = mov("2026-01-05 10:00", "OKX", Derivati.CONTRATTO, "", "4500.00");
        Map<String, String> d = Derivati.Dati(conguaglio);
        d.put(DerivatiLifo.CHIAVE_CONGUAGLIO, "4500.00");
        Derivati.ImpostaDati(conguaglio, d);
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2026", List.of(conguaglio,
                mov("2026-01-05 10:00", "OKX", Derivati.PNL, "", "4500.00")));
        assertEquals(0, new BigDecimal("9000").compareTo(e.colonna1()));
    }

    @Test
    void unAnnoSenzaDerivatiNonHaNulla() {
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2023", esempio2025());
        assertFalse(e.presente());
        assertEquals(0, e.colonna1().signum());
        assertEquals(0, e.colonna2().signum());
    }

    @Test
    void lePerditeDeiQuattroAnniPrimaDallaPiuVecchia() {
        List<String[]> l = new ArrayList<>(esempio2025());
        l.add(mov("2021-02-01 10:00", "OKX", Derivati.PNL, "", "-50.00"));   //cinque anni prima del 2026: fuori
        l.add(mov("2022-02-01 10:00", "OKX", Derivati.PNL, "", "-40.00"));
        l.add(mov("2023-02-01 10:00", "OKX", Derivati.PNL, "", "60.00"));    //anno in guadagno: non è una perdita
        Map<String, BigDecimal> p = Principale_RT_Derivati.PerditeAnniPrecedenti("2026", l);
        //il 2024 dell'esempio ha +999
        assertEquals(List.of("2022", "2025"), new ArrayList<>(p.keySet()));
        assertEquals(0, new BigDecimal("40").compareTo(p.get("2022")));
        assertEquals(0, new BigDecimal("27").compareTo(p.get("2025")));
    }

    // ===== Riquadro della scheda RT =====

    @Test
    void ilRiepilogoDelRiquadroDaColonneRisultatoRighiEPerdite() {
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2025", esempio2025());
        List<String> r = Principale_RT_Derivati.RiepilogoRiquadro(e, new java.util.LinkedHashMap<>(Map.of("2022", new BigDecimal("40"))));
        assertEquals(4, r.size(), r.toString());
        assertEquals("Anno 2025 : 10 movimenti con un risultato dei derivati.", r.get(0));
        assertTrue(r.get(1).contains("385 euro") && r.get(1).contains("412 euro") && r.get(1).contains("minusvalenza di 27 euro"), r.get(1));
        assertTrue(r.get(2).contains("RT11") && r.get(2).contains("RT52") && r.get(2).contains("RT102") && r.get(2).contains("T11"), r.get(2));
        assertTrue(r.get(3).contains("2022 : 40 euro"), r.get(3));
        for (String s : r) assertFalse(s.contains(";"), s);
    }

    @Test
    void unAnnoSenzaDerivatiDiceNessunMovimentoEMostraComunqueLePerdite() {
        List<String> r = Principale_RT_Derivati.RiepilogoRiquadro(Principale_RT_Derivati.Calcola("2026", esempio2025()),
                Principale_RT_Derivati.PerditeAnniPrecedenti("2026", esempio2025()));
        assertEquals(2, r.size(), r.toString());
        assertEquals("Anno 2026 : nessun movimento con un risultato dei derivati.", r.get(0));
        assertTrue(r.get(1).contains("2025 : 27 euro"), r.get(1));
    }

    @Test
    void iRighiDipendonoDalModelloDellAnno() {
        assertTrue(Principale_RT_Derivati.Righi("2022").contains("solo nelle note"));
        String r2023 = Principale_RT_Derivati.Righi("2023");
        assertTrue(r2023.contains("RT21") && r2023.contains("RT93") && !r2023.contains("RT11"), r2023);
        assertTrue(Principale_RT_Derivati.Righi("2024").contains("RT11"));
    }

    @Test
    void leRigheDelRiquadroHannoIlTipoInParoleELIDDelMovimento() {
        String[] conguaglio = mov("2025-02-01 10:00", "OKX", Derivati.CONTRATTO, "", "50.00");
        Map<String, String> d = Derivati.Dati(conguaglio);
        d.put(DerivatiLifo.CHIAVE_CONGUAGLIO, "20.00");
        Derivati.ImpostaDati(conguaglio, d);
        List<String[]> l = new ArrayList<>(esempio2025());
        l.add(conguaglio);
        Principale_RT_Derivati.Esito e = Principale_RT_Derivati.Calcola("2025", l);
        List<Object[]> righe = Principale_RT_Derivati.RigheRiquadro(e);
        assertEquals(e.voci.size(), righe.size());
        for (Object[] r : righe) assertEquals(Principale_RT_Derivati.COLONNE_RIQUADRO.length, r.length);
        Object[] prima = righe.get(0);
        assertEquals(conguaglio[0], prima[Principale_RT_Derivati.COLONNA_ID_RIQUADRO], "le voci sono in ordine di ID");
        assertEquals("Chiusura del contratto (LIFO), con il conguaglio degli anni precedenti", prima[2]);
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) prima[4]));
        assertEquals("1 (positivi)", prima[5]);
        assertTrue(righe.stream().anyMatch(r -> "Funding".equals(r[2]) && "BTC-USDT-SWAP".equals(r[3])));
        assertEquals("Commissione", Principale_RT_Derivati.DescrizioneTipo(Derivati.COMMISSIONE, false));
    }

    // ===== Archivio delle dichiarazioni ed Excel (decisione R4) =====

    @Test
    void unAnnoSenzaDerivatiNonScriveITotaliEResta_ugualeAllaDefinitiva() {
        Dichiarazioni.Dichiarazione def = new Dichiarazioni.Dichiarazione();
        def.Totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "100.00");
        Dichiarazioni.Dichiarazione ora = new Dichiarazioni.Dichiarazione();
        ora.Totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "100.00");
        Principale_Dichiarazioni.AggiungiTotaliDerivati(ora.Totali, Principale_RT_Derivati.Calcola("2023", esempio2025()));
        assertEquals(1, ora.Totali.size());
        assertTrue(Dichiarazioni.Confronta(def, ora).isEmpty());
    }

    @Test
    void unaDefinitivaSenzaIDerivatiDiUnAnnoCheNeHaRisultaDiversaEIlTestoLoDice() {
        Dichiarazioni.Dichiarazione def = new Dichiarazioni.Dichiarazione();
        def.Anno = "2025";
        def.Creata = "2026-05-01";
        def.Totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "100.00");
        Dichiarazioni.Dichiarazione ora = new Dichiarazioni.Dichiarazione();
        ora.Totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "100.00");
        Principale_Dichiarazioni.AggiungiTotaliDerivati(ora.Totali, Principale_RT_Derivati.Calcola("2025", esempio2025()));
        assertEquals("385.00", ora.Totali.get(Principale_Dichiarazioni.TOT_RT_DER_POSITIVI));
        assertEquals("412.00", ora.Totali.get(Principale_Dichiarazioni.TOT_RT_DER_NEGATIVI));
        assertEquals("-27.00", ora.Totali.get(Principale_Dichiarazioni.TOT_RT_DER_RISULTATO));
        List<String[]> diff = Dichiarazioni.Confronta(def, ora);
        assertEquals(3, diff.size());
        String testo = Principale_Dichiarazioni.TestoDifferenze("2025", def, diff);
        assertTrue(testo.contains("la definitiva non comprendeva i redditi da derivati"), testo);
        assertFalse("la definitiva non comprendeva i redditi da derivati".contains(";"));
    }

    @Test
    void ilFoglioExcelDeiDerivatiSiScrive() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        org.dhatim.fastexcel.Workbook wb = new org.dhatim.fastexcel.Workbook(out, "prova", "1.0");
        Principale_Dichiarazioni.ScriviFoglioDerivati(wb.newWorksheet("Derivati 2025"), Principale_RT_Derivati.Calcola("2025", esempio2025()));
        wb.finish();
        assertTrue(out.size() > 0);
    }
}
