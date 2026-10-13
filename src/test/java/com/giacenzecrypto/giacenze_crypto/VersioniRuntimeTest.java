package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le versioni di Node e CCXT di {@code config/varie/Runtime.json} ({@link VersioniRuntime}). */
class VersioniRuntimeTest {

    private static String json(String node, String ccxt) {
        return "{\"node\":{\"versione\":\"" + node + "\"},\"ccxt\":{\"versione\":\"" + ccxt + "\"}}";
    }

    @Test
    void ilFileDistribuitoHaLeVersioniCheEranoNelCodice() throws Exception {
        //Caratterizzazione: erano CcxtInterop.NODE_VERSION e CCXT_VERSION. Aggiornarle e' lo scopo del file: aggiornare qui.
        String contenuto = Files.readString(Path.of("config/varie/" + VersioniRuntime.NOME + ".json"));
        VersioniRuntime.Versioni v = VersioniRuntime.Interpreta(contenuto, VersioniRuntime.NOME);
        assertNotNull(v);
        assertEquals("v24.7.0", v.node());
        assertEquals("4.5.77", v.ccxt());
        assertTrue(Arrays.asList(MappeCausali.FILE_VARIE).contains(VersioniRuntime.NOME));
        assertFalse(contenuto.contains("\"centralizzato\""), "un file centralizzato non ancora pubblicato verrebbe cancellato all'avvio");
    }

    @Test
    void ccxtInteropLeggeLeVersioniDalFile() {
        assertEquals(VersioniRuntime.versioni().node(), CcxtInterop.nodeVersion());
        assertEquals(VersioniRuntime.versioni().ccxt(), CcxtInterop.ccxtVersion());
        assertTrue(CcxtInterop.nodeVersion().startsWith("v"));
    }

    @Test
    void soloNumeriNellaFormaAttesaSiAccettano() {
        assertNotNull(VersioniRuntime.Interpreta(json("v25.1.0", "5.0.1"), "x"));
        //Finiscono in un percorso, in un indirizzo e nella riga di comando di npm: niente altro che numeri
        for (String node : new String[]{"24.7.0", "v24.7", "v24.7.0-rc1", "../v24.7.0", "v24.7.0/../x", "v24.7.0 ", "latest", ""}) {
            if (node.equals("v24.7.0 ")) continue; // gli spazi attorno si tagliano
            assertNull(VersioniRuntime.Interpreta(json(node, "4.5.77"), "x"), node);
        }
        for (String ccxt : new String[]{"v4.5.77", "4.5", "4.5.77; echo", "latest", "^4.5.77", ""}) {
            assertNull(VersioniRuntime.Interpreta(json("v24.7.0", ccxt), "x"), ccxt);
        }
        assertNotNull(VersioniRuntime.Interpreta(json(" v24.7.0 ", " 4.5.77 "), "x"));
    }

    @Test
    void contenutoMancanteOGuastoNonSiInterpreta() {
        assertNull(VersioniRuntime.Interpreta(null, "x"));
        assertNull(VersioniRuntime.Interpreta("non json", "x"));
        assertNull(VersioniRuntime.Interpreta("{\"node\":{\"versione\":\"v24.7.0\"}}", "x"));
    }
}
