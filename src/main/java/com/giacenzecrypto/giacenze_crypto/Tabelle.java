/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
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
    static String Rosso="red";
    static String Verde="green";
    static Color gialloChiaro = new Color(255, 250, 180);

    //Questo serve per la funzione get SommeColonne e per fare in modo che il risultato dato sia l'ultimo eseguito
    private static final Map<JTable, AtomicInteger> versioniSomma = new ConcurrentHashMap<>();
    
    public static final Map<JTable, Map<Integer, RowFilter<DefaultTableModel, Integer>>> tableFilters = new HashMap<>();
    public static Map<JTable, Map<Integer, String>> SommaColonne = new HashMap<>();
    private static final Set<JTable> tabelleConFiltroColonne = new HashSet<>();


    
    
    public static JTable ColoraRigheTabellaCrypto(final JTable table) {
      //  bg=grigioChiaro;
     //   Data="";

     //System.out.println("test");

     

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()  {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        int modelRow=row;
        if (table.getRowSorter()!=null){
            modelRow = table.getRowSorter().convertRowIndexToModel(row);
        }
        Color bg; 
        Color fore;
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
                }
            
                else if (col==3 && 
                        (value.toString().toLowerCase().equals("deposito crypto")||value.toString().toLowerCase().equals("deposito nft"))) {
                    setBackground(bg);
                    setForeground(verdeScuro);
                } 
                else if (col==3 && 
                        (value.toString().toLowerCase().equals("prelievo crypto")||value.toString().toLowerCase().equals("prelievo nft"))) {
                    setBackground(bg);
                    setForeground(rosso);
                } 
                else if (value!=null && col==11 && value.toString().trim().contains("-")) {
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
            
            
            

                // Inserisci icona in colonna 2 se contiene "negativa"
                if (table.getName()!=null&&col==3 &&table.getName().equals("TabellaMovimentiCrypto")&&
                        (value.toString().toLowerCase().equals("deposito crypto")
                        ||value.toString().toLowerCase().equals("prelievo crypto")
                        ||value.toString().toLowerCase().equals("deposito nft")
                        ||value.toString().toLowerCase().equals("prelievo nft"))) {
                    
                    //adesso verifico che il movimento non coinvolga tokenscam altrimenti non voglio che venga evidenziato il problema
                    boolean SCAMUscita=Funzioni.isSCAM(table.getModel().getValueAt(modelRow, 8).toString());
                    boolean SCAMEntrata=Funzioni.isSCAM(table.getModel().getValueAt(modelRow, 11).toString());
                    if(!SCAMUscita&&!SCAMEntrata){
                        JLabel label = new JLabel();
                        label.setOpaque(true);
                        label.setBackground(c.getBackground());
                        label.setForeground(c.getForeground());
                        label.setText(value.toString());
                        Icon icon = Icone.getAlert(18);
                        label.setIcon(icon);
                        label.setIconTextGap(6); // spazio tra icona e testo
                        return label;
                    }
                }
                else if ((col == 11 && !table.getModel().getValueAt(modelRow, 38).toString().isBlank())//Manca parte del LiFo
                        ||                     
                    (table.getModel().getColumnCount()>32 && col==9&& table.getModel().getValueAt(modelRow, 32).toString().toUpperCase().contains("NO"))//Transazione senza prezzo
                        ) 
                {
                    boolean SCAMUscita=Funzioni.isSCAM(table.getModel().getValueAt(modelRow, 8).toString());
                    boolean SCAMEntrata=Funzioni.isSCAM(table.getModel().getValueAt(modelRow, 11).toString());
                    if(!SCAMUscita&&!SCAMEntrata){
                        JLabel label = new JLabel();
                        label.setOpaque(true);
                        label.setBackground(c.getBackground());
                        label.setForeground(c.getForeground());
                        label.setText(value.toString());
                        Icon icon = Icone.getAlert(18);
                        label.setIcon(icon);
                        label.setIconTextGap(6); // spazio tra icona e testo
                   
                    // Tooltip personalizzata in base alla colonna
             /*       if (col == 11) {
                        label.setToolTipText("Manca parte del calcolo LIFO – verifica che ci siano tutti gli acquisti");
                    } else if (col == 9) {
                        label.setToolTipText("Transazione senza prezzo");
                    }*/
                    
                    return label;
                    }
                }
                
             /*   if (col==9&& table.getModel().getValueAt(modelRow, 32).toString().toUpperCase().contains("NO")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    JLabel label = new JLabel();
                    label.setOpaque(true);
                    label.setBackground(c.getBackground());
                    label.setForeground(c.getForeground());
                    label.setText(value.toString());
                    Icon icon = Icone.getAlert(18);
                    label.setIcon(icon);
                    label.setIconTextGap(6); // spazio tra icona e testo
                    return label;
                } */
                

                return c;
            }
        };

        
        
        // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
        return table;
    }
    

    
    public static void Funzioni_EliminaRigheDuplicate(JTable table) {
    DefaultTableModel model = (DefaultTableModel) table.getModel();
    int colCount = model.getColumnCount();
    int rowCount = model.getRowCount();
   // System.out.println("aaaaaaaaaaaaaaaa"+rowCount);

    // Set per memorizzare righe uniche
    Set<String> righeUniche = new LinkedHashSet<>();

    for (int r = 0; r < rowCount; r++) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < colCount; c++) {
            sb.append(String.valueOf(model.getValueAt(r, c))).append("||"); // separatore
        }
        righeUniche.add(sb.toString());
        //System.out.println(sb.toString()+"bbb");
    }

    // Crea nuovo modello
    DefaultTableModel nuovoModel = new DefaultTableModel();
    // aggiungi colonne
    for (int c = 0; c < colCount; c++) {
        nuovoModel.addColumn(model.getColumnName(c));
    }

    // aggiungi righe uniche
    for (String riga : righeUniche) {
        //System.out.println(riga);
        String[] valori = riga.split("\\|\\|", -1); // -1 per preservare stringhe vuote
        nuovoModel.addRow(valori);
    }

    table.setModel(nuovoModel);
}
    
    
    
    
        public static JTable ColoraRigheTabella0GiacenzeaData(final JTable table) {

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


                    Color bg; 
        Color fore;
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
                else if (table.getModel()!=null&&table.getModel().getColumnCount()>3 && table.getModel()!=null &&table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(row), 4).toString().contains("-")) {
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

        int modelRow=row;
        if (table.getRowSorter()!=null){
            modelRow = table.getRowSorter().convertRowIndexToModel(row);
        }
        String GiacWallet=null;
        
        String GiacGruppo=null;
        
        String GiacTotale=null;
        

        Color bg;
        Color bg2;
        Color fore;
        Color fore2;
        
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
 
          /*  if(col==10){if (table.getModel().getValueAt(modelRow, 10)!=null)GiacWallet=table.getModel().getValueAt(modelRow, 10).toString();
            System.out.println(table.getModel().getValueAt(modelRow, 10));}
            if(col==11){if (table.getModel().getValueAt(modelRow, 11)!=null)GiacGruppo=table.getModel().getValueAt(modelRow, 11).toString();}
            if(col==12){if (table.getModel().getValueAt(modelRow, 12)!=null)GiacTotale=table.getModel().getValueAt(modelRow, 12).toString();}*/
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
                JLabel label = new JLabel();
                    label.setOpaque(true);
                    label.setBackground(bg);
                    label.setForeground(fore2);
                    label.setText(value.toString());
                    if (table.getModel().getValueAt(modelRow, 10)!=null)GiacWallet=table.getModel().getValueAt(modelRow, 10).toString();
                    if (table.getModel().getValueAt(modelRow, 11)!=null)GiacGruppo=table.getModel().getValueAt(modelRow, 11).toString();
                    if (table.getModel().getValueAt(modelRow, 12)!=null)GiacTotale=table.getModel().getValueAt(modelRow, 12).toString();
                    //System.out.println(GiacTotale);
                    if (GiacTotale!=null){
                        String text="<html>"
                                + "Rimanenze Wallet = "+GiacWallet+"<br>"
                                + "Rimanenze Gruppo = "+GiacGruppo+"<br>"
                                + "Rimanenze Totali = "+GiacTotale                               
                                + "</html>";
                        label.setToolTipText(text);
                    }
                    return label;
                 //   setForeground(fore2);
                //  c.setBackground(bg);
                }
            else if (col==7 && value.toString().toLowerCase().contains("-")) {
                JLabel label = new JLabel();
                    label.setOpaque(true);
                    label.setBackground(bg2);
                    label.setForeground(Color.black);
                    label.setText(value.toString());
                    if (table.getModel().getValueAt(modelRow, 10)!=null)GiacWallet=table.getModel().getValueAt(modelRow, 10).toString();
                    if (table.getModel().getValueAt(modelRow, 11)!=null)GiacGruppo=table.getModel().getValueAt(modelRow, 11).toString();
                    if (table.getModel().getValueAt(modelRow, 12)!=null)GiacTotale=table.getModel().getValueAt(modelRow, 12).toString();
                    if (GiacTotale!=null){
                        String text="<html>"
                                + "Rimanenze Wallet = "+GiacWallet+"<br>"
                                + "Rimanenze Gruppo = "+GiacGruppo+"<br>"
                                + "Rimanenze Totali = "+GiacTotale                               
                                + "</html>";
                        label.setToolTipText(text);
                    }
                    return label;
                 //   setForeground(Color.black);
                //  c.setBackground(bg2);
                }
            else if (table.getModel().getColumnCount()>4 && !table.getModel().getValueAt(modelRow, 5).toString().contains("-")) {
                    setBackground(bg);
                    setForeground(verdeScuro);
                } 
                else if (table.getModel().getColumnCount()>4 && table.getModel().getValueAt(modelRow, 5).toString().contains("-")) {
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
    
   
        
  /* public static void Funzioni_PosizionaTabellasuRiga(JTable tabella,int riga,boolean misposto){
       tabella.setRowSelectionInterval(riga, riga);
       //Se misposto è true oltre che ad evidenziare la riga indicata mi sposto anche in quella riga
       if (misposto){
            tabella.scrollRectToVisible(new Rectangle(tabella.getCellRect(riga, 0, true))); 
       }
   }   */ 
   /**
 * Posiziona la selezione della JTable sulla riga indicata e, opzionalmente,
 * effettua lo scroll della tabella per rendere visibile la riga selezionata.
 *
 * <p>La funzione esegue due operazioni:
 * <ul>
 *     <li>Se la riga è valida (compresa tra 0 e rowCount-1), imposta la selezione della JTable su quella riga.</li>
 *     <li>Se {@code misposto} è true, effettua lo scroll automatico per portare la riga selezionata in vista.
 *         L’operazione di scroll viene eseguita sulla Event Dispatch Thread tramite {@code SwingUtilities.invokeLater}.</li>
 * </ul>
 *
 * @param tabella  la JTable su cui effettuare la selezione e lo scroll.  
 *                 Non deve essere {@code null}.
 * @param riga     indice della riga da selezionare e/o rendere visibile.  
 *                 Deve essere compreso tra 0 e {@code tabella.getRowCount() - 1}.
 * @param misposto se true, la tabella effettuerà lo scroll fino alla riga indicata;  
 *                 se false, la riga verrà solo selezionata senza alcun movimento della viewport.
 */
   public static void Funzioni_PosizionaTabellasuRiga(JTable tabella, int riga, boolean misposto) {
    if (riga >= 0 && riga < tabella.getRowCount()) {
        tabella.setRowSelectionInterval(riga, riga);
    }
    if (misposto) {
        SwingUtilities.invokeLater(() -> {
            Rectangle rect = tabella.getCellRect(riga, 0, true);
            tabella.scrollRectToVisible(rect);
        });
    }
}
   
   public static void Funzioni_RipristinaSelezioneEPosizione(JTable tabella, int riga,int scrollValue) {
   JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tabella);
  if (riga >= 0 && riga < tabella.getRowCount()) {
            tabella.setRowSelectionInterval(riga, riga);
        }

    SwingUtilities.invokeLater(() -> {

        if (scrollPane != null) {
            scrollPane.getVerticalScrollBar().setValue(scrollValue);
        }
        
    });
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
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
       // Color fore;
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            //fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
         //   fore=table.getForeground();
            //fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
               // c.setForeground(table.getSelectionForeground());
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



public static void GUI_ModificaPrezzo_ColoraTabelle(
        JTable table1, JTable table2) {
    
 
    int rigaTab1[]=new int[1];
    rigaTab1[0]=0;
    int colonneDaControllare = Math.min(6, table1.getColumnCount());
    int righeDaControllare = Math.min(table1.getRowCount(), table2.getRowCount());

    // Memorizza i valori di tutte le righe di table1 da confrontare
    Object[][] valoriTable1 = new Object[righeDaControllare][colonneDaControllare];
    for (int row = 0; row < righeDaControllare; row++) {
        for (int col = 0; col < colonneDaControllare; col++) {
            valoriTable1[row][col] = table1.getValueAt(row, col);
        }
    }

   // boolean giallo[]=new boolean[2];
   // giallo[0]=false;
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // ---------------------------
            // 1. LOGICA DEL RENDERER
            // ---------------------------
            Color bg;
            if (Principale.tema.equalsIgnoreCase("Scuro")) {
                bg = (row % 2 == 0 ? grigio : Color.DARK_GRAY);
                c.setForeground(Color.LIGHT_GRAY);   // testo standard tema scuro
            } else {
                bg = (row % 2 == 0 ? grigioChiaro : bianco);
                c.setForeground(Color.BLACK);        // testo standard tema chiaro
            }

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                c.setBackground(bg);
            }

            // ---------------------------
            // 2. LOGICA EVIDENZIAZIONE
            // ---------------------------
            boolean match = false;
            for (int rigaT1 = 0; rigaT1 < righeDaControllare; rigaT1++) {
                boolean corrisponde = true;
                for (int colo = 0; colo < colonneDaControllare; colo++) {
                    Object val1 = valoriTable1[rigaT1][colo];
                    Object val2 = table2.getValueAt(row, colo);

                    if (val1 == null && val2 == null) continue;
                    if (val1 == null || val2 == null || !val1.equals(val2)) {
                        corrisponde = false;
                        break;
                    }
                }
                if (corrisponde) {
                    rigaTab1[0]=rigaT1;
                    match = true;
                    break;
                }
            }

            // Se la riga matcha E NON è selezionata → giallo
            if (match && !isSelected) {
                c.setBackground(gialloChiaro);
                //giallo[0]=true;
                //giallo=true;
               // giallo[0]=true;
                GUI_ModificaPrezzo_ColoraTabellaGialla(table1,rigaTab1[0]);
                // TEMA SCURO → testo nero sul giallo
                if (Principale.tema.equalsIgnoreCase("Scuro")) {
                    c.setForeground(Color.BLACK);
                } else {
                    // TEMA CHIARO → testo normale
                  //  c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    };

    // Applica il renderer a tutte le colonne
    TableColumnModel tcm = table2.getColumnModel();
    for (int i = 0; i < tcm.getColumnCount(); i++) {
        tcm.getColumn(i).setCellRenderer(renderer);
    }

    table2.repaint();
   // if(giallo[0])GUI_ModificaPrezzo_ColoraTabellaGialla(table1);
    
}

public static void GUI_ModificaPrezzo_ColoraTabellaGialla(JTable table1,int riga) {

    
    // -------------------------
    // RENDERER TABLE1
    // -------------------------
    DefaultTableCellRenderer renderer1 = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // colore base
            if (!isSelected) {
                if (Principale.tema.equalsIgnoreCase("Scuro")) {
                    c.setBackground(row % 2 == 0 ? grigio : Color.DARK_GRAY);
                    c.setForeground(Color.LIGHT_GRAY);
                } else {
                    c.setBackground(row % 2 == 0 ? grigioChiaro : Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }

            // giallo solo sulla riga 0
            if (row == riga) {
                c.setBackground(gialloChiaro);

                if (Principale.tema.equalsIgnoreCase("Scuro"))
                    c.setForeground(Color.BLACK);
            }

            return c;
        }
    };

    for (int i = 0; i < table1.getColumnCount(); i++) {
        table1.getColumnModel().getColumn(i).setCellRenderer(renderer1);
    }

    table1.repaint();

}




public static JTable ColoraTabellaLiFoTransazione(final JTable table) {
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
                    Color bgUscita;
                    Color fore=table.getSelectionForeground();
                    Color RossopiuLeggero = Color.decode("#ffe5e5");
                    Color RossoLeggero = Color.decode("#ffcccc");
                    Color verde1 = Color.decode("#e6f4ea");
                    Color verde2 = Color.decode("#ccf2d5");
                    Color bgEntrata;
                    bgUscita= (row % 2 == 0  ? RossoLeggero : RossopiuLeggero);
                    bgEntrata= (row % 2 == 0  ? verde1 : verde1);
       // Riconversione riga se usa RowSorter
    int modelRow = table.getRowSorter().convertRowIndexToModel(row);
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            //fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
         //   fore=table.getForeground();
            fore=Color.BLACK;
        }
            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
               // c.setForeground(table.getSelectionForeground());
            }
          /*  else if (table.getName().equalsIgnoreCase("Uscita") && table.getModel().getValueAt(modelRow, 2).toString().contains("negativa")) {
                c.setBackground(Color.RED);
                c.setForeground(Color.BLACK);
            }*/else if (table.getName().equalsIgnoreCase("Uscita") && (boolean)table.getModel().getValueAt(modelRow, 7)) {
                c.setBackground(bgUscita);
                c.setForeground(Color.BLACK);
            }

            else if (table.getName().equalsIgnoreCase("Entrata") && (boolean)table.getModel().getValueAt(modelRow, 7)) {
                c.setBackground(bgEntrata);
                c.setForeground(Color.BLACK);
            }
            else {
                c.setBackground(bg);
                c.setForeground(fore);
            }
            
                // Inserisci icona in colonna 2 se contiene "negativa"
    if (col == 2 && table.getModel().getValueAt(modelRow, col).toString().toLowerCase().contains("negativa")) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        //label.setBackground(c.getBackground());
        label.setBackground(Tabelle.rosso);
        label.setForeground(c.getForeground());
        label.setText(value.toString());

        // Icona di alert (puoi cambiarla)
        // Usa l'icona di default di Java (warning) e la ridimensiona
        Icon icon = Icone.getAlert(18); // percorso relativo alle risorse
        label.setIcon(icon);
          //      Icon alertIcon = resizeIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Alert.png")),14,14);
             //   label.setIcon(alertIcon);
                label.setIconTextGap(6); // spazio tra icona e testo
                return label;
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


public static Icon resizeIcon(Icon icon, int width, int height) {
    BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    icon.paintIcon(null, g2d, 0, 0);
    g2d.dispose();

    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    return new ImageIcon(scaled);
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
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=table.getForeground();
          //  fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
               // c.setForeground(table.getSelectionForeground());
            } 
            else if (col==7 && value.toString().toLowerCase().contains("-")) {
                    setForeground(rosso);
                }
            else if (col==10)setForeground(rosso);
            else{
                setForeground(fore);
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
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : Color.DARK_GRAY);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=table.getForeground();
            //fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } 
            else if (col==3 && value.toString().toLowerCase().contains("-")) {
                    setForeground(rosso);
                }
            else if (col==3)setForeground(verdeScuro);
            else if (col==6)setForeground(rosso);
            else{
                setForeground(fore);
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
      


public static int Funzioni_getRigaSelezionata(JTable table) {
    int viewRow = table.getSelectedRow();
    if (viewRow == -1) {
        return -1; // Nessuna riga selezionata
    }
    return table.convertRowIndexToModel(viewRow);
}

public static int[] Funzioni_getRigheSelezionate(JTable table) {
    int[] viewRows = table.getSelectedRows();
    int[] modelRows = new int[viewRows.length];

    for (int i = 0; i < viewRows.length; i++) {
        modelRows[i] = table.convertRowIndexToModel(viewRows[i]);
    }

    return modelRows;
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
        if (Principale.tema.equalsIgnoreCase("Scuro")){
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
       
       
/*public static List<String> Tabelle_getUniqueValuesForColumnOLD(JTable table, int col) {
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
}*/

    
 
    
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
                            if (Funzioni.isNumeric(strVal, false)) {
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
                        SommaColonne.put(table, valori);
                    table.getTableHeader().repaint();
                }
            });
        }).start();
    });
}
  



public static Map<String, String[]> Tabelle_getValoriUnivociColonnaConVisibilita(
        JTable table, int viewColIndex) {

    Map<String, String[]> valori = new TreeMap<>();

    DefaultTableModel model = (DefaultTableModel) table.getModel();
    int modelColIndex = table.convertColumnIndexToModel(viewColIndex);
    int rowCount = model.getRowCount();

    Map<Integer, RowFilter<DefaultTableModel, Integer>> filters = tableFilters.getOrDefault(table, Map.of());

    boolean hasFilterOnCurrentColumn = filters.containsKey(modelColIndex);

    RowSorter<? extends TableModel> sorter = table.getRowSorter();
    if (sorter == null) {
        // Se non c’è sorter considera tutte le righe visibili
        // (valori visibili = tutte le righe)
        for (int row = 0; row < rowCount; row++) {
            Object valObj = model.getValueAt(row, modelColIndex);
            String val = valObj != null ? valObj.toString() : "";
            valori.put(val, new String[]{val, "1"});
        }
        return valori;
    }

    // Ottengo l’insieme di righe visibili col filtro completo (filtro su tutta la tabella)
    Set<Integer> visibleRows = new HashSet<>();
    int visibleRowCount = sorter.getViewRowCount();
    for (int i = 0; i < visibleRowCount; i++) {
        visibleRows.add(sorter.convertRowIndexToModel(i));
    }

    if (!hasFilterOnCurrentColumn) {
        // Se non c’è filtro sulla colonna, mostro solo i valori delle righe visibili
        for (Integer modelRow : visibleRows) {
            Object valObj = model.getValueAt(modelRow, modelColIndex);
            String val = valObj != null ? valObj.toString() : "";
            valori.put(val, new String[]{val, "1"});
        }
        return valori;
    }

    // Se c’è filtro sulla colonna:

    // 1) Ricavo filtro senza quello sulla colonna
    List<RowFilter<DefaultTableModel, Integer>> filtersExcludingCurrent = filters.entrySet().stream()
            .filter(e -> e.getKey() != modelColIndex)
            .map(Map.Entry::getValue)
            .toList();

    RowFilter<DefaultTableModel, Integer> combinedFilterExcludingCurrent = null;
    if (!filtersExcludingCurrent.isEmpty()) {
        combinedFilterExcludingCurrent = RowFilter.andFilter(filtersExcludingCurrent);
    }

    TableRowSorter<DefaultTableModel> tempSorter = new TableRowSorter<>(model);
    tempSorter.setRowFilter(combinedFilterExcludingCurrent);

    // Ottengo righe visibili senza il filtro sulla colonna
    Set<Integer> rowsVisibleWithoutCurrentFilter = new HashSet<>();
    int tempVisibleCount = tempSorter.getViewRowCount();
    for (int i = 0; i < tempVisibleCount; i++) {
        rowsVisibleWithoutCurrentFilter.add(tempSorter.convertRowIndexToModel(i));
    }

    // Ora scorro tutte le righe del modello
    for (int row = 0; row < rowCount; row++) {
        Object valObj = model.getValueAt(row, modelColIndex);
        String val = valObj != null ? valObj.toString() : "";

        boolean isVisibleNow = visibleRows.contains(row);
        boolean isVisibleWithoutFilter = rowsVisibleWithoutCurrentFilter.contains(row);

        if (isVisibleNow) {
            // Righe visibili col filtro completo, checkbox spuntato
            valori.put(val, new String[]{val, "1"});
        } else if (isVisibleWithoutFilter) {
            // Righe non visibili col filtro completo ma visibili senza filtro sulla colonna
            // checkbox NON spuntato, valore mostrato perché influenzabile
            valori.put(val, valori.getOrDefault(val, new String[]{val, "0"}));
        }
        // Se non è visibile ne con ne senza filtro sulla colonna, non lo aggiungo (non influenzabile)
    }

    return valori;
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
      //  if(tbl.getRowSorter()!=null){
        List<? extends RowSorter.SortKey> sortKeys = tbl.getRowSorter().getSortKeys();
        if (!sortKeys.isEmpty()) {
            RowSorter.SortKey primarySortKey = sortKeys.get(0);
            if (primarySortKey.getColumn() == modelCol) {
                sortIcon = UIManager.getIcon(primarySortKey.getSortOrder() == SortOrder.ASCENDING
                        ? "Table.ascendingSortIcon"
                        : "Table.descendingSortIcon");
            }
        }
      //  }

        if (activeFilters.containsKey(modelCol)) {
            label.setIcon(new MultiSelectPopUp_CombinedIcon(sortIcon, filterIcon));
        } else {
            label.setIcon(sortIcon);
        }

        // Recupera la somma dalla mappa globale
        Map<Integer, String> colSums = SommaColonne.get(table);
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

 
 public static void Tabelle_InizializzaHeader(JTable table) { 
    ImageIcon originalIco = new javax.swing.ImageIcon(Principale.class.getResource("/Images/24_Imbuto.png"));
    //Image image = Icone.svgImbuto.getImage();  // Ottiene l'immagine interna
   // ImageIcon originalIco = new ImageIcon(image);  // Converte in ImageIcon
   //  ImageIcon originalIco = Icone.Imbuto;
    //ImageIcon originalIco = new javax.swing.ImageIcon(getClass().getResource("/Images/24_Imbuto.png"));
    Image scaledImag = originalIco.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH);
    Icon filterIco = new ImageIcon(scaledImag);
    Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.get(table);
   table.getTableHeader().setDefaultRenderer(Tabelle.Tabelle_creaNuovoHeaderRenderer(table, activeFilters, filterIco));
}

     public static void Tabelle_applyCombinedFilter(JTable table, TableRowSorter<DefaultTableModel> sorter, String globalFilterText) {
    Map<Integer, RowFilter<DefaultTableModel, Integer>> filters = tableFilters.getOrDefault(table, Map.of());

    List<RowFilter<DefaultTableModel, Integer>> combinedFilters = new ArrayList<>(filters.values());

    if (globalFilterText != null && !globalFilterText.isEmpty()) {
        RowFilter<DefaultTableModel, Integer> globalFilter = new RowFilter<>() {
            @Override
            public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object value = entry.getValue(i);
                    if (value != null && value.toString().toLowerCase().contains(globalFilterText.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
        };
        combinedFilters.add(globalFilter);
    }

    if (combinedFilters.isEmpty()) {
        sorter.setRowFilter(null);
    } else {
        sorter.setRowFilter(RowFilter.andFilter(combinedFilters));
    }
    //nel caso sia la tabella principale filtro le plusvalenze
    //if (table.equals(TransazioniCryptoTabella))TransazioniCrypto_CalcolaPlusvalenzeFiltrate();
    Tabelle.Tabelle_getSommeColonne(table);
   // System.out.println("Apply filter "+table);
}    

     
     
    public static void Tabelle_FiltroColonne2(JTable table,JTextField filtro,Tabelle_PopupSelezioneMultipla popup) {
    
    //Inizializza tableFilters se non esiste
    tableFilters.putIfAbsent(table, new HashMap<>());
    Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.get(table);
    
    Tabelle.Tabelle_InizializzaHeader(table);
    JTableHeader header = table.getTableHeader();

    DefaultTableModel model = (DefaultTableModel) table.getModel();
    TableRowSorter<DefaultTableModel> sorter;

    if (table.getRowSorter() instanceof TableRowSorter) {
        sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
    } else {
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
    }
    
    String filtrot = (filtro != null) ? filtro.getText() : "";
    Tabelle.Tabelle_applyCombinedFilter(table, sorter, filtrot);


    header.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) { // Tasto destro
                header.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                System.out.println("Filtro colonne");
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0) {
                    int modelCol = table.convertColumnIndexToModel(col);
                    Map<String, String[]> mappa = Tabelle_getValoriUnivociColonnaConVisibilita(table, col);
                    List<String[]> valori = new ArrayList<>(mappa.values());
                    popup.updateOptions(valori);


                    popup.setApplyAction(() -> {
                        
                        List<String> selected = popup.getSelectedOptions();
                        if (selected.isEmpty() || selected.size() == mappa.size()) {
                            // Nessun filtro: rimuovi filtro e icona
                            activeFilters.remove(modelCol);
                         //   filteredColumns.remove(modelCol);
                        } 
                         else {
                            RowFilter<DefaultTableModel, Integer> filter = new RowFilter<>() {
                                @Override
                                public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                                    Object cellValue = entry.getValue(modelCol);
                                    return selected.contains(cellValue != null ? cellValue.toString() : "");
                                }
                            };
                            activeFilters.put(modelCol, filter);
                        }
                        String filtrot = (filtro != null) ? filtro.getText() : "";
                        Tabelle.Tabelle_applyCombinedFilter(table,sorter, filtrot);
                        popup.AzzeraTestoRicerca();
                        
                        
                        header.repaint();
                        popup.hide();
                    });

                    popup.setCancelAction(() -> {
                        popup.AzzeraTestoRicerca();
                        popup.hide();
                    });
                    
                    Rectangle headerRect = header.getHeaderRect(col);
                    Point headerLoc = header.getLocationOnScreen();

                    int popupX = headerLoc.x + headerRect.x;
                    int popupY = headerLoc.y + headerRect.height;

                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Dimension popupSize = popup.getPreferredSize();

                    if (popupX + popupSize.width > screenSize.width) {
                        popupX = Math.max(screenSize.width - popupSize.width, 0);
                    }
                    popup.showAt(popupX, popupY);
                }
                header.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    });
    
}



public static void Tabelle_FiltroColonne(JTable table, JTextField filtro, Tabelle_PopupSelezioneMultipla popup) {
    // Inizializza tableFilters se non esiste
    tableFilters.putIfAbsent(table, new HashMap<>());
    Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.get(table);

    Tabelle.Tabelle_InizializzaHeader(table);
    JTableHeader header = table.getTableHeader();

    DefaultTableModel model = (DefaultTableModel) table.getModel();
    TableRowSorter<DefaultTableModel> sorter;

    if (table.getRowSorter() instanceof TableRowSorter) {
        sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
    } else {
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
    }

    String filtrot = (filtro != null) ? filtro.getText() : "";
    Tabelle.Tabelle_applyCombinedFilter(table, sorter, filtrot);

    // Evita di aggiungere il listener più volte
    if (!tabelleConFiltroColonne.contains(table)) {

        header.addMouseListener(new MouseAdapter() {
            @Override
            public String toString() {
                return "FiltroColonneMouseListener";
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Tasto destro
                    header.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    int col = table.columnAtPoint(e.getPoint());
                    if (col >= 0) {
                        int modelCol = table.convertColumnIndexToModel(col);
                        Map<String, String[]> mappa = Tabelle_getValoriUnivociColonnaConVisibilita(table, col);
                        List<String[]> valori = new ArrayList<>(mappa.values());
                        popup.updateOptions(valori);

                        popup.setApplyAction(() -> {
                            List<String> selected = popup.getSelectedOptions();
                            if (selected.isEmpty() || selected.size() == mappa.size()) {
                                activeFilters.remove(modelCol);
                            } else {
                                RowFilter<DefaultTableModel, Integer> filter = new RowFilter<>() {
                                    @Override
                                    public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                                        Object cellValue = entry.getValue(modelCol);
                                        return selected.contains(cellValue != null ? cellValue.toString() : "");
                                    }
                                };
                                activeFilters.put(modelCol, filter);
                            }

                            String filtrot = (filtro != null) ? filtro.getText() : "";
                            Tabelle.Tabelle_applyCombinedFilter(table, sorter, filtrot);
                            popup.AzzeraTestoRicerca();
                            header.repaint();
                            popup.hide();
                        });

                        popup.setCancelAction(() -> {
                            popup.AzzeraTestoRicerca();
                            popup.hide();
                        });

                        Rectangle headerRect = header.getHeaderRect(col);
                        Point headerLoc = header.getLocationOnScreen();

                        int popupX = headerLoc.x + headerRect.x;
                        int popupY = headerLoc.y + headerRect.height;

                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        Dimension popupSize = popup.getPreferredSize();

                        if (popupX + popupSize.width > screenSize.width) {
                            popupX = Math.max(screenSize.width - popupSize.width, 0);
                        }

                        popup.showAt(popupX, popupY);
                    }

                    header.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        // Segna che la tabella ha già il listener
        tabelleConFiltroColonne.add(table);
    }
}
   
     
     
    public static void Tabella_RimuoviFiltri(JTable table) {
    // Rimuovi tutti i filtri dalla mappa relativa a questa tabella
    Map<Integer, RowFilter<DefaultTableModel, Integer>> filters = tableFilters.get(table);
    if (filters != null) {
        filters.clear();
    }

    // Rimuovi colonne filtrate
    //if (filters.keySet()!=null){
    Set<Integer> filteredCols = filters.keySet();
    if (filteredCols != null) {
        filteredCols.clear();
    }

    // Rimuovi il filtro dal TableRowSorter della tabella
    RowSorter<?> rowSorter = table.getRowSorter();
    if (rowSorter instanceof TableRowSorter<?>) {
        ((TableRowSorter<?>) rowSorter).setRowFilter(null);
    }
    //table.setRowSorter(null);
    // Forza repaint dell'header per togliere icone o evidenziazioni
    table.getTableHeader().repaint();
}

    public static void Funzioni_PulisciTabella(DefaultTableModel modello) {
     /*   int z = modello.getRowCount();
        // System.out.println(modelProblemi.getRowCount());
        while (z != 0) {
            modello.removeRow(0);
            z = modello.getRowCount();
        }*/
        modello.setRowCount(0);
    }
     
    public static class OptionEntry {
    public final String value;
    public boolean selected;

    public OptionEntry(String value) {
        this.value = value;
        this.selected = false;
    }
}

    
    public static class MultiSelectPopUp_CombinedIcon implements Icon {
    private final Icon sortIcon;
    private final Icon filterIcon;

    public MultiSelectPopUp_CombinedIcon(Icon sortIcon, Icon filterIcon) {
        // Se sortIcon è già un MultiSelectPopUp_CombinedIcon, estrai l'originale
        if (sortIcon instanceof MultiSelectPopUp_CombinedIcon) {
            MultiSelectPopUp_CombinedIcon ci = (MultiSelectPopUp_CombinedIcon) sortIcon;
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


    
}
