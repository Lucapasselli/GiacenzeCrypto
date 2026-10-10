package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Giacenze lette dagli exchange, per la stessa colonna di confronto della blockchain in "Giacenze a data"
 * ({@link GiacenzeBlockchain}): OKX alla data scelta, Binance solo ad oggi.
 *
 * <p><b>OKX: il saldo sta nei bill gia' scaricati.</b> Ogni bill (conto Funding e conto Trading, archivio trimestrale
 * compreso) porta in {@code bal} il saldo della moneta dopo il movimento, quindi la giacenza a una data si legge dai
 * documenti di origine degli scaricamenti API, senza rete. Tre cose non ovvie, trovate sui dati reali:
 * <ul>
 *   <li><b>Un conto ha piu' catene di saldo per moneta</b>: ogni bot di trading ha il suo saldo, e i suoi bill
 *       riportano quello e non il saldo del conto. Il saldo del conto e' la somma delle catene. Un bill appartiene
 *       alla catena in cui {@code saldo precedente + balChg = bal}; quella di un bot fermato si chiude a zero col
 *       trasferimento di uscita.</li>
 *   <li><b>L'ordine della catena e' quello del {@code billId}, non dell'orario</b>: OKX a volte data un bill un
 *       secondo dopo pur avendolo registrato prima (un trasferimento dal Trading "dopo" la sottoscrizione Earn che lo
 *       usa). Ordinando per orario la catena si rompe e resta una giacenza fantasma.</li>
 *   <li><b>Le monete in Earn non sono nel {@code bal}</b>, mentre l'archivio le tiene nel wallet (le sottoscrizioni
 *       sono ignorate). Si aggiunge il capitale in Earn: sottoscrizioni meno riscatti (Simple Earn 75/76, Flash Deals e
 *       On-chain Earn 80/81/82), mai sotto zero (un riscatto porta anche gli interessi), e le posizioni On-chain Earn
 *       si chiudono alla data di riscatto dello storico ordini (OKX le converte in token di Liquid Staking, senza bill
 *       di riscatto). Gli interessi maturati dentro Earn non sono in nessuno dei due: mentre le monete sono in Earn
 *       l'archivio, che li registra, e' piu' alto di OKX esattamente di quegli interessi.</li>
 * </ul>
 * Prima del primo bill dei documenti la giacenza non si conosce ({@code n.d.}); una catena il cui primo bill non parte
 * da zero esisteva gia' prima dei documenti, e il suo saldo di partenza vale fino a quel bill.
 *
 * <p><b>Binance non ha uno storico dei saldi</b>: lo snapshot giornaliero copre solo l'ultimo mese e solo lo Spot
 * (documentazione ufficiale), mentre l'archivio tiene insieme Spot, Funding ed Earn. Si confronta quindi solo la
 * situazione di adesso ({@code Binance_Saldi.js}).
 */
final class GiacenzeExchange {

    private GiacenzeExchange() {
    }

    static final String OKX = "OKX";
    static final String BINANCE = "Binance";

    /** Coda del testo della colonna quando una parte della moneta e' in Earn (il confronto non si colora). */
    static final String IN_EARN = " (in Earn)";

    /**
     * Uno scaricamento che parte prima di questo istante (01/01/2018) ha chiesto tutta la storia: il programma chiede
     * dal 01/01/2017 quando non ha ancora nessun movimento OKX, e OKX non conserva nulla di cosi' vecchio.
     */
    static final long INIZIO_STORIA_COMPLETA = 1514764800000L;

    /** Tipi dei bill del Funding che spostano monete dentro o fuori un prodotto Earn. */
    static final Set<String> TIPI_EARN = Set.of("75", "76", "80", "81", "82");

    /**
     * L'exchange da confrontare per la selezione della scheda, come {@link GiacenzeBlockchain#WalletDaLeggere}: il
     * wallet dell'exchange, tutti i sotto-wallet (l'exchange non li conosce: "Principale", scambi differiti, Dual).
     *
     * @return {@link #OKX}, {@link #BINANCE} o {@code null}
     */
    static String ExchangeDaLeggere(String Wallet, String SottoWallet) {
        if (Wallet == null || SottoWallet == null || !SottoWallet.trim().equalsIgnoreCase("Tutti")) return null;
        if (Wallet.trim().equalsIgnoreCase(OKX)) return OKX;
        if (Wallet.trim().equalsIgnoreCase(BINANCE)) return BINANCE;
        return null;
    }

    /** Intestazione della colonna di confronto. */
    static String Intestazione(String Fonte) {
        return "<html><center>Qta<br>" + Fonte + "</html>";
    }

    //=====================================================================================================
    //=== OKX
    //=====================================================================================================

    /** Un bill: conto ({@code F} Funding, {@code T} Trading), moneta, orario, id, variazione e saldo dopo. */
    record Bill(String conto, String ccy, long ts, long billId, BigDecimal balChg, BigDecimal bal, String tipo, String note) {
    }

    /** Un ordine On-chain Earn chiuso: la moneta e la quantita' investita escono da Earn a {@code riscatto}. */
    record OrdineOnChain(String ordId, String ccy, BigDecimal qta, long acquisto, long riscatto) {
    }

    /** Un punto di una catena di saldo, nell'ordine del tempo. */
    private record Punto(long ts, long billId, BigDecimal bal) {
    }

    /** Una catena di saldo: il saldo prima del primo bill e i punti. */
    private static final class Catena {
        final BigDecimal inizio;
        final List<Punto> punti = new ArrayList<>();
        BigDecimal ultimo;

        Catena(BigDecimal inizio) {
            this.inizio = inizio;
            this.ultimo = inizio;
        }

        /** Il saldo della catena prima dell'istante: quello del bill con l'id piu' alto fra quelli anteriori. */
        BigDecimal Saldo(long t) {
            Punto migliore = null;
            for (Punto p : punti) {
                if (p.ts() >= t) continue;
                if (migliore == null || p.billId() > migliore.billId()) migliore = p;
            }
            return migliore == null ? inizio : migliore.bal();
        }
    }

    /** Lo storico ricostruito dai bill: catene per (conto, moneta) e movimenti dentro e fuori Earn. */
    static final class StoricoOKX {
        /** Primo bill dei documenti: prima la giacenza non si conosce. */
        long Inizio = Long.MAX_VALUE;
        /** Ultimo bill dei documenti. */
        long Fine = Long.MIN_VALUE;
        /**
         * Vero se almeno uno scaricamento ha chiesto tutta la storia ({@link #INIZIO_STORIA_COMPLETA}): solo allora una
         * moneta che non compare in nessun bill vale zero. Con scaricamenti incrementali (gli anni prima importati da
         * CSV) potrebbe essere su OKX senza essersi mossa, e sarebbe un falso "0" che invita a rettificare.
         */
        boolean StoriaCompleta = false;
        private final Map<String, List<Catena>> Catene = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /** moneta -> (istante, variazione del capitale in Earn), in ordine di tempo */
        private final Map<String, List<long[]>> EarnIstanti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, List<BigDecimal>> EarnVariazioni = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /** moneta -> istante da cui il capitale in Earn non si conosce (sottoscrizione On-chain senza ordine) */
        private final Map<String, Long> EarnIgnoto = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        boolean Vuoto() {
            return Inizio == Long.MAX_VALUE;
        }

        /** Il capitale in Earn prima dell'istante, mai negativo. */
        BigDecimal InEarn(String Moneta, long t) {
            List<long[]> ist = EarnIstanti.get(Moneta);
            if (ist == null) return BigDecimal.ZERO;
            List<BigDecimal> var = EarnVariazioni.get(Moneta);
            BigDecimal pos = BigDecimal.ZERO;
            for (int i = 0; i < ist.size(); i++) {
                if (ist.get(i)[0] >= t) break;
                pos = pos.add(var.get(i));
                if (pos.signum() < 0) pos = BigDecimal.ZERO;
            }
            return pos;
        }

        /**
         * Il testo della colonna di confronto: la giacenza, {@code n.d.} se non si conosce, e {@code (in Earn)} in coda
         * se una parte e' in Earn. In quel caso il confronto non si colora: l'archivio registra gli interessi maturati
         * dentro Earn, che il {@code bal} non ha, e il rosso sarebbe quasi sempre un falso allarme.
         */
        String Testo(String Moneta, long t) {
            BigDecimal q = Saldo(Moneta, t);
            if (q == null) return GiacenzeBlockchain.NON_DISPONIBILE;
            return InEarn(Moneta, t).signum() > 0 ? q.toPlainString() + IN_EARN : q.toPlainString();
        }

        /**
         * La giacenza di OKX della moneta prima dell'istante: somma delle catene di Funding e Trading e capitale in
         * Earn.
         *
         * @return {@code null} se non si conosce (prima dei documenti, Earn non ricostruibile)
         */
        BigDecimal Saldo(String Moneta, long t) {
            if (Vuoto() || t <= Inizio) return null;
            Long ignoto = EarnIgnoto.get(Moneta);
            if (ignoto != null && t > ignoto) return null;
            BigDecimal s = InEarn(Moneta, t);
            boolean Vista = EarnIstanti.containsKey(Moneta);
            for (String conto : new String[]{"F", "T"}) {
                List<Catena> cc = Catene.get(conto + "|" + Moneta);
                if (cc == null) continue;
                Vista = true;
                for (Catena c : cc) s = s.add(c.Saldo(t));
            }
            if (!Vista && !StoriaCompleta) return null;
            return s.stripTrailingZeros();
        }
    }

    /**
     * Costruisce lo storico dai bill (gia' senza doppioni) e dagli ordini On-chain Earn chiusi. Logica pura.
     */
    static StoricoOKX Costruisci(Collection<Bill> Bills, Collection<OrdineOnChain> Ordini) {
        return Costruisci(Bills, Ordini, true);
    }

    /** @param StoriaCompleta se almeno uno scaricamento ha chiesto tutta la storia (vedi {@link StoricoOKX#StoriaCompleta}) */
    static StoricoOKX Costruisci(Collection<Bill> Bills, Collection<OrdineOnChain> Ordini, boolean StoriaCompleta) {
        StoricoOKX s = new StoricoOKX();
        s.StoriaCompleta = StoriaCompleta;
        Map<String, List<Bill>> perCatena = new HashMap<>();
        for (Bill b : Bills) {
            s.Inizio = Math.min(s.Inizio, b.ts());
            s.Fine = Math.max(s.Fine, b.ts());
            perCatena.computeIfAbsent(b.conto() + "|" + b.ccy().toUpperCase(), k -> new ArrayList<>()).add(b);
        }
        BigDecimal tolleranza = new BigDecimal("1e-8");
        for (Map.Entry<String, List<Bill>> e : perCatena.entrySet()) {
            List<Bill> l = e.getValue();
            l.sort((a, b) -> Long.compare(a.billId(), b.billId()));
            List<Catena> catene = new ArrayList<>();
            for (Bill b : l) {
                Catena trovata = null;
                for (Catena c : catene) {
                    if (c.ultimo.add(b.balChg()).subtract(b.bal()).abs().compareTo(tolleranza) <= 0) {
                        trovata = c;
                        break;
                    }
                }
                if (trovata == null) {
                    trovata = new Catena(b.bal().subtract(b.balChg()));
                    catene.add(trovata);
                }
                trovata.ultimo = b.bal();
                trovata.punti.add(new Punto(b.ts(), b.billId(), b.bal()));
            }
            s.Catene.put(e.getKey(), catene);
        }

        //Earn: le sottoscrizioni e i riscatti dal conto Funding, e la chiusura delle posizioni On-chain
        List<Object[]> earn = new ArrayList<>();
        for (Bill b : Bills) {
            if (!"F".equals(b.conto()) || b.tipo() == null || !TIPI_EARN.contains(b.tipo())) continue;
            earn.add(new Object[]{b.ccy().toUpperCase(), b.ts(), b.balChg().negate()});
            boolean onChain = "80".equals(b.tipo()) && b.note() != null && b.note().toLowerCase().contains("on-chain");
            if (onChain && b.balChg().signum() < 0) {
                OrdineOnChain o = OrdineDellaSottoscrizione(b, Ordini);
                if (o == null) {
                    s.EarnIgnoto.merge(b.ccy().toUpperCase(), b.ts(), Math::min);
                } else if (o.riscatto() > 0) {
                    earn.add(new Object[]{o.ccy().toUpperCase(), o.riscatto(), o.qta().negate()});
                }
            }
        }
        earn.sort((a, b) -> Long.compare((Long) a[1], (Long) b[1]));
        for (Object[] x : earn) {
            s.EarnIstanti.computeIfAbsent((String) x[0], k -> new ArrayList<>()).add(new long[]{(Long) x[1]});
            s.EarnVariazioni.computeIfAbsent((String) x[0], k -> new ArrayList<>()).add((BigDecimal) x[2]);
        }
        return s;
    }

    /** L'ordine On-chain della sottoscrizione: stessa moneta e quantita', acquistato entro un giorno. */
    private static OrdineOnChain OrdineDellaSottoscrizione(Bill b, Collection<OrdineOnChain> Ordini) {
        BigDecimal q = b.balChg().abs();
        for (OrdineOnChain o : Ordini) {
            if (o.ccy().equalsIgnoreCase(b.ccy()) && o.qta().compareTo(q) == 0
                    && Math.abs(o.acquisto() - b.ts()) <= 86_400_000L) {
                return o;
            }
        }
        return null;
    }

    /** Vero se c'e' almeno un documento di uno scaricamento OKX via API: non legge i documenti, solo il registro. */
    static boolean OKXDisponibile() {
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            if (DocumentiFonte.TIPO_NDJSON.equals(d.Tipo) && ScartiImport.ORIGINE_OKX.equalsIgnoreCase(d.Origine.trim())) return true;
        }
        return false;
    }

    /** Ultimo storico costruito e i documenti da cui viene: si ricostruisce solo se cambiano. */
    private static volatile StoricoOKX StoricoCorrente;
    private static volatile String ChiaveStorico = "";

    /**
     * Lo storico di OKX dai documenti di origine degli scaricamenti API (NDJSON di origine "OKX"). Letto la prima volta
     * e tenuto finche' l'elenco dei documenti non cambia.
     *
     * @return {@code null} se non c'e' nessun documento con dei bill
     */
    static synchronized StoricoOKX OKX() {
        List<DocumentiFonte.Documento> docs = new ArrayList<>();
        StringBuilder chiave = new StringBuilder();
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            if (DocumentiFonte.TIPO_NDJSON.equals(d.Tipo) && ScartiImport.ORIGINE_OKX.equalsIgnoreCase(d.Origine.trim())) {
                docs.add(d);
                chiave.append(d.Id).append(',');
            }
        }
        if (docs.isEmpty()) return null;
        if (chiave.toString().equals(ChiaveStorico) && StoricoCorrente != null) return StoricoCorrente;
        List<File> file = new ArrayList<>();
        for (DocumentiFonte.Documento d : docs) {
            File f = DocumentiFonte.FileConservato(d.Id);
            if (f != null) file.add(f);
        }
        StoricoOKX s = DaFile(file);
        StoricoCorrente = s;
        ChiaveStorico = chiave.toString();
        return s;
    }

    /** Lo storico da documenti NDJSON compressi, senza passare dal registro. @return {@code null} se non ci sono bill */
    static StoricoOKX DaFile(Collection<File> File_) {
        Map<String, Bill> bills = new LinkedHashMap<>();
        Map<String, OrdineOnChain> ordini = new LinkedHashMap<>();
        long[] inizioRichiesto = {Long.MAX_VALUE};
        for (File f : File_) LeggiDocumento(f, bills, ordini, inizioRichiesto);
        return bills.isEmpty() ? null : Costruisci(bills.values(), ordini.values(), inizioRichiesto[0] < INIZIO_STORIA_COMPLETA);
    }

    /** Legge un documento NDJSON, tollerando la riga troncata di uno scaricamento interrotto. */
    private static void LeggiDocumento(File f, Map<String, Bill> Bills, Map<String, OrdineOnChain> Ordini, long[] InizioRichiesto) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(f)), StandardCharsets.UTF_8))) {
            String linea;
            while (true) {
                try {
                    linea = br.readLine();
                } catch (IOException troncato) {
                    break;
                }
                if (linea == null) break;
                LeggiRisposta(linea, Bills, Ordini, InizioRichiesto);
            }
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
        }
    }

    static void LeggiRisposta(String Linea, Map<String, Bill> Bills, Map<String, OrdineOnChain> Ordini) {
        LeggiRisposta(Linea, Bills, Ordini, new long[]{Long.MAX_VALUE});
    }

    /**
     * Una riga {@code {ts, script, argomenti, risposta}}: i bill dei due conti e gli ordini On-chain Earn.
     *
     * @param InizioRichiesto il minimo {@code startDate} degli scaricamenti dei bill, aggiornato qui
     */
    static void LeggiRisposta(String Linea, Map<String, Bill> Bills, Map<String, OrdineOnChain> Ordini, long[] InizioRichiesto) {
        JsonObject r;
        try {
            JsonElement e = JsonParser.parseString(Linea);
            if (!e.isJsonObject() || !e.getAsJsonObject().has("risposta") || !e.getAsJsonObject().get("risposta").isJsonObject()) return;
            JsonObject riga = e.getAsJsonObject();
            r = riga.getAsJsonObject("risposta");
            if ("OKX_Bills".equals(Testo(riga, "script"))) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("startDate=(\\d+)").matcher(Testo(riga, "argomenti"));
                if (m.find()) InizioRichiesto[0] = Math.min(InizioRichiesto[0], Lungo(m.group(1)));
            }
        } catch (RuntimeException troncata) {
            return;
        }
        AggiungiBill(r, "okx_fundingBills", "F", Bills);
        AggiungiBill(r, "okx_tradingBills", "T", Bills);
        AggiungiBill(r, "okx_archivioBills", "T", Bills);
        for (String k : new String[]{"staking_storico", "staking_attivi"}) {
            if (!r.has(k) || !r.get(k).isJsonArray()) continue;
            for (JsonElement el : r.getAsJsonArray(k)) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String ordId = Testo(o, "ordId");
                long acquisto = Lungo(Testo(o, "purchasedTime"));
                long riscatto = Lungo(Testo(o, "redeemedTime"));
                if (!o.has("investData") || !o.get("investData").isJsonArray()) continue;
                for (JsonElement inv : o.getAsJsonArray("investData")) {
                    if (!inv.isJsonObject()) continue;
                    BigDecimal q = Numero(Testo(inv.getAsJsonObject(), "amt"));
                    String ccy = Testo(inv.getAsJsonObject(), "ccy");
                    if (q == null || ccy.isEmpty()) continue;
                    //Lo stesso ordine compare attivo e poi chiuso: vince la versione col riscatto
                    String chiave = ordId + "|" + ccy;
                    OrdineOnChain prima = Ordini.get(chiave);
                    if (prima == null || prima.riscatto() <= 0) Ordini.put(chiave, new OrdineOnChain(ordId, ccy, q, acquisto, riscatto));
                }
            }
        }
    }

    private static void AggiungiBill(JsonObject r, String Chiave, String Conto, Map<String, Bill> Bills) {
        if (!r.has(Chiave) || !r.get(Chiave).isJsonArray()) return;
        for (JsonElement el : r.getAsJsonArray(Chiave)) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = Testo(o, "billId");
            BigDecimal chg = Numero(Testo(o, "balChg"));
            BigDecimal bal = Numero(Testo(o, "bal"));
            long ts = Lungo(Testo(o, "ts"));
            String ccy = Testo(o, "ccy");
            if (id.isEmpty() || chg == null || bal == null || ts <= 0 || ccy.isEmpty()) continue;
            long billId;
            try {
                billId = Long.parseLong(id);
            } catch (NumberFormatException ex) {
                continue;
            }
            Bills.putIfAbsent(Conto + "|" + id, new Bill(Conto, ccy, ts, billId, chg, bal, Testo(o, "type"), Testo(o, "notes")));
        }
    }

    private static String Testo(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString().trim() : "";
    }

    private static BigDecimal Numero(String s) {
        try {
            return s.isEmpty() ? null : new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static long Lungo(String s) {
        try {
            return s.isEmpty() ? 0 : Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    //=====================================================================================================
    //=== BINANCE
    //=====================================================================================================

    /** Validita' della lettura dei saldi di Binance: la tabella si ricalcola spesso, Binance non va chiamato ogni volta. */
    static final long DURATA_LETTURA_BINANCE_MS = TimeUnit.MINUTES.toMillis(5);

    private static volatile Map<String, BigDecimal> UltimaLetturaBinance;
    private static volatile long IstanteLetturaBinance;

    /** Vero se fra le API configurate c'e' una riga di Binance. */
    static boolean BinanceConfigurato() {
        return CredenzialiBinance() != null;
    }

    private static String[] CredenzialiBinance() {
        try {
            for (String[] r : DatabaseH2.Pers_ExchangeApi_LeggiTabella().values()) {
                if (r[1] != null && r[1].trim().equalsIgnoreCase(BINANCE) && r[2] != null && !r[2].isBlank()) return r;
            }
        } catch (RuntimeException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return null;
    }

    /**
     * I saldi attuali di Binance per moneta (Spot + Funding + Simple Earn), letti con le API configurate e tenuti per
     * {@link #DURATA_LETTURA_BINANCE_MS}. Pensata per un thread di background.
     *
     * @param Avvisi dove aggiungere cosa e' andato storto
     * @return {@code null} se le API non ci sono o se una delle parti non ha risposto: un totale senza una parte
     *         sembrerebbe giusto e non lo sarebbe
     */
    static synchronized Map<String, BigDecimal> SaldiBinance(List<String> Avvisi) {
        if (UltimaLetturaBinance != null && System.currentTimeMillis() - IstanteLetturaBinance < DURATA_LETTURA_BINANCE_MS) {
            return UltimaLetturaBinance;
        }
        String[] cred = CredenzialiBinance();
        if (cred == null) {
            Avvisi.add("nessuna API di Binance configurata");
            return null;
        }
        String json = EseguiScriptSaldi(cred[2], cred[3], Avvisi);
        if (json == null) return null;
        Map<String, BigDecimal> saldi = InterpretaSaldiBinance(json, Avvisi);
        if (saldi != null) {
            UltimaLetturaBinance = saldi;
            IstanteLetturaBinance = System.currentTimeMillis();
        }
        return saldi;
    }

    /** Somma le sezioni della risposta di {@code Binance_Saldi.js}. Logica pura. */
    static Map<String, BigDecimal> InterpretaSaldiBinance(String Json, List<String> Avvisi) {
        JsonObject o;
        try {
            o = JsonParser.parseString(Json).getAsJsonObject();
        } catch (RuntimeException ex) {
            Avvisi.add("risposta non leggibile");
            return null;
        }
        if (o.has("errori") && o.get("errori").isJsonObject() && !o.getAsJsonObject("errori").isEmpty()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("errori").entrySet()) {
                Avvisi.add(e.getKey() + ": " + e.getValue().getAsString());
            }
            return null;
        }
        Map<String, BigDecimal> saldi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String sezione : new String[]{"spot", "funding", "earnFlessibile", "earnBloccato"}) {
            if (!o.has(sezione) || !o.get(sezione).isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(sezione).entrySet()) {
                BigDecimal t = saldi.getOrDefault(e.getKey(), BigDecimal.ZERO);
                if (e.getValue().isJsonArray()) {
                    for (JsonElement v : e.getValue().getAsJsonArray()) {
                        BigDecimal n = Numero(v.getAsString());
                        if (n != null) t = t.add(n);
                    }
                }
                saldi.put(e.getKey(), t.stripTrailingZeros());
            }
        }
        return saldi;
    }

    private static String EseguiScriptSaldi(String Chiave, String Segreto, List<String> Avvisi) {
        try {
            CcxtInterop.ensureNodeInstalled();
            CcxtInterop.installCcxt();
            Path node = CcxtInterop.getNodeExePath();
            Path script = Paths.get(VarStatiche.getPathRisorse() + "Scripts/Binance_Saldi.js");
            if (!Files.exists(node) || !Files.exists(script)) {
                Avvisi.add("node o script non trovati");
                return null;
            }
            //Le credenziali passano come argomenti, come in tutti gli script di Binance; non si scrivono mai nel log
            Process p = ServizioNodePrezzi.ProcessoScript(node, script, List.of("binance", Chiave, Segreto)).start();
            java.util.concurrent.atomic.AtomicBoolean scaduto = CcxtInterop.avviaWatchdogTimeout(p, 2);
            Thread log = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) System.out.println("[Node-LOG] " + l);
                } catch (IOException ignored) {
                    //fine del processo
                }
            });
            log.setDaemon(true);
            log.start();
            String uscita = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor();
            if (scaduto.get()) {
                Avvisi.add("Binance non ha risposto in tempo");
                return null;
            }
            return uscita.isEmpty() ? null : uscita;
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            Avvisi.add(String.valueOf(ex.getMessage()));
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
