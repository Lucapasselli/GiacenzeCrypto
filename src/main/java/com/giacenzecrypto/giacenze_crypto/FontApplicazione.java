package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Font dell'interfaccia, incluso nel jar e registrato all'avvio.
 * <p>
 * <b>Perché esiste.</b> Prima di questa classe l'aspetto dell'applicazione lo decideva il sans-serif
 * predefinito del sistema: il default globale era {@code "Inter"} e 55 componenti generati da NetBeans
 * chiedevano {@code "Segoe UI"}, famiglie che su una macchina Linux normale non esistono. E
 * {@code new Font(famiglia, ...)} <b>non segnala niente</b> quando la famiglia manca: ripiega in
 * silenzio sul font logico {@code Dialog}, che a sua volta viene risolto dal sistema (fontconfig su
 * Linux, che su Windows e macOS non esiste nemmeno). Le metriche cambiano parecchio — "Ricalcola
 * Plusvalenze" a 12pt occupa 118 px con Noto Sans e 131 px con DejaVu Sans, +11%, e in grassetto +18% —
 * e sommate ai {@code setPreferredSize} fissi di molti di quei componenti il risultato era testo che
 * deborda e pulsanti disallineati, diversi da un sistema all'altro.
 * <p>
 * <b>Scelta della famiglia.</b> Noto Sans, e non è una preferenza estetica: è il sans-serif predefinito
 * della maggior parte delle distribuzioni Linux e quindi è già quello contro cui tutti i layout sono
 * stati tarati. Includerlo lascia identico l'aspetto dove lo era già e porta Windows, macOS e il
 * runtime freedesktop del flatpak (che monta DejaVu Sans) sullo stesso risultato.
 * <p>
 * <b>Perché registrarlo invece di dichiararlo nel pacchetto.</b> {@link GraphicsEnvironment#registerFont}
 * vale per ogni pacchetto e ogni sistema operativo, senza dipendere da fontconfig né da cosa l'utente
 * finale ha installato. La correzione precedente — i quattro TTF inclusi nel flatpak più un
 * {@code fonts.conf} che li mette davanti — copriva solo quel pacchetto e lasciava fuori deb, AUR,
 * portable, installer Windows e dmg.
 * <p>
 * <b>Due trappole.</b> {@code registerFont} ritorna {@code false} quando la famiglia è già presente nel
 * sistema (il caso normale su Linux): <b>non è un errore</b> e non va trattato come tale. L'unica
 * verifica che significhi qualcosa è {@link #Disponibile()}, cioè che la ricerca per nome non sia
 * ricaduta su {@code Dialog}. E i quattro tagli vanno registrati tutti: il corsivo non è chiesto da
 * nessun componente, ma dai {@code <i>} dell'HTML dentro le label e i {@code JTextPane} sì.
 * <p>
 * Come {@link SplashAvvio}, questa classe non dipende da nient'altro del programma: viene chiamata
 * prima dello splash, cioè prima del database e prima del Look&amp;Feel.
 */
public final class FontApplicazione {

    /** Famiglia dell'interfaccia. Da usare ovunque al posto di un nome scritto a mano. */
    public static final String FAMIGLIA = "Noto Sans";

    /** I quattro tagli inclusi in {@code src/main/resources/Fonts/} (~2,5 MB in tutto). */
    private static final String[] FACCE = {
        "/Fonts/NotoSans-Regular.ttf",
        "/Fonts/NotoSans-Bold.ttf",
        "/Fonts/NotoSans-Italic.ttf",
        "/Fonts/NotoSans-BoldItalic.ttf"
    };

    private static boolean registrato = false;

    private FontApplicazione() {
    }

    /**
     * Carica dal jar i tagli di {@link #FAMIGLIA} e li registra nel {@link GraphicsEnvironment}.
     * Idempotente: le chiamate successive alla prima non fanno nulla, così può essere invocata senza
     * pensarci da ogni punto d'ingresso (l'applicazione, {@link GeneraSplash}, i test).
     *
     * @return {@code true} se dopo la registrazione la famiglia è effettivamente raggiungibile per nome
     */
    public static synchronized boolean Registra() {
        if (registrato) {
            return Disponibile();
        }
        registrato = true;

        long inizio = System.currentTimeMillis();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String risorsa : FACCE) {
            try (InputStream is = FontApplicazione.class.getResourceAsStream(risorsa)) {
                if (is == null) {
                    System.err.println("Font non trovato nel jar: " + risorsa);
                    continue;
                }
                //registerFont ritorna false se la famiglia è già installata nel sistema: è il caso
                //normale su Linux e non toglie niente, il font del jar e quello di sistema sono lo
                //stesso file (i quattro TTF sono fissati per checksum, vedi il manifest del flatpak).
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, is));
            } catch (Exception ex) {
                //Nessuna eccezione qui deve impedire l'avvio: senza il font incluso l'applicazione
                //resta usabile con quello di sistema, esattamente com'era prima.
                System.err.println("Errore nel caricamento del font " + risorsa + ": " + ex);
            }
        }

        boolean ok = Disponibile();
        System.out.println("Font applicazione '" + FAMIGLIA + "' registrato in "
                + (System.currentTimeMillis() - inizio) + " ms, disponibile=" + ok);
        return ok;
    }

    /**
     * Verifica che la famiglia sia raggiungibile per nome, cioè che {@code new Font(FAMIGLIA, ...)} non
     * sia ricaduto in silenzio su {@code Dialog}. È l'unico controllo sensato: il valore ritornato da
     * {@code registerFont} dice solo se la famiglia è stata <i>aggiunta</i>, e vale {@code false} anche
     * quando è già presente nel sistema.
     */
    public static boolean Disponibile() {
        //getFamily() senza Locale restituirebbe il nome tradotto della famiglia, che non è detto
        //coincida con quello con cui la si cerca.
        return FAMIGLIA.equalsIgnoreCase(new Font(FAMIGLIA, Font.PLAIN, 12).getFamily(Locale.ROOT));
    }

    /** Il font dell'interfaccia nello stile e nel corpo richiesti. */
    public static Font Font(int stile, int corpo) {
        return new Font(FAMIGLIA, stile, corpo);
    }

    private static final Map<String, Font> CON_RIPIEGO = new ConcurrentHashMap<>();

    /**
     * Il font con cui disegnare {@code testo}: {@code f} stesso, oppure lo stesso font con il ripiego sui font di sistema
     * quando il testo ha caratteri che {@code f} non disegna (i simboli dei token truffa scritti in lisu, cherokee,
     * sillabico canadese...). Il ripiego è il font composito di Swing ({@code StyleContext.getFont}): Noto Sans per i
     * caratteri che ha, gli altri dai font di sistema, quindi metriche e aspetto del resto non cambiano.
     * <p>
     * <b>Non con le emoji fuori dal piano di base</b>: dove il sistema le ha solo a colori (Noto Color Emoji su Linux)
     * Java non le disegna e il composito le lascerebbe vuote, peggio del quadratino di Noto Sans. Un testo con una
     * di quelle resta col font di partenza. Dove il sistema non ha i caratteri resta il quadratino di prima.
     */
    public static Font PerTesto(Font f, String testo) {
        if (f == null || testo == null || testo.isEmpty() || f.canDisplayUpTo(testo) == -1) return f;
        boolean serve = false;
        for (int i = 0; i < testo.length(); ) {
            int c = testo.codePointAt(i);
            i += Character.charCount(c);
            if (c >= 0x1F000 && c <= 0x1FAFF) return f;
            if (!f.canDisplay(c) && Character.getType(c) != Character.FORMAT && !Character.isWhitespace(c)) serve = true;
        }
        if (!serve) return f;
        return Composito(f);
    }

    private static Font Composito(Font f) {
        String famiglia = f.getFamily(Locale.ROOT);
        return CON_RIPIEGO.computeIfAbsent(famiglia + "|" + f.getStyle() + "|" + f.getSize2D(),
                k -> javax.swing.text.StyleContext.getDefaultStyleContext().getFont(famiglia, f.getStyle(), f.getSize())
                        .deriveFont(f.getSize2D()));
    }

    /** Il taglio monocromatico di Noto Emoji incluso nel jar, letto alla prima richiesta ({@code null} se manca). */
    private static final String RISORSA_EMOJI = "/Fonts/NotoEmoji.ttf";
    private static Font emojiBase;
    private static boolean emojiLetto = false;

    private static synchronized Font EmojiBase() {
        if (!emojiLetto) {
            emojiLetto = true;
            try (InputStream is = FontApplicazione.class.getResourceAsStream(RISORSA_EMOJI)) {
                //Registrato solo alla prima emoji incontrata (non costa nulla all'avvio): i componenti non lo chiedono per
                //nome, ma l'HTML delle etichette sì (HtmlConEmoji, font-family nello span).
                if (is != null) {
                    emojiBase = Font.createFont(Font.TRUETYPE_FONT, is);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(emojiBase);
                }
            } catch (Exception ex) {
                System.err.println("Errore nel caricamento del font emoji: " + ex);
            }
        }
        return emojiBase;
    }

    /** Noto Emoji nel corpo di {@code f}, sempre in stile normale, oppure {@code null} se non è nel jar. */
    private static Font FontEmoji(Font f) {
        Font e = EmojiBase();
        return e == null ? null : e.deriveFont(Font.PLAIN, f.getSize2D());
    }

    /**
     * Un testo diviso in tratti, ciascuno col suo font: {@code Noto Sans} per i caratteri che ha, {@code Noto Emoji} per le
     * emoji e i pittogrammi che Noto Sans non ha, il ripiego di sistema ({@link #PerTesto}) per tutto il resto (lisu, cherokee...).
     *
     * @return {@code null} se nel testo non c'è nessuna emoji da disegnare con Noto Emoji: allora basta {@link #PerTesto} col
     *         normale disegno di Swing, che qui non si tocca
     */
    public static java.text.AttributedString PerTestoConEmoji(Font f, String testo) {
        if (f == null || testo == null || testo.isEmpty()) return null;
        //Il font della cella può essere già il composito di PerTesto, che dichiara di disegnare le emoji a colori senza
        //disegnarle: si riparte dal font fisico della stessa famiglia, che i caratteri che non ha li dichiara mancanti.
        f = new Font(f.getFamily(Locale.ROOT), f.getStyle(), f.getSize()).deriveFont(f.getSize2D());
        if (f.canDisplayUpTo(testo) == -1) return null;
        Font emoji = FontEmoji(f);
        if (emoji == null) return null;
        Font ripiego = null;
        java.text.AttributedString as = new java.text.AttributedString(testo);
        boolean haEmoji = false;
        int inizio = 0;
        Font corrente = null;
        for (int i = 0; i < testo.length(); ) {
            int c = testo.codePointAt(i);
            Font fc;
            if (f.canDisplay(c) || Character.getType(c) == Character.FORMAT || Character.isWhitespace(c)) {
                fc = f;
            } else if (emoji.canDisplay(c)) {
                fc = emoji;
                haEmoji = true;
            } else {
                if (ripiego == null) ripiego = Composito(f);
                fc = ripiego;
            }
            if (corrente != null && fc != corrente) {
                as.addAttribute(java.awt.font.TextAttribute.FONT, corrente, inizio, i);
                inizio = i;
            }
            corrente = fc;
            i += Character.charCount(c);
        }
        as.addAttribute(java.awt.font.TextAttribute.FONT, corrente, inizio, testo.length());
        return haEmoji ? as : null;
    }

    /** Disegna i tratti di {@link #PerTestoConEmoji} con la linea di base in {@code y}, con le stesse rifiniture del testo di Swing. */
    public static void Disegna(java.awt.Graphics g, java.text.AttributedString tratti, float x, float y) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        try {
            Object hints = java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints instanceof Map) g2.addRenderingHints((Map<?, ?>) hints);
            g2.drawString(tratti.getIterator(), x, y);
        } finally {
            g2.dispose();
        }
    }

    /** La larghezza con cui {@link #Disegna} scrive i tratti. */
    public static float Larghezza(java.text.AttributedString tratti) {
        return new java.awt.font.TextLayout(tratti.getIterator(), new java.awt.font.FontRenderContext(null, true, true)).getAdvance();
    }

    private static final String SPAN_EMOJI = "<span style=\"font-family:'Noto Emoji';font-weight:normal\">";

    /**
     * Lo stesso HTML con le emoji (quelle che Noto Sans non ha e Noto Emoji sì) avvolte in uno {@code span} col font emoji,
     * perché una vista HTML di Swing sceglie il font per famiglia e non ripiega da sé. Peso normale: in grassetto il font
     * variabile sintetizzato non disegna niente. Il testo fuori dai tag, mai i tag.
     * Ritorna {@code html} stesso quando non c'è niente da cambiare.
     */
    public static String HtmlConEmoji(String html) {
        if (html == null || html.isEmpty()) return html;
        Font emoji = EmojiBase();
        if (emoji == null) return html;
        Font sans = new Font(FAMIGLIA, Font.PLAIN, 12);
        StringBuilder sb = null;
        boolean nelTag = false, aperto = false;
        for (int i = 0; i < html.length(); ) {
            int c = html.codePointAt(i);
            int n = Character.charCount(c);
            if (c == '<') nelTag = true;
            boolean emojiQui = !nelTag && !sans.canDisplay(c) && emoji.canDisplay(c) && !Character.isWhitespace(c);
            //Il selettore di variante e i caratteri invisibili tra due emoji restano nello span, se è aperto
            boolean invisibile = !nelTag && aperto && (Character.getType(c) == Character.FORMAT || (c >= 0xFE00 && c <= 0xFE0F));
            if (emojiQui && sb == null) {
                sb = new StringBuilder(html.length() + 64).append(html, 0, i);
            }
            if (sb != null) {
                if ((emojiQui || invisibile) && !aperto) {
                    sb.append(SPAN_EMOJI);
                    aperto = true;
                } else if (!emojiQui && !invisibile && aperto) {
                    sb.append("</span>");
                    aperto = false;
                }
                sb.appendCodePoint(c);
            }
            if (c == '>') nelTag = false;
            i += n;
        }
        if (sb == null) return html;
        if (aperto) sb.append("</span>");
        return sb.toString();
    }
}
