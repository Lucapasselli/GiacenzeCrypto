package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Creazione di una procedura di fallimento: piattaforma, data della domanda, nome breve, anteprima delle giacenze
 * alla data con il valore del credito modificabile, credito in dollari e recupero atteso. Scritto a mano, senza
 * {@code .form}. Alla conferma chiama {@link Principale_Fallimenti#CreaProcedura}; il ricalcolo resta al chiamante.
 */
public class GUI_NuovaProceduraFallimento extends JDialog {

    private static final int COL_INCLUDI = 0;
    private static final int COL_VALORE = 5;

    private final JComboBox<String> Piattaforma;
    private final JTextField DataDomanda = new JTextField(10);
    private final JTextField NomeBreve = new JTextField(14);
    private final JTextField CreditoUSD = new JTextField(12);
    private final JTextField RecuperoAtteso = new JTextField("100", 6);
    private final JLabel Avvisi = new JLabel(" ");
    private final JButton Leggi = new JButton("Leggi le giacenze");
    private final JButton Crea = new JButton("Crea procedura");
    private final DefaultTableModel Modello;
    private final JTable Tabella;
    private final List<Principale_Fallimenti.Posizione> Posizioni = new ArrayList<>();
    private String DataLetta = "";
    private String PiattaformaLetta = "";
    private BigDecimal EuroPerDollaro;
    private BigDecimal FiatEUR = BigDecimal.ZERO;
    /** Il credito in dollari scritto a mano non viene più sostituito dalla proposta. */
    private boolean CreditoScrittoAMano = false;
    private boolean Creata = false;

    /**
     * Mostra il dialogo e aspetta che venga chiuso.
     * @return {@code true} se la procedura è stata creata (i movimenti sono cambiati)
     */
    public static boolean Mostra(Window Proprietario) {
        GUI_NuovaProceduraFallimento d = new GUI_NuovaProceduraFallimento(Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
        return d.Creata;
    }

    private GUI_NuovaProceduraFallimento(Window Proprietario) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Nuova procedura di fallimento");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Piattaforma = new JComboBox<>(Principale_Fallimenti.Exchange().toArray(new String[0]));
        Piattaforma.addActionListener(e -> {
            NomeBreve.setText(Principale_Fallimenti.NomeBreveProposto((String) Piattaforma.getSelectedItem()));
            Svuota();
        });
        NomeBreve.setText(Principale_Fallimenti.NomeBreveProposto((String) Piattaforma.getSelectedItem()));
        DataDomanda.setToolTipText("Data della domanda di fallimento, aaaa-mm-gg: le giacenze sono quelle dei giorni precedenti");
        NomeBreve.setToolTipText("Prima parte del nome dei token credito: NOME.CREDITO.BTC");
        CreditoUSD.setToolTipText("Credito ammesso in dollari alla data della domanda (lista dei crediti del piano). "
                + "Proposto dai valori in euro della tabella e dei FIAT, finché non lo scrivi a mano");
        RecuperoAtteso.setToolTipText("Percentuale del credito che ci si aspetta di recuperare in totale: ripartisce il "
                + "costo fra le tranche future. 100 = ogni tranche scarica la sua percentuale del credito");
        Leggi.addActionListener(e -> LeggiGiacenze());
        CreditoUSD.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                CreditoScrittoAMano = true;
            }
        });

        JPanel Campi = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        Riga(Campi, g, 0, "Piattaforma:", Piattaforma);
        Riga(Campi, g, 1, "Data della domanda (aaaa-mm-gg):", DataDomanda);
        Riga(Campi, g, 2, "Nome breve:", NomeBreve);
        g.gridx = 2;
        g.gridy = 1;
        Campi.add(Leggi, g);

        Modello = new DefaultTableModel(new Object[]{"Includi", "Sotto-wallet", "Moneta", "Rete", "Quantità",
            "Valore credito (EUR)", "Token credito"}, 0) {
            /** @return il tipo della colonna: la prima è una casella di spunta */
            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_INCLUDI ? Boolean.class : String.class;
            }

            /** @return modificabili solo la spunta e il valore del credito */
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_INCLUDI || c == COL_VALORE;
            }
        };
        Modello.addTableModelListener(e -> {
            if (e.getColumn() == COL_INCLUDI || e.getColumn() == COL_VALORE) ProponiCreditoUSD();
        });
        Tabella = new JTable(Modello);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        JScrollPane Scorrimento = new JScrollPane(Tabella);
        Scorrimento.setPreferredSize(new Dimension(900, 260));

        JPanel Piano = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Piano.add(new JLabel("Credito ammesso (USD):"));
        Piano.add(CreditoUSD);
        Piano.add(new JLabel("   Recupero atteso (%):"));
        Piano.add(RecuperoAtteso);

        //Le monete in custodia tornano in quantità: nel credito la loro restituzione consumerebbe tutti i token
        JLabel NotaCustodia = new JLabel("<html><body style='width:820px'><b>Monete in custodia</b> (per esempio un conto "
                + "Custody), che la piattaforma restituisce nella stessa quantità: togliendo la spunta <i>Includi</i> "
                + "restano sul loro sotto-wallet con il loro costo di carico e non entrano nel credito. La loro "
                + "restituzione si classifica poi come un trasferimento tra wallet propri.</body></html>");

        JPanel Centro = new JPanel(new BorderLayout(0, 6));
        Centro.add(Scorrimento, BorderLayout.CENTER);
        JPanel SottoTabella = new JPanel(new BorderLayout(0, 4));
        SottoTabella.add(Avvisi, BorderLayout.NORTH);
        SottoTabella.add(NotaCustodia, BorderLayout.CENTER);
        SottoTabella.add(Piano, BorderLayout.SOUTH);
        Centro.add(SottoTabella, BorderLayout.SOUTH);

        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());
        Crea.setEnabled(false);
        Crea.addActionListener(e -> CreaProcedura());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Annulla);
        Pulsanti.add(Crea);

        JPanel Contenuto = new JPanel(new BorderLayout(0, 8));
        Contenuto.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel CampiASinistra = new JPanel(new BorderLayout());
        CampiASinistra.add(Campi, BorderLayout.WEST);
        Contenuto.add(CampiASinistra, BorderLayout.NORTH);
        Contenuto.add(Centro, BorderLayout.CENTER);
        Contenuto.add(Pulsanti, BorderLayout.SOUTH);
        setContentPane(Contenuto);
        pack();
        setMinimumSize(new Dimension(800, 500));
    }

    private static void Riga(JPanel p, GridBagConstraints g, int y, String Etichetta, java.awt.Component c) {
        g.gridx = 0;
        g.gridy = y;
        p.add(new JLabel(Etichetta), g);
        g.gridx = 1;
        p.add(c, g);
    }

    private void Svuota() {
        Posizioni.clear();
        Modello.setRowCount(0);
        Avvisi.setText(" ");
        Crea.setEnabled(false);
    }

    private boolean DataValida(String d) {
        return d.matches("\\d{4}-\\d{2}-\\d{2}") && FunzioniDate.ConvertiDatainLong(d) > 0;
    }

    /** Somma le quantità alla data e cerca i prezzi (anche in rete) fuori dal thread della grafica. */
    private void LeggiGiacenze() {
        String Data = DataDomanda.getText().trim();
        String Exchange = (String) Piattaforma.getSelectedItem();
        if (Exchange == null || !DataValida(Data)) {
            Avvisi.setText("<html><font color='red'>Scegli la piattaforma e scrivi la data come aaaa-mm-gg.</font></html>");
            return;
        }
        Svuota();
        Leggi.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Avvisi.setText("Lettura delle giacenze e dei prezzi alla data in corso...");
        new SwingWorker<List<Principale_Fallimenti.Posizione>, Void>() {
            private Map<String, BigDecimal> Fiat;
            private int Successivi;
            private BigDecimal Cambio;
            private BigDecimal ValoreFiat;

            @Override
            protected List<Principale_Fallimenti.Posizione> doInBackground() {
                List<Principale_Fallimenti.Posizione> P = Principale_Fallimenti.GiacenzeAllaData(Exchange, Data);
                Principale_Fallimenti.Valorizza(P, Data);
                Fiat = Principale_Fallimenti.GiacenzeFiatAllaData(Exchange, Data);
                ValoreFiat = Principale_Fallimenti.ValoreFiatAllaData(Fiat, Data);
                Successivi = Principale_Fallimenti.MovimentiSuccessivi(Exchange, Data).size();
                //Il cambio può richiedere la rete: si legge qui e non nel thread della grafica
                Cambio = GUI_FallimentiPiattaforme_Pannello.Numero(Prezzi.CambioUSDEUR("1", Data));
                return P;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                Leggi.setEnabled(true);
                try {
                    Posizioni.addAll(get());
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    Avvisi.setText("<html><font color='red'>Errore nella lettura delle giacenze.</font></html>");
                    return;
                }
                DataLetta = Data;
                PiattaformaLetta = Exchange;
                EuroPerDollaro = Cambio;
                FiatEUR = ValoreFiat;
                CreditoScrittoAMano = false;
                Map<String, String> Nomi = Principale_Fallimenti.NomiToken(
                        Principale_Fallimenti.NormalizzaNomeBreve(NomeBreve.getText()), Posizioni);
                for (Principale_Fallimenti.Posizione p : Posizioni) {
                    Modello.addRow(new Object[]{true, p.SottoWallet, p.Moneta, p.Rete, p.Qta.toPlainString(),
                        p.ValoreEUR == null ? "" : p.ValoreEUR.toPlainString(), Nomi.get(p.ChiaveMoneta())});
                }
                StringBuilder t = new StringBuilder("<html>");
                if (Posizioni.isEmpty()) t.append("Nessuna giacenza cripto su ").append(Exchange).append(" alla data.<br>");
                long SenzaPrezzo = Posizioni.stream().filter(p -> p.ValoreEUR == null).count();
                if (SenzaPrezzo > 0) t.append("<font color='red'>").append(SenzaPrezzo)
                        .append(" monete senza prezzo alla data: scrivi il valore del credito a mano.</font><br>");
                if (!Fiat.isEmpty()) {
                    t.append("FIAT sulla piattaforma: ");
                    Fiat.forEach((k, v) -> t.append(v.toPlainString()).append(" ").append(k).append("  "));
                    t.append("(").append(FiatEUR.toPlainString()).append(" EUR). Fanno parte del credito e restano FIAT.<br>");
                }
                for (String sw : SottoWalletCustodia()) {
                    t.append("<font color='red'>Il sotto-wallet ").append(sw)
                            .append(" sembra un conto di custodia: vedi la nota sotto.</font><br>");
                }
                if (Successivi > 0) t.append("<font color='red'>").append(Successivi)
                        .append(Successivi == 1 ? " movimento di " : " movimenti di ").append(Exchange)
                        .append(Successivi == 1 ? " è" : " sono")
                        .append(" dal giorno della domanda in poi: dopo il blocco possono dare giacenze negative.</font><br>");
                Avvisi.setText(t.append("</html>").toString());
                ProponiCreditoUSD();
                Crea.setEnabled(!Posizioni.isEmpty());
            }
        }.execute();
    }

    /** @return i sotto-wallet delle giacenze lette il cui nome fa pensare a un conto di custodia */
    private java.util.Set<String> SottoWalletCustodia() {
        java.util.Set<String> Ris = new java.util.TreeSet<>();
        for (Principale_Fallimenti.Posizione p : Posizioni) {
            if (p.SottoWallet.toLowerCase().contains("custod")) Ris.add(p.SottoWallet);
        }
        return Ris;
    }

    /** Propone il credito in dollari dalla somma dei valori in euro inclusi, al cambio della data. */
    private void ProponiCreditoUSD() {
        if (DataLetta.isEmpty() || CreditoScrittoAMano) return;
        BigDecimal Euro = FiatEUR;
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (!Boolean.TRUE.equals(Modello.getValueAt(r, COL_INCLUDI))) continue;
            BigDecimal v = GUI_FallimentiPiattaforme_Pannello.Numero(Modello.getValueAt(r, COL_VALORE));
            if (v != null) Euro = Euro.add(v);
        }
        if (EuroPerDollaro != null && EuroPerDollaro.signum() > 0) {
            CreditoUSD.setText(Euro.divide(EuroPerDollaro, 2, RoundingMode.HALF_UP).toPlainString());
        }
    }

    private void Errore(String Testo) {
        AppDialog.builder(this).windowTitle("Nuova procedura").showTitleInBody(false).theme()
                .type(AppDialog.DialogType.WARNING).message(Testo).primaryAction("ok", "OK").showDialog();
    }

    private void CreaProcedura() {
        if (Tabella.isEditing()) Tabella.getCellEditor().stopCellEditing();
        String Nome = Principale_Fallimenti.NormalizzaNomeBreve(NomeBreve.getText());
        if (Nome.isEmpty()) {
            Errore("Scrivi il nome breve della piattaforma.");
            return;
        }
        if (!DataDomanda.getText().trim().equals(DataLetta) || !PiattaformaLetta.equals(Piattaforma.getSelectedItem())) {
            Errore("Piattaforma o data sono cambiate dopo la lettura delle giacenze: rileggile.");
            return;
        }
        List<Principale_Fallimenti.Posizione> Incluse = new ArrayList<>();
        for (int r = 0; r < Modello.getRowCount(); r++) {
            if (!Boolean.TRUE.equals(Modello.getValueAt(r, COL_INCLUDI))) continue;
            Principale_Fallimenti.Posizione p = Posizioni.get(r);
            p.ValoreEUR = GUI_FallimentiPiattaforme_Pannello.Numero(Modello.getValueAt(r, COL_VALORE));
            if (p.ValoreEUR != null) p.ValoreEUR = p.ValoreEUR.abs().setScale(2, RoundingMode.HALF_UP);
            Incluse.add(p);
        }
        BigDecimal Atteso = GUI_FallimentiPiattaforme_Pannello.Numero(RecuperoAtteso.getText());
        if (Atteso == null || Atteso.signum() <= 0) {
            Errore("Il recupero atteso dev'essere una percentuale maggiore di zero.");
            return;
        }
        FallimentiPiattaforme.Procedura P = new FallimentiPiattaforme.Procedura();
        P.Id = Nome;
        P.NomeBreve = Nome;
        P.Exchange = PiattaformaLetta;
        P.DataDomanda = DataLetta;
        P.CreditoUSD = GUI_FallimentiPiattaforme_Pannello.Numero(CreditoUSD.getText());
        P.RecuperoAtteso = Atteso;
        P.FiatEUR = FiatEUR;
        String Esito = Principale_Fallimenti.CreaProcedura(P, Incluse);
        if (Esito != null) {
            Errore(Esito);
            return;
        }
        Creata = true;
        dispose();
    }
}
