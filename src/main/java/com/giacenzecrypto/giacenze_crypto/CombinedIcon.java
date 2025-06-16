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
    private final Icon icon1;
    private final Icon icon2;

    public CombinedIcon(Icon icon1, Icon icon2) {
        this.icon1 = icon1;
        this.icon2 = icon2;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int yOffset1 = (getIconHeight() - icon1.getIconHeight()) / 2;
        int yOffset2 = (getIconHeight() - icon2.getIconHeight()) / 2;

        icon1.paintIcon(c, g, x, y + yOffset1);
        icon2.paintIcon(c, g, x + icon1.getIconWidth(), y + yOffset2);
    }

    @Override
    public int getIconWidth() {
        return icon1.getIconWidth() + icon2.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return Math.max(icon1.getIconHeight(), icon2.getIconHeight());
    }
}    
