package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * La media giornaliera dei rapporti 99 nel tab "ISEE": i prezzi di fine giornata con il percorso fiscale (lo stesso
 * del valore al 31/12), scaricati in un lotto solo prima del calcolo, e i giorni rimasti senza prezzo da sistemare con
 * prezzi personalizzati. Nessun campo Swing: la parte grafica è in {@link GUI_ISEE_Pannello} e
 * {@link GUI_PrezziMancantiISEE}. Decisioni nel §26 di {@code Analisi_ISEE_DSU.md}.
 *
 * <p>Il prezzo del giorno D è quello delle 00:00 del giorno D+1 ({@link Calcoli_ISEE#istanteFineGiornata}). Un prezzo
 * scritto qui è un prezzo personalizzato a quell'istante, gruppo {@code TUTTI}: lo vedono tutte le valutazioni a
 * quell'istante, compreso il quadro W/RW quando il giorno è il 31/12.</p>
 */
public final class Principale_ISEE_Media {

    private Principale_ISEE_Media() {
    }

    /** Etichetta del pre-scarico nel log. */
    private static final String ORIGINE = "ISEE media";

    /** Fonte dei prezzi scritti dalla tabella dei prezzi mancanti. */
    static final String FONTE_PERSONALIZZATO = "Personalizzato";

    /**
     * Secondi per quotazione della stima mostrata prima dello scaricamento, misurati il 2026-11-05 su un mese del
     * dataset reale: 730 richieste agli exchange in 107 s, 3.460 coppie DefiLlama in 6,8 s.
     */
    private static final double SECONDI_PER_RICHIESTA_EXCHANGE = 0.15;
    private static final double SECONDI_PER_COPPIA_DEFILLAMA = 0.002;

    /** Quello che manca per la media: le richieste agli exchange e le coppie DefiLlama, dopo i filtri locali. */
    public static final class Piano {
        final List<Prezzi.RichiestaPrezzo> exchange;
        final List<PrezziDefiLlama.Coppia> defillama;
        /** Giorni-moneta da valorizzare e monete distinte, per il messaggio. */
        public final int giorni;
        public final int monete;

        Piano(List<Prezzi.RichiestaPrezzo> exchange, List<PrezziDefiLlama.Coppia> defillama, int giorni, int monete) {
            this.exchange = exchange;
            this.defillama = defillama;
            this.giorni = giorni;
            this.monete = monete;
        }

        /** Quotazioni da scaricare. */
        public int quotazioni() {
            return exchange.size() + defillama.size();
        }

        /** Stima indicativa della durata dello scaricamento, in secondi. */
        public long secondiStimati() {
            return Math.round(exchange.size() * SECONDI_PER_RICHIESTA_EXCHANGE + defillama.size() * SECONDI_PER_COPPIA_DEFILLAMA);
        }
    }

    /**
     * Raccoglie i giorni con giacenza dei rapporti 99 e conta, senza rete, quante quotazioni mancano. I token con
     * address senza alias vanno a DefiLlama, gli altri (e gli alias, col simbolo dell'alias) agli exchange, come li
     * cercherà la valorizzazione. Da chiamare in un thread di background.
     */
    public static Piano Prepara(Calcoli_ISEE.Risultato ris, Download progress) {
        if (progress != null) {
            progress.SetLabel("Controllo dei prezzi già presenti...");
        }
        Set<Prezzi.SimboloIstante> simboli = new LinkedHashSet<>();
        Set<PrezziDefiLlama.Coppia> coppie = new LinkedHashSet<>();
        Set<String> monete = new TreeSet<>();
        int giorni = 0;
        Set<String> giorniMoneta = new java.util.HashSet<>();
        for (Calcoli_ISEE.GiornoConGiacenza g : Calcoli_ISEE.giorniConGiacenza(ris)) {
            Moneta m = g.moneta();
            String tipo = m.Tipo == null ? "" : m.Tipo.trim();
            String chiave = Calcoli_ISEE.PrezzoMancante.chiaveMoneta(m);
            if (giorniMoneta.add(chiave + "|" + g.giorno())) {
                giorni++;
            }
            monete.add(chiave);
            if (tipo.equalsIgnoreCase("FIAT") || tipo.equalsIgnoreCase("NFT")) {
                continue;
            }
            long istante = Calcoli_ISEE.istanteFineGiornata(g.giorno());
            String rete = m.Rete == null ? "" : m.Rete.trim();
            if (!rete.isEmpty() && Funzioni_WalletDeFi.isValidAddress(m.MonetaAddress, rete)) {
                String alias = AliasPrezziToken.Alias(m.MonetaAddress, rete, istante);
                if (alias == null) {
                    coppie.add(new PrezziDefiLlama.Coppia(m.MonetaAddress, rete, istante));
                } else {
                    simboli.add(new Prezzi.SimboloIstante(alias, istante));
                }
            } else {
                simboli.add(new Prezzi.SimboloIstante(m.Moneta, istante));
            }
        }
        List<PrezziDefiLlama.Coppia> defillama = PrezziDefiLlama.DaChiedere(coppie);
        List<Prezzi.RichiestaPrezzo> exchange = Prezzi.RichiesteDaScaricarePerSimboli(simboli, progress, ORIGINE, "");
        return new Piano(exchange, defillama, giorni, monete.size());
    }

    /**
     * Scarica le quotazioni del piano, interrompibile: quello che è già arrivato resta in cache, e la volta dopo
     * {@link #Prepara} non lo chiede più.
     *
     * @return {@code false} se l'utente ha interrotto
     */
    public static boolean Scarica(Piano piano, Download progress) {
        Interruzione.Apri();
        try {
            if (!piano.defillama.isEmpty()) {
                if (progress != null) {
                    progress.SetLabel("Prezzi dei token per address: " + piano.defillama.size() + " quotazioni...");
                }
                PrezziDefiLlama.ScaricaCoppie(piano.defillama, progress);
            }
            if (interrotto(progress)) {
                return false;
            }
            Prezzi.ScaricaRichiestePerSimboli(piano.exchange, progress, ORIGINE);
            return !interrotto(progress);
        } finally {
            Interruzione.Chiudi();
        }
    }

    private static boolean interrotto(Download progress) {
        return Interruzione.Richiesta() || (progress != null && progress.FineThread());
    }

    /** Calcola la media giornaliera sul risultato con i prezzi fiscali (dopo {@link #Scarica} vengono dalla cache). */
    public static void CalcolaMedia(Calcoli_ISEE.Risultato ris, Download progress) {
        if (progress != null) {
            progress.SetLabel("Calcolo della media giornaliera del " + ris.anno + "...");
        }
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, Calcoli_ISEE.VALORE_MERCATO);
    }

    /** Il testo della conferma prima di scaricare, con la stima. */
    public static String DescriviPiano(Piano p) {
        long s = p.secondiStimati();
        String durata = s < 60 ? "meno di un minuto" : s < 120 ? "circa un minuto" : "circa " + Math.round(s / 60.0) + " minuti";
        return "La media valorizza " + p.giorni + " giorni-moneta (" + p.monete + " monete).<br>"
                + "Mancano " + p.quotazioni() + " quotazioni da scaricare (" + p.exchange.size() + " dagli exchange, "
                + p.defillama.size() + " da DefiLlama): " + durata + ", stima indicativa.<br><br>"
                + "Lo scaricamento si può interrompere: i prezzi già arrivati restano e la volta dopo si riparte da lì.";
    }

    // --- prezzi mancanti -------------------------------------------------------------

    /** Giorni consecutivi senza prezzo della stessa moneta. */
    public static final class Tratto {
        public final Moneta moneta;
        public final List<Calcoli_ISEE.PrezzoMancante> giorni = new ArrayList<>();

        Tratto(Moneta moneta) {
            this.moneta = moneta;
        }

        public LocalDate dal() {
            return giorni.get(0).giorno;
        }

        public LocalDate al() {
            return giorni.get(giorni.size() - 1).giorno;
        }

        public Set<String> gruppi() {
            Set<String> out = new TreeSet<>();
            for (Calcoli_ISEE.PrezzoMancante g : giorni) {
                out.addAll(g.gruppi);
            }
            return out;
        }

        public BigDecimal qtaMin() {
            BigDecimal min = null;
            for (Calcoli_ISEE.PrezzoMancante g : giorni) {
                min = min == null || g.qta.compareTo(min) < 0 ? g.qta : min;
            }
            return min;
        }

        public BigDecimal qtaMax() {
            BigDecimal max = null;
            for (Calcoli_ISEE.PrezzoMancante g : giorni) {
                max = max == null || g.qta.compareTo(max) > 0 ? g.qta : max;
            }
            return max;
        }
    }

    /** I giorni senza prezzo dell'ultima media, raggruppati per moneta in tratti di giorni consecutivi. */
    public static List<Tratto> Tratti(Calcoli_ISEE.Risultato ris) {
        List<Tratto> out = new ArrayList<>();
        Tratto corrente = null;
        for (Calcoli_ISEE.PrezzoMancante pm : ris.prezziMancanti) {
            boolean continua = corrente != null
                    && corrente.giorni.get(0).chiaveMoneta().equals(pm.chiaveMoneta())
                    && corrente.al().plusDays(1).equals(pm.giorno);
            if (!continua) {
                corrente = new Tratto(pm.moneta);
                out.add(corrente);
            }
            corrente.giorni.add(pm);
        }
        return out;
    }

    /**
     * Scrive lo stesso prezzo unitario in euro per ognuno dei giorni, come prezzo personalizzato all'istante di fine
     * giornata (gruppo {@code TUTTI}), in una transazione sola: o tutti o nessuno. La chiave è quella del movimento
     * (simbolo, oppure address e rete), prima degli alias, come la cercano i prezzi.
     *
     * @return {@code true} se tutti i prezzi sono stati scritti
     */
    public static boolean ScriviPrezzi(List<Calcoli_ISEE.PrezzoMancante> giorni, BigDecimal prezzoUnitario, Download progress) {
        if (giorni == null || giorni.isEmpty()) {
            return true;
        }
        Connection c = DatabaseH2.connectionPersonale;
        try {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                if (progress != null) {
                    progress.SetMassimo(giorni.size());
                    progress.SetAvanzamento(0);
                }
                int fatti = 0;
                for (Calcoli_ISEE.PrezzoMancante g : giorni) {
                    long istante = Calcoli_ISEE.istanteFineGiornata(g.giorno);
                    Moneta m = g.moneta;
                    if (!DatabaseH2.InserisciPrezzoPresonalizzato(istante, FONTE_PERSONALIZZATO, m.Moneta,
                            prezzoUnitario.toPlainString(), m.Rete, m.MonetaAddress, "TUTTI", istante)) {
                        c.rollback();
                        return false;
                    }
                    fatti++;
                    if (progress != null && fatti % 200 == 0) {
                        progress.SetAvanzamento(fatti);
                    }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        } catch (SQLException | RuntimeException ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    /** Un prezzo unitario scritto dall'utente: numero non negativo, virgola o punto come separatore. {@code null} se non valido. */
    public static BigDecimal LeggiPrezzo(String testo) {
        if (testo == null) {
            return null;
        }
        String t = testo.trim().replace(" ", "");
        // Con virgola e punto insieme il separatore dei decimali è l'ultimo dei due (1.234,56 o 1,234.56).
        int virgola = t.lastIndexOf(',');
        int punto = t.lastIndexOf('.');
        if (virgola > punto) {
            t = t.replace(".", "").replace(',', '.');
        } else if (virgola >= 0) {
            t = t.replace(",", "");
        }
        try {
            BigDecimal v = new BigDecimal(t);
            return v.signum() < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
