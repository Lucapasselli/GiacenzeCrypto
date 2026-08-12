package com.giacenzecrypto.giacenze_crypto;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Genera le immagini dello splash nativo AWT ({@code src/main/resources/splash*.png}).
 * <p>
 * Non fa parte della build né dell'applicazione: è uno strumento da lanciare a mano, e solo quando cambia
 * {@code logo.png} oppure la grafica dello splash in {@link SplashAvvio}. Disegna con la <em>stessa</em>
 * routine usata a runtime ({@link SplashAvvio#disegnaContenuto}), quindi l'immagine nativa non può
 * divergere dalla finestra Swing che la sostituisce durante l'avvio. La barra di avanzamento viene
 * disegnata vuota: è esattamente il fotogramma in cui lo splash nativo passa il testimone alla finestra.
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

    /**
     * Disegna il contenuto dello splash alla scala richiesta e lo salva come PNG con trasparenza.
     * <p>Il disegno avviene <b>direttamente alla risoluzione finale</b> e non ingrandendo la versione 1x:
     * a runtime la finestra fa lo stesso (vedi il fattore di scala in {@link SplashAvvio}), e una variante
     * HiDPI interpolata si vedrebbe come uno scatto di nitidezza nel passaggio dallo splash nativo alla
     * finestra Swing.
     */
    private static void scrivi(BufferedImage logo, double scala, File destinazione) throws Exception {
        int w = (int) Math.round(SplashAvvio.LARGHEZZA * scala);
        int h = (int) Math.round(SplashAvvio.ALTEZZA * scala);

        //Il contenuto viene disegnato a piena opacità su un'immagine di appoggio; l'opacità della finestra
        //(JWindow.setOpacity, che offscreen non ha equivalente) viene poi applicata una sola volta
        //sull'intero risultato, altrimenti i bordi arrotondati verrebbero scuriti due volte.
        BufferedImage piena = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gp = piena.createGraphics();
        try {
            gp.scale(scala, scala);
            SplashAvvio.disegnaContenuto(gp, logo, 0, "Caricamento in corso...");
        } finally {
            gp.dispose();
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SplashAvvio.OPACITA));
            g.drawImage(piena, 0, 0, null);
        } finally {
            g.dispose();
        }

        ImageIO.write(out, "png", destinazione);
        System.out.println("Creato " + destinazione.getPath() + " (" + w + " x " + h + ")");
    }
}
