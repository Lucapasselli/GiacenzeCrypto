package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GOLDEN MASTER di {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenze()} sul dataset
 * reale locale (Fase 3 della voce B3 di Documentazione/Analisi_Bug_Criticita.md).
 *
 * Il dataset ("test/Dichiarazione 2025/") contiene dati personali e NON è nel
 * repository (la cartella /test/ è esclusa da .gitignore): su una macchina dove
 * non esiste, l'intera classe viene saltata con una assumption. Anche lo
 * snapshot di riferimento ("nocommit/GoldenMaster/") resta locale e fuori da git.
 *
 * Funzionamento:
 *  - alla prima esecuzione il test calcola le plusvalenze su tutti i movimenti
 *    reali e scrive lo snapshot di riferimento (baseline), terminando come "skipped";
 *  - alle esecuzioni successive ricalcola tutto e confronta riga per riga con la
 *    baseline: qualunque differenza nei campi v[16]/v[17]/v[19]/v[33]/v[38] fa
 *    fallire il test.
 *
 * Se una differenza è ATTESA (nuovi movimenti importati nel dataset, oppure la
 * correzione di un bug che cambia deliberatamente i risultati), eliminare il file
 * baseline e rilanciare i test per rigenerarlo, verificando prima che le
 * differenze siano tutte spiegabili.
 *
 * Il database personale reale viene COPIATO in una directory temporanea: i file
 * originali del dataset non vengono mai aperti né modificati. I database principale
 * e prezzi vengono creati vuoti nella stessa directory temporanea (il motore delle
 * plusvalenze non li legge).
 */
class CalcoliPlusvalenzeNewGoldenMasterTest {

    private static final Path DATASET = Path.of("test", "Dichiarazione 2025");
    private static final Path FILE_MOVIMENTI = DATASET.resolve("movimenti.crypto.db");
    private static final Path FILE_PERSONALE = DATASET.resolve("personale.mv.db");
    private static final Path FILE_BASELINE = Path.of("nocommit", "GoldenMaster", "plusvalenze.golden");

    @TempDir
    static Path tempDir;

    private static boolean databaseAperto = false;

    @BeforeAll
    static void preparaAmbiente() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(FILE_MOVIMENTI) && Files.isRegularFile(FILE_PERSONALE),
                "Dataset reale non presente in \"" + DATASET + "\": golden master saltato");

        // Copio il DB personale reale (opzioni, gruppi wallet, tabella EMONEY) nella
        // directory temporanea: l'originale non viene mai aperto né lockato.
        Files.copy(FILE_PERSONALE, tempDir.resolve("personale.mv.db"), StandardCopyOption.REPLACE_EXISTING);

        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        databaseAperto = DatabaseH2.CreaoCollegaDatabase();
        Assumptions.assumeTrue(databaseAperto,
                "Impossibile aprire la copia del database personale (app in esecuzione durante la copia?)");

        // Come fa l'app all'avvio: i token EMoney cambiano la tipologia fiscale degli scambi
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();

        caricaMovimentiReali();
    }

    @AfterAll
    static void ripulisce() throws Exception {
        if (databaseAperto) {
            DatabaseH2.connection.close();
            DatabaseH2.connectionPersonale.close();
            DatabaseH2.connectionPrezzi.close();
        }
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
    }

    /**
     * Replica del caricamento di Principale.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaFile:
     * split su ";" con padding a ColonneTabella colonne. Le migrazioni di versione e
     * Prezzi.isMovimentoPrezzato (col. 32) sono volutamente omesse: la colonna 32 non
     * viene letta dal motore delle plusvalenze.
     */
    private static void caricaMovimentiReali() throws IOException {
        Principale.MappaCryptoWallet.clear();
        for (String riga : Files.readAllLines(FILE_MOVIMENTI, StandardCharsets.UTF_8)) {
            if (riga.isBlank()) continue;
            String[] splittata = riga.split(";", -1);
            if (splittata.length < Importazioni.ColonneTabella) {
                splittata = java.util.Arrays.copyOf(splittata, Importazioni.ColonneTabella);
            }
            Importazioni.RiempiVuotiArray(splittata);
            Principale.MappaCryptoWallet.put(splittata[0], splittata);
        }
        assertFalse(Principale.MappaCryptoWallet.isEmpty(), "Nessun movimento caricato dal dataset reale");
    }

    @Test
    void ricalcoloCompleto_corrispondeAllaBaseline() throws IOException {
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        List<String> attuale = generaSnapshot();

        if (!Files.isRegularFile(FILE_BASELINE)) {
            Files.createDirectories(FILE_BASELINE.getParent());
            Files.write(FILE_BASELINE, attuale, StandardCharsets.UTF_8);
            Assumptions.abort("Baseline creata ora in \"" + FILE_BASELINE + "\" ("
                    + attuale.size() + " righe): rilanciare i test per usarla come riferimento");
        }

        List<String> baseline = Files.readAllLines(FILE_BASELINE, StandardCharsets.UTF_8);
        List<String> differenze = confronta(baseline, attuale);

        assertTrue(differenze.isEmpty(), () ->
                "Il ricalcolo differisce dalla baseline in " + differenze.size() + " punti.\n"
                + "Se la differenza è attesa (nuovi movimenti nel dataset o correzione di un bug),\n"
                + "verificare le differenze, eliminare \"" + FILE_BASELINE + "\" e rilanciare i test.\n\n"
                + String.join("\n", differenze.subList(0, Math.min(20, differenze.size()))));
    }

    /**
     * Equivalenza fra ricalcolo incrementale e completo SUL DATASET REALE.
     *
     * Il golden master qui sopra confronta una passata completa con una baseline, quindi non dice
     * nulla sul percorso incrementale; CalcoliPlusvalenzeNewEquivalenzaIncrementaleTest lo verifica
     * ma su sette movimenti costruiti a mano. Questo test chiude il buco: modifica un movimento nel
     * mezzo dello storico vero, lascia lavorare l'incrementale e confronta con il ricalcolo
     * completo degli stessi dati.
     *
     * I movimenti vengono ricaricati da file all'inizio e alla fine, così il test non dipende
     * dall'ordine di esecuzione e non lascia dati modificati all'altro test della classe.
     */
    @Test
    void ricalcoloIncrementale_coincideConQuelloCompleto_suiDatiReali() throws IOException {
        caricaMovimentiReali();
        try {
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            long avvio = System.currentTimeMillis();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            long msCompleta = System.currentTimeMillis() - avvio;
            assertTrue(Calcoli_PlusvalenzeNew.UltimaPassata_Completa, "La prima passata deve essere completa");

            // Un movimento a circa metà storico: cambio il controvalore in EUR (campo 15),
            // che il motore legge per plusvalenza e costo di carico.
            List<String> chiavi = new ArrayList<>(Principale.MappaCryptoWallet.keySet());
            String chiaveModificata = chiavi.get(chiavi.size() / 2);
            String[] modificato = Principale.MappaCryptoWallet.get(chiaveModificata);
            modificato[15] = new BigDecimal(modificato[15].isBlank() ? "0" : modificato[15])
                    .add(new BigDecimal("13.57")).toPlainString();

            avvio = System.currentTimeMillis();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            long msIncrementale = System.currentTimeMillis() - avvio;
            int rielaborati = Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati;
            assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa,
                    "La modifica di un solo movimento non doveva far ripartire tutto");
            assertTrue(rielaborati < Principale.MappaCryptoWallet.size(),
                    "La ripartenza doveva usare un checkpoint intermedio");
            List<String> incrementale = generaSnapshotConCampo31();

            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            List<String> completa = generaSnapshotConCampo31();

            System.out.println("Equivalenza su dati reali: " + Principale.MappaCryptoWallet.size()
                    + " movimenti, completa " + msCompleta + " ms, incrementale " + msIncrementale
                    + " ms (" + rielaborati + " rielaborati, modifica a meta' storico)");

            List<String> differenze = confronta(completa, incrementale);
            assertTrue(differenze.isEmpty(), () ->
                    "Incrementale e completo divergono in " + differenze.size() + " punti sui dati reali:\n"
                    + String.join("\n", differenze.subList(0, Math.min(10, differenze.size()))));

            // Il caso DOMINANTE nell'uso reale non è la modifica di un vecchio movimento ma
            // l'import, che aggiunge in coda. Lì la rielaborazione tocca pochissimi movimenti,
            // ed è la differenza che l'utente percepisce davvero.
            String[] ultimo = Principale.MappaCryptoWallet.lastEntry().getValue();
            String[] nuovo = ultimo.clone();
            nuovo[0] = "20991231235900_TestIncrementale_1_1_" + ultimo[0].substring(ultimo[0].lastIndexOf('_') + 1);
            nuovo[1] = "2099-12-31 23:59";
            nuovo[20] = "";
            Principale.MappaCryptoWallet.put(nuovo[0], nuovo);

            avvio = System.currentTimeMillis();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            long msCoda = System.currentTimeMillis() - avvio;
            int rielaboratiCoda = Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati;
            List<String> incrementaleCoda = generaSnapshotConCampo31();

            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            List<String> completaCoda = generaSnapshotConCampo31();

            System.out.println("Aggiunta in coda (caso import): " + msCoda + " ms, "
                    + rielaboratiCoda + " movimenti rielaborati");

            List<String> differenzeCoda = confronta(completaCoda, incrementaleCoda);
            assertTrue(differenzeCoda.isEmpty(), () ->
                    "Incrementale e completo divergono dopo un'aggiunta in coda in "
                    + differenzeCoda.size() + " punti");
        } finally {
            caricaMovimentiReali();
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        }
    }

    /**
     * Una riga per movimento (l'ordine della TreeMap è deterministico) con i campi
     * scritti dal motore, precedute dai totali di plusvalenza per anno come
     * controllo di sintesi immediatamente leggibile.
     */
    private static List<String> generaSnapshot() {
        Map<String, BigDecimal> plusPerAnno = new TreeMap<>();
        List<String> righe = new ArrayList<>();

        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if ("S".equals(v[33]) && !v[19].isBlank()) {
                String anno = v[1].length() >= 4 ? v[1].substring(0, 4) : "????";
                plusPerAnno.merge(anno, new BigDecimal(v[19]), BigDecimal::add);
            }
        }

        righe.add("# Golden master AggiornaPlusvalenze - movimenti: " + Principale.MappaCryptoWallet.size());
        plusPerAnno.forEach((anno, tot) ->
                righe.add("# plusvalenza " + anno + " = " + tot.toPlainString()));
        righe.add("# ID;vecchioCarico(16);nuovoCarico(17);plusvalenza(19);flagCalcolo(33);anomalia(38)");

        for (String[] v : Principale.MappaCryptoWallet.values()) {
            righe.add(v[0] + ";" + v[16] + ";" + v[17] + ";" + v[19] + ";" + v[33] + ";" + v[38]);
        }
        return righe;
    }

    /**
     * Come generaSnapshot() ma con anche il campo 31, usato solo dal confronto
     * incrementale/completo (la baseline del golden master resta nel formato originale).
     *
     * Il campo 31 è di sola visualizzazione, ma è l'unico che il motore scrive DENTRO UN ALTRO
     * movimento (il DTW timbra la data sulla propria controparte): è quindi l'unico che una
     * passata incrementale può toccare PRIMA del proprio punto di ripartenza, ed è l'unico posto
     * dove incrementale e completo potrebbero divergere senza che gli altri campi lo mostrino.
     */
    private static List<String> generaSnapshotConCampo31() {
        List<String> righe = new ArrayList<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            righe.add(v[0] + ";" + v[16] + ";" + v[17] + ";" + v[19] + ";" + v[31] + ";" + v[33] + ";" + v[38]);
        }
        return righe;
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
}
