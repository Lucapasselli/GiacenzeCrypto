/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Point;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
/**
 *
 * @author lucap
 */
public class JTableConTooltipIcone extends JTable {

    public JTableConTooltipIcone() {
        super();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int viewRow = rowAtPoint(p);
        int viewCol = columnAtPoint(p);

        if (viewRow == -1 || viewCol == -1) return null;

        //int modelRow = convertRowIndexToModel(viewRow);
        int modelCol = convertColumnIndexToModel(viewCol);

        Component comp = prepareRenderer(getCellRenderer(viewRow, viewCol), viewRow, viewCol);
        if (comp instanceof JLabel label) {
            Icon icon = label.getIcon();
            if (icon != null) {
                Rectangle cellRect = getCellRect(viewRow, viewCol, false);
                Insets insets = label.getInsets();

                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                int iconX = cellRect.x + insets.left;
                int iconY = cellRect.y + (cellRect.height - iconHeight) / 2;

                if (e.getX() >= iconX && e.getX() <= iconX + iconWidth &&
                    e.getY() >= iconY && e.getY() <= iconY + iconHeight) {
                    
                    return switch (modelCol) {
                        case 5 ->
                            """
<html>
    <b>MOVIMENTO NON CLASSIFICATO</b><br><br>
    Per sistemarlo:<br>
    &nbsp;• Usa la funzione <i><b>'Classificazione Depositi/Prelievi'</b></i><br>
    &nbsp;• Oppure clicca col tasto destro e seleziona <i><b>'Classifica Movimento'</b></i>
</html>
""";
                        case 15 -> """
<html>
    <b>TRANSAZIONE SENZA PREZZO</b><br><br>
    Per sistemarlo:<br>
    &nbsp;• Premi su <i><b>'Modifica Movimento'</b></i> e assegna un prezzo.<br>
    &nbsp;• Oppure clicca col tasto destro e seleziona <i><b>'Modifica Prezzo'</b></i><br>
    &nbsp;• Oppure classifica il movimento come Scam dalla funzione <i><b>'Classificazione Depositi/Prelievi'</b></i>
</html>
""";
                        case 19 ->  """
<html>
    <b>MOVIMENTO IN INGRESSO MANCANTE</b>
</html>
""";
                        default -> "";
                    };
                }
            }
        }

        return null;
    }
}
