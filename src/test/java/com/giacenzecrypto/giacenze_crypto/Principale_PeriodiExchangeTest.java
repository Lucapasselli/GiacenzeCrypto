package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Logica di {@link Principale_PeriodiExchange} : validazione strutturale dei periodi fiscali di un
 * exchange e risoluzione "quale periodo vale alla data X" (semantica dei periodi aperti). DB H2
 * temporaneo come {@link DatabaseH2_RwConfigTest}.
 */
class Principale_PeriodiExchangeTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void pulisce() {
        for (String id : new ArrayList<>(DatabaseH2.Pers_ExchangePeriodo_LeggiTabella().keySet())) {
            DatabaseH2.Pers_ExchangePeriodo_CancellaExchange(id);
        }
    }

    // riga GUI a 8 colonne
    private static String[] riga(String prog, String di, String df, String nome, String stato, String ident) {
        return new String[]{prog, di, df, nome, stato, ident, "", ""};
    }

    @SafeVarargs
    private static List<String[]> lista(String[]... rr) {
        return Arrays.asList(rr);
    }

    // ---------------- validaPeriodi ----------------

    @Test
    void validaPeriodi_righeMinime_nessunErrore() {
        assertTrue(Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "2021-01-31", "Payward Ltd", "031", ""),
                riga("2", "2021-02-01", "", "Payward Europe", "040", "711781"))).isEmpty());
    }

    @Test
    void validaPeriodi_progressivoDuplicato_segnalato() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "", "A", "040", ""),
                riga("1", "", "", "B", "092", "")));
        assertFalse(err.isEmpty());
        assertTrue(err.get(0).contains("progressivo"));
    }

    @Test
    void validaPeriodi_dataInizioDopoDataFine_segnalato() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "2022-12-31", "2022-01-01", "A", "040", "")));
        assertFalse(err.isEmpty());
    }

    @Test
    void validaPeriodi_dataMalformata_segnalata() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "31/12/2022", "", "A", "040", "")));
        assertFalse(err.isEmpty());
    }

    @Test
    void validaPeriodi_statoTroppoLungo_segnalato() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "", "A", "0400", "")));
        assertFalse(err.isEmpty());
    }

    @Test
    void validaPeriodi_identificativoOltre15Caratteri_segnalato() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "", "A", "040", "5493007WZ7IFULIL8G21"))); // LEI = 20 char
        assertFalse(err.isEmpty());
        assertTrue(err.get(0).contains("15"));
    }

    // ---------------- salva / carica ----------------

    @Test
    void salvaPeriodi_conErrori_nonScriveNulla() {
        List<String> err = Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "2022-12-31", "2022-01-01", "A", "040", "")));
        assertFalse(err.isEmpty());
        assertTrue(Principale_PeriodiExchange.caricaPeriodi("kraken").isEmpty());
    }

    @Test
    void salvaPeriodi_ok_roundTripViaCaricaPeriodi() {
        List<String> err = Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "", "2021-01-31", "Payward Ltd", "031", ""),
                riga("2", "2021-02-01", "", "Payward Europe Solutions Ltd", "040", "711781")));
        assertTrue(err.isEmpty(), err::toString);

        List<String[]> righe = Principale_PeriodiExchange.caricaPeriodi("kraken");
        assertEquals(2, righe.size());
        assertEquals("031", righe.get(0)[Principale_PeriodiExchange.COL_STATO]);
        assertEquals("711781", righe.get(1)[Principale_PeriodiExchange.COL_IDENT]);
    }

    // ---------------- risoluzione per data ----------------

    @Test
    void periodoAllaData_semanticaPeriodiAperti() {
        Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "", "2021-01-31", "Payward Ltd", "031", ""),
                riga("2", "2021-02-01", "", "Payward Europe", "040", "711781")));

        assertEquals("031", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2018, 5, 1)),
                "data inizio vuota = valido da sempre");
        assertEquals("031", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2021, 1, 31)),
                "estremo incluso");
        assertEquals("040", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2021, 2, 1)));
        assertEquals("040", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2030, 1, 1)),
                "data fine vuota = ancora in corso");
    }

    @Test
    void periodoAllaData_nessunPeriodoCopreLaData_ritornaNull() {
        Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "2021-01-01", "2021-12-31", "Payward Europe", "040", "711781")));

        assertNull(Principale_PeriodiExchange.periodoAllaData("kraken", LocalDate.of(2019, 1, 1)));
        assertEquals("", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2025, 1, 1)));
    }

    @Test
    void validaPeriodi_sovrapposizione_segnalata_eSalvaNonScriveNulla() {
        List<String[]> righe = lista(
                riga("1", "2020-01-01", "2025-12-31", "Vecchia entità", "040", "AAA"),
                riga("2", "2023-01-01", "", "Nuova entità", "092", "BBB"));
        List<String> err = Principale_PeriodiExchange.validaPeriodi(righe);
        assertEquals(1, err.size());
        assertTrue(err.get(0).toLowerCase().contains("sovrappong"), err.toString());
        assertFalse(Principale_PeriodiExchange.salvaPeriodi("kraken", righe).isEmpty());
        assertTrue(Principale_PeriodiExchange.caricaPeriodi("kraken").isEmpty(), "niente scritto sulla sovrapposizione");
    }

    @Test
    void validaPeriodi_periodoApertoNonConfliggeConDatati() {
        assertTrue(Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "", "Fallback", "040", ""),
                riga("2", "2023-01-01", "2023-12-31", "Datato", "092", ""))).isEmpty());
    }

    @Test
    void validaPeriodi_duePeriodiInteramenteAperti_segnalati() {
        List<String> err = Principale_PeriodiExchange.validaPeriodi(lista(
                riga("1", "", "", "A", "040", ""),
                riga("2", "", "", "B", "092", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).toLowerCase().contains("sovrappong"), err.toString());
    }

    @Test
    void avvisiPeriodi_buchiSegnalatiSenzaBloccare() {
        List<String[]> righe = lista(
                riga("1", "2020-01-01", "2021-06-30", "A", "040", ""),
                riga("2", "2022-01-01", "", "B", "092", ""));
        assertTrue(Principale_PeriodiExchange.validaPeriodi(righe).isEmpty(), "il buco non è un errore");
        List<String> avv = Principale_PeriodiExchange.avvisiPeriodi(righe);
        assertEquals(1, avv.size());
        assertTrue(avv.get(0).contains("2021-07-01") && avv.get(0).contains("2021-12-31"), avv.toString());
        assertTrue(Principale_PeriodiExchange.salvaPeriodi("kraken", righe).isEmpty(), "il salvataggio non è bloccato dal buco");
    }

    @Test
    void periodoAllaData_periodiSovrapposti_ilResolverRestaDeterministico() {
        // scritti direttamente (bypassando la validazione) : il risolutore deve comunque scegliere
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("kraken", 1, "2020-01-01", "2025-12-31", "Vecchia", "040", "AAA", "", "");
        DatabaseH2.Pers_ExchangePeriodo_Scrivi("kraken", 2, "2023-01-01", "", "Nuova", "092", "BBB", "", "");

        assertEquals("092", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2024, 6, 1)),
                "sulla sovrapposizione vince la DataInizio più recente");
        assertEquals("040", Principale_PeriodiExchange.statoEsteroAllaData("kraken", LocalDate.of(2021, 1, 1)));
    }

    @Test
    void periodoCorrente_preferisceLaFineAperta() {
        Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "", "2021-01-31", "Payward Ltd", "031", ""),
                riga("2", "2021-02-01", "", "Payward Europe", "040", "711781")));

        assertEquals("040", Principale_PeriodiExchange.statoEsteroCorrente("kraken"));
        assertEquals("711781", Principale_PeriodiExchange.identificativoCorrente("kraken"));
    }

    @Test
    void descriviCorrente_rifletteIlNumeroDiPeriodi() {
        assertEquals("nessun periodo", Principale_PeriodiExchange.descriviCorrente("kraken"));

        Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "2025-01-01", "", "Payward Europe", "040", "711781")));
        assertEquals("stato 040", Principale_PeriodiExchange.descriviCorrente("kraken"));

        Principale_PeriodiExchange.salvaPeriodi("kraken", lista(
                riga("1", "", "2020-12-31", "Payward Ltd", "031", ""),
                riga("2", "2021-01-01", "", "Payward Europe", "040", "711781")));
        assertEquals("più periodi, corrente 040", Principale_PeriodiExchange.descriviCorrente("kraken"));
    }
}
