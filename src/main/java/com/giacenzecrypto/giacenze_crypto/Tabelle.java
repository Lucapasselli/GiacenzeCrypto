/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

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

    //Questo serve per la funzione get SommeColonne e per fare in modo che il risultato dato sia l'ultimo eseguito
    private static final Map<JTable, AtomicInteger> versioniSomma = new ConcurrentHashMap<>();


    
    
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
      


public static int getSelectedModelRow(JTable table) {
    int viewRow = table.getSelectedRow();
    if (viewRow == -1) {
        return -1; // Nessuna riga selezionata
    }
    return table.convertRowIndexToModel(viewRow);
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

    
    public static void Tabelle_getSommeColonne2(JTable table) {
    // Cattura snapshot sicuro dei dati visibili su EDT
    SwingUtilities.invokeLater(() -> {
        int rowCount = table.getRowCount();
        TableModel model = table.getModel();
        int colCount = model.getColumnCount();
        TableRowSorter<?> sorter = null;

        if (table.getRowSorter() instanceof TableRowSorter) {
            sorter = (TableRowSorter<?>) table.getRowSorter();
        }

        // Prepara snapshot di indici visibili
        int[] visibleRows = new int[rowCount];
        for (int i = 0; i < rowCount; i++) {
            visibleRows[i] = (sorter != null) ? sorter.convertRowIndexToModel(i) : i;
        }

        // Avvia calcolo in thread separato
        new Thread(() -> {
            Map<Integer, String> valori = new HashMap<>();

            for (int col = 0; col < colCount; col++) {
                BigDecimal somma = BigDecimal.ZERO;

                for (int modelRow : visibleRows) {
                    try {
                        Object val = model.getValueAt(modelRow, col);
                        if (val != null) {
                            String strVal = val.toString();
                            if (Funzioni.Funzioni_isNumeric(strVal, false)) {
                                somma = somma.add(new BigDecimal(strVal));
                            }
                        }
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                        // Skip invalid/missing rows
                    }
                }

                somma = somma.setScale(2, RoundingMode.HALF_UP);
                String text = somma.compareTo(BigDecimal.ZERO) != 0 ? somma.toPlainString() : "";
                if (!text.isBlank())text=Funzioni.formattaBigDecimal(somma, true);
                valori.put(col, text);
            }

            // Aggiorna mappa e repaint header su EDT
            SwingUtilities.invokeLater(() -> {
                CDC_Grafica.SommaColonne.put(table, valori);
                table.getTableHeader().repaint();
            });
        }).start();
    });
}
    
    public static void Tabelle_getSommeColonne(JTable table) {
    SwingUtilities.invokeLater(() -> {
        int rowCount = table.getRowCount();
        TableModel model = table.getModel();
        int colCount = model.getColumnCount();
        TableRowSorter<?> sorter = (table.getRowSorter() instanceof TableRowSorter)
            ? (TableRowSorter<?>) table.getRowSorter()
            : null;

        int[] visibleRows = new int[rowCount];
        for (int i = 0; i < rowCount; i++) {
            visibleRows[i] = (sorter != null) ? sorter.convertRowIndexToModel(i) : i;
        }

        // ✅ Prendi o crea il contatore versione per la tabella
        AtomicInteger versione = versioniSomma.computeIfAbsent(table, t -> new AtomicInteger());
        int versioneCorrente = versione.incrementAndGet();

        new Thread(() -> {
            Map<Integer, String> valori = new HashMap<>();

            for (int col = 0; col < colCount; col++) {
                BigDecimal somma = BigDecimal.ZERO;

                for (int modelRow : visibleRows) {
                    try {
                        Object val = model.getValueAt(modelRow, col);
                        if (val != null) {
                            String strVal = val.toString();
                            if (Funzioni.Funzioni_isNumeric(strVal, false)) {
                                somma = somma.add(new BigDecimal(strVal));
                            }
                        }
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    }
                }

                somma = somma.setScale(2, RoundingMode.HALF_UP);
                String text = somma.compareTo(BigDecimal.ZERO) != 0 ? somma.toPlainString() : "";
                if (!text.isBlank()) text = Funzioni.formattaBigDecimal(somma, true);
                valori.put(col, text);
            }

            // ✅ Solo il thread più recente per quella tabella aggiorna
            SwingUtilities.invokeLater(() -> {
                AtomicInteger attuale = versioniSomma.get(table);
                if (attuale != null && attuale.get() == versioneCorrente) {
                    CDC_Grafica.SommaColonne.put(table, valori);
                    table.getTableHeader().repaint();
                }
            });
        }).start();
    });
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
       
  

public static TableCellRenderer Tabelle_creaNuovoHeaderRenderer(
        JTable table,
        Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters,
        Icon filterIcon) {

    TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();

    return (tbl, value, isSelected, hasFocus, row, col) -> {
        JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
        int modelCol = tbl.convertColumnIndexToModel(col);

        // ICONE
        Icon sortIcon = null;
        List<? extends RowSorter.SortKey> sortKeys = tbl.getRowSorter().getSortKeys();
        if (!sortKeys.isEmpty()) {
            RowSorter.SortKey primarySortKey = sortKeys.get(0);
            if (primarySortKey.getColumn() == modelCol) {
                sortIcon = UIManager.getIcon(primarySortKey.getSortOrder() == SortOrder.ASCENDING
                        ? "Table.ascendingSortIcon"
                        : "Table.descendingSortIcon");
            }
        }

        if (activeFilters.containsKey(modelCol)) {
            label.setIcon(new MultiSelectPopUp_CombinedIcon(sortIcon, filterIcon));
        } else {
            label.setIcon(sortIcon);
        }

        // Recupera la somma dalla mappa globale
        Map<Integer, String> colSums = CDC_Grafica.SommaColonne.get(table);
        String somma = (colSums != null) ? colSums.get(modelCol) : null;

        // Testo header
        String titolo = table.getColumnName(col);

        if (somma != null&&!somma.isBlank()) {
            if (!titolo.toLowerCase().startsWith("<html>")) {
                titolo = "<html>" + titolo + "<br><small style='color:gray'>Somma: " + somma + "</small></html>";
            } else {
                        
                int fine = titolo.toLowerCase().lastIndexOf("</html>");
                if (fine > 0) {
                    titolo = titolo.substring(0, fine) + "<br><small style='color:gray'>Somma: " + somma + "</small>" + titolo.substring(fine);
                } else {
                    titolo += "<br><small style='color:gray'>Somma: " + somma + "</small>";
                }
            }
        }

        label.setText(titolo);
        //label.setToolTipText("Tasto destro x filtrare " + Jsoup.parse(tbl.getColumnName(col)).text());
        label.setToolTipText("Tasto destro x filtrare \n\n" + htmlToTextWithLineBreaks(label.getText().replace("Somma:", "<br>Somma:")));

        return label;
    };
}


public static String htmlToTextWithLineBreaks(String html) {
    Document doc = Jsoup.parse(html);
    StringBuilder sb = new StringBuilder();
    for (Node node : doc.body().childNodes()) {
        processNode(node, sb);
    }
    return sb.toString().trim();
}

private static void processNode(Node node, StringBuilder sb) {
    if (node instanceof TextNode) {
        sb.append(((TextNode) node).text());
    } else if (node.nodeName().equals("br")) {
        sb.append("\n");
    } else {
        for (Node child : node.childNodes()) {
            processNode(child, sb);
        }
    }
}

 
}
