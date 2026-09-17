package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Mostra le versioni precedenti di un movimento, cioè le righe che
 * {@link MovimentiStorico} ha salvato prima di ogni modifica manuale.
 *
 * <p>Dialogo senza {@code .form} (contenuto dinamico: il numero di versioni non è noto a priori),
 * sullo schema di {@link GUI_ColonneMovimenti}.</p>
 *
 * <p><b>È dichiaratamente statico e non riusa
 * {@code GUI_DettaglioTransazione.TransazioniCrypto_CompilaTextPaneDatiMovimento}</b>: quel metodo
 * insegue i movimenti collegati dal campo {@code [20]} rileggendoli <i>dal vivo</i> da
 * {@link Principale#MappaCryptoWallet}, e su una riga storica quei collegamenti possono puntare a ID
 * non più esistenti o nel frattempo riassegnati — quindi darebbe {@code NullPointerException} o, peggio,
 * dati plausibili ma falsi. Qui si mostrano solo le colonne leggibili così come erano, senza nessun
 * calcolo, nessun inseguimento di collegamenti e nessun pulsante DeFi.</p>
 *
 * @author luca.passelli
 */
public class GUI_StoricoMovimento extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<String[]> versioni;
    private final DefaultTableModel modello;

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
        List<String[]> Versioni = DatabaseH2.StoricoMovimenti_Leggi(MovimentiStorico.LignaggioDi(ID));
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
        super(Proprietario, "Versioni precedenti del movimento", ModalityType.APPLICATION_MODAL);
        this.versioni = Versioni;

        JLabel intestazione = new JLabel("<html>Movimento <b>" + ID + "</b><br>"
                + "Queste sono le righe come erano <b>prima</b> di ogni modifica fatta a mano: "
                + "sono una fotografia, non movimenti attivi.</html>");
        intestazione.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        modello = new DefaultTableModel(new String[]{"Campo", "Valore"}, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        JTable tabella = new JTable(modello);
        tabella.getColumnModel().getColumn(0).setPreferredWidth(190);
        tabella.getColumnModel().getColumn(1).setPreferredWidth(360);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);

        JPanel nord = new JPanel(new BorderLayout());
        nord.add(intestazione, BorderLayout.NORTH);

        //La tendina compare solo se c'è davvero più di una versione da scegliere
        if (versioni.size() > 1) {
            JComboBox<String> scelta = new JComboBox<>();
            for (String[] v : versioni) {
                scelta.addItem(DescrizioneVersione(v));
            }
            scelta.addActionListener(e -> Compila(scelta.getSelectedIndex()));
            JPanel rigaScelta = new JPanel(new FlowLayout(FlowLayout.LEFT));
            rigaScelta.add(new JLabel("Versione:"));
            rigaScelta.add(scelta);
            nord.add(rigaScelta, BorderLayout.SOUTH);
        }

        JButton bChiudi = new JButton("Chiudi");
        bChiudi.addActionListener(e -> dispose());
        JPanel sud = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sud.add(bChiudi);
        sud.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel contenuto = new JPanel(new BorderLayout());
        contenuto.add(nord, BorderLayout.NORTH);
        contenuto.add(new JScrollPane(tabella), BorderLayout.CENTER);
        contenuto.add(sud, BorderLayout.SOUTH);
        setContentPane(contenuto);

        Compila(0);

        getRootPane().setDefaultButton(bChiudi);
        setMinimumSize(new Dimension(620, 420));
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
            case MovimentiStorico.OP_IN_PLACE ->
                "Modifica dei dati (ID invariato)";
            default ->
                Operazione;
        };
    }

    /** Riempie la tabella con la versione scelta. */
    private void Compila(int Indice) {
        modello.setRowCount(0);
        if (Indice < 0 || Indice >= versioni.size()) {
            return;
        }
        String[] Versione = versioni.get(Indice);
        String[] v = Importazioni.DeserializzaRiga(Versione[2]);

        Riga("Modifica effettuata il", DataLeggibile(Versione[1]));
        Riga("Operazione", OperazioneLeggibile(Versione[3]));
        if (!Funzioni.noData(Versione[0]) && !Versione[0].equals(v[0])) {
            Riga("ID prima della modifica", Versione[0]);
        }
        modello.addRow(new String[]{"", ""});

        Riga("ID", v[0]);
        Riga("Data e ora", v[1]);
        Riga("Exchange / wallet", v[3]);
        Riga("Sotto-wallet", v[4]);
        Riga("Causale", v[5]);
        Riga("Causale originale del file", v[7]);
        Riga("Moneta in uscita", Moneta(v[8], v[10]));
        Riga("Moneta in entrata", Moneta(v[11], v[13]));
        Riga("Controvalore in euro", v[15]);
        Riga("Classificazione", v[18]);
        Riga("Note", v[21]);
        //Il documento di origine si mostra come testo e non come collegamento: è la fotografia di un
        //riferimento che nel frattempo può essere stato eliminato.
        Riga("Documento di origine (id)", v[41]);
    }

    private void Riga(String Campo, String Valore) {
        if (Funzioni.noData(Valore)) {
            return;
        }
        modello.addRow(new String[]{Campo, Valore});
    }

    private static String Moneta(String Simbolo, String Qta) {
        if (Funzioni.noData(Simbolo) && Funzioni.noData(Qta)) {
            return "";
        }
        return (Qta == null ? "" : Qta) + " " + (Simbolo == null ? "" : Simbolo);
    }
}
