package com.giacenzecrypto.giacenze_crypto;

/**
 * Attesa del ritorno della connessione durante un'importazione, e abbandono dell'importazione quando
 * non torna.
 *
 * <p><b>Il problema che risolve.</b> {@link Prezzi#CambioXXXEUR} controlla la connessione prima di
 * andare in rete e, se manca, restituisce il prezzo di ripiego — spesso {@code null}. Il chiamante non
 * ha modo di distinguere «questa moneta non ha prezzo» da «è caduta la linea», quindi
 * {@link MovimentiCrypto#creaMovimento} scrive {@code 0.00} con campo {@code [32] = "NO"} e
 * l'importazione tira dritto. Peggio, {@link Funzioni#CeConnessioneInternet()} tiene in cache l'esito
 * per un minuto: dopo il primo fallimento ogni movimento successivo riceve un {@code false} immediato e
 * l'intera fase prezzi collassa in una frazione di secondo. Il 12/09/2026, su un archivio Binance di
 * 19.166 movimenti, la caduta della linea alle 19:11:51 ha lasciato <b>15.158 movimenti senza prezzo</b>
 * — scritti, salvati e segnalati solo da un contatore.
 *
 * <p><b>La regola concordata:</b> meglio non importare niente che importare senza prezzo. Prima di
 * arrendersi si riprova {@link #TENTATIVI} volte a distanza di {@link #PAUSA_MS}; se la linea non torna
 * l'importazione viene abbandonata <b>per intero</b>, senza scrivere nulla. Non scrivere nulla è anche
 * ciò che la rende ripetibile: è lo stesso principio dell'import OKX incompleto (bug C13), dove una
 * scrittura parziale sposterebbe in avanti la data di partenza e i movimenti saltati non verrebbero più
 * richiesti.
 *
 * <p>Cinque cose che il disegno dà per assodate:
 *
 * <ul>
 *   <li><b>L'attesa non sta dentro {@code CeConnessioneInternet()}.</b> Quel controllo è condiviso da
 *       tutto il programma — la valorizzazione di un singolo movimento, il ricalcolo RW, l'aggiornamento
 *       delle configurazioni — e bloccarlo nove minuti congelerebbe operazioni che non c'entrano nulla.
 *       Qui l'attesa vale <b>solo dentro uno scope aperto</b> da chi possiede l'importazione: fuori,
 *       {@link #Attendi()} risponde {@code false} all'istante e il comportamento è quello di sempre.</li>
 *   <li><b>{@link #Attendi()} riprova una volta sola per scope.</b> Appena la resa è decisa,
 *       {@link #Abortita()} diventa vera e ogni chiamata successiva torna subito: altrimenti i nove
 *       minuti si pagherebbero a ogni movimento rimasto, cioè per sempre. Dopo la resa l'importazione
 *       corre fino in fondo senza rete e non scrive.</li>
 *   <li><b>La cache di {@code CeConnessioneInternet()} va invalidata a ogni tentativo</b>
 *       ({@link Funzioni#ScadeCacheConnessione()}). Con {@link #PAUSA_MS} a tre minuti il minuto di
 *       cache sarebbe già scaduto da sé, ma legare la correttezza alla durata della pausa significa
 *       rompere il meccanismo il giorno in cui qualcuno la accorcia per provarlo.</li>
 *   <li><b>L'attesa è annullabile e si vede.</b> Si dorme a fette di {@link #PASSO_MS} controllando
 *       {@link Interruzione#Richiesta()} e la finestra di avanzamento; annullare equivale ad arrendersi,
 *       perché la linea è comunque assente. Per questo lo scope di {@link Interruzione} è stato esteso
 *       agli import da file: prima era aperto solo da {@code CcxtInterop.fetchMovimentiConBar} e durante
 *       un import da CSV il tasto Interrompi non arrivava alla fase prezzi.</li>
 *   <li><b>Mai sull'EDT.</b> Dormire sul thread grafico bloccherebbe anche la finestra che deve mostrare
 *       l'attesa e il pulsante che deve annullarla. Le importazioni girano già su un thread proprio; se
 *       qualcuno chiamasse da EDT si rinuncia all'attesa invece di congelare il programma.</li>
 * </ul>
 */
public final class AttesaConnessione {

    private AttesaConnessione() { }

    /** Quante volte si riprova prima di abbandonare l'importazione. */
    public static final int TENTATIVI = 3;
    /** Pausa fra un tentativo e il successivo. */
    public static final long PAUSA_MS = 3 * 60 * 1000L;
    /** Granularità del sonno: è anche la latenza con cui l'annullamento viene raccolto. */
    private static final long PASSO_MS = 500L;

    /** Livello di annidamento degli scope aperti; 0 = nessuna importazione in corso. */
    private static volatile int annidamento = 0;
    /** Resa decisa nello scope corrente. Ha significato solo con {@link #annidamento} maggiore di zero. */
    private static volatile boolean abortita = false;
    /** Finestra di avanzamento dell'operazione, per mostrare l'attesa e raccogliere l'annullamento. */
    private static volatile Download avanzamento = null;

    /**
     * Apre lo scope di un'importazione. Va sempre accoppiata a {@link #Chiudi()} in un {@code finally},
     * altrimenti una resa vecchia sopravvivrebbe all'importazione che l'ha subita e bloccherebbe in
     * silenzio la scrittura di quella dopo.
     *
     * @param finestraAvanzamento finestra su cui scrivere lo stato dell'attesa e da cui raccogliere
     *                            l'annullamento; può essere {@code null}
     */
    public static synchronized void Apri(Download finestraAvanzamento) {
        if (annidamento == 0) {
            abortita = false;
            avanzamento = finestraAvanzamento;
        }
        annidamento++;
    }

    /** Chiude lo scope aperto da {@link #Apri(Download)}, azzerando la resa quando si esce dall'ultimo. */
    public static synchronized void Chiudi() {
        annidamento--;
        if (annidamento <= 0) {
            annidamento = 0;
            abortita = false;
            avanzamento = null;
        }
    }

    /** @return {@code true} se un'importazione è in corso, cioè se {@link #Attendi()} ha qualcosa da fare */
    public static boolean ScopeAperto() {
        return annidamento > 0;
    }

    /**
     * @return {@code true} se in questo scope la linea non è tornata e l'importazione va abbandonata.
     *         Fuori da uno scope è sempre {@code false}, quindi i punti di scrittura possono
     *         interrogarla senza condizioni.
     */
    public static boolean Abortita() {
        return annidamento > 0 && abortita;
    }

    /**
     * Chiamata dai punti che hanno appena constatato l'assenza di connessione. Aspetta il ritorno della
     * linea, e se non torna decide la resa dell'importazione in corso.
     *
     * @return {@code true} se la connessione è tornata e il chiamante può riprovare ad andare in rete;
     *         {@code false} in ogni altro caso — fuori da uno scope, dopo una resa già decisa, su EDT,
     *         o quando i tentativi si esauriscono
     */
    public static boolean Attendi() {
        if (annidamento == 0) return false;      //fuori da un'importazione non si cambia nulla
        if (abortita) return false;              //resa gia' decisa: non si pagano altri nove minuti

        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            LoggerGC.ScriviErrore("AttesaConnessione: chiamata dall'EDT, attesa saltata per non bloccare l'interfaccia");
            Arrenditi();
            return false;
        }

        for (int tentativo = 1; tentativo <= TENTATIVI; tentativo++) {
            String stato = "Connessione assente. Nuovo tentativo fra "
                    + (PAUSA_MS / 60000) + " minuti (" + tentativo + " di " + TENTATIVI + ")...";
            LoggerGC.logInfo("AttesaConnessione: " + stato);
            Avvisa(stato);

            if (!Dormi(PAUSA_MS)) {
                LoggerGC.logInfo("AttesaConnessione: attesa annullata, importazione abbandonata");
                Arrenditi();
                return false;
            }

            //Senza questa invalidazione il test tornerebbe l'esito in cache invece di riprovare davvero
            Funzioni.ScadeCacheConnessione();
            if (Funzioni.CeConnessioneInternet()) {
                LoggerGC.logInfo("AttesaConnessione: connessione tornata al tentativo " + tentativo
                        + ", l'importazione riprende");
                Avvisa("Connessione ripristinata, l'importazione riprende.");
                return true;
            }
        }

        LoggerGC.ScriviErrore("AttesaConnessione: connessione assente dopo " + TENTATIVI
                + " tentativi, l'importazione viene abbandonata e non verra' scritto alcun movimento");
        Arrenditi();
        return false;
    }

    /** Decide la resa dello scope corrente. */
    private static synchronized void Arrenditi() {
        if (annidamento > 0) abortita = true;
        Avvisa("Connessione assente: importazione annullata, nessun movimento verra' inserito.");
    }

    /**
     * Dorme a fette raccogliendo l'annullamento.
     * @return {@code false} se l'attesa è stata annullata prima di completarsi
     */
    private static boolean Dormi(long millis) {
        long scadenza = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < scadenza) {
            if (Annullata()) return false;
            try {
                Thread.sleep(PASSO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !Annullata();
    }

    /** @return {@code true} se l'utente ha chiesto di interrompere, dal tasto o chiudendo la finestra */
    private static boolean Annullata() {
        if (Interruzione.Richiesta()) return true;
        Download d = avanzamento;
        //Si legge il campo, non il metodo FineThread(): quello ha effetti collaterali (chiude la
        //finestra se il thread e' morto, azzera Principale.InterrompiCiclo) che qui non si vogliono.
        return d != null && d.FineThread;
    }

    /** Scrive lo stato sulla finestra di avanzamento, se c'è. */
    private static void Avvisa(String testo) {
        Download d = avanzamento;
        if (d == null) return;
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                d.SetMessaggioAvanzamento(testo);
            } catch (Exception ignorata) {
                //la finestra puo' essere gia' stata chiusa: l'avviso e' comunque nel log
            }
        });
    }
}
