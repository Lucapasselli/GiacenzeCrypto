package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * L'archivio delle dichiarazioni generate: una cartella per ogni stampa dei quadri W/RW o T/RT, con i
 * PDF, l'Excel di dettaglio e un {@code manifest.json} che descrive cosa contengono.
 *
 * <p>Logica pura, senza Swing: la generazione dei file sta in {@code Principale} (che ha i dati dei
 * quadri), i dialoghi in {@link Principale_Dichiarazioni}, la parte grafica in
 * {@link GUI_ArchivioDichiarazioni_Pannello}.
 *
 * <p>Tre scelte su cui il resto si appoggia:
 * <ul>
 *   <li><b>Il manifest è l'unica fonte</b>, compresi il segno di <i>definitiva</i> e la nota. Un
 *       registro in {@code personale.mv.db} verrebbe riscritto da un ripristino, mentre le cartelle
 *       delle dichiarazioni al ripristino si aggiungono soltanto ({@link Backup_Restore}): flag e
 *       cartelle andrebbero ognuno per conto suo.</li>
 *   <li><b>Una dichiarazione esiste solo se tutti i suoi file sono stati scritti.</b> I file nascono in
 *       una cartella temporanea dentro l'archivio ({@link #NuovaCartellaTemporanea()}) e
 *       {@link #Registra} la rinomina nel nome definitivo solo alla fine: PDF ed Excel di una
 *       dichiarazione vengono sempre dallo stesso calcolo, oppure non c'è niente.</li>
 *   <li><b>Al massimo una definitiva per anno e famiglia</b> (W/RW e T/RT sono famiglie distinte). Se
 *       un ripristino ne riporta una seconda, vale quella segnata per ultima ({@link #Elenco}).</li>
 * </ul>
 */
public final class Dichiarazioni {

    /** Nome del file che descrive la dichiarazione dentro la sua cartella. */
    public static final String NOME_MANIFEST = "manifest.json";

    /** Prefisso delle cartelle ancora in scrittura: {@link #Elenco} le ignora. */
    static final String PREFISSO_TEMPORANEA = ".in_corso_";

    /** Versione del formato del manifest. */
    static final int FORMATO = 1;

    private static final DateTimeFormatter FMT_NOME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FMT_LEGGIBILE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Cartella radice dell'archivio; {@code null} = quella della directory di lavoro. Solo per i test. */
    static File RadiceDiProva = null;

    private Dichiarazioni() {
    }

    /** Le due famiglie di quadri: una definitiva per anno ciascuna. */
    public enum Famiglia {
        RW("W/RW", "RW"),
        RT("T/RT", "RT");

        /** Etichetta mostrata all'utente. */
        public final String Etichetta;
        /** Sigla usata nel nome della cartella e nel manifest. */
        public final String Sigla;

        Famiglia(String Etichetta, String Sigla) {
            this.Etichetta = Etichetta;
            this.Sigla = Sigla;
        }

        /** @return la famiglia con quella sigla, {@code null} se sconosciuta */
        public static Famiglia DaSigla(String s) {
            for (Famiglia f : values()) {
                if (f.Sigla.equalsIgnoreCase(s)) {
                    return f;
                }
            }
            return null;
        }
    }

    /**
     * Una dichiarazione: quello che il manifest descrive.
     *
     * <p>{@link #Righi}, {@link #Totali} e {@link #Opzioni} sono la fotografia dei valori stampati, presa
     * prima di scrivere i file. Servono al pannello (senza aprire PDF o Excel) e a riconoscere una
     * ristampa identica ({@link #Impronta()}).
     */
    public static final class Dichiarazione {

        /** La cartella; {@code null} per una bozza non ancora registrata. */
        public File Cartella;
        public Famiglia Fam;
        public String Anno = "";
        /** {@code yyyy-MM-dd HH:mm:ss}. */
        public String Creata = "";
        public String Versione = "";
        /** Descrizione degli errori presenti al momento della stampa, vuota se non ce n'erano. */
        public String Errori = "";
        /** Voce → valore, nell'ordine in cui vanno mostrati. */
        public final Map<String, String> Totali = new LinkedHashMap<>();
        /** Un rigo per ogni rigo del quadro: intestazione → valore. */
        public final List<Map<String, String>> Righi = new ArrayList<>();
        /** Opzione di calcolo → valore scelto. */
        public final Map<String, String> Opzioni = new LinkedHashMap<>();
        /** Nome del file → SHA-256, scritto da {@link #Registra}. */
        public final Map<String, String> File = new LinkedHashMap<>();
        public boolean Definitiva;
        /** Quando è stata segnata definitiva ({@code yyyy-MM-dd HH:mm:ss}), vuoto se non lo è. */
        public String DefinitivaDal = "";
        public String Nota = "";
        /**
         * Nome dell'archivio di backup creato quando è stata segnata definitiva ({@code ArchivioBackup/}),
         * vuoto se non c'è: è ciò che permette di ricostruire i dati da cui sono nati i valori dichiarati.
         */
        public String Backup = "";
        /** File allegati dall'utente (ricevuta, modello inviato): nome nella cartella → SHA-256. */
        public final Map<String, String> Allegati = new LinkedHashMap<>();

        /** @return il nome della cartella, che fa da identificativo; vuoto per una bozza */
        public String Id() {
            return Cartella == null ? "" : Cartella.getName();
        }

        /**
         * @return un'impronta di ciò che è stato dichiarato (anno, famiglia, totali, righi, opzioni):
         *         due stampe con la stessa impronta hanno prodotto gli stessi numeri
         */
        public String Impronta() {
            JSONObject o = new JSONObject();
            o.put("anno", Anno);
            o.put("famiglia", Fam == null ? "" : Fam.Sigla);
            o.put("totali", OggettoOrdinato(Totali));
            JSONArray r = new JSONArray();
            for (Map<String, String> rigo : Righi) {
                r.put(OggettoOrdinato(rigo));
            }
            o.put("righi", r);
            o.put("opzioni", OggettoOrdinato(Opzioni));
            o.put("errori", Errori);
            return Sha256(o.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    // =================================================================================================
    // CARTELLE
    // =================================================================================================
    /** @return la cartella radice dell'archivio (non necessariamente esistente) */
    public static File Radice() {
        return RadiceDiProva != null ? RadiceDiProva : new File(VarStatiche.getCartella_Dichiarazioni());
    }

    /**
     * Crea una cartella temporanea dentro l'archivio in cui scrivere i file di una nuova dichiarazione.
     * Sta nell'archivio e non in {@code Temporanei/} perché la rinomina finale resti sullo stesso disco.
     *
     * @return la cartella creata
     * @throws IOException se non è possibile crearla
     */
    public static File NuovaCartellaTemporanea() throws IOException {
        File radice = Radice();
        if (!radice.exists() && !radice.mkdirs()) {
            throw new IOException("Impossibile creare la cartella " + radice);
        }
        String base = PREFISSO_TEMPORANEA + LocalDateTime.now().format(FMT_NOME);
        File f = new File(radice, base);
        int n = 2;
        while (f.exists()) {
            f = new File(radice, base + "_" + n++);
        }
        if (!f.mkdirs()) {
            throw new IOException("Impossibile creare la cartella " + f);
        }
        return f;
    }

    /** Cancella una cartella temporanea e quello che contiene. Non fa nulla su {@code null}. */
    public static void Scarta(File Temporanea) {
        if (Temporanea != null) {
            CancellaRicorsivo(Temporanea);
        }
    }

    /**
     * Registra una dichiarazione: calcola gli SHA-256 dei file presenti nella cartella temporanea,
     * scrive il manifest e rinomina la cartella nel nome definitivo {@code <anno>_<sigla>_<data>}.
     *
     * <p>Se qualcosa fallisce la cartella temporanea viene cancellata e nulla risulta registrato.
     *
     * @param Temporanea cartella creata da {@link #NuovaCartellaTemporanea()}, con i file già scritti
     * @param Bozza la descrizione; {@code Creata} e {@code Versione}, se vuoti, vengono valorizzati qui
     * @param FileAttesi i nomi dei file che devono esserci, non vuoti
     * @return la dichiarazione registrata, con {@code Cartella} valorizzata
     * @throws IOException se manca un file atteso o non si riesce a scrivere/rinominare
     */
    public static Dichiarazione Registra(File Temporanea, Dichiarazione Bozza, List<String> FileAttesi) throws IOException {
        try {
            LocalDateTime adesso = LocalDateTime.now();
            if (Bozza.Creata.isEmpty()) {
                Bozza.Creata = adesso.format(FMT_LEGGIBILE);
            }
            if (Bozza.Versione.isEmpty()) {
                Bozza.Versione = VarStatiche.Versione == null ? "" : VarStatiche.Versione;
            }
            Bozza.File.clear();
            for (String nome : FileAttesi) {
                File f = new File(Temporanea, nome);
                if (!f.isFile() || f.length() == 0) {
                    throw new IOException("File " + nome + " non generato");
                }
                Bozza.File.put(nome, Sha256(f));
            }
            Files.write(new File(Temporanea, NOME_MANIFEST).toPath(),
                    ManifestAJson(Bozza).toString(2).getBytes(StandardCharsets.UTF_8));

            String base = Bozza.Anno + "_" + Bozza.Fam.Sigla + "_" + adesso.format(FMT_NOME);
            File dest = new File(Temporanea.getParentFile(), base);
            int n = 2;
            while (dest.exists()) {
                dest = new File(Temporanea.getParentFile(), base + "_" + n++);
            }
            Sposta(Temporanea, dest);
            Bozza.Cartella = dest;
            //Sola lettura: aperti dall'archivio, PDF ed Excel non si modificano per sbaglio (il programma
            //che li apre lo dice). Il manifest resta scrivibile per definitiva e nota.
            for (String nome : FileAttesi) {
                new File(dest, nome).setReadOnly();
            }
            return Bozza;
        } catch (IOException | RuntimeException e) {
            Scarta(Temporanea);
            throw e instanceof IOException io ? io : new IOException(e.getMessage(), e);
        }
    }

    // =================================================================================================
    // ELENCO E MODIFICHE
    // =================================================================================================
    /**
     * Legge tutte le dichiarazioni dell'archivio, dalla più recente.
     *
     * <p>Le cartelle temporanee rimaste da una generazione interrotta (più vecchie di un giorno) vengono
     * cancellate. Se per un anno e una famiglia risultano più definitive (succede dopo un ripristino che
     * ha riportato una cartella segnata in un altro momento), resta definitiva quella segnata per ultima
     * e le altre vengono corrette anche su disco.
     *
     * @return l'elenco, ordinato per anno decrescente e poi per data di creazione decrescente
     */
    public static List<Dichiarazione> Elenco() {
        List<Dichiarazione> elenco = new ArrayList<>();
        File[] cartelle = Radice().listFiles(File::isDirectory);
        if (cartelle == null) {
            return elenco;
        }
        long unGiornoFa = System.currentTimeMillis() - 86_400_000L;
        for (File c : cartelle) {
            if (c.getName().startsWith(PREFISSO_TEMPORANEA)) {
                if (c.lastModified() < unGiornoFa) {
                    CancellaRicorsivo(c);
                }
                continue;
            }
            if (c.getName().startsWith(".")) {
                continue;
            }
            Dichiarazione d = Leggi(c);
            if (d != null) {
                elenco.add(d);
            }
        }
        elenco.sort(Comparator.comparing((Dichiarazione d) -> d.Anno).reversed()
                .thenComparing(Comparator.comparing((Dichiarazione d) -> d.Creata).reversed())
                .thenComparing(Comparator.comparing(Dichiarazione::Id).reversed()));
        RisolviDefinitiveDoppie(elenco);
        return elenco;
    }

    /**
     * @return la dichiarazione descritta dal manifest della cartella, {@code null} se il manifest manca
     *         o non è leggibile
     */
    public static Dichiarazione Leggi(File Cartella) {
        File m = new File(Cartella, NOME_MANIFEST);
        if (!m.isFile()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(Files.readString(m.toPath(), StandardCharsets.UTF_8));
            Dichiarazione d = JsonAManifest(o);
            if (d.Fam == null) {
                return null;
            }
            d.Cartella = Cartella;
            return d;
        } catch (IOException | RuntimeException e) {
            System.out.println("Dichiarazioni.Leggi : manifest illeggibile in " + Cartella + " : " + e.getMessage());
            return null;
        }
    }

    /**
     * @return la definitiva di quell'anno e famiglia, {@code null} se non ce n'è
     */
    public static Dichiarazione Definitiva(Famiglia Fam, String Anno) {
        for (Dichiarazione d : Elenco()) {
            if (d.Definitiva && d.Fam == Fam && d.Anno.equals(Anno)) {
                return d;
            }
        }
        return null;
    }

    /**
     * @return la dichiarazione più recente di quell'anno e famiglia con la stessa impronta della bozza,
     *         {@code null} se l'ultima registrata è diversa (o non ce n'è)
     */
    public static Dichiarazione UltimaUguale(Dichiarazione Bozza) {
        String impronta = Bozza.Impronta();
        for (Dichiarazione d : Elenco()) {
            if (d.Fam == Bozza.Fam && d.Anno.equals(Bozza.Anno)) {
                return impronta.equals(d.Impronta()) ? d : null;
            }
        }
        return null;
    }

    /**
     * Segna (o toglie) la definitiva. Segnarla toglie il segno alle altre dichiarazioni dello stesso anno
     * e famiglia.
     *
     * @throws IOException se non si riesce a riscrivere un manifest
     */
    public static void ImpostaDefinitiva(Dichiarazione D, boolean Definitiva) throws IOException {
        if (Definitiva) {
            for (Dichiarazione altra : Elenco()) {
                if (altra.Definitiva && altra.Fam == D.Fam && altra.Anno.equals(D.Anno)
                        && !altra.Id().equals(D.Id())) {
                    altra.Definitiva = false;
                    altra.DefinitivaDal = "";
                    Riscrivi(altra);
                }
            }
            D.Definitiva = true;
            D.DefinitivaDal = LocalDateTime.now().format(FMT_LEGGIBILE);
        } else {
            D.Definitiva = false;
            D.DefinitivaDal = "";
        }
        Riscrivi(D);
    }

    /** Sostituisce la nota della dichiarazione. */
    public static void ImpostaNota(Dichiarazione D, String Nota) throws IOException {
        D.Nota = Nota == null ? "" : Nota.strip();
        Riscrivi(D);
    }

    /** Imposta il nome del backup collegato alla dichiarazione. */
    public static void ImpostaBackup(Dichiarazione D, String NomeBackup) throws IOException {
        D.Backup = NomeBackup == null ? "" : NomeBackup;
        Riscrivi(D);
    }

    /** Prefisso dei file allegati nella cartella, così non si confondono con quelli generati. */
    static final String PREFISSO_ALLEGATO = "Allegato - ";

    /**
     * Copia un file nella cartella della dichiarazione come allegato (sola lettura, con SHA-256 nel manifest).
     *
     * @return il nome dato all'allegato nella cartella
     * @throws IOException se la copia o il manifest non si scrivono
     */
    public static String AggiungiAllegato(Dichiarazione D, File Sorgente) throws IOException {
        String base = PREFISSO_ALLEGATO + Sorgente.getName().replace(";", " ");
        String nome = base;
        int n = 2;
        while (new File(D.Cartella, nome).exists()) {
            int punto = base.lastIndexOf('.');
            nome = punto > PREFISSO_ALLEGATO.length()
                    ? base.substring(0, punto) + " (" + n + ")" + base.substring(punto)
                    : base + " (" + n + ")";
            n++;
        }
        File dest = new File(D.Cartella, nome);
        Files.copy(Sorgente.toPath(), dest.toPath());
        try {
            D.Allegati.put(nome, Sha256(dest));
            Riscrivi(D);
        } catch (IOException | RuntimeException e) {
            D.Allegati.remove(nome);
            dest.delete();
            throw e;
        }
        dest.setReadOnly();
        return nome;
    }

    /** Toglie un allegato: lo cancella dalla cartella e dal manifest. */
    public static void RimuoviAllegato(Dichiarazione D, String Nome) throws IOException {
        if (!D.Allegati.containsKey(Nome)) {
            return;
        }
        D.Allegati.remove(Nome);
        Riscrivi(D);
        File f = new File(D.Cartella, Nome);
        f.setWritable(true);
        Files.deleteIfExists(f.toPath());
    }

    /**
     * @return nome dell'archivio di backup → la definitiva che lo cita, per avvisare prima di cancellarlo
     */
    public static Map<String, Dichiarazione> BackupDelleDefinitive() {
        Map<String, Dichiarazione> m = new LinkedHashMap<>();
        for (Dichiarazione d : Elenco()) {
            if (d.Definitiva && !d.Backup.isBlank()) {
                m.put(d.Backup, d);
            }
        }
        return m;
    }

    // =================================================================================================
    // CONFRONTO FRA DUE DICHIARAZIONI
    // =================================================================================================
    /**
     * Le differenze fra due fotografie degli stessi quadri: totali, righi (riconosciuti per nome, natura e
     * periodo, o per tipologia), errori e opzioni di calcolo. I numeri si confrontano come numeri, così
     * {@code 2.0} e {@code 2.00} sono uguali.
     *
     * @return una riga per differenza: {@code {voce, valore in A, valore in B}}, vuoto se coincidono
     */
    public static List<String[]> Confronta(Dichiarazione A, Dichiarazione B) {
        List<String[]> diff = new ArrayList<>();
        ConfrontaMappe("", A.Totali, B.Totali, diff);
        Map<String, Map<String, String>> ra = RighiPerChiave(A);
        Map<String, Map<String, String>> rb = RighiPerChiave(B);
        java.util.Set<String> chiavi = new java.util.LinkedHashSet<>(ra.keySet());
        chiavi.addAll(rb.keySet());
        for (String k : chiavi) {
            Map<String, String> x = ra.get(k);
            Map<String, String> y = rb.get(k);
            if (x == null) {
                diff.add(new String[]{"Rigo " + k, "(assente)", "presente"});
            } else if (y == null) {
                diff.add(new String[]{"Rigo " + k, "presente", "(assente)"});
            } else {
                ConfrontaMappe("Rigo " + k + ": ", x, y, diff);
            }
        }
        if (!A.Errori.equals(B.Errori)) {
            diff.add(new String[]{"Errori", A.Errori.isBlank() ? "nessuno" : A.Errori, B.Errori.isBlank() ? "nessuno" : B.Errori});
        }
        ConfrontaMappe("Opzione: ", A.Opzioni, B.Opzioni, diff);
        return diff;
    }

    /** Campi che identificano un rigo invece di essere suoi valori. */
    private static final List<String> CAMPI_CHIAVE = List.of("Rigo", "Natura", "Periodo", "Tipologia");

    private static Map<String, Map<String, String>> RighiPerChiave(Dichiarazione D) {
        Map<String, Map<String, String>> m = new LinkedHashMap<>();
        for (Map<String, String> r : D.Righi) {
            StringBuilder k = new StringBuilder();
            for (String c : CAMPI_CHIAVE) {
                String v = r.getOrDefault(c, "");
                if (!v.isBlank() && !"CRYPTO".equals(v)) {
                    k.append(k.length() > 0 ? " - " : "").append(v);
                }
            }
            String chiave = k.toString();
            int n = 2;
            while (m.containsKey(chiave)) {
                chiave = k + " (" + n++ + ")";
            }
            Map<String, String> valori = new LinkedHashMap<>(r);
            CAMPI_CHIAVE.forEach(valori::remove);
            m.put(chiave, valori);
        }
        return m;
    }

    private static void ConfrontaMappe(String Prefisso, Map<String, String> A, Map<String, String> B, List<String[]> Diff) {
        java.util.Set<String> chiavi = new java.util.LinkedHashSet<>(A.keySet());
        chiavi.addAll(B.keySet());
        for (String k : chiavi) {
            String a = A.getOrDefault(k, "");
            String b = B.getOrDefault(k, "");
            if (!Uguali(a, b)) {
                Diff.add(new String[]{Prefisso + k, a, b});
            }
        }
    }

    private static boolean Uguali(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        try {
            return new java.math.BigDecimal(a.trim()).compareTo(new java.math.BigDecimal(b.trim())) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Cancella la cartella della dichiarazione e tutto il suo contenuto. */
    public static boolean Elimina(Dichiarazione D) {
        if (D == null || D.Cartella == null) {
            return false;
        }
        CancellaRicorsivo(D.Cartella);
        return !D.Cartella.exists();
    }

    /** Esito della verifica di un file della dichiarazione. */
    public enum StatoFile {
        INTEGRO("integro"),
        MANCANTE("mancante"),
        ALTERATO("modificato dopo la generazione");

        public final String Descrizione;

        StatoFile(String Descrizione) {
            this.Descrizione = Descrizione;
        }
    }

    /**
     * Confronta ogni file con lo SHA-256 scritto nel manifest.
     *
     * @return nome del file → esito, nell'ordine del manifest
     */
    public static Map<String, StatoFile> VerificaIntegrita(Dichiarazione D) {
        Map<String, StatoFile> esito = new LinkedHashMap<>();
        Map<String, String> tutti = new LinkedHashMap<>(D.File);
        tutti.putAll(D.Allegati);
        for (Map.Entry<String, String> e : tutti.entrySet()) {
            File f = new File(D.Cartella, e.getKey());
            if (!f.isFile()) {
                esito.put(e.getKey(), StatoFile.MANCANTE);
                continue;
            }
            try {
                esito.put(e.getKey(), e.getValue().equalsIgnoreCase(Sha256(f)) ? StatoFile.INTEGRO : StatoFile.ALTERATO);
            } catch (IOException ex) {
                esito.put(e.getKey(), StatoFile.MANCANTE);
            }
        }
        return esito;
    }

    // =================================================================================================
    // CONTINUITÀ FRA DUE ANNI (W/RW)
    // =================================================================================================
    /** Opzione del manifest W/RW con il metodo di calcolo dei giorni di detenzione. */
    public static final String OPZIONE_METODO_RW = "Metodo";
    /**
     * Il metodo W/RW in cui il valore iniziale di un anno è il valore finale dell'anno prima: l'unico con
     * cui il confronto fra due anni ha senso. Con gli altri i lotti si valorizzano al carico, e valore
     * finale e iniziale differiscono per costruzione.
     */
    public static final String METODO_RW_SOLO_VALORI_INIZIO_FINE = "Solo valori di inizio e fine anno";

    /** Esito di {@link #ConfrontaContinuita}. */
    public static final class Continuita {

        /** Perché il confronto non si fa; vuoto se è stato fatto. */
        public String NonVerificabile = "";
        /** Righi confrontati. */
        public int Confrontati;
        /** Righi il cui valore finale dell'anno prima non coincide col valore iniziale dell'anno dopo. */
        public final List<String> Differenze = new ArrayList<>();
        /** Righi presenti in uno solo dei due anni, o divisi per periodo (quindi non confrontabili). */
        public final List<String> NonConfrontati = new ArrayList<>();
    }

    /**
     * Confronta, rigo per rigo, il valore finale della dichiarazione W/RW di un anno con il valore iniziale
     * di quella dell'anno dopo.
     *
     * <p>Il confronto si fa solo se le due dichiarazioni hanno <b>le stesse opzioni</b> e il metodo
     * {@link #METODO_RW_SOLO_VALORI_INIZIO_FINE}: con gli altri metodi, o con opzioni diverse, i due valori
     * differiscono per costruzione e il confronto darebbe solo falsi allarmi. Si confrontano i righi crypto
     * interi del gruppo (non la liquidità FIAT, il cui valore finale può essere una giacenza media, né i
     * righi divisi per periodo), arrotondati all'euro come nel quadro.
     *
     * @param Prec la dichiarazione dell'anno prima
     * @param Succ quella dell'anno dopo
     */
    public static Continuita ConfrontaContinuita(Dichiarazione Prec, Dichiarazione Succ) {
        Continuita c = new Continuita();
        if (Prec == null || Succ == null || Prec.Fam != Famiglia.RW || Succ.Fam != Famiglia.RW) {
            c.NonVerificabile = "Il confronto vale solo fra due dichiarazioni W/RW.";
            return c;
        }
        if (!METODO_RW_SOLO_VALORI_INIZIO_FINE.equals(Prec.Opzioni.get(OPZIONE_METODO_RW))
                || !METODO_RW_SOLO_VALORI_INIZIO_FINE.equals(Succ.Opzioni.get(OPZIONE_METODO_RW))) {
            c.NonVerificabile = "Il confronto ha senso solo se entrambe le dichiarazioni sono calcolate con il metodo \""
                    + METODO_RW_SOLO_VALORI_INIZIO_FINE + "\": con gli altri metodi valore finale e valore iniziale differiscono per costruzione.";
            return c;
        }
        List<String> diverse = new ArrayList<>();
        java.util.Set<String> chiavi = new java.util.TreeSet<>(Prec.Opzioni.keySet());
        chiavi.addAll(Succ.Opzioni.keySet());
        for (String k : chiavi) {
            if (!Prec.Opzioni.getOrDefault(k, "").equals(Succ.Opzioni.getOrDefault(k, ""))) {
                diverse.add(k);
            }
        }
        if (!diverse.isEmpty()) {
            c.NonVerificabile = "Le due dichiarazioni sono calcolate con opzioni diverse (" + String.join(", ", diverse) + ").";
            return c;
        }
        Map<String, String> finali = RighiCryptoInteri(Prec, "Valore finale", c.NonConfrontati);
        Map<String, String> iniziali = RighiCryptoInteri(Succ, "Valore iniziale", c.NonConfrontati);
        for (Map.Entry<String, String> e : finali.entrySet()) {
            String iniziale = iniziali.remove(e.getKey());
            if (iniziale == null) {
                c.NonConfrontati.add(e.getKey() + ": solo nel " + Prec.Anno);
                continue;
            }
            c.Confrontati++;
            java.math.BigDecimal f = Euro(e.getValue());
            java.math.BigDecimal i = Euro(iniziale);
            if (f.compareTo(i) != 0) {
                c.Differenze.add(e.getKey() + ": valore finale " + Prec.Anno + " " + f.toPlainString()
                        + ", valore iniziale " + Succ.Anno + " " + i.toPlainString());
            }
        }
        for (String k : iniziali.keySet()) {
            c.NonConfrontati.add(k + ": solo nel " + Succ.Anno);
        }
        return c;
    }

    /** Rigo → valore della colonna indicata, per i soli righi crypto non divisi per periodo. */
    private static Map<String, String> RighiCryptoInteri(Dichiarazione D, String Colonna, List<String> Esclusi) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map<String, String> r : D.Righi) {
            if ("FIAT".equalsIgnoreCase(r.getOrDefault("Natura", ""))) {
                continue;
            }
            String nome = r.getOrDefault("Rigo", "");
            if (!r.getOrDefault("Periodo", "").isBlank()) {
                String voce = nome + ": diviso per periodi nel " + D.Anno;
                if (!Esclusi.contains(voce)) {
                    Esclusi.add(voce);
                }
                continue;
            }
            m.put(nome, r.getOrDefault(Colonna, "0"));
        }
        return m;
    }

    private static java.math.BigDecimal Euro(String v) {
        try {
            return new java.math.BigDecimal(v.trim()).setScale(0, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException | NullPointerException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    // =================================================================================================
    // INTERNI
    // =================================================================================================
    /** Lascia definitiva una sola dichiarazione per anno e famiglia: quella segnata per ultima. */
    private static void RisolviDefinitiveDoppie(List<Dichiarazione> Elenco) {
        Map<String, Dichiarazione> vincente = new LinkedHashMap<>();
        for (Dichiarazione d : Elenco) {
            if (!d.Definitiva) {
                continue;
            }
            String chiave = d.Fam.Sigla + "|" + d.Anno;
            Dichiarazione v = vincente.get(chiave);
            if (v == null || PiuRecente(d, v)) {
                vincente.put(chiave, d);
            }
        }
        for (Dichiarazione d : Elenco) {
            if (d.Definitiva && vincente.get(d.Fam.Sigla + "|" + d.Anno) != d) {
                d.Definitiva = false;
                d.DefinitivaDal = "";
                try {
                    Riscrivi(d);
                } catch (IOException e) {
                    System.out.println("Dichiarazioni : manifest non corretto in " + d.Cartella + " : " + e.getMessage());
                }
            }
        }
    }

    /** {@code true} se {@code a} è stata segnata dopo {@code b}; a parità, la più recente di creazione. */
    private static boolean PiuRecente(Dichiarazione a, Dichiarazione b) {
        int c = a.DefinitivaDal.compareTo(b.DefinitivaDal);
        if (c != 0) {
            return c > 0;
        }
        c = a.Creata.compareTo(b.Creata);
        return c != 0 ? c > 0 : a.Id().compareTo(b.Id()) > 0;
    }

    /** Riscrive il manifest passando da un file temporaneo, così un errore a metà non lo tronca. */
    private static void Riscrivi(Dichiarazione D) throws IOException {
        File m = new File(D.Cartella, NOME_MANIFEST);
        File tmp = new File(D.Cartella, NOME_MANIFEST + ".tmp");
        Files.write(tmp.toPath(), ManifestAJson(D).toString(2).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp.toPath(), m.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), m.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void Sposta(File Da, File A) throws IOException {
        try {
            Files.move(Da.toPath(), A.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(Da.toPath(), A.toPath());
        }
    }

    static JSONObject ManifestAJson(Dichiarazione D) {
        JSONObject o = new JSONObject();
        o.put("formato", FORMATO);
        o.put("famiglia", D.Fam.Sigla);
        o.put("anno", D.Anno);
        o.put("creata", D.Creata);
        o.put("versione", D.Versione);
        o.put("errori", D.Errori);
        o.put("definitiva", D.Definitiva);
        o.put("definitivaDal", D.DefinitivaDal);
        o.put("nota", D.Nota);
        o.put("backup", D.Backup);
        o.put("totali", Coppie(D.Totali));
        JSONArray r = new JSONArray();
        for (Map<String, String> rigo : D.Righi) {
            r.put(Coppie(rigo));
        }
        o.put("righi", r);
        o.put("opzioni", Coppie(D.Opzioni));
        o.put("file", Coppie(D.File));
        o.put("allegati", Coppie(D.Allegati));
        return o;
    }

    static Dichiarazione JsonAManifest(JSONObject o) {
        Dichiarazione d = new Dichiarazione();
        d.Fam = Famiglia.DaSigla(o.optString("famiglia"));
        d.Anno = o.optString("anno");
        d.Creata = o.optString("creata");
        d.Versione = o.optString("versione");
        d.Errori = o.optString("errori");
        d.Definitiva = o.optBoolean("definitiva");
        d.DefinitivaDal = o.optString("definitivaDal");
        d.Nota = o.optString("nota");
        d.Backup = o.optString("backup");
        LeggiCoppie(o.optJSONArray("totali"), d.Totali);
        JSONArray r = o.optJSONArray("righi");
        if (r != null) {
            for (int i = 0; i < r.length(); i++) {
                Map<String, String> rigo = new LinkedHashMap<>();
                LeggiCoppie(r.optJSONArray(i), rigo);
                d.Righi.add(rigo);
            }
        }
        LeggiCoppie(o.optJSONArray("opzioni"), d.Opzioni);
        LeggiCoppie(o.optJSONArray("file"), d.File);
        LeggiCoppie(o.optJSONArray("allegati"), d.Allegati);
        return d;
    }

    /**
     * Una mappa come array di coppie {@code [chiave, valore]}: un {@code JSONObject} non conserva
     * l'ordine, e l'ordine delle voci è quello in cui il pannello le mostra.
     */
    private static JSONArray Coppie(Map<String, String> m) {
        JSONArray a = new JSONArray();
        for (Map.Entry<String, String> e : m.entrySet()) {
            a.put(new JSONArray().put(e.getKey()).put(e.getValue() == null ? "" : e.getValue()));
        }
        return a;
    }

    private static void LeggiCoppie(JSONArray a, Map<String, String> m) {
        if (a == null) {
            return;
        }
        for (int i = 0; i < a.length(); i++) {
            JSONArray c = a.optJSONArray(i);
            if (c != null && c.length() >= 2) {
                m.put(c.optString(0), c.optString(1));
            }
        }
    }

    /** Per l'impronta: chiavi ordinate, così conta il contenuto e non l'ordine di inserimento. */
    private static JSONObject OggettoOrdinato(Map<String, String> m) {
        return new JSONObject(new java.util.TreeMap<>(m));
    }

    static String Sha256(File f) throws IOException {
        try (InputStream in = Files.newInputStream(f.toPath())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String Sha256(byte[] dati) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dati));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return {@code true} per i file di blocco che i programmi d'ufficio lasciano accanto a un file
     *         aperto ({@code .~lock.*#} di LibreOffice, {@code ~$*} di Office): non fanno parte della
     *         dichiarazione e restano fuori da backup ed esportazioni
     */
    public static boolean FileDiBlocco(String Nome) {
        return Nome.startsWith(".~lock.") || Nome.startsWith("~$");
    }

    private static void CancellaRicorsivo(File f) {
        //Su Windows un file di sola lettura non si cancella
        f.setWritable(true);
        File[] figli = f.listFiles();
        if (figli != null) {
            for (File c : figli) {
                CancellaRicorsivo(c);
            }
        }
        if (!f.delete() && f.exists()) {
            System.out.println("Dichiarazioni : impossibile cancellare " + f);
        }
    }
}
