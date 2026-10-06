package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Collegamento fra le commissioni e i movimenti a cui appartengono, nel campo {@value #CAMPO} del
 * movimento.
 *
 * <p><b>Il collegamento è una chiave di gruppo, non un puntatore.</b> Lo stesso valore opaco è scritto
 * sulla commissione (o sulle commissioni) e sul movimento (o sui movimenti) che l'hanno generata. Un ID
 * non andrebbe bene: cambia in Trasla Orario, in Modifica Movimento, nella classificazione, nello scambio
 * differito e in Separa/Unisci, e ognuno di quei punti dovrebbe ricordarsi di riscrivere il riferimento.
 * La chiave invece viaggia con la riga, e copre da sola i casi 1:1, una commissione su più movimenti (il
 * gas di una transazione DeFi con più token) e più commissioni su un movimento.
 *
 * <p><b>È un dato solo informativo.</b> Nessun motore di calcolo lo legge, e per questo il campo resta
 * fuori da {@code Calcoli_PlusvalenzeNew.Impronta} e dal golden master. Servirà per i derivati, dove le
 * commissioni vanno dedotte; per lo spot non si deducono e il {@code CM} resta una cessione a sé.
 *
 * <p>Una commissione è un movimento di categoria {@code CM}. Le commissioni sui trasferimenti create dalla
 * classificazione ({@code CM} marcato {@code AU}, legato al prelievo via {@code [20]}) non ricevono la
 * chiave, per scelta. Analisi completa in {@code nocommit/Documentazione/Analisi_Commissioni_Collegate.md}.
 *
 * <p>Il campo porta <b>solo</b> commissioni. Dal 2026-10-02 al 2026-10-05 portava anche la chiave dei contratti
 * Dual Investment ({@code DUAL-<id>}), che ora sta in un campo suo ({@link GruppoOperazione}): nello stesso campo una
 * commissione collegata al Purchase veniva fusa nel gruppo del contratto e perdeva il movimento a cui apparteneva. Le
 * righe di quel periodo vengono spostate al caricamento ({@link GruppoOperazione#MigraAllaFormaAttuale}).
 */
public final class CommissioniCollegate {

    /** Posizione della chiave di gruppo nella riga del movimento. */
    public static final int CAMPO = 43;

    private CommissioniCollegate() {
    }

    /** @return la chiave di gruppo del movimento, stringa vuota se non ne ha */
    public static String Chiave(String[] v) {
        if (v == null || v.length <= CAMPO || v[CAMPO] == null) return "";
        return v[CAMPO].trim();
    }

    /** @return {@code true} se il movimento è una commissione, cioè ha categoria {@code CM} */
    public static boolean isCommissione(String[] v) {
        if (v == null || v[0] == null) return false;
        String[] Parti = v[0].split("_");
        return Parti.length > 4 && Parti[4].trim().equalsIgnoreCase("CM");
    }

    /**
     * Una chiave nuova. Casuale e non derivata dall'ID del movimento: due movimenti costruiti nello stesso
     * secondo possono nascere con lo stesso ID prima che {@code CreaListaConIDUnivoco} li distingua, e una
     * chiave derivata dall'ID fonderebbe i loro due gruppi.
     */
    static String NuovaChiave() {
        return UUID.randomUUID().toString();
    }

    private static void Scrivi(String[] v, String Chiave) {
        if (v != null && v.length > CAMPO) v[CAMPO] = Chiave;
    }

    /**
     * La chiave descritta per chi la legge: un gruppo di commissioni dice quanti movimenti contiene (la chiave
     * stessa è un UUID, illeggibile).
     * @return il testo, vuoto se la chiave è vuota
     */
    public static String Descrizione(String Chiave) {
        if (Chiave == null || Chiave.isBlank()) return "";
        int n = Membri(Chiave).size();
        return "Gruppo commissioni (" + n + (n == 1 ? " movimento)" : " movimenti)");
    }

    /**
     * Collega le commissioni ai movimenti principali indicati. Non fa nulla se manca una delle due parti:
     * una commissione senza movimento (transazione fallita, commissione a sé nel CSV) resta senza chiave.
     * Se una delle righe ha già una chiave la si riusa, altrimenti se ne crea una.
     *
     * @param Principali movimenti a cui le commissioni appartengono; le commissioni eventualmente presenti
     *                   in questa lista vengono ignorate
     * @param Commissioni righe di commissione; quelle che non sono {@code CM} vengono ignorate
     */
    public static void Collega(Collection<String[]> Principali, Collection<String[]> Commissioni) {
        Collega(Principali, Commissioni, null);
    }

    /**
     * Come {@link #Collega(Collection, Collection)}. Se le righe portano già chiavi <b>diverse</b> i gruppi
     * vengono fusi, non spezzati: sovrascrivere la chiave di un movimento lascerebbe la sua vecchia
     * commissione con una chiave che nessun movimento porta più. La chiave sostituita viene quindi
     * riscritta anche sulle righe di {@code Ambito} (le altre righe della stessa importazione, non ancora
     * in mappa) e sui movimenti già in mappa.
     *
     * @param Ambito righe non ancora in mappa su cui propagare la fusione, può essere {@code null}
     */
    public static void Collega(Collection<String[]> Principali, Collection<String[]> Commissioni,
            Collection<String[]> Ambito) {
        if (Principali == null || Commissioni == null) return;
        List<String[]> P = new ArrayList<>();
        for (String[] v : Principali) if (v != null && !isCommissione(v)) P.add(v);
        List<String[]> C = new ArrayList<>();
        for (String[] v : Commissioni) if (isCommissione(v)) C.add(v);
        if (P.isEmpty() || C.isEmpty()) return;

        Set<String> Esistenti = new LinkedHashSet<>();
        for (String[] v : P) if (!Chiave(v).isEmpty()) Esistenti.add(Chiave(v));
        for (String[] v : C) if (!Chiave(v).isEmpty()) Esistenti.add(Chiave(v));
        String K = Esistenti.isEmpty() ? NuovaChiave() : Esistenti.iterator().next();
        for (String[] v : P) Scrivi(v, K);
        for (String[] v : C) Scrivi(v, K);
        if (Esistenti.size() > 1) {
            if (Ambito != null) {
                for (String[] v : Ambito) if (Esistenti.contains(Chiave(v))) Scrivi(v, K);
            }
            FondiChiavi(K, Esistenti);
        }
    }

    /** Come {@link #Collega(Collection, Collection)} per un solo movimento principale. */
    public static void Collega(String[] Principale, String[]... Commissioni) {
        if (Principale == null || Commissioni == null) return;
        List<String[]> C = new ArrayList<>();
        for (String[] v : Commissioni) if (v != null) C.add(v);
        //Non List.of(Principale): con un solo array come argomento lo prenderebbe per i varargs
        List<String[]> P = new ArrayList<>();
        P.add(Principale);
        Collega(P, C);
    }

    /**
     * Collega tutte le commissioni di un gruppo di righe che descrivono una sola operazione (le righe di
     * una transazione DeFi, di un ordine) a tutti gli altri movimenti del gruppo.
     */
    public static void CollegaGruppo(List<String[]> Righe) {
        CollegaGruppo(Righe, null);
    }

    /**
     * Come {@link #CollegaGruppo(List)}, ma se {@code Preferiti} contiene almeno un movimento non di
     * commissione le commissioni del gruppo vanno solo a quelli. Serve agli import che raggruppano le righe
     * per istante: le commissioni di Binance sono commissioni di scambio, e vanno sullo scambio ricostruito
     * e non su un reward capitato nello stesso secondo.
     */
    public static void CollegaGruppo(List<String[]> Righe, List<String[]> Preferiti) {
        if (Righe == null || Righe.isEmpty()) return;
        List<String[]> Principali = new ArrayList<>();
        if (Preferiti != null) {
            for (String[] v : Preferiti) if (v != null && !isCommissione(v)) Principali.add(v);
        }
        if (Principali.isEmpty()) {
            for (String[] v : Righe) if (v != null && !isCommissione(v)) Principali.add(v);
        }
        Collega(Principali, Righe, Righe);
    }

    /** @return gli ID dei movimenti in mappa che portano la chiave indicata, in ordine di ID */
    public static Set<String> Membri(String Chiave) {
        Set<String> Ris = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (Chiave == null || Chiave.isBlank()) return Ris;
        for (Map.Entry<String, String[]> e : MappaCryptoWallet.entrySet()) {
            if (Chiave.equals(Chiave(e.getValue()))) Ris.add(e.getKey());
        }
        return Ris;
    }

    /**
     * Riscrive con {@code Nuova} la chiave di tutti i movimenti in mappa che portano una delle chiavi
     * {@code Vecchie}: è la fusione di più gruppi in uno, usata quando si uniscono movimenti che avevano
     * commissioni diverse.
     */
    public static void FondiChiavi(String Nuova, Collection<String> Vecchie) {
        if (Nuova == null || Nuova.isBlank() || Vecchie == null) return;
        Set<String> DaSostituire = new java.util.HashSet<>();
        for (String k : Vecchie) if (k != null && !k.isBlank() && !k.equals(Nuova)) DaSostituire.add(k);
        if (DaSostituire.isEmpty()) return;
        for (String[] v : MappaCryptoWallet.values()) {
            if (DaSostituire.contains(Chiave(v))) Scrivi(v, Nuova);
        }
    }

    /**
     * Le commissioni che resterebbero senza movimento se si cancellassero gli ID indicati: quelle il cui
     * gruppo non ha più nessun movimento principale fuori dall'elenco. Una commissione condivisa con un
     * movimento che resta non è orfana. I movimenti generati automaticamente ({@code [22] = "AU"}, come lo
     * scambio sintetico di uno scambio differito) non contano come principali, perché la cancellazione
     * della classificazione li porta via insieme al movimento.
     *
     * @param DaCancellare ID dei movimenti che si stanno cancellando
     * @return ID delle commissioni orfane, non comprese in {@code DaCancellare}, in ordine di ID
     */
    public static Set<String> CommissioniOrfane(Collection<String> DaCancellare) {
        Set<String> Ris = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (DaCancellare == null || DaCancellare.isEmpty()) return Ris;
        Set<String> Cancellati = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Cancellati.addAll(DaCancellare);

        //Chiavi dei movimenti principali che si stanno cancellando
        Set<String> Chiavi = new LinkedHashSet<>();
        for (String ID : Cancellati) {
            String[] v = MappaCryptoWallet.get(ID);
            if (v != null && !isCommissione(v) && !Chiave(v).isEmpty()) Chiavi.add(Chiave(v));
        }
        if (Chiavi.isEmpty()) return Ris;

        //Una sola passata sulla mappa per tutte le chiavi
        Set<String> ConPrincipaleSuperstite = new java.util.HashSet<>();
        Map<String, List<String>> Commissioni = new java.util.HashMap<>();
        for (Map.Entry<String, String[]> e : MappaCryptoWallet.entrySet()) {
            String K = Chiave(e.getValue());
            if (K.isEmpty() || !Chiavi.contains(K) || Cancellati.contains(e.getKey())) continue;
            if (isCommissione(e.getValue())) {
                Commissioni.computeIfAbsent(K, x -> new ArrayList<>()).add(e.getKey());
            } else if (!"AU".equalsIgnoreCase(e.getValue()[22])) {
                ConPrincipaleSuperstite.add(K);
            }
        }
        for (Map.Entry<String, List<String>> e : Commissioni.entrySet()) {
            if (!ConPrincipaleSuperstite.contains(e.getKey())) Ris.addAll(e.getValue());
        }
        return Ris;
    }
}
