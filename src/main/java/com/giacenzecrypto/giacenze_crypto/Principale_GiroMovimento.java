package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Il <b>giro</b> di un movimento (2026-11-20, punto 6 delle mancanze sui derivati, richiesta dell'utente del 19/11): i movimenti legati a
 * quello scelto, ciascuno con una riga che dice cos'e'. Serve a rendere leggibile tutto il giro, per l'utente e per un controllore.
 *
 * <p>Il giro e' <b>limitato</b>, mai calcolato a catena (un legame per stesso identificativo entro un'ora potrebbe tirarsi dietro righe che
 * non c'entrano):
 * <ol>
 *   <li>il movimento scelto e la sua <b>operazione</b> ({@link OperazioniCalcolate}: {@code [20]}, {@code [43]}, {@code [45]}, stesso
 *       {@code [24]} entro un'ora);</li>
 *   <li>se e' un derivato con un contratto, la sua <b>posizione</b>: l'episodio del contratto (da zero contratti a zero, la regola e' quella di
 *       {@link DerivatiLifo}, che registra le righe di ogni episodio in {@link DerivatiLifo.Posizione#righeEpisodio}), con le righe degli
 *       stessi fill ({@code [3]|[24]}: margine, profitto, commissione) e quelle con lo stesso contratto nelle note dentro l'episodio (funding,
 *       rientri del margine). Un rientro della 1.0.66 porta con se' gli altri rientri della sua posizione ({@link Principale_RientroMargine})
 *       e le righe della sua chiusura;</li>
 *   <li>un solo passo di {@code [20]} da ogni movimento trovato (i gruppi delle classificazioni).</li>
 * </ol>
 * La spiegazione si calcola ogni volta e non si salva. Dice l'effetto, mai cosa fare, e non cita mai la chiave di un'operazione ({@code OP-}),
 * che non e' stabile. Logica senza Swing, il dialogo e' {@code GUI_GiroMovimento}.
 */
public final class Principale_GiroMovimento {

    private Principale_GiroMovimento() {
    }

    /** Testo della voce del menu dei movimenti (sottomenu <i>Analizza</i>). */
    public static final String VOCE_MENU = "Mostra il giro del movimento";

    /** Oltre questo numero di righe il giro si ferma (una posizione tenuta per mesi ha centinaia di funding). */
    static final int MASSIMO = 2000;

    /** Perche' un movimento e' nel giro. */
    public enum Legame {
        SCELTO("Movimento scelto"),
        OPERAZIONE("Stessa operazione"),
        POSIZIONE("Stessa posizione"),
        COLLEGATO("Collegato dalla classificazione");

        public final String testo;

        Legame(String testo) {
            this.testo = testo;
        }
    }

    /** Una riga del giro. */
    public static final class Voce {
        public final String id;
        public final Legame legame;
        public final String spiegazione;

        Voce(String id, Legame legame, String spiegazione) {
            this.id = id;
            this.legame = legame;
            this.spiegazione = spiegazione;
        }
    }

    /** Il giro di un movimento. */
    public static final class Giro {
        public final String id;
        /** In ordine di tempo, il movimento scelto compreso. */
        public final List<Voce> voci = new ArrayList<>();
        /** Il contratto della posizione, vuoto se il movimento non e' di un derivato con contratto. */
        public String contratto = "";
        /** Righe lasciate fuori per il limite {@link #MASSIMO}. */
        public int escluse;

        Giro(String id) {
            this.id = id;
        }

        /** Somma del risultato dei derivati ({@code [46]}) delle righe del giro. */
        public BigDecimal RisultatoDerivati(Map<String, String[]> mappa) {
            BigDecimal t = BigDecimal.ZERO;
            for (Voce x : voci) {
                BigDecimal r = Derivati.Risultato(mappa.get(x.id));
                if (r != null) t = t.add(r);
            }
            return t;
        }

        /** Somma delle plusvalenze ({@code [19]}) delle righe che le hanno ({@code [33]} = S), come i riquadri della tabella. */
        public BigDecimal Plusvalenze(Map<String, String[]> mappa) {
            BigDecimal t = BigDecimal.ZERO;
            for (Voce x : voci) {
                String[] v = mappa.get(x.id);
                if (v != null && "S".equalsIgnoreCase(v[33]) && Funzioni.isNumeric(v[19], false)) t = t.add(new BigDecimal(v[19]));
            }
            return t;
        }
    }

    /** {@link #Calcola(String, Map)} sull'archivio. */
    public static Giro Calcola(String id) {
        return Calcola(id, MappaCryptoWallet);
    }

    /**
     * @param id il movimento scelto
     * @param mappa tutti i movimenti (la mappa intera: i riferimenti si cercano qui)
     * @return il giro, con il solo movimento scelto se non e' legato a nulla, vuoto se il movimento non c'e'
     */
    static Giro Calcola(String id, Map<String, String[]> mappa) {
        Giro g = new Giro(id);
        String[] scelto = mappa.get(id);
        if (scelto == null) return g;
        //ID senza distinzione fra maiuscole, come la mappa
        Map<String, Legame> membri = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        membri.put(scelto[0], Legame.SCELTO);

        //1. L'operazione
        Map<String, String> operazioni = OperazioniCalcolate.Calcola(mappa);
        String chiave = operazioni.get(scelto[0]);
        if (chiave != null) {
            for (String m : OperazioniCalcolate.Membri(operazioni, chiave)) membri.putIfAbsent(m, Legame.OPERAZIONE);
        }

        //2. La posizione sul contratto
        String contratto = ContrattoDi(scelto);
        if (contratto != null) {
            g.contratto = contratto;
            Posizione(scelto, contratto, mappa, membri);
        }
        if (Principale_RientroMargine.IsRientro(scelto)) {
            Principale_RientroMargine.Posizione p = Principale_RientroMargine.Analizza(scelto[0], mappa);
            for (String r : p.rientri) membri.putIfAbsent(r, Legame.POSIZIONE);
        }
        //Le righe della chiusura di ogni rientro (profitto e commissione dello stesso fill)
        Set<String> fillRientri = new HashSet<>();
        for (String m : new ArrayList<>(membri.keySet())) {
            String[] v = mappa.get(m);
            if (Principale_RientroMargine.IsRientro(v) && DerivatiLifo.ChiaveFill(v) != null) fillRientri.add(DerivatiLifo.ChiaveFill(v));
        }
        if (!fillRientri.isEmpty()) {
            for (String[] v : mappa.values()) {
                if (v != null && fillRientri.contains(DerivatiLifo.ChiaveFill(v))) membri.putIfAbsent(v[0], Legame.POSIZIONE);
            }
        }

        //3. Un passo di [20] da ogni movimento trovato
        for (String m : new ArrayList<>(membri.keySet())) {
            String[] v = mappa.get(m);
            if (v == null || v.length <= 20 || v[20] == null || v[20].isBlank()) continue;
            for (String c : v[20].split(",")) {
                if (!c.isBlank() && mappa.containsKey(c.trim())) membri.putIfAbsent(mappa.get(c.trim())[0], Legame.COLLEGATO);
            }
        }

        //In ordine di tempo (l'ID comincia con data e ora), con il limite
        int n = 0;
        for (Map.Entry<String, Legame> e : membri.entrySet()) {
            String[] v = mappa.get(e.getKey());
            if (v == null) continue;
            if (n >= MASSIMO && e.getValue() != Legame.SCELTO) {
                g.escluse++;
                continue;
            }
            g.voci.add(new Voce(v[0], e.getValue(), Spiega(v, mappa)));
            n++;
        }
        return g;
    }

    /** Il contratto di un movimento: dal campo dati per token e margine, dalle note ("Contratto X") per gli altri. */
    static String ContrattoDi(String[] v) {
        if (v == null) return null;
        if (v.length > Derivati.CAMPO_DATI && Derivati.IsRigaContrattoOMargine(v)) {
            String c = Derivati.Dati(v).get("contratto");
            return c == null || c.isBlank() ? null : c;
        }
        return v.length > 21 ? OKXDerivati.ContrattoDaNota(v[21]) : null;
    }

    /** Istante in millisecondi: dal campo dati se c'e', altrimenti dai 14 caratteri iniziali dell'ID. */
    private static long Istante(String[] v) {
        String ts = v.length > Derivati.CAMPO_DATI ? Derivati.Dati(v).get("ts") : null;
        if (ts != null && Funzioni.isNumeric(ts, false)) return Long.parseLong(ts);
        if (v[0] != null && v[0].length() >= 14) return FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
        return 0;
    }

    /**
     * Aggiunge la posizione del movimento: gli episodi del contratto che lo contengono (per un token o un fill con token, il suo episodio;
     * per un funding o un rientro, quelli il cui intervallo comprende il suo istante, in entrambi i versi), le righe degli stessi fill e
     * quelle con lo stesso contratto nelle note dentro gli episodi.
     */
    private static void Posizione(String[] scelto, String contratto, Map<String, String[]> mappa, Map<String, Legame> membri) {
        List<String[]> token = new ArrayList<>();
        for (String[] v : mappa.values()) {
            if (v != null && v.length > Derivati.CAMPO_DATI && Derivati.Tipo(v).equals(Derivati.CONTRATTO)
                    && contratto.equals(Derivati.Dati(v).get("contratto")) && scelto[3] != null && scelto[3].equals(v[3])) token.add(v);
        }
        if (token.isEmpty()) return;
        //Il token dello stesso fill del movimento scelto, se c'e'
        String fillScelto = DerivatiLifo.ChiaveFill(scelto);
        String tokenDelFill = null;
        for (String[] t : token) {
            if (t[0].equalsIgnoreCase(scelto[0]) || (fillScelto != null && fillScelto.equals(DerivatiLifo.ChiaveFill(t)))) {
                tokenDelFill = t[0];
                break;
            }
        }
        final String tokenScelto = tokenDelFill;
        long istante = Istante(scelto);
        Set<String> righeToken = new HashSet<>();
        long inizio = Long.MAX_VALUE;
        long fine = Long.MIN_VALUE;
        for (DerivatiLifo.Posizione p : DerivatiLifo.Calcola(token).values()) {
            int ultimo = -1;
            for (int e : p.righeEpisodio.keySet()) ultimo = Math.max(ultimo, e);
            for (Map.Entry<Integer, List<String>> e : p.righeEpisodio.entrySet()) {
                List<String> ids = e.getValue();
                if (ids.isEmpty()) continue;
                long da = Istante(mappa.get(ids.get(0)));
                //L'ultimo episodio ancora aperto non ha fine
                long a = e.getKey() == ultimo && p.contrattiAperti().signum() > 0 ? Long.MAX_VALUE : Istante(mappa.get(ids.get(ids.size() - 1)));
                boolean preso = tokenScelto != null ? ids.stream().anyMatch(x -> x.equalsIgnoreCase(tokenScelto))
                        : istante >= da && istante <= a;
                if (!preso) continue;
                righeToken.addAll(ids);
                inizio = Math.min(inizio, da);
                fine = Math.max(fine, a);
            }
        }
        if (righeToken.isEmpty()) return;
        Set<String> fill = new HashSet<>();
        for (String id : righeToken) {
            String[] t = mappa.get(id);
            if (t == null) continue;
            membri.putIfAbsent(t[0], Legame.POSIZIONE);
            String f = DerivatiLifo.ChiaveFill(t);
            if (f != null) fill.add(f);
        }
        for (String[] v : mappa.values()) {
            if (v == null) continue;
            if (fill.contains(DerivatiLifo.ChiaveFill(v))) {
                membri.putIfAbsent(v[0], Legame.POSIZIONE);
            } else if (contratto.equals(v.length > 21 ? OKXDerivati.ContrattoDaNota(v[21]) : null) && scelto[3].equals(v[3])) {
                long t = Istante(v);
                if (t >= inizio && t <= fine) membri.putIfAbsent(v[0], Legame.POSIZIONE);
            }
        }
    }

    // =================================================================================================
    // SPIEGAZIONE DI UNA RIGA
    // =================================================================================================

    private static boolean Vuoto(String s) {
        return s == null || s.isBlank();
    }

    private static String Num(String s) {
        if (!Funzioni.isNumeric(s, false)) return s == null ? "" : s;
        return new BigDecimal(s).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Euro con due decimali e virgola. */
    private static String Euro(BigDecimal b) {
        return b.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Cos'e' un movimento, in una riga. Ogni combinazione sconosciuta ripiega su campo 5 e classificazione: una riga non resta mai vuota.
     *
     * @param mappa i movimenti, per sapere se il fill di un profitto ha il token del contratto
     */
    static String Spiega(String[] v, Map<String, String[]> mappa) {
        String tipo = Derivati.Tipo(v);
        String c18 = v[18] == null ? "" : v[18].trim();
        boolean entra = !Vuoto(v[11]) && Vuoto(v[8]);
        boolean generata = "AU".equalsIgnoreCase(v[22]);
        Map<String, String> d = v.length > Derivati.CAMPO_DATI ? Derivati.Dati(v) : Map.of();

        if (tipo.equals(Derivati.CONTRATTO)) {
            boolean apre = "apre".equals(d.get("azione"));
            String verso = "short".equals(d.get("verso")) ? "short" : "long";
            String s = (apre ? "Apertura di " : "Chiusura di ") + Num(d.get("contratti")) + " contratti " + verso
                    + (Vuoto(d.get("prezzo")) ? "" : " al prezzo " + Num(d.get("prezzo"))) + " (token del contratto, nessun effetto sulle cripto)";
            if (apre) return s;
            String conguaglio = DerivatiLifo.TestoConguaglio(v);
            if (!conguaglio.isBlank()) return s + ". " + conguaglio;
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) return s + ". Reddito in LIFO: " + Euro(r) + " euro";
            return s + ". Reddito: il profitto dell'exchange, sulla riga del profitto della stessa chiusura";
        }
        if (tipo.equals(Derivati.MARGINE)) {
            boolean derivati = OKXDerivati.SOTTOWALLET.equals(v[4]);
            if (ContoDerivati.Attivo()) {
                //Opzione Conto derivati (P2): il giroconto ha effetto
                BigDecimal r = Derivati.Risultato(v);
                if (derivati) return entra ? "Margine entrato nel conto derivati: lì vale il suo valore di ingresso (opzione Conto derivati)"
                        : "Margine uscito dal conto derivati: reddito da derivati pari al valore meno quello di ingresso" + (r == null ? "" : ", " + Euro(r) + " euro");
                return entra ? "Margine rientrato dal conto derivati: entra fra le cripto-attività al valore del momento (opzione Conto derivati)"
                        : "Margine passato al conto derivati: cessione di cripto-attività al valore del momento (opzione Conto derivati)";
            }
            if (derivati) return (entra ? "Margine parcheggiato nel" : "Margine restituito dal") + " sotto-wallet Derivati (giroconto, nessun effetto fiscale)";
            return (entra ? "Margine tornato dalla posizione" : "Margine messo nella posizione") + " (giroconto con il sotto-wallet Derivati, nessun effetto fiscale)";
        }
        if (Principale_RientroMargine.IsRientro(v)) {
            return "Rientro del margine di una posizione aperta con la versione 1.0.66"
                    + (Vuoto(c18) ? ", da classificare: conta come deposito di cripto" : ", abbinato come sblocco di collaterale");
        }
        if (v[7] != null && v[7].startsWith(Principale_RientroMargine.CAUSALE_RESIDUO + "|")) {
            BigDecimal r = Derivati.Risultato(v);
            return "Margine non rientrato di una posizione aperta con la versione 1.0.66 (funding pagato prima dell'aggiornamento), "
                    + (entra ? "accredito" : "addebito") + " da derivati" + (r == null ? "" : ": " + Euro(r) + " euro");
        }
        if (generata && Principale_RientroMargine.WALLET_COLLATERALE.equals(v[4])
                && (Principale_RientroMargine.DESCRIZIONE_BLOCCO.equals(v[5]) || Principale_RientroMargine.DESCRIZIONE_SBLOCCO.equals(v[5]))) {
            return "Riga generata: margine " + (Principale_RientroMargine.DESCRIZIONE_BLOCCO.equals(v[5]) ? "bloccato" : "sbloccato") + " sul collaterale";
        }
        if (Principale_RientroMargine.DETTAGLIO_BLOCCO.equals(c18) && "Sell".equals(v[7])) {
            return "Prelievo del margine registrato dalla versione 1.0.66, abbinato come blocco di collaterale"
                    + (ContoDerivati.Attivo() ? ": con l'opzione Conto derivati è una cessione di cripto-attività" : " (il costo segue il margine)");
        }
        //Dual Investment: marcato, ma cripto-attivita' per scelta (decisione D5): si spiega come gli altri movimenti, sotto
        if (!tipo.isEmpty() && !Derivati.IsDual(v)) {
            String nome = switch (tipo) {
                case Derivati.PNL -> entra ? "Profitto realizzato" : "Perdita realizzata";
                case Derivati.FUNDING -> entra ? "Funding incassato" : "Funding pagato";
                case Derivati.COMMISSIONE -> "Commissione del derivato";
                case Derivati.BONUS -> entra ? "Bonus" : "Recupero del bonus";
                case Derivati.RIMBORSO_COMMISSIONI -> "Rimborso di commissioni";
                case Derivati.CONSEGNA -> "Regolamento alla scadenza";
                default -> "Movimento su derivati (" + tipo + ")";
            };
            if (!Derivati.HaMarcatore(v) && ContoDerivati.Attivo() && ContoDerivati.CalcolatoComeDerivato(v, mappa)) {
                BigDecimal r = Derivati.Risultato(v);
                return nome + ", da classificare ma sul conto derivati: con l'opzione Conto derivati conta già fra i redditi da derivati"
                        + (r == null ? "" : ", risultato " + Euro(r) + " euro");
            }
            if (!Derivati.HaMarcatore(v)) {
                return nome + ", da classificare: conta come cripto-attività e non fra i redditi da derivati";
            }
            BigDecimal r = Derivati.Risultato(v);
            if (tipo.equals(Derivati.PNL) && r != null && r.signum() == 0 && FillConToken(v, mappa)) {
                return nome + ": il reddito è quello in LIFO sulla riga di chiusura del token, qui resta il denaro accreditato o addebitato";
            }
            return nome + (r == null ? "" : ", risultato dei derivati " + Euro(r) + " euro");
        }

        //Movimenti che non sono derivati
        String s;
        if (c18.contains("Scambio Differito")) s = "Parte di uno scambio differito";
        else if (c18.startsWith("PTW")) s = "Trasferimento in uscita verso un wallet o un sotto-wallet proprio";
        else if (c18.startsWith("DTW")) s = "Trasferimento in entrata da un wallet o un sotto-wallet proprio";
        else if (CommissioniCollegate.isCommissione(v) && !Vuoto(CommissioniCollegate.Chiave(v))) s = "Commissione collegata a un movimento";
        else if (GruppoOperazione.isContrattoDual(GruppoOperazione.Chiave(v))) s = "Movimento di un contratto Dual Investment";
        else if (GruppoOperazione.Chiave(v).startsWith(FallimentiPiattaforme.PREFISSO_CHIAVE)) s = "Movimento di una procedura di fallimento";
        else s = (Vuoto(v[5]) ? "Movimento" : v[5].trim()) + (Vuoto(c18) ? " (da classificare)" : " (" + c18 + ")");
        if (Derivati.IsDual(v)) s += ", Dual Investment trattato come cripto-attività";
        return generata ? s + ", riga generata dalla classificazione" : s;
    }

    /** {@code true} se il fill del movimento ({@code [3]|[24]}) ha una riga di token del contratto. */
    private static boolean FillConToken(String[] v, Map<String, String[]> mappa) {
        String f = DerivatiLifo.ChiaveFill(v);
        if (f == null) return false;
        for (String[] t : mappa.values()) {
            if (t != null && t.length > Derivati.CAMPO_DATI && Derivati.Tipo(t).equals(Derivati.CONTRATTO) && f.equals(DerivatiLifo.ChiaveFill(t))) return true;
        }
        return false;
    }
}
