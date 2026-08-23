package com.giacenzecrypto.giacenze_crypto;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.awt.Desktop;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Logica operativa del tab "Normative": legge il catalogo da {@code Normativa/fonti.csv},
 * orchestra l'aggiornamento dal repository GitHub, estrae e cachea il testo dei documenti per
 * la ricerca a testo libero, apre una copia del file o la fonte ufficiale in browser.
 *
 * <p>Sul modello di {@link Principale_DocumentiFonte}: metodi {@code public static}, nessun campo
 * Swing, nessun riferimento a {@link Principale}. La parte grafica resta in
 * {@link GUI_Normativa_Pannello}.
 */
public class Principale_Normativa {

    /** Voce "nessun filtro" della combo categoria: mai una combo costruita vuota. */
    public static final String TUTTE_LE_CATEGORIE = "Tutte";

    /**
     * Cartella di primo livello sotto {@code Normativa/} (due livelli sotto {@code Leggi/}, dove
     * "originale"/"estratti"/"consolidato" sono la distinzione che conta) -> etichetta leggibile
     * per la combo categoria. Le sei chiavi sono le cartelle descritte in {@code Normativa/README.md}.
     */
    private static final Map<String, String> ETICHETTE_CATEGORIA = new LinkedHashMap<>();
    static {
        ETICHETTE_CATEGORIA.put("Leggi/Originale", "Leggi — testo originale");
        ETICHETTE_CATEGORIA.put("Leggi/Estratti", "Leggi — estratti");
        ETICHETTE_CATEGORIA.put("Leggi/Consolidato", "Leggi — consolidato");
        ETICHETTE_CATEGORIA.put("Prassi_AgenziaEntrate", "Prassi Agenzia delle Entrate");
        ETICHETTE_CATEGORIA.put("Istruzioni_Dichiarazioni", "Istruzioni dichiarazioni");
        ETICHETTE_CATEGORIA.put("ISEE_DSU", "ISEE / DSU");
    }

    /** @return le etichette di categoria da mettere in combo, "Tutte" compreso e in testa */
    public static List<String> EtichetteCategorie() {
        List<String> risultato = new ArrayList<>();
        risultato.add(TUTTE_LE_CATEGORIE);
        risultato.addAll(ETICHETTE_CATEGORIA.values());
        return risultato;
    }

    /** @return l'etichetta di categoria del percorso, o la cartella stessa se non mappata */
    public static String Categoria(String percorso) {
        String[] parti = percorso.split("/");
        String chiave = (parti.length > 1 && "Leggi".equals(parti[0])) ? parti[0] + "/" + parti[1] : parti[0];
        return ETICHETTE_CATEGORIA.getOrDefault(chiave, chiave);
    }

    // ------------------------------------------------------------------ catalogo (fonti.csv)

    /**
     * Legge {@code Normativa/fonti.csv} e lo converte nel catalogo dei documenti.
     * @return il catalogo, vuoto se {@code Normativa/} non è (ancora) stato scaricato o il file manca
     */
    public static List<DocumentoNormativa> LeggiCatalogo() {
        List<DocumentoNormativa> risultato = new ArrayList<>();
        Path file = Paths.get(VarStatiche.getCartella_Normativa(), "fonti.csv");
        if (!Files.exists(file)) {
            return risultato;
        }
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String intestazione = br.readLine();
            if (intestazione == null) {
                return risultato;
            }
            String riga;
            while ((riga = br.readLine()) != null) {
                if (riga.isBlank()) {
                    continue;
                }
                List<String> c = ParsaRigaCSV(riga);
                //11 colonne: file;tipo;titolo;autorita;identificativo;data_documento;argomenti;url;scaricato_il;sha256;byte
                if (c.size() < 11) {
                    continue;
                }
                String percorso = c.get(0);
                risultato.add(new DocumentoNormativa(
                        percorso, c.get(1), c.get(2), c.get(3), c.get(4),
                        ParsaData(c.get(5)), ParsaArgomenti(c.get(6)), c.get(7), c.get(9),
                        Categoria(percorso)));
            }
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return risultato;
    }

    private static LocalDate ParsaData(String valore) {
        if (valore == null || valore.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valore.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static List<String> ParsaArgomenti(String valore) {
        List<String> risultato = new ArrayList<>();
        if (valore == null || valore.isBlank()) {
            return risultato;
        }
        for (String a : valore.split(",")) {
            String t = a.trim();
            if (!t.isEmpty()) {
                risultato.add(t);
            }
        }
        return risultato;
    }

    /**
     * Un piccolo parser CSV a mano: {@code fonti.csv} è scritto da Python {@code csv.writer} con
     * delimitatore {@code ;} e virgolette per i soli campi che contengono il delimitatore (oggi solo
     * l'{@code url} dei consolidati, che elenca più file separati da {@code "; "}). Niente a che
     * vedere con il formato di {@code movimenti.crypto.db}, che invece non cita mai il delimitatore
     * e per questo può usare uno split ingenuo.
     */
    private static List<String> ParsaRigaCSV(String riga) {
        List<String> campi = new ArrayList<>();
        StringBuilder corrente = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < riga.length(); i++) {
            char c = riga.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < riga.length() && riga.charAt(i + 1) == '"') {
                        corrente.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    corrente.append(c);
                }
            } else if (c == '"') {
                inQuote = true;
            } else if (c == ';') {
                campi.add(corrente.toString());
                corrente.setLength(0);
            } else {
                corrente.append(c);
            }
        }
        campi.add(corrente.toString());
        return campi;
    }

    // ------------------------------------------------------------------ aggiornamento da GitHub

    /**
     * Scarica/aggiorna {@code Normativa/} dal repository GitHub, con la finestra di attesa
     * {@link Download} (stesso pattern di {@link Principale_Opzioni_Pulizie#CompattaSeConviene}) e
     * un messaggio di esito.
     *
     * @param owner finestra a cui agganciare l'attesa e i messaggi
     * @return il riepilogo dell'operazione, o {@code null} se il thread non è mai partito
     */
    public static Funzioni.RisultatoNormativa Aggiorna(Window owner) {
        Download dow = new Download();
        dow.NascondiInterrompi();
        dow.MostraProgressAttesa("Normative", "Scaricamento in corso da GitHub...");
        dow.SetLabel("Confronto con il repository, attendere...");
        dow.setLocationRelativeTo(owner);

        AtomicReference<Funzioni.RisultatoNormativa> esito = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                esito.set(Funzioni.AggiornamentoNormativaDaRepository());
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            } finally {
                dow.ChiudiFinestra();
            }
        });
        t.start();
        dow.setVisible(true); //bloccante finché il thread chiama ChiudiFinestra

        Funzioni.RisultatoNormativa r = esito.get();
        if (r == null || !r.riuscito()) {
            Messaggi.WarningMessage("Aggiornamento non riuscito",
                    "Impossibile scaricare l'archivio da GitHub. Verifica la connessione e riprova: i file già presenti restano invariati.",
                    owner);
        } else if (r.aggiornati().isEmpty() && r.cancellati().isEmpty()) {
            Messaggi.InfoMessage("Normative aggiornate", "L'archivio locale è già allineato al repository.", owner);
        } else {
            Messaggi.SuccessMessage("Normative aggiornate",
                    "%d file scaricati, %d rimossi.".formatted(r.aggiornati().size(), r.cancellati().size()),
                    owner);
        }
        return r;
    }

    // ------------------------------------------------------------------ apertura file/url

    /**
     * Apre una copia del documento con l'applicazione predefinita del sistema. Un XML Akoma Ntoso
     * non è leggibile da una persona (è markup strutturato, non testo formattato): viene prima
     * convertito in un file di testo semplice in {@code Temporanei/}, sul modello di
     * {@link DocumentiFonte#EstraiPerApertura}, ed è quella copia ad essere aperta.
     */
    public static void ApriCopia(DocumentoNormativa d, Window owner) {
        if (d == null) {
            return;
        }
        File file = new File(VarStatiche.getCartella_Normativa(), d.percorso());
        if (!file.exists()) {
            Messaggi.WarningMessage("File non trovato",
                    "Il file non è (più) presente sul disco: prova ad aggiornare l'archivio da GitHub.", owner);
            return;
        }
        File daAprire = file;
        if (d.percorso().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            File leggibile = PreparaXmlLeggibile(d, file);
            if (leggibile != null) {
                daAprire = leggibile;
            }
        }
        try {
            Desktop.getDesktop().open(daAprire);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Impossibile aprire il file", ex.getMessage(), owner);
        }
    }

    /**
     * Converte l'XML in una pagina HTML autosufficiente in {@code Temporanei/} (nessun link a
     * risorse esterne: l'installazione locale non ha lo {@code stile.css} del sito), con
     * l'intestazione del documento (titolo/identificativo/autorità) e il corpo reso leggibile da
     * {@link #EstraiXmlLeggibile}. {@code Desktop.getDesktop().open()} la apre con il browser
     * predefinito del sistema, che è l'associazione normale per {@code .html}. Copia di comodo:
     * {@code Temporanei/} è svuotata automaticamente.
     *
     * @return il file HTML, {@code null} se la conversione fallisce (in quel caso si apre l'XML originale)
     */
    private static File PreparaXmlLeggibile(DocumentoNormativa d, File xml) {
        try {
            String corpo = EstraiXmlLeggibile(xml);
            File cartella = new File(VarStatiche.getCartella_Temporanei());
            if (!cartella.exists()) {
                cartella.mkdirs();
            }
            String nomeBase = xml.getName().replaceFirst("(?i)\\.xml$", "");
            File out = new File(cartella, nomeBase + ".html");
            Files.writeString(out.toPath(), ComponiPaginaLeggibile(d, corpo), StandardCharsets.UTF_8);
            return out;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /**
     * Pagina HTML autosufficiente per {@link #PreparaXmlLeggibile}: stile inline (nessun file
     * esterno), sul modello di {@code Sito/strumenti/genera_indice_normativa.py:scrivi_pagina_leggibile}
     * ma con colori fissi invece delle variabili del tema del sito, che qui non esiste.
     */
    private static String ComponiPaginaLeggibile(DocumentoNormativa d, String corpoHtml) {
        return """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                <meta charset="UTF-8">
                <title>%s — Giacenze Crypto</title>
                <style>
                  body { margin:0; font-family:"Segoe UI","Noto Sans",Arial,sans-serif; background:#ffffff; color:#1a1a1a; }
                  .pagina-leggibile { max-width:760px; margin:0 auto; padding:40px 24px 80px; }
                  .pagina-leggibile h1 { font-size:1.5rem; margin:0 0 10px; }
                  .pagina-leggibile .meta-doc { color:#5a5a52; font-size:0.9rem; margin:0 0 6px; }
                  .pagina-leggibile .avviso-conversione { background:#f7f7f2; border:1px solid #ddd; border-left:4px solid #8a8a1f; border-radius:6px; padding:14px 18px; font-size:0.85rem; color:#5a5a52; margin:20px 0 28px; }
                  .corpo-testo { font-size:0.98rem; line-height:1.7; }
                  .corpo-testo p { margin:0 0 14px; }
                  .intestazione-atto { margin-bottom:32px; padding-bottom:20px; border-bottom:1px solid #ddd; }
                  .intestazione-atto .tipo-atto { margin:0 0 6px; font-weight:700; text-transform:uppercase; letter-spacing:0.03em; font-size:0.85rem; color:#6b6b16; }
                  .intestazione-atto .titolo-atto { margin:0; font-size:1.1rem; color:#5a5a52; font-style:italic; }
                  .formula-preambolo { text-align:center; font-style:italic; color:#5a5a52; margin:0 0 10px; }
                  .titolo-struttura { font-size:1.25rem; color:#6b6b16; margin:36px 0 16px; padding-bottom:8px; border-bottom:2px solid #ddd; }
                  .articolo { margin:28px 0; padding-top:20px; border-top:1px solid #ddd; }
                  .articolo:first-child { border-top:none; padding-top:0; }
                  .titolo-articolo { font-size:1.05rem; margin:0 0 14px; font-weight:700; }
                  .comma { display:flex; gap:10px; margin:0 0 12px; align-items:baseline; }
                  .comma .numero-comma { flex:0 0 auto; font-weight:700; color:#6b6b16; font-size:0.88rem; min-width:1.6em; }
                  .comma > *:not(.numero-comma) { flex:1 1 auto; min-width:0; }
                  .comma p { margin:0 0 8px; }
                  .comma p:last-child { margin-bottom:0; }
                  .elenco-normativo { margin:8px 0 12px 1.8em; }
                  .punto-elenco { display:flex; gap:10px; margin:0 0 8px; align-items:baseline; }
                  .punto-elenco .numero-punto { flex:0 0 auto; font-weight:700; color:#5a5a52; min-width:1.6em; }
                  .punto-elenco p { margin:0; }
                  .nota-atto { font-size:0.85rem; color:#5a5a52; background:#f7f7f2; border-left:3px solid #ddd; padding:10px 14px; margin:0 0 20px; }
                </style>
                </head>
                <body>
                <div class="pagina-leggibile">
                  <h1>%s</h1>
                  <p class="meta-doc">%s</p>
                  <p class="meta-doc">%s</p>
                  <div class="avviso-conversione">Versione resa leggibile dal documento XML originale (struttura di
                  articoli e commi ricostruita dai tag) — a scopo di consultazione, non sostituisce il testo ufficiale.</div>
                  <div class="corpo-testo">%s</div>
                </div>
                </body>
                </html>
                """.formatted(EscapeHtml(d.titolo()), EscapeHtml(d.titolo()),
                EscapeHtml(d.autorita() == null ? "" : d.autorita()),
                EscapeHtml(d.identificativo() == null ? "" : d.identificativo()),
                corpoHtml);
    }

    /** @return {@code true} se {@link DocumentoNormativa#url()} è un indirizzo apribile in browser */
    public static boolean UrlApribile(DocumentoNormativa d) {
        return d != null && d.url() != null
                && (d.url().startsWith("http://") || d.url().startsWith("https://"));
    }

    /** Apre la fonte ufficiale nel browser predefinito, per confrontare il testo con l'originale. */
    public static void ApriFonteUfficiale(DocumentoNormativa d, Window owner) {
        if (!UrlApribile(d)) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(d.url()));
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Impossibile aprire l'indirizzo", ex.getMessage(), owner);
        }
    }

    // ------------------------------------------------------------------ testo per la ricerca

    /**
     * Restituisce il testo del documento, dalla cache {@code NORMATIVA_TESTO} se lo sha coincide
     * con quello attuale del file (vedi {@code fonti.csv}), altrimenti estraendolo ed
     * aggiornando la cache. Fa I/O (file e database): da chiamare fuori dall'EDT.
     *
     * @return il testo estratto, stringa vuota se il file manca o non è di un formato riconosciuto
     */
    public static String TestoDocumento(DocumentoNormativa d) {
        if (d == null || d.sha256() == null || d.sha256().isBlank()) {
            return "";
        }
        String shaCache = DatabaseH2.NormativaTesto_LeggiSha(d.percorso());
        if (d.sha256().equalsIgnoreCase(shaCache)) {
            String testo = DatabaseH2.NormativaTesto_Leggi(d.percorso());
            if (testo != null) {
                return testo;
            }
        }
        String testo = Estrai(d);
        DatabaseH2.NormativaTesto_Scrivi(d.percorso(), d.sha256(), testo);
        return testo;
    }

    /**
     * Estrae e cachea il testo di ogni documento del catalogo non ancora in cache (o con sha
     * cambiato), aggiornando {@code cache} man mano e richiamando {@code dopoOgniDocumento} dopo
     * ciascuno (tipicamente per aggiornare una label di avanzamento e riapplicare il filtro sull'EDT
     * - questo metodo gira su un thread in background, non chiama mai Swing direttamente).
     *
     * @param catalogo documenti da indicizzare
     * @param cache mappa percorso→testo da popolare (una {@code ConcurrentHashMap}, letta dall'EDT
     *        mentre questo metodo scrive da un altro thread)
     * @param dopoOgniDocumento eseguito dopo ogni documento elaborato, può essere {@code null}
     */
    public static void PrecaricaTesti(List<DocumentoNormativa> catalogo, Map<String, String> cache,
            Runnable dopoOgniDocumento) {
        for (DocumentoNormativa d : catalogo) {
            cache.put(d.percorso(), TestoDocumento(d));
            if (dopoOgniDocumento != null) {
                dopoOgniDocumento.run();
            }
        }
    }

    private static String Estrai(DocumentoNormativa d) {
        File file = new File(VarStatiche.getCartella_Normativa(), d.percorso());
        if (!file.exists()) {
            return "";
        }
        String nome = d.percorso().toLowerCase(Locale.ROOT);
        try {
            if (nome.endsWith(".pdf")) {
                return EstraiPdf(file);
            }
            if (nome.endsWith(".xml")) {
                return EstraiXml(file);
            }
            if (nome.endsWith(".md") || nome.endsWith(".txt")) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return "";
    }

    private static String EstraiPdf(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(file.getAbsolutePath())) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                sb.append(extractor.getTextFromPage(pagina)).append('\n');
            }
        }
        return sb.toString();
    }

    private static String EstraiXml(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //Niente DTD esterni: i file sono di provenienza controllata (il repository del progetto),
        //ma disabilitarli non costa nulla ed è la postura corretta per un parser XML.
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(file);
        StringBuilder sb = new StringBuilder();
        RaccogliTesto(doc, sb);
        return sb.toString();
    }

    private static void RaccogliTesto(Node nodo, StringBuilder sb) {
        if (nodo.getNodeType() == Node.TEXT_NODE) {
            String v = nodo.getNodeValue();
            if (v != null && !v.isBlank()) {
                sb.append(v.trim()).append(' ');
            }
        }
        NodeList figli = nodo.getChildNodes();
        for (int i = 0; i < figli.getLength(); i++) {
            RaccogliTesto(figli.item(i), sb);
        }
    }

    /**
     * Come {@link #EstraiXml}, ma per la lettura umana invece che per la ricerca: usa la struttura
     * reale del documento (docType/docTitle nell'intestazione, capitoli e articoli come titoli, i
     * commi come paragrafi numerati — {@link #RenderNodo}), sul modello di
     * {@code Sito/strumenti/genera_indice_normativa.py:_render_nodo}, la cui logica questo metodo
     * riporta 1:1 sul DOM di Java. Se il render strutturato non produce nulla (variante di schema
     * non prevista) ripiega su {@link #RaccogliTestoLeggibile}, l'euristica "un elemento, una riga"
     * che non richiede capire lo schema — verificata leggibile su un vero atto.
     *
     * @return un frammento HTML (non un documento completo): lo compone {@link #ComponiPaginaLeggibile}
     */
    static String EstraiXmlLeggibile(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(file);

        String strutturato = RenderNodo(doc.getDocumentElement());
        if (strutturato != null && !strutturato.isBlank()) {
            return strutturato;
        }

        StringBuilder sb = new StringBuilder();
        RaccogliTestoLeggibile(doc, sb);
        String testo = sb.toString()
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
        StringBuilder fallback = new StringBuilder();
        for (String riga : testo.split("\n")) {
            String r = riga.strip();
            if (!r.isEmpty()) {
                fallback.append("<p>").append(EscapeHtml(r)).append("</p>");
            }
        }
        return fallback.toString();
    }

    private static void RaccogliTestoLeggibile(Node nodo, StringBuilder sb) {
        if (nodo.getNodeType() == Node.TEXT_NODE) {
            String v = nodo.getNodeValue();
            if (v != null && !v.isBlank()) {
                sb.append(v.trim()).append(' ');
            }
            return;
        }
        NodeList figli = nodo.getChildNodes();
        for (int i = 0; i < figli.getLength(); i++) {
            RaccogliTestoLeggibile(figli.item(i), sb);
        }
        if (nodo.getNodeType() == Node.ELEMENT_NODE) {
            sb.append('\n');
        }
    }

    // ------------------------------------------------------------------ XML -> HTML strutturato

    private static final Set<String> TAG_CONTENITORE = Set.of("akomaNtoso", "act", "bill", "doc");
    private static final Set<String> TAG_STRUTTURA = Set.of("part", "book", "title", "chapter", "section", "subsection");
    private static final Set<String> ESCLUDI_NUM = Set.of("num");
    private static final Set<String> ESCLUDI_NUM_HEADING = Set.of("num", "heading");

    /**
     * Traduce ricorsivamente un elemento Akoma Ntoso in HTML, un tag alla volta — porto Java di
     * {@code _render_nodo} in {@code genera_indice_normativa.py}, tenuto in sincronia con quello
     * per struttura e nomi di classe CSS. I file del repository non usano un prefisso di
     * namespace sugli elementi (verificato: {@code <article>}, non {@code <akn:article>}), quindi
     * {@link Element#getTagName()} è già il nome locale e non serve un parser namespace-aware.
     *
     * <p>{@code meta} è escluso deliberatamente: contiene lo storico delle modifiche (fino a
     * centinaia di KB su un testo come il TUIR) e non porterebbe nulla di leggibile prima del
     * titolo vero.
     */
    static String RenderNodo(Element el) {
        String tag = el.getTagName();

        if ("meta".equals(tag)) {
            return "";
        }
        if (TAG_CONTENITORE.contains(tag)) {
            return RenderFigli(el, Set.of());
        }
        if ("preface".equals(tag)) {
            String numero = TestoDiretto(FiglioDiretto(el, "docNumber"));
            String etichetta = Stream.of(
                            TestoDiretto(FiglioDiretto(el, "docType")),
                            TestoDiretto(FiglioDiretto(el, "docDate")),
                            numero.isEmpty() ? "" : "n. " + numero)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(" "));
            String titolo = TestoCompleto(FiglioDiretto(el, "docTitle"));
            StringBuilder out = new StringBuilder("<div class=\"intestazione-atto\">");
            if (!etichetta.isEmpty()) {
                out.append("<p class=\"tipo-atto\">").append(EscapeHtml(etichetta)).append("</p>");
            }
            if (!titolo.isEmpty()) {
                out.append("<p class=\"titolo-atto\">").append(EscapeHtml(titolo)).append("</p>");
            }
            out.append("</div>");
            NodeList note = el.getElementsByTagName("authorialNote");
            for (int i = 0; i < note.getLength(); i++) {
                out.append(RenderNodo((Element) note.item(i)));
            }
            return out.toString();
        }
        if ("preamble".equals(tag)) {
            StringBuilder out = new StringBuilder();
            NodeList formule = el.getElementsByTagName("formula");
            for (int i = 0; i < formule.getLength(); i++) {
                String t = TestoCompleto((Element) formule.item(i));
                if (!t.isEmpty()) {
                    out.append("<p class=\"formula-preambolo\">").append(EscapeHtml(t)).append("</p>");
                }
            }
            return out.toString();
        }
        if ("body".equals(tag) || "intro".equals(tag) || "content".equals(tag)) {
            return RenderFigli(el, Set.of());
        }
        if (TAG_STRUTTURA.contains(tag)) {
            String num = TestoDiretto(FiglioDiretto(el, "num"));
            String heading = TestoCompleto(FiglioDiretto(el, "heading"));
            String titolo = Stream.of(num, heading).filter(s -> !s.isEmpty()).collect(Collectors.joining(" — "));
            String intestazione = titolo.isEmpty() ? "" : "<h2 class=\"titolo-struttura\">" + EscapeHtml(titolo) + "</h2>";
            return intestazione + RenderFigli(el, ESCLUDI_NUM_HEADING);
        }
        if ("article".equals(tag)) {
            String num = TestoDiretto(FiglioDiretto(el, "num"));
            String heading = TestoCompleto(FiglioDiretto(el, "heading"));
            String titolo = Stream.of(num, heading).filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
            StringBuilder out = new StringBuilder("<div class=\"articolo\">");
            if (!titolo.isEmpty()) {
                out.append("<h3 class=\"titolo-articolo\">").append(EscapeHtml(titolo)).append("</h3>");
            }
            out.append(RenderFigli(el, ESCLUDI_NUM_HEADING));
            out.append("</div>");
            return out.toString();
        }
        if ("paragraph".equals(tag) || "alinea".equals(tag)) {
            String num = TestoDiretto(FiglioDiretto(el, "num"));
            String corpo = RenderFigli(el, ESCLUDI_NUM);
            if (corpo.isBlank()) {
                return "";
            }
            String prefisso = num.isEmpty() ? "" : "<span class=\"numero-comma\">" + EscapeHtml(num) + "</span>";
            return "<div class=\"comma\">" + prefisso + corpo + "</div>";
        }
        if ("point".equals(tag)) {
            String num = TestoDiretto(FiglioDiretto(el, "num"));
            String corpo = RenderFigli(el, ESCLUDI_NUM);
            if (corpo.isBlank()) {
                return "";
            }
            String prefisso = num.isEmpty() ? "" : "<span class=\"numero-punto\">" + EscapeHtml(num) + "</span>";
            return "<div class=\"punto-elenco\">" + prefisso + corpo + "</div>";
        }
        if ("list".equals(tag)) {
            return "<div class=\"elenco-normativo\">" + RenderFigli(el, Set.of()) + "</div>";
        }
        if ("authorialNote".equals(tag)) {
            String t = TestoCompleto(el);
            return t.isEmpty() ? "" : "<p class=\"nota-atto\">" + EscapeHtml(t) + "</p>";
        }
        if ("p".equals(tag)) {
            String t = TestoCompleto(el);
            return t.isEmpty() ? "" : "<p>" + EscapeHtml(t) + "</p>";
        }
        if ("num".equals(tag) || "heading".equals(tag)) {
            return ""; // gestiti dal genitore; se raggiunti qui non c'è niente da fare
        }
        // Tag non censiti sopra (varianti non ancora viste): si scende comunque nei figli, così il
        // testo non sparisce mai — solo la formattazione speciale manca finché non si aggiunge un caso.
        return RenderFigli(el, Set.of());
    }

    private static String RenderFigli(Element el, Set<String> escludi) {
        StringBuilder sb = new StringBuilder();
        NodeList figli = el.getChildNodes();
        for (int i = 0; i < figli.getLength(); i++) {
            Node n = figli.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element f = (Element) n;
                if (!escludi.contains(f.getTagName())) {
                    sb.append(RenderNodo(f));
                }
            }
        }
        return sb.toString();
    }

    /** Primo figlio diretto con questo nome, altrimenti il primo discendente — come {@code el.find()}/{@code .//} in Python. */
    private static Element FiglioDiretto(Element el, String nome) {
        if (el == null) {
            return null;
        }
        NodeList figli = el.getChildNodes();
        for (int i = 0; i < figli.getLength(); i++) {
            Node n = figli.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && nome.equals(((Element) n).getTagName())) {
                return (Element) n;
            }
        }
        NodeList discendenti = el.getElementsByTagName(nome);
        return discendenti.getLength() > 0 ? (Element) discendenti.item(0) : null;
    }

    /** Solo il testo dei nodi di testo diretti (non dei discendenti), spazi collassati. */
    private static String TestoDiretto(Element el) {
        if (el == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        NodeList figli = el.getChildNodes();
        for (int i = 0; i < figli.getLength(); i++) {
            Node n = figli.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                String v = n.getNodeValue();
                if (v != null && !v.isBlank()) {
                    sb.append(v.trim()).append(' ');
                }
            }
        }
        return sb.toString().strip();
    }

    /** Tutto il testo del sottoalbero (equivalente a {@code el.itertext()}), spazi collassati. */
    private static String TestoCompleto(Element el) {
        if (el == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        RaccogliTesto(el, sb);
        return sb.toString().strip();
    }

    /** Neutralizza i caratteri che romperebbero il markup HTML generato. */
    private static String EscapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
