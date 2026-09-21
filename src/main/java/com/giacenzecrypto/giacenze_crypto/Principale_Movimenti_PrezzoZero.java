package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica operativa della voce di menu contestuale "Conferma che il token non ha prezzo (valorizza a
 * Zero)": su uno o più movimenti privi di prezzo, conferma a mano che il controvalore è davvero zero.
 *
 * <p><b>Non è il pulsante "Conferma prezzo ZERO" di "Giacenze a data".</b> Quello lavora sul <i>token</i>
 * a una data (scrive un prezzo personalizzato {@code 0.00} in {@code PrezziNew}) e non tocca nessun
 * movimento, che resta fra gli errori "senza prezzo". Questa operazione lavora sul <i>movimento</i>, con
 * lo stesso effetto della conferma a zero di {@code Funzioni.GUIModificaPrezzo}: {@code [15]="0.00"} e
 * {@code [32]="SI"}. Volutamente <b>non</b> scrive nulla in {@code PrezziNew}: quella tabella contiene i
 * prezzi di fine anno inseriti a mano su cui si costruisce il quadro RW, e uno zero per-movimento vi
 * finirebbe come prezzo di quel token a quell'istante.</p>
 *
 * <p>{@code [32]="SI"} è la prima uscita di {@link Prezzi#isMovimentoPrezzato}, quindi la conferma non
 * viene rimessa in discussione ai caricamenti successivi. {@code [40]} riceve la fonte "Personalizzato"
 * come nel percorso "Modifica Prezzo", così anche "Trasla Orario" non ricalcola il prezzo confermato.</p>
 *
 * @author lucap
 */
public class Principale_Movimenti_PrezzoZero {

    private Principale_Movimenti_PrezzoZero() {
    }

    /**
     * Dice se un movimento è "senza prezzo" nello stesso senso in cui lo conta il pulsante Errori e il
     * filtro relativo (esito negativo di {@link Prezzi#isMovimentoPrezzato}, che considera prezzati anche i
     * token SCAM).
     *
     * <p>Chiama la funzione su una <b>copia</b> della riga: sul campo {@code [32]} vuoto quella funzione
     * va a cercare un prezzo in rete, cosa che non deve mai succedere mentre si sta aprendo un menu. Il
     * campo vuoto (movimento non ancora valutato dal caricamento tabella) viene quindi escluso a monte, e
     * sulla copia la funzione non ha effetti collaterali.</p>
     *
     * @param Movimento riga del movimento, anche {@code null}
     * @return {@code true} se il movimento è stato valutato e risulta privo di prezzo
     */
    public static boolean isSenzaPrezzo(String[] Movimento) {
        if (Movimento == null || Movimento.length <= 40) return false;
        if (Movimento[32] == null || Movimento[32].isBlank()) return false;
        return !Prezzi.isMovimentoPrezzato(Movimento.clone());
    }

    /**
     * Abilitazione della voce di menu: la selezione non è vuota e <b>tutti</b> i suoi movimenti sono
     * privi di prezzo. Basta uno solo già prezzato per disabilitarla.
     * @param IDs ID dei movimenti selezionati
     * @return {@code true} se la conferma a zero è applicabile a tutta la selezione
     */
    public static boolean isConfermabile(List<String> IDs) {
        if (IDs == null || IDs.isEmpty()) return false;
        for (String ID : IDs) {
            if (!isSenzaPrezzo(ID == null ? null : MappaCryptoWallet.get(ID))) return false;
        }
        return true;
    }

    /**
     * Chiede conferma e valorizza a zero i movimenti indicati.
     *
     * <p>Rilegge la condizione su ogni movimento anche qui (la lista arriva dal popup e potrebbe essere
     * datata): quelli nel frattempo prezzati o spariti vengono scartati, e se non ne resta nessuno non si
     * chiede niente. Ogni movimento modificato lascia una voce nello storico modifiche, come
     * {@code GUI_ModificaMovimento} per le modifiche sul posto: il lignaggio {@code [42]} si timbra
     * <b>prima</b> di toccare la riga, così la versione salvata è quella "come era prima".</p>
     *
     * <p>Il ricalcolo delle tabelle resta al chiamante, una sola volta per tutta la selezione.</p>
     *
     * @param IDs   ID dei movimenti selezionati
     * @param owner finestra proprietaria del dialogo di conferma
     * @return {@code true} se almeno un movimento è stato modificato
     */
    public static boolean ConfermaPrezzoZero(List<String> IDs, Window owner) {
        if (IDs == null || IDs.isEmpty()) return false;

        List<String[]> DaConfermare = new ArrayList<>();
        for (String ID : IDs) {
            String Movimento[] = ID == null ? null : MappaCryptoWallet.get(ID);
            if (isSenzaPrezzo(Movimento)) DaConfermare.add(Movimento);
        }
        if (DaConfermare.isEmpty()) return false;

        if (!Messaggi.ConfermaMovimentiSenzaPrezzo(DaConfermare.size(), owner)) return false;

        for (String[] Movimento : DaConfermare) {
            String Lignaggio = MovimentiStorico.AssicuraLignaggio(Movimento);
            MovimentiStorico.AccodaModifica(Lignaggio, Movimento[0], Movimento[0],
                    Importazioni.SerializzaRiga(Movimento), MovimentiStorico.OP_IN_PLACE);
            Movimento[15] = "0.00";
            Movimento[32] = "SI";
            Movimento[40] = "|||Personalizzato";
        }
        return true;
    }
}
