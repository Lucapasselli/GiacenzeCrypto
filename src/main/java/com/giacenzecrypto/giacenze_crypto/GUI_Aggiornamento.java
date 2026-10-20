package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * La finestra degli aggiornamenti: versione in uso, canale (stabile o beta), controllo all'avvio, esito del
 * controllo e, se c'è una versione nuova, la richiesta all'utente se vuole aggiornare. Nessun aggiornamento
 * parte senza che l'utente lo chieda da qui.
 *
 * <p>Scritta a mano, senza {@code .form}, come {@link GUI_NovitaVersione}: il contenuto è tutto dinamico. La
 * logica sta in {@link Aggiornamenti}.
 */
public class GUI_Aggiornamento extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String TESTO_STABILE = "Stabile";
    private static final String TESTO_BETA = "Beta (anche le versioni di prova)";
    /** Larghezza dei paragrafi: l'HTML di Swing va a capo solo dentro un blocco di larghezza fissata. */
    private static final String PARAGRAFO = "<html><div style='width:460px'>";

    private final JComboBox<String> canale = new JComboBox<>(new String[]{TESTO_STABILE, TESTO_BETA});
    private final JLabel descrizioneCanale = new JLabel();
    private final JLabel esito = new JLabel();
    private final JButton controlla = new JButton("Controlla ora");
    private final JButton aggiorna = new JButton("Aggiorna");
    private final JButton ignora = new JButton("Ignora questa versione");

    /** La release proposta in questo momento, {@code null} se non ce n'è una. */
    private Aggiornamenti.Rilascio proposto;

    /**
     * Apre la finestra, modale. Se in questa sessione c'è già un esito lo mostra subito, altrimenti controlla.
     * @param Proprietario finestra su cui centrarsi, eventualmente {@code null}
     */
    public static void Mostra(Window Proprietario) {
        if (!Aggiornamenti.Abilitati()) return;
        GUI_Aggiornamento d = new GUI_Aggiornamento(Proprietario);
        d.setLocationRelativeTo(Proprietario);
        Aggiornamenti.Risultato r = Aggiornamenti.UltimoRisultato();
        if (r != null && r.Stato() != Aggiornamenti.Stato.SCONOSCIUTO) {
            d.MostraEsito(r);
        } else {
            d.Controlla();
        }
        d.setVisible(true);
    }

    private GUI_Aggiornamento(Window Proprietario) {
        super(Proprietario, "Aggiornamenti", ModalityType.APPLICATION_MODAL);
        int Corpo = GUI_Informazioni.Corpo();

        JLabel titolo = new JLabel("Versione in uso: " + VarStatiche.Versione);
        titolo.setFont(FontApplicazione.Font(Font.BOLD, Corpo + 4));
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rigaCanale = new JPanel();
        rigaCanale.setLayout(new BoxLayout(rigaCanale, BoxLayout.X_AXIS));
        rigaCanale.setAlignmentX(Component.LEFT_ALIGNMENT);
        rigaCanale.add(new JLabel("Canale: "));
        canale.setMaximumSize(canale.getPreferredSize());
        rigaCanale.add(canale);
        rigaCanale.add(Box.createHorizontalGlue());
        canale.setSelectedItem(Aggiornamenti.CanaleScelto() == Aggiornamenti.Canale.BETA ? TESTO_BETA : TESTO_STABILE);
        canale.addActionListener(e -> {
            Aggiornamenti.ScriviCanale(CanaleSelezionato());
            AggiornaDescrizioneCanale();
            Controlla();
        });
        descrizioneCanale.setAlignmentX(Component.LEFT_ALIGNMENT);
        AggiornaDescrizioneCanale();

        JCheckBox allAvvio = new JCheckBox("Controlla gli aggiornamenti all'avvio del programma",
                Aggiornamenti.ControllaAllAvvio());
        allAvvio.setAlignmentX(Component.LEFT_ALIGNMENT);
        allAvvio.addActionListener(e -> Aggiornamenti.ScriviControllaAllAvvio(allAvvio.isSelected()));

        esito.setAlignmentX(Component.LEFT_ALIGNMENT);
        esito.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(titolo);
        centro.add(Box.createVerticalStrut(12));
        centro.add(rigaCanale);
        centro.add(Box.createVerticalStrut(6));
        centro.add(descrizioneCanale);
        centro.add(Box.createVerticalStrut(8));
        centro.add(allAvvio);
        centro.add(esito);
        centro.add(Box.createVerticalGlue());

        controlla.addActionListener(e -> Controlla());
        aggiorna.setToolTipText("Scarica e installa la nuova versione, chiedendo conferma prima di chiudere il programma");
        aggiorna.addActionListener(e -> Aggiorna());
        ignora.setToolTipText("Non segnala più questa versione all'avvio, segnalerà la prossima");
        ignora.addActionListener(e -> {
            if (proposto != null) Aggiornamenti.Ignora(proposto.Versione());
            dispose();
        });
        JButton chiudi = new JButton("Chiudi");
        chiudi.addActionListener(e -> dispose());
        aggiorna.setVisible(false);
        ignora.setVisible(false);

        JPanel pulsanti = new JPanel();
        pulsanti.setLayout(new BoxLayout(pulsanti, BoxLayout.X_AXIS));
        pulsanti.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        pulsanti.add(controlla);
        pulsanti.add(Box.createHorizontalGlue());
        pulsanti.add(ignora);
        pulsanti.add(Box.createHorizontalStrut(6));
        pulsanti.add(aggiorna);
        pulsanti.add(Box.createHorizontalStrut(6));
        pulsanti.add(chiudi);

        JPanel contenuto = new JPanel(new BorderLayout());
        contenuto.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenuto.add(centro, BorderLayout.CENTER);
        contenuto.add(pulsanti, BorderLayout.SOUTH);

        setContentPane(contenuto);
        getRootPane().setDefaultButton(chiudi);
        pack();
        Icone.AdattaIconeAlTema(contenuto);
    }

    private Aggiornamenti.Canale CanaleSelezionato() {
        return TESTO_BETA.equals(canale.getSelectedItem()) ? Aggiornamenti.Canale.BETA : Aggiornamenti.Canale.STABILE;
    }

    private void AggiornaDescrizioneCanale() {
        String testo = CanaleSelezionato() == Aggiornamenti.Canale.BETA
                ? "Propone anche le versioni di prova intermedie (per esempio 1.0.65.01), pubblicate prima della "
                + "versione definitiva per provare le novità. Possono contenere errori."
                : "Propone solo le versioni definitive.";
        descrizioneCanale.setText(PARAGRAFO + testo
                + "<br><br>Passando dal canale beta allo stabile non si torna a una versione precedente: si resta su "
                + "quella in uso finché non esce una versione stabile più recente.</div></html>");
    }

    /** Cosa succederà premendo "Aggiorna", detto prima di premerlo. */
    private String ComeAggiorna() {
        AggiornamentoInstallazione.Installazione inst = AggiornamentoInstallazione.Rileva();
        if (MotivoSoloManuale(inst, AggiornamentoInstallazione.AllegatoPer(inst.Tipo(), proposto)) != null) {
            return "Verrà aperta la pagina della versione, da cui scaricarla.";
        }
        return "La nuova versione verrà scaricata e verificata, poi verrà chiesta conferma prima di chiudere il "
                + "programma per installarla.";
    }

    /** Controlla in background, perché la richiesta può durare qualche secondo. */
    private void Controlla() {
        controlla.setEnabled(false);
        aggiorna.setVisible(false);
        ignora.setVisible(false);
        esito.setText("Controllo in corso...");
        Aggiornamenti.Canale c = CanaleSelezionato();
        Thread t = new Thread(() -> {
            Aggiornamenti.Risultato r = Aggiornamenti.Controlla(c, false);
            SwingUtilities.invokeLater(() -> MostraEsito(r));
        }, "ControlloAggiornamenti");
        t.setDaemon(true);
        t.start();
    }

    private void MostraEsito(Aggiornamenti.Risultato r) {
        controlla.setEnabled(true);
        proposto = null;
        switch (r.Stato()) {
            case DISPONIBILE -> {
                proposto = r.Rilascio();
                String tipo = proposto.Beta() ? "versione di prova (beta)" : "versione stabile";
                esito.setText(PARAGRAFO + "<b>È disponibile la " + tipo + " " + proposto.Versione() + ".</b><br>"
                        + "Vuoi aggiornare? " + ComeAggiorna() + "</div></html>");
                aggiorna.setVisible(true);
                ignora.setVisible(true);
            }
            case AGGIORNATO -> esito.setText("Il programma è aggiornato.");
            default -> esito.setText(PARAGRAFO + "Non è stato possibile verificare la presenza di aggiornamenti. "
                    + "Si riproverà al prossimo avvio.</div></html>");
        }
        pack();
    }

    /**
     * L'utente ha chiesto di aggiornare. Dove il programma può installare da sé: scarica e verifica, chiede conferma
     * prima di chiudere, fa il backup, avvia l'installazione ed esce. Altrimenti spiega cosa fare e apre la pagina.
     */
    private void Aggiorna() {
        if (proposto == null) return;
        AggiornamentoInstallazione.Installazione inst = AggiornamentoInstallazione.Rileva();
        Aggiornamenti.Allegato allegato = AggiornamentoInstallazione.AllegatoPer(inst.Tipo(), proposto);
        String motivo = MotivoSoloManuale(inst, allegato);
        if (motivo != null) {
            Messaggi.InfoMessage("Aggiornamento", "Versione " + proposto.Versione(),
                    motivo + "<br><br>Verrà aperta la pagina della versione.", this);
            Funzioni.ApriWeb(proposto.Pagina());
            dispose();
            return;
        }

        Path cartella = AggiornamentoInstallazione.Cartella().resolve(proposto.Versione());
        Path scaricato = cartella.resolve(allegato.Nome());
        if (!Scarica(allegato, scaricato)) return;

        //Lo zip si estrae subito: se non è del tipo atteso lo si scopre ora, con il programma ancora aperto
        Path pacchetto = scaricato;
        if (inst.Tipo() != AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS) {
            try {
                Path estratto = cartella.resolve("estratto");
                AggiornamentoInstallazione.Estrai(scaricato, estratto);
                pacchetto = AggiornamentoInstallazione.RadiceEstratta(estratto, inst.Tipo());
                if (pacchetto == null) throw new IOException("Il pacchetto scaricato non contiene il programma atteso.");
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                Messaggi.WarningMessage("Aggiornamento", "Pacchetto non utilizzabile", ex.getMessage(), this);
                return;
            }
        }

        if (!ConfermaInstallazione(inst)) return;

        Predicate<BooleanSupplier> chiusura = Aggiornamenti.Chiusura();
        if (chiusura == null) {
            Messaggi.WarningMessage("Aggiornamento", "Impossibile chiudere il programma da qui.", "", this);
            return;
        }
        final Path daInstallare = pacchetto;
        dispose();
        //Se la chiusura va a buon fine il programma termina dentro questa chiamata
        chiusura.test(() -> BackupEAvvia(inst, daInstallare, cartella));
    }

    /** @return perché questa installazione va aggiornata a mano, o {@code null} se il programma può farlo da sé */
    private String MotivoSoloManuale(AggiornamentoInstallazione.Installazione inst, Aggiornamenti.Allegato allegato) {
        if (!inst.Tipo().Automatico) return AggiornamentoInstallazione.IstruzioniManuali(inst.Tipo());
        if (allegato == null) {
            return "La versione " + proposto.Versione() + " non contiene il pacchetto per la " + inst.Tipo().Descrizione
                    + " in uso.";
        }
        if (allegato.Sha256().isBlank()) {
            return "La versione non pubblica l'impronta (sha256) del pacchetto, che quindi non può essere verificato.";
        }
        if (!Files.isWritable(inst.Radice())) {
            return "La cartella del programma (" + inst.Radice() + ") non è scrivibile.";
        }
        return null;
    }

    /** Scarica con barra di avanzamento e possibilità di annullare. @return {@code true} se il file è pronto e verificato */
    private boolean Scarica(Aggiornamenti.Allegato allegato, Path destinazione) {
        Download progress = new Download();
        progress.setLocationRelativeTo(this);
        progress.Titolo("Aggiornamento");
        progress.SetLabel("Scaricamento della versione " + proposto.Versione() + "...");
        progress.RipristinaStdout();
        progress.SetMassimo(100);
        final boolean[] pronto = {false};
        final String[] errore = {null};
        long dimensione = Math.max(1, allegato.Dimensione());
        Thread t = new Thread(() -> {
            try {
                pronto[0] = AggiornamentoInstallazione.Scarica(allegato, destinazione, letti -> {
                    int perc = (int) Math.min(100, letti * 100 / dimensione);
                    SwingUtilities.invokeLater(() -> {
                        progress.updateProgress(perc);
                        progress.SetMessaggioAvanzamento(Mb(letti) + " di " + Mb(dimensione) + " MB");
                    });
                }, () -> progress.FineThread);
            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
                errore[0] = ex.getMessage();
            }
            SwingUtilities.invokeLater(progress::ChiudiFineLavoro);
        }, "ScaricamentoAggiornamento");
        progress.SetThread(t);
        t.start();
        progress.setVisible(true);
        try {
            t.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (errore[0] != null) {
            Messaggi.WarningMessage("Aggiornamento", "Scaricamento non riuscito", errore[0], this);
        }
        return pronto[0];
    }

    private static String Mb(long b) {
        return String.format(java.util.Locale.ITALY, "%.1f", b / 1048576.0);
    }

    /** L'ultima conferma, con quello che sta per succedere. */
    private boolean ConfermaInstallazione(AggiornamentoInstallazione.Installazione inst) {
        String come = inst.Tipo() == AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS
                ? "il programma di installazione aggiornerà il programma"
                : "i file nuovi verranno copiati nella cartella del programma (i dati non vengono toccati)";
        AppDialog.DialogResult r = AppDialog.builder(this)
                .windowTitle("Aggiornamento")
                .bodyTitle("Installare la versione " + proposto.Versione() + "?")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("La versione " + proposto.Versione() + " è stata scaricata e verificata.")
                .details("Per installarla il programma verrà chiuso. Prima verrà fatto un backup dell'archivio "
                        + "(in ArchivioBackup), poi " + come + " e la nuova versione si avvierà da sola.\n\n"
                        + "Se ci sono movimenti non salvati verrà chiesto se salvarli.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("installa", "Chiudi e installa")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        return r != null && r.isAction("installa");
    }

    /**
     * Eseguita dalla chiusura del programma, dopo i salvataggi: backup automatico e avvio dello script. Se uno dei due
     * non riesce il programma resta aperto.
     */
    private boolean BackupEAvvia(AggiornamentoInstallazione.Installazione inst, Path pacchetto, Path cartella) {
        Window proprietario = getOwner();
        Backup_Restore.OpzioniBackup opz = new Backup_Restore.OpzioniBackup();
        opz.Automatico = true;
        final File[] creato = new File[1];
        Download progress = new Download();
        progress.setLocationRelativeTo(proprietario);
        Thread t = new Thread(() -> {
            progress.Titolo("Backup prima dell'aggiornamento");
            progress.SetLabel("Preparazione...");
            creato[0] = Backup_Restore.Esegui(opz, progress);
            progress.ChiudiFinestra();
        }, "BackupAggiornamento");
        progress.SetThread(t);
        t.start();
        progress.setVisible(true);
        try {
            t.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (creato[0] == null) {
            Messaggi.WarningMessage("Aggiornamento", "Backup non riuscito",
                    "Senza il backup l'aggiornamento non viene installato. Il programma resta aperto.", proprietario);
            return false;
        }
        try {
            AggiornamentoInstallazione.ScriviInCorso(cartella, VarStatiche.Versione, proposto.Versione());
            AggiornamentoInstallazione.AvviaInstallazione(inst, pacchetto, cartella);
            System.out.println("Aggiornamento: installazione di " + proposto.Versione() + " avviata, backup "
                    + creato[0].getName());
            return true;
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Aggiornamento", "Installazione non avviata", ex.getMessage(), proprietario);
            return false;
        }
    }

    /**
     * All'avvio: se il precedente avvio aveva lanciato un aggiornamento, verifica che sia riuscito e pulisce i file
     * scaricati. Un aggiornamento non riuscito si segnala, con il percorso del registro dell'installazione.
     */
    public static void ControllaEsitoInstallazione(Window Proprietario) {
        AggiornamentoInstallazione.EsitoPrecedente e = AggiornamentoInstallazione.LeggiEPulisci(
                AggiornamentoInstallazione.Cartella(), VarStatiche.Versione);
        if (e == null) return;
        System.out.println("Aggiornamento da " + e.Da() + " a " + e.A() + ": " + (e.Riuscito() ? "riuscito" : "NON riuscito"));
        if (!e.Riuscito()) {
            Messaggi.WarningMessage("Aggiornamento", "L'aggiornamento alla versione " + e.A() + " non è riuscito",
                    "Il programma è ancora alla versione " + VarStatiche.Versione + ". Il backup fatto prima "
                    + "dell'aggiornamento è in ArchivioBackup. Il registro dell'installazione è in " + e.Log() + ".",
                    Proprietario);
        }
    }
}
