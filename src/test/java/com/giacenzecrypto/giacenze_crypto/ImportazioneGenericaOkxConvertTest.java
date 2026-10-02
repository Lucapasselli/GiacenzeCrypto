package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code config/import/OKX_Trading.json} sulle righe {@code Convert} dell'export "Trading History":
 * Trade Type {@code Spot}, Symbol {@code TAO-USDC-CONVERT} e <b>Action vuota</b>. La causale composta
 * ({@code Trade Type.Action}) è quindi {@code "Spot."}, che la config non mappava: segnalazione #11,
 * 52 righe su 66 finite fra le sconosciute.
 *
 * <p>Righe sintetiche nella forma dell'export reale (22 colonne, le ultime cinque non usate dalla config).</p>
 */
class ImportazioneGenericaOkxConvertTest {

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

    private static final String RIGA_UID = "UID,123456,Trading account,,,,,,,,,,,,,,,,,,,";
    private static final String INTESTAZIONE = "id,Order id,Time,Trade Type,Symbol,Action,Amount,Trading Unit,Filled Price,"
            + "PnL,Fee,Fee Unit,Position Change,Position Balance,Balance Change,Balance,Balance Unit,x1,x2,x3,x4,x5";

    private static ConfigurazioneImport cfg() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/OKX_Trading.json");
        c.colonnaValoreEuro = 22; // controvalore sintetico: evita la ricerca prezzi in rete
        return c;
    }

    /** Importa un file intero: lettura, raggruppamento e costruzione, come fa l'import vero. */
    private static List<String[]> importa(String... righe) throws Exception {
        List<String> tutte = new ArrayList<>(List.of(RIGA_UID, INTESTAZIONE));
        tutte.addAll(Arrays.asList(righe));
        Path f = tempDir.resolve("okx" + System.nanoTime() + ".csv");
        Files.write(f, (String.join("\n", tutte) + "\n").getBytes("UTF-8"));
        ConfigurazioneImport c = cfg();
        List<String[]> conValore = new ArrayList<>();
        for (String[] r : ImportazioneGenerica.leggiCSV(f.toString(), c)) {
            String[] cv = Arrays.copyOf(r, 23);
            cv[22] = "1.00";
            conValore.add(cv);
        }
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(conValore, c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        return movs;
    }

    @Test
    void convertCryptoCrypto_unSoloScambio() throws Exception {
        List<String[]> movs = importa(
                "3717152364831481856,3717152364697264128,2026-07-06 04:09:33,Spot,TAO-USDC-CONVERT,,0.00506778,USDC,209.68861305,0,0,TAO,0,0,-0.00506778,0,TAO,183.85,0,0,0,-0.95",
                "3717152364831481857,3717152364697264128,2026-07-06 04:09:33,Spot,TAO-USDC-CONVERT,,1.06265576,USDC,209.68861305,0,0,USDC,0,0,1.06265576,1.06265576,USDC,183.85,0,0,0.93,0.93");
        assertEquals(1, movs.size(), Arrays.deepToString(movs.toArray()));
        String[] m = movs.get(0);
        assertEquals("TAO", m[8]);
        assertEquals("-0.00506778", m[10]);
        assertEquals("USDC", m[11]);
        assertEquals("1.06265576", m[13]);
    }

    /** La conversione contro euro e' un acquisto: la gamba FIAT la riconosce il programma, non la config. */
    @Test
    void convertControEuro_eUnAcquistoCrypto() throws Exception {
        List<String[]> movs = importa(
                "3791444272696483840,3791444272629374976,2026-07-31 19:10:44,Spot,USDC-EUR-CONVERT,,57.19923035,EUR,0.87,0,0,USDC,0,0,57.19923035,681.13,USDC,0.87,0,0,596,50.09",
                "3791444272696483841,3791444272629374976,2026-07-31 19:10:44,Spot,USDC-EUR-CONVERT,,50.00000000,EUR,0.87,0,0,EUR,0,0,-50.00000000,2350.00,EUR,0.87,0,0,2364,-50.30");
        assertEquals(1, movs.size());
        assertEquals("ACQUISTO CRYPTO", movs.get(0)[5]);
        assertEquals("EUR", movs.get(0)[8]);
        assertEquals("USDC", movs.get(0)[11]);
    }

    /** Due conversioni nello stesso secondo, Order id diversi: restano due scambi. */
    @Test
    void dueConvertNelloStessoSecondo_restanoDueScambi() throws Exception {
        List<String[]> movs = importa(
                "1001,ORD-A,2026-07-06 04:09:33,Spot,TAO-USDC-CONVERT,,0.5,USDC,200,0,0,TAO,0,0,-0.5,0,TAO,183,0,0,0,0",
                "1002,ORD-A,2026-07-06 04:09:33,Spot,TAO-USDC-CONVERT,,100,USDC,200,0,0,USDC,0,0,100,100,USDC,183,0,0,0,0",
                "1003,ORD-B,2026-07-06 04:09:33,Spot,BTC-USDC-CONVERT,,0.001,USDC,60000,0,0,BTC,0,0,0.001,0.001,BTC,55000,0,0,0,0",
                "1004,ORD-B,2026-07-06 04:09:33,Spot,BTC-USDC-CONVERT,,60,USDC,60000,0,0,USDC,0,0,-60,0,USDC,55000,0,0,0,0");
        assertEquals(2, movs.size());
    }

    /**
     * Polvere: OKX arrotonda a 8 decimali e puo' lasciare la gamba in entrata a zero. Il programma tiene la
     * sola uscita come prelievo da classificare, senza scartare la riga ne' inventare un'entrata.
     */
    @Test
    void convertDiPolvereConGambaAZero_restaUnPrelievoDaClassificare() throws Exception {
        List<String[]> movs = importa(
                "3832201364046565377,3832201363878793216,2026-08-14 20:35:00,Spot,SLX-USDC-CONVERT,,0.00000001,USDC,0.0778,0,0,SLX,0,0,-0.00000001,0,SLX,0.06,0,0,0,0",
                "3832201364046565378,3832201363878793216,2026-08-14 20:35:00,Spot,SLX-USDC-CONVERT,,0.00000000,USDC,0.0778,0,0,USDC,0,0,0.00000000,1000.45,USDC,0.06,0,0,867,0");
        assertEquals(1, movs.size());
        assertEquals("PRELIEVO CRYPTO", movs.get(0)[5]);
        assertEquals("SLX", movs.get(0)[8]);
        assertEquals("-0.00000001", movs.get(0)[10]);
    }

    @Test
    void laConfigMappaLaCausaleSpotConActionVuota() throws Exception {
        assertEquals("SCAMBIO CRYPTO-CRYPTO", cfg().convertiCausale("Spot."));
    }
}
