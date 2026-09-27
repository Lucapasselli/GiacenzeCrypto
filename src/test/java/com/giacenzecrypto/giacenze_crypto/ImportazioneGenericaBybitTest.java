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
 * Caratterizza {@code config/import/Bybit Spot.json} e {@code config/import/Bybit Funding.json}
 * (export "Asset Change Details", un file per conto). Righe sintetiche, nessun dato reale.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>le due config condividono exchange e wallet, così i giroconti fra conti Bybit si ignorano
 *       da entrambi i lati;</li>
 *   <li>{@code trade} + {@code tradingFee} nello stesso secondo danno uno scambio più un movimento
 *       {@code COMMISSIONI}, che nell'ordine della mappa viene dopo lo scambio (la fee è nella moneta
 *       ricevuta);</li>
 *   <li>la conversione dei piccoli saldi in MNT, con le gambe su secondi consecutivi, è un solo
 *       scambio;</li>
 *   <li>la riga Spot con Type vuoto non viene scartata ma diventa un prelievo da classificare;</li>
 *   <li>gli interessi del Funding in notazione scientifica ({@code 2.7E-7}) sono un'entrata
 *       (bug M7 su questo tracciato);</li>
 *   <li>sottoscrizioni/riscatti Earn, Launchpool e giroconti non producono movimenti.</li>
 * </ul>
 */
class ImportazioneGenericaBybitTest {

    @TempDir
    static Path tempDir;

    /** Un movimento in cripto senza prezzo fa consultare la cache prezzi/EMoney: serve il DB. */
    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Colonna controvalore sintetica in coda alla riga: niente ricerca prezzi in rete. */
    private static ConfigurazioneImport spot() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit Spot.json");
        c.colonnaValoreEuro = 6;
        return c;
    }

    private static ConfigurazioneImport funding() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit Funding.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    /** Spot: {@code Uid,Type,Coin,Amount,Wallet Balance,Time(UTC)} + [6]=controvalore sintetico */
    private static String[] rigaSpot(String type, String coin, String amount, String time) {
        return new String[]{"1", type, coin, amount, "0", time, "1.00"};
    }

    /** Funding: {@code Uid,Date & Time(UTC),Coin,QTY,Type,Account Balance,Description} + [7] */
    private static String[] rigaFunding(String time, String coin, String qty, String type, String descr) {
        return new String[]{"1", time, coin, qty, type, "0", descr, "1.00"};
    }

    private static List<String[]> importa(List<String[]> righe, ConfigurazioneImport c) {
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(righe, c)) {
            movs.addAll(ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>()));
        }
        return movs;
    }

    private static String categoria(String[] m) {
        return m[0].substring(m[0].lastIndexOf('_') + 1);
    }

    @Test
    void configurazioni_stessoExchangeEWallet() throws Exception {
        ConfigurazioneImport s = spot(), f = funding();
        assertEquals("Bybit", s.nomeExchange, "deve coincidere col nome in RW_Predefiniti");
        assertEquals(s.nomeExchange, f.nomeExchange);
        assertEquals(s.nomeWallet, f.nomeWallet);
        assertNotEquals(s.estrazione, f.estrazione, "le due config si distinguono per estrazione");
        assertEquals(6, f.colonnaCausale, "Funding: la causale e' Description, non Type");
    }

    @Test
    void tradePiuFee_unoScambioPiuCommissioneDopo() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaSpot("trade", "USDT", "-1.000000000000000000", "2024-03-01 10:00:00"));
        righe.add(rigaSpot("trade", "WEMIX", "1.290000000000000000", "2024-03-01 10:00:00"));
        righe.add(rigaSpot("tradingFee", "WEMIX", "-0.001290000000000000", "2024-03-01 10:00:00"));

        List<String[]> movs = importa(righe, spot());
        assertEquals(2, movs.size(), "uno scambio + un movimento COMMISSIONI");
        String[] scambio = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        String[] fee = movs.stream().filter(m -> categoria(m).equals("CM")).findFirst().orElseThrow();
        assertEquals("USDT", scambio[8]);
        assertEquals("WEMIX", scambio[11]);
        assertEquals(0, new BigDecimal(scambio[13]).compareTo(new BigDecimal("1.29")), "quantita' lorda");
        assertEquals("WEMIX", fee[8]);
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(scambio[0], fee[0]) < 0,
                "la fee scarica la moneta appena ricevuta: deve venire dopo lo scambio");
    }

    @Test
    void conversionePiccoliSaldi_gambeSuSecondiDiversi_unoScambio() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaSpot("convertSmallBalanceIntoBITIncreaseAssets", "MNT", "0.861709440000000000", "2024-03-02 10:30:44"));
        righe.add(rigaSpot("smallBalanceIntoBITReduceAsset", "USDT", "-0.518251350000000000", "2024-03-02 10:30:45"));

        List<String[]> movs = importa(righe, spot());
        assertEquals(1, movs.size(), "le due gambe a un secondo di distanza sono lo stesso scambio");
        assertEquals("SC", categoria(movs.get(0)));
        assertEquals("USDT", movs.get(0)[8]);
        assertEquals("MNT", movs.get(0)[11]);
    }

    @Test
    void tipoVuoto_nonScartato_prelievoDaClassificare() throws Exception {
        ConfigurazioneImport c = spot();
        assertEquals("TRASFERIMENTO-CRYPTO", c.mappaCausali.get(""), "la chiave vuota sopravvive al caricamento");
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaSpot("", "USDT", "-85.000000000000000000", "2024-03-03 19:57:12"), null, c);
        assertNotNull(movs);
        assertEquals(1, movs.size());
        assertEquals("PC", categoria(movs.get(0)));
        assertEquals("", movs.get(0)[18], "campo18 vuoto: da classificare a mano");
    }

    @Test
    void interesseInNotazioneScientifica_eUnEntrata() throws Exception {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaFunding("2024-12-31 00:19:34", "BTC", "2.70000000000E-7", "Earn",
                        "Easy Earn | Flexible Interest Distribution"), null, funding());
        assertNotNull(movs);
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("RW", categoria(m));
        assertEquals("BTC", m[11], "gamba in entrata, non in uscita");
        assertEquals(0, new BigDecimal(m[13]).compareTo(new BigDecimal("0.00000027")));
    }

    @Test
    void earnLaunchpoolEGiroconti_nessunMovimento() throws Exception {
        ConfigurazioneImport f = funding();
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2024-03-04 10:00:00", "MNT", "-100", "Earn", "Easy Earn | Flexible Subscription"));
        righe.add(rigaFunding("2024-03-05 10:00:00", "MNT", "100", "Earn", "Easy Earn | Flexible Redemption"));
        righe.add(rigaFunding("2024-03-05 10:01:00", "MNT", "-100", "Earn", "Launchpool Subscription"));
        righe.add(rigaFunding("2024-03-06 10:01:00", "MNT", "100", "Earn", "Launchpool Manual Withdrawal"));
        righe.add(rigaFunding("2024-03-06 10:02:00", "MNT", "-100", "Transfer out", "Transfer to Unified Trading Account"));
        righe.add(rigaFunding("2024-03-06 10:03:00", "USDT", "5", "Transfer in", "Transfer from Spot Account"));
        assertTrue(importa(righe, f).isEmpty());

        List<String[]> spot = new ArrayList<>();
        spot.add(rigaSpot("internalAccountTransferDeposit", "USDT", "5", "2024-03-06 10:03:00"));
        spot.add(rigaSpot("Flexible Savings Subscription", "USDT", "-5", "2024-03-06 10:05:00"));
        assertTrue(importa(spot, spot()).isEmpty());
    }
}
