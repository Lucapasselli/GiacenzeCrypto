package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * I token di moneta elettronica noti al programma ({@code config/varie/EmoneyToken.json}) e il controllo che li
 * cerca nei movimenti quando mancano dalla sezione E-Money dell'utente.
 *
 * <p>La sezione E-Money ({@code EMONEY} in {@code personale.mv.db}, {@link Principale#Mappa_EMoney}) la compila
 * l'utente: un EMT che non c'è viene trattato come una cripto-attività qualsiasi, e una permuta con un'altra
 * cripto-attività non realizza nulla. Il controllo serve all'avviso del calcolo dei quadri
 * ({@link Principale_EmoneyMancanti}), su richiesta dell'utente del 2026-10-09.
 *
 * <p>L'elenco è un file e non un array nel codice per la stessa ragione delle note di compilazione e degli alias dei
 * prezzi: gli EMT cambiano (USDG si è aggiunto nel 2026), e un elenco nel codice vorrebbe una versione nuova ogni volta.
 * È anche l'unico elenco: il pulsante dei token standard delle opzioni legge lo stesso file.
 */
final class EmoneyToken {

    private EmoneyToken() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "EmoneyToken";

    /** Il contenuto del file: simboli in ordine e data da cui valgono come E-Money quando li si aggiunge. */
    record Elenco(List<String> simboli, String decorrenza) {}

    /** @return l'elenco (disco, poi copia nel jar), oppure {@code null} se non è disponibile da nessuna parte */
    static Elenco Carica() {
        return MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, EmoneyToken::Interpreta);
    }

    /** @return l'elenco, oppure {@code null} se il contenuto non è valido o non ha nessun token */
    static Elenco Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject radice = new JSONObject(contenuto);
            String decorrenza = radice.optString("decorrenza", "").trim();
            if (!decorrenza.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LoggerGC.ScriviErrore("EmoneyToken: decorrenza non valida in " + nome + ".json");
                return null;
            }
            List<String> simboli = new ArrayList<>();
            JSONArray voci = radice.getJSONArray("emoneyToken");
            for (int i = 0; i < voci.length(); i++) {
                String s = voci.getJSONObject(i).optString("simbolo", "").trim();
                if (!s.isEmpty()) simboli.add(s);
            }
            return simboli.isEmpty() ? null : new Elenco(Collections.unmodifiableList(simboli), decorrenza);
        } catch (Exception e) {
            LoggerGC.ScriviErrore("EmoneyToken: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }

    /**
     * I token dell'elenco che compaiono nei movimenti da {@code decorrenza} in poi e che la sezione E-Money non
     * riconosce, con il numero di movimenti: cioè i movimenti che aggiungerli cambierebbe. Il confronto con la sezione
     * passa da {@link Funzioni#DataDecorrenzaEmoney}, come nel motore (maiuscole e minuscole comprese). Restano
     * fuori le gambe FIAT e i token marcati SCAM.
     *
     * @return simbolo dell'elenco → numero di movimenti, in ordine di simbolo
     */
    static Map<String, Integer> MancantiNeiMovimenti(Map<String, String[]> archivio, Elenco elenco) {
        Map<String, Integer> mancanti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (archivio == null || elenco == null) return mancanti;
        Map<String, String> noti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String s : elenco.simboli()) noti.put(s, s);
        String dal = elenco.decorrenza().replace("-", "");
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 12 || v[0].length() < 8 || v[0].substring(0, 8).compareTo(dal) < 0) continue;
            String trovato = null;
            for (int[] gamba : new int[][]{{8, 9}, {11, 12}}) {
                String simbolo = v[gamba[0]] == null ? "" : v[gamba[0]].trim();
                if (simbolo.isEmpty() || "FIAT".equalsIgnoreCase(v[gamba[1]]) || Funzioni.isSCAM(simbolo)) continue;
                String nota = noti.get(simbolo);
                if (nota != null && Funzioni.DataDecorrenzaEmoney(simbolo) == null) trovato = nota;
            }
            if (trovato != null) mancanti.merge(trovato, 1, Integer::sum);
        }
        return mancanti;
    }
}
