package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Prova di {@link Binance_AutoInvest} su un dataset reale, fuori da git: {@code test/AutoInvest_Prova/dati/} con le
 * righe Auto-Invest di un CSV delle transazioni Binance ({@code Binance_Transaction_History*.csv}) e i tre file di
 * dettaglio Subscription, Rebalancing e Redemption. Senza la cartella si salta, come il golden master.
 *
 * <p>Importa il CSV come farebbe l'utente (con prezzi veri, quindi in rete: la cache dei prezzi resta nella cartella
 * e i giri successivi sono veloci), misura lo stato lasciato dallo scambio differito automatico, passa i file di
 * dettaglio e controlla che non resti nulla da classificare, che il sotto-wallet Auto-Invest finisca a zero senza
 * mai andare sotto, che i gruppi {@code [20]} siano coerenti e che un secondo passaggio non cambi niente.
 */
@Tag(GuardiaRete.TAG)
class Binance_AutoInvestDatasetRealeTest {

    static final Path CARTELLA = Path.of("test/AutoInvest_Prova");

    @BeforeAll
    static void apre() {
        assumeTrue(Files.isDirectory(CARTELLA.resolve("dati")), "Dataset Auto-Invest non presente");
        VarStatiche.setWorkingDirectory(CARTELLA.toAbsolutePath() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Database della prova non apribile (applicazione aperta?)");
    }

    @AfterAll
    static void chiude() throws Exception {
        if (DatabaseH2.connection == null) return;
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static List<String[]> DaClassificare() {
        List<String[]> r = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!"AU".equals(v[22]) && (Binance_AutoInvest.CausaleAutoInvest(v[7])
                    || Binance_AutoInvest.CAUSALE_RISCATTO.equals(v[7])) && Funzioni.noData(v[18])) r.add(v);
        }
        return r;
    }

    private static Map<String, BigDecimal> SaldiSottoWallet() {
        Map<String, BigDecimal> saldi = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!Binance_AutoInvest.WALLET_AUTO_INVEST.equals(v[4])) continue;
            if (!Funzioni.noData(v[8])) saldi.merge(v[8], new BigDecimal(v[10]), BigDecimal::add);
            if (!Funzioni.noData(v[11])) saldi.merge(v[11], new BigDecimal(v[13]), BigDecimal::add);
        }
        return saldi;
    }

    @Test
    void importaEAbbina() throws Exception {
        Path dati = CARTELLA.resolve("dati");
        File csv = null;
        List<File> dettaglio = new ArrayList<>();
        for (File f : dati.toFile().listFiles()) {
            if (f.getName().startsWith("Binance_Transaction_History")) csv = f;
            else if (f.getName().contains("Auto_Invest_History")) dettaglio.add(f);
        }
        assumeTrue(csv != null && !dettaglio.isEmpty(), "File del dataset mancanti");

        MappaCryptoWallet.clear();
        assertTrue(ImportazioneGenerica.importa(csv.getAbsolutePath(), "config/import/Binance CSV.json", false, null,
                null, "UTC+2"), "Importazione del CSV principale fallita");

        int differiti = 0;
        for (String[] v : MappaCryptoWallet.values()) {
            if (MovimentiCollegati.CAMPO18_PRELIEVO_DIFFERITO.equals(v[18])) differiti++;
        }
        List<String[]> prima = DaClassificare();
        System.out.println("=== PRIMA: movimenti " + MappaCryptoWallet.size() + ", Auto-Invest da classificare "
                + prima.size() + ", scambi differiti automatici " + differiti);

        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(dettaglio.toArray(new File[0]));
        System.out.println("=== ESITO: esecuzioni " + e.esecuzioni + " (create " + e.esecuzioniCreate + ", già a posto "
                + e.esecuzioniGiaAPosto + ", scambio differito tenuto " + e.esecuzioniScambioDifferito + ", non trovate "
                + e.esecuzioniNonTrovate + ", uscite generate " + e.usciteGenerate + ", acquisti indice " + e.acquistiIndice
                + "), scambi differiti rifatti " + e.scambiDifferitiSostituiti);
        System.out.println("    ribilanciamenti " + e.ribilanciamenti + " (creati " + e.ribilanciamentiCreati
                + ", non ricostruiti " + e.ribilanciamentiNonRicostruiti + ", monete per quadratura "
                + e.monetePerQuadratura + ", per prezzo " + e.monetePerPrezzo + "), riscatti " + e.riscatti + " (creati "
                + e.riscattiCreati + ", non trovati " + e.riscattiNonTrovati + ")");
        e.avvisi.forEach(a -> System.out.println("    AVVISO " + a));
        e.dettagli.forEach(d -> System.out.println("    DETTAGLIO " + d));
        e.riparazioni.forEach(d -> System.out.println("    RIPARATO " + d));

        List<String[]> dopo = DaClassificare();
        dopo.forEach(v -> System.out.println("    RESTA " + v[0] + " " + v[7]));
        Map<String, BigDecimal> saldi = SaldiSottoWallet();
        System.out.println("=== SALDI FINALI SOTTO-WALLET: " + saldi);
        //ControllaSaldiNegativi restituisce "exchange;wallet;moneta": il sotto-wallet si riconosce dal secondo campo.
        //Fuori dal sotto-wallet i negativi ci sono per costruzione (la prova non ha i depositi di stablecoin)
        List<String> negativi = new ArrayList<>();
        List<String> tutti = Funzioni.ControllaSaldiNegativi(MappaCryptoWallet.values());
        for (String s : tutti) {
            if (s.split(";")[1].equals(Binance_AutoInvest.WALLET_AUTO_INVEST)) negativi.add(s);
        }
        System.out.println("=== GIACENZE NEGATIVE: " + tutti);
        negativi.forEach(s -> System.out.println("    NEGATIVO " + s));
        MovimentiCollegati.Esito collegati = MovimentiCollegati.Controlla(MappaCryptoWallet.values());

        int senzaPrezzo = 0, generati = 0;
        for (String[] v : MappaCryptoWallet.values()) {
            if (!"AU".equals(v[22]) || Funzioni.noData(v[8]) || Funzioni.noData(v[11])) continue;
            generati++;
            if ("NO".equalsIgnoreCase(v[32]) || "0.00".equals(v[15])) senzaPrezzo++;
        }
        System.out.println("=== SCAMBI GENERATI: " + generati + ", senza prezzo " + senzaPrezzo);
        //Il motore sul risultato: nessuna moneta del sotto-wallet deve risultare senza costo di carico (LiFo mancante)
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        //Il dataset di prova ha solo le righe Auto-Invest: le stablecoin spese non hanno mai un deposito, quindi lì il
        //LiFo manca per costruzione. Le monete comprate dal piano invece devono averlo sempre
        Map<String, Integer> lifoMancante = new TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (Binance_AutoInvest.WALLET_AUTO_INVEST.equals(v[4]) && v[38] != null && v[38].contains("A")) {
                lifoMancante.merge(v[8], 1, Integer::sum);
            }
        }
        System.out.println("=== LIFO MANCANTE SUL SOTTO-WALLET, per moneta ceduta: " + lifoMancante);
        lifoMancante.keySet().removeAll(List.of("USDT", "BUSD", "FDUSD"));
        assertTrue(lifoMancante.isEmpty(), "LiFo mancante su monete comprate dal piano: " + lifoMancante);

        assertEquals(0, dopo.size(), "Movimenti Auto-Invest ancora da classificare");
        for (Map.Entry<String, BigDecimal> s : saldi.entrySet()) {
            assertEquals(0, s.getValue().signum(), "Saldo finale del sotto-wallet in " + s.getKey());
        }
        assertTrue(negativi.isEmpty(), "Sotto-wallet sotto zero");
        assertEquals(0, collegati.Totale(), "Movimenti collegati incoerenti");

        int n = MappaCryptoWallet.size();
        Binance_AutoInvest.Esito e2 = Binance_AutoInvest.Abbina(dettaglio.toArray(new File[0]));
        assertEquals(0, e2.Creati(), "Il secondo passaggio non deve creare nulla");
        assertEquals(n, MappaCryptoWallet.size());
        assertEquals(e.Totali(), e2.GiaAPosto() + e2.NonSistemati());
    }

    /**
     * Il verso dei ribilanciamenti ricavato dai soli prezzi (come per un piano ancora aperto, senza riscatto) confrontato
     * con quello esatto che danno sottoscrizioni e riscatto. Non può essere esatto: lo sbilancio del verso giusto è fra
     * lo 0,1% e l'1,2% del valore scambiato, e una moneta da pochi centesimi lo sposta di meno. Sul dataset del
     * 2026-10-18 sbagliano 2 righe su 153 (BTC per 0,35 euro, DOT per 0,10): la soglia fissa quel risultato.
     */
    @Test
    void versoDaiPrezzi_coincideConLaQuadratura() throws Exception {
        Path dati = CARTELLA.resolve("dati");
        Binance_AutoInvest.Dati letti = new Binance_AutoInvest.Dati();
        File csv = null;
        for (File f : dati.toFile().listFiles()) {
            if (f.getName().contains("Auto_Invest_History")) Binance_AutoInvest.LeggiFile(f, letti);
            else if (f.getName().startsWith("Binance_Transaction_History")) csv = f;
        }
        assumeTrue(csv != null && !letti.ribilanciamenti.isEmpty() && !letti.riscatti.isEmpty());
        //Acquisti indice: quelli senza un'entrata con la stessa quantità nel CSV principale
        java.util.Set<String> entrate = new java.util.HashSet<>();
        for (String riga : Files.readAllLines(csv.toPath())) {
            String[] c = riga.split(",", -1);
            if (c.length > 5 && Binance_AutoInvest.CAUSALE_TRANSAZIONE.equals(c[3]) && !c[5].startsWith("-")) {
                entrate.add(c[4] + "|" + new BigDecimal(c[5]).stripTrailingZeros().toPlainString());
            }
        }
        List<Binance_AutoInvest.Acquisto> indice = new ArrayList<>();
        for (Binance_AutoInvest.Acquisto a : letti.acquisti) {
            if (!entrate.contains(a.moneta + "|" + a.unita.stripTrailingZeros().toPlainString())) indice.add(a);
        }
        Binance_AutoInvest.Esito esito = new Binance_AutoInvest.Esito();
        assertNull(Binance_AutoInvest.SegniRibilanciamenti(indice, letti.ribilanciamenti, letti.riscatti,
                Binance_AutoInvest.PrezzatoreDefault, esito));
        Map<Binance_AutoInvest.Ribilanciamento, Integer> esatti = new java.util.IdentityHashMap<>();
        for (Binance_AutoInvest.Ribilanciamento r : letti.ribilanciamenti) {
            esatti.put(r, r.segno);
            r.segno = 0;
        }
        assertNull(Binance_AutoInvest.SegniRibilanciamenti(indice, letti.ribilanciamenti, List.of(),
                Binance_AutoInvest.PrezzatoreDefault, esito));
        int diversi = 0, totali = 0;
        for (Binance_AutoInvest.Ribilanciamento r : letti.ribilanciamenti) {
            if (r.quantita.signum() == 0) continue;
            totali++;
            if (r.segno != esatti.get(r)) {
                diversi++;
                System.out.println("    VERSO DIVERSO " + r.data + " " + r.moneta + " " + r.quantita.toPlainString()
                        + ": prezzi " + r.segno + ", quadratura " + esatti.get(r));
            }
        }
        System.out.println("=== VERSO DAI PREZZI: " + (totali - diversi) + " righe su " + totali + " come la quadratura");
        assertTrue(diversi * 50 <= totali, "Verso dai prezzi sbagliato su più del 2% delle righe");
    }

    /**
     * I file di dettaglio rinominati dall'utente (niente {@code UTC2} nel nome) danno lo stesso risultato dei file
     * col nome di Binance: il tipo viene dall'intestazione, il fuso si ricava dal contenuto.
     */
    @Test
    void fileRinominati_stessoRisultato(@org.junit.jupiter.api.io.TempDir Path rinominati) throws Exception {
        Path dati = CARTELLA.resolve("dati");
        File csv = null;
        List<File> dettaglio = new ArrayList<>();
        List<File> copie = new ArrayList<>();
        for (File f : dati.toFile().listFiles()) {
            if (f.getName().startsWith("Binance_Transaction_History")) csv = f;
            else if (f.getName().contains("Auto_Invest_History")) dettaglio.add(f);
        }
        assumeTrue(csv != null && !dettaglio.isEmpty(), "File del dataset mancanti");
        for (int i = 0; i < dettaglio.size(); i++) {
            Path copia = rinominati.resolve("dettaglio_" + i + ".csv");
            Files.copy(dettaglio.get(i).toPath(), copia);
            copie.add(copia.toFile());
        }

        Map<String, String> colNome = new TreeMap<>();
        MappaCryptoWallet.clear();
        assertTrue(ImportazioneGenerica.importa(csv.getAbsolutePath(), "config/import/Binance CSV.json", false, null,
                null, "UTC+2"));
        Binance_AutoInvest.Abbina(dettaglio.toArray(new File[0]));
        MappaCryptoWallet.forEach((k, v) -> colNome.put(k, v[5] + "|" + v[18] + "|" + v[10] + "|" + v[13]));

        Map<String, String> senzaNome = new TreeMap<>();
        MappaCryptoWallet.clear();
        assertTrue(ImportazioneGenerica.importa(csv.getAbsolutePath(), "config/import/Binance CSV.json", false, null,
                null, "UTC+2"));
        Binance_AutoInvest.Esito e = Binance_AutoInvest.Abbina(copie.toArray(new File[0]));
        e.avvisi.forEach(a -> System.out.println("    AVVISO " + a));
        MappaCryptoWallet.forEach((k, v) -> senzaNome.put(k, v[5] + "|" + v[18] + "|" + v[10] + "|" + v[13]));

        //In caso di differenza le prime righe diverse, invece del confronto troncato delle due mappe intere
        java.util.Set<String> chiavi = new java.util.TreeSet<>(colNome.keySet());
        chiavi.addAll(senzaNome.keySet());
        chiavi.stream().filter(k -> !java.util.Objects.equals(colNome.get(k), senzaNome.get(k))).limit(20)
                .forEach(k -> System.out.println("    DIVERSO " + k + ": " + colNome.get(k) + " / " + senzaNome.get(k)));
        assertTrue(e.avvisi.stream().anyMatch(a -> a.contains("spostati di -2 ore")), e.avvisi.toString());
        assertEquals(colNome, senzaNome);
    }
}
