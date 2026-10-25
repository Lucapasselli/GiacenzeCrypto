package com.giacenzecrypto.giacenze_crypto;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le configurazioni condivise con la community, dal lato del programma: i controlli di sicurezza
 * ({@link ValidazioneConfigCommunity}, gli stessi del server), il client ({@link CommunityConfigClient}) contro un
 * server HTTP locale, il salvataggio di un file scaricato e la sua comparsa nelle tendine della finestra Importazioni.
 */
class CommunityConfigTest {

    @TempDir
    static Path tempDir;
    private static HttpServer server;
    private static String workdirPrima;
    /** L'ultima richiesta POST ricevuta dal server finto */
    private static volatile String ultimoCorpo = "";

    private static final String BUONA = new JSONObject()
            .put("nomeExchange", "Kraken").put("estrazione", "Ledger").put("descrizione", "Export Ledger di Kraken.")
            .put("formatoData", "yyyy-MM-dd HH:mm:ss")
            .put("colonne", new JSONObject().put("data", 1).put("causale", 3).put("moneta", 4).put("quantita", 5))
            .put("mappaCausali", new JSONObject().put("deposit", "TRASFERIMENTO-CRYPTO").put("staking", "STAKING REWARDS"))
            .put("causaliChiuse", new org.json.JSONArray().put("TRASFERIMENTO-CRYPTO").put("STAKING REWARDS"))
            .put("_nota", "versioni < 1.0.66: niente $schema")
            .toString(2);

    private static String conModifica(Consumer<JSONObject> f) {
        JSONObject o = new JSONObject(BUONA);
        f.accept(o);
        return o.toString();
    }

    @BeforeAll
    static void avvia() throws Exception {
        workdirPrima = VarStatiche.getWorkingDirectory();
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/community/proposte", s -> {
            ultimoCorpo = new String(s.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean ritiro = s.getRequestURI().getPath().endsWith("/ritira");
            String risposta;
            int codice;
            if (ritiro) {
                codice = ultimoCorpo.contains("giusto") ? 200 : 404;
                risposta = codice == 200 ? "{\"esito\":\"ok\"}" : "{\"esito\":\"non-ritirabile\"}";
            } else if (ultimoCorpo.contains("separatore")) {
                codice = 422;
                risposta = "{\"esito\":\"non-valida\",\"errori\":[\"separatore non ammesso\"]}";
            } else {
                codice = 201;
                risposta = "{\"esito\":\"ok\",\"id\":7,\"codiceRitiro\":\"abc123\"}";
            }
            byte[] b = risposta.getBytes(StandardCharsets.UTF_8);
            s.sendResponseHeaders(codice, b.length);
            s.getResponseBody().write(b);
            s.close();
        });
        server.createContext("/community/catalogo", s -> {
            String sha = CommunityConfigurazioni.Sha256(BUONA.getBytes(StandardCharsets.UTF_8));
            byte[] b = ("{\"formato\":1,\"configurazioni\":[{\"id\":7,\"nomeFile\":\"kraken.json\",\"fornitore\":\"\","
                    + "\"estrazione\":\"Ledger\",\"nomeExchange\":\"Kraken\",\"descrizione\":\"Export Ledger di Kraken.\","
                    + "\"versioneMinimaApp\":\"\",\"autore\":\"Mario\",\"pubblicata\":1761400000000,\"sha256\":\"" + sha + "\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
            s.sendResponseHeaders(200, b.length);
            s.getResponseBody().write(b);
            s.close();
        });
        server.createContext("/community/configurazioni/", s -> {
            byte[] b = BUONA.getBytes(StandardCharsets.UTF_8);
            s.sendResponseHeaders(200, b.length);
            s.getResponseBody().write(b);
            s.close();
        });
        server.start();
        System.setProperty("community.servizio.urlbase", "http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterAll
    static void ferma() {
        server.stop(0);
        System.clearProperty("community.servizio.urlbase");
        VarStatiche.setWorkingDirectory(workdirPrima);
    }

    // ---------------------------------------------------------------- controlli

    @Test
    void unaConfigurazioneCorrettaPassa() {
        assertEquals(List.of(), ValidazioneConfigCommunity.Controlla(BUONA));
    }

    @Test
    void leConfigurazioniDistribuitePassanoSalvoLeRegoleDellaCommunity() throws Exception {
        //Chi parte da una configurazione ufficiale non deve trovarsi il file rifiutato per un falso positivo
        List<String> errori = new ArrayList<>();
        for (File f : new File("config/import").listFiles((d, n) -> n.endsWith(".json"))) {
            for (String e : ValidazioneConfigCommunity.Controlla(Files.readString(f.toPath(), StandardCharsets.UTF_8))) {
                if (!e.startsWith("centralizzato") && !e.startsWith("descrizione: il carattere")
                        && !e.startsWith("descrizione: testo oltre")) {
                    errori.add(f.getName() + ": " + e);
                }
            }
        }
        assertEquals(List.of(), errori);
    }

    @Test
    void rifiutaIFilePericolosiOSbagliati() {
        Map<String, String> casi = new LinkedHashMap<>();
        casi.put("JSON rotto", "{\"nomeExchange\": ");
        casi.put("troppo grande", new JSONObject().put("descrizione", "x".repeat(70 * 1024)).toString());
        casi.put("separatore regex", conModifica(o -> o.put("separatore", "(a+)+$")));
        casi.put("html nella descrizione", conModifica(o -> o.put("descrizione", "<html><img src='http://x/t.png'></html>")));
        casi.put("html in una causale", conModifica(o -> o.getJSONObject("mappaCausali").put("<html>x", "REWARD")));
        casi.put("centralizzato", conModifica(o -> o.put("centralizzato", true)));
        casi.put("chiave inventata", conModifica(o -> o.put("mappaColonne", new JSONObject())));
        casi.put("tipo in minuscolo", conModifica(o -> o.getJSONObject("mappaCausali").put("staking", "reward")));
        casi.put("indirizzo EVM", conModifica(o -> o.put("nomeWallet", "0x" + "ab".repeat(20))));
        casi.put("email", conModifica(o -> o.put("descrizione", "scrivetemi a mario.rossi@example.com")));
        casi.put("IBAN", conModifica(o -> o.put("descrizione", "IT60 X054 2811 1010 0000 0123 456")));
        casi.put("annidamento", "{\"_a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":1}}}}}}}}");
        casi.put("carattere di controllo", conModifica(o -> o.put("nomeWallet", "a\u0007b")));
        casi.put("regola incoerente", conModifica(o -> o.put("regoleRiga", new org.json.JSONArray()
                .put(new JSONObject().put("colonna", 1).put("uguale", "a").put("contiene", "b").put("scarta", true)))));
        for (Map.Entry<String, String> c : casi.entrySet()) {
            assertFalse(ValidazioneConfigCommunity.Controlla(c.getValue()).isEmpty(), c.getKey());
        }
    }

    @Test
    void separatoriAmmessi() {
        for (String sep : List.of(",", ";", "\t", "|", "\\|")) {
            assertEquals(List.of(), ValidazioneConfigCommunity.Controlla(conModifica(o -> o.put("separatore", sep))), sep);
        }
    }

    // ---------------------------------------------------------------- client e salvataggio

    @Test
    void inviaERicordaIlCodiceDiRitiro() throws Exception {
        CommunityConfigClient.EsitoInvio ok = CommunityConfigClient.Invia(BUONA, "kraken.json", "Mario", null);
        assertTrue(ok.ok(), ok.messaggio());
        assertEquals(7, ok.id());
        assertEquals("abc123", ok.codiceRitiro());
        JSONObject inviato = new JSONObject(ultimoCorpo);
        assertEquals(BUONA, inviato.getString("configurazione"), "il JSON parte cosi' com'e'");
        assertEquals(VarStatiche.Versione, inviato.getString("versioneApp"));

        CommunityConfigClient.EsitoInvio ko = CommunityConfigClient.Invia(conModifica(o -> o.put("separatore", "x")), "x.json", "", null);
        assertFalse(ko.ok());
        assertEquals(List.of("separatore non ammesso"), ko.errori());

        CommunityConfigurazioni.RegistraProposta(7, "kraken.json", "abc123");
        assertEquals(7, CommunityConfigurazioni.ProposteInviate().get(0).id());
        assertFalse(CommunityConfigClient.Ritira(7, "sbagliato"));
        assertTrue(CommunityConfigClient.Ritira(7, "giusto"));
        CommunityConfigurazioni.TogliProposta(7);
        assertTrue(CommunityConfigurazioni.ProposteInviate().isEmpty());
    }

    @Test
    void scaricaControllaESalvaEPoiCompareNelleTendine() throws Exception {
        List<CommunityConfigClient.VoceCatalogo> catalogo = CommunityConfigClient.Catalogo();
        assertEquals(1, catalogo.size());
        CommunityConfigClient.VoceCatalogo v = catalogo.get(0);
        assertEquals("Mario", v.autore());

        //Byte diversi da quelli del catalogo: non si salva nulla
        Exception sbagliato = assertThrows(Exception.class,
                () -> CommunityConfigurazioni.SalvaScaricata(v, (BUONA + " ").getBytes(StandardCharsets.UTF_8)));
        assertTrue(sbagliato.getMessage().contains("SHA-256"), sbagliato.getMessage());
        assertFalse(CommunityConfigurazioni.FileLocale(7).exists());

        File f = CommunityConfigurazioni.SalvaScaricata(v, CommunityConfigClient.Scarica(7));
        assertEquals("community-7.json", f.getName());
        assertTrue(CommunityConfigurazioni.Aggiornata(v));

        Importazioni_Gestione.VoceImport voce = Importazioni_Gestione.RaccogliEstrazioni().get("Kraken").get(0);
        assertTrue(voce.community);
        assertFalse(voce.personalizzata);
        assertEquals("Community: Ledger", voce.toString());
        assertTrue(voce.tooltip().contains("Configurazione della community"), voce.tooltip());

        assertTrue(CommunityConfigurazioni.Elimina(7));
        assertFalse(Importazioni_Gestione.RaccogliEstrazioni().containsKey("Kraken"));
    }

    @Test
    void unFileSalvatoMaPericolosoNonSiAccettaNemmenoConLImprontaGiusta() {
        String cattivo = conModifica(o -> o.put("separatore", "(a+)+$"));
        byte[] b = cattivo.getBytes(StandardCharsets.UTF_8);
        CommunityConfigClient.VoceCatalogo v = new CommunityConfigClient.VoceCatalogo(9, "x.json", "", "x", "X", "", "", "",
                0, CommunityConfigurazioni.Sha256(b));
        Exception e = assertThrows(Exception.class, () -> CommunityConfigurazioni.SalvaScaricata(v, b));
        assertTrue(e.getMessage().contains("separatore"), e.getMessage());
        assertFalse(CommunityConfigurazioni.FileLocale(9).exists());
    }

    @Test
    void unTestoDiAltriNonDiventaHtmlInUnaCella() {
        assertEquals(" <html><b>x</b>", GUI_ConfigurazioniCommunity.Piano("<html><b>x</b>"));
        assertEquals("Kraken", GUI_ConfigurazioniCommunity.Piano("Kraken"));
    }
}
