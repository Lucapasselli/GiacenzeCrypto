package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caratterizza le causali di {@code config/import/Bitget CSV.json} (export "spot transactions").
 * Righe sintetiche, nessun dato reale.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>'LaunchX expense' + 'LaunchX income' nello stesso secondo sono un solo scambio (il BGB e' il
 *       prezzo del token), non un premio con il BGB che resta in giacenza;</li>
 *   <li>'Automatic deposit'/'Automatic withdrawal' non vengono scartati come causale sconosciuta ma
 *       diventano depositi e prelievi da classificare, uno per riga anche quando USDT e BGB entrano
 *       nello stesso secondo.</li>
 * </ul>
 */
class ImportazioneGenericaBitgetTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
        Funzioni.ReteDisabilitataPerTest = true;
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Colonna controvalore sintetica in coda alla riga: niente ricerca prezzi in rete. */
    private static ConfigurazioneImport bitget() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bitget CSV.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    /** {@code order,Date,Coin,Type,Amount,Fee,Available} + [7]=controvalore sintetico */
    private static String[] riga(String order, String date, String coin, String type, String amount) {
        return new String[]{order, date, coin, type, amount, "0", "0", "5.00"};
    }

    /** Stessa sequenza dell'import vero: annullamenti, somma per giorno, raggruppamento, consolidamento. */
    private static List<String[]> importa(List<String[]> righe, ConfigurazioneImport c) {
        List<String[]> movs = new ArrayList<>();
        righe = ImportazioneGenerica.scartaAnnullamenti(righe, c);
        righe = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, c);
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(righe, c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        return movs;
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    @Test
    void launchX_expensePiuIncome_unoScambio() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(riga("2", "2025-01-22 03:44:55", "J", "LaunchX income", "9.19"));
        righe.add(riga("1", "2025-01-22 03:44:55", "BGB", "LaunchX expense", "-0.659058093"));

        Importazioni.AzzeraContatori();
        List<String[]> movs = importa(righe, bitget());
        assertEquals(0, Importazioni.TrasazioniSconosciute);
        assertEquals(1, movs.size(), "impegno e token ricevuto sono lo stesso scambio");
        String[] m = movs.get(0);
        assertEquals("SC", categoria(m));
        assertEquals("BGB", m[8]);
        assertEquals(0, new BigDecimal(m[10]).compareTo(new BigDecimal("-0.659058093")),
                "il BGB impegnato esce dalla giacenza");
        assertEquals("J", m[11]);
        assertEquals(0, new BigDecimal(m[13]).compareTo(new BigDecimal("9.19")));
    }

    @Test
    void automatic_depositiEPrelieviDaClassificare() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(riga("13", "2025-01-24 13:42:42", "USDT", "Automatic withdrawal", "-65"));
        righe.add(riga("12", "2025-01-24 12:56:47", "BGB", "Automatic deposit", "3.7101861"));
        righe.add(riga("11", "2025-01-24 12:56:47", "USDT", "Automatic deposit", "38.3787648"));

        Importazioni.AzzeraContatori();
        List<String[]> movs = importa(righe, bitget());
        assertEquals(0, Importazioni.TrasazioniSconosciute, "non piu' scartate come causale sconosciuta");
        assertEquals(3, movs.size(), "un movimento per riga, anche per le due entrate dello stesso secondo");

        String[] usdtIn = movs.stream().filter(m -> "USDT".equals(m[11])).findFirst().orElseThrow();
        String[] bgbIn = movs.stream().filter(m -> "BGB".equals(m[11])).findFirst().orElseThrow();
        String[] usdtOut = movs.stream().filter(m -> "USDT".equals(m[8])).findFirst().orElseThrow();
        assertEquals("DC", categoria(usdtIn));
        assertEquals("DC", categoria(bgbIn));
        assertEquals("PC", categoria(usdtOut));
        assertEquals(0, new BigDecimal(usdtOut[10]).compareTo(new BigDecimal("-65")));
        for (String[] m : movs) {
            assertEquals("", m[18], "campo18 vuoto: da classificare a mano");
        }
    }
}
