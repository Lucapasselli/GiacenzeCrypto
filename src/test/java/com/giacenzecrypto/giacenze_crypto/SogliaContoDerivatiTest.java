package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Soglia dei 51.645,69 euro (anni fino al 2022) e conto derivati: con l'opzione {@code Plusvalenze_SogliaContoDerivati} (default: conta,
 * lettura prudenziale) la giacenza sul sotto-wallet {@code Derivati} entra nella soglia, spenta ne resta fuori.
 */
class SogliaContoDerivatiTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        MappaCryptoWallet.clear();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void archivio() {
        MappaCryptoWallet.clear();
        DatabaseH2.Pers_Opzioni_Scrivi(Principale_RT_Soglia.OPZIONE_CONTO_DERIVATI, "SI");
    }

    @AfterEach
    void ripristina() {
        DatabaseH2.Pers_Opzioni_Scrivi(Principale_RT_Soglia.OPZIONE_CONTO_DERIVATI, "SI");
    }

    private static void mov(String id, String data, String sottoWallet, String monU, String qtaU, String monE, String qtaE, String derivato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "Bybit";
        v[4] = sottoWallet;
        v[8] = monU; v[9] = monU.isEmpty() ? "" : "Crypto"; v[10] = qtaU;
        v[11] = monE; v[12] = monE.isEmpty() ? "" : "Crypto"; v[13] = qtaE;
        v[Derivati.CAMPO] = derivato;
        MappaCryptoWallet.put(id, v);
    }

    /** 600 BTC a 100 euro = 60.000 sullo spot, di cui 400 (40.000) spostati sul conto derivati: spot 20.000, conto 40.000. */
    private static void scenario() {
        mov("20210301100000_TEST_001_001_DC", "2021-03-01 10:00", "Principale", "", "", "BTC", "600", "");
        mov("20210301110000_TEST_901_001_TI", "2021-03-01 11:00", "Principale", "BTC", "-400", "", "", Derivati.MARGINE);
        mov("20210301110000_TEST_902_001_TI", "2021-03-01 11:00", OKXDerivati.SOTTOWALLET, "", "", "BTC", "400", Derivati.MARGINE);
    }

    private static SogliaGiacenzaAnte2023.Esito calcola() {
        return SogliaGiacenzaAnte2023.Calcola(2021, (m, istante) -> new BigDecimal("100").multiply(new BigDecimal(m.Qta)).toPlainString());
    }

    @Test
    void diDefaultIlContoContaNellaSoglia() {
        scenario();
        assertTrue(Principale_RT_Soglia.contoDerivatiConta());
        var e = calcola();
        assertTrue(e.superata, "60.000 euro in tutto, conto compreso");
        assertEquals(0, new BigDecimal("60000").compareTo(e.valoreMassimo));
    }

    @Test
    void conLOpzioneSpentaIlContoNonConta() {
        scenario();
        DatabaseH2.Pers_Opzioni_Scrivi(Principale_RT_Soglia.OPZIONE_CONTO_DERIVATI, "NO");
        assertFalse(Principale_RT_Soglia.contoDerivatiConta());
        var e = calcola();
        assertFalse(e.superata, "restano 200 BTC sullo spot, 20.000 euro");
        assertEquals(0, new BigDecimal("20000").compareTo(e.valoreMassimo));
    }
}
