package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** La marcatura retroattiva dei derivati: riconosce per exchange e causale, solo righe singole, mai gli scambi spot, con storico. */
class Principale_DerivatiMarcaturaTest {

    @TempDir
    static Path tempDir;
    @TempDir
    Path cartellaConfig;

    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabase() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        Principale.MappaCryptoWallet.clear();
    }

    private void config(String nome, String exchange, String causaliDerivati) throws Exception {
        Files.writeString(cartellaConfig.resolve(nome + ".json"),
                "{\"nomeExchange\":\"" + exchange + "\",\"causaliDerivati\":" + causaliDerivati + "}", StandardCharsets.UTF_8);
    }

    private static String[] mov(String exchange, String causale, String cat) {
        String id = "202601011000" + String.format("%02d", ++progressivo % 60) + "_" + exchange + "_001_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = "2026-01-01 10:00";
        m[3] = exchange;
        m[4] = "Principale";
        m[7] = causale;
        m[22] = "A";
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    @Test
    void siMarcanoLeRigheSingoleDiUnaCausaleDichiarataEIlTipoEQuelloDellaConfigurazione() throws Exception {
        config("Bybit Derivati", "Bybit", "{\"funding\":\"FUNDING\",\"trade\":\"PNL\"}");
        String[] funding = mov("Bybit", "funding", "PC");
        String[] pnl = mov("Bybit", "trade", "DC");
        String[] altro = mov("Bybit", "Earn", "DC");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        var piano = Principale_DerivatiMarcatura.Pianifica(List.of(funding[0], pnl[0], altro[0]), tipi);
        assertEquals(2, piano.voci.size());
        assertEquals(1, piano.nonRiconosciuti);
        assertEquals(2, Principale_DerivatiMarcatura.Applica(piano, tipi));
        assertEquals(Derivati.FUNDING, Derivati.Tipo(funding));
        assertEquals(Derivati.PNL, Derivati.Tipo(pnl));
        assertEquals("", Derivati.Tipo(altro));
        assertFalse(funding[Derivati.CAMPO_RISULTATO].length() > 0, "il marcatore non scrive nessun risultato");
        assertEquals("", funding[18], "resta da classificare");
        assertFalse(funding[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank(), "la modifica e' nello storico");
    }

    @Test
    void unaCommissioneDiUnaCausaleDerivataEUnaCommissioneDerivata() throws Exception {
        config("Bybit UTA", "Bybit", "{\"TRADE|OPEN\":\"PNL\"}");
        String[] fee = mov("Bybit", "TRADE|OPEN", "CM");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        Principale_DerivatiMarcatura.Applica(Principale_DerivatiMarcatura.Pianifica(List.of(fee[0]), tipi), tipi);
        assertEquals(Derivati.COMMISSIONE, Derivati.Tipo(fee));
    }

    @Test
    void ilTradeSpotNonSiMarcaPerchEUnoScambioENonUnaRigaSingola() throws Exception {
        config("Bybit Derivati", "Bybit", "{\"trade\":\"PNL\"}");
        String[] scambio = mov("Bybit", "trade", "SC");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        assertEquals(0, Principale_DerivatiMarcatura.Pianifica(List.of(scambio[0]), tipi).voci.size());
    }

    @Test
    void unaCausaleConTipiDiscordiFraConfigurazioniDellOStessoExchangeNonSiMarca() throws Exception {
        config("A", "Bybit", "{\"trade\":\"PNL\"}");
        config("B", "Bybit", "{\"trade\":\"FUNDING\"}");
        String[] m = mov("Bybit", "trade", "DC");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        assertEquals(0, Principale_DerivatiMarcatura.Pianifica(List.of(m[0]), tipi).voci.size());
    }

    @Test
    void unMovimentoGiaMarcatoOUnAltroExchangeNonSiTocca() throws Exception {
        config("Bybit Derivati", "Bybit", "{\"funding\":\"FUNDING\"}");
        String[] gia = mov("Bybit", "funding", "PC");
        Derivati.Marca(gia, Derivati.BONUS);
        String[] altroExchange = mov("OKX", "funding", "PC");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        var piano = Principale_DerivatiMarcatura.Pianifica(List.of(gia[0], altroExchange[0]), tipi);
        assertEquals(0, piano.voci.size());
        assertEquals(1, piano.giaMarcati);
        assertEquals(Derivati.BONUS, Derivati.Tipo(gia));
    }

    @Test
    void lAnteprimaDiceCheNonCambiaNessunCalcolo() throws Exception {
        config("Bybit Derivati", "Bybit", "{\"funding\":\"FUNDING\"}");
        String[] f = mov("Bybit", "funding", "PC");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        String testo = Principale_DerivatiMarcatura.TestoAnteprima(Principale_DerivatiMarcatura.Pianifica(List.of(f[0]), tipi));
        assertTrue(testo.contains("non cambia nessuna plusvalenza"), testo);
        assertTrue(testo.contains("2026"), testo);
    }

    @Test
    void leConfigurazioniVereDelProgettoDichiaranoIDerivatiDiBybitEBinance() {
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(new File("config/import"));
        assertTrue(tipi.containsKey("bybit"), tipi.keySet().toString());
        assertEquals("FUNDING", tipi.get("bybit").get("funding"));
        assertTrue(tipi.containsKey("binance"));
        assertEquals("DUAL", tipi.get("binance").get("Dual Savings Purchase"));
    }

    /**
     * Dalla 1.0.67 il conto derivati di Bybit e di Binance sta sul sotto-wallet Derivati: la stessa voce sposta li' i movimenti importati
     * prima (sul wallet principale), marcati o no. Il conto spot, l'UTA e i giroconti restano dove sono. Configurazioni vere del progetto.
     */
    @Test
    void conLeConfigurazioniVereSiSpostaSulContoDerivatiSoloIlContoDerivati() throws Exception {
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(new File("config/import"));
        var conti = Principale_DerivatiMarcatura.ContiDaConfigurazioni(new File("config/import"));
        String[] tradeContratti = mov("Bybit", "trade", "PC");
        Derivati.Marca(tradeContratti, Derivati.PNL);                 //marcato da una versione precedente
        String[] fundingContratti = mov("Bybit", "funding", "DC");    //non marcato
        String[] utaChiusura = mov("Bybit", "TRADE|CLOSE", "DC");
        Derivati.Marca(utaChiusura, Derivati.PNL);
        String[] fundingFutures = mov("Binance", "Funding Fee", "PC");
        String[] feeFutures = mov("Binance", "Futures Fee", "CM");
        String[] feeSpot = mov("Binance", "Fee", "CM");
        String[] giaSulConto = mov("Binance", "Realized Profit and Loss", "DC");
        giaSulConto[4] = OKXDerivati.SOTTOWALLET;
        List<String> ids = List.of(tradeContratti[0], fundingContratti[0], utaChiusura[0], fundingFutures[0], feeFutures[0], feeSpot[0], giaSulConto[0]);

        var piano = Principale_DerivatiMarcatura.Pianifica(ids, tipi, conti);
        assertEquals(java.util.Set.of(tradeContratti[0], fundingContratti[0], fundingFutures[0], feeFutures[0]), piano.sposta);
        assertTrue(Principale_DerivatiMarcatura.TestoAnteprima(piano).contains("sotto-wallet " + OKXDerivati.SOTTOWALLET));
        assertEquals(java.util.Set.of(fundingContratti[0], fundingFutures[0], feeFutures[0], giaSulConto[0]), piano.voci.keySet(),
                "si marcano quelli non ancora marcati, come prima");

        Principale_DerivatiMarcatura.Applica(piano, tipi, conti);
        for (String[] m : List.of(tradeContratti, fundingContratti, fundingFutures, feeFutures, giaSulConto)) {
            assertEquals(OKXDerivati.SOTTOWALLET, m[4], m[7]);
        }
        assertEquals("Principale", utaChiusura[4], "l'UTA resta sul wallet principale (P1, nessun giroconto)");
        assertEquals("Principale", feeSpot[4]);
        assertFalse(tradeContratti[MovimentiStorico.CAMPO_LIGNAGGIO].isBlank(), "lo spostamento entra nello storico");
        assertEquals(Derivati.PNL, Derivati.Tipo(tradeContratti), "il marcatore che c'era resta");
        assertEquals(Derivati.FUNDING, Derivati.Tipo(fundingContratti));
    }

    @Test
    void siMarcanoAncheIDepositiEIPrelieviFiat() throws Exception {
        //Un derivato regolato in euro (20/11/2026): deposito e prelievo FIAT si marcano come gli altri
        config("Bybit Derivati", "Bybit", "{\"trade\":\"PNL\"}");
        String[] df = mov("Bybit", "trade", "DF");
        String[] pf = mov("Bybit", "trade", "PF");
        var tipi = Principale_DerivatiMarcatura.TipiDaConfigurazioni(cartellaConfig.toFile());
        assertEquals(2, Principale_DerivatiMarcatura.Applica(Principale_DerivatiMarcatura.Pianifica(List.of(df[0], pf[0]), tipi), tipi));
        assertEquals(Derivati.PNL, Derivati.Tipo(df));
        assertEquals(Derivati.PNL, Derivati.Tipo(pf));
    }
}
