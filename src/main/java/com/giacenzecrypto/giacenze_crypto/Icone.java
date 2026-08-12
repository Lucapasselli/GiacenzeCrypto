/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.RootPaneContainer;
import javax.swing.plaf.UIResource;

/**
 *
 * @author lucap
 */
public class Icone {
    public static Icon FrecciaDestra = new FlatSVGIcon("Images/FrecciaDestra.svg", 40, 40);
    public static Icon FrecciaSinistra = new FlatSVGIcon("Images/FrecciaSinistra.svg", 40, 40);
    public static Icon Modifica = new FlatSVGIcon("Images/Modifica.svg", 24, 24);
    public static Icon Carica = new FlatSVGIcon("Images/Carica.svg", 24, 24);
    public static Icon Chiave = new FlatSVGIcon("Images/Chiave.svg", 24, 24);
    public static Icon Catena = new FlatSVGIcon("Images/Catena.svg", 24, 24);
    public static Icon Banana = new FlatSVGIcon("Images/Banana.svg", 24, 24);
    public static Icon Imbuto = new FlatSVGIcon("Images/Imbuto.svg", 24, 24);
    public static Icon Wallet = new FlatSVGIcon("Images/Wallet.svg", 24, 24);
    public static Icon ImbutoX = new FlatSVGIcon("Images/ImbutoX.svg", 24, 24);
    public static Icon Annulla = new FlatSVGIcon("Images/Annulla.svg", 24, 24);
    public static Icon Salva = new FlatSVGIcon("Images/Salva.svg", 24, 24);
    public static Icon Stack = new FlatSVGIcon("Images/Stack.svg", 24, 24);
    public static Icon Euro = new FlatSVGIcon("Images/Euro.svg", 24, 24);
    public static Icon Attenzione = new FlatSVGIcon("Images/Attenzione.svg", 24, 24);
    public static Icon Unlock = new FlatSVGIcon("Images/Unlock.svg", 24, 24);
    public static Icon Cestino = new FlatSVGIcon("Images/Cestino.svg", 24, 24);
    public static Icon AssegnazioneAutomatica = new FlatSVGIcon("Images/AssegnazioneAutomatica.svg", 24, 24);
    
   // public static FlatSVGIcon svgImbuto = new FlatSVGIcon("Images/Imbuto.svg", 12, 12);
    
    /**
     * @param Dimensione larghezza e altezza dell'icona in pixel
     * @return l'icona di alert (SVG) alla dimensione richiesta
     */
    public static Icon getAlert(int Dimensione){
        return new FlatSVGIcon("Images/Alert.svg", Dimensione, Dimensione);
    }

    //====================================================================================
    // Adattamento delle icone al tema scuro
    //
    // Tutte le icone dell'applicazione sono monocromatiche NERE, in due formati e per due motivi
    // diversi: i PNG di /Images sono stati disegnati neri, e gli SVG sono file lucide con
    // stroke="currentColor", che svgSalamander non sa risolvere e rende comunque nero (verificato
    // rasterizzandoli: 100% dei pixel visibili a #000000). Sullo sfondo quasi nero del tema scuro
    // sparirebbero entrambi, quindi vanno ridisegnati in chiaro.
    //
    // Le due famiglie hanno bisogno di due meccanismi distinti:
    //  - SVG: basta il ColorFilter globale di FlatSVGIcon, che agisce al momento del disegno e quindi
    //    vale anche per le icone già costruite (i campi statici qui sopra) e per quelle create dopo
    //    (getAlert). È un solo aggancio per tutta l'applicazione.
    //  - PNG: nessun aggancio equivalente. Le ~144 chiamate a new ImageIcon(getResource(...)) sono
    //    dentro il codice generato da NetBeans e sono descritte anche nei .form, quindi modificarle
    //    non ha senso: verrebbero rigenerate. Si passa invece sull'albero dei componenti a finestra
    //    aperta e si sostituisce l'icona già assegnata.
    //
    // L'adattamento è a senso unico e deciso all'avvio: il cambio tema a caldo avvisa comunque
    // l'utente che occorre riavviare.
    //====================================================================================

    /** Colore con cui vengono ridisegnate le icone monocromatiche quando il tema è scuro. */
    private static final Color COLORE_ICONE_TEMA_SCURO = new Color(0xDCDCDC);

    /** Soglia di luminosità sotto la quale un colore è considerato "scuro" e quindi da schiarire. */
    private static final int SOGLIA_LUMINOSITA = 96;

    /** Colore del testo per i componenti che hanno uno sfondo chiaro tutto loro. */
    private static final Color COLORE_TESTO_SU_SFONDO_CHIARO = new Color(0x333333);

    /** true se le icone vanno schiarite, cioè se all'avvio il tema era quello scuro. */
    private static boolean AdattamentoAttivo = false;

    /**
     * Icone PNG già ridisegnate, indicizzate per URL di origine (la descrizione impostata da
     * ImageIcon(URL)): ogni glifo viene rasterizzato una volta sola anche se compare su più pulsanti.
     */
    private static final Map<String, Icon> CacheRicolorate = new ConcurrentHashMap<>();

    /**
     * Attiva l'adattamento delle icone al tema scuro. Va chiamata una sola volta all'avvio, dopo
     * l'installazione del Look&amp;Feel: registra il filtro colore per gli SVG e un ascoltatore AWT che
     * ripassa le icone PNG di ogni finestra nel momento in cui viene aperta (così non serve una
     * chiamata esplicita nel costruttore di ognuna delle sedici finestre dell'applicazione).
     *
     * @param Scuro true se il tema attivo è quello scuro; con false non viene fatto nulla
     */
    public static void InizializzaTema(boolean Scuro) {
        if (!Scuro || AdattamentoAttivo) return;
        AdattamentoAttivo = true;

        //Gli SVG dell'applicazione sono tutti monocromatici, ma il filtro è globale e verrebbe
        //applicato anche a un eventuale SVG a colori aggiunto in futuro: si schiariscono quindi
        //solo i colori davvero scuri, lasciando intatto tutto il resto.
        FlatSVGIcon.ColorFilter.getInstance().setMapper(c -> Schiarisci(c));

        Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {
            if (evento.getID() == WindowEvent.WINDOW_OPENED && evento instanceof WindowEvent)
                AdattaIconeAlTema(((WindowEvent) evento).getWindow());
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    /**
     * Restituisce la versione chiara di un colore scuro, lasciando invariati i colori già chiari.
     * @param c colore di partenza (può essere null)
     * @return il colore da usare al suo posto
     */
    private static Color Schiarisci(Color c) {
        if (c == null) return null;
        int luminosita = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
        if (luminosita > SOGLIA_LUMINOSITA) return c;
        return new Color(COLORE_ICONE_TEMA_SCURO.getRed(), COLORE_ICONE_TEMA_SCURO.getGreen(),
                COLORE_ICONE_TEMA_SCURO.getBlue(), c.getAlpha());
    }

    /**
     * Restituisce la versione da usare col tema attivo dell'icona passata: con tema chiaro, o per
     * icone che non sono PNG monocromatici dell'applicazione, ritorna l'icona originale.
     * Utile per le icone costruite a runtime fuori dall'albero dei componenti, come quella del filtro
     * negli header delle tabelle.
     *
     * @param ic icona di partenza (può essere null)
     * @return l'icona adattata, oppure la stessa icona se non c'è nulla da fare
     */
    public static Icon Adatta(Icon ic) {
        if (!AdattamentoAttivo || !(ic instanceof ImageIcon)) return ic;
        ImageIcon immagine = (ImageIcon) ic;
        //ImageIcon(URL) usa l'URL come descrizione: è l'unico modo per sapere quale file è, e serve
        //sia come chiave di cache sia per NON toccare i QR code, che un'inversione distruggerebbe.
        String descrizione = immagine.getDescription();
        if (descrizione == null || !descrizione.contains("/Images/")) return ic;
        String nome = descrizione.substring(descrizione.lastIndexOf('/') + 1);
        if (!nome.matches("^\\d+_.*\\.png$")) return ic;
        Icon giaFatta = CacheRicolorate.get(descrizione);
        if (giaFatta != null) return giaFatta;
        Icon nuova = Ridisegna(immagine);
        CacheRicolorate.put(descrizione, nuova);
        return nuova;
    }

    /**
     * Ridisegna un'icona monocromatica conservandone il canale alfa e sostituendo il colore dei pixel
     * scuri: è la trasparenza a disegnare la forma, quindi il risultato resta identico ma in chiaro.
     * @param immagine icona da ridisegnare
     * @return una nuova icona con lo stesso ingombro, che mantiene la descrizione dell'originale
     */
    private static Icon Ridisegna(ImageIcon immagine) {
        int larghezza = Math.max(1, immagine.getIconWidth());
        int altezza = Math.max(1, immagine.getIconHeight());
        BufferedImage destinazione = new BufferedImage(larghezza, altezza, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = destinazione.createGraphics();
        immagine.paintIcon(null, g, 0, 0);
        g.dispose();
        for (int y = 0; y < altezza; y++) {
            for (int x = 0; x < larghezza; x++) {
                int pixel = destinazione.getRGB(x, y);
                int alfa = pixel >>> 24;
                if (alfa == 0) continue;
                Color sostituto = Schiarisci(new Color(pixel, true));
                destinazione.setRGB(x, y, (alfa << 24) | (sostituto.getRGB() & 0xFFFFFF));
            }
        }
        ImageIcon risultato = new ImageIcon(destinazione);
        risultato.setDescription(immagine.getDescription());
        return risultato;
    }

    /**
     * Dice se il componente ha uno sfondo chiaro deciso dal codice e non dal tema.
     *
     * <p>La distinzione si legge dal tipo del colore: quelli che arrivano dal Look&amp;Feel sono
     * {@link UIResource}, quelli scritti a mano nel codice generato dal GUI Builder
     * ({@code setBackground(new java.awt.Color(204, 255, 204))}) no. È l'unico modo affidabile per
     * riconoscere un componente a cui è stato dato un colore proprio, che il tema scuro non cambia.</p>
     *
     * @param c componente da esaminare
     * @return true se lo sfondo è chiaro ed è stato imposto dal codice
     */
    private static boolean SfondoChiaroProprio(Component c) {
        Color sfondo = c.getBackground();
        if (sfondo == null || sfondo instanceof UIResource) return false;
        int luminosita = (sfondo.getRed() * 299 + sfondo.getGreen() * 587 + sfondo.getBlue() * 114) / 1000;
        return luminosita > SOGLIA_LUMINOSITA;
    }

    /**
     * Scurisce il testo di un componente rimasto su sfondo chiaro, ma solo se il colore del testo
     * viene dal tema: se il codice ne ha già scelto uno, quello vince.
     * @param c componente da sistemare
     */
    private static void SistemaTestoSuSfondoChiaro(Component c) {
        if (c.getForeground() instanceof UIResource) c.setForeground(COLORE_TESTO_SU_SFONDO_CHIARO);
    }

    /**
     * Sostituisce, in tutto il sottoalbero passato, le icone PNG monocromatiche con la loro versione
     * chiara. Con tema chiaro non fa nulla.
     *
     * <p>Oltre ai figli normali scende anche nella barra dei menu e nei menu a discesa, che non sono
     * figli del content pane e verrebbero altrimenti saltati. I JPopupMenu tenuti come campo (quello
     * dei movimenti in {@code Principale} e quello di {@code GUI_DettaglioTransazione}) non sono
     * raggiungibili da nessun albero finché non vengono mostrati: vanno passati esplicitamente.</p>
     *
     * @param radice componente da cui partire (può essere null)
     */
    public static void AdattaIconeAlTema(Component radice) {
        if (!AdattamentoAttivo || radice == null) return;

        //Alcuni pulsanti e alcune etichette hanno uno sfondo chiaro scritto nel codice generato dal
        //GUI Builder e restano chiari anche col tema scuro: lì l'icona nera è quella giusta, e a
        //schiarirla si otterrebbe solo di farla sparire.
        if (SfondoChiaroProprio(radice)) {
            SistemaTestoSuSfondoChiaro(radice);
        } else if (radice instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) radice;
            b.setIcon(Adatta(b.getIcon()));
            b.setPressedIcon(Adatta(b.getPressedIcon()));
            b.setSelectedIcon(Adatta(b.getSelectedIcon()));
            b.setRolloverIcon(Adatta(b.getRolloverIcon()));
            //getDisabledIcon() non va letto: se è null Swing ne genera uno al volo dall'icona
            //normale, e assegnarlo lo congelerebbe sulla versione nera.
        } else if (radice instanceof JLabel) {
            JLabel l = (JLabel) radice;
            l.setIcon(Adatta(l.getIcon()));
            l.setDisabledIcon(null);
        }

        if (radice instanceof JTabbedPane) {
            JTabbedPane schede = (JTabbedPane) radice;
            for (int i = 0; i < schede.getTabCount(); i++) schede.setIconAt(i, Adatta(schede.getIconAt(i)));
        }

        if (radice instanceof RootPaneContainer) {
            Container contenitore = ((RootPaneContainer) radice).getContentPane();
            AdattaIconeAlTema(contenitore);
            JMenuBar barra = ((RootPaneContainer) radice).getRootPane().getJMenuBar();
            if (barra != null) AdattaIconeAlTema(barra);
        }

        //I menu a discesa vivono nel loro JPopupMenu, che non compare tra i getComponents() del JMenu
        if (radice instanceof JMenu) AdattaIconeAlTema(((JMenu) radice).getPopupMenu());

        if (radice instanceof Container) {
            for (Component figlio : ((Container) radice).getComponents()) AdattaIconeAlTema(figlio);
        }
    }
}
