package com.giacenzecrypto.giacenze_crypto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Tratti di detenzione <b>CRYPTO</b> di un gruppo wallet in un anno: dove il rigo cripto del quadro W/RW
 * va spezzato, e con quale bollo e quali modalità di calcolo vale ciascun pezzo. È il gemello di
 * {@link Calcoli_RW_Fiat#intervalliFiat} per i periodi {@code TipoRigo = CRYPTO} di
 * {@code GRUPPO_PERIODO_RW}, e usa le stesse finestre effettive
 * ({@link Principale_GruppiWalletRW#finestreEffettive}).
 *
 * <p>Regole decise con l'utente il 2026-09-26 ({@code Analisi_QuadroRW_Crypto_Periodi.md} §6):</p>
 * <ul>
 *   <li>si taglia a <b>ogni</b> confine di periodo che cade dentro l'anno, anche fra due periodi con le
 *       stesse impostazioni;</li>
 *   <li>un tratto non coperto da nessun periodo (un buco) è un rigo a sé, senza progressivo, col bollo
 *       del flag di gruppo ({@code GRUPPO_ALIAS.PagaBollo});</li>
 *   <li>un tratto coperto da un periodo prende il bollo <b>del periodo</b> (vuoto = NO, come
 *       {@code statoBolloPeriodi});</li>
 *   <li>le modalità di calcolo valgono solo sui confini che l'utente ha fissato, con lo stesso ripiego
 *       del FIAT al 1° gennaio e al 31 dicembre; i valori manuali dei periodi CRYPTO si ignorano.</li>
 * </ul>
 *
 * <p>Un gruppo senza periodi CRYPTO ha un solo tratto sull'anno intero col bollo del gruppo: è il rigo
 * unico di sempre. Non guarda i movimenti.</p>
 */
public class Calcoli_RW_PeriodiCrypto {

    private Calcoli_RW_PeriodiCrypto() {
    }

    /** Colonne di una riga di {@link #trattiCrypto}. */
    public static final int TC_DATA_INIZIO = 0, TC_DATA_FINE = 1,
            /** Progressivo del periodo CRYPTO che copre il tratto; {@code ""} in un buco fra periodi. */
            TC_PROGRESSIVO = 2,
            /** {@code SI}/{@code NO}: bollo pagato dall'intermediario nel tratto. */
            TC_BOLLO = 3,
            TC_MOD_INIZIALE = 4, TC_MOD_FINALE = 5;
    public static final int TC_COLONNE = 6;

    /** Tratti CRYPTO del gruppo nell'anno, con periodi e flag bollo letti da {@code personale.mv.db}. */
    public static List<String[]> trattiCrypto(String gruppo, String anno) {
        if (gruppo == null || gruppo.isBlank()) {
            return new ArrayList<>();
        }
        String[] alias = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        boolean bolloGruppo = alias != null && "S".equalsIgnoreCase(trim(alias[2]));
        return trattiCrypto(anno, Principale_GruppiWalletRW.caricaPeriodi(gruppo), bolloGruppo);
    }

    /**
     * Spezza l'anno nei tratti CRYPTO definiti da {@code periodi}.
     *
     * @param anno        anno d'imposta, "yyyy"
     * @param periodi     righe del gruppo in forma GUI ({@link Principale_GruppiWalletRW#COLONNE_PERIODO}
     *                    colonne, righe FIAT comprese: vengono scartate)
     * @param bolloGruppo flag bollo del gruppo, usato nei tratti non coperti da un periodo
     * @return tratti contigui che coprono {@code [anno-01-01, anno-12-31]} in ordine di data
     *         ({@link #TC_COLONNE} colonne); lista vuota se l'anno non è valido
     */
    public static List<String[]> trattiCrypto(String anno, List<String[]> periodi, boolean bolloGruppo) {
        List<String[]> out = new ArrayList<>();
        final LocalDate annoInizio;
        final LocalDate annoFine;
        try {
            annoInizio = LocalDate.parse(anno + "-01-01");
            annoFine = LocalDate.parse(anno + "-12-31");
        } catch (DateTimeParseException | NullPointerException e) {
            return out;
        }

        List<Principale_GruppiWalletRW.Finestra> finestre =
                Principale_GruppiWalletRW.finestreEffettive(periodi, Principale_GruppiWalletRW.TIPO_CRYPTO);

        TreeSet<LocalDate> tagli = new TreeSet<>();
        for (Principale_GruppiWalletRW.Finestra f : finestre) {
            if (f.inizio != null && f.inizio.isAfter(annoInizio) && !f.inizio.isAfter(annoFine)) {
                tagli.add(f.inizio);
            }
            if (f.fine != null && !f.fine.isBefore(annoInizio) && f.fine.isBefore(annoFine)) {
                tagli.add(f.fine.plusDays(1));
            }
        }

        List<LocalDate> inizi = new ArrayList<>();
        inizi.add(annoInizio);
        inizi.addAll(tagli);
        for (int i = 0; i < inizi.size(); i++) {
            LocalDate di = inizi.get(i);
            LocalDate df = i + 1 < inizi.size() ? inizi.get(i + 1).minusDays(1) : annoFine;

            String[] tc = new String[TC_COLONNE];
            Arrays.fill(tc, "");
            tc[TC_DATA_INIZIO] = di.toString();
            tc[TC_DATA_FINE] = df.toString();

            Principale_GruppiWalletRW.Finestra copre = Calcoli_RW_Fiat.finestraCheCopre(finestre, di);
            if (copre != null) {
                tc[TC_PROGRESSIVO] = trim(copre.riga[Principale_GruppiWalletRW.COL_PROGRESSIVO]);
                tc[TC_BOLLO] = Principale_GruppiWalletRW.BOLLO_SI.equals(
                        trim(copre.riga[Principale_GruppiWalletRW.COL_BOLLO]))
                        ? Principale_GruppiWalletRW.BOLLO_SI : Principale_GruppiWalletRW.BOLLO_NO;
            } else {
                tc[TC_BOLLO] = bolloGruppo ? Principale_GruppiWalletRW.BOLLO_SI : Principale_GruppiWalletRW.BOLLO_NO;
            }

            String[] pIni = Calcoli_RW_Fiat.periodoConInizio(finestre, di, di.equals(annoInizio));
            if (pIni != null) {
                tc[TC_MOD_INIZIALE] = trim(pIni[Principale_GruppiWalletRW.COL_MOD_INIZIALE]);
            }
            String[] pFin = Calcoli_RW_Fiat.periodoConFine(finestre, df, df.equals(annoFine));
            if (pFin != null) {
                tc[TC_MOD_FINALE] = trim(pFin[Principale_GruppiWalletRW.COL_MOD_FINALE]);
            }
            out.add(tc);
        }
        return out;
    }

    /** Le date (dentro l'anno, dopo il 1° gennaio) in cui comincia un nuovo tratto: i tagli del rigo. */
    public static List<LocalDate> dateDiTaglio(List<String[]> tratti) {
        List<LocalDate> out = new ArrayList<>();
        for (int i = 1; i < tratti.size(); i++) {
            out.add(LocalDate.parse(tratti.get(i)[TC_DATA_INIZIO]));
        }
        return out;
    }

    /** Il tratto che contiene il giorno ISO indicato, o {@code null} se fuori dall'anno. */
    public static String[] trattoAllaData(List<String[]> tratti, String giornoIso) {
        for (String[] t : tratti) {
            if (giornoIso.compareTo(t[TC_DATA_INIZIO]) >= 0 && giornoIso.compareTo(t[TC_DATA_FINE]) <= 0) {
                return t;
            }
        }
        return null;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
