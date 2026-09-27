package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Fotografia delle giacenze cripto di ogni gruppo wallet agli estremi di uno o più tratti dell'anno: la
 * giacenza a inizio del primo giorno e a fine dell'ultimo, prezzate a quegli istanti. È l'<b>unica</b>
 * routine che produce i righi "Giacenza Inizio → Giacenza Fine" del quadro W/RW, usata sia dalla Rilevanza A
 * ({@link Calcoli_RW#ChiudiRWGiacenzeFinali}) sia dalla sostituzione per i gruppi che pagano il bollo
 * ({@link Funzioni#RW_GiacenzeInizioFineAnno}). Fino al 2026-09-26 erano due implementazioni distinte che
 * divergevano (NFT, euro, giacenze negative, valore di apertura): vedi
 * {@code nocommit/Documentazione/Analisi_QuadroRW_Crypto_Periodi.md} §8.
 *
 * <p>Regole, decise con l'utente:</p>
 * <ul>
 *   <li><b>Tutto tranne FIAT</b>: cripto e NFT entrano, le valute no.</li>
 *   <li><b>Gruppo aperto dentro il tratto</b>: l'inizio è il suo primo movimento, per tutte le monete. Solo
 *       la moneta <b>entrata</b> con quel movimento (se non è FIAT) parte con una giacenza, valorizzata al
 *       valore del movimento {@code v[15]}; se quel valore è zero o manca si ripiega sul prezzo di mercato in
 *       quell'istante (il valore del movimento è zero per reward e airdrop non rilevanti). La moneta in
 *       uscita non è mai una giacenza iniziale.</li>
 *   <li><b>Inizio di un tratto per un gruppo che esiste già</b> (1° gennaio o confine di periodo): contano
 *       solo la giacenza e il prezzo alle 00:00, nessun movimento di quel giorno — salvo una modalità di
 *       calcolo impostata dall'utente su quel confine.</li>
 *   <li><b>Modalità sui confini</b> (dai periodi CRYPTO, vedi {@link Calcoli_RW_PeriodiCrypto}): come sul
 *       FIAT, a livello di gruppo e in ordine di movimento, esclusi i trasferimenti interni ({@code TI}). In
 *       apertura "primo apporto" / "somma degli apporti" della giornata si aggiungono al residuo; in chiusura
 *       "ultima uscita" / "somma delle uscite" si riaggiungono alla giacenza di fine giornata. Il residuo è
 *       prezzato all'istante del confine, apporti e uscite al valore del loro movimento (mercato se zero).</li>
 *   <li><b>Giacenza negativa</b> (movimento mancante): valore zero, mai valore assoluto, e rigo marcato
 *       {@link #ERRORE_GIACENZA_NEGATIVA} — la stessa dicitura che scrive il motore LIFO.</li>
 * </ul>
 *
 * <p>La chiave della moneta è {@code Moneta;Tipo}: lo stesso simbolo su due reti nello stesso gruppo viene
 * sommato e prezzato con address e rete del primo movimento incontrato. Limite noto, da affrontare a parte.</p>
 *
 * <p>Il risultato <b>non</b> passa da {@link Calcoli_RW#SistemaErroriInListe}: lo fa il chiamante, una volta
 * sola (applicarlo due volte raddoppia il prefisso HTML della colonna errori).</p>
 */
public class Calcoli_RW_Giacenze {

    private Calcoli_RW_Giacenze() {
    }

    /** Dicitura di errore di una giacenza negativa, identica a quella di {@code Calcoli_RW.ChiudiRWFR}. */
    public static final String ERRORE_GIACENZA_NEGATIVA = "Errore (Giacenza Negativa)";

    /** Valore di una giacenza a zero o non conteggiata: con le 4 cifre non fa scattare l'errore "prezzo mancante". */
    static final String VALORE_ZERO = "0.0000";

    /** Controvalore in euro di una quantità di moneta a un istante; {@code null} se il prezzo non si trova. */
    @FunctionalInterface
    public interface PrezzoGiacenza {
        String prezzo(Moneta m, long istante);
    }

    /** Prezzo di mercato dalla cache/dalle fonti, come l'ha sempre chiesto la sostituzione per i gruppi col bollo. */
    public static final PrezzoGiacenza PREZZO_MERCATO =
            (m, istante) -> Prezzi.DammiPrezzoTransazione(m, null, istante, null, false, 15, m.Rete, "");

    /** Giacenze di inizio e fine anno di ogni gruppo wallet, a prezzo di mercato (un rigo per moneta, anno intero). */
    public static Map<String, List<String[]>> GiacenzeInizioFineAnno(String Anno) {
        return GiacenzeInizioFineAnno(Anno, PREZZO_MERCATO);
    }

    /** Come {@link #GiacenzeInizioFineAnno(String)}, con la fonte dei prezzi iniettata. */
    public static Map<String, List<String[]>> GiacenzeInizioFineAnno(String Anno, PrezzoGiacenza prezzo) {
        return GiacenzeFinestra(Anno, Anno + "-01-01", Anno + "-12-31", prezzo);
    }

    /**
     * Giacenze di ogni gruppo wallet agli estremi della finestra {@code [DataInizio, DataFine]} (ISO, giorni
     * inclusi), la stessa per tutti i gruppi e senza modalità: un solo tratto.
     */
    public static Map<String, List<String[]>> GiacenzeFinestra(String Anno, String DataInizio, String DataFine,
            PrezzoGiacenza prezzo) {
        String[] unico = new String[Calcoli_RW_PeriodiCrypto.TC_COLONNE];
        java.util.Arrays.fill(unico, "");
        unico[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO] = DataInizio;
        unico[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE] = DataFine;
        List<String[]> tratti = List.<String[]>of(unico);
        return GiacenzePerTratti(Anno, g -> tratti, prezzo);
    }

    /** Giacenze per tratto CRYPTO di ogni gruppo ({@link Calcoli_RW_PeriodiCrypto#trattiCrypto}), a prezzo di mercato. */
    public static Map<String, List<String[]>> GiacenzePerTratti(String Anno) {
        return GiacenzePerTratti(Anno, g -> Calcoli_RW_PeriodiCrypto.trattiCrypto(g, Anno), PREZZO_MERCATO);
    }

    /** Un lato non FIAT di un movimento, già attribuito al suo gruppo. */
    private static final class Gamba {
        final String giorno;
        final String chiave;
        final Moneta moneta;
        final String qta;
        final boolean entrata;
        final boolean trasferimentoInterno;
        final String valore;
        final String dataOra;

        Gamba(String[] v, Moneta m, boolean entrata) {
            this.giorno = v[1].substring(0, 10);
            this.chiave = chiave(m);
            this.moneta = m;
            this.qta = m.Qta;
            this.entrata = entrata;
            String[] id = v[0].split("_");
            this.trasferimentoInterno = "TI".equals(id[id.length - 1]);
            this.valore = v[15];
            this.dataOra = v[1];
        }
    }

    /** Il primo movimento del gruppo: data, valore e la moneta entrata (null se era FIAT o assente). */
    private static final class Apertura {
        final String id;
        final String data;
        final String valore;
        final String chiaveMonetaEntrata;
        final String qtaEntrata;

        Apertura(String[] v, Moneta entrata) {
            id = v[0];
            data = v[1];
            valore = v[15];
            chiaveMonetaEntrata = entrata == null ? null : chiave(entrata);
            qtaEntrata = v[13];
        }
    }

    /** Tutto ciò che serve di un gruppo: le monete (nell'ordine della chiave), le gambe in ordine di ID, l'apertura. */
    private static final class Gruppo {
        final Map<String, Moneta> monete = new TreeMap<>();
        final List<Gamba> gambe = new ArrayList<>();
        Apertura apertura;
    }

    /**
     * Giacenze di ogni gruppo wallet per ciascuno dei suoi tratti: un rigo per moneta e per tratto con
     * giacenza diversa da zero in almeno uno dei due estremi, nel formato {@code String[17]} delle liste del
     * quadro RW. Il tratto di un rigo si riconosce dalle sue date ({@code [4]}/{@code [9]}).
     *
     * @param Anno            anno RW scritto in colonna 0
     * @param trattiDelGruppo gruppo → tratti contigui ({@link Calcoli_RW_PeriodiCrypto#TC_COLONNE} colonne)
     * @param prezzo          fonte dei prezzi (in produzione {@link #PREZZO_MERCATO})
     * @return gruppo wallet → righi (ordine gruppi case-insensitive, come le altre mappe RW). Ogni gruppo con
     *         almeno un movimento compare, anche senza righi: {@code RW_CalcolaRW} legge la lista per ogni
     *         gruppo del motore, che comprende anche quelli con movimenti solo dopo l'anno
     */
    public static Map<String, List<String[]>> GiacenzePerTratti(String Anno,
            Function<String, List<String[]>> trattiDelGruppo, PrezzoGiacenza prezzo) {
        // MappaCryptoWallet è ordinata per ID, che comincia con yyyyMMddHHmm: il primo movimento
        // incontrato per un gruppo è il suo primo movimento in assoluto, e le gambe sono cronologiche.
        Map<String, Gruppo> gruppi = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            Gruppo g = gruppi.computeIfAbsent(DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true), k -> new Gruppo());
            Moneta[] monete = Funzioni.RitornaMoneteDaID(v[0]);
            if (g.apertura == null) {
                g.apertura = new Apertura(v, contaQuantita(monete[1]) ? monete[1] : null);
            }
            for (int a = 0; a < 2; a++) {
                Moneta m = monete[a];
                if (contaQuantita(m)) {
                    g.monete.putIfAbsent(chiave(m), m);
                    g.gambe.add(new Gamba(v, m, a == 1));
                }
            }
        }

        Map<String, List<String[]>> risultato = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Gruppo> e : gruppi.entrySet()) {
            List<String[]> lista = new ArrayList<>();
            List<String[]> tratti = trattiDelGruppo.apply(e.getKey());
            for (int i = 0; i < tratti.size(); i++) {
                boolean ultimo = i == tratti.size() - 1;
                boolean primo = tratti.get(i)[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO].endsWith("-01-01");
                righiDelTratto(Anno, e.getKey(), e.getValue(), tratti.get(i), primo, ultimo, prezzo, lista);
            }
            risultato.put(e.getKey(), lista);
        }
        return risultato;
    }

    private static void righiDelTratto(String Anno, String gruppo, Gruppo g, String[] tratto,
            boolean primoDellAnno, boolean ultimoDellAnno, PrezzoGiacenza prezzo, List<String[]> lista) {
        String di = tratto[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO];
        String df = tratto[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE];
        String giornoApertura = g.apertura.data.substring(0, 10);
        if (giornoApertura.compareTo(df) > 0) {
            return; // il gruppo non esisteva ancora
        }
        boolean apertoNelTratto = giornoApertura.compareTo(di) >= 0;
        String dataInizioRigo = apertoNelTratto ? g.apertura.data : di + " 00:00";
        String giorni = String.valueOf(FunzioniDate.DifferenzaDate(dataInizioRigo.substring(0, 10), df) + 1);
        long istanteInizio = FunzioniDate.ConvertiDatainLongMinuto(di + " 00:00");
        long istanteFine = FunzioniDate.ConvertiDatainLongMinuto(LocalDate.parse(df).plusDays(1) + " 00:00");

        // Quantità: la prima resta nella forma scritta sul movimento, le successive sono somme normalizzate,
        // come faceva la routine precedente (il golden master confronta le stringhe).
        Map<String, String> qIni = new HashMap<>();
        Map<String, String> qFin = new HashMap<>();
        for (Gamba gm : g.gambe) {
            if (gm.giorno.compareTo(df) > 0) {
                continue;
            }
            if (gm.giorno.compareTo(di) < 0) {
                qIni.merge(gm.chiave, gm.qta, Calcoli_RW_Giacenze::accumula);
            }
            qFin.merge(gm.chiave, gm.qta, Calcoli_RW_Giacenze::accumula);
        }
        if (apertoNelTratto && g.apertura.chiaveMonetaEntrata != null) {
            // La giacenza iniziale di un gruppo aperto nel tratto è ciò che è entrato col primo movimento.
            qIni.put(g.apertura.chiaveMonetaEntrata, g.apertura.qtaEntrata);
        }

        // Modalità sui confini: apporti del primo giorno / uscite dell'ultimo, per moneta.
        Map<String, BigDecimal[]> extraIni = new HashMap<>();
        Map<String, BigDecimal[]> extraFin = new HashMap<>();
        String modIni = tratto[Calcoli_RW_PeriodiCrypto.TC_MOD_INIZIALE];
        String modFin = tratto[Calcoli_RW_PeriodiCrypto.TC_MOD_FINALE];
        if (!apertoNelTratto && !Principale_GruppiWalletRW.soloResiduo(modIni)) {
            boolean soloPrimo = Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO.equals(modIni.trim());
            for (Gamba gm : g.gambe) {
                if (gm.giorno.equals(di) && gm.entrata && !gm.trasferimentoInterno) {
                    aggiungiExtra(extraIni, gm, prezzo);
                    if (soloPrimo) {
                        break;
                    }
                }
            }
        }
        if (!Principale_GruppiWalletRW.soloResiduo(modFin)) {
            boolean soloUltima = Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA.equals(modFin.trim());
            Gamba ultima = null;
            for (Gamba gm : g.gambe) {
                if (gm.giorno.equals(df) && !gm.entrata && !gm.trasferimentoInterno) {
                    if (soloUltima) {
                        ultima = gm;
                    } else {
                        aggiungiExtra(extraFin, gm, prezzo);
                    }
                }
            }
            if (ultima != null) {
                aggiungiExtra(extraFin, ultima, prezzo);
            }
        }

        String etichettaInizio = primoDellAnno ? "Giacenza Inizio Anno" : "Giacenza Inizio Periodo";
        String etichettaFine = ultimoDellAnno ? "Giacenza Fine Anno" : "Giacenza Fine Periodo";
        String causaleFine = ultimoDellAnno ? "Fine Anno" : "Fine Periodo";

        for (Map.Entry<String, Moneta> mon : g.monete.entrySet()) {
            String chiave = mon.getKey();
            if (!qIni.containsKey(chiave) && !qFin.containsKey(chiave)) {
                continue;
            }
            Moneta inizio = copia(mon.getValue(), qIni.getOrDefault(chiave, "0"));
            Moneta fine = copia(mon.getValue(), qFin.getOrDefault(chiave, "0"));
            BigDecimal[] xIni = extraIni.get(chiave);
            BigDecimal[] xFin = extraFin.get(chiave);
            int segnoIni = new BigDecimal(inizio.Qta).signum();
            int segnoFin = new BigDecimal(fine.Qta).signum();
            if (segnoIni == 0 && segnoFin == 0 && xIni == null && xFin == null) {
                continue;
            }
            boolean monetaDiApertura = apertoNelTratto && chiave.equals(g.apertura.chiaveMonetaEntrata);

            String valoreIni;
            if (segnoIni < 0) {
                valoreIni = VALORE_ZERO;
            } else if (monetaDiApertura && segnoIni > 0) {
                valoreIni = valoreApertura(inizio, g.apertura, prezzo);
            } else {
                String residuo = segnoIni == 0 ? VALORE_ZERO : prezzo.prezzo(inizio,
                        apertoNelTratto ? FunzioniDate.ConvertiDatainLongMinuto(dataInizioRigo) : istanteInizio);
                valoreIni = conExtra(inizio, residuo, xIni);
            }
            String valoreFin = segnoFin < 0 ? VALORE_ZERO
                    : conExtra(fine, segnoFin == 0 ? VALORE_ZERO : prezzo.prezzo(fine, istanteFine), xFin);

            String[] xlista = new String[17];
            xlista[0] = Anno;                                                   //Anno RW
            xlista[1] = gruppo;                                                 //Gruppo Wallet Inizio
            xlista[2] = inizio.Moneta;                                          //Moneta Inizio
            xlista[3] = inizio.Qta;                                             //Qta Inizio
            xlista[4] = dataInizioRigo;                                         //Data Inizio
            xlista[5] = valoreIni;                                              //Prezzo Inizio
            xlista[6] = gruppo;                                                 //GruppoWallet Fine
            xlista[7] = fine.Moneta;                                            //Moneta Fine
            xlista[8] = fine.Qta;                                               //Qta Fine
            xlista[9] = df + " 23:59";                                          //Data Fine
            xlista[10] = valoreFin;                                             //Prezzo Fine
            xlista[11] = giorni;                                                //Giorni di Detenzione
            xlista[12] = monetaDiApertura ? "Apertura Wallet/" + causaleFine : causaleFine; //Causale
            xlista[13] = monetaDiApertura ? g.apertura.id : etichettaInizio;   //ID Movimento Apertura
            xlista[14] = etichettaFine;                                         //ID Movimento Chiusura
            xlista[15] = (segnoIni < 0 || segnoFin < 0) ? ERRORE_GIACENZA_NEGATIVA : ""; //Tipo Errore
            xlista[16] = "";                                                    //Lista ID coinvolti
            lista.add(xlista);
        }
    }

    /**
     * Somma a una moneta la quantità e il valore di un apporto/uscita del giorno di confine (quantità sempre
     * positiva: un'uscita si riaggiunge). [0] = quantità, [1] = valore, [2] = 1 se un valore manca.
     */
    private static void aggiungiExtra(Map<String, BigDecimal[]> extra, Gamba gm, PrezzoGiacenza prezzo) {
        BigDecimal qta = new BigDecimal(gm.qta).abs();
        String valore = valoreMovimento(gm.valore, copia(gm.moneta, qta.toPlainString()),
                gm.dataOra, prezzo);
        BigDecimal[] x = extra.computeIfAbsent(gm.chiave, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        x[0] = x[0].add(qta);
        if (valore == null) {
            x[2] = BigDecimal.ONE;
        } else {
            x[1] = x[1].add(new BigDecimal(valore));
        }
    }

    /**
     * Valore di un estremo con apporti/uscite del giorno di confine: il residuo (già prezzato) più il valore
     * degli apporti, e la quantità dell'estremo cresce di conseguenza. Senza extra restituisce il residuo
     * tale e quale; un prezzo mancante da una parte o dall'altra dà {@code null} (errore "prezzo mancante").
     */
    private static String conExtra(Moneta estremo, String residuo, BigDecimal[] extra) {
        if (extra == null) {
            return residuo;
        }
        estremo.Qta = new BigDecimal(estremo.Qta).add(extra[0]).stripTrailingZeros().toPlainString();
        if (residuo == null || extra[2].signum() != 0) {
            return null;
        }
        return new BigDecimal(residuo).add(extra[1]).stripTrailingZeros().toPlainString();
    }

    /** Valore della moneta che ha aperto il gruppo: quello del movimento, o il mercato se è zero/assente. */
    private static String valoreApertura(Moneta m, Apertura apertura, PrezzoGiacenza prezzo) {
        return valoreMovimento(apertura.valore, m, apertura.data, prezzo);
    }

    /** Il valore del movimento se positivo, altrimenti il prezzo di mercato della moneta all'istante del movimento. */
    private static String valoreMovimento(String v, Moneta m, String dataOra, PrezzoGiacenza prezzo) {
        if (v != null && !v.isBlank()) {
            try {
                if (new BigDecimal(v.trim()).signum() > 0) {
                    return v.trim();
                }
            } catch (NumberFormatException ignorato) {
                // valore non numerico: si ripiega sul prezzo di mercato
            }
        }
        return prezzo.prezzo(m, FunzioniDate.ConvertiDatainLongMinuto(dataOra));
    }

    /** La moneta pesa sulle giacenze: c'è, ha una quantità e non è FIAT (NFT inclusi). */
    private static boolean contaQuantita(Moneta m) {
        return m != null && m.Moneta != null && !m.Moneta.isBlank()
                && m.Qta != null && !m.Qta.isBlank()
                && !"FIAT".equalsIgnoreCase(m.Tipo);
    }

    private static Moneta copia(Moneta tipo, String qta) {
        Moneta m = new Moneta();
        m.InserisciValori(tipo.Moneta, qta, tipo.MonetaAddress, tipo.Tipo);
        m.Rete = tipo.Rete;
        return m;
    }

    private static String chiave(Moneta m) {
        return m.Moneta + ";" + m.Tipo;
    }

    private static String accumula(String attuale, String q) {
        return new BigDecimal(attuale).add(new BigDecimal(q)).stripTrailingZeros().toPlainString();
    }
}
