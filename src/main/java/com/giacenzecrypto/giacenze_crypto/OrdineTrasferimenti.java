package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * L'ordine di un trasferimento fra wallet quando prelievo e deposito cadono nello stesso secondo.
 *
 * <p>{@code MappaCryptoWallet} è ordinata sull'ID {@code data_wallet_nm1_nm2_categoria}: a parità di secondo
 * decide il segmento 1 (il nome del wallet, o per l'on-chain {@code BC.rete.indirizzo.hash}), quindi il
 * deposito può comparire prima del prelievo che lo ha originato. Il risultato fiscale non cambia (il {@code PTW}
 * legge soltanto, il {@code DTW} toglie dalla pila della controparte), cambia l'ordine mostrato e quello in cui il
 * motore vede i due movimenti.</p>
 *
 * <p>Quando una classificazione abbina un prelievo e un deposito invertiti nello stesso secondo, il deposito prende
 * il prefisso {@link #MARCATORE} sul segmento 1 e passa dopo il prelievo. Regole da non rompere:</p>
 * <ul>
 *   <li><b>Non si rinomina dentro l'abbinamento ma dopo</b>: i cicli di classificazione automatica scorrono
 *       {@code DepositiPrelieviDaCategorizzare}, una lista di ID raccolti prima, e un deposito rinominato a metà
 *       darebbe {@code null} al {@code get}. {@link #Registra} annota la coppia, {@link #Riordina} la sistema
 *       a inizio {@code Funzioni_AggiornaTutto}, quando nessun ciclo è in corso.</li>
 *   <li><b>Guardia</b>: il deposito passa dopo tutti i movimenti di quel secondo, quindi si rinuncia se uno di
 *       quelli che oggi lo seguono è un'uscita dello stesso token sullo stesso wallet (andrebbe in saldo negativo
 *       e senza LiFo). Il caso resta invertito e si scrive una riga nel log, non è un errore.</li>
 *   <li><b>Il marcatore si riconosce solo con {@code startsWith("~")}</b>: {@code MovimentiCrypto.normalizzaNome} toglie
 *       un {@code ~} iniziale, quindi un ID costruito da {@code creaMovimento} non può nascere marcato. Nei movimenti
 *       on-chain il segmento 1 non coincide con {@code [3]}: non si può quindi riconoscere il marcatore
 *       confrontando il resto con il wallet.</li>
 *   <li>L'annullamento della classificazione e il ripristino delle modifiche manuali tolgono il marcatore
 *       ({@link #IDSenzaMarcatore}).</li>
 *   <li>Nessuna voce di storico: è un'etichetta di ordinamento, non cambia il contenuto del movimento. Il lignaggio
 *       {@code [42]} resta sulla riga, che è la stessa.</li>
 * </ul>
 * Decisioni e conteggi sull'archivio reale: {@code nocommit/Documentazione/Analisi_Ordine_Trasferimenti.md}.
 */
public final class OrdineTrasferimenti {

    /** Prefisso del segmento 1 di un deposito spostato dopo il suo prelievo. {@code ~} ordina dopo ogni lettera e cifra. */
    public static final String MARCATORE = "~";

    /** Coppie {prelievo, deposito} (le righe vive, non gli ID) in attesa di {@link #Riordina}. */
    private static final List<String[][]> DaRiordinare = new ArrayList<>();

    private OrdineTrasferimenti() {
    }

    /**
     * Annota una coppia appena abbinata se ha il deposito prima del prelievo nello stesso secondo.
     * Le coppie nell'ordine giusto non vengono nemmeno ricordate.
     * @param prelievo riga del prelievo (quella viva nella mappa)
     * @param deposito riga del deposito (quella viva nella mappa)
     */
    public static void Registra(String[] prelievo, String[] deposito) {
        if (!Invertita(prelievo, deposito)) return;
        synchronized (DaRiordinare) {
            DaRiordinare.add(new String[][]{prelievo, deposito});
        }
    }

    /** Riordina le coppie annotate sulla mappa principale. @return quanti depositi sono stati spostati. */
    public static int Riordina() {
        return Riordina(Principale.MappaCryptoWallet);
    }

    /**
     * Sposta dopo il prelievo i depositi annotati da {@link #Registra}, dove la guardia lo consente.
     * Si fida delle righe e non degli ID: una riga non più nella mappa (cancellata, sostituita), una coppia che
     * non è più abbinata o già nell'ordine giusto viene ignorata.
     * @param mappa la mappa dei movimenti (ordinata senza distinzione di maiuscole come {@code MappaCryptoWallet})
     * @return quanti depositi sono stati rinominati
     */
    public static int Riordina(NavigableMap<String, String[]> mappa) {
        List<String[][]> coppie;
        synchronized (DaRiordinare) {
            if (DaRiordinare.isEmpty()) return 0;
            coppie = new ArrayList<>(DaRiordinare);
            DaRiordinare.clear();
        }
        int spostati = 0;
        for (String[][] c : coppie) {
            String[] prelievo = c[0];
            String[] deposito = c[1];
            if (mappa.get(prelievo[0]) != prelievo || mappa.get(deposito[0]) != deposito) continue;
            if (!Abbinati(prelievo, deposito) || !Invertita(prelievo, deposito)) continue;
            String ostacolo = PrimoConsumatore(mappa, prelievo, deposito);
            if (ostacolo != null) {
                LoggerGC.logInfo("Ordine trasferimenti: deposito " + deposito[0] + " lasciato prima del prelievo, il movimento "
                        + ostacolo + " usa lo stesso token sullo stesso wallet nello stesso secondo");
                continue;
            }
            String nuovo = NuovoID(mappa, prelievo, deposito);
            if (nuovo == null) continue;
            Rinomina(mappa, deposito, nuovo);
            spostati++;
        }
        return spostati;
    }

    /** Il segmento 1 dell'ID inizia con il marcatore. */
    public static boolean HaMarcatore(String id) {
        String[] p = id == null ? new String[0] : id.split("_");
        return p.length >= 5 && p[1].startsWith(MARCATORE);
    }

    /**
     * L'ID com'era prima della rinomina.
     * @return l'ID senza il marcatore, {@code null} se l'ID non lo ha
     */
    public static String IDSenzaMarcatore(String id) {
        if (!HaMarcatore(id)) return null;
        String[] p = id.split("_");
        p[1] = p[1].substring(MARCATORE.length());
        return String.join("_", p);
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Stesso secondo, wallet diversi e il deposito ordina prima del prelievo. */
    static boolean Invertita(String[] prelievo, String[] deposito) {
        if (prelievo == null || deposito == null || prelievo.length <= 20 || deposito.length <= 20) return false;
        String[] ip = prelievo[0].split("_");
        String[] id = deposito[0].split("_");
        if (ip.length < 5 || id.length < 5 || !ip[0].equals(id[0])) return false;
        //Un deposito già marcato è già stato sistemato (o lo ha scelto qualcun altro)
        if (id[1].startsWith(MARCATORE)) return false;
        //Sullo stesso wallet l'ordine lo decidono nm1, nm2 e categoria, non il nome: non è il caso di questa classe
        if (StessoWallet(prelievo, deposito)) return false;
        return String.CASE_INSENSITIVE_ORDER.compare(deposito[0], prelievo[0]) < 0;
    }

    private static boolean StessoWallet(String[] a, String[] b) {
        return a[3].trim().equalsIgnoreCase(b[3].trim()) && a[4].trim().equalsIgnoreCase(b[4].trim());
    }

    private static List<String> Elenco(String campo20) {
        List<String> r = new ArrayList<>();
        if (campo20 == null) return r;
        for (String s : campo20.split(",")) if (!s.isBlank()) r.add(s.trim());
        return r;
    }

    /** Il prelievo e il deposito sono ancora collegati fra loro come trasferimento. */
    private static boolean Abbinati(String[] prelievo, String[] deposito) {
        return prelievo[18] != null && prelievo[18].contains("PTW")
                && deposito[18] != null && deposito[18].contains("DTW")
                && Elenco(prelievo[20]).contains(deposito[0])
                && Elenco(deposito[20]).contains(prelievo[0]);
    }

    private static boolean Diversa(String qta) {
        if (qta == null || qta.isBlank()) return false;
        try {
            return new BigDecimal(qta.trim()).signum() != 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Il primo movimento che il deposito scavalcherebbe e che non può stargli dietro: dello stesso secondo, dopo di lui,
     * sullo stesso wallet ({@code [3]} e {@code [4]}), con quantità in uscita dello stesso token. I movimenti dello
     * stesso trasferimento (prelievo, commissione, reward) non contano.
     * @return l'ID del movimento, {@code null} se la strada è libera
     */
    static String PrimoConsumatore(NavigableMap<String, String[]> mappa, String[] prelievo, String[] deposito) {
        String secondo = deposito[0].split("_")[0];
        Set<String> delGruppo = new HashSet<>(Elenco(deposito[20]));
        delGruppo.addAll(Elenco(prelievo[20]));
        delGruppo.add(prelievo[0]);
        delGruppo.add(deposito[0]);
        //Tutte le chiavi del secondo hanno la forma "<secondo>_...": '_' < '`', quindi stanno fra questi due estremi
        NavigableMap<String, String[]> dopo = mappa.subMap(deposito[0], false, secondo + "`", false);
        for (Map.Entry<String, String[]> e : dopo.entrySet()) {
            String[] r = e.getValue();
            if (r == null || r.length <= 20 || delGruppo.contains(e.getKey())) continue;
            if (!StessoWallet(r, deposito)) continue;
            if (r[8] == null || !r[8].equals(deposito[11]) || !Diversa(r[10])) continue;
            return e.getKey();
        }
        return null;
    }

    /** Il nuovo ID del deposito: lo stesso con il marcatore sul segmento 1, reso univoco, e che ordina dopo il prelievo. */
    static String NuovoID(NavigableMap<String, String[]> mappa, String[] prelievo, String[] deposito) {
        String[] p = deposito[0].split("_");
        if (p.length < 5) return null;
        p[1] = MARCATORE + p[1];
        String nuovo = String.join("_", p);
        if (mappa.containsKey(nuovo)) nuovo = MovimentiCrypto.getIDUnivoco(mappa, nuovo);
        if (nuovo == null || String.CASE_INSENSITIVE_ORDER.compare(nuovo, prelievo[0]) <= 0) return null;
        return nuovo;
    }

    /** Cambia la chiave della riga e i riferimenti {@code [20]} che la citano (quelli dei suoi collegati). */
    private static void Rinomina(NavigableMap<String, String[]> mappa, String[] deposito, String nuovo) {
        String vecchio = deposito[0];
        List<String> collegati = Elenco(deposito[20]);
        mappa.remove(vecchio);
        deposito[0] = nuovo;
        mappa.put(nuovo, deposito);
        for (String id : collegati) {
            String[] r = mappa.get(id);
            if (r == null || r.length <= 20) continue;
            List<String> voci = new ArrayList<>(Arrays.asList(r[20].split(",")));
            for (int i = 0; i < voci.size(); i++) if (voci.get(i).trim().equals(vecchio)) voci.set(i, nuovo);
            r[20] = String.join(",", voci);
        }
        LoggerGC.logInfo("Ordine trasferimenti: deposito " + vecchio + " spostato dopo il prelievo come " + nuovo);
    }
}
