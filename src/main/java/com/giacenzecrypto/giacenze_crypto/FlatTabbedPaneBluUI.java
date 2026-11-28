package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

/**
 * Le schede del tema <i>Blu</i> come pastiglie staccate: ognuna è un rettangolo arrotondato con uno spazio fra una e l'altra, invece di
 * un'unica fascia blu. FlatLaf non ha una proprietà per la distanza fra le schede (ogni scheda riempie tutto il suo rettangolo), quindi si
 * ridisegna lo sfondo (e l'indicatore della scheda scelta) accorciato del vuoto su ogni lato. Misure, testi e comportamento restano quelli di
 * {@link FlatTabbedPaneUI}.
 *
 * <p>Lo installa solo {@code Temi/FlatTemaBluLaf.properties} ({@code TabbedPaneUI}), quindi Chiaro e Scuro non lo vedono. Il vuoto è la chiave
 * {@code Giacenze.tabVuoto} (pixel a scala 100%, tre se manca), il raggio è {@code TabbedPane.tabArc}.</p>
 */
public class FlatTabbedPaneBluUI extends FlatTabbedPaneUI {

    private static final int VUOTO_PREDEFINITO = 3;

    public static ComponentUI createUI(JComponent c) {
        return new FlatTabbedPaneBluUI();
    }

    private int vuoto() {
        Object v = UIManager.get("Giacenze.tabVuoto");
        return UIScale.scale(v instanceof Integer i ? i : VUOTO_PREDEFINITO);
    }

    /**
     * Lo sfondo della fascia delle schede lo riempie FlatLaf con il colore del pannello ({@code TabbedPane.background}), che nel tema Blu è
     * quello della pagina: così fra una pastiglia e l'altra si vede la pagina. Il blu notte delle schede non scelte è
     * {@code Giacenze.tabSfondo}; scelta e passaggio del mouse hanno i loro colori come sempre.
     */
    @Override
    protected Color getTabBackground(int tabPlacement, int tabIndex, boolean isSelected) {
        Color colore = super.getTabBackground(tabPlacement, tabIndex, isSelected);
        if (!isSelected && colore != null && colore.equals(tabPane.getBackground())) {
            Color sfondo = UIManager.getColor("Giacenze.tabSfondo");
            if (sfondo != null) return sfondo;
        }
        return colore;
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Color colore = getTabBackground(tabPlacement, tabIndex, isSelected);
        if (colore == null) return;
        int v = vuoto();
        boolean orizzontale = isHorizontalTabPlacement(tabPlacement);
        int rx = orizzontale ? x + v : x;
        int ry = orizzontale ? y : y + v;
        int rw = orizzontale ? w - 2 * v : w;
        int rh = orizzontale ? h : h - 2 * v;
        if (rw <= 0 || rh <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colore);
            int arco = UIScale.scale(Math.max(tabArc, 0));
            g2.fillRoundRect(rx, ry, rw, rh, arco, arco);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabSelection(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h) {
        int v = vuoto();
        if (isHorizontalTabPlacement(tabPlacement)) super.paintTabSelection(g, tabPlacement, tabIndex, x + v, y, w - 2 * v, h);
        else super.paintTabSelection(g, tabPlacement, tabIndex, x, y + v, w, h - 2 * v);
    }
}
