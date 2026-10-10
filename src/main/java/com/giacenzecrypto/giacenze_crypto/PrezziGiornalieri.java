package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Prezzi di fine giornata per il grafico delle giacenze ({@link GraficoGiacenze}): una quotazione per moneta e
 * giorno, in euro.
 *
 * <p><b>Non sono prezzi fiscali e non toccano la cache dei prezzi.</b> Stanno nella tabella {@code PrezziGiornalieri}
 * del database prezzi e nessun calcolo la legge. In {@code PrezziNew} un'apertura giornaliera alle 00:00 UTC
 * diventerebbe la quotazione più vicina di qualche valorizzazione (la scheda stessa allarga la ricerca fino a ±24
 * ore, il quadro RW legge la cache dal vivo), e i marcatori delle ore già scaricate fermerebbero per sempre lo
 * scaricamento al minuto di quei giorni. Per questo si usano solo il JSON dello script
 * ({@link Prezzi#EseguiLottoScript(List, String, boolean, String)}) e le funzioni pure di {@link PrezziDefiLlama},
 * mai i loro percorsi che scrivono in cache.
 *
 * <p>Fonti, in ordine: la tabella; per le monete senza address il vecchio archivio orario {@code XXXEUR} (copre i
 * primi anni senza rete); poi, solo per i giorni che mancano e se c'è la connessione, le candele giornaliere degli
 * exchange CCXT (prezzo di apertura, convertito in euro dallo script) o, per i token con address senza alias, la
 * serie giornaliera di DefiLlama ({@code /chart}, {@code period=1d}): per un grafico un punto al giorno basta, e una
 * chiamata ne porta 500. Un giorno chiesto e non trovato non si richiede più nella sessione.
 *
 * <p>Il prezzo di fine del giorno D (fuso Europe/Rome) è l'apertura del giorno UTC D+1, cioè la chiusura di D; per
 * l'ultimo giorno, se è oggi, l'apertura di oggi.
 */
final class PrezziGiornalieri {

    private PrezziGiornalieri() {
    }

    static final long GIORNO_MS = 86_400_000L;

    /** Due tratti di giorni mancanti più vicini di così si chiedono insieme: costa meno di due richieste. */
    private static final int UNISCI_TRATTI_ENTRO_GIORNI = 31;

    /** Punti per chiamata della serie {@code /chart} di DefiLlama. */
    private static final int PUNTI_DEFILLAMA = 500;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /** Giorni già chiesti in questa sessione, trovati o no: {@code chiave|giornoUTC}. */
    private static final Set<String> CHIESTI = ConcurrentHashMap.newKeySet();

    enum TipoFonte { EURO, USD, EXCHANGE, DEFILLAMA, NESSUNA }

    /**
     * Da dove prende i prezzi una moneta.
     *
     * @param Chiave chiave nella tabella: il simbolo per gli exchange, {@code rete:address} in minuscolo per DefiLlama
     */
    record Fonte(TipoFonte Tipo, String Chiave, String Address, String Rete) {
    }

    /**
     * La fonte dei prezzi della moneta, con le stesse regole della valorizzazione: SCAM senza prezzo, un token con
     * address e alias si prezza col simbolo dell'alias, uno senza alias per address, il resto per simbolo (con
     * {@code stessoPrezzo}). Gli alias si prendono senza guardare la data {@code dal}: qui non c'è nessun anno
     * dichiarato da proteggere, e il prezzo dell'exchange è il migliore.
     */
    static Fonte FonteDi(String Moneta, String Tipo, String Address, String Rete) {
        if (Moneta == null || Moneta.isBlank()) return new Fonte(TipoFonte.NESSUNA, "", "", "");
        Address = Address == null ? "" : Address.trim();
        Rete = Rete == null ? "" : Rete.trim();
        if (Tipo != null && Tipo.trim().equalsIgnoreCase("FIAT")) {
            if (Moneta.equalsIgnoreCase("EUR")) return new Fonte(TipoFonte.EURO, "EUR", "", "");
            if (Moneta.equalsIgnoreCase("USD")) return new Fonte(TipoFonte.USD, "USD", "", "");
            return new Fonte(TipoFonte.EXCHANGE, Moneta.toUpperCase(), "", "");
        }
        if (Funzioni.isSCAM(Moneta)) return new Fonte(TipoFonte.NESSUNA, "", "", "");
        if (!Address.isBlank() && !Rete.isBlank() && Funzioni_WalletDeFi.isValidAddress(Address, Rete)) {
            String Alias = AliasPrezziToken.Alias(Address, Rete, Long.MAX_VALUE);
            if (Alias != null) {
                return new Fonte(TipoFonte.EXCHANGE, AliasPrezziToken.StessoPrezzo(Alias, Long.MAX_VALUE).toUpperCase(), "", "");
            }
            String NomeRete = PrezziDefiLlama.NomeRete(Rete);
            if (NomeRete == null) return new Fonte(TipoFonte.NESSUNA, "", "", "");
            return new Fonte(TipoFonte.DEFILLAMA, (NomeRete + ":" + Address).toLowerCase(), Address, Rete);
        }
        return new Fonte(TipoFonte.EXCHANGE, AliasPrezziToken.StessoPrezzo(Moneta, Long.MAX_VALUE).toUpperCase(), "", "");
    }

    /** @return il giorno UTC (giorni dal 1970) la cui apertura è il prezzo di fine del giorno locale {@code g} */
    static long GiornoUTC(LocalDate g, long OggiUTC) {
        return Math.min(g.plusDays(1).toEpochDay(), OggiUTC);
    }

    /** @return il giorno UTC più vicino all'istante: le candele giornaliere di qualche exchange non partono a mezzanotte UTC */
    static long GiornoUTCDaIstante(long Istante) {
        return Math.floorDiv(Istante + GIORNO_MS / 2, GIORNO_MS);
    }

    /**
     * Raggruppa i giorni in tratti contigui, unendo quelli separati da meno di {@link #UNISCI_TRATTI_ENTRO_GIORNI}.
     * @return coppie {primo, ultimo}
     */
    static List<long[]> Tratti(SortedSet<Long> Giorni) {
        List<long[]> Ris = new ArrayList<>();
        long[] Corrente = null;
        for (long g : Giorni) {
            if (Corrente != null && g - Corrente[1] <= UNISCI_TRATTI_ENTRO_GIORNI) {
                Corrente[1] = g;
            } else {
                Corrente = new long[]{g, g};
                Ris.add(Corrente);
            }
        }
        return Ris;
    }

    /**
     * I prezzi di fine giornata della moneta per i giorni indicati. Pensata per un thread di background: legge il
     * database e, con {@code Scarica}, va in rete per i giorni che mancano.
     *
     * @param Annullato vero se il risultato non serve più: si smette prima della prossima richiesta
     * @return giorno → prezzo unitario in euro, solo per i giorni che ne hanno uno
     */
    static Map<LocalDate, BigDecimal> PrezziFineGiorno(String Moneta, String Tipo, String Address, String Rete,
            SortedSet<LocalDate> Giorni, boolean Scarica, BooleanSupplier Annullato) {
        Map<LocalDate, BigDecimal> Ris = new HashMap<>();
        if (Giorni.isEmpty()) return Ris;
        Fonte F = FonteDi(Moneta, Tipo, Address, Rete);
        switch (F.Tipo()) {
            case NESSUNA:
                return Ris;
            case EURO:
                for (LocalDate g : Giorni) Ris.put(g, BigDecimal.ONE);
                return Ris;
            case USD:
                if (Prezzi.MappaConversioneUSDEUR.isEmpty()) Prezzi.RecuperaTassiCambioEURUSD();
                for (LocalDate g : Giorni) {
                    String Tasso = PrezziDefiLlama.TassoUSDEUR(GraficoGiacenze.InizioGiorno(g.plusDays(1)) - 1);
                    if (Tasso != null) Ris.put(g, new BigDecimal(Tasso));
                }
                return Ris;
            default:
                break;
        }

        //E-Money in euro: 1:1 dal giorno in cui la sezione E-Money lo dice, come nella valorizzazione
        SortedSet<LocalDate> DaCercare = new TreeSet<>();
        for (LocalDate g : Giorni) {
            if (Prezzi.EMoneyAncoratoAdEuro(Moneta, GraficoGiacenze.InizioGiorno(g.plusDays(1)) - 1)) Ris.put(g, BigDecimal.ONE);
            else DaCercare.add(g);
        }
        if (DaCercare.isEmpty()) return Ris;

        long OggiUTC = Math.floorDiv(System.currentTimeMillis(), GIORNO_MS);
        Map<LocalDate, Long> UTCdelGiorno = new HashMap<>();
        TreeSet<Long> GiorniUTC = new TreeSet<>();
        for (LocalDate g : DaCercare) {
            long u = GiornoUTC(g, OggiUTC);
            UTCdelGiorno.put(g, u);
            GiorniUTC.add(u);
        }
        Map<Long, Double> Prezzi_ = Leggi(F.Chiave(), GiorniUTC.first(), GiorniUTC.last());

        //Il vecchio archivio orario: gratis, e copre i primi anni (fino al 2022 per le monete principali)
        if (F.Tipo() == TipoFonte.EXCHANGE) {
            for (LocalDate g : DaCercare) {
                if (Prezzi_.containsKey(UTCdelGiorno.get(g))) continue;
                BigDecimal p = DaArchivioOrario(F.Chiave(), GraficoGiacenze.InizioGiorno(g.plusDays(1)));
                if (p != null) Ris.put(g, p);
            }
        }

        TreeSet<Long> Mancanti = new TreeSet<>();
        for (LocalDate g : DaCercare) {
            long u = UTCdelGiorno.get(g);
            if (!Ris.containsKey(g) && !Prezzi_.containsKey(u) && !CHIESTI.contains(F.Chiave() + "|" + u)) Mancanti.add(u);
        }
        if (Scarica && !Mancanti.isEmpty() && !Annullato.getAsBoolean() && Funzioni.CeConnessioneInternet()) {
            Map<Long, Double> Scaricati = F.Tipo() == TipoFonte.EXCHANGE
                    ? ScaricaDaExchange(F.Chiave(), Tratti(Mancanti), Annullato)
                    : ScaricaDaDefiLlama(F, Tratti(Mancanti), Annullato);
            //Un giorno conta come chiesto solo se lo scaricamento non è stato abbandonato a metà
            if (!Annullato.getAsBoolean()) for (long u : Mancanti) CHIESTI.add(F.Chiave() + "|" + u);
            Salva(F, Scaricati, OggiUTC);
            Prezzi_.putAll(Scaricati);
        }

        for (LocalDate g : DaCercare) {
            if (Ris.containsKey(g)) continue;
            Double p = Prezzi_.get(UTCdelGiorno.get(g));
            if (p == null) p = Prezzi_.get(UTCdelGiorno.get(g) - 1);//l'apertura del giorno stesso, se manca quella del dopo
            if (p != null && p > 0) Ris.put(g, BigDecimal.valueOf(p));
        }
        return Ris;
    }

    private static BigDecimal DaArchivioOrario(String Simbolo, long Istante) {
        try {
            String Valore = DatabaseH2.XXXEUR_Leggi(FunzioniDate.ConvertiDatadaLongallOra(Istante) + " " + Simbolo);
            if (Valore != null && Funzioni.isNumeric(Valore, false)) {
                BigDecimal p = new BigDecimal(Valore);
                if (p.signum() > 0) return p;
            }
        } catch (RuntimeException ex) {
            //Archivio non leggibile: il giorno si cerca nelle altre fonti
        }
        return null;
    }

    /** I prezzi salvati per {@code Chiave} fra i due giorni UTC compresi. */
    private static Map<Long, Double> Leggi(String Chiave, long Da, long A) {
        Map<Long, Double> Ris = new TreeMap<>();
        String sql = "SELECT giorno, prezzo FROM PrezziGiornalieri WHERE moneta = ? AND giorno BETWEEN ? AND ?";
        //La connessione si prende a ogni uso: la compattazione del database la sostituisce
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setString(1, Chiave);
            ps.setLong(2, Da - 1);
            ps.setLong(3, A);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) Ris.put(rs.getLong(1), rs.getDouble(2));
            }
        } catch (SQLException | RuntimeException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Ris;
    }

    /** Salva i prezzi scaricati, tranne quello di oggi (DefiLlama dà l'ultimo prezzo, non l'apertura). */
    private static void Salva(Fonte F, Map<Long, Double> Prezzi_, long OggiUTC) {
        if (Prezzi_.isEmpty()) return;
        String sql = "MERGE INTO PrezziGiornalieri (moneta, giorno, prezzo, fonte) KEY (moneta, giorno) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (var e : Prezzi_.entrySet()) {
                if (e.getKey() >= OggiUTC || e.getValue() == null || e.getValue() <= 0) continue;
                ps.setString(1, F.Chiave());
                ps.setLong(2, e.getKey());
                ps.setDouble(3, e.getValue());
                ps.setString(4, F.Tipo() == TipoFonte.EXCHANGE ? "exchange" : PrezziDefiLlama.EXCHANGE);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException | RuntimeException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     * Candele giornaliere degli exchange CCXT, in un solo lotto, in cascata: lo script si ferma al primo exchange
     * che ha il primo giorno del tratto. Prezzo = apertura in euro; i punti senza tasso EUR dell'exchange arrivano
     * "grezzi" in USD/USDT/USDC e si convertono col cambio della Banca d'Italia.
     */
    private static Map<Long, Double> ScaricaDaExchange(String Simbolo, List<long[]> Tratti, BooleanSupplier Annullato) {
        Map<Long, Double> Ris = new TreeMap<>();
        if (Annullato.getAsBoolean()) return Ris;
        long Adesso = System.currentTimeMillis();
        List<Prezzi.RichiestaPrezzo> Richieste = new ArrayList<>();
        for (long[] t : Tratti) {
            long Da = t[0] * GIORNO_MS;
            long A = Math.min(t[1] * GIORNO_MS + GIORNO_MS - 1, Adesso);
            if (Da > Adesso || A < Da) continue;
            Richieste.add(new Prezzi.RichiestaPrezzo(Simbolo, Da, A, Da));
        }
        if (Richieste.isEmpty()) return Ris;
        JsonElement Radice;
        try {
            Radice = Prezzi.EseguiLottoScript(Richieste, Prezzi.EXCHANGES_CCXT, false, "1d");
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            return Ris;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Ris;
        }
        if (Radice == null || !Radice.isJsonArray()) return Ris;
        List<String> Ordine = List.of(Prezzi.EXCHANGES_CCXT.split(","));
        for (JsonElement el : Radice.getAsJsonArray()) {
            if (!el.isJsonObject() || !el.getAsJsonObject().has("punti")) continue;
            for (JsonElement p : el.getAsJsonObject().getAsJsonArray("punti")) {
                Double Prezzo = PrezzoDelPunto(p, Ordine);
                if (Prezzo != null) Ris.putIfAbsent(GiornoUTCDaIstante(p.getAsJsonObject().get("timestamp").getAsLong()), Prezzo);
            }
        }
        System.out.println("Prezzi giornalieri " + Simbolo + ": " + Ris.size() + " giorni dagli exchange");
        return Ris;
    }

    /**
     * Il prezzo in euro di un punto dello script: il primo exchange nell'ordine dato, altrimenti un prezzo grezzo in
     * dollari convertito.
     */
    static Double PrezzoDelPunto(JsonElement Punto, List<String> Ordine) {
        if (!Punto.isJsonObject() || !Punto.getAsJsonObject().has("timestamp")) return null;
        JsonObject o = Punto.getAsJsonObject();
        if (o.has("prices") && o.get("prices").isJsonObject()) {
            JsonObject Prezzi_ = o.getAsJsonObject("prices");
            for (String ex : Ordine) {
                Double v = Numero(Prezzi_.get(ex));
                if (v != null && v > 0) return v;
            }
            for (var e : Prezzi_.entrySet()) {
                Double v = Numero(e.getValue());
                if (v != null && v > 0) return v;
            }
        }
        if (o.has("grezzi") && o.get("grezzi").isJsonObject()) {
            long ts = o.get("timestamp").getAsLong();
            for (var e : o.getAsJsonObject("grezzi").entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject g = e.getValue().getAsJsonObject();
                String Denom = g.has("denom") ? g.get("denom").getAsString() : "";
                Double v = Numero(g.get("valore"));
                if (v == null || v <= 0 || !(Denom.equals("USD") || Denom.equals("USDT") || Denom.equals("USDC"))) continue;
                if (Prezzi.MappaConversioneUSDEUR.isEmpty()) Prezzi.RecuperaTassiCambioEURUSD();
                String Tasso = PrezziDefiLlama.TassoUSDEUR(ts);
                if (Tasso != null) return v * Double.parseDouble(Tasso);
            }
        }
        return null;
    }

    private static Double Numero(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Serie giornaliera di DefiLlama, a blocchi di {@link #PUNTI_DEFILLAMA} giorni, convertita in euro. */
    private static Map<Long, Double> ScaricaDaDefiLlama(Fonte F, List<long[]> Tratti, BooleanSupplier Annullato) {
        Map<Long, Double> Ris = new TreeMap<>();
        if (Prezzi.MappaConversioneUSDEUR.isEmpty()) Prezzi.RecuperaTassiCambioEURUSD();
        Boolean InElenco = null;
        long OggiUTC = Math.floorDiv(System.currentTimeMillis(), GIORNO_MS);
        for (long[] t : Tratti) {
            for (long Da = t[0]; Da <= t[1]; Da += PUNTI_DEFILLAMA) {
                if (Annullato.getAsBoolean()) return Ris;
                long Punti = Math.min(PUNTI_DEFILLAMA, Math.min(t[1], OggiUTC) - Da + 1);
                if (Punti <= 0) break;
                String Url = "https://coins.llama.fi/chart/" + F.Chiave() + "?start=" + (Da * GIORNO_MS / 1000)
                        + "&span=" + Punti + "&period=1d";
                String Corpo;
                try (Response r = HTTP.newCall(new Request.Builder().url(Url).build()).execute()) {
                    if (!r.isSuccessful() || r.body() == null) {
                        System.out.println("Prezzi giornalieri DefiLlama: risposta " + r.code());
                        continue;
                    }
                    Corpo = r.body().string();
                } catch (IOException ex) {
                    LoggerGC.ScriviErrore(ex);
                    continue;
                }
                try {
                    JsonObject Coins = JsonParser.parseString(Corpo).getAsJsonObject().getAsJsonObject("coins");
                    if (Coins == null) continue;
                    for (var e : Coins.entrySet()) {
                        JsonObject c = e.getValue().getAsJsonObject();
                        Double Affidabilita = c.has("confidence") && !c.get("confidence").isJsonNull()
                                ? c.get("confidence").getAsDouble() : null;
                        if (Affidabilita == null || Affidabilita <= PrezziDefiLlama.SOGLIA_AFFIDABILITA) {
                            if (InElenco == null) InElenco = PrezziDefiLlama.InElencoCoinGecko(F.Address(), F.Rete());
                        }
                        if (!PrezziDefiLlama.Ammesso(Affidabilita, InElenco != null && InElenco)) continue;
                        JsonArray Serie = c.getAsJsonArray("prices");
                        if (Serie == null) continue;
                        for (JsonElement p : Serie) {
                            JsonObject q = p.getAsJsonObject();
                            Double v = Numero(q.get("price"));
                            if (v == null || v <= 0 || !q.has("timestamp")) continue;
                            long ts = q.get("timestamp").getAsLong() * 1000;
                            String Tasso = PrezziDefiLlama.TassoUSDEUR(ts);
                            if (Tasso != null) Ris.putIfAbsent(GiornoUTCDaIstante(ts), v * Double.parseDouble(Tasso));
                        }
                    }
                } catch (RuntimeException ex) {
                    LoggerGC.ScriviErrore(ex);
                }
            }
        }
        System.out.println("Prezzi giornalieri DefiLlama " + F.Chiave() + ": " + Ris.size() + " giorni");
        return Ris;
    }
}
