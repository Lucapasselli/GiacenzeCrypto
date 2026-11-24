package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le posizioni su derivati ancora aperte a una data (2026-11-20, punto 6 delle mancanze sui derivati, richiesta dell'utente del 19/11):
 * per ogni token del contratto ({@code DERIV.<instId>.LONG|SHORT}) i contratti aperti, da quando, a quale prezzo medio (lotti LIFO
 * ancora aperti, {@link DerivatiLifo}) e il margine ancora parcheggiato nel sotto-wallet {@value OKXDerivati#SOTTOWALLET}.
 *
 * <p>Solo consultazione: nessun movimento si modifica e nessun totale fiscale cambia. Vede solo le posizioni con i token del contratto,
 * cioe' i perpetui e i futures di OKX scaricati dalle API dalla 1.0.67: le posizioni aperte con la 1.0.66 e quelle importate da CSV
 * (Bybit, Binance) non ne hanno. Logica senza Swing, il dialogo e' {@code GUI_ContrattiAperti}.
 */
public final class Principale_ContrattiAperti {

    private Principale_ContrattiAperti() {
    }

    /** Una posizione aperta. */
    public static final class Riga {
        public String exchange = "";
        public String contratto = "";
        public boolean lungo;
        public BigDecimal contratti = BigDecimal.ZERO;
        /** Data del lotto aperto piu' vecchio. */
        public String apertaDal = "";
        /** Media dei prezzi dei lotti ancora aperti, pesata sui contratti; {@code null} se manca un prezzo. */
        public BigDecimal prezzoMedio;
        /** Margine ancora nel sotto-wallet dei derivati, nella moneta di regolamento. */
        public BigDecimal margine = BigDecimal.ZERO;
        public String moneta = "";
        /** Una riga di token della posizione, per aprire il riepilogo del contratto. */
        public String idMovimento = "";
    }

    /** {@link #Calcola(long, Collection)} sull'archivio. */
    public static List<Riga> Calcola(long limite) {
        return Calcola(limite, Principale.MappaCryptoWallet.values());
    }

    /**
     * @param limite istante in millisecondi: contano i movimenti prima di questo (la mezzanotte dopo il giorno scelto)
     * @param movimenti i movimenti dell'archivio
     * @return le posizioni con contratti aperti, in ordine di contratto e verso
     */
    static List<Riga> Calcola(long limite, Collection<String[]> movimenti) {
        List<String[]> fino = new ArrayList<>();
        for (String[] v : movimenti) {
            if (v == null || v.length <= Derivati.CAMPO_DATI || !Derivati.IsRigaContrattoOMargine(v)) continue;
            if (Istante(v) < limite) fino.add(v);
        }
        //Margine parcheggiato (dai dati dei token e dal funding sul conto, non dalla quantita' del giroconto, che nella forma "conto" porta
        //anche profitto e commissione) e ultima riga di token, per posizione
        Map<String, BigDecimal> margine = OKXDerivati.MarginiParcheggiati(movimenti, limite);
        Map<String, String[]> ultimoToken = new LinkedHashMap<>();
        for (String[] v : fino) {
            String chiave = DerivatiLifo.ChiavePosizioneDaMovimento(v);
            if (chiave == null) continue;
            chiave = chiave.toUpperCase();
            if (Derivati.Tipo(v).equals(Derivati.CONTRATTO)) {
                String[] prima = ultimoToken.get(chiave);
                if (prima == null || OKXDerivati.ORDINE_ESECUZIONE.compare(prima, v) < 0) ultimoToken.put(chiave, v);
            }
        }

        List<Riga> righe = new ArrayList<>();
        for (DerivatiLifo.Posizione p : DerivatiLifo.Calcola(fino).values()) {
            BigDecimal aperti = p.contrattiAperti();
            if (aperti.signum() <= 0) continue;
            Riga r = new Riga();
            String chiave = p.token.toUpperCase();
            r.contratto = p.contratto;
            r.lungo = p.lungo;
            r.contratti = aperti;
            r.moneta = p.moneta;
            r.margine = margine.getOrDefault(chiave, BigDecimal.ZERO);
            String[] t = ultimoToken.get(chiave);
            if (t != null) {
                r.exchange = t[3];
                r.idMovimento = t[0];
            }
            BigDecimal somma = BigDecimal.ZERO;
            boolean prezzi = true;
            for (DerivatiLifo.Lotto l : p.aperti) {
                if (r.apertaDal.isEmpty() || (l.data != null && l.data.compareTo(r.apertaDal) < 0)) r.apertaDal = l.data == null ? "" : l.data;
                //DerivatiLifo apre un lotto senza prezzo a zero, non a null: zero non e' un prezzo
                if (l.prezzo == null || l.prezzo.signum() <= 0) prezzi = false;
                else somma = somma.add(l.prezzo.multiply(l.contratti));
            }
            if (prezzi) r.prezzoMedio = somma.divide(aperti, 8, RoundingMode.HALF_UP).stripTrailingZeros();
            righe.add(r);
        }
        righe.sort((a, b) -> {
            int c = a.contratto.compareTo(b.contratto);
            return c != 0 ? c : Boolean.compare(b.lungo, a.lungo);
        });
        return righe;
    }

    /** Istante di un movimento: dal campo dati se c'e', altrimenti dai 14 caratteri iniziali dell'ID. */
    private static long Istante(String[] v) {
        String ts = Derivati.Dati(v).get("ts");
        if (ts != null && Funzioni.isNumeric(ts, false)) return Long.parseLong(ts);
        if (v[0] != null && v[0].length() >= 14) return FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
        return 0;
    }

    /** Il testo sopra la tabella: cosa mostra e cosa non puo' mostrare. Dice l'effetto, mai come trattare il caso. */
    public static String TestoIntestazione(String data, int quante) {
        return (quante == 0 ? "Nessuna posizione su derivati aperta" : quante == 1 ? "1 posizione su derivati aperta"
                : quante + " posizioni su derivati aperte") + " alla fine del " + data + ", con tutti i wallet.\n"
                + "Prezzo medio: media dei prezzi di esecuzione dei lotti ancora aperti, nell'ordine LIFO del calcolo. Margine: quello "
                + "ancora parcheggiato nel sotto-wallet Derivati, nella moneta di regolamento. Sono dati di consultazione, non valori fiscali.\n"
                + "Compaiono solo le posizioni con il token del contratto, cioè i perpetui e i futures di OKX scaricati dalle API dalla "
                + "versione 1.0.67. Non compaiono le posizioni aperte con la versione 1.0.66 né quelle importate da file CSV (Bybit, Binance).";
    }
}
