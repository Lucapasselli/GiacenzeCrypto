/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logica operativa dei <b>periodi fiscali di un exchange</b> ({@code EXCHANGE_PERIODO}) : per ogni
 * exchange, una riga per ogni entità legale / Stato estero nel tempo (le migrazioni MiCA fanno
 * cambiare Stato e identificativo, e ogni cambio genera un nuovo rigo RW e ISEE).
 *
 * <p>Companion di {@code Principale} nello stile di {@link Principale_GruppiWalletRW} : metodi
 * {@code public static}, nessun campo Swing. Persistenza via {@code DatabaseH2.Pers_ExchangePeriodo_*}.</p>
 *
 * <p>Validazione volutamente <i>strutturale</i> (progressivo, formato date, lunghezze). La
 * risoluzione "quale periodo vale alla data X" ({@link #periodoAllaData}) usa la semantica dei
 * periodi di {@code GRUPPO_PERIODO_RW} : data vuota = periodo aperto. Nessun consumo lato motore
 * RW/ISEE : per ora la GUI compila e {@link Principale_GruppiWalletRW} legge il periodo corrente.</p>
 */
public class Principale_PeriodiExchange {

    private Principale_PeriodiExchange() {
    }

    /**
     * Riga della GUI "periodi fiscali exchange" : {@code [Progressivo, DataInizio, DataFine, Nome,
     * StatoEstero, IdentificativoFiscale, Note, Fonte, Origine, ChiaveDefault]} — 10 colonne, senza
     * chiave sintetica né ExchangeId. {@link #COL_ORIGINE} / {@link #COL_CHIAVE_DEFAULT} sono in coda :
     * gli indici funzionali (0-7) non si sono spostati.
     */
    public static final int COL_PROGRESSIVO = 0, COL_DATA_INIZIO = 1, COL_DATA_FINE = 2, COL_NOME = 3,
            COL_STATO = 4, COL_IDENT = 5, COL_NOTE = 6, COL_FONTE = 7, COL_ORIGINE = 8, COL_CHIAVE_DEFAULT = 9;
    public static final int COLONNE = 10;

    public static final String ORIGINE_SISTEMA = Principale_GruppiWalletRW.ORIGINE_SISTEMA;
    public static final String ORIGINE_UTENTE = Principale_GruppiWalletRW.ORIGINE_UTENTE;

    /** Lunghezza massima dell'identificativo di un operatore finanziario estero nel modulo FC.1 della DSU/ISEE ({@code E} + 15). */
    public static final int MAX_IDENT_ISEE = 15;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    // --- lettura / scrittura -------------------------------------------------

    /** I periodi dell'exchange in forma GUI ({@link #COLONNE} colonne), ordinati per progressivo. */
    public static List<String[]> caricaPeriodi(String exchangeId) {
        List<String[]> out = new ArrayList<>();
        if (exchangeId == null || exchangeId.isBlank()) {
            return out;
        }
        for (String[] db : DatabaseH2.Pers_ExchangePeriodo_LeggiExchange(exchangeId)) {
            // db : [ExchangeId_Progressivo, ExchangeId, Progressivo, DataInizio, DataFine, Nome,
            //       StatoEstero, IdentificativoFiscale, Note, Fonte, Origine, ChiaveDefault]
            out.add(new String[] {db[2], db[3], db[4], db[5], db[6], db[7], db[8], db[9], db[10], db[11]});
        }
        return out;
    }

    /**
     * Validazione dei periodi : strutturale e semantica sulle sovrapposizioni. Regole :
     * <ul>
     *   <li>progressivo intero &gt;= 1, unico</li>
     *   <li>date, se valorizzate, in formato {@code yyyy-MM-dd} valido; inizio &lt;= fine</li>
     *   <li>codice Stato estero al massimo 3 caratteri</li>
     *   <li>identificativo fiscale al massimo {@value #MAX_IDENT_ISEE} caratteri (limite modulo FC.1 ISEE)</li>
     *   <li>i periodi non si sovrappongono : la cronologia delle entità legali / Stati esteri di un
     *       exchange è una successione, non un accavallamento. Un periodo con entrambe le date vuote
     *       è un fallback e non è in conflitto con i periodi datati.</li>
     * </ul>
     * I controlli semantici girano solo se quelli strutturali non hanno prodotto errori. I
     * <b>buchi</b> fra periodi (exchange usato, poi non usato, poi ripreso) non sono errori : vedi
     * {@link #avvisiPeriodi(List)}.
     *
     * @return elenco errori ({@code isEmpty()} = valido)
     */
    public static List<String> validaPeriodi(List<String[]> righe) {
        List<String> errori = new ArrayList<>();
        if (righe == null) {
            return errori;
        }
        Set<Integer> progVisti = new HashSet<>();
        for (int i = 0; i < righe.size(); i++) {
            String[] r = righe.get(i);
            String et = "Riga " + (i + 1) + " : ";
            if (r == null || r.length <= COL_FONTE) {
                errori.add(et + "riga incompleta.");
                continue;
            }
            String prog = trim(r[COL_PROGRESSIVO]);
            int p = -1;
            try {
                p = Integer.parseInt(prog);
            } catch (NumberFormatException e) {
                errori.add(et + "progressivo non numerico (\"" + prog + "\").");
            }
            if (p < 1 && !prog.isEmpty()) {
                errori.add(et + "progressivo deve essere >= 1.");
            }
            if (p >= 1 && !progVisti.add(p)) {
                errori.add(et + "progressivo " + p + " già usato.");
            }

            LocalDate di = dataONull(r[COL_DATA_INIZIO], et + "data inizio", errori);
            LocalDate df = dataONull(r[COL_DATA_FINE], et + "data fine", errori);
            if (di != null && df != null && di.isAfter(df)) {
                errori.add(et + "data inizio successiva alla data fine.");
            }

            if (trim(r[COL_STATO]).length() > 3) {
                errori.add(et + "il codice dello Stato estero è al massimo di 3 caratteri (tabella \"Elenco Paesi\" del modello Redditi).");
            }
            if (trim(r[COL_IDENT]).length() > MAX_IDENT_ISEE) {
                errori.add(et + "l'identificativo fiscale supera i " + MAX_IDENT_ISEE
                        + " caratteri ammessi dal modulo FC.1 dell'ISEE (un LEI a 20 va indicato nelle note).");
            }
        }
        if (errori.isEmpty()) {
            for (int i = 0; i < righe.size(); i++) {
                for (int j = i + 1; j < righe.size(); j++) {
                    if (finestreSovrapposte(righe.get(i), righe.get(j))) {
                        errori.add("I periodi " + trim(righe.get(i)[COL_PROGRESSIVO]) + " e "
                                + trim(righe.get(j)[COL_PROGRESSIVO]) + " si sovrappongono : la successione "
                                + "di entità / Stati esteri dell'exchange non può accavallarsi.");
                    }
                }
            }
        }
        return errori;
    }

    /**
     * Buchi di copertura fra periodi datati consecutivi (l'exchange usato, poi non usato per un
     * intervallo, poi ripreso) : sono <b>ammessi</b> e vanno solo segnalati, senza bloccare il
     * salvataggio. Vuoto se un periodo interamente aperto fa da copertura.
     *
     * @return elenco avvisi ({@code isEmpty()} = nessun buco)
     */
    public static List<String> avvisiPeriodi(List<String[]> righe) {
        List<String[]> valide = new ArrayList<>();
        if (righe != null) {
            for (String[] r : righe) {
                if (r != null && r.length > COL_FONTE) {
                    valide.add(r);
                }
            }
        }
        return buchiPeriodi("exchange", valide);
    }

    /** {@code true} se le finestre di due periodi si intersecano ; un periodo interamente aperto è un fallback. */
    private static boolean finestreSovrapposte(String[] a, String[] b) {
        LocalDate iA = parseSilenzioso(a[COL_DATA_INIZIO]), fA = parseSilenzioso(a[COL_DATA_FINE]);
        LocalDate iB = parseSilenzioso(b[COL_DATA_INIZIO]), fB = parseSilenzioso(b[COL_DATA_FINE]);
        boolean aAperto = iA == null && fA == null;
        boolean bAperto = iB == null && fB == null;
        if (aAperto && bAperto) {
            return true;
        }
        if (aAperto || bAperto) {
            return false;
        }
        LocalDate i1 = iA == null ? LocalDate.MIN : iA;
        LocalDate f1 = fA == null ? LocalDate.MAX : fA;
        LocalDate i2 = iB == null ? LocalDate.MIN : iB;
        LocalDate f2 = fB == null ? LocalDate.MAX : fB;
        return !i1.isAfter(f2) && !i2.isAfter(f1);
    }

    private static List<String> buchiPeriodi(String etichetta, List<String[]> righe) {
        List<String> avvisi = new ArrayList<>();
        List<LocalDate[]> range = new ArrayList<>();
        for (String[] r : righe) {
            LocalDate i = parseSilenzioso(r[COL_DATA_INIZIO]), f = parseSilenzioso(r[COL_DATA_FINE]);
            if (i == null && f == null) {
                return avvisi; // fallback interamente aperto
            }
            range.add(new LocalDate[] {i == null ? LocalDate.MIN : i, f == null ? LocalDate.MAX : f});
        }
        range.sort(java.util.Comparator.comparing(x -> x[0]));
        for (int k = 1; k < range.size(); k++) {
            LocalDate finePrec = range.get(k - 1)[1];
            LocalDate inizioSucc = range.get(k)[0];
            if (!finePrec.equals(LocalDate.MAX) && !inizioSucc.equals(LocalDate.MIN)
                    && finePrec.plusDays(1).isBefore(inizioSucc)) {
                avvisi.add("Periodo " + etichetta + " scoperto fra " + finePrec.plusDays(1) + " e "
                        + inizioSucc.minusDays(1) + " (exchange non usato in quel tratto? verificare le date).");
            }
        }
        return avvisi;
    }

    /**
     * Valida e salva i periodi dell'exchange (delete exchange + reinsert). Non scrive nulla se
     * {@link #validaPeriodi(List)} torna errori.
     *
     * @return elenco errori ({@code isEmpty()} = salvato)
     */
    public static List<String> salvaPeriodi(String exchangeId, List<String[]> righe) {
        List<String> errori = validaPeriodi(righe);
        if (exchangeId == null || exchangeId.isBlank()) {
            errori.add("Exchange non indicato.");
        }
        if (!errori.isEmpty()) {
            return errori;
        }
        DatabaseH2.Pers_ExchangePeriodo_CancellaExchange(exchangeId);
        if (righe != null) {
            for (String[] r : righe) {
                // Provenienza : chi non la porta (array a 8 col) lascia NULL = UTENTE.
                String origine = r.length > COL_ORIGINE ? nz(r[COL_ORIGINE]) : null;
                String chiaveDefault = r.length > COL_CHIAVE_DEFAULT ? nz(r[COL_CHIAVE_DEFAULT]) : null;
                DatabaseH2.Pers_ExchangePeriodo_Scrivi(exchangeId,
                        Integer.parseInt(trim(r[COL_PROGRESSIVO])),
                        nz(r[COL_DATA_INIZIO]), nz(r[COL_DATA_FINE]),
                        nz(r[COL_NOME]), nz(r[COL_STATO]), nz(r[COL_IDENT]),
                        nz(r[COL_NOTE]), nz(r[COL_FONTE]), origine, chiaveDefault);
            }
        }
        return errori;
    }

    // --- provenienza + pulsanti "Ripristina al default" -------------------

    /** Come {@link Principale_GruppiWalletRW#descrizioneOrigineRiga}. */
    public static String descrizioneOrigineRiga(String origine, String chiaveDefault) {
        return Principale_GruppiWalletRW.descrizioneOrigineRiga(origine, chiaveDefault);
    }

    /**
     * Rimette la riga {@code indice} ai valori di {@code RW_Predefiniti.json} se ha una
     * {@link #COL_CHIAVE_DEFAULT} nota (progressivo e id exchange restano quelli della riga). Non salva.
     *
     * @return {@code true} se ha ripristinato ; {@code false} se non c'è un default con quella chiave
     */
    public static boolean ripristinaRigaAlDefault(String exchangeId, List<String[]> righe, int indice) {
        if (righe == null || indice < 0 || indice >= righe.size()) {
            return false;
        }
        String[] r = righe.get(indice);
        String chiave = r.length > COL_CHIAVE_DEFAULT ? trim(r[COL_CHIAVE_DEFAULT]) : "";
        if (chiave.isEmpty()) {
            return false;
        }
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        DatiPredefinitiRW.ExchangePredef ex = p == null ? null : p.exchangePerId(exchangeId);
        if (ex == null) {
            return false;
        }
        for (DatiPredefinitiRW.PeriodoFiscalePredef pf : ex.periodiFiscali) {
            if (chiave.equals(pf.chiave)) {
                righe.set(indice, rigaDaPredef(pf, trim(r[COL_PROGRESSIVO])));
                return true;
            }
        }
        return false;
    }

    /**
     * Ricostruisce la lista GUI dei periodi fiscali di un exchange dai soli default del JSON (tutti
     * {@link #ORIGINE_SISTEMA}). Non salva.
     *
     * @return la nuova lista, o {@code null} se il JSON non è disponibile
     */
    public static List<String[]> ripristinaTuttiAlDefault(String exchangeId) {
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        DatiPredefinitiRW.ExchangePredef ex = p == null ? null : p.exchangePerId(exchangeId);
        if (ex == null) {
            return null;
        }
        List<String[]> out = new ArrayList<>();
        for (DatiPredefinitiRW.PeriodoFiscalePredef pf : ex.periodiFiscali) {
            out.add(rigaDaPredef(pf, String.valueOf(pf.progressivo)));
        }
        return out;
    }

    private static String[] rigaDaPredef(DatiPredefinitiRW.PeriodoFiscalePredef pf, String progressivo) {
        String[] r = new String[COLONNE];
        r[COL_PROGRESSIVO] = progressivo == null || progressivo.isBlank() ? String.valueOf(pf.progressivo) : progressivo;
        r[COL_DATA_INIZIO] = s(pf.dataInizio);
        r[COL_DATA_FINE] = s(pf.dataFine);
        r[COL_NOME] = s(pf.nome);
        r[COL_STATO] = s(pf.statoEstero);
        r[COL_IDENT] = s(pf.identificativoFiscale);
        r[COL_NOTE] = s(pf.note);
        r[COL_FONTE] = s(pf.fonte);
        r[COL_ORIGINE] = ORIGINE_SISTEMA;
        r[COL_CHIAVE_DEFAULT] = s(pf.chiave);
        return r;
    }

    private static String s(String v) {
        return v == null ? "" : v;
    }

    /** Prossimo progressivo libero, dato l'elenco righe già presente in tabella. */
    public static int prossimoProgressivo(List<String[]> righe) {
        int max = 0;
        if (righe != null) {
            for (String[] r : righe) {
                if (r != null && r.length > COL_PROGRESSIVO) {
                    try {
                        max = Math.max(max, Integer.parseInt(trim(r[COL_PROGRESSIVO])));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return max + 1;
    }

    // --- risoluzione per data ---------------------------------------------

    /**
     * Il periodo dell'exchange valido alla data indicata, o {@code null} se nessuno la copre.
     *
     * <p>Semantica dei periodi aperti : {@code DataInizio} vuota = valido "da sempre",
     * {@code DataFine} vuota = ancora in corso. Con {@code data == null} torna il periodo
     * <i>corrente</i> ({@code DataFine} vuota; a parità, quello con {@code DataInizio} più recente).
     * In caso di sovrapposizione vince il periodo con {@code DataInizio} più recente.</p>
     *
     * @return riga in forma GUI ({@link #COLONNE} colonne) oppure {@code null}
     */
    public static String[] periodoAllaData(String exchangeId, LocalDate data) {
        String[] migliore = null;
        LocalDate miglioreInizio = null;
        for (String[] r : caricaPeriodi(exchangeId)) {
            LocalDate di = parseSilenzioso(r[COL_DATA_INIZIO]);
            LocalDate df = parseSilenzioso(r[COL_DATA_FINE]);
            boolean copre;
            if (data == null) {
                copre = df == null; // "corrente" = fine aperta
            } else {
                copre = (di == null || !di.isAfter(data)) && (df == null || !df.isBefore(data));
            }
            if (!copre) {
                continue;
            }
            LocalDate inizioConfronto = di == null ? LocalDate.MIN : di;
            if (migliore == null || inizioConfronto.isAfter(miglioreInizio)) {
                migliore = r;
                miglioreInizio = inizioConfronto;
            }
        }
        return migliore;
    }

    /** Il periodo "corrente" dell'exchange (vedi {@link #periodoAllaData} con {@code data} = oggi, con ripiego sulla fine aperta). */
    public static String[] periodoCorrente(String exchangeId) {
        String[] p = periodoAllaData(exchangeId, LocalDate.now());
        return p != null ? p : periodoAllaData(exchangeId, null);
    }

    /** Stato estero del periodo corrente dell'exchange, {@code ""} se non risolvibile. */
    public static String statoEsteroCorrente(String exchangeId) {
        String[] p = periodoCorrente(exchangeId);
        return p == null ? "" : trim(p[COL_STATO]);
    }

    /** Identificativo fiscale del periodo corrente dell'exchange, {@code ""} se non risolvibile. */
    public static String identificativoCorrente(String exchangeId) {
        String[] p = periodoCorrente(exchangeId);
        return p == null ? "" : trim(p[COL_IDENT]);
    }

    /** Stato estero dell'exchange alla data indicata, {@code ""} se nessun periodo la copre. */
    public static String statoEsteroAllaData(String exchangeId, LocalDate data) {
        String[] p = periodoAllaData(exchangeId, data);
        return p == null ? "" : trim(p[COL_STATO]);
    }

    /** Identificativo fiscale dell'exchange alla data indicata, {@code ""} se nessun periodo la copre. */
    public static String identificativoAllaData(String exchangeId, LocalDate data) {
        String[] p = periodoAllaData(exchangeId, data);
        return p == null ? "" : trim(p[COL_IDENT]);
    }

    public static int numeroPeriodi(String exchangeId) {
        return caricaPeriodi(exchangeId).size();
    }

    /**
     * Sintesi dello stato fiscale dell'exchange per le combo / colonne della GUI :
     * {@code "nessun periodo"}, {@code "stato XXX"} (un solo periodo) oppure
     * {@code "più periodi, corrente XXX"}.
     */
    public static String descriviCorrente(String exchangeId) {
        List<String[]> periodi = caricaPeriodi(exchangeId);
        if (periodi.isEmpty()) {
            return "nessun periodo";
        }
        String corrente = statoEsteroCorrente(exchangeId);
        if (periodi.size() == 1) {
            return "stato " + (corrente.isEmpty() ? "?" : corrente);
        }
        return "più periodi, corrente " + (corrente.isEmpty() ? "?" : corrente);
    }

    // --- helper ---------------------------------------------------------

    private static LocalDate dataONull(String v, String etichetta, List<String> errori) {
        String s = trim(v);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s, ISO);
        } catch (DateTimeParseException e) {
            errori.add(etichetta + " : formato non valido, atteso yyyy-MM-dd (\"" + s + "\").");
            return null;
        }
    }

    private static LocalDate parseSilenzioso(String v) {
        String s = trim(v);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s, ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nz(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }
}
