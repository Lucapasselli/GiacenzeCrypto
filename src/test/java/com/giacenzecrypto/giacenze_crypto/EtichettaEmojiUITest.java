package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Le emoji nelle etichette, nei renderer e nei dialoghi ({@link EtichettaEmojiUI}): testo semplice, disabilitato e HTML.
 * Il caso HTML che ha richiesto più lavoro: {@code AppDialog} chiama {@code setFont} subito dopo aver creato l'etichetta e
 * FlatLaf rifà la vista dal testo originale, cancellando l'avvolgimento fatto dopo il cambio di testo.
 */
class EtichettaEmojiUITest {

    private static LookAndFeel precedente;

    @BeforeAll
    static void installa() throws Exception {
        FontApplicazione.Registra();
        precedente = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        UIManager.put("LabelUI", EtichettaEmojiUI.class.getName());
    }

    @AfterAll
    static void ripristina() throws Exception {
        UIManager.put("LabelUI", null);
        if (precedente != null) UIManager.setLookAndFeel(precedente);
    }

    private static int pixelScuri(JLabel l, int larghezza) {
        l.setFont(FontApplicazione.Font(Font.PLAIN, 14));
        l.setSize(larghezza, 28);
        BufferedImage img = new BufferedImage(larghezza, 28, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, larghezza, 28);
        l.paint(g);
        g.dispose();
        int n = 0;
        for (int x = 0; x < larghezza; x++) {
            for (int y = 0; y < 28; y++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) n++;
            }
        }
        return n;
    }

    @Test
    void ilTestoSempliceDisegnaLeEmoji() {
        assertTrue(pixelScuri(new JLabel("🐷"), 100) > 30, "l'etichetta con 🐷 è vuota");
        int solo = pixelScuri(new JLabel("$Gold"), 160);
        assertTrue(pixelScuri(new JLabel("$Gold🍆"), 160) > solo + 30, "l'emoji dopo il testo non si vede");
    }

    @Test
    void ancheDisabilitata() {
        JLabel l = new JLabel("🐷");
        l.setEnabled(false);
        assertTrue(pixelScuri(l, 100) > 30);
    }

    @Test
    void lHtmlConSetFontDopoLaCreazioneDisegnaLeEmoji() {
        //Come AppDialog: etichetta creata col testo, font impostato dopo
        String html = "<html><div style='width:150px;font-weight:600;'>Il token <b>BABYPIG 🐷</b> e $Gold🍆</div></html>";
        String senza = "<html><div style='width:150px;font-weight:600;'>Il token <b>BABYPIG </b> e $Gold</div></html>";
        int a = pixelScuri(new JLabel(senza), 200);
        int b = pixelScuri(new JLabel(html), 200);
        assertTrue(b > a + 40, "le emoji dell'HTML non si vedono: " + a + " contro " + b);
    }

    @Test
    void laLarghezzaPreferitaTieneContoDellaEmoji() {
        JLabel l = new JLabel("BABYPIG 🐷");
        l.setFont(FontApplicazione.Font(Font.PLAIN, 14));
        Dimension d = l.getPreferredSize();
        float vera = FontApplicazione.Larghezza(FontApplicazione.PerTestoConEmoji(l.getFont(), l.getText()));
        assertTrue(d.width >= vera, "preferita " + d.width + " < disegnata " + vera);
    }

    @Test
    void leEtichetteSenzaEmojiNonCambiano() {
        JLabel l = new JLabel("Giacenze USDC 1.000,00");
        l.setFont(FontApplicazione.Font(Font.PLAIN, 14));
        int con = l.getPreferredSize().width;
        assertEquals(l.getFontMetrics(l.getFont()).stringWidth(l.getText()), con - l.getInsets().left - l.getInsets().right);
    }

    @Test
    void htmlConEmojiToccaSoloIlTestoFuoriDaiTag() {
        assertEquals("<html><b>solo testo</b></html>", FontApplicazione.HtmlConEmoji("<html><b>solo testo</b></html>"));
        String r = FontApplicazione.HtmlConEmoji("<html><div title='🐷'>x 🐷</div></html>");
        assertTrue(r.contains("title='🐷'"), "l'attributo non va toccato: " + r);
        assertTrue(r.contains("Noto Emoji") && r.contains("x <span"), r);
        //Il selettore di variante resta dentro lo span, i tag restano ben annidati
        String v = FontApplicazione.HtmlConEmoji("<html><b>❤️🔥 ok</b></html>");
        assertEquals(1, v.split("<span", -1).length - 1, v);
        assertTrue(v.contains("</span> ok</b>"), v);
    }
}
