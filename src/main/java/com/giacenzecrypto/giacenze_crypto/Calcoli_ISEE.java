package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * I rapporti del Quadro FC2 della DSU (patrimonio mobiliare) per un anno di riferimento: un rigo per rapporto, con
 * tipo (01 conto corrente, 99 altri strumenti), identificativo, codice operatore, valore al 31/12, giacenza media e
 * date di inizio e fine. Non scrive nei movimenti, non tocca la cache dei prezzi fiscali né le mappe del quadro
 * W/RW, non entra in {@code OpzioniRicalcolo.Epoca()}. Nessun campo Swing.
 *
 * <p>Regole decise con l'utente il 2026-11-04 ({@code nocommit/Documentazione/Analisi_ISEE_DSU.md} §19-25):</p>
 * <ul>
 *   <li><b>Rapporto = gruppo wallet</b>, spezzato dove cambia l'<b>operatore</b>. L'operatore viene dai periodi FIAT
 *       di {@code GRUPPO_PERIODO_RW} (Stato estero e identificativo fiscale: "E" + identificativo per l'estero, il
 *       codice fiscale per l'Italia, {@code END} se manca). Conta solo il <b>confine fra due periodi</b> con
 *       operatori diversi (Coinbase Irlanda fino al 19/06/2025, Lussemburgo dal 20/06/2025): l'inizio del primo
 *       periodo e la fine dell'ultimo dicono solo da quando a quando valgono quei dati (le date di licenza MiCA
 *       dei predefiniti non sono aperture), quindi i giorni scoperti prima del primo periodo e dopo l'ultimo
 *       restano al rapporto vicino.</li>
 *   <li><b>Valore al 31/12</b>: le cripto dalla fotografia {@link Calcoli_RW_Giacenze#GiacenzeInizioFineAnno} (la
 *       stessa del quadro W/RW), la liquidità dalle gambe FIAT del quadro W/RW ({@link Calcoli_RW_Fiat#liquidita}).
 *       Tutto al rapporto che copre il 31/12, gli altri valgono 0: sono cessati e un valore sul rapporto chiuso
 *       conterebbe due volte lo stesso patrimonio.</li>
 *   <li><b>Liquidità</b>: con "conto corrente = NO" sul periodo FIAT è sommata nel 99 del gruppo. Con "SI" va in un
 *       rapporto 01 a parte, con la giacenza media annua (somma dei saldi di fine giornata dei giorni del rapporto
 *       / 365, cambio al 31/12 o alla chiusura).</li>
 *   <li><b>Date</b>: inizio = la data scritta dall'utente, altrimenti l'inizio del periodo dopo un cambio di
 *       operatore, altrimenti il primo movimento del gruppo; solo se cade nell'anno. Fine = la data di chiusura
 *       scritta dall'utente, altrimenti la fine del periodo prima di un cambio di operatore; solo se cade
 *       nell'anno. Dai movimenti la fine <b>non</b> si ricava: un conto a saldo zero di solito resta aperto, e lo
 *       dice una nota.</li>
 *   <li>Un gruppo senza movimenti nell'anno e senza saldo al 31/12 non compare.</li>
 *   <li><b>Prezzo mancante = n.d., mai zero</b> (valore incompleto). <b>Giacenza negativa</b> = zero, con nota. I token
 *       segnati <b>SCAM</b> non contano, come nel quadro W/RW, e lo dice una nota.</li>
 *   <li><b>Media giornaliera dei rapporti 99</b>: informativa (la FAQ MLPS n. 13 del 2024 chiede per i VASP il solo
 *       valore), somma dei valori di ogni giorno / 365, calcolata solo su richiesta
 *       ({@link #calcolaMediaGiornaliera}) perché serve un prezzo per ogni giorno.</li>
 * </ul>
 */
public final class Calcoli_ISEE {

    private Calcoli_ISEE() {
    }

    /** Divisore della giacenza media annua: sempre 365, come da istruzioni DSU (par. 4.1). */
    public static final int GIORNI_DIVISORE = 365;
    /** Tipo rapporto della Sezione I: conto corrente (le carte prepagate con IBAN ci sono assimilate). */
    public static final String TIPO_CONTO_CORRENTE = "01";
    /** Tipo rapporto della Sezione II: altri strumenti e rapporti finanziari. */
    public static final String TIPO_ALTRI_STRUMENTI = "99";
    /** Codice operatore quando non c'è un operatore finanziario (autocustodia) o non è noto. */
    public static final String OPERATORE_END = "END";
    /** Parte finale della chiave di un rapporto di un gruppo senza periodi FIAT. */
    public static final String SENZA_PERIODO = "NESSUN_PERIODO";
    /** Caratteri ammessi dopo la "E" del codice di un operatore estero (istruzioni DSU par. 4.1). */
    public static final int MAX_CODICE_ESTERO = 15;

    private static final DateTimeFormatter GG_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Valore in euro di una quantità di moneta alla fine di un giorno, {@code null} se il prezzo non si trova. In
     * produzione {@link #VALORE_MERCATO}: la stessa funzione della fotografia del 31/12, all'istante
     * {@link #istanteFineGiornata}.
     */
    @FunctionalInterface
    public interface ValoreFineGiornata {
        BigDecimal valore(Moneta moneta, BigDecimal qta, LocalDate giorno);
    }

    /**
     * Il valore di produzione: {@link Calcoli_RW_Giacenze#PREZZO_MERCATO} all'istante di fine giornata, quindi con le
     * stesse regole del valore al 31/12 (prezzi personalizzati per primi, alias, e-money, crediti dei fallimenti).
     */
    public static final ValoreFineGiornata VALORE_MERCATO = valoreDa(Calcoli_RW_Giacenze.PREZZO_MERCATO);

    /** Il valore di fine giornata dato da una funzione di prezzo della fotografia, chiesta a {@link #istanteFineGiornata}. */
    static ValoreFineGiornata valoreDa(Calcoli_RW_Giacenze.PrezzoGiacenza prezzo) {
        return (m, qta, giorno) -> {
            Moneta c = m.ClonaMoneta();
            c.Qta = qta.toPlainString();
            String v = prezzo.prezzo(c, istanteFineGiornata(giorno));
            if (v == null || v.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(v);
            } catch (NumberFormatException e) {
                return null;
            }
        };
    }

    /** Liquidità in valuta di un gruppo, in euro. In produzione {@link Calcoli_RW_Fiat.Liquidita}. */
    public interface LiquiditaFiat {
        /** {@code true} se il gruppo ha movimenti in valuta. */
        boolean presente(String gruppo);

        /** I gruppi con movimenti in valuta (anche quelli senza nessun movimento cripto). */
        Set<String> gruppi();

        /** Giorno del primo movimento in valuta del gruppo, {@code null} se non ne ha. */
        LocalDate primo(String gruppo);

        /** Giorno dell'ultimo movimento in valuta non posteriore a {@code limite}, {@code null} se non ne ha. */
        LocalDate ultimoFinoA(String gruppo, LocalDate limite);

        /** Saldo a fine giornata, convertito alla data del cambio. Può essere negativo. */
        BigDecimal saldoEUR(String gruppo, LocalDate giorno, LocalDate dataCambio, List<String> avvisi);

        /** Somma dei saldi di fine giornata (negativi = zero) dei giorni da {@code da} ad {@code a}, diviso 365. */
        BigDecimal mediaAnnua(String gruppo, LocalDate da, LocalDate a, LocalDate dataCambio, List<String> avvisi);
    }

    /** Nessuna liquidità: per i gruppi o le prove che non ne hanno. */
    public static final LiquiditaFiat SENZA_LIQUIDITA = new LiquiditaFiat() {
        @Override
        public boolean presente(String gruppo) {
            return false;
        }

        @Override
        public Set<String> gruppi() {
            return Set.of();
        }

        @Override
        public LocalDate primo(String gruppo) {
            return null;
        }

        @Override
        public LocalDate ultimoFinoA(String gruppo, LocalDate limite) {
            return null;
        }

        @Override
        public BigDecimal saldoEUR(String gruppo, LocalDate giorno, LocalDate dataCambio, List<String> avvisi) {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal mediaAnnua(String gruppo, LocalDate da, LocalDate a, LocalDate dataCambio, List<String> avvisi) {
            return BigDecimal.ZERO;
        }
    };

    /** Dati di un rapporto scritti dall'utente nel tab ISEE, tutti facoltativi. */
    public static final class DatiUtente {
        /** IBAN o numero di conto / ID utente. Vuoto = il nome del gruppo, con nota. */
        public String identificativo = "";
        /** Codice operatore. Vuoto = quello del periodo FIAT. */
        public String operatore = "";
        /** Apertura del rapporto. {@code null} = dal primo movimento o dal cambio di operatore. */
        public LocalDate apertura;
        /** Chiusura del rapporto. {@code null} = ancora aperto (o fino al cambio di operatore). */
        public LocalDate chiusura;
    }

    /** Una moneta del gruppo al 31/12, come la dà la fotografia del quadro W/RW. */
    public static final class Posizione {
        public final String moneta;
        public final BigDecimal qtaFine;
        /** Valore in euro al 31/12, {@code null} = n.d. (prezzo mancante). Zero se la quantità non è positiva. */
        public final BigDecimal valoreFine;

        Posizione(String moneta, BigDecimal qtaFine, BigDecimal valoreFine) {
            this.moneta = moneta;
            this.qtaFine = qtaFine;
            this.valoreFine = valoreFine;
        }

        public boolean negativa() {
            return qtaFine.signum() < 0;
        }
    }

    /** Un rigo del Quadro FC2. */
    public static final class Rapporto {
        /**
         * Chiave stabile negli anni per i dati dell'utente: {@code gruppo|tipo|periodo}, dove il periodo è il primo
         * periodo FIAT del tratto ({@code ChiaveDefault} per i predefiniti, {@code P} + progressivo per quelli
         * dell'utente, {@link #SENZA_PERIODO} se il gruppo non ne ha). Non dipende dall'identificativo fiscale:
         * completarlo o correggerlo non deve staccare l'IBAN o la data di chiusura già scritti. Se i dati dell'utente
         * stanno sotto l'identità di un altro periodo dello stesso tratto ({@code Segmento#identitaDiRicerca}), la
         * chiave è quella: il tab li riscrive lì.
         */
        public final String chiave;
        public final String gruppo;
        public final String tipo;
        /** Codice operatore ricavato dal periodo FIAT ({@code END} se manca). */
        public final String operatorePeriodo;
        /** Stato estero del periodo FIAT, {@code ""} se manca. */
        public final String statoEstero;
        /** Codice operatore da dichiarare: quello dell'utente se l'ha scritto, altrimenti {@link #operatorePeriodo}. */
        public String operatore;
        public String identificativo;
        /** Primo e ultimo giorno del rapporto dentro l'anno: i giorni delle giacenze medie. */
        public LocalDate da;
        public LocalDate a;
        /** Date da dichiarare: solo se il rapporto è aperto o cessato nell'anno. */
        public LocalDate dataInizio;
        public LocalDate dataFine;
        /** {@code true} se il rapporto esiste al 31/12 e quindi porta il valore di fine anno. */
        public boolean copreFineAnno;
        /** Valore al 31/12: cripto più liquidità (99) o saldo del conto (01). */
        public BigDecimal valore = BigDecimal.ZERO;
        /** {@code true} se almeno una moneta con quantità positiva non ha prezzo al 31/12. */
        public boolean valoreIncompleto;
        /** La parte di {@link #valore} che viene dalla liquidità (sempre zero sul 01, dove è tutto il valore). */
        public BigDecimal liquidita = BigDecimal.ZERO;
        /** Sul 99: {@code true} se la liquidità del gruppo sta qui (niente conto corrente nel tratto). */
        public boolean conLiquidita;
        /**
         * Giacenza media annua: sul 01 quella delle istruzioni DSU, sempre calcolata. Sul 99 la media giornaliera
         * informativa, {@code null} finché non la si chiede con {@link #calcolaMediaGiornaliera}.
         */
        public BigDecimal giacenzaMedia;
        /** {@code true} se nella media giornaliera del 99 qualche giorno con giacenza è rimasto senza prezzo. */
        public boolean mediaIncompleta;
        /** Giorni con giacenza ma senza prezzo giornaliero, nella media del 99. */
        public int giorniSenzaPrezzo;
        /** Le monete al 31/12 (solo sul 99 che copre il 31/12). */
        public final List<Posizione> posizioni = new ArrayList<>();
        /** Avvisi per l'utente, senza ripetizioni. */
        public final Set<String> note = new LinkedHashSet<>();

        Rapporto(String chiave, String gruppo, String tipo, String operatorePeriodo, String statoEstero) {
            this.chiave = chiave;
            this.gruppo = gruppo;
            this.tipo = tipo;
            this.operatorePeriodo = operatorePeriodo;
            this.statoEstero = statoEstero;
        }

        public boolean contoCorrente() {
            return TIPO_CONTO_CORRENTE.equals(tipo);
        }

        public boolean haNegative() {
            for (Posizione p : posizioni) {
                if (p.negativa()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** I rapporti di un anno, più quello che serve per calcolare dopo la media giornaliera. */
    public static final class Risultato {
        public final int anno;
        /** Nell'ordine dei gruppi ({@link Principale_GruppiWalletRW#ORDINE_GRUPPO}), poi per data, il 01 prima del 99. */
        public final List<Rapporto> rapporti = new ArrayList<>();
        private final Map<String, Attivita> attivita;
        private final LiquiditaFiat liquidita;
        /** {@code true} dopo {@link #calcolaMediaGiornaliera}: le medie dei 99 sono quelle giornaliere. */
        public boolean mediaCalcolata;
        /**
         * I giorni con giacenza rimasti senza prezzo nell'ultima media giornaliera, uno per moneta e giorno (fra i
         * gruppi la quantità si somma), in ordine di moneta e di giorno.
         */
        public final List<PrezzoMancante> prezziMancanti = new ArrayList<>();

        Risultato(int anno, Map<String, Attivita> attivita, LiquiditaFiat liquidita) {
            this.anno = anno;
            this.attivita = attivita;
            this.liquidita = liquidita;
        }

        /** I rapporti di un gruppo. */
        public List<Rapporto> delGruppo(String gruppo) {
            List<Rapporto> out = new ArrayList<>();
            for (Rapporto r : rapporti) {
                if (r.gruppo.equals(gruppo)) {
                    out.add(r);
                }
            }
            return out;
        }
    }

    /** I movimenti cripto di un gruppo fino al 31/12, per le date e per la media giornaliera. */
    private static final class Attivita {
        LocalDate primo;
        LocalDate ultimo;
        boolean movimentiNellAnno;
        /** {@code Moneta;Tipo} come la fotografia del quadro W/RW. */
        final Map<String, Andamento> monete = new TreeMap<>();
        /** Media giornaliera già calcolata: valore di ogni giorno dell'anno e giorni senza prezzo. */
        BigDecimal[] valoreGiorno;
        boolean[] senzaPrezzo;
    }

    private static final class Andamento {
        final Moneta moneta;
        final List<Object[]> variazioni = new ArrayList<>();

        Andamento(Moneta moneta) {
            this.moneta = moneta;
        }

        BigDecimal qtaAl(LocalDate giorno) {
            BigDecimal q = BigDecimal.ZERO;
            for (Object[] v : variazioni) {
                if (!((LocalDate) v[0]).isAfter(giorno)) {
                    q = q.add((BigDecimal) v[1]);
                }
            }
            return q;
        }
    }

    /**
     * Un giorno con giacenza senza prezzo nella media giornaliera: la moneta come la cercano i prezzi (simbolo del
     * movimento, rete e address), la quantità di fine giornata sommata fra i gruppi e i gruppi che la tengono.
     */
    public static final class PrezzoMancante {
        public final Moneta moneta;
        public final LocalDate giorno;
        public BigDecimal qta;
        public final Set<String> gruppi = new java.util.TreeSet<>();

        PrezzoMancante(Moneta moneta, LocalDate giorno, BigDecimal qta) {
            this.moneta = moneta;
            this.giorno = giorno;
            this.qta = qta;
        }

        /** Chiave della moneta per i prezzi: simbolo, rete e address (minuscolo). */
        public String chiaveMoneta() {
            return chiaveMoneta(moneta);
        }

        static String chiaveMoneta(Moneta m) {
            String rete = m.Rete == null ? "" : m.Rete.trim();
            String address = m.MonetaAddress == null ? "" : m.MonetaAddress.trim().toLowerCase();
            return m.Moneta + "|" + rete + "|" + address;
        }
    }

    /** Un giorno con giacenza di una moneta: quello che la media giornaliera valorizza. */
    public record GiornoConGiacenza(Moneta moneta, LocalDate giorno, BigDecimal qta, String gruppo) {
    }

    /** Un tratto del gruppo con lo stesso operatore: da {@code inizio} (null = da sempre) a {@code fine} (null = in corso). */
    static final class Segmento {
        LocalDate inizio;
        LocalDate fine;
        /** L'ultimo periodo FIAT del tratto, {@code null} se il gruppo non ne ha. */
        String[] riga;
        /** Tutti i periodi FIAT del tratto, in ordine di inizio. Vuoto se il gruppo non ne ha. */
        final List<String[]> righe = new ArrayList<>();
        String operatore;
        /** Identità del primo periodo del tratto, per la chiave del rapporto. */
        String identita;

        /** {@code true} per il primo tratto del gruppo. */
        boolean primo;

        /**
         * Le identità con cui si cercano i dati dell'utente: quella del primo periodo, poi quelle degli altri periodi
         * del tratto. Aggiungere un periodo prima del primo, con lo stesso operatore, non stacca i dati già scritti.
         * Il primo tratto cerca anche {@link #SENZA_PERIODO}: i dati scritti quando il gruppo non aveva periodi FIAT
         * restano al rapporto quando il primo periodo viene aggiunto.
         */
        List<String> identitaDiRicerca() {
            List<String> out = new ArrayList<>();
            out.add(identita);
            for (String[] r : righe) {
                String id = identitaPeriodo(r);
                if (!out.contains(id)) {
                    out.add(id);
                }
            }
            if (primo && !out.contains(SENZA_PERIODO)) {
                out.add(SENZA_PERIODO);
            }
            return out;
        }
    }

    // --- ingressi di produzione ------------------------------------------

    /**
     * I rapporti dell'anno dall'archivio: fotografia cripto del quadro W/RW a prezzo di mercato, liquidità al cambio
     * Banca d'Italia, periodi FIAT di {@code GRUPPO_PERIODO_RW}.
     *
     * @param datiUtente chiave del rapporto → dati scritti dall'utente ({@code null} o risultato {@code null} = nessuno)
     */
    public static Risultato calcola(int anno, Function<String, DatiUtente> datiUtente) {
        return calcola(anno, Calcoli_RW_Giacenze.PREZZO_MERCATO, Calcoli_RW_Fiat::controvaloreBancaItalia, datiUtente);
    }

    /** Come {@link #calcola(int, Function)}, con prezzi e cambio iniettati (per le prove). */
    static Risultato calcola(int anno, Calcoli_RW_Giacenze.PrezzoGiacenza prezzo, Calcoli_RW_Fiat.ControvaloreEUR cambio,
            Function<String, DatiUtente> datiUtente) {
        // Della fotografia si legge solo il valore di fine anno ([10]): i prezzi di inizio anno costerebbero solo
        // scaricamenti. Con la finestra dell'anno intero non ci sono apporti sui confini, quindi [10] chiede il
        // prezzo solo all'istante di fine anno.
        long fineAnno = istanteFineAnno(anno);
        Calcoli_RW_Giacenze.PrezzoGiacenza soloFineAnno = (m, istante) -> istante == fineAnno
                ? prezzo.prezzo(m, istante) : Calcoli_RW_Giacenze.VALORE_ZERO;
        Map<String, List<String[]>> giacenze = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno(String.valueOf(anno), soloFineAnno);
        Calcoli_RW_Fiat.Liquidita liq = Calcoli_RW_Fiat.liquidita(cambio);
        LiquiditaFiat liquidita = new LiquiditaFiat() {
            @Override
            public boolean presente(String gruppo) {
                return liq.presente(gruppo);
            }

            @Override
            public Set<String> gruppi() {
                return liq.gruppi();
            }

            @Override
            public LocalDate primo(String gruppo) {
                return liq.primo(gruppo);
            }

            @Override
            public LocalDate ultimoFinoA(String gruppo, LocalDate limite) {
                return liq.ultimoFinoA(gruppo, limite);
            }

            @Override
            public BigDecimal saldoEUR(String gruppo, LocalDate giorno, LocalDate dataCambio, List<String> avvisi) {
                return liq.saldoEUR(gruppo, giorno, dataCambio, avvisi);
            }

            @Override
            public BigDecimal mediaAnnua(String gruppo, LocalDate da, LocalDate a, LocalDate dataCambio, List<String> avvisi) {
                return liq.mediaAnnua(gruppo, da, a, dataCambio, avvisi);
            }
        };
        return calcola(anno, MappaCryptoWallet.values(), Calcoli_ISEE::gruppoWalletDi, giacenze,
                Principale_GruppiWalletRW::caricaPeriodi, liquidita, datiUtente, Calcoli_ISEE::aliasDi);
    }

    /**
     * Le monete con quantità positiva al 31/12 di tutti i gruppi, per scaricarne i prezzi in un lotto solo
     * ({@link Prezzi#PreScaricaPrezziMonete}) prima di {@link #calcola(int, Function)}: sono quelle che la fotografia
     * valorizzerà, all'istante {@link #istanteFineAnno}.
     */
    public static List<Moneta> moneteAFineAnno(int anno) {
        long fineAnno = istanteFineAnno(anno);
        List<Moneta> out = new ArrayList<>();
        Calcoli_RW_Giacenze.GiacenzeInizioFineAnno(String.valueOf(anno), (m, istante) -> {
            if (istante == fineAnno && !"FIAT".equalsIgnoreCase(m.Tipo) && new BigDecimal(m.Qta).signum() > 0) {
                out.add(m.ClonaMoneta());
            }
            return Calcoli_RW_Giacenze.VALORE_ZERO;
        });
        return out;
    }

    /** L'istante a cui la fotografia del quadro W/RW prezza il 31/12: le 00:00 del primo gennaio successivo. */
    public static long istanteFineAnno(int anno) {
        return FunzioniDate.ConvertiDatainLongMinuto((anno + 1) + "-01-01 00:00");
    }

    /**
     * L'istante del prezzo di fine giornata della media: le 00:00 del giorno dopo, ora italiana. Per il 31/12 è
     * {@link #istanteFineAnno}, quindi l'ultimo giorno della media vale quanto il valore al 31/12.
     */
    public static long istanteFineGiornata(LocalDate giorno) {
        return FunzioniDate.ConvertiDatainLongMinuto(giorno.plusDays(1) + " 00:00");
    }

    /**
     * Gruppo wallet di un movimento, letto come lo legge la fotografia del quadro W/RW: un wallet non ancora
     * assegnato finisce in "Wallet 99" (e l'associazione viene salvata, come in ogni calcolo W/RW).
     */
    static String gruppoWalletDi(String[] movimento) {
        String wallet = movimento[3];
        if (wallet == null || wallet.isEmpty()) {
            return null;
        }
        return DatabaseH2.Pers_GruppoWallet_Leggi(wallet, true);
    }

    /** Nome del gruppo per l'utente: l'alias se c'è, altrimenti il nome ("Wallet 102"). */
    static String aliasDi(String gruppo) {
        String[] a = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        String alias = a != null && a[1] != null ? a[1].trim() : "";
        return alias.isEmpty() ? gruppo : alias;
    }

    // --- calcolo ----------------------------------------------------------

    /**
     * Il calcolo, con tutte le fonti iniettate.
     *
     * @param movimenti   tutti i movimenti (quelli dopo l'anno vengono ignorati)
     * @param gruppoDi    gruppo wallet di un movimento ({@code null} = scartato)
     * @param giacenze    fotografia cripto di fine anno per gruppo, righi {@code String[17]} di
     *                    {@link Calcoli_RW_Giacenze}: {@code [7]} moneta, {@code [8]} quantità, {@code [10]} valore
     *                    ({@code null} = n.d.)
     * @param periodiDi   periodi del gruppo in forma GUI ({@link Principale_GruppiWalletRW#caricaPeriodi})
     * @param liquidita   liquidità in valuta dei gruppi
     * @param datiUtente  chiave del rapporto → dati dell'utente, anche {@code null}
     * @param aliasDi     nome del gruppo da usare come identificativo quando l'utente non l'ha scritto
     */
    static Risultato calcola(int anno, Collection<String[]> movimenti, Function<String[], String> gruppoDi,
            Map<String, List<String[]>> giacenze, Function<String, List<String[]>> periodiDi, LiquiditaFiat liquidita,
            Function<String, DatiUtente> datiUtente, Function<String, String> aliasDi) {
        LocalDate inizioAnno = LocalDate.of(anno, 1, 1);
        LocalDate fineAnno = LocalDate.of(anno, 12, 31);
        Map<String, Attivita> attivita = scansiona(movimenti, gruppoDi, inizioAnno, fineAnno);
        // La liquidità può avere date sue: il Fiat Wallet di Crypto.com è un file a parte e può cominciare prima
        // del primo movimento cripto, e un gruppo può avere solo valuta.
        for (String gruppo : liquidita.gruppi()) {
            LocalDate primo = liquidita.primo(gruppo);
            LocalDate ultimo = liquidita.ultimoFinoA(gruppo, fineAnno);
            if (primo == null || primo.isAfter(fineAnno)) {
                continue;
            }
            Attivita a = attivita.computeIfAbsent(gruppo, k -> new Attivita());
            if (a.primo == null || primo.isBefore(a.primo)) {
                a.primo = primo;
            }
            if (ultimo != null && (a.ultimo == null || ultimo.isAfter(a.ultimo))) {
                a.ultimo = ultimo;
            }
            if (ultimo != null && !ultimo.isBefore(inizioAnno)) {
                a.movimentiNellAnno = true;
            }
        }
        Risultato ris = new Risultato(anno, attivita, liquidita);

        List<String> gruppi = new ArrayList<>(attivita.keySet());
        gruppi.sort(Principale_GruppiWalletRW.ORDINE_GRUPPO);
        for (String gruppo : gruppi) {
            Attivita att = attivita.get(gruppo);
            List<String> avvisiGruppo = new ArrayList<>();

            // Valore al 31/12: cripto dalla fotografia, liquidità dalle gambe FIAT.
            List<Posizione> posizioni = new ArrayList<>();
            BigDecimal cripto = BigDecimal.ZERO;
            boolean incompleto = false;
            boolean qualcosaAFineAnno = false;
            Map<String, BigDecimal> qtaFoto = new HashMap<>();
            int scam = 0;
            for (String[] r : giacenze.getOrDefault(gruppo, List.of())) {
                BigDecimal q = numero(r[8]);
                if (q == null) {
                    continue;
                }
                qtaFoto.merge(r[7], q, BigDecimal::add);
                if (q.signum() == 0) {
                    continue; // c'era a inizio anno, non più alla fine
                }
                if (Funzioni.isSCAM(r[7])) {
                    scam++; // come nel quadro W/RW: mostrato, mai contato
                    continue;
                }
                qualcosaAFineAnno = true;
                BigDecimal val = q.signum() > 0 ? numero(r[10]) : BigDecimal.ZERO;
                posizioni.add(new Posizione(r[7], q, val));
                if (q.signum() > 0 && val == null) {
                    incompleto = true;
                } else if (val != null) {
                    cripto = cripto.add(val);
                }
                if (q.signum() < 0) {
                    avvisiGruppo.add("giacenza negativa di " + q.toPlainString() + " " + r[7]
                            + " al 31/12 (movimento mancante): contata zero");
                }
            }
            if (scam > 0) {
                avvisiGruppo.add(scam + (scam == 1 ? " token segnato" : " token segnati")
                        + " SCAM al 31/12 non contati nel valore, come nel quadro W/RW");
            }
            controllaQuantita(att, fineAnno, qtaFoto, avvisiGruppo);
            BigDecimal fiat = liquidita.saldoEUR(gruppo, fineAnno, fineAnno, avvisiGruppo);
            if (fiat.signum() != 0) {
                qualcosaAFineAnno = true;
            }
            if (fiat.signum() < 0) {
                avvisiGruppo.add("saldo in valuta negativo al 31/12 (" + euro2(fiat) + " EUR, movimento mancante): contato zero");
                fiat = BigDecimal.ZERO;
            }
            if (!att.movimentiNellAnno && !qualcosaAFineAnno) {
                continue; // vuoto da prima dell'anno
            }
            boolean saldoZero = fiat.signum() == 0 && posizioni.stream().noneMatch(p -> p.qtaFine.signum() > 0);
            boolean haCripto = !att.monete.isEmpty() || !posizioni.isEmpty();

            List<Segmento> segmenti = segmenti(periodiDi.apply(gruppo));
            for (int i = 0; i < segmenti.size(); i++) {
                Segmento s = segmenti.get(i);
                boolean primo = i == 0;
                boolean ultimo = i == segmenti.size() - 1;
                if ((s.inizio != null && s.inizio.isAfter(fineAnno)) || (s.fine != null && s.fine.isBefore(inizioAnno))
                        || (s.fine != null && att.primo != null && s.fine.isBefore(att.primo))) {
                    continue; // fuori dall'anno, o il gruppo non esisteva ancora
                }
                boolean contoCorrente = s.riga != null && Principale_GruppiWalletRW.CONTO_CORRENTE_SI
                        .equals(trim(s.riga[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE]));
                String stato = s.riga == null ? "" : trim(s.riga[Principale_GruppiWalletRW.COL_STATO_ESTERO]);
                List<String> tipi = new ArrayList<>();
                if (contoCorrente && liquidita.presente(gruppo)) {
                    tipi.add(TIPO_CONTO_CORRENTE);
                }
                if (!contoCorrente || haCripto || tipi.isEmpty()) {
                    tipi.add(TIPO_ALTRI_STRUMENTI);
                }
                for (String tipo : tipi) {
                    String chiave = chiave(gruppo, tipo, s.identita);
                    DatiUtente du = null;
                    for (String id : datiUtente == null ? List.<String>of() : s.identitaDiRicerca()) {
                        du = datiUtente.apply(chiave(gruppo, tipo, id));
                        if (du != null) {
                            chiave = chiave(gruppo, tipo, id); // il tab riscrive i dati dove li ha trovati
                            break;
                        }
                    }
                    boolean copre = ultimo || s.fine == null || !s.fine.isBefore(fineAnno);
                    if (du != null && du.chiusura != null && du.chiusura.isBefore(inizioAnno) && (saldoZero || !copre)) {
                        continue; // chiuso in un anno precedente (con un saldo resta, e lo dice la nota)
                    }
                    if (du != null && du.apertura != null && du.apertura.isAfter(fineAnno)) {
                        continue; // aperto dopo l'anno
                    }
                    Rapporto r = new Rapporto(chiave, gruppo, tipo, s.operatore, stato);
                    compilaRapporto(r, s, primo, ultimo, du, att, inizioAnno, fineAnno, aliasDi.apply(gruppo));
                    r.conLiquidita = !r.contoCorrente() && !contoCorrente;

                    if (r.contoCorrente()) {
                        LocalDate cambioAl = r.copreFineAnno ? fineAnno : r.a;
                        if (r.copreFineAnno) {
                            r.valore = fiat;
                        }
                        List<String> avvisi = new ArrayList<>();
                        r.giacenzaMedia = liquidita.mediaAnnua(gruppo, r.da, r.a, cambioAl, avvisi);
                        r.note.addAll(avvisi);
                    } else if (r.copreFineAnno) {
                        r.posizioni.addAll(posizioni);
                        r.valoreIncompleto = incompleto;
                        r.liquidita = r.conLiquidita ? fiat : BigDecimal.ZERO;
                        r.valore = cripto.add(r.liquidita);
                    }
                    if (r.copreFineAnno) {
                        r.note.addAll(avvisiGruppo);
                        if (saldoZero && r.dataFine == null && att.ultimo != null && !att.ultimo.isBefore(inizioAnno)) {
                            r.note.add("saldo a zero dal " + att.ultimo.format(GG_MM_AAAA)
                                    + ": se il conto è stato chiuso, la data di chiusura si indica nel rapporto");
                        }
                        if (du != null && du.chiusura != null && du.chiusura.isBefore(fineAnno) && !saldoZero) {
                            // il valore resta qui: toglierlo farebbe sparire un patrimonio che i movimenti mostrano
                            r.note.add("rapporto chiuso il " + du.chiusura.format(GG_MM_AAAA)
                                    + " ma al 31/12 il gruppo ha ancora un saldo: controlla i movimenti o la data di chiusura");
                        }
                    }
                    ris.rapporti.add(r);
                }
            }
        }
        return ris;
    }

    /** Identificativo, operatore, giorni e date di un rapporto. */
    private static void compilaRapporto(Rapporto r, Segmento s, boolean primo, boolean ultimo, DatiUtente du,
            Attivita att, LocalDate inizioAnno, LocalDate fineAnno, String alias) {
        String operatoreUtente = du == null ? "" : trim(du.operatore);
        r.operatore = operatoreUtente.isEmpty() ? r.operatorePeriodo : operatoreUtente.toUpperCase();
        if (operatoreUtente.isEmpty()) {
            if (s.riga == null) {
                r.note.add("nessun periodo FIAT con i dati dell'intermediario: operatore " + OPERATORE_END);
            } else if (OPERATORE_END.equals(r.operatorePeriodo)) {
                r.note.add("identificativo fiscale dell'intermediario mancante nel periodo FIAT: operatore " + OPERATORE_END);
            } else if (r.operatore.startsWith("E") && !StatiEsteri.isItalia(r.statoEstero)
                    && r.operatore.length() - 1 > MAX_CODICE_ESTERO) {
                r.note.add("codice operatore oltre i " + MAX_CODICE_ESTERO + " caratteri dopo la E: da accorciare");
            }
        }
        String identUtente = du == null ? "" : trim(du.identificativo);
        if (identUtente.isEmpty()) {
            r.identificativo = alias;
            r.note.add("identificativo del rapporto da completare (IBAN o numero di conto): indicato il nome del gruppo");
        } else {
            // Il solo codice cliente non dice di quale operatore si parla: davanti sta sempre l'alias del gruppo.
            r.identificativo = alias + " - " + identUtente;
        }

        // Inizio: data dell'utente, altrimenti cambio di operatore, altrimenti primo movimento.
        LocalDate inizio;
        if (du != null && du.apertura != null) {
            inizio = du.apertura;
        } else if (!primo && s.inizio != null) {
            inizio = att.primo != null && att.primo.isAfter(s.inizio) ? att.primo : s.inizio;
        } else {
            inizio = att.primo;
        }
        // Fine: data dell'utente, altrimenti cambio di operatore. Mai dai movimenti.
        LocalDate fine = du != null && du.chiusura != null ? du.chiusura : (!ultimo ? s.fine : null);

        r.dataInizio = inizio != null && !inizio.isBefore(inizioAnno) && !inizio.isAfter(fineAnno) ? inizio : null;
        r.dataFine = fine != null && !fine.isBefore(inizioAnno) && !fine.isAfter(fineAnno) ? fine : null;
        // I giorni partono dal tratto (o dall'apertura scritta dall'utente), non dal primo movimento: prima di quello
        // i saldi sono zero comunque, e una fonte con date sue (il Fiat Wallet) non resta tagliata.
        LocalDate partenza = du != null && du.apertura != null ? du.apertura : (!primo ? s.inizio : null);
        r.da = partenza != null && partenza.isAfter(inizioAnno) ? partenza : inizioAnno;
        LocalDate fineSegmento = !ultimo && s.fine != null && s.fine.isBefore(fineAnno) ? s.fine : fineAnno;
        r.a = fine != null && fine.isBefore(fineSegmento) ? fine : fineSegmento;
        if (r.a.isBefore(r.da)) {
            r.a = r.da;
        }
        r.copreFineAnno = ultimo || s.fine == null || !s.fine.isBefore(fineAnno);
    }

    /**
     * I tratti del gruppo con lo stesso operatore, dai periodi FIAT in ordine di inizio
     * ({@link Principale_GruppiWalletRW#finestreEffettive}, la stessa lettura del quadro W/RW). Un cambio di
     * operatore taglia all'inizio del periodo nuovo. Il primo tratto parte da sempre e l'ultimo non finisce:
     * l'inizio del primo periodo e la fine dell'ultimo non sono aperture né chiusure.
     */
    static List<Segmento> segmenti(List<String[]> periodi) {
        List<Segmento> out = new ArrayList<>();
        for (Principale_GruppiWalletRW.Finestra f
                : Principale_GruppiWalletRW.finestreEffettive(periodi, Principale_GruppiWalletRW.TIPO_FIAT)) {
            String op = operatoreDelPeriodo(f.riga);
            Segmento ultimo = out.isEmpty() ? null : out.get(out.size() - 1);
            if (ultimo != null && (ultimo.operatore.equals(op) || f.inizio == null)) {
                ultimo.riga = f.riga; // stesso operatore (o periodo senza data da agganciare): stesso rapporto
                ultimo.righe.add(f.riga);
                continue;
            }
            Segmento s = new Segmento();
            s.inizio = ultimo == null ? null : f.inizio;
            s.riga = f.riga;
            s.righe.add(f.riga);
            s.operatore = op;
            s.identita = identitaPeriodo(f.riga);
            if (ultimo != null) {
                ultimo.fine = f.inizio.minusDays(1);
            }
            out.add(s);
        }
        if (out.isEmpty()) {
            Segmento s = new Segmento();
            s.operatore = OPERATORE_END;
            s.identita = SENZA_PERIODO;
            out.add(s);
        }
        out.get(0).primo = true;
        return out;
    }

    /** La chiave di un rapporto: {@code gruppo|tipo|identità del periodo}. */
    static String chiave(String gruppo, String tipo, String identita) {
        return gruppo + "|" + tipo + "|" + identita;
    }

    /**
     * Identità stabile di un periodo FIAT: {@code ChiaveDefault} per i predefiniti (resta anche quando l'utente li
     * modifica), {@code P} + progressivo per quelli dell'utente.
     */
    static String identitaPeriodo(String[] riga) {
        String chiaveDefault = trim(riga[Principale_GruppiWalletRW.COL_CHIAVE_DEFAULT]);
        return chiaveDefault.isEmpty() ? "P" + trim(riga[Principale_GruppiWalletRW.COL_PROGRESSIVO]) : chiaveDefault;
    }

    /**
     * Codice operatore del Quadro FC2 da un periodo FIAT: in Italia il codice fiscale, all'estero "E" seguito
     * dall'identificativo (solo lettere e cifre), {@link #OPERATORE_END} se l'identificativo manca.
     */
    static String operatoreDelPeriodo(String[] riga) {
        if (riga == null) {
            return OPERATORE_END;
        }
        String ident = trim(riga[Principale_GruppiWalletRW.COL_IDENT_FISCALE]).toUpperCase();
        if (ident.isEmpty()) {
            return OPERATORE_END;
        }
        if (StatiEsteri.isItalia(trim(riga[Principale_GruppiWalletRW.COL_STATO_ESTERO]))) {
            return ident;
        }
        return "E" + ident.replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Confronta la quantità di fine anno dei movimenti con la fotografia del quadro W/RW: la media giornaliera
     * e il valore al 31/12 devono parlare delle stesse monete. Una differenza diventa una nota.
     */
    private static void controllaQuantita(Attivita att, LocalDate fineAnno, Map<String, BigDecimal> qtaFoto,
            List<String> avvisi) {
        Map<String, BigDecimal> qtaMov = new HashMap<>();
        for (Andamento an : att.monete.values()) {
            qtaMov.merge(an.moneta.Moneta, an.qtaAl(fineAnno), BigDecimal::add);
        }
        Set<String> monete = new LinkedHashSet<>(qtaMov.keySet());
        monete.addAll(qtaFoto.keySet());
        for (String m : monete) {
            BigDecimal a = qtaMov.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal b = qtaFoto.getOrDefault(m, BigDecimal.ZERO);
            if (a.compareTo(b) != 0) {
                avvisi.add("quantità di " + m + " al 31/12 diversa dalla fotografia del quadro W/RW ("
                        + a.toPlainString() + " contro " + b.toPlainString() + ")");
            }
        }
    }

    /** I movimenti fino al 31/12, per gruppo: date, monete (chiave {@code Moneta;Tipo}, FIAT escluso) e variazioni. */
    private static Map<String, Attivita> scansiona(Collection<String[]> movimenti, Function<String[], String> gruppoDi,
            LocalDate inizioAnno, LocalDate fineAnno) {
        Map<String, Attivita> out = new HashMap<>();
        for (String[] v : movimenti) {
            LocalDate giorno = giornoDi(v);
            if (giorno == null || giorno.isAfter(fineAnno)) {
                continue;
            }
            String gruppo = gruppoDi.apply(v);
            if (gruppo == null) {
                continue;
            }
            Attivita a = out.computeIfAbsent(gruppo, k -> new Attivita());
            if (a.primo == null || giorno.isBefore(a.primo)) {
                a.primo = giorno;
            }
            if (a.ultimo == null || giorno.isAfter(a.ultimo)) {
                a.ultimo = giorno;
            }
            if (!giorno.isBefore(inizioAnno)) {
                a.movimentiNellAnno = true;
            }
            // gamba 0: uscita (campi 8-10, indirizzo 26), gamba 1: entrata (campi 11-13, indirizzo 28)
            for (int gamba = 0; gamba < 2; gamba++) {
                int c = gamba == 0 ? 8 : 11;
                String moneta = v[c];
                String tipo = v[c + 1] == null ? "" : v[c + 1];
                BigDecimal qta = numero(v[c + 2]);
                if (moneta == null || moneta.isBlank() || qta == null || "FIAT".equalsIgnoreCase(tipo)) {
                    continue;
                }
                Andamento an = a.monete.computeIfAbsent(moneta + ";" + tipo, k -> {
                    Moneta m = new Moneta();
                    String address = v.length > 28 ? (c == 8 ? v[26] : v[28]) : "";
                    m.InserisciValori(moneta, "1", address == null ? "" : address, tipo);
                    m.Rete = reteDi(v);
                    return new Andamento(m);
                });
                an.variazioni.add(new Object[] {giorno, qta});
            }
        }
        return out;
    }

    // --- media giornaliera dei rapporti 99 --------------------------------

    /**
     * Media giornaliera dei rapporti 99: somma dei valori di ogni giorno del rapporto (quantità a fine giornata per
     * prezzo di fine giornata, più la liquidità se il gruppo non ha il conto corrente) diviso 365, anche nel
     * bisestile. Il valore si chiede solo per i giorni con giacenza, i token SCAM non contano (come al 31/12). Un
     * giorno senza prezzo non vale zero "di nascosto": rende la media incompleta e finisce in
     * {@link Risultato#prezziMancanti}. Si può ripetere sullo stesso risultato (dopo aver scritto dei prezzi).
     */
    public static void calcolaMediaGiornaliera(Risultato ris, ValoreFineGiornata valori) {
        LocalDate inizio = LocalDate.of(ris.anno, 1, 1);
        LocalDate fine = LocalDate.of(ris.anno, 12, 31);
        int giorni = (int) (fine.toEpochDay() - inizio.toEpochDay()) + 1;
        Map<String, PrezzoMancante> mancanti = new TreeMap<>();
        for (Attivita att : ris.attivita.values()) {
            att.valoreGiorno = null;
            att.senzaPrezzo = null;
        }
        for (Rapporto r : ris.rapporti) {
            if (r.contoCorrente()) {
                continue;
            }
            Attivita att = ris.attivita.get(r.gruppo);
            if (att.valoreGiorno == null) {
                valoriGiornalieri(att, r.gruppo, inizio, fine, giorni, valori, mancanti);
            }
            BigDecimal somma = BigDecimal.ZERO;
            int senza = 0;
            for (LocalDate g = r.da; !g.isAfter(r.a); g = g.plusDays(1)) {
                int i = (int) (g.toEpochDay() - inizio.toEpochDay());
                somma = somma.add(att.valoreGiorno[i]);
                if (att.senzaPrezzo[i]) {
                    senza++;
                }
            }
            BigDecimal media = somma.divide(BigDecimal.valueOf(GIORNI_DIVISORE), 8, RoundingMode.HALF_UP);
            if (r.conLiquidita) {
                List<String> avvisi = new ArrayList<>();
                media = media.add(ris.liquidita.mediaAnnua(r.gruppo, r.da, r.a, r.copreFineAnno ? fine : r.a, avvisi));
                r.note.addAll(avvisi);
            }
            r.giacenzaMedia = media;
            r.giorniSenzaPrezzo = senza;
            r.mediaIncompleta = senza > 0;
        }
        ris.prezziMancanti.clear();
        ris.prezziMancanti.addAll(mancanti.values());
        ris.mediaCalcolata = true;
    }

    /**
     * I giorni con giacenza dei rapporti 99, senza prezzi: quelli che {@link #calcolaMediaGiornaliera} valorizzerà.
     * Servono a scaricare i prezzi prima, in un lotto solo.
     */
    public static List<GiornoConGiacenza> giorniConGiacenza(Risultato ris) {
        LocalDate inizio = LocalDate.of(ris.anno, 1, 1);
        LocalDate fine = LocalDate.of(ris.anno, 12, 31);
        Set<String> gruppi = new LinkedHashSet<>();
        for (Rapporto r : ris.rapporti) {
            if (!r.contoCorrente()) {
                gruppi.add(r.gruppo);
            }
        }
        List<GiornoConGiacenza> out = new ArrayList<>();
        for (String gruppo : gruppi) {
            Attivita att = ris.attivita.get(gruppo);
            if (att == null) {
                continue;
            }
            for (Andamento an : att.monete.values()) {
                perOgniGiornoConGiacenza(an, inizio, fine, (g, qta) -> out.add(new GiornoConGiacenza(an.moneta, g, qta, gruppo)));
            }
        }
        return out;
    }

    private static void valoriGiornalieri(Attivita att, String gruppo, LocalDate inizio, LocalDate fine, int giorni,
            ValoreFineGiornata valori, Map<String, PrezzoMancante> mancanti) {
        att.valoreGiorno = new BigDecimal[giorni];
        att.senzaPrezzo = new boolean[giorni];
        java.util.Arrays.fill(att.valoreGiorno, BigDecimal.ZERO);
        for (Andamento an : att.monete.values()) {
            perOgniGiornoConGiacenza(an, inizio, fine, (g, qta) -> {
                int i = (int) (g.toEpochDay() - inizio.toEpochDay());
                BigDecimal v = valori.valore(an.moneta, qta, g);
                if (v == null) {
                    att.senzaPrezzo[i] = true;
                    PrezzoMancante pm = mancanti.computeIfAbsent(PrezzoMancante.chiaveMoneta(an.moneta) + "|" + g,
                            k -> new PrezzoMancante(an.moneta, g, BigDecimal.ZERO));
                    pm.qta = pm.qta.add(qta);
                    pm.gruppi.add(gruppo);
                } else {
                    att.valoreGiorno[i] = att.valoreGiorno[i].add(v);
                }
            });
        }
    }

    /** Chiama {@code azione} per ogni giorno dell'anno in cui la moneta ha quantità positiva a fine giornata. SCAM esclusi. */
    private static void perOgniGiornoConGiacenza(Andamento an, LocalDate inizio, LocalDate fine,
            java.util.function.BiConsumer<LocalDate, BigDecimal> azione) {
        if (Funzioni.isSCAM(an.moneta.Moneta)) {
            return;
        }
        BigDecimal qta = BigDecimal.ZERO;
        TreeMap<LocalDate, BigDecimal> perGiorno = new TreeMap<>();
        for (Object[] var : an.variazioni) {
            LocalDate g = (LocalDate) var[0];
            if (g.isBefore(inizio)) {
                qta = qta.add((BigDecimal) var[1]);
            } else {
                perGiorno.merge(g, (BigDecimal) var[1], BigDecimal::add);
            }
        }
        for (LocalDate g = inizio; !g.isAfter(fine); g = g.plusDays(1)) {
            BigDecimal delta = perGiorno.get(g);
            if (delta != null) {
                qta = qta.add(delta);
            }
            if (qta.signum() > 0) {
                azione.accept(g, qta);
            }
        }
    }

    // --- utilità ------------------------------------------------------------

    /** Importo arrotondato all'euro come lo vuole la DSU (0,50 verso l'alto). */
    public static BigDecimal euro(BigDecimal v) {
        return v == null ? null : v.setScale(0, RoundingMode.HALF_UP);
    }

    private static String euro2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String reteDi(String[] v) {
        try {
            String rete = Funzioni.TrovaReteDaIMovimento(v);
            return rete == null ? "" : rete;
        } catch (RuntimeException e) {
            return ""; // ID non standard: la rete resta vuota
        }
    }

    /** Il giorno del movimento dal campo 1 ({@code yyyy-MM-dd HH:mm}), {@code null} se non leggibile. */
    private static LocalDate giornoDi(String[] v) {
        if (v == null || v.length < 14 || v[1] == null || v[1].length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(v[1].substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static BigDecimal numero(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
