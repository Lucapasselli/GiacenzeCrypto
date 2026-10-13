package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le tre primitive di lettura della riga dell'import generico (2026-10-13): {@code fusoPerPeriodo},
 * {@code colonneCalcolate}, {@code regoleRiga}. Senza le chiavi la riga deve restare com'era.
 */
class ImportazioneGenericaPrimitiveRigaTest {

    @TempDir
    Path dir;

    private ConfigurazioneImport cfg(String extra) throws Exception {
        String json = "{\"nomeExchange\":\"Prova\",\"separatore\":\",\",\"righeIntestazione\":1,\"rigaIntestazione\":1,"
                + "\"autoDetectColonne\":false,\"formatoData\":\"yyyy-MM-dd HH:mm:ss\",\"fuso\":\"UTC\","
                + "\"colonne\":{\"data\":0,\"moneta\":1,\"quantita\":2,\"causale\":3}" + extra + "}";
        Path f = dir.resolve("cfg" + System.nanoTime() + ".json");
        Files.writeString(f, json);
        return ConfigurazioneImport.carica(f.toString());
    }

    private List<String[]> leggi(ConfigurazioneImport c, String... righe) throws Exception {
        Path f = dir.resolve("f" + System.nanoTime() + ".csv");
        Files.writeString(f, "data,moneta,qta,causale,prezzo\n" + String.join("\n", righe) + "\n");
        return ImportazioneGenerica.leggiCSV(f.toString(), c);
    }

    private static long ms(int a, int m, int g, int h, ZoneId z) {
        return LocalDateTime.of(a, m, g, h, 0).atZone(z).toInstant().toEpochMilli();
    }

    @Test
    void senzaLeChiaviLaRigaResta() throws Exception {
        List<String[]> r = leggi(cfg(""), "2025-01-01 10:00:00,BTC,0.5,Buy,100");
        assertArrayEquals(new String[]{"2025-01-01 10:00:00", "BTC", "0.5", "Buy", "100"}, r.get(0));
    }

    @Test
    void ilFusoDelPeriodoValeSoloPerLeSueDate() throws Exception {
        ConfigurazioneImport c = cfg(",\"fusoPerPeriodo\":[{\"da\":\"2025-01-01\",\"a\":\"2025-12-31\",\"fuso\":\"Europe/Rome\"}]");
        assertEquals(ms(2023, 6, 1, 10, ZoneOffset.UTC), c.convertiDataInMillis("2023-06-01 10:00:00"));
        assertEquals(ms(2025, 3, 12, 10, ZoneId.of("Europe/Rome")), c.convertiDataInMillis("2025-03-12 10:00:00"));
        //estremo compreso, giorno dopo no
        assertEquals(ms(2025, 12, 31, 23, ZoneId.of("Europe/Rome")), c.convertiDataInMillis("2025-12-31 23:00:00"));
        assertEquals(ms(2026, 1, 1, 0, ZoneOffset.UTC), c.convertiDataInMillis("2026-01-01 00:00:00"));
    }

    @Test
    void periodiNonValidiSiRifiutano() {
        assertThrows(Exception.class, () -> cfg(",\"fusoPerPeriodo\":[{\"da\":\"2025-02-01\",\"a\":\"2025-01-01\",\"fuso\":\"UTC\"}]"));
        assertThrows(Exception.class, () -> cfg(",\"fusoPerPeriodo\":[{\"da\":\"2025-01-01\",\"a\":\"2025-12-31\",\"fuso\":\"Marte/Base\"}]"));
        assertThrows(Exception.class, () -> cfg(",\"fusoPerPeriodo\":[{\"da\":\"2025-01-01\",\"a\":\"2025-06-30\",\"fuso\":\"UTC\"},"
                + "{\"da\":\"2025-06-30\",\"a\":\"2025-12-31\",\"fuso\":\"UTC+1\"}]"));
    }

    @Test
    void leColonneCalcolateSiAccodanoInOrdine() throws Exception {
        ConfigurazioneImport c = cfg(",\"colonneCalcolate\":[{\"colonna\":5,\"operazione\":\"prodotto\",\"a\":2,\"b\":4},"
                + "{\"colonna\":6,\"operazione\":\"quoziente\",\"a\":5,\"b\":2},{\"colonna\":7,\"operazione\":\"differenza\",\"a\":2,\"b\":2}]");
        String[] r = leggi(c, "2025-01-01 10:00:00,BTC,0.000000001,Buy,0.0000025").get(0);
        assertEquals("0.0000000000000025", r[5]); //mai notazione scientifica (bug M7)
        assertEquals("0.0000025", r[6]);
        assertEquals("0", r[7]);
        String[] v = leggi(c, "2025-01-01 10:00:00,BTC,,Buy,3").get(0);
        assertEquals("", v[5]);
        assertEquals("", v[6]);
        assertEquals("", ImportazioneGenerica.calcola("quoziente", "1", "0"));
        assertEquals("0.3333333333333333333333333333333333", ImportazioneGenerica.calcola("quoziente", "1", "3"));
        assertThrows(Exception.class, () -> cfg(",\"colonneCalcolate\":[{\"colonna\":5,\"operazione\":\"potenza\",\"a\":2,\"b\":4}]"));
    }

    @Test
    void leRegoleDiRigaImpostanoOScartano() throws Exception {
        ConfigurazioneImport c = cfg(",\"regoleRiga\":[{\"colonna\":4,\"vuota\":true,\"scarta\":true},"
                + "{\"colonna\":3,\"contiene\":\"launch\",\"imposta\":{\"colonna\":3,\"valore\":\"AIRDROP\"}},"
                + "{\"colonna\":1,\"uguale\":\"btc\",\"imposta\":{\"colonna\":6,\"valore\":\"X\"}}]");
        List<String[]> r = leggi(c, "2025-01-01 10:00:00,ETH,1,LaunchPool reward,5", "2025-01-01 11:00:00,BTC,1,Buy,",
                "2025-01-01 12:00:00,BTC,1,Buy,7");
        assertEquals(2, r.size());
        assertEquals("AIRDROP", r.get(0)[3]);
        assertEquals(5, r.get(0).length);
        assertEquals("X", r.get(1)[6]);
        assertEquals("", r.get(1)[5]);
    }

    @Test
    void regoleNonValideSiRifiutano() {
        //sulla data
        assertThrows(Exception.class, () -> cfg(",\"regoleRiga\":[{\"colonna\":1,\"uguale\":\"BTC\",\"imposta\":{\"colonna\":0,\"valore\":\"2020-01-01 00:00:00\"}}]"));
        //due condizioni, nessuna azione, due azioni
        assertThrows(Exception.class, () -> cfg(",\"regoleRiga\":[{\"colonna\":1,\"uguale\":\"BTC\",\"vuota\":true,\"scarta\":true}]"));
        assertThrows(Exception.class, () -> cfg(",\"regoleRiga\":[{\"colonna\":1,\"uguale\":\"BTC\"}]"));
        assertThrows(Exception.class, () -> cfg(",\"regoleRiga\":[{\"colonna\":1,\"uguale\":\"BTC\",\"scarta\":true,\"imposta\":{\"colonna\":3,\"valore\":\"A\"}}]"));
    }
}
