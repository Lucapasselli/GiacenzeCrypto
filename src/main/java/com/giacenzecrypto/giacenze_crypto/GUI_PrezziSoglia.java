package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Elenco dei token e degli NFT che erano in giacenza ma non hanno un prezzo alle 00:00 del 1° gennaio, per
 * assegnarglielo (verifica della soglia dei 51.645,69 euro, {@link SogliaGiacenzaAnte2023}). Costruita a codice, senza
 * {@code .form}: una tabella e tre pulsanti. Le righe sono di due tipi: i token <b>senza alcun prezzo</b> (l'anno non è
 * verificabile finché non se ne assegna uno) e quelli <b>valorizzati al prezzo di acquisto</b> (il calcolo li ha già
 * valorizzati col primo ingresso, ma l'utente può dare loro il prezzo del 1° gennaio). La colonna "Prezzo usato dal
 * calcolo" dice quale dei due casi è. La logica di salvataggio è in {@link Principale_RT_Soglia#SalvaPrezzo}.
 *
 * <p>Con <b>Assegna prezzo</b> si apre la finestra dei prezzi di sempre ({@link Prezzi#GUI_ModificaPrezzoConAttesa}), che
 * propone le quotazioni trovate e permette di inserirne una a mano. Con <b>Valore zero</b> il token vale zero senza
 * altre domande (tipico dei token tecnici). Ogni prezzo è salvato subito: la finestra segnala solo se ne è stato salvato
 * almeno uno, perché il chiamante ricalcoli.</p>
 */
public class GUI_PrezziSoglia extends JDialog {

    private static final int COL_PREZZO = 6;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int anno;
    private final List<Moneta> mancanti;
    private final DefaultTableModel modello;
    private final JTable tabella;
    private boolean modificato = false;

    /**
     * Mostra la finestra (modale) e restituisce se è stato salvato almeno un prezzo.
     *
     * @param owner    finestra proprietaria
     * @param anno     anno della soglia
     * @param esito    esito dell'anno: i token senza prezzo ({@link SogliaGiacenzaAnte2023.Esito#monetePrezzoMancante}) e
     *                 quelli a prezzo di acquisto ({@link SogliaGiacenzaAnte2023.Esito#prezzoDiAcquisto})
     */
    public static boolean Mostra(Window owner, int anno, SogliaGiacenzaAnte2023.Esito esito) {
        GUI_PrezziSoglia g = new GUI_PrezziSoglia(owner, anno, esito);
        g.setVisible(true);
        return g.modificato;
    }

    private GUI_PrezziSoglia(Window owner, int anno, SogliaGiacenzaAnte2023.Esito esito) {
        super(owner, "Prezzi al 1° gennaio " + anno, ModalityType.APPLICATION_MODAL);
        this.anno = anno;
        this.mancanti = new ArrayList<>();
        List<Object> usati = new ArrayList<>(); //il testo della colonna "Prezzo usato dal calcolo", una per riga
        for (Moneta m : esito.monetePrezzoMancante) {
            mancanti.add(m);
            usati.add("Nessun prezzo (vale zero, anno non verificabile)");
        }
        for (SogliaGiacenzaAnte2023.PrezzoAcquisto a : esito.prezzoDiAcquisto) {
            mancanti.add(a.moneta);
            usati.add("Prezzo di acquisto " + String.format(Locale.ITALY, "%,.6f", a.prezzoUnitario) + " euro del " + DATA.format(a.data));
        }

        JLabel testo = new JLabel("<html><body style='width:900px'>Questi token e NFT erano in giacenza nel " + anno + " ma non hanno un prezzo alle 00:00 del 1° gennaio "
                + anno + ". La norma non dice come valorizzarli: quelli con un movimento di ingresso valorizzato sono calcolati al <b>prezzo di acquisto</b>, "
                + "quelli senza nemmeno quello valgono zero e l'anno non può essere verificato.<br>"
                + "Per dare a un token il prezzo del 1° gennaio: <b>Assegna prezzo</b> (un token alla volta) propone le quotazioni trovate o permette di inserirne una, "
                + "<b>Valore zero</b> è per i token tecnici o senza valore e vale per tutte le righe selezionate (Ctrl+A le seleziona tutte). "
                + "Il prezzo vale anche per gli altri calcoli che lo usano.</body></html>");
        testo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modello = new DefaultTableModel(
                new Object[] {"Token", "Tipo", "Rete", "Address", "Quantità massima", "Prezzo usato dal calcolo", "Prezzo unitario assegnato"}, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        for (int i = 0; i < mancanti.size(); i++) {
            Moneta m = mancanti.get(i);
            modello.addRow(new Object[] {m.Moneta, m.Tipo == null ? "" : m.Tipo, m.Rete == null ? "" : m.Rete,
                m.MonetaAddress == null ? "" : m.MonetaAddress, m.Qta, usati.get(i), ""});
        }
        tabella = new JTable(modello);
        tabella.setAutoCreateRowSorter(true);
        tabella.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        int[] larghezze = {110, 60, 60, 200, 110, 330, 140};
        for (int i = 0; i < larghezze.length; i++) {
            tabella.getColumnModel().getColumn(i).setPreferredWidth(larghezze[i]);
        }
        JScrollPane scorri = new JScrollPane(tabella);
        scorri.setPreferredSize(new Dimension(1010, 320));

        JButton assegna = new JButton("Assegna prezzo");
        assegna.addActionListener(e -> AssegnaPrezzo());
        JButton zero = new JButton("Valore zero");
        zero.addActionListener(e -> ValoreZero());
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        javax.swing.JPanel pulsanti = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
        pulsanti.add(assegna);
        pulsanti.add(zero);
        pulsanti.add(chiudi);

        setLayout(new BorderLayout());
        add(testo, BorderLayout.NORTH);
        add(scorri, BorderLayout.CENTER);
        add(pulsanti, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Il token della riga selezionata, o {@code null} (con un avviso) se la selezione non è di una sola riga. */
    private Moneta Selezionato() {
        int[] viste = tabella.getSelectedRows();
        if (viste.length != 1) {
            Messaggi.WarningMessage(viste.length == 0 ? "Nessun token selezionato" : "Più token selezionati",
                    viste.length == 0 ? "Selezionare prima un token dall'elenco." : "Per assegnare un prezzo selezionare un solo token.", this);
            return null;
        }
        return mancanti.get(tabella.convertRowIndexToModel(viste[0]));
    }

    private void Segna(Moneta m, BigDecimal unitario) {
        int indice = mancanti.indexOf(m);
        modello.setValueAt(unitario.stripTrailingZeros().toPlainString(), indice, COL_PREZZO);
        modello.setValueAt("Assegnato, si applica al ricalcolo", indice, COL_PREZZO - 1);
        modificato = true;
    }

    /** Prezzo zero a tutte le righe selezionate. Segnala quelle non salvate, senza fermarsi alla prima. */
    private void ValoreZero() {
        int[] viste = tabella.getSelectedRows();
        if (viste.length == 0) {
            Messaggi.WarningMessage("Nessun token selezionato", "Selezionare prima uno o più token dall'elenco.", this);
            return;
        }
        //Si risolvono prima i token: Segna cambia il modello, e le posizioni di vista si spostano con l'ordinamento
        List<Moneta> scelti = new java.util.ArrayList<>();
        for (int v : viste) {
            scelti.add(mancanti.get(tabella.convertRowIndexToModel(v)));
        }
        StringBuilder nonSalvati = new StringBuilder();
        for (Moneta m : scelti) {
            if (Principale_RT_Soglia.SalvaPrezzo(m, anno, BigDecimal.ZERO, "Personalizzato", 0)) {
                Segna(m, BigDecimal.ZERO);
            } else {
                nonSalvati.append(nonSalvati.length() > 0 ? ", " : "").append(m.Moneta);
            }
        }
        if (nonSalvati.length() > 0) {
            Messaggi.WarningMessage("Prezzo non salvato", "Non è stato possibile salvare il prezzo di: " + nonSalvati + ".", this);
        }
    }

    private void AssegnaPrezzo() {
        Moneta scelto = Selezionato();
        if (scelto == null) {
            return;
        }
        //Si chiede il prezzo di una unità: il totale che torna dalla finestra è allora il prezzo unitario
        Moneta una = new Moneta();
        una.InserisciValori(scelto.Moneta, "1", scelto.MonetaAddress, scelto.Tipo);
        una.Rete = scelto.Rete;
        String[] ritorno = new String[2];
        long istante = Principale_RT_Soglia.IstantePrezzo(anno);
        long daCancellare = Prezzi.GUI_ModificaPrezzoConAttesa(una, ritorno, this, istante, "0");
        if (ritorno[0] == null) {
            return; //finestra chiusa senza scegliere
        }
        Prezzi.InfoPrezzo info = ritorno[1] == null ? new Prezzi.InfoPrezzo() : new Prezzi.InfoPrezzo(ritorno[1]);
        BigDecimal unitario = info.prezzoUnitario != null ? info.prezzoUnitario : numero(ritorno[0]);
        if (unitario == null) {
            Messaggi.WarningMessage("Prezzo non valido", "Il prezzo scelto non è un numero.", this);
            return;
        }
        if (Principale_RT_Soglia.SalvaPrezzo(scelto, anno, unitario, info.Fonte, daCancellare)) {
            Segna(scelto, unitario);
        } else {
            Messaggi.WarningMessage("Prezzo non salvato", "Non è stato possibile salvare il prezzo di " + scelto.Moneta + ".", this);
        }
    }

    private static BigDecimal numero(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
