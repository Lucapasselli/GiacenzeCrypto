package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
    private final DefaultTableModel ModelloRimborsi;
    private final JTable TabellaRimborsi;
    private final JLabel TitoloRimborsi = new JLabel(" ");

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
                + "I rimborsi si registrano come scambi dal token credito alle monete ricevute, che sono un realizzo.</body></html>");
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
        TabellaRimborsi.setAutoCreateRowSorter(true);
        Tabelle.Tabelle_InizializzaHeader(TabellaRimborsi);
        Tabelle.ColoraTabellaSemplice(TabellaRimborsi);
        Tabelle.TooltipHeaderColonne(TabellaRimborsi, null, null, "Percentuale del credito pagata, dal piano",
                "Dollari pagati, dal piano", null, "Piattaforma e wallet del deposito abbinato",
                "Valore delle monete ricevute", "Plusvalenze meno minusvalenze del rimborso, dopo l'ultimo ricalcolo", null);
        TabellaRimborsi.getColumnModel().getColumn(0).setPreferredWidth(130);
        TabellaRimborsi.getColumnModel().getColumn(4).setPreferredWidth(220);
        TabellaRimborsi.getColumnModel().getColumn(5).setPreferredWidth(220);
        JPanel PannelloRimborsi = new JPanel(new BorderLayout(0, 4));
        PannelloRimborsi.add(TitoloRimborsi, BorderLayout.NORTH);
        PannelloRimborsi.add(new JScrollPane(TabellaRimborsi), BorderLayout.CENTER);
        JSplitPane Divisione = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(Tabella), PannelloRimborsi);
        Divisione.setResizeWeight(0.5);
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
        Tabella.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Elimina.setEnabled(Tabella.getSelectedRow() >= 0);
            boolean ConPiano = ProceduraSelezionata() != null;
            Rimborso.setEnabled(ConPiano);
            Chiudi.setEnabled(ConPiano);
            Annulla.setEnabled(IdSelezionato() != null);
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
        JButton Guida = new JButton("Guida...");
        Icona(Nuova, "Nuovo");
        Icona(Rimborso, "Prezzo");
        Icona(Chiudi, "Chiave");
        Icona(Annulla, "Annulla");
        Icona(Elimina, "Cestino");
        Icona(Guida, "Libro");
        Guida.setToolTipText("Apre la guida passo per passo con gli esempi di Celsius, FTX e Voyager");
        Guida.addActionListener(e -> DocumentiAiuto.Apri(DocumentiAiuto.FALLIMENTI_PIATTAFORME));
        Pulsanti.add(Guida);
        JPanel Opzione = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Opzione.add(new JLabel("Valore dei crediti nel quadro W/RW e in Giacenze a data:"));
        Opzione.add(Valore);
        JPanel Sotto = new JPanel(new BorderLayout());
        Sotto.add(Pulsanti, BorderLayout.NORTH);
        Sotto.add(Opzione, BorderLayout.SOUTH);
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

    /** Riempie l'elenco dei rimborsi registrati della procedura selezionata. */
    private void CaricaRimborsi() {
        ModelloRimborsi.setRowCount(0);
        String Id = IdSelezionato();
        if (Id == null) {
            TitoloRimborsi.setText("Seleziona una procedura per vedere i rimborsi registrati.");
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
        }
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
