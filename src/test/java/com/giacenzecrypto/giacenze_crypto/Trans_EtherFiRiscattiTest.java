package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Abbinamento dei riscatti in due tempi di ether.fi ({@link Trans_EtherFi#Abbina}): la richiesta e il pagamento si
 * legano dalla chiave {@code contratto + numero} che le loro causali portano, e diventano uno scambio differito. Indirizzi
 * e numeri inventati.
 */
class Trans_EtherFiRiscattiTest {

    @TempDir
    static Path tempDir;

    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String VAULT = "0x888800000000000000000000000000000000000e";

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

    private static String[] riga(String id, String data, String causale, String moneta, String qta) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = W + " (OP)";
        v[4] = "Wallet";
        boolean uscita = id.endsWith("_PC");
        v[5] = uscita ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        v[7] = causale;
        if (uscita) {
            v[8] = moneta;
            v[9] = "Crypto";
            v[10] = "-" + qta;
        } else {
            v[11] = moneta;
            v[12] = "Crypto";
            v[13] = qta;
        }
        v[15] = "23.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(id, v);
        return v;
    }

    private static String chiave(String n) {
        return "Midas, contratto " + VAULT + ", n. " + n;
    }

    @Test
    void richiestaEPagamentoConLaStessaChiaveDiventanoUnoScambioDifferito() {
        String[] r = riga("20260310213000_" + W + " (OP)_001_001_PC", "2026-03-10 21:30:00",
                Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + chiave("7"), "weEUR", "20");
        String[] p = riga("20260313120000_" + W + " (OP)_001_001_DC", "2026-03-13 12:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + chiave("7"), "EURC", "20.5");
        String idRichiesta = r[0];
        //un'altra richiesta, ancora aperta: non ha pagamento
        String[] aperta = riga("20260311100000_" + W + " (OP)_001_001_PC", "2026-03-11 10:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + chiave("1800"), "weEUR", "5");

        assertEquals(1, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));

        //prelievo e deposito rinumerati + trasferimento, scambio e trasferimento generati, piu' la richiesta aperta
        assertEquals(6, MappaCryptoWallet.size());
        assertFalse(MappaCryptoWallet.containsKey(idRichiesta), "la richiesta e' stata rinumerata");
        long scambi = MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_SC")).count();
        assertEquals(1, scambi);
        String[] scambio = MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_SC")).findFirst().orElseThrow();
        assertEquals("weEUR", scambio[8]);
        assertEquals("EURC", scambio[11]);
        assertEquals("Midas", scambio[4], "il sotto-wallet dei movimenti generati porta il nome del prodotto");
        assertTrue(aperta[18].isBlank(), "una richiesta ancora aperta non si tocca");
        assertTrue(MappaCryptoWallet.containsKey(aperta[0]));
        //Rifatto non cambia nulla
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertEquals(6, MappaCryptoWallet.size());
    }

    private static final String DEST = "0xbbbb000000000000000000000000000000000002";
    private static final String WBTC = "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599";

    private static String causaleSwap(long scadenza) {
        return Trans_EtherFi.CAUSALE_SWAP_FRA_CATENE + " verso ETH, destinatario " + DEST + ", token " + WBTC
                + ", almeno 295000 unita', scadenza " + scadenza;
    }

    private static String[] depositoSu(String wallet, String id, String data, String moneta, String token, String qta) {
        String[] d = riga(id, data, "Deposito", moneta, qta);
        d[3] = wallet;
        d[28] = token;
        return d;
    }

    @Test
    void loSwapFraCateneSiAbbinaAlDepositoSulWalletDiDestinazione() {
        long scadenza = FunzioniDate.ConvertiDatainLongMinuto("2026-03-11 23:15:00") / 1000;
        String[] swap = riga("20260310231500_" + W + " (OP)_001_001_PC", "2026-03-10 23:15:00", causaleSwap(scadenza), "USDC", "240");
        //un deposito dello stesso token ma di un altro wallet, uno fuori tempo e quello giusto
        depositoSu("0xcccc000000000000000000000000000000000003 (ETH)", "20260310232000_0xcccc (ETH)_001_001_DC", "2026-03-10 23:20:00", "WBTC", WBTC, "0.002");
        depositoSu(DEST + " (ETH)", "20260305100000_" + DEST + " (ETH)_001_001_DC", "2026-03-05 10:00:00", "WBTC", WBTC, "0.001");
        String[] giusto = depositoSu(DEST + " (ETH)", "20260310232500_" + DEST + " (ETH)_001_001_DC", "2026-03-10 23:25:00", "WBTC", WBTC, "0.003");

        assertEquals(1, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));

        String[] scambio = MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_SC")).findFirst().orElseThrow();
        assertEquals("USDC", scambio[8]);
        assertEquals("WBTC", scambio[11]);
        assertEquals("0.003", scambio[13].replaceFirst("^-", ""));
        assertTrue(giusto[18].startsWith("DTW"), giusto[18]);
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)), "rifatto non cambia nulla");
    }

    @Test
    void loSwapFraCateneNonSiAbbinaSeIWalletSonoInGruppiDiversi() {
        long scadenza = FunzioniDate.ConvertiDatainLongMinuto("2026-03-11 23:15:00") / 1000;
        String[] swap = riga("20260310231500_" + W + " (OP)_001_001_PC", "2026-03-10 23:15:00", causaleSwap(scadenza), "USDC", "240");
        depositoSu(DEST + " (ETH)", "20260310232500_" + DEST + " (ETH)_001_001_DC", "2026-03-10 23:25:00", "WBTC", WBTC, "0.003");
        DatabaseH2.Pers_GruppoWallet_Scrivi(W + " (OP)", "Wallet 117");
        DatabaseH2.Pers_GruppoWallet_Scrivi(DEST + " (ETH)", "Wallet 5");
        try {
            assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
            assertTrue(swap[18].isBlank());
            //lo stesso gruppo: si abbina; un wallet senza gruppo prende quello del Safe
            DatabaseH2.Pers_GruppoWallet_Scrivi(DEST + " (ETH)", "Wallet 117");
            assertEquals(1, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        } finally {
            DatabaseH2.Pers_GruppoWallet_Scrivi(W + " (OP)", "Wallet 99");
            DatabaseH2.Pers_GruppoWallet_Scrivi(DEST + " (ETH)", "Wallet 99");
        }
    }

    @Test
    void loSwapFraCateneSenzaIlWalletDiDestinazioneRestaUnPrelievo() {
        long scadenza = FunzioniDate.ConvertiDatainLongMinuto("2026-03-11 23:15:00") / 1000;
        String[] swap = riga("20260310231500_" + W + " (OP)_001_001_PC", "2026-03-10 23:15:00", causaleSwap(scadenza), "USDC", "240");
        //deposito dopo la scadenza: non e' quello
        depositoSu(DEST + " (ETH)", "20260315100000_" + DEST + " (ETH)_001_001_DC", "2026-03-15 10:00:00", "WBTC", WBTC, "0.004");
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertTrue(swap[18].isBlank());
    }

    @Test
    void numeriDiversiOContrattiDiversiNonSiAbbinano() {
        riga("20260310213000_" + W + " (OP)_001_001_PC", "2026-03-10 21:30:00",
                Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + chiave("7"), "weEUR", "20");
        riga("20260313120000_" + W + " (OP)_001_001_DC", "2026-03-13 12:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + chiave("8"), "EURC", "20.5");
        riga("20260314120000_" + W + " (OP)_002_001_DC", "2026-03-14 12:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + "Midas, contratto 0x777700000000000000000000000000000000000f, n. 7",
                "EURC", "20");
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertEquals(3, MappaCryptoWallet.size());
    }

    @Test
    void unaRichiestaAnnullataOLaStessaMonetaNonDiventanoUnoScambio() {
        riga("20260310213000_" + W + " (OP)_001_001_PC", "2026-03-10 21:30:00",
                Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + chiave("7"), "weEUR", "20");
        //le quote tornano all'utente: stessa moneta, e la causale dice annullata
        riga("20260310223000_" + W + " (OP)_001_001_DC", "2026-03-10 22:30:00",
                Trans_EtherFi.CAUSALE_RISCATTO_ANNULLATO + chiave("7"), "weEUR", "20");
        //pagamento nella stessa moneta delle quote: non e' una permuta
        riga("20260311100000_" + W + " (OP)_001_001_PC", "2026-03-11 10:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + chiave("1800"), "weEUR", "5");
        riga("20260312100000_" + W + " (OP)_001_001_DC", "2026-03-12 10:00:00",
                Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + chiave("1800"), "weEUR", "5.1");
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertEquals(4, MappaCryptoWallet.size());
    }
}
