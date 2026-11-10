package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Il TradingSafe di ether.fi (secondo conto, su Ethereum): scoperta degli indirizzi candidati, registrazione, voci da
 * aggiungere all'importazione e abbinamento dello swap che parte da lui e arriva sul Safe. Indirizzi e importi inventati.
 */
class Trans_EtherFiTradingSafeTest {

    @TempDir
    static Path tempDir;

    private static final String SAFE = "0xaaaa000000000000000000000000000000000001";
    private static final String WBTC = "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599";

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

    private static String[] riga(String wallet, String id, String data, String causale, String moneta, String qta, String valore) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = wallet;
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
        v[15] = valore;
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(id, v);
        return v;
    }

    /** Un TradingSafe registrato per il Safe, con l'indirizzo scelto dal test. */
    private static String registra(String finale) {
        String ts = "0xbbbb00000000000000000000000000000000" + finale;
        Trans_EtherFi.RegistraTradingSafe(SAFE, ts);
        return ts;
    }

    private static String idUscita(String ts, String quando) {
        return quando + "_" + ts + " (ETH)_001_001_PC";
    }

    private static String idDeposito(String quando) {
        return quando + "_" + SAFE + " (OP)_001_001_DC";
    }

    @Test
    void loSwapCheParteDalTradingSafeSiAbbinaAlDepositoSulSafe() {
        String ts = registra("0001");
        String[] uscita = riga(ts + " (ETH)", idUscita(ts, "20250210100000"), "2025-02-10 10:00:00", "transfer", "WBTC", "0.003", "230.00");
        uscita[8] = "WBTC";
        String[] deposito = riga(SAFE + " (OP)", idDeposito("20250210100100"), "2025-02-10 10:01:00", "Deposito", "USDC", "229", "229.00");

        assertEquals(1, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)));

        String[] scambio = MappaCryptoWallet.values().stream().filter(v -> v[0].endsWith("_SC")).findFirst().orElseThrow();
        assertEquals("WBTC", scambio[8]);
        assertEquals("USDC", scambio[11]);
        assertTrue(deposito[18].startsWith("DTW") || MappaCryptoWallet.values().stream().anyMatch(v -> v[0].endsWith("_SC")));
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)), "rifatto non cambia nulla");
    }

    @Test
    void conPiuDepositiPossibiliNonSiIndovina() {
        String ts = registra("0002");
        String[] uscita = riga(ts + " (ETH)", idUscita(ts, "20261104100000"), "2026-11-04 10:00:00", "transfer", "WBTC", "0.003", "230.00");
        riga(SAFE + " (OP)", idDeposito("20261104100100"), "2026-11-04 10:01:00", "Deposito", "USDC", "229", "229.00");
        riga(SAFE + " (OP)", idDeposito("20261104100200"), "2026-11-04 10:02:00", "Deposito", "EURC", "200", "231.00");

        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)));
        assertTrue(uscita[18].isBlank(), "restano da classificare");
    }

    @Test
    void dueUsciteContendentiLoStessoDepositoNonSiAbbinano() {
        String ts = registra("0003");
        riga(ts + " (ETH)", idUscita(ts, "20261105100000"), "2026-11-05 10:00:00", "transfer", "WBTC", "0.003", "230.00");
        riga(ts + " (ETH)", idUscita(ts, "20261105100100"), "2026-11-05 10:01:00", "transfer", "WBTC", "0.003", "230.00");
        riga(SAFE + " (OP)", idDeposito("20261105100200"), "2026-11-05 10:02:00", "Deposito", "USDC", "229", "229.00");

        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)));
    }

    @Test
    void nonSiAbbinaSeCausaleMonetaTempoOValoreNonQuadrano() {
        String ts = registra("0004");
        String[] uscita = riga(ts + " (ETH)", idUscita(ts, "20261106100000"), "2026-11-06 10:00:00", "transfer", "WBTC", "0.003", "230.00");
        //non e' un deposito generico (e' una ricarica), stessa moneta, troppo tardi, valore doppio
        riga(SAFE + " (OP)", idDeposito("20261106100100"), "2026-11-06 10:01:00", Trans_EtherFi.CAUSALE_RICARICA + " da ARB", "USDC", "229", "229.00");
        riga(SAFE + " (OP)", idDeposito("20261106100200"), "2026-11-06 10:02:00", "Deposito", "WBTC", "0.003", "230.00");
        riga(SAFE + " (OP)", idDeposito("20261106113000"), "2026-11-06 11:30:00", "Deposito", "USDC", "229", "229.00");
        riga(SAFE + " (OP)", idDeposito("20261106100300"), "2026-11-06 10:03:00", "Deposito", "USDC", "460", "460.00");

        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)));
        assertTrue(uscita[18].isBlank());
    }

    @Test
    void unTradingSafeNonRegistratoNonSiAbbina() {
        String ts = "0xbbbb000000000000000000000000000000009999";
        riga(ts + " (ETH)", idUscita(ts, "20261107100000"), "2026-11-07 10:00:00", "transfer", "WBTC", "0.003", "230.00");
        riga(SAFE + " (OP)", idDeposito("20261107100100"), "2026-11-07 10:01:00", "Deposito", "USDC", "229", "229.00");
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(SAFE)));
    }

    @Test
    void ICandidatiSonoIDestinatariDegliSwapEDelleUsciteDelSafe() {
        String dest = "0xbbbb000000000000000000000000000000000005";
        String altro = "0xdddd000000000000000000000000000000000006";
        String[] swap = riga(SAFE + " (OP)", "20250215100000_" + SAFE + " (OP)_001_001_PC", "2025-02-15 10:00:00",
                Trans_EtherFi.CAUSALE_SWAP_FRA_CATENE + " verso ETH, destinatario " + dest + ", token " + WBTC
                + ", almeno 300000 unita', scadenza 1700000000", "USDC", "240", "240.00");
        swap[30] = dest;
        String[] prelievo = riga(SAFE + " (OP)", "20250216100000_" + SAFE + " (OP)_001_001_PC", "2025-02-16 10:00:00", "Prelievo", "USDC", "500", "450.00");
        prelievo[30] = altro;
        //un deposito e un'uscita di un altro wallet non contano
        String[] estraneo = riga("0xeeee000000000000000000000000000000000007 (OP)", "20250216100001_0xeeee (OP)_001_001_PC", "2025-02-16 10:00:01", "Prelievo", "USDC", "1", "1.00");
        estraneo[30] = "0xffff000000000000000000000000000000000008";

        Set<String> c = Trans_EtherFi.CandidatiTradingSafe(MappaCryptoWallet, SAFE, List.of());
        assertEquals(Set.of(dest, altro), c);
    }

    @Test
    void ICandidatiLeggonoAncheLeTransazioniAppenaScaricate() {
        String dest = "0xbbbb000000000000000000000000000000000009";
        Trans_EtherFi.Token usdc = new Trans_EtherFi.Token("0xusdc", "USDC", 6);
        Trans_EtherFi.Movimento m = new Trans_EtherFi.Movimento(null,
                Trans_EtherFi.CAUSALE_SWAP_FRA_CATENE + " verso ETH, destinatario " + dest + ", token " + WBTC
                        + ", almeno 1 unita', scadenza 1700000000",
                usdc, java.math.BigInteger.valueOf(240000000L), null, null, "", false, false);
        Trans_EtherFi.Transazione t = new Trans_EtherFi.Transazione("0xhash", 1L, 1L, List.of(m));
        assertEquals(Set.of(dest), Trans_EtherFi.CandidatiTradingSafe(MappaCryptoWallet, SAFE, List.of(t)));
    }

    @Test
    void ilTradingSafeRegistratoSegueSempreIlSuoSafe() {
        String ts = registra("0010");
        String[] precedente = riga(ts + " (ETH)", idUscita(ts, "20261101100000"), "2026-11-01 10:00:00", "transfer", "WBTC", "1", "1.00");
        precedente[23] = "1000000";
        assertTrue(Trans_EtherFi.isTradingSafeRegistrato(ts));
        assertTrue(Trans_EtherFi.isTradingSafeRegistrato(ts.toUpperCase().replace("0X", "0x")));
        assertEquals(List.of(ts), Trans_EtherFi.TradingSafeDi(SAFE).stream().filter(x -> x.equals(ts)).toList());

        //col Safe fra le voci: il TradingSafe segue, con il suo ultimo blocco
        List<String> voci = List.of(SAFE + ";100;OP");
        List<String> aggiunte = Trans_EtherFi.VociTradingSafe(voci, MappaCryptoWallet);
        assertTrue(aggiunte.contains(ts + ";1000000;ETH"), aggiunte.toString());
        //senza il Safe fra le voci no, e non si duplica se c'e' gia'
        assertFalse(Trans_EtherFi.VociTradingSafe(List.of("0x1234;1;OP"), MappaCryptoWallet).contains(ts + ";1000000;ETH"));
        assertFalse(Trans_EtherFi.VociTradingSafe(List.of(SAFE + ";100;OP", ts + ";5;ETH"), MappaCryptoWallet).stream().anyMatch(x -> x.startsWith(ts)));
        //non e' fra i wallet dell'utente
        assertNull(DatabaseH2.Pers_Wallets_LeggiTabella().get(ts + "_ETH"));
    }

    @Test
    void laRegistrazioneMetteIlWalletNelGruppoDelSafeMaNonFraIWalletDellUtente() {
        DatabaseH2.Pers_GruppoWallet_Scrivi(SAFE + " (OP)", "Wallet 117");
        String ts = registra("0011");
        try {
            assertNull(DatabaseH2.Pers_Wallets_LeggiTabella().get(ts + "_ETH"));
            assertEquals("Wallet 117", DatabaseH2.Pers_GruppoWallet_Leggi(ts + " (ETH)", false));
            //registrare di nuovo non duplica
            Trans_EtherFi.RegistraTradingSafe(SAFE, ts);
            assertEquals(1, Trans_EtherFi.TradingSafeRegistrati().keySet().stream().filter(x -> x.equals(ts)).count());
        } finally {
            DatabaseH2.Pers_GruppoWallet_Scrivi(SAFE + " (OP)", "Wallet 99");
        }
    }
}
