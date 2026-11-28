package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Il tema "Blu" è una variante chiara con il suo file di valori. Due cose da non rompere: i suoi valori
 * arrivano davvero (la registrazione di {@code Temi} e il nome del file devono combaciare con la classe) e
 * <b>non entrano nel tema "Chiaro" di serie</b>, che condivide la stessa registrazione.
 */
class FlatTemaBluLafTest {

    @BeforeAll
    static void registra() {
        // Come in Giacenze_Crypto.main: una volta sola, prima di installare il Look&Feel
        FlatLaf.registerCustomDefaultsSource("Temi");
    }

    @AfterEach
    void ripristina() throws Exception {
        FlatTemaBluLaf.RipristinaColoriTabelle();
        UIManager.setLookAndFeel(new FlatLightLaf());
    }

    @Test
    void iValoriDelTemaBluArrivanoDalSuoFile() throws Exception {
        UIManager.setLookAndFeel(new FlatTemaBluLaf());
        // Lo sfondo della fascia e' quello della pagina, il blu notte e' delle singole schede (FlatTabbedPaneBluUI)
        assertEquals(new Color(0xF1F5FA), UIManager.getColor("TabbedPane.background"));
        assertEquals(new Color(0x12243A), UIManager.getColor("Giacenze.tabSfondo"));
        assertEquals(new Color(0x2667BC), UIManager.getColor("TabbedPane.selectedBackground"));
        assertEquals(Color.WHITE, UIManager.getColor("TabbedPane.selectedForeground"));
        assertEquals(new Color(0xF1F5FA), UIManager.getColor("Panel.background"));
        assertEquals(new Color(0x12243A), UIManager.getColor("Label.foreground"));
        // L'accento è fisso: senza, su macOS e Windows prevarrebbe quello del sistema
        assertEquals(new Color(0x2667BC), UIManager.getColor("Button.default.background"));
        assertEquals(new Color(0x2667BC), UIManager.getColor("Component.focusColor"));
    }

    @Test
    void ilTemaChiaroDiSerieNonCambia() throws Exception {
        UIManager.setLookAndFeel(new FlatTemaBluLaf());
        UIManager.setLookAndFeel(new FlatLightLaf());
        assertNotEquals(new Color(0xF1F5FA), UIManager.getColor("Panel.background"));
        assertNotEquals(new Color(0x12243A), UIManager.getColor("Giacenze.tabSfondo"));
        assertNotEquals(new Color(0x2667BC), UIManager.getColor("TabbedPane.selectedBackground"));
    }

    @Test
    void ilTemaBluNonEScuro() {
        assertFalse(new FlatTemaBluLaf().isDark(), "icone nere e colori delle righe dipendono da questo");
        assertTrue(FlatTemaBluLaf.is("Blu"));
        assertTrue(FlatTemaBluLaf.is("blu"));
        assertFalse(FlatTemaBluLaf.is("Scuro"));
        assertFalse(FlatTemaBluLaf.is("Chiaro"));
        assertFalse(FlatTemaBluLaf.is(null));
        // Per il resto del programma vale come il chiaro: il confronto è sempre e solo con "Scuro"
        assertFalse("Blu".equalsIgnoreCase("Scuro"));
    }

    @Test
    void icoloriDelleTabelleSiRipristinano() {
        Color riga = Tabelle.grigioChiaro, sel = Tabelle.selezione, selAlt = Tabelle.selezioneAlt;
        FlatTemaBluLaf.ImpostaColoriTabelle();
        assertNotEquals(riga, Tabelle.grigioChiaro);
        assertEquals(new Color(38, 103, 188), Tabelle.selezione);
        // Una seconda chiamata non deve far perdere i valori di partenza
        FlatTemaBluLaf.ImpostaColoriTabelle();
        FlatTemaBluLaf.RipristinaColoriTabelle();
        assertEquals(riga, Tabelle.grigioChiaro);
        assertEquals(sel, Tabelle.selezione);
        assertEquals(selAlt, Tabelle.selezioneAlt);
    }

    @Test
    void ilPulsanteDelTitoloSegueIlTemaSoloSeIlTemaLoDichiara() throws Exception {
        // Principale legge Giacenze.titoloSfondo/Testo: il Blu e lo Scuro li dichiarano, il Chiaro no (restano i colori del form)
        for (String file : new String[] {"/Temi/FlatTemaBluLaf.properties", "/Temi/FlatDarkLaf.properties"}) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.InputStream in = FlatTemaBluLafTest.class.getResourceAsStream(file)) {
                p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
            assertTrue(p.containsKey("Giacenze.titoloSfondo"), file);
            assertTrue(p.containsKey("Giacenze.titoloTesto"), file);
        }
        assertEquals(null, FlatTemaBluLafTest.class.getResource("/Temi/FlatLightLaf.properties"));
    }

    @Test
    void leSchedeDelBluSonoPastiglieStaccateEQuelleDelChiaroNo() throws Exception {
        UIManager.setLookAndFeel(new FlatTemaBluLaf());
        assertTrue(new javax.swing.JTabbedPane().getUI() instanceof FlatTabbedPaneBluUI);
        UIManager.setLookAndFeel(new FlatLightLaf());
        assertFalse(new javax.swing.JTabbedPane().getUI() instanceof FlatTabbedPaneBluUI, "il Chiaro di serie non cambia");
    }
}
