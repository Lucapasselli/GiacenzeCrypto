package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Invio di una configurazione di importazione al catalogo della community ({@link CommunityConfigClient#Invia}).
 * Si apre dalla finestra della prova, solo dopo una prova senza problemi ({@link CommunityConfigurazioni#ProvaSenzaProblemi}).
 *
 * <p>Scritto a mano, senza {@code .form}. Mostra esattamente cosa parte: il JSON, cosi' com'e', e l'esito della prova
 * senza righe del CSV. L'invio richiede il consenso esplicito alla pubblicazione con licenza MIT.
 */
public class GUI_CondividiConfigurazione extends JDialog {

    private static final int MAX_AUTORE = 40;

    /**
     * @param Proprietario la finestra della prova
     * @param Json la configurazione provata
     * @param Esito l'esito della prova
     */
    public static void Mostra(Window Proprietario, File Json, ProvaConfigurazioneImport.Esito Esito) {
        String testo;
        try {
            byte[] dati = Files.readAllBytes(Json.toPath());
            //Si condivide il file provato, non una sua versione modificata dopo la prova
            if (!CommunityConfigurazioni.Sha256(dati).equals(Esito.impronta)) {
                JOptionPane.showMessageDialog(Proprietario, "La configurazione e' cambiata dopo la prova.\n"
                        + "Premi \"Ripeti la prova\" e condividila se la prova e' ancora senza problemi.",
                        "Condividi con la community", JOptionPane.WARNING_MESSAGE);
                return;
            }
            testo = new String(dati, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(Proprietario, "Il file non si puo' leggere: " + ex.getMessage(),
                    "Condividi con la community", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GUI_CondividiConfigurazione d = new GUI_CondividiConfigurazione(Proprietario, Json, Esito, testo);
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    private GUI_CondividiConfigurazione(Window Proprietario, File Json, ProvaConfigurazioneImport.Esito Esito, String Testo) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setTitle("Condividi la configurazione con la community");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        List<String> errori = ValidazioneConfigCommunity.Controlla(Testo);

        StringBuilder t = new StringBuilder("<html><body style='width:760px'>");
        t.append("La configurazione <b>").append(Html(Json.getName())).append("</b> viene inviata al catalogo della community "
                + "di Giacenze Crypto. Diventa scaricabile dagli altri utenti <b>solo dopo il controllo dell'autore del "
                + "programma</b>, che puo' anche rifiutarla.<br><br>");
        t.append("Viene inviato il file qui sotto, cosi' com'e', insieme all'esito della prova (")
                .append(Esito.righeLette).append(" righe, ").append(Esito.movimenti.size())
                .append(" movimenti, nessuna riga scartata) e alla versione del programma. <b>Nessuna riga del tuo CSV "
                + "viene inviata.</b> Il servizio conserva l'impronta del tuo indirizzo IP, per limitare gli invii.<br><br>");
        t.append("Controlla che il file non contenga dati tuoi: nomi, indirizzi dei wallet, identificativi nelle note o nei "
                + "commenti. Il nome visibile e' facoltativo.");
        if (!errori.isEmpty()) {
            t.append("<br><br><font color='#C0392B'><b>La configurazione non si puo' condividere:</b><br>");
            for (String e : errori.subList(0, Math.min(errori.size(), 12))) t.append("- ").append(Html(e)).append("<br>");
            t.append("</font>");
        }
        t.append("</body></html>");
        JLabel Spiegazione = new JLabel(t.toString());
        Spiegazione.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea Anteprima = new JTextArea(Testo);
        Anteprima.setEditable(false);
        Anteprima.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        Anteprima.setCaretPosition(0);
        JScrollPane Scorrimento = new JScrollPane(Anteprima);
        Scorrimento.setPreferredSize(new Dimension(800, 300));

        JTextField Autore = new JTextField(20);
        Autore.setToolTipText("Facoltativo: il nome con cui comparira' nel catalogo (massimo " + MAX_AUTORE + " caratteri)");
        JCheckBox Consenso = new JCheckBox("<html>Accetto che la configurazione sia pubblicata con licenza MIT, come il programma, "
                + "e confermo che non contiene dati personali</html>");
        JButton Invia = new JButton("Invia");
        Invia.setEnabled(false);
        Consenso.setEnabled(errori.isEmpty());
        Consenso.addActionListener(e -> Invia.setEnabled(Consenso.isSelected()));
        JButton Annulla = new JButton("Annulla");
        Annulla.addActionListener(e -> dispose());

        Invia.addActionListener(e -> {
            String autore = Autore.getText().trim();
            if (autore.length() > MAX_AUTORE) autore = autore.substring(0, MAX_AUTORE);
            final String nome = autore;
            Invia.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<CommunityConfigClient.EsitoInvio, Void>() {
                /** Invia fuori dall'EDT. */
                @Override
                protected CommunityConfigClient.EsitoInvio doInBackground() {
                    return CommunityConfigClient.Invia(Testo, Json.getName(), nome, CommunityConfigurazioni.RiepilogoPerInvio(Esito));
                }

                /** Mostra l'esito e, se riuscito, ricorda il codice di ritiro. */
                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        CommunityConfigClient.EsitoInvio r = get();
                        if (r.ok()) {
                            CommunityConfigurazioni.RegistraProposta(r.id(), Json.getName(), r.codiceRitiro());
                            JOptionPane.showMessageDialog(GUI_CondividiConfigurazione.this, r.messaggio()
                                    + "\n\nNumero della proposta: " + r.id()
                                    + "\nPuoi ritirarla dalla finestra \"Configurazioni della community\" in Importazioni.",
                                    "Condividi con la community", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                            return;
                        }
                        StringBuilder m = new StringBuilder(r.messaggio());
                        for (String err : r.errori().subList(0, Math.min(r.errori().size(), 12))) m.append("\n- ").append(err);
                        JOptionPane.showMessageDialog(GUI_CondividiConfigurazione.this, m.toString(),
                                "Condividi con la community", JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) {
                        LoggerGC.ScriviErrore(ex);
                    }
                    Invia.setEnabled(Consenso.isSelected());
                }
            }.execute();
        });

        JPanel Riga = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Riga.add(new JLabel("Nome visibile (facoltativo):"));
        Riga.add(Autore);
        JPanel Basso = new JPanel();
        Basso.setLayout(new BoxLayout(Basso, BoxLayout.Y_AXIS));
        Riga.setAlignmentX(LEFT_ALIGNMENT);
        Consenso.setAlignmentX(LEFT_ALIGNMENT);
        Basso.add(Riga);
        Basso.add(Consenso);
        Basso.add(Box.createVerticalStrut(6));
        JPanel Pulsanti = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Pulsanti.add(Invia);
        Pulsanti.add(Annulla);
        Pulsanti.setAlignmentX(LEFT_ALIGNMENT);
        Basso.add(Pulsanti);
        Basso.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));

        JPanel Contenuto = new JPanel(new BorderLayout());
        Contenuto.add(Spiegazione, BorderLayout.NORTH);
        Contenuto.add(Scorrimento, BorderLayout.CENTER);
        Contenuto.add(Basso, BorderLayout.SOUTH);
        setContentPane(Contenuto);
        pack();
    }

    private static String Html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
