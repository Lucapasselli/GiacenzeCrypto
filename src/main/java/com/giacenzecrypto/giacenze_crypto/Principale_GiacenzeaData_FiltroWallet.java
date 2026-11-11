package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Logica dei tre selettori di "Giacenze a data" (gruppo wallet, wallet, sotto-wallet), senza Swing.
 * <p>
 * Il resto del programma (calcolo, chiave dei filtri, costi netti) continua a ricevere una sola stringa, quella che
 * prima usciva dall'unica combo dei wallet: {@code "Tutti"}, il nome di un wallet oppure {@code "Gruppo : X ( alias )"}.
 * {@link #Effettivo} la ricostruisce dalle due combo, così la forma dei filtri non cambia.
 */
public final class Principale_GiacenzeaData_FiltroWallet {

    /** Voce "nessun filtro" comune alle tre combo. */
    public static final String TUTTI = "Tutti";

    /** Prefisso che il calcolo si aspetta davanti al gruppo. */
    public static final String PREFISSO_GRUPPO = "Gruppo : ";

    private Principale_GiacenzeaData_FiltroWallet() {
    }

    /** @return true se la voce è vuota o è "Tutti" */
    public static boolean IsTutti(String voce) {
        return voce == null || voce.isBlank() || voce.trim().equalsIgnoreCase(TUTTI);
    }

    /** Voce della combo dei gruppi: {@code "Wallet 1 ( alias )"}, senza il prefisso {@code "Gruppo : "}. */
    public static String VoceGruppo(String id, String alias) {
        return id + " ( " + alias + " )";
    }

    /**
     * Identificativo del gruppo contenuto in una voce della combo o in una stringa del calcolo.
     * @param voce {@code "Wallet 1 ( alias )"}, {@code "Gruppo : Wallet 1 ( alias )"} o {@code "Wallet 1"}
     * @return {@code "Wallet 1"}
     */
    public static String IdGruppo(String voce) {
        if (voce == null) {
            return "";
        }
        String v = voce.trim();
        if (v.startsWith(PREFISSO_GRUPPO)) {
            v = v.substring(PREFISSO_GRUPPO.length());
        }
        int p = v.indexOf('(');
        return (p >= 0 ? v.substring(0, p) : v).trim();
    }

    /**
     * La stringa unica che il calcolo si aspetta: il wallet se scelto, altrimenti il gruppo, altrimenti "Tutti".
     * @param wallet voce della combo dei wallet
     * @param gruppo voce della combo dei gruppi
     */
    public static String Effettivo(String wallet, String gruppo) {
        if (!IsTutti(wallet)) {
            return wallet;
        }
        if (!IsTutti(gruppo)) {
            return PREFISSO_GRUPPO + gruppo;
        }
        return TUTTI;
    }

    /**
     * I wallet da proporre nella combo, in ordine, preceduti da "Tutti".
     * @param wallets tutti i wallet noti
     * @param gruppo voce o identificativo del gruppo scelto, "Tutti" per non restringere
     * @param gruppoDi gruppo (identificativo) a cui appartiene un wallet
     */
    public static List<String> WalletDelGruppo(Collection<String> wallets, String gruppo, Function<String, String> gruppoDi) {
        List<String> r = new ArrayList<>();
        r.add(TUTTI);
        boolean tutti = IsTutti(gruppo);
        String id = IdGruppo(gruppo);
        for (String w : wallets) {
            if (tutti || id.equalsIgnoreCase(gruppoDi.apply(w))) {
                r.add(w);
            }
        }
        return r;
    }

    /**
     * Cerca nella lista di voci (di una combo) quella che corrisponde al gruppo indicato.
     * @return la voce, oppure {@code null} se quel gruppo non è tra le voci
     */
    public static String TrovaVoceGruppo(Collection<String> voci, String gruppo) {
        String id = IdGruppo(gruppo);
        for (String v : voci) {
            if (!IsTutti(v) && IdGruppo(v).equalsIgnoreCase(id)) {
                return v;
            }
        }
        return null;
    }

    private static final String ETICHETTA_INIZIO = "<html>Wallet <b>";
    private static final String ETICHETTA_FINE = "</b> non più disponibile</html>";

    /** Voce che una combo mostra per un wallet che non esiste più (è la stessa di {@code Funzione_RiposizionaComboBox}). */
    public static String EtichettaWalletMancante(String wallet) {
        return ETICHETTA_INIZIO + wallet + ETICHETTA_FINE;
    }

    /** Il nome del wallet, anche se la voce è l'etichetta di {@link #EtichettaWalletMancante}. */
    public static String NomeWallet(String voce) {
        if (voce != null && voce.startsWith(ETICHETTA_INIZIO) && voce.endsWith(ETICHETTA_FINE)) {
            return voce.substring(ETICHETTA_INIZIO.length(), voce.length() - ETICHETTA_FINE.length());
        }
        return voce;
    }
}
