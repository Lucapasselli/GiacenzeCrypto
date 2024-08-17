/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    
    public void AggiungiImmagine(String Immagine) {
          try {
              com.lowagie.text.Image image01 = com.lowagie.text.Image.getInstance(Immagine);
              doc.add(image01);
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
    
    
    public void AggiungiTesto(String Testo,int intfont,float size){
              Paragraph par = new Paragraph(Testo,FontFactory.getFont(FontFactory.COURIER,size, intfont));              
              doc.add(par);

 
     }
    







}
