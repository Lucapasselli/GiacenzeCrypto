/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author luca.passelli
 */
public class Principale_Opzioni_Pulizie {

    public static boolean confermaECancellaWalletFIATeCARDPerIntervallo(
            String dataIniziale,
            String dataFinale,
            String TIPOWALLET,
            String FileDaPulire,
            Window win) {

        long timeStampIniziale = FunzioniDate.ConvertiDatainLong(dataIniziale);
        long timeStampFinale = FunzioniDate.ConvertiDatainLong(dataFinale) + 86400000;

        String messaggio = "Vuoi eliminare tutti i dati del "+TIPOWALLET+" nel periodo selezionato?";

        AppDialog.DialogResult result = AppDialog.builder(win)
                .windowTitle("Cancellazione "+TIPOWALLET)
                .bodyTitle("Eliminare i dati selezionati?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(messaggio)
                .details("""
                Intervallo incluso: dal %s al %s.

                Verranno rimossi tutti i movimenti del %s compresi nel periodo selezionato.
                """.formatted(dataIniziale, dataFinale,TIPOWALLET))
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("delete-range", "Elimina dati")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result != null && result.isAction("delete-range")) {
            try {
                FileReader fire = new FileReader(FileDaPulire);
                BufferedReader bure = new BufferedReader(fire);
                String rigas;

                List<String> daMantenere = new ArrayList<>();

                while ((rigas = bure.readLine()) != null) {
                    long timeStampMovimento = FunzioniDate.ConvertiDatainLong(rigas.split(" ")[0]);

                    if (timeStampMovimento < timeStampIniziale || timeStampMovimento >= timeStampFinale) {
                        daMantenere.add(rigas);
                    }
                }

                bure.close();
                fire.close();

                FileWriter w = new FileWriter(FileDaPulire);
                BufferedWriter b = new BufferedWriter(w);

                Iterator<String> it = daMantenere.iterator();
                while (it.hasNext()) {
                    b.write(it.next() + "\n");
                }

                b.close();
                w.close();

            } catch (IOException ex) {
                LoggerGC.ScriviErrore(ex);
            }

            return true;
            /*CDC_FiatWallet_Mappa.clear();
            CDC_FiatWallet_Funzione_ImportaWallet(VarStatiche.getFile_CDCFiatWallet());
            CDC_FiatWallet_AggiornaDatisuGUI();

            Messaggi.SuccessMessage("Dati eliminati", "I dati del Fiat Wallet nel periodo selezionato sono stati eliminati correttamente.", win);*/
        }
        return false;
    }


    
    

    // =================================================================================================
    // COMPATTAZIONE DEI DATABASE H2
    // =================================================================================================
    /**
     * Stato dei due database compattabili in una riga: dimensione, riempimento e spazio recuperabile.
     *
     * <p>Legge {@code INFORMATION_SCHEMA.SETTINGS}, che MVStore tiene già calcolato: costa poche decine
     * di millisecondi anche su un file da qualche GB, e può quindi essere richiamato tutte le volte che
     * il pannello viene mostrato.
     *
     * @return il testo già formattato per l'etichetta del pannello
     */
    public static String DescrizioneStatoCompattazione() {

        DatabaseH2.StatoCompattazione Prezzi = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connectionPrezzi);
        DatabaseH2.StatoCompattazione Principale = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connection);

        return "prezzi.mv.db: " + Dimensione(Prezzi.DimensioneFile)
                + " (riempimento " + Percentuale(Prezzi.Riempimento) + ")"
                + "   -   database.mv.db: " + Dimensione(Principale.DimensioneFile)
                + " (riempimento " + Percentuale(Principale.Riempimento) + ")"
                + "   -   recuperabili circa "
                + Dimensione(Prezzi.SpazioRecuperabile() + Principale.SpazioRecuperabile());
    }

    /**
     * Compatta i file di {@code prezzi.mv.db} e {@code database.mv.db} su richiesta dell'utente.
     *
     * <p>La compattazione <b>non cancella nessuna riga</b>: recupera solo lo spazio delle pagine
     * sostituite, che {@code AUTO_COMPACT_FILL_RATE=0} non riusa mai. Le due connessioni vengono chiuse
     * e riaperte, quindi il lavoro sta su un thread separato dietro una finestra di attesa modale.
     * L'attesa modale impedisce solo nuove azioni dall'interfaccia: non ferma i thread dei prezzi ne'
     * un'importazione gia' in corso, che pero' hanno una finestra modale propria.
     *
     * @param win finestra a cui agganciare i dialoghi
     * @return {@code true} se la compattazione è stata eseguita
     */
    public static boolean CompattaDatabase(Window win) {

        DatabaseH2.StatoCompattazione PrezziPrima = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connectionPrezzi);
        DatabaseH2.StatoCompattazione PrincipalePrima = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connection);
        long Recuperabile = PrezziPrima.SpazioRecuperabile() + PrincipalePrima.SpazioRecuperabile();

        AppDialog.DialogResult result = AppDialog.builder(win)
                .windowTitle("Compattazione dei database")
                .bodyTitle("Compattare i file dei database?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Nessun dato viene cancellato: viene recuperato solo lo spazio delle pagine sostituite, che il database non riusa mai da solo.")
                .details("""
                prezzi.mv.db: %s, riempimento %s
                database.mv.db: %s, riempimento %s

                Spazio recuperabile stimato: %s.

                Durante l'operazione i due database vengono chiusi e riaperti: su archivi grandi può durare qualche decina di secondi.
                """.formatted(
                        Dimensione(PrezziPrima.DimensioneFile), Percentuale(PrezziPrima.Riempimento),
                        Dimensione(PrincipalePrima.DimensioneFile), Percentuale(PrincipalePrima.Riempimento),
                        Dimensione(Recuperabile)))
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("compatta", "Compatta")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("compatta")) {
            return false;
        }

        //AtomicBoolean e non un boolean[]: il valore è scritto dal thread di lavoro e letto dall'EDT
        //quando la finestra modale si chiude, quindi serve la garanzia di visibilità tra i due
        AtomicBoolean Esito = new AtomicBoolean(false);

        Download dow = new Download();
        dow.NascondiInterrompi();
        dow.MostraProgressAttesa("Compattazione", "Compattazione dei database in corso");
        dow.SetLabel("Compattazione in corso, attendere...");
        dow.setLocationRelativeTo(win);

        Thread t = new Thread(() -> {
            try {
                //Le due chiamate sono in sequenza e non condizionate l'una all'altra: se la prima
                //fallisce la seconda è comunque indipendente, i lock di H2 sono per file
                boolean OKPrezzi = DatabaseH2.CompattaPrezzi(true);
                boolean OKPrincipale = DatabaseH2.CompattaPrincipale(true);
                Esito.set(OKPrezzi && OKPrincipale);
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            } finally {
                dow.ChiudiFinestra();
            }
        });
        t.start();
        dow.setVisible(true); //bloccante finché il thread chiama ChiudiFinestra

        if (!Esito.get()) {
            Messaggi.WarningMessage("Compattazione non riuscita",
                    "La compattazione non è stata completata. Nessun dato è stato modificato, ma i database potrebbero essere rimasti chiusi: conviene riavviare l'applicazione. Il dettaglio dell'errore è nel log.",
                    win);
            return false;
        }

        DatabaseH2.StatoCompattazione PrezziDopo = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connectionPrezzi);
        DatabaseH2.StatoCompattazione PrincipaleDopo = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connection);

        Messaggi.SuccessMessage("Compattazione completata",
                "Recuperati %s. Nessun dato è stato cancellato.".formatted(
                        Dimensione(Math.max(0, (PrezziPrima.DimensioneFile + PrincipalePrima.DimensioneFile)
                                - (PrezziDopo.DimensioneFile + PrincipaleDopo.DimensioneFile)))),
                """
                prezzi.mv.db: da %s a %s
                database.mv.db: da %s a %s
                """.formatted(
                        Dimensione(PrezziPrima.DimensioneFile), Dimensione(PrezziDopo.DimensioneFile),
                        Dimensione(PrincipalePrima.DimensioneFile), Dimensione(PrincipaleDopo.DimensioneFile)),
                win);

        return true;
    }

    /**
     * Compatta alla chiusura dell'applicazione i database che ne hanno bisogno davvero.
     *
     * <p>È il percorso automatico, e per questo non chiede niente e non mostra risultati: si limita a
     * una finestra di attesa, perché su un archivio grande la compattazione dura una decina di secondi
     * e senza segnalazione sembrerebbe che la chiusura si sia piantata.
     *
     * <p>La decisione sta in {@link DatabaseH2.StatoCompattazione#Conviene()}: un file già in ordine
     * costerebbe il tempo e non restituirebbe niente. La lettura dello stato costa poche decine di
     * millisecondi, quindi la chiusura di chi non ha niente da compattare resta immediata.
     *
     * @param win finestra a cui agganciare l'attesa
     * @return {@code true} se è stata eseguita una compattazione
     */
    public static boolean CompattaSeConviene(Window win) {

        boolean ServePrezzi = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connectionPrezzi).Conviene();
        boolean ServePrincipale = DatabaseH2.LeggiStatoCompattazione(DatabaseH2.connection).Conviene();

        if (!ServePrezzi && !ServePrincipale) {
            return false;
        }

        Download dow = new Download();
        dow.NascondiInterrompi();
        dow.MostraProgressAttesa("Chiusura", "Compattazione dei database in corso");
        dow.SetLabel("Compattazione dei database, attendere...");
        dow.setLocationRelativeTo(win);

        Thread t = new Thread(() -> {
            try {
                //Riapri=false: l'applicazione sta chiudendo, riaprire i file servirebbe solo a
                //richiuderli subito dopo
                if (ServePrezzi) {
                    DatabaseH2.CompattaPrezzi(false);
                }
                if (ServePrincipale) {
                    DatabaseH2.CompattaPrincipale(false);
                }
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            } finally {
                dow.ChiudiFinestra();
            }
        });
        t.start();
        dow.setVisible(true); //bloccante finché il thread chiama ChiudiFinestra

        return true;
    }

    /**
     * Formatta una dimensione in byte in MB o GB.
     *
     * @param Bytes i byte da formattare; un valore negativo vale "non letto"
     * @return il testo formattato, {@code "n.d."} se il valore non è disponibile
     */
    private static String Dimensione(long Bytes) {
        if (Bytes < 0) {
            return "n.d.";
        }
        double MB = Bytes / (1024.0 * 1024.0);
        if (MB >= 1024) {
            return String.format("%.2f GB", MB / 1024.0);
        }
        return String.format("%.0f MB", MB);
    }

    /**
     * @param Valore percentuale di riempimento, negativa se non letta
     * @return la percentuale con il simbolo, {@code "n.d."} se non disponibile
     */
    private static String Percentuale(int Valore) {
        return Valore < 0 ? "n.d." : Valore + "%";
    }

}
