package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import javax.swing.UIManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa il contenuto della finestra <i>Informazioni</i>, che non è testo decorativo: contiene le
 * attribuzioni dovute per contratto a CoinGecko e a Etherscan, e la dichiarazione sulla riservatezza
 * dei dati che accompagna la pubblicazione sul Microsoft Store.
 *
 * <p>Il testo si verifica senza aprire niente perché {@link GUI_Informazioni#Testo()} è separato dalla
 * costruzione della finestra: quello che deve restare vero è cosa c'è scritto, non come è disegnato.
 */
public class GUI_InformazioniTest {

    private final String EdizioneOriginale = VarStatiche.Edizione;

    @AfterEach
    public void RipristinaEdizione() {
        VarStatiche.Edizione = EdizioneOriginale;
    }

    @Test
    public void leDueAttribuzioniObbligatorieCiSonoInEntrambeLeEdizioni() {
        for (String edizione : new String[]{"completa", VarStatiche.EDIZIONE_STORE}) {
            VarStatiche.Edizione = edizione;
            String t = GUI_Informazioni.Testo();
            //CoinGecko pretende la dicitura e il collegamento; Etherscan un backlink o la dicitura.
            //Sono obblighi del prodotto, non della chiave dell'utente: valgono per ogni edizione.
            assertTrue(t.contains("Powered by") && t.contains("CoinGecko"),
                    "manca l'attribuzione CoinGecko nell'edizione " + edizione);
            assertTrue(t.contains(GUI_Informazioni.URL_COINGECKO),
                    "l'attribuzione CoinGecko deve essere un collegamento (" + edizione + ")");
            assertTrue(t.contains("Etherscan.io") && t.contains("APIs"),
                    "manca l'attribuzione Etherscan nell'edizione " + edizione);
            assertTrue(t.contains(GUI_Informazioni.URL_ETHERSCAN),
                    "l'attribuzione Etherscan deve essere un collegamento (" + edizione + ")");
        }
    }

    @Test
    public void ilCorpoNonScendeSottoIlMinimoPretesoDaCoinGecko() {
        //CoinGecko chiede un carattere non inferiore a 10, e l'utente puo' rimpicciolire tutta
        //l'interfaccia con --fontSize: il minimo va tenuto qui, non sperato dal contesto
        Font precedente = UIManager.getFont("Label.font");
        try {
            UIManager.put("Label.font", new Font(FontApplicazione.FAMIGLIA, Font.PLAIN, 8));
            assertEquals(10, GUI_Informazioni.Corpo(), "con un'interfaccia a 8 il minimo deve valere 10");
            assertTrue(GUI_Informazioni.Testo().contains("font-size:10pt"),
                    "il corpo delle attribuzioni deve essere dichiarato esplicitamente, non ereditato");

            //sopra il minimo invece si segue l'interfaccia, altrimenti su schermi grandi
            //l'attribuzione sarebbe l'unica riga piccola della finestra
            UIManager.put("Label.font", new Font(FontApplicazione.FAMIGLIA, Font.PLAIN, 16));
            assertEquals(16, GUI_Informazioni.Corpo());
            assertTrue(GUI_Informazioni.Testo().contains("font-size:16pt"));
        } finally {
            UIManager.put("Label.font", precedente);
        }
    }

    @Test
    public void laDichiarazioneSulleChiaviPrivateCiSempre() {
        for (String edizione : new String[]{"completa", VarStatiche.EDIZIONE_STORE}) {
            VarStatiche.Edizione = edizione;
            String t = GUI_Informazioni.Testo().toLowerCase();
            assertTrue(t.contains("seed phrase") && t.contains("chiavi private"),
                    "manca la dichiarazione su seed phrase e chiavi private (" + edizione + ")");
            assertTrue(t.contains("restano su questo dispositivo"),
                    "manca la dichiarazione che i dati restano sul dispositivo (" + edizione + ")");
        }
    }

    @Test
    public void lEdizioneStoreNonSiVantaDiUnAccessoAgliExchangeCheNonHa() {
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        String t = GUI_Informazioni.Testo();
        assertFalse(t.contains("Le chiavi API degli exchange che vengono eventualmente inserite"),
                "l'edizione Store non conserva chiavi di exchange : non deve nominarle come se le avesse");
        assertTrue(t.contains("non accede ad alcun conto di exchange"),
                "l'edizione Store deve dirlo esplicitamente : è la frase che regge davanti alla 10.2.6");
    }

    @Test
    public void lEdizioneCompletaSpiegaCheFineFannoLeChiaviDegliExchange() {
        VarStatiche.Edizione = "completa";
        String t = GUI_Informazioni.Testo();
        assertTrue(t.contains("Le chiavi API degli exchange che vengono eventualmente inserite"));
        assertFalse(t.contains("non accede ad alcun conto di exchange"),
                "questa frase è vera solo nell'edizione Store");
    }

    @Test
    public void ilTestoNominaLeAltreFontiELaLicenza() {
        String t = GUI_Informazioni.Testo();
        for (String fonte : new String[]{"Binance", "Coinbase", "CoinMarketCap", "DefiLlama",
            "Blockscout", "Moralis", "Helius", "Unisat", "mempool.space", "GoPlus", "Banca d'Italia"}) {
            assertTrue(t.contains(fonte), "la fonte " + fonte + " non è nominata");
        }
        assertTrue(t.contains("MIT"));
        assertTrue(t.contains("THIRD-PARTY-LICENSES.md"));
        assertTrue(t.contains("Noto Sans"), "il font incluso ha una licenza da citare (SIL OFL)");
    }
}
