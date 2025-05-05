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
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
      
     public Stampe(String PDFPath) throws FileNotFoundException {
       FilePDF=PDFPath;
       doc = new Document();
       writer = PdfWriter.getInstance(doc, new FileOutputStream(FilePDF));
     //  doc.open();  
       
     } 
     
     public void ApriDocumento(){
        doc.open(); 
     }
     
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
    
    public void NuovaPagina(){
        doc.newPage();
    }
    
    public void Piede(String piendino){
    //    doc.setFooter(new HeaderFooter(piedino,piedino));
        Font font = new Font(Font.HELVETICA, 6, Font.NORMAL);       
        HeaderFooter footer = new HeaderFooter(new Phrase(piendino,font), false);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(Rectangle.NO_BORDER);    
        doc.setFooter(footer);
        
    } 
    
    public void AggiungiTitolo(String Titolo){
        doc.addTitle(Titolo);
     }
    
    public void AggiungiQuadroW(String Immagine,String NumeroQuadro,String ValoreIniziale,String ValoreFinale,String Giorni) {
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
             //Codice Individuazione Bene
             setPara(writer.getDirectContent(), new Phrase("21",font), 140+doc.leftMargin(), psosizioneVeriticale+75);
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
             //Codice 14
             setPara(writer.getDirectContent(), new Phrase("vedi",font), 420+doc.leftMargin(), psosizioneVeriticale+45);
             setPara(writer.getDirectContent(), new Phrase("note",font), 420+doc.leftMargin(), psosizioneVeriticale+38);
             //Solo Monitoraggio
             if (Giorni.isBlank()||Giorni.contains("(")){
                setPara(writer.getDirectContent(), new Phrase("X",font), 505+doc.leftMargin(), psosizioneVeriticale+40);
             }
             
             //Font font = new Font(Font.HELVETICA, 6, Font.NORMAL);       
             //HeaderFooter footer = new HeaderFooter(new Phrase("155",font), false);
             
             //doc.add(image01);
             

          } catch (BadElementException | IOException ex) {
              Logger.getLogger(Stampe.class.getName()).log(Level.SEVERE, null, ex);
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
                 setPara(writer.getDirectContent(), new Phrase(ICTot,font), 150+doc.leftMargin(), psosizioneVeriticale+12);
                 setPara(writer.getDirectContent(), new Phrase(ICTot,font), 405+doc.leftMargin(), psosizioneVeriticale+12);
             }
             font = new Font(Font.HELVETICA, 10, Font.BOLD);
             setPara(writer.getDirectContent(), new Phrase("QUADRO RW PER CRIPTO-ATTIVITA'",font), 200+doc.leftMargin(), psosizioneVeriticale+655);
             for (int i=0;i<5;i++){
                 if (ValoriIniziali[i]!=null){
                   // font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                   ValoriIniziali[i]=Funzioni.formattaBigDecimal(new BigDecimal(ValoriIniziali[i]),false);
                        ValoriFinali[i]=Funzioni.formattaBigDecimal(new BigDecimal(ValoriFinali[i]),false);
                    if (i==0){
                        //Wallet e Note
                        font = new Font(Font.HELVETICA, 10, Font.BOLD);
                        setPara(writer.getDirectContent(), new Phrase(Wallet[i],font), 5+doc.leftMargin(), psosizioneVeriticale+525);
                        font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                        setPara(writer.getDirectContent(), new Phrase(Note[i],font), 5+doc.leftMargin(), psosizioneVeriticale+510);
                        //Codice Possesso
                        setPara(writer.getDirectContent(), new Phrase("1",font), 142+doc.leftMargin(), psosizioneVeriticale+540);
                        //Codice Individuazione Bene
                        setPara(writer.getDirectContent(), new Phrase("21",font), 220+doc.leftMargin(), psosizioneVeriticale+540);
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
                        //Codice 14
                        setPara(writer.getDirectContent(), new Phrase("vedi",font), 425+doc.leftMargin(), psosizioneVeriticale+513);
                        setPara(writer.getDirectContent(), new Phrase("note",font), 425+doc.leftMargin(), psosizioneVeriticale+506);
                        //Solo Monitoraggio
                        if (Giorni[i].isBlank()||Giorni[i].contains("(")){
                            setPara(writer.getDirectContent(), new Phrase("X",font), 496+doc.leftMargin(), psosizioneVeriticale+508);
                        }else{
                        //IC
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 410 + doc.leftMargin(), psosizioneVeriticale + 445);
                            setPara(writer.getDirectContent(), new Phrase(IC[i], font), 475 + doc.leftMargin(), psosizioneVeriticale + 445); 
                        }
                    }else{
                        //Wallet e Note
                        font = new Font(Font.HELVETICA, 10, Font.BOLD);
                        setPara(writer.getDirectContent(), new Phrase(Wallet[i],font), 5+doc.leftMargin(), psosizioneVeriticale+410-(i-1)*84);
                        font = new Font(Font.HELVETICA, 8, Font.NORMAL);
                        setPara(writer.getDirectContent(), new Phrase(Note[i],font), 5+doc.leftMargin(), psosizioneVeriticale+395-(i-1)*84);
                        //Codice Possesso
                        setPara(writer.getDirectContent(), new Phrase("1",font), 142+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
                        //Codice Individuazione Bene
                        setPara(writer.getDirectContent(), new Phrase("21",font), 220+doc.leftMargin(), psosizioneVeriticale+425-(i-1)*84);
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
                        //Codice 14
                        setPara(writer.getDirectContent(), new Phrase("vedi",font), 425+doc.leftMargin(), psosizioneVeriticale+409-(i-1)*84);
                        setPara(writer.getDirectContent(), new Phrase("note",font), 425+doc.leftMargin(), psosizioneVeriticale+402-(i-1)*84);
                        //Solo Monitoraggio
                        if (Giorni[i].isBlank()||Giorni[i].contains("(")){
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
    
    public void setPara(PdfContentByte canvas, Phrase p, float x, float y) {
    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p, x, y, 0);
}
    
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
    
    public void AggiungiHtml(String html){
          try {
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
    
    public void AggiungiTesto(String Testo, int intfont, float size) {
        //Element e =new Element(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));
        Paragraph par = new Paragraph();
        par.setAlignment(Element.ALIGN_LEFT);
        par.add(new Chunk(Testo, FontFactory.getFont(FontFactory.COURIER, size, intfont)));              
        doc.add(par);
        //par.add(new Chunk(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont)));
        //  Paragraph par = new Paragraph(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));

    }
    
    public void AggiungiTestoCentrato(String Testo,int intfont,float size){
              Paragraph par = new Paragraph(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));
              par.setAlignment(Element.ALIGN_CENTER);
              doc.add(par);

 
     }






}
