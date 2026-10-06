package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link GruppoOperazione}, la chiave di operazione nel campo 45, separata dal 2026-10-05 dalle commissioni
 * collegate del campo 43: la migrazione delle righe scritte prima, la copia e la duplicazione, il dettaglio.
 */
class GruppoOperazioneTest {

    @BeforeEach
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2022-05-12 06:24";
        v[3] = "Binance";
        v[5] = "PRELIEVO CRYPTO";
        v[8] = "USDT";
        v[10] = "-100";
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    @Test
    void laRigaHaUnCampoPerLOperazione() {
        assertEquals(46, Importazioni.ColonneTabella);
        assertEquals(45, GruppoOperazione.CAMPO);
        assertNotEquals(CommissioniCollegate.CAMPO, GruppoOperazione.CAMPO);
    }

    @Test
    void migrazione_spostaIlContrattoDalCampoDelleCommissioni() {
        String[] v = movimento("20220512062450_Binance_001_001_PC");
        v[CommissioniCollegate.CAMPO] = "DUAL-1232611";

        assertTrue(GruppoOperazione.MigraAllaFormaAttuale(v));

        assertEquals("DUAL-1232611", GruppoOperazione.Chiave(v));
        assertEquals("", CommissioniCollegate.Chiave(v));
        assertFalse(GruppoOperazione.MigraAllaFormaAttuale(v), "una seconda volta non cambia nulla");
    }

    @Test
    void migrazione_lasciaStareLeCommissioni() {
        String[] v = movimento("20220512062450_Binance_001_001_PC");
        v[CommissioniCollegate.CAMPO] = "3f1c2a7e-commissione";

        assertFalse(GruppoOperazione.MigraAllaFormaAttuale(v));

        assertEquals("3f1c2a7e-commissione", CommissioniCollegate.Chiave(v));
        assertEquals("", GruppoOperazione.Chiave(v));
    }

    @Test
    void unaRigaCortaDiUnaVersionePrecedente_siAllungaEPoiSiMigra() {
        //Come la legge il caricamento: 45 campi, il contratto nel 43
        String[] vecchia = new String[45];
        java.util.Arrays.fill(vecchia, "");
        vecchia[0] = "20220512062450_Binance_001_001_PC";
        vecchia[43] = "DUAL-7";
        String[] v = java.util.Arrays.copyOf(vecchia, Importazioni.ColonneTabella);
        Importazioni.RiempiVuotiArray(v);

        GruppoOperazione.MigraAllaFormaAttuale(v);

        assertEquals("DUAL-7", v[45]);
        assertEquals("", v[43]);
    }

    @Test
    void ilDuplicato_nonFaParteDellOperazione() {
        String[] v = movimento("20220512062450_Binance_001_001_PC");
        GruppoOperazione.Scrivi(v, "DUAL-7");

        assertTrue(Funzioni.DuplicaMovimento(v[0]));

        for (String[] m : MappaCryptoWallet.values()) {
            if (m != v) assertEquals("", GruppoOperazione.Chiave(m), "il duplicato è un movimento indipendente");
        }
        assertEquals("DUAL-7", GruppoOperazione.Chiave(v));
    }

    @Test
    void dettaglio_mostraLOperazioneEPoiLeCommissioni() {
        String[] p = movimento("20220512062450_Binance_001_001_PC");
        String[] s = movimento("20220524083341_Binance_002_001_DC");
        String[] fee = movimento("20220512062450_Binance_001_002_CM");
        GruppoOperazione.Scrivi(p, "DUAL-7");
        GruppoOperazione.Scrivi(s, "DUAL-7");
        p[CommissioniCollegate.CAMPO] = "k";
        fee[CommissioniCollegate.CAMPO] = "k";

        List<String[]> righe = OperazioniCalcolate.RigheDettaglio(p[0]);

        //L'operazione comprende Settlement e commissione, la riga delle commissioni dice di chi è la fee
        assertEquals("Contratto Dual Investment 7 (3 movimenti)", righe.get(0)[0]);
        assertTrue(righe.get(0)[1].contains(s[0]));
        assertTrue(righe.get(0)[1].contains(fee[0]));
        assertEquals("Commissioni collegate", righe.get(1)[0]);
        assertTrue(righe.get(1)[1].contains(fee[0]));
        assertEquals("", GruppoOperazione.Chiave(fee), "la commissione non porta l'identità del contratto");
    }

    @Test
    void migrazione_togliendoLaChiaveDaiGenerati() {
        String[] g = movimento("20220512062450_Binance_001A_001_DC");
        g[22] = "AU";
        GruppoOperazione.Scrivi(g, "DUAL-7");

        assertTrue(GruppoOperazione.MigraAllaFormaAttuale(g));

        assertEquals("", GruppoOperazione.Chiave(g));
    }

    @Test
    void chiaveEffettiva_diUnGenerato_vieneDagliOriginaliDelGruppo() {
        String[] p = movimento("20220512062450_Binance_001_001_PC");
        String[] s = movimento("20220524083341_Binance_002_001_DC");
        String[] g = movimento("20220512062450_Binance_001A_001_DC");
        g[22] = "AU";
        p[20] = g[0] + "," + s[0];
        s[20] = p[0] + "," + g[0];
        g[20] = p[0] + "," + s[0];
        GruppoOperazione.Scrivi(p, "DUAL-7");
        GruppoOperazione.Scrivi(s, "DUAL-7");

        assertEquals("DUAL-7", GruppoOperazione.ChiaveEffettiva(g));
        assertEquals(java.util.Set.of(p[0], s[0], g[0]), new java.util.HashSet<>(GruppoOperazione.Membri("DUAL-7")));

        //Originali di due contratti diversi: il generato non ne prende nessuno
        GruppoOperazione.Scrivi(s, "DUAL-8");
        assertEquals("", GruppoOperazione.ChiaveEffettiva(g));
        //Un movimento originale non ricava nulla dal gruppo: la sua chiave è solo quella scritta
        assertEquals("", GruppoOperazione.ChiaveEffettiva(movimento("20230101100000_Binance_009_001_PC")));
    }
}
