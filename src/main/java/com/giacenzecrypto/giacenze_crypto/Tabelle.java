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
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
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
    static Color rossoChiaro=new Color(255, 130, 130);
    //Colore di selezione delle righe: lo stesso blu dei pulsanti primari dei dialog (AppDialog:
    //accent #3478F6 nel tema chiaro, #4A90E2 nello scuro). Due sfumature vicine per riga pari/dispari,
    //così la selezione multipla non risulta piatta ma alternata come le righe normali. Testo bianco.
    static Color selezione        = new Color(52, 120, 246);   // #3478F6  chiaro, riga pari (accent dei dialog)
    static Color selezioneAlt     = new Color(78, 138, 247);   // #4E8AF7  chiaro, riga dispari
    static Color selezioneScura   = new Color(74, 144, 226);   // #4A90E2  scuro, riga pari (accent dei dialog)
    static Color selezioneScuraAlt= new Color(92, 156, 232);   // #5C9CE8  scuro, riga dispari
    //Evidenziatore "filtri attivi" sulla tabella movimenti: ambra scuro, leggibile con testo bianco
    //sia in tema chiaro sia in tema scuro (riempie lo sfondo di label/pulsante, non e' un foreground).
    static Color ambra=new Color(198, 128, 0);
    static Color bianco=new Color(255, 255, 255);
    static Color grigioChiaro=new Color(245, 245, 245);
    //Righe pari/dispari del tema scuro. Erano 70,70,70 e grigioScuro (64,64,64): 6 livelli
    //di differenza, cioè nessun motivo visibile, e su uno sfondo tabella #202020 stonavano.
    static Color grigio=new Color(0x2A2A2A);
    static Color grigioScuro=new Color(0x202020);
    static String Rosso="red";
    static String Verde="green";
    static Color gialloChiaro = new Color(255, 250, 180);
  //  static final Color bluRigaSelezionata = new Color(30, 60, 120);
  //  static final Color bluCellaSelezionata = new Color(70, 120, 200);

    //Questo serve per la funzione get SommeColonne e per fare in modo che il risultato dato sia l'ultimo eseguito
    private static final Map<JTable, AtomicInteger> versioniSomma = new ConcurrentHashMap<>();
    
    public static final Map<JTable, Map<Integer, RowFilter<DefaultTableModel, Integer>>> tableFilters = new HashMap<>();
    public static Map<JTable, Map<Integer, String>> SommaColonne = new HashMap<>();
    private static final Set<JTable> tabelleConFiltroColonne = new HashSet<>();


    
    
    /**
     * Applica alla tabella dei movimenti crypto un renderer che alterna lo sfondo delle righe (in base al
     * tema chiaro/scuro), colora in verde/rosso i depositi/prelievi e i valori positivi/negativi in colonna 11,
     * e mostra un'icona di allerta (con tooltip implicito) sulle righe con deposito/prelievo non classificato,
     * LIFO mancante o transazione senza prezzo — escludendo sempre le monete marcate come scam.
     * @param table la tabella dei movimenti crypto a cui applicare il renderer
     * @return la stessa tabella passata, con il renderer applicato
     */
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
        //rosso/verdeScuro sono gia' impostati per tema all'avvio (Giacenze_Crypto.main): nel tema
        //chiaro sono le varianti sature e scure (#C62828 / #1B5E20) leggibili su bianco.
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            fore=Color.lightGray;
        }
            else
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

        //Colorazione moneta/qta per direzione. La stessa renderer serve due tabelle con colonne diverse:
        // - "TabellaMovimentiCrypto" (movimenti): colonne di modello 8/10 = moneta/qta uscente -> rosso,
        //   11/13 = moneta/qta entrante -> verde. Una riga di scambio le ha entrambe.
        // - "DepositiPrelievi": una sola gamba per riga; la direzione la da la descrizione del tipo in
        //   colonna di modello 3 ("PRELIEVO ..." / "DEPOSITO ..."), e si colorano tipo+moneta+qta (3/4/5).
        //   Regressione 2026-08-28: passando i gate da indice di vista a indice di modello, il verde/rosso
        //   dei depositi/prelievi in questa tabella era sparito (indice 5 la' e' la Qta, non il tipo).
        String nomeTab = table.getName();
        boolean tabMovimenti = "TabellaMovimentiCrypto".equals(nomeTab);
        boolean tabDepPrel   = "DepositiPrelievi".equals(nomeTab);
        int mCol = table.convertColumnIndexToModel(col);
        int dirDP = 0; // -1 uscita, +1 entrata, 0 non determinata
        if (tabDepPrel) {
            Object tv = table.getModel().getValueAt(modelRow, 3);
            String tipoDP = tv == null ? "" : tv.toString().trim().toUpperCase();
            if (tipoDP.startsWith("PRELIEVO")) dirDP = -1;
            else if (tipoDP.startsWith("DEPOSITO")) dirDP = 1;
        }

            if (isSelected) {

                    setBackground(Tabelle.SfondoSelezione(row));
                }

                else if (tabMovimenti && (mCol == 8 || mCol == 10) && value != null && !value.toString().isBlank()) {
                    c.setBackground(bg);
                    setForeground(rosso);
                }
                else if (tabMovimenti && (mCol == 11 || mCol == 13) && value != null && !value.toString().isBlank()) {
                    c.setBackground(bg);
                    setForeground(verdeScuro);
                }
                else if (tabDepPrel && dirDP != 0 && (mCol == 3 || mCol == 4 || mCol == 5)) {
                    c.setBackground(bg);
                    setForeground(dirDP < 0 ? rosso : verdeScuro);
                }

                else if (!tabMovimenti && table.convertColumnIndexToModel(col)==5 &&
                        (value.toString().toLowerCase().equals("deposito crypto")||value.toString().toLowerCase().equals("deposito nft"))) {
                    setBackground(bg);
                    setForeground(verdeScuro);
                }
                else if (!tabMovimenti && table.convertColumnIndexToModel(col)==5 &&
                        (value.toString().toLowerCase().equals("prelievo crypto")||value.toString().toLowerCase().equals("prelievo nft"))) {
                    setBackground(bg);
                    setForeground(rosso);
                }
                else if (value!=null && table.convertColumnIndexToModel(col)==19 && value.toString().trim().contains("-")) {
                  //  bg = (row % 2 == 0 ? grigioChiaro : bianco);
                    c.setBackground(bg);
                    setForeground(rosso);
                } 
                else if (value!=null && table.convertColumnIndexToModel(col)==19 && !value.toString().trim().equalsIgnoreCase("0.00")) {
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
                if (table.getName()!=null&&table.convertColumnIndexToModel(col)==5 &&table.getName().equals("TabellaMovimentiCrypto")&&
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
                else if ((table.convertColumnIndexToModel(col) == 19 && !table.getModel().getValueAt(modelRow, 38).toString().isBlank())//Manca parte del LiFo
                        ||                     
                    (table.getModel().getColumnCount()>32 && table.convertColumnIndexToModel(col)==15&& table.getModel().getValueAt(modelRow, 32).toString().toUpperCase().contains("NO"))//Transazione senza prezzo
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
    

    
    /**
     * Ricostruisce il modello della tabella eliminando le righe duplicate (confronto testuale di tutti i
     * valori di ogni riga), preservando l'ordine di prima occorrenza.
     * @param table la tabella da cui rimuovere le righe duplicate
     */
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
    
    
    
    
        /**
         * Applica alla tabella 0 (riepilogo) di "Giacenze a data" un renderer che alterna lo sfondo delle righe
         * (in base al tema) e colora in rosso la colonna 6 e la colonna 4 quando contiene un valore negativo.
         * @param table la tabella a cui applicare il renderer
         * @return la stessa tabella passata, con il renderer applicato
         */
        public static JTable ColoraRigheTabella0GiacenzeaData(final JTable table) {

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                
             
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);


                    Color bg; 
        Color fore;
        if (Principale.tema.equalsIgnoreCase("Scuro")){
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
            if (isSelected) {

                    c.setBackground(Tabelle.SfondoSelezione(row));

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

    
        /**
         * Applica alla tabella 1 (dettaglio) di "Giacenze a data" un renderer che alterna lo sfondo delle righe
         * e colora in verde/rosso la colonna 5 in base al segno del valore, evidenziando in rosso la colonna 7
         * (giacenza negativa) con tooltip che riepiloga le rimanenze per wallet/gruppo/totale.
         * @param table la tabella a cui applicare il renderer
         * @return la stessa tabella passata, con il renderer applicato
         */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
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

                    c.setBackground(Tabelle.SfondoSelezione(row));
                    //c.revalidate();
                }
            else if (isSelected&&col==7&& value.toString().toLowerCase().contains("-")) {

                    c.setBackground(Tabelle.SfondoSelezione(row));
                    c.setForeground(rosso);
                }
            else if (isSelected&&col==7) {

                    c.setBackground(Tabelle.SfondoSelezione(row));
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
                    label.setForeground(Color.WHITE);
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
   
   /**
    * Ripristina la selezione su una riga e la posizione di scroll verticale di una tabella (es. dopo un
    * ricaricamento del modello che ne avrebbe altrimenti perso lo stato visivo).
    * @param tabella la tabella su cui ripristinare selezione e scroll
    * @param riga indice della riga da riselezionare
    * @param scrollValue valore della barra di scroll verticale da ripristinare
    */
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
   
        
   /**
    * Applica alla tabella un renderer che alterna lo sfondo delle righe (in base al tema) e mostra il testo in
    * grigio, usato per rappresentare visivamente dati non rilevanti/secondari.
    * @param table la tabella a cui applicare il renderer
    * @return la stessa tabella passata, con il renderer applicato
    */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
            if (isSelected) {

                    c.setBackground(Tabelle.SfondoSelezione(row));
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
    
/**
 * Applica alla tabella un renderer semplice che alterna lo sfondo delle righe in base al tema chiaro/scuro,
 * senza altre logiche di colorazione condizionale.
 * @param table la tabella a cui applicare il renderer
 * @return la stessa tabella passata, con il renderer applicato
 */
/**
 * Colore di sfondo della riga indicata nel motivo a righe alternate, secondo il tema attivo.
 * <p>È esposto perché i renderer scritti a mano fuori da questa classe — per esempio quello delle date
 * della tabella E-Money in {@code Principale} — devono usare esattamente lo stesso motivo delle altre
 * colonne, altrimenti la loro colonna stona.</p>
 * @param row indice di riga <em>di vista</em>
 * @return lo sfondo da usare per quella riga quando non è selezionata
 */
public static Color SfondoRigaAlternata(int row) {
    if (Principale.tema.equalsIgnoreCase("Scuro")) return (row % 2 == 0 ? grigio : grigioScuro);
    return (row % 2 == 0 ? grigioChiaro : bianco);
}

/**
 * Colore di sfondo di una riga <em>selezionata</em>: il blu accent dei dialog, alternato
 * riga pari/dispari come {@link #SfondoRigaAlternata(int)}, così una selezione multipla non
 * risulta una banda piatta. Usato da tutti i renderer di questa classe al posto di
 * {@code Tabelle.SfondoSelezione(row)}; il foreground di selezione (nero nel tema chiaro,
 * chiaro nello scuro) è impostato via {@code UIManager} all'avvio.
 * @param row indice di riga <em>di vista</em>
 */
public static Color SfondoSelezione(int row) {
    boolean scuro = Principale.tema != null && Principale.tema.equalsIgnoreCase("Scuro");
    if (scuro) return (row % 2 == 0 ? selezioneScura : selezioneScuraAlt);
    return (row % 2 == 0 ? selezione : selezioneAlt);
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            //fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
         //   fore=table.getForeground();
            //fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
          /*  if (isSelected) {
                c.setBackground(Tabelle.SfondoSelezione(row));
               // c.setForeground(table.getSelectionForeground());
            } else {
                c.setBackground(bg);
            }*/
           // if (table.getCellSelectionEnabled()) {
              /*  if (table.isRowSelected(row)) {
                    // tutta la riga selezionata
                    c.setBackground(Tabelle.SfondoSelezione(row));
                }else {
                    c.setBackground(bg);
                }*/
                if (table.isCellSelected(row, col)) {
                    // la cella selezionata deve prevalere
                    c.setBackground(Tabelle.SfondoSelezione(row));
                }else if (table.isRowSelected(row)) {
                    // tutta la riga selezionata
                    c.setBackground(Tabelle.SfondoSelezione(row).brighter());
                }else {
                    c.setBackground(bg);
                }
         /*   } else {
                // selezione classica per righe
                if (isSelected) {
                    c.setBackground(Tabelle.SfondoSelezione(row));
                } else {
                    c.setBackground(bg);
                }
            }*/

            return c;
        }
    };

    //Le colonne booleane hanno bisogno di un renderer proprio: quello predefinito di JTable disegna
    //la spunta ma ignora lo sfondo alternato, e la colonna resterebbe l'unica fuori dal motivo a righe
    TableCellRenderer rendererBooleano = new TableCellRenderer() {
        private final JCheckBox spunta = new JCheckBox();
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            spunta.setHorizontalAlignment(SwingConstants.CENTER);
            spunta.setOpaque(true);
            spunta.setSelected(value instanceof Boolean && (Boolean) value);
            spunta.setForeground(table.getForeground());
            Color bg;
            if (Principale.tema.equalsIgnoreCase("Scuro")) bg = (row % 2 == 0 ? grigio : grigioScuro);
            else bg = (row % 2 == 0 ? grigioChiaro : bianco);
            if (table.isCellSelected(row, col)) spunta.setBackground(Tabelle.SfondoSelezione(row));
            else if (table.isRowSelected(row)) spunta.setBackground(Tabelle.SfondoSelezione(row).brighter());
            else spunta.setBackground(bg);
            return spunta;
        }
    };

    // Configura il renderer per i tipi più comuni
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    table.setDefaultRenderer(Integer.class, renderer);
    table.setDefaultRenderer(Boolean.class, rendererBooleano);
    

    // Restituisci la tabella
    return table;
}

    /**
     * Installa un {@link TransferHandler} sulla tabella che, alla copia (Ctrl+C), serializza la selezione
     * corrente in testo delimitato da tabulazioni/a-capo, ripulendo eventuali celle con markup HTML tramite
     * {@link #stripHtml}.
     * @param table la tabella su cui installare il gestore di copia
     */
    public static void CopiaPulitadaTAG(final JTable table) {
        table.setTransferHandler(new TransferHandler() {

            /** Serializza la selezione corrente della tabella in testo delimitato da tabulazioni/a-capo, ripulendo l'eventuale HTML. */
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTable table = (JTable) c;

                int[] rows = table.getSelectedRows();
                int[] cols = table.getSelectedColumns();

                if (rows.length == 0 || cols.length == 0) {
                    return null;
                }

                StringBuilder sb = new StringBuilder();

                for (int r = 0; r < rows.length; r++) {
                    for (int cIdx = 0; cIdx < cols.length; cIdx++) {

                        Object value = table.getValueAt(rows[r], cols[cIdx]);
                        String text = value == null ? "" : value.toString();

                        if (text.contains("<html")) {
                            text = stripHtml(text);
                        }

                        sb.append(text);

                        if (cIdx < cols.length - 1) {
                            sb.append('\t'); // separatore colonne
                        }
                    }
                    if (r < rows.length - 1) {
                        sb.append('\n'); // separatore righe
                    }
                }

                return new StringSelection(sb.toString());
            }

            /** @return {@link TransferHandler#COPY}, unica azione di trasferimento supportata */
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

    }
    
    private static String stripHtml(String html) {
    if (html == null) return "";

    html = html.replaceAll("(?i)<br\\s*/?>", "\n");
    html = html.replaceAll("<[^>]*>", "");
    return html.trim();
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
                bg = (row % 2 == 0 ? grigio : grigioScuro);
                c.setForeground(Color.LIGHT_GRAY);   // testo standard tema scuro
            } else {
                bg = (row % 2 == 0 ? grigioChiaro : bianco);
                c.setForeground(Color.BLACK);        // testo standard tema chiaro
            }

            if (isSelected) {
                c.setBackground(Tabelle.SfondoSelezione(row));
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

/**
 * Applica a tutte le colonne della tabella un renderer che alterna lo sfondo delle righe in base al tema e
 * evidenzia in giallo la riga indicata (usato in "Modifica Prezzo" per segnalare la riga selezionata).
 * @param table1 la tabella a cui applicare il renderer
 * @param riga indice della riga da evidenziare in giallo
 */
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
                    c.setBackground(row % 2 == 0 ? grigio : grigioScuro);
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




/**
 * Applica alla tabella del dettaglio LIFO di una transazione un renderer che alterna lo sfondo delle righe (in
 * base al tema), colorando in rosso chiaro le righe della tabella "Uscita" e in verde chiaro quelle della
 * tabella "Entrata" quando la colonna booleana 7 è {@code true}, e mostrando un'icona di allerta sulla colonna
 * 2 quando il testo contiene "negativa" (nome della tabella riconosciuto tramite {@link JTable#getName()}).
 * @param table la tabella a cui applicare il renderer (deve chiamarsi {@code "Uscita"} o {@code "Entrata"} per la colorazione condizionale)
 * @return la stessa tabella passata, con il renderer applicato
 */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
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
                c.setBackground(Tabelle.SfondoSelezione(row));
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


/**
 * Ridimensiona un'icona alle dimensioni indicate, ridisegnandola su un'immagine bufferizzata e scalandola.
 * @param icon icona da ridimensionare
 * @param width larghezza desiderata in pixel
 * @param height altezza desiderata in pixel
 * @return la nuova icona ridimensionata
 */
public static Icon resizeIcon(Icon icon, int width, int height) {
    BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    icon.paintIcon(null, g2d, 0, 0);
    g2d.dispose();

    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    return new ImageIcon(scaled);
}


/**
 * Applica alla tabella un renderer che alterna lo sfondo delle righe (in base al tema) e colora il testo di
 * verde o rosso per le colonne indicate negli array {@code ColVerde}/{@code ColRosso}.
 * @param table la tabella a cui applicare il renderer
 * @param ColVerde indici delle colonne da colorare di verde
 * @param ColRosso indici delle colonne da colorare di rosso
 * @return la stessa tabella passata, con il renderer applicato
 */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }

            // Imposta il colore di sfondo alternato
            if (isSelected) {
                c.setBackground(Tabelle.SfondoSelezione(row));
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

/**
 * @param array array in cui cercare, può essere {@code null}
 * @param target valore da cercare
 * @return {@code true} se {@code target} è presente in {@code array}, {@code false} se non trovato o se {@code array} è {@code null}
 */
public static boolean contiene(int[] array, int target) {
    if (array==null)return false;
    for (int n : array) {
        if (n == target) return true;
    }
    return false;
}

/**
 * Applica alla tabella di dettaglio del quadro RT un renderer che alterna lo sfondo delle righe (in base al
 * tema) e colora in rosso il testo della colonna 10 e della colonna 7 quando contiene un valore negativo.
 * @param table la tabella a cui applicare il renderer
 * @return la stessa tabella passata, con il renderer applicato
 */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
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
                c.setBackground(Tabelle.SfondoSelezione(row));
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
    
/**
 * Applica alla tabella principale del quadro RT un renderer che alterna lo sfondo delle righe (in base al
 * tema) e colora in rosso il testo della colonna 3 quando contiene un valore negativo.
 * @param table la tabella a cui applicare il renderer
 * @return la stessa tabella passata, con il renderer applicato
 */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
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
                c.setBackground(Tabelle.SfondoSelezione(row));
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

/**
 * Renderer per la tabella pivot "Dettagli x Tipologia" del quadro RT: alterna lo sfondo delle righe in
 * base al tema, allinea a destra e colora in verde/rosso le celle numeriche (colonne &gt;= {@code primaColonnaNumerica})
 * secondo il segno del {@link BigDecimal} — usando {@code signum()} e non un test testuale sul "-", per non
 * sbagliare colore sui valori in notazione scientifica (bug M7). La riga dei totali (colonna 0 uguale a
 * {@code etichettaTotale}) è resa in grassetto. Celle {@code null} sono gestite senza NPE.
 *
 * @param table la tabella a cui applicare il renderer
 * @param primaColonnaNumerica indice della prima colonna che contiene valori numerici da colorare
 * @param etichettaTotale testo della colonna 0 che identifica la riga dei totali (in grassetto)
 * @return la stessa tabella passata, con il renderer applicato
 */
public static JTable ColoraTabellaPlusvalenzeMensili(final JTable table, int primaColonnaNumerica, String etichettaTotale) {
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            Color bg;
            Color fore;
            if (Principale.tema.equalsIgnoreCase("Scuro")) {
                bg = (row % 2 == 0 ? grigio : grigioScuro);
                fore = Color.lightGray;
            } else {
                bg = (row % 2 == 0 ? grigioChiaro : bianco);
                fore = table.getForeground();
            }

            boolean numerica = col >= primaColonnaNumerica;
            setHorizontalAlignment(numerica ? SwingConstants.RIGHT : SwingConstants.LEFT);

            boolean rigaTotale = false;
            Object c0 = table.getValueAt(row, 0);
            if (c0 != null && etichettaTotale != null && etichettaTotale.equals(c0.toString())) {
                rigaTotale = true;
            }
            setFont(rigaTotale ? table.getFont().deriveFont(java.awt.Font.BOLD) : table.getFont());

            if (isSelected) {
                c.setBackground(Tabelle.SfondoSelezione(row));
                setForeground(table.getSelectionForeground());
                return c;
            }
            c.setBackground(bg);

            int segno = 0;
            if (value instanceof BigDecimal) {
                segno = ((BigDecimal) value).signum();
            } else if (numerica && value != null && Funzioni_isNumericLocale(value.toString())) {
                segno = new BigDecimal(value.toString().trim()).signum();
            }
            if (numerica && segno < 0) {
                setForeground(rosso);
            } else if (numerica && segno > 0) {
                setForeground(verdeScuro);
            } else {
                setForeground(fore);
            }
            return c;
        }
    };
    table.setDefaultRenderer(Object.class, renderer);
    table.setDefaultRenderer(Double.class, renderer);
    table.setDefaultRenderer(BigDecimal.class, renderer);
    return table;
}

/** @return {@code true} se {@code s} è un numero interpretabile da {@link BigDecimal}. */
private static boolean Funzioni_isNumericLocale(String s) {
    if (s == null || s.isBlank()) return false;
    try {
        new BigDecimal(s.trim());
        return true;
    } catch (NumberFormatException ex) {
        return false;
    }
}



/**
 * @param table tabella da cui leggere la selezione
 * @return l'indice (nel model, non nella vista) della riga attualmente selezionata, oppure {@code -1} se nessuna riga è selezionata
 */
public static int Funzioni_getRigaSelezionata(JTable table) {
    int viewRow = table.getSelectedRow();
    if (viewRow == -1) {
        return -1; // Nessuna riga selezionata
    }
    return table.convertRowIndexToModel(viewRow);
}

/**
 * @param table tabella da cui leggere la selezione
 * @return gli indici (nel model, non nella vista) delle righe attualmente selezionate
 */
public static int[] Funzioni_getRigheSelezionate(JTable table) {
    int[] viewRows = table.getSelectedRows();
    int[] modelRows = new int[viewRows.length];

    for (int i = 0; i < viewRows.length; i++) {
        modelRows[i] = table.convertRowIndexToModel(viewRows[i]);
    }

    return modelRows;
}
       
           /**
            * Applica alla tabella un renderer che alterna lo sfondo delle righe (in base al tema) e colora in
            * rosso il testo di una riga se una delle colonne 2, 4 o 15 contiene la parola "error".
            * @param table la tabella a cui applicare il renderer
            * @return la stessa tabella passata, con il renderer applicato
            */
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
            bg= (row % 2 == 0  ? grigio : grigioScuro);
            fore=Color.lightGray;
        }
            else 
        {
            bg= (row % 2 == 0  ? grigioChiaro : bianco);
            fore=Color.BLACK;
        }
 
            if (isSelected) {

                    c.setBackground(Tabelle.SfondoSelezione(row));
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
       
       
       
       
       
       
       
       
       /**
        * Adatta l'altezza di ogni riga della tabella al contenuto renderizzato più alto tra tutte le sue colonne
        * (utile per celle multi-riga o con font/icone di dimensione variabile).
        * @param table la tabella di cui ricalcolare le altezze delle righe
        */
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

    
 
    
    /**
     * Calcola in background (thread separato) la somma dei valori numerici di ogni colonna attualmente
     * visibile nella tabella (rispettando l'eventuale filtro/ordinamento) e la memorizza in {@link #SommaColonne}
     * per essere mostrata nell'header. Usa un contatore di versione per tabella così che, se viene richiesto un
     * nuovo calcolo prima che il precedente termini, solo il risultato del calcolo più recente venga applicato.
     * @param table la tabella di cui calcolare le somme delle colonne
     */
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
                //Se nel frattempo è partito un ricalcolo più recente per questa tabella, questo
                //lavoro è già superato: mi fermo subito invece di completare tutte le colonne.
                //Il controllo è per colonna e non per cella: su tabelle grandi le celle sono
                //milioni, le colonne poche decine. Uscendo qui non viene scritto nulla, quindi
                //un thread superato non lascia stati parziali.
                if (versione.get() != versioneCorrente) {
                    return;
                }

                BigDecimal somma = BigDecimal.ZERO;

                for (int modelRow : visibleRows) {
                    try {
                        //NumeroONull scarta le celle non numeriche senza costruire l'eccezione:
                        //la maggior parte delle colonne è testuale (date, ID, simboli, indirizzi)
                        //e prima si pagava una NumberFormatException per ognuna di quelle celle
                        BigDecimal val = Funzioni.NumeroONull(model.getValueAt(modelRow, col));
                        if (val != null) {
                            somma = somma.add(val);
                        }
                    } catch (IndexOutOfBoundsException ignored) {
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
        if (tbl.getRowSorter() != null) {
            List<? extends RowSorter.SortKey> sortKeys = tbl.getRowSorter().getSortKeys();
            if (!sortKeys.isEmpty()) {
                RowSorter.SortKey primarySortKey = sortKeys.get(0);
                if (primarySortKey.getColumn() == modelCol) {
                    sortIcon = UIManager.getIcon(primarySortKey.getSortOrder() == SortOrder.ASCENDING
                            ? "Table.ascendingSortIcon"
                            : "Table.descendingSortIcon");
                }
            }
        }

        if (activeFilters != null && activeFilters.containsKey(modelCol)) {
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
        label.setHorizontalAlignment(JLabel.CENTER);
        //label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
        //label.setToolTipText("Tasto destro x filtrare " + Jsoup.parse(tbl.getColumnName(col)).text());
        label.setToolTipText("Tasto destro x filtrare \n\n" + htmlToTextWithLineBreaks(label.getText().replace("Somma:", "<br>Somma:")));

        return label;
    };
}


/**
 * Converte un frammento HTML in testo semplice, trasformando i tag {@code <br>} in ritorni a capo e
 * scartando ogni altro markup.
 * @param html markup HTML da convertire
 * @return il testo semplice risultante, senza spazi iniziali/finali
 */
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

 
 /**
  * Applica all'header della tabella il renderer completo (grassetto+centrato, icone di ordinamento e filtro),
  * inizializzando la mappa dei filtri attivi per questa tabella in {@link #tableFilters} se non già presente.
  * Da chiamare nel costruttore, dopo {@code initComponents()}, per le tabelle che useranno anche
  * {@link #Tabelle_FiltroColonne}.
  * @param table la tabella su cui inizializzare l'header
  */
 public static void Tabelle_InizializzaHeader(JTable table) {
    ImageIcon originalIco = new javax.swing.ImageIcon(Principale.class.getResource("/Images/24_Imbuto.png"));
    //Image image = Icone.svgImbuto.getImage();  // Ottiene l'immagine interna
   // ImageIcon originalIco = new ImageIcon(image);  // Converte in ImageIcon
   //  ImageIcon originalIco = Icone.Imbuto;
    //ImageIcon originalIco = new javax.swing.ImageIcon(getClass().getResource("/Images/24_Imbuto.png"));
    //Va adattata al tema PRIMA di rimpicciolirla: lo scaling produce un ImageIcon senza descrizione,
    //e la descrizione è l'unica cosa da cui Icone.Adatta capisce di quale file si tratta
    ImageIcon iconaTema = (ImageIcon) Icone.Adatta(originalIco);
    Image scaledImag = iconaTema.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH);
    Icon filterIco = new ImageIcon(scaledImag);
    Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.computeIfAbsent(table, k -> new HashMap<>());
    table.getTableHeader().setDefaultRenderer(Tabelle.Tabelle_creaNuovoHeaderRenderer(table, activeFilters, filterIco));
}

/**
 * Applica all'header della tabella un renderer leggero (solo centrato) senza le icone di ordinamento/filtro,
 * per le tabelle che non supportano il filtro colonne.
 * @param table la tabella su cui applicare il renderer
 */
public static void Tabelle_ApplicaHeaderBoldCentrato(JTable table) {
    TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
    table.getTableHeader().setDefaultRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
        JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
        label.setHorizontalAlignment(JLabel.CENTER);
        //label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
        return label;
    });
}

     /**
      * Applica al sorter della tabella la combinazione (AND logico) dei filtri per colonna già attivi
      * (in {@link #tableFilters}) più, se non vuoto, un filtro globale che verifica se una qualsiasi colonna
      * della riga contiene il testo indicato (case-insensitive). Ricalcola infine le somme colonna.
      * @param table la tabella a cui applicare il filtro combinato
      * @param sorter il sorter della tabella su cui impostare il filtro
      * @param globalFilterText testo del filtro globale, oppure {@code null}/vuoto per non applicarne uno
      */
     public static void Tabelle_applyCombinedFilter(JTable table, TableRowSorter<DefaultTableModel> sorter, String globalFilterText) {
    Map<Integer, RowFilter<DefaultTableModel, Integer>> filters = tableFilters.getOrDefault(table, Map.of());

    List<RowFilter<DefaultTableModel, Integer>> combinedFilters = new ArrayList<>(filters.values());

    if (globalFilterText != null && !globalFilterText.isEmpty()) {
        RowFilter<DefaultTableModel, Integer> globalFilter = new RowFilter<>() {
            /** @return {@code true} se una qualsiasi colonna della riga contiene (case-insensitive) il testo del filtro globale */
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

     
     




/**
 * Abilita il filtro colonne su una tabella: applica subito il filtro combinato corrente e, se non già fatto
 * in precedenza per questa tabella (tracciato in {@link #tabelleConFiltroColonne}), installa il click destro
 * sull'header per aprire il popup di selezione multipla dei valori di una colonna e filtrare di conseguenza.
 * Non riapplica il renderer dell'header (già impostato una volta da {@link #Tabelle_InizializzaHeader}).
 * @param table la tabella su cui abilitare il filtro colonne
 * @param filtro campo di testo del filtro globale (può essere {@code null})
 * @param popup popup di selezione multipla da usare per scegliere i valori da filtrare
 */
public static void Tabelle_FiltroColonne(JTable table, JTextField filtro, Tabelle_PopupSelezioneMultipla popup) {
    // Inizializza tableFilters se non esiste
    tableFilters.putIfAbsent(table, new HashMap<>());
    Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.get(table);

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
            /** @return un nome fisso, usato solo per riconoscere/loggare questo listener */
            @Override
            public String toString() {
                return "FiltroColonneMouseListener";
            }

            /** Apre, al click destro sull'header, il popup di selezione multipla dei valori della colonna cliccata. */
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
                                    /** @return {@code true} se il valore della colonna filtrata è tra quelli selezionati nel popup */
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
   
     
     
    /**
     * Rimuove tutti i filtri per colonna attivi su una tabella (svuotando la relativa mappa in
     * {@link #tableFilters}), azzera il {@link RowFilter} del sorter e forza il repaint dell'header per
     * togliere le eventuali icone/evidenziazioni residue.
     * @param table la tabella da cui rimuovere tutti i filtri
     */
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
    

//In teoria con queste 2 righe risolvo il tutto e non servono le righe sotto che ora commento
//tableFilters.putIfAbsent(table, new HashMap<>());
  //  Map<Integer, RowFilter<DefaultTableModel, Integer>> activeFilters = tableFilters.get(table);

  /*  table.setRowSorter(null);
    table.setRowSorter(new TableRowSorter<>(table.getModel()));*/
 //   tableFilters.put(table, activeFilters);
}

    /**
     * Svuota il modello di una tabella impostando direttamente il conteggio righe a zero (rapido).
     * @param modello il modello da svuotare
     */
    public static void Funzioni_PulisciTabella(DefaultTableModel modello) {
     /*   int z = modello.getRowCount();
        // System.out.println(modelProblemi.getRowCount());
        while (z != 0) {
            modello.removeRow(0);
            z = modello.getRowCount();
        }*/
        modello.setRowCount(0);
    }
    
            /**
             * Come {@link #Funzioni_PulisciTabella}, ma rimuovendo le righe una alla volta (più lento, mantenuto
             * per compatibilità con codice che si affida agli eventi di rimozione riga per riga).
             * @param modello il modello da svuotare
             */
            public static void Funzioni_PulisciTabellaLento(DefaultTableModel modello) {
        int z = modello.getRowCount();
        // System.out.println(modelProblemi.getRowCount());
        while (z != 0) {
            modello.removeRow(0);
            z = modello.getRowCount();
        }
       // modello.setRowCount(0);
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

    /** @return la somma delle larghezze delle due icone componenti (0 per quelle assenti) */
    @Override
    public int getIconWidth() {
        int w1 = sortIcon != null ? sortIcon.getIconWidth() : 0;
        int w2 = filterIcon != null ? filterIcon.getIconWidth() : 0;
        return w1 + w2;
    }

    /** @return la maggiore tra le altezze delle due icone componenti (0 per quelle assenti) */
    @Override
    public int getIconHeight() {
        int h1 = sortIcon != null ? sortIcon.getIconHeight() : 0;
        int h2 = filterIcon != null ? filterIcon.getIconHeight() : 0;
        return Math.max(h1, h2);
    }

    /** Disegna in sequenza l'icona di ordinamento e quella di filtro, affiancate orizzontalmente. */
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
