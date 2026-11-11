package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RapportiISEE}: la tabella dei dati dei rapporti ISEE scritti dall'utente, la migrazione dell'identificativo
 * ISEE che stava sul periodo FIAT, le chiavi rimaste senza periodo.
 */
class RapportiISEETest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Principale.MappaCryptoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        Principale.MappaCryptoWallet.clear();
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM ISEE_RAPPORTI");
            st.execute("DELETE FROM WALLETGRUPPO");
        }
        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo("Wallet 01");
        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo("Wallet 02");
    }

    private static String[] periodoFiat(int prog, String inizio, String fine, String stato, String ident,
            String identIsee, String contoCorrente) {
        String[] p = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        Arrays.fill(p, "");
        p[Principale_GruppiWalletRW.COL_TIPO] = Principale_GruppiWalletRW.TIPO_FIAT;
        p[Principale_GruppiWalletRW.COL_PROGRESSIVO] = String.valueOf(prog);
        p[Principale_GruppiWalletRW.COL_DATA_INIZIO] = inizio;
        p[Principale_GruppiWalletRW.COL_DATA_FINE] = fine;
        p[Principale_GruppiWalletRW.COL_STATO_ESTERO] = stato;
        p[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = ident;
        p[Principale_GruppiWalletRW.COL_IDENT_ISEE] = identIsee;
        p[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE] = contoCorrente;
        return p;
    }

    private static void salva(String gruppo, String[]... righe) {
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi(gruppo, new ArrayList<>(List.of(righe))).isEmpty());
    }

    private static void assertVuoto(String s) {
        assertTrue(s == null || s.isBlank(), "il campo del periodo è svuotato, invece vale " + s);
    }

    private static Calcoli_ISEE.DatiUtente dati(String identificativo) {
        Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
        d.identificativo = identificativo;
        return d;
    }

    @Test
    void scriviLeggiECancella() {
        Calcoli_ISEE.DatiUtente d = dati(" IT60X0542811101000000123456 ");
        d.operatore = "e123456";
        d.apertura = LocalDate.of(2024, 3, 1);
        d.chiusura = LocalDate.of(2025, 6, 19);
        assertTrue(RapportiISEE.Scrivi("Wallet 01|01|P1", d));

        Calcoli_ISEE.DatiUtente letto = RapportiISEE.Leggi().get("Wallet 01|01|P1");
        assertEquals("IT60X0542811101000000123456", letto.identificativo);
        assertEquals("E123456", letto.operatore);
        assertEquals(LocalDate.of(2024, 3, 1), letto.apertura);
        assertEquals(LocalDate.of(2025, 6, 19), letto.chiusura);

        // tutti i campi vuoti: la riga sparisce e il rapporto torna ai dati dei periodi
        assertTrue(RapportiISEE.Scrivi("Wallet 01|01|P1", new Calcoli_ISEE.DatiUtente()));
        assertFalse(RapportiISEE.Leggi().containsKey("Wallet 01|01|P1"));
    }

    @Test
    void migrazione_dalPeriodoFiatAiRapporti01E99_eLaColonnaSiSvuota() {
        salva("Wallet 01", periodoFiat(1, "", "", "040", "123456", "IBAN-A", Principale_GruppiWalletRW.CONTO_CORRENTE_SI));

        assertEquals(2, RapportiISEE.MigraIdentificativiPeriodi());

        Map<String, Calcoli_ISEE.DatiUtente> m = RapportiISEE.Leggi();
        assertEquals("IBAN-A", m.get("Wallet 01|01|P1").identificativo);
        assertEquals("IBAN-A", m.get("Wallet 01|99|P1").identificativo);
        assertVuoto(Principale_GruppiWalletRW.caricaPeriodi("Wallet 01").get(0)[Principale_GruppiWalletRW.COL_IDENT_ISEE]);

        // rilanciata (ogni apertura del database) non scrive nulla, e un dato cancellato nel tab non ritorna
        RapportiISEE.Cancella("Wallet 01|99|P1");
        assertEquals(0, RapportiISEE.MigraIdentificativiPeriodi());
        assertFalse(RapportiISEE.Leggi().containsKey("Wallet 01|99|P1"));
    }

    @Test
    void migrazione_nonSovrascriveIlDatoDelTab_eOgniOperatoreHaIlSuo() {
        // Wallet 02: Irlanda (P1, P2 stesso operatore) poi Lussemburgo (P3)
        salva("Wallet 02",
                periodoFiat(1, "", "2023-12-31", "040", "675475", "VECCHIO", ""),
                periodoFiat(2, "2024-01-01", "2025-06-19", "040", "675475", "IRL", ""),
                periodoFiat(3, "2025-06-20", "", "092", "LU36476644", "LUX", ""));
        RapportiISEE.Scrivi("Wallet 02|01|P1", dati("DAL-TAB"));

        assertEquals(3, RapportiISEE.MigraIdentificativiPeriodi());

        Map<String, Calcoli_ISEE.DatiUtente> m = RapportiISEE.Leggi();
        assertEquals("DAL-TAB", m.get("Wallet 02|01|P1").identificativo, "il dato scritto nel tab resta");
        assertEquals("IRL", m.get("Wallet 02|99|P1").identificativo, "nel tratto vince il periodo più recente");
        assertEquals("LUX", m.get("Wallet 02|99|P3").identificativo);
        assertEquals("LUX", m.get("Wallet 02|01|P3").identificativo);
        for (String[] r : Principale_GruppiWalletRW.caricaPeriodi("Wallet 02")) {
            assertVuoto(r[Principale_GruppiWalletRW.COL_IDENT_ISEE]);
        }
    }

    @Test
    void chiaviOrfane_periodoCancellatoOChiaveMalformata() {
        salva("Wallet 01", periodoFiat(1, "", "", "040", "123456", "", ""));
        Set<String> orfane = RapportiISEE.Orfane(List.of(
                "Wallet 01|99|P1", "Wallet 01|01|P1", "Wallet 01|99|NESSUN_PERIODO",
                "Wallet 01|99|P9", "Wallet 01|07|P1", "Wallet 03|99|NESSUN_PERIODO", "Wallet 03|99|P1", "spazzatura"),
                Principale_GruppiWalletRW::caricaPeriodi);

        assertEquals(Set.of("Wallet 01|99|P9", "Wallet 01|07|P1", "Wallet 03|99|P1", "spazzatura"), orfane);
    }

    @Test
    void ilMotoreLeggeIDatiDallaTabella() {
        salva("Wallet 01", periodoFiat(1, "", "", "040", "123456", "", ""));
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = "202402011000_TEST_001_001_DC";
        m[1] = "2024-02-01 10:00";
        m[3] = "Kraken";
        m[11] = "BTC";
        m[12] = "Crypto";
        m[13] = "0.5";
        m[15] = "50";
        Principale.MappaCryptoWallet.put(m[0], m);
        Calcoli_ISEE.DatiUtente d = dati("KR-777");
        d.chiusura = LocalDate.of(2026, 1, 15);
        RapportiISEE.Scrivi("Wallet 01|99|P1", d);

        Calcoli_ISEE.Risultato ris = Calcoli_ISEE.calcola(2024,
                (mon, istante) -> new BigDecimal(mon.Qta).multiply(new BigDecimal("100")).toPlainString(),
                (valuta, importo, data) -> importo, RapportiISEE.datiUtente());

        assertEquals(1, ris.rapporti.size());
        Calcoli_ISEE.Rapporto r = ris.rapporti.get(0);
        assertEquals("Wallet 01|99|P1", r.chiave);
        assertEquals("Wallet 01 - KR-777", r.identificativo, "alias (qui il nome, senza alias) davanti all'identificativo");
        assertNull(r.dataFine, "chiuso dopo l'anno: la data non si dichiara");
    }
}
