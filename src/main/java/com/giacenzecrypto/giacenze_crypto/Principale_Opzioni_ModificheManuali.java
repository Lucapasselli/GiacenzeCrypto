package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Esportazione in CSV delle modifiche fatte a mano sui movimenti (Opzioni, scheda Varie): per ogni movimento
 * modificato la riga com'era all'importazione e com'è adesso, i campi cambiati, ogni passaggio, e lo stato attuale
 * di ciò che lo storico non conosce (movimenti inseriti a mano, gruppi classificati).
 *
 * <p>Il file è il punto di partenza per <b>riapplicare</b> le modifiche su un archivio reimportato da zero, anche
 * senza {@code personale.mv.db}: per questo è un CSV unico e leggibile dal programma, non un Excel (un lettore di
 * Excel avrebbe richiesto una dipendenza in più nel jar). Il ripristino è in
 * {@link Principale_Opzioni_ModificheManuali_Ripristino}: qui c'è il formato, e quello è il contratto fra le due classi.</p>
 *
 * <p><b>Formato (versione {@value #FORMATO}).</b> Separatore {@code ;}, UTF-8 con BOM (Excel italiano lo apre
 * direttamente). I valori non contengono mai {@code ;} né a capo, con la stessa regola di
 * {@link Importazioni#SerializzaRiga} (il {@code ;} è tolto, l'a capo diventa uno spazio, nelle note {@code <br>}),
 * quindi non servono virgolette. La prima riga è {@code #...} e porta formato, numero di campi, versione del
 * programma e data. La seconda è l'intestazione. Poi una riga per record, con {@link #COLONNE_FISSE} colonne di
 * servizio seguite dai {@link Importazioni#ColonneTabella} campi del movimento (la colonna {@code N} è il campo
 * {@code [N]}). Il tipo di record è nella prima colonna:</p>
 * <ul>
 * <li>{@code INIZIALE}: la riga com'era prima della prima modifica. Chiave = lignaggio. Un'unione ne ha più d'una.</li>
 * <li>{@code FINALE}: la riga com'è adesso, con operazioni, ID prima/dopo, campi cambiati e differenze. Una
 * separazione ne ha più d'una. Un movimento che non esiste più ha l'INIZIALE e nessun FINALE.</li>
 * <li>{@code PASSAGGIO}: una modifica dello storico con la riga com'era subito prima. Serve a ricostruire
 * {@code MOVIMENTI_STORICO} su un archivio pulito. {@code Millis} è l'istante esatto.</li>
 * <li>{@code SENZA_STORICO}: movimento che lo storico non conosce ({@code Operazione} dice quale: inserito a mano,
 * o marcato M senza storico). Solo lo stato attuale.</li>
 * <li>{@code CLASSIFICAZIONE}: un movimento di un gruppo classificato ({@code [20]} collegati a righe generate dal
 * programma), originale o generato. Chiave = il più piccolo ID del gruppo. Solo lo stato attuale: il "prima" di
 * una classificazione è il movimento non classificato, che il reimport produce di nuovo.</li>
 * </ul>
 *
 * <p>La fonte è {@link MovimentiStorico}, quindi il perimetro è quello dello storico: modifica movimento, trasla
 * orario, unione, prezzo a zero, tipologia reward, sovrascrittura da reimport. Classificazione e separazione non
 * registrano lo storico, e le cancellazioni non sono registrate da nessuna parte.</p>
 *
 * <p>Logica pura in {@link #Costruisci}, scrittura in {@link #ScriviCsv}, dialogo in {@link #Esporta}.</p>
 */
public class Principale_Opzioni_ModificheManuali {

    private Principale_Opzioni_ModificheManuali() {
    }

    /** Versione del formato del file: un lettore futuro rifiuta quelle che non conosce. */
    static final int FORMATO = 1;

    static final String TIPO_INIZIALE = "INIZIALE";
    static final String TIPO_FINALE = "FINALE";
    static final String TIPO_PASSAGGIO = "PASSAGGIO";
    static final String TIPO_SENZA_STORICO = "SENZA_STORICO";
    static final String TIPO_CLASSIFICAZIONE = "CLASSIFICAZIONE";

    /** Colonne di servizio prima dei campi del movimento. */
    static final int C_TIPO = 0, C_CHIAVE = 1, C_PASSO = 2, C_DATA = 3, C_MILLIS = 4, C_OPERAZIONE = 5,
            C_ID_PRIMA = 6, C_ID_DOPO = 7, C_CAMPI = 8, C_DIFFERENZE = 9;
    static final int COLONNE_FISSE = 10;

    private static final String OPZIONE_CARTELLA = "ModificheManuali_Cartella";

    static final String CATEGORIA_INSERITO = "Inserito a mano";
    static final String CATEGORIA_MARCATO_M = "Marcato M senza storico";
    static final String CATEGORIA_ORIGINALE = "Originale";
    static final String CATEGORIA_GENERATO = "Generato dal programma";
    static final String STATO_NON_PRESENTE = "Movimento non più presente";

    /**
     * Campi che il motore ricalcola da solo (costi di carico, plusvalenza, "genera plusvalenza"), la segnalazione
     * errori e il lignaggio: cambiano a ogni ricalcolo e non sono una modifica dell'utente.
     */
    static final Set<Integer> CAMPI_NON_CONFRONTATI = Set.of(16, 17, 19, 33, 38, MovimentiStorico.CAMPO_LIGNAGGIO);

    /** Nomi dei campi della riga movimento, nell'ordine di {@code Importazioni} (la colonna N è il campo N). */
    static final String[] NOMI_CAMPI = {
        "ID", "Data e ora", "Numero movimenti", "Exchange", "Wallet", "Tipo transazione", "Dettaglio movimento",
        "Causale originale", "Moneta uscita", "Tipo moneta uscita", "Qta uscita", "Moneta entrata",
        "Tipo moneta entrata", "Qta entrata", "Valore come da CSV", "Valore in euro", "Costo di carico uscente",
        "Costo di carico entrante", "Tipo trasferimento", "Plusvalenza", "Riferimento trasferimenti", "Note",
        "Auto", "Blocco", "Hash / ID exchange", "DeFi nome token uscita", "DeFi address token uscita",
        "DeFi nome token entrata", "DeFi address token entrata", "Timestamp", "Address controparte",
        "Data fine trasferimento", "Movimento valorizzato", "Genera plusvalenza", "Rete",
        "Prezzo transazione originale", "Address provenienza", "Address destinazione", "Segnalazione errori",
        "Fonte dati", "Fonte prezzi", "Documento di origine", "Lignaggio", "Chiave commissioni collegate",
        "Marcatore derivato", "Chiave operazione"
    };

    private static final String[] INTESTAZIONE_FISSA = {"Tipo", "Chiave", "Passo", "Data", "Millis", "Operazione",
        "ID prima", "ID dopo", "Campi modificati", "Differenze"};

    /** Il contenuto del file: i record (senza intestazioni) e qualche conteggio per il messaggio finale. */
    public record Esportazione(List<String[]> record, int movimentiConStorico, int senzaStorico, int classificati) {

    }

    private static final SimpleDateFormat FORMATO_MODIFICA = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static String Modifica(String millis) {
        synchronized (FORMATO_MODIFICA) {
            return FORMATO_MODIFICA.format(new Date(Long.parseLong(millis)));
        }
    }

    private static String Campo(String[] v, int i) {
        return v != null && i < v.length && v[i] != null ? v[i] : "";
    }

    /** Un valore di servizio senza {@code ;} né a capo, che romperebbero il formato. */
    private static String Pulito(String s) {
        return s == null ? "" : s.replace(";", "").replace("\r\n", " ").replace("\r", " ").replace("\n", " ");
    }

    /** I campi di una riga come li scrive {@link Importazioni#SerializzaRiga}, già normalizzati a 46 campi. */
    private static String[] CampiPuliti(String[] v) {
        return Importazioni.DeserializzaRiga(Importazioni.SerializzaRiga(v));
    }

    private static String[] Record(String tipo, String chiave, String passo, String data, String millis,
            String operazione, String idPrima, String idDopo, String campi, String differenze, String[] riga) {
        String[] r = new String[COLONNE_FISSE + Importazioni.ColonneTabella];
        String[] fissi = {tipo, chiave, passo, data, millis, operazione, idPrima, idDopo, campi, differenze};
        for (int i = 0; i < COLONNE_FISSE; i++) {
            r[i] = Pulito(fissi[i]);
        }
        String[] campiRiga = CampiPuliti(riga);
        System.arraycopy(campiRiga, 0, r, COLONNE_FISSE, Importazioni.ColonneTabella);
        return r;
    }

    /** Le righe "iniziali" di una catena: quelle che nessun passaggio precedente della catena ha prodotto. */
    static List<String[]> Iniziali(List<String[]> versioni) {
        List<String[]> iniziali = new ArrayList<>();
        Set<String> viste = new HashSet<>();
        for (int i = 0; i < versioni.size(); i++) {
            String[] v = versioni.get(i);
            long data = Long.parseLong(v[1]);
            boolean prodotta = false;
            for (int j = 0; j < i && !prodotta; j++) {
                String[] w = versioni.get(j);
                //Strettamente prima: le N righe di un'unione hanno quasi lo stesso istante e nessuna è figlia dell'altra
                prodotta = Long.parseLong(w[1]) < data && Objects.equals(w[4], v[0]);
            }
            if (!prodotta && viste.add(v[0])) {
                iniziali.add(v);
            }
        }
        return iniziali;
    }

    /** Campi confrontabili che differiscono, nell'ordine dei campi. */
    static List<Integer> CampiCambiati(String[] iniziale, String[] finale) {
        List<Integer> campi = new ArrayList<>();
        for (int i = 0; i < Importazioni.ColonneTabella; i++) {
            if (!CAMPI_NON_CONFRONTATI.contains(i) && !Campo(iniziale, i).equals(Campo(finale, i))) {
                campi.add(i);
            }
        }
        return campi;
    }

    private static String NomeCampo(int i) {
        return "[" + i + "] " + (i < NOMI_CAMPI.length ? NOMI_CAMPI[i] : "");
    }

    private static String Breve(String s) {
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    /**
     * Costruisce i record da una mappa di movimenti e dallo storico. Pura: nessuna lettura di DB né di stato statico.
     *
     * <p>Una catena (lignaggio) non è sempre "un prima e un dopo": una unione ha più righe iniziali, una separazione
     * più righe finali (le due gambe portano lo stesso lignaggio), e un movimento cancellato dopo una modifica non
     * ne ha nessuna. Per questo i record INIZIALE e FINALE sono separati e legati dal lignaggio, e le differenze
     * sono calcolate rispetto alla riga iniziale solo quando ce n'è una.</p>
     *
     * @param movimenti movimenti vivi, ID -> riga
     * @param versioni storico, lignaggio -> versioni dalla più vecchia ({@link MovimentiStorico#TutteLeVersioni()})
     */
    public static Esportazione Costruisci(Map<String, String[]> movimenti, Map<String, List<String[]>> versioni) {
        List<String[]> record = new ArrayList<>();

        //Un solo passaggio sulla mappa per sapere chi porta quale lignaggio
        Map<String, List<String[]>> vivi = new LinkedHashMap<>();
        for (String[] m : movimenti.values()) {
            String l = Campo(m, MovimentiStorico.CAMPO_LIGNAGGIO);
            if (!l.isBlank() && versioni.containsKey(l)) {
                vivi.computeIfAbsent(l, k -> new ArrayList<>()).add(m);
            }
        }

        //Ordine del file: per data della prima modifica
        List<Map.Entry<String, List<String[]>>> catene = new ArrayList<>(versioni.entrySet());
        catene.removeIf(e -> e.getValue().isEmpty());
        catene.sort((a, b) -> Long.compare(Long.parseLong(a.getValue().get(0)[1]), Long.parseLong(b.getValue().get(0)[1])));

        Set<String> conStoria = new HashSet<>();
        for (Map.Entry<String, List<String[]>> catena : catene) {
            String lignaggio = catena.getKey();
            List<String[]> v = catena.getValue();
            List<String[]> iniziali = Iniziali(v);
            List<String[]> finali = vivi.getOrDefault(lignaggio, List.of());
            Set<String> operazioni = new LinkedHashSet<>();
            int passo = 0;
            for (String[] p : v) {
                operazioni.add(p[3]);
                record.add(Record(TIPO_PASSAGGIO, lignaggio, String.valueOf(++passo), Modifica(p[1]), p[1], p[3],
                        p[0], p[4], "", "", Importazioni.DeserializzaRiga(p[2])));
            }
            String ultima = Modifica(v.get(v.size() - 1)[1]);
            List<String[]> righeIniziali = new ArrayList<>();
            StringBuilder idIniziali = new StringBuilder();
            int n = 0;
            for (String[] i : iniziali) {
                String[] riga = Importazioni.DeserializzaRiga(i[2]);
                righeIniziali.add(riga);
                idIniziali.append(idIniziali.length() > 0 ? " | " : "").append(i[0]);
                record.add(Record(TIPO_INIZIALE, lignaggio, String.valueOf(++n), Modifica(i[1]), i[1], "", i[0], "",
                        "", finali.isEmpty() ? STATO_NON_PRESENTE : "", riga));
            }
            for (String[] f : finali) {
                conStoria.add(Campo(f, 0));
                String campi = "";
                String testo = "";
                if (righeIniziali.size() == 1) {
                    List<String> nomi = new ArrayList<>();
                    List<String> coppie = new ArrayList<>();
                    for (int c : CampiCambiati(righeIniziali.get(0), f)) {
                        nomi.add(NomeCampo(c));
                        coppie.add(NomeCampo(c) + ": " + Breve(Campo(righeIniziali.get(0), c)) + " -> " + Breve(Campo(f, c)));
                    }
                    campi = String.join(", ", nomi);
                    testo = String.join(" | ", coppie);
                } else {
                    testo = "Più movimenti di partenza: confrontare le righe INIZIALE della stessa chiave";
                }
                record.add(Record(TIPO_FINALE, lignaggio, "", ultima, v.get(v.size() - 1)[1],
                        String.join(", ", operazioni), idIniziali.toString(), Campo(f, 0), campi, testo, f));
            }
        }
        int conStorico = catene.size();
        List<String[]> classificati = Classificazioni(movimenti);
        //Un movimento classificato resta marcato M ma il suo stato è già nel gruppo: non va esportato due volte
        for (String[] r : classificati) {
            conStoria.add(r[C_ID_DOPO]);
        }
        List<String[]> senzaStorico = SenzaStorico(movimenti, conStoria);
        record.addAll(senzaStorico);
        record.addAll(classificati);
        return new Esportazione(record, conStorico, senzaStorico.size(), classificati.size());
    }

    /**
     * I movimenti toccati a mano che lo storico non conosce: gli inseriti a mano (fonte dati {@code E}) e quelli
     * marcati {@code M} senza storico (modificati prima che esistesse il lignaggio, o copiati con Duplica). I
     * movimenti già esportati altrove ({@code esclusi}: catene dello storico e gruppi classificati) non si ripetono.
     */
    static List<String[]> SenzaStorico(Map<String, String[]> movimenti, Set<String> esclusi) {
        List<String[]> righe = new ArrayList<>();
        Map<String, String[]> ordinati = new TreeMap<>(movimenti);
        for (String[] m : ordinati.values()) {
            String id = Campo(m, 0);
            if (esclusi.contains(id) || "AU".equals(Campo(m, 22))) {
                continue;
            }
            String categoria = "E".equals(Campo(m, 39)) ? CATEGORIA_INSERITO : "M".equals(Campo(m, 22)) ? CATEGORIA_MARCATO_M : null;
            if (categoria != null) {
                righe.add(Record(TIPO_SENZA_STORICO, id, "", Campo(m, 1), "", categoria, "", id, "", "", m));
            }
        }
        return righe;
    }

    /**
     * I gruppi classificati: ogni riga generata dal programma ({@code [22] = AU}) con i movimenti che il suo campo
     * {@code [20]} collega, originali compresi, raggruppati per componente connessa. La chiave del gruppo è il più
     * piccolo ID.
     */
    static List<String[]> Classificazioni(Map<String, String[]> movimenti) {
        Map<String, String> padre = new HashMap<>();
        Set<String> coinvolti = new LinkedHashSet<>();
        for (String[] m : movimenti.values()) {
            if (!"AU".equals(Campo(m, 22))) {
                continue;
            }
            String id = Campo(m, 0);
            coinvolti.add(id);
            for (String r : Campo(m, 20).split(",")) {
                if (!r.isBlank() && movimenti.containsKey(r.trim())) {
                    coinvolti.add(r.trim());
                    Unisci(padre, id, r.trim());
                }
            }
        }
        //Gli originali collegati portano a loro volta il gruppo nel loro [20]: stesso gruppo, anche se la riga AU non li nomina
        for (String id : new ArrayList<>(coinvolti)) {
            for (String r : Campo(movimenti.get(id), 20).split(",")) {
                if (!r.isBlank() && coinvolti.contains(r.trim())) {
                    Unisci(padre, id, r.trim());
                }
            }
        }
        Map<String, String> chiaviGruppo = new HashMap<>();
        for (String id : coinvolti) {
            chiaviGruppo.merge(Radice(padre, id), id, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
        List<String[]> righe = new ArrayList<>();
        for (String id : new TreeSet<>(coinvolti)) {
            String[] m = movimenti.get(id);
            righe.add(Record(TIPO_CLASSIFICAZIONE, chiaviGruppo.get(Radice(padre, id)), "", Campo(m, 1), "",
                    "AU".equals(Campo(m, 22)) ? CATEGORIA_GENERATO : CATEGORIA_ORIGINALE, "", id, "", "", m));
        }
        righe.sort((a, b) -> a[C_CHIAVE].equals(b[C_CHIAVE])
                ? a[C_ID_DOPO].compareTo(b[C_ID_DOPO]) : a[C_CHIAVE].compareTo(b[C_CHIAVE]));
        return righe;
    }

    private static String Radice(Map<String, String> padre, String id) {
        String x = id;
        while (padre.containsKey(x) && !padre.get(x).equals(x)) {
            x = padre.get(x);
        }
        return x;
    }

    private static void Unisci(Map<String, String> padre, String a, String b) {
        padre.putIfAbsent(a, a);
        padre.putIfAbsent(b, b);
        padre.put(Radice(padre, a), Radice(padre, b));
    }

    /** Il testo del file: commento di formato, intestazione e un record per riga. */
    static String Testo(Esportazione e) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("#GiacenzeCrypto ModificheManuali;formato=").append(FORMATO)
                .append(";campi=").append(Importazioni.ColonneTabella)
                .append(";versione=").append(Pulito(VarStatiche.Versione))
                .append(";generato=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .append("\r\n");
        List<String> colonne = new ArrayList<>(List.of(INTESTAZIONE_FISSA));
        for (int i = 0; i < Importazioni.ColonneTabella; i++) {
            colonne.add(NomeCampo(i));
        }
        sb.append(String.join(";", colonne)).append("\r\n");
        for (String[] r : e.record()) {
            sb.append(String.join(";", r)).append("\r\n");
        }
        return sb.toString();
    }

    /** Scrive il file (UTF-8 con BOM). */
    static void ScriviCsv(Esportazione e, File f) throws IOException {
        Files.writeString(f.toPath(), Testo(e), StandardCharsets.UTF_8);
    }

    /**
     * Chiede dove salvare e scrive il file.
     *
     * @return il file scritto, {@code null} se annullato o fallito
     */
    public static File Esporta(Window owner) {
        Esportazione esp = Costruisci(Principale.MappaCryptoWallet, MovimentiStorico.TutteLeVersioni());
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA, ""));
        fc.setDialogTitle("Esporta le modifiche manuali dei movimenti");
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        fc.setSelectedFile(new File(fc.getCurrentDirectory(),
                "Modifiche_manuali_movimenti_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv"));
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null) {
            return null;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) {
            f = new File(f.getParentFile(), f.getName() + ".csv");
        }
        if (f.getParentFile() != null) {
            DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA, f.getParentFile().getAbsolutePath());
        }
        try {
            ScriviCsv(esp, f);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            Messaggi.WarningMessage("Esportazione modifiche manuali",
                    "Non è stato possibile scrivere il file.",
                    "Verifica che la cartella sia scrivibile e che il file non sia aperto in un altro programma.", owner);
            return null;
        }
        Messaggi.SuccessMessage("Esportazione modifiche manuali",
                "Movimenti modificati a mano con storico: " + esp.movimentiConStorico(),
                "File: " + f.getAbsolutePath() + "\n\nNel file ci sono anche " + esp.senzaStorico()
                + " movimenti toccati a mano senza storico e " + esp.classificati()
                + " movimenti di gruppi classificati, di cui si conosce solo lo stato attuale. "
                + "Le cancellazioni non vengono registrate.",
                owner);
        return f;
    }
}
