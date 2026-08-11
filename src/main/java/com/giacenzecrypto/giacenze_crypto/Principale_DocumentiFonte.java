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
     * Elimina i documenti selezionati, copia e riga di registro.
     *
     * <p>Rifiuta l'operazione se anche uno solo ha ancora dei movimenti agganciati: quei movimenti
     * resterebbero a puntare nel vuoto, e la traccia del file da cui provengono sarebbe persa per sempre —
     * i documenti non si possono ricostruire.
     *
     * @param Ids documenti da eliminare
     * @param owner finestra rispetto a cui centrare la conferma
     * @return {@code true} se qualcosa è stato eliminato
     */
    public static boolean EliminaDocumenti(List<Integer> Ids, Window owner) {
        if (Ids == null || Ids.isEmpty()) {
            return false;
        }
        List<String> conMovimenti = NonEliminabili(Ids);
        if (!conMovimenti.isEmpty()) {
            Messaggi.WarningMessage("Eliminazione documenti",
                    "Ci sono ancora movimenti che fanno riferimento a questi documenti",
                    "Non eliminabili : " + String.join(", ", conMovimenti)
                    + "<br><br>Eliminandoli quei movimenti perderebbero per sempre il collegamento al file "
                    + "da cui provengono, che non è ricostruibile.", owner);
            return false;
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
            return false;
        }
        for (int Id : Ids) {
            DocumentiFonte.Annulla(Id);
        }
        return true;
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
