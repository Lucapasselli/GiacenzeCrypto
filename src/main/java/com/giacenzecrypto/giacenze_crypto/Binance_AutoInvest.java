package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sistema i movimenti dei piani Auto-Invest di Binance usando i tre file di dettaglio scaricabili dalla sezione
 * "Auto-Invest - History" dell'exchange: <b>Subscription</b> ({@code Time, Holding Coin, Amount Per Period, Trading
 * Fee, Units, From, Status}), <b>Rebalancing</b> ({@code Time, Index Name, Rebalancing Coin, Rebalancing Amount,
 * Total Rebalancing Fee}) e <b>Redemption</b> ({@code Time, Index Name, Coin, Redemption Amount, Redemption Fee}).
 *
 * <p>Il CSV delle transazioni da solo non basta, e l'abbinamento automatico dello scambio differito (25 minuti,
 * valore entro il 10%, una uscita con una entrata) lascia da classificare la maggior parte delle righe:
 * <ul>
 *   <li>un piano su più monete spende con <b>una</b> uscita e riceve <b>più</b> entrate;</li>
 *   <li>un piano <b>indice</b> (es. "Binance CMC Top 10") ha solo l'uscita: le monete comprate restano nel conto
 *       Auto-Invest, vengono ribilanciate ogni mese senza che il CSV lo registri, e ricompaiono solo al riscatto
 *       ({@code Index-Auto-Investment - Redeem Positions});</li>
 *   <li>fino a metà 2023 l'importo di un piano prelevato da Flexible Savings non compare affatto: c'è solo l'entrata;</li>
 *   <li>l'entrata può arrivare ore dopo l'uscita.</li>
 * </ul>
 * Il file Subscription spiega ogni uscita al centesimo (raggruppato per esecuzione e moneta spesa) e ogni entrata
 * alla quantità esatta; il Rebalancing dà le quantità ribilanciate ma <b>non il verso</b>, che si ricostruisce da
 * sottoscrizioni e riscatto ({@link #SegniRibilanciamenti}).
 *
 * <p>La forma è quella del Dual Investment a moneta uguale ({@link Binance_DualInvestment}): <b>i movimenti
 * importati cambiano solo nei campi 5, 18 e 20</b>, quantità e valore restano quelli del CSV (così il reimport li
 * riconosce), e tutto il resto avviene su un sotto-wallet {@value #WALLET_AUTO_INVEST} dello stesso exchange, quindi
 * dello stesso gruppo wallet:
 * <ul>
 *   <li>l'uscita diventa un giroconto verso il sotto-wallet (PTW), con la sua gamba speculare in entrata;</li>
 *   <li>ogni acquisto del file è uno scambio generato sul sotto-wallet (moneta spesa &rarr; moneta comprata);</li>
 *   <li>l'entrata nello Spot, per i piani normali, diventa un giroconto dal sotto-wallet (DTW), con la sua gamba
 *       speculare in uscita; per un piano indice le monete restano sul sotto-wallet;</li>
 *   <li>ogni ribilanciamento è un gruppo di scambi diretti cripto-cripto sul sotto-wallet, con le quantità vendute
 *       ripartite fra le monete comprate in proporzione al valore (scelta dell'utente, 2026-10-18: il file non mostra
 *       alcun passaggio intermedio in USDT, e passare da USDT creerebbe permute in più);</li>
 *   <li>il riscatto diventa un giroconto dal sotto-wallet, con la sua gamba speculare in uscita.</li>
 * </ul>
 * Ogni esecuzione, ogni ribilanciamento e ogni riscatto è un gruppo {@code [20]} simmetrico: annullarne un
 * movimento riporta gli importati come erano e toglie i generati, e ripassando i file si rifà.
 *
 * <p>Uno scambio differito automatico che coincide con un'esecuzione a una moneta del file resta com'è (non sposta
 * i calcoli già fatti); uno che ha unito movimenti di esecuzioni diverse viene annullato e rifatto, e lo dice il
 * resoconto ({@link Esito#riparazioni}).
 */
public class Binance_AutoInvest {

    static final String WALLET_AUTO_INVEST = "Auto-Invest";

    //Causali grezze del CSV principale (campo [7])
    static final String CAUSALE_TRANSAZIONE = "Auto-Invest Transaction";
    static final String CAUSALE_DEDUCT = "Auto-Invest - Deduct Asset";
    static final String CAUSALE_RISCATTO = "Index-Auto-Investment - Redeem Positions";

    //Campo [7] dei movimenti generati: dice da quale file vengono, e non coincide con nessuna causale del CSV
    static final String ORIGINE_SOTTOSCRIZIONE = "Auto-Invest Subscription";
    static final String ORIGINE_RIBILANCIAMENTO = "Auto-Invest Rebalancing";
    static final String ORIGINE_RISCATTO = "Auto-Invest Redemption";

    static final String CAMPO18_USCITA = "PTW - Trasferimento a Auto-Invest";
    static final String CAMPO18_ENTRATA = "DTW - Trasferimento da Auto-Invest";
    static final String CAMPO5_USCITA = "TRASFERIMENTO A AUTO-INVEST";
    static final String CAMPO5_ENTRATA = "TRASFERIMENTO DA AUTO-INVEST";

    //Secondo segmento dell'ID dei generati: a parità di secondo l'ordine della mappa (per ID) mette l'entrata sul
    //sotto-wallet prima degli scambi e gli scambi prima dell'uscita, così il sotto-wallet non va mai sotto zero
    static final String ID_USCITA_GENERATA = "AutoInvest0";
    static final String ID_SPECULARE_ENTRATA = "AutoInvest1";
    static final String ID_SCAMBIO = "AutoInvest2";
    static final String ID_SPECULARE_USCITA = "AutoInvest3";

    /** Distanza massima fra l'orario programmato del file e l'uscita del CSV principale. */
    static final long FINESTRA_USCITA_MS = 12L * 3600_000L;
    /** Distanza massima fra l'orario programmato e l'entrata: sul dataset reale fino a quasi sei ore. */
    static final long FINESTRA_ENTRATA_MS = 24L * 3600_000L;
    /** Oltre questo numero di ribilanciamenti di una moneta il verso si cerca coi prezzi, non per enumerazione. */
    static final int MAX_EVENTI_RICERCA_ESATTA = 36;
    /** Sbilancio massimo ammesso, coi prezzi, fra venduto e comprato di un ribilanciamento (frazione del comprato). */
    static final BigDecimal SBILANCIO_MAX = new BigDecimal("0.15");
    /** Sbilanci che differiscono meno di questa frazione del valore scambiato contano come pari. */
    static final BigDecimal TOLLERANZA_PARI = new BigDecimal("0.02");

    private static final Pattern OFFSET_PATTERN = Pattern.compile("UTC([+-]?\\d{1,2})(?!\\d)", Pattern.CASE_INSENSITIVE);

    /** Esito, per il resoconto. */
    public static class Esito {
        public int righeSottoscrizione = 0;
        public int righeNonRiuscite = 0;
        /** Esecuzioni: un'uscita del CSV (o una generata) con gli acquisti che ha pagato. */
        public int esecuzioni = 0;
        public int esecuzioniCreate = 0;
        public int esecuzioniGiaAPosto = 0;
        /** Esecuzioni che lo scambio differito automatico aveva già abbinato correttamente: lasciate com'erano. */
        public int esecuzioniScambioDifferito = 0;
        public int esecuzioniNonTrovate = 0;
        /** Acquisti di piani indice: le monete restano sul sotto-wallet. */
        public int acquistiIndice = 0;
        /** Esecuzioni senza uscita nel CSV (prelievo da Flexible Savings prima del 2023): uscita generata. */
        public int usciteGenerate = 0;
        public int ribilanciamenti = 0;
        public int ribilanciamentiCreati = 0;
        public int ribilanciamentiGiaAPosto = 0;
        public int ribilanciamentiNonRicostruiti = 0;
        /** Quante monete hanno avuto il verso dei ribilanciamenti da sottoscrizioni e riscatti (esatto). */
        public int monetePerQuadratura = 0;
        /** Quante coi prezzi di mercato. */
        public int monetePerPrezzo = 0;
        public int riscatti = 0;
        public int riscattiCreati = 0;
        public int riscattiGiaAPosto = 0;
        public int riscattiNonTrovati = 0;
        /** Scambi differiti automatici annullati e rifatti perché univano movimenti di esecuzioni diverse. */
        public int scambiDifferitiSostituiti = 0;
        /** Una riga per ogni caso non sistemato. */
        public List<String> dettagli = new ArrayList<>();
        /** Una riga per ogni scambio differito sostituito, con la data: cambia i calcoli di quell'anno. */
        public List<String> riparazioni = new ArrayList<>();
        /** Avvisi che non riguardano un caso singolo (file mancanti, fuso orario corretto...). */
        public List<String> avvisi = new ArrayList<>();
        /** La connessione è caduta durante la ricerca dei prezzi: il resto non è stato scritto. */
        public boolean interrotto = false;

        public int Totali() {
            return esecuzioni + ribilanciamenti + riscatti;
        }

        public int Creati() {
            return esecuzioniCreate + ribilanciamentiCreati + riscattiCreati;
        }

        public int GiaAPosto() {
            return esecuzioniGiaAPosto + esecuzioniScambioDifferito + ribilanciamentiGiaAPosto + riscattiGiaAPosto;
        }

        public int NonSistemati() {
            return esecuzioniNonTrovate + ribilanciamentiNonRicostruiti + riscattiNonTrovati;
        }
    }

    // =============================================================================================
    // Dati letti dai file
    // =============================================================================================

    /** Una riga del file Subscription: un acquisto di una moneta. */
    static final class Acquisto {
        long ts;
        String data;
        String moneta;
        BigDecimal importo;
        String quota;
        String commissione;
        BigDecimal unita;
        String origine;
        /** Entrata nello Spot nel CSV principale, {@code null} per un acquisto di un piano indice. */
        String[] entrata;
        /** Un'entrata con moneta e quantità giuste esiste, ma è classificata in altro modo. */
        boolean entrataAltrove;
    }

    /** Un'uscita (o la sua assenza) con gli acquisti che ha pagato. */
    static final class Esecuzione {
        long ts;
        String data;
        String quota;
        List<Acquisto> acquisti = new ArrayList<>();
        String[] uscita;
    }

    static final class Ribilanciamento {
        long ts;
        String data;
        String indice;
        String moneta;
        BigDecimal quantita;
        String commissione;
        /** +1 comprata, -1 venduta, 0 non ancora deciso. */
        int segno;
    }

    static final class Riscatto {
        long ts;
        String data;
        String indice;
        String moneta;
        BigDecimal quantita;
    }

    static final class Dati {
        List<Acquisto> acquisti = new ArrayList<>();
        List<Ribilanciamento> ribilanciamenti = new ArrayList<>();
        List<Riscatto> riscatti = new ArrayList<>();
        int righeNonRiuscite = 0;
        boolean fileSottoscrizioni, fileRibilanciamenti, fileRiscatti;
    }

    /** Fornitore del controvalore in euro di una quantità a un istante, sostituibile nei test. */
    interface Prezzatore {
        BigDecimal Valore(String moneta, BigDecimal quantita, long ts);
    }

    static Prezzatore PrezzatoreDefault = (moneta, quantita, ts) -> {
        Moneta m = new Moneta();
        m.InserisciValori(moneta, quantita.toPlainString(), null, "Crypto");
        String v = Prezzi.DammiPrezzoTransazione(m, null, ts, "0", true, 10, "", "");
        try {
            return new BigDecimal(v).abs();
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    };

    // =============================================================================================
    // Ingresso
    // =============================================================================================

    /**
     * Legge i file di dettaglio (uno o più, in qualunque ordine: il tipo si riconosce dall'intestazione, le righe
     * ripetute in più parti si contano una volta) e sistema i movimenti Auto-Invest già importati dal CSV principale.
     * Non ricalcola nulla: il chiamante invoca {@code Funzioni_AggiornaTutto()} una volta sola alla fine.
     * @param files i CSV Subscription, Rebalancing e Redemption scaricati da Binance
     * @return il resoconto
     * @throws IOException se un file non è leggibile
     */
    public static Esito Abbina(File... files) throws IOException {
        return Abbina(PrezzatoreDefault, files);
    }

    static Esito Abbina(Prezzatore prezzatore, File... files) throws IOException {
        Esito esito = new Esito();
        Dati dati = new Dati();
        for (File f : files) LeggiFile(f, dati);
        esito.righeNonRiuscite = dati.righeNonRiuscite;
        esito.righeSottoscrizione = dati.acquisti.size();
        if (!dati.fileSottoscrizioni) {
            esito.avvisi.add("Manca il file Subscription: senza non si può abbinare nessuna esecuzione dei piani.");
        }

        String exchange = "Binance";
        //1. Entrate: moneta e quantità esatte, la più vicina all'orario del file
        Candidati candidati = new Candidati();
        dati.acquisti.sort(Comparator.comparingLong(a -> a.ts));
        Set<String[]> usate = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Acquisto a : dati.acquisti) {
            String[] entrata = Piu_Vicino(candidati.entrate, usate, a.moneta, a.unita, a.ts, FINESTRA_ENTRATA_MS);
            if (entrata == null) continue;
            if (Stato(entrata) == StatoRiga.ALTRO) {
                a.entrataAltrove = true;
                continue;
            }
            usate.add(entrata);
            a.entrata = entrata;
            exchange = entrata[3];
        }

        //2. Uscite: per orario e moneta spesa, ripartendo gli acquisti fra le uscite con somma esatta
        List<Esecuzione> esecuzioni = new ArrayList<>();
        Map<String, List<Acquisto>> gruppi = new LinkedHashMap<>();
        for (Acquisto a : dati.acquisti) {
            gruppi.computeIfAbsent(a.ts + "|" + a.quota.toUpperCase(), k -> new ArrayList<>()).add(a);
        }
        List<Long> scarti = new ArrayList<>();
        for (List<Acquisto> gruppo : gruppi.values()) {
            Acquisto primo = gruppo.get(0);
            List<String[]> cand = new ArrayList<>();
            for (String[] u : candidati.uscite) {
                if (usate.contains(u) || !primo.quota.equalsIgnoreCase(u[8])) continue;
                if (Math.abs(TsRiga(u) - primo.ts) <= FINESTRA_USCITA_MS) cand.add(u);
            }
            cand.sort(Comparator.comparingLong(u -> Math.abs(TsRiga(u) - primo.ts)));
            List<Esecuzione> partizione = new ArrayList<>();
            if (Partiziona(new ArrayList<>(gruppo), cand, usate, partizione)) {
                for (Esecuzione e : partizione) {
                    usate.add(e.uscita);
                    exchange = e.uscita[3];
                    scarti.add(TsRiga(e.uscita) - e.ts);
                }
                esecuzioni.addAll(partizione);
            } else {
                //Nessuna uscita: si genera solo se ogni acquisto ha la sua entrata (prelievo da Flexible Savings
                //prima del 2023) e non c'è nessuna uscita di quella moneta nei paraggi, che vorrebbe dire dati diversi
                //da quelli attesi. Altrimenti l'esecuzione resta non abbinata (ts = -1)
                Esecuzione e = new Esecuzione();
                e.ts = gruppo.stream().allMatch(a -> a.entrata != null) && cand.isEmpty() ? primo.ts : -1;
                e.data = primo.data;
                e.quota = primo.quota;
                e.acquisti.addAll(gruppo);
                esecuzioni.add(e);
            }
        }
        esito.esecuzioni = esecuzioni.size();

        //Fuso orario dei file: se le uscite cadono tutte un numero intero di ore dopo, il nome del file non diceva
        //il fuso del CSV principale. Serve ai ribilanciamenti, che non hanno un movimento del CSV a cui agganciarsi
        long correzione = CorrezioneFuso(scarti);
        if (correzione != 0) {
            esito.avvisi.add("Gli orari dei file di dettaglio risultano spostati di " + (correzione / 3600_000L)
                    + " ore rispetto al CSV principale: corretti per i ribilanciamenti.");
        }

        //3. Stato delle esecuzioni: già fatte, scambi differiti automatici da tenere o da rifare
        Set<String[]> daAnnullare = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Esecuzione> daCreare = new ArrayList<>();
        for (Esecuzione e : esecuzioni) {
            if (e.ts == -1) {
                esito.esecuzioniNonTrovate++;
                Acquisto a = e.acquisti.get(0);
                esito.dettagli.add("Esecuzione del " + a.data + " (" + Descrivi(e) + "): "
                        + (e.acquisti.stream().anyMatch(x -> x.entrataAltrove)
                        ? "l'entrata in archivio è classificata in altro modo - lasciata com'è"
                        : "uscita di " + a.quota + " non trovata in archivio (CSV principale non importato per quel periodo?)"));
                continue;
            }
            if (e.acquisti.stream().anyMatch(x -> x.entrataAltrove)) {
                esito.esecuzioniNonTrovate++;
                esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): un'entrata in archivio è "
                        + "classificata in altro modo - lasciata com'è");
                continue;
            }
            List<String[]> righe = RigheImportate(e);
            boolean nuove = true, mie = true, differite = false, altre = false;
            for (String[] r : righe) {
                StatoRiga s = Stato(r);
                nuove &= s == StatoRiga.NUOVO;
                mie &= s == StatoRiga.MIO;
                differite |= s == StatoRiga.DIFFERITO;
                altre |= s == StatoRiga.ALTRO;
            }
            if (e.uscita == null) {
                //Uscita generata: è "mia" se le entrate sono già giroconti dal sotto-wallet
                mie = righe.stream().allMatch(r -> Stato(r) == StatoRiga.MIO);
            }
            if (mie && !righe.isEmpty()) {
                esito.esecuzioniGiaAPosto++;
                continue;
            }
            if (altre) {
                esito.esecuzioniNonTrovate++;
                esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): un movimento è classificato "
                        + "in altro modo - lasciata com'è");
                continue;
            }
            if (differite) {
                if (ScambioDifferitoCoincide(e)) {
                    esito.esecuzioniScambioDifferito++;
                    continue;
                }
                //Si annulla solo uno scambio differito fra due movimenti Auto-Invest: con un movimento di
                //un'altra operazione non si sa che cosa rifare
                boolean annullabile = true;
                for (String[] r : righe) {
                    if (Stato(r) != StatoRiga.DIFFERITO) continue;
                    String[] partner = PartnerScambioDifferito(r);
                    if (partner == null || !CausaleAutoInvest(partner[7])) annullabile = false;
                }
                if (!annullabile) {
                    esito.esecuzioniNonTrovate++;
                    esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): abbinata in uno scambio "
                            + "differito con un movimento che non è Auto-Invest - lasciata com'è");
                    continue;
                }
                for (String[] r : righe) if (Stato(r) == StatoRiga.DIFFERITO) daAnnullare.add(r);
                daCreare.add(e);
                continue;
            }
            if (!nuove) {
                esito.esecuzioniNonTrovate++;
                esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): movimenti in parte già "
                        + "abbinati - annullare la classificazione e ripassare i file");
                continue;
            }
            daCreare.add(e);
        }

        //4. Annullo gli scambi differiti automatici sbagliati, poi creo
        for (String[] r : daAnnullare) {
            if (Stato(r) != StatoRiga.DIFFERITO) continue; //già annullato insieme al suo partner
            String[] partner = PartnerScambioDifferito(r);
            String descrizione = DescriviScambio(r, partner);
            String[] parti = (r[0] + "," + r[20]).split(",");
            GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(parti, r[0]);
            esito.scambiDifferitiSostituiti++;
            esito.riparazioni.add(descrizione);
        }
        Map<String, String> tipiMoneta = new HashMap<>();
        for (Esecuzione e : daCreare) {
            if (Interrotto(esito)) {
                esito.esecuzioniNonTrovate++;
                continue;
            }
            if (!RigheImportate(e).stream().allMatch(r -> Stato(r) == StatoRiga.NUOVO)) {
                esito.esecuzioniNonTrovate++;
                esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): movimenti cambiati durante "
                        + "l'abbinamento - ripassare i file");
                continue;
            }
            String errore = CreaEsecuzione(e, tipiMoneta);
            if (errore != null) {
                esito.esecuzioniNonTrovate++;
                esito.dettagli.add("Esecuzione del " + e.data + " (" + Descrivi(e) + "): " + errore);
                continue;
            }
            esito.esecuzioniCreate++;
            if (e.uscita == null) esito.usciteGenerate++;
            for (Acquisto a : e.acquisti) if (a.entrata == null) esito.acquistiIndice++;
        }

        //5. Ribilanciamenti
        List<Acquisto> acquistiIndice = new ArrayList<>();
        for (Acquisto a : dati.acquisti) if (a.entrata == null && !a.entrataAltrove) acquistiIndice.add(a);
        for (Ribilanciamento r : dati.ribilanciamenti) r.ts += correzione;
        Map<Long, List<Ribilanciamento>> eventi = new TreeMap<>();
        for (Ribilanciamento r : dati.ribilanciamenti) {
            if (r.quantita.signum() != 0) eventi.computeIfAbsent(r.ts, k -> new ArrayList<>()).add(r);
        }
        esito.ribilanciamenti = eventi.size();
        if (!eventi.isEmpty() || !acquistiIndice.isEmpty()) {
            if (!acquistiIndice.isEmpty() && !dati.fileRibilanciamenti) {
                esito.avvisi.add("Ci sono acquisti di un piano indice ma manca il file Rebalancing: le quantità sul "
                        + "sotto-wallet " + WALLET_AUTO_INVEST + " non terranno conto dei ribilanciamenti.");
            }
            if (!acquistiIndice.isEmpty() && !dati.fileRiscatti) {
                esito.avvisi.add("Ci sono acquisti di un piano indice ma manca il file Redemption: i riscatti restano "
                        + "da classificare, e il verso dei ribilanciamenti si ricava solo dai prezzi.");
            }
            String errore = SegniRibilanciamenti(acquistiIndice, dati.ribilanciamenti, dati.riscatti, prezzatore, esito);
            if (errore != null) {
                esito.ribilanciamentiNonRicostruiti = eventi.size();
                esito.dettagli.add("Ribilanciamenti non ricostruiti: " + errore);
            } else {
                for (List<Ribilanciamento> evento : eventi.values()) {
                    if (Interrotto(esito)) {
                        esito.ribilanciamentiNonRicostruiti++;
                        continue;
                    }
                    if (RibilanciamentoGiaCreato(evento.get(0).ts)) {
                        esito.ribilanciamentiGiaAPosto++;
                        continue;
                    }
                    String err = CreaRibilanciamento(evento, exchange, prezzatore);
                    if (err != null) {
                        esito.ribilanciamentiNonRicostruiti++;
                        esito.dettagli.add("Ribilanciamento del " + evento.get(0).data + ": " + err);
                    } else {
                        esito.ribilanciamentiCreati++;
                    }
                }
            }
        }

        //6. Riscatti
        esito.riscatti = dati.riscatti.size();
        for (Riscatto r : dati.riscatti) {
            String[] mov = Piu_Vicino(candidati.riscatti, usate, r.moneta, r.quantita, r.ts, FINESTRA_ENTRATA_MS);
            if (mov == null) {
                esito.riscattiNonTrovati++;
                esito.dettagli.add("Riscatto del " + r.data + " di " + r.quantita.toPlainString() + " " + r.moneta
                        + ": non trovato in archivio");
                continue;
            }
            usate.add(mov);
            StatoRiga s = Stato(mov);
            if (s == StatoRiga.MIO) {
                esito.riscattiGiaAPosto++;
                continue;
            }
            if (s != StatoRiga.NUOVO) {
                esito.riscattiNonTrovati++;
                esito.dettagli.add("Riscatto del " + r.data + " di " + r.quantita.toPlainString() + " " + r.moneta
                        + ": già classificato in altro modo - lasciato com'è");
                continue;
            }
            if (!CreaRiscatto(mov)) {
                esito.riscattiNonTrovati++;
                esito.dettagli.add("Riscatto del " + r.data + " di " + r.quantita.toPlainString() + " " + r.moneta
                        + ": impossibile creare i movimenti, dettagli nel log");
                continue;
            }
            esito.riscattiCreati++;
        }
        return esito;
    }

    /**
     * La ricerca dei prezzi ha rinunciato per la linea assente ({@link AttesaConnessione}): da qui non si scrive più.
     * Quello già scritto resta, completo per esecuzione, e ripassando i file si completa il resto.
     */
    private static boolean Interrotto(Esito esito) {
        if (!AttesaConnessione.Abortita()) return false;
        if (!esito.interrotto) {
            esito.interrotto = true;
            esito.avvisi.add("Connessione persa durante la ricerca dei prezzi: l'abbinamento si è fermato. Quello già "
                    + "abbinato resta, ripassando i file quando la connessione torna si completa il resto.");
        }
        return true;
    }

    // =============================================================================================
    // Lettura dei file
    // =============================================================================================

    static void LeggiFile(File file, Dati dati) throws IOException {
        int offset = EstraiOffset(file.getName());
        List<String> righe = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String riga;
            while ((riga = br.readLine()) != null) {
                riga = riga.replace("﻿", "").trim();
                if (!riga.isEmpty()) righe.add(riga);
            }
        }
        if (righe.isEmpty()) return;
        String intestazione = righe.get(0).toLowerCase();
        Set<String> viste = new HashSet<>();
        for (int i = 1; i < righe.size(); i++) {
            if (!viste.add(righe.get(i))) continue;
            String[] c = righe.get(i).split(",", -1);
            if (intestazione.contains("holding coin")) {
                dati.fileSottoscrizioni = true;
                if (c.length < 7) continue;
                if (!c[6].trim().equalsIgnoreCase("Success")) {
                    dati.righeNonRiuscite++;
                    continue;
                }
                String[] importo = Binance_DualInvestment.parseImportoMoneta(c[2]);
                String[] unita = Binance_DualInvestment.parseImportoMoneta(c[4]);
                if (importo == null || unita == null) continue;
                Acquisto a = new Acquisto();
                a.data = c[0].trim();
                a.ts = Binance_DualInvestment.convertiAEpocaUtc(a.data, offset);
                a.moneta = unita[1];
                a.unita = new BigDecimal(unita[0]);
                a.importo = new BigDecimal(importo[0]);
                a.quota = importo[1];
                a.commissione = c[3].trim();
                a.origine = c[5].trim();
                dati.acquisti.add(a);
            } else if (intestazione.contains("rebalancing coin")) {
                dati.fileRibilanciamenti = true;
                if (c.length < 5) continue;
                Ribilanciamento r = new Ribilanciamento();
                r.data = c[0].trim();
                r.ts = Binance_DualInvestment.convertiAEpocaUtc(r.data, offset);
                r.indice = c[1].trim();
                r.moneta = c[2].trim();
                r.quantita = new BigDecimal(c[3].trim()).abs();
                r.commissione = c[4].trim();
                dati.ribilanciamenti.add(r);
            } else if (intestazione.contains("redemption amount")) {
                dati.fileRiscatti = true;
                if (c.length < 4) continue;
                Riscatto r = new Riscatto();
                r.data = c[0].trim();
                r.ts = Binance_DualInvestment.convertiAEpocaUtc(r.data, offset);
                r.indice = c[1].trim();
                r.moneta = c[2].trim();
                r.quantita = new BigDecimal(c[3].trim()).abs();
                if (r.quantita.signum() != 0) dati.riscatti.add(r);
            }
        }
    }

    /**
     * Offset orario dal nome del file: Binance scrive {@code UTC+2}, ma il segno può mancare ({@code UTC2}).
     * @return l'offset in ore, 0 se il nome non lo dice
     */
    static int EstraiOffset(String nomeFile) {
        Matcher m = OFFSET_PATTERN.matcher(nomeFile);
        if (!m.find()) return 0;
        try {
            return Integer.parseInt(m.group(1).replace("+", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // =============================================================================================
    // Movimenti del CSV principale
    // =============================================================================================

    enum StatoRiga { NUOVO, MIO, DIFFERITO, ALTRO }

    static StatoRiga Stato(String[] v) {
        String c18 = v[18] == null ? "" : v[18].trim();
        if (c18.isEmpty()) return StatoRiga.NUOVO;
        if (c18.equals(CAMPO18_USCITA) || c18.equals(CAMPO18_ENTRATA)) return StatoRiga.MIO;
        if (c18.equals(MovimentiCollegati.CAMPO18_PRELIEVO_DIFFERITO)
                || c18.equals(MovimentiCollegati.CAMPO18_DEPOSITO_DIFFERITO)) return StatoRiga.DIFFERITO;
        return StatoRiga.ALTRO;
    }

    static boolean CausaleAutoInvest(String causale) {
        return CAUSALE_TRANSAZIONE.equalsIgnoreCase(causale) || CAUSALE_DEDUCT.equalsIgnoreCase(causale);
    }

    /** I movimenti Auto-Invest dell'archivio, in tutti gli stati: lo stato si guarda dopo l'abbinamento. */
    static final class Candidati {
        final List<String[]> uscite = new ArrayList<>();
        final List<String[]> entrate = new ArrayList<>();
        final List<String[]> riscatti = new ArrayList<>();

        Candidati() {
            for (String[] v : MappaCryptoWallet.values()) {
                if (v == null || v.length <= 20 || v[7] == null || "AU".equalsIgnoreCase(v[22])) continue;
                String[] id = v[0].split("_");
                if (id.length < 5) continue;
                String cat = id[4].toUpperCase();
                if (CausaleAutoInvest(v[7]) && (cat.equals("PC") || cat.equals("PF"))) uscite.add(v);
                else if (CAUSALE_TRANSAZIONE.equalsIgnoreCase(v[7]) && (cat.equals("DC") || cat.equals("DF"))) entrate.add(v);
                else if (CAUSALE_RISCATTO.equalsIgnoreCase(v[7]) && cat.equals("DC")) riscatti.add(v);
            }
        }
    }

    /** Istante del movimento dal prefisso dell'ID, che porta i secondi (il campo 1 no). */
    static long TsRiga(String[] v) {
        return FunzioniDate.ConvertiDataIDinLong(v[0].substring(0, 14));
    }

    private static BigDecimal Quantita(String[] v) {
        String q = Funzioni.noData(v[10]) ? v[13] : v[10];
        try {
            return new BigDecimal(q).abs();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String MonetaDi(String[] v) {
        return Funzioni.noData(v[8]) ? v[11] : v[8];
    }

    /**
     * Il candidato con moneta e quantità esatte più vicino a {@code ts}, entro {@code finestra}. Un pareggio esatto
     * fra due candidati equidistanti non sceglie: {@code null}.
     */
    static String[] Piu_Vicino(List<String[]> candidati, Set<String[]> usate, String moneta, BigDecimal quantita,
            long ts, long finestra) {
        String[] migliore = null;
        long scarto = Long.MAX_VALUE;
        boolean pari = false;
        for (String[] v : candidati) {
            if (usate.contains(v) || !moneta.equalsIgnoreCase(MonetaDi(v))) continue;
            BigDecimal q = Quantita(v);
            if (q == null || q.compareTo(quantita) != 0) continue;
            long s = Math.abs(TsRiga(v) - ts);
            if (s > finestra) continue;
            if (s < scarto) {
                scarto = s;
                migliore = v;
                pari = false;
            } else if (s == scarto) {
                pari = true;
            }
        }
        return pari ? null : migliore;
    }

    /**
     * Ripartisce gli acquisti di un orario fra le uscite candidate (le più vicine prima): ogni uscita prende un
     * sottoinsieme di acquisti la cui somma è esattamente il suo importo. Due piani con la stessa moneta allo stesso
     * orario danno due uscite (sul dataset reale 1 FDUSD e 0,5 FDUSD per due monete).
     * @return {@code true} se tutti gli acquisti hanno trovato un'uscita; {@code partizione} contiene le esecuzioni
     */
    static boolean Partiziona(List<Acquisto> restanti, List<String[]> candidati, Set<String[]> usate, List<Esecuzione> partizione) {
        if (restanti.isEmpty()) return true;
        if (restanti.size() > 16) {
            //Troppi per provare i sottoinsiemi: solo l'intero gruppo con un'uscita sola
            BigDecimal somma = restanti.stream().map(a -> a.importo).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (String[] u : candidati) {
                if (usate.contains(u) || partizione.stream().anyMatch(e -> e.uscita == u)) continue;
                BigDecimal q = Quantita(u);
                if (q != null && q.compareTo(somma) == 0) {
                    partizione.add(NuovaEsecuzione(u, restanti));
                    return true;
                }
            }
            return false;
        }
        for (String[] u : candidati) {
            if (usate.contains(u) || partizione.stream().anyMatch(e -> e.uscita == u)) continue;
            BigDecimal q = Quantita(u);
            if (q == null) continue;
            int n = restanti.size();
            //Prima l'intero gruppo, poi i sottoinsiemi dal più grande
            List<Integer> maschere = new ArrayList<>();
            for (int m = (1 << n) - 1; m > 0; m--) maschere.add(m);
            maschere.sort(Comparator.comparingInt(Integer::bitCount).reversed());
            for (int m : maschere) {
                BigDecimal somma = BigDecimal.ZERO;
                for (int i = 0; i < n; i++) if ((m & (1 << i)) != 0) somma = somma.add(restanti.get(i).importo);
                if (somma.compareTo(q) != 0) continue;
                List<Acquisto> presi = new ArrayList<>(), altri = new ArrayList<>();
                for (int i = 0; i < n; i++) ((m & (1 << i)) != 0 ? presi : altri).add(restanti.get(i));
                partizione.add(NuovaEsecuzione(u, presi));
                if (Partiziona(altri, candidati, usate, partizione)) return true;
                partizione.remove(partizione.size() - 1);
            }
        }
        return false;
    }

    private static Esecuzione NuovaEsecuzione(String[] uscita, List<Acquisto> acquisti) {
        Esecuzione e = new Esecuzione();
        e.uscita = uscita;
        e.acquisti.addAll(acquisti);
        e.ts = acquisti.get(0).ts;
        e.data = acquisti.get(0).data;
        e.quota = acquisti.get(0).quota;
        return e;
    }

    /**
     * Se le uscite stanno tutte almeno tre quarti d'ora dall'orario del file, la differenza in ore intere è un fuso
     * diverso fra i file. L'esecuzione vera arriva pochi minuti dopo l'orario programmato.
     */
    static long CorrezioneFuso(List<Long> scarti) {
        if (scarti.isEmpty()) return 0;
        List<Long> s = new ArrayList<>(scarti);
        Collections.sort(s);
        long mediana = s.get(s.size() / 2);
        if (Math.abs(mediana) < 45 * 60_000L) return 0;
        return Math.round(mediana / 3600_000.0) * 3600_000L;
    }

    /** Uscita ed entrate dell'esecuzione presenti nel CSV principale. */
    private static List<String[]> RigheImportate(Esecuzione e) {
        List<String[]> r = new ArrayList<>();
        if (e.uscita != null) r.add(e.uscita);
        for (Acquisto a : e.acquisti) if (a.entrata != null) r.add(a.entrata);
        return r;
    }

    /** L'altro capo dello scambio differito di {@code v}, se è ancora in archivio. */
    static String[] PartnerScambioDifferito(String[] v) {
        if (Funzioni.noData(v[20])) return null;
        for (String id : v[20].split(",")) {
            String[] m = MappaCryptoWallet.get(id.trim());
            if (m != null && m != v && !"AU".equalsIgnoreCase(m[22]) && Stato(m) == StatoRiga.DIFFERITO) return m;
        }
        return null;
    }

    /** Lo scambio differito automatico unisce proprio l'uscita e l'unica entrata di questa esecuzione. */
    private static boolean ScambioDifferitoCoincide(Esecuzione e) {
        if (e.uscita == null || e.acquisti.size() != 1) return false;
        String[] entrata = e.acquisti.get(0).entrata;
        return entrata != null && Stato(e.uscita) == StatoRiga.DIFFERITO && Stato(entrata) == StatoRiga.DIFFERITO
                && PartnerScambioDifferito(e.uscita) == entrata;
    }

    private static String Descrivi(Esecuzione e) {
        StringBuilder sb = new StringBuilder();
        BigDecimal totale = e.acquisti.stream().map(a -> a.importo).reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append(totale.stripTrailingZeros().toPlainString()).append(' ').append(e.quota).append(" -> ");
        for (int i = 0; i < e.acquisti.size(); i++) {
            if (i > 0) sb.append(" + ");
            if (i == 3 && e.acquisti.size() > 4) {
                sb.append("altre ").append(e.acquisti.size() - 3);
                break;
            }
            sb.append(e.acquisti.get(i).moneta);
        }
        return sb.toString();
    }

    private static String DescriviScambio(String[] r, String[] partner) {
        String[] uscita = Funzioni.noData(r[8]) ? partner : r;
        String[] entrata = uscita == r ? partner : r;
        String testo = "Scambio differito automatico del " + r[1];
        if (uscita != null && entrata != null) {
            testo += ": " + Quantita(uscita).toPlainString() + " " + uscita[8] + " -> "
                    + Quantita(entrata).toPlainString() + " " + entrata[11];
        }
        return testo + " - univa movimenti di esecuzioni diverse, rifatto dal file";
    }

    // =============================================================================================
    // Creazione dei movimenti
    // =============================================================================================

    /** Riserva gli ID dei generati prima di metterli in mappa, così due generati della stessa esecuzione non collidono. */
    private static final class GeneratoreID {
        final Set<String> riservati = new HashSet<>();

        String Nuovo(long ts, String sezione, String categoria) {
            String dataID = FunzioniDate.ConvertiDatadaLongAlSecondo(ts).replaceAll(" |-|:", "");
            for (int n = 1; n < 1000; n++) {
                String id = dataID + "_" + sezione + "_001_" + String.format("%03d", n) + "_" + categoria;
                if (!MappaCryptoWallet.containsKey(id) && !riservati.contains(id)) {
                    riservati.add(id);
                    return id;
                }
            }
            return null;
        }
    }

    private static Moneta NuovaMoneta(String simbolo, String tipo, BigDecimal qta) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Tipo = Funzioni.noData(tipo) ? "Crypto" : tipo;
        m.Qta = qta.stripTrailingZeros().toPlainString();
        return m;
    }

    /**
     * Un movimento generato sul sotto-wallet (o, per l'uscita mancante, sul wallet dell'entrata): il prezzo passato
     * si usa così com'è, {@code null} lo fa cercare a {@link MovimentiCrypto#creaMovimento} con la regola di sempre
     * (gamba FIAT, altrimenti quella in entrata).
     */
    private static String[] Genera(Moneta out, Moneta in, String exchange, String wallet, long ts, String prezzo,
            String nota, String origine, String sezione, GeneratoreID gid) {
        String[] v = MovimentiCrypto.creaMovimento(out, in, exchange, wallet, ts, prezzo, null, 1, 1, null, nota, "AU",
                null, null, null);
        if (v == null) return null;
        String id = gid.Nuovo(ts, sezione, v[0].split("_")[4]);
        if (id == null) return null;
        v[0] = id;
        v[2] = "1 di 1";
        v[7] = origine;
        return v;
    }

    /** Gamba speculare di un giroconto: valore dell'importato, nessun prezzo proprio (come nel Dual Investment). */
    private static void Speculare(String[] v, String campo18) {
        v[5] = "TRASFERIMENTO INTERNO";
        v[18] = campo18;
        v[32] = "";
        v[40] = "";
    }

    private static String TipoMoneta(String simbolo, Map<String, String> cache) {
        return cache.computeIfAbsent(simbolo.toUpperCase(), s -> {
            for (String[] v : MappaCryptoWallet.values()) {
                if (s.equalsIgnoreCase(v[8]) && !Funzioni.noData(v[9])) return v[9];
                if (s.equalsIgnoreCase(v[11]) && !Funzioni.noData(v[12])) return v[12];
            }
            return "Crypto";
        });
    }

    /**
     * Crea i movimenti di un'esecuzione e li scrive in mappa, tutti o nessuno.
     * @return {@code null} se fatto, altrimenti il motivo per cui non si è fatto nulla
     */
    static String CreaEsecuzione(Esecuzione e, Map<String, String> tipiMoneta) {
        GeneratoreID gid = new GeneratoreID();
        List<String[]> gruppo = new ArrayList<>();
        List<String[]> generati = new ArrayList<>();
        String[] uscita = e.uscita;
        String exchange;
        long tsUscita;
        String tipoQuota;
        BigDecimal importoTotale = e.acquisti.stream().map(a -> a.importo).reduce(BigDecimal.ZERO, BigDecimal::add);
        String valoreUscita;
        String documento;

        if (uscita != null) {
            exchange = uscita[3];
            tsUscita = TsRiga(uscita);
            tipoQuota = uscita[9];
            valoreUscita = uscita[15];
            documento = uscita[41];
            gruppo.add(uscita);
        } else {
            //Prelievo da Flexible Savings che il CSV non registra: lo si genera sul wallet dell'entrata, al primo
            //istante in cui una moneta comprata arriva, valorizzato come quello che si è ricevuto
            String[] primaEntrata = e.acquisti.stream().map(a -> a.entrata)
                    .min(Comparator.comparingLong(Binance_AutoInvest::TsRiga)).orElse(null);
            if (primaEntrata == null) return "nessun movimento in archivio";
            exchange = primaEntrata[3];
            tsUscita = TsRiga(primaEntrata);
            tipoQuota = TipoMoneta(e.quota, tipiMoneta);
            BigDecimal valore = BigDecimal.ZERO;
            for (Acquisto a : e.acquisti) {
                try {
                    valore = valore.add(new BigDecimal(a.entrata[15]));
                } catch (Exception ex) {
                    //valore mancante: resta quello delle altre
                }
            }
            valoreUscita = valore.setScale(2, RoundingMode.HALF_UP).toPlainString();
            documento = primaEntrata[41];
            String[] generata = Genera(NuovaMoneta(e.quota, tipoQuota, importoTotale.negate()), null, exchange,
                    primaEntrata[4], tsUscita, valoreUscita, "Importo del piano Auto-Invest prelevato da " + e.acquisti.get(0).origine
                    + " - non presente nel CSV delle transazioni", ORIGINE_SOTTOSCRIZIONE, ID_USCITA_GENERATA, gid);
            if (generata == null) return "impossibile generare l'uscita mancante";
            generata[5] = CAMPO5_USCITA;
            generata[18] = CAMPO18_USCITA;
            generata[32] = "";
            generata[40] = "";
            generata[41] = documento;
            gruppo.add(generata);
            generati.add(generata);
        }

        //Gamba speculare in entrata sul sotto-wallet
        String[] specIn = Genera(null, NuovaMoneta(e.quota, tipoQuota, importoTotale), exchange, WALLET_AUTO_INVEST,
                tsUscita, valoreUscita, null, ORIGINE_SOTTOSCRIZIONE, ID_SPECULARE_ENTRATA, gid);
        if (specIn == null) return "impossibile creare il giroconto verso il sotto-wallet";
        Speculare(specIn, "DTW - Trasferimento Interno");
        specIn[41] = documento;
        gruppo.add(specIn);
        generati.add(specIn);

        boolean quotaFiat = "FIAT".equalsIgnoreCase(tipoQuota);
        for (Acquisto a : e.acquisti) {
            String[] entrata = a.entrata;
            //Lo scambio si chiude quando la moneta arriva nello Spot; mai prima dell'entrata sul sotto-wallet
            long tsScambio = entrata != null ? Math.max(TsRiga(entrata), tsUscita) : tsUscita;
            String tipoMoneta = entrata != null ? entrata[12] : TipoMoneta(a.moneta, tipiMoneta);
            //Valore: la gamba FIAT se c'è, altrimenti la moneta ricevuta. Se è entrata nello Spot il suo valore è
            //già stato calcolato all'import, per la stessa quantità allo stesso istante
            String prezzo = null;
            if (!quotaFiat && entrata != null && "SI".equalsIgnoreCase(entrata[32]) && !Funzioni.noData(entrata[15])) {
                prezzo = entrata[15];
            }
            String nota = "Piano Auto-Invest del " + a.data + (a.commissione.equals("--") || a.commissione.isBlank()
                    ? "" : " - commissione " + a.commissione + " compresa nell'importo")
                    + (entrata == null ? " - le monete restano nel conto Auto-Invest" : "");
            String[] scambio = Genera(NuovaMoneta(e.quota, tipoQuota, a.importo.negate()),
                    NuovaMoneta(a.moneta, tipoMoneta, a.unita), exchange, WALLET_AUTO_INVEST, tsScambio, prezzo, nota,
                    ORIGINE_SOTTOSCRIZIONE, ID_SCAMBIO, gid);
            if (scambio == null) return "impossibile creare lo scambio " + a.quota + " -> " + a.moneta;
            if (prezzo != null) {
                scambio[40] = entrata[40];
                scambio[32] = entrata[32];
            }
            scambio[41] = documento;
            gruppo.add(scambio);
            generati.add(scambio);
            if (entrata != null) {
                String[] specOut = Genera(NuovaMoneta(a.moneta, tipoMoneta, a.unita.negate()), null, exchange,
                        WALLET_AUTO_INVEST, tsScambio, entrata[15], null, ORIGINE_SOTTOSCRIZIONE, ID_SPECULARE_USCITA, gid);
                if (specOut == null) return "impossibile creare il giroconto dal sotto-wallet";
                Speculare(specOut, "PTW - Trasferimento Interno");
                specOut[41] = entrata[41];
                gruppo.add(specOut);
                generati.add(specOut);
                gruppo.add(entrata);
            }
        }

        //Un prezzo cercato mentre cadeva la linea sarebbe 0: meglio non scrivere (regola di AttesaConnessione)
        if (AttesaConnessione.Abortita()) return "connessione persa durante la ricerca dei prezzi, non scritta";
        //Tutto creato: ora si toccano gli importati e la mappa
        if (uscita != null) {
            uscita[5] = CAMPO5_USCITA;
            uscita[18] = CAMPO18_USCITA;
        }
        for (Acquisto a : e.acquisti) {
            if (a.entrata == null) continue;
            a.entrata[5] = CAMPO5_ENTRATA;
            a.entrata[18] = CAMPO18_ENTRATA;
        }
        CollegaTutti(gruppo);
        for (String[] g : generati) {
            Importazioni.RiempiVuotiArray(g);
            MappaCryptoWallet.put(g[0], g);
        }
        return null;
    }

    /** Un ribilanciamento è già stato creato se sul sotto-wallet c'è un suo scambio in quel secondo. */
    static boolean RibilanciamentoGiaCreato(long ts) {
        String dataID = FunzioniDate.ConvertiDatadaLongAlSecondo(ts).replaceAll(" |-|:", "");
        String inizio = dataID + "_" + ID_SCAMBIO + "_";
        for (String id : MappaCryptoWallet.keySet()) {
            if (!id.startsWith(inizio)) continue;
            String[] v = MappaCryptoWallet.get(id);
            if (v != null && ORIGINE_RIBILANCIAMENTO.equals(v[7])) return true;
        }
        return false;
    }

    /** Un pezzo di ribilanciamento: quanto di una moneta venduta va in quanto di una comprata. */
    static final class Pezzo {
        Ribilanciamento venduta, comprata;
        BigDecimal qtaVenduta, qtaComprata;
    }

    /**
     * Ripartisce le monete vendute fra quelle comprate in proporzione al valore, scorrendo le due liste insieme:
     * al più (vendute + comprate - 1) scambi. Le quantità di ogni moneta tornano esattamente al totale del file
     * (l'ultimo pezzo prende il resto).
     * @return i pezzi, o {@code null} se manca un prezzo
     */
    static List<Pezzo> Ripartisci(List<Ribilanciamento> vendute, List<Ribilanciamento> comprate, Map<Ribilanciamento, BigDecimal> valori) {
        //Arrotondando al più vicino 1/3 di 0,3 fa 0,1 e non 0,09999999; se però un resto andasse sotto zero si
        //ripiega sul troncamento, che non può farlo
        List<Pezzo> pezzi = Ripartisci(vendute, comprate, valori, RoundingMode.HALF_UP);
        if (pezzi != null && pezzi.stream().anyMatch(p -> p.qtaVenduta.signum() < 0 || p.qtaComprata.signum() < 0)) {
            pezzi = Ripartisci(vendute, comprate, valori, RoundingMode.DOWN);
        }
        if (pezzi != null) pezzi.removeIf(p -> p.qtaVenduta.signum() <= 0 || p.qtaComprata.signum() <= 0);
        return pezzi;
    }

    private static List<Pezzo> Ripartisci(List<Ribilanciamento> vendute, List<Ribilanciamento> comprate,
            Map<Ribilanciamento, BigDecimal> valori, RoundingMode arrotondamento) {
        BigDecimal totV = BigDecimal.ZERO, totC = BigDecimal.ZERO;
        for (Ribilanciamento r : vendute) totV = totV.add(valori.get(r));
        for (Ribilanciamento r : comprate) totC = totC.add(valori.get(r));
        if (totV.signum() == 0 || totC.signum() == 0) return null;
        final int SCALA = 20;
        //Frazioni cumulate: il pezzo è l'intersezione degli intervalli [inizio, fine) delle due monete
        List<BigDecimal[]> intV = Intervalli(vendute, valori, totV, SCALA);
        List<BigDecimal[]> intC = Intervalli(comprate, valori, totC, SCALA);
        List<Pezzo> pezzi = new ArrayList<>();
        int i = 0, j = 0;
        while (i < vendute.size() && j < comprate.size()) {
            BigDecimal da = intV.get(i)[0].max(intC.get(j)[0]);
            BigDecimal a = intV.get(i)[1].min(intC.get(j)[1]);
            if (a.compareTo(da) > 0) {
                Pezzo p = new Pezzo();
                p.venduta = vendute.get(i);
                p.comprata = comprate.get(j);
                BigDecimal larghezza = a.subtract(da);
                p.qtaVenduta = Quota(vendute.get(i), larghezza, intV.get(i), arrotondamento);
                p.qtaComprata = Quota(comprate.get(j), larghezza, intC.get(j), arrotondamento);
                pezzi.add(p);
            }
            if (intV.get(i)[1].compareTo(intC.get(j)[1]) < 0) i++;
            else if (intV.get(i)[1].compareTo(intC.get(j)[1]) > 0) j++;
            else {
                i++;
                j++;
            }
        }
        //Resto all'ultimo pezzo di ogni moneta, così le somme sono esatte
        AggiustaResti(pezzi, true);
        AggiustaResti(pezzi, false);
        //Un pezzo arrotondato a zero da un lato prende l'unità minima dal pezzo più grande della stessa moneta
        for (Pezzo p : pezzi) {
            if (p.qtaVenduta.signum() == 0) PrendiUnita(pezzi, p, true);
            if (p.qtaComprata.signum() == 0) PrendiUnita(pezzi, p, false);
        }
        return pezzi;
    }

    private static List<BigDecimal[]> Intervalli(List<Ribilanciamento> lista, Map<Ribilanciamento, BigDecimal> valori, BigDecimal tot, int scala) {
        List<BigDecimal[]> r = new ArrayList<>();
        BigDecimal cumulata = BigDecimal.ZERO;
        for (int k = 0; k < lista.size(); k++) {
            BigDecimal fine = k == lista.size() - 1 ? BigDecimal.ONE
                    : cumulata.add(valori.get(lista.get(k)).divide(tot, scala, RoundingMode.HALF_UP));
            r.add(new BigDecimal[]{cumulata, fine});
            cumulata = fine;
        }
        return r;
    }

    private static BigDecimal Quota(Ribilanciamento r, BigDecimal larghezza, BigDecimal[] intervallo, RoundingMode arrotondamento) {
        BigDecimal ampiezza = intervallo[1].subtract(intervallo[0]);
        if (ampiezza.signum() == 0) return BigDecimal.ZERO;
        int scala = Math.max(8, r.quantita.scale());
        return r.quantita.multiply(larghezza).divide(ampiezza, scala, arrotondamento);
    }

    private static void AggiustaResti(List<Pezzo> pezzi, boolean vendute) {
        Map<Ribilanciamento, Pezzo> ultimo = new IdentityHashMap<>();
        Map<Ribilanciamento, BigDecimal> somma = new IdentityHashMap<>();
        for (Pezzo p : pezzi) {
            Ribilanciamento r = vendute ? p.venduta : p.comprata;
            ultimo.put(r, p);
            somma.merge(r, vendute ? p.qtaVenduta : p.qtaComprata, BigDecimal::add);
        }
        for (Map.Entry<Ribilanciamento, Pezzo> e : ultimo.entrySet()) {
            BigDecimal resto = e.getKey().quantita.subtract(somma.get(e.getKey()));
            Pezzo p = e.getValue();
            if (vendute) p.qtaVenduta = p.qtaVenduta.add(resto);
            else p.qtaComprata = p.qtaComprata.add(resto);
        }
    }

    private static void PrendiUnita(List<Pezzo> pezzi, Pezzo vuoto, boolean vendute) {
        Ribilanciamento r = vendute ? vuoto.venduta : vuoto.comprata;
        BigDecimal unita = BigDecimal.ONE.movePointLeft(Math.max(8, r.quantita.scale()));
        Pezzo donatore = null;
        for (Pezzo p : pezzi) {
            if (p == vuoto || (vendute ? p.venduta : p.comprata) != r) continue;
            BigDecimal q = vendute ? p.qtaVenduta : p.qtaComprata;
            if (q.compareTo(unita) > 0 && (donatore == null || q.compareTo(vendute ? donatore.qtaVenduta : donatore.qtaComprata) > 0)) {
                donatore = p;
            }
        }
        if (donatore == null) return;
        if (vendute) {
            donatore.qtaVenduta = donatore.qtaVenduta.subtract(unita);
            vuoto.qtaVenduta = unita;
        } else {
            donatore.qtaComprata = donatore.qtaComprata.subtract(unita);
            vuoto.qtaComprata = unita;
        }
    }

    /**
     * Crea gli scambi di un ribilanciamento sul sotto-wallet, tutti o nessuno.
     * @return {@code null} se fatto, altrimenti il motivo
     */
    static String CreaRibilanciamento(List<Ribilanciamento> evento, String exchange, Prezzatore prezzatore) {
        List<Ribilanciamento> vendute = new ArrayList<>(), comprate = new ArrayList<>();
        for (Ribilanciamento r : evento) (r.segno < 0 ? vendute : comprate).add(r);
        if (vendute.isEmpty() || comprate.isEmpty()) return "solo monete " + (vendute.isEmpty() ? "comprate" : "vendute");
        long ts = evento.get(0).ts;
        Map<Ribilanciamento, BigDecimal> valori = new IdentityHashMap<>();
        for (Ribilanciamento r : evento) {
            BigDecimal v = prezzatore.Valore(r.moneta, r.quantita, ts);
            if (v == null || v.signum() <= 0) return "prezzo di " + r.moneta + " non trovato";
            valori.put(r, v);
        }
        List<Pezzo> pezzi = Ripartisci(vendute, comprate, valori);
        if (pezzi == null || pezzi.isEmpty()) return "ripartizione non riuscita";
        GeneratoreID gid = new GeneratoreID();
        List<String[]> gruppo = new ArrayList<>();
        String commissione = evento.get(0).commissione;
        String nota = "Ribilanciamento " + evento.get(0).indice
                + (Funzioni.noData(commissione) ? "" : " - commissione totale " + commissione + " compresa nelle quantità");
        for (Pezzo p : pezzi) {
            String[] v = Genera(NuovaMoneta(p.venduta.moneta, "Crypto", p.qtaVenduta.negate()),
                    NuovaMoneta(p.comprata.moneta, "Crypto", p.qtaComprata), exchange, WALLET_AUTO_INVEST, ts, null,
                    nota, ORIGINE_RIBILANCIAMENTO, ID_SCAMBIO, gid);
            if (v == null) return "impossibile creare lo scambio " + p.venduta.moneta + " -> " + p.comprata.moneta;
            gruppo.add(v);
        }
        if (AttesaConnessione.Abortita()) return "connessione persa durante la ricerca dei prezzi, non scritto";
        CollegaTutti(gruppo);
        for (String[] v : gruppo) {
            Importazioni.RiempiVuotiArray(v);
            MappaCryptoWallet.put(v[0], v);
        }
        return null;
    }

    /** Riscatto: il deposito diventa un giroconto dal sotto-wallet, con la sua gamba speculare in uscita. */
    static boolean CreaRiscatto(String[] riscatto) {
        GeneratoreID gid = new GeneratoreID();
        String[] spec = Genera(NuovaMoneta(riscatto[11], riscatto[12], Quantita(riscatto).negate()), null, riscatto[3],
                WALLET_AUTO_INVEST, TsRiga(riscatto), riscatto[15], null, ORIGINE_RISCATTO, ID_SPECULARE_USCITA, gid);
        if (spec == null) {
            LoggerGC.ScriviErrore("Auto-Invest: impossibile creare il giroconto del riscatto " + riscatto[0]);
            return false;
        }
        Speculare(spec, "PTW - Trasferimento Interno");
        spec[41] = riscatto[41];
        riscatto[5] = CAMPO5_ENTRATA;
        riscatto[18] = CAMPO18_ENTRATA;
        CollegaTutti(List.of(riscatto, spec));
        Importazioni.RiempiVuotiArray(spec);
        MappaCryptoWallet.put(spec[0], spec);
        return true;
    }

    /** Ogni riga porta in {@code [20]} gli ID di tutte le altre, come nel Dual Investment. */
    private static void CollegaTutti(List<String[]> righe) {
        for (String[] v : righe) {
            StringBuilder altri = new StringBuilder();
            for (String[] a : righe) {
                if (a == v) continue;
                if (altri.length() > 0) altri.append(',');
                altri.append(a[0]);
            }
            v[20] = altri.toString();
        }
    }

    // =============================================================================================
    // Verso dei ribilanciamenti
    // =============================================================================================

    /**
     * Decide per ogni riga di ribilanciamento se la moneta è stata comprata o venduta: il file dà solo la quantità.
     *
     * <p>Se il piano è stato <b>chiuso</b> (un riscatto dopo il quale non ci sono più acquisti né ribilanciamenti con
     * quantità), per ogni moneta la somma di acquisti, ribilanciamenti con il loro segno e riscatti deve fare
     * esattamente zero, e la giacenza non deve mai andare sotto zero. Sul dataset reale c'è una sola combinazione
     * di segni che lo fa, per ognuna delle 13 monete. Si cerca dividendo le righe in due metà (2^18 combinazioni per
     * metà al massimo). Se la combinazione non è unica, o il piano è ancora aperto, si usano i prezzi: in ogni
     * ribilanciamento il valore venduto è circa quello comprato.
     * @return {@code null} se ogni riga ha un segno, altrimenti il motivo
     */
    static String SegniRibilanciamenti(List<Acquisto> acquistiIndice, List<Ribilanciamento> ribilanciamenti,
            List<Riscatto> riscatti, Prezzatore prezzatore, Esito esito) {
        List<Ribilanciamento> conQuantita = new ArrayList<>();
        for (Ribilanciamento r : ribilanciamenti) if (r.quantita.signum() != 0) conQuantita.add(r);
        if (conQuantita.isEmpty()) return null;

        long ultimoRiscatto = riscatti.stream().mapToLong(r -> r.ts).max().orElse(Long.MIN_VALUE);
        boolean chiuso = !riscatti.isEmpty()
                && acquistiIndice.stream().noneMatch(a -> a.ts > ultimoRiscatto)
                && conQuantita.stream().noneMatch(r -> r.ts > ultimoRiscatto);

        Set<String> monete = new LinkedHashSet<>();
        for (Ribilanciamento r : conQuantita) monete.add(r.moneta.toUpperCase());
        Set<String> irrisolte = new LinkedHashSet<>();
        for (String moneta : monete) {
            List<Ribilanciamento> righe = new ArrayList<>();
            for (Ribilanciamento r : conQuantita) if (r.moneta.equalsIgnoreCase(moneta)) righe.add(r);
            righe.sort(Comparator.comparingLong(r -> r.ts));
            if (chiuso && righe.size() <= MAX_EVENTI_RICERCA_ESATTA && SegniPerQuadratura(moneta, righe, acquistiIndice, riscatti)) {
                esito.monetePerQuadratura++;
            } else {
                irrisolte.add(moneta);
            }
        }
        if (irrisolte.isEmpty()) return null;

        //Coi prezzi, evento per evento, tenendo conto della giacenza
        Map<String, BigDecimal> giacenze = new HashMap<>();
        TreeMap<Long, List<Object>> timeline = new TreeMap<>();
        for (Acquisto a : acquistiIndice) timeline.computeIfAbsent(a.ts, k -> new ArrayList<>()).add(a);
        for (Ribilanciamento r : conQuantita) timeline.computeIfAbsent(r.ts, k -> new ArrayList<>()).add(r);
        for (Riscatto r : riscatti) timeline.computeIfAbsent(r.ts, k -> new ArrayList<>()).add(r);
        for (Map.Entry<Long, List<Object>> t : timeline.entrySet()) {
            List<Ribilanciamento> evento = new ArrayList<>();
            for (Object o : t.getValue()) {
                if (o instanceof Acquisto a) giacenze.merge(a.moneta.toUpperCase(), a.unita, BigDecimal::add);
                else if (o instanceof Ribilanciamento r) evento.add(r);
            }
            if (!evento.isEmpty()) {
                String errore = SegniPerPrezzo(evento, irrisolte, giacenze, prezzatore);
                if (errore != null) return errore;
                for (Ribilanciamento r : evento) {
                    giacenze.merge(r.moneta.toUpperCase(), r.segno > 0 ? r.quantita : r.quantita.negate(), BigDecimal::add);
                }
            }
            for (Object o : t.getValue()) {
                if (o instanceof Riscatto r) giacenze.merge(r.moneta.toUpperCase(), r.quantita.negate(), BigDecimal::add);
            }
        }
        esito.monetePerPrezzo += irrisolte.size();
        return null;
    }

    /** Ricerca esatta dei segni di una moneta per un piano chiuso; scrive i segni solo se la soluzione è unica. */
    static boolean SegniPerQuadratura(String moneta, List<Ribilanciamento> righe, List<Acquisto> acquisti, List<Riscatto> riscatti) {
        int scala = 0;
        BigDecimal entrato = BigDecimal.ZERO, uscito = BigDecimal.ZERO;
        for (Acquisto a : acquisti) {
            if (!a.moneta.equalsIgnoreCase(moneta)) continue;
            entrato = entrato.add(a.unita);
            scala = Math.max(scala, a.unita.scale());
        }
        for (Riscatto r : riscatti) {
            if (!r.moneta.equalsIgnoreCase(moneta)) continue;
            uscito = uscito.add(r.quantita);
            scala = Math.max(scala, r.quantita.scale());
        }
        for (Ribilanciamento r : righe) scala = Math.max(scala, r.quantita.scale());
        if (scala > 12) return false;
        long[] q = new long[righe.size()];
        long obiettivo;
        try {
            for (int i = 0; i < q.length; i++) q[i] = righe.get(i).quantita.movePointRight(scala).longValueExact();
            obiettivo = uscito.subtract(entrato).movePointRight(scala).longValueExact();
        } catch (ArithmeticException ex) {
            return false;
        }
        List<Long> soluzioni = SoluzioniSegni(q, obiettivo, 64);
        if (soluzioni == null) return false;
        Long valida = null;
        for (long s : soluzioni) {
            if (!GiacenzaMaiNegativa(moneta, righe, s, acquisti, riscatti)) continue;
            if (valida != null) return false; //due soluzioni ammissibili: non si sceglie a caso
            valida = s;
        }
        if (valida == null) return false;
        for (int i = 0; i < righe.size(); i++) righe.get(i).segno = ((valida >> i) & 1L) != 0 ? -1 : 1;
        return true;
    }

    /**
     * Combinazioni di segni (bit 1 = venduta) con somma {@code obiettivo}, cercate dividendo in due metà.
     * @return le combinazioni, al più {@code massimo}; {@code null} se sono di più
     */
    static List<Long> SoluzioniSegni(long[] q, long obiettivo, int massimo) {
        int n = q.length;
        int n1 = n / 2, n2 = n - n1;
        Map<Long, List<Integer>> prima = new HashMap<>();
        for (int m = 0; m < (1 << n1); m++) {
            long s = 0;
            for (int i = 0; i < n1; i++) s += ((m >> i) & 1) != 0 ? -q[i] : q[i];
            prima.computeIfAbsent(s, k -> new ArrayList<>(1)).add(m);
        }
        List<Long> risultato = new ArrayList<>();
        for (int m = 0; m < (1 << n2); m++) {
            long s = 0;
            for (int i = 0; i < n2; i++) s += ((m >> i) & 1) != 0 ? -q[n1 + i] : q[n1 + i];
            List<Integer> compl = prima.get(obiettivo - s);
            if (compl == null) continue;
            for (int m1 : compl) {
                risultato.add(((long) m << n1) | m1);
                if (risultato.size() > massimo) return null;
            }
        }
        return risultato;
    }

    private static boolean GiacenzaMaiNegativa(String moneta, List<Ribilanciamento> righe, long segni,
            List<Acquisto> acquisti, List<Riscatto> riscatti) {
        //A parità di istante prima le entrate: un acquisto e una vendita nello stesso secondo non sono un problema
        TreeMap<Long, BigDecimal[]> movimenti = new TreeMap<>();
        for (Acquisto a : acquisti) {
            if (a.moneta.equalsIgnoreCase(moneta)) Somma(movimenti, a.ts, a.unita);
        }
        for (int i = 0; i < righe.size(); i++) {
            Ribilanciamento r = righe.get(i);
            Somma(movimenti, r.ts, ((segni >> i) & 1L) != 0 ? r.quantita.negate() : r.quantita);
        }
        for (Riscatto r : riscatti) {
            if (r.moneta.equalsIgnoreCase(moneta)) Somma(movimenti, r.ts, r.quantita.negate());
        }
        BigDecimal giacenza = BigDecimal.ZERO;
        for (BigDecimal[] m : movimenti.values()) {
            giacenza = giacenza.add(m[0]);
            if (giacenza.add(m[1]).signum() < 0) return false;
            giacenza = giacenza.add(m[1]);
        }
        return true;
    }

    private static void Somma(TreeMap<Long, BigDecimal[]> m, long ts, BigDecimal q) {
        BigDecimal[] v = m.computeIfAbsent(ts, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        if (q.signum() >= 0) v[0] = v[0].add(q);
        else v[1] = v[1].add(q);
    }

    /**
     * Segni delle monete irrisolte di un ribilanciamento, coi prezzi. Fra le combinazioni che non vendono più di
     * quanto c'è vince quella con lo sbilancio minimo fra venduto e comprato (che dovrebbe essere la sola commissione):
     * invertire una moneta lo sposta del doppio del suo valore. Lo sbilancio però resta identico invertendo <b>tutti</b>
     * i segni, e fra combinazioni a pari sbilancio (entro {@link #TOLLERANZA_PARI} del valore scambiato) decide il
     * peso uguale: vince quella che lascia i controvalori delle monete ribilanciate più vicini fra loro, scopo di un
     * indice "Equal-Weighted". Non è esatto: sul dataset reale lo sbilancio del verso giusto va dallo 0,1% all'1,2% del
     * valore scambiato, quindi una moneta da pochi centesimi resta incerta. Senza riscatto dà il verso esatto su 151
     * righe su 153 (sbagliate BTC per 0,35 euro e DOT per 0,10); col solo peso uguale erano 144, col solo sbilancio
     * un ribilanciamento successivo diventava impossibile. Vedi {@code Binance_AutoInvestDatasetRealeTest}.
     */
    static String SegniPerPrezzo(List<Ribilanciamento> evento, Set<String> irrisolte, Map<String, BigDecimal> giacenze,
            Prezzatore prezzatore) {
        List<Ribilanciamento> libere = new ArrayList<>();
        for (Ribilanciamento r : evento) if (irrisolte.contains(r.moneta.toUpperCase())) libere.add(r);
        if (libere.isEmpty()) return null;
        if (libere.size() > 20) return "troppe monete in un ribilanciamento del " + evento.get(0).data;
        Map<Ribilanciamento, BigDecimal> valori = new IdentityHashMap<>();
        BigDecimal fisso = BigDecimal.ZERO, totale = BigDecimal.ZERO;
        for (Ribilanciamento r : evento) {
            BigDecimal v = prezzatore.Valore(r.moneta, r.quantita, r.ts);
            if (v == null || v.signum() <= 0) return "prezzo di " + r.moneta + " del " + r.data + " non trovato";
            valori.put(r, v);
            totale = totale.add(v);
            if (!libere.contains(r)) fisso = fisso.add(r.segno > 0 ? v : v.negate());
        }
        //Lo sbilancio è la commissione più l'errore dei prezzi: oltre una certa misura i dati non tornano
        BigDecimal tetto = totale.divide(new BigDecimal(2), 10, RoundingMode.HALF_UP).multiply(SBILANCIO_MAX);
        BigDecimal meta = totale.divide(new BigDecimal(2), 10, RoundingMode.HALF_UP);
        List<int[]> ammesse = new ArrayList<>();
        List<BigDecimal> sbilanci = new ArrayList<>();
        BigDecimal minimo = null;
        for (int m = 0; m < (1 << libere.size()); m++) {
            BigDecimal sbilancio = fisso;
            boolean possibile = true;
            for (int i = 0; i < libere.size(); i++) {
                Ribilanciamento r = libere.get(i);
                boolean venduta = ((m >> i) & 1) != 0;
                if (venduta && giacenze.getOrDefault(r.moneta.toUpperCase(), BigDecimal.ZERO).compareTo(r.quantita) < 0) {
                    possibile = false;
                    break;
                }
                sbilancio = sbilancio.add(venduta ? valori.get(r).negate() : valori.get(r));
            }
            if (!possibile || sbilancio.abs().compareTo(tetto) > 0) continue;
            ammesse.add(new int[]{m});
            sbilanci.add(sbilancio.abs());
            if (minimo == null || sbilancio.abs().compareTo(minimo) < 0) minimo = sbilancio.abs();
        }
        BigDecimal miglioreDispersione = null;
        int scelta = -1;
        for (int k = 0; k < ammesse.size(); k++) {
            if (sbilanci.get(k).compareTo(minimo.add(meta.multiply(TOLLERANZA_PARI))) > 0) continue;
            int m = ammesse.get(k)[0];
            //Controvalore di ogni moneta dopo il ribilanciamento, al prezzo unitario del ribilanciamento
            List<BigDecimal> dopo = new ArrayList<>();
            for (Ribilanciamento r : evento) {
                boolean venduta = libere.contains(r) ? ((m >> libere.indexOf(r)) & 1) != 0 : r.segno < 0;
                BigDecimal q = giacenze.getOrDefault(r.moneta.toUpperCase(), BigDecimal.ZERO)
                        .add(venduta ? r.quantita.negate() : r.quantita);
                dopo.add(q.multiply(valori.get(r)).divide(r.quantita, 10, RoundingMode.HALF_UP));
            }
            BigDecimal media = dopo.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(dopo.size()), 10, RoundingMode.HALF_UP);
            BigDecimal dispersione = BigDecimal.ZERO;
            for (BigDecimal d : dopo) dispersione = dispersione.add(d.subtract(media).pow(2));
            if (miglioreDispersione == null || dispersione.compareTo(miglioreDispersione) < 0) {
                miglioreDispersione = dispersione;
                scelta = m;
            }
        }
        if (scelta < 0) return "il ribilanciamento del " + evento.get(0).data + " non quadra con i prezzi di mercato";
        for (int i = 0; i < libere.size(); i++) libere.get(i).segno = ((scelta >> i) & 1) != 0 ? -1 : 1;
        return null;
    }

}
