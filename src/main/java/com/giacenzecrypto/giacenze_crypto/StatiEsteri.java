package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabella dei codici degli Stati / territori esteri usata dal quadro W/RW e dall'ISEE
 * (codice a 3 cifre + denominazione). Fonte : <b>Tabella 10 "Elenco dei Paesi e dei Territori
 * esteri"</b> delle istruzioni per la compilazione del modello Redditi PF (fascicolo 1),
 * edizione 2026 — la stessa tabella richiamata nei campi "Stato estero" dei quadri RW/RT.
 *
 * <p>Serve a far scegliere lo Stato da un elenco invece di doverne indovinare il numero : chi
 * conosce lo Stato non deve fare una ricerca a parte. L'elenco cambia di rado, con l'aggiornamento annuale della
 * modulistica, ma quando cambia (come l'elenco dei privilegiati del D.M. 4/5/1999) non deve voler dire una versione
 * nuova : i dati stanno in {@code config/varie/StatiEsteri.json}, riallineato dal repository come gli altri file
 * di quella cartella. Cambiare il campo {@code privilegiato} cambia numeri fiscali (IVAFE), non solo un'etichetta.</p>
 */
public final class StatiEsteri {

    private StatiEsteri() {
    }

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "StatiEsteri";

    /** Il contenuto del file: voci {codice, denominazione} nell'ordine del file e codici dell'elenco del D.M. 4/5/1999. */
    record Dati(String[][] elenco, java.util.Set<String> privilegiati) {}

    private static volatile Dati dati;

    /**
     * {codice a 3 cifre, denominazione (grafia della Tabella 10)}, da {@code config/varie/StatiEsteri.json}. Tenuto in
     * memoria e rimpiazzato da {@link #Ricarica()} quando l'aggiornamento in background porta un file nuovo. Ordine :
     * per codice numerico non è garantito.
     */
    public static String[][] elenco() {
        return dati().elenco();
    }

    private static Dati dati() {
        Dati d = dati;
        if (d == null) {
            d = Carica();
            dati = d;
        }
        return d;
    }

    /** Rilegge il file (disco, poi copia nel jar). */
    static void Ricarica() {
        dati = Carica();
    }

    /**
     * Il file è l'unica copia dei dati : non c'è una lista di ripiego nel codice, che sarebbe una seconda copia da
     * tenere allineata. La copia di default sta nel jar, quindi un file mancante da entrambe le parti è una
     * installazione rotta : lo si dice nel log e lo Stato resta senza nome e non privilegiato.
     */
    private static Dati Carica() {
        Dati d = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, StatiEsteri::Interpreta);
        return d != null ? d : new Dati(new String[0][], java.util.Set.of());
    }

    /** @return i dati, oppure {@code null} se il contenuto non è valido (codici non a 3 cifre, doppi, nomi vuoti, lista vuota) */
    static Dati Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) {
            return null;
        }
        try {
            org.json.JSONArray voci = new org.json.JSONObject(contenuto).getJSONArray("stati");
            List<String[]> elenco = new ArrayList<>();
            java.util.Set<String> visti = new java.util.HashSet<>();
            java.util.Set<String> privilegiati = new java.util.HashSet<>();
            for (int i = 0; i < voci.length(); i++) {
                org.json.JSONObject v = voci.getJSONObject(i);
                String codice = v.getString("codice").trim();
                String denominazione = v.getString("nome").trim();
                if (!codice.matches("\\d{3}") || denominazione.isEmpty() || !visti.add(codice)) {
                    LoggerGC.ScriviErrore("StatiEsteri: voce non valida o doppia in " + nome + ".json : " + codice);
                    return null;
                }
                elenco.add(new String[]{codice, denominazione});
                if (v.optBoolean("privilegiato", false)) {
                    privilegiati.add(codice);
                }
            }
            return elenco.isEmpty() ? null : new Dati(elenco.toArray(new String[0][]), java.util.Set.copyOf(privilegiati));
        } catch (Exception e) {
            LoggerGC.ScriviErrore("StatiEsteri: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }

    /** Voce vuota in testa alla combo (nessuno Stato indicato). */
    public static final String VOCE_VUOTA = "(nessuno)";

    /**
     * Valore sentinella per "conto detenuto in Italia". L'Italia <b>non</b> ha un codice nella
     * Tabella 10 "Elenco Paesi e Territori esteri" (verificato sulle istruzioni Redditi PF 2026 :
     * 085 = Sri Lanka, 087 = Albania, nessun 086), perché il quadro RW monitora le sole attività
     * <i>estere</i>. Qui è una voce speciale del campo Stato estero : quando è selezionata, la parte
     * FIAT del quadro W/RW per quel periodo non va compilata — {@code Calcoli_RW_Fiat.generaRighiFiat}
     * non emette righi per i tratti che ricadono in un periodo con questo valore. È
     * l'ISO&#8209;3166 alpha&#8209;2 {@code "IT"} : due lettere, non confondibile con un codice a
     * 3 cifre della Tabella 10 e sta nella colonna {@code StatoEstero VARCHAR(3)} del DB.
     */
    public static final String CODICE_ITALIA = "IT";

    private static final String SEP = "  —  "; // " — "

    /*
     * I codici dell'elenco del D.M. 4 maggio 1999 (art. 19 c. 20-bis D.L. 201/2011, IVAFE maggiorata; art. 5 c. 2
     * D.L. 167/1990, sanzioni) sono il campo "privilegiato" delle voci di config/varie/StatiEsteri.json, dove stanno
     * anche le ragioni per cui una sola lista basta per tutti gli anni e i 58 codici tornano con le 55 voci
     * pubblicate (chiave "nota_privilegiati").
     */

    /**
     * {@code true} se il codice Stato estero è fra quelli dell'elenco del D.M. 4 maggio 1999
     * (campo {@code privilegiato} del file). Un codice vuoto o sconosciuto è {@code false} : non si presume la
     * fiscalità privilegiata di uno Stato che l'utente non ha indicato.
     */
    public static boolean isPrivilegiato(String codice) {
        return codice != null && dati().privilegiati().contains(codice.trim());
    }

    /** I codici dell'elenco del D.M. 4 maggio 1999, per i test e le diagnostiche. */
    public static java.util.Set<String> codiciPrivilegiati() {
        return dati().privilegiati();
    }

    /** {@code true} se il codice è il valore sentinella {@link #CODICE_ITALIA} ("conto in Italia"). */
    public static boolean isItalia(String codice) {
        return CODICE_ITALIA.equals(codice == null ? "" : codice.trim());
    }

    /** Denominazione del codice, o {@code ""} se non in tabella. */
    public static String nome(String codice) {
        if (codice == null) {
            return "";
        }
        String c = codice.trim();
        if (isItalia(c)) {
            return "conto in Italia (nessun rigo RW valuta)";
        }
        for (String[] r : elenco()) {
            if (r[0].equals(c)) {
                return r[1];
            }
        }
        return "";
    }

    /** {@code "092  —  LUSSEMBURGO"} ; {@link #VOCE_VUOTA} se {@code codice} è vuoto ; {@code "<codice> (non in elenco)"} se sconosciuto. */
    public static String etichetta(String codice) {
        String c = codice == null ? "" : codice.trim();
        if (c.isEmpty()) {
            return VOCE_VUOTA;
        }
        String n = nome(c);
        return n.isEmpty() ? c + " (non in elenco)" : c + SEP + n;
    }

    /** Il codice a 3 cifre estratto da una etichetta della combo ; {@code ""} per {@link #VOCE_VUOTA}. */
    public static String codiceDaEtichetta(String etichetta) {
        if (etichetta == null) {
            return "";
        }
        String e = etichetta.trim();
        if (e.isEmpty() || VOCE_VUOTA.equals(e)) {
            return "";
        }
        int i = e.indexOf(SEP);
        String testa = (i >= 0 ? e.substring(0, i) : e).trim();
        // "092 (non in elenco)" -> "092"
        int sp = testa.indexOf(' ');
        return sp > 0 ? testa.substring(0, sp) : testa;
    }

    /**
     * Le voci per una {@code JComboBox} del campo "Stato estero", ordinate per denominazione, con la
     * {@link #VOCE_VUOTA} in testa. Se {@code codiceCorrente} non è in tabella (valore legacy digitato
     * a mano) viene aggiunto come ultima voce così che la selezione corrente sopravviva.
     */
    public static String[] etichetteCombo(String codiceCorrente) {
        List<String[]> ordinato = new ArrayList<>(List.of(elenco()));
        ordinato.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
        List<String> voci = new ArrayList<>();
        voci.add(VOCE_VUOTA);
        voci.add(CODICE_ITALIA + SEP + nome(CODICE_ITALIA)); // voce speciale : conto in Italia
        for (String[] r : ordinato) {
            voci.add(r[0] + SEP + r[1]);
        }
        String c = codiceCorrente == null ? "" : codiceCorrente.trim();
        if (!c.isEmpty() && nome(c).isEmpty()) {
            voci.add(c + " (non in elenco)");
        }
        return voci.toArray(new String[0]);
    }
}
