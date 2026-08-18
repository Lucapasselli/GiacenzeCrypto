package com.giacenzecrypto.giacenze_crypto;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
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
        return Dammi(nomeLogo, null, lato);
    }

    /**
     * Come {@link #Dammi(String, int)}, ma sa anche come si chiama la voce: dove il file del logo manca,
     * l'edizione Store disegna al suo posto il monogramma (vedi {@link #Monogramma(String, int)}) invece
     * del segnaposto trasparente.
     * <p>L'ordine è questo e non l'inverso: <b>un logo presente vince sempre sul monogramma</b>. Anche
     * un'installazione Store può ritrovarsi i file addosso, perché {@code Backup_Restore.CartelleConfig()}
     * comprende {@code config/loghi} e il ripristino di un archivio prodotto dall'edizione completa li
     * riporta sul disco — comportamento voluto, documentato in
     * {@code test/Documentazione/Analisi_API_Terze_Parti.md} §2.
     * @param nomeLogo nome del file senza estensione, già in forma di slug; può essere {@code null}
     * @param etichetta nome della voce come compare a video ({@code "Bitpanda"}, {@code "Arbitrum (ARB)"},
     *        {@code "ARB"}); può essere {@code null}, e allora non si disegna nessun monogramma
     * @param lato lato del quadrato in pixel, viene comunque riportato entro limiti ragionevoli
     * @return un'icona mai {@code null}
     */
    public static Icon Dammi(String nomeLogo, String etichetta, int lato) {
        int l = Math.max(LATO_MIN, Math.min(LATO_MAX, lato));

        Icon logo = null;
        if (nomeLogo != null && !nomeLogo.isBlank()) {
            String chiave = nomeLogo + "@" + l;
            if (CACHE.containsKey(chiave)) {
                logo = CACHE.get(chiave);
            } else {
                logo = Carica(nomeLogo, l);
                //anche l'assenza va messa in cache, altrimenti si ritenta la lettura a ogni ridisegno
                CACHE.put(chiave, logo);
            }
        }
        if (logo != null) {
            return logo;
        }

        if (VarStatiche.EdizioneStore()) {
            Icon m = Monogramma(etichetta, l);
            if (m != null) {
                return m;
            }
        }
        return Segnaposto(l);
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

    //====================================================================================
    // Monogrammi — l'alternativa ai loghi per l'edizione Store
    //
    // L'edizione destinata al Microsoft Store non distribuisce config/loghi (vedi
    // test/Documentazione/Analisi_API_Terze_Parti.md §2): i loghi degli exchange e delle catene sono
    // marchi altrui, e ridisegnarli non cambierebbe nulla, perché il marchio non è il file ma il segno.
    // Al loro posto si disegna una sigla dentro un cerchio: non è di nessuno, segue il tema, e copre
    // tutte le voci — comprese quelle che verranno aggiunte domani, senza generare niente.
    //====================================================================================

    /**
     * Raggio degli angoli del monogramma, in frazione del lato: è quello del logo dell'applicazione
     * ({@code logo.png}, 114 px su 500), così la sigla accanto alla voce richiama la forma che l'utente
     * ha appena visto all'avvio.
     */
    private static final double RAGGIO_ANGOLI = 0.228;

    /** Codice di rete fra parentesi tonde in fondo all'etichetta: {@code "Arbitrum (ARB)"} → {@code ARB} */
    private static final java.util.regex.Pattern CODICE_IN_ETICHETTA =
            java.util.regex.Pattern.compile(".*\\(\\s*([A-Za-z0-9]+)\\s*\\)\\s*$");

    /**
     * Nomi noti, per la lettera distintiva. Calcolato una volta sola e <b>non svuotato da
     * {@link #SvuotaCache()}</b>: gli elenchi di {@link Importazioni_Gestione} sono array statici, scritti
     * nel codice, e non cambiano mentre il programma è aperto. Se un giorno diventassero modificabili a
     * runtime, questo campo va invalidato <i>insieme</i> alla cache delle icone — le sigle già disegnate
     * sono memorizzate lì.
     */
    private static String[] NOMI_NOTI;

    /** @return exchange e wallet degli elenchi di import, cioè i nomi fra cui la sigla deve distinguere */
    private static String[] NomiNoti() {
        if (NOMI_NOTI == null) {
            java.util.List<String> l = new java.util.ArrayList<>();
            java.util.Collections.addAll(l, Importazioni_Gestione.Exchanges);
            java.util.Collections.addAll(l, Importazioni_Gestione.Wallets);
            NOMI_NOTI = l.toArray(new String[0]);
        }
        return NOMI_NOTI;
    }

    /**
     * Sigla da disegnare nel monogramma di una voce.
     * <p>Tre casi, in quest'ordine:
     * <ul>
     * <li><b>reti</b>: il codice che l'etichetta già porta con sé — {@code "Arbitrum (ARB)"} → {@code ARB},
     *     e la sola {@code "ARB"} della colonna della tabella dà lo stesso risultato. Non è un'iniziale
     *     inventata, è l'identificativo che l'utente sta già leggendo;
     * <li><b>nomi composti</b>: le iniziali delle prime due parole ({@code "Yield App"} → {@code YA});
     * <li><b>nomi di una parola sola</b>: l'iniziale più <b>la prima lettera che distingue il nome dagli
     *     altri con la stessa iniziale</b>, non la seconda del nome. È l'unico modo perché Binance,
     *     Bitfinex, Bitpanda, Bitrue, Bitstamp e Bittrex non diventino sei cerchi identici con scritto
     *     «Bi»: diventano Bn, Bf, Bp, Br, Bs, Bt.
     * </ul>
     * <p>Le sigle già in maiuscolo restano come sono ({@code OKX}, {@code MEXC} → {@code MEX}): un
     * acronimo scritto {@code "Ok"} si legge come un nome diverso. Oltre le tre lettere si abbrevia,
     * perché a 19 pixel la quarta non si legge e {@code "GNOS"} sembra un troncamento sbagliato mentre
     * {@code "GNO"} si legge come abbreviazione.
     * @param etichetta nome della voce come compare a video; può essere {@code null}
     * @return la sigla, in maiuscolo o in forma {@code Xy}; stringa vuota se non c'è nulla da scrivere
     */
    public static String Sigla(String etichetta) {
        return Sigla(etichetta, NomiNoti());
    }

    /**
     * Come {@link #Sigla(String)}, ma con l'elenco dei nomi fra cui distinguere passato dal chiamante.
     * <p>Esiste separata perché la lettera distintiva dipende da <i>tutti</i> gli altri nomi, e una
     * regola così va potuta verificare su un elenco scelto invece che su quello dell'applicazione.
     */
    static String Sigla(String etichetta, String[] elenco) {
        String e = etichetta == null ? "" : etichetta.trim();
        if (e.isEmpty() || isVoceSpeciale(e)) {
            return "";
        }

        java.util.regex.Matcher m = CODICE_IN_ETICHETTA.matcher(e);
        if (m.matches()) {
            return Abbrevia(m.group(1));
        }

        String[] parole = e.split("\\s+");
        if (parole.length >= 2 && !parole[0].isEmpty() && !parole[1].isEmpty()) {
            return ("" + parole[0].charAt(0) + parole[1].charAt(0)).toUpperCase();
        }

        String sole = e.replaceAll("[^A-Za-z0-9]", "");
        if (sole.isEmpty()) {
            return "";
        }
        if (sole.length() == 1 || sole.equals(sole.toUpperCase())) {
            return Abbrevia(sole);
        }
        return "" + Character.toUpperCase(sole.charAt(0))
                + Character.toLowerCase(LetteraDistintiva(sole, elenco));
    }

    /** @return la sigla in maiuscolo, ridotta a tre caratteri */
    private static String Abbrevia(String codice) {
        String c = codice.toUpperCase();
        return c.length() <= 3 ? c : c.substring(0, 3);
    }

    /**
     * @return la prima lettera di {@code nome} (dopo l'iniziale) che lo distingue dagli altri nomi di una
     *         sola parola con lo stesso inizio; la seconda lettera del nome se nessuna lo distingue
     */
    private static char LetteraDistintiva(String nome, String[] elenco) {
        String min = nome.toLowerCase();
        for (int i = 1; i < min.length(); i++) {
            String inizio = min.substring(0, i);
            char ch = min.charAt(i);
            boolean unica = true;
            for (String v : elenco) {
                if (v == null) continue;
                String t = v.trim();
                //i nomi composti fanno sigla con le iniziali delle parole: non competono su questa lettera
                if (t.split("\\s+").length > 1 || isVoceSpeciale(t)) continue;
                String altro = t.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                if (altro.equals(min) || altro.length() <= i || !altro.startsWith(inizio)) continue;
                if (altro.charAt(i) == ch) {
                    unica = false;
                    break;
                }
            }
            if (unica) {
                return nome.charAt(i);
            }
        }
        return nome.charAt(1);
    }

    /**
     * Disegna il monogramma di una voce: la sua {@link #Sigla(String) sigla} scavata dentro un quadrato
     * pieno dagli angoli smussati, del colore delle icone e della forma del logo dell'applicazione.
     * <p>Le lettere non sono dipinte del colore di sfondo, sono <b>tolte</b> ({@link AlphaComposite#Clear}):
     * così sotto si vede quello che c'è davvero — la riga alternata della tabella, il colore della
     * selezione, il fondo della combo — senza che questa classe debba indovinarlo.
     * <p>Il corpo del carattere non è scelto a mano: la sigla viene presa come tracciato e scalata per
     * riempire lo spazio disponibile, così una sigla di tre lettere entra come una di due.
     * @param etichetta nome della voce; {@code null}, vuoto o voce speciale danno {@code null}
     * @param lato lato del quadrato in pixel
     * @return l'icona, oppure {@code null} se non c'è una sigla da disegnare
     */
    static Icon Monogramma(String etichetta, int lato) {
        String sigla = Sigla(etichetta);
        if (sigla.isEmpty()) {
            return null;
        }

        Color inchiostro = Icone.ColoreMonocromatico();
        String chiave = "\u00a7mono:" + sigla + "@" + lato + "@" + inchiostro.getRGB();
        Icon memorizzata = CACHE.get(chiave);
        if (memorizzata != null) {
            return memorizzata;
        }

        BufferedImage img = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        //Quadrato con gli angoli smussati, non cerchio: è la forma del logo dell'applicazione, e il
        //raggio è il suo — misurato su logo.png, 114 px su 500, cioè il 22,8% del lato (l'arco di
        //RoundRectangle2D è il diametro, quindi il doppio).
        double margine = lato * 0.05;
        double arco = lato * RAGGIO_ANGOLI * 2;
        g.setColor(inchiostro);
        g.fill(new RoundRectangle2D.Double(margine, margine,
                lato - 2 * margine, lato - 2 * margine, arco, arco));

        //il quadrato lascia più spazio del cerchio, dove la corda si accorcia allontanandosi dal centro
        double larghezzaUtile = (sigla.length() >= 3 ? 0.80 : 0.66) * lato;
        double altezzaUtile = 0.50 * lato;

        Font f = FontApplicazione.Font(Font.BOLD, lato);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), sigla);
        java.awt.Shape testo = gv.getOutline();
        Rectangle2D b = testo.getBounds2D();
        if (b.getWidth() > 0 && b.getHeight() > 0) {
            double k = Math.min(larghezzaUtile / b.getWidth(), altezzaUtile / b.getHeight());
            AffineTransform at = new AffineTransform();
            at.translate(lato / 2.0, lato / 2.0);
            at.scale(k, k);
            at.translate(-b.getCenterX(), -b.getCenterY());
            g.setComposite(AlphaComposite.Clear);
            g.fill(at.createTransformedShape(testo));
        }
        g.dispose();

        Icon icona = new ImageIcon(img);
        CACHE.put(chiave, icona);
        return icona;
    }

    /**
     * Svuota la cache delle icone. Serve se i file dei loghi cambiano mentre il programma è aperto,
     * cosa che di norma non accade: l'allineamento con il repository avviene all'avvio.
     */
    public static void SvuotaCache() {
        CACHE.clear();
        SEGNAPOSTO.clear();
    }

    /** Etichetta di rete nella forma {@code "Nome (CODICE)"}: il codice è quello salvato nei dati */
    private static final java.util.regex.Pattern CODICE_RETE =
            java.util.regex.Pattern.compile(".*\\(\\s*([A-Za-z0-9]+)\\s*\\)\\s*$");

    /**
     * Ricava dalle etichette complete delle reti la mappa <i>codice rete → nome del file del logo</i>.
     * <p>Serve dove la rete è salvata con il solo codice — la colonna "Rete" della tabella dei wallet,
     * per esempio, contiene {@code ARB}, non {@code "Arbitrum (ARB)"} — mentre il logo prende il nome
     * dall'etichetta intera ({@code arbitrum-arb.png}). Le etichette sono quelle già mostrate nelle
     * finestre, così a una rete aggiunta a un elenco l'icona segue da sola.
     * <p>A parità di codice vince il primo elenco: si passa per primo quello della finestra che si sta
     * disegnando.
     * @param elenchi elenchi di etichette, nella forma {@code "Nome (CODICE)"}; le voci di altra forma
     *        (separatori, segnaposto) vengono ignorate
     * @return mappa dal codice rete in maiuscolo allo slug del logo
     */
    public static Map<String, String> MappaLoghiRete(String[]... elenchi) {
        Map<String, String> mappa = new HashMap<>();
        for (String[] elenco : elenchi) {
            if (elenco == null) continue;
            for (String etichetta : elenco) {
                if (etichetta == null) continue;
                java.util.regex.Matcher m = CODICE_RETE.matcher(etichetta);
                if (m.matches()) {
                    mappa.putIfAbsent(m.group(1).toUpperCase(), Slug(etichetta));
                }
            }
        }
        return mappa;
    }

    /** @return {@code true} se la voce è un separatore o un segnaposto, non un nome con un logo */
    static boolean isVoceSpeciale(String voce) {
        String v = voce == null ? "" : voce.trim();
        return v.isEmpty() || v.startsWith("-") || v.startsWith("*");
    }

    /**
     * Disegna le voci di una combo con il logo della piattaforma a sinistra del nome, così la si
     * riconosce a colpo d'occhio.
     * <p>Serve tutte le combo che contengono nomi di exchange, wallet o blockchain: quelle della finestra
     * di import e quella della rete nella gestione dei wallet. Il logo si ricava dal nome con
     * {@link #Slug(String)} — è la stessa regola con cui i file sono stati salvati, quindi le due parti
     * non possono divergere.
     * <p>Non serve invece alle combo che ripetono un nome già indicato altrove (le estrazioni di un
     * fornitore): lì il logo su ogni riga sarebbe solo rumore.
     * <p>Il lato dell'icona segue l'altezza del carattere invece di essere fisso, perché la dimensione del
     * font si può cambiare all'avvio con {@code --fontSize}. Le voci senza logo ricevono un segnaposto
     * trasparente della stessa dimensione, che tiene i nomi incolonnati.
     */
    public static class RenderComboConLogo extends javax.swing.DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valore,
                int indice, boolean selezionato, boolean conFuoco) {

            super.getListCellRendererComponent(lista, valore, indice, selezionato, conFuoco);

            String nomeLogo = null;
            if (valore instanceof String nome && !isVoceSpeciale(nome)) {
                nomeLogo = Slug(nome);
            }

            if (nomeLogo == null) {
                setIcon(null);
                return this;
            }

            //un logo alto quanto il carattere risulta minuto in una riga da 40 pixel: il fattore
            //lo porta a occupare la riga senza sovrastare il testo
            int lato = Math.round(getFontMetrics(getFont()).getHeight() * 1.4f);
            setIcon(Dammi(nomeLogo, valore instanceof String s ? s : null, lato));
            setIconTextGap(8);
            return this;
        }
    }

    /**
     * Disegna una colonna di tabella con il logo a sinistra del testo.
     * <p>Il colore di fondo delle righe lo decide il renderer di default installato sulla tabella
     * (le righe alternate di {@link Tabelle#ColoraTabellaSemplice}, che cambia anche col tema): questo
     * renderer glielo chiede a ogni disegno e ne <b>copia</b> l'aspetto invece di restituirne il
     * componente. La copia è necessaria, non un vezzo: {@code DefaultTableCellRenderer} riusa sempre la
     * stessa etichetta, quindi un'icona posata su quella condivisa resterebbe attaccata anche alle celle
     * delle altre colonne disegnate dopo.
     * <p>L'icona è alta quanto la riga: una più alta verrebbe tagliata.
     */
    public static class RenderTabellaConLogo extends javax.swing.table.DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /** Ricava dal valore della cella il nome del file del logo; può restituire {@code null} */
        private final java.util.function.Function<Object, String> nomeLogo;

        /**
         * @param nomeLogo funzione dal valore della cella al nome del logo (senza estensione), libera di
         *        restituire {@code null} per le celle che non ne hanno uno
         */
        public RenderTabellaConLogo(java.util.function.Function<Object, String> nomeLogo) {
            this.nomeLogo = nomeLogo;
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable tabella, Object valore,
                boolean selezionato, boolean conFuoco, int riga, int colonna) {

            super.getTableCellRendererComponent(tabella, valore, selezionato, conFuoco, riga, colonna);

            javax.swing.table.TableCellRenderer predefinito = tabella.getDefaultRenderer(Object.class);
            if (predefinito != null && predefinito != this) {
                java.awt.Component base = predefinito.getTableCellRendererComponent(
                        tabella, valore, selezionato, conFuoco, riga, colonna);
                setOpaque(true);
                setBackground(base.getBackground());
                setForeground(base.getForeground());
                setFont(base.getFont());
                if (base instanceof javax.swing.JComponent jc) {
                    setBorder(jc.getBorder());
                }
            }

            //l'icona non deve superare l'altezza della riga, altrimenti viene tagliata
            int perFont = Math.round(getFontMetrics(getFont()).getHeight() * 1.4f);
            int lato = Math.min(perFont, tabella.getRowHeight() - 2);
            //nella colonna della rete il valore della cella è già il codice ("ARB"), che è la sigla giusta
            setIcon(Dammi(nomeLogo.apply(valore), valore == null ? null : valore.toString(), lato));
            setIconTextGap(6);
            return this;
        }
    }
}
