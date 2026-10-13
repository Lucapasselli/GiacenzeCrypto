package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Le liste di simboli che il programma tratta in modo particolare, lette da {@code config/varie/Simboli.json}.
 *
 * <p>Contiene i simboli prioritari dei prezzi ({@link Prezzi#SimboliPrioritari()}), le valute FIAT
 * ({@link Moneta#AssegnaTipoAuto()}) e le stablecoin non MiCA del pulsante dei token standard della sezione E-Money
 * ({@code stablecoinDubbie}, {@code stablecoinAltre}); altre liste si aggiungeranno come chiavi nuove dello stesso file.
 *
 * <p>È un file e non un array nel codice per la stessa ragione delle note di compilazione e degli alias dei prezzi:
 * una moneta nuova da quotare per prima non deve voler dire una versione nuova. Le liste restano in memoria e si
 * rileggono dopo l'aggiornamento in background ({@link RicaricaConfig}).
 *
 * <p><b>Nel codice non c'è nessuna copia delle liste</b>: sarebbe una seconda parte da tenere allineata, e un file
 * modificato non coinciderebbe più con essa. Il default è la copia nel jar ({@link MappeCausali#CaricaConRipiego}),
 * che vale anche quando il file su disco non è valido o chiede una versione più recente del programma. Se manca anche
 * quella l'installazione è rotta: lo si scrive nel log e la lista resta vuota.
 */
final class ListeSimboli {

    private ListeSimboli() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "Simboli";

    private static volatile Set<String> fiat;

    /** @return i simboli prioritari del file (disco, poi copia nel jar) */
    static String[] CaricaPrioritari() {
        return CaricaLista("prioritari");
    }

    /** @return i simboli, oppure {@code null} se il contenuto non è valido o la lista è vuota */
    static String[] InterpretaPrioritari(String contenuto, String nome) {
        return Interpreta(contenuto, nome, "prioritari");
    }

    /** @return le stablecoin dubbie del file (lette a ogni chiamata: si usano a un clic) */
    static String[] StablecoinDubbie() {
        return CaricaLista("stablecoinDubbie");
    }

    /** @return le stablecoin non MiCA né dubbie del file */
    static String[] StablecoinAltre() {
        return CaricaLista("stablecoinAltre");
    }

    /** @return le valute FIAT del file */
    static Set<String> CaricaFiat() {
        return Set.of(CaricaLista("fiat"));
    }

    /**
     * Le valute FIAT, in memoria: {@link Moneta#AssegnaTipoAuto()} gira a ogni moneta creata e non può rileggere il
     * file ogni volta. Rilette da {@link #RicaricaFiat()} dopo l'aggiornamento in background.
     */
    static Set<String> Fiat() {
        Set<String> s = fiat;
        if (s == null) {
            s = CaricaFiat();
            fiat = s;
        }
        return s;
    }

    static void RicaricaFiat() {
        fiat = CaricaFiat();
    }

    private static String[] CaricaLista(String chiave) {
        String[] lette = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE,
                (contenuto, nome) -> Interpreta(contenuto, nome, chiave));
        return lette != null ? lette : new String[0];
    }

    /** @return i simboli della lista {@code chiave}, oppure {@code null} se il contenuto non è valido o la lista è vuota */
    static String[] Interpreta(String contenuto, String nome, String chiave) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONArray voci = new JSONObject(contenuto).getJSONObject(chiave).getJSONArray("simboli");
            List<String> simboli = new ArrayList<>();
            for (int i = 0; i < voci.length(); i++) {
                String s = voci.getString(i).trim();
                if (!s.isEmpty()) simboli.add(s);
            }
            return simboli.isEmpty() ? null : simboli.toArray(new String[0]);
        } catch (Exception e) {
            LoggerGC.ScriviErrore("ListeSimboli: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }
}
