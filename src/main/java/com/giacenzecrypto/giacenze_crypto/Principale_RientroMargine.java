package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abbinamento dei <b>rientri del margine</b> ai prelievi del margine registrati dalla versione 1.0.66 (2026-11-20, decisione
 * dell'utente, D18.8 di {@code Decisioni_Derivati.md}).
 *
 * <p>La 1.0.66 registrava il margine messo all'apertura di una posizione OKX come un prelievo da classificare (causale {@code Sell},
 * senza marcatore ne' nota del contratto). Se la posizione si chiude dopo l'aggiornamento, l'importazione fa rientrare il margine con un
 * deposito da classificare ({@link OKXDerivati#CAUSALE_RIENTRO}). Lasciati cosi', il prelievo conta come una cessione e il deposito
 * entra a costo zero. Qui i due lati diventano un <b>blocco e uno sblocco di collaterale</b>, le classificazioni che il programma ha gia'
 * ({@code PTW}/{@code DTW} verso il sotto-wallet {@value #WALLET_COLLATERALE} dello stesso exchange): il costo di carico segue il margine e
 * torna con il rientro, senza cessione e senza costo zero.
 *
 * <p><b>Il margine che non rientra</b> (prelievi meno rientri, a posizione chiusa) e' il funding pagato prima dell'aggiornamento, che la
 * 1.0.66 non importava e che OKX ha tolto dal margine. Diventa un movimento a se' sul collaterale, classificato come derivato
 * ({@code PDV}, marcato {@link Derivati#FUNDING}) alla data dell'ultima chiusura, cosi' il collaterale torna a zero (lettura "a" scelta
 * dall'utente). Se ne e' tornato di piu' (funding incassato) e' un {@code DDV}. La data non e' quella dei pagamenti: quelle righe non ci sono.
 *
 * <p>Regole da non rompere:
 * <ul>
 *   <li>si abbina solo una posizione <b>chiusa</b> (l'ultimo rientro ha {@code margineTotale} zero in {@code [47]}): prima non si conosce il
 *       residuo. Una posizione chiusa in piu' volte ha un rientro per chiusura, e si abbinano tutti insieme;</li>
 *   <li>le quantita' dei movimenti originali non cambiano mai: il residuo e' una riga {@code AU} a parte. Cosi' l'annullamento standard
 *       ({@code RiportaTransazioniASituazioneIniziale}, che toglie le righe {@code AU}) e la deduplica dei reimport restano esatti;</li>
 *   <li>tutti i movimenti (prelievi, rientri, righe generate) sono <b>un solo gruppo</b> {@code [20]} simmetrico: annullando da uno
 *       qualunque si annulla tutto, e il controllo dei movimenti collegati non segnala nulla;</li>
 *   <li>lo sblocco <b>non</b> usa {@code GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoDa}: quella funzione classifica come reward
 *       ogni deposito da classificare con lo stesso {@code [24]}, cioe' il profitto della stessa chiusura;</li>
 *   <li>i prelievi della 1.0.66 non dicono il contratto: li sceglie l'utente, fra quelli dopo l'ultima chiusura registrata da quella
 *       versione (deposito {@code Buy}) prima del primo rientro, e il programma ne spunta uno solo quando e' l'unico. Un prelievo prima
 *       di quella chiusura non si abbina mai (decisione dell'utente del 20/11/2026): se la chiusura e' della stessa posizione, parte del
 *       margine e' gia' rientrata e il residuo sarebbe una perdita da derivati inventata. Il programma non puo' sapere se lo e'.</li>
 * </ul>
 * Logica senza Swing: il dialogo e' {@code GUI_RientroMargine}, la voce di menu e il ricalcolo stanno in {@code Principale}.
 */
public final class Principale_RientroMargine {

    private Principale_RientroMargine() {
    }

    /** Testo della voce del menu dei movimenti (sottomenu <i>Altre modifiche</i>). */
    public static final String VOCE_MENU = "Abbina rientro del margine";

    /** Sotto-wallet del collaterale: lo stesso della classificazione manuale «Messa a collaterale». */
    static final String WALLET_COLLATERALE = "Collaterale Bloccato";

    /** Causale ({@code [7]}) della riga del margine non rientrato, seguita da {@code |<instType>}. */
    static final String CAUSALE_RESIDUO = "Margine non rientrato";

    static final String DESCRIZIONE_BLOCCO = "BLOCCO COLLATERALE";
    static final String DETTAGLIO_BLOCCO = "PTW - Trasferimento a Collaterale";
    static final String DESCRIZIONE_SBLOCCO = "SBLOCCO COLLATERALE";
    static final String DETTAGLIO_SBLOCCO = "DTW - Trasferimento da Collaterale";

    /** Oltre questa frazione del margine bloccato il residuo e' sospetto: probabilmente un prelievo sbagliato o mancante. */
    private static final BigDecimal SOGLIA_RESIDUO = new BigDecimal("0.5");

    /** Rientri scritti dall'ultima importazione, per la domanda alla chiusura del resoconto. */
    private static final Set<String> NUOVI = new LinkedHashSet<>();

    // =================================================================================================
    // RICONOSCIMENTO
    // =================================================================================================

    /** @return {@code true} se il movimento e' un deposito di rientro del margine ({@link OKXDerivati#CAUSALE_RIENTRO}) */
    static boolean IsRientro(String[] v) {
        return v != null && v.length > Derivati.CAMPO_DATI && v[0] != null && v[0].endsWith("_DC")
                && v[7] != null && v[7].startsWith(OKXDerivati.CAUSALE_RIENTRO + "|");
    }

    /**
     * Una riga dell'abbinamento di un rientro ({@link #Esegui}): blocco o sblocco del collaterale (gambe del giroconto del margine) o margine
     * non rientrato. Lo si riconosce dal gruppo ({@code [20]}) che contiene un rientro: "BLOCCO COLLATERALE" vale anche per i collaterali
     * DeFi classificati a mano. Serve all'opzione P2 ({@link ContoDerivati}): il collaterale di queste righe e' il conto derivati di allora.
     */
    static boolean IsRigaAbbinamento(String[] v, java.util.Map<String, String[]> mappa) {
        if (v == null || v.length <= Derivati.CAMPO_DATI || Vuoto(v[20])) return false;
        boolean forma = DESCRIZIONE_BLOCCO.equals(v[5]) || DESCRIZIONE_SBLOCCO.equals(v[5])
                || (v[7] != null && v[7].startsWith(CAUSALE_RESIDUO + "|"));
        if (!forma) return false;
        if (IsRientro(v)) return true;
        for (String id : v[20].split(",")) {
            if (IsRientro(mappa.get(id.trim()))) return true;
        }
        return false;
    }

    /** @return {@code true} se la riga dell'abbinamento e' una gamba del giroconto (blocco o sblocco), non il margine non rientrato */
    static boolean IsGirocontoAbbinamento(String[] v, java.util.Map<String, String[]> mappa) {
        return (DESCRIZIONE_BLOCCO.equals(v[5]) || DESCRIZIONE_SBLOCCO.equals(v[5])) && IsRigaAbbinamento(v, mappa);
    }

    /** La voce di menu e' attiva su un rientro del margine ancora da classificare. */
    public static boolean VoceAttiva(String[] v) {
        return IsRientro(v) && (v[18] == null || v[18].isBlank());
    }

    /** L'ultima chiusura della posizione: l'exchange ha lasciato zero margine ({@code margineTotale}, il {@code posBal} del bill). */
    static boolean Chiude(String[] rientro) {
        String m = Derivati.Dati(rientro).get("margineTotale");
        return m != null && Funzioni.isNumeric(m, false) && new BigDecimal(m).signum() == 0;
    }

    /** Un prelievo del margine nella forma della 1.0.66, ancora da classificare. */
    private static boolean PrelievoVecchio(String[] v, String exchange, String wallet, String moneta) {
        return v != null && v.length > Derivati.CAMPO_DATI && v[0] != null && v[0].endsWith("_PC") && "Sell".equals(v[7])
                && exchange.equals(v[3]) && wallet.equals(v[4]) && moneta.equalsIgnoreCase(v[8])
                && OKXDerivati.ContrattoDaNota(v[21]) == null && Vuoto(v[18]) && Vuoto(v[20]) && !"AU".equalsIgnoreCase(v[22]);
    }

    /** Una chiusura nella forma della 1.0.66 (deposito {@code Buy} di margine e profitto), quale che sia la classificazione. */
    private static boolean ChiusuraVecchia(String[] v, String exchange, String wallet, String moneta) {
        return v != null && v.length > Derivati.CAMPO_DATI && v[0] != null && v[0].endsWith("_DC") && "Buy".equals(v[7])
                && exchange.equals(v[3]) && wallet.equals(v[4]) && moneta.equalsIgnoreCase(v[11])
                && OKXDerivati.ContrattoDaNota(v[21]) == null;
    }

    private static boolean Vuoto(String s) {
        return s == null || s.isBlank();
    }

    /** Istante in millisecondi: dal campo dati se c'e', altrimenti dai 14 caratteri iniziali dell'ID (che hanno i secondi). */
    private static long Istante(String[] v) {
        String ts = Derivati.Dati(v).get("ts");
        if (ts != null && Funzioni.isNumeric(ts, false)) return Long.parseLong(ts);
        if (v[0] != null && v[0].length() >= 14) return FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
        return 0;
    }

    private static BigDecimal Numero(String s) {
        return s != null && Funzioni.isNumeric(s, false) ? new BigDecimal(s) : BigDecimal.ZERO;
    }

    // =================================================================================================
    // LA POSIZIONE DA ABBINARE
    // =================================================================================================

    /** Una posizione aperta con la 1.0.66 e chiusa dopo: i suoi rientri e i prelievi che si possono abbinare. */
    public static final class Posizione {
        public String contratto = "";
        public String moneta = "";
        public String exchange = "";
        public String wallet = "";
        /** Tipo di strumento ({@code FUTURES}, {@code SWAP}), dalla causale del rientro. */
        public String tipo = "";
        /** I rientri della posizione, in ordine di tempo: l'ultimo e' quello della chiusura finale. */
        public final List<String> rientri = new ArrayList<>();
        /** I prelievi della 1.0.66 abbinabili, dal piu' recente. */
        public final List<String> candidati = new ArrayList<>();
        /** I candidati che il programma propone gia' scelti (uno solo, o nessuno). */
        public final Set<String> proposti = new LinkedHashSet<>();
        /** Perche' non si puo' abbinare, {@code null} se si puo'. */
        public String motivo;

        public String finale() {
            return rientri.isEmpty() ? null : rientri.get(rientri.size() - 1);
        }
    }

    /** {@link #Analizza(String, Map)} sull'archivio. */
    public static Posizione Analizza(String id) {
        return Analizza(id, MappaCryptoWallet);
    }

    /**
     * La posizione a cui appartiene un rientro del margine: tutti i rientri dello stesso contratto fino alla chiusura finale e i prelievi
     * della 1.0.66 che si possono abbinare (stesso exchange, sotto-wallet e moneta, da classificare, prima della chiusura finale).
     *
     * @param id l'ID di uno dei rientri della posizione
     * @param mappa i movimenti
     * @return la posizione, con {@link Posizione#motivo} valorizzato se non si puo' abbinare
     */
    static Posizione Analizza(String id, Map<String, String[]> mappa) {
        Posizione p = new Posizione();
        String[] scelto = mappa.get(id);
        if (!IsRientro(scelto)) {
            p.motivo = "Il movimento non è un rientro del margine di una posizione aperta con la versione 1.0.66.";
            return p;
        }
        p.contratto = OKXDerivati.ContrattoDaNota(scelto[21]);
        if (p.contratto == null) {
            p.motivo = "Il rientro non porta il contratto nelle note, quindi non si può ricostruire la sua posizione.";
            p.contratto = "";
            return p;
        }
        p.moneta = scelto[11];
        p.exchange = scelto[3];
        p.wallet = scelto[4];
        String causale = scelto[7];
        p.tipo = causale.substring(causale.indexOf('|') + 1);

        //I rientri dello stesso contratto in ordine di tempo, divisi in posizioni dalle chiusure finali
        List<String[]> tutti = new ArrayList<>();
        for (String[] v : mappa.values()) {
            if (IsRientro(v) && p.exchange.equals(v[3]) && p.contratto.equals(OKXDerivati.ContrattoDaNota(v[21]))) tutti.add(v);
        }
        tutti.sort((a, b) -> {
            int c = Long.compare(Istante(a), Istante(b));
            return c != 0 ? c : a[0].compareTo(b[0]);
        });
        List<String[]> gruppo = new ArrayList<>();
        boolean trovato = false;
        boolean chiusa = false;
        for (String[] v : tutti) {
            gruppo.add(v);
            if (v[0].equals(id)) trovato = true;
            if (Chiude(v)) {
                if (trovato) {
                    chiusa = true;
                    break;
                }
                gruppo.clear();
            }
        }
        for (String[] v : gruppo) p.rientri.add(v[0]);
        if (!chiusa) {
            p.motivo = "La posizione su " + p.contratto + " non risulta ancora chiusa del tutto. L'abbinamento si fa dopo il rientro "
                    + "dell'ultima chiusura, quando si conosce tutto il margine tornato indietro.";
            return p;
        }
        for (String[] v : gruppo) {
            if (!Vuoto(v[18])) {
                p.motivo = "Un rientro di questa posizione è già classificato (" + v[0] + "). Per abbinarli di nuovo va prima annullata "
                        + "quella classificazione.";
                return p;
            }
        }

        long primo = Istante(gruppo.get(0));
        long finale = Istante(gruppo.get(gruppo.size() - 1));
        //L'ultima chiusura registrata dalla 1.0.66 prima del primo rientro (deposito Buy di margine e profitto, quale che sia la
        //posizione: le righe di allora non dicono il contratto)
        String[] ultimaChiusuraVecchia = null;
        for (String[] v : mappa.values()) {
            if (!ChiusuraVecchia(v, p.exchange, p.wallet, p.moneta)) continue;
            long t = Istante(v);
            if (t < primo && (ultimaChiusuraVecchia == null || t > Istante(ultimaChiusuraVecchia))) ultimaChiusuraVecchia = v;
        }
        long limite = ultimaChiusuraVecchia == null ? Long.MIN_VALUE : Istante(ultimaChiusuraVecchia);
        //Candidati solo dopo quella chiusura (decisione dell'utente del 20/11/2026): con una chiusura della 1.0.66 fra prelievo e rientro
        //una parte del margine potrebbe essere gia' rientrata con lei, e il residuo diventerebbe una perdita da derivati che non c'e'
        List<String[]> candidati = new ArrayList<>();
        boolean primaDellaChiusura = false;
        for (String[] v : mappa.values()) {
            if (!PrelievoVecchio(v, p.exchange, p.wallet, p.moneta)) continue;
            long t = Istante(v);
            if (t >= finale) continue;
            if (t > limite) candidati.add(v);
            else primaDellaChiusura = true;
        }
        candidati.sort((a, b) -> {
            int c = Long.compare(Istante(b), Istante(a));
            return c != 0 ? c : b[0].compareTo(a[0]);
        });
        for (String[] v : candidati) p.candidati.add(v[0]);
        if (candidati.isEmpty() && primaDellaChiusura) {
            p.motivo = "Fra i prelievi del margine della versione 1.0.66 e il rientro c'è una chiusura registrata da quella versione, il "
                    + FunzioniDate.DataOraAlSecondo(ultimaChiusuraVecchia) + ". Se è di questa posizione una parte del margine è già "
                    + "rientrata con lei, e il margine non rientrato non sarebbe solo funding: risulterebbe un addebito per derivati che "
                    + "non c'è stato. Per questo l'abbinamento non si fa. Prelievo e rientro restano da classificare, e si possono "
                    + "classificare a mano come messa a collaterale e sblocco collaterale (Classifica Movimento).";
            return p;
        }
        if (candidati.isEmpty()) {
            p.motivo = "Non c'è nessun prelievo del margine della versione 1.0.66 da abbinare: stesso exchange, sotto-wallet " + p.wallet
                    + ", moneta " + p.moneta + ", da classificare e prima della chiusura.";
            return p;
        }
        //Si propone solo un candidato che non ha alternative
        if (candidati.size() == 1) p.proposti.add(candidati.get(0)[0]);
        return p;
    }

    // =================================================================================================
    // ANTEPRIMA
    // =================================================================================================

    /** I conti di un abbinamento, per il dialogo e per l'applicazione. */
    public static final class Anteprima {
        /** Margine uscito con i prelievi scelti (positivo). */
        public BigDecimal bloccato = BigDecimal.ZERO;
        /** Margine rientrato con i rientri della posizione. */
        public BigDecimal rientrato = BigDecimal.ZERO;
        /** Bloccato meno rientrato: positivo e' un costo da derivati, negativo un provento. */
        public BigDecimal residuo = BigDecimal.ZERO;
        /** Valore in euro del residuo, al prezzo unitario del rientro finale; {@code null} se il rientro non ha valore. */
        public BigDecimal valoreResiduo;
        /** Anni dei movimenti coinvolti che hanno una dichiarazione definitiva archiviata. */
        public final Set<String> anniDichiarati = new TreeSet<>();
        /** Il residuo supera meta' del margine bloccato: probabilmente un prelievo sbagliato o mancante. */
        public boolean residuoSospetto;
        /**
         * Chiusure nella forma della 1.0.66 (depositi {@code Buy}) fra il primo prelievo scelto e il primo rientro: se sono di questa
         * posizione, parte del margine e' gia' rientrata con loro e il residuo non e' solo funding (sarebbe una perdita da derivati
         * che non c'e'). Il programma non puo' saperlo: lo dice.
         */
        public final List<String> chiusureVecchieInMezzo = new ArrayList<>();
    }

    /** {@link #Calcola(Posizione, Collection, Map, Set)} sull'archivio, con gli anni delle definitive archiviate. */
    public static Anteprima Calcola(Posizione p, Collection<String> prelievi) {
        return Calcola(p, prelievi, MappaCryptoWallet, Principale_Derivati.AnniConDefinitiva.get());
    }

    static Anteprima Calcola(Posizione p, Collection<String> prelievi, Map<String, String[]> mappa, Set<String> anniDefinitiva) {
        Anteprima a = new Anteprima();
        int primoAnno = Integer.MAX_VALUE;
        int ultimoAnno = Integer.MIN_VALUE;
        long primoPrelievo = Long.MAX_VALUE;
        for (String id : prelievi) {
            String[] v = mappa.get(id);
            if (v == null) continue;
            a.bloccato = a.bloccato.add(Numero(v[10]).abs());
            int anno = Integer.parseInt(v[1].substring(0, 4));
            primoAnno = Math.min(primoAnno, anno);
            ultimoAnno = Math.max(ultimoAnno, anno);
            primoPrelievo = Math.min(primoPrelievo, Istante(v));
        }
        long primoRientro = Long.MAX_VALUE;
        for (String id : p.rientri) {
            String[] v = mappa.get(id);
            if (v == null) continue;
            a.rientrato = a.rientrato.add(Numero(v[13]).abs());
            int anno = Integer.parseInt(v[1].substring(0, 4));
            primoAnno = Math.min(primoAnno, anno);
            ultimoAnno = Math.max(ultimoAnno, anno);
            primoRientro = Math.min(primoRientro, Istante(v));
        }
        if (primoPrelievo != Long.MAX_VALUE) {
            for (String[] v : mappa.values()) {
                if (!ChiusuraVecchia(v, p.exchange, p.wallet, p.moneta)) continue;
                long t = Istante(v);
                if (t > primoPrelievo && t < primoRientro) a.chiusureVecchieInMezzo.add(v[0]);
            }
        }
        a.residuo = a.bloccato.subtract(a.rientrato);
        String[] finale = p.finale() == null ? null : mappa.get(p.finale());
        if (finale != null && a.residuo.signum() != 0) {
            BigDecimal qta = Numero(finale[13]).abs();
            BigDecimal valore = Numero(finale[15]).abs();
            if (qta.signum() != 0 && Funzioni.isNumeric(finale[15], false)) {
                a.valoreResiduo = valore.multiply(a.residuo.abs()).divide(qta, 2, RoundingMode.HALF_UP);
            }
        }
        //Fra il prelievo e il rientro il margine resta fra le giacenze dell'exchange: cambiano anche gli anni senza movimenti
        if (anniDefinitiva != null && primoAnno <= ultimoAnno) {
            for (int anno = primoAnno; anno <= ultimoAnno; anno++) {
                if (anniDefinitiva.contains(String.valueOf(anno))) a.anniDichiarati.add(String.valueOf(anno));
            }
        }
        a.residuoSospetto = a.bloccato.signum() > 0 && a.residuo.abs().compareTo(a.bloccato.multiply(SOGLIA_RESIDUO)) > 0;
        return a;
    }

    /**
     * Il testo che spiega l'abbinamento scelto: cosa diventano i movimenti e il residuo. Dice l'effetto, mai come compilare.
     */
    public static String TestoAnteprima(Posizione p, Anteprima a) {
        StringBuilder sb = new StringBuilder();
        String m = " " + p.moneta;
        sb.append("Margine uscito con i prelievi scelti: ").append(Fmt(a.bloccato)).append(m).append('\n');
        sb.append("Margine rientrato (").append(p.rientri.size()).append(p.rientri.size() == 1 ? " rientro" : " rientri").append("): ")
                .append(Fmt(a.rientrato)).append(m).append("\n\n");
        if (ContoDerivati.Attivo()) {
            sb.append("I prelievi diventano un blocco di collaterale e i rientri uno sblocco. Con l'opzione Conto derivati attiva il "
                    + "collaterale fa da conto derivati: il blocco è una cessione di cripto-attività al valore di allora, lo sblocco dà un "
                    + "reddito da derivati pari al valore meno quello del blocco, e il margine non rientrato vale il suo costo nel conto.\n");
        } else {
            sb.append("I prelievi diventano un blocco di collaterale e i rientri uno sblocco: il costo di carico segue il margine, senza "
                    + "cessione e senza ingresso a costo zero (che è come contano oggi, se i movimenti non classificati entrano nel calcolo).\n");
        }
        if (a.residuo.signum() > 0) {
            sb.append("Il margine non rientrato, ").append(Fmt(a.residuo)).append(m)
                    .append(a.valoreResiduo != null ? " (" + Fmt(a.valoreResiduo) + " euro)" : "")
                    .append(", diventa un addebito per derivati alla data dell'ultima chiusura: è il funding pagato prima "
                            + "dell'aggiornamento, che la versione 1.0.66 non importava.\n");
        } else if (a.residuo.signum() < 0) {
            sb.append("È rientrato più margine di quello uscito: la differenza, ").append(Fmt(a.residuo.abs())).append(m)
                    .append(a.valoreResiduo != null ? " (" + Fmt(a.valoreResiduo) + " euro)" : "")
                    .append(", diventa un accredito da derivati alla data dell'ultima chiusura (funding incassato prima "
                            + "dell'aggiornamento).\n");
        } else {
            sb.append("Il margine è rientrato tutto: nessun residuo.\n");
        }
        if (!a.chiusureVecchieInMezzo.isEmpty()) {
            sb.append("\nAttenzione: fra il primo prelievo scelto e il primo rientro ci sono ").append(a.chiusureVecchieInMezzo.size())
                    .append(a.chiusureVecchieInMezzo.size() == 1 ? " chiusura registrata" : " chiusure registrate")
                    .append(" dalla versione 1.0.66 (depositi di margine e profitto, nella stessa moneta). Se sono di questa posizione, una "
                            + "parte del margine è già rientrata con loro e il margine non rientrato qui sopra non è solo funding: "
                            + "risulterebbe un addebito per derivati che non c'è stato.\n");
        }
        if (a.residuoSospetto) {
            sb.append("\nIl residuo supera la metà del margine uscito: i prelievi scelti potrebbero non essere quelli di questa posizione, "
                    + "oppure ne manca qualcuno.\n");
        }
        if (!a.anniDichiarati.isEmpty()) {
            sb.append("\nL'abbinamento tocca anni con una dichiarazione definitiva archiviata (")
                    .append(String.join(", ", a.anniDichiarati)).append("): abbinandoli cambiano le plusvalenze e le giacenze di "
                    + "fine anno (quadro W/RW, ISEE) di quegli anni, perché fra il prelievo e il rientro il margine resta fra le giacenze "
                    + "dell'exchange, e il confronto con la definitiva segnalerà la differenza.\n");
        }
        return sb.toString();
    }

    private static String Fmt(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    // =================================================================================================
    // APPLICAZIONE
    // =================================================================================================

    /**
     * Abbina i prelievi scelti ai rientri della posizione, tutto o niente ({@link Principale_DepositiPrelievi.Esito#atomico}).
     * Ricontrolla che la posizione sia ancora abbinabile e che i prelievi siano ancora candidati. Chi chiama fa ricalcolare.
     *
     * @param p la posizione, da {@link #Analizza}
     * @param prelievi gli ID dei prelievi scelti (almeno uno)
     * @return {@code true} se l'abbinamento e' stato fatto
     */
    public static boolean Applica(Posizione p, Collection<String> prelievi) {
        if (p == null || p.motivo != null || p.rientri.isEmpty() || prelievi == null || prelievi.isEmpty()) return false;
        Posizione ora = Analizza(p.finale());
        if (ora.motivo != null || !ora.rientri.equals(p.rientri) || !ora.candidati.containsAll(prelievi)) return false;
        Anteprima a = Calcola(p, prelievi, MappaCryptoWallet, null);
        if (!a.chiusureVecchieInMezzo.isEmpty()) return false;   //non succede: i candidati vengono tutti dopo l'ultima chiusura vecchia
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();
        boolean fatto = esito.atomico("Abbinamento del rientro del margine di " + p.contratto, () -> {
            Esegui(p, new ArrayList<>(prelievi), a);
            return true;
        }, false);
        if (fatto) {
            LoggerGC.logInfo("Rientro del margine di " + p.contratto + " abbinato: prelievi " + String.join(", ", prelievi)
                    + ", rientri " + String.join(", ", p.rientri) + ", residuo " + a.residuo.toPlainString() + " " + p.moneta);
        } else {
            for (String problema : esito.problemi) LoggerGC.ScriviErrore(problema);
        }
        return fatto;
    }

    private static void Esegui(Posizione p, List<String> prelievi, Anteprima a) {
        List<String> gruppo = new ArrayList<>();
        String nota = "Contratto " + p.contratto + " - ";

        //Prelievi: blocco di collaterale, con la riga speculare sul collaterale (la stessa funzione della classificazione manuale)
        for (String id : prelievi) {
            String[] v = MappaCryptoWallet.get(id);
            String[] s = id.split("_");
            String speculare = s[0] + "_" + s[1] + "_" + s[2] + "A_" + s[3] + "_DC";
            Libero(speculare);
            GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA(id, DESCRIZIONE_BLOCCO, DETTAGLIO_BLOCCO, WALLET_COLLATERALE,
                    DESCRIZIONE_BLOCCO);
            String[] au = MappaCryptoWallet.get(speculare);
            if (au == null) throw new IllegalStateException("riga speculare del prelievo " + id + " non creata");
            au[21] = nota + "margine bloccato con la versione 1.0.66";
            gruppo.add(v[0]);
            gruppo.add(au[0]);
        }

        //Rientri: sblocco di collaterale, con la riga speculare costruita qui (vedi la descrizione della classe)
        for (String id : p.rientri) {
            String[] v = MappaCryptoWallet.get(id);
            String[] s = id.split("_");
            String speculare = s[0] + "_" + s[1] + "_0" + s[2] + "_" + s[3] + "_PC";
            Libero(speculare);
            Moneta out = new Moneta();
            out.Moneta = v[11];
            out.Tipo = v[12];
            out.Qta = new BigDecimal(v[13]).negate().stripTrailingZeros().toPlainString();
            out.NomeEsteso = v[27];
            out.MonetaAddress = v[28];
            String[] au = MovimentiCrypto.creaMovimento(out, null, v[3], WALLET_COLLATERALE, FunzioniDate.ConvertiDataIDinLong(s[0]),
                    MovimentiCrypto.PrezzoPrezzato(v[15]) ? v[15] : "0.00", null, 1, 1, null, null, "AU", v[24], null, null);
            if (au == null) throw new IllegalStateException("riga speculare del rientro " + id + " non creata");
            au[0] = speculare;
            au[1] = v[1];
            au[2] = "1 di 1";
            au[5] = DESCRIZIONE_SBLOCCO;
            au[15] = v[15];
            au[18] = "PTW - Trasferimento Interno";
            au[21] = nota + "margine rientrato";
            au[23] = v[23];
            au[29] = v[29];
            au[32] = "";
            au[40] = "";
            au[41] = DocumentiFonte.SoloOrigine(v[41]);
            MappaCryptoWallet.put(speculare, au);
            v[5] = DESCRIZIONE_SBLOCCO;
            v[18] = DETTAGLIO_SBLOCCO;
            gruppo.add(v[0]);
            gruppo.add(au[0]);
        }

        //Margine non rientrato: un addebito (o accredito) da derivati sul collaterale, alla data dell'ultima chiusura. L'ID ("_00")
        //viene prima dello sblocco nello stesso secondo: il collaterale non va mai sotto zero
        if (a.residuo.signum() != 0) {
            String[] f = MappaCryptoWallet.get(p.finale());
            String[] s = f[0].split("_");
            boolean addebito = a.residuo.signum() > 0;
            String idResiduo = s[0] + "_" + s[1] + "_00" + s[2] + "_" + s[3] + (addebito ? "_PC" : "_DC");
            Libero(idResiduo);
            Moneta m = new Moneta();
            m.Moneta = f[11];
            m.Tipo = f[12];
            m.Qta = a.residuo.negate().stripTrailingZeros().toPlainString();   //addebito in uscita, accredito in entrata
            m.NomeEsteso = f[27];
            m.MonetaAddress = f[28];
            String valore = a.valoreResiduo == null ? null : a.valoreResiduo.toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(addebito ? m : null, addebito ? null : m, f[3], WALLET_COLLATERALE,
                    FunzioniDate.ConvertiDataIDinLong(s[0]), valore, null, 1, 1, null, null, "AU", f[24], null, null);
            if (r == null) throw new IllegalStateException("riga del margine non rientrato non creata");
            r[0] = idResiduo;
            r[1] = f[1];
            r[2] = "1 di 1";
            r[5] = addebito ? Derivati.CAMPO5_ADDEBITO : Derivati.CAMPO5_ACCREDITO;
            r[7] = CAUSALE_RESIDUO + "|" + p.tipo;
            if (valore != null) r[15] = valore;
            r[18] = addebito ? Derivati.CAMPO18_ADDEBITO : Derivati.CAMPO18_ACCREDITO;
            r[21] = nota + "margine non rientrato (funding pagato prima dell'aggiornamento)";
            r[29] = f[29];
            r[32] = "";
            r[40] = "";
            r[41] = DocumentiFonte.SoloOrigine(f[41]);
            r[Derivati.CAMPO] = Derivati.FUNDING;
            MappaCryptoWallet.put(idResiduo, r);
            gruppo.add(idResiduo);
        }

        //Un solo gruppo simmetrico: ogni movimento cita tutti gli altri
        for (String id : gruppo) {
            List<String> altri = new ArrayList<>(gruppo);
            altri.remove(id);
            MappaCryptoWallet.get(id)[20] = String.join(",", altri);
        }
    }

    /** Un ID generato non deve mai sovrascrivere un movimento: l'eccezione annulla tutto l'abbinamento. */
    private static void Libero(String id) {
        if (MappaCryptoWallet.containsKey(id)) throw new IllegalStateException("l'ID " + id + " è già occupato");
    }

    // =================================================================================================
    // DOPO L'IMPORTAZIONE
    // =================================================================================================

    /** Annota i rientri fra i movimenti appena scritti dall'importazione ({@code Importazioni.ScriviListaSuMappaCrypto}). */
    static void RegistraNuovi(List<String[]> nuovi) {
        if (nuovi == null) return;
        synchronized (NUOVI) {
            for (String[] v : nuovi) if (IsRientro(v)) NUOVI.add(v[0]);
        }
    }

    /**
     * I rientri appena importati ancora da classificare, uno per posizione chiusa (la prima di ogni posizione basta: il dialogo li
     * abbina tutti insieme). Svuota l'elenco.
     */
    static List<String> DaAbbinareDopoImportazione() {
        List<String> ids;
        synchronized (NUOVI) {
            ids = new ArrayList<>(NUOVI);
            NUOVI.clear();
        }
        List<String> uno = new ArrayList<>();
        Set<String> visti = new LinkedHashSet<>();
        for (String id : ids) {
            if (!VoceAttiva(MappaCryptoWallet.get(id))) continue;
            Posizione p = Analizza(id);
            if (p.motivo != null || !visti.add(String.join(",", p.rientri))) continue;
            uno.add(id);
        }
        return uno;
    }
}
