package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La voce "Classifica come derivato" del menu dei movimenti: porta i movimenti marcati come derivati
 * ({@link Derivati}, campo 44) da "da classificare" ad accredito ({@code DDV}) o addebito ({@code PDV}) per derivati,
 * così che il motore delle plusvalenze ne scriva il risultato nella sua colonna ({@link Derivati#CAMPO_RISULTATO})
 * e non fra le plusvalenze cripto. E il verso opposto, che li riporta da classificare.
 *
 * <p><b>Da sola solo per i movimenti nuovi.</b> La classificazione cambia i risultati dell'anno del movimento e dei successivi
 * (la moneta accreditata entra nel LIFO al valore del momento invece che a costo zero). Dal 17/11/2026 (decisione dell'utente)
 * un movimento <b>nuovo</b> marcato come derivato nasce già classificato ({@link #ClassificaAllImportazione}, chiamata
 * dall'importazione), salvo che abbia la data in un anno con una dichiarazione definitiva: allora il resoconto dell'importazione
 * chiede se classificarlo (decisione dell'utente del 18/11/2026, {@link #InAttesa}). Un movimento già in archivio si classifica solo con una scelta dell'utente, dopo un'anteprima
 * ({@link #Pianifica}), mai all'apertura dell'archivio: cambierebbe anni già dichiarati.
 *
 * <p><b>Solo movimenti già marcati.</b> Un deposito o prelievo senza il marcatore del campo 44 non si classifica
 * da qui: non c'è modo di distinguere un clic sbagliato da un derivato vero, e il marcatore messo per errore
 * resterebbe (con gli avvisi sui quadri). La marcatura di archivi importati prima dei marcatori è un lavoro a parte.
 *
 * <p><b>Le commissioni seguono il loro movimento</b>: una commissione marcata come derivato e collegata
 * ({@link CommissioniCollegate}, campo 43) a un movimento del piano entra nel piano anche se non era selezionata,
 * e riceve {@code PDV} senza cambiare il campo 5.
 *
 * <p>Segue lo schema delle altre estrazioni: nessun campo Swing, chi chiama mostra i dialoghi e aggiorna le tabelle.
 * Ogni modifica passa da {@link MovimentiStorico#AccodaModifica}, altrimenti sarebbe invisibile all'esportazione
 * delle modifiche manuali.
 */
public final class Principale_Derivati {

    private Principale_Derivati() {
    }

    /** Testo della voce del menu dei movimenti (le voci si trovano per testo). */
    public static final String VOCE_MENU = "Classifica come derivato";

    /** Voce di consultazione del menu dei movimenti: il riepilogo della posizione a cui appartiene un movimento di contratto o di margine. */
    public static final String VOCE_RIEPILOGO = "Riepilogo contratto derivato";

    /** @return {@code true} se il movimento e' una riga di contratto o di margine con i dati per risalire alla posizione */
    public static boolean RiepilogoDisponibile(String[] v) {
        return DerivatiLifo.ChiavePosizioneDaMovimento(v) != null;
    }

    /**
     * Apre il riepilogo della posizione del movimento: tutte le chiusure con il differenziale in LIFO e il profitto dell'exchange.
     * Sola consultazione.
     *
     * @param owner finestra proprietaria del dialogo
     * @param id ID del movimento
     * @return {@code true} se il dialogo si e' aperto
     */
    public static boolean ApriRiepilogo(java.awt.Window owner, String id) {
        String[] v = Principale.MappaCryptoWallet.get(id);
        String chiave = DerivatiLifo.ChiavePosizioneDaMovimento(v);
        if (chiave == null) return false;
        DerivatiLifo.Posizione p = DerivatiLifo.Calcola(Principale.MappaCryptoWallet.values()).get(chiave);
        if (p == null) return false;
        new GUI_RiepilogoContratto(owner, p).setVisible(true);
        return true;
    }

    /** Etichette delle operazioni nello storico modifiche (tradotte in {@code GUI_StoricoMovimento}). */
    static final String OP_CLASSIFICA = "ClassificaDerivato";
    static final String OP_ANNULLA = "AnnullaClassificazioneDerivato";

    /** Che cosa diventa un movimento del piano. */
    public enum Tipo {
        ACCREDITO, ADDEBITO, COMMISSIONE
    }

    /** Una riga dell'anteprima. */
    public static final class Voce {
        public final String id;
        public final Tipo tipo;
        /** Valore in euro del movimento ({@code v[15]}), zero se manca o non è numerico. */
        public final BigDecimal valore;
        /** {@code true} se il movimento non ha un valore in euro: il risultato sarebbe zero. */
        public final boolean senzaValore;
        /**
         * {@code true} per il profitto dell'exchange ({@code [44]} PNL) di un fill che ha la sua riga di token: il reddito di
         * quel fill è il differenziale in LIFO della chiusura del token ({@code DerivatiLifo}), quindi la voce resta fuori da
         * {@link Piano#risultato()}.
         */
        public final boolean conToken;

        Voce(String id, Tipo tipo, BigDecimal valore, boolean senzaValore, boolean conToken) {
            this.id = id;
            this.tipo = tipo;
            this.valore = valore;
            this.senzaValore = senzaValore;
            this.conToken = conToken;
        }
    }

    /** L'anteprima: che cosa verrebbe classificato, che cosa no e perché. */
    public static final class Piano {
        public final List<Voce> voci = new ArrayList<>();
        /** Per ogni ID selezionato che non si classifica, il motivo. */
        public final Map<String, String> scartati = new LinkedHashMap<>();
        /** ID già classificati come derivati fra quelli selezionati (si possono riportare da classificare). */
        public final List<String> giaClassificati = new ArrayList<>();

        public int conta(Tipo t) {
            int n = 0;
            for (Voce v : voci) if (v.tipo == t) n++;
            return n;
        }

        /** Somma dei valori in euro delle voci di un tipo, sempre positiva. */
        public BigDecimal totale(Tipo t) {
            BigDecimal s = BigDecimal.ZERO;
            for (Voce v : voci) if (v.tipo == t) s = s.add(v.valore);
            return s;
        }

        /**
         * Risultato dei derivati che il motore scriverebbe: accrediti meno addebiti meno commissioni, senza le voci di un fill
         * con il suo token ({@link Voce#conToken}), il cui reddito si calcola in LIFO sulla chiusura del token.
         */
        public BigDecimal risultato() {
            BigDecimal r = BigDecimal.ZERO;
            for (Voce v : voci) {
                if (v.conToken) continue;
                r = v.tipo == Tipo.ACCREDITO ? r.add(v.valore) : r.subtract(v.valore);
            }
            return r;
        }

        /** Quante voci sono il profitto dell'exchange di un fill con il suo token ({@link Voce#conToken}). */
        public int conToken() {
            int n = 0;
            for (Voce v : voci) if (v.conToken) n++;
            return n;
        }

        public int senzaValore() {
            int n = 0;
            for (Voce v : voci) if (v.senzaValore) n++;
            return n;
        }
    }

    private static String categoria(String[] v) {
        String[] parti = v[0].split("_");
        return parti.length > 4 ? parti[4].trim().toUpperCase() : "";
    }

    /**
     * @param v il movimento, anche {@code null}
     * @return {@code null} se il movimento si può classificare come derivato, altrimenti il motivo
     */
    static String MotivoNonClassificabile(String[] v) {
        if (v == null) return "movimento non trovato";
        if (!Derivati.isDerivato(v)) return "non è marcato come derivato";
        if (Derivati.IsDual(v)) return "è un Dual Investment, che il programma tratta come cripto-attività";
        if (Derivati.HaMarcatore(v)) return "è già classificato come derivato";
        if (Derivati.IsRigaContrattoOMargine(v)) return "è una riga di contratto o di margine, nate già classificate dall'import";
        if (TipoDaCategoria(categoria(v)) == null) return "non è un deposito, un prelievo o una commissione";
        if (v[18] != null && !v[18].isBlank()) return "ha già un'altra classificazione";
        if ("AU".equalsIgnoreCase(v[22])) return "è generato da una classificazione, va annullata quella";
        if (!Funzioni.noData(v[20])) return "è collegato ad altri movimenti";
        return null;
    }

    /**
     * Il tipo di classificazione dalla categoria dell'ID. Unica copia per la voce di menu e per l'importazione. Dal 20/11/2026 anche i
     * depositi e i prelievi FIAT ({@code DF}/{@code PF}): un derivato regolato in euro o in dollari. Una categoria sconosciuta non diventa
     * mai una commissione: un deposito trattato come commissione prenderebbe {@code PDV} e il suo provento diventerebbe un onere.
     *
     * @return {@code null} se la categoria non si classifica come derivato
     */
    static Tipo TipoDaCategoria(String cat) {
        return switch (cat) {
            case "DC", "DF" -> Tipo.ACCREDITO;
            case "PC", "PF" -> Tipo.ADDEBITO;
            case "CM" -> Tipo.COMMISSIONE;
            default -> null;
        };
    }

    /** @return {@code true} se la voce del menu ha senso per questo movimento: è marcato come derivato */
    public static boolean VoceAttiva(String[] v) {
        return v != null && Derivati.isDerivato(v);
    }

    /**
     * Costruisce l'anteprima per i movimenti selezionati, senza toccare nulla. Alle voci si aggiungono le commissioni
     * marcate come derivato e collegate (campo 43) a un movimento che entra nel piano.
     *
     * @param IDs ID dei movimenti selezionati
     */
    public static Piano Pianifica(Collection<String> IDs) {
        Piano piano = new Piano();
        Set<String> daValutare = new LinkedHashSet<>(IDs);
        Set<String> nelPiano = new LinkedHashSet<>();
        for (String id : daValutare) {
            String[] v = MappaCryptoWallet.get(id);
            if (v != null && Derivati.HaMarcatore(v)) {
                piano.giaClassificati.add(id);
                continue;
            }
            String motivo = MotivoNonClassificabile(v);
            if (motivo != null) {
                piano.scartati.put(id, motivo);
                continue;
            }
            nelPiano.add(id);
        }
        //Commissioni collegate ai movimenti del piano, anche se non selezionate
        for (String id : new ArrayList<>(nelPiano)) {
            String chiave = CommissioniCollegate.Chiave(MappaCryptoWallet.get(id));
            if (chiave == null || chiave.isBlank()) continue;
            for (String membro : CommissioniCollegate.Membri(chiave)) {
                String[] m = MappaCryptoWallet.get(membro);
                if (m != null && CommissioniCollegate.isCommissione(m) && MotivoNonClassificabile(m) == null) nelPiano.add(membro);
            }
        }
        Set<String> fillConToken = null;
        for (String id : nelPiano) {
            String[] v = MappaCryptoWallet.get(id);
            boolean conToken = false;
            if (Derivati.Tipo(v).equals(Derivati.PNL) && v[24] != null && !v[24].isBlank()) {
                if (fillConToken == null) fillConToken = FillConToken();
                conToken = fillConToken.contains(v[3] + "|" + v[24]);
            }
            Tipo tipo = TipoDaCategoria(categoria(v));
            BigDecimal valore = BigDecimal.ZERO;
            boolean senza = true;
            if (v[15] != null && Funzioni.isNumeric(v[15].trim(), false)) {
                valore = new BigDecimal(v[15].trim()).abs();
                senza = valore.signum() == 0;
            }
            piano.voci.add(new Voce(id, tipo, valore, senza, conToken));
        }
        return piano;
    }

    /** Exchange e fill ({@code [3]|[24]}) delle righe di token di un contratto nell'archivio. */
    private static Set<String> FillConToken() {
        Set<String> s = new java.util.HashSet<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (Derivati.Tipo(v).equals(Derivati.CONTRATTO) && v[24] != null && !v[24].isBlank()) s.add(v[3] + "|" + v[24]);
        }
        return s;
    }

    /**
     * Classifica i movimenti del piano. Ogni voce ricontrolla le sue condizioni (fra l'anteprima e la conferma
     * l'archivio può essere cambiato) e, se non valgono più, viene saltata.
     *
     * @return quanti movimenti sono stati classificati. Chi chiama ricalcola una volta sola alla fine
     */
    public static int Applica(Piano piano) {
        List<String> fatti = new ArrayList<>();
        for (Voce voce : piano.voci) {
            String[] v = MappaCryptoWallet.get(voce.id);
            if (MotivoNonClassificabile(v) != null) continue;
            String prima = Importazioni.SerializzaRiga(v);
            ImpostaClassificazione(v, voce.tipo);
            MovimentiStorico.AccodaModifica(MovimentiStorico.AssicuraLignaggio(v), voce.id, voce.id, prima, OP_CLASSIFICA);
            fatti.add(voce.id);
        }
        if (!fatti.isEmpty()) LoggerGC.logInfo("Classificati come derivati: " + String.join(", ", fatti));
        return fatti.size();
    }

    /**
     * Campi 5 e 18 di un movimento classificato come derivato. Unica copia, per la voce di menu e per l'importazione. Un deposito o
     * prelievo FIAT tiene il suo campo 5 ("DEPOSITO FIAT", "PRELIEVO FIAT"), come una commissione: lo legge la riconciliazione dei
     * movimenti Binance ({@code RiconciliazioneBinance}), e cambiarlo farebbe rientrare la riga come doppione.
     */
    private static void ImpostaClassificazione(String[] v, Tipo tipo) {
        String cat = categoria(v);
        if (cat.equals("DF") || cat.equals("PF")) {
            v[18] = tipo == Tipo.ACCREDITO ? Derivati.CAMPO18_ACCREDITO : Derivati.CAMPO18_ADDEBITO;
            return;
        }
        switch (tipo) {
            case ACCREDITO -> {
                v[5] = Derivati.CAMPO5_ACCREDITO;
                v[18] = Derivati.CAMPO18_ACCREDITO;
            }
            case ADDEBITO -> {
                v[5] = Derivati.CAMPO5_ADDEBITO;
                v[18] = Derivati.CAMPO18_ADDEBITO;
            }
            default -> v[18] = Derivati.CAMPO18_ADDEBITO;   //commissione: il campo 5 resta quello delle commissioni
        }
    }

    /**
     * Tipi di derivato che l'importazione classifica da sé ({@link #ClassificaAllImportazione}): profitto o perdita, funding,
     * commissioni, rimborsi di commissioni, regolamento alla scadenza, bonus e loro recupero (decisione D13 del 17/11/2026: il
     * bonus è un provento, il recupero un onere, così il bonus consumato compensa gli oneri che ha pagato al posto dell'utente).
     * Restano da classificare a mano i Dual Investment (hanno il loro abbinamento) e le righe di contratto e di margine (già
     * classificate).
     */
    static final Set<String> TIPI_CLASSIFICATI_ALL_IMPORTAZIONE = Set.of(Derivati.PNL, Derivati.FUNDING, Derivati.COMMISSIONE,
            Derivati.RIMBORSO_COMMISSIONI, Derivati.CONSEGNA, Derivati.BONUS);

    /**
     * Classifica come derivato un movimento appena importato, se è marcato con un tipo di {@link #TIPI_CLASSIFICATI_ALL_IMPORTAZIONE}
     * e si può classificare ({@link #MotivoNonClassificabile}): deposito {@code DDV}, prelievo {@code PDV}, commissione {@code PDV},
     * come la voce del menu. Nessuna voce di storico: è la forma con cui il movimento nasce, non una modifica a mano.
     * Lo chiama solo {@code Importazioni.ScriviListaSuMappaCrypto}, per i movimenti nuovi (decisione dell'utente del 17/11/2026):
     * i movimenti già in archivio si classificano solo con la voce del menu, perché cambierebbero anni già dichiarati.
     *
     * @param v il movimento appena inserito
     * @return {@code true} se è stato classificato
     */
    static boolean ClassificaAllImportazione(String[] v) {
        return ClassificaAllImportazione(v, Set.of());
    }

    /**
     * Come {@link #ClassificaAllImportazione(String[])}; un movimento con la data in uno degli {@code anniBloccati} resta da
     * classificare e va fra quelli in attesa della scelta dell'utente ({@link #InAttesa}).
     */
    private static boolean ClassificaAllImportazione(String[] v, Set<String> anniBloccati) {
        if (v == null || !TIPI_CLASSIFICATI_ALL_IMPORTAZIONE.contains(Derivati.Tipo(v)) || MotivoNonClassificabile(v) != null) return false;
        //Difesa: un movimento con uno storico, modificato o inserito a mano, o generato da una classificazione non e' un'importazione
        if (v.length > MovimentiStorico.CAMPO_LIGNAGGIO && !Funzioni.noData(v[MovimentiStorico.CAMPO_LIGNAGGIO])) return false;
        if ("M".equalsIgnoreCase(v[22]) || "AU".equalsIgnoreCase(v[22]) || (v.length > 39 && "E".equalsIgnoreCase(v[39]))) return false;
        if (v[1] != null && v[1].length() >= 4 && anniBloccati.contains(v[1].substring(0, 4))) {
            synchronized (IN_ATTESA) {
                IN_ATTESA.add(v[0]);
            }
            return false;
        }
        ImpostaClassificazione(v, TipoDaCategoria(categoria(v)));
        return true;
    }

    /**
     * Come {@link #ClassificaAllImportazione(String[])} per i movimenti nuovi di una scrittura. Prima i movimenti, poi le commissioni:
     * una commissione collegata ({@link CommissioniCollegate}, campo 43) si classifica solo se almeno uno dei suoi movimenti, di
     * questa importazione o già in archivio, è classificato (una commissione di un Dual Investment o di un bonus resta da classificare con il suo
     * movimento), come fa la voce di menu. Un movimento con la data in un anno con una dichiarazione definitiva
     * ({@link #AnniConDefinitiva}) resta da classificare e aspetta la scelta dell'utente nel resoconto dell'importazione
     * ({@link #InAttesa}).
     *
     * @param righe i movimenti appena inseriti
     * @return quanti sono stati classificati
     */
    static int ClassificaAllImportazione(List<String[]> righe) {
        //L'elenco delle dichiarazioni si legge dal disco: solo se c'e' almeno un derivato da classificare
        if (righe.stream().noneMatch(v -> TIPI_CLASSIFICATI_ALL_IMPORTAZIONE.contains(Derivati.Tipo(v)))) return 0;
        return ClassificaAllImportazione(righe, AnniConDefinitiva.get());
    }

    private static int ClassificaAllImportazione(List<String[]> righe, Set<String> anniBloccati) {
        int n = 0;
        Map<String, List<String[]>> perChiave = new LinkedHashMap<>();
        for (String[] v : righe) {
            if (CommissioniCollegate.isCommissione(v)) continue;
            if (ClassificaAllImportazione(v, anniBloccati)) n++;
            String chiave = CommissioniCollegate.Chiave(v);
            if (!chiave.isBlank()) perChiave.computeIfAbsent(chiave, k -> new ArrayList<>()).add(v);
        }
        //Chiavi di commissioni i cui movimenti non sono in questa scrittura: si cercano nell'archivio, una passata sola
        Set<String> daCercare = new java.util.HashSet<>();
        for (String[] v : righe) {
            String chiave = CommissioniCollegate.isCommissione(v) ? CommissioniCollegate.Chiave(v) : "";
            if (!chiave.isBlank() && !perChiave.containsKey(chiave)) daCercare.add(chiave);
        }
        if (!daCercare.isEmpty() && Principale.MappaCryptoWallet != null) {
            for (String[] v : Principale.MappaCryptoWallet.values()) {
                if (v == null || CommissioniCollegate.isCommissione(v)) continue;
                String chiave = CommissioniCollegate.Chiave(v);
                if (daCercare.contains(chiave)) perChiave.computeIfAbsent(chiave, k -> new ArrayList<>()).add(v);
            }
        }
        for (String[] v : righe) {
            if (!CommissioniCollegate.isCommissione(v)) continue;
            List<String[]> collegati = perChiave.get(CommissioniCollegate.Chiave(v));
            if (collegati != null && collegati.stream().noneMatch(Derivati::HaMarcatore)) continue;
            if (ClassificaAllImportazione(v, anniBloccati)) n++;
        }
        return n;
    }

    /**
     * Anni con una dichiarazione definitiva archiviata, di qualunque quadro ({@link Dichiarazioni}): un movimento nuovo con la
     * data in uno di questi anni non nasce classificato (decisione dell'utente del 18/11/2026). Serve a chi cancella e riscarica i
     * movimenti di un exchange: le righe riscaricate sono nuove e cambierebbero da sole i valori di anni già dichiarati.
     * Sostituibile nei test.
     */
    static java.util.function.Supplier<Set<String>> AnniConDefinitiva = () -> {
        Set<String> anni = new java.util.TreeSet<>();
        for (Dichiarazioni.Dichiarazione d : Dichiarazioni.Elenco()) {
            if (d.Definitiva && d.Anno != null) anni.add(d.Anno.trim());
        }
        return anni;
    };

    /** ID dei movimenti nuovi lasciati da classificare perché in un anno con una definitiva, in attesa della scelta dell'utente. */
    private static final Set<String> IN_ATTESA = new LinkedHashSet<>();

    /**
     * I movimenti lasciati da classificare all'importazione perché hanno la data in un anno con una dichiarazione definitiva,
     * ancora nell'archivio e ancora da classificare. Li mostra il resoconto dell'importazione, che chiede se classificarli
     * ({@link #ClassificaInAttesa}) o lasciarli così ({@link #ScartaInAttesa}). Restano in attesa fra un'importazione e l'altra
     * finché un resoconto non li mostra (un import di più file ha un solo resoconto alla fine).
     */
    static List<String[]> InAttesa() {
        List<String[]> righe = new ArrayList<>();
        synchronized (IN_ATTESA) {
            for (String id : IN_ATTESA) {
                String[] v = MappaCryptoWallet.get(id);
                if (v != null && Derivati.isDerivato(v) && !Derivati.HaMarcatore(v)) righe.add(v);
            }
        }
        return righe;
    }

    /** @return gli anni dei movimenti in attesa ({@link #InAttesa}), in ordine */
    static Set<String> AnniInAttesa() {
        Set<String> anni = new java.util.TreeSet<>();
        for (String[] v : InAttesa()) anni.add(v[1].substring(0, 4));
        return anni;
    }

    /**
     * Classifica i movimenti in attesa con le stesse regole dell'importazione, come se l'anno non avesse una definitiva: è la
     * forma con cui sarebbero nati, quindi nessuna voce di storico. Le commissioni collegate seguono il loro movimento.
     * Chi chiama fa ricalcolare.
     *
     * @return quanti movimenti sono stati classificati
     */
    static int ClassificaInAttesa() {
        List<String[]> righe = InAttesa();
        ScartaInAttesa();
        //Le commissioni collegate non sono fra quelli in attesa (all'import hanno seguito il loro movimento, rimasto da classificare)
        Set<String> presi = new java.util.HashSet<>();
        for (String[] v : righe) presi.add(v[0]);
        for (String[] v : new ArrayList<>(righe)) {
            String chiave = CommissioniCollegate.Chiave(v);
            if (chiave.isBlank()) continue;
            for (String membro : CommissioniCollegate.Membri(chiave)) {
                String[] m = MappaCryptoWallet.get(membro);
                if (m != null && CommissioniCollegate.isCommissione(m) && Derivati.isDerivato(m) && !Derivati.HaMarcatore(m)
                        && presi.add(membro)) righe.add(m);
            }
        }
        return ClassificaAllImportazione(righe, Set.of());
    }

    /** Lascia da classificare i movimenti in attesa: restano come i movimenti già in archivio, per la voce di menu. */
    static void ScartaInAttesa() {
        synchronized (IN_ATTESA) {
            IN_ATTESA.clear();
        }
    }

    /**
     * Riporta da classificare i movimenti indicati che sono classificati come derivati, con le commissioni collegate.
     * Il marcatore del campo 44 resta: dice che cosa è il movimento, non come è trattato.
     *
     * @return quanti movimenti sono tornati da classificare
     */
    public static int Annulla(Collection<String> IDs) {
        Set<String> tutti = new LinkedHashSet<>();
        for (String id : IDs) {
            String[] v = MappaCryptoWallet.get(id);
            if (v == null || !Derivati.HaMarcatore(v)) continue;
            tutti.add(id);
            String chiave = CommissioniCollegate.Chiave(v);
            if (chiave == null || chiave.isBlank()) continue;
            for (String membro : CommissioniCollegate.Membri(chiave)) {
                String[] m = MappaCryptoWallet.get(membro);
                if (m != null && CommissioniCollegate.isCommissione(m) && Derivati.HaMarcatore(m)) tutti.add(membro);
            }
        }
        List<String> fatti = new ArrayList<>();
        for (String id : tutti) {
            String[] v = MappaCryptoWallet.get(id);
            String prima = Importazioni.SerializzaRiga(v);
            //Il campo 5 di un deposito o prelievo torna quello di un movimento non classificato, come fa la
            //classificazione manuale quando si sceglie "nessuna". Quello di una commissione non era stato toccato
            if (!CommissioniCollegate.isCommissione(v)) v[5] = Importazioni.RitornaTipologiaTransazione(v[9], v[12], 1);
            v[18] = "";
            if (v.length > Derivati.CAMPO_RISULTATO) v[Derivati.CAMPO_RISULTATO] = "";
            MovimentiStorico.AccodaModifica(MovimentiStorico.AssicuraLignaggio(v), id, id, prima, OP_ANNULLA);
            fatti.add(id);
        }
        if (!fatti.isEmpty()) LoggerGC.logInfo("Derivati riportati da classificare: " + String.join(", ", fatti));
        return fatti.size();
    }

    /**
     * Testo dell'anteprima per il dialogo di conferma. Dice l'effetto, non come trattare il caso.
     *
     * @param piano l'anteprima
     * @return il testo, su più righe
     */
    public static String TestoAnteprima(Piano piano) {
        StringBuilder t = new StringBuilder();
        if (!piano.voci.isEmpty()) {
            t.append("Movimenti che verranno classificati come derivati: ").append(piano.voci.size()).append("\n");
            riga(t, "Accrediti", piano.conta(Tipo.ACCREDITO), piano.totale(Tipo.ACCREDITO), "");
            riga(t, "Addebiti", piano.conta(Tipo.ADDEBITO), piano.totale(Tipo.ADDEBITO), "-");
            riga(t, "Commissioni", piano.conta(Tipo.COMMISSIONE), piano.totale(Tipo.COMMISSIONE), "-");
            t.append("Risultato dei derivati: ").append(euro(piano.risultato())).append(" euro\n\n");
            t.append("Il risultato esce dalle plusvalenze delle cripto-attività e va nel risultato dei derivati. ")
             .append("La moneta accreditata entra nel calcolo al valore del momento, quella addebitata ne esce ")
             .append("come una cessione (gli euro e le altre valute FIAT no: per loro c'è solo il risultato dei derivati). ")
             .append("Cambiano le plusvalenze e le giacenze dell'anno di questi movimenti e degli anni successivi.\n");
            if (piano.conToken() > 0) {
                t.append("\nMovimenti di un contratto con il suo token (derivati OKX): ").append(piano.conToken())
                 .append(". Il loro reddito è il differenziale in LIFO scritto sulla chiusura del token (o il profitto ")
                 .append("dell'exchange, se la posizione non si ricostruisce dai prezzi) e non è compreso nel risultato sopra.\n");
            }
            if (piano.senzaValore() > 0) {
                t.append("\nMovimenti con valore zero in euro: ").append(piano.senzaValore())
                 .append(" (importi sotto il centesimo, oppure senza prezzo). Il loro risultato è zero.\n");
            }
        }
        if (!piano.giaClassificati.isEmpty()) {
            t.append(t.length() > 0 ? "\n" : "").append("Già classificati come derivati: ")
             .append(piano.giaClassificati.size()).append(".\n");
        }
        if (!piano.scartati.isEmpty()) {
            t.append(t.length() > 0 ? "\n" : "").append("Non classificabili: ").append(piano.scartati.size()).append("\n");
            int mostrati = 0;
            for (Map.Entry<String, String> e : piano.scartati.entrySet()) {
                if (mostrati++ == 10) {
                    t.append("... e altri ").append(piano.scartati.size() - 10).append("\n");
                    break;
                }
                t.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        return t.toString().trim();
    }

    private static void riga(StringBuilder t, String nome, int quanti, BigDecimal totale, String segno) {
        if (quanti == 0) return;
        String importo = euro(totale);
        //Niente "-0.00": un totale che arrotonda a zero si scrive senza segno
        t.append(nome).append(": ").append(quanti).append(", ").append(importo.equals("0.00") ? "" : segno).append(importo).append(" euro\n");
    }

    private static String euro(BigDecimal b) {
        return b.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
