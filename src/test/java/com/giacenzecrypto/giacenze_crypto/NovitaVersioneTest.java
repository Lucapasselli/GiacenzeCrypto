package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link NovitaVersione}: la pagina delle novità è nel jar, la sezione giusta si trova (per una versione di prova la
 * più in alto), il Markdown diventa HTML senza resti di sintassi, e il testo si disegna col font incluso, che non ha
 * ripieghi per i glifi mancanti (vedi {@code FontApplicazioneTest}, che queste note non le copre).
 */
class NovitaVersioneTest {

    private static List<NovitaVersione.Sezione> sezioni() {
        String md = NovitaVersione.Leggi();
        assertNotNull(md, "la pagina delle novità deve essere nel jar: " + NovitaVersione.RISORSA);
        List<NovitaVersione.Sezione> s = NovitaVersione.Sezioni(md);
        assertFalse(s.isEmpty());
        return s;
    }

    @Test
    void laPaginaENelJarEHaLeSezioniDelleVersioni() {
        List<NovitaVersione.Sezione> s = sezioni();
        assertTrue(s.size() > 10, "una sezione per versione");
        for (NovitaVersione.Sezione x : s) {
            assertTrue(x.Versione().matches("\\d+(\\.\\d+)+"), x.Versione());
            assertFalse(x.Markdown().contains("## Versione"), "ogni sezione si ferma alla successiva");
        }
    }

    @Test
    void sceltaDellaSezione() {
        List<NovitaVersione.Sezione> s = sezioni();
        assertSame(s.get(0), NovitaVersione.SezionePer("1.0.64.08", s), "una versione di prova mostra le note in preparazione");
        assertEquals("1.0.64", NovitaVersione.SezionePer("1.0.64", s).Versione());
        assertNull(NovitaVersione.SezionePer("9.9.9", s), "una versione rilasciata senza note non mostra nulla");
        assertTrue(NovitaVersione.Titolo("1.0.64.08", s.get(0)).contains("versione di prova 1.0.64.08"));
        assertEquals("Novità della versione 1.0.64", NovitaVersione.Titolo("1.0.64", NovitaVersione.SezionePer("1.0.64", s)));
    }

    @Test
    void quandoMostrarle() {
        assertTrue(NovitaVersione.DaMostrare("1.0.65", "1.0.64", false), "aggiornamento");
        assertTrue(NovitaVersione.DaMostrare("1.0.65", "", false), "aggiornamento da una versione che non le ricordava");
        assertFalse(NovitaVersione.DaMostrare("1.0.65", "", true), "installazione nuova");
        assertFalse(NovitaVersione.DaMostrare("1.0.65", "1.0.65", false), "già viste");
        assertFalse(NovitaVersione.DaMostrare("sconosciuta", "1.0.64", false));
        assertFalse(NovitaVersione.DaMostrare("${project.version}", "", false));
    }

    @Test
    void ilMarkdownDiOgniSezioneDiventaHtmlPulito() {
        for (NovitaVersione.Sezione x : sezioni()) {
            String h = NovitaVersione.Html(x.Markdown());
            String testo = h.replaceAll("<[^>]+>", "");
            assertFalse(testo.contains("**"), x.Versione() + ": grassetto non convertito");
            assertFalse(testo.contains("{#"), x.Versione() + ": ancora rimasta");
            assertFalse(testo.contains("]("), x.Versione() + ": collegamento non convertito");
            assertFalse(h.contains("href='./") || h.contains("href='#"), x.Versione() + ": collegamento relativo");
        }
    }

    @Test
    void conversioneDelleFormeUsate() {
        String h = NovitaVersione.Html("**Nuove implementazioni**\n\n- **Uno** con *corsivo* e `codice`\ncontinua\n- [due](./) e [tre](#versione-1064)\n\n> nota");
        assertTrue(h.contains("<p><b>Nuove implementazioni</b></p>"), h);
        assertTrue(h.contains("<ul><li><b>Uno</b> con <i>corsivo</i> e <code>codice</code><br>continua</li><li>"), h);
        assertTrue(h.contains("href='" + DocumentiAiuto.Url("") + "'"), h);
        assertTrue(h.contains("href='" + DocumentiAiuto.Url(DocumentiAiuto.NOVITA_VERSIONI) + "#versione-1064'"), h);
        assertTrue(h.contains("<blockquote>nota</blockquote>"), h);
        assertTrue(NovitaVersione.Html("a < b & c").contains("a &lt; b &amp; c"));
    }

    @Test
    void ilTestoDelleNoteSiDisegnaColFontIncluso() {
        FontApplicazione.Registra();
        Font f = FontApplicazione.Font(Font.PLAIN, 12);
        for (NovitaVersione.Sezione x : sezioni().subList(0, 3)) {
            String testo = NovitaVersione.Html(x.Markdown()).replaceAll("<[^>]+>", "")
                    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
            int i = f.canDisplayUpTo(testo);
            assertEquals(-1, i, x.Versione() + ": glifo mancante " + (i < 0 ? "" : "'" + testo.charAt(i) + "' in ..."
                    + testo.substring(Math.max(0, i - 30), Math.min(testo.length(), i + 10))));
        }
    }

    private static List<NovitaVersione.Sezione> sezioniBeta() {
        String md = NovitaVersione.LeggiBeta();
        assertNotNull(md, "le note delle beta devono essere nel jar: " + NovitaVersione.RISORSA_BETA);
        return NovitaVersione.Sezioni(md);
    }

    @Test
    void leNoteDelleBetaSonoNelJarConVersioniAQuattroNumeri() {
        List<NovitaVersione.Sezione> b = sezioniBeta();
        assertFalse(b.isEmpty());
        FontApplicazione.Registra();
        Font f = FontApplicazione.Font(Font.PLAIN, 12);
        for (NovitaVersione.Sezione x : b) {
            assertTrue(NovitaVersione.isVersioneDiProva(x.Versione()), x.Versione() + ": una beta ha quattro numeri");
            assertFalse(x.Markdown().isBlank(), x.Versione() + ": sezione vuota");
            String h = NovitaVersione.Html(x.Markdown());
            String testo = h.replaceAll("<[^>]+>", "");
            assertFalse(testo.contains("**") || testo.contains("]("), x.Versione() + ": Markdown non convertito");
            assertEquals(-1, f.canDisplayUpTo(testo.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")),
                    x.Versione() + ": glifo che il font incluso non ha");
        }
        for (int i = 1; i < b.size(); i++) {
            assertTrue(Funzioni.ConfrontaVersioni(b.get(i - 1).Versione(), b.get(i).Versione()) > 0, "dalla beta più recente");
        }
    }

    /** Ogni voce di una beta ha la sua voce, con lo stesso titolo, nel changelog principale: quello resta completo. */
    @Test
    void ogniVoceDelleBetaENelChangelogPrincipale() {
        String principale = NovitaVersione.Leggi();
        Pattern voce = Pattern.compile("(?m)^- \\*\\*(.+?)\\*\\*");
        for (NovitaVersione.Sezione x : sezioniBeta()) {
            Matcher m = voce.matcher(x.Markdown());
            while (m.find()) {
                assertTrue(principale.contains("**" + m.group(1) + "**"),
                        x.Versione() + ": \"" + m.group(1) + "\" manca in changelog.md");
            }
        }
    }

    private static NovitaVersione.Sezione s(String v) {
        return new NovitaVersione.Sezione(v, "- voce " + v);
    }

    @Test
    void sezioniDaMostrareCumulative() {
        List<NovitaVersione.Sezione> principali = List.of(s("1.0.66"), s("1.0.65"), s("1.0.64"), s("1.0.63"));
        List<NovitaVersione.Sezione> beta = List.of(s("1.0.65.14"), s("1.0.65.13"), s("1.0.65.12"));
        //Beta: solo il file delle beta, e tutte quelle saltate
        assertEquals(List.of("1.0.65.13", "1.0.65.12"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.13", "1.0.65.11", principali, beta)));
        assertEquals(List.of("1.0.65.13"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.13", "1.0.65.12", principali, beta)));
        assertEquals(List.of("1.0.65.13"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.13", "", principali, beta)));
        assertEquals(List.of("1.0.65.13"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.13", "1.0.65.13", principali, beta)),
                "dal pulsante delle Informazioni: solo la versione");
        assertEquals(List.of("1.0.65.13", "1.0.65.12"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.13", "1.0.65", principali, beta)),
                "dalla stabile alla beta");
        //Beta senza note proprie: le note in preparazione
        assertEquals(List.of("1.0.66"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65.15", "1.0.65.14", principali, beta)));
        //Stabile: dal changelog, comprese quelle saltate, mai le beta
        assertEquals(List.of("1.0.66"), versioni(NovitaVersione.SezioniDaMostrare("1.0.66", "1.0.65.14", principali, beta)));
        assertEquals(List.of("1.0.66", "1.0.65", "1.0.64"), versioni(NovitaVersione.SezioniDaMostrare("1.0.66", "1.0.63", principali, beta)));
        assertEquals(List.of("1.0.65"), versioni(NovitaVersione.SezioniDaMostrare("1.0.65", "", principali, beta)));
        assertFalse(NovitaVersione.SezioniDaMostrare("9.9.9", "1.0.63", principali, beta).isEmpty(),
                "le sezioni saltate si mostrano anche se la versione nuova non ha note");
        assertTrue(NovitaVersione.SezioniDaMostrare("sconosciuta", "", principali, beta).isEmpty());
    }

    @Test
    void titoliEHtmlDiPiuSezioni() {
        List<NovitaVersione.Sezione> due = List.of(s("1.0.65.13"), s("1.0.65.12"));
        assertEquals("Novità delle versioni di prova dalla 1.0.65.12 alla 1.0.65.13", NovitaVersione.Titolo("1.0.65.13", due));
        assertEquals("Novità della versione di prova 1.0.65.13", NovitaVersione.Titolo("1.0.65.13", List.of(s("1.0.65.13"))));
        assertEquals("Novità della versione 1.0.66 (versione di prova 1.0.65.15)", NovitaVersione.Titolo("1.0.65.15", List.of(s("1.0.66"))));
        assertEquals("Novità della versione 1.0.66", NovitaVersione.Titolo("1.0.66", List.of(s("1.0.66"))));
        String h = NovitaVersione.Html(due);
        assertTrue(h.indexOf("Versione 1.0.65.13") < h.indexOf("Versione 1.0.65.12") && h.contains("<h3>"), h);
    }

    private static List<String> versioni(List<NovitaVersione.Sezione> l) {
        return l.stream().map(NovitaVersione.Sezione::Versione).toList();
    }
}
