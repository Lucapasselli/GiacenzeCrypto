package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip di {@code GRUPPO_PERIODO_RW}, l'unica tabella di configurazione del quadro RW dei gruppi
 * wallet rimasta dal 2026-09-09 (prima erano quattro : {@code EXCHANGE_ANAGRAFICA},
 * {@code EXCHANGE_PERIODO} e {@code GRUPPO_RIFERIMENTO_ESTERO} sono state eliminate e i loro dati
 * fiscali sono diventati colonne del rigo FIAT del periodo).
 *
 * <p>Su H2 temporaneo, come {@link DatabaseH2UpsertTest}. I punti delicati coperti : la chiave
 * PRIMARIA SINTETICA ({@code Gruppo_Tipo_Prog}), perché {@code U_ScriviRecord} accetta una sola
 * {@code primaryKeyColumn} — progressivi diversi dello stesso gruppo/tipo NON si devono sovrascrivere
 * a vicenda, e cancellare un singolo periodo o un intero gruppo deve colpire esattamente le righe
 * giuste — e il fatto che {@code IdentificativoISEE} passato a {@code null} lasci la colonna
 * <b>intatta</b>, che è ciò che rende quel campo "sempre a mano" anche sulle righe predefinite.</p>
 */
class DatabaseH2_RwConfigTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void puliscePartenza() {
        for (String g : new TreeSet<>(DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo(g);
        }
    }

    // ------------------------------------------------------------------
    //  DDL
    // ------------------------------------------------------------------

    @Test
    void ddl_gruppoPeriodoRWConLeColonneAttese() throws Exception {
        assertEquals(
                Set.of("GRUPPO_TIPO_PROG", "GRUPPO", "TIPORIGO", "PROGRESSIVO", "DATAINIZIO", "DATAFINE",
                        "VALOREINIZIALEMANUALE", "NOTAVALOREINIZIALE", "VALOREFINALEMANUALE", "NOTAVALOREFINALE",
                        "MODALITACALCOLOINIZIALE", "MODALITACALCOLOFINALE", "PAGABOLLOPERIODO", "ORIGINE",
                        "CHIAVEDEFAULT", "STATOESTERO", "IDENTIFICATIVOFISCALE", "NOTEFISCALI", "FONTEFISCALE",
                        "IDENTIFICATIVOISEE", "ECONTOCORRENTE"),
                colonne("GRUPPO_PERIODO_RW"));
    }

    @Test
    void ddl_leTreTabelleVecchieNonEsistonoPiu() throws Exception {
        assertTrue(colonne("EXCHANGE_ANAGRAFICA").isEmpty(), "EXCHANGE_ANAGRAFICA doveva essere eliminata");
        assertTrue(colonne("EXCHANGE_PERIODO").isEmpty(), "EXCHANGE_PERIODO doveva essere eliminata");
        assertTrue(colonne("GRUPPO_RIFERIMENTO_ESTERO").isEmpty(), "GRUPPO_RIFERIMENTO_ESTERO doveva essere eliminata");
    }

    private static Set<String> colonne(String tabella) throws Exception {
        Set<String> out = new TreeSet<>();
        try (var ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?")) {
            ps.setString(1, tabella);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1).toUpperCase());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    //  GRUPPO_PERIODO_RW — chiave sintetica Gruppo_Tipo_Prog
    // ------------------------------------------------------------------

    @Test
    void gruppoPeriodoRW_roundTrip_tuttiICampi() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 05", "FIAT", 1,
                "2024-01-01", "2024-06-30", "1500.00", "saldo da estratto conto",
                "0.00", "conto svuotato", "PRIMO_APPORTO", "ULTIMA_USCITA", "SI",
                "UTENTE", "chiave-x", "092", "LU36476644", "nota fiscale", "VIES", "E12345", "SI");

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 05");
        assertEquals(1, righe.size());
        assertArrayEquals(new String[]{
                "Wallet 05_FIAT_1", "Wallet 05", "FIAT", "1", "2024-01-01", "2024-06-30",
                "1500.00", "saldo da estratto conto", "0.00", "conto svuotato",
                "PRIMO_APPORTO", "ULTIMA_USCITA", "SI", "UTENTE", "chiave-x",
                "092", "LU36476644", "nota fiscale", "VIES", "E12345", "SI"}, righe.get(0));
    }

    @Test
    void gruppoPeriodoRW_identificativoIseeNull_lasciaLaColonnaIntatta() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 05", "FIAT", 1, null, null, null, null, null, null,
                "SOLO_RESIDUO", "SOLO_RESIDUO", null, "SISTEMA", "coinbase-mica",
                "092", "LU36476644", "prima nota", "prima fonte", "E-a-mano", "NO");

        // giro di reconcile dei predefiniti : riscrive tutto tranne l'identificativo ISEE
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 05", "FIAT", 1, "2025-06-20", null, null, null, null, null,
                "SOLO_RESIDUO", "SOLO_RESIDUO", null, "SISTEMA", "coinbase-mica",
                "092", "LU36476644", "nota aggiornata", "fonte aggiornata", null, "SI");

        String[] r = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 05").get(0);
        assertEquals("nota aggiornata", r[17]);
        assertEquals("E-a-mano", r[19], "l'identificativo ISEE non deve essere toccato dal reconcile");
        assertEquals("SI", r[20], "il campo \"è conto corrente\" segue il reconcile come gli altri dati fiscali");
    }

    @Test
    void gruppoPeriodoRW_progressiviDiversi_nonSiSovrascrivono() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "CRYPTO", 1, "2023-01-01", "2023-05-31",
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "CRYPTO", 2, "2023-09-01", null,
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 06", "FIAT", 1, "2023-01-01", null,
                null, null, null, null, null, null, null);

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 06");
        assertEquals(3, righe.size());
        // ordinamento: TipoRigo poi Progressivo
        assertEquals("Wallet 06_CRYPTO_1", righe.get(0)[0]);
        assertEquals("Wallet 06_CRYPTO_2", righe.get(1)[0]);
        assertEquals("Wallet 06_FIAT_1", righe.get(2)[0]);
        assertEquals("2023-05-31", righe.get(0)[5]);
        assertEquals("2023-09-01", righe.get(1)[4]);
    }

    @Test
    void gruppoPeriodoRW_stessoProgressivo_aggiornaInPlace() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 07", "FIAT", 1, "2024-01-01", null,
                null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 07", "FIAT", 1, "2024-02-15", "2024-12-31",
                "10.00", "nota", null, null, "SOMMA_APPORTI_GIORNO", "SOMMA_USCITE_GIORNO", "NO");

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 07");
        assertEquals(1, righe.size());
        assertEquals("2024-02-15", righe.get(0)[4]);
        assertEquals("2024-12-31", righe.get(0)[5]);
        assertEquals("10.00", righe.get(0)[6]);
        assertEquals("SOMMA_APPORTI_GIORNO", righe.get(0)[10]);
    }

    @Test
    void gruppoPeriodoRW_cancellaSingolo_lasciaGliAltri() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 08", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 08", "CRYPTO", 2, "2022-07-01", null, null, null, null, null, null, null, null);

        DatabaseH2.Pers_GruppoPeriodoRW_Cancella("Wallet 08", "CRYPTO", 1);

        List<String[]> righe = DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 08");
        assertEquals(1, righe.size());
        assertEquals("Wallet 08_CRYPTO_2", righe.get(0)[0]);
    }

    @Test
    void gruppoPeriodoRW_cancellaGruppo_rimuoveSoloQuelGruppo() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 09", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 09", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 10", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);

        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo("Wallet 09");

        assertTrue(DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 09").isEmpty());
        assertEquals(1, DatabaseH2.Pers_GruppoPeriodoRW_LeggiGruppo("Wallet 10").size());
    }

    @Test
    void gruppoPeriodoRW_leggiTabella_raggruppaPerGruppo() {
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 11", "CRYPTO", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 11", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);
        DatabaseH2.Pers_GruppoPeriodoRW_Scrivi("Wallet 12", "FIAT", 1, "2022-01-01", null, null, null, null, null, null, null, null);

        Map<String, List<String[]>> tab = DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella();
        assertEquals(2, tab.size());
        assertEquals(2, tab.get("Wallet 11").size());
        assertEquals(1, tab.get("Wallet 12").size());
    }
}
