package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Le plusvalenze della tabella dei movimenti divise per aliquota dell'imposta sostitutiva, con la somma di vendite e
 * costi di carico di ciascun gruppo: è il contenuto del dialogo che si apre dal riquadro della plusvalenza
 * ({@link GUI_PlusvalenzeAliquote}).
 *
 * <p><b>La regola.</b> Fino al 31/12/2025 tutto al 26%. Dal 01/01/2026 le plusvalenze e gli altri proventi della lettera
 * c-sexies sono al 33% (L. 207/2024 art. 1 c. 24), salvo quelli "derivanti da operazioni di detenzione, cessione o
 * impiego di token di moneta elettronica denominati in euro", che restano al 26% (L. 199/2025 art. 1 c. 28). Il
 * programma legge la norma così: la plusvalenza di un movimento nasce da una gamba, e va al 26% se quella gamba è un
 * token di moneta elettronica in euro. La gamba è quella <b>uscita</b> se c'è e non è FIAT (una cessione, anche in
 * permuta), altrimenti quella <b>entrata</b> (un provento, per esempio una reward). Quindi la vendita di BTC contro EURC
 * resta al 33% (il reddito nasce dal BTC), la cessione di EURC contro BTC e la reward in EURC vanno al 26%. È
 * un'interpretazione dell'utente (13/11/2026), la norma non dice nulla sugli scambi: vedi
 * {@code nocommit/Documentazione/Analisi_Regole_Scoperte_e_EMT_Euro.md}.</p>
 *
 * <p><b>Che cosa vuol dire "in euro".</b> Servono due cose insieme: il token è E-Money nella sezione delle opzioni
 * dell'utente alla data del movimento (stessa regola del motore, {@link Funzioni#RitornaTipoCrypto}) e l'elenco
 * {@code config/varie/EmoneyToken.json} lo dichiara {@code denominatoInEuro}. Il nome non decide: USDC è E-Money ma
 * non in euro, e un token con "EUR" nel simbolo che non è nell'elenco è 33%.</p>
 *
 * <p><b>Informativo.</b> Non entra in nessun calcolo: motore, quadro RT e golden master non lo vedono. Il dialogo mostra
 * anche un'imposta per aliquota ({@link #Imposta}, plusvalenza netta della riga per l'aliquota, zero se negativa, tutti gli
 * anni insieme: nessuna compensazione né regola del quadro RT dell'anno, per esempio la soglia ante 2023) e la dichiara
 * un'indicazione. La regola di somma sta qui sola, in {@link #Totali}: la plusvalenza è la somma di {@code v[19]}
 * numerici, vendite {@code v[15]} e costi {@code v[16]} contano solo se {@code v[33]} è "S". Dal 2026-11-13 la usano
 * anche i tre riquadri della tabella movimenti (non più sommati nel ciclo di caricamento), e le righe sono quelle che la
 * tabella mostra davvero, con periodo, filtri di caricamento <b>e filtri di riga</b> ({@link #InOrdineDiVista}): il
 * totale del dialogo coincide con i riquadri e con la somma nell'intestazione della colonna.</p>
 */
final class Principale_PlusvalenzeAliquote {

    private Principale_PlusvalenzeAliquote() {}

    /** Primo giorno (yyyyMMdd) in cui vale l'aliquota del 33%: i redditi realizzati da quella data (L. 207/2024 c. 24). */
    static final String INIZIO_33 = "20260101";

    /** Il gruppo di aliquota di una plusvalenza. */
    enum Categoria {
        FINO_2025("Fino al 31/12/2025, tutto al 26%", 26),
        EMT_EURO("Dal 01/01/2026, token di moneta elettronica in euro", 26),
        ALTRE("Dal 01/01/2026, altre cripto-attività", 33);

        final String etichetta;
        final int aliquota;

        Categoria(String etichetta, int aliquota) {
            this.etichetta = etichetta;
            this.aliquota = aliquota;
        }
    }

    /** Le somme di un gruppo. */
    static final class Riga {
        int movimenti;
        BigDecimal vendite = BigDecimal.ZERO;
        BigDecimal costi = BigDecimal.ZERO;
        BigDecimal plusvalenza = BigDecimal.ZERO;

        private void somma(Riga altra) {
            movimenti += altra.movimenti;
            vendite = vendite.add(altra.vendite);
            costi = costi.add(altra.costi);
            plusvalenza = plusvalenza.add(altra.plusvalenza);
        }
    }

    /** Aliquote mostrate nel dialogo, in questo ordine. */
    static final int[] ALIQUOTE = {26, 33};

    /** Un movimento che ha concorso al calcolo, con il gruppo di aliquota a cui è stato attribuito. */
    record Voce(String[] movimento, Categoria categoria) {}

    /**
     * L'imposta sostitutiva che risulterebbe da una plusvalenza a una data aliquota, arrotondata al centesimo. Una
     * plusvalenza negativa (minusvalenza) dà zero: compensazioni e riporti fra aliquote e anni non li conosce il
     * programma, quindi non si sottrae nulla.
     */
    static BigDecimal Imposta(BigDecimal plusvalenza, int aliquota) {
        if (plusvalenza == null || plusvalenza.signum() <= 0) return BigDecimal.ZERO;
        return plusvalenza.multiply(BigDecimal.valueOf(aliquota)).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Tutto quello che mostra il dialogo. */
    static final class Risultato {
        final Map<Categoria, Riga> perCategoria = new EnumMap<>(Categoria.class);
        /** Aliquota (26, 33) → somme: è la tabella in alto del dialogo, senza distinzione per anno. */
        final Map<Integer, Riga> perAliquota = new TreeMap<>();
        /** Aliquota → movimenti che hanno determinato quelle somme, nell'ordine ricevuto. */
        final Map<Integer, java.util.List<Voce>> vociPerAliquota = new TreeMap<>();
        final Riga totale = new Riga();
        /** Token E-Money in euro che hanno prodotto plusvalenza dal 2026 (al 26%), con il numero di movimenti. */
        final Map<String, Integer> tokenEuro = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /** Token E-Money NON in euro (o non nell'elenco) che hanno prodotto plusvalenza dal 2026 (al 33%). */
        final Map<String, Integer> tokenEmtNonEuro = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /** {@code false} se l'elenco {@code EmoneyToken.json} non c'era: nessun token è risultato in euro. */
        boolean elencoDisponibile = true;

        Riga riga(Categoria c) {
            return perCategoria.computeIfAbsent(c, k -> new Riga());
        }

        /** @return le somme di un'aliquota (vuote se nessun movimento ci è finito) */
        Riga rigaAliquota(int aliquota) {
            return perAliquota.computeIfAbsent(aliquota, k -> new Riga());
        }

        /** @return i movimenti di un'aliquota, o tutti (in ordine di aliquota) con {@code aliquota <= 0} */
        java.util.List<Voce> voci(int aliquota) {
            if (aliquota > 0) return vociPerAliquota.getOrDefault(aliquota, java.util.List.of());
            java.util.List<Voce> tutte = new java.util.ArrayList<>();
            for (java.util.List<Voce> l : vociPerAliquota.values()) tutte.addAll(l);
            return tutte;
        }

        /** @return l'imposta complessiva, somma di quella di ciascuna aliquota */
        BigDecimal impostaTotale() {
            BigDecimal t = BigDecimal.ZERO;
            for (Map.Entry<Integer, Riga> e : perAliquota.entrySet()) t = t.add(Imposta(e.getValue().plusvalenza, e.getKey()));
            return t;
        }
    }

    /**
     * Divide i movimenti per aliquota. Passare quelli mostrati nella tabella (periodo e filtri scelti dall'utente).
     *
     * @param movimenti righe da 46 campi; quelle senza plusvalenza né vendite non contano
     * @param elenco l'elenco {@link EmoneyToken#Carica()}, oppure {@code null} se non disponibile
     */
    static Risultato Calcola(Collection<String[]> movimenti, EmoneyToken.Elenco elenco) {
        Risultato r = new Risultato();
        r.elencoDisponibile = elenco != null;
        for (String[] v : movimenti) {
            if (v == null || v.length <= 33) continue;
            Riga mov = SommeDelMovimento(v);
            boolean haPlusvalenza = "S".equals(v[33]);
            if (mov.plusvalenza.signum() == 0 && !haPlusvalenza) continue;
            mov.movimenti = 1;

            Categoria c = Categoria(v, elenco);
            r.riga(c).somma(mov);
            r.rigaAliquota(c.aliquota).somma(mov);
            r.vociPerAliquota.computeIfAbsent(c.aliquota, k -> new java.util.ArrayList<>()).add(new Voce(v, c));
            r.totale.somma(mov);

            if (c != Categoria.FINO_2025) {
                String simbolo = SimboloDellaGamba(v);
                if (c == Categoria.EMT_EURO) {
                    r.tokenEuro.merge(simbolo, 1, Integer::sum);
                } else if (EMoneyAllaData(v)) {
                    r.tokenEmtNonEuro.merge(simbolo, 1, Integer::sum);
                }
            }
        }
        return r;
    }

    /**
     * Le righe dei movimenti nell'ordine in cui la tabella le mostra, tenendo solo quelle che hanno un movimento fra i
     * caricati. Serve a far valere anche i filtri di riga (ricerca e filtri per colonna), che agiscono sul modello
     * della tabella e non sulla lista del caricamento.
     *
     * @param caricati i movimenti che passano i filtri di caricamento
     * @param idInVista gli ID (colonna 0 del modello) delle righe in vista, già filtrate dal sorter
     */
    static java.util.List<String[]> InOrdineDiVista(Collection<String[]> caricati, java.util.List<String> idInVista) {
        Map<String, String[]> perId = new java.util.HashMap<>();
        for (String[] v : caricati) {
            if (v != null && v.length > 0) perId.put(v[0], v);
        }
        java.util.List<String[]> r = new java.util.ArrayList<>(idInVista.size());
        for (String id : idInVista) {
            String[] v = perId.get(id);
            if (v != null) r.add(v);
        }
        return r;
    }

    /**
     * Plusvalenza, vendite e costi di carico di un insieme di movimenti: sono i tre riquadri della tabella
     * movimenti. Il campo {@code movimenti} conta le righe passate.
     */
    static Riga Totali(Collection<String[]> movimenti) {
        Riga t = new Riga();
        for (String[] v : movimenti) {
            if (v == null || v.length <= 33) continue;
            Riga mov = SommeDelMovimento(v);
            mov.movimenti = 1;
            t.somma(mov);
        }
        return t;
    }

    /** @return plusvalenza, vendite e costi del movimento, come li conta il ciclo della tabella */
    private static Riga SommeDelMovimento(String[] v) {
        Riga m = new Riga();
        if (Funzioni.isNumeric(v[19], false)) m.plusvalenza = new BigDecimal(v[19]);
        if ("S".equals(v[33])) {
            m.vendite = NumeroOZero(v[15]);
            m.costi = NumeroOZero(v[16]);
        }
        return m;
    }

    private static BigDecimal NumeroOZero(String s) {
        return s != null && Funzioni.isNumeric(s, false) ? new BigDecimal(s) : BigDecimal.ZERO;
    }

    /** @return il gruppo di aliquota del movimento (la regola nella descrizione della classe) */
    static Categoria Categoria(String[] v, EmoneyToken.Elenco elenco) {
        if (Data(v).compareTo(INIZIO_33) < 0) return Categoria.FINO_2025;
        return elenco != null && EMoneyAllaData(v) && elenco.denominatoInEuro(SimboloDellaGamba(v))
                ? Categoria.EMT_EURO : Categoria.ALTRE;
    }

    /** @return il simbolo della gamba da cui nasce la plusvalenza: l'uscita se c'è e non è FIAT, altrimenti l'entrata */
    static String SimboloDellaGamba(String[] v) {
        return UsaLUscita(v) ? v[8].trim() : v[11].trim();
    }

    private static boolean UsaLUscita(String[] v) {
        return !v[8].isBlank() && !"FIAT".equalsIgnoreCase(Funzioni.RitornaTipoCrypto(v[8].trim(), v[1].trim(), v[9].trim()));
    }

    /** @return {@code true} se la gamba della plusvalenza era E-Money alla data del movimento, come per il motore */
    private static boolean EMoneyAllaData(String[] v) {
        String tipo = UsaLUscita(v)
                ? Funzioni.RitornaTipoCrypto(v[8].trim(), v[1].trim(), v[9].trim())
                : Funzioni.RitornaTipoCrypto(v[11].trim(), v[1].trim(), v[12].trim());
        return "EMoney".equalsIgnoreCase(tipo);
    }

    /** @return la data del movimento come yyyyMMdd (da {@code v[1]}, altrimenti dal prefisso dell'ID) */
    private static String Data(String[] v) {
        if (v[1] != null && v[1].length() >= 10 && v[1].charAt(4) == '-') return v[1].substring(0, 10).replace("-", "");
        return v[0] != null && v[0].length() >= 8 ? v[0].substring(0, 8) : "";
    }

}
