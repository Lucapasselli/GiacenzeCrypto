/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.util.List;

import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COLONNE_PERIODO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_BOLLO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_CHIAVE_DEFAULT;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_DATA_FINE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_DATA_INIZIO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_MOD_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_MOD_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_FONTE_FISCALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_IDENT_FISCALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_IDENT_ISEE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_NOTA_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_NOTA_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_NOTE_FISCALI;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_ORIGINE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_STATO_ESTERO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_PROGRESSIVO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_TIPO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_VAL_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_VAL_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.ORIGINE_UTENTE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_CRYPTO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_FIAT;

/**
 * Dialogo dedicato per aggiungere / modificare <b>un periodo di detenzione di un gruppo wallet</b>
 * ({@code GRUPPO_PERIODO_RW}). Sostituisce l'editing in cella di {@link GUI_PeriodiDetenzioneRW}.
 * La riga prodotta ({@link #risultato}, {@link Principale_GruppiWalletRW#COLONNE_PERIODO} colonne)
 * ha {@code Origine = UTENTE}; le colonne "Calcolo" contengono i <b>codici</b>, non le etichette.
 *
 * <p>I campi abilitati dipendono dal tipo : il bollo solo su CRYPTO, i dati fiscali (Stato estero,
 * "è conto corrente", identificativo, alias ISEE, note, fonte) solo su FIAT — vedi
 * {@link #aggiornaAbilitazioni()}. I
 * valori iniziale/finale a mano non si modificano più da qui (2026-09-09) : la riga esistente li
 * <b>conserva</b>, così una riga che li aveva non li perde passando dal dialogo.</p>
 */
public class GUI_ModificaPeriodoDetenzione extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final String chiaveDefault;
    /** Valori campo 7/8 a mano : non più modificabili qui, ma conservati (vedi javadoc di classe). */
    private final String valIniziale, notaIniziale, valFinale, notaFinale;

    public boolean confermato = false;
    public String[] risultato;

    public GUI_ModificaPeriodoDetenzione(java.awt.Window owner, String[] rigaEsistente,
            int progressivoProposto, String bolloDefault) {
        super(owner, ModalityType.APPLICATION_MODAL);
        try {
            setIconImage(new javax.swing.ImageIcon(VarStatiche.getPathRisorse() + "logo.png").getImage());
        } catch (RuntimeException ignore) {
        }
        initComponents();

        boolean nuovo = rigaEsistente == null;
        setTitle(nuovo ? "Nuovo periodo di detenzione" : "Modifica periodo di detenzione");

        RwDialoghi.configura(Data_Inizio);
        RwDialoghi.configura(Data_Fine);
        Combo_Tipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{TIPO_CRYPTO, TIPO_FIAT}));
        Combo_ModIni.setModel(new javax.swing.DefaultComboBoxModel<>(etichette(MODALITA_INIZIALE)));
        Combo_ModFin.setModel(new javax.swing.DefaultComboBoxModel<>(etichette(MODALITA_FINALE)));
        Combo_Bollo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
            Principale_GruppiWalletRW.BOLLO_NO, Principale_GruppiWalletRW.BOLLO_SI}));
        Combo_ContoCorrente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
            Principale_GruppiWalletRW.CONTO_CORRENTE_NO, Principale_GruppiWalletRW.CONTO_CORRENTE_SI}));
        String statoCorrente = nuovo ? "" : val(rigaEsistente, COL_STATO_ESTERO);
        for (String voce : StatiEsteri.etichetteCombo(statoCorrente)) {
            Combo_Stato.addItem(voce);
        }
        Combo_Stato.setSelectedItem(StatiEsteri.etichetta(statoCorrente));

        String tipo = nuovo ? TIPO_CRYPTO : val(rigaEsistente, COL_TIPO);
        Combo_Tipo.setSelectedItem(TIPO_FIAT.equals(tipo) ? TIPO_FIAT : TIPO_CRYPTO);
        Campo_Prog.setText(nuovo ? String.valueOf(progressivoProposto) : val(rigaEsistente, COL_PROGRESSIVO));
        RwDialoghi.impostaData(Data_Inizio, nuovo ? "" : val(rigaEsistente, COL_DATA_INIZIO));
        RwDialoghi.impostaData(Data_Fine, nuovo ? "" : val(rigaEsistente, COL_DATA_FINE));
        Campo_Ident.setText(nuovo ? "" : val(rigaEsistente, COL_IDENT_FISCALE));
        Campo_Isee.setText(nuovo ? "" : val(rigaEsistente, COL_IDENT_ISEE));
        Campo_Note.setText(nuovo ? "" : val(rigaEsistente, COL_NOTE_FISCALI));
        Campo_Fonte.setText(nuovo ? "" : val(rigaEsistente, COL_FONTE_FISCALE));
        // Non si modificano più da qui, ma non si perdono : li riporto tali e quali nel risultato.
        this.valIniziale = nuovo ? "" : val(rigaEsistente, COL_VAL_INIZIALE);
        this.notaIniziale = nuovo ? "" : val(rigaEsistente, COL_NOTA_INIZIALE);
        this.valFinale = nuovo ? "" : val(rigaEsistente, COL_VAL_FINALE);
        this.notaFinale = nuovo ? "" : val(rigaEsistente, COL_NOTA_FINALE);
        Combo_ModIni.setSelectedItem(Principale_GruppiWalletRW.etichettaModalita(
                nuovo ? "" : val(rigaEsistente, COL_MOD_INIZIALE), MODALITA_INIZIALE));
        Combo_ModFin.setSelectedItem(Principale_GruppiWalletRW.etichettaModalita(
                nuovo ? "" : val(rigaEsistente, COL_MOD_FINALE), MODALITA_FINALE));
        String bollo = nuovo ? bolloDefault : val(rigaEsistente, COL_BOLLO);
        Combo_Bollo.setSelectedItem(Principale_GruppiWalletRW.BOLLO_SI.equals(bollo)
                ? Principale_GruppiWalletRW.BOLLO_SI : Principale_GruppiWalletRW.BOLLO_NO);
        String contoCorrente = nuovo ? "" : val(rigaEsistente, COL_E_CONTO_CORRENTE);
        Combo_ContoCorrente.setSelectedItem(Principale_GruppiWalletRW.CONTO_CORRENTE_SI.equals(contoCorrente)
                ? Principale_GruppiWalletRW.CONTO_CORRENTE_SI : Principale_GruppiWalletRW.CONTO_CORRENTE_NO);
        this.chiaveDefault = nuovo ? "" : val(rigaEsistente, COL_CHIAVE_DEFAULT);

        aggiornaAbilitazioni();
        getRootPane().setDefaultButton(Bottone_Ok);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Bollo solo sui righi CRYPTO, dati fiscali solo sui righi FIAT. */
    private void aggiornaAbilitazioni() {
        boolean fiat = TIPO_FIAT.equals(Combo_Tipo.getSelectedItem());
        Combo_Bollo.setEnabled(!fiat);
        Combo_Stato.setEnabled(fiat);
        Combo_ContoCorrente.setEnabled(fiat);
        Campo_Ident.setEnabled(fiat);
        Campo_Isee.setEnabled(fiat);
        Campo_Note.setEnabled(fiat);
        Campo_Fonte.setEnabled(fiat);
    }

    private static String[] etichette(String[][] tabella) {
        String[] e = new String[tabella.length];
        for (int i = 0; i < tabella.length; i++) {
            e[i] = tabella[i][1];
        }
        return e;
    }

    private static String val(String[] r, int i) {
        return r != null && i >= 0 && i < r.length && r[i] != null ? r[i] : "";
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

        Pannello_Campi = new javax.swing.JPanel();
        Label_Tipo = new javax.swing.JLabel();
        Combo_Tipo = new javax.swing.JComboBox();
        Label_Prog = new javax.swing.JLabel();
        Campo_Prog = new javax.swing.JTextField();
        Label_DataInizio = new javax.swing.JLabel();
        Data_Inizio = new com.toedter.calendar.JDateChooser();
        Label_DataFine = new javax.swing.JLabel();
        Data_Fine = new com.toedter.calendar.JDateChooser();
        Label_ModIni = new javax.swing.JLabel();
        Combo_ModIni = new javax.swing.JComboBox();
        Label_ModFin = new javax.swing.JLabel();
        Combo_ModFin = new javax.swing.JComboBox();
        Label_Bollo = new javax.swing.JLabel();
        Combo_Bollo = new javax.swing.JComboBox();
        Label_Stato = new javax.swing.JLabel();
        Combo_Stato = new javax.swing.JComboBox();
        Label_ContoCorrente = new javax.swing.JLabel();
        Combo_ContoCorrente = new javax.swing.JComboBox();
        Label_Ident = new javax.swing.JLabel();
        Campo_Ident = new javax.swing.JTextField();
        Label_Isee = new javax.swing.JLabel();
        Campo_Isee = new javax.swing.JTextField();
        Label_Note = new javax.swing.JLabel();
        Scroll_Note = new javax.swing.JScrollPane();
        Campo_Note = new javax.swing.JTextArea();
        Label_Fonte = new javax.swing.JLabel();
        Scroll_Fonte = new javax.swing.JScrollPane();
        Campo_Fonte = new javax.swing.JTextArea();
        Pannello_Pulsanti = new javax.swing.JPanel();
        Bottone_Ok = new javax.swing.JButton();
        Bottone_Annulla = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Modifica periodo di detenzione");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Pannello_Campi.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 8, 12));
        Pannello_Campi.setLayout(new java.awt.GridBagLayout());

        Label_Tipo.setText("Tipo :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Tipo, gridBagConstraints);

        Combo_Tipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Combo_TipoActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_Tipo, gridBagConstraints);

        Label_Prog.setText("Progressivo :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Prog, gridBagConstraints);

        Campo_Prog.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Campo_Prog, gridBagConstraints);

        Label_DataInizio.setText("Data inizio (vuota = dedotta) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_DataInizio, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Data_Inizio, gridBagConstraints);

        Label_DataFine.setText("Data fine (vuota = periodo aperto) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_DataFine, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Data_Fine, gridBagConstraints);

        Label_ModIni.setText("Calcolo iniziale :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_ModIni, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_ModIni, gridBagConstraints);

        Label_ModFin.setText("Calcolo finale :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_ModFin, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_ModFin, gridBagConstraints);

        Label_Bollo.setText("Bollo exchange (solo CRYPTO) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Bollo, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_Bollo, gridBagConstraints);

        Label_Stato.setText("Stato estero (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Stato, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_Stato, gridBagConstraints);

        Label_ContoCorrente.setText("È conto corrente estero (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_ContoCorrente, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Combo_ContoCorrente, gridBagConstraints);

        Label_Ident.setText("Identificativo fiscale (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Ident, gridBagConstraints);

        Campo_Ident.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Campo_Ident, gridBagConstraints);

        Label_Isee.setText("Identificativo ISEE (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Isee, gridBagConstraints);

        Campo_Isee.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Campo_Isee, gridBagConstraints);

        Label_Note.setText("Note (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 10);
        Pannello_Campi.add(Label_Note, gridBagConstraints);

        Campo_Note.setColumns(28);
        Campo_Note.setLineWrap(true);
        Campo_Note.setRows(2);
        Campo_Note.setWrapStyleWord(true);
        Scroll_Note.setViewportView(Campo_Note);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        Pannello_Campi.add(Scroll_Note, gridBagConstraints);

        Label_Fonte.setText("Fonte (solo FIAT) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        Pannello_Campi.add(Label_Fonte, gridBagConstraints);

        Campo_Fonte.setColumns(28);
        Campo_Fonte.setLineWrap(true);
        Campo_Fonte.setRows(2);
        Campo_Fonte.setWrapStyleWord(true);
        Scroll_Fonte.setViewportView(Campo_Fonte);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        Pannello_Campi.add(Scroll_Fonte, gridBagConstraints);

        getContentPane().add(Pannello_Campi, java.awt.BorderLayout.CENTER);

        Pannello_Pulsanti.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));
        Pannello_Pulsanti.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        Bottone_Ok.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Salva.png"))); // NOI18N
        Bottone_Ok.setText("OK");
        Bottone_Ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_OkActionPerformed(evt);
            }
        });
        Pannello_Pulsanti.add(Bottone_Ok);

        Bottone_Annulla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_Annulla.setText("Annulla");
        Bottone_Annulla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AnnullaActionPerformed(evt);
            }
        });
        Pannello_Pulsanti.add(Bottone_Annulla);

        getContentPane().add(Pannello_Pulsanti, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Combo_TipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Combo_TipoActionPerformed
        aggiornaAbilitazioni();
    }//GEN-LAST:event_Combo_TipoActionPerformed

    private void Bottone_OkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_OkActionPerformed
        String tipo = TIPO_FIAT.equals(Combo_Tipo.getSelectedItem()) ? TIPO_FIAT : TIPO_CRYPTO;
        boolean fiat = TIPO_FIAT.equals(tipo);
        String[] r = new String[COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[COL_TIPO] = tipo;
        r[COL_PROGRESSIVO] = Campo_Prog.getText().trim();
        r[COL_DATA_INIZIO] = RwDialoghi.leggiData(Data_Inizio);
        r[COL_DATA_FINE] = RwDialoghi.leggiData(Data_Fine);
        r[COL_VAL_INIZIALE] = valIniziale;
        r[COL_NOTA_INIZIALE] = notaIniziale;
        r[COL_VAL_FINALE] = valFinale;
        r[COL_NOTA_FINALE] = notaFinale;
        r[COL_MOD_INIZIALE] = Principale_GruppiWalletRW.codiceModalita(
                String.valueOf(Combo_ModIni.getSelectedItem()), MODALITA_INIZIALE);
        r[COL_MOD_FINALE] = Principale_GruppiWalletRW.codiceModalita(
                String.valueOf(Combo_ModFin.getSelectedItem()), MODALITA_FINALE);
        r[COL_BOLLO] = fiat ? "" : String.valueOf(Combo_Bollo.getSelectedItem());
        r[COL_ORIGINE] = ORIGINE_UTENTE;
        r[COL_CHIAVE_DEFAULT] = chiaveDefault == null ? "" : chiaveDefault;
        r[COL_STATO_ESTERO] = fiat ? StatiEsteri.codiceDaEtichetta(String.valueOf(Combo_Stato.getSelectedItem())) : "";
        // "NO" e vuoto sono equivalenti : scrivo un valore solo per il SI esplicito, così una riga mai
        // toccata non si ritrova un "NO" dove prima non c'era nulla.
        r[COL_E_CONTO_CORRENTE] = fiat && Principale_GruppiWalletRW.CONTO_CORRENTE_SI.equals(
                String.valueOf(Combo_ContoCorrente.getSelectedItem())) ? Principale_GruppiWalletRW.CONTO_CORRENTE_SI : "";
        r[COL_IDENT_FISCALE] = fiat ? Campo_Ident.getText().trim() : "";
        r[COL_IDENT_ISEE] = fiat ? Campo_Isee.getText().trim() : "";
        r[COL_NOTE_FISCALI] = fiat ? Campo_Note.getText().trim() : "";
        r[COL_FONTE_FISCALE] = fiat ? Campo_Fonte.getText().trim() : "";

        List<String> errori = Principale_GruppiWalletRW.validaPeriodi(java.util.Collections.singletonList(r));
        if (!errori.isEmpty()) {
            Messaggi.WarningMessage("Dati non validi", String.join("\n", errori), this);
            return;
        }
        risultato = r;
        confermato = true;
        dispose();
    }//GEN-LAST:event_Bottone_OkActionPerformed

    private void Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AnnullaActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_AnnullaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Annulla;
    private javax.swing.JButton Bottone_Ok;
    private javax.swing.JTextArea Campo_Fonte;
    private javax.swing.JTextField Campo_Ident;
    private javax.swing.JTextField Campo_Isee;
    private javax.swing.JTextArea Campo_Note;
    private javax.swing.JTextField Campo_Prog;
    private javax.swing.JComboBox Combo_Bollo;
    private javax.swing.JComboBox Combo_ContoCorrente;
    private javax.swing.JComboBox Combo_ModFin;
    private javax.swing.JComboBox Combo_ModIni;
    private javax.swing.JComboBox Combo_Stato;
    private javax.swing.JComboBox Combo_Tipo;
    private com.toedter.calendar.JDateChooser Data_Fine;
    private com.toedter.calendar.JDateChooser Data_Inizio;
    private javax.swing.JLabel Label_Bollo;
    private javax.swing.JLabel Label_ContoCorrente;
    private javax.swing.JLabel Label_DataFine;
    private javax.swing.JLabel Label_DataInizio;
    private javax.swing.JLabel Label_Fonte;
    private javax.swing.JLabel Label_Ident;
    private javax.swing.JLabel Label_Isee;
    private javax.swing.JLabel Label_ModFin;
    private javax.swing.JLabel Label_ModIni;
    private javax.swing.JLabel Label_Note;
    private javax.swing.JLabel Label_Prog;
    private javax.swing.JLabel Label_Stato;
    private javax.swing.JLabel Label_Tipo;
    private javax.swing.JPanel Pannello_Campi;
    private javax.swing.JPanel Pannello_Pulsanti;
    private javax.swing.JScrollPane Scroll_Fonte;
    private javax.swing.JScrollPane Scroll_Note;
    // End of variables declaration//GEN-END:variables
}
