package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Contratti dei token da cercare in un wallet letto dai nodi pubblici: quelli ricevuti senza fare nulla e mai
 * spostati, che la scansione normale non vede. La logica sta in {@link NodoPubblicoDefi} ({@code LeggiToken},
 * {@code CercaToken}); la ricerca la lancia {@code GUI_GestioneWallets} con lo scaricamento normale del wallet.
 * Scritto a mano, senza {@code .form}, come {@link GUI_WalletBtcIndirizzi}.
 */
public class GUI_CercaTokenWallet extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final String Wallet;
    private final String Rete;
    private final JTextArea Area = new JTextArea();
    private final JButton Cerca = new JButton("Cerca i movimenti");
    private final JButton Annulla = new JButton("Annulla");
    private List<String> Risultato = List.of();

    /**
     * Chiede i contratti da cercare nel wallet {@code Wallet} della rete {@code Rete}.
     *
     * @return gli indirizzi dei token da cercare, gia' controllati sul nodo; vuoto se l'utente annulla
     */
    public static List<String> Mostra(String Wallet, String Rete, Window Proprietario) {
        GUI_CercaTokenWallet d = new GUI_CercaTokenWallet(Wallet, Rete, Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d.Risultato;
    }

    private GUI_CercaTokenWallet(String Wallet, String Rete, Window Proprietario) {
        super(Proprietario, "Token da cercare nel wallet " + Wallet + " (" + Rete + ")", ModalityType.APPLICATION_MODAL);
        this.Wallet = Wallet;
        this.Rete = Rete;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel Spiegazione = new JLabel("<html>Su questa rete i movimenti si leggono dai nodi pubblici, che non vedono un token "
                + "<b>ricevuto senza fare nulla e mai spostato</b> (tipicamente un airdrop).<br>"
                + "Se il wallet ne possiede uno, scrivere qui l'indirizzo del contratto, uno per riga: il programma ne cerca "
                + "i movimenti<br>e scarica anche i nuovi movimenti del wallet, come \"Aggiorna Selezionati\". Da li' in "
                + "avanti il token viene seguito.<br>"
                + "I token posseduti si vedono nel proprio wallet o sulla pagina del wallet nell'explorer.<br>"
                + "Molti token arrivati senza richiesta sono SCAM: prima della ricerca il programma ne mostra nome e "
                + "saldo.</html>");
        Spiegazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Area.setFont(new Font("Noto Sans", Font.PLAIN, 14));
        JScrollPane Scroll = new JScrollPane(Area);
        Scroll.setPreferredSize(new Dimension(620, 200));
        Scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 10, 0, 10), Scroll.getBorder()));

        JButton Explorer = new JButton("Apri il wallet nell'explorer");
        Explorer.setEnabled(Funzioni_WalletDeFi.UrlSituazioneWallet(Wallet, Rete) != null);
        Explorer.addActionListener(e -> Funzioni_WalletDeFi.ApriSituazioneWallet(Wallet, Rete));
        Cerca.addActionListener(e -> Cerca());
        Annulla.addActionListener(e -> dispose());
        JPanel Sinistra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Sinistra.add(Explorer);
        JPanel Destra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Destra.add(Annulla);
        Destra.add(Cerca);
        JPanel Pulsanti = new JPanel(new BorderLayout());
        Pulsanti.add(Sinistra, BorderLayout.WEST);
        Pulsanti.add(Destra, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Spiegazione, BorderLayout.NORTH);
        getContentPane().add(Scroll, BorderLayout.CENTER);
        getContentPane().add(Pulsanti, BorderLayout.SOUTH);
        pack();
    }

    private void Cerca() {
        String Testo = Area.getText();
        List<String> Indirizzi = NodoPubblicoDefi.IndirizziDaTesto(Testo);
        List<String> NonValide = NodoPubblicoDefi.RigheSenzaIndirizzo(Testo);
        if (!NonValide.isEmpty()) {
            Messaggi.WarningMessage("Elenco non valido", "Queste righe non contengono un indirizzo di contratto "
                    + "(0x seguito da 40 caratteri esadecimali):<br>" + String.join("<br>", NonValide), this);
            return;
        }
        if (Indirizzi.isEmpty()) {
            Messaggi.WarningMessage("Nessun token indicato", "Scrivere almeno un indirizzo di contratto.", this);
            return;
        }
        Attesa(true);
        new SwingWorker<List<NodoPubblicoDefi.InfoToken>, Void>() {
            @Override
            protected List<NodoPubblicoDefi.InfoToken> doInBackground() throws Exception {
                return NodoPubblicoDefi.LeggiToken(Rete, Wallet, Indirizzi);
            }

            @Override
            protected void done() {
                Attesa(false);
                List<NodoPubblicoDefi.InfoToken> Info;
                try {
                    Info = get();
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    Messaggi.WarningMessage("Lettura dei token non riuscita",
                            "I nodi pubblici di " + Rete + " non hanno risposto: " + causa.getMessage(), GUI_CercaTokenWallet.this);
                    return;
                }
                Conferma(Info);
            }
        }.execute();
    }

    private void Conferma(List<NodoPubblicoDefi.InfoToken> Info) {
        List<String> DaCercare = new ArrayList<>();
        StringBuilder Validi = new StringBuilder();
        StringBuilder Scartati = new StringBuilder();
        for (NodoPubblicoDefi.InfoToken t : Info) {
            if (t.scarto() != null) {
                Scartati.append("<br>").append(t.indirizzo()).append(": ").append(t.scarto());
                continue;
            }
            DaCercare.add(t.indirizzo());
            String Simbolo = t.simbolo().isBlank() ? "(senza simbolo)" : Html(t.simbolo());
            Validi.append("<br><b>").append(Simbolo).append("</b>");
            if (!t.nome().isBlank() && !t.nome().equals(t.simbolo())) Validi.append(" - ").append(Html(t.nome()));
            Validi.append(", saldo attuale ").append(Saldo(t)).append("<br>&nbsp;&nbsp;").append(t.indirizzo());
            //Un ERC-1155 non ha balanceOf(address): saldo zero e la ricerca per stati non lo vede
            if (t.saldo().signum() == 0) Validi.append("<br>&nbsp;&nbsp;saldo zero: se e' un NFT ERC-1155 la ricerca non lo trova");
        }
        if (DaCercare.isEmpty()) {
            Messaggi.WarningMessage("Nessun token da cercare", "Nessuno degli indirizzi si puo' cercare:" + Scartati, this);
            return;
        }
        String Dettaglio = "Token da cercare:" + Validi
                + (Scartati.length() > 0 ? "<br><br>Non si cercano:" + Scartati : "")
                + "<br><br>La ricerca puo' richiedere qualche minuto. Avviarla?";
        AppDialog.DialogResult r = AppDialog.builder(this)
                .windowTitle("Ricerca dei token")
                .bodyTitle("Ricerca dei token")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("")
                .details(Dettaglio)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("si", "Cerca")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        if (r != null && r.isAction("si")) {
            Risultato = DaCercare;
            dispose();
        }
    }

    /** Il saldo nelle unita' del token se i decimali sono leggibili, altrimenti com'e' sul contratto. */
    private static String Saldo(NodoPubblicoDefi.InfoToken t) {
        try {
            return new BigDecimal(t.saldo(), Integer.parseInt(t.decimali())).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return t.saldo().toString();
        }
    }

    /** I nomi dei token SCAM contengono spesso markup: si mostrano come testo. */
    private static String Html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void Attesa(boolean Si) {
        Cerca.setEnabled(!Si);
        Annulla.setEnabled(!Si);
        Area.setEditable(!Si);
        setCursor(Si ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
