package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifica il contratto lato client: forma della richiesta ({@code errore-import} JSON,
 * {@code log} con corpo grezzo + header {@code X-Segnalazione-Meta} base64), presenza dei campi
 * comuni (versione/OS/Java), descrittore serializzato, e traduzione degli esiti.
 */
class SegnalazioniClientTest {

    private HttpServer server;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> percorso = new AtomicReference<>();
    private final AtomicReference<String> corpoRicevuto = new AtomicReference<>();
    private final AtomicReference<byte[]> corpoBinario = new AtomicReference<>();
    private final AtomicReference<String> headerMeta = new AtomicReference<>();
    private volatile int statoRisposta = 201;
    private volatile String corpoRisposta = "{\"esito\":\"ok\",\"id\":1}";

    @BeforeEach
    void avvia() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", (HttpExchange ex) -> {
            metodo.set(ex.getRequestMethod());
            percorso.set(ex.getRequestURI().getPath());
            headerMeta.set(ex.getRequestHeaders().getFirst("X-Segnalazione-Meta"));
            byte[] b = ex.getRequestBody().readAllBytes();
            corpoBinario.set(b);
            corpoRicevuto.set(new String(b, StandardCharsets.UTF_8));
            byte[] risp = corpoRisposta.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(statoRisposta, risp.length);
            ex.getResponseBody().write(risp);
            ex.close();
        });
        server.start();
        System.setProperty("segnalazioni.servizio.urlbase",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void ferma() {
        server.stop(0);
        System.clearProperty("segnalazioni.servizio.urlbase");
    }

    @Test
    void inviaErroreImport_formaRichiestaEDescrittore() {
        SegnalazioniClient.DescrittoreImport d =
                new SegnalazioniClient.DescrittoreImport("OKX CSV (formato storico)", "okx.csv", 10, 5, 3, 2);

        SegnalazioniClient.Esito e = SegnalazioniClient.inviaErroreImport("MOV SCONOSCIUTO xyz", d);

        assertEquals("POST", metodo.get());
        assertEquals("/segnalazioni/errore-import", percorso.get());
        JsonObject body = JsonParser.parseString(corpoRicevuto.get()).getAsJsonObject();
        assertEquals("MOV SCONOSCIUTO xyz", body.get("corpo").getAsString());
        assertNotNull(body.get("versioneApp"));
        assertTrue(body.has("os") && body.has("java"));
        JsonObject desc = body.getAsJsonObject("descrittore");
        assertEquals("OKX CSV (formato storico)", desc.get("tipoImport").getAsString());
        assertEquals("okx.csv", desc.get("file").getAsString());
        assertEquals(2, desc.getAsJsonObject("conteggi").get("sconosciute").getAsInt());

        assertTrue(e.ok());
        assertNotNull(e.messaggio());
    }

    @Test
    void inviaLog_corpoGrezzoEHeaderMetaBase64() throws IOException {
        byte[] gz = gzip("===== GiacenzeCrypto.log =====\nqualcosa\n");

        SegnalazioniClient.Esito e = SegnalazioniClient.inviaLog(gz, "bundle", null);

        assertEquals("/segnalazioni/log", percorso.get());
        assertEquals(gz.length, corpoBinario.get().length);
        assertNotNull(headerMeta.get(), "l'header X-Segnalazione-Meta deve esserci");
        String metaJson = new String(Base64.getDecoder().decode(headerMeta.get()), StandardCharsets.UTF_8);
        JsonObject meta = JsonParser.parseString(metaJson).getAsJsonObject();
        assertEquals("bundle", meta.get("modalita").getAsString());
        assertNotNull(meta.get("versioneApp"));
        assertTrue(e.ok());
    }

    @Test
    void esitoNonRiuscito_messaggioAmichevoleNonEccezione() {
        statoRisposta = 429;
        corpoRisposta = "{\"esito\":\"quota-giornaliera\"}";

        SegnalazioniClient.Esito e = SegnalazioniClient.inviaErroreImport("x", null);

        assertFalse(e.ok());
        assertEquals(429, e.codice());
        assertTrue(e.messaggio().toLowerCase().contains("giornalier"));
    }

    private static byte[] gzip(String s) throws IOException {
        var bos = new java.io.ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bos)) {
            g.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }
}
