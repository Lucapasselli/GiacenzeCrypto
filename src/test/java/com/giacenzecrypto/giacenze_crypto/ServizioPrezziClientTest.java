package com.giacenzecrypto.giacenze_crypto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Caratterizza {@link ServizioPrezziClient#tentaRecupero(String, long)} contro un server HTTP
 * finto (locale, {@code com.sun.net.httpserver}, nessuna nuova dipendenza di test) che simula i
 * quattro esiti del contratto documentato in nocommit/Documentazione/API_ServizioPrezzi.md:
 * risolto (con e senza punti), occupato/parziale (503), moneta non gestita (400). Copre anche
 * l'interruttore di sessione e il fatto che senza una chiave configurata non parte nessuna
 * chiamata di rete.
 */
class ServizioPrezziClientTest {

    @TempDir
    static Path tempDir;

    private HttpServer server;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void avviaServerFintoEImpostaOverride() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        System.setProperty("prezzi.servizio.urlbase", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        System.setProperty("prezzi.servizio.apikey", "chiave-di-test");
        // Il servizio è disabilitato di default dal 2026-08-25 (vedi ServizioPrezziClient) in
        // attesa di chiarire i vincoli contrattuali degli exchange rimanenti: i test continuano a
        // esercitare la logica reale sovrascrivendo l'interruttore.
        System.setProperty("prezzi.servizio.abilitato", "true");
        ServizioPrezziClient.ReimpostaStatoSessione_PerTest();
    }

    @AfterEach
    void fermaServerFintoEPulisceOverride() {
        server.stop(0);
        System.clearProperty("prezzi.servizio.urlbase");
        System.clearProperty("prezzi.servizio.apikey");
        System.clearProperty("prezzi.servizio.abilitato");
    }

    private static void rispondi(HttpExchange ex, int status, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static int contaRigheDbPrezzi(String symbol) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "SELECT COUNT(*) FROM PrezziNew WHERE symbol = ?")) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    void esitoRisolto_conPunti_scrivePrezziInCacheERitornaTrue() throws Exception {
        String simbolo = "ZZZ1";
        long ts = 1718420400000L;
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200,
                "{\"esito\":\"risolto\",\"punti\":[{\"timestamp\":" + ts
                        + ",\"prices\":{\"binance\":1.23,\"coinbase\":1.24}}]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero(simbolo, ts));
        assertEquals(2, contaRigheDbPrezzi(simbolo), "un punto con due exchange -> due righe");
    }

    @Test
    void esitoRisolto_puntiVuoti_ritornaTrueSenzaScrivereNulla() throws Exception {
        String simbolo = "ZZZ2";
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero(simbolo, 1718420400000L),
                "punti:[] e' un'assenza di prezzo confermata, non un errore");
        assertEquals(0, contaRigheDbPrezzi(simbolo));
    }

    @Test
    void esito503_ritornaFalseSenzaScrivereNulla() throws Exception {
        String simbolo = "ZZZ3";
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 503, "{\"esito\":\"occupato\"}"));

        assertFalse(ServizioPrezziClient.tentaRecupero(simbolo, 1718420400000L));
        assertEquals(0, contaRigheDbPrezzi(simbolo));
    }

    @Test
    void esito400_ricordaIlSimboloPerSessioneENonRichiedePiu() throws Exception {
        String simbolo = "ZZZ4";
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 400, "{\"errore\":\"moneta \\\"" + simbolo + "\\\" non gestita da questo servizio\"}");
        });

        assertFalse(ServizioPrezziClient.tentaRecupero(simbolo, 1718420400000L));
        assertFalse(ServizioPrezziClient.tentaRecupero(simbolo, 1718420460000L));

        assertEquals(1, chiamate.get(), "il secondo tentativo per lo stesso simbolo non deve toccare la rete");
    }

    @Test
    void fallimentiConsecutivi_apronoInterruttoreDiSessione() throws Exception {
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 503, "{\"esito\":\"occupato\"}");
        });

        for (int i = 0; i < 5; i++) {
            assertFalse(ServizioPrezziClient.tentaRecupero("ZZZ5", 1718420400000L + i));
        }
        int chiamateDopoSoglia = chiamate.get();
        assertEquals(5, chiamateDopoSoglia);

        // interruttore aperto: la richiesta successiva (anche per un altro simbolo) non deve
        // più raggiungere il server per il resto della sessione.
        assertFalse(ServizioPrezziClient.tentaRecupero("ZZZ6", 1718420400000L));
        assertEquals(chiamateDopoSoglia, chiamate.get());
    }

    @Test
    void nessunaChiaveConfigurata_ritornaFalseSenzaChiamareIlServizio() {
        // Proprietà impostata ma vuota = "nessuna chiave", a prescindere da un eventuale
        // src/main/resources/ServizioPrezzi_ApiKey.txt reale già creato in locale da chi esegue
        // il test (System.clearProperty ricadrebbe su quel file, se presente, rendendo il test
        // dipendente dalla macchina su cui gira — vedi apiKey() in ServizioPrezziClient).
        System.setProperty("prezzi.servizio.apikey", "");
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}");
        });

        assertFalse(ServizioPrezziClient.tentaRecupero("ZZZ7", 1718420400000L));
        assertEquals(0, chiamate.get());
    }

    @Test
    void servizioDisabilitato_ritornaFalseSenzaChiamareIlServizio() {
        // Il default di produzione (2026-08-25): nessuna chiamata al servizio remoto finché
        // l'interruttore non viene riattivato a mano. Qui si toglie l'override che tutti gli
        // altri test impostano in @BeforeEach, per verificare esattamente il default.
        System.clearProperty("prezzi.servizio.abilitato");
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}");
        });

        assertFalse(ServizioPrezziClient.tentaRecupero("ZZZ8", 1718420400000L));
        assertEquals(0, chiamate.get());
    }
}
