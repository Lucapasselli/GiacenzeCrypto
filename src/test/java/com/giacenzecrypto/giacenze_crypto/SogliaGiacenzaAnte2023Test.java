package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regole del controllo della soglia dei 51.645,69 euro ({@link SogliaGiacenzaAnte2023}). Dati inventati e
 * prezzi iniettati: nessun accesso a rete, cache o database. Il 2021 comincia di venerdì e il 1° marzo 2021
 * è un lunedì, quindi le serie di marzo non incontrano festività.
 */
class SogliaGiacenzaAnte2023Test {

    /** Prezzo unitario del 1° gennaio per simbolo; un simbolo assente non ha prezzo. */
    private final Map<String, String> prezzi = new HashMap<>();
    /** Chiamate alla fonte dei prezzi: "simbolo@istante". */
    private final List<String> chiamate = new ArrayList<>();

    private final Calcoli_RW_Giacenze.PrezzoGiacenza fonte = (m, istante) -> {
        chiamate.add(m.Moneta + "@" + istante);
        String p = prezzi.get(m.Moneta);
        return p == null ? null : new BigDecimal(p).multiply(new BigDecimal(m.Qta)).toPlainString();
    };

    @BeforeEach
    void setUp() {
        prezzi.clear();
        chiamate.clear();
        Principale.MappaCryptoWallet.clear();
        prezzi.put("BTC", "100");
    }

    private static SogliaGiacenzaAnte2023.Gamba gamba(String giorno, String simbolo, String qta) {
        Moneta m = new Moneta();
        m.InserisciValori(simbolo, qta, "", "Crypto");
        m.Rete = "";
        return new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse(giorno), m, new BigDecimal(qta));
    }

    private SogliaGiacenzaAnte2023.Esito calcola(int anno, SogliaGiacenzaAnte2023.Gamba... gambe) {
        return SogliaGiacenzaAnte2023.Calcola(anno, Arrays.asList(gambe), fonte, d -> false);
    }

    @Test
    void seiGiorniLavorativiNonBastano_setteSi() {
        // 600 BTC a 100 euro = 60.000, sopra soglia. Dal lunedì 1 marzo il saldo vale fino al giorno prima della vendita.
        var sei = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-09", "BTC", "-600"));
        assertFalse(sei.superata, "1,2,3,4,5 e 8 marzo sono sei giorni lavorativi");
        assertEquals(6, sei.serieMassima);
        assertNull(sei.inizioPeriodo);

        var sette = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-10", "BTC", "-600"));
        assertTrue(sette.superata, "con il 9 marzo i giorni lavorativi diventano sette: il fine settimana non interrompe");
        assertEquals(LocalDate.parse("2021-03-01"), sette.inizioPeriodo);
        assertEquals(LocalDate.parse("2021-03-09"), sette.finePeriodo);
    }

    @Test
    void laSogliaVaSuperataNonRaggiunta() {
        prezzi.put("BTC", "1");
        var pari = calcola(2021, gamba("2021-03-01", "BTC", "51645.69"), gamba("2021-03-20", "BTC", "-51645.69"));
        assertFalse(pari.superata, "esattamente 51.645,69 non e' superiore");
        assertEquals(0, pari.serieMassima);
        assertEquals(0, new BigDecimal("51645.69").compareTo(pari.valoreMassimo));

        var oltre = calcola(2021, gamba("2021-03-01", "BTC", "51645.70"), gamba("2021-03-20", "BTC", "-51645.70"));
        assertTrue(oltre.superata);
    }

    @Test
    void unFestivoNonContaENonInterrompe() {
        var gambe = Arrays.asList(gamba("2021-03-01", "BTC", "600"), gamba("2021-03-10", "BTC", "-600"));
        // Con il 3 marzo festivo i giorni lavorativi fino al 9 sono sei: 1, 2, 4, 5, 8, 9.
        var festivo = SogliaGiacenzaAnte2023.Calcola(2021, gambe, fonte, d -> d.equals(LocalDate.parse("2021-03-03")));
        assertFalse(festivo.superata);
        assertEquals(6, festivo.serieMassima);

        var vendutaDopo = Arrays.asList(gamba("2021-03-01", "BTC", "600"), gamba("2021-03-11", "BTC", "-600"));
        var settimo = SogliaGiacenzaAnte2023.Calcola(2021, vendutaDopo, fonte, d -> d.equals(LocalDate.parse("2021-03-03")));
        assertTrue(settimo.superata);
        assertEquals(LocalDate.parse("2021-03-10"), settimo.finePeriodo);
    }

    @Test
    void unGiornoSottoSogliaAzzeraLaSerie() {
        var e = calcola(2021,
                gamba("2021-03-01", "BTC", "600"), gamba("2021-03-05", "BTC", "-600"),
                gamba("2021-03-08", "BTC", "600"), gamba("2021-03-15", "BTC", "-600"));
        assertFalse(e.superata, "4 giorni e poi 5: mai sette di fila");
        assertEquals(5, e.serieMassima);
    }

    @Test
    void ilSaldoDegliAnniPrecedentiEraGiaInGiacenza() {
        var e = calcola(2021, gamba("2020-06-01", "BTC", "600"));
        assertTrue(e.superata);
        // 1 gennaio 2021 e' un venerdi: 1, 4, 5, 6, 7, 8 e 11 gennaio.
        assertEquals(LocalDate.parse("2021-01-01"), e.inizioPeriodo);
        assertEquals(LocalDate.parse("2021-01-11"), e.finePeriodo);
    }

    @Test
    void lePosizioniDopoFineAnnoNonContano() {
        var e = calcola(2021, gamba("2022-01-03", "BTC", "600"));
        assertFalse(e.superata);
        assertEquals(0, BigDecimal.ZERO.compareTo(e.valoreMassimo));
    }

    @Test
    void prezzoDelPrimoGennaio_unaSolaVoltaPerToken() {
        prezzi.put("ETH", "10");
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-02", "ETH", "10"),
                gamba("2021-03-20", "BTC", "-600"));
        long primoGennaio = FunzioniDate.ConvertiDatainLongMinuto("2021-01-01 00:00");
        assertTrue(chiamate.contains("BTC@" + primoGennaio));
        assertTrue(chiamate.contains("ETH@" + primoGennaio));
        assertEquals(1, chiamate.stream().filter(c -> c.startsWith("BTC@")).count(),
                "il prezzo non si richiede ogni giorno");
        assertEquals(1, chiamate.stream().filter(c -> c.startsWith("ETH@")).count());
        assertEquals(0, new BigDecimal("60100").compareTo(e.valoreMassimo));
        assertEquals(LocalDate.parse("2021-03-02"), e.giornoMassimo);
    }

    @Test
    void tokenSenzaPrezzo_valeZeroEVieneSegnalato() {
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-01", "MISTERO", "999999"),
                gamba("2021-03-20", "BTC", "-600"));
        assertTrue(e.incompleto());
        assertEquals(List.of("MISTERO"), e.tokenSenzaPrezzo);
        assertEquals(0, new BigDecimal("60000").compareTo(e.valoreMassimo), "il token senza prezzo non pesa");
        assertTrue(e.superata, "il resto basta");
    }

    @Test
    void monetePrezzoMancante_portanoAddressReteTipoEGiacenzaMassima() {
        Moneta nft = new Moneta();
        nft.InserisciValori("PUNK", "1", "0xabc", "NFT");
        nft.Rete = "ETH";
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-01", "MISTERO", "5"),
                gamba("2021-03-03", "MISTERO", "10"), gamba("2021-03-05", "MISTERO", "-12"),
                new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse("2021-03-02"), nft, BigDecimal.ONE),
                gamba("2021-03-20", "BTC", "-600"));
        assertEquals(2, e.monetePrezzoMancante.size());
        Moneta mistero = e.monetePrezzoMancante.get(0).Moneta.equals("MISTERO") ? e.monetePrezzoMancante.get(0) : e.monetePrezzoMancante.get(1);
        assertEquals("15", mistero.Qta, "la giacenza massima e' 15 (5 + 10), prima del prelievo");
        Moneta punk = e.monetePrezzoMancante.get(0).Moneta.equals("PUNK") ? e.monetePrezzoMancante.get(0) : e.monetePrezzoMancante.get(1);
        assertEquals("NFT", punk.Tipo);
        assertEquals("0xabc", punk.MonetaAddress);
        assertEquals("ETH", punk.Rete);
        assertEquals(List.of("MISTERO", "PUNK"), e.tokenSenzaPrezzo);
    }

    @Test
    void zeroEUnPrezzo_nonUnPrezzoMancante() {
        prezzi.put("MISTERO", "0");
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-01", "MISTERO", "999999"),
                gamba("2021-03-20", "BTC", "-600"));
        assertFalse(e.incompleto(), "un prezzo zero assegnato dall'utente toglie il token dai senza prezzo");
        assertTrue(e.monetePrezzoMancante.isEmpty());
        assertEquals(0, new BigDecimal("60000").compareTo(e.valoreMassimo));
    }

    @Test
    void unTokenSenzaPrezzoMaSenzaGiacenzaNonSiSegnala() {
        var e = calcola(2021, gamba("2021-03-01", "MISTERO", "5"), gamba("2021-03-01", "MISTERO", "-5"));
        assertFalse(e.incompleto());
    }

    @Test
    void unSaldoNegativoValeZero_nonAssoluto() {
        var e = calcola(2021, gamba("2021-03-01", "BTC", "-600"));
        assertFalse(e.superata);
        assertEquals(0, BigDecimal.ZERO.compareTo(e.valoreMassimo));
    }

    @Test
    void ilTrasferimentoFraWalletPropriNonCambiaLaGiacenza() {
        // Esce da un wallet ed entra nell'altro lo stesso giorno: il saldo complessivo e' uguale.
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"),
                gamba("2021-03-02", "BTC", "-600"), gamba("2021-03-02", "BTC", "600"),
                gamba("2021-03-20", "BTC", "-600"));
        assertTrue(e.superata);
        assertEquals(LocalDate.parse("2021-03-01"), e.inizioPeriodo);
    }

    @Test
    void dettaglioGiornalieroSoloGiorniLavorativi() {
        var e = calcola(2021, gamba("2021-03-01", "BTC", "600"));
        assertEquals(261, e.giorni.size(), "il 2021 ha 261 giorni da lunedi a venerdi");
        assertTrue(e.giorni.stream().noneMatch(g -> g.data.getDayOfWeek().getValue() > 5));
        assertFalse(e.giorni.get(0).sopraSoglia);
        assertTrue(e.giorni.stream().filter(g -> g.data.equals(LocalDate.parse("2021-03-01"))).findFirst().get().sopraSoglia);
    }

    @Test
    void festivitaItaliane() {
        // Pasqua 2021: 4 aprile, 2022: 17 aprile. Il lunedi successivo e' festivo.
        assertEquals(LocalDate.parse("2021-04-04"), SogliaGiacenzaAnte2023.pasqua(2021));
        assertEquals(LocalDate.parse("2022-04-17"), SogliaGiacenzaAnte2023.pasqua(2022));
        assertEquals(LocalDate.parse("2019-04-21"), SogliaGiacenzaAnte2023.pasqua(2019));
        assertTrue(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse("2021-04-05")));
        assertTrue(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse("2022-04-18")));
        for (String f : new String[] {"2021-01-01", "2021-01-06", "2021-04-25", "2021-05-01", "2021-06-02",
            "2021-08-15", "2021-11-01", "2021-12-08", "2021-12-25", "2021-12-26"}) {
            assertTrue(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse(f)), f);
        }
        assertFalse(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse("2021-04-06")));
        assertFalse(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse("2021-03-01")));
        assertFalse(SogliaGiacenzaAnte2023.festivoItaliano(LocalDate.parse("2021-12-24")));
        assertFalse(SogliaGiacenzaAnte2023.lavorativo(LocalDate.parse("2021-03-06"), d -> false), "sabato");
        assertFalse(SogliaGiacenzaAnte2023.lavorativo(LocalDate.parse("2021-03-07"), d -> false), "domenica");
        assertTrue(SogliaGiacenzaAnte2023.lavorativo(LocalDate.parse("2021-03-08"), d -> false));
    }

    // --- Dall'archivio: FIAT, NFT e SCAM fuori, uscite col segno ---

    private static int progressivo = 0;

    private static void mov(String data, String cat, String monU, String tipoU, String qtaU,
            String monE, String tipoE, String qtaE) {
        String id = data.replace("-", "").replace(":", "").replace(" ", "") + "_TEST_"
                + String.format("%03d", ++progressivo) + "_001_" + cat;
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "Kraken";
        m[8] = monU;  m[9] = tipoU;  m[10] = qtaU;
        m[11] = monE; m[12] = tipoE; m[13] = qtaE;
        Principale.MappaCryptoWallet.put(id, m);
    }

    @Test
    void ilDollaroContaConIlSuoCambio_euroEAltreValuteNo() {
        prezzi.put("USD", "0.8");
        mov("2021-03-01 10:00", "DF", "", "", "", "USD", "FIAT", "70000");
        mov("2021-03-01 11:00", "DF", "", "", "", "EUR", "FIAT", "500000");
        mov("2021-03-01 12:00", "DF", "", "", "", "GBP", "FIAT", "500000");
        prezzi.put("GBP", "1.2");

        var e = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertEquals(0, new BigDecimal("56000").compareTo(e.valoreMassimo), "solo i 70.000 USD a 0,8");
        assertFalse(e.incompleto());

        // Il cambio si chiede una sola volta, senza address ne' rete, il 1° gennaio.
        long primoGennaio = FunzioniDate.ConvertiDatainLongMinuto("2021-01-01 00:00");
        assertEquals(List.of("USD@" + primoGennaio), chiamate);
        mov("2021-03-10 10:00", "PF", "USD", "FIAT", "-70000", "", "", "");
        assertTrue(SogliaGiacenzaAnte2023.Calcola(2021, fonte).superata);
    }

    @Test
    void nftSenzaPrezzo_valeZeroEVieneSegnalato() {
        mov("2021-03-01 10:00", "DC", "", "", "", "PUNK", "NFT", "1");
        var e = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertEquals(List.of("PUNK"), e.tokenSenzaPrezzo);
        assertEquals(0, BigDecimal.ZERO.compareTo(e.valoreMassimo));
    }

    @Test
    void dollaroSenzaCambio_valeZeroEVieneSegnalato() {
        mov("2021-03-01 10:00", "DF", "", "", "", "USD", "FIAT", "70000");
        var e = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertEquals(List.of("USD"), e.tokenSenzaPrezzo);
        assertFalse(e.superata);
    }

    @Test
    void dallArchivio_criptoENftContano_euroEScamNo() {
        mov("2021-03-01 10:00", "AC", "EUR", "FIAT", "-70000", "BTC", "Crypto", "600");
        mov("2021-03-01 11:00", "DC", "", "", "", "PUNK", "NFT", "1");
        mov("2021-03-01 12:00", "DC", "", "", "", "TRUFFA **", "Crypto", "1000000");
        prezzi.put("PUNK", "1000");
        prezzi.put("TRUFFA **", "1000000");

        var e = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertEquals(0, new BigDecimal("61000").compareTo(e.valoreMassimo),
                "600 BTC e l'NFT: ne' l'euro, ne' il token SCAM");
        assertFalse(e.incompleto());

        mov("2021-03-10 10:00", "VC", "BTC", "Crypto", "-600", "EUR", "FIAT", "70000");
        var venduto = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertTrue(venduto.superata);
        assertEquals(LocalDate.parse("2021-03-09"), venduto.finePeriodo);
    }

    // --- Token senza prezzo del 1° gennaio: prezzo di acquisto, identificabile e correggibile a mano ---

    private static SogliaGiacenzaAnte2023.Gamba gamba(String giorno, String simbolo, String qta, String valoreEuro) {
        Moneta m = new Moneta();
        m.InserisciValori(simbolo, qta, "0xabc", "Crypto");
        m.Rete = "BSC";
        return new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse(giorno), m, new BigDecimal(qta),
                valoreEuro == null ? null : new BigDecimal(valoreEuro));
    }

    @Test
    void senzaPrezzoDelPrimoGennaio_siUsaIlPrezzoDiAcquisto_eIlTokenResta_identificabile() {
        // 1.000 token comprati a 60 euro l'uno (valore del movimento 60.000), mai quotati al 1° gennaio
        var e = calcola(2021, gamba("2021-03-01", "NUOVO", "1000", "60000"), gamba("2021-03-20", "NUOVO", "-1000", "70000"));
        assertFalse(e.incompleto(), "con un prezzo di acquisto l'anno e' verificabile");
        assertTrue(e.tokenSenzaPrezzo.isEmpty());
        assertTrue(e.monetePrezzoMancante.isEmpty());
        assertTrue(e.superata, "60.000 euro per piu' di sette giorni lavorativi");
        assertEquals(1, e.prezzoDiAcquisto.size());
        var a = e.prezzoDiAcquisto.get(0);
        assertEquals("NUOVO", a.moneta.Moneta);
        assertEquals("0xabc", a.moneta.MonetaAddress);
        assertEquals("BSC", a.moneta.Rete);
        assertEquals("1000", a.moneta.Qta, "giacenza massima");
        assertEquals(0, new BigDecimal("60").compareTo(a.prezzoUnitario));
        assertEquals(LocalDate.parse("2021-03-01"), a.data);
        assertTrue(e.haPrezziDaAssegnare());
    }

    @Test
    void ilPrezzoDelPrimoGennaioViene_primaDelPrezzoDiAcquisto() {
        prezzi.put("NUOVO", "1"); //assegnato a mano (o trovato su una fonte)
        var e = calcola(2021, gamba("2021-03-01", "NUOVO", "1000", "60000"), gamba("2021-03-20", "NUOVO", "-1000", "70000"));
        assertTrue(e.prezzoDiAcquisto.isEmpty(), "il token esce dall'elenco appena ha il suo prezzo");
        assertFalse(e.haPrezziDaAssegnare());
        assertFalse(e.superata);
        assertEquals(0, new BigDecimal("1000").compareTo(e.valoreMassimo));
    }

    @Test
    void ilPrezzoDiAcquistoEIlPrimoIngressoConUnValore() {
        // l'uscita non conta, un ingresso senza valore nemmeno, quelli dopo il 31 dicembre neppure
        var e = calcola(2021,
                gamba("2021-02-01", "NUOVO", "10", null),
                gamba("2021-02-02", "NUOVO", "10", "50"),
                gamba("2021-02-03", "NUOVO", "-5", "999"),
                gamba("2021-02-04", "NUOVO", "10", "1000"),
                gamba("2022-01-04", "NUOVO", "10", "9999"));
        var a = e.prezzoDiAcquisto.get(0);
        assertEquals(0, new BigDecimal("5").compareTo(a.prezzoUnitario), "50 euro / 10 token, il secondo ingresso");
        assertEquals(LocalDate.parse("2021-02-02"), a.data);

        var dopo = calcola(2021, gamba("2021-02-01", "NUOVO", "10", null), gamba("2022-01-04", "NUOVO", "10", "9999"));
        assertEquals(List.of("NUOVO"), dopo.tokenSenzaPrezzo, "senza nessun ingresso valorizzato nell'anno il token vale zero e si segnala");
        assertTrue(dopo.prezzoDiAcquisto.isEmpty());
        assertTrue(dopo.incompleto());
    }

    @Test
    void dallArchivio_ilValoreDelMovimentoDiventaIlPrezzoDiAcquisto() {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        String id = "202103011000_TEST_990_001_DC";
        m[0] = id;
        m[1] = "2021-03-01 10:00";
        m[3] = "Kraken";
        m[11] = "NUOVO"; m[12] = "Crypto"; m[13] = "1000";
        m[15] = "60000.00";
        Principale.MappaCryptoWallet.put(id, m);

        var e = SogliaGiacenzaAnte2023.Calcola(2021, fonte);
        assertEquals(1, e.prezzoDiAcquisto.size());
        assertEquals(0, new BigDecimal("60").compareTo(e.prezzoDiAcquisto.get(0).prezzoUnitario));
        assertTrue(e.superata);
    }
}
