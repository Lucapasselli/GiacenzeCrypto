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
    static Color verdeScuro=new Color (23, 114, 69);
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
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());
                    c.revalidate();
                }
                else if (col==3 && value.toString().toLowerCase().contains("deposito")) {
                       // if (value.toString().equalsIgnoreCase("da calcolare"))
                         //   {
                   // c.setBackground(MappaColori.get(row));
                    setBackground(bg);
                    setForeground(verdeScuro);
                   // }

                } 
                else if (col==3 && value.toString().toLowerCase().contains("prelievo")) {
                       // if (value.toString().equalsIgnoreCase("da calcolare"))
                         //   {
                   // c.setBackground(MappaColori.get(row));
                    setBackground(bg);
                    setForeground(Color.RED);
                   // }

                } 

                else if (value!=null && col==10 && value.toString().equalsIgnoreCase("da calcolare")) {
                       // if (value.toString().equalsIgnoreCase("da calcolare"))
                         //   {
                   // c.setBackground(MappaColori.get(row));
                    setBackground(arancione);
                    setForeground(table.getForeground());
                   // }

                } else if (value!=null && col==11 && value.toString().equalsIgnoreCase("da calcolare")) { 
                  //  c.setBackground(MappaColori.get(row));
                    setBackground(arancione);
                    setForeground(table.getForeground());
                   // c.setBackground(MappaColori.get(row));
                }
                else if (value!=null && col==11 && value.toString().trim().contains("-")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    c.setBackground(bg);
                    setForeground(Color.RED);
                } 
                else if (value!=null && col==11 && !value.toString().trim().equalsIgnoreCase("0.00")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    c.setBackground(bg);
                    setForeground(verdeScuro);
                }   
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    setForeground(Color.black);
                  c.setBackground(bg);
                  

                }

              // riga=row;
                return this;
            }
        });
        return table;
    }
    

        public static JTable ColoraRigheTabella0GiacenzeaData(final JTable table) {

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());

                }
                else if (table.getModel().getColumnCount()>3 && table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(row), 4).toString().contains("-")) {
                    c.setBackground(bg);
                    c.setForeground(Color.RED);
                }
                else {
                    setForeground(Color.black);
                  c.setBackground(bg);
                  

                }
                return this;
            }
        });
        return table;
    }

    
        public static JTable ColoraRigheTabella1GiacenzeaData(final JTable table) {

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected&&col!=7) {

                    c.setBackground(table.getSelectionBackground());
                    //c.revalidate();
                }
            else if (col==7 && !value.toString().toLowerCase().contains("-")) {
                    setForeground(Color.black);
                  c.setBackground(bg);
                }
            else if (col==7 && value.toString().toLowerCase().contains("-")) {
                    setForeground(Color.black);
                  c.setBackground(Color.RED);
                }
            else if (table.getModel().getColumnCount()>4 && !table.getModel().getValueAt(row, 5).toString().contains("-")) {
                    setBackground(bg);
                    setForeground(verdeScuro);
                } 
                else if (table.getModel().getColumnCount()>4 && table.getModel().getValueAt(row, 5).toString().contains("-")) {
                    c.setBackground(bg);
                    c.setForeground(Color.RED);
                }
                else {
                    setForeground(Color.black);
                  c.setBackground(bg);
                  

                }
                return this;
            }
        });
        return table;
    }
    
    
   public static JTable ColoraTabelladiGrigio(final JTable table) {
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());
                    c.revalidate();
                }
                 
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    setForeground(Color.gray);
                  c.setBackground(bg);
                  
                  

                }

              // riga=row;
                return this;
            }
        });
        return table;
    }  
    
       public static JTable ColoraTabellaSemplice(final JTable table) {
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());
                    c.revalidate();
                }
                 
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                   // setForeground(Color.gray);
                  c.setBackground(bg);
                  
                  

                }

              // riga=row;
                return this;
            }
        });
        return table;
    }  
    
    
       
       
           public static JTable ColoraTabellaEvidenzaRigheErrore(final JTable table) {
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


            Color bg= (row % 2 == 0  ? grigioChiaro : bianco);
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());
                    c.revalidate();
                }else if (table.getModel().getColumnCount()>1 && table.getModel().getValueAt(row, 2).toString().toLowerCase().contains("error")) {
                    setForeground(Color.RED);
                  c.setBackground(bg);
                }else if (table.getModel().getColumnCount()>3 && table.getModel().getValueAt(row, 4).toString().toLowerCase().contains("error")) {
                    setForeground(Color.RED);
                  c.setBackground(bg);
                }else if (table.getModel().getColumnCount()>11 && table.getModel().getValueAt(row, 15).toString().toLowerCase().contains("error")) {          
                    setForeground(Color.RED);
                  c.setBackground(bg);                
                }
                 
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                  setForeground(Color.black);
                  c.setBackground(bg);
                  
                  

                }

              // riga=row;
                return this;
            }
        });
        return table;
    }     
       
       
       
       
       
       
       
       
       public static void updateRowHeights(JTable table)
{
    for (int row = 0; row < table.getRowCount(); row++)
    {
        int rowHeight = table.getRowHeight();

        for (int column = 0; column < table.getColumnCount(); column++)
        {
            Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
            rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
        }

        table.setRowHeight(row, rowHeight);
    }
}
       
       
}
