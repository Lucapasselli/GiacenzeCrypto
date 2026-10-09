package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Avviso del calcolo dei quadri W/RW e T/RT: nei movimenti ci sono token di moneta elettronica noti
 * ({@link EmoneyToken}) che la sezione E-Money dell'utente non contiene. Propone di aggiungerli, con la data
 * dell'elenco, oppure di non riproporli.
 *
 * <p>Si chiama solo dai pulsanti <i>Calcola</i> dei due quadri, non dai ricalcoli che partono da altre parti del
 * programma: lì l'avviso tornerebbe a ogni modifica. "Non ora" vale per la sessione, "Non proporli più" per i token
 * elencati in quel momento: un EMT aggiunto all'elenco dopo viene proposto lo stesso.
 *
 * <p>Segue lo schema delle altre estrazioni: nessun campo Swing, chi chiama passa la finestra e, se l'esito è
 * {@code true}, ricarica la tabella E-Money e ricalcola prima del quadro.
 */
public final class Principale_EmoneyMancanti {

    /** Opzione (personale.mv.db): token dell'elenco che l'utente non vuole più vedere proposti, separati da virgola. */
    static final String OPZIONE_NON_PROPORRE = "EmoneyToken_NonProporre";

    /** Token per cui in questa sessione l'utente ha risposto "Non ora". */
    private static final Set<String> RimandatiInSessione = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private Principale_EmoneyMancanti() {}

    /**
     * Controlla i movimenti e, se serve, mostra l'avviso.
     *
     * @param Owner finestra su cui centrare il dialogo
     * @return {@code true} se ha aggiunto token alla sezione E-Money: il chiamante deve ricalcolare
     */
    public static boolean Controlla(Window Owner) {
        try {
            EmoneyToken.Elenco elenco = EmoneyToken.Carica();
            if (elenco == null) return false;
            Map<String, Integer> mancanti = EmoneyToken.MancantiNeiMovimenti(Principale.MappaCryptoWallet, elenco);
            Set<String> esclusi = NonProporre();
            esclusi.addAll(RimandatiInSessione);
            mancanti.keySet().removeIf(esclusi::contains);
            if (mancanti.isEmpty()) return false;

            AppDialog.DialogResult result = AppDialog.builder(Owner)
                    .windowTitle("Token di moneta elettronica")
                    .bodyTitle("Token di moneta elettronica non nella sezione E-Money")
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("Nei movimenti ci sono token di moneta elettronica che non sono nella sezione E-Money.")
                    .details(Testo(mancanti, elenco.decorrenza()))
                    .action(AppDialog.DialogAction.builder("mai", "Non proporli più")
                            .role(AppDialog.ActionRole.NEUTRAL)
                            .build())
                    .action(AppDialog.DialogAction.builder("dopo", "Non ora")
                            .role(AppDialog.ActionRole.SECONDARY)
                            .build())
                    .action(AppDialog.DialogAction.builder("aggiungi", "Aggiungi alla sezione E-Money")
                            .role(AppDialog.ActionRole.PRIMARY)
                            .build())
                    .showDialog();

            if (result != null && result.isAction("aggiungi")) {
                for (String s : mancanti.keySet()) DatabaseH2.Pers_Emoney_Scrivi(s, elenco.decorrenza());
                return true;
            }
            if (result != null && result.isAction("mai")) {
                Set<String> nuovi = NonProporre();
                nuovi.addAll(mancanti.keySet());
                DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_NON_PROPORRE, String.join(",", nuovi));
            } else {
                RimandatiInSessione.addAll(mancanti.keySet());
            }
            return false;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    /** Testo del dialogo: i token con i loro movimenti e l'effetto dell'aggiunta, senza consigli. */
    static String Testo(Map<String, Integer> mancanti, String decorrenza) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : mancanti.entrySet()) {
            sb.append("<b>").append(e.getKey()).append("</b>: ").append(e.getValue())
                    .append(e.getValue() == 1 ? " movimento" : " movimenti").append("<br>");
        }
        String data = decorrenza.substring(8, 10) + "/" + decorrenza.substring(5, 7) + "/" + decorrenza.substring(0, 4);
        sb.append("<br>Finché non sono nella sezione E-Money il programma li tratta come cripto-attività.")
                .append("<br>Se li aggiungi sono trattati come E-Money dal ").append(data)
                .append(": cambiano i calcoli di tutti gli anni da quella data in poi, compresi quelli già dichiarati.")
                .append("<br><br>La sezione e le date si possono modificare in Opzioni, E-Money Token (EMT).");
        return sb.toString();
    }

    private static Set<String> NonProporre() {
        Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_NON_PROPORRE);
        if (v != null) {
            for (String t : v.split(",")) {
                if (!t.isBlank()) s.add(t.trim());
            }
        }
        return s;
    }
}
