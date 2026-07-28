package com.giacenzecrypto.giacenze_crypto;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Genera le immagini dello splash nativo AWT ({@code src/main/resources/splash*.png}).
 * <p>
 * Non fa parte della build né dell'applicazione: è uno strumento da lanciare a mano, e solo quando cambia
 * {@code logo.png} oppure la grafica dello splash in {@link SplashAvvio}. Disegna la <em>stessa</em>
 * gerarchia di pannelli usata a runtime ({@link SplashAvvio#creaContenuto}), quindi l'immagine nativa non
 * può divergere dalla finestra Swing che la sostituisce durante l'avvio.
 * <p>
 * Uso: da NetBeans basta "Run File" su questa classe. Da riga di comando, nella root del progetto (serve
 * solo un JDK, funziona su qualsiasi piattaforma; il rendering è offscreen quindi gira anche in ambiente
 * headless, senza display):
 * <pre>
 * mvn -q compile
 * java -cp target/classes com.giacenzecrypto.giacenze_crypto.GeneraSplash src/main/resources
 * </pre>
 * Rigenera {@code splash.png} e le varianti HiDPI {@code splash.scale-150.png} / {@code splash.scale-200.png}.
 * Il logo viene letto da {@code logo.png} nella root; se cambia va anche ricopiato in
 * {@code src/main/resources/logo.png}, che è il fallback usato da {@link SplashAvvio} quando il logo non è
 * presente nella cartella delle risorse accanto al jar.
 */
public final class GeneraSplash {

    private GeneraSplash() {
    }

    /**
     * @param args {@code [0]} cartella di destinazione (default {@code src/main/resources}),
     *             {@code [1]} percorso del logo (default {@code logo.png})
     * @throws Exception se il logo non è leggibile o le immagini non sono scrivibili
     */
    public static void main(String[] args) throws Exception {
        File destinazione = new File(args.length > 0 ? args[0] : "src/main/resources");
        File logo = new File(args.length > 1 ? args[1] : "logo.png");

        BufferedImage img = ImageIO.read(logo);
        if (img == null) {
            throw new IllegalArgumentException("Logo non leggibile: " + logo.getAbsolutePath());
        }

        //La variante 1x e le due per i monitor HiDPI: il suffisso .scale-<percentuale> è la convenzione
        //con cui la JVM sceglie l'immagine dello splash in base al fattore di scala dello schermo.
        scrivi(img, 1.0, new File(destinazione, "splash.png"));
        scrivi(img, 1.5, new File(destinazione, "splash.scale-150.png"));
        scrivi(img, 2.0, new File(destinazione, "splash.scale-200.png"));
    }

    /** Disegna il contenuto dello splash alla scala richiesta e lo salva come PNG con trasparenza. */
    private static void scrivi(BufferedImage logo, double scala, File destinazione) throws Exception {
        JPanel root = SplashAvvio.creaContenuto(logo, false).root;

        Dimension dim = root.getPreferredSize();
        root.setSize(dim);
        disponi(root);

        //Il pannello viene disegnato a piena opacità su un'immagine di appoggio; l'opacità della finestra
        //(JWindow.setOpacity, che offscreen non ha equivalente) viene poi applicata una sola volta
        //sull'intero risultato, altrimenti i bordi arrotondati verrebbero scuriti due volte.
        BufferedImage piena = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gp = piena.createGraphics();
        try {
            gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            root.paint(gp);
        } finally {
            gp.dispose();
        }

        int w = (int) Math.round(dim.width * scala);
        int h = (int) Math.round(dim.height * scala);

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SplashAvvio.OPACITA));
            g.drawImage(piena, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }

        ImageIO.write(out, "png", destinazione);
        System.out.println("Creato " + destinazione.getPath() + " (" + w + " x " + h + ")");
    }

    /**
     * Dispone ricorsivamente la gerarchia dei pannelli. Serve perché il contenuto non viene mai inserito in
     * una finestra: {@code pack()} lancerebbe {@code HeadlessException} e renderebbe lo strumento
     * inutilizzabile proprio dove serve di più (build headless, sviluppo da Linux senza display). Senza la
     * discesa ricorsiva i figli dei contenitori interni resterebbero a dimensione zero.
     */
    private static void disponi(Container contenitore) {
        contenitore.doLayout();
        for (Component figlio : contenitore.getComponents()) {
            if (figlio instanceof Container) {
                disponi((Container) figlio);
            }
        }
    }
}
