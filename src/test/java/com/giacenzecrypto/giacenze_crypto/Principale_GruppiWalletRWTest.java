package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
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
 * Logica di {@link Principale_GruppiWalletRW} : validazione strutturale dei periodi di
 * detenzione e salvataggio/rilettura del riferimento estero del gruppo. DB H2 temporaneo
 * come {@link DatabaseH2_RwConfigTest}.
 */
class Principale_GruppiWalletRWTest {

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
        for (String g : new ArrayList<>(DatabaseH2.Pers_GruppoRiferimento_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoRiferimento_Cancella(g);
        }
        for (String g : new ArrayList<>(DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo(g);
        }
    }

    // riga GUI a 10 colonne
    private static String[] riga(String tipo, String prog, String di, String df,
            String vi, String ni, String vf, String nf, String mi, String mf) {
        return new String[]{tipo, prog, di, df, vi, ni, vf, nf, mi, mf};
    }

    // List.of(String[]...) verrebbe inferito come List<String>: helper esplicito
    @SafeVarargs
    private static List<String[]> lista(String[]... rr) {
        return Arrays.asList(rr);
    }

    // ---------------- validaPeriodi ----------------

    @Test
    void validaPeriodi_righeMinimeValide_nessunErrore() {
        List<String[]> r = lista(
                riga("FIAT", "1", "2024-01-01", "2024-12-31", "", "", "", "", "", ""),
                riga("CRYPTO", "1", "", "", "", "", "", "", "", ""));
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(r).isEmpty(),
                () -> Principale_GruppiWalletRW.validaPeriodi(r).toString());
    }

    @Test
    void validaPeriodi_tipoNonValido_segnalato() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("ALTRO", "1", "", "", "", "", "", "", "", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("tipo"));
    }

    @Test
    void validaPeriodi_progressivoDuplicatoStessoTipo_segnalato_maCryptoEFiatIndipendenti() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("CRYPTO", "1", "", "", "", "", "", "", "", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("progressivo 1"));
    }

    @Test
    void validaPeriodi_dataFormatoErrato_segnalato() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "01/01/2024", "", "", "", "", "", "", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("yyyy-MM-dd"));
    }

    @Test
    void validaPeriodi_inizioDopoFine_segnalato() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "2024-06-01", "2024-01-01", "", "", "", "", "", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("successiva"));
    }

    @Test
    void validaPeriodi_modalitaSconosciuta_segnalata() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "", "", "", "", "", "", "PIPPO", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("iniziale"));
    }

    @Test
    void validaPeriodi_modalitaAmmesseAccettate() {
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("CRYPTO", "1", "", "", "", "", "", "",
                        Principale_GruppiWalletRW.MOD_INIZIALE_SOMMA_APPORTI,
                        Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA))).isEmpty());
    }

    @Test
    void prossimoProgressivo_contaSoloIlTipoRichiesto() {
        List<String[]> r = lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("FIAT", "3", "", "", "", "", "", "", "", ""),
                riga("CRYPTO", "1", "", "", "", "", "", "", "", ""));
        assertEquals(4, Principale_GruppiWalletRW.prossimoProgressivo(r, "FIAT"));
        assertEquals(2, Principale_GruppiWalletRW.prossimoProgressivo(r, "CRYPTO"));
        assertEquals(1, Principale_GruppiWalletRW.prossimoProgressivo(new ArrayList<>(), "FIAT"));
    }

    // ---------------- riferimento estero ----------------

    @Test
    void salvaRiferimento_statoSenzaCodice_erroreENonScrive() {
        List<String> err = Principale_GruppiWalletRW.salvaRiferimento(
                "Wallet 01", Principale_GruppiWalletRW.RIFERIMENTO_STATO, "", "", null);
        assertFalse(err.isEmpty());
        assertNull(DatabaseH2.Pers_GruppoRiferimento_Leggi("Wallet 01")[1]);
    }

    @Test
    void salvaRiferimento_exchangeInesistente_errore() {
        List<String> err = Principale_GruppiWalletRW.salvaRiferimento(
                "Wallet 01", Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE, null, null, "nonesiste");
        assertFalse(err.isEmpty());
        assertTrue(err.get(0).contains("anagrafica"));
    }

    @Test
    void salvaRiferimento_statoOk_riletturaEDescrizione() {
        List<String> err = Principale_GruppiWalletRW.salvaRiferimento(
                "Wallet 02", Principale_GruppiWalletRW.RIFERIMENTO_STATO, "092", "LU12345678", null);
        assertTrue(err.isEmpty(), err::toString);

        assertEquals("092", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 02"));
        assertEquals("LU12345678", Principale_GruppiWalletRW.identificativoFiscaleEffettivo("Wallet 02"));
        assertTrue(Principale_GruppiWalletRW.descriviRiferimento("Wallet 02").contains("092"));
    }

    @Test
    void salvaRiferimento_exchangeOk_statoEPivaDerivatiDallAnagrafica() {
        DatabaseH2.Pers_ExchangeAnagrafica_Scrivi("coinbase", "Coinbase", "092", "LU99999999",
                null, null, null);

        List<String> err = Principale_GruppiWalletRW.salvaRiferimento(
                "Wallet 03", Principale_GruppiWalletRW.RIFERIMENTO_EXCHANGE, null, null, "coinbase");
        assertTrue(err.isEmpty(), err::toString);

        assertEquals("092", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 03"));
        assertEquals("LU99999999", Principale_GruppiWalletRW.identificativoFiscaleEffettivo("Wallet 03"));
        assertTrue(Principale_GruppiWalletRW.descriviRiferimento("Wallet 03").contains("Coinbase"));
    }

    @Test
    void descriviRiferimento_gruppoSenzaConfig_nonImpostato() {
        assertEquals("— non impostato", Principale_GruppiWalletRW.descriviRiferimento("Wallet 40"));
    }

    // ---------------- periodi : salva / carica ----------------

    @Test
    void salvaPeriodi_conErrori_nonScriveNulla() {
        List<String> err = Principale_GruppiWalletRW.salvaPeriodi("Wallet 05", lista(
                riga("FIAT", "1", "2024-12-31", "2024-01-01", "", "", "", "", "", "")));
        assertFalse(err.isEmpty());
        assertTrue(Principale_GruppiWalletRW.caricaPeriodi("Wallet 05").isEmpty());
    }

    @Test
    void salvaPeriodi_ok_roundTripViaCaricaPeriodi() {
        List<String[]> in = lista(
                riga("CRYPTO", "1", "2023-01-01", "2023-05-31", "", "", "", "", "", ""),
                riga("CRYPTO", "2", "2023-09-01", "", "", "", "", "", "", ""),
                riga("FIAT", "1", "2023-01-01", "2023-12-31", "1500.00", "estratto conto",
                        "0.00", "conto svuotato",
                        Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO,
                        Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA));
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 06", in).isEmpty());

        List<String[]> out = Principale_GruppiWalletRW.caricaPeriodi("Wallet 06");
        assertEquals(3, out.size());
        // ordinamento tipo poi progressivo
        assertArrayEquals(new String[]{"CRYPTO", "1", "2023-01-01", "2023-05-31", null, null, null, null, null, null}, out.get(0));
        assertArrayEquals(new String[]{"CRYPTO", "2", "2023-09-01", null, null, null, null, null, null, null}, out.get(1));
        assertArrayEquals(new String[]{"FIAT", "1", "2023-01-01", "2023-12-31", "1500.00", "estratto conto",
                "0.00", "conto svuotato",
                Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO,
                Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA}, out.get(2));
    }

    @Test
    void salvaPeriodi_riscrittura_sostituisceIProgressiviPrecedenti() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 07", lista(
                riga("FIAT", "1", "2022-01-01", null, "", "", "", "", "", ""),
                riga("FIAT", "2", "2022-06-01", null, "", "", "", "", "", "")));
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 07", lista(
                riga("FIAT", "1", "2023-01-01", null, "", "", "", "", "", "")));

        List<String[]> out = Principale_GruppiWalletRW.caricaPeriodi("Wallet 07");
        assertEquals(1, out.size());
        assertEquals("2023-01-01", out.get(0)[Principale_GruppiWalletRW.COL_DATA_INIZIO]);
    }
}
