package com.giacenzecrypto.giacenze_crypto;

/**
 * Richiesta di interruzione di un'operazione lunga, con un ciclo di vita <b>esplicito</b>.
 *
 * <p>Esiste perché {@link Principale#InterrompiCiclo} non è utilizzabile fuori dagli script Node: è
 * <b>appiccicoso</b>. Viene azzerato dal costruttore di {@link Download} e riacceso da
 * {@code formWindowClosed}, che scatta su <b>ogni</b> chiusura della finestra di avanzamento, anche quella
 * normale di fine lavoro. Dopo un'importazione andata a buon fine il flag resta quindi {@code true} fino
 * alla successiva apertura di una finestra di avanzamento: un controllo messo dentro il ciclo dei prezzi lo
 * leggerebbe acceso e disattiverebbe in silenzio ogni scaricamento successivo della stessa sessione.
 *
 * <p>Qui la richiesta vale <b>solo dentro un'operazione aperta</b>: {@link #Richiesta()} è
 * {@code false} per costruzione fuori da una coppia {@link #Apri()}/{@link #Chiudi()}, quindi una richiesta
 * rimasta pendente non può sopravvivere all'operazione che l'ha ricevuta. È questa la proprietà che rende
 * sicuro il controllo nei punti caldi: la fase di scaricamento prezzi delle importazioni
 * ({@link Prezzi#CambioXXXEUR}, {@link Prezzi#RecuperaTassidiCambio}), che dura minuti e prima non era
 * fermabile in alcun modo.
 *
 * <p><b>{@code Apri()}/{@code Chiudi()} vanno solo nel proprietario dell'operazione</b> — oggi
 * {@link CcxtInterop#fetchMovimentiConBar} — e mai nel costruttore di {@link Download}: le finestre di
 * avanzamento si annidano (per esempio quella di {@link Prezzi#GUI_ModificaPrezzoConAttesa}) e legare
 * l'apertura alla finestra lascerebbe il conteggio disallineato appena una di esse salta la chiusura.
 * Il conteggio è comunque annidabile, così un'operazione che ne contiene un'altra non chiude lo scope
 * dell'altra prima del tempo.
 *
 * <p><b>Invariante da non rompere:</b> dentro uno scope aperto non deve chiudersi <i>nessun'altra</i>
 * finestra {@link Download} oltre a quella dell'operazione. {@code Download.formWindowClosed} chiama
 * {@link #Chiedi()} e scatta su ogni chiusura, anche quella programmatica di fine lavoro: una finestra
 * altrui che si chiudesse lì dentro interromperebbe un'importazione sana. Oggi l'invariante regge perché
 * la finestra dell'operazione è <b>modale</b> — {@code progress.setVisible(true)} blocca il chiamante e
 * l'utente non può avviare nient'altro — e nessuna delle chiamate dentro lo scope ne apre una propria.
 * Chi estendesse lo scope ad altri flussi (gli import da file, per esempio) deve ricontrollarlo.
 *
 * <p>Il vecchio {@code InterrompiCiclo} resta al suo posto e continua a fare il suo mestiere (fermare gli
 * script Node): i due convivono, e chi chiede l'interruzione accende entrambi.
 */
public final class Interruzione {

    private Interruzione() { }

    /** Livello di annidamento delle operazioni aperte; 0 = nessuna operazione in corso. */
    private static volatile int annidamento = 0;
    /** Richiesta pendente. Ha significato solo con {@link #annidamento} maggiore di zero. */
    private static volatile boolean richiesta = false;

    /**
     * Apre un'operazione interrompibile. Va sempre accoppiata a {@link #Chiudi()} in un {@code finally},
     * altrimenti il conteggio resta alto e una richiesta vecchia tornerebbe a essere appiccicosa.
     */
    public static synchronized void Apri() {
        if (annidamento == 0) richiesta = false;
        annidamento++;
    }

    /** Chiude l'operazione aperta da {@link #Apri()}, azzerando la richiesta quando si esce dall'ultima. */
    public static synchronized void Chiudi() {
        annidamento--;
        if (annidamento <= 0) {
            annidamento = 0;
            richiesta = false;
        }
    }

    /** Registra la richiesta di interruzione dell'utente. Fuori da un'operazione aperta non ha effetto. */
    public static void Chiedi() {
        richiesta = true;
    }

    /**
     * @return {@code true} solo se c'è un'operazione aperta <b>e</b> l'utente ne ha chiesto l'interruzione
     */
    public static boolean Richiesta() {
        return annidamento > 0 && richiesta;
    }

    /** @return {@code true} se è in corso un'operazione interrompibile (usato dai test) */
    static boolean OperazioneAperta() {
        return annidamento > 0;
    }

    /** Riporta lo stato a operazione chiusa e senza richieste. Serve ai test, non al codice di produzione. */
    static synchronized void Azzera() {
        annidamento = 0;
        richiesta = false;
    }
}
