package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Integra le due fonti di Binance (file CSV e API) senza doppioni: dato un elenco di movimenti in arrivo e l'archivio,
 * toglie dall'elenco ciò che l'archivio contiene già, anche se l'altra fonte lo descrive in modo diverso.
 *
 * <p>La chiave di {@link Importazioni#F_buildKeyMovimento} (istante al secondo, exchange, monete, quantità) riconosce
 * solo il movimento identico. Fra CSV e API lo stesso fatto cambia forma (verificato il 2026-11-14: senza
 * riconciliazione quasi tutte le righe di una fonte sarebbero entrate come doppioni dell'altra):</p>
 * <ul>
 * <li><b>Convert e scambi</b>: orario sfasato di 1 s;</li>
 * <li><b>premi Earn</b> (Locked, Flexible, Real-time): il CSV ha una riga al giorno per moneta, l'API più righe con gli
 * orari veri, e il giorno può slittare di uno (il premio Real-time dell'API ha l'ID alle 23:59:59 UTC);</li>
 * <li><b>depositi e prelievi FIAT</b>: l'API dà il lordo e la commissione a parte, il CSV il netto, e l'orario può
 * differire di qualche giorno (accredito contro richiesta);</li>
 * <li><b>prelievi cripto</b>: la commissione è una riga a parte nell'API ed è compresa nella quantità del CSV;</li>
 * <li><b>acquisto con saldo</b>: una riga con due gambe nell'API, due righe (prelievo FIAT, deposito cripto) nel CSV.</li>
 * </ul>
 *
 * <p><b>Regole.</b> Un movimento in arrivo è già presente se ogni sua <i>gamba</i> (moneta e quantità con segno)
 * trova una gamba libera dell'archivio, della stessa famiglia, entro la tolleranza di orario della famiglia
 * ({@link #TOLLERANZA_DEFAULT_S}, {@link #TOLLERANZA_PREMIO_S}, {@link #TOLLERANZA_FIAT_S}). Ogni gamba dell'archivio
 * si usa <b>una volta sola</b> (due ordini identici nello stesso secondo restano due, se l'archivio ne ha due). Una riga con
 * commissioni collegate ({@code [43]}) offre anche la quantità al lordo e al netto di quelle commissioni. Una
 * commissione segue il movimento a cui è legata: se quello è già presente lo è anche lei, altrimenti si guarda la sua
 * gamba. I premi Earn con causale riconosciuta si confrontano per somma di (famiglia, moneta, giorno), con giorno
 * dell'archivio uguale o adiacente.</p>
 *
 * <p><b>Mai si modifica o si sostituisce un movimento dell'archivio</b>: la riga in arrivo viene solo scartata
 * (scelta dell'utente, 2026-11-14). Valgono solo i movimenti con exchange «Binance».</p>
 */
final class RiconciliazioneBinance {

    /** Convert, scambi, trade, commissioni, prelievi cripto: arrotondamenti di orario fra le due fonti. */
    static final long TOLLERANZA_DEFAULT_S = 5;
    /** Premi isolati (airdrop, extra reward di staking): CSV e API ne datano l'accredito in momenti diversi. */
    static final long TOLLERANZA_PREMIO_S = 86400;
    /** Depositi e prelievi FIAT: orario dell'accredito contro orario della richiesta (visto fino a 3 giorni). */
    static final long TOLLERANZA_FIAT_S = 4 * 86400L;
    /** Depositi e prelievi cripto: l'API data la richiesta, il CSV il completamento (visti 11 minuti di differenza). */
    static final long TOLLERANZA_TRASFERIMENTO_S = 86400;
    /** Somma di un giorno di premi: lo stesso importo con arrotondamenti all'ottavo decimale. */
    private static final BigDecimal TOLLERANZA_SOMMA = new BigDecimal("0.00000002");

    private static final String EXCHANGE = "Binance";
    private static final Set<String> TIPI_PREMIO = Set.of("EARN", "REWARD", "STAKING REWARD", "AIRDROP", "CASHBACK");
    private static final Set<String> TIPI_FIAT = Set.of("DEPOSITO FIAT", "PRELIEVO FIAT");
    private static final Set<String> TIPI_TRASFERIMENTO = Set.of("DEPOSITO CRYPTO", "PRELIEVO CRYPTO");

    private RiconciliazioneBinance() {
    }

    /** Esito: le righe da importare, quelle già presenti e quante per motivo (per il registro). */
    static final class Risultato {
        final List<String[]> nuove = new ArrayList<>();
        final List<String[]> giaPresenti = new ArrayList<>();
        final Map<String, Integer> perMotivo = new TreeMap<>();

        private void scarta(String[] riga, String motivo) {
            giaPresenti.add(riga);
            perMotivo.merge(motivo, 1, Integer::sum);
        }
    }

    private enum Classe { LOCKED, FLEXIBLE, REALTIME }

    private enum Pool { ALTRO, PREMIO, FEE }

    /** Una gamba di un movimento dell'archivio, usabile una volta sola. */
    private static final class Gamba {
        final String moneta;
        final Set<String> importi = new HashSet<>();
        final long istante;
        final long tolleranza;
        boolean usata;

        Gamba(String moneta, long istante, long tolleranza) {
            this.moneta = moneta;
            this.istante = istante;
            this.tolleranza = tolleranza;
        }
    }

    /** Somma di un giorno di premi di una famiglia e di una moneta. */
    private static final class GruppoGiorno {
        BigDecimal somma = BigDecimal.ZERO;
        boolean usato;
    }

    private static final class Indice {
        final Map<String, List<Gamba>> perChiave = new HashMap<>();

        void aggiungi(Gamba g) {
            for (String importo : g.importi) {
                perChiave.computeIfAbsent(g.moneta + "|" + importo, k -> new ArrayList<>()).add(g);
            }
        }

        /** La gamba libera più vicina nel tempo che ha uno degli importi, nella tolleranza propria o di quella cercata. */
        Gamba trova(String moneta, Set<String> importi, long istante, long tolleranza) {
            Gamba migliore = null;
            long distanzaMigliore = Long.MAX_VALUE;
            for (String importo : importi) {
                List<Gamba> lista = perChiave.get(moneta + "|" + importo);
                if (lista == null) continue;
                for (Gamba g : lista) {
                    if (g.usata) continue;
                    long distanza = Math.abs(g.istante - istante);
                    if (distanza <= Math.max(tolleranza, g.tolleranza) && distanza < distanzaMigliore) {
                        migliore = g;
                        distanzaMigliore = distanza;
                    }
                }
            }
            return migliore;
        }
    }

    /** Gamba di una riga: moneta e quantità con segno (uscita negativa, entrata positiva). */
    private record Leg(String moneta, BigDecimal importo) {
    }

    /**
     * Toglie da {@code nuove} i movimenti Binance che l'archivio contiene già in un'altra forma.
     *
     * @param nuove movimenti in arrivo, già consolidati; non viene modificata
     * @param archivio movimenti già presenti (tutti gli exchange: si guardano solo quelli di Binance)
     * @return le righe da importare (nell'ordine di arrivo) e quelle scartate
     */
    static Risultato Riconcilia(List<String[]> nuove, Collection<String[]> archivio) {
        Risultato r = new Risultato();

        // ---- Archivio: gambe libere per famiglia e somme giornaliere dei premi ----
        Map<Pool, Indice> pool = new HashMap<>();
        for (Pool p : Pool.values()) pool.put(p, new Indice());
        Map<String, TreeMap<LocalDate, GruppoGiorno>> giorniArchivio = new HashMap<>();
        Map<String, List<String[]>> commissioniArchivio = commissioniPerChiave(archivio);
        boolean almenoUnaBinance = false;
        List<String[]> righeArchivio = new ArrayList<>();
        for (String[] v : archivio) {
            if (!eBinance(v)) continue;
            almenoUnaBinance = true;
            Classe classe = classe(v);
            if (classe != null) {
                aggiungiAlGiorno(giorniArchivio, classe, v);
                continue;
            }
            righeArchivio.add(v);
        }
        for (List<String[]> unita : unita(righeArchivio)) {
            Pool p = poolDi(unita.get(0));
            for (Gamba g : gambeUnita(unita, commissioniArchivio)) pool.get(p).aggiungi(g);
        }

        // ---- Niente da riconciliare se l'archivio non ha nulla di Binance ----
        if (!almenoUnaBinance) {
            r.nuove.addAll(nuove);
            return r;
        }

        // ---- In arrivo ----
        Map<String, List<String[]>> commissioniArrivo = commissioniPerChiave(nuove);
        Map<String[], String> motivo = new java.util.IdentityHashMap<>(); // riga scartata -> motivo

        // 1) Premi con famiglia: somme per (famiglia, moneta, giorno)
        Map<String, TreeMap<LocalDate, List<String[]>>> giorniArrivo = new HashMap<>();
        for (String[] v : nuove) {
            if (!eBinance(v)) continue;
            Classe classe = classe(v);
            LocalDate giorno = giornoDi(v);
            if (classe == null || giorno == null) continue;
            giorniArrivo.computeIfAbsent(classe + "|" + monetaEntrata(v), k -> new TreeMap<>())
                    .computeIfAbsent(giorno, k -> new ArrayList<>()).add(v);
        }
        for (Map.Entry<String, TreeMap<LocalDate, List<String[]>>> e : giorniArrivo.entrySet()) {
            TreeMap<LocalDate, GruppoGiorno> esistenti = giorniArchivio.get(e.getKey());
            if (esistenti == null) continue;
            for (Map.Entry<LocalDate, List<String[]>> g : e.getValue().entrySet()) {
                BigDecimal somma = BigDecimal.ZERO;
                for (String[] v : g.getValue()) somma = somma.add(quantitaEntrata(v));
                GruppoGiorno trovato = null;
                for (int scarto : new int[]{0, -1, 1}) {
                    GruppoGiorno cand = esistenti.get(g.getKey().plusDays(scarto));
                    if (cand != null && !cand.usato && somma.signum() > 0
                            && cand.somma.subtract(somma).abs().compareTo(TOLLERANZA_SOMMA) <= 0) {
                        trovato = cand;
                        break;
                    }
                }
                if (trovato != null) {
                    trovato.usato = true;
                    for (String[] v : g.getValue()) motivo.put(v, "premio Earn già presente (somma del giorno)");
                }
            }
        }

        // 2) Tutto ciò che non è commissione e non è un premio con famiglia: gamba per gamba. Le esecuzioni dello stesso
        //    ordine nello stesso secondo (l'API le dà una per una, il CSV le somma) si confrontano per somma
        List<String[]> daVerificare = new ArrayList<>();
        for (String[] v : nuove) {
            if (eBinance(v) && !motivo.containsKey(v) && !eCommissione(v) && classe(v) == null) daVerificare.add(v);
        }
        for (List<String[]> unita : unita(daVerificare)) {
            List<Gamba> richieste = gambeUnita(unita, commissioniArrivo);
            if (richieste.isEmpty()) continue;
            Indice indice = pool.get(poolDi(unita.get(0)));
            List<Gamba> prese = new ArrayList<>();
            boolean tutte = true;
            for (Gamba richiesta : richieste) {
                Gamba g = indice.trova(richiesta.moneta, richiesta.importi, richiesta.istante, richiesta.tolleranza);
                if (g == null) {
                    tutte = false;
                    break;
                }
                g.usata = true;
                prese.add(g);
            }
            if (tutte) {
                for (String[] v : unita) motivo.put(v, "movimento già presente da un'altra fonte");
            } else {
                for (Gamba g : prese) g.usata = false;
            }
        }

        // 3) Commissioni: seguono il movimento a cui sono legate, altrimenti contano per conto loro
        for (String[] v : nuove) {
            if (!eBinance(v) || !eCommissione(v)) continue;
            List<String[]> legate = legate(v, nuove);
            // la gamba propria dell'archivio, se c'è: una commissione senza movimento legato conta per conto suo
            Gamba propria = null;
            List<Gamba> richieste = gambeDi(v, Map.of());
            if (!richieste.isEmpty()) {
                Gamba r0 = richieste.get(0);
                propria = pool.get(Pool.FEE).trova(r0.moneta, r0.importi, r0.istante, r0.tolleranza);
            }
            boolean duplicata;
            if (legate.isEmpty()) {
                duplicata = propria != null;
            } else {
                duplicata = true;
                for (String[] l : legate) {
                    if (!motivo.containsKey(l)) {
                        duplicata = false;
                        break;
                    }
                }
            }
            if (duplicata) {
                if (propria != null) propria.usata = true;
                motivo.put(v, "commissione di un movimento già presente");
            }
        }

        // ---- Esito, nell'ordine di arrivo ----
        for (String[] v : nuove) {
            String m = motivo.get(v);
            if (m == null) r.nuove.add(v);
            else r.scarta(v, m);
        }
        return r;
    }

    // ======================================================================================================
    // Lettura delle righe
    // ======================================================================================================

    private static String campo(String[] v, int i) {
        return v != null && v.length > i && v[i] != null ? v[i] : "";
    }

    private static boolean eBinance(String[] v) {
        return EXCHANGE.equalsIgnoreCase(campo(v, 3).trim());
    }

    private static boolean eCommissione(String[] v) {
        return "COMMISSIONI".equalsIgnoreCase(campo(v, 5).trim());
    }

    /** Famiglia di un premio Earn dalla causale originale (campo 7), {@code null} se non è nota. */
    private static Classe classe(String[] v) {
        if (!"EARN".equalsIgnoreCase(campo(v, 5).trim())) return null;
        String c = campo(v, 7).toUpperCase(Locale.ROOT);
        if (c.contains("REALTIME") || c.contains("REAL-TIME")) return Classe.REALTIME;
        if (c.contains("LOCKED")) return Classe.LOCKED;
        if (c.contains("BONUS") || c.contains("FLEXIBLE INTEREST")) return Classe.FLEXIBLE;
        return null;
    }

    private static Pool poolDi(String[] v) {
        if (eCommissione(v)) return Pool.FEE;
        if (TIPI_PREMIO.contains(campo(v, 5).trim().toUpperCase(Locale.ROOT))) return Pool.PREMIO;
        return Pool.ALTRO;
    }

    private static long tolleranzaDi(String[] v) {
        String tipo = campo(v, 5).trim().toUpperCase(Locale.ROOT);
        if (TIPI_FIAT.contains(tipo)) return TOLLERANZA_FIAT_S;
        if (TIPI_PREMIO.contains(tipo)) return TOLLERANZA_PREMIO_S;
        if (TIPI_TRASFERIMENTO.contains(tipo)) return TOLLERANZA_TRASFERIMENTO_S;
        return TOLLERANZA_DEFAULT_S;
    }

    /** Istante in secondi (dal prefisso {@code yyyyMMddHHmmss} dell'ID), {@code Long.MIN_VALUE} se non leggibile. */
    private static long istanteDi(String[] v) {
        String id = campo(v, 0);
        if (id.length() < 14) return Long.MIN_VALUE;
        try {
            return LocalDateTime.of(Integer.parseInt(id.substring(0, 4)), Integer.parseInt(id.substring(4, 6)),
                    Integer.parseInt(id.substring(6, 8)), Integer.parseInt(id.substring(8, 10)),
                    Integer.parseInt(id.substring(10, 12)), Integer.parseInt(id.substring(12, 14))).toEpochSecond(ZoneOffset.UTC);
        } catch (RuntimeException ex) {
            return Long.MIN_VALUE;
        }
    }

    private static LocalDate giornoDi(String[] v) {
        String id = campo(v, 0);
        if (id.length() < 8) return null;
        try {
            return LocalDate.of(Integer.parseInt(id.substring(0, 4)), Integer.parseInt(id.substring(4, 6)),
                    Integer.parseInt(id.substring(6, 8)));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static BigDecimal numero(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String monetaEntrata(String[] v) {
        return campo(v, 11).trim();
    }

    private static BigDecimal quantitaEntrata(String[] v) {
        BigDecimal q = numero(campo(v, 13));
        return q == null ? BigDecimal.ZERO : q.abs();
    }

    private static String chiaveImporto(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString();
    }

    /** Le gambe non nulle di una riga: uscita con segno negativo, entrata positivo. */
    private static List<Leg> legs(String[] v) {
        List<Leg> l = new ArrayList<>();
        BigDecimal u = numero(campo(v, 10));
        if (u != null && u.signum() != 0 && !campo(v, 8).isBlank()) l.add(new Leg(campo(v, 8).trim(), u.abs().negate()));
        BigDecimal e = numero(campo(v, 13));
        if (e != null && e.signum() != 0 && !campo(v, 11).isBlank()) l.add(new Leg(campo(v, 11).trim(), e.abs()));
        return l;
    }

    /** Commissioni (righe COMMISSIONI con {@code [43]}) raggruppate per chiave del gruppo. */
    private static Map<String, List<String[]>> commissioniPerChiave(Collection<String[]> righe) {
        Map<String, List<String[]>> m = new HashMap<>();
        for (String[] v : righe) {
            if (!eBinance(v) || !eCommissione(v)) continue;
            String k = campo(v, 43).trim();
            if (!k.isEmpty()) m.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
        }
        return m;
    }

    /** I movimenti non commissione che condividono la chiave {@code [43]} di una commissione. */
    private static List<String[]> legate(String[] fee, List<String[]> righe) {
        String k = campo(fee, 43).trim();
        List<String[]> out = new ArrayList<>();
        if (k.isEmpty()) return out;
        for (String[] v : righe) {
            if (v != fee && eBinance(v) && !eCommissione(v) && k.equals(campo(v, 43).trim())) out.add(v);
        }
        return out;
    }

    /**
     * Le gambe di una riga con le quantità accettate: quella scritta e, se la riga ha commissioni collegate nella stessa
     * moneta, quella al netto di ciascuna e di tutte (deposito FIAT lordo contro netto, prelievo con commissione compresa).
     */
    private static List<Gamba> gambeDi(String[] v, Map<String, List<String[]>> commissioni) {
        List<Gamba> out = new ArrayList<>();
        long t = istanteDi(v);
        if (t == Long.MIN_VALUE) return out;
        String k = campo(v, 43).trim();
        List<String[]> fee = eCommissione(v) || k.isEmpty() ? List.of() : commissioni.getOrDefault(k, List.of());
        for (Leg leg : legs(v)) {
            Gamba g = new Gamba(leg.moneta(), t, tolleranzaDi(v));
            g.importi.add(chiaveImporto(leg.importo()));
            BigDecimal tutte = leg.importo();
            boolean alMenoUna = false;
            for (String[] f : fee) {
                for (Leg lf : legs(f)) {
                    if (!lf.moneta().equals(leg.moneta())) continue;
                    // la commissione è sempre un'uscita: l'uscita cresce (prelievo con commissione compresa), l'entrata cala
                    // (deposito netto); in tutti e due i casi si somma la commissione con il segno negativo
                    BigDecimal commissione = lf.importo().abs().negate();
                    g.importi.add(chiaveImporto(leg.importo().add(commissione)));
                    tutte = tutte.add(commissione);
                    alMenoUna = true;
                }
            }
            if (alMenoUna) g.importi.add(chiaveImporto(tutte));
            out.add(g);
        }
        return out;
    }

    /**
     * Raggruppa le righe che sono lo stesso ordine eseguito a pezzi: stesso secondo, stesso tipo, stesse due monete, tutte e
     * due le gambe presenti. Ogni altra riga è un gruppo a sé.
     */
    private static List<List<String[]>> unita(List<String[]> righe) {
        Map<String, List<String[]>> m = new java.util.LinkedHashMap<>();
        int n = 0;
        for (String[] v : righe) {
            String chiave;
            if (legs(v).size() == 2 && campo(v, 0).length() >= 14) {
                chiave = campo(v, 0).substring(0, 14) + "|" + campo(v, 5) + "|" + campo(v, 8) + "|" + campo(v, 11);
            } else {
                chiave = "#" + (n++);
            }
            m.computeIfAbsent(chiave, k -> new ArrayList<>()).add(v);
        }
        return new ArrayList<>(m.values());
    }

    /** Le gambe di un gruppo: quelle della riga se è una sola, altrimenti le somme per moneta (senza quantità alternative). */
    private static List<Gamba> gambeUnita(List<String[]> unita, Map<String, List<String[]>> commissioni) {
        if (unita.size() == 1) return gambeDi(unita.get(0), commissioni);
        long t = istanteDi(unita.get(0));
        List<Gamba> out = new ArrayList<>();
        if (t == Long.MIN_VALUE) return out;
        Map<String, BigDecimal> somme = new java.util.LinkedHashMap<>();
        for (String[] v : unita) {
            for (Leg leg : legs(v)) somme.merge(leg.moneta() + "|" + (leg.importo().signum() < 0 ? "-" : "+"), leg.importo(), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : somme.entrySet()) {
            Gamba g = new Gamba(e.getKey().substring(0, e.getKey().lastIndexOf('|')), t, tolleranzaDi(unita.get(0)));
            g.importi.add(chiaveImporto(e.getValue()));
            out.add(g);
        }
        return out;
    }

    private static void aggiungiAlGiorno(Map<String, TreeMap<LocalDate, GruppoGiorno>> m, Classe classe, String[] v) {
        LocalDate giorno = giornoDi(v);
        if (giorno == null) return;
        GruppoGiorno g = m.computeIfAbsent(classe + "|" + monetaEntrata(v), k -> new TreeMap<>())
                .computeIfAbsent(giorno, k -> new GruppoGiorno());
        g.somma = g.somma.add(quantitaEntrata(v));
    }
}
