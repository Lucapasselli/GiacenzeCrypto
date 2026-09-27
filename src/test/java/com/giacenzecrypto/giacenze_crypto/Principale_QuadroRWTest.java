package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Righi CRYPTO della tabella di sintesi del quadro RW ({@link Principale_QuadroRW#RighiCrypto}): un rigo per
 * gruppo e per tratto, bollo del tratto, fotografia nei tratti col bollo, valore iniziale sul wallet di origine
 * nel tratto giusto. Tutto in memoria: righe di dettaglio, tratti e alias costruiti a mano.
 */
class Principale_QuadroRWTest {

    private static final Function<String, String[]> ALIAS = g -> new String[] {g, "Alias " + g, "N", ""};

    private static String[] tratto(String di, String df, String prog, String bollo) {
        String[] t = new String[Calcoli_RW_PeriodiCrypto.TC_COLONNE];
        Arrays.fill(t, "");
        t[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO] = di;
        t[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE] = df;
        t[Calcoli_RW_PeriodiCrypto.TC_PROGRESSIVO] = prog;
        t[Calcoli_RW_PeriodiCrypto.TC_BOLLO] = bollo;
        return t;
    }

    private static List<String[]> annoIntero(String bollo) {
        return List.<String[]>of(tratto("2024-01-01", "2024-12-31", "", bollo));
    }

    private static List<String[]> dueTratti(String bollo1, String bollo2) {
        return List.of(tratto("2024-01-01", "2024-06-30", "1", bollo1), tratto("2024-07-01", "2024-12-31", "2", bollo2));
    }

    /** Riga di dettaglio String[17] del motore. */
    private static String[] riga(String grIni, String dataIni, String valIni, String grFin, String dataFin,
            String valFin, String giorni) {
        String[] r = new String[17];
        Arrays.fill(r, "");
        r[0] = "2024";
        r[1] = grIni; r[2] = "BTC"; r[3] = "1"; r[4] = dataIni; r[5] = valIni;
        r[6] = grFin; r[7] = "BTC"; r[8] = "1"; r[9] = dataFin; r[10] = valFin;
        r[11] = giorni; r[12] = "Fine Anno"; r[13] = "x"; r[14] = "y"; r[15] = "<html></html>";
        return r;
    }

    private static Map<String, List<String[]>> liste(String gruppo, String[]... righe) {
        Map<String, List<String[]>> m = new HashMap<>();
        m.put(gruppo, new ArrayList<>(List.of(righe)));
        return m;
    }

    private static void num(String atteso, String reale) {
        num(atteso, reale, "");
    }

    private static void num(String atteso, String reale, String messaggio) {
        assertEquals(0, new java.math.BigDecimal(atteso).compareTo(new java.math.BigDecimal(reale)),
                messaggio + " atteso " + atteso + ", trovato " + reale);
    }

    private static Map<String, String[]> quadro(Map<String, List<String[]>> liste, Map<String, List<String[]>> foto,
            Function<String, List<String[]>> tratti, boolean inizioSuOrigine, boolean mostraGiacenze) {
        return Principale_QuadroRW.RighiCrypto(liste, foto, tratti, ALIAS, inizioSuOrigine, false, mostraGiacenze);
    }

    @Test
    void senzaPeriodi_rigoUnicoConLaChiaveDiSempre() {
        Map<String, String[]> q = quadro(liste("Wallet 01",
                riga("Wallet 01", "2024-01-01 00:00", "1000", "Wallet 01", "2024-12-31 23:59", "2000", "366")),
                null, g -> annoIntero("NO"), false, false);
        assertEquals(1, q.size());
        String[] r = q.get("Wallet 01");
        assertEquals("01 ( Alias Wallet 01 )", r[0]);
        assertEquals("Wallet 01|CRYPTO|", r[6]);
        num("1000", r[1]);
        num("2000", r[2]);
        assertEquals("366.00", r[3]);
        assertEquals("NO", r[7]);
        assertEquals("4.01", r[5], "2000 / 365 × 366 × 2‰");
    }

    @Test
    void dueTratti_dueRighiSeparati_conDateNellEtichettaENellaChiave() {
        Map<String, String[]> q = quadro(liste("Wallet 01",
                riga("Wallet 01", "2024-01-01 00:00", "1000", "Wallet 01", "2024-06-30 23:59", "1500", "182"),
                riga("Wallet 01", "2024-07-01 00:00", "1500", "Wallet 01", "2024-12-31 23:59", "3000", "184")),
                null, g -> dueTratti("NO", "NO"), false, false);
        assertEquals(2, q.size());
        String[] p1 = q.get("Wallet 01|2024-01-01");
        String[] p2 = q.get("Wallet 01|2024-07-01");
        assertEquals("01 ( Alias Wallet 01 ) [2024-01-01 / 2024-06-30]", p1[0]);
        assertEquals("Wallet 01|CRYPTO|2024-01-01/2024-06-30", p1[6]);
        num("1000", p1[1]);
        num("1500", p1[2]);
        num("1500", p2[1], "il valore iniziale del secondo periodo non si somma al primo");
        num("3000", p2[2]);
        assertEquals("Wallet 01|CRYPTO|2024-07-01/2024-12-31", p2[6]);
    }

    @Test
    void bolloDelTratto_imposta0SoloDoveIlBolloEPagato() {
        Map<String, String[]> q = quadro(liste("Wallet 01",
                riga("Wallet 01", "2024-01-01 00:00", "1000", "Wallet 01", "2024-06-30 23:59", "1500", "182"),
                riga("Wallet 01", "2024-07-01 00:00", "1500", "Wallet 01", "2024-12-31 23:59", "3000", "184")),
                null, g -> dueTratti("SI", "NO"), false, false);
        String[] p1 = q.get("Wallet 01|2024-01-01");
        String[] p2 = q.get("Wallet 01|2024-07-01");
        assertEquals("SI", p1[7]);
        assertEquals("0.00", p1[5]);
        assertEquals("NO", p2[7]);
        assertNotEquals("0.00", p2[5]);
    }

    @Test
    void mostraGiacenze_nelTrattoColBolloLaFotografiaSostituisceIlCalcolo() {
        Map<String, List<String[]>> liste = liste("Wallet 01",
                riga("Wallet 01", "2024-03-01 10:00", "700", "Wallet 01", "2024-06-30 23:59", "800", "122"),
                riga("Wallet 01", "2024-07-01 00:00", "1500", "Wallet 01", "2024-12-31 23:59", "3000", "184"));
        Map<String, List<String[]>> foto = liste("Wallet 01",
                riga("Wallet 01", "2024-01-01 00:00", "1000", "Wallet 01", "2024-06-30 23:59", "1500", "182"),
                riga("Wallet 01", "2024-07-01 00:00", "1500", "Wallet 01", "2024-12-31 23:59", "2900", "184"));

        Map<String, String[]> q = quadro(liste, foto, g -> dueTratti("SI", "NO"), false, true);
        String[] p1 = q.get("Wallet 01|2024-01-01");
        num("1000", p1[1], "valore iniziale della fotografia");
        num("1500", p1[2]);
        assertEquals("182.00", p1[3], "giorni del tratto, non la media ponderata");
        num("3000", q.get("Wallet 01|2024-07-01")[2], "il tratto senza bollo resta calcolato");
        assertEquals(2, liste.get("Wallet 01").size(), "il dettaglio mostra la fotografia al posto del calcolo");
        assertTrue(liste.get("Wallet 01").stream().anyMatch(r -> r[4].equals("2024-01-01 00:00")));
    }

    @Test
    void inizioSuWalletOrigine_ilValoreInizialeVaNelTrattoDiOrigineCheContieneLaData() {
        Map<String, List<String[]>> liste = liste("Wallet 01",
                riga("Wallet 02", "2024-08-01 10:00", "900", "Wallet 01", "2024-12-31 23:59", "1000", "153"));
        liste.put("Wallet 02", new ArrayList<>());
        Map<String, String[]> q = quadro(liste, null,
                g -> g.equals("Wallet 02") ? dueTratti("NO", "NO") : annoIntero("NO"), true, false);
        num("0.00", q.get("Wallet 01")[1]);
        num("1000", q.get("Wallet 01")[2]);
        num("900", q.get("Wallet 02|2024-07-01")[1]);
        num("0.00", q.get("Wallet 02|2024-01-01")[1]);
    }

    @Test
    void erroreDiUnaRiga_marcaSoloIlRigoDelSuoPeriodo() {
        String[] errata = riga("Wallet 01", "2024-01-01 00:00", "1000", "Wallet 01", "2024-06-30 23:59", "1500", "182");
        errata[15] = "<html>Errore (Giacenza Negativa)<br>";
        Map<String, String[]> q = quadro(liste("Wallet 01", errata,
                riga("Wallet 01", "2024-07-01 00:00", "1500", "Wallet 01", "2024-12-31 23:59", "3000", "184")),
                null, g -> dueTratti("NO", "NO"), false, false);
        assertEquals("ERRORI", q.get("Wallet 01|2024-01-01")[4]);
        assertEquals("", q.get("Wallet 01|2024-07-01")[4]);
    }

    @Test
    void gruppoSenzaRighe_unRigoVuotoPerTratto() {
        Map<String, List<String[]>> liste = new HashMap<>();
        liste.put("Wallet 03", new ArrayList<>());
        Map<String, String[]> q = quadro(liste, null, g -> dueTratti("NO", "NO"), false, false);
        assertEquals(2, q.size());
        num("0.00", q.get("Wallet 03|2024-01-01")[2]);
    }
}
