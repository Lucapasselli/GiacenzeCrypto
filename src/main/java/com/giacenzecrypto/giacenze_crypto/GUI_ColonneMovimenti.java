package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableModel;
import org.jsoup.Jsoup;

/**
 * Dialogo (senza {@code .form}, contenuto dinamico) per scegliere quali colonne mostrare nella
 * tabella dei movimenti. Spostare e ridimensionare le colonne si fa trascinando direttamente le
 * intestazioni: qui si decide solo la visibilità, più il "Ripristina predefinito".
 *
 * <p>Non applica e non salva nulla: restituisce un {@link LayoutColonneMovimenti} (via
 * {@link #getRisultato()}) oppure segnala {@link #isRipristino()} / {@link #isAnnullato()}, e lascia
 * che sia {@code Principale} ad applicarlo alla tabella e a scriverlo in {@code personale.mv.db}.
 */
public class GUI_ColonneMovimenti extends JDialog {

    private final Map<Integer, JCheckBox> caselle = new LinkedHashMap<>();
    private final LayoutColonneMovimenti statoIniziale;

    private boolean annullato = true;
    private boolean ripristino = false;
    private LayoutColonneMovimenti risultato;

    public GUI_ColonneMovimenti(Window owner, javax.swing.JTable tabellaMovimenti) {
        super(owner, "Colonne della tabella movimenti", ModalityType.APPLICATION_MODAL);

        statoIniziale = LayoutColonneMovimenti.daTabella(tabellaMovimenti);
        List<Integer> visibiliOra = statoIniziale.ordine();
        TableModel model = tabellaMovimenti.getModel();

        JLabel intestazione = new JLabel("<html>Scegli le colonne da mostrare nella tabella dei movimenti.<br>"
                + "Per spostarle o ridimensionarle trascina direttamente le intestazioni.</html>");
        intestazione.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        for (int m : LayoutColonneMovimenti.colonneOffribili()) {
            String nome = Jsoup.parse(String.valueOf(model.getColumnName(m))).text().trim();
            if (nome.isEmpty() || "null".equalsIgnoreCase(nome)) continue;
            boolean fissa = LayoutColonneMovimenti.COLONNE_FISSE.contains(m);
            JCheckBox cb = new JCheckBox(fissa ? nome + "  (sempre visibile)" : nome);
            cb.setSelected(fissa || visibiliOra.contains(m));
            cb.setEnabled(!fissa);
            caselle.put(m, cb);
            lista.add(cb);
        }

        JScrollPane scroll = new JScrollPane(lista,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton bRipristina = new JButton("Ripristina predefinito");
        bRipristina.addActionListener(e -> {
            annullato = false;
            ripristino = true;
            risultato = null;
            dispose();
        });

        JButton bAnnulla = new JButton("Annulla");
        bAnnulla.addActionListener(e -> {
            annullato = true;
            dispose();
        });

        JButton bApplica = new JButton("Applica");
        bApplica.addActionListener(e -> {
            annullato = false;
            ripristino = false;
            risultato = componiRisultato();
            dispose();
        });

        JPanel sinistra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sinistra.add(bRipristina);
        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        destra.add(bAnnulla);
        destra.add(bApplica);
        JPanel sud = new JPanel(new BorderLayout());
        sud.add(sinistra, BorderLayout.WEST);
        sud.add(destra, BorderLayout.EAST);
        sud.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel contenuto = new JPanel(new BorderLayout());
        contenuto.add(intestazione, BorderLayout.NORTH);
        contenuto.add(scroll, BorderLayout.CENTER);
        contenuto.add(sud, BorderLayout.SOUTH);
        setContentPane(contenuto);

        getRootPane().setDefaultButton(bApplica);
        setMinimumSize(new Dimension(430, 360));
        pack();
        if (getHeight() > 640) setSize(getWidth(), 640);
    }

    /**
     * Ordine: prima le colonne già visibili nell'ordine attuale (l'utente può averle trascinate),
     * poi quelle appena spuntate nell'ordine del dialogo. Le larghezze correnti vengono conservate
     * per le colonne che restano.
     */
    private LayoutColonneMovimenti componiRisultato() {
        List<Integer> selezionate = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> e : caselle.entrySet()) {
            if (e.getValue().isSelected()) selezionate.add(e.getKey());
        }
        List<Integer> ordine = new ArrayList<>();
        for (int m : statoIniziale.ordine()) {
            if (selezionate.contains(m)) ordine.add(m);
        }
        for (int m : selezionate) {
            if (!ordine.contains(m)) ordine.add(m);
        }
        Map<Integer, Integer> larghezze = new LinkedHashMap<>();
        Map<Integer, Integer> correnti = statoIniziale.larghezze();
        for (int m : ordine) {
            if (correnti.containsKey(m)) larghezze.put(m, correnti.get(m));
        }
        return new LayoutColonneMovimenti(ordine, larghezze);
    }

    public boolean isAnnullato() {
        return annullato;
    }

    public boolean isRipristino() {
        return ripristino;
    }

    public LayoutColonneMovimenti getRisultato() {
        return risultato;
    }
}
