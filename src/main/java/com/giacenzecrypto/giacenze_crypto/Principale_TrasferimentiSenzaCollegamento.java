package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * La voce "Trasferimenti senza collegamento" del pulsante Errori: i movimenti classificati come trasferimento ({@code PTW}/{@code DTW})
 * che non sono collegati a nessuna controparte ({@link MovimentiCollegati.Esito#SenzaCollegamento}). Mostra l'elenco e, se l'utente
 * lo chiede, <b>annulla la classificazione</b> di quei movimenti: tornano non classificati e riappaiono in Depositi/Prelievi, dove si
 * abbinano di nuovo. Non si ricostruisce nessun abbinamento da soli: la controparte giusta non è deducibile dal solo movimento.
 *
 * <p>Con le plusvalenze divise per gruppo wallet un trasferimento senza {@code [20]} non sposta il costo di carico fra le pile dei
 * due gruppi (verificato da {@code TrasferimentoSenzaCollegamentiPlusXWalletTest}). Il controllo si rifà qui sulla mappa viva, non si usa il
 * conteggio del thread in background, che può essere di una passata precedente.</p>
 */
public final class Principale_TrasferimentiSenzaCollegamento {

    private Principale_TrasferimentiSenzaCollegamento() {
    }

    /** Quanti movimenti elencare al più nel dialogo: oltre, si dice solo quanti altri ce ne sono. */
    private static final int MAX_ELENCATI = 15;

    /**
     * Annulla la classificazione dei movimenti indicati, uno alla volta e <b>tutto o niente</b> ciascuno ({@link
     * Principale_DepositiPrelievi.Esito#atomico}): un errore su uno lo lascia com'era, annota il problema e prosegue con gli altri.
     *
     * @param ids gli ID dei movimenti
     * @param esito dove annotare i problemi
     * @return quanti movimenti sono tornati non classificati
     */
    static int AnnullaClassificazione(List<String> ids, Principale_DepositiPrelievi.Esito esito) {
        int fatti = 0;
        for (String id : ids) {
            if (MappaCryptoWallet.get(id) == null) continue;
            if (esito.atomico("Annullamento della classificazione di " + id, () -> {
                GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(new String[] {id}, id);
                return true;
            }, false)) {
                fatti++;
            }
        }
        return fatti;
    }

    /**
     * Mostra l'elenco e, se scelto, annulla la classificazione.
     *
     * @param owner finestra su cui centrare i dialoghi
     * @return {@code true} se almeno un movimento è tornato non classificato: il chiamante deve ricalcolare tutto una volta
     */
    public static boolean Gestisci(Window owner) {
        MovimentiCollegati.Esito E = MovimentiCollegati.Controlla(new ArrayList<>(MappaCryptoWallet.values()));
        List<String> ids = new ArrayList<>(E.SenzaCollegamento);
        if (ids.isEmpty()) {
            Messaggi.InfoMessage("Trasferimenti senza collegamento", "Nessun trasferimento senza collegamento.", owner);
            return false;
        }
        boolean plusPerWallet = "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet"));
        String effetto = plusPerWallet
                ? "Le plusvalenze sono divise per gruppo wallet: se i due wallet del trasferimento sono in gruppi diversi, il costo di "
                  + "carico non passa dal gruppo che invia a quello che riceve. La vendita del gruppo di arrivo può non trovare il lotto "
                  + "(parte del LIFO mancante) e il lotto resta nella pila del gruppo di partenza."
                : "Le plusvalenze per wallet sono spente, quindi oggi il calcolo non cambia. Se si attivano, il costo di carico non passerà "
                  + "da un gruppo all'altro.";
        String dettagli = "Movimenti classificati come trasferimento fra wallet ma senza il movimento della controparte collegato.<br>"
                + effetto + "<br><br>"
                + "Annullando la classificazione i movimenti tornano non classificati e riappaiono in <i>Depositi/Prelievi</i>, dove "
                + "si possono abbinare di nuovo.<br>" + Elenco(ids);

        AppDialog.DialogResult R = AppDialog.builder(owner)
                .windowTitle("Trasferimenti senza collegamento")
                .bodyTitle("Trasferimenti senza collegamento: " + ids.size())
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Alcuni trasferimenti fra wallet non sono collegati alla loro controparte.")
                .details(dettagli)
                .action(AppDialog.DialogAction.builder("cancel", "Chiudi")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("annulla", "Annulla la classificazione (" + ids.size() + ")")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        if (R == null || !R.isAction("annulla")) return false;

        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();
        int fatti = AnnullaClassificazione(ids, esito);
        StringBuilder testo = new StringBuilder("Classificazione annullata su <b>" + fatti + "</b> movimenti. Ora sono nella sezione "
                + "<i>Depositi/Prelievi</i>, da dove si possono abbinare di nuovo.");
        if (fatti > 0) {
            testo.append("<br><br>Premi <b>Salva</b> nella sezione 'Transazioni Crypto' per rendere permanenti le modifiche.");
        }
        if (!esito.problemi.isEmpty()) {
            testo.append("<br><br>").append(esito.testoProblemi());
        }
        Messaggi.InfoMessage("Trasferimenti senza collegamento", testo.toString(), owner);
        return fatti > 0;
    }

    /** Elenco HTML dei movimenti, al più {@link #MAX_ELENCATI}: data, wallet, moneta, quantità e classificazione. */
    private static String Elenco(List<String> ids) {
        StringBuilder S = new StringBuilder();
        for (int i = 0; i < ids.size() && i < MAX_ELENCATI; i++) {
            String[] v = MappaCryptoWallet.get(ids.get(i));
            if (v == null) continue;
            boolean uscita = v[8] != null && !v[8].isBlank();
            S.append(" - ").append(v[1]).append(" ").append(v[3]).append(" ")
                    .append(uscita ? v[8] + " " + v[10] : v[11] + " " + v[13]).append(" (").append(v[18]).append(")<br>");
        }
        if (ids.size() > MAX_ELENCATI) S.append(" ... e altri ").append(ids.size() - MAX_ELENCATI).append("<br>");
        return S.toString();
    }
}
