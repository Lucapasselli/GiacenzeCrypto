/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
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
 *   <li><b>F2</b> {@link #intervalliFiat} : i tratti di detenzione dell'anno, tagliati dai periodi
 *       {@code GRUPPO_PERIODO_RW} di tipo FIAT (che dal 2026-09-09 portano anche i dati fiscali).</li>
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
     * L'indice delle gambe FIAT più gli avvisi maturati costruendolo, per gruppo wallet. Gli avvisi
     * riguardano il gruppo nel suo insieme (non un tratto) e vengono premessi agli avvisi di ogni
     * rigo che quel gruppo produce, perché un movimento non contabilizzato sposta il saldo e chi
     * legge il rigo deve poterlo sapere.
     */
    private static final class IndiceFiat {
        final Map<String, List<GambaFiat>> gambe = new HashMap<>();
        final Map<String, List<String>> avvisi = new HashMap<>();

        void avvisa(String gruppo, String avviso) {
            avvisi.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(avviso);
        }
    }

    /**
     * Tutte le gambe FIAT raggruppate per gruppo wallet, in <b>una sola passata</b> su
     * {@code MappaCryptoWallet}. Ogni lista è ordinata per id movimento (= cronologico). I
     * trasferimenti interni ({@code TI}) sono esclusi. Un gruppo senza gambe FIAT non compare nella
     * mappa. Usato da {@link #generaRighiFiat}, che così non riscorre l'archivio per ogni gruppo e
     * per ogni tratto.
     *
     * <p><b>Unica eccezione alla regola : Crypto.com App.</b> Per quell'exchange la parte FIAT non
     * si legge dai movimenti crypto ma dalla tabella del <i>Fiat Wallet</i>
     * ({@code crypto.com.fiatwallet.db}), che è il libro mastro del conto in euro — vedi
     * {@link #gambeDalFiatWalletCDC}. Le gambe FIAT dei movimenti di {@code Crypto.com App} sono
     * quindi scartate qui e rimpiazzate da quelle sintetizzate dal Fiat Wallet.</p>
     */
    private static IndiceFiat indicizzaGambeFiat() {
        IndiceFiat indice = new IndiceFiat();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!movimentoUtile(v)) {
                continue;
            }
            if (CDC_FiatECardWallet.NOME_EXCHANGE_CDC_APP.equals(v[3].trim())) {
                continue; // la parte FIAT arriva dal Fiat Wallet, vedi sotto
            }
            String giorno = v[1].substring(0, 10);
            String gruppo = DatabaseH2.Pers_GruppoWallet_Leggi(v[3].trim(), true);
            List<GambaFiat> gambe = new ArrayList<>();
            aggiungiSeFiat(gambe, giorno, v[0], v[8], v[9], v[10], false);
            aggiungiSeFiat(gambe, giorno, v[0], v[11], v[12], v[13], true);
            if (!gambe.isEmpty()) {
                indice.gambe.computeIfAbsent(gruppo, k -> new ArrayList<>()).addAll(gambe);
            }
        }
        gambeDalFiatWalletCDC(indice);
        for (List<GambaFiat> gambe : indice.gambe.values()) {
            gambe.sort(Comparator.comparing(g -> g.id));
        }
        return indice;
    }

    /**
     * Aggiunge all'indice le gambe FIAT di <b>Crypto.com App</b> lette dal Fiat Wallet.
     *
     * <p>L'esclusione a monte è per <b>nome exchange</b> ({@code v[3]}) e non per gruppo wallet : se
     * l'utente ha messo Crypto.com App nello stesso gruppo di un altro exchange, escludere per
     * gruppo cancellerebbe anche le gambe FIAT di quell'altro. Il gruppo resta invece la chiave a cui
     * le gambe sintetizzate vengono attribuite, esattamente come per gli altri exchange.</p>
     *
     * <p>Tre cose che il Fiat Wallet impone :</p>
     * <ul>
     *   <li><b>Solo euro.</b> Una riga senza colonna in EUR non è contabilizzata dal tab e non lo è
     *       nemmeno qui : viene scartata (non valorizzata a zero) e conteggiata in un avviso.
     *       Le gambe prodotte hanno quindi tutte valuta {@code EUR}.</li>
     *   <li><b>Il segno viene dal tipo movimento</b>, mai dalla quantità : l'importatore rende
     *       positiva la colonna dell'importo. Un tipo sconosciuto è scartato e segnalato.</li>
     *   <li><b>L'id porta l'istante</b>, non la chiave del Fiat Wallet : le gambe sono ordinate per
     *       id e le modalità "primo apporto" / "ultima uscita" dipendono da quell'ordine, che deve
     *       essere cronologico anche dentro la giornata.</li>
     * </ul>
     */
    private static void gambeDalFiatWalletCDC(IndiceFiat indice) {
        String gruppo = DatabaseH2.Pers_GruppoWallet_Leggi(CDC_FiatECardWallet.NOME_EXCHANGE_CDC_APP, true);
        CDC_FiatECardWallet.EsitoFiatWallet esito =
                CDC_FiatECardWallet.MovimentiEuro(VarStatiche.getFile_CDCFiatWallet());
        if (esito.movimenti.isEmpty() && esito.scartatiNonInEuro == 0 && esito.scartatiTipoSconosciuto == 0
                && CDC_FiatECardWallet.SaldoAperturaFiatWallet() == null) {
            return; // nessun Fiat Wallet su questa installazione
        }
        int progressivo = 0;
        List<GambaFiat> gambe = new ArrayList<>();
        //Saldo di apertura inserito a mano nel tab Fiat Wallet : il CSV comincia dal primo movimento
        //scaricato, non dall'apertura del conto. Il tab lo somma come offset costante a saldo iniziale,
        //finale e giacenza media (CDC_FiatECardWallet.calcolaSaldiEMedia) ; qui la stessa cosa si
        //ottiene con una gamba sintetica, cosi' entra in tutte le giacenze giornaliere del motore.
        //L'id porta l'orario 000000 perche' le gambe si ordinano per id e le modalita' "primo apporto"
        /// "ultima uscita" dipendono da quell'ordine : l'apertura deve venire prima di ogni movimento
        //dello stesso giorno. Resta datata al primo movimento importato e non al giorno precedente :
        //spostarla indietro anticiperebbe dataAperturaGruppo, cioe' il prorata dell'IVAFE, su una data
        //che nessun dato conferma.
        String[] apertura = CDC_FiatECardWallet.SaldoAperturaFiatWallet();
        if (apertura != null) {
            gambe.add(new GambaFiat(apertura[1], "EUR", new BigDecimal(apertura[0]),
                    apertura[1].replaceAll("[^0-9]", "") + "000000_FW00000"));
        }
        for (CDC_FiatECardWallet.MovimentoFiatWallet m : esito.movimenti) {
            if (m.importoEUR.signum() == 0) {
                continue;
            }
            progressivo++;
            String id = m.istante.replaceAll("[^0-9]", "") + "_FW" + String.format("%05d", progressivo);
            gambe.add(new GambaFiat(m.giorno, "EUR", m.importoEUR, id));
        }
        if (!gambe.isEmpty()) {
            indice.gambe.computeIfAbsent(gruppo, k -> new ArrayList<>()).addAll(gambe);
        }
        if (esito.scartatiNonInEuro > 0) {
            indice.avvisa(gruppo, "Fiat Wallet Crypto.com : " + esito.scartatiNonInEuro
                    + " movimenti non in euro non contabilizzati");
        }
        if (esito.scartatiTipoSconosciuto > 0) {
            indice.avvisa(gruppo, "Fiat Wallet Crypto.com : " + esito.scartatiTipoSconosciuto
                    + " movimenti di tipo sconosciuto non contabilizzati");
        }
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
        return indicizzaGambeFiat().gambe.getOrDefault(gruppo, new ArrayList<>());
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
    // F2 : intervalli di detenzione FIAT (tagli dai soli periodi FIAT di GRUPPO_PERIODO_RW)
    // =======================================================================

    /** Colonne di una riga di {@link #intervalliFiat}. */
    public static final int IV_DATA_INIZIO = 0, IV_DATA_FINE = 1, IV_STATO = 2,
            IV_MOD_INIZIALE = 3, IV_MOD_FINALE = 4,
            IV_VAL_INIZIALE_MAN = 5, IV_VAL_FINALE_MAN = 6,
            IV_NOTA_INIZIALE = 7, IV_NOTA_FINALE = 8,
            /** "SI"/"NO"/"" — è conto corrente estero : consumato da {@link #applicaContoCorrente} (codice bene 1, IVAFE fissa). */
            IV_E_CONTO_CORRENTE = 9;
    public static final int IV_COLONNE = 10;

    /**
     * Spezza l'anno di riferimento nei tratti di detenzione FIAT di un gruppo wallet.
     *
     * <p>Le <b>date di taglio</b> interne all'anno vengono da una sorgente sola : le finestre
     * <i>effettive</i> dei periodi {@code GRUPPO_PERIODO_RW} di tipo {@code FIAT} del gruppo (vedi
     * {@link Principale_GruppiWalletRW#finestreEffettive}, che deduce l'inizio mancante dalla fine del
     * periodo precedente). Fino al 2026-09-08 c'era una seconda sorgente — i periodi fiscali
     * dell'exchange di riferimento — e il tratto nasceva dall'incrocio delle due ; ora un cambio di
     * Stato estero <i>è</i> un periodo FIAT in più, quindi il taglio è già qui.</p>
     *
     * <p>Ogni inizio di finestra dentro l'anno apre un tratto ; ogni fine ne chiude uno. Per ogni
     * tratto : {@code IV_STATO} è lo Stato estero del periodo che lo copre ({@code ""} se nessuno lo
     * copre — un gruppo senza righi FIAT non ha Stato, ed è un caso legittimo) ;
     * {@code IV_MOD_INIZIALE} / {@code IV_VAL_INIZIALE_MAN} vengono dal periodo il cui inizio effettivo
     * coincide con l'inizio del tratto (confine definito dall'utente), e al 1° gennaio dal periodo che
     * parte "dal primo movimento" ; {@code IV_MOD_FINALE} / {@code IV_VAL_FINALE_MAN} idem dal periodo
     * la cui fine coincide con la fine del tratto, e al 31 dicembre dal periodo ancora aperto. Un
     * periodo spezzato più avanti mantiene le modalità iniziali sul primo tratto e quelle finali
     * sull'ultimo, col taglio interno "nudo".</p>
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

        List<Principale_GruppiWalletRW.Finestra> finestre = Principale_GruppiWalletRW.finestreEffettive(
                Principale_GruppiWalletRW.caricaPeriodi(gruppo), Principale_GruppiWalletRW.TIPO_FIAT);

        TreeSet<LocalDate> tagli = new TreeSet<>();
        for (Principale_GruppiWalletRW.Finestra f : finestre) {
            if (f.inizio != null && f.inizio.isAfter(annoInizio) && !f.inizio.isAfter(annoFine)) {
                tagli.add(f.inizio);
            }
            if (f.fine != null && !f.fine.isBefore(annoInizio) && f.fine.isBefore(annoFine)) {
                tagli.add(f.fine.plusDays(1));
            }
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

            Principale_GruppiWalletRW.Finestra copre = finestraCheCopre(finestre, di);
            if (copre != null) {
                iv[IV_STATO] = trim(copre.riga[Principale_GruppiWalletRW.COL_STATO_ESTERO]);
                iv[IV_E_CONTO_CORRENTE] = trim(copre.riga[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE]);
            }

            String[] pIni = periodoConInizio(finestre, di, di.equals(annoInizio));
            if (pIni != null) {
                iv[IV_MOD_INIZIALE] = trim(pIni[Principale_GruppiWalletRW.COL_MOD_INIZIALE]);
                iv[IV_VAL_INIZIALE_MAN] = trim(pIni[Principale_GruppiWalletRW.COL_VAL_INIZIALE]);
                iv[IV_NOTA_INIZIALE] = trim(pIni[Principale_GruppiWalletRW.COL_NOTA_INIZIALE]);
            }
            String[] pFin = periodoConFine(finestre, df, df.equals(annoFine));
            if (pFin != null) {
                iv[IV_MOD_FINALE] = trim(pFin[Principale_GruppiWalletRW.COL_MOD_FINALE]);
                iv[IV_VAL_FINALE_MAN] = trim(pFin[Principale_GruppiWalletRW.COL_VAL_FINALE]);
                iv[IV_NOTA_FINALE] = trim(pFin[Principale_GruppiWalletRW.COL_NOTA_FINALE]);
            }
            out.add(iv);
        }
        return out;
    }

    /** La prima finestra che copre la data, o {@code null}. */
    private static Principale_GruppiWalletRW.Finestra finestraCheCopre(
            List<Principale_GruppiWalletRW.Finestra> finestre, LocalDate data) {
        for (Principale_GruppiWalletRW.Finestra f : finestre) {
            if (f.copre(data)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Il periodo FIAT il cui inizio effettivo coincide con {@code data}, cioè un confine definito
     * dall'utente. Se {@code ammettiAperto} (siamo al 1° gennaio) e nessuno coincide, ripiega sul
     * periodo che parte "dal primo movimento" (inizio effettivo nullo) <b>e che copre quella data</b>,
     * il quale porta modalità e valore manuale all'inizio dell'anno. Un periodo che il 1° gennaio è
     * semplicemente <i>in corso</i> non ripiega : la sua modalità descrive come valutare l'<i>apertura</i>
     * del periodo, e applicarla a metà cambierebbe la valutazione al confine d'anno.
     *
     * <p>⚠️ Il {@code copre(data)} sul ripiego non è ridondante : un periodo con inizio indefinito ma
     * <b>già chiuso</b> (la riga "vecchia entità legale", chiusa il giorno prima del cambio di Stato
     * estero) resta il primo della lista ordinata, e senza quel controllo porterebbe le sue modalità e i
     * suoi valori manuali negli anni <i>successivi</i> alla propria chiusura.</p>
     */
    private static String[] periodoConInizio(List<Principale_GruppiWalletRW.Finestra> finestre,
            LocalDate data, boolean ammettiAperto) {
        for (Principale_GruppiWalletRW.Finestra f : finestre) {
            if (f.inizio != null && f.inizio.equals(data)) {
                return f.riga;
            }
        }
        if (ammettiAperto) {
            for (Principale_GruppiWalletRW.Finestra f : finestre) {
                if (f.inizio == null && f.copre(data)) {
                    return f.riga;
                }
            }
        }
        return null;
    }

    /**
     * Simmetrico di {@link #periodoConInizio} sulla fine del tratto ; il ripiego è il periodo ancora
     * aperto <b>che copre quella data</b> (stessa avvertenza : un periodo aperto ma cominciato dopo non
     * deve portare la sua modalità alla fine di un anno precedente).
     */
    private static String[] periodoConFine(List<Principale_GruppiWalletRW.Finestra> finestre,
            LocalDate data, boolean ammettiAperto) {
        for (Principale_GruppiWalletRW.Finestra f : finestre) {
            if (f.fine != null && f.fine.equals(data)) {
                return f.riga;
            }
        }
        if (ammettiAperto) {
            for (Principale_GruppiWalletRW.Finestra f : finestre) {
                if (f.fine == null && f.copre(data)) {
                    return f.riga;
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

    /**
     * Colonne extra di un rigo FIAT rispetto al {@code String[17]} del rigo CRYPTO. Le prime 17 hanno
     * lo stesso significato del rigo CRYPTO; {@code [10]} resta il valore <b>finale</b> reale del tratto
     * (per il dettaglio a video), {@code [11]} i giorni del tratto. Le colonne dalla 18 in poi servono
     * al <b>conto corrente estero</b> ({@code EContoCorrente = SI} sul periodo) : sono vuote / "14" /
     * "NO" per gli altri righi FIAT.
     */
    public static final int FIAT_COL_STATO_ESTERO = 17;
    /** Codice individuazione bene (colonna 3 del quadro RW) : "1" per il conto corrente, "14" per le altre attività estere. */
    public static final int FIAT_COL_CODICE_BENE = 18;
    /** Valore medio di giacenza in EUR sull'intero anno di vita del conto (colonna 8 del quadro RW per il conto corrente). "" per gli altri righi. */
    public static final int FIAT_COL_VALORE_MEDIO = 19;
    /** IVAFE dovuta in EUR (misura fissa 34,20 € pro quota/giorni). "0.00" se non dovuta (esenzione ≤ 5.000 €) o rigo non conto corrente. */
    public static final int FIAT_COL_IVAFE = 20;
    /** "SI" se il rigo assolve i soli obblighi di monitoraggio (colonna 16 barrata), "NO" se è dovuta l'IVAFE. */
    public static final int FIAT_COL_SOLO_MONITORAGGIO = 21;
    /** Valore massimo raggiunto in EUR nell'anno di vita del conto (soglia di monitoraggio 15.000 €). "" per gli altri righi. */
    public static final int FIAT_COL_VALORE_MASSIMO = 22;
    /** Lunghezza di un rigo FIAT ({@code String[23]}). */
    public static final int FIAT_COLONNE = 23;

    /** Codice individuazione bene per un vero conto corrente / deposito bancario estero. */
    public static final String CODICE_BENE_CONTO_CORRENTE = "1";
    /** IVAFE in misura fissa per i conti correnti e libretti di risparmio (istruzioni Redditi PF 2026, colonna 29 del quadro RW). */
    static final BigDecimal IVAFE_CONTO_CORRENTE_FISSA = new BigDecimal("34.20");
    /** Sotto (o pari a) questo valore medio di giacenza l'IVAFE sul conto corrente non è dovuta. */
    static final BigDecimal SOGLIA_ESENZIONE_IVAFE = new BigDecimal("5000");

    /**
     * Aliquota IVAFE <b>ordinaria</b> sui prodotti finanziari diversi dai conti correnti e dai
     * libretti di risparmio : 2 per mille (art. 19 comma 20 del D.L. 201/2011 ; istruzioni Redditi
     * PF, colonna 29 punto I).
     */
    static final BigDecimal ALIQUOTA_IVAFE_ORDINARIA = new BigDecimal("0.002");

    /**
     * Aliquota IVAFE <b>maggiorata</b> per i prodotti finanziari detenuti in Stati o territori a
     * fiscalità privilegiata : 4 per mille (art. 19 comma 20-bis del D.L. 201/2011 ; istruzioni
     * Redditi PF, colonna 29 punto I, con barratura della colonna 21). L'elenco degli Stati è in
     * {@link StatiEsteri#isPrivilegiato(String)}.
     */
    static final BigDecimal ALIQUOTA_IVAFE_PRIVILEGIATA = new BigDecimal("0.004");

    /**
     * Primo anno d'imposta in cui si applica {@link #ALIQUOTA_IVAFE_PRIVILEGIATA} : il comma 20-bis
     * dispone «a decorrere dall'anno 2024». Prima di quell'anno vale l'aliquota ordinaria anche sugli
     * Stati dell'elenco.
     */
    static final int ANNO_INIZIO_IVAFE_PRIVILEGIATA = 2024;

    /**
     * L'aliquota IVAFE da applicare a un rigo di liquidità : maggiorata se lo Stato estero del tratto
     * è nell'elenco del D.M. 4 maggio 1999 <b>e</b> l'anno è almeno
     * {@value #ANNO_INIZIO_IVAFE_PRIVILEGIATA}, altrimenti ordinaria. Uno Stato non indicato non fa
     * scattare la maggiorazione : il rigo porta già l'avviso "Stato estero mancante".
     */
    static BigDecimal aliquotaIvafe(int anno, String statoEstero) {
        return anno >= ANNO_INIZIO_IVAFE_PRIVILEGIATA && StatiEsteri.isPrivilegiato(statoEstero)
                ? ALIQUOTA_IVAFE_PRIVILEGIATA : ALIQUOTA_IVAFE_ORDINARIA;
    }

    /**
     * Opzione utente (in {@code personale.mv.db}) : {@code "SI"} = la liquidità in valuta presso
     * intermediari esteri si dichiara in <b>solo monitoraggio</b>, senza liquidare l'IVAFE ;
     * {@code "NO"} (default, {@link #LIQUIDITA_SOLO_MONITORAGGIO_DEFAULT}) = si liquida l'IVAFE
     * ordinaria.
     *
     * <p>La scelta esiste perché la questione <b>non è risolta</b> e nessuna delle due risposte è
     * dimostrabile : l'art. 19 comma 18 tassa i «prodotti finanziari», termine chiuso del TUF
     * (art. 1 comma 1 lett. u del D.Lgs. 58/1998) che esclude ciò che non è investimento, mentre la
     * circolare 28/E del 2012 — pubblicata dopo la modifica che ha introdotto quel termine —
     * descrive ancora la base imponibile come «ogni altra attività da cui possono derivare redditi
     * di capitale o redditi diversi di natura finanziaria di fonte estera», cioè come il perimetro
     * stesso del monitoraggio. Il default liquida l'imposta perché è la lettura che segue la prassi
     * dell'Agenzia ; chi segue la lettera della norma attiva l'opzione. Analisi completa in
     * {@code nocommit/Documentazione/Analisi_QuadroRW_Codice14.md}, § 6.</p>
     *
     * <p>Non tocca i righi <b>conto corrente</b> (codice bene {@value #CODICE_BENE_CONTO_CORRENTE}) :
     * lì l'imposta è quella in misura fissa e non è in discussione.</p>
     */
    public static final String OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO = "RW_LiquiditaSoloMonitoraggio";

    /** Default di {@link #OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO} : {@code "NO"}, cioè l'IVAFE si liquida. */
    public static final String LIQUIDITA_SOLO_MONITORAGGIO_DEFAULT = "NO";
    /** Sotto (o pari a) questo valore massimo non c'è nemmeno obbligo di monitoraggio (rigo comunque prodotto, con avviso). */
    static final BigDecimal SOGLIA_MONITORAGGIO_CONTO = new BigDecimal("15000");

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
     * <p>I tratti che ricadono in un periodo FIAT il cui Stato estero è il valore sentinella
     * {@link StatiEsteri#CODICE_ITALIA} ("conto in Italia") <b>non producono rigo</b> : il quadro W/RW
     * monitora le sole attività estere.</p>
     *
     * <p>Valore iniziale del tratto : il valore manuale del periodo FIAT se impostato ; altrimenti,
     * su un confine definito dall'utente, la modalità di calcolo (residuo + primo apporto / somma
     * apporti della giornata) ; altrimenti — ed è il default {@code SOLO_RESIDUO} — la giacenza a
     * inizio giornata. Valore finale : simmetrico (giacenza a fine giornata, oppure {@code Sfin +
     * ultima uscita / + somma uscite}). Il primo tratto parte dalla data del primo movimento del gruppo se questo
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

        IndiceFiat indice = indicizzaGambeFiat();
        Map<String, String> primoMovXGruppo = Funzioni.MappaPrimoMovimentoXGruppoWallet();

        for (Map.Entry<String, List<GambaFiat>> voce : indice.gambe.entrySet()) {
            String gruppo = voce.getKey();
            List<GambaFiat> gambe = voce.getValue();
            List<String> avvisiGruppo = indice.avvisi.getOrDefault(gruppo, new ArrayList<>());

            List<String[]> intervalli = intervalliFiat(gruppo, anno);
            LocalDate aperturaGruppo = dataAperturaGruppo(primoMovXGruppo.get(gruppo), gambe, anno);

            List<String[]> righe = new ArrayList<>();
            List<List<String>> avvisiRighe = new ArrayList<>();   // parallelo a righe, per la fase conto corrente
            List<int[]> rangeCC = new ArrayList<>();              // [epochDay di, epochDay df] dei tratti conto corrente
            List<Integer> idxCC = new ArrayList<>();              // indici in righe dei tratti conto corrente
            for (int i = 0; i < intervalli.size(); i++) {
                String[] iv = intervalli.get(i);
                LocalDate di = parseData(iv[IV_DATA_INIZIO]);
                LocalDate df = parseData(iv[IV_DATA_FINE]);
                if (di == null || df == null) {
                    continue;
                }
                // Conto detenuto in Italia : nessun obbligo di monitoraggio, la parte FIAT del quadro
                // W/RW per questo tratto non va compilata. Il tratto resta comunque nei tagli di
                // intervalliFiat (continuità dell'algebra degli intervalli), qui semplicemente non
                // produce rigo.
                if (StatiEsteri.isItalia(trim(iv[IV_STATO]))) {
                    continue;
                }
                boolean aperturaInfraAnno = i == 0 && aperturaGruppo != null && aperturaGruppo.isAfter(di);
                if (aperturaInfraAnno) {
                    di = aperturaGruppo;
                }
                if (df.isBefore(di)) {
                    continue; // tratto svuotato dallo spostamento dell'apertura
                }

                List<String> avvisi = new ArrayList<>(avvisiGruppo);
                BigDecimal valIni = valoreIniziale(gambe, iv, di, aperturaInfraAnno, cambio, avvisi);
                BigDecimal valFin = valoreFinale(gambe, iv, df, cambio, avvisi);

                boolean contoCorrente = Principale_GruppiWalletRW.CONTO_CORRENTE_SI.equals(trim(iv[IV_E_CONTO_CORRENTE]));
                boolean negativo = valIni.signum() < 0 || valFin.signum() < 0;
                // Salto il tratto solo se i due estremi sono GENUINAMENTE nulli (punto 14) : un saldo
                // negativo viene invece portato a zero ma il rigo resta, con l'avviso.
                // Eccezione : per un vero conto corrente estero la misura che conta e' la giacenza
                // MEDIA sull'anno di vita, non i due estremi : un conto aperto e svuotato dentro l'anno
                // ha entrambi gli estremi a zero ma puo' comunque dovere l'IVAFE. Il rigo passa e ci
                // pensa applicaContoCorrente() a decidere media, massimo e imposta.
                if (!negativo && valIni.signum() == 0 && valFin.signum() == 0 && !contoCorrente) {
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
                        trim(iv[IV_STATO]), etichettaValute(gambe, df), avvisi, contoCorrente));
                avvisiRighe.add(avvisi);
                if (contoCorrente) {
                    idxCC.add(righe.size() - 1);
                    rangeCC.add(new int[] {(int) di.toEpochDay(), (int) df.toEpochDay()});
                }
            }
            // Fase conto corrente estero : valore medio / massimo sull'anno di vita del conto, IVAFE fissa.
            applicaContoCorrente(anno, gambe, righe, avvisiRighe, rangeCC, idxCC, cambio);
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
        if (Principale_GruppiWalletRW.soloResiduo(mod)) {
            return residuo; // giacenza a inizio giornata, e basta
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
        if (Principale_GruppiWalletRW.soloResiduo(mod)) {
            return sfin; // giacenza a fine giornata, e basta
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
            List<String> avvisi, boolean contoCorrente) {
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
        r[15] = componiAvvisi(avvisi);
        r[16] = "";                           // lista ID coinvolti
        r[FIAT_COL_STATO_ESTERO] = statoEstero == null ? "" : statoEstero;
        // Altra attività estera di natura finanziaria. applicaContoCorrente() riscrive queste
        // colonne sui tratti con EContoCorrente = SI (codice bene 1, imposta in misura fissa).
        r[FIAT_COL_CODICE_BENE] = CODICE_BENE_FIAT;
        r[FIAT_COL_VALORE_MEDIO] = "";
        r[FIAT_COL_VALORE_MASSIMO] = "";
        int annoNum = Integer.parseInt(anno);
        int giorniAnno = Year.of(annoNum).length();
        BigDecimal aliquota = aliquotaIvafe(annoNum, r[FIAT_COL_STATO_ESTERO]);
        boolean privilegiata = aliquota.compareTo(ALIQUOTA_IVAFE_PRIVILEGIATA) == 0;
        // Sui tratti conto corrente l'imposta e l'avviso li scrive applicaContoCorrente() (misura
        // fissa, che il comma 20-bis non tocca : parla dei soli "prodotti finanziari") : liquidare
        // qui l'aliquota ordinaria lascerebbe in coda un avviso smentito subito dopo.
        BigDecimal ivafe = contoCorrente || liquiditaSoloMonitoraggio()
                ? BigDecimal.ZERO
                : ivafeLiquidita(valFin, giorni, giorniAnno, aliquota);
        if (ivafe.signum() > 0) {
            r[FIAT_COL_IVAFE] = ivafe.toPlainString();
            r[FIAT_COL_SOLO_MONITORAGGIO] = "NO";
            avvisi.add("liquidità : IVAFE " + ivafe.toPlainString() + " EUR ("
                    + (privilegiata ? "0,40 %" : "0,20 %") + " di "
                    + valFin.setScale(2, RoundingMode.HALF_UP).toPlainString() + " x " + giorni + "/"
                    + giorniAnno + ")");
            if (privilegiata) {
                // La barratura della colonna 21 non è riproducibile sul modulo stampato : senza
                // questo avviso l'aliquota doppia risulterebbe applicata su un rigo che, a vederlo,
                // dichiara di essere ordinario.
                avvisi.add("Stato a fiscalità privilegiata (D.M. 4 maggio 1999) : barrare a mano la"
                        + " colonna 21 del quadro RW");
            }
            r[15] = componiAvvisi(avvisi);
        } else {
            r[FIAT_COL_IVAFE] = "0.00";
            r[FIAT_COL_SOLO_MONITORAGGIO] = "SI";
        }
        return r;
    }

    /**
     * {@code true} se l'utente ha scelto di dichiarare la liquidità in valuta in solo monitoraggio.
     * Letta a ogni ricalcolo (non messa in cache) : è un'opzione da casella di spunta, cambiarla
     * deve avere effetto al ricalcolo successivo senza riavviare.
     */
    static boolean liquiditaSoloMonitoraggio() {
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO);
        return v != null && v.equalsIgnoreCase("SI");
    }

    /**
     * IVAFE ordinaria di un tratto di liquidità : {@code valore finale x 0,20 % x giorni/giorni
     * dell'anno}, quota di possesso 100 %.
     *
     * <p>La base è il <b>valore finale del tratto</b> e non il saldo al 31 dicembre. È una scelta,
     * non un vincolo: i tratti nascono da {@link #intervalliFiat} (cambio di Stato estero, del flag
     * conto corrente, apertura del gruppo), non da periodi di detenzione distinti, quindi la
     * colonna 8 del quadro — «valore al termine del periodo di detenzione» — andrebbe letta sul
     * rapporto intero. Si valorizza per tratto perché è l'unica lettura coerente con la colonna 10
     * (giorni) che il programma già scrive per tratto: usare il saldo al 31 dicembre su un tratto
     * chiuso a marzo attribuirebbe a quel tratto un valore che in quel periodo non c'era. Un gruppo
     * che non cambia Stato estero né natura ha comunque un tratto solo, e i due criteri coincidono.</p>
     */
    private static BigDecimal ivafeLiquidita(BigDecimal valFin, int giorni, int giorniAnno,
            BigDecimal aliquota) {
        if (valFin == null || valFin.signum() <= 0 || giorni <= 0 || giorniAnno <= 0) {
            return BigDecimal.ZERO;
        }
        return valFin.multiply(aliquota)
                .multiply(BigDecimal.valueOf(giorni))
                .divide(BigDecimal.valueOf(giorniAnno), 2, RoundingMode.HALF_UP);
    }

    /** Testo della colonna avvisi ({@code [15]}) da una lista di messaggi (deduplicati, ordine di inserimento). */
    private static String componiAvvisi(List<String> avvisi) {
        return avvisi.isEmpty() ? "" : "Avviso (" + String.join("; ", new LinkedHashSet<>(avvisi)) + ")";
    }

    /**
     * Fase <b>conto corrente estero</b>. Per un gruppo con almeno un tratto {@code EContoCorrente = SI}
     * nell'anno : calcola il <b>valore medio</b> e il <b>valore massimo</b> (EUR) della giacenza
     * sull'intero periodo di vita del conto nell'anno, poi riscrive i righi conto corrente con codice
     * individuazione bene {@value #CODICE_BENE_CONTO_CORRENTE}, la giacenza media in
     * {@link #FIAT_COL_VALORE_MEDIO} e — se dovuta — l'IVAFE in {@link #FIAT_COL_IVAFE}.
     *
     * <p>IVAFE in <b>misura fissa</b> {@link #IVAFE_CONTO_CORRENTE_FISSA} € rapportata alla quota
     * (100 %) e al periodo di possesso ({@code giorni tratto / giorni dell'anno}). Non dovuta se il
     * valore medio ≤ {@link #SOGLIA_ESENZIONE_IVAFE} € : la soglia si confronta col valore medio del
     * conto <b>sulla sua vita</b> (i giorni prorata solo l'imposta, non la soglia — è la lettura
     * dell'esempio delle istruzioni Redditi PF 2026). La verifica è sul <b>singolo gruppo wallet</b> :
     * il programma non gestisce due conti presso lo stesso intermediario. Se non dovuta ma il valore
     * massimo &gt; {@link #SOGLIA_MONITORAGGIO_CONTO} € resta l'obbligo di monitoraggio ; sotto quella
     * soglia il rigo è prodotto comunque, con avviso.</p>
     *
     * <p>La conversione in EUR è lineare a data fissa (l'ultimo giorno di vita del conto), quindi si
     * ricava <b>un tasso per valuta</b> con una sola chiamata e non una conversione per giorno.</p>
     */
    private static void applicaContoCorrente(String anno, List<GambaFiat> gambe, List<String[]> righe,
            List<List<String>> avvisiRighe, List<int[]> rangeCC, List<Integer> idxCC, ControvaloreEUR cambio) {
        if (idxCC.isEmpty()) {
            return;
        }
        // Giorni di vita del conto nell'anno = unione dei giorni dei tratti conto corrente.
        TreeSet<LocalDate> giorniVita = new TreeSet<>();
        LocalDate ultimoGiorno = null;
        for (int[] r : rangeCC) {
            LocalDate a = LocalDate.ofEpochDay(r[0]);
            LocalDate b = LocalDate.ofEpochDay(r[1]);
            for (LocalDate g = a; !g.isAfter(b); g = g.plusDays(1)) {
                giorniVita.add(g);
            }
            if (ultimoGiorno == null || b.isAfter(ultimoGiorno)) {
                ultimoGiorno = b;
            }
        }
        String dataCambio = ultimoGiorno.toString();

        List<String> avvisiComuni = new ArrayList<>();
        Map<String, BigDecimal> tasso = new HashMap<>();
        List<GambaFiat> ordinate = new ArrayList<>(gambe);
        ordinate.sort(Comparator.comparing(g -> g.giorno));

        int gi = 0;
        Map<String, BigDecimal> saldo = new HashMap<>();
        BigDecimal somma = BigDecimal.ZERO;
        BigDecimal massimo = BigDecimal.ZERO;
        int n = 0;
        for (LocalDate g : giorniVita) {
            String gs = g.toString();
            while (gi < ordinate.size() && ordinate.get(gi).giorno.compareTo(gs) <= 0) {
                GambaFiat gf = ordinate.get(gi++);
                saldo.merge(gf.valuta, gf.importo, BigDecimal::add);
            }
            BigDecimal totGiorno = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> e : saldo.entrySet()) {
                if (e.getValue().signum() == 0) {
                    continue;
                }
                BigDecimal t = tasso.computeIfAbsent(e.getKey(), v -> tassoEUR(v, dataCambio, cambio, avvisiComuni));
                totGiorno = totGiorno.add(t.multiply(e.getValue()));
            }
            if (totGiorno.signum() < 0) {
                totGiorno = BigDecimal.ZERO; // conto in rosso : 0 ai fini della giacenza
            }
            somma = somma.add(totGiorno);
            if (totGiorno.compareTo(massimo) > 0) {
                massimo = totGiorno;
            }
            n++;
        }
        BigDecimal valoreMedio = n > 0
                ? somma.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        massimo = massimo.setScale(2, RoundingMode.HALF_UP);
        boolean dovuta = valoreMedio.compareTo(SOGLIA_ESENZIONE_IVAFE) > 0;
        int giorniAnno = Year.of(Integer.parseInt(anno)).length();

        for (int idx : idxCC) {
            String[] r = righe.get(idx);
            List<String> av = avvisiRighe.get(idx);
            av.addAll(avvisiComuni);
            r[FIAT_COL_CODICE_BENE] = CODICE_BENE_CONTO_CORRENTE;
            r[FIAT_COL_VALORE_MEDIO] = valoreMedio.toPlainString();
            r[FIAT_COL_VALORE_MASSIMO] = massimo.toPlainString();
            if (dovuta) {
                int gg = Integer.parseInt(r[11]);
                BigDecimal ivafe = IVAFE_CONTO_CORRENTE_FISSA
                        .multiply(BigDecimal.valueOf(gg))
                        .divide(BigDecimal.valueOf(giorniAnno), 2, RoundingMode.HALF_UP);
                r[FIAT_COL_IVAFE] = ivafe.toPlainString();
                r[FIAT_COL_SOLO_MONITORAGGIO] = "NO";
                av.add("conto corrente : IVAFE " + ivafe.toPlainString() + " EUR (34,20 x " + gg + "/" + giorniAnno + ")");
            } else {
                r[FIAT_COL_IVAFE] = "0.00";
                r[FIAT_COL_SOLO_MONITORAGGIO] = "SI";
                if (massimo.compareTo(SOGLIA_MONITORAGGIO_CONTO) <= 0) {
                    av.add("conto corrente sotto la soglia di monitoraggio (valore massimo "
                            + massimo.toPlainString() + " EUR <= 15.000) : rigo prodotto per completezza");
                } else {
                    av.add("conto corrente : IVAFE non dovuta (valore medio " + valoreMedio.toPlainString()
                            + " EUR <= 5.000), resta l'obbligo di monitoraggio (valore massimo > 15.000 EUR)");
                }
            }
            r[15] = componiAvvisi(av);
        }
    }

    /** Tasso EUR di una valuta a data fissa (conversione lineare) : {@code cambio.eur(valuta, 1, data)}. {@code 0} se non convertibile. */
    private static BigDecimal tassoEUR(String valuta, String dataIso, ControvaloreEUR cambio, List<String> avvisi) {
        BigDecimal t = cambio.eur(valuta, BigDecimal.ONE, dataIso);
        if (t == null) {
            avvisi.add("valuta " + trim(valuta).toUpperCase() + " non convertita in EUR");
            return BigDecimal.ZERO;
        }
        return t;
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
    private static LocalDate dataAperturaGruppo(String idPrimoMovimento, List<GambaFiat> gambe, String anno) {
        String giorno = null;
        String[] mov = idPrimoMovimento == null ? null : MappaCryptoWallet.get(idPrimoMovimento);
        if (mov != null && mov[1] != null && mov[1].length() >= 10) {
            giorno = mov[1].substring(0, 10);
        }
        // Le gambe FIAT di Crypto.com App arrivano dal Fiat Wallet, che e' una sorgente indipendente
        // dai movimenti crypto e puo' quindi cominciare PRIMA del primo movimento del gruppo. Per
        // ogni altro exchange la gamba FIAT e' parte di un movimento, quindi il minimo e' gia' quello
        // e questo giro non cambia nulla. Il minimo va preso PRIMA del test sull'anno : invertire i
        // due passaggi trasformerebbe un gruppo aperto in un anno precedente in una falsa apertura
        // infra-anno.
        for (GambaFiat g : gambe) {
            if (giorno == null || g.giorno.compareTo(giorno) < 0) {
                giorno = g.giorno;
            }
        }
        if (giorno == null || !giorno.startsWith(anno + "-")) {
            return null;
        }
        return parseData(giorno);
    }
}
