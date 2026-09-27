package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Le date {@code dal} degli alias e l'opzione "anche per gli anni già dichiarati".
 * <p>Esistono per una sola ragione: chi ha già presentato una dichiarazione con il programma non deve
 * vedersi cambiare il quadro RW di quegli anni perché è stato aggiornato l'elenco. Questi test fissano
 * che, con l'opzione spenta, prima del {@code dal} tutto funzioni esattamente come senza la voce.
 */
class AliasPrezziTokenDateTest {

    /** 31/12/2025 23:59 e 01/01/2026 00:01 a Roma: i due istanti a cavallo del {@code dal} delle voci nuove. */
    private static final long FINE_2025 = FunzioniDate.ConvertiDatainLongMinuto("2025-12-31 23:59");
    private static final long INIZIO_2026 = FunzioniDate.ConvertiDatainLongMinuto("2026-01-01 00:01");
    /** L'istante con cui Calcoli_RW valorizza la fine del 2025 (e l'inizio del 2026). */
    private static final long FINE_ANNO_RW_2025 = FunzioniDate.ConvertiDatainLongMinuto("2026-01-01 00:00");

    /** WETH su Arbitrum: voce nuova, {@code dal} 2026-01-01. */
    private static final String WETH_ARB = "0x82af49447d8a07e3bd95bd0d56f35241523fbab1";
    /** WETH su Base: una delle 12 voci di prima, senza data. */
    private static final String WETH_BASE = "0x4200000000000000000000000000000000000006";

    @BeforeAll
    static void carica() {
        AliasPrezziToken.Carica();
    }

    @AfterEach
    void spegneOpzione() {
        AliasPrezziToken.AncheAnniPassati = false;
    }

    @Test
    void unaVoceNuovaNonValePrimaDelSuoDal() {
        assertNull(AliasPrezziToken.Alias(WETH_ARB, "ARB", FINE_2025),
                "il 31/12/2025 il WETH su Arbitrum si prezza per address, come prima dell'elenco");
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_ARB, "ARB", INIZIO_2026));
    }

    @Test
    void ilValoreDiFineAnno2025DelQuadroRwRestaQuelloDiPrima() {
        //Calcoli_RW prezza "31/12/2025 23:59" all'istante 01/01/2026 00:00: e' proprio il bordo del dal,
        //e deve restare dalla parte dell'anno gia' dichiarato
        assertNull(AliasPrezziToken.Alias(WETH_ARB, "ARB", FINE_ANNO_RW_2025));
        assertEquals("WBNB", AliasPrezziToken.StessoPrezzo("WBNB", FINE_ANNO_RW_2025));
    }

    @Test
    void leVociDiPrimaValgonoSempre() {
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_BASE, "BASE", FunzioniDate.ConvertiDatainLongMinuto("2022-06-01 12:00")));
    }

    @Test
    void conLOpzioneAccesaLeDateSiIgnorano() {
        AliasPrezziToken.AncheAnniPassati = true;
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_ARB, "ARB", FINE_2025));
        assertEquals("BNB", AliasPrezziToken.StessoPrezzo("WBNB", FINE_2025));
    }

    @Test
    void stessoPrezzoRispettaLaData() {
        assertEquals("WBNB", AliasPrezziToken.StessoPrezzo("WBNB", FINE_2025), "voce nuova: prima del dal il simbolo resta com'e'");
        assertEquals("BNB", AliasPrezziToken.StessoPrezzo("WBNB", INIZIO_2026));
        assertEquals("ETH", AliasPrezziToken.StessoPrezzo("WETH", FINE_2025), "voce di prima: vale sempre");
        assertEquals("PEPE", AliasPrezziToken.StessoPrezzo("PEPE", INIZIO_2026));
    }

    @Test
    void ogniVoceNuovaHaUnaData() throws Exception {
        //Una voce nuova senza `dal` cambierebbe i valori di anni gia' dichiarati: e' esattamente l'errore da evitare
        JSONObject json = new JSONObject(Files.readString(Path.of("config/varie/" + AliasPrezziToken.NOME + ".json")));
        Map<String, String> vecchie = AliasPrezziToken.Predefinite().alias();
        JSONArray voci = json.getJSONArray("alias");
        for (int i = 0; i < voci.length(); i++) {
            JSONObject v = voci.getJSONObject(i);
            String chiave = v.getString("address") + "_" + v.getString("rete");
            boolean vecchia = vecchie.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(chiave));
            assertEquals(!vecchia, v.has("dal"), (vecchia ? "voce di prima con data: " : "voce nuova senza data: ") + chiave);
        }
        JSONObject sp = json.getJSONObject("stessoPrezzo");
        for (String simbolo : sp.keySet()) {
            boolean vecchia = AliasPrezziToken.Predefinite().stessoPrezzo().containsKey(simbolo);
            assertEquals(!vecchia, sp.optJSONObject(simbolo) != null, "stessoPrezzo " + simbolo);
        }
    }

    @Test
    void unaDataIllegibileScartaLaVoceInvecedifarlaValereSempre() {
        String contenuto = """
            {"alias":[
              {"rete":"ETH","address":"0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2","prezzoDa":"ETH","dal":"31/12/2026"},
              {"rete":"BASE","address":"0x4200000000000000000000000000000000000006","prezzoDa":"ETH"}
            ],
             "stessoPrezzo":{"WETH":"ETH","WBNB":{"prezzoDa":"BNB","dal":"2026-01-01"},"WXYZ":{"prezzoDa":"XYZ","dal":"boh"}}}""";
        AliasPrezziToken.Tabelle t = AliasPrezziToken.Interpreta(contenuto, "prova");
        assertEquals(1, t.alias().size());
        assertEquals("BNB", t.stessoPrezzo().get("WBNB"));
        assertTrue(t.dalStessoPrezzo().containsKey("WBNB"));
        assertFalse(t.stessoPrezzo().containsKey("WXYZ"));
    }

    @Test
    void installazioneNuovaSeNonCiSonoMovimenti(@TempDir Path dir) throws Exception {
        Path movimenti = dir.resolve("movimenti.crypto.db");
        assertTrue(AliasPrezziToken.ValoreInizialeOpzione(movimenti), "file assente: installazione nuova");
        Files.writeString(movimenti, "");
        assertTrue(AliasPrezziToken.ValoreInizialeOpzione(movimenti), "file vuoto: nessun archivio");
        Files.writeString(movimenti, "20250101000000_x_1_1_DC;...\n");
        assertFalse(AliasPrezziToken.ValoreInizialeOpzione(movimenti), "con dei movimenti e' un aggiornamento");
    }
}
