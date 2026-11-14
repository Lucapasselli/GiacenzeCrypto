package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Principale_DepositiPrelievi}: la classificazione automatica di depositi e prelievi non lascia mai una classificazione
 * a metà e non si ferma al primo movimento che dà errore. Dati inventati.
 *
 * <p>I casi di errore sono quelli veri: lo scambio differito che toglie i due movimenti dalla mappa e poi fallisce nel calcolo
 * (prima di questa protezione i due movimenti sparivano), e l'abbinamento con commissione che imposta {@code [5]} e {@code [18]}
 * e poi fallisce nel calcolo del valore (restavano un {@code PTW} e un {@code DTW} senza {@code [20]}).</p>
 */
class Principale_DepositiPrelieviTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID, String exchange, String moneta, String qta, String valore) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2025-03-10 12:00";
        v[2] = "1 di 1";
        v[3] = exchange;
        v[4] = "Wallet";
        if (qta.startsWith("-")) {
            v[5] = "PRELIEVO CRYPTO";
            v[8] = moneta; v[9] = "Crypto"; v[10] = qta;
        } else {
            v[5] = "DEPOSITO CRYPTO";
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = valore;
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    private static Map<String, List<String>> contenuto() {
        Map<String, List<String>> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String[]> e : MappaCryptoWallet.entrySet()) m.put(e.getKey(), Arrays.asList(e.getValue().clone()));
        return m;
    }

    @Test
    void idPresenti_togliGliIDChe_nonSonoPiuInMappa_conservandoLOrdine() {
        String[] a = movimento("20250310120000_A_001_001_PC", "A", "X", "-1", "1.00");
        String[] c = movimento("20250310120000_C_001_001_DC", "C", "X", "1", "1.00");
        MappaCryptoWallet.put(a[0], a);
        MappaCryptoWallet.put(c[0], c);
        List<String> lista = new ArrayList<>(List.of(a[0], "20250310120000_B_001_001_PC", c[0]));

        assertEquals(List.of(a[0], c[0]), Principale_DepositiPrelievi.IDPresenti(lista, MappaCryptoWallet));
        assertTrue(Principale_DepositiPrelievi.IDPresenti(null, MappaCryptoWallet).isEmpty());
    }

    @Test
    void idPresenti_eUnaCopia_ChiLaScorrePuoCambiareLaMappa() {
        String[] a = movimento("20250310120000_A_001_001_PC", "A", "X", "-1", "1.00");
        String[] c = movimento("20250310120000_C_001_001_DC", "C", "X", "1", "1.00");
        MappaCryptoWallet.put(a[0], a);
        MappaCryptoWallet.put(c[0], c);
        List<String> lista = List.of(a[0], c[0]);

        for (String id : Principale_DepositiPrelievi.IDPresenti(lista, MappaCryptoWallet)) {
            MappaCryptoWallet.remove(id); //senza ConcurrentModificationException
        }
        assertTrue(MappaCryptoWallet.isEmpty());
    }

    @Test
    void atomico_seRiesce_tieneLeModifiche() {
        String[] a = movimento("20250310120000_A_001_001_PC", "A", "X", "-1", "1.00");
        MappaCryptoWallet.put(a[0], a);
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        boolean r = esito.atomico("prova", () -> {
            a[5] = "CAMBIATO";
            return true;
        }, false);

        assertTrue(r);
        assertEquals("CAMBIATO", MappaCryptoWallet.get(a[0])[5]);
        assertTrue(esito.problemi.isEmpty());
    }

    @Test
    void atomico_seFallisce_ripristinaRigheTolteAggiunteEModificate() {
        String[] a = movimento("20250310120000_A_001_001_PC", "A", "X", "-1", "1.00");
        String[] c = movimento("20250310120000_C_001_001_DC", "C", "X", "1", "1.00");
        MappaCryptoWallet.put(a[0], a);
        MappaCryptoWallet.put(c[0], c);
        Map<String, List<String>> prima = contenuto();
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        Boolean r = esito.atomico("prova", () -> {
            a[18] = "PTW - qualcosa";           //modifica sul posto
            a[20] = c[0];
            MappaCryptoWallet.remove(c[0]);     //riga tolta...
            c[0] = "20250310120000_C_001_099_DC"; //...e rinominata
            MappaCryptoWallet.put("20250310120000_Z_001_001_CM", movimento("20250310120000_Z_001_001_CM", "Z", "X", "1", "1.00")); //aggiunta
            throw new IllegalStateException("guasto");
        }, false);

        assertFalse(r);
        assertEquals(prima, contenuto(), "la mappa è esattamente com'era");
        assertSame(a, MappaCryptoWallet.get(a[0]), "stessi oggetti: chi ne tiene un riferimento lo ritrova valido");
        assertSame(c, MappaCryptoWallet.get("20250310120000_C_001_001_DC"));
        assertEquals("20250310120000_C_001_001_DC", c[0]);
        assertEquals(1, esito.problemi.size());
        assertTrue(esito.problemi.get(0).contains("prova"));
    }

    @Test
    void protetto_annotaEVaAvanti() {
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();
        int r = esito.protetto("fase", () -> {
            throw new NullPointerException("x");
        }, 7);
        assertEquals(7, r);
        assertEquals(1, esito.problemi.size());
    }

    @Test
    void scambioDifferito_cheFallisceNelCalcolo_nonFaSparireIMovimenti() {
        //Il deposito ha la quantità vuota: il calcolo ("new BigDecimal") fallisce DOPO che la funzione ha tolto i due movimenti
        String[] p = movimento("20250310120000_Prova_001_001_PC", "Prova (BSC)", "TKNA", "-10", "50.00");
        String[] d = movimento("20250310120000_Prova_001_002_DC", "Prova (BSC)", "ETH", "0.02", "50.00");
        d[13] = "";
        MappaCryptoWallet.put(p[0], p);
        MappaCryptoWallet.put(d[0], d);
        Map<String, List<String>> prima = contenuto();
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        boolean r = esito.atomico("scambio differito", () -> GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], d[0]), false);

        assertFalse(r);
        assertEquals(prima, contenuto(), "prelievo e deposito sono ancora lì, con i loro ID e senza classificazione");
        assertEquals("", p[18]);
        assertEquals("", d[18]);
        assertEquals(1, esito.problemi.size());
    }

    @Test
    void abbinamentoConCommissione_cheFallisceNelCalcolo_nonLasciaPTWeDTWNonCollegati() {
        //Prelevati 10.1, depositati 10: la commissione si calcola dal valore del prelievo, che manca. La funzione ha già
        //scritto [5] e [18] su entrambi quando fallisce
        String[] p = movimento("20250310130000_Uno_001_001_PC", "Uno (BSC)", "TKNB", "-10.1", "");
        String[] d = movimento("20250310130000_Due_001_001_DC", "Due (BSC)", "TKNB", "10", "1.00");
        MappaCryptoWallet.put(p[0], p);
        MappaCryptoWallet.put(d[0], d);
        Map<String, List<String>> prima = contenuto();
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        boolean r = esito.atomico("trasferimento", () -> {
            GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);
            return true;
        }, false);

        assertFalse(r);
        assertEquals(prima, contenuto());
        assertEquals("", p[18]);
        assertEquals("", d[18]);
        assertEquals("", p[20]);
        assertEquals(2, MappaCryptoWallet.size(), "nessuna commissione orfana");
    }

    @Test
    void unaCoppiaFallita_nonImpediscePerLeAltre() {
        String[] p1 = movimento("20250310130000_Uno_001_001_PC", "Uno (BSC)", "TKNB", "-10.1", "");
        String[] d1 = movimento("20250310130000_Due_001_001_DC", "Due (BSC)", "TKNB", "10", "1.00");
        String[] p2 = movimento("20250310140000_Tre_001_001_PC", "Tre (BSC)", "TKNC", "-5", "5.00");
        String[] d2 = movimento("20250310140000_Quattro_001_001_DC", "Quattro (BSC)", "TKNC", "5", "5.00");
        for (String[] v : List.of(p1, d1, p2, d2)) MappaCryptoWallet.put(v[0], v);
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();
        int fatte = 0;

        //Come la FASE 1: ogni coppia è un'unità, e un errore non interrompe il resto
        for (String[][] coppia : List.of(new String[][]{p1, d1}, new String[][]{p2, d2})) {
            String idP = coppia[0][0], idD = coppia[1][0];
            if (esito.atomico("trasferimento " + idP, () -> {
                GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(idP, idD);
                return true;
            }, false)) fatte++;
        }

        assertEquals(1, fatte);
        assertEquals(1, esito.problemi.size());
        assertEquals("", p1[18], "la prima è rimasta com'era");
        assertTrue(p2[18].startsWith("PTW"), "la seconda è stata classificata");
        assertTrue(d2[18].startsWith("DTW"));
        assertEquals(d2[0], p2[20]);
        assertEquals(p2[0], d2[20]);
    }

    @Test
    void fotografiaDellaMappa_costaPoco_ancheConUnArchivioGrande() {
        for (int i = 0; i < 30000; i++) {
            String id = String.format("2025%010d_W_001_001_DC", i);
            MappaCryptoWallet.put(id, movimento(id, "W", "X", "1", "1.00"));
        }
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();
        long t0 = System.nanoTime();
        for (int k = 0; k < 20; k++) esito.atomico("niente", () -> true, false);
        long ms = (System.nanoTime() - t0) / 1_000_000 / 20;
        System.out.println("Fotografia di 30000 movimenti: " + ms + " ms a chiamata");
        assertTrue(ms < 500, "una fotografia deve restare trascurabile rispetto a una classificazione: " + ms + " ms");
    }
}
