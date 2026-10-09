package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regole di {@link PrezziDefiLlama}: quale quotazione di {@code /prices/historical} diventa un prezzo.
 * Solo la parte senza rete e senza database ({@link PrezziDefiLlama#Interpreta}, {@link PrezziDefiLlama#Ammesso}):
 * l'elenco CoinGecko arriva da un predicato.
 *
 * <p>La risposta di esempio ha la forma reale, presa il 2026-10-09 per il 31/12/2025 23:00 UTC
 * (un token GM di GMX e un vault Beefy).
 */
class PrezziDefiLlamaTest {

    /** 31/12/2025 23:00 UTC, cioe' il fine anno 2025 come lo prezza il quadro RW (01/01/2026 00:00 in Italia). */
    private static final long FINE_2025 = 1767222000000L;

    private static final String GM = "0x47c031236e19d024b42f8ae6780e44a573170703";
    private static final String MOO = "0x09139A80454609B69700836A9EE12DB4B5DBB15F";

    private static String risposta(String chiave, double prezzo, long tsSecondi, Double affidabilita) {
        return "{\"coins\":{\"" + chiave + "\":{\"decimals\":18,\"symbol\":\"X\",\"price\":" + prezzo
                + ",\"timestamp\":" + tsSecondi
                + (affidabilita == null ? "" : ",\"confidence\":" + affidabilita) + "}}}";
    }

    private static Map<String, List<String[]>> uno(String chiaveMinuscola, String address, String rete) {
        Map<String, List<String[]>> m = new LinkedHashMap<>();
        List<String[]> l = new ArrayList<>();
        l.add(new String[]{address, rete});
        m.put(chiaveMinuscola, l);
        return m;
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
        String json = risposta("arbitrum:" + GM, 2.43, 1767223653L, 0.93);
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(json, FINE_2025,
                uno("arbitrum:" + GM, GM.toUpperCase(), "ARB"), t -> false);
        assertEquals(1, q.size());
        assertEquals(1767223653000L, q.get(0).timestamp(), "si salva l'ora della quotazione, non quella chiesta");
        assertEquals(GM.toUpperCase(), q.get(0).address(), "la cache confronta l'address esatto: resta quello del chiamante");
        assertEquals("ARB", q.get(0).rete());
        assertEquals(2.43, q.get(0).prezzoUSD(), 1e-12);
    }

    @Test
    void laChiaveDellaRispostaSiConfrontaSenzaMaiuscole() {
        String json = risposta("base:" + MOO, 1.5e8, 1767223577L, 0.94);
        List<PrezziDefiLlama.Quotazione> q = PrezziDefiLlama.Interpreta(json, FINE_2025,
                uno(("base:" + MOO).toLowerCase(), MOO, "BASE"), t -> false);
        assertEquals(1, q.size());
    }

    @Test
    void oltreLOraNonVale() {
        long troppoLontano = FINE_2025 / 1000 + 3601;
        String json = risposta("arbitrum:" + GM, 2.43, troppoLontano, 0.99);
        assertTrue(PrezziDefiLlama.Interpreta(json, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).isEmpty());
        String alLimite = risposta("arbitrum:" + GM, 2.43, FINE_2025 / 1000 - 3600, 0.99);
        assertEquals(1, PrezziDefiLlama.Interpreta(alLimite, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).size());
    }

    @Test
    void affidabilitaBassaFuoriElencoNonVale() {
        String json = risposta("arbitrum:" + GM, 2.43, 1767223653L, 0.6);
        assertTrue(PrezziDefiLlama.Interpreta(json, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> false).isEmpty());
        assertEquals(1, PrezziDefiLlama.Interpreta(json, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).size());
    }

    @Test
    void prezzoNulloOTokenNonChiestoSiIgnorano() {
        String zero = risposta("arbitrum:" + GM, 0, 1767223653L, 0.99);
        assertTrue(PrezziDefiLlama.Interpreta(zero, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).isEmpty());
        String altro = risposta("arbitrum:0x0000000000000000000000000000000000000001", 1, 1767223653L, 0.99);
        assertTrue(PrezziDefiLlama.Interpreta(altro, FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).isEmpty());
        assertTrue(PrezziDefiLlama.Interpreta("{\"coins\":{}}", FINE_2025, uno("arbitrum:" + GM, GM, "ARB"), t -> true).isEmpty());
    }

    @Test
    void piuGrafieDelloStessoAddressSiScrivonoTutte() {
        Map<String, List<String[]>> m = uno("arbitrum:" + GM, GM, "ARB");
        m.get("arbitrum:" + GM).add(new String[]{GM.toUpperCase(), "ARB"});
        String json = risposta("arbitrum:" + GM, 2.43, 1767223653L, 0.93);
        assertEquals(2, PrezziDefiLlama.Interpreta(json, FINE_2025, m, t -> false).size());
    }

    @Test
    void unaSerieScartataNonFaChiedereLIstante() {
        String address = "0x00000000000000000000000000000000000000aa";
        assertTrue(PrezziDefiLlama.DaChiedereAllIstante(address, "BSC"), "mai chiesto a /chart: si chiede");
        PrezziDefiLlama.RegistraEsitoSerie(address, "BSC", false);
        assertFalse(PrezziDefiLlama.DaChiedereAllIstante(address.toUpperCase(), "BSC"));
        PrezziDefiLlama.RegistraEsitoSerie(address, "BSC", true);
        assertTrue(PrezziDefiLlama.DaChiedereAllIstante(address, "BSC"));
    }
}
