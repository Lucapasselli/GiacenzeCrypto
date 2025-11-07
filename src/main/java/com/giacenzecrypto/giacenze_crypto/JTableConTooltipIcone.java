/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Point;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
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
                String ID = this.getModel().getValueAt(viewRow, 0).toString();
                String mov[]=Principale.MappaCryptoWallet.get(ID);
                String MonetaU = mov[8];
                String QtaMancanteLiFo="";
                String ValRimanenze="";
                if (Funzioni.Funzioni_isNumeric(mov[10], false))
                {
                    BigDecimal QtaU = new BigDecimal(mov[10]);
                    BigDecimal ValTrans=new BigDecimal(mov[15]);
                    QtaMancanteLiFo=RecuperaQtaMancante(ID);
                    if(QtaMancanteLiFo!=null&&Funzioni.Funzioni_isNumeric(QtaMancanteLiFo, false)&&QtaU.compareTo(BigDecimal.ZERO)!=0)
                    {
                        ValRimanenze=ValTrans.divide(QtaU, 10, RoundingMode.HALF_UP).multiply(new BigDecimal(QtaMancanteLiFo)).setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
                    }
                }
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
    Per sistemarla:<br>
    &nbsp;• Premi su <i><b>'Modifica Movimento'</b></i> e assegna un prezzo.<br>
    &nbsp;• Oppure clicca col tasto destro e seleziona <i><b>'Modifica Prezzo'</b></i><br>
    &nbsp;• Oppure classifica il movimento come Scam dalla funzione <i><b>'Classificazione Depositi/Prelievi'</b></i>
</html>
""";
                        case 19 ->  String.format("""
<html>
    <b>MOVIMENTO IN INGRESSO MANCANTE</b><br><br>
    Mancano acquisti per <b>%s %s</b> corrispondenti ad un valore di <b>€ %s</b><br><br>                                            
    Per sistemarlo:<br>
    &nbsp;• Utilizzare l'apposita funzione <i><b>'Verifica Saldi Negativi'</b></i> presente nella tab <i><b>'Analisi Crypto'</b></i>.<br>
</html>
""",QtaMancanteLiFo,MonetaU,ValRimanenze);
                        default -> "";
                    };
                }
            }
        }

        return null;
    }
    
    private String RecuperaQtaMancante(String ID){
        Calcoli_PlusvalenzeNew.LifoXID lifoID=Calcoli_PlusvalenzeNew.getIDLiFo(ID);
        if (lifoID==null)return null;
        ArrayDeque<String[]> StackUscito=lifoID.Get_CryptoStackUscito();
        ArrayDeque<String[]> stack=StackUscito.clone();
        //System.out.println(stack.size());
        while (!stack.isEmpty()) {
            String[] ultimoRecupero = stack.pop();
            String mov[]=Principale.MappaCryptoWallet.get(ultimoRecupero[3]);
            if (mov==null){
                return ultimoRecupero[1];
            }
        }
        return "";
    }
}
