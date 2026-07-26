/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.TransazioneDefi.ValoriToken;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Raccolta delle eccezioni note delle importazioni DeFi, ovvero dei casi in cui quanto registrato
 * sulla blockchain (e quindi restituito dagli explorer) non corrisponde a quanto realmente movimentato
 * sul wallet e va corretto prima di generare i movimenti.
 * <p>
 * La classe non ha stato e non modifica nulla: espone metodi che, data una transazione, restituiscono
 * l'elenco delle correzioni da applicare (per ora i soli indirizzi di token da eliminare); è
 * {@link TransazioneDefi} ad applicarle materialmente.
 * <p>
 * Per aggiungere una nuova eccezione dello stesso tipo (token migrato su nuovo contratto) è sufficiente
 * aggiungere una riga a {@link #TOKEN_MIGRATI}.
 *
 * @author Luca
 */
public class EccezioniDefi {

    /** Lato della transazione a cui applicare l'eccezione. */
    public enum Direzione {
        /** Solo movimenti in entrata (quantità positiva). */
        ENTRATA,
        /** Solo movimenti in uscita (quantità negativa). */
        USCITA,
        /** Sia movimenti in entrata sia movimenti in uscita. */
        ENTRAMBE
    }

    /**
     * Token che ha cambiato indirizzo di contratto: alcune piattaforme non aggiornate continuano a
     * movimentare anche il vecchio contratto insieme al nuovo, per cui l'explorer registra due movimenti
     * identici quando in realtà è stato movimentato solo il token nuovo.
     *
     * @param Rete rete su cui vale l'eccezione (confronto case-insensitive, es. {@code GNOSIS})
     * @param AddressVecchio indirizzo del contratto dismesso, da eliminare quando è un doppione
     * @param AddressNuovo indirizzo del contratto attuale, che resta l'unico movimento valido
     * @param Lato lato della transazione a cui applicare la correzione
     * @param Descrizione descrizione dell'eccezione, usata nei log
     */
    public record TokenMigrato(String Rete, String AddressVecchio, String AddressNuovo,
                               Direzione Lato, String Descrizione) {}

    /** Elenco dei token migrati su un nuovo contratto per cui esistono doppioni sugli explorer. */
    private static final List<TokenMigrato> TOKEN_MIGRATI = List.of(
            new TokenMigrato("GNOSIS",
                    "0xcB444e90D8198415266c6a2724b7900fb12FC56E",
                    "0x420CA0f9B9b604cE0fd9C18EF134C705e5Fa3430",
                    Direzione.ENTRAMBE,
                    "EURe: vecchio contratto movimentato insieme al nuovo da piattaforme non aggiornate")
    );

    /** Classe di sola utilità: non va istanziata. */
    private EccezioniDefi() {
    }

    /**
     * Individua i token da eliminare da una transazione DeFi in base alle eccezioni note.
     * <p>
     * Per ogni {@link TokenMigrato} previsto sulla rete indicata, se nella stessa transazione compaiono
     * sia il contratto vecchio sia quello nuovo con la stessa quantità (stesso valore e stesso segno,
     * confrontati come {@link BigDecimal}) e nella direzione prevista dall'eccezione, il movimento del
     * contratto vecchio viene segnalato come da eliminare perché è un doppione mai realmente transitato
     * dal wallet.
     *
     * @param Rete rete della transazione (può essere {@code null})
     * @param MappaToken mappa indirizzo → token della transazione, così come popolata da
     *                   {@link TransazioneDefi#InserisciMonete} (può essere {@code null} o vuota)
     * @return la lista degli indirizzi di contratto da rimuovere dalla transazione (vuota se non ci sono
     *         eccezioni da applicare)
     */
    public static List<String> IndirizziDaEliminare(String Rete, Map<String, ValoriToken> MappaToken) {
        List<String> daEliminare = new ArrayList<>();
        if (Rete == null || MappaToken == null || MappaToken.isEmpty()) return daEliminare;

        for (TokenMigrato eccezione : TOKEN_MIGRATI) {
            if (!Rete.trim().equalsIgnoreCase(eccezione.Rete())) continue;
            //La mappa dei token è case-insensitive, quindi l'indirizzo viene trovato
            //indipendentemente dal maiuscolo/minuscolo del checksum
            ValoriToken Vecchio = MappaToken.get(eccezione.AddressVecchio());
            ValoriToken Nuovo = MappaToken.get(eccezione.AddressNuovo());
            if (Vecchio == null || Nuovo == null) continue;
            if (!StessaQuantita(Vecchio.Qta, Nuovo.Qta)) continue;
            if (!DirezioneCompatibile(Vecchio.Qta, eccezione.Lato())) continue;

            daEliminare.add(Vecchio.MonetaAddress != null ? Vecchio.MonetaAddress : eccezione.AddressVecchio());
            LoggerGC.logInfo("Eccezione DeFi applicata (" + eccezione.Descrizione() + "): eliminato il movimento di "
                    + Vecchio.Moneta + " (" + eccezione.AddressVecchio() + ") su rete " + eccezione.Rete()
                    + " perché doppione di " + eccezione.AddressNuovo() + " per la quantità " + Nuovo.Qta);
        }
        return daEliminare;
    }

    /**
     * Confronta due quantità come numeri (e non come stringhe, dato che possono differire per zeri non
     * significativi), verificando quindi implicitamente anche l'uguaglianza del segno.
     * @param Qta1 prima quantità
     * @param Qta2 seconda quantità
     * @return {@code true} se le due quantità sono numericamente uguali
     */
    private static boolean StessaQuantita(String Qta1, String Qta2) {
        if (Qta1 == null || Qta2 == null) return false;
        try {
            return new BigDecimal(Qta1.trim()).compareTo(new BigDecimal(Qta2.trim())) == 0;
        } catch (NumberFormatException e) {
            LoggerGC.ScriviErrore(e);
            return false;
        }
    }

    /**
     * Verifica che il movimento sia sul lato (entrata/uscita) previsto dall'eccezione.
     * @param Qta quantità del movimento (negativa se in uscita)
     * @param Lato direzione prevista dall'eccezione
     * @return {@code true} se l'eccezione è applicabile a questo movimento
     */
    private static boolean DirezioneCompatibile(String Qta, Direzione Lato) {
        if (Lato == Direzione.ENTRAMBE) return true;
        boolean InUscita = Qta.trim().startsWith("-");
        return Lato == Direzione.USCITA ? InUscita : !InUscita;
    }
}
