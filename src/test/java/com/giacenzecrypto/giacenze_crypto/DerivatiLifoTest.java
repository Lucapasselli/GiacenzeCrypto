package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il LIFO dei contratti ({@link DerivatiLifo}) contro il profitto dell'exchange (prezzo medio): coincidono a lotto singolo,
 * divergono quando una posizione si chiude in piu' riprese a cavallo d'anno (D3). Dati inventati, forma dei bill vera.
 */
class DerivatiLifoTest {

    @TempDir
    static Path tempDir;

    private static final String LIN = "BTC-USD_UM_XPERP-991231";
    private static final long DIC = 1_796_000_000_000L; //2026-11-30 circa
    private static final long GEN = 1_799_000_000_000L; //2027-01-03 circa

    @BeforeAll
    static void apreDatabase() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        Principale.InterrompiCiclo = false;
        OKXDerivati.AzzeraCacheValore();
        ValoriContratti.ImpostaPerTest(java.util.List.of());   //le regole vere hanno una prova a parte
    }

    @AfterEach
    void ripristina() {
        OKXDerivati.FonteValoreContratto = id -> "";
        ValoriContratti.ImpostaPerTest(null);
    }

    private static JSONObject fill(String inst, String ccy, String billId, long ts, String subType, String sz, String px,
            String posBalChg, String pnl) {
        BigDecimal bal = new BigDecimal(posBalChg).negate().add(new BigDecimal(pnl));
        return new JSONObject().put("billId", billId).put("instId", inst).put("instType", "FUTURES").put("ccy", ccy)
                .put("type", "2").put("subType", subType).put("sz", sz).put("px", px).put("balChg", bal.toPlainString())
                .put("posBalChg", posBalChg).put("pnl", pnl).put("fee", "0").put("mgnMode", "isolated")
                .put("ordId", "7" + billId).put("tradeId", "9" + billId).put("ts", String.valueOf(ts));
    }

    private static List<String[]> righe(JSONObject... bills) {
        List<String[]> tutti = new ArrayList<>();
        for (JSONObject b : bills) {
            String[] r = OKXDerivati.RigaContratto(b);
            assertNotNull(r);
            r[1] = "OKX";
            r[2] = "Principale";
            tutti.addAll(OKXDerivati.CreaMovimenti(r));
        }
        return tutti;
    }

    private static DerivatiLifo.Posizione unica(Map<String, DerivatiLifo.Posizione> m) {
        assertEquals(1, m.size());
        return m.values().iterator().next();
    }

    @Test
    void aLottoSingoloIlLifoCoincideConIlProfittoDellExchange() {
        OKXDerivati.FonteValoreContratto = id -> "0.0001";
        //apertura di 2 contratti, due chiusure da 1: valori inventati (pnl = (pc - pa) x ctVal)
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC, "1", "2", "60000.0", "12.00000", "0"),
                fill(LIN, "USDC", "2", DIC + 1000, "2", "1", "59970.0", "-6.00000", "-0.003"),
                fill(LIN, "USDC", "3", DIC + 2000, "2", "1", "59400.0", "-6.00000", "-0.06"))));
        assertTrue(p.lungo);
        assertFalse(p.inverso);
        assertFalse(p.datiIncompleti);
        assertEquals(2, p.chiusure.size());
        assertEquals(0, new BigDecimal("-0.003").compareTo(p.chiusure.get(0).differenzialeLifo));
        assertEquals(0, new BigDecimal("-0.06").compareTo(p.chiusure.get(1).differenzialeLifo));
        assertEquals(0, p.contrattiAperti().signum());
        assertFalse(p.DivergePerAnno());
    }

    @Test
    void aCavalloDAnnoLifoEPrezzoMedioSpostanoIlRedditoFraGliAnni() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        //D3: due contratti aperti a 30.000 e a 40.000 (medio 35.000). Chiusura di dicembre a 45.000, di gennaio a 38.000.
        //L'exchange riconosce +10.000 e +3.000, il LIFO +5.000 (lotto a 40.000) e +8.000 (lotto a 30.000)
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "3", DIC, "2", "1", "45000", "-35000", "10000"),
                fill(LIN, "USDC", "4", GEN, "2", "1", "38000", "-35000", "3000"))));
        assertEquals(0, new BigDecimal("5000").compareTo(p.chiusure.get(0).differenzialeLifo));
        assertEquals(0, new BigDecimal("8000").compareTo(p.chiusure.get(1).differenzialeLifo));
        assertEquals(1, p.chiusure.get(0).pezzi.size());
        assertEquals(0, new BigDecimal("40000").compareTo(p.chiusure.get(0).pezzi.get(0).prezzoApertura), "chiude per primo il lotto piu' recente");
        assertTrue(p.DivergePerAnno());
        var anni = p.perAnno();
        assertEquals(2, anni.size());
        assertEquals(0, new BigDecimal("-5000").compareTo(anni.get("2026")[2]));
        assertEquals(0, new BigDecimal("5000").compareTo(anni.get("2027")[2]));
        //il totale e' lo stesso: cambia solo la ripartizione fra gli anni
        BigDecimal lifo = anni.values().stream().map(t -> t[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal borsa = anni.values().stream().map(t -> t[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, lifo.compareTo(borsa));
    }

    /**
     * D18.9: in un episodio aperto al 31/12 quello che succede l'anno dopo (una chiusura che non torna, un'apertura senza prezzo)
     * toglie il LIFO alle chiusure di quell'anno, non a quelle dell'anno prima.
     */
    @Test
    void laVerificaFinoAllAnnoNonGuardaGliAnniDopo() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        List<String[]> r = righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "3", DIC, "2", "1", "45000", "-35000", "10000"),
                fill(LIN, "USDC", "4", GEN, "1", "1", "41000", "41000", "0"),
                fill(LIN, "USDC", "5", GEN + 1000, "2", "1", "38000", "-35000", "3000"));
        //all'apertura di gennaio manca il prezzo di esecuzione
        for (String[] v : r) {
            if (!"4".equals(v[24]) || !DerivatiLifo.IsRigaToken(v)) continue;
            Map<String, String> d = Derivati.Dati(v);
            d.remove("prezzo");
            Derivati.ImpostaDati(v, d);
        }
        var p = unica(DerivatiLifo.Calcola(r));
        var dicembre = p.chiusure.get(0);
        var gennaio = p.chiusure.get(1);
        assertTrue(dicembre.verificata);
        assertFalse(p.episodioVerificato(dicembre.episodio), "l'episodio intero non torna: in gennaio manca un prezzo");
        assertTrue(p.verificataFinoAllAnno(dicembre));
        assertFalse(p.verificataFinoAllAnno(gennaio));
    }

    /**
     * Una chiusura senza contratti aperti dopo una posizione finita (posizione aperta prima dei token, righe mancanti) sta in un
     * episodio a se': prima entrava nell'episodio appena chiuso e lo faceva tornare al profitto dell'exchange.
     */
    @Test
    void unaChiusuraSenzaContrattiApertiNonEntraNellEpisodioFinito() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "2", "1", "35000", "-30000", "5000"),
                fill(LIN, "USDC", "3", DIC, "2", "1", "38000", "-36000", "2000"))));
        var prima = p.chiusure.get(0);
        var orfana = p.chiusure.get(1);
        assertNotEquals(prima.episodio, orfana.episodio);
        assertTrue(p.episodioVerificato(prima.episodio));
        assertTrue(p.verificataFinoAllAnno(prima));
        assertEquals(0, BigDecimal.ONE.compareTo(orfana.scoperti));
        assertFalse(p.episodioVerificato(orfana.episodio), "nessun contratto aperto da cui ricostruirla");
        //un'apertura dopo comincia ancora un episodio nuovo
        var q = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "2", "1", "35000", "-30000", "5000"),
                fill(LIN, "USDC", "3", DIC, "2", "1", "38000", "-36000", "2000"),
                fill(LIN, "USDC", "4", GEN, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "5", GEN + 1000, "2", "1", "41000", "-40000", "1000"))));
        assertEquals(3, q.chiusure.stream().map(c -> c.episodio).distinct().count());
        assertTrue(q.episodioVerificato(q.chiusure.get(2).episodio));
    }

    /**
     * Due fill nello stesso millisecondo (la chiusura della posizione e l'apertura della successiva) seguono il billId, come in
     * {@code OKXDerivati.FiltraPerSaldoAperto}: prima si ordinavano per ID del movimento, che non segue il bill.
     */
    @Test
    void nelloStessoMillisecondoDecideIlBill() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        List<String[]> r = righe(
                fill(LIN, "USDC", "8", DIC - 10_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "10", DIC, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "9", DIC, "2", "1", "35000", "-30000", "5000"));
        java.util.Collections.reverse(r);
        var p = unica(DerivatiLifo.Calcola(r));
        var c = p.chiusure.get(0);
        assertEquals(0, new BigDecimal("30000").compareTo(c.pezzi.get(0).prezzoApertura), "la chiusura del bill 9 chiude l'apertura del bill 8");
        assertTrue(c.verificata);
        assertEquals(0, new BigDecimal("1").compareTo(p.contrattiAperti()));
        assertEquals(0, new BigDecimal("40000").compareTo(p.aperti.peek().prezzo), "l'apertura del bill 10 comincia la posizione dopo");
    }

    @Test
    void unaChiusuraSuPiuLottiLiConsumaDalPiuRecenteEDivideIlDifferenziale() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "3", DIC, "2", "2", "45000", "-70000", "20000"))));
        var c = p.chiusure.get(0);
        assertEquals(2, c.pezzi.size());
        assertEquals(0, new BigDecimal("5000").compareTo(c.pezzi.get(0).differenziale));
        assertEquals(0, new BigDecimal("15000").compareTo(c.pezzi.get(1).differenziale));
        assertEquals(0, new BigDecimal("20000").compareTo(c.differenzialeLifo));
    }

    @Test
    void unoShortGuadagnaSeIlPrezzoScende() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC, "2", "1", "100", "100", "0"),      //vendita che apre: short
                fill(LIN, "USDC", "2", DIC + 1000, "1", "1", "90", "-100", "10")))); //acquisto che chiude
        assertFalse(p.lungo);
        assertEquals(0, new BigDecimal("10").compareTo(p.chiusure.get(0).differenzialeLifo));
        assertFalse(p.DivergePerAnno());
    }

    @Test
    void unContrattoInversoDifferenziaInMonetaDelSottostante() {
        OKXDerivati.FonteValoreContratto = id -> "100"; //100 dollari per contratto
        //long su BTC-USD-SWAP regolato in BTC: 100 x (1/20000 - 1/25000) = 0,001 BTC
        var p = unica(DerivatiLifo.Calcola(righe(
                fill("BTC-USD-SWAP", "BTC", "1", DIC, "1", "1", "20000", "0.005", "0"),
                fill("BTC-USD-SWAP", "BTC", "2", DIC + 1000, "2", "1", "25000", "-0.005", "0.001"))));
        assertTrue(p.inverso);
        assertEquals(0, new BigDecimal("0.001").compareTo(p.chiusure.get(0).differenzialeLifo.setScale(9, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void senzaIlValoreDelContrattoNonSiCalcolaNulla() {
        OKXDerivati.FonteValoreContratto = id -> "";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC, "1", "1", "100", "100", "0"),
                fill(LIN, "USDC", "2", DIC + 1000, "2", "1", "110", "-100", "1"))));
        assertTrue(p.datiIncompleti);
        assertNull(p.chiusure.get(0).differenzialeLifo);
        assertNull(p.chiusure.get(0).differenza());
        assertTrue(p.perAnno().isEmpty());
    }

    @Test
    void lePosizioniLongEShortDelloStessoContrattoSonoSeparate() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        var m = DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC, "1", "1", "100", "100", "0"),
                fill(LIN, "USDC", "2", DIC + 1000, "2", "1", "110", "-100", "10"),
                fill(LIN, "USDC", "3", DIC + 2000, "2", "1", "110", "110", "0"),
                fill(LIN, "USDC", "4", DIC + 3000, "1", "1", "105", "-110", "5")));
        assertEquals(2, m.size());
        for (var p : m.values()) assertEquals(1, p.chiusure.size());
    }

    @Test
    void ilProfittoDellExchangeCheTornaConIlPrezzoMedioVerificaLaPosizione() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill(LIN, "USDC", "2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill(LIN, "USDC", "3", DIC, "2", "1", "45000", "-35000", "10000"),
                fill(LIN, "USDC", "4", GEN, "2", "1", "38000", "-35000", "3000"))));
        assertTrue(p.verificata());
        assertEquals(0, new BigDecimal("10000").compareTo(p.chiusure.get(0).pnlConPrezzoMedio));
        assertEquals(0, new BigDecimal("3000").compareTo(p.chiusure.get(1).pnlConPrezzoMedio));
    }

    @Test
    void unProfittoCheNonSiRicostruisceDaiPrezziNonVerificaLaPosizione() {
        OKXDerivati.FonteValoreContratto = id -> "1";
        //prezzi in un'unita' e profitto in un'altra (come i primi mesi dell'export Bybit): il LIFO non ha fondamento
        var p = unica(DerivatiLifo.Calcola(righe(
                fill(LIN, "USDC", "1", DIC, "1", "1", "0.00025", "0.00025", "0"),
                fill(LIN, "USDC", "2", DIC + 1000, "2", "1", "0.00026", "-0.00025", "0.058"))));
        assertFalse(p.verificata());
        assertFalse(p.chiusure.get(0).verificata);
    }

    @Test
    void unaPosizioneInversaVerificaConIlPrezzoMedioArmonico() {
        OKXDerivati.FonteValoreContratto = id -> "100";
        var p = unica(DerivatiLifo.Calcola(righe(
                fill("BTC-USD-SWAP", "BTC", "1", DIC, "1", "1", "20000", "0.005", "0"),
                fill("BTC-USD-SWAP", "BTC", "2", DIC + 1000, "1", "1", "25000", "0.004", "0"),
                //media armonica di 20000 e 25000 = 22222,22: chiudendo 2 contratti a 30000
                fill("BTC-USD-SWAP", "BTC", "3", DIC + 2000, "2", "2", "30000", "-0.009", "0.0023333333"))));
        assertTrue(p.inverso);
        assertTrue(p.verificata(), String.valueOf(p.chiusure.get(0).pnlConPrezzoMedio));
    }
}
