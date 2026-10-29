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
     * Valore in euro, alla data della domanda, delle giacenze FIAT della piattaforma: fanno parte del credito (pesano
     * nella ripartizione) ma restano FIAT. Una valuta senza cambio alla data non viene contata.
     */
    public static BigDecimal ValoreFiatAllaData(Map<String, BigDecimal> Fiat, String DataDomanda) {
        BigDecimal Totale = BigDecimal.ZERO;
        long Istante = IstanteDomanda(DataDomanda);
        for (Map.Entry<String, BigDecimal> e : Fiat.entrySet()) {
            if (e.getKey().equalsIgnoreCase("EUR")) {
                Totale = Totale.add(e.getValue());
                continue;
            }
            Moneta m = new Moneta();
            m.Moneta = e.getKey();
            m.Tipo = "FIAT";
            m.Qta = e.getValue().toPlainString();
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(m, null, Istante, "", "");
            if (IP != null && IP.prezzoUnitario != null) Totale = Totale.add(e.getValue().multiply(IP.prezzoUnitario).abs());
        }
        return Totale.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * I FIAT da spostare al blocco nel sotto-wallet {@value #SOTTOWALLET}, da cui escono le loro quote dei rimborsi e
     * il residuo alla chiusura. Per ogni valuta si sposta il <b>saldo netto</b> della piattaforma, lo stesso che pesa
     * nel credito ({@link #GiacenzeFiatAllaData}), prelevato dai sotto-wallet positivi a partire dal più grande: un
     * sotto-wallet negativo (scambi importati senza il deposito) resta com'è e la piattaforma torna a zero alla
     * chiusura. Il valore è quello in euro solo per l'EUR.
     */
    static List<Posizione> GiacenzeFiatPerSottoWallet(String Exchange, String DataDomanda) {
        List<Posizione> Ris = new ArrayList<>();
        long Istante = IstanteDomanda(DataDomanda);
        TreeSet<String> SottoWallet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] != null && v[3].trim().equalsIgnoreCase(Exchange) && v[4] != null) SottoWallet.add(v[4].trim());
        }
        Map<String, List<Posizione>> Positivi = new TreeMap<>();
        for (String sw : SottoWallet) {
            for (Moneta m : Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(), Istante, Exchange, sw).values()) {
                if (m.Tipo == null || !m.Tipo.equalsIgnoreCase("FIAT")) continue;
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

    /** Le giacenze FIAT positive alla data: restano FIAT sulla piattaforma, il loro valore entra nel credito. */
    public static Map<String, BigDecimal> GiacenzeFiatAllaData(String Exchange, String DataDomanda) {
        Map<String, BigDecimal> Ris = new TreeMap<>();
        Map<String, Moneta> Qta = Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(),
                IstanteDomanda(DataDomanda), Exchange, "tutti");
        for (Moneta m : Qta.values()) {
            if (m.Tipo == null || !m.Tipo.equalsIgnoreCase("FIAT")) continue;
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
     * usare la rete). Lasciato {@code null} dove il prezzo non si trova: l'utente lo inserisce a mano.
     */
    public static void Valorizza(List<Posizione> Posizioni, String DataDomanda) {
        long Istante = IstanteDomanda(DataDomanda);
        for (Posizione p : Posizioni) {
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

        //1 - Giroconti verso il sotto-wallet del credito, alla mezzanotte della domanda. Anche i FIAT, che restano
        //FIAT ma fanno parte del credito: le loro quote dei rimborsi e il residuo alla chiusura escono da lì
        List<Posizione> DaSpostare = new ArrayList<>(Posizioni);
        DaSpostare.addAll(GiacenzeFiatPerSottoWallet(P.Exchange, P.DataDomanda));
        int n = 0;
        for (Posizione p : DaSpostare) {
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

        //2 - Una riga di blocco per moneta, un secondo dopo
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
            Moneta Credito = new Moneta();
            Credito.Moneta = Nomi.get(e.getKey());
            Credito.Tipo = "Crypto";
            Credito.MonetaAddress = "";
            Credito.Rete = Prima.Rete;
            Credito.Qta = Qta.toPlainString();
            String ValoreTesto = Valore.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(Gamba(Prima, Qta.negate()), Credito, P.Exchange, SOTTOWALLET,
                    Istante + 1000, ValoreTesto, "Credito alla data della domanda", ++b, 1, null,
                    "Fallimento " + P.NomeBreve + ": credito alla data della domanda", "M", "", null, Identificativo);
            if (r == null) throw new IllegalStateException("Blocco non creato per " + Prima.Moneta);
            r[0] = SostituisciCategoria(r[0], "SC");
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

    /** Il blocco è sempre uno scambio cripto-cripto: la categoria in coda all'ID è {@code SC}. */
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

    /**
     * Prepara le righe di una tranche senza toccare la mappa. Metodo A': ogni token credito scarica la frazione
     * {@code min(Percentuale ÷ Atteso, frazione residua)} della quantità bloccata, e il valore delle monete ricevute
     * si divide fra i token (e i FIAT della procedura) col peso del loro credito alla data della domanda. Per ogni
     * coppia credito → moneta ricevuta una riga {@code FRB} (o {@code FGC} per la stessa moneta restituita in
     * natura); per la quota dei FIAT un acquisto della moneta ricevuta con quei FIAT. Le monete ricevute arrivano
     * sul sotto-wallet {@value #SOTTOWALLET} e da lì escono: verso il deposito importato (giroconto da abbinare) o,
     * per i FIAT ricevuti fuori dal programma, con un prelievo.
     * @param Data data e ora del rimborso, {@code yyyy-MM-dd HH:mm}
     * @param Percentuale percentuale del credito pagata dalla tranche
     * @param Atteso recupero atteso in percentuale
     */
    public static Tranche PreparaTranche(FallimentiPiattaforme.Procedura P, String Data, BigDecimal Percentuale,
            BigDecimal Atteso, List<Ricevuta> Ricevute) {
        Tranche T = new Tranche();
        long Istante = FunzioniDate.ConvertiDatainLongMinuto(Data);
        //Dopo il blocco, che sta alla mezzanotte della domanda e un secondo dopo
        if (Istante <= 0 || Istante <= IstanteDomanda(P.DataDomanda) + 1000) {
            T.Errore = "La data del rimborso dev'essere successiva alla data della domanda.";
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
        if (Percentuale == null || Percentuale.signum() <= 0 || Atteso == null || Atteso.signum() <= 0) {
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
        //Frazione residua, uguale per tutti i token perché ogni tranche li consuma nella stessa proporzione
        FallimentiPiattaforme.TokenCredito Primo = Token.get(0);
        BigDecimal Residua = Primo.QtaAl(IstanteResiduo).divide(Primo.Qta, SCALA, RoundingMode.HALF_UP);
        if (Residua.signum() <= 0) return OltreIlCredito(T, P, Istante, Percentuale, Ricevute, Primo.ReteOriginale);
        //In natura: con più parti (più monete, o FIAT) il rimborso si ripartirebbe su tutte e consumerebbe tutti i
        //token, mentre è tornata una moneta sola. Limite noto: si esclude quella posizione al blocco
        boolean PiuParti = Token.size() > 1 || (P.FiatEUR != null && P.FiatEUR.signum() > 0);
        for (Ricevuta r : Ricevute) {
            if (r.InNatura && PiuParti) {
                T.Errore = "La restituzione in natura si può registrare solo se il credito è di una moneta sola. "
                        + "Per una posizione restituita nella stessa moneta (per esempio un conto Custody) escludila "
                        + "dal blocco e classifica la sua restituzione come un normale trasferimento.";
                return T;
            }
        }
        BigDecimal c =Percentuale.divide(Atteso, SCALA, RoundingMode.HALF_UP).min(Residua);
        boolean Ultima = c.compareTo(Residua) == 0;
        T.Frazione = c;

        //Pesi: credito di ogni token e FIAT della procedura alla data della domanda
        Map<String, BigDecimal> Fiat = GiacenzeFiatAllaData(P.Exchange, P.DataDomanda);
        BigDecimal FiatEUR = P.FiatEUR == null ? BigDecimal.ZERO : P.FiatEUR;
        if (Fiat.isEmpty()) FiatEUR = BigDecimal.ZERO;
        BigDecimal Totale = FiatEUR;
        for (FallimentiPiattaforme.TokenCredito t : Token) Totale = Totale.add(t.ValoreEUR);
        if (Totale.signum() <= 0) {
            T.Errore = "Il valore del credito alla data della domanda è zero: non si può ripartire il rimborso.";
            return T;
        }
        //Parti del credito: i token e, se ci sono, le valute FIAT (il loro valore diviso per quantità)
        List<String> Parti = new ArrayList<>();
        List<BigDecimal> Pesi = new ArrayList<>();
        for (FallimentiPiattaforme.TokenCredito t : Token) {
            Parti.add(t.Simbolo);
            Pesi.add(t.ValoreEUR.divide(Totale, SCALA, RoundingMode.HALF_UP));
        }
        if (FiatEUR.signum() > 0) {
            BigDecimal FiatQta = BigDecimal.ZERO;
            for (BigDecimal q : Fiat.values()) FiatQta = FiatQta.add(q);
            for (Map.Entry<String, BigDecimal> e : Fiat.entrySet()) {
                Parti.add("FIAT:" + e.getKey());
                //Senza un cambio per valuta, le valute pesano in proporzione alla quantità dentro la quota FIAT
                Pesi.add(FiatEUR.multiply(e.getValue()).divide(FiatQta, SCALA, RoundingMode.HALF_UP)
                        .divide(Totale, SCALA, RoundingMode.HALF_UP));
            }
        }

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
        for (int p = 0; p < Parti.size(); p++) {
            BigDecimal QtaParte;
            if (p < Token.size()) {
                FallimentiPiattaforme.TokenCredito t = Token.get(p);
                QtaParte = Ultima ? t.QtaAl(IstanteResiduo) : t.Qta.multiply(c).setScale(SCALA, RoundingMode.HALF_UP);
            } else {
                QtaParte = Fiat.get(Parti.get(p).substring(5)).multiply(c).setScale(2, RoundingMode.HALF_UP);
            }
            UscitePerParte.add(Ripartisci(QtaParte, Quote, p < Token.size() ? SCALA : 2));
        }
        List<List<BigDecimal>> EntratePerMoneta = new ArrayList<>();
        List<List<BigDecimal>> ValoriPerMoneta = new ArrayList<>();
        for (Ricevuta r : Ricevute) {
            EntratePerMoneta.add(Ripartisci(r.Qta, Pesi, r.isFiat() ? 2 : SCALA));
            ValoriPerMoneta.add(Ripartisci(r.ValoreEUR, Pesi, 2));
        }

        for (int j = 0; j < Ricevute.size(); j++) {
            Ricevuta r = Ricevute.get(j);
            for (int p = 0; p < Parti.size(); p++) {
                BigDecimal Out = UscitePerParte.get(p).get(j);
                BigDecimal In = EntratePerMoneta.get(j).get(p);
                BigDecimal Val = ValoriPerMoneta.get(j).get(p);
                if (Out.signum() == 0 && In.signum() == 0) continue;
                Moneta Uscita = new Moneta();
                Moneta Entrata = new Moneta();
                Entrata.Moneta = r.Moneta;
                Entrata.Tipo = r.Tipo;
                Entrata.MonetaAddress = "";
                Entrata.Qta = In.toPlainString();
                RigaAnteprima A = new RigaAnteprima();
                String Campo18;
                String Campo5;
                if (p < Token.size()) {
                    FallimentiPiattaforme.TokenCredito t = Token.get(p);
                    Uscita.Moneta = t.Simbolo;
                    Uscita.Tipo = "Crypto";
                    Uscita.MonetaAddress = "";
                    Uscita.Rete = t.ReteOriginale;
                    Entrata.Rete = t.ReteOriginale;
                    boolean Natura = r.InNatura && r.Moneta.equalsIgnoreCase(t.MonetaOriginale);
                    Campo18 = Natura ? FallimentiPiattaforme.CAMPO18_RESTITUZIONE_IN_NATURA : FallimentiPiattaforme.CAMPO18_RIMBORSO;
                    Campo5 = Natura ? "RESTITUZIONE IN NATURA" : "RIMBORSO DA FALLIMENTO";
                    A.Credito = t.Simbolo;
                    if (t.Costo != null && t.Qta.signum() > 0) {
                        A.Costo = t.Costo.multiply(Out).divide(t.Qta, 2, RoundingMode.HALF_UP);
                    }
                    A.Trattamento = Natura ? "Giroconto: costo trasferito" : "Rimborso: plusvalenza o minusvalenza";
                } else {
                    String Valuta = Parti.get(p).substring(5);
                    Uscita.Moneta = Valuta;
                    Uscita.Tipo = "FIAT";
                    Uscita.MonetaAddress = "";
                    Campo18 = "";
                    Campo5 = r.isFiat() ? FallimentiPiattaforme.CAMPO5_RIMBORSO_FIAT : FallimentiPiattaforme.CAMPO5_ACQUISTO_FIAT;
                    A.Credito = Valuta + " (FIAT)";
                    A.Trattamento = r.isFiat() ? "FIAT: nessun effetto" : "Acquisto: costo = valore ricevuto";
                }
                Uscita.Qta = Out.negate().toPlainString();
                A.QtaUscita = Out;
                A.Ricevuta = r.Moneta;
                A.QtaEntrata = In;
                A.Valore = Val;
                T.Anteprima.add(A);

                String ValTesto = Val.setScale(2, RoundingMode.HALF_UP).toPlainString();
                List<String[]> Nuove = new ArrayList<>();
                if (Uscita.Tipo.equals("FIAT") && r.isFiat()) {
                    //FIAT contro FIAT: un prelievo e un deposito, nessun effetto fiscale
                    for (String[] Riga : new String[][]{
                        MovimentiCrypto.creaMovimento(Uscita, null, P.Exchange, SOTTOWALLET, Istante, ValTesto,
                            "Rimborso da fallimento", ++n, 1, null, Nota, "M", "", null, Identificativo),
                        MovimentiCrypto.creaMovimento(null, Entrata, P.Exchange, SOTTOWALLET, Istante, ValTesto,
                            "Rimborso da fallimento", n, 2, null, Nota, "M", "", null, Identificativo)}) {
                        if (Riga != null) Riga[5] = Campo5;
                        Nuove.add(Riga);
                    }
                } else {
                    String[] Riga = MovimentiCrypto.creaMovimento(Out.signum() == 0 ? null : Uscita, Entrata, P.Exchange,
                            SOTTOWALLET, Istante, ValTesto, "Rimborso da fallimento", ++n, 1, null, Nota, "M", "", null, Identificativo);
                    if (Riga != null) {
                        Riga[5] = Campo5;
                        Riga[18] = Campo18;
                    }
                    Nuove.add(Riga);
                }
                for (String[] Riga : Nuove) {
                    if (Riga == null) {
                        T.Errore = "Riga del rimborso non creata per " + A.Credito + " verso " + r.Moneta + ".";
                        return T;
                    }
                    Completa(Riga, Chiave, ValTesto, IDUsati);
                    T.Righe.add(Riga);
                }
            }

            if (!UscitaRicevuta(T, P, r, Istante, Primo.ReteOriginale, Nota, ++n, IDUsati)) return T;
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
        String Nota = "Fallimento " + P.NomeBreve + ": rimborso del " + Percentuale.stripTrailingZeros().toPlainString()
                + "% oltre il credito";
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
        t.Data = FunzioniDate.ConvertiDatadaLongAlSecondo(FunzioniDate.ConvertiDatainLongMinuto(Data)).substring(0, 16);
        t.Percentuale = Percentuale;
        t.USDPagati = USDPagati != null ? USDPagati : P.CreditoUSD == null ? BigDecimal.ZERO
                : P.CreditoUSD.multiply(Percentuale).divide(FallimentiPiattaforme.CENTO, 2, RoundingMode.HALF_UP);
        //Le tranche registrate e mai salvate non si riscrivono
        List<FallimentiPiattaforme.Tranche> Valide = FallimentiPiattaforme.TrancheValide(P);
        P.Tranche.retainAll(Valide);
        P.Tranche.add(t);
        P.RecuperoAtteso = Atteso;
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
        long Istante = FunzioniDate.ConvertiDatainLongMinuto(Data);
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
            m.Tipo = "Crypto";
            m.MonetaAddress = "";
            m.Rete = t.ReteOriginale;
            m.Qta = Residuo.stripTrailingZeros().negate().toPlainString();
            String[] r = MovimentiCrypto.creaMovimento(m, null, P.Exchange, SOTTOWALLET, Istante, "0.00",
                    "Chiusura procedura", ++n, 1, null, Nota, "M", "", null, Identificativo);
            if (r == null) {
                C.Errore = "Riga di chiusura non creata per " + t.Simbolo + ".";
                return C;
            }
            r[0] = SostituisciCategoria(r[0], "PC");
            r[5] = FallimentiPiattaforme.CAMPO5_CHIUSURA;
            r[18] = FallimentiPiattaforme.CAMPO18_CHIUSURA;
            Completa(r, Chiave, "0.00", IDUsati);
            C.Righe.add(r);
            String Costo = "";
            if (t.Costo != null && t.Qta.signum() > 0) {
                Costo = " (costo residuo " + t.Costo.multiply(Residuo).divide(t.Qta, 2, RoundingMode.HALF_UP).toPlainString()
                        + " EUR non dedotto)";
            }
            C.Descrizione.add(Residuo.stripTrailingZeros().toPlainString() + " " + t.Simbolo + Costo);
        }

        //I FIAT rimasti nel sotto-wallet del credito: le quote non consumate dalle tranche. Solo le righe di questa
        //procedura, perché una seconda procedura sulla stessa piattaforma usa lo stesso sotto-wallet
        Map<String, BigDecimal> Fiat = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (v[3] == null || !v[3].trim().equalsIgnoreCase(P.Exchange) || v[4] == null
                    || !v[4].trim().equalsIgnoreCase(SOTTOWALLET) || !Chiave.equals(GruppoOperazione.Chiave(v))) continue;
            if (FunzioniDate.ConvertiDatainLongMinuto(v[1]) > Istante) continue;
            if ("FIAT".equalsIgnoreCase(v[9]) && !v[8].isBlank()) Fiat.merge(v[8], Qta(v[10]), BigDecimal::add);
            if ("FIAT".equalsIgnoreCase(v[12]) && !v[11].isBlank()) Fiat.merge(v[11], Qta(v[13]), BigDecimal::add);
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
        for (FallimentiPiattaforme.TokenCredito t : FallimentiPiattaforme.Registro().Token.values()) {
            if (!Id.equals(t.IdProcedura) || t.Qta.signum() <= 0) continue;
            BigDecimal Residua = t.QtaAl(Long.MAX_VALUE).multiply(FallimentiPiattaforme.CENTO)
                    .divide(t.Qta, 2, RoundingMode.HALF_UP);
            return Residua.signum() <= 0 ? "Credito esaurito" + Coda
                    : "Aperta, residuo " + Residua.stripTrailingZeros().toPlainString() + "%" + Coda;
        }
        return "Aperta" + Coda;
    }
}
