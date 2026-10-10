package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * I dati del grafico "Giacenze a data" (scheda <i>Grafico</i> accanto alla tabella dettaglio movimenti): la giacenza
 * della moneta selezionata nel tempo e il suo valore in euro. Nessuna dipendenza da Swing, rete o database: le
 * variazioni arrivano dalla costruzione della tabella dettaglio, i prezzi da chi chiama ({@link PrezziGiornalieri}),
 * e il disegno lo fa {@link GiacenzeaData_Grafico}.
 *
 * <p>Il valore ha due tipi di punti:
 * <ul>
 *   <li><b>giornalieri</b>: la giacenza a fine giornata (fuso Europe/Rome) per il prezzo di fine giornata;</li>
 *   <li><b>ai movimenti</b>: la giacenza subito dopo il movimento per il prezzo del movimento, lo stesso valore della
 *       colonna "valore qta residua" della tabella; se la tabella non ce l'ha, il prezzo di fine giornata.</li>
 * </ul>
 * Una giacenza nulla vale zero anche senza prezzo; un giorno con giacenza ma senza prezzo resta un buco nella linea
 * del valore, contato in {@link Serie#GiorniSenzaPrezzo()}.
 */
public final class GraficoGiacenze {

    private GraficoGiacenze() {
    }

    /** Il fuso dei giorni del grafico: lo stesso delle date dei movimenti e della data di riferimento. */
    static final ZoneId FUSO = ZoneId.of("Europe/Rome");

    /**
     * Una variazione della giacenza, cioè una riga del dettaglio movimenti.
     *
     * @param Istante istante del movimento, dal prefisso {@code yyyyMMddHHmmss} dell'ID
     * @param ID ID del movimento
     * @param Qta quantità della gamba (negativa in uscita)
     * @param QtaDopo giacenza della selezione dopo il movimento (la "Qta Residua")
     * @param ValoreDopo valore della giacenza dopo il movimento come in tabella, o {@code null} se la tabella non ce l'ha
     * @param Data data e ora del movimento come le mostra la tabella
     * @param Tipo tipo del movimento ({@code [5]})
     * @param Wallet wallet e sotto-wallet del movimento
     */
    public record Variazione(long Istante, String ID, BigDecimal Qta, BigDecimal QtaDopo, BigDecimal ValoreDopo,
            String Data, String Tipo, String Wallet) {
    }

    /**
     * Un punto della linea del valore.
     *
     * @param Istante istante del punto: per i giornalieri l'ultimo millisecondo della giornata (o della data di riferimento)
     * @param Qta giacenza in quel momento
     * @param Prezzo prezzo unitario usato, {@code null} se non usato (giacenza nulla, valore preso dalla tabella) o mancante
     * @param Valore valore della giacenza, {@code null} se manca il prezzo
     * @param Variazione il movimento del punto, {@code null} per i punti giornalieri
     */
    public record PuntoValore(long Istante, BigDecimal Qta, BigDecimal Prezzo, BigDecimal Valore, Variazione Variazione) {
    }

    /**
     * Tutto quello che serve per disegnare.
     *
     * @param Variazioni le variazioni della giacenza, in ordine di tempo
     * @param Valori i punti della linea del valore, in ordine di tempo (vuota finché i prezzi non sono pronti)
     * @param Inizio istante del primo movimento
     * @param Fine istante di fine del grafico (la data di riferimento della scheda, esclusa)
     * @param GiorniSenzaPrezzo giorni con giacenza diversa da zero senza un prezzo
     */
    public record Serie(List<Variazione> Variazioni, List<PuntoValore> Valori, long Inizio, long Fine, int GiorniSenzaPrezzo) {

        /** Vero se c'è almeno un punto con un valore. */
        public boolean HaValori() {
            for (PuntoValore p : Valori) {
                if (p.Valore() != null) return true;
            }
            return false;
        }
    }

    /** @return l'istante del movimento dal prefisso {@code yyyyMMddHHmmss} del suo ID, 0 se non leggibile */
    static long IstanteDaID(String ID) {
        if (ID == null || ID.length() < 14) return 0;
        return FunzioniDate.ConvertiDataIDinLong(ID.substring(0, 14));
    }

    /** @return il giorno (Europe/Rome) dell'istante */
    static LocalDate Giorno(long Istante) {
        return Instant.ofEpochMilli(Istante).atZone(FUSO).toLocalDate();
    }

    /** @return il primo millisecondo del giorno (Europe/Rome) */
    static long InizioGiorno(LocalDate Giorno) {
        return Giorno.atStartOfDay(FUSO).toInstant().toEpochMilli();
    }

    /**
     * La giacenza a fine di ogni giorno, dal giorno del primo movimento all'ultimo giorno prima di {@code Fine}.
     * La giornata che contiene {@code Fine} non c'è: la data di riferimento della scheda è la mezzanotte del giorno
     * dopo quello scelto (o l'istante attuale, e allora l'ultimo giorno è quello di oggi fino ad adesso).
     *
     * @param Variazioni in ordine di tempo
     * @param Fine istante escluso
     */
    static NavigableMap<LocalDate, BigDecimal> GiacenzaFineGiorno(List<Variazione> Variazioni, long Fine) {
        NavigableMap<LocalDate, BigDecimal> Ris = new TreeMap<>();
        if (Variazioni.isEmpty() || Fine <= Variazioni.get(0).Istante()) return Ris;
        LocalDate Ultimo = Giorno(Fine - 1);
        int i = 0;
        BigDecimal Qta = BigDecimal.ZERO;
        for (LocalDate g = Giorno(Variazioni.get(0).Istante()); !g.isAfter(Ultimo); g = g.plusDays(1)) {
            long FineGiorno = Math.min(InizioGiorno(g.plusDays(1)), Fine);
            while (i < Variazioni.size() && Variazioni.get(i).Istante() < FineGiorno) {
                Qta = Variazioni.get(i).QtaDopo();
                i++;
            }
            Ris.put(g, Qta);
        }
        return Ris;
    }

    /**
     * I giorni per cui serve un prezzo di fine giornata: quelli che finiscono con una giacenza diversa da zero e
     * quelli di un movimento il cui valore la tabella non ha.
     */
    static SortedSet<LocalDate> GiorniDaPrezzare(List<Variazione> Variazioni, long Fine) {
        SortedSet<LocalDate> Ris = new TreeSet<>();
        for (var e : GiacenzaFineGiorno(Variazioni, Fine).entrySet()) {
            if (e.getValue().signum() != 0) Ris.add(e.getKey());
        }
        for (Variazione v : Variazioni) {
            if (v.ValoreDopo() == null && v.QtaDopo().signum() != 0 && v.Istante() < Fine) Ris.add(Giorno(v.Istante()));
        }
        return Ris;
    }

    /** La serie senza valori, per disegnare subito la giacenza mentre i prezzi arrivano. */
    static Serie SoloGiacenza(List<Variazione> Variazioni, long Fine) {
        long Inizio = Variazioni.isEmpty() ? Fine : Variazioni.get(0).Istante();
        return new Serie(Collections.unmodifiableList(new ArrayList<>(Variazioni)), List.of(), Inizio, Fine, 0);
    }

    /**
     * La serie completa: giacenza e valore.
     *
     * @param Variazioni in ordine di tempo
     * @param Fine istante escluso
     * @param PrezzoFineGiorno prezzo unitario in euro a fine giornata, {@code null} se non c'è
     */
    static Serie Costruisci(List<Variazione> Variazioni, long Fine, Function<LocalDate, BigDecimal> PrezzoFineGiorno) {
        List<PuntoValore> Valori = new ArrayList<>();
        int SenzaPrezzo = 0;
        for (var e : GiacenzaFineGiorno(Variazioni, Fine).entrySet()) {
            long Istante = Math.min(InizioGiorno(e.getKey().plusDays(1)), Fine) - 1;
            BigDecimal Qta = e.getValue();
            if (Qta.signum() == 0) {
                Valori.add(new PuntoValore(Istante, Qta, null, BigDecimal.ZERO, null));
                continue;
            }
            BigDecimal Prezzo = PrezzoFineGiorno.apply(e.getKey());
            if (Prezzo == null) SenzaPrezzo++;
            Valori.add(new PuntoValore(Istante, Qta, Prezzo, Prezzo == null ? null : Valore(Qta, Prezzo), null));
        }
        for (Variazione v : Variazioni) {
            if (v.Istante() >= Fine) continue;
            BigDecimal Valore;
            BigDecimal Prezzo = null;
            if (v.ValoreDopo() != null) {
                Valore = v.ValoreDopo();
            } else if (v.QtaDopo().signum() == 0) {
                Valore = BigDecimal.ZERO;
            } else {
                Prezzo = PrezzoFineGiorno.apply(Giorno(v.Istante()));
                Valore = Prezzo == null ? null : Valore(v.QtaDopo(), Prezzo);
            }
            Valori.add(new PuntoValore(v.Istante(), v.QtaDopo(), Prezzo, Valore, v));
        }
        //A parità di istante il punto del movimento viene prima di quello giornaliero (che è a fine giornata)
        Valori.sort((a, b) -> a.Istante() != b.Istante() ? Long.compare(a.Istante(), b.Istante())
                : Boolean.compare(a.Variazione() == null, b.Variazione() == null));
        long Inizio = Variazioni.isEmpty() ? Fine : Variazioni.get(0).Istante();
        return new Serie(Collections.unmodifiableList(new ArrayList<>(Variazioni)), Collections.unmodifiableList(Valori),
                Inizio, Fine, SenzaPrezzo);
    }

    private static BigDecimal Valore(BigDecimal Qta, BigDecimal Prezzo) {
        return Qta.multiply(Prezzo).setScale(2, RoundingMode.HALF_UP);
    }
}
