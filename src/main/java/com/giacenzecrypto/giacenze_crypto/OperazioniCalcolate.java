package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * L'<b>operazione</b> di cui fa parte un movimento, calcolata e mai salvata (2026-10-05, scelta dell'utente): due
 * movimenti stanno nella stessa operazione se sono legati, anche passando per altri, da uno di questi legami:
 * <ul>
 *   <li>{@code [20]}, il gruppo della classificazione (trasferimento, scambio differito, Vault, contratto Dual);</li>
 *   <li>{@code [43]}, la commissione collegata al suo movimento ({@link CommissioniCollegate});</li>
 *   <li>{@code [45]}, l'identità dell'operazione ({@link GruppoOperazione}, oggi il contratto Dual);</li>
 *   <li>{@code [24]}, lo stesso hash o id d'ordine sullo stesso exchange/wallet {@code [3]} entro un'ora, per i gruppi
 *       che la classificazione non crea (gamba speculare dell'import generico, transazioni DeFi ricevute con più
 *       token, righe dello stesso ordine). Sono esclusi i movimenti generati, i valori più corti di
 *       {@value #LUNGHEZZA_MINIMA_HASH} caratteri (sugli archivi reali {@code "-"} e {@code "_"} fanno da segnaposto a
 *       migliaia) e i gruppi di più di {@value #MAX_STESSO_HASH} movimenti, dove il valore non identifica una
 *       transazione.</li>
 * </ul>
 * Non si salva perché ogni legame sta già in un campo suo: un quarto dato andrebbe allineato a mano con gli altri tre.
 * Così il pregresso è coperto senza migrazione, e annullare una classificazione, cancellare o scollegare una
 * commissione aggiorna l'operazione da solo. Nessun calcolo la legge.
 *
 * <p>L'operazione si mostra con una chiave: l'identità {@code [45]} di un suo membro se c'è (per un contratto Dual
 * {@code DUAL-<id>}), altrimenti {@code OP-} e l'ID del primo movimento. Serve a vedere e filtrare e si ricalcola a ogni
 * caricamento: non va salvata né citata, perché l'ID del primo movimento cambia se quel movimento cambia ID.
 *
 * <p>Si calcola sull'intera mappa, mai sulle righe caricate in tabella: i filtri di caricamento tolgono righe, e
 * un'operazione calcolata solo su quelle si spezzerebbe (la stessa regola del costo di carico in "Giacenze a data").
 */
public final class OperazioniCalcolate {

    private OperazioniCalcolate() {
    }

    /** Prefisso della chiave di un'operazione senza identità propria. */
    public static final String PREFISSO = "OP-";

    /** Sotto questa lunghezza un valore di {@code [24]} è un segnaposto, non un hash né un id d'ordine. */
    static final int LUNGHEZZA_MINIMA_HASH = 4;

    /** Oltre questo numero di movimenti con lo stesso {@code [24]} il valore non identifica una transazione. */
    static final int MAX_STESSO_HASH = 20;

    /** Distanza massima fra movimenti con lo stesso {@code [24]} perché siano la stessa transazione. */
    static final long DISTANZA_MAX_HASH_S = 3600L;

    /** Union-find sulle righe stesse (identità degli oggetti in mappa), senza convertire gli ID. */
    private static final class Unione {
        final java.util.IdentityHashMap<String[], String[]> Padre = new java.util.IdentityHashMap<>();

        String[] Radice(String[] x) {
            String[] r = x;
            while (true) {
                String[] p = Padre.get(r);
                if (p == null || p == r) break;
                r = p;
            }
            //Compressione del cammino
            while (x != r) {
                String[] n = Padre.get(x);
                Padre.put(x, r);
                x = n;
            }
            return r;
        }

        void Unisci(String[] a, String[] b) {
            Padre.putIfAbsent(a, a);
            Padre.putIfAbsent(b, b);
            String[] ra = Radice(a), rb = Radice(b);
            if (ra != rb) Padre.put(ra, rb);
        }
    }

    private static boolean isGenerato(String[] v) {
        return v.length > 22 && v[22] != null && v[22].equalsIgnoreCase("AU");
    }

    /**
     * L'istante del movimento in secondi, dal prefisso {@code yyyyMMddHHmmss} dell'ID, con aritmetica sulle cifre: serve
     * solo a misurare distanze di un'ora, e la conversione con il formato di data costava quasi tutto il calcolo su un
     * archivio grande (centinaia di millisecondi su 100.000 movimenti). Il fuso non conta per una differenza.
     * @return i secondi, o 0 se l'ID non comincia con una data leggibile
     */
    static long Istante(String[] v) {
        String ID = v[0];
        if (ID == null || ID.length() < 14) return 0;
        for (int i = 0; i < 14; i++) if (!Character.isDigit(ID.charAt(i))) return 0;
        try {
            java.time.LocalDateTime t = java.time.LocalDateTime.of(Cifre(ID, 0, 4), Cifre(ID, 4, 6), Cifre(ID, 6, 8),
                    Cifre(ID, 8, 10), Cifre(ID, 10, 12), Cifre(ID, 12, 14));
            return t.toEpochSecond(java.time.ZoneOffset.UTC);
        } catch (java.time.DateTimeException ex) {
            return 0;
        }
    }

    private static int Cifre(String s, int da, int a) {
        int n = 0;
        for (int i = da; i < a; i++) n = n * 10 + (s.charAt(i) - '0');
        return n;
    }

    /**
     * Calcola l'operazione di ogni movimento.
     * @param Mappa tutti i movimenti, cioè la mappa intera e non le righe filtrate: i riferimenti di {@code [20]} si
     *        cercano qui, e una mappa che non distingue le maiuscole (come {@code MappaCryptoWallet}) li trova come li
     *        trova il resto del programma
     * @return per ogni ID di un movimento che fa parte di un'operazione la chiave dell'operazione; un movimento che
     *         non è legato a nessun altro e non ha un'identità {@code [45]} non compare
     */
    public static Map<String, String> Calcola(Map<String, String[]> Mappa) {
        Unione U = new Unione();
        Map<String, String[]> PrimoPerCommissione = new HashMap<>();
        Map<String, String[]> PrimoPerIdentita = new HashMap<>();
        Map<String, List<String[]>> PerHash = new HashMap<>();
        List<String[]> ConIdentita = new ArrayList<>();
        for (String[] v : Mappa.values()) {
            if (v == null || v[0] == null) continue;
            //[20]: ogni riferimento a un movimento che esiste, in entrambe le direzioni
            if (v.length > 20 && v[20] != null && !v[20].isEmpty()) {
                for (String ID : v[20].split(",")) {
                    if (ID.isBlank()) continue;
                    String[] a = Mappa.get(ID.trim());
                    if (a != null) U.Unisci(v, a);
                }
            }
            //[43] e [45]: stessa chiave, stessa operazione
            if (v.length > CommissioniCollegate.CAMPO && v[CommissioniCollegate.CAMPO] != null && !v[CommissioniCollegate.CAMPO].isEmpty()) {
                String Commissione = CommissioniCollegate.Chiave(v);
                if (!Commissione.isEmpty()) {
                    String[] Primo = PrimoPerCommissione.putIfAbsent(Commissione, v);
                    if (Primo != null) U.Unisci(v, Primo);
                }
            }
            if (v.length > GruppoOperazione.CAMPO && v[GruppoOperazione.CAMPO] != null && !v[GruppoOperazione.CAMPO].isEmpty()) {
                String Identita = GruppoOperazione.Chiave(v);
                if (!Identita.isEmpty()) {
                    ConIdentita.add(v);
                    String[] Primo = PrimoPerIdentita.putIfAbsent(Identita, v);
                    if (Primo != null) U.Unisci(v, Primo);
                }
            }
            //[24]: raccolti per exchange e valore, uniti sotto
            if (v.length > 24 && v[24] != null && v[24].length() >= LUNGHEZZA_MINIMA_HASH && !isGenerato(v)) {
                String H = v[24].trim();
                if (H.length() >= LUNGHEZZA_MINIMA_HASH) {
                    PerHash.computeIfAbsent(v[3] + "|" + H.toLowerCase(Locale.ROOT), x -> new ArrayList<>()).add(v);
                }
            }
        }
        for (List<String[]> Gruppo : PerHash.values()) {
            if (Gruppo.size() < 2 || Gruppo.size() > MAX_STESSO_HASH) continue;
            Gruppo.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a[0], b[0]));
            //Movimenti in ordine di tempo: un nuovo blocco quando ci si allontana più di un'ora dal primo del blocco
            String[] Inizio = Gruppo.get(0);
            long IstanteInizio = Istante(Inizio);
            for (int i = 1; i < Gruppo.size(); i++) {
                String[] v = Gruppo.get(i);
                long t = Istante(v);
                if (IstanteInizio > 0 && t > 0 && t - IstanteInizio <= DISTANZA_MAX_HASH_S) {
                    U.Unisci(v, Inizio);
                } else {
                    Inizio = v;
                    IstanteInizio = t;
                }
            }
        }
        //Un movimento con un'identità ma senza legami (un Purchase di un contratto annullato) è un'operazione da solo
        for (String[] v : ConIdentita) U.Padre.putIfAbsent(v, v);

        //Membri di ogni operazione, e la chiave da mostrare
        java.util.IdentityHashMap<String[], List<String[]>> Componenti = new java.util.IdentityHashMap<>();
        for (String[] v : new ArrayList<>(U.Padre.keySet())) Componenti.computeIfAbsent(U.Radice(v), x -> new ArrayList<>()).add(v);
        Map<String, String> Ris = new HashMap<>();
        for (List<String[]> Membri : Componenti.values()) {
            String Identita = null;
            String PrimoID = null;
            for (String[] m : Membri) {
                String I = GruppoOperazione.Chiave(m);
                if (!I.isEmpty() && (Identita == null || I.compareTo(Identita) < 0)) Identita = I;
                if (PrimoID == null || String.CASE_INSENSITIVE_ORDER.compare(m[0], PrimoID) < 0) PrimoID = m[0];
            }
            if (Membri.size() < 2 && Identita == null) continue;
            String Chiave = Identita != null ? Identita : PREFISSO + PrimoID;
            for (String[] m : Membri) Ris.put(m[0], Chiave);
        }
        return Ris;
    }

    /**
     * La chiave descritta per chi la legge, con il numero dei movimenti.
     * @return il testo, vuoto se la chiave è vuota
     */
    public static String Descrizione(String Chiave, int NumeroMovimenti) {
        if (Chiave == null || Chiave.isBlank()) return "";
        String Cosa = GruppoOperazione.isContrattoDual(Chiave) ? GruppoOperazione.Descrizione(Chiave) : "Operazione";
        return Cosa + " (" + NumeroMovimenti + (NumeroMovimenti == 1 ? " movimento)" : " movimenti)");
    }

    /** @return gli ID dei movimenti in mappa che hanno la chiave indicata, in ordine di ID */
    static List<String> Membri(Map<String, String> Operazioni, String Chiave) {
        Map<String, String> Ordinati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> e : Operazioni.entrySet()) {
            if (e.getValue().equals(Chiave)) Ordinati.put(e.getKey(), e.getKey());
        }
        return new ArrayList<>(Ordinati.keySet());
    }

    /**
     * Le righe del dettaglio di un movimento sull'operazione: gli altri movimenti dell'operazione, con quantità, moneta,
     * ID e cosa sono (tipo e classificazione), poi le commissioni collegate al movimento
     * ({@link Principale_CommissioniCollegate#RigheDettaglio}), che dicono a quale movimento appartiene una fee.
     * Prende il posto dell'elenco degli ID nudi di {@code [20]} ("Movimenti Correlati").
     * @return coppie {etichetta, valore HTML}
     */
    public static List<String[]> RigheDettaglio(String ID) {
        List<String[]> Ris = new ArrayList<>();
        String[] Mov = MappaCryptoWallet.get(ID);
        if (Mov != null) {
            Map<String, String> Operazioni = Calcola(MappaCryptoWallet);
            String K = Operazioni.get(Mov[0]);
            if (K != null) {
                List<String> Membri = Membri(Operazioni, K);
                StringBuilder Altri = new StringBuilder();
                for (String Altro : Membri) {
                    if (Altro.equalsIgnoreCase(Mov[0])) continue;
                    String[] v = MappaCryptoWallet.get(Altro);
                    if (v == null) continue;
                    if (Altri.length() > 0) Altri.append("<br>");
                    Altri.append(Principale_CommissioniCollegate.Descrizione(v));
                    if (!Funzioni.noData(v[5])) Altri.append(" - ").append(v[5]);
                    if (!Funzioni.noData(v[18])) Altri.append(" (").append(v[18]).append(")");
                }
                Ris.add(new String[]{Descrizione(K, Membri.size()),
                    Altri.length() > 0 ? "<html>" + Altri + "</html>" : "nessun altro movimento"});
            }
        }
        Ris.addAll(Principale_CommissioniCollegate.RigheDettaglio(ID));
        return Ris;
    }
}
