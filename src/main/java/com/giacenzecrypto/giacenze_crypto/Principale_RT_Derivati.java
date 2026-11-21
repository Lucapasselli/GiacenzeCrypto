package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * I redditi da derivati di un anno per il quadro RT (sezione II-A) e il quadro T (sezione II) del 730. Logica senza Swing, letta da
 * scheda RT, stampa, archivio delle dichiarazioni ed Excel.
 *
 * <p>Criterio deciso dall'utente il 19/11/2026 (D2 di {@code Decisioni_Derivati.md}): <b>movimento per movimento</b>. Ogni movimento
 * con un risultato dei derivati ({@code [46]}, scritto dal motore per i classificati {@code DDV}/{@code PDV} e dal LIFO dei
 * contratti) e la data nell'anno va in colonna 1 di RT11/T11 se positivo e in colonna 2, senza segno, se negativo. I movimenti a
 * zero (le righe del profitto di OKX di una chiusura calcolata in LIFO) non contano. Le due colonne si arrotondano all'euro sul
 * totale, mai movimento per movimento, e il risultato (RT52) si ricava dalle due colonne arrotondate, così il modulo torna.
 */
public final class Principale_RT_Derivati {

    private Principale_RT_Derivati() {
    }

    /** Un movimento che entra nel quadro, con la colonna in cui va. */
    public static final class Voce {
        public final String id;
        public final String data;
        public final String exchange;
        public final String tipo;
        public final String contratto;
        public final BigDecimal valore;
        /** 1 = colonna 1 (positivo), 2 = colonna 2 (negativo, va senza segno). */
        public final int colonna;
        /** {@code true} se il valore comprende il conguaglio del LIFO dei contratti ({@link DerivatiLifo#CHIAVE_CONGUAGLIO}). */
        public final boolean conguaglio;

        Voce(String id, String data, String exchange, String tipo, String contratto, BigDecimal valore) {
            this(id, data, exchange, tipo, contratto, valore, false);
        }

        Voce(String id, String data, String exchange, String tipo, String contratto, BigDecimal valore, boolean conguaglio) {
            this.conguaglio = conguaglio;
            this.id = id;
            this.data = data;
            this.exchange = exchange;
            this.tipo = tipo;
            this.contratto = contratto;
            this.valore = valore;
            this.colonna = valore.signum() > 0 ? 1 : 2;
        }
    }

    /** I totali di un anno. */
    public static final class Esito {
        public final String anno;
        /** Somma dei valori positivi, ai centesimi. */
        public final BigDecimal positivi;
        /** Somma dei valori negativi senza segno, ai centesimi. */
        public final BigDecimal negativi;
        public final List<Voce> voci;

        Esito(String anno, BigDecimal positivi, BigDecimal negativi, List<Voce> voci) {
            this.anno = anno;
            this.positivi = positivi;
            this.negativi = negativi;
            this.voci = Collections.unmodifiableList(voci);
        }

        /** {@code true} se l'anno ha almeno un movimento con un risultato dei derivati diverso da zero. */
        public boolean presente() {
            return !voci.isEmpty();
        }

        /** RT11/T11 colonna 1, all'euro. */
        public BigDecimal colonna1() {
            return positivi.setScale(0, RoundingMode.HALF_UP);
        }

        /** RT11/T11 colonna 2, all'euro. */
        public BigDecimal colonna2() {
            return negativi.setScale(0, RoundingMode.HALF_UP);
        }

        /** RT52 (o RT23 fino al 2023): colonna 1 meno colonna 2, all'euro. Negativo = perdita. */
        public BigDecimal risultato() {
            return colonna1().subtract(colonna2());
        }

        /** Il risultato ai centesimi, per l'archivio. */
        public BigDecimal risultatoCentesimi() {
            return positivi.subtract(negativi);
        }
    }

    /** Come {@link #Calcola(String, Iterable)} sui movimenti dell'archivio. */
    public static Esito Calcola(String anno) {
        return Calcola(anno, Principale.MappaCryptoWallet == null ? List.of() : Principale.MappaCryptoWallet.values());
    }

    /**
     * @param anno anno a quattro cifre
     * @param movimenti i movimenti, in qualunque ordine (le voci escono in ordine di ID)
     */
    public static Esito Calcola(String anno, Iterable<String[]> movimenti) {
        BigDecimal pos = BigDecimal.ZERO;
        BigDecimal neg = BigDecimal.ZERO;
        List<Voce> voci = new ArrayList<>();
        String a = anno == null ? "" : anno.trim();
        if (movimenti != null && a.length() == 4) {
            for (String[] v : movimenti) {
                if (v == null || v.length <= Derivati.CAMPO_RISULTATO || v[1] == null || !v[1].startsWith(a)) continue;
                BigDecimal r = Derivati.Risultato(v);
                if (r == null || r.signum() == 0) continue;
                String cg = Derivati.Dati(v).get(DerivatiLifo.CHIAVE_CONGUAGLIO);
                voci.add(new Voce(v[0], v[1], v[3], Derivati.Tipo(v), Contratto(v), r, cg != null && !cg.isBlank()));
                if (r.signum() > 0) pos = pos.add(r);
                else neg = neg.add(r.negate());
            }
        }
        voci.sort((x, y) -> String.valueOf(x.id).compareTo(String.valueOf(y.id)));
        return new Esito(a, pos.setScale(2, RoundingMode.HALF_UP), neg.setScale(2, RoundingMode.HALF_UP), voci);
    }

    /** Il contratto del movimento, se si conosce: campo dati delle righe di token, altrimenti la nota {@code Contratto <instId>}. */
    static String Contratto(String[] v) {
        String c = Derivati.Dati(v).get("contratto");
        if (c != null && !c.isBlank()) return c.trim();
        String n = v.length > 21 ? OKXDerivati.ContrattoDaNota(v[21]) : null;
        return n == null ? "" : n;
    }

    /**
     * Le perdite della sezione II-A che risultano dal programma nei quattro anni prima di {@code anno}, solo derivati, dalla più vecchia.
     * Non sono residui: il programma non sa quanto è già stato usato contro altri redditi, né conosce le perdite da altri strumenti.
     *
     * @return anno di origine, perdita all'euro (positiva)
     */
    public static Map<String, BigDecimal> PerditeAnniPrecedenti(String anno, Iterable<String[]> movimenti) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        int a;
        try {
            a = Integer.parseInt(anno.trim());
        } catch (RuntimeException e) {
            return m;
        }
        List<String[]> tutti = new ArrayList<>();
        if (movimenti != null) for (String[] v : movimenti) tutti.add(v);
        for (int y = a - 4; y < a; y++) {
            Esito e = Calcola(String.valueOf(y), tutti);
            if (e.presente() && e.risultato().signum() < 0) m.put(String.valueOf(y), e.risultato().negate());
        }
        return m;
    }

    /** Come {@link #PerditeAnniPrecedenti(String, Iterable)} sui movimenti dell'archivio. */
    public static Map<String, BigDecimal> PerditeAnniPrecedenti(String anno) {
        return PerditeAnniPrecedenti(anno, Principale.MappaCryptoWallet == null ? List.of() : Principale.MappaCryptoWallet.values());
    }

    /**
     * Il blocco delle note di stampa sui redditi da derivati ({@link NoteCompilazione#DERIVATI_RT} o {@link NoteCompilazione#DERIVATI_T},
     * con la variante dell'anno che dà i righi di quel modello), vuoto se l'anno non ha derivati e non ci sono perdite dei quattro
     * anni prima. Valori all'euro, perdite come risultano dal programma (decisione R3, A+).
     */
    public static String Nota(Esito E, Map<String, BigDecimal> Perdite, boolean QuadroT) {
        if ((E == null || !E.presente()) && (Perdite == null || Perdite.isEmpty())) return "";
        Map<String, String> v = new LinkedHashMap<>();
        BigDecimal r = E == null ? BigDecimal.ZERO : E.risultato();
        v.put("col1", Funzioni.formattaBigDecimal(E == null ? BigDecimal.ZERO : E.colonna1(), false));
        v.put("col2", Funzioni.formattaBigDecimal(E == null ? BigDecimal.ZERO : E.colonna2(), false));
        v.put("movimenti", String.valueOf(E == null ? 0 : E.voci.size()));
        v.put("risultato", r.signum() > 0 ? "plusvalenza di " + Funzioni.formattaBigDecimal(r, false)
                : r.signum() < 0 ? "minusvalenza di " + Funzioni.formattaBigDecimal(r.negate(), false) : "zero");
        StringBuilder p = new StringBuilder();
        if (Perdite != null) {
            for (Map.Entry<String, BigDecimal> e : Perdite.entrySet()) {
                if (p.length() > 0) p.append(", ");
                p.append(e.getKey()).append(" : ").append(Funzioni.formattaBigDecimal(e.getValue(), false));
            }
        }
        v.put("perdite", p.length() == 0 ? "nessuna" : p.toString());
        return NoteCompilazione.Testo(QuadroT ? NoteCompilazione.DERIVATI_T : NoteCompilazione.DERIVATI_RT, E == null ? "" : E.anno, v);
    }
    // ===== Riquadro della scheda RT (decisione dell'utente del 20/11/2026) =====

    /** Intestazioni della tabella del riquadro: l'ultima colonna (ID) si nasconde. */
    public static final String[] COLONNE_RIQUADRO = {"Data", "Exchange", "Tipo", "Contratto", "Risultato (euro)", "Colonna", "ID"};
    /** Colonna del modello con l'ID del movimento, per il menu dei movimenti. */
    public static final int COLONNA_ID_RIQUADRO = 6;

    /** Il tipo di derivato in parole, per la tabella del riquadro e l'Excel. */
    public static String DescrizioneTipo(String Tipo, boolean Conguaglio) {
        String t = Tipo == null ? "" : Tipo;
        String d = switch (t) {
            case Derivati.PNL -> "Risultato realizzato";
            case Derivati.FUNDING -> "Funding";
            case Derivati.COMMISSIONE -> "Commissione";
            case Derivati.CONTRATTO -> "Chiusura del contratto (LIFO)";
            case Derivati.BONUS -> "Bonus";
            case Derivati.RIMBORSO_COMMISSIONI -> "Rimborso commissioni";
            case Derivati.CONSEGNA -> "Consegna alla scadenza";
            case Derivati.DUAL -> "Dual Investment";
            default -> t.isEmpty() ? "Non indicato" : t;
        };
        return Conguaglio ? d + ", con il conguaglio degli anni precedenti" : d;
    }

    /** Le righe della tabella del riquadro, nell'ordine di {@link Esito#voci}. */
    public static List<Object[]> RigheRiquadro(Esito E) {
        List<Object[]> l = new ArrayList<>();
        if (E == null) return l;
        for (Voce v : E.voci) {
            l.add(new Object[]{FunzioniDate.DataOraAlSecondo(v.id, v.data), v.exchange, DescrizioneTipo(v.tipo, v.conguaglio),
                v.contratto, v.valore, v.colonna == 1 ? "1 (positivi)" : "2 (negativi)", v.id});
        }
        return l;
    }

    /** I righi dei moduli in cui vanno le due colonne nell'anno, come li compila il programma. */
    public static String Righi(String Anno) {
        int a;
        try {
            a = Integer.parseInt(Anno.trim());
        } catch (RuntimeException e) {
            return "";
        }
        if (a < 2023) return "il programma non ha il modulo di quell'anno, i valori e i righi di allora sono solo nelle note del report.";
        if (a == 2023) return "quadro RT del Modello Redditi PF " + (a + 1) + ", righi RT21, RT22 colonna 3, RT23 e, se è una perdita, RT93 colonna 5. "
                + "Il quadro T del 730 per quell'anno non c'era.";
        return "quadro RT, righi RT11 colonne 1 e 2, RT52 e, se è una perdita, RT102 colonna 5. Quadro T del 730, rigo T11 colonne 1 e 2.";
    }

    /**
     * Le righe di testo del riepilogo in testa al riquadro, senza HTML: anno e movimenti, le due colonne e il risultato, i righi,
     * le perdite dei quattro anni prima.
     */
    public static List<String> RiepilogoRiquadro(Esito E, Map<String, BigDecimal> Perdite) {
        List<String> l = new ArrayList<>();
        if (E == null) return l;
        if (!E.presente()) {
            l.add("Anno " + E.anno + " : nessun movimento con un risultato dei derivati.");
        } else {
            BigDecimal r = E.risultato();
            l.add("Anno " + E.anno + " : " + E.voci.size() + (E.voci.size() == 1 ? " movimento" : " movimenti") + " con un risultato dei derivati.");
            l.add("Colonna 1 (differenziali positivi e proventi) : " + Funzioni.formattaBigDecimal(E.colonna1(), false)
                    + " euro. Colonna 2 (differenziali negativi e oneri) : " + Funzioni.formattaBigDecimal(E.colonna2(), false)
                    + " euro. Risultato : " + (r.signum() > 0 ? "plusvalenza di " + Funzioni.formattaBigDecimal(r, false) + " euro."
                    : r.signum() < 0 ? "minusvalenza di " + Funzioni.formattaBigDecimal(r.negate(), false) + " euro." : "zero."));
            l.add("Righi : " + Righi(E.anno));
        }
        StringBuilder p = new StringBuilder();
        if (Perdite != null) {
            for (Map.Entry<String, BigDecimal> e : Perdite.entrySet()) {
                if (p.length() > 0) p.append(", ");
                p.append(e.getKey()).append(" : ").append(Funzioni.formattaBigDecimal(e.getValue(), false)).append(" euro");
            }
        }
        l.add("Perdite dei derivati nei quattro anni precedenti, come risultano dal programma : " + (p.length() == 0 ? "nessuna." : p + ".")
                + " Non tengono conto di quanto già usato in compensazione né delle perdite da altri strumenti.");
        return l;
    }
}
