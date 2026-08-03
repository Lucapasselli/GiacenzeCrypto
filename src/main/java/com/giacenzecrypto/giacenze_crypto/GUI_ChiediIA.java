/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Dialogo della funzione "Chiedi a IA": mostra in anteprima il testo che verr&agrave; consegnato al chatbot
 * scelto, permette di cambiarne il profilo di riservatezza e lo consegna tramite gli appunti e/o l'URL.
 *
 * <p>La finestra &egrave; costruita con il GUI Builder di NetBeans ({@code GUI_ChiediIA.form}). Quello che
 * &egrave; dinamico resta fuori da {@code initComponents()}: l'elenco dei chatbot viene letto da
 * {@link ChatbotIA} e caricato nella combo a runtime, e cos&igrave; il testo dell'anteprima, la riga di
 * stato e l'etichetta del pulsante di apertura, che dipendono dalle scelte fatte nel dialogo.</p>
 *
 * <p>Il testo non viene mai inviato da solo: l'utente lo vede, pu&ograve; modificarlo e deve premere
 * esplicitamente il pulsante di apertura. Al primo utilizzo viene mostrato un avviso sui dati che escono
 * dal computer.</p>
 *
 * @author luca.passelli
 */
public class GUI_ChiediIA extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private static final String OPZ_AVVISO = "IA_AvvisoPrivacy";
    private static final String OPZ_CHATBOT = "IA_Chatbot";
    private static final String OPZ_PROFILO = "IA_Profilo";
    private static final String OPZ_FISCALE = "IA_Fiscale";

    private final String ID;

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
     * Creates new form GUI_ChiediIA
     * @param ID identificativo del movimento su cui costruire la domanda
     * @param Preselezionato chatbot scelto dal menu, eventualmente {@code null}
     * @param Proprietario finestra parent
     */
    private GUI_ChiediIA(String ID, ChatbotIA.Bot Preselezionato, Window Proprietario) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        this.ID = ID;
        initComponents();

        //L'elenco dei chatbot arriva da ChatbotIA.json e cambia da un'installazione all'altra: la combo
        //viene quindi creata vuota nel form e riempita qui.
        for (ChatbotIA.Bot b : ChatbotIA.Lista()) ComboBox_Chatbot.addItem(b);
        ChatbotIA.Bot iniziale = Preselezionato != null
                ? Preselezionato
                : ChatbotIA.CercaPerNome(DatabaseH2.Pers_Opzioni_Leggi(OPZ_CHATBOT, ""));
        ComboBox_Chatbot.setSelectedItem(iniziale);

        boolean generico = "GENERICO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZ_PROFILO, "COMPLETO"));
        RadioButton_Completo.setSelected(!generico);
        RadioButton_Generico.setSelected(generico);
        CheckBox_Fiscale.setSelected(!"NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZ_FISCALE, "SI")));

        //Il conteggio caratteri deve seguire anche le modifiche fatte a mano nell'anteprima. Il listener
        //resta scritto a mano: il GUI Builder non modella gli eventi del Document.
        TextArea_Prompt.getDocument().addDocumentListener(new DocumentListener() {
            /** @param e evento di inserimento */
            @Override public void insertUpdate(DocumentEvent e) { AggiornaStato(); }
            /** @param e evento di cancellazione */
            @Override public void removeUpdate(DocumentEvent e) { AggiornaStato(); }
            /** @param e evento di modifica degli attributi */
            @Override public void changedUpdate(DocumentEvent e) { AggiornaStato(); }
        });

        AggiornaPrompt();
        pack();
        setMinimumSize(new Dimension(640, 480));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        GruppoProfilo = new javax.swing.ButtonGroup();
        Label_Chatbot = new javax.swing.JLabel();
        ComboBox_Chatbot = new javax.swing.JComboBox<>();
        Label_Dati = new javax.swing.JLabel();
        RadioButton_Completo = new javax.swing.JRadioButton();
        RadioButton_Generico = new javax.swing.JRadioButton();
        CheckBox_Fiscale = new javax.swing.JCheckBox();
        Label_Anteprima = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TextArea_Prompt = new javax.swing.JTextArea();
        Label_Stato = new javax.swing.JLabel();
        Bottone_Copia = new javax.swing.JButton();
        Bottone_Apri = new javax.swing.JButton();
        Bottone_Chiudi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Chiedi a IA");

        Label_Chatbot.setText("Chatbot :");

        ComboBox_Chatbot.setToolTipText("L'elenco viene letto dal file ChatbotIA.json nella cartella dei dati");
        ComboBox_Chatbot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboBox_ChatbotActionPerformed(evt);
            }
        });

        Label_Dati.setText("Dati da inviare :");

        GruppoProfilo.add(RadioButton_Completo);
        RadioButton_Completo.setSelected(true);
        RadioButton_Completo.setText("Completo");
        RadioButton_Completo.setToolTipText("Tutti i dati del movimento: hash, indirizzi, quantità e controvalore. È il profilo che permette al chatbot di leggere la transazione sull'explorer.");
        RadioButton_Completo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RadioButton_CompletoActionPerformed(evt);
            }
        });

        GruppoProfilo.add(RadioButton_Generico);
        RadioButton_Generico.setText("Generico (senza dati riconducibili a me)");
        RadioButton_Generico.setToolTipText("Solo piattaforma, causale e simboli delle monete: niente hash, indirizzi, importi o date.");
        RadioButton_Generico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RadioButton_GenericoActionPerformed(evt);
            }
        });

        CheckBox_Fiscale.setText("Chiedi anche l'inquadramento fiscale italiano");
        CheckBox_Fiscale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckBox_FiscaleActionPerformed(evt);
            }
        });

        Label_Anteprima.setText("Testo che verrà consegnato al chatbot (puoi modificarlo) :");

        TextArea_Prompt.setColumns(76);
        TextArea_Prompt.setLineWrap(true);
        TextArea_Prompt.setRows(18);
        TextArea_Prompt.setWrapStyleWord(true);
        jScrollPane1.setViewportView(TextArea_Prompt);

        Label_Stato.setText(" ");

        Bottone_Copia.setText("Copia negli appunti");
        Bottone_Copia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_CopiaActionPerformed(evt);
            }
        });

        Bottone_Apri.setText("Apri");
        Bottone_Apri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ApriActionPerformed(evt);
            }
        });

        Bottone_Chiudi.setText("Chiudi");
        Bottone_Chiudi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ChiudiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                    .addComponent(Label_Anteprima)
                    .addComponent(CheckBox_Fiscale)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Label_Chatbot)
                            .addComponent(Label_Dati))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ComboBox_Chatbot, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(RadioButton_Completo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(RadioButton_Generico))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Label_Stato)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Copia)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Apri)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Chiudi)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_Chatbot)
                    .addComponent(ComboBox_Chatbot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_Dati)
                    .addComponent(RadioButton_Completo)
                    .addComponent(RadioButton_Generico))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CheckBox_Fiscale)
                .addGap(18, 18, 18)
                .addComponent(Label_Anteprima)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_Stato)
                    .addComponent(Bottone_Copia)
                    .addComponent(Bottone_Apri)
                    .addComponent(Bottone_Chiudi))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ComboBox_ChatbotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ComboBox_ChatbotActionPerformed
        //Il testo non cambia col chatbot: cambia solo il modo in cui gli verrà consegnato
        AggiornaStato();
    }//GEN-LAST:event_ComboBox_ChatbotActionPerformed

    private void RadioButton_CompletoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RadioButton_CompletoActionPerformed
        AggiornaPrompt();
    }//GEN-LAST:event_RadioButton_CompletoActionPerformed

    private void RadioButton_GenericoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RadioButton_GenericoActionPerformed
        AggiornaPrompt();
    }//GEN-LAST:event_RadioButton_GenericoActionPerformed

    private void CheckBox_FiscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckBox_FiscaleActionPerformed
        AggiornaPrompt();
    }//GEN-LAST:event_CheckBox_FiscaleActionPerformed

    private void Bottone_CopiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_CopiaActionPerformed
        Copia();
        Label_Stato.setText("Testo copiato negli appunti.");
    }//GEN-LAST:event_Bottone_CopiaActionPerformed

    private void Bottone_ApriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ApriActionPerformed
        Apri();
    }//GEN-LAST:event_Bottone_ApriActionPerformed

    private void Bottone_ChiudiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ChiudiActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_ChiudiActionPerformed

    /** @return il profilo attualmente selezionato nel dialogo */
    private PromptIA.Profilo ProfiloSelezionato() {
        return RadioButton_Generico.isSelected() ? PromptIA.Profilo.GENERICO : PromptIA.Profilo.COMPLETO;
    }

    /** Ricostruisce il testo della domanda in base alle opzioni selezionate e aggiorna lo stato. */
    private void AggiornaPrompt() {
        String testo = PromptIA.Costruisci(ID, ProfiloSelezionato(), CheckBox_Fiscale.isSelected());
        TextArea_Prompt.setText(testo == null ? "" : testo);
        TextArea_Prompt.setCaretPosition(0);
        AggiornaStato();
    }

    /**
     * Aggiorna la riga di stato indicando la lunghezza del testo e il modo in cui verr&agrave; consegnato al
     * chatbot selezionato: direttamente nell'indirizzo oppure tramite gli appunti.
     */
    private void AggiornaStato() {
        ChatbotIA.Bot b = (ChatbotIA.Bot) ComboBox_Chatbot.getSelectedItem();
        if (b == null) return;
        Bottone_Apri.setText("Apri " + b.Nome);

        int caratteri = TextArea_Prompt.getText().length();
        if (ChatbotIA.UrlConDomanda(b, TextArea_Prompt.getText()) != null) {
            Label_Stato.setText(caratteri + " caratteri - la domanda verrà inserita direttamente nella chat.");
        } else {
            Label_Stato.setText(caratteri + " caratteri - il testo verrà copiato: incollalo con Ctrl+V nella chat.");
        }
    }

    /** Copia negli appunti il testo attualmente presente nell'anteprima. */
    private void Copia() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(TextArea_Prompt.getText()), null);
    }

    /**
     * Consegna la domanda al chatbot selezionato: la copia sempre negli appunti (cos&igrave; resta
     * disponibile anche se il prefill non funziona) e apre il browser sull'indirizzo con la domanda, oppure
     * sulla sola pagina della chat quando il testo &egrave; troppo lungo o il chatbot non supporta il prefill.
     */
    private void Apri() {
        ChatbotIA.Bot b = (ChatbotIA.Bot) ComboBox_Chatbot.getSelectedItem();
        if (b == null) return;

        Copia();
        String url = ChatbotIA.UrlConDomanda(b, TextArea_Prompt.getText());
        Funzioni.ApriWeb(url != null ? url : b.Url);

        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_CHATBOT, b.Nome);
        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_PROFILO, ProfiloSelezionato().name());
        DatabaseH2.Pers_Opzioni_Scrivi(OPZ_FISCALE, CheckBox_Fiscale.isSelected() ? "SI" : "NO");
        dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Apri;
    private javax.swing.JButton Bottone_Chiudi;
    private javax.swing.JButton Bottone_Copia;
    private javax.swing.JCheckBox CheckBox_Fiscale;
    private javax.swing.JComboBox<ChatbotIA.Bot> ComboBox_Chatbot;
    private javax.swing.ButtonGroup GruppoProfilo;
    private javax.swing.JLabel Label_Anteprima;
    private javax.swing.JLabel Label_Chatbot;
    private javax.swing.JLabel Label_Dati;
    private javax.swing.JLabel Label_Stato;
    private javax.swing.JRadioButton RadioButton_Completo;
    private javax.swing.JRadioButton RadioButton_Generico;
    private javax.swing.JTextArea TextArea_Prompt;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
