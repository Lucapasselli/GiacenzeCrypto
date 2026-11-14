package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali.*;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Ripristino delle modifiche manuali dei movimenti da un file di {@link Principale_Opzioni_ModificheManuali}, su un
 * archivio reimportato. <b>Fase 1</b>: lettore e anteprima. <b>Fase 2</b>: applicazione delle modifiche
 * puntuali, delle rinomine (trasla orario), delle unioni, dei movimenti inseriti a mano e dello storico.
 * <b>Fase 3</b>: le classificazioni, rimettendo il gruppo com'è nel file.
 *
 * <p>Decisioni dell'utente (2026-11-09): vale anche su un archivio pulito, senza {@code personale.mv.db} (quindi lo
 * storico si ricostruisce dai record {@code PASSAGGIO}), un conflitto si <b>segnala</b> e non si applica, si
 * ripristina tutto comprese le classificazioni, le cancellazioni non sono registrate.</p>
 *
 * <p><b>Un solo piano.</b> {@link #Pianifica} abbina il file all'archivio e produce, per ogni modifica, una
 * {@link Voce} (l'esito) e, se è applicabile, la sua azione già legata ai movimenti abbinati. {@link #Applica}
 * esegue <i>solo quelle azioni</i>: non riabbina mai, quindi l'anteprima e l'applicazione non possono divergere.
 * Ogni azione ricontrolla le sue precondizioni (il movimento è ancora in archivio con quell'ID, l'ID finale è
 * libero): se non reggono non fa nulla, e la voce risulta saltata. Applicare due volte dà, al secondo giro, tutto
 * «già applicato».</p>
 *
 * <p><b>Come si abbina.</b> Prima per ID, poi per chiave logica ({@link Importazioni#F_buildKeyMovimento}: istante,
 * exchange, monete, quantità), dando ogni movimento dell'archivio a una riga sola. È una copia in piccolo di ciò che
 * fa {@code FormeImportate.Indice}, che non si può riusare perché legge {@code MappaCryptoWallet} e il suo scopo è
 * l'importazione. Una modifica puntuale è <i>applicabile</i> solo se ogni campo che cambierebbe vale ancora quello
 * della riga iniziale (se vale già quello finale è <i>già applicata</i>, altrimenti è un <i>conflitto</i>). Nel
 * confronto fra file e archivio si ignorano i campi ricalcolati
 * ({@link Principale_Opzioni_ModificheManuali#CAMPI_NON_CONFRONTATI}) e il documento di origine ({@code [41]}), che in
 * un archivio nuovo ha un altro id.</p>
 *
 * <p><b>Che cosa scrive.</b> Una modifica puntuale o una rinomina riscrive nella riga abbinata i soli campi cambiati
 * fra la riga iniziale e la finale (ID compreso, con la chiave della mappa) e il lignaggio in {@code [42]}. Un'unione
 * toglie le N righe abbinate e mette la riga finale con {@code [41]} unione di quelli abbinati
 * ({@link DocumentiFonte#Unione}), non quello del file. Un movimento inserito a mano si inserisce com'è, con
 * {@code [41]} e {@code [42]} vuoti (il documento di origine del vecchio archivio non esiste nel nuovo). I campi
 * ricalcolati dal motore non si toccano: li rifà {@code AggiornaPlusvalenze}. Lo storico si rimette dai
 * {@code PASSAGGIO} con la data originale, saltando le voci che l'archivio ha già.</p>
 *
 * <p><b>Le classificazioni</b> non si rieseguono (la logica è nei metodi privati del dialogo e dipende dalle scelte
 * dell'utente): il gruppo si rimette com'è nel file, perché la classificazione non scrive altro che righe di
 * movimento. Ogni originale si ritrova per ID (anche nella forma di partenza di uno scambio differito) o per chiave
 * con la quantità di prima della classificazione, e <b>se è in archivio con un'altra forma è un conflitto</b>. Si
 * copiano tutti i suoi campi dal file tranne quelli ricalcolati, il documento di origine e il lignaggio, e si
 * inseriscono le righe generate ({@code AU}) con l'origine del movimento corrispondente del nuovo archivio.
 * L'azione è tutto o niente. I <b>documenti di supporto</b> collegati dalla classificazione (Dual Investment,
 * Subscription...) si perdono: hanno gli id del vecchio archivio. Una catena il cui movimento finale è anche in un
 * gruppo (la classificazione è venuta dopo la modifica) è <i>rinviata</i> al gruppo: la riga finale del file ha già
 * entrambe, il gruppo toglie i movimenti di partenza, mette lignaggio e storico della catena.</p>
 *
 * <p><b>Non si applica</b>: una separazione dopo la modifica, i movimenti marcati {@code M} di cui non si conosce il
 * prima, i movimenti poi cancellati.</p>
 */
public class Principale_Opzioni_ModificheManuali_Ripristino {

    private Principale_Opzioni_ModificheManuali_Ripristino() {
    }

    /** Il file non è un'esportazione che questa versione sa leggere. */
    public static class FileNonValido extends Exception {

        public FileNonValido(String messaggio) {
            super(messaggio);
        }
    }

    /** Il contenuto letto: formato, numero di campi e record (colonne di servizio + campi). */
    public record Contenuto(int formato, int campi, List<String[]> record) {

    }

    public enum Esito {
        /** Ritrovata e applicabile. */
        APPLICABILE("applicabili", "applicabile"),
        /** L'archivio ha già il risultato finale. */
        GIA_APPLICATA("già applicate", "già applicata"),
        /** Ritrovata, ma l'archivio ha valori diversi sia dall'iniziale sia dal finale: non si applica. */
        CONFLITTO("in conflitto", "conflitto"),
        /** Il movimento di partenza non c'è nell'archivio. */
        NON_TROVATO("non ritrovate", "non ritrovata"),
        /** Presente, ma lo stato di partenza non è noto: non si può dire se applicarla. */
        DA_VERIFICARE("da verificare", "da verificare"),
        /** Il file ne conserva l'informazione, ma va rifatta a mano (una separazione dopo la modifica). */
        MANUALE("da rifare a mano", "da rifare a mano"),
        /** Il movimento è anche in un gruppo classificato: si applica insieme al gruppo. */
        RINVIATA("rinviate alla classificazione", "con la classificazione"),
        /** Niente da ripristinare (movimento poi cancellato). */
        NESSUNA_AZIONE("senza azione", "senza azione");

        /** Al plurale, per i conteggi del riepilogo. */
        final String etichetta;
        /** Al singolare, per una riga sola. */
        final String testo;

        Esito(String etichetta, String testo) {
            this.etichetta = etichetta;
            this.testo = testo;
        }

        /** @return il nome dell'esito in italiano, al singolare */
        public String testo() {
            return testo;
        }
    }

    /**
     * Una riga dell'anteprima: che cosa, di quale chiave del file, con che esito e perché. {@code quando} e
     * {@code movimento} dicono a chi sceglie quale movimento è (data e "exchange: uscita -> entrata"), vuoti se non noti.
     * {@code dettaglio} è il perché in una riga sola, con i valori abbreviati, per l'elenco e il rapporto; {@code completo}
     * è lo stesso su più righe e <b>senza abbreviazioni</b>, per il pannello di dettaglio (uguale a {@code dettaglio}
     * quando non c'è altro da dire).
     */
    public record Voce(String tipo, String chiave, Esito esito, String dettaglio, String quando, String movimento,
            String completo) {

        public Voce(String tipo, String chiave, Esito esito, String dettaglio, String quando, String movimento) {
            this(tipo, chiave, esito, dettaglio, quando, movimento, dettaglio);
        }

        public Voce(String tipo, String chiave, Esito esito, String dettaglio) {
            this(tipo, chiave, esito, dettaglio, "", "", dettaglio);
        }

        /** La stessa voce con data e descrizione del movimento presi da una riga. */
        Voce con(String[] riga) {
            return new Voce(tipo, chiave, esito, dettaglio, Valore(riga, 1), Descrizione(riga), completo);
        }

        /** La stessa voce con il dettaglio completo, su più righe e senza abbreviazioni. */
        Voce conCompleto(String testoCompleto) {
            return new Voce(tipo, chiave, esito, dettaglio, quando, movimento, testoCompleto);
        }
    }

    /** "Exchange: qta moneta -> qta moneta", per riconoscere un movimento in un elenco. */
    static String Descrizione(String[] riga) {
        StringBuilder sb = new StringBuilder(Valore(riga, 3));
        String uscita = Valore(riga, 10).isEmpty() || Uguali(Valore(riga, 10), "0") ? "" : Valore(riga, 10) + " " + Valore(riga, 8);
        String entrata = Valore(riga, 13).isEmpty() || Uguali(Valore(riga, 13), "0") ? "" : Valore(riga, 13) + " " + Valore(riga, 11);
        if (!uscita.isEmpty() || !entrata.isEmpty()) {
            sb.append(": ").append(uscita).append(uscita.isEmpty() || entrata.isEmpty() ? "" : " -> ").append(entrata);
        }
        return sb.toString().trim();
    }

    /** L'anteprima: le voci e i conteggi. */
    public record Anteprima(List<Voce> voci) {

        /** @return quante voci hanno quell'esito */
        public long conta(Esito e) {
            return voci.stream().filter(v -> v.esito() == e).count();
        }

        /** Conteggio per tipo ed esito, una riga per tipo. */
        public String riepilogo() {
            Map<String, Map<Esito, Integer>> perTipo = new LinkedHashMap<>();
            for (Voce v : voci) {
                perTipo.computeIfAbsent(v.tipo(), k -> new EnumMap<>(Esito.class)).merge(v.esito(), 1, Integer::sum);
            }
            if (perTipo.isEmpty()) {
                return "Il file non contiene modifiche.\n";
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Map<Esito, Integer>> e : perTipo.entrySet()) {
                int totale = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
                sb.append(e.getKey()).append(": ").append(totale);
                List<String> parti = new ArrayList<>();
                for (Map.Entry<Esito, Integer> x : e.getValue().entrySet()) {
                    parti.add(x.getValue() + " " + x.getKey().etichetta);
                }
                sb.append(" (").append(String.join(", ", parti)).append(")\n");
            }
            return sb.toString();
        }

        /** Le voci che richiedono attenzione (conflitti, non ritrovate, da verificare, a mano), una per riga. */
        public List<String> problemi() {
            List<String> righe = new ArrayList<>();
            for (Voce v : voci) {
                if (v.esito() == Esito.CONFLITTO || v.esito() == Esito.NON_TROVATO
                        || v.esito() == Esito.DA_VERIFICARE || v.esito() == Esito.MANUALE) {
                    righe.add(v.tipo() + " " + v.chiave() + " - " + v.esito().etichetta + ": " + v.dettaglio());
                }
            }
            return righe;
        }

        /** Il rapporto completo, tutte le voci. */
        public String rapporto() {
            StringBuilder sb = new StringBuilder(riepilogo()).append("\n");
            for (Voce v : voci) {
                sb.append(v.tipo()).append(" | ").append(v.chiave()).append(" | ").append(v.esito().etichetta)
                        .append(" | ").append(v.dettaglio()).append("\n");
            }
            return sb.toString();
        }
    }

    /** Dove si rimette lo storico: l'archivio vero, o un registratore nei test. */
    interface Storico {

        boolean contiene(String lignaggio, long millis, String idVecchio);

        void aggiungi(String lignaggio, String idNuovo, String idVecchio, String riga, String operazione, long millis);

        /** Lo storico delle modifiche vero ({@link MovimentiStorico}), che si scrive al Salva. */
        Storico PREDEFINITO = new Storico() {
            @Override
            public boolean contiene(String lignaggio, long millis, String idVecchio) {
                return MovimentiStorico.ContieneVoce(lignaggio, millis, idVecchio);
            }

            @Override
            public void aggiungi(String lignaggio, String idNuovo, String idVecchio, String riga, String operazione, long millis) {
                MovimentiStorico.AccodaModifica(lignaggio, idNuovo, idVecchio, riga, operazione, millis);
            }
        };
    }

    /** Un passo del piano: l'azione da eseguire sull'archivio, che dice se è riuscita. */
    @FunctionalInterface
    interface Azione {

        boolean esegui(Map<String, String[]> archivio, Storico storico);
    }

    /** Il piano: le voci (con gli esiti) e, per quelle eseguibili, l'azione. */
    public static final class Piano {

        private final List<Voce> voci = new ArrayList<>();
        private final List<Azione> azioni = new ArrayList<>();
        /** La riga che descrive la prossima voce: ogni unità del file ne aggiunge una sola, la usa e la azzera. */
        private String[] contesto;

        void contesto(String[] riga) {
            contesto = riga;
        }

        void aggiungi(Voce v, Azione a) {
            voci.add(contesto == null ? v : v.con(contesto));
            azioni.add(a);
            contesto = null;
        }

        /** @return se la voce {@code i} ha un'azione e si può scegliere di applicarla */
        public boolean eseguibile(int i) {
            return voci.get(i).esito() == Esito.APPLICABILE && azioni.get(i) != null;
        }

        public Anteprima anteprima() {
            return new Anteprima(List.copyOf(voci));
        }

        /** @return quante voci hanno un'azione che {@link #Applica} sa eseguire */
        public int eseguibili() {
            int n = 0;
            for (int i = 0; i < voci.size(); i++) {
                if (voci.get(i).esito() == Esito.APPLICABILE && azioni.get(i) != null) {
                    n++;
                }
            }
            return n;
        }

        /** @return quante voci sono applicabili ma senza azione (non dovrebbero essercene) */
        public int applicabiliSenzaAzione() {
            int n = 0;
            for (int i = 0; i < voci.size(); i++) {
                if (voci.get(i).esito() == Esito.APPLICABILE && azioni.get(i) == null) {
                    n++;
                }
            }
            return n;
        }
    }

    /** L'esito dell'applicazione: quante modifiche eseguite, quali saltate e perché. */
    public record Risultato(int applicate, List<String> saltate, int senzaAzione) {

    }

    // ------------------------------------------------------------------------------------------------------------
    //  Lettura
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Legge un file di esportazione.
     *
     * @throws FileNonValido se non è un'esportazione, se il formato è più nuovo di questa versione, se i campi per
     *         riga non sono quelli di {@link Importazioni#ColonneTabella} o se una riga ha un numero di colonne sbagliato
     */
    public static Contenuto Leggi(File f) throws IOException, FileNonValido {
        return Leggi(Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    /** Come {@link #Leggi(File)} sul testo del file. */
    static Contenuto Leggi(String testo) throws FileNonValido {
        if (testo.startsWith("﻿")) {
            testo = testo.substring(1);
        }
        String[] righe = testo.split("\r?\n", -1);
        if (righe.length < 2 || !righe[0].startsWith("#GiacenzeCrypto ModificheManuali;")) {
            throw new FileNonValido("Il file non è un'esportazione delle modifiche manuali dei movimenti.");
        }
        int formato = -1;
        int campi = -1;
        for (String parte : righe[0].split(";")) {
            if (parte.startsWith("formato=")) {
                formato = Intero(parte.substring(8));
            } else if (parte.startsWith("campi=")) {
                campi = Intero(parte.substring(6));
            }
        }
        if (formato < 1 || formato > FORMATO) {
            throw new FileNonValido("Il formato del file (" + (formato < 1 ? "sconosciuto" : formato)
                    + ") non è leggibile da questa versione del programma.");
        }
        if (campi != Importazioni.ColonneTabella) {
            throw new FileNonValido("Il file ha " + campi + " campi per movimento, questa versione ne usa "
                    + Importazioni.ColonneTabella + ".");
        }
        int colonne = COLONNE_FISSE + campi;
        List<String[]> record = new ArrayList<>();
        //La riga 1 è il commento, la 2 l'intestazione
        for (int i = 2; i < righe.length; i++) {
            if (righe[i].isEmpty()) {
                continue;
            }
            String[] r = righe[i].split(";", -1);
            if (r.length != colonne) {
                throw new FileNonValido("Riga " + (i + 1) + " del file: " + r.length + " colonne invece di " + colonne + ".");
            }
            record.add(r);
        }
        return new Contenuto(formato, campi, record);
    }

    private static int Intero(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    //  Confronti e abbinamento
    // ------------------------------------------------------------------------------------------------------------

    /** Nel confronto fra file e archivio nuovo: i campi ricalcolati e il documento di origine, che cambia id. */
    private static boolean Ignorato(int campo) {
        return CAMPI_NON_CONFRONTATI.contains(campo) || campo == 41;
    }

    private static String[] Campi(String[] record) {
        return Arrays.copyOfRange(record, COLONNE_FISSE, COLONNE_FISSE + Importazioni.ColonneTabella);
    }

    private static String Valore(String[] v, int i) {
        return v != null && i < v.length && v[i] != null ? v[i].trim() : "";
    }

    /** Uguali come testo o come numero (1.50 e 1.5 sono lo stesso valore). */
    static boolean Uguali(String a, String b) {
        a = a == null ? "" : a.trim();
        b = b == null ? "" : b.trim();
        if (a.equals(b)) {
            return true;
        }
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** I campi su cui due righe differiscono, senza quelli ignorati. */
    static List<Integer> Diversi(String[] a, String[] b) {
        List<Integer> campi = new ArrayList<>();
        for (int i = 0; i < Importazioni.ColonneTabella; i++) {
            if (!Ignorato(i) && !Uguali(Valore(a, i), Valore(b, i))) {
                campi.add(i);
            }
        }
        return campi;
    }

    /** L'ID di un estremo di scambio differito classificato com'era prima (senza {@code 00}/{@code 04}), o {@code null}. */
    static String IDBaseDifferito(String id) {
        String[] s = id == null ? new String[0] : id.split("_");
        if (s.length >= 5 && s[1].length() > 2 && (s[1].startsWith("00") || s[1].startsWith("04"))) {
            s[1] = s[1].substring(2);
            return String.join("_", s);
        }
        return null;
    }

    private static String NomeCampo(int i) {
        return "[" + i + "] " + (i < NOMI_CAMPI.length ? NOMI_CAMPI[i] : "");
    }

    /** Un valore vuoto si legge come niente: nell'elenco dei cambiamenti si scrive. */
    private static String Vuoto(String s) {
        return s.isEmpty() ? "(vuoto)" : s;
    }

    private static String Breve(String s) {
        return s.length() > 40 ? s.substring(0, 37) + "..." : s;
    }

    /** L'archivio da cui cercare, con ogni movimento dato a una riga sola quando si abbina per chiave. */
    private static final class Abbinatore {

        final TreeMap<String, String[]> archivio = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, ArrayDeque<String[]>> perChiave = new HashMap<>();
        private final Set<String[]> usati = Collections.newSetFromMap(new IdentityHashMap<>());
        /** Gli ID citati nel campo {@code [20]} di qualche movimento (per avvisare che una rinomina li rompe). */
        private Set<String> citati;

        Abbinatore(Map<String, String[]> movimenti) {
            for (Map.Entry<String, String[]> e : movimenti.entrySet()) {
                String[] v = e.getValue();
                if (v == null || v.length < Importazioni.ColonneTabella) {
                    continue;
                }
                archivio.put(e.getKey(), v);
                if (!"AU".equalsIgnoreCase(v[22])) {
                    perChiave.computeIfAbsent(Importazioni.F_buildKeyMovimento(v), k -> new ArrayDeque<>()).addLast(v);
                }
            }
        }

        /** Per ID, anche nella forma di partenza di uno scambio differito. Non consuma. */
        String[] perId(String id) {
            String[] v = archivio.get(id);
            if (v == null && IDBaseDifferito(id) != null) {
                v = archivio.get(IDBaseDifferito(id));
            }
            //Il deposito di un trasferimento spostato dopo il prelievo (OrdineTrasferimenti) è nell'archivio nuovo con l'ID senza "~"
            if (v == null && OrdineTrasferimenti.IDSenzaMarcatore(id) != null) {
                v = archivio.get(OrdineTrasferimenti.IDSenzaMarcatore(id));
            }
            return v;
        }

        /** Per ID e poi per chiave. Prende il movimento: non sarà più dato a un'altra riga. */
        String[] trova(String[] riga) {
            String[] v = perId(riga[0]);
            if (v != null && !"AU".equalsIgnoreCase(v[22])) {
                usati.add(v);
                return v;
            }
            return perChiave(riga);
        }

        String[] perChiave(String[] riga) {
            ArrayDeque<String[]> coda = perChiave.get(Importazioni.F_buildKeyMovimento(riga));
            while (coda != null && !coda.isEmpty()) {
                String[] v = coda.pollFirst();
                //Non ancora dato a una riga, e ancora in archivio con lo stesso ID
                if (!usati.contains(v) && archivio.get(v[0]) == v) {
                    usati.add(v);
                    return v;
                }
            }
            return null;
        }

        boolean citato(String id) {
            if (citati == null) {
                citati = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (String[] v : archivio.values()) {
                    for (String r : Valore(v, 20).split(",")) {
                        if (!r.isBlank() && !r.trim().equalsIgnoreCase(v[0])) {
                            citati.add(r.trim());
                        }
                    }
                }
            }
            return citati.contains(id);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    //  Piano
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Analizza il file contro un archivio e basta: è {@code Pianifica(...).anteprima()}. Pura: non modifica né il
     * contenuto né l'archivio.
     *
     * @param archivio i movimenti dell'archivio reimportato, ID -> riga
     */
    public static Anteprima Analizza(Contenuto contenuto, Map<String, String[]> archivio) {
        return Pianifica(contenuto, archivio).anteprima();
    }

    /**
     * Abbina il file all'archivio e prepara, per ogni modifica, l'esito e l'azione. Non modifica nulla: le azioni
     * agiranno solo in {@link #Applica}.
     *
     * @param archivio i movimenti dell'archivio reimportato, ID -> riga
     */
    public static Piano Pianifica(Contenuto contenuto, Map<String, String[]> archivio) {
        Abbinatore abb = new Abbinatore(archivio);
        Piano piano = new Piano();
        //ID finale di un movimento modificato -> il movimento dell'archivio che gli corrisponde
        Map<String, String[]> perIdFinale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //ID finale di una catena rinviata a un gruppo classificato -> ciò che serve al gruppo per applicarla
        Map<String, InfoCatena> catenePerFinale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Map<String, List<String[]>> iniziali = new LinkedHashMap<>();
        Map<String, List<String[]>> finali = new LinkedHashMap<>();
        Map<String, List<String[]>> passaggi = new HashMap<>();
        List<String[]> senzaStorico = new ArrayList<>();
        Map<String, List<String[]>> gruppi = new LinkedHashMap<>();
        for (String[] r : contenuto.record()) {
            switch (r[C_TIPO]) {
                case TIPO_INIZIALE -> iniziali.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(r);
                case TIPO_FINALE -> finali.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(r);
                case TIPO_PASSAGGIO -> passaggi.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(r);
                case TIPO_SENZA_STORICO -> senzaStorico.add(r);
                case TIPO_CLASSIFICAZIONE -> gruppi.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(r);
                default -> {
                    //Un tipo che questa versione non conosce: lo ignora
                }
            }
        }
        Set<String> classificati = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        gruppi.values().forEach(g -> g.forEach(r -> classificati.add(r[C_ID_DOPO])));

        for (Map.Entry<String, List<String[]>> catena : iniziali.entrySet()) {
            PianificaCatena(piano, catena.getKey(), catena.getValue(), finali.getOrDefault(catena.getKey(), List.of()),
                    passaggi.getOrDefault(catena.getKey(), List.of()), classificati, abb, perIdFinale, catenePerFinale);
        }
        for (String[] r : senzaStorico) {
            PianificaSenzaStorico(piano, r, abb);
        }
        for (Map.Entry<String, List<String[]>> g : gruppi.entrySet()) {
            PianificaGruppo(piano, g.getKey(), g.getValue(), abb, perIdFinale, catenePerFinale);
        }
        return piano;
    }

    private static void PianificaCatena(Piano piano, String lignaggio, List<String[]> iniziali, List<String[]> finali,
            List<String[]> passaggi, Set<String> classificati, Abbinatore abb, Map<String, String[]> perIdFinale,
            Map<String, InfoCatena> catenePerFinale) {
        String idIniz = iniziali.get(0)[C_ID_PRIMA];
        piano.contesto(finali.isEmpty() ? Campi(iniziali.get(0)) : Campi(finali.get(0)));
        if (finali.isEmpty()) {
            piano.aggiungi(new Voce("Movimento non più presente", idIniz, Esito.NESSUNA_AZIONE,
                    "il movimento era stato cancellato dopo la modifica"), null);
            return;
        }
        if (finali.size() > 1) {
            boolean carta = false;
            for (String[] f : finali) {
                carta |= f[C_OPERAZIONE] != null && f[C_OPERAZIONE].contains(MiniPay_SpeseCarta.OP_DIVISIONE);
            }
            piano.aggiungi(new Voce("Separazione", idIniz, Esito.MANUALE, carta
                    ? "il movimento della catena è stato diviso in " + finali.size() + " per pagamento con la carta MiniPay: si rifà"
                    + " importando di nuovo il CSV della carta"
                    : "il movimento è stato separato in " + finali.size() + " dopo la modifica: la separazione va rifatta a mano"), null);
            return;
        }
        String[] fin = Campi(finali.get(0));
        String idFin = fin[0];
        String tipo = iniziali.size() > 1 ? "Unione" : idFin.equals(idIniz) ? "Modifica puntuale" : "Rinomina";
        //Già applicata: l'archivio ha già il movimento finale
        String[] giaFinale = abb.archivio.get(idFin);
        if (giaFinale != null && Diversi(giaFinale, fin).isEmpty()) {
            perIdFinale.put(idFin, giaFinale);
            piano.aggiungi(new Voce(tipo, idFin, Esito.GIA_APPLICATA, "l'archivio ha già il movimento finale"), null);
            return;
        }
        List<String[]> trovati = new ArrayList<>();
        List<String> mancanti = new ArrayList<>();
        for (String[] i : iniziali) {
            String[] riga = Campi(i);
            String[] a = abb.trova(riga);
            if (a == null) {
                mancanti.add(riga[0]);
            } else {
                trovati.add(a);
            }
        }
        if (!mancanti.isEmpty()) {
            piano.aggiungi(new Voce(tipo, iniziali.size() > 1 ? idFin : idIniz, Esito.NON_TROVATO,
                    iniziali.size() > 1 ? "movimenti di partenza non ritrovati: " + String.join(", ", mancanti)
                    : "movimento di partenza non ritrovato (né per ID né per chiave)"), null);
            return;
        }
        //Se la classificazione è venuta dopo la modifica, la differenza contiene campi della classificazione, con un [20]
        //che punta a righe generate che non ci sono ancora: lo applica il gruppo, non la catena. Le righe iniziali si
        //abbinano comunque, e il gruppo trova da lì il movimento (anche se è stato rinominato o unito)
        if (classificati.contains(idFin)) {
            catenePerFinale.put(idFin, new InfoCatena(lignaggio, passaggi, new ArrayList<>(trovati)));
            perIdFinale.put(idFin, trovati.get(0));
            piano.aggiungi(new Voce(tipo, idFin, Esito.RINVIATA,
                    "il movimento è anche in un gruppo classificato: si applica con la classificazione"), null);
            return;
        }
        //L'ID finale deve essere libero, o essere uno dei movimenti abbinati
        String[] occupante = abb.archivio.get(idFin);
        if (occupante != null && !trovati.contains(occupante)) {
            piano.aggiungi(new Voce(tipo, idIniz, Esito.CONFLITTO, "l'ID finale " + idFin + " è occupato da un altro movimento"), null);
            return;
        }
        if (iniziali.size() > 1) {
            trovati.forEach(a -> perIdFinale.put(idFin, a));
            List<String> partenze = new ArrayList<>();
            for (String[] i : iniziali) {
                partenze.add("  " + Campi(i)[0] + "  (" + Descrizione(Campi(i)) + ")");
            }
            piano.aggiungi(new Voce(tipo, idFin, Esito.APPLICABILE, "unione di " + iniziali.size() + " movimenti, tutti ritrovati")
                    .conCompleto("Unione di " + iniziali.size() + " movimenti, tutti ritrovati nell'archivio:\n" + String.join("\n", partenze)
                            + "\n\nIl risultato è il movimento " + idFin + "  (" + Descrizione(fin) + ")."),
                    AzioneUnione(lignaggio, new ArrayList<>(trovati), fin, passaggi));
            return;
        }
        String[] a = trovati.get(0);
        perIdFinale.put(idFin, a);
        String[] ini = Campi(iniziali.get(0));
        List<String> conflitti = new ArrayList<>();
        List<String> conflittiCompleti = new ArrayList<>();
        List<Integer> daScrivere = new ArrayList<>();
        int gia = 0;
        for (int c : CampiCambiati(ini, fin)) {
            if (c == 0) {
                //L'ID non è un conflitto: abbinando per chiave l'ID dell'archivio è diverso da quello del file per costruzione
                if (Uguali(Valore(a, 0), Valore(fin, 0))) {
                    gia++;
                } else {
                    daScrivere.add(c);
                }
            } else if (Uguali(Valore(a, c), Valore(ini, c))) {
                daScrivere.add(c);
            } else if (Uguali(Valore(a, c), Valore(fin, c))) {
                gia++;
            } else {
                conflitti.add(NomeCampo(c) + ": nell'archivio " + Breve(Valore(a, c)) + ", atteso " + Breve(Valore(ini, c))
                        + " o " + Breve(Valore(fin, c)));
                conflittiCompleti.add(NomeCampo(c) + "\n    nell'archivio: " + Vuoto(Valore(a, c)) + "\n    il file si aspetta: "
                        + Vuoto(Valore(ini, c)) + "\n    oppure, se già applicata: " + Vuoto(Valore(fin, c)));
            }
        }
        if (!conflitti.isEmpty()) {
            piano.aggiungi(new Voce(tipo, idIniz, Esito.CONFLITTO, String.join(" | ", conflitti))
                    .conCompleto("L'archivio ha valori diversi sia da quelli di partenza sia da quelli finali, quindi non si applica:\n\n"
                            + String.join("\n\n", conflittiCompleti)), null);
            return;
        }
        if (daScrivere.isEmpty()) {
            piano.aggiungi(new Voce(tipo, idIniz, Esito.GIA_APPLICATA, "i campi modificati hanno già il valore finale"), null);
            return;
        }
        String avviso = daScrivere.contains(0) && abb.citato(a[0])
                ? ". Attenzione: il vecchio ID è citato nel campo [20] di altri movimenti" : "";
        List<String> cambi = new ArrayList<>();
        List<String> cambiCompleti = new ArrayList<>();
        for (int c : daScrivere) {
            cambi.add(NomeCampo(c) + ": " + Vuoto(Breve(Valore(a, c))) + " -> " + Vuoto(Breve(Valore(fin, c))));
            cambiCompleti.add(NomeCampo(c) + "\n    ora:     " + Vuoto(Valore(a, c)) + "\n    diventa: " + Vuoto(Valore(fin, c)));
        }
        String completoCambi = "Campi che cambiano:\n\n" + String.join("\n\n", cambiCompleti)
                + (gia > 0 ? "\n\n" + gia + " campi modificati hanno già il valore finale." : "")
                + (avviso.isEmpty() ? "" : "\n\nAttenzione: il vecchio ID è citato nel campo [20] di altri movimenti, il collegamento si romperebbe.");
        piano.aggiungi(new Voce(tipo, idIniz, Esito.APPLICABILE,
                String.join(" | ", cambi) + (gia > 0 ? " (" + gia + " già a posto)" : "") + avviso).conCompleto(completoCambi),
                AzionePuntuale(lignaggio, a, fin, daScrivere, passaggi));
    }

    /** Ciò che il gruppo classificato deve fare di una catena rinviata: lignaggio, storico e movimenti di partenza. */
    private record InfoCatena(String lignaggio, List<String[]> passaggi, List<String[]> abbinati) {

    }

    /** Riscrive nella riga abbinata i campi cambiati (l'ID con la chiave della mappa) e mette il lignaggio. */
    private static Azione AzionePuntuale(String lignaggio, String[] a, String[] fin, List<Integer> campi, List<String[]> passaggi) {
        return (archivio, storico) -> {
            if (archivio.get(a[0]) != a) {
                return false;
            }
            if (campi.contains(0)) {
                String[] occupante = archivio.get(fin[0]);
                if (occupante != null && occupante != a) {
                    return false;
                }
            }
            //La chiave della mappa segue l'ID: si toglie prima di cambiarlo
            archivio.remove(a[0]);
            for (int c : campi) {
                a[c] = fin[c];
            }
            a[MovimentiStorico.CAMPO_LIGNAGGIO] = lignaggio;
            archivio.put(a[0], a);
            RimettiStorico(lignaggio, passaggi, storico);
            return true;
        };
    }

    /** Toglie le righe abbinate e mette la riga finale, con il documento di origine unione di quelli abbinati. */
    private static Azione AzioneUnione(String lignaggio, List<String[]> abbinati, String[] fin, List<String[]> passaggi) {
        return (archivio, storico) -> {
            for (String[] a : abbinati) {
                if (archivio.get(a[0]) != a) {
                    return false;
                }
            }
            String[] occupante = archivio.get(fin[0]);
            if (occupante != null && !abbinati.contains(occupante)) {
                return false;
            }
            String[] nuovo = fin.clone();
            nuovo[41] = DocumentiFonte.Unione(abbinati);
            nuovo[MovimentiStorico.CAMPO_LIGNAGGIO] = lignaggio;
            for (String[] a : abbinati) {
                archivio.remove(a[0]);
            }
            archivio.put(nuovo[0], nuovo);
            RimettiStorico(lignaggio, passaggi, storico);
            return true;
        };
    }

    /** Rimette le voci di storico di una catena con la data originale, senza duplicare quelle già presenti. */
    private static void RimettiStorico(String lignaggio, List<String[]> passaggi, Storico storico) {
        for (String[] p : passaggi) {
            long millis = Long.parseLong(p[C_MILLIS]);
            if (!storico.contiene(lignaggio, millis, p[C_ID_PRIMA])) {
                storico.aggiungi(lignaggio, p[C_ID_DOPO], p[C_ID_PRIMA], Importazioni.SerializzaRiga(Campi(p)), p[C_OPERAZIONE], millis);
            }
        }
    }

    private static void PianificaSenzaStorico(Piano piano, String[] record, Abbinatore abb) {
        String[] riga = Campi(record);
        String id = riga[0];
        String categoria = record[C_OPERAZIONE];
        piano.contesto(riga);
        String[] a = abb.archivio.get(id);
        if (CATEGORIA_INSERITO.equals(categoria)) {
            if (a == null) {
                piano.aggiungi(new Voce(categoria, id, Esito.APPLICABILE, "da inserire, l'ID è libero"), AzioneInserimento(riga));
            } else if (Diversi(a, riga).isEmpty()) {
                piano.aggiungi(new Voce(categoria, id, Esito.GIA_APPLICATA, "già presente"), null);
            } else {
                piano.aggiungi(new Voce(categoria, id, Esito.CONFLITTO, "l'ID è occupato da un altro movimento"), null);
            }
            return;
        }
        if (a == null) {
            piano.aggiungi(new Voce(categoria, id, Esito.NON_TROVATO,
                    "nessun movimento con questo ID: il suo stato di partenza non è noto, da rifare a mano"), null);
            return;
        }
        List<Integer> diversi = Diversi(a, riga);
        if (diversi.isEmpty()) {
            piano.aggiungi(new Voce(categoria, id, Esito.GIA_APPLICATA, "l'archivio ha già lo stesso contenuto"), null);
            return;
        }
        List<String> nomi = new ArrayList<>();
        diversi.forEach(c -> nomi.add(NomeCampo(c)));
        piano.aggiungi(new Voce(categoria, id, Esito.DA_VERIFICARE,
                "presente ma diverso, il prima non è noto. Campi: " + String.join(", ", nomi)), null);
    }

    /** Inserisce il movimento com'è, senza documento di origine né lignaggio (quelli del vecchio archivio non esistono). */
    private static Azione AzioneInserimento(String[] riga) {
        return (archivio, storico) -> {
            if (archivio.containsKey(riga[0])) {
                return false;
            }
            String[] nuovo = riga.clone();
            nuovo[41] = "";
            nuovo[MovimentiStorico.CAMPO_LIGNAGGIO] = "";
            archivio.put(nuovo[0], nuovo);
            return true;
        };
    }

    /** I campi che il gruppo non copia dal file: ricalcolati dal motore, documento di origine e lignaggio. */
    private static final Set<Integer> CAMPI_NON_COPIATI = Set.of(16, 17, 19, 33, 38, 41, MovimentiStorico.CAMPO_LIGNAGGIO);

    /** Un originale di un gruppo: la sua riga nel file, i movimenti dell'archivio che gli corrispondono e la sua catena. */
    private record Originale(String[] file, List<String[]> abbinati, InfoCatena catena) {

    }

    private static void PianificaGruppo(Piano piano, String chiave, List<String[]> righe, Abbinatore abb,
            Map<String, String[]> perIdFinale, Map<String, InfoCatena> catenePerFinale) {
        Map<String, String[]> gruppo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String[]> originali = new ArrayList<>();
        List<String[]> generati = new ArrayList<>();
        for (String[] r : righe) {
            String[] c = Campi(r);
            gruppo.put(c[0], c);
            (CATEGORIA_GENERATO.equals(r[C_OPERAZIONE]) ? generati : originali).add(c);
        }
        piano.contesto(originali.isEmpty() ? generati.get(0) : originali.get(0));
        //Già applicata: tutti i movimenti del gruppo sono già nell'archivio com'erano
        boolean tutti = true;
        for (String[] c : gruppo.values()) {
            String[] a = abb.archivio.get(c[0]);
            if (a == null || !Diversi(a, c).isEmpty()) {
                tutti = false;
                break;
            }
        }
        if (tutti) {
            piano.aggiungi(new Voce("Classificazione", chiave, Esito.GIA_APPLICATA, "il gruppo è già nell'archivio"), null);
            return;
        }
        List<String> problemi = new ArrayList<>();
        List<String> mancanti = new ArrayList<>();
        List<Originale> trovati = new ArrayList<>();
        boolean citato = false;
        for (String[] o : originali) {
            InfoCatena info = catenePerFinale.get(o[0]);
            List<String[]> abbinati;
            if (info != null) {
                //Modificato prima di essere classificato: le righe di partenza della catena sono già abbinate
                abbinati = info.abbinati();
            } else {
                String[] a = perIdFinale.get(o[0]);
                if (a == null) {
                    String[] prima = GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(o, gruppo);
                    String[] attesa = prima != null ? prima : o;
                    a = abb.perId(o[0]);
                    if (a != null && !Importazioni.F_buildKeyMovimento(a).equals(Importazioni.F_buildKeyMovimento(attesa))) {
                        //Stesso ID ma un'altra forma (data, monete, quantità): copiare i campi del file la sovrascriverebbe
                        problemi.add(o[0] + " in archivio ha un'altra data, moneta o quantità di quella attesa");
                        continue;
                    }
                    if (a == null) {
                        a = abb.perChiave(attesa);
                    }
                }
                if (a == null) {
                    mancanti.add(o[0]);
                    continue;
                }
                abbinati = Collections.singletonList(a);
            }
            String[] a = abbinati.get(0);
            if (!Funzioni.noData(Valore(a, 20)) && !Uguali(Valore(a, 20), Valore(o, 20))) {
                //Solo il [20]: il campo 18 lo scrivono anche alcuni importatori, non è prova di una classificazione
                problemi.add(o[0] + " è già classificato in modo diverso nell'archivio");
            }
            String[] occupante = abb.archivio.get(o[0]);
            if (occupante != null && !abbinati.contains(occupante)) {
                problemi.add("l'ID " + o[0] + " è occupato da un altro movimento");
            }
            for (String[] x : abbinati) {
                citato |= !x[0].equalsIgnoreCase(o[0]) && abb.citato(x[0]);
            }
            trovati.add(new Originale(o, abbinati, info));
        }
        if (!mancanti.isEmpty()) {
            piano.aggiungi(new Voce("Classificazione", chiave, Esito.NON_TROVATO,
                    "originali non ritrovati (né per ID né per chiave): " + String.join(", ", mancanti))
                    .conCompleto("Originali non ritrovati nell'archivio, né per ID né per chiave:\n  " + String.join("\n  ", mancanti)), null);
            return;
        }
        for (String[] g : generati) {
            String[] a = abb.archivio.get(g[0]);
            if (a != null && !Diversi(a, g).isEmpty()) {
                problemi.add("l'ID della riga generata " + g[0] + " è occupato da un altro movimento");
            }
        }
        if (!problemi.isEmpty()) {
            piano.aggiungi(new Voce("Classificazione", chiave, Esito.CONFLITTO, String.join(" | ", problemi))
                    .conCompleto("Non si applica:\n  " + String.join("\n  ", problemi)), null);
            return;
        }
        List<String> dettaglioOriginali = new ArrayList<>();
        for (Originale x : trovati) {
            String dove = x.abbinati().get(0)[0].equalsIgnoreCase(x.file()[0]) ? "" : "  (in archivio: " + x.abbinati().get(0)[0] + ", prende l'ID del file)";
            dettaglioOriginali.add("  " + x.file()[0] + "  (" + Descrizione(x.file()) + ")" + dove);
        }
        List<String> dettaglioGenerati = new ArrayList<>();
        for (String[] g : generati) {
            dettaglioGenerati.add("  " + g[0] + "  (" + Descrizione(g) + ")" + (abb.archivio.containsKey(g[0]) ? "  già presente" : ""));
        }
        piano.aggiungi(new Voce("Classificazione", chiave, Esito.APPLICABILE,
                originali.size() + " originali ritrovati, " + generati.size() + " righe generate da inserire"
                + (citato ? ". Attenzione: il vecchio ID di un originale è citato nel campo [20] di altri movimenti" : ""))
                .conCompleto("Originali ritrovati, che prendono i campi della classificazione:\n" + String.join("\n", dettaglioOriginali)
                        + "\n\nRighe generate dal programma, da inserire:\n" + (dettaglioGenerati.isEmpty() ? "  nessuna" : String.join("\n", dettaglioGenerati))
                        + (citato ? "\n\nAttenzione: il vecchio ID di un originale è citato nel campo [20] di altri movimenti." : "")),
                AzioneGruppo(trovati, generati));
    }

    /**
     * Rimette il gruppo com'è nel file, tutto o niente: prima si controllano tutte le precondizioni, poi si scrive.
     * Gli originali prendono i campi del file (la chiave della mappa segue l'ID, che in uno scambio differito cambia), le
     * righe generate si inseriscono con l'origine del movimento corrispondente del nuovo archivio. Un originale che
     * veniva da una catena mette anche lignaggio e storico, e un'unione toglie tutti i suoi movimenti di partenza.
     */
    private static Azione AzioneGruppo(List<Originale> originali, List<String[]> generati) {
        return (archivio, storico) -> {
            for (Originale x : originali) {
                for (String[] a : x.abbinati()) {
                    if (archivio.get(a[0]) != a) {
                        return false;
                    }
                }
                String[] occupante = archivio.get(x.file()[0]);
                if (occupante != null && !x.abbinati().contains(occupante)) {
                    return false;
                }
            }
            for (String[] g : generati) {
                String[] occupante = archivio.get(g[0]);
                if (occupante != null && !Diversi(occupante, g).isEmpty()) {
                    return false;
                }
            }
            //Il documento di origine del vecchio archivio -> quello del movimento corrispondente nel nuovo
            Map<Integer, String> origini = new HashMap<>();
            for (Originale x : originali) {
                String[] a = x.abbinati().get(0);
                int vecchia = DocumentiFonte.IdDaCampo41(x.file()[41]);
                String documento = x.abbinati().size() > 1 ? DocumentiFonte.Unione(x.abbinati()) : a[41];
                for (String[] r : x.abbinati()) {
                    archivio.remove(r[0]);
                }
                for (int c = 0; c < Importazioni.ColonneTabella; c++) {
                    if (!CAMPI_NON_COPIATI.contains(c)) {
                        a[c] = x.file()[c];
                    }
                }
                a[41] = documento;
                if (x.catena() != null) {
                    a[MovimentiStorico.CAMPO_LIGNAGGIO] = x.catena().lignaggio();
                }
                archivio.put(a[0], a);
                if (vecchia > 0) {
                    origini.putIfAbsent(vecchia, DocumentiFonte.SoloOrigine(a[41]));
                }
                if (x.catena() != null) {
                    RimettiStorico(x.catena().lignaggio(), x.catena().passaggi(), storico);
                }
            }
            for (String[] g : generati) {
                if (archivio.get(g[0]) == null) {
                    String[] nuovo = g.clone();
                    nuovo[41] = origini.getOrDefault(DocumentiFonte.IdDaCampo41(g[41]), "");
                    nuovo[MovimentiStorico.CAMPO_LIGNAGGIO] = "";
                    archivio.put(nuovo[0], nuovo);
                }
            }
            return true;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    //  Applicazione
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Esegue le azioni del piano sull'archivio, in memoria: non scrive su disco (lo fa il Salva, e l'Annulla le
     * scarta insieme allo storico accodato). Le voci non applicabili non si toccano. Un'azione che non regge le
     * proprie precondizioni (il movimento non c'è più, l'ID finale è stato preso) non fa nulla e si elenca fra le
     * saltate.
     *
     * @param piano il piano, fatto sullo stesso archivio
     * @param archivio i movimenti, modificati sul posto
     * @param storico dove rimettere lo storico
     */
    static Risultato Applica(Piano piano, Map<String, String[]> archivio, Storico storico) {
        return Applica(piano, archivio, storico, null);
    }

    /**
     * Come {@link #Applica(Piano, Map, Storico)} ma solo per le voci scelte.
     *
     * @param scelte indici delle voci del piano da applicare ({@code piano.anteprima().voci()}), {@code null} per tutte.
     *        Le voci applicabili sono indipendenti fra loro: una catena rinviata a un gruppo si applica con il gruppo
     *        e non è mai una voce a sé
     */
    static Risultato Applica(Piano piano, Map<String, String[]> archivio, Storico storico, Set<Integer> scelte) {
        int applicate = 0;
        List<String> saltate = new ArrayList<>();
        for (int i = 0; i < piano.voci.size(); i++) {
            if (scelte != null && !scelte.contains(i)) {
                continue;
            }
            Voce v = piano.voci.get(i);
            Azione a = piano.azioni.get(i);
            if (v.esito() != Esito.APPLICABILE || a == null) {
                continue;
            }
            if (a.esegui(archivio, storico)) {
                applicate++;
            } else {
                saltate.add(v.tipo() + " " + v.chiave() + ": l'archivio è cambiato dopo l'analisi, non applicata");
            }
        }
        return new Risultato(applicate, saltate, piano.applicabiliSenzaAzione());
    }

    // ------------------------------------------------------------------------------------------------------------
    //  Dialogo
    // ------------------------------------------------------------------------------------------------------------

    private static final String OPZIONE_CARTELLA = "ModificheManuali_Cartella";
    private static final int PROBLEMI_NEL_MESSAGGIO = 25;

    /**
     * Chiede il file, lo analizza contro l'archivio attuale, mostra l'anteprima e, se c'è qualcosa da applicare,
     * fa scegliere quali modifiche riapplicare ({@link GUI_RipristinoModifiche}, di partenza tutte). Le modifiche
     * restano in memoria finché l'utente non preme Salva in Transazioni Crypto
     * (Annulla le scarta, storico compreso). Il rapporto completo si scrive accanto al file, con suffisso
     * {@code _anteprima.txt}.
     *
     * @return {@code true} se ha modificato i movimenti: il chiamante deve fare un solo {@code Funzioni_AggiornaTutto()}
     */
    public static boolean Ripristina(Window owner) {
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA, ""));
        fc.setDialogTitle("File delle modifiche manuali da ripristinare");
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (fc.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null) {
            return false;
        }
        File f = fc.getSelectedFile();
        Contenuto contenuto;
        try {
            contenuto = Leggi(f);
        } catch (FileNonValido ex) {
            Messaggi.WarningMessage("Ripristino modifiche manuali", "Il file non si può leggere.", ex.getMessage(), owner);
            return false;
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Ripristino modifiche manuali", "Il file non si può aprire.", ex.getMessage(), owner);
            return false;
        }
        Piano piano = Pianifica(contenuto, Principale.MappaCryptoWallet);
        Anteprima anteprima = piano.anteprima();
        File rapporto = new File(f.getParentFile(), f.getName().replaceAll("(?i)\\.csv$", "") + "_anteprima.txt");
        String avviso = "";
        try {
            Files.writeString(rapporto.toPath(), "Anteprima del " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                    + " per " + f.getName() + "\n\n" + anteprima.rapporto(), StandardCharsets.UTF_8);
            avviso = "\n\nRapporto completo: " + rapporto.getAbsolutePath();
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        List<String> problemi = anteprima.problemi();
        StringBuilder dettaglio = new StringBuilder();
        for (int i = 0; i < Math.min(problemi.size(), PROBLEMI_NEL_MESSAGGIO); i++) {
            dettaglio.append(problemi.get(i)).append("\n");
        }
        if (problemi.size() > PROBLEMI_NEL_MESSAGGIO) {
            dettaglio.append("... e altre ").append(problemi.size() - PROBLEMI_NEL_MESSAGGIO).append(".\n");
        }
        String testo = anteprima.riepilogo() + (dettaglio.length() > 0 ? "\nDa controllare:\n" + dettaglio : "") + avviso;
        int eseguibili = piano.eseguibili();
        if (eseguibili == 0) {
            String fine = "\n\nNon c'è nulla da applicare.";
            if (problemi.isEmpty()) {
                Messaggi.SuccessMessage("Ripristino modifiche manuali", "Esito del controllo", testo + fine, owner);
            } else {
                Messaggi.WarningMessage("Ripristino modifiche manuali", "Esito del controllo", testo + fine, owner);
            }
            return false;
        }
        //L'utente sceglie quali riapplicare: di partenza sono spuntate tutte quelle applicabili
        Set<Integer> scelte = GUI_RipristinoModifiche.Mostra(owner, anteprima.riepilogo() + "\n" + avviso.trim(),
                anteprima.voci(), piano::eseguibile);
        if (scelte == null) {
            return false;
        }
        Risultato r = Applica(piano, Principale.MappaCryptoWallet, Storico.PREDEFINITO, scelte);
        StringBuilder esito = new StringBuilder("Modifiche applicate: " + r.applicate() + ".");
        if (!r.saltate().isEmpty()) {
            esito.append("\n\nSaltate:\n").append(String.join("\n", r.saltate()));
        }
        esito.append("\n\nControlla il risultato e premi Salva in Transazioni Crypto, oppure Annulla per scartarlo.");
        if (r.saltate().isEmpty()) {
            Messaggi.SuccessMessage("Ripristino modifiche manuali", "Ripristino eseguito", esito.toString(), owner);
        } else {
            Messaggi.WarningMessage("Ripristino modifiche manuali", "Ripristino eseguito con alcune modifiche saltate", esito.toString(), owner);
        }
        return r.applicate() > 0;
    }
}
