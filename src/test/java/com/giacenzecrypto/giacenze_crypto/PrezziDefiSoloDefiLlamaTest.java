package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opzione "prezzi dei token DeFi solo da DefiLlama e coingecko"
 * ({@link Prezzi#AddressAmmessoSugliExchange}): un token cercato per address non ripiega sugli exchange per
 * simbolo, salvo che sia nell'elenco degli alias di {@code AliasPrezziToken.json}, a qualunque data.
 */
class PrezziDefiSoloDefiLlamaTest {

    private static final String RETE = "BASE";
    private static final String TOKEN = "0x1111111111111111111111111111111111111111";
    private static final String TOKEN_ALIAS = "0x4200000000000000000000000000000000000006";

    private Map<String, String> salvata;

    //La mappa si sostituisce in blocco, come fa AliasPrezziToken.Applica, mai sul posto
    @BeforeEach
    void setUp() {
        salvata = Principale.Mappa_AddressRete_Nome;
        Map<String, String> prova = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        prova.put(TOKEN_ALIAS + "_" + RETE, "ETH");
        Principale.Mappa_AddressRete_Nome = prova;
    }

    @AfterEach
    void tearDown() {
        Principale.Mappa_AddressRete_Nome = salvata;
    }

    @Test
    void conLOpzioneSpentaGliExchangeRestanoAmmessi() {
        assertTrue(Prezzi.AddressAmmessoSugliExchange(false, TOKEN, RETE));
        assertTrue(Prezzi.AddressAmmessoSugliExchange(false, TOKEN_ALIAS, RETE));
    }

    @Test
    void conLOpzioneAttivaUnTokenFuoriElencoNonVaSugliExchange() {
        assertFalse(Prezzi.AddressAmmessoSugliExchange(true, TOKEN, RETE));
    }

    @Test
    void unTokenDellElencoAliasRestaEscluso() {
        assertTrue(Prezzi.AddressAmmessoSugliExchange(true, TOKEN_ALIAS, RETE));
        //La rete fa parte della chiave: lo stesso address su un'altra rete non e' in elenco
        assertFalse(Prezzi.AddressAmmessoSugliExchange(true, TOKEN_ALIAS, "ARB"));
    }
}
