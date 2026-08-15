package com.giacenzecrypto.giacenze_crypto;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Locale;

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
}
