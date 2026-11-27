package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il mark price dei contratti derivati ({@link MarkPriceDerivati}): lettura delle risposte di OKX e di Bybit, tolleranza sulla candela,
 * esiti certi e non, cache in {@code prezzi.mv.db}. Risposte e numeri inventati, nella forma dei due endpoint pubblici. Senza rete.
 */
class MarkPriceDerivatiTest {

    @TempDir
    static Path tempDir;

    /** Un minuto qualsiasi, fuori dall'archivio vero. */
    private static final long MINUTO = 1_767_222_000_000L;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private MarkPriceDerivati.Fonte originale;

    @BeforeEach
    void prepara() throws Exception {
        originale = MarkPriceDerivati.FONTE;
        MarkPriceDerivati.AzzeraMemoria();
        try (Statement st = DatabaseH2.connectionPrezzi.createStatement()) {
            st.execute("DELETE FROM PrezziMarkDerivati");
        }
    }

    @AfterEach
    void ripristina() {
        MarkPriceDerivati.FONTE = originale;
        MarkPriceDerivati.AzzeraMemoria();
    }

    private static String okx(String ts, String chiusura) {
        return "{\"code\":\"0\",\"data\":[[\"" + ts + "\",\"87682.8\",\"87682.9\",\"87564.4\",\"" + chiusura + "\",\"1\"]],\"msg\":\"\"}";
    }

    private static String bybit(String ts, String chiusura) {
        return "{\"retCode\":0,\"retMsg\":\"OK\",\"result\":{\"symbol\":\"BTCUSDT\",\"category\":\"linear\",\"list\":[[\"" + ts
                + "\",\"87691.3\",\"87693.3\",\"87577.89\",\"" + chiusura + "\"]]},\"retExtInfo\":{},\"time\":1795553704476}";
    }

    @Test
    void okx_laChiusuraDellaCandelaDelMinuto() {
        MarkPriceDerivati.Esito e = MarkPriceDerivati.InterpretaOkx(200, okx(String.valueOf(MINUTO), "87594.1"), MINUTO);
        assertEquals(0, new BigDecimal("87594.1").compareTo(e.prezzo()));
        assertTrue(e.certo());
    }

    @Test
    void bybit_laChiusuraDellaCandelaDelMinuto() {
        MarkPriceDerivati.Esito e = MarkPriceDerivati.InterpretaBybit(200, bybit(String.valueOf(MINUTO), "87596.52"), MINUTO);
        assertEquals(0, new BigDecimal("87596.52").compareTo(e.prezzo()));
    }

    @Test
    void laCandelaDopoIlMinutoOMoltoVecchiaNonVale() {
        MarkPriceDerivati.Esito dopo = MarkPriceDerivati.InterpretaOkx(200, okx(String.valueOf(MINUTO + 60_000L), "1"), MINUTO);
        assertNull(dopo.prezzo());
        assertTrue(dopo.certo());
        MarkPriceDerivati.Esito vecchia = MarkPriceDerivati.InterpretaOkx(200, okx(String.valueOf(MINUTO - 600_000L), "1"), MINUTO);
        assertNull(vecchia.prezzo());
        //Una candela di pochi minuti prima (lacuna del feed) vale
        MarkPriceDerivati.Esito poco = MarkPriceDerivati.InterpretaOkx(200, okx(String.valueOf(MINUTO - 120_000L), "5"), MINUTO);
        assertEquals(0, BigDecimal.valueOf(5).compareTo(poco.prezzo()));
    }

    @Test
    void ilContrattoInesistenteEUnEsitoCerto_unErroreNo() {
        MarkPriceDerivati.Esito scaduto = MarkPriceDerivati.InterpretaOkx(200,
                "{\"code\":\"51001\",\"data\":[],\"msg\":\"Instrument ID, Instrument ID code, or Spread ID doesn't exist.\"}", MINUTO);
        assertNull(scaduto.prezzo());
        assertTrue(scaduto.certo());
        MarkPriceDerivati.Esito regolato = MarkPriceDerivati.InterpretaOkx(200, "{\"code\":\"50047\",\"data\":[],\"msg\":\"has already settled\"}", MINUTO);
        assertTrue(regolato.certo());
        assertFalse(MarkPriceDerivati.InterpretaOkx(200, "{\"code\":\"50011\",\"data\":[],\"msg\":\"Too many requests\"}", MINUTO).certo());
        assertFalse(MarkPriceDerivati.InterpretaOkx(500, "", MINUTO).certo());
        assertFalse(MarkPriceDerivati.InterpretaOkx(200, "non e' json", MINUTO).certo());
        assertTrue(MarkPriceDerivati.InterpretaBybit(200, "{\"retCode\":10001,\"retMsg\":\"params error\"}", MINUTO).certo());
        assertFalse(MarkPriceDerivati.InterpretaBybit(200, "{\"retCode\":10006,\"retMsg\":\"Too many visits\"}", MINUTO).certo());
    }

    @Test
    void soloOkxEBybitSonoSupportati() {
        assertEquals("OKX", MarkPriceDerivati.Normalizza("OKX"));
        assertEquals("BYBIT", MarkPriceDerivati.Normalizza("Bybit Derivati"));
        assertEquals("BYBIT", MarkPriceDerivati.Normalizza("Bybit UTA"));
        assertNull(MarkPriceDerivati.Normalizza("Binance"));
        assertNull(MarkPriceDerivati.Normalizza(null));
        assertNull(MarkPriceDerivati.Mark("Binance", "BTCUSDT", false, MINUTO + 60_000L), "senza un exchange supportato non si interroga nulla");
    }

    @Test
    void unaLetturaRiuscitaSiSalvaESiRitrovaSenzaRete() {
        AtomicInteger chiamate = new AtomicInteger();
        MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
            chiamate.incrementAndGet();
            assertEquals("OKX", p);
            assertEquals("BTC-USDT-SWAP", s);
            assertEquals(MINUTO, minuto, "il minuto e' quello prima dell'istante chiesto");
            return MarkPriceDerivati.Esito.trovato(new BigDecimal("87594.1"));
        };
        assertEquals(0, new BigDecimal("87594.1").compareTo(MarkPriceDerivati.Mark("okx", "BTC-USDT-SWAP", false, MINUTO + 60_000L)));
        MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
            chiamate.incrementAndGet();
            return MarkPriceDerivati.Esito.nonLetto();
        };
        assertEquals(0, new BigDecimal("87594.1").compareTo(MarkPriceDerivati.Mark("OKX", "BTC-USDT-SWAP", false, MINUTO + 60_000L)),
                "dalla cache di prezzi.mv.db");
        assertEquals(1, chiamate.get());
    }

    @Test
    void unErroreSiRiprova_unContrattoInesistenteNoPerLaSessione() {
        AtomicInteger chiamate = new AtomicInteger();
        MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
            chiamate.incrementAndGet();
            return MarkPriceDerivati.Esito.nonLetto();
        };
        assertNull(MarkPriceDerivati.Mark("OKX", "BTC-USDT-SWAP", false, MINUTO + 60_000L));
        assertNull(MarkPriceDerivati.Mark("OKX", "BTC-USDT-SWAP", false, MINUTO + 60_000L));
        assertEquals(2, chiamate.get(), "dopo un errore di rete si riprova");

        MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
            chiamate.incrementAndGet();
            return MarkPriceDerivati.Esito.assente();
        };
        assertNull(MarkPriceDerivati.Mark("OKX", "BTC-USDT-250627", false, MINUTO + 60_000L));
        assertNull(MarkPriceDerivati.Mark("OKX", "BTC-USDT-250627", false, MINUTO + 60_000L));
        assertEquals(3, chiamate.get(), "un contratto che l'exchange non ha si chiede una volta sola");
    }

    @Test
    void bybitInversoEUnaCategoriaADiParte() {
        MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
            assertEquals("BYBIT", p);
            assertTrue(inverso);
            return MarkPriceDerivati.Esito.trovato(new BigDecimal("104773.21"));
        };
        assertEquals(0, new BigDecimal("104773.21").compareTo(MarkPriceDerivati.Mark("Bybit", "BTCUSDM25", true, MINUTO + 60_000L)));
    }
}
