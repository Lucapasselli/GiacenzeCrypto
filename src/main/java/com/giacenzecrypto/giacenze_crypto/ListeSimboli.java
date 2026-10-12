package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Le liste di simboli che il programma tratta in modo particolare, lette da {@code config/varie/Simboli.json}.
 *
 * <p>Oggi contiene i simboli prioritari dei prezzi ({@link Prezzi#SimboliPrioritari()}) e le valute FIAT
 * ({@link Moneta#AssegnaTipoAuto()}) e le stablecoin non MiCA del pulsante dei token standard della sezione E-Money
 * ({@code stablecoinDubbie}, {@code stablecoinAltre}); altre liste si aggiungeranno come chiavi nuove dello stesso file.
 *
 * <p>È un file e non un array nel codice per la stessa ragione delle note di compilazione e degli alias dei prezzi:
 * una moneta nuova da quotare per prima non deve voler dire una versione nuova. Le liste restano in memoria e si
 * rileggono dopo l'aggiornamento in background ({@link Prezzi#RicaricaSimboliPrioritari()}). Se il file manca o non è
 * valido vale la lista di {@link #PRIORITARI_PREDEFINITI}, la stessa che era nel codice: un prezzo non deve cambiare
 * perché un file è sparito.
 */
final class ListeSimboli {

    private ListeSimboli() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "Simboli";

    /** La lista che era scritta in {@code Prezzi}, per il caso in cui il file non sia disponibile da nessuna parte. */
    static final String[] PRIORITARI_PREDEFINITI = {"EURI", "EURC", "EURCV", "EUROe", "EURQ", "EURR", "EURe", "USDT",
        "USDC", "BUSD", "USDE", "DAI", "TUSD", "BTC", "ETH", "BNB", "SOL", "LTC", "ADA", "XRP", "XLM", "PAX", "TRX",
        "ATOM", "POL", "CRO", "MON"};

    /** Le valute FIAT che erano scritte in {@code Moneta.AssegnaTipoAuto}, per il caso in cui il file non sia disponibile. */
    static final String[] FIAT_PREDEFINITE = {"EUR", "USD", "TRY", "GBP"};

    /** Le stablecoin «dubbie» e le altre che erano scritte in {@code Principale}, come ripiego. */
    static final String[] STABLECOIN_DUBBIE_PREDEFINITE = {"BUSD", "USDT", "PYUSD"};
    static final String[] STABLECOIN_ALTRE_PREDEFINITE = {"DAI", "USDE", "FRAX", "EURT", "AEUR", "VEUR", "JEUR", "LUSD", "USD0",
        "USDS", "USDY"};

    private static volatile Set<String> fiat;

    /** @return le stablecoin dubbie del file, oppure quelle predefinite (lette a ogni chiamata: si usano a un clic) */
    static String[] StablecoinDubbie() {
        return CaricaLista("stablecoinDubbie", STABLECOIN_DUBBIE_PREDEFINITE);
    }

    /** @return le stablecoin non MiCA né dubbie del file, oppure quelle predefinite */
    static String[] StablecoinAltre() {
        return CaricaLista("stablecoinAltre", STABLECOIN_ALTRE_PREDEFINITE);
    }

    private static String[] CaricaLista(String chiave, String[] predefinita) {
        String[] lette = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE,
                (contenuto, nome) -> Interpreta(contenuto, nome, chiave));
        return lette != null ? lette : predefinita.clone();
    }

    /** @return i simboli prioritari del file (disco, poi copia nel jar), oppure quelli predefiniti */
    static String[] CaricaPrioritari() {
        String[] letti = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, ListeSimboli::InterpretaPrioritari);
        return letti != null ? letti : PRIORITARI_PREDEFINITI.clone();
    }

    /** @return i simboli, oppure {@code null} se il contenuto non è valido o la lista è vuota */
    static String[] InterpretaPrioritari(String contenuto, String nome) {
        return Interpreta(contenuto, nome, "prioritari");
    }

    /** @return le valute FIAT del file, oppure quelle predefinite */
    static Set<String> CaricaFiat() {
        String[] lette = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE,
                (contenuto, nome) -> Interpreta(contenuto, nome, "fiat"));
        return Set.of(lette != null ? lette : FIAT_PREDEFINITE);
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
