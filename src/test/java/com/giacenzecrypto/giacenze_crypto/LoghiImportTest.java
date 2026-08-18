package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import javax.swing.Icon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private final String EdizioneOriginale = VarStatiche.Edizione;

    /** L'edizione è una statica: senza ripristino i test dei monogrammi cambierebbero quelli successivi */
    @org.junit.jupiter.api.AfterEach
    public void ripristinaEdizioneETema() {
        VarStatiche.Edizione = EdizioneOriginale;
        Icone.ForzaTemaScuroPerProva(false);
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
    public void ilCodiceDellaReteRitrovaIlLogoDellEtichettaCompleta() {
        //Nei dati la rete è salvata col solo codice ("ARB"), il logo prende il nome dall'etichetta
        //intera ("arbitrum-arb"): senza questa mappa la colonna "Rete" resterebbe senza icone
        java.util.Map<String, String> loghi = LoghiImport.MappaLoghiRete(new String[]{
            "--- nessuna selezione ---", "Arbitrum (ARB)", "Gnosis Chain (GNOSIS)", "Monad (MONAD)"});

        assertEquals("arbitrum-arb", loghi.get("ARB"));
        assertEquals("gnosis-chain-gnosis", loghi.get("GNOSIS"));
        assertEquals("monad-monad", loghi.get("MONAD"));
        assertEquals(3, loghi.size(), "le voci senza codice fra parentesi non vanno in mappa");
        assertEquals(null, loghi.get("SCONOSCIUTA"), "un codice ignoto non deve inventare un logo");
    }

    @Test
    public void aParitaDiCodiceVinceIlPrimoElenco() {
        java.util.Map<String, String> loghi = LoghiImport.MappaLoghiRete(
                new String[]{"Ethereum (ETH)"},
                new String[]{"Etereo (ETH)"});
        assertEquals("ethereum-eth", loghi.get("ETH"));
    }

    @Test
    public void leBlockchainDellaFinestraDiImportHannoUnLogoSuDisco() {
        //Se un file cambia nome o non viene committato l'icona sparisce senza che nulla vada in errore.
        //L'elenco è quello da cui GeneraLoghi produce i PNG: una rete aggiunta alla sola combo dei
        //wallet non avrebbe nessun logo, ed è questo il test che lo fa notare
        for (String etichetta : Importazioni_Gestione.BlockChain) {
            if (etichetta.startsWith("-")) continue;
            assertTrue(new File(VarStatiche.getCartella_ConfigLoghi(), LoghiImport.Slug(etichetta) + ".png").isFile(),
                    "manca il logo della blockchain " + etichetta);
        }
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

    //====================================================================================
    // Monogrammi (edizione Store)
    //====================================================================================

    @Test
    public void laSiglaDiUnaReteEIlCodiceCheLEtichettaGiaPorta() {
        assertEquals("ARB", LoghiImport.Sigla("Arbitrum (ARB)"));
        assertEquals("ETH", LoghiImport.Sigla("Ethereum (ETH)"));
        //la colonna "Rete" della tabella dei wallet contiene il solo codice: deve dare lo stesso risultato
        assertEquals("ARB", LoghiImport.Sigla("ARB"));
        //oltre le tre lettere si abbrevia: a 19 px la quarta non si legge
        assertEquals("GNO", LoghiImport.Sigla("Gnosis Chain (GNOSIS)"));
    }

    @Test
    public void laSiglaDistingueINomiCheCominciamoAllaStessaManiera() {
        //è il motivo per cui la seconda lettera non è la seconda del nome: sarebbero sei "Bi" identici
        assertEquals("Bn", LoghiImport.Sigla("Binance"));
        assertEquals("Bf", LoghiImport.Sigla("Bitfinex"));
        assertEquals("Bp", LoghiImport.Sigla("Bitpanda"));
        assertEquals("Br", LoghiImport.Sigla("Bitrue"));
        assertEquals("Bt", LoghiImport.Sigla("Bittrex"));
        assertEquals("Cb", LoghiImport.Sigla("Coinbase"));
        assertEquals("Ce", LoghiImport.Sigla("CoinEx"));
    }

    @Test
    public void unNomeGiaInFormaDiSiglaNonDiventaUnaParola() {
        //"Ok" si leggerebbe come un nome diverso da OKX
        assertEquals("OKX", LoghiImport.Sigla("OKX"));
        assertEquals("MEX", LoghiImport.Sigla("MEXC"));
    }

    @Test
    public void iNomiCompostiUsanoLeInizialiDelleParole() {
        assertEquals("YA", LoghiImport.Sigla("Yield App"));
        assertEquals("LL", LoghiImport.Sigla("Ledger Live"));
        assertEquals("CE", LoghiImport.Sigla("Crypto.com Exchange"));
    }

    @Test
    public void leVociSpecialiNonHannoSigla() {
        //separatori e segnaposto degli elenchi: un cerchio con dentro un trattino sarebbe rumore
        assertEquals("", LoghiImport.Sigla("----------"));
        assertEquals("", LoghiImport.Sigla("*Nome Personalizzato*"));
        assertEquals("", LoghiImport.Sigla(null));
    }

    @Test
    public void nellEdizioneCompletaUnLogoMancanteRestaUnSegnaposto() {
        VarStatiche.Edizione = "completa";
        assertEquals(0, AlphaAngolo(LoghiImport.Dammi("voce-senza-logo", "Voce Senza Logo", 24)),
                "il segnaposto trasparente dell'edizione completa non deve diventare un monogramma");
        assertEquals(0, CentroOpaco(LoghiImport.Dammi("voce-senza-logo", "Voce Senza Logo", 24)));
    }

    @Test
    public void nellEdizioneStoreUnLogoMancanteDiventaUnMonogramma() {
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        Icon i = LoghiImport.Dammi("voce-senza-logo", "Voce Senza Logo", 24);
        assertEquals(0, AlphaAngolo(i), "il cerchio non deve arrivare negli angoli del quadrato");
        assertEquals(255, CentroOpaco(i), "senza il file del logo l'edizione Store deve disegnare il cerchio");
    }

    @Test
    public void ancheNellEdizioneStoreIlLogoPresenteVinceSulMonogramma() {
        //un archivio ripristinato dall'edizione completa riporta config/loghi sul disco: quei file
        //vanno disegnati, non coperti dal monogramma (Analisi_API_Terze_Parti.md §2)
        Icon completa = LoghiImport.Dammi("binance", "Binance", 32);
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        assertSame(completa, LoghiImport.Dammi("binance", "Binance", 32),
                "con il file presente le due edizioni devono restituire lo stesso logo");
    }

    @Test
    public void ilMonogrammaSegueIlTema() {
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        Icone.ForzaTemaScuroPerProva(false);
        int chiaro = Centro(LoghiImport.Dammi("voce-senza-logo", "Voce Senza Logo", 24)) & 0xFFFFFF;
        LoghiImport.SvuotaCache();
        Icone.ForzaTemaScuroPerProva(true);
        int scuro = Centro(LoghiImport.Dammi("voce-senza-logo", "Voce Senza Logo", 24)) & 0xFFFFFF;
        assertTrue(chiaro < 0x404040, "sul tema chiaro il cerchio deve essere scuro, era " + Integer.toHexString(chiaro));
        assertTrue(scuro > 0xA0A0A0, "sul tema scuro il cerchio deve essere chiaro, era " + Integer.toHexString(scuro));
    }

    /** @return il colore del pixel al centro dell'icona */
    private static int Centro(Icon icona) {
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(
                icona.getIconWidth(), icona.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        icona.paintIcon(null, g, 0, 0);
        g.dispose();
        //il centro geometrico cade dentro una lettera, che è scavata: si guarda poco sotto il bordo alto
        return im.getRGB(icona.getIconWidth() / 2, Math.max(1, icona.getIconHeight() / 6));
    }

    /** @return 255 se il punto guardato da {@link #Centro(Icon)} è opaco, 0 se è trasparente */
    private static int CentroOpaco(Icon icona) {
        return (Centro(icona) >>> 24) > 127 ? 255 : 0;
    }

    @Test
    public void ilRendererDellaComboDisegnaIlMonogramma() {
        //è la riga passata a mano dentro RenderComboConLogo: se l'etichetta non arrivasse a Dammi, il
        //monogramma non comparirebbe e nessuna delle prove qui sopra se ne accorgerebbe
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        javax.swing.JList<String> lista = new javax.swing.JList<>(new String[]{"Voce Senza Logo"});
        LoghiImport.RenderComboConLogo r = new LoghiImport.RenderComboConLogo();
        java.awt.Component c = r.getListCellRendererComponent(lista, "Voce Senza Logo", 0, false, false);
        Icon i = ((javax.swing.JLabel) c).getIcon();
        assertNotNull(i, "la voce senza logo deve ricevere comunque un'icona");
        assertEquals(255, CentroOpaco(i), "il renderer della combo non ha disegnato il monogramma");
    }
}
