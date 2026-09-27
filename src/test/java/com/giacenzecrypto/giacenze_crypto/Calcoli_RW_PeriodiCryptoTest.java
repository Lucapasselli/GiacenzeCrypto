package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Calcoli_RW_PeriodiCrypto.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tratti CRYPTO di {@link Calcoli_RW_PeriodiCrypto#trattiCrypto(String, List, boolean)}: dove si spezza il
 * rigo cripto, con che bollo e con che modalità. Decisioni dell'utente del 2026-09-26: taglio a ogni
 * confine, buco = rigo senza periodo col bollo del gruppo, bollo del periodo dove c'è.
 */
class Calcoli_RW_PeriodiCryptoTest {

    private static String[] periodo(String tipo, String prog, String di, String df, String bollo,
            String modIni, String modFin) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = tipo;
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = prog;
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = di;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = df;
        r[Principale_GruppiWalletRW.COL_BOLLO] = bollo;
        r[Principale_GruppiWalletRW.COL_MOD_INIZIALE] = modIni;
        r[Principale_GruppiWalletRW.COL_MOD_FINALE] = modFin;
        return r;
    }

    private static String[] crypto(String prog, String di, String df, String bollo) {
        return periodo("CRYPTO", prog, di, df, bollo, "", "");
    }

    /** I periodi seminati per Binance da RW_Predefiniti.json. */
    private static List<String[]> binance() {
        return List.of(crypto("1", "", "2026-06-30", "SI"), crypto("2", "2026-07-01", "", "NO"));
    }

    private static void tratto(String[] t, String di, String df, String prog, String bollo) {
        assertEquals(di, t[TC_DATA_INIZIO]);
        assertEquals(df, t[TC_DATA_FINE]);
        assertEquals(prog, t[TC_PROGRESSIVO]);
        assertEquals(bollo, t[TC_BOLLO]);
    }

    @Test
    void senzaPeriodi_unTrattoSullAnnoColBolloDelGruppo() {
        List<String[]> t = trattiCrypto("2024", new ArrayList<>(), true);
        assertEquals(1, t.size());
        tratto(t.get(0), "2024-01-01", "2024-12-31", "", "SI");
        tratto(trattiCrypto("2024", null, false).get(0), "2024-01-01", "2024-12-31", "", "NO");
    }

    @Test
    void binance2026_dueTrattiConBolloDiverso() {
        List<String[]> t = trattiCrypto("2026", binance(), false);
        assertEquals(2, t.size());
        tratto(t.get(0), "2026-01-01", "2026-06-30", "1", "SI");
        tratto(t.get(1), "2026-07-01", "2026-12-31", "2", "NO");
        assertEquals(List.of(LocalDate.parse("2026-07-01")), dateDiTaglio(t));
    }

    @Test
    void binanceAnniPrecedenti_nessunTaglio() {
        List<String[]> t = trattiCrypto("2025", binance(), false);
        assertEquals(1, t.size());
        tratto(t.get(0), "2025-01-01", "2025-12-31", "1", "SI");
        assertTrue(dateDiTaglio(t).isEmpty());
    }

    @Test
    void confineFraPeriodiUguali_siTagliaLoStesso() {
        List<String[]> t = trattiCrypto("2024",
                List.of(crypto("1", "", "2024-06-30", "NO"), crypto("2", "2024-07-01", "", "NO")), false);
        assertEquals(2, t.size());
    }

    @Test
    void bucoFraPeriodi_trattoSenzaProgressivoColBolloDelGruppo() {
        List<String[]> t = trattiCrypto("2024",
                List.of(crypto("1", "2024-01-01", "2024-03-31", "NO"), crypto("2", "2024-07-01", "", "NO")), true);
        assertEquals(3, t.size());
        tratto(t.get(0), "2024-01-01", "2024-03-31", "1", "NO");
        tratto(t.get(1), "2024-04-01", "2024-06-30", "", "SI");
        tratto(t.get(2), "2024-07-01", "2024-12-31", "2", "NO");
    }

    @Test
    void bolloVuotoSulPeriodo_valeNO() {
        List<String[]> t = trattiCrypto("2024", List.<String[]>of(crypto("1", "", "", "")), true);
        tratto(t.get(0), "2024-01-01", "2024-12-31", "1", "NO");
    }

    @Test
    void periodiFiatIgnorati() {
        List<String[]> t = trattiCrypto("2024",
                List.<String[]>of(periodo("FIAT", "1", "2024-05-01", "", "", "", "")), false);
        assertEquals(1, t.size());
        tratto(t.get(0), "2024-01-01", "2024-12-31", "", "NO");
    }

    @Test
    void modalitaSoloSuiConfiniDellUtente() {
        List<String[]> t = trattiCrypto("2024", List.of(
                periodo("CRYPTO", "1", "", "2024-06-30", "NO",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA),
                periodo("CRYPTO", "2", "2024-07-01", "2024-09-30", "NO",
                        Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO, Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE)),
                false);
        assertEquals(3, t.size());
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI, t.get(0)[TC_MOD_INIZIALE], "1/1: periodo dal primo movimento");
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA, t.get(0)[TC_MOD_FINALE]);
        assertEquals(Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO, t.get(1)[TC_MOD_INIZIALE]);
        assertEquals(Principale_GruppiWalletRW.MOD_FINALE_SOMMA_USCITE, t.get(1)[TC_MOD_FINALE]);
        assertEquals("", t.get(2)[TC_MOD_INIZIALE], "buco: nessun confine dell'utente");
        assertEquals("", t.get(2)[TC_MOD_FINALE]);
    }

    @Test
    void trattoAllaData_trovaIlPezzoGiusto() {
        List<String[]> t = trattiCrypto("2026", binance(), false);
        assertEquals("1", trattoAllaData(t, "2026-06-30")[TC_PROGRESSIVO]);
        assertEquals("2", trattoAllaData(t, "2026-07-01")[TC_PROGRESSIVO]);
        assertNull(trattoAllaData(t, "2027-01-01"));
    }
}
