package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import java.util.List;
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
}
