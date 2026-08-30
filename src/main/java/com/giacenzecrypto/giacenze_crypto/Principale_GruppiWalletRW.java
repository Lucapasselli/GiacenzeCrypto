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
 * Logica operativa della configurazione del quadro W/RW per i gruppi wallet:
 * riferimento estero del gruppo (stato / P.IVA o exchange) e periodi di detenzione
 * (righi CRYPTO / FIAT, valori campo 7-8 manuali, modalità di calcolo).
 *
 * <p>Companion di {@code Principale} nello stile di {@link Principale_GiacenzeaData}:
 * metodi {@code public static}, nessun campo Swing, nessun riferimento a {@code Principale}.
 * Persistenza via {@code DatabaseH2.Pers_ExchangeAnagrafica_* / Pers_GruppoRiferimento_* /
 * Pers_GruppoPeriodoRW_*} (Fase 1).</p>
 *
 * <p><b>Fase 2 : solo dati.</b> Nulla di qui è ancora letto da
 * {@code Calcoli_RW.AggiornaRWFR}. La validazione dei periodi è volutamente
 * <i>strutturale</i> (tipo, progressivo, formato date) e non fiscale : sovrapposizioni,
 * deduzione della data di inizio dal primo movimento, generazione dei righi RW sono
 * materia della Fase 3, da riprogettare.</p>
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
     * ModalitaCalcoloIniziale, ModalitaCalcoloFinale]} — 10 colonne, senza chiave sintetica né gruppo.
     */
    public static final int COL_TIPO = 0, COL_PROGRESSIVO = 1, COL_DATA_INIZIO = 2, COL_DATA_FINE = 3,
            COL_VAL_INIZIALE = 4, COL_NOTA_INIZIALE = 5, COL_VAL_FINALE = 6, COL_NOTA_FINALE = 7,
            COL_MOD_INIZIALE = 8, COL_MOD_FINALE = 9;
    public static final int COLONNE_PERIODO = 10;

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
            String stato = ex[2] != null && !ex[2].isBlank() ? ex[2] : "?";
            return "exchange : " + nome + " (stato " + stato + ")";
        }
        // STATO manuale
        String stato = r[2] != null && !r[2].isBlank() ? r[2] : "?";
        String piva = r[3] != null && !r[3].isBlank() ? " · " + r[3] : "";
        return "stato " + stato + piva;
    }

    /** Stato estero effettivo del gruppo (risolve STATO vs EXCHANGE). {@code ""} se non impostato. */
    public static String statoEsteroEffettivo(String gruppo) {
        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (r[1] == null) {
            return "";
        }
        if (RIFERIMENTO_EXCHANGE.equals(r[1]) && r[4] != null && !r[4].isBlank()) {
            String s = DatabaseH2.Pers_ExchangeAnagrafica_Leggi(r[4])[2];
            return s == null ? "" : s;
        }
        return r[2] == null ? "" : r[2];
    }

    /** Identificativo fiscale / P.IVA effettivo del gruppo (per l'ISEE futura). {@code ""} se non impostato. */
    public static String identificativoFiscaleEffettivo(String gruppo) {
        String[] r = DatabaseH2.Pers_GruppoRiferimento_Leggi(gruppo);
        if (r[1] == null) {
            return "";
        }
        if (RIFERIMENTO_EXCHANGE.equals(r[1]) && r[4] != null && !r[4].isBlank()) {
            String s = DatabaseH2.Pers_ExchangeAnagrafica_Leggi(r[4])[3];
            return s == null ? "" : s;
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
            //       ValIniManuale, NotaIni, ValFinManuale, NotaFin, ModIni, ModFin]
            out.add(new String[] {
                db[2], db[3], db[4], db[5], db[6], db[7], db[8], db[9], db[10], db[11]
            });
        }
        return out;
    }

    /**
     * Validazione STRUTTURALE dei periodi (non fiscale). Regole :
     * <ul>
     *   <li>tipo ∈ {CRYPTO, FIAT}</li>
     *   <li>progressivo intero >= 1, unico per (tipo)</li>
     *   <li>date, se valorizzate, in formato {@code yyyy-MM-dd} valido</li>
     *   <li>data inizio <= data fine quando entrambe presenti</li>
     *   <li>modalità di calcolo ∈ valori ammessi (o vuota)</li>
     * </ul>
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
            if (r == null || r.length < COLONNE_PERIODO) {
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
        }
        return errori;
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
                DatabaseH2.Pers_GruppoPeriodoRW_Scrivi(gruppo,
                        trim(r[COL_TIPO]),
                        Integer.parseInt(trim(r[COL_PROGRESSIVO])),
                        nz(r[COL_DATA_INIZIO]), nz(r[COL_DATA_FINE]),
                        nz(r[COL_VAL_INIZIALE]), nz(r[COL_NOTA_INIZIALE]),
                        nz(r[COL_VAL_FINALE]), nz(r[COL_NOTA_FINALE]),
                        nz(r[COL_MOD_INIZIALE]), nz(r[COL_MOD_FINALE]));
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
}
