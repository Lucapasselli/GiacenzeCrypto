package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il giro completo dei prezzi della soglia: un token senza prezzo al 1° gennaio finisce fra i mancanti, l'utente gli
 * assegna un prezzo con {@link Principale_RT_Soglia#SalvaPrezzo} e il calcolo, che usa le fonti vere dei prezzi
 * ({@link Calcoli_RW_Giacenze#PREZZO_MERCATO}), lo trova fra i personalizzati. Database temporaneo, nessuna rete
 * (la guardia dei test spegne le fonti remote: un token senza personalizzato resta senza prezzo).
 */
class Principale_RT_SogliaPrezziTest {

    @TempDir
    static Path tempDir;

    private static int progressivo = 0;

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
        Principale.MappaCryptoWallet.clear();
    }

    @BeforeEach
    void setUp() throws Exception {
        //I prezzi personalizzati stanno nel database, condiviso fra i test della classe: ognuno parte senza
        try (java.sql.Statement st = DatabaseH2.connectionPersonale.createStatement()) {
            st.execute("DELETE FROM PrezziNew");
        }
        Principale.MappaCryptoWallet.clear();
        progressivo = 0;
        mov("2021-03-01 10:00", "DC", "", "", "", "ZZZTEST", "Crypto", "100");
    }

    private static void mov(String data, String cat, String monU, String tipoU, String qtaU,
            String monE, String tipoE, String qtaE) {
        String id = data.replace("-", "").replace(":", "").replace(" ", "") + "_TEST_"
                + String.format("%03d", ++progressivo) + "_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "Kraken";
        m[8] = monU;  m[9] = tipoU;  m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        Principale.MappaCryptoWallet.put(id, m);
    }

    @Test
    void ilPrezzoAssegnatoLoLeggeIlCalcoloDellaSoglia() {
        var prima = SogliaGiacenzaAnte2023.Calcola(2021);
        assertTrue(prima.incompleto(), "nessuna fonte ha il prezzo di un token inventato");
        assertEquals(1, prima.monetePrezzoMancante.size());
        Moneta m = prima.monetePrezzoMancante.get(0);
        assertEquals("ZZZTEST", m.Moneta);
        assertEquals("100", m.Qta);

        assertTrue(Principale_RT_Soglia.SalvaPrezzo(m, 2021, new BigDecimal("700"), "", 0));
        var dopo = SogliaGiacenzaAnte2023.Calcola(2021);
        assertFalse(dopo.incompleto(), "con il prezzo assegnato il token non e' piu' senza prezzo");
        assertEquals(0, new BigDecimal("70000").compareTo(dopo.valoreMassimo), "100 unita' a 700 euro");
        assertTrue(dopo.superata, "70.000 euro superano la soglia dal primo giorno lavorativo dopo il deposito");
    }

    @Test
    void ilPrezzoZeroTogliIlTokenDaiMancanti() {
        Moneta m = SogliaGiacenzaAnte2023.Calcola(2021).monetePrezzoMancante.get(0);
        assertTrue(Principale_RT_Soglia.SalvaPrezzo(m, 2021, BigDecimal.ZERO, "Personalizzato", 0));
        var dopo = SogliaGiacenzaAnte2023.Calcola(2021);
        assertFalse(dopo.incompleto());
        assertEquals(0, BigDecimal.ZERO.compareTo(dopo.valoreMassimo));
        assertFalse(dopo.superata);
    }

    @Test
    void unPrezzoNegativoNonSiSalva() {
        Moneta m = SogliaGiacenzaAnte2023.Calcola(2021).monetePrezzoMancante.get(0);
        assertFalse(Principale_RT_Soglia.SalvaPrezzo(m, 2021, new BigDecimal("-1"), "", 0));
        assertFalse(Principale_RT_Soglia.SalvaPrezzo(null, 2021, BigDecimal.ONE, "", 0));
    }

    @Test
    void l_istanteDelPrezzoELe0000DelPrimoGennaio() {
        assertEquals(FunzioniDate.ConvertiDatainLongMinuto("2021-01-01 00:00"), Principale_RT_Soglia.IstantePrezzo(2021));
    }
}
