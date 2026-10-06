package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;

/**
 * Le novità e i bug corretti della versione, dalla sezione di {@code changelog.md} scelta da {@link NovitaVersione}.
 * Si apre da sola al primo avvio dopo un aggiornamento ({@link #MostraSeNuovaVersione}) e dal pulsante della finestra
 * Informazioni. Scritta a mano, senza {@code .form}, come {@link GUI_Informazioni}: il contenuto è tutto dinamico.
 */
public class GUI_NovitaVersione extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Apre la finestra, modale, sulle novità della versione in uso. Se le note non si trovano (jar senza la pagina) apre
     * la pagina online.
     * @param Proprietario finestra su cui centrarsi, eventualmente {@code null}
     */
    public static void Mostra(Window Proprietario) {
        String VersioneProgramma = VarStatiche.Versione;
        NovitaVersione.Sezione s = NovitaVersione.SezionePer(VersioneProgramma,
                NovitaVersione.Sezioni(NovitaVersione.Leggi()));
        if (s == null) {
            DocumentiAiuto.Apri(DocumentiAiuto.NOVITA_VERSIONI);
            return;
        }
        GUI_NovitaVersione d = new GUI_NovitaVersione(Proprietario, NovitaVersione.Titolo(VersioneProgramma, s),
                NovitaVersione.Html(s.Markdown()));
        d.setLocationRelativeTo(Proprietario);
        d.setVisible(true);
    }

    /**
     * All'avvio: mostra le novità la prima volta che parte una versione diversa dall'ultima vista
     * ({@link NovitaVersione#DaMostrare}) e ricorda la versione, anche quando non le mostra (installazione nuova).
     * @param Proprietario la finestra principale
     * @param ArchivioVuoto {@code true} se non ci sono movimenti
     */
    public static void MostraSeNuovaVersione(Window Proprietario, boolean ArchivioVuoto) {
        String VersioneProgramma = VarStatiche.Versione;
        String UltimaVista = DatabaseH2.Pers_Opzioni_Leggi(NovitaVersione.OPZIONE_ULTIMA_VERSIONE);
        boolean Mostrare = NovitaVersione.DaMostrare(VersioneProgramma, UltimaVista, ArchivioVuoto);
        if (!NovitaVersione.isVersioneValida(VersioneProgramma) || VersioneProgramma.equals(UltimaVista)) return;
        //Si scrive prima di mostrare: se la finestra fallisse, non si riproporrebbe a ogni avvio
        DatabaseH2.Pers_Opzioni_Scrivi(NovitaVersione.OPZIONE_ULTIMA_VERSIONE, VersioneProgramma);
        if (Mostrare && NovitaVersione.SezionePer(VersioneProgramma,
                NovitaVersione.Sezioni(NovitaVersione.Leggi())) != null) {
            Mostra(Proprietario);
        }
    }

    private GUI_NovitaVersione(Window Proprietario, String Titolo, String Html) {
        super(Proprietario, "Novità della versione", ModalityType.APPLICATION_MODAL);
        //Lo stesso corpo della finestra Informazioni: quello dell'interfaccia
        int Corpo = GUI_Informazioni.Corpo();

        //Il carattere si impone anche dal foglio di stile, così ogni elemento dell'HTML (paragrafi, elenchi, citazioni)
        //usa quello dell'applicazione e non uno scelto dal motore HTML di Swing
        String Stile = "<head><style>body, p, ul, li, blockquote { font-family: '" + FontApplicazione.FAMIGLIA
                + "'; font-size: " + Corpo + "pt; }</style></head>";
        JEditorPane corpo = new JEditorPane("text/html", Html.replaceFirst("<html>", "<html>" + Stile));
        corpo.setEditable(false);
        corpo.setOpaque(false);
        //Senza questa proprietà il pane si sceglie il font da sé e ignora quello dell'applicazione
        corpo.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        corpo.setFont(FontApplicazione.Font(Font.PLAIN, Corpo));
        corpo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        corpo.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                Funzioni.ApriWeb(e.getURL().toString());
            }
        });
        corpo.setCaretPosition(0);

        JScrollPane scorrimento = new JScrollPane(corpo,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);

        JLabel titolo = new JLabel(Titolo);
        titolo.setFont(FontApplicazione.Font(Font.BOLD, Corpo + 6));
        titolo.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 4));

        JButton tutte = new JButton("Novità di tutte le versioni");
        tutte.setToolTipText("Apre nel browser la pagina con le novità di ogni versione");
        tutte.addActionListener(e -> DocumentiAiuto.Apri(DocumentiAiuto.NOVITA_VERSIONI));
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        JPanel pulsanti = new JPanel();
        pulsanti.setLayout(new BoxLayout(pulsanti, BoxLayout.X_AXIS));
        pulsanti.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        pulsanti.add(tutte);
        pulsanti.add(Box.createHorizontalGlue());
        pulsanti.add(chiudi);

        JPanel contenuto = new JPanel(new BorderLayout());
        contenuto.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenuto.add(titolo, BorderLayout.NORTH);
        contenuto.add(scorrimento, BorderLayout.CENTER);
        contenuto.add(pulsanti, BorderLayout.SOUTH);

        setContentPane(contenuto);
        getRootPane().setDefaultButton(chiudi);
        setPreferredSize(new Dimension(820, 640));
        pack();
        Icone.AdattaIconeAlTema(contenuto);
    }
}
