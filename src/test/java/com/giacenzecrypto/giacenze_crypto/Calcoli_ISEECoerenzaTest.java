package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Calcoli_ISEE} sulle fonti vere (archivio dei movimenti, gruppi wallet, fotografia del quadro W/RW, gambe
 * FIAT): il valore al 31/12 di un rapporto deve essere quello della fotografia {@link Calcoli_RW_Giacenze} più la
 * liquidità, e le quantità che il motore ISEE legge dai movimenti per la media giornaliera devono coincidere con
 * quelle della fotografia (trasferimenti interni, NFT e più wallet nello stesso gruppo compresi).
 */
class Calcoli_ISEECoerenzaTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

    /** Prezzo finto: 100 euro per unità di qualunque moneta. */
    private static final Calcoli_RW_Giacenze.PrezzoGiacenza PREZZO = (m, istante) ->
            new BigDecimal(m.Qta).multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();

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
            st.execute("DELETE FROM WALLETGRUPPO");
        }
        DatabaseH2.Pers_GruppoPeriodoRW_CancellaGruppo("Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken", "Wallet 01");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Kraken Earn", "Wallet 01");
        progressivo = 0;
    }

    private static void mov(String data, String wallet, String cat, String valore,
            String monU, String tipoU, String qtaU, String monE, String tipoE, String qtaE) {
        String id = data.replace("-", "").replace(":", "").replace(" ", "") + "_TEST_"
                + String.format("%03d", ++progressivo) + "_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = wallet;
        m[8] = monU;  m[9] = tipoU;  m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        m[15] = valore;
        Principale.MappaCryptoWallet.put(id, m);
    }

    @Test
    void valoreEQuantitaCoincidonoConLaFotografiaDelQuadroRW() {
        mov("2023-02-01 10:00", "Kraken", "DF", "1000", "", "", "", "EUR", "FIAT", "1000");
        mov("2023-03-01 10:00", "Kraken", "AC", "1000", "EUR", "FIAT", "-1000", "BTC", "Crypto", "0.5");
        mov("2023-04-01 10:00", "Kraken", "DC", "0", "", "", "", "PUNK", "NFT", "1");
        mov("2024-02-01 10:00", "Kraken", "DF", "300", "", "", "", "EUR", "FIAT", "300");
        // trasferimento fra due wallet dello stesso gruppo
        mov("2024-05-01 10:00", "Kraken", "TI", "10", "BTC", "Crypto", "-0.1", "", "", "");
        mov("2024-05-01 10:05", "Kraken Earn", "TI", "10", "", "", "", "BTC", "Crypto", "0.1");
        mov("2024-07-01 10:00", "Kraken", "DC", "200", "", "", "", "ETH", "Crypto", "2");
        // dopo l'anno: non conta
        mov("2025-01-10 10:00", "Kraken", "DC", "100", "", "", "", "ETH", "Crypto", "1");

        Calcoli_ISEE.Risultato ris = Calcoli_ISEE.calcola(2024, PREZZO, (valuta, importo, data) -> importo, null);

        Map<String, List<String[]>> foto = Calcoli_RW_Giacenze.GiacenzeInizioFineAnno("2024", PREZZO);
        BigDecimal valoreFoto = BigDecimal.ZERO;
        for (String[] r : foto.get("Wallet 01")) {
            valoreFoto = valoreFoto.add(new BigDecimal(r[10]));
        }
        assertEquals(0, new BigDecimal("350").compareTo(valoreFoto), "0,5 BTC + 1 PUNK + 2 ETH a 100 euro");

        assertEquals(1, ris.rapporti.size());
        Calcoli_ISEE.Rapporto r = ris.rapporti.get(0);
        assertEquals("Wallet 01", r.gruppo);
        assertEquals(0, valoreFoto.add(new BigDecimal("300")).compareTo(r.valore), "fotografia + 300 euro di liquidità");
        assertEquals(0, new BigDecimal("300").compareTo(r.liquidita));
        assertTrue(r.note.stream().noneMatch(n -> n.contains("fotografia")),
                "le quantità dei movimenti coincidono con la fotografia: " + r.note);
    }

    @Test
    void ilValoreDel31_12NellaMediaEQuelloDellaFotografia() {
        // 0,5 BTC tutto il 2024. La fotografia e la media chiedono il prezzo con la stessa funzione: il 31/12 della
        // media deve chiederlo allo stesso istante e con la stessa quantità della fotografia.
        mov("2023-03-01 10:00", "Kraken", "DC", "1000", "", "", "", "BTC", "Crypto", "0.5");
        java.util.Map<Long, String> chiestiFoto = new java.util.HashMap<>();
        Calcoli_RW_Giacenze.PrezzoGiacenza prezzo = (m, istante) -> {
            // prezzo diverso a ogni istante: 100 euro più un euro per ogni ora dal 2024
            long ore = (istante - FunzioniDate.ConvertiDatainLongMinuto("2024-01-01 00:00")) / 3_600_000L;
            String v = new BigDecimal(m.Qta).multiply(new BigDecimal(100 + ore)).toPlainString();
            chiestiFoto.put(istante, v);
            return v;
        };
        Calcoli_ISEE.Risultato ris = Calcoli_ISEE.calcola(2024, prezzo, (valuta, importo, data) -> importo, null);
        String valoreFoto = chiestiFoto.get(Calcoli_ISEE.istanteFineAnno(2024));
        assertNotNull(valoreFoto, "la fotografia prezza alle 00:00 del 1/1");

        java.util.Map<LocalDate, BigDecimal> giorno = new java.util.HashMap<>();
        Calcoli_ISEE.ValoreFineGiornata valori = Calcoli_ISEE.valoreDa(prezzo);
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, (m, qta, g) -> {
            BigDecimal v = valori.valore(m, qta, g);
            giorno.put(g, v);
            return v;
        });

        assertEquals(366, giorno.size());
        assertEquals(0, new BigDecimal(valoreFoto).compareTo(giorno.get(LocalDate.of(2024, 12, 31))),
                "31/12 della media = valore della fotografia");
        assertEquals(0, new BigDecimal(valoreFoto).compareTo(ris.rapporti.get(0).valore));
        // il 1/1 vale 0,5 x (100 + 24): il prezzo delle 00:00 del 2/1
        assertEquals(0, new BigDecimal("62").compareTo(giorno.get(LocalDate.of(2024, 1, 1))));
    }

    @Test
    void contoCorrenteDalPeriodoSalvato_rapporto01ConLaGiacenzaMedia() {
        // Periodo FIAT salvato come lo salva la finestra: conto corrente, Irlanda, identificativo 123456.
        String[] p = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        Arrays.fill(p, "");
        p[Principale_GruppiWalletRW.COL_TIPO] = Principale_GruppiWalletRW.TIPO_FIAT;
        p[Principale_GruppiWalletRW.COL_PROGRESSIVO] = "1";
        p[Principale_GruppiWalletRW.COL_STATO_ESTERO] = "040";
        p[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = "123456";
        p[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE] = Principale_GruppiWalletRW.CONTO_CORRENTE_SI;
        assertTrue(Principale_GruppiWalletRW.salvaPeriodi("Wallet 01", List.<String[]>of(p)).isEmpty());
        // 300 euro dal 1/2/2024 al 31/12: 335 giorni x 300 = 100.500 / 365 = 275,34
        mov("2023-02-01 10:00", "Kraken", "DF", "1000", "", "", "", "EUR", "FIAT", "1000");
        mov("2023-03-01 10:00", "Kraken", "AC", "1000", "EUR", "FIAT", "-1000", "BTC", "Crypto", "0.5");
        mov("2024-02-01 10:00", "Kraken", "DF", "300", "", "", "", "EUR", "FIAT", "300");

        Calcoli_ISEE.Risultato ris = Calcoli_ISEE.calcola(2024, PREZZO, (valuta, importo, data) -> importo, null);

        assertEquals(2, ris.rapporti.size());
        Calcoli_ISEE.Rapporto conto = ris.rapporti.get(0);
        assertEquals("Wallet 01|01|P1", conto.chiave);
        assertEquals("E123456", conto.operatore);
        assertEquals(0, new BigDecimal("300").compareTo(conto.valore));
        assertEquals(0, new BigDecimal("275.34").compareTo(conto.giacenzaMedia));
        Calcoli_ISEE.Rapporto cripto = ris.rapporti.get(1);
        assertEquals(0, new BigDecimal("50").compareTo(cripto.valore), "0,5 BTC a 100 euro, la liquidità sta nel 01");
    }
}
