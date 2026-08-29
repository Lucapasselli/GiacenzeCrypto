package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Il model resta a 40 colonne nell'ordine originale, quindi continuano a funzionare senza modifiche:
 * i filtri per colonna ({@code Tabelle.tableFilters}, indicizzati per model), l'ordinamento, le somme
 * nell'header, l'export Excel e ogni {@code getValueAt(modelRow, N)} dei renderer. Nascondere una
 * colonna = {@code removeColumn}; mostrarla = ricrearla e riordinarla. {@code convertColumnIndexToModel}
 * regge perché ogni {@link TableColumn} conserva il proprio {@code modelIndex}.
 *
 * <p>Il default (costruttore {@code applica(tabella, null)}) riproduce esattamente l'insieme e
 * l'ordine storici di {@code Principale.TransazioniCrypto_Funzioni_NascondiColonneTabellaCrypto()}.
 * La preferenza dell'utente è salvata in {@code personale.mv.db} sotto {@link #OPZIONE} come JSON
 * versionato: un JSON di schema diverso o corrotto viene ignorato e si torna al default.
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

    private final List<Integer> ordine;
    private final Map<Integer, Integer> larghezze;

    LayoutColonneMovimenti(List<Integer> ordine, Map<Integer, Integer> larghezze) {
        this.ordine = new ArrayList<>(ordine);
        this.larghezze = new LinkedHashMap<>(larghezze);
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
        List<Integer> l = new ArrayList<>();
        for (int m = 0; m <= 34; m++) {
            if (!COLONNE_INTERNE.contains(m)) l.add(m);
        }
        return l;
    }

    static LayoutColonneMovimenti predefinito() {
        return new LayoutColonneMovimenti(ORDINE_DEFAULT, Map.of());
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
                if (m < 0 || m > 39 || COLONNE_INTERNE.contains(m) || ord.contains(m)) continue;
                ord.add(m);
                if (c.has("w")) {
                    int w = c.getInt("w");
                    if (w > 0 && w < 4000) larg.put(m, w);
                }
            }
            if (ord.isEmpty()) return null;
            return new LayoutColonneMovimenti(ord, larg);
        } catch (Exception e) {
            System.out.println("LayoutColonneMovimenti.fromJson : " + e.getMessage());
            return null;
        }
    }

    /** Legge dalla tabella il layout attualmente in vista (ordine e larghezze correnti). */
    static LayoutColonneMovimenti daTabella(JTable tabella) {
        List<Integer> ord = new ArrayList<>();
        Map<Integer, Integer> larg = new LinkedHashMap<>();
        TableColumnModel cm = tabella.getColumnModel();
        for (int v = 0; v < cm.getColumnCount(); v++) {
            TableColumn c = cm.getColumn(v);
            int m = c.getModelIndex();
            if (COLONNE_INTERNE.contains(m) || ord.contains(m)) continue;
            ord.add(m);
            larg.put(m, c.getWidth() > 0 ? c.getWidth() : c.getPreferredWidth());
        }
        if (ord.isEmpty()) return predefinito();
        return new LayoutColonneMovimenti(ord, larg);
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
        if (layout == null) layout = predefinito();

        tabella.setAutoCreateColumnsFromModel(false);
        tabella.createDefaultColumnsFromModel();          // 40 colonne, viewIndex == modelIndex
        impostaLarghezzeBase(tabella);
        tabella.getTableHeader().setReorderingAllowed(true);

        TableColumnModel cm = tabella.getColumnModel();

        // 1. quali colonne restano visibili, in quale ordine
        List<Integer> visibili = new ArrayList<>();
        for (int m : layout.ordine) {
            if (m >= 0 && m <= 39 && !COLONNE_INTERNE.contains(m) && !visibili.contains(m)) visibili.add(m);
        }
        for (int f : COLONNE_FISSE) {
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
    private static void impostaLarghezzeBase(JTable tabella) {
        TableColumnModel cm = tabella.getColumnModel();
        for (int v = 0; v < cm.getColumnCount(); v++) {
            TableColumn c = cm.getColumn(v);
            c.setMinWidth(30);
            c.setMaxWidth(2000);
            c.setPreferredWidth(110);
        }
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
    }

    private static void preferita(TableColumnModel cm, int modelIndex, int pref) {
        if (modelIndex < 0 || modelIndex >= cm.getColumnCount()) return;
        TableColumn c = cm.getColumn(modelIndex);
        if (c.getModelIndex() != modelIndex) return;
        c.setPreferredWidth(pref);
    }
}
