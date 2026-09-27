package com.giacenzecrypto.giacenze_crypto;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rigenera i PDF dei manuali ({@code docs/documentazione/*.md}) con la stessa veste grafica delle stampe dei
 * quadri W/RW ({@link Stampe#AttivaVesteDocumento}): copertina, testata col logo, fascia nel margine, filigrana,
 * piede numerato e Noto Sans incorporato. Lanciato da {@code docs/strumenti/genera-pdf.sh}; non fa parte
 * dell'applicazione.
 *
 * <p>La fonte resta il Markdown, che GitHub Pages pubblica come pagina web. I PDF restano pubblicati perche' le
 * versioni del programma fino alla 1.0.61 aprono i manuali all'indirizzo {@code .../documentazione/<nome>.pdf}
 * (vedi {@code DocumentiAiutoTest}), quindi i nomi dei file non cambiano. Il convertitore copre il sottoinsieme
 * di Markdown usato dai manuali: titoli con ancora esplicita {@code {#id}} (che diventano destinazioni cliccabili
 * e segnalibri del PDF), grassetto, corsivo, codice, collegamenti, liste anche annidate, citazioni, blocchi di
 * codice, tabelle, immagini e righe orizzontali.</p>
 */
public class GeneraPdfDocumentazione {

    private static final Color FONDO_CITAZIONE = new Color(0xF6, 0xF6, 0xEC);
    private static final Color FONDO_CODICE = new Color(0xF4, 0xF4, 0xF4);
    private static final Color FONDO_RIGA_ALTERNA = new Color(0xFA, 0xFA, 0xF5);
    private static final Color BLU_LINK = new Color(0x2A, 0x5D, 0x8F);
    private static final float CORPO = 9.5f;

    private static BaseFont bfCorsivo, bfGrassettoCorsivo;
    private static Document doc;
    private static PdfWriter writer;
    private static File cartella;
    private static PdfOutline capitoloCorrente;

    private GeneraPdfDocumentazione() {
    }

    public static void main(String[] args) throws Exception {
        cartella = new File(args.length > 0 ? args[0] : "docs/documentazione");
        File[] sorgenti = cartella.listFiles((d, n) -> n.endsWith(".md"));
        if (sorgenti == null) throw new IllegalArgumentException("Cartella non trovata: " + cartella);
        java.util.Arrays.sort(sorgenti);
        for (File md : sorgenti) {
            String nome = md.getName().replace(".md", "");
            if (nome.equals("index") || nome.equals("changelog")) continue;   // solo pagine web
            genera(md, new File(cartella, nome + ".pdf"));
            System.out.println("scritto " + nome + ".pdf");
        }
    }

    private static void genera(File md, File pdf) throws Exception {
        String testo = Files.readString(md.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        String titolo = md.getName();
        Matcher fm = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(testo);
        if (fm.find()) {
            Matcher t = Pattern.compile("(?m)^title:\\s*(.+)$").matcher(fm.group(1));
            if (t.find()) titolo = t.group(1).trim();
            testo = testo.substring(fm.end());
        }
        Matcher h1 = Pattern.compile("(?m)^#\\s+(.+)$").matcher(testo);
        if (h1.find()) titolo = h1.group(1).replaceAll("\\s*\\{#[^}]*\\}\\s*$", "").trim();

        Stampe stampa = new Stampe(pdf.getPath());
        doc = Stampe.doc;
        writer = Stampe.writer;
        stampa.AttivaVesteDocumento("DOCUMENTAZIONE");
        if (bfCorsivo == null) {
            bfCorsivo = BaseFont.createFont("/Fonts/NotoSans-Italic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bfGrassettoCorsivo = BaseFont.createFont("/Fonts/NotoSans-BoldItalic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
        // un'immagine che non ci sta va alla pagina dopo senza che il testo successivo la scavalchi
        writer.setStrictImageSequence(true);
        stampa.ApriDocumento();
        doc.addTitle(titolo);
        doc.addAuthor("Giacenze Crypto");
        copertina(titolo, md.getName().replace(".md", ""));
        doc.newPage();
        stampa.ContestoPagina(titolo, false);
        capitoloCorrente = null;
        corpo(testo);
        doc.close();
        writer.close();
    }

    // ------------------------------------------------------------------ copertina

    private static void copertina(String titolo, String nome) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float W = doc.getPageSize().getWidth();
        float H = doc.getPageSize().getHeight();
        Stampe.cornice.PaginaCopertina = writer.getPageNumber();

        Image logo = Stampe.LogoApplicazione();
        if (logo != null) {
            logo.scaleAbsolute(54, 54);
            logo.setAbsolutePosition(70, H - 120);
            cb.addImage(logo);
        }
        Stampe.Testo(cb, "GIACENZE CRYPTO", Stampe.bfBold, 12, Stampe.NERO, 136, H - 84, Element.ALIGN_LEFT, 3.5f);
        Stampe.Testo(cb, "monitoraggio e fiscalità delle cripto-attività", Stampe.bfRegular, 8, Stampe.GRIGIO_TENUE,
                136, H - 98, Element.ALIGN_LEFT, 0.5f);
        Stampe.Linea(cb, 70, H - 140, W - 70, H - 140, Stampe.VERDE, 0.8f);

        Stampe.Testo(cb, "MANUALE", Stampe.bfRegular, 9.5f, Stampe.VERDE_SCURO, 70, H - 300, Element.ALIGN_LEFT, 3f);
        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(70, 260, W - 70, H - 312);
        Paragraph p = new Paragraph(new Chunk(titolo, new Font(Stampe.bfBold, 30, Font.NORMAL, Stampe.NERO)));
        p.setLeading(36f);
        ct.addElement(p);
        ct.go();
        float y = ct.getYLine() - 18;
        Stampe.Linea(cb, 70, y, 240, y, Stampe.VERDE, 2.5f);
        Stampe.Testo(cb, "Documentazione di Giacenze Crypto", Stampe.bfRegular, 13, Stampe.GRIGIO_TESTO, 70, y - 30,
                Element.ALIGN_LEFT, 0.5f);

        float yd = 250;
        Stampe.Linea(cb, 70, yd + 22, W - 70, yd + 22, Stampe.GRIGIO_FILO, 0.6f);
        Stampe.RigaDato(cb, "Documento", nome + ".pdf", 70, yd, W - 70);
        Stampe.RigaDato(cb, "Sito", DocumentiAiuto.BASE.replaceFirst("^https?://", "").replaceAll("/$", ""), 70, yd - 26, W - 70);
        Stampe.RigaDato(cb, "Generato il", new SimpleDateFormat("dd/MM/yyyy").format(new Date()), 70, yd - 52, W - 70);
        Stampe.RigaDato(cb, "Versione", VarStatiche.Titolo, 70, yd - 78, W - 70);

        Stampe.Testo(cb, "AVVERTENZA", Stampe.bfBold, 7.5f, Stampe.VERDE_SCURO, 70, 108, Element.ALIGN_LEFT, 2f);
        Stampe.Testo(cb, "I documenti ottenuti e le informazioni presenti hanno sempre valenza informativa e meramente",
                Stampe.bfRegular, 7.5f, Stampe.GRIGIO_TESTO, 70, 94, Element.ALIGN_LEFT, 0f);
        Stampe.Testo(cb, "indicativa ed esemplificativa, e non sono in alcun modo sostitutive di una consulenza fiscale.",
                Stampe.bfRegular, 7.5f, Stampe.GRIGIO_TESTO, 70, 83, Element.ALIGN_LEFT, 0f);
        Stampe.Linea(cb, 70, 58, W - 70, 58, Stampe.VERDE, 0.8f);
        String rif = VarStatiche.RiferimentoStampe();
        if (!rif.isBlank()) {
            Stampe.Testo(cb, rif.replaceFirst("^\\s*-\\s*", ""), Stampe.bfRegular, 8, Stampe.GRIGIO_TENUE, 70, 42,
                    Element.ALIGN_LEFT, 1f);
        }
        doc.add(new Paragraph(" "));
    }

    // ------------------------------------------------------------------ blocchi

    private static final Pattern TITOLO = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern VOCE = Pattern.compile("^(\\s*)([-*]|\\d+\\.)\\s+(.*)$");
    private static final Pattern SEPARATORE_TABELLA = Pattern.compile("^\\|[\\s:|-]+\\|$");
    private static final Pattern SOLA_IMMAGINE = Pattern.compile("^!\\[([^\\]]*)\\]\\(([^)]+)\\)$");

    private static boolean inizioBlocco(String r) {
        String s = r.strip();
        return s.startsWith("#") || s.startsWith("|") || s.startsWith(">") || s.startsWith("```")
                || VOCE.matcher(r).matches() || s.equals("---");
    }

    private static void corpo(String md) throws Exception {
        String[] righe = md.split("\n", -1);
        int[] contatori = new int[10];
        int i = 0;
        while (i < righe.length) {
            String r = righe[i];
            String s = r.strip();
            if (s.isEmpty() || s.startsWith("[Torna all'indice")) {
                i++;
                continue;
            }
            if (s.startsWith("```")) {
                List<String> codice = new ArrayList<>();
                i++;
                while (i < righe.length && !righe[i].strip().startsWith("```")) codice.add(righe[i++]);
                i++;
                bloccoCodice(codice);
                continue;
            }
            if (s.startsWith("|") && i + 1 < righe.length && SEPARATORE_TABELLA.matcher(righe[i + 1].strip()).matches()) {
                List<String[]> tab = new ArrayList<>();
                tab.add(celle(s));
                i += 2;
                while (i < righe.length && righe[i].strip().startsWith("|")) tab.add(celle(righe[i++].strip()));
                tabella(tab);
                continue;
            }
            Matcher t = TITOLO.matcher(s);
            if (t.matches()) {
                titolo(t.group(1).length(), t.group(2));
                java.util.Arrays.fill(contatori, 0);
                i++;
                continue;
            }
            if (s.startsWith(">")) {
                StringBuilder sb = new StringBuilder();
                while (i < righe.length && righe[i].strip().startsWith(">")) {
                    sb.append(righe[i].strip().replaceFirst("^>\\s?", "")).append(' ');
                    i++;
                }
                citazione(sb.toString().trim());
                continue;
            }
            Matcher v = VOCE.matcher(r);
            if (v.matches()) {
                int livello = Math.min(v.group(1).replace("\t", "    ").length() / 2, 9);
                StringBuilder sb = new StringBuilder(v.group(3));
                i++;
                while (i < righe.length && !righe[i].strip().isEmpty() && !inizioBlocco(righe[i])) {
                    sb.append(' ').append(righe[i].strip());
                    i++;
                }
                String segno;
                if (Character.isDigit(v.group(2).charAt(0))) {
                    contatori[livello]++;
                    segno = contatori[livello] + ".";
                } else {
                    segno = "•";
                }
                for (int k = livello + 1; k < contatori.length; k++) contatori[k] = 0;
                voce(livello, segno, sb.toString());
                continue;
            }
            if (s.equals("---") || s.equals("***") || s.equals("___")) {
                LineSeparator ls = new LineSeparator(0.6f, 100, Stampe.GRIGIO_FILO, Element.ALIGN_CENTER, -4);
                doc.add(new Chunk(ls));
                i++;
                continue;
            }
            StringBuilder sb = new StringBuilder(s);
            i++;
            while (i < righe.length && !righe[i].strip().isEmpty() && !inizioBlocco(righe[i])) {
                sb.append(' ').append(righe[i].strip());
                i++;
            }
            String par = sb.toString();
            Matcher img = SOLA_IMMAGINE.matcher(par);
            if (img.matches()) {
                immagine(img.group(2), img.group(1));
            } else {
                Paragraph p = paragrafo(par, CORPO);
                p.setSpacingAfter(5f);
                doc.add(p);
            }
        }
    }

    private static void titolo(int livello, String testoGrezzo) throws Exception {
        Matcher a = Pattern.compile("\\{#([^}]*)\\}\\s*$").matcher(testoGrezzo);
        String ancora = a.find() ? a.group(1) : null;
        String testo = testoGrezzo.replaceAll("\\s*\\{#[^}]*\\}\\s*$", "").trim();
        if (livello == 1) return;   // e' il titolo in copertina
        float dim = livello == 2 ? 14f : livello == 3 ? 11.5f : 10f;
        Color colore = livello == 2 ? Stampe.NERO : livello == 3 ? Stampe.VERDE_SCURO : Stampe.GRIGIO_TESTO;
        Paragraph p = new Paragraph();
        p.setSpacingBefore(livello == 2 ? 14f : 9f);
        p.setSpacingAfter(livello == 2 ? 2f : 4f);
        p.setKeepTogether(true);
        List<Chunk> pezzi = inline(testo, new Stile(true, false, false, null), dim, colore);
        if (ancora != null && !pezzi.isEmpty()) pezzi.get(0).setLocalDestination(ancora);
        for (Chunk c : pezzi) p.add(c);
        float yPrima = writer.getVerticalPosition(false);
        doc.add(p);
        if (livello == 2) {
            doc.add(new Chunk(new LineSeparator(0.8f, 100, Stampe.VERDE, Element.ALIGN_LEFT, -3)));
            Paragraph spazio = new Paragraph(" ");
            spazio.setLeading(6f);
            doc.add(spazio);
        }
        if (livello <= 3) {
            String etichetta = testoSemplice(testo);
            PdfDestination dest = new PdfDestination(PdfDestination.FITH, yPrima + 20);
            if (livello == 2) {
                capitoloCorrente = new PdfOutline(writer.getRootOutline(), dest, etichetta, false);
            } else {
                new PdfOutline(capitoloCorrente != null ? capitoloCorrente : writer.getRootOutline(), dest, etichetta);
            }
        }
    }

    private static void voce(int livello, String segno, String testo) throws Exception {
        Paragraph p = new Paragraph();
        float rientro = 14f + livello * 14f;
        p.setIndentationLeft(rientro);
        p.setFirstLineIndent(-11f);
        p.setLeading(CORPO * 1.45f);
        p.setSpacingAfter(2.5f);
        for (Chunk c : testoConRipiego(segno + "  ", new Font(Stampe.bfRegular, CORPO, Font.NORMAL, Stampe.VERDE_SCURO))) p.add(c);
        for (Chunk c : inline(testo, new Stile(false, false, false, null), CORPO, Stampe.NERO)) p.add(c);
        doc.add(p);
    }

    private static void citazione(String testo) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(3f);
        t.setSpacingAfter(7f);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.LEFT);
        c.setBorderColorLeft(Stampe.VERDE);
        c.setBorderWidthLeft(2.5f);
        c.setBackgroundColor(FONDO_CITAZIONE);
        c.setPadding(7f);
        c.setPaddingLeft(10f);
        Paragraph p = new Paragraph();
        p.setLeading(CORPO * 1.4f);
        for (Chunk k : inline(testo, new Stile(false, true, false, null), CORPO, Stampe.GRIGIO_TESTO)) p.add(k);
        c.addElement(p);
        t.addCell(c);
        doc.add(t);
    }

    private static void bloccoCodice(List<String> righe) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(3f);
        t.setSpacingAfter(7f);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(FONDO_CODICE);
        c.setPadding(7f);
        Font mono = new Font(Font.COURIER, 7.5f, Font.NORMAL, Stampe.NERO);
        Paragraph p = new Paragraph();
        p.setLeading(9.5f);
        for (int k = 0; k < righe.size(); k++) {
            p.add(new Chunk(soloLatino(righe.get(k)).replace("\t", "    "), mono));
            if (k < righe.size() - 1) p.add(Chunk.NEWLINE);
        }
        c.addElement(p);
        t.addCell(c);
        doc.add(t);
    }

    private static void tabella(List<String[]> righe) throws Exception {
        int n = 0;
        for (String[] r : righe) n = Math.max(n, r.length);
        float dim = n <= 3 ? 8.5f : n <= 5 ? 8f : n <= 7 ? 7.2f : n <= 10 ? 6.4f : 5.8f;
        float[] larghezze = new float[n];
        for (String[] r : righe) {
            for (int k = 0; k < r.length; k++) {
                String cella = testoSemplice(r[k]);
                int parolaLunga = 0;
                for (String w : cella.split("\\s+")) parolaLunga = Math.max(parolaLunga, w.length());
                // mai meno della parola piu' lunga: un'intestazione spezzata a meta' ("Caus-ale") non si legge
                larghezze[k] = Math.max(larghezze[k], Math.max(parolaLunga + 1, Math.min(38, Math.max(4, cella.length()))));
            }
        }
        PdfPTable t = new PdfPTable(n);
        t.setWidthPercentage(100);
        t.setWidths(larghezze);
        t.setHeaderRows(1);
        t.setSplitLate(false);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(8f);
        for (int ri = 0; ri < righe.size(); ri++) {
            String[] r = righe.get(ri);
            for (int k = 0; k < n; k++) {
                String cella = k < r.length ? r[k] : "";
                PdfPCell c = new PdfPCell();
                c.setBorderColor(Stampe.GRIGIO_FILO);
                c.setBorderWidth(0.5f);
                c.setPadding(3f);
                c.setPaddingBottom(4f);
                if (ri == 0) c.setBackgroundColor(Stampe.FASCIA_MARGINE);
                else if (ri % 2 == 0) c.setBackgroundColor(FONDO_RIGA_ALTERNA);
                Paragraph p = new Paragraph();
                p.setLeading(dim * 1.3f);
                for (Chunk ch : inline(cella, new Stile(ri == 0, false, false, null), dim, Stampe.NERO)) p.add(ch);
                c.addElement(p);
                t.addCell(c);
            }
        }
        doc.add(t);
    }

    private static void immagine(String percorso, String alt) throws Exception {
        File f = new File(cartella, percorso);
        if (!f.isFile()) {
            System.out.println("  immagine mancante: " + percorso);
            return;
        }
        Image im = Image.getInstance(f.getPath());
        float larghezzaUtile = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
        float altezzaUtile = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin();
        // una cattura dello schermo a dimensione naturale (circa 96 dpi) e' leggibile a 0,6 punti per pixel
        float l = im.getWidth() * 0.6f, h = im.getHeight() * 0.6f;
        float fattore = Math.min(1f, Math.min(larghezzaUtile / l, altezzaUtile * 0.62f / h));
        im.scaleAbsolute(l * fattore, h * fattore);
        im.setAlignment(Image.MIDDLE);
        im.setAlt(alt);
        im.setSpacingBefore(4f);
        im.setSpacingAfter(8f);
        // elemento a se' e non dentro un paragrafo: solo cosi' un'immagine che non ci sta passa alla pagina dopo
        doc.add(im);
    }

    // ------------------------------------------------------------------ testo in linea

    private record Stile(boolean grassetto, boolean corsivo, boolean codice, String collegamento) {
        Stile con(boolean g, boolean c, boolean cod, String link) {
            return new Stile(grassetto || g, corsivo || c, codice || cod, link != null ? link : collegamento);
        }
    }

    private static final Pattern IN_LINEA = Pattern.compile(
            "\\[([^\\]]+)\\]\\(([^)]+)\\)"           // 1,2 collegamento
            + "|`([^`]+)`"                              // 3 codice
            + "|\\*\\*([^*]+?)\\*\\*"                   // 4 grassetto
            + "|(?<![\\w*])\\*([^*\\n]+?)\\*(?![\\w*])"); // 5 corsivo

    private static Paragraph paragrafo(String testo, float dim) {
        Paragraph p = new Paragraph();
        p.setLeading(dim * 1.45f);
        for (Chunk c : inline(testo, new Stile(false, false, false, null), dim, Stampe.NERO)) p.add(c);
        return p;
    }

    private static List<Chunk> inline(String testo, Stile st, float dim, Color colore) {
        List<Chunk> out = new ArrayList<>();
        Matcher m = IN_LINEA.matcher(testo);
        int pos = 0;
        while (m.find()) {
            if (m.start() > pos) out.addAll(letterale(testo.substring(pos, m.start()), st, dim, colore));
            if (m.group(1) != null) {
                out.addAll(inline(m.group(1), st.con(false, false, false, m.group(2)), dim, colore));
            } else if (m.group(3) != null) {
                out.addAll(letterale(m.group(3), st.con(false, false, true, null), dim, colore));
            } else if (m.group(4) != null) {
                out.addAll(inline(m.group(4), st.con(true, false, false, null), dim, colore));
            } else {
                out.addAll(inline(m.group(5), st.con(false, true, false, null), dim, colore));
            }
            pos = m.end();
        }
        if (pos < testo.length()) out.addAll(letterale(testo.substring(pos), st, dim, colore));
        return out;
    }

    private static List<Chunk> letterale(String s, Stile st, float dim, Color colore) {
        if (!st.codice()) s = s.replace("*", "");   // asterischi rimasti da enfasi non chiuse
        s = s.replace("\\|", "|");
        Font f;
        if (st.codice()) {
            f = new Font(Font.COURIER, dim * 0.95f, Font.NORMAL, new Color(0x7A, 0x28, 0x10));
        } else {
            BaseFont bf = st.grassetto() && st.corsivo() ? bfGrassettoCorsivo
                    : st.grassetto() ? Stampe.bfBold : st.corsivo() ? bfCorsivo : Stampe.bfRegular;
            f = new Font(bf, dim, Font.NORMAL, st.collegamento() != null ? BLU_LINK : colore);
        }
        List<Chunk> pezzi = st.codice() ? List.of(new Chunk(soloLatino(s), f)) : testoConRipiego(s, f);
        if (st.collegamento() != null) {
            String link = st.collegamento();
            for (Chunk c : pezzi) {
                if (link.startsWith("#")) {
                    c.setLocalGoto(link.substring(1));
                } else if (link.startsWith("http")) {
                    c.setAnchor(link);
                } else if (!link.startsWith(".")) {
                    c.setAnchor(DocumentiAiuto.BASE + link);   // un altro manuale: la sua pagina web
                }
            }
        }
        return pezzi;
    }

    /**
     * Spezza il testo in pezzi nel font richiesto e, per i caratteri che Noto Sans non ha (frecce e simili),
     * nel font Symbol di base del PDF: un carattere mancante altrimenti sparirebbe dalla pagina.
     */
    private static List<Chunk> testoConRipiego(String s, Font f) {
        List<Chunk> out = new ArrayList<>();
        BaseFont bf = f.getBaseFont();
        StringBuilder corrente = new StringBuilder();
        boolean ripiego = false;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            boolean manca = bf != null && !bf.charExists(c) && !Character.isWhitespace(c);
            if (manca != ripiego && corrente.length() > 0) {
                out.add(pezzo(corrente.toString(), f, ripiego));
                corrente.setLength(0);
            }
            ripiego = manca;
            corrente.append(c);
        }
        if (corrente.length() > 0) out.add(pezzo(corrente.toString(), f, ripiego));
        return out;
    }

    private static Chunk pezzo(String s, Font f, boolean ripiego) {
        if (!ripiego) return new Chunk(s, f);
        return new Chunk(s, new Font(Font.SYMBOL, f.getSize(), Font.NORMAL, f.getColor()));
    }

    private static String soloLatino(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(c < 256 || c == '€' ? c : '?');
        return sb.toString();
    }

    private static String testoSemplice(String md) {
        return md.replaceAll("!?\\[([^\\]]*)\\]\\([^)]+\\)", "$1").replace("**", "").replace("`", "").replace("*", "").trim();
    }

    private static String[] celle(String riga) {
        String interno = riga.strip();
        if (interno.startsWith("|")) interno = interno.substring(1);
        if (interno.endsWith("|")) interno = interno.substring(0, interno.length() - 1);
        String[] parti = interno.split("(?<!\\\\)\\|", -1);
        for (int k = 0; k < parti.length; k++) parti[k] = parti[k].trim();
        return parti;
    }
}
