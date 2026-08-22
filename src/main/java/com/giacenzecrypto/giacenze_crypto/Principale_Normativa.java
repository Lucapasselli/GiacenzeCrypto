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
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
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
     * Converte l'XML in un file di testo semplice in {@code Temporanei/}, con qualche riga di
     * intestazione (titolo/identificativo/autorità) e il testo del documento formattato un
     * paragrafo per riga - non un semplice svuotamento dei tag, che produrrebbe un unico blocco
     * senza interruzioni. Copia di comodo: {@code Temporanei/} è svuotata automaticamente.
     *
     * @return il file di testo, {@code null} se la conversione fallisce (in quel caso si apre l'XML originale)
     */
    private static File PreparaXmlLeggibile(DocumentoNormativa d, File xml) {
        try {
            String testo = EstraiXmlLeggibile(xml);
            File cartella = new File(VarStatiche.getCartella_Temporanei());
            if (!cartella.exists()) {
                cartella.mkdirs();
            }
            String nomeBase = xml.getName().replaceFirst("(?i)\\.xml$", "");
            File out = new File(cartella, nomeBase + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append(d.titolo()).append('\n');
            if (!d.identificativo().isBlank()) {
                sb.append(d.identificativo()).append('\n');
            }
            if (!d.autorita().isBlank()) {
                sb.append(d.autorita()).append('\n');
            }
            sb.append('\n').append(testo);
            Files.writeString(out.toPath(), sb.toString(), StandardCharsets.UTF_8);
            return out;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
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
     * Come {@link #EstraiXml}, ma per la lettura umana invece che per la ricerca: un a capo dopo
     * ogni elemento invece di un unico paragrafo continuo, così i commi/lettere/punti dell'Akoma
     * Ntoso restano su righe separate senza dover conoscere i nomi dei tag (l'euristica è "un
     * elemento, una riga", non specifica di Akoma Ntoso). Verificata leggibile su un vero atto:
     * i numeri di comma e le lettere (a), b)...) escono ciascuno sulla propria riga.
     */
    private static String EstraiXmlLeggibile(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(file);
        StringBuilder sb = new StringBuilder();
        RaccogliTestoLeggibile(doc, sb);
        String testo = sb.toString()
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n");
        return testo.strip();
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
}
