/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca.passelli
 */
public class Stampe {
      static String FilePDF="";
      static Document doc;
      static PdfWriter writer;

      // ═══════════════════════════════════════════════════════════════════════════════
      //  VESTE GRAFICA (copertina, testata, piede, filigrana) - opzionale
      //
      //  Si attiva con AttivaVesteGrafica() PRIMA di ApriDocumento() : chi non la chiama
      //  (stampe RT e le due di Principale) ottiene esattamente il PDF di prima.
      //
      //  REGOLA DA NON VIOLARE : tutto quello che sta qui sotto si disegna sul canvas
      //  (PdfContentByte), mai nel flusso del documento. La pagina del Quadro W porta 5
      //  strisce di modulo da ~105 pt piu' il rigo-titolo da ~129 pt e lascia poco piu' di
      //  100 pt liberi fra i margini : un elemento in flusso di troppo fa traboccare il
      //  quinto rigo sulla pagina dopo, e siccome la paginazione di Principale e' fissa a 5
      //  per foglio i valori finiscono sovrastampati sul rigo sbagliato - un modulo fiscale
      //  errato, non un difetto estetico. Sul Quadro RW il vincolo e' l'opposto e piu'
      //  stretto : il template A4 occupa da y=65,8 a y=805,9 e il suo inchiostro comincia
      //  21,5 pt sotto il proprio bordo, quindi restano 57,5 pt utili in testa (misurati).
      //  La testata ne usa 44 : alzarla oltre i 57 taglierebbe il logo del modulo.
      // ═══════════════════════════════════════════════════════════════════════════════

      /** Palette del logo : verde accento, verde forte, nero, grigi. */
      static final Color VERDE        = new Color(0xA8, 0xA8, 0x34);
      static final Color VERDE_SCURO  = new Color(0x8A, 0x8A, 0x1F);
      static final Color NERO         = new Color(0x0A, 0x0A, 0x0A);
      static final Color GRIGIO_TESTO = new Color(0x55, 0x55, 0x55);
      static final Color GRIGIO_TENUE = new Color(0x99, 0x99, 0x99);
      static final Color GRIGIO_FILO  = new Color(0xDD, 0xDD, 0xDD);
      static final Color FASCIA_MARGINE = new Color(0xEC, 0xEC, 0xD2);

      /** Facce del font dell'applicazione, caricate dal jar alla prima stampa che le usa. */
      private static BaseFont bfRegular, bfBold;
      private static boolean fontHtmlRegistrato = false;

      /**
       * Cornice attiva sul documento corrente. <b>Statica come {@link #doc} e {@link #writer}</b>, e
       * azzerata nel costruttore: la classe tiene il documento in campi statici, quindi un campo
       * d'istanza qui darebbe una coppia disallineata — un secondo {@code Stampe} rimpiazzerebbe
       * {@code writer} lasciando la cornice del primo a puntare a quello vecchio. Azzerarla nel
       * costruttore e' cio' che impedisce l'errore opposto, cioe' che una stampa senza veste (RT,
       * o le due di {@code Principale}) erediti la cornice di una stampa RW precedente.
       */
      static CorniceReport cornice = null;
      
     public Stampe(String PDFPath) throws FileNotFoundException {
       FilePDF=PDFPath;
       cornice = null;   //ogni documento riparte senza veste grafica : vedi il campo
       doc = new Document();
       writer = PdfWriter.getInstance(doc, new FileOutputStream(FilePDF));
     //  doc.open();  
       
     } 
     
     /** Apre il documento PDF per la scrittura (da chiamare prima di aggiungere qualsiasi contenuto). */
     public void ApriDocumento(){
        doc.open();
     }

    /** Chiude il documento e lo scrive su disco, poi lo apre con l'applicazione predefinita del sistema se disponibile. */
    public void ScriviPDF(){
    try {
      doc.close();
      writer.close();
      File file = new File(FilePDF);
      if(Desktop.isDesktopSupported()&&file.exists())//check if Desktop is supported by Platform or not  
{  
    Desktop desktop = Desktop.getDesktop();  
    desktop.open(file);              //opens the specified file  
}  
    } catch (DocumentException  e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }     catch (IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
     }
    
    /** Forza l'inizio di una nuova pagina nel documento PDF. */
    public void NuovaPagina(){
        doc.newPage();
    }

    /** @param piendino testo del piè di pagina, mostrato allineato a destra su ogni pagina */
    public void Piede(String piendino){
    //    doc.setFooter(new HeaderFooter(piedino,piedino));
        Font font = new Font(Font.HELVETICA, 6, Font.NORMAL);       
        HeaderFooter footer = new HeaderFooter(new Phrase(piendino,font), false);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(Rectangle.NO_BORDER);    
        doc.setFooter(footer);
        
    } 
    
    /** @param Titolo titolo da impostare nei metadati del documento PDF */
    public void AggiungiTitolo(String Titolo){
        doc.addTitle(Titolo);
     }

    /**
     * Aggiunge al PDF una pagina del quadro W/RW (dichiarazione patrimoniale), sovrapponendo all'immagine del
     * modulo fornita i dati del wallet (numero quadro, codice possesso/individuazione bene/quota fissi, valore
     * iniziale/finale, giorni di detenzione), scalando l'immagine alla larghezza della pagina.
     * @param Immagine percorso dell'immagine del modulo su cui sovrapporre i dati
     * @param NumeroQuadro numero progressivo del quadro W
     * @param ValoreIniziale valore iniziale del wallet (senza decimali, aggiunti automaticamente come {@code ",00"})
     * @param ValoreFinale valore finale del wallet (senza decimali, aggiunti automaticamente come {@code ",00"})
     * @param Giorni giorni di detenzione ai fini IVAFE; se vuoto o tra parentesi, marca la riga come "solo monitoraggio"
     */
    public void AggiungiQuadroW(String Immagine,String NumeroQuadro,String ValoreIniziale,String ValoreFinale,String Giorni) {
        AggiungiQuadroW(Immagine, NumeroQuadro, ValoreIniziale, ValoreFinale, Giorni, "21", "", false);
    }

    /**
     * Variante del rigo Quadro W con codice individuazione bene, codice Stato estero e "solo monitoraggio"
     * espliciti : usata dalla parte FIAT (valuta estera presso intermediario estero, codice bene 14).
     * Con {@code CodiceBene="21"}, {@code StatoEstero=""} e {@code SoloMonitoraggio=false} riproduce il
     * comportamento cripto.
     * @param CodiceBene codice colonna 3 ("21" cripto, "14" valuta estera)
     * @param StatoEstero codice Stato estero (colonna 4) ; se vuoto la colonna resta vuota
     * @param SoloMonitoraggio se {@code true} : colonna 16 sempre barrata e colonna 14 (codice) lasciata vuota
     */
    public void AggiungiQuadroW(String Immagine,String NumeroQuadro,String ValoreIniziale,String ValoreFinale,String Giorni,
                String CodiceBene,String StatoEstero,boolean SoloMonitoraggio) {
          try {


// String Errore="Attenzione per questo wallet ci sono degli errori da correggere!";
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
             // image01.s
             float LarghezzaPagina=doc.getPageSize().getWidth()-doc.rightMargin()-doc.leftMargin();
             float LarghezzaImmagine=image01.getWidth();
             float PercentualeScala=LarghezzaPagina/LarghezzaImmagine*100;
             image01.scalePercent(PercentualeScala);
             //doc.bottom()
             doc.add(image01);
             float psosizioneVeriticale=writer.getVerticalPosition(false);
            // Paragraph par = new Paragraph("155",FontFactory.getFont(FontFactory.COURIER,6, Font.NORMAL));
             Font font = new Font(Font.HELVETICA, 6, Font.BOLD);
             //Numero Quadro
             setPara(writer.getDirectContent(), new Phrase("W"+NumeroQuadro,font), doc.leftMargin(), psosizioneVeriticale+45);
             font = new Font(Font.HELVETICA, 8, Font.NORMAL);
             //Codice Possesso
             setPara(writer.getDirectContent(), new Phrase("1",font), 40+doc.leftMargin(), psosizioneVeriticale+75);
             //Codice Individuazione Bene (col 3)
             setPara(writer.getDirectContent(), new Phrase(CodiceBene,font), 140+doc.leftMargin(), psosizioneVeriticale+75);
             //Codice Stato Estero (col 4) - solo per i righi FIAT ; x approssimativa, da tarare sul modulo reale
             if (StatoEstero!=null && !StatoEstero.isBlank()){
                setPara(writer.getDirectContent(), new Phrase(StatoEstero,font), 190+doc.leftMargin(), psosizioneVeriticale+75);
             }
             //Quota di Possesso
             setPara(writer.getDirectContent(), new Phrase("100,00",font), 245+doc.leftMargin(), psosizioneVeriticale+75);
             //Criterio Determinazione Valore
             setPara(writer.getDirectContent(), new Phrase("1",font), 310+doc.leftMargin(), psosizioneVeriticale+75);
             //Valore Iniziale
             setPara(writer.getDirectContent(), new Phrase(ValoreIniziale+",00",font), 370+doc.leftMargin(), psosizioneVeriticale+75);
             //Valore Finale
             setPara(writer.getDirectContent(), new Phrase(ValoreFinale+",00",font), 460+doc.leftMargin(), psosizioneVeriticale+75);
             //Giorni IVAFE
             setPara(writer.getDirectContent(), new Phrase(Giorni,font), 130+doc.leftMargin(), psosizioneVeriticale+40);
             //Codice 14 (col 14) : non compilato sui righi solo-monitoraggio (FIAT)
             if (!SoloMonitoraggio){
                setPara(writer.getDirectContent(), new Phrase("vedi",font), 420+doc.leftMargin(), psosizioneVeriticale+45);
                setPara(writer.getDirectContent(), new Phrase("note",font), 420+doc.leftMargin(), psosizioneVeriticale+38);
             }
             //Solo Monitoraggio (col 16)
             if (SoloMonitoraggio||Giorni.isBlank()||Giorni.contains("(")){
                setPara(writer.getDirectContent(), new Phrase("X",font), 505+doc.leftMargin(), psosizioneVeriticale+40);
             }

             //Font font = new Font(Font.HELVETICA, 6, Font.NORMAL);       
             //HeaderFooter footer = new HeaderFooter(new Phrase("155",font), false);
             
             //doc.add(image01);
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
        
    
    /**
     * Importa la prima pagina di un PDF esterno come template e la inserisce come contenuto nel documento
     * corrente tramite {@link PdfContentByte#addTemplate}.
     * @param FontePDF percorso del PDF sorgente da cui importare la prima pagina
     */
    public void InserisciPDF(String FontePDF) {
        try {
            PdfReader reader = new PdfReader(FontePDF);
           // Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream("output.pdf"));

          //  doc.open();

            PdfContentByte cb = writer.getDirectContent();
            PdfImportedPage page = writer.getImportedPage(reader, 1);

// Inserisce la pagina come contenuto
            cb.addTemplate(page, 0, 0);

           // doc.close();
        } catch (IOException ex) {
            System.getLogger(Stampe.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    public void AggiungiQuadroRW(String Immagine,
                String NumeroQuadro,
                String ValoriIniziali[],
                String ValoriFinali[],
                String Giorni[],
                String IC[],
                String Wallet[],
                String Note[],
                int foglio,
                String ICTot) {
        AggiungiQuadroRW(Immagine, NumeroQuadro, ValoriIniziali, ValoriFinali, Giorni, IC, Wallet, Note, foglio, ICTot, null, null);
    }

    /**
     * Variante del foglio Quadro RW con codice individuazione bene e codice Stato estero per rigo :
     * usata dalla parte FIAT. {@code CodiceBene}/{@code StatoEstero} a {@code null} (o {@code null}
     * per il singolo indice) = comportamento cripto (codice "21").
     *
     * <p>{@code SoloMonitoraggio[i]} decide se barrare la colonna 16 e lasciare vuote colonna 14 e
     * IC. A {@code null} si ricade sul vecchio criterio {@code "14".equals(CodiceBene[i])}, che vale
     * per i chiamanti che non lo passano ma <b>non</b> e' piu' vero in generale : sulla liquidita' in
     * valuta (codice 14) l'IVAFE ordinaria puo' essere dovuta, quindi chi conosce il regime del rigo
     * deve passarlo esplicitamente invece di lasciarlo dedurre dal codice.</p>
     */
    public void AggiungiQuadroRW(String Immagine,
                String NumeroQuadro,
                String ValoriIniziali[],
                String ValoriFinali[],
                String Giorni[],
                String IC[],
                String Wallet[],
                String Note[],
                int foglio,
                String ICTot,
                String CodiceBene[],
                String StatoEstero[]) {
        AggiungiQuadroRW(Immagine, NumeroQuadro, ValoriIniziali, ValoriFinali, Giorni, IC, Wallet,
                Note, foglio, ICTot, CodiceBene, StatoEstero, null);
    }

    /** Come sopra, con il regime dichiarativo esplicito per rigo. */
    public void AggiungiQuadroRW(String Immagine,
                String NumeroQuadro,
                String ValoriIniziali[],
                String ValoriFinali[],
                String Giorni[],
                String IC[],
                String Wallet[],
                String Note[],
                int foglio,
                String ICTot,
                String CodiceBene[],
                String StatoEstero[],
                boolean SoloMonitoraggio[]) {
          try {
              ICTot=Funzioni.formattaBigDecimal(new BigDecimal(ICTot),false);

// String Errore="Attenzione per questo wallet ci sono degli errori da correggere!";
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
             // image01.s
             float LarghezzaPagina=doc.getPageSize().getWidth()-doc.rightMargin()-doc.leftMargin();
             float LarghezzaImmagine=image01.getWidth();
             float PercentualeScala=LarghezzaPagina/LarghezzaImmagine*100;
             image01.scalePercent(PercentualeScala);
             //doc.bottom()
             //doc.left(110);
             //doc.setMargins(doc.leftMargin()+100, doc.rightMargin(), doc.topMargin(), doc.bottomMargin());
             doc.add(image01);
             float psosizioneVeriticale=writer.getVerticalPosition(false);
            // Paragraph par = new Paragraph("155",FontFactory.getFont(FontFactory.COURIER,6, Font.NORMAL));
             Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
             //Foglio
             setPara(writer.getDirectContent(), new Phrase(String.valueOf(foglio),font), 507+doc.leftMargin(), psosizioneVeriticale+582);
             //RW8 se foglio 1
             if (foglio==1){                 
                 font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                 //RW8 : SOLO la colonna 1 (totale imposta dovuta). La colonna 5 (imposta a debito)
                 //vale col.1 - col.2 + col.3 - col.4 e non va versata sotto i 12 euro (istruzioni
                 //Redditi PF 2026, fascicolo 2 p. 53): le colonne 2-4 vengono dalle dichiarazioni
                 //precedenti e il programma non le conosce, quindi ci scriveva un debito giusto solo
                 //per chi non ha eccedenze ne' acconti. Ora resta vuota, con la formula nelle note.
                 setPara(writer.getDirectContent(), new Phrase(ICTot,font), 150+doc.leftMargin(), psosizioneVeriticale+12);

             }
             font = new Font(Font.HELVETICA, 10, Font.BOLD);
             setPara(writer.getDirectContent(), new Phrase("QUADRO RW PER CRIPTO-ATTIVITA'",font), 200+doc.leftMargin(), psosizioneVeriticale+655);
             for (int i=0;i<5;i++){
                 if (ValoriIniziali[i]!=null){
                   // font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                   ValoriIniziali[i]=Funzioni.formattaBigDecimal(new BigDecimal(ValoriIniziali[i]),false);
                        ValoriFinali[i]=Funzioni.formattaBigDecimal(new BigDecimal(ValoriFinali[i]),false);
                    //Codice individuazione bene / Stato estero per rigo : "14" = valuta estera FIAT (solo monitoraggio)
                    String cb=(CodiceBene!=null && CodiceBene[i]!=null)?CodiceBene[i]:"21";
                    String se=(StatoEstero!=null && StatoEstero[i]!=null)?StatoEstero[i]:"";
                    boolean monit = (SoloMonitoraggio != null && i < SoloMonitoraggio.length)
                            ? SoloMonitoraggio[i] : "14".equals(cb);
                    if (i==0){
                        //Wallet e Note
                        font = new Font(Font.HELVETICA, 10, Font.BOLD);
                        ColonnaNomeENota(Wallet[i], Note[i], psosizioneVeriticale+525, LARG_COLONNA_RW_IMMAGINE,
                                5+doc.leftMargin(), psosizioneVeriticale+510);
                        font = new Font(Font.HELVETICA, 8, Font.NORMAL);

                        //Codice Possesso
                        setPara(writer.getDirectContent(), new Phrase("1",font), 142+doc.leftMargin(), psosizioneVeriticale+540);
                        //Codice Individuazione Bene (col 3)
                        setPara(writer.getDirectContent(), new Phrase(cb,font), 220+doc.leftMargin(), psosizioneVeriticale+540);
                        //Codice Stato Estero (col 4) - solo FIAT ; x approssimativa, da tarare sul modulo reale
                        if (!se.isBlank()) setPara(writer.getDirectContent(), new Phrase(se,font), 252+doc.leftMargin(), psosizioneVeriticale+540);
                        //Quota di Possesso
                        setPara(writer.getDirectContent(), new Phrase("100,00",font), 285+doc.leftMargin(), psosizioneVeriticale+540);
                        //Criterio Determinazione Valore
                        setPara(writer.getDirectContent(), new Phrase("1",font), 330+doc.leftMargin(), psosizioneVeriticale+540);
                        //Valore Iniziale
                        setPara(writer.getDirectContent(), new Phrase(ValoriIniziali[i],font), 380+doc.leftMargin(), psosizioneVeriticale+540);
                        //Valore Finale
                        setPara(writer.getDirectContent(), new Phrase(ValoriFinali[i],font), 460+doc.leftMargin(), psosizioneVeriticale+540);
                        //Giorni IVAFE
                        setPara(writer.getDirectContent(), new Phrase(Giorni[i],font), 210+doc.leftMargin(), psosizioneVeriticale+508);
                        //Codice 14 (col 14) : non compilato sui righi FIAT (solo monitoraggio)
                        if (!monit){
                            setPara(writer.getDirectContent(), new Phrase("vedi",font), 425+doc.leftMargin(), psosizioneVeriticale+513);
                            setPara(writer.getDirectContent(), new Phrase("note",font), 425+doc.leftMargin(), psosizioneVeriticale+506);
                        }
                        //Solo Monitoraggio (col 16)
                        if (monit||Giorni[i].isBlank()||Giorni[i].contains("(")){
                            setPara(writer.getDirectContent(), new Phrase("X",font), 496+doc.leftMargin(), psosizioneVeriticale+508);
                        }else{
                        //IC
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 410 + doc.leftMargin(), psosizioneVeriticale + 445);
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 475 + doc.leftMargin(), psosizioneVeriticale + 445);
                        }
                    }else{
                        //Wallet e Note
                        font = new Font(Font.HELVETICA, 10, Font.BOLD);
                        ColonnaNomeENota(Wallet[i], Note[i], psosizioneVeriticale+410-(i-1)*84, LARG_COLONNA_RW_IMMAGINE,
                                5+doc.leftMargin(), psosizioneVeriticale+395-(i-1)*84);
                        font = new Font(Font.HELVETICA, 8, Font.NORMAL);

                        //Codice Possesso
                        setPara(writer.getDirectContent(), new Phrase("1",font), 142+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Codice Individuazione Bene (col 3)
                        setPara(writer.getDirectContent(), new Phrase(cb,font), 220+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Codice Stato Estero (col 4) - solo FIAT ; x approssimativa
                        if (!se.isBlank()) setPara(writer.getDirectContent(), new Phrase(se,font), 252+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Quota di Possesso
                        setPara(writer.getDirectContent(), new Phrase("100,00",font), 285+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Criterio Determinazione Valore
                        setPara(writer.getDirectContent(), new Phrase("1",font), 330+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Valore Iniziale
                        setPara(writer.getDirectContent(), new Phrase(ValoriIniziali[i],font), 380+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Valore Finale
                        setPara(writer.getDirectContent(), new Phrase(ValoriFinali[i],font), 460+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Giorni IVAFE
                        setPara(writer.getDirectContent(), new Phrase(Giorni[i],font), 210+doc.leftMargin(), psosizioneVeriticale+404-(i-1)*84);
                        //Codice 14 (col 14) : non compilato sui righi FIAT (solo monitoraggio)
                        if (!monit){
                            setPara(writer.getDirectContent(), new Phrase("vedi",font), 425+doc.leftMargin(), psosizioneVeriticale+409-(i-1)*84);
                            setPara(writer.getDirectContent(), new Phrase("note",font), 425+doc.leftMargin(), psosizioneVeriticale+402-(i-1)*84);
                        }
                        //Solo Monitoraggio (col 16)
                        if (monit||Giorni[i].isBlank()||Giorni[i].contains("(")){
                            setPara(writer.getDirectContent(), new Phrase("X",font), 496+doc.leftMargin(), psosizioneVeriticale+404-(i-1)*84);
                        }else{
                        //IC
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 410 + doc.leftMargin(), psosizioneVeriticale +360-(i-1)*84);
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 475 + doc.leftMargin(), psosizioneVeriticale +360-(i-1)*84);
                        }
                    }
                    //setPara(writer.getDirectContent(), new Phrase("1",font), 140+doc.leftMargin(), psosizioneVeriticale+630-i*100);
                 }
             }
         /*  

             
             //Solo Monitoraggio
             if (Giorni.isBlank()||Giorni.contains("(")){
                setPara(writer.getDirectContent(), new Phrase("X",font), 495+doc.leftMargin(), psosizioneVeriticale+85);
             }else{
             //Adesso calcolo l'IC per poterla mettere nei campi appositi
             String IC = new BigDecimal(ValoreFinale)
                         .multiply(new BigDecimal(Giorni))
                         .multiply(new BigDecimal(0.002))
                         .divide(new BigDecimal(365), 0, RoundingMode.HALF_UP).toPlainString() + ",00";
                 setPara(writer.getDirectContent(), new Phrase(IC, font), 380 + doc.leftMargin(), psosizioneVeriticale + 8);
                 setPara(writer.getDirectContent(), new Phrase(IC, font), 460 + doc.leftMargin(), psosizioneVeriticale + 8);
             }*/

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
        
    
    public void AggiungiQuadroRW2025(String FilePdf,
            String NumeroQuadro,
            String ValoriIniziali[],
            String ValoriFinali[],
            String Giorni[],
            String IC[],
            String Wallet[],
            String Note[],
            int foglio,
            String ICTot) {
        AggiungiQuadroRW2025(FilePdf, NumeroQuadro, ValoriIniziali, ValoriFinali, Giorni, IC, Wallet, Note, foglio, ICTot, null, null);
    }

    /** Variante 2025 con codice individuazione bene / Stato estero per rigo (parte FIAT). */
    public void AggiungiQuadroRW2025(String FilePdf,
            String NumeroQuadro,
            String ValoriIniziali[],
            String ValoriFinali[],
            String Giorni[],
            String IC[],
            String Wallet[],
            String Note[],
            int foglio,
            String ICTot,
            String CodiceBene[],
            String StatoEstero[]) {
        AggiungiQuadroRW2025(FilePdf, NumeroQuadro, ValoriIniziali, ValoriFinali, Giorni, IC, Wallet,
                Note, foglio, ICTot, CodiceBene, StatoEstero, null);
    }

    /**
     * Come sopra, con il regime dichiarativo esplicito per rigo : {@code SoloMonitoraggio[i]} decide
     * la barratura della colonna 16. A {@code null} vale il vecchio criterio
     * {@code "14".equals(CodiceBene[i])} — vedi la nota su {@link #AggiungiQuadroRW} : sul codice 14
     * l'imposta puo' essere dovuta, quindi dedurla dal codice non e' piu' corretto.
     */
    public void AggiungiQuadroRW2025(String FilePdf,
            String NumeroQuadro,
            String ValoriIniziali[],
            String ValoriFinali[],
            String Giorni[],
            String IC[],
            String Wallet[],
            String Note[],
            int foglio,
            String ICTot,
            String CodiceBene[],
            String StatoEstero[],
            boolean SoloMonitoraggio[]) {
    try {
        ICTot = Funzioni.formattaBigDecimal(new BigDecimal(ICTot), false);

        // ── CARICAMENTO PDF DI SFONDO ──────────────────────────────────────
        PdfReader pdfReader = new PdfReader(FilePdf);
        PdfImportedPage paginaSfondo = writer.getImportedPage(pdfReader, 1);

        float larghezzaPagina = doc.getPageSize().getWidth()
                                - doc.rightMargin()
                                - doc.leftMargin();
        float altezzaPagina   = doc.getPageSize().getHeight()
                                - doc.topMargin()
                                - doc.bottomMargin();

        // Calcola scala proporzionale in base alla larghezza
        float scalaX = larghezzaPagina / paginaSfondo.getWidth();
        float scalaY = altezzaPagina   / paginaSfondo.getHeight();
        float scala  = Math.min(scalaX, scalaY); // mantieni proporzioni

        // Disegna il PDF di sfondo direttamente sul canvas
        PdfContentByte cb = writer.getDirectContentUnder(); // SOTTO il testo
        cb.saveState();
        cb.addTemplate(
            paginaSfondo,
            scala, 0, 0, scala,          // trasformazione (scaleX, 0, 0, scaleY)
            doc.leftMargin(),             // X origine
            doc.bottomMargin()            // Y origine
        );
        cb.restoreState();

        // ── POSIZIONE VERTICALE DI RIFERIMENTO (come prima) ───────────────
        // Poiché non aggiungiamo l'immagine tramite doc.add(),
        // la posizione verticale va calcolata manualmente dalla cima
        float psosizioneVeriticale = doc.getPageSize().getHeight()
                                     - doc.topMargin()
                                     - (paginaSfondo.getHeight() * scala);

        // ── DA QUI IN POI: TUTTO IDENTICO ALLA VERSIONE ORIGINALE ─────────
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD);

        // Foglio
         setPara(writer.getDirectContent(), new Phrase(String.valueOf(foglio), font),
                490 + doc.leftMargin(), psosizioneVeriticale + 608);
         //507-582
         //-17    .....   +26

        // RW8 se foglio 1 : SOLO la colonna 1 (totale imposta dovuta), vedi ICTot
        if (foglio == 1) {
            font = new Font(Font.HELVETICA, 8, Font.NORMAL);
            setPara(writer.getDirectContent(), new Phrase(ICTot, font),
                    133 + doc.leftMargin(), psosizioneVeriticale + 38);
        }

        font = new Font(Font.HELVETICA, 10, Font.BOLD);
        setPara(writer.getDirectContent(),
                new Phrase("QUADRO RW PER CRIPTO-ATTIVITA'", font),
                183 + doc.leftMargin(), psosizioneVeriticale + 681);

        for (int i = 0; i < 5; i++) {
            if (ValoriIniziali[i] != null) {
                ValoriIniziali[i] = Funzioni.formattaBigDecimal(
                        new BigDecimal(ValoriIniziali[i]), false);
                ValoriFinali[i]   = Funzioni.formattaBigDecimal(
                        new BigDecimal(ValoriFinali[i]), false);

                String cbene = (CodiceBene != null && CodiceBene[i] != null) ? CodiceBene[i] : "21";
                String sest = (StatoEstero != null && StatoEstero[i] != null) ? StatoEstero[i] : "";
                boolean monit = (SoloMonitoraggio != null && i < SoloMonitoraggio.length)
                        ? SoloMonitoraggio[i] : "14".equals(cbene);

                if (i == 0) {
                    ColonnaNomeENota(Wallet[i], Note[i], psosizioneVeriticale + 551, LARG_COLONNA_RW_2025,
                            -15 + doc.leftMargin(), psosizioneVeriticale + 536);
                    font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                    setPara(writer.getDirectContent(), new Phrase("1", font),
                            125 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase(cbene, font),
                            203 + doc.leftMargin(), psosizioneVeriticale + 566);
                    if (!sest.isBlank()) setPara(writer.getDirectContent(), new Phrase(sest, font),
                            235 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase("100,00", font),
                            268 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase("1", font),
                            313 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase(ValoriIniziali[i], font),
                            363 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase(ValoriFinali[i], font),
                            443 + doc.leftMargin(), psosizioneVeriticale + 566);
                    setPara(writer.getDirectContent(), new Phrase(Giorni[i], font),
                            191 + doc.leftMargin(), psosizioneVeriticale + 534);
                    if (!monit) {
                        setPara(writer.getDirectContent(), new Phrase("vedi", font),
                                407 + doc.leftMargin(), psosizioneVeriticale + 539);
                        setPara(writer.getDirectContent(), new Phrase("note", font),
                                407 + doc.leftMargin(), psosizioneVeriticale + 532);
                    }
                    if (monit || Giorni[i].isBlank() || Giorni[i].contains("(")) {
                        setPara(writer.getDirectContent(), new Phrase("X", font),
                                480 + doc.leftMargin(), psosizioneVeriticale + 531);
                    } else {
                        setPara(writer.getDirectContent(), new Phrase(IC[i], font),
                                393 + doc.leftMargin(), psosizioneVeriticale + 471);
                        setPara(writer.getDirectContent(), new Phrase(IC[i], font),
                                458 + doc.leftMargin(), psosizioneVeriticale + 471);
                    }
                } else {
                    ColonnaNomeENota(Wallet[i], Note[i], psosizioneVeriticale + 436 - (i - 1) * 84,
                            LARG_COLONNA_RW_2025, -15 + doc.leftMargin(), psosizioneVeriticale + 421 - (i - 1) * 84);
                    font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                    setPara(writer.getDirectContent(), new Phrase("1", font),
                            125 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase(cbene, font),
                            203 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    if (!sest.isBlank()) setPara(writer.getDirectContent(), new Phrase(sest, font),
                            235 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase("100,00", font),
                            268 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase("1", font),
                            313 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase(ValoriIniziali[i], font),
                            363 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase(ValoriFinali[i], font),
                            443 + doc.leftMargin(), psosizioneVeriticale + 451 - (i - 1) * 84);
                    setPara(writer.getDirectContent(), new Phrase(Giorni[i], font),
                            191 + doc.leftMargin(), psosizioneVeriticale + 430 - (i - 1) * 84);
                    if (!monit) {
                        setPara(writer.getDirectContent(), new Phrase("vedi", font),
                                408 + doc.leftMargin(), psosizioneVeriticale + 435 - (i - 1) * 84);
                        setPara(writer.getDirectContent(), new Phrase("note", font),
                                408 + doc.leftMargin(), psosizioneVeriticale + 428 - (i - 1) * 84);
                    }
                    if (monit || Giorni[i].isBlank() || Giorni[i].contains("(")) {
                        setPara(writer.getDirectContent(), new Phrase("X", font),
                                480 + doc.leftMargin(), psosizioneVeriticale + 427 - (i - 1) * 84);
                    } else {
                        setPara(writer.getDirectContent(), new Phrase(IC[i], font),
                                393 + doc.leftMargin(), psosizioneVeriticale + 386 - (i - 1) * 84);
                        setPara(writer.getDirectContent(), new Phrase(IC[i], font),
                                458 + doc.leftMargin(), psosizioneVeriticale + 386 - (i - 1) * 84);
                    }
                }
            }
        }

        // Chiudi il reader per liberare risorse
        pdfReader.close();

    } catch (IOException ex) {
        Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
    }
}
    
    
    
    
         /**
          * Aggiunge al PDF una pagina del rigo RW8 (crypto-attività), sovrapponendo il valore indicato
          * all'immagine del modulo fornita.
          * @param Immagine percorso dell'immagine del modulo su cui sovrapporre i dati
          * @param Valore valore da riportare nel rigo
          */
         public void AggiungiRW8(String Immagine,String Valore) {
          try {
              

// String Errore="Attenzione per questo wallet ci sono degli errori da correggere!";
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
             // image01.s
             float LarghezzaPagina=doc.getPageSize().getWidth()-doc.rightMargin()-doc.leftMargin();
             float LarghezzaImmagine=image01.getWidth();
             float PercentualeScala=LarghezzaPagina/LarghezzaImmagine*100;
             image01.scalePercent(PercentualeScala);
             //doc.bottom()
             doc.add(image01);
             float psosizioneVeriticale=writer.getVerticalPosition(false);        
             Font font = new Font(Font.HELVETICA, 8, Font.NORMAL); 
             //Codice Possesso
             setPara(writer.getDirectContent(), new Phrase(Valore,font), 110+doc.leftMargin(), psosizioneVeriticale+8);
             //Codice Individuazione Bene
             setPara(writer.getDirectContent(), new Phrase(Valore,font), 390+doc.leftMargin(), psosizioneVeriticale+8);
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
    /**
     * Disegna una frase allineata a sinistra a coordinate assolute nella pagina PDF corrente.
     * @param canvas contenuto diretto della pagina su cui disegnare
     * @param p testo da disegnare
     * @param x coordinata X
     * @param y coordinata Y
     */
    public void setPara(PdfContentByte canvas, Phrase p, float x, float y) {
    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p, x, y, 0);
}
    
    /**
     * Aggiunge al PDF una pagina del quadro T (plusvalenze cripto-attività, modulo pre-2025), sovrapponendo
     * all'immagine del modulo fornita il totale vendite, il costo di acquisto, la plusvalenza calcolata
     * (vendite - costo) e le eventuali segnalazioni/errori.
     * @param Immagine percorso dell'immagine del modulo su cui sovrapporre i dati
     * @param Vendite totale corrispettivi delle vendite dell'anno, come stringa decimale
     * @param Costo totale costo di acquisto relativo alle vendite, come stringa decimale
     * @param Segnalazioni testo libero con eventuali segnalazioni/errori da riportare in calce
     * @param Anno anno di riferimento
     */
    public void AggiungiT(String Immagine,String Vendite,String Costo,String Segnalazioni,String Anno) {
          try {
              
             Costo=new BigDecimal(Costo).setScale(0, RoundingMode.HALF_UP).toPlainString();
             Vendite=new BigDecimal(Vendite).setScale(0, RoundingMode.HALF_UP).toPlainString();
             String Plusvalenze=new BigDecimal(Vendite).subtract(new BigDecimal(Costo)).toPlainString();
             Costo=Funzioni.formattaBigDecimal(new BigDecimal(Costo),false);
             Vendite=Funzioni.formattaBigDecimal(new BigDecimal(Vendite),false);
             Plusvalenze=Funzioni.formattaBigDecimal(new BigDecimal(Plusvalenze),false);
             //Plusvalnze del periodo
             //this.AggiungiTestoCentrato("Plusvalenze Cripto-Attività anno "+Anno+" : € "+Plusvalenze+"\n",Font.BOLD,12);
            // this.AggiungiHtml(Segnalazioni);
// String Errore="Attenzione per questo wallet ci sono degli errori da correggere!";
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
             // image01.s
             float LarghezzaPagina=doc.getPageSize().getWidth()-doc.rightMargin()-doc.leftMargin();
             float LarghezzaImmagine=image01.getWidth();
             float PercentualeScala=LarghezzaPagina/LarghezzaImmagine*95;
             image01.scalePercent(PercentualeScala);
             //doc.bottom()
             doc.add(image01);
             float psosizioneVeriticale=writer.getVerticalPosition(false); 
             Font font = new Font(Font.HELVETICA, 8, Font.NORMAL); 
             //Valore delle Vendite
             setPara(writer.getDirectContent(), new Phrase(Vendite,font), 273+doc.leftMargin(), psosizioneVeriticale+645);
             //Valore dei Costi relativi alle Vendite
             setPara(writer.getDirectContent(), new Phrase(Costo,font), 415+doc.leftMargin(), psosizioneVeriticale+645);
             
             font = new Font(Font.HELVETICA, 10, Font.BOLD);
             String MessPlus="Plusvalenze Cripto-Attività anno "+Anno+" : € "+Plusvalenze;
             setPara(writer.getDirectContent(), new Phrase(MessPlus,font), doc.leftMargin(), psosizioneVeriticale+120);
             
             font = new Font(Font.HELVETICA, 8, Font.NORMAL);
             PdfContentByte canvas = writer.getDirectContent();
            float x = doc.leftMargin();
            float y = psosizioneVeriticale + 100;
            // Imposta un'area di testo (rettangolo) dove disegnare il contenuto
            ColumnText ct = new ColumnText(canvas);
            ct.setText(new Phrase(Segnalazioni, font));
            ct.setSimpleColumn(
                x,           // left
                y - 50,      // bottom
                x + 500,     // right
                y            // top
                );
            ct.go();
             
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
    /**
     * Come {@link #AggiungiT}, ma per il modulo quadro T 2025: usa come sfondo la prima pagina di un PDF
     * (invece di un'immagine), importata e scalata proporzionalmente tramite {@link PdfImportedPage}, con la
     * posizione verticale di riferimento calcolata di conseguenza.
     * @param FilePdf percorso del PDF del modulo da usare come sfondo
     * @param Vendite totale corrispettivi delle vendite dell'anno, come stringa decimale
     * @param Costo totale costo di acquisto relativo alle vendite, come stringa decimale
     * @param Segnalazioni testo libero con eventuali segnalazioni/errori da riportare in calce
     * @param Anno anno di riferimento
     */
    public void AggiungiT2025(String FilePdf,String Vendite,String Costo,String Segnalazioni,String Anno) {
          try {
             
              
               // ── CARICAMENTO PDF DI SFONDO ──────────────────────────────────────
        PdfReader pdfReader = new PdfReader(FilePdf);
        PdfImportedPage paginaSfondo = writer.getImportedPage(pdfReader, 1);

        float larghezzaPagina = doc.getPageSize().getWidth()
                                - doc.rightMargin()
                                - doc.leftMargin();
        float altezzaPagina   = doc.getPageSize().getHeight()
                                - doc.topMargin()
                                - doc.bottomMargin();

        // Calcola scala proporzionale in base alla larghezza
        float scalaX = larghezzaPagina / paginaSfondo.getWidth();
        float scalaY = altezzaPagina   / paginaSfondo.getHeight();
        float scala  = Math.min(scalaX, scalaY); // mantieni proporzioni

        // Disegna il PDF di sfondo direttamente sul canvas
        PdfContentByte cb = writer.getDirectContentUnder(); // SOTTO il testo
        cb.saveState();
        cb.addTemplate(
            paginaSfondo,
            scala, 0, 0, scala,          // trasformazione (scaleX, 0, 0, scaleY)
            doc.leftMargin(),             // X origine
            doc.bottomMargin()            // Y origine
        );
        cb.restoreState();

        // ── POSIZIONE VERTICALE DI RIFERIMENTO (come prima) ───────────────
        // Poiché non aggiungiamo l'immagine tramite doc.add(),
        // la posizione verticale va calcolata manualmente dalla cima
        float psosizioneVeriticale = doc.getPageSize().getHeight()
                                     - doc.topMargin()
                                     - (paginaSfondo.getHeight() * scala);
              
              
             Costo=new BigDecimal(Costo).setScale(0, RoundingMode.HALF_UP).toPlainString();
             Vendite=new BigDecimal(Vendite).setScale(0, RoundingMode.HALF_UP).toPlainString();
             String Plusvalenze=new BigDecimal(Vendite).subtract(new BigDecimal(Costo)).toPlainString();
             Costo=Funzioni.formattaBigDecimal(new BigDecimal(Costo),false);
             Vendite=Funzioni.formattaBigDecimal(new BigDecimal(Vendite),false);
             Plusvalenze=Funzioni.formattaBigDecimal(new BigDecimal(Plusvalenze),false);



             Font font = new Font(Font.HELVETICA, 8, Font.NORMAL); 
             //Valore delle Vendite
             setPara(writer.getDirectContent(), new Phrase(Vendite,font), 299+doc.leftMargin(), psosizioneVeriticale+630);
             //Valore dei Costi relativi alle Vendite
             setPara(writer.getDirectContent(), new Phrase(Costo,font), 376+doc.leftMargin(), psosizioneVeriticale+630);
             
             font = new Font(Font.HELVETICA, 10, Font.BOLD);
             String MessPlus="Plusvalenze Cripto-Attività anno "+Anno+" : € "+Plusvalenze;
             setPara(writer.getDirectContent(), new Phrase(MessPlus,font), doc.leftMargin(), psosizioneVeriticale+120);
             
             font = new Font(Font.HELVETICA, 8, Font.NORMAL);
             PdfContentByte canvas = writer.getDirectContent();
            float x = doc.leftMargin();
            float y = psosizioneVeriticale + 100;
            // Imposta un'area di testo (rettangolo) dove disegnare il contenuto
            ColumnText ct = new ColumnText(canvas);
            ct.setText(new Phrase(Segnalazioni, font));
            ct.setSimpleColumn(
                x,           // left
                y - 50,      // bottom
                x + 500,     // right
                y            // top
                );
            ct.go();
             
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
    /**
     * Come {@link #AggiungiT}, ma per il quadro RT (plusvalenze cripto-attività, modulo Redditi pre-2025).
     * @param Immagine percorso dell'immagine del modulo su cui sovrapporre i dati
     * @param Vendite totale corrispettivi delle vendite dell'anno, come stringa decimale
     * @param Costo totale costo di acquisto relativo alle vendite, come stringa decimale
     * @param Segnalazioni testo libero con eventuali segnalazioni/errori da riportare in calce
     * @param Anno anno di riferimento
     */
    public void AggiungiRT(String Immagine,String Vendite,String Costo,String Segnalazioni,String Anno) {
          try {
              
             Costo=new BigDecimal(Costo).setScale(0, RoundingMode.HALF_UP).toPlainString();
             Vendite=new BigDecimal(Vendite).setScale(0, RoundingMode.HALF_UP).toPlainString();
             String Plusvalenze=new BigDecimal(Vendite).subtract(new BigDecimal(Costo)).toPlainString();
             Costo=Funzioni.formattaBigDecimal(new BigDecimal(Costo),false);
             Vendite=Funzioni.formattaBigDecimal(new BigDecimal(Vendite),false);
             Plusvalenze=Funzioni.formattaBigDecimal(new BigDecimal(Plusvalenze),false);
             //Plusvalnze del periodo
             
             //this.AggiungiTestoCentrato("Plusvalenze Cripto-Attività anno "+Anno+" : € "+Plusvalenze+"\n",Font.BOLD,10);
             //this.AggiungiHtml(Segnalazioni);
// String Errore="Attenzione per questo wallet ci sono degli errori da correggere!";
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
             // image01.s
             float LarghezzaPagina=doc.getPageSize().getWidth()-doc.rightMargin()-doc.leftMargin();
             float LarghezzaImmagine=image01.getWidth();
             float PercentualeScala=LarghezzaPagina/LarghezzaImmagine*95;
             image01.scalePercent(PercentualeScala);
             //doc.bottom()
             doc.add(image01);
             float psosizioneVeriticale=writer.getVerticalPosition(false); 
             Font font = new Font(Font.HELVETICA, 8, Font.NORMAL); 
             //Valore delle Vendite
             setPara(writer.getDirectContent(), new Phrase(Vendite,font), 273+doc.leftMargin(), psosizioneVeriticale+303);
             //Valore dei Costi relativi alle Vendite
             setPara(writer.getDirectContent(), new Phrase(Costo,font), 415+doc.leftMargin(), psosizioneVeriticale+303);
             
             font = new Font(Font.HELVETICA, 10, Font.BOLD);
             String MessPlus="Plusvalenze Cripto-Attività anno "+Anno+" : € "+Plusvalenze;
             setPara(writer.getDirectContent(), new Phrase(MessPlus,font), doc.leftMargin(), psosizioneVeriticale+120);
             
             font = new Font(Font.HELVETICA, 8, Font.NORMAL);
             PdfContentByte canvas = writer.getDirectContent();
            float x = doc.leftMargin();
            float y = psosizioneVeriticale + 100;
            // Imposta un'area di testo (rettangolo) dove disegnare il contenuto
            ColumnText ct = new ColumnText(canvas);
            ct.setText(new Phrase(Segnalazioni, font));
            ct.setSimpleColumn(
                x,           // left
                y - 50,      // bottom
                x + 500,     // right
                y            // top
                );
            ct.go();
             
             
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
    /**
     * Come {@link #AggiungiT2025}, ma per il quadro RT 2025 (sfondo da PDF invece che immagine).
     * @param FilePdf percorso del PDF del modulo da usare come sfondo
     * @param Vendite totale corrispettivi delle vendite dell'anno, come stringa decimale
     * @param Costo totale costo di acquisto relativo alle vendite, come stringa decimale
     * @param Segnalazioni testo libero con eventuali segnalazioni/errori da riportare in calce
     * @param Anno anno di riferimento
     */
    public void AggiungiRT2025(String FilePdf, String Vendite, String Costo, String Segnalazioni, String Anno) {
        try {

            // ── CARICAMENTO PDF DI SFONDO ──────────────────────────────────────
            PdfReader pdfReader = new PdfReader(FilePdf);
            PdfImportedPage paginaSfondo = writer.getImportedPage(pdfReader, 1);

            float larghezzaPagina = doc.getPageSize().getWidth()
                    - doc.rightMargin()
                    - doc.leftMargin();
            float altezzaPagina = doc.getPageSize().getHeight()
                    - doc.topMargin()
                    - doc.bottomMargin();

            // Calcola scala proporzionale in base alla larghezza
            float scalaX = larghezzaPagina / paginaSfondo.getWidth();
            float scalaY = altezzaPagina / paginaSfondo.getHeight();
            float scala = Math.min(scalaX, scalaY); // mantieni proporzioni

            // Disegna il PDF di sfondo direttamente sul canvas
            PdfContentByte cb = writer.getDirectContentUnder(); // SOTTO il testo
            cb.saveState();
            cb.addTemplate(
                    paginaSfondo,
                    scala, 0, 0, scala, // trasformazione (scaleX, 0, 0, scaleY)
                    doc.leftMargin(), // X origine
                    doc.bottomMargin() // Y origine
            );
            cb.restoreState();

            // ── POSIZIONE VERTICALE DI RIFERIMENTO (come prima) ───────────────
            // Poiché non aggiungiamo l'immagine tramite doc.add(),
            // la posizione verticale va calcolata manualmente dalla cima
            float psosizioneVeriticale = doc.getPageSize().getHeight()
                    - doc.topMargin()
                    - (paginaSfondo.getHeight() * scala);

            Costo = new BigDecimal(Costo).setScale(0, RoundingMode.HALF_UP).toPlainString();
            Vendite = new BigDecimal(Vendite).setScale(0, RoundingMode.HALF_UP).toPlainString();
            String Plusvalenze = new BigDecimal(Vendite).subtract(new BigDecimal(Costo)).toPlainString();
            Costo = Funzioni.formattaBigDecimal(new BigDecimal(Costo), false);
            Vendite = Funzioni.formattaBigDecimal(new BigDecimal(Vendite), false);
            Plusvalenze = Funzioni.formattaBigDecimal(new BigDecimal(Plusvalenze), false);
            
            boolean plus=true;
            if (Plusvalenze.contains("-"))plus=false;//in questo caso si parla di minus
            String plmn=Plusvalenze.replace("-", "");

            Font font = new Font(Font.HELVETICA, 8, Font.NORMAL);
            //Valore delle Vendite - RT41 colonna 1
            setPara(writer.getDirectContent(), new Phrase(Vendite, font), 326 + doc.leftMargin(), psosizioneVeriticale + 269);
            //Valore dei Costi relativi alle Vendite - RT41 colonna 2
            setPara(writer.getDirectContent(), new Phrase(Costo, font), 400 + doc.leftMargin(), psosizioneVeriticale + 269);
            
            //Rigo RT57
            if (plus){
                //plusvalenza
                setPara(writer.getDirectContent(), new Phrase(plmn, font), 440 + doc.leftMargin(), psosizioneVeriticale + 142);
            }
            else{
                //minusvalenza
                setPara(writer.getDirectContent(), new Phrase(plmn, font), 345 + doc.leftMargin(), psosizioneVeriticale + 142);
            }

            font = new Font(Font.HELVETICA, 10, Font.BOLD);
            String MessPlus ="NB : Ricordarsi di compilare i campi non completati dal programma seguendo le istruzioni di compilazione";
            setPara(writer.getDirectContent(), new Phrase(MessPlus, font), doc.leftMargin(), psosizioneVeriticale + 100);

            font = new Font(Font.HELVETICA, 8, Font.NORMAL);
            PdfContentByte canvas = writer.getDirectContent();
            float x = doc.leftMargin();
            float y = psosizioneVeriticale + 100;
            // Imposta un'area di testo (rettangolo) dove disegnare il contenuto
            ColumnText ct = new ColumnText(canvas);
            ct.setText(new Phrase(Segnalazioni, font));
            ct.setSimpleColumn(
                    x, // left
                    y - 50, // bottom
                    x + 500, // right
                    y // top
            );
            ct.go();

        } catch (BadElementException | IOException ex) {
            Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Come {@link #AggiungiRT2025}, ma per la seconda pagina del modulo quadro RT 2025 (sfondo da un PDF separato).
     * @param FilePdf percorso del PDF della seconda pagina del modulo da usare come sfondo
     * @param Vendite totale corrispettivi delle vendite dell'anno, come stringa decimale
     * @param Costo totale costo di acquisto relativo alle vendite, come stringa decimale
     * @param Segnalazioni testo libero con eventuali segnalazioni/errori da riportare in calce
     * @param Anno anno di riferimento
     */
    public void AggiungiRT2025p2(String FilePdf, String Vendite, String Costo, String Segnalazioni, String Anno) {
        try {

            // ── CARICAMENTO PDF DI SFONDO ──────────────────────────────────────
            PdfReader pdfReader = new PdfReader(FilePdf);
            PdfImportedPage paginaSfondo = writer.getImportedPage(pdfReader, 1);

            float larghezzaPagina = doc.getPageSize().getWidth()
                    - doc.rightMargin()
                    - doc.leftMargin();
            float altezzaPagina = doc.getPageSize().getHeight()
                    - doc.topMargin()
                    - doc.bottomMargin();

            // Calcola scala proporzionale in base alla larghezza
            float scalaX = larghezzaPagina / paginaSfondo.getWidth();
            float scalaY = altezzaPagina / paginaSfondo.getHeight();
            float scala = Math.min(scalaX, scalaY); // mantieni proporzioni

            // Disegna il PDF di sfondo direttamente sul canvas
            PdfContentByte cb = writer.getDirectContentUnder(); // SOTTO il testo
            cb.saveState();
            cb.addTemplate(
                    paginaSfondo,
                    scala, 0, 0, scala, // trasformazione (scaleX, 0, 0, scaleY)
                    doc.leftMargin(), // X origine
                    doc.bottomMargin() // Y origine
            );
            cb.restoreState();

            // ── POSIZIONE VERTICALE DI RIFERIMENTO (come prima) ───────────────
            // Poiché non aggiungiamo l'immagine tramite doc.add(),
            // la posizione verticale va calcolata manualmente dalla cima
            float psosizioneVeriticale = doc.getPageSize().getHeight()
                    - doc.topMargin()
                    - (paginaSfondo.getHeight() * scala);

            //Costo = new BigDecimal(Costo).setScale(0, RoundingMode.HALF_UP).toPlainString();
            //Vendite = new BigDecimal(Vendite).setScale(0, RoundingMode.HALF_UP).toPlainString();
            String Plusvalenze = new BigDecimal(Vendite).subtract(new BigDecimal(Costo)).toPlainString();
            Font font;
           // Costo = Funzioni.formattaBigDecimal(new BigDecimal(Costo), false);
           // Vendite = Funzioni.formattaBigDecimal(new BigDecimal(Vendite), false);
            Plusvalenze = Funzioni.formattaBigDecimal(new BigDecimal(Plusvalenze), false);
            
            boolean plus=true;
            if (Plusvalenze.contains("-"))plus=false;//in questo caso si parla di minus
            String plmn=Plusvalenze.replace("-", "");

            font = new Font(Font.HELVETICA, 8, Font.NORMAL);
            
            //Rigo RT105
            if (!plus){
                //minusvalenza nel 105
                setPara(writer.getDirectContent(), new Phrase(plmn, font), 440 + doc.leftMargin(), psosizioneVeriticale + 448);
            }

            font = new Font(Font.HELVETICA, 10, Font.BOLD);
            String MessPlus = "NB : Ricordarsi di compilare i campi non completati dal programma seguendo le istruzioni di compilazione";
            setPara(writer.getDirectContent(), new Phrase(MessPlus, font), doc.leftMargin(), psosizioneVeriticale + 100);
            
          /*  font = new Font(Font.HELVETICA, 10, Font.BOLD);
            String MessPlus = "Plusvalenze Cripto-Attività anno " + Anno + " : € " + Plusvalenze;
            setPara(writer.getDirectContent(), new Phrase(MessPlus, font), doc.leftMargin(), psosizioneVeriticale + 100);*/

            font = new Font(Font.HELVETICA, 8, Font.NORMAL);
            PdfContentByte canvas = writer.getDirectContent();
            float x = doc.leftMargin();
            float y = psosizioneVeriticale + 100;
            // Imposta un'area di testo (rettangolo) dove disegnare il contenuto
            ColumnText ct = new ColumnText(canvas);
            ct.setText(new Phrase(Segnalazioni, font));
            ct.setSimpleColumn(
                    x, // left
                    y - 50, // bottom
                    x + 500, // right
                    y // top
            );
            ct.go();

        } catch (BadElementException | IOException ex) {
            Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    /**
     * Aggiunge al PDF una tabella con intestazione in grassetto e righe di dettaglio in font monospace,
     * a larghezza piena pagina. Le righe di dettaglio con meno colonne del previsto vengono ignorate.
     * @param Titoli intestazioni delle colonne
     * @param Dettagli righe di dati, ciascuna con lo stesso numero di colonne di {@code Titoli}
     */

    // ═══════════════════════════════════════════════════════════════════════════════
    //  Veste grafica : API pubblica
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Attiva la veste grafica del report (copertina, testata, piede, filigrana).
     * <p>
     * <b>Va chiamata prima di {@link #ApriDocumento()}</b> e <b>al posto di</b> {@link #Piede(String)} :
     * il piede vecchio usa {@code doc.setFooter(HeaderFooter)}, che stampa da pagina 1 e finirebbe
     * anche sulla copertina. Chi non la chiama ottiene il PDF esattamente come prima.
     *
     * @param Quadro sigla del quadro ({@code "QUADRO W"}, {@code "QUADRO RW"}), usata nel marcatore
     *               verticale del margine
     * @param Anno anno d'imposta
     */
    public void AttivaVesteGrafica(String Quadro, String Anno) {
        CaricaFont();
        cornice = new CorniceReport(Quadro, Anno);
        writer.setPageEvent(cornice);
    }

    /**
     * Testo mostrato a destra nella testata delle pagine successive alla copertina.
     * Da impostare all'inizio di ogni pagina, prima di comporla.
     *
     * @param Contesto descrizione della pagina (es. {@code "Quadro W - anno 2025 - Foglio 1"})
     * @param ModuloATuttaPagina {@code true} sulle pagine del Quadro RW, dove il modulo copre tutta
     *        la pagina : la filigrana viene dimezzata per non velare l'intero foglio
     */
    public void ContestoPagina(String Contesto, boolean ModuloATuttaPagina) {
        if (cornice == null) return;
        cornice.Contesto = Contesto == null ? "" : Contesto;
        cornice.ModuloATuttaPagina = ModuloATuttaPagina;
    }

    /** @return {@code true} se la veste grafica e' attiva su questo documento */
    public boolean VesteGraficaAttiva() {
        return cornice != null;
    }

    /**
     * Disegna la copertina sulla pagina corrente. La pagina resta senza testata e senza piede :
     * la cornice riconosce il numero di pagina e la salta.
     *
     * @param Quadro titolo grande (es. {@code "Quadro W"})
     * @param Sottotitolo riga sotto il titolo (es. {@code "Cripto-attivita'"})
     * @param Anno anno d'imposta
     * @param Generato data e ora di generazione, gia' formattate
     */
    public void AggiungiCopertina(String Quadro, String Sottotitolo, String Anno, String Generato) {
        if (cornice == null) return;
        try {
            PdfContentByte cb = writer.getDirectContent();
            float W = doc.getPageSize().getWidth();
            float H = doc.getPageSize().getHeight();
            cornice.PaginaCopertina = writer.getPageNumber();

            com.lowagie.text.Image logo = LogoApplicazione();
            if (logo != null) {
                logo.scaleAbsolute(54, 54);
                logo.setAbsolutePosition(70, H - 120);
                cb.addImage(logo);
            }
            Testo(cb, "GIACENZE CRYPTO", bfBold, 12, NERO, 136, H - 84, Element.ALIGN_LEFT, 3.5f);
            Testo(cb, "monitoraggio e fiscalità delle cripto-attività", bfRegular, 8, GRIGIO_TENUE,
                    136, H - 98, Element.ALIGN_LEFT, 0.5f);
            Linea(cb, 70, H - 140, W - 70, H - 140, VERDE, 0.8f);

            Testo(cb, "REPORT PER LA COMPILAZIONE", bfRegular, 9.5f, VERDE_SCURO, 70, H - 330, Element.ALIGN_LEFT, 3f);
            Testo(cb, Quadro, bfBold, 40, NERO, 70, H - 382, Element.ALIGN_LEFT, 0f);
            Testo(cb, Sottotitolo, bfRegular, 24, GRIGIO_TESTO, 70, H - 416, Element.ALIGN_LEFT, 0f);
            Linea(cb, 70, H - 446, 240, H - 446, VERDE, 2.5f);
            Testo(cb, "Anno d'imposta " + Anno, bfRegular, 13, NERO, 70, H - 478, Element.ALIGN_LEFT, 0.5f);

            float y = 250;
            Linea(cb, 70, y + 22, W - 70, y + 22, GRIGIO_FILO, 0.6f);
            RigaDato(cb, "Documento", "Report " + Quadro + " - " + Sottotitolo, 70, y, W - 70);
            RigaDato(cb, "Anno d'imposta", Anno, 70, y - 26, W - 70);
            RigaDato(cb, "Generato il", Generato, 70, y - 52, W - 70);
            //Titolo contiene gia' la versione (VarStatiche.componiTitolo).
            RigaDato(cb, "Prodotto da", VarStatiche.Titolo, 70, y - 78, W - 70);

            Testo(cb, "AVVERTENZA", bfBold, 7.5f, VERDE_SCURO, 70, 108, Element.ALIGN_LEFT, 2f);
            Testo(cb, "I documenti ottenuti e le informazioni presenti hanno sempre valenza informativa e meramente",
                    bfRegular, 7.5f, GRIGIO_TESTO, 70, 94, Element.ALIGN_LEFT, 0f);
            Testo(cb, "indicativa ed esemplificativa, e non sono in alcun modo sostitutive di una consulenza fiscale.",
                    bfRegular, 7.5f, GRIGIO_TESTO, 70, 83, Element.ALIGN_LEFT, 0f);
            Linea(cb, 70, 58, W - 70, 58, VERDE, 0.8f);
            //L'indirizzo esce da RiferimentoStampe() e non e' scritto a mano : nell'edizione Store
            //e' vuoto di proposito, e una copertina che lo mostrasse aggirerebbe quella scelta.
            String rif = VarStatiche.RiferimentoStampe();
            if (!rif.isBlank()) {
                Testo(cb, rif.replaceFirst("^\\s*-\\s*", ""), bfRegular, 8, GRIGIO_TENUE, 70, 42, Element.ALIGN_LEFT, 1f);
            }

            //La copertina e' tutta sul canvas : serve un elemento perche' la pagina venga emessa.
            doc.add(new Paragraph(" "));
        } catch (Exception ex) {
            Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Distanziatore alto quanto la testata disegnata, da mettere in cima alle pagine dei moduli.
     * La testata sta sul canvas e non occupa flusso : senza questo il primo elemento le finirebbe sotto.
     */
    public void DistanziatoreTestata() {
        if (cornice == null) return;
        Paragraph spazio = new Paragraph(" ");
        spazio.setLeading(28f);
        doc.add(spazio);
    }

    /**
     * Titolo di sezione, per le pagine che hanno spazio (note, riepiloghi).
     * <b>Non usarlo sulle pagine dei moduli</b> : vedi la nota sul budget di flusso in testa alla classe.
     *
     * @param Titolo titolo della sezione
     * @param Sottotitolo riga piccola sotto il titolo
     */
    public void AggiungiTitoloSezione(String Titolo, String Sottotitolo) {
        if (cornice == null) return;
        DistanziatoreTestata();
        Paragraph par = new Paragraph();
        par.add(new Chunk(Titolo, new Font(bfBold, 15, Font.NORMAL, NERO)));
        par.add(Chunk.NEWLINE);
        par.add(new Chunk(Sottotitolo, new Font(bfRegular, 8.5f, Font.NORMAL, GRIGIO_TESTO)));
        par.setSpacingAfter(8f);
        doc.add(par);
    }

    /**
     * Riga di intestazione di un rigo del Quadro W : progressivo in pastiglia verde, nome del gruppo
     * wallet, eventuale segnalazione di errore in rosso. Sostituisce la riga HTML in Courier.
     *
     * @param Progressivo etichetta del rigo (es. {@code "W1"})
     * @param Gruppo nome del gruppo wallet
     * @param Errore testo dell'errore, vuoto se non ce ne sono
     */
    public void AggiungiEtichettaGruppo(String Progressivo, String Gruppo, String Errore) {
        AggiungiEtichettaGruppo(Progressivo, Gruppo, "", Errore);
    }

    /**
     * Altezza fissa riservata in flusso a un'etichetta di rigo : una riga, o due quando c'e' anche
     * il dettaglio. Vedi {@link #AggiungiEtichettaGruppo}.
     */
    private static final float ALTEZZA_ETICHETTA = 13f;
    private static final float ALTEZZA_ETICHETTA_CON_DETTAGLIO = 18f;

    /**
     * Come {@link #AggiungiEtichettaGruppo(String, String, String)}, con in piu' un dettaglio neutro
     * fra il nome e l'eventuale errore : usato dalla parte FIAT per il tipo di attivita' e l'IVAFE.
     * <p>
     * <b>L'etichetta si disegna sul canvas dentro uno spazio di altezza fissa</b> ({@value #ALTEZZA_ETICHETTA} pt),
     * non come paragrafo che si impagina da solo, e il testo che eccede la larghezza viene troncato.
     * Non e' pignoleria: il foglio porta 5 moduli da ~105 pt piu' il rigo-titolo da ~129 pt, e la
     * paginazione di {@code Principale} e' fissa a 5 righi per foglio. Con un paragrafo normale
     * un'etichetta lunga va a capo, il quinto rigo scivola sulla pagina dopo e i valori finiscono
     * sovrastampati sul rigo sbagliato — un modulo fiscale errato. Misurato sul caso peggiore reale
     * (nome lungo + dettaglio conto corrente + IVAFE + avviso Stato): con i paragrafi restavano
     * <b>1,7 pt</b> di margine, e la vecchia riga HTML in Courier traboccava gia' per conto suo.
     * Con l'altezza fissa il margine e' di oltre 120 pt e non dipende piu' dalla lunghezza dei nomi.
     *
     * @param Dettaglio testo descrittivo, in grigio (non e' una segnalazione)
     */
    public void AggiungiEtichettaGruppo(String Progressivo, String Gruppo, String Dettaglio, String Errore) {
        if (cornice == null) {
            //Senza veste grafica resta la riga HTML di prima, cosi' i chiamanti non devono ramificare.
            AggiungiHtml("<html><font size=\"2\" face=\"Courier New,Courier, mono\" ><b>" + Gruppo + "</b>"
                    + (Dettaglio == null ? "" : Dettaglio) + (Errore == null || Errore.isBlank() ? "" : " - " + Errore) + "</html>");
            return;
        }
        boolean conDettaglio = Dettaglio != null && !Dettaglio.isBlank();
        Paragraph spazio = new Paragraph(" ");
        spazio.setLeading(conDettaglio ? ALTEZZA_ETICHETTA_CON_DETTAGLIO : ALTEZZA_ETICHETTA);
        doc.add(spazio);

        PdfContentByte cb = writer.getDirectContent();
        float x = doc.leftMargin();
        float xMax = doc.getPageSize().getWidth() - doc.rightMargin();
        //Con il dettaglio la prima riga sta piu' in alto : la seconda le va sotto.
        float y = writer.getVerticalPosition(false) + (conDettaglio ? 9.5f : 3f);

        //Pastiglia verde col progressivo
        String p = " " + Progressivo + " ";
        float largP = bfBold.getWidthPoint(p, 7.5f);
        cb.saveState();
        cb.setColorFill(VERDE);
        cb.rectangle(x, y - 2.5f, largP, 11f);
        cb.fill();
        cb.restoreState();
        Testo(cb, p, bfBold, 7.5f, new Color(0x16, 0x16, 0x0A), x, y, Element.ALIGN_LEFT, 0f);
        x += largP + 6f;

        //Prima riga : nome ed eventuale errore. L'errore sta qui e non in coda al dettaglio perche'
        //e' la parte che non puo' permettersi di finire troncata.
        x = TestoTroncato(cb, Gruppo, bfBold, 10.5f, NERO, x, y, xMax);
        if (Errore != null && !Errore.isBlank()) {
            TestoTroncato(cb, "   " + Errore.trim(), bfBold, 8f, new Color(0xB0, 0x30, 0x10), x, y, xMax);
        }
        //Seconda riga : il dettaglio, in grigio, rientrato sotto il nome.
        if (conDettaglio) {
            String d = Dettaglio.trim();
            if (d.startsWith("-")) d = d.substring(1).trim();
            TestoTroncato(cb, d, bfRegular, 7.5f, GRIGIO_TESTO,
                    doc.leftMargin() + largP + 6f, y - 9f, xMax);
        }
    }

    /**
     * Colonna bianca a sinistra del modulo RW, dove finiscono nome del gruppo e note.
     * <p>
     * Misurata sui moduli veri: l'inchiostro delle caselle comincia a 130 pt sul template 2025 e a
     * 145 pt sulle scansioni 2023/2024, mentre la fascia verde di margine arriva a 24,5 pt. Prima
     * i nomi partivano da 21 pt, cioe' <b>dentro</b> la fascia, e le note lunghe proseguendo su una
     * riga sola finivano sopra il modulo compilato.
     */
    private static final float X_COLONNA_RW = 30f;
    private static final float LARG_COLONNA_RW_2025 = 97f;
    private static final float LARG_COLONNA_RW_IMMAGINE = 109f;

    /**
     * Scrive {@code s} nella colonna larga {@code larghezza} andando a capo sugli spazi, e spezzando
     * a meta' parola solo le parole che da sole non ci starebbero (un indirizzo, un nome senza spazi).
     * Disegnando sul canvas non esiste il ritorno a capo automatico: senza questo il testo prosegue
     * dritto oltre la colonna e va a finire sopra il modulo.
     *
     * @param maxRighe oltre questo numero di righe il testo viene troncato con dei puntini, cosi' una
     *                 nota molto lunga non invade il rigo successivo
     * @return la y dell'ultima riga scritta
     */
    private static float TestoAvvolto(PdfContentByte cb, String s, BaseFont bf, float size, Color c,
            float x, float y, float larghezza, float interlinea, int maxRighe) {
        java.util.List<String> righe = new ArrayList<>();
        StringBuilder riga = new StringBuilder();
        for (String parola : s.trim().split("\\s+")) {
            if (parola.isEmpty()) continue;
            String prova = riga.length() == 0 ? parola : riga + " " + parola;
            if (bf.getWidthPoint(prova, size) <= larghezza) {
                riga.setLength(0);
                riga.append(prova);
                continue;
            }
            if (riga.length() > 0) {
                righe.add(riga.toString());
                riga.setLength(0);
            }
            //Parola piu' larga della colonna : va spezzata, altrimenti sborderebbe da sola.
            while (bf.getWidthPoint(parola, size) > larghezza) {
                int taglio = 1;
                while (taglio < parola.length()
                        && bf.getWidthPoint(parola.substring(0, taglio + 1), size) <= larghezza) taglio++;
                righe.add(parola.substring(0, taglio));
                parola = parola.substring(taglio);
            }
            riga.append(parola);
        }
        if (riga.length() > 0) righe.add(riga.toString());

        float yr = y;
        for (int i = 0; i < righe.size(); i++) {
            String t = righe.get(i);
            if (i == maxRighe - 1 && righe.size() > maxRighe) {
                while (t.length() > 0 && bf.getWidthPoint(t + "...", size) > larghezza) t = t.substring(0, t.length() - 1);
                t = t + "...";
            }
            Testo(cb, t, bf, size, c, x, yr, Element.ALIGN_LEFT, 0f);
            yr -= interlinea;
            if (i == maxRighe - 1) break;
        }
        return yr + interlinea;
    }

    /**
     * Nome del gruppo e nota di un rigo RW, nella colonna bianca a sinistra del modulo.
     * <p>
     * Con la veste grafica attiva vanno a capo dentro la colonna; senza, restano le due righe secche
     * di prima alle coordinate storiche ({@code xVecchia}), perche' li' i font del report non sono
     * nemmeno caricati.
     *
     * @param yNome y della prima riga del nome
     * @param larghezza larghezza della colonna, diversa fra template 2025 e scansioni precedenti
     * @param xVecchia x delle due righe nel comportamento senza veste
     * @param yNotaVecchia y della nota nel comportamento senza veste
     */
    private void ColonnaNomeENota(String Nome, String Nota, float yNome, float larghezza,
            float xVecchia, float yNotaVecchia) {
        PdfContentByte cb = writer.getDirectContent();
        if (cornice == null) {
            setPara(cb, new Phrase(Nome, new Font(Font.HELVETICA, 10, Font.BOLD)), xVecchia, yNome);
            setPara(cb, new Phrase(Nota == null ? "" : Nota, new Font(Font.HELVETICA, 8, Font.NORMAL)),
                    xVecchia, yNotaVecchia);
            return;
        }
        float y = TestoAvvolto(cb, Nome == null ? "" : Nome, bfBold, 9f, NERO,
                X_COLONNA_RW, yNome, larghezza, 10f, 3);
        if (Nota != null && !Nota.isBlank()) {
            TestoAvvolto(cb, Nota, bfRegular, 7f, ColoreNota(Nota),
                    X_COLONNA_RW, y - 11f, larghezza, 8f, 4);
        }
    }

    /**
     * Colore di una nota di rigo RW. Le note del percorso cripto sono sempre segnalazioni di errore
     * ({@code "Errori da correggere!"}), quelle del percorso FIAT sono informative (Stato mancante,
     * fiscalita' privilegiata, conto sotto soglia): il rosso va solo alle prime.
     */
    private static Color ColoreNota(String nota) {
        return nota != null && nota.startsWith("Errori") ? new Color(0xB0, 0x30, 0x10) : GRIGIO_TESTO;
    }

    /**
     * Scrive {@code s} a partire da {@code x}, troncandolo con dei puntini se non ci sta entro
     * {@code xMax}. Disegnando sul canvas non c'e' ritorno a capo automatico: senza troncatura il
     * testo proseguirebbe oltre il margine destro.
     *
     * @return la x raggiunta dalla fine del testo scritto
     */
    private static float TestoTroncato(PdfContentByte cb, String s, BaseFont bf, float size, Color c,
            float x, float y, float xMax) {
        if (x >= xMax) return x;
        float disponibile = xMax - x;
        if (bf.getWidthPoint(s, size) > disponibile) {
            String puntini = "...";
            float largPuntini = bf.getWidthPoint(puntini, size);
            StringBuilder sb = new StringBuilder();
            float largo = 0;
            for (int i = 0; i < s.length(); i++) {
                float w = bf.getWidthPoint(String.valueOf(s.charAt(i)), size);
                if (largo + w + largPuntini > disponibile) break;
                sb.append(s.charAt(i));
                largo += w;
            }
            s = sb.toString().stripTrailing() + puntini;
        }
        Testo(cb, s, bf, size, c, x, y, Element.ALIGN_LEFT, 0f);
        return x + bf.getWidthPoint(s, size);
    }

    // ─── Caricamento del font e del logo ──────────────────────────────────────────

    /**
     * Carica dal jar le due facce di Noto Sans usate dalla veste grafica e le registra anche in
     * {@link FontFactory}, cosi' {@link #AggiungiHtml(String)} puo' chiedere {@code face="Noto Sans"}.
     * <p>
     * Il {@code registerFamily} non e' decorativo : registrando i due file senza alias la famiglia
     * risultante e' {@code "Noto Sans Regular"} e {@code HTMLWorker} non risolverebbe il grassetto,
     * stampando l'intero testo in bold (o l'intero testo in regular). {@code RandomAccessFileOrArray}
     * accetta un percorso di classpath, quindi i TTF si leggono direttamente da dentro il jar.
     */
    private static synchronized void CaricaFont() {
        try {
            if (bfRegular == null) {
                bfRegular = BaseFont.createFont("/Fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                bfBold    = BaseFont.createFont("/Fonts/NotoSans-Bold.ttf",    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            if (!fontHtmlRegistrato) {
                FontFactory.register("/Fonts/NotoSans-Regular.ttf");
                FontFactory.register("/Fonts/NotoSans-Bold.ttf");
                FontFactory.register("/Fonts/NotoSans-Italic.ttf");
                FontFactory.getFontImp().registerFamily(FAMIGLIA_HTML, "NotoSans-Regular", "/Fonts/NotoSans-Regular.ttf");
                FontFactory.getFontImp().registerFamily(FAMIGLIA_HTML, "NotoSans-Bold",    "/Fonts/NotoSans-Bold.ttf");
                fontHtmlRegistrato = true;
            }
        } catch (Exception ex) {
            Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, "Font del report non caricato", ex);
        }
    }

    /** Nome famiglia con cui il font e' registrato in FontFactory (minuscolo : la mappa e' case-folded). */
    private static final String FAMIGLIA_HTML = "noto sans";

    /** Il logo sta nel classpath ({@code src/main/resources/logo.png}), non in {@code Immagini/}. */
    private static com.lowagie.text.Image LogoApplicazione() {
        try {
            java.net.URL u = Stampe.class.getResource("/logo.png");
            return u == null ? null : com.lowagie.text.Image.getInstance(u);
        } catch (Exception ex) {
            return null;
        }
    }

    // ─── Utilita' di disegno ──────────────────────────────────────────────────────

    static void Testo(PdfContentByte cb, String s, BaseFont bf, float size, Color c,
            float x, float y, int align, float spaziatura) {
        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(bf, size);
        cb.setColorFill(c);
        if (spaziatura > 0) cb.setCharacterSpacing(spaziatura);
        cb.showTextAligned(align, s, x, y, 0);
        cb.setCharacterSpacing(0);
        cb.endText();
        cb.restoreState();
    }

    static void Linea(PdfContentByte cb, float x1, float y1, float x2, float y2, Color c, float w) {
        cb.saveState();
        cb.setColorStroke(c);
        cb.setLineWidth(w);
        cb.moveTo(x1, y1);
        cb.lineTo(x2, y2);
        cb.stroke();
        cb.restoreState();
    }

    static void RigaDato(PdfContentByte cb, String etichetta, String valore, float x, float y, float xFine) {
        Testo(cb, etichetta.toUpperCase(), bfRegular, 7, GRIGIO_TENUE, x, y, Element.ALIGN_LEFT, 1.5f);
        Testo(cb, valore, bfRegular, 9.5f, NERO, x + 130, y, Element.ALIGN_LEFT, 0f);
        Linea(cb, x, y - 8, xFine, y - 8, GRIGIO_FILO, 0.6f);
    }

    /**
     * Testata, piede e filigrana del report, disegnati sul canvas a fine pagina.
     * La copertina viene saltata.
     */
    static class CorniceReport extends PdfPageEventHelper {

        final String Quadro;
        final String Anno;
        String Contesto = "";
        boolean ModuloATuttaPagina = false;
        int PaginaCopertina = 0;

        CorniceReport(String Quadro, String Anno) {
            this.Quadro = Quadro;
            this.Anno = Anno;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            int pagina = writer.getPageNumber();
            if (pagina == PaginaCopertina) return;
            try {
                float W = doc.getPageSize().getWidth();
                float H = doc.getPageSize().getHeight();
                Filigrana(writer.getDirectContentUnder(), writer.getDirectContent(), W, H);
                Testata(writer.getDirectContent(), W, H);
                Piede(writer.getDirectContent(), W, pagina);
            } catch (Exception ex) {
                Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /**
         * Due filigrane sovrapposte : la fascia verde nel margine sinistro, che il modulo non copre
         * mai, e la scritta diagonale sovrastampata.
         * <p>
         * La scritta <b>deve</b> stare sul canvas sopra ({@code getDirectContent}) con opacita' bassa :
         * i moduli dell'Agenzia sono immagini opache (QuadroW_2023.png e' RGBA con alpha 255 ovunque,
         * gli altri sono JPEG), quindi sotto sparirebbe. Verificata a 300 dpi sui riquadri degli
         * importi : al 4,5 % non intacca la leggibilita' delle cifre.
         */
        void Filigrana(PdfContentByte sotto, PdfContentByte sopra, float W, float H) {
            sotto.saveState();
            sotto.setColorFill(FASCIA_MARGINE);
            sotto.rectangle(0, 0, 22, H);
            sotto.fill();
            sotto.setColorFill(VERDE);
            sotto.rectangle(22, 0, 2.5f, H);
            sotto.fill();
            sotto.restoreState();
            sotto.saveState();
            sotto.beginText();
            sotto.setFontAndSize(bfBold, 7);
            sotto.setColorFill(VERDE_SCURO);
            sotto.setCharacterSpacing(2f);
            sotto.showTextAligned(Element.ALIGN_CENTER, Quadro + " " + Anno, 12, H / 2, 90);
            sotto.setCharacterSpacing(0);
            sotto.endText();
            sotto.restoreState();

            PdfGState gs = new PdfGState();
            gs.setFillOpacity(ModuloATuttaPagina ? 0.028f : 0.045f);
            sopra.saveState();
            sopra.setGState(gs);
            sopra.beginText();
            sopra.setFontAndSize(bfBold, 58);
            sopra.setColorFill(VERDE);
            sopra.showTextAligned(Element.ALIGN_CENTER, "GIACENZE CRYPTO", W / 2, H / 2, 55);
            sopra.endText();
            sopra.restoreState();
        }

        /** Alta 44 pt dal bordo : sul Quadro RW ne sono disponibili 57,5 prima dell'inchiostro del modulo. */
        void Testata(PdfContentByte cb, float W, float H) {
            com.lowagie.text.Image logo = LogoApplicazione();
            if (logo != null) {
                try {
                    logo.scaleAbsolute(14, 14);
                    logo.setAbsolutePosition(46, H - 38);
                    cb.addImage(logo);
                } catch (Exception ignora) {
                }
            }
            Testo(cb, "GIACENZE CRYPTO", bfBold, 7.5f, GRIGIO_TESTO, 66, H - 33, Element.ALIGN_LEFT, 2.5f);
            Testo(cb, Contesto, bfRegular, 7.5f, GRIGIO_TENUE, W - 36, H - 33, Element.ALIGN_RIGHT, 0f);
            Linea(cb, 46, H - 44, W - 36, H - 44, VERDE, 0.7f);
        }

        void Piede(PdfContentByte cb, float W, int pagina) {
            Linea(cb, 46, 40, W - 36, 40, new Color(0xE2, 0xE2, 0xE2), 0.6f);
            Testo(cb, "Generato da " + VarStatiche.Titolo + VarStatiche.RiferimentoStampe(),
                    bfRegular, 6.5f, GRIGIO_TENUE, 46, 28, Element.ALIGN_LEFT, 0f);
            Testo(cb, "Pagina " + pagina, bfRegular, 7, GRIGIO_TESTO, W - 36, 28, Element.ALIGN_RIGHT, 0f);
        }
    }

    public void AggiungiTabella(String[] Titoli,List<String[]> Dettagli){
      Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
      int NumeroColonne=Titoli.length;
      PdfPTable table = new PdfPTable(NumeroColonne);
      table.setWidthPercentage(100);
      //setto le larghezze delle colonne
      float[] larghezzacelle=new float[NumeroColonne];
      for (int i=0;i<NumeroColonne;i++){
          larghezzacelle[i]=6.0f;
      }
      //scrivo la riga dei titoli
      table.setWidths(larghezzacelle);
      PdfPCell cell = new PdfPCell();
      for (int i=0;i<NumeroColonne;i++){
          cell.setPhrase(new Phrase(Titoli[i], font));
          table.addCell(cell);
      }
      //scrivo la riga tabella
      font = new Font(Font.COURIER, 8, Font.NORMAL); 
      
      for(int i= 0; i < Dettagli.size(); i++){
          String Dati[]=Dettagli.get(i);
          if (Dati.length>=NumeroColonne)
          {
              for(int h= 0; h < NumeroColonne; h++){
                cell.setPhrase(new Phrase(Dati[h], font));
                //System.out.println(Dati[h]);
                table.addCell(cell);
              }
          }
          
      }
      doc.add(table);
      
      
     }
    
    /**
     * Aggiunge al PDF un blocco di contenuto HTML, convertito tramite {@link HTMLWorker}.
     * @param html markup HTML da aggiungere al documento
     */
    public void AggiungiHtml(String html){
          try {
              //Con la veste grafica attiva le note escono nel font del programma invece che in
              //Courier. La sostituzione e' qui e non nei chiamanti perche' le stringhe HTML sono
              //sparse in Principale e tutte scritte con la stessa face : un punto solo da tenere
              //allineato, e chi non attiva la veste non vede alcuna differenza.
              //Le due grafie con e senza spazio dopo la virgola sono entrambe presenti in Principale.
              if (cornice!=null) html=html.replaceAll("face=\"Courier New,\\s*Courier,\\s*mono\"", "face=\"Noto Sans\"");
              StyleSheet style=new StyleSheet();
              ArrayList<Element> htmlContetList = HTMLWorker.parseToList(new StringReader(html), null);
              Paragraph paragraph = new Paragraph();
              for (Element element : htmlContetList) {
              paragraph.add(element);
              }
              doc.add(paragraph);
          } catch (IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
          }
             
    }
    
    /**
     * Aggiunge al PDF un paragrafo di testo semplice (font Courier), allineato a sinistra.
     * @param Testo testo da aggiungere
     * @param intfont stile del font (es. {@link Font#NORMAL}, {@link Font#BOLD})
     * @param size dimensione del font in punti
     */
    public void AggiungiTesto(String Testo, int intfont, float size) {
        //Element e =new Element(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));
        Paragraph par = new Paragraph();
        par.setAlignment(Element.ALIGN_LEFT);
        par.add(new Chunk(Testo, FontFactory.getFont(FontFactory.COURIER, size, intfont)));              
        doc.add(par);
        //par.add(new Chunk(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont)));
        //  Paragraph par = new Paragraph(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));

    }
    
    /**
     * Come {@link #AggiungiTesto}, ma con il paragrafo centrato orizzontalmente.
     * @param Testo testo da aggiungere
     * @param intfont stile del font (es. {@link Font#NORMAL}, {@link Font#BOLD})
     * @param size dimensione del font in punti
     */
    public void AggiungiTestoCentrato(String Testo,int intfont,float size){
              Paragraph par = new Paragraph(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));
              par.setAlignment(Element.ALIGN_CENTER);
              doc.add(par);

 
     }






}
