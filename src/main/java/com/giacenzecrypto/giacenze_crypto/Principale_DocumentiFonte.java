package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

/**
 * Logica operativa della voce di menu contestuale "Apri documento di origine": dal movimento selezionato al
 * file da cui è stato importato.
 *
 * <p>Segue lo schema delle altre estrazioni ({@code Principale_GiacenzeaData},
 * {@code Principale_Movimenti_SeparaUnisci}): metodi {@code public static}, nessun campo Swing, nessun
 * riferimento a {@link Principale}, la {@code Window} per i dialoghi passata dal chiamante e lo stato
 * condiviso raggiunto tramite {@link Principale#MappaCryptoWallet}.
 */
public class Principale_DocumentiFonte {

    /** Ultima cartella scelta per l'esportazione, ricordata in {@code personale.mv.db} */
    private static final String OPZIONE_CARTELLA_EXPORT = "DocumentiFonte_CartellaExport";

    /**
     * Indice, nelle righe prodotte da {@link #RigheDiRiepilogo()}, della colonna con i movimenti agganciati.
     * <p>Sta qui e non nella finestra perché è la colonna che decide se un documento sia eliminabile:
     * chi la legge (il pannello) e chi la scrive (questa classe) devono usare la stessa costante.
     */
    public static final int COLONNA_MOVIMENTI = 7;

    /**
     * Apre il documento da cui il movimento è stato importato.
     *
     * <p>La voce di menu è già disabilitata quando non c'è nulla da aprire ({@link Funzioni#PopUpMenu}), ma i
     * controlli sono ripetuti qui: fra l'apertura del menu e il clic il file può essere sparito, e in ogni caso
     * l'utente merita di sapere <i>perché</i> non si apre invece di veder fallire un comando in silenzio.
     *
     * @param IDMovimento identificativo del movimento selezionato
     * @param owner finestra rispetto a cui centrare gli eventuali messaggi
     */
    public static void ApriDocumentoDiOrigine(String IDMovimento, Window owner) {
        if (Funzioni.noData(IDMovimento)) {
            return;
        }
        String v[] = MappaCryptoWallet.get(IDMovimento);
        if (v == null || v.length <= 41) {
            return;
        }
        int Id = DocumentiFonte.IdDaCampo41(v[41]);
        if (Id <= 0) {
            Messaggi.InfoMessage("Documento di origine",
                    "Questo movimento non ha un documento di origine.",
                    "I movimenti inseriti a mano, quelli generati dall'applicazione e quelli importati prima "
                    + "dell'introduzione dei documenti di origine non hanno un file a cui risalire.", owner);
            return;
        }
        DocumentiFonte.Documento d = DocumentiFonte.Leggi(Id);
        if (d == null) {
            Messaggi.WarningMessage("Documento di origine",
                    "Il documento " + Id + " non è più disponibile.",
                    "Il movimento fa riferimento a un documento che non risulta nel registro di questa "
                    + "installazione: può succedere ripristinando i movimenti su un'altra installazione.", owner);
            return;
        }
        if (DocumentiFonte.FileConservato(Id) == null) {
            Messaggi.WarningMessage("Documento di origine",
                    "Il file di " + d.NomeOriginale + " non è più presente.",
                    "Il documento " + Id + " risulta importato il "
                    + FunzioniDate.ConvertiDatadaLong(d.DataImport)
                    + " ma la sua copia non si trova più nella cartella dei documenti di origine.", owner);
            return;
        }
        if (!DocumentiFonte.Apri(Id)) {
            Messaggi.WarningMessage("Documento di origine",
                    "Non è stato possibile aprire " + d.NomeOriginale + ".",
                    "Il documento è conservato ma il sistema non ha un'applicazione associata a questo tipo di "
                    + "file, oppure l'apertura è stata rifiutata.", owner);
        }
    }

    /**
     * Riga da mostrare nel dettaglio di un movimento.
     * @param IDMovimento identificativo del movimento
     * @return la descrizione del documento di origine, stringa vuota se il movimento non ne ha uno
     */
    public static String DescrizioneDocumento(String IDMovimento) {
        if (Funzioni.noData(IDMovimento)) {
            return "";
        }
        String v[] = MappaCryptoWallet.get(IDMovimento);
        if (v == null || v.length <= 41) {
            return "";
        }
        return DocumentiFonte.Descrizione(v[41]);
    }

    // =================================================================================================
    // PANNELLO DI GESTIONE
    // =================================================================================================
    /**
     * Costruisce le righe della tabella del pannello di gestione, unendo il registro dei documenti al
     * riepilogo dei movimenti che vi puntano <b>adesso</b>.
     *
     * <p>Il conteggio mostrato non è quello del registro: quello dice quanti movimenti il documento ha
     * prodotto quando fu importato, e non cambia se poi l'utente ne cancella o ne separa qualcuno. Per
     * decidere se un documento sia eliminabile serve il conteggio attuale, quindi è quello che si mostra.
     *
     * <p>Le dimensioni mostrate sono due: quella del file <b>originale</b> e quella che occupa <b>sul
     * disco</b> compresso. Mostrare solo la seconda sarebbe fuorviante — un CSV da qualche centinaio di KB
     * ne occupa poche decine — e solo la prima nasconderebbe quanto costa davvero conservarli.
     *
     * @return una riga per documento, nell'ordine del registro (dal più recente), con le colonne
     *         id, nome, tipo, origine, data di importazione, dimensione, occupazione su disco,
     *         movimenti, wallet, periodo
     */
    public static List<Object[]> RigheDiRiepilogo() {
        Map<Integer, DocumentiFonte.Riepilogo> riepiloghi = DocumentiFonte.Riepiloghi();
        List<Object[]> righe = new ArrayList<>();
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            DocumentiFonte.Riepilogo r = riepiloghi.get(d.Id);
            if (r == null) {
                r = new DocumentiFonte.Riepilogo();
            }
            righe.add(new Object[]{
                d.Id,
                d.NomeOriginale,
                d.Tipo,
                d.Origine,
                d.DataImport > 0 ? FunzioniDate.ConvertiDatadaLongAlSecondo(d.DataImport) : "",
                DimensioneLeggibile(DocumentiFonte.DimensioneOriginale(d.Id)),
                DimensioneLeggibile(DocumentiFonte.Dimensione(d.Id)),
                r.Movimenti,
                r.WalletInRiga(),
                r.Periodo()
            });
        }
        return righe;
    }

    /**
     * @param Righe le righe ritornate da {@link #RigheDiRiepilogo()}
     * @return la riga di riepilogo da mostrare in fondo al pannello
     */
    public static String TestoRiepilogo(List<Object[]> Righe) {
        long dimensione = 0;
        int senzaMovimenti = 0;
        for (Object[] r : Righe) {
            dimensione += DocumentiFonte.Dimensione((Integer) r[0]);
            if ((Integer) r[COLONNA_MOVIMENTI] == 0) {
                senzaMovimenti++;
            }
        }
        String s = Righe.size() + " documenti conservati, " + DimensioneLeggibile(dimensione)
                + " occupati sul disco";
        if (senzaMovimenti > 0) {
            s = s + " - " + senzaMovimenti + " senza movimenti agganciati";
        }
        return s;
    }

    /**
     * Apre una copia del documento con l'applicazione predefinita del sistema.
     *
     * <p>È sempre una <b>copia</b>: il documento è conservato compresso, quindi viene scompattato in
     * {@code Temporanei/} e si apre quello. L'archivio non è così modificabile per sbaglio da chi lo apre,
     * ed è il motivo per cui la compressione non è solo un risparmio di spazio.
     *
     * @param Id documento da aprire
     * @param owner finestra rispetto a cui centrare gli eventuali messaggi
     */
    public static void ApriDocumento(int Id, Window owner) {
        if (Id <= 0) {
            return;
        }
        DocumentiFonte.Documento d = DocumentiFonte.Leggi(Id);
        if (d == null || DocumentiFonte.FileConservato(Id) == null) {
            Messaggi.WarningMessage("Documento di origine",
                    "Il documento " + Id + " non è più disponibile.",
                    "La copia conservata non si trova più nella cartella dei documenti di origine.", owner);
            return;
        }
        if (!DocumentiFonte.Apri(Id)) {
            Messaggi.WarningMessage("Documento di origine",
                    "Non è stato possibile aprire " + d.NomeOriginale + ".",
                    "Il documento è conservato ma il sistema non ha un'applicazione associata a questo tipo "
                    + "di file, oppure l'apertura è stata rifiutata.", owner);
        }
    }

    /**
     * Chiede una cartella e vi scrive i documenti selezionati, decompressi.
     * @param Ids documenti da esportare
     * @param owner finestra proprietaria del selettore di cartella e dei messaggi
     * @return {@code true} se almeno un documento è stato scritto
     */
    public static boolean EsportaDocumenti(List<Integer> Ids, Window owner) {
        if (Ids == null || Ids.isEmpty()) {
            return false;
        }
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA_EXPORT, ""));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Cartella in cui esportare i documenti selezionati");
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File cartella = fc.getSelectedFile();
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA_EXPORT, cartella.getAbsolutePath());

        int scritti = 0;
        List<String> falliti = new ArrayList<>();
        for (int Id : Ids) {
            if (DocumentiFonte.EsportaDecompresso(Id, cartella) != null) {
                scritti++;
            } else {
                DocumentiFonte.Documento d = DocumentiFonte.Leggi(Id);
                falliti.add(d == null ? "documento " + Id : d.NomeOriginale);
            }
        }

        if (falliti.isEmpty()) {
            Messaggi.SuccessMessage("Esportazione documenti",
                    scritti + (scritti == 1 ? " documento esportato" : " documenti esportati"),
                    "Cartella di destinazione : " + cartella.getAbsolutePath(), owner);
        } else {
            Messaggi.WarningMessage("Esportazione documenti",
                    scritti + " esportati, " + falliti.size() + " non riusciti",
                    "Non è stato possibile esportare : " + String.join(", ", falliti)
                    + "<br>La copia conservata di questi documenti non è più presente.", owner);
        }
        return scritti > 0;
    }

    /**
     * Esito di {@link #EliminaDocumenti(List, Window)} : quanti documenti e quanti movimenti sono stati
     * eliminati davvero.
     *
     * <p>Un {@code boolean} non basta più da quando l'eliminazione può portarsi via anche i movimenti :
     * il pannello deve poter distinguere "è cambiato il registro" (basta rileggere la tabella) da "è
     * cambiata la mappa dei movimenti" (serve il ricalcolo di tutto il resto dell'applicazione).
     *
     * @param Documenti documenti eliminati
     * @param Movimenti movimenti eliminati insieme a loro
     */
    public record EsitoEliminazione(int Documenti, int Movimenti) {

        /** Esito di un'operazione annullata o rifiutata */
        public static final EsitoEliminazione NIENTE = new EsitoEliminazione(0, 0);

        /** @return {@code true} se il registro dei documenti è cambiato */
        public boolean Modificato() {
            return Documenti > 0;
        }
    }

    /**
     * Elimina i documenti selezionati, copia e riga di registro.
     *
     * <p>Due percorsi, e la differenza sta tutta in quanti documenti sono selezionati :
     * <ul>
     *   <li><b>documento singolo</b> : si elimina anche se ha ancora dei movimenti agganciati, ma allora
     *       quei movimenti vengono eliminati <b>insieme</b> a lui, dietro due conferme distinte. Lasciarli
     *       orfani sarebbe peggio che non poter eliminare niente : resterebbero a puntare a un file che
     *       non esiste più, e la loro provenienza sarebbe persa senza che nulla lo dica;</li>
     *   <li><b>selezione multipla</b> : si elimina solo ciò a cui non punta più nessun movimento. La
     *       cascata è deliberatamente riservata al documento singolo — un avviso che elenca cinque
     *       documenti e qualche migliaio di movimenti non lo legge nessuno, e qui sbagliare non è
     *       rimediabile.</li>
     * </ul>
     *
     * @param Ids documenti da eliminare
     * @param owner finestra rispetto a cui centrare le conferme
     * @return quanto è stato eliminato davvero
     */
    public static EsitoEliminazione EliminaDocumenti(List<Integer> Ids, Window owner) {
        if (Ids == null || Ids.isEmpty()) {
            return EsitoEliminazione.NIENTE;
        }
        List<String> conMovimenti = NonEliminabili(Ids);

        if (!conMovimenti.isEmpty() && Ids.size() > 1) {
            Messaggi.WarningMessage("Eliminazione documenti",
                    "Ci sono ancora movimenti che fanno riferimento a questi documenti",
                    "Non eliminabili : " + String.join(", ", conMovimenti)
                    + "<br><br>Su una selezione multipla si possono eliminare solo i documenti a cui non "
                    + "punta più nessun movimento.<br>Per eliminare un documento insieme ai suoi movimenti "
                    + "selezionalo da solo.", owner);
            return EsitoEliminazione.NIENTE;
        }

        if (!conMovimenti.isEmpty()) {
            return EliminaDocumentoConMovimenti(Ids.get(0), owner);
        }

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Conferma eliminazione")
                .bodyTitle("Eliminazione dei documenti di origine")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("")
                .details("Stai per eliminare " + Ids.size()
                        + (Ids.size() == 1 ? " documento" : " documenti")
                        + ", copia del file e riga di registro.<br><br>"
                        + "Nessun movimento vi fa più riferimento, ma l'operazione non è reversibile : "
                        + "il file originale non è più recuperabile dall'applicazione.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("elimina")) {
            return EsitoEliminazione.NIENTE;
        }
        for (int Id : Ids) {
            DocumentiFonte.Annulla(Id);
        }
        return new EsitoEliminazione(Ids.size(), 0);
    }

    /**
     * Elimina un documento <b>e</b> i movimenti che ancora vi puntano, dietro due conferme distinte.
     *
     * <p>Le due conferme non sono un vezzo : il pulsante è lo stesso con cui si eliminano i documenti
     * inerti, la selezione della tabella si sposta con una freccia, e qui si cancella una parte
     * dell'archivio fiscale. La prima conferma dice <i>che cosa</i> sparisce, la seconda <i>che non
     * torna indietro</i>.
     *
     * <p>L'ordine delle tre operazioni conta, e non è intercambiabile :
     * <ol>
     *   <li>si tolgono i movimenti dalla mappa;</li>
     *   <li>si <b>salva subito</b> con backup permanente. La cancellazione dei movimenti, nel resto
     *       dell'applicazione, è solo in memoria finché l'utente non preme <i>Salva</i> — ma qui
     *       l'eliminazione del documento è immediata e definitiva, quindi un <i>Annulla</i> nella
     *       tabella dei movimenti li rileggerebbe dal file facendoli puntare a un documento che non
     *       esiste più. {@code Scrivi_Movimenti_Crypto(.., true)} rinomina il file <i>attuale</i> —
     *       quello che i movimenti ce li ha ancora — in {@code Backup/movimenti.crypto.backup.&lt;ts&gt;} :
     *       quel backup è l'unica via di recupero, ed è il motivo per cui si scrive <b>prima</b> di
     *       eliminare il documento;</li>
     *   <li>si elimina il documento.</li>
     * </ol>
     *
     * @param Id documento da eliminare
     * @param owner finestra rispetto a cui centrare le conferme
     * @return quanto è stato eliminato
     */
    private static EsitoEliminazione EliminaDocumentoConMovimenti(int Id, Window owner) {
        DocumentiFonte.Documento d = DocumentiFonte.Leggi(Id);
        String Nome = d == null ? "documento " + Id : d.NomeOriginale;
        DocumentiFonte.Riepilogo r = DocumentiFonte.Riepiloghi().get(Id);
        if (r == null || r.Movimenti == 0) {
            //La selezione era stata fatta su una tabella caricata prima : non c'è più niente da
            //travolgere, quindi si ricade sul percorso ordinario invece di spaventare per niente
            return EliminaDocumenti(List.of(Id), owner);
        }

        AppDialog.DialogResult primo = AppDialog.builder(owner)
                .windowTitle("Conferma eliminazione")
                .bodyTitle("Eliminare anche i movimenti importati?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("A " + Nome + " sono ancora agganciati " + r.Movimenti
                        + (r.Movimenti == 1 ? " movimento." : " movimenti."))
                .details("Wallet / exchange : " + (r.WalletInRiga().isEmpty() ? "-" : r.WalletInRiga())
                        + "<br>Periodo dei movimenti : " + (r.Periodo().isEmpty() ? "-" : r.Periodo())
                        + "<br><br>Eliminando il documento verranno eliminati <b>anche quei movimenti</b>, "
                        + "insieme agli eventuali movimenti generati automaticamente a partire da loro "
                        + "(commissioni, reward, classificazioni). I movimenti di altri documenti collegati "
                        + "a questi verranno riportati alla situazione precedente alla classificazione.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("avanti", "Continua")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (primo == null || !primo.isAction("avanti")) {
            return EsitoEliminazione.NIENTE;
        }

        AppDialog.DialogResult secondo = AppDialog.builder(owner)
                .windowTitle("Conferma definitiva")
                .bodyTitle("Sei sicuro? L'operazione non è reversibile")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Stai per eliminare " + r.Movimenti
                        + (r.Movimenti == 1 ? " movimento" : " movimenti")
                        + " dall'archivio, insieme a " + Nome + ".")
                .details("I movimenti eliminati non si possono recuperare dall'applicazione, e nemmeno il "
                        + "file da cui provenivano : plusvalenze, giacenze e quadri RW/RT verranno "
                        + "ricalcolati senza di loro.<br><br>"
                        + "L'archivio viene salvato subito : dell'archivio <b>prima</b> della "
                        + "cancellazione resta una copia nella cartella <b>Backup</b>, ed è l'unico modo "
                        + "per tornare indietro.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina movimenti e documento")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (secondo == null || !secondo.isAction("elimina")) {
            return EsitoEliminazione.NIENTE;
        }

        int eliminati = EliminaMovimentiDelDocumento(Id);
        //Salvataggio permanente : il file attuale, con i movimenti ancora dentro, diventa il backup
        Importazioni.Scrivi_Movimenti_Crypto(MappaCryptoWallet, true);
        DocumentiFonte.Annulla(Id);
        return new EsitoEliminazione(1, eliminati);
    }

    /**
     * Toglie dalla mappa tutti i movimenti che puntano al documento indicato.
     *
     * <p>Passa da {@link Funzioni#RimuoviMovimentazioneXID}, e non da una {@code remove} diretta, perché
     * un movimento classificato non è solo sé stesso : quella funzione elimina i movimenti generati
     * automaticamente dal suo gruppo e riporta gli altri alla situazione precedente. Da qui i due
     * accorgimenti :
     * <ul>
     *   <li>gli ID si raccolgono in una passata a parte e si eliminano dopo, perché la mappa cambia
     *       sotto le mani;</li>
     *   <li>si <b>ripete</b> finché una passata non trova più nulla. Sugli scambi differiti il
     *       ripristino <i>rinomina</i> i movimenti rimasti (toglie il prefisso dall'ID), quindi un
     *       movimento di questo documento può ricomparire sotto una chiave nuova dopo che la sua è già
     *       stata visitata — e resterebbe a puntare a un documento che non esiste più. Il limite di
     *       passate è una rete di sicurezza, non il caso normale.</li>
     * </ul>
     *
     * <p>Un movimento di un <b>altro</b> documento non resta con un {@code [20]} che punta nel vuoto, e
     * non è un caso fortunato : il riferimento incrociato è scritto <b>su tutti i membri del gruppo</b>,
     * ognuno con l'elenco degli altri (verificato su tutti i punti che scrivono {@code [20]} in
     * {@code GUI_ClassificazioneMovimento}). Chi cita un movimento eliminato è quindi a sua volta citato
     * da lui, finisce nel gruppo che {@code RimuoviMovimentazioneXID} passa al ripristino e si ritrova
     * {@code [18]}/{@code [19]}/{@code [20]} azzerati. È l'invariante su cui poggia il primo dei due
     * avvisi, che lo promette all'utente.
     *
     * @param Id documento di cui eliminare i movimenti
     * @return quanti movimenti sono stati eliminati davvero
     */
    static int EliminaMovimentiDelDocumento(int Id) {
        int eliminati = 0;
        for (int passata = 0; passata < 10; passata++) {
            List<String> daEliminare = new ArrayList<>();
            for (Map.Entry<String, String[]> e : MappaCryptoWallet.entrySet()) {
                String v[] = e.getValue();
                if (v != null && v.length > 41 && DocumentiFonte.IdDaCampo41(v[41]) == Id) {
                    daEliminare.add(e.getKey());
                }
            }
            if (daEliminare.isEmpty()) {
                break;
            }
            for (String ID : daEliminare) {
                //Come in Principale.Funzione_EliminaMovimenti : rimuovendo un movimento collegato ne
                //spariscono altri del suo gruppo, quindi si conta solo ciò su cui si è agito davvero
                if (MappaCryptoWallet.get(ID) != null) {
                    Funzioni.RimuoviMovimentazioneXID(ID);
                    eliminati++;
                }
            }
        }
        return eliminati;
    }

    /**
     * Quali fra i documenti indicati non si possono eliminare, e perché.
     *
     * <p>È il controllo <b>autorevole</b>: il pannello disabilita già il pulsante leggendo la propria
     * colonna, ma quello è solo un aiuto all'utente e si basa su una tabella che potrebbe essere stata
     * caricata prima dell'ultima importazione. La decisione vera si prende qui, sui movimenti in mappa in
     * questo momento, perché eliminare un documento ancora agganciato perderebbe per sempre la provenienza
     * di quei movimenti: i file non sono ricostruibili.
     *
     * @param Ids documenti candidati all'eliminazione
     * @return descrizione dei documenti non eliminabili, vuota se lo sono tutti
     */
    public static List<String> NonEliminabili(List<Integer> Ids) {
        List<String> conMovimenti = new ArrayList<>();
        if (Ids == null || Ids.isEmpty()) {
            return conMovimenti;
        }
        Map<Integer, DocumentiFonte.Riepilogo> riepiloghi = DocumentiFonte.Riepiloghi();
        for (int Id : Ids) {
            DocumentiFonte.Riepilogo r = riepiloghi.get(Id);
            if (r != null && r.Movimenti > 0) {
                DocumentiFonte.Documento d = DocumentiFonte.Leggi(Id);
                conMovimenti.add((d == null ? "documento " + Id : d.NomeOriginale)
                        + " (" + r.Movimenti + " movimenti)");
            }
        }
        return conMovimenti;
    }

    /** @return la dimensione in byte resa leggibile (B / KB / MB) */
    static String DimensioneLeggibile(long Byte) {
        if (Byte <= 0) {
            return "-";
        }
        if (Byte < 1024) {
            return Byte + " B";
        }
        if (Byte < 1024 * 1024) {
            return String.format("%.1f KB", Byte / 1024.0);
        }
        return String.format("%.1f MB", Byte / (1024.0 * 1024.0));
    }
}
