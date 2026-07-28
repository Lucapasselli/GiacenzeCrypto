package com.giacenzecrypto.giacenze_crypto;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Splash screen di avvio dell'applicazione.
 * <p>
 * È volutamente una classe autonoma e senza dipendenze dal resto del programma: viene invocata come
 * primissima operazione di {@link Giacenze_Crypto#main(String[])}, prima dell'apertura del database e
 * prima dell'installazione del Look&amp;Feel, in modo da mostrare qualcosa a video il prima possibile.
 * Per lo stesso motivo non tocca {@link Principale} (caricare quella classe significherebbe caricare
 * anche tutte le sue dipendenze prima di disegnare il primo pixel).
 * <p>
 * A monte di questa finestra c'è lo splash nativo AWT ({@code SplashScreen-Image} nel manifest del jar),
 * che viene disegnato dalla JVM prima ancora del caricamento della classe main: {@link #mostra()} lo
 * chiude solo dopo che questa finestra è visibile, così la transizione non lascia lo schermo vuoto.
 */
public final class SplashAvvio {

    /** Raggio degli angoli arrotondati della finestra. */
    static final int ARCO = 24;
    /** Opacità della finestra: applicata da Swing a runtime, va replicata a mano nel rendering offscreen. */
    static final float OPACITA = 0.97f;
    /** Font usato per il testo della barra di caricamento: esplicito perché il L&amp;F non è ancora installato. */
    private static final Font FONT_BARRA = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    private static JWindow finestra;
    private static JPanel barra;
    private static String testo = "Caricamento in corso...";

    private SplashAvvio() {
    }

    /**
     * Costruisce e mostra lo splash screen, quindi chiude lo splash nativo della JVM.
     * Può essere chiamato da qualunque thread; se chiamato fuori dall'EDT attende che la finestra sia
     * effettivamente visibile. Qualsiasi errore viene ignorato: uno splash non funzionante non deve
     * mai impedire l'avvio del programma.
     */
    public static void mostra() {
        esegui(() -> {
            if (finestra != null) {
                return;
            }
            //Il riferimento allo splash nativo va preso PRIMA di mostrare la finestra Swing: AWT chiude
            //da solo lo splash nativo quando compare la prima finestra, e da quel momento
            //getSplashScreen() restituisce null.
            SplashScreen nativo = splashNativo();
            System.out.println("Splash nativo: " + (nativo != null ? "presente" : "assente"));
            costruisci();
            chiudiSplashNativo(nativo);
        });
    }

    /**
     * Aggiorna il testo mostrato nella barra di caricamento.
     * @param nuovoTesto testo da mostrare
     */
    public static void aggiornaTesto(String nuovoTesto) {
        esegui(() -> {
            testo = nuovoTesto;
            if (barra != null) {
                barra.repaint();
            }
        });
    }

    /**
     * Chiude lo splash screen. Può essere chiamato più volte e da qualunque thread; va invocato prima
     * di qualsiasi dialogo di errore in fase di avvio, altrimenti lo splash resterebbe sospeso sopra
     * la finestra di dialogo.
     */
    public static void chiudi() {
        esegui(() -> {
            if (finestra != null) {
                finestra.setVisible(false);
                finestra.dispose();
                finestra = null;
                barra = null;
            }
            //Se lo splash Swing non è mai stato creato (immagine mancante) lo splash nativo sarebbe
            //rimasto a video: lo chiudo comunque.
            chiudiSplashNativo(splashNativo());
        });
    }

    /** Esegue l'operazione sull'EDT, in modo sincrono e ignorando ogni errore. */
    private static void esegui(Runnable operazione) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                operazione.run();
            } else {
                SwingUtilities.invokeAndWait(operazione);
            }
        } catch (Exception ex) {
            System.err.println("Splash screen non disponibile: " + ex);
        } catch (Error err) {
            System.err.println("Splash screen non disponibile: " + err);
        }
    }

    /** @return lo splash nativo della JVM, o {@code null} se non è stato mostrato (es. avvio da IDE, senza jar). */
    private static SplashScreen splashNativo() {
        try {
            return SplashScreen.getSplashScreen();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Chiude lo splash nativo della JVM, se presente e non già chiuso da AWT. */
    private static void chiudiSplashNativo(SplashScreen nativo) {
        try {
            if (nativo != null && nativo.isVisible()) {
                nativo.close();
            }
        } catch (Exception ex) {
            //Splash nativo già chiuso da AWT alla comparsa della prima finestra: irrilevante.
        }
    }

    /** Contenuto grafico dello splash: il pannello radice e, al suo interno, la barra di caricamento. */
    static final class Contenuto {
        final JPanel root;
        final JPanel barra;

        Contenuto(JPanel root, JPanel barra) {
            this.root = root;
            this.barra = barra;
        }
    }

    /**
     * Costruisce il contenuto grafico dello splash. È l'unica definizione di questa grafica: la usano sia
     * la finestra mostrata a runtime sia {@code tools/GeneraSplash.java}, che da qui ricava l'immagine
     * dello splash nativo AWT. Tenerla in un solo punto è ciò che garantisce che le due non divergano e
     * che il passaggio dallo splash nativo a questa finestra resti impercettibile.
     *
     * @param img logo da disegnare
     * @param animato se {@code true} avvia il timer di dissolvenza del logo; se {@code false} il logo resta
     *                all'alpha iniziale {@code 1.0}, cioè il fotogramma esatto in cui lo splash nativo
     *                passa il testimone a questa finestra
     * @return il pannello radice e la barra di caricamento
     */
    static Contenuto creaContenuto(BufferedImage img, boolean animato) {
        final Color sfondo = new Color(10, 10, 10, 225);

        JPanel root = new JPanel(new BorderLayout()) {
            /** Disegna lo sfondo semitrasparente con bordi arrotondati dello splash screen. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(sfondo);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARCO, ARCO);

                    g2.setColor(new Color(255, 255, 255, 55));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARCO, ARCO);
                } finally {
                    g2.dispose();
                }
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel imagePanel = new JPanel() {
            private float alpha = 1.0f;
            private boolean fadingOut = true;

            {
                setOpaque(false);

                //Con animato==false (generazione dell'immagine dello splash nativo) niente timer:
                //l'alpha resta fermo al valore iniziale.
                if (animato) {
                    Timer timerAnimazione = new Timer(80, e -> {
                        if (!isShowing()) {
                            ((Timer) e.getSource()).stop();
                            return;
                        }

                        if (fadingOut) {
                            alpha -= 0.025f;
                            if (alpha <= 0.72f) {
                                alpha = 0.72f;
                                fadingOut = false;
                            }
                        } else {
                            alpha += 0.025f;
                            if (alpha >= 1.0f) {
                                alpha = 1.0f;
                                fadingOut = true;
                            }
                        }

                        repaint();
                    });
                    timerAnimazione.setCoalesce(true);
                    timerAnimazione.start();
                }
            }

            /** Dimensione fissa del pannello che ospita il logo animato. */
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(300, 280);
            }

            /** Disegna il logo centrato e ridimensionato, con dissolvenza (alpha) animata dal timer. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int panelW = getWidth();
                    int panelH = getHeight();

                    int imgW = img.getWidth();
                    int imgH = img.getHeight();

                    double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
                    int drawW = (int) Math.round(imgW * scale);
                    int drawH = (int) Math.round(imgH * scale);

                    int x = (panelW - drawW) / 2;
                    int y = (panelH - drawH) / 2;

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.drawImage(img, x, y, drawW, drawH, this);
                } finally {
                    g2.dispose();
                }
            }
        };

        JPanel loadingBar = new JPanel() {
            /** Dimensione fissa della barra di caricamento dello splash screen. */
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(220, 30);
            }

            /** Disegna lo sfondo arrotondato e il testo della barra di caricamento dello splash screen. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();

                    g2.setColor(new Color(30, 30, 30, 210));
                    g2.fillRoundRect(0, 0, w, h, 14, 14);

                    g2.setColor(new Color(255, 255, 255, 45));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                    g2.setColor(Color.WHITE);
                    g2.setFont(FONT_BARRA);

                    String text = testo;
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (w - fm.stringWidth(text)) / 2;
                    int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

                    g2.drawString(text, textX, textY);
                } finally {
                    g2.dispose();
                }
            }
        };
        loadingBar.setOpaque(false);

        JPanel barraWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        barraWrapper.setOpaque(false);
        barraWrapper.add(loadingBar);

        root.add(imagePanel, BorderLayout.CENTER);
        root.add(barraWrapper, BorderLayout.SOUTH);

        return new Contenuto(root, loadingBar);
    }

    /** Crea la finestra dello splash. Da chiamare solo sull'EDT. */
    private static void costruisci() {
        BufferedImage img = leggiLogo();
        if (img == null) {
            return;
        }

        Contenuto contenuto = creaContenuto(img, true);

        JWindow splash = new JWindow();
        splash.setContentPane(contenuto.root);
        splash.pack();

        splash.setShape(new java.awt.geom.RoundRectangle2D.Double(
                0, 0, splash.getWidth(), splash.getHeight(), ARCO, ARCO));

        try {
            splash.setOpacity(OPACITA);
        } catch (Exception ex) {
            //Trasparenza non supportata dalla piattaforma: lo splash resta comunque utilizzabile.
        }

        //NON usare setLocationRelativeTo(null): quello centra sull'area di lavoro, cioè esclude la barra
        //delle applicazioni, mentre AWT centra lo splash nativo sullo schermo intero. La differenza è
        //metà altezza della barra (24px su una barra da 48) e si vedrebbe come uno scatto verticale nel
        //momento in cui questa finestra sostituisce lo splash nativo.
        try {
            Rectangle schermo = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            splash.setLocation(
                    schermo.x + (schermo.width - splash.getWidth()) / 2,
                    schermo.y + (schermo.height - splash.getHeight()) / 2);
        } catch (Exception ex) {
            splash.setLocationRelativeTo(null);
        }

        splash.setVisible(true);

        finestra = splash;
        barra = contenuto.barra;
    }

    /**
     * Legge il logo dalla cartella delle risorse; se non è presente ripiega sulla copia inclusa nel jar
     * (la stessa usata dallo splash nativo), così lo splash funziona anche prima che i percorsi siano
     * completamente configurati.
     */
    private static BufferedImage leggiLogo() {
        try {
            File file = new File(VarStatiche.getPathRisorse() + "logo.png");
            if (file.isFile()) {
                return ImageIO.read(file);
            }
        } catch (Exception ex) {
            System.err.println("Logo dello splash non leggibile dalle risorse: " + ex);
        }

        try (InputStream in = SplashAvvio.class.getResourceAsStream("/logo.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ex) {
            System.err.println("Logo dello splash non leggibile dal jar: " + ex);
        }

        return null;
    }
}
