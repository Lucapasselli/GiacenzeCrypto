/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 * Logica operativa della scheda "Dettagli x Tipologia" del quadro RT
 * (companion class di {@link Principale}, sulla falsariga di
 * {@code Principale_GiacenzeaData} / {@code Principale_DocumentiFonte}).
 *
 * <p>È una scheda di sola consultazione: aggrega, per l'anno selezionato nella
 * tabella principale del quadro RT, la plusvalenza fiscale ({@code v[19]}) dei
 * soli movimenti fiscalmente rilevanti ({@code v[33] == "S"}), raggruppandola per
 * tipologia di movimento ({@code v[5]}) e per mese di competenza ({@code v[1]}).
 * La somma della colonna "Totale" riconcilia con la colonna "Plusvalenze
 * Realizzate" della tabella principale del quadro RT, che accumula {@code v[19]}
 * con lo stesso filtro {@code v[33] == "S"} + {@code Funzioni_isNumeric}.
 *
 * <p>Nessun metodo qui tocca componenti Swing: ricevono i {@link DefaultTableModel}
 * da riempire e restituiscono i dati che servono al grafico. Il refresh delle
 * tabelle resta in {@link Principale}.
 */
public class Principale_RT_Tipologie {

    /** Etichetta usata quando il campo tipologia ({@code v[5]}) è vuoto. */
    public static final String TIPOLOGIA_VUOTA = "(senza tipologia)";
    /** Etichetta della riga dei totali nella tabella pivot. */
    public static final String RIGA_TOTALE = "TOTALE";

    /** Colonne della tabella pivot: Tipologia + 12 mesi + Totale. */
    public static final String[] COLONNE_TIPOLOGIE = {
        "Tipologia", "Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
        "Lug", "Ago", "Set", "Ott", "Nov", "Dic", "Totale"
    };

    /** Colonne della tabella con l'elenco dei movimenti della tipologia selezionata. */
    public static final String[] COLONNE_MOVIMENTI = {
        "Data e Ora", "Tipologia", "Sottotipo", "Wallet",
        "Moneta Uscita", "Qta Uscita", "Moneta Entrata", "Qta Entrata",
        "Corrispettivo", "Costo di Carico", "Plusvalenza", "ID"
    };

    /** Dati aggregati per un anno: plusvalenza mensile per tipologia e totale mensile. */
    public static class DatiTipologie {
        /** Tipologia → 12 valori (indice 0 = gennaio), ordinata per plusvalenza totale decrescente. */
        public final LinkedHashMap<String, BigDecimal[]> perTipologia = new LinkedHashMap<>();
        /** Plusvalenza totale di ogni mese (tutte le tipologie), indice 0 = gennaio. */
        public final BigDecimal[] totaleMensile = azzera12();
        /** Anno di riferimento. */
        public String anno = "";

        /** @return la plusvalenza totale dei 12 mesi per la {@code tipologia} indicata, o tutti zeri se assente. */
        public BigDecimal[] serie(String tipologia) {
            BigDecimal[] s = perTipologia.get(tipologia);
            return s != null ? s : azzera12();
        }
    }

    private Principale_RT_Tipologie() {
    }

    /** @return un array di 12 {@link BigDecimal} inizializzati a zero. */
    public static BigDecimal[] azzera12() {
        BigDecimal[] a = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            a[i] = BigDecimal.ZERO;
        }
        return a;
    }

    /**
     * Scorre {@link Principale#MappaCryptoWallet} una volta e aggrega la plusvalenza
     * dei movimenti rilevanti dell'{@code anno} per tipologia e mese.
     *
     * @param anno anno di competenza (formato {@code yyyy}, come nella colonna 0 della tabella principale RT)
     * @return i dati aggregati, con le tipologie ordinate per plusvalenza totale decrescente
     */
    public static DatiTipologie Calcola(String anno) {
        DatiTipologie dati = new DatiTipologie();
        dati.anno = anno == null ? "" : anno;
        if (dati.anno.isEmpty()) {
            return dati;
        }

        for (String[] v : MappaCryptoWallet.values()) {
            if (v.length <= 33) {
                continue;
            }
            if (!"S".equals(v[33])) {
                continue;
            }
            String data = v[1];
            if (data == null || data.length() < 7) {
                continue;
            }
            if (!dati.anno.equals(data.substring(0, 4))) {
                continue;
            }
            int mese;
            try {
                mese = Integer.parseInt(data.substring(5, 7));
            } catch (NumberFormatException ex) {
                continue;
            }
            if (mese < 1 || mese > 12) {
                continue;
            }
            //Stesso filtro di Calcoli_RT: solo v[19] numerico contribuisce al totale annuo
            if (!Principale.Funzioni_isNumeric(v[19], false)) {
                continue;
            }
            BigDecimal plus = new BigDecimal(v[19]);
            String tip = v[5] == null ? "" : v[5].trim();
            if (tip.isEmpty()) {
                tip = TIPOLOGIA_VUOTA;
            }
            BigDecimal[] riga = dati.perTipologia.get(tip);
            if (riga == null) {
                riga = azzera12();
                dati.perTipologia.put(tip, riga);
            }
            riga[mese - 1] = riga[mese - 1].add(plus);
            dati.totaleMensile[mese - 1] = dati.totaleMensile[mese - 1].add(plus);
        }

        //Riordino per plusvalenza totale decrescente (più significative in cima)
        List<Map.Entry<String, BigDecimal[]>> voci = new ArrayList<>(dati.perTipologia.entrySet());
        voci.sort(Comparator.comparing((Map.Entry<String, BigDecimal[]> e) -> somma(e.getValue())).reversed());
        LinkedHashMap<String, BigDecimal[]> ordinata = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal[]> e : voci) {
            ordinata.put(e.getKey(), e.getValue());
        }
        dati.perTipologia.clear();
        dati.perTipologia.putAll(ordinata);
        return dati;
    }

    /**
     * Riempie il modello della tabella pivot: una riga per tipologia (12 mesi + totale)
     * e una riga finale {@value #RIGA_TOTALE} con i totali di colonna. Il modello va
     * svuotato dal chiamante prima della chiamata.
     */
    public static void PopolaTabella(DefaultTableModel modello, DatiTipologie dati) {
        for (Map.Entry<String, BigDecimal[]> e : dati.perTipologia.entrySet()) {
            modello.addRow(rigaConTotale(e.getKey(), e.getValue()));
        }
        if (!dati.perTipologia.isEmpty()) {
            modello.addRow(rigaConTotale(RIGA_TOTALE, dati.totaleMensile));
        }
    }

    private static Object[] rigaConTotale(String etichetta, BigDecimal[] mesi) {
        Object[] r = new Object[COLONNE_TIPOLOGIE.length];
        r[0] = etichetta;
        for (int m = 0; m < 12; m++) {
            r[m + 1] = mesi[m];
        }
        r[13] = somma(mesi);
        return r;
    }

    /**
     * Riempie il modello con l'elenco dei movimenti rilevanti dell'{@code anno} che
     * appartengono alla {@code tipologia} indicata, ordinati per data. Il modello va
     * svuotato dal chiamante prima della chiamata.
     */
    public static void PopolaMovimenti(DefaultTableModel modello, String anno, String tipologia) {
        if (anno == null || anno.isEmpty() || tipologia == null) {
            return;
        }
        boolean cercaVuota = TIPOLOGIA_VUOTA.equals(tipologia);
        List<Object[]> righe = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (v.length <= 33 || !"S".equals(v[33])) {
                continue;
            }
            String data = v[1];
            if (data == null || data.length() < 7 || !anno.equals(data.substring(0, 4))) {
                continue;
            }
            String tip = v[5] == null ? "" : v[5].trim();
            boolean match = cercaVuota ? tip.isEmpty() : tip.equals(tipologia);
            if (!match) {
                continue;
            }
            righe.add(new Object[]{
                v[1], v[5], v[18], v[3],
                v[8], v[10], v[11], v[13],
                numero(v[15]), numero(v[16]), numero(v[19]), v[0]
            });
        }
        righe.sort(Comparator.comparing(r -> String.valueOf(r[0])));
        for (Object[] r : righe) {
            modello.addRow(r);
        }
    }

    /** @return la somma dei 12 valori mensili. */
    public static BigDecimal somma(BigDecimal[] mesi) {
        BigDecimal tot = BigDecimal.ZERO;
        for (BigDecimal b : mesi) {
            if (b != null) {
                tot = tot.add(b);
            }
        }
        return tot;
    }

    /** Converte in {@link BigDecimal} se numerico, altrimenti restituisce la stringa originale (o {@code null} se vuota). */
    private static Object numero(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        if (Principale.Funzioni_isNumeric(s, false)) {
            return new BigDecimal(s);
        }
        return s;
    }
}
