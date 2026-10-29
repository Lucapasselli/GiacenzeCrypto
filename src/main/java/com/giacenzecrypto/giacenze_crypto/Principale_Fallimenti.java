package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Logica operativa della scheda "Fallimenti piattaforme" (GUI {@link GUI_FallimentiPiattaforme_Pannello}): metodi
 * statici, nessun campo Swing. Costruisce le righe dei movimenti di una procedura; i marcatori, le tabelle e il
 * registro dei token credito stanno in {@link FallimentiPiattaforme}.
 *
 * <p><b>Il blocco</b> (decisioni in {@code nocommit/Documentazione/Analisi_Fallimenti_Piattaforme.md} §7-8): le
 * giacenze della piattaforma alla data della domanda di fallimento (movimenti dei giorni precedenti) vengono prima
 * spostate con giroconti interni ({@code TI}) nel sotto-wallet {@value #SOTTOWALLET}, poi ogni moneta passa al suo
 * token credito con <b>una sola</b> riga {@code FBL}: così il token credito nasce con un solo lotto LIFO e ogni
 * tranche ne scarica esattamente la frazione di costo prevista. Le righe sono {@code [22] = "M"} (inserite
 * dall'utente: la chiave {@code [45]} non viene tolta al caricamento), hanno il valore anche in {@code [14]} (così
 * nessun ricalcolo dei prezzi le riprezza) e portano la chiave {@code FALL-<id>}.
 */
public final class Principale_Fallimenti {

    /** Sotto-wallet in cui si raccolgono i crediti della procedura. */
    public static final String SOTTOWALLET = "Credito fallimento";

    private Principale_Fallimenti() {
    }

    /** Una giacenza della piattaforma alla data della domanda: sotto-wallet e moneta. */
    public static final class Posizione {
        public String SottoWallet = "";
        public String Moneta = "";
        public String Tipo = "Crypto";
        public String Address = "";
        public String Rete = "";
        public BigDecimal Qta = BigDecimal.ZERO;
        /** Valore del credito in euro alla data della domanda, {@code null} se non trovato. */
        public BigDecimal ValoreEUR;

        /** @return la chiave della moneta, come in Giacenze a data ({@code Moneta;Tipo;Address;Rete}) */
        public String ChiaveMoneta() {
            return Moneta + ";" + Tipo + ";" + Address + ";" + Rete;
        }
    }

    /** @return gli exchange ({@code [3]}) presenti nei movimenti, in ordine alfabetico */
    public static List<String> Exchange() {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] != null && !v[3].isBlank()) s.add(v[3].trim());
        }
        return new ArrayList<>(s);
    }

    /** @return il nome breve proposto per una piattaforma: maiuscolo, solo lettere e cifre ({@code Nexo Pro} → {@code NEXOPRO}) */
    public static String NomeBreveProposto(String Exchange) {
        return Exchange == null ? "" : Exchange.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /** @return il nome breve ripulito: maiuscolo, solo lettere, cifre e trattino */
    public static String NormalizzaNomeBreve(String Nome) {
        return Nome == null ? "" : Nome.toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }

    /** @return il nome del token credito ({@code CELSIUS.CREDITO.BTC}) */
    public static String NomeToken(String NomeBreve, String Simbolo) {
        return NomeBreve + ".CREDITO." + Simbolo.replace(";", "").replace(",", "").trim();
    }

    private static final java.util.regex.Pattern DATA_ISO = java.util.regex.Pattern.compile(
            "(\\d{4})-(\\d{1,2})-(\\d{1,2})(?:[ T]+(\\d{1,2})[:.](\\d{2})(?::\\d{2})?)?");
    private static final java.util.regex.Pattern DATA_ITALIANA = java.util.regex.Pattern.compile(
            "(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})(?: +(\\d{1,2})[:.](\\d{2})(?::\\d{2})?)?");

    /**
     * L'istante di una data scritta dall'utente nei dialoghi: {@code aaaa-mm-gg hh:mm} (anche con i secondi o senza
     * l'ora, che vale mezzanotte) oppure {@code gg/mm/aaaa} con o senza ora. Una data inesistente ({@code 2024-02-30})
     * non diventa un'altra data.
     * @return i millisecondi al minuto, 0 se il testo non è una data valida
     */
    static long IstanteDigitato(String Testo) {
        if (Testo == null) return 0;
        String s = Testo.trim();
        int Anno, Mese, Giorno;
        String Ora, Minuti;
        java.util.regex.Matcher m = DATA_ISO.matcher(s);
        if (m.matches()) {
            Anno = Integer.parseInt(m.group(1));
            Mese = Integer.parseInt(m.group(2));
            Giorno = Integer.parseInt(m.group(3));
        } else {
            m = DATA_ITALIANA.matcher(s);
            if (!m.matches()) return 0;
            Giorno = Integer.parseInt(m.group(1));
            Mese = Integer.parseInt(m.group(2));
            Anno = Integer.parseInt(m.group(3));
        }
        Ora = m.group(4) == null ? "0" : m.group(4);
        Minuti = m.group(5) == null ? "0" : m.group(5);
        try {
            java.time.LocalDateTime d = java.time.LocalDateTime.of(Anno, Mese, Giorno, Integer.parseInt(Ora), Integer.parseInt(Minuti));
            return FunzioniDate.ConvertiDatainLongMinuto(d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } catch (java.time.DateTimeException ex) {
            return 0;
        }
    }

    /** L'istante della domanda: mezzanotte del giorno indicato ({@code yyyy-MM-dd}). */
    static long IstanteDomanda(String DataDomanda) {
        return FunzioniDate.ConvertiDatainLong(DataDomanda);
    }

    /**
     * Le giacenze cripto positive della piattaforma alla data della domanda, per sotto-wallet e moneta, con la stessa
     * somma della scheda Giacenze a data (movimenti dei giorni precedenti). Esclusi FIAT, SCAM e le giacenze nulle o
     * negative. Il valore non è ancora calcolato ({@link #Valorizza}).
     */
    public static List<Posizione> GiacenzeAllaData(String Exchange, String DataDomanda) {
        List<Posizione> Ris = new ArrayList<>();
        long Istante = IstanteDomanda(DataDomanda);
        TreeSet<String> SottoWallet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] != null && v[3].trim().equalsIgnoreCase(Exchange) && v[4] != null) SottoWallet.add(v[4].trim());
        }
        for (String sw : SottoWallet) {
            Map<String, Moneta> Qta = Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(), Istante, Exchange, sw);
            for (Moneta m : Qta.values()) {
                //I token credito di una procedura precedente sulla stessa piattaforma non si bloccano di nuovo
                if (m.Tipo == null || m.Tipo.equalsIgnoreCase("FIAT") || Funzioni.isSCAM(m.Moneta)
                        || FallimentiPiattaforme.isTokenCredito(m.Moneta)) continue;
                BigDecimal q;
                try {
                    q = new BigDecimal(m.Qta);
                } catch (NumberFormatException | NullPointerException ex) {
                    continue;
                }
                if (q.signum() <= 0) continue;
                Posizione p = new Posizione();
                p.SottoWallet = sw;
                p.Moneta = m.Moneta;
                p.Tipo = m.Tipo;
                p.Address = m.MonetaAddress == null ? "" : m.MonetaAddress;
                p.Rete = m.Rete == null ? "" : m.Rete;
                p.Qta = q;
                Ris.add(p);
            }
        }
        return Ris;
    }

    /**
     * Le giacenze da bloccare che il dialogo propone: le cripto ({@link #GiacenzeAllaData}) e poi i FIAT
     * ({@link #GiacenzeFiatPerSottoWallet}), che diventano crediti in valuta. Il valore non è ancora calcolato per le
     * cripto e per le valute diverse dall'euro ({@link #Valorizza}).
     */
    public static List<Posizione> PosizioniAllaData(String Exchange, String DataDomanda) {
        List<Posizione> Ris = GiacenzeAllaData(Exchange, DataDomanda);
        Ris.addAll(GiacenzeFiatPerSottoWallet(Exchange, DataDomanda));
        return Ris;
    }

    /**
     * I FIAT da bloccare: passano nel sotto-wallet {@value #SOTTOWALLET} e da lì al loro credito in valuta
     * ({@code CELSIUS.CREDITO.EUR}, tipo FIAT). Per ogni valuta si blocca il <b>saldo netto</b> della piattaforma
     * ({@link #GiacenzeFiatAllaData}), prelevato dai sotto-wallet positivi a partire dal più grande: un sotto-wallet
     * negativo (scambi importati senza il deposito) resta com'è e la piattaforma torna a zero alla chiusura. Il valore
     * è quello in euro solo per l'EUR.
     */
    public static List<Posizione> GiacenzeFiatPerSottoWallet(String Exchange, String DataDomanda) {
        List<Posizione> Ris = new ArrayList<>();
        long Istante = IstanteDomanda(DataDomanda);
        TreeSet<String> SottoWallet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] != null && v[3].trim().equalsIgnoreCase(Exchange) && v[4] != null) SottoWallet.add(v[4].trim());
        }
        Map<String, List<Posizione>> Positivi = new TreeMap<>();
        for (String sw : SottoWallet) {
            for (Moneta m : Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(), Istante, Exchange, sw).values()) {
                if (m.Tipo == null || !m.Tipo.equalsIgnoreCase("FIAT") || FallimentiPiattaforme.isTokenCredito(m.Moneta)) continue;
                BigDecimal q;
                try {
                    q = new BigDecimal(m.Qta);
                } catch (NumberFormatException | NullPointerException ex) {
                    continue;
                }
                if (q.signum() <= 0) continue;
                Posizione p = new Posizione();
                p.SottoWallet = sw;
                p.Moneta = m.Moneta;
                p.Tipo = "FIAT";
                p.Qta = q;
                Positivi.computeIfAbsent(m.Moneta, k -> new ArrayList<>()).add(p);
            }
        }
        Map<String, BigDecimal> Netto = GiacenzeFiatAllaData(Exchange, DataDomanda);
        for (Map.Entry<String, List<Posizione>> e : Positivi.entrySet()) {
            BigDecimal DaSpostare = Netto.getOrDefault(e.getKey(), BigDecimal.ZERO);
            e.getValue().sort((a, b) -> b.Qta.compareTo(a.Qta));
            for (Posizione p : e.getValue()) {
                if (DaSpostare.signum() <= 0) break;
                p.Qta = p.Qta.min(DaSpostare);
                DaSpostare = DaSpostare.subtract(p.Qta);
                p.ValoreEUR = p.Moneta.equalsIgnoreCase("EUR") ? p.Qta : null;
                Ris.add(p);
            }
        }
        return Ris;
    }

    /**
     * Le giacenze FIAT positive alla data, sommate su tutta la piattaforma. I crediti in valuta di una procedura
     * precedente sulla stessa piattaforma non contano: non si bloccano di nuovo.
     */
    public static Map<String, BigDecimal> GiacenzeFiatAllaData(String Exchange, String DataDomanda) {
        Map<String, BigDecimal> Ris = new TreeMap<>();
        Map<String, Moneta> Qta = Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(),
                IstanteDomanda(DataDomanda), Exchange, "tutti");
        for (Moneta m : Qta.values()) {
            if (m.Tipo == null || !m.Tipo.equalsIgnoreCase("FIAT") || FallimentiPiattaforme.isTokenCredito(m.Moneta)) continue;
            try {
                BigDecimal q = new BigDecimal(m.Qta);
                if (q.signum() > 0) Ris.merge(m.Moneta, q, BigDecimal::add);
            } catch (NumberFormatException | NullPointerException ex) {
                //quantità non numerica: non si mostra
            }
        }
        return Ris;
    }

    /**
     * Valore del credito di ogni posizione alla data della domanda: il prezzo della moneta a quell'istante (può
     * usare la rete). Lasciato {@code null} dove il prezzo non si trova: l'utente lo inserisce a mano. Le posizioni
     * che hanno già un valore (gli euro) non si toccano.
     */
    public static void Valorizza(List<Posizione> Posizioni, String DataDomanda) {
        long Istante = IstanteDomanda(DataDomanda);
        for (Posizione p : Posizioni) {
            if (p.ValoreEUR != null) continue;
            Moneta m = new Moneta();
            m.Moneta = p.Moneta;
            m.Tipo = p.Tipo;
            m.MonetaAddress = p.Address;
            m.Rete = p.Rete;
            m.Qta = p.Qta.toPlainString();
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(m, null, Istante, p.Rete, "");
            if (IP != null && IP.prezzoUnitario != null) {
                p.ValoreEUR = p.Qta.multiply(IP.prezzoUnitario).abs().setScale(2, RoundingMode.HALF_UP);
            }
        }
    }

    /** @return i movimenti della piattaforma dal giorno della domanda in poi, escluse le righe di procedure */
    public static List<String[]> MovimentiSuccessivi(String Exchange, String DataDomanda) {
        List<String[]> Ris = new ArrayList<>();
        long Istante = IstanteDomanda(DataDomanda);
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] == null || !v[3].trim().equalsIgnoreCase(Exchange)) continue;
            if (GruppoOperazione.Chiave(v).startsWith(FallimentiPiattaforme.PREFISSO_CHIAVE)) continue;
            if (FunzioniDate.ConvertiDatainLong(v[1]) >= Istante) Ris.add(v);
        }
        return Ris;
    }

    /**
     * Il nome del token credito di ogni moneta: {@code <NOME>.CREDITO.<SIMBOLO>}, con un suffisso {@code -2},
     * {@code -3}... se due monete diverse (reti o address diversi) hanno lo stesso simbolo.
     * @return chiave moneta → nome del token credito, nell'ordine delle posizioni
     */
    public static Map<String, String> NomiToken(String NomeBreve, List<Posizione> Posizioni) {
        Map<String, String> Ris = new LinkedHashMap<>();
        Map<String, Integer> PerSimbolo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Posizione p : Posizioni) {
            if (Ris.containsKey(p.ChiaveMoneta())) continue;
            int n = PerSimbolo.merge(p.Moneta, 1, Integer::sum);
            Ris.put(p.ChiaveMoneta(), NomeToken(NomeBreve, p.Moneta) + (n > 1 ? "-" + n : ""));
        }
        return Ris;
    }

    /**
     * Le righe del blocco: per ogni posizione un giroconto interno (uscita dal sotto-wallet, entrata in
     * {@value #SOTTOWALLET}) se non è già lì, poi una riga {@code FBL} per moneta. Non tocca la mappa.
     * @param Posizioni le posizioni incluse, con il valore del credito
     * @return le righe in ordine, con ID già univoci rispetto alla mappa e fra loro
     */
    public static List<String[]> RigheBlocco(FallimentiPiattaforme.Procedura P, List<Posizione> Posizioni) {
        List<String[]> Righe = new ArrayList<>();
        Map<String, String[]> IDUsati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        long Istante = IstanteDomanda(P.DataDomanda);
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + P.Id;
        String Identificativo = "Fallimento " + P.NomeBreve;
        Map<String, String> Nomi = NomiToken(P.NomeBreve, Posizioni);

        //1 - Giroconti verso il sotto-wallet del credito, alla mezzanotte della domanda (anche i FIAT, che fra le
        //posizioni arrivano da GiacenzeFiatPerSottoWallet)
        int n = 0;
        for (Posizione p : Posizioni) {
            if (p.SottoWallet.equalsIgnoreCase(SOTTOWALLET)) continue;
            String Valore = Testo(p.ValoreEUR);
            n++;
            String[] Uscita = MovimentiCrypto.creaMovimento(Gamba(p, p.Qta.negate()), null, P.Exchange, p.SottoWallet,
                    Istante, Valore, "Credito alla data della domanda", n, 1, null,
                    "Fallimento " + P.NomeBreve + ": giacenza spostata nel credito", "M", "", "TRASFERIMENTO INTERNO", Identificativo);
            String[] Entrata = MovimentiCrypto.creaMovimento(null, Gamba(p, p.Qta), P.Exchange, SOTTOWALLET,
                    Istante, Valore, "Credito alla data della domanda", n, 2, null,
                    "Fallimento " + P.NomeBreve + ": giacenza spostata nel credito", "M", "", "TRASFERIMENTO INTERNO", Identificativo);
            for (String[] r : new String[][]{Uscita, Entrata}) {
                if (r == null) throw new IllegalStateException("Giroconto non creato per " + p.Moneta);
                Completa(r, Chiave, Valore, IDUsati);
                Righe.add(r);
            }
        }

        //2 - Una riga di blocco per moneta, un secondo dopo. Una valuta passa a un credito in valuta, che resta FIAT:
        //uno scambio FIAT → FIAT (SF), senza effetti fiscali, e i rimborsi mostrano lo scambio dal credito
        Map<String, List<Posizione>> PerMoneta = new LinkedHashMap<>();
        for (Posizione p : Posizioni) PerMoneta.computeIfAbsent(p.ChiaveMoneta(), k -> new ArrayList<>()).add(p);
        int b = 0;
        for (Map.Entry<String, List<Posizione>> e : PerMoneta.entrySet()) {
            Posizione Prima = e.getValue().get(0);
            BigDecimal Qta = BigDecimal.ZERO;
            BigDecimal Valore = BigDecimal.ZERO;
            for (Posizione p : e.getValue()) {
                Qta = Qta.add(p.Qta);
                if (p.ValoreEUR != null) Valore = Valore.add(p.ValoreEUR);
            }
            //0,2 + 0,8 darebbe "1.0"
            Qta = Qta.stripTrailingZeros();
            boolean Fiat = "FIAT".equalsIgnoreCase(Prima.Tipo);
            Moneta Credito = new Moneta();
            Credito.Moneta = Nomi.get(e.getKey());
            Credito.Tipo = Fiat ? "FIAT" : "Crypto";
            Credito.MonetaAddress = "";
            Credito.Rete = Prima.Rete;
            Credito.Qta = Qta.toPlainString();
            String ValoreTesto = Valore.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(Gamba(Prima, Qta.negate()), Credito, P.Exchange, SOTTOWALLET,
                    Istante + 1000, ValoreTesto, "Credito alla data della domanda", ++b, 1, null,
                    "Fallimento " + P.NomeBreve + ": credito alla data della domanda", "M", "", null, Identificativo);
            if (r == null) throw new IllegalStateException("Blocco non creato per " + Prima.Moneta);
            r[0] = SostituisciCategoria(r[0], Fiat ? "SF" : "SC");
            r[5] = "BLOCCO PER FALLIMENTO";
            r[18] = FallimentiPiattaforme.CAMPO18_BLOCCO;
            Completa(r, Chiave, ValoreTesto, IDUsati);
            Righe.add(r);
        }
        return Righe;
    }

    /** La gamba di una posizione con la quantità indicata (negativa in uscita). */
    private static Moneta Gamba(Posizione p, BigDecimal Qta) {
        Moneta m = new Moneta();
        m.Moneta = p.Moneta;
        m.Tipo = p.Tipo;
        m.MonetaAddress = p.Address;
        m.Rete = p.Rete;
        m.Qta = Qta.toPlainString();
        return m;
    }

    private static String Testo(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Sostituisce la categoria in coda all'ID ({@code SC} per il blocco di una cripto, {@code SF} di una valuta). */
    private static String SostituisciCategoria(String ID, String Categoria) {
        int i = ID.lastIndexOf('_');
        return i < 0 ? ID : ID.substring(0, i + 1) + Categoria;
    }

    /** Campi comuni alle righe della procedura e ID univoco rispetto alla mappa e alle righe già preparate. */
    private static void Completa(String[] r, String Chiave, String Valore, Map<String, String[]> IDUsati) {
        r[14] = Valore + " EUR";
        r[22] = "M";
        r[39] = "E";
        r[GruppoOperazione.CAMPO] = Chiave;
        String ID = r[0];
        while (IDUsati.containsKey(ID) || MappaCryptoWallet.containsKey(ID)) {
            String Nuovo = MovimentiCrypto.getIDUnivoco(IDUsati.containsKey(ID) ? IDUsati : MappaCryptoWallet, ID);
            if (Nuovo == null) throw new IllegalStateException("Impossibile rendere univoco l'ID " + r[0]);
            ID = Nuovo;
        }
        r[0] = ID;
        IDUsati.put(ID, r);
    }

    /**
     * Crea la procedura: scrive la tabella e poi le righe del blocco nella mappa dei movimenti. Se le righe non si
     * possono costruire non scrive nulla. Il ricalcolo delle tabelle resta al chiamante.
     * @return {@code null} se tutto è andato bene, altrimenti il motivo
     */
    public static String CreaProcedura(FallimentiPiattaforme.Procedura P, List<Posizione> Posizioni) {
        if (P.Id.isBlank()) return "Manca il nome breve della piattaforma.";
        //Una procedura registrata ma senza movimenti (creata e mai salvata) si sovrascrive. Contano i movimenti, non la
        //tabella: righe rimaste senza i dati del piano (ripristino parziale) darebbero un secondo blocco sommato al primo
        if (!MovimentiProcedura(P.Id).isEmpty())
            return "Esiste già una procedura " + P.Id + ": scegli un altro nome breve.";
        if (Posizioni.isEmpty()) return "Nessuna giacenza da bloccare.";
        for (Posizione p : Posizioni) {
            if (p.ValoreEUR == null) return "Manca il valore del credito di " + p.Moneta + " (" + p.SottoWallet + ").";
        }
        List<String[]> Righe;
        try {
            Righe = RigheBlocco(P, Posizioni);
        } catch (IllegalStateException ex) {
            LoggerGC.ScriviErrore(ex);
            return ex.getMessage();
        }
        if (!FallimentiPiattaforme.ScriviProcedura(P)) return "Impossibile salvare la procedura nel database.";
        for (String[] r : Righe) MappaCryptoWallet.put(r[0], r);
        FallimentiPiattaforme.Invalida();
        return null;
    }

    // ------------------------------------------------------------------
    // Le tranche di rimborso (fase 3)
    // ------------------------------------------------------------------

    /** Una moneta ricevuta in una tranche. */
    public static final class Ricevuta {
        public String Moneta = "";
        public String Tipo = "Crypto";
        public BigDecimal Qta = BigDecimal.ZERO;
        /** Valore in euro alla data del rimborso: il corrispettivo. */
        public BigDecimal ValoreEUR = BigDecimal.ZERO;
        /** Deposito già importato su cui è arrivata, {@code null} se ricevuta fuori dal programma (solo FIAT). */
        public String IDDeposito;
        /** Restituzione della stessa moneta trattata come giroconto ({@code FGC}) invece che come rimborso. */
        public boolean InNatura;

        boolean isFiat() {
            return "FIAT".equalsIgnoreCase(Tipo);
        }
    }

    /** Una riga dell'anteprima: cosa esce dal credito, cosa entra, costo scaricato e risultato atteso. */
    public static final class RigaAnteprima {
        public String Credito;
        public BigDecimal QtaUscita;
        public String Ricevuta;
        public BigDecimal QtaEntrata;
        public BigDecimal Valore;
        /** Costo di carico che la riga scarica, {@code null} per i FIAT o se il motore non l'ha ancora calcolato. */
        public BigDecimal Costo;
        public String Trattamento;
    }

    /** Il risultato della preparazione di una tranche: le righe da scrivere, gli abbinamenti, l'anteprima. */
    public static final class Tranche {
        public final List<String[]> Righe = new ArrayList<>();
        /** Coppie {prelievo dalla piattaforma, deposito importato} da abbinare come trasferimento tra wallet. */
        public final List<String[]> Abbinamenti = new ArrayList<>();
        public final List<RigaAnteprima> Anteprima = new ArrayList<>();
        /** Frazione della quantità originaria di ogni token credito consumata da questa tranche. */
        public BigDecimal Frazione = BigDecimal.ZERO;
        public String Errore;
        /** Cosa succede oltre alle righe (credito esaurito, chiusura da spostare), {@code null} se nulla. */
        public String Avviso;
    }

    private static final int SCALA = 18;

    /** @return i token credito della procedura, ricostruendo il registro sui movimenti attuali */
    static List<FallimentiPiattaforme.TokenCredito> TokenDellaProcedura(String Id) {
        FallimentiPiattaforme.Ricostruisci();
        List<FallimentiPiattaforme.TokenCredito> Ris = new ArrayList<>();
        for (FallimentiPiattaforme.TokenCredito t : FallimentiPiattaforme.Registro().Token.values()) {
            if (Id.equals(t.IdProcedura)) Ris.add(t);
        }
        Ris.sort((a, b) -> a.Simbolo.compareToIgnoreCase(b.Simbolo));
        return Ris;
    }

    /**
     * I depositi già importati che possono essere l'arrivo di un rimborso: depositi cripto o FIAT su altre
     * piattaforme, dalla data della domanda in poi, non classificati e non collegati ad altri movimenti.
     */
    public static List<String[]> DepositiCandidati(FallimentiPiattaforme.Procedura P) {
        List<String[]> Ris = new ArrayList<>();
        long Dal = IstanteDomanda(P.DataDomanda);
        for (String[] v : MappaCryptoWallet.values()) {
            String Cat = v[0].substring(v[0].lastIndexOf('_') + 1);
            if (!Cat.equals("DC") && !Cat.equals("DF")) continue;
            if (v[3] != null && v[3].trim().equalsIgnoreCase(P.Exchange)) continue;
            if (!v[18].isBlank() || !v[20].isBlank() || "AU".equalsIgnoreCase(v[22])) continue;
            //Le righe di un'altra procedura (per esempio il deposito FIAT di una tranche) non sono arrivi
            if (GruppoOperazione.Chiave(v).startsWith(FallimentiPiattaforme.PREFISSO_CHIAVE)) continue;
            if (FunzioniDate.ConvertiDatainLong(v[1]) < Dal) continue;
            Ris.add(v);
        }
        return Ris;
    }

    /** @return la quantità ripartita: le quote di tutte le parti tranne l'ultima, che prende il resto */
    private static List<BigDecimal> Ripartisci(BigDecimal Totale, List<BigDecimal> Pesi, int Scala) {
        List<BigDecimal> Ris = new ArrayList<>();
        BigDecimal Somma = BigDecimal.ZERO;
        for (int i = 0; i < Pesi.size(); i++) {
            BigDecimal q = i == Pesi.size() - 1 ? Totale.subtract(Somma)
                    : Totale.multiply(Pesi.get(i)).setScale(Scala, RoundingMode.HALF_UP);
            Somma = Somma.add(q);
            Ris.add(q.stripTrailingZeros());
        }
        return Ris;
    }

    /** @return la rete delle monete ricevute: quella del primo token cripto, vuota se il credito è solo in valuta */
    private static String ReteRicevute(List<FallimentiPiattaforme.TokenCredito> Token) {
        for (FallimentiPiattaforme.TokenCredito t : Token) {
            if (!t.Fiat) return t.ReteOriginale;
        }
        return "";
    }

    /** @return {@code true} se almeno un token credito ha ancora una quantità all'istante */
    private static boolean CreditoResiduo(List<FallimentiPiattaforme.TokenCredito> Token, long Istante) {
        for (FallimentiPiattaforme.TokenCredito t : Token) {
            if (t.QtaAl(Istante).signum() > 0) return true;
        }
        return false;
    }

    /** Quanto consuma una tranche: per ogni token, nell'ordine, la quantità che esce e la quota del valore ricevuto. */
    static final class Consumo {
        final List<BigDecimal> Qta = new ArrayList<>();
        final List<BigDecimal> Pesi = new ArrayList<>();
        /** Frazione della quantità originaria dei token cripto consumata (tutti la stessa). */
        BigDecimal FrazioneCripto = BigDecimal.ZERO;
    }

    /**
     * Quanto consuma ogni token credito e quale quota del valore ricevuto gli tocca, con il metodo A' su tutto il
     * credito, valuta compresa: la tranche consuma {@code Percentuale ÷ Atteso} del credito alla data della domanda
     * ({@code [15]} del blocco), ripartito sui token in proporzione al credito che resta a ognuno, e il valore va in
     * proporzione al credito alla domanda. Se supera il residuo, la tranche consuma tutto. I token hanno sempre la
     * stessa frazione residua, ma il calcolo regge anche residui diversi (consumo e valore in proporzione al credito
     * che resta). "Consuma prima il credito FIAT", provato il 29/10, è stato tolto su decisione dell'utente: troppe
     * incongruenze, la differenza della parte in valuta si mostra a parte ({@link #RisultatiValuta}).
     * @return {@code null} se il valore del credito alla domanda è zero
     */
    static Consumo Consumi(List<FallimentiPiattaforme.TokenCredito> Token, long Istante, BigDecimal Percentuale,
            BigDecimal Atteso) {
        BigDecimal Totale = BigDecimal.ZERO, ValoreCripto = BigDecimal.ZERO;
        BigDecimal FiatResidua = BigDecimal.ZERO;
        BigDecimal ResCripto = null;
        List<BigDecimal> Residui = new ArrayList<>();
        List<BigDecimal> Frazioni = new ArrayList<>();
        for (FallimentiPiattaforme.TokenCredito t : Token) {
            BigDecimal Res = t.QtaAl(Istante).max(BigDecimal.ZERO);
            BigDecimal Fr = t.Qta.signum() == 0 ? BigDecimal.ZERO : Res.divide(t.Qta, SCALA, RoundingMode.HALF_UP);
            Residui.add(Res);
            Frazioni.add(Fr);
            Totale = Totale.add(t.ValoreEUR);
            if (t.Fiat) {
                FiatResidua = FiatResidua.add(t.ValoreEUR.multiply(Fr));
            } else {
                ValoreCripto = ValoreCripto.add(t.ValoreEUR);
                if (ResCripto == null) ResCripto = Fr;
            }
        }
        if (Totale.signum() <= 0) return null;
        if (ResCripto == null) ResCripto = BigDecimal.ZERO;
        Consumo C = new Consumo();

        //Con la stessa frazione residua per tutti i token quantità e pesi sono quelli del metodo A' di sempre
        BigDecimal c = Percentuale.divide(Atteso, SCALA, RoundingMode.HALF_UP);
        BigDecimal ResiduoTotale = ValoreCripto.multiply(ResCripto).add(FiatResidua).divide(Totale, SCALA, RoundingMode.HALF_UP);
        boolean Uniforme = true;
        for (BigDecimal Fr : Frazioni) {
            if (Fr.compareTo(Frazioni.get(0)) != 0) Uniforme = false;
        }
        if (Uniforme) ResiduoTotale = Frazioni.get(0);
        boolean Ultima = c.compareTo(ResiduoTotale) >= 0;
        BigDecimal Consumato = BigDecimal.ZERO;
        List<BigDecimal> Nominali = new ArrayList<>();
        for (int k = 0; k < Token.size(); k++) {
            FallimentiPiattaforme.TokenCredito t = Token.get(k);
            BigDecimal Q;
            if (Ultima) Q = Residui.get(k);
            else {
                //La frazione del token: c, corretta per quanto gli resta rispetto al resto del credito
                BigDecimal Fr = Frazioni.get(k).compareTo(ResiduoTotale) == 0 ? c
                        : c.multiply(Frazioni.get(k)).divide(ResiduoTotale, SCALA, RoundingMode.HALF_UP);
                Q = t.Qta.multiply(Fr).setScale(t.Fiat ? 2 : SCALA, RoundingMode.HALF_UP);
            }
            C.Qta.add(Q);
            BigDecimal Nominale = t.Qta.signum() == 0 ? BigDecimal.ZERO : t.ValoreEUR.multiply(Q).divide(t.Qta, SCALA, RoundingMode.HALF_UP);
            Nominali.add(Nominale);
            Consumato = Consumato.add(Nominale);
            if (!t.Fiat && C.FrazioneCripto.signum() == 0 && t.Qta.signum() > 0) C.FrazioneCripto = Q.divide(t.Qta, SCALA, RoundingMode.HALF_UP);
        }
        for (int k = 0; k < Token.size(); k++) {
            if (Uniforme) C.Pesi.add(Token.get(k).ValoreEUR.divide(Totale, SCALA, RoundingMode.HALF_UP));
            else C.Pesi.add(Consumato.signum() == 0 ? BigDecimal.ZERO : Nominali.get(k).divide(Consumato, SCALA, RoundingMode.HALF_UP));
        }
        return C;
    }

    /**
     * Prepara le righe di una tranche senza toccare la mappa. Le parti del credito sono i token credito, cripto e in
     * valuta: quanto consuma ognuna e quale quota del valore ricevuto le tocca lo decide {@link #Consumi}. Per ogni
     * coppia parte → moneta ricevuta una riga: da un token cripto {@code FRB} (o {@code FGC} per la stessa moneta
     * restituita in natura, o un deposito {@code FRT} se la parte riceve valore senza consumare credito), da un
     * credito in valuta un acquisto della moneta ricevuta (o uno scambio FIAT, senza effetti, se è FIAT). Le monete
     * ricevute arrivano sul sotto-wallet {@value #SOTTOWALLET} e da lì escono: verso il deposito importato (giroconto
     * da abbinare) o, per i FIAT ricevuti fuori dal programma, con un prelievo.
     * @param Data data e ora del rimborso, {@code yyyy-MM-dd HH:mm}
     * @param Percentuale percentuale del credito pagata dalla tranche
     * @param Atteso recupero atteso in percentuale
     */
    public static Tranche PreparaTranche(FallimentiPiattaforme.Procedura P, String Data, BigDecimal Percentuale,
            BigDecimal Atteso, List<Ricevuta> Ricevute) {
        Tranche T = new Tranche();
        long Istante = IstanteDigitato(Data);
        if (Istante <= 0) {
            T.Errore = "Scrivi la data del rimborso come aaaa-mm-gg hh:mm (per esempio 2024-01-15 10:00).";
            return T;
        }
        //Dopo il blocco, che sta alla mezzanotte della domanda e un secondo dopo
        if (Istante <= IstanteDomanda(P.DataDomanda) + 1000) {
            T.Errore = "La data del rimborso dev'essere successiva alla data della domanda (" + P.DataDomanda + ").";
            return T;
        }
        //Procedura chiusa: il rimborso si registra lo stesso e la chiusura si rifà dopo (RegistraTranche). Il credito
        //si legge com'era prima della chiusura, che lo azzera: un residuo rimasto serve a questo rimborso
        String Chiusa = FallimentiPiattaforme.DataChiusura(P.Id);
        long IstanteResiduo = Istante;
        if (Chiusa != null) {
            IstanteResiduo = Math.min(Istante, FunzioniDate.ConvertiDatainLongMinuto(Chiusa) - 1);
            T.Avviso = "La procedura è chiusa (" + Chiusa + "): la chiusura viene tolta e rifatta subito dopo questo "
                    + "rimborso, con quello che resta del credito.";
        }
        //Le tranche vanno in ordine: il residuo di una tranche precedente non vedrebbe quelle successive
        long Precedente = IstanteUltimaTranche(P.Id);
        if (Istante <= Precedente) {
            T.Errore = "Il rimborso dev'essere successivo all'ultimo già registrato ("
                    + FunzioniDate.ConvertiDatadaLongAlSecondo(Precedente).substring(0, 16) + ").";
            return T;
        }
        //A credito esaurito la percentuale non serve: il rimborso non scarica nulla ed è tutto plusvalenza
        if ((Percentuale == null || Percentuale.signum() <= 0 || Atteso == null || Atteso.signum() <= 0) && !CreditoEsaurito(P)) {
            T.Errore = "Percentuale pagata e recupero atteso devono essere maggiori di zero.";
            return T;
        }
        if (Ricevute.isEmpty()) {
            T.Errore = "Indica almeno una moneta ricevuta.";
            return T;
        }
        BigDecimal ValoreRicevuto = BigDecimal.ZERO;
        for (Ricevuta r : Ricevute) {
            if (r.Qta == null || r.Qta.signum() <= 0 || r.ValoreEUR == null || r.ValoreEUR.signum() < 0) {
                T.Errore = "Quantità e valore di " + r.Moneta + " non sono validi.";
                return T;
            }
            if (r.IDDeposito == null && !r.isFiat()) {
                T.Errore = r.Moneta + ": una moneta cripto ricevuta va abbinata al deposito importato su cui è arrivata.";
                return T;
            }
            if (r.IDDeposito != null) {
                String[] d = MappaCryptoWallet.get(r.IDDeposito);
                if (d == null) {
                    T.Errore = "Il deposito di " + r.Moneta + " non esiste più.";
                    return T;
                }
                if (new BigDecimal(d[13]).abs().compareTo(r.Qta) != 0 || !d[11].equalsIgnoreCase(r.Moneta)) {
                    T.Errore = "La moneta ricevuta deve essere quella del deposito abbinato, con la stessa quantità ("
                            + d[13] + " " + d[11] + ").";
                    return T;
                }
                if (FunzioniDate.ConvertiDataIDinLong(d[0].split("_")[0]) <= Istante + 1000) {
                    T.Errore = "Il rimborso dev'essere registrato prima del deposito di " + r.Moneta
                            + " (" + d[1] + "), che ne è l'arrivo.";
                    return T;
                }
            }
            ValoreRicevuto = ValoreRicevuto.add(r.ValoreEUR);
        }
        //Le quote si fanno sul valore: una moneta a valore zero accanto ad altre con valore non consumerebbe credito
        //e diventerebbe un deposito senza uscita. A valore zero tutte, invece, le quote sono in parti uguali
        if (ValoreRicevuto.signum() > 0) {
            for (Ricevuta r : Ricevute) {
                if (r.ValoreEUR.signum() == 0) {
                    T.Errore = "Manca il valore in euro di " + r.Moneta + ": serve per ripartire il credito fra le monete ricevute.";
                    return T;
                }
            }
        }
        List<FallimentiPiattaforme.TokenCredito> Token = TokenDellaProcedura(P.Id);
        if (Token.isEmpty()) {
            T.Errore = "La procedura non ha token credito: il blocco manca o è stato tolto.";
            return T;
        }
        String Rete = ReteRicevute(Token);
        if (!CreditoResiduo(Token, IstanteResiduo)) return OltreIlCredito(T, P, Istante, Percentuale, Ricevute, Rete);
        //Il controllo iniziale guarda il credito alla fine (CreditoEsaurito): qui conta quello alla data del rimborso
        if (Percentuale == null || Percentuale.signum() <= 0 || Atteso == null || Atteso.signum() <= 0) {
            T.Errore = "Percentuale pagata e recupero atteso devono essere maggiori di zero.";
            return T;
        }
        //In natura: con più parti (più monete, o un credito in valuta) il rimborso si ripartirebbe su tutte e
        //consumerebbe tutti i token, mentre è tornata una moneta sola. Limite noto: si esclude quella posizione al blocco
        boolean PiuParti = Token.size() > 1;
        for (Ricevuta r : Ricevute) {
            if (r.InNatura && PiuParti) {
                T.Errore = "La restituzione in natura si può registrare solo se il credito è di una moneta sola. "
                        + "Per una posizione restituita nella stessa moneta (per esempio un conto Custody) escludila "
                        + "dal blocco e classifica la sua restituzione come un normale trasferimento.";
                return T;
            }
        }
        Consumo C = Consumi(Token, IstanteResiduo, Percentuale, Atteso);
        if (C == null) {
            T.Errore = "Il valore del credito alla data della domanda è zero: non si può ripartire il rimborso.";
            return T;
        }
        T.Frazione = C.FrazioneCripto;
        if (Token.stream().anyMatch(t -> t.Fiat)) {
            String Valuta = "Il credito in valuta riceve la sua quota del valore: la differenza fra quanto riceve e il credito "
                    + "che consuma non è una plusvalenza o minusvalenza su cripto-attività e qui non viene calcolata "
                    + "(la mostra il pulsante Risultato parte FIAT).";
            T.Avviso = T.Avviso == null ? Valuta : T.Avviso + " " + Valuta;
        }
        List<BigDecimal> Pesi = C.Pesi;

        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + P.Id;
        String Identificativo = "Fallimento " + P.NomeBreve;
        Map<String, String[]> IDUsati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String Nota = "Fallimento " + P.NomeBreve + ": rimborso del " + Percentuale.stripTrailingZeros().toPlainString() + "% del credito";
        int n = 0;

        //Quote delle monete ricevute sul valore della tranche (a valore zero: in parti uguali)
        List<BigDecimal> Quote = new ArrayList<>();
        for (Ricevuta r : Ricevute) {
            Quote.add(ValoreRicevuto.signum() == 0 ? BigDecimal.ONE.divide(new BigDecimal(Ricevute.size()), SCALA, RoundingMode.HALF_UP)
                    : r.ValoreEUR.divide(ValoreRicevuto, SCALA, RoundingMode.HALF_UP));
        }

        //Quantità uscita per parte e moneta ricevuta; quantità e valore entrati per moneta ricevuta e parte
        List<List<BigDecimal>> UscitePerParte = new ArrayList<>();
        for (int p = 0; p < Token.size(); p++) {
            UscitePerParte.add(Ripartisci(C.Qta.get(p), Quote, Token.get(p).Fiat ? 2 : SCALA));
        }
        List<List<BigDecimal>> EntratePerMoneta = new ArrayList<>();
        List<List<BigDecimal>> ValoriPerMoneta = new ArrayList<>();
        for (Ricevuta r : Ricevute) {
            EntratePerMoneta.add(Ripartisci(r.Qta, Pesi, r.isFiat() ? 2 : SCALA));
            ValoriPerMoneta.add(Ripartisci(r.ValoreEUR, Pesi, 2));
        }

        for (int j = 0; j < Ricevute.size(); j++) {
            Ricevuta r = Ricevute.get(j);
            for (int p = 0; p < Token.size(); p++) {
                BigDecimal Out = UscitePerParte.get(p).get(j);
                BigDecimal In = EntratePerMoneta.get(j).get(p);
                BigDecimal Val = ValoriPerMoneta.get(j).get(p);
                if (Out.signum() == 0 && In.signum() == 0) continue;
                FallimentiPiattaforme.TokenCredito t = Token.get(p);
                Moneta Uscita = new Moneta();
                Moneta Entrata = new Moneta();
                Entrata.Moneta = r.Moneta;
                Entrata.Tipo = r.Tipo;
                Entrata.MonetaAddress = "";
                Entrata.Qta = In.toPlainString();
                Uscita.Moneta = t.Simbolo;
                Uscita.Tipo = t.Fiat ? "FIAT" : "Crypto";
                Uscita.MonetaAddress = "";
                Uscita.Rete = t.ReteOriginale;
                RigaAnteprima A = new RigaAnteprima();
                A.Credito = t.Fiat ? t.Simbolo + " (FIAT)" : t.Simbolo;
                String Campo18;
                String Campo5;
                if (t.Fiat) {
                    //Credito in valuta: acquisto della moneta ricevuta, o scambio FIAT senza effetti se è una valuta
                    Campo18 = "";
                    Campo5 = r.isFiat() ? FallimentiPiattaforme.CAMPO5_RIMBORSO_FIAT : FallimentiPiattaforme.CAMPO5_ACQUISTO_FIAT;
                    A.Trattamento = r.isFiat() ? "FIAT: nessun effetto" : "Acquisto: costo = valore ricevuto";
                } else if (Out.signum() == 0) {
                    //Il token riceve valore senza consumare credito (una quantità così piccola che l'arrotondamento la
                    //azzera): come un rimborso oltre il credito, tutto plusvalenza
                    Entrata.Rete = t.ReteOriginale;
                    Campo18 = FallimentiPiattaforme.CAMPO18_RIMBORSO_OLTRE_IL_CREDITO;
                    Campo5 = FallimentiPiattaforme.CAMPO5_RIMBORSO_OLTRE_IL_CREDITO;
                    A.Costo = BigDecimal.ZERO;
                    A.Trattamento = "Rimborso oltre il credito consumato: tutto plusvalenza";
                } else if (In.signum() == 0) {
                    //Il token consuma credito senza ricevere nulla di questa moneta: senza entrata la riga è un prelievo,
                    //e col PCO il motore la realizza al suo valore (di solito zero), minusvalenza pari al costo
                    Campo18 = FallimentiPiattaforme.CAMPO18_RIMBORSO_SENZA_VALORE;
                    Campo5 = "RIMBORSO DA FALLIMENTO";
                    if (t.Costo != null && t.Qta.signum() > 0) {
                        A.Costo = t.Costo.multiply(Out).divide(t.Qta, 2, RoundingMode.HALF_UP);
                    }
                    A.Trattamento = "Rimborso senza valore: minusvalenza pari al costo";
                } else {
                    Entrata.Rete = t.ReteOriginale;
                    boolean Natura = r.InNatura && r.Moneta.equalsIgnoreCase(t.MonetaOriginale);
                    Campo18 = Natura ? FallimentiPiattaforme.CAMPO18_RESTITUZIONE_IN_NATURA : FallimentiPiattaforme.CAMPO18_RIMBORSO;
                    Campo5 = Natura ? "RESTITUZIONE IN NATURA" : "RIMBORSO DA FALLIMENTO";
                    if (t.Costo != null && t.Qta.signum() > 0) {
                        A.Costo = t.Costo.multiply(Out).divide(t.Qta, 2, RoundingMode.HALF_UP);
                    }
                    A.Trattamento = Natura ? "Giroconto: costo trasferito" : "Rimborso: plusvalenza o minusvalenza";
                }
                Uscita.Qta = Out.negate().toPlainString();
                A.QtaUscita = Out;
                A.Ricevuta = r.Moneta;
                A.QtaEntrata = In;
                A.Valore = Val;
                T.Anteprima.add(A);

                String ValTesto = Val.setScale(2, RoundingMode.HALF_UP).toPlainString();
                String[] Riga = MovimentiCrypto.creaMovimento(Out.signum() == 0 ? null : Uscita, Entrata, P.Exchange,
                        SOTTOWALLET, Istante, ValTesto, "Rimborso da fallimento", ++n, 1, null, Nota, "M", "", null, Identificativo);
                if (Riga == null) {
                    T.Errore = "Riga del rimborso non creata per " + A.Credito + " verso " + r.Moneta + ".";
                    return T;
                }
                Riga[5] = Campo5;
                Riga[18] = Campo18;
                Completa(Riga, Chiave, ValTesto, IDUsati);
                T.Righe.add(Riga);
            }

            if (!UscitaRicevuta(T, P, r, Istante, Rete, Nota, ++n, IDUsati)) return T;
        }
        return T;
    }

    /**
     * Uscita della moneta ricevuta dalla piattaforma, un secondo dopo il rimborso: verso il deposito importato (da
     * abbinare come trasferimento tra wallet) o, per i FIAT ricevuti fuori dal programma, un prelievo.
     * @return {@code false} se la riga non si è potuta creare ({@code T.Errore} lo dice)
     */
    private static boolean UscitaRicevuta(Tranche T, FallimentiPiattaforme.Procedura P, Ricevuta r, long Istante, String Rete,
            String Nota, int n, Map<String, String[]> IDUsati) {
        Moneta Esce = new Moneta();
        Esce.Moneta = r.Moneta;
        Esce.Tipo = r.Tipo;
        Esce.MonetaAddress = "";
        Esce.Rete = Rete;
        Esce.Qta = r.Qta.negate().toPlainString();
        String ValTesto = r.ValoreEUR.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String[] Prelievo = MovimentiCrypto.creaMovimento(Esce, null, P.Exchange, SOTTOWALLET, Istante + 1000, ValTesto,
                "Rimborso da fallimento", n, 1, null,
                r.IDDeposito == null ? Nota + ", ricevuto fuori dal programma" : Nota, "M", "", null, "Fallimento " + P.NomeBreve);
        if (Prelievo == null) {
            T.Errore = "Uscita di " + r.Moneta + " dalla piattaforma non creata.";
            return false;
        }
        Completa(Prelievo, FallimentiPiattaforme.PREFISSO_CHIAVE + P.Id, ValTesto, IDUsati);
        T.Righe.add(Prelievo);
        if (r.IDDeposito != null) T.Abbinamenti.add(new String[]{Prelievo[0], r.IDDeposito});
        return true;
    }

    /**
     * Rimborso arrivato quando il credito è già esaurito: non c'è più un token credito da scambiare e il suo costo è
     * già stato usato dalle tranche precedenti. Ogni moneta ricevuta entra sul sotto-wallet {@value #SOTTOWALLET} con
     * un deposito {@code FRT}, il cui valore è tutto plusvalenza (e il costo di carico della moneta ricevuta), e da lì
     * esce come nelle altre tranche. Nessuna riga inventata: è quello che è successo.
     */
    private static Tranche OltreIlCredito(Tranche T, FallimentiPiattaforme.Procedura P, long Istante, BigDecimal Percentuale,
            List<Ricevuta> Ricevute, String Rete) {
        for (Ricevuta r : Ricevute) {
            if (r.InNatura) {
                T.Errore = "Il credito è esaurito: la restituzione in natura non si applica, togli la spunta In natura.";
                return T;
            }
        }
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + P.Id;
        String Nota = "Fallimento " + P.NomeBreve + ": rimborso "
                + (Percentuale == null || Percentuale.signum() <= 0 ? "" : "del " + Percentuale.stripTrailingZeros().toPlainString() + "% ")
                + "oltre il credito";
        Map<String, String[]> IDUsati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int n = 0;
        for (Ricevuta r : Ricevute) {
            Moneta Entrata = new Moneta();
            Entrata.Moneta = r.Moneta;
            Entrata.Tipo = r.Tipo;
            Entrata.MonetaAddress = "";
            Entrata.Rete = Rete;
            Entrata.Qta = r.Qta.toPlainString();
            String ValTesto = r.ValoreEUR.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String[] Riga = MovimentiCrypto.creaMovimento(null, Entrata, P.Exchange, SOTTOWALLET, Istante, ValTesto,
                    "Rimborso da fallimento", ++n, 1, null, Nota, "M", "", null, "Fallimento " + P.NomeBreve);
            if (Riga == null) {
                T.Errore = "Riga del rimborso non creata per " + r.Moneta + ".";
                return T;
            }
            Riga[5] = FallimentiPiattaforme.CAMPO5_RIMBORSO_OLTRE_IL_CREDITO;
            Riga[18] = FallimentiPiattaforme.CAMPO18_RIMBORSO_OLTRE_IL_CREDITO;
            Completa(Riga, Chiave, ValTesto, IDUsati);
            T.Righe.add(Riga);

            RigaAnteprima A = new RigaAnteprima();
            A.Credito = "Credito esaurito";
            A.QtaUscita = BigDecimal.ZERO;
            A.Ricevuta = r.Moneta;
            A.QtaEntrata = r.Qta;
            A.Valore = r.ValoreEUR;
            A.Costo = BigDecimal.ZERO;
            A.Trattamento = "Rimborso oltre il credito: tutto plusvalenza";
            T.Anteprima.add(A);

            if (!UscitaRicevuta(T, P, r, Istante, Rete, Nota, ++n, IDUsati)) return T;
        }
        String Esaurito = "Il credito è già esaurito: il costo di carico è stato usato dalle tranche precedenti, quindi "
                + "tutto il valore ricevuto è plusvalenza.";
        T.Avviso = T.Avviso == null ? Esaurito : Esaurito + " " + T.Avviso;
        return T;
    }

    /**
     * Registra una tranche: righe nella mappa, abbinamento ai depositi importati, tranche e recupero atteso nella
     * tabella. Il ricalcolo resta al chiamante.
     * @param USDPagati dollari pagati dalla tranche, {@code null} = percentuale × credito in dollari
     * @return {@code null} se è andato tutto bene, altrimenti il motivo
     */
    public static String RegistraTranche(FallimentiPiattaforme.Procedura P, String Data, BigDecimal Percentuale,
            BigDecimal USDPagati, BigDecimal Atteso, List<Ricevuta> Ricevute) {
        Tranche T = PreparaTranche(P, Data, Percentuale, Atteso, Ricevute);
        if (T.Errore != null) return T.Errore;
        //Procedura chiusa (decisione dell'utente del 2026-10-28): la chiusura si toglie e si rifà dopo il rimborso, così
        //un residuo rimasto serve a questo rimborso invece di uscire senza valore
        boolean EraChiusa = FallimentiPiattaforme.DataChiusura(P.Id) != null;
        long DataChiusura = EraChiusa ? FunzioniDate.ConvertiDatainLongMinuto(FallimentiPiattaforme.DataChiusura(P.Id)) : 0;
        if (EraChiusa) {
            Funzioni.RimuoviMovimenti(UltimaOperazione(P.Id).ID);
            FallimentiPiattaforme.Invalida();
        }
        for (String[] r : T.Righe) MappaCryptoWallet.put(r[0], r);
        FallimentiPiattaforme.Invalida();
        for (String[] a : T.Abbinamenti) GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(a[0], a[1]);
        FallimentiPiattaforme.Tranche t = new FallimentiPiattaforme.Tranche();
        //Nella forma del campo 1 dei movimenti: è così che la tranche si ritrova nella mappa (TrancheValide)
        t.Data = FunzioniDate.ConvertiDatadaLongAlSecondo(IstanteDigitato(Data)).substring(0, 16);
        //Senza percentuale (credito esaurito) la tranche resta nel piano a zero: il credito in dollari non cambia
        t.Percentuale = Percentuale == null ? BigDecimal.ZERO : Percentuale;
        t.USDPagati = USDPagati != null ? USDPagati : P.CreditoUSD == null ? BigDecimal.ZERO
                : P.CreditoUSD.multiply(t.Percentuale).divide(FallimentiPiattaforme.CENTO, 2, RoundingMode.HALF_UP);
        //Le tranche registrate e mai salvate non si riscrivono
        List<FallimentiPiattaforme.Tranche> Valide = FallimentiPiattaforme.TrancheValide(P);
        P.Tranche.retainAll(Valide);
        P.Tranche.add(t);
        if (Atteso != null && Atteso.signum() > 0) P.RecuperoAtteso = Atteso;
        if (!FallimentiPiattaforme.ScriviProcedura(P)) return "Rimborso registrato nei movimenti ma non nel database della procedura.";
        FallimentiPiattaforme.Invalida();
        if (EraChiusa) {
            //Alla data di prima, o un minuto dopo l'ultimo movimento se il rimborso è più recente. Se non resta nulla da
            //chiudere la procedura resta senza chiusura, con il credito esaurito
            long Nuova = Math.max(DataChiusura, IstanteUltimoMovimento(P.Id) + 60000);
            Chiusura C = PreparaChiusura(P, FunzioniDate.ConvertiDatadaLongAlSecondo(Nuova).substring(0, 16));
            if (!C.Righe.isEmpty()) {
                if (C.Errore != null) return "Rimborso registrato, ma la procedura non si è potuta richiudere: " + C.Errore;
                for (String[] r : C.Righe) MappaCryptoWallet.put(r[0], r);
                FallimentiPiattaforme.Invalida();
            }
        }
        return null;
    }

    /** @return gli ID dei movimenti della procedura (chiave {@code FALL-<id>}) */
    public static List<String> MovimentiProcedura(String Id) {
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + Id;
        List<String> Ris = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (Chiave.equals(GruppoOperazione.Chiave(v))) Ris.add(v[0]);
        }
        return Ris;
    }

    /**
     * Elimina una procedura: tutti i suoi movimenti (blocco, tranche, chiusura) e i dati del piano. Come ogni
     * cancellazione, diventa definitiva al salvataggio dei movimenti. Il ricalcolo resta al chiamante.
     * @return quanti movimenti sono stati tolti
     */
    public static int EliminaProcedura(String Id) {
        int Tolti = Funzioni.RimuoviMovimenti(MovimentiProcedura(Id));
        FallimentiPiattaforme.CancellaProcedura(Id);
        FallimentiPiattaforme.Invalida();
        return Tolti;
    }

    // ------------------------------------------------------------------
    // Chiusura e annullamento (fase 4)
    // ------------------------------------------------------------------

    /** @return l'istante (al minuto) dell'ultima tranche nei movimenti, 0 se non ce ne sono */
    static long IstanteUltimaTranche(String Id) {
        long Ultimo = 0;
        for (String d : FallimentiPiattaforme.Registro().DateTranche.getOrDefault(Id, java.util.Set.of())) {
            Ultimo = Math.max(Ultimo, FunzioniDate.ConvertiDatainLongMinuto(d));
        }
        return Ultimo;
    }

    /**
     * @return {@code true} se un nuovo rimborso troverebbe il credito già esaurito dalle tranche registrate, cioè
     *         sarebbe un rimborso oltre il credito. Una chiusura non conta: il rimborso la sposta e legge il credito
     *         com'era prima (come {@link #PreparaTranche}). {@code false} se la procedura non ha il blocco
     */
    public static boolean CreditoEsaurito(FallimentiPiattaforme.Procedura P) {
        List<FallimentiPiattaforme.TokenCredito> Token = TokenDellaProcedura(P.Id);
        if (Token.isEmpty()) return false;
        String Chiusa = FallimentiPiattaforme.DataChiusura(P.Id);
        long Istante = Chiusa == null ? Long.MAX_VALUE : FunzioniDate.ConvertiDatainLongMinuto(Chiusa) - 1;
        //Tutti i token, non il primo: il residuo si legge su ognuno
        return !CreditoResiduo(Token, Istante);
    }

    /** @return l'istante (al minuto) dell'ultimo movimento della procedura, 0 se non ce ne sono */
    static long IstanteUltimoMovimento(String Id) {
        long Ultimo = 0;
        for (String ID : MovimentiProcedura(Id)) {
            Ultimo = Math.max(Ultimo, FunzioniDate.ConvertiDatainLongMinuto(MappaCryptoWallet.get(ID)[1]));
        }
        return Ultimo;
    }

    /** Il risultato della preparazione della chiusura: le righe da scrivere e la loro descrizione per la conferma. */
    public static final class Chiusura {
        public final List<String[]> Righe = new ArrayList<>();
        public final List<String> Descrizione = new ArrayList<>();
        public String Errore;
    }

    /**
     * Prepara la chiusura senza toccare la mappa: il residuo di ogni token credito esce con un {@code PWN} (nessuna
     * minusvalenza, decisione §7.3), i FIAT rimasti nel sotto-wallet {@value #SOTTOWALLET} con un prelievo. Tutto a
     * valore zero, anche in {@code [14]}, così nessun ricalcolo dei prezzi lo riprezza.
     * @param Data data e ora della chiusura, {@code yyyy-MM-dd HH:mm}
     */
    public static Chiusura PreparaChiusura(FallimentiPiattaforme.Procedura P, String Data) {
        Chiusura C = new Chiusura();
        long Istante = IstanteDigitato(Data);
        if (Istante <= 0) {
            C.Errore = "Scrivi la data della chiusura come aaaa-mm-gg hh:mm.";
            return C;
        }
        String Chiusa = FallimentiPiattaforme.DataChiusura(P.Id);
        if (Chiusa != null) {
            C.Errore = "La procedura è già chiusa (" + Chiusa + ").";
            return C;
        }
        long Ultimo = IstanteUltimoMovimento(P.Id);
        if (Istante <= Ultimo) {
            C.Errore = "La chiusura dev'essere successiva all'ultimo movimento della procedura ("
                    + FunzioniDate.ConvertiDatadaLongAlSecondo(Ultimo).substring(0, 16) + ").";
            return C;
        }
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + P.Id;
        String Identificativo = "Fallimento " + P.NomeBreve;
        String Nota = "Fallimento " + P.NomeBreve + ": chiusura della procedura, il residuo esce senza valore";
        Map<String, String[]> IDUsati = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int n = 0;

        for (FallimentiPiattaforme.TokenCredito t : TokenDellaProcedura(P.Id)) {
            BigDecimal Residuo = t.QtaAl(Istante);
            if (Residuo.signum() <= 0) continue;
            Moneta m = new Moneta();
            m.Moneta = t.Simbolo;
            m.Tipo = t.Fiat ? "FIAT" : "Crypto";
            m.MonetaAddress = "";
            m.Rete = t.ReteOriginale;
            m.Qta = Residuo.stripTrailingZeros().negate().toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(m, null, P.Exchange, SOTTOWALLET, Istante, "0.00",
                    "Chiusura procedura", ++n, 1, null, Nota, "M", "", null, Identificativo);
            if (r == null) {
                C.Errore = "Riga di chiusura non creata per " + t.Simbolo + ".";
                return C;
            }
            r[5] = FallimentiPiattaforme.CAMPO5_CHIUSURA;
            //Il credito in valuta esce con un prelievo FIAT senza marcatore, come i FIAT rimasti: nessun effetto
            if (!t.Fiat) {
                r[0] = SostituisciCategoria(r[0], "PC");
                r[18] = FallimentiPiattaforme.CAMPO18_CHIUSURA;
            }
            Completa(r, Chiave, "0.00", IDUsati);
            C.Righe.add(r);
            String Costo = "";
            if (t.Fiat) Costo = " (FIAT)";
            else if (t.Costo != null && t.Qta.signum() > 0) {
                Costo = " (costo residuo " + t.Costo.multiply(Residuo).divide(t.Qta, 2, RoundingMode.HALF_UP).toPlainString()
                        + " EUR non dedotto)";
            }
            C.Descrizione.add(Residuo.stripTrailingZeros().toPlainString() + " " + t.Simbolo + Costo);
        }

        //I FIAT rimasti nel sotto-wallet del credito (i crediti in valuta escono sopra). Solo le righe di questa
        //procedura, perché una seconda procedura sulla stessa piattaforma usa lo stesso sotto-wallet
        Map<String, BigDecimal> Fiat = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] == null || !v[3].trim().equalsIgnoreCase(P.Exchange) || v[4] == null
                    || !v[4].trim().equalsIgnoreCase(SOTTOWALLET) || !Chiave.equals(GruppoOperazione.Chiave(v))) continue;
            if (FunzioniDate.ConvertiDatainLongMinuto(v[1]) > Istante) continue;
            if ("FIAT".equalsIgnoreCase(v[9]) && !v[8].isBlank() && !FallimentiPiattaforme.isTokenCredito(v[8]))
                Fiat.merge(v[8], Qta(v[10]), BigDecimal::add);
            if ("FIAT".equalsIgnoreCase(v[12]) && !v[11].isBlank() && !FallimentiPiattaforme.isTokenCredito(v[11]))
                Fiat.merge(v[11], Qta(v[13]), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : Fiat.entrySet()) {
            if (e.getValue().signum() <= 0) continue;
            Moneta m = new Moneta();
            m.Moneta = e.getKey();
            m.Tipo = "FIAT";
            m.MonetaAddress = "";
            m.Qta = e.getValue().stripTrailingZeros().negate().toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(m, null, P.Exchange, SOTTOWALLET, Istante, "0.00",
                    "Chiusura procedura", ++n, 1, null, Nota, "M", "", null, Identificativo);
            if (r == null) {
                C.Errore = "Riga di chiusura non creata per " + e.getKey() + ".";
                return C;
            }
            r[5] = FallimentiPiattaforme.CAMPO5_CHIUSURA;
            Completa(r, Chiave, "0.00", IDUsati);
            C.Righe.add(r);
            C.Descrizione.add(e.getValue().stripTrailingZeros().toPlainString() + " " + e.getKey() + " (FIAT)");
        }
        if (C.Righe.isEmpty()) {
            C.Errore = "Il credito è già stato tutto rimborsato: non resta nulla da chiudere.";
        }
        return C;
    }

    /** @return la quantità del campo, zero se vuota o non numerica */
    private static BigDecimal Qta(String s) {
        try {
            return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Chiude la procedura: scrive le righe di {@link #PreparaChiusura}. Lo stato "chiusa" si ricava dai movimenti,
     * non dalla tabella: togliendo le righe (annullamento, o chiusura mai salvata) la procedura torna aperta.
     * @return {@code null} se è andato tutto bene, altrimenti il motivo
     */
    public static String ChiudiProcedura(FallimentiPiattaforme.Procedura P, String Data) {
        Chiusura C = PreparaChiusura(P, Data);
        if (C.Errore != null) return C.Errore;
        for (String[] r : C.Righe) MappaCryptoWallet.put(r[0], r);
        FallimentiPiattaforme.Invalida();
        return null;
    }

    /** L'ultima operazione della procedura che si può annullare: la chiusura o l'ultima tranche. */
    public static final class Operazione {
        public String Descrizione;
        public final List<String> ID = new ArrayList<>();
        /** Data ({@code [1]}) della tranche da togliere anche dalla tabella, {@code null} per la chiusura. */
        String DataTranche;
    }

    /**
     * @return l'ultima operazione annullabile: la chiusura se c'è, altrimenti l'ultima tranche (le sue righe dalla
     *         data della tranche in poi, uscite verso i depositi comprese). {@code null} se resta solo il blocco,
     *         che si toglie con l'eliminazione della procedura.
     */
    public static Operazione UltimaOperazione(String Id) {
        Operazione O = new Operazione();
        String Chiusa = FallimentiPiattaforme.DataChiusura(Id);
        List<String> Righe = MovimentiProcedura(Id);
        if (Chiusa != null) {
            for (String ID : Righe) {
                if (FallimentiPiattaforme.CAMPO5_CHIUSURA.equals(MappaCryptoWallet.get(ID)[5])) O.ID.add(ID);
            }
            O.Descrizione = "la chiusura del " + Chiusa;
            return O;
        }
        long Ultima = IstanteUltimaTranche(Id);
        if (Ultima == 0) return null;
        for (String ID : Righe) {
            String[] v = MappaCryptoWallet.get(ID);
            if (FallimentiPiattaforme.Inizia(v[18], FallimentiPiattaforme.BLOCCO)) continue;
            if (FunzioniDate.ConvertiDatainLongMinuto(v[1]) >= Ultima) O.ID.add(ID);
        }
        O.DataTranche = FunzioniDate.ConvertiDatadaLongAlSecondo(Ultima).substring(0, 16);
        O.Descrizione = "il rimborso del " + O.DataTranche;
        return O;
    }

    /**
     * Annulla l'ultima operazione: ne toglie le righe con {@link Funzioni#RimuoviMovimenti} (i depositi abbinati
     * tornano da classificare) e, per una tranche, la toglie dalla tabella. Il ricalcolo resta al chiamante.
     * @return {@code null} se è andato tutto bene, altrimenti il motivo
     */
    public static String AnnullaUltimaOperazione(String Id) {
        Operazione O = UltimaOperazione(Id);
        if (O == null) return "Non ci sono rimborsi né chiusura da annullare: per togliere il blocco elimina la procedura.";
        Funzioni.RimuoviMovimenti(O.ID);
        FallimentiPiattaforme.Invalida();
        if (O.DataTranche != null) {
            FallimentiPiattaforme.Procedura P = FallimentiPiattaforme.LeggiProcedure().get(Id);
            if (P != null) {
                P.Tranche.removeIf(t -> t.Data.equals(O.DataTranche));
                if (!FallimentiPiattaforme.ScriviProcedura(P)) return "Movimenti tolti ma tranche rimasta nel database della procedura.";
            }
        }
        FallimentiPiattaforme.Invalida();
        return null;
    }

    /** @return lo stato da mostrare nell'elenco: chiusa, credito esaurito, o aperta con la frazione residua */
    public static String Stato(String Id) {
        int Oltre = FallimentiPiattaforme.RimborsiOltreIlCredito(Id);
        String Coda = Oltre == 0 ? "" : ", " + Oltre + (Oltre == 1 ? " rimborso" : " rimborsi") + " oltre il credito";
        String Chiusa = FallimentiPiattaforme.DataChiusura(Id);
        if (Chiusa != null) return "Chiusa il " + Chiusa + Coda;
        //Il residuo di tutto il credito, pesato col valore alla domanda: cripto e valuta possono finire in tempi diversi
        BigDecimal Valore = BigDecimal.ZERO, Resta = BigDecimal.ZERO;
        for (FallimentiPiattaforme.TokenCredito t : FallimentiPiattaforme.Registro().Token.values()) {
            if (!Id.equals(t.IdProcedura) || t.Qta.signum() <= 0) continue;
            Valore = Valore.add(t.ValoreEUR);
            Resta = Resta.add(t.ValoreEUR.multiply(t.QtaAl(Long.MAX_VALUE).max(BigDecimal.ZERO)).divide(t.Qta, SCALA, RoundingMode.HALF_UP));
        }
        if (Valore.signum() > 0) {
            BigDecimal Residua = Resta.multiply(FallimentiPiattaforme.CENTO).divide(Valore, 2, RoundingMode.HALF_UP);
            return Resta.signum() <= 0 ? "Credito esaurito" + Coda
                    : "Aperta, residuo " + Residua.stripTrailingZeros().toPlainString() + "%" + Coda;
        }
        return "Aperta" + Coda;
    }

    /** Il risultato della parte in valuta di un rimborso o della chiusura, che il programma non mette in nessun quadro. */
    public static final class RisultatoValuta {
        /** Data e ora, {@code yyyy-MM-dd HH:mm}. */
        public String Data = "";
        /** Rimborso o chiusura. */
        public String Tipo = "";
        /** Il credito in valuta, per esempio {@code CELSIUS.CREDITO.EUR}. */
        public String Credito = "";
        /** Credito consumato, in euro al valore della data della domanda. */
        public BigDecimal Consumato = BigDecimal.ZERO;
        /** Valore ricevuto per quel credito ({@code [15]} delle righe, zero alla chiusura). */
        public BigDecimal Ricevuto = BigDecimal.ZERO;

        public BigDecimal Differenza() {
            return Ricevuto.subtract(Consumato);
        }
    }

    /**
     * Le differenze della parte in valuta della procedura: per ogni rimborso (e la chiusura) e ogni credito in valuta,
     * il credito consumato al suo valore in euro alla domanda e il valore ricevuto. Il motore non le calcola (le righe
     * sono acquisti o scambi FIAT): servono a chi le vuole indicare per conto suo, fuori dai quadri delle cripto.
     * @return in ordine di data
     */
    public static List<RisultatoValuta> RisultatiValuta(String Id) {
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + Id;
        Map<String, RisultatoValuta> PerData = new TreeMap<>();
        for (FallimentiPiattaforme.TokenCredito t : TokenDellaProcedura(Id)) {
            if (!t.Fiat || t.Qta.signum() <= 0) continue;
            for (String[] v : MappaCryptoWallet.values()) {
                if (!t.Simbolo.equals(v[8]) || !Chiave.equals(GruppoOperazione.Chiave(v))) continue;
                BigDecimal Q = FallimentiPiattaforme.Numero(v[10]);
                if (Q == null || Q.signum() == 0) continue;
                RisultatoValuta R = PerData.computeIfAbsent(v[1] + ";" + t.Simbolo, k -> new RisultatoValuta());
                R.Data = v[1];
                R.Credito = t.Simbolo;
                R.Tipo = FallimentiPiattaforme.CAMPO5_CHIUSURA.equals(v[5]) ? "Chiusura" : "Rimborso";
                R.Consumato = R.Consumato.add(t.ValoreEUR.multiply(Q.abs()).divide(t.Qta, 2, RoundingMode.HALF_UP));
                BigDecimal Val = FallimentiPiattaforme.Numero(v[15]);
                if (Val != null && R.Tipo.equals("Rimborso")) R.Ricevuto = R.Ricevuto.add(Val.abs());
            }
        }
        return new ArrayList<>(PerData.values());
    }

    /** Un rimborso registrato di una procedura, o la sua chiusura, come lo raccontano i movimenti. */
    public static final class RimborsoRegistrato {
        /** Data e ora, {@code yyyy-MM-dd HH:mm}. */
        public String Data = "";
        /** Rimborso, restituzione in natura, rimborso oltre il credito o chiusura. */
        public String Tipo = "";
        /** Percentuale e dollari dal piano, {@code null} se il piano non ha la tranche (o è la chiusura). */
        public BigDecimal Percentuale;
        public BigDecimal USD;
        /** Le monete ricevute, per esempio {@code 0.27 BTC, 550 EUR}. */
        public String Ricevute = "";
        /** Dove sono arrivate: la piattaforma e il wallet del deposito abbinato, o fuori dal programma. */
        public String Arrivo = "";
        public BigDecimal ValoreEUR = BigDecimal.ZERO;
        /** Plusvalenze meno minusvalenze delle righe, come le ha lasciate l'ultimo ricalcolo. */
        public BigDecimal Risultato = BigDecimal.ZERO;
        /** Quanti movimenti della procedura compongono il rimborso (i depositi importati abbinati non contano). */
        public int Movimenti;
        /** Gli ID dei movimenti del rimborso più quelli dei depositi importati abbinati, in ordine di ID. */
        public List<String> ID = new ArrayList<>();
    }

    /**
     * I rimborsi della procedura che esistono nei movimenti, in ordine di data, più la chiusura se c'è. Le monete
     * ricevute sono le entrate sul sotto-wallet {@value #SOTTOWALLET} (sommate per moneta), l'arrivo è il deposito
     * abbinato all'uscita di quella moneta o, se non c'è, "fuori dal programma".
     */
    public static List<RimborsoRegistrato> RimborsiRegistrati(String Id) {
        String Chiave = FallimentiPiattaforme.PREFISSO_CHIAVE + Id;
        java.util.Set<String> Date = new java.util.HashSet<>(FallimentiPiattaforme.Registro().DateTranche.getOrDefault(Id, java.util.Set.of()));
        String Chiusa = FallimentiPiattaforme.DataChiusura(Id);
        if (Chiusa != null) Date.add(Chiusa);
        Map<String, List<String[]>> PerData = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (Date.contains(v[1]) && Chiave.equals(GruppoOperazione.Chiave(v))) {
                PerData.computeIfAbsent(v[1], k -> new ArrayList<>()).add(v);
            }
        }
        Map<String, FallimentiPiattaforme.Tranche> Piano = new TreeMap<>();
        FallimentiPiattaforme.Procedura P = FallimentiPiattaforme.LeggiProcedure().get(Id);
        if (P != null) for (FallimentiPiattaforme.Tranche t : P.Tranche) Piano.put(t.Data, t);

        List<RimborsoRegistrato> Ris = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> e : PerData.entrySet()) {
            RimborsoRegistrato R = new RimborsoRegistrato();
            R.Data = e.getKey();
            R.Movimenti = e.getValue().size();
            java.util.Set<String> Tutti = new java.util.TreeSet<>();
            Map<String, BigDecimal> Entrate = new LinkedHashMap<>();
            Map<String, String> Arrivi = new LinkedHashMap<>();
            boolean Natura = false, Oltre = false, ConCredito = false;
            for (String[] v : e.getValue()) {
                Tutti.add(v[0]);
                if (FallimentiPiattaforme.Inizia(v[18], FallimentiPiattaforme.RESTITUZIONE_IN_NATURA)) Natura = true;
                //Un FRT accanto a righe che consumano credito è la parte cripto che riceve valore senza consumare
                if (FallimentiPiattaforme.RimborsoOltreIlCredito(v[18])) Oltre = true;
                else if (FallimentiPiattaforme.isRigaDiRimborso(v)) ConCredito = true;
                if ("S".equals(v[33])) {
                    BigDecimal Plus = FallimentiPiattaforme.Numero(v[19]);
                    if (Plus != null) R.Risultato = R.Risultato.add(Plus);
                }
                if (!SOTTOWALLET.equals(v[4])) continue;
                BigDecimal QEntra = FallimentiPiattaforme.Numero(v[13]);
                if (!v[11].isBlank() && QEntra != null && QEntra.signum() > 0 && !FallimentiPiattaforme.isTokenCredito(v[11])) {
                    Entrate.merge(v[11], QEntra, BigDecimal::add);
                    BigDecimal Val = FallimentiPiattaforme.Numero(v[15]);
                    if (Val != null) R.ValoreEUR = R.ValoreEUR.add(Val.abs());
                }
                //L'uscita della moneta ricevuta: se è abbinata a un deposito, [20] porta all'arrivo
                if (!v[8].isBlank() && v[11].isBlank() && !FallimentiPiattaforme.isTokenCredito(v[8]) && v[20] != null) {
                    for (String Altro : v[20].split(",")) {
                        String[] d = MappaCryptoWallet.get(Altro.trim());
                        if (d != null && !Chiave.equals(GruppoOperazione.Chiave(d))) {
                            Arrivi.put(v[8], d[3] + " / " + d[4]);
                            Tutti.add(d[0]);
                        }
                    }
                }
            }
            R.ID.addAll(Tutti);
            if (R.Data.equals(Chiusa)) {
                R.Tipo = "Chiusura";
                R.ValoreEUR = null;
            } else {
                R.Tipo = Oltre && !ConCredito ? "Oltre il credito" : Natura ? "Restituzione in natura" : "Rimborso";
                FallimentiPiattaforme.Tranche t = Piano.get(R.Data);
                //Un rimborso oltre il credito si registra senza percentuale: nel piano ha zero, qui resta vuoto
                if (t != null && t.Percentuale.signum() > 0) {
                    R.Percentuale = t.Percentuale;
                    R.USD = t.USDPagati;
                }
                List<String> Monete = new ArrayList<>();
                java.util.Set<String> Dove = new java.util.LinkedHashSet<>();
                for (Map.Entry<String, BigDecimal> m : Entrate.entrySet()) {
                    Monete.add(m.getValue().setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " " + m.getKey());
                    Dove.add(Arrivi.getOrDefault(m.getKey(), "fuori dal programma"));
                }
                R.Ricevute = String.join(", ", Monete);
                R.Arrivo = String.join(", ", Dove);
            }
            Ris.add(R);
        }
        return Ris;
    }
}
