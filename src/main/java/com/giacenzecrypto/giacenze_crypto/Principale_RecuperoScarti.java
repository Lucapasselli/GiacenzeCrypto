package com.giacenzecrypto.giacenze_crypto;

import java.awt.Cursor;
import java.awt.Window;
import java.util.List;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Quando e come proporre il recupero dei movimenti scartati perché sconosciuti ({@link ScartiImport}).
 *
 * <p>Tre momenti, decisi con l'utente il 2026-10-08: dopo ogni importazione via API, all'avvio (mappe arrivate
 * con una versione nuova o aggiornate in una sessione precedente) e quando l'aggiornamento delle mappe da GitHub,
 * che gira in background all'avvio, ne porta di nuove. Il costo è nullo se non è cambiato niente: si rileggono
 * solo i documenti non ancora riletti con le mappe di oggi.
 *
 * <p>Segue lo schema delle altre estrazioni: nessun campo Swing, nessun riferimento a {@code Principale}; chi
 * chiama passa la finestra e, se serve, ricalcola. Restituisce {@code true} se ha aggiunto movimenti.
 */
public final class Principale_RecuperoScarti {

    /** Un controllo alla volta: quello dell'aggiornamento mappe può arrivare mentre un altro è aperto */
    private static boolean InCorso = false;

    /**
     * Impronta delle mappe per cui l'utente ha risposto "Chiedi più tardi": in questa sessione i controlli
     * automatici d'avvio non la ripropongono. Dopo un'importazione sì, perché è l'utente ad aver agito.
     */
    private static String RimandatoPer = null;

    /** Il timer che aspetta la finestra libera dopo l'aggiornamento delle mappe */
    private static Timer Attesa = null;

    private Principale_RecuperoScarti() {
    }

    /**
     * Rilegge i documenti non ancora riletti con le mappe di oggi e, se c'è qualcosa da recuperare, lo propone.
     *
     * @param Owner finestra su cui centrare dialoghi e avanzamento
     * @param DopoImportazione {@code true} se chiamato alla fine di un'importazione
     * @return {@code true} se sono stati aggiunti movimenti: il chiamante deve aggiornare le tabelle
     */
    public static boolean Controlla(Window Owner, boolean DopoImportazione) {
        if (InCorso) {
            return false;
        }
        InCorso = true;
        try {
            Owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            ScartiImport.Analisi A;
            try {
                A = ScartiImport.AnalizzaOKX(Principale.MappaCryptoWallet, false);
            } finally {
                Owner.setCursor(Cursor.getDefaultCursor());
            }
            for (int Id : A.SenzaSospesi) {
                ScartiImport.SegnaAnalizzato(Id, A.Impronta);
            }
            if (A.Vuota() || (!DopoImportazione && A.Impronta.equals(RimandatoPer))) {
                return false;
            }

            GUI_RecuperoScarti d = GUI_RecuperoScarti.Mostra(Owner, A);
            if (d.Scelta() != GUI_RecuperoScarti.Scelta.RECUPERA) {
                RimandatoPer = A.Impronta;
                return false;
            }
            //Prima gli ignorati e poi il recupero: l'import modifica sul posto le righe che riceve ([2] diventa
            //"Principale"), e la chiave del registro e' fatta proprio col conto
            ScartiImport.Ignora(d.NonSelezionati());
            List<ScartiImport.Candidato> scelti = d.Selezionati();
            if (scelti.isEmpty()) {
                return false;
            }
            return Recupera(Owner, scelti) > 0;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        } finally {
            InCorso = false;
        }
    }

    /**
     * Importa i candidati scelti in background, con la finestra di avanzamento: il consolidamento scarica i
     * prezzi. Stessi scope di interruzione e di attesa connessione di uno scaricamento via API.
     *
     * @return movimenti aggiunti
     */
    private static int Recupera(Window Owner, List<ScartiImport.Candidato> Scelti) {
        final int[] Aggiunti = {0};
        final List<ScartiImport.Candidato> NonRecuperati = new java.util.ArrayList<>();
        Download progress = new Download();
        progress.setIndeterminate(true);
        progress.SetLabel("Recupero dei movimenti dai documenti di origine...");
        progress.setLocationRelativeTo(Owner);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            /** Importa i candidati scelti */
            @Override
            protected Void doInBackground() {
                try {
                    Aggiunti[0] = ScartiImport.RecuperaOKX(Scelti);
                    //Dentro gli scope, finche' si sa ancora se l'utente ha interrotto o se la linea e' caduta: in
                    //quei casi cio' che manca resta in sospeso e torna al prossimo controllo. Altrimenti un'unita'
                    //confermata e non scritta si segna ignorata, o verrebbe riproposta per sempre.
                    if (!Interruzione.Richiesta() && !AttesaConnessione.Abortita()) {
                        NonRecuperati.addAll(ScartiImport.NonRecuperati(Scelti, Principale.MappaCryptoWallet));
                        ScartiImport.Ignora(NonRecuperati);
                    }
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
                return null;
            }

            /** Chiude la finestra di avanzamento */
            @Override
            protected void done() {
                progress.ChiudiFineLavoro();
            }
        };
        Interruzione.Apri();
        AttesaConnessione.Apri(progress);
        try {
            worker.execute();
            progress.setVisible(true);
        } finally {
            AttesaConnessione.Chiudi();
            Interruzione.Chiudi();
        }
        String testo = Aggiunti[0] == 0 ? "Nessun movimento è stato aggiunto."
                : "Movimenti aggiunti: " + Aggiunti[0] + ".<br><br>Ricordati di salvare.";
        if (!NonRecuperati.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ScartiImport.Candidato c : NonRecuperati) {
                sb.append("<br>").append(c.Data()).append(" - ").append(c.Causali()).append(" - ").append(c.Movimento());
            }
            testo += "<br><br>Questi non è stato possibile recuperarli e non verranno più proposti :" + sb;
        }
        Messaggi.InfoMessage("Recupero movimenti", testo, Owner);
        return Aggiunti[0];
    }

    /**
     * Fa il controllo appena la finestra è libera: attiva, cioè senza un dialogo modale aperto sopra. Serve
     * all'aggiornamento delle mappe, che finisce in background in un momento qualsiasi: aprire subito il dialogo
     * lo metterebbe sopra un avviso d'avvio o in mezzo a un'importazione.
     *
     * @param Owner la finestra principale
     * @param DopoRecupero cosa fare se sono stati aggiunti movimenti (il ricalcolo), sul thread grafico
     */
    public static void ControllaQuandoLibero(Window Owner, Runnable DopoRecupero) {
        if (Attesa != null) {
            return;
        }
        Attesa = new Timer(3000, null);
        Attesa.addActionListener(e -> {
            if (!Owner.isActive() || InCorso) {
                return;
            }
            Attesa.stop();
            Attesa = null;
            if (Controlla(Owner, false) && DopoRecupero != null) {
                DopoRecupero.run();
            }
        });
        Attesa.start();
    }
}
