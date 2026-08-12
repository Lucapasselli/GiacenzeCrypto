package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Logica operativa dei pulsanti del pannello "Backup / Ripristino".
 *
 * <p>Segue lo schema delle altre estrazioni ({@code Principale_DocumentiFonte},
 * {@code Principale_GiacenzeaData}) : metodi {@code public static}, nessun campo Swing, nessun
 * riferimento a {@link Principale}, la {@code Window} per i dialoghi passata dal chiamante. Il motore
 * vero è {@link Backup_Restore}; qui ci sono le domande da fare all'utente, la finestra di avanzamento
 * e i messaggi di esito.
 *
 * <p>Le due finestre di scelta (opzioni di backup, gruppi da ripristinare) sono <b>scritte a mano</b> e
 * non hanno un {@code .form} : il loro contenuto è un elenco di caselle generato dai gruppi di
 * {@link Backup_Restore.Gruppo}, quindi dipende dai dati e non da un layout disegnato. È la stessa
 * scelta fatta per {@link GUI_ChiediIA}. {@code AppDialog} resta per tutto il resto, dove le caselle
 * non servono.
 */
public class Principale_Backup {

    /** Ultima cartella scelta per esportare o importare un archivio, ricordata in {@code personale.mv.db} */
    private static final String OPZIONE_CARTELLA = "Backup_CartellaEsterna";

    /** Ricorda se l'utente ha già visto l'avviso sulle chiavi API, per non ripeterlo a ogni backup. */
    private static final String OPZIONE_AVVISO_CHIAVI = "Backup_AvvisoChiaviApi";

    // =================================================================================================
    // ESECUZIONE DEL BACKUP
    // =================================================================================================
    /**
     * Chiede le opzioni ed esegue il backup, con barra di avanzamento.
     *
     * <p>La finestra {@link Download} resta aperta e modale per tutta la durata, con il lavoro su un
     * thread separato : è il pattern già stabilito dalla rimozione SCAM di massa, e serve qui per lo
     * stesso motivo — l'esportazione dei prezzi completi può durare minuti e l'interfaccia non deve
     * sembrare bloccata.
     *
     * @param owner finestra proprietaria dei dialoghi
     * @return il file creato, {@code null} se l'utente ha annullato o l'operazione è fallita
     */
    public static File EseguiBackup(Window owner) {
        Backup_Restore.OpzioniBackup opz = ChiediOpzioni(owner);
        if (opz == null) {
            return null;
        }

        final File[] creato = new File[1];
        Download progress = new Download();
        progress.setLocationRelativeTo(owner);
        Thread t = new Thread(() -> {
            progress.Titolo("Backup in corso");
            progress.SetLabel("Preparazione...");
            creato[0] = Backup_Restore.Esegui(opz, progress);
            progress.ChiudiFinestra();
        });
        progress.SetThread(t);
        t.start();
        progress.setVisible(true);

        if (creato[0] == null) {
            Messaggi.WarningMessage("Backup",
                    "Il backup non è stato completato.",
                    "L'operazione è stata interrotta oppure si è verificato un errore in scrittura. "
                    + "L'archivio parziale è stato eliminato : nell'elenco non compare nulla di incompleto.",
                    owner);
            return null;
        }
        Backup_Restore.Manifest m = Backup_Restore.LeggiManifest(creato[0]);
        Messaggi.SuccessMessage("Backup",
                "Backup completato : " + Backup_Restore.DimensioneLeggibile(creato[0].length()),
                (m == null ? "" : m.MovimentiTotali + " movimenti, " + m.Documenti + " documenti di origine, "
                        + m.RighePrezzi + " prezzi (" + m.LivelloPrezzi + ").<br>")
                + "Archivio : " + creato[0].getName(), owner);
        return creato[0];
    }

    /**
     * Le tre scelte del backup.
     * @return le opzioni, {@code null} se l'utente annulla
     */
    private static Backup_Restore.OpzioniBackup ChiediOpzioni(Window owner) {
        JCheckBox prezziCompleti = new JCheckBox("Salva l'intero database dei prezzi");
        JCheckBox chiaviApi = new JCheckBox("Salva anche le chiavi API degli exchange e degli explorer");
        JCheckBox cacheToken = new JCheckBox("Salva la cache dei token analizzati (GoPlus, Solana)", true);

        prezziCompleti.setToolTipText("Senza questa opzione vengono salvati solo i prezzi che RW e RT "
                + "possono leggere : confini d'anno e movimenti reward azzerati");
        chiaviApi.setToolTipText("Le chiavi sono in chiaro dentro l'archivio");
        cacheToken.setToolTipText("Evita di rinterrogare GoPlusLabs e Helius token per token");

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(Etichetta("<html><b>Contenuto dell'archivio</b><br><br>"
                + "Movimenti, dati Crypto.com, opzioni di calcolo, gruppi wallet, prezzi personalizzati,<br>"
                + "marcature SCAM, documenti di origine e configurazioni di import<br>"
                + "vengono salvati sempre.</html>"));
        p.add(Box.createVerticalStrut(12));
        p.add(prezziCompleti);
        p.add(Etichetta("<html><i>&nbsp;&nbsp;&nbsp;&nbsp;Senza questa opzione l'archivio contiene i soli "
                + "prezzi che RW e RT possono<br>&nbsp;&nbsp;&nbsp;&nbsp;leggere : su un archivio da "
                + "100.000 movimenti sono circa 20 MB e pochi secondi.<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Con l'opzione attiva diventano circa 115 MB e mezzo minuto, e "
                + "il ripristino<br>&nbsp;&nbsp;&nbsp;&nbsp;richiede qualche minuto e fa <b>crescere di "
                + "molto</b> il file dei prezzi sul disco.</i></html>"));
        p.add(Box.createVerticalStrut(8));
        p.add(chiaviApi);
        p.add(Etichetta("<html><i>&nbsp;&nbsp;&nbsp;&nbsp;<font color='#c05000'>Attenzione : le chiavi "
                + "finiscono in chiaro nell'archivio.</font><br>&nbsp;&nbsp;&nbsp;&nbsp;Non servono a "
                + "ristampare RW/RT, solo a riscaricare i movimenti.</i></html>"));
        p.add(Box.createVerticalStrut(8));
        p.add(cacheToken);
        p.add(Etichetta("<html><i>&nbsp;&nbsp;&nbsp;&nbsp;Analisi di sicurezza GoPlus e identita' dei mint "
                + "Solana, accumulate un token alla volta.<br>&nbsp;&nbsp;&nbsp;&nbsp;Senza, vanno "
                + "rinterrogate a una a una al prossimo import, a carico della tua API key.</i></html>"));

        int scelta = JOptionPane.showConfirmDialog(owner, p, "Esegui backup",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (scelta != JOptionPane.OK_OPTION) {
            return null;
        }

        if (chiaviApi.isSelected() && !"SI".equals(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_AVVISO_CHIAVI, "NO"))) {
            AppDialog.DialogResult r = AppDialog.builder(owner)
                    .windowTitle("Chiavi API nel backup")
                    .bodyTitle("Stai includendo le credenziali degli exchange")
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("")
                    .details("L'archivio conterrà in chiaro le chiavi API e i relativi segreti.<br><br>"
                            + "Chiunque ne entri in possesso — copiandolo su una chiavetta, caricandolo su "
                            + "un cloud, allegandolo a un messaggio — avrà accesso a quegli account.<br><br>"
                            + "Le chiavi non servono a ristampare RW/RT : servono soltanto a riscaricare i "
                            + "movimenti dagli exchange.")
                    .action(AppDialog.DialogAction.builder("cancel", "Escludile")
                            .role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("includi", "Includile comunque")
                            .role(AppDialog.ActionRole.DANGER).build())
                    .showDialog();
            if (r == null || !r.isAction("includi")) {
                chiaviApi.setSelected(false);
            } else {
                DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_AVVISO_CHIAVI, "SI");
            }
        }

        Backup_Restore.OpzioniBackup opz = new Backup_Restore.OpzioniBackup();
        opz.PrezziCompleti = prezziCompleti.isSelected();
        opz.ChiaviApi = chiaviApi.isSelected();
        opz.CacheToken = cacheToken.isSelected();
        return opz;
    }

    // =================================================================================================
    // RIPRISTINO
    // =================================================================================================
    /**
     * Verifica l'archivio, chiede conferma e cosa ripristinare, crea il backup di sicurezza ed esegue.
     *
     * <p>Non ricarica niente : al ritorno tocca al chiamante rileggere i movimenti, invalidare le cache
     * del motore incrementale e rifare i calcoli. La sequenza è documentata in
     * {@code test/Documentazione/Analisi_Backup_Ripristino.md} §7.4 e sta in {@code Principale} perché
     * la funzione che ricarica {@code MappaCryptoWallet} è privata di quella classe.
     *
     * @param Zip archivio selezionato nella tabella
     * @param owner finestra proprietaria dei dialoghi
     * @return l'esito, {@code null} se l'utente ha annullato o l'archivio è incompatibile
     */
    public static Backup_Restore.EsitoRipristino RipristinaBackup(File Zip, Window owner) {
        Backup_Restore.Manifest m = Backup_Restore.LeggiManifest(Zip);
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);
        if (!v.Ripristinabile()) {
            Messaggi.WarningMessage("Ripristino non possibile",
                    "Questo archivio non può essere ripristinato su questa versione.",
                    v.Descrizione(), owner);
            return null;
        }

        Set<Backup_Restore.Gruppo> gruppi = ChiediGruppi(m, v, owner);
        if (gruppi == null || gruppi.isEmpty()) {
            return null;
        }

        StringBuilder dettagli = new StringBuilder();
        dettagli.append("Stai per sostituire i dati di questa installazione con quelli del backup del <b>")
                .append(m.Creato).append("</b>.<br><br>");
        dettagli.append("Per ogni gruppo selezionato i dati attuali vengono <b>cancellati</b> e sostituiti "
                + "con quelli dell'archivio. L'operazione non è reversibile con un annulla.<br><br>");
        dettagli.append("Verranno ripristinati :<br>");
        for (Backup_Restore.Gruppo g : gruppi) {
            dettagli.append("&bull; ").append(g.Etichetta).append("<br>");
        }
        dettagli.append("<br>Prima di procedere viene creato automaticamente un <b>backup di sicurezza</b> "
                + "dello stato attuale, così è sempre possibile tornare indietro.");
        if (!v.Avvisi.isEmpty()) {
            dettagli.append("<br><br><b>Avvisi di compatibilità :</b><br>").append(v.Descrizione());
        }

        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Conferma ripristino")
                .bodyTitle("Ripristinare questo backup?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("")
                .details(dettagli.toString())
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("ripristina", "Ripristina")
                        .role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (r == null || !r.isAction("ripristina")) {
            return null;
        }

        final Backup_Restore.EsitoRipristino[] esito = new Backup_Restore.EsitoRipristino[1];
        Download progress = new Download();
        progress.setLocationRelativeTo(owner);
        progress.NascondiInterrompi();
        Thread t = new Thread(() -> {
            //Il backup di sicurezza è la rete sotto tutto il resto : l'utente ha appena autorizzato la
            //cancellazione del proprio archivio vivo, e deve poter tornare indietro anche se è il backup
            //ripristinato a rivelarsi sbagliato
            progress.Titolo("Ripristino in corso");
            progress.SetLabel("Backup di sicurezza dello stato attuale...");
            Backup_Restore.OpzioniBackup sicurezza = new Backup_Restore.OpzioniBackup();
            sicurezza.Automatico = true;
            sicurezza.ChiaviApi = true;
            //Anche le cache token, per lo stesso motivo delle chiavi : il ripristino le sostituisce
            //(ImportaTabella con svuotamento), e ricostruirle costa una interrogazione per token a
            //GoPlusLabs e a Helius. Quando il gruppo conteneva i registri GESTITI* qui stava false,
            //perche' quelli si riscaricano in blocco e gratis
            sicurezza.CacheToken = true;
            Backup_Restore.Esegui(sicurezza, null);

            progress.SetLabel("Ripristino dei dati...");
            esito[0] = Backup_Restore.Ripristina(Zip, gruppi, progress);
            progress.ChiudiFinestra();
        });
        t.start();
        progress.setVisible(true);
        return esito[0];
    }

    /**
     * Quali gruppi ripristinare.
     * <p>I gruppi che l'archivio non contiene sono mostrati disabilitati anziché nascosti : sapere che
     * un backup <i>non</i> ha le chiavi API o i prezzi è un'informazione, non un dettaglio da omettere.
     *
     * @return i gruppi scelti, {@code null} se l'utente annulla
     */
    private static Set<Backup_Restore.Gruppo> ChiediGruppi(Backup_Restore.Manifest m,
            Backup_Compatibilita.Verdetto v, Window owner) {
        Set<Backup_Restore.Gruppo> presenti = m.GruppiPresenti();
        Map<Backup_Restore.Gruppo, JCheckBox> caselle = new LinkedHashMap<>();

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(Etichetta("<html><b>Cosa ripristinare dal backup del " + m.Creato + "</b><br><br>"
                + "Ogni gruppo selezionato sostituisce i dati attuali corrispondenti.</html>"));
        p.add(Box.createVerticalStrut(12));

        for (Backup_Restore.Gruppo g : Backup_Restore.Gruppo.values()) {
            boolean c = presenti.contains(g);
            //Le chiavi API restano deselezionate anche quando ci sono : sovrascrivere le credenziali in
            //uso con quelle di un backup vecchio è quasi sempre il contrario di quello che si vuole
            JCheckBox cb = new JCheckBox(g.Etichetta, c && g != Backup_Restore.Gruppo.CHIAVI_API);
            cb.setEnabled(c);
            if (!c) {
                cb.setToolTipText("Questo archivio non contiene dati di questo tipo");
            }
            caselle.put(g, cb);
            p.add(cb);
        }
        if (!v.Avvisi.isEmpty()) {
            p.add(Box.createVerticalStrut(12));
            p.add(Etichetta("<html><i>Il ripristino è possibile ma con avvisi : verranno mostrati nella "
                    + "conferma successiva.</i></html>"));
        }

        int scelta = JOptionPane.showConfirmDialog(owner, p, "Ripristina backup",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (scelta != JOptionPane.OK_OPTION) {
            return null;
        }
        Set<Backup_Restore.Gruppo> scelti = EnumSet.noneOf(Backup_Restore.Gruppo.class);
        for (Map.Entry<Backup_Restore.Gruppo, JCheckBox> e : caselle.entrySet()) {
            if (e.getValue().isEnabled() && e.getValue().isSelected()) {
                scelti.add(e.getKey());
            }
        }
        return scelti;
    }

    /**
     * Confronta le plusvalenze ricalcolate con quelle registrate nel backup, e lo dice.
     *
     * <p>È l'unico controllo che distingue "il ripristino è andato a buon fine" da "la ristampa sarà
     * identica". Una divergenza significa che qualche prezzo è stato risolto diversamente : su cache
     * mancante {@code Prezzi.DammiPrezzoTransazione} scarica dalla rete, e un prezzo assente verrebbe
     * recuperato da un altro exchange o da un'altra candela senza che nulla segnali niente.
     *
     * <p><b>Non si esegue su un ripristino parziale.</b> L'impronta descrive il risultato del motore su
     * <i>tutti</i> gli ingressi che c'erano al momento del backup : movimenti, opzioni di calcolo,
     * gruppi wallet e prezzi personalizzati. Se l'utente ne lascia fuori qualcuno, il motore gira sui
     * dati di questa installazione e il confronto fallirebbe <b>sempre e comunque</b>, senza che nulla
     * sia andato storto — un controllo che non può passare non è un controllo. Quali gruppi siano
     * indispensabili, e perché la cache dei prezzi di mercato invece non lo sia, è deciso e motivato in
     * {@code Backup_Restore.GruppiMancantiPerVerifica}.
     *
     * @param esito l'esito del ripristino appena eseguito
     * @param owner finestra proprietaria del messaggio
     * @return {@code true} se le due impronte coincidono, o se non c'era nulla da confrontare
     */
    public static boolean VerificaDopoRipristino(Backup_Restore.EsitoRipristino esito, Window owner) {
        String ImprontaAttesa = esito == null ? "" : esito.ImprontaAttesa;
        if (ImprontaAttesa == null || ImprontaAttesa.isBlank()) {
            //Backup prodotto prima dell'introduzione della verifica : non è un fallimento
            return true;
        }
        if (!esito.VerificaMancanti.isEmpty()) {
            StringBuilder elenco = new StringBuilder();
            for (Backup_Restore.Gruppo g : esito.VerificaMancanti) {
                elenco.append("&bull; ").append(g.Etichetta).append("<br>");
            }
            Messaggi.InfoMessage("Verifica del ripristino",
                    "Ripristino parziale : i calcoli non sono confrontabili.",
                    "I dati selezionati sono stati ripristinati correttamente, ma il confronto con le "
                    + "plusvalenze registrate nel backup non è stato eseguito : non hai ripristinato "
                    + "questi dati, che entrano nel calcolo.<br><br>"
                    + elenco
                    + "<br>Il motore ha quindi lavorato sui dati di questa installazione, e un confronto "
                    + "con l'impronta del backup segnalerebbe una differenza in ogni caso, anche se tutto "
                    + "è a posto.<br><br>"
                    + "Per avere la verifica, ripeti il ripristino selezionando anche i gruppi qui "
                    + "sopra.", owner);
            return true;
        }
        String adesso = Backup_Restore.ImprontaPlusvalenze();
        if (ImprontaAttesa.equals(adesso)) {
            Messaggi.SuccessMessage("Verifica del ripristino",
                    "I calcoli coincidono con quelli del backup.",
                    "Le plusvalenze ricalcolate su questa installazione sono identiche, movimento per "
                    + "movimento, a quelle registrate al momento del backup : la ristampa di RW e RT "
                    + "darà gli stessi risultati.", owner);
            return true;
        }
        Messaggi.WarningMessage("Verifica del ripristino",
                "I calcoli non coincidono con quelli del backup.",
                "Il ripristino è avvenuto, ma le plusvalenze ricalcolate differiscono da quelle registrate "
                + "al momento del backup.<br><br>"
                + "La causa più probabile è un <b>prezzo risolto diversamente</b> : se un prezzo non è "
                + "nella cache locale viene scaricato dalla rete, e può arrivare da un altro exchange o "
                + "da un'altra rilevazione.<br><br>"
                //Il consiglio giusto dipende da cosa è stato ripristinato, e da cosa c'era da
                //ripristinare : dire "rifai il backup includendo i prezzi" a chi ha semplicemente
                //lasciato la casella deselezionata manda a rifare il lavoro dal lato sbagliato, e dire
                //"riseleziona i prezzi" quando l'archivio non ne ha manda su una casella disabilitata
                + (!esito.PrezziNellArchivio
                        ? "Questo archivio non contiene prezzi di mercato, quindi i prezzi sono stati "
                        + "presi da questa installazione o riscaricati : è la spiegazione più probabile. "
                        + "Per una ristampa identica rifai il backup dall'installazione di origine "
                        + "includendo i prezzi. "
                        : esito.PrezziRipristinati
                        ? "Se il backup era stato fatto con i soli prezzi essenziali, rifallo "
                        + "dall'installazione di origine includendo l'intero database dei prezzi. "
                        : "Non hai ripristinato la cache dei prezzi di mercato : i prezzi mancanti in "
                        + "locale sono stati riscaricati, ed è la spiegazione più probabile. Ripeti il "
                        + "ripristino selezionando anche i prezzi di mercato. ")
                + "Verifica comunque i quadri RW e RT prima di usarli.", owner);
        return false;
    }

    // =================================================================================================
    // ESPORTA / IMPORTA / ELIMINA
    // =================================================================================================
    /**
     * Copia un archivio in una cartella a scelta.
     * @return {@code true} se la copia è riuscita
     */
    public static boolean EsportaBackup(File Zip, Window owner) {
        if (Zip == null) {
            return false;
        }
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA, ""));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Cartella in cui esportare il backup");
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File cartella = fc.getSelectedFile();
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA, cartella.getAbsolutePath());
        File scritto = Backup_Restore.Esporta(Zip, cartella);
        if (scritto == null) {
            Messaggi.WarningMessage("Esportazione backup",
                    "Non è stato possibile copiare l'archivio.",
                    "Verifica che ci sia spazio sufficiente e che la cartella sia scrivibile.", owner);
            return false;
        }
        Messaggi.SuccessMessage("Esportazione backup",
                "Archivio esportato : " + Backup_Restore.DimensioneLeggibile(scritto.length()),
                "Destinazione : " + scritto.getAbsolutePath(), owner);
        return true;
    }

    /**
     * Sceglie un archivio esterno, lo verifica e lo copia nell'elenco.
     * @return il file importato, {@code null} se annullato o non valido
     */
    public static File ImportaBackup(Window owner) {
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA, ""));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Archivio di backup da importare");
        fc.setFileFilter(new FileNameExtensionFilter("Archivi di backup (*.zip)", "zip"));
        if (fc.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File scelto = fc.getSelectedFile();
        if (scelto.getParentFile() != null) {
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA, scelto.getParentFile().getAbsolutePath());
        }

        Backup_Restore.Manifest m = Backup_Restore.LeggiManifest(scelto);
        if (m == null) {
            Messaggi.WarningMessage("Importazione backup",
                    "Il file selezionato non è un archivio di backup valido.",
                    "Non contiene il manifest che descrive il contenuto : potrebbe essere danneggiato, "
                    + "oppure essere uno zip qualsiasi.", owner);
            return null;
        }
        Backup_Compatibilita.Verdetto v = Backup_Compatibilita.Verifica(m);

        File importato = Backup_Restore.Importa(scelto);
        if (importato == null) {
            Messaggi.WarningMessage("Importazione backup",
                    "Non è stato possibile importare l'archivio.",
                    "Verifica che ci sia spazio sufficiente nella cartella di lavoro.", owner);
            return null;
        }

        StringBuilder d = new StringBuilder();
        d.append("Backup del <b>").append(m.Creato).append("</b>, versione ").append(m.Versione)
                .append("<br>").append(m.MovimentiTotali).append(" movimenti");
        if (!m.PrimoMovimento.isEmpty()) {
            d.append(", dal ").append(m.PrimoMovimento).append(" al ").append(m.UltimoMovimento);
        }
        d.append("<br><br>");
        if (!v.Ripristinabile()) {
            d.append("<b>Non è però ripristinabile su questa versione :</b><br>").append(v.Descrizione());
            d.append("<br>L'archivio resta comunque nell'elenco.");
            Messaggi.WarningMessage("Importazione backup", "Archivio importato, ma non ripristinabile",
                    d.toString(), owner);
            //Ritorna null di proposito: il valore di ritorno significa "prosegui col ripristino", e
            //proseguire qui farebbe apparire subito dopo un secondo messaggio che rifiuta l'operazione
            return null;
        }
        d.append("Vuoi ripristinarlo adesso sul gestionale?");

        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Importazione backup")
                .bodyTitle("Archivio importato")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("")
                .details(d.toString())
                .action(AppDialog.DialogAction.builder("no", "Solo importa")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("si", "Ripristina adesso")
                        .role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        //Chi vuole ripristinare subito lo fa dal pulsante, così passa dalla stessa conferma di sempre :
        //il valore di ritorno dice al pannello se selezionare la riga e proseguire
        return (r != null && r.isAction("si")) ? importato : null;
    }

    /**
     * Elimina gli archivi selezionati, previa conferma.
     * <p>La ritenzione è eterna e non c'è potatura automatica — {@code ArchivioBackup/} è volutamente
     * fuori dalla cartella {@code Backup/}, che invece viene potata a 180 giorni all'avvio — quindi
     * questo è l'unico modo per liberare spazio.
     *
     * @return {@code true} se qualcosa è stato eliminato
     */
    public static boolean EliminaBackup(List<File> Archivi, Window owner) {
        if (Archivi == null || Archivi.isEmpty()) {
            return false;
        }
        long byteTotali = 0;
        for (File f : Archivi) {
            byteTotali += f.length();
        }
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Conferma eliminazione")
                .bodyTitle("Eliminare " + (Archivi.size() == 1 ? "questo backup" : "questi backup") + "?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("")
                .details("Stai per eliminare " + Archivi.size()
                        + (Archivi.size() == 1 ? " archivio" : " archivi")
                        + " per un totale di " + Backup_Restore.DimensioneLeggibile(byteTotali)
                        + ".<br><br>L'operazione non è reversibile : gli archivi non sono ricostruibili.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina")
                        .role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (r == null || !r.isAction("elimina")) {
            return false;
        }
        int eliminati = 0;
        for (File f : Archivi) {
            if (Backup_Restore.Elimina(f)) {
                eliminati++;
            }
        }
        return eliminati > 0;
    }

    // =================================================================================================
    // RIGHE PER LE DUE TABELLE DEL PANNELLO
    // =================================================================================================
    /**
     * Righe della tabella superiore : un archivio per riga, con le informazioni che servono a
     * riconoscerlo al volo.
     *
     * @param Manifesti l'elenco restituito da {@link Backup_Restore#Elenco()}
     * @return una riga per archivio, con le colonne data, movimenti, primo e ultimo movimento, prezzi,
     *         documenti, dimensione, versione, origine
     */
    public static List<Object[]> RigheElenco(List<Backup_Restore.Manifest> Manifesti) {
        List<Object[]> righe = new ArrayList<>();
        for (Backup_Restore.Manifest m : Manifesti) {
            righe.add(new Object[]{
                m.Creato,
                m.MovimentiTotali,
                m.PrimoMovimento,
                m.UltimoMovimento,
                DescrizionePrezzi(m),
                m.Documenti,
                Backup_Restore.DimensioneLeggibile(m.DimensioneCompressa),
                m.Versione,
                m.Automatico ? "automatico" : (m.Importato ? "importato" : "manuale")
            });
        }
        return righe;
    }

    private static String DescrizionePrezzi(Backup_Restore.Manifest m) {
        if ("completi".equals(m.LivelloPrezzi)) {
            return "completi (" + m.RighePrezzi + ")";
        }
        if ("necessari".equals(m.LivelloPrezzi)) {
            return "essenziali (" + m.RighePrezzi + ")";
        }
        return "assenti";
    }

    /**
     * Righe della tabella inferiore : il dettaglio dell'archivio selezionato, come coppie voce/valore.
     * <p>È volutamente piatta anziché ad albero : la si legge scorrendo, e le voci sono poche decine.
     *
     * @param m il manifest della riga selezionata, può essere {@code null}
     * @return le righe {@code {voce, valore}}
     */
    public static List<Object[]> RigheDettaglio(Backup_Restore.Manifest m) {
        List<Object[]> righe = new ArrayList<>();
        if (m == null) {
            return righe;
        }
        righe.add(new Object[]{"Archivio", m.Archivio == null ? "" : m.Archivio.getName()});
        righe.add(new Object[]{"Creato il", m.Creato});
        righe.add(new Object[]{"Versione del programma", m.Versione});
        righe.add(new Object[]{"Formato archivio", String.valueOf(m.FormatoBackup)});
        righe.add(new Object[]{"Campi per movimento", String.valueOf(m.ColonneTabella)});
        righe.add(new Object[]{"Dimensione compressa", Backup_Restore.DimensioneLeggibile(m.DimensioneCompressa)});
        righe.add(new Object[]{"Dati originali", Backup_Restore.DimensioneLeggibile(m.DimensioneOriginale)});
        righe.add(new Object[]{"", ""});

        righe.add(new Object[]{"Movimenti totali", String.valueOf(m.MovimentiTotali)});
        righe.add(new Object[]{"Primo movimento", m.PrimoMovimento});
        righe.add(new Object[]{"Ultimo movimento", m.UltimoMovimento});
        for (Map.Entry<String, Integer> e : m.MovimentiPerAnno.entrySet()) {
            righe.add(new Object[]{"   movimenti " + e.getKey(), String.valueOf(e.getValue())});
        }
        for (Map.Entry<String, Integer> e : m.MovimentiPerWallet.entrySet()) {
            righe.add(new Object[]{"   wallet : " + e.getKey(), String.valueOf(e.getValue())});
        }
        righe.add(new Object[]{"", ""});

        righe.add(new Object[]{"Documenti di origine", m.Documenti + " ("
            + Backup_Restore.DimensioneLeggibile(m.DocumentiByte) + ")"});
        righe.add(new Object[]{"File di configurazione", String.valueOf(m.FileSupporto)});
        righe.add(new Object[]{"Prezzi", DescrizionePrezzi(m)});
        righe.add(new Object[]{"Chiavi API incluse", m.ChiaviApi ? "sì" : "no"});
        righe.add(new Object[]{"Cache token analizzati", m.CacheToken ? "sì" : "no"});
        righe.add(new Object[]{"Impronta plusvalenze",
            m.ImprontaPlusvalenze.isEmpty() ? "(assente)" : m.ImprontaPlusvalenze.substring(0,
                    Math.min(16, m.ImprontaPlusvalenze.length())) + "..."});
        righe.add(new Object[]{"Epoca di ricalcolo", String.valueOf(m.EpocaRicalcolo)});
        righe.add(new Object[]{"", ""});

        righe.add(new Object[]{"IMPOSTAZIONI SALVATE", String.valueOf(m.Impostazioni.size())});
        for (Map.Entry<String, String> e : m.Impostazioni.entrySet()) {
            righe.add(new Object[]{"   " + e.getKey(), e.getValue()});
        }
        righe.add(new Object[]{"", ""});

        righe.add(new Object[]{"TABELLE SALVATE", String.valueOf(m.Tabelle.size())});
        for (Map.Entry<String, Backup_Restore.TabellaSalvata> e : m.Tabelle.entrySet()) {
            righe.add(new Object[]{"   " + e.getKey(), e.getValue().Righe + " righe, "
                + e.getValue().Colonne.size() + " colonne"});
        }
        return righe;
    }

    /** Etichetta allineata a sinistra dentro un {@code BoxLayout} verticale. */
    private static JLabel Etichetta(String Html) {
        JLabel l = new JLabel(Html);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Non istanziabile. */
    private Principale_Backup() {
    }
}
