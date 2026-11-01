package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.ui.FlatLabelUI;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.text.AttributedString;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;

/**
 * Il {@code LabelUI} dell'applicazione: quello di FlatLaf più le emoji. Ogni {@link JLabel} lo usa, quindi anche i renderer di
 * combo, liste e alberi (sono etichette) e i messaggi dei dialoghi ({@code AppDialog}). Noto Sans non ha le emoji e il
 * ripiego di sistema, su Linux, le dichiara disegnabili senza disegnarle ({@link FontApplicazione#PerTestoConEmoji}), quindi
 * qui il testo si disegna a tratti, ciascuno col suo font. Due strade, perché una etichetta è disegnata in due modi:
 * <ul>
 * <li><b>testo semplice</b>: {@code paintEnabledText}/{@code paintDisabledText} disegnano i tratti, e la dimensione preferita
 * cresce della differenza tra la larghezza vera e quella calcolata coi metrici di Noto Sans (il quadratino è più stretto);</li>
 * <li><b>HTML</b>: la vista di Swing sceglie il font per famiglia, quindi le emoji si avvolgono in uno {@code span} col font
 * emoji ({@link FontApplicazione#HtmlConEmoji}) e la vista si rifà. FlatLaf rifà la vista a ogni cambio di testo, font o
 * colore: lo si fa dopo di lui, negli stessi eventi.</li>
 * </ul>
 * Le etichette senza emoji non cambiano di un pixel: si esce al primo controllo. Non coprono i campi di testo né i pulsanti.
 */
public class EtichettaEmojiUI extends FlatLabelUI {

    private static final EtichettaEmojiUI CONDIVISO = new EtichettaEmojiUI();

    public static ComponentUI createUI(JComponent c) {
        return CONDIVISO;
    }

    protected EtichettaEmojiUI() {
        super(true);
    }

    @Override
    protected void installComponents(JLabel c) {
        super.installComponents(c);
        RifaiHtml(c);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        String nome = e.getPropertyName();
        if (("text".equals(nome) || "font".equals(nome) || "foreground".equals(nome)) && e.getSource() instanceof JLabel) {
            RifaiHtml((JLabel) e.getSource());
        }
    }

    /** Chiave della vista HTML già controllata (con o senza emoji): se la vista corrente è un'altra, la ha rifatta qualcuno dopo di noi. */
    private static final String VISTA_CONTROLLATA = "EtichettaEmojiUI.vistaControllata";

    private static void RifaiHtml(JLabel l) {
        String testo = l.getText();
        if (testo == null || !BasicHTML.isHTMLString(testo)) return;
        String conEmoji = FontApplicazione.HtmlConEmoji(testo);
        if (!conEmoji.equals(testo)) BasicHTML.updateRenderer(l, conEmoji);
        l.putClientProperty(VISTA_CONTROLLATA, l.getClientProperty(BasicHTML.propertyKey));
    }

    /**
     * Gli eventi non bastano: FlatLaf rifà la vista dal testo originale anche dopo il nostro {@code propertyChange} (a ogni
     * {@code setFont}, che {@code AppDialog} chiama subito dopo aver creato l'etichetta). Prima di misurare o disegnare si
     * verifica quindi che la vista sia quella già controllata, e se no la si rifà.
     */
    private static void AssicuraVista(JLabel l) {
        Object vista = l.getClientProperty(BasicHTML.propertyKey);
        if (vista != null && vista != l.getClientProperty(VISTA_CONTROLLATA)) RifaiHtml(l);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (c instanceof JLabel) AssicuraVista((JLabel) c);
        super.paint(g, c);
    }

    /** I tratti con cui disegnare il testo semplice dell'etichetta, {@code null} se non servono (HTML, senza emoji). */
    private static AttributedString Tratti(JLabel l, String testo) {
        if (testo == null || BasicHTML.isHTMLString(testo)) return null;
        return FontApplicazione.PerTestoConEmoji(l.getFont(), testo);
    }

    @Override
    protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        AttributedString tratti = Tratti(l, s);
        if (tratti == null) {
            super.paintEnabledText(l, g, s, textX, textY);
            return;
        }
        g.setColor(l.getForeground());
        FontApplicazione.Disegna(g, tratti, textX, textY);
    }

    @Override
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        AttributedString tratti = Tratti(l, s);
        if (tratti == null) {
            super.paintDisabledText(l, g, s, textX, textY);
            return;
        }
        g.setColor(disabledForeground);
        FontApplicazione.Disegna(g, tratti, textX, textY);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (c instanceof JLabel) AssicuraVista((JLabel) c);
        Dimension d = super.getPreferredSize(c);
        if (d == null || !(c instanceof JLabel)) return d;
        JLabel l = (JLabel) c;
        AttributedString tratti = Tratti(l, l.getText());
        if (tratti == null) return d;
        FontMetrics fm = l.getFontMetrics(l.getFont());
        int in_piu = (int) Math.ceil(FontApplicazione.Larghezza(tratti) - fm.stringWidth(l.getText()));
        return in_piu > 0 ? new Dimension(d.width + in_piu, d.height) : d;
    }
}
