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

    /** Categoria del deposito FIAT : l'unica entrata di valuta che è un apporto di capitale nuovo. */
    static final String CATEGORIA_DEPOSITO_FIAT = "DF";

    /** Categoria del prelievo FIAT : l'unica uscita di valuta che è un'uscita di capitale dal rapporto. */
    static final String CATEGORIA_PRELIEVO_FIAT = "PF";

    // =======================================================================
    // Indice delle gambe FIAT (una gamba = un lato FIAT di un movimento)
    // =======================================================================

    /**
     * Natura di una gamba rispetto al rapporto : capitale che entra dall'esterno ({@code APPORTO}),
     * capitale che esce verso l'esterno ({@code PRELIEVO}), oppure una semplice variazione della
     * composizione del rapporto ({@code INTERNA} : acquisti e vendite di crypto, commissioni, saldo di
     * apertura). Decisa all'indicizzazione dalla categoria del movimento o dal tipo del Fiat Wallet,
     * <b>mai dal segno</b> : una vendita di crypto è un'entrata FIAT ma non è un apporto.
     */
    enum NaturaGamba { APPORTO, PRELIEVO, INTERNA }

    /** Un lato FIAT di un movimento : giorno ISO, valuta (maiuscolo), importo firmato (uscita &lt; 0), id movimento. */
    private static final class GambaFiat {
        final String giorno;
        final String valuta;
        final BigDecimal importo;
        final String id;
        final NaturaGamba natura;

        GambaFiat(String giorno, String valuta, BigDecimal importo, String id, NaturaGamba natura) {
            this.giorno = giorno;
            this.valuta = valuta;
            this.importo = importo;
            this.id = id;
            this.natura = natura;
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
            String cat = categoria(v);
            aggiungiSeFiat(gambe, giorno, v[0], v[8], v[9], v[10], false,
                    CATEGORIA_PRELIEVO_FIAT.equals(cat) ? NaturaGamba.PRELIEVO : NaturaGamba.INTERNA);
            aggiungiSeFiat(gambe, giorno, v[0], v[11], v[12], v[13], true,
                    CATEGORIA_DEPOSITO_FIAT.equals(cat) ? NaturaGamba.APPORTO : NaturaGamba.INTERNA);
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
            //Il saldo di apertura non e' un apporto : e' il capitale che c'era gia' prima del CSV.
            gambe.add(new GambaFiat(apertura[1], "EUR", new BigDecimal(apertura[0]),
                    apertura[1].replaceAll("[^0-9]", "") + "000000_FW00000", NaturaGamba.INTERNA));
        }
        for (CDC_FiatECardWallet.MovimentoFiatWallet m : esito.movimenti) {
            if (m.importoEUR.signum() == 0) {
                continue;
            }
            progressivo++;
            String id = m.istante.replaceAll("[^0-9]", "") + "_FW" + String.format("%05d", progressivo);
            gambe.add(new GambaFiat(m.giorno, "EUR", m.importoEUR, id, naturaTipoFiatWalletCDC(m.tipo)));
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
     * Natura di un movimento del Fiat Wallet Crypto.com : solo il bonifico in ingresso
     * ({@code viban_deposit}) è un apporto e solo il bonifico verso il conto corrente
     * ({@code viban_withdrawal}) è un prelievo. Acquisti e vendite di crypto cambiano la composizione
     * del rapporto, non il capitale. La ricarica della carta ({@code viban_card_top_up}) resta
     * interna di proposito : è denaro che passa a un altro prodotto dello stesso intermediario, non
     * un bonifico verso l'esterno. I tipi personalizzati dall'utente non si possono classificare e
     * restano interni.
     */
    static NaturaGamba naturaTipoFiatWalletCDC(String tipo) {
        String t = trim(tipo);
        if ("viban_deposit".equalsIgnoreCase(t)) {
            return NaturaGamba.APPORTO;
        }
        if ("viban_withdrawal".equalsIgnoreCase(t)) {
            return NaturaGamba.PRELIEVO;
        }
        return NaturaGamba.INTERNA;
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
            String simbolo, String tipo, String qta, boolean entrata, NaturaGamba natura) {
        if (!gambaFiatValida(tipo, simbolo, qta)) {
            return;
        }
        BigDecimal importo = absOrNull(qta);
        if (importo == null) {
            return;
        }
        gambe.add(new GambaFiat(giorno, trim(simbolo).toUpperCase(),
                entrata ? importo : importo.negate(), id, natura));
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
    static Principale_GruppiWalletRW.Finestra finestraCheCopre(
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
    static String[] periodoConInizio(List<Principale_GruppiWalletRW.Finestra> finestre,
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
    static String[] periodoConFine(List<Principale_GruppiWalletRW.Finestra> finestre,
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
    /**
     * Valore medio di giacenza in EUR : sul conto corrente la giacenza media annua (somma dei saldi dei giorni
     * di vita del conto diviso {@value #GIORNI_GIACENZA_MEDIA_ANNUA}, colonna 8 del quadro RW) ; sugli altri righi quello del tratto, solo con l'opzione
     * {@link #OPZIONE_VALORE_FINALE_MAX_MEDIA} attiva, altrimenti "". Il valore da dichiarare lo
     * decide {@link #valoreFinaleDichiarato(String[])}.
     */
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
    /**
     * Opzione utente (in {@code personale.mv.db}) : {@code "SI"} = sui righi di liquidità <b>diversi
     * dal conto corrente</b> (codice bene {@value #CODICE_BENE_FIAT}) il valore finale dichiarato
     * (colonna 8 del quadro RW, e base dell'IVAFE ordinaria) è il <b>maggiore</b> fra la giacenza
     * media giornaliera del tratto e il saldo a fine tratto ; {@code "NO"} (default,
     * {@link #VALORE_FINALE_MAX_MEDIA_DEFAULT}) = il saldo a fine tratto, come prima.
     *
     * <p>La giacenza media è quella del <b>periodo di detenzione</b> : somma dei saldi giornalieri
     * del tratto divisa per i giorni del tratto ({@link #mediaPeriodo}, dove si spiega perché non la
     * media annua DSU), con saldo negativo portato a zero e conversione in EUR alla data di fine tratto (la stessa del saldo finale). Il saldo reale resta
     * in {@code [10]} ; la media va in {@link #FIAT_COL_VALORE_MEDIO} e il valore da dichiarare lo
     * ricava {@link #valoreFinaleDichiarato(String[])}. Un tratto con valore finale <b>manuale</b>
     * non è toccato : chi scrive il valore a mano lo fa di solito perché i movimenti sono incompleti,
     * e una media calcolata da quei movimenti non sarebbe migliore.</p>
     *
     * <p>Non tocca i righi conto corrente : lì la colonna 8 porta già la giacenza media annua del
     * conto ({@link #applicaContoCorrente}).</p>
     */
    public static final String OPZIONE_VALORE_FINALE_MAX_MEDIA = "RW_FiatValoreFinaleMaxMedia";

    /** Default di {@link #OPZIONE_VALORE_FINALE_MAX_MEDIA} : {@code "NO"}, cioè il saldo a fine tratto. */
    public static final String VALORE_FINALE_MAX_MEDIA_DEFAULT = "NO";

    /**
     * Opzione utente (in {@code personale.mv.db}) : {@code "SI"} = un <b>deposito FIAT</b> (apporto di
     * capitale nuovo) di controvalore superiore a {@link #OPZIONE_SOGLIA_APPORTI} chiude il rigo di
     * liquidità il giorno prima dell'apporto e ne apre uno nuovo dal giorno dell'apporto. È la regola
     * della circolare 12/E dell'8 aprile 2016, § 14.1 : «il momento di avvenuta variazione dovrà essere
     * considerata come discriminante temporale da cui far discendere un nuovo adempimento
     * dichiarativo». Default {@code "NO"}.
     *
     * <p>Contano <b>solo</b> gli apporti ({@link NaturaGamba#APPORTO} : categoria {@code DF}, bonifico
     * in ingresso sul Fiat Wallet Crypto.com) : una vendita di crypto porta euro nel rapporto ma ne
     * cambia soltanto la composizione, e la circolare dice che quelle variazioni non rilevano.</p>
     *
     * <p>Non si applica in regime di <b>solo monitoraggio</b> ({@link #liquiditaSoloMonitoraggio()}) :
     * senza imposta i giorni di detenzione non servono a nulla e spezzare il rigo non cambia niente.
     * Non si applica ai tratti <b>conto corrente</b> : lì la colonna 8 è la giacenza media annua del
     * conto ({@link #applicaContoCorrente}) e la circolare stessa rimanda i conti correnti a quella
     * soluzione, quindi i pezzi sarebbero righi identici.</p>
     */
    public static final String OPZIONE_SPEZZA_SU_APPORTI = "RW_FiatSpezzaSuApporti";
    /** Soglia in EUR di {@link #OPZIONE_SPEZZA_SU_APPORTI} : spezza solo un apporto <b>strettamente</b> maggiore. */
    public static final String OPZIONE_SOGLIA_APPORTI = "RW_FiatSogliaApporti";
    /**
     * Come {@link #OPZIONE_SPEZZA_SU_APPORTI} ma per i <b>prelievi FIAT</b> (categoria {@code PF},
     * bonifico in uscita dal Fiat Wallet Crypto.com). La circolare 12/E/2016 non lo chiede : il testo
     * parla solo di apporti. È una libertà in più per chi preferisce la lettura estesa, e va saputo che
     * spezzare su un prelievo <b>alza</b> la base dell'IVAFE (il valore più alto di prima del prelievo
     * pesa per i suoi giorni invece di sparire). Default {@code "NO"}.
     */
    public static final String OPZIONE_SPEZZA_SU_PRELIEVI = "RW_FiatSpezzaSuPrelievi";
    /** Soglia in EUR di {@link #OPZIONE_SPEZZA_SU_PRELIEVI} : spezza solo un prelievo <b>strettamente</b> maggiore. */
    public static final String OPZIONE_SOGLIA_PRELIEVI = "RW_FiatSogliaPrelievi";
    /** Default delle due opzioni di taglio : disattive. */
    public static final String SPEZZA_DEFAULT = "NO";
    /** Default delle due soglie, in EUR. */
    public static final String SOGLIA_TAGLIO_DEFAULT = "500";

    /**
     * La soglia di taglio attiva per l'opzione indicata, o {@code null} se il taglio non si applica :
     * opzione spenta, liquidità in solo monitoraggio, soglia illeggibile. Una soglia vuota vale il
     * default. Letta a ogni ricalcolo, come le altre opzioni della parte FIAT.
     */
    static BigDecimal sogliaTaglio(String opzioneAttiva, String opzioneSoglia) {
        String attiva = DatabaseH2.Pers_Opzioni_Leggi(opzioneAttiva);
        if (attiva == null || !attiva.equalsIgnoreCase("SI") || liquiditaSoloMonitoraggio()) {
            return null;
        }
        return leggiSoglia(DatabaseH2.Pers_Opzioni_Leggi(opzioneSoglia));
    }

    /**
     * Interpreta una soglia scritta dall'utente : accetta la virgola decimale e il punto delle
     * migliaia all'italiana ({@code 1.500,50}, e anche {@code 1.500} senza decimali, che altrimenti
     * varrebbe 1,5) ; un punto che non separa gruppi di tre cifre resta il separatore decimale
     * ({@code 500.5}) ; vuoto = {@link #SOGLIA_TAGLIO_DEFAULT} ; {@code null} se non numerica o negativa.
     */
    public static BigDecimal leggiSoglia(String testo) {
        String t = trim(testo);
        if (t.isEmpty()) {
            t = SOGLIA_TAGLIO_DEFAULT;
        }
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        try {
            BigDecimal b = new BigDecimal(t);
            return b.signum() < 0 ? null : b;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** {@code true} se l'utente ha scelto il massimo fra giacenza media e saldo finale (letta a ogni ricalcolo). */
    static boolean valoreFinaleMaxMedia() {
        String v = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_VALORE_FINALE_MAX_MEDIA);
        return v != null && v.equalsIgnoreCase("SI");
    }

    /**
     * Il valore da riportare come "valore finale" (colonna 8 del quadro RW / W) per un rigo della
     * mappa FIAT : sul conto corrente la giacenza media ; sugli altri righi il maggiore fra giacenza
     * media e saldo finale se la media è valorizzata (opzione {@link #OPZIONE_VALORE_FINALE_MAX_MEDIA}),
     * altrimenti il saldo finale {@code [10]}. Unico punto in cui si decide : la tabella di sintesi e
     * le stampe lo leggono da qui.
     */
    public static String valoreFinaleDichiarato(String[] d) {
        String saldo = d[10];
        String media = d.length > FIAT_COL_VALORE_MEDIO ? d[FIAT_COL_VALORE_MEDIO] : null;
        if (media == null || media.isBlank()) {
            return saldo;
        }
        if (CODICE_BENE_CONTO_CORRENTE.equals(d[FIAT_COL_CODICE_BENE])) {
            return media;
        }
        try {
            return new BigDecimal(media).compareTo(new BigDecimal(saldo)) > 0 ? media : saldo;
        } catch (RuntimeException e) {
            return saldo;
        }
    }

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
     * <p>Con le opzioni {@link #OPZIONE_SPEZZA_SU_APPORTI} / {@link #OPZIONE_SPEZZA_SU_PRELIEVI} un
     * tratto non conto corrente si spezza ulteriormente a ogni deposito / prelievo FIAT sopra soglia
     * ({@link #tagliApportiPrelievi}), un rigo per pezzo.</p>
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
        BigDecimal sogliaApporti = sogliaTaglio(OPZIONE_SPEZZA_SU_APPORTI, OPZIONE_SOGLIA_APPORTI);
        BigDecimal sogliaPrelievi = sogliaTaglio(OPZIONE_SPEZZA_SU_PRELIEVI, OPZIONE_SOGLIA_PRELIEVI);

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

                boolean contoCorrente = Principale_GruppiWalletRW.CONTO_CORRENTE_SI.equals(trim(iv[IV_E_CONTO_CORRENTE]));
                // Apporti / prelievi sopra soglia (opzioni RW_FiatSpezzaSu*) : il tratto si spezza in
                // pezzi, ognuno col suo rigo. Mai sul conto corrente (vedi OPZIONE_SPEZZA_SU_APPORTI).
                List<Taglio> tagli = contoCorrente ? new ArrayList<>()
                        : tagliApportiPrelievi(gambe, di, df, sogliaApporti, sogliaPrelievi, cambio);
                LocalDate pezzoInizio = di;
                for (int k = 0; k <= tagli.size(); k++) {
                    boolean primo = k == 0;
                    boolean ultimo = k == tagli.size();
                    Taglio tIni = primo ? null : tagli.get(k - 1);
                    Taglio tFin = ultimo ? null : tagli.get(k);
                    LocalDate pezzoFine = ultimo ? df : tFin.giorno.minusDays(1);
                    generaRigoPezzo(anno, gruppo, gambe, iv, pezzoInizio, pezzoFine, tIni, tFin,
                            primo && aperturaInfraAnno, contoCorrente, avvisiGruppo, cambio,
                            righe, avvisiRighe, rangeCC, idxCC);
                    if (!ultimo) {
                        pezzoInizio = tFin.giorno;
                    }
                }
            }
            // Fase conto corrente estero : valore medio / massimo sull'anno di vita del conto, IVAFE fissa.
            applicaContoCorrente(anno, gambe, righe, avvisiRighe, rangeCC, idxCC, cambio);
            if (!righe.isEmpty()) {
                Principale.Mappa_RW_ListeXGruppoWallet_Fiat.put(gruppo, righe);
            }
        }
    }

    /**
     * Un taglio di un tratto per apporto o prelievo di capitale : {@code giorno} è il primo giorno
     * del pezzo che comincia, {@code posizione} l'indice della gamba nella lista ordinata del gruppo.
     */
    private static final class Taglio {
        final LocalDate giorno;
        final int posizione;
        final GambaFiat gamba;
        final BigDecimal eur;

        Taglio(LocalDate giorno, int posizione, GambaFiat gamba, BigDecimal eur) {
            this.giorno = giorno;
            this.posizione = posizione;
            this.gamba = gamba;
            this.eur = eur;
        }

        /** Testo per gli avvisi del rigo : "un apporto di capitale di 1000.00 EUR del 2024-06-10". */
        String descrizione() {
            return (gamba.natura == NaturaGamba.APPORTO ? "un apporto di capitale" : "un prelievo") + " di "
                    + eur.setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR del " + giorno;
        }

        String fonte() {
            return gamba.natura == NaturaGamba.APPORTO ? "circ. 12/E/2016 par. 14.1" : "opzione sui prelievi";
        }
    }

    /**
     * I tagli di un tratto {@code [di, df]} : un apporto ({@code sogliaApporti != null}) o un prelievo
     * ({@code sogliaPrelievi != null}) il cui controvalore in EUR alla sua data supera
     * <b>strettamente</b> la soglia. Il taglio cade sul giorno della gamba : il pezzo precedente finisce
     * il giorno prima, il successivo comincia quel giorno (stessa convenzione dei periodi CRYPTO). Una
     * gamba nel <b>primo</b> giorno del tratto non taglia nulla (il pezzo precedente sarebbe vuoto), e
     * in una giornata si taglia una volta sola, sulla prima gamba che supera la soglia : gli altri
     * movimenti di quel giorno finiscono nel pezzo nuovo.
     */
    private static List<Taglio> tagliApportiPrelievi(List<GambaFiat> gambe, LocalDate di, LocalDate df,
            BigDecimal sogliaApporti, BigDecimal sogliaPrelievi, ControvaloreEUR cambio) {
        List<Taglio> tagli = new ArrayList<>();
        if (sogliaApporti == null && sogliaPrelievi == null) {
            return tagli;
        }
        String da = di.toString();
        String a = df.toString();
        for (int i = 0; i < gambe.size(); i++) {
            GambaFiat g = gambe.get(i);
            if (g.giorno.compareTo(da) <= 0 || g.giorno.compareTo(a) > 0) {
                continue;
            }
            BigDecimal soglia = g.natura == NaturaGamba.APPORTO ? sogliaApporti
                    : g.natura == NaturaGamba.PRELIEVO ? sogliaPrelievi : null;
            if (soglia == null) {
                continue;
            }
            // Una valuta non convertibile non taglia : l'avviso lo porta gia' il saldo del rigo.
            BigDecimal eur = cambio.eur(g.valuta, g.importo.abs(), g.giorno);
            if (eur == null || eur.compareTo(soglia) <= 0) {
                continue;
            }
            LocalDate giorno = parseData(g.giorno);
            if (giorno == null || (!tagli.isEmpty() && tagli.get(tagli.size() - 1).giorno.equals(giorno))) {
                continue;
            }
            tagli.add(new Taglio(giorno, i, g, eur));
        }
        return tagli;
    }

    /**
     * Il rigo di un pezzo di tratto {@code [di, df]}. Senza tagli il pezzo è il tratto intero e questo
     * metodo fa esattamente quello che faceva il corpo del ciclo di {@link #generaRighiFiat} prima delle
     * opzioni di taglio.
     *
     * <p>Sui bordi <b>interni</b> ({@code tIni} / {@code tFin} non nulli) i valori non vengono dalle
     * modalità del periodo né dai valori manuali, che descrivono i bordi del tratto : il valore finale è
     * il saldo immediatamente <b>prima</b> della gamba del taglio, quello iniziale del pezzo successivo
     * il saldo immediatamente <b>dopo</b> (circ. 12/E/2016 § 14.1, punti 1 e 2). Entrambi si leggono per
     * posizione nella lista ordinata, quindi comprendono i movimenti dello stesso giorno venuti prima.</p>
     */
    private static void generaRigoPezzo(String anno, String gruppo, List<GambaFiat> gambe, String[] iv,
            LocalDate di, LocalDate df, Taglio tIni, Taglio tFin, boolean aperturaInfraAnno,
            boolean contoCorrente, List<String> avvisiGruppo, ControvaloreEUR cambio,
            List<String[]> righe, List<List<String>> avvisiRighe, List<int[]> rangeCC, List<Integer> idxCC) {
        List<String> avvisi = new ArrayList<>(avvisiGruppo);
        if (tIni != null) {
            avvisi.add("periodo aperto da " + tIni.descrizione() + " (" + tIni.fonte() + ")");
        }
        if (tFin != null) {
            avvisi.add("periodo chiuso il giorno prima di " + tFin.descrizione() + " (" + tFin.fonte() + ")");
        }
        BigDecimal valIni = tIni != null
                ? saldoEURFinoA(gambe, tIni.posizione + 1, tIni.giorno.toString(), cambio, avvisi)
                : valoreIniziale(gambe, iv, di, aperturaInfraAnno, cambio, avvisi);
        BigDecimal valFin = tFin != null
                ? saldoEURFinoA(gambe, tFin.posizione, tFin.giorno.toString(), cambio, avvisi)
                : valoreFinale(gambe, iv, df, cambio, avvisi);

        boolean negativo = valIni.signum() < 0 || valFin.signum() < 0;
        // Opzione "massimo fra giacenza media e saldo finale" (solo righi non conto corrente,
        // e mai su un valore finale scritto a mano) : la media va calcolata PRIMA del salto
        // degli estremi nulli, perche' un tratto aperto e svuotato nell'anno ha i due estremi a
        // zero ma una media positiva, e con l'opzione attiva quel rigo va dichiarato. Il valore
        // manuale riguarda solo il pezzo che finisce sul bordo del tratto.
        BigDecimal media = null;
        if (!contoCorrente && valoreFinaleMaxMedia()
                && (tFin != null || importoONull(iv[IV_VAL_FINALE_MAN]) == null)) {
            media = mediaPeriodo(gambe, di, df, df.toString(), cambio, avvisi);
        }
        // Salto il tratto solo se i due estremi sono GENUINAMENTE nulli (punto 14) : un saldo
        // negativo viene invece portato a zero ma il rigo resta, con l'avviso.
        // Eccezione : per un vero conto corrente estero la misura che conta e' la giacenza
        // MEDIA sull'anno di vita, non i due estremi : un conto aperto e svuotato dentro l'anno
        // ha entrambi gli estremi a zero ma puo' comunque dovere l'IVAFE. Il rigo passa e ci
        // pensa applicaContoCorrente() a decidere media, massimo e imposta.
        if (!negativo && valIni.signum() == 0 && valFin.signum() == 0 && !contoCorrente
                && (media == null || media.signum() == 0)) {
            return;
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
        righe.add(rigaFiat(anno, gruppo, di, df, valIni, valFin, media, giorni,
                trim(iv[IV_STATO]), etichettaValute(gambe, df), avvisi, contoCorrente));
        avvisiRighe.add(avvisi);
        if (contoCorrente) {
            idxCC.add(righe.size() - 1);
            rangeCC.add(new int[] {(int) di.toEpochDay(), (int) df.toEpochDay()});
        }
    }

    // --- valore iniziale / finale di un tratto ---------------------------

    /** Controvalore in EUR del saldo delle prime {@code n} gambe della lista ordinata (saldo a un istante, non a una giornata). */
    private static BigDecimal saldoEURFinoA(List<GambaFiat> gambe, int n, String dataCambioIso,
            ControvaloreEUR cambio, List<String> avvisi) {
        Map<String, BigDecimal> saldi = new TreeMap<>();
        for (int i = 0; i < n && i < gambe.size(); i++) {
            saldi.merge(gambe.get(i).valuta, gambe.get(i).importo, BigDecimal::add);
        }
        BigDecimal tot = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : saldi.entrySet()) {
            if (e.getValue().signum() != 0) {
                tot = tot.add(converti(e.getKey(), e.getValue(), dataCambioIso, cambio, avvisi));
            }
        }
        return tot;
    }

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
            BigDecimal valIni, BigDecimal valFin, BigDecimal media, int giorni, String statoEstero,
            String etichettaValute, List<String> avvisi, boolean contoCorrente) {
        // Valore da dichiarare in colonna 8 e base dell'IVAFE : il saldo finale, oppure la giacenza
        // media del tratto se l'opzione e' attiva e la media e' maggiore. [10] resta il saldo reale.
        BigDecimal valDichiarato = valFin;
        if (media != null && media.compareTo(valFin) > 0) {
            valDichiarato = media;
            avvisi.add("valore finale = giacenza media del periodo " + media.toPlainString()
                    + " EUR, maggiore del saldo a fine periodo "
                    + valFin.setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR");
        }
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
        r[FIAT_COL_VALORE_MEDIO] = media == null ? "" : media.toPlainString();
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
                : ivafeLiquidita(valDichiarato, giorni, giorniAnno, aliquota);
        if (ivafe.signum() > 0) {
            r[FIAT_COL_IVAFE] = ivafe.toPlainString();
            r[FIAT_COL_SOLO_MONITORAGGIO] = "NO";
            avvisi.add("liquidità : IVAFE " + ivafe.toPlainString() + " EUR ("
                    + (privilegiata ? "0,40 %" : "0,20 %") + " di "
                    + valDichiarato.setScale(2, RoundingMode.HALF_UP).toPlainString() + " x " + giorni + "/"
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
     * Divisore della giacenza media annua del <b>conto corrente</b> : sempre 365, qualunque sia il numero
     * di giorni in cui il conto è rimasto aperto nell'anno, e anche nel bisestile. È la definizione
     * ufficiale dell'Agenzia delle entrate, provvedimento n. 73782 del 28 maggio 2015, punto 3.1 : «Il
     * calcolo della giacenza media annua si determina dividendo la somma delle giacenze giornaliere per
     * 365, indipendentemente dal numero di giorni in cui il deposito/conto risulta attivo» ; coerente con
     * la circolare 28/E/2012 § 2.4.1, per cui ai fini della soglia di 5.000 € rileva il «valore medio di
     * giacenza annuo […] a nulla rilevando il periodo di detenzione del rapporto». Qui non conta i giorni
     * due volte : l'IVAFE del conto corrente è fissa e i giorni la prorata da soli, la media decide solo
     * colonna 8 e soglia. Fino al 2026-09-27 il divisore era il numero di giorni di vita del conto.
     *
     * <p>Da <b>non</b> usare per la liquidità codice 14 : lì la media diventa la base di un'imposta
     * proporzionale già rapportata ai giorni (vedi {@link #mediaPeriodo}).</p>
     */
    static final int GIORNI_GIACENZA_MEDIA_ANNUA = 365;

    /**
     * Fase <b>conto corrente estero</b>. Per un gruppo con almeno un tratto {@code EContoCorrente = SI}
     * nell'anno : calcola il <b>valore medio</b> e il <b>valore massimo</b> (EUR) della giacenza
     * sui giorni di vita del conto nell'anno (media divisa per {@value #GIORNI_GIACENZA_MEDIA_ANNUA}), poi riscrive i righi conto corrente con codice
     * individuazione bene {@value #CODICE_BENE_CONTO_CORRENTE}, la giacenza media in
     * {@link #FIAT_COL_VALORE_MEDIO} e — se dovuta — l'IVAFE in {@link #FIAT_COL_IVAFE}.
     *
     * <p>IVAFE in <b>misura fissa</b> {@link #IVAFE_CONTO_CORRENTE_FISSA} € rapportata alla quota
     * (100 %) e al periodo di possesso ({@code giorni tratto / giorni dell'anno}). Non dovuta se il
     * valore medio ≤ {@link #SOGLIA_ESENZIONE_IVAFE} € (verifica sul <b>singolo gruppo wallet</b>) :
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
        BigDecimal[] mediaMassimo = giacenzeGiornaliere(gambe, giorniVita, GIORNI_GIACENZA_MEDIA_ANNUA,
                dataCambio, cambio, avvisiComuni);
        BigDecimal valoreMedio = mediaMassimo[0];
        BigDecimal massimo = mediaMassimo[1];
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

    /**
     * Giacenza media del <b>periodo di detenzione</b> dei giorni da {@code da} ad {@code a} inclusi,
     * per l'opzione {@link #OPZIONE_VALORE_FINALE_MAX_MEDIA} : somma dei saldi giornalieri divisa per
     * i giorni del tratto stesso, cioè per gli stessi giorni che il rigo dichiara in colonna 10.
     *
     * <p><b>Non</b> è la giacenza media annua delle istruzioni DSU (somma / 365 a prescindere dai
     * giorni di apertura), che fino al 2026-09-27 si usava qui ed era sbagliata per questo scopo :
     * l'IVAFE ordinaria è già rapportata ai giorni ({@code valore x aliquota x giorni / giorni
     * dell'anno}), e una media divisa per 365 contava i giorni <b>due volte</b>. L'effetto era che
     * spezzare un rigo faceva pagare meno : 10.000 € tutto l'anno davano 20 € di IVAFE, gli stessi
     * 10.000 € in due righi da 180 e 185 giorni ne davano circa 10. Con la media sul periodo un saldo
     * costante paga lo stesso comunque lo si divida. La media DSU resta materia dell'ISEE, non del
     * quadro RW.</p>
     */
    private static BigDecimal mediaPeriodo(List<GambaFiat> gambe, LocalDate da, LocalDate a,
            String dataCambio, ControvaloreEUR cambio, List<String> avvisi) {
        List<LocalDate> giorni = new ArrayList<>();
        for (LocalDate g = da; !g.isAfter(a); g = g.plusDays(1)) {
            giorni.add(g);
        }
        return giacenzeGiornaliere(gambe, giorni, 0, dataCambio, cambio, avvisi)[0];
    }

    /**
     * Giacenza <b>media</b> e <b>massima</b> in EUR (scala 2) del saldo FIAT a fine giornata sui
     * giorni indicati, che devono essere in ordine crescente. Un saldo giornaliero negativo conta
     * zero (conto in rosso). La conversione è lineare a data fissa {@code dataCambio} : un tasso per
     * valuta, non una conversione per giorno. Unica implementazione, usata dal conto corrente e
     * dall'opzione {@link #OPZIONE_VALORE_FINALE_MAX_MEDIA}.
     *
     * @param divisore giorni per cui dividere la somma dei saldi ; {@code 0} = il numero di giorni
     *                 passati (media del periodo, quella della liquidità codice 14)
     * @return {@code [media, massimo]} ; {@code [0, 0]} se non ci sono giorni
     */
    private static BigDecimal[] giacenzeGiornaliere(List<GambaFiat> gambe, Iterable<LocalDate> giorni,
            int divisore, String dataCambio, ControvaloreEUR cambio, List<String> avvisi) {
        Map<String, BigDecimal> tasso = new HashMap<>();
        List<GambaFiat> ordinate = new ArrayList<>(gambe);
        ordinate.sort(Comparator.comparing(g -> g.giorno));

        int gi = 0;
        Map<String, BigDecimal> saldo = new HashMap<>();
        BigDecimal somma = BigDecimal.ZERO;
        BigDecimal massimo = BigDecimal.ZERO;
        int n = 0;
        for (LocalDate g : giorni) {
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
                BigDecimal t = tasso.computeIfAbsent(e.getKey(), v -> tassoEUR(v, dataCambio, cambio, avvisi));
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
        int d = divisore > 0 ? divisore : n;
        BigDecimal media = d > 0
                ? somma.divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new BigDecimal[] {media, massimo.setScale(2, RoundingMode.HALF_UP)};
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
