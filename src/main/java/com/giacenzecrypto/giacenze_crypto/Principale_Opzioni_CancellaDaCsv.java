package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Cancellazione di movimenti scelti da un file CSV ({@code Opzioni - Varie}): il file dice <b>quali</b> movimenti, la
 * cancellazione vera la fa {@code Principale.Funzione_EliminaMovimenti}, l'unica strada che c'è (gruppi collegati,
 * commissioni orfane, storico, ricalcolo, provvisoria fino a Salva).
 *
 * <p><b>Il file.</b> La prima riga è l'intestazione e dice quale campo del movimento contiene ogni colonna: {@code ID}
 * (il campo 0) oppure il numero del campo, scritto {@code 5}, {@code [5]} o {@code campo 5} (da 0 a
 * {@link Importazioni#ColonneTabella} - 1, i campi descritti in {@code Dettagli_ModelloRigaMovimento.md}). Ogni riga
 * successiva è un criterio: un movimento corrisponde se <b>ogni</b> cella piena della riga è uguale al campo della sua
 * colonna, una cella vuota vale «qualsiasi valore». Basta che corrisponda a una riga sola. Un file di una sola colonna
 * di ID può non avere l'intestazione. Separatore {@code ;}, tabulazione o {@code ,}, in quest'ordine, scelto
 * dall'intestazione (dal resto del file se l'intestazione ha una colonna sola), virgolette alla maniera CSV.</p>
 *
 * <p><b>Nel dubbio si cancella di meno, e si dice perché.</b> Un criterio perso allarga la corrispondenza, quindi
 * cancella di più: una colonna dell'intestazione che non si riconosce, una colonna ripetuta o una riga con più celle
 * dell'intestazione fermano la lettura, mai si saltano. Una riga tutta vuota corrisponderebbe a tutto: si salta e si
 * conta. I valori si confrontano esattamente, tolti solo gli spazi ai lati: niente maiuscole/minuscole, niente numeri
 * normalizzati ({@code -0.6} non è {@code -0.60}). Una riga che non trova nulla compare nell'anteprima. L'ID fa
 * eccezione e si cerca nella mappa dei movimenti, che non distingue maiuscole e minuscole come in tutto il programma.</p>
 *
 * <p>Logica senza Swing, come le altre classi {@code Principale_*}: l'anteprima è {@link GUI_CancellaDaCsv}.</p>
 */
public final class Principale_Opzioni_CancellaDaCsv {

    private Principale_Opzioni_CancellaDaCsv() {
    }

    static final String OPZIONE_CARTELLA = "CancellaDaCsv_Cartella";

    /** Un file senza intestazione è ammesso solo se la sua prima cella ha la forma di un ID di movimento */
    private static final Pattern FORMA_ID = Pattern.compile("^\\d{14}_.+");
    private static final Pattern NUMERO_CAMPO = Pattern.compile("^(?:\\[\\s*(\\d+)\\s*\\]|(?:campo\\s*)?(\\d+))$",
            Pattern.CASE_INSENSITIVE);

    /** Il file non si può usare: il messaggio dice perché, con il numero di riga quando c'è */
    public static class FileNonValido extends Exception {

        public FileNonValido(String messaggio) {
            super(messaggio);
        }
    }

    /**
     * Una riga del file.
     * @param riga numero della riga nel file, da 1
     * @param campi campo del movimento -> valore cercato, solo le celle piene, nell'ordine delle colonne
     */
    public record Criterio(int riga, Map<Integer, String> campi) {

        /** @return il criterio in forma leggibile, per l'anteprima: {@code riga 3: [3]=Binance, [18]=DCZ} */
        public String descrizione() {
            StringBuilder sb = new StringBuilder("riga ").append(riga).append(": ");
            boolean primo = true;
            for (Map.Entry<Integer, String> e : campi.entrySet()) {
                if (!primo) sb.append(", ");
                sb.append(e.getKey() == 0 ? "ID" : "[" + e.getKey() + "]").append("=").append(e.getValue());
                primo = false;
            }
            return sb.toString();
        }
    }

    /**
     * Il contenuto del file.
     * @param colonne il campo di ogni colonna, nell'ordine del file
     * @param criteri le righe con almeno una cella piena
     * @param righeVuote righe saltate perché vuote (avrebbero corrisposto a tutto)
     * @param conIntestazione {@code false} per un elenco di ID senza intestazione
     */
    public record Criteri(List<Integer> colonne, List<Criterio> criteri, int righeVuote, boolean conIntestazione) {
    }

    /**
     * Il risultato della ricerca.
     * @param ids gli ID trovati, nell'ordine dell'archivio
     * @param rigaDi per ogni ID, la prima riga del file che lo ha trovato
     * @param trovatiPerCriterio quanti movimenti ha trovato ogni criterio, nell'ordine dei criteri
     * @param senzaCorrispondenza i criteri che non hanno trovato nulla
     * @param conGruppoIncompleto movimenti trovati che fanno parte di un gruppo collegato ({@code [20]}) con almeno un
     *        altro movimento non trovato: cancellarli annulla la classificazione di quel gruppo
     * @param generati movimenti trovati generati da una classificazione ({@code [22]} = {@code AU})
     */
    public record Esito(List<String> ids, Map<String, Integer> rigaDi, List<Integer> trovatiPerCriterio,
            List<Criterio> senzaCorrispondenza, int conGruppoIncompleto, int generati) {
    }

    /**
     * @param intestazione il testo di una cella dell'intestazione
     * @return il campo del movimento che indica, {@code -1} se non è un'intestazione riconosciuta
     */
    static int Colonna(String intestazione) {
        String t = intestazione == null ? "" : intestazione.trim();
        if (t.equalsIgnoreCase("ID")) return 0;
        java.util.regex.Matcher m = NUMERO_CAMPO.matcher(t);
        if (!m.matches()) return -1;
        try {
            int n = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
            return n >= 0 && n < Importazioni.ColonneTabella ? n : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Legge il file (UTF-8, con o senza BOM) */
    public static Criteri Leggi(File f) throws IOException, FileNonValido {
        return Leggi(Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    /**
     * Legge il contenuto di un file.
     * @throws FileNonValido se il file è vuoto, se l'intestazione ha una colonna sconosciuta o ripetuta, se una riga ha
     *         più celle dell'intestazione o una virgoletta non chiusa
     */
    public static Criteri Leggi(String testo) throws FileNonValido {
        if (testo == null) testo = "";
        if (testo.startsWith("﻿")) testo = testo.substring(1);
        String[] righe = testo.split("\r?\n|\r", -1);
        int prima = 0;
        while (prima < righe.length && righe[prima].isBlank()) prima++;
        if (prima >= righe.length) throw new FileNonValido("Il file è vuoto.");

        String intestazione = righe[prima];
        //dall'intestazione, o dal resto del file se l'intestazione ha una colonna sola: una riga di dati con più celle
        //deve risultare tale e fermare la lettura, non diventare un valore unico che non trova nulla
        String perSeparatore = intestazione.matches(".*[;\t,].*") ? intestazione : testo;
        char separatore = perSeparatore.indexOf(';') >= 0 ? ';' : perSeparatore.indexOf('\t') >= 0 ? '\t' : ',';
        List<String> celle = Celle(intestazione, separatore, prima + 1);
        List<Integer> colonne = new ArrayList<>();
        boolean conIntestazione = true;
        List<String> sconosciute = new ArrayList<>();
        for (String c : celle) {
            int n = Colonna(c);
            if (n < 0) sconosciute.add(c.trim());
            colonne.add(n);
        }
        int inizioDati = prima + 1;
        if (!sconosciute.isEmpty()) {
            //Senza intestazione solo un elenco di ID: una sola colonna e la prima cella con la forma di un ID
            if (celle.size() == 1 && FORMA_ID.matcher(celle.get(0).trim()).matches()) {
                colonne = List.of(0);
                conIntestazione = false;
                inizioDati = prima;
            } else {
                throw new FileNonValido("La prima riga deve dire quale campo del movimento contiene ogni colonna. "
                        + (sconosciute.size() == 1 ? "Questa colonna non è riconosciuta: " : "Queste colonne non sono riconosciute: ")
                        + String.join(", ", sconosciute.stream().map(s -> "«" + s + "»").toList())
                        + ". Si scrive ID oppure il numero del campo (5, [5] o campo 5), da 0 a "
                        + (Importazioni.ColonneTabella - 1) + ". Senza intestazione si può dare solo un elenco di ID, una colonna sola.");
            }
        }
        Set<Integer> viste = new HashSet<>();
        for (int n : colonne) {
            if (!viste.add(n)) {
                throw new FileNonValido("Nella prima riga il campo " + (n == 0 ? "ID" : "[" + n + "]")
                        + " compare due volte: non si sa quale valore cercare.");
            }
        }

        List<Criterio> criteri = new ArrayList<>();
        int vuote = 0;
        //le righe vuote in fondo al file non contano: quasi ogni file finisce con un a capo
        int ultima = righe.length - 1;
        while (ultima >= inizioDati && righe[ultima].isBlank()) ultima--;
        for (int i = inizioDati; i <= ultima; i++) {
            List<String> valori = Celle(righe[i], separatore, i + 1);
            if (valori.size() > colonne.size()) {
                throw new FileNonValido("La riga " + (i + 1) + " ha " + valori.size() + " celle, l'intestazione "
                        + colonne.size() + ": non si sa a quale campo appartengono quelle in più.");
            }
            Map<Integer, String> campi = new LinkedHashMap<>();
            for (int c = 0; c < valori.size(); c++) {
                String v = valori.get(c).trim();
                if (!v.isEmpty()) campi.put(colonne.get(c), v);
            }
            if (campi.isEmpty()) {
                vuote++;
            } else {
                criteri.add(new Criterio(i + 1, Collections.unmodifiableMap(campi)));
            }
        }
        return new Criteri(List.copyOf(colonne), List.copyOf(criteri), vuote, conIntestazione);
    }

    /** Divide una riga in celle, con le virgolette alla maniera CSV ({@code ""} dentro un valore fra virgolette) */
    static List<String> Celle(String riga, char separatore, int numeroRiga) throws FileNonValido {
        List<String> celle = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean virgolette = false;
        for (int i = 0; i < riga.length(); i++) {
            char c = riga.charAt(i);
            if (virgolette) {
                if (c == '"') {
                    if (i + 1 < riga.length() && riga.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        virgolette = false;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == '"' && sb.toString().isBlank()) {
                virgolette = true;
                sb.setLength(0);
            } else if (c == separatore) {
                celle.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (virgolette) throw new FileNonValido("La riga " + numeroRiga + " ha una virgoletta non chiusa.");
        celle.add(sb.toString());
        return celle;
    }

    /**
     * Cerca nell'archivio i movimenti che corrispondono ad almeno un criterio.
     * @param c il contenuto del file
     * @param archivio i movimenti, per ID ({@code Principale.MappaCryptoWallet})
     */
    public static Esito Trova(Criteri c, Map<String, String[]> archivio) {
        Map<String, Integer> rigaDi = new HashMap<>();
        List<Integer> perCriterio = new ArrayList<>();
        List<Criterio> senza = new ArrayList<>();
        //i criteri con l'ID si risolvono con la mappa, gli altri guardano ogni movimento: un giro solo per tutti
        List<Integer> daScorrere = new ArrayList<>();
        int[] conteggi = new int[c.criteri().size()];
        for (int i = 0; i < c.criteri().size(); i++) {
            Criterio k = c.criteri().get(i);
            String id = k.campi().get(0);
            if (id != null) {
                String[] v = archivio.get(id);
                if (v != null && Corrisponde(v, k)) {
                    conteggi[i]++;
                    rigaDi.putIfAbsent(v[0], k.riga());
                }
            } else {
                daScorrere.add(i);
            }
        }
        if (!daScorrere.isEmpty()) {
            for (String[] v : archivio.values()) {
                for (int i : daScorrere) {
                    Criterio k = c.criteri().get(i);
                    if (Corrisponde(v, k)) {
                        conteggi[i]++;
                        rigaDi.merge(v[0], k.riga(), Math::min);
                    }
                }
            }
        }
        for (int i = 0; i < conteggi.length; i++) {
            perCriterio.add(conteggi[i]);
            if (conteggi[i] == 0) senza.add(c.criteri().get(i));
        }
        //nell'ordine dell'archivio, e con l'ID come è scritto in archivio
        List<String> ids = new ArrayList<>();
        Set<String> trovati = new HashSet<>(rigaDi.keySet());
        for (Map.Entry<String, String[]> e : archivio.entrySet()) {
            if (trovati.contains(e.getValue()[0])) ids.add(e.getKey());
        }
        Set<String> insieme = new HashSet<>();
        for (String id : ids) insieme.add(id.toUpperCase());
        int gruppiIncompleti = 0;
        int generati = 0;
        for (String id : ids) {
            String[] v = archivio.get(id);
            if ("AU".equalsIgnoreCase(Campo(v, 22))) generati++;
            for (String altro : Campo(v, 20).split(",")) {
                String a = altro.trim();
                if (!a.isEmpty() && archivio.get(a) != null && !insieme.contains(a.toUpperCase())) {
                    gruppiIncompleti++;
                    break;
                }
            }
        }
        return new Esito(List.copyOf(ids), Collections.unmodifiableMap(rigaDi), List.copyOf(perCriterio),
                List.copyOf(senza), gruppiIncompleti, generati);
    }

    private static boolean Corrisponde(String[] v, Criterio k) {
        for (Map.Entry<Integer, String> e : k.campi().entrySet()) {
            int n = e.getKey();
            if (n == 0) {
                if (!Campo(v, 0).equalsIgnoreCase(e.getValue())) return false;
            } else if (!Campo(v, n).trim().equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static String Campo(String[] v, int n) {
        return v == null || n >= v.length || v[n] == null ? "" : v[n];
    }

    /**
     * Chiede il file, lo legge, cerca i movimenti e mostra l'anteprima.
     * @return gli ID da cancellare, {@code null} se l'utente annulla, se il file non va o se non trova nulla
     */
    public static List<String> ScegliEAnteprima(Window owner) {
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA, ""));
        fc.setDialogTitle("Elenco dei movimenti da cancellare");
        fc.setFileFilter(new FileNameExtensionFilter("CSV o testo (*.csv, *.txt)", "csv", "txt"));
        if (fc.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null) return null;
        File f = fc.getSelectedFile();
        if (f.getParentFile() != null) DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA, f.getParentFile().getAbsolutePath());
        Criteri criteri;
        try {
            criteri = Leggi(f);
        } catch (FileNonValido ex) {
            Messaggi.WarningMessage("Cancellazione da CSV", "Il file non si può usare.", ex.getMessage(), owner);
            return null;
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Cancellazione da CSV", "Il file non si può aprire.", ex.getMessage(), owner);
            return null;
        }
        if (criteri.criteri().isEmpty()) {
            Messaggi.WarningMessage("Cancellazione da CSV", "Il file non contiene nessun criterio.",
                    "Dopo l'intestazione non c'è nessuna riga con un valore.", owner);
            return null;
        }
        Esito esito = Trova(criteri, Principale.MappaCryptoWallet);
        if (esito.ids().isEmpty()) {
            Messaggi.InfoMessage("Cancellazione da CSV", "Nessun movimento corrisponde.",
                    "Nessuna delle " + criteri.criteri().size() + " righe del file trova un movimento nell'archivio.", owner);
            return null;
        }
        return GUI_CancellaDaCsv.Mostra(owner, f.getName(), criteri, esito, Principale.MappaCryptoWallet) ? esito.ids() : null;
    }
}
