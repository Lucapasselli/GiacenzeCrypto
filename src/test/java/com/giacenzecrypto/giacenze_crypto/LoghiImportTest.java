package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import javax.swing.Icon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa la convenzione con cui i loghi vengono nominati e ritrovati.
 * <p>La regola è condivisa fra chi scrive i file ({@link GeneraLoghi}) e chi li cerca a runtime
 * ({@link LoghiImport}): se le due parti divergessero, i loghi smetterebbero di comparire senza che
 * nulla vada in errore.
 */
public class LoghiImportTest {

    /**
     * Riporta la directory di lavoro alla radice del progetto, dove stanno i loghi veri.
     * <p>Serve perché altri test della suite ({@code DatabaseH2UpsertTest},
     * {@code CalcoliPlusvalenzeNewAggiornaPlusvalenzeTest}) la spostano su una cartella temporanea e non
     * la ripristinano: surefire riusa la stessa JVM, quindi senza questo tutte le icone tornerebbero
     * segnaposto e le verifiche passerebbero senza verificare nulla. Va svuotata anche la cache, che
     * potrebbe aver già memorizzato le assenze lette dalla cartella sbagliata.
     */
    @BeforeEach
    public void riportaLaCartellaDiLavoroSullaRadiceDelProgetto() {
        VarStatiche.setWorkingDirectory(System.getProperty("user.dir") + "/");
        LoghiImport.SvuotaCache();
    }

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
            //Anche il segnaposto è quadrato del lato giusto: senza questo il test passerebbe pure
            //se il file del logo non venisse trovato
            assertTrue(HaPixelVisibili(i), "il logo binance non è stato caricato, icona vuota");
        }
    }

    /** @return {@code true} se l'icona contiene almeno un pixel non trasparente */
    private static boolean HaPixelVisibili(Icon icona) {
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(
                icona.getIconWidth(), icona.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        icona.paintIcon(null, g, 0, 0);
        g.dispose();
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if ((im.getRGB(x, y) >>> 24) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void ilLatoRestaEntroLimitiUsabili() {
        assertTrue(LoghiImport.Dammi("binance", 2).getIconWidth() >= 14, "lato troppo piccolo non riportato al minimo");
        assertTrue(LoghiImport.Dammi("binance", 500).getIconWidth() <= 40, "lato troppo grande non riportato al massimo");
    }

    /**
     * I loghi vanno riprodotti come sono nel file, senza correzioni legate al tema in uso: un tentativo
     * di posare quelli poco contrastati su una piastrella neutra finiva per incorniciare di nero i loghi
     * semplicemente colorati e chiari, come il cerchio verde di Tatax.
     */
    @Test
    public void ilLogoNonVieneAlteratoInBaseAlTema() throws Exception {
        //Sagome su fondo trasparente: l'angolo deve restare trasparente anche dopo il ridimensionamento.
        //Se comparisse un colore pieno vorrebbe dire che è stato aggiunto uno sfondo che nel file non c'è
        for (String nome : new String[]{"tatax", "dogecoin-doge", "ledger-live"}) {
            assertEquals(0, AlphaAngolo(LoghiImport.Dammi(nome, 32)),
                    "l'angolo del logo " + nome + " non è trasparente: è stato aggiunto uno sfondo");
        }

        //Loghi che portano il proprio fondo pieno fino ai bordi: quello va conservato, non rimosso
        int alphaOriginale = javax.imageio.ImageIO.read(new File("config/loghi", "binance.png")).getRGB(0, 0) >>> 24;
        assertEquals(255, alphaOriginale, "binance.png non è più un quadrato opaco: scegliere un altro esempio");
        assertEquals(255, AlphaAngolo(LoghiImport.Dammi("binance", 32)),
                "il fondo pieno del logo binance è andato perso");
    }

    /** @return la trasparenza dell'angolo in alto a sinistra dell'icona, da 0 (trasparente) a 255 (opaco) */
    private static int AlphaAngolo(Icon icona) {
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(
                icona.getIconWidth(), icona.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        icona.paintIcon(null, g, 0, 0);
        g.dispose();
        return im.getRGB(0, 0) >>> 24;
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
