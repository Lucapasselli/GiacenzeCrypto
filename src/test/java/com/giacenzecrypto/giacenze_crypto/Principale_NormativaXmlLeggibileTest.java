package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Il render strutturato XML→HTML del tab Normative ({@link Principale_Normativa#RenderNodo}) è un
 * porto Java di {@code Sito/strumenti/genera_indice_normativa.py:_render_nodo}: questi test
 * verificano che i due si comportino allo stesso modo sui casi che contano — intestazione
 * dell'atto, articoli/commi numerati, esclusione di {@code meta}, e il ripiego sul testo piatto
 * quando la struttura non produce nulla.
 */
class Principale_NormativaXmlLeggibileTest {

    private static Element parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return doc.getDocumentElement();
    }

    @Test
    void intestazioneERendeDocTypeDocNumberDocTitle() throws Exception {
        String xml = """
                <akomaNtoso>
                  <act>
                    <preface>
                      <docType>Legge</docType>
                      <docNumber>197</docNumber>
                      <docDate date="2022-12-29"/>
                      <docTitle>Bilancio di previsione</docTitle>
                    </preface>
                    <body/>
                  </act>
                </akomaNtoso>
                """;
        String html = Principale_Normativa.RenderNodo(parse(xml));

        assertTrue(html.contains("class=\"intestazione-atto\""));
        assertTrue(html.contains("Legge"));
        assertTrue(html.contains("n. 197"));
        assertTrue(html.contains("Bilancio di previsione"));
    }

    @Test
    void articoloConDueCommiNumerati() throws Exception {
        String xml = """
                <akomaNtoso><act><body>
                  <article>
                    <num>Art. 3</num>
                    <heading>Disposizioni transitorie</heading>
                    <paragraph>
                      <num>1.</num>
                      <content><p>Primo comma.</p></content>
                    </paragraph>
                    <paragraph>
                      <num>2.</num>
                      <content><p>Secondo comma.</p></content>
                    </paragraph>
                  </article>
                </body></act></akomaNtoso>
                """;
        String html = Principale_Normativa.RenderNodo(parse(xml));

        assertTrue(html.contains("class=\"articolo\""));
        assertTrue(html.contains("Art. 3 Disposizioni transitorie"));
        assertEquals(2, html.split("class=\"comma\"", -1).length - 1);
        assertTrue(html.contains(">1.<"));
        assertTrue(html.contains("Primo comma."));
        assertTrue(html.contains("Secondo comma."));
    }

    @Test
    void puntoElencoDentroList() throws Exception {
        String xml = """
                <akomaNtoso><act><body>
                  <article>
                    <list>
                      <point><num>a)</num><content><p>primo punto</p></content></point>
                      <point><num>b)</num><content><p>secondo punto</p></content></point>
                    </list>
                  </article>
                </body></act></akomaNtoso>
                """;
        String html = Principale_Normativa.RenderNodo(parse(xml));

        assertTrue(html.contains("class=\"elenco-normativo\""));
        assertEquals(2, html.split("class=\"punto-elenco\"", -1).length - 1);
        assertTrue(html.contains("primo punto"));
        assertTrue(html.contains("secondo punto"));
    }

    @Test
    void metaEscluso() throws Exception {
        String xml = """
                <akomaNtoso><act>
                  <meta><identification>QUESTO-TESTO-NON-DEVE-APPARIRE</identification></meta>
                  <body><article><num>Art. 1</num><content><p>Corpo.</p></content></article></body>
                </act></akomaNtoso>
                """;
        String html = Principale_Normativa.RenderNodo(parse(xml));

        assertFalse(html.contains("QUESTO-TESTO-NON-DEVE-APPARIRE"));
        assertTrue(html.contains("Corpo."));
    }

    @Test
    void tagSconosciutoScendeComunqueNeiFigli() throws Exception {
        // Variante di schema non prevista: nessun caso specifico per <schedule>, ma il testo
        // dei figli non deve sparire (fallback generico in fondo a RenderNodo).
        String xml = """
                <akomaNtoso><act><body>
                  <schedule><p>Testo in una variante non censita.</p></schedule>
                </body></act></akomaNtoso>
                """;
        String html = Principale_Normativa.RenderNodo(parse(xml));

        assertTrue(html.contains("Testo in una variante non censita."));
    }

    @Test
    void suUnAttoRealeIlRenderStrutturatoEscludeIMetadatiEProduceArticoli() throws Exception {
        File file = new File("Normativa/Leggi/Originale/NormeRichiamate/DL_1990-06-28_167_vigente.akn.xml");
        assertTrue(file.exists(), "Normativa/ e' committata: se manca, il percorso o il repository sono cambiati");

        String html = Principale_Normativa.EstraiXmlLeggibile(file);

        assertFalse(html.isBlank());
        assertFalse(html.contains("passiveModifications"),
                "il testo dei metadati (storico delle modifiche) non deve trapelare nell'HTML leggibile");
        assertTrue(html.contains("class=\"articolo\""));
    }
}
