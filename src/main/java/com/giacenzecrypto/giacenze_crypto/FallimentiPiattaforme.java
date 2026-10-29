package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * Il fallimento di una piattaforma e i rimborsi della procedura.
 *
 * <p>Alla data della domanda di fallimento le monete ferme sulla piattaforma diventano un <b>credito</b> verso la
 * procedura: ogni moneta {@code X} passa al token {@code <PIATTAFORMA>.CREDITO.<X>} con uno scambio che porta con sé
 * il costo di carico. I rimborsi, a tranche e in monete diverse, sono scambi dal token credito alla moneta ricevuta, e
 * sono un realizzo (art. 67, c. 1, lett. c-sexies TUIR: «rimborso»). Il nome del token è solo un'etichetta: il
 * programma non lo interpreta mai.
 *
 * <p>I tre marcatori del campo 18 decidono da soli la rilevanza dello scambio, <b>a prescindere dalle opzioni</b>
 * {@code Plusvalenze_Pre2023ScambiRilevanti}/{@code Plusvalenze_ScambiSempreRilevanti} e dai tipi delle due monete
 * (i fallimenti Celsius e FTX sono del 2022, e un rimborso cripto-cripto dopo il 2023 sarebbe altrimenti neutro):
 * <ul>
 *   <li>{@value #BLOCCO}: blocco per fallimento, costo trasferito, mai plusvalenza;</li>
 *   <li>{@value #RIMBORSO}: rimborso da fallimento, sempre realizzo (plusvalenza o minusvalenza);</li>
 *   <li>{@value #RESTITUZIONE_IN_NATURA}: restituzione della stessa moneta trattata come giroconto, costo trasferito.</li>
 * </ul>
 * La chiusura della procedura usa il {@code PWN} esistente: il residuo esce senza minusvalenza.
 *
 * <p>Li leggono il motore delle plusvalenze ({@code Calcoli_PlusvalenzeNew.ElaboraMovimento}), la sua copia
 * {@code Funzioni.MovimentoRilevante} e il motore del quadro RW ({@code Calcoli_RW.ScambioRilevante}).
 * Analisi e decisioni: {@code nocommit/Documentazione/Analisi_Fallimenti_Piattaforme.md}.
 */
public final class FallimentiPiattaforme {

    /** Codice del campo 18 del blocco per fallimento. */
    public static final String BLOCCO = "FBL";
    /** Codice del campo 18 del rimborso da fallimento. */
    public static final String RIMBORSO = "FRB";
    /** Codice del campo 18 della restituzione in natura trattata come giroconto. */
    public static final String RESTITUZIONE_IN_NATURA = "FGC";
    /**
     * Codice del campo 18 di un rimborso arrivato quando il credito è già esaurito: un deposito (cripto o FIAT) senza
     * token credito da scambiare, il cui valore è tutto plusvalenza (il costo del credito è già stato usato).
     */
    public static final String RIMBORSO_OLTRE_IL_CREDITO = "FRT";

    /** Campo 18 completo del blocco. */
    public static final String CAMPO18_BLOCCO = BLOCCO + " - BLOCCO PER FALLIMENTO";
    /** Campo 18 completo del rimborso. */
    public static final String CAMPO18_RIMBORSO = RIMBORSO + " - RIMBORSO DA FALLIMENTO";
    /** Campo 18 completo della restituzione in natura. */
    public static final String CAMPO18_RESTITUZIONE_IN_NATURA = RESTITUZIONE_IN_NATURA + " - RESTITUZIONE IN NATURA";
    /**
     * Campo 18 di un token credito consumato da una tranche senza ricevere nulla (una parte a cui non tocca valore, per
     * esempio un credito con valore zero alla domanda): un prelievo {@code PC} col {@code PCO} esistente, che il motore
     * realizza a valore zero, quindi minusvalenza pari al costo scaricato. Un {@code PC} con {@code FRB} il motore non lo
     * elaborava: la quantità usciva e il costo restava nel LIFO.
     */
    public static final String CAMPO18_RIMBORSO_SENZA_VALORE = "PCO - RIMBORSO DA FALLIMENTO SENZA VALORE";
    /** Campo 18 completo del rimborso oltre il credito. */
    public static final String CAMPO18_RIMBORSO_OLTRE_IL_CREDITO = RIMBORSO_OLTRE_IL_CREDITO + " - RIMBORSO OLTRE IL CREDITO";

    private FallimentiPiattaforme() {
    }

    /**
     * Il codice sta in testa al campo 18 ({@code "FBL - ..."}): si confronta l'inizio e non si cerca la sottostringa,
     * perché il resto del campo è testo descrittivo.
     */
    static boolean Inizia(String Campo18, String Codice) {
        return Campo18 != null && Campo18.trim().toUpperCase().startsWith(Codice);
    }

    /** @return {@code true} se lo scambio trasferisce sempre il costo senza plusvalenza (blocco o restituzione in natura) */
    public static boolean TrasferisceCosto(String Campo18) {
        return Inizia(Campo18, BLOCCO) || Inizia(Campo18, RESTITUZIONE_IN_NATURA);
    }

    /** @return {@code true} se lo scambio è sempre un realizzo (rimborso da fallimento) */
    public static boolean Realizza(String Campo18) {
        return Inizia(Campo18, RIMBORSO);
    }

    /**
     * @return {@code true} se il deposito è un rimborso oltre il credito: costo di carico e plusvalenza pari al valore,
     *         sempre, qualunque siano le opzioni delle reward (non è una reward)
     */
    public static boolean RimborsoOltreIlCredito(String Campo18) {
        return Inizia(Campo18, RIMBORSO_OLTRE_IL_CREDITO);
    }

    // ------------------------------------------------------------------
    // Le procedure: i fatti del piano (tabelle FALLIMENTI e FALLIMENTI_TRANCHE)
    // ------------------------------------------------------------------

    /** Prefisso della chiave di operazione {@code [45]} delle righe di una procedura, seguito dal suo id. */
    public static final String PREFISSO_CHIAVE = "FALL-";

    /**
     * Una procedura di fallimento come la descrive il piano. Il motore delle plusvalenze non la legge mai: le
     * quantità e i valori stanno nelle righe dei movimenti. Serve al dialogo e al valore del credito nel quadro RW.
     */
    public static final class Procedura {
        /** Identificativo, anche nella chiave {@code FALL-<Id>} delle righe. */
        public String Id = "";
        /** Nome breve della piattaforma nei token credito ({@code CELSIUS}). */
        public String NomeBreve = "";
        /** Exchange delle righe ({@code [3]}). */
        public String Exchange = "";
        /** Data della domanda di fallimento, {@code yyyy-MM-dd}. */
        public String DataDomanda = "";
        /** Credito ammesso in dollari alla data della domanda, {@code null} se non indicato. */
        public BigDecimal CreditoUSD;
        /** Percentuale di recupero attesa per il metodo A' (100 = metodo A). */
        public BigDecimal RecuperoAtteso = CENTO;
        /**
         * Valore in euro, alla data della domanda, dei FIAT bloccati. Solo informativo: i calcoli leggono i crediti in
         * valuta dal registro ({@link TokenCredito#Fiat}), che li ricava dalle righe di blocco.
         */
        public BigDecimal FiatEUR = BigDecimal.ZERO;
        /** {@code APERTA} o {@code CHIUSA}. */
        public String Stato = "APERTA";
        /** Le tranche pagate, in ordine di data. */
        public final List<Tranche> Tranche = new ArrayList<>();
    }

    /** Una tranche pagata dalla procedura. */
    public static final class Tranche {
        /** Data e ora del pagamento, {@code yyyy-MM-dd HH:mm}. */
        public String Data = "";
        /** Percentuale del credito pagata da questa tranche. */
        public BigDecimal Percentuale = BigDecimal.ZERO;
        /** Dollari pagati da questa tranche (credito × percentuale, o l'importo del piano). */
        public BigDecimal USDPagati = BigDecimal.ZERO;
    }

    static final BigDecimal CENTO = new BigDecimal(100);

    /** Crea le due tabelle in {@code personale.mv.db} (idempotente, chiamato da {@code DatabaseH2.CreaoCollegaDatabase}). */
    static void CreaTabelle(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS FALLIMENTI ("
                    + "Id VARCHAR(100) PRIMARY KEY, NomeBreve VARCHAR(100), Exchange VARCHAR(255), "
                    + "DataDomanda VARCHAR(20), CreditoUSD VARCHAR(40), RecuperoAtteso VARCHAR(40), Stato VARCHAR(10), "
                    + "FiatEUR VARCHAR(40))");
            st.execute("ALTER TABLE FALLIMENTI ADD COLUMN IF NOT EXISTS FiatEUR VARCHAR(40)");
            st.execute("CREATE TABLE IF NOT EXISTS FALLIMENTI_TRANCHE ("
                    + "IdProcedura VARCHAR(100), Progressivo INT, Data VARCHAR(20), "
                    + "Percentuale VARCHAR(40), USDPagati VARCHAR(40), PRIMARY KEY (IdProcedura, Progressivo))");
        }
    }

    /** @return le procedure registrate, per id, con le loro tranche in ordine di data */
    public static Map<String, Procedura> LeggiProcedure() {
        Map<String, Procedura> Ris = new TreeMap<>();
        Connection c = DatabaseH2.connectionPersonale;
        if (c == null) return Ris;
        try (Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT Id, NomeBreve, Exchange, DataDomanda, CreditoUSD, RecuperoAtteso, Stato, FiatEUR FROM FALLIMENTI")) {
                while (rs.next()) {
                    Procedura p = new Procedura();
                    p.Id = Testo(rs.getString(1));
                    p.NomeBreve = Testo(rs.getString(2));
                    p.Exchange = Testo(rs.getString(3));
                    p.DataDomanda = Testo(rs.getString(4));
                    p.CreditoUSD = Numero(rs.getString(5));
                    BigDecimal Atteso = Numero(rs.getString(6));
                    if (Atteso != null && Atteso.signum() > 0) p.RecuperoAtteso = Atteso;
                    p.Stato = Testo(rs.getString(7));
                    BigDecimal Fiat = Numero(rs.getString(8));
                    if (Fiat != null) p.FiatEUR = Fiat;
                    Ris.put(p.Id, p);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT IdProcedura, Data, Percentuale, USDPagati FROM FALLIMENTI_TRANCHE ORDER BY IdProcedura, Data, Progressivo")) {
                while (rs.next()) {
                    Procedura p = Ris.get(Testo(rs.getString(1)));
                    if (p == null) continue;
                    Tranche t = new Tranche();
                    t.Data = Testo(rs.getString(2));
                    BigDecimal Perc = Numero(rs.getString(3));
                    BigDecimal Usd = Numero(rs.getString(4));
                    t.Percentuale = Perc == null ? BigDecimal.ZERO : Perc;
                    t.USDPagati = Usd == null ? BigDecimal.ZERO : Usd;
                    p.Tranche.add(t);
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Ris;
    }

    /** Scrive (o sostituisce) una procedura con tutte le sue tranche, e invalida il registro. */
    public static boolean ScriviProcedura(Procedura p) {
        Connection c = DatabaseH2.connectionPersonale;
        if (c == null || p == null || p.Id.isBlank()) return false;
        try {
            try (PreparedStatement ps = c.prepareStatement("MERGE INTO FALLIMENTI (Id, NomeBreve, Exchange, DataDomanda, CreditoUSD, RecuperoAtteso, Stato, FiatEUR) KEY (Id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, p.Id);
                ps.setString(2, p.NomeBreve);
                ps.setString(3, p.Exchange);
                ps.setString(4, p.DataDomanda);
                ps.setString(5, p.CreditoUSD == null ? "" : p.CreditoUSD.toPlainString());
                ps.setString(6, p.RecuperoAtteso == null ? "" : p.RecuperoAtteso.toPlainString());
                ps.setString(7, p.Stato);
                ps.setString(8, p.FiatEUR == null ? "0" : p.FiatEUR.toPlainString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM FALLIMENTI_TRANCHE WHERE IdProcedura = ?")) {
                ps.setString(1, p.Id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO FALLIMENTI_TRANCHE VALUES (?, ?, ?, ?, ?)")) {
                int Prog = 0;
                for (Tranche t : p.Tranche) {
                    ps.setString(1, p.Id);
                    ps.setInt(2, ++Prog);
                    ps.setString(3, t.Data);
                    ps.setString(4, t.Percentuale.toPlainString());
                    ps.setString(5, t.USDPagati.toPlainString());
                    ps.executeUpdate();
                }
            }
            Invalida();
            return true;
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    /** Cancella una procedura e le sue tranche (le righe dei movimenti restano: le toglie chi chiama). */
    public static boolean CancellaProcedura(String Id) {
        Connection c = DatabaseH2.connectionPersonale;
        if (c == null || Id == null) return false;
        try (PreparedStatement p1 = c.prepareStatement("DELETE FROM FALLIMENTI_TRANCHE WHERE IdProcedura = ?");
             PreparedStatement p2 = c.prepareStatement("DELETE FROM FALLIMENTI WHERE Id = ?")) {
            p1.setString(1, Id);
            p1.executeUpdate();
            p2.setString(1, Id);
            p2.executeUpdate();
            Invalida();
            return true;
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    private static String Testo(String s) {
        return s == null ? "" : s.trim();
    }

    static BigDecimal Numero(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Il registro dei token credito e il loro prezzo
    // ------------------------------------------------------------------

    /** Opzione personale: come si valuta il token credito nelle giacenze (quadro RW, Giacenze a data). */
    public static final String OPZIONE_VALORE = "RW_ValoreCreditiFallimento";
    /** Prezzo attuale della moneta originale. */
    public static final String VALORE_PREZZO_ORIGINALE = "A";
    /** Credito nominale ancora da rimborsare, al cambio USD/EUR del giorno (default). */
    public static final String VALORE_CREDITO = "B";
    /** Costo di carico. */
    public static final String VALORE_COSTO = "C";

    /** Nome dell'opzione nella fotografia delle dichiarazioni (manifest) e nella stampa del quadro W/RW. */
    public static final String NOME_OPZIONE = "Valore dei crediti verso piattaforme fallite";

    /**
     * Effetto della scelta {@value #OPZIONE_VALORE}, lo stesso testo nel manifest delle dichiarazioni e nella stampa.
     * @param Opzione {@value #VALORE_PREZZO_ORIGINALE}, {@value #VALORE_CREDITO} o {@value #VALORE_COSTO}
     */
    public static String DescrizioneValore(String Opzione) {
        if (VALORE_PREZZO_ORIGINALE.equalsIgnoreCase(Opzione)) return "prezzo di mercato della moneta che era depositata";
        if (VALORE_COSTO.equalsIgnoreCase(Opzione)) return "costo di carico trasferito al credito";
        return "credito nominale ancora da rimborsare, dollari al cambio del giorno";
    }

    /** @return l'effetto della scelta salvata nelle opzioni personali */
    public static String DescrizioneValore() {
        return DescrizioneValore(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_VALORE, VALORE_CREDITO));
    }

    /**
     * @param Anno anno del quadro W/RW ({@code yyyy})
     * @return {@code true} se nell'anno è detenuto un token credito: in giacenza all'inizio dell'anno o movimentato
     *         durante l'anno. Solo allora la scelta {@value #OPZIONE_VALORE} cambia i valori del quadro, e solo allora
     *         entra nella fotografia della dichiarazione (un'opzione sempre presente farebbe risultare diversa dal
     *         ricalcolo ogni dichiarazione archiviata prima)
     */
    public static boolean InfluisconoSullAnno(String Anno) {
        long Inizio = FunzioniDate.ConvertiDatainLongMinuto(Anno + "-01-01 00:00");
        long Fine = FunzioniDate.ConvertiDatainLongMinuto(Anno + "-12-31 23:59");
        for (TokenCredito t : Registro().Token.values()) {
            if (t.QtaAl(Inizio).signum() > 0) return true;
            if (!t.Variazioni.subMap(Inizio, true, Fine, true).isEmpty()) return true;
        }
        return false;
    }

    /** Un token credito, nato da una riga di blocco. */
    static final class TokenCredito {
        String Simbolo;
        String IdProcedura;
        String MonetaOriginale;
        String AddressOriginale;
        String ReteOriginale;
        /**
         * Credito in valuta (tipo FIAT, nato da euro o dollari rimasti sulla piattaforma): resta FIAT, non ha costo di
         * carico e vale la sua valuta alla pari.
         */
        boolean Fiat;
        /** Quantità bloccata. */
        BigDecimal Qta;
        /** Valore del credito in euro alla data della domanda ({@code [15]} del blocco): il peso. */
        BigDecimal ValoreEUR;
        /** Costo di carico trasferito ({@code [17]} del blocco), {@code null} se il motore non l'ha ancora scritto. */
        BigDecimal Costo;
        /** Variazioni di quantità del token, per istante (millisecondi): il blocco in positivo, le uscite in negativo. */
        final TreeMap<Long, BigDecimal> Variazioni = new TreeMap<>();

        BigDecimal QtaAl(long Istante) {
            BigDecimal Q = BigDecimal.ZERO;
            for (BigDecimal d : Variazioni.headMap(Istante, true).values()) Q = Q.add(d);
            return Q;
        }
    }

    /** Il registro: token credito per simbolo e procedure per id. Immutabile una volta costruito. */
    static final class Registro {
        final Map<String, TokenCredito> Token = new HashMap<>();
        final Map<String, Procedura> Procedure;
        /** Per procedura, le date ({@code [1]}) in cui la mappa ha righe di rimborso: le tranche che esistono davvero. */
        final Map<String, java.util.Set<String>> DateTranche = new HashMap<>();
        /** Per procedura chiusa, la data ({@code [1]}) delle righe di chiusura. */
        final Map<String, String> Chiusure = new HashMap<>();
        /** Per procedura, le date ({@code [1]}) dei rimborsi arrivati a credito esaurito. */
        final Map<String, java.util.Set<String>> DateOltreIlCredito = new HashMap<>();

        Registro(Map<String, Procedura> Procedure) {
            this.Procedure = Procedure;
        }
    }

    private static volatile Registro registro;

    /** Il registro va ricostruito: chiamato a ogni modifica dei movimenti e delle procedure. */
    public static void Invalida() {
        registro = null;
    }

    /** Ricostruisce subito il registro, da chiamare in testa ai calcoli che ne dipendono (quadro RW). */
    public static void Ricostruisci() {
        registro = Costruisci(Principale.MappaCryptoWallet.values(), LeggiProcedure());
    }

    static Registro Registro() {
        Registro r = registro;
        if (r == null) {
            try {
                r = Costruisci(Principale.MappaCryptoWallet.values(), LeggiProcedure());
                registro = r;
            } catch (java.util.ConcurrentModificationException ex) {
                //La mappa cambia mentre la si legge (un altro thread la sta scrivendo): questa valutazione resta
                //senza registro e il prossimo accesso riprova. Il quadro RW non passa di qui (Ricostruisci)
                LoggerGC.ScriviErrore("FallimentiPiattaforme: movimenti modificati durante la costruzione del registro");
                return new Registro(new TreeMap<>());
            }
        }
        return r;
    }

    /**
     * Costruisce il registro con due passate: la prima trova le righe di blocco (i token credito), la seconda
     * raccoglie ogni entrata e uscita di quei simboli. Il nome del token non viene mai interpretato.
     */
    static Registro Costruisci(Collection<String[]> Movimenti, Map<String, Procedura> Procedure) {
        Registro r = new Registro(Procedure);
        for (String[] v : Movimenti) {
            if (v == null || v.length <= GruppoOperazione.CAMPO || !Inizia(v[18], BLOCCO)) continue;
            TokenCredito t = r.Token.get(v[11]);
            if (t == null) {
                t = new TokenCredito();
                t.Simbolo = v[11];
                t.Qta = BigDecimal.ZERO;
                t.ValoreEUR = BigDecimal.ZERO;
                r.Token.put(v[11], t);
            }
            String Chiave = GruppoOperazione.Chiave(v);
            t.IdProcedura = Chiave.startsWith(PREFISSO_CHIAVE) ? Chiave.substring(PREFISSO_CHIAVE.length()) : "";
            t.MonetaOriginale = v[8];
            t.AddressOriginale = v[26];
            t.ReteOriginale = Funzioni.TrovaReteDaIMovimento(v);
            t.Fiat = "FIAT".equalsIgnoreCase(v[12]);
            BigDecimal Q = Numero(v[13]);
            BigDecimal Val = Numero(v[15]);
            BigDecimal Cos = Numero(v[17]);
            if (Q != null) t.Qta = t.Qta.add(Q.abs());
            if (Val != null) t.ValoreEUR = t.ValoreEUR.add(Val.abs());
            if (Cos != null) t.Costo = t.Costo == null ? Cos : t.Costo.add(Cos);
        }
        //Un deposito FRT dentro una tranche che consuma credito (la parte cripto di un rimborso che vale più di quanto
        //consuma, con "Consuma prima il credito FIAT") non fa della tranche un rimborso oltre il credito
        Map<String, java.util.Set<String>> ConCredito = new HashMap<>();
        for (String[] v : Movimenti) {
            if (v == null || v.length <= GruppoOperazione.CAMPO) continue;
            if (CAMPO5_CHIUSURA.equals(v[5]) && GruppoOperazione.Chiave(v).startsWith(PREFISSO_CHIAVE)) {
                r.Chiusure.put(GruppoOperazione.Chiave(v).substring(PREFISSO_CHIAVE.length()), v[1]);
            }
            if (!isRigaDiRimborso(v)) continue;
            String Chiave = GruppoOperazione.Chiave(v);
            if (Chiave.startsWith(PREFISSO_CHIAVE)) {
                String Id = Chiave.substring(PREFISSO_CHIAVE.length());
                r.DateTranche.computeIfAbsent(Id, k -> new java.util.HashSet<>()).add(v[1]);
                if (RimborsoOltreIlCredito(v[18])) r.DateOltreIlCredito.computeIfAbsent(Id, k -> new java.util.HashSet<>()).add(v[1]);
                else ConCredito.computeIfAbsent(Id, k -> new java.util.HashSet<>()).add(v[1]);
            }
        }
        for (Map.Entry<String, java.util.Set<String>> e : r.DateOltreIlCredito.entrySet()) {
            e.getValue().removeAll(ConCredito.getOrDefault(e.getKey(), java.util.Set.of()));
        }
        if (r.Token.isEmpty()) return r;
        for (String[] v : Movimenti) {
            if (v == null || v.length < 14) continue;
            TokenCredito U = r.Token.get(v[8]);
            TokenCredito E = r.Token.get(v[11]);
            if (U == null && E == null) continue;
            long Istante = FunzioniDate.ConvertiDatainLongMinuto(v[1]);
            if (U != null) {
                BigDecimal Q = Numero(v[10]);
                if (Q != null) U.Variazioni.merge(Istante, Q.abs().negate(), BigDecimal::add);
            }
            if (E != null) {
                BigDecimal Q = Numero(v[13]);
                if (Q != null) E.Variazioni.merge(Istante, Q.abs(), BigDecimal::add);
            }
        }
        return r;
    }

    /** Campo 5 delle righe di una tranche che non hanno un marcatore nel campo 18 (quota dei FIAT). */
    static final String CAMPO5_ACQUISTO_FIAT = "ACQUISTO CON RIMBORSO DA FALLIMENTO";
    static final String CAMPO5_RIMBORSO_FIAT = "RIMBORSO DA FALLIMENTO (FIAT)";
    /** Campo 5 del deposito di un rimborso oltre il credito (campo 18 {@value #CAMPO18_RIMBORSO_OLTRE_IL_CREDITO}). */
    static final String CAMPO5_RIMBORSO_OLTRE_IL_CREDITO = "RIMBORSO OLTRE IL CREDITO";
    /** Campo 5 delle righe di chiusura: il residuo esce senza valore (campo 18 {@value #CAMPO18_CHIUSURA} per i token). */
    static final String CAMPO5_CHIUSURA = "CHIUSURA PROCEDURA FALLIMENTO";
    /** Campo 18 della chiusura di un token credito: il {@code PWN} esistente, uscita senza minusvalenza. */
    static final String CAMPO18_CHIUSURA = "PWN - CHIUSURA PROCEDURA FALLIMENTO";

    /** @return {@code true} se la riga fa parte di una tranche di rimborso (marcatore o campo 5 della quota FIAT) */
    static boolean isRigaDiRimborso(String[] v) {
        return Realizza(v[18]) || Inizia(v[18], RESTITUZIONE_IN_NATURA) || RimborsoOltreIlCredito(v[18])
                || CAMPO18_RIMBORSO_SENZA_VALORE.equals(v[18])
                || CAMPO5_ACQUISTO_FIAT.equals(v[5]) || CAMPO5_RIMBORSO_FIAT.equals(v[5]);
    }

    /**
     * Le tranche della procedura che esistono anche nei movimenti. Una tranche registrata e poi non salvata (o
     * tolta con l'annullamento) resta nella tabella ma non nella mappa: non va contata, né nel credito residuo
     * dell'opzione B né nell'elenco.
     */
    public static List<Tranche> TrancheValide(Procedura p) {
        java.util.Set<String> Date = Registro().DateTranche.getOrDefault(p.Id, java.util.Set.of());
        List<Tranche> Ris = new ArrayList<>();
        for (Tranche t : p.Tranche) {
            if (Date.contains(t.Data)) Ris.add(t);
        }
        return Ris;
    }

    /** @return la data ({@code [1]}) della chiusura della procedura, {@code null} se non è chiusa */
    public static String DataChiusura(String Id) {
        return Registro().Chiusure.get(Id);
    }

    /** @return quanti rimborsi della procedura sono arrivati a credito esaurito */
    public static int RimborsiOltreIlCredito(String Id) {
        return Registro().DateOltreIlCredito.getOrDefault(Id, java.util.Set.of()).size();
    }

    /** @return {@code true} se il simbolo è un token credito di una procedura (nato da una riga di blocco) */
    public static boolean isTokenCredito(String Simbolo) {
        return Simbolo != null && Registro().Token.containsKey(Simbolo);
    }

    /**
     * La valuta di un credito in valuta ({@code CELSIUS.CREDITO.EUR} → {@code EUR}), per la parte FIAT del quadro W/RW:
     * lì il credito resta la valuta che era sulla piattaforma, come prima del blocco.
     * @return la valuta originale per un credito in valuta, altrimenti il simbolo così com'è
     */
    public static String ValutaDelCredito(String Simbolo) {
        if (Simbolo == null) return null;
        TokenCredito t = Registro().Token.get(Simbolo);
        return t != null && t.Fiat ? t.MonetaOriginale : Simbolo;
    }

    /** Conversione dollari → euro alla data ({@code yyyy-MM-dd}); sostituibile nei test, che non usano la rete. */
    static BiFunction<String, String, String> CambioUSDEUR = Prezzi::CambioUSDEUR;

    /**
     * Prezzo di un token credito in una valutazione a moneta singola (giacenze del quadro RW, Giacenze a data),
     * secondo l'opzione {@value #OPZIONE_VALORE}. Chiamato da {@code Prezzi.DammiPrezzoInfoTransazione} solo
     * quando la seconda gamba manca: il valore di un movimento non deve mai dipendere da un'opzione del quadro RW,
     * che il ricalcolo incrementale delle plusvalenze non conosce.
     * @param m la moneta valutata (simbolo e quantità)
     * @param Istante istante della valutazione, millisecondi
     * @return il prezzo, oppure {@code null} se la moneta non è un token credito (o il prezzo non si può dare)
     */
    static Prezzi.InfoPrezzo PrezzoCredito(Moneta m, long Istante) {
        if (m == null || m.Moneta == null || m.Moneta.isBlank()) return null;
        if (m.MonetaAddress != null && !m.MonetaAddress.isBlank()) return null;
        Registro r = Registro();
        TokenCredito t = r.Token.get(m.Moneta);
        if (t == null) return null;
        String Opzione = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_VALORE, VALORE_CREDITO);
        BigDecimal Qta = Numero(m.Qta);
        if (Qta == null) Qta = BigDecimal.ZERO;

        //Credito in valuta: con ogni opzione vale la sua valuta (l'euro alla pari, le altre al cambio del giorno). Non
        //ha costo di carico (il motore non lo scrive sulle gambe FIAT) e la sua quantità è già il credito residuo
        if (t.Fiat) {
            if (t.MonetaOriginale.equalsIgnoreCase("EUR")) return Info(m, Qta, BigDecimal.ONE, Istante, "Credito fallimento: euro");
            Moneta o = new Moneta();
            o.Moneta = t.MonetaOriginale;
            o.MonetaAddress = "";
            o.Rete = "";
            o.Tipo = "FIAT";
            o.Qta = Qta.abs().toPlainString();
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(o, null, Istante, "", "");
            if (IP == null || IP.prezzoUnitario == null) return null;
            return Info(m, Qta, IP.prezzoUnitario, Istante, "Credito fallimento: " + t.MonetaOriginale);
        }

        if (VALORE_PREZZO_ORIGINALE.equalsIgnoreCase(Opzione)) {
            Moneta o = new Moneta();
            o.Moneta = t.MonetaOriginale;
            o.MonetaAddress = t.AddressOriginale;
            o.Rete = t.ReteOriginale;
            o.Tipo = "Crypto";
            o.Qta = Qta.abs().toPlainString();
            Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(o, null, Istante, t.ReteOriginale, "");
            if (IP == null || IP.prezzoUnitario == null) return null;
            return Info(m, Qta, IP.prezzoUnitario, Istante, "Credito fallimento: prezzo di " + t.MonetaOriginale);
        }
        BigDecimal Unitario = VALORE_COSTO.equalsIgnoreCase(Opzione)
                ? UnitarioCosto(t) : UnitarioCredito(r, t, Istante);
        if (Unitario == null) return null;
        return Info(m, Qta, Unitario, Istante, VALORE_COSTO.equalsIgnoreCase(Opzione)
                ? "Credito fallimento: costo di carico" : "Credito fallimento: credito residuo");
    }

    private static Prezzi.InfoPrezzo Info(Moneta m, BigDecimal Qta, BigDecimal Unitario, long Istante, String Fonte) {
        Prezzi.InfoPrezzo IP = new Prezzi.InfoPrezzo();
        IP.Moneta = m.Moneta;
        //Come le altre fonti: la quantità col suo segno, il valore in assoluto
        IP.Qta = Qta;
        IP.prezzoUnitario = Unitario;
        IP.prezzoQta = Qta.multiply(Unitario).abs();
        IP.Fonte = Fonte;
        IP.timestamp = Istante;
        return IP;
    }

    /** Opzione C: costo unitario del blocco (il token credito nasce con un solo lotto). */
    static BigDecimal UnitarioCosto(TokenCredito t) {
        if (t.Costo == null || t.Qta.signum() == 0) return null;
        return t.Costo.divide(t.Qta, 20, RoundingMode.HALF_UP);
    }

    /**
     * Opzione B: credito nominale ancora da rimborsare all'istante, ripartito sul token col peso del suo valore
     * alla data della domanda, al cambio USD/EUR del giorno, diviso per la quantità residua. Senza il credito in
     * dollari della procedura resta il valore unitario del credito in euro alla data della domanda.
     *
     * <p>I crediti in valuta della procedura valgono la loro quantità residua alla pari: dei dollari ancora da pagare
     * si toglie prima la loro parte (la quota del credito in dollari che avevano alla domanda, per la frazione che ne
     * resta) e il resto si ripartisce fra i token cripto. Con le tranche proporzionali e recupero atteso 100 è la
     * stessa ripartizione di prima, con "Consuma prima il credito FIAT" i dollari pagati coprono prima la valuta.
     */
    static BigDecimal UnitarioCredito(Registro r, TokenCredito t, long Istante) {
        BigDecimal Residua = t.QtaAl(Istante);
        if (Residua.signum() <= 0) return BigDecimal.ZERO;
        Procedura p = r.Procedure.get(t.IdProcedura);
        if (p == null || p.CreditoUSD == null || p.CreditoUSD.signum() <= 0) {
            if (t.Qta.signum() == 0) return null;
            return t.ValoreEUR.divide(t.Qta, 20, RoundingMode.HALF_UP);
        }
        BigDecimal ValoreProcedura = BigDecimal.ZERO;
        BigDecimal ValoreCripto = BigDecimal.ZERO;
        BigDecimal FiatResiduaEUR = BigDecimal.ZERO;
        for (TokenCredito x : r.Token.values()) {
            if (!t.IdProcedura.equals(x.IdProcedura)) continue;
            ValoreProcedura = ValoreProcedura.add(x.ValoreEUR);
            if (!x.Fiat) ValoreCripto = ValoreCripto.add(x.ValoreEUR);
            else if (x.Qta.signum() > 0) FiatResiduaEUR = FiatResiduaEUR.add(x.ValoreEUR.multiply(x.QtaAl(Istante))
                    .divide(x.Qta, 20, RoundingMode.HALF_UP));
        }
        if (ValoreProcedura.signum() == 0 || ValoreCripto.signum() == 0) return null;
        BigDecimal Pagati = BigDecimal.ZERO;
        java.util.Set<String> Esistenti = r.DateTranche.getOrDefault(p.Id, java.util.Set.of());
        for (Tranche tr : p.Tranche) {
            if (Esistenti.contains(tr.Data) && FunzioniDate.ConvertiDatainLongMinuto(tr.Data) <= Istante) Pagati = Pagati.add(tr.USDPagati);
        }
        BigDecimal FiatResiduaUSD = p.CreditoUSD.multiply(FiatResiduaEUR).divide(ValoreProcedura, 20, RoundingMode.HALF_UP);
        BigDecimal ResiduoUSD = p.CreditoUSD.subtract(Pagati).subtract(FiatResiduaUSD).max(BigDecimal.ZERO)
                .multiply(t.ValoreEUR).divide(ValoreCripto, 20, RoundingMode.HALF_UP);
        String Giorno = FunzioniDate.ConvertiDatadaLong(Istante);
        String Euro = CambioUSDEUR.apply(ResiduoUSD.toPlainString(), Giorno);
        BigDecimal ResiduoEUR = Numero(Euro);
        if (ResiduoEUR == null) return null;
        return ResiduoEUR.divide(Residua, 20, RoundingMode.HALF_UP);
    }
}
