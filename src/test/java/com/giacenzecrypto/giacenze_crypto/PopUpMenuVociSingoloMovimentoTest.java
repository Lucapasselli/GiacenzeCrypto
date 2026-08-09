package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Fissa le voci del menu contestuale dei movimenti che devono restare disattivate su selezione multipla.
 *
 * <p>Il meccanismo di abilitazione confronta le <b>etichette</b> delle voci
 * ({@link Funzioni#PopUp_disabilitaMenuDatesto}): una stringa che non corrisponde a nessuna voce non
 * produce alcun errore, semplicemente non disabilita niente. È già accaduto per davvero — la voce
 * "Chiedi all'IA" veniva cercata come "Chiedi a IA" e quindi non è mai stata disabilitata, nemmeno nel
 * caso, previsto dal codice, di riga che non è un movimento.
 *
 * <p>Per questo il test non si limita a verificare il comportamento su un menu costruito qui (sarebbe
 * circolare, ripeterebbe le stesse stringhe): legge le etichette vere da {@code Principale.form}, che è
 * la sorgente di verità del Designer NetBeans.
 */
public class PopUpMenuVociSingoloMovimentoTest {

    private static final Path FORM =
            Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto/Principale.form");

    /**
     * Estrae le etichette delle voci del solo {@code JPopupMenu} chiamato "PopupMenu", cioè il menu
     * contestuale dei movimenti: il form contiene centinaia di altri testi e cercarli tutti renderebbe
     * il controllo inutile.
     */
    private static Set<String> etichetteDelPopupDeiMovimenti() throws IOException {
        String form = Files.readString(FORM, StandardCharsets.UTF_8);

        int inizio = form.indexOf("<Container class=\"javax.swing.JPopupMenu\" name=\"PopupMenu\">");
        assertTrue(inizio >= 0, "in Principale.form non c'è più il JPopupMenu chiamato PopupMenu");
        int fine = form.indexOf("</Container>", inizio);
        assertTrue(fine > inizio, "il blocco del PopupMenu in Principale.form non risulta chiuso");

        String blocco = form.substring(inizio, fine);
        Set<String> etichette = new LinkedHashSet<>();
        Matcher m = Pattern.compile("<Property name=\"text\" type=\"java\\.lang\\.String\" value=\"([^\"]*)\"")
                .matcher(blocco);
        while (m.find()) {
            etichette.add(deXml(m.group(1)));
        }
        return etichette;
    }

    /** Riporta al testo vero le entità XML usate dal Designer (l'apostrofo di "Chiedi all'IA" è {@code &apos;}). */
    private static String deXml(String v) {
        return v.replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    @Test
    public void ogniVoceDaDisattivareEsisteDavveroNelMenu() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(FORM),
                "Principale.form non trovato: test eseguito fuori dalla radice del progetto");

        Set<String> etichette = etichetteDelPopupDeiMovimenti();

        List<String> mancanti = new ArrayList<>();
        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            boolean presente = etichette.stream().anyMatch(e -> e.equalsIgnoreCase(voce));
            if (!presente) mancanti.add(voce);
        }

        assertTrue(mancanti.isEmpty(),
                "queste voci non corrispondono a nessuna etichetta del menu contestuale, quindi non "
                        + "verrebbero disabilitate su selezione multipla: " + mancanti
                        + " — etichette presenti nel menu: " + etichette);
    }

    @Test
    public void laDisattivazioneAgisceSuTutteLeVociElencate() {
        JPopupMenu pop = menuFinto();

        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            Funzioni.PopUp_disabilitaMenuDatesto(pop, voce);
        }

        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            assertFalse(vocePerTesto(pop, voce).isEnabled(),
                    "la voce \"" + voce + "\" doveva risultare disattivata");
        }
    }

    @Test
    public void leVociRestanoRiabilitabiliPerLaSelezionesingola() {
        //La riabilitazione la fa PopUpMenu all'inizio del ramo "ID valorizzato": se una voce non fosse
        //riabilitabile per etichetta resterebbe grigia per sempre dopo la prima selezione multipla
        JPopupMenu pop = menuFinto();

        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            Funzioni.PopUp_disabilitaMenuDatesto(pop, voce);
            Funzioni.PopUp_abilitaMenuDaTesto(pop, voce);
            assertTrue(vocePerTesto(pop, voce).isEnabled(),
                    "la voce \"" + voce + "\" non è tornata attivabile");
        }
    }

    @Test
    public void laVoceChiediAllIaEUnSottomenuEVieneDisattivataComeLeAltre() {
        //"Chiedi all'IA" è un JMenu con i chatbot dentro, non un JMenuItem semplice: è il caso che il
        //vecchio refuso nell'etichetta teneva sempre attivo
        JPopupMenu pop = menuFinto();
        JMenuItem chiediIA = vocePerTesto(pop, "Chiedi all'IA");
        assertTrue(chiediIA instanceof JMenu, "la voce Chiedi all'IA deve restare un sottomenu");

        Funzioni.PopUp_disabilitaMenuDatesto(pop, "Chiedi all'IA");
        assertFalse(chiediIA.isEnabled(), "il sottomenu Chiedi all'IA doveva risultare disattivato");
    }

    /** Menu con le stesse etichette di quello vero, per provare le funzioni di abilitazione senza aprire la GUI. */
    private static JPopupMenu menuFinto() {
        JPopupMenu pop = new JPopupMenu();
        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            //Il sottomenu dei chatbot va riprodotto come JMenu, gli altri come voci semplici
            pop.add(voce.equalsIgnoreCase("Chiedi all'IA") ? new JMenu(voce) : new JMenuItem(voce));
        }
        return pop;
    }

    private static JMenuItem vocePerTesto(JPopupMenu pop, String testo) {
        for (JMenuItem item : Funzioni.PopUp_getAllMenuItems(pop)) {
            if (testo.equalsIgnoreCase(item.getText())) return item;
        }
        throw new AssertionError("voce non trovata nel menu di prova: " + testo);
    }

    @Test
    public void lElencoNonContieneDuplicatiNeVociVuote() {
        Set<String> distinte = new LinkedHashSet<>(Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO);
        assertEquals(Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO.size(), distinte.size(),
                "l'elenco delle voci da disattivare contiene duplicati");
        for (String voce : Funzioni.POPUP_VOCI_SOLO_SINGOLO_MOVIMENTO) {
            assertFalse(voce == null || voce.isBlank(), "voce vuota nell'elenco");
        }
    }
}
