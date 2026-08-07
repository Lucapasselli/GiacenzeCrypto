package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import javax.swing.Icon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Fissa la convenzione con cui i loghi vengono nominati e ritrovati.
 * <p>La regola è condivisa fra chi scrive i file ({@link GeneraLoghi}) e chi li cerca a runtime
 * ({@link LoghiImport}): se le due parti divergessero, i loghi smetterebbero di comparire senza che
 * nulla vada in errore.
 */
public class LoghiImportTest {

    @Test
    public void loSlugSegueLaConvenzioneDeiNomiFile() {
        assertEquals("binance", LoghiImport.Slug("Binance"));
        assertEquals("crypto-com-exchange", LoghiImport.Slug("Crypto.com Exchange"));
        assertEquals("bitcoin-btc", LoghiImport.Slug("Bitcoin (BTC)"));
        assertEquals("bithumb-glo", LoghiImport.Slug("Bithumb Glo."));
        assertEquals("btc-markets", LoghiImport.Slug("BTC Markets"));
        assertEquals("binance-old", LoghiImport.Slug("Binance_Old"));
        assertEquals("", LoghiImport.Slug(null));
    }

    @Test
    public void ilGeneratoreUsaLaStessaRegolaDelCaricatore() {
        for (String voce : new String[]{"Binance", "Crypto.com Exchange", "Bitcoin (BTC)", "Yield App", "DFX.swiss"}) {
            assertEquals(LoghiImport.Slug(voce), GeneraLoghi.Slug(voce),
                    "generatore e caricatore devono ricavare lo stesso nome file per " + voce);
        }
    }

    @Test
    public void unLogoMancanteRestituisceUnSegnapostoDelleStesseDimensioni() {
        //Un logo assente non deve restituire null né un'icona di dimensione diversa: nel menù
        //sposterebbe il testo e le voci non sarebbero più incolonnate
        Icon segnaposto = LoghiImport.Dammi("marchio-che-non-esiste-9f3a2b", 24);
        assertNotNull(segnaposto);
        assertEquals(24, segnaposto.getIconWidth());
        assertEquals(24, segnaposto.getIconHeight());

        Icon senzaNome = LoghiImport.Dammi("", 24);
        assertEquals(24, senzaNome.getIconWidth());
        assertEquals(24, senzaNome.getIconHeight());
    }

    @Test
    public void leIconeSonoQuadrateEDelLatoRichiesto() {
        //I loghi di partenza non sono quadrati: vengono centrati in un quadrato perché la colonna
        //del testo resti alla stessa distanza per tutte le voci
        for (int lato : new int[]{16, 24, 32}) {
            Icon i = LoghiImport.Dammi("binance", lato);
            assertEquals(lato, i.getIconWidth());
            assertEquals(lato, i.getIconHeight());
        }
    }

    @Test
    public void ilLatoRestaEntroLimitiUsabili() {
        assertTrue(LoghiImport.Dammi("binance", 2).getIconWidth() >= 14, "lato troppo piccolo non riportato al minimo");
        assertTrue(LoghiImport.Dammi("binance", 500).getIconWidth() <= 40, "lato troppo grande non riportato al massimo");
    }

    @Test
    public void iLoghiDelleVociNativeEsistono() {
        //I nomi sono scritti a mano in popolaComboTipoFile: un refuso lascerebbe la voce senza logo
        //senza nessun errore visibile
        for (String nome : new String[]{"binance", "crypto-com", "crypto-com-exchange", "cointracking", "tatax", "okx"}) {
            assertTrue(new File("config/loghi", nome + ".png").isFile(),
                    "manca il logo config/loghi/" + nome + ".png usato dalla finestra di import");
        }
    }
}
