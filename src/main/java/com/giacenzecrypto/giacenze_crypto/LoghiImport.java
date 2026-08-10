package com.giacenzecrypto.giacenze_crypto;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Loghi degli exchange, dei wallet e delle blockchain mostrati accanto ai nomi nelle finestre di import.
 * <p>I file stanno in {@code config/loghi/}, allineati al repository all'avvio insieme alle altre
 * configurazioni, e sono prodotti dallo strumento di sviluppo {@link GeneraLoghi}. Il nome del file è lo
 * slug della voce — vedi {@link #Slug(String)} — così a un exchange aggiunto in futuro basta accostare un
 * PNG con il nome giusto, senza toccare il programma.
 * <p>Le icone vengono disegnate dentro un quadrato di lato fisso: un logo mancante o non quadrato non
 * sposta il testo, e le etichette dell'elenco restano incolonnate.
 * <p>I loghi sono riprodotti <b>come sono nel file</b>, senza correzioni di contrasto rispetto al tema in
 * uso. Si era provato a posare quelli poco contrastati su una piastrella neutra, ma la luminosità media
 * non distingue una sagoma monocromatica da un logo semplicemente colorato: il cerchio verde di Tatax o
 * la moneta d'oro di Dogecoin, perfettamente leggibili su bianco, si ritrovavano dentro un riquadro nero.
 * Il prezzo di rinunciarci è che un paio di loghi neri su fondo trasparente (Ledger, Mycelium) restano
 * poco leggibili sul tema scuro: si risolve sostituendo quei due file, non alterando tutti gli altri.
 *
 * @author luca.passelli
 */
public class LoghiImport {

    /** Icone già scalate, per nome logo e lato richiesto: il renderer viene chiamato a ogni ridisegno */
    private static final Map<String, Icon> CACHE = new HashMap<>();

    /** Segnaposto trasparenti, per lato */
    private static final Map<Integer, Icon> SEGNAPOSTO = new HashMap<>();

    /** Lati ammessi per l'icona: sotto non si riconosce nulla, sopra il menù diventa sgraziato */
    private static final int LATO_MIN = 14;
    private static final int LATO_MAX = 40;

    /**
     * Riduce un'etichetta al nome del file del suo logo: minuscolo, ogni sequenza di caratteri non
     * alfanumerici sostituita da un trattino.
     * <p>È la stessa regola che {@link GeneraLoghi} applica quando salva i file, e sta qui perché le due
     * parti non possano divergere.
     * @param testo l'etichetta da convertire, es. {@code "Crypto.com Exchange"}
     * @return lo slug corrispondente, es. {@code "crypto-com-exchange"}, stringa vuota se {@code testo} è nullo
     */
    public static String Slug(String testo) {
        return testo == null ? "" : testo.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    /**
     * Restituisce il logo indicato, scalato per stare in un quadrato del lato richiesto, oppure un
     * segnaposto trasparente delle stesse dimensioni se il file non c'è: un logo mancante è un problema
     * estetico, non deve impedire di scegliere l'import né disallineare l'elenco.
     * @param nomeLogo nome del file senza estensione, già in forma di slug; può essere {@code null}
     * @param lato lato del quadrato in pixel, viene comunque riportato entro limiti ragionevoli
     * @return un'icona mai {@code null}
     */
    public static Icon Dammi(String nomeLogo, int lato) {
        int l = Math.max(LATO_MIN, Math.min(LATO_MAX, lato));

        if (nomeLogo == null || nomeLogo.isBlank()) {
            return Segnaposto(l);
        }

        String chiave = nomeLogo + "@" + l;
        if (CACHE.containsKey(chiave)) {
            Icon i = CACHE.get(chiave);
            return i != null ? i : Segnaposto(l);
        }

        Icon icona = Carica(nomeLogo, l);
        //anche l'assenza va messa in cache, altrimenti si ritenta la lettura a ogni ridisegno
        CACHE.put(chiave, icona);
        return icona != null ? icona : Segnaposto(l);
    }

    /** @return l'icona letta da {@code config/loghi/<nomeLogo>.png}, oppure {@code null} se assente o illeggibile */
    private static Icon Carica(String nomeLogo, int lato) {
        try {
            File f = new File(VarStatiche.getCartella_ConfigLoghi(), nomeLogo + ".png");
            if (!f.isFile()) {
                return null;
            }
            BufferedImage originale = ImageIO.read(f);
            if (originale == null) {
                return null;
            }

            //Il logo viene centrato dentro un quadrato di lato fisso: i loghi non quadrati non
            //allargano la colonna del testo
            double k = (double) lato / Math.max(originale.getWidth(), originale.getHeight());
            int w = Math.max(1, (int) Math.round(originale.getWidth() * k));
            int h = Math.max(1, (int) Math.round(originale.getHeight() * k));

            BufferedImage quadro = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = quadro.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.drawImage(originale, (lato - w) / 2, (lato - h) / 2, w, h, null);
            g.dispose();

            return new ImageIcon(quadro);

        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /** @return un'icona trasparente quadrata del lato indicato, per tenere allineate le voci senza logo */
    private static Icon Segnaposto(int lato) {
        return SEGNAPOSTO.computeIfAbsent(lato,
                l -> new ImageIcon(new BufferedImage(l, l, BufferedImage.TYPE_INT_ARGB)));
    }

    /**
     * Svuota la cache delle icone. Serve se i file dei loghi cambiano mentre il programma è aperto,
     * cosa che di norma non accade: l'allineamento con il repository avviene all'avvio.
     */
    public static void SvuotaCache() {
        CACHE.clear();
        SEGNAPOSTO.clear();
    }
}
