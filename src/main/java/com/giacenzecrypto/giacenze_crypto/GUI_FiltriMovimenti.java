package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.FiltriMovimenti;
import java.awt.Window;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 * Dialogo dei filtri della tabella dei movimenti crypto.
 *
 * <p>Raccoglie in un posto solo i criteri che decidono <b>quali righe la tabella contiene</b>, e che
 * finora stavano sparsi nella barra sopra la tabella (due combo e quattro caselle), pi&ugrave; il criterio
 * sul <b>documento di origine</b>, che una casella propria non l'ha mai avuta.
 *
 * <p>Due cose che non sono ovvie:
 *
 * <ul>
 *   <li><b>Le combo Wallet e Token non ricostruiscono i propri elenchi</b>: li copiano da quelle della
 *       barra, che {@code Principale.Funzione_AggiornaComboBox()} riempie durante il caricamento della
 *       tabella. Rigenerarli qui significherebbe due sorgenti della stessa lista, destinate a divergere.</li>
 *   <li><b>Il dialogo non applica nulla da s&eacute;.</b> Restituisce un {@link FiltriMovimenti} e chi lo ha
 *       aperto decide cosa farne: &egrave; ci&ograve; che permette a {@code Principale} di ricaricare la
 *       tabella <b>una volta sola</b>, mentre oggi ogni casella toccata nella barra ne fa ripartire una.</li>
 * </ul>
 *
 * <p>Le date non compaiono qui: non sono nella barra dei movimenti nemmeno adesso, arrivano dai due
 * selettori della scheda Crypto.com e sono condivise con quella. Spostarle &egrave; una modifica a
 * s&eacute;, da fare sapendo che riguarda anche l'altra scheda.
 *
 * @author luca.passelli
 */
public class GUI_FiltriMovimenti extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    /** Prefisso della voce che seleziona un documento preciso: quello che segue &egrave; l'id. */
    private static final String VOCE_TUTTI = "Tutti";
    private static final String VOCE_CON = "Solo movimenti con un documento";
    private static final String VOCE_SENZA = "Solo movimenti senza documento";

    /** Criteri scelti, {@code null} finch&eacute; l'utente non preme Applica. */
    private FiltriMovimenti Esito = null;

    /** Intervallo di date d'ingresso, riportato tale e quale nel record restituito. */
    private final long DataInizio;
    private final long DataFine;

    /** Id del documento corrispondente a ciascuna voce della combo; {@code null} per le prime tre. */
    private final java.util.List<String> DocumentiPerVoce = new java.util.ArrayList<>();

    /**
     * Apre il dialogo e restituisce i criteri scelti.
     *
     * @param Proprietario finestra su cui centrare il dialogo
     * @param Correnti criteri attivi in questo momento, che il dialogo mostra come punto di partenza
     * @param ComboWallet la combo dei wallet della barra, da cui copiare le voci
     * @param ComboToken la combo dei token della barra, da cui copiare le voci
     * @return i criteri scelti, oppure {@code null} se l'utente ha annullato
     */
    public static FiltriMovimenti Mostra(Window Proprietario, FiltriMovimenti Correnti,
            JComboBox<String> ComboWallet, JComboBox<String> ComboToken) {
        GUI_FiltriMovimenti d = new GUI_FiltriMovimenti(Proprietario, Correnti, ComboWallet, ComboToken);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d.Esito;
    }

    private GUI_FiltriMovimenti(Window Proprietario, FiltriMovimenti Correnti,
            JComboBox<String> ComboWallet, JComboBox<String> ComboToken) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        //Le date entrano ed escono immutate: il dialogo non le espone, ma il record deve restare
        //completo, altrimenti chi un domani lo salvasse cosi' com'e' si ritroverebbe l'intervallo a zero.
        DataInizio = (Correnti == null) ? 0 : Correnti.DataInizio();
        DataFine = (Correnti == null) ? 0 : Correnti.DataFine();
        initComponents();

        CopiaVoci(ComboWallet, ComboBox_Wallet);
        CopiaVoci(ComboToken, ComboBox_Token);
        RiempiDocumenti();

        if (Correnti != null) {
            SelezionaOTieni(ComboBox_Wallet, Correnti.Wallet());
            SelezionaOTieni(ComboBox_Token, Correnti.Token());
            SelezionaDocumento(Correnti.Documento());
            CheckBox_TrasferimentiInterni.setSelected(Correnti.NascondiTrasferimentiInterni());
            CheckBox_TokenScam.setSelected(Correnti.NascondiTokenScam());
            CheckBox_SenzaPrezzo.setSelected(Correnti.SoloSenzaPrezzo());
            CheckBox_LifoMancante.setSelected(Correnti.SoloLifoMancante());
        }

        Icone.AdattaIconeAlTema(this);
        getRootPane().setDefaultButton(Bottone_Ok);
        pack();
    }

    /** Copia le voci e la selezione da una combo della barra a quella del dialogo. */
    private static void CopiaVoci(JComboBox<String> Da, JComboBox<String> A) {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        if (Da != null) {
            for (int i = 0; i < Da.getItemCount(); i++) m.addElement(String.valueOf(Da.getItemAt(i)));
        }
        if (m.getSize() == 0) m.addElement(Principale_FiltriMovimenti.TUTTI);
        A.setModel(m);
    }

    /**
     * Riempie la combo dei documenti di origine: le tre voci generiche e poi un documento per riga, con
     * il numero di movimenti che vi puntano <b>adesso</b>.
     *
     * <p>Il conteggio viene da {@link DocumentiFonte#Riepiloghi()}, che percorre {@code MappaCryptoWallet}
     * una volta sola, e non dalla colonna {@code Movimenti} della tabella {@code DOCUMENTIFONTE}: quella
     * &egrave; storica — dice quanti movimenti l'importazione aveva aggiunto — e dopo qualche
     * cancellazione non corrisponde pi&ugrave; a quello che il filtro troverebbe.
     */
    private void RiempiDocumenti() {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        DocumentiPerVoce.clear();

        m.addElement(VOCE_TUTTI);
        DocumentiPerVoce.add(Principale_FiltriMovimenti.DOC_TUTTI);
        m.addElement(VOCE_CON);
        DocumentiPerVoce.add(Principale_FiltriMovimenti.DOC_CON);
        m.addElement(VOCE_SENZA);
        DocumentiPerVoce.add(Principale_FiltriMovimenti.DOC_SENZA);

        try {
            java.util.Map<Integer, DocumentiFonte.Riepilogo> conteggi = DocumentiFonte.Riepiloghi();
            List<DocumentiFonte.Documento> elenco = DocumentiFonte.Elenco();
            for (DocumentiFonte.Documento d : elenco) {
                DocumentiFonte.Riepilogo r = conteggi.get(d.Id);
                int n = (r == null) ? 0 : r.Movimenti;
                m.addElement(d.Id + " — " + d.NomeOriginale + "  (" + n + " movimenti)");
                DocumentiPerVoce.add(String.valueOf(d.Id));
            }
        } catch (Exception e) {
            //Un registro non leggibile non deve impedire di usare gli altri criteri: restano le tre voci
            //generiche, che non hanno bisogno del database.
            System.out.println("GUI_FiltriMovimenti.RiempiDocumenti : " + e.getMessage());
        }

        ComboBox_Documento.setModel(m);
    }

    /** Seleziona una voce se c'&egrave;, altrimenti lascia la selezione com'&egrave;. */
    private static void SelezionaOTieni(JComboBox<String> Combo, String Voce) {
        if (Voce == null) return;
        for (int i = 0; i < Combo.getItemCount(); i++) {
            if (Voce.equalsIgnoreCase(String.valueOf(Combo.getItemAt(i)))) {
                Combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Porta la combo dei documenti sul criterio indicato; se quel documento non c'&egrave; pi&ugrave;, su "Tutti". */
    private void SelezionaDocumento(String Criterio) {
        int i = DocumentiPerVoce.indexOf(Criterio == null ? Principale_FiltriMovimenti.DOC_TUTTI : Criterio);
        ComboBox_Documento.setSelectedIndex(i >= 0 ? i : 0);
    }

    /** @return il criterio sul documento corrispondente alla voce selezionata */
    private String DocumentoScelto() {
        int i = ComboBox_Documento.getSelectedIndex();
        if (i < 0 || i >= DocumentiPerVoce.size()) return Principale_FiltriMovimenti.DOC_TUTTI;
        return DocumentiPerVoce.get(i);
    }

    /**
     * @param DataInizio estremo inferiore da riportare nel record, che il dialogo non modifica
     * @param DataFine estremo superiore, idem
     * @return i criteri come sono impostati adesso nel dialogo
     */
    private FiltriMovimenti Leggi(long DataInizio, long DataFine) {
        return new FiltriMovimenti(
                String.valueOf(ComboBox_Wallet.getSelectedItem()),
                String.valueOf(ComboBox_Token.getSelectedItem()),
                DocumentoScelto(),
                DataInizio, DataFine,
                CheckBox_TrasferimentiInterni.isSelected(),
                CheckBox_TokenScam.isSelected(),
                CheckBox_SenzaPrezzo.isSelected(),
                CheckBox_LifoMancante.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        Pannello_Criteri = new javax.swing.JPanel();
        Label_Wallet = new javax.swing.JLabel();
        ComboBox_Wallet = new javax.swing.JComboBox();
        Label_Token = new javax.swing.JLabel();
        ComboBox_Token = new javax.swing.JComboBox();
        Label_Documento = new javax.swing.JLabel();
        ComboBox_Documento = new javax.swing.JComboBox();
        Separatore = new javax.swing.JSeparator();
        CheckBox_TrasferimentiInterni = new javax.swing.JCheckBox();
        CheckBox_TokenScam = new javax.swing.JCheckBox();
        CheckBox_SenzaPrezzo = new javax.swing.JCheckBox();
        CheckBox_LifoMancante = new javax.swing.JCheckBox();
        Pannello_Pulsanti = new javax.swing.JPanel();
        Bottone_Azzera = new javax.swing.JButton();
        Bottone_Annulla = new javax.swing.JButton();
        Bottone_Ok = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Filtri dei movimenti");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Pannello_Criteri.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14));
        Pannello_Criteri.setLayout(new java.awt.GridBagLayout());

        Label_Wallet.setText("Wallet / Gruppo :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Criteri.add(Label_Wallet, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Criteri.add(ComboBox_Wallet, gridBagConstraints);

        Label_Token.setText("Token :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Criteri.add(Label_Token, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Criteri.add(ComboBox_Token, gridBagConstraints);

        Label_Documento.setText("Documento di origine :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Criteri.add(Label_Documento, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Criteri.add(ComboBox_Documento, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 8, 0);
        Pannello_Criteri.add(Separatore, gridBagConstraints);

        CheckBox_TrasferimentiInterni.setText("Nascondi Trasferimenti Interni");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        Pannello_Criteri.add(CheckBox_TrasferimentiInterni, gridBagConstraints);

        CheckBox_TokenScam.setText("Nascondi Token SCAM");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        Pannello_Criteri.add(CheckBox_TokenScam, gridBagConstraints);

        CheckBox_SenzaPrezzo.setText("Mostra solo i movimenti senza prezzo");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        Pannello_Criteri.add(CheckBox_SenzaPrezzo, gridBagConstraints);

        CheckBox_LifoMancante.setText("Mostra solo i movimenti con LiFo mancante");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        Pannello_Criteri.add(CheckBox_LifoMancante, gridBagConstraints);

        getContentPane().add(Pannello_Criteri, java.awt.BorderLayout.CENTER);

        Pannello_Pulsanti.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));
        Pannello_Pulsanti.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        Bottone_Azzera.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Cestino.png"))); // NOI18N
        Bottone_Azzera.setText("Azzera");
        Bottone_Azzera.setToolTipText("Riporta tutti i criteri al valore che non filtra nulla");
        Bottone_Azzera.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AzzeraActionPerformed(evt);
            }
        });
        Pannello_Pulsanti.add(Bottone_Azzera);

        Bottone_Annulla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_Annulla.setText("Annulla");
        Bottone_Annulla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AnnullaActionPerformed(evt);
            }
        });
        Pannello_Pulsanti.add(Bottone_Annulla);

        Bottone_Ok.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Salva.png"))); // NOI18N
        Bottone_Ok.setText("Applica");
        Bottone_Ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_OkActionPerformed(evt);
            }
        });
        Pannello_Pulsanti.add(Bottone_Ok);

        getContentPane().add(Pannello_Pulsanti, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_AzzeraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AzzeraActionPerformed
        SelezionaOTieni(ComboBox_Wallet, Principale_FiltriMovimenti.TUTTI);
        SelezionaOTieni(ComboBox_Token, Principale_FiltriMovimenti.TUTTI);
        ComboBox_Documento.setSelectedIndex(0);
        CheckBox_TrasferimentiInterni.setSelected(false);
        CheckBox_TokenScam.setSelected(false);
        CheckBox_SenzaPrezzo.setSelected(false);
        CheckBox_LifoMancante.setSelected(false);
    }//GEN-LAST:event_Bottone_AzzeraActionPerformed

    private void Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AnnullaActionPerformed
        Esito = null;
        dispose();
    }//GEN-LAST:event_Bottone_AnnullaActionPerformed

    private void Bottone_OkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_OkActionPerformed
        //Le date restano quelle che erano: il dialogo non le espone, quindi non deve nemmeno poterle
        //cambiare per sbaglio.
        Esito = Leggi(DataInizio, DataFine);
        dispose();
    }//GEN-LAST:event_Bottone_OkActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Annulla;
    private javax.swing.JButton Bottone_Azzera;
    private javax.swing.JButton Bottone_Ok;
    private javax.swing.JCheckBox CheckBox_LifoMancante;
    private javax.swing.JCheckBox CheckBox_SenzaPrezzo;
    private javax.swing.JCheckBox CheckBox_TokenScam;
    private javax.swing.JCheckBox CheckBox_TrasferimentiInterni;
    private javax.swing.JComboBox ComboBox_Documento;
    private javax.swing.JComboBox ComboBox_Token;
    private javax.swing.JComboBox ComboBox_Wallet;
    private javax.swing.JLabel Label_Documento;
    private javax.swing.JLabel Label_Token;
    private javax.swing.JLabel Label_Wallet;
    private javax.swing.JPanel Pannello_Criteri;
    private javax.swing.JPanel Pannello_Pulsanti;
    private javax.swing.JSeparator Separatore;
    // End of variables declaration//GEN-END:variables
}
