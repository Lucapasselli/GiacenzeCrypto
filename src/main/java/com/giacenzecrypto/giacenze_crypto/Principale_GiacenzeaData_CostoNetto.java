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
 * Costo medio netto per la tabella "Giacenze a data", stile scheda <i>My Portfolio</i> di CoinGecko:
 * <b>(speso - incassato) / quantità posseduta</b>. Non è una media ponderata del costo (quella toglie dal costo la
 * quantità venduta per il costo medio) ma un prezzo di pareggio, che può diventare zero o negativo.
 * Il <b>profitto/perdita totale</b> è {@code quantità × prezzo + incassato - speso}.
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
    }

    /** Il risultato di una passata: un {@link Pool} per ogni simbolo e tipo toccato. */
    public static final class Risultato {
        private final Map<String, Pool> Pools = new HashMap<>();

        private Pool PoolDi(String Moneta, String Tipo) {
            return Pools.computeIfAbsent(Chiave(Moneta, Tipo), k -> new Pool());
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
     * mostrare: token FIAT/NFT, costo non calcolabile, giacenza nulla o negativa, prezzo assente.
     */
    public static final class Riga {
        public Double Speso;
        public Double Incassato;
        public Double CostoMedioNetto;
        public Double ProfittoPerdita;
        /** Che cosa ha reso il numero incerto o assente, vuoto se nulla. */
        public String Note = "";
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
     * Le colonne di una riga della tabella.
     *
     * @param Ris la passata {@link #Calcola}
     * @param Moneta simbolo della riga
     * @param Tipo tipo della riga
     * @param PrezzoUnitario prezzo unitario alla data, {@code null} se il token non ha prezzo: il profitto/perdita
     * resta vuoto, nessun prezzo viene cercato qui
     * @return la riga, con voci {@code null} dove non c'è nulla da mostrare
     */
    public static Riga PerRiga(Risultato Ris, String Moneta, String Tipo, BigDecimal PrezzoUnitario) {
        Riga R = new Riga();
        if (Ris == null || !HaPool(Moneta, Tipo)) {
            return R;
        }
        Pool P = Ris.Pool(Moneta, Tipo);
        if (P == null) {
            return R;
        }
        R.Speso = P.Speso.setScale(2, RoundingMode.HALF_UP).doubleValue();
        R.Incassato = P.Incassato.setScale(2, RoundingMode.HALF_UP).doubleValue();
        R.Note = Note(P);
        if (P.SenzaPrezzo == 0 && P.Qta.signum() > 0) {
            R.CostoMedioNetto = P.Speso.subtract(P.Incassato)
                    .divide(P.Qta, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).doubleValue();
            if (PrezzoUnitario != null) {
                R.ProfittoPerdita = P.Qta.multiply(PrezzoUnitario).add(P.Incassato).subtract(P.Speso)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }
        return R;
    }

    /** @return il testo che dice che cosa rende incerto il costo del token, vuoto se nulla */
    static String Note(Pool P) {
        List<String> Parti = new ArrayList<>();
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
