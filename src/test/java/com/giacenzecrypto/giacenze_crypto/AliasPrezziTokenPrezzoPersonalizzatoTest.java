package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Un prezzo personalizzato inserito a mano per un token con alias deve continuare a valere.
 * <p>Il prezzo si salva per address+rete e col simbolo grezzo ("WETH"), mentre l'alias di
 * {@link Principale#Mappa_AddressRete_Nome} rinomina il token ("ETH") e ne toglie l'address prima di
 * cercare il prezzo. Senza {@link Prezzi#PrezzoPersonalizzatoTokenConAlias} la ricerca per simbolo
 * non ritrovava la riga, e il prezzo di fine anno scelto dall'utente spariva dal quadro RW senza avvisi.
 * Il difetto c'era già con i 12 alias scritti nel codice; con l'elenco allargato e aggiornato da
 * GitHub sarebbe diventato frequente.
 */
class AliasPrezziTokenPrezzoPersonalizzatoTest {

    @TempDir
    static Path tempDir;

    private static final long FINE_2025 = 1767222000000L;
    private static final String WETH_BASE = "0x4200000000000000000000000000000000000006";

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        //CompilaMappaChain e non solo AliasPrezziToken.Carica(): InserisciPrezzoPresonalizzato azzera una
        //rete che non trova in Mappa_ChainExplorer, e il prezzo finirebbe salvato per nome invece che per address
        VarCondivise.CompilaMappaChain();
        VarCondivise.CompilaMappaRetiSupportate();
        assertTrue(DatabaseH2.InserisciPrezzoPresonalizzato(FINE_2025, "Personalizzato (TUTTI)",
                "WETH", "2500.5", "BASE", WETH_BASE, "TUTTI", FINE_2025));
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @Test
    void ilPrezzoPersonalizzatoPerAddressVinceSullAlias() {
        Moneta weth = new Moneta();
        weth.Moneta = "WETH";
        weth.Tipo = "Crypto";
        weth.Qta = "2";
        weth.MonetaAddress = WETH_BASE;
        weth.Rete = "BASE";

        //Il prezzo personalizzato si trova prima di qualunque ricerca esterna: nessuna chiamata di rete
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(weth, null, FINE_2025, "BASE", "");

        assertNotNull(ip, "il prezzo personalizzato salvato per address deve essere ritrovato");
        assertEquals(0, new BigDecimal("2500.5").compareTo(ip.prezzoUnitario));
        assertEquals(0, new BigDecimal("5001.0").compareTo(ip.prezzoQta));
        assertEquals("ETH", ip.Moneta, "il token resta rinominato dall'alias, come per gli altri prezzi");
    }

    @Test
    void senzaAliasIlSupportoNonFaNulla() {
        Moneta m = new Moneta();
        m.Moneta = "ETH";
        m.Qta = "1";
        assertNull(Prezzi.PrezzoPersonalizzatoTokenConAlias(m, null, "BASE", FINE_2025, ""));
    }
}
