package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa la regola di scelta della gamba che fa il prezzo di un movimento (dal 2026-09-29, circolare AdE
 * 30/E p. 50-51, valore normale della cripto-attività ricevuta): la gamba FIAT se c'è, altrimenti quella
 * in entrata, altrimenti quella in uscita. Vale sia quando il prezzo va cercato
 * ({@link Prezzi#DammiPrezzoInfoTransazione}) sia quando le gambe arrivano già prezzate
 * ({@link MovimentiCrypto#DammiMonetaPrioritaria}). Eccezione sulla precisione: se l'entrata non e' quotata
 * entro 5 minuti dal movimento e l'uscita si', vince l'uscita. Nessuna rete: la ricerca resta nel database
 * temporaneo.
 */
class PrezziGambaEntrataTest {

    @TempDir
    static Path tempDir;

    private static final long DATA = FunzioniDate.ConvertiDatainLong("2024-06-01");

    @BeforeAll
    static void apreDatabase() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        System.setProperty("prezzi.servizio.abilitato", "false");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        Funzioni.ReteDisabilitataPerTest = true;
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void emoney() {
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_EMoney.put("EURe", "2024-01-01");
    }

    @AfterEach
    void pulisci() {
        Principale.Mappa_EMoney.clear();
    }

    private static Moneta moneta(String simbolo, String qta, String tipo) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Qta = qta;
        m.Tipo = tipo;
        return m;
    }

    private static Moneta prezzata(String simbolo, String qta, String prezzo) {
        Moneta m = moneta(simbolo, qta, "Crypto");
        m.Prezzo = prezzo;
        return m;
    }

    // ---------------------------------------------------------------- ordine delle gambe

    @Test
    void ordine_cripto_primaLEntrataPoiLUscita() {
        assertArrayEquals(new int[]{1, 0}, Prezzi.OrdineGambe(new Moneta[]{
            moneta("BTC", "-0.01", "Crypto"), moneta("ETH", "0.3", "Crypto")}));
    }

    @Test
    void ordine_lEntrataSiRiconosceDalSegnoAncheSeGliArgomentiSonoInvertiti() {
        //creaMovimento riceve le gambe come capita (TransazioneDefi passa l'entrata per prima)
        assertArrayEquals(new int[]{0, 1}, Prezzi.OrdineGambe(new Moneta[]{
            moneta("ETH", "0.3", "Crypto"), moneta("BTC", "-0.01", "Crypto")}));
        //notazione scientifica: il segno si legge col BigDecimal (bug M7)
        assertArrayEquals(new int[]{0, 1}, Prezzi.OrdineGambe(new Moneta[]{
            moneta("PEPE", "2.5E-9", "Crypto"), moneta("BTC", "-1E-8", "Crypto")}));
    }

    @Test
    void ordine_laGambaFiatVienePrimaAncheSeEInUscita() {
        Moneta eur = moneta("EUR", "-100", "FIAT");
        assertArrayEquals(new int[]{0, 1}, Prezzi.OrdineGambe(new Moneta[]{eur, moneta("BTC", "0.002", "Crypto")}));
        Moneta usd = moneta("USD", "-100", "");
        assertArrayEquals(new int[]{0, 1}, Prezzi.OrdineGambe(new Moneta[]{usd, moneta("BTC", "0.002", "Crypto")}),
                "USD senza address conta come valuta anche senza tipo");
    }

    @Test
    void ordine_fraDueValuteVinceLEuro() {
        assertArrayEquals(new int[]{0, 1}, Prezzi.OrdineGambe(new Moneta[]{
            moneta("EUR", "-100", "FIAT"), moneta("USD", "108", "FIAT")}));
    }

    @Test
    void ordine_unaSolaGamba() {
        assertArrayEquals(new int[]{0}, Prezzi.OrdineGambe(new Moneta[]{moneta("BTC", "0.1", "Crypto"), null}));
    }

    // ---------------------------------------------------------------- gambe gia' prezzate

    @Test
    void prezzate_entrambe_vinceLEntrata() {
        Moneta uscita = prezzata("USDT", "-1000", "920.00");
        Moneta entrata = prezzata("BTC", "0.02", "890.00");
        assertSame(entrata, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA),
                "fino al 2026-09-29 vinceva USDT perche' stablecoin");
    }

    @Test
    void prezzate_entrataAZero_vinceLUscita() {
        Moneta uscita = prezzata("ETH", "-0.1", "300.00");
        Moneta entrata = prezzata("SCAMTOKEN", "1000", "0.00");
        assertSame(uscita, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA));
    }

    @Test
    void prezzate_entrataSenzaPrezzo_vinceLUscita() {
        Moneta uscita = prezzata("ETH", "-0.1", "300.00");
        Moneta entrata = moneta("NUOVO", "50", "Crypto");
        assertSame(uscita, MovimentiCrypto.DammiMonetaPrioritaria(entrata, uscita, DATA));
    }

    @Test
    void prezzate_conUnaGambaFiat_vinceLaFiatAncheSenzaPrezzo() {
        Moneta eur = moneta("EUR", "-100", "FIAT");
        Moneta btc = prezzata("BTC", "0.002", "101.00");
        assertSame(eur, MovimentiCrypto.DammiMonetaPrioritaria(eur, btc, DATA),
                "acquisto: il corrispettivo e' l'importo in euro, lo calcola poi DammiPrezzoInfoTransazione");
    }

    @Test
    void prezzate_nessunPrezzo_null() {
        assertNull(MovimentiCrypto.DammiMonetaPrioritaria(moneta("A", "-1", "Crypto"), moneta("B", "1", "Crypto"), DATA));
    }

    @Test
    void prezzate_soloZeriEspliciti_restaQuelloDellEntrata() {
        Moneta uscita = prezzata("A", "-1", "0.00");
        Moneta entrata = prezzata("B", "1", "0.00");
        assertSame(entrata, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA));
    }

    // ---------------------------------------------------------------- prezzo da cercare

    @Test
    void daCercare_entrataSenzaPrezzo_ripiegaSullUscita() {
        //EURe in uscita vale 1:1, il token ricevuto non ha prezzo da nessuna parte
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("EURe", "-60", "Crypto"), moneta("TOKENSENZAPREZZO", "5", "Crypto"), DATA, null, "");
        assertNotNull(IP);
        assertEquals("EURe", IP.Moneta);
        assertEquals(0, new BigDecimal("60").compareTo(IP.prezzoQta));
    }

    @Test
    void daCercare_conFiatEuro_decideLEuro() {
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("TOKENSENZAPREZZO", "-5", "Crypto"), moneta("EUR", "42.5", "FIAT"), DATA, null, "");
        assertNotNull(IP);
        assertEquals("EUR", IP.Moneta);
        assertEquals(0, new BigDecimal("42.5").compareTo(IP.prezzoQta));
    }

    @Test
    void daCercare_nessunPrezzo_null() {
        assertNull(Prezzi.DammiPrezzoInfoTransazione(
                moneta("TOKENA", "-5", "Crypto"), moneta("TOKENB", "7", "Crypto"), DATA, null, ""));
    }

    // ---------------------------------------------------------------- precisione (5 minuti)

    private static Prezzi.InfoPrezzo quotazione(long ts, String fonte) {
        Prezzi.InfoPrezzo ip = new Prezzi.InfoPrezzo();
        ip.timestamp = ts;
        ip.Fonte = fonte;
        ip.prezzoUnitario = BigDecimal.ONE;
        ip.Qta = BigDecimal.ONE;
        return ip;
    }

    private static Moneta quotata(String simbolo, String qta, String prezzo, long ts, String fonte) {
        Moneta m = prezzata(simbolo, qta, prezzo);
        m.InfoPrezzo = quotazione(ts, fonte);
        return m;
    }

    @Test
    void precisione_entroCinqueMinutiSi_oltreNo() {
        assertTrue(Prezzi.isPrezzoPreciso(quotazione(DATA + 5 * 60_000L, "binance"), DATA));
        assertTrue(Prezzi.isPrezzoPreciso(quotazione(DATA - 5 * 60_000L, "binance"), DATA));
        assertFalse(Prezzi.isPrezzoPreciso(quotazione(DATA + 5 * 60_000L + 1, "binance"), DATA));
        assertFalse(Prezzi.isPrezzoPreciso(quotazione(DATA - 3_600_000L, "defillama"), DATA));
    }

    @Test
    void precisione_personalizzatoEPrezzoEsplicitoContanoComePrecisi() {
        assertTrue(Prezzi.isPrezzoPreciso(quotazione(DATA - 3_600_000L, "Personalizzato"), DATA),
                "il prezzo personalizzato l'ha deciso l'utente per quel movimento");
        assertTrue(Prezzi.isPrezzoPreciso(quotazione(0, "CSV"), DATA),
                "senza orario di quotazione e' un valore esplicito, non una ricerca");
    }

    @Test
    void prezzate_entrataOrariaEUscitaAlMinuto_vinceLUscita() {
        //Il caso DeFi: token ricevuto con la sola quotazione oraria, token ceduto quotato al minuto
        Moneta uscita = quotata("ETH", "-0.1", "300.00", DATA + 60_000L, "binance");
        Moneta entrata = quotata("TOKENDEFI", "5000", "280.00", DATA - 40 * 60_000L, "defillama");
        assertSame(uscita, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA));
    }

    @Test
    void prezzate_entrambeImprecise_vinceComunqueLEntrata() {
        Moneta uscita = quotata("TOKENA", "-10", "50.00", DATA - 30 * 60_000L, "defillama");
        Moneta entrata = quotata("TOKENB", "20", "48.00", DATA - 30 * 60_000L, "defillama");
        assertSame(entrata, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA));
    }

    @Test
    void prezzate_entrataImprecisaEUscitaEsplicita_vinceLUscita() {
        Moneta uscita = prezzata("USDT", "-100", "92.00");
        Moneta entrata = quotata("TOKENDEFI", "5000", "95.00", DATA - 40 * 60_000L, "coingecko");
        assertSame(uscita, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA),
                "un prezzo senza InfoPrezzo e' un valore esplicito, quindi preciso");
    }

    @Test
    void prezzate_entrataPersonalizzata_vinceLEntrataAncheSeLontana() {
        Moneta uscita = quotata("ETH", "-0.1", "300.00", DATA, "binance");
        Moneta entrata = quotata("TOKENDEFI", "5000", "290.00", DATA - 50 * 60_000L, "Personalizzato");
        assertSame(entrata, MovimentiCrypto.DammiMonetaPrioritaria(uscita, entrata, DATA));
    }

    @Test
    void daCercare_entrataSoloOrariaEUscitaAlMinuto_vinceLUscita() throws Exception {
        //Movimento alle 12:30: l'entrata ha solo il vecchio archivio orario (quotazione delle 12:00, 30
        //minuti prima), l'uscita ha la quotazione al minuto in cache
        long mezzora = FunzioniDate.ConvertiDatainLongMinuto(FunzioniDate.ConvertiDatadaLongallOra(DATA_ORA) + ":30");
        DatabaseH2.OLD_XXXEUR_Scrivi(FunzioniDate.ConvertiDatadaLongallOra(mezzora) + " TSTENTRATA", "2", false);
        scriviCache("TSTUSCITA", mezzora, "3");

        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("TSTUSCITA", "-10", "Crypto"), moneta("TSTENTRATA", "15", "Crypto"), mezzora, null, "");
        assertNotNull(IP);
        assertEquals("TSTUSCITA", IP.Moneta);
        assertEquals(0, new BigDecimal("30").compareTo(IP.prezzoQta));
    }

    @Test
    void daCercare_entrambeAlMinuto_vinceLEntrata() throws Exception {
        long istante = DATA_ORA + 10 * 60_000L;
        scriviCache("TSTIN2", istante, "4");
        scriviCache("TSTOUT2", istante, "3");
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("TSTOUT2", "-10", "Crypto"), moneta("TSTIN2", "8", "Crypto"), istante, null, "");
        assertNotNull(IP);
        assertEquals("TSTIN2", IP.Moneta);
        assertEquals(0, new BigDecimal("32").compareTo(IP.prezzoQta));
    }

    /** 2024-06-01 12:00 UTC, un'ora intera nel passato (le ricerche rifiutano le date future). */
    private static final long DATA_ORA = 1717243200000L;

    private static void scriviCache(String simbolo, long ts, String prezzo) throws Exception {
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, 'binance', ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, simbolo);
            ps.setBigDecimal(3, new BigDecimal(prezzo));
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- gambe a zero, prezzo al minuto, omonimi CMC

    @Test
    void ordine_gambaAQuantitaZeroEsclusa() {
        //Scambio a piu' monete: la moneta con peso zero riceve quantita' zero ma porta ancora l'InfoPrezzo
        //dell'altra parte (un prelievo di SON finiva con la fonte di BIT)
        assertArrayEquals(new int[]{0}, Prezzi.OrdineGambe(new Moneta[]{
            moneta("SON", "-143.1", "Crypto"), moneta("BIT", "0", "Crypto")}));
    }

    @Test
    void prezzate_gambaAQuantitaZeroConPrezzoNonConta() {
        Moneta son = moneta("SON", "-143.1", "Crypto");
        Moneta bit = prezzata("BIT", "0", "0.00");
        bit.InfoPrezzo = quotazione(DATA, "CoinMarketCap");
        assertNull(MovimentiCrypto.DammiMonetaPrioritaria(son, bit, DATA));
    }

    @Test
    void prezzate_coinMarketCapTroppoDiversoDallAltraGamba_vinceLAltra() {
        Moneta usdt = quotata("USDT", "-58.98", "52.06", DATA, "binance");
        Moneta bit = quotata("BIT", "29.88", "0.00", DATA, "CoinMarketCap");
        bit.InfoPrezzo.prezzoUnitario = new BigDecimal("0.0000281");
        bit.Prezzo = "0.00084";
        assertSame(usdt, MovimentiCrypto.DammiMonetaPrioritaria(usdt, bit, DATA),
                "BIT da CoinMarketCap vale 0,00084 contro 52,06 dell'USDT: e' un omonimo");
    }

    @Test
    void prezzate_coinMarketCapVicinoAllAltraGamba_resta() {
        Moneta usdt = quotata("USDT", "-100", "92.00", DATA, "binance");
        usdt.InfoPrezzo.prezzoUnitario = new BigDecimal("0.92");
        Moneta tok = quotata("TOKEN", "10", "85.00", DATA, "CoinMarketCap");
        tok.InfoPrezzo.prezzoUnitario = new BigDecimal("8.5");
        assertSame(tok, MovimentiCrypto.DammiMonetaPrioritaria(usdt, tok, DATA), "85 contro 92: entro il 10%");
    }

    @Test
    void prezzate_coinMarketCapSuEntrambeLeGambe_nessunConfronto() {
        //Due prezzi della stessa fonte inaffidabile: non si sa quale sia sbagliato, resta l'entrata
        Moneta ply = quotata("PLY", "-120.9", "0.15", DATA, "CoinMarketCap");
        ply.InfoPrezzo.prezzoUnitario = new BigDecimal("0.00127");
        Moneta bit = quotata("BIT", "0.4157", "0.00", DATA, "CoinMarketCap");
        bit.InfoPrezzo.prezzoUnitario = new BigDecimal("0.0000037675");
        assertSame(bit, MovimentiCrypto.DammiMonetaPrioritaria(ply, bit, DATA));
    }

    @Test
    void valoriTroppoDiversi_sogliaDelDieciPerCento() {
        assertFalse(Prezzi.ValoriTroppoDiversi(new BigDecimal("110"), new BigDecimal("100")));
        assertFalse(Prezzi.ValoriTroppoDiversi(new BigDecimal("90"), new BigDecimal("100")));
        assertTrue(Prezzi.ValoriTroppoDiversi(new BigDecimal("110.01"), new BigDecimal("100")));
        assertTrue(Prezzi.ValoriTroppoDiversi(new BigDecimal("89.99"), new BigDecimal("100")));
        assertTrue(Prezzi.ValoriTroppoDiversi(new BigDecimal("0.001"), new BigDecimal("52")));
        assertFalse(Prezzi.ValoriTroppoDiversi(BigDecimal.ZERO, new BigDecimal("52")), "senza valore non si confronta");
    }

    @Test
    void daCercare_coinMarketCapOmonimo_vinceLAltraGamba() throws Exception {
        long istante = DATA_ORA + 20 * 60_000L;
        scriviCacheFonte("TSTBIT", istante, "0.0000281", "CoinMarketCap");
        scriviCache("TSTUSD", istante, "0.88");
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("TSTUSD", "-59", "Crypto"), moneta("TSTBIT", "29.88", "Crypto"), istante, null, "");
        assertNotNull(IP);
        assertEquals("TSTUSD", IP.Moneta);
    }

    @Test
    void daCercare_uscitaConVecchioArchivioOrarioMaMinutoInCache_usaIlMinuto() throws Exception {
        //Il caso USDT -> BIT del 19/01/2022 alle 18:27: USDT ha il vecchio archivio orario (18:00), letto per
        //primo, ma in cache c'e' il prezzo al minuto; BIT ha solo una quotazione oraria
        long istante = FunzioniDate.ConvertiDatainLongMinuto(FunzioniDate.ConvertiDatadaLongallOra(DATA_ORA) + ":27");
        DatabaseH2.OLD_XXXEUR_Scrivi(FunzioniDate.ConvertiDatadaLongallOra(istante) + " TSTUSDT", "0.90", false);
        scriviCache("TSTUSDT", istante, "0.88");
        DatabaseH2.OLD_XXXEUR_Scrivi(FunzioniDate.ConvertiDatadaLongallOra(istante) + " TSTBIT2", "1.5", false);

        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(
                moneta("TSTUSDT", "-100", "Crypto"), moneta("TSTBIT2", "58", "Crypto"), istante, null, "");
        assertNotNull(IP);
        assertEquals("TSTUSDT", IP.Moneta);
        assertEquals(0, new BigDecimal("88").compareTo(IP.prezzoQta), "prezzo al minuto, non quello orario (90)");
    }

    @Test
    void prezzate_uscitaConPrezzoOrarioMaMinutoInCache_usaIlMinuto() throws Exception {
        long istante = FunzioniDate.ConvertiDatainLongMinuto(FunzioniDate.ConvertiDatadaLongallOra(DATA_ORA) + ":33");
        scriviCache("TSTUSDT3", istante, "0.87");
        Moneta usdt = quotata("TSTUSDT3", "-100", "90.00", istante - 33 * 60_000L, "DB Interno (Old)");
        Moneta tok = quotata("TSTTOK3", "58", "88.00", istante - 33 * 60_000L, "DB Interno (Old)");
        Moneta scelta = MovimentiCrypto.DammiMonetaPrioritaria(usdt, tok, istante);
        assertSame(usdt, scelta, "l'entrata non ha un prezzo al minuto, l'uscita si'");
        assertEquals(0, new BigDecimal("0.87").compareTo(scelta.InfoPrezzo.prezzoUnitario));
    }

    private static void scriviCacheFonte(String simbolo, long ts, String prezzo, String exchange) throws Exception {
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, exchange);
            ps.setString(3, simbolo);
            ps.setBigDecimal(4, new BigDecimal(prezzo));
            ps.executeUpdate();
        }
    }
}
