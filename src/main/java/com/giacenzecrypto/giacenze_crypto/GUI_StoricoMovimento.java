package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Mostra le versioni precedenti di un movimento, cioè le righe che {@link MovimentiStorico} ha salvato
 * prima di ogni modifica manuale — comprese quelle ancora in buffer e non salvate su disco
 * ({@link MovimentiStorico#Versioni(String)}), così lo storico è visibile anche prima che l'utente
 * prema Salva in "Transazioni Crypto".
 *
 * <p>Dialogo con {@code .form} sullo schema delle altre finestre dell'app (header in grassetto, righe
 * a colori alternati). <b>È dichiaratamente statico e non riusa
 * {@code GUI_DettaglioTransazione.TransazioniCrypto_CompilaTextPaneDatiMovimento}</b>: quel metodo
 * insegue i movimenti collegati dal campo {@code [20]} rileggendoli <i>dal vivo</i> da
 * {@link Principale#MappaCryptoWallet}, e su una riga storica quei collegamenti possono puntare a ID
 * non più esistenti o nel frattempo riassegnati — quindi darebbe {@code NullPointerException} o, peggio,
 * dati plausibili ma falsi. Qui si mostrano solo le colonne leggibili così come erano, senza nessun
 * calcolo, nessun inseguimento di collegamenti e nessun pulsante DeFi.</p>
 *
 * <p><b>Le righe evidenziate in giallo</b> sono i campi che quella particolare modifica ha cambiato: la
 * versione mostrata si confronta con quella che l'ha sostituita — il movimento vivo per la versione più
 * recente, la versione immediatamente successiva della catena per le altre — non con la voce successiva
 * nell'elenco (l'elenco va dal più recente al più vecchio, la direzione opposta).</p>
 *
 * @author luca.passelli
 */
public class GUI_StoricoMovimento extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final List<String[]> versioni;
    private final String[] movimentoVivo;
    private final DefaultTableModel modello;
    private final Set<Integer> righeModificate = new HashSet<>();

    /**
     * Apre il dialogo sulle versioni precedenti di un movimento.
     *
     * @param ID movimento di cui mostrare lo storico
     * @param Proprietario finestra su cui centrare il dialogo
     */
    public static void Mostra(String ID, Window Proprietario) {
        if (ID == null || ID.isBlank()) {
            return;
        }
        //Lettura per lignaggio e non per ID: con la chiave sull'ID una versione salvata quando il
        //movimento si chiamava ancora in un altro modo resterebbe agganciata a un ID inesistente e
        //l'elenco sarebbe silenziosamente incompleto dopo ogni modifica che ricalcola l'ID.
        //MovimentiStorico.Versioni unisce il DB e il buffer non ancora salvato.
        List<String[]> Versioni = MovimentiStorico.Versioni(MovimentiStorico.LignaggioDi(ID));
        if (Versioni.isEmpty()) {
            Messaggi.WarningMessage("Versioni precedenti",
                    "Di questo movimento non è stata salvata nessuna versione precedente.", Proprietario);
            return;
        }
        GUI_StoricoMovimento d = new GUI_StoricoMovimento(ID, Versioni, Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    private GUI_StoricoMovimento(String ID, List<String[]> Versioni, Window Proprietario) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        this.versioni = Versioni;
        this.movimentoVivo = Principale.MappaCryptoWallet.get(ID);
        initComponents();
        this.modello = (DefaultTableModel) Tabella.getModel();

        Label_Titolo.setText("<html><div style='width:580px'>Movimento <b>" + ID + "</b><br>"
                + "Queste sono le righe come erano <b>prima</b> di ogni modifica fatta a mano: sono una "
                + "fotografia, non movimenti attivi. Le righe in giallo sono i campi che quella modifica "
                + "ha cambiato.</div></html>");

        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        Tabelle.CopiaPulitadaTAG(Tabella);
        ApplicaRendererDiff();
        Tabella.getColumnModel().getColumn(0).setPreferredWidth(190);
        Tabella.getColumnModel().getColumn(1).setPreferredWidth(360);

        //La tendina viene creata vuota nel form e riempita qui: il numero di versioni non è noto a
        //priori. Il modello si costruisce a parte e si installa in un colpo solo, così l'evento
        //generato dalla prima selezione arriva a Compila() quando "versioni" e "modello" sono già pronti.
        DefaultComboBoxModel<String> modelloCombo = new DefaultComboBoxModel<>();
        for (String[] v : versioni) {
            modelloCombo.addElement(DescrizioneVersione(v));
        }
        Combo_Versione.setModel(modelloCombo);
        Combo_Versione.setEnabled(versioni.size() > 1);

        Compila(0);

        getRootPane().setDefaultButton(Bottone_Chiudi);
        setMinimumSize(new Dimension(640, 460));
        pack();
        if (getHeight() > 640) {
            setSize(getWidth(), 640);
        }
    }

    /** Etichetta di una versione nella tendina: quando è stata sostituita e da quale operazione. */
    private static String DescrizioneVersione(String[] Versione) {
        return DataLeggibile(Versione[1]) + "  —  " + OperazioneLeggibile(Versione[3]);
    }

    private static String DataLeggibile(String Millis) {
        try {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(Long.parseLong(Millis)));
        } catch (NumberFormatException ex) {
            return String.valueOf(Millis);
        }
    }

    private static String OperazioneLeggibile(String Operazione) {
        if (Operazione == null) {
            return "";
        }
        return switch (Operazione) {
            case "ModificaMovimento" ->
                "Modifica del movimento (ID ricalcolato)";
            case "TraslaOrario" ->
                "Traslazione dell'orario";
            case Principale_Movimenti_SeparaUnisci.OP_UNIONE ->
                "Unione con altri movimenti (riga di partenza)";
            case MovimentiStorico.OP_IN_PLACE ->
                "Modifica dei dati (ID invariato)";
            default ->
                Operazione;
        };
    }

    /** Riempie la tabella con la versione scelta ed evidenzia i campi che quella modifica ha cambiato. */
    private void Compila(int Indice) {
        modello.setRowCount(0);
        righeModificate.clear();
        if (Indice < 0 || Indice >= versioni.size()) {
            return;
        }
        String[] Versione = versioni.get(Indice);
        String[] v = Importazioni.DeserializzaRiga(Versione[2]);

        //Lo stato in cui il movimento si trovava DOPO questa particolare modifica: per la versione più
        //recente (indice 0) è il movimento vivo, per le altre è la versione immediatamente successiva
        //della catena (indice-1, essendo l'elenco ordinato dal più recente al più vecchio). Confrontare
        //con questo, e non con l'indice successivo nell'elenco, è ciò che dice quali campi la modifica
        //ha davvero cambiato.
        String[] vDopo = (Indice == 0) ? movimentoVivo : Importazioni.DeserializzaRiga(versioni.get(Indice - 1)[2]);

        Riga("Modifica effettuata il", DataLeggibile(Versione[1]), false);
        Riga("Operazione", OperazioneLeggibile(Versione[3]), false);
        if (!Funzioni.noData(Versione[0]) && !Versione[0].equals(v[0])) {
            Riga("ID prima della modifica", Versione[0], false);
        }
        modello.addRow(new String[]{"", ""});

        Riga("ID", v[0], CampoModificato(v, vDopo, 0));
        Riga("Data e ora", v[1], CampoModificato(v, vDopo, 1));
        Riga("Exchange / wallet", v[3], CampoModificato(v, vDopo, 3));
        Riga("Sotto-wallet", v[4], CampoModificato(v, vDopo, 4));
        Riga("Causale", v[5], CampoModificato(v, vDopo, 5));
        Riga("Causale originale del file", v[7], CampoModificato(v, vDopo, 7));
        Riga("Moneta in uscita", Moneta(v[8], v[10]), CampoModificato(v, vDopo, 8, 10));
        Riga("Moneta in entrata", Moneta(v[11], v[13]), CampoModificato(v, vDopo, 11, 13));
        Riga("Controvalore in euro", v[15], CampoModificato(v, vDopo, 15));
        Riga("Classificazione", v[18], CampoModificato(v, vDopo, 18));
        Riga("Note", v[21], CampoModificato(v, vDopo, 21));
        //Il documento di origine si mostra come testo e non come collegamento: è la fotografia di un
        //riferimento che nel frattempo può essere stato eliminato.
        Riga("Documento di origine (id)", v[41], CampoModificato(v, vDopo, 41));

        Tabella.repaint();
    }

    /** @return {@code true} se almeno uno dei campi indicati differisce fra le due righe */
    private static boolean CampoModificato(String[] vPrima, String[] vDopo, int... Indici) {
        if (vDopo == null) {
            return false; //movimento vivo non trovato (es. cancellato): niente diff calcolabile
        }
        for (int i : Indici) {
            String a = (vPrima != null && i < vPrima.length) ? vPrima[i] : "";
            String b = (i < vDopo.length) ? vDopo[i] : "";
            if (!Objects.equals(a, b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aggiunge una riga Campo/Valore. Una riga vuota e non modificata non si mostra, come nelle altre
     * viste di dettaglio; una riga modificata si mostra comunque, anche se il valore in questa versione
     * è vuoto (un campo può essere stato svuotato, o valorizzato per la prima volta), e viene segnata
     * per l'evidenziazione in {@link #ApplicaRendererDiff()}.
     */
    private void Riga(String Campo, String Valore, boolean Modificata) {
        String testo = Funzioni.noData(Valore) ? "" : Valore;
        if (testo.isEmpty() && !Modificata) {
            return;
        }
        if (Modificata) {
            righeModificate.add(modello.getRowCount());
        }
        modello.addRow(new String[]{Campo, testo});
    }

    private static String Moneta(String Simbolo, String Qta) {
        if (Funzioni.noData(Simbolo) && Funzioni.noData(Qta)) {
            return "";
        }
        return (Qta == null ? "" : Qta) + " " + (Simbolo == null ? "" : Simbolo);
    }

    /**
     * Renderer della tabella: stesso motivo a righe alternate delle altre tabelle dell'app
     * ({@link Tabelle#SfondoRigaAlternata(int)} / {@link Tabelle#SfondoSelezione(int)} — esposti da
     * {@link Tabelle} proprio per i renderer scritti a mano fuori da quella classe), con le righe di
     * {@link #righeModificate} evidenziate in giallo come {@code Tabelle.GUI_ModificaPrezzo_ColoraTabellaGialla}.
     */
    private void ApplicaRendererDiff() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(Tabelle.SfondoSelezione(row));
                } else if (righeModificate.contains(row)) {
                    c.setBackground(Tabelle.gialloChiaro);
                } else {
                    c.setBackground(Tabelle.SfondoRigaAlternata(row));
                }
                return c;
            }
        };
        Tabella.setDefaultRenderer(Object.class, renderer);
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
        Label_Versione = new javax.swing.JLabel();
        Combo_Versione = new javax.swing.JComboBox<>();
        ScrollTabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Bottone_Chiudi = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Versioni precedenti del movimento");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        Label_Titolo.setText("Movimento");

        Label_Versione.setText("Versione :");

        Combo_Versione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Combo_VersioneActionPerformed(evt);
            }
        });

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Campo", "Valore"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ScrollTabella.setViewportView(Tabella);

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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Label_Versione)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Combo_Versione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(ScrollTabella)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Bottone_Chiudi)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Label_Titolo, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Label_Versione)
                    .addComponent(Combo_Versione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollTabella, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Bottone_Chiudi)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Combo_VersioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Combo_VersioneActionPerformed
        Compila(Combo_Versione.getSelectedIndex());
    }//GEN-LAST:event_Combo_VersioneActionPerformed

    private void Bottone_ChiudiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ChiudiActionPerformed
        dispose();
    }//GEN-LAST:event_Bottone_ChiudiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Chiudi;
    private javax.swing.JComboBox<String> Combo_Versione;
    private javax.swing.JLabel Label_Titolo;
    private javax.swing.JLabel Label_Versione;
    private javax.swing.JScrollPane ScrollTabella;
    private javax.swing.JTable Tabella;
    // End of variables declaration//GEN-END:variables
}
