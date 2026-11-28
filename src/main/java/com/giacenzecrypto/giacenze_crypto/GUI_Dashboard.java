package com.giacenzecrypto.giacenze_crypto;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * La scheda <i>Dashboard</i>: schede riassuntive di un anno, la tabella di tutti gli anni e un grafico a barre.
 *
 * <p>È costruita a codice (niente {@code .form}) e aggiunta a {@code Principale.CDC} dopo {@code initComponents()}, come
 * {@link GUI_RT_Derivati}. Non legge né calcola nulla da sé: riceve un {@link Principale_Dashboard.Riepilogo} e i
 * contatori degli errori da {@code Principale}, e riporta indietro solo i due clic (<i>Aggiorna</i> e <i>Apri errori</i>).
 * I colori vengono da {@code UIManager} e da {@link Tabelle}, quindi seguono qualunque tema senza confronti sul nome.</p>
 *
 * <p>I valori sono <b>informativi</b>: il quadro RT ha la sua elaborazione. Il testo in testa alla scheda lo dice.</p>
 *
 * <p>Il valore del patrimonio a inizio e fine anno è l'unica cosa lenta (può cercare prezzi): la scheda lo chiede a {@code Principale}
 * con {@link RichiestaValore} <b>solo per l'anno scelto</b>, mostra «calcolo in corso» e lo riceve con {@link #ValoreCalcolato}. Ogni
 * {@link #Imposta} apre una nuova «epoca»: una risposta di un'epoca vecchia (registro cambiato nel frattempo) si ignora.</p>
 */
final class GUI_Dashboard extends JPanel {

    private static final int COLONNE = 4;
    private static final String[] INTESTAZIONI = {"Anno", "Movimenti", "Costi di carico", "Corrispettivi", "Plusvalenze",
        "Minusvalenze", "Risultato netto", "Derivati", "Errori", "Dichiarazioni archiviate"};
    private static final int COLONNA_ERRORI = 8;
    private static final int COLONNA_DICHIARAZIONI = 9;

    /** Un importo nella tabella: il numero serve al colore, il testo a ciò che si vede. */
    private record Importo(BigDecimal valore, String testo, boolean colora) {
        @Override
        public String toString() {
            return testo;
        }
    }

    /** Chiede a {@code Principale} il valore di un anno (fuori dall'EDT), indicando l'epoca in cui lo si chiede. */
    @FunctionalInterface
    interface RichiestaValore {
        void richiedi(int anno, int epoca);
    }

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Runnable aggiorna;
    private final Runnable apriErrori;
    private final RichiestaValore richiediValore;
    private final BiConsumer<String, String> apriTransazioni;
    private final JButton bottoneTransazioni = new JButton();
    private final Map<Integer, Principale_Dashboard.ValoreAnno> valori = new HashMap<>();
    private final Map<Integer, String> rimandati = new HashMap<>();
    private final Set<Integer> richiesti = new HashSet<>();
    private int epoca;
    private final JLabel avviso = new JLabel(" ");
    private final JLabel titoloAnno = new JLabel();
    private final JPanel schede = new JPanel(new GridLayout(0, COLONNE, 12, 12));
    private final DefaultTableModel modello = new DefaultTableModel(INTESTAZIONI, 0) {
        @Override
        public boolean isCellEditable(int riga, int colonna) {
            return false;
        }
    };
    private final JTable tabella = new JTable(modello);
    private final Grafico grafico = new Grafico(this::SelezionaAnno, this::ApriTransazioni);

    private Principale_Dashboard.Riepilogo riepilogo;
    private Principale_Errori.Contatori errori;
    private int annoScelto;
    private boolean sincronizzando;

    /**
     * @param aggiorna eseguita dal pulsante <i>Aggiorna</i> (rilegge il registro)
     * @param apriErrori eseguita dal pulsante <i>Apri gli errori</i> (apre il dialogo degli errori del programma)
     * @param richiediValore chiede il valore di un anno (vedi {@link RichiestaValore})
     * @param apriTransazioni imposta il periodo (prima e ultima data, {@code yyyy-MM-dd}) e apre <i>Transazioni Crypto</i>
     */
    GUI_Dashboard(Runnable aggiorna, Runnable apriErrori, RichiestaValore richiediValore, BiConsumer<String, String> apriTransazioni) {
        super(new BorderLayout());
        this.aggiorna = aggiorna;
        this.apriErrori = apriErrori;
        this.richiediValore = richiediValore;
        this.apriTransazioni = apriTransazioni;
        Costruisci();
    }

    private void Costruisci() {
        JPanel contenuto = new JPanel();
        contenuto.setLayout(new BoxLayout(contenuto, BoxLayout.Y_AXIS));
        contenuto.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // Testata: titolo, spiegazione, pulsante
        JPanel testata = new JPanel(new BorderLayout(12, 4));
        JLabel titolo = new JLabel("Dashboard");
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, titolo.getFont().getSize2D() * 1.7f));
        JLabel spiegazione = new JLabel("<html><body style='width:880px'>Riepilogo per anno dei risultati che il programma ha già calcolato sulle righe del registro. "
                + "È un quadro informativo: il quadro RT ha la sua elaborazione (soglia fino al 2022, compensazioni, derivati). "
                + "I redditi da derivati sono a parte e non si sommano alla plusvalenza cripto.</body></html>");
        JPanel sinistra = new JPanel(new BorderLayout(0, 4));
        sinistra.add(titolo, BorderLayout.NORTH);
        sinistra.add(spiegazione, BorderLayout.CENTER);
        sinistra.add(avviso, BorderLayout.SOUTH);
        avviso.setForeground(Tabelle.rosso);
        JButton bottoneAggiorna = new JButton("Aggiorna");
        bottoneAggiorna.setToolTipText("Rilegge il registro e ricompone il riepilogo");
        bottoneAggiorna.addActionListener(e -> aggiorna.run());
        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        destra.add(bottoneAggiorna);
        testata.add(sinistra, BorderLayout.CENTER);
        testata.add(destra, BorderLayout.EAST);
        testata.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenuto.add(testata);
        contenuto.add(Box.createVerticalStrut(14));

        // Le schede dell'anno scelto
        titoloAnno.setFont(titoloAnno.getFont().deriveFont(Font.BOLD, titoloAnno.getFont().getSize2D() * 1.25f));
        bottoneTransazioni.addActionListener(e -> ApriTransazioni(annoScelto));
        bottoneTransazioni.setToolTipText("<html>Apre <i>Transazioni Crypto</i> con il periodo impostato su questo anno.<br>"
                + "<i>Azzera filtri</i> rimette il periodo di prima.<br>Vale anche il doppio clic su una riga o su una barra.</html>");
        JPanel intestazioneAnno = new JPanel(new BorderLayout(12, 0));
        intestazioneAnno.add(titoloAnno, BorderLayout.WEST);
        JPanel destraAnno = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        destraAnno.add(bottoneTransazioni);
        intestazioneAnno.add(destraAnno, BorderLayout.EAST);
        intestazioneAnno.setAlignmentX(Component.LEFT_ALIGNMENT);
        intestazioneAnno.setMaximumSize(new Dimension(Integer.MAX_VALUE, intestazioneAnno.getPreferredSize().height + 6));
        contenuto.add(intestazioneAnno);
        contenuto.add(Box.createVerticalStrut(8));
        schede.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenuto.add(schede);
        contenuto.add(Box.createVerticalStrut(18));

        // Tabella di tutti gli anni, con a fianco il grafico
        JLabel titoloTabella = new JLabel("Tutti gli anni");
        titoloTabella.setFont(titoloTabella.getFont().deriveFont(Font.BOLD, titoloTabella.getFont().getSize2D() * 1.25f));
        titoloTabella.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenuto.add(titoloTabella);
        contenuto.add(Box.createVerticalStrut(8));
        PreparaTabella();
        JScrollPane scorri = new JScrollPane(tabella);
        JPanel grafica = new JPanel(new BorderLayout(0, 4));
        grafica.add(new JLabel("<html>Plusvalenze (verde) e minusvalenze (rosso) di ogni anno. Il rombo è il risultato netto.</html>"),
                BorderLayout.NORTH);
        grafica.add(grafico, BorderLayout.CENTER);
        grafica.setPreferredSize(new Dimension(430, 250));
        JPanel riga = new JPanel(new BorderLayout(14, 0));
        riga.add(scorri, BorderLayout.CENTER);
        riga.add(grafica, BorderLayout.EAST);
        riga.setPreferredSize(new Dimension(900, 260));
        riga.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        riga.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenuto.add(riga);

        JPanel base = new Pagina();
        base.add(contenuto, BorderLayout.NORTH);
        JScrollPane pagina = new JScrollPane(base);
        pagina.setBorder(BorderFactory.createEmptyBorder());
        pagina.getVerticalScrollBar().setUnitIncrement(24);
        add(pagina, BorderLayout.CENTER);
    }

    /**
     * Il contenitore scorrevole: segue la larghezza della finestra (niente barra orizzontale) e lascia al contenuto la
     * sua altezza, senza stirarlo quando la finestra è più alta.
     */
    private static final class Pagina extends JPanel implements Scrollable {
        Pagina() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibile, int orientamento, int direzione) {
            return Math.max(24, visibile.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Stile come le altre tabelle del programma (righe alternate a tema, intestazione in grassetto) e colori dei risultati. */
    private void PreparaTabella() {
        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.setRowHeight(tabella.getRowHeight() + 6);
        tabella.setFillsViewportHeight(true);
        tabella.getTableHeader().setReorderingAllowed(false);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        Tabelle.ColoraTabellaSemplice(tabella);
        final TableCellRenderer base = tabella.getDefaultRenderer(Object.class);
        tabella.setDefaultRenderer(Object.class, (t, valore, selezionata, fuoco, riga, colonna) -> {
            Component c = base.getTableCellRendererComponent(t, valore, selezionata, fuoco, riga, colonna);
            if (c instanceof JLabel etichetta) {
                etichetta.setHorizontalAlignment(colonna == 0 || colonna == COLONNA_DICHIARAZIONI ? SwingConstants.LEFT : SwingConstants.RIGHT);
                boolean totale = riga == t.getRowCount() - 1;
                etichetta.setFont(t.getFont().deriveFont(totale ? Font.BOLD : Font.PLAIN));
                if (!selezionata) {
                    Color colore = t.getForeground();
                    if (valore instanceof Importo i && i.colora() && i.valore() != null) {
                        colore = i.valore().signum() > 0 ? Tabelle.verdeScuro : i.valore().signum() < 0 ? Tabelle.rosso : colore;
                    }
                    if (colonna == COLONNA_ERRORI && valore instanceof Integer n && n > 0) colore = Tabelle.rosso;
                    etichetta.setForeground(colore);
                }
            }
            return c;
        });
        int[] larghezze = {90, 80, 120, 120, 120, 120, 120, 110, 70, 190};
        for (int i = 0; i < larghezze.length; i++) {
            tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        tabella.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || sincronizzando || riepilogo == null) return;
            int riga = tabella.getSelectedRow();
            if (riga < 0) return;
            Object anno = modello.getValueAt(riga, 0);
            SelezionaAnno(anno instanceof Integer a ? a : 0);
        });
        tabella.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || riepilogo == null) return;
                int riga = tabella.rowAtPoint(e.getPoint());
                if (riga < 0) return;
                Object anno = modello.getValueAt(riga, 0);
                ApriTransazioni(anno instanceof Integer a ? a : 0);
            }
        });
    }

    /**
     * Mostra il riepilogo.
     *
     * @param annoPreferito l'anno da selezionare se esiste, 0 per il totale storico, altrimenti (anche un valore negativo) il più recente
     */
    void Imposta(Principale_Dashboard.Riepilogo nuovo, int annoPreferito, Principale_Errori.Contatori contatori) {
        riepilogo = nuovo;
        errori = contatori;
        //Registro riletto: i valori dei patrimoni calcolati prima non valgono piu', e le risposte ancora in viaggio nemmeno
        epoca++;
        valori.clear();
        rimandati.clear();
        richiesti.clear();
        avviso.setText(" ");
        modello.setRowCount(0);
        List<Principale_Dashboard.RigaAnno> anni = nuovo.anni();
        for (int i = anni.size() - 1; i >= 0; i--) {
            modello.addRow(Celle(anni.get(i)));
        }
        modello.addRow(Celle(nuovo.totale()));
        if (annoPreferito == 0 || nuovo.anno(annoPreferito) != null) annoScelto = annoPreferito;
        else annoScelto = anni.isEmpty() ? 0 : anni.get(anni.size() - 1).anno();
        grafico.Imposta(anni, annoScelto);
        SelezionaRigaTabella();
        AggiornaSchede();
    }

    /** Il riepilogo è fermo mentre il registro si sta ricalcolando: si dice e si lascia quello che c'è. */
    void MostraAvviso(String testo) {
        avviso.setText(testo == null || testo.isBlank() ? " " : testo);
    }

    /**
     * I contatori degli errori del programma cambiano anche senza che il registro cambi (i controlli sull'intero registro finiscono
     * dopo il caricamento della tabella): si riscrive solo la scheda che li mostra.
     */
    void ImpostaContatori(Principale_Errori.Contatori contatori) {
        if (contatori.equals(errori)) return;
        errori = contatori;
        if (riepilogo != null) AggiornaSchede(isShowing());
    }

    /** La scheda torna a video: se il valore dell'anno scelto non e' stato ancora chiesto (o era stato rimandato) lo si chiede ora. */
    void AlMostrarsi() {
        if (riepilogo != null) AggiornaSchede(true);
    }

    /** Il valore di un anno è pronto (sull'EDT). Una risposta di un'epoca vecchia si scarta. */
    void ValoreCalcolato(int anno, int epocaRichiesta, Principale_Dashboard.ValoreAnno valore) {
        if (epocaRichiesta != epoca) return;
        richiesti.remove(anno);
        valori.put(anno, valore);
        if (anno == annoScelto && riepilogo != null) AggiornaSchede();
    }

    /** Il valore di un anno non si è potuto calcolare adesso: si scrive il motivo, e si riprova alla prossima scelta di quell'anno. */
    void ValoreRimandato(int anno, int epocaRichiesta, String motivo) {
        if (epocaRichiesta != epoca) return;
        richiesti.remove(anno);
        rimandati.put(anno, motivo);
        if (anno == annoScelto && riepilogo != null) AggiornaSchede();
    }

    /** Apre le transazioni dell'anno (0 = dal primo all'ultimo anno del registro). */
    private void ApriTransazioni(int anno) {
        if (riepilogo == null || riepilogo.anni().isEmpty()) return;
        if (anno != 0 && riepilogo.anno(anno) == null) return;
        int primo = anno == 0 ? riepilogo.anni().get(0).anno() : anno;
        int ultimo = anno == 0 ? riepilogo.anni().get(riepilogo.anni().size() - 1).anno() : anno;
        apriTransazioni.accept(primo + "-01-01", ultimo + "-12-31");
    }

    /** @return l'anno scelto (0 = totale storico) */
    int AnnoScelto() {
        return annoScelto;
    }

    private void SelezionaAnno(int anno) {
        if (riepilogo == null || (anno != 0 && riepilogo.anno(anno) == null)) return;
        annoScelto = anno;
        rimandati.remove(anno);
        grafico.Imposta(riepilogo.anni(), annoScelto);
        SelezionaRigaTabella();
        AggiornaSchede();
    }

    private void SelezionaRigaTabella() {
        sincronizzando = true;
        try {
            int riga = modello.getRowCount() - 1;
            for (int i = 0; i < modello.getRowCount() - 1; i++) {
                if (Integer.valueOf(annoScelto).equals(modello.getValueAt(i, 0))) {
                    riga = i;
                    break;
                }
            }
            if (riga >= 0) {
                tabella.setRowSelectionInterval(riga, riga);
                tabella.scrollRectToVisible(tabella.getCellRect(riga, 0, true));
            }
        } finally {
            sincronizzando = false;
        }
    }

    private static Object[] Celle(Principale_Dashboard.RigaAnno r) {
        return new Object[] {
            r.anno() == 0 ? "Totale storico" : (Object) r.anno(),
            r.movimenti(),
            new Importo(r.costi(), Principale_Dashboard.Euro(r.costi()), false),
            new Importo(r.corrispettivi(), Principale_Dashboard.Euro(r.corrispettivi()), false),
            new Importo(r.plusvalenze(), Principale_Dashboard.Euro(r.plusvalenze()), true),
            new Importo(r.minusvalenze(), Principale_Dashboard.Euro(r.minusvalenze()), true),
            new Importo(r.netto(), Principale_Dashboard.EuroConSegno(r.netto()), true),
            r.derivati() == null ? "-" : new Importo(r.derivati(), Principale_Dashboard.EuroConSegno(r.derivati()), true),
            r.errori().totale(),
            r.dichiarazioni().isEmpty() ? "nessuna" : r.dichiarazioni()
        };
    }

    //=== SCHEDE ===

    private void AggiornaSchede() {
        AggiornaSchede(true);
    }

    /** @param richiedi {@code false} per solo ridisegnare (scheda non a video): il calcolo del valore parte quando la scheda si mostra */
    private void AggiornaSchede(boolean richiedi) {
        schede.removeAll();
        Principale_Dashboard.RigaAnno r = annoScelto == 0 ? riepilogo.totale() : riepilogo.anno(annoScelto);
        titoloAnno.setText(annoScelto == 0 ? "Totale storico" : "Anno " + annoScelto);
        bottoneTransazioni.setText(annoScelto == 0 ? "Apri tutte le transazioni" : "Apri le transazioni del " + annoScelto);
        bottoneTransazioni.setEnabled(r != null && !riepilogo.anni().isEmpty());
        if (r == null) {
            schede.revalidate();
            schede.repaint();
            return;
        }
        schede.add(Scheda("Costi di carico", Principale_Dashboard.Euro(r.costi()), "Righe con effetto sul riepilogo", null));
        schede.add(Scheda("Corrispettivi", Principale_Dashboard.Euro(r.corrispettivi()), "Righe con effetto sul riepilogo", null));
        schede.add(Scheda("Plusvalenza netta cripto", Principale_Dashboard.EuroConSegno(r.netto()), "Plusvalenze meno minusvalenze", r.netto()));
        schede.add(Scheda("Redditi da derivati", r.derivati() == null ? "-" : Principale_Dashboard.EuroConSegno(r.derivati()),
                r.derivati() == null ? "Nessun derivato" : "A parte, non è nella plusvalenza", r.derivati()));
        schede.add(Scheda("Plusvalenze", Principale_Dashboard.EuroConSegno(r.plusvalenze()), "Solo le righe in guadagno", r.plusvalenze()));
        schede.add(Scheda("Minusvalenze", Principale_Dashboard.EuroConSegno(r.minusvalenze()), "Solo le righe in perdita", r.minusvalenze()));
        boolean daRichiedere = AggiungiSchedeValore();
        schede.add(Scheda("Movimenti", Integer.toString(r.movimenti()), "Righe del registro, comprese le generate", null));
        schede.add(Scheda("Wallet", Integer.toString(r.wallet()), "Exchange e wallet distinti", null));
        schede.add(Scheda("Documenti", Integer.toString(r.documenti()), "Documenti di origine e di supporto", null));
        schede.add(Scheda("Dichiarazioni archiviate", r.dichiarazioni().isEmpty() ? "nessuna" : r.dichiarazioni(),
                annoScelto == 0 ? "In tutti gli anni" : "Dall'archivio delle dichiarazioni", null));
        schede.add(SchedaErrori(r));
        int senzaData = riepilogo.senzaData();
        schede.add(Scheda("Anni nel registro", riepilogo.anni().isEmpty() ? "-"
                : riepilogo.anni().get(0).anno() + " - " + riepilogo.anni().get(riepilogo.anni().size() - 1).anno(),
                senzaData > 0 ? senzaData + " movimenti senza data, fuori dagli anni" : "Dal primo all'ultimo movimento", null));
        schede.revalidate();
        schede.repaint();
        //La richiesta parte per ultima: se risponde subito (anno rimandato) la scheda e' gia' completa
        if (daRichiedere && richiedi) {
            richiesti.add(annoScelto);
            richiediValore.richiedi(annoScelto, epoca);
        }
    }

    /**
     * Le due schede del valore delle cripto-attività a inizio e fine anno.
     *
     * @return {@code true} se il valore dell'anno scelto va ancora chiesto
     */
    private boolean AggiungiSchedeValore() {
        final String nota = "A prezzi di mercato o personalizzati";
        if (annoScelto == 0) {
            schede.add(Scheda("Valore a inizio anno", "-", "Scegli un anno", null));
            schede.add(Scheda("Valore a fine anno", "-", "Scegli un anno", null));
            return false;
        }
        Principale_Dashboard.ValoreAnno v = valori.get(annoScelto);
        if (v == null) {
            String motivo = rimandati.get(annoScelto);
            String simbolo = motivo != null ? "-" : "...";
            String sotto = motivo != null ? motivo : "Calcolo in corso, la scheda resta utilizzabile";
            schede.add(Scheda("Valore a inizio anno", simbolo, sotto, null, motivo != null));
            schede.add(Scheda("Valore a fine anno", simbolo, motivo != null ? "Premi Aggiorna o riseleziona l'anno" : "Cerco i prezzi", null));
            return motivo == null && !richiesti.contains(annoScelto);
        }
        String avvisoPrezzi = v.senzaPrezzo() > 0 ? "Prezzo non trovato per " + v.senzaPrezzo()
                + " asset, valore parziale" : null;
        schede.add(Scheda("Valore a inizio anno", Principale_Dashboard.Euro(v.inizio()), avvisoPrezzi != null ? avvisoPrezzi : nota, null,
                avvisoPrezzi != null));
        if (v.fine() == null) {
            schede.add(Scheda("Valore a fine anno", "-", "Anno appena iniziato", null));
        } else {
            boolean parziale = !v.fineAl().endsWith("-12-31");
            String data = parziale ? "Al " + LocalDate.parse(v.fineAl()).format(FORMATO_DATA) + ", anno in corso" : nota;
            schede.add(Scheda("Valore a fine anno", Principale_Dashboard.Euro(v.fine()), avvisoPrezzi != null ? avvisoPrezzi : data, null,
                    avvisoPrezzi != null));
        }
        return false;
    }

    private JPanel SchedaErrori(Principale_Dashboard.RigaAnno r) {
        int globali = errori == null ? 0 : errori.totale();
        int n = annoScelto == 0 ? globali : r.errori().totale();
        String nota;
        if (annoScelto == 0) {
            nota = globali == 0 ? "Nessun errore segnalato" : "Tutto il registro, giacenze negative e collegamenti compresi";
        } else {
            Principale_Dashboard.ErroriAnno e = r.errori();
            StringBuilder dettaglio = new StringBuilder();
            Aggiungi(dettaglio, e.senzaPrezzo(), "senza prezzo");
            Aggiungi(dettaglio, e.nonClassificati(), "non classificati");
            Aggiungi(dettaglio, e.lifoMancante(), "LiFo mancante");
            nota = "<html>" + (n == 0 ? "Nessun errore in questo anno" : dettaglio)
                    + (globali > 0 ? "<br>Tutto il registro: " + globali : "") + "</html>";
        }
        JPanel s = Scheda("Errori del registro", Integer.toString(n), nota, null, n > 0);
        if (globali > 0) {
            JButton apri = new JButton("Apri gli errori");
            apri.setToolTipText("Apre l'elenco degli errori di tutto il registro, non solo di questo anno");
            apri.addActionListener(e -> apriErrori.run());
            JPanel sotto = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
            sotto.setOpaque(false);
            sotto.add(apri);
            s.add(sotto, BorderLayout.EAST);
        }
        return s;
    }

    private static void Aggiungi(StringBuilder sb, int quanti, String testo) {
        if (quanti <= 0) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(quanti).append(' ').append(testo);
    }

    private static JPanel Scheda(String titolo, String valore, String nota, BigDecimal risultato) {
        return Scheda(titolo, valore, nota, risultato, false);
    }

    /** Una scheda: titolo piccolo, valore grande (colorato se è un risultato), nota (in rosso se è un avviso). */
    private static JPanel Scheda(String titolo, String valore, String nota, BigDecimal risultato, boolean avviso) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        Color fondo = UIManager.getColor("TextField.background");
        p.setBackground(fondo == null ? Color.WHITE : fondo);
        Color bordo = UIManager.getColor("Component.borderColor");
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bordo == null ? Color.LIGHT_GRAY : bordo),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel t = new JLabel(titolo);
        t.setForeground(UIManager.getColor("Label.disabledForeground"));
        JLabel v = new JLabel("<html>" + valore.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        v.setFont(v.getFont().deriveFont(Font.BOLD, v.getFont().getSize2D() * 1.6f));
        if (risultato != null && risultato.signum() != 0) {
            v.setForeground(risultato.signum() > 0 ? Tabelle.verdeScuro : Tabelle.rosso);
        }
        JLabel n = new JLabel(nota);
        n.setFont(n.getFont().deriveFont(n.getFont().getSize2D() * 0.9f));
        n.setForeground(avviso ? Tabelle.rosso : UIManager.getColor("Label.disabledForeground"));
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        p.add(n, BorderLayout.SOUTH);
        return p;
    }

    //=== GRAFICO ===

    /**
     * Barre per anno: la plusvalenza sopra lo zero, la minusvalenza sotto, un rombo sul risultato netto. Dipinto a mano come
     * {@link RT_GraficoPlusvalenze} (nessuna libreria), ma con i colori di {@code UIManager}: segue il tema senza confronti.
     * Un clic seleziona l'anno, il doppio clic apre le sue transazioni.
     */
    private static final class Grafico extends JPanel {
        private static final int MARGINE_SX = 62;
        private static final int MARGINE_DX = 10;
        private static final int MARGINE_SU = 10;
        private final IntConsumer sceltaAnno;
        private final IntConsumer doppioClic;
        private List<Principale_Dashboard.RigaAnno> anni = List.of();
        private int selezionato;

        Grafico(IntConsumer sceltaAnno, IntConsumer doppioClic) {
            this.sceltaAnno = sceltaAnno;
            this.doppioClic = doppioClic;
            setOpaque(true);
            setMinimumSize(new Dimension(260, 160));
            setToolTipText("");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = Indice(e.getX());
                    if (i < 0) return;
                    Grafico.this.sceltaAnno.accept(anni.get(i).anno());
                    if (e.getClickCount() == 2) Grafico.this.doppioClic.accept(anni.get(i).anno());
                }
            });
        }

        void Imposta(List<Principale_Dashboard.RigaAnno> nuovi, int anno) {
            anni = nuovi;
            selezionato = anno;
            repaint();
        }

        private int Indice(int x) {
            if (anni.isEmpty()) return -1;
            double fetta = (getWidth() - MARGINE_SX - MARGINE_DX) / (double) anni.size();
            int i = (int) Math.floor((x - MARGINE_SX) / fetta);
            return i >= 0 && i < anni.size() ? i : -1;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int i = Indice(e.getX());
            if (i < 0) return null;
            Principale_Dashboard.RigaAnno r = anni.get(i);
            return "<html><b>" + r.anno() + "</b><br>Plusvalenze " + Principale_Dashboard.EuroConSegno(r.plusvalenze())
                    + "<br>Minusvalenze " + Principale_Dashboard.EuroConSegno(r.minusvalenze())
                    + "<br>Netto " + Principale_Dashboard.EuroConSegno(r.netto()) + "</html>";
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color fondo = UIManager.getColor("TextField.background");
            setBackground(fondo == null ? Color.WHITE : fondo);
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color testo = UIManager.getColor("Label.foreground");
                Color griglia = UIManager.getColor("Component.borderColor");
                g2.setFont(UIManager.getFont("Label.font"));
                FontMetrics fm = g2.getFontMetrics();
                int w = getWidth();
                int h = getHeight();
                int margineGiu = fm.getHeight() + 8;
                int plotW = w - MARGINE_SX - MARGINE_DX;
                int plotH = h - MARGINE_SU - margineGiu;
                if (anni.isEmpty() || plotW < 40 || plotH < 40) {
                    g2.setColor(testo);
                    String m = "Nessun movimento";
                    g2.drawString(m, (w - fm.stringWidth(m)) / 2, h / 2);
                    return;
                }
                double max = 0;
                double min = 0;
                for (Principale_Dashboard.RigaAnno r : anni) {
                    max = Math.max(max, r.plusvalenze().doubleValue());
                    min = Math.min(min, r.minusvalenze().doubleValue());
                }
                if (max == min) max = min + 1;
                double intervallo = max - min;
                int zero = MARGINE_SU + (int) Math.round(max / intervallo * plotH);
                // Zero, massimo e minimo
                g2.setColor(griglia);
                g2.drawLine(MARGINE_SX, zero, MARGINE_SX + plotW, zero);
                g2.drawLine(MARGINE_SX, MARGINE_SU, MARGINE_SX + plotW, MARGINE_SU);
                g2.drawLine(MARGINE_SX, MARGINE_SU + plotH, MARGINE_SX + plotW, MARGINE_SU + plotH);
                g2.setColor(testo);
                Etichetta(g2, fm, max, MARGINE_SU);
                Etichetta(g2, fm, 0, zero);
                if (min < 0) Etichetta(g2, fm, min, MARGINE_SU + plotH);
                double fetta = plotW / (double) anni.size();
                int larghezza = (int) Math.min(fetta * 0.55, 46);
                for (int i = 0; i < anni.size(); i++) {
                    Principale_Dashboard.RigaAnno r = anni.get(i);
                    int centro = MARGINE_SX + (int) Math.round(fetta * (i + 0.5));
                    if (r.anno() == selezionato) {
                        Color sel = UIManager.getColor("Table.selectionBackground");
                        g2.setColor(sel == null ? griglia : new Color(sel.getRed(), sel.getGreen(), sel.getBlue(), 45));
                        g2.fillRect((int) Math.round(MARGINE_SX + fetta * i), MARGINE_SU, (int) Math.round(fetta), plotH);
                    }
                    int alto = MARGINE_SU + (int) Math.round((max - r.plusvalenze().doubleValue()) / intervallo * plotH);
                    int basso = MARGINE_SU + (int) Math.round((max - r.minusvalenze().doubleValue()) / intervallo * plotH);
                    g2.setColor(Tabelle.verdeScuro);
                    if (zero > alto) g2.fillRect(centro - larghezza / 2, alto, larghezza, zero - alto);
                    g2.setColor(Tabelle.rosso);
                    if (basso > zero) g2.fillRect(centro - larghezza / 2, zero, larghezza, basso - zero);
                    int yNetto = MARGINE_SU + (int) Math.round((max - r.netto().doubleValue()) / intervallo * plotH);
                    Polygon rombo = new Polygon(new int[] {centro, centro + 5, centro, centro - 5},
                            new int[] {yNetto - 5, yNetto, yNetto + 5, yNetto}, 4);
                    g2.setColor(testo);
                    g2.fillPolygon(rombo);
                    g2.setColor(fondo == null ? Color.WHITE : fondo);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawPolygon(rombo);
                    g2.setColor(testo);
                    String a = Integer.toString(r.anno());
                    g2.drawString(a, centro - fm.stringWidth(a) / 2, MARGINE_SU + plotH + fm.getAscent() + 4);
                }
            } finally {
                g2.dispose();
            }
        }

        private static void Etichetta(Graphics2D g2, FontMetrics fm, double valore, int y) {
            String s = Compatto(valore);
            g2.drawString(s, MARGINE_SX - 6 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }

        /** 1.234 diventa 1,2k, 1.234.567 diventa 1,2M: le etichette dell'asse devono stare in poco spazio. */
        static String Compatto(double v) {
            double a = Math.abs(v);
            String segno = v < 0 ? "-" : "";
            if (a >= 1_000_000) return segno + String.format(Locale.ITALY, "%.1fM", a / 1_000_000);
            if (a >= 1_000) return segno + String.format(Locale.ITALY, "%.1fk", a / 1_000);
            return segno + String.format(Locale.ITALY, "%.0f", a);
        }
    }
}
