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
        for (String g : new ArrayList<>(DatabaseH2.Pers_GruppoPeriodoRW_LeggiTabella().keySet())) {
            DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo(g);
        }
        for (String w : new ArrayList<>(DatabaseH2.Pers_GruppoWallet_LeggiTuttiIWallet())) {
            DatabaseH2.Pers_GruppoWallet_Cancella(w);
        }
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_AUTOGRUPPO_FATTI", "");
        DatabaseH2.Pers_Opzioni_Scrivi("EXCHANGE_AUTOGRUPPO_INIZIALIZZATO", "");
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
    }

    // riga GUI a 11 colonne (bollo lasciato vuoto : trattato come "non impostato")
    private static String[] riga(String tipo, String prog, String di, String df,
            String vi, String ni, String vf, String nf, String mi, String mf) {
        return new String[]{tipo, prog, di, df, vi, ni, vf, nf, mi, mf, ""};
    }

    // idem, con il valore del bollo del periodo (SI / NO)
    private static String[] rigaB(String tipo, String prog, String di, String df, String bollo) {
        return new String[]{tipo, prog, di, df, "", "", "", "", "", "", bollo};
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

    // ---------------- validaPeriodi : sovrapposizioni / buchi ----------------

    @Test
    void validaPeriodi_periodiStessoTipoSovrapposti_segnalati() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "2024-01-01", "2024-06-30", "", "", "", "", "", ""),
                riga("FIAT", "2", "2024-06-01", "2024-12-31", "", "", "", "", "", "")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).toLowerCase().contains("sovrappong"), err.toString());
    }

    @Test
    void validaPeriodi_sovrapposizioneSoloEntroLoStessoTipo() {
        // FIAT e CRYPTO sono righi distinti : possono coprire lo stesso intervallo
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "2024-01-01", "2024-12-31", "", "", "", "", "", ""),
                riga("CRYPTO", "1", "2024-01-01", "2024-12-31", "", "", "", "", "", ""))).isEmpty());
    }

    @Test
    void validaPeriodi_periodoApertoNonConfliggeConDatati() {
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("FIAT", "2", "2024-03-01", "2024-08-31", "", "", "", "", "", ""))).isEmpty());
    }

    @Test
    void validaPeriodi_duePeriodiInteramenteApertiStessoTipo_avvisoNonErrore() {
        // il flusso "bollo per periodo" crea periodi CRYPTO senza date : non è un errore bloccante
        List<String[]> righe = lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("FIAT", "2", "", "", "", "", "", "", "", ""));
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(righe).isEmpty());
        List<String> avv = Principale_GruppiWalletRW.avvisiPeriodi(righe);
        assertEquals(1, avv.size());
        assertTrue(avv.get(0).contains("senza date"), avv.toString());
    }

    @Test
    void avvisiPeriodi_buchiFraDatatiSegnalatiSenzaBloccare() {
        List<String[]> righe = lista(
                riga("FIAT", "1", "2024-01-01", "2024-03-31", "", "", "", "", "", ""),
                riga("FIAT", "2", "2024-07-01", "2024-12-31", "", "", "", "", "", ""));
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(righe).isEmpty(), "il buco non è un errore");
        List<String> avv = Principale_GruppiWalletRW.avvisiPeriodi(righe);
        assertEquals(1, avv.size());
        assertTrue(avv.get(0).contains("FIAT") && avv.get(0).contains("2024-04-01") && avv.get(0).contains("2024-06-30"),
                avv.toString());
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 20", righe).isEmpty(), "salvataggio non bloccato dal buco");
    }

    @Test
    void avvisiPeriodi_periodoSenzaDateAccantoADatati_nonCopreIBuchi() {
        // Da quando l'inizio mancante si deduce (finestreEffettive), una riga senza date NON è più un
        // "fallback che copre tutto" : parte dal giorno dopo l'ultima fine, qui il 2025-01-01. Il buco
        // fra i due periodi datati resta quindi segnalato.
        List<String> avv = Principale_GruppiWalletRW.avvisiPeriodi(lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""),
                riga("FIAT", "2", "2024-01-01", "2024-03-31", "", "", "", "", "", ""),
                riga("FIAT", "3", "2024-07-01", "2024-12-31", "", "", "", "", "", "")));
        assertEquals(1, avv.size(), avv.toString());
        assertTrue(avv.get(0).contains("2024-04-01"), avv.toString());
    }

    @Test
    void avvisiPeriodi_unicoPeriodoSenzaDate_copreTutto() {
        assertTrue(Principale_GruppiWalletRW.avvisiPeriodi(lista(
                riga("FIAT", "1", "", "", "", "", "", "", "", ""))).isEmpty());
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

    // ---------------- dati fiscali sul rigo FIAT ----------------

    /** riga FIAT con i dati fiscali (Stato estero + identificativo). */
    private static String[] rigaF(String prog, String di, String df, String stato, String ident) {
        String[] r = riga("FIAT", prog, di, df, "", "", "", "", "", "");
        String[] full = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(full, "");
        System.arraycopy(r, 0, full, 0, r.length);
        full[Principale_GruppiWalletRW.COL_STATO_ESTERO] = stato;
        full[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = ident;
        return full;
    }

    @Test
    void statoEsteroEffettivo_senzaPeriodiFiat_vuoto() {
        assertEquals("", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 40"));
        assertEquals("", Principale_GruppiWalletRW.identificativoFiscaleEffettivo("Wallet 40"));
        assertEquals("— non impostato", Principale_GruppiWalletRW.descriviRiferimento("Wallet 40"));
    }

    @Test
    void statoEsteroEffettivo_periodoFiatAperto_valeSempre() {
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 02",
                lista(rigaF("1", "", "", "092", "LU12345678"))).isEmpty());

        assertEquals("092", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 02"));
        assertEquals("LU12345678", Principale_GruppiWalletRW.identificativoFiscaleEffettivo("Wallet 02"));
        assertTrue(Principale_GruppiWalletRW.descriviRiferimento("Wallet 02").contains("092"));
    }

    @Test
    void statoEsteroEffettivo_duePeriodiFiat_risolvePerData() {
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 04", lista(
                rigaF("1", "", "2021-01-31", "031", ""),
                rigaF("2", "2021-02-01", "", "040", "711781"))).isEmpty());

        assertEquals("031", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 04", java.time.LocalDate.of(2020, 6, 1)));
        assertEquals("040", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 04", java.time.LocalDate.of(2022, 6, 1)));
        assertEquals("040", Principale_GruppiWalletRW.statoEsteroEffettivo("Wallet 04")); // periodo corrente
        assertEquals("711781", Principale_GruppiWalletRW.identificativoFiscaleEffettivo("Wallet 04"));
    }

    @Test
    void datiFiscali_soloSulRigoFiat_ilRigoCryptoNonLiMemorizza() {
        String[] crypto = rigaF("1", "", "", "092", "LU12345678");
        crypto[Principale_GruppiWalletRW.COL_TIPO] = "CRYPTO";
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 05", lista(crypto)).isEmpty());

        String[] out = Principale_GruppiWalletRW.caricaPeriodi("Wallet 05").get(0);
        assertNull(out[Principale_GruppiWalletRW.COL_STATO_ESTERO]);
        assertNull(out[Principale_GruppiWalletRW.COL_IDENT_FISCALE]);
    }

    @Test
    void validaPeriodi_identificativiTroppoLunghi_segnalati() {
        String[] r = rigaF("1", "", "", "092", "0123456789ABCDEF"); // 16 caratteri
        assertFalse(Principale_GruppiWalletRW.validaPeriodi(lista(r)).isEmpty());

        String[] r2 = rigaF("1", "", "", "0921", "");               // stato di 4 caratteri
        assertFalse(Principale_GruppiWalletRW.validaPeriodi(lista(r2)).isEmpty());

        String[] r3 = rigaF("1", "", "", "092", "");
        r3[Principale_GruppiWalletRW.COL_IDENT_ISEE] = "0123456789ABCDEF";
        assertFalse(Principale_GruppiWalletRW.validaPeriodi(lista(r3)).isEmpty());
    }

    // ---------------- finestre effettive (date dedotte) ----------------

    @Test
    void finestreEffettive_inizioVuoto_partOndaFineDelPeriodoPrecedente() {
        List<Principale_GruppiWalletRW.Finestra> f = Principale_GruppiWalletRW.finestreEffettive(lista(
                riga("FIAT", "1", "", "2025-06-19", "", "", "", "", "", ""),
                riga("FIAT", "2", "", "", "", "", "", "", "", "")), "FIAT");

        assertEquals(2, f.size());
        // la riga 1 non ha nessuna fine precedente a cui agganciarsi : parte dal primo movimento
        assertNull(f.get(0).inizio);
        assertEquals(java.time.LocalDate.of(2025, 6, 19), f.get(0).fine);
        // la riga 2 non ha date : l'inizio si deduce dalla fine della riga 1
        assertEquals(java.time.LocalDate.of(2025, 6, 20), f.get(1).inizio);
        assertTrue(f.get(1).inizioDedotto);
        assertNull(f.get(1).fine);
    }

    @Test
    void finestreEffettive_inizioVuotoConFinePropria_prendeLaFinePrecedentePiuVicina() {
        List<Principale_GruppiWalletRW.Finestra> f = Principale_GruppiWalletRW.finestreEffettive(lista(
                riga("FIAT", "1", "", "2020-12-31", "", "", "", "", "", ""),
                riga("FIAT", "2", "", "2023-12-31", "", "", "", "", "", ""),
                riga("FIAT", "3", "", "2025-12-31", "", "", "", "", "", "")), "FIAT");

        assertEquals(3, f.size());
        assertNull(f.get(0).inizio);
        assertEquals(java.time.LocalDate.of(2021, 1, 1), f.get(1).inizio);
        assertEquals(java.time.LocalDate.of(2024, 1, 1), f.get(2).inizio);
    }

    @Test
    void finestreEffettive_soloIlTipoRichiesto_eLaFineDiUnAltroTipoNonInfluisce() {
        List<Principale_GruppiWalletRW.Finestra> f = Principale_GruppiWalletRW.finestreEffettive(lista(
                riga("CRYPTO", "1", "", "2025-06-19", "", "", "", "", "", ""),
                riga("FIAT", "1", "", "", "", "", "", "", "", "")), "FIAT");

        assertEquals(1, f.size());
        assertNull(f.get(0).inizio, "la fine di un rigo CRYPTO non deve dedurre l'inizio di un rigo FIAT");
    }

    @Test
    void validaPeriodi_coppiaDaCambioStato_nonSiSovrappone() {
        // (vuoto -> 19/06) + (senza date, dedotto dal 20/06) : contigui, non sovrapposti
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(lista(
                riga("FIAT", "1", "", "2025-06-19", "", "", "", "", "", ""),
                riga("FIAT", "2", "", "", "", "", "", "", "", ""))).isEmpty());
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
        // ordinamento tipo poi progressivo ; colonne in coda = bollo / Origine / ChiaveDefault / dati fiscali (non impostati -> null)
        assertArrayEquals(new String[]{"CRYPTO", "1", "2023-01-01", "2023-05-31", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null}, out.get(0));
        assertArrayEquals(new String[]{"CRYPTO", "2", "2023-09-01", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null}, out.get(1));
        assertArrayEquals(new String[]{"FIAT", "1", "2023-01-01", "2023-12-31", "1500.00", "estratto conto",
                "0.00", "conto svuotato",
                Principale_GruppiWalletRW.MOD_INIZIALE_PRIMO_APPORTO,
                Principale_GruppiWalletRW.MOD_FINALE_ULTIMA_USCITA,
                null, null, null, null, null, null, null, null}, out.get(2));
    }

    @Test
    void salvaPeriodi_riscrittura_sostituisceIProgressiviPrecedenti() {
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 07", lista(
                riga("FIAT", "1", "2022-01-01", "2022-05-31", "", "", "", "", "", ""),
                riga("FIAT", "2", "2022-06-01", null, "", "", "", "", "", ""))).isEmpty());
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 07", lista(
                riga("FIAT", "1", "2023-01-01", null, "", "", "", "", "", "")));

        List<String[]> out = Principale_GruppiWalletRW.caricaPeriodi("Wallet 07");
        assertEquals(1, out.size());
        assertEquals("2023-01-01", out.get(0)[Principale_GruppiWalletRW.COL_DATA_INIZIO]);
    }

    // ---------------- bollo per periodo ----------------

    @Test
    void validaPeriodi_bolloNonValidoSuRigoCrypto_segnalato() {
        List<String> err = Principale_GruppiWalletRW.validaPeriodi(lista(
                rigaB("CRYPTO", "1", "", "", "FORSE")));
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("bollo"));
    }

    @Test
    void validaPeriodi_bolloSuRigoFiat_ignorato() {
        // sul rigo FIAT il bollo non è dovuto : un valore anomalo non è un errore, viene scartato
        assertTrue(Principale_GruppiWalletRW.validaPeriodi(lista(
                rigaB("FIAT", "1", "", "", "FORSE"))).isEmpty());
    }

    @Test
    void salvaPeriodi_bolloDelRigoFiat_nonVieneMemorizzato() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 25", lista(
                rigaB("CRYPTO", "1", "", "", "SI"),
                rigaB("FIAT", "1", "", "", "SI")));

        for (String[] r : Principale_GruppiWalletRW.caricaPeriodi("Wallet 25")) {
            if ("FIAT".equals(r[Principale_GruppiWalletRW.COL_TIPO])) {
                assertNull(r[Principale_GruppiWalletRW.COL_BOLLO], "il bollo del rigo FIAT non va salvato");
            } else {
                assertEquals("SI", r[Principale_GruppiWalletRW.COL_BOLLO]);
            }
        }
    }

    @Test
    void statoBolloPeriodi_consideraSoloIRighiCrypto() {
        assertEquals(Principale_GruppiWalletRW.BOLLO_STATO_NESSUNO,
                Principale_GruppiWalletRW.statoBolloPeriodi("Wallet 20"));

        // solo periodi FIAT -> NESSUNO (il bollo lì non esiste)
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 21", lista(
                rigaB("FIAT", "1", "", "", "SI")));
        assertEquals(Principale_GruppiWalletRW.BOLLO_STATO_NESSUNO,
                Principale_GruppiWalletRW.statoBolloPeriodi("Wallet 21"));

        // CRYPTO uniformi (+ un FIAT ignorato) -> TUTTI_SI
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 22", lista(
                rigaB("CRYPTO", "1", "", "", "SI"),
                rigaB("FIAT", "1", "", "", "NO")));
        assertEquals(Principale_GruppiWalletRW.BOLLO_STATO_TUTTI_SI,
                Principale_GruppiWalletRW.statoBolloPeriodi("Wallet 22"));

        // due CRYPTO discordi -> MISTI
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 23", lista(
                rigaB("CRYPTO", "1", "", "", "SI"),
                rigaB("CRYPTO", "2", "", "", "NO")));
        assertEquals(Principale_GruppiWalletRW.BOLLO_STATO_MISTI,
                Principale_GruppiWalletRW.statoBolloPeriodi("Wallet 23"));
        assertTrue(Principale_GruppiWalletRW.periodiBolloIncoerenti("Wallet 23"));
    }

    @Test
    void bolloDefaultGruppo_ereditaIlFlagPerGruppo() {
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 26", "conto estero", true);
        assertEquals(Principale_GruppiWalletRW.BOLLO_SI, Principale_GruppiWalletRW.bolloDefaultGruppo("Wallet 26"));
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 26", "conto estero", false);
        assertEquals(Principale_GruppiWalletRW.BOLLO_NO, Principale_GruppiWalletRW.bolloDefaultGruppo("Wallet 26"));
    }

    @Test
    void propagaBolloAiPeriodi_riscriveSoloIRighiCrypto() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 27", lista(
                rigaB("CRYPTO", "1", "", "", "SI"),
                rigaB("CRYPTO", "2", "", "", "SI"),
                rigaB("FIAT", "1", "", "", "")));

        Principale_GruppiWalletRW.propagaBolloAiPeriodi("Wallet 27", false);

        for (String[] r : Principale_GruppiWalletRW.caricaPeriodi("Wallet 27")) {
            if ("CRYPTO".equals(r[Principale_GruppiWalletRW.COL_TIPO])) {
                assertEquals("NO", r[Principale_GruppiWalletRW.COL_BOLLO]);
            } else {
                assertNull(r[Principale_GruppiWalletRW.COL_BOLLO]);
            }
        }
        assertEquals(Principale_GruppiWalletRW.BOLLO_STATO_TUTTI_NO,
                Principale_GruppiWalletRW.statoBolloPeriodi("Wallet 27"));
    }

    // ---------------- gruppi wallet preconfigurati per exchange ----------------

    @Test
    void etichettaGruppo_mostraLAliasQuandoDiversoDalNomeGruppo() {
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 101", "Binance", false);
        assertEquals("Wallet 101 — Binance", Principale_GruppiWalletRW.etichettaGruppo("Wallet 101"));
        // alias uguale al nome del gruppo (caso Wallet 01..40) -> nessun suffisso
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 05", "Wallet 05", false);
        assertEquals("Wallet 05", Principale_GruppiWalletRW.etichettaGruppo("Wallet 05"));
    }

    @Test
    void ordineGruppo_numericoNonLessicografico() {
        List<String> l = new ArrayList<>(Arrays.asList(
                "Wallet 101", "Wallet 2", "Wallet 10", "Wallet 114", "Wallet 11", "Wallet 40", "Wallet 99"));
        l.sort(Principale_GruppiWalletRW.ORDINE_GRUPPO);
        assertEquals(Arrays.asList(
                "Wallet 2", "Wallet 10", "Wallet 11", "Wallet 40", "Wallet 99", "Wallet 101", "Wallet 114"), l);
    }

    @Test
    void ordineGruppo_nomiNonStandardInCoda() {
        List<String> l = new ArrayList<>(Arrays.asList("Zeta", "Wallet 5", "Alfa", "Wallet 100"));
        l.sort(Principale_GruppiWalletRW.ORDINE_GRUPPO);
        assertEquals(Arrays.asList("Wallet 5", "Wallet 100", "Alfa", "Zeta"), l);
    }

    @Test
    void exchangeIdDaSorgente_riconosceNomiEVarianti() {
        assertEquals("cryptocom", Principale_GruppiWalletRW.exchangeIdDaSorgente("Crypto.com"));
        assertEquals("cryptocom", Principale_GruppiWalletRW.exchangeIdDaSorgente("crypto.com app"));
        assertEquals("okx", Principale_GruppiWalletRW.exchangeIdDaSorgente("OKX"));
        assertEquals("okx", Principale_GruppiWalletRW.exchangeIdDaSorgente("okex"));
        assertEquals("coinbase", Principale_GruppiWalletRW.exchangeIdDaSorgente("Coinbase"));
        // "Coinbase Pro" resta un wallet a sé
        assertEquals("", Principale_GruppiWalletRW.exchangeIdDaSorgente("Coinbase Pro"));
        assertEquals("", Principale_GruppiWalletRW.exchangeIdDaSorgente("Un exchange qualsiasi"));
    }

    @Test
    void gruppoPreconfigurato_indiceStabileDaEXCHANGE_NOTI() {
        assertEquals("Wallet 101", Principale_GruppiWalletRW.gruppoPreconfigurato("binance"));
        assertEquals("Wallet 102", Principale_GruppiWalletRW.gruppoPreconfigurato("coinbase"));
        assertEquals("Wallet 104", Principale_GruppiWalletRW.gruppoPreconfigurato("cryptocom"));
        assertEquals("Wallet 114", Principale_GruppiWalletRW.gruppoPreconfigurato("bitfinex"));
        assertEquals("", Principale_GruppiWalletRW.gruppoPreconfigurato("sconosciuto"));
    }

    @Test
    void autoAssocia_primoIncontro_associaAlGruppoPreconfigurato() {
        boolean scritto = Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(
                java.util.List.of("Coinbase", "Crypto.com"));
        assertTrue(scritto);
        assertEquals("Wallet 102", DatabaseH2.Pers_GruppoWallet_Leggi("Coinbase", false));
        assertEquals("Wallet 104", DatabaseH2.Pers_GruppoWallet_Leggi("Crypto.com", false));
    }

    @Test
    void autoAssocia_nonToccaUnaSceltaDellUtente() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 07");

        Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("Kraken"));

        assertEquals("Wallet 07", DatabaseH2.Pers_GruppoWallet_Leggi("Kraken", false));
    }

    @Test
    void autoAssocia_daWallet99_procede_maUnaVoltaSola() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("OKX", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);

        assertTrue(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("OKX")));
        assertEquals("Wallet 105", DatabaseH2.Pers_GruppoWallet_Leggi("OKX", false));

        // l'utente lo rimette in "da classificare" : la seconda passata NON lo riprende (marcatore)
        DatabaseH2.Pers_GruppoWallet_Scrivi("OKX", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);
        DatabaseH2.Mappa_Wallet_Gruppo.clear();
        assertFalse(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("OKX")));
        assertEquals(Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI,
                DatabaseH2.Pers_GruppoWallet_Leggi("OKX", false));
    }

    @Test
    void exchangeIdDaSorgente_coerenteConINomiExchangeDeiConfigImport() throws Exception {
        // "nomeExchange" dei config diventa il campo [3] del movimento: se un config introduce una
        // grafia nuova che smette di matchare, questo test lo segnala. Grafie tenute apposta come
        // wallet a sé (non associate a un gruppo preconfigurato):
        java.util.Set<String> separatiDiProposito = java.util.Set.of("coinbase pro", "gdax");

        java.nio.file.Path dir = java.nio.file.Path.of("config", "import");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isDirectory(dir),
                "config/import non presente: test saltato");
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"nomeExchange\"\\s*:\\s*\"([^\"]*)\"");
        int visti = 0;
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path f : (Iterable<java.nio.file.Path>) files.filter(x -> x.toString().endsWith(".json"))::iterator) {
                java.util.regex.Matcher m = p.matcher(java.nio.file.Files.readString(f));
                while (m.find()) {
                    String nome = m.group(1).trim();
                    if (nome.isEmpty()) {
                        continue;
                    }
                    visti++;
                    String id = Principale_GruppiWalletRW.exchangeIdDaSorgente(nome);
                    if (separatiDiProposito.contains(nome.toLowerCase())) {
                        assertEquals("", id, nome + " deve restare un wallet a sé");
                    } else {
                        assertFalse(id.isEmpty(),
                                "nomeExchange \"" + nome + "\" (" + f.getFileName() + ") non è più riconosciuto da exchangeIdDaSorgente");
                        assertFalse(Principale_GruppiWalletRW.gruppoPreconfigurato(id).isEmpty(),
                                "l'id \"" + id + "\" non ha un gruppo preconfigurato");
                    }
                }
            }
        }
        assertTrue(visti > 0, "nessun nomeExchange trovato nei config: regex o percorso da rivedere");
    }

    @Test
    void autoAssocia_nomeNonRiconosciuto_ignorato_eNonInquinaIlMarcatore() {
        assertFalse(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(
                java.util.List.of("Un CSV custom", "Coinbase Pro")));
        assertNull(DatabaseH2.Pers_GruppoWallet_Leggi("Un CSV custom", false));
        assertEquals("", nz(DatabaseH2.Pers_Opzioni_Leggi("EXCHANGE_AUTOGRUPPO_FATTI")));
    }

    @Test
    void congela_archivioEsistente_ilPregressoInWallet99NonVieneSpostato() {
        // pregresso: Binance già "da classificare" (come lo scrive il primo calcolo)
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);

        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();
        assertFalse(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("Binance")),
                "il nome è nel marcatore congelato: niente da spostare");

        assertEquals(Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI,
                DatabaseH2.Pers_GruppoWallet_Leggi("Binance", false),
                "un archivio esistente non deve essere ri-suddiviso automaticamente");
    }

    @Test
    void congela_exchangeNuovoDopoLAggiornamento_vieneComunqueAssociato() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();

        // Kraken non era nell'archivio al momento del congelamento: compare adesso
        assertTrue(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("Binance", "Kraken")));
        assertEquals("Wallet 103", DatabaseH2.Pers_GruppoWallet_Leggi("Kraken", false));
        assertEquals(Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI,
                DatabaseH2.Pers_GruppoWallet_Leggi("Binance", false));
    }

    @Test
    void congela_archivioNuovo_nonCongelaNulla_eLAutoAssocResta() {
        // WALLETGRUPPO vuoto: nessun pregresso
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();
        assertTrue(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("Coinbase")));
        assertEquals("Wallet 102", DatabaseH2.Pers_GruppoWallet_Leggi("Coinbase", false));
    }

    @Test
    void congela_giraUnaVoltaSola() {
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();
        assertEquals("SI", DatabaseH2.Pers_Opzioni_Leggi("EXCHANGE_AUTOGRUPPO_INIZIALIZZATO"));
        // un wallet aggiunto dopo il primo congelamento non viene congelato da una seconda chiamata
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();
        assertTrue(Principale_GruppiWalletRW.autoAssociaGruppiPreconfigurati(java.util.List.of("Binance")));
        assertEquals("Wallet 101", DatabaseH2.Pers_GruppoWallet_Leggi("Binance", false));
    }

    @Test
    void raggruppaForzato_spostaAncheIlPregressoCongelato_maNonLeScelteUtente() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", Principale_GruppiWalletRW.GRUPPO_NON_CLASSIFICATI);
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 07"); // scelta dell'utente
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();

        java.util.List<String[]> spostati = Principale_GruppiWalletRW.raggruppaWalletExchangeNoti(
                java.util.List.of("Binance", "Kraken"));

        assertEquals(1, spostati.size());
        assertEquals("Binance", spostati.get(0)[0]);
        assertEquals("Wallet 101", spostati.get(0)[1]);
        assertEquals("Wallet 101", DatabaseH2.Pers_GruppoWallet_Leggi("Binance", false));
        assertEquals("Wallet 07", DatabaseH2.Pers_GruppoWallet_Leggi("Kraken", false),
                "la scelta dell'utente non va toccata neanche dal raggruppamento forzato");
    }

    @Test
    void raggruppaForzato_nienteDaFare_ritornaListaVuota() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Binance", "Wallet 101");
        Principale_GruppiWalletRW.congelaPreesistentiSeNecessario();
        assertTrue(Principale_GruppiWalletRW.raggruppaWalletExchangeNoti(java.util.List.of("Binance")).isEmpty());
    }

    // ---------------- riepilogo "Info su Gruppo Wallet selezionato" ----------------

    @Test
    void formattaTimestampId_formatoLeggibile() {
        assertEquals("2024-03-15 10:30", Principale_GruppiWalletRW.formattaTimestampId("20240315103000"));
        assertEquals("", Principale_GruppiWalletRW.formattaTimestampId(null));
        assertEquals("abc", Principale_GruppiWalletRW.formattaTimestampId("abc")); // non conforme: restituito com'è
    }

    @Test
    void infoWalletDelGruppo_contaMovimentiEDate() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase", "Wallet 102");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 102");
        String[] w1 = new String[45]; w1[0] = "20240101120000_x_001_AC"; w1[3] = "Coinbase";
        String[] w2 = new String[45]; w2[0] = "20240310090000_x_002_PC"; w2[3] = "Coinbase";
        String[] w3 = new String[45]; w3[0] = "20231201080000_x_003_DC"; w3[3] = "Kraken";
        String[] w4 = new String[45]; w4[0] = "20240101000000_x_004_AC"; w4[3] = "AltroWallet"; // altro gruppo
        try {
            Principale.MappaCryptoWallet.put(w1[0], w1);
            Principale.MappaCryptoWallet.put(w2[0], w2);
            Principale.MappaCryptoWallet.put(w3[0], w3);
            Principale.MappaCryptoWallet.put(w4[0], w4);

            List<String[]> info = Principale_GruppiWalletRW.infoWalletDelGruppo("Wallet 102");
            assertEquals(2, info.size(), "solo i wallet associati a Wallet 102");
            // ordine per LOWER(Wallet): Coinbase, Kraken
            assertArrayEquals(new String[]{"Coinbase", "2", "2024-01-01 12:00", "2024-03-10 09:00"}, info.get(0));
            assertArrayEquals(new String[]{"Kraken", "1", "2023-12-01 08:00", "2023-12-01 08:00"}, info.get(1));
        } finally {
            Principale.MappaCryptoWallet.remove(w1[0]);
            Principale.MappaCryptoWallet.remove(w2[0]);
            Principale.MappaCryptoWallet.remove(w3[0]);
            Principale.MappaCryptoWallet.remove(w4[0]);
        }
    }

    @Test
    void infoWalletDelGruppo_walletSenzaMovimenti_conteggioZero() {
        DatabaseH2.Pers_GruppoWallet_Scrivi("Bitget", "Wallet 107");
        List<String[]> info = Principale_GruppiWalletRW.infoWalletDelGruppo("Wallet 107");
        assertEquals(1, info.size());
        assertArrayEquals(new String[]{"Bitget", "0", "", ""}, info.get(0));
    }

    @Test
    void datiFiscaliGruppo_periodoFiatCorrenteEBollo() {
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 102", "Coinbase", false);
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 102",
                lista(rigaF("1", "", "", "092", "LU36476644"))).isEmpty());

        // chiave -> valore, per comodità
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (String[] r : Principale_GruppiWalletRW.datiFiscaliGruppo("Wallet 102")) {
            m.put(r[0], r[1]);
        }
        assertEquals("Coinbase", m.get("Alias"));
        assertTrue(m.get("Stato estero (corrente)").contains("092"), m.toString());
        assertEquals("LU36476644", m.get("Identificativo fiscale (corrente)"));
        assertEquals("NO", m.get("Bollo pagato dall'intermediario"));
    }

    @Test
    void datiFiscaliGruppo_senzaPeriodiFiat_nessunoStato() {
        DatabaseH2.Pers_GruppoAlias_Scrivi("Wallet 09", "Mio gruppo", true);
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (String[] r : Principale_GruppiWalletRW.datiFiscaliGruppo("Wallet 09")) {
            m.put(r[0], r[1]);
        }
        assertTrue(m.get("Periodo FIAT corrente").startsWith("— nessuno"), m.toString());
        assertEquals("SI", m.get("Bollo pagato dall'intermediario")); // dal flag per-gruppo
    }

    @Test
    void periodiPerVista_sottoinsiemeColonne_bolloSoloCryptoDatiFiscaliSoloFiat() {
        Principale_GruppiWalletRW.salvaPeriodi("Wallet 102", java.util.List.of(
                rigaB(Principale_GruppiWalletRW.TIPO_CRYPTO, "1", "2025-01-01", "", Principale_GruppiWalletRW.BOLLO_SI),
                rigaF("1", "2025-01-01", "", "092", "LU36476644")));
        List<String[]> v = Principale_GruppiWalletRW.periodiPerVista("Wallet 102");
        assertEquals(2, v.size());
        assertEquals(11, v.get(0).length);
        assertEquals(Principale_GruppiWalletRW.TIPO_CRYPTO, v.get(0)[0]);
        assertEquals("n/d", v.get(0)[6], "sul rigo CRYPTO lo Stato estero è n/d");
        assertEquals("SI", v.get(0)[9]);
        assertTrue(v.get(1)[6].contains("092"));
        assertEquals("LU36476644", v.get(1)[7]);
        assertEquals("n/d", v.get(1)[9], "sul rigo FIAT il bollo è n/d");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
