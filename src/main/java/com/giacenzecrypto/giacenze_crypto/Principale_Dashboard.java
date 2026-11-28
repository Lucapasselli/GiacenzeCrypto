package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Logica della scheda <i>Dashboard</i>: un riepilogo <b>per anno</b> dei risultati che il motore ha già scritto nelle righe
 * dei movimenti. Non ricalcola niente e non prende nessuna decisione fiscale: somma i campi come li somma la tabella dei
 * movimenti ({@link Principale_PlusvalenzeAliquote#Totali}), quindi i numeri sono gli stessi dei tre riquadri della scheda
 * <i>Transazioni Crypto</i> con l'anno intero selezionato.
 *
 * <p>Regole da non rompere:</p>
 * <ul>
 * <li><b>È informativo</b>: il quadro RT ha la sua elaborazione (soglia ante 2023, compensazioni, derivati con il conto), che
 * qui non si conosce. Il testo della scheda lo dice, e nessuno scrive questi totali nell'archivio delle dichiarazioni.</li>
 * <li><b>I derivati stanno a parte</b>: il risultato c-quater è in {@code [46]} e non va mai sommato alla plusvalenza
 * cripto ({@code v[19]}). La colonna è la stessa regola del quadro RT ({@link Principale_RT_Derivati#Calcola}).</li>
 * <li>Plusvalenze e minusvalenze sono la parte positiva e quella negativa di {@code v[19]} <b>riga per riga</b>, non il
 * segno del totale: un anno con più cessioni in guadagno e altre in perdita mostra entrambe.</li>
 * <li>L'anno è quello di {@code v[1]}: i movimenti senza data leggibile non entrano in nessun anno e sono contati a parte
 * ({@link Riepilogo#senzaData}), perché la somma delle righe annuali deve restare quella del totale.</li>
 * </ul>
 * Solo logica: nessun Swing, quello che serve arriva dai parametri. Il pannello è {@link GUI_Dashboard}.
 */
final class Principale_Dashboard {

    private Principale_Dashboard() {}

    /**
     * Un anno.
     *
     * @param anno l'anno (0 per la riga del totale storico)
     * @param movimenti quante righe del registro (comprese le generate)
     * @param wallet quante coppie exchange/wallet distinte
     * @param documenti quanti documenti di origine o di supporto distinti
     * @param costi costi di carico delle righe con effetto sul riepilogo ({@code v[33]} = S)
     * @param corrispettivi corrispettivi delle stesse righe
     * @param plusvalenze somma delle sole {@code v[19]} positive
     * @param minusvalenze somma delle sole {@code v[19]} negative, con il segno (zero o negativa)
     * @param derivati risultato dei derivati dell'anno (c-quater), {@code null} se l'anno non ne ha
     * @param dichiarazioni le dichiarazioni archiviate per l'anno, già scritte per l'utente (vuoto se non ce ne sono)
     * @param errori gli errori dell'anno che si riconoscono dalle righe (gli altri sono solo globali, vedi {@link ErroriAnno})
     */
    record RigaAnno(int anno, int movimenti, int wallet, int documenti, BigDecimal costi, BigDecimal corrispettivi,
            BigDecimal plusvalenze, BigDecimal minusvalenze, BigDecimal derivati, String dichiarazioni, ErroriAnno errori) {

        /** La plusvalenza netta cripto: è la somma di {@code v[19]}, la stessa del riquadro della tabella. */
        BigDecimal netto() {
            return plusvalenze.add(minusvalenze);
        }
    }

    /**
     * Gli errori che stanno <b>nella riga</b> del movimento e quindi si possono attribuire a un anno: gli stessi tre contatori del pulsante
     * <i>Errori</i> calcolati dal ciclo di caricamento ({@link Principale_Errori#NonClassificato}, {@link Principale_Errori#SenzaPrezzo},
     * {@link Principale_Errori#LifoMancante}). Giacenze negative, movimenti collegati e trasferimenti senza collegamento nascono da controlli
     * sull'intero registro e restano solo nel totale del pulsante.
     */
    record ErroriAnno(int nonClassificati, int senzaPrezzo, int lifoMancante) {
        int totale() {
            return nonClassificati + senzaPrezzo + lifoMancante;
        }
    }

    /** Le righe annuali (dall'anno più vecchio al più recente), il totale storico e le righe senza data. */
    record Riepilogo(List<RigaAnno> anni, RigaAnno totale, int senzaData) {
        RigaAnno anno(int anno) {
            return anni.stream().filter(r -> r.anno() == anno).findFirst().orElse(null);
        }
    }

    /** Raccoglie le righe di un anno. */
    private static final class Accumulo {
        int movimenti;
        int nonClassificati;
        int senzaPrezzo;
        int lifoMancante;
        BigDecimal costi = BigDecimal.ZERO;
        BigDecimal corrispettivi = BigDecimal.ZERO;
        BigDecimal plus = BigDecimal.ZERO;
        BigDecimal minus = BigDecimal.ZERO;
        final Set<String> wallet = new HashSet<>();
        final Set<Integer> documenti = new HashSet<>();
        final List<String[]> righe = new ArrayList<>();
    }

    /**
     * @param movimenti il registro (l'ordine non conta)
     * @param dichiarazioniPerAnno per ogni anno il testo delle dichiarazioni archiviate, ad esempio {@code "RW definitiva · RT archiviata"}
     */
    static Riepilogo Calcola(Collection<String[]> movimenti, Map<Integer, String> dichiarazioniPerAnno) {
        Map<Integer, Accumulo> perAnno = new TreeMap<>();
        Accumulo storico = new Accumulo();
        int senzaData = 0;
        for (String[] v : movimenti) {
            if (v == null || v.length <= 33) continue;
            int anno = Anno(v[1]);
            if (anno <= 0) {
                senzaData++;
                continue;
            }
            Accumulo a = perAnno.computeIfAbsent(anno, k -> new Accumulo());
            Aggiungi(a, v);
            Aggiungi(storico, v);
        }
        List<RigaAnno> righe = new ArrayList<>();
        for (Map.Entry<Integer, Accumulo> e : perAnno.entrySet()) {
            Accumulo a = e.getValue();
            Principale_RT_Derivati.Esito der = Principale_RT_Derivati.Calcola(Integer.toString(e.getKey()), a.righe);
            BigDecimal derivati = der.presente() ? der.risultatoCentesimi() : null;
            righe.add(Riga(e.getKey(), a, derivati, dichiarazioniPerAnno.getOrDefault(e.getKey(), "")));
        }
        BigDecimal derivatiTotali = null;
        for (RigaAnno r : righe) {
            if (r.derivati() != null) derivatiTotali = (derivatiTotali == null ? BigDecimal.ZERO : derivatiTotali).add(r.derivati());
        }
        return new Riepilogo(List.copyOf(righe), Riga(0, storico, derivatiTotali, ""), senzaData);
    }

    private static RigaAnno Riga(int anno, Accumulo a, BigDecimal derivati, String dichiarazioni) {
        return new RigaAnno(anno, a.movimenti, a.wallet.size(), a.documenti.size(), a.costi, a.corrispettivi, a.plus, a.minus,
                derivati, dichiarazioni, new ErroriAnno(a.nonClassificati, a.senzaPrezzo, a.lifoMancante));
    }

    /** Le stesse regole di somma di {@link Principale_PlusvalenzeAliquote#Totali}, divise in parte positiva e negativa. */
    private static void Aggiungi(Accumulo a, String[] v) {
        a.movimenti++;
        a.righe.add(v);
        if (Principale_Errori.NonClassificato(v)) a.nonClassificati++;
        if (Principale_Errori.SenzaPrezzo(v)) a.senzaPrezzo++;
        if (Principale_Errori.LifoMancante(v)) a.lifoMancante++;
        if (Funzioni.isNumeric(v[19], false)) {
            BigDecimal p = new BigDecimal(v[19]);
            if (p.signum() > 0) a.plus = a.plus.add(p);
            else a.minus = a.minus.add(p);
        }
        if ("S".equals(v[33])) {
            a.corrispettivi = a.corrispettivi.add(NumeroOZero(v[15]));
            a.costi = a.costi.add(NumeroOZero(v[16]));
        }
        String exchange = v[3] == null ? "" : v[3].trim();
        String wallet = v.length > 4 && v[4] != null ? v[4].trim() : "";
        if (!(exchange + wallet).isEmpty()) a.wallet.add(exchange + " / " + wallet);
        if (v.length > 41 && v[41] != null && !v[41].isBlank()) {
            int origine = DocumentiFonte.IdDaCampo41(v[41]);
            if (origine > 0) a.documenti.add(origine);
            a.documenti.addAll(DocumentiFonte.Supporti(v[41]));
        }
    }

    private static BigDecimal NumeroOZero(String s) {
        return s != null && Funzioni.isNumeric(s, false) ? new BigDecimal(s) : BigDecimal.ZERO;
    }

    /** @return l'anno di una data {@code yyyy-MM-dd...}, 0 se non è leggibile */
    static int Anno(String data) {
        if (data == null || data.length() < 10 || data.charAt(4) != '-' || data.charAt(7) != '-') return 0;
        try {
            return Integer.parseInt(data.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Le dichiarazioni archiviate, anno per anno, come testo per l'utente: {@code "RW definitiva · RT archiviata"}. Una
     * famiglia compare una volta sola, con lo stato migliore fra le sue dichiarazioni (una definitiva vale più di una bozza).
     */
    static Map<Integer, String> DichiarazioniPerAnno(Collection<Dichiarazioni.Dichiarazione> elenco) {
        Map<Integer, Map<Dichiarazioni.Famiglia, Boolean>> stati = new TreeMap<>();
        for (Dichiarazioni.Dichiarazione d : elenco) {
            int anno = d.Anno == null ? 0 : Anno(d.Anno.trim() + "-01-01");
            if (anno <= 0 || d.Fam == null) continue;
            stati.computeIfAbsent(anno, k -> new TreeMap<>()).merge(d.Fam, d.Definitiva, Boolean::logicalOr);
        }
        Map<Integer, String> testo = new TreeMap<>();
        stati.forEach((anno, famiglie) -> {
            Set<String> voci = new TreeSet<>();
            famiglie.forEach((fam, definitiva) -> voci.add(fam.name() + (definitiva ? " definitiva" : " archiviata")));
            testo.put(anno, String.join(" · ", voci));
        });
        return testo;
    }

    /**
     * Il valore delle cripto-attività all'inizio e alla fine di un anno, a prezzo di mercato o personalizzato.
     *
     * @param inizio valore al 1° gennaio alle 00:00
     * @param fine valore a fine anno ({@code null} se l'anno è appena iniziato e non c'è ancora un fine)
     * @param fineAl l'ultimo giorno compreso nel valore di fine ({@code yyyy-MM-dd}, per l'anno in corso è ieri)
     * @param senzaPrezzo le monete (distinte) che avevano una giacenza ma di cui non si è trovato il prezzo: il loro valore non è nella somma
     * @param negative le monete con giacenza negativa, valutate zero (sono già fra gli errori del registro)
     */
    record ValoreAnno(BigDecimal inizio, BigDecimal fine, String fineAl, int senzaPrezzo, int negative) {
    }

    /**
     * Somma le righe di {@code Calcoli_RW_Giacenze} (colonna 5 valore iniziale, 10 valore finale). È la sola logica della Dashboard sul
     * patrimonio: la fotografia delle giacenze resta quella del quadro RW, qui non se ne fa una seconda.
     *
     * <p>Un prezzo {@code null} non è zero: la moneta è contata fra quelle senza prezzo e il suo valore manca dalla somma. {@code 0.0000}
     * ({@code Calcoli_RW_Giacenze.VALORE_ZERO}) è una giacenza nulla o negativa, non un prezzo mancante.</p>
     */
    static ValoreAnno ValoreDaRighe(Map<String, List<String[]>> righePerGruppo, boolean conFine, String fineAl) {
        BigDecimal inizio = BigDecimal.ZERO;
        BigDecimal fine = BigDecimal.ZERO;
        Set<String> senzaPrezzo = new TreeSet<>();
        Set<String> negative = new TreeSet<>();
        for (List<String[]> righe : righePerGruppo.values()) {
            for (String[] r : righe) {
                if (r == null || r.length < 16) continue;
                String moneta = (r[2] == null || r[2].isBlank() ? r[7] : r[2]) + "";
                //Un token SCAM non e' un prezzo da cercare: come per i contatori degli errori, non si segnala
                boolean scam = Funzioni.isSCAM(moneta);
                if (Calcoli_RW_Giacenze.ERRORE_GIACENZA_NEGATIVA.equals(r[15]) && !scam) negative.add(moneta);
                BigDecimal i = Importo(r[5]);
                if (i == null) {
                    if (!scam) senzaPrezzo.add(moneta);
                } else {
                    inizio = inizio.add(i);
                }
                if (conFine) {
                    BigDecimal f = Importo(r[10]);
                    if (f == null) {
                        if (!scam) senzaPrezzo.add(moneta);
                    } else {
                        fine = fine.add(f);
                    }
                }
            }
        }
        return new ValoreAnno(inizio, conFine ? fine : null, conFine ? fineAl : null, senzaPrezzo.size(), negative.size());
    }

    private static BigDecimal Importo(String s) {
        return s != null && Funzioni.isNumeric(s, false) ? new BigDecimal(s) : null;
    }

    /**
     * L'ultimo giorno da valutare per un anno: il 31/12 per gli anni chiusi, ieri per l'anno in corso (i prezzi di oggi non sono ancora
     * finiti), {@code null} se non c'è niente da valutare (anno futuro, o 1° gennaio dell'anno in corso).
     */
    static String FineValutazione(int anno, java.time.LocalDate oggi) {
        if (anno < oggi.getYear()) return anno + "-12-31";
        if (anno > oggi.getYear()) return null;
        java.time.LocalDate ieri = oggi.minusDays(1);
        return ieri.getYear() == anno ? ieri.toString() : null;
    }

    /**
     * Calcola il valore di un anno con la fotografia delle giacenze del quadro RW, <b>solo con i prezzi già nel programma</b> (nessun
     * scaricamento: un prezzo che non c'è è un prezzo mancante, contato e segnalato). Resta un calcolo su tutto il registro: va chiamato
     * <b>fuori dall'EDT</b>, e legge {@code MappaCryptoWallet} senza copiarla (può lanciare {@link java.util.ConcurrentModificationException}).
     */
    static ValoreAnno CalcolaValore(int anno, java.time.LocalDate oggi) {
        String fine = FineValutazione(anno, oggi);
        String a = Integer.toString(anno);
        //Senza rete: i prezzi sono quelli gia' nel programma (cache e personalizzati), niente si scarica aprendo una scheda
        Map<String, List<String[]>> righe = Funzioni.SenzaRete(() -> Calcoli_RW_Giacenze.GiacenzeFinestra(a, a + "-01-01",
                fine == null ? a + "-01-01" : fine, Calcoli_RW_Giacenze.PREZZO_MERCATO));
        return ValoreDaRighe(righe, fine != null, fine);
    }

    /** @return un importo come lo mostra la scheda: {@code € 1.234,56}, con il meno davanti se negativo */
    static String Euro(BigDecimal v) {
        if (v == null) return "-";
        return (v.signum() < 0 ? "-" : "") + "€ " + Funzioni.formattaBigDecimal(v.abs(), true);
    }

    /** @return l'importo con il segno sempre scritto ({@code +€ ...} o {@code -€ ...}), per i risultati */
    static String EuroConSegno(BigDecimal v) {
        if (v == null) return "-";
        return (v.signum() > 0 ? "+" : v.signum() < 0 ? "-" : "") + "€ " + Funzioni.formattaBigDecimal(v.abs(), true);
    }
}
