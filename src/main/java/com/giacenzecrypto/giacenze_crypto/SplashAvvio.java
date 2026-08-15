package com.giacenzecrypto.giacenze_crypto;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * Splash screen di avvio dell'applicazione, con barra di avanzamento delle fasi di caricamento.
 * <p>
 * È volutamente una classe autonoma e senza dipendenze dal resto del programma: viene invocata come
 * primissima operazione di {@link Giacenze_Crypto#main(String[])}, prima dell'apertura del database e
 * prima dell'installazione del Look&amp;Feel, in modo da mostrare qualcosa a video il prima possibile.
 * Per lo stesso motivo non tocca {@link Principale} (caricare quella classe significherebbe caricare
 * anche tutte le sue dipendenze prima di disegnare il primo pixel). Le uniche dipendenze ammesse sono
 * {@link VarStatiche}, che contiene solo percorsi, e {@link FontApplicazione}, che carica il font dal
 * jar: entrambe non trascinano dietro nient'altro.
 * <p>
 * A monte di questa finestra c'è lo splash nativo AWT ({@code SplashScreen-Image} nel manifest del jar),
 * che viene disegnato dalla JVM prima ancora del caricamento della classe main: {@link #mostra()} lo
 * chiude solo dopo che questa finestra è visibile, così la transizione non lascia lo schermo vuoto.
 *
 * <h2>Perché il disegno non passa dall'EDT</h2>
 * Quasi tutto l'avvio (il costruttore di {@link Principale}, cioè {@code initComponents()}, la lettura
 * dei movimenti, il motore plusvalenze e la costruzione delle tabelle) gira <b>sull'EDT</b>: finché non
 * finisce, nessun {@code repaint()} viene servito. Una barra aggiornata con i normali meccanismi Swing
 * resterebbe quindi ferma a 0 e salterebbe a 100 alla fine, cioè proprio nei secondi in cui deve dire
 * qualcosa. Per questo il contenuto viene disegnato in un {@link BufferedImage} e riversato a video con
 * {@code finestra.getGraphics().drawImage(...)} <b>dal thread che sta caricando</b>, senza passare dalla
 * coda degli eventi. Verificato che funziona anche con {@code setOpacity} e {@code setShape} attivi.
 * {@code paintComponent} ridisegna la stessa immagine, così gli eventi di riesposizione gestiti
 * dall'EDT (quando è libero) non mostrano mai contenuto stantio.
 * <p>
 * Per lo stesso motivo il logo <b>non è più animato</b>: la dissolvenza era pilotata da un
 * {@code javax.swing.Timer}, quindi restava congelata esattamente durante le fasi lente.
 *
 * <h2>Da dove vengono i pesi della barra</h2>
 * Le fasi hanno durate molto diverse fra loro e soprattutto fra installazioni diverse (un archivio con
 * 100.000 movimenti non ha nulla a che vedere con un'installazione nuova). Ogni fase ha quindi un peso
 * <i>di default</i> misurato in sviluppo, che viene però sostituito dai tempi realmente misurati
 * nell'avvio precedente, salvati in {@code avvio.tempi.db} nella directory di lavoro. Dentro le tre fasi
 * che scalano col numero di movimenti la barra avanza anche <i>durante</i> la fase, via
 * {@link #avanzamentoFase(double)}.
 */
public final class SplashAvvio {

    /** Raggio degli angoli arrotondati della finestra. */
    static final int ARCO = 24;
    /** Opacità della finestra: applicata da Swing a runtime, va replicata a mano nel rendering offscreen. */
    static final float OPACITA = 0.97f;
    /** Font usato per il testo della barra di caricamento: esplicito perché il L&amp;F non è ancora installato. */
    private static final Font FONT_BARRA = fontBarra();

    /**
     * Il font incluso nel jar, registrandolo se qualcun altro non l'ha già fatto. La registrazione è
     * idempotente, e serve perché questa classe viene usata anche da {@link GeneraSplash}, che genera le
     * immagini dello splash nativo fuori dall'applicazione: le due strade devono disegnare lo stesso
     * testo con lo stesso font, altrimenti il passaggio dallo splash nativo alla finestra si vede.
     */
    private static Font fontBarra() {
        FontApplicazione.Registra();
        return new Font(FontApplicazione.FAMIGLIA, Font.BOLD, 12);
    }

    /** Geometria dello splash: fissa, perché l'immagine dello splash nativo deve combaciare al pixel. */
    private static final int BORDO = 12;
    private static final int LOGO_L = 300;
    private static final int LOGO_A = 280;
    private static final int BARRA_L = 220;
    private static final int BARRA_A = 30;
    private static final int SPAZIO_BARRA = 10;
    static final int LARGHEZZA = LOGO_L + BORDO * 2;
    static final int ALTEZZA = LOGO_A + SPAZIO_BARRA + BARRA_A + BORDO * 2;
    private static final int BARRA_X = (LARGHEZZA - BARRA_L) / 2;
    private static final int BARRA_Y = BORDO + LOGO_A + SPAZIO_BARRA;

    private static final Color COLORE_SFONDO = new Color(10, 10, 10, 225);
    private static final Color COLORE_BORDO = new Color(255, 255, 255, 55);
    private static final Color COLORE_BARRA = new Color(30, 30, 30, 210);
    private static final Color COLORE_BARRA_BORDO = new Color(255, 255, 255, 45);
    /** Verde oliva del logo: la barra si riempie del colore dell'immagine che le sta sopra. */
    private static final Color COLORE_AVANZAMENTO = new Color(168, 168, 52);

    /**
     * Le fasi dell'avvio, <b>in ordine di esecuzione</b>, con la loro etichetta e il peso di default in
     * millisecondi. I pesi di default vengono da una misura su un archivio da ~102.000 movimenti (vedi
     * {@code test/Documentazione/Analisi_Performance_Caricamento.md}); dal secondo avvio in poi contano
     * invece i tempi reali dell'avvio precedente, letti da {@code avvio.tempi.db}.
     */
    public enum Fase {
        DATABASE("Apertura del database...", 700),
        AMBIENTE("Preparazione dell'ambiente...", 950),
        INTERFACCIA("Costruzione dell'interfaccia...", 2100),
        IMPOSTAZIONI("Lettura delle impostazioni...", 1300),
        MOVIMENTI("Caricamento dei movimenti...", 800),
        PLUSVALENZE("Calcolo delle plusvalenze...", 3100),
        TABELLE("Costruzione delle tabelle...", 1400),
        FINE("Pronto", 0);

        final String etichetta;
        final int pesoDefault;

        Fase(String etichetta, int pesoDefault) {
            this.etichetta = etichetta;
            this.pesoDefault = pesoDefault;
        }
    }

    /** Peso di ogni fase in ms: default, poi sovrascritto dai tempi dell'avvio precedente. */
    private static final EnumMap<Fase, Long> PESI = new EnumMap<>(Fase.class);
    /** Durata realmente misurata di ogni fase dell'avvio in corso, salvata alla chiusura. */
    private static final EnumMap<Fase, Long> DURATE = new EnumMap<>(Fase.class);

    /**
     * Vero quando la finestra è a video. Letto dai chiamanti dentro cicli su tutti i movimenti, quindi
     * deve costare quanto la lettura di un campo: nessun lock, nessuna chiamata a Swing.
     */
    private static volatile boolean attivo = false;

    private static JWindow finestra;
    private static BufferedImage sfondo;
    private static BufferedImage fotogramma;
    /**
     * Fattore di scala dello schermo. Le due immagini di appoggio si allocano a risoluzione del
     * dispositivo e non a quella logica: su un monitor HiDPI un'immagine 1x riversata a video verrebbe
     * ingrandita dalla pipeline grafica, e logo e testo risulterebbero sfocati.
     */
    private static double scala = 1.0;
    private static volatile String testo = "Caricamento in corso...";
    private static volatile double frazione = 0;
    /** Larghezza in pixel del riempimento disegnato per ultimo: evita di ridisegnare per nulla. */
    private static int ultimaLarghezza = -1;
    private static String ultimoTesto = null;

    private static Fase faseCorrente = null;
    private static long inizioFase = 0;
    private static boolean pesiCaricati = false;
    private static boolean tempiAttendibili = true;

    private SplashAvvio() {
    }

    /**
     * Costruisce e mostra lo splash screen, quindi chiude lo splash nativo della JVM.
     * Può essere chiamato da qualunque thread; se chiamato fuori dall'EDT attende che la finestra sia
     * effettivamente visibile. Qualsiasi errore viene ignorato: uno splash non funzionante non deve
     * mai impedire l'avvio del programma.
     */
    public static void mostra() {
        esegui(() -> {
            if (finestra != null) {
                return;
            }
            //Il riferimento allo splash nativo va preso PRIMA di mostrare la finestra Swing: AWT chiude
            //da solo lo splash nativo quando compare la prima finestra, e da quel momento
            //getSplashScreen() restituisce null.
            SplashScreen nativo = splashNativo();
            System.out.println("Splash nativo: " + (nativo != null ? "presente" : "assente"));
            costruisci();
            chiudiSplashNativo(nativo);
        });
    }

    /** @return {@code true} se lo splash è a video, cioè se ha senso segnalargli un avanzamento. */
    public static boolean attivo() {
        return attivo;
    }

    /**
     * Dichiara l'inizio di una fase dell'avvio: aggiorna l'etichetta e porta la barra al punto di
     * partenza della fase. Registra anche la durata effettiva della fase precedente, che a fine avvio
     * viene salvata su disco e diventa il peso della fase al prossimo avvio.
     * <p>Le fasi vanno dichiarate nell'ordine dell'enum: la barra non torna mai indietro.
     * @param nuova fase che sta per iniziare
     */
    public static void fase(Fase nuova) {
        if (!attivo) {
            return;
        }
        try {
            long adesso = System.currentTimeMillis();
            if (faseCorrente != null) {
                long durata = adesso - inizioFase;
                DURATE.put(faseCorrente, durata);
                System.out.println("Avvio - " + faseCorrente + " : " + durata + " ms");
            }
            faseCorrente = nuova;
            inizioFase = adesso;
            testo = nuova.etichetta;
            frazione = frazioneInizio(nuova);
            disegnaEMostra();
        } catch (Exception | Error ex) {
            System.err.println("Splash: fase non aggiornata: " + ex);
        }
    }

    /**
     * Avanzamento <i>dentro</i> la fase corrente, per le fasi che scorrono tutti i movimenti e che
     * altrimenti lascerebbero la barra ferma per secondi.
     * <p>È pensato per essere chiamato dentro un ciclo: il disegno avviene solo quando il riempimento
     * cambia di almeno un pixel, quindi al massimo un paio di centinaia di volte in tutto l'avvio.
     * @param frazioneFase quanto della fase è stato completato, da 0 a 1
     */
    public static void avanzamentoFase(double frazioneFase) {
        if (!attivo || faseCorrente == null) {
            return;
        }
        try {
            double inizio = frazioneInizio(faseCorrente);
            double fine = frazioneFine(faseCorrente);
            double f = inizio + Math.max(0, Math.min(1, frazioneFase)) * (fine - inizio);
            //Mai indietro: una fase può essere segnalata con denominatori diversi (passata completa o
            //incrementale) e un arretramento della barra sarebbe l'unica cosa che l'utente nota.
            if (f > frazione) {
                frazione = f;
                disegnaEMostra();
            }
        } catch (Exception | Error ex) {
            System.err.println("Splash: avanzamento non aggiornato: " + ex);
        }
    }

    /**
     * Dichiara che questo avvio non è rappresentativo e i suoi tempi non vanno salvati.
     * <p>Serve all'avvio dopo un cambio di versione: lì il programma riscrive l'intero file dei
     * movimenti e apre una finestra di attesa, quindi due fasi durano molto più del normale. Siccome è
     * il primo avvio dopo <em>ogni</em> aggiornamento, senza questa esclusione la barra risulterebbe
     * mal tarata proprio all'avvio successivo a ogni nuova versione.
     */
    public static void tempiNonAttendibili() {
        tempiAttendibili = false;
    }

    /**
     * Chiude lo splash screen. Può essere chiamato più volte e da qualunque thread; va invocato prima
     * di qualsiasi dialogo di errore in fase di avvio, altrimenti lo splash resterebbe sospeso sopra
     * la finestra di dialogo.
     */
    public static void chiudi() {
        boolean avvioCompletato = (faseCorrente == Fase.FINE);
        attivo = false;
        esegui(() -> {
            if (finestra != null) {
                finestra.setVisible(false);
                finestra.dispose();
                finestra = null;
                sfondo = null;
                fotogramma = null;
            }
            //Se lo splash Swing non è mai stato creato (immagine mancante) lo splash nativo sarebbe
            //rimasto a video: lo chiudo comunque.
            chiudiSplashNativo(splashNativo());
        });
        //Solo un avvio arrivato in fondo descrive quanto costano davvero le fasi: se lo splash è stato
        //chiuso per mostrare un errore, i tempi parziali falserebbero la barra del prossimo avvio.
        if (avvioCompletato && tempiAttendibili) {
            salvaTempi();
        }
        faseCorrente = null;
    }

    /** Esegue l'operazione sull'EDT, in modo sincrono e ignorando ogni errore. */
    private static void esegui(Runnable operazione) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                operazione.run();
            } else {
                SwingUtilities.invokeAndWait(operazione);
            }
        } catch (Exception ex) {
            System.err.println("Splash screen non disponibile: " + ex);
        } catch (Error err) {
            System.err.println("Splash screen non disponibile: " + err);
        }
    }

    /** @return lo splash nativo della JVM, o {@code null} se non è stato mostrato (es. avvio da IDE, senza jar). */
    private static SplashScreen splashNativo() {
        try {
            return SplashScreen.getSplashScreen();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Chiude lo splash nativo della JVM, se presente e non già chiuso da AWT. */
    private static void chiudiSplashNativo(SplashScreen nativo) {
        try {
            if (nativo != null && nativo.isVisible()) {
                nativo.close();
            }
        } catch (Exception ex) {
            //Splash nativo già chiuso da AWT alla comparsa della prima finestra: irrilevante.
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Pesi delle fasi
    // ---------------------------------------------------------------------------------------------

    /** Somma dei pesi di tutte le fasi; mai zero, così le divisioni sono sempre lecite. */
    private static long pesoTotale() {
        caricaPesi();
        long totale = 0;
        for (Fase f : Fase.values()) {
            totale += PESI.getOrDefault(f, (long) f.pesoDefault);
        }
        return Math.max(1, totale);
    }

    /** @return la frazione di barra a cui la fase indicata inizia */
    private static double frazioneInizio(Fase fase) {
        long totale = pesoTotale();
        long prima = 0;
        for (Fase f : Fase.values()) {
            if (f == fase) {
                break;
            }
            prima += PESI.getOrDefault(f, (long) f.pesoDefault);
        }
        return (double) prima / totale;
    }

    /** @return la frazione di barra a cui la fase indicata si conclude */
    private static double frazioneFine(Fase fase) {
        return frazioneInizio(fase) + (double) PESI.getOrDefault(fase, (long) fase.pesoDefault) / pesoTotale();
    }

    /**
     * Legge i tempi dell'avvio precedente da {@code avvio.tempi.db}. Va chiamata quando la directory di
     * lavoro è già nota (cioè dopo la lettura degli argomenti): la prima {@link Fase} dichiarata arriva
     * comunque dopo. In mancanza del file restano i pesi di default.
     */
    private static void caricaPesi() {
        if (pesiCaricati) {
            return;
        }
        pesiCaricati = true;
        for (Fase f : Fase.values()) {
            PESI.put(f, (long) f.pesoDefault);
        }
        try {
            File file = new File(VarStatiche.getFile_TempiAvvio());
            if (!file.isFile()) {
                return;
            }
            for (String riga : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] parti = riga.split("=", 2);
                if (parti.length != 2) {
                    continue;
                }
                try {
                    Fase f = Fase.valueOf(parti[0].trim());
                    long ms = Long.parseLong(parti[1].trim());
                    //Una fase misurata a zero (archivio vuoto) non deve sparire dalla barra: resta un
                    //minimo, altrimenti la fase successiva partirebbe con un salto.
                    PESI.put(f, Math.max(20, ms));
                } catch (IllegalArgumentException ex) {
                    //Fase non più esistente o valore non numerico: si tiene il default.
                }
            }
        } catch (Exception ex) {
            System.err.println("Splash: tempi di avvio non leggibili: " + ex);
        }
    }

    /**
     * Salva i tempi misurati, mediati con quelli già noti. La media smorza l'avvio anomalo (cache del
     * sistema fredda, primo avvio dopo un import massiccio) senza impedire alla barra di adattarsi
     * quando l'archivio cresce davvero.
     */
    private static void salvaTempi() {
        try {
            if (DURATE.isEmpty()) {
                return;
            }
            List<String> righe = new ArrayList<>();
            for (Fase f : Fase.values()) {
                Long misurata = DURATE.get(f);
                long peso = PESI.getOrDefault(f, (long) f.pesoDefault);
                long nuovo = (misurata == null) ? peso : Math.max(0, (peso + misurata) / 2);
                righe.add(f.name() + "=" + nuovo);
            }
            Files.write(new File(VarStatiche.getFile_TempiAvvio()).toPath(), righe, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Splash: tempi di avvio non salvati: " + ex);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Disegno
    // ---------------------------------------------------------------------------------------------

    /**
     * Disegna il contenuto completo dello splash. È l'unica definizione di questa grafica: la usano sia
     * la finestra mostrata a runtime sia {@link GeneraSplash}, che da qui ricava l'immagine dello splash
     * nativo AWT. Tenerla in un solo punto è ciò che garantisce che le due non divergano e che il
     * passaggio dallo splash nativo a questa finestra resti impercettibile.
     *
     * @param g2 contesto su cui disegnare, di dimensione {@link #LARGHEZZA} x {@link #ALTEZZA}
     * @param logo immagine del logo, centrata e riscalata nello spazio disponibile
     * @param frazione riempimento della barra, da 0 a 1
     * @param testo testo mostrato dentro la barra
     */
    static void disegnaContenuto(Graphics2D g2, BufferedImage logo, double frazione, String testo) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(COLORE_SFONDO);
        g2.fillRoundRect(0, 0, LARGHEZZA, ALTEZZA, ARCO, ARCO);
        g2.setColor(COLORE_BORDO);
        g2.drawRoundRect(0, 0, LARGHEZZA - 1, ALTEZZA - 1, ARCO, ARCO);

        if (logo != null) {
            double scala = Math.min((double) LOGO_L / logo.getWidth(), (double) LOGO_A / logo.getHeight());
            int larg = (int) Math.round(logo.getWidth() * scala);
            int alt = (int) Math.round(logo.getHeight() * scala);
            g2.drawImage(logo, BORDO + (LOGO_L - larg) / 2, BORDO + (LOGO_A - alt) / 2, larg, alt, null);
        }

        disegnaBarra(g2, frazione, testo);
    }

    /** Disegna la sola barra di avanzamento: sfondo, riempimento, cornice e testo centrato. */
    private static void disegnaBarra(Graphics2D g2, double frazione, String testo) {
        g2.setColor(COLORE_BARRA);
        g2.fillRoundRect(BARRA_X, BARRA_Y, BARRA_L, BARRA_A, 14, 14);

        int riempimento = larghezzaRiempimento(frazione);
        if (riempimento > 0) {
            //Il riempimento viene ritagliato sulla barra arrotondata, così il bordo destro resta
            //squadrato mentre avanza e gli angoli restano quelli della barra.
            java.awt.Shape clip = g2.getClip();
            g2.clip(new java.awt.geom.RoundRectangle2D.Float(BARRA_X, BARRA_Y, BARRA_L, BARRA_A, 14, 14));
            g2.setColor(COLORE_AVANZAMENTO);
            g2.fillRect(BARRA_X, BARRA_Y, riempimento, BARRA_A);
            g2.setClip(clip);
        }

        g2.setColor(COLORE_BARRA_BORDO);
        g2.drawRoundRect(BARRA_X, BARRA_Y, BARRA_L - 1, BARRA_A - 1, 14, 14);

        g2.setColor(Color.WHITE);
        g2.setFont(FONT_BARRA);
        FontMetrics fm = g2.getFontMetrics();
        String t = testo == null ? "" : testo;
        int x = BARRA_X + (BARRA_L - fm.stringWidth(t)) / 2;
        int y = BARRA_Y + (BARRA_A - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(t, x, y);
    }

    /** @return larghezza in pixel del riempimento della barra per la frazione indicata */
    private static int larghezzaRiempimento(double frazione) {
        return (int) Math.round(BARRA_L * Math.max(0, Math.min(1, frazione)));
    }

    /**
     * Ridisegna il fotogramma e lo riversa a video. Chiamabile da qualunque thread, EDT compreso: il
     * disegno non passa dalla coda degli eventi (vedi la nota in testa alla classe). Non fa nulla se
     * né il riempimento né il testo sono cambiati.
     */
    private static synchronized void disegnaEMostra() {
        //Riferimenti presi una volta sola: chiudi() li azzera dall'EDT mentre il thread che carica
        //potrebbe essere qui dentro, e un controllo fatto su un campo riletto sarebbe inutile.
        final BufferedImage base = sfondo;
        final BufferedImage foto = fotogramma;
        if (foto == null || base == null) {
            return;
        }
        int larghezza = larghezzaRiempimento(frazione);
        String t = testo;
        if (larghezza == ultimaLarghezza && t.equals(ultimoTesto)) {
            return;
        }
        ultimaLarghezza = larghezza;
        ultimoTesto = t;

        Graphics2D g2 = foto.createGraphics();
        try {
            g2.setComposite(AlphaComposite.Src);
            g2.drawImage(base, 0, 0, null);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.scale(scala, scala);
            disegnaBarra(g2, frazione, t);
        } finally {
            g2.dispose();
        }

        JWindow f = finestra;
        if (f == null) {
            return;
        }
        Graphics g = f.getGraphics();
        if (g != null) {
            try {
                //Dimensione LOGICA: l'immagine è a risoluzione del dispositivo e va rimappata 1:1 sui
                //pixel fisici, non disegnata al doppio della finestra.
                g.drawImage(foto, 0, 0, LARGHEZZA, ALTEZZA, null);
            } finally {
                g.dispose();
            }
        }
    }

    /** Crea la finestra dello splash. Da chiamare solo sull'EDT. */
    private static void costruisci() {
        BufferedImage logo = leggiLogo();
        if (logo == null) {
            return;
        }

        scala = fattoreDiScala();
        int larghezzaPixel = (int) Math.ceil(LARGHEZZA * scala);
        int altezzaPixel = (int) Math.ceil(ALTEZZA * scala);

        //Lo sfondo (pannello + logo + barra vuota) viene disegnato una volta sola: a ogni avanzamento
        //si ricopia questa immagine e si ridisegna la sola barra.
        sfondo = new BufferedImage(larghezzaPixel, altezzaPixel, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = sfondo.createGraphics();
        try {
            gs.scale(scala, scala);
            disegnaContenuto(gs, logo, 0, testo);
        } finally {
            gs.dispose();
        }
        fotogramma = new BufferedImage(larghezzaPixel, altezzaPixel, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gf = fotogramma.createGraphics();
        try {
            gf.setComposite(AlphaComposite.Src);
            gf.drawImage(sfondo, 0, 0, null);
        } finally {
            gf.dispose();
        }

        JPanel root = new JPanel() {
            /** Dimensione fissa dello splash. */
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(LARGHEZZA, ALTEZZA);
            }

            /**
             * Ridisegna l'ultimo fotogramma prodotto. Serve solo agli eventi di riesposizione: gli
             * avanzamenti arrivano a video per conto loro, senza aspettare l'EDT.
             */
            @Override
            protected void paintComponent(Graphics g) {
                BufferedImage f = fotogramma;
                if (f != null) {
                    g.drawImage(f, 0, 0, LARGHEZZA, ALTEZZA, null);
                }
            }
        };
        root.setOpaque(false);

        JWindow splash = new JWindow();
        splash.setContentPane(root);
        splash.pack();

        splash.setShape(new java.awt.geom.RoundRectangle2D.Double(
                0, 0, splash.getWidth(), splash.getHeight(), ARCO, ARCO));

        try {
            splash.setOpacity(OPACITA);
        } catch (Exception ex) {
            //Trasparenza non supportata dalla piattaforma: lo splash resta comunque utilizzabile.
        }

        //NON usare setLocationRelativeTo(null): quello centra sull'area di lavoro, cioè esclude la barra
        //delle applicazioni, mentre AWT centra lo splash nativo sullo schermo intero. La differenza è
        //metà altezza della barra (24px su una barra da 48) e si vedrebbe come uno scatto verticale nel
        //momento in cui questa finestra sostituisce lo splash nativo.
        try {
            Rectangle schermo = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            splash.setLocation(
                    schermo.x + (schermo.width - splash.getWidth()) / 2,
                    schermo.y + (schermo.height - splash.getHeight()) / 2);
        } catch (Exception ex) {
            splash.setLocationRelativeTo(null);
        }

        splash.setVisible(true);

        finestra = splash;
        attivo = true;
    }

    /**
     * @return il fattore di scala dello schermo principale (2.0 su un monitor HiDPI al 200%), oppure
     *         1.0 se non è determinabile
     */
    private static double fattoreDiScala() {
        try {
            double s = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().getDefaultTransform().getScaleX();
            return (s > 0 && s <= 8) ? s : 1.0;
        } catch (Exception ex) {
            return 1.0;
        }
    }

    /**
     * Legge il logo dalla cartella delle risorse; se non è presente ripiega sulla copia inclusa nel jar
     * (la stessa usata dallo splash nativo), così lo splash funziona anche prima che i percorsi siano
     * completamente configurati.
     */
    private static BufferedImage leggiLogo() {
        try {
            File file = new File(VarStatiche.getPathRisorse() + "logo.png");
            if (file.isFile()) {
                return ImageIO.read(file);
            }
        } catch (Exception ex) {
            System.err.println("Logo dello splash non leggibile dalle risorse: " + ex);
        }

        try (InputStream in = SplashAvvio.class.getResourceAsStream("/logo.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ex) {
            System.err.println("Logo dello splash non leggibile dal jar: " + ex);
        }

        return null;
    }
}
