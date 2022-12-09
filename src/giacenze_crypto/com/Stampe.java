/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

/**
 *
 * @author luca.passelli
 */
public class Stampe {
      static Document doc = new Document();
      static PdfWriter writer;
     public Stampe(String PDFPath) throws FileNotFoundException {
       writer = PdfWriter.getInstance(doc, new FileOutputStream(PDFPath));
       doc.open();  
       
     } 
    public static void ScriviPDF(){
    try {
      Font font = new Font(Font.HELVETICA, 12, Font.BOLDITALIC);
     // Document doc = new Document();
      //PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(PDFPath));
      PdfPTable table = new PdfPTable(4);
      table.setWidthPercentage(100);
      // setting column widths
      table.setWidths(new float[] {6.0f, 6.0f, 6.0f, 6.0f});
      PdfPCell cell = new PdfPCell();
      // table headers
      cell.setPhrase(new Phrase("First Name", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("Last Name", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("Email", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("DOB", font));
      table.addCell(cell);
   /*   List<User> users = getListOfUsers();
      // adding table rows
      for(User user : users) {
        table.addCell(user.getFirstName());
        table.addCell(user.getLastName());
        table.addCell(user.getEmail());
        table.addCell(new SimpleDateFormat("dd/MM/yyyy").format(user.getDob()));
      }*/
      // adding table to document
      doc.add(table);
      doc.close();
      writer.close();
      System.out.println("PDF using OpenPDF created successfully");
    } catch (DocumentException  e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
     }
    public static void AggiungiTitolo(String PDFPath){

     }
    public static void AggiungiTabella(String PDFPath){

     }
    public static void AggiungiTesto(String PDFPath){

     }
    
  public static void creaTabellaPDF(String PDFPath,String[] Titoli, List<String[]> Dettagli){
    try {
      Font font = new Font(Font.HELVETICA, 12, Font.BOLDITALIC);
     // Document doc = new Document();
      PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(PDFPath));
      PdfPTable table = new PdfPTable(4);
      table.setWidthPercentage(100);
      // setting column widths
      table.setWidths(new float[] {6.0f, 6.0f, 6.0f, 6.0f});
      PdfPCell cell = new PdfPCell();
      // table headers
      cell.setPhrase(new Phrase("First Name", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("Last Name", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("Email", font));
      table.addCell(cell);
      cell.setPhrase(new Phrase("DOB", font));
      table.addCell(cell);
   /*   List<User> users = getListOfUsers();
      // adding table rows
      for(User user : users) {
        table.addCell(user.getFirstName());
        table.addCell(user.getLastName());
        table.addCell(user.getEmail());
        table.addCell(new SimpleDateFormat("dd/MM/yyyy").format(user.getDob()));
      }*/
      doc.open();
      // adding table to document
      doc.add(table);
      doc.close();
      writer.close();
      System.out.println("PDF using OpenPDF created successfully");
    } catch (DocumentException | FileNotFoundException  e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }






}
