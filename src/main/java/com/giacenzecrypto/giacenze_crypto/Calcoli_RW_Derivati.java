package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * I righi codice 9 del quadro W/RW (contratti derivati esteri), decisioni D7 e P2 di {@code nocommit/Documentazione/Decisioni_Derivati.md}
 * (piano in {@code Analisi_P2_ContoDerivati.md}). Un rigo per gruppo wallet e anno, somma dei singoli valori con i giorni come media
 * ponderata (istruzioni Redditi PF, "prodotti finanziari omogenei"), e un prospetto con una riga per contratto o per conto.
 * <ul>
 *   <li><b>P1</b> (margine cripto-attivita'): una voce per posizione con i token del contratto (OKX). Colonna 7 = somma dei margini
 *       impegnati dalle aperture dell'anno, piu' quello parcheggiato al 1/1 per una posizione aperta da prima: lo zero non si
 *       trasmette. Colonna 8 = guadagno della chiusura se positivo, per una posizione aperta al 31/12 il guadagno non realizzato
 *       se positivo (prezzo a pronti del sottostante, approssimazione dichiarata).
 *       Un gruppo con il conto derivati ma senza posizioni (Bybit, Binance) ha colonna 7 = valore del conto all'ingresso e colonna 8
 *       zero.</li>
 *   <li><b>P2</b> (conto derivati come derivato): una voce per ogni tratto dell'anno in cui il conto ha un saldo (il conto che si
 *       svuota chiude il tratto, quello che torna positivo ne apre un altro). Colonna 7 = valore al 1/1 o all'ingresso del tratto,
 *       colonna 8 = valore al 31/12 o appena prima che il conto si svuoti, piu' per l'ultimo tratto il risultato non realizzato
 *       delle posizioni aperte con il suo segno (mai sotto zero in totale). Il tratto che parte dal 1/1 con il saldo dell'anno prima ha in colonna 7 anche il risultato non realizzato al 31/12
 *       dell'anno prima (continuita': colonna 8 dell'anno = colonna 7 dell'anno dopo). I giorni a conto vuoto non contano. Con {@link #OPZIONE_PERIODO_UNICO} una voce sola dal primo ingresso all'ultimo
 *       giorno con un saldo.</li>
 * </ul>
 * IVAFE = colonna 8 per l'aliquota (0,20%, 0,40% con Stato privilegiato dal 2024) per i giorni / 365. Mai solo monitoraggio (colonna 16):
 * si barra solo se l'intermediario paga l'imposta, e un exchange estero non lo fa. Un valore finale zero si stampa come 1 (il software
 * dell'Agenzia non accetta lo zero, provato sulle cripto), con i giorni e l'IVAFE (zero) come per gli altri righi. Logica senza Swing:
 * prezzi e Stato estero si passano dall'esterno.
 */
public final class Calcoli_RW_Derivati {

    private Calcoli_RW_Derivati() {
    }

    /** Prezzo in euro di un'unita' della moneta all'istante, {@code null} se non si trova. */
    public interface Prezzo {
        BigDecimal euro(String moneta, long istante);
    }

    /**
     * Colonne del prospetto: le prime 18 sono quelle della tabella dei dettagli del quadro RW (16 = ID dei movimenti della voce, che il
     * clic su una riga elenca), la 18 e' l'IVAFE della voce, che la tabella non mostra (il modello tronca le righe al numero di colonne).
     */
    public static final int P_ANNO = 0, P_GRUPPO = 1, P_MONETA = 2, P_QTA_INI = 3, P_DATA_INI = 4, P_VAL_INI = 5, P_GRUPPO_FINE = 6,
            P_MONETA_FINE = 7, P_QTA_FINE = 8, P_DATA_FINE = 9, P_VAL_FIN = 10, P_GIORNI = 11, P_VOCE = 12, P_ID_APERTURA = 13,
            P_ID_CHIUSURA = 14, P_AVVISI = 15, P_ID_MOVIMENTI = 16, P_STATO = 17, P_IVAFE = 18, P_COLONNE = 19;

    /** Prefissi della colonna Causale (12) delle voci del prospetto: il clic sulla riga li riconosce. */
    public static final String VOCE_CONTRATTO = "Contratto ", VOCE_CONTO = "Conto derivati";

    /** @return {@code true} se la causale e' quella di una voce del prospetto dei derivati */
    public static boolean IsVoce(String causale) {
        return causale != null && (causale.startsWith(VOCE_CONTRATTO) || causale.startsWith(VOCE_CONTO));
    }

    /** Il rigo di un gruppo wallet. */
    public static final class Rigo {
        public String gruppo = "";
        public BigDecimal valoreIniziale = BigDecimal.ZERO;
        public BigDecimal valoreFinale = BigDecimal.ZERO;
        public int giorni = 0;
        public BigDecimal ivafe = BigDecimal.ZERO;
        public String stato = "";
        /** Colonna 14: 3 se il gruppo ha redditi da derivati nell'anno, altrimenti 5. */
        public String colonna14 = "5";
        public final List<String> avvisi = new ArrayList<>();
        public final List<String[]> prospetto = new ArrayList<>();

        /** @return {@code true} se qualche avviso del rigo e' un errore da correggere prima della trasmissione */
        public boolean conErrori() {
            for (String a : avvisi) if (a.startsWith("ERRORE")) return true;
            return false;
        }
    }

    /** Una voce del prospetto prima della somma. */
    private static final class Voce {
        String voce = "", moneta = "", dataIni = "", dataFine = "", idApertura = "", idChiusura = "";
        final List<String> idMovimenti = new ArrayList<>();
        BigDecimal valIni = BigDecimal.ZERO, valFin = BigDecimal.ZERO, qtaIni, qtaFine;
        int giorni;
        final List<String> avvisi = new ArrayList<>();
    }

    /**
     * Opzione utente (in {@code personale.mv.db}), conta solo con P2: {@code "SI"} = il conto derivati e' una voce sola dal primo
     * ingresso dell'anno all'ultimo giorno con un saldo (valore iniziale del primo ingresso, finale dell'ultimo svuotamento o del 31/12,
     * giorni a conto vuoto compresi), {@code "NO"} (default) = una voce per ogni tratto con un saldo. Entrambe le letture stanno nelle
     * istruzioni: la prima tratta il conto come un rapporto unico aperto per tutto il periodo, la seconda ogni periodo con un saldo come
     * una detenzione a se'. Fuori dal motore delle plusvalenze, quindi non in {@code OpzioniRicalcolo.Epoca()}.
     */
    public static final String OPZIONE_PERIODO_UNICO = "RW_DerivatiContoPeriodoUnico";

    /** Default di {@link #OPZIONE_PERIODO_UNICO}: una voce per tratto. */
    public static final String PERIODO_UNICO_DEFAULT = "NO";

    /** Voce delle opzioni archiviate con una dichiarazione W/RW. */
    public static final String NOME_OPZIONE_PERIODO = "Conto derivati nel codice 9";

    /** @return {@code true} se il conto derivati va in una voce sola ({@link #OPZIONE_PERIODO_UNICO}) */
    public static boolean PeriodoUnico() {
        return "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_PERIODO_UNICO));
    }

    /** Valore archiviato di {@link #NOME_OPZIONE_PERIODO}. */
    public static String DescrizioneOpzionePeriodo(boolean periodoUnico) {
        return periodoUnico ? "un solo periodo dal primo ingresso all'ultimo giorno con un saldo"
                : "un periodo per ogni tratto con un saldo";
    }

    /** Righi dell'anno con i prezzi di mercato e lo Stato dei periodi FIAT del gruppo. */
    public static Map<String, Rigo> Calcola(String anno) {
        return Calcola(anno, Principale.MappaCryptoWallet, ContoDerivati.Attivo(), PeriodoUnico(), PREZZO_MERCATO,
                Calcoli_RW_Derivati::StatoDelGruppo);
    }

    /** Prezzo di mercato in euro di un'unita', come le giacenze del quadro. */
    public static final Prezzo PREZZO_MERCATO = (moneta, istante) -> {
        try {
            Moneta m = new Moneta();
            m.Moneta = moneta;
            m.Qta = "1";
            m.AssegnaTipoAuto();
            String p = Calcoli_RW_Giacenze.PREZZO_MERCATO.prezzo(m, istante);
            return p == null || !Funzioni.isNumeric(p, false) ? null : new BigDecimal(p);
        } catch (RuntimeException e) {
            return null;
        }
    };

    /** Stato estero del gruppo alla data, dai periodi FIAT (come la liquidita'); vuoto se non indicato. */
    static String StatoDelGruppo(String gruppo, String giorno) {
        String anno = giorno.substring(0, 4);
        for (String[] t : Calcoli_RW_Fiat.intervalliFiat(gruppo, anno)) {
            if (t[Calcoli_RW_Fiat.IV_DATA_INIZIO].compareTo(giorno) <= 0 && t[Calcoli_RW_Fiat.IV_DATA_FINE].compareTo(giorno) >= 0) {
                return t[Calcoli_RW_Fiat.IV_STATO] == null ? "" : t[Calcoli_RW_Fiat.IV_STATO];
            }
        }
        return "";
    }

    /**
     * @param anno anno del quadro
     * @param mappa tutti i movimenti
     * @param p2 opzione P2
     * @param prezzo prezzo in euro di un'unita'
     * @param stato Stato estero di un gruppo a un giorno ({@code yyyy-MM-dd})
     * @return gruppo wallet -> rigo (solo i gruppi con contratti o con il conto derivati nell'anno)
     */
    static Map<String, Rigo> Calcola(String anno, Map<String, String[]> mappa, boolean p2, Prezzo prezzo,
            BiFunction<String, String, String> stato) {
        return Calcola(anno, mappa, p2, false, prezzo, stato);
    }

    /** Come sopra, con {@code periodoUnico} = {@link #OPZIONE_PERIODO_UNICO} (solo con P2). */
    static Map<String, Rigo> Calcola(String anno, Map<String, String[]> mappa, boolean p2, boolean periodoUnico, Prezzo prezzo,
            BiFunction<String, String, String> stato) {
        String inizioAnno = anno + "-01-01 00:00";
        String fineAnno = anno + "-12-31 23:59";
        long istanteInizio = FunzioniDate.ConvertiDatainLongMinuto(inizioAnno);
        long istanteFine = FunzioniDate.ConvertiDatainLongMinuto((Integer.parseInt(anno) + 1) + "-01-01 00:00");

        //Movimenti del conto e righe dei token, per gruppo, fino alla fine dell'anno
        Map<String, List<String[]>> conto = new TreeMap<>();
        Map<String, List<String[]>> token = new TreeMap<>();
        Map<String, Boolean> redditi = new TreeMap<>();
        for (String[] v : mappa.values()) {
            if (v == null || v[1] == null || v[1].compareTo(fineAnno) > 0) continue;
            String gruppo = DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true);
            if (v[1].startsWith(anno)) {
                BigDecimal r = Derivati.Risultato(v);
                if (r != null && r.signum() != 0) redditi.put(gruppo, true);
            }
            if (Derivati.IsTokenContratto(v[8]) || Derivati.IsTokenContratto(v[11])) {
                token.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(v);
                continue;
            }
            if (ContoDerivati.SulContoP2(v, mappa)) conto.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(v);
        }

        Map<String, Rigo> risultato = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        java.util.Set<String> gruppi = new java.util.TreeSet<>(conto.keySet());
        gruppi.addAll(token.keySet());
        for (String gruppo : gruppi) {
            List<Voce> voci = new ArrayList<>();
            List<String[]> righeConto = conto.getOrDefault(gruppo, List.of());
            List<String[]> righeToken = token.getOrDefault(gruppo, List.of());
            if (p2) {
                //Una voce per tratto con un saldo (o una sola con l'opzione del periodo unico): il risultato non realizzato va
                //all'ultima, l'unica che puo' arrivare al 31/12
                List<Tratto> tratti = TrattiConto(righeConto, anno, inizioAnno, istanteInizio, istanteFine);
                //Continuita' fra gli anni: il valore finale dell'anno prima comprende il risultato non realizzato delle posizioni aperte, quindi
                //anche il valore iniziale del tratto che parte dal 1/1 (stessa funzione, stesso istante: i due valori coincidono per costruzione)
                String fineAnnoPrima = (Integer.parseInt(anno) - 1) + "-12-31 23:59";
                for (Tratto t : periodoUnico ? Unisci(tratti) : tratti) {
                    Voce c = VoceConto(t, prezzo);
                    c.voce = VOCE_CONTO;
                    if (t.daAnnoPrima) {
                        BigDecimal pnlIniziale = NonRealizzato(righeToken, fineAnnoPrima, istanteInizio, prezzo, c.avvisi, null);
                        if (pnlIniziale.signum() != 0) {
                            c.valIni = c.valIni.add(pnlIniziale).max(BigDecimal.ZERO);
                            c.avvisi.add("Valore iniziale comprensivo del risultato non realizzato al 31/12 dell'anno prima, come il valore finale di quell'anno");
                        }
                    }
                    voci.add(c);
                }
                if (!voci.isEmpty()) {
                    Voce ultima = voci.get(voci.size() - 1);
                    BigDecimal pnl = NonRealizzato(righeToken, fineAnno, istanteFine, prezzo, ultima.avvisi, null);
                    //Il risultato non realizzato conta con il suo segno (fair value del conto), senza scendere sotto zero
                    if (pnl.signum() != 0) {
                        ultima.valFin = ultima.valFin.add(pnl).max(BigDecimal.ZERO);
                        ultima.avvisi.add("Valore finale comprensivo del risultato non realizzato al 31/12 delle posizioni aperte (stima, vedi la fonte del prezzo)");
                    }
                }
            } else if (!righeToken.isEmpty()) {
                voci.addAll(VociContratti(righeToken, righeConto, mappa, anno, inizioAnno, fineAnno, istanteInizio, istanteFine, prezzo));
            } else {
                for (Tratto t : Unisci(TrattiConto(righeConto, anno, inizioAnno, istanteInizio, istanteFine))) {
                    Voce c = VoceConto(t, prezzo);
                    c.voce = VOCE_CONTO + " (posizioni non disponibili, valore finale zero)";
                    c.valFin = BigDecimal.ZERO;
                    voci.add(c);
                }
            }
            if (voci.isEmpty()) continue;
            risultato.put(gruppo, Somma(gruppo, anno, voci, redditi.getOrDefault(gruppo, false), stato));
        }
        return risultato;
    }

    // =================================================================================================
    // CONTO
    // =================================================================================================

    /** Un tratto del conto derivati con un saldo: dal primo ingresso (o dal 1/1) al giorno in cui si svuota (o al 31/12). */
    private static final class Tratto {
        String dataIni, dataFine;
        long istIni, istFine;
        Map<String, BigDecimal> saldoIni, saldoFine;
        final List<String> idMovimenti = new ArrayList<>();
        /** Il tratto parte dal 1/1 con il saldo che il conto aveva alla fine dell'anno prima (non nasce da un ingresso dell'anno). */
        boolean daAnnoPrima;
    }

    /**
     * I tratti dell'anno in cui il conto derivati del gruppo ha un saldo, in ordine. Il conto che si svuota chiude il tratto (valore
     * appena prima), il conto che torna positivo ne apre un altro: i giorni a conto vuoto non sono giorni di detenzione e il valore di
     * ogni tratto resta nel rigo. I movimenti dell'anno fuori da ogni tratto (conto a zero o negativo) vanno al tratto seguente, quelli
     * dopo l'ultimo all'ultimo. Lista vuota se nell'anno il conto non ha mai avuto un saldo.
     */
    private static List<Tratto> TrattiConto(List<String[]> righe, String anno, String inizioAnno, long istanteInizio, long istanteFine) {
        List<Tratto> tratti = new ArrayList<>();
        if (righe.isEmpty()) return tratti;
        List<String[]> ordinate = new ArrayList<>(righe);
        ordinate.sort((a, b) -> a[0].compareTo(b[0]));
        Map<String, BigDecimal> saldo = new TreeMap<>();
        Tratto corrente = null;
        boolean primoDellAnno = true;
        List<String> sospesi = new ArrayList<>();
        for (String[] v : ordinate) {
            boolean nellAnno = v[1].compareTo(inizioAnno) >= 0;
            if (nellAnno && primoDellAnno) {
                primoDellAnno = false;
                if (Positivo(saldo)) {
                    corrente = NuovoTratto(anno + "-01-01", istanteInizio, saldo);
                    corrente.daAnnoPrima = true;
                }
            }
            Map<String, BigDecimal> prima = new TreeMap<>(saldo);
            Somma(saldo, v[8], v[10]);
            Somma(saldo, v[11], v[13]);
            if (!nellAnno) continue;
            if (corrente == null && Positivo(saldo)) {
                corrente = NuovoTratto(v[1].substring(0, 10), FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14)), saldo);
            }
            if (corrente == null) {
                sospesi.add(v[0]);
                continue;
            }
            corrente.idMovimenti.addAll(sospesi);
            sospesi.clear();
            corrente.idMovimenti.add(v[0]);
            if (Positivo(prima) && !Positivo(saldo)) {
                corrente.dataFine = v[1].substring(0, 10);
                corrente.istFine = FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
                corrente.saldoFine = prima;
                tratti.add(corrente);
                corrente = null;
            }
        }
        if (primoDellAnno && Positivo(saldo)) {
            //saldo dagli anni prima e nessun movimento nell'anno
            corrente = NuovoTratto(anno + "-01-01", istanteInizio, saldo);
            corrente.daAnnoPrima = true;
        }
        if (corrente != null) {
            corrente.dataFine = anno + "-12-31";
            corrente.istFine = istanteFine;
            corrente.saldoFine = saldo;
            tratti.add(corrente);
        }
        if (!tratti.isEmpty()) tratti.get(tratti.size() - 1).idMovimenti.addAll(sospesi);
        return tratti;
    }

    private static Tratto NuovoTratto(String data, long istante, Map<String, BigDecimal> saldo) {
        Tratto t = new Tratto();
        t.dataIni = data;
        t.istIni = istante;
        t.saldoIni = new TreeMap<>(saldo);
        return t;
    }

    /**
     * Un solo tratto dal primo ingresso all'ultimo giorno con un saldo, con i valori del primo e dell'ultimo, giorni a conto vuoto
     * compresi: il rigo di solo monitoraggio di P1 (gruppo senza posizioni) e, con {@link #OPZIONE_PERIODO_UNICO}, quello di P2.
     */
    private static List<Tratto> Unisci(List<Tratto> tratti) {
        if (tratti.size() <= 1) return tratti;
        Tratto u = new Tratto();
        Tratto primo = tratti.get(0), ultimo = tratti.get(tratti.size() - 1);
        u.dataIni = primo.dataIni;
        u.istIni = primo.istIni;
        u.saldoIni = primo.saldoIni;
        u.daAnnoPrima = primo.daAnnoPrima;
        u.dataFine = ultimo.dataFine;
        u.istFine = ultimo.istFine;
        u.saldoFine = ultimo.saldoFine;
        for (Tratto t : tratti) u.idMovimenti.addAll(t.idMovimenti);
        return List.of(u);
    }

    /** La voce del prospetto di un tratto, con i valori di mercato al primo e all'ultimo istante. */
    private static Voce VoceConto(Tratto t, Prezzo prezzo) {
        Voce c = new Voce();
        c.idMovimenti.addAll(t.idMovimenti);
        c.dataIni = t.dataIni;
        c.dataFine = t.dataFine;
        Map<String, BigDecimal> positivi = SoloPositivi(t.saldoFine);
        c.moneta = String.join(", ", positivi.isEmpty() ? SoloPositivi(t.saldoIni).keySet() : positivi.keySet());
        c.valIni = Valore(t.saldoIni, t.istIni, prezzo, c.avvisi);
        c.valFin = Valore(t.saldoFine, t.istFine, prezzo, c.avvisi);
        c.giorni = Giorni(t.dataIni, t.dataFine);
        return c;
    }

    private static void Somma(Map<String, BigDecimal> saldo, String moneta, String qta) {
        if (moneta == null || moneta.isBlank() || qta == null || !Funzioni.isNumeric(qta, false)) return;
        saldo.merge(moneta, new BigDecimal(qta), BigDecimal::add);
    }

    private static boolean Positivo(Map<String, BigDecimal> saldo) {
        for (BigDecimal q : saldo.values()) if (q.signum() > 0) return true;
        return false;
    }

    private static Map<String, BigDecimal> SoloPositivi(Map<String, BigDecimal> saldo) {
        Map<String, BigDecimal> m = new TreeMap<>();
        for (Map.Entry<String, BigDecimal> e : saldo.entrySet()) if (e.getValue().signum() > 0) m.put(e.getKey(), e.getValue());
        return m;
    }

    private static BigDecimal Valore(Map<String, BigDecimal> saldo, long istante, Prezzo prezzo, List<String> avvisi) {
        BigDecimal t = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : saldo.entrySet()) {
            if (e.getValue().signum() <= 0) continue;
            BigDecimal p = prezzo.euro(e.getKey(), istante);
            if (p == null) {
                avvisi.add("ERRORE: prezzo mancante di " + e.getKey() + " al " + FunzioniDate.ConvertiDatadaLongAlSecondo(istante).substring(0, 10));
                continue;
            }
            t = t.add(e.getValue().multiply(p));
        }
        return t;
    }

    // =================================================================================================
    // CONTRATTI (P1)
    // =================================================================================================

    /** Una voce per episodio (da zero contratti a zero) che tocca l'anno. */
    private static List<Voce> VociContratti(List<String[]> righeToken, List<String[]> righeConto, Map<String, String[]> mappa, String anno,
            String inizioAnno, String fineAnno, long istanteInizio, long istanteFine, Prezzo prezzo) {
        List<Voce> voci = new ArrayList<>();
        Map<String, DerivatiLifo.Posizione> posizioni = DerivatiLifo.Calcola(righeToken);
        for (DerivatiLifo.Posizione p : posizioni.values()) {
            for (Map.Entry<Integer, List<String>> e : p.righeEpisodio.entrySet()) {
                List<String[]> righe = new ArrayList<>();
                for (String id : e.getValue()) {
                    String[] v = mappa.get(id);
                    if (v != null) righe.add(v);
                }
                if (righe.isEmpty()) continue;
                String primo = righe.get(0)[1], ultimo = righe.get(righe.size() - 1)[1];
                BigDecimal aperti = BigDecimal.ZERO;
                for (String[] v : righe) aperti = aperti.add(Contratti(v));
                boolean chiusa = aperti.signum() == 0;
                if (ultimo.compareTo(inizioAnno) < 0 && chiusa) continue;          //finita prima dell'anno
                if (primo.compareTo(fineAnno) > 0) continue;
                Voce c = new Voce();
                c.voce = VOCE_CONTRATTO + p.contratto + (p.lungo ? " (long)" : " (short)");
                for (String[] v : righe) if (v[1].compareTo(inizioAnno) >= 0 && v[1].compareTo(fineAnno) <= 0) c.idMovimenti.add(v[0]);
                c.moneta = p.moneta;
                c.idApertura = righe.get(0)[0];
                String chiave = p.token.toUpperCase();
                //Colonna 7: margine parcheggiato al 1/1 (posizione aperta da prima) piu' la somma dei margini di tutte le aperture
                //dell'anno, non solo della prima: una posizione costruita in piu' volte impegna il margine di ogni apertura
                java.util.Set<String> apertureAnno = new java.util.HashSet<>();
                for (String[] v : righe) {
                    if (!v[13].isBlank() && v[1].compareTo(inizioAnno) >= 0 && v[1].compareTo(fineAnno) <= 0) apertureAnno.add(v[24]);
                }
                //Il margine si legge dal campo dati: il giroconto dell'apertura porta anche la commissione (forma "conto" di OKXDerivati),
                //il suo valore unitario e' quello del giroconto
                for (String[] m : righeConto) {
                    if (Derivati.Tipo(m).equals(Derivati.MARGINE) && OKXDerivati.SOTTOWALLET.equals(m[4]) && apertureAnno.contains(m[24])
                            && Funzioni.isNumeric(m[13], false) && Funzioni.isNumeric(m[15], false)
                            && Funzioni.isNumeric(Derivati.Dati(m).get("margine"), false) && new BigDecimal(m[13]).signum() != 0) {
                        BigDecimal unitario = new BigDecimal(m[15]).divide(new BigDecimal(m[13]).abs(), 18, java.math.RoundingMode.HALF_UP);
                        c.valIni = c.valIni.add(unitario.multiply(new BigDecimal(Derivati.Dati(m).get("margine")).abs())
                                .setScale(2, java.math.RoundingMode.HALF_UP));
                    }
                }
                if (primo.compareTo(inizioAnno) >= 0) {
                    c.dataIni = primo.substring(0, 10);
                } else {
                    c.dataIni = anno + "-01-01";
                    List<String[]> primaDellAnno = new ArrayList<>(righe);
                    primaDellAnno.addAll(righeConto);
                    BigDecimal parcheggiato = OKXDerivati.MarginiParcheggiati(primaDellAnno, istanteInizio)
                            .getOrDefault(chiave.toUpperCase(), BigDecimal.ZERO);
                    BigDecimal pr = prezzo.euro(p.moneta, istanteInizio);
                    if (pr == null) c.avvisi.add("ERRORE: prezzo mancante di " + p.moneta + " al " + anno + "-01-01");
                    else c.valIni = c.valIni.add(parcheggiato.max(BigDecimal.ZERO).multiply(pr));
                }
                //Colonna 8: guadagno della chiusura se positivo, o guadagno non realizzato al 31/12 se positivo
                boolean chiusaNellAnno = chiusa && ultimo.compareTo(fineAnno) <= 0;
                if (chiusaNellAnno) {
                    c.dataFine = ultimo.substring(0, 10);
                    c.idChiusura = righe.get(righe.size() - 1)[0];
                    BigDecimal realizzato = Realizzato(righe, mappa, inizioAnno);
                    c.valFin = realizzato.max(BigDecimal.ZERO);
                } else {
                    c.dataFine = anno + "-12-31";
                    BigDecimal pnl = NonRealizzato(righe, fineAnno, istanteFine, prezzo, c.avvisi, p);
                    c.valFin = pnl.max(BigDecimal.ZERO);
                    c.avvisi.add("Guadagno non realizzato al 31/12 stimato (vedi la fonte del prezzo)");
                }
                if (c.valIni.signum() == 0) {
                    //I file di Bybit (DerivatiDaCsv) non dicono il margine di ogni posizione: colonna 7 zero, stampata come 1 (decisione
                    //dell'utente del 22/11/2026), senza errore. Per OKX il margine c'e' sempre, mancarlo e' un errore
                    if (DerivatiLifo.RichiedeRigaProfitto(righe.get(0))) {
                        c.avvisi.add("Margine della posizione non disponibile nel file dell'exchange: valore iniziale zero, riportato come 1");
                    } else {
                        c.avvisi.add("ERRORE: margine impegnato non trovato, il valore iniziale zero non si trasmette");
                    }
                }
                c.giorni = Giorni(c.dataIni, c.dataFine);
                voci.add(c);
            }
        }
        return voci;
    }

    /** Contratti con segno di una riga di token (+ apertura, - chiusura). */
    private static BigDecimal Contratti(String[] v) {
        String q = !v[13].isBlank() ? v[13] : v[10];
        return q == null || !Funzioni.isNumeric(q, false) ? BigDecimal.ZERO : new BigDecimal(q);
    }

    /** Risultato in euro delle chiusure dell'episodio nell'anno: righe di token e righe di pnl classificate degli stessi fill. */
    private static BigDecimal Realizzato(List<String[]> righe, Map<String, String[]> mappa, String inizioAnno) {
        java.util.Set<String> fill = new java.util.HashSet<>();
        for (String[] v : righe) if (!v[10].isBlank() && v[1].compareTo(inizioAnno) >= 0) fill.add(DerivatiLifo.ChiaveFill(v));
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : mappa.values()) {
            String f = DerivatiLifo.ChiaveFill(v);
            if (f == null || !fill.contains(f)) continue;
            if (!Derivati.Tipo(v).equals(Derivati.CONTRATTO) && !Derivati.Tipo(v).equals(Derivati.PNL)) continue;
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    /**
     * Risultato non realizzato in euro (guadagno o perdita) delle posizioni aperte alla fine dell'anno, con il prezzo a pronti del sottostante. Con
     * {@code sola} solo quella posizione, altrimenti tutte quelle delle righe.
     */
    private static BigDecimal NonRealizzato(List<String[]> righeToken, String fineAnno, long istanteFine, Prezzo prezzo,
            List<String> avvisi, DerivatiLifo.Posizione sola) {
        BigDecimal t = BigDecimal.ZERO;
        if (righeToken.isEmpty()) return t;
        List<String[]> fino = new ArrayList<>();
        for (String[] v : righeToken) if (v[1].compareTo(fineAnno) <= 0) fino.add(v);
        for (DerivatiLifo.Posizione p : DerivatiLifo.Calcola(fino).values()) {
            if (sola != null && !p.token.equalsIgnoreCase(sola.token)) continue;
            if (p.contrattiAperti().signum() <= 0) continue;
            BigDecimal pMoneta = prezzo.euro(p.moneta, istanteFine);
            //Il prezzo del contratto: prima il mark price dell'exchange (il prezzo con cui calcola il suo non realizzato, per quel contratto),
            //poi, se non si legge o l'exchange non e' supportato (Binance, future OKX scaduti), lo spot del sottostante
            String[] origine = OrigineContratto(fino, p);
            BigDecimal mark = origine == null ? null : MarkPriceDerivati.Mark(origine[0], origine[1], p.inverso, istanteFine);
            String giorno = fineAnno.substring(0, 10);
            if (mark != null) {
                avvisi.add("Non realizzato di " + p.contratto + " al " + giorno + " valutato con il mark price di " + origine[0]);
            } else {
                String[] parti = p.contratto.split("-");
                String base = parti.length > 0 ? parti[0] : "";
                String quotazione = parti.length > 1 ? parti[1].split("_")[0] : "";
                BigDecimal pBase = prezzo.euro(base, istanteFine);
                BigDecimal pQuot = prezzo.euro(quotazione, istanteFine);
                if (pBase == null || pQuot == null || pQuot.signum() == 0) pMoneta = null;
                else mark = pBase.divide(pQuot, MathContext.DECIMAL64);
                avvisi.add("Non realizzato di " + p.contratto + " al " + giorno + " stimato con il prezzo a pronti del sottostante"
                        + (origine != null ? " (mark price non disponibile)" : ""));
            }
            if (pMoneta == null || mark == null) {
                avvisi.add("ERRORE: prezzo al 31/12 mancante per il contratto " + p.contratto);
                continue;
            }
            for (DerivatiLifo.Lotto l : p.aperti) {
                BigDecimal ctVal = ValoreContratto(l.idApertura, fino);
                BigDecimal diff = DerivatiLifo.Differenziale(p, l.contratti, l.prezzo, mark, ctVal);
                if (diff == null) {
                    avvisi.add("ERRORE: prezzo o valore del contratto mancante per " + p.contratto);
                    continue;
                }
                t = t.add(diff.multiply(pMoneta));
            }
        }
        return t;
    }

    /**
     * Piattaforma ({@code OKX} o {@code BYBIT}) e simbolo nativo del contratto di una posizione, dal campo dati delle sue righe di token
     * ({@code contratto} per OKX, {@code simbolo} per i file CSV); {@code null} se l'exchange non ha il mark price o manca il simbolo.
     */
    private static String[] OrigineContratto(List<String[]> righe, DerivatiLifo.Posizione p) {
        for (String[] v : righe) {
            if (!p.token.equalsIgnoreCase(v[8]) && !p.token.equalsIgnoreCase(v[11])) continue;
            Map<String, String> d = Derivati.Dati(v);
            String piattaforma = MarkPriceDerivati.Normalizza(d.get("piattaforma"));
            if (piattaforma == null) return null;
            String simbolo = piattaforma.equals("OKX") ? d.get("contratto") : d.get("simbolo");
            return simbolo == null || simbolo.isBlank() ? null : new String[]{piattaforma, simbolo.trim()};
        }
        return null;
    }

    private static BigDecimal ValoreContratto(String idApertura, List<String[]> righe) {
        for (String[] v : righe) {
            if (!v[0].equals(idApertura)) continue;
            String s = Derivati.Dati(v).get("valoreContratto");
            return s == null || !Funzioni.isNumeric(s, false) ? null : new BigDecimal(s);
        }
        return null;
    }

    // =================================================================================================
    // SOMMA
    // =================================================================================================

    private static Rigo Somma(String gruppo, String anno, List<Voce> voci, boolean redditi, BiFunction<String, String, String> stato) {
        Rigo r = new Rigo();
        r.gruppo = gruppo;
        r.colonna14 = redditi ? "3" : "5";
        String ultimoGiorno = anno + "-01-01";
        for (Voce v : voci) if (v.dataFine != null && v.dataFine.compareTo(ultimoGiorno) > 0) ultimoGiorno = v.dataFine;
        r.stato = stato == null ? "" : stato.apply(gruppo, ultimoGiorno);
        BigDecimal aliquota = Calcoli_RW_Fiat.aliquotaIvafe(Integer.parseInt(anno), r.stato);
        BigDecimal pesoFin = BigDecimal.ZERO, pesoIni = BigDecimal.ZERO, giorniFin = BigDecimal.ZERO, giorniIni = BigDecimal.ZERO;
        int giorniMax = 0;
        for (Voce v : voci) {
            r.valoreIniziale = r.valoreIniziale.add(v.valIni);
            r.valoreFinale = r.valoreFinale.add(v.valFin);
            BigDecimal ivafe = v.valFin.multiply(aliquota).multiply(BigDecimal.valueOf(v.giorni))
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            r.ivafe = r.ivafe.add(ivafe);
            pesoFin = pesoFin.add(v.valFin);
            giorniFin = giorniFin.add(v.valFin.multiply(BigDecimal.valueOf(v.giorni)));
            pesoIni = pesoIni.add(v.valIni);
            giorniIni = giorniIni.add(v.valIni.multiply(BigDecimal.valueOf(v.giorni)));
            giorniMax = Math.max(giorniMax, v.giorni);
            for (String a : v.avvisi) if (!r.avvisi.contains(a)) r.avvisi.add(a);
            String[] p = new String[P_COLONNE];
            java.util.Arrays.fill(p, "");
            p[P_ANNO] = anno;
            p[P_GRUPPO] = gruppo;
            p[P_MONETA] = v.moneta;
            p[P_DATA_INI] = v.dataIni;
            p[P_VAL_INI] = Due(v.valIni);
            p[P_GRUPPO_FINE] = gruppo;
            p[P_MONETA_FINE] = v.moneta;
            p[P_DATA_FINE] = v.dataFine == null ? "" : v.dataFine;
            p[P_VAL_FIN] = Due(v.valFin);
            p[P_GIORNI] = String.valueOf(v.giorni);
            p[P_VOCE] = v.voce;
            p[P_ID_APERTURA] = v.idApertura;
            p[P_ID_CHIUSURA] = v.idChiusura;
            p[P_AVVISI] = String.join(" - ", v.avvisi);
            p[P_ID_MOVIMENTI] = String.join(",", v.idMovimenti);
            p[P_IVAFE] = Due(ivafe);
            p[P_STATO] = r.stato;
            r.prospetto.add(p);
        }
        //Giorni: media ponderata sui valori finali (sui valori iniziali se non ce ne sono), come per i prodotti omogenei
        if (pesoFin.signum() > 0) r.giorni = giorniFin.divide(pesoFin, 0, RoundingMode.HALF_UP).intValue();
        else if (pesoIni.signum() > 0) r.giorni = giorniIni.divide(pesoIni, 0, RoundingMode.HALF_UP).intValue();
        else r.giorni = giorniMax;
        r.giorni = Math.max(1, Math.min(r.giorni, 365));
        r.ivafe = r.ivafe.setScale(2, RoundingMode.HALF_UP);
        if (r.stato.isBlank()) r.avvisi.add("Stato estero non impostato nei periodi di detenzione del gruppo");
        return r;
    }

    private static int Giorni(String da, String a) {
        if (da == null || a == null || da.isBlank() || a.isBlank()) return 0;
        return FunzioniDate.DifferenzaDate(da, a) + 1;
    }

    private static String Due(BigDecimal b) {
        return b.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Valore da stampare in una colonna del rigo: all'euro, e 1 al posto dello zero (il software dell'Agenzia non lo accetta). */
    public static String ValoreStampa(BigDecimal valore) {
        String v = valore.setScale(0, RoundingMode.HALF_UP).toPlainString();
        return v.equals("0") || v.equals("-0") ? "1" : v;
    }

    private static final String NOTA_ZERO = " Un valore iniziale o finale zero è riportato come 1, perché il software dell'Agenzia delle "
            + "entrate non accetta lo zero.";

    /** Testo della nota del quadro W/RW quando ci sono righi codice 9. Dice l'effetto, mai come trattare il caso. */
    public static String TestoNota(boolean p2, boolean periodoUnico) {
        if (p2 && periodoUnico) {
            return "Il rigo con codice 9 di ogni exchange riporta il conto derivati come un solo periodo "
                    + "dal primo ingresso dell'anno all'ultimo giorno con un saldo: colonna 7 il valore al 1° gennaio o al primo ingresso, "
                    + "colonna 8 il valore alla fine dell'anno o appena prima dell'ultimo svuotamento, più il risultato non realizzato "
                    + "delle posizioni aperte (anche negativo). Un periodo che parte dal 1° gennaio con posizioni aperte ha in colonna 7 anche il risultato non realizzato al 31 dicembre dell'anno prima, come nel valore finale di quell'anno. I giorni comprendono quelli a conto vuoto. Il conto non compare fra i righi delle cripto-attività." + NOTA_ZERO;
        }
        return p2
                ? "Il rigo con codice 9 di ogni exchange riporta il conto derivati, sommando i periodi "
                + "dell'anno in cui il conto ha un saldo: colonna 7 i valori al 1° gennaio o all'ingresso di ciascun periodo, colonna 8 i "
                + "valori alla fine dell'anno o appena prima che il conto si svuoti, più il risultato non realizzato delle posizioni aperte (anche negativo), giorni "
                + "come media ponderata sui valori finali. Un periodo che parte dal 1° gennaio con posizioni aperte ha in colonna 7 anche il risultato non realizzato al 31 dicembre dell'anno prima, come nel valore finale di quell'anno. I giorni a conto vuoto non contano. Il conto non compare fra i righi delle "
                + "cripto-attività." + NOTA_ZERO
                : "Il rigo con codice 9 di ogni exchange somma i contratti derivati dell'anno: colonna 7 il margine impegnato dalle aperture "
                + "dell'anno più quello ancora vincolato al 1° gennaio (lo zero non si trasmette), colonna 8 il guadagno della chiusura o il guadagno non realizzato al 31 "
                + "dicembre, se positivi, altrimenti zero. Il margine resta fra le cripto-attività. Per gli exchange di cui il programma non "
                + "conosce le posizioni (Bybit, Binance) la colonna 7 è il valore del conto derivati all'ingresso e la colonna 8 è zero."
                + NOTA_ZERO;
    }
}
