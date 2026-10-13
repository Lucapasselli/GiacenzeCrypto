package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'archivio delle dichiarazioni: registrazione tutto-o-niente, una sola definitiva per anno e
 * famiglia, riconoscimento delle ristampe identiche, integrità dei file, continuità W/RW e ripristino
 * da backup che aggiunge senza sovrascrivere.
 */
class DichiarazioniTest {

    @TempDir
    File radice;

    @BeforeEach
    void preparaRadice() {
        Dichiarazioni.RadiceDiProva = radice;
    }

    @AfterEach
    void ripristinaRadice() {
        Dichiarazioni.RadiceDiProva = null;
    }

    private static Dichiarazioni.Dichiarazione bozza(Dichiarazioni.Famiglia fam, String anno, String valore) {
        Dichiarazioni.Dichiarazione d = new Dichiarazioni.Dichiarazione();
        d.Fam = fam;
        d.Anno = anno;
        d.Totali.put("Totale", valore);
        d.Opzioni.put(Dichiarazioni.OPZIONE_METODO_RW, Dichiarazioni.METODO_RW_SOLO_VALORI_INIZIO_FINE);
        return d;
    }

    private static Dichiarazioni.Dichiarazione registra(Dichiarazioni.Famiglia fam, String anno, String valore) throws IOException {
        File tmp = Dichiarazioni.NuovaCartellaTemporanea();
        Files.writeString(tmp.toPath().resolve("a.pdf"), "pdf " + valore);
        Files.writeString(tmp.toPath().resolve("b.xlsx"), "xlsx " + valore);
        return Dichiarazioni.Registra(tmp, bozza(fam, anno, valore), List.of("a.pdf", "b.xlsx"));
    }

    @Test
    void laRegistrazioneScriveIlManifestERinominaLaCartella() throws IOException {
        Dichiarazioni.Dichiarazione d = registra(Dichiarazioni.Famiglia.RW, "2025", "100");
        assertTrue(d.Cartella.getName().startsWith("2025_RW_"));
        assertEquals(2, d.File.size());

        List<Dichiarazioni.Dichiarazione> elenco = Dichiarazioni.Elenco();
        assertEquals(1, elenco.size());
        Dichiarazioni.Dichiarazione letta = elenco.get(0);
        assertEquals("100", letta.Totali.get("Totale"));
        assertEquals(d.Impronta(), letta.Impronta());
        assertEquals(d.File, letta.File);
    }

    @Test
    void unFileMancanteNonRegistraNienteECancellaLaCartellaTemporanea() throws IOException {
        File tmp = Dichiarazioni.NuovaCartellaTemporanea();
        Files.writeString(tmp.toPath().resolve("a.pdf"), "pdf");
        Dichiarazioni.Dichiarazione b = bozza(Dichiarazioni.Famiglia.RW, "2025", "1");
        assertThrows(IOException.class, () -> Dichiarazioni.Registra(tmp, b, List.of("a.pdf", "b.xlsx")));
        assertFalse(tmp.exists());
        assertTrue(Dichiarazioni.Elenco().isEmpty());
    }

    @Test
    void unaSolaDefinitivaPerAnnoEFamiglia() throws IOException {
        Dichiarazioni.Dichiarazione a = registra(Dichiarazioni.Famiglia.RW, "2025", "1");
        Dichiarazioni.Dichiarazione b = registra(Dichiarazioni.Famiglia.RW, "2025", "2");
        Dichiarazioni.Dichiarazione t = registra(Dichiarazioni.Famiglia.RT, "2025", "3");
        Dichiarazioni.ImpostaDefinitiva(a, true);
        Dichiarazioni.ImpostaDefinitiva(t, true);
        Dichiarazioni.ImpostaDefinitiva(b, true);

        assertEquals(b.Id(), Dichiarazioni.Definitiva(Dichiarazioni.Famiglia.RW, "2025").Id());
        //L'altra famiglia dello stesso anno non e' toccata
        assertEquals(t.Id(), Dichiarazioni.Definitiva(Dichiarazioni.Famiglia.RT, "2025").Id());
        long definitive = Dichiarazioni.Elenco().stream().filter(d -> d.Definitiva).count();
        assertEquals(2, definitive);

        Dichiarazioni.ImpostaDefinitiva(b, false);
        assertNull(Dichiarazioni.Definitiva(Dichiarazioni.Famiglia.RW, "2025"));
    }

    @Test
    void dueDefinitiveDopoUnRipristinoVinceQuellaSegnataPerUltima() throws IOException {
        Dichiarazioni.Dichiarazione a = registra(Dichiarazioni.Famiglia.RW, "2024", "1");
        Dichiarazioni.Dichiarazione b = registra(Dichiarazioni.Famiglia.RW, "2024", "2");
        //Scritte a mano come le porterebbe un ripristino: entrambe definitive
        a.Definitiva = true;
        a.DefinitivaDal = "2026-01-01 10:00:00";
        b.Definitiva = true;
        b.DefinitivaDal = "2026-03-01 10:00:00";
        Files.writeString(new File(a.Cartella, Dichiarazioni.NOME_MANIFEST).toPath(), Dichiarazioni.ManifestAJson(a).toString(), StandardCharsets.UTF_8);
        Files.writeString(new File(b.Cartella, Dichiarazioni.NOME_MANIFEST).toPath(), Dichiarazioni.ManifestAJson(b).toString(), StandardCharsets.UTF_8);

        assertEquals(b.Id(), Dichiarazioni.Definitiva(Dichiarazioni.Famiglia.RW, "2024").Id());
        //La correzione e' scritta anche su disco
        assertFalse(Dichiarazioni.Leggi(a.Cartella).Definitiva);
    }

    @Test
    void ristampaIdenticaRiconosciuta() throws IOException {
        registra(Dichiarazioni.Famiglia.RT, "2025", "50");
        assertNotNull(Dichiarazioni.UltimaUguale(bozza(Dichiarazioni.Famiglia.RT, "2025", "50")));
        assertNull(Dichiarazioni.UltimaUguale(bozza(Dichiarazioni.Famiglia.RT, "2025", "51")));
        assertNull(Dichiarazioni.UltimaUguale(bozza(Dichiarazioni.Famiglia.RW, "2025", "50")));
    }

    @Test
    void unFileModificatoDopoLaGenerazioneSiVede() throws IOException {
        Dichiarazioni.Dichiarazione d = registra(Dichiarazioni.Famiglia.RW, "2025", "1");
        File pdf = new File(d.Cartella, "a.pdf");
        //I file archiviati sono di sola lettura: modificarli richiede di toglierla di proposito
        assertFalse(pdf.canWrite());
        pdf.setWritable(true);
        Files.writeString(pdf.toPath(), "altro");
        new File(d.Cartella, "b.xlsx").delete();
        Map<String, Dichiarazioni.StatoFile> esito = Dichiarazioni.VerificaIntegrita(d);
        assertEquals(Dichiarazioni.StatoFile.ALTERATO, esito.get("a.pdf"));
        assertEquals(Dichiarazioni.StatoFile.MANCANTE, esito.get("b.xlsx"));
    }

    @Test
    void laNotaSiConserva() throws IOException {
        Dichiarazioni.Dichiarazione d = registra(Dichiarazioni.Famiglia.RW, "2025", "1");
        Dichiarazioni.ImpostaNota(d, "  inviata il 30/09  ");
        assertEquals("inviata il 30/09", Dichiarazioni.Leggi(d.Cartella).Nota);
    }

    // ---------------------------------------------------------------- confronto, allegati, backup

    @Test
    void ilConfrontoTrovaSoloLeDifferenzeVere() {
        String m = Dichiarazioni.METODO_RW_SOLO_VALORI_INIZIO_FINE;
        Dichiarazioni.Dichiarazione a = rw("2025", m, "01 (Binance)", "10.0", "20", "02 (Kraken)", "5", "5");
        Dichiarazioni.Dichiarazione b = rw("2025", m, "01 (Binance)", "10.00", "21", "03 (OKX)", "1", "1");
        a.Totali.put("Totale", "30");
        b.Totali.put("Totale", "30.00");
        b.Opzioni.put("LiFo", "per gruppo wallet");
        List<String[]> diff = Dichiarazioni.Confronta(a, b);
        //10.0 e 10.00, 30 e 30.00 sono uguali: restano valore finale Binance, Kraken sparito, OKX nuovo, l'opzione
        assertEquals(4, diff.size());
        assertEquals("Rigo 01 (Binance): Valore finale", diff.get(0)[0]);
        assertEquals("20", diff.get(0)[1]);
        assertEquals("21", diff.get(0)[2]);
        assertEquals("(assente)", diff.get(2)[1]);
        assertEquals("Opzione: LiFo", diff.get(3)[0]);
        assertTrue(Dichiarazioni.Confronta(a, a).isEmpty());
    }

    @Test
    void gliAllegatiSiAggiungonoEToglono() throws IOException {
        Dichiarazioni.Dichiarazione d = registra(Dichiarazioni.Famiglia.RT, "2025", "1");
        File ricevuta = new File(radice.getParentFile(), "ricevuta_" + System.nanoTime() + ".pdf");
        try {
            Files.writeString(ricevuta.toPath(), "ricevuta");
            String nome = Dichiarazioni.AggiungiAllegato(d, ricevuta);
            String nome2 = Dichiarazioni.AggiungiAllegato(d, ricevuta);
            assertTrue(nome.startsWith(Dichiarazioni.PREFISSO_ALLEGATO));
            assertFalse(nome.equals(nome2));
            Dichiarazioni.Dichiarazione letta = Dichiarazioni.Leggi(d.Cartella);
            assertEquals(2, letta.Allegati.size());
            assertEquals(Dichiarazioni.StatoFile.INTEGRO, Dichiarazioni.VerificaIntegrita(letta).get(nome));
            //Gli allegati non cambiano i valori: la ristampa identica resta riconosciuta
            assertNotNull(Dichiarazioni.UltimaUguale(bozza(Dichiarazioni.Famiglia.RT, "2025", "1")));

            Dichiarazioni.RimuoviAllegato(letta, nome);
            assertFalse(new File(d.Cartella, nome).exists());
            assertEquals(1, Dichiarazioni.Leggi(d.Cartella).Allegati.size());
        } finally {
            ricevuta.delete();
        }
    }

    @Test
    void ilBackupCollegatoSiRitrovaSoloSullaDefinitiva() throws IOException {
        Dichiarazioni.Dichiarazione d = registra(Dichiarazioni.Famiglia.RW, "2025", "1");
        Dichiarazioni.ImpostaBackup(d, "backup_20261013120000.zip");
        assertTrue(Dichiarazioni.BackupDelleDefinitive().isEmpty());
        Dichiarazioni.ImpostaDefinitiva(d, true);
        assertEquals(d.Id(), Dichiarazioni.BackupDelleDefinitive().get("backup_20261013120000.zip").Id());
        assertEquals("backup_20261013120000.zip", Dichiarazioni.Leggi(d.Cartella).Backup);
    }

    // ---------------------------------------------------------------- Excel W/RW

    private static String[] tratto(String da, String a) {
        String[] t = new String[Calcoli_RW_PeriodiCrypto.TC_COLONNE];
        java.util.Arrays.fill(t, "");
        t[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO] = da;
        t[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE] = a;
        return t;
    }

    @Test
    void ogniRigaDiDettaglioVaNelPeriodoCheContieneLaSuaDataDiFine() {
        List<String[]> tratti = List.of(tratto("2025-01-01", "2025-06-30"), tratto("2025-07-01", "2025-12-31"));
        assertEquals("dal 01/01 al 30/06", Principale_Dichiarazioni.PeriodoDellaData("2025-06-30", tratti));
        assertEquals("dal 01/07 al 31/12", Principale_Dichiarazioni.PeriodoDellaData("2025-07-01", tratti));
        assertEquals("", Principale_Dichiarazioni.PeriodoDellaData("2026-01-01", tratti));
        assertEquals("anno intero", Principale_Dichiarazioni.PeriodoDellaData("2025-03-15",
                List.<String[]>of(tratto("2025-01-01", "2025-12-31"))));
    }

    // ---------------------------------------------------------------- continuita'

    private static Dichiarazioni.Dichiarazione rw(String anno, String metodo, String... righi) {
        Dichiarazioni.Dichiarazione d = new Dichiarazioni.Dichiarazione();
        d.Fam = Dichiarazioni.Famiglia.RW;
        d.Anno = anno;
        d.Opzioni.put(Dichiarazioni.OPZIONE_METODO_RW, metodo);
        for (int i = 0; i < righi.length; i += 3) {
            d.Righi.add(new java.util.LinkedHashMap<>(Map.of(
                    "Rigo", righi[i], "Natura", "CRYPTO", "Periodo", "",
                    "Valore iniziale", righi[i + 1], "Valore finale", righi[i + 2])));
        }
        return d;
    }

    @Test
    void continuitaSoloConIlMetodoDeiValoriDiInizioEFineAnno() {
        Dichiarazioni.Dichiarazione p = rw("2024", "Media ponderata", "01 (Binance)", "0", "100");
        Dichiarazioni.Dichiarazione s = rw("2025", "Media ponderata", "01 (Binance)", "50", "80");
        assertFalse(Dichiarazioni.ConfrontaContinuita(p, s).NonVerificabile.isEmpty());
    }

    @Test
    void continuitaNonVerificabileConOpzioniDiverse() {
        String m = Dichiarazioni.METODO_RW_SOLO_VALORI_INIZIO_FINE;
        Dichiarazioni.Dichiarazione p = rw("2024", m, "01 (Binance)", "0", "100");
        Dichiarazioni.Dichiarazione s = rw("2025", m, "01 (Binance)", "100", "80");
        s.Opzioni.put("LiFo", "per gruppo wallet");
        Dichiarazioni.Continuita c = Dichiarazioni.ConfrontaContinuita(p, s);
        assertTrue(c.NonVerificabile.contains("LiFo"));
    }

    @Test
    void continuitaTrovaIRighiCheNonCoincidono() {
        String m = Dichiarazioni.METODO_RW_SOLO_VALORI_INIZIO_FINE;
        Dichiarazioni.Dichiarazione p = rw("2024", m,
                "01 (Binance)", "0", "100.40",
                "02 (Coinbase)", "0", "200",
                "03 (Kraken)", "0", "5");
        Dichiarazioni.Dichiarazione s = rw("2025", m,
                "01 (Binance)", "100.20", "0",
                "02 (Coinbase)", "190", "0",
                "04 (OKX)", "10", "0");
        Dichiarazioni.Continuita c = Dichiarazioni.ConfrontaContinuita(p, s);
        assertEquals("", c.NonVerificabile);
        assertEquals(2, c.Confrontati);
        //Arrotondati all'euro come nel quadro: 100,40 e 100,20 coincidono
        assertEquals(1, c.Differenze.size());
        assertTrue(c.Differenze.get(0).startsWith("02 (Coinbase)"));
        assertEquals(2, c.NonConfrontati.size());
    }

    // ---------------------------------------------------------------- backup

    @Test
    void ilRipristinoAggiungeSenzaSovrascrivere() throws IOException {
        Dichiarazioni.Dichiarazione esistente = registra(Dichiarazioni.Famiglia.RW, "2025", "1");
        Dichiarazioni.ImpostaNota(esistente, "nota locale");

        File zip = new File(radice.getParentFile(), "backup_prova_" + System.nanoTime() + ".zip");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                //La stessa cartella con un altro manifest: non deve vincere
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + esistente.Id() + "/" + Dichiarazioni.NOME_MANIFEST, "{}");
                //Una nuova: va aggiunta
                Dichiarazioni.Dichiarazione nuova = bozza(Dichiarazioni.Famiglia.RT, "2023", "9");
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + "2023_RT_20240101000000/" + Dichiarazioni.NOME_MANIFEST,
                        Dichiarazioni.ManifestAJson(nuova).toString());
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + "2023_RT_20240101000000/a.pdf", "x");
                //Percorsi che non devono uscire dall'archivio ne' creare cartelle nascoste
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + "../fuori/a.pdf", "x");
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + ".in_corso_1/a.pdf", "x");
                voce(out, Backup_Restore.PREFISSO_DICHIARAZIONI + "x/y/z.pdf", "x");
            }
            int aggiunte;
            try (ZipFile z = new ZipFile(zip)) {
                aggiunte = Backup_Restore.EstraiDichiarazioni(z, radice);
            }
            assertEquals(1, aggiunte);
            assertEquals("nota locale", Dichiarazioni.Leggi(esistente.Cartella).Nota);
            assertTrue(new File(radice, "2023_RT_20240101000000/a.pdf").isFile());
            assertFalse(new File(radice.getParentFile(), "fuori").exists());
            assertFalse(new File(radice, ".in_corso_1").exists());
            assertFalse(new File(radice, "x").exists());
            assertEquals(2, Dichiarazioni.Elenco().size());
        } finally {
            zip.delete();
        }
    }

    private static void voce(ZipOutputStream out, String nome, String testo) throws IOException {
        out.putNextEntry(new ZipEntry(nome));
        out.write(testo.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
