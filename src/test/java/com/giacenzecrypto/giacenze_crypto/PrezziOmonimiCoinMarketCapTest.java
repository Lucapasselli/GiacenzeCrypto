package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La mappa CoinMarketCap con gli omonimi nel database, la pulizia una tantum della cache e l'esclusione dei
 * prezzi degli omonimi scartati dalla ricerca automatica. Database temporaneo, nessuna rete.
 */
class PrezziOmonimiCoinMarketCapTest {

    @TempDir
    static Path tempDir;

    private static final long ISTANTE = 1642615200000L; //2022-01-19 18:00 UTC

    @BeforeAll
    static void apre() {
        System.setProperty("prezzi.servizio.abilitato", "false");
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        Funzioni.ReteDisabilitataPerTest = true;
    }

    @AfterAll
    static void chiude() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static void cache(String simbolo, String exchange, String prezzo) throws Exception {
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ISTANTE);
            ps.setString(2, exchange);
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal(prezzo));
            ps.executeUpdate();
        }
    }

    @Test
    void mappa_tiensTuttiGliOmonimiInOrdineDiRank_eLiRiconosce() {
        DatabaseH2.GestitiCoinMarketCap_ScriviNuovaTabella(List.of(
                new String[]{"BIT", "11221", "BitDAO", "5046"},
                new String[]{"BIT", "11500", "Biconomy Exchange Token", "4096"},
                new String[]{"ACH", "6958", "Alchemy Pay", "300"}));

        List<DatabaseH2.MonetaCoinMarketCap> bit = DatabaseH2.GestitiCoinMarketCap_LeggiTutti("bit");
        assertEquals(2, bit.size());
        assertEquals(11500, bit.get(0).Id, "ordine per rank di oggi");
        assertEquals("BitDAO", bit.get(1).Nome);
        assertEquals(11500, DatabaseH2.GestitiCoinMarketCap_Leggi("BIT"));
        assertEquals(List.of("BIT"), DatabaseH2.GestitiCoinMarketCap_SimboliAmbigui());
    }

    @Test
    void pulizia_cancellaSoloLeRigheCoinMarketCapDeiSimboliIndicati() throws Exception {
        cache("TSTAMB", "CoinMarketCap", "0.00003");
        cache("TSTAMB", "binance", "1.5");
        cache("TSTUNICO", "CoinMarketCap", "2");

        assertEquals(1, DatabaseH2.PrezziCoinMarketCap_CancellaSimboli(List.of("tstamb")));
        assertEquals("binance", Prezzi.DammiPrezzoDaDatabase("TSTAMB", ISTANTE, "CoinMarketCap", "", "", 5, BigDecimal.ONE).Fonte,
                "la riga CoinMarketCap dell'omonimo non c'e' piu', resta quella dell'exchange");
        assertEquals("binance", Prezzi.DammiPrezzoDaDatabase("TSTAMB", ISTANTE, "", "", "", 5, BigDecimal.ONE).Fonte);
        assertEquals("CoinMarketCap", Prezzi.DammiPrezzoDaDatabase("TSTUNICO", ISTANTE, "", "", "", 5, BigDecimal.ONE).Fonte);
    }

    @Test
    void pulizia_piuSimboliInUnaSolaCancellazione() throws Exception {
        //Una DELETE per simbolo su decine di milioni di righe bloccava il programma per ore: ora e' una sola
        cache("TSTAMB1", "CoinMarketCap", "1");
        cache("TSTAMB2", "CoinMarketCap", "2");
        cache("TSTAMB3", "CoinMarketCap", "3");
        cache("TSTRESTA", "CoinMarketCap", "4");
        //Anche quelle col nome e degli omonimi: la scelta per volume poteva aver salvato come buona l'altra moneta
        cache("TSTAMB1", "CoinMarketCap (Biconomy Exchange Token)", "0.00001");
        cache("TSTAMB1", "CoinMarketCap omonimo (BitDAO)", "1.1");

        assertEquals(5, DatabaseH2.PrezziCoinMarketCap_CancellaSimboli(List.of("TSTAMB1", "tstamb2", "TSTAMB3")));
        assertTrue(Prezzi.DammiListaPrezziDaDatabase("TSTAMB1", ISTANTE, "", "", 60, BigDecimal.ONE).isEmpty());
        assertNull(Prezzi.DammiPrezzoDaDatabase("TSTAMB2", ISTANTE, "", "", "", 5, BigDecimal.ONE));
        assertEquals("CoinMarketCap", Prezzi.DammiPrezzoDaDatabase("TSTRESTA", ISTANTE, "", "", "", 5, BigDecimal.ONE).Fonte);
        assertEquals(0, DatabaseH2.PrezziCoinMarketCap_CancellaSimboli(List.of()));
    }

    @Test
    void ricercaAutomatica_ignoraGliOmonimiScartati() throws Exception {
        //"Scarica da tutte le fonti" salva anche l'omonimo scartato, con la sua etichetta: in ordine alfabetico
        //verrebbe dopo "CoinMarketCap", ma da solo verrebbe scelto. Non deve mai esserlo.
        cache("TSTOMO", Prezzi.EtichettaOmonimoCmc("Biconomy Exchange Token"), "0.00003");
        assertNull(Prezzi.DammiPrezzoDaDatabase("TSTOMO", ISTANTE, "", "", "", 5, BigDecimal.ONE));
        assertNull(Prezzi.DammiPrezzoDaDatabase("TSTOMO", ISTANTE, "CoinMarketCap", "", "", 5, BigDecimal.ONE));

        cache("TSTOMO", "CoinMarketCap", "1.8");
        assertEquals(0, new BigDecimal("1.8").compareTo(
                Prezzi.DammiPrezzoDaDatabase("TSTOMO", ISTANTE, "", "", "", 5, BigDecimal.ONE).prezzoUnitario));
    }

    @Test
    void sezionePrezzi_vedeAncheGliOmonimi() throws Exception {
        cache("TSTLISTA", "CoinMarketCap", "1.8");
        cache("TSTLISTA", Prezzi.EtichettaOmonimoCmc("Biconomy Exchange Token"), "0.00003");
        List<Prezzi.InfoPrezzo> lista = Prezzi.DammiListaPrezziDaDatabase("TSTLISTA", ISTANTE, "", "", 60, BigDecimal.ONE);
        assertTrue(lista.stream().anyMatch(ip -> "Biconomy Exchange Token".equals(ip.NomeMoneta)),
                "l'utente deve poter scegliere anche l'omonimo");
        assertTrue(lista.stream().allMatch(ip -> "CoinMarketCap".equals(ip.Fonte)), "la fonte si mostra nuda");
    }

    @Test
    void fonteConNome_trovataAncheChiedendoCoinMarketCap_eGliOmonimiDelFormatoPrecedenteRestanoEsclusi() throws Exception {
        cache("TSTNOME", "CoinMarketCap (BitDAO)", "1.8");
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoDaDatabase("TSTNOME", ISTANTE, "", "", "", 5, BigDecimal.ONE);
        assertEquals("CoinMarketCap", ip.Fonte, "il nome non sta nella fonte");
        assertEquals("BitDAO", ip.NomeMoneta);
        assertEquals("TSTNOME (BitDAO)", ip.Ritorna40().split("\\|")[0], "nel campo 40 il nome va accanto alla moneta");
        assertEquals("BitDAO", Prezzi.DammiPrezzoDaDatabase("TSTNOME", ISTANTE, "CoinMarketCap", "", "", 5, BigDecimal.ONE).NomeMoneta,
                "chiedendo la fonte CoinMarketCap si trovano i prezzi salvati col nome");

        cache("TSTVECCHIO", "CoinMarketCap - omonimo: Biconomy Exchange Token (id 11500)", "0.00003");
        assertNull(Prezzi.DammiPrezzoDaDatabase("TSTVECCHIO", ISTANTE, "", "", "", 5, BigDecimal.ONE));
    }

    @Test
    void sceltaDellaMoneta_cancellaNellaFinestraIPrezziCoinMarketCapDiUnAltraMoneta() throws Exception {
        cache("TSTSCELTA", "CoinMarketCap", "0.00003");                    //senza nome, formato precedente
        cache("TSTSCELTA", "CoinMarketCap (Biconomy Exchange Token)", "0.00003"); //vinceva su un'altra finestra
        cache("TSTSCELTA", "CoinMarketCap omonimo (BitDAO)", "1.8");       //omonimo scartato: resta
        cache("TSTSCELTA", "binance", "1.9");                              //altra fonte: resta

        Prezzi.CancellaAltriPrezziCoinMarketCap("TSTSCELTA",
                List.of(new Prezzi.CandelaCmc(ISTANTE - 3600000L, 1.8, 1), new Prezzi.CandelaCmc(ISTANTE + 3600000L, 1.8, 1)),
                "CoinMarketCap (BitDAO)");

        List<String> fonti = Prezzi.DammiListaPrezziDaDatabase("TSTSCELTA", ISTANTE, "", "", 60, BigDecimal.ONE)
                .stream().map(ip -> ip.Fonte + "|" + ip.NomeMoneta).sorted().toList();
        assertEquals(List.of("CoinMarketCap|BitDAO", "binance|null"), fonti);
    }

    @Test
    void campo40_ilNomeAccantoAllaMonetaRestaFuoriDalSimbolo() {
        Prezzi.InfoPrezzo ip = new Prezzi.InfoPrezzo("BIT (BitDAO)|1642615200000|1.8|CoinMarketCap");
        assertEquals("BIT", ip.Moneta, "i confronti con le gambe usano il simbolo nudo");
        assertEquals("BitDAO", ip.NomeMoneta);
        assertEquals("BIT (BitDAO)|1642615200000|1.8|CoinMarketCap", ip.Ritorna40());

        Prezzi.InfoPrezzo vecchio = new Prezzi.InfoPrezzo("ETH|1642615200000|2500|binance");
        assertEquals("ETH", vecchio.Moneta);
        assertNull(vecchio.NomeMoneta);
        assertEquals("ETH|1642615200000|2500|binance", vecchio.Ritorna40());
    }
}
