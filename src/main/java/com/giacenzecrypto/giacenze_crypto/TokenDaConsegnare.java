package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;

/**
 * Il token di possesso {@code X.DA_CONSEGNARE} (per esempio {@code ETH.DA_CONSEGNARE}) di uno scambio differito con
 * il deposito <b>prima</b> del prelievo (OTC visto da chi incassa e poi spedisce): la moneta già ceduta con lo scambio,
 * la cui plusvalenza è realizzata alla data del deposito, ma che fisicamente resta nel wallet fino al prelievo.
 * Lo crea la classificazione ({@link GUI_ClassificazioneMovimento#CreaMovimentiScambioCryptoDifferito}, regola in
 * {@code UsaTokenDaConsegnare}): {@code MT1} lo fa entrare al deposito, il prelievo originale lo fa uscire.
 *
 * <p><b>Questa classe è l'unico posto che conosce le sue eccezioni.</b> Il token <b>non ha lotti LIFO</b> e non porta
 * né costo né plusvalenza, quindi chi legge le pile per mostrare un costo o una latente lo tratta in modo diverso, e lo
 * fa chiamando da qui. Chi aggiunge un nuovo lettore di pile (costo di carico, plusvalenza latente) deve farlo passare
 * da {@link #CostoDiCarico}, o la sua latente mostrerebbe come guadagno tutto il valore.
 *
 * <p>I punti in cui vale:
 * <ul>
 * <li><b>Prezzo</b>: si prezza come la moneta di cui è la copia, prima di alias e prezzi personalizzati
 * ({@link #Base}, in {@code Prezzi.DammiPrezzoInfoTransazione});</li>
 * <li><b>Quadro RT</b> ({@code Calcoli_RT.ChiudiAnno}): costo = valore di fine anno, latente zero ({@link #CostoDiCarico});</li>
 * <li><b>Giacenze a data</b>, tabella principale ({@link #CostoRigaGiacenze}) e dettaglio del token
 * ({@link #AdattaColonneDettaglio}): costo = valore, differenza zero;</li>
 * <li><b>Quadro W/RW</b> metodi B, C, D ({@code Calcoli_RW.AggiornaRWFR}): lotto che si apre a {@code MT1}
 * ({@link #IsIngressoRW}) e si chiude al prelievo ({@link #IsConsegnaRW}). La Rilevanza A legge le quantità e non fa eccezioni;</li>
 * <li><b>Annullamento e reimport</b>: il prelievo torna alla sua moneta ({@link #Base}).</li>
 * </ul>
 * Il costo vale <b>il valore alla data della lettura</b> e non quello dello scambio: con quello dello scambio la latente
 * sarebbe la variazione di prezzo fra il deposito e la data di lettura, che non è un guadagno del contribuente.
 * Il motore delle plusvalenze, l'ISEE e la Rilevanza A non cambiano. Analisi: {@code Analisi_Scambio_Differito_Ordine_Invertito.md}.
 */
public final class TokenDaConsegnare {

    /** Suffisso del simbolo. Cambiarlo qui basta: nessun'altra parte del programma lo scrive per esteso. */
    public static final String SUFFISSO = ".DA_CONSEGNARE";

    /** Causale con cui il RW chiude il lotto del token al prelievo. */
    public static final String CAUSALE_RW_CONSEGNA = "Consegna dei token ceduti con lo scambio differito";

    private TokenDaConsegnare() {
    }

    /** @return {@code true} se il simbolo è un token {@code X.DA_CONSEGNARE} */
    public static boolean Is(String Moneta) {
        return Moneta != null && Moneta.length() > SUFFISSO.length() && Moneta.endsWith(SUFFISSO);
    }

    /** @return il simbolo senza il suffisso, invariato se non lo ha */
    public static String Base(String Moneta) {
        return Is(Moneta) ? Moneta.substring(0, Moneta.length() - SUFFISSO.length()) : Moneta;
    }

    /** @return il simbolo del token di possesso della moneta indicata */
    public static String Di(String Moneta) {
        return Moneta + SUFFISSO;
    }

    /**
     * Costo di carico di una moneta alla chiusura di un periodo, per i lettori che lo ricavano dalle pile.
     *
     * @param Moneta simbolo della moneta
     * @param Valore valore della moneta alla data di lettura (prezzo per quantità)
     * @param CostoLotti costo ricavato dai lotti LIFO
     * @return il valore per un token da consegnare (latente zero), altrimenti il costo dei lotti
     */
    public static BigDecimal CostoDiCarico(String Moneta, BigDecimal Valore, BigDecimal CostoLotti) {
        return Is(Moneta) ? Valore : CostoLotti;
    }

    /**
     * Come {@link #CostoDiCarico} per la riga della tabella principale di Giacenze a data, che ha prezzo unitario e
     * quantità. Un token senza prezzo ha costo zero (la riga resta «Token senza prezzo»), e una quantità nulla,
     * negativa o illeggibile non ha rimanenze da valorizzare.
     *
     * @return il valore esatto (prezzo per quantità) per un token da consegnare, altrimenti {@code CostoLotti}
     */
    public static BigDecimal CostoRigaGiacenze(String Moneta, String Qta, BigDecimal PrezzoUnitario, BigDecimal CostoLotti) {
        if (!Is(Moneta)) {
            return CostoLotti;
        }
        try {
            BigDecimal Q = new BigDecimal(Qta.trim());
            if (PrezzoUnitario == null || Q.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            return PrezzoUnitario.multiply(Q);
        } catch (NumberFormatException | NullPointerException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Le colonne dei costi della tabella dettaglio di Giacenze a data per un movimento del token: costo residuo uguale al
     * valore residuo e differenza zero. Senza valore il costo resta vuoto, così la riga non va nemmeno alla ricerca del
     * prezzo in background (che non aggiornerebbe il costo).
     *
     * @param Colonne le cinque colonne di {@code ColonneCostiDettaglio}: 2 valore residuo, 3 costo residuo, 4 differenza
     */
    public static void AdattaColonneDettaglio(String[] Colonne) {
        if (Colonne[2].isEmpty()) {
            Colonne[3] = "";
        } else {
            Colonne[3] = Colonne[2];
            if (!Colonne[4].isEmpty()) {
                Colonne[4] = "0.00";
            }
        }
    }

    /** @return {@code true} se il movimento è l'ingresso del token alla data del deposito ({@code MT1}): il RW apre il lotto */
    public static boolean IsIngressoRW(String Campo18, String MonetaEntrata) {
        return Campo18 != null && Campo18.contains("DTW - Trasferimento Interno") && Is(MonetaEntrata);
    }

    /** @return {@code true} se il movimento è il prelievo originale che consegna il token: il RW chiude il lotto */
    public static boolean IsConsegnaRW(String Campo18, String MonetaUscita) {
        return Campo18 != null && Campo18.contains("PTW - Scambio Differito") && Is(MonetaUscita);
    }
}
