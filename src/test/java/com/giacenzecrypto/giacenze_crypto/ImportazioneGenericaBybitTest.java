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
 * Caratterizza le quattro config Bybit di {@code config/import/} (Spot, Funding, Derivati, UTA:
 * export "Asset Change Details", un file per conto). Righe sintetiche, nessun dato reale.
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
 *   <li>sottoscrizioni/riscatti Earn, Launchpool e giroconti non producono movimenti;</li>
 *   <li>Funding dal 2026: 'Convert' e' un acquisto (gamba in euro) o uno scambio, 'Airdrop Bonus' un
 *       airdrop a se', un prelievo seguito da 'Withdrawal Canceled' non produce movimenti;</li>
 *   <li>derivati (conto Derivati e perpetui dell'UTA): PnL e funding sommati per giorno sulla
 *       variazione di saldo, gia' netta di commissioni, quindi nessun movimento COMMISSIONI;</li>
 *   <li>UTA: scambio spot con la gamba in entrata riportata al lordo e la fee a parte, conversioni
 *       '--' in MNT come scambio.</li>
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
        //Niente rete: una ricerca prezzi online scaricava Node dentro la cartella temporanea mentre JUnit la
        //cancellava (errore di pulizia a fine test). Le righe sintetiche hanno gia' il loro controvalore.
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

    private static ConfigurazioneImport fundingEU() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit EU Funding.json");
        c.colonnaValoreEuro = 7;
        return c;
    }

    private static ConfigurazioneImport utaEU() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit EU UTA.json");
        c.colonnaValoreEuro = 15;
        return c;
    }

    private static ConfigurazioneImport derivati() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit Derivati.json");
        c.colonnaValoreEuro = 16;
        return c;
    }

    private static ConfigurazioneImport uta() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit UTA.json");
        c.colonnaValoreEuro = 15;
        return c;
    }

    /** Derivati: {@code Time,Currency,Contract,Type,Direction,Quantity,Position,Filled Price,Funding,
     *  Fee Paid,Cash Flow,Change,Wallet Balance,Fee Rate,Trade ID,Order ID} + [16] */
    private static String[] rigaDerivati(String time, String coin, String type, String fee, String change) {
        return new String[]{time, coin, "BTCUSDT", type, "Open Long", "1", "1", "1", "0", fee, "0", change,
            "0", "0", "", "", "1.00"};
    }

    /** UTA: {@code Uid,Currency,Contract,Type,Direction,Quantity,Position,Filled Price,Funding,Fee Paid,
     *  Cash Flow,Change,Wallet Balance,Action,Time(UTC)} + [15] */
    private static String[] rigaUta(String coin, String type, String fee, String change, String action, String time) {
        return new String[]{"1", coin, "", type, "--", "0", "0", "0", "0", fee, "0", change, "0", action, time, "1.00"};
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

    @Test
    void configurazioniNuove_stessoExchangeEWallet() throws Exception {
        ConfigurazioneImport s = spot(), d = derivati(), u = uta();
        for (ConfigurazioneImport c : List.of(d, u)) {
            assertEquals(s.nomeExchange, c.nomeExchange);
            assertEquals(s.nomeWallet, c.nomeWallet);
        }
        assertEquals(4, java.util.stream.Stream.of(s, funding(), d, u).map(c -> c.estrazione).distinct().count());
    }

    @Test
    void spot2022_launchpadEBot_nonIgnorati() throws Exception {
        List<String[]> movs = importa(List.of(
                rigaSpot("commitmentForLaunchpad", "BIT", "-1.5", "2022-10-05 11:00:00"),
                rigaSpot("botTradingReduceAsset", "USDT", "-25", "2022-12-18 16:00:00"),
                rigaSpot("userDeposit", "USDT", "40", "2022-12-12 10:00:00")), spot());
        assertEquals(3, movs.size(), "il BIT impegnato nel Launchpad non torna: non si puo' ignorare");
        assertEquals(List.of("DC", "PC", "PC"), movs.stream().map(ImportazioneGenericaBybitTest::categoria).sorted().toList());
    }

    @Test
    void launchpad_bitImpegnatoETokenRicevuto_unoScambio() throws Exception {
        //Dati reali 2022: il token arriva nello stesso secondo o a un secondo dal BIT impegnato.
        List<String[]> movs = importa(List.of(
                rigaSpot("commitmentForLaunchpad", "BIT", "-0.110131", "2022-04-27 11:08:26"),
                rigaSpot("airdropAssetIncrease", "APEX", "2.6796", "2022-04-27 11:08:27"),
                rigaSpot("commitmentForLaunchpad", "BIT", "-0.686780", "2022-07-19 11:11:08"),
                rigaSpot("airdropAssetIncrease", "SLG", "11.8615", "2022-07-19 11:11:08")), spot());
        assertEquals(2, movs.size(), "una sottoscrizione = uno scambio BIT -> token, non due movimenti");
        assertTrue(movs.stream().allMatch(m -> categoria(m).equals("SC")));
        String[] apex = movs.stream().filter(m -> m[8].equals("BIT") && m[11].equals("APEX")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(apex[10]).abs().compareTo(new BigDecimal("0.110131")));
    }

    @Test
    void launchpad_airdropSenzaBitVicino_restaAirdrop() throws Exception {
        //Distribuzioni indipendenti (di norma alle 03:00), e quelle dal 2023 il cui impegno sta nel
        //Funding: nel gruppo manca il commitmentForLaunchpad e la riga non diventa mezzo scambio.
        List<String[]> movs = importa(List.of(
                rigaSpot("airdropAssetIncrease", "STRM", "1.0866", "2022-04-28 00:02:07"),
                rigaSpot("commitmentForLaunchpad", "BIT", "-0.483668", "2022-05-05 11:12:10"),
                rigaSpot("airdropAssetIncrease", "PLY", "120.917", "2022-05-05 11:12:12")), spot());
        assertEquals(3, movs.size());
        long airdrop = movs.stream().filter(m -> categoria(m).equals("RW") || m[5].toUpperCase().contains("AIRDROP")).count();
        assertEquals(2, airdrop, "STRM da solo e PLY a 2 secondi dal BIT sono airdrop");
        assertEquals(1, movs.stream().filter(m -> m[8].equals("BIT")).count());
        assertTrue(movs.stream().noneMatch(m -> categoria(m).equals("SC")), "nessuno scambio: i due non sono abbinati");
    }

    @Test
    void funding_conversioneBitMnt_gambeSuSecondiDiversi_unoScambio() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2023-07-15 05:00:00", "BIT", "-300", "Convert", "BIT → MNT Auto-Conversion"));
        righe.add(rigaFunding("2023-07-15 05:00:01", "MNT", "300", "Convert", "BIT → MNT Auto-Conversion"));
        righe.add(rigaFunding("2023-07-15 05:00:01", "MNT", "0.5", "Earn", "Easy Earn | Flexible Interest Distribution"));

        List<String[]> movs = importa(righe, funding());
        assertEquals(2, movs.size(), "uno scambio + l'interesse, che resta un movimento a se'");
        String[] sc = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        assertEquals("BIT", sc[8]);
        assertEquals("MNT", sc[11]);
        assertEquals(0, new BigDecimal(sc[13]).compareTo(new BigDecimal("300")), "l'interesse non entra nello scambio");
    }

    @Test
    void funding_convert_euroAcquisto_cripto_scambio() throws Exception {
        //Forma della segnalazione #14, valori inventati: EUR -> USDC con le gambe a un secondo, ETH -> BTC nello stesso secondo.
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2026-05-12 10:15:30", "EUR", "-20.000000000000000000", "Fiat", "Convert"));
        righe.add(rigaFunding("2026-05-12 10:15:31", "USDC", "23.100000000000000000", "Fiat", "Convert"));
        righe.add(rigaFunding("2026-05-18 14:40:05", "BTC", "0.000400000000000000", "Convert", "Convert"));
        righe.add(rigaFunding("2026-05-18 14:40:05", "ETH", "-0.012000000000000000", "Convert", "Convert"));

        List<String[]> movs = importa(righe, funding());
        assertEquals(2, movs.size(), "due conversioni, ciascuna un solo movimento");
        String[] ac = movs.stream().filter(m -> categoria(m).equals("AC")).findFirst().orElseThrow();
        assertEquals("EUR", ac[8]);
        assertEquals("USDC", ac[11]);
        String[] sc = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        assertEquals("ETH", sc[8]);
        assertEquals("BTC", sc[11]);
    }

    @Test
    void funding_airdropBonus_airdropASe_ancheASecondiVicini() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2026-05-20 03:00:00", "ETH", "0.010000000000000000", "Airdrop", "Airdrop Bonus"));
        righe.add(rigaFunding("2026-05-20 03:00:01", "BTC", "0.000300000000000000", "Airdrop", "Airdrop Bonus"));
        righe.add(rigaFunding("2026-05-20 03:00:01", "BTC", "0.000100000000000000", "Airdrop", "Airdrop Bonus"));

        List<String[]> movs = importa(righe, funding());
        assertEquals(3, movs.size(), "nessuno scambio fra accrediti vicini");
        for (String[] m : movs) {
            assertEquals("RW", categoria(m));
            assertEquals("AIRDROP", m[5]);
            assertTrue(m[8].isBlank(), "solo entrata");
        }
    }

    @Test
    void funding_prelievoAnnullato_nessunMovimento() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2026-06-10 18:00:00", "BTC", "-0.003500000000000000", "Withdraw", "Withdrawal"));
        righe.add(rigaFunding("2026-06-10 18:02:00", "USDT", "-5", "Withdraw", "Withdrawal"));
        righe.add(rigaFunding("2026-06-10 18:05:00", "BTC", "0.003500000000000000", "Withdraw", "Withdrawal Canceled"));

        List<String[]> movs = importa(righe, funding());
        assertEquals(1, movs.size(), "il prelievo annullato e il suo annullamento non entrano");
        assertEquals("PC", categoria(movs.get(0)));
        assertEquals("USDT", movs.get(0)[8], "l'altro prelievo resta");
    }

    @Test
    void funding_prelievoAnnullatoGiaInArchivio_annullamentoEntra() throws Exception {
        //File importato prima che 'Withdrawal Canceled' fosse conosciuta: il prelievo e' gia' in archivio.
        ConfigurazioneImport f = funding();
        String[] prelievo = rigaFunding("2026-06-10 18:00:00", "BTC", "-0.003500000000000000", "Withdraw", "Withdrawal");
        String[] archiviato = ImportazioneGenerica.costruisciMovimenti(prelievo, null, f).get(0);
        Principale.MappaCryptoWallet.put(archiviato[0], archiviato);
        try {
            List<String[]> righe = new ArrayList<>();
            righe.add(prelievo);
            righe.add(rigaFunding("2026-06-10 18:05:00", "BTC", "0.003500000000000000", "Withdraw", "Withdrawal Canceled"));
            List<String[]> movs = importa(righe, f);
            assertEquals(List.of("DC", "PC"), movs.stream().map(ImportazioneGenericaBybitTest::categoria).sorted().toList(),
                    "il prelievo cadra' come doppione, l'annullamento deve entrare");
        } finally {
            Principale.MappaCryptoWallet.remove(archiviato[0]);
        }
    }

    @Test
    void funding_annullamentoSenzaPrelievo_entraComeDeposito() throws Exception {
        List<String[]> righe = new ArrayList<>();
        //Importo diverso: non e' il prelievo annullato. E un annullamento il cui prelievo e' in un altro export.
        righe.add(rigaFunding("2026-06-10 18:00:00", "BTC", "-0.0034", "Withdraw", "Withdrawal"));
        righe.add(rigaFunding("2026-06-10 18:05:00", "BTC", "0.0035", "Withdraw", "Withdrawal Canceled"));
        //L'annullamento abbina solo prelievi precedenti.
        righe.add(rigaFunding("2026-06-11 10:00:00", "ETH", "0.5", "Withdraw", "Withdrawal Canceled"));
        righe.add(rigaFunding("2026-06-11 11:00:00", "ETH", "-0.5", "Withdraw", "Withdrawal"));

        List<String[]> movs = importa(righe, funding());
        assertEquals(List.of("DC", "DC", "PC", "PC"), movs.stream().map(ImportazioneGenericaBybitTest::categoria).sorted().toList());
        assertTrue(movs.stream().allMatch(m -> m[18].isBlank()), "da classificare a mano");
    }

    @Test
    void derivati_pnlEFundingSommatiPerGiornoECausale_bonusASe_girocontiIgnorati_marcati() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaDerivati("2022-11-15 00:00:00", "USDT", "funding", "0.01", "-0.01"));
        righe.add(rigaDerivati("2022-11-15 09:00:00", "USDT", "trade", "0.005", "-0.005"));
        righe.add(rigaDerivati("2022-11-15 15:00:00", "USDT", "trade", "0.01", "-0.3"));
        righe.add(rigaDerivati("2022-11-15 16:00:00", "USDT", "funding", "-0.02", "0.02"));
        righe.add(rigaDerivati("2022-11-15 17:00:00", "USDT", "bonusClaimed", "0", "10"));
        righe.add(rigaDerivati("2022-11-15 18:00:00", "USDT", "transferOut", "0", "-5"));

        List<String[]> movs = importa(righe, derivati());
        assertEquals(3, movs.size(), "un movimento per il PnL del giorno, uno per il funding, uno per il bonus");
        assertTrue(movs.stream().noneMatch(m -> categoria(m).equals("CM")),
                "la commissione e' gia' dentro Change: nessun movimento COMMISSIONI");
        String[] pnl = movs.stream().filter(m -> Derivati.Tipo(m).equals(Derivati.PNL)).findFirst().orElseThrow();
        assertEquals("PC", categoria(pnl));
        assertEquals("USDT", pnl[8]);
        assertEquals(0, new BigDecimal(pnl[10]).compareTo(new BigDecimal("-0.305")), "somma dei Change del PnL del giorno");
        assertEquals("", pnl[18], "da classificare a mano: trattato come cripto-attivita'");
        String[] funding = movs.stream().filter(m -> Derivati.Tipo(m).equals(Derivati.FUNDING)).findFirst().orElseThrow();
        assertEquals("DC", categoria(funding));
        assertEquals(0, new BigDecimal(funding[13]).compareTo(new BigDecimal("0.01")), "somma dei funding del giorno");
        String[] bonus = movs.stream().filter(m -> Derivati.Tipo(m).equals(Derivati.BONUS)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(bonus[13]).compareTo(BigDecimal.TEN));
    }

    @Test
    void uta_scambioSpot_lordoPiuCommissione() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("USDT", "TRADE", "0", "-2", "--", "2024-12-10 11:00:00"));
        righe.add(rigaUta("MNT", "TRADE", "-0.0018", "1.7982", "--", "2024-12-10 11:00:00"));

        List<String[]> movs = importa(righe, uta());
        assertEquals(2, movs.size(), "uno scambio + un movimento COMMISSIONI");
        String[] sc = movs.stream().filter(m -> categoria(m).equals("SC")).findFirst().orElseThrow();
        String[] fee = movs.stream().filter(m -> categoria(m).equals("CM")).findFirst().orElseThrow();
        assertEquals("USDT", sc[8]);
        assertEquals("MNT", sc[11]);
        assertEquals(0, new BigDecimal(sc[13]).compareTo(new BigDecimal("1.8")), "Change + fee = quantita' lorda");
        assertEquals("MNT", fee[8]);
        assertEquals(0, new BigDecimal(fee[10]).compareTo(new BigDecimal("-0.0018")));
    }

    @Test
    void uta_conversioneInMnt_unoScambio() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("XION", "--", "0", "-0.5", "--", "2024-12-12 08:00:00"));
        righe.add(rigaUta("MNT", "--", "0", "2", "--", "2024-12-12 08:00:00"));
        righe.add(rigaUta("MNT", "TRANSFER_OUT", "0", "-2", "--", "2024-12-12 08:00:20"));

        List<String[]> movs = importa(righe, uta());
        assertEquals(1, movs.size(), "il giroconto verso il Funding e' ignorato");
        assertEquals("SC", categoria(movs.get(0)));
        assertEquals("XION", movs.get(0)[8]);
        assertEquals("MNT", movs.get(0)[11]);
    }

    @Test
    void uta_perpetui_sommatiPerGiorno_feeNonContataDueVolte() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("USDT", "TRADE", "-0.0015", "-0.0015", "OPEN", "2024-11-28 16:00:30"));
        righe.add(rigaUta("USDT", "TRADE", "-0.001", "-0.001", "OPEN", "2024-11-28 16:00:30"));
        righe.add(rigaUta("USDT", "SETTLEMENT", "0", "-0.006", "SETTLEMENT", "2024-11-28 16:00:00"));

        List<String[]> movs = importa(righe, uta());
        assertEquals(2, movs.size(), "un movimento per il PnL e uno per il funding del giorno, nessuna COMMISSIONI a parte");
        assertTrue(movs.stream().allMatch(m -> categoria(m).equals("PC")));
        String[] pnl = movs.stream().filter(m -> Derivati.Tipo(m).equals(Derivati.PNL)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(pnl[10]).compareTo(new BigDecimal("-0.0025")),
                "somma dei Change: la fee e' gia' dentro e non va sottratta una seconda volta");
        String[] funding = movs.stream().filter(m -> Derivati.Tipo(m).equals(Derivati.FUNDING)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(funding[10]).compareTo(new BigDecimal("-0.006")));
    }

    @Test
    void uta_scambioSpot_nonMarcatoComeDerivato() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("USDT", "TRADE", "0", "-2", "--", "2024-12-10 11:00:00"));
        righe.add(rigaUta("MNT", "TRADE", "-0.0018", "1.7982", "--", "2024-12-10 11:00:00"));
        assertTrue(importa(righe, uta()).stream().noneMatch(Derivati::isDerivato),
                "'TRADE|--' e' uno scambio spot, non un derivato: ne' lo scambio ne' la sua commissione");
    }

    @Test
    void configurazioniDerivate_causaliDerivatiAlzanoAncheLAvviso() throws Exception {
        for (ConfigurazioneImport c : List.of(derivati(), uta())) {
            assertFalse(c.causaliDerivati.isEmpty());
            for (String causale : c.causaliDerivati.keySet()) {
                assertTrue(c.causaliAllertaDerivati.contains(causale), causale);
                assertNotNull(c.mappaCausali.get(causale), "causale derivata non mappata: " + causale);
            }
        }
        assertEquals(Derivati.PNL, derivati().tipoDerivato("trade"));
        assertEquals("", spot().tipoDerivato("trade"), "il 'trade' dello Spot e' uno scambio a pronti");
    }

    // ---- Bybit EU (Bybit EU GmbH): stesso tracciato, exchange distinto ----

    @Test
    void bybitEU_exchangeDistinto_comeInRWPredefiniti() throws Exception {
        ConfigurazioneImport f = fundingEU(), u = utaEU();
        assertEquals("Bybit EU", f.nomeExchange);
        assertEquals(f.nomeExchange, u.nomeExchange, "i giroconti Funding/UTA si ignorano solo se l'exchange coincide");
        assertEquals(f.nomeWallet, u.nomeWallet);
        assertNotEquals(funding().nomeExchange, f.nomeExchange, "Bybit EU e' un intermediario diverso da Bybit");
        assertEquals(f.nomeExchange, DatiPredefinitiRW.Carica().exchangePerId("bybiteu").nome,
                "deve coincidere col nome in RW_Predefiniti, o i movimenti non finiscono nel gruppo di Bybit EU");
    }

    @Test
    void bybitEU_uta_righeTrattinoSonoEarnENonProduconoMovimenti() throws Exception {
        //Nell'UTA di Bybit EU '--' e' una sottoscrizione o un riscatto Easy Earn fatto dall'UTA (una sola
        //riga, nessuna controparte), non la conversione in MNT dell'UTA globale
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("USDC", "--", "0", "-40", "--", "2026-06-03 10:00:00"));
        righe.add(rigaUta("BTC", "--", "0", "-0.0005", "--", "2026-06-04 10:00:00"));
        righe.add(rigaUta("USDC", "--", "0", "5", "--", "2026-06-05 10:00:00"));
        assertTrue(importa(righe, utaEU()).isEmpty());
        assertEquals("SCAMBIO CRYPTO-CRYPTO", uta().mappaCausali.get("--|--"), "l'UTA globale resta com'era");
    }

    @Test
    void bybitEU_uta_acquistoInEuro_lordoPiuCommissione() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaUta("USDC", "TRADE", "-0.25", "99.75", "--", "2026-06-03 09:00:00"));
        righe.add(rigaUta("EUR", "TRADE", "0", "-87.5", "--", "2026-06-03 09:00:00"));
        righe.add(rigaUta("EUR", "TRANSFER_IN", "0", "87.5", "--", "2026-06-03 08:59:30"));

        List<String[]> movs = importa(righe, utaEU());
        assertEquals(2, movs.size(), "il giroconto non produce movimenti: un acquisto + una COMMISSIONI");
        String[] ac = movs.stream().filter(m -> categoria(m).equals("AC")).findFirst().orElseThrow();
        String[] fee = movs.stream().filter(m -> categoria(m).equals("CM")).findFirst().orElseThrow();
        assertEquals("EUR", ac[8]);
        assertEquals("USDC", ac[11]);
        assertEquals(0, new BigDecimal(ac[13]).compareTo(new BigDecimal("100")), "gamba in entrata al lordo");
        assertEquals(0, new BigDecimal(fee[10]).compareTo(new BigDecimal("-0.25")));
        assertEquals("Bybit EU", ac[3]);
    }

    @Test
    void bybitEU_funding_depositoEuro_convert_prelievoAnnullato() throws Exception {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2026-06-01 10:00:00", "EUR", "50", "Fiat", "Deposit"));
        righe.add(rigaFunding("2026-06-02 09:00:00", "EUR", "-50", "Fiat", "Convert"));
        righe.add(rigaFunding("2026-06-02 09:00:01", "USDC", "57", "Fiat", "Convert"));
        righe.add(rigaFunding("2026-06-20 08:00:00", "BTC", "-0.001", "Withdraw", "Withdrawal"));
        righe.add(rigaFunding("2026-06-20 12:00:00", "BTC", "0.001", "Withdraw", "Withdrawal Canceled"));
        righe.add(rigaFunding("2026-06-20 12:05:00", "BTC", "-0.001", "Withdraw", "Withdrawal"));
        righe.add(rigaFunding("2026-06-21 00:30:00", "USDC", "2.5E-3", "Earn", "Easy Earn | Flexible Interest Distribution"));
        righe.add(rigaFunding("2026-06-15 10:00:00", "USDC", "5", "Earn", "Easy Earn | Flexible Redemption"));
        righe.add(rigaFunding("2026-06-15 10:00:10", "USDC", "-5", "Transfer out", "Transfer to Unified Trading Account"));

        List<String[]> movs = importa(righe, fundingEU());
        assertEquals(4, movs.size(), "deposito, acquisto, un solo prelievo, interesse");
        assertEquals(1, movs.stream().filter(m -> categoria(m).equals("DF")).count(), "il bonifico e' un deposito FIAT");
        String[] ac = movs.stream().filter(m -> categoria(m).equals("AC")).findFirst().orElseThrow();
        assertEquals("EUR", ac[8]);
        assertEquals("USDC", ac[11]);
        String[] pc = movs.stream().filter(m -> categoria(m).equals("PC")).findFirst().orElseThrow();
        assertTrue(pc[0].startsWith("20260620140500"), "resta il prelievo dopo l'annullamento (ID in ora di Roma)");
        assertEquals(1, movs.stream().filter(m -> categoria(m).equals("RW")).count());
    }

    // ---- Carta Bybit nel conto Funding (Type 'Bybit Card'), forma dell'export reale, valori inventati ----

    /** Spesa in euro, spesa coperta da USDC (tre righe a un secondo), spesa in euro subito dopo. */
    private static List<String[]> righeCarta() {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaFunding("2025-06-02 10:00:00", "EUR", "200", "Fiat", "Deposit"));
        righe.add(rigaFunding("2025-06-02 12:00:00", "EUR", "-40.5", "Bybit Card", "Purchase"));
        righe.add(rigaFunding("2025-06-02 17:20:10", "USDC", "-70.25", "Bybit Card", "Purchase"));
        righe.add(rigaFunding("2025-06-02 17:20:11", "EUR", "60", "Bybit Card", "Coin Purchase"));
        righe.add(rigaFunding("2025-06-02 17:20:11", "EUR", "-60", "Bybit Card", "Purchase"));
        righe.add(rigaFunding("2025-06-02 17:40:00", "EUR", "-10", "Bybit Card", "Purchase"));
        return righe;
    }

    private static void verificaCarta(List<String[]> movs, String exchange) {
        assertEquals(5, movs.size(), "deposito, tre prelievi fiat, una vendita: nessuna riga scartata");
        String[] vc = movs.stream().filter(m -> categoria(m).equals("VC")).findFirst().orElseThrow();
        assertEquals("USDC", vc[8]);
        assertEquals(0, new BigDecimal(vc[10]).compareTo(new BigDecimal("-70.25")));
        assertEquals("EUR", vc[11]);
        assertEquals(0, new BigDecimal(vc[13]).compareTo(new BigDecimal("60")), "gli euro di 'Coin Purchase'");
        assertEquals(exchange, vc[3]);
        List<String[]> pf = movs.stream().filter(m -> categoria(m).equals("PF")).toList();
        assertEquals(3, pf.size(), "anche il 'Purchase' in euro della spesa coperta da USDC e' un prelievo fiat");
        BigDecimal eur = BigDecimal.ZERO;
        for (String[] m : movs) {
            if (m[8].equals("EUR")) eur = eur.add(new BigDecimal(m[10]));
            if (m[11].equals("EUR")) eur = eur.add(new BigDecimal(m[13]));
        }
        assertEquals(0, eur.compareTo(new BigDecimal("149.5")), "saldo EUR: 200 - 40,5 + 60 - 60 - 10");
    }

    @Test
    void bybitEU_funding_carta_spesaInEuroPrelievo_spesaInUsdcVendita() throws Exception {
        verificaCarta(importa(righeCarta(), fundingEU()), "Bybit EU");
    }

    @Test
    void funding_carta_stesseRegoleDiBybitEU() throws Exception {
        verificaCarta(importa(righeCarta(), funding()), "Bybit");
    }

    @Test
    void tipoSeCrypto_valeSoloPerLeMoneteNonFiat() throws Exception {
        ConfigurazioneImport c = fundingEU();
        assertEquals("SCAMBIO CRYPTO-CRYPTO", c.tipoMovimentoPerRiga(
                rigaFunding("2025-06-02 17:20:10", "USDC", "-1", "Bybit Card", "Purchase")));
        assertEquals("TRASFERIMENTO-CRYPTO", c.tipoMovimentoPerRiga(
                rigaFunding("2025-06-02 17:20:11", "EUR", "-1", "Bybit Card", "Purchase")));
        assertEquals("AIRDROP", c.tipoMovimentoPerRiga(
                rigaFunding("2025-06-02 17:20:11", "USDC", "1", "Airdrop", "Airdrop Bonus")),
                "una causale senza voce in tipoSeCrypto resta quella della mappaCausali");
    }
}
