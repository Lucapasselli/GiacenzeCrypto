package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Registrazione di una tranche di rimborso di una procedura di fallimento: data, percentuale del credito pagata,
 * dollari, recupero atteso e monete ricevute (ognuna dal deposito importato su cui è arrivata, o FIAT ricevuti fuori
 * dal programma). L'anteprima mostra le righe che verranno scritte con il costo scaricato e il risultato. Scritto a
 * mano, senza {@code .form}; la logica è {@link Principale_Fallimenti#PreparaTranche}.
 */
public class GUI_RegistraTrancheFallimento extends JDialog {

    private static final int COL_MONETA = 0;
    private static final int COL_QTA = 2;
    private static final int COL_VALORE = 3;
    private static final int COL_NATURA = 5;

    private final FallimentiPiattaforme.Procedura P;
    private final Set<String> MoneteOriginali = new HashSet<>();
    private final JTextField Data = new JTextField(14);
    private final JTextField Percentuale = new JTextField(8);
    private final JTextField USD = new JTextField(12);
    private final JTextField Atteso = new JTextField(6);
    private final DefaultTableModel Ricevute;
    private final JTable TabellaRicevute;
    private final List<String> Depositi = new ArrayList<>();
    private final DefaultTableModel Anteprima;
    private final JLabel Esito = new JLabel(" ");
    /** L'ultimo dei due campi scritto a mano, percentuale o dollari: l'altro si ricava da quello. */
    private boolean USDScrittiAMano = false;
    /** Un campo riempito dal programma non deve rimbalzare sull'altro. */
    private boolean Ricavando = false;
    private boolean Registrata = false;
    /** Credito già esaurito: il rimborso è oltre il credito e percentuale, dollari e recupero atteso non servono. */
    private final boolean Esaurito;

    /**
     * Mostra il dialogo e aspetta che venga chiuso.
     * @return {@code true} se la tranche è stata registrata (i movimenti sono cambiati)
     */
    public static boolean Mostra(Window Proprietario, FallimentiPiattaforme.Procedura P) {
        GUI_RegistraTrancheFallimento d = new GUI_RegistraTrancheFallimento(Proprietario, P);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d.Registrata;
    }

    private GUI_RegistraTrancheFallimento(Window Proprietario, FallimentiPiattaforme.Procedura P) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        this.P = P;
        Esaurito = Principale_Fallimenti.CreditoEsaurito(P);
        setTitle("Rimborso della procedura " + P.Id);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        for (FallimentiPiattaforme.TokenCredito t : Principale_Fallimenti.TokenDellaProcedura(P.Id)) {
            MoneteOriginali.add(t.MonetaOriginale.toUpperCase());
        }

        BigDecimal Pagata = BigDecimal.ZERO;
        List<FallimentiPiattaforme.Tranche> Valide = FallimentiPiattaforme.TrancheValide(P);
        for (FallimentiPiattaforme.Tranche t : Valide) Pagata = Pagata.add(t.Percentuale);
        JLabel Intestazione = new JLabel("<html>Piattaforma <b>" + P.Exchange + "</b>, domanda del " + P.DataDomanda
                + ", credito " + (P.CreditoUSD == null ? "non indicato" : P.CreditoUSD.toPlainString() + " USD")
                + ". Tranche già registrate: " + Valide.size() + " (" + Pagata.stripTrailingZeros().toPlainString()
                + "% del credito)."
                + (Esaurito ? "<br><b>Il credito è esaurito</b>: questo rimborso è oltre il credito e il suo valore è tutto "
                        + "plusvalenza, quindi non servono la percentuale né i dollari pagati." : "")
                + "</html>");

        Atteso.setText(P.RecuperoAtteso.stripTrailingZeros().toPlainString());
        Data.setToolTipText("Data e ora del rimborso, aaaa-mm-gg hh:mm: prima dei depositi su cui è arrivato");
        Percentuale.setToolTipText("Percentuale del credito pagata da questa tranche (per esempio 57,65). Basta scrivere "
                + "questa o i dollari: l'altra si ricava dal credito ammesso");
        USD.setToolTipText("Dollari pagati da questa tranche, come nella comunicazione della procedura. Basta scrivere "
                + "questi o la percentuale: l'altra si ricava dal credito ammesso");
        Atteso.setToolTipText("Recupero totale atteso in percentuale: ripartisce il costo fra le tranche. Vale da questa tranche in poi");
        Percentuale.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { ProponiUSD(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { ProponiUSD(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { ProponiUSD(); }
        });
        USD.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { ProponiPercentuale(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { ProponiPercentuale(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { ProponiPercentuale(); }
        });
        Percentuale.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                USDScrittiAMano = false;
            }
        });
        USD.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                USDScrittiAMano = true;
            }
        });

        JPanel Campi = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        Campo(Campi, g, 0, 0, "Data e ora (aaaa-mm-gg hh:mm):", Data);
        if (!Esaurito) {
            Campo(Campi, g, 0, 2, "Percentuale del credito pagata:", Percentuale);
            Campo(Campi, g, 1, 0, "Dollari pagati:", USD);
            Campo(Campi, g, 1, 2, "Recupero atteso (%):", Atteso);
        }
        JPanel CampiASinistra = new JPanel(new BorderLayout());
        CampiASinistra.add(Campi, BorderLayout.WEST);
        JPanel Nord = new JPanel(new BorderLayout(0, 6));
        Nord.add(Intestazione, BorderLayout.NORTH);
        Nord.add(CampiASinistra, BorderLayout.CENTER);

        Ricevute = new DefaultTableModel(new Object[]{"Moneta ricevuta", "Tipo", "Quantità", "Valore (EUR)", "Arrivo",
            "In natura"}, 0) {
            /** @return il tipo della colonna: l'ultima è una casella di spunta */
            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_NATURA ? Boolean.class : String.class;
            }

            /** @return il valore sempre, la quantità solo per i FIAT fuori dal programma, la spunta solo per una moneta bloccata */
            @Override
            public boolean isCellEditable(int r, int c) {
                if (c == COL_VALORE) return true;
                if (c == COL_QTA) return Depositi.get(r) == null;
                //A credito esaurito non c'è più nulla da restituire in natura
                if (c == COL_NATURA) return !Esaurito && MoneteOriginali.contains(String.valueOf(getValueAt(r, COL_MONETA)).toUpperCase());
                return false;
            }
        };
        TabellaRicevute = new JTable(Ricevute);
        TabellaRicevute.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(TabellaRicevute);
        Tabelle.ColoraTabellaSemplice(TabellaRicevute);
        TabellaRicevute.setToolTipText("In natura: la stessa moneta che era bloccata, trattata come giroconto (costo trasferito, "
                + "nessuna plusvalenza) invece che come rimborso");

        JButton DaDeposito = new JButton("Aggiungi da deposito importato...");
        DaDeposito.addActionListener(e -> AggiungiDeposito());
        JButton Fuori = new JButton("Aggiungi euro ricevuti fuori dal programma");
        Fuori.setToolTipText("Bonifico, PayPal o azioni valutate in euro: escono dalla piattaforma con un prelievo");
        GUI_FallimentiPiattaforme_Pannello.Icona(DaDeposito, "Wallet");
        GUI_FallimentiPiattaforme_Pannello.Icona(Fuori, "Nuovo");
        Fuori.addActionListener(e -> {
            Depositi.add(null);
            Ricevute.addRow(new Object[]{"EUR", "FIAT", "", "", "Fuori dal programma", false});
        });
        JButton Togli = new JButton("Togli");
        GUI_FallimentiPiattaforme_Pannello.Icona(Togli, "Cestino");
        Togli.addActionListener(e -> {
            int r = TabellaRicevute.getSelectedRow();
            if (r >= 0) {
                Depositi.remove(r);
                Ricevute.removeRow(r);
            }
        });
        JPanel PulsantiRicevute = new JPanel(new FlowLayout(FlowLayout.LEFT));
        PulsantiRicevute.add(DaDeposito);
        PulsantiRicevute.add(Fuori);
        PulsantiRicevute.add(Togli);
        JPanel PannelloRicevute = new JPanel(new BorderLayout());
        PannelloRicevute.add(new JScrollPane(TabellaRicevute), BorderLayout.CENTER);
        PannelloRicevute.add(PulsantiRicevute, BorderLayout.SOUTH);

        Anteprima = new DefaultTableModel(new Object[]{"Esce", "Quantità", "Entra", "Quantità", "Valore (EUR)",
            "Costo scaricato", "Risultato", "Trattamento"}, 0) {
            /** @return sempre {@code false}: l'anteprima non si modifica */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable TabellaAnteprima = new JTable(Anteprima);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(TabellaAnteprima);
        Tabelle.ColoraTabellaSemplice(TabellaAnteprima);
        //Il nome del token credito e il trattamento sono le colonne da leggere per intero
        TabellaAnteprima.getColumnModel().getColumn(0).setPreferredWidth(210);
        TabellaAnteprima.getColumnModel().getColumn(7).setPreferredWidth(300);
        JPanel PannelloAnteprima = new JPanel(new BorderLayout());
        PannelloAnteprima.add(new JScrollPane(TabellaAnteprima), BorderLayout.CENTER);
        PannelloAnteprima.add(Esito, BorderLayout.SOUTH);

        JSplitPane Divisione = new JSplitPane(JSplitPane.VERTICAL_SPLIT, PannelloRicevute, PannelloAnteprima);
        Divisione.setResizeWeight(0.4);
        Divisione.setPreferredSize(new Dimension(1250, 440));

        JButton Calcola = new JButton("Anteprima");
        Calcola.addActionListener(e -> MostraAnteprima());
        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());
        JButton Registra = new JButton("Registra rimborso");
        GUI_FallimentiPiattaforme_Pannello.Icona(Calcola, "Calcolatrice");
        GUI_FallimentiPiattaforme_Pannello.Icona(Annulla, "Annulla");
        GUI_FallimentiPiattaforme_Pannello.Icona(Registra, "Salva");
        Registra.addActionListener(e -> Registra());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Calcola);
        Pulsanti.add(Annulla);
        Pulsanti.add(Registra);

        JPanel Contenuto = new JPanel(new BorderLayout(0, 8));
        Contenuto.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Contenuto.add(Nord, BorderLayout.NORTH);
        Contenuto.add(Divisione, BorderLayout.CENTER);
        Contenuto.add(Pulsanti, BorderLayout.SOUTH);
        setContentPane(Contenuto);
        Icone.AdattaIconeAlTema(Contenuto);
        pack();
        setMinimumSize(new Dimension(900, 550));
    }

    private static void Campo(JPanel p, GridBagConstraints g, int y, int x, String Etichetta, java.awt.Component c) {
        g.gridx = x;
        g.gridy = y;
        p.add(new JLabel(Etichetta), g);
        g.gridx = x + 1;
        p.add(c, g);
    }

    private void ProponiUSD() {
        if (Ricavando || USDScrittiAMano || P.CreditoUSD == null) return;
        BigDecimal Perc = GUI_FallimentiPiattaforme_Pannello.Numero(Percentuale.getText());
        Ricava(USD, Perc == null ? "" : P.CreditoUSD.multiply(Perc)
                .divide(FallimentiPiattaforme.CENTO, 2, RoundingMode.HALF_UP).toPlainString());
    }

    /** I dollari scritti a mano danno la percentuale: dollari ÷ credito ammesso, a quattro decimali. */
    private void ProponiPercentuale() {
        if (Ricavando || !USDScrittiAMano || P.CreditoUSD == null || P.CreditoUSD.signum() <= 0) return;
        BigDecimal Dollari = GUI_FallimentiPiattaforme_Pannello.Numero(USD.getText());
        Ricava(Percentuale, Dollari == null ? "" : Dollari.multiply(FallimentiPiattaforme.CENTO)
                .divide(P.CreditoUSD, 4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
    }

    /** Scrive il campo ricavato senza che la sua modifica ricalcoli a sua volta l'altro campo. */
    private void Ricava(JTextField Campo, String Testo) {
        Ricavando = true;
        try {
            Campo.setText(Testo);
        } finally {
            Ricavando = false;
        }
    }

    /** Sceglie un deposito importato e lo aggiunge alle monete ricevute; propone la data un minuto prima. */
    private void AggiungiDeposito() {
        List<String[]> Candidati = Principale_Fallimenti.DepositiCandidati(P);
        Candidati.removeIf(v -> Depositi.contains(v[0]));
        DefaultTableModel M = new DefaultTableModel(new Object[]{"Data", "Piattaforma", "Wallet", "Moneta", "Quantità",
            "Valore (EUR)"}, 0) {
            /** @return sempre {@code false} */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (String[] v : Candidati) M.addRow(new Object[]{v[1], v[3], v[4], v[11], v[13], v[15]});
        JTable T = new JTable(M);
        T.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //Quantità e valore sono testi: si ordinano come numeri
        TableRowSorter<DefaultTableModel> Ordine = new TableRowSorter<>(M);
        java.util.Comparator<Object> ComeNumeri = (a, b) -> {
            BigDecimal x = GUI_FallimentiPiattaforme_Pannello.Numero(a), y = GUI_FallimentiPiattaforme_Pannello.Numero(b);
            if (x == null || y == null) return x == null ? (y == null ? 0 : -1) : 1;
            return x.compareTo(y);
        };
        Ordine.setComparator(4, ComeNumeri);
        Ordine.setComparator(5, ComeNumeri);
        T.setRowSorter(Ordine);
        //Le quantità sono di monete diverse: sommarle nell'intestazione non ha senso, il valore sì
        T.putClientProperty("ColonneSenzaSomma", java.util.Set.of(4));
        Tabelle.Tabelle_InizializzaHeader(T);
        Tabelle.ColoraTabellaSemplice(T);
        JDialog Scelta = new JDialog(this, "Deposito su cui è arrivato il rimborso", ModalityType.APPLICATION_MODAL);
        Tabelle_PopupSelezioneMultipla Popup = new Tabelle_PopupSelezioneMultipla(Scelta);
        JTextField Cerca = new JTextField(24);
        Cerca.setToolTipText("Mostra solo i depositi che contengono il testo in una qualsiasi colonna");
        Tabelle.Tabelle_FiltroColonne(T, Cerca, Popup);
        Cerca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { Tabelle.Tabelle_FiltroColonne(T, Cerca, Popup); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { Tabelle.Tabelle_FiltroColonne(T, Cerca, Popup); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { Tabelle.Tabelle_FiltroColonne(T, Cerca, Popup); }
        });
        JLabel Testo = new JLabel("<html>Depositi su altre piattaforme dal " + P.DataDomanda
                + " in poi, non ancora classificati. Diventeranno l'arrivo di un trasferimento dalla piattaforma fallita.<br>"
                + "Clic sull'intestazione per ordinare, clic destro per filtrare una colonna.</html>");
        JPanel Ricerca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Ricerca.add(new JLabel("Cerca:"));
        Ricerca.add(Cerca);
        JPanel Sopra = new JPanel(new BorderLayout());
        Sopra.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        Sopra.add(Testo, BorderLayout.NORTH);
        Sopra.add(Ricerca, BorderLayout.SOUTH);
        int[] Scelto = {-1};
        Runnable Conferma = () -> {
            Scelto[0] = T.getSelectedRow() < 0 ? -1 : T.convertRowIndexToModel(T.getSelectedRow());
            if (Scelto[0] >= 0) Scelta.dispose();
        };
        T.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && T.getSelectedRow() >= 0) Conferma.run();
            }
        });
        JButton Ok = new JButton("Scegli");
        GUI_FallimentiPiattaforme_Pannello.Icona(Ok, "Catena");
        Ok.addActionListener(e -> Conferma.run());
        JButton Rinuncia = new JButton("Annulla");
        GUI_FallimentiPiattaforme_Pannello.Icona(Rinuncia, "Annulla");
        Rinuncia.addActionListener(e -> Scelta.dispose());
        JPanel Sotto = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Sotto.add(Rinuncia);
        Sotto.add(Ok);
        Scelta.getContentPane().setLayout(new BorderLayout());
        Scelta.getContentPane().add(Sopra, BorderLayout.NORTH);
        JScrollPane S = new JScrollPane(T);
        S.setPreferredSize(new Dimension(800, 300));
        Scelta.getContentPane().add(S, BorderLayout.CENTER);
        Scelta.getContentPane().add(Sotto, BorderLayout.SOUTH);
        Icone.AdattaIconeAlTema(Scelta.getContentPane());
        Scelta.pack();
        Scelta.setLocationRelativeTo(this);
        Scelta.setVisible(true);
        //Il dialogo non c'è più: la tabella non deve restare nelle mappe statiche dei filtri
        Tabelle.tableFilters.remove(T);
        Tabelle.tableFiltersListener.remove(T);
        if (Scelto[0] < 0) return;
        String[] v = Candidati.get(Scelto[0]);
        Depositi.add(v[0]);
        Ricevute.addRow(new Object[]{v[11], v[12], new BigDecimal(v[13]).abs().toPlainString(), v[15],
            v[3] + " / " + v[4] + " del " + v[1], false});
        if (Data.getText().isBlank()) {
            long Prima = FunzioniDate.ConvertiDataIDinLong(v[0].split("_")[0]) - 60000;
            Data.setText(FunzioniDate.ConvertiDatadaLongAlSecondo(Prima).substring(0, 16));
        }
    }

    private List<Principale_Fallimenti.Ricevuta> LeggiRicevute() {
        if (TabellaRicevute.isEditing()) TabellaRicevute.getCellEditor().stopCellEditing();
        List<Principale_Fallimenti.Ricevuta> L = new ArrayList<>();
        for (int r = 0; r < Ricevute.getRowCount(); r++) {
            Principale_Fallimenti.Ricevuta x = new Principale_Fallimenti.Ricevuta();
            x.Moneta = String.valueOf(Ricevute.getValueAt(r, COL_MONETA));
            x.Tipo = String.valueOf(Ricevute.getValueAt(r, 1));
            x.Qta = GUI_FallimentiPiattaforme_Pannello.Numero(Ricevute.getValueAt(r, COL_QTA));
            x.ValoreEUR = GUI_FallimentiPiattaforme_Pannello.Numero(Ricevute.getValueAt(r, COL_VALORE));
            //Per gli euro fuori dal programma quantità e valore coincidono: basta scriverne uno
            if (Depositi.get(r) == null && "EUR".equals(x.Moneta)) {
                if (x.Qta == null) x.Qta = x.ValoreEUR;
                if (x.ValoreEUR == null) x.ValoreEUR = x.Qta;
            }
            x.IDDeposito = Depositi.get(r);
            x.InNatura = Boolean.TRUE.equals(Ricevute.getValueAt(r, COL_NATURA));
            L.add(x);
        }
        return L;
    }

    private Principale_Fallimenti.Tranche Prepara() {
        return Principale_Fallimenti.PreparaTranche(P, Data.getText().trim(), Digitato(Percentuale), Digitato(Atteso),
                LeggiRicevute());
    }

    /** @return il numero scritto nel campo, {@code null} se vuoto o se il credito è esaurito (il campo non si vede) */
    private BigDecimal Digitato(JTextField Campo) {
        return Esaurito ? null : GUI_FallimentiPiattaforme_Pannello.Numero(Campo.getText());
    }

    private void MostraAnteprima() {
        Anteprima.setRowCount(0);
        Principale_Fallimenti.Tranche T = Prepara();
        if (T.Errore != null) {
            Esito.setText("<html><font color='red'>" + T.Errore + "</font></html>");
            return;
        }
        BigDecimal Risultato = BigDecimal.ZERO;
        for (Principale_Fallimenti.RigaAnteprima a : T.Anteprima) {
            boolean Realizza = a.Trattamento.startsWith("Rimborso");
            BigDecimal r = Realizza && a.Costo != null ? a.Valore.subtract(a.Costo) : null;
            if (r != null) Risultato = Risultato.add(r);
            Anteprima.addRow(new Object[]{a.Credito, Quantita(a.QtaUscita), a.Ricevuta, Quantita(a.QtaEntrata),
                a.Valore.toPlainString(), a.Costo == null ? "" : a.Costo.toPlainString(),
                r == null ? "" : r.toPlainString(), a.Trattamento});
        }
        String Consumo = T.Frazione.signum() == 0 ? "" : "La tranche consuma il " + T.Frazione.multiply(FallimentiPiattaforme.CENTO)
                .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "% della quantità bloccata"
                + ". ";
        Esito.setText("<html>" + Consumo + "Plusvalenze meno minusvalenze attese: <b>"
                + Risultato.setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR</b> (calcolo definitivo dopo il ricalcolo)."
                + (T.Avviso == null ? "" : "<br>" + T.Avviso) + "</html>");
    }

    /** @return la quantità a 8 decimali al massimo, solo per la lettura: le righe scritte la tengono intera */
    private static String Quantita(BigDecimal q) {
        return q.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private void Registra() {
        //Procedura chiusa: la chiusura si toglie e si rifà dopo il rimborso, solo con il consenso dell'utente
        String Chiusa = FallimentiPiattaforme.DataChiusura(P.Id);
        if (Chiusa != null) {
            AppDialog.DialogResult Esito = AppDialog.builder(this)
                    .windowTitle("Rimborso")
                    .bodyTitle("La procedura " + P.Id + " è chiusa il " + Chiusa)
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("La chiusura viene tolta e rifatta subito dopo questo rimborso, con quello che resta del "
                            + "credito. Se non resta nulla, la procedura risulta con il credito esaurito.")
                    .details("Annulla ultima operazione toglierà poi la nuova chiusura e quindi questo rimborso: la "
                            + "chiusura del " + Chiusa + " non torna.")
                    .action(AppDialog.DialogAction.builder("annulla", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("registra", "Registra e sposta la chiusura")
                            .role(AppDialog.ActionRole.PRIMARY).build())
                    .showDialog();
            if (Esito == null || !Esito.isAction("registra")) return;
        }
        String Errore = Principale_Fallimenti.RegistraTranche(P, Data.getText().trim(), Digitato(Percentuale),
                Digitato(USD), Digitato(Atteso), LeggiRicevute());
        if (Errore != null) {
            AppDialog.builder(this).windowTitle("Rimborso").showTitleInBody(false).theme()
                    .type(AppDialog.DialogType.WARNING).message(Errore).primaryAction("ok", "OK").showDialog();
            return;
        }
        Registrata = true;
        dispose();
    }
}
