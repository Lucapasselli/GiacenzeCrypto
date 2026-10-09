package com.giacenzecrypto.giacenze_crypto;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regole di {@link PrezziDefiLlama}: quale quotazione di {@code /batchHistorical} diventa un prezzo, e come si
 * dividono le richieste nell'URL. Solo la parte
 * senza rete e senza database: l'elenco CoinGecko arriva da un predicato.
 *
 * <p>Le risposte di esempio hanno la forma reale, presa il 2026-10-09 per il 31/12/2025 23:00 UTC
 * (un token GM di GMX e un vault Beefy).
 */
class PrezziDefiLlamaTest {

    /** 31/12/2025 23:00 UTC, cioe' il fine anno 2025 come lo prezza il quadro RW (01/01/2026 00:00 in Italia). */
    private static final long FINE_2025 = 1767222000000L;

    private static final String GM = "0x47c031236e19d024b42f8ae6780e44a573170703";
    private static final String MOO = "0x09139A80454609B69700836A9EE12DB4B5DBB15F";

    /** Risposta con un token e i suoi punti: {secondi, prezzo, affidabilita' o null}. */
    private static String risposta(String chiave, Object[]... punti) {
        StringBuilder sb = new StringBuilder("{\"coins\":{\"" + chiave + "\":{\"symbol\":\"X\",\"prices\":[");
        for (int i = 0; i < punti.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"timestamp\":").append(punti[i][0]).append(",\"price\":").append(punti[i][1]);
            if (punti[i][2] != null) sb.append(",\"confidence\":").append(punti[i][2]);
            sb.append('}');
        }
        return sb.append("]}}}").toString();
    }

    private static Object[] punto(long secondi, double prezzo, Double affidabilita) {
        return new Object[]{secondi, prezzo, affidabilita};
    }

    private static Map<String, List<Long>> istanti(String chiave, long... ms) {
        Map<String, List<Long>> m = new LinkedHashMap<>();
        List<Long> l = new ArrayList<>();
        for (long x : ms) l.add(x);
        m.put(chiave, l);
        return m;
    }

    private static Map<String, List<String[]>> dest(String chiave, String address, String rete) {
        Map<String, List<String[]>> m = new LinkedHashMap<>();
        List<String[]> l = new ArrayList<>();
        l.add(new String[]{address, rete});
        m.put(chiave, l);
        return m;
    }

    private static List<PrezziDefiLlama.Quotazione> interpreta(String json, boolean inElenco) {
        String k = "arbitrum:" + GM;
        return PrezziDefiLlama.Interpreta(json, istanti(k, FINE_2025), dest(k, GM, "ARB"), t -> inElenco);
    }

    @Test
    void sopraLaSogliaVale() {
        assertTrue(PrezziDefiLlama.Ammesso(0.93, false));
    }

    @Test
    void allaSogliaOSottoValeSoloInElencoCoinGecko() {
        assertFalse(PrezziDefiLlama.Ammesso(0.9, false), "la soglia e' esclusa");
        assertFalse(PrezziDefiLlama.Ammesso(0.5, false));
        assertTrue(PrezziDefiLlama.Ammesso(0.5, true), "in elenco come prima: l'affidabilita' non conta");
    }

    @Test
    void senzaAffidabilitaValeSoloInElencoCoinGecko() {
        assertFalse(PrezziDefiLlama.Ammesso(null, false), "le memecoin del 2021 arrivano senza confidence");
        assertTrue(PrezziDefiLlama.Ammesso(null, true));
    }

    @Test
    void laQuotazioneTieneLOraRestituitaELAddressDelChiamante() {
        String k = "arbitrum:" + GM;
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(
                risposta(k, punto(1767223653L, 2.43, 0.93)), istanti(k, FINE_2025),
                dest(k, GM.toUpperCase(), "ARB"), t -> false);
        assertEquals(1, q.size());
        assertEquals(1767223653000L, q.get(0).timestamp(), "si salva l'ora della quotazione, non quella chiesta");
        assertEquals(GM.toUpperCase(), q.get(0).address(), "la cache confronta l'address esatto: resta quello del chiamante");
        assertEquals("ARB", q.get(0).rete());
        assertEquals(2.43, q.get(0).prezzoUSD(), 1e-12);
    }

    @Test
    void laChiaveDellaRispostaSiConfrontaSenzaMaiuscole() {
        String chiaveRisposta = "base:" + MOO;
        String chiaveRichiesta = chiaveRisposta.toLowerCase();
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(
                risposta(chiaveRisposta, punto(1767223577L, 1.5e8, 0.94)),
                istanti(chiaveRichiesta, FINE_2025), dest(chiaveRichiesta, MOO, "BASE"), t -> false);
        assertEquals(1, q.size());
    }

    @Test
    void oltreLOraNonVale() {
        assertTrue(interpreta(risposta("arbitrum:" + GM, punto(FINE_2025 / 1000 + 3601, 2.43, 0.99)), true).isEmpty());
        assertEquals(1, interpreta(risposta("arbitrum:" + GM, punto(FINE_2025 / 1000 - 3600, 2.43, 0.99)), true).size());
    }

    @Test
    void unPuntoValeSeEVicinoAQualunqueIstanteChiestoPerQuelToken() {
        String k = "arbitrum:" + GM;
        long altro = FINE_2025 - 10L * 86400000L;
        String json = risposta(k, punto(altro / 1000 + 600, 2.1, 0.93), punto(FINE_2025 / 1000 + 1653, 2.43, 0.93));
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(json, istanti(k, altro, FINE_2025), dest(k, GM, "ARB"), t -> false);
        assertEquals(2, q.size());
    }

    @Test
    void loStessoPuntoRestituitoPerPiuIstantiSiSalvaUnaVolta() {
        String k = "arbitrum:" + GM;
        String json = risposta(k, punto(1767223653L, 2.43, 0.93), punto(1767223653L, 2.43, 0.93));
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(json,
                istanti(k, FINE_2025, FINE_2025 + 600000), dest(k, GM, "ARB"), t -> false);
        assertEquals(1, q.size());
    }

    @Test
    void affidabilitaBassaFuoriElencoNonVale() {
        String json = risposta("arbitrum:" + GM, punto(1767223653L, 2.43, 0.6));
        assertTrue(interpreta(json, false).isEmpty());
        assertEquals(1, interpreta(json, true).size());
    }

    @Test
    void lElencoCoinGeckoSiConsultaSoloSeServe() {
        String k = "arbitrum:" + GM;
        int[] consultato = {0};
        PrezziDefiLlama.Interpreta(risposta(k, punto(1767223653L, 2.43, 0.93)), istanti(k, FINE_2025),
                dest(k, GM, "ARB"), t -> { consultato[0]++; return false; });
        assertEquals(0, consultato[0], "sopra soglia l'elenco non serve: scaricarlo costa secondi una volta al giorno");
    }

    @Test
    void prezzoNulloOTokenNonChiestoSiIgnorano() {
        assertTrue(interpreta(risposta("arbitrum:" + GM, punto(1767223653L, 0, 0.99)), true).isEmpty());
        assertTrue(interpreta(risposta("arbitrum:0x0000000000000000000000000000000000000001", punto(1767223653L, 1, 0.99)), true).isEmpty());
        assertTrue(interpreta("{\"coins\":{}}", true).isEmpty(), "un token che DefiLlama non conosce non compare nella risposta");
    }

    @Test
    void piuGrafieDelloStessoAddressSiScrivonoTutte() {
        String k = "arbitrum:" + GM;
        Map<String, List<String[]>> d = dest(k, GM, "ARB");
        d.get(k).add(new String[]{GM.toUpperCase(), "ARB"});
        assertEquals(2, PrezziDefiLlama.Interpreta(risposta(k, punto(1767223653L, 2.43, 0.93)),
                istanti(k, FINE_2025), d, t -> false).size());
    }

    @Test
    void iBlocchiStannoNellUrlEContengonoTuttiGliIstanti() {
        Map<String, TreeSet<Long>> richieste = new LinkedHashMap<>();
        int totale = 0;
        for (int t = 0; t < 40; t++) {
            TreeSet<Long> s = new TreeSet<>();
            for (int i = 0; i < 30; i++) s.add(FINE_2025 - i * 3600000L);
            richieste.put(String.format("bsc:0x%040d", t), s);
            totale += s.size();
        }
        List<Map<String, List<Long>>> blocchi = PrezziDefiLlama.Blocchi(richieste);
        assertTrue(blocchi.size() > 1);
        int contati = 0;
        for (Map<String, List<Long>> b : blocchi) {
            String url = PrezziDefiLlama.UrlBlocco(b);
            assertTrue(url.length() <= PrezziDefiLlama.LUNGHEZZA_MAX_URL, "URL di " + url.length() + " caratteri");
            for (List<Long> l : b.values()) contati += l.size();
        }
        assertEquals(totale, contati);
    }

    @Test
    void lUrlPortaGliIstantiInSecondi() {
        Map<String, List<Long>> b = istanti("arbitrum:" + GM, FINE_2025);
        String url = URLDecoder.decode(PrezziDefiLlama.UrlBlocco(b), StandardCharsets.UTF_8);
        assertTrue(url.startsWith("https://coins.llama.fi/batchHistorical?coins={\"arbitrum:" + GM + "\":[1767222000]}"), url);
        assertTrue(url.endsWith("&searchWidth=1h"));
    }
}
