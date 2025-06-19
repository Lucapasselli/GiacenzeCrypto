/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jsoup.Jsoup;

/**
 *
 * @author luca.passelli
 */
public class Tabelle {
    //static Color verdeScuro=new Color (23, 114, 69);
    //static Color verdeScuro=new Color (43, 130, 81);
    static Color verdeScuro=new Color (145, 255, 143);
    static Color verde=new Color (145, 255, 143);
    static Color rosso=new Color(255, 80, 80);
    static Color rossoChiaro=new Color(255, 160, 160);
    static Color bianco=new Color(255, 255, 255);
    static Color grigioChiaro=new Color(245, 245, 245);
    static Color grigio=new Color(70, 70, 70);
    static Color arancione=new Color(255, 216, 166);
    static String Rosso="red";
    static String Verde="green";




    
    
    public static JTable ColoraRigheTabellaCrypto(final JTable table) {
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()  {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
            if (isSelected) {

                    setBackground(table.getSelectionBackground());
                   // setForeground(Color.BLACK);
                   // c.revalidate();//NON CREDO SERVA PIU'
                }
            
           /*  else if (table.isRowSelected(row)&&table.hasFocus()){
                  setBackground(bluChiaro); 
                  setForeground(Color.DARK_GRAY);
                } else if (isSelected&&!table.hasFocus()){
                 setBackground(Color.GRAY);  
             }else if (table.isRowSelected(row)&&!table.hasFocus()){
                 setBackground(Color.GRAY);  
             }
            
            
            */
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
                    setForeground(rosso);
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
                    setForeground(rosso);
                } 
                else if (value!=null &&col==9&& table.getModel().getColumnCount()>32 && 
                        table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(row), 32).toString().toUpperCase().contains("NO")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    c.setBackground(bg);
                    setForeground(rosso);
                } 
                else if (value!=null && col==11 && !value.toString().trim().equalsIgnoreCase("0.00")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    c.setBackground(bg);
                    setForeground(verdeScuro);
                }
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    setForeground(fore);
                  c.setBackground(bg);
                  

                }

              // riga=row;
                return c;
            }
        };
        
        // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
        return table;
    }
    

        public static JTable ColoraRigheTabella0GiacenzeaData(final JTable table) {

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());

                }
                else if (table.getModel().getColumnCount()>3 && table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(row), 4).toString().contains("-")) {
                    c.setBackground(bg);
                    c.setForeground(rosso);
                }
                else if (col==6) {
                    c.setBackground(bg);
                    c.setForeground(rosso);
                }
                else {
                    setForeground(fore);
                  c.setBackground(bg);
                  

                }
                return c;
            }
        };
        // Configura il renderer per i tipi più comuni
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Double.class, renderer);
        return table;
    }

    
        public static JTable ColoraRigheTabella1GiacenzeaData(final JTable table) {

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


        Color bg;
        Color bg2;
        Color fore;
        Color fore2;
        
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            bg2= (row % 2 == 0  ? rossoChiaro : rosso);
            fore=Color.lightGray;
            fore2=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            bg2= (row % 2 == 0  ? rosso : rossoChiaro);
            fore=Color.BLACK;
            fore2=Color.BLACK;
        }
 
            if (isSelected&&col!=7) {

                    c.setBackground(table.getSelectionBackground());
                    //c.revalidate();
                }
            else if (isSelected&&col==7&& value.toString().toLowerCase().contains("-")) {

                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(rosso);
                }
            else if (isSelected&&col==7) {

                    c.setBackground(table.getSelectionBackground());
                }
            else if (col==7 && !value.toString().toLowerCase().contains("-")) {
                    setForeground(fore2);
                  c.setBackground(bg);
                }
            else if (col==7 && value.toString().toLowerCase().contains("-")) {
                    setForeground(Color.black);
                  c.setBackground(bg2);
                }
            else if (table.getModel().getColumnCount()>4 && !table.getModel().getValueAt(row, 5).toString().contains("-")) {
                    setBackground(bg);
                    setForeground(verdeScuro);
                } 
                else if (table.getModel().getColumnCount()>4 && table.getModel().getValueAt(row, 5).toString().contains("-")) {
                    c.setBackground(bg);
                    c.setForeground(rosso);
                }
                else {
                    setForeground(fore);
                  c.setBackground(bg);
                  

                }
                return c;
            }
        };
         table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
        return table;
    }
    
   
        
   public static void PosizionaTabellasuRiga(JTable tabella,int riga,boolean misposto){
       tabella.setRowSelectionInterval(riga, riga);
       //Se misposto è true oltre che ad evidenziare la riga indicata mi sposto anche in quella riga
       if (misposto){
            tabella.scrollRectToVisible(new Rectangle(tabella.getCellRect(riga, 0, true))); 
       }
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


                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
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
    // Definizione dei colori
  //  final Color grigioChiaro = new Color(240, 240, 240); // Colore grigio chiaro
  //  final Color bianco = Color.WHITE;                   // Colore bianco

    // Renderer generico per alternare i colori delle righe
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            // Ottieni il componente standard per la cella
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(bg);
            }

            return c;
        }
    };

    // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    

    // Restituisci la tabella
    return table;
}

public static JTable ColoraTabellaSempliceVerdeRosso(final JTable table,int[] ColVerde,int[] ColRosso) {
    // Definizione dei colori
  //  final Color grigioChiaro = new Color(240, 240, 240); // Colore grigio chiaro
  //  final Color bianco = Color.WHITE;                   // Colore bianco

    // Renderer generico per alternare i colori delle righe
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            // Ottieni il componente standard per la cella
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else if (contiene(ColRosso, col) &&
                value != null) {

                    c.setBackground(bg);
                    c.setForeground(Tabelle.rosso);
                }else if (contiene(ColVerde, col) &&
                value != null) {

                    c.setBackground(bg);
                    c.setForeground(Tabelle.verdeScuro);
                }else{
                c.setBackground(bg);
                c.setForeground(fore);
            }

            return c;
        }
    };

    // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    

    // Restituisci la tabella
    return table;
}

public static boolean contiene(int[] array, int target) {
    if (array==null)return false;
    for (int n : array) {
        if (n == target) return true;
    }
    return false;
}

public static JTable ColoraTabellaRTDettaglio(final JTable table) {
    // Definizione dei colori
  //  final Color grigioChiaro = new Color(240, 240, 240); // Colore grigio chiaro
  //  final Color bianco = Color.WHITE;                   // Colore bianco

    // Renderer generico per alternare i colori delle righe
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            // Ottieni il componente standard per la cella
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } 
            else {
                c.setBackground(bg);
            }
            if (col==7 && value.toString().toLowerCase().contains("-")) {
                    setForeground(rosso);
                }
            else if (col==10)setForeground(rosso);
            else{
                setForeground(fore);
            }
            return c;
        }
    };

    // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    

    // Restituisci la tabella
    return table;
}    
    
public static JTable ColoraTabellaRTPrincipale(final JTable table) {
    // Definizione dei colori
  //  final Color grigioChiaro = new Color(240, 240, 240); // Colore grigio chiaro
  //  final Color bianco = Color.WHITE;                   // Colore bianco

    // Renderer generico per alternare i colori delle righe
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            // Ottieni il componente standard per la cella
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } 
            else {
                c.setBackground(bg);
            }
            if (col==3 && value.toString().toLowerCase().contains("-")) {
                    setForeground(rosso);
                }
            else if (col==3)setForeground(verdeScuro);
            else if (col==6)setForeground(rosso);
            else{
                setForeground(fore);
            }
            return c;
        }
    };

    // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    

    // Restituisci la tabella
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


                    Color bg; 
        Color fore;
        if (CDC_Grafica.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
            if (isSelected) {

                    c.setBackground(table.getSelectionBackground());
                    c.revalidate();
                }else if (table.getModel().getColumnCount()>1 && table.getModel().getValueAt(row, 2).toString().toLowerCase().contains("error")) {
                    setForeground(rosso);
                  c.setBackground(bg);
                }else if (table.getModel().getColumnCount()>3 && table.getModel().getValueAt(row, 4).toString().toLowerCase().contains("error")) {
                    setForeground(rosso);
                  c.setBackground(bg);
                }else if (table.getModel().getColumnCount()>11 && table.getModel().getValueAt(row, 15).toString().toLowerCase().contains("error")) {          
                    setForeground(rosso);
                  c.setBackground(bg);                
                }
                 
                else {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                  setForeground(fore);
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
       
       
public static List<String> Tabelle_getUniqueValuesForColumn(JTable table, int col) {
    List<String> values = new ArrayList<>();
    TableModel model = table.getModel();
    int rowCount = model.getRowCount();

    for (int row = 0; row < rowCount; row++) {
        Object value = model.getValueAt(row, col);
        String text = value != null ? value.toString() : "";
        if (!values.contains(text)) {
            values.add(text);
        }
    }
    return values;
}
       
       
     public static List<String> Tabelle_getVisibleValuesForColumn(JTable table, int col) {
    List<String> values = new ArrayList<>();
    int rowCount = table.getRowCount();

    for (int row = 0; row < rowCount; row++) {
        Object value = table.getValueAt(row, col);
        String text = value != null ? value.toString() : "";
        if (!values.contains(text)) {
            values.add(text);
        }
    }
    return values;
}  
       
  
       
  
 
 public static TableCellRenderer Tabelle_creaNuovoHeaderRenderer(JTable table, Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters, Icon filterIcon) {
    TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();

    return (tbl, value, isSelected, hasFocus, row, col) -> {
        JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
        int modelCol = tbl.convertColumnIndexToModel(col);

        Icon sortIcon = null;
        RowSorter.SortKey sortKey = null;
        List<? extends RowSorter.SortKey> sortKeys = tbl.getRowSorter().getSortKeys();
        if (!sortKeys.isEmpty()) {
            RowSorter.SortKey primarySortKey = sortKeys.get(0); // solo la colonna principale
            if (primarySortKey.getColumn() == modelCol) {
                sortIcon = UIManager.getIcon(primarySortKey.getSortOrder() == SortOrder.ASCENDING
                        ? "Table.ascendingSortIcon"
                        : "Table.descendingSortIcon");
            }
        }

        if (sortKey != null) {
            sortIcon = UIManager.getIcon(sortKey.getSortOrder() == SortOrder.ASCENDING ? "Table.ascendingSortIcon" : "Table.descendingSortIcon");
        }

        if (activeFilters.containsKey(modelCol)) {
            label.setIcon(new MultiSelectPopup_CombinedIcon(sortIcon, filterIcon));
        } else {
            label.setIcon(sortIcon);
        }

        label.setToolTipText("Tasto destro x filtrare " + Jsoup.parse(tbl.getColumnName(col)).text()); 
        return label;
    };
}
 

 
 
}
