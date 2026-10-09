package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Importazioni#DeFi_TestoErroreExplorer}: che cosa legge l'utente quando l'explorer rifiuta uno
 * scaricamento. Il caso che lo ha fatto nascere (2026-10-09): Base via Blockscout senza ApiKey risponde 403 con
 * una pagina di verifica Cloudflare, e il messaggio di prima mostrava quella pagina e invitava a riprovare.
 */
class ImportazioniErroreExplorerTest {

    private static final String PAGINA_CLOUDFLARE =
            "<!DOCTYPE html><html lang=\"en-US\"><head><title>Just a moment...</title></head><body></body></html>";

    @Test
    void senzaChiaveUnaPaginaDiVerificaChiedeLaChiaveBlockscout() {
        String t = Importazioni.DeFi_TestoErroreExplorer(403, PAGINA_CLOUDFLARE, PAGINA_CLOUDFLARE,
                "https://base.blockscout.com/api", false);
        assertTrue(t.contains("base.blockscout.com"), t);
        assertTrue(t.contains("ApiKey Blockscout"), t);
        assertFalse(t.contains("<html"), "la pagina HTML non va mostrata");
        assertFalse(t.contains("Riprovare in un secondo momento"), "riprovare non serve: senza chiave non funzionera' mai");
    }

    @Test
    void laProApiSenzaChiaveChiedeLaChiave() {
        String corpo = "{\"error\":\"Proceed with API key or make a X402 payment to continue\"}";
        String t = Importazioni.DeFi_TestoErroreExplorer(401, corpo, corpo, "https://api.blockscout.com/v2/api?chain_id=8453", false);
        assertTrue(t.contains("ApiKey Blockscout"), t);
    }

    @Test
    void conLaChiaveUnRifiutoResta_unErroreDaRiprovare() {
        String t = Importazioni.DeFi_TestoErroreExplorer(403, PAGINA_CLOUDFLARE, PAGINA_CLOUDFLARE,
                "https://api.blockscout.com/v2/api?chain_id=8453", true);
        assertFalse(t.contains("ApiKey Blockscout"), t);
        assertFalse(t.contains("<html"), "anche qui la pagina HTML non va mostrata");
        assertTrue(t.contains("pagina web"), t);
    }

    @Test
    void unErroreQualunqueMantieneIlTestoDiPrima() {
        String t = Importazioni.DeFi_TestoErroreExplorer(500, "{}", "Errore interno: ", "https://eth.blockscout.com/api", false);
        assertEquals("Errore HTTP 500 durante l'importazione dei dati\nErrore interno: "
                + "\nRiprovare in un secondo momento, le API dell'Explorer non rispondono correttamente.", t);
    }
}
