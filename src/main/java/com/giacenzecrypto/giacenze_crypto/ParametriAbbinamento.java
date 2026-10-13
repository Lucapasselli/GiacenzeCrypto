package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Le tolleranze dell'abbinamento automatico di depositi e prelievi (fase 1 di «Assegnazione Automatica»), lette da
 * {@code config/varie/Parametri.json}, blocco {@code abbinamentoDepositiPrelievi}.
 *
 * <p>Sono numeri, non regole: la finestra di un'ora, il 2% di differenza fra le quantità e la tolleranza inversa. Stavano
 * in {@code Principale} e un caso reale (due trasferimenti Nexo del 12/03/2025 distanti esattamente 3600 s, quindi fuori
 * dal «strettamente minore di un'ora») voleva una versione nuova. Il file si rilegge a ogni pressione del pulsante, senza
 * cache, e non c'è una copia dei valori nel codice (il default è il jar, vedi {@link MappeCausali#CaricaConRipiego}).
 */
final class ParametriAbbinamento {

    /** Eccezione alla finestra per un exchange. */
    record Eccezione(String exchange, long finestraSecondi) {}

    /**
     * @param finestraSecondi              distanza massima (esclusa) fra le date delle due righe, in secondi
     * @param differenzaMassimaPercentuale differenza massima (esclusa) fra le due quantità, in percentuale
     * @param tolleranzaInversaDivisore    l'eccedenza ammessa del deposito sul prelievo è quantità / divisore (coppie crypto)
     * @param perExchange                  eccezioni alla finestra
     */
    record Parametri(long finestraSecondi, BigDecimal differenzaMassimaPercentuale, BigDecimal tolleranzaInversaDivisore,
                     List<Eccezione> perExchange) {

        /** La finestra per due movimenti: la più larga fra quella generale e le eccezioni dei due exchange. */
        long finestraPer(String exchange1, String exchange2) {
            long finestra = finestraSecondi;
            for (Eccezione e : perExchange) {
                if (e.exchange().equalsIgnoreCase(exchange1 == null ? "" : exchange1.trim())
                        || e.exchange().equalsIgnoreCase(exchange2 == null ? "" : exchange2.trim())) {
                    finestra = Math.max(finestra, e.finestraSecondi());
                }
            }
            return finestra;
        }

        /** L'eccedenza ammessa del deposito sul prelievo per una quantità (crypto): quantità / divisore. */
        BigDecimal tolleranzaInversa(BigDecimal quantita) {
            return quantita.abs().divide(tolleranzaInversaDivisore, MathContext.DECIMAL128);
        }
    }

    private ParametriAbbinamento() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "Parametri";

    /** @return i parametri (disco, poi copia nel jar), oppure {@code null} se non sono disponibili da nessuna parte */
    static Parametri Carica() {
        return MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, ParametriAbbinamento::Interpreta);
    }

    /** @return i parametri, oppure {@code null} se il blocco manca o ha valori non validi (finestra ≤ 0, divisore ≤ 0, percentuale < 0) */
    static Parametri Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject b = new JSONObject(contenuto).getJSONObject("abbinamentoDepositiPrelievi");
            long finestra = b.getLong("finestraSecondi");
            BigDecimal percentuale = new BigDecimal(b.get("differenzaMassimaPercentuale").toString());
            BigDecimal divisore = new BigDecimal(b.get("tolleranzaInversaDivisore").toString());
            if (finestra <= 0 || percentuale.signum() < 0 || divisore.signum() <= 0) {
                LoggerGC.ScriviErrore("ParametriAbbinamento: valori fuori intervallo in " + nome + ".json");
                return null;
            }
            List<Eccezione> eccezioni = new ArrayList<>();
            JSONArray voci = b.optJSONArray("perExchange");
            for (int i = 0; voci != null && i < voci.length(); i++) {
                JSONObject v = voci.getJSONObject(i);
                String exchange = v.getString("exchange").trim().toLowerCase(Locale.ROOT);
                long f = v.getLong("finestraSecondi");
                if (exchange.isEmpty() || f <= 0) {
                    LoggerGC.ScriviErrore("ParametriAbbinamento: eccezione per exchange non valida in " + nome + ".json");
                    return null;
                }
                eccezioni.add(new Eccezione(exchange, f));
            }
            return new Parametri(finestra, percentuale, divisore, List.copyOf(eccezioni));
        } catch (Exception e) {
            LoggerGC.ScriviErrore("ParametriAbbinamento: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }
}
