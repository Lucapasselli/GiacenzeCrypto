package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Controllo di coerenza dei movimenti collegati dalla classificazione (campo {@code [20]}) e riparazione degli
 * scambi differiti sovrascritti. Solo logica, nessun componente Swing: il conteggio gira nel thread delle tabelle
 * secondarie di {@code Principale} e alimenta il quinto contatore del pulsante Errori.
 *
 * <p>Ogni membro di un gruppo classificato porta in {@code [20]} l'elenco completo degli altri, quindi ogni
 * riferimento dovrebbe essere ricambiato e puntare a un movimento che esiste. Le eccezioni trovate sono di tre tipi:
 * <ul>
 *   <li><b>scambio differito sovrascritto</b> (bug C17): due scambi con lo stesso istante di deposito ricevevano lo
 *       stesso ID per lo scambio e per il trasferimento in uscita, e il secondo sovrascriveva il primo. Il prelievo
 *       "perdente" cita movimenti che citano un altro prelievo. Si ripara con {@link #RiparaScambioDifferito};</li>
 *   <li><b>contratto Dual Investment a moneta uguale nella forma precedente al 2026-10-05</b> (bug C18): i suoi
 *       {@code [20]} andavano in un verso solo per costruzione. Non è un errore dei dati ma va convertito, e lo fa
 *       {@link Binance_DualInvestment#AggiornaFormaStessaMoneta} ripassando il file di dettaglio;</li>
 *   <li><b>altri</b>: riferimenti a movimenti cancellati o non ricambiati per qualunque altra causa, da sistemare a
 *       mano annullando la classificazione.</li>
 * </ul>
 * Sugli archivi reali senza i due difetti il controllo non trova nulla: le forme legittime di {@code [20]} sono
 * tutte simmetriche.
 */
public final class MovimentiCollegati {

    private MovimentiCollegati() {
    }

    static final String CAMPO18_PRELIEVO_DIFFERITO = "PTW - Scambio Differito";
    static final String CAMPO18_DEPOSITO_DIFFERITO = "DTW - Scambio Differito";
    static final String WALLET_PIATTAFORMA_PREDEFINITO = "Piattaforma di scambio";

    /** Esito di {@link #RiparaScambioDifferito} e di {@link Binance_DualInvestment#AggiornaFormaStessaMoneta}. */
    enum EsitoRiparazione { NON_SERVE, RIPARATO, FALLITO }

    /** Risultato di {@link #Controlla}: ID dei movimenti, in ordine di ID. */
    public static final class Esito {
        /** Prelievi di scambi differiti sovrascritti, riparabili. */
        public final Set<String> ScambiSovrascritti = new LinkedHashSet<>();
        /** Settlement di contratti Dual a moneta uguale nella forma precedente: si convertono ripassando il file. */
        public final Set<String> DualFormaPrecedente = new LinkedHashSet<>();
        /** Altri movimenti con un {@code [20]} incoerente. */
        public final Set<String> Altri = new LinkedHashSet<>();

        public int Totale() {
            return ScambiSovrascritti.size() + DualFormaPrecedente.size() + Altri.size();
        }
    }

    private static String Chiave(String ID) {
        return ID.toLowerCase(Locale.ROOT);
    }

    private static List<String> Citati(String[] v) {
        List<String> Ris = new ArrayList<>();
        if (v == null || v.length <= 20 || v[20] == null) return Ris;
        for (String ID : v[20].split(",")) if (!ID.isBlank()) Ris.add(ID.trim());
        return Ris;
    }

    /** @return {@code true} se {@code v} cita l'ID indicato nel suo {@code [20]} (senza badare alle maiuscole, come la mappa) */
    private static boolean Cita(String[] v, String ID) {
        for (String c : Citati(v)) if (c.equalsIgnoreCase(ID)) return true;
        return false;
    }

    /**
     * Controlla i collegamenti {@code [20]} dei movimenti indicati. Non modifica nulla e non legge la mappa viva:
     * riceve la copia della collezione fatta sull'EDT, come gli altri calcoli delle tabelle secondarie, e legge solo
     * i campi 0, 4, 18, 20 e 22, che il motore delle plusvalenze non scrive.
     */
    static Esito Controlla(Collection<String[]> Movimenti) {
        Esito E = new Esito();
        //Indice dei soli movimenti collegati: quelli con un [20] e quelli citati da un [20]. Chiave in minuscolo,
        //perché la mappa dei movimenti non distingue le maiuscole; in ordine di ID, come la mappa
        Set<String> CitatiDaQualcuno = new HashSet<>();
        for (String[] v : Movimenti) {
            if (v != null) for (String ID : Citati(v)) CitatiDaQualcuno.add(Chiave(ID));
        }
        Map<String, String[]> Indice = new HashMap<>();
        TreeMap<String, String[]> Collegati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : Movimenti) {
            if (v == null || v[0] == null) continue;
            boolean HaRiferimenti = v.length > 20 && v[20] != null && !v[20].isBlank();
            if (!HaRiferimenti && !CitatiDaQualcuno.contains(Chiave(v[0]))) continue;
            Indice.put(Chiave(v[0]), v);
            Collegati.put(v[0], v);
        }
        Function<String, String[]> Trova = ID -> Indice.get(Chiave(ID));
        Collection<String[]> RigheCollegate = Collegati.values();

        //Movimenti i cui riferimenti sono già spiegati da uno scambio sovrascritto
        Set<String> Spiegati = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        //1. Scambi differiti: ogni membro deve esistere e citare il prelievo
        for (String[] p : RigheCollegate) {
            if (!CAMPO18_PRELIEVO_DIFFERITO.equals(p[18]) || "AU".equalsIgnoreCase(p[22])) continue;
            boolean Rotto = false;
            for (String ID : Citati(p)) {
                String[] m = Trova.apply(ID);
                if (m == null || !Cita(m, p[0])) Rotto = true;
            }
            if (!Rotto) continue;
            String[] s = DepositoDelPrelievo(p, Trova);
            if (s == null) continue; //senza il suo deposito non è riparabile: finisce fra gli altri
            E.ScambiSovrascritti.add(p[0]);
            //Le righe di questo scambio citano movimenti dell'altro: è lo stesso difetto, non un secondo
            Spiegati.add(p[0]);
            Spiegati.add(s[0]);
            for (String ID : Citati(p)) {
                String[] m = Trova.apply(ID);
                if (m != null && Cita(m, p[0])) Spiegati.add(m[0]);
            }
        }

        //2. Dual a moneta uguale nella forma precedente: Settlement il cui gruppo ha una gamba senza [20]
        //o una reward fuori dal sotto-wallet
        for (String[] s : RigheCollegate) {
            if (!Binance_DualInvestment.CAMPO18_SETTLEMENT_STESSA_MONETA.equals(s[18])) continue;
            boolean Precedente = false;
            for (String ID : Citati(s)) {
                String[] m = Trova.apply(ID);
                if (m == null) continue;
                if (Citati(m).isEmpty()) Precedente = true;
                if (Binance_DualInvestment.CAMPO18_REWARD.equals(m[18])
                        && !Binance_DualInvestment.WALLET_DUAL_SAVINGS.equals(m[4])) Precedente = true;
            }
            if (Precedente) E.DualFormaPrecedente.add(s[0]);
        }

        //3. Ogni altro riferimento a un movimento che non c'è o che non ricambia
        for (String[] v : RigheCollegate) {
            if (Spiegati.contains(v[0])) continue;
            for (String ID : Citati(v)) {
                String[] m = Trova.apply(ID);
                if (m == null) {
                    E.Altri.add(v[0]);
                    break;
                }
                if (Cita(m, v[0]) || FormaPrecedenteDual(v, m)) continue;
                E.Altri.add(v[0]);
                break;
            }
        }
        return E;
    }

    /**
     * I versi unici che l'abbinamento Dual a moneta uguale scriveva prima del 2026-10-05: Purchase/Settlement/reward
     * verso una gamba speculare {@code AU} sul sotto-wallet con {@code [20]} vuoto, e Settlement verso la sua reward.
     */
    private static boolean FormaPrecedenteDual(String[] v, String[] Citato) {
        boolean SpecularePrecedente = Binance_DualInvestment.WALLET_DUAL_SAVINGS.equals(Citato[4])
                && "AU".equalsIgnoreCase(Citato[22]) && Citati(Citato).isEmpty();
        boolean RewardPrecedente = Binance_DualInvestment.CAMPO18_REWARD.equals(Citato[18])
                && Binance_DualInvestment.CAMPO18_SETTLEMENT_STESSA_MONETA.equals(v[18]);
        return SpecularePrecedente || RewardPrecedente;
    }

    /** Il deposito dello scambio differito di un prelievo: il membro {@code DTW - Scambio Differito} che lo cita. */
    private static String[] DepositoDelPrelievo(String[] p, Function<String, String[]> Trova) {
        for (String ID : Citati(p)) {
            String[] m = Trova.apply(ID);
            if (m != null && CAMPO18_DEPOSITO_DIFFERITO.equals(m[18]) && !"AU".equalsIgnoreCase(m[22]) && Cita(m, p[0])) return m;
        }
        return null;
    }

    /** Una riparazione fatta da {@link #RiparaScambiSovrascritti}, per il resoconto all'utente. */
    public static final class Riparazione {
        public final String Prelievo;
        public final String Descrizione;
        public final EsitoRiparazione Esito;

        Riparazione(String Prelievo, String Descrizione, EsitoRiparazione Esito) {
            this.Prelievo = Prelievo;
            this.Descrizione = Descrizione;
            this.Esito = Esito;
        }
    }

    /**
     * Ripara sulla mappa viva gli scambi differiti sovrascritti dei prelievi indicati (da {@link Esito#ScambiSovrascritti}).
     * Il sotto-wallet dei movimenti ricostruiti è quello della loro entrata sulla piattaforma (MT1), che lo scambio
     * conserva: "Dual Savings" per i Dual Investment, "Piattaforma di scambio" per gli altri. Un contratto Dual riceve
     * di nuovo il suo gruppo {@code DUAL-} sui movimenti ricostruiti. Non ricalcola nulla: lo fa il chiamante, una volta.
     */
    static List<Riparazione> RiparaScambiSovrascritti(Collection<String> Prelievi) {
        List<Riparazione> Ris = new ArrayList<>();
        for (String ID : Prelievi) {
            String[] p = MappaCryptoWallet.get(ID);
            if (p == null) continue;
            String[] s = DepositoDelPrelievo(p, MappaCryptoWallet::get);
            if (s == null) continue;
            String Wallet = WALLET_PIATTAFORMA_PREDEFINITO;
            for (String c : Citati(p)) {
                String[] m = MappaCryptoWallet.get(c);
                if (m != null && "AU".equalsIgnoreCase(m[22]) && m[18].contains("DTW") && Cita(m, p[0]) && !m[4].isBlank()) Wallet = m[4];
            }
            String Descrizione = "scambio " + p[8] + " -> " + s[11] + " del " + s[1] + " (" + p[3] + ")";
            EsitoRiparazione E = RiparaScambioDifferito(p, s, Wallet);
            if (E == EsitoRiparazione.RIPARATO) {
                String Chiave = CommissioniCollegate.Chiave(p);
                if (CommissioniCollegate.isGruppoDual(Chiave)) Binance_DualInvestment.MarcaConChiave(Chiave, p, s);
            }
            Ris.add(new Riparazione(ID, Descrizione, E));
        }
        return Ris;
    }

    /**
     * Ricostruisce uno scambio differito i cui movimenti generati sono stati
     * sovrascritti da un altro scambio (bug C17: due scambi con lo stesso istante di deposito ricevevano lo
     * stesso ID per lo scambio e per il trasferimento in uscita). Il contratto "perdente" ha prelievo, MT1 e
     * deposito, ma il suo {@code [20]} cita uno scambio e un trasferimento che appartengono all'altro: quelli
     * citano l'altro prelievo, non il suo.
     *
     * <p>Si annullano solo i movimenti di questo contratto (prelievo, deposito e i generati che lo citano),
     * senza toccare quelli dell'altro, e si rifà lo scambio con
     * {@link GUI_ClassificazioneMovimento#CreaMovimentiScambioCryptoDifferito}, che ora genera ID univoci. Il
     * contratto "vincente" resta identico. Il prezzo dello scambio nuovo è calcolato come in qualunque
     * abbinamento: è un movimento che prima non c'era, e aggiunge una permuta all'anno del deposito.
     *
     * <p>Se lo scambio non si può ricreare tutto torna com'era: le righe originali vengono ricopiate negli
     * stessi oggetti, così chi le tiene in mano (il chiamante, per il gruppo del contratto) vede lo stato giusto.
     * Lavora solo sulle righe, senza il file di dettaglio.
     */
    static EsitoRiparazione RiparaScambioDifferito(String[] Purchase, String[] Settlement, String WalletPiattaforma) {
        if (Purchase == null || Settlement == null) return EsitoRiparazione.NON_SERVE;
        //Le righe di questo contratto: prelievo, deposito e i generati che citano il prelievo
        List<String[]> Proprie = new ArrayList<>(List.of(Purchase, Settlement));
        boolean Rotto = false;
        for (String[] r : new String[][]{Purchase, Settlement}) {
            for (String ID : r[20].split(",")) {
                if (ID.isBlank()) continue;
                String[] v = MappaCryptoWallet.get(ID.trim());
                if (v == null) {
                    Rotto = true;
                    continue;
                }
                if (v == Purchase || v == Settlement) continue;
                if (Cita(v, Purchase[0])) {
                    if (!Proprie.contains(v)) Proprie.add(v);
                } else {
                    Rotto = true;
                }
            }
        }
        if (!Rotto) return EsitoRiparazione.NON_SERVE;

        //Copia per il ripristino, con l'ID che la riga ha adesso
        List<String[]> Originali = new ArrayList<>();
        List<String[]> Copie = new ArrayList<>();
        for (String[] v : Proprie) {
            Originali.add(v);
            Copie.add(v.clone());
        }
        String[] Parti = new String[Proprie.size()];
        for (int i = 0; i < Parti.length; i++) Parti[i] = Proprie.get(i)[0];
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(Parti, Purchase[0]);
        //Purchase e Settlement sono gli stessi oggetti, con l'ID di partenza restituito dal ripristino
        if (GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(Purchase[0], Settlement[0], WalletPiattaforma)) {
            LoggerGC.logInfo("Scambio differito: ricostruito lo scambio differito di " + Purchase[0] + " / " + Settlement[0]);
            return EsitoRiparazione.RIPARATO;
        }
        //Ripristino: via le righe come le ha lasciate l'annullamento, dentro quelle di prima
        MappaCryptoWallet.remove(Purchase[0]);
        MappaCryptoWallet.remove(Settlement[0]);
        for (int i = 0; i < Originali.size(); i++) {
            String[] v = Originali.get(i);
            System.arraycopy(Copie.get(i), 0, v, 0, v.length);
            MappaCryptoWallet.put(v[0], v);
        }
        LoggerGC.ScriviErrore("Scambio differito: impossibile ricostruirlo per " + Purchase[0]
                + " / " + Settlement[0] + ", lasciato com'era");
        return EsitoRiparazione.FALLITO;
    }
}
