/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;

/**
 *
 * @author luca.passelli
 */
public class Messaggi {
    
    /**
     * Mostra un dialog di conferma per assegnare valore zero a un token privo di prezzo.
     * @param Moneta simbolo del token privo di prezzo
     * @param Qta quantità del token
     * @param w finestra parent del dialog
     * @return {@code true} se l'utente conferma l'assegnazione di valore zero
     */
    public static boolean ConfermaTokenSenzaPrezzo(String Moneta, String Qta,Window w) {
         AppDialog.DialogResult result = AppDialog.builder(w)
                        .windowTitle("Token senza prezzo")
                        .bodyTitle("Token senza prezzo")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Il token " + Moneta + " attualmente non ha un prezzo disponibile.")
                        .details("Vuoi assegnare il valore 0 ai " + Qta + " " + Moneta + "?")
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder("assign-zero", "Assegna zero")
                                .role(AppDialog.ActionRole.DANGER)
                                .build())
                        .showDialog();

                return result.isAction("assign-zero");
        
    }
    
        /**
         * Mostra un dialog di conferma per assegnare valore zero a un movimento privo di prezzo.
         * @param w finestra parent del dialog
         * @return {@code true} se l'utente conferma il valore zero
         */
        public static boolean ConfermaMovimentoSenzaPrezzo(Window w) {
         AppDialog.DialogResult result = AppDialog.builder(w)
                        .windowTitle("Movimento senza prezzo")
                        .bodyTitle("Movimento senza prezzo")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Il movimento attualmente non ha prezzo o è valorizzato a 0.00")
                        .details("Vuoi confermare il valore di € 0.00 al movimento?")
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder("assign-zero", "Assegna zero")
                                .role(AppDialog.ActionRole.DANGER)
                                .build())
                        .showDialog();

                return result.isAction("assign-zero");
        
    }
    
        
        
        //Messaggi di conferma semplificati
        
        /**
         * Mostra un dialog di avviso (tipo {@code WARNING}) senza messaggio principale, solo titolo e dettaglio.
         * @param Titolo titolo del dialog
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void WarningMessage(String Titolo,String Dettaglio,Window w){
            Message(Titolo,"",Dettaglio,AppDialog.DialogType.WARNING,w);
        }
        /**
         * Mostra un dialog di avviso (tipo {@code WARNING}) con messaggio principale e dettaglio.
         * @param Titolo titolo del dialog
         * @param Messaggio messaggio principale
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void WarningMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
            Message(Titolo,Messaggio,Dettaglio,AppDialog.DialogType.WARNING,w);
        }
        /**
         * Mostra un dialog informativo (tipo {@code INFO}) senza messaggio principale, solo titolo e dettaglio.
         * @param Titolo titolo del dialog
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void InfoMessage(String Titolo,String Dettaglio,Window w){
            Message(Titolo,"",Dettaglio,AppDialog.DialogType.INFO,w);
        }
        /**
         * Mostra un dialog informativo (tipo {@code INFO}) con messaggio principale e dettaglio.
         * @param Titolo titolo del dialog
         * @param Messaggio messaggio principale
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void InfoMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
            Message(Titolo,Messaggio,Dettaglio,AppDialog.DialogType.INFO,w);
        }
        /**
         * Mostra un dialog di successo (tipo {@code SUCCESS}) senza messaggio principale, solo titolo e dettaglio.
         * @param Titolo titolo del dialog
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void SuccessMessage(String Titolo,String Dettaglio,Window w){
            Message(Titolo,"",Dettaglio,AppDialog.DialogType.SUCCESS,w);
        }
        /**
         * Mostra un dialog di successo (tipo {@code SUCCESS}) con messaggio principale e dettaglio.
         * @param Titolo titolo del dialog
         * @param Messaggio messaggio principale
         * @param Dettaglio testo di dettaglio
         * @param w finestra parent del dialog
         */
        public static void SuccessMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
            Message(Titolo,Messaggio,Dettaglio,AppDialog.DialogType.SUCCESS,w);
        }
        /**
         * Mostra un dialog informativo generico con singolo pulsante "OK", usato come base dai metodi
         * {@code WarningMessage}/{@code InfoMessage}/{@code SuccessMessage}.
         * @param Titolo titolo del dialog
         * @param Messaggio messaggio principale
         * @param Dettaglio testo di dettaglio
         * @param TipoM tipo di dialog (icona/stile)
         * @param w finestra parent del dialog
         */
        public static void Message(String Titolo,String Messaggio,String Dettaglio,AppDialog.DialogType TipoM,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(TipoM)
                            .message(Messaggio)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }

        
        
        
        
    /**
     * Mostra un dialog di conferma per modificare un movimento associato a un altro movimento, avvisando che
     * l'associazione verrà rimossa.
     * @param win finestra parent del dialog
     * @return {@code true} se l'utente conferma di voler proseguire con la modifica
     */
    public static boolean Personalizzati_SINO_ModificaMovimento(Window win) {
        AppDialog.DialogResult result = AppDialog.builder(win)
                    .windowTitle("Conferma modifica")
                    .bodyTitle("Modificare il movimento?")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Attenzione!<br><br>Il movimento è associato a un altro movimento.")
                    .details("""
                    Se prosegui, l'associazione verrà rimossa prima della modifica.

                    Vuoi continuare?
                    """)
                    .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                            .role(AppDialog.ActionRole.SECONDARY)
                            .build())
                    .action(AppDialog.DialogAction.builder("continue-edit", "Continua modifica")
                            .role(AppDialog.ActionRole.DANGER)
                            .build())
                    .showDialog();
        return (result != null && result.isAction("continue-edit"));
    }
    
    
     /**
      * Mostra un dialog di conferma per rimuovere un token dalla lista degli E-Money Token.
      * @param moneta simbolo del token da rimuovere
      * @param win finestra parent del dialog
      * @return {@code true} se l'utente conferma la rimozione
      */
     public static boolean Personalizzati_SINO_RimuoviEmoneyToken(String moneta,Window win) {
        AppDialog.DialogResult result = AppDialog.builder(win)
                    .windowTitle("Gestione EMoney Token")
                    .bodyTitle("Rimuovere il token?")
                    .showTitleInBody(false)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Vuoi rimuovere il token " + moneta + " dalla lista degli EMoney Token?")
                    .details("L'operazione aggiornerà anche i dati collegati.")
                    .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                            .role(AppDialog.ActionRole.SECONDARY)
                            .build())
                    .action(AppDialog.DialogAction.builder("delete-emoney-token", "Rimuovi token")
                            .role(AppDialog.ActionRole.DANGER)
                            .build())
                    .showDialog();
        return (result != null && result.isAction("delete-emoney-token"));
    }
     
     /**
      * Mostra un dialog di input per digitare il nome (case-sensitive) di un nuovo token da aggiungere alla
      * lista degli E-Money Token.
      * @param win finestra parent del dialog
      * @return il nome del token inserito, oppure {@code null} se l'operazione è stata annullata
      */
     public static String Personalizzati_Input_NuovoEmoneyToken(Window win) {
        String testo = "Digita il nome della moneta da aggiungere alla lista degli E-Money Token.";
        String dettagli = """
        Il nome del token è case-sensitive.

        Ad esempio: BTC è diverso da Btc o btc.
        """;

        AppDialog.DialogResult result = AppDialog.builder(win)
                .windowTitle("Aggiunta E-Money Token")
                .bodyTitle("Nuovo E-Money Token")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message(testo)
                .details(dettagli)
                .inputField("")
                .inputColumns(18)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("add-token", "Aggiungi")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        if (result != null && result.isAction("add-token")) {
            String m = result.getInputValue();
            return m;           
        }
        return null;
    }
     
     /**
      * Mostra un dialog di input per modificare l'alias di un gruppo wallet, precompilato con il valore attuale.
      * @param gruppo nome del gruppo wallet
      * @param val valore attuale dell'alias, mostrato come default nel campo di input
      * @param win finestra parent del dialog
      * @return il nuovo alias inserito, oppure {@code null} se l'operazione è stata annullata
      */
     public static String Personalizzati_Input_AliasGruppoWallet(String gruppo,String val,Window win) {
        AppDialog.DialogResult result = AppDialog.builder(win)
            .windowTitle("Alias gruppo wallet")
            .bodyTitle("Modifica alias")
            .showTitleInBody(true)
            .theme()
            .type(AppDialog.DialogType.INFO)
            .message("Indica il nuovo alias per il gruppo " + gruppo + ".")
            .details("Il nome verrà normalizzato prima del salvataggio.")
            .inputField("Valore Originale : "+val,val)
            .inputColumns(24)
            .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                    .role(AppDialog.ActionRole.SECONDARY)
                    .build())
            .action(AppDialog.DialogAction.builder("save-alias", "Salva")
                    .role(AppDialog.ActionRole.PRIMARY)
                    .build())
            .showDialog();
        if (result != null && result.isAction("save-alias")) {
            String m = result.getInputValue();
            return m;           
        }
        return null;
    }
     
     
     /**
      * Mostra un dialog con una scelta tra 3 tipologie di errore da correggere (movimento non classificato,
      * transazione senza prezzo, parte del LIFO mancante), mostrando il conteggio di ciascuna tipologia.
      * @param NumErroriMovSconosciuti numero di movimenti non classificati
      * @param NumErroriMovNoPrezzo numero di transazioni senza prezzo
      * @param NumErroriStackLiFoMancante numero di errori con parte del LIFO mancante
      * @param win finestra parent del dialog
      * @return il risultato del dialog, da cui leggere l'azione scelta dall'utente tramite {@code isAction}
      */
     public static AppDialog.DialogResult Personalizzati_Multi_ScegliErrori(int NumErroriMovSconosciuti,int NumErroriMovNoPrezzo,int NumErroriStackLiFoMancante,Window win) {
         String testo = "Scegli quale tipologia di errore correggere.";
         AppDialog.DialogResult result = AppDialog.builder(win)
                 .windowTitle("Correzione errori")
                 .bodyTitle("Selezione errore")
                 .showTitleInBody(true)
                 .theme()
                 .type(AppDialog.DialogType.INFO)
                 .message(testo)
                 .details("")
                 .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                         .role(AppDialog.ActionRole.SECONDARY)
                         .build())
                 .action(AppDialog.DialogAction.builder("nonClassificati", "Movimento non classificato (" + NumErroriMovSconosciuti + ")")
                         .role(AppDialog.ActionRole.PRIMARY)
                         .build())
                 .action(AppDialog.DialogAction.builder("PrezzoMancante", "Transazione senza prezzo (" + NumErroriMovNoPrezzo + ")")
                         .role(AppDialog.ActionRole.PRIMARY)
                         .build())
                 .action(AppDialog.DialogAction.builder("LifoMancante", "Parte del LIFO mancante (" + NumErroriStackLiFoMancante + ")")
                         .role(AppDialog.ActionRole.PRIMARY)
                         .build())
                 .showDialog();
         
         return result;
     }
     
          /**
           * Mostra un dialog di avviso quando un token marcato come scam presenta movimenti diversi dal semplice
           * deposito/prelievo, chiedendo se proseguire comunque nonostante il rischio di calcoli errati.
           * @param NomeMoneta simbolo del token scam
           * @param win finestra parent del dialog
           * @return il risultato del dialog, da cui leggere l'azione scelta dall'utente tramite {@code isAction}
           */
          public static AppDialog.DialogResult Personalizzati_SINO_SCAMMovimentiNonCongrui(String NomeMoneta,Window win) {
         AppDialog.DialogResult result = AppDialog.builder(win)
                        .windowTitle("Verifica movimenti")
                        .bodyTitle("Movimenti non coerenti con token scam")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Il token " + NomeMoneta + " presenta movimenti diversi dal semplice deposito o prelievo.")
                        .details("""
                        Solitamente i token scam presentano solo movimenti di deposito o prelievo.

                        Se prosegui senza verificare, i calcoli potrebbero risultare errati.
                        """)
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder("continue-anyway", "Continua comunque")
                                .role(AppDialog.ActionRole.DANGER)
                                .build())
                        .showDialog();
         
         return result;
     }
     
                /**
                 * Mostra un dialog di conferma per contrassegnare un token come scam o, se già scam, per rimuovere
                 * la classificazione, adattando titolo/messaggio/dettaglio in base allo stato attuale.
                 * @param scamAttuale {@code true} se il token è attualmente classificato come scam (mostra il dialog di rimozione), {@code false} altrimenti (mostra il dialog di contrassegno)
                 * @param NomeMoneta simbolo del token
                 * @param Address indirizzo di contratto del token (mostrato solo quando si contrassegna come scam)
                 * @param win finestra parent del dialog
                 * @return il risultato del dialog, da cui leggere l'azione scelta dall'utente tramite {@code isAction}
                 */
                public static AppDialog.DialogResult Personalizzati_SINO_SCAMRimuovereContrassegnare(boolean scamAttuale,String NomeMoneta,String Address,Window win) {
         String bodyTitle = scamAttuale
                        ? "Rimuovere classificazione SCAM?"
                        : "Contrassegnare come SCAM?";

                String message = scamAttuale
                        ? "Vuoi fare in modo che il token " + NomeMoneta + " non venga più considerato SCAM?"
                        : "Vuoi identificare il token " + NomeMoneta + " con address " + Address + " come SCAM?";

                String details = scamAttuale
                        ? ""
                        : """
          Nelle varie funzioni del programma sarà possibile nascondere questo asset.

          Quando mostrato, verrà identificato con un doppio asterisco (**) alla fine del nome.

          Per riportare il token allo stato normale, usa l'apposita funzione in "Giacenze a data".
          """;

                AppDialog.DialogType type = scamAttuale
                        ? AppDialog.DialogType.INFO
                        : AppDialog.DialogType.WARNING;

                String actionId = scamAttuale ? "unmark-scam" : "mark-scam";
                String actionLabel = scamAttuale ? "Rimuovi classificazione" : "Segna come SCAM";
                AppDialog.ActionRole actionRole = scamAttuale
                        ? AppDialog.ActionRole.PRIMARY
                        : AppDialog.ActionRole.DANGER;

                AppDialog.DialogResult result = AppDialog.builder(win)
                        .windowTitle("Classificazione token")
                        .bodyTitle(bodyTitle)
                        .showTitleInBody(true)
                        .theme()
                        .type(type)
                        .message(message)
                        .details(details)
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder(actionId, actionLabel)
                                .role(actionRole)
                                .build())
                        .showDialog();
         
         return result;
     }
     
     

}
