package com.giacenzecrypto.giacenze_crypto;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jsoup.Jsoup;

/**
 * Operazioni sull'archivio delle dichiarazioni che coinvolgono l'utente: la generazione congiunta dei
 * file di una dichiarazione (PDF ed Excel) e le azioni del pannello <i>Archivio dichiarazioni</i>.
 *
 * <p>Segue lo schema delle altre estrazioni ({@link Principale_Backup}, {@link Principale_GiacenzeaData}):
 * metodi {@code public static}, nessun campo Swing, la finestra a cui agganciare i dialoghi passata da
 * chi chiama. L'archivio vero e proprio (cartelle, manifest, definitiva) è {@link Dichiarazioni}.
 */
public final class Principale_Dichiarazioni {

    /** Ultima cartella scelta per esportare una dichiarazione ({@code personale.mv.db}). */
    static final String OPZIONE_CARTELLA_ESPORTA = "Dichiarazioni_CartellaEsporta";

    private Principale_Dichiarazioni() {
    }

    /**
     * Scrive i file di una dichiarazione nella cartella che riceve. Ogni errore, compreso un PDF che non
     * risulta valido, deve diventare un'eccezione con un messaggio che dica quale file non è riuscito.
     */
    @FunctionalInterface
    public interface Scrittore {

        void Scrivi(File Cartella) throws Exception;
    }

    // =================================================================================================
    // GENERAZIONE
    // =================================================================================================
    /**
     * Genera e archivia una dichiarazione: tutti i file nascono dallo stesso calcolo e vengono registrati
     * insieme, oppure nessuno.
     *
     * <p>Se l'ultima dichiarazione dello stesso anno e famiglia ha gli stessi valori, opzioni ed errori
     * (stessa {@link Dichiarazioni.Dichiarazione#Impronta()}) e i suoi file sono integri, si chiede se
     * aprire quella invece di archiviarne un doppione.
     *
     * @param Bozza i valori stampati (anno, famiglia, totali, righi, opzioni, errori)
     * @param FileAttesi i nomi dei file che lo scrittore deve produrre
     * @param S lo scrittore dei file
     * @param DaAprire il file da aprire alla fine (uno di {@code FileAttesi}), {@code null} per nessuno
     * @param owner la finestra a cui agganciare i dialoghi
     * @return la dichiarazione archiviata (o quella già presente scelta dall'utente), {@code null} se
     *         annullata o fallita
     */
    public static Dichiarazioni.Dichiarazione Genera(Dichiarazioni.Dichiarazione Bozza, List<String> FileAttesi,
            Scrittore S, String DaAprire, Window owner) {
        Dichiarazioni.Dichiarazione uguale = Dichiarazioni.UltimaUguale(Bozza);
        if (uguale != null && TuttiIntegri(uguale)) {
            AppDialog.DialogResult r = AppDialog.builder(owner)
                    .windowTitle("Dichiarazione già archiviata")
                    .bodyTitle("Questa dichiarazione è già nell'archivio")
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.INFO)
                    .message("L'ultima dichiarazione " + Bozza.Fam.Etichetta + " " + Bozza.Anno
                            + " archiviata il " + uguale.Creata + " ha gli stessi valori, le stesse opzioni e gli stessi errori.")
                    .details("Puoi aprire i file già archiviati, oppure archiviarne una nuova copia.")
                    .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                            .role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("nuova", "Archivia una nuova copia")
                            .role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("apri", "Apri quella archiviata")
                            .role(AppDialog.ActionRole.PRIMARY).build())
                    .showDialog();
            if (r == null || r.isAction("annulla")) {
                return null;
            }
            if (r.isAction("apri")) {
                if (DaAprire != null) {
                    ApriFile(new File(uguale.Cartella, DaAprire), owner);
                }
                return uguale;
            }
        }

        File temporanea = null;
        Dichiarazioni.Dichiarazione registrata;
        try {
            temporanea = Dichiarazioni.NuovaCartellaTemporanea();
            S.Scrivi(temporanea);
            for (String nome : FileAttesi) {
                if (nome.toLowerCase().endsWith(".pdf") && !Stampe.PdfValido(new File(temporanea, nome))) {
                    throw new IOException("il file " + nome + " non risulta un PDF valido");
                }
            }
            registrata = Dichiarazioni.Registra(temporanea, Bozza, FileAttesi);
        } catch (Exception e) {
            Dichiarazioni.Scarta(temporanea);
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Dichiarazione non generata",
                    "Non è stato archiviato nessun file.",
                    "La generazione si è interrotta: " + (e.getMessage() == null ? e.toString() : e.getMessage())
                    + "<br><br>PDF ed Excel si archiviano solo insieme, così da avere sempre gli stessi valori.",
                    owner);
            return null;
        }
        if (DaAprire != null) {
            ApriFile(new File(registrata.Cartella, DaAprire), owner);
        }
        return registrata;
    }

    private static boolean TuttiIntegri(Dichiarazioni.Dichiarazione D) {
        for (Dichiarazioni.StatoFile s : Dichiarazioni.VerificaIntegrita(D).values()) {
            if (s != Dichiarazioni.StatoFile.INTEGRO) {
                return false;
            }
        }
        return !D.File.isEmpty();
    }

    /**
     * Scrive l'Excel di dettaglio del quadro T/RT di un anno.
     *
     * @param f il file da scrivere
     * @param Anno l'anno d'imposta
     * @param IntestazioneRiepilogo intestazioni del rigo dell'anno (tabella principale RT)
     * @param RigaRiepilogo il rigo dell'anno
     * @param IntestazioneMonete intestazioni del dettaglio per gruppo wallet e moneta
     * @param Monete righe del dettaglio ({@code Calcoli_RT.AnalisiPlus.RitornaTabellaAnno})
     * @param Tipologie plusvalenze per tipologia e mese
     * @param Movimenti movimenti fiscalmente rilevanti dell'anno
     *        ({@link Principale_RT_Tipologie#MovimentiRilevanti}), colonne
     *        {@link Principale_RT_Tipologie#COLONNE_MOVIMENTI}
     * @throws IOException se la scrittura fallisce
     */
    public static void RT_CreaExcel(File f, String Anno, String[] IntestazioneRiepilogo, Object[] RigaRiepilogo,
            String[] IntestazioneMonete, List<Object[]> Monete, Principale_RT_Tipologie.DatiTipologie Tipologie,
            List<Object[]> Movimenti) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            Workbook wb = new Workbook(fos, "excel1", "1.0");

            Worksheet ws = wb.newWorksheet("Riepilogo " + Anno);
            ws.value(0, 0, "Quadro T/RT - riepilogo anno " + Anno);
            ScriviRiga(ws, 1, IntestazioneRiepilogo);
            ScriviRiga(ws, 2, RigaRiepilogo);
            ws.finish();

            ws = wb.newWorksheet("Monete " + Anno);
            ScriviRiga(ws, 0, IntestazioneMonete);
            int r = 1;
            for (Object[] riga : Monete) {
                ScriviRiga(ws, r++, riga);
            }
            ws.finish();

            ws = wb.newWorksheet("Tipologie " + Anno);
            ScriviRiga(ws, 0, Principale_RT_Tipologie.COLONNE_TIPOLOGIE);
            r = 1;
            for (Map.Entry<String, BigDecimal[]> e : Tipologie.perTipologia.entrySet()) {
                ScriviRiga(ws, r++, RigaTipologia(e.getKey(), e.getValue()));
            }
            if (!Tipologie.perTipologia.isEmpty()) {
                ScriviRiga(ws, r, RigaTipologia(Principale_RT_Tipologie.RIGA_TOTALE, Tipologie.totaleMensile));
            }
            ws.finish();

            ws = wb.newWorksheet("Movimenti rilevanti " + Anno);
            ScriviRiga(ws, 0, Principale_RT_Tipologie.COLONNE_MOVIMENTI);
            r = 1;
            for (Object[] riga : Movimenti) {
                ScriviRiga(ws, r++, riga);
            }
            ws.finish();

            wb.finish();
        }
    }

    /**
     * Scrive l'Excel di dettaglio del quadro W/RW di un anno: il prospetto da conservare per la
     * compilazione aggregata del quadro (istruzioni RW, "apposito prospetto ... in cui sono specificati i
     * dati delle singole attività").
     *
     * <p>Un foglio di riepilogo con tutti i righi del quadro, poi per ogni <b>gruppo wallet</b> (non per
     * rigo: un gruppo diviso per periodi o con la liquidità ha più righi, e prima ognuno ripeteva gli stessi
     * fogli):
     * <ul>
     *   <li><i>Calcoli</i>: le righe di dettaglio del motore RW, con il <b>periodo</b> del rigo a cui
     *       appartengono (quello che contiene la data di fine della riga, come nel quadro);</li>
     *   <li><i>Giacenze</i>: inizio e fine di ogni periodo, dalla <b>stessa fotografia</b> usata dal calcolo
     *       del quadro ({@code Calcoli_RW_Giacenze}, passata da chi chiama): niente euro, giacenze negative a
     *       zero e segnalate, nessun prezzo chiesto di nuovo. Sono i valori del quadro con l'opzione A e nei
     *       periodi col bollo pagato; con la media ponderata il quadro somma le righe di <i>Calcoli</i>;</li>
     *   <li><i>Liquidità</i>: i righi FIAT con giacenza media, codice bene, Stato estero e IVAFE, se il
     *       gruppo ne ha;</li>
     *   <li><i>Movimenti</i>: i movimenti dell'anno del gruppo.</li>
     * </ul>
     *
     * @param Quadro il model di {@code RW_Tabella} (righi del quadro)
     * @param Anno l'anno d'imposta
     * @param f il file da scrivere
     * @param Fotografia gruppo → giacenze per periodo, quella del calcolo del quadro
     * @throws IOException se la scrittura fallisce
     */
    public static void RW_CreaExcel(javax.swing.table.TableModel Quadro, String Anno, File f,
            Map<String, List<String[]>> Fotografia) throws IOException {
        Map<String, String[]> alias = DatabaseH2.Pers_GruppoAlias_LeggiTabella();
        //I gruppi nell'ordine dei righi del quadro, letti dalla chiave di sintesi (colonna 6: "Wallet NN|...")
        java.util.LinkedHashSet<String> gruppi = new java.util.LinkedHashSet<>();
        for (int i = 0; i < Quadro.getRowCount(); i++) {
            Object k = Quadro.getValueAt(i, 6);
            String g = k == null ? "" : k.toString().split("\\|")[0].trim();
            if (!g.isEmpty()) {
                gruppi.add(g);
            }
        }
        //I movimenti dell'anno divisi per gruppo in una sola passata
        Map<String, List<String[]>> movimenti = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] != null && v[1].startsWith(Anno)) {
                movimenti.computeIfAbsent(DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true), k -> new ArrayList<>()).add(v);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(f)) {
            Workbook wb = new Workbook(fos, "excel1", "1.0");

            Worksheet ws = wb.newWorksheet("Riepilogo " + Anno);
            ws.value(0, 0, "Quadro W/RW - riepilogo anno " + Anno);
            ScriviRiga(ws, 1, new Object[]{"Rigo", "Natura", "Periodo", "Valore iniziale", "Valore finale",
                "Giorni di detenzione", "IC / IVAFE dovuta", "Bollo pagato", "Stato estero", "Errori"});
            for (int i = 0; i < Quadro.getRowCount(); i++) {
                String nome = Testo(Quadro.getValueAt(i, 0));
                String periodo = "anno intero";
                java.util.regex.Matcher mt = java.util.regex.Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}) / (\\d{4}-\\d{2}-\\d{2})\\]").matcher(nome);
                if (mt.find()) {
                    periodo = EtichettaPeriodo(mt.group(1), mt.group(2));
                    nome = nome.substring(0, mt.start()).trim();
                }
                String natura = Quadro.getColumnCount() > 8 ? Testo(Quadro.getValueAt(i, 8)) : "";
                ScriviRiga(ws, i + 2, new Object[]{nome, natura.isEmpty() ? "CRYPTO" : natura, periodo,
                    Quadro.getValueAt(i, 1), Quadro.getValueAt(i, 2), Quadro.getValueAt(i, 3), Quadro.getValueAt(i, 5),
                    Quadro.getValueAt(i, 7), Quadro.getColumnCount() > 9 ? Quadro.getValueAt(i, 9) : "",
                    Quadro.getValueAt(i, 4)});
            }
            ws.finish();

            for (String gruppo : gruppi) {
                String[] a = alias.get(gruppo);
                String num = gruppo.contains(" ") ? gruppo.substring(gruppo.indexOf(' ') + 1).trim() : gruppo;
                String titolo = num + (a != null && a.length > 1 && a[1] != null && !a[1].isBlank() ? " ( " + a[1] + " )" : "");
                List<String[]> tratti = Calcoli_RW_PeriodiCrypto.trattiCrypto(gruppo, Anno);

                List<String[]> calcoli = Principale.Mappa_RW_ListeXGruppoWallet.get(gruppo);
                if (calcoli != null && !calcoli.isEmpty()) {
                    ws = wb.newWorksheet(num + " - Calcoli");
                    ws.value(0, 0, titolo + " - righe di dettaglio del quadro W/RW " + Anno);
                    ScriviRiga(ws, 1, new Object[]{"Periodo", "Gruppo iniziale", "Moneta iniziale", "Qta iniziale",
                        "Data iniziale", "Valore iniziale", "Gruppo finale", "Moneta finale", "Qta finale", "Data finale",
                        "Valore finale", "Giorni di detenzione", "Motivo fine detenzione", "Movimento di apertura",
                        "Movimento di chiusura", "Errori"});
                    int r = 2;
                    for (String[] l : calcoli) {
                        ScriviRiga(ws, r++, new Object[]{PeriodoDellaData(Data(l, 9), tratti),
                            NomeGruppo(l[1], alias), l[2], l[3], l[4], l[5], NomeGruppo(l[6], alias), l[7], l[8], l[9],
                            l[10], l[11], l[12], l[13], l[14], l.length > 15 ? l[15] : ""});
                    }
                    ws.finish();
                }

                List<String[]> foto = Fotografia == null ? null : Fotografia.get(gruppo);
                if (foto != null && !foto.isEmpty()) {
                    ws = wb.newWorksheet(num + " - Giacenze");
                    //Sono i valori del quadro solo con l'opzione A e nei periodi col bollo pagato: con la media
                    //ponderata il quadro somma le righe di Calcoli, e le giacenze ai confini restano un riscontro
                    ws.value(0, 0, titolo + " - giacenze di inizio e fine di ogni periodo " + Anno
                            + ", calcolate come nel quadro (sono i valori del quadro con l'opzione A e nei periodi col bollo pagato)");
                    ScriviRiga(ws, 1, new Object[]{"Periodo", "Moneta", "Data inizio", "Qta inizio", "Valore inizio",
                        "Data fine", "Qta fine", "Valore fine", "Errori"});
                    int r = 2;
                    for (String[] l : foto) {
                        ScriviRiga(ws, r++, new Object[]{PeriodoDellaData(Data(l, 9), tratti), l[2], l[4], l[3], l[5],
                            l[9], l[8], l[10], l.length > 15 ? l[15] : ""});
                    }
                    ws.finish();
                }

                List<String[]> fiat = Principale.Mappa_RW_ListeXGruppoWallet_Fiat.get(gruppo);
                if (fiat != null && !fiat.isEmpty()) {
                    ws = wb.newWorksheet(num + " - Liquidità");
                    ws.value(0, 0, titolo + " - liquidità presso l'intermediario estero " + Anno);
                    ScriviRiga(ws, 1, new Object[]{"Periodo", "Valute", "Data inizio", "Valore iniziale", "Data fine",
                        "Saldo finale", "Giacenza media", "Valore finale dichiarato", "Valore massimo", "Giorni",
                        "Codice bene", "Stato estero", "IVAFE dovuta", "Solo monitoraggio", "Avvisi"});
                    int r = 2;
                    for (String[] d : fiat) {
                        ScriviRiga(ws, r++, new Object[]{EtichettaPeriodo(Data(d, 4), Data(d, 9)), d[2], d[4], d[5],
                            d[9], d[10], d[Calcoli_RW_Fiat.FIAT_COL_VALORE_MEDIO],
                            Calcoli_RW_Fiat.valoreFinaleDichiarato(d), d[Calcoli_RW_Fiat.FIAT_COL_VALORE_MASSIMO], d[11],
                            d[Calcoli_RW_Fiat.FIAT_COL_CODICE_BENE], d[Calcoli_RW_Fiat.FIAT_COL_STATO_ESTERO],
                            d[Calcoli_RW_Fiat.FIAT_COL_IVAFE], d[Calcoli_RW_Fiat.FIAT_COL_SOLO_MONITORAGGIO], d[15]});
                    }
                    ws.finish();
                }

                ws = wb.newWorksheet(num + " - Movimenti");
                ws.value(0, 0, titolo + " - movimenti " + Anno);
                ScriviRiga(ws, 1, new Object[]{"ID", "Data", "Wallet Principale/Exchange", "Dettaglio Wallet",
                    "Rete Wallet", "Tipo Transazione", "Token Venduto/Ceduto", "Tipo Token Venduto/Ceduto",
                    "Address Token Venduto/Ceduto", "Quantità Venduta/Ceduta", "Token Acquistato/Ricevuto",
                    "Tipo Token Acquistato/Ricevuto", "Address Token Acquistato/Ricevuto", "Quantità Acquistata/Ricevuta",
                    "Valore Transazione", "Riferimento Trasferimenti", "Note"});
                int r = 2;
                for (String[] v : movimenti.getOrDefault(gruppo, List.of())) {
                    ScriviRiga(ws, r++, new Object[]{v[0], v[1], v[3], v[4], Funzioni.TrovaReteDaID(v[0]), v[5],
                        v[8], v[9], v[26], v[10], v[11], v[12], v[28], v[13], v[15], v[20], v[21]});
                }
                ws.finish();
            }
            wb.finish();
        }
    }

    /** Il giorno ({@code yyyy-MM-dd}) di una colonna data-ora di una riga, vuoto se manca. */
    private static String Data(String[] l, int Colonna) {
        String d = l.length > Colonna && l[Colonna] != null ? l[Colonna].trim() : "";
        return d.length() >= 10 ? d.substring(0, 10) : d;
    }

    /** {@code "Wallet 04"} → {@code "04 ( Binance )"} se il gruppo ha un alias. */
    private static String NomeGruppo(String Gruppo, Map<String, String[]> Alias) {
        String[] a = Gruppo == null ? null : Alias.get(Gruppo);
        if (a == null || a.length < 2 || a[1] == null || a[1].isBlank() || !Gruppo.contains(" ")) {
            return Gruppo == null ? "" : Gruppo;
        }
        return Gruppo.substring(Gruppo.indexOf(' ') + 1).trim() + " ( " + a[1] + " )";
    }

    /** {@code "dal 01/01 al 30/06"}, oppure {@code "anno intero"} per 1° gennaio - 31 dicembre dello stesso anno. */
    static String EtichettaPeriodo(String Inizio, String Fine) {
        if (Inizio.length() < 10 || Fine.length() < 10) {
            return "";
        }
        if (Inizio.endsWith("-01-01") && Fine.endsWith("-12-31") && Inizio.substring(0, 4).equals(Fine.substring(0, 4))) {
            return "anno intero";
        }
        return "dal " + Inizio.substring(8, 10) + "/" + Inizio.substring(5, 7)
                + " al " + Fine.substring(8, 10) + "/" + Fine.substring(5, 7);
    }

    /**
     * Il periodo a cui appartiene una riga con quella data di fine: il tratto che la contiene, la stessa regola
     * con cui il quadro attribuisce le righe di dettaglio ai righi di periodo.
     *
     * @param DataFine giorno {@code yyyy-MM-dd}
     * @param Tratti i tratti CRYPTO del gruppo ({@link Calcoli_RW_PeriodiCrypto#trattiCrypto})
     * @return l'etichetta del periodo, vuota se nessun tratto la contiene
     */
    static String PeriodoDellaData(String DataFine, List<String[]> Tratti) {
        for (String[] t : Tratti) {
            String di = t[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO];
            String df = t[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE];
            if (DataFine.compareTo(di) >= 0 && DataFine.compareTo(df) <= 0) {
                return EtichettaPeriodo(di, df);
            }
        }
        return "";
    }

    private static String Testo(Object v) {
        return v == null ? "" : Jsoup.parse(v.toString()).text().trim();
    }

    private static Object[] RigaTipologia(String Etichetta, BigDecimal[] Mesi) {
        Object[] riga = new Object[14];
        riga[0] = Etichetta;
        for (int m = 0; m < 12; m++) {
            riga[m + 1] = Mesi[m];
        }
        riga[13] = Principale_RT_Tipologie.somma(Mesi);
        return riga;
    }

    /** Scrive una riga: i numeri come numeri, il resto come testo ripulito dall'HTML. */
    private static void ScriviRiga(Worksheet ws, int Riga, Object[] Valori) {
        for (int c = 0; c < Valori.length; c++) {
            Object v = Valori[c];
            if (v instanceof BigDecimal b) {
                ws.value(Riga, c, b);
            } else if (v instanceof Number n) {
                ws.value(Riga, c, n.doubleValue());
            } else {
                String s = v == null ? "" : Jsoup.parse(v.toString()).text();
                if (Funzioni.isNumeric(s, false)) {
                    ws.value(Riga, c, new BigDecimal(s));
                } else {
                    ws.value(Riga, c, s);
                }
            }
        }
    }

    // =================================================================================================
    // AZIONI DEL PANNELLO
    // =================================================================================================
    /**
     * Segna la dichiarazione come definitiva, chiedendo conferma (con un avviso in più se aveva errori,
     * e dicendo quale definitiva precedente perde il segno).
     *
     * @return {@code true} se il segno è stato messo
     */
    public static boolean SegnaDefinitiva(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null || D.Definitiva) {
            return false;
        }
        Dichiarazioni.Dichiarazione precedente = Dichiarazioni.Definitiva(D.Fam, D.Anno);
        StringBuilder dettagli = new StringBuilder();
        dettagli.append("Serve a ricordare quale delle dichiarazioni ").append(D.Fam.Etichetta).append(" ")
                .append(D.Anno).append(" è quella presentata.");
        if (precedente != null) {
            dettagli.append("<br><br>La dichiarazione del ").append(precedente.Creata)
                    .append(", oggi definitiva, non lo sarà più.");
        }
        if (!D.Errori.isBlank()) {
            dettagli.append("<br><br><b>Al momento della stampa c'erano degli errori:</b> ").append(D.Errori);
        }
        dettagli.append("<br><br>Viene creato anche un backup dei dati come sono adesso, collegato alla dichiarazione:"
                + " serve a ricostruire i valori dichiarati anche dopo modifiche successive ai movimenti.");
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Dichiarazione definitiva")
                .bodyTitle("Segnare come definitiva?")
                .showTitleInBody(true)
                .theme()
                .type(D.Errori.isBlank() ? AppDialog.DialogType.INFO : AppDialog.DialogType.WARNING)
                .message("Dichiarazione " + D.Fam.Etichetta + " " + D.Anno + " del " + D.Creata)
                .details(dettagli.toString())
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("segna", "Segna come definitiva")
                        .role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (r == null || !r.isAction("segna")) {
            return false;
        }
        try {
            Dichiarazioni.ImpostaDefinitiva(D, true);
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Dichiarazione definitiva", "Non è stato possibile salvare la scelta.",
                    String.valueOf(e.getMessage()), owner);
            return false;
        }
        File zip = CreaBackup(owner);
        if (zip == null) {
            Messaggi.WarningMessage("Dichiarazione definitiva", "La dichiarazione è definitiva, ma il backup non è stato creato.",
                    "Si può creare a mano da Opzioni - Backup / Ripristino.", owner);
            return true;
        }
        try {
            Dichiarazioni.ImpostaBackup(D, zip.getName());
            return true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Dichiarazione definitiva", "Non è stato possibile salvare la scelta.",
                    String.valueOf(e.getMessage()), owner);
            return false;
        }
    }

    /**
     * Crea un backup con le opzioni predefinite (prezzi necessari, niente chiavi API), con la finestra di
     * avanzamento ma senza chiedere nulla: è il backup collegato a una definitiva.
     *
     * @return l'archivio creato, {@code null} se interrotto o fallito
     */
    private static File CreaBackup(Window owner) {
        final File[] creato = new File[1];
        Download progress = new Download();
        progress.setLocationRelativeTo(owner);
        Thread t = new Thread(() -> {
            progress.Titolo("Backup della dichiarazione definitiva");
            progress.SetLabel("Preparazione...");
            creato[0] = Backup_Restore.Esegui(new Backup_Restore.OpzioniBackup(), progress);
            progress.ChiudiFinestra();
        });
        progress.SetThread(t);
        t.start();
        progress.setVisible(true);
        return creato[0];
    }

    /** Toglie il segno di definitiva, previa conferma. @return {@code true} se tolto */
    public static boolean TogliDefinitiva(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null || !D.Definitiva) {
            return false;
        }
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Dichiarazione definitiva")
                .bodyTitle("Togliere il segno di definitiva?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Dichiarazione " + D.Fam.Etichetta + " " + D.Anno + " del " + D.Creata)
                .details("Per l'anno " + D.Anno + " non resterà nessuna dichiarazione " + D.Fam.Etichetta
                        + " definitiva. I file non vengono toccati.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("togli", "Togli il segno")
                        .role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (r == null || !r.isAction("togli")) {
            return false;
        }
        try {
            Dichiarazioni.ImpostaDefinitiva(D, false);
            return true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Dichiarazione definitiva", "Non è stato possibile salvare la scelta.",
                    String.valueOf(e.getMessage()), owner);
            return false;
        }
    }

    /** Modifica la nota. @return {@code true} se cambiata */
    public static boolean ModificaNota(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null) {
            return false;
        }
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Nota della dichiarazione")
                .bodyTitle("Nota")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Dichiarazione " + D.Fam.Etichetta + " " + D.Anno + " del " + D.Creata)
                .details("Ad esempio la data di invio o il numero di protocollo telematico.")
                .textAreaField("Nota:", D.Nota)
                .textAreaSize(8, 40)
                .resizable(true)
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("salva", "Salva")
                        .role(AppDialog.ActionRole.PRIMARY).build())
                .showDialog();
        if (r == null || !r.isAction("salva")) {
            return false;
        }
        try {
            Dichiarazioni.ImpostaNota(D, r.getInputValue());
            return true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Nota", "Non è stato possibile salvare la nota.", String.valueOf(e.getMessage()), owner);
            return false;
        }
    }

    /**
     * Elimina le dichiarazioni, previa conferma. Se fra queste c'è una definitiva la conferma è doppia.
     *
     * @return {@code true} se almeno una è stata eliminata
     */
    public static boolean Elimina(List<Dichiarazioni.Dichiarazione> Elenco, Window owner) {
        if (Elenco == null || Elenco.isEmpty()) {
            return false;
        }
        int definitive = 0;
        for (Dichiarazioni.Dichiarazione d : Elenco) {
            if (d.Definitiva) {
                definitive++;
            }
        }
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Conferma eliminazione")
                .bodyTitle("Eliminare " + (Elenco.size() == 1 ? "questa dichiarazione" : Elenco.size() + " dichiarazioni") + "?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("")
                .details("Vengono cancellati i PDF, l'Excel e la nota."
                        + "<br><br>L'operazione non è reversibile, salvo ripristinare un backup che le contiene.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina")
                        .role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (r == null || !r.isAction("elimina")) {
            return false;
        }
        if (definitive > 0) {
            AppDialog.DialogResult r2 = AppDialog.builder(owner)
                    .windowTitle("Conferma eliminazione")
                    .bodyTitle(definitive == 1 ? "Stai eliminando una dichiarazione definitiva" : "Stai eliminando " + definitive + " dichiarazioni definitive")
                    .showTitleInBody(true)
                    .theme()
                    .type(AppDialog.DialogType.WARNING)
                    .message("")
                    .details("La definitiva è quella segnata come presentata. Dopo l'eliminazione per quell'anno non ne resterà nessuna.")
                    .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                            .role(AppDialog.ActionRole.SECONDARY).build())
                    .action(AppDialog.DialogAction.builder("elimina", "Elimina comunque")
                            .role(AppDialog.ActionRole.DANGER).build())
                    .showDialog();
            if (r2 == null || !r2.isAction("elimina")) {
                return false;
            }
        }
        int eliminate = 0;
        for (Dichiarazioni.Dichiarazione d : Elenco) {
            if (Dichiarazioni.Elimina(d)) {
                eliminate++;
            }
        }
        return eliminate > 0;
    }

    /**
     * Esporta la dichiarazione come zip (PDF, Excel, manifest) in una cartella scelta dall'utente.
     *
     * @return il file creato, {@code null} se annullato o fallito
     */
    public static File Esporta(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null || D.Cartella == null) {
            return null;
        }
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA_ESPORTA, ""));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Cartella in cui esportare la dichiarazione");
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null) {
            return null;
        }
        File cartella = fc.getSelectedFile();
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA_ESPORTA, cartella.getAbsolutePath());
        File zip = new File(cartella, "Dichiarazione_" + D.Id() + ".zip");
        try {
            ScriviZip(D, zip);
        } catch (IOException e) {
            zip.delete();
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Esportazione", "Esportazione non riuscita.", String.valueOf(e.getMessage()), owner);
            return null;
        }
        Messaggi.SuccessMessage("Esportazione", "Dichiarazione esportata in " + zip.getAbsolutePath(), owner);
        return zip;
    }

    /**
     * Allega un file scelto dall'utente (per esempio la ricevuta di presentazione) alla dichiarazione.
     *
     * @return {@code true} se allegato
     */
    public static boolean AggiungiAllegato(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null) {
            return false;
        }
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_CARTELLA_ALLEGATI, ""));
        fc.setDialogTitle("File da allegare alla dichiarazione " + D.Fam.Etichetta + " " + D.Anno);
        if (fc.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null
                || !fc.getSelectedFile().isFile()) {
            return false;
        }
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_CARTELLA_ALLEGATI, fc.getSelectedFile().getParent());
        try {
            Dichiarazioni.AggiungiAllegato(D, fc.getSelectedFile());
            return true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Allegato", "Il file non è stato allegato.", String.valueOf(e.getMessage()), owner);
            return false;
        }
    }

    /** Toglie un allegato, previa conferma. @return {@code true} se tolto */
    public static boolean RimuoviAllegato(Dichiarazioni.Dichiarazione D, String Nome, Window owner) {
        if (D == null || Nome == null) {
            return false;
        }
        AppDialog.DialogResult r = AppDialog.builder(owner)
                .windowTitle("Allegato")
                .bodyTitle("Togliere l'allegato?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(Nome)
                .details("Il file viene cancellato dalla cartella della dichiarazione.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("togli", "Togli")
                        .role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        if (r == null || !r.isAction("togli")) {
            return false;
        }
        try {
            Dichiarazioni.RimuoviAllegato(D, Nome);
            return true;
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Allegato", "L'allegato non è stato tolto.", String.valueOf(e.getMessage()), owner);
            return false;
        }
    }

    /** Ultima cartella da cui si è allegato un file ({@code personale.mv.db}). */
    static final String OPZIONE_CARTELLA_ALLEGATI = "Dichiarazioni_CartellaAllegati";

    /**
     * Mostra le differenze fra due dichiarazioni della stessa famiglia in una tabella
     * ({@link Dichiarazioni#Confronta}). La più vecchia va nella prima colonna.
     */
    public static void MostraConfronto(Dichiarazioni.Dichiarazione A, Dichiarazioni.Dichiarazione B, Window owner) {
        if (A == null || B == null) {
            return;
        }
        if (A.Fam != B.Fam) {
            Messaggi.InfoMessage("Confronta", "Si confrontano solo dichiarazioni dello stesso quadro.",
                    "Una è " + A.Fam.Etichetta + ", l'altra " + B.Fam.Etichetta + ".", owner);
            return;
        }
        if ((A.Anno + A.Creata).compareTo(B.Anno + B.Creata) > 0) {
            Dichiarazioni.Dichiarazione t = A;
            A = B;
            B = t;
        }
        List<String[]> diff = Dichiarazioni.Confronta(A, B);
        if (diff.isEmpty()) {
            Messaggi.SuccessMessage("Confronta", "Le due dichiarazioni hanno gli stessi valori, opzioni ed errori.", owner);
            return;
        }
        String colA = A.Anno + " del " + A.Creata;
        String colB = B.Anno + " del " + B.Creata;
        javax.swing.table.DefaultTableModel modello = new javax.swing.table.DefaultTableModel(
                new Object[]{"Voce", colA, colB}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (String[] d : diff) {
            modello.addRow(d);
        }
        javax.swing.JTable tabella = new javax.swing.JTable(modello);
        tabella.setAutoCreateRowSorter(true);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(tabella);
        Tabelle.ColoraTabellaSemplice(tabella);
        tabella.getColumnModel().getColumn(0).setPreferredWidth(420);
        tabella.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabella.getColumnModel().getColumn(2).setPreferredWidth(260);
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tabella);
        sp.setPreferredSize(new java.awt.Dimension(960, Math.min(520, 60 + 22 * diff.size())));
        javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        p.add(new javax.swing.JLabel(diff.size() + (diff.size() == 1 ? " differenza" : " differenze")
                + " fra le dichiarazioni " + A.Fam.Etichetta + " " + colA + " e " + colB + "."), java.awt.BorderLayout.NORTH);
        p.add(sp, java.awt.BorderLayout.CENTER);
        javax.swing.JOptionPane.showMessageDialog(owner, p, "Confronto fra dichiarazioni", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    /** Scrive nello zip i file della cartella della dichiarazione (manifest compreso). */
    static void ScriviZip(Dichiarazioni.Dichiarazione D, File Zip) throws IOException {
        File[] file = D.Cartella.listFiles(File::isFile);
        if (file == null) {
            throw new IOException("cartella non leggibile: " + D.Cartella);
        }
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(Zip))) {
            for (File f : file) {
                if (Dichiarazioni.FileDiBlocco(f.getName())) {
                    continue;
                }
                out.putNextEntry(new ZipEntry(D.Id() + "/" + f.getName()));
                out.write(Files.readAllBytes(f.toPath()));
                out.closeEntry();
            }
        }
    }

    /**
     * Su richiesta (pulsante <i>Continuità</i>): confronta la dichiarazione W/RW selezionata con la
     * definitiva W/RW dell'anno prima, alle condizioni di {@link Dichiarazioni#ConfrontaContinuita}.
     * Non è automatico di proposito: con molte opzioni di calcolo i due valori differiscono per costruzione.
     */
    public static void VerificaContinuita(Dichiarazioni.Dichiarazione D, Window owner) {
        if (D == null || D.Fam != Dichiarazioni.Famiglia.RW) {
            return;
        }
        String annoPrec;
        try {
            annoPrec = String.valueOf(Integer.parseInt(D.Anno) - 1);
        } catch (NumberFormatException e) {
            return;
        }
        Dichiarazioni.Dichiarazione prec = Dichiarazioni.Definitiva(Dichiarazioni.Famiglia.RW, annoPrec);
        if (prec == null) {
            Messaggi.InfoMessage("Continuità", "Nessun confronto possibile.",
                    "Non c'è una dichiarazione W/RW definitiva per l'anno " + annoPrec + ".", owner);
            return;
        }
        Dichiarazioni.Continuita c = Dichiarazioni.ConfrontaContinuita(prec, D);
        if (!c.NonVerificabile.isEmpty()) {
            Messaggi.InfoMessage("Continuità", "Confronto non verificabile con queste opzioni.", c.NonVerificabile, owner);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Confrontata la definitiva ").append(annoPrec).append(" del ").append(prec.Creata)
                .append(" con la dichiarazione ").append(D.Anno).append(" del ").append(D.Creata)
                .append(": ").append(c.Confrontati).append(c.Confrontati == 1 ? " rigo." : " righi.");
        if (!c.Differenze.isEmpty()) {
            sb.append("<br><br><b>Valori che non coincidono:</b><br>").append(String.join("<br>", c.Differenze));
        }
        if (!c.NonConfrontati.isEmpty()) {
            sb.append("<br><br><b>Righi non confrontati:</b><br>").append(String.join("<br>", c.NonConfrontati));
        }
        Messaggi.Message("Continuità",
                c.Differenze.isEmpty() ? "I valori finali " + annoPrec + " coincidono con i valori iniziali " + D.Anno + "."
                        : c.Differenze.size() + (c.Differenze.size() == 1 ? " rigo non coincide." : " righi non coincidono."),
                sb.toString(),
                c.Differenze.isEmpty() ? AppDialog.DialogType.SUCCESS : AppDialog.DialogType.WARNING, owner);
    }

    /** Apre un file con l'applicazione predefinita del sistema, avvisando se non è possibile. */
    public static void ApriFile(File f, Window owner) {
        if (owner != null) {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        try {
            if (f == null || !f.exists()) {
                Messaggi.WarningMessage("File mancante", "Il file non esiste più.", f == null ? "" : f.getAbsolutePath(), owner);
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            }
        } catch (IOException | RuntimeException e) {
            LoggerGC.ScriviErrore(e);
            Messaggi.WarningMessage("Apertura file", "Impossibile aprire il file.", f.getAbsolutePath(), owner);
        } finally {
            if (owner != null) {
                owner.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    // =================================================================================================
    // RIGHE PER LE DUE TABELLE DEL PANNELLO
    // =================================================================================================
    /** Intestazioni della tabella superiore: devono corrispondere a {@link #RigheElenco}. */
    static final String[] COLONNE_ELENCO = {
        "Anno", "Quadri", "Definitiva", "Generata il", "Valore finale / Plusvalenze", "Imposta / Corrispettivi",
        "Errori", "File", "Versione", "Nota"
    };

    /**
     * @return una riga per dichiarazione; la colonna 2 (definitiva) è un {@code Boolean}
     */
    public static List<Object[]> RigheElenco(List<Dichiarazioni.Dichiarazione> Elenco) {
        List<Object[]> righe = new ArrayList<>();
        for (Dichiarazioni.Dichiarazione d : Elenco) {
            String v1, v2;
            if (d.Fam == Dichiarazioni.Famiglia.RW) {
                v1 = d.Totali.getOrDefault(TOT_RW_FINALE, "");
                v2 = d.Totali.getOrDefault(TOT_RW_IC, "");
            } else {
                v1 = d.Totali.getOrDefault(TOT_RT_PLUS, "");
                v2 = d.Totali.getOrDefault(TOT_RT_CORRISPETTIVI, "");
            }
            boolean integri = true;
            for (Dichiarazioni.StatoFile s : Dichiarazioni.VerificaIntegrita(d).values()) {
                integri &= s == Dichiarazioni.StatoFile.INTEGRO;
            }
            righe.add(new Object[]{
                d.Anno,
                d.Fam.Etichetta,
                d.Definitiva,
                d.Creata,
                v1,
                v2,
                d.Errori.isBlank() ? "" : "Sì",
                integri ? "integri" : "da verificare",
                d.Versione,
                d.Nota.replace('\n', ' ')
            });
        }
        return righe;
    }

    /** Totali scritti dalla generazione W/RW e letti dall'elenco. */
    public static final String TOT_RW_INIZIALE = "Valore iniziale complessivo (crypto)";
    public static final String TOT_RW_FINALE = "Valore finale complessivo (crypto)";
    public static final String TOT_RW_IC = "IC dovuta complessiva";
    /** Vedi {@link Dichiarazioni#OPZIONE_METODO_RW}. */
    public static final String OPZIONE_METODO_RW = Dichiarazioni.OPZIONE_METODO_RW;
    /** Vedi {@link Dichiarazioni#METODO_RW_SOLO_VALORI_INIZIO_FINE}. */
    public static final String METODO_RW_SOLO_VALORI_INIZIO_FINE = Dichiarazioni.METODO_RW_SOLO_VALORI_INIZIO_FINE;
    /** Totali scritti dalla generazione T/RT e letti dall'elenco. */
    public static final String TOT_RT_COSTI = "Totale dei costi o valori di acquisto";
    public static final String TOT_RT_CORRISPETTIVI = "Totale dei corrispettivi";
    public static final String TOT_RT_PLUS = "Plusvalenze realizzate";

    /**
     * Righe della tabella inferiore ({@code Voce}, {@code Valore}): stato, totali, righi, file con la
     * loro integrità, nota e opzioni di calcolo.
     */
    public static List<Object[]> RigheDettaglio(Dichiarazioni.Dichiarazione D) {
        List<Object[]> righe = new ArrayList<>();
        if (D == null) {
            return righe;
        }
        righe.add(new Object[]{"Dichiarazione", D.Fam.Etichetta + " anno d'imposta " + D.Anno});
        righe.add(new Object[]{"Generata il", D.Creata + " (versione " + D.Versione + ")"});
        righe.add(new Object[]{"Stato", D.Definitiva ? "Definitiva dal " + D.DefinitivaDal : "Non definitiva"});
        righe.add(new Object[]{"Errori al momento della stampa", D.Errori.isBlank() ? "nessuno" : D.Errori});
        if (!D.Nota.isBlank()) {
            righe.add(new Object[]{"Nota", D.Nota.replace('\n', ' ')});
        }
        if (!D.Backup.isBlank()) {
            boolean presente = new File(VarStatiche.getCartella_ArchivioBackup(), D.Backup).isFile();
            righe.add(new Object[]{"Backup dei dati", D.Backup + (presente ? "" : " (non più presente)")});
        }
        for (Map.Entry<String, String> e : D.Totali.entrySet()) {
            righe.add(new Object[]{e.getKey(), e.getValue()});
        }
        int n = 1;
        for (Map<String, String> rigo : D.Righi) {
            StringBuilder sb = new StringBuilder();
            String nome = "";
            for (Map.Entry<String, String> e : rigo.entrySet()) {
                if (nome.isEmpty()) {
                    nome = e.getValue();
                    continue;
                }
                if (e.getValue() == null || e.getValue().isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("   |   ");
                }
                sb.append(e.getKey()).append(": ").append(e.getValue());
            }
            righe.add(new Object[]{"Rigo " + n++ + " - " + nome, sb.toString()});
        }
        for (Map.Entry<String, Dichiarazioni.StatoFile> e : Dichiarazioni.VerificaIntegrita(D).entrySet()) {
            righe.add(new Object[]{"File " + e.getKey(), e.getValue().Descrizione});
        }
        for (Map.Entry<String, String> e : D.Opzioni.entrySet()) {
            righe.add(new Object[]{"Opzione: " + e.getKey(), e.getValue()});
        }
        return righe;
    }
}
