package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Le date {@code dal} degli alias.
 * <p>Dalla 1.0.65.14 le voci valgono per tutti gli anni: il {@code dal} conta solo con {@code "dalVincolante": true},
 * cioe' quando prima di quella data il sostituto non ha prezzi (MATIC → POL prima della migrazione). Le altre date
 * restano nel file per le versioni dalla 1.0.65 alla 1.0.65.13, che le leggono tutte.
 */
class AliasPrezziTokenDateTest {

    /** 31/12/2025 23:59 a Roma: prima del {@code dal} 2026-01-01 delle voci aggiunte nel 2026. */
    private static final long FINE_2025 = FunzioniDate.ConvertiDatainLongMinuto("2025-12-31 23:59");

    /** WETH su Arbitrum: voce nuova, {@code dal} 2026-01-01 non vincolante. */
    private static final String WETH_ARB = "0x82af49447d8a07e3bd95bd0d56f35241523fbab1";
    /** WETH su Base: una delle 12 voci di prima, senza data. */
    private static final String WETH_BASE = "0x4200000000000000000000000000000000000006";

    @BeforeAll
    static void carica() {
        AliasPrezziToken.Carica();
    }

    @Test
    void unDalNonVincolanteNonConta() {
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_ARB, "ARB", FINE_2025),
                "il dal 2026-01-01 serve solo alle versioni fino alla 1.0.65.13");
        assertEquals("BNB", AliasPrezziToken.StessoPrezzo("WBNB", FINE_2025));
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_ARB, "ARB", FunzioniDate.ConvertiDatainLongMinuto("2022-06-01 12:00")));
    }

    @Test
    void leVociDiPrimaValgonoSempre() {
        assertEquals("ETH", AliasPrezziToken.Alias(WETH_BASE, "BASE", FunzioniDate.ConvertiDatainLongMinuto("2022-06-01 12:00")));
        assertEquals("ETH", AliasPrezziToken.StessoPrezzo("WETH", FINE_2025));
        assertEquals("PEPE", AliasPrezziToken.StessoPrezzo("PEPE", FINE_2025));
    }

    @Test
    void maticEdEth2HannoUnDalVincolante() {
        //Fine 2022 e fine 2023 del quadro RW: POL non ha quotazioni prima di settembre 2024, MATIC si'
        //(segnalazione del 25/10/2026: con l'opzione "anni passati", poi tolta, quei due anni restavano senza prezzo)
        assertEquals("MATIC", AliasPrezziToken.StessoPrezzo("MATIC", FunzioniDate.ConvertiDatainLongMinuto("2023-01-01 00:00")));
        assertEquals("MATIC", AliasPrezziToken.StessoPrezzo("MATIC", FunzioniDate.ConvertiDatainLongMinuto("2024-01-01 00:00")));
        assertEquals("MATIC", AliasPrezziToken.StessoPrezzo("MATIC", FunzioniDate.ConvertiDatainLongMinuto("2024-09-03 23:59")),
                "prima della migrazione MATIC si prezza come MATIC");
        assertEquals("POL", AliasPrezziToken.StessoPrezzo("MATIC", FunzioniDate.ConvertiDatainLongMinuto("2024-09-04 00:01")));
        assertEquals("POL", AliasPrezziToken.StessoPrezzo("MATIC", FunzioniDate.ConvertiDatainLongMinuto("2025-01-01 00:00")),
                "il valore di fine 2024 del quadro RW");
        assertEquals("ETH", AliasPrezziToken.StessoPrezzo("ETH2", FunzioniDate.ConvertiDatainLongMinuto("2025-01-01 00:00")));
        assertEquals("ETH2", AliasPrezziToken.StessoPrezzo("ETH2", FunzioniDate.ConvertiDatainLongMinuto("2020-11-30 23:59")));
    }

    @Test
    void soloMaticEdEth2SonoVincolati() {
        //Un vincolo nuovo e' una decisione fiscale: aggiungerlo deve far fallire questo test e farlo guardare
        assertEquals(java.util.Set.of("MATIC", "ETH2"), AliasPrezziToken.DalStessoPrezzo.keySet());
        assertTrue(AliasPrezziToken.DalAlias.isEmpty());
    }

    @Test
    void ogniVoceNuovaHaUnaData() throws Exception {
        //Per le versioni dalla 1.0.65 alla 1.0.65.13, che leggono ogni `dal`: senza, una voce nuova cambierebbe
        //per loro i valori di anni gia' dichiarati e nasconderebbe i prezzi personalizzati per simbolo
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
              {"rete":"BASE","address":"0x4200000000000000000000000000000000000006","prezzoDa":"ETH","dal":"2026-01-01","dalVincolante":true}
            ],
             "stessoPrezzo":{"WETH":"ETH","WBNB":{"prezzoDa":"BNB","dal":"2026-01-01"},"WXYZ":{"prezzoDa":"XYZ","dal":"boh"},
                             "MATIC":{"prezzoDa":"POL","dal":"2024-09-04","dalVincolante":true}}}""";
        AliasPrezziToken.Tabelle t = AliasPrezziToken.Interpreta(contenuto, "prova");
        assertEquals(1, t.alias().size());
        assertEquals("BNB", t.stessoPrezzo().get("WBNB"));
        assertFalse(t.dalStessoPrezzo().containsKey("WBNB"), "dal senza dalVincolante: la voce vale sempre");
        assertTrue(t.dalStessoPrezzo().containsKey("MATIC"));
        assertTrue(t.dalAlias().containsKey("0x4200000000000000000000000000000000000006_BASE"));
        assertFalse(t.stessoPrezzo().containsKey("WXYZ"));
    }
}
