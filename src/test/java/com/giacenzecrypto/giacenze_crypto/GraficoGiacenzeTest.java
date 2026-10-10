package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * {@link GraficoGiacenze} e la parte senza rete di {@link PrezziGiornalieri}: giacenza a fine giornata, giorni da
 * prezzare, punti del valore (dalla tabella o col prezzo del giorno), buchi, giacenze negative, tratti da scaricare
 * e lettura dei punti dello script dei prezzi.
 */
class GraficoGiacenzeTest {

    private static long Istante(String yyyyMMddHHmmss) {
        return GraficoGiacenze.IstanteDaID(yyyyMMddHHmmss + "_1_1_DC");
    }

    private static GraficoGiacenze.Variazione V(String quando, String qta, String dopo, String valoreDopo) {
        return new GraficoGiacenze.Variazione(Istante(quando), quando + "_1_1_DC", new BigDecimal(qta), new BigDecimal(dopo),
                valoreDopo == null ? null : new BigDecimal(valoreDopo), quando, "DEPOSITO", "Binance");
    }

    /** 1/3 entrano 2, 1/3 sera escono 0.5, 3/3 escono 1.5 (giacenza zero), fine il 5/3 a mezzanotte. */
    private static final List<GraficoGiacenze.Variazione> TRE = List.of(
            V("20240301100000", "2", "2", "100.00"),
            V("20240301220000", "-0.5", "1.5", null),
            V("20240303120000", "-1.5", "0", null));
    private static final long FINE = Istante("20240305000000");

    @Test
    void laGiacenzaAFineGiornoVaDalPrimoMovimentoAllUltimoGiornoPrimaDellaFine() {
        NavigableMap<LocalDate, BigDecimal> g = GraficoGiacenze.GiacenzaFineGiorno(TRE, FINE);
        assertEquals(LocalDate.of(2024, 3, 1), g.firstKey());
        assertEquals(LocalDate.of(2024, 3, 4), g.lastKey(), "il 5/3 a mezzanotte e' escluso");
        assertEquals(0, new BigDecimal("1.5").compareTo(g.get(LocalDate.of(2024, 3, 1))));
        assertEquals(0, new BigDecimal("1.5").compareTo(g.get(LocalDate.of(2024, 3, 2))), "un giorno senza movimenti tiene la giacenza");
        assertEquals(0, g.get(LocalDate.of(2024, 3, 3)).signum());
        assertEquals(0, g.get(LocalDate.of(2024, 3, 4)).signum());
    }

    @Test
    void unaFineDentroLaGiornataLaTagliaLi() {
        //Data di riferimento = oggi: la fine e' l'istante attuale, l'ultimo giorno e' quello stesso
        long fine = Istante("20240301150000");
        NavigableMap<LocalDate, BigDecimal> g = GraficoGiacenze.GiacenzaFineGiorno(TRE, fine);
        assertEquals(1, g.size());
        assertEquals(0, new BigDecimal("2").compareTo(g.get(LocalDate.of(2024, 3, 1))), "il movimento delle 22 e' dopo la fine");
    }

    @Test
    void siPrezzanoSoloIGiorniConGiacenzaEQuelliDeiMovimentiSenzaValore() {
        SortedSet<LocalDate> giorni = GraficoGiacenze.GiorniDaPrezzare(TRE, FINE);
        assertEquals(new TreeSet<>(List.of(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 2))), giorni,
                "il 3 e il 4 finiscono a zero e il movimento del 3 porta la giacenza a zero: nessun prezzo serve");
    }

    @Test
    void ilValoreUsaLaTabellaPoiIlPrezzoDelGiornoEContaIBuchi() {
        Map<LocalDate, BigDecimal> prezzi = Map.of(LocalDate.of(2024, 3, 1), new BigDecimal("40"));
        GraficoGiacenze.Serie s = GraficoGiacenze.Costruisci(TRE, FINE, prezzi::get);
        assertEquals(1, s.GiorniSenzaPrezzo(), "il 2/3 ha giacenza e nessun prezzo");
        assertTrue(s.HaValori());

        List<GraficoGiacenze.PuntoValore> v = s.Valori();
        //In ordine: mov 1/3 10:00, mov 1/3 22:00, fine 1/3, fine 2/3, mov 3/3, fine 3/3, fine 4/3
        assertEquals(7, v.size());
        assertEquals("20240301100000_1_1_DC", v.get(0).Variazione().ID());
        assertEquals(0, new BigDecimal("100.00").compareTo(v.get(0).Valore()), "valore dalla tabella, non ricalcolato");
        assertNull(v.get(0).Prezzo());
        assertEquals(0, new BigDecimal("60.00").compareTo(v.get(1).Valore()), "senza valore in tabella: giacenza dopo per prezzo del giorno");
        assertNull(v.get(2).Variazione(), "fine giornata dopo i movimenti dello stesso giorno");
        assertEquals(0, new BigDecimal("60.00").compareTo(v.get(2).Valore()));
        assertNull(v.get(3).Valore(), "2/3 senza prezzo: buco");
        assertEquals(0, v.get(4).Valore().signum(), "giacenza zero vale zero anche senza prezzo");
        assertEquals(0, v.get(6).Valore().signum());
        for (int i = 1; i < v.size(); i++) assertTrue(v.get(i - 1).Istante() <= v.get(i).Istante());
    }

    @Test
    void unaGiacenzaNegativaHaUnValoreNegativo() {
        List<GraficoGiacenze.Variazione> neg = List.of(V("20240301100000", "-1", "-1", null));
        GraficoGiacenze.Serie s = GraficoGiacenze.Costruisci(neg, Istante("20240302000000"), g -> new BigDecimal("10"));
        assertEquals(0, new BigDecimal("-10.00").compareTo(s.Valori().get(1).Valore()));
    }

    @Test
    void senzaMovimentiNonCiSonoPunti() {
        GraficoGiacenze.Serie s = GraficoGiacenze.Costruisci(List.of(), FINE, g -> BigDecimal.ONE);
        assertTrue(s.Valori().isEmpty());
        assertFalse(s.HaValori());
        assertTrue(GraficoGiacenze.GiorniDaPrezzare(List.of(), FINE).isEmpty());
    }

    @Test
    void lApertureDelGiornoDopoEIlPrezzoDiFineGiornata() {
        long oggi = LocalDate.of(2024, 3, 10).toEpochDay();
        assertEquals(LocalDate.of(2024, 3, 2).toEpochDay(), PrezziGiornalieri.GiornoUTC(LocalDate.of(2024, 3, 1), oggi));
        assertEquals(oggi, PrezziGiornalieri.GiornoUTC(LocalDate.of(2024, 3, 10), oggi), "oggi: l'apertura di oggi, domani non c'e'");
        //Candela giornaliera che parte alle 16:00 UTC (fuso di Hong Kong): e' del giorno dopo
        long giorno = LocalDate.of(2024, 3, 1).toEpochDay();
        assertEquals(giorno + 1, PrezziGiornalieri.GiornoUTCDaIstante(giorno * PrezziGiornalieri.GIORNO_MS + 16 * 3_600_000L));
        assertEquals(giorno, PrezziGiornalieri.GiornoUTCDaIstante(giorno * PrezziGiornalieri.GIORNO_MS + 61_000L));
    }

    @Test
    void iGiorniVicinisiChiedonoInUnTrattoSolo() {
        List<long[]> t = PrezziGiornalieri.Tratti(new TreeSet<>(List.of(1L, 2L, 3L, 20L, 100L, 101L)));
        assertEquals(2, t.size());
        assertArrayEquals(new long[]{1, 20}, t.get(0));
        assertArrayEquals(new long[]{100, 101}, t.get(1));
    }

    @Test
    void ilPuntoDelloScriptPrendeIlPrimoExchangeNellOrdine() {
        var punto = JsonParser.parseString("{\"timestamp\":1709251200000,\"prices\":{\"okx\":51.5,\"binance\":50.25}}");
        assertEquals(50.25, PrezziGiornalieri.PrezzoDelPunto(punto, List.of("binance", "okx")));
        var vuoto = JsonParser.parseString("{\"timestamp\":1709251200000,\"prices\":{}}");
        assertNull(PrezziGiornalieri.PrezzoDelPunto(vuoto, List.of("binance")));
    }

    @Test
    void laFonteSegueLeRegoleDellaValorizzazione() {
        assertEquals(PrezziGiornalieri.TipoFonte.EURO, PrezziGiornalieri.FonteDi("EUR", "FIAT", "", "").Tipo());
        assertEquals(PrezziGiornalieri.TipoFonte.USD, PrezziGiornalieri.FonteDi("USD", "FIAT", "", "").Tipo());
        PrezziGiornalieri.Fonte btc = PrezziGiornalieri.FonteDi("btc", "Crypto", "", "");
        assertEquals(PrezziGiornalieri.TipoFonte.EXCHANGE, btc.Tipo());
        assertEquals("BTC", btc.Chiave());
        assertEquals(PrezziGiornalieri.TipoFonte.NESSUNA, PrezziGiornalieri.FonteDi("", "Crypto", "", "").Tipo());
    }
}
