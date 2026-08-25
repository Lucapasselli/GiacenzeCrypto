package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

/**
 * Finestra <i>Informazioni</i>: versione, riservatezza dei dati, fonti dei dati e licenze.
 *
 * <p><b>Non è una vetrina: è il posto dove il programma assolve gli obblighi di attribuzione dei
 * fornitori di dati che interroga.</b> Due sono dovuti per contratto e non sono facoltativi:
 * <ul>
 * <li><b>CoinGecko</b> pretende la dicitura {@code Powered by CoinGecko} con collegamento, in
 *     carattere <b>non inferiore a 10</b> e in posizione visibile vicino al dato — di qui
 *     {@link #CORPO_MINIMO_ATTRIBUZIONE} e il fatto che la finestra sia raggiungibile da ogni scheda,
 *     con un clic sul titolo;</li>
 * <li><b>Etherscan</b> chiede {@code Powered by Etherscan.io APIs} o un backlink per qualunque uso
 *     che non sia strettamente personale, e un'applicazione distribuita non lo è.</li>
 * </ul>
 * Il ragionamento completo, fornitore per fornitore, sta in
 * {@code nocommit/Documentazione/Analisi_API_Terze_Parti.md}.
 *
 * <p><b>Perché non c'è un {@code .form}</b>: il contenuto è generato — dipende dalla versione e
 * dall'edizione (vedi {@link VarStatiche#EdizioneStore()}), e nell'edizione Store non deve nominare lo
 * scaricamento dai conti exchange, che lì non esiste. Un testo fisso disegnato nel Builder sarebbe una
 * dichiarazione falsa proprio nella finestra che un revisore legge per prima. Il corpo è un
 * {@link JEditorPane} in sola lettura perché i collegamenti devono essere <b>cliccabili</b>: una
 * {@code JLabel} con dell'HTML dentro non lo sono.
 *
 * <p><b>Il logo di CoinGecko non c'è, ed è una scelta.</b> Le loro linee guida ammettono il logo solo
 * accompagnato dal testo "Data powered by" oppure "Price data provided by": il solo logo è peggio di
 * nessun logo, e la dicitura testuale da sola è già conforme.
 *
 * @author luca.passelli
 */
public class GUI_Informazioni extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Corpo minimo con cui scrivere le attribuzioni. Non è una scelta estetica: è il minimo preteso
     * dai termini d'uso di CoinGecko, e va rispettato anche quando l'utente ha rimpicciolito tutto il
     * resto dell'interfaccia con {@code --fontSize}.
     */
    static final int CORPO_MINIMO_ATTRIBUZIONE = 10;

    static final String URL_COINGECKO = "https://www.coingecko.com";
    static final String URL_ETHERSCAN = "https://etherscan.io";

    /**
     * Apre la finestra, modale, centrata sul proprietario.
     * @param Proprietario finestra su cui centrarsi, eventualmente {@code null}
     */
    public static void Mostra(Window Proprietario) {
        GUI_Informazioni d = new GUI_Informazioni(Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    private GUI_Informazioni(Window Proprietario) {
        super(Proprietario, "Informazioni", ModalityType.APPLICATION_MODAL);

        JEditorPane corpo = new JEditorPane("text/html", Testo());
        corpo.setEditable(false);
        corpo.setOpaque(false);
        //Senza questa proprietà il pane si sceglie il font da sé e ignora quello dell'applicazione
        corpo.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        corpo.setFont(FontApplicazione.Font(Font.PLAIN, Corpo()));
        corpo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        corpo.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                Funzioni.ApriWeb(e.getURL().toString());
            }
        });
        corpo.setCaretPosition(0);

        JScrollPane scorrimento = new JScrollPane(corpo,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);

        JPanel testata = new JPanel();
        testata.setLayout(new BoxLayout(testata, BoxLayout.X_AXIS));
        testata.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 4));
        //Il logo sta accanto al jar, non nel classpath: se manca, ImageIcon resta di dimensione zero
        //e la testata si limita al titolo, senza eccezioni e senza buchi visibili
        ImageIcon logo = new ImageIcon(VarStatiche.getPathRisorse() + "logo.png");
        if (logo.getIconWidth() > 0) {
            testata.add(new JLabel(new ImageIcon(logo.getImage()
                    .getScaledInstance(48, 48, java.awt.Image.SCALE_SMOOTH))));
            testata.add(Box.createHorizontalStrut(12));
        }
        JLabel titolo = new JLabel(VarStatiche.Titolo);
        titolo.setFont(FontApplicazione.Font(Font.BOLD, Corpo() + 6));
        testata.add(titolo);
        testata.add(Box.createHorizontalGlue());

        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel();
        pulsanti.setLayout(new BoxLayout(pulsanti, BoxLayout.X_AXIS));
        pulsanti.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        pulsanti.add(Box.createHorizontalGlue());
        pulsanti.add(chiudi);

        JPanel contenuto = new JPanel(new BorderLayout());
        contenuto.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenuto.add(testata, BorderLayout.NORTH);
        contenuto.add(scorrimento, BorderLayout.CENTER);
        contenuto.add(pulsanti, BorderLayout.SOUTH);

        setContentPane(contenuto);
        getRootPane().setDefaultButton(chiudi);
        setPreferredSize(new Dimension(640, 620));
        pack();
        Icone.AdattaIconeAlTema(contenuto);
    }

    /**
     * @return il corpo con cui scrivere il testo: quello dell'interfaccia, ma mai sotto
     *         {@link #CORPO_MINIMO_ATTRIBUZIONE}
     */
    static int Corpo() {
        Font f = UIManager.getFont("Label.font");
        int corpo = f != null ? f.getSize() : CORPO_MINIMO_ATTRIBUZIONE;
        return Math.max(CORPO_MINIMO_ATTRIBUZIONE, corpo);
    }

    /**
     * Costruisce il testo della finestra.
     *
     * <p>È separato dalla costruzione della finestra per poterlo verificare senza aprire niente: le
     * due attribuzioni obbligatorie e la coerenza con l'edizione sono affermazioni che devono restare
     * vere, non dettagli grafici (vedi {@code GUI_InformazioniTest}).
     *
     * @return il documento HTML mostrato nel corpo della finestra
     */
    static String Testo() {
        boolean Store = VarStatiche.EdizioneStore();
        StringBuilder h = new StringBuilder(2000);

        h.append("<html><body>");

        h.append("<p>Calcolo delle giacenze e delle plusvalenze in criptovaluta per la dichiarazione ")
         .append("dei redditi italiana (Quadro RW e Quadro RT), con metodo LIFO.</p>");

        //Le due attribuzioni dovute stanno in cima, non in fondo: CoinGecko le vuole "in a visible
        //location", e in fondo a una finestra con la barra di scorrimento visibili non sono.
        h.append("<p style='font-size:").append(Corpo()).append("pt'>")
         .append("<b>Powered by <a href='").append(URL_COINGECKO).append("'>CoinGecko</a></b><br>")
         .append("<b>Powered by <a href='").append(URL_ETHERSCAN).append("'>Etherscan.io</a> APIs</b>")
         .append("</p>");

        h.append("<h3>Documentazione</h3>");
        h.append("<p>I manuali e l'elenco delle novità di ogni versione si consultano dal browser: ")
         .append("<a href='").append(DocumentiAiuto.Url("")).append("'>documentazione</a> e ")
         .append("<a href='").append(DocumentiAiuto.Url(DocumentiAiuto.NOVITA_VERSIONI))
         .append("'>novità delle versioni</a>.</p>");

        h.append("<h3>Dati e riservatezza</h3>");
        h.append("<p><b>Tutti i dati restano su questo dispositivo.</b> Movimenti, wallet, quotazioni e ")
         .append("preferenze sono salvati nei database locali della cartella di lavoro. Il programma non ")
         .append("ha un server proprio e non invia da nessuna parte l'archivio dei movimenti.</p>");
        h.append("<p><b>Il programma non chiede, non gestisce e non conserva seed phrase o chiavi private ")
         .append("dei wallet</b>, in nessuna forma e in nessun file. Dei wallet in blockchain gli serve ")
         .append("soltanto l'indirizzo pubblico, che è quello che serve a leggerne le transazioni.</p>");
        if (!Store) {
            h.append("<p>Le chiavi API degli exchange che vengono eventualmente inserite restano nel ")
             .append("database locale e viaggiano solo verso l'exchange a cui appartengono. Non sono ")
             .append("necessarie: servono unicamente a scaricare i propri movimenti senza passare da un ")
             .append("file, e i backup le escludono se non lo si chiede esplicitamente.</p>");
        }
        h.append("<p>Le uniche chiamate di rete sono quelle che servono a importare i movimenti dagli ")
         .append("explorer di blockchain e a recuperare le quotazioni storiche, elencate qui sotto.</p>");

        h.append("<h3>Fonti dei dati</h3>");
        h.append("<p>Oltre a CoinGecko e a Etherscan, le quotazioni e i movimenti in blockchain ")
         .append("provengono da:</p>");
        h.append("<ul>");
        h.append("<li>Binance, Coinbase Exchange, KuCoin, Bybit, Bitstamp, OKX e Crypto.com, dai loro ")
         .append("dati di mercato pubblici</li>");
        h.append("<li>CoinMarketCap, per l'anagrafica dei token</li>");
        h.append("<li>DefiLlama, per le quotazioni storiche di ripiego</li>");
        h.append("<li>Blockscout, Moralis, Helius, Unisat e mempool.space, per le transazioni delle ")
         .append("varie blockchain</li>");
        h.append("<li>GoPlus Labs, per l'analisi di sicurezza dei token</li>");
        h.append("<li>Banca d'Italia, per il cambio ufficiale EUR/USD usato nei calcoli fiscali</li>");
        h.append("</ul>");
        h.append("<p>Le chiavi API di questi fornitori, dove servono, sono quelle dell'utente e restano ")
         .append("regolate dalle condizioni del piano che ha sottoscritto con ciascuno di loro.</p>");
        if (Store) {
            h.append("<p>Questa edizione <b>non accede ad alcun conto di exchange</b>: i movimenti si ")
             .append("importano dai file esportati dalle piattaforme e dagli indirizzi pubblici dei ")
             .append("wallet.</p>");
        }

        h.append("<h3>Licenze</h3>");
        h.append("<p>Giacenze Crypto è distribuito con licenza MIT. Le librerie di terze parti incluse ")
         .append("nel programma mantengono ciascuna la propria licenza: l'elenco completo, con le ")
         .append("versioni, è nel file THIRD-PARTY-LICENSES.md distribuito insieme al programma.</p>");
        h.append("<p>Il carattere tipografico è Noto Sans, distribuito con SIL Open Font License 1.1.</p>");

        h.append("</body></html>");
        return h.toString();
    }
}
