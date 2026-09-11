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
 * Logica operativa dei <b>periodi di detenzione</b> di un gruppo wallet per il quadro W/RW : righi
 * CRYPTO / FIAT con date, modalità di calcolo del valore agli estremi e — sui soli righi FIAT — i
 * dati fiscali dell'intermediario (Stato estero, identificativo, note, fonte, alias ISEE).
 *
 * <p>Companion di {@code Principale} nello stile di {@link Principale_GiacenzeaData}:
 * metodi {@code public static}, nessun campo Swing, nessun riferimento a {@code Principale}.
 * Persistenza via {@code DatabaseH2.Pers_GruppoPeriodoRW_*}.</p>
 *
 * <p><b>Una tabella sola (2026-09-09).</b> Fino al 2026-09-08 i dati fiscali stavano in due tabelle a
 * parte — {@code EXCHANGE_ANAGRAFICA}/{@code EXCHANGE_PERIODO}, con {@code GRUPPO_RIFERIMENTO_ESTERO}
 * a collegare il gruppo al suo exchange — e il tratto di detenzione nasceva dall'<i>incrocio</i> fra
 * quei periodi e questi. Ora un cambio di Stato estero è semplicemente un periodo FIAT in più, come
 * lo è un cambio di regime del bollo per i righi CRYPTO : il taglio è uno solo e viene da qui.
 * Un gruppo senza righi FIAT non ha Stato estero, e va bene così.</p>
 *
 * <p><b>Stato.</b> I periodi CRYPTO non sono ancora letti da {@code Calcoli_RW.AggiornaRWFR} ; i
 * periodi <b>FIAT</b> sono consumati da {@link Calcoli_RW_Fiat}. La validazione di
 * {@link #validaPeriodi(List)} è strutturale <i>più</i> semantica sulle <b>sovrapposizioni</b>
 * (errore bloccante : le finestre di detenzione dello stesso rigo non possono accavallarsi) ; i
 * <b>buchi</b> sono ammessi (conto chiuso e poi riaperto) e vengono solo segnalati da
 * {@link #avvisiPeriodi(List)}, senza bloccare il salvataggio.</p>
 */
public class Principale_GruppiWalletRW {

    private Principale_GruppiWalletRW() {
    }

    // --- valori ammessi -------------------------------------------------------

    public static final String TIPO_CRYPTO = "CRYPTO";
    public static final String TIPO_FIAT = "FIAT";

    /**
     * Valore agli estremi del periodo = <b>solo il residuo</b> : la giacenza a inizio giornata per il
     * valore iniziale, quella a fine giornata per il valore finale. È il default, ed è anche ciò che
     * significa una modalità <b>vuota</b> : le righe scritte prima che questo codice esistesse si
     * comportano già così, quindi i motori accettano entrambi ({@link #soloResiduo(String)}) e la GUI
     * scrive sempre il codice esplicito.
     */
    public static final String MOD_SOLO_RESIDUO = "SOLO_RESIDUO";
    public static final String MOD_INIZIALE_PRIMO_APPORTO = "PRIMO_APPORTO";
    public static final String MOD_INIZIALE_SOMMA_APPORTI = "SOMMA_APPORTI_GIORNO";
    public static final String MOD_FINALE_ULTIMA_USCITA = "ULTIMA_USCITA";
    public static final String MOD_FINALE_SOMMA_USCITE = "SOMMA_USCITE_GIORNO";

    /** Bollo pagato dall'intermediario/exchange nel singolo periodo. */
    public static final String BOLLO_SI = "SI";
    public static final String BOLLO_NO = "NO";

    /**
     * "È conto corrente" del rigo FIAT : {@code SI} = l'intermediario estero è una banca e il conto in
     * valuta è un vero conto corrente / deposito bancario estero (codice individuazione bene 1, IVAFE in
     * misura fissa, valore medio di giacenza — vedi le istruzioni Redditi PF, colonna 3 del quadro RW).
     * {@code NO}/vuoto = altra attività estera di natura finanziaria (codice bene 14). Il calcolo è in
     * {@code Calcoli_RW_Fiat.applicaContoCorrente} (IVAFE fissa 34,20 € pro quota/giorni, esente sotto
     * 5.000 € di giacenza media, soglia per gruppo wallet).
     */
    public static final String CONTO_CORRENTE_SI = "SI";
    public static final String CONTO_CORRENTE_NO = "NO";

    /**
     * Lunghezza massima dell'identificativo di un operatore finanziario estero nel modulo FC.1 della
     * DSU/ISEE ({@code E} + 15). Vale sia per l'identificativo fiscale sia per l'alias ISEE.
     */
    public static final int MAX_IDENT_ISEE = 15;

    /** Etichette leggibili per le combo della GUI, nell'ordine {codice, etichetta}. */
    public static final String[][] MODALITA_INIZIALE = {
        {MOD_SOLO_RESIDUO, "Solo residuo (giacenza a inizio giornata)"},
        {MOD_INIZIALE_PRIMO_APPORTO, "Primo apporto della giornata + residuo"},
        {MOD_INIZIALE_SOMMA_APPORTI, "Somma degli apporti della giornata + residuo"},
    };
    public static final String[][] MODALITA_FINALE = {
        {MOD_SOLO_RESIDUO, "Solo residuo (giacenza a fine giornata)"},
        {MOD_FINALE_ULTIMA_USCITA, "Ultima uscita della giornata + residuo"},
        {MOD_FINALE_SOMMA_USCITE, "Somma delle uscite della giornata + residuo"},
    };

    /** {@code true} se la modalità di calcolo vale "solo residuo" : il codice esplicito o il vuoto. */
    public static boolean soloResiduo(String modalita) {
        String m = trim(modalita);
        return m.isEmpty() || MOD_SOLO_RESIDUO.equals(m);
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Riga della GUI "periodi" : {@code [TipoRigo, Progressivo, DataInizio, DataFine,
     * ValoreInizialeManuale, NotaValoreIniziale, ValoreFinaleManuale, NotaValoreFinale,
     * ModalitaCalcoloIniziale, ModalitaCalcoloFinale, PagaBolloPeriodo, Origine, ChiaveDefault,
     * StatoEstero, IdentificativoFiscale, NoteFiscali, FonteFiscale, IdentificativoISEE,
     * EContoCorrente]} — 19 colonne, senza chiave sintetica né gruppo. I motori RW leggono i periodi
     * per indice ({@code COL_*}) : le colonne nuove vanno <b>sempre in coda</b>, mai inserite in mezzo.
     *
     * <p>Le sei colonne fiscali finali (Stato estero → EContoCorrente) hanno senso <b>solo sui righi
     * FIAT</b> ; sui righi CRYPTO {@link #salvaPeriodi} le scrive vuote, come già fa col bollo
     * all'inverso.</p>
     */
    public static final int COL_TIPO = 0, COL_PROGRESSIVO = 1, COL_DATA_INIZIO = 2, COL_DATA_FINE = 3,
            COL_VAL_INIZIALE = 4, COL_NOTA_INIZIALE = 5, COL_VAL_FINALE = 6, COL_NOTA_FINALE = 7,
            COL_MOD_INIZIALE = 8, COL_MOD_FINALE = 9, COL_BOLLO = 10, COL_ORIGINE = 11, COL_CHIAVE_DEFAULT = 12,
            COL_STATO_ESTERO = 13, COL_IDENT_FISCALE = 14, COL_NOTE_FISCALI = 15, COL_FONTE_FISCALE = 16,
            COL_IDENT_ISEE = 17, COL_E_CONTO_CORRENTE = 18;
    public static final int COLONNE_PERIODO = 19;

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
     * Testo di sintesi dei dati fiscali di un gruppo, per la colonna della tabella "Gruppi Wallet" :
     * Stato estero e identificativo del periodo FIAT valido <i>oggi</i>. Non lancia mai : su dati
     * incoerenti torna una dicitura neutra.
     */
    public static String descriviRiferimento(String gruppo) {
        if (gruppo == null || gruppo.isBlank()) {
            return "";
        }
        String[] p = periodoFiatAllaData(gruppo, null);
        if (p == null) {
            return "— non impostato";
        }
        String stato = trim(p[COL_STATO_ESTERO]);
        String ident = trim(p[COL_IDENT_FISCALE]);
        if (stato.isEmpty() && ident.isEmpty()) {
            return "periodo FIAT senza dati fiscali";
        }
        return "stato " + (stato.isEmpty() ? "?" : StatiEsteri.etichetta(stato))
                + (ident.isEmpty() ? "" : " · " + ident);
    }

    /** Stato estero del gruppo secondo il periodo FIAT valido <i>oggi</i>. {@code ""} se non impostato. */
    public static String statoEsteroEffettivo(String gruppo) {
        return statoEsteroEffettivo(gruppo, null);
    }

    /**
     * Stato estero del gruppo a una certa data, letto dal periodo <b>FIAT</b> che la copre.
     * {@code data == null} = oggi. {@code ""} se nessun periodo copre la data o non lo valorizza.
     */
    public static String statoEsteroEffettivo(String gruppo, LocalDate data) {
        String[] p = periodoFiatAllaData(gruppo, data);
        return p == null ? "" : trim(p[COL_STATO_ESTERO]);
    }

    /** Identificativo fiscale / P.IVA del gruppo secondo il periodo FIAT valido <i>oggi</i>. */
    public static String identificativoFiscaleEffettivo(String gruppo) {
        return identificativoFiscaleEffettivo(gruppo, null);
    }

    /** Identificativo fiscale del gruppo a una certa data ({@code null} = oggi), dal periodo FIAT che la copre. */
    public static String identificativoFiscaleEffettivo(String gruppo, LocalDate data) {
        String[] p = periodoFiatAllaData(gruppo, data);
        return p == null ? "" : trim(p[COL_IDENT_FISCALE]);
    }

    /** Alias ISEE (modulo FC.1 della DSU) del gruppo a una certa data ({@code null} = oggi). */
    public static String identificativoIseeEffettivo(String gruppo, LocalDate data) {
        String[] p = periodoFiatAllaData(gruppo, data);
        return p == null ? "" : trim(p[COL_IDENT_ISEE]);
    }

    /**
     * Il periodo <b>FIAT</b> del gruppo che copre {@code data} ({@code null} = oggi), secondo le
     * finestre <i>effettive</i> di {@link #finestreEffettive(List, String)}. {@code null} se il gruppo
     * non ha periodi FIAT o nessuno copre la data — che è il caso normale, non un errore : un gruppo
     * senza righi FIAT semplicemente non ha Stato estero.
     */
    public static String[] periodoFiatAllaData(String gruppo, LocalDate data) {
        if (gruppo == null || gruppo.isBlank()) {
            return null;
        }
        LocalDate d = data == null ? LocalDate.now() : data;
        for (Finestra f : finestreEffettive(caricaPeriodi(gruppo), TIPO_FIAT)) {
            if (f.copre(d)) {
                return f.riga;
            }
        }
        return null;
    }

    // --- periodi di detenzione -------------------------------------------

    /** I periodi del gruppo in forma GUI ({@link #COLONNE_PERIODO} colonne), ordinati per tipo/progressivo. */
    public static List<String[]> caricaPeriodi(String gruppo) {
        List<String[]> out = new ArrayList<>();
        for (String[] db : DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo(gruppo)) {
            // db : [Gruppo_Tipo_Prog, Gruppo, TipoRigo, Progressivo, DataInizio, DataFine,
            //       ValIniManuale, NotaIni, ValFinManuale, NotaFin, ModIni, ModFin, PagaBolloPeriodo,
            //       Origine, ChiaveDefault, StatoEstero, IdentFiscale, NoteFiscali, FonteFiscale, IdentISEE,
            //       EContoCorrente]
            out.add(new String[] {
                db[2], db[3], db[4], db[5], db[6], db[7], db[8], db[9], db[10], db[11], db[12], db[13], db[14],
                db[15], db[16], db[17], db[18], db[19], db[20]
            });
        }
        return out;
    }

    // --- finestre effettive (date dedotte) --------------------------------

    /**
     * Un periodo con le sue date <b>effettive</b>, cioè quelle che valgono davvero dopo aver dedotto
     * ciò che l'utente ha lasciato in bianco. {@code inizio == null} = "dal primo movimento del
     * gruppo" ; {@code fine == null} = "ancora aperto".
     */
    public static final class Finestra {

        /** La riga di origine, in forma GUI ({@link #COLONNE_PERIODO} colonne). */
        public final String[] riga;
        /** Inizio effettivo, o {@code null} = dal primo movimento del gruppo. */
        public final LocalDate inizio;
        /** Fine effettiva, o {@code null} = periodo ancora aperto. */
        public final LocalDate fine;
        /** {@code true} se {@link #inizio} è stato dedotto dalla fine del periodo precedente. */
        public final boolean inizioDedotto;

        Finestra(String[] riga, LocalDate inizio, LocalDate fine, boolean inizioDedotto) {
            this.riga = riga;
            this.inizio = inizio;
            this.fine = fine;
            this.inizioDedotto = inizioDedotto;
        }

        /** {@code true} se la data cade dentro la finestra (estremi aperti = infinito da quel lato). */
        public boolean copre(LocalDate d) {
            return d != null && (inizio == null || !d.isBefore(inizio)) && (fine == null || !d.isAfter(fine));
        }
    }

    /**
     * Le finestre effettive dei periodi di un tipo, ordinate per inizio (le finestre che partono "dal
     * primo movimento" per prime).
     *
     * <p><b>La regola sulla data di inizio mancante.</b> Un periodo senza {@code DataInizio} non parte
     * più necessariamente dal primo movimento del gruppo : se fra gli <i>altri</i> periodi dello stesso
     * tipo ce n'è uno che finisce prima, il periodo comincia il giorno dopo quella fine. "Prima" vuol
     * dire prima della propria {@code DataFine} quando c'è ; se non c'è nemmeno quella — la riga
     * legittima con <b>entrambe</b> le date vuote, cioè "il periodo in corso, la cui fine non è ancora
     * definita" — si prende la fine più avanzata fra tutte le altre. Solo quando non esiste nessuna
     * {@code DataFine} a cui agganciarsi l'inizio resta {@code null}, e allora sì che vuol dire "dal
     * primo movimento". Entrambi i rami servono : è la coppia di righe che nasce da un cambio di Stato
     * estero (una chiusa il giorno prima, l'altra aperta da lì in avanti e senza fine).</p>
     *
     * @param righe righe in forma GUI ({@link #COLONNE_PERIODO} colonne) ; {@code null} ammesso
     * @param tipo  {@link #TIPO_CRYPTO} o {@link #TIPO_FIAT}
     */
    public static List<Finestra> finestreEffettive(List<String[]> righe, String tipo) {
        List<String[]> delTipo = new ArrayList<>();
        if (righe != null) {
            for (String[] r : righe) {
                if (r != null && r.length > COL_DATA_FINE && trim(r[COL_TIPO]).equals(trim(tipo))) {
                    delTipo.add(r);
                }
            }
        }
        List<Finestra> out = new ArrayList<>();
        for (String[] r : delTipo) {
            LocalDate fine = parseData(r[COL_DATA_FINE]);
            LocalDate inizio = parseData(r[COL_DATA_INIZIO]);
            boolean dedotto = false;
            if (inizio == null) {
                LocalDate precedente = null;
                for (String[] altro : delTipo) {
                    if (altro == r) {
                        continue;
                    }
                    LocalDate f = parseData(altro[COL_DATA_FINE]);
                    if (f == null || (fine != null && !f.isBefore(fine))) {
                        continue;
                    }
                    if (precedente == null || f.isAfter(precedente)) {
                        precedente = f;
                    }
                }
                if (precedente != null) {
                    inizio = precedente.plusDays(1);
                    dedotto = true;
                }
            }
            out.add(new Finestra(r, inizio, fine, dedotto));
        }
        out.sort((a, b) -> {
            if (a.inizio == null && b.inizio == null) {
                return 0;
            }
            if (a.inizio == null) {
                return -1;
            }
            if (b.inizio == null) {
                return 1;
            }
            return a.inizio.compareTo(b.inizio);
        });
        return out;
    }

    /**
     * Validazione dei periodi : strutturale e semantica sulle sovrapposizioni. Regole :
     * <ul>
     *   <li>tipo ∈ {CRYPTO, FIAT}</li>
     *   <li>progressivo intero >= 1, unico per (tipo)</li>
     *   <li>date, se valorizzate, in formato {@code yyyy-MM-dd} valido</li>
     *   <li>data inizio <= data fine quando entrambe presenti</li>
     *   <li>modalità di calcolo ∈ valori ammessi (o vuota = solo residuo)</li>
     *   <li>codice Stato estero al massimo 3 caratteri ; identificativo fiscale e alias ISEE al
     *       massimo {@value #MAX_IDENT_ISEE} caratteri (limite del modulo FC.1 della DSU)</li>
     *   <li>le finestre <b>effettive</b> (vedi {@link #finestreEffettive}) dello <b>stesso tipo</b> non
     *       si sovrappongono. Un periodo la cui finestra resta aperta da entrambi i lati è un fallback
     *       e non è in conflitto con gli altri ; averne più d'uno per tipo è solo un avviso.</li>
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
            // I dati fiscali riguardano solo il rigo FIAT ; sui righi CRYPTO salvaPeriodi li azzera.
            if (TIPO_FIAT.equals(tipo)) {
                // Il codice Stato è al massimo di 3 caratteri : ci stanno i codici a 3 cifre della
                // Tabella 10 e la sentinella "IT" (conto in Italia, vedi StatiEsteri.CODICE_ITALIA).
                if (campo(r, COL_STATO_ESTERO).length() > 3) {
                    errori.add(et + "il codice dello Stato estero è al massimo di 3 caratteri (tabella \"Elenco Paesi\" del modello Redditi).");
                }
                String contoCorrente = campo(r, COL_E_CONTO_CORRENTE);
                if (!contoCorrente.isEmpty() && !CONTO_CORRENTE_SI.equals(contoCorrente) && !CONTO_CORRENTE_NO.equals(contoCorrente)) {
                    errori.add(et + "valore \"è conto corrente\" non riconosciuto (\"" + contoCorrente + "\"), atteso "
                            + CONTO_CORRENTE_SI + " o " + CONTO_CORRENTE_NO + ".");
                }
                if (campo(r, COL_IDENT_FISCALE).length() > MAX_IDENT_ISEE) {
                    errori.add(et + "l'identificativo fiscale è al massimo di " + MAX_IDENT_ISEE
                            + " caratteri (limite del modulo FC.1 della DSU/ISEE).");
                }
                if (campo(r, COL_IDENT_ISEE).length() > MAX_IDENT_ISEE) {
                    errori.add(et + "l'identificativo ISEE è al massimo di " + MAX_IDENT_ISEE
                            + " caratteri (limite del modulo FC.1 della DSU/ISEE).");
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
     * Buchi di copertura fra le finestre effettive di periodi consecutivi dello stesso tipo : sono
     * <b>ammessi</b> (un conto può essere chiuso e poi riaperto) e vanno solo segnalati all'utente,
     * <b>senza</b> bloccare il salvataggio. Se per un tipo esiste una finestra aperta da entrambi i
     * lati che fa da copertura, non si segnala nulla.
     *
     * <p>Da quando l'inizio mancante si deduce dalla fine del periodo precedente
     * ({@link #finestreEffettive}), la coppia "una riga chiusa al giorno prima + una riga senza date"
     * non produce più nessun buco : è esattamente la configurazione che nasce da un cambio di Stato
     * estero, e adesso è contigua per costruzione.</p>
     *
     * @param righe righe in forma GUI ({@link #COLONNE_PERIODO} colonne)
     * @return elenco avvisi ({@code isEmpty()} = nessun buco)
     */
    public static List<String> avvisiPeriodi(List<String[]> righe) {
        List<String> avvisi = new ArrayList<>();
        if (righe == null) {
            return avvisi;
        }
        for (String tipo : new String[] {TIPO_CRYPTO, TIPO_FIAT}) {
            List<Finestra> finestre = finestreEffettive(righe, tipo);
            if (finestre.isEmpty()) {
                continue;
            }
            int aperti = 0;
            for (Finestra f : finestre) {
                if (f.inizio == null && f.fine == null) {
                    aperti++;
                }
            }
            if (aperti > 1) {
                avvisi.add("Più di un periodo " + tipo + " senza date : per la generazione dei "
                        + "righi conterà solo quello con progressivo più basso, dai una finestra agli altri.");
            }
            avvisi.addAll(buchiPeriodi(tipo, finestre));
        }
        return avvisi;
    }

    /** Coppie di periodi dello stesso tipo con finestre <b>effettive</b> che si intersecano. */
    private static List<String> sovrapposizioniPeriodi(List<String[]> righe) {
        List<String> errori = new ArrayList<>();
        for (String tipo : new String[] {TIPO_CRYPTO, TIPO_FIAT}) {
            List<Finestra> g = finestreEffettive(righe, tipo);
            for (int i = 0; i < g.size(); i++) {
                for (int j = i + 1; j < g.size(); j++) {
                    if (finestreSovrapposte(g.get(i), g.get(j))) {
                        errori.add("I periodi " + tipo + " " + trim(g.get(i).riga[COL_PROGRESSIVO])
                                + " e " + trim(g.get(j).riga[COL_PROGRESSIVO]) + " si sovrappongono : le finestre "
                                + "di detenzione dello stesso rigo non possono accavallarsi.");
                    }
                }
            }
        }
        return errori;
    }

    // --- algebra sugli intervalli (semantica dei periodi aperti : data vuota = -inf / +inf) ------

    /**
     * {@code true} se due finestre effettive si intersecano. Una finestra aperta da <b>entrambi</b> i
     * lati (nessuna data propria e nessuna fine precedente da cui dedurre l'inizio) è un fallback e non
     * entra mai in conflitto : più periodi così dello stesso tipo sono solo un avviso
     * ({@link #avvisiPeriodi(List)}), non un errore — è la forma che ha un gruppo appena creato, prima
     * che l'utente dia una finestra a qualcosa.
     */
    static boolean finestreSovrapposte(Finestra a, Finestra b) {
        if ((a.inizio == null && a.fine == null) || (b.inizio == null && b.fine == null)) {
            return false;
        }
        LocalDate i1 = a.inizio == null ? LocalDate.MIN : a.inizio;
        LocalDate f1 = a.fine == null ? LocalDate.MAX : a.fine;
        LocalDate i2 = b.inizio == null ? LocalDate.MIN : b.inizio;
        LocalDate f2 = b.fine == null ? LocalDate.MAX : b.fine;
        return !i1.isAfter(f2) && !i2.isAfter(f1);
    }

    /** Buchi fra le finestre effettive (già ordinate per inizio). Vuoto se una finestra è aperta da entrambi i lati. */
    static List<String> buchiPeriodi(String etichetta, List<Finestra> finestre) {
        List<String> avvisi = new ArrayList<>();
        List<LocalDate[]> range = new ArrayList<>();
        for (Finestra f : finestre) {
            if (f.inizio == null && f.fine == null) {
                return avvisi; // fallback interamente aperto : copre ogni buco
            }
            range.add(new LocalDate[] {
                f.inizio == null ? LocalDate.MIN : f.inizio,
                f.fine == null ? LocalDate.MAX : f.fine
            });
        }
        range.sort(Comparator.comparing(x -> x[0]));
        for (int k = 1; k < range.size(); k++) {
            LocalDate finePrec = range.get(k - 1)[1];
            LocalDate inizioSucc = range.get(k)[0];
            if (!finePrec.equals(LocalDate.MAX) && !inizioSucc.equals(LocalDate.MIN)
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
                String tipo = trim(r[COL_TIPO]);
                boolean fiat = TIPO_FIAT.equals(tipo);
                // bollo scritto solo per il rigo CRYPTO : sul rigo FIAT non è dovuto -> sempre null
                String bollo = fiat ? null : (r.length > COL_BOLLO ? r[COL_BOLLO] : null);
                // Provenienza : chi non la porta (chiamate legacy con array corti) lascia NULL = UTENTE.
                String origine = r.length > COL_ORIGINE ? nz(r[COL_ORIGINE]) : null;
                String chiaveDefault = r.length > COL_CHIAVE_DEFAULT ? r[COL_CHIAVE_DEFAULT] : null;
                DatabaseH2.Pers_GruppoPeriodoRW_Scrivi(gruppo,
                        tipo,
                        Integer.parseInt(trim(r[COL_PROGRESSIVO])),
                        nz(r[COL_DATA_INIZIO]), nz(r[COL_DATA_FINE]),
                        nz(r[COL_VAL_INIZIALE]), nz(r[COL_NOTA_INIZIALE]),
                        nz(r[COL_VAL_FINALE]), nz(r[COL_NOTA_FINALE]),
                        nz(r[COL_MOD_INIZIALE]), nz(r[COL_MOD_FINALE]),
                        nz(bollo), origine, chiaveDefault == null ? null : chiaveDefault.trim(),
                        // i dati fiscali esistono solo sul rigo FIAT
                        fiat ? nz(campo(r, COL_STATO_ESTERO)) : null,
                        fiat ? nz(campo(r, COL_IDENT_FISCALE)) : null,
                        fiat ? nz(campo(r, COL_NOTE_FISCALI)) : null,
                        fiat ? nz(campo(r, COL_FONTE_FISCALE)) : null,
                        fiat ? nz(campo(r, COL_IDENT_ISEE)) : null,
                        fiat ? nz(campo(r, COL_E_CONTO_CORRENTE)) : null);
            }
        }
        return errori;
    }

    /** Prossimo progressivo libero per un tipo, dato l'elenco righe già presente in tabella. */
    /**
     * L'elenco dei periodi <b>come sarebbe</b> dopo aver confermato {@code candidato} : la riga di
     * indice {@code indiceModificato} è sostituita, oppure il candidato è aggiunto in coda quando
     * l'indice è fuori intervallo (riga nuova, tipicamente {@code -1}).
     *
     * <p>Serve alla validazione incrociata del dialogo di inserimento/modifica : il candidato va
     * controllato <i>insieme</i> agli altri periodi (progressivo già usato, finestre sovrapposte) e
     * non da solo. <b>L'esclusione della riga in modifica è il punto delicato</b> : lasciandola dentro,
     * ogni modifica si scontrerebbe con sé stessa e nessuna riga esistente sarebbe più salvabile.</p>
     *
     * @param righe elenco corrente ({@code null} ammesso)
     * @param indiceModificato posizione della riga che si sta modificando, {@code -1} per una nuova
     * @param candidato la riga proposta
     */
    public static List<String[]> prospettivaConCandidato(List<String[]> righe, int indiceModificato,
            String[] candidato) {
        List<String[]> out = new ArrayList<>();
        if (righe != null) {
            for (int i = 0; i < righe.size(); i++) {
                if (i == indiceModificato) {
                    if (candidato != null) {
                        out.add(candidato);
                    }
                } else {
                    out.add(righe.get(i));
                }
            }
        }
        boolean sostituito = righe != null && indiceModificato >= 0 && indiceModificato < righe.size();
        if (candidato != null && !sostituito) {
            out.add(candidato);
        }
        return out;
    }

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

    /** Nome sorgente del movimento (campo {@code [3]}) normalizzato → id exchange di {@link DatabaseH2#EXCHANGE_NOTI}. */
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

    /** Il campo {@code i} della riga, già {@code trim}ato, tollerante alle righe più corte. */
    private static String campo(String[] r, int i) {
        return r != null && i >= 0 && i < r.length ? trim(r[i]) : "";
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
     * Righe chiave/valore per la tabella "Dati fiscali del gruppo" : alias, Stato estero e
     * identificativo fiscale <i>correnti</i> (dal periodo FIAT valido oggi), alias ISEE, stato del bollo.
     */
    public static List<String[]> datiFiscaliGruppo(String gruppo) {
        List<String[]> out = new ArrayList<>();
        if (gruppo == null || gruppo.isBlank()) {
            return out;
        }
        String[] alias = DatabaseH2.Pers_GruppoAlias_Leggi(gruppo);
        out.add(new String[] {"Alias", alias != null ? sv(alias[1]) : ""});

        String[] p = periodoFiatAllaData(gruppo, null);
        if (p == null) {
            out.add(new String[] {"Periodo FIAT corrente", "— nessuno (il gruppo non ha Stato estero)"});
        } else {
            out.add(new String[] {"Periodo FIAT corrente", descriviFinestra(p)});
            String stato = campo(p, COL_STATO_ESTERO);
            out.add(new String[] {"Stato estero (corrente)",
                stato.isEmpty() ? "—" : StatiEsteri.etichetta(stato)});
            out.add(new String[] {"Identificativo fiscale (corrente)", nzVuoto(campo(p, COL_IDENT_FISCALE))});
            out.add(new String[] {"Identificativo ISEE (corrente)", nzVuoto(campo(p, COL_IDENT_ISEE))});
            out.add(new String[] {"Fonte", nzVuoto(campo(p, COL_FONTE_FISCALE))});
        }

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

    /** {@code "dal 2025-06-20"} / {@code "fino al 2025-06-19"} / {@code "dal ... al ..."} / {@code "sempre"}. */
    private static String descriviFinestra(String[] r) {
        String di = campo(r, COL_DATA_INIZIO);
        String df = campo(r, COL_DATA_FINE);
        if (di.isEmpty() && df.isEmpty()) {
            return "n. " + campo(r, COL_PROGRESSIVO) + " (senza date)";
        }
        if (di.isEmpty()) {
            return "n. " + campo(r, COL_PROGRESSIVO) + " (fino al " + df + ")";
        }
        if (df.isEmpty()) {
            return "n. " + campo(r, COL_PROGRESSIVO) + " (dal " + di + ")";
        }
        return "n. " + campo(r, COL_PROGRESSIVO) + " (dal " + di + " al " + df + ")";
    }

    private static String nzVuoto(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    /**
     * Righe per la tabella "Periodi di detenzione" (sottoinsieme leggibile delle colonne di
     * {@link #caricaPeriodi}) : {@code [tipo, progr, dataInizio, dataFine, calcoloIniziale,
     * calcoloFinale, statoEstero, identFiscale, identISEE, bollo, origine]}. I valori iniziale/finale a
     * mano non ci sono più : restano nel DB ma non si mostrano (vedi {@link #COL_VAL_INIZIALE}).
     */
    public static List<String[]> periodiPerVista(String gruppo) {
        List<String[]> out = new ArrayList<>();
        for (String[] r : caricaPeriodi(gruppo)) {
            String tipo = sv(r[COL_TIPO]);
            boolean fiat = TIPO_FIAT.equals(tipo);
            String stato = campo(r, COL_STATO_ESTERO);
            out.add(new String[] {
                tipo, sv(r[COL_PROGRESSIVO]), sv(r[COL_DATA_INIZIO]), sv(r[COL_DATA_FINE]),
                etichettaModalita(r[COL_MOD_INIZIALE], MODALITA_INIZIALE),
                etichettaModalita(r[COL_MOD_FINALE], MODALITA_FINALE),
                fiat ? (stato.isEmpty() ? "" : StatiEsteri.etichetta(stato)) : "n/d",
                fiat ? campo(r, COL_IDENT_FISCALE) : "n/d",
                fiat ? campo(r, COL_IDENT_ISEE) : "n/d",
                fiat ? "n/d" : (r.length > COL_BOLLO ? sv(r[COL_BOLLO]) : ""),
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
                riconciliaGruppoExchange(ex);
            }
            for (DatiPredefinitiRW.GruppoPeriodiPredef g : p.periodiDetenzioneGruppi()) {
                riconciliaPeriodiDetenzione(g);
            }
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_HASH_PREDEFINITI, p.hash());
        } catch (RuntimeException e) {
            LoggerGC.ScriviErrore(e);
        }
    }

    /**
     * Semina il gruppo wallet preconfigurato di un exchange noto ({@code "Wallet 101"} ↔ Binance, ...) :
     * lo crea se non c'è e ne allinea l'alias, ma <b>solo</b> se la riga è nuova o {@code SISTEMA}.
     * I periodi di quel gruppo — righi FIAT con i dati fiscali compresi — arrivano dal blocco
     * {@code periodiDetenzioneGruppi}, in cui {@link DatiPredefinitiRW} riversa anche i
     * {@code periodiFiscali} scritti sotto l'exchange.
     */
    private static void riconciliaGruppoExchange(DatiPredefinitiRW.ExchangePredef ex) {
        if (ex.gruppo == null || ex.gruppo.isBlank()) {
            return;
        }
        String[] alias = DatabaseH2.Pers_GruppoAlias_Leggi(ex.gruppo);
        boolean nuovo = alias[0] == null;
        boolean sistema = ORIGINE_SISTEMA.equals(alias.length > 3 ? alias[3] : null);
        if (nuovo || sistema) {
            boolean pagaBollo = "S".equals(alias.length > 2 ? alias[2] : null);
            DatabaseH2.Pers_GruppoAlias_Scrivi(ex.gruppo, ex.nome, nuovo ? false : pagaBollo, ORIGINE_SISTEMA);
        }
    }

    private static void riconciliaPeriodiDetenzione(DatiPredefinitiRW.GruppoPeriodiPredef g) {
        List<String[]> esistenti = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo(g.gruppo);
        // riga : [0]key [1]gruppo [2]tipo [3]prog [4]di [5]df [6]valI [7]notaI [8]valF [9]notaF
        //        [10]modI [11]modF [12]bollo [13]origine [14]chiaveDefault [15]stato [16]ident
        //        [17]noteFisc [18]fonteFisc [19]identISEE [20]eContoCorrente
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

    /**
     * Scrive una riga predefinita. L'<b>identificativo ISEE non viene mai passato</b> ({@code null} =
     * colonna non toccata) : è il campo che l'utente compila a mano anche sulle righe che arrivano dal
     * programma, e un aggiornamento dei predefiniti non deve cancellarglielo.
     */
    private static void scriviPeriodoDetPredef(String gruppo, String tipo, int prog,
            DatiPredefinitiRW.PeriodoDetPredef pd) {
        boolean fiat = TIPO_FIAT.equals(tipo);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi(gruppo, tipo, prog, nn(pd.dataInizio), nn(pd.dataFine),
                nn(pd.valIniziale), nn(pd.notaIniziale), nn(pd.valFinale), nn(pd.notaFinale),
                nn(pd.calcoloIniziale), nn(pd.calcoloFinale), fiat ? null : nn(pd.bollo),
                ORIGINE_SISTEMA, pd.chiave,
                fiat ? nn(pd.statoEstero) : null, fiat ? nn(pd.identificativoFiscale) : null,
                fiat ? nn(pd.note) : null, fiat ? nn(pd.fonte) : null, null,
                fiat ? nn(pd.contoCorrente) : null);
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
        boolean fiat = TIPO_FIAT.equals(pd.tipo);
        String[] r = new String[COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
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
        r[COL_BOLLO] = fiat ? "" : sv(pd.bollo);
        r[COL_ORIGINE] = ORIGINE_SISTEMA;
        r[COL_CHIAVE_DEFAULT] = sv(pd.chiave);
        r[COL_STATO_ESTERO] = fiat ? sv(pd.statoEstero) : "";
        r[COL_IDENT_FISCALE] = fiat ? sv(pd.identificativoFiscale) : "";
        r[COL_NOTE_FISCALI] = fiat ? sv(pd.note) : "";
        r[COL_FONTE_FISCALE] = fiat ? sv(pd.fonte) : "";
        r[COL_E_CONTO_CORRENTE] = fiat ? sv(pd.contoCorrente) : "";
        // L'identificativo ISEE non ha un default : è sempre inserito a mano. Il "ripristina al
        // default" lo azzera come qualsiasi altro campo non predefinito.
        r[COL_IDENT_ISEE] = "";
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
