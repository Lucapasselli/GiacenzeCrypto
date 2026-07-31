/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Dialogo della funzione "Chiedi a IA": mostra in anteprima il testo che verr&agrave; consegnato al chatbot
 * scelto, permette di cambiarne il profilo di riservatezza e lo consegna tramite gli appunti e/o l'URL.
 *
 * <p>Non ha un file {@code .form}: &egrave; scritto a mano perch&eacute; il contenuto (elenco chatbot,
 * anteprima ricalcolata) &egrave; dinamico.</p>
 *
 * <p>Il testo non viene mai inviato da solo: l'utente lo vede, pu&ograve; modificarlo e deve premere
 * esplicitamente il pulsante di apertura. Al primo utilizzo viene mostrato un avviso sui dati che escono
 * dal computer.</p>
 *
 * @author luca.passelli
 */
public class GUI_ChiediIA extends JDialog {

    private static final String OPZ_AVVISO = "IA_AvvisoPrivacy";
    private static final String OPZ_CHATBOT = "IA_Chatbot";
    private static final String OPZ_PROFILO = "IA_Profilo";
    private static final String OPZ_FISCALE = "IA_Fiscale";

    private final String ID;
    private final JComboBox<ChatbotIA.Bot> ComboChatbot = new JComboBox<>();
    private final JRadioButton RadioCompleto = new JRadioButton("Completo");
    private final JRadioButton RadioGenerico = new JRadioButton("Generico (senza dati riconducibili a me)");
    private final JCheckBox CheckFiscale = new JCheckBox("Chiedi anche l'inquadramento fiscale italiano");
    private final JTextArea AreaPrompt = new JTextArea(18, 76);
    private final JLabel LabelStato = new JLabel(" ");
    private final JButton BottoneApri = new JButton("Apri");

    /**
     * Mostra il dialogo per un movimento, previo avviso sui dati condivisi alla prima esecuzione.
     * @param ID identificativo del movimento selezionato, eventualmente {@code null}
     * @param Preselezionato chatbot scelto dal menu, eventualmente {@code null}
     * @param Proprietario finestra parent
     */
    public static void Mostra(String ID, ChatbotIA.Bot Preselezionato, Window Proprietario) {
        if (ID == null || Principale.MappaCryptoWallet.get(ID) == null) {
            Messaggi.WarningMessage("Chiedi a IA",
                    "Seleziona prima un movimento: la domanda viene costruita sui suoi dati.", Proprietario);
            return;
        }
        if (!AvvisoAccettato(Proprietario)) return;

        GUI_ChiediIA d = new GUI_ChiediIA(ID, Preselezionato, Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    /**
     * Mostra, se non &egrave; ancora stato accettato, l'avviso sui dati che vengono consegnati al chatbot e
     * memorizza la scelta dell'utente.
     * @param Proprietario finestra parent dell'avviso
     * @return {@code true} se l'utente ha accettato (ora o in passato)
     */
    private static boolean AvvisoAccettato(Window Proprietario) {
        if ("SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZ_AVVISO, "NO"))) return true;

        AppDialog.DialogResult r = AppDialog.builder(Proprietario)
                .windowTitle("Chiedi a IA")
                .bodyTitle("I dati escono dal tuo computer")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("La domanda viene consegnata a un servizio di chat esterno, che non è gestito da "
                        + "questo programma.")
                .details("Con il profilo Completo il testo contiene hash della transazione, indirizzi wallet, "
                        + "quantità e controvalore: sono dati che identificano il tuo portafoglio e che, una "
                        + "volta inviati, possono essere conservati dal servizio.\n\n"
                        + "Il profilo Generico invia solo causale, simboli delle monete, rete e nome della "
                        + "piattaforma (mai un indirizzo).\n\n"
                        + "Vedrai sempre il testo prima di inviarlo e potrai modificarlo. Le risposte del "
                        + "chatbot non sono una consulenza fiscale.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("procedi", "Ho capito, procedi")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (!r.isAction("procedi")) return false;
        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_AVVISO, "SI");
        return true;
    }

    /**
     * @param ID identificativo del movimento su cui costruire la domanda
     * @param Preselezionato chatbot scelto dal menu, eventualmente {@code null}
     * @param Proprietario finestra parent
     */
    private GUI_ChiediIA(String ID, ChatbotIA.Bot Preselezionato, Window Proprietario) {
        super(Proprietario, "Chiedi a IA", ModalityType.APPLICATION_MODAL);
        this.ID = ID;

        for (ChatbotIA.Bot b : ChatbotIA.Lista()) ComboChatbot.addItem(b);
        ChatbotIA.Bot iniziale = Preselezionato != null
                ? Preselezionato
                : ChatbotIA.CercaPerNome(DatabaseH2.Pers_Opzioni_Leggi(OPZ_CHATBOT, ""));
        ComboChatbot.setSelectedItem(iniziale);

        boolean generico = "GENERICO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZ_PROFILO, "COMPLETO"));
        RadioCompleto.setSelected(!generico);
        RadioGenerico.setSelected(generico);
        ButtonGroup gruppo = new ButtonGroup();
        gruppo.add(RadioCompleto);
        gruppo.add(RadioGenerico);

        CheckFiscale.setSelected(!"NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZ_FISCALE, "SI")));

        RadioCompleto.setToolTipText("Tutti i dati del movimento: hash, indirizzi, quantità e controvalore. "
                + "È il profilo che permette al chatbot di leggere la transazione sull'explorer.");
        RadioGenerico.setToolTipText("Solo piattaforma, causale e simboli delle monete: niente hash, "
                + "indirizzi, importi o date.");

        add(PannelloOpzioni(), BorderLayout.NORTH);
        add(PannelloAnteprima(), BorderLayout.CENTER);
        add(PannelloBottoni(), BorderLayout.SOUTH);

        ComboChatbot.addActionListener(e -> AggiornaStato());
        RadioCompleto.addActionListener(e -> AggiornaPrompt());
        RadioGenerico.addActionListener(e -> AggiornaPrompt());
        CheckFiscale.addActionListener(e -> AggiornaPrompt());

        AggiornaPrompt();
        pack();
        setMinimumSize(new Dimension(640, 480));
    }

    /**
     * @return il pannello superiore con la scelta del chatbot, del profilo e dell'inquadramento fiscale
     */
    private JPanel PannelloOpzioni() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Chatbot:"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        p.add(ComboChatbot, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Dati da inviare:"), c);
        c.gridx = 1;
        p.add(RadioCompleto, c);
        c.gridx = 2;
        p.add(RadioGenerico, c);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        p.add(CheckFiscale, c);

        //riempitivo a destra: senza di esso GridBagLayout centrerebbe tutte le opzioni nel pannello
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JLabel(), c);
        return p;
    }

    /**
     * @return il pannello centrale con l'anteprima modificabile della domanda
     */
    private JPanel PannelloAnteprima() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        p.add(new JLabel("Testo che verrà consegnato al chatbot (puoi modificarlo):"), BorderLayout.NORTH);

        AreaPrompt.setLineWrap(true);
        AreaPrompt.setWrapStyleWord(true);
        //il conteggio caratteri deve seguire anche le modifiche fatte a mano nell'anteprima
        AreaPrompt.getDocument().addDocumentListener(new DocumentListener() {
            /** @param e evento di inserimento */
            @Override public void insertUpdate(DocumentEvent e) { AggiornaStato(); }
            /** @param e evento di cancellazione */
            @Override public void removeUpdate(DocumentEvent e) { AggiornaStato(); }
            /** @param e evento di modifica degli attributi */
            @Override public void changedUpdate(DocumentEvent e) { AggiornaStato(); }
        });
        p.add(new JScrollPane(AreaPrompt), BorderLayout.CENTER);
        return p;
    }

    /**
     * @return il pannello inferiore con lo stato e i pulsanti di azione
     */
    private JPanel PannelloBottoni() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        p.add(LabelStato, BorderLayout.WEST);

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton bCopia = new JButton("Copia negli appunti");
        bCopia.addActionListener(e -> {
            Copia();
            LabelStato.setText("Testo copiato negli appunti.");
        });
        JButton bChiudi = new JButton("Chiudi");
        bChiudi.addActionListener(e -> dispose());

        BottoneApri.addActionListener(e -> Apri());
        destra.add(bCopia);
        destra.add(BottoneApri);
        destra.add(bChiudi);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    /** @return il profilo attualmente selezionato nel dialogo */
    private PromptIA.Profilo ProfiloSelezionato() {
        return RadioGenerico.isSelected() ? PromptIA.Profilo.GENERICO : PromptIA.Profilo.COMPLETO;
    }

    /** Ricostruisce il testo della domanda in base alle opzioni selezionate e aggiorna lo stato. */
    private void AggiornaPrompt() {
        String testo = PromptIA.Costruisci(ID, ProfiloSelezionato(), CheckFiscale.isSelected());
        AreaPrompt.setText(testo == null ? "" : testo);
        AreaPrompt.setCaretPosition(0);
        AggiornaStato();
    }

    /**
     * Aggiorna la riga di stato indicando la lunghezza del testo e il modo in cui verr&agrave; consegnato al
     * chatbot selezionato: direttamente nell'indirizzo oppure tramite gli appunti.
     */
    private void AggiornaStato() {
        ChatbotIA.Bot b = (ChatbotIA.Bot) ComboChatbot.getSelectedItem();
        if (b == null) return;
        BottoneApri.setText("Apri " + b.Nome);

        int caratteri = AreaPrompt.getText().length();
        if (ChatbotIA.UrlConDomanda(b, AreaPrompt.getText()) != null) {
            LabelStato.setText(caratteri + " caratteri - la domanda verrà inserita direttamente nella chat.");
        } else {
            LabelStato.setText(caratteri + " caratteri - il testo verrà copiato: incollalo con Ctrl+V nella chat.");
        }
    }

    /** Copia negli appunti il testo attualmente presente nell'anteprima. */
    private void Copia() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(AreaPrompt.getText()), null);
    }

    /**
     * Consegna la domanda al chatbot selezionato: la copia sempre negli appunti (cos&igrave; resta
     * disponibile anche se il prefill non funziona) e apre il browser sull'indirizzo con la domanda, oppure
     * sulla sola pagina della chat quando il testo &egrave; troppo lungo o il chatbot non supporta il prefill.
     */
    private void Apri() {
        ChatbotIA.Bot b = (ChatbotIA.Bot) ComboChatbot.getSelectedItem();
        if (b == null) return;

        Copia();
        String url = ChatbotIA.UrlConDomanda(b, AreaPrompt.getText());
        Funzioni.ApriWeb(url != null ? url : b.Url);

        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_CHATBOT, b.Nome);
        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_PROFILO, ProfiloSelezionato().name());
        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_FISCALE, CheckFiscale.isSelected() ? "SI" : "NO");
        dispose();
    }
}
