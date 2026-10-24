package com.giacenzecrypto.giacenze_crypto;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le novità della versione, mostrate da {@link GUI_NovitaVersione} al primo avvio dopo un aggiornamento e dalla finestra
 * Informazioni.
 *
 * <p><b>La sorgente è una sola</b>: {@code docs/documentazione/changelog.md}, la pagina "Novità delle versioni"
 * pubblicata su GitHub Pages, che il pom copia anche nel jar ({@value #RISORSA}). Così il dialogo funziona senza rete e
 * le note si scrivono una volta. Il Markdown usato in quella pagina è poco (titoli, grassetti, corsivi, codice, elenchi,
 * collegamenti, citazioni): {@link #Html} converte solo quello, nell'HTML che {@code JEditorPane} sa disegnare.</p>
 *
 * <p><b>Due file.</b> {@code changelog.md} raccoglie le novità di ogni versione rilasciata, e nella sezione più in alto
 * quelle della versione in preparazione, cumulative. {@code changelog-beta.md} ({@value #RISORSA_BETA}, solo nel jar, non
 * pubblicato come pagina) ha una sezione per ogni beta con quello che è cambiato rispetto alla beta precedente: le stesse
 * note fanno da descrizione della prerelease su GitHub ({@code release.yml}).</p>
 *
 * <p><b>Quali sezioni.</b> {@link #SezioniDaMostrare}: una versione rilasciata ({@code 1.0.65}) mostra le sezioni del
 * changelog, una beta ({@code 1.0.65.12}, quattro numeri) quelle del file delle beta. All'avvio dopo un aggiornamento
 * tutte quelle uscite dopo l'ultima versione vista, così chi salta una beta non perde le sue novità. Una beta senza note
 * proprie (una build locale) mostra la sezione più in alto del changelog, come prima che esistesse il file delle beta.</p>
 *
 * <p><b>Quando.</b> {@link #DaMostrare}: la prima volta che parte una versione diversa dall'ultima vista, salvo su
 * un'installazione nuova (archivio vuoto), dove le novità rispetto a una versione mai usata non dicono nulla.</p>
 */
public final class NovitaVersione {

    private NovitaVersione() {
    }

    /** Posizione di {@code changelog.md} nel jar (vedi il {@code <resource>} del pom). */
    static final String RISORSA = "/Novita/changelog.md";

    /** Le note delle singole beta, in {@code src/main/resources/Novita/}: nel jar ma non sul sito. */
    static final String RISORSA_BETA = "/Novita/changelog-beta.md";

    /** Al massimo tante sezioni in una volta, le più recenti: chi torna dopo molte versioni ha la pagina online. */
    static final int SEZIONI_MASSIME = 10;

    /** Opzione in {@code personale.mv.db}: l'ultima versione per cui le novità sono state mostrate (o saltate). */
    public static final String OPZIONE_ULTIMA_VERSIONE = "Novita_UltimaVersioneVista";

    private static final Pattern TITOLO_VERSIONE = Pattern.compile("^##\\s+Versione\\s+([0-9][0-9.]*)");

    /** Una sezione della pagina: il numero di versione e il suo Markdown, senza il titolo. */
    record Sezione(String Versione, String Markdown) {

    }

    /** @return il Markdown della pagina incluso nel jar, o {@code null} se manca */
    static String Leggi() {
        return Leggi(RISORSA);
    }

    /** @return il Markdown delle note delle beta incluso nel jar, o {@code null} se manca */
    static String LeggiBeta() {
        return Leggi(RISORSA_BETA);
    }

    private static String Leggi(String Risorsa) {
        try (InputStream in = NovitaVersione.class.getResourceAsStream(Risorsa)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /** Le sezioni "## Versione x.y.z" della pagina, nell'ordine in cui compaiono (dalla più recente). */
    static List<Sezione> Sezioni(String Markdown) {
        List<Sezione> Ris = new ArrayList<>();
        if (Markdown == null) return Ris;
        String Versione = null;
        StringBuilder Corpo = new StringBuilder();
        for (String Riga : Markdown.replace("\r", "").split("\n", -1)) {
            Matcher m = TITOLO_VERSIONE.matcher(Riga);
            boolean TitoloVersione = m.find();
            //Ogni titolo di secondo livello chiude la sezione precedente; solo quelli di versione ne aprono una
            if (TitoloVersione || Riga.startsWith("## ")) {
                if (Versione != null) Ris.add(new Sezione(Versione, Corpo.toString().trim()));
                Versione = TitoloVersione ? m.group(1) : null;
                Corpo.setLength(0);
            } else if (Versione != null) {
                Corpo.append(Riga).append('\n');
            }
        }
        if (Versione != null) Ris.add(new Sezione(Versione, Corpo.toString().trim()));
        return Ris;
    }

    /** Una versione letta davvero dal jar: non vuota, non il segnaposto di Maven non sostituito, non "sconosciuta". */
    static boolean isVersioneValida(String Versione) {
        return !Funzioni.noData(Versione) && !Versione.startsWith("${") && !Versione.equalsIgnoreCase("sconosciuta");
    }

    /** Una versione di prova ha quattro numeri (1.0.64.08). */
    static boolean isVersioneDiProva(String Versione) {
        return Versione != null && Versione.split("\\.").length >= 4;
    }

    /**
     * La sezione da mostrare per la versione del programma: quella con lo stesso numero, oppure, per una versione di
     * prova, la più in alto della pagina.
     * @return la sezione, o {@code null} se non ce n'è una
     */
    static Sezione SezionePer(String VersioneProgramma, List<Sezione> Sezioni) {
        if (VersioneProgramma == null || Sezioni.isEmpty()) return null;
        for (Sezione s : Sezioni) if (s.Versione().equals(VersioneProgramma)) return s;
        return isVersioneDiProva(VersioneProgramma) ? Sezioni.get(0) : null;
    }

    /**
     * Le sezioni da mostrare per la versione del programma, dalla più recente: quella della versione e quelle uscite dopo
     * {@code UltimaVista} e non dopo la versione del programma (al massimo {@value #SEZIONI_MASSIME}). Una beta le
     * prende da {@code Beta}, una versione rilasciata da {@code Principali}. Una beta senza note proprie ripiega sulla
     * sezione più in alto di {@code Principali}, le note in preparazione.
     * @param UltimaVista l'ultima versione per cui le novità sono state mostrate; vuota, o uguale alla versione del
     *                    programma, per la sola sezione della versione
     */
    static List<Sezione> SezioniDaMostrare(String VersioneProgramma, String UltimaVista, List<Sezione> Principali,
            List<Sezione> Beta) {
        List<Sezione> Ris = new ArrayList<>();
        if (!isVersioneValida(VersioneProgramma)) return Ris;
        boolean Prova = isVersioneDiProva(VersioneProgramma);
        boolean Intervallo = isVersioneValida(UltimaVista) && Funzioni.ConfrontaVersioni(UltimaVista, VersioneProgramma) < 0;
        for (Sezione s : Prova ? Beta : Principali) {
            int RispettoProgramma = Funzioni.ConfrontaVersioni(s.Versione(), VersioneProgramma);
            if (RispettoProgramma == 0 || (RispettoProgramma < 0 && Intervallo
                    && Funzioni.ConfrontaVersioni(s.Versione(), UltimaVista) > 0)) Ris.add(s);
        }
        Ris.sort((a, b) -> Funzioni.ConfrontaVersioni(b.Versione(), a.Versione()));
        if (Ris.size() > SEZIONI_MASSIME) Ris = new ArrayList<>(Ris.subList(0, SEZIONI_MASSIME));
        if (Ris.isEmpty() && Prova && !Principali.isEmpty()) Ris.add(Principali.get(0));
        return Ris;
    }

    /** Il titolo del dialogo per le sezioni di {@link #SezioniDaMostrare}. */
    static String Titolo(String VersioneProgramma, List<Sezione> Sezioni) {
        if (Sezioni == null || Sezioni.isEmpty()) return "Novità della versione";
        if (Sezioni.size() == 1) {
            Sezione s = Sezioni.get(0);
            if (s.Versione().equals(VersioneProgramma) && isVersioneDiProva(VersioneProgramma)) {
                return "Novità della versione di prova " + VersioneProgramma;
            }
            return Titolo(VersioneProgramma, s);
        }
        String Piu = Sezioni.get(0).Versione(), Meno = Sezioni.get(Sezioni.size() - 1).Versione();
        return (isVersioneDiProva(VersioneProgramma) ? "Novità delle versioni di prova dalla " : "Novità delle versioni dalla ")
                + Meno + " alla " + Piu;
    }

    /** L'HTML delle sezioni: una sola com'è, più sezioni ciascuna col suo titolo "Versione x". */
    static String Html(List<Sezione> Sezioni) {
        if (Sezioni.size() == 1) return Html(Sezioni.get(0).Markdown());
        StringBuilder md = new StringBuilder();
        for (Sezione s : Sezioni) md.append("## Versione ").append(s.Versione()).append("\n\n").append(s.Markdown()).append("\n\n");
        return Html(md.toString());
    }

    /** Il titolo del dialogo, che per una versione di prova dice anche il numero della prova. */
    static String Titolo(String VersioneProgramma, Sezione s) {
        if (s == null) return "Novità della versione";
        String T = "Novità della versione " + s.Versione();
        if (!s.Versione().equals(VersioneProgramma)) T += " (versione di prova " + VersioneProgramma + ")";
        return T;
    }

    /**
     * Se le novità vanno mostrate all'avvio.
     * @param VersioneProgramma la versione che sta partendo
     * @param UltimaVista l'ultima versione per cui sono state mostrate, vuota se mai
     * @param ArchivioVuoto {@code true} su un'installazione nuova, senza movimenti
     */
    static boolean DaMostrare(String VersioneProgramma, String UltimaVista, boolean ArchivioVuoto) {
        if (!isVersioneValida(VersioneProgramma) || VersioneProgramma.equals(UltimaVista)) return false;
        return !(Funzioni.noData(UltimaVista) && ArchivioVuoto);
    }

    /**
     * Converte il Markdown di una sezione in HTML per {@code JEditorPane}. Solo il sottoinsieme usato dalla pagina:
     * titoli, paragrafi, elenchi puntati (le righe che seguono una voce senza "- " la continuano), citazioni, grassetto,
     * corsivo, codice e collegamenti. I collegamenti relativi diventano assoluti sul sito della documentazione.
     */
    static String Html(String Markdown) {
        StringBuilder h = new StringBuilder("<html><body>");
        boolean InElenco = false, InCitazione = false, InParagrafo = false;
        for (String Riga : (Markdown == null ? "" : Markdown).replace("\r", "").split("\n", -1)) {
            String r = Riga.strip();
            if (r.isEmpty()) {
                if (InElenco) h.append("</li></ul>");
                if (InCitazione) h.append("</blockquote>");
                if (InParagrafo) h.append("</p>");
                InElenco = InCitazione = InParagrafo = false;
                continue;
            }
            if (r.startsWith("#")) {
                if (InElenco) h.append("</li></ul>");
                if (InCitazione) h.append("</blockquote>");
                if (InParagrafo) h.append("</p>");
                InElenco = InCitazione = InParagrafo = false;
                int Livello = Math.min(4, Math.max(3, r.indexOf(' ') + 1));
                h.append("<h").append(Livello).append('>').append(InLinea(r.replaceFirst("^#+\\s*", "")))
                        .append("</h").append(Livello).append('>');
            } else if (r.startsWith("- ") || r.startsWith("* ")) {
                if (InParagrafo) h.append("</p>");
                InParagrafo = false;
                h.append(InElenco ? "</li><li>" : "<ul><li>").append(InLinea(r.substring(2)));
                InElenco = true;
            } else if (r.startsWith(">")) {
                h.append(InCitazione ? " " : "<blockquote>").append(InLinea(r.substring(1).strip()));
                InCitazione = true;
            } else if (InElenco) {
                h.append("<br>").append(InLinea(r));
            } else {
                h.append(InParagrafo ? " " : "<p>").append(InLinea(r));
                InParagrafo = true;
            }
        }
        if (InElenco) h.append("</li></ul>");
        if (InCitazione) h.append("</blockquote>");
        if (InParagrafo) h.append("</p>");
        return h.append("</body></html>").toString();
    }

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");

    /** Formattazione dentro una riga: ancore {@code {#..}} tolte, caratteri HTML protetti, poi codice, grassetto, corsivo, collegamenti. */
    static String InLinea(String Testo) {
        String t = Testo.replaceAll("\\s*\\{#[^}]*\\}", "");
        t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        t = t.replaceAll("`([^`]+)`", "<code>$1</code>");
        t = t.replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
        t = t.replaceAll("(?<![\\w*])\\*([^*\\s][^*]*?)\\*(?![\\w*])", "<i>$1</i>");
        Matcher m = LINK.matcher(t);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "<a href='" + UrlAssoluto(m.group(2)) + "'>" + m.group(1) + "</a>"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Un collegamento della pagina, reso assoluto: le pagine sorelle e le ancore stanno sul sito della documentazione. */
    static String UrlAssoluto(String Url) {
        if (Url.matches("(?i)^(https?|mailto):.*")) return Url;
        if (Url.startsWith("#")) return DocumentiAiuto.Url(DocumentiAiuto.NOVITA_VERSIONI) + Url;
        return DocumentiAiuto.Url(Url.replaceFirst("^\\./", ""));
    }
}
