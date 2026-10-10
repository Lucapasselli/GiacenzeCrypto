package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Layout personalizzabile delle colonne della tabella dei movimenti
 * ({@code Principale.TransazioniCryptoTabella}): quali colonne sono visibili, in quale ordine e con
 * quale larghezza.
 *
 * <p><b>Agisce solo sul {@link TableColumnModel} (la vista), mai sul {@link javax.swing.table.TableModel}.</b>
 * Il model resta a 43 colonne nell'ordine originale, quindi continuano a funzionare senza modifiche:
 * i filtri per colonna ({@code Tabelle.tableFilters}, indicizzati per model), l'ordinamento, le somme
 * nell'header, l'export Excel e ogni {@code getValueAt(modelRow, N)} dei renderer. Nascondere una
 * colonna = {@code removeColumn}; mostrarla = ricrearla e riordinarla. {@code convertColumnIndexToModel}
 * regge perché ogni {@link TableColumn} conserva il proprio {@code modelIndex}.
 *
 * <p>Le colonne 0-39 sono i campi del movimento ({@code v[0..39]}, via {@code Funzioni.Converti_String_Object}).
 * Le colonne 40 ("Fonte Prezzi") e 41 ("Alias Gruppo Wallet") sono state aggiunte in coda apposta per non
 * spostare nessun indice esistente: la 40 è comunque un campo reale del movimento ({@code v[40]}, già
 * esistente e usato da {@link Prezzi.InfoPrezzo#Ritorna40()} ma mai mostrato in questa tabella prima d'ora),
 * la 41 è **derivata** — non esiste in {@code v[]}, viene calcolata dal ciclo di caricamento risalendo dal
 * gruppo wallet di {@code v[3]} all'alias in {@code GRUPPO_ALIAS} — quindi non può ottenersi semplicemente
 * spuntando un campo esistente, a differenza di tutte le altre. Anche la 42 ("Operazione", prima "Gruppo Collegato") è una
 * colonna di model che non coincide con {@code v[42]}: mostra {@code v[43]}, la chiave di
 * {@code CommissioniCollegate} (commissioni e contratti Dual Investment), perché il model non contiene
 * i campi 40-44 e senza quella colonna né la ricerca né i filtri potrebbero trovarla.
 *
 * <p>Il default (costruttore {@code applica(tabella, null)}) riproduce esattamente l'insieme e
 * l'ordine storici di {@code Principale.TransazioniCrypto_Funzioni_NascondiColonneTabellaCrypto()}.
 * La preferenza dell'utente è salvata in {@code personale.mv.db} sotto {@link #OPZIONE} come JSON
 * versionato: un JSON di schema diverso o corrotto viene ignorato e si torna al default.
 *
 * <p><b>Il meccanismo non è più solo della tabella movimenti</b>: ciò che dipende dalla tabella (quali colonne sono
 * interne o fisse, l'ordine di default, il massimo indice, le larghezze base, la chiave in {@code personale.mv.db})
 * sta in un {@link Profilo}. {@link #PROFILO_MOVIMENTI} è quello storico, {@link #PROFILO_GIACENZE_DETTAGLIO} quello della
 * tabella dettaglio movimenti di "Giacenze a data". Ogni layout ricorda il proprio profilo; le funzioni senza profilo
 * esplicito lavorano sul profilo dei movimenti, come prima.
 */
public final class LayoutColonneMovimenti {

    /** Versione dello schema serializzato. Un JSON con {@code v} diverso viene scartato da {@link #fromJson}. */
    public static final int VERSIONE_SCHEMA = 1;

    /** Chiave in {@code personale.mv.db} (via {@code DatabaseH2.Pers_Opzioni_*}). */
    public static final String OPZIONE = "Movimenti_LayoutColonne";

    /** Indici di model mai offerti all'utente: id, campi ausiliari letti dai renderer, colonne "null". */
    static final List<Integer> COLONNE_INTERNE = List.of(0,2,22, 32, 33, 35, 36, 37, 38, 39);

    /** Indici di model sempre visibili (scelta di prodotto: set ridotto). Checkbox bloccata nel dialogo. */
    static final List<Integer> COLONNE_FISSE = List.of(1, 3, 5, 8, 10, 11, 13);

    /** Ordine di default delle colonne visibili: coincide con lo storico di NascondiColonneTabellaCrypto(). */
    static final List<Integer> ORDINE_DEFAULT = List.of(1, 3, 4, 5, 6, 8, 10, 11, 13, 15, 17, 19);

    /** Indice di model massimo esistente (0-based): 40 = Fonte Prezzi, 41 = Alias Gruppo Wallet, 42 = Operazione. */
    static final int COLONNA_MASSIMA = 42;

    /**
     * Ciò che distingue una tabella personalizzabile dall'altra. Gli indici sono sempre quelli del
     * <b>model</b>, che non cambia mai.
     */
    public static final class Profilo {
        /** Chiave in {@code personale.mv.db} dove si salva il layout. */
        public final String opzione;
        /** Indici mai offerti all'utente né mostrati (id, campi letti dai renderer). */
        final List<Integer> interne;
        /** Indici sempre visibili. */
        final List<Integer> fisse;
        /** Ordine di default delle colonne visibili. */
        final List<Integer> ordineDefault;
        /** Indice di model massimo esistente. */
        final int colonnaMassima;
        /** Riporta ogni colonna a una larghezza preferita ragionevole, subito dopo che le colonne sono state ricreate. */
        final Consumer<TableColumnModel> larghezzeBase;

        Profilo(String opzione, List<Integer> interne, List<Integer> fisse, List<Integer> ordineDefault,
                int colonnaMassima, Consumer<TableColumnModel> larghezzeBase) {
            this.opzione = opzione;
            this.interne = interne;
            this.fisse = fisse;
            this.ordineDefault = ordineDefault;
            this.colonnaMassima = colonnaMassima;
            this.larghezzeBase = larghezzeBase;
        }
    }

    /** La tabella dei movimenti della pagina principale. */
    public static final Profilo PROFILO_MOVIMENTI = new Profilo(OPZIONE, COLONNE_INTERNE, COLONNE_FISSE,
            ORDINE_DEFAULT, COLONNA_MASSIMA, LayoutColonneMovimenti::impostaLarghezzeBase);

    /** Chiave in {@code personale.mv.db} del layout della tabella dettaglio movimenti di "Giacenze a data". */
    public static final String OPZIONE_GIACENZE_DETTAGLIO = "GiacenzeaData_Dettaglio_LayoutColonne";

    /**
     * La tabella dettaglio movimenti di "Giacenze a data". Il model ha 19 colonne: 8-12 (ID, saldi negativi
     * precedenti e tre colonne "null") sono interne e non vengono mai mostrate; Data, Quantità e Qta Residua sono
     * fisse. La 18 (Qta Blockchain) non è del layout: la mette in vista e la toglie {@code Principale}, solo quando il
     * wallet si può confrontare con la blockchain, quindi non si offre e non si salva. Il costo di carico del movimento (13) sta <b>prima</b> della Qta Residua (7) pur essendo in coda al
     * model: gli indici fissi di {@code Tabelle.ColoraRigheTabella1GiacenzeaData} e quelli letti da selezione e popup
     * non si spostano, si riordina solo la vista.
     */
    public static final Profilo PROFILO_GIACENZE_DETTAGLIO = new Profilo(OPZIONE_GIACENZE_DETTAGLIO,
            List.of(8, 9, 10, 11, 12, 18), List.of(0, 5, 7),
            List.of(0, 1, 2, 3, 4, 5, 6, 13, 7, 14, 15, 16, 17), 17,
            cm -> {
                preferita(cm, 0, 130);   // Data
                preferita(cm, 1, 110);   // Wallet
                preferita(cm, 2, 60);    // Moneta
                preferita(cm, 3, 110);   // Address Moneta
                preferita(cm, 4, 150);   // Tipo Movimento
                preferita(cm, 5, 110);   // Quantita'
                preferita(cm, 6, 100);   // Valore in Euro
                preferita(cm, 7, 110);   // Qta Residua
                preferita(cm, 13, 100);  // Costo Carico Movimento
                preferita(cm, 14, 90);   // Prezzo Unitario
                preferita(cm, 15, 100);  // Valore Qta Residua
                preferita(cm, 16, 110);  // Costo Carico Qta Residua
                preferita(cm, 17, 110);  // Plus/Minus Latente Qta Residua
            });

    /** Chiave in {@code personale.mv.db} del layout della tabella dei depositi/prelievi da classificare. */
    public static final String OPZIONE_DEPOSITI_PRELIEVI = "DepositiPrelievi_LayoutColonne";

    /**
     * La tabella della scheda Depositi/Prelievi. Il model ha 12 colonne: la 0 (ID) è interna, letta da selezione e
     * popup; Data, Tipo Transazione, Moneta e Qta sono fisse (le ultime tre sono quelle che {@code Tabelle} colora
     * per direzione). "Gruppo Wallet" (10) sta in vista accanto a "Exchange / Wallet" pur essendo in coda al model,
     * "Note" (11, {@code v[21]}) è l'ultima.
     */
    public static final Profilo PROFILO_DEPOSITI_PRELIEVI = new Profilo(OPZIONE_DEPOSITI_PRELIEVI,
            List.of(0), List.of(1, 3, 4, 5),
            List.of(1, 2, 10, 3, 4, 5, 6, 7, 8, 9, 11), 11,
            cm -> {
                preferita(cm, 1, 120);   // Data e Ora
                preferita(cm, 2, 300);   // Exchange / Wallet
                preferita(cm, 3, 160);   // Tipo Transazione
                preferita(cm, 4, 70);    // Moneta
                preferita(cm, 5, 110);   // Qta
                preferita(cm, 6, 160);   // Dettaglio Trasferimento
                preferita(cm, 7, 80);    // Prezzo
                preferita(cm, 8, 160);   // Dett. Defi/CSV
                preferita(cm, 9, 160);   // Controparte
                preferita(cm, 10, 140);  // Gruppo Wallet
                preferita(cm, 11, 200);  // Note
            });

    private final Profilo profilo;
    private final List<Integer> ordine;
    private final Map<Integer, Integer> larghezze;

    LayoutColonneMovimenti(List<Integer> ordine, Map<Integer, Integer> larghezze) {
        this(PROFILO_MOVIMENTI, ordine, larghezze);
    }

    LayoutColonneMovimenti(Profilo profilo, List<Integer> ordine, Map<Integer, Integer> larghezze) {
        this.profilo = profilo;
        this.ordine = new ArrayList<>(ordine);
        this.larghezze = new LinkedHashMap<>(larghezze);
    }

    /** @return il profilo della tabella a cui appartiene questo layout. */
    public Profilo profilo() {
        return profilo;
    }

    /** @return gli indici di model visibili, nell'ordine di vista. */
    public List<Integer> ordine() {
        return new ArrayList<>(ordine);
    }

    /** @return larghezza in pixel per indice di model (solo per le colonne che ne hanno una salvata). */
    public Map<Integer, Integer> larghezze() {
        return new LinkedHashMap<>(larghezze);
    }

    /** @return gli indici di model proponibili nel dialogo (tutti i non interni, in ordine di model). */
    static List<Integer> colonneOffribili() {
        return colonneOffribili(PROFILO_MOVIMENTI);
    }

    static List<Integer> colonneOffribili(Profilo profilo) {
        List<Integer> l = new ArrayList<>();
        for (int m = 0; m <= profilo.colonnaMassima; m++) {
            if (!profilo.interne.contains(m)) l.add(m);
        }
        return l;
    }

    static LayoutColonneMovimenti predefinito() {
        return predefinito(PROFILO_MOVIMENTI);
    }

    static LayoutColonneMovimenti predefinito(Profilo profilo) {
        return new LayoutColonneMovimenti(profilo, profilo.ordineDefault, Map.of());
    }

    String toJson() {
        JSONObject o = new JSONObject();
        o.put("v", VERSIONE_SCHEMA);
        JSONArray col = new JSONArray();
        for (int m : ordine) {
            JSONObject c = new JSONObject();
            c.put("m", m);
            Integer w = larghezze.get(m);
            if (w != null) c.put("w", (int) w);
            col.put(c);
        }
        o.put("col", col);
        return o.toString();
    }

    /** @return il layout descritto dal JSON, oppure {@code null} se assente, di schema diverso o non valido. */
    static LayoutColonneMovimenti fromJson(String s) {
        return fromJson(s, PROFILO_MOVIMENTI);
    }

    /** @return il layout descritto dal JSON per il profilo indicato, oppure {@code null} se assente, di schema diverso o non valido. */
    static LayoutColonneMovimenti fromJson(String s, Profilo profilo) {
        if (s == null || s.isBlank()) return null;
        try {
            JSONObject o = new JSONObject(s);
            if (o.optInt("v", -1) != VERSIONE_SCHEMA) return null;
            JSONArray col = o.optJSONArray("col");
            if (col == null) return null;
            List<Integer> ord = new ArrayList<>();
            Map<Integer, Integer> larg = new LinkedHashMap<>();
            for (int i = 0; i < col.length(); i++) {
                JSONObject c = col.getJSONObject(i);
                int m = c.getInt("m");
                if (m < 0 || m > profilo.colonnaMassima || profilo.interne.contains(m) || ord.contains(m)) continue;
                ord.add(m);
                if (c.has("w")) {
                    int w = c.getInt("w");
                    if (w > 0 && w < 4000) larg.put(m, w);
                }
            }
            if (ord.isEmpty()) return null;
            return new LayoutColonneMovimenti(profilo, ord, larg);
        } catch (Exception e) {
            System.out.println("LayoutColonneMovimenti.fromJson : " + e.getMessage());
            return null;
        }
    }

    /** Legge dalla tabella il layout attualmente in vista (ordine e larghezze correnti). */
    static LayoutColonneMovimenti daTabella(JTable tabella) {
        return daTabella(tabella, PROFILO_MOVIMENTI);
    }

    /** Legge dalla tabella il layout attualmente in vista (ordine e larghezze correnti), per il profilo indicato. */
    static LayoutColonneMovimenti daTabella(JTable tabella, Profilo profilo) {
        List<Integer> ord = new ArrayList<>();
        Map<Integer, Integer> larg = new LinkedHashMap<>();
        TableColumnModel cm = tabella.getColumnModel();
        for (int v = 0; v < cm.getColumnCount(); v++) {
            TableColumn c = cm.getColumn(v);
            int m = c.getModelIndex();
            if (profilo.interne.contains(m) || ord.contains(m)) continue;
            ord.add(m);
            larg.put(m, c.getWidth() > 0 ? c.getWidth() : c.getPreferredWidth());
        }
        if (ord.isEmpty()) return predefinito(profilo);
        return new LayoutColonneMovimenti(profilo, ord, larg);
    }

    /**
     * Applica il layout alla tabella. {@code layout == null} ripristina il default.
     *
     * <p>Ricostruisce da zero il {@link TableColumnModel} ({@code createDefaultColumnsFromModel}),
     * riapplica le larghezze di base, poi rimuove le colonne non visibili, riordina e imposta le
     * larghezze salvate. Le colonne fisse ({@link #COLONNE_FISSE}) sono forzate visibili anche se il
     * layout salvato le omettesse.
     */
    static void applica(JTable tabella, LayoutColonneMovimenti layout) {
        applica(tabella, layout, PROFILO_MOVIMENTI);
    }

    /** Come {@link #applica(JTable, LayoutColonneMovimenti)} per un profilo qualunque; {@code layout == null} ripristina il default di quel profilo. */
    static void applica(JTable tabella, LayoutColonneMovimenti layout, Profilo profilo) {
        if (layout == null) layout = predefinito(profilo);

        tabella.setAutoCreateColumnsFromModel(false);
        tabella.createDefaultColumnsFromModel();          // viewIndex == modelIndex
        impostaLarghezzeBase(tabella, profilo);
        tabella.getTableHeader().setReorderingAllowed(true);

        TableColumnModel cm = tabella.getColumnModel();

        // 1. quali colonne restano visibili, in quale ordine
        List<Integer> visibili = new ArrayList<>();
        for (int m : layout.ordine) {
            if (m >= 0 && m <= profilo.colonnaMassima && !profilo.interne.contains(m) && !visibili.contains(m)) visibili.add(m);
        }
        for (int f : profilo.fisse) {
            if (!visibili.contains(f)) visibili.add(f);
        }

        // 2. rimuovo dalla vista tutto ciò che non è visibile
        for (TableColumn c : Collections.list(cm.getColumns())) {
            if (!visibili.contains(c.getModelIndex())) cm.removeColumn(c);
        }

        // 3. riordino secondo 'visibili'
        for (int pos = 0; pos < visibili.size(); pos++) {
            int cur = indiceVista(cm, visibili.get(pos));
            if (cur >= 0 && cur != pos) cm.moveColumn(cur, pos);
        }

        // 4. larghezze salvate
        for (Map.Entry<Integer, Integer> e : layout.larghezze.entrySet()) {
            int cur = indiceVista(cm, e.getKey());
            if (cur >= 0) {
                TableColumn c = cm.getColumn(cur);
                c.setPreferredWidth(e.getValue());
                c.setWidth(e.getValue());
            }
        }
    }

    private static int indiceVista(TableColumnModel cm, int modelIndex) {
        for (int v = 0; v < cm.getColumnCount(); v++) {
            if (cm.getColumn(v).getModelIndex() == modelIndex) return v;
        }
        return -1;
    }

    /**
     * Riporta ogni colonna a una larghezza <i>preferita</i> ragionevole, senza mai bloccarne il
     * ridimensionamento: min basso e max alto per tutte, così l'utente può trascinare le
     * intestazioni liberamente. I valori "preferiti" ricalcano quelli storici di
     * {@code initComponents()}, che ora sono solo un punto di partenza. Qui gli indici di vista
     * coincidono con quelli di model, perché le colonne sono appena state ricreate da
     * {@code createDefaultColumnsFromModel()}.
     */
    private static void impostaLarghezzeBase(JTable tabella, Profilo profilo) {
        TableColumnModel cm = tabella.getColumnModel();
        for (int v = 0; v < cm.getColumnCount(); v++) {
            TableColumn c = cm.getColumn(v);
            c.setMinWidth(30);
            c.setMaxWidth(2000);
            c.setPreferredWidth(110);
        }
        profilo.larghezzeBase.accept(cm);
    }

    /** Le larghezze preferite della tabella movimenti. */
    private static void impostaLarghezzeBase(TableColumnModel cm) {
        preferita(cm, 1, 120);    // Data e Ora
        preferita(cm, 4, 90);     // Dettaglio Wallet
        preferita(cm, 8, 80);     // Moneta Ven./Trasf.
        preferita(cm, 11, 80);    // Moneta Acq./Ric.
        preferita(cm, 12, 70);    // Tipo Moneta Acq./Ric.
        preferita(cm, 15, 100);   // Valore transazione in EURO
        preferita(cm, 16, 100);   // Costo di Carico C.A. Uscente
        preferita(cm, 17, 100);   // Nuovo Costo di Carico in EURO
        preferita(cm, 18, 100);   // Tipo Trasferimento
        preferita(cm, 19, 100);   // Plusvalenza in EURO
        preferita(cm, 40, 100);   // Fonte Prezzi
        preferita(cm, 41, 120);   // Alias Gruppo Wallet
        preferita(cm, 42, 150);   // Operazione
    }

    private static void preferita(TableColumnModel cm, int modelIndex, int pref) {
        if (modelIndex < 0 || modelIndex >= cm.getColumnCount()) return;
        TableColumn c = cm.getColumn(modelIndex);
        if (c.getModelIndex() != modelIndex) return;
        c.setPreferredWidth(pref);
    }
}
