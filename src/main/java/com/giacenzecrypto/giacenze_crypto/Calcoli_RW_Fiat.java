/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Motore della <b>parte FIAT del quadro W/RW</b> : la giacenza in valuta (euro e valute estere)
 * detenuta presso un intermediario estero va monitorata in RW come "altre attività estere di
 * natura finanziaria – valute estere – depositi e conti correnti bancari costituiti all'estero"
 * (codice individuazione bene {@value #CODICE_BENE_FIAT}).
 *
 * <p>Companion di {@code Calcoli_RW} nello stile di {@link Principale_GruppiWalletRW} : metodi
 * {@code public static}, nessun campo Swing, nessun riferimento a {@code Principale} salvo la mappa
 * dei risultati. Invocato da {@code Calcoli_RW.AggiornaRWFR} <b>dopo</b> il calcolo della parte
 * CRYPTO e solo se l'opzione {@code RW_FiatInRW} è attiva (disattiva di default) ; i righi prodotti finiscono in
 * una mappa separata ({@code Principale.Mappa_RW_ListeXGruppoWallet_Fiat}, righe {@code String[18]})
 * così che il path CRYPTO — e il suo golden master — resti immutato.</p>
 *
 * <ul>
 *   <li><b>F1</b> {@link #saldiFiatPerValuta} / {@link #gambeFiatGiorno} / {@link #haMovimentiFiat} :
 *       saldo FIAT per valuta di un gruppo a una certa data e gambe FIAT di una giornata.</li>
 *   <li><b>F2</b> {@link #intervalliFiat} : i tratti di detenzione dell'anno, tagliati da
 *       {@code EXCHANGE_PERIODO} e dai periodi {@code GRUPPO_PERIODO_RW} FIAT.</li>
 *   <li><b>F3</b> {@link #generaRighiFiat} : un rigo per tratto con valore ≠ 0.</li>
 * </ul>
 *
 * <h3>Modello dei dati</h3>
 * Un movimento è un {@code String[]} di {@link Importazioni#ColonneTabella} colonne : gamba in
 * <b>uscita</b> in {@code v[8..10]} (simbolo / tipo / quantità), gamba in <b>entrata</b> in
 * {@code v[11..13]}. La <b>direzione</b> di una gamba è data dalla <i>posizione</i>, non dal segno
 * della quantità (che a seconda dell'importatore può essere memorizzata con o senza segno) : qui
 * la gamba di uscita sottrae {@code abs(qta)}, quella di entrata somma {@code abs(qta)}.
 * I trasferimenti interni allo stesso wallet (categoria {@code TI}) sono esclusi ; un giroconto di
 * valuta fra due exchange diversi resta invece composto da due movimenti ({@code PF} + {@code DF})
 * su gruppi diversi, e la somma per gruppo lo assorbe da sola.
 */
public final class Calcoli_RW_Fiat {

    private Calcoli_RW_Fiat() {
    }

    /** Codice individuazione bene per il rigo RW/W della valuta estera presso intermediario estero. */
    public static final String CODICE_BENE_FIAT = "14";

    /** Causale scritta nel campo 12 dei righi FIAT ({@code String[18]}). */
    public static final String CAUSALE_PERIODO = "Periodo FIAT";

    /** Tipo di moneta che identifica una gamba FIAT nel modello dei movimenti. */
    static final String TIPO_FIAT = "FIAT";

    /** Categoria dei trasferimenti interni allo stesso wallet : niente RW, niente saldo FIAT. */
    static final String CATEGORIA_TRASFERIMENTO_INTERNO = "TI";

    // =======================================================================
    // Indice delle gambe FIAT (una gamba = un lato FIAT di un movimento)
    // =======================================================================

    /** Un lato FIAT di un movimento : giorno ISO, valuta (maiuscolo), importo firmato (uscita &lt; 0), id movimento. */
    private static final class GambaFiat {
        final String giorno;
        final String valuta;
        final BigDecimal importo;
        final String id;

        GambaFiat(String giorno, String valuta, BigDecimal importo, String id) {
            this.giorno = giorno;
            this.valuta = valuta;
            this.importo = importo;
            this.id = id;
        }
    }

    /**
     * Tutte le gambe FIAT di {@code MappaCryptoWallet} raggruppate per gruppo wallet, in <b>una sola
     * passata</b>. Ogni lista è ordinata per id movimento (= cronologico). I trasferimenti interni
     * ({@code TI}) sono esclusi. Un gruppo senza gambe FIAT non compare nella mappa. Usato da
     * {@link #generaRighiFiat}, che così non riscorre l'archivio per ogni gruppo e per ogni tratto.
     */
    private static Map<String, List<GambaFiat>> indicizzaGambeFiat() {
        Map<String, List<GambaFiat>> indice = new HashMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!movimentoUtile(v)) {
                continue;
            }
            String giorno = v[1].substring(0, 10);
            String gruppo = DatabaseH2.Pers_GruppoWallet_Leggi(v[3].trim(), true);
            List<GambaFiat> gambe = new ArrayList<>();
            aggiungiSeFiat(gambe, giorno, v[0], v[8], v[9], v[10], false);
            aggiungiSeFiat(gambe, giorno, v[0], v[11], v[12], v[13], true);
            if (!gambe.isEmpty()) {
                indice.computeIfAbsent(gruppo, k -> new ArrayList<>()).addAll(gambe);
            }
        }
        for (List<GambaFiat> gambe : indice.values()) {
            gambe.sort(Comparator.comparing(g -> g.id));
        }
        return indice;
    }

    /**
     * Le gambe FIAT di un <b>singolo</b> gruppo, per i chiamanti pubblici che non hanno l'indice
     * completo. Ricava la vista dall'unico costruttore {@link #indicizzaGambeFiat()} (una passata
     * sull'archivio, come facevano già questi metodi prima dell'indicizzazione) : così esiste una
     * sola implementazione del filtro FIAT e non può divergere fra il path pubblico e {@code F3}.
     */
    private static List<GambaFiat> gambeDelGruppo(String gruppo) {
        if (gruppo == null || gruppo.isBlank()) {
            return new ArrayList<>();
        }
        return indicizzaGambeFiat().getOrDefault(gruppo, new ArrayList<>());
    }

    /** {@code true} se il movimento ha data e wallet valorizzati e non è un trasferimento interno. */
    private static boolean movimentoUtile(String[] v) {
        return v != null && v.length > 13 && v[3] != null && !v[3].isBlank()
                && v[1] != null && v[1].length() >= 10 && v[0] != null
                && !isTrasferimentoInterno(v);
    }

    private static void aggiungiSeFiat(List<GambaFiat> gambe, String giorno, String id,
            String simbolo, String tipo, String qta, boolean entrata) {
        if (!gambaFiatValida(tipo, simbolo, qta)) {
            return;
        }
        BigDecimal importo = absOrNull(qta);
        if (importo == null) {
            return;
        }
        gambe.add(new GambaFiat(giorno, trim(simbolo).toUpperCase(),
                entrata ? importo : importo.negate(), id));
    }

    /** Saldo firmato per valuta da una lista di gambe già filtrata per gruppo. */
    private static Map<String, BigDecimal> saldiDaGambe(List<GambaFiat> gambe, String giornoLimite, boolean inclusivo) {
        Map<String, BigDecimal> saldi = new TreeMap<>();
        for (GambaFiat g : gambe) {
            int cmp = g.giorno.compareTo(giornoLimite);
            if (inclusivo ? cmp <= 0 : cmp < 0) {
                saldi.merge(g.valuta, g.importo, BigDecimal::add);
            }
        }
        saldi.values().removeIf(bd -> bd.signum() == 0);
        return saldi;
    }

    /** Sottoinsieme delle gambe di una giornata (la lista sorgente è già ordinata per id). */
    private static List<GambaFiat> gambeDelGiorno(List<GambaFiat> gambe, String giorno) {
        List<GambaFiat> out = new ArrayList<>();
        for (GambaFiat g : gambe) {
            if (g.giorno.equals(giorno)) {
                out.add(g);
            }
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // F1 : API pubblica di saldo (sottili wrapper sull'indice di gruppo)
    // -----------------------------------------------------------------------

    /**
     * Giacenza FIAT <b>firmata per valuta</b> di un gruppo wallet, considerando i movimenti fino a
     * una certa giornata.
     *
     * @param gruppo          nome del gruppo wallet (come da {@code DatabaseH2.Pers_GruppoWallet_Leggi})
     * @param giornoLimiteIso giornata di riferimento, formato {@code yyyy-MM-dd}
     * @param inclusivo       {@code true} = saldo a <i>fine</i> di {@code giornoLimiteIso} ; {@code false}
     *                        = saldo a <i>inizio</i> giornata, cioè il "residuo" (solo giorni precedenti)
     * @return mappa {@code valuta → saldo} (solo valute con saldo diverso da zero)
     */
    public static Map<String, BigDecimal> saldiFiatPerValuta(String gruppo, String giornoLimiteIso, boolean inclusivo) {
        if (gruppo == null || gruppo.isBlank() || giornoLimiteIso == null || giornoLimiteIso.length() < 10) {
            return new TreeMap<>();
        }
        return saldiDaGambe(gambeDelGruppo(gruppo), giornoLimiteIso.substring(0, 10), inclusivo);
    }

    /**
     * Elenco delle <b>gambe FIAT</b> di un gruppo in una singola giornata, in ordine cronologico (per
     * id movimento). Un movimento può contribuire con due voci (es. uno scambio EUR↔USD).
     *
     * @return lista di {@code [idMovimento, valuta, importoFirmato]} ; {@code importoFirmato} &lt; 0 = uscita, &gt; 0 = apporto
     */
    public static List<String[]> gambeFiatGiorno(String gruppo, String giornoIso) {
        List<String[]> out = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank() || giornoIso == null || giornoIso.length() < 10) {
            return out;
        }
        for (GambaFiat g : gambeDelGiorno(gambeDelGruppo(gruppo), giornoIso.substring(0, 10))) {
            out.add(new String[] {g.id, g.valuta, g.importo.toPlainString()});
        }
        return out;
    }

    /** {@code true} se il gruppo ha almeno una gamba FIAT (categoria diversa da {@code TI}) in qualunque data. */
    public static boolean haMovimentiFiat(String gruppo) {
        return !gambeDelGruppo(gruppo).isEmpty();
    }

    // =======================================================================
    // F2 : intervalli di detenzione FIAT (tagli da EXCHANGE_PERIODO + GRUPPO_PERIODO_RW)
    // =======================================================================

    /** Colonne di una riga di {@link #intervalliFiat}. */
    public static final int IV_DATA_INIZIO = 0, IV_DATA_FINE = 1, IV_STATO = 2,
            IV_MOD_INIZIALE = 3, IV_MOD_FINALE = 4,
            IV_VAL_INIZIALE_MAN = 5, IV_VAL_FINALE_MAN = 6,
            IV_NOTA_INIZIALE = 7, IV_NOTA_FINALE = 8;
    public static final int IV_COLONNE = 9;

    /**
     * Spezza l'anno di riferimento nei tratti di detenzione FIAT di un gruppo wallet.
     *
     * <p>Le <b>date di taglio</b> interne all'anno vengono da due sorgenti :
     * <ul>
     *   <li>i periodi {@code EXCHANGE_PERIODO} dell'exchange di riferimento — <b>solo</b> se il
     *       gruppo è in modalità {@code EXCHANGE} (in modalità {@code STATO} lo Stato è fisso) ;</li>
     *   <li>i periodi {@code GRUPPO_PERIODO_RW} di tipo {@code FIAT} del gruppo.</li>
     * </ul>
     * Ogni {@code DataInizio} dentro l'anno apre un tratto ; ogni {@code DataFine} ne chiude uno. Un
     * periodo FIAT con <b>entrambe</b> le date vuote copre tutto l'anno e porta le sue modalità /
     * valori manuali agli estremi (1° gennaio e 31 dicembre) ; i periodi datati vincono sulle loro
     * finestre.</p>
     *
     * <p>Per ogni tratto : {@code IV_STATO} = {@link Principale_GruppiWalletRW#statoEsteroEffettivo}
     * alla data di inizio ; {@code IV_MOD_INIZIALE} / {@code IV_VAL_INIZIALE_MAN} presi dal periodo
     * FIAT il cui {@code DataInizio} coincide con l'inizio del tratto (confine definito dall'utente),
     * assenti sui tagli da cambio Stato o al 1° gennaio (F3 userà la giacenza pura) ;
     * {@code IV_MOD_FINALE} / {@code IV_VAL_FINALE_MAN} idem dal periodo il cui {@code DataFine}
     * coincide con la fine del tratto. Un periodo manuale spezzato da un cambio Stato mantiene le
     * modalità iniziali sul primo tratto e quelle finali sull'ultimo, col taglio interno "nudo".</p>
     *
     * <p><b>Non</b> guarda i movimenti : senza tagli torna un solo tratto sull'intero anno.</p>
     *
     * @return tratti contigui che coprono {@code [anno-01-01, anno-12-31]}, in ordine di data
     *         ({@link #IV_COLONNE} colonne) ; lista vuota se {@code gruppo}/{@code anno} non validi
     */
    public static List<String[]> intervalliFiat(String gruppo, String anno) {
        List<String[]> out = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank() || anno == null || anno.length() != 4) {
            return out;
        }
        final LocalDate annoInizio;
        final LocalDate annoFine;
        try {
            annoInizio = LocalDate.parse(anno + "-01-01");
            annoFine = LocalDate.parse(anno + "-12-31");
        } catch (DateTimeParseException e) {
            return out;
        }

        List<String[]> periodiFiat = new ArrayList<>();
        for (String[] r : Principale_GruppiWalletRW.caricaPeriodi(gruppo)) {
            if (Principale_GruppiWalletRW.TIPO_FIAT.equals(trim(r[Principale_GruppiWalletRW.COL_TIPO]))) {
                periodiFiat.add(r);
            }
        }

        List<String[]> periodiExchange = new ArrayList<>();
        String[] rif = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (rif != null && Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE.equals(rif[1])
                && rif[4] != null && !rif[4].isBlank()) {
            periodiExchange = Principale_PeriodiExchange.caricaPeriodi(rif[4].trim());
        }

        TreeSet<LocalDate> tagli = new TreeSet<>();
        for (String[] r : periodiFiat) {
            aggiungiTagli(tagli, r[Principale_GruppiWalletRW.COL_DATA_INIZIO],
                    r[Principale_GruppiWalletRW.COL_DATA_FINE], annoInizio, annoFine);
        }
        for (String[] r : periodiExchange) {
            aggiungiTagli(tagli, r[Principale_PeriodiExchange.COL_DATA_INIZIO],
                    r[Principale_PeriodiExchange.COL_DATA_FINE], annoInizio, annoFine);
        }

        List<LocalDate> inizi = new ArrayList<>();
        inizi.add(annoInizio);
        inizi.addAll(tagli);
        for (int i = 0; i < inizi.size(); i++) {
            LocalDate di = inizi.get(i);
            LocalDate df = i + 1 < inizi.size() ? inizi.get(i + 1).minusDays(1) : annoFine;

            String[] iv = new String[IV_COLONNE];
            Arrays.fill(iv, "");
            iv[IV_DATA_INIZIO] = di.toString();
            iv[IV_DATA_FINE] = df.toString();
            iv[IV_STATO] = Principale_GruppiWalletRW.statoEsteroEffettivo(gruppo, di);

            String[] pIni = periodoConBordo(periodiFiat, Principale_GruppiWalletRW.COL_DATA_INIZIO,
                    di, di.equals(annoInizio));
            if (pIni != null) {
                iv[IV_MOD_INIZIALE] = trim(pIni[Principale_GruppiWalletRW.COL_MOD_INIZIALE]);
                iv[IV_VAL_INIZIALE_MAN] = trim(pIni[Principale_GruppiWalletRW.COL_VAL_INIZIALE]);
                iv[IV_NOTA_INIZIALE] = trim(pIni[Principale_GruppiWalletRW.COL_NOTA_INIZIALE]);
            }
            String[] pFin = periodoConBordo(periodiFiat, Principale_GruppiWalletRW.COL_DATA_FINE,
                    df, df.equals(annoFine));
            if (pFin != null) {
                iv[IV_MOD_FINALE] = trim(pFin[Principale_GruppiWalletRW.COL_MOD_FINALE]);
                iv[IV_VAL_FINALE_MAN] = trim(pFin[Principale_GruppiWalletRW.COL_VAL_FINALE]);
                iv[IV_NOTA_FINALE] = trim(pFin[Principale_GruppiWalletRW.COL_NOTA_FINALE]);
            }
            out.add(iv);
        }
        return out;
    }

    /**
     * Aggiunge a {@code tagli} l'inizio di un periodo (se cade dentro {@code (annoInizio, annoFine]})
     * e il giorno successivo alla sua fine (se cade dentro {@code [annoInizio, annoFine)}).
     */
    private static void aggiungiTagli(TreeSet<LocalDate> tagli, String inizioStr, String fineStr,
            LocalDate annoInizio, LocalDate annoFine) {
        LocalDate di = parseData(inizioStr);
        if (di != null && di.isAfter(annoInizio) && !di.isAfter(annoFine)) {
            tagli.add(di);
        }
        LocalDate df = parseData(fineStr);
        if (df != null && !df.isBefore(annoInizio) && df.isBefore(annoFine)) {
            tagli.add(df.plusDays(1));
        }
    }

    /**
     * Il periodo FIAT il cui bordo (inizio o fine, secondo {@code col}) coincide con {@code data} —
     * cioè un confine definito dall'utente. Se {@code ammettiAperto} e nessuno coincide, ripiega sul
     * periodo FIAT interamente aperto (entrambe le date vuote), che porta modalità / valori manuali
     * agli estremi dell'anno.
     */
    private static String[] periodoConBordo(List<String[]> periodiFiat, int col, LocalDate data, boolean ammettiAperto) {
        for (String[] r : periodiFiat) {
            LocalDate bordo = parseData(r[col]);
            if (bordo != null && bordo.equals(data)) {
                return r;
            }
        }
        if (ammettiAperto) {
            for (String[] r : periodiFiat) {
                if (parseData(r[Principale_GruppiWalletRW.COL_DATA_INIZIO]) == null
                        && parseData(r[Principale_GruppiWalletRW.COL_DATA_FINE]) == null) {
                    return r;
                }
            }
        }
        return null;
    }

    private static LocalDate parseData(String v) {
        String s = trim(v);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // helper comuni
    // -----------------------------------------------------------------------

    private static boolean isTrasferimentoInterno(String[] v) {
        return CATEGORIA_TRASFERIMENTO_INTERNO.equals(categoria(v));
    }

    /** Ultimo segmento dell'ID movimento ({@code v[0]}), cioè la categoria fiscale (es. {@code DF}, {@code AC}). */
    private static String categoria(String[] v) {
        String id = v[0];
        if (id == null) {
            return "";
        }
        int i = id.lastIndexOf('_');
        return i < 0 ? "" : id.substring(i + 1);
    }

    private static boolean gambaFiatValida(String tipo, String simbolo, String qta) {
        return TIPO_FIAT.equalsIgnoreCase(trim(tipo)) && !trim(simbolo).isEmpty() && !trim(qta).isEmpty();
    }

    /** {@code abs} del valore, tollerante alla notazione scientifica ({@code 2.5E-9}) ; {@code null} se non numerico. */
    private static BigDecimal absOrNull(String qta) {
        try {
            return new BigDecimal(qta.trim()).abs();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** Importo decimale, o {@code null} se vuoto/non numerico. */
    private static BigDecimal importoONull(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =======================================================================
    // F3 : generazione dei righi FIAT (mappa separata Mappa_RW_ListeXGruppoWallet_Fiat)
    // =======================================================================

    /** Colonna extra di un rigo FIAT rispetto al {@code String[17]} del rigo CRYPTO : codice Stato estero. */
    public static final int FIAT_COL_STATO_ESTERO = 17;
    /** Lunghezza di un rigo FIAT ({@code String[18]}). Le prime 17 colonne hanno lo stesso significato del rigo CRYPTO. */
    public static final int FIAT_COLONNE = 18;

    /**
     * Controvalore in EUR di un importo in una valuta a una certa data. Iniettabile per i test :
     * l'implementazione di produzione ({@link #controvaloreBancaItalia}) usa il cambio Banca d'Italia.
     */
    @FunctionalInterface
    public interface ControvaloreEUR {
        /**
         * @param valuta  simbolo valuta ("EUR", "USD", ...)
         * @param importo importo firmato in quella valuta
         * @param dataIso data del cambio, formato {@code yyyy-MM-dd}
         * @return valore in EUR, oppure {@code null} se la valuta non è convertibile (→ segnalata a parte)
         */
        BigDecimal eur(String valuta, BigDecimal importo, String dataIso);
    }

    /** Cambio di produzione : EUR 1:1, USD via {@link Prezzi#CambioUSDEUR}, altre valute non convertibili in v1. */
    public static BigDecimal controvaloreBancaItalia(String valuta, BigDecimal importo, String dataIso) {
        String v = trim(valuta).toUpperCase();
        if (importo == null) {
            return null;
        }
        if (v.isEmpty() || "EUR".equals(v)) {
            return importo;
        }
        if ("USD".equals(v)) {
            String eur = Prezzi.CambioUSDEUR(importo.toPlainString(), dataIso);
            if (eur == null) {
                return null;
            }
            try {
                return new BigDecimal(eur);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Ricostruisce da zero {@code Principale.Mappa_RW_ListeXGruppoWallet_Fiat} per l'anno indicato :
     * per ogni gruppo wallet con movimenti FIAT, un rigo {@code String[18]} per ogni tratto di
     * {@link #intervalliFiat(String, String)} il cui valore iniziale o finale non è nullo.
     *
     * <p>Valore iniziale del tratto : il valore manuale del periodo FIAT se impostato ; altrimenti,
     * su un confine definito dall'utente, la modalità di calcolo (residuo + primo apporto / somma
     * apporti della giornata) ; altrimenti la <b>giacenza pura</b> a inizio giornata (residuo). Valore
     * finale : simmetrico (giacenza pura a fine giornata, oppure {@code Sfin + ultima uscita /
     * + somma uscite}). Il primo tratto parte dalla data del primo movimento del gruppo se questo
     * cade nell'anno (apertura infra-anno). Un valore negativo è portato a zero con un avviso ; un
     * tratto con valore iniziale e finale <i>genuinamente</i> nulli non produce rigo.</p>
     *
     * @param anno anno di riferimento, formato {@code yyyy}
     */
    public static void generaRighiFiat(String anno) {
        generaRighiFiat(anno, Calcoli_RW_Fiat::controvaloreBancaItalia);
    }

    /** Come {@link #generaRighiFiat(String)} ma con cambio EUR iniettabile (per i test). */
    public static void generaRighiFiat(String anno, ControvaloreEUR cambio) {
        Principale.Mappa_RW_ListeXGruppoWallet_Fiat.clear();
        if (anno == null || anno.length() != 4 || cambio == null) {
            return;
        }

        Map<String, List<GambaFiat>> indice = indicizzaGambeFiat();
        Map<String, String> primoMovXGruppo = Funzioni.MappaPrimoMovimentoXGruppoWallet();

        for (Map.Entry<String, List<GambaFiat>> voce : indice.entrySet()) {
            String gruppo = voce.getKey();
            List<GambaFiat> gambe = voce.getValue();

            List<String[]> intervalli = intervalliFiat(gruppo, anno);
            LocalDate aperturaGruppo = dataAperturaGruppo(primoMovXGruppo.get(gruppo), anno);

            List<String[]> righe = new ArrayList<>();
            for (int i = 0; i < intervalli.size(); i++) {
                String[] iv = intervalli.get(i);
                LocalDate di = parseData(iv[IV_DATA_INIZIO]);
                LocalDate df = parseData(iv[IV_DATA_FINE]);
                if (di == null || df == null) {
                    continue;
                }
                boolean aperturaInfraAnno = i == 0 && aperturaGruppo != null && aperturaGruppo.isAfter(di);
                if (aperturaInfraAnno) {
                    di = aperturaGruppo;
                }
                if (df.isBefore(di)) {
                    continue; // tratto svuotato dallo spostamento dell'apertura
                }

                List<String> avvisi = new ArrayList<>();
                BigDecimal valIni = valoreIniziale(gambe, iv, di, aperturaInfraAnno, cambio, avvisi);
                BigDecimal valFin = valoreFinale(gambe, iv, df, cambio, avvisi);

                boolean negativo = valIni.signum() < 0 || valFin.signum() < 0;
                // Salto il tratto solo se i due estremi sono GENUINAMENTE nulli (punto 14) : un saldo
                // negativo viene invece portato a zero ma il rigo resta, con l'avviso.
                if (!negativo && valIni.signum() == 0 && valFin.signum() == 0) {
                    continue;
                }
                if (negativo) {
                    if (valIni.signum() < 0) {
                        valIni = BigDecimal.ZERO;
                    }
                    if (valFin.signum() < 0) {
                        valFin = BigDecimal.ZERO;
                    }
                    avvisi.add("saldo FIAT negativo nel periodo, valorizzato a zero");
                }

                int giorni = FunzioniDate.DifferenzaDate(di.toString(), df.toString()) + 1;
                righe.add(rigaFiat(anno, gruppo, di, df, valIni, valFin, giorni,
                        trim(iv[IV_STATO]), etichettaValute(gambe, df), avvisi));
            }
            if (!righe.isEmpty()) {
                Principale.Mappa_RW_ListeXGruppoWallet_Fiat.put(gruppo, righe);
            }
        }
    }

    // --- valore iniziale / finale di un tratto ---------------------------

    private static BigDecimal valoreIniziale(List<GambaFiat> gambe, String[] iv, LocalDate di,
            boolean aperturaInfraAnno, ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal manuale = importoONull(iv[IV_VAL_INIZIALE_MAN]);
        if (manuale != null) {
            return manuale;
        }
        if (aperturaInfraAnno) {
            // il gruppo apre qui : nessun residuo, valore = saldo a fine del primo giorno di detenzione
            return saldoEUR(gambe, di.toString(), true, di.toString(), cambio, avvisi);
        }
        BigDecimal residuo = saldoEUR(gambe, di.toString(), false, di.toString(), cambio, avvisi);
        String mod = trim(iv[IV_MOD_INIZIALE]);
        if (mod.isEmpty()) {
            return residuo; // giacenza pura
        }
        return residuo.add(apportiGiornoEUR(gambe, di.toString(), mod, cambio, avvisi));
    }

    private static BigDecimal valoreFinale(List<GambaFiat> gambe, String[] iv, LocalDate df,
            ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal manuale = importoONull(iv[IV_VAL_FINALE_MAN]);
        if (manuale != null) {
            return manuale;
        }
        BigDecimal sfin = saldoEUR(gambe, df.toString(), true, df.toString(), cambio, avvisi);
        String mod = trim(iv[IV_MOD_FINALE]);
        if (mod.isEmpty()) {
            return sfin; // giacenza pura
        }
        return sfin.add(usciteGiornoEUR(gambe, df.toString(), mod, cambio, avvisi));
    }

    /** Controvalore in EUR del saldo FIAT del gruppo a inizio ({@code inclusivo=false}) o fine ({@code true}) giornata. */
    private static BigDecimal saldoEUR(List<GambaFiat> gambe, String giornoIso, boolean inclusivo,
            String dataCambioIso, ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal tot = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : saldiDaGambe(gambe, giornoIso, inclusivo).entrySet()) {
            tot = tot.add(converti(e.getKey(), e.getValue(), dataCambioIso, cambio, avvisi));
        }
        return tot;
    }

    /** Somma degli apporti (gambe FIAT positive) della giornata : primo apporto o tutti, secondo la modalità. */
    private static BigDecimal apportiGiornoEUR(List<GambaFiat> gambe, String giornoIso, String modalita,
            ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal tot = BigDecimal.ZERO;
        boolean soloPrimo = Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO.equals(modalita);
        for (GambaFiat g : gambeDelGiorno(gambe, giornoIso)) {
            if (g.importo.signum() <= 0) {
                continue; // solo apporti
            }
            tot = tot.add(converti(g.valuta, g.importo, giornoIso, cambio, avvisi));
            if (soloPrimo) {
                break;
            }
        }
        return tot;
    }

    /** Riaccredito delle uscite (gambe FIAT negative) della giornata : ultima uscita o somma di tutte. */
    private static BigDecimal usciteGiornoEUR(List<GambaFiat> gambe, String giornoIso, String modalita,
            ControvaloreEUR cambio, List<String> avvisi) {
        boolean soloUltima = Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA.equals(modalita);
        if (soloUltima) {
            GambaFiat ultima = null;
            for (GambaFiat g : gambeDelGiorno(gambe, giornoIso)) {
                if (g.importo.signum() < 0) {
                    ultima = g;
                }
            }
            return ultima == null ? BigDecimal.ZERO
                    : converti(ultima.valuta, ultima.importo.abs(), giornoIso, cambio, avvisi);
        }
        BigDecimal tot = BigDecimal.ZERO;
        for (GambaFiat g : gambeDelGiorno(gambe, giornoIso)) {
            if (g.importo.signum() < 0) {
                tot = tot.add(converti(g.valuta, g.importo.abs(), giornoIso, cambio, avvisi));
            }
        }
        return tot;
    }

    private static BigDecimal converti(String valuta, BigDecimal importo, String dataIso,
            ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal eur = cambio.eur(valuta, importo, dataIso);
        if (eur == null) {
            avvisi.add("valuta " + trim(valuta).toUpperCase() + " non convertita in EUR");
            return BigDecimal.ZERO;
        }
        return eur;
    }

    // --- costruzione del rigo -------------------------------------------

    private static String[] rigaFiat(String anno, String gruppo, LocalDate di, LocalDate df,
            BigDecimal valIni, BigDecimal valFin, int giorni, String statoEstero, String etichettaValute,
            List<String> avvisi) {
        String[] r = new String[FIAT_COLONNE];
        Arrays.fill(r, "");
        r[0] = anno;
        r[1] = gruppo;                         // gruppo wallet inizio
        r[2] = etichettaValute;                // "moneta" inizio (etichetta valute del rigo)
        r[3] = valIni.toPlainString();         // "qta" inizio ~ valore
        r[4] = di + " 00:00";                  // data inizio
        r[5] = valIni.toPlainString();         // valore iniziale
        r[6] = gruppo;                         // gruppo wallet fine
        r[7] = etichettaValute;                // "moneta" fine
        r[8] = valFin.toPlainString();         // "qta" fine
        r[9] = df + " 23:59";                  // data fine
        r[10] = valFin.toPlainString();        // valore finale
        r[11] = String.valueOf(giorni);       // giorni di detenzione
        r[12] = CAUSALE_PERIODO;              // causale
        r[13] = "";                           // ID movimento apertura (nessuno : è un periodo)
        r[14] = "";                           // ID movimento chiusura
        r[15] = avvisi.isEmpty() ? "" : "Avviso (" + String.join("; ", new LinkedHashSet<>(avvisi)) + ")";
        r[16] = "";                           // lista ID coinvolti
        r[FIAT_COL_STATO_ESTERO] = statoEstero == null ? "" : statoEstero;
        return r;
    }

    /** Codici valuta presenti nel saldo FIAT del gruppo alla data, uniti da "+" (fallback "EUR"). */
    private static String etichettaValute(List<GambaFiat> gambe, LocalDate alGiorno) {
        StringBuilder sb = new StringBuilder();
        for (String valuta : saldiDaGambe(gambe, alGiorno.toString(), true).keySet()) {
            sb.append(sb.length() == 0 ? "" : "+").append(valuta);
        }
        return sb.length() == 0 ? "EUR" : sb.toString();
    }

    /** Data del primo movimento del gruppo se cade nell'anno indicato (apertura infra-anno), altrimenti {@code null}. */
    private static LocalDate dataAperturaGruppo(String idPrimoMovimento, String anno) {
        if (idPrimoMovimento == null) {
            return null;
        }
        String[] mov = MappaCryptoWallet.get(idPrimoMovimento);
        if (mov == null || mov[1] == null || mov[1].length() < 10) {
            return null;
        }
        String giorno = mov[1].substring(0, 10);
        if (!giorno.startsWith(anno + "-")) {
            return null;
        }
        return parseData(giorno);
    }
}
