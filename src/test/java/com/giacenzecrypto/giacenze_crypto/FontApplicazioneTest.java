package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test del font incluso nel jar ({@link FontApplicazione}) e dei due modi in cui l'uniformità che quel
 * font garantisce può essere rotta di nuovo senza che nessuno se ne accorga.
 * <p>
 * <b>Perché non basta provare la registrazione.</b> Passare da un font <i>logico</i> ({@code Dialog},
 * {@code SansSerif}) a un font <i>fisico</i> cambia due comportamenti opposti, ed entrambi sono
 * silenziosi:
 * <ul>
 *   <li>una famiglia inesistente non fa scattare nessuna eccezione, si ripiega su {@code Dialog} e
 *       l'aspetto torna a dipendere dal sistema — è il difetto che il font incluso ha chiuso, e
 *       {@link #nessunComponenteChiedeUnFontDiSistema()} impedisce di reintrodurlo;</li>
 *   <li>un font fisico <b>non cerca altrove</b> i caratteri che non ha: dove il font logico pescava il
 *       glifo da un altro font del sistema, adesso compare un rettangolo vuoto. Noto Sans non contiene
 *       le frecce, i segni di spunta, i simboli matematici né le emoji, quindi
 *       {@link #iTestiDellaGuiUsanoSoloCaratteriPresentiNelFont()} controlla i testi della GUI carattere
 *       per carattere.</li>
 * </ul>
 * Il secondo controllo guarda <b>tutte</b> le stringhe letterali: i file {@code .form} per intero
 * (quello che contengono è per definizione interfaccia) e, nei {@code .java}, ogni letterale che non
 * stia in un commento o in un blocco di testo. Restringere il campo alle righe con {@code setText}
 * sembra più mirato ma non funziona: nessuno dei tre punti che si sono davvero rotti passando al font
 * incluso era su una riga del genere. Quello che finisce nel PDF o sulla console si dichiara riga per
 * riga con {@code //NON-A-VIDEO}.
 */
class FontApplicazioneTest {

    /** I sorgenti da ispezionare. I test che li leggono si autoescludono se la cartella non c'è. */
    private static final Path SORGENTI = Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto");

    /**
     * Le famiglie che un componente può chiedere per nome. Oltre a quella inclusa nel jar restano i
     * <b>font logici</b>, che non sono un font ma una categoria che la JVM compone da più font di
     * sistema: sono l'unico modo di scrivere caratteri che il font incluso non ha, e in un punto
     * (la finestra dei log) servono davvero. Restano ammessi per quello, non come alternativa
     * generica — le loro metriche le decide il sistema, che è il difetto da cui si viene.
     */
    private static final Set<String> FAMIGLIE_AMMESSE = Set.of(
            FontApplicazione.FAMIGLIA, "Monospaced", "Dialog", "DialogInput", "SansSerif", "Serif");

    @BeforeAll
    static void registra() {
        FontApplicazione.Registra();
    }

    @Test
    void iQuattroTagliSonoNelJarESiRegistrano() {
        for (String risorsa : new String[]{"/Fonts/NotoSans-Regular.ttf", "/Fonts/NotoSans-Bold.ttf",
            "/Fonts/NotoSans-Italic.ttf", "/Fonts/NotoSans-BoldItalic.ttf"}) {
            try (InputStream is = FontApplicazione.class.getResourceAsStream(risorsa)) {
                assertNotNull(is, "manca dalle risorse: " + risorsa);
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
        }

        //Il valore ritornato da registerFont NON va usato come esito: è false anche quando la famiglia
        //è già installata nel sistema, che sotto Linux è il caso normale. Quello che conta è che la
        //ricerca per nome non sia ricaduta in silenzio su Dialog.
        assertTrue(FontApplicazione.Disponibile(),
                "la famiglia " + FontApplicazione.FAMIGLIA + " non è raggiungibile per nome");
    }

    @Test
    void iQuattroTagliSonoDistinti() {
        //Se mancasse il taglio corsivo o grassetto la JVM non protesterebbe: sintetizzerebbe (o
        //ignorerebbe) lo stile, e il corsivo dell'HTML dentro le label uscirebbe diverso dal previsto.
        Set<String> nomi = new LinkedHashSet<>();
        for (int stile : new int[]{Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}) {
            nomi.add(FontApplicazione.Font(stile, 12).getFontName(Locale.ROOT));
        }
        assertEquals(4, nomi.size(), "i quattro stili non danno quattro tagli distinti: " + nomi);
    }

    @Test
    void nessunComponenteChiedeUnFontDiSistema() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(SORGENTI), "sorgenti non raggiungibili da qui");

        //new java.awt.Font("Segoe UI", ...) nei sorgenti, <Font name="Segoe UI" .../> nei .form.
        Pattern inJava = Pattern.compile("new (?:java\\.awt\\.)?Font\\(\"([^\"]+)\"");
        Pattern inForm = Pattern.compile("<Font name=\"([^\"]+)\"");

        List<String> problemi = new ArrayList<>();
        for (Path f : sorgenti(".java", ".form")) {
            String contenuto = Files.readString(f, StandardCharsets.UTF_8);
            Matcher m = (f.toString().endsWith(".form") ? inForm : inJava).matcher(contenuto);
            while (m.find()) {
                if (!FAMIGLIE_AMMESSE.contains(m.group(1))) {
                    problemi.add(f.getFileName() + " chiede il font '" + m.group(1) + "'");
                }
            }
        }

        assertTrue(problemi.isEmpty(), "l'aspetto tornerebbe a dipendere dal sistema:\n"
                + String.join("\n", problemi));
    }

    @Test
    void iTestiDellaGuiUsanoSoloCaratteriPresentiNelFont() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(SORGENTI), "sorgenti non raggiungibili da qui");

        Font font = FontApplicazione.Font(Font.PLAIN, 12);
        List<String> problemi = new ArrayList<>();

        for (Path f : sorgenti(".java", ".form")) {
            if (f.toString().endsWith(".form")) {
                //Nei .form non c'è codice: tutto quello che contengono è descrizione di interfaccia.
                int riga = 0;
                for (String testo : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    riga++;
                    for (String mancante : caratteriMancanti(decodifica(testo), font)) {
                        problemi.add(f.getFileName() + ":" + riga + " usa " + mancante);
                    }
                }
            } else {
                for (Letterale l : letterali(Files.readString(f, StandardCharsets.UTF_8))) {
                    if (FUORI_DALLA_GUI.contains(f.getFileName().toString()) || l.esente()) {
                        continue;
                    }
                    for (String mancante : caratteriMancanti(decodifica(l.testo), font)) {
                        problemi.add(f.getFileName() + ":" + l.riga + " usa " + mancante);
                    }
                }
            }
        }

        assertTrue(problemi.isEmpty(), "caratteri che il font incluso non ha, e che quindi verrebbero "
                + "disegnati come un rettangolo vuoto (se il testo non finisce a video, marcare la riga "
                + "con " + ESENZIONE + "):\n" + String.join("\n", problemi));
    }

    /**
     * File i cui testi non passano mai dal font dell'interfaccia. {@code Stampe} produce il PDF, dove
     * i font sono quelli di iText (le Helvetica base-14, uguali ovunque per definizione).
     */
    private static final Set<String> FUORI_DALLA_GUI = Set.of("Stampe.java");

    /**
     * Marcatore da mettere in coda alla riga quando una stringa contiene di proposito un carattere che
     * il font non ha, perché non viene disegnata dall'interfaccia (testo del PDF costruito a pezzi,
     * log, output di console). È volutamente esplicito: la scorciatoia "escludo l'intero file" nasconde
     * anche i testi della GUI che quel file dovesse avere.
     */
    private static final String ESENZIONE = "//NON-A-VIDEO";

    /** Una stringa letterale trovata nel sorgente, con la riga da cui viene e il resto della riga. */
    private record Letterale(int riga, String testo, String rigaIntera) {

        /** Vero se la riga è marcata come non destinata all'interfaccia. */
        boolean esente() {
            return rigaIntera.contains(ESENZIONE);
        }
    }

    /**
     * Le stringhe letterali di un sorgente Java, saltando commenti, letterali di carattere e
     * <b>blocchi di testo</b>.
     * <p>
     * I blocchi di testo restano fuori di proposito: nel programma sono le note di compilazione dei
     * quadri fiscali, cioè HTML che finisce nel PDF, e contengono frecce a decine. Filtrare invece per
     * {@code setText(} — come faceva la prima versione di questo test — sembra più mirato ma lascia
     * fuori proprio i casi che il test è nato per prendere: un {@code case SUCCESS -> "✓"}, una
     * concatenazione assegnata a una variabile e passata a un {@code setToolTipText} due righe dopo.
     */
    private static List<Letterale> letterali(String sorgente) {
        List<Letterale> trovati = new ArrayList<>();
        String[] righe = sorgente.split("\n", -1);
        boolean inCommentoBlocco = false;
        boolean inBloccoTesto = false;

        for (int n = 0; n < righe.length; n++) {
            String r = righe[n];
            int i = 0;
            while (i < r.length()) {
                if (inCommentoBlocco) {
                    int fine = r.indexOf("*/", i);
                    if (fine < 0) {
                        break;
                    }
                    inCommentoBlocco = false;
                    i = fine + 2;
                } else if (inBloccoTesto) {
                    int fine = r.indexOf("\"\"\"", i);
                    if (fine < 0) {
                        break;
                    }
                    inBloccoTesto = false;
                    i = fine + 3;
                } else if (r.startsWith("//", i)) {
                    break;
                } else if (r.startsWith("/*", i)) {
                    inCommentoBlocco = true;
                    i += 2;
                } else if (r.startsWith("\"\"\"", i)) {
                    inBloccoTesto = true;
                    i += 3;
                } else if (r.charAt(i) == '\'') {
                    //Letterale di carattere: '\'' e '\\' vanno saltati senza chiudere qui.
                    i++;
                    while (i < r.length() && r.charAt(i) != '\'') {
                        i += r.charAt(i) == '\\' ? 2 : 1;
                    }
                    i++;
                } else if (r.charAt(i) == '"') {
                    int inizio = ++i;
                    while (i < r.length() && r.charAt(i) != '"') {
                        i += r.charAt(i) == '\\' ? 2 : 1;
                    }
                    trovati.add(new Letterale(n + 1, r.substring(inizio, Math.min(i, r.length())), r));
                    i++;
                } else {
                    i++;
                }
            }
        }
        return trovati;
    }

    /** I caratteri non ASCII della riga che il font non sa disegnare, in forma leggibile. */
    private static Set<String> caratteriMancanti(String testo, Font font) {
        Set<String> mancanti = new LinkedHashSet<>();
        for (int i = 0; i < testo.length(); ) {
            int cp = testo.codePointAt(i);
            i += Character.charCount(cp);
            //I caratteri di controllo e i selettori di variante non hanno un glifo per definizione.
            if (cp < 128 || Character.getType(cp) == Character.FORMAT || Character.isISOControl(cp)) {
                continue;
            }
            if (!font.canDisplay(cp)) {
                mancanti.add(String.format("U+%04X (%s)", cp, new String(Character.toChars(cp))));
            }
        }
        return mancanti;
    }

    /**
     * Riporta alla forma reale i caratteri che nei sorgenti sono scritti in codice: entità XML nei
     * {@code .form} ({@code &#x2714;}) ed escape unicode nei {@code .java} ({@code \\u2192}). Senza
     * questo passaggio il controllo guarderebbe delle cifre e non troverebbe mai niente.
     */
    private static String decodifica(String riga) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("&#x([0-9a-fA-F]+);|&#(\\d+);|\\\\u([0-9a-fA-F]{4})").matcher(riga);
        int fine = 0;
        while (m.find()) {
            sb.append(riga, fine, m.start());
            String esa = m.group(1) != null ? m.group(1) : m.group(3);
            int cp = esa != null ? Integer.parseInt(esa, 16) : Integer.parseInt(m.group(2));
            sb.appendCodePoint(cp);
            fine = m.end();
        }
        return sb.append(riga.substring(fine)).toString();
    }

    private static List<Path> sorgenti(String... estensioni) throws IOException {
        try (Stream<Path> s = Files.list(SORGENTI)) {
            return s.filter(p -> {
                for (String e : estensioni) {
                    if (p.toString().endsWith(e)) {
                        return true;
                    }
                }
                return false;
            }).sorted().toList();
        }
    }
}
