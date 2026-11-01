package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Costo medio netto di "Giacenze a data", mostrato nel dialogo {@link GUI_CostiNetti} (pulsante <i>Costi netti</i>, solo
 * con tutti i wallet), stile scheda <i>My Portfolio</i> di CoinGecko:
 * <b>(speso - incassato) / quantità posseduta</b>. Non è una media ponderata del costo (quella toglie dal costo la
 * quantità venduta per il costo medio) ma un prezzo di pareggio, che può diventare zero o negativo.
 * Il <b>profitto/perdita totale</b> è {@code quantità × prezzo + incassato - speso}, e con giacenza zero
 * {@code incassato - speso} senza bisogno di un prezzo.
 * <p>
 * È un calcolo <b>solo informativo</b>: non scrive nessun campo di riga, non alimenta RW/RT e non entra in
 * {@code Calcoli_PlusvalenzeNew.OpzioniRicalcolo.Epoca()}. Non è nemmeno il costo di carico LIFO: ignora le regole
 * fiscali (scambi neutri, reward rilevanti, opzioni {@code PDD_*}) e vale per tutti i wallet insieme.
 * È la <b>quarta copia</b> delle regole che dicono che cosa entra e che cosa esce dal costo, dopo il motore delle
 * plusvalenze, {@code Calcoli_RT} e {@code Principale_GiacenzeaData.ElaboraLotti}: chi aggiunge una classificazione
 * nuova deve valutare se serve qui.
 * <p>
 * Due flussi tenuti <b>separati</b>:
 * <ul>
 * <li><b>La quantità</b> è la somma con segno di <i>ogni</i> gamba non FIAT di <i>ogni</i> movimento anteriore alla
 * data, senza filtro di wallet, trasferimenti compresi. Una commissione di rete trattenuta in un giroconto (prelievo
 * di 1,00 e deposito di 0,99) esce quindi dalla quantità da sola, senza incasso.</li>
 * <li><b>Il denaro</b> (speso e incassato) lo decide la classificazione del movimento, gamba per gamba.</li>
 * </ul>
 * Il pool è per <b>simbolo e tipo</b> ({@link #Chiave}), come le pile del motore: righe dello stesso simbolo su reti
 * diverse formano un pool solo, così un bridge non lascia un costo zero sulla rete di arrivo.
 * <p>
 * Regole (decisioni del 30/10/2026, {@code nocommit/Documentazione/Analisi_Costo_Medio_Ponderato.md} §6 bis):
 * <ul>
 * <li>Trasferimenti (categoria {@code TI}, campo 18 {@code PTW}/{@code DTW}, scambio differito compreso): nessun denaro.</li>
 * <li>Movimento a due gambe (acquisto, vendita, scambio, NFT, fallimenti FBL/FGC/FRB): la gamba cripto in uscita
 * incassa {@code v[15]}, quella in entrata spende {@code v[15]}. Le gambe FIAT, NFT e vuote non hanno pool.</li>
 * <li>Una gamba sola in entrata: speso {@code v[15]} se è un acquisto ({@code AC}, {@code DAC}), altrimenti zero
 * (reward, {@code DAI}, {@code DCZ}, {@code DDO}, {@code FRT}). Senza classificazione (campo 18 vuoto) è un deposito
 * non classificato: zero e segnalato.</li>
 * <li>Una gamba sola in uscita: incasso {@code v[15]} per vendita ({@code VC}) e cashout ({@code PCO}); nessun incasso
 * per commissioni, rettifiche ({@code PWN}) e rimborsi di reward. Senza classificazione è un prelievo non
 * classificato: nessun incasso e segnalato. Le commissioni sono quelle di categoria {@code CM} e i {@code PCO} la cui
 * descrizione (campo 5) contiene {@code COMMISSION}, come nel motore.</li>
 * <li>Commissione in FIAT con chiave {@code [43]}: si somma allo speso dell'<b>unico</b> movimento non commissione
 * dello stesso gruppo che porta una gamba cripto in entrata. Con zero o più destinazioni non si attribuisce.</li>
 * <li>Un valore {@code v[15]} vuoto o non numerico, su una gamba che porta denaro, rende il token "senza prezzo":
 * niente costo medio netto né profitto/perdita. {@code 0.00} è un valore valido (importi sotto il centesimo, o
 * prezzo zero confermato con {@code v[32] = "NO"}).</li>
 * </ul>
 */
public final class Principale_GiacenzeaData_CostoNetto {

    private Principale_GiacenzeaData_CostoNetto() {
    }

    /** Ogni quanti movimenti si controlla se l'utente ha annullato: come {@code Principale_GiacenzeaData}. */
    private static final int MOVIMENTI_PER_CONTROLLO_INTERRUZIONE = 2000;

    /** Totali di un token (pool per simbolo e tipo). */
    public static final class Pool {
        public final String Moneta;
        public final String Tipo;
        /** Giacenza globale alla data, con segno, come la somma della tabella senza filtri. */
        public BigDecimal Qta = BigDecimal.ZERO;
        /** Somma di quanto è stato speso per acquistare il token, in euro. */
        public BigDecimal Speso = BigDecimal.ZERO;
        /** Somma di quanto è stato incassato cedendo il token, in euro. */
        public BigDecimal Incassato = BigDecimal.ZERO;
        /** Movimenti con denaro di valore mancante: rendono il costo non calcolabile. */
        public int SenzaPrezzo;
        /** Depositi non classificati, contati come speso zero. */
        public int DepositiNC;
        /** Prelievi non classificati, contati senza incasso. */
        public int PrelieviNC;

        private Pool(String Moneta, String Tipo) {
            this.Moneta = Moneta;
            this.Tipo = Tipo;
        }
    }

    /** Il risultato di una passata: un {@link Pool} per ogni simbolo e tipo toccato. */
    public static final class Risultato {
        private final Map<String, Pool> Pools = new HashMap<>();

        private Pool PoolDi(String Moneta, String Tipo) {
            return Pools.computeIfAbsent(Chiave(Moneta, Tipo), k -> new Pool(Moneta, Tipo));
        }

        /** @return tutti i pool toccati, anche quelli a giacenza zero che nessuna riga della tabella mostra */
        public Collection<Pool> Pools() {
            return Pools.values();
        }

        /** @return il pool del token, {@code null} se nessun movimento lo tocca */
        public Pool Pool(String Moneta, String Tipo) {
            return Pools.get(Chiave(Moneta, Tipo));
        }
    }

    /** @return la chiave del pool, {@code Moneta;Tipo}: niente address e niente rete */
    public static String Chiave(String Moneta, String Tipo) {
        return Moneta + ";" + Tipo;
    }

    /**
     * Le quattro colonne della riga (più la nota) per un token. Ogni voce è {@code null} quando non c'è nulla da
     * mostrare: token FIAT/NFT, costo non calcolabile, giacenza negativa, prezzo assente. Con giacenza nulla il costo
     * medio resta vuoto ma il profitto/perdita c'è (vedi {@link #PerPool}).
     */
    public static final class Riga {
        public Double Speso;
        public Double Incassato;
        public Double CostoMedioNetto;
        public Double ProfittoPerdita;
        /** Che cosa ha reso il numero incerto o assente, vuoto se nulla. */
        public String Note = "";
    }

    /** Una riga del dialogo dei costi netti: un token, con le quantità e le colonne di {@link Riga}. */
    public static final class Voce {
        public final String Moneta;
        public final String Tipo;
        /** Giacenza globale alla data. */
        public final BigDecimal Qta;
        /** Valore alla data della giacenza, {@code null} se un prezzo manca. Zero con giacenza zero. */
        public final BigDecimal Valore;
        public final Riga Colonne;

        private Voce(String Moneta, String Tipo, BigDecimal Qta, BigDecimal Valore, Riga Colonne) {
            this.Moneta = Moneta;
            this.Tipo = Tipo;
            this.Qta = Qta;
            this.Valore = Valore;
            this.Colonne = Colonne;
        }
    }

    /** Il valore della giacenza di un token, somma di tutte le sue righe di rete e indirizzo. */
    public static final class ValoreGiacenza {
        public BigDecimal Valore = BigDecimal.ZERO;
        /** {@code true} se almeno una riga con giacenza non ha prezzo: la somma non è un valore. */
        public boolean SenzaPrezzo;
    }

    /** @return {@code true} se la gamba ha un pool: moneta e tipo presenti, e non FIAT né NFT */
    private static boolean HaPool(String Moneta, String Tipo) {
        return Moneta != null && !Moneta.isBlank() && Tipo != null && !Tipo.isBlank()
                && !Tipo.equalsIgnoreCase("FIAT") && !Tipo.equalsIgnoreCase("NFT");
    }

    private static BigDecimal Numero(String Testo) {
        if (Testo == null || Testo.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(Testo.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Una commissione in FIAT in attesa di essere attribuita all'acquisto del suo gruppo. */
    private record CommissioneFiat(String Chiave, BigDecimal Valore) {
    }

    /**
     * Una passata sui movimenti anteriori alla data. Non dipende dall'ordine dei movimenti: la quantità e i flussi
     * sono somme.
     *
     * @param Movimenti i movimenti da considerare, tutti i wallet
     * @param DataRiferimento istante escluso, come nel ciclo che riempie la tabella
     * @param Interrotto sorgente dello stato di annullamento, {@code null} per non interrompere mai
     * @return i pool; se l'elaborazione è stata interrotta sono <b>incompleti</b> e il chiamante non deve mostrarli
     */
    public static Risultato Calcola(Collection<String[]> Movimenti, long DataRiferimento, BooleanSupplier Interrotto) {
        Risultato Ris = new Risultato();
        //Chiave [43] -> pool delle gambe cripto in entrata dei movimenti non commissione di quel gruppo
        Map<String, List<String[]>> Destinazioni = new HashMap<>();
        List<CommissioneFiat> Commissioni = new ArrayList<>();
        int Esaminati = 0;
        for (String[] v : Movimenti) {
            if (Interrotto != null && Esaminati++ % MOVIMENTI_PER_CONTROLLO_INTERRUZIONE == 0
                    && Interrotto.getAsBoolean()) {
                return Ris;
            }
            if (v == null || v.length < 19 || !(FunzioniDate.ConvertiDatainLong(v[1]) < DataRiferimento)) {
                continue;
            }
            boolean UscitaPool = HaPool(v[8], v[9]);
            boolean EntrataPool = HaPool(v[11], v[12]);

            //La quantita' si somma sempre, trasferimenti compresi: e' la stessa somma della tabella senza filtri
            if (UscitaPool) {
                BigDecimal Q = Numero(v[10]);
                if (Q != null) {
                    Ris.PoolDi(v[8], v[9]).Qta = Ris.PoolDi(v[8], v[9]).Qta.add(Q);
                }
            }
            if (EntrataPool) {
                BigDecimal Q = Numero(v[13]);
                if (Q != null) {
                    Ris.PoolDi(v[11], v[12]).Qta = Ris.PoolDi(v[11], v[12]).Qta.add(Q);
                }
            }

            String[] IDTS = v[0].split("_");
            String Cat = IDTS.length > 4 ? IDTS[4].trim().toUpperCase() : "";
            String C18 = v[18] == null ? "" : v[18].trim().toUpperCase();
            //Trasferimenti fra wallet propri: il denaro non si muove (scambio differito compreso)
            if (Cat.equals("TI") || C18.contains("PTW") || C18.contains("DTW")) {
                continue;
            }
            boolean UscitaPresente = !v[8].isBlank();
            boolean EntrataPresente = !v[11].isBlank();
            BigDecimal Valore = v.length > 15 ? Numero(v[15]) : null;
            if (Valore != null) {
                Valore = Valore.abs();
            }
            String Chiave43 = v.length > CommissioniCollegate.CAMPO && v[CommissioniCollegate.CAMPO] != null
                    ? v[CommissioniCollegate.CAMPO].trim() : "";

            if (UscitaPresente && EntrataPresente) {
                //Acquisto, vendita, scambio, NFT, fallimenti: la gamba cripto che esce incassa, quella che entra spende
                if (UscitaPool) {
                    Incassa(Ris.PoolDi(v[8], v[9]), Valore);
                }
                if (EntrataPool) {
                    Spendi(Ris.PoolDi(v[11], v[12]), Valore);
                }
            } else if (UscitaPresente) {
                if (UscitaPool) {
                    boolean Commissione = Cat.equals("CM")
                            || (C18.contains("PCO") && v[5] != null && v[5].toUpperCase().contains("COMMISSION"));
                    Pool P = Ris.PoolDi(v[8], v[9]);
                    if (Commissione || Cat.equals("RW") || C18.contains("PWN")) {
                        //Esce senza incasso : la quantita' e' gia' scesa
                    } else if (Cat.equals("VC") || C18.contains("PCO")) {
                        Incassa(P, Valore);
                    } else if (C18.isBlank() && (Cat.equals("PC") || Cat.isBlank())) {
                        P.PrelieviNC++;
                    }
                } else if (Cat.equals("CM") && "FIAT".equalsIgnoreCase(v[9]) && !Chiave43.isEmpty()) {
                    Commissioni.add(new CommissioneFiat(Chiave43, Valore));
                }
            } else if (EntrataPresente && EntrataPool) {
                Pool P = Ris.PoolDi(v[11], v[12]);
                if (Cat.equals("AC") || C18.contains("DAC")) {
                    Spendi(P, Valore);
                } else if (Cat.equals("RW") || C18.contains("DAI") || C18.contains("DCZ") || C18.contains("DDO")
                        || C18.contains("FRT")) {
                    //Reward, deposito a costo zero, donazione, rimborso oltre il credito : a zero per scelta
                } else if (C18.isBlank()) {
                    P.DepositiNC++;
                }
            }

            //Una commissione in FIAT si attribuisce all'acquisto del suo gruppo: serve conoscere la gamba che entra
            if (!Chiave43.isEmpty() && !Cat.equals("CM") && EntrataPool) {
                Destinazioni.computeIfAbsent(Chiave43, k -> new ArrayList<>()).add(new String[]{v[11], v[12]});
            }
        }
        for (CommissioneFiat C : Commissioni) {
            List<String[]> Dest = Destinazioni.get(C.Chiave());
            if (C.Valore() != null && Dest != null && Dest.size() == 1) {
                Pool P = Ris.PoolDi(Dest.get(0)[0], Dest.get(0)[1]);
                P.Speso = P.Speso.add(C.Valore());
            }
        }
        return Ris;
    }

    private static void Spendi(Pool P, BigDecimal Valore) {
        if (Valore == null) {
            P.SenzaPrezzo++;
        } else {
            P.Speso = P.Speso.add(Valore);
        }
    }

    private static void Incassa(Pool P, BigDecimal Valore) {
        if (Valore == null) {
            P.SenzaPrezzo++;
        } else {
            P.Incassato = P.Incassato.add(Valore);
        }
    }

    /**
     * Le colonne di un token, con un prezzo unitario noto.
     *
     * @param Ris la passata {@link #Calcola}
     * @param PrezzoUnitario prezzo unitario alla data, {@code null} se il token non ha prezzo: il profitto/perdita
     * resta vuoto con giacenza positiva, nessun prezzo viene cercato qui
     * @return la riga, con voci {@code null} dove non c'è nulla da mostrare
     * @see #PerPool
     */
    public static Riga PerRiga(Risultato Ris, String Moneta, String Tipo, BigDecimal PrezzoUnitario) {
        Pool P = Ris == null || !HaPool(Moneta, Tipo) ? null : Ris.Pool(Moneta, Tipo);
        if (P == null) {
            return new Riga();
        }
        return PerPool(P, PrezzoUnitario == null ? null : P.Qta.multiply(PrezzoUnitario));
    }

    /**
     * Le colonne di un token.
     * <ul>
     * <li>Giacenza positiva: costo medio netto sempre, profitto/perdita solo con il valore della giacenza.</li>
     * <li>Giacenza <b>zero</b>: il profitto/perdita è {@code incassato - speso} e non serve nessun prezzo, perché il
     * token non si possiede più e il risultato è tutto realizzato.</li>
     * <li>Giacenza negativa: nessun numero derivato, solo la nota. Un valore su una quantità che non si possiede
     * sarebbe inventato.</li>
     * <li>Un movimento senza prezzo toglie in ogni caso costo medio e profitto/perdita.</li>
     * </ul>
     *
     * @param ValoreGiacenza valore alla data della giacenza, {@code null} se non noto (ignorato con giacenza zero)
     */
    public static Riga PerPool(Pool P, BigDecimal ValoreGiacenza) {
        Riga R = new Riga();
        R.Speso = P.Speso.setScale(2, RoundingMode.HALF_UP).doubleValue();
        R.Incassato = P.Incassato.setScale(2, RoundingMode.HALF_UP).doubleValue();
        R.Note = Note(P);
        if (P.SenzaPrezzo == 0) {
            if (P.Qta.signum() > 0) {
                R.CostoMedioNetto = P.Speso.subtract(P.Incassato)
                        .divide(P.Qta, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).doubleValue();
                if (ValoreGiacenza != null) {
                    R.ProfittoPerdita = ValoreGiacenza.add(P.Incassato).subtract(P.Speso)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue();
                }
            } else if (P.Qta.signum() == 0) {
                R.ProfittoPerdita = P.Incassato.subtract(P.Speso).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }
        return R;
    }

    /**
     * Somma per token (simbolo e tipo) il valore delle righe della giacenza, che in tabella sono una per rete e per
     * indirizzo: il prezzo si cerca per riga, il pool è uno solo.
     *
     * @param Monete le righe della giacenza globale, quantità con segno. Quelle che non si vogliono valutare (gli SCAM
     * nascosti) si tolgono prima con {@link #RigheDaValutare}, perché non si cerchi un prezzo per loro
     * @param PrezzoUnitario prezzo unitario della riga alla data, {@code null} se manca
     * @param Interrotto sorgente dello stato di annullamento, {@code null} per non interrompere mai
     */
    public static Map<String, ValoreGiacenza> ValoriGiacenze(Collection<Moneta> Monete,
            java.util.function.Function<Moneta, BigDecimal> PrezzoUnitario, BooleanSupplier Interrotto) {
        Map<String, ValoreGiacenza> Valori = new HashMap<>();
        for (Moneta M : RigheDaValutare(Monete, null)) {
            if (Interrotto != null && Interrotto.getAsBoolean()) {
                return Valori;
            }
            ValoreGiacenza V = Valori.computeIfAbsent(Chiave(M.Moneta, M.Tipo), k -> new ValoreGiacenza());
            BigDecimal Prezzo = PrezzoUnitario.apply(M);
            if (Prezzo == null) {
                V.SenzaPrezzo = true;
            } else {
                V.Valore = V.Valore.add(Prezzo.multiply(Numero(M.Qta)));
            }
        }
        return Valori;
    }

    /**
     * @param Scarta simboli da non valutare (per esempio gli SCAM), {@code null} per valutarli tutti: non si chiede
     * nemmeno il predicato per le righe che non hanno bisogno di un prezzo
     * @return le righe che hanno bisogno di un prezzo: giacenza diversa da zero, token cripto (non FIAT né NFT)
     */
    public static List<Moneta> RigheDaValutare(Collection<Moneta> Monete, java.util.function.Predicate<String> Scarta) {
        List<Moneta> Righe = new ArrayList<>();
        for (Moneta M : Monete) {
            BigDecimal Q = Numero(M.Qta);
            if (Q != null && Q.signum() != 0 && HaPool(M.Moneta, M.Tipo) && (Scarta == null || !Scarta.test(M.Moneta))) {
                Righe.add(M);
            }
        }
        return Righe;
    }

    /**
     * Le righe del dialogo: un {@link Voce} per ogni token con qualcosa da dire, giacenza zero compresa, in ordine di
     * simbolo. Un token senza speso, incassato né note (mosso solo da trasferimenti) non compare.
     *
     * @param Valori il valore delle giacenze, {@link #ValoriGiacenze}
     * @param Scarta simboli da non elencare (per esempio gli SCAM), {@code null} per elencarli tutti
     */
    public static List<Voce> Voci(Risultato Ris, Map<String, ValoreGiacenza> Valori,
            java.util.function.Predicate<String> Scarta) {
        List<Voce> Voci = new ArrayList<>();
        for (Pool P : Ris.Pools()) {
            if (!HaPool(P.Moneta, P.Tipo) || (Scarta != null && Scarta.test(P.Moneta))) {
                continue;
            }
            ValoreGiacenza V = Valori.get(Chiave(P.Moneta, P.Tipo));
            BigDecimal Valore = P.Qta.signum() == 0 ? BigDecimal.ZERO
                    : (V == null || V.SenzaPrezzo ? null : V.Valore);
            Riga R = PerPool(P, Valore);
            if (P.Qta.signum() > 0 && Valore == null) {
                R.Note = R.Note.isEmpty() ? "token senza prezzo alla data" : R.Note + ", token senza prezzo alla data";
            }
            if (P.Qta.signum() == 0 && P.Speso.signum() == 0 && P.Incassato.signum() == 0 && R.Note.isEmpty()) {
                continue;
            }
            Voci.add(new Voce(P.Moneta, P.Tipo, P.Qta, Valore, R));
        }
        Voci.sort((a, b) -> {
            int c = a.Moneta.compareToIgnoreCase(b.Moneta);
            return c != 0 ? c : a.Tipo.compareToIgnoreCase(b.Tipo);
        });
        return Voci;
    }

    /** @return il testo che dice che cosa rende incerto il costo del token, vuoto se nulla */
    static String Note(Pool P) {
        List<String> Parti = new ArrayList<>();
        if (P.Qta.signum() < 0) {
            Parti.add("giacenza negativa");
        }
        if (P.SenzaPrezzo > 0) {
            Parti.add(P.SenzaPrezzo + (P.SenzaPrezzo == 1 ? " movimento senza prezzo" : " movimenti senza prezzo"));
        }
        if (P.DepositiNC > 0) {
            Parti.add(P.DepositiNC + (P.DepositiNC == 1 ? " deposito non classificato a zero"
                    : " depositi non classificati a zero"));
        }
        if (P.PrelieviNC > 0) {
            Parti.add(P.PrelieviNC + (P.PrelieviNC == 1 ? " prelievo non classificato senza incasso"
                    : " prelievi non classificati senza incasso"));
        }
        return String.join(", ", Parti);
    }
}
