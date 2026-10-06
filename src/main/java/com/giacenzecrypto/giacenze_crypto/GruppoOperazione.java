package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * La chiave di <b>operazione</b> nel campo {@value #CAMPO} del movimento: «questi movimenti sono la stessa
 * operazione». Oggi la scrive solo l'abbinamento dei contratti Binance Dual Investment ({@code DUAL-<id contratto>});
 * è il posto dove andranno i gruppi delle altre operazioni quando verranno salvati.
 *
 * <p><b>Perché un campo a parte dalle commissioni collegate</b> ({@link CommissioniCollegate}, campo 43, dal
 * 2026-10-05). Fino ad allora la chiave del contratto stava nello stesso campo delle commissioni, e i due concetti si
 * pestavano i piedi: una commissione collegata al Purchase veniva <b>fusa</b> nel gruppo del contratto, perdendo il
 * movimento a cui apparteneva (il dato che servirà per dedurla nei derivati); servivano eccezioni in Scollega, nel
 * dettaglio e nell'unione; lo scambio di uno scambio differito ereditava la chiave del prelievo con la regola delle
 * commissioni e la chiave del contratto restava a metà. Con due campi ognuno ha le sue regole: una commissione resta
 * collegata al suo movimento, e il movimento porta in più l'identità del contratto.
 *
 * <p>Regole del campo:
 * <ul>
 *   <li><b>solo informativo</b>: nessun motore lo legge, è fuori dall'impronta del ricalcolo e dal golden master;</li>
 *   <li><b>è un'identità, e sta solo sui movimenti originali</b> (dal 2026-10-05): resta su Purchase e Settlement
 *       anche quando la loro classificazione viene annullata (serve a ritrovarli ripassando il file), e <b>non si
 *       scrive mai sui movimenti generati</b> ({@code [22] = "AU"}). Il gruppo, cioè chi è collegato a chi, lo dice
 *       già {@code [20]}: un generato appartiene all'operazione dei movimenti originali del suo gruppo, e la sua
 *       chiave si <b>ricava</b> da lì ({@link #ChiaveEffettiva}). Così ogni dato sta in un posto solo, e una chiave
 *       sui generati non può restare incompleta o sbagliata dopo una riclassificazione;</li>
 *   <li>si copia come il lignaggio e le commissioni: fuori da {@code MovimentiCrypto.CampiNonCopiabiliVerbatim}, dentro
 *       {@code CampiDaRiportare} e {@code CampiProvenienzaUnione}; il duplicato di un movimento la perde.</li>
 * </ul>
 * È una delle basi dell'operazione calcolata ({@link OperazioniCalcolate}), che si vede nella colonna di model 42
 * «Operazione» della tabella movimenti e nel dettaglio del movimento: un'operazione che contiene un movimento con questa
 * chiave si mostra con essa.
 */
public final class GruppoOperazione {

    /** Posizione della chiave di operazione nella riga del movimento. */
    public static final int CAMPO = 45;

    /** Prefisso della chiave di un contratto Dual Investment di Binance, seguito dall'id del contratto. */
    public static final String PREFISSO_DUAL = "DUAL-";

    private GruppoOperazione() {
    }

    /** @return la chiave di operazione del movimento, stringa vuota se non ne ha */
    public static String Chiave(String[] v) {
        if (v == null || v.length <= CAMPO || v[CAMPO] == null) return "";
        return v[CAMPO].trim();
    }

    /** @return {@code true} se la chiave è quella di un contratto Dual Investment */
    public static boolean isContrattoDual(String Chiave) {
        return Chiave != null && Chiave.startsWith(PREFISSO_DUAL);
    }

    /**
     * La chiave del gruppo di un contratto Dual Investment. Il {@code ;} è tolto perché il file dei movimenti è
     * delimitato da {@code ;} e {@code Scrivi_Movimenti_Crypto} lo cancella invece di proteggerlo: una chiave con il
     * {@code ;} non coinciderebbe più dopo un salvataggio.
     * @return la chiave, o stringa vuota se l'id del contratto è vuoto
     */
    public static String ChiaveDual(String IdContratto) {
        if (IdContratto == null) return "";
        String Id = IdContratto.replace(";", "").replaceAll("\\s+", "").trim();
        return Id.isEmpty() ? "" : PREFISSO_DUAL + Id;
    }

    static void Scrivi(String[] v, String Chiave) {
        if (v != null && v.length > CAMPO) v[CAMPO] = Chiave == null ? "" : Chiave;
    }

    /** @return {@code true} se il movimento è generato dal programma ({@code [22] = "AU"}) */
    static boolean isGenerato(String[] v) {
        return v != null && v.length > 22 && v[22] != null && v[22].equalsIgnoreCase("AU");
    }

    /**
     * La chiave di operazione di un movimento come la si mostra: quella scritta sul movimento, oppure, per un movimento
     * generato che non ne ha, quella dei movimenti originali del suo gruppo {@code [20]}. Se gli originali portano
     * chiavi diverse (prelievo e deposito di due contratti abbinati a mano fra loro) il generato non ne prende nessuna.
     * @param Trova ricerca di un movimento per ID (la mappa viva)
     * @return la chiave, vuota se il movimento non fa parte di nessuna operazione
     */
    public static String ChiaveEffettiva(String[] v, Function<String, String[]> Trova) {
        String K = Chiave(v);
        if (!K.isEmpty() || !isGenerato(v) || v[20] == null || v[20].isBlank()) return K;
        Set<String> Chiavi = new LinkedHashSet<>();
        for (String ID : v[20].split(",")) {
            if (ID.isBlank()) continue;
            String[] m = Trova.apply(ID.trim());
            if (m != null && !isGenerato(m) && !Chiave(m).isEmpty()) Chiavi.add(Chiave(m));
        }
        return Chiavi.size() == 1 ? Chiavi.iterator().next() : "";
    }

    /** Come {@link #ChiaveEffettiva(String[], Function)} sulla mappa viva. */
    public static String ChiaveEffettiva(String[] v) {
        return ChiaveEffettiva(v, MappaCryptoWallet::get);
    }

    /**
     * Gli ID dei movimenti in mappa che fanno parte dell'operazione: quelli che ne portano la chiave e i movimenti
     * generati dei loro gruppi {@code [20]} la cui chiave effettiva è la stessa.
     * @return gli ID in ordine di ID
     */
    public static Set<String> Membri(String Chiave) {
        Set<String> Ris = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (Chiave == null || Chiave.isBlank()) return Ris;
        List<String[]> Originali = new ArrayList<>();
        for (Map.Entry<String, String[]> e : MappaCryptoWallet.entrySet()) {
            if (Chiave.equals(Chiave(e.getValue()))) {
                Ris.add(e.getKey());
                Originali.add(e.getValue());
            }
        }
        for (String[] o : Originali) {
            if (o[20] == null || o[20].isBlank()) continue;
            for (String ID : o[20].split(",")) {
                String[] m = ID.isBlank() ? null : MappaCryptoWallet.get(ID.trim());
                if (m != null && isGenerato(m) && Chiave.equals(ChiaveEffettiva(m))) Ris.add(m[0]);
            }
        }
        return Ris;
    }

    /** @return la chiave descritta per chi la legge, vuota se la chiave è vuota */
    public static String Descrizione(String Chiave) {
        if (Chiave == null || Chiave.isBlank()) return "";
        if (isContrattoDual(Chiave)) return "Contratto Dual Investment " + Chiave.substring(PREFISSO_DUAL.length());
        return "Operazione " + Chiave;
    }

    /**
     * Migrazione al caricamento delle righe scritte prima della forma attuale (2026-10-05), in due passi:
     * <ul>
     *   <li>la chiave del contratto che stava nel campo delle commissioni collegate si sposta qui e quel campo si
     *       libera. Una commissione che era stata fusa nel gruppo del contratto resta senza collegamento di commissione:
     *       lo si ricostruisce con <i>Opzioni → Commissioni collegate</i>;</li>
     *   <li>un movimento generato non porta la chiave: la sua si ricava dal gruppo (vedi {@link #ChiaveEffettiva}),
     *       quindi quella scritta dalle versioni precedenti si toglie.</li>
     * </ul>
     * Su una riga già nella forma attuale non fa nulla.
     * @return {@code true} se la riga è cambiata
     */
    static boolean MigraAllaFormaAttuale(String[] v) {
        if (v == null || v.length <= CAMPO) return false;
        boolean Cambiata = false;
        String Vecchia = CommissioniCollegate.Chiave(v);
        if (isContrattoDual(Vecchia)) {
            if (Chiave(v).isEmpty()) Scrivi(v, Vecchia);
            v[CommissioniCollegate.CAMPO] = "";
            Cambiata = true;
        }
        if (isGenerato(v) && !Chiave(v).isEmpty()) {
            Scrivi(v, "");
            Cambiata = true;
        }
        return Cambiata;
    }
}
