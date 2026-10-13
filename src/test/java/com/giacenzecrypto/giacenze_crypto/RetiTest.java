package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * {@code config/varie/Reti.json} ({@link Reti}) e {@code config/varie/IndirizziServizi.json} ({@link IndirizziServizi}).
 * Fissa quello che il file ha sostituito nel codice, cosi' una modifica del file che lo cambia per sbaglio si vede.
 */
public class RetiTest {

    /** Aggiungere reti al file e' lo scopo: si controlla che quelle di prima ci siano ancora, nello stesso ordine. */
    @Test
    public void laTendinaDiGestioneWalletContieneQuellaDiPrima() {
        List<String> prima = List.of("--- nessuna selezione ---", "Arbitrum (ARB)", "Avalanche (AVAX)", "Base (BASE)", "Berachain (BERA)",
                "Binance Smart Chain (BSC)", "Bitcoin (BTC)", "Cronos Chain (CRO)", "Ethereum (ETH)", "Gnosis Chain (GNOSIS)",
                "Solana (SOL)", "Polygon (POL)", "Monad (MONAD)", "Linea (LINEA)", "Blast (BLAST)", "Unichain (UNICHAIN)",
                "World Chain (WORLD)", "Taiko (TAIKO)", "Abstract (ABSTRACT)", "Katana (KATANA)", "Sonic (SONIC)", "Mantle (MANTLE)",
                "Optimism (OP)", "HyperEVM (HYPEREVM)", "Ink (INK)", "Robinhood Chain (ROBINHOOD)", "X Layer (XLAYER)");
        List<String> ora = List.of(Reti.EtichetteComboWallet());
        int i = 0;
        for (String v : ora) if (i < prima.size() && v.equals(prima.get(i))) i++;
        assertEquals(prima.size(), i, "manca o e' fuori ordine: " + (i < prima.size() ? prima.get(i) : ""));
        assertEquals("--- nessuna selezione ---", ora.get(0));
        assertTrue(Reti.EtichetteImport().length >= 40);
        assertTrue(List.of(Reti.EtichetteImport()).containsAll(List.of("Cardano (ADA)", "Zcash (ZEC)", "Tron (TRX)")));
    }

    @Test
    public void lePagineDelBrowserSonoQuelleDiPrima() {
        assertEquals("https://bscscan.com/token/0xT?a=0xW", Funzioni_WalletDeFi.UrlMovimentiWallet("0xW", "0xT", "bsc"));
        assertEquals("https://explorer.inkonchain.com/address/0xW?tab=token_transfers&token=0xT",
                Funzioni_WalletDeFi.UrlMovimentiWallet("0xW", "0xT", "INK"));
        assertEquals("https://mempool.space/address/bc1q", Funzioni_WalletDeFi.UrlMovimentiWallet("bc1q", "", "BTC"));
        assertEquals("https://solscan.io/account/S#portfolio", Funzioni_WalletDeFi.UrlSituazioneWallet("S", "SOL"));
        assertEquals("https://robinhoodchain.blockscout.com/address/0xW?tab=tokens", Funzioni_WalletDeFi.UrlSituazioneWallet("0xW", "ROBINHOOD"));
        //AVAX non ha mai avuto queste due pagine: il pulsante non apre nulla, come prima
        assertNull(Funzioni_WalletDeFi.UrlMovimentiWallet("0xW", "0xT", "AVAX"));
        assertNull(Funzioni_WalletDeFi.UrlSituazioneWallet("0xW", "AVAX"));
        assertNull(Funzioni_WalletDeFi.UrlSituazioneWallet("0xW", "ADA"));
    }

    @Test
    public void lApiDegliExplorerNonSiScriveNelFile() {
        //A quelle API va la ApiKey: l'indirizzo lo costruisce il codice dal chain id, o e' nel codice
        assertEquals("https://api.etherscan.io/v2/api?chainid=42161", Reti.Get("ARB").api());
        assertEquals("https://explorer-api.cronos.org/mainnet/api/v2", Reti.Get("CRO").api());
        assertEquals("", Reti.Get("INK").api());
        String json = "{\"reti\":[{\"id\":\"ABC\",\"nome\":\"Abc\",\"defi\":true,\"api\":\"https://altrove.example\","
                + "\"urlTransazione\":\"https://x.example/tx/{hash}\"}]}";
        assertEquals("", Reti.Interpreta(json, "prova").perId().get("ABC").api());
    }

    @Test
    public void unFileNonValidoNonSiUsa() {
        assertNull(Reti.Interpreta("{\"reti\":[{\"id\":\"ABC\",\"nome\":\"Abc\",\"defi\":true,\"urlTransazione\":\"http://x/{hash}\"}]}", "prova"));
        assertNull(Reti.Interpreta("{\"reti\":[{\"id\":\"ABC\",\"nome\":\"Abc\",\"nodiStorico\":[\"http://nodo\"]}]}", "prova"));
        assertNull(Reti.Interpreta("{\"reti\":[{\"id\":\"ABC\",\"nome\":\"Abc\"},{\"id\":\"ABC\",\"nome\":\"Abc\"}]}", "prova"));
        assertNull(Reti.Interpreta("{\"reti\":[{\"id\":\"XLAYER\",\"nome\":\"X\",\"tokenNodo\":[{\"indirizzo\":\"0x12\",\"simbolo\":\"U\",\"decimali\":6}]}]}", "prova"));
        assertNull(Reti.Interpreta("{\"reti\":[]}", "prova"));
    }

    @Test
    public void iTokenDellaCartaOkxVengonoDalFile() {
        Map<String, Trans_XLayer.Token> m = Trans_XLayer.Monete();
        assertEquals("USDG", m.values().iterator().next().simbolo());
        Trans_XLayer.Token a = m.get("0x228765a3c18065c923f23a0ccb6c7cefb3ea2223");
        assertNotNull(a);
        assertTrue(a.aToken());
        assertEquals(6, a.decimali());
    }

    @Test
    public void ilNodoPubblicoDiBaseVieneDalFile() {
        NodoPubblicoDefi.Configurazione c = NodoPubblicoDefi.ConfigurazioneRete("base");
        assertNotNull(c);
        assertEquals("https://mainnet.base.org", c.nodiStato().get(0));
        assertEquals(List.of("0x4200000000000000000000000000000000000006"), c.tokenAvvolti());
        assertTrue(c.tokenNoti().contains("0x833589fcd6edb6e08f4c7c32d4f71b54bda02913")); //USDC
        assertFalse(NodoPubblicoDefi.ReteSupportata("ETH"));
        assertTrue(GiacenzeBlockchain.SenzaStorico("monad"));
    }

    @Test
    public void gliIndirizziDeiServiziSonoSoloHttpsSenzaParametri() {
        assertEquals("https://coins.llama.fi", IndirizziServizi.Url(IndirizziServizi.DEFILLAMA));
        assertEquals("https://mempool.space/api", IndirizziServizi.Url(IndirizziServizi.MEMPOOL));
        for (String k : List.of(IndirizziServizi.BANCA_ITALIA, IndirizziServizi.COINBASE, IndirizziServizi.COINMARKETCAP_DATI,
                IndirizziServizi.GOPLUS, IndirizziServizi.BINANCE_PUBBLICO, IndirizziServizi.CRONOS_EXPLORER)) {
            assertTrue(IndirizziServizi.Url(k).startsWith("https://"), k);
        }
        assertNull(IndirizziServizi.Interpreta("{\"indirizzi\":{\"defillama\":\"http://coins.llama.fi\"}}", "prova"));
        assertNull(IndirizziServizi.Interpreta("{\"indirizzi\":{\"defillama\":\"https://coins.llama.fi/\"}}", "prova"));
        assertNull(IndirizziServizi.Interpreta("{\"indirizzi\":{\"defillama\":\"https://coins.llama.fi?k=1\"}}", "prova"));
        assertNull(IndirizziServizi.Interpreta("{\"indirizzi\":{\"defillama\":\"https://u:p@coins.llama.fi\"}}", "prova"));
    }
}
