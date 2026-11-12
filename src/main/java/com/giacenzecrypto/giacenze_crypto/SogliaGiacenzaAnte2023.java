package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Controllo della soglia dei 51.645,69 euro, la condizione che fino ai redditi 2022 rendeva imponibili le
 * plusvalenze da cessione a pronti e da prelievo di cripto (art. 67, comma 1-ter, TUIR, applicato alle
 * cripto-valute dalla circolare 30/E del 2023): la giacenza complessiva del contribuente doveva superare la
 * soglia per almeno sette giorni lavorativi continui nel periodo d'imposta, valorizzata al cambio del 1° gennaio.
 *
 * <p>Questa classe <b>calcola soltanto</b> l'esito di un anno. Non tocca il motore delle plusvalenze e non
 * legge né scrive alcun campo di movimento ({@code v[16]}, {@code v[17]}, {@code v[19]}, {@code v[33]}):
 * cosa farne nel quadro RT è una scelta a parte.</p>
 *
 * <p>Regole adottate (le prime due sono la lettera della norma e della circolare, le altre sono scelte da
 * confermare e sono in {@code nocommit/Documentazione/Analisi_Soglia_51645.md}):</p>
 * <ul>
 *   <li><b>Tutti i wallet insieme</b>: la giacenza è la somma di tutti i movimenti di tutti i gruppi wallet. Un
 *       trasferimento fra wallet propri si annulla da sé, purché i suoi due lati cadano nello stesso giorno.</li>
 *   <li><b>Prezzo del 1° gennaio</b>: ogni token è valorizzato per tutto l'anno al prezzo delle 00:00 del 1°
 *       gennaio, mai al prezzo del giorno.</li>
 *   <li><b>Soglia superata</b> vuol dire valore <b>strettamente maggiore</b> di 51.645,69 euro.</li>
 *   <li><b>Giorni lavorativi continui</b>: lunedì-venerdì senza le festività nazionali. Sabato, domenica e
 *       festivi non contano e non interrompono la serie: sette giorni lavorativi di fila, con un fine settimana
 *       in mezzo, sono continui.</li>
 *   <li><b>Giacenza di un giorno</b> = saldo a fine giornata (23:59), non il massimo raggiunto durante il giorno.</li>
 *   <li><b>Cosa conta</b>: le cripto (anche le stablecoin e gli EMoney) e i <b>dollari</b> (FIAT {@code USD}, decisione
 *       dell'utente del 2026-11-12: sono la sola valuta estera che chi detiene cripto usa davvero, convertita col
 *       cambio Banca d'Italia del 1° gennaio, o dell'ultimo giorno utile prima) e gli <b>NFT</b> (decisione dell'utente del
 *       2026-11-12, come nel quadro W/RW: contano al prezzo che hanno, e senza prezzo valgono zero e vengono
 *       segnalati). Fuori l'euro, ogni altra valuta e i token marcati SCAM. Una quantità negativa (movimento mancante) vale zero, mai valore assoluto, come
 *       nel quadro W/RW.</li>
 *   <li><b>Token senza prezzo del 1° gennaio</b> (né personalizzato né di mercato): la norma e la circolare non dicono
 *       cosa fare per un token nato dopo il 1° gennaio o non quotato (decisione dell'utente del 2026-11-12). Si usa il
 *       <b>prezzo di acquisto</b>, cioè quello del primo movimento di ingresso con un valore in euro, e il token resta
 *       identificabile in {@link Esito#prezzoDiAcquisto} perché l'utente possa dargli a mano il prezzo del 1° gennaio (un
 *       prezzo personalizzato viene sempre prima). Solo se manca anche quello il token vale zero e finisce in
 *       {@link Esito#tokenSenzaPrezzo}: l'esito allora potrebbe cambiare e il chiamante deve avvisare l'utente.</li>
 * </ul>
 *
 * <p>La chiave del token è {@code simbolo;address;rete}: lo stesso simbolo su due reti ha due saldi, ciascuno
 * valorizzato col proprio prezzo.</p>
 */
public final class SogliaGiacenzaAnte2023 {

    private SogliaGiacenzaAnte2023() {
    }

    /** Soglia in euro: cento milioni di lire. Il controllo è «superiore a», non «pari o superiore». */
    public static final BigDecimal SOGLIA = new BigDecimal("51645.69");

    /** Giorni lavorativi continui sopra soglia che fanno scattare la condizione. */
    public static final int GIORNI_LAVORATIVI_CONTINUI = 7;

    /** Ultimo anno in cui la condizione esisteva: dal 1° gennaio 2023 ogni realizzo è rilevante. */
    public static final int ULTIMO_ANNO = 2022;

    /** Un lato non FIAT di un movimento: giorno, token e quantità con segno (negativa se uscita). */
    public static final class Gamba {
        final LocalDate giorno;
        final String chiave;
        final Moneta moneta;
        final BigDecimal qta;
        /** Valore in euro del movimento ({@code v[15]}), {@code null} se manca o non è positivo. */
        final BigDecimal valoreEuro;

        public Gamba(LocalDate giorno, Moneta moneta, BigDecimal qta) {
            this(giorno, moneta, qta, null);
        }

        public Gamba(LocalDate giorno, Moneta moneta, BigDecimal qta, BigDecimal valoreEuro) {
            this.giorno = giorno;
            this.moneta = moneta;
            this.chiave = chiave(moneta);
            this.qta = qta;
            this.valoreEuro = valoreEuro != null && valoreEuro.signum() > 0 ? valoreEuro : null;
        }
    }

    /**
     * Un token valorizzato al prezzo del suo primo ingresso perché senza prezzo al 1° gennaio. L'elenco serve a
     * identificarlo e a dargli a mano il prezzo del 1° gennaio.
     */
    public static final class PrezzoAcquisto {
        /** Il token, con address e rete come nei movimenti e {@code Qta} la giacenza massima in un giorno lavorativo. */
        public final Moneta moneta;
        /** Euro per una unità: valore del movimento di ingresso diviso la quantità. */
        public final BigDecimal prezzoUnitario;
        /** Giorno del movimento di ingresso da cui viene il prezzo. */
        public final LocalDate data;

        PrezzoAcquisto(Moneta moneta, BigDecimal prezzoUnitario, LocalDate data) {
            this.moneta = moneta;
            this.prezzoUnitario = prezzoUnitario;
            this.data = data;
        }
    }

    /** La giacenza di un giorno lavorativo, con il valore al prezzo del 1° gennaio. */
    public static final class Giorno {
        public final LocalDate data;
        public final BigDecimal valore;
        public final boolean sopraSoglia;

        Giorno(LocalDate data, BigDecimal valore) {
            this.data = data;
            this.valore = valore;
            this.sopraSoglia = valore.compareTo(SOGLIA) > 0;
        }
    }

    /** Esito dell'anno: la condizione, il periodo che l'ha fatta scattare e i dati per spiegarla all'utente. */
    public static final class Esito {
        /** Anno d'imposta controllato. */
        public final int anno;
        /** {@code true} se la giacenza ha superato la soglia per almeno sette giorni lavorativi continui. */
        public final boolean superata;
        /** Primo giorno lavorativo della prima serie che ha raggiunto i sette giorni, altrimenti {@code null}. */
        public final LocalDate inizioPeriodo;
        /** Settimo giorno lavorativo di quella serie, altrimenti {@code null}. */
        public final LocalDate finePeriodo;
        /** La serie più lunga di giorni lavorativi continui sopra soglia (0 se la soglia non è mai superata). */
        public final int serieMassima;
        /** Il valore più alto della giacenza in un giorno lavorativo. */
        public final BigDecimal valoreMassimo;
        /** Il giorno lavorativo di quel valore massimo ({@code null} se l'anno non ha giorni con giacenza). */
        public final LocalDate giornoMassimo;
        /** Tutti i giorni lavorativi dell'anno, in ordine, per la finestra di dettaglio. */
        public final List<Giorno> giorni;
        /** Simboli con giacenza positiva in un giorno lavorativo ma senza prezzo al 1° gennaio (valorizzati a zero). */
        public final List<String> tokenSenzaPrezzo;
        /**
         * Gli stessi token con tutto ciò che serve per assegnargli un prezzo (address, rete, tipo): uno per
         * simbolo+address+rete, con {@code Qta} la giacenza massima raggiunta in un giorno lavorativo. Stesso ordine
         * di {@link #tokenSenzaPrezzo} per simbolo.
         */
        public final List<Moneta> monetePrezzoMancante;
        /** I token valorizzati al prezzo di acquisto perché senza prezzo al 1° gennaio, per simbolo. */
        public final List<PrezzoAcquisto> prezzoDiAcquisto;

        Esito(int anno, LocalDate inizioPeriodo, LocalDate finePeriodo, int serieMassima, BigDecimal valoreMassimo,
                LocalDate giornoMassimo, List<Giorno> giorni, List<String> tokenSenzaPrezzo,
                List<Moneta> monetePrezzoMancante, List<PrezzoAcquisto> prezzoDiAcquisto) {
            this.anno = anno;
            this.superata = inizioPeriodo != null;
            this.inizioPeriodo = inizioPeriodo;
            this.finePeriodo = finePeriodo;
            this.serieMassima = serieMassima;
            this.valoreMassimo = valoreMassimo;
            this.giornoMassimo = giornoMassimo;
            this.giorni = giorni;
            this.tokenSenzaPrezzo = tokenSenzaPrezzo;
            this.monetePrezzoMancante = monetePrezzoMancante;
            this.prezzoDiAcquisto = prezzoDiAcquisto;
        }

        /** {@code true} se c'è almeno un token a cui l'utente può assegnare il prezzo del 1° gennaio. */
        public boolean haPrezziDaAssegnare() {
            return !monetePrezzoMancante.isEmpty() || !prezzoDiAcquisto.isEmpty();
        }

        /** {@code true} se qualche token in giacenza non aveva prezzo: l'esito potrebbe cambiare con il suo valore. */
        public boolean incompleto() {
            return !tokenSenzaPrezzo.isEmpty();
        }
    }

    /**
     * Esito dell'anno sui movimenti dell'archivio, con prezzi di mercato al 1° gennaio e festività italiane.
     *
     * @param anno anno d'imposta, al più {@link #ULTIMO_ANNO}
     */
    public static Esito Calcola(int anno) {
        return Calcola(anno, Calcoli_RW_Giacenze.PREZZO_MERCATO);
    }

    /** Come {@link #Calcola(int)}, con la fonte dei prezzi iniettata. */
    public static Esito Calcola(int anno, Calcoli_RW_Giacenze.PrezzoGiacenza prezzo) {
        return Calcola(anno, GambeDaArchivio(), prezzo, SogliaGiacenzaAnte2023::festivoItaliano);
    }

    /**
     * Le gambe non FIAT di tutti i movimenti dell'archivio, in ordine cronologico (la mappa è ordinata per ID,
     * che comincia con {@code yyyyMMddHHmm}).
     */
    static List<Gamba> GambeDaArchivio() {
        List<Gamba> gambe = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            LocalDate giorno = LocalDate.parse(v[1].substring(0, 10));
            Moneta[] monete = Funzioni.RitornaMoneteDaID(v[0]);
            BigDecimal valore = v.length > 15 ? quantita(v[15]) : null;
            for (Moneta m : monete) {
                if (contaQuantita(m)) {
                    BigDecimal q = quantita(m.Qta);
                    if (q != null) {
                        gambe.add(new Gamba(giorno, m, q, valore));
                    }
                }
            }
        }
        return gambe;
    }

    /**
     * Il calcolo vero e proprio, senza archivio né rete.
     *
     * @param anno   anno d'imposta
     * @param gambe  tutte le gambe dei movimenti fino a fine anno almeno, in ordine cronologico (le successive
     *               al 31 dicembre sono ignorate)
     * @param prezzo fonte dei prezzi: valore in euro della quantità di moneta all'istante chiesto, {@code null}
     *               se non esiste. Qui si chiede sempre una unità alle 00:00 del 1° gennaio
     * @param festivo giorni feriali da non contare come lavorativi (i sabati e le domeniche sono già esclusi)
     */
    public static Esito Calcola(int anno, List<Gamba> gambe, Calcoli_RW_Giacenze.PrezzoGiacenza prezzo,
            Predicate<LocalDate> festivo) {
        LocalDate primo = LocalDate.of(anno, 1, 1);
        LocalDate ultimo = LocalDate.of(anno, 12, 31);
        long istantePrezzo = FunzioniDate.ConvertiDatainLongMinuto(primo + " 00:00");

        Map<String, BigDecimal> saldi = new LinkedHashMap<>();
        Map<String, Moneta> monete = new HashMap<>();
        Map<String, BigDecimal> prezzi = new HashMap<>(); //chiave -> prezzo unitario (assente = non ancora cercato)
        TreeSet<String> senzaPrezzo = new TreeSet<>();
        Map<String, BigDecimal> qtaMassimaSenzaPrezzo = new LinkedHashMap<>(); //chiave -> giacenza massima, solo i senza prezzo
        Map<String, Gamba> ingressi = null; //chiave -> primo ingresso con valore, costruita alla prima necessità
        Map<String, BigDecimal> prezzoAcquisto = new HashMap<>(); //chiave -> prezzo unitario, solo quelli a prezzo di acquisto
        Map<String, LocalDate> dataAcquisto = new HashMap<>();
        Map<String, BigDecimal> qtaMassimaAcquisto = new LinkedHashMap<>(); //chiave -> giacenza massima, solo quelli a prezzo di acquisto
        List<Giorno> giorni = new ArrayList<>();

        int i = 0;
        // Le gambe prima del 1° gennaio formano il saldo di partenza.
        while (i < gambe.size() && gambe.get(i).giorno.isBefore(primo)) {
            applica(saldi, monete, gambe.get(i++));
        }

        int serie = 0;
        int serieMassima = 0;
        LocalDate inizioSerie = null;
        LocalDate inizioPeriodo = null;
        LocalDate finePeriodo = null;
        BigDecimal valoreMassimo = BigDecimal.ZERO;
        LocalDate giornoMassimo = null;

        for (LocalDate d = primo; !d.isAfter(ultimo); d = d.plusDays(1)) {
            while (i < gambe.size() && !gambe.get(i).giorno.isAfter(d)) {
                applica(saldi, monete, gambe.get(i++));
            }
            if (!lavorativo(d, festivo)) {
                continue;
            }
            BigDecimal valore = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> s : saldi.entrySet()) {
                if (s.getValue().signum() <= 0) {
                    continue;
                }
                BigDecimal unitario = prezzi.get(s.getKey());
                if (unitario == null) {
                    unitario = prezzoUnitario(monete.get(s.getKey()), prezzo, istantePrezzo);
                    if (unitario == null) {
                        if (ingressi == null) {
                            ingressi = primiIngressi(gambe, ultimo);
                        }
                        Gamba ingresso = ingressi.get(s.getKey());
                        if (ingresso != null) {
                            unitario = ingresso.valoreEuro.divide(ingresso.qta, 12, java.math.RoundingMode.HALF_UP);
                            prezzoAcquisto.put(s.getKey(), unitario);
                            dataAcquisto.put(s.getKey(), ingresso.giorno);
                            qtaMassimaAcquisto.put(s.getKey(), BigDecimal.ZERO);
                        } else {
                            senzaPrezzo.add(monete.get(s.getKey()).Moneta);
                            qtaMassimaSenzaPrezzo.put(s.getKey(), BigDecimal.ZERO);
                            unitario = BigDecimal.ZERO;
                        }
                    }
                    prezzi.put(s.getKey(), unitario);
                }
                if (qtaMassimaSenzaPrezzo.containsKey(s.getKey())) {
                    qtaMassimaSenzaPrezzo.merge(s.getKey(), s.getValue(), BigDecimal::max);
                }
                if (qtaMassimaAcquisto.containsKey(s.getKey())) {
                    qtaMassimaAcquisto.merge(s.getKey(), s.getValue(), BigDecimal::max);
                }
                valore = valore.add(s.getValue().multiply(unitario));
            }
            Giorno g = new Giorno(d, valore);
            giorni.add(g);
            if (giornoMassimo == null || valore.compareTo(valoreMassimo) > 0) {
                valoreMassimo = valore;
                giornoMassimo = d;
            }
            if (g.sopraSoglia) {
                if (serie == 0) {
                    inizioSerie = d;
                }
                serie++;
                serieMassima = Math.max(serieMassima, serie);
                if (serie == GIORNI_LAVORATIVI_CONTINUI && inizioPeriodo == null) {
                    inizioPeriodo = inizioSerie;
                    finePeriodo = d;
                }
            } else {
                serie = 0;
            }
        }
        // I token senza prezzo noti solo per simbolo: se due reti hanno lo stesso simbolo compare una volta.
        List<Moneta> mancanti = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : qtaMassimaSenzaPrezzo.entrySet()) {
            mancanti.add(monetaConQta(monete.get(e.getKey()), e.getValue()));
        }
        mancanti.sort((a, b) -> a.Moneta.compareToIgnoreCase(b.Moneta));
        List<PrezzoAcquisto> acquisti = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : qtaMassimaAcquisto.entrySet()) {
            acquisti.add(new PrezzoAcquisto(monetaConQta(monete.get(e.getKey()), e.getValue()),
                    prezzoAcquisto.get(e.getKey()), dataAcquisto.get(e.getKey())));
        }
        acquisti.sort((a, b) -> a.moneta.Moneta.compareToIgnoreCase(b.moneta.Moneta));
        return new Esito(anno, inizioPeriodo, finePeriodo, serieMassima, valoreMassimo, giornoMassimo,
                giorni, new ArrayList<>(senzaPrezzo), mancanti, acquisti);
    }

    private static Moneta monetaConQta(Moneta tipo, BigDecimal qta) {
        Moneta m = new Moneta();
        m.InserisciValori(tipo.Moneta, qta.stripTrailingZeros().toPlainString(), tipo.MonetaAddress, tipo.Tipo);
        m.Rete = tipo.Rete;
        return m;
    }

    /**
     * Il primo ingresso di ogni token con un valore in euro (quantità positiva e valore del movimento positivo), fino
     * al {@code limite} compreso: da lì viene il prezzo di acquisto. Le gambe sono in ordine cronologico.
     */
    private static Map<String, Gamba> primiIngressi(List<Gamba> gambe, LocalDate limite) {
        Map<String, Gamba> primi = new HashMap<>();
        for (Gamba g : gambe) {
            if (g.giorno.isAfter(limite)) {
                break;
            }
            if (g.qta.signum() > 0 && g.valoreEuro != null) {
                primi.putIfAbsent(g.chiave, g);
            }
        }
        return primi;
    }

    private static void applica(Map<String, BigDecimal> saldi, Map<String, Moneta> monete, Gamba g) {
        monete.putIfAbsent(g.chiave, g.moneta);
        saldi.merge(g.chiave, g.qta, BigDecimal::add);
    }

    /**
     * Prezzo di una unità del token, o {@code null} se la fonte non ne ha uno. <b>Zero è un prezzo</b>: è ciò che
     * l'utente assegna a un token senza alcuna quotazione (tecnico o senza valore), ed è ciò che lo toglie dai
     * "senza prezzo". Un prezzo negativo non esiste.
     */
    private static BigDecimal prezzoUnitario(Moneta tipo, Calcoli_RW_Giacenze.PrezzoGiacenza prezzo, long istante) {
        Moneta una = new Moneta();
        boolean valuta = "FIAT".equalsIgnoreCase(tipo.Tipo);
        //Il dollaro lo riconosce il prezzatore solo senza address (cambio Banca d'Italia)
        una.InserisciValori(tipo.Moneta, "1", valuta ? null : tipo.MonetaAddress, tipo.Tipo);
        una.Rete = valuta ? null : tipo.Rete;
        String p = prezzo.prezzo(una, istante);
        BigDecimal v = quantita(p);
        return v != null && v.signum() >= 0 ? v : null;
    }

    /** Lunedì-venerdì che non siano festivi. */
    static boolean lavorativo(LocalDate d, Predicate<LocalDate> festivo) {
        DayOfWeek w = d.getDayOfWeek();
        return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY && !festivo.test(d);
    }

    /**
     * Festività nazionali italiane: Capodanno, Epifania, Pasquetta, 25 aprile, 1° maggio, 2 giugno, Ferragosto,
     * Ognissanti, Immacolata, Natale, Santo Stefano. (La Pasqua cade di domenica e la esclude già il fine settimana.)
     */
    static boolean festivoItaliano(LocalDate d) {
        int m = d.getMonthValue();
        int g = d.getDayOfMonth();
        switch (m) {
            case 1: return g == 1 || g == 6;
            case 4: if (g == 25) return true; break;
            case 5: return g == 1;
            case 6: return g == 2;
            case 8: return g == 15;
            case 11: return g == 1;
            case 12: return g == 8 || g == 25 || g == 26;
            default: break;
        }
        return d.equals(pasqua(d.getYear()).plusDays(1));
    }

    /** Domenica di Pasqua (calendario gregoriano, algoritmo di Meeus/Jones/Butcher). */
    static LocalDate pasqua(int anno) {
        int a = anno % 19;
        int b = anno / 100;
        int c = anno % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int mm = (a + 11 * h + 22 * l) / 451;
        int mese = (h + l - 7 * mm + 114) / 31;
        int giorno = ((h + l - 7 * mm + 114) % 31) + 1;
        return LocalDate.of(anno, mese, giorno);
    }

    /**
     * La moneta pesa sulla soglia: c'è, ha una quantità e non è marcata SCAM (gli NFT contano). Fra le FIAT
     * conta solo il dollaro: l'euro non è una valuta estera e le altre valute non si usano.
     */
    private static boolean contaQuantita(Moneta m) {
        if (m == null || m.Moneta == null || m.Moneta.isBlank() || m.Qta == null || m.Qta.isBlank()) {
            return false;
        }
        if ("FIAT".equalsIgnoreCase(m.Tipo)) {
            return "USD".equalsIgnoreCase(m.Moneta.trim());
        }
        return !Funzioni.isSCAM(m.Moneta);
    }

    private static BigDecimal quantita(String testo) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(testo.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String chiave(Moneta m) {
        if ("FIAT".equalsIgnoreCase(m.Tipo)) {
            return m.Moneta.trim().toUpperCase(); //una valuta non ha address né rete
        }
        return m.Moneta + ";" + (m.MonetaAddress == null ? "" : m.MonetaAddress) + ";" + (m.Rete == null ? "" : m.Rete);
    }
}
