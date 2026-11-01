package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Le emoji nel nome di un token (BABYPIG, BSC, ha per simbolo 🐷) devono comparire nella cella: Noto Sans non le ha e il
 * ripiego di sistema, su Linux, dichiara di disegnarle (Noto Color Emoji) ma non lascia nemmeno un pixel. Le disegna
 * Noto Emoji, incluso nel jar ({@link FontApplicazione#PerTestoConEmoji}, {@link Tabelle.CellaConRipiegoFont}).
 */
class CellaConEmojiTest {

    @BeforeAll
    static void registra() {
        FontApplicazione.Registra();
    }

    /** I pixel diversi dallo sfondo della cella, disegnata come la disegna la tabella. */
    private static int pixelScuri(String testo, int allineamento, int larghezza) {
        return Disegna(testo, allineamento, larghezza).pixel;
    }

    private record Prova(int pixel, BufferedImage immagine) {
    }

    private static Prova Disegna(String testo, int allineamento, int larghezza) {
        JTable t = new JTable(new DefaultTableModel(new Object[][]{{testo}}, new String[]{"Token"}));
        t.setFont(FontApplicazione.Font(Font.PLAIN, 12));
        Tabelle.CellaConRipiegoFont cella = new Tabelle.CellaConRipiegoFont();
        Component c = cella.getTableCellRendererComponent(t, testo, false, false, 0, 0);
        cella.setHorizontalAlignment(allineamento);
        cella.setBackground(Color.WHITE);
        cella.setForeground(Color.BLACK);
        cella.setOpaque(true);
        cella.setSize(larghezza, 24);
        BufferedImage img = new BufferedImage(larghezza, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        int n = 0;
        for (int x = 0; x < larghezza; x++) {
            for (int y = 0; y < 24; y++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) n++;
            }
        }
        return new Prova(n, img);
    }

    @Test
    void unaEmojiDaSolaSiDisegna() {
        assertTrue(pixelScuri("🐷", SwingConstants.LEFT, 120) > 30, "la cella con 🐷 è vuota");
    }

    @Test
    void ilTestoMistoDisegnaTestoEEmoji() {
        int solo = pixelScuri("$Gold", SwingConstants.LEFT, 160);
        int misto = pixelScuri("$Gold🍆", SwingConstants.LEFT, 160);
        assertTrue(misto > solo + 30, "l'emoji dopo il testo non si vede: " + solo + " contro " + misto);
        assertTrue(pixelScuri("✅$ARB AIRDROP", SwingConstants.LEFT, 160) > pixelScuri("$ARB AIRDROP", SwingConstants.LEFT, 160) + 30,
                "il dingbat davanti al testo non si vede");
    }

    @Test
    void ilTestoSenzaEmojiNonCambiaStrada() {
        Font f = FontApplicazione.Font(Font.PLAIN, 12);
        assertNull(FontApplicazione.PerTestoConEmoji(f, "Giacenze USDC 1.000,00"));
        assertNull(FontApplicazione.PerTestoConEmoji(f, "Ꮎ ꓔ"), "senza emoji resta il ripiego di sistema di PerTesto");
        assertNotNull(FontApplicazione.PerTestoConEmoji(f, "BABYPIG 🐷"));
    }

    @Test
    void allineamentoATroncamento() {
        //A destra l'emoji sta contro il bordo destro, a sinistra contro il sinistro
        BufferedImage destra = Disegna("🐷", SwingConstants.RIGHT, 120).immagine();
        BufferedImage sinistra = Disegna("🐷", SwingConstants.LEFT, 120).immagine();
        assertTrue(primaColonnaScura(destra) > 60, "a destra l'emoji deve stare nella metà destra");
        assertTrue(primaColonnaScura(sinistra) < 20, "a sinistra l'emoji deve stare vicino al bordo sinistro");
        //Una cella stretta non disegna fuori dai propri bordi: si accorcia coi puntini
        int pixelStretta = pixelScuri("BABYPIG BABYPIG BABYPIG 🐷", SwingConstants.LEFT, 60);
        assertTrue(pixelStretta > 0);
    }

    private static int primaColonnaScura(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) return x;
            }
        }
        return -1;
    }

    /** Il font è nel jar e la sua licenza viaggia con i font. */
    @Test
    void ilFontEmojiENelJar() {
        assertNotNull(FontApplicazione.class.getResource("/Fonts/NotoEmoji.ttf"));
        assertEquals(0x1F437, "🐷".codePointAt(0));
    }
}
