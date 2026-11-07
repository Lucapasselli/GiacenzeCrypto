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
        //Le lettere accentate il Designer le scrive come entità numeriche ("quantit&#xe0;")
        v = Pattern.compile("&#x([0-9a-fA-F]+);").matcher(v)
                .replaceAll(r -> String.valueOf((char) Integer.parseInt(r.group(1), 16)));
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
    public void leVociDelleCommissioniCollegateEsistonoNelMenu() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(FORM),
                "Principale.form non trovato: test eseguito fuori dalla radice del progetto");
        Set<String> etichette = etichetteDelPopupDeiMovimenti();
        //Abilitate e disabilitate per testo da Funzioni.PopUpMenu: un testo diverso non darebbe errori,
        //la voce resterebbe semplicemente sempre attiva
        assertTrue(etichette.contains(Funzioni.POPUP_VOCE_COLLEGA_COMMISSIONI), etichette.toString());
        assertTrue(etichette.contains(Funzioni.POPUP_VOCE_SCOLLEGA_COMMISSIONI), etichette.toString());
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

    @Test
    public void leVociDentroUnaCategoriaSiAbilitanoPerTesto() {
        //Le voci sono raggruppate in sottomenu: la ricerca per testo deve scendere fino a loro, anche
        //dentro un sottomenu annidato come "Chiedi all'IA" dentro "Analizza"
        JPopupMenu pop = new JPopupMenu();
        JMenu analizza = new JMenu("Analizza");
        JMenuItem lifo = new JMenuItem("Mostra LiFo Transazione");
        JMenu chiediIA = new JMenu("Chiedi all'IA");
        JMenuItem bot = new JMenuItem("Un chatbot");
        chiediIA.add(bot);
        analizza.add(lifo);
        analizza.add(chiediIA);
        pop.add(analizza);

        Funzioni.PopUp_disabilitaMenuDatesto(pop, "Mostra LiFo Transazione");
        Funzioni.PopUp_disabilitaMenuDatesto(pop, "Chiedi all'IA");
        assertFalse(lifo.isEnabled());
        assertFalse(chiediIA.isEnabled());
        Funzioni.PopUp_abilitaMenuDaTesto(pop, "Mostra LiFo Transazione");
        assertTrue(lifo.isEnabled());
    }

    @Test
    public void unaCategoriaSenzaVociAttiveSiIngrigisceESiRiaccende() {
        JPopupMenu pop = new JPopupMenu();
        JMenu analizza = new JMenu("Analizza");
        JMenuItem lifo = new JMenuItem("Mostra LiFo Transazione");
        JMenu chiediIA = new JMenu("Chiedi all'IA");
        chiediIA.add(new JMenuItem("Un chatbot"));
        analizza.add(lifo);
        analizza.add(chiediIA);
        pop.add(analizza);

        lifo.setEnabled(false);
        chiediIA.setEnabled(false);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertFalse(analizza.isEnabled(), "una categoria con tutte le voci grigie va ingrigita");
        //Il sottomenu dei chatbot non è una categoria: le sue voci attive non devono riaccenderlo
        assertFalse(chiediIA.isEnabled(), "Chiedi all'IA disattivato per testo deve restare grigio");

        lifo.setEnabled(true);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertTrue(analizza.isEnabled(), "la categoria va riaccesa appena una sua voce torna attiva");
    }

    @Test
    public void iSeparatoriOrfaniSiNascondono() {
        //Su un campo di testo "Seleziona tutte le righe visibili" ed "Esporta Tabella in Excel" sono nascosti:
        //il separatore che li precedeva resterebbe in fondo al menu
        JPopupMenu pop = new JPopupMenu();
        JPopupMenu.Separator iniziale = new JPopupMenu.Separator();
        JMenuItem copia = new JMenuItem("Copia selezione");
        JPopupMenu.Separator doppio1 = new JPopupMenu.Separator();
        JMenuItem nascosta = new JMenuItem("Seleziona tutte le righe visibili");
        JPopupMenu.Separator doppio2 = new JPopupMenu.Separator();
        JMenuItem incolla = new JMenuItem("Incolla");
        JPopupMenu.Separator finale = new JPopupMenu.Separator();
        JMenuItem esporta = new JMenuItem("Esporta Tabella in Excel");
        pop.add(iniziale);
        pop.add(copia);
        pop.add(doppio1);
        pop.add(nascosta);
        pop.add(doppio2);
        pop.add(incolla);
        pop.add(finale);
        pop.add(esporta);

        Funzioni.PopUp_mostraMenuDaTesto(pop, "Seleziona tutte le righe visibili", false);
        Funzioni.PopUp_mostraMenuDaTesto(pop, "Esporta Tabella in Excel", false);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertFalse(iniziale.isVisible(), "separatore in testa al menu");
        assertTrue(doppio1.isVisible() != doppio2.isVisible(), "di due separatori accostati se ne vede uno");
        assertFalse(finale.isVisible(), "separatore in fondo al menu");

        //Tornando su una tabella le voci ricompaiono e con loro i separatori
        Funzioni.PopUp_mostraMenuDaTesto(pop, "Seleziona tutte le righe visibili", true);
        Funzioni.PopUp_mostraMenuDaTesto(pop, "Esporta Tabella in Excel", true);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertTrue(doppio1.isVisible() && doppio2.isVisible() && finale.isVisible());
        assertFalse(iniziale.isVisible());
    }

    @Test
    public void alPrimoLivelloDelMenuCiSonoSoloCategorieConVoci() throws IOException {
        //PopUp_SistemaCategorieESeparatori tratta ogni sottomenu di primo livello come una categoria e lo
        //accende se ha una voce attiva: un sottomenu riempito da codice come "Chiedi all'IA" messo al primo
        //livello verrebbe riacceso anche quando la sua disattivazione per testo lo vuole grigio
        Assumptions.assumeTrue(Files.isRegularFile(FORM),
                "Principale.form non trovato: test eseguito fuori dalla radice del progetto");
        String form = Files.readString(FORM, StandardCharsets.UTF_8);
        int inizio = form.indexOf("<Container class=\"javax.swing.JPopupMenu\" name=\"PopupMenu\">");
        String blocco = form.substring(inizio, form.indexOf("</Container>", inizio));
        Matcher m = Pattern.compile("(?ms)^        <Menu class=\"javax\\.swing\\.JMenu\" name=\"(\\w+)\">\\n(.*?)^        </Menu>")
                .matcher(blocco);
        int categorie = 0;
        while (m.find()) {
            categorie++;
            assertTrue(m.group(2).contains("<SubComponents>"),
                    "il sottomenu " + m.group(1) + " è al primo livello ma non ha voci nel form: va messo dentro una categoria");
        }
        assertTrue(categorie > 0, "il menu contestuale non ha più categorie: il test non controlla niente");
    }

    @Test
    public void ogniVoceDelMenuEDiModificaODiConsultazione() throws IOException {
        //I profili ridotti nascondono le voci di POPUP_VOCI_MODIFICA: una voce nuova che modifica, non elencata,
        //comparirebbe anche nella classificazione. Ogni voce del form deve stare in uno solo dei due elenchi
        Assumptions.assumeTrue(Files.isRegularFile(FORM),
                "Principale.form non trovato: test eseguito fuori dalla radice del progetto");
        String form = Files.readString(FORM, StandardCharsets.UTF_8);
        int inizio = form.indexOf("<Container class=\"javax.swing.JPopupMenu\" name=\"PopupMenu\">");
        String blocco = form.substring(inizio, form.indexOf("</Container>", inizio));
        Matcher m = Pattern.compile("(?m)^( *)<(MenuItem|Menu) class=\"javax\\.swing\\.J(?:MenuItem|Menu)\" name=\"\\w+\">")
                .matcher(blocco);
        Pattern testo = Pattern.compile("<Property name=\"text\" type=\"java\\.lang\\.String\" value=\"([^\"]*)\"");
        List<String> nonClassificate = new ArrayList<>();
        List<String> doppie = new ArrayList<>();
        int voci = 0;
        while (m.find()) {
            //I sottomenu di primo livello sono categorie, non voci
            if (m.group(1).length() == 8 && m.group(2).equals("Menu")) continue;
            Matcher t = testo.matcher(blocco);
            assertTrue(t.find(m.end()), "voce senza testo nel form");
            String voce = deXml(t.group(1));
            voci++;
            boolean modifica = Funzioni.POPUP_VOCI_MODIFICA.stream().anyMatch(voce::equalsIgnoreCase);
            boolean consultazione = Funzioni.POPUP_VOCI_CONSULTAZIONE.stream().anyMatch(voce::equalsIgnoreCase);
            if (!modifica && !consultazione) nonClassificate.add(voce);
            if (modifica && consultazione) doppie.add(voce);
        }
        assertTrue(voci > 20, "trovate solo " + voci + " voci: il test non legge più il form");
        assertTrue(nonClassificate.isEmpty(), "voci né di modifica né di consultazione: " + nonClassificate);
        assertTrue(doppie.isEmpty(), "voci in tutti e due gli elenchi: " + doppie);
    }

    /** Menu con la forma di quello vero: due voci in alto, due categorie, Chiedi all'IA dentro la prima. */
    private static JPopupMenu menuConCategorie() {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new JMenuItem("Dettagli Movimento"));
        pop.add(new JMenuItem("Modifica Movimento"));
        pop.add(new JPopupMenu.Separator());
        JMenu analizza = new JMenu("Analizza");
        analizza.add(new JMenuItem("Mostra LiFo Transazione"));
        JMenu chiediIA = new JMenu("Chiedi all'IA");
        chiediIA.add(new JMenuItem("Un chatbot"));
        analizza.add(chiediIA);
        pop.add(analizza);
        JMenu prezzi = new JMenu("Prezzi");
        prezzi.add(new JMenuItem("Modifica Prezzo"));
        prezzi.add(new JMenuItem(Funzioni.POPUP_VOCE_RICALCOLA_PREZZI));
        pop.add(prezzi);
        JMenu separa = new JMenu("Separa e unisci");
        separa.add(new JMenuItem("Separa in Deposito/Prelievo"));
        pop.add(separa);
        JMenu altre = new JMenu("Altre modifiche");
        altre.add(new JMenuItem("Modifica Note"));
        altre.add(new JMenuItem("Trasla Orario"));
        pop.add(altre);
        pop.add(new JPopupMenu.Separator());
        pop.add(new JMenuItem("Copia ID Transazione"));
        return pop;
    }

    private static JMenuItem voce(JPopupMenu pop, String testo) {
        return vocePerTesto(pop, testo);
    }

    @Test
    public void ilProfiloDiConsultazioneNascondeLeModificheELeCategorieVuote() {
        JPopupMenu pop = menuConCategorie();
        Funzioni.PopUp_ApplicaProfilo(pop, Funzioni.ProfiloMenuMovimenti.CONSULTAZIONE);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertTrue(voce(pop, "Dettagli Movimento").isVisible());
        assertFalse(voce(pop, "Modifica Movimento").isVisible());
        assertTrue(voce(pop, "Analizza").isVisible());
        assertTrue(voce(pop, "Chiedi all'IA").isVisible());
        assertTrue(voce(pop, "Un chatbot").isVisible(), "i chatbot non sono voci di modifica e devono restare");
        assertFalse(voce(pop, "Prezzi").isVisible(), "categoria con sole voci di modifica");
        assertFalse(voce(pop, "Separa e unisci").isVisible());
        assertFalse(voce(pop, "Altre modifiche").isVisible());
        assertTrue(voce(pop, "Copia ID Transazione").isVisible());

        //Riaperto dalla finestra principale torna tutto
        Funzioni.PopUp_ApplicaProfilo(pop, Funzioni.ProfiloMenuMovimenti.COMPLETO);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        for (String v : List.of("Modifica Movimento", "Prezzi", "Modifica Prezzo", "Separa e unisci", "Altre modifiche", "Trasla Orario")) {
            assertTrue(voce(pop, v).isVisible(), v);
        }
    }

    @Test
    public void ilProfiloDelDettaglioTienePrezzoENoteMaNonIlDettaglio() {
        JPopupMenu pop = menuConCategorie();
        Funzioni.PopUp_ApplicaProfilo(pop, Funzioni.ProfiloMenuMovimenti.DETTAGLIO_MOVIMENTO);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertFalse(voce(pop, "Dettagli Movimento").isVisible(), "il dettaglio non riapre se stesso");
        assertFalse(voce(pop, "Modifica Movimento").isVisible());
        assertTrue(voce(pop, "Prezzi").isVisible());
        assertTrue(voce(pop, "Modifica Prezzo").isVisible());
        assertFalse(voce(pop, Funzioni.POPUP_VOCE_RICALCOLA_PREZZI).isVisible());
        assertTrue(voce(pop, "Altre modifiche").isVisible());
        assertTrue(voce(pop, "Modifica Note").isVisible());
        assertFalse(voce(pop, "Trasla Orario").isVisible());
        assertFalse(voce(pop, "Separa e unisci").isVisible());
    }

    @Test
    public void ilDettaglioApertoInConsultazioneNonLasciaModificare() {
        //Dalla classificazione (consultazione) "Dettagli Movimento" apriva un dettaglio da cui si poteva modificare:
        //il dettaglio aperto da lì è in sola lettura, e il suo menu non ha nessuna voce di modifica
        assertTrue(Funzioni.ProfiloMenuMovimenti.CONSULTAZIONE.DettaglioInSolaLettura());
        assertTrue(Funzioni.ProfiloMenuMovimenti.DETTAGLIO_SOLA_LETTURA.DettaglioInSolaLettura());
        assertFalse(Funzioni.ProfiloMenuMovimenti.COMPLETO.DettaglioInSolaLettura());
        assertFalse(Funzioni.ProfiloMenuMovimenti.DETTAGLIO_MOVIMENTO.DettaglioInSolaLettura());
        for (String voce : Funzioni.POPUP_VOCI_MODIFICA) {
            assertFalse(Funzioni.ProfiloMenuMovimenti.DETTAGLIO_SOLA_LETTURA.Mostra(voce), voce);
        }
        assertFalse(Funzioni.ProfiloMenuMovimenti.DETTAGLIO_SOLA_LETTURA.Mostra("Dettagli Movimento"));
        assertTrue(Funzioni.ProfiloMenuMovimenti.DETTAGLIO_SOLA_LETTURA.Mostra("Mostra LiFo Transazione"));

        JPopupMenu pop = menuConCategorie();
        Funzioni.PopUp_ApplicaProfilo(pop, Funzioni.ProfiloMenuMovimenti.DETTAGLIO_SOLA_LETTURA);
        Funzioni.PopUp_SistemaCategorieESeparatori(pop);
        assertTrue(voce(pop, "Analizza").isVisible());
        assertFalse(voce(pop, "Prezzi").isVisible());
        assertFalse(voce(pop, "Altre modifiche").isVisible());
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
