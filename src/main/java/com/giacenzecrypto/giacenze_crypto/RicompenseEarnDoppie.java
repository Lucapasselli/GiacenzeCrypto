package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Il Bonus Tiered APR di Binance Simple Earn importato due volte, e la sua pulizia.
 *
 * <p>Dagli export 2026 Binance scrive ogni bonus giornaliero di Simple Earn su due righe: sul conto Earn
 * ({@code Simple Earn Flexible - Rewards Income}, {@code Simple Earn Locked - Rewards Income}) e sul conto Spot, dove lo
 * paga ({@code Simple Earn Flexible Interest}, {@code Simple Earn Locked Rewards}), con la stessa moneta e quantità, di
 * solito 0-2 ore dopo. Verificato sullo storico Rewards dell'app (2026-10-06): il "Bonus Tiered APR Rewards" coincide
 * per ora e importo con la riga del conto Earn. Fino alla 1.016 {@code config/import/Binance CSV.json} importava tutte e
 * due come EARN; dalla 1.017 la riga Earn è IGNORA. Questa classe trova le righe Earn già importate che hanno la loro
 * gemella Spot e le toglie: una riga Earn senza gemella non si tocca, e nemmeno una che fa parte di un gruppo
 * classificato ({@code [20]} non vuoto), che l'utente ha evidentemente trattato a mano.</p>
 *
 * <p><b>Dopo la pulizia possono comparire giacenze negative su Binance, e non è un errore della pulizia.</b> Le
 * "Real-Time APR Rewards" di Simple Earn non compaiono in nessuna riga dell'export: maturano dentro il prodotto ed
 * escono solo con i riscatti, che l'import ignora. I doppioni coprivano in parte quel buco (sull'archivio 2026
 * dell'utente, da un prelievo totale all'altro, 8,88 USDC di doppioni contro 23,64 di Real-Time APR mancanti).</p>
 *
 * <p>Logica pura, senza Swing: l'avviso all'avvio e il pulsante nelle Opzioni stanno in
 * {@link Principale_Opzioni_Pulizie}.</p>
 */
public final class RicompenseEarnDoppie {

    private RicompenseEarnDoppie() {
    }

    /** Causale della riga del conto Earn → causale della sua gemella sul conto Spot. */
    static final Map<String, String> GEMELLE = Map.of(
            "Simple Earn Flexible - Rewards Income", "Simple Earn Flexible Interest",
            "Simple Earn Locked - Rewards Income", "Simple Earn Locked Rewards");

    /** Distanza massima fra le due righe: nell'export dell'utente quasi tutte entro 2 ore, una entro 24. */
    static final long DISTANZA_MAX_S = 86400L;

    /** Opzione in {@code personale.mv.db}: "SI" se l'utente ha chiesto di non vedere più l'avviso all'avvio. */
    public static final String OPZIONE_NON_MOSTRARE = "RicompenseEarnDoppie_NonMostrare";

    /**
     * Le righe del conto Earn che hanno la loro gemella Spot, da togliere. Ogni riga Spot fa da gemella a una sola riga
     * Earn, la più vicina nel tempo.
     * @param Mappa i movimenti dell'archivio
     * @return le righe Earn doppie, in ordine di ID
     */
    public static List<String[]> Trova(Map<String, String[]> Mappa) {
        //Righe Spot candidate gemelle, per exchange, causale, moneta e quantità
        Map<String, List<String[]>> Spot = new HashMap<>();
        List<String[]> Earn = new ArrayList<>();
        for (String[] v : Mappa.values()) {
            if (v == null || v.length <= 22 || v[7] == null) continue;
            String Causale = v[7].trim();
            if (GEMELLE.containsKey(Causale)) {
                if (isRicompensa(v) && Funzioni.noData(v[20])) Earn.add(v);
            } else if (GEMELLE.containsValue(Causale) && isRicompensa(v)) {
                String K = Chiave(v, Causale);
                if (K != null) Spot.computeIfAbsent(K, k -> new ArrayList<>()).add(v);
            }
        }
        List<String[]> Doppie = new ArrayList<>();
        Set<String[]> Usate = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String[] e : Earn) {
            String K = Chiave(e, GEMELLE.get(e[7].trim()));
            List<String[]> Candidate = K == null ? null : Spot.get(K);
            if (Candidate == null) continue;
            long t = OperazioniCalcolate.Istante(e);
            String[] Migliore = null;
            long Distanza = Long.MAX_VALUE;
            for (String[] s : Candidate) {
                if (Usate.contains(s)) continue;
                long d = Math.abs(OperazioniCalcolate.Istante(s) - t);
                if (t > 0 && d <= DISTANZA_MAX_S && d < Distanza) {
                    Migliore = s;
                    Distanza = d;
                }
            }
            if (Migliore != null) {
                Usate.add(Migliore);
                Doppie.add(e);
            }
        }
        return Doppie;
    }

    /** Una ricompensa: categoria RW nell'ID, solo entrata. */
    private static boolean isRicompensa(String[] v) {
        return v[0] != null && v[0].endsWith("_RW") && !Funzioni.noData(v[11]) && !Funzioni.noData(v[13]);
    }

    /** Exchange, causale della gemella, moneta e quantità come numero; {@code null} se la quantità non è un numero. */
    private static String Chiave(String[] v, String CausaleSpot) {
        try {
            String Qta = new BigDecimal(v[13].trim()).stripTrailingZeros().toPlainString();
            return v[3] + "|" + CausaleSpot + "|" + v[11] + "|" + Qta;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** @return il controvalore in euro delle righe, dal campo 15 */
    public static BigDecimal Valore(Collection<String[]> Righe) {
        BigDecimal Totale = BigDecimal.ZERO;
        for (String[] v : Righe) {
            try {
                Totale = Totale.add(new BigDecimal(v[15].trim()));
            } catch (RuntimeException ex) {
                //valore mancante: non conta
            }
        }
        return Totale;
    }

    /**
     * Toglie le righe dall'archivio, per righe e non per ID ({@link Funzioni#RimuoviMovimenti}). Non ricalcola nulla:
     * lo fa il chiamante, una volta.
     * @return quante righe sono state tolte
     */
    public static int Rimuovi(Collection<String[]> Righe) {
        List<String> IDs = new ArrayList<>();
        for (String[] v : Righe) IDs.add(v[0]);
        return Funzioni.RimuoviMovimenti(IDs);
    }
}
