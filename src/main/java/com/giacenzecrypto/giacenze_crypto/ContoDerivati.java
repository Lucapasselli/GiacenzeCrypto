package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Il conto derivati come derivato: l'opzione <b>P2</b> della decisione D15 ({@code nocommit/Documentazione/Decisioni_Derivati.md},
 * piano in {@code Analisi_P2_ContoDerivati.md}). Con P1 il margine resta una cripto-attività e nulla cambia. Con P2 (dal 24/11/2026 l'unico metodo valido, {@link #P1Disponibile}) il
 * sotto-wallet {@value OKXDerivati#SOTTOWALLET} di ogni exchange ha una pila LIFO sua, e:
 * <ul>
 *   <li>il giroconto del margine (righe marcate {@link Derivati#MARGINE}) dallo spot al sotto-wallet è una <b>cessione</b> di
 *       cripto-attività al valore del momento, e la moneta entra nella pila del sotto-wallet a quel valore;</li>
 *   <li>il giroconto dal sotto-wallet allo spot scarica la pila: la differenza fra valore e costo è un reddito da derivati
 *       ({@code [46]}), e la moneta rientra nello spot al valore del momento;</li>
 *   <li>un accredito da derivati sul sotto-wallet entra nella pila (solo derivati), un addebito ne esce con un onere pari al costo
 *       dei lotti scaricati, senza plusvalenza cripto;</li>
 *   <li>un movimento su derivati ancora da classificare che sta sul sotto-wallet conta come accredito o addebito
 *       ({@link #DaClassificareSulConto});</li>
 *   <li>profitti, perdite, commissioni e funding sullo spot restano con il doppio effetto (D10): per OKX dal 22/11/2026 solo quelli
 *       senza il margine della posizione, gli altri stanno sul sotto-wallet ({@link OKXDerivati}, forma "conto").</li>
 * </ul>
 * Logica senza Swing. Le regole sono applicate dal motore ({@code Calcoli_PlusvalenzeNew.ElaboraMovimento}).
 */
public final class ContoDerivati {

    private ContoDerivati() {
    }

    /** Opzione personale: {@code SI} = P2 (il conto derivati conta come derivato), altrimenti P1. */
    public static final String OPZIONE = "Derivati_ContoRilevante";

    /** Valore di {@link #OPZIONE} quando l'utente non l'ha mai salvata: P2 (scelta dell'utente, 24/11/2026). */
    public static final String OPZIONE_PREDEFINITA = "SI";

    /** Suffisso della chiave della pila del sotto-wallet nella mappa delle pile del motore. */
    static final String SUFFISSO_PILA = "|" + OKXDerivati.SOTTOWALLET;

    /**
     * Dal 24/11/2026 P2 è l'unico metodo valido (scelta dell'utente: i problemi del quadro W/RW con P1): l'opzione non si vede più e
     * {@link #Attivo()} dà sempre {@code true}. Il codice di P1 resta, a {@code true} {@link #Attivo()} torna a leggere l'opzione
     * (i test che provano P1 lo impostano). Per riaprire P1 all'utente basta renderlo {@code true} e mostrare la casella.
     */
    static boolean P1Disponibile = false;

    /** @return {@code true} con P2: sempre, finché {@link #P1Disponibile} è {@code false} */
    public static boolean Attivo() {
        if (!P1Disponibile) return true;
        return "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE, OPZIONE_PREDEFINITA));
    }

    /**
     * Suffisso della chiave interna con cui una moneta del conto derivati si distingue da quella spot nelle mappe che hanno il simbolo
     * come chiave (quadro RT, righe di "Giacenze a data"). E' solo una chiave: non si salva in nessun movimento, e il simbolo vero
     * (prezzi, alias, SCAM, scritture) si ricava con {@link #SimboloDaChiave}.
     */
    public static final String SUFFISSO_CHIAVE = ".DERIVATI";

    /**
     * Il movimento ha una riga a parte, quella del conto derivati, nelle tabelle per simbolo: sta sul conto ({@link #SulContoP2}) e P2 e'
     * il metodo in uso (con P1, ora solo nei test, il conto e' un sotto-wallet qualunque e le pile sono quelle di sempre).
     */
    public static boolean RigaSulConto(String[] v, java.util.Map<String, String[]> mappa) {
        return Attivo() && SulContoP2(v, mappa);
    }

    /** @return la chiave della moneta: il simbolo, col suffisso se il movimento sta sul conto derivati ({@link #SulContoP2}) */
    public static String ChiaveMoneta(String moneta, boolean sulConto) {
        //Il token di un contratto (DERIV.*) e' gia' un derivato: nessun suffisso
        return sulConto && moneta != null && !moneta.isBlank() && !Derivati.IsTokenContratto(moneta) ? moneta + SUFFISSO_CHIAVE : moneta;
    }

    /** @return {@code true} se la chiave e' quella di una moneta del conto derivati */
    public static boolean IsChiaveConto(String chiave) {
        return chiave != null && chiave.endsWith(SUFFISSO_CHIAVE);
    }

    /** @return il simbolo vero di una chiave (senza il suffisso del conto) */
    public static String SimboloDaChiave(String chiave) {
        return IsChiaveConto(chiave) ? chiave.substring(0, chiave.length() - SUFFISSO_CHIAVE.length()) : chiave;
    }

    /** @return {@code true} se il sotto-wallet è quello dei derivati */
    public static boolean IsSottoWallet(String sottoWallet) {
        return sottoWallet != null && OKXDerivati.SOTTOWALLET.equalsIgnoreCase(sottoWallet.trim());
    }

    /** @return {@code true} se il movimento sta sul sotto-wallet dei derivati */
    public static boolean SulConto(String[] v) {
        return v != null && v.length > 4 && IsSottoWallet(v[4]);
    }

    /** Chiave della pila del sotto-wallet dei derivati del gruppo, indipendente dalle plusvalenze per wallet. */
    static String ChiavePila(String gruppoWallet) {
        return gruppoWallet + SUFFISSO_PILA;
    }

    /** @return {@code true} se il movimento è una gamba di un giroconto fra lo spot e il conto derivati */
    public static boolean IsGiroconto(String[] v) {
        return Derivati.Tipo(v).equals(Derivati.MARGINE);
    }

    /**
     * Con P2, il movimento sta sul conto derivati: il sotto-wallet dei derivati, o il collaterale di un rientro del margine abbinato
     * (posizioni aperte con la 1.0.66, {@link Principale_RientroMargine}).
     */
    static boolean SulContoP2(String[] v, java.util.Map<String, String[]> mappa) {
        if (SulConto(v)) return true;
        return v != null && v.length > 4 && Principale_RientroMargine.WALLET_COLLATERALE.equals(v[4])
                && Principale_RientroMargine.IsRigaAbbinamento(v, mappa);
    }

    /** Con P2, il movimento e' una gamba di un giroconto fra spot e conto derivati: righe del margine o blocco e sblocco di un rientro. */
    static boolean IsGirocontoP2(String[] v, java.util.Map<String, String[]> mappa) {
        return IsGiroconto(v) || Principale_RientroMargine.IsGirocontoAbbinamento(v, mappa);
    }

    /**
     * Con P2, un movimento marcato come derivato e ancora da classificare (campo 18 vuoto) che sta sul conto derivati conta come se
     * fosse classificato: accredito se entra, addebito se esce (decisione dell'utente del 21/11/2026). Il conto e' parte del derivato,
     * quindi quello che vi entra o esce per un risultato non e' una cripto-attivita'. Solo i tipi di risultato: non il margine, i
     * token del contratto o i Dual Investment. Lo applica il motore ({@code Calcoli_PlusvalenzeNew.ElaboraMovimento}), e
     * {@link Derivati#Conta} non lo conta fra i derivati trattati come cripto-attivita'.
     */
    static boolean DaClassificareSulConto(String[] v, java.util.Map<String, String[]> mappa) {
        if (v == null || v.length <= Derivati.CAMPO || v[18] == null || !v[18].isBlank()) return false;
        if (!Derivati.isDerivato(v) || Derivati.IsRigaContrattoOMargine(v) || Derivati.IsDual(v)) return false;
        return SulContoP2(v, mappa);
    }

    /**
     * Come {@link #DaClassificareSulConto}, con la stessa condizione del motore: una commissione conta sempre, un deposito o un prelievo
     * solo se i movimenti non classificati entrano nel calcolo (opzione {@code PL_CosiderareMovimentiNC}). Per chi deve dire se il
     * movimento e' calcolato come derivato.
     */
    static boolean CalcolatoComeDerivato(String[] v, java.util.Map<String, String[]> mappa) {
        if (!DaClassificareSulConto(v, mappa)) return false;
        String[] id = v[0].split("_");
        if (id.length > 4 && id[4].equalsIgnoreCase("CM")) return true;
        return !"NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC", "SI"));
    }

    /** Voce delle opzioni archiviate con una dichiarazione. */
    public static final String NOME_OPZIONE = "Conto derivati";
    public static final String DESCRIZIONE_OPZIONE = "parte del derivato: il passaggio dallo spot al conto è una cessione, il rientro un reddito da derivati";

    /**
     * Con P2, l'archivio ha movimenti del conto derivati (giroconti o righe sul conto) fino all'anno compreso: il loro effetto sui costi
     * passa agli anni dopo. Decide se l'opzione entra fra quelle archiviate con una dichiarazione dell'anno.
     */
    public static boolean InfluisceFinoAllAnno(String anno) {
        if (anno == null || anno.isBlank() || !Attivo() || Principale.MappaCryptoWallet == null) return false;
        String fine = anno.trim() + "-12-31 23:59";
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] == null || v[1].compareTo(fine) > 0) continue;
            if (IsGirocontoP2(v, Principale.MappaCryptoWallet) || SulContoP2(v, Principale.MappaCryptoWallet)) return true;
        }
        return false;
    }

    /** Differenza valore - costo in euro, due decimali, come ogni {@code [46]}. */
    static String Differenza(BigDecimal valore, BigDecimal costo) {
        return valore.subtract(costo).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
