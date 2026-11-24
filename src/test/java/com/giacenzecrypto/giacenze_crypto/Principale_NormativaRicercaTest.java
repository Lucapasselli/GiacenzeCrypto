package com.giacenzecrypto.giacenze_crypto;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ricerca per estremi del tab "Normative" (stessa regola della pagina del sito): un atto si trova
 * dal tipo e dal numero, anche senza la "/" o con il "n." in mezzo. I documenti sono inventati
 * ma con la forma di quelli di {@code fonti.csv}. Nessun database.
 */
class Principale_NormativaRicercaTest {

    private static DocumentoNormativa doc(String percorso, String titolo, String identificativo, String... argomenti) {
        return new DocumentoNormativa(percorso, "ufficiale", titolo, "Agenzia delle entrate", identificativo,
                LocalDate.of(2016, 4, 8), List.of(argomenti), "", "", "x");
    }

    private static final DocumentoNormativa CIRC_12E = doc("Prassi_AgenziaEntrate/a.pdf",
            "Chiarimenti interpretativi relativi a quesiti", "Circolare n. 12/E dell'8 aprile 2016", "quadro RW");
    private static final DocumentoNormativa CIRC_28E = doc("Prassi_AgenziaEntrate/b.pdf",
            "Imposta sul valore degli immobili", "Circolare n. 28/E del 2 luglio 2012", "IVAFE");
    private static final DocumentoNormativa RIS_36E = doc("Prassi_AgenziaEntrate/c.pdf",
            "Istituzione dei codici tributo", "Risoluzione n. 36/E del 26 giugno 2023", "codici tributo");
    private static final DocumentoNormativa LEGGE = doc("Leggi/Originale/d.xml",
            "Legge 29 dicembre 2022, n. 197 - Bilancio di previsione", "urn:nir:stato:legge:2022-12-29;197");

    @Test
    void laSiglaSiTrovaInTuttiIModiDiScriverla() {
        for (String q : new String[]{"circolare 12/e", "Circolare 12/E", "circolare 12 / e", "circolare 12e",
            "circolare n. 12/E", "circolare 12", "12/e", "12e", "circ 12/e"}) {
            assertTrue(Principale_Normativa.CorrispondeEstremi(CIRC_12E, q), q);
        }
    }

    @Test
    void ilNumeroDeveCoincidereIntero() {
        // "12" non è "2012" (circolare 28/E del 2012) né "28"
        assertFalse(Principale_Normativa.CorrispondeEstremi(CIRC_28E, "circolare 12"));
        assertFalse(Principale_Normativa.CorrispondeEstremi(CIRC_28E, "circolare 12/e"));
        assertTrue(Principale_Normativa.CorrispondeEstremi(CIRC_28E, "circolare 28/e"));
        assertTrue(Principale_Normativa.CorrispondeEstremi(CIRC_28E, "circolare 2012"));
    }

    @Test
    void iltipoDiAttoDeveCombaciare() {
        assertFalse(Principale_Normativa.CorrispondeEstremi(RIS_36E, "circolare 36/e"));
        assertTrue(Principale_Normativa.CorrispondeEstremi(RIS_36E, "risoluzione 36"));
        assertTrue(Principale_Normativa.CorrispondeEstremi(LEGGE, "legge 197/2022"));
        assertTrue(Principale_Normativa.CorrispondeEstremi(LEGGE, "legge 197"));
    }

    @Test
    void unaRicercaVuotaONeutraNonTrovaNulla() {
        assertFalse(Principale_Normativa.CorrispondeEstremi(CIRC_12E, ""));
        assertFalse(Principale_Normativa.CorrispondeEstremi(CIRC_12E, "  n. del "));
    }

    @Test
    void iltipoDiAttoSiRicavaDagliEstremi() {
        assertEquals("Circolare", Principale_Normativa.TipoAtto(CIRC_12E));
        assertEquals("Risoluzione", Principale_Normativa.TipoAtto(RIS_36E));
        assertEquals("Legge", Principale_Normativa.TipoAtto(LEGGE));
        assertEquals("Risposta a interpello", Principale_Normativa.TipoAtto(
                doc("p/e.pdf", "Oggetto", "Risposta a interpello n. 149 del 24 gennaio 2023")));
        assertEquals("Provvedimento", Principale_Normativa.TipoAtto(
                doc("p/f.pdf", "Oggetto", "Allegato 2 al Provvedimento del 7 agosto 2023")));
        assertEquals("Decreto-legge", Principale_Normativa.TipoAtto(
                doc("p/g.xml", "D.L. 6 dicembre 2011, n. 201 (conv. L. 22 dicembre 2011, n. 214) - Disposizioni", "urn:nir:x")));
        assertEquals("Modelli e istruzioni", Principale_Normativa.TipoAtto(
                doc("Istruzioni_Dichiarazioni/h.pdf", "Modello 730 2023 - Istruzioni", "Modello 730 2023 (redditi 2022)")));
        assertEquals("Altro", Principale_Normativa.TipoAtto(doc("p/i.md", "Qualcosa di strano", "")));
    }

    @Test
    void gliEstremiDelleLeggiSonoLaPrimaParteDelTitolo() {
        assertEquals("Legge 29 dicembre 2022, n. 197", Principale_Normativa.Estremi(LEGGE));
        assertEquals("Circolare n. 12/E dell'8 aprile 2016", Principale_Normativa.Estremi(CIRC_12E));
    }

    @Test
    void ilRipiegoSuTutteLeParoleNonRichiedeLaFrase() {
        String testo = "il trattamento fiscale delle cripto-attivita e dei token di moneta elettronica";
        assertTrue(Principale_Normativa.ContieneTutteLeParole(testo, "moneta token"));
        assertFalse(Principale_Normativa.ContieneTutteLeParole(testo, "moneta derivati"));
        assertFalse(Principale_Normativa.ContieneTutteLeParole(testo, "   "));
    }

    @Test
    void ifiltriDelCatalogoElencanoIPiuFrequentiPerPrimi() {
        List<DocumentoNormativa> cat = List.of(CIRC_12E, CIRC_28E, RIS_36E, LEGGE);
        List<String> tipi = Principale_Normativa.TipiAttoPresenti(cat);
        assertEquals(Principale_Normativa.TUTTI, tipi.get(0));
        assertEquals("Circolare", tipi.get(1));
        assertTrue(tipi.containsAll(List.of("Risoluzione", "Legge")));
        List<String> argomenti = Principale_Normativa.ArgomentiPresenti(cat);
        assertEquals(Principale_Normativa.TUTTI, argomenti.get(0));
        assertTrue(argomenti.containsAll(List.of("quadro RW", "IVAFE", "codici tributo")));
    }
}
