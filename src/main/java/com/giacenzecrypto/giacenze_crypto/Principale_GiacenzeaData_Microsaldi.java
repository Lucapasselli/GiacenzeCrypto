package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.math.BigDecimal;

/**
 * Evidenziazione in giallo dei microsaldi nelle tabelle di "Giacenze a data": quantità positive talmente
 * piccole da essere quasi certamente un residuo (una fee persa in un cashout, per esempio) e non una giacenza vera.
 *
 * <p>Solo visualizzazione. Non è un errore da correggere (la polvere può essere vera e l'exchange la conserva),
 * quindi non entra nel pulsante <em>Errori</em>, e non tocca il motore delle plusvalenze: niente
 * {@code OpzioniRicalcolo.Epoca()}, niente golden master. Le tabelle leggono le {@link Soglie} dalla proprietà
 * {@link Tabelle#PROP_MICROSALDI}; il saldo che le API di exchange o blockchain confermano non si colora mai
 * (lo decide il renderer, con lo stesso confronto della colonna <em>Qta Blockchain</em>).
 */
public final class Principale_GiacenzeaData_Microsaldi {

    static final String OPZIONE_ATTIVO = "Giacenze_Microsaldi_Attivo";
    static final String OPZIONE_VALORE_MAX = "Giacenze_Microsaldi_ValoreMaxEuro";
    static final String OPZIONE_QTA_MAX = "Giacenze_Microsaldi_QtaMax";
    /** Valore massimo in euro proposto la prima volta. */
    static final String VALORE_MAX_DEFAULT = "0.10";

    private Principale_GiacenzeaData_Microsaldi() {
    }

    /**
     * Le soglie: un saldo positivo è un microsaldo se vale non più di {@code valoreMaxEuro} (quando il token ha un
     * prezzo) <b>oppure</b> se la quantità non supera {@code qtaMax}. Una soglia {@code null} non si applica; la
     * quantità serve dove il valore non c'è o non basta (1e-6 BTC e 1e-6 SHIB valgono cose molto diverse).
     * Un token senza prezzo non si colora per il valore (ha già il suo avviso «Token senza prezzo»), solo per la quantità.
     */
    public record Soglie(BigDecimal valoreMaxEuro, BigDecimal qtaMax) {

        /**
         * @param qta la quantità come testo (anche in notazione scientifica)
         * @param valoreEuro il valore in euro della quantità, {@code null} se il token non ha un prezzo
         * @return vero se la quantità è positiva e sotto una delle due soglie
         */
        public boolean IsMicro(String qta, BigDecimal valoreEuro) {
            BigDecimal q = Numero(qta);
            if (q == null || q.signum() <= 0) {
                return false;
            }
            if (valoreMaxEuro != null && valoreEuro != null && valoreEuro.abs().compareTo(valoreMaxEuro) <= 0) {
                return true;
            }
            return qtaMax != null && q.compareTo(qtaMax) <= 0;
        }

        /** Il testo delle soglie per il tooltip. */
        public String Descrizione() {
            StringBuilder sb = new StringBuilder();
            if (valoreMaxEuro != null) {
                sb.append("valore fino a ").append(valoreMaxEuro.toPlainString()).append(" EUR");
            }
            if (qtaMax != null) {
                if (sb.length() > 0) {
                    sb.append(" o ");
                }
                sb.append("quantità fino a ").append(qtaMax.toPlainString());
            }
            return sb.toString();
        }
    }

    /** FIAT e NFT non hanno microsaldi: la FIAT ha due decimali, gli NFT sono unità intere. */
    public static boolean TipoEscluso(String tipo) {
        return tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase("FIAT") || tipo.equalsIgnoreCase("NFT");
    }

    /** @return il numero, {@code null} se il testo è vuoto o non è un numero */
    static BigDecimal Numero(String testo) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(testo.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** @return la soglia scritta dall'utente: {@code null} se vuota, lancia se non è un numero positivo */
    static BigDecimal SogliaDaTesto(String testo) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        BigDecimal n = Numero(testo);
        if (n == null || n.signum() <= 0) {
            throw new IllegalArgumentException(testo);
        }
        return n;
    }

    /** Le soglie salvate, o quelle proposte la prima volta. */
    public static Soglie Leggi() {
        try {
            return new Soglie(
                    SogliaDaTesto(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_VALORE_MAX, VALORE_MAX_DEFAULT)),
                    SogliaDaTesto(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_QTA_MAX, "")));
        } catch (IllegalArgumentException ex) {
            return new Soglie(new BigDecimal(VALORE_MAX_DEFAULT), null);
        }
    }

    public static void Scrivi(Soglie s) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_VALORE_MAX, s.valoreMaxEuro() == null ? "" : s.valoreMaxEuro().toPlainString());
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_QTA_MAX, s.qtaMax() == null ? "" : s.qtaMax().toPlainString());
    }

    public static boolean Attivo() {
        return "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ATTIVO, "NO"));
    }

    public static void ImpostaAttivo(boolean attivo) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ATTIVO, attivo ? "SI" : "NO");
    }

    /**
     * Chiede le due soglie all'utente, una dopo l'altra, partendo da quelle attuali.
     *
     * @return le soglie scelte; {@code null} se annulla o se lascia vuote tutte e due (allora non c'è nulla da evidenziare)
     */
    public static Soglie ChiediSoglie(Window owner, Soglie attuali) {
        BigDecimal valore = attuali.valoreMaxEuro();
        BigDecimal qta = attuali.qtaMax();
        String spiegazione = "Un saldo positivo viene evidenziato in giallo se vale non più della soglia in euro "
                + "oppure se la quantità non supera la soglia in quantità. Lascia vuota una soglia per non usarla. "
                + "Il saldo confermato dalle API di exchange o blockchain non viene mai evidenziato.";
        while (true) {
            String v = AppDialog.showTextInputDialog(owner, "Microsaldi", "Soglia in euro",
                    spiegazione + "\n\nValore massimo in euro (vuoto = non usare):",
                    "Euro", valore == null ? "" : valore.toPlainString());
            if (v == null) {
                return null;
            }
            try {
                valore = SogliaDaTesto(v);
                break;
            } catch (IllegalArgumentException ex) {
                Messaggi.WarningMessage("Microsaldi", "«" + v.trim() + "» non è un numero maggiore di zero.", owner);
            }
        }
        while (true) {
            String q = AppDialog.showTextInputDialog(owner, "Microsaldi", "Soglia in quantità",
                    "Quantità massima del token (vuoto = non usare). Vale anche per i token senza un prezzo:",
                    "Quantità", qta == null ? "" : qta.toPlainString());
            if (q == null) {
                return null;
            }
            try {
                qta = SogliaDaTesto(q);
                break;
            } catch (IllegalArgumentException ex) {
                Messaggi.WarningMessage("Microsaldi", "«" + q.trim() + "» non è un numero maggiore di zero.", owner);
            }
        }
        if (valore == null && qta == null) {
            Messaggi.WarningMessage("Microsaldi", "Servono una soglia in euro o in quantità, altrimenti non c'è nulla da evidenziare.", owner);
            return null;
        }
        return new Soglie(valore, qta);
    }
}
