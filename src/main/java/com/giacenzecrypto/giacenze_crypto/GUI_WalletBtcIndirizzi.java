package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Elenco degli indirizzi di un wallet BTC multi-indirizzo, uno per riga. La logica (controllo
 * dell'elenco, rielaborazione dei movimenti gia' importati) sta in {@link WalletBtcMultiIndirizzo}.
 * Scritto a mano, senza {@code .form}: e' un'area di testo e due pulsanti.
 */
public class GUI_WalletBtcIndirizzi extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final String Nome;
    private final JTextArea Area = new JTextArea();

    /** Apre l'elenco del wallet {@code Nome} (senza il suffisso " (BTC)") */
    public static void Mostra(String Nome, Window Proprietario) {
        GUI_WalletBtcIndirizzi d = new GUI_WalletBtcIndirizzi(Nome, Proprietario);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    private GUI_WalletBtcIndirizzi(String Nome, Window Proprietario) {
        super(Proprietario, "Indirizzi del wallet BTC " + Nome, ModalityType.APPLICATION_MODAL);
        this.Nome = Nome;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel Spiegazione = new JLabel("<html>Indirizzi Bitcoin che compongono il wallet <b>" + Nome
                + "</b>, uno per riga.<br>Le transazioni fra questi indirizzi (resti, consolidamenti) restano "
                + "interne al wallet.<br>Se il wallet ha gia' movimenti, aggiungere o togliere un indirizzo "
                + "rielabora solo le transazioni di quell'indirizzo.</html>");
        Spiegazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Area.setFont(new Font("Noto Sans", Font.PLAIN, 14));
        Area.setText(String.join("\n", DatabaseH2.Pers_WalletBtcIndirizzi_Leggi(Nome)));
        JScrollPane Scroll = new JScrollPane(Area);
        Scroll.setPreferredSize(new Dimension(620, 300));
        Scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 10, 0, 10), Scroll.getBorder()));

        JButton Salva = new JButton("Salva");
        Salva.addActionListener(e -> Salva());
        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Annulla);
        Pulsanti.add(Salva);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Spiegazione, BorderLayout.NORTH);
        getContentPane().add(Scroll, BorderLayout.CENTER);
        getContentPane().add(Pulsanti, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(Salva);
        pack();
    }

    private void Salva() {
        Set<String> Indirizzi = WalletBtcMultiIndirizzo.LeggiElenco(Area.getText());
        String Errore = WalletBtcMultiIndirizzo.ValidaIndirizzi(Nome, Indirizzi,
                DatabaseH2.Pers_Wallets_LeggiTabella(), DatabaseH2.Pers_WalletBtcIndirizzi_LeggiTutti());
        if (Errore != null) {
            Messaggi.WarningMessage("Elenco non valido", Errore, this);
            return;
        }
        if (WalletBtcMultiIndirizzo.ModificaIndirizzi(Nome, Indirizzi, this)) dispose();
    }
}
