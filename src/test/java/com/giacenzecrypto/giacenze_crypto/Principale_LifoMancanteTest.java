package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link Principale_LifoMancante}: quali depositi il dialogo "Parte del LIFO mancante" conta come trasferimenti
 * senza prelievo o come classificazioni non riconosciute, e che il riconoscimento delle causali resti allineato alla
 * catena di {@code if} dei depositi di {@link Calcoli_PlusvalenzeNew#ElaboraMovimento}.
 */
class Principale_LifoMancanteTest {

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
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    private static String[] deposito(String ID, String moneta, String tipo, String campo18, String collegati) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-10 10:00:00";
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = "DEPOSITO CRYPTO";
        v[11] = moneta;
        v[12] = tipo;
        v[13] = "1.5";
        v[15] = "100.00";
        v[18] = campo18;
        v[20] = collegati;
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static String[] prelievo(String ID, String campo18, String collegati) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-10 09:59:00";
        v[2] = "1 di 1";
        v[3] = "Kraken";
        v[4] = "Principale";
        v[5] = "PRELIEVO CRYPTO";
        v[8] = "BTC";
        v[9] = "Crypto";
        v[10] = "-1.5";
        v[15] = "100.00";
        v[18] = campo18;
        v[20] = collegati;
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static final String DTW = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
    private static final String PTW = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";

    @Test
    void trasferimentoConPrelievoCollegatoNonESegnalato() {
        prelievo("202403100959_Kraken_1_1_PC", PTW, "202403101000_Binance_1_1_DC");
        deposito("202403101000_Binance_1_1_DC", "BTC", "Crypto", DTW, "202403100959_Kraken_1_1_PC");

        Principale_LifoMancante.Esito E = Principale_LifoMancante.Controlla(MappaCryptoWallet.values());
        assertTrue(E.TrasferimentiSenzaPrelievo.isEmpty());
        assertTrue(E.CausaliNonRiconosciute.isEmpty());
    }

    @Test
    void trasferimentoSenzaCollegamentoOConCollegamentoRottoESegnalato() {
        deposito("202403101000_Binance_1_1_DC", "BTC", "Crypto", DTW, "");
        deposito("202403101001_Binance_1_1_DC", "ETH", "Crypto", DTW, "202403100959_Kraken_9_1_PC");
        //Collegato a un movimento che esiste ma non è un PTW
        prelievo("202403100959_Kraken_1_1_PC", "", "202403101002_Binance_1_1_DC");
        deposito("202403101002_Binance_1_1_DC", "SOL", "Crypto", DTW, "202403100959_Kraken_1_1_PC");

        Principale_LifoMancante.Esito E = Principale_LifoMancante.Controlla(MappaCryptoWallet.values());
        assertEquals(3, E.TrasferimentiSenzaPrelievo.size());
        assertTrue(E.CausaliNonRiconosciute.isEmpty());
    }

    @Test
    void depositiTecniciInterniNonClassificatiScamEFiatNonSonoSegnalati() {
        //TI: l'importazione li genera insieme al prelievo TI dello stesso exchange, senza campo 20
        deposito("202403101000_Binance_1_1_TI", "BTC", "Crypto", "", "");
        //Non classificato: dipende dall'opzione, lo conta già il pulsante Errori
        deposito("202403101001_Binance_1_1_DC", "BTC", "Crypto", "", "");
        //Token SCAM e deposito FIAT con causale sconosciuta
        deposito("202403101002_Binance_1_1_DC", "TRUFFA **", "Crypto", "XYZ - Inventato", "");
        deposito("202403101003_Binance_1_1_DC", "EUR", "FIAT", "XYZ - Inventato", "");

        Principale_LifoMancante.Esito E = Principale_LifoMancante.Controlla(MappaCryptoWallet.values());
        assertTrue(E.TrasferimentiSenzaPrelievo.isEmpty());
        assertTrue(E.CausaliNonRiconosciute.isEmpty());
    }

    @Test
    void causaleSconosciutaESegnalata() {
        deposito("202403101000_Binance_1_1_DC", "BTC", "Crypto", "XYZ - Inventato", "");
        Principale_LifoMancante.Esito E = Principale_LifoMancante.Controlla(MappaCryptoWallet.values());
        assertEquals(1, E.CausaliNonRiconosciute.size());
        assertTrue(E.TrasferimentiSenzaPrelievo.isEmpty());
    }

    /**
     * Il predicato è una copia della catena dei depositi del motore. Ogni ramo riconosciuto azzera il campo 16
     * (vecchio costo di carico), un deposito che non entra in nessun ramo lo lascia al valore iniziale "0.00":
     * è il segnale con cui qui si verifica che le due copie dicono la stessa cosa.
     */
    @Test
    void ilRiconoscimentoCoincideConIRamiDelMotore() {
        String[][] Casi = {
            {"DC", "DAI - Reward"}, {"RW", ""}, {"TI", ""}, {"DC", DTW}, {"DC", "DCZ - Deposito a costo zero"},
            {"DC", "DAC - Acquisto Crypto"}, {"AC", ""}, {"DC", "DDO - Donazione"}, {"DC", ""},
            {"DC", "XYZ - Inventato"}, {"DC", "PTW - Prelievo"}, {"ac", "XYZ"}, {"SC", "XYZ"}, {"rw", "XYZ"}
        };
        Calcoli_PlusvalenzeNew.OpzioniRicalcolo Opzioni = new Calcoli_PlusvalenzeNew.OpzioniRicalcolo();
        int n = 0;
        for (String[] Caso : Casi) {
            String ID = "20240310100" + (n++) + "_Binance_1_1_" + Caso[0];
            String[] v = deposito(ID, "BTC", "Crypto", Caso[1], "");
            Map<String, Map<String, ArrayDeque<String[]>>> Stato = new TreeMap<>();
            Calcoli_PlusvalenzeNew.ElaboraMovimento(v, Stato, Opzioni);
            boolean MotoreLoRiconosce = !"0.00".equals(v[16]);
            assertEquals(MotoreLoRiconosce, Principale_LifoMancante.CausaleDepositoRiconosciuta(Caso[0], Caso[1]),
                    "Categoria " + Caso[0] + ", campo 18 \"" + Caso[1] + "\"");
        }
    }
}
