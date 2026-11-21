package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Riconosce, fra i bill del conto Trading di OKX, le posizioni su perpetui o futures aperte con un
 * <b>voucher</b> ("contract voucher": un credito di margine per una sola posizione, con fondi separati da
 * quelli dell'utente) e le riduce al solo importo che il voucher ha davvero accreditato.
 *
 * <p><b>Perché serve.</b> Sui bill il margine del voucher esce e rientra come se fosse denaro dell'utente:
 * l'import produrrebbe un prelievo e un deposito di USDC che non sono mai passati dal suo saldo, con una
 * plusvalenza falsa se la pila LIFO di quella moneta è vuota. L'utente ha in realtà ricevuto soltanto
 * l'eccedenza oltre il valore del voucher (il profitto netto di commissioni e funding), che OKX accredita
 * con un bill {@code type 1 / subType 257} senza {@code from}, {@code to} e {@code notes}. Se la posizione va
 * in perdita OKX copre e non accredita nulla (e in quel caso questa classe <b>non</b> può riconoscere nulla,
 * vedi sotto).
 *
 * <p><b>Il solo criterio è un'identità esatta.</b> Una posizione è a voucher se e solo se:
 * <ol>
 *   <li>è un <i>episodio chiuso</i>: i bill {@code type 2} sullo stesso strumento e moneta, dal primo fill
 *       (che apre da zero, {@code posBal == posBalChg}) al fill che riporta {@code posBal} a zero;</li>
 *   <li>entro pochi secondi dalla chiusura c'è un bill {@code type 1 / subType 257} nella stessa moneta;</li>
 *   <li>la somma dei {@code balChg} di <b>tutto</b> l'episodio (fill e commissioni, più i funding fee
 *       {@code type 8} dello stesso strumento caduti dentro l'episodio) è uguale al {@code balChg} della
 *       257, a meno di {@link #TOLLERANZA}.</li>
 * </ol>
 * Se la stessa 257 risulta uguale a <b>più</b> episodi (due posizioni con lo stesso netto nello stesso momento)
 * non si abbina nessuno: non si sa quale sia, e in dubbio si lascia tutto com'è.
 *
 * <p>Nessun altro segnale è affidabile: il campo {@code bal} è il saldo di un libro, e anche i bot di trading
 * ne hanno di propri; {@code positions-history} ha {@code realizedPnl} per ogni posizione. L'uguaglianza
 * invece vale solo se il margine non ha toccato il saldo vero (verificata sui dati reali al centesimo di
 * milionesimo), e una posizione con fondi propri non ha la 257.
 *
 * <p><b>Cosa si fa quando c'è l'abbinamento.</b> Tutti i bill dell'episodio vengono omessi ({@link
 * Esito#omessi}) e la sola 257 diventa un movimento con la causale {@link #CAUSALE} seguita dal tipo di
 * strumento ({@code "Utilizzo di voucher|FUTURES"}), che {@code OKX.json} manda fra i depositi da classificare
 * e che l'import marca come derivato, come ogni altra riga dei derivati. Quando <b>non</b> c'è, non si
 * toglie nulla: il comportamento resta quello di ogni altra posizione su derivati.
 *
 * <p><b>Limiti noti, dichiarati e non nascosti.</b>
 * <ul>
 *   <li><b>Perdita o liquidazione:</b> senza la 257 non c'è nulla da abbinare, e il margine resta nei
 *       movimenti da classificare (con una perdita che l'utente non ha sostenuto).</li>
 *   <li><b>Episodio diviso fra due scaricamenti:</b> se l'import gira con la posizione ancora aperta, i fill
 *       di apertura sono già nell'archivio e la corsa dopo vede solo chiusura e 257: non si abbina, e le
 *       righe già salvate non si riscrivono. {@link Esito#episodiAperti} serve a dirlo nel log.</li>
 *   <li>Il CSV dell'archivio trimestrale non ha {@code posBal}: lì non si riconosce nulla. Chi lo importa
 *       toglie dalle sue righe quelle già omesse dallo scaricamento ({@link Esito#omessi},
 *       {@link Esito#voucher}), altrimenti tornerebbero dal secondo percorso.</li>
 * </ul>
 * Decisioni e dati reali su cui poggia: {@code nocommit/Documentazione/Dettagli_Scripts_Node_OKX.md}
 * (sezione voucher) e {@code Decisioni_Derivati.md} (D16).
 */
public final class OKXVoucher {

    private OKXVoucher() {
    }

    /** Causale della 257 abbinata: a questa si aggiunge {@code |<instType>}, che {@code OKX.json} mappa. */
    public static final String CAUSALE = "Utilizzo di voucher";

    /** Differenza massima fra la somma dell'episodio e la 257 perché si considerino uguali (OKX scrive 18 decimali). */
    static final BigDecimal TOLLERANZA = new BigDecimal("0.0000001");

    /** Finestra, rispetto alla chiusura, in cui cercare la 257: poco prima (ordine dei bill) e fino a due minuti dopo. */
    static final long FINESTRA_PRIMA_MS = 5_000L;
    static final long FINESTRA_DOPO_MS = 120_000L;

    /** Esito dell'analisi di un elenco di bill del Trading. */
    public static final class Esito {
        /** {@code billId} dei bill da non importare: fill, commissioni e funding degli episodi a voucher. */
        public final Set<String> omessi = new HashSet<>();
        /** {@code billId} della 257 abbinata, con il tipo di strumento ({@code FUTURES}, {@code SWAP}...). */
        public final Map<String, String> voucher = new LinkedHashMap<>();
        /** {@code billId} della 257 abbinata, con il contratto della posizione ({@code instId}). */
        public final Map<String, String> contratto = new LinkedHashMap<>();
        /** Episodi aperti e non ancora chiusi nei bill analizzati (la posizione è ancora aperta, o la chiusura non è stata scaricata). */
        public int episodiAperti = 0;

        /** @return {@code true} se il bill è da omettere o è una 257 abbinata (serve a chi importa dal secondo percorso) */
        public boolean riguarda(String billId) {
            return billId != null && (omessi.contains(billId) || voucher.containsKey(billId));
        }
    }

    /** Esito vuoto, per il conto Funding e per chi non ha bill. */
    public static Esito Vuoto() {
        return new Esito();
    }

    private static final class Bill {
        String id;
        String instType;
        String instId;
        String ccy;
        String tipo;
        String subTipo;
        BigDecimal balChg;
        BigDecimal posBal;       //null se assente
        BigDecimal posBalChg;    //null se assente
        long ts;
        BigInteger idNumerico;   //per ordinare i fill con lo stesso istante
    }

    /**
     * Analizza i bill del conto Trading.
     *
     * @param bills array dei bill così come li restituisce {@code OKX_Bills.js}, anche {@code null}
     * @return l'esito, mai {@code null}
     */
    public static Esito Analizza(JsonArray bills) {
        Esito esito = new Esito();
        if (bills == null || bills.isEmpty()) return esito;

        //Fill dei derivati per strumento+moneta, funding fee dei derivati, e le 257 candidate
        Map<String, List<Bill>> fillPerStrumento = new HashMap<>();
        Map<String, List<Bill>> fundingPerStrumento = new HashMap<>();
        List<Bill> candidate = new ArrayList<>();

        for (JsonElement el : bills) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String tipo = testo(o, "type");
            String subTipo = testo(o, "subType");
            boolean is257 = tipo.equals("1") && subTipo.equals("257");
            String instType = testo(o, "instType").toUpperCase();
            boolean derivato = CcxtInterop.INSTTYPE_DERIVATI_OKX.contains(instType);
            if (!is257 && !(derivato && (tipo.equals("2") || tipo.equals("8")))) continue;

            Bill b = new Bill();
            b.id = testo(o, "billId");
            b.instType = instType;
            b.instId = testo(o, "instId");
            b.ccy = testo(o, "ccy");
            b.tipo = tipo;
            b.subTipo = subTipo;
            b.balChg = numero(o, "balChg");
            b.posBal = numero(o, "posBal");
            b.posBalChg = numero(o, "posBalChg");
            BigDecimal ts = numero(o, "ts");
            if (b.id.isEmpty() || b.ccy.isEmpty() || b.balChg == null || ts == null) continue;
            b.ts = ts.longValue();
            try {
                b.idNumerico = new BigInteger(b.id);
            } catch (NumberFormatException e) {
                b.idNumerico = BigInteger.ZERO;
            }
            if (is257) {
                candidate.add(b);
            } else {
                if (b.instId.isEmpty()) continue;
                String chiave = b.instId + "|" + b.ccy;
                (tipo.equals("2") ? fillPerStrumento : fundingPerStrumento)
                        .computeIfAbsent(chiave, k -> new ArrayList<>()).add(b);
            }
        }

        Comparator<Bill> perTempo = Comparator.<Bill, Long>comparing(b -> b.ts).thenComparing(b -> b.idNumerico);
        //Ogni 257 con gli episodi che la reclamano: se e' uno solo l'abbinamento e' certo, se sono piu' d'uno
        //(due posizioni con lo stesso netto nello stesso momento) non si abbina nessuno, perche' non si sa quale
        Map<String, List<Reclamo>> reclami = new LinkedHashMap<>();

        for (Map.Entry<String, List<Bill>> e : fillPerStrumento.entrySet()) {
            List<Bill> fill = e.getValue();
            fill.sort(perTempo);
            List<Bill> episodio = new ArrayList<>();
            boolean episodioCompleto = true;   //false se il primo fill non apre da zero (iniziato prima dei bill scaricati)
            for (Bill b : fill) {
                if (episodio.isEmpty()) {
                    episodioCompleto = b.posBal != null && b.posBalChg != null && b.posBal.compareTo(b.posBalChg) == 0;
                }
                episodio.add(b);
                if (b.posBal != null && b.posBal.signum() == 0) {
                    if (episodioCompleto) {
                        reclama(episodio, fundingPerStrumento.get(e.getKey()), candidate, reclami);
                    }
                    episodio = new ArrayList<>();
                }
            }
            if (!episodio.isEmpty() && episodioCompleto) esito.episodiAperti++;
        }
        for (Map.Entry<String, List<Reclamo>> r : reclami.entrySet()) {
            if (r.getValue().size() != 1) continue;   //ambigua: nessun abbinamento
            Reclamo c = r.getValue().get(0);
            esito.voucher.put(r.getKey(), c.instType);
            esito.contratto.put(r.getKey(), c.instId);
            for (Bill b : c.bill) esito.omessi.add(b.id);
        }
        return esito;
    }

    /** Un episodio chiuso che ha trovato la sua 257, con tutti i bill da omettere se l'abbinamento regge. */
    private static final class Reclamo {
        final String instType;
        final String instId;
        final List<Bill> bill;

        Reclamo(String instType, String instId, List<Bill> bill) {
            this.instType = instType;
            this.instId = instId;
            this.bill = bill;
        }
    }

    /** Cerca la 257 che chiude i conti di un episodio chiuso e, se c'è, la registra fra i reclami. */
    private static void reclama(List<Bill> episodio, List<Bill> funding, List<Bill> candidate,
            Map<String, List<Reclamo>> reclami) {
        long inizio = episodio.get(0).ts;
        long chiusura = episodio.get(episodio.size() - 1).ts;
        BigDecimal somma = BigDecimal.ZERO;
        for (Bill b : episodio) somma = somma.add(b.balChg);
        List<Bill> fundingDentro = new ArrayList<>();
        if (funding != null) {
            for (Bill f : funding) {
                if (f.ts >= inizio && f.ts <= chiusura) {
                    somma = somma.add(f.balChg);
                    fundingDentro.add(f);
                }
            }
        }
        //Una posizione chiusa in pari non ha nulla da accreditare: nessuna 257 da abbinare
        if (somma.signum() <= 0) return;
        String ccy = episodio.get(0).ccy;
        for (Bill c : candidate) {
            if (!c.ccy.equals(ccy) || c.balChg.signum() <= 0) continue;
            if (c.ts < chiusura - FINESTRA_PRIMA_MS || c.ts > chiusura + FINESTRA_DOPO_MS) continue;
            if (c.balChg.subtract(somma).abs().compareTo(TOLLERANZA) > 0) continue;
            List<Bill> tutti = new ArrayList<>(episodio);
            tutti.addAll(fundingDentro);
            reclami.computeIfAbsent(c.id, k -> new ArrayList<>()).add(new Reclamo(episodio.get(0).instType, episodio.get(0).instId, tutti));
        }
    }

    private static String testo(JsonObject o, String chiave) {
        JsonElement e = o.get(chiave);
        return e == null || e.isJsonNull() ? "" : e.getAsString().trim();
    }

    private static BigDecimal numero(JsonObject o, String chiave) {
        String t = testo(o, chiave);
        if (t.isEmpty()) return null;
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
