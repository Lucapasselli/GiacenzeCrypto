package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Principale_TrasferimentiSenzaCollegamento#AnnullaClassificazione}: i trasferimenti senza collegamento tornano non classificati
 * (descrizione e classificazione dell'import), e un movimento che non c'e' piu' si salta. Dati inventati.
 */
class Principale_TrasferimentiSenzaCollegamentoTest {

    @TempDir
    static Path tempDir;

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
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String id, String descrizione, String classificazione, boolean uscita) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = "2025-01-01 10:00";
        v[3] = "Wallet";
        v[4] = "Principale";
        v[5] = descrizione;
        if (uscita) {
            v[8] = "BTC"; v[9] = "Crypto"; v[10] = "-1";
        } else {
            v[11] = "BTC"; v[12] = "Crypto"; v[13] = "1";
        }
        v[15] = "100.00";
        v[18] = classificazione;
        v[22] = "A";
        MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void ilPrelievoEIlDepositoTornanoNonClassificati() {
        String[] p = movimento("20250101100000_A_001_001_PC", "TRASFERIMENTO TRA WALLET", "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", true);
        String[] d = movimento("20250102100000_B_001_001_DC", "TRASFERIMENTO TRA WALLET", "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", false);
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        int fatti = Principale_TrasferimentiSenzaCollegamento.AnnullaClassificazione(List.of(p[0], d[0]), esito);

        assertEquals(2, fatti);
        assertEquals("", p[18]);
        assertEquals("PRELIEVO CRYPTO", p[5]);
        assertEquals("", d[18]);
        assertEquals("DEPOSITO CRYPTO", d[5]);
        assertEquals("", p[20]);
        assertTrue(esito.problemi.isEmpty());
        //E ora non risultano piu' fra i trasferimenti senza collegamento
        assertTrue(MovimentiCollegati.Controlla(MappaCryptoWallet.values()).SenzaCollegamento.isEmpty());
    }

    @Test
    void unMovimentoCheNonCePiu_siSalta() {
        String[] p = movimento("20250101100000_A_001_001_PC", "TRASFERIMENTO TRA WALLET", "PTW - Trasferimento tra Wallet", true);
        Principale_DepositiPrelievi.Esito esito = new Principale_DepositiPrelievi.Esito();

        int fatti = Principale_TrasferimentiSenzaCollegamento.AnnullaClassificazione(List.of("20250101100000_Z_001_001_PC", p[0]), esito);

        assertEquals(1, fatti);
        assertEquals("", p[18]);
    }

    @Test
    void unMovimentoNonSegnalatoNonSiTocca() {
        String[] altro = movimento("20250101100000_A_001_001_DC", "REWARD", "DAI - Provento da Detenzione", false);
        String[] p = movimento("20250102100000_A_001_001_PC", "TRASFERIMENTO TRA WALLET", "PTW - Trasferimento tra Wallet", true);

        Principale_TrasferimentiSenzaCollegamento.AnnullaClassificazione(List.of(p[0]), new Principale_DepositiPrelievi.Esito());

        assertEquals("DAI - Provento da Detenzione", altro[18]);
        assertEquals("REWARD", altro[5]);
    }
}
