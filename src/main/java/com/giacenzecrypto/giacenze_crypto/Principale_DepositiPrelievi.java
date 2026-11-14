package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Logica della classificazione automatica dei depositi e prelievi ({@code Principale.DepositiPrelievi_AssegnazioneAutomatica}),
 * senza Swing: la lista degli ID ancora da classificare e le garanzie sulle singole classificazioni.
 *
 * <p>Due regole da non rompere:</p>
 * <ul>
 *   <li><b>La lista degli ID non è affidabile fra una fase e l'altra.</b> {@code Principale.DepositiPrelieviDaCategorizzare} viene
 *       riempita una volta sola, ma lo scambio differito toglie dalla mappa gli ID di prelievo e deposito
 *       ({@code RimuoviMovimentazioneXID}) e li ricrea con altri. Un ciclo che scorre la lista e fa {@code get(id)} trova
 *       {@code null}. Ogni ciclo parte quindi da {@link #IDPresenti}, e quelli che rimuovono ID durante il ciclo (FASE 5 e
 *       FASE 6) controllano comunque il {@code null}.</li>
 *   <li><b>Una classificazione è tutta o niente.</b> Le funzioni che classificano toccano più righe in più passi (cambiano
 *       {@code [5]} e {@code [18]}, tolgono righe dalla mappa, ne aggiungono, calcolano valori e prezzi): un errore nel mezzo
 *       lasciava un {@code PTW}/{@code DTW} senza {@code [20]} o, nello scambio differito, i due movimenti fuori dalla mappa.
 *       {@link Esito#atomico} fotografa la mappa e, se l'azione lancia un'eccezione, la ripristina com'era (stesse righe, stesso
 *       contenuto) e annota il problema: le altre classificazioni proseguono.</li>
 * </ul>
 * Il ripristino riguarda la sola mappa dei movimenti. Le funzioni protette non scrivono altrove (né storico né documenti di
 * origine: usano solo {@code DocumentiFonte.SoloOrigine}, che legge).
 */
public final class Principale_DepositiPrelievi {

    private Principale_DepositiPrelievi() {
    }

    /**
     * Gli ID della lista che hanno ancora un movimento nella mappa, nello stesso ordine, in una <b>copia</b> (chi la scorre
     * può cambiare la mappa senza {@code ConcurrentModificationException}).
     *
     * <p>Solo la presenza, non {@code [18]} vuoto: ogni ciclo controlla già da sé se il movimento è classificato, e filtrare
     * qui cambierebbe quali righe vede ciascuno.</p>
     * @param id gli ID (di solito {@code Principale.DepositiPrelieviDaCategorizzare}); {@code null} = nessuno
     * @param mappa la mappa dei movimenti
     */
    public static List<String> IDPresenti(Collection<String> id, Map<String, String[]> mappa) {
        List<String> risultato = new ArrayList<>();
        if (id == null) return risultato;
        for (String x : id) {
            if (mappa.get(x) != null) risultato.add(x);
        }
        return risultato;
    }

    /** La mappa com'era: le righe (gli oggetti) e il loro contenuto. */
    static final class Fotografia {

        private final String[] chiavi;
        private final String[][] righe;
        private final String[][] contenuti;

        private Fotografia(Map<String, String[]> mappa) {
            int n = mappa.size();
            chiavi = new String[n];
            righe = new String[n][];
            contenuti = new String[n][];
            int i = 0;
            for (Map.Entry<String, String[]> e : mappa.entrySet()) {
                chiavi[i] = e.getKey();
                righe[i] = e.getValue();
                contenuti[i] = e.getValue() == null ? null : e.getValue().clone();
                i++;
            }
        }

        static Fotografia scatta(Map<String, String[]> mappa) {
            return new Fotografia(mappa);
        }

        /**
         * Rimette la mappa com'era: le righe aggiunte spariscono, quelle tolte tornano, e ogni riga riprende il contenuto di
         * prima <b>sullo stesso oggetto</b> (chi ne tiene un riferimento lo ritrova valido).
         */
        void ripristina(Map<String, String[]> mappa) {
            mappa.clear();
            for (int i = 0; i < chiavi.length; i++) {
                if (righe[i] != null && contenuti[i] != null) {
                    System.arraycopy(contenuti[i], 0, righe[i], 0, Math.min(contenuti[i].length, righe[i].length));
                }
                mappa.put(chiavi[i], righe[i]);
            }
        }
    }

    /**
     * Il risultato di una classificazione automatica: quante modifiche, e che cosa non è riuscito. Una per esecuzione.
     */
    public static final class Esito {

        /** Modifiche fatte, scritte dal chiamante a fine fasi (anche se una fase si interrompe). */
        public int modifiche;
        /** Le cose non riuscite, una riga ciascuna, nell'ordine in cui sono capitate. */
        public final List<String> problemi = new ArrayList<>();

        private final Map<String, String[]> mappa;

        public Esito() {
            this(Principale.MappaCryptoWallet);
        }

        Esito(Map<String, String[]> mappa) {
            this.mappa = mappa;
        }

        /**
         * Esegue una classificazione tutta o niente: se l'azione lancia un'eccezione la mappa torna com'era e si annota il
         * problema. Costa una fotografia della mappa (qualche millisecondo), quindi si usa attorno alla classificazione
         * vera e non a ogni iterazione dei cicli.
         * @param descrizione che cosa si sta facendo, per il log e per l'avviso finale
         * @param azione la classificazione
         * @param seFallita il valore da restituire se l'azione fallisce
         */
        public <T> T atomico(String descrizione, Supplier<T> azione, T seFallita) {
            Fotografia prima = Fotografia.scatta(mappa);
            try {
                return azione.get();
            } catch (RuntimeException e) {
                try {
                    prima.ripristina(mappa);
                    segnala(descrizione + ": annullato, i movimenti sono rimasti come prima", e);
                } catch (RuntimeException ripristino) {
                    //Non deve succedere: se capita lo si dice chiaramente, la mappa potrebbe essere cambiata
                    segnala(descrizione + ": ATTENZIONE, il ripristino non è riuscito (" + ripristino + ")", e);
                }
                return seFallita;
            }
        }

        /**
         * Esegue una fase (o un elemento di una fase) che non ha bisogno del ripristino perché non lascia stati a metà:
         * se lancia un'eccezione la annota e va avanti.
         */
        public <T> T protetto(String descrizione, Supplier<T> azione, T seFallita) {
            try {
                return azione.get();
            } catch (RuntimeException e) {
                segnala(descrizione + ": interrotto", e);
                return seFallita;
            }
        }

        /** Annota un problema (log e riepilogo finale). */
        public void segnala(String descrizione, Throwable e) {
            problemi.add(descrizione + (e == null ? "" : " [" + e + "]"));
            LoggerGC.ScriviErrore("Classificazione automatica depositi/prelievi - " + descrizione);
            if (e != null) LoggerGC.ScriviErrore(e);
        }

        /** Il testo del riepilogo dei problemi per l'utente (senza dettagli tecnici: stanno nel log). */
        public String testoProblemi() {
            return "Non tutte le classificazioni sono riuscite (" + problemi.size() + "). Quelle non riuscite sono state annullate "
                    + "e i movimenti sono rimasti com'erano, le altre sono state fatte. I dettagli sono nel log.";
        }
    }
}
