package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Dialogo "Invia log" (Opzioni → Varie). Costruisce il testo da spedire in una delle due
 * modalit&agrave; (bundle diagnostico / log completo redatto), lo mostra in un'area
 * <b>modificabile</b>, e lo invia solo dopo una conferma esplicita. Ci&ograve; che parte
 * &egrave; esattamente il testo come l'utente lo lascia — nessun percorso parallelo
 * (nocommit/Documentazione/Analisi_Segnalazioni_Log.md, Decisione 4).
 *
 * <p>Finestra scritta a mano (nessun {@code .form}): il contenuto &egrave; dinamico (caselle dei
 * log ruotati, rigenerazione dell'anteprima). Se l'utente ha modificato il testo, cambiare
 * modalit&agrave; chiede conferma prima di rigenerare, cos&igrave; una modifica non viene persa
 * di nascosto.
 */
public class GUI_InviaLog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JRadioButton radioBundle = new JRadioButton("Bundle diagnostico (consigliato)", true);
    private final JRadioButton radioCompleto = new JRadioButton("Log completo redatto");
    private final JCheckBox[] caselleRuotati;
    private final JTextArea areaTesto = new JTextArea();
    private final JLabel etichettaInfo = new JLabel(" ");
    private final JCheckBox consenso = new JCheckBox("Ho controllato il contenuto qui sopra e acconsento all'invio");
    private final JButton bottoneInvia = new JButton("Invia segnalazione");

    /** true mentre {@link #rigenera()} scrive nell'area: evita di marcarla "modificata dall'utente". */
    private boolean rigenerazioneInCorso = false;
    private boolean modificatoDaUtente = false;
    /** Modalità con cui l'anteprima attualmente visibile è stata generata: {@code "bundle"} o {@code "log-completo"}. */
    private String modalitaCorrente = "bundle";

    /**
     * Debounce per la stima della dimensione compressa: {@link SegnalazioneBundle#gzip} sull'intero
     * testo (fino a ~25 MB in modalit&agrave; log completo con tutti i ruotati) &egrave; troppo
     * costoso da rifare a ogni battuta sull'EDT. Il conteggio caratteri &egrave; immediato; la
     * stima dei KB compressi arriva ~500 ms dopo l'ultima modifica.
     */
    private final Timer timerStima = new Timer(500, e -> aggiornaStima());

    public static void Mostra(Window proprietario) {
        GUI_InviaLog d = new GUI_InviaLog(proprietario);
        d.setLocationRelativeTo(proprietario);
        d.setVisible(true);
    }

    private GUI_InviaLog(Window proprietario) {
        super(proprietario, "Invia log per segnalazione", ModalityType.APPLICATION_MODAL);

        List<File> log = SegnalazioneBundle.fileLog();
        int nRuotati = Math.max(0, Math.min(SegnalazioneBundle.MAX_RUOTATI, log.size() - 1));
        caselleRuotati = new JCheckBox[nRuotati];

        // --- NORD: spiegazione + modalità + caselle log ruotati ---
        JPanel nord = new JPanel();
        nord.setLayout(new BoxLayout(nord, BoxLayout.Y_AXIS));
        nord.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        JLabel spiega = new JLabel("<html>Invii una copia dei log all'autore per la diagnosi di un problema. "
                + "Il testo &egrave; gi&agrave; ripulito dai dati identificabili (indirizzi, hash, percorsi, "
                + "credenziali) ma la ripulitura non &egrave; garantita: <b>controlla l'anteprima e togli "
                + "a mano ci&ograve; che non vuoi spedire</b>. Conservato al massimo 30 giorni, solo per "
                + "diagnosi.</html>");
        spiega.setAlignmentX(LEFT_ALIGNMENT);
        nord.add(spiega);
        nord.add(javax.swing.Box.createVerticalStrut(8));

        ButtonGroup gruppo = new ButtonGroup();
        gruppo.add(radioBundle);
        gruppo.add(radioCompleto);
        radioBundle.setAlignmentX(LEFT_ALIGNMENT);
        radioCompleto.setAlignmentX(LEFT_ALIGNMENT);
        nord.add(radioBundle);
        nord.add(radioCompleto);

        if (nRuotati > 0) {
            JPanel pRuotati = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            pRuotati.setAlignmentX(LEFT_ALIGNMENT);
            pRuotati.add(new JLabel("Includi anche i log precedenti:"));
            for (int i = 0; i < nRuotati; i++) {
                caselleRuotati[i] = new JCheckBox(log.get(i + 1).getName());
                caselleRuotati[i].setEnabled(false);
                caselleRuotati[i].addActionListener(e -> rigeneraSeConfermato());
                pRuotati.add(caselleRuotati[i]);
            }
            nord.add(javax.swing.Box.createVerticalStrut(4));
            nord.add(pRuotati);
        }
        add(nord, BorderLayout.NORTH);

        // --- CENTRO: anteprima modificabile ---
        areaTesto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaTesto.setLineWrap(false);
        JScrollPane scroll = new JScrollPane(areaTesto);
        scroll.setBorder(BorderFactory.createTitledBorder("Anteprima — verrà spedito esattamente questo testo"));
        add(scroll, BorderLayout.CENTER);

        // --- SUD: info dimensione + consenso + pulsanti ---
        JPanel sud = new JPanel(new BorderLayout());
        sud.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        etichettaInfo.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        sud.add(etichettaInfo, BorderLayout.NORTH);
        sud.add(consenso, BorderLayout.CENTER);

        JPanel pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton annulla = new JButton("Annulla");
        annulla.addActionListener(e -> dispose());
        bottoneInvia.setEnabled(false);
        bottoneInvia.addActionListener(e -> invia());
        pulsanti.add(annulla);
        pulsanti.add(bottoneInvia);
        sud.add(pulsanti, BorderLayout.SOUTH);
        add(sud, BorderLayout.SOUTH);

        // --- listener ---
        radioBundle.addActionListener(e -> cambioModalita());
        radioCompleto.addActionListener(e -> cambioModalita());
        consenso.addActionListener(e -> aggiornaStatoInvia());
        areaTesto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { tocco(); }
            @Override public void removeUpdate(DocumentEvent e) { tocco(); }
            @Override public void changedUpdate(DocumentEvent e) { tocco(); }
        });

        setSize(820, 620);
        timerStima.setRepeats(false);
        rigenera();
    }

    private void tocco() {
        if (!rigenerazioneInCorso) {
            modificatoDaUtente = true;
        }
        aggiornaConteggio();
        timerStima.restart();
        aggiornaStatoInvia();
    }

    private void aggiornaStatoInvia() {
        bottoneInvia.setEnabled(consenso.isSelected() && !areaTesto.getText().isBlank());
    }

    /** Solo il conteggio caratteri: immediato, adatto a ogni battuta. */
    private void aggiornaConteggio() {
        etichettaInfo.setText(String.format("%,d caratteri — stima compressione in corso... (limite 10 MB)",
                areaTesto.getText().length()));
    }

    /** Conteggio + stima della dimensione compressa (costosa: comprime tutto il testo). */
    private void aggiornaStima() {
        String testo = areaTesto.getText();
        int compressi = SegnalazioneBundle.gzip(testo).length;
        etichettaInfo.setText(String.format("%,d caratteri — circa %,d KB compressi (limite 10 MB)",
                testo.length(), Math.max(1, compressi / 1024)));
    }

    private void cambioModalita() {
        if (!rigeneraSeConfermato()) {
            // Rigenerazione rifiutata: riporta i radio alla modalità dell'anteprima visibile.
            radioBundle.setSelected("bundle".equals(modalitaCorrente));
            radioCompleto.setSelected("log-completo".equals(modalitaCorrente));
        }
        boolean completo = radioCompleto.isSelected();
        for (JCheckBox c : caselleRuotati) {
            if (c != null) {
                c.setEnabled(completo);
            }
        }
    }

    /** @return true se l'anteprima è stata (o poteva essere) rigenerata; false se l'utente ha rifiutato. */
    private boolean rigeneraSeConfermato() {
        if (modificatoDaUtente) {
            AppDialog.DialogResult r = AppDialog.builder(this)
                    .windowTitle("Rigenerare l'anteprima?")
                    .bodyTitle("Rigenerare l'anteprima?")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Hai modificato il testo a mano. Rigenerando l'anteprima quelle modifiche andranno perse.")
                    .action(AppDialog.DialogAction.builder("cancel", "Tieni le modifiche")
                            .role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("rigenera", "Rigenera")
                            .role(AppDialog.ActionRole.DANGER).build())
                    .showDialog();
            if (r == null || !r.isAction("rigenera")) {
                return false;
            }
        }
        rigenera();
        return true;
    }

    private void rigenera() {
        rigenerazioneInCorso = true;
        try {
            String testo;
            if (radioCompleto.isSelected()) {
                boolean[] incl = new boolean[caselleRuotati.length];
                for (int i = 0; i < caselleRuotati.length; i++) {
                    incl[i] = caselleRuotati[i] != null && caselleRuotati[i].isSelected();
                }
                testo = SegnalazioneBundle.logCompleto(incl);
            } else {
                testo = SegnalazioneBundle.bundleDiagnostico();
            }
            areaTesto.setText(testo);
            areaTesto.setCaretPosition(0);
            modificatoDaUtente = false;
            modalitaCorrente = radioCompleto.isSelected() ? "log-completo" : "bundle";
        } finally {
            rigenerazioneInCorso = false;
        }
        timerStima.stop();
        aggiornaStima();
        aggiornaStatoInvia();
    }

    private void invia() {
        byte[] gz = SegnalazioneBundle.gzip(areaTesto.getText());
        if (gz.length > 10 * 1024 * 1024) {
            Messaggi.WarningMessage("Troppo grande",
                    "Il testo supera i 10 MB una volta compresso. Usa il bundle diagnostico o togli qualche log allegato.",
                    this);
            return;
        }
        String modalita = modalitaCorrente;

        Download dow = new Download();
        dow.NascondiInterrompi();
        dow.MostraProgressAttesa("Invio segnalazione", "Invio del log in corso...");
        dow.SetLabel("Invio in corso, attendere...");
        dow.setLocationRelativeTo(this);

        final SegnalazioniClient.Esito[] esito = new SegnalazioniClient.Esito[1];
        Thread t = new Thread(() -> {
            try {
                esito[0] = SegnalazioniClient.inviaLog(gz, modalita, null);
            } catch (RuntimeException ex) {
                LoggerGC.ScriviErrore(ex);
                esito[0] = new SegnalazioniClient.Esito(false, 0, "Errore imprevisto: " + ex.getMessage());
            } finally {
                dow.ChiudiFinestra();
            }
        });
        t.start();
        dow.setVisible(true); // bloccante finché il thread chiama ChiudiFinestra

        SegnalazioniClient.Esito e = esito[0];
        if (e != null && e.ok()) {
            Messaggi.SuccessMessage("Inviata", e.messaggio(), this);
            dispose();
        } else {
            Messaggi.WarningMessage("Invio non riuscito",
                    e != null ? e.messaggio() : "Nessuna risposta dal servizio.", this);
        }
    }
}
