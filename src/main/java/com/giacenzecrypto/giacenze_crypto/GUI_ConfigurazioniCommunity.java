package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import org.json.JSONObject;

/**
 * Il catalogo delle configurazioni di importazione condivise dalla community: scaricare, eliminare la copia locale,
 * ritirare le proprie proposte. Si apre dalla finestra Importazioni, che a chiusura ricarica le tendine.
 *
 * <p>Scritto a mano, senza {@code .form}. I testi che arrivano dal catalogo sono di altri utenti: nelle celle non devono
 * poter diventare HTML ({@link #Piano}), anche se server e {@link ValidazioneConfigCommunity} rifiutano gia' il "<".
 */
public class GUI_ConfigurazioniCommunity extends JDialog {

    private final DefaultTableModel Modello = new DefaultTableModel(
            new Object[]{"Fornitore", "Estrazione", "Exchange", "Autore", "Pubblicata", "Sul tuo PC", "Descrizione"}, 0) {
        /** @return sempre {@code false}: la tabella si legge, non si modifica */
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable Tabella = new JTable(Modello);
    private final JLabel Stato = new JLabel(" ");
    /** Le righe della tabella, nello stesso ordine del modello: voce del catalogo o copia locale non piu' pubblicata */
    private final List<CommunityConfigClient.VoceCatalogo> Righe = new ArrayList<>();
    private boolean Cambiato = false;

    /**
     * Mostra il catalogo e aspetta che venga chiuso.
     * @return vero se le configurazioni sul disco sono cambiate (le tendine vanno ricaricate)
     */
    public static boolean Mostra(Window Proprietario) {
        GUI_ConfigurazioniCommunity d = new GUI_ConfigurazioniCommunity(Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.Carica();
        d.setVisible(true);
        return d.Cambiato;
    }

    private GUI_ConfigurazioniCommunity(Window Proprietario) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Configurazioni della community");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel Intestazione = new JLabel("<html><body style='width:960px'>"
                + "Configurazioni di importazione scritte da altri utenti per formati che il programma non legge da solo. "
                + "Sono pubblicate solo dopo il controllo dell'autore del programma, che verifica che siano sicure e sensate "
                + "ma non le puo' provare su ogni file reale. <b>Prima di importare, prova la configurazione sul tuo CSV</b> "
                + "con il pulsante <i>Prova configurazione</i> e controlla i movimenti.<br><br>"
                + "Le configurazioni scaricate compaiono nelle tendine della finestra Importazioni con il prefisso "
                + "<i>Community:</i>, e si salvano nella cartella config/community.</body></html>");
        Intestazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Tabella.setAutoCreateRowSorter(true);
        Tabella.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Tabella.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        int[] larghezze = {130, 150, 110, 110, 90, 150, 450};
        for (int i = 0; i < larghezze.length; i++) Tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        JScrollPane Scorrimento = new JScrollPane(Tabella);
        Scorrimento.setPreferredSize(new Dimension(1000, 320));

        JButton Scarica = new JButton("Scarica");
        Scarica.addActionListener(e -> Scarica());
        JButton Elimina = new JButton("Elimina dal tuo PC");
        Elimina.addActionListener(e -> Elimina());
        JButton Proposte = new JButton("Le mie proposte...");
        Proposte.addActionListener(e -> LeMieProposte());
        JButton Aggiorna = new JButton("Aggiorna l'elenco");
        Aggiorna.addActionListener(e -> Carica());
        JButton Chiudi = new JButton("Chiudi");
        Chiudi.addActionListener(e -> dispose());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton b : new JButton[]{Scarica, Elimina, Proposte, Aggiorna, Chiudi}) Pulsanti.add(b);

        Stato.setBorder(BorderFactory.createEmptyBorder(4, 10, 0, 10));
        JPanel Basso = new JPanel(new BorderLayout());
        Basso.add(Stato, BorderLayout.NORTH);
        Basso.add(Pulsanti, BorderLayout.SOUTH);

        JPanel Contenuto = new JPanel(new BorderLayout());
        Contenuto.add(Intestazione, BorderLayout.NORTH);
        Contenuto.add(Scorrimento, BorderLayout.CENTER);
        Contenuto.add(Basso, BorderLayout.SOUTH);
        setContentPane(Contenuto);
        pack();
    }

    /** Scarica il catalogo in background e riempie la tabella, aggiungendo le copie locali non piu' pubblicate */
    private void Carica() {
        Stato.setText("Scaricamento del catalogo...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<CommunityConfigClient.VoceCatalogo>, Void>() {
            /** Legge il catalogo fuori dall'EDT. */
            @Override
            protected List<CommunityConfigClient.VoceCatalogo> doInBackground() throws Exception {
                return CommunityConfigClient.Catalogo();
            }

            /** Riempie la tabella, o dice perche' non e' stato possibile. */
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                List<CommunityConfigClient.VoceCatalogo> catalogo = new ArrayList<>();
                try {
                    catalogo = get();
                    Stato.setText("Configurazioni pubblicate: " + catalogo.size() + ".");
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    Stato.setText("Catalogo non raggiungibile: si vedono solo le configurazioni gia' sul tuo PC.");
                }
                Riempi(catalogo);
            }
        }.execute();
    }

    private void Riempi(List<CommunityConfigClient.VoceCatalogo> catalogo) {
        Righe.clear();
        Modello.setRowCount(0);
        SimpleDateFormat giorno = new SimpleDateFormat("yyyy-MM-dd");
        Set<Integer> pubblicate = new HashSet<>();
        for (CommunityConfigClient.VoceCatalogo v : catalogo) {
            pubblicate.add(v.id());
            String sulPc;
            if (!Funzioni.VersioneAppAlmeno(VarStatiche.Versione, v.versioneMinimaApp())) {
                sulPc = "richiede la versione " + v.versioneMinimaApp();
            } else if (CommunityConfigurazioni.Aggiornata(v)) {
                sulPc = "scaricata";
            } else if (CommunityConfigurazioni.FileLocale(v.id()).isFile()) {
                sulPc = "da aggiornare";
            } else {
                sulPc = "";
            }
            Aggiungi(v, v.pubblicata() > 0 ? giorno.format(new Date(v.pubblicata())) : "", sulPc);
        }
        //Le copie locali che il catalogo non ha piu' (ritirate o tolte): restano usabili, si possono eliminare
        File[] locali = new File(VarStatiche.getCartella_ConfigCommunity())
                .listFiles((d, n) -> n.startsWith(CommunityConfigurazioni.PREFISSO_FILE) && n.endsWith(".json"));
        if (locali != null) {
            for (File f : locali) {
                try {
                    int id = Integer.parseInt(f.getName().substring(CommunityConfigurazioni.PREFISSO_FILE.length(),
                            f.getName().length() - 5));
                    if (pubblicate.contains(id)) continue;
                    JSONObject o = new JSONObject(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                    Aggiungi(new CommunityConfigClient.VoceCatalogo(id, f.getName(), o.optString("fornitore"),
                            o.optString("estrazione"), o.optString("nomeExchange"), o.optString("descrizione"),
                            o.optString("versioneMinimaApp"), "", 0, ""), "", "non piu' nel catalogo");
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
            }
        }
    }

    private void Aggiungi(CommunityConfigClient.VoceCatalogo v, String data, String sulPc) {
        Righe.add(v);
        Modello.addRow(new Object[]{Piano(v.fornitore().isBlank() ? v.nomeExchange() : v.fornitore()), Piano(v.estrazione()),
            Piano(v.nomeExchange()), Piano(v.autore()), data, sulPc, Piano(v.descrizione())});
    }

    /** Le righe selezionate, convertite nell'ordine del modello */
    private List<CommunityConfigClient.VoceCatalogo> Selezionate() {
        List<CommunityConfigClient.VoceCatalogo> scelte = new ArrayList<>();
        for (int r : Tabella.getSelectedRows()) scelte.add(Righe.get(Tabella.convertRowIndexToModel(r)));
        return scelte;
    }

    private void Scarica() {
        List<CommunityConfigClient.VoceCatalogo> scelte = Selezionate();
        scelte.removeIf(v -> v.sha256().isBlank());
        if (scelte.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleziona una o piu' configurazioni del catalogo.", getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<String>, Void>() {
            /** Scarica, controlla e salva ogni configurazione scelta. */
            @Override
            protected List<String> doInBackground() {
                List<String> esiti = new ArrayList<>();
                for (CommunityConfigClient.VoceCatalogo v : scelte) {
                    String nome = (v.fornitore().isBlank() ? v.nomeExchange() : v.fornitore()) + " - " + v.estrazione();
                    if (!Funzioni.VersioneAppAlmeno(VarStatiche.Versione, v.versioneMinimaApp())) {
                        esiti.add(nome + ": richiede la versione " + v.versioneMinimaApp() + " del programma, non scaricata");
                        continue;
                    }
                    try {
                        CommunityConfigurazioni.SalvaScaricata(v, CommunityConfigClient.Scarica(v.id()));
                        esiti.add(nome + ": scaricata");
                        Cambiato = true;
                    } catch (Exception ex) {
                        LoggerGC.ScriviErrore(ex);
                        esiti.add(nome + ": " + ex.getMessage());
                    }
                }
                return esiti;
            }

            /** Riepiloga e aggiorna la tabella. */
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    JOptionPane.showMessageDialog(GUI_ConfigurazioniCommunity.this, String.join("\n", get())
                            + "\n\nLe configurazioni scaricate sono nelle tendine della finestra Importazioni con il prefisso"
                            + " \"Community:\".\nPrima di importare, provale sul tuo file con \"Prova configurazione\".",
                            getTitle(), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
                Carica();
            }
        }.execute();
    }

    private void Elimina() {
        List<CommunityConfigClient.VoceCatalogo> scelte = Selezionate();
        scelte.removeIf(v -> !CommunityConfigurazioni.FileLocale(v.id()).isFile());
        if (scelte.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nessuna delle righe selezionate e' sul tuo PC.", getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Eliminare dal tuo PC " + scelte.size()
                + " configurazioni della community?\nI movimenti gia' importati non cambiano.", getTitle(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        for (CommunityConfigClient.VoceCatalogo v : scelte) {
            if (CommunityConfigurazioni.Elimina(v.id())) Cambiato = true;
        }
        Carica();
    }

    /** Le proprie proposte, con la possibilita' di ritirarle */
    private void LeMieProposte() {
        List<CommunityConfigurazioni.PropostaInviata> proposte = CommunityConfigurazioni.ProposteInviate();
        if (proposte.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Da questo PC non e' stata inviata nessuna configurazione.\n"
                    + "Si propone una configurazione dalla finestra della prova, con \"Condividi con la community...\".",
                    getTitle(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SimpleDateFormat giorno = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] voci = proposte.stream()
                .map(p -> "n. " + p.id() + " - " + p.nomeFile() + " (inviata il " + giorno.format(new Date(p.inviata())) + ")")
                .toArray(String[]::new);
        JList<String> Elenco = new JList<>(voci);
        Elenco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane s = new JScrollPane(Elenco);
        s.setPreferredSize(new Dimension(520, 160));
        Object[] opzioni = {"Ritira la proposta selezionata", "Chiudi"};
        int scelta = JOptionPane.showOptionDialog(this, new Object[]{"Le configurazioni inviate da questo PC:", s},
                "Le mie proposte", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opzioni, opzioni[1]);
        if (scelta != 0 || Elenco.getSelectedIndex() < 0) return;
        CommunityConfigurazioni.PropostaInviata p = proposte.get(Elenco.getSelectedIndex());
        if (JOptionPane.showConfirmDialog(this, "Ritirare la proposta n. " + p.id() + "?\n"
                + "Se e' gia' pubblicata sparisce dal catalogo. Chi l'ha gia' scaricata ne conserva la copia.",
                "Le mie proposte", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            if (CommunityConfigClient.Ritira(p.id(), p.codice())) {
                CommunityConfigurazioni.TogliProposta(p.id());
                JOptionPane.showMessageDialog(this, "Proposta n. " + p.id() + " ritirata.", "Le mie proposte",
                        JOptionPane.INFORMATION_MESSAGE);
                Carica();
            } else {
                JOptionPane.showMessageDialog(this, "Il servizio non ha ritirato la proposta n. " + p.id()
                        + ": forse era gia' stata rifiutata o ritirata.", "Le mie proposte", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            JOptionPane.showMessageDialog(this, "Servizio non raggiungibile: " + ex.getMessage(), "Le mie proposte",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Un testo di altri da mettere in una cella: Swing interpreta come HTML un testo che comincia con {@code <html>},
     * uno spazio davanti glielo impedisce.
     */
    static String Piano(String s) {
        if (s == null) return "";
        return s.startsWith("<") ? " " + s : s;
    }
}
