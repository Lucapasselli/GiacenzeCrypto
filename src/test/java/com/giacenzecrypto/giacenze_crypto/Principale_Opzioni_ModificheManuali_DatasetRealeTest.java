package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Anteprima;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Contenuto;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Esito;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Esportazione e anteprima del ripristino sul <b>dataset reale locale</b> ({@code test/Dichiarazione 2025/}, fuori da
 * git: senza, la classe si salta). Lavora su una copia in cartella temporanea e stampa solo conteggi.
 *
 * <p>Non si può reimportare da zero (i file sorgente non sono qui), quindi l'archivio "pulito" è simulato: si parte
 * dall'archivio vero, si annullano le classificazioni con la funzione vera ({@code RiportaTransazioniASituazioneIniziale}),
 * si rimettono le righe iniziali al posto delle finali e si tolgono i movimenti inseriti a mano. Prova l'abbinamento su
 * ID, quantità e chiavi reali, non l'importazione.</p>
 */
class Principale_Opzioni_ModificheManuali_DatasetRealeTest {

    private static final Path DATASET = Path.of("test", "Dichiarazione 2025");
    private static final Path FILE_MOVIMENTI = DATASET.resolve("movimenti.crypto.db");
    private static final Path FILE_PERSONALE = DATASET.resolve("personale.mv.db");

    @TempDir
    static Path tempDir;

    private static boolean databaseAperto = false;

    @BeforeAll
    static void preparaAmbiente() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(FILE_MOVIMENTI) && Files.isRegularFile(FILE_PERSONALE),
                "Dataset reale non presente in \"" + DATASET + "\": prova saltata");
        Files.copy(FILE_PERSONALE, tempDir.resolve("personale.mv.db"), StandardCopyOption.REPLACE_EXISTING);
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        databaseAperto = DatabaseH2.CreaoCollegaDatabase();
        Assumptions.assumeTrue(databaseAperto, "Impossibile aprire la copia del database personale");
        DatabaseH2.Pers_Emoney_PopolaMappaEmoney();
        for (String riga : Files.readAllLines(FILE_MOVIMENTI, StandardCharsets.UTF_8)) {
            if (riga.isBlank()) {
                continue;
            }
            String[] s = riga.split(";", -1);
            if (s.length < Importazioni.ColonneTabella) {
                s = Arrays.copyOf(s, Importazioni.ColonneTabella);
            }
            Importazioni.RiempiVuotiArray(s);
            ORIGINALI.add(s);
        }
        assertFalse(ORIGINALI.isEmpty());
    }

    /** Le righe com'erano nel file: ogni prova riparte da una copia, perche' alcune modificano la mappa. */
    private static final List<String[]> ORIGINALI = new ArrayList<>();

    private static void ricarica() {
        Principale.MappaCryptoWallet.clear();
        for (String[] r : ORIGINALI) {
            Principale.MappaCryptoWallet.put(r[0], r.clone());
        }
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

    private static String[] campi(String[] record) {
        return Arrays.copyOfRange(record, COLONNE_FISSE, COLONNE_FISSE + Importazioni.ColonneTabella);
    }

    private static void stampa(String titolo, Anteprima a) {
        System.out.println("=== " + titolo + "\n" + a.riepilogo());
        List<String> problemi = a.problemi();
        for (int i = 0; i < Math.min(15, problemi.size()); i++) {
            System.out.println("  ! " + problemi.get(i));
        }
        if (problemi.size() > 15) {
            System.out.println("  ... e altri " + (problemi.size() - 15));
        }
    }

    @Test
    void giroCompletoSuArchivioRealeSimulandoIlReimport() throws Exception {
        ricarica();
        Map<String, String[]> archivio = Principale.MappaCryptoWallet;
        int totale = archivio.size();
        Esportazione e = Costruisci(archivio, MovimentiStorico.TutteLeVersioni());
        Contenuto c = Principale_Opzioni_ModificheManuali_Ripristino.Leggi(Testo(e));
        System.out.println("Movimenti " + totale + ", con storico " + e.movimentiConStorico() + ", senza storico "
                + e.senzaStorico() + ", classificati " + e.classificati() + ", record " + c.record().size());

        //1. Sull'archivio stesso tutto deve risultare gia' applicato: niente conflitti ne' mancanti
        Anteprima identico = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, archivio);
        stampa("Archivio identico al file", identico);
        assertEquals(0, identico.conta(Esito.CONFLITTO), "nessun conflitto sull'archivio stesso");
        assertEquals(0, identico.conta(Esito.NON_TROVATO), "nessun mancante sull'archivio stesso");
        assertEquals(0, identico.conta(Esito.APPLICABILE), "nulla da applicare sull'archivio stesso");

        //2. Archivio "pulito" simulato. Prima le classificazioni, sullo stato finale
        Map<String, List<String[]>> gruppi = new LinkedHashMap<>();
        List<String[]> inseriti = new ArrayList<>();
        Map<String, List<String[]>> iniziali = new LinkedHashMap<>();
        Map<String, List<String[]>> finali = new LinkedHashMap<>();
        for (String[] r : c.record()) {
            switch (r[C_TIPO]) {
                case TIPO_CLASSIFICAZIONE -> gruppi.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(campi(r));
                case TIPO_SENZA_STORICO -> {
                    if (CATEGORIA_INSERITO.equals(r[C_OPERAZIONE])) {
                        inseriti.add(campi(r));
                    }
                }
                case TIPO_INIZIALE -> iniziali.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(campi(r));
                case TIPO_FINALE -> finali.computeIfAbsent(r[C_CHIAVE], k -> new ArrayList<>()).add(campi(r));
                default -> {
                }
            }
        }
        int gruppiAnnullati = 0;
        for (List<String[]> g : gruppi.values()) {
            Map<String, String[]> parti = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String[] riga : g) {
                String[] att = archivio.get(riga[0]);
                if (att != null && !"AU".equalsIgnoreCase(att[22])) {
                    parti.put(riga[0], (att[0] + "," + att[20]).split(","));
                }
            }
            if (!parti.isEmpty()) {
                GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(parti);
                gruppiAnnullati++;
            }
            //L'annullamento vero non tocca note e marcatore: un archivio reimportato ha note vuote e [22] = A
            for (String[] riga : g) {
                String[] att = archivio.get(riga[0]);
                String base = Principale_Opzioni_ModificheManuali_Ripristino.IDBaseDifferito(riga[0]);
                if (att == null && base != null) {
                    att = archivio.get(base);
                }
                if (att != null && !"AU".equalsIgnoreCase(att[22])) {
                    att[21] = "";
                    att[22] = "A";
                }
            }
        }
        //Poi i movimenti inseriti a mano e le righe iniziali al posto delle finali
        inseriti.forEach(r -> archivio.remove(r[0]));
        int invertiti = 0;
        int nonInvertiti = 0;
        for (Map.Entry<String, List<String[]>> catena : iniziali.entrySet()) {
            List<String[]> fin = finali.getOrDefault(catena.getKey(), List.of());
            if (fin.size() != 1) {
                continue; //cancellato o separato: non si simula
            }
            if (archivio.remove(fin.get(0)[0]) == null) {
                nonInvertiti++;
                continue;
            }
            for (String[] ini : catena.getValue()) {
                String[] riga = ini.clone();
                riga[MovimentiStorico.CAMPO_LIGNAGGIO] = "";
                archivio.put(riga[0], riga);
            }
            invertiti++;
        }
        System.out.println("Simulazione pulito: gruppi annullati " + gruppiAnnullati + ", inseriti tolti " + inseriti.size()
                + ", catene invertite " + invertiti + ", non invertibili " + nonInvertiti + ", movimenti ora " + archivio.size());

        Anteprima pulito = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, archivio);
        stampa("Archivio pulito simulato", pulito);
        assertEquals(0, pulito.conta(Esito.CONFLITTO), "nessun conflitto sull'archivio pulito simulato");
        assertEquals(0, pulito.conta(Esito.NON_TROVATO) - pulito.voci().stream()
                .filter(v -> v.esito() == Esito.NON_TROVATO && v.tipo().equals(CATEGORIA_MARCATO_M)).count(),
                "tutto ritrovato, salvo i marcati M senza storico (se mancano)");

        //3. Fase 3: si applicano i 238 gruppi e l'archivio deve tornare identico a quello di partenza
        var piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        var risultato = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio,
                new Principale_Opzioni_ModificheManuali_Ripristino.Storico() {
            @Override
            public boolean contiene(String lignaggio, long millis, String idVecchio) {
                return false;
            }

            @Override
            public void aggiungi(String lignaggio, String idNuovo, String idVecchio, String riga, String operazione, long millis) {
                throw new AssertionError("nessuno storico da rimettere");
            }
        });
        System.out.println("Applicazione sull'archivio pulito: applicate " + risultato.applicate() + ", saltate " + risultato.saltate().size());
        assertEquals(gruppiAnnullati, risultato.applicate());
        assertEquals(List.of(), risultato.saltate());
        Map<String, String[]> attesi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ORIGINALI.forEach(r -> attesi.put(r[0], r));
        assertEquals(attesi.keySet(), archivio.keySet(), "stessi movimenti, stessi ID");
        int righeDiverse = 0;
        for (Map.Entry<String, String[]> voce : attesi.entrySet()) {
            String[] ripristinato = archivio.get(voce.getKey());
            if (!Principale_Opzioni_ModificheManuali_Ripristino.Diversi(ripristinato, voce.getValue()).isEmpty()
                    || !ripristinato[41].equals(voce.getValue()[41])) {
                righeDiverse++;
                if (righeDiverse <= 5) {
                    System.out.println("  riga diversa " + voce.getKey() + " campi "
                            + Principale_Opzioni_ModificheManuali_Ripristino.Diversi(ripristinato, voce.getValue())
                            + (ripristinato[41].equals(voce.getValue()[41]) ? "" : " e [41]"));
                }
            }
        }
        assertEquals(0, righeDiverse, "righe diverse dopo il ripristino (campi ricalcolati esclusi)");
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili(), "idempotente");

        //4. Criterio di accettazione: stesse plusvalenze del movimento originale, con ricalcolo completo di entrambi
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String> ripristinate = campiMotore(archivio);
        ricarica();
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String> originali = campiMotore(Principale.MappaCryptoWallet);
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        //Il confronto non deve essere vuoto: il motore ha davvero scritto costi e plusvalenze
        assertTrue(originali.stream().filter(r -> !r.split(";", -1)[3].isBlank()).count() > 100,
                "il motore ha calcolato delle plusvalenze");
        assertEquals(originali.size(), ripristinate.size());
        for (int i = 0; i < originali.size(); i++) {
            assertEquals(originali.get(i), ripristinate.get(i), "campi del motore diversi dopo il ripristino");
        }
        System.out.println("Plusvalenze: " + originali.size() + " movimenti, campi 16/17/19/33/38 identici fra originale e ripristinato");
    }

    private static List<String> campiMotore(Map<String, String[]> movimenti) {
        List<String> righe = new ArrayList<>();
        new TreeMap<>(movimenti).values().forEach(v -> righe.add(v[0] + ";" + v[16] + ";" + v[17] + ";" + v[19] + ";" + v[33] + ";" + v[38]));
        return righe;
    }

    private static final class Registratore implements Principale_Opzioni_ModificheManuali_Ripristino.Storico {

        final List<String[]> voci = new ArrayList<>();

        @Override
        public boolean contiene(String lignaggio, long millis, String idVecchio) {
            return voci.stream().anyMatch(v -> v[0].equals(lignaggio) && v[5].equals(String.valueOf(millis)) && v[2].equals(idVecchio));
        }

        @Override
        public void aggiungi(String lignaggio, String idNuovo, String idVecchio, String riga, String operazione, long millis) {
            voci.add(new String[]{lignaggio, idNuovo, idVecchio, riga, operazione, String.valueOf(millis)});
        }
    }

    /**
     * Il dataset non ha nessuna catena di storico, quindi le si costruisce su movimenti veri: prezzo cambiato,
     * orario spostato (con cambio di ID), un'unione, un movimento inserito a mano. Si esporta, si riparte dai
     * movimenti originali, si applica e si confronta con lo stato finale.
     */
    @Test
    void modificheCostruiteSuMovimentiVeriSiRipristinanoSuLArchivioOriginale() throws Exception {
        ricarica();
        Map<String, String[]> finale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Principale.MappaCryptoWallet.forEach((k, v) -> finale.put(k, v.clone()));
        Map<String, String[]> pristino = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Principale.MappaCryptoWallet.forEach((k, v) -> pristino.put(k, v.clone()));

        //Movimenti semplici: importati (A), mai classificati, di tipo scambio, deposito o acquisto
        List<String> scelti = new ArrayList<>();
        for (String[] v : finale.values()) {
            String[] parti = v[0].split("_");
            if (scelti.size() < 14 && "A".equals(v[22]) && v[20].isBlank() && parti.length == 5 && parti[0].length() == 14
                    && List.of("SC", "DC", "AC").contains(parti[4])) {
                scelti.add(v[0]);
            }
        }
        assertEquals(14, scelti.size());
        Map<String, List<String[]>> storico = new LinkedHashMap<>();
        java.time.format.DateTimeFormatter idF = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        java.time.format.DateTimeFormatter campoF = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        long t = 1000;
        for (int i = 0; i < 9; i++) {
            String id = scelti.get(i);
            String[] riga = finale.get(id);
            String lign = "L" + i;
            String[] iniziale = riga.clone();
            iniziale[MovimentiStorico.CAMPO_LIGNAGGIO] = lign; //il lignaggio e' stampato prima di serializzare
            String nuovoId = id;
            String op = "ModificaMovimento";
            if (i < 6) {
                riga[15] = "1234.56";
            } else {
                //Sposta di un'ora: cambiano ID e data
                java.time.LocalDateTime d = java.time.LocalDateTime.parse(id.split("_")[0], idF).plusHours(1);
                String[] p = id.split("_");
                p[0] = d.format(idF);
                nuovoId = String.join("_", p);
                assertFalse(finale.containsKey(nuovoId));
                riga[0] = nuovoId;
                riga[1] = d.format(campoF);
                op = "TraslaOrario";
                finale.remove(id);
                finale.put(nuovoId, riga);
            }
            riga[MovimentiStorico.CAMPO_LIGNAGGIO] = lign;
            storico.put(lign, new ArrayList<>(List.<String[]>of(new String[]{id, String.valueOf(t++), Importazioni.SerializzaRiga(iniziale), op, nuovoId})));
        }
        //Unione dei movimenti 9 e 10: resta il 9 con la quantita' cambiata, il 10 sparisce
        String[] u1 = finale.get(scelti.get(9));
        String[] u2 = finale.get(scelti.get(10));
        String[] i1 = u1.clone();
        String[] i2 = u2.clone();
        i1[MovimentiStorico.CAMPO_LIGNAGGIO] = "LU";
        i2[MovimentiStorico.CAMPO_LIGNAGGIO] = "LU";
        u1[13] = "424242";
        u1[MovimentiStorico.CAMPO_LIGNAGGIO] = "LU";
        finale.remove(scelti.get(10));
        storico.put("LU", new ArrayList<>(List.<String[]>of(
                new String[]{scelti.get(9), "5000", Importazioni.SerializzaRiga(i1), "UnisciMovimenti", scelti.get(9)},
                new String[]{scelti.get(10), "5000", Importazioni.SerializzaRiga(i2), "UnisciMovimenti", scelti.get(9)})));
        //Inserito a mano
        String[] inserito = finale.get(scelti.get(11)).clone();
        inserito[0] = "20250101000000_Inventato_1_1_DC";
        inserito[39] = "E";
        inserito[41] = "99";
        finale.put(inserito[0], inserito);

        Contenuto c = Principale_Opzioni_ModificheManuali_Ripristino.Leggi(Testo(Costruisci(finale, storico)));
        Registratore registratore = new Registratore();
        var piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, pristino);
        var anteprima = piano.anteprima();
        System.out.println("Giro su righe vere:\n" + anteprima.riepilogo());
        assertEquals(0, anteprima.conta(Esito.CONFLITTO));
        assertEquals(0, anteprima.conta(Esito.NON_TROVATO));
        assertEquals(11, piano.eseguibili(), "6 prezzi + 3 spostamenti + 1 unione + 1 inserito");

        var risultato = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, pristino, registratore);

        assertEquals(11, risultato.applicate());
        assertEquals(finale.keySet(), pristino.keySet(), "stessi movimenti, stessi ID");
        for (Map.Entry<String, String[]> e : finale.entrySet()) {
            assertEquals(List.of(), Principale_Opzioni_ModificheManuali_Ripristino.Diversi(pristino.get(e.getKey()), e.getValue()),
                    "campi diversi su " + e.getKey());
        }
        for (int i = 0; i < 9; i++) {
            String lign = "L" + i;
            assertEquals(1, pristino.values().stream().filter(v -> v[MovimentiStorico.CAMPO_LIGNAGGIO].equals(lign)).count(),
                    "ogni movimento modificato porta il lignaggio della sua catena");
        }
        assertEquals("LU", pristino.get(scelti.get(9))[42]);
        assertEquals("", pristino.get("20250101000000_Inventato_1_1_DC")[41], "il documento del vecchio archivio non vale");
        assertEquals(11, registratore.voci.size(), "9 modifiche + 2 passaggi dell'unione");
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, pristino).eseguibili(), "idempotente");
    }
}
