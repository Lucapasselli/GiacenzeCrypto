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
 * risolto (con e senza punti), occupato/parziale (503), moneta non gestita (400). Il servizio
 * risponde già in EUR (conversione fatta lato server, vedi Analisi_VPS_Prezzi_Sito.md,
 * "Fase 1-ter"): qui non c'è nessuna conversione da verificare, solo il contratto HTTP.
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
        // Il servizio è disattivato di default finché l'utente non lo abilita dalle Opzioni
        // (vedi ServizioPrezziClient.OPZIONE_ABILITATO): i test continuano a esercitare la
        // logica reale sovrascrivendo l'interruttore con la proprietà di sistema.
        System.setProperty("prezzi.servizio.abilitato", "true");
        ServizioPrezziClient.ReimpostaStatoSessione_PerTest();
    }

    @AfterEach
    void fermaServerFintoEPulisceOverride() {
        server.stop(0);
        System.clearProperty("prezzi.servizio.urlbase");
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

    private static double prezzoDb(String symbol, String exchange) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "SELECT prezzo FROM PrezziNew WHERE symbol = ? AND exchange = ?")) {
            ps.setString(1, symbol);
            ps.setString(2, exchange);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "nessuna riga per " + symbol + "/" + exchange);
                return rs.getDouble(1);
            }
        }
    }

    @Test
    void esitoRisolto_conPunti_scrivePrezziGiaInEurInCacheERitornaTrue() throws Exception {
        String simbolo = "ZZZ1";
        long ts = 1718420400000L;
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200,
                "{\"esito\":\"risolto\",\"punti\":[{\"timestamp\":" + ts
                        + ",\"prices\":{\"onchain\":93.0}}]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero(simbolo, ts));
        assertEquals(1, contaRigheDbPrezzi(simbolo));
        // Il nome interno del server ("onchain") non finisce mai nella cache locale: viene
        // rinominato in ServizioPrezziClient.CODICE_FONTE prima di scrivere (vedi rinominaFonte).
        assertEquals(93.0, prezzoDb(simbolo, ServizioPrezziClient.CODICE_FONTE), 0.0001,
                "il servizio converte gia' in EUR lato server: nessuna conversione qui");
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
    void servizioDisabilitatoDiDefault_nonChiamaIlServizioSenzaCheLutenteLoAbiliti() {
        // Il default (opzione ServizioPrezziClient.OPZIONE_ABILITATO mai scritta = "NO", vedi
        // OPZIONE_ABILITATO_DEFAULT): il servizio resta muto finché l'utente non lo abilita dalle
        // Opzioni. Qui si toglie l'override che tutti gli altri test impostano in @BeforeEach,
        // per verificare esattamente il default persistito.
        System.clearProperty("prezzi.servizio.abilitato");
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}");
        });

        assertFalse(ServizioPrezziClient.tentaRecupero("ZZZ8", 1718420400000L));
        assertEquals(0, chiamate.get());
    }

    @Test
    void servizioAbilitatoEsplicitamente_chiamaIlServizio() {
        // L'utente ha messo la spunta in Opzioni: l'opzione persistita vale "SI" esplicitamente.
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.Pers_Opzioni_Scrivi(ServizioPrezziClient.OPZIONE_ABILITATO, "SI");
        AtomicInteger chiamate = new AtomicInteger(0);
        server.createContext("/v1/prezzo", ex -> {
            chiamate.incrementAndGet();
            rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}");
        });

        try {
            assertTrue(ServizioPrezziClient.tentaRecupero("ZZZ9", 1718420400000L));
            assertEquals(1, chiamate.get());
        } finally {
            // Non deve influenzare gli altri test: sono nella stessa tabella OPZIONI del DB temporaneo.
            DatabaseH2.Pers_Opzioni_Scrivi(ServizioPrezziClient.OPZIONE_ABILITATO, "NO");
        }
    }

    @Test
    void moneteEsposte_simboloAssenteDallElenco_nonChiamaMaiVPrezzo() throws Exception {
        String simbolo = "ZZZA";
        AtomicInteger chiamatePrezzo = new AtomicInteger(0);
        server.createContext("/v1/monete", ex -> rispondi(ex, 200,
                "{\"monete\":[{\"symbol\":\"BTC\",\"esposta\":true},{\"symbol\":\"" + simbolo + "\",\"esposta\":false}]}"));
        server.createContext("/v1/prezzo", ex -> {
            chiamatePrezzo.incrementAndGet();
            rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}");
        });

        assertFalse(ServizioPrezziClient.tentaRecupero(simbolo, 1718420400000L));
        assertEquals(0, chiamatePrezzo.get(), "un simbolo non esposto non deve mai raggiungere /v1/prezzo");

        // Ricordato per sessione come i simboli scoperti non gestiti via 400: un secondo tentativo
        // non deve nemmeno ripassare da /v1/monete (già in cache), tantomeno da /v1/prezzo.
        assertFalse(ServizioPrezziClient.tentaRecupero(simbolo, 1718420460000L));
        assertEquals(0, chiamatePrezzo.get());
    }

    @Test
    void moneteEsposte_simboloPresenteEEsposto_procedeVersoVPrezzo() throws Exception {
        String simbolo = "ZZZB";
        long ts = 1718420400000L;
        server.createContext("/v1/monete", ex -> rispondi(ex, 200,
                "{\"monete\":[{\"symbol\":\"" + simbolo + "\",\"esposta\":true}]}"));
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200,
                "{\"esito\":\"risolto\",\"punti\":[{\"timestamp\":" + ts + ",\"prices\":{\"onchain\":42.0}}]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero(simbolo, ts));
        assertEquals(42.0, prezzoDb(simbolo, ServizioPrezziClient.CODICE_FONTE), 0.0001);
    }

    @Test
    void moneteEsposte_endpointIrraggiungibile_ripiegaSullaScopertaReattivaViaPrezzo() throws Exception {
        // Nessun contesto per /v1/monete: il server finto risponde 404 di suo (nessun handler
        // registrato) — trattato come "non so", non come "nessuna moneta esposta".
        String simbolo = "ZZZC";
        long ts = 1718420400000L;
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200,
                "{\"esito\":\"risolto\",\"punti\":[{\"timestamp\":" + ts + ",\"prices\":{\"onchain\":7.0}}]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero(simbolo, ts));
        assertEquals(7.0, prezzoDb(simbolo, ServizioPrezziClient.CODICE_FONTE), 0.0001);
    }

    @Test
    void moneteEsposte_interrogatoUnaSolaVoltaPerSessione() throws Exception {
        AtomicInteger chiamateMonete = new AtomicInteger(0);
        server.createContext("/v1/monete", ex -> {
            chiamateMonete.incrementAndGet();
            rispondi(ex, 200, "{\"monete\":[{\"symbol\":\"ZZZD\",\"esposta\":true},{\"symbol\":\"ZZZE\",\"esposta\":true}]}");
        });
        server.createContext("/v1/prezzo", ex -> rispondi(ex, 200, "{\"esito\":\"risolto\",\"punti\":[]}"));

        assertTrue(ServizioPrezziClient.tentaRecupero("ZZZD", 1718420400000L));
        assertTrue(ServizioPrezziClient.tentaRecupero("ZZZE", 1718420400000L));

        assertEquals(1, chiamateMonete.get(), "la lista delle monete esposte va richiesta una sola volta per sessione");
    }
}
