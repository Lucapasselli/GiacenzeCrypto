package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * La voce "Movimenti collegati incoerenti" del pulsante Errori: mostra cosa ha trovato
 * {@link MovimentiCollegati#Controlla} e, se l'utente lo chiede, ricostruisce gli scambi differiti sovrascritti.
 * Il controllo si rifà qui sulla mappa viva, non si usa il conteggio del thread in background, che può essere di
 * una passata precedente.
 */
public final class Principale_MovimentiCollegati {

    private Principale_MovimentiCollegati() {
    }

    /** Quanti ID elencare al più nel dialogo per ogni gruppo: oltre, si dice solo quanti altri ce ne sono. */
    private static final int MAX_ELENCATI = 15;

    /**
     * Mostra il dialogo e, se scelto, ripara.
     * @param owner finestra su cui centrare i dialoghi
     * @return {@code true} se almeno uno scambio è stato ricostruito: il chiamante deve ricalcolare tutto una volta
     */
    public static boolean Gestisci(Window owner) {
        MovimentiCollegati.Esito E = MovimentiCollegati.Controlla(new ArrayList<>(MappaCryptoWallet.values()));
        if (E.Totale() == 0) {
            Messaggi.InfoMessage("Movimenti collegati", "Nessun movimento collegato incoerente.", owner);
            return false;
        }

        StringBuilder Dettagli = new StringBuilder();
        if (!E.ScambiSovrascritti.isEmpty()) {
            Dettagli.append("<b>Scambi differiti sovrascritti: ").append(E.ScambiSovrascritti.size()).append("</b><br>")
                    .append("Due scambi differiti con il deposito nello stesso secondo avevano ricevuto gli stessi ")
                    .append("identificativi, e il secondo aveva cancellato lo scambio del primo (difetto delle versioni ")
                    .append("precedenti). Al prelievo rimasto senza scambio manca una permuta, e la piattaforma di scambio ")
                    .append("mostra un saldo che non esiste.<br>")
                    .append("Si possono ricostruire: l'altro scambio non viene toccato. Ricostruire aggiunge la permuta ")
                    .append("mancante, quindi possono cambiare le plusvalenze di quell'anno, i costi delle cessioni ")
                    .append("successive e le giacenze di fine anno (quadro W/RW), anche in anni già dichiarati.<br>")
                    .append(Elenco(new ArrayList<>(E.ScambiSovrascritti), true)).append("<br>");
        }
        if (!E.DualFormaPrecedente.isEmpty()) {
            Dettagli.append("<b>Contratti Binance Dual Investment da aggiornare: ").append(E.DualFormaPrecedente.size())
                    .append("</b><br>")
                    .append("Sono stati abbinati da una versione precedente. Per aggiornarli ripassa il file di dettaglio ")
                    .append("Dual Investment da <i>Importazioni</i>, voce <i>Binance - Dettaglio Dual Investment</i>: ")
                    .append("non cambia nessun calcolo. Fino ad allora non annullare la classificazione dal Settlement, ")
                    .append("perché la reward andrebbe persa.<br>")
                    .append(Elenco(new ArrayList<>(E.DualFormaPrecedente), false)).append("<br>");
        }
        if (!E.Altri.isEmpty()) {
            Dettagli.append("<b>Altri movimenti con collegamenti incoerenti: ").append(E.Altri.size()).append("</b><br>")
                    .append("Citano un movimento che non esiste più, o che non li cita a sua volta. Vanno sistemati a ")
                    .append("mano: apri il movimento con <i>Modifica movimento</i>, che annulla la classificazione, e ")
                    .append("classificalo di nuovo.<br>")
                    .append(Elenco(new ArrayList<>(E.Altri), false));
        }

        AppDialog.Builder B = AppDialog.builder(owner)
                .windowTitle("Movimenti collegati incoerenti")
                .bodyTitle("Movimenti collegati incoerenti: " + E.Totale())
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Alcuni movimenti classificati insieme non sono più collegati come dovrebbero.")
                .details(Dettagli.toString())
                .action(AppDialog.DialogAction.builder("cancel", "Chiudi")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build());
        if (!E.ScambiSovrascritti.isEmpty()) {
            B.action(AppDialog.DialogAction.builder("ripara", "Ricostruisci gli scambi (" + E.ScambiSovrascritti.size() + ")")
                    .role(AppDialog.ActionRole.PRIMARY)
                    .build());
        }
        AppDialog.DialogResult R = B.showDialog();
        if (R == null || !R.isAction("ripara")) return false;

        List<MovimentiCollegati.Riparazione> Fatte = MovimentiCollegati.RiparaScambiSovrascritti(E.ScambiSovrascritti);
        StringBuilder Riparati = new StringBuilder(), Falliti = new StringBuilder();
        int NRiparati = 0;
        for (MovimentiCollegati.Riparazione r : Fatte) {
            if (r.Esito == MovimentiCollegati.EsitoRiparazione.RIPARATO) {
                NRiparati++;
                Riparati.append("<br> - ").append(r.Descrizione);
            } else if (r.Esito == MovimentiCollegati.EsitoRiparazione.FALLITO) {
                Falliti.append("<br> - ").append(r.Descrizione).append(" (").append(r.Prelievo).append(")");
            }
        }
        StringBuilder Testo = new StringBuilder("Scambi ricostruiti: <b>").append(NRiparati).append("</b>").append(Riparati);
        if (Falliti.length() > 0) {
            Testo.append("<br><br>Non è stato possibile ricostruire, e sono rimasti com'erano (dettagli nel log):").append(Falliti);
        }
        if (NRiparati > 0) {
            Testo.append("<br><br>Premi <b>Salva</b> nella sezione 'Transazioni Crypto' per rendere permanenti le modifiche.");
        }
        Messaggi.InfoMessage("Scambi differiti", Testo.toString(), owner);
        return NRiparati > 0;
    }

    /** Elenco HTML degli ID, al più {@link #MAX_ELENCATI}; per un prelievo anche le monete e la data del deposito. */
    private static String Elenco(List<String> IDs, boolean Prelievi) {
        StringBuilder S = new StringBuilder();
        for (int i = 0; i < IDs.size() && i < MAX_ELENCATI; i++) {
            String[] v = MappaCryptoWallet.get(IDs.get(i));
            S.append(" - ");
            if (Prelievi && v != null) S.append(v[1]).append(" ").append(v[3]).append(" ").append(v[8]).append(" - ");
            S.append(IDs.get(i)).append("<br>");
        }
        if (IDs.size() > MAX_ELENCATI) S.append(" ... e altri ").append(IDs.size() - MAX_ELENCATI).append("<br>");
        return S.toString();
    }
}
