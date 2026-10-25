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
    /** 31/12/2024 23:59: MATIC ed ETH2 hanno un alias stessoPrezzo (POL, ETH) con dal anteriore. */
    private static final long FINE_2024 = FunzioniDate.ConvertiDatainLongMinuto("2024-12-31 23:59");

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
        //Prezzi per simbolo, senza address: uno sul simbolo del movimento, uno sulla moneta di prezzo
        assertTrue(DatabaseH2.InserisciPrezzoPresonalizzato(FINE_2024, "Personalizzato (TUTTI)",
                "MATIC", "0.5", "", "", "TUTTI", FINE_2024));
        assertTrue(DatabaseH2.InserisciPrezzoPresonalizzato(FINE_2024, "Personalizzato (TUTTI)",
                "ETH", "3200", "", "", "TUTTI", FINE_2024));
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

    private static Moneta MonetaSenzaAddress(String simbolo, String qta) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Tipo = "Crypto";
        m.Qta = qta;
        return m;
    }

    @Test
    void ilPrezzoPersonalizzatoPerSimboloVinceSullAliasStessoPrezzo() {
        //Il prezzo e' salvato come "MATIC", l'alias porta la ricerca su "POL": prima del 2026-10-24 la lettura
        //del personalizzato avveniva dopo la sostituzione e il prezzo messo a mano spariva
        assertEquals("POL", AliasPrezziToken.StessoPrezzo("MATIC", FINE_2024), "premessa: l'alias vale a fine 2024");
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(MonetaSenzaAddress("MATIC", "10"), null, FINE_2024, "", "");

        assertNotNull(ip, "il prezzo personalizzato salvato col simbolo del movimento deve essere ritrovato");
        assertEquals(0, new BigDecimal("0.5").compareTo(ip.prezzoUnitario));
        assertEquals(0, new BigDecimal("5.0").compareTo(ip.prezzoQta));
    }

    @Test
    void ilPrezzoPersonalizzatoVecchioPerSimboloVinceSullAliasStessoPrezzo() {
        //I personalizzati inseriti fino a novembre 2025 stanno nell'archivio orario XXXEUR, mai migrato, e anche
        //loro col simbolo del movimento: chi aveva messo a mano MATIC per la dichiarazione 2024 deve ritrovarlo
        long meta2025 = FunzioniDate.ConvertiDatainLongMinuto("2025-06-30 12:00");
        DatabaseH2.OLD_XXXEUR_Scrivi(FunzioniDate.ConvertiDatadaLongallOra(meta2025) + " MATIC", "0.3", true);
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(MonetaSenzaAddress("MATIC", "10"), null, meta2025, "", "");

        assertNotNull(ip, "il prezzo personalizzato dell'archivio orario deve essere ritrovato");
        assertEquals(0, new BigDecimal("0.3").compareTo(ip.prezzoUnitario));
        assertEquals("Personalizzato (Old)", ip.Fonte);
    }

    @Test
    void ilPrezzoPersonalizzatoVecchioPerAddressVinceSullAlias() throws Exception {
        //I personalizzati per address inseriti fino a novembre 2025 stanno nell'archivio orario Prezzo_ora_Address_Chain.
        //WETH su Arbitrum ha un alias con dal 2026-01-01 che dal 2026-10-25 non conta piu': a fine 2024 l'alias vale,
        //e il prezzo messo a mano per la dichiarazione 2024 deve restare quello
        String wethArb = "0x82af49447d8a07e3bd95bd0d56f35241523fbab1";
        assertEquals("ETH", AliasPrezziToken.Alias(wethArb, "ARB", FINE_2024), "premessa: l'alias vale a fine 2024");
        String chiave = (FunzioniDate.ConvertiDatadaLongallOra(FINE_2024) + "_" + wethArb + "_ARB").toUpperCase();
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "INSERT INTO Prezzo_ora_Address_Chain (ora_address_chain, prezzo) VALUES (?, ?)")) {
            ps.setString(1, chiave);
            ps.setString(2, "3100");
            ps.executeUpdate();
        }
        Moneta weth = new Moneta();
        weth.Moneta = "WETH";
        weth.Tipo = "Crypto";
        weth.Qta = "2";
        weth.MonetaAddress = wethArb;
        weth.Rete = "ARB";
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(weth, null, FINE_2024, "ARB", "");

        assertNotNull(ip, "il prezzo personalizzato dell'archivio orario per address deve essere ritrovato");
        assertEquals(0, new BigDecimal("3100").compareTo(ip.prezzoUnitario));
        assertEquals(0, new BigDecimal("6200").compareTo(ip.prezzoQta));
        assertEquals("Personalizzato (Old)", ip.Fonte);
    }

    @Test
    void ilPrezzoPersonalizzatoDellaMonetaDiPrezzoValeAncheperLAlias() {
        //Nessun personalizzato su "ETH2": vale quello messo su "ETH", come prima della correzione
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoInfoTransazione(MonetaSenzaAddress("ETH2", "0.1"), null, FINE_2024, "", "");

        assertNotNull(ip);
        assertEquals(0, new BigDecimal("3200").compareTo(ip.prezzoUnitario));
        assertEquals(0, new BigDecimal("320.0").compareTo(ip.prezzoQta));
    }

    @Test
    void senzaAliasIlSupportoNonFaNulla() {
        Moneta m = new Moneta();
        m.Moneta = "ETH";
        m.Qta = "1";
        assertNull(Prezzi.PrezzoPersonalizzatoTokenConAlias(m, null, "BASE", FINE_2025, ""));
    }
}
