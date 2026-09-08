/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica operativa della configurazione del quadro W/RW per i gruppi wallet:
 * riferimento estero del gruppo (stato / P.IVA o exchange) e periodi di detenzione
 * (righi CRYPTO / FIAT, valori campo 7-8 manuali, modalità di calcolo).
 *
 * <p>Companion di {@code Principale} nello stile di {@link Principale_GiacenzeaData}:
 * metodi {@code public static}, nessun campo Swing, nessun riferimento a {@code Principale}.
 * Persistenza via {@code DatabaseH2.Pers_ExchangeAnagrafica_* / Pers_GruppoRiferimento_* /
 * Pers_GruppoPeriodoRW_*} (Fase 1).</p>
 *
 * <p><b>Stato.</b> Il riferimento estero e i periodi CRYPTO non sono ancora letti da
 * {@code Calcoli_RW.AggiornaRWFR}. I periodi <b>FIAT</b> sono invece consumati da
 * {@link Calcoli_RW_Fiat} (parte FIAT del quadro W/RW, Fase 3). La validazione di
 * {@link #validaPeriodi(List)} è strutturale <i>più</i> semantica sulle
 * <b>sovrapposizioni</b> (errore bloccante : le finestre di detenzione dello stesso rigo non
 * possono accavallarsi) ; i <b>buchi</b> sono ammessi (conto chiuso e poi riaperto) e vengono
 * solo segnalati da {@link #avvisiPeriodi(List)}, senza bloccare il salvataggio.</p>
 */
public class Principale_GruppiWalletRW {

    private Principale_GruppiWalletRW() {
    }

    // --- valori ammessi -------------------------------------------------------

    public static final String TIPO_CRYPTO = "CRYPTO";
    public static final String TIPO_FIAT = "FIAT";

    public static final String MOD_INIZIALE_PRIMO_APPORTO = "PRIMO_APPORTO";
    public static final String MOD_INIZIALE_SOMMA_APPORTI = "SOMMA_APPORTI_GIORNO";
    public static final String MOD_FINALE_ULTIMA_USCITA = "ULTIMA_USCITA";
    public static final String MOD_FINALE_SOMMA_USCITE = "SOMMA_USCITE_GIORNO";

    public static final String RIFERIMENTO_STATO = "STATO";
    public static final String RIFERIMENTO_EXCHANGE = "EXCHANGE";

    /** Bollo pagato dall'intermediario/exchange nel singolo periodo. */
    public static final String BOLLO_SI = "SI";
    public static final String BOLLO_NO = "NO";

    /** Etichette leggibili per le combo della GUI, nell'ordine {codice, etichetta}. */
    public static final String[][] MODALITA_INIZIALE = {
        {"", "(automatico / non impostato)"},
        {MOD_INIZIALE_PRIMO_APPORTO, "Primo apporto della giornata + residuo"},
        {MOD_INIZIALE_SOMMA_APPORTI, "Somma degli apporti della giornata + residuo"},
    };
    public static final String[][] MODALITA_FINALE = {
        {"", "(automatico / non impostato)"},
        {MOD_FINALE_ULTIMA_USCITA, "Ultima uscita della giornata + residuo"},
        {MOD_FINALE_SOMMA_USCITE, "Somma delle uscite della giornata + residuo"},
    };

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    // --- riferimento estero del gruppo -------------------------------------

    /**
     * Riga della GUI "periodi" : {@code [TipoRigo, Progressivo, DataInizio, DataFine,
     * ValoreInizialeManuale, NotaValoreIniziale, ValoreFinaleManuale, NotaValoreFinale,
     * ModalitaCalcoloIniziale, ModalitaCalcoloFinale, PagaBolloPeriodo, Origine, ChiaveDefault]} —
     * 13 colonne, senza chiave sintetica né gruppo. {@link #COL_ORIGINE} / {@link #COL_CHIAVE_DEFAULT}
     * sono in coda apposta : i motori RW leggono i periodi per indice ({@code COL_*}) e nessuno di
     * quegli indici si è spostato.
     */
    public static final int COL_TIPO = 0, COL_PROGRESSIVO = 1, COL_DATA_INIZIO = 2, COL_DATA_FINE = 3,
            COL_VAL_INIZIALE = 4, COL_NOTA_INIZIALE = 5, COL_VAL_FINALE = 6, COL_NOTA_FINALE = 7,
            COL_MOD_INIZIALE = 8, COL_MOD_FINALE = 9, COL_BOLLO = 10, COL_ORIGINE = 11, COL_CHIAVE_DEFAULT = 12;
    public static final int COLONNE_PERIODO = 13;

    /** Provenienza di una riga : seminata da {@code RW_Predefiniti.json} e mai più toccata. */
    public static final String ORIGINE_SISTEMA = "SISTEMA";
    /** Provenienza di una riga : creata o modificata a mano (o {@code null} sui DB pre-esistenti). */
    public static final String ORIGINE_UTENTE = "UTENTE";

    /** {@code true} se {@code v} è {@code null}/vuoto o vale esplicitamente {@link #ORIGINE_UTENTE}. */
    public static boolean origineUtente(String v) {
        return v == null || v.isBlank() || ORIGINE_UTENTE.equals(v.trim());
    }

    private static final java.util.regex.Pattern NUM_GRUPPO =
            java.util.regex.Pattern.compile("(?i)wallet\\s+(\\d+)");

    /** Numero di un nome gruppo {@code "Wallet <n>"}, o {@code null} se non segue lo schema. */
    private static Integer numeroGruppo(String s) {
        if (s == null) {
            return null;
        }
        java.util.regex.Matcher m = NUM_GRUPPO.matcher(s.trim());
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    /**
     * Ordina i nomi di gruppo {@code "Wallet <n>"} per valore <b>numerico</b> ({@code "Wallet 2"} &lt;
     * {@code "Wallet 10"} &lt; {@code "Wallet 101"}), invece dell'ordine lessicografico che infilerebbe
     * {@code "Wallet 101".."Wallet 114"} fra {@code "Wallet 10"} e {@code "Wallet 11"}. I nomi che non
     * seguono lo schema {@code "Wallet <n>"} vanno in coda, in ordine alfabetico case-insensitive.
     */
    public static final Comparator<String> ORDINE_GRUPPO = (a, b) -> {
        Integer na = numeroGruppo(a), nb = numeroGruppo(b);
        if (na != null && nb != null) {
            return Integer.compare(na, nb);
        }
        if (na != null) {
            return -1;
        }
        if (nb != null) {
            return 1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(a == null ? "" : a, b == null ? "" : b);
    };

    /**
     * Etichetta leggibile di un gruppo wallet per titoli e label : {@code "Wallet 101 — Coinbase"}
     * quando l'alias è diverso dal nome del gruppo, altrimenti il solo nome del gruppo.
     */
    public static String etichettaGruppo(String gruppo) {
        if (gruppo == null || gruppo.isBlank()) {
            return "";
        }
        String[] a = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        String alias = a != null && a[1] != null ? a[1].trim() : "";
        return alias.isEmpty() || alias.equalsIgnoreCase(gruppo.trim()) ? gruppo : gruppo + " — " + alias;
    }

    /**
     * Testo di sintesi del riferimento estero di un gruppo, per la colonna della tabella
     * "Gruppi Wallet". Non lancia mai : su dati incoerenti torna una dicitura neutra.
     */
    public static String descriviRiferimento(String gruppo) {
        if (gruppo == null || gruppo.isBlank()) {
            return "";
        }
        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (r[0] == null || r[1] == null || r[1].isBlank()) {
            return "— non impostato";
        }
        if (RIFERIMENTO_EXCHANGE.equals(r[1])) {
            String exId = r[4];
            if (exId == null || exId.isBlank()) {
                return "exchange (da scegliere)";
            }
            String[] ex = DatabaseH2.Pers_ExchangeAnagrafica_Leggi(exId);
            String nome = ex[1] != null && !ex[1].isBlank() ? ex[1] : exId;
            // Stato / identificativo non stanno più nell'anagrafica ma nei periodi (EXCHANGE_PERIODO).
            return "exchange : " + nome + " (" + Principale_PeriodiExchange.descriviCorrente(exId) + ")";
        }
        // STATO manuale
        String stato = r[2] != null && !r[2].isBlank() ? r[2] : "?";
        String piva = r[3] != null && !r[3].isBlank() ? " · " + r[3] : "";
        return "stato " + stato + piva;
    }

    /** Stato estero effettivo del gruppo (risolve STATO vs EXCHANGE, periodo <i>corrente</i> dell'exchange). {@code ""} se non impostato. */
    public static String statoEsteroEffettivo(String gruppo) {
        return statoEsteroEffettivo(gruppo, null);
    }

    /**
     * Stato estero effettivo del gruppo a una certa data (per l'exchange risolve il periodo di
     * {@code EXCHANGE_PERIODO} valido a quella data). {@code data == null} = periodo corrente.
     * {@code ""} se non impostato / nessun periodo copre la data.
     */
    public static String statoEsteroEffettivo(String gruppo, LocalDate data) {
        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (r[1] == null) {
            return "";
        }
        if (RIFERIMENTO_EXCHANGE.equals(r[1]) && r[4] != null && !r[4].isBlank()) {
            return data == null
                    ? Principale_PeriodiExchange.statoEsteroCorrente(r[4])
                    : Principale_PeriodiExchange.statoEsteroAllaData(r[4], data);
        }
        return r[2] == null ? "" : r[2];
    }

    /** Identificativo fiscale / P.IVA effettivo del gruppo (per l'ISEE futura), periodo <i>corrente</i>. {@code ""} se non impostato. */
    public static String identificativoFiscaleEffettivo(String gruppo) {
        return identificativoFiscaleEffettivo(gruppo, null);
    }

    /**
     * Identificativo fiscale effettivo del gruppo a una certa data (per l'exchange risolve il periodo
     * di {@code EXCHANGE_PERIODO} valido a quella data). {@code data == null} = periodo corrente.
     */
    public static String identificativoFiscaleEffettivo(String gruppo, LocalDate data) {
        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (r[1] == null) {
            return "";
        }
        if (RIFERIMENTO_EXCHANGE.equals(r[1]) && r[4] != null && !r[4].isBlank()) {
            return data == null
                    ? Principale_PeriodiExchange.identificativoCorrente(r[4])
                    : Principale_PeriodiExchange.identificativoAllaData(r[4], data);
        }
        return r[3] == null ? "" : r[3];
    }

    /**
     * Salva il riferimento estero di un gruppo.
     *
     * @return lista di errori ({@code isEmpty()} = salvato). Non scrive nulla se ci sono errori.
     */
    public static List<String> salvaRiferimento(String gruppo, String modalita, String statoEstero,
            String identificativoFiscale, String exchangeId) {
        List<String> errori = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank()) {
            errori.add("Gruppo non indicato.");
            return errori;
        }
        boolean stato = RIFERIMENTO_STATO.equals(modalita);
        boolean exchange = RIFERIMENTO_EXCHANGE.equals(modalita);
        if (!stato && !exchange) {
            errori.add("Modalità non valida : usare \"" + RIFERIMENTO_STATO + "\" o \"" + RIFERIMENTO_EXCHANGE + "\".");
            return errori;
        }
        if (stato) {
            String s = statoEstero == null ? "" : statoEstero.trim();
            if (s.isEmpty()) {
                errori.add("Con modalità \"stato\" il codice dello Stato estero è obbligatorio.");
            } else if (s.length() > 3) {
                errori.add("Il codice dello Stato estero è al massimo di 3 caratteri (tabella \"Elenco Paesi\" del modello Redditi).");
            }
        }
        if (exchange) {
            String e = exchangeId == null ? "" : exchangeId.trim();
            if (e.isEmpty()) {
                errori.add("Con modalità \"exchange\" occorre scegliere un exchange.");
            } else if (DatabaseH2.Pers_ExchangeAnagrafica_Leggi(e)[0] == null) {
                errori.add("L'exchange \"" + e + "\" non è nell'anagrafica.");
            }
        }
        if (!errori.isEmpty()) {
            return errori;
        }
        DatabaseH2.Pers_GruppoRiferimento_Scrivi(gruppo, modalita,
                stato ? nz(statoEstero) : null,
                stato ? nz(identificativoFiscale) : null,
                exchange ? nz(exchangeId) : null);
        return errori;
    }

    public static void cancellaRiferimento(String gruppo) {
        DatabaseH2.Pers_GruppoRiferimento_Cancella(gruppo);
    }

    // --- periodi di detenzione -------------------------------------------

    /** I periodi del gruppo in forma GUI (10 colonne, vedi {@link #COLONNE_PERIODO}), ordinati per tipo/progressivo. */
    public static List<String[]> caricaPeriodi(String gruppo) {
        List<String[]> out = new ArrayList<>();
        for (String[] db : DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo(gruppo)) {
            // db : [Gruppo_Tipo_Prog, Gruppo, TipoRigo, Progressivo, DataInizio, DataFine,
            //       ValIniManuale, NotaIni, ValFinManuale, NotaFin, ModIni, ModFin, PagaBolloPeriodo,
            //       Origine, ChiaveDefault]
            out.add(new String[] {
                db[2], db[3], db[4], db[5], db[6], db[7], db[8], db[9], db[10], db[11], db[12], db[13], db[14]
            });
        }
        return out;
    }

    /**
     * Validazione dei periodi : strutturale e semantica sulle sovrapposizioni. Regole :
     * <ul>
     *   <li>tipo ∈ {CRYPTO, FIAT}</li>
     *   <li>progressivo intero >= 1, unico per (tipo)</li>
     *   <li>date, se valorizzate, in formato {@code yyyy-MM-dd} valido</li>
     *   <li>data inizio <= data fine quando entrambe presenti</li>
     *   <li>modalità di calcolo ∈ valori ammessi (o vuota)</li>
     *   <li>i periodi <b>datati</b> dello <b>stesso tipo</b> non si sovrappongono (finestre che si
     *       intersecano = errore). Un periodo con entrambe le date vuote è un fallback e non è in
     *       conflitto con i periodi datati ; averne più d'uno per tipo è solo un avviso.</li>
     * </ul>
     * I controlli semantici girano solo se quelli strutturali non hanno prodotto errori. I
     * <b>buchi</b> fra periodi e i periodi senza date in eccesso non sono errori (vedi
     * {@link #avvisiPeriodi(List)}).
     *
     * @param righe righe in forma GUI ({@link #COLONNE_PERIODO} colonne)
     * @return elenco errori ({@code isEmpty()} = valido)
     */
    public static List<String> validaPeriodi(List<String[]> righe) {
        List<String> errori = new ArrayList<>();
        if (righe == null) {
            return errori;
        }
        Set<String> tipoProg = new HashSet<>();
        Set<String> modIni = valoriAmmessi(MODALITA_INIZIALE);
        Set<String> modFin = valoriAmmessi(MODALITA_FINALE);

        for (int i = 0; i < righe.size(); i++) {
            String[] r = righe.get(i);
            String et = "Riga " + (i + 1) + " : ";
            // il bollo (COL_BOLLO) è opzionale : una riga a 10 colonne resta valida (bollo assente)
            if (r == null || r.length <= COL_MOD_FINALE) {
                errori.add(et + "riga incompleta.");
                continue;
            }
            String tipo = trim(r[COL_TIPO]);
            if (!TIPO_CRYPTO.equals(tipo) && !TIPO_FIAT.equals(tipo)) {
                errori.add(et + "tipo deve essere " + TIPO_CRYPTO + " o " + TIPO_FIAT + ".");
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
            if (p >= 1 && !tipoProg.add(tipo + "#" + p)) {
                errori.add(et + "progressivo " + p + " già usato per un periodo " + tipo + ".");
            }

            LocalDate di = dataONull(r[COL_DATA_INIZIO], et + "data inizio", errori);
            LocalDate df = dataONull(r[COL_DATA_FINE], et + "data fine", errori);
            if (di != null && df != null && di.isAfter(df)) {
                errori.add(et + "data inizio successiva alla data fine.");
            }

            String mi = trim(r[COL_MOD_INIZIALE]);
            if (!mi.isEmpty() && !modIni.contains(mi)) {
                errori.add(et + "modalità di calcolo iniziale non riconosciuta (\"" + mi + "\").");
            }
            String mf = trim(r[COL_MOD_FINALE]);
            if (!mf.isEmpty() && !modFin.contains(mf)) {
                errori.add(et + "modalità di calcolo finale non riconosciuta (\"" + mf + "\").");
            }

            // Il bollo riguarda solo il rigo CRYPTO : per il rigo FIAT non è dovuto e il valore è ignorato.
            if (TIPO_CRYPTO.equals(tipo)) {
                String bollo = trim(r.length > COL_BOLLO ? r[COL_BOLLO] : "");
                if (!bollo.isEmpty() && !BOLLO_SI.equals(bollo) && !BOLLO_NO.equals(bollo)) {
                    errori.add(et + "valore bollo non riconosciuto (\"" + bollo + "\"), atteso " + BOLLO_SI + " o " + BOLLO_NO + ".");
                }
            }
        }
        // Semantico : solo se la struttura è valida (altrimenti le date potrebbero non essere parsabili).
        if (errori.isEmpty()) {
            errori.addAll(sovrapposizioniPeriodi(righe));
        }
        return errori;
    }

    /**
     * Buchi di copertura fra periodi datati consecutivi dello stesso tipo : sono <b>ammessi</b>
     * (un conto può essere chiuso e poi riaperto più avanti) e vanno solo segnalati all'utente,
     * <b>senza</b> bloccare il salvataggio. Se per un tipo esiste un periodo interamente aperto
     * (entrambe le date vuote) che fa da copertura, non si segnala nulla.
     *
     * @param righe righe in forma GUI ({@link #COLONNE_PERIODO} colonne)
     * @return elenco avvisi ({@code isEmpty()} = nessun buco)
     */
    public static List<String> avvisiPeriodi(List<String[]> righe) {
        List<String> avvisi = new ArrayList<>();
        if (righe == null) {
            return avvisi;
        }
        Map<String, List<String[]>> perTipo = new java.util.LinkedHashMap<>();
        for (String[] r : righe) {
            if (r == null || r.length <= COL_DATA_FINE) {
                continue;
            }
            perTipo.computeIfAbsent(trim(r[COL_TIPO]), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<String[]>> e : perTipo.entrySet()) {
            int aperti = 0;
            for (String[] r : e.getValue()) {
                if (parseData(r[COL_DATA_INIZIO]) == null && parseData(r[COL_DATA_FINE]) == null) {
                    aperti++;
                }
            }
            if (aperti > 1) {
                avvisi.add("Più di un periodo " + e.getKey() + " senza date : per la generazione dei "
                        + "righi conterà solo quello con progressivo più basso, dai una finestra agli altri.");
            }
            avvisi.addAll(buchiPeriodi(e.getKey(), e.getValue(), COL_DATA_INIZIO, COL_DATA_FINE));
        }
        return avvisi;
    }

    /** Coppie di periodi <b>datati</b> dello stesso tipo con finestre che si intersecano. */
    private static List<String> sovrapposizioniPeriodi(List<String[]> righe) {
        List<String> errori = new ArrayList<>();
        Map<String, List<String[]>> perTipo = new java.util.LinkedHashMap<>();
        for (String[] r : righe) {
            if (r == null || r.length <= COL_DATA_FINE) {
                continue;
            }
            perTipo.computeIfAbsent(trim(r[COL_TIPO]), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<String[]>> e : perTipo.entrySet()) {
            List<String[]> g = e.getValue();
            for (int i = 0; i < g.size(); i++) {
                for (int j = i + 1; j < g.size(); j++) {
                    if (finestreSovrapposte(g.get(i), g.get(j), COL_DATA_INIZIO, COL_DATA_FINE)) {
                        errori.add("I periodi " + e.getKey() + " " + trim(g.get(i)[COL_PROGRESSIVO])
                                + " e " + trim(g.get(j)[COL_PROGRESSIVO]) + " si sovrappongono : le finestre "
                                + "di detenzione dello stesso rigo non possono accavallarsi.");
                    }
                }
            }
        }
        return errori;
    }

    // --- algebra sugli intervalli (semantica dei periodi aperti : data vuota = -inf / +inf) ------

    /**
     * {@code true} se le finestre temporali di <b>due periodi datati</b> si intersecano. Un periodo
     * con entrambe le date vuote è un fallback e non entra mai in conflitto (i periodi datati vincono
     * sulle loro finestre) : più periodi interamente aperti dello stesso tipo sono solo un avviso
     * ({@link #avvisiPeriodi(List)}), non un errore, perché il flusso "bollo per periodo" ne crea di
     * legittimi prima che l'utente li dati.
     */
    static boolean finestreSovrapposte(String[] a, String[] b, int colInizio, int colFine) {
        java.time.LocalDate iA = parseData(a[colInizio]), fA = parseData(a[colFine]);
        java.time.LocalDate iB = parseData(b[colInizio]), fB = parseData(b[colFine]);
        if ((iA == null && fA == null) || (iB == null && fB == null)) {
            return false;
        }
        java.time.LocalDate i1 = iA == null ? java.time.LocalDate.MIN : iA;
        java.time.LocalDate f1 = fA == null ? java.time.LocalDate.MAX : fA;
        java.time.LocalDate i2 = iB == null ? java.time.LocalDate.MIN : iB;
        java.time.LocalDate f2 = fB == null ? java.time.LocalDate.MAX : fB;
        return !i1.isAfter(f2) && !i2.isAfter(f1);
    }

    /** Buchi fra le finestre di un insieme di periodi (ordinate per inizio). Vuoto se un periodo è interamente aperto. */
    static List<String> buchiPeriodi(String etichetta, List<String[]> righe, int colInizio, int colFine) {
        List<String> avvisi = new ArrayList<>();
        List<java.time.LocalDate[]> range = new ArrayList<>();
        for (String[] r : righe) {
            java.time.LocalDate i = parseData(r[colInizio]), f = parseData(r[colFine]);
            if (i == null && f == null) {
                return avvisi; // fallback interamente aperto : copre ogni buco
            }
            range.add(new java.time.LocalDate[] {
                i == null ? java.time.LocalDate.MIN : i,
                f == null ? java.time.LocalDate.MAX : f
            });
        }
        range.sort(Comparator.comparing(x -> x[0]));
        for (int k = 1; k < range.size(); k++) {
            java.time.LocalDate finePrec = range.get(k - 1)[1];
            java.time.LocalDate inizioSucc = range.get(k)[0];
            if (!finePrec.equals(java.time.LocalDate.MAX) && !inizioSucc.equals(java.time.LocalDate.MIN)
                    && finePrec.plusDays(1).isBefore(inizioSucc)) {
                avvisi.add("Periodo " + etichetta + " scoperto fra " + finePrec.plusDays(1) + " e "
                        + inizioSucc.minusDays(1) + " (conto chiuso e poi riaperto? verificare le date).");
            }
        }
        return avvisi;
    }

    /** {@code yyyy-MM-dd} → {@code LocalDate}, oppure {@code null} se vuoto o non valido (senza sollevare). */
    static java.time.LocalDate parseData(String v) {
        String s = trim(v);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(s, ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Valida e salva i periodi del gruppo (delete gruppo + reinsert). Non scrive nulla se
     * {@link #validaPeriodi(List)} torna errori.
     *
     * @return elenco errori ({@code isEmpty()} = salvato)
     */
    public static List<String> salvaPeriodi(String gruppo, List<String[]> righe) {
        List<String> errori = validaPeriodi(righe);
        if (!errori.isEmpty()) {
            return errori;
        }
        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo(gruppo);
        if (righe != null) {
            for (String[] r : righe) {
                // bollo scritto solo per il rigo CRYPTO : sul rigo FIAT non è dovuto -> sempre null
                String bollo = TIPO_CRYPTO.equals(trim(r[COL_TIPO]))
                        ? (r.length > COL_BOLLO ? r[COL_BOLLO] : null) : null;
                // Provenienza : chi non la porta (chiamate legacy con array a 11 col) lascia NULL = UTENTE.
                String origine = r.length > COL_ORIGINE ? nz(r[COL_ORIGINE]) : null;
                String chiaveDefault = r.length > COL_CHIAVE_DEFAULT ? r[COL_CHIAVE_DEFAULT] : null;
                DatabaseH2.Pers_GruppoPeriodoRW_Scrivi(gruppo,
                        trim(r[COL_TIPO]),
                        Integer.parseInt(trim(r[COL_PROGRESSIVO])),
                        nz(r[COL_DATA_INIZIO]), nz(r[COL_DATA_FINE]),
                        nz(r[COL_VAL_INIZIALE]), nz(r[COL_NOTA_INIZIALE]),
                        nz(r[COL_VAL_FINALE]), nz(r[COL_NOTA_FINALE]),
                        nz(r[COL_MOD_INIZIALE]), nz(r[COL_MOD_FINALE]),
                        nz(bollo), origine, chiaveDefault == null ? null : chiaveDefault.trim());
            }
        }
        return errori;
    }

    /** Prossimo progressivo libero per un tipo, dato l'elenco righe già presente in tabella. */
    public static int prossimoProgressivo(List<String[]> righe, String tipo) {
        int max = 0;
        if (righe != null) {
            for (String[] r : righe) {
                if (r != null && r.length > COL_PROGRESSIVO && tipo.equals(trim(r[COL_TIPO]))) {
                    try {
                        max = Math.max(max, Integer.parseInt(trim(r[COL_PROGRESSIVO])));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return max + 1;
    }

    // --- bollo per periodo --------------------------------------------

    public static final String BOLLO_STATO_NESSUNO = "NESSUNO";
    public static final String BOLLO_STATO_TUTTI_SI = "TUTTI_SI";
    public static final String BOLLO_STATO_TUTTI_NO = "TUTTI_NO";
    public static final String BOLLO_STATO_MISTI = "MISTI";

    /** {@code SI} se il flag per-gruppo {@code GRUPPO_ALIAS.PagaBollo} è attivo, altrimenti {@code NO}. */
    public static String bolloDefaultGruppo(String gruppo) {
        if (gruppo == null || gruppo.isBlank()) {
            return BOLLO_NO;
        }
        String[] a = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        return a[2] != null && a[2].equals("S") ? BOLLO_SI : BOLLO_NO;
    }

    private static String normalizzaBollo(String v) {
        return BOLLO_SI.equals(trim(v)) ? BOLLO_SI : BOLLO_NO;
    }

    /**
     * Stato del bollo sui periodi <b>CRYPTO</b> del gruppo (sul rigo FIAT il bollo non è dovuto) :
     * {@link #BOLLO_STATO_NESSUNO} (nessun periodo CRYPTO), {@link #BOLLO_STATO_TUTTI_SI} /
     * {@link #BOLLO_STATO_TUTTI_NO} (uniforme), {@link #BOLLO_STATO_MISTI} (periodi con impostazione
     * diversa fra loro).
     */
    public static String statoBolloPeriodi(String gruppo) {
        boolean visto_si = false, visto_no = false;
        for (String[] r : caricaPeriodi(gruppo)) {
            if (!TIPO_CRYPTO.equals(trim(r[COL_TIPO]))) {
                continue;
            }
            if (BOLLO_SI.equals(normalizzaBollo(r.length > COL_BOLLO ? r[COL_BOLLO] : null))) {
                visto_si = true;
            } else {
                visto_no = true;
            }
        }
        if (!visto_si && !visto_no) {
            return BOLLO_STATO_NESSUNO;
        }
        if (visto_si && visto_no) {
            return BOLLO_STATO_MISTI;
        }
        return visto_si ? BOLLO_STATO_TUTTI_SI : BOLLO_STATO_TUTTI_NO;
    }

    /** {@code true} quando i periodi del gruppo non concordano sul bollo (il toggle per-gruppo va bloccato). */
    public static boolean periodiBolloIncoerenti(String gruppo) {
        return BOLLO_STATO_MISTI.equals(statoBolloPeriodi(gruppo));
    }

    /**
     * Testo (HTML) per il tooltip del toggle "Bollo Pagato dall'Intermediario" quando è disabilitato :
     * elenco dei periodi con lo stato del bollo di ciascuno.
     */
    public static String descriviBolloPeriodi(String gruppo) {
        StringBuilder sb = new StringBuilder("<html>Bollo gestito per periodo (solo righi CRYPTO), valori non uniformi :<br>");
        for (String[] r : caricaPeriodi(gruppo)) {
            String tipo = trim(r[COL_TIPO]);
            if (!TIPO_CRYPTO.equals(tipo)) {
                continue;
            }
            String prog = trim(r[COL_PROGRESSIVO]);
            String di = trim(r[COL_DATA_INIZIO]);
            String df = trim(r[COL_DATA_FINE]);
            String periodo = di.isEmpty() && df.isEmpty() ? "(date da dedurre)"
                    : (di.isEmpty() ? "..." : di) + " - " + (df.isEmpty() ? "in corso" : df);
            String bollo = BOLLO_SI.equals(normalizzaBollo(r.length > COL_BOLLO ? r[COL_BOLLO] : null))
                    ? "paga bollo" : "non paga bollo";
            sb.append("&nbsp;&nbsp;").append(tipo).append(' ').append(prog)
                    .append(" (").append(periodo).append(") : ").append(bollo).append("<br>");
        }
        sb.append("Modifica dal pulsante \"Periodi di detenzione...\".</html>");
        return sb.toString();
    }

    /**
     * Riscrive il bollo di tutti i periodi <b>CRYPTO</b> del gruppo con un unico valore (usato quando
     * l'utente cambia il toggle per-gruppo e i periodi erano già uniformi). No-op se il gruppo non ha
     * periodi CRYPTO.
     */
    public static void propagaBolloAiPeriodi(String gruppo, boolean pagaBollo) {
        List<String[]> periodi = caricaPeriodi(gruppo);
        boolean qualcosa = false;
        String v = pagaBollo ? BOLLO_SI : BOLLO_NO;
        for (String[] r : periodi) {
            if (TIPO_CRYPTO.equals(trim(r[COL_TIPO])) && r.length > COL_BOLLO) {
                r[COL_BOLLO] = v;
                // È una scelta esplicita dell'utente sul bollo di quel periodo : la riga diventa
                // personalizzata, così il prossimo reconcile del JSON non la sovrascrive.
                if (r.length > COL_ORIGINE) {
                    r[COL_ORIGINE] = ORIGINE_UTENTE;
                }
                qualcosa = true;
            }
        }
        if (qualcosa) {
            salvaPeriodi(gruppo, periodi);
        }
    }

    /** Allinea il flag per-gruppo {@code GRUPPO_ALIAS.PagaBollo} al valore comune dei periodi. No-op se il gruppo non esiste. */
    public static void allineaBolloGruppo(String gruppo, boolean pagaBollo) {
        String[] a = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        if (a[0] == null) {
            return;
        }
        DatabaseH2.Pers_GruppoAlias_Scrivi(a[0], a[1], pagaBollo);
    }

    // --- gruppi wallet preconfigurati per exchange -----------------------

    /** Gruppo assegnato d'ufficio agli exchange non ancora classificati. */
    public static final String GRUPPO_NON_CLASSIFICATI = "Wallet 99";

    /** Opzione ({@code personale.mv.db}) : nomi sorgente exchange già valutati dall'auto-associazione. */
    private static final String OPZIONE_AUTOGRUPPO_FATTI = "EXCHANGE_AUTOGRUPPO_FATTI";

    /**
     * Opzione ({@code personale.mv.db}) : {@code "SI"} dopo il congelamento una-tantum del pregresso
     * ({@link #congelaPreesistentiSeNecessario()}). Serve a far scattare quel congelamento una sola volta.
     */
    private static final String OPZIONE_AUTOGRUPPO_INIZIALIZZATO = "EXCHANGE_AUTOGRUPPO_INIZIALIZZATO";

    /** Nome sorgente del movimento (campo {@code [3]}) normalizzato → id exchange di {@code EXCHANGE_ANAGRAFICA}. */
    private static final Map<String, String> SORGENTE_A_EXCHANGE = creaMappaSorgenti();

    private static Map<String, String> creaMappaSorgenti() {
        Map<String, String> m = new HashMap<>();
        for (String[] e : DatabaseH2.EXCHANGE_NOTI) {
            m.put(e[0], e[0]);                          // id stesso ("okx")
            m.put(normalizzaSorgente(e[1]), e[0]);      // nome visualizzato ("crypto.com" → cryptocom)
        }
        // varianti note viste negli import e nell'inserimento manuale
        m.put("crypto.com app", "cryptocom");
        m.put("crypto.com exchange", "cryptocom");
        m.put("crypto com", "cryptocom");
        m.put("cdc", "cryptocom");
        m.put("okex", "okx");
        // "coinbase pro" / "gdax" restano volutamente FUORI : l'applicazione li tiene come wallet a sé.
        return m;
    }

    private static String normalizzaSorgente(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    /** Id exchange corrispondente al nome sorgente {@code [3]} del movimento, o {@code ""} se non riconosciuto. */
    public static String exchangeIdDaSorgente(String nomeSorgente) {
        String id = SORGENTE_A_EXCHANGE.get(normalizzaSorgente(nomeSorgente));
        return id == null ? "" : id;
    }

    /** Nome del gruppo wallet preconfigurato per un id exchange (es. {@code "Wallet 102"}), o {@code ""}. */
    public static String gruppoPreconfigurato(String exchangeId) {
        if (exchangeId == null) {
            return "";
        }
        String id = exchangeId.trim();
        for (int i = 0; i < DatabaseH2.EXCHANGE_NOTI.length; i++) {
            if (DatabaseH2.EXCHANGE_NOTI[i][0].equalsIgnoreCase(id)) {
                return "Wallet " + (DatabaseH2.GRUPPO_PRECONF_BASE + i);
            }
        }
        return "";
    }

    /** Nomi già registrati nel marcatore {@value #OPZIONE_AUTOGRUPPO_FATTI} (uno per riga). */
    private static Set<String> leggiMarcatoreFatti() {
        Set<String> fatti = new LinkedHashSet<>();
        String raw = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_AUTOGRUPPO_FATTI);
        if (raw != null && !raw.isBlank()) {
            for (String s : raw.split("\n")) {
                if (!s.isBlank()) {
                    fatti.add(s.trim());
                }
            }
        }
        return fatti;
    }

    /**
     * Congelamento <b>una-tantum</b> del pregresso : alla prima esecuzione dopo l'introduzione della
     * feature marca come "già valutati" tutti i nomi sorgente exchange <i>noti</i> che hanno già
     * un'associazione in {@code WALLETGRUPPO} (di norma {@link #GRUPPO_NON_CLASSIFICATI}, scritto
     * automaticamente dal primo calcolo). Così {@link #autoAssociaGruppiPreconfigurati} non sposta
     * mai da sola i wallet di un archivio <b>esistente</b> : la ri-suddivisione dei righi del quadro
     * RW resta una scelta esplicita dell'utente (pulsante "Raggruppa exchange noti…" in
     * <i>Opzioni → Gruppi Wallet</i>).
     *
     * <p>Su un archivio <b>nuovo</b> ({@code WALLETGRUPPO} vuoto) non congela nulla e l'auto-associazione
     * resta trasparente dal primo import. Guardia {@value #OPZIONE_AUTOGRUPPO_INIZIALIZZATO} : gira una
     * volta sola. Va chiamato <b>prima</b> di ogni {@code autoAssociaGruppiPreconfigurati}.</p>
     */
    public static void congelaPreesistentiSeNecessario() {
        if ("SI".equals(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_AUTOGRUPPO_INIZIALIZZATO))) {
            return;
        }
        Set<String> fatti = leggiMarcatoreFatti();
        for (String w : DatabaseH2.Pers_GruppoWallet_LeggiTuttiIWallet()) {
            if (w != null && !w.isBlank() && !exchangeIdDaSorgente(w).isEmpty()) {
                fatti.add(w.trim());
            }
        }
        if (!fatti.isEmpty()) {
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_AUTOGRUPPO_FATTI, String.join("\n", fatti));
        }
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_AUTOGRUPPO_INIZIALIZZATO, "SI");
    }

    /**
     * Associa a un gruppo wallet preconfigurato gli exchange che compaiono per la prima volta.
     *
     * <p>Per ogni nome sorgente ({@code [3]} del movimento) riconosciuto e mai valutato prima
     * (marcatore persistente {@value #OPZIONE_AUTOGRUPPO_FATTI}) : se l'associazione attuale in
     * {@code WALLETGRUPPO} è assente o {@link #GRUPPO_NON_CLASSIFICATI} e l'exchange ha un gruppo
     * preconfigurato, scrive l'associazione. Un'associazione dell'utente verso un gruppo diverso
     * <b>non</b> viene mai toccata, e l'exchange non viene più ripreso in mano nemmeno se l'utente
     * lo rimette in "Wallet 99".</p>
     *
     * <p>Il pregresso è protetto da {@link #congelaPreesistentiSeNecessario()} (da chiamare prima) :
     * su un archivio esistente i nomi già presenti sono nel marcatore, quindi qui vengono saltati e
     * solo un exchange <i>nuovo</i> viene associato d'ufficio.</p>
     *
     * <p>Il marcatore tiene solo i nomi <i>riconosciuti</i> (al più una quindicina, poche decine di
     * caratteri) : se per qualche motivo eccede i 255 caratteri della colonna {@code OPZIONI.Valore}
     * la scrittura fallisce e viene loggata, il nome viene rivalutato al giro dopo — innocuo, perché
     * la guardia sull'associazione attuale evita comunque di scippare una scelta dell'utente.</p>
     *
     * @param nomiSorgente i valori {@code [3]} distinti presenti nei movimenti
     * @return {@code true} se ha scritto almeno un'associazione (il chiamante può ricalcolare)
     */
    public static boolean autoAssociaGruppiPreconfigurati(Collection<String> nomiSorgente) {
        return !associaGruppiPreconfigurati(nomiSorgente, false).isEmpty();
    }

    /**
     * Variante <b>forzata</b> dell'auto-associazione, per il pulsante "Raggruppa exchange noti…" :
     * rivaluta ogni nome anche se già nel marcatore {@value #OPZIONE_AUTOGRUPPO_FATTI}, ma mantiene
     * la guardia sulle scelte dell'utente (un wallet in un gruppo diverso da
     * {@link #GRUPPO_NON_CLASSIFICATI} non viene toccato). Aggiorna comunque il marcatore, così la
     * successiva passata automatica resta coerente.
     *
     * @return gli spostamenti eseguiti, come {@code [wallet, gruppo]} ({@code isEmpty()} = nulla da fare)
     */
    public static List<String[]> raggruppaWalletExchangeNoti(Collection<String> nomiSorgente) {
        return associaGruppiPreconfigurati(nomiSorgente, true);
    }

    /**
     * Cuore condiviso dell'auto-associazione. Con {@code forza == false} salta i nomi già nel
     * marcatore; con {@code forza == true} li rivaluta comunque. In entrambi i casi non tocca un
     * wallet che l'utente ha assegnato a un gruppo suo.
     */
    private static List<String[]> associaGruppiPreconfigurati(Collection<String> nomiSorgente, boolean forza) {
        List<String[]> spostati = new ArrayList<>();
        if (nomiSorgente == null || nomiSorgente.isEmpty()) {
            return spostati;
        }
        Set<String> fatti = leggiMarcatoreFatti();
        boolean marcatoreCambiato = false;
        for (String nome : nomiSorgente) {
            if (nome == null || nome.isBlank()) {
                continue;
            }
            String n = nome.trim();
            String exId = exchangeIdDaSorgente(n);
            if (exId.isEmpty()) {
                continue; // non è un exchange noto : non lo ricordo nemmeno
            }
            String gruppo = gruppoPreconfigurato(exId);
            if (gruppo.isEmpty()) {
                continue; // exchange senza gruppo preconfigurato
            }
            boolean giaValutato = !fatti.add(n);
            if (giaValutato && !forza) {
                continue; // nome già valutato in passato (e non stiamo forzando)
            }
            if (!giaValutato) {
                marcatoreCambiato = true;
            }
            String attuale = DatabaseH2.Pers_GruppoWallet_Leggi(n, false);
            if (attuale == null || attuale.equals(GRUPPO_NON_CLASSIFICATI)) {
                DatabaseH2.Pers_GruppoWallet_Scrivi(n, gruppo);
                spostati.add(new String[] {n, gruppo});
            }
        }
        if (marcatoreCambiato) {
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_AUTOGRUPPO_FATTI, String.join("\n", fatti));
        }
        return spostati;
    }

    // --- helper ---------------------------------------------------------

    /** Etichetta leggibile di una modalità di calcolo (per le combo della GUI). Codice ignoto o vuoto → prima voce. */
    public static String etichettaModalita(String codice, String[][] tabella) {
        String c = trim(codice);
        for (String[] v : tabella) {
            if (v[0].equals(c)) {
                return v[1];
            }
        }
        return tabella[0][1];
    }

    /** Codice di una modalità di calcolo a partire dall'etichetta mostrata nella combo. Sconosciuta → {@code ""}. */
    public static String codiceModalita(String etichetta, String[][] tabella) {
        String e = trim(etichetta);
        for (String[] v : tabella) {
            if (v[1].equals(e)) {
                return v[0];
            }
        }
        return "";
    }

    private static Set<String> valoriAmmessi(String[][] tabella) {
        Set<String> s = new HashSet<>();
        for (String[] v : tabella) {
            if (!v[0].isEmpty()) {
                s.add(v[0]);
            }
        }
        return s;
    }

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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nz(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }

    // --- riepilogo "Info su Gruppo Wallet selezionato" -----------------

    /** Come {@link #trim} ma non restituisce mai {@code null} : comodo per le celle di tabella. */
    private static String sv(String s) {
        return s == null ? "" : s.trim();
    }

    /** {@code "yyyyMMddHHmmss"} (prefisso dell'ID movimento) → {@code "yyyy-MM-dd HH:mm"}. Input non conforme restituito com'è. */
    static String formattaTimestampId(String pref) {
        if (pref == null || pref.length() < 12 || !pref.chars().limit(12).allMatch(Character::isDigit)) {
            return pref == null ? "" : pref;
        }
        return pref.substring(0, 4) + "-" + pref.substring(4, 6) + "-" + pref.substring(6, 8)
                + " " + pref.substring(8, 10) + ":" + pref.substring(10, 12);
    }

    /**
     * Righe per la tabella "Exchange / wallet del gruppo" : una per ogni wallet ({@code v[3]})
     * associato al gruppo. {@code [wallet, nMovimenti, primoMovimento, ultimoMovimento]}.
     * Le date sono ricavate dal prefisso {@code yyyyMMddHHmmss} dell'ID movimento (ordine
     * lessicografico = cronologico, come {@code DocumentiFonte.Riepiloghi}).
     */
    public static List<String[]> infoWalletDelGruppo(String gruppo) {
        List<String[]> out = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank()) {
            return out;
        }
        List<String> wallets = DatabaseH2.Pers_GruppoWallet_WalletDelGruppo(gruppo);
        if (wallets.isEmpty()) {
            return out;
        }
        Set<String> set = new HashSet<>(wallets);
        Map<String, int[]> conteggio = new HashMap<>();
        Map<String, String[]> estremi = new HashMap<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v == null || v.length < 4 || v[3] == null) {
                continue;
            }
            String w = v[3].trim();
            if (!set.contains(w)) {
                continue;
            }
            conteggio.computeIfAbsent(w, k -> new int[1])[0]++;
            String id = v[0] == null ? "" : v[0];
            String pref = id.length() >= 14 ? id.substring(0, 14) : id;
            String[] e = estremi.get(w);
            if (e == null) {
                estremi.put(w, new String[] {pref, pref});
            } else {
                if (pref.compareTo(e[0]) < 0) {
                    e[0] = pref;
                }
                if (pref.compareTo(e[1]) > 0) {
                    e[1] = pref;
                }
            }
        }
        for (String w : wallets) {
            int[] c = conteggio.get(w);
            String[] e = estremi.get(w);
            out.add(new String[] {
                w,
                c == null ? "0" : String.valueOf(c[0]),
                e == null ? "" : formattaTimestampId(e[0]),
                e == null ? "" : formattaTimestampId(e[1])
            });
        }
        return out;
    }

    /**
     * Righe chiave/valore per la tabella "Dati fiscali del gruppo" : alias, modalità del riferimento
     * estero, exchange di riferimento, stato estero e identificativo fiscale <i>correnti</i>, stato del
     * bollo.
     */
    public static List<String[]> datiFiscaliGruppo(String gruppo) {
        List<String[]> out = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank()) {
            return out;
        }
        String[] alias = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        out.add(new String[] {"Alias", alias != null ? sv(alias[1]) : ""});

        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        String mod = r != null ? r[1] : null;
        if (mod == null) {
            out.add(new String[] {"Riferimento estero", "— non impostato"});
        } else if (RIFERIMENTO_EXCHANGE.equals(mod)) {
            String exId = sv(r[4]);
            String nome = exId;
            if (!exId.isEmpty()) {
                String[] a = DatabaseH2.Pers_ExchangeAnagrafica_Leggi(exId);
                if (a != null && a[1] != null && !a[1].isBlank()) {
                    nome = a[1];
                }
            }
            out.add(new String[] {"Riferimento estero", "Exchange di riferimento"});
            out.add(new String[] {"Exchange", exId.isEmpty() ? "—" : nome + " (" + exId + ")"});
            if (!exId.isEmpty()) {
                out.add(new String[] {"Periodo corrente exchange", Principale_PeriodiExchange.descriviCorrente(exId)});
            }
        } else {
            out.add(new String[] {"Riferimento estero", "Stato inserito a mano"});
        }
        out.add(new String[] {"Stato estero (corrente)", nzVuoto(statoEsteroEffettivo(gruppo))});
        out.add(new String[] {"Identificativo fiscale (corrente)", nzVuoto(identificativoFiscaleEffettivo(gruppo))});

        String stato = statoBolloPeriodi(gruppo);
        String bollo;
        if (BOLLO_STATO_MISTI.equals(stato)) {
            bollo = "gestito per periodo (valori non uniformi)";
        } else if (BOLLO_STATO_TUTTI_SI.equals(stato)) {
            bollo = "SI (tutti i periodi CRYPTO)";
        } else if (BOLLO_STATO_TUTTI_NO.equals(stato)) {
            bollo = "NO (tutti i periodi CRYPTO)";
        } else {
            bollo = BOLLO_SI.equals(bolloDefaultGruppo(gruppo)) ? "SI" : "NO";
        }
        out.add(new String[] {"Bollo pagato dall'intermediario", bollo});
        return out;
    }

    private static String nzVuoto(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    /**
     * Righe per la tabella "Periodi di detenzione" (sottoinsieme leggibile delle colonne di
     * {@link #caricaPeriodi}) : {@code [tipo, progr, dataInizio, dataFine, valIniziale, valFinale,
     * calcoloIniziale, calcoloFinale, bollo, origine]}.
     */
    public static List<String[]> periodiPerVista(String gruppo) {
        List<String[]> out = new ArrayList<>();
        for (String[] r : caricaPeriodi(gruppo)) {
            String tipo = sv(r[COL_TIPO]);
            out.add(new String[] {
                tipo, sv(r[COL_PROGRESSIVO]), sv(r[COL_DATA_INIZIO]), sv(r[COL_DATA_FINE]),
                sv(r[COL_VAL_INIZIALE]), sv(r[COL_VAL_FINALE]),
                etichettaModalita(r[COL_MOD_INIZIALE], MODALITA_INIZIALE),
                etichettaModalita(r[COL_MOD_FINALE], MODALITA_FINALE),
                TIPO_CRYPTO.equals(tipo) ? (r.length > COL_BOLLO ? sv(r[COL_BOLLO]) : "") : "n/d",
                descrizioneOrigineRiga(r.length > COL_ORIGINE ? r[COL_ORIGINE] : null,
                        r.length > COL_CHIAVE_DEFAULT ? r[COL_CHIAVE_DEFAULT] : null)
            });
        }
        return out;
    }

    // --- provenienza (Origine / ChiaveDefault) -----------------------------

    /** {@code "Predefinito"} / {@code "Modificato"} (ex predefinito, ora personalizzato) / {@code "Manuale"}. */
    public static String descrizioneOrigineRiga(String origine, String chiaveDefault) {
        if (ORIGINE_SISTEMA.equals(origine)) {
            return "Predefinito";
        }
        return chiaveDefault != null && !chiaveDefault.isBlank() ? "Modificato" : "Manuale";
    }

    /** {@code true} se l'id exchange è uno dei {@link DatabaseH2#EXCHANGE_NOTI}. */
    public static boolean exchangeEPredefinito(String exchangeId) {
        if (exchangeId == null) {
            return false;
        }
        String id = exchangeId.trim();
        for (String[] e : DatabaseH2.EXCHANGE_NOTI) {
            if (e[0].equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /** Provenienza di un exchange dell'anagrafica per la colonna "Origine". {@code null} = seme pregresso, non "modificato". */
    public static String descrizioneOrigineExchange(String exchangeId, String origine) {
        if (!exchangeEPredefinito(exchangeId)) {
            return "Manuale";
        }
        return ORIGINE_UTENTE.equals(origine) ? "Predefinito (mod.)" : "Predefinito";
    }

    /** {@code true} se il nome gruppo è uno dei preconfigurati {@code Wallet 101..}{@code (101 + numero exchange noti - 1)}. */
    public static boolean gruppoEPredefinito(String gruppo) {
        Integer n = numeroGruppo(gruppo);
        return n != null && n >= DatabaseH2.GRUPPO_PRECONF_BASE
                && n < DatabaseH2.GRUPPO_PRECONF_BASE + DatabaseH2.EXCHANGE_NOTI.length;
    }

    /** Provenienza del gruppo per la colonna "Origine" della tabella "Gruppi Wallet". */
    public static String descrizioneOrigineGruppo(String gruppo) {
        if (!gruppoEPredefinito(gruppo)) {
            return "Manuale";
        }
        String[] a = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        return ORIGINE_UTENTE.equals(a.length > 3 ? a[3] : null) ? "Predefinito (mod.)" : "Predefinito";
    }

    /**
     * Voce da mettere in una combo di scelta gruppo : {@code "Wallet 101 ( Binance — predefinito )"}.
     * Il marcatore sta <b>dentro</b> le parentesi apposta : chi legge la selezione fa
     * {@code split("\\(")[0]} e deve continuare a ottenere il solo nome del gruppo.
     */
    public static String etichettaComboGruppo(String gruppo, String alias) {
        String a = alias == null ? "" : alias;
        return gruppo + " ( " + a + (gruppoEPredefinito(gruppo) ? " — predefinito" : "") + " )";
    }

    // --- semina e reconcile dei dati predefiniti (RW_Predefiniti.json) ----

    /** Opzione ({@code personale.mv.db}) : hash del JSON dei predefiniti dell'ultimo reconcile riuscito. */
    public static final String OPZIONE_HASH_PREDEFINITI = "RW_PREDEFINITI_HASH";

    /**
     * Riversa {@code RW_Predefiniti.json} nelle tabelle {@code personale.mv.db}, toccando <b>solo</b> le
     * righe con {@code Origine == SISTEMA} (le righe {@code UTENTE} — o {@code NULL} sui DB creati prima
     * di questa feature — non vengono mai modificate né cancellate). Chiamata da
     * {@code DatabaseH2.CreaoCollegaDatabase()} : sostituisce i vecchi seed hardcoded.
     *
     * <p>Guardia = hash del file : se non è cambiato dall'ultimo giro non fa nulla. Se il JSON non è
     * disponibile ({@link DatiPredefinitiRW#Carica()} torna {@code null}) non semina e non riconcilia.</p>
     */
    public static void Pers_RW_SeminaERiconcilia() {
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        if (p == null) {
            LoggerGC.ScriviErrore("Pers_RW_SeminaERiconcilia: RW_Predefiniti.json non disponibile, semina/reconcile saltati");
            return;
        }
        if (p.hash().equals(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_HASH_PREDEFINITI))) {
            return;
        }
        try {
            for (DatiPredefinitiRW.ExchangePredef ex : p.exchange()) {
                riconciliaExchange(ex);
            }
            for (DatiPredefinitiRW.GruppoPeriodiPredef g : p.periodiDetenzioneGruppi()) {
                riconciliaPeriodiDetenzione(g);
            }
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_HASH_PREDEFINITI, p.hash());
        } catch (RuntimeException e) {
            LoggerGC.ScriviErrore(e);
        }
    }

    private static void riconciliaExchange(DatiPredefinitiRW.ExchangePredef ex) {
        // 1. anagrafica : creo se assente, aggiorno il nome se SISTEMA, lascio se UTENTE/NULL
        String[] ana = DatabaseH2.Pers_ExchangeAnagrafica_Leggi(ex.id);
        if (ana[0] == null || ORIGINE_SISTEMA.equals(ana.length > 7 ? ana[7] : null)) {
            DatabaseH2.Pers_ExchangeAnagrafica_ScriviNome(ex.id, ex.nome, ORIGINE_SISTEMA);
        }

        // 2. gruppo wallet preconfigurato : solo se nuovo o già SISTEMA
        if (ex.gruppo != null && !ex.gruppo.isBlank()) {
            String[] alias = DatabaseH2.Pers_GruppoAlias_Leggi(ex.gruppo);
            boolean nuovo = alias[0] == null;
            boolean sistema = ORIGINE_SISTEMA.equals(alias.length > 3 ? alias[3] : null);
            if (nuovo || sistema) {
                boolean pagaBollo = "S".equals(alias.length > 2 ? alias[2] : null);
                DatabaseH2.Pers_GruppoAlias_Scrivi(ex.gruppo, ex.nome, nuovo ? false : pagaBollo, ORIGINE_SISTEMA);
                if (DatabaseH2.Pers_GruppoRiferimento_Leggi(ex.gruppo)[0] == null) {
                    DatabaseH2.Pers_GruppoRiferimento_Scrivi(ex.gruppo, RIFERIMENTO_EXCHANGE, null, null, ex.id);
                }
            }
        }

        // 3. periodi fiscali dell'exchange, per ChiaveDefault
        List<String[]> esistenti = DatabaseH2.Pers_ExchangePeriodo_LeggiExchange(ex.id);
        // riga : [0]key [1]exid [2]prog [3]di [4]df [5]nome [6]stato [7]ident [8]note [9]fonte [10]origine [11]chiaveDefault
        boolean vuoto = esistenti.isEmpty();
        boolean gestita = vuoto || esistenti.stream().anyMatch(r -> ORIGINE_SISTEMA.equals(cd(r, 10)));
        Map<String, String[]> perChiave = new HashMap<>();
        Set<Integer> progUsati = new HashSet<>();
        for (String[] r : esistenti) {
            progUsati.add(intOrZero(r[2]));
            if (r[11] != null && !r[11].isBlank()) {
                perChiave.put(r[11], r);
            }
        }
        Set<String> chiaviJson = new HashSet<>();
        for (DatiPredefinitiRW.PeriodoFiscalePredef pf : ex.periodiFiscali) {
            chiaviJson.add(pf.chiave);
            String[] r = perChiave.get(pf.chiave);
            if (r == null) {
                if (!gestita) {
                    continue; // set di periodi tutto dell'utente : non lo invado
                }
                int prog = vuoto ? pf.progressivo : prossimoLibero(progUsati);
                progUsati.add(prog);
                DatabaseH2.Pers_ExchangePeriodo_Scrivi(ex.id, prog, nn(pf.dataInizio), nn(pf.dataFine),
                        nn(pf.nome), nn(pf.statoEstero), nn(pf.identificativoFiscale), nn(pf.note),
                        nn(pf.fonte), ORIGINE_SISTEMA, pf.chiave);
            } else if (ORIGINE_SISTEMA.equals(cd(r, 10))) {
                DatabaseH2.Pers_ExchangePeriodo_Scrivi(ex.id, intOrZero(r[2]), nn(pf.dataInizio), nn(pf.dataFine),
                        nn(pf.nome), nn(pf.statoEstero), nn(pf.identificativoFiscale), nn(pf.note),
                        nn(pf.fonte), ORIGINE_SISTEMA, pf.chiave);
            }
        }
        for (String[] r : esistenti) {
            if (ORIGINE_SISTEMA.equals(cd(r, 10)) && r[11] != null && !r[11].isBlank()
                    && !chiaviJson.contains(r[11])) {
                DatabaseH2.Pers_ExchangePeriodo_Cancella(ex.id, intOrZero(r[2]));
            }
        }
    }

    private static void riconciliaPeriodiDetenzione(DatiPredefinitiRW.GruppoPeriodiPredef g) {
        List<String[]> esistenti = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo(g.gruppo);
        // riga : [0]key [1]gruppo [2]tipo [3]prog [4]di [5]df [6]valI [7]notaI [8]valF [9]notaF
        //        [10]modI [11]modF [12]bollo [13]origine [14]chiaveDefault
        boolean vuoto = esistenti.isEmpty();
        boolean gestita = vuoto || esistenti.stream().anyMatch(r -> ORIGINE_SISTEMA.equals(cd(r, 13)));
        Map<String, String[]> perChiave = new HashMap<>();
        Map<String, Set<Integer>> progPerTipo = new HashMap<>();
        for (String[] r : esistenti) {
            progPerTipo.computeIfAbsent(trim(r[2]), k -> new HashSet<>()).add(intOrZero(r[3]));
            if (r[14] != null && !r[14].isBlank()) {
                perChiave.put(r[14], r);
            }
        }
        Set<String> chiaviJson = new HashSet<>();
        for (DatiPredefinitiRW.PeriodoDetPredef pd : g.periodi) {
            chiaviJson.add(pd.chiave);
            String[] r = perChiave.get(pd.chiave);
            Set<Integer> usati = progPerTipo.computeIfAbsent(pd.tipo, k -> new HashSet<>());
            if (r == null) {
                if (!gestita) {
                    continue;
                }
                int prog = vuoto ? pd.progressivo : prossimoLibero(usati);
                usati.add(prog);
                scriviPeriodoDetPredef(g.gruppo, pd.tipo, prog, pd);
            } else if (ORIGINE_SISTEMA.equals(cd(r, 13))) {
                scriviPeriodoDetPredef(g.gruppo, trim(r[2]), intOrZero(r[3]), pd);
            }
        }
        for (String[] r : esistenti) {
            if (ORIGINE_SISTEMA.equals(cd(r, 13)) && r[14] != null && !r[14].isBlank()
                    && !chiaviJson.contains(r[14])) {
                DatabaseH2.Pers_GruppoPeriodoRW_Cancella(g.gruppo, trim(r[2]), intOrZero(r[3]));
            }
        }
    }

    private static void scriviPeriodoDetPredef(String gruppo, String tipo, int prog,
            DatiPredefinitiRW.PeriodoDetPredef pd) {
        String bollo = TIPO_CRYPTO.equals(tipo) ? nn(pd.bollo) : null;
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi(gruppo, tipo, prog, nn(pd.dataInizio), nn(pd.dataFine),
                nn(pd.valIniziale), nn(pd.notaIniziale), nn(pd.valFinale), nn(pd.notaFinale),
                nn(pd.calcoloIniziale), nn(pd.calcoloFinale), bollo, ORIGINE_SISTEMA, pd.chiave);
    }

    // --- pulsanti "Ripristina al default" (lavorano sulla lista in memoria della GUI) -----

    /**
     * Rimette la riga {@code indice} della lista GUI ai valori di {@code RW_Predefiniti.json}, se ha una
     * {@link #COL_CHIAVE_DEFAULT} nota. Torna {@code true} se ha cambiato qualcosa (la GUI ricostruisce
     * la vista). Non salva : il salvataggio resta sul pulsante "Salva".
     *
     * @return {@code true} = riga ripristinata ; {@code false} = nessuna corrispondenza nel JSON
     */
    public static boolean ripristinaRigaAlDefault(String gruppo, List<String[]> righe, int indice) {
        if (righe == null || indice < 0 || indice >= righe.size()) {
            return false;
        }
        String[] r = righe.get(indice);
        String chiave = r.length > COL_CHIAVE_DEFAULT ? trim(r[COL_CHIAVE_DEFAULT]) : "";
        if (chiave.isEmpty()) {
            return false;
        }
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        if (p == null) {
            return false;
        }
        String g = gruppo == null ? "" : gruppo.trim();
        for (DatiPredefinitiRW.GruppoPeriodiPredef gp : p.periodiDetenzioneGruppi()) {
            if (!gp.gruppo.equalsIgnoreCase(g)) {
                continue;
            }
            for (DatiPredefinitiRW.PeriodoDetPredef pd : gp.periodi) {
                if (chiave.equals(pd.chiave)) {
                    righe.set(indice, rigaDaPredef(pd, trim(r[COL_PROGRESSIVO])));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ricostruisce la lista GUI dei periodi di un gruppo dai soli default del JSON (tutti
     * {@link #ORIGINE_SISTEMA}), scartando le personalizzazioni. Non salva.
     *
     * @return la nuova lista, o {@code null} se il JSON non è disponibile
     */
    public static List<String[]> ripristinaTuttiAlDefault(String gruppo) {
        DatiPredefinitiRW p = DatiPredefinitiRW.Carica();
        if (p == null) {
            return null;
        }
        List<String[]> out = new ArrayList<>();
        for (DatiPredefinitiRW.GruppoPeriodiPredef g : p.periodiDetenzioneGruppi()) {
            if (g.gruppo.equalsIgnoreCase(gruppo == null ? "" : gruppo.trim())) {
                for (DatiPredefinitiRW.PeriodoDetPredef pd : g.periodi) {
                    out.add(rigaDaPredef(pd, String.valueOf(pd.progressivo)));
                }
            }
        }
        return out;
    }

    private static String[] rigaDaPredef(DatiPredefinitiRW.PeriodoDetPredef pd, String progressivo) {
        String[] r = new String[COLONNE_PERIODO];
        r[COL_TIPO] = pd.tipo;
        r[COL_PROGRESSIVO] = progressivo == null || progressivo.isBlank() ? String.valueOf(pd.progressivo) : progressivo;
        r[COL_DATA_INIZIO] = sv(pd.dataInizio);
        r[COL_DATA_FINE] = sv(pd.dataFine);
        r[COL_VAL_INIZIALE] = sv(pd.valIniziale);
        r[COL_NOTA_INIZIALE] = sv(pd.notaIniziale);
        r[COL_VAL_FINALE] = sv(pd.valFinale);
        r[COL_NOTA_FINALE] = sv(pd.notaFinale);
        r[COL_MOD_INIZIALE] = sv(pd.calcoloIniziale);
        r[COL_MOD_FINALE] = sv(pd.calcoloFinale);
        r[COL_BOLLO] = TIPO_CRYPTO.equals(pd.tipo) ? sv(pd.bollo) : "";
        r[COL_ORIGINE] = ORIGINE_SISTEMA;
        r[COL_CHIAVE_DEFAULT] = sv(pd.chiave);
        return r;
    }

    private static String cd(String[] r, int i) {
        return r != null && r.length > i ? r[i] : null;
    }

    private static int intOrZero(String s) {
        try {
            return Integer.parseInt(trim(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int prossimoLibero(Set<Integer> usati) {
        int p = 1;
        while (usati.contains(p)) {
            p++;
        }
        return p;
    }

    private static String nn(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
