package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GOLDEN MASTER di {@link Calcoli_RW#AggiornaRWFR(String)} e di
 * {@link Funzioni#RW_GiacenzeInizioFineAnno(String)} sul dataset reale locale.
 *
 * <p>SCOPO. Fotografa il comportamento ATTUALE del motore del quadro W/RW prima di
 * qualunque intervento legato alla gestione dei periodi di detenzione (righi
 * multipli per apertura/chiusura conto, valori campo 7/8 inseriti a mano, stato
 * estero / exchange di riferimento per gruppo wallet). Non giudica se i risultati
 * di oggi siano "giusti": li congela, così che una modifica successiva che li
 * cambi SENZA VOLERLO faccia fallire il test. È lo stesso schema di
 * {@link CalcoliPlusvalenzeNewGoldenMasterTest}, applicato all'RW.</p>
 *
 * <p>CASISTICHE DI CALCOLO. Il calcolo non viene fotografato una volta sola con
 * le opzioni salvate nel dataset, ma rigirato per ognuna delle {@link #COMBINAZIONI}
 * (DEFAULT, le 4 rilevanze A/B/C/D, e ogni flag booleano acceso singolarmente):
 * ogni combinazione è una sezione a sé della baseline. Così una regressione in un
 * ramo di {@code AggiornaRWFR} diverso da quello attualmente configurato non passa
 * inosservata. I limiti di questa copertura sono in {@link #RW_OPZIONI}.</p>
 *
 * <p>DATASET. Serve la cartella privata {@code test/Dichiarazione 2025/} con
 * {@code movimenti.crypto.db}, {@code personale.mv.db} e {@code prezzi.mv.db}: non
 * è nel repository (la cartella {@code /test/} è in .gitignore). Su una macchina
 * dove manca, l'intera classe viene SALTATA con una assumption — un "skipped" su
 * un clone pulito è il comportamento atteso, non un errore. Anche la baseline
 * ({@code nocommit/GoldenMaster/rw.golden}) resta locale e fuori da git.</p>
 *
 * <p>PERCHÉ COPIA ANCHE {@code prezzi.mv.db}. A differenza del motore delle
 * plusvalenze, {@code AggiornaRWFR} valorizza le giacenze di inizio/fine anno
 * chiamando {@link Prezzi#DammiPrezzoTransazione}. Senza la cache prezzi reale
 * quelle valorizzazioni fallirebbero e la baseline sarebbe una fotografia di soli
 * errori, inutile come rete di sicurezza. La copia (diverse centinaia di MB) va
 * su {@code nocommit/} — stesso filesystem del progetto, non la tmpfs di
 * {@code /tmp} — e viene rimossa in {@code @AfterAll}. È un costo pagato solo da
 * chi ha il dataset, cioè in pratica solo sulla macchina di sviluppo.</p>
 *
 * <p>DETERMINISMO. Il confronto ha senso solo se due esecuzioni sullo stesso
 * dataset producono lo stesso output. Le condizioni:
 * <ul>
 *   <li>i database originali NON vengono mai aperti (si lavora su copie in
 *       {@code nocommit/}), quindi il lock single-instance dell'app non interferisce;</li>
 *   <li>il test forza lo stato "offline" di {@link Funzioni#CeConnessioneInternet()}
 *       prima di iniziare, così le valorizzazioni mancanti in cache non partono in
 *       rete a prendere un prezzo diverso a ogni run. La finestra di cache di quel
 *       metodo è di 60 s: per un batch lungo conviene comunque lanciare il test a
 *       connessione staccata quando si (ri)genera la baseline.</li>
 * </ul>
 * Se una differenza rispetto alla baseline è ATTESA (nuovi movimenti nel dataset,
 * nuove opzioni RW, correzione voluta di un calcolo), verificare che ogni
 * differenza sia spiegabile, eliminare {@code nocommit/GoldenMaster/rw.golden} e
 * rilanciare per rigenerarla.</p>
 */
class Calcoli_RW_GoldenMasterTest {

    private static final Path DATASET = Path.of("test", "Dichiarazione 2025");
    private static final Path FILE_MOVIMENTI = DATASET.resolve("movimenti.crypto.db");
    private static final Path FILE_PERSONALE = DATASET.resolve("personale.mv.db");
    private static final Path FILE_PREZZI = DATASET.resolve("prezzi.mv.db");
    private static final Path FILE_CAMBIO = DATASET.resolve("cambioUSDEUR.db");

    private static final Path FILE_BASELINE = Path.of("nocommit", "GoldenMaster", "rw.golden");

    /**
     * Ultimo anno d'imposta verificato. L'anno iniziale è dedotto dal primo
     * movimento. Restringere questo intervallo (o {@link #annoMinimo}) è il modo
     * per accorciare il test: ogni anno è una passata LIFO completa sull'intero
     * archivio.
     */
    private static final int ANNO_MASSIMO = 2025;

    /**
     * Opzioni della sezione RW lette da {@link Calcoli_RW#AggiornaRWFR(String)}
     * (via {@code DatabaseH2.Pers_Opzioni_Leggi}). Il golden master rigira il
     * calcolo per ognuna delle {@link #COMBINAZIONI} qui sotto, così una
     * regressione che tocchi un ramo diverso da quello attualmente configurato
     * dall'utente viene comunque intercettata.
     *
     * <p>NON coperte: le opzioni lette solo dall'aggregazione lato GUI
     * ({@code Principale.RW_CalcolaRW}: {@code RW_InizioSuWOriginale},
     * {@code RW_1RigoXOperazione}, e il ramo "mostra solo giacenze" di
     * {@code RW_MostraGiacenzeSePagaBollo}), perché quella logica vive dentro un
     * metodo Swing e non è invocabile qui. Vanno testate a parte quando/se
     * verrà estratta in una classe companion.</p>
     */
    private static final String[] RW_OPZIONI = {
        "RW_Rilevanza", "RW_ChiudiRWsuTrasferimento", "RW_StakingZero",
        "RW_LiFoComplessivo", "RW_LiFoSubMovimenti", "RW_MostraGiacenzeSePagaBollo"
    };

    /**
     * Matrice di combinazioni: {@code {etichetta, valori nell'ordine di RW_OPZIONI...}}.
     * DEFAULT + le 4 rilevanze (A/B/C/D) + ogni booleano acceso singolarmente
     * (one-factor-at-a-time). Restringere questo array è l'altro modo per
     * accorciare il test (vedi {@link #ANNO_MASSIMO}).
     */
    private static final String[][] COMBINAZIONI = {
        {"DEFAULT",           "D", "NO", "NO", "NO", "NO", "NO"},
        {"RILEVANZA_A",       "A", "NO", "NO", "NO", "NO", "NO"},
        {"RILEVANZA_B",       "B", "NO", "NO", "NO", "NO", "NO"},
        {"RILEVANZA_C",       "C", "NO", "NO", "NO", "NO", "NO"},
        {"CHIUDI_SU_TRASF",   "D", "SI", "NO", "NO", "NO", "NO"},
        {"STAKING_ZERO",      "D", "NO", "SI", "NO", "NO", "NO"},
        {"LIFO_COMPLESSIVO",  "D", "NO", "NO", "SI", "NO", "NO"},
        {"LIFO_SUBMOVIMENTI", "D", "NO", "NO", "NO", "SI", "NO"},
        {"BOLLO_MOSTRA_GIAC", "D", "NO", "NO", "NO", "NO", "SI"},
    };

    private static Path workDir;
    private static boolean databaseAperto = false;
    private static int annoMinimo;

    // ----------------------------------------------------------------- setup

    @BeforeAll
    static void preparaAmbiente() throws IOException {
        Assumptions.assumeTrue(
                Files.isRegularFile(FILE_MOVIMENTI) && Files.isRegularFile(FILE_PERSONALE) && Files.isRegularFile(FILE_PREZZI),
                "Dataset reale non presente in \"" + DATASET + "\" (servono movimenti.crypto.db, personale.mv.db, prezzi.mv.db): golden master RW saltato");

        // Directory di lavoro su nocommit/ (gitignored, stesso disco del progetto): NON su
        // @TempDir, perché /tmp qui è tmpfs e prezzi.mv.db pesa centinaia di MB.
        Files.createDirectories(Path.of("nocommit"));
        workDir = Files.createTempDirectory(Path.of("nocommit"), "rw-goldenmaster-").toAbsolutePath();

        long t0 = System.currentTimeMillis();
        Files.copy(FILE_PERSONALE, workDir.resolve("personale.mv.db"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(FILE_PREZZI, workDir.resolve("prezzi.mv.db"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.isRegularFile(FILE_CAMBIO)) {
            Files.copy(FILE_CAMBIO, workDir.resolve("cambioUSDEUR.db"), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[RW-GM] copia database in " + workDir + " (" + (System.currentTimeMillis() - t0) + " ms)");

        VarStatiche.setWorkingDirectory(workDir.toString() + "/");
        databaseAperto = DatabaseH2.CreaoCollegaDatabase();
        Assumptions.assumeTrue(databaseAperto,
                "Impossibile aprire la copia dei database (app in esecuzione durante la copia?)");

        // Come fa l'app all'avvio: i token EMoney cambiano la tipologia fiscale degli scambi,
        // e AggiornaRWFR li legge tramite Funzioni.RitornaTipoCrypto / Calcoli_RW.RitornaTipoCrypto.
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();

        // Forzo "nessuna connessione" per le valorizzazioni non in cache: senza questo, un prezzo
        // mancante partirebbe in rete e la baseline non sarebbe riproducibile.
        Funzioni.ConnInternetAttiva = false;
        Funzioni.TimesTampUltimoControlloInternet = System.currentTimeMillis();

        caricaMovimentiReali();

        String primoID = Principale.MappaCryptoWallet.firstKey();
        annoMinimo = Integer.parseInt(primoID.substring(0, 4));
        assertTrue(annoMinimo >= 2009 && annoMinimo <= ANNO_MASSIMO,
                "Anno del primo movimento fuori range: " + annoMinimo);
    }

    @AfterAll
    static void ripulisce() throws Exception {
        if (databaseAperto) {
            try { DatabaseH2.connection.close(); } catch (Exception ignored) {}
            try { DatabaseH2.connectionPersonale.close(); } catch (Exception ignored) {}
            try { DatabaseH2.connectionPrezzi.close(); } catch (Exception ignored) {}
        }
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_RW_ListeXGruppoWallet.clear();
        Principale.Mappa_RW_GiacenzeInizioPeriodo.clear();
        Principale.Mappa_RW_GiacenzeFinePeriodo.clear();
        if (workDir != null) cancellaRicorsivo(workDir);
    }

    /**
     * Replica del caricamento di
     * {@code Principale.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile}: split su ";"
     * con padding a {@link Importazioni#ColonneTabella} colonne. Identico a quello del golden
     * master delle plusvalenze.
     */
    private static void caricaMovimentiReali() throws IOException {
        Principale.MappaCryptoWallet.clear();
        for (String riga : Files.readAllLines(FILE_MOVIMENTI, StandardCharsets.UTF_8)) {
            if (riga.isBlank()) continue;
            String[] splittata = riga.split(";", -1);
            if (splittata.length < Importazioni.ColonneTabella) {
                splittata = Arrays.copyOf(splittata, Importazioni.ColonneTabella);
            }
            Importazioni.RiempiVuotiArray(splittata);
            Principale.MappaCryptoWallet.put(splittata[0], splittata);
        }
        assertFalse(Principale.MappaCryptoWallet.isEmpty(), "Nessun movimento caricato dal dataset reale");
    }

    // ----------------------------------------------------------------- test

    @Test
    void ricalcoloRW_corrispondeAllaBaseline() throws IOException {
        long t0 = System.currentTimeMillis();
        List<String> attuale = generaSnapshotCompleto();
        System.out.println("[RW-GM] snapshot di " + COMBINAZIONI.length + " combinazioni x "
                + (ANNO_MASSIMO - annoMinimo + 1) + " anni in "
                + (System.currentTimeMillis() - t0) + " ms, " + attuale.size() + " righe");

        if (!Files.isRegularFile(FILE_BASELINE)) {
            Files.createDirectories(FILE_BASELINE.getParent());
            Files.write(FILE_BASELINE, attuale, StandardCharsets.UTF_8);
            Assumptions.abort("Baseline creata ora in \"" + FILE_BASELINE + "\" ("
                    + attuale.size() + " righe): rilanciare i test per usarla come riferimento");
        }

        List<String> baseline = Files.readAllLines(FILE_BASELINE, StandardCharsets.UTF_8);
        List<String> differenze = confronta(baseline, attuale);

        assertTrue(differenze.isEmpty(), () ->
                "Il ricalcolo RW differisce dalla baseline in " + differenze.size() + " punti.\n"
                + "Se la differenza è attesa (nuovi movimenti nel dataset, nuove opzioni RW,\n"
                + "correzione voluta di un calcolo), verificarla, eliminare \"" + FILE_BASELINE + "\"\n"
                + "e rilanciare i test per rigenerarla.\n\n"
                + String.join("\n", differenze.subList(0, Math.min(30, differenze.size()))));
    }

    /**
     * {@code AggiornaRWFR} riparte da mappe che azzera in testa: due esecuzioni consecutive
     * sullo stesso anno devono dare lo stesso identico output. È la rete contro le regressioni
     * di stato residuo, quelle che la nuova logica a periodi multipli rischia di introdurre.
     */
    @Test
    void dueEsecuzioniConsecutive_stessoAnno_stessoRisultato() throws IOException {
        int anno = Math.min(ANNO_MASSIMO, Math.max(annoMinimo, ANNO_MASSIMO - 1));
        caricaMovimentiReali();
        impostaCombinazione(COMBINAZIONI[0]); // DEFAULT

        Calcoli_RW.AggiornaRWFR(String.valueOf(anno));
        List<String> primo = snapshotAnno(anno);

        Calcoli_RW.AggiornaRWFR(String.valueOf(anno));
        List<String> secondo = snapshotAnno(anno);

        List<String> differenze = confronta(primo, secondo);
        assertTrue(differenze.isEmpty(), () ->
                "AggiornaRWFR(" + anno + ") non è deterministico: due run consecutive divergono in "
                + differenze.size() + " punti:\n"
                + String.join("\n", differenze.subList(0, Math.min(15, differenze.size()))));
    }

    // ------------------------------------------------------------- snapshot

    private static List<String> generaSnapshotCompleto() throws IOException {
        List<String> righe = new ArrayList<>();
        righe.add("# Golden master Calcoli_RW.AggiornaRWFR + Funzioni.RW_GiacenzeInizioFineAnno");
        righe.add("# movimenti: " + Principale.MappaCryptoWallet.size());
        righe.add("# anni verificati: " + annoMinimo + ".." + ANNO_MASSIMO);
        righe.add("# combinazioni opzioni RW: " + COMBINAZIONI.length + " (chiavi: " + String.join(",", RW_OPZIONI) + ")");
        righe.add("# formato righe RW: [RW] <gruppo> #<i> | " + intestazioneXlista());

        for (String[] combo : COMBINAZIONI) {
            // Ricarico i movimenti a ogni combinazione: AggiornaRWFR oggi non tocca le righe
            // di MappaCryptoWallet, ma così l'isolamento non dipende da quel dettaglio.
            caricaMovimentiReali();
            impostaCombinazione(combo);

            righe.add("");
            righe.add(">>>>> COMBINAZIONE " + combo[0] + " | " + descriviCombinazione(combo));
            for (int anno = annoMinimo; anno <= ANNO_MASSIMO; anno++) {
                righe.addAll(snapshotAnno(anno));
            }
        }
        return righe;
    }

    /** Scrive nel DB personale (copia usa e getta) i valori delle opzioni RW per la combinazione. */
    private static void impostaCombinazione(String[] combo) {
        for (int i = 0; i < RW_OPZIONI.length; i++) {
            DatabaseH2.Pers_Opzioni_Scrivi(RW_OPZIONI[i], combo[i + 1]);
        }
    }

    private static String descriviCombinazione(String[] combo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < RW_OPZIONI.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(RW_OPZIONI[i]).append('=').append(combo[i + 1]);
        }
        return sb.toString();
    }

    /** Un anno: righe RW per gruppo + totali di sintesi + giacenze inizio/fine periodo + giacenze inizio/fine anno. */
    private static List<String> snapshotAnno(int anno) {
        Calcoli_RW.AggiornaRWFR(String.valueOf(anno));
        Map<String, List<String[]>> inizioFine = Funzioni.RW_GiacenzeInizioFineAnno(String.valueOf(anno));

        List<String> righe = new ArrayList<>();
        righe.add("=== ANNO " + anno + " ===");

        // 1) righe di dettaglio RW (xlista, 17 colonne) + rollup puramente aggregativo
        for (String gruppo : new TreeSet<>(Principale.Mappa_RW_ListeXGruppoWallet.keySet())) {
            List<String[]> lista = Principale.Mappa_RW_ListeXGruppoWallet.get(gruppo);
            BigDecimal sPrezzoIni = BigDecimal.ZERO, sPrezzoFin = BigDecimal.ZERO;
            BigDecimal sGiorni = BigDecimal.ZERO, sGiorniXPrezzoFin = BigDecimal.ZERO;
            for (int i = 0; i < lista.size(); i++) {
                String[] r = lista.get(i);
                righe.add("[RW] " + gruppo + " #" + i + " | " + String.join(" | ", normalizza(r)));
                BigDecimal pIni = bd(campo(r, 5));
                BigDecimal pFin = bd(campo(r, 10));
                BigDecimal gg = bd(campo(r, 11));
                sPrezzoIni = sPrezzoIni.add(pIni);
                sPrezzoFin = sPrezzoFin.add(pFin);
                sGiorni = sGiorni.add(gg);
                sGiorniXPrezzoFin = sGiorniXPrezzoFin.add(gg.multiply(pFin));
            }
            righe.add("[RW-TOT] " + gruppo
                    + " | righe=" + lista.size()
                    + " | sumPrezzoIniz=" + norm(sPrezzoIni)
                    + " | sumPrezzoFin=" + norm(sPrezzoFin)
                    + " | sumGiorni=" + norm(sGiorni)
                    + " | sumGiorniXPrezzoFin=" + norm(sGiorniXPrezzoFin));
        }

        // 2) giacenze di inizio e fine periodo (List<Moneta>), ordinate per stabilità
        righe.addAll(snapshotGiacenze("GIAC-INI", Principale.Mappa_RW_GiacenzeInizioPeriodo));
        righe.addAll(snapshotGiacenze("GIAC-FIN", Principale.Mappa_RW_GiacenzeFinePeriodo));

        // 3) giacenze inizio/fine anno calcolate da Funzioni.RW_GiacenzeInizioFineAnno
        for (String gruppo : new TreeSet<>(inizioFine.keySet())) {
            List<String[]> lista = new ArrayList<>(inizioFine.get(gruppo));
            lista.sort(Comparator.comparing(a -> String.join("\u0001", normalizza(a))));
            for (int i = 0; i < lista.size(); i++) {
                righe.add("[INIFINE] " + gruppo + " #" + i + " | " + String.join(" | ", normalizza(lista.get(i))));
            }
        }
        return righe;
    }

    private static List<String> snapshotGiacenze(String tag, Map<String, List<Moneta>> mappa) {
        List<String> righe = new ArrayList<>();
        for (String gruppo : new TreeSet<>(mappa.keySet())) {
            List<Moneta> monete = new ArrayList<>(mappa.get(gruppo));
            monete.sort(Comparator
                    .comparing((Moneta m) -> String.valueOf(m.Moneta))
                    .thenComparing(m -> String.valueOf(m.Tipo))
                    .thenComparing(m -> String.valueOf(m.Qta)));
            StringBuilder sb = new StringBuilder("[" + tag + "] " + gruppo + " |");
            for (Moneta m : monete) {
                sb.append(' ').append(String.valueOf(m.Moneta))
                  .append(';').append(String.valueOf(m.Tipo))
                  .append(';').append(String.valueOf(m.Qta))
                  .append(';').append(String.valueOf(m.Prezzo))
                  .append(" ||");
            }
            righe.add(sb.toString());
        }
        return righe;
    }

    // ------------------------------------------------------------- helper

    private static String intestazioneXlista() {
        return "annoRW | grWalletIni | monetaIni | qtaIni | dataIni | prezzoIni | grWalletFin | monetaFin | "
             + "qtaFin | dataFin | prezzoFin | giorni | causale | idApertura | idChiusura | tipoErrore | idCoinvolti";
    }

    /** Rende ogni cella non nulla e senza newline/tab, così una riga di snapshot resta una riga sola. */
    private static String[] normalizza(String[] r) {
        String[] out = new String[r == null ? 0 : r.length];
        for (int i = 0; i < out.length; i++) {
            String c = r[i] == null ? "<null>" : r[i];
            out[i] = c.replace("\r", "\\r").replace("\n", "\\n").replace("\t", " ");
        }
        return out;
    }

    private static String campo(String[] r, int i) {
        return (r != null && i < r.length && r[i] != null) ? r[i] : "";
    }

    /** BigDecimal tollerante: 0 su null/blank/non numerico (le righe di errore hanno prezzi non parsabili). */
    private static BigDecimal bd(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String norm(BigDecimal v) {
        return v.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static List<String> confronta(List<String> baseline, List<String> attuale) {
        List<String> differenze = new ArrayList<>();
        if (baseline.size() != attuale.size()) {
            differenze.add("Numero righe: baseline=" + baseline.size() + " attuale=" + attuale.size());
        }
        int max = Math.min(baseline.size(), attuale.size());
        for (int i = 0; i < max; i++) {
            if (!baseline.get(i).equals(attuale.get(i))) {
                differenze.add("Riga " + (i + 1) + ":\n  baseline: " + baseline.get(i)
                        + "\n  attuale : " + attuale.get(i));
            }
        }
        return differenze;
    }

    private static void cancellaRicorsivo(Path radice) throws IOException {
        if (!Files.exists(radice)) return;
        try (var stream = Files.walk(radice)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}
