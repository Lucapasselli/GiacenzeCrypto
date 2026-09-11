/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

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
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_NOTE_FISCALI;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_ORIGINE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_PROGRESSIVO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_STATO_ESTERO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.COL_TIPO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_FINALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.MODALITA_INIZIALE;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_CRYPTO;
import static com.giacenzecrypto.giacenze_crypto.Principale_GruppiWalletRW.TIPO_FIAT;

/**
 * Periodi di detenzione di un gruppo wallet per il quadro W/RW ({@code GRUPPO_PERIODO_RW}) : righi
 * CRYPTO / FIAT con date e modalità di calcolo, più — sui soli righi FIAT — i dati fiscali
 * dell'intermediario (Stato estero, identificativo, alias ISEE, note, fonte). Colonna <b>Origine</b>
 * (Predefinito / Modificato / Manuale).
 *
 * <p>I valori iniziale/finale a mano e le loro note <b>non si mostrano più</b> (2026-09-09) : restano
 * nel DB e negli indici {@code COL_*}, ma erano di utilità dubbia e riempivano la tabella. Per
 * rimetterli servono una colonna qui e i campi nel dialogo, niente di più.</p>
 *
 * <p>Tabella di sola lettura (colonne molte e larghe : scroll orizzontale). Si aggiunge / modifica
 * una riga dal dialogo {@link GUI_ModificaPeriodoDetenzione}. "Ripristina riga / tutti al default"
 * riportano ai valori di {@code RW_Predefiniti.json}. Il modello di verità è {@link #righe}.</p>
 */
public class GUI_PeriodiDetenzioneRW extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Indici delle colonne <b>della tabella</b>. Non coincidono più con i {@code COL_*} del modello
     * dati : la vista ne nasconde quattro (valori e note a mano) e ne aggiunge cinque (i dati fiscali).
     */
    private static final int V_TIPO = 0, V_PROG = 1, V_DATA_INIZIO = 2, V_DATA_FINE = 3,
            V_MOD_INIZIALE = 4, V_MOD_FINALE = 5, V_STATO = 6, V_IDENT = 7, V_ISEE = 8,
            V_NOTE = 9, V_FONTE = 10, V_CONTO_CORRENTE = 11, V_BOLLO = 12, V_ORIGINE = 13;

    private final String gruppo;
    private final List<String[]> righe = new ArrayList<>();

    /** {@code true} se l'utente ha salvato almeno una volta. */
    public boolean salvato = false;

    public GUI_PeriodiDetenzioneRW(String gruppo) {
        this.gruppo = gruppo;
        try {
            setIconImage(new ImageIcon(VarStatiche.getPathRisorse() + "logo.png").getImage());
        } catch (RuntimeException ignore) {
        }
        initComponents();
        // Stile di default delle tabelle dell'applicazione (righe alternate a tema, header in grassetto).
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabelle.ColoraTabellaSemplice(Tabella);

        Tabella.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        Tabella.setShowGrid(false); // niente righe fra celle : come le altre tabelle dell'app (default FlatLaf)
        Tabella.setPreferredScrollableViewportSize(new java.awt.Dimension(900, 260));
        int[] larghezze = {60, 45, 90, 90, 190, 190, 150, 130, 130, 260, 200, 110, 90, 110};
        for (int i = 0; i < larghezze.length && i < Tabella.getColumnModel().getColumnCount(); i++) {
            Tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        for (int c : new int[]{V_MOD_INIZIALE, V_MOD_FINALE, V_NOTE, V_FONTE}) {
            Tabella.getColumnModel().getColumn(c).setCellRenderer(new Tabelle.WrapCellRenderer());
        }
        Tabelle.TooltipHeaderColonne(Tabella,
                "CRYPTO o FIAT : il periodo vale per le cripto-attività o per la valuta estera",
                "Numero d'ordine del periodo per quel tipo",
                "Inizio del periodo (vuoto = dedotto : dalla fine del periodo precedente, o dal primo movimento del gruppo)",
                "Fine del periodo (vuoto = periodo ancora aperto)",
                "Come stimare la giacenza all'inizio del periodo",
                "Come stimare la giacenza alla fine del periodo",
                "Solo righi FIAT : Stato estero dell'intermediario in questo periodo (tabella \"Elenco Paesi\" del modello Redditi)",
                "Solo righi FIAT : identificativo fiscale / P.IVA dell'intermediario in questo periodo",
                "Solo righi FIAT : identificativo per il modulo FC.1 della DSU/ISEE, sempre da inserire a mano",
                "Solo righi FIAT : nota sull'entità legale dell'intermediario",
                "Solo righi FIAT : da dove viene il dato fiscale",
                "Solo righi FIAT : SI se l'intermediario estero è una banca e il conto valuta è un vero conto corrente estero (codice bene 1, IVAFE in misura fissa 34,20 € sul valore medio di giacenza)",
                "Solo righi CRYPTO : in questo periodo l'intermediario ha già assolto l'imposta di bollo",
                "Provenienza della riga : Predefinito / Modificato / Manuale");

        String etichetta = Principale_GruppiWalletRW.etichettaGruppo(gruppo);
        setTitle("Periodi di detenzione — " + etichetta);
        Label_Titolo.setText("Periodi di detenzione del gruppo \"" + etichetta + "\"");
        Label_Titolo.setFont(Label_Titolo.getFont().deriveFont(java.awt.Font.BOLD,
                Label_Titolo.getFont().getSize2D() + 2f));

        // Label_Info come riquadro informativo (callout con banda laterale) invece di testo nudo.
        // Sfondo chiaro proprio + testo scuro esplicito: leggibile anche col tema scuro (cfr. Icone).
        Label_Info.setOpaque(true);
        Label_Info.setBackground(new java.awt.Color(0xF3, 0xF6, 0xFA));
        Label_Info.setForeground(new java.awt.Color(0x2B, 0x2B, 0x2B));
        Label_Info.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        Label_Info.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 4, 0, 0, new java.awt.Color(0x5B, 0x8D, 0xEF)),
                javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                Tabelle.AdattaAltezzaRighe(Tabella, 24);
            }
        });

        // Label_Avvisi : stessa logica di colore del callout informativo, in tinta d'allerta.
        Label_Avvisi.setForeground(new java.awt.Color(0x8A, 0x5A, 0x00));

        righe.addAll(Principale_GruppiWalletRW.caricaPeriodi(gruppo));
        ricostruisciVista();
        mostraAvvisiCopertura();
        setLocationRelativeTo(null);
    }

    private void ricostruisciVista() {
        DefaultTableModel m = (DefaultTableModel) Tabella.getModel();
        m.setRowCount(0);
        for (String[] r : righe) {
            String tipo = v(r, COL_TIPO);
            boolean fiat = TIPO_FIAT.equals(tipo);
            String stato = v(r, COL_STATO_ESTERO);
            m.addRow(new Object[]{
                tipo, v(r, COL_PROGRESSIVO), v(r, COL_DATA_INIZIO), v(r, COL_DATA_FINE),
                Principale_GruppiWalletRW.etichettaModalita(v(r, COL_MOD_INIZIALE), MODALITA_INIZIALE),
                Principale_GruppiWalletRW.etichettaModalita(v(r, COL_MOD_FINALE), MODALITA_FINALE),
                fiat ? (stato.isEmpty() ? "" : StatiEsteri.etichetta(stato)) : "n/d",
                fiat ? v(r, COL_IDENT_FISCALE) : "n/d",
                fiat ? v(r, COL_IDENT_ISEE) : "n/d",
                fiat ? v(r, COL_NOTE_FISCALI) : "n/d",
                fiat ? v(r, COL_FONTE_FISCALE) : "n/d",
                fiat ? (v(r, COL_E_CONTO_CORRENTE).isEmpty() ? Principale_GruppiWalletRW.CONTO_CORRENTE_NO : v(r, COL_E_CONTO_CORRENTE)) : "n/d",
                fiat ? "n/d" : v(r, COL_BOLLO),
                Principale_GruppiWalletRW.descrizioneOrigineRiga(v(r, COL_ORIGINE), v(r, COL_CHIAVE_DEFAULT))});
        }
        Tabelle.AdattaAltezzaRighe(Tabella, 24);
    }

    private int rigaSelezionata() {
        int v = Tabella.getSelectedRow();
        return v < 0 ? -1 : Tabella.convertRowIndexToModel(v);
    }

    private void selezionaUltima() {
        int n = Tabella.getRowCount();
        if (n > 0) {
            Tabella.setRowSelectionInterval(n - 1, n - 1);
            Tabella.scrollRectToVisible(Tabella.getCellRect(n - 1, 0, true));
        }
    }

    private static String v(String[] r, int i) {
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

        Label_Titolo = new javax.swing.JLabel();
        Label_Info = new javax.swing.JLabel();
        Label_Avvisi = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_Aggiungi = new javax.swing.JButton();
        Bottone_Modifica = new javax.swing.JButton();
        Bottone_Rimuovi = new javax.swing.JButton();
        Bottone_RipristinaRiga = new javax.swing.JButton();
        Bottone_RipristinaTutti = new javax.swing.JButton();
        Bottone_Chiudi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Periodi di detenzione");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Label_Titolo.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        Label_Titolo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Label_Titolo.setText("Periodi di detenzione");

        Label_Info.setText("<html><div style='width:840px'><b>A cosa serve.</b> Mostra come il gruppo ha detenuto crypto e valuta estera nel tempo, ai fini del Quadro&nbsp;W/RW. <br>\nGenera un rigo per periodo, separato per CRYPTO e FIAT : serve quando un conto viene chiuso e poi riaperto, o quando cambia il regime del bollo.<br><br>\n<b>Colonne : </b>\n<ul style='margin:3px 10 3px 10'>\n<li><b>Data inizio</b> vuota = dal primo movimento del gruppo&nbsp;&nbsp;&#8226;&nbsp;&nbsp;<b>Data fine</b> vuota = periodo ancora aperto</li>\n<li><b>Calcolo iniziale</b> / <b>finale</b> = come stimare la giacenza al bordo del periodo</li>\n<li><b>Bollo exchange</b> (solo righi CRYPTO) = in quel periodo l'intermediario ha già assolto l'imposta di bollo</li>\n<li><b>Origine</b> = &quot;Predefinito&quot; (dal programma) / &quot;Modificato&quot; / &quot;Manuale&quot;</li>\n</ul>\n<br><b>Pulsanti :</b>\n<ul style='margin:3px 10 3px 10'>\n<li><b>Aggiungi</b> e <b>Modifica</b> aprono un dialogo per inserire o modificare i dati.</li>\n<li><b>Ripristina riga</b> e <b>Ripristina tutto</b> riportano ai valori predefiniti.</li>\n<li>Non c'\u00e8 un pulsante <i>Salva</i> : <b>ogni operazione viene salvata subito</b>.</li>\n<br><center><font color='#8A8A8A'>Le colonne sono molte e larghe: scorri la tabella in orizzontale.</font></div></html>\n</ul>");

        Label_Avvisi.setText(" ");

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Tipo", "Progr.", "Data inizio", "Data fine", "Calcolo iniziale", "Calcolo finale", "Stato estero", "Identificativo fiscale", "Identificativo ISEE", "Note", "Fonte", "Conto corrente", "Bollo exchange", "Origine"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Tabella.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        jScrollPane1.setViewportView(Tabella);

        Bottone_Aggiungi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        Bottone_Aggiungi.setText("Aggiungi");
        Bottone_Aggiungi.setToolTipText("Aggiunge un nuovo periodo di detenzione (CRYPTO o FIAT)");
        Bottone_Aggiungi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AggiungiActionPerformed(evt);
            }
        });

        Bottone_Modifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        Bottone_Modifica.setText("Modifica");
        Bottone_Modifica.setToolTipText("Modifica il periodo selezionato nel dialogo dedicato (con calendario)");
        Bottone_Modifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ModificaActionPerformed(evt);
            }
        });

        Bottone_Rimuovi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Cestino.png"))); // NOI18N
        Bottone_Rimuovi.setText("Rimuovi");
        Bottone_Rimuovi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RimuoviActionPerformed(evt);
            }
        });

        Bottone_RipristinaRiga.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_RipristinaRiga.setText("Ripristina riga");
        Bottone_RipristinaRiga.setToolTipText("Riporta la riga selezionata ai valori predefiniti del programma");
        Bottone_RipristinaRiga.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RipristinaRigaActionPerformed(evt);
            }
        });

        Bottone_RipristinaTutti.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_RipristinaTutti.setText("Ripristina tutto");
        Bottone_RipristinaTutti.setToolTipText("Scarta le personalizzazioni e riapplica i valori predefiniti del programma");
        Bottone_RipristinaTutti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_RipristinaTuttiActionPerformed(evt);
            }
        });

        Bottone_Chiudi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
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
                    .addComponent(Label_Titolo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Label_Info)
                    .addComponent(Label_Avvisi)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Aggiungi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Modifica)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Rimuovi)
                        .addGap(18, 18, 18)
                        .addComponent(Bottone_RipristinaRiga)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_RipristinaTutti)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Chiudi)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Label_Titolo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Label_Info, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Label_Avvisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_Aggiungi)
                    .addComponent(Bottone_Modifica)
                    .addComponent(Bottone_Rimuovi)
                    .addComponent(Bottone_RipristinaRiga)
                    .addComponent(Bottone_RipristinaTutti)
                    .addComponent(Bottone_Chiudi))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Bottone_AggiungiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AggiungiActionPerformed
        int prog = Principale_GruppiWalletRW.prossimoProgressivo(righe, TIPO_CRYPTO);
        GUI_ModificaPeriodoDetenzione d = new GUI_ModificaPeriodoDetenzione(this, null, prog,
                Principale_GruppiWalletRW.bolloDefaultGruppo(gruppo), righe, -1);
        d.setVisible(true);
        if (d.confermato) {
            righe.add(d.risultato);
            salvaAdesso();
            selezionaUltima();
        }
    }//GEN-LAST:event_Bottone_AggiungiActionPerformed

    private void Bottone_ModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ModificaActionPerformed
        int i = rigaSelezionata();
        if (i < 0) {
            Messaggi.WarningMessage("Nessuna selezione", "Seleziona prima un periodo.", this);
            return;
        }
        GUI_ModificaPeriodoDetenzione d = new GUI_ModificaPeriodoDetenzione(this, righe.get(i), 0,
                Principale_GruppiWalletRW.bolloDefaultGruppo(gruppo), righe, i);
        d.setVisible(true);
        if (d.confermato) {
            righe.set(i, d.risultato);
            salvaAdesso();
        }
    }//GEN-LAST:event_Bottone_ModificaActionPerformed

    private void Bottone_RimuoviActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RimuoviActionPerformed
        int i = rigaSelezionata();
        if (i >= 0) {
            righe.remove(i);
            salvaAdesso();
        }
    }//GEN-LAST:event_Bottone_RimuoviActionPerformed

    private void Bottone_RipristinaRigaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RipristinaRigaActionPerformed
        int i = rigaSelezionata();
        if (i < 0) {
            Messaggi.WarningMessage("Nessuna selezione", "Seleziona prima un periodo.", this);
            return;
        }
        if (Principale_GruppiWalletRW.ripristinaRigaAlDefault(gruppo, righe, i)) {
            salvaAdesso();
        } else {
            Messaggi.InfoMessage("Nessun default", "Questo periodo non ha un corrispondente nei dati "
                    + "predefiniti : non c'è nulla a cui ripristinarlo.", this);
        }
    }//GEN-LAST:event_Bottone_RipristinaRigaActionPerformed

    private void Bottone_RipristinaTuttiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_RipristinaTuttiActionPerformed
        int scelta = JOptionPane.showConfirmDialog(this,
                "Rimuovere le personalizzazioni e riportare tutti i periodi di detenzione di questo\n"
                + "gruppo ai valori predefiniti del programma?", "Ripristina tutti al default",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (scelta != JOptionPane.YES_OPTION) {
            return;
        }
        List<String[]> def = Principale_GruppiWalletRW.ripristinaTuttiAlDefault(gruppo);
        if (def == null) {
            Messaggi.WarningMessage("Dati predefiniti non disponibili",
                    "Non è stato possibile leggere RW_Predefiniti.json.", this);
            return;
        }
        righe.clear();
        righe.addAll(def);
        salvaAdesso();
    }//GEN-LAST:event_Bottone_RipristinaTuttiActionPerformed

    /**
     * Persiste subito lo stato corrente di {@link #righe}. Sostituisce il pulsante "Salva", tolto il
     * 2026-09-10 : ogni operazione che tocca l'elenco (aggiunta, modifica, rimozione, ripristino)
     * salva per conto proprio, e la finestra non è più un foglio di lavoro da confermare a parte.
     *
     * <p>È l'<b>unico</b> punto che scrive : la validazione incrociata sta in
     * {@link GUI_ModificaPeriodoDetenzione}, ma rimozione e ripristini non passano di lì, e due
     * scrittori vorrebbero dire due posti dove un errore può sfuggire.</p>
     *
     * <p>Non dovrebbe mai fallire — quello che entra nell'elenco è già stato validato — ma se
     * {@code salvaPeriodi} rifiuta, la memoria viene <b>riallineata al database</b> invece di restare
     * avanti : una vista che mostra righe non salvate è peggio di un'operazione annullata.</p>
     *
     * @return {@code true} se il salvataggio è riuscito
     */
    private boolean salvaAdesso() {
        List<String> errori = Principale_GruppiWalletRW.salvaPeriodi(gruppo, righe);
        if (!errori.isEmpty()) {
            Messaggi.WarningMessage("Salvataggio non riuscito",
                    String.join("\n", errori) + "\n\nL'elenco è stato riportato all'ultimo stato salvato.", this);
            ricaricaDaDatabase();
            return false;
        }
        salvato = true;
        // Se i periodi CRYPTO concordano sul bollo, allineo il flag per-gruppo (toggle tabella "Gruppi Wallet").
        String stato = Principale_GruppiWalletRW.statoBolloPeriodi(gruppo);
        if (Principale_GruppiWalletRW.BOLLO_STATO_TUTTI_SI.equals(stato)) {
            Principale_GruppiWalletRW.allineaBolloGruppo(gruppo, true);
        } else if (Principale_GruppiWalletRW.BOLLO_STATO_TUTTI_NO.equals(stato)) {
            Principale_GruppiWalletRW.allineaBolloGruppo(gruppo, false);
        }
        ricaricaDaDatabase();
        return true;
    }

    /** Rilegge i periodi dal database e ridisegna : dopo un salvataggio le righe tornano con l'Origine aggiornata. */
    private void ricaricaDaDatabase() {
        righe.clear();
        righe.addAll(Principale_GruppiWalletRW.caricaPeriodi(gruppo));
        ricostruisciVista();
        mostraAvvisiCopertura();
    }

    /**
     * Segnala i buchi di copertura fra periodi ({@link Principale_GruppiWalletRW#avvisiPeriodi}) nella
     * label informativa, senza finestre : sono legittimi — un conto chiuso e poi riaperto — e vederli
     * comparire in un dialogo a ogni singola operazione sarebbe insopportabile.
     */
    private void mostraAvvisiCopertura() {
        List<String> avvisi = Principale_GruppiWalletRW.avvisiPeriodi(righe);
        if (avvisi.isEmpty()) {
            Label_Avvisi.setText(" ");
            Label_Avvisi.setToolTipText(null);
        } else {
            Label_Avvisi.setText("<html><b>Avvisi</b> (non bloccanti) : " + String.join(" ; ", avvisi) + "</html>");
            Label_Avvisi.setToolTipText("<html>" + String.join("<br>", avvisi) + "</html>");
        }
    }

    private void Bottone_ChiudiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ChiudiActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_ChiudiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Aggiungi;
    private javax.swing.JButton Bottone_Chiudi;
    private javax.swing.JButton Bottone_Modifica;
    private javax.swing.JButton Bottone_Rimuovi;
    private javax.swing.JButton Bottone_RipristinaRiga;
    private javax.swing.JButton Bottone_RipristinaTutti;
    private javax.swing.JLabel Label_Avvisi;
    private javax.swing.JLabel Label_Info;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JTable Tabella;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
