/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author luca.passelli
 */
public class Tabelle {
    static Color verde=new Color (145, 255, 143);
    static Color verdeChiaro=new Color (172, 255, 171);
    static Color rosso=new Color(255, 120, 120);
    static Color rossoChiaro=new Color(255, 133, 133);
    static Color giallo=new Color(255, 255, 156);
    static Color gialloChiaro=new Color(255, 255, 176);
    static Color bianco=new Color(255, 255, 255);
    static Color grigioChiaro=new Color(245, 245, 245);
    static Color arancione=new Color(255, 216, 166);
    static Color arancioneChiaro=new Color(255, 226, 189);
    
    
    public static JTable ColoraRigheTabellaCrypto(final JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Color bg;
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                // modelrow serve per prendere la riga anche se è stata riordinata
                // int modelRow = table.getRowSorter().convertRowIndexToModel(row);

                if (isSelected) {
                    setBackground(table.getSelectionBackground());

                } else if (table.getModel().getValueAt(row, 5).toString().equals("OK")) {

                    setBackground(Color.RED);
                    setForeground(Color.BLACK);
                    bg = (row % 2 == 0 ? verde : verdeChiaro);
                    setBackground(bg);

                } else {
                    bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    setBackground(bg);
                    setForeground(table.getForeground());
                }

                return this;
            }
        });
        return table;
    }
    
    
    
    
    
    
    
}
