/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;


/**
 *
 * @author lucap
 */
public class CombinedIcon implements Icon {
    private final Icon sortIcon;
    private final Icon filterIcon;

    public CombinedIcon(Icon sortIcon, Icon filterIcon) {
        // Se sortIcon è già un CombinedIcon, estrai l'originale
        if (sortIcon instanceof CombinedIcon) {
            CombinedIcon ci = (CombinedIcon) sortIcon;
            this.sortIcon = ci.sortIcon;
        } else {
            this.sortIcon = sortIcon;
        }
        this.filterIcon = filterIcon;
    }

    @Override
    public int getIconWidth() {
        int w1 = sortIcon != null ? sortIcon.getIconWidth() : 0;
        int w2 = filterIcon != null ? filterIcon.getIconWidth() : 0;
        return w1 + w2;
    }

    @Override
    public int getIconHeight() {
        int h1 = sortIcon != null ? sortIcon.getIconHeight() : 0;
        int h2 = filterIcon != null ? filterIcon.getIconHeight() : 0;
        return Math.max(h1, h2);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int xPos = x;
        if (sortIcon != null) {
            sortIcon.paintIcon(c, g, xPos, y);
            xPos += sortIcon.getIconWidth();
        }
        if (filterIcon != null) {
            filterIcon.paintIcon(c, g, xPos, y);
        }
    }
}
