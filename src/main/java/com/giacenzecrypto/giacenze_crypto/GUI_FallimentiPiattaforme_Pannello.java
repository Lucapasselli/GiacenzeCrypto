package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Scheda "Fallimenti piattaforme" di Analisi Crypto: l'elenco delle procedure di fallimento, la creazione di una nuova
 * procedura ({@link GUI_NuovaProceduraFallimento}) e il modo di valutare i crediti nelle giacenze. Scritta a mano,
 * senza {@code .form}, e aggiunta ad Analisi Crypto dopo {@code initComponents()}. La logica sta in
 * {@link Principale_Fallimenti} e {@link FallimentiPiattaforme}.
 *
 * <p>Le procedure elencate sono quelle della tabella {@code FALLIMENTI} più quelle che esistono solo nei movimenti
 * (chiave {@code FALL-<id>} senza dati del piano: per esempio dopo un'eliminazione non salvata).
 */
public class GUI_FallimentiPiattaforme_Pannello extends JPanel {

    private static final String[] VOCI_VALORE = {
        "B - Credito nominale ancora da rimborsare (dollari al cambio del giorno)",
        "A - Prezzo della moneta bloccata",
        "C - Costo di carico"
    };

    private final Runnable AggiornaTutto;
    private final Runnable OpzioneCambiata;
    private final DefaultTableModel Modello;
    private final JTable Tabella;
    private final JButton Elimina;
    private final JButton Rimborso;
    private final JButton Chiudi;
    private final JButton Annulla;
    private final JButton RisultatoFiat;
    private final DefaultTableModel ModelloRimborsi;
    private final JTable TabellaRimborsi;
    private final JLabel TitoloRimborsi = new JLabel(" ");
    /** Gli ID dei movimenti di ogni riga dei rimborsi, nell'ordine del model. */
    private final List<List<String>> IDRimborsi = new ArrayList<>();
    private final DefaultTableModel ModelloMovimenti;
    private final JLabel TitoloMovimenti = new JLabel(" ");

    /**
     * @param AggiornaTutto ricalcolo e ricarica delle tabelle della finestra principale, da chiamare una volta dopo
     *                      ogni modifica dei movimenti
     * @param OpzioneCambiata segnala che quadro W/RW e Giacenze a data già calcolati vanno rifatti, dopo un cambio
     *                        del valore dei crediti: i movimenti e le plusvalenze non cambiano
     */
    public GUI_FallimentiPiattaforme_Pannello(Runnable AggiornaTutto, Runnable OpzioneCambiata) {
        this.AggiornaTutto = AggiornaTutto;
        this.OpzioneCambiata = OpzioneCambiata;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel Testo = new JLabel("<html><body style='width:760px'>Quando una piattaforma fallisce, le monete che vi erano "
                + "depositate diventano un <b>credito</b> verso la procedura. Con una nuova procedura il programma sposta, "
                + "alla data della domanda di fallimento, le giacenze della piattaforma nel sotto-wallet <i>"
                + Principale_Fallimenti.SOTTOWALLET + "</i> e trasforma ogni moneta nel suo token credito "
                + "(per esempio <i>CELSIUS.CREDITO.BTC</i>), che conserva il costo di carico senza generare plusvalenze. "
                + "I rimborsi si registrano come scambi dal token credito alle monete ricevute, che di default sono un realizzo (vedi l'opzione in fondo).</body></html>");
        add(Testo, BorderLayout.NORTH);

        Modello = new DefaultTableModel(new Object[]{"Procedura", "Piattaforma", "Data domanda", "Credito (USD)",
            "Recupero atteso %", "Tranche", "Movimenti", "Stato"}, 0) {
            /** @return sempre {@code false}: la tabella è solo un elenco */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        Tabella = new JTable(Modello);
        Tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Tabella.setAutoCreateRowSorter(true);
        Tabelle.Tabelle_InizializzaHeader(Tabella);
        Tabelle.ColoraTabellaSemplice(Tabella);
        //Lo stato è la colonna più lunga ("Credito esaurito, 1 rimborso oltre il credito")
        Tabella.getColumnModel().getColumn(7).setPreferredWidth(420);

        //Sotto, i rimborsi già registrati della procedura selezionata, letti dai movimenti
        ModelloRimborsi = new DefaultTableModel(new Object[]{"Data", "Tipo", "% pagata", "Dollari", "Monete ricevute",
            "Arrivo", "Valore (EUR)", "Plus/minus (EUR)", "Movimenti"}, 0) {
            /** @return sempre {@code false}: la tabella è solo un elenco */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        TabellaRimborsi = new JTable(ModelloRimborsi);
        TabellaRimborsi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TabellaRimborsi.setAutoCreateRowSorter(true);
        Tabelle.Tabelle_InizializzaHeader(TabellaRimborsi);
        Tabelle.ColoraTabellaSemplice(TabellaRimborsi);
        Tabelle.TooltipHeaderColonne(TabellaRimborsi, null, null, "Percentuale del credito pagata, dal piano",
                "Dollari pagati, dal piano", null, "Piattaforma e wallet del deposito abbinato",
                "Valore delle monete ricevute", "Plusvalenze meno minusvalenze del rimborso, dopo l'ultimo ricalcolo",
                "Movimenti scritti dalla procedura, senza i depositi importati abbinati");
        TabellaRimborsi.getColumnModel().getColumn(0).setPreferredWidth(130);
        TabellaRimborsi.getColumnModel().getColumn(4).setPreferredWidth(220);
        TabellaRimborsi.getColumnModel().getColumn(5).setPreferredWidth(220);
        JPanel PannelloRimborsi = new JPanel(new BorderLayout(0, 4));
        PannelloRimborsi.add(TitoloRimborsi, BorderLayout.NORTH);
        JScrollPane ScorriRimborsi = new JScrollPane(TabellaRimborsi);
        //Altezze preferite piccole: lo spazio che avanza lo dividono le due tabelle, senza schiacciare le procedure
        ScorriRimborsi.setPreferredSize(new Dimension(100, 120));
        PannelloRimborsi.add(ScorriRimborsi, BorderLayout.CENTER);

        //Sotto ancora, i movimenti del rimborso selezionato: quelli della procedura e il deposito abbinato
        ModelloMovimenti = new DefaultTableModel(new Object[]{"Data e ora", "Piattaforma", "Wallet", "Tipo", "Esce",
            "Quantità uscita", "Entra", "Quantità entrata", "Valore (EUR)", "Plus/minus (EUR)", "ID"}, 0) {
            /** @return sempre {@code false}: la tabella è solo un elenco */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable TabellaMovimenti = new JTable(ModelloMovimenti);
        TabellaMovimenti.setAutoCreateRowSorter(true);
        //Le quantità sono di monete diverse: sommarle nell'intestazione non ha senso
        TabellaMovimenti.putClientProperty("ColonneSenzaSomma", java.util.Set.of(5, 7));
        Tabelle.Tabelle_InizializzaHeader(TabellaMovimenti);
        Tabelle.ColoraTabellaSemplice(TabellaMovimenti);
        Tabelle.TooltipHeaderColonne(TabellaMovimenti, null, null, null, "Tipo di movimento", null, null, null, null,
                null, "Plusvalenza o minusvalenza del movimento, dopo l'ultimo ricalcolo", null);
        TabellaMovimenti.getColumnModel().getColumn(0).setPreferredWidth(130);
        TabellaMovimenti.getColumnModel().getColumn(2).setPreferredWidth(140);
        TabellaMovimenti.getColumnModel().getColumn(3).setPreferredWidth(220);
        TabellaMovimenti.getColumnModel().getColumn(4).setPreferredWidth(170);
        TabellaMovimenti.getColumnModel().getColumn(6).setPreferredWidth(170);
        TabellaMovimenti.getColumnModel().getColumn(10).setPreferredWidth(260);
        JPanel PannelloMovimenti = new JPanel(new BorderLayout(0, 4));
        PannelloMovimenti.add(TitoloMovimenti, BorderLayout.NORTH);
        JScrollPane ScorriMovimenti = new JScrollPane(TabellaMovimenti);
        ScorriMovimenti.setPreferredSize(new Dimension(100, 160));
        PannelloMovimenti.add(ScorriMovimenti, BorderLayout.CENTER);
        TabellaRimborsi.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) CaricaMovimenti();
        });

        //Le procedure sono poche: la loro tabella resta bassa, cinque righe, e lo spazio va a rimborsi e movimenti
        JScrollPane ScorriProcedure = new JScrollPane(Tabella);
        ScorriProcedure.setPreferredSize(new Dimension(100, Tabella.getRowHeight() * 5
                + Tabella.getTableHeader().getPreferredSize().height + 6));
        ScorriProcedure.setMinimumSize(new Dimension(100, Tabella.getRowHeight() * 2
                + Tabella.getTableHeader().getPreferredSize().height + 6));
        JSplitPane DivisioneRimborsi = new JSplitPane(JSplitPane.VERTICAL_SPLIT, PannelloRimborsi, PannelloMovimenti);
        DivisioneRimborsi.setResizeWeight(0.4);
        JSplitPane Divisione = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ScorriProcedure, DivisioneRimborsi);
        Divisione.setResizeWeight(0);
        add(Divisione, BorderLayout.CENTER);

        JButton Nuova = new JButton("Nuova procedura...");
        Nuova.setToolTipText("Blocca le giacenze di una piattaforma alla data della domanda di fallimento");
        Nuova.addActionListener(e -> NuovaProcedura());
        Elimina = new JButton("Elimina procedura...");
        Elimina.setToolTipText("Toglie tutti i movimenti della procedura selezionata e i dati del piano");
        Elimina.setEnabled(false);
        Elimina.addActionListener(e -> EliminaProcedura());
        Rimborso = new JButton("Registra rimborso...");
        Rimborso.setToolTipText("Registra una tranche pagata dalla procedura selezionata: scambi dal credito alle monete ricevute");
        Rimborso.setEnabled(false);
        Rimborso.addActionListener(e -> RegistraRimborso());
        Chiudi = new JButton("Chiudi procedura...");
        Chiudi.setToolTipText("Fa uscire senza valore il credito non rimborsato e i FIAT rimasti: nessuna minusvalenza");
        Chiudi.setEnabled(false);
        Chiudi.addActionListener(e -> ChiudiProcedura());
        Annulla = new JButton("Annulla ultima operazione...");
        Annulla.setToolTipText("Toglie la chiusura o, se non c'è, l'ultimo rimborso registrato");
        Annulla.setEnabled(false);
        Annulla.addActionListener(e -> AnnullaUltimaOperazione());
        RisultatoFiat = new JButton("Risultato parte FIAT...");
        RisultatoFiat.setToolTipText("Differenza fra valore ricevuto e credito in valuta consumato, rimborso per rimborso e "
                + "per anno: il programma non la calcola fra plusvalenze e minusvalenze delle cripto-attività");
        RisultatoFiat.setEnabled(false);
        RisultatoFiat.addActionListener(e -> MostraRisultatoValuta());
        Tabella.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Elimina.setEnabled(Tabella.getSelectedRow() >= 0);
            boolean ConPiano = ProceduraSelezionata() != null;
            Rimborso.setEnabled(ConPiano);
            Chiudi.setEnabled(ConPiano);
            Annulla.setEnabled(IdSelezionato() != null);
            RisultatoFiat.setEnabled(IdSelezionato() != null);
            CaricaRimborsi();
        });

        JComboBox<String> Valore = new JComboBox<>(VOCI_VALORE);
        String Attuale = DatabaseH2.Pers_Opzioni_Leggi(FallimentiPiattaforme.OPZIONE_VALORE, FallimentiPiattaforme.VALORE_CREDITO);
        for (int i = 0; i < VOCI_VALORE.length; i++) {
            if (VOCI_VALORE[i].startsWith(Attuale + " ")) Valore.setSelectedIndex(i);
        }
        Valore.setToolTipText("<html>Valore dei token credito nel quadro W/RW e in Giacenze a data.<br>"
                + "B: il credito ammesso in dollari meno le tranche già pagate, al cambio del giorno.<br>"
                + "A: il prezzo di mercato della moneta che era depositata, che può essere molto diverso dal credito.<br>"
                + "C: il costo di carico trasferito al credito.<br>Non cambia plusvalenze e minusvalenze.</html>");
        Valore.addActionListener(e -> {
            String Scelta = ((String) Valore.getSelectedItem()).substring(0, 1);
            if (Scelta.equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(FallimentiPiattaforme.OPZIONE_VALORE,
                    FallimentiPiattaforme.VALORE_CREDITO))) return;
            DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_VALORE, Scelta);
            OpzioneCambiata.run();
        });

        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Pulsanti.add(Nuova);
        Pulsanti.add(Rimborso);
        Pulsanti.add(Chiudi);
        Pulsanti.add(Annulla);
        Pulsanti.add(Elimina);
        Pulsanti.add(RisultatoFiat);
        JButton Guida = new JButton("Guida...");
        Icona(Nuova, "Nuovo");
        Icona(Rimborso, "Prezzo");
        Icona(Chiudi, "Chiave");
        Icona(Annulla, "Annulla");
        Icona(Elimina, "Cestino");
        Icona(RisultatoFiat, "Calcolatrice");
        Icona(Guida, "Libro");
        Guida.setToolTipText("Apre la guida passo per passo con gli esempi di Celsius, FTX e Voyager");
        Guida.addActionListener(e -> DocumentiAiuto.Apri(DocumentiAiuto.FALLIMENTI_PIATTAFORME));
        Pulsanti.add(Guida);
        JPanel Opzione = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Opzione.add(new JLabel("Valore dei crediti nel quadro W/RW e in Giacenze a data:"));
        Opzione.add(Valore);
        JPanel Sotto = new JPanel(new BorderLayout());
        Sotto.add(Pulsanti, BorderLayout.NORTH);
        //Opzione che cambia le plusvalenze: i rimborsi come permuta ordinaria fra cripto-attività
        JCheckBox Permuta = new JCheckBox("Tratta i rimborsi come scambi ordinari fra cripto-attività", FallimentiPiattaforme.RimborsoComePermuta());
        Permuta.setToolTipText("<html>Di default ogni rimborso da fallimento è un realizzo: plusvalenza o minusvalenza.<br>"
                + "Attiva: il rimborso segue le regole degli scambi fra cripto-attività. Fra monete con le medesime<br>"
                + "caratteristiche (per esempio credito BTC verso ETH) non c'è plusvalenza e il costo di carico passa alla<br>"
                + "moneta ricevuta, fra monete diverse (per esempio credito USDC E-Money verso ETH) c'è il realizzo.<br>"
                + "Il tipo del credito è quello della moneta che era depositata, alla data del rimborso.<br>"
                + "Vale per tutti i rimborsi già registrati e va dichiarato fra le opzioni dei quadri.<br>"
                + "La parte di credito non rimborsata alla chiusura esce senza minusvalenza, come sempre.</html>");
        Permuta.addActionListener(e -> {
            DatabaseH2.Pers_Opzioni_Scrivi(FallimentiPiattaforme.OPZIONE_RIMBORSO_PERMUTA, Permuta.isSelected() ? "SI" : "NO");
            //Cambia le plusvalenze: l'opzione sta in OpzioniRicalcolo.Epoca(), quindi il ricalcolo riparte da solo
            AggiornaTutto.run();
            OpzioneCambiata.run();
            Ricarica();
        });
        JPanel OpzionePermuta = new JPanel(new FlowLayout(FlowLayout.LEFT));
        OpzionePermuta.add(Permuta);
        JPanel Opzioni = new JPanel(new BorderLayout());
        Opzioni.add(Opzione, BorderLayout.NORTH);
        Opzioni.add(OpzionePermuta, BorderLayout.SOUTH);
        Sotto.add(Opzioni, BorderLayout.SOUTH);
        add(Sotto, BorderLayout.SOUTH);

        //Si rilegge ogni volta che diventa visibile: scelta la scheda, o tornando su Analisi Crypto con la scheda già
        //aperta (i movimenti possono essere cambiati altrove nel frattempo)
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) Ricarica();
        });
        //Il pannello nasce dopo initComponents: le icone nere vanno adattate al tema qui (Icone è idempotente)
        Icone.AdattaIconeAlTema(this);
        Ricarica();
    }

    /** Icona di un pulsante, dalle immagini del programma ({@code /Images/24_<Nome>.png}). */
    static void Icona(JButton b, String Nome) {
        b.setIcon(new ImageIcon(GUI_FallimentiPiattaforme_Pannello.class.getResource("/Images/24_" + Nome + ".png")));
    }

    /** Rilegge procedure e movimenti, tenendo selezionata la procedura che lo era. */
    public final void Ricarica() {
        String Selezionata = IdSelezionato();
        Modello.setRowCount(0);
        Map<String, FallimentiPiattaforme.Procedura> Procedure = FallimentiPiattaforme.LeggiProcedure();
        Map<String, Integer> Movimenti = new TreeMap<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            String k = GruppoOperazione.Chiave(v);
            if (k.startsWith(FallimentiPiattaforme.PREFISSO_CHIAVE)) {
                Movimenti.merge(k.substring(FallimentiPiattaforme.PREFISSO_CHIAVE.length()), 1, Integer::sum);
            }
        }
        TreeSet<String> Id = new TreeSet<>(Procedure.keySet());
        Id.addAll(Movimenti.keySet());
        for (String i : Id) {
            FallimentiPiattaforme.Procedura p = Procedure.get(i);
            int n = Movimenti.getOrDefault(i, 0);
            if (p == null) {
                Modello.addRow(new Object[]{i, "", "", "", "", "", n, "Dati del piano mancanti"});
            } else {
                Modello.addRow(new Object[]{p.Id, p.Exchange, p.DataDomanda,
                    p.CreditoUSD == null ? "" : p.CreditoUSD.toPlainString(),
                    p.RecuperoAtteso.stripTrailingZeros().toPlainString(), FallimentiPiattaforme.TrancheValide(p).size(), n,
                    n == 0 ? "Nessun movimento" : Principale_Fallimenti.Stato(p.Id)});
            }
        }
        for (int r = 0; Selezionata != null && r < Tabella.getRowCount(); r++) {
            if (Selezionata.equals(Modello.getValueAt(Tabella.convertRowIndexToModel(r), 0))) {
                Tabella.setRowSelectionInterval(r, r);
                break;
            }
        }
        CaricaRimborsi();
    }

    /** Riempie l'elenco dei rimborsi registrati della procedura selezionata, tenendo selezionato quello che lo era. */
    private void CaricaRimborsi() {
        String DataSelezionata = TabellaRimborsi.getSelectedRow() < 0 ? null
                : (String) ModelloRimborsi.getValueAt(TabellaRimborsi.convertRowIndexToModel(TabellaRimborsi.getSelectedRow()), 0);
        ModelloRimborsi.setRowCount(0);
        IDRimborsi.clear();
        String Id = IdSelezionato();
        if (Id == null) {
            TitoloRimborsi.setText("Seleziona una procedura per vedere i rimborsi registrati.");
            CaricaMovimenti();
            return;
        }
        java.util.List<Principale_Fallimenti.RimborsoRegistrato> L = Principale_Fallimenti.RimborsiRegistrati(Id);
        TitoloRimborsi.setText(L.isEmpty() ? "Nessun rimborso registrato per la procedura " + Id + "."
                : "Rimborsi registrati della procedura " + Id + ":");
        for (Principale_Fallimenti.RimborsoRegistrato R : L) {
            ModelloRimborsi.addRow(new Object[]{R.Data, R.Tipo,
                R.Percentuale == null ? "" : R.Percentuale.stripTrailingZeros().toPlainString(),
                R.USD == null ? "" : R.USD.toPlainString(), R.Ricevute, R.Arrivo,
                R.ValoreEUR == null ? "" : R.ValoreEUR.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                R.Risultato.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), R.Movimenti});
            IDRimborsi.add(R.ID);
        }
        for (int r = 0; DataSelezionata != null && r < TabellaRimborsi.getRowCount(); r++) {
            if (DataSelezionata.equals(ModelloRimborsi.getValueAt(TabellaRimborsi.convertRowIndexToModel(r), 0))) {
                TabellaRimborsi.setRowSelectionInterval(r, r);
                break;
            }
        }
        CaricaMovimenti();
    }

    /** Riempie l'elenco dei movimenti del rimborso selezionato, letti dalla mappa dei movimenti. */
    private void CaricaMovimenti() {
        ModelloMovimenti.setRowCount(0);
        int r = TabellaRimborsi.getSelectedRow();
        if (r < 0) {
            TitoloMovimenti.setText(ModelloRimborsi.getRowCount() == 0 ? " "
                    : "Seleziona un rimborso per vedere i suoi movimenti.");
            return;
        }
        int m = TabellaRimborsi.convertRowIndexToModel(r);
        TitoloMovimenti.setText("Movimenti del " + ModelloRimborsi.getValueAt(m, 0) + " (" + ModelloRimborsi.getValueAt(m, 1)
                + "), compresi i depositi importati abbinati:");
        for (String ID : IDRimborsi.get(m)) {
            String[] v = Principale.MappaCryptoWallet.get(ID);
            if (v == null) continue;
            BigDecimal Plus = "S".equals(v[33]) ? FallimentiPiattaforme.Numero(v[19]) : null;
            ModelloMovimenti.addRow(new Object[]{v[1], v[3], v[4], v[5], v[8], Quantita(v[10]), v[11], Quantita(v[13]),
                Euro(FallimentiPiattaforme.Numero(v[15])), Euro(Plus), v[0]});
        }
    }

    /** @return l'importo a due decimali, vuoto se non c'è */
    private static String Euro(BigDecimal e) {
        return e == null ? "" : e.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** @return la quantità a 8 decimali al massimo, vuota se non c'è: solo per la lettura */
    private static String Quantita(String q) {
        BigDecimal n = FallimentiPiattaforme.Numero(q);
        return n == null ? "" : n.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private Window Finestra() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void NuovaProcedura() {
        if (GUI_NuovaProceduraFallimento.Mostra(Finestra())) {
            AggiornaTutto.run();
            Ricarica();
        }
    }

    /** @return l'id della procedura selezionata, {@code null} se non c'è selezione */
    private String IdSelezionato() {
        int r = Tabella.getSelectedRow();
        return r < 0 ? null : (String) Modello.getValueAt(Tabella.convertRowIndexToModel(r), 0);
    }

    /** @return la procedura selezionata, {@code null} se non c'è o se ha solo movimenti senza dati del piano */
    private FallimentiPiattaforme.Procedura ProceduraSelezionata() {
        String Id = IdSelezionato();
        return Id == null ? null : FallimentiPiattaforme.LeggiProcedure().get(Id);
    }

    private void Avviso(String Titolo, String Testo) {
        AppDialog.builder(Finestra()).windowTitle(Titolo).showTitleInBody(false).theme()
                .type(AppDialog.DialogType.WARNING).message(Testo).primaryAction("ok", "OK").showDialog();
    }

    private void ChiudiProcedura() {
        FallimentiPiattaforme.Procedura P = ProceduraSelezionata();
        if (P == null) return;
        long Ultimo = Principale_Fallimenti.IstanteUltimoMovimento(P.Id);
        String Proposta = FunzioniDate.ConvertiDatadaLongAlSecondo(Ultimo > 0 ? Ultimo + 60_000 : System.currentTimeMillis()).substring(0, 16);
        AppDialog.DialogResult Data = AppDialog.builder(Finestra())
                .windowTitle("Chiudi procedura")
                .bodyTitle("Data della chiusura della procedura " + P.Id)
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Il credito non rimborsato e i FIAT rimasti escono a questa data senza valore, senza "
                        + "minusvalenza. Dalla data indicata non compaiono più nel quadro W/RW né in Giacenze a data.")
                .inputField("Data e ora (aaaa-mm-gg hh:mm):", Proposta)
                .action(AppDialog.DialogAction.builder("annulla", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("avanti", "Avanti").role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (Data == null || !Data.isAction("avanti") || Data.getInputValue() == null) return;
        String Quando = Data.getInputValue().trim();
        Principale_Fallimenti.Chiusura C = Principale_Fallimenti.PreparaChiusura(P, Quando);
        if (C.Errore != null) {
            Avviso("Chiudi procedura", C.Errore);
            return;
        }
        AppDialog.DialogResult Esito = AppDialog.builder(Finestra())
                .windowTitle("Chiudi procedura")
                .bodyTitle("Chiudere la procedura " + P.Id + " il " + Quando + "?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Escono senza valore:\n" + String.join("\n", C.Descrizione))
                .details("Se dopo arriva un altro rimborso, registrandolo la chiusura viene tolta e rifatta dopo il "
                        + "rimborso. Annulla ultima operazione la toglie e riporta la procedura aperta.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("chiudi", "Chiudi procedura").role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (Esito == null || !Esito.isAction("chiudi")) return;
        String Errore = Principale_Fallimenti.ChiudiProcedura(P, Quando);
        if (Errore != null) {
            Avviso("Chiudi procedura", Errore);
            return;
        }
        AggiornaTutto.run();
        Ricarica();
    }

    /**
     * Mostra le differenze della parte in valuta della procedura selezionata ({@link Principale_Fallimenti#RisultatiValuta}),
     * rimborso per rimborso e per anno. Il testo dice l'effetto, non come dichiararle.
     */
    private void MostraRisultatoValuta() {
        String Id = IdSelezionato();
        if (Id == null) return;
        List<Principale_Fallimenti.RisultatoValuta> L = Principale_Fallimenti.RisultatiValuta(Id);
        DefaultTableModel M = new DefaultTableModel(new Object[]{"Data", "Tipo", "Credito in valuta",
            "Credito consumato (EUR)", "Valore ricevuto (EUR)", "Differenza (EUR)"}, 0) {
            /** @return sempre {@code false} */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        Map<String, BigDecimal> PerAnno = new TreeMap<>();
        for (Principale_Fallimenti.RisultatoValuta r : L) {
            M.addRow(new Object[]{r.Data, r.Tipo, r.Credito, Euro(r.Consumato), Euro(r.Ricevuto), Euro(r.Differenza())});
            PerAnno.merge(r.Data.substring(0, 4), r.Differenza(), BigDecimal::add);
        }
        JTable T = new JTable(M);
        Tabelle.Tabelle_InizializzaHeader(T);
        Tabelle.ColoraTabellaSemplice(T);
        T.getColumnModel().getColumn(2).setPreferredWidth(200);
        StringBuilder Testo = new StringBuilder("<html><body style='width:760px'>");
        if (L.isEmpty()) {
            Testo.append("La procedura ").append(Id).append(" non ha un credito in valuta, oppure nessun rimborso o chiusura "
                    + "lo ha ancora consumato.");
        } else {
            Testo.append("Per ogni rimborso, il credito in valuta consumato (al suo valore in euro alla data della domanda) e "
                    + "il valore ricevuto per quella parte. Alla chiusura il credito rimasto esce senza valore. La "
                    + "differenza non è una plusvalenza o minusvalenza su cripto-attività: il programma non la riporta "
                    + "in nessun quadro.<br><br><b>Per anno:</b> ");
            List<String> Anni = new ArrayList<>();
            PerAnno.forEach((a, d) -> Anni.add(a + ": " + Euro(d) + " EUR"));
            Testo.append(String.join(", ", Anni)).append(".");
        }
        Testo.append("</body></html>");
        javax.swing.JDialog D = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(this),
                "Risultato della parte FIAT della procedura " + Id, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JLabel Spiegazione = new JLabel(Testo.toString());
        Spiegazione.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane S = new JScrollPane(T);
        S.setPreferredSize(new Dimension(820, 220));
        JButton Chiudi = new JButton("Chiudi");
        Icona(Chiudi, "Annulla");
        Chiudi.addActionListener(e -> D.dispose());
        JPanel Sotto = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Sotto.add(Chiudi);
        D.getContentPane().setLayout(new BorderLayout());
        D.getContentPane().add(Spiegazione, BorderLayout.NORTH);
        D.getContentPane().add(S, BorderLayout.CENTER);
        D.getContentPane().add(Sotto, BorderLayout.SOUTH);
        Icone.AdattaIconeAlTema(D.getContentPane());
        D.pack();
        D.setLocationRelativeTo(this);
        D.setVisible(true);
        //Il dialogo non c'è più: la tabella non deve restare nelle mappe statiche dei filtri
        Tabelle.tableFilters.remove(T);
        Tabelle.tableFiltersListener.remove(T);
    }

    private void AnnullaUltimaOperazione() {
        String Id = IdSelezionato();
        if (Id == null) return;
        Principale_Fallimenti.Operazione O = Principale_Fallimenti.UltimaOperazione(Id);
        if (O == null) {
            Avviso("Annulla ultima operazione", "Non ci sono rimborsi né chiusura da annullare: per togliere il blocco "
                    + "elimina la procedura.");
            return;
        }
        AppDialog.DialogResult Esito = AppDialog.builder(Finestra())
                .windowTitle("Annulla ultima operazione")
                .bodyTitle("Annullare " + O.Descrizione + "?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message((O.ID.size() == 1 ? "Verrà tolto 1 movimento" : "Verranno tolti " + O.ID.size() + " movimenti")
                        + " della procedura " + Id + ".")
                .details("I depositi importati abbinati a un rimborso tornano da classificare. La cancellazione dei "
                        + "movimenti diventa definitiva quando salvi.")
                .action(AppDialog.DialogAction.builder("no", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("togli", "Togli").role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (Esito == null || !Esito.isAction("togli")) return;
        String Errore = Principale_Fallimenti.AnnullaUltimaOperazione(Id);
        if (Errore != null) Avviso("Annulla ultima operazione", Errore);
        AggiornaTutto.run();
        Ricarica();
    }

    private void RegistraRimborso() {
        FallimentiPiattaforme.Procedura P = ProceduraSelezionata();
        if (P == null) return;
        if (GUI_RegistraTrancheFallimento.Mostra(Finestra(), P)) {
            AggiornaTutto.run();
            Ricarica();
        }
    }

    private void EliminaProcedura() {
        int r = Tabella.getSelectedRow();
        if (r < 0) return;
        String Id = (String) Modello.getValueAt(Tabella.convertRowIndexToModel(r), 0);
        int n = Principale_Fallimenti.MovimentiProcedura(Id).size();
        AppDialog.DialogResult Esito = AppDialog.builder(Finestra())
                .windowTitle("Elimina procedura")
                .bodyTitle("Eliminare la procedura " + Id + "?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Verranno tolti " + n + " movimenti (blocco, rimborsi e chiusura) e i dati del piano.")
                .details("Le giacenze tornano quelle di prima del blocco. La cancellazione dei movimenti diventa "
                        + "definitiva quando salvi, quella dei dati del piano subito.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina").role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (Esito == null || !Esito.isAction("elimina")) return;
        Principale_Fallimenti.EliminaProcedura(Id);
        AggiornaTutto.run();
        Ricarica();
    }

    /** Usato dal dialogo: il valore digitato come numero, {@code null} se vuoto o non valido. */
    static BigDecimal Numero(Object o) {
        if (o == null) return null;
        String s = o.toString().trim().replace(",", ".");
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
