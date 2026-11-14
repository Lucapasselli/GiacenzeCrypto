package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Esito;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Gravita;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Impostazioni;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Richiesta;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Risposta;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Stato;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Verdetto;
import com.giacenzecrypto.giacenze_crypto.DiagnosiServizi.Voce;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link DiagnosiServizi}: classificazione degli esiti, controlli sul contenuto, diagnosi, riservatezza delle chiavi e la sonda
 * HTTP vera contro un server su loopback. Nessun test va in rete (il {@code GuardiaRete} fallirebbe).
 */
class DiagnosiServiziTest {

    private static HttpServer server;
    private static int porta;

    @BeforeAll
    static void avviaServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ok", ex -> rispondi(ex, 200, "{\"coins\":{}}"));
        server.createContext("/limite", ex -> rispondi(ex, 429, "troppe richieste"));
        server.createContext("/guasto", ex -> rispondi(ex, 503, "giu"));
        server.createContext("/vietato", ex -> rispondi(ex, 403, "no"));
        server.createContext("/rpc", ex -> {
            byte[] letto = ex.getRequestBody().readAllBytes();
            rispondi(ex, 200, new String(letto, StandardCharsets.UTF_8).contains("eth_blockNumber") ? "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x10\"}" : "{}");
        });
        server.createContext("/lento", ex -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignorata) {
                Thread.currentThread().interrupt();
            }
            rispondi(ex, 200, "ok");
        });
        server.start();
        porta = server.getAddress().getPort();
    }

    @AfterAll
    static void fermaServer() {
        server.stop(0);
    }

    private static void rispondi(com.sun.net.httpserver.HttpExchange ex, int codice, String corpo) throws IOException {
        byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(codice, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    private static String url(String percorso) {
        return "http://127.0.0.1:" + porta + percorso;
    }

    private static Risposta risposta(int codice, String corpo, long totaleMs) {
        return new Risposta(codice, corpo, 10, 2, 2, 20, totaleMs, System.currentTimeMillis());
    }

    private static Voce voce(Gravita g) {
        return new Voce("Prezzi e cambi", "Prova", Richiesta.get("https://esempio.invalid/x"), g);
    }

    // ---------------------------------------------------------------- classificazione

    @Test
    void unaRispostaBuonaEVelocePassaConTuttiITempi() {
        Esito e = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), risposta(200, "x", 300), null);
        assertEquals(Stato.OK, e.stato());
        assertEquals(300, e.ms());
        assertTrue(e.dettaglio().contains("DNS 10 ms (2 IPv4, 2 IPv6)"), e.dettaglio());
        assertTrue(e.dettaglio().contains("connessione 20 ms"), e.dettaglio());
    }

    @Test
    void unaRispostaOltreLaSogliaELenta() {
        Esito e = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), risposta(200, "x", DiagnosiServizi.SOGLIA_LENTO_MS + 1), null);
        assertEquals(Stato.LENTO, e.stato());
    }

    @Test
    void ilGuastoDiUnServizioSecondarioEUnAttenzioneNonUnProblema() {
        Esito essenziale = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), risposta(503, "", 100), null);
        Esito secondario = DiagnosiServizi.Classifica(voce(Gravita.SECONDARIO), risposta(503, "", 100), null);
        Esito spento = DiagnosiServizi.Classifica(voce(Gravita.SPENTO), risposta(503, "", 100), null);
        assertEquals(Stato.ERRORE, essenziale.stato());
        assertEquals(Stato.AVVISO, secondario.stato());
        assertEquals(Stato.INFO, spento.stato());
    }

    @Test
    void ilLimiteDiRichiesteEUnAttenzioneAncheSuUnServizioEssenziale() {
        Esito e = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), risposta(429, "", 100), null);
        assertEquals(Stato.AVVISO, e.stato());
        assertTrue(e.dettaglio().contains("limite di richieste"), e.dettaglio());
        Esito geo = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), risposta(451, "", 100), null);
        assertEquals(Stato.AVVISO, geo.stato());
        assertTrue(geo.dettaglio().contains("area geografica"), geo.dettaglio());
    }

    @Test
    void unaSondaDiSolaRaggiungibilitaAccettaUnErroreDelClient() {
        Voce v = voce(Gravita.SECONDARIO).soloRaggiungibilita();
        assertEquals(Stato.OK, DiagnosiServizi.Classifica(v, risposta(403, "", 100), null).stato());
        assertEquals(Stato.OK, DiagnosiServizi.Classifica(v, risposta(404, "", 100), null).stato());
        assertEquals(Stato.AVVISO, DiagnosiServizi.Classifica(v, risposta(500, "", 100), null).stato());
        assertEquals(Stato.AVVISO, DiagnosiServizi.Classifica(voce(Gravita.SECONDARIO), risposta(403, "", 100), null).stato());
    }

    @Test
    void ilContenutoInattesoSuUnaRispostaRiuscitaEUnProblema() {
        Voce v = voce(Gravita.ESSENZIALE).conControllo(r -> Verdetto.problema("manca il campo"));
        Esito e = DiagnosiServizi.Classifica(v, risposta(200, "{}", 100), null);
        assertEquals(Stato.ERRORE, e.stato());
        assertTrue(e.dettaglio().contains("manca il campo"));
    }

    @Test
    void unErroreDiReteDiceInQualeFase() {
        Esito dns = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), null,
                new DiagnosiServizi.ErroreRete("DNS", "nome non risolto: esempio.invalid", 12));
        assertEquals(Stato.ERRORE, dns.stato());
        assertTrue(dns.dettaglio().contains("(DNS)"), dns.dettaglio());
        assertEquals(12, dns.ms());
    }

    // ---------------------------------------------------------------- riservatezza delle chiavi

    @Test
    void nessunaChiaveArrivaAlTestoDegliEsiti() {
        assertEquals("GET https://x/y?apikey=*** fine", DiagnosiServizi.senzaChiavi("GET https://x/y?apikey=SEGRETO123 fine"));
        assertEquals("https://x?a=1&api_key=***&b=2", DiagnosiServizi.senzaChiavi("https://x?a=1&api_key=SEGRETO&b=2"));
        Esito e = DiagnosiServizi.Classifica(voce(Gravita.ESSENZIALE), null,
                new IOException("Server returned HTTP response code: 500 for URL: https://api.etherscan.io/v2/api?apikey=SEGRETO999"));
        assertFalse(e.dettaglio().contains("SEGRETO999"), e.dettaglio());
    }

    @Test
    void leImpostazioniNonStampanoLaChiave() {
        String t = new Impostazioni("SEGRETO123", true, false).toString();
        assertFalse(t.contains("SEGRETO123"));
        assertTrue(t.contains("chiaveEtherscan=si"));
    }

    @Test
    void ilRapportoNonContieneLaChiaveNemmenoSeUnDettaglioLaPortava() {
        List<Esito> esiti = List.of(new Esito(DiagnosiServizi.CAT_BLOCKCHAIN, "Etherscan", Stato.ERRORE, 5,
                "errore su https://api.etherscan.io/v2/api?chainid=1&apikey=SEGRETO555"));
        assertFalse(DiagnosiServizi.Rapporto(esiti, "oggi").contains("SEGRETO555"));
    }

    // ---------------------------------------------------------------- controlli sul contenuto

    @Test
    void ilControlloDellOrologioSegnalaUnoSfasamentoOltreLaSoglia() {
        long adesso = System.currentTimeMillis();
        Risposta preciso = new Risposta(200, "{\"serverTime\":" + (adesso - 300) + "}", 1, 1, 0, 1, 50, adesso);
        Verdetto ok = DiagnosiServizi.ControllaOrologioBinance(preciso);
        assertTrue(ok.problema().isEmpty(), ok.problema());
        assertTrue(ok.nota().contains("avanti di 300 ms"), ok.nota());
        Risposta indietro = new Risposta(200, "{\"serverTime\":" + (adesso + 9000) + "}", 1, 1, 0, 1, 50, adesso);
        Verdetto ko = DiagnosiServizi.ControllaOrologioBinance(indietro);
        assertFalse(ko.problema().isEmpty());
        assertTrue(ko.problema().contains("indietro di 9000 ms"), ko.problema());
        assertFalse(DiagnosiServizi.ControllaOrologioBinance(risposta(200, "{}", 1)).problema().isEmpty());
    }

    @Test
    void ilControlloDiUnNodoRifiutaZeroEdErrori() {
        assertTrue(DiagnosiServizi.ControllaNodoRpc(risposta(200, "{\"result\":\"0x10\"}", 1)).problema().isEmpty());
        assertEquals("blocco 16", DiagnosiServizi.ControllaNodoRpc(risposta(200, "{\"result\":\"0x10\"}", 1)).nota());
        assertTrue(DiagnosiServizi.ControllaNodoRpc(risposta(200, "{\"result\":\"0x0\"}", 1)).problema().contains("0x0"));
        assertTrue(DiagnosiServizi.ControllaNodoRpc(risposta(200, "{\"error\":{\"message\":\"rate limited\"}}", 1)).problema().contains("rate limited"));
        assertFalse(DiagnosiServizi.ControllaNodoRpc(risposta(200, "non e' json", 1)).problema().isEmpty());
    }

    @Test
    void ilLimiteDiGitHubSiLeggeComeAttenzioneNonComeIrraggiungibile() {
        String ok = "{\"resources\":{\"core\":{\"limit\":60,\"remaining\":42,\"reset\":1}}}";
        Verdetto v = DiagnosiServizi.ControllaGitHub(risposta(200, ok, 1));
        assertTrue(v.problema().isEmpty());
        assertTrue(v.nota().contains("42 richieste rimaste su 60"), v.nota());
        String esaurito = "{\"resources\":{\"core\":{\"limit\":60,\"remaining\":0,\"reset\":1700000000}}}";
        assertTrue(DiagnosiServizi.ControllaGitHub(risposta(200, esaurito, 1)).problema().contains("limite di richieste raggiunto"));
    }

    @Test
    void etherscanConChiaveRifiutataEUnProblemaDiChiave() {
        assertTrue(DiagnosiServizi.ControllaEtherscan(risposta(200, "{\"jsonrpc\":\"2.0\",\"result\":\"0x1\"}", 1)).problema().isEmpty());
        assertTrue(DiagnosiServizi.ControllaEtherscan(risposta(200, "{\"status\":\"0\",\"result\":\"Invalid API Key\"}", 1)).problema().contains("chiave"));
    }

    // ---------------------------------------------------------------- la sonda vera, contro il server locale

    @Test
    void laSondaMisuraUnaRispostaVera() throws IOException {
        Risposta r = new DiagnosiServizi.SondaHttp().esegui(Richiesta.get(url("/ok")), 2000, 2000);
        assertEquals(200, r.codice());
        assertTrue(r.corpo().contains("coins"));
        assertTrue(r.totaleMs() >= 0);
        assertTrue(r.indirizzi4() + r.indirizzi6() >= 1);
    }

    @Test
    void laSondaPassaIlCorpoDiUnaRichiestaPost() throws IOException {
        Risposta r = new DiagnosiServizi.SondaHttp().esegui(Richiesta.rpc(url("/rpc"), "eth_blockNumber"), 2000, 2000);
        assertEquals(200, r.codice());
        assertTrue(DiagnosiServizi.ControllaNodoRpc(r).problema().isEmpty());
    }

    @Test
    void esegueELeggeICodiciDErrore() {
        DiagnosiServizi.Sonda sonda = new DiagnosiServizi.SondaHttp();
        assertEquals(Stato.OK, DiagnosiServizi.Esegui(new Voce("c", "s", Richiesta.get(url("/ok")), Gravita.ESSENZIALE), sonda).stato());
        assertEquals(Stato.AVVISO, DiagnosiServizi.Esegui(new Voce("c", "s", Richiesta.get(url("/limite")), Gravita.ESSENZIALE), sonda).stato());
        assertEquals(Stato.ERRORE, DiagnosiServizi.Esegui(new Voce("c", "s", Richiesta.get(url("/guasto")), Gravita.ESSENZIALE), sonda).stato());
        assertEquals(Stato.AVVISO, DiagnosiServizi.Esegui(new Voce("c", "s", Richiesta.get(url("/vietato")), Gravita.SECONDARIO), sonda).stato());
    }

    @Test
    void unaRispostaLentaOltreIlTimeoutDiLetturaDiceInQualeFase() {
        Voce v = new Voce("c", "s", Richiesta.get(url("/lento")), Gravita.ESSENZIALE);
        Esito e;
        try {
            Risposta r = new DiagnosiServizi.SondaHttp().esegui(v.richiesta(), 2000, 300);
            e = DiagnosiServizi.Classifica(v, r, null);
        } catch (IOException ex) {
            e = DiagnosiServizi.Classifica(v, null, ex);
        }
        assertEquals(Stato.ERRORE, e.stato());
        assertTrue(e.dettaglio().contains("(risposta)"), e.dettaglio());
        assertTrue(e.dettaglio().contains("nessuna risposta"), e.dettaglio());
    }

    @Test
    void unaPortaChiusaDiceCheLaConnessioneFallisce() throws IOException {
        int chiusa;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            chiusa = s.getLocalPort();
        }
        Voce v = new Voce("c", "s", Richiesta.get("http://127.0.0.1:" + chiusa + "/x"), Gravita.ESSENZIALE);
        Esito e = DiagnosiServizi.Esegui(v, new DiagnosiServizi.SondaHttp());
        assertEquals(Stato.ERRORE, e.stato());
        assertTrue(e.dettaglio().contains("(connessione)"), e.dettaglio());
    }

    // ---------------------------------------------------------------- l'elenco

    @Test
    void lElencoHaOgniCategoriaEInomiDiversi() {
        List<Voce> voci = DiagnosiServizi.Elenco(new Impostazioni("", false, false));
        assertFalse(voci.isEmpty());
        List<String> chiavi = new ArrayList<>();
        for (Voce v : voci) {
            assertTrue(DiagnosiServizi.CATEGORIE.contains(v.categoria()), v.categoria());
            assertTrue(v.richiesta().url().startsWith("https://"), v.servizio() + " " + v.richiesta().url());
            chiavi.add(v.categoria() + "|" + v.servizio());
        }
        assertEquals(chiavi.size(), chiavi.stream().distinct().count(), "voci doppie");
        assertTrue(voci.stream().anyMatch(v -> v.categoria().equals(DiagnosiServizi.CAT_EXCHANGE_JAVA) && v.servizio().equals("okx")));
        assertTrue(voci.stream().anyMatch(v -> v.categoria().equals(DiagnosiServizi.CAT_NODI)), "nodi pubblici di ETH e BASE");
    }

    @Test
    void laChiaveEtherscanEntraNellaRichiestaSoloSeImpostata() {
        List<Voce> senza = DiagnosiServizi.Elenco(new Impostazioni("", false, false));
        assertTrue(senza.stream().noneMatch(v -> v.richiesta().url().contains("apikey")));
        assertTrue(senza.stream().anyMatch(v -> v.servizio().equals("Etherscan (senza chiave)")));
        List<Voce> con = DiagnosiServizi.Elenco(new Impostazioni("ABC", false, false));
        assertTrue(con.stream().anyMatch(v -> v.servizio().equals("Etherscan (con la tua chiave)") && v.richiesta().url().endsWith("apikey=ABC")));
        //Il nome della voce non contiene mai la chiave: e' quello che finisce nella tabella e nel rapporto.
        assertTrue(con.stream().noneMatch(v -> v.servizio().contains("ABC")));
    }

    @Test
    void tuttiINodiAggiungonoLeAltreReti() {
        int predefiniti = DiagnosiServizi.NodiDaVerificare(false).size();
        int tutti = DiagnosiServizi.NodiDaVerificare(true).size();
        assertTrue(predefiniti >= 2);
        assertTrue(tutti > predefiniti);
    }

    @Test
    void ilServizioDellAutoreDisattivatoNonEUnProblema() {
        Voce v = DiagnosiServizi.Elenco(new Impostazioni("", false, false)).stream()
                .filter(x -> x.categoria().equals(DiagnosiServizi.CAT_AUTORE)).findFirst().orElseThrow();
        assertEquals(Gravita.SPENTO, v.gravita());
        assertEquals(Stato.INFO, DiagnosiServizi.Classifica(v, risposta(503, "", 1), null).stato());
    }

    @Test
    void gliExchangeDiJavaENodeSonoGliStessiDeiPrezziELoScriptHaGliStessiIndirizzi() throws IOException {
        assertEquals(Prezzi.EXCHANGE_CCXT, DiagnosiServizi.IdExchange(), "stessi exchange dei prezzi");
        Path script = Paths.get(System.getProperty("user.dir"), "Scripts", "Verifica_Rete.js");
        String js = Files.readString(script, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?m)^\\s+(\\w+): '(https://[^']+)',?\\s*$").matcher(js);
        Map<String, String> daScript = new LinkedHashMap<>();
        while (m.find()) daScript.put(m.group(1), m.group(2));
        assertEquals(DiagnosiServizi.ENDPOINT_EXCHANGE, daScript, "Verifica_Rete.js e DiagnosiServizi.ENDPOINT_EXCHANGE vanno tenuti uguali");
    }

    // ---------------------------------------------------------------- Node

    @Test
    void leRigheDelloScriptNodeDiventanoEsiti() {
        Esito amb = DiagnosiServizi.EsitoDaRigaNode("{\"tipo\":\"ambiente\",\"node\":\"v24.7.0\",\"piattaforma\":\"win32-x64\",\"ccxt\":\"4.5.77\",\"ipv6\":false,\"proxy\":false,\"correzione\":false}");
        assertNotNull(amb);
        assertTrue(amb.dettaglio().contains("correzione di rete (Ccxt_Rete.js): assente"), amb.dettaglio());
        assertTrue(amb.dettaglio().contains("IPv6 globale: no"));
        Esito ok = DiagnosiServizi.EsitoDaRigaNode("{\"tipo\":\"exchange\",\"exchange\":\"okx\",\"ok\":true,\"ms\":212,\"nota\":\"\"}");
        assertEquals(Stato.OK, ok.stato());
        assertEquals(212, ok.ms());
        Esito ko = DiagnosiServizi.EsitoDaRigaNode("{\"tipo\":\"exchange\",\"exchange\":\"kucoin\",\"ok\":false,\"ms\":15003,\"errore\":\"NetworkError: fetch failed\"}");
        assertEquals(Stato.AVVISO, ko.stato());
        assertTrue(ko.dettaglio().contains("non raggiungibile da Node"));
        assertNull(DiagnosiServizi.EsitoDaRigaNode("rumore non json"));
        assertNull(DiagnosiServizi.EsitoDaRigaNode("{\"tipo\":\"altro\"}"));
    }

    // ---------------------------------------------------------------- diagnosi

    private static Esito java(String id, Stato s) {
        return new Esito(DiagnosiServizi.CAT_EXCHANGE_JAVA, id, s, 100, "x");
    }

    private static Esito node(String id, Stato s) {
        return new Esito(DiagnosiServizi.CAT_EXCHANGE_NODE, id, s, 100, "x");
    }

    @Test
    void senzaInternetLaDiagnosiELaSolaConnessione() {
        List<Esito> esiti = List.of(
                new Esito(DiagnosiServizi.CAT_BASE, "Internet (cloudflare.com)", Stato.ERRORE, 5000, "non raggiungibile (DNS): nome non risolto"),
                new Esito(DiagnosiServizi.CAT_PREZZI, "DefiLlama", Stato.ERRORE, 5000, "non raggiungibile"));
        List<String> c = DiagnosiServizi.Conclusioni(esiti);
        assertEquals(1, c.size());
        assertTrue(c.get(0).contains("non raggiunge Internet"));
    }

    @Test
    void seJavaArrivaENodeNoLaCausaENodeNonLaRete() {
        List<Esito> esiti = new ArrayList<>(List.of(
                new Esito(DiagnosiServizi.CAT_BASE, "Internet", Stato.OK, 100, "x")));
        for (String id : List.of("binance", "okx", "kucoin")) {
            esiti.add(java(id, Stato.OK));
            esiti.add(node(id, Stato.AVVISO));
        }
        List<String> c = DiagnosiServizi.Conclusioni(esiti);
        assertEquals(1, c.size(), c.toString());
        assertTrue(c.get(0).contains("Java raggiunge gli exchange ma Node.js no"), c.get(0));
        assertTrue(c.get(0).contains("antivirus"), c.get(0));
    }

    @Test
    void seNodeFallisceSoloSuAlcuniExchangeLiNomina() {
        List<Esito> esiti = List.of(java("binance", Stato.OK), node("binance", Stato.OK), java("okx", Stato.OK), node("okx", Stato.AVVISO),
                java("kucoin", Stato.OK), node("kucoin", Stato.AVVISO));
        List<String> c = DiagnosiServizi.Conclusioni(esiti);
        assertEquals(1, c.size(), c.toString());
        assertTrue(c.get(0).contains("okx, kucoin"), c.get(0));
    }

    @Test
    void unExchangeIrraggiungibileDaEntrambeLeStradeEUnBloccoDellaRete() {
        List<Esito> esiti = List.of(java("okx", Stato.AVVISO), node("okx", Stato.AVVISO), java("binance", Stato.OK), node("binance", Stato.OK));
        List<String> c = DiagnosiServizi.Conclusioni(esiti);
        assertEquals(1, c.size(), c.toString());
        assertTrue(c.get(0).contains("né da Java né da Node"), c.get(0));
    }

    @Test
    void conTuttoARegolaLaDiagnosiLoDice() {
        List<Esito> esiti = List.of(new Esito(DiagnosiServizi.CAT_BASE, "Internet", Stato.OK, 100, "x"), java("okx", Stato.OK), node("okx", Stato.OK),
                new Esito(DiagnosiServizi.CAT_AMBIENTE, "Sistema", Stato.INFO, -1, "Linux"));
        assertEquals(List.of("Tutto a posto: tutti i servizi controllati rispondono nei tempi normali."), DiagnosiServizi.Conclusioni(esiti));
    }

    @Test
    void leRisposteLenteEGliAvvisiDeiServiziEssenzialiCompaionoInDiagnosi() {
        List<Esito> esiti = List.of(
                new Esito(DiagnosiServizi.CAT_BASE, "Internet", Stato.OK, 100, "x"),
                new Esito(DiagnosiServizi.CAT_PREZZI, "DefiLlama", Stato.LENTO, 4000, "lenta"),
                new Esito(DiagnosiServizi.CAT_AGGIORNAMENTI, "GitHub", Stato.AVVISO, 90, "limite di richieste raggiunto"));
        String t = String.join("\n", DiagnosiServizi.Conclusioni(esiti));
        assertTrue(t.contains("GitHub: limite di richieste raggiunto"), t);
        assertTrue(t.contains("Risposte lente: DefiLlama (4000 ms)"), t);
    }

    @Test
    void ilRapportoRaggruppaPerCategoriaEIncludeLaDiagnosi() {
        List<Esito> esiti = List.of(java("okx", Stato.OK), new Esito(DiagnosiServizi.CAT_AMBIENTE, "Sistema", Stato.INFO, -1, "Linux"));
        String r = DiagnosiServizi.Rapporto(esiti, "13/11/2026 19:00:00");
        assertTrue(r.indexOf("Ambiente") < r.indexOf("Exchange (da Java)"), r);
        assertTrue(r.contains("  [OK] okx - 100 ms - x"), r);
        assertTrue(r.contains("Diagnosi"), r);
    }
}
