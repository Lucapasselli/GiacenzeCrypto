package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.json.JSONObject;

/**
 * Posizioni su perpetui e futures di OKX come <b>token del contratto</b> e <b>margine parcheggiato</b>
 * (2026-11-16, tappa A di {@code nocommit/Documentazione/Decisioni_Derivati.md} D15/P1).
 *
 * <p><b>Che cosa produce.</b> Per ogni fill del conto Trading su un contratto (un bill {@code type 2} di strumento
 * {@code FUTURES} o {@code SWAP} con {@code posBalChg}):
 * <ul>
 *   <li><b>il margine</b> ({@code posBalChg}) passa dal wallet {@code Principale} al sotto-wallet {@value #SOTTOWALLET}
 *       all'apertura e torna indietro alla chiusura: due movimenti {@code TI} (trasferimento interno), neutri per
 *       costruzione perché restano nello stesso gruppo wallet;</li>
 *   <li><b>il token del contratto</b> ({@link Derivati#NomeTokenContratto}, quantità in <b>contratti</b>, {@code sz} del bill):
 *       un deposito {@code DCZ} a prezzo zero all'apertura, un prelievo {@code PWN} alla chiusura, nel sotto-wallet.
 *       Un long e uno short sono due token distinti, nessuno ha quantità negativa.</li>
 * </ul>
 * Tutto il resto dell'import resta com'è: il profitto realizzato e la commissione di ogni fill continuano a essere i
 * movimenti {@code DDV}/{@code PDV} di prima. Le nuove righe hanno <b>effetto fiscale nullo</b> (DCZ e PWN non
 * generano plusvalenza, TI non muove nulla) e portano in {@code [47]} tutti i dati del contratto
 * ({@link Derivati#CAMPO_DATI}). Il LIFO dei rapporti e il confronto con il pnl dell'exchange sono la tappa B.
 *
 * <p><b>Come arriva il fill fin qui.</b> {@code CcxtInterop.convertOKXBills} aggiunge alle righe intermedie una riga
 * "portatrice" per fill ({@code Contratto|FUTURES}, con i dati del bill in {@code [8]} e il {@code billId} seguito da
 * {@value #SUFFISSO_BILL} in {@code [14]}, altrimenti la deduplica per billId la scambierebbe per la riga del pnl).
 * {@code Ex_OKX_Consolida} la intercetta e chiama {@link #CreaMovimenti}; il {@code [24]} dei movimenti è il
 * {@code billId} puro, lo stesso delle righe del pnl e della commissione dello stesso fill.
 *
 * <p><b>Dove si ripiega sulla forma di prima</b> (nessuna riga di contratto, solo pnl e commissione):
 * <ul>
 *   <li>il bill non rispetta l'identità {@code balChg = -posBalChg + pnl + fee} o manca {@code posBalChg}
 *       (margine cross, archivio trimestrale in CSV);</li>
 *   <li>il verso non è deducibile ({@code subType} sconosciuto, o in disaccordo col segno di {@code posBalChg});</li>
 *   <li><b>una chiusura che non ha l'apertura</b> fra i movimenti già importati e quelli di questo scaricamento
 *       ({@link #FiltraPerSaldoAperto}): è il caso di una posizione aperta con la forma di prima e chiusa adesso, o di
 *       una chiusura più grande dell'apertura. Il token e il margine di quella chiusura non si creano, altrimenti
 *       resterebbero un prelievo senza lotto e un sotto-wallet in negativo. Se l'apertura era nella forma delle versioni
 *       fino alla 1.0.66 (il margine usciva come prelievo da classificare) la chiusura riporta il margine con un deposito
 *       da classificare ({@link #CAUSALE_RIENTRO}, vedi {@link #FiltraPerSaldoAperto}).</li>
 * </ul>
 * Il fill con una riga già in archivio (stesso {@code [24]}) non si reinserisce, come per ogni movimento OKX: una
 * posizione aperta con la forma di prima non riceve il token a metà strada.
 */
public final class OKXDerivati {

    private OKXDerivati() {
    }

    /** Causale della riga portatrice, seguita da {@code |<instType>}. Non sta in {@code OKX.json}: la riconosce il codice. */
    public static final String CAUSALE = "Contratto";
    /** Categoria interna ({@code [3]} della riga intermedia) assegnata alle righe portatrici. */
    public static final String CATEGORIA = "CONTRATTO DERIVATO";
    /** Suffisso del billId nel {@code [14]} della riga portatrice. */
    static final String SUFFISSO_BILL = "-DRV";
    /** Sotto-wallet (campo {@code [4]}) in cui stanno margine parcheggiato e token dei contratti. */
    public static final String SOTTOWALLET = "Derivati";
    /**
     * Causale ({@code [7]}, seguita da {@code |<instType>}) del deposito che riporta il margine di una posizione aperta con la
     * forma delle versioni fino alla 1.0.66 e chiusa dopo l'aggiornamento ({@link #FiltraPerSaldoAperto}). Non marcato come
     * derivato: è la controparte del prelievo da classificare con cui quel margine era uscito.
     */
    public static final String CAUSALE_RIENTRO = "Rientro margine";
    /** Numero di movimento (terzo segmento dell'ID) del deposito di rientro del margine: 901-903 sono margine e token. */
    static final int NUMERO_RIENTRO = 904;

    /** Scarto massimo, in valore assoluto, dell'identità del bill (OKX scrive fino a 18 decimali). */
    static final BigDecimal TOLLERANZA = new BigDecimal("0.0000001");

    static final String CAMPO5_APERTURA = "APERTURA CONTRATTO DERIVATO";
    static final String CAMPO18_APERTURA = "DCZ - APERTURA CONTRATTO DERIVATO";
    static final String CAMPO5_CHIUSURA = "CHIUSURA CONTRATTO DERIVATO";
    static final String CAMPO18_CHIUSURA = "PWN - CHIUSURA CONTRATTO DERIVATO";
    static final String FONTE_PREZZO = "Contratto derivato";

    /** @return {@code true} se la causale è quella di una riga portatrice ({@code Contratto|FUTURES}) */
    public static boolean IsCausale(String causale) {
        return causale != null && causale.startsWith(CAUSALE + "|");
    }

    /** @return {@code true} se la riga intermedia a 19 campi è una riga portatrice */
    public static boolean IsRiga(String[] riga) {
        return riga != null && riga.length > 4 && IsCausale(riga[4]);
    }

    // =================================================================================================
    // DAL BILL ALLA RIGA PORTATRICE
    // =================================================================================================

    /**
     * La riga portatrice di un fill, oppure {@code null} se il bill non si presta (vedi la descrizione della classe).
     *
     * @param obj il bill del conto Trading
     * @return la riga intermedia a 19 campi, {@code null} per ripiegare sulla forma di prima
     */
    static String[] RigaContratto(JSONObject obj) {
        String instType = obj.optString("instType", "").trim().toUpperCase();
        if (!instType.equals("FUTURES") && !instType.equals("SWAP")) return null;
        String tipoBill = obj.optString("type", "").trim();
        if (!"2".equals(tipoBill) && !TIPI_CHIUSURA_FORZATA.contains(tipoBill)) return null;
        String instId = obj.optString("instId", "").trim();
        String billId = obj.optString("billId", "").trim();
        String ccy = obj.optString("ccy", "").trim();
        if (instId.isEmpty() || billId.isEmpty() || ccy.isEmpty()) return null;

        BigDecimal balChg = Numero(obj, "balChg", false);
        BigDecimal posBalChg = Numero(obj, "posBalChg", false);
        BigDecimal pnl = Numero(obj, "pnl", true);
        BigDecimal fee = Numero(obj, "fee", true);
        BigDecimal sz = Numero(obj, "sz", false);
        BigDecimal ts = Numero(obj, "ts", false);
        if (balChg == null || posBalChg == null || pnl == null || fee == null || sz == null || ts == null) return null;
        if (posBalChg.signum() == 0 || sz.signum() <= 0) return null;

        //Identità del bill: il margine non e' mai stato un guadagno ne' una spesa, balChg = -posBalChg + pnl + fee.
        //Se non torna (margine cross, bill di altra natura) il margine di questo fill non lo si puo' attribuire. Una liquidazione
        //ha in piu' la penale (negativa), che il bill non riporta: e' la differenza (PenaleLiquidazione)
        BigDecimal penale = null;
        if (TIPO_LIQUIDAZIONE.equals(tipoBill)) {
            penale = PenaleLiquidazione(obj);
            if (penale == null) return null;
        } else if (balChg.add(posBalChg).subtract(pnl).subtract(fee).abs().compareTo(TOLLERANZA) > 0) return null;

        boolean apre = posBalChg.signum() > 0;
        String subTipo = obj.optString("subType", "").trim();
        Boolean lungo = Verso(subTipo, apre);
        if (lungo == null) return null;
        //Liquidazione, ADL e consegna chiudono sempre
        if (TIPI_CHIUSURA_FORZATA.contains(tipoBill) && apre) return null;

        Map<String, String> dati = new LinkedHashMap<>();
        dati.put("piattaforma", "OKX");
        dati.put("contratto", instId);
        dati.put("tipo", instType);
        dati.put("verso", lungo ? "long" : "short");
        dati.put("azione", apre ? "apre" : "chiude");
        dati.put("contratti", sz.stripTrailingZeros().toPlainString());
        dati.put("prezzo", obj.optString("px", ""));
        dati.put("prezzoIndice", obj.optString("fillIdxPx", ""));
        dati.put("prezzoMark", obj.optString("fillMarkPx", ""));
        dati.put("margine", posBalChg.abs().stripTrailingZeros().toPlainString());
        dati.put("moneta", ccy);
        //Una liquidazione puo' lasciare il margine sotto zero: il deficit lo copre OKX con il clawback (type 10), la posizione e' chiusa
        String posBal = obj.optString("posBal", "");
        if (Funzioni.isNumeric(posBal, false) && new BigDecimal(posBal).signum() < 0) {
            dati.put("deficit", new BigDecimal(posBal).negate().stripTrailingZeros().toPlainString());
            posBal = "0";
        }
        dati.put("margineTotale", posBal);
        if (!"2".equals(tipoBill)) dati.put("evento", EVENTI.get(tipoBill));
        if (penale != null && penale.signum() != 0) dati.put("penale", penale.stripTrailingZeros().toPlainString());
        dati.put("modoMargine", obj.optString("mgnMode", ""));
        dati.put("pnl", pnl.stripTrailingZeros().toPlainString());
        dati.put("commissione", fee.stripTrailingZeros().toPlainString());
        dati.put("ordine", obj.optString("ordId", ""));
        dati.put("scambio", obj.optString("tradeId", ""));
        dati.put("bill", billId);
        dati.put("ts", ts.toBigInteger().toString());

        String riga[] = new String[19];
        riga[0] = FunzioniDate.ConvertiDatadaLongAlSecondo(ts.longValue());
        riga[1] = "OKX";
        riga[2] = "Trading";
        riga[3] = "";
        riga[4] = CAUSALE + "|" + instType;
        riga[5] = ccy;
        riga[6] = posBalChg.stripTrailingZeros().toPlainString();
        riga[7] = instId;
        riga[8] = Derivati.ScriviDati(dati);
        riga[13] = obj.optString("ordId", "");
        riga[14] = billId + SUFFISSO_BILL;
        riga[15] = "NO";
        Importazioni.RiempiVuotiArray(riga);
        return riga;
    }

    /**
     * Il verso della posizione di un fill: {@code true} long, {@code false} short, {@code null} se non si sa.
     * In net mode {@code subType} 1 (acquisto) apre un long ma chiude uno short, 2 (vendita) apre uno short ma chiude
     * un long: lo decide il segno di {@code posBalChg} ({@code apre}). In long/short mode i codici 3-6 dicono tutto.
     */
    static Boolean Verso(String subTipo, boolean apre) {
        //Liquidazione (parziale 100-103, totale 104-107), consegna (112-113) e ADL (125-128): chiudono sempre. "long"/"sell" chiudono un
        //long, "short"/"buy" chiudono uno short (tabella dei subType dei bill di OKX)
        if (!apre) {
            switch (subTipo) {
                case "100", "103", "104", "107", "112", "125", "128":
                    return Boolean.TRUE;
                case "101", "102", "105", "106", "113", "126", "127":
                    return Boolean.FALSE;
                default:
                    break;
            }
        }
        switch (subTipo) {
            case "1":
                return apre;
            case "2":
                return !apre;
            case "3":
                return apre ? Boolean.TRUE : null;
            case "4":
                return apre ? Boolean.FALSE : null;
            case "5":
                return apre ? null : Boolean.TRUE;
            case "6":
                return apre ? null : Boolean.FALSE;
            default:
                return null;
        }
    }

    /** Bill che chiudono una posizione senza un ordine dell'utente: consegna a scadenza (3), liquidazione (5), ADL (9). */
    static final java.util.Set<String> TIPI_CHIUSURA_FORZATA = java.util.Set.of("3", "5", "9");
    /** Bill di liquidazione. */
    static final String TIPO_LIQUIDAZIONE = "5";
    /** Bill di clawback: OKX copre il deficit di una posizione liquidata ({@code posBalChg} positivo, {@code balChg} 0). */
    static final String TIPO_CLAWBACK = "10";
    private static final Map<String, String> EVENTI = Map.of("3", "consegna", "5", "liquidazione", "9", "adl");

    /**
     * La penale di una liquidazione (negativa, o zero): il bill non la riporta, e' la differenza dell'identita' del margine,
     * {@code balChg + posBalChg - pnl - fee}. Verificato sui bill reali del 21/11/2026: coincide con la {@code liqPenalty} dello storico
     * delle posizioni. {@code null} se il bill non ha i campi o la differenza e' positiva oltre la tolleranza (non e' una penale).
     */
    static BigDecimal PenaleLiquidazione(JSONObject obj) {
        BigDecimal balChg = Numero(obj, "balChg", false);
        BigDecimal posBalChg = Numero(obj, "posBalChg", false);
        BigDecimal pnl = Numero(obj, "pnl", true);
        BigDecimal fee = Numero(obj, "fee", true);
        if (balChg == null || posBalChg == null || pnl == null || fee == null) return null;
        BigDecimal p = balChg.add(posBalChg).subtract(pnl).subtract(fee);
        if (p.compareTo(TOLLERANZA) > 0) return null;
        return p.signum() > 0 ? BigDecimal.ZERO : p;
    }

    private static BigDecimal Numero(JSONObject obj, String chiave, boolean vuotoEZero) {
        String s = obj.optString(chiave, "").trim();
        if (s.isEmpty()) return vuotoEZero ? BigDecimal.ZERO : null;
        if (!Funzioni.isNumeric(s, false)) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =================================================================================================
    // DALLA RIGA PORTATRICE AI MOVIMENTI
    // =================================================================================================

    /**
     * I movimenti di un fill: margine in uscita e in entrata (TI) e token del contratto (DCZ o PWN).
     *
     * @param riga riga portatrice (19 campi), con {@code [1]} exchange e {@code [2]} wallet principale
     * @return i movimenti, vuota se la riga non è leggibile
     */
    static List<String[]> CreaMovimenti(String[] riga) {
        List<String[]> out = new ArrayList<>();
        Map<String, String> dati = Derivati.LeggiDati(riga[8]);
        String instId = dati.getOrDefault("contratto", riga[7]);
        String qtaContratti = dati.get("contratti");
        String qtaMargine = dati.get("margine");
        if (instId == null || instId.isBlank() || !Funzioni.isNumeric(qtaContratti, false) || !Funzioni.isNumeric(qtaMargine, false)) {
            return out;
        }
        boolean apre = "apre".equals(dati.get("azione"));
        boolean lungo = "long".equals(dati.get("verso"));
        String billId = riga[14] != null && riga[14].endsWith(SUFFISSO_BILL)
                ? riga[14].substring(0, riga[14].length() - SUFFISSO_BILL.length()) : riga[14];
        String wallet = riga[1];
        String principale = riga[2];
        long istante = FunzioniDate.ConvertiDatainLongSecondo(riga[0]);

        String valoreContratto = ValoreContratto(instId, ValoriContratti.Giorno(riga[0]));
        if (!valoreContratto.isEmpty()) dati.put("valoreContratto", valoreContratto);
        String testoDati = Derivati.ScriviDati(dati);

        BigDecimal margine = new BigDecimal(qtaMargine).abs();
        String moneta = dati.getOrDefault("moneta", riga[5]);
        String identificazione = wallet + billId;

        //Margine: esce da un wallet ed entra nell'altro, sempre nello stesso gruppo
        String daWallet = apre ? principale : SOTTOWALLET;
        String aWallet = apre ? SOTTOWALLET : principale;
        out.add(Margine(moneta, margine.negate(), wallet, daWallet, istante, billId, identificazione, 901, testoDati));
        out.add(Margine(moneta, margine, wallet, aWallet, istante, billId, identificazione, 902, testoDati));

        //Token del contratto, in quantita' di contratti
        Moneta token = new Moneta();
        token.Moneta = Derivati.NomeTokenContratto(instId, lungo);
        BigDecimal contratti = new BigDecimal(qtaContratti).abs();
        token.Qta = (apre ? contratti : contratti.negate()).toPlainString();
        token.AssegnaTipoAuto();
        String[] t = MovimentiCrypto.creaMovimento(token, null, wallet, SOTTOWALLET, istante, "0.00", FONTE_PREZZO,
                903, 1, null, null, "A", billId, apre ? "DEPOSITO A COSTO 0" : "RETTIFICA GIACENZA", identificazione);
        if (t != null) {
            t[5] = apre ? CAMPO5_APERTURA : CAMPO5_CHIUSURA;
            t[18] = apre ? CAMPO18_APERTURA : CAMPO18_CHIUSURA;
            Derivati.Marca(t, Derivati.CONTRATTO);
            t[Derivati.CAMPO_DATI] = testoDati;
            out.add(t);
        }
        out.removeIf(java.util.Objects::isNull);

        //Prezzo in euro di un'unita' della moneta di regolamento in questo istante: serve a dare un valore al differenziale
        //in LIFO (DerivatiLifo.AggiornaRisultati). Si scrive sulle righe del fill se la ricerca lo trova.
        BigDecimal unitario = PrezzoUnitarioMoneta(moneta, istante);
        if (unitario != null) {
            dati.put(DerivatiLifo.CHIAVE_PREZZO_MONETA, unitario.stripTrailingZeros().toPlainString());
            String conPrezzo = Derivati.ScriviDati(dati);
            for (String[] r : out) r[Derivati.CAMPO_DATI] = conPrezzo;
        }
        return out;
    }

    /** Prezzo di mercato in euro di un'unita' della moneta all'istante, {@code null} se non si trova (o nelle conversioni "a secco"). */
    private static BigDecimal PrezzoUnitarioMoneta(String moneta, long istante) {
        try {
            Moneta m = new Moneta();
            m.Moneta = moneta;
            m.Qta = "1";
            m.AssegnaTipoAuto();
            Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(m, null, istante, null, "okx");
            return ip != null && ip.prezzoUnitario != null && ip.prezzoUnitario.signum() > 0 ? ip.prezzoUnitario : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String[] Margine(String moneta, BigDecimal qta, String wallet, String sottoWallet, long istante,
            String billId, String identificazione, int numero, String testoDati) {
        Moneta m = new Moneta();
        m.Moneta = moneta;
        m.Qta = qta.toPlainString();
        m.AssegnaTipoAuto();
        String[] r = MovimentiCrypto.creaMovimento(m, null, wallet, sottoWallet, istante, null, null,
                numero, 1, null, null, "A", billId, "TRASFERIMENTO INTERNO", identificazione);
        if (r == null) return null;
        Derivati.Marca(r, Derivati.MARGINE);
        r[Derivati.CAMPO_DATI] = testoDati;
        return r;
    }

    // =================================================================================================
    // CHIUSURE SENZA APERTURA
    // =================================================================================================

    /**
     * Toglie dai movimenti da importare le righe di contratto e di margine dei fill di <b>chiusura</b> che non hanno la
     * posizione aperta: guardando i token già in archivio e quelli di questo stesso scaricamento, in ordine di
     * esecuzione. Un fill scartato perde tutte le sue righe (margine e token), non solo la chiusura; il suo pnl e la sua
     * commissione restano com'erano. Le aperture passano sempre.
     *
     * <p><b>Il margine che torna indietro e' quello parcheggiato, non quello dell'exchange.</b> {@code posBalChg} di una
     * chiusura e' il margine della posizione <i>in quel momento</i>: ha gia' dentro il funding pagato o incassato
     * (bill {@code type 8}, che toglie o aggiunge alla posizione) e ogni margine aggiunto o
     * tolto a mano. Riportarlo tale e quale lascerebbe il sotto-wallet con un residuo positivo (funding pagato) o in
     * <b>negativo</b> (funding incassato). Si riporta invece il margine parcheggiato ancora presente in proporzione ai
     * contratti chiusi, e tutto il residuo con l'ultima chiusura: a posizione chiusa il sotto-wallet e' a zero. Il valore
     * dell'exchange resta in {@code [47]} ({@code margineBorsa}), accanto a quello riportato ({@code margineRiportato}).
     *
     * <p><b>Posizione aperta con la forma delle versioni fino alla 1.0.66</b> (2026-11-19, decisione dell'utente, D18 punto 8
     * di {@code Decisioni_Derivati.md}). Allora un fill su derivati entrava come variazione di saldo: l'apertura e' in archivio
     * come un prelievo da classificare del margine (causale {@code Sell}, senza marcatore) e il funding non c'e'. Chiudendo
     * adesso entrerebbero solo profitto e commissione, e il margine uscito non rientrerebbe mai: il saldo della moneta di
     * regolamento resterebbe piu' basso di quello dell'exchange. Una chiusura senza apertura fra i token riporta quindi il
     * margine con un deposito da classificare ({@link #CAUSALE_RIENTRO}, senza marcatore, come il prelievo di allora) se
     * <ul>
     *   <li>c'e' la prova che il margine e' uscito: dopo l'ultima volta che i token davano il contratto a zero e prima della chiusura, un prelievo nella
     *       forma di allora (stesso exchange e sotto-wallet, causale {@code Sell}, nella moneta di regolamento, senza la nota del
     *       contratto), una riga {@code Sell|<instType>} del contratto senza token (archivio trimestrale, chiusura scartata) o
     *       un rientro precedente dello stesso contratto;</li>
     *   <li>e non c'e' un'apertura del contratto nella forma nuova senza token (commissione {@code Fee|<instType>} il cui fill
     *       non ha token: identita' del bill non rispettata, margine cross): li' il margine non e' mai uscito e un deposito
     *       creerebbe moneta che non esiste.</li>
     * </ul>
     * L'importo e' {@code margineBorsa} meno il margine parcheggiato riportato, piu' il funding del contratto importato dopo
     * quell'azzeramento o l'ultimo rientro (gia' tolto dal margine dell'exchange e gia' in archivio come movimento a se': senza
     * riaggiungerlo si conterebbe due volte). Cosi' la somma dei movimenti della posizione e' uguale a quella delle
     * variazioni di saldo dell'exchange. Una chiusura piu' grande dei contratti aperti fra i token (posizione aperta prima
     * dell'aggiornamento e aumentata dopo) chiude coi token i contratti che ci sono e riporta il resto cosi'. La causale
     * finisce fra quelle segnalate nel resoconto ({@link Importazioni#TestoAvvisoDerivati}).
     *
     * @param daImportare i movimenti già privi di quelli presenti in archivio (le righe di margine si correggono sul posto)
     * @param archivio i movimenti dell'archivio (valori della mappa)
     * @return una nuova lista, nello stesso ordine, con in coda gli eventuali depositi di rientro del margine
     */
    static List<String[]> FiltraPerSaldoAperto(List<String[]> daImportare, Iterable<String[]> archivio) {
        Map<String, BigDecimal> saldo = new LinkedHashMap<>();      //contratti aperti per simbolo del token
        Map<String, BigDecimal> parcheggiato = new LinkedHashMap<>(); //margine parcheggiato ancora presente, per simbolo
        List<String[]> tutte = new ArrayList<>();                     //archivio e scaricamento, per i rientri del margine
        if (archivio != null) {
            for (String[] v : archivio) {
                if (v == null || v.length <= Derivati.CAMPO_DATI) continue;
                tutte.add(v);
                if (Derivati.IsTokenContratto(v[11]) && Funzioni.isNumeric(v[13], false)) {
                    saldo.merge(v[11].toUpperCase(), new BigDecimal(v[13]).abs(), BigDecimal::add);
                } else if (Derivati.IsTokenContratto(v[8]) && Funzioni.isNumeric(v[10], false)) {
                    saldo.merge(v[8].toUpperCase(), new BigDecimal(v[10]).abs().negate(), BigDecimal::add);
                } else if (Derivati.Tipo(v).equals(Derivati.MARGINE) && OKXDerivati.SOTTOWALLET.equals(v[4])) {
                    String simbolo = SimboloDaDati(v);
                    if (simbolo == null) continue;
                    boolean entra = v[13] != null && Funzioni.isNumeric(v[13], false) && new BigDecimal(v[13]).signum() != 0;
                    BigDecimal q = entra ? new BigDecimal(v[13]).abs()
                            : Funzioni.isNumeric(v[10], false) ? new BigDecimal(v[10]).abs().negate() : BigDecimal.ZERO;
                    parcheggiato.merge(simbolo, q, BigDecimal::add);
                }
            }
        }

        int nArchivio = tutte.size();
        List<String[]> token = new ArrayList<>();
        Map<String, List<String[]>> marginiPerFill = new LinkedHashMap<>();
        for (String[] v : daImportare) {
            if (v == null || v.length <= Derivati.CAMPO_DATI) continue;
            tutte.add(v);
            if (Derivati.Tipo(v).equals(Derivati.CONTRATTO)) token.add(v);
            else if (Derivati.Tipo(v).equals(Derivati.MARGINE)) marginiPerFill.computeIfAbsent(v[24], k -> new ArrayList<>()).add(v);
        }
        token.sort(ORDINE_ESECUZIONE);
        Rientri rientri = new Rientri(tutte, nArchivio);

        Set<String> rifiutati = new HashSet<>();
        Set<String> daTogliere = new HashSet<>();   //righe di margine il cui importo riportato e' zero
        List<String[]> aggiunte = new ArrayList<>(); //depositi di rientro del margine
        boolean cambiate = false;                     //chiusure ridotte ai contratti aperti fra i token
        for (String[] v : token) {
            boolean apre = v[11] != null && !v[11].isBlank();
            String simbolo = (apre ? v[11] : v[8]).toUpperCase();
            BigDecimal q = new BigDecimal(apre ? v[13] : v[10]).abs();
            BigDecimal attuale = saldo.getOrDefault(simbolo, BigDecimal.ZERO);
            Map<String, String> dati = Derivati.Dati(v);
            if (apre) {
                saldo.put(simbolo, attuale.add(q));
                String m = dati.get("margine");
                if (m != null && Funzioni.isNumeric(m, false)) parcheggiato.merge(simbolo, new BigDecimal(m).abs(), BigDecimal::add);
            } else if (attuale.compareTo(q) >= 0) {
                BigDecimal residuo = parcheggiato.getOrDefault(simbolo, BigDecimal.ZERO).max(BigDecimal.ZERO);
                BigDecimal riportato = q.compareTo(attuale) == 0 ? residuo
                        : residuo.multiply(q).divide(attuale, 12, java.math.RoundingMode.HALF_UP);
                saldo.put(simbolo, attuale.subtract(q));
                parcheggiato.put(simbolo, residuo.subtract(riportato));
                RiportaMargine(marginiPerFill.get(v[24]), v, dati, riportato, daTogliere);
                if (attuale.compareTo(q) == 0) rientri.zero(dati.get("contratto"), IstanteMs(v));
            } else {
                String margineBorsa = dati.get("margine");
                BigDecimal riportato = BigDecimal.ZERO;
                if (attuale.signum() > 0) {
                    //Aperta prima dell'aggiornamento e aumentata dopo: i contratti aperti fra i token si chiudono coi token,
                    //con tutto il margine parcheggiato, e il resto rientra come quello della forma di allora
                    riportato = parcheggiato.getOrDefault(simbolo, BigDecimal.ZERO).max(BigDecimal.ZERO);
                    saldo.put(simbolo, BigDecimal.ZERO);
                    parcheggiato.put(simbolo, BigDecimal.ZERO);
                    dati.put("contrattiBorsa", q.stripTrailingZeros().toPlainString());
                    dati.put("contratti", attuale.stripTrailingZeros().toPlainString());
                    v[10] = attuale.negate().stripTrailingZeros().toPlainString();
                    RiportaMargine(marginiPerFill.get(v[24]), v, dati, riportato, daTogliere);
                    cambiate = true;
                } else {
                    rifiutati.add(v[24]);
                }
                String[] rientro = rientri.crea(v, dati, margineBorsa, riportato, marginiPerFill.get(v[24]), rifiutati);
                if (rientro != null) aggiunte.add(rientro);
                if (attuale.signum() > 0) rientri.zero(dati.get("contratto"), IstanteMs(v));
                LoggerGC.logInfo("OKX derivati: chiusura di " + q.toPlainString() + " su " + simbolo + " con soli "
                        + attuale.toPlainString() + " aperti fra i movimenti, bill " + v[24] + ": "
                        + (attuale.signum() > 0 ? "chiusi coi token i contratti aperti, " : "token e margine di questo fill non si creano, ")
                        + (rientro != null ? "margine della forma di prima riportato con un deposito di " + rientro[13]
                                : "nessun rientro del margine (" + rientri.motivo + ")"));
            }
        }
        if (rifiutati.isEmpty() && daTogliere.isEmpty() && aggiunte.isEmpty() && !cambiate) return daImportare;

        List<String[]> filtrata = new ArrayList<>();
        for (String[] v : daImportare) {
            if (v != null && v.length > Derivati.CAMPO_DATI && Derivati.IsRigaContrattoOMargine(v)) {
                if (rifiutati.contains(v[24])) continue;
                if (Derivati.Tipo(v).equals(Derivati.MARGINE) && daTogliere.contains(v[0])) continue;
            }
            filtrata.add(v);
        }
        filtrata.addAll(aggiunte);
        return filtrata;
    }

    /** Il contratto scritto nelle note da {@code Importazioni.NotaContrattoDerivato} ("Contratto X"), {@code null} se non c'e'. */
    static String ContrattoDaNota(String nota) {
        if (nota == null) return null;
        int i = nota.indexOf("Contratto ");
        if (i < 0) return null;
        String resto = nota.substring(i + "Contratto ".length());
        int fine = resto.indexOf(" - ");
        String c = (fine < 0 ? resto : resto.substring(0, fine)).trim();
        return c.isEmpty() ? null : c;
    }

    /** Istante di un movimento in millisecondi: dal campo dati se c'e', altrimenti dai 14 caratteri iniziali dell'ID. */
    private static long IstanteRiga(String[] v) {
        String ts = Derivati.Dati(v).get("ts");
        if (ts != null && Funzioni.isNumeric(ts, false)) return Long.parseLong(ts);
        if (v[0] != null && v[0].length() >= 14) return FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
        return 0;
    }

    private static BigDecimal Numero(String s) {
        return s != null && Funzioni.isNumeric(s, false) ? new BigDecimal(s) : BigDecimal.ZERO;
    }

    /**
     * Il deposito che riporta il margine di una posizione aperta con la forma delle versioni fino alla 1.0.66: le prove (vedi
     * {@link #FiltraPerSaldoAperto}) e l'importo. Tiene, per contratto, l'ultimo istante in cui i token lo davano a zero (una
     * chiusura che lo azzera) e l'ultimo rientro: prove e funding si cercano dopo, cosi' una posizione gia' chiusa coi token non
     * conta e una aumentata dopo l'aggiornamento (token aperti dopo il prelievo di allora) si'.
     */
    private static final class Rientri {
        private final List<String[]> tutte;
        /** Fill (campo 24) che hanno una riga di token, in archivio o nello scaricamento. */
        private final Set<String> fillConToken = new HashSet<>();
        private final Map<String, Long> ultimoZero = new LinkedHashMap<>();
        private final Map<String, Long> ultimoRientro = new LinkedHashMap<>();
        /** Perche' l'ultima chiusura non ha avuto il rientro, per il log. */
        String motivo = "";

        /** @param tutte l'archivio seguito dallo scaricamento, @param nArchivio quante righe di {@code tutte} vengono dall'archivio */
        Rientri(List<String[]> tutte, int nArchivio) {
            this.tutte = tutte;
            List<String[]> tokenArchivio = new ArrayList<>();
            for (int i = 0; i < tutte.size(); i++) {
                String[] v = tutte.get(i);
                if (Derivati.Tipo(v).equals(Derivati.CONTRATTO)) {
                    fillConToken.add(v[24]);
                    //i token dello scaricamento li segue il ciclo delle chiusure, in ordine di esecuzione
                    if (i < nArchivio) tokenArchivio.add(v);
                } else if (v[7] != null && v[7].startsWith(CAUSALE_RIENTRO + "|")) {
                    String c = ContrattoDaNota(v[21]);
                    if (c != null) ultimoRientro.merge(c, IstanteRiga(v), Math::max);
                }
            }
            //Quando i token dell'archivio hanno portato a zero ogni simbolo, ripercorsi in ordine di esecuzione
            tokenArchivio.sort(ORDINE_ESECUZIONE);
            Map<String, BigDecimal> saldo = new LinkedHashMap<>();
            for (String[] v : tokenArchivio) {
                boolean apre = v[11] != null && !v[11].isBlank();
                String simbolo = (apre ? v[11] : v[8]).toUpperCase();
                BigDecimal q = Numero(apre ? v[13] : v[10]).abs();
                BigDecimal nuovo = saldo.getOrDefault(simbolo, BigDecimal.ZERO).add(apre ? q : q.negate());
                saldo.put(simbolo, nuovo);
                if (!apre && nuovo.signum() <= 0) zero(Derivati.Dati(v).get("contratto"), IstanteRiga(v));
            }
        }

        void zero(String contratto, long istante) {
            if (contratto != null) ultimoZero.merge(contratto, istante, Math::max);
        }

        /**
         * @param chiusura la riga di token della chiusura (con i dati del fill)
         * @param margineBorsa il margine che l'exchange ha restituito con la chiusura ({@code posBalChg})
         * @param riportato il margine parcheggiato riportato coi token da questa stessa chiusura
         * @param margini le righe di margine del fill (per il sotto-wallet principale), anche {@code null}
         * @param rifiutati i fill di chiusura scartati fin qui (non hanno token)
         * @return il deposito, {@code null} se mancano le prove o l'importo non e' positivo
         */
        String[] crea(String[] chiusura, Map<String, String> dati, String margineBorsa, BigDecimal riportato,
                List<String[]> margini, Set<String> rifiutati) {
            String contratto = dati.get("contratto");
            String moneta = dati.get("moneta");
            if (contratto == null || moneta == null || !Funzioni.isNumeric(margineBorsa, false)) {
                motivo = "dati del fill incompleti";
                return null;
            }
            long istante = IstanteMs(chiusura);
            String exchange = chiusura[3];
            String principale = Importazioni.WALLET_OKX;
            if (margini != null) {
                for (String[] m : margini) if (!SOTTOWALLET.equals(m[4])) principale = m[4];
            }
            long daToken = ultimoZero.getOrDefault(contratto, Long.MIN_VALUE);
            long daRientro = Math.max(daToken, ultimoRientro.getOrDefault(contratto, Long.MIN_VALUE));

            boolean uscito = ultimoRientro.containsKey(contratto) && ultimoRientro.get(contratto) > daToken;
            BigDecimal funding = BigDecimal.ZERO;
            for (String[] v : tutte) {
                //prima i controlli che non costano: l'istante si legge dall'ID solo per le righe che possono contare
                String c = ContrattoDaNota(v[21]);
                boolean delContratto = contratto.equals(c);
                boolean prelievoVecchio = !uscito && c == null && "Sell".equals(v[7]) && v[0] != null && v[0].endsWith("_PC");
                if (!delContratto && !prelievoVecchio) continue;
                long t = IstanteRiga(v);
                if (t >= istante || t <= daToken) continue;
                String tipo = Derivati.Tipo(v);
                boolean senzaToken = !fillConToken.contains(v[24]) || rifiutati.contains(v[24]);
                if (contratto.equals(c) && tipo.equals(Derivati.COMMISSIONE) && v[7] != null && v[7].startsWith("Fee|")
                        && senzaToken) {
                    motivo = "il contratto ha un'apertura nella forma nuova senza token, il margine non e' uscito";
                    return null;
                }
                if (!uscito && v[0] != null && v[0].endsWith("_PC") && moneta.equalsIgnoreCase(v[8])) {
                    boolean formaPubblicata = "Sell".equals(v[7]) && exchange.equals(v[3]) && principale.equals(v[4]) && c == null;
                    boolean senzaContratto = contratto.equals(c) && v[7] != null && v[7].startsWith("Sell|") && senzaToken;
                    if (formaPubblicata || senzaContratto) uscito = true;
                }
                if (t > daRientro && contratto.equals(c) && tipo.equals(Derivati.FUNDING)) {
                    if (moneta.equalsIgnoreCase(v[11])) funding = funding.add(Numero(v[13]));
                    else if (moneta.equalsIgnoreCase(v[8])) funding = funding.add(Numero(v[10]));
                }
            }
            if (!uscito) {
                motivo = "nessun prelievo del margine con la forma di prima prima della chiusura";
                return null;
            }
            //Il funding pagato (negativo) ha gia' ridotto il margine dell'exchange ed e' gia' un movimento: lo si riaggiunge
            BigDecimal importo = new BigDecimal(margineBorsa).abs().subtract(riportato).subtract(funding);
            if (importo.signum() <= 0) {
                motivo = "importo non positivo";
                return null;
            }
            Moneta m = new Moneta();
            m.Moneta = moneta;
            m.Qta = importo.stripTrailingZeros().toPlainString();
            m.AssegnaTipoAuto();
            String bill = dati.getOrDefault("bill", chiusura[24]);
            String[] r = MovimentiCrypto.creaMovimento(m, null, exchange, principale, istante, null, null,
                    NUMERO_RIENTRO, 1, null, null, "A", chiusura[24], "TRASFERIMENTO-CRYPTO", exchange + bill);
            if (r == null) {
                motivo = "movimento non creato";
                return null;
            }
            r[7] = CAUSALE_RIENTRO + "|" + dati.getOrDefault("tipo", "");
            r[21] = "Contratto " + contratto;
            Map<String, String> d = new LinkedHashMap<>(dati);
            d.put("margineBorsa", new BigDecimal(margineBorsa).abs().stripTrailingZeros().toPlainString());
            d.put("margineRiportato", riportato.stripTrailingZeros().toPlainString());
            d.put("fundingRiaggiunto", funding.negate().stripTrailingZeros().toPlainString());
            d.put("rientroMargine", m.Qta);
            r[Derivati.CAMPO_DATI] = Derivati.ScriviDati(d);
            ultimoRientro.merge(contratto, istante, Math::max);
            Importazioni.CausaliDerivatiSegnalate.add(r[7]);
            motivo = "";
            return r;
        }
    }

    /** Simbolo del token del contratto di una riga, letto dal suo campo dati; {@code null} se il campo manca o e' incompleto. */
    private static String SimboloDaDati(String[] v) {
        Map<String, String> d = Derivati.Dati(v);
        String contratto = d.get("contratto");
        String verso = d.get("verso");
        if (contratto == null || verso == null) return null;
        return Derivati.NomeTokenContratto(contratto, verso.equals("long")).toUpperCase();
    }

    /**
     * Riscrive le due righe di margine di una chiusura con il margine parcheggiato da riportare, e lo dice nel campo dati
     * di tutte e tre le righe del fill. Con importo zero le righe di margine si tolgono.
     */
    private static void RiportaMargine(List<String[]> margini, String[] token, Map<String, String> dati,
            BigDecimal riportato, Set<String> daTogliere) {
        String borsa = dati.get("margine");
        dati.put("margineBorsa", borsa == null ? "" : borsa);
        dati.put("margine", riportato.stripTrailingZeros().toPlainString());
        dati.put("margineRiportato", riportato.stripTrailingZeros().toPlainString());
        String testo = Derivati.ScriviDati(dati);
        token[Derivati.CAMPO_DATI] = testo;
        if (margini == null) return;
        for (String[] m : margini) {
            m[Derivati.CAMPO_DATI] = testo;
            if (riportato.signum() == 0) {
                daTogliere.add(m[0]);
                continue;
            }
            boolean entra = m[11] != null && !m[11].isBlank();
            String q = riportato.stripTrailingZeros().toPlainString();
            if (entra) m[13] = q;
            else m[10] = "-" + q;
        }
    }

    /**
     * Ordine di esecuzione delle righe di token: istante del fill, poi billId come numero (OKX li assegna crescenti). Uno solo per
     * {@link #FiltraPerSaldoAperto}, i rientri e {@link DerivatiLifo#Calcola}: con due fill dello stesso millisecondo un ordine
     * diverso darebbe saldi ed episodi diversi.
     */
    static final java.util.Comparator<String[]> ORDINE_ESECUZIONE = (a, b) -> {
        int c = Long.compare(IstanteMs(a), IstanteMs(b));
        if (c != 0) return c;
        c = BillNumerico(a).compareTo(BillNumerico(b));
        if (c != 0) return c;
        return String.valueOf(a[0]).compareTo(String.valueOf(b[0]));
    };

    private static long IstanteMs(String[] v) {
        String ts = Derivati.Dati(v).get("ts");
        if (ts != null && Funzioni.isNumeric(ts, false)) return Long.parseLong(ts);
        return FunzioniDate.ConvertiDatainLongSecondo(v[1] + ":00");
    }

    private static BigInteger BillNumerico(String[] v) {
        String b = Derivati.Dati(v).getOrDefault("bill", v[24]);
        try {
            return new BigInteger(b.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    // =================================================================================================
    // VALORE DI UN CONTRATTO
    // =================================================================================================

    /**
     * Dove si legge {@code ctVal}: in produzione la rete, nei test un valore fisso. Riceve l'{@code instId}, restituisce {@code ctVal},
     * stringa vuota se OKX ha risposto ma non elenca il contratto (un futures a scadenza già scaduto), {@code null} se non si è potuto
     * chiedere (rete assente, errore): solo questo caso si riprova.
     */
    static volatile Function<String, String> FonteValoreContratto = OKXDerivati::ValoreContrattoDaRete;

    private static final Map<String, String> CACHE_VALORE = new ConcurrentHashMap<>();

    /** Come {@link #ValoreContratto(String, String)} senza il giorno: valgono solo le regole della tabella senza periodo. */
    static String ValoreContratto(String instId) {
        return ValoreContratto(instId, null);
    }

    /**
     * Valore di un contratto nell'unita' del sottostante ({@code ctVal}) per un fill di quel giorno. In ordine: un valore della
     * tabella {@link ValoriContratti} (valori di prima di un cambio, futures non piu' quotati), l'endpoint pubblico
     * {@code public/instruments} (senza chiavi, una chiamata per contratto per esecuzione), poi, se OKX non elenca il futures
     * e la tabella lo dice ({@code comeSwap}), il valore del perpetuo della stessa famiglia. La quantita' del token resta in
     * contratti, ma il valore serve al differenziale in LIFO ({@link DerivatiLifo}): senza, la posizione resta al profitto
     * dell'exchange, e lo stesso succede se il valore e' sbagliato (il controllo per posizione non torna). Un errore di rete non
     * si ricorda (si riprova alla chiamata dopo), un contratto che OKX non elenca si', e i movimenti gia' salvati senza il valore
     * si completano al primo import OKX dopo ({@link #CompletaValoriContratto}).
     *
     * @param giorno giorno del fill {@code yyyy-MM-dd}, null se non si sa (allora non valgono le regole con un periodo)
     * @return il valore, oppure stringa vuota se non si e' potuto leggere
     */
    static String ValoreContratto(String instId, String giorno) {
        if (instId == null || instId.isBlank()) return "";
        String chiave = instId.trim();
        ValoriContratti.Regola regola = ValoriContratti.Cerca(chiave, giorno);
        if (regola != null && !regola.comeSwap()) return regola.valore();
        String daRete = ValoreDaRete(chiave);
        //Non si e' potuto chiedere: niente valore adesso (si riprova), mai il perpetuo al suo posto, che verrebbe scritto per sempre
        if (daRete == null) return "";
        if (!daRete.isEmpty() || regola == null) return daRete;
        String[] ft = ValoriContratti.FamigliaETipo(chiave);
        return ValoreContratto(ft[0] + "-SWAP", giorno);
    }

    /**
     * Il valore letto dall'endpoint pubblico, con la memoria degli esiti certi.
     * @return il valore, stringa vuota se OKX ha risposto e non elenca il contratto, {@code null} se non si e' potuto chiedere
     */
    private static String ValoreDaRete(String chiave) {
        String trovato = CACHE_VALORE.get(chiave);
        if (trovato != null) return trovato;
        String letto;
        try {
            letto = FonteValoreContratto.apply(chiave);
        } catch (RuntimeException e) {
            LoggerGC.logInfo("OKX derivati: valore del contratto " + chiave + " non letto: " + e.getMessage());
            return null;
        }
        if (letto == null) return null;   //non si e' potuto chiedere: si riprova la prossima volta
        String v = Funzioni.isNumeric(letto.trim(), false) ? letto.trim() : "";
        CACHE_VALORE.put(chiave, v);
        return v;
    }

    /**
     * Completa con il valore del contratto ({@code valoreContratto} del campo dati) le righe di contratto e di margine gia' in
     * archivio che ne sono senza: l'import le aveva salvate mentre la lettura non riusciva, o prima della tabella
     * {@link ValoriContratti}. Una chiamata di rete per contratto ({@link #ValoreContratto}). Si richiama a ogni import OKX. Chi
     * chiama, se qualcosa e' cambiato, rifa' il passo del LIFO ({@link DerivatiLifo#AggiornaRisultati}): il campo dati sta fuori
     * dall'impronta del motore, che altrimenti non se ne accorgerebbe.
     *
     * @param movimenti i movimenti dell'archivio
     * @return quante righe sono state completate
     */
    static int CompletaValoriContratto(Iterable<String[]> movimenti) {
        int n = 0;
        for (String[] v : movimenti) {
            if (v == null || v.length <= Derivati.CAMPO_DATI || !Derivati.IsRigaContrattoOMargine(v)) continue;
            Map<String, String> d = Derivati.Dati(v);
            String contratto = d.get("contratto");
            String valore = d.get("valoreContratto");
            if (contratto == null || contratto.isBlank() || (valore != null && !valore.isBlank())) continue;
            valore = ValoreContratto(contratto.trim(), ValoriContratti.Giorno(v[1]));
            if (valore.isEmpty()) continue;
            d.put("valoreContratto", valore);
            Derivati.ImpostaDati(v, d);
            n++;
        }
        if (n > 0) LoggerGC.logInfo("OKX derivati: completato il valore del contratto su " + n + " movimenti gia' in archivio");
        return n;
    }

    /** Solo per i test: svuota la memoria dei valori letti. */
    static void AzzeraCacheValore() {
        CACHE_VALORE.clear();
    }

    private static String ValoreContrattoDaRete(String instId) {
        if (Funzioni.ReteDisabilitataPerTest) return null;
        //Dal file degli indirizzi (si cambia senza una versione): senza indirizzo nessuna risposta, il valore si riprova poi
        String base = IndirizziServizi.Url(IndirizziServizi.OKX_PUBBLICO);
        if (base.isEmpty()) return null;
        int risposte = 0;   //tipi per cui OKX ha risposto, anche con un elenco vuoto
        //L'instType non si deduce dall'instId in modo affidabile (i contratti "xperp" hanno una data e sono FUTURES): si provano i due
        for (String tipo : new String[]{"FUTURES", "SWAP"}) {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5)).build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(base + "/api/v5/public/instruments?instType=" + tipo
                                + "&instId=" + java.net.URLEncoder.encode(instId, java.nio.charset.StandardCharsets.UTF_8)))
                        .timeout(java.time.Duration.ofSeconds(8)).GET().build();
                java.net.http.HttpResponse<String> r = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() != 200) continue;
                JSONObject j = new JSONObject(r.body());
                //"0" = richiesta accettata: un elenco vuoto vuol dire che OKX non ha (piu') quel contratto fra quelli di questo tipo
                if ("0".equals(j.optString("code", ""))) risposte++;
                org.json.JSONArray a = j.optJSONArray("data");
                if (a == null || a.isEmpty()) continue;
                String ctVal = a.getJSONObject(0).optString("ctVal", "").trim();
                if (!ctVal.isEmpty()) return ctVal;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (java.io.IOException | RuntimeException e) {
                //si prova l'altro tipo, poi si rinuncia
            }
        }
        //"Non elencato" solo se entrambi i tipi hanno risposto: un errore su uno dei due si riprova
        return risposte == 2 ? "" : null;
    }
}
