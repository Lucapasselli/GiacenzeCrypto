package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Eccezione CoinGecko al controllo anti-impersonazione
 * ({@link AliasPrezziToken#MotivoImpersonazioneEsclusiCensiti}): un token con lo stesso nome di un
 * riferimento, ma il cui contratto è censito da CoinGecko, non va marcato SCAM. Ogni test scrive da sé
 * lo stato di {@code GESTITICOINGECKO}, che {@code GestitiCoingecko_ScriviNuovaTabella} ricrea da zero.
 */
class AliasPrezziTokenCoingeckoTest {

    @TempDir
    static Path tempDir;

    /** ETH di Wormhole su BSC: legittimo, non nell'elenco degli alias, si chiama "ETH" come il Binance-Peg. */
    private static final String ETH_WORMHOLE_BSC = "0x4db5a66e937a9f4473fa95b1caf1d1e1d62e29ea";
    /** Un contratto qualunque, che CoinGecko non conosce: e' la forma di un finto token inviato per spam. */
    private static final String ADDRESS_QUALUNQUE = "0x1111111111111111111111111111111111111111";

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        VarCondivise.CompilaMappaChain();
        VarCondivise.CompilaMappaRetiSupportate();
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Scrive la lista come la scrive RecuperaCoinsCoingecko: chiave in maiuscolo tranne che su Solana. */
    private static void listaCoingecko(String... addressRete) {
        List<String[]> gestiti = new ArrayList<>();
        for (String ar : addressRete) {
            String chiave = ar.endsWith("_SOL") ? ar : ar.toUpperCase();
            gestiti.add(new String[]{chiave, "eth", "Ethereum"});
        }
        DatabaseH2.GestitiCoingecko_ScriviNuovaTabella(gestiti);
    }

    @Test
    void unTokenCensitoDaCoingeckoNonEImitazione() {
        listaCoingecko(ETH_WORMHOLE_BSC + "_BSC");
        assertNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("ETH", ETH_WORMHOLE_BSC, "BSC"));
        //...e l'indirizzo scritto con le maiuscole di checksum si ritrova lo stesso
        assertNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("ETH", "0x4DB5A66E937A9F4473FA95B1CAF1D1E1D62E29EA", "BSC"));
    }

    @Test
    void unTokenNonCensitoConLoStessoNomeRestaImitazione() {
        listaCoingecko(ETH_WORMHOLE_BSC + "_BSC");
        assertNotNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("ETH", ADDRESS_QUALUNQUE, "BSC"));
        assertNotNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("USDT", ADDRESS_QUALUNQUE, "BSC"));
    }

    @Test
    void ilCensimentoValeSoloSullaSuaRete() {
        //Lo stesso contratto su un'altra rete e' un altro token: l'eccezione e' per address+rete
        listaCoingecko(ETH_WORMHOLE_BSC + "_BSC");
        assertNotNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("ETH", ETH_WORMHOLE_BSC, "ARB"));
    }

    @Test
    void conLaListaVuotaLaRegolaLavoraComePrima() {
        //Primo avvio senza rete: nessuna eccezione, nessun errore
        listaCoingecko();
        assertNotNull(AliasPrezziToken.MotivoImpersonazioneEsclusiCensiti("ETH", ETH_WORMHOLE_BSC, "BSC"));
    }
}
