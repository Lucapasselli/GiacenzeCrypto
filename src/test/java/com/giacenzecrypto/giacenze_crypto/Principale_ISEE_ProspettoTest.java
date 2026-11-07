package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il prospetto DSU ({@link Principale_ISEE_Prospetto}) e le tre schede del dettaglio del tab ISEE
 * ({@link Principale_ISEE#Dettaglio}, {@link Principale_ISEE#Monete}, {@link Principale_ISEE#Avvisi}): bozza del
 * manifest, PDF ed Excel, archiviazione nella famiglia {@link Dichiarazioni.Famiglia#ISEE}. I rapporti nascono da
 * {@link Calcoli_ISEE} con le fonti iniettate, il database è temporaneo (alias e opzioni).
 */
class Principale_ISEE_ProspettoTest {

    @TempDir
    static Path tempDir;

    @TempDir
    Path radice;

    private static int progressivo = 0;

    private final List<String[]> movimenti = new ArrayList<>();
    private final Map<String, List<String[]>> foto = new HashMap<>();
    private final Map<String, List<String[]>> periodi = new HashMap<>();
    private final Map<String, TreeMap<LocalDate, BigDecimal>> euro = new HashMap<>();

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        progressivo = 0;
        Dichiarazioni.RadiceDiProva = radice.toFile();
    }

    @AfterEach
    void tearDown() {
        Dichiarazioni.RadiceDiProva = null;
    }

    // --- dati, come in Calcoli_ISEETest -------------------------------------------

    private void mov(String data, String gruppo, String monetaU, String qtaU, String monetaE, String qtaE) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = data.replace("-", "").replace(" ", "").replace(":", "") + "_Test_" + (++progressivo) + "_1_DC";
        v[1] = data;
        v[3] = gruppo;
        v[8] = monetaU;
        v[9] = monetaU.isEmpty() ? "" : "Crypto";
        v[10] = qtaU;
        v[11] = monetaE;
        v[12] = monetaE.isEmpty() ? "" : "Crypto";
        v[13] = qtaE;
        movimenti.add(v);
    }

    private void fotografia(String gruppo, String moneta, String qta, String valore) {
        String[] r = new String[17];
        java.util.Arrays.fill(r, "");
        r[7] = moneta;
        r[8] = qta;
        r[10] = valore;
        foto.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(r);
    }

    private void periodoFiat(String gruppo, String inizio, String stato, String ident, String contoCorrente) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = Principale_GruppiWalletRW.TIPO_FIAT;
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = "1";
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = inizio;
        r[Principale_GruppiWalletRW.COL_STATO_ESTERO] = stato;
        r[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = ident;
        r[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE] = contoCorrente;
        periodi.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(r);
    }

    private final Calcoli_ISEE.LiquiditaFiat liq = new Calcoli_ISEE.LiquiditaFiat() {
        @Override
        public boolean presente(String gruppo) {
            return euro.containsKey(gruppo);
        }

        @Override
        public java.util.Set<String> gruppi() {
            return euro.keySet();
        }

        @Override
        public LocalDate primo(String gruppo) {
            return euro.containsKey(gruppo) ? euro.get(gruppo).firstKey() : null;
        }

        @Override
        public LocalDate ultimoFinoA(String gruppo, LocalDate limite) {
            return euro.containsKey(gruppo) ? euro.get(gruppo).floorKey(limite) : null;
        }

        @Override
        public BigDecimal saldoEUR(String gruppo, LocalDate giorno, LocalDate dataCambio, List<String> avvisi) {
            BigDecimal s = BigDecimal.ZERO;
            for (Map.Entry<LocalDate, BigDecimal> e : euro.getOrDefault(gruppo, new TreeMap<>()).entrySet()) {
                if (!e.getKey().isAfter(giorno)) {
                    s = s.add(e.getValue());
                }
            }
            return s;
        }

        @Override
        public BigDecimal mediaAnnua(String gruppo, LocalDate da, LocalDate a, LocalDate dataCambio, List<String> avvisi) {
            BigDecimal somma = BigDecimal.ZERO;
            for (LocalDate g = da; !g.isAfter(a); g = g.plusDays(1)) {
                somma = somma.add(saldoEUR(gruppo, g, dataCambio, avvisi).max(BigDecimal.ZERO));
            }
            return somma.divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        }
    };

    /**
     * Crypto.com 2025: conto corrente da 100 euro al 31/12 (media 299,45, vedi Calcoli_ISEETest) e 1 ETH da 3000,40.
     * Binance: 2 BTC a 150.000,60 e un token senza prezzo, quindi valore incompleto.
     */
    private Calcoli_ISEE.Risultato dueGruppi() {
        periodoFiat("Crypto.com", "2025-01-27", "105", "C88392", "SI");
        euro.computeIfAbsent("Crypto.com", k -> new TreeMap<>()).put(LocalDate.parse("2024-06-01"), new BigDecimal("500"));
        euro.get("Crypto.com").put(LocalDate.parse("2025-07-02"), new BigDecimal("-400"));
        mov("2024-06-02 10:00", "Crypto.com", "", "", "ETH", "1");
        fotografia("Crypto.com", "ETH", "1", "3000.40");
        mov("2025-03-01 10:00", "Binance", "", "", "BTC", "2");
        mov("2025-03-02 10:00", "Binance", "", "", "XYZ", "50");
        fotografia("Binance", "BTC", "2", "150000.60");
        fotografia("Binance", "XYZ", "50", null);
        return Calcoli_ISEE.calcola(2025, movimenti, v -> v[3], foto, g -> periodi.getOrDefault(g, List.of()), liq,
                k -> null, g -> g);
    }

    private static Calcoli_ISEE.Rapporto delTipo(Calcoli_ISEE.Risultato ris, String gruppo, String tipo) {
        for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
            if (r.gruppo.equals(gruppo) && r.tipo.equals(tipo)) {
                return r;
            }
        }
        fail("manca il rapporto " + gruppo + " " + tipo);
        return null;
    }

    // --- schede del dettaglio -------------------------------------------------------

    @Test
    void schedaMonete_unaRigaPerMonetaEIlTotaleDelRapporto_eIlDettaglioNonLeRipete() {
        Calcoli_ISEE.Risultato ris = dueGruppi();
        Calcoli_ISEE.Rapporto binance = delTipo(ris, "Binance", "99");

        List<Object[]> monete = Principale_ISEE.Monete(binance);
        assertEquals(3, monete.size(), "BTC, XYZ e il totale");
        assertEquals("BTC", monete.get(0)[0]);
        assertEquals("2", monete.get(0)[1]);
        assertEquals("150.000,60", monete.get(0)[2]);
        assertEquals("n.d.", monete.get(1)[2]);
        assertTrue(monete.get(1)[3].toString().contains("prezzo mancante"));
        assertEquals("Totale", monete.get(2)[0]);
        assertEquals("150.000,60", monete.get(2)[2]);
        assertEquals("incompleto", monete.get(2)[3]);

        for (Object[] riga : Principale_ISEE.Dettaglio(binance)) {
            assertFalse(riga[0].toString().startsWith("Moneta "), "le monete stanno nella loro scheda");
            assertNotEquals("Avviso", riga[0], "gli avvisi stanno nella loro scheda");
        }
        assertEquals(binance.note.size(), Principale_ISEE.Avvisi(binance).size());
        assertFalse(binance.note.isEmpty(), "senza periodo FIAT e senza identificativo ci sono avvisi");
    }

    @Test
    void schedaMonete_ilContoCorrenteHaUnaRigaCheDicePerche() {
        Calcoli_ISEE.Rapporto conto = delTipo(dueGruppi(), "Crypto.com", "01");
        List<Object[]> monete = Principale_ISEE.Monete(conto);
        assertEquals(1, monete.size());
        assertTrue(monete.get(0)[3].toString().contains("saldo del conto"));
        assertTrue(Principale_ISEE.Monete(null).isEmpty());
    }

    // --- bozza del manifest ---------------------------------------------------------

    @Test
    void bozza_totaliDelleDueSezioni_erroriEMediaNonCalcolata() {
        Calcoli_ISEE.Risultato ris = dueGruppi();
        Dichiarazioni.Dichiarazione d = Principale_ISEE_Prospetto.Bozza(ris);

        assertEquals(Dichiarazioni.Famiglia.ISEE, d.Fam);
        assertEquals("2025", d.Anno, "l'anno è quello del patrimonio");
        assertEquals("2027", d.Totali.get(Principale_ISEE_Prospetto.TOT_DSU));
        assertEquals("3", d.Totali.get(Principale_ISEE_Prospetto.TOT_RAPPORTI));
        assertEquals("100", d.Totali.get(Principale_ISEE_Prospetto.TOT_SEZ_I_SALDI));
        assertEquals("299", d.Totali.get(Principale_ISEE_Prospetto.TOT_SEZ_I_MEDIE));
        // somma dei valori arrotondati all'unità: 3000 + 150001
        assertEquals("153001", d.Totali.get(Principale_ISEE_Prospetto.TOT_SEZ_II));
        assertEquals("153101", d.Totali.get(Principale_Dichiarazioni.TOT_ISEE_VALORE));
        assertTrue(d.Errori.contains("valore al 31/12 incompleto"));
        assertTrue(d.Errori.contains("Binance - 99"));

        Map<String, String> conto = d.Righi.stream().filter(r -> "I".equals(r.get("Sezione"))).findFirst().orElseThrow();
        assertTrue(conto.get("Rapporto").startsWith("Crypto.com - 01"), conto.get("Rapporto"));
        assertEquals("01", conto.get("Tipo rapporto"));
        assertEquals("299", conto.get("Giacenza media"));
        assertFalse(conto.containsKey("Media giornaliera informativa"), "il 01 ha la giacenza media del modulo");
        for (Map<String, String> rigo : d.Righi) {
            if ("II".equals(rigo.get("Sezione"))) {
                assertEquals(Principale_ISEE_Prospetto.NON_CALCOLATA, rigo.get("Media giornaliera informativa"),
                        "mai vuota né zero se non è stata calcolata");
                assertFalse(rigo.containsKey("Giacenza media"), "la Sez. II non ha la colonna della giacenza media");
            }
        }
        assertEquals(Principale_ISEE_Prospetto.NON_CALCOLATA, d.Opzioni.get("Media giornaliera dei rapporti 99"));

        // con la media calcolata la colonna informativa ha il numero e l'impronta cambia
        String prima = d.Impronta();
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, (m, qta, g) -> "XYZ".equals(m.Moneta) ? null : qta.multiply(new BigDecimal("10")));
        Dichiarazioni.Dichiarazione dopo = Principale_ISEE_Prospetto.Bozza(ris);
        String mediaBinance = dopo.Righi.stream().filter(r -> r.get("Rapporto").startsWith("Binance"))
                .findFirst().orElseThrow().get("Media giornaliera informativa");
        assertTrue(mediaBinance.endsWith("(incompleta)"), mediaBinance);
        assertTrue(dopo.Errori.contains("media giornaliera informativa incompleta"));
        assertNotEquals(prima, dopo.Impronta());
    }

    // --- file e archivio ------------------------------------------------------------

    @Test
    void pdfEdExcel_siArchivianoNellaFamigliaDSU_eIlManifestSiRilegge() throws Exception {
        Calcoli_ISEE.Risultato ris = dueGruppi();
        Dichiarazioni.Dichiarazione bozza = Principale_ISEE_Prospetto.Bozza(ris);
        File tmp = Dichiarazioni.NuovaCartellaTemporanea();
        Principale_ISEE_Prospetto.ScriviPdf(ris, bozza, new File(tmp, "p.pdf"));
        Principale_ISEE_Prospetto.ScriviExcel(ris, new File(tmp, "d.xlsx"));
        assertTrue(Stampe.PdfValido(new File(tmp, "p.pdf")));
        try (com.lowagie.text.pdf.PdfReader r = new com.lowagie.text.pdf.PdfReader(Files.readAllBytes(tmp.toPath().resolve("p.pdf")))) {
            assertTrue(r.getNumberOfPages() >= 4, "copertina, Quadro FC2, dettaglio, opzioni e note");
        }
        try (ZipFile z = new ZipFile(new File(tmp, "d.xlsx"))) {
            assertNotNull(z.getEntry("xl/workbook.xml"));
        }

        Dichiarazioni.Dichiarazione reg = Dichiarazioni.Registra(tmp, bozza, List.of("p.pdf", "d.xlsx"));
        assertTrue(reg.Cartella.getName().startsWith("2025_ISEE_"));
        Dichiarazioni.Dichiarazione letta = Dichiarazioni.Leggi(reg.Cartella);
        assertNotNull(letta);
        assertEquals(Dichiarazioni.Famiglia.ISEE, letta.Fam);
        assertEquals(bozza.Impronta(), letta.Impronta());

        List<Object[]> elenco = Principale_Dichiarazioni.RigheElenco(List.of(letta));
        assertEquals("DSU", elenco.get(0)[1]);
        assertEquals("153101", elenco.get(0)[4]);
        assertEquals("", elenco.get(0)[5], "la DSU non ha un'imposta");
        assertEquals("DSU 2027 (patrimonio al 31/12/2025)", Principale_Dichiarazioni.RigheDettaglio(letta).get(0)[1]);
    }

    @Test
    void unaVersioneCheNonConosceLaFamigliaIgnoraLaCartella() throws Exception {
        File c = new File(radice.toFile(), "2025_XYZ_20261107120000");
        assertTrue(c.mkdirs());
        Files.writeString(new File(c, Dichiarazioni.NOME_MANIFEST).toPath(), "{\"famiglia\":\"XYZ\",\"anno\":\"2025\"}");
        assertNull(Dichiarazioni.Leggi(c));
    }

    // --- confronto con la definitiva ----------------------------------------------

    @Test
    void definitiva_unCalcoloSenzaMediaNonRisultaDiversoDaUnaDefinitivaConLaMedia() {
        Calcoli_ISEE.Risultato conMedia = dueGruppi();
        Calcoli_ISEE.calcolaMediaGiornaliera(conMedia, (m, qta, g) -> "XYZ".equals(m.Moneta) ? null : qta.multiply(new BigDecimal("10")));
        Dichiarazioni.Dichiarazione def = Principale_ISEE_Prospetto.Bozza(conMedia);
        assertTrue(def.Errori.contains(Principale_ISEE_Prospetto.ERRORE_MEDIA), "la definitiva ha anche l'errore della media");

        movimenti.clear();
        foto.clear();
        periodi.clear();
        euro.clear();
        Calcoli_ISEE.Risultato senzaMedia = dueGruppi();
        assertTrue(Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(senzaMedia, def).isEmpty(),
                "la media non chiesta non è una differenza");

        // con la media calcolata la si confronta: con prezzi diversi la media cambia
        Calcoli_ISEE.calcolaMediaGiornaliera(senzaMedia, (m, qta, g) -> "XYZ".equals(m.Moneta) ? null : qta.multiply(new BigDecimal("20")));
        List<String[]> diff = Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(senzaMedia, def);
        assertTrue(diff.stream().anyMatch(d -> d[0].endsWith(Principale_ISEE_Prospetto.RIGO_MEDIA)), "manca la differenza della media");
    }

    @Test
    void definitiva_unValoreCambiatoEUnaDifferenzaDelSuoRapporto() {
        Dichiarazioni.Dichiarazione def = Principale_ISEE_Prospetto.Bozza(dueGruppi());
        assertTrue(Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(null, def).isEmpty());

        movimenti.clear();
        foto.clear();
        periodi.clear();
        euro.clear();
        Calcoli_ISEE.Risultato ris = dueGruppi();
        assertTrue(Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(ris, null).isEmpty(), "senza definitiva nessuna differenza");
        assertTrue(Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(ris, def).isEmpty(), "stesso calcolo, nessuna differenza");

        // Binance vale 1.000 euro in più al 31/12: stessi movimenti, fotografia cambiata
        foto.get("Binance").get(0)[10] = "151000.60";
        List<String[]> diff = Principale_ISEE_Prospetto.DifferenzeDallaDefinitiva(Calcoli_ISEE.calcola(2025, movimenti,
                v -> v[3], foto, g -> periodi.getOrDefault(g, List.of()), liq, k -> null, g -> g), def);
        assertTrue(diff.stream().anyMatch(d -> d[0].startsWith("Rigo Binance - 99") && d[0].endsWith("Valore al 31/12")
                && d[1].equals("150001") && d[2].equals("151001")), "manca la differenza del rapporto Binance");
    }
}
