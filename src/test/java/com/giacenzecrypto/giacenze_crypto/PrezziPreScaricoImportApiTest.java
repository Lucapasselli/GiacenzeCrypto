package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pre-scarico dei prezzi nelle importazioni via API: quali coppie (moneta, istante) si chiedono per OKX (righe a 19
 * campi, {@link Importazioni#Ex_OKX_SimboliDaPrezzare}) e per Binance (righe convertite a secco,
 * {@link Prezzi#SimboliDaMovimenti}), con la fonte del consumatore, e la conversione a secco
 * ({@link Prezzi#SenzaValorizzare}). Nessuna rete, nessun database, nessun processo Node.
 */
class PrezziPreScaricoImportApiTest {

    private static final String DATA = "2025-06-10 14:30:15";

    @BeforeAll
    static void apre() {
        Prezzi.CompilaMoneteStessoPrezzo();
        Interruzione.Azzera();
    }

    /** Riga intermedia OKX a 19 campi: data, moneta e quantita', moneta e quantita' della commissione. */
    private static String[] rigaOKX(String moneta, String qta, String monetaFee, String qtaFee) {
        String[] r = new String[19];
        java.util.Arrays.fill(r, "");
        r[0] = DATA;
        r[1] = "OKX";
        r[5] = moneta;
        r[6] = qta;
        r[11] = monetaFee;
        r[12] = qtaFee;
        return r;
    }

    @Test
    void okxChiedeLaMonetaELaCommissioneAllIstanteDellaRiga() {
        long istante = FunzioniDate.ConvertiDatainLongSecondo(DATA);
        List<Prezzi.SimboloIstante> coppie = Importazioni.Ex_OKX_SimboliDaPrezzare(List.<String[]>of(
                rigaOKX("BTC", "0.5", "SOL", "-0.001")));
        assertEquals(List.of(new Prezzi.SimboloIstante("BTC", istante), new Prezzi.SimboloIstante("SOL", istante)), coppie);
    }

    @Test
    void okxSaltaValuteQuantitaNulleECommissioniAssenti() {
        List<Prezzi.SimboloIstante> coppie = Importazioni.Ex_OKX_SimboliDaPrezzare(List.<String[]>of(
                rigaOKX("EUR", "100", "", ""),
                rigaOKX("USD", "100", "", ""),
                rigaOKX("ETH", "0", "ETH", "0"),
                rigaOKX("ETH", "1", "", "")));
        assertEquals(1, coppie.size());
        assertEquals("ETH", coppie.get(0).simbolo());
    }

    @Test
    void leRichiestePartonoDallExchangeDelConsumatore() {
        long istante = FunzioniDate.ConvertiDatainLongSecondo(DATA);
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerSimboli(
                List.of(new Prezzi.SimboloIstante("BTC", istante)), Prezzi.ExchangeRiconosciuto("okx"));
        assertFalse(richieste.isEmpty());
        for (Prezzi.RichiestaPrezzo r : richieste) assertEquals("okx", r.exchangePreferito);

        //Binance valorizza col nome dell'exchange come fonte ("Binance"), che diventa l'id CCXT
        assertEquals("binance", Prezzi.ExchangeRiconosciuto("Binance"));
        //Senza fonte resta la cascata di sempre
        for (Prezzi.RichiestaPrezzo r : Prezzi.RaccogliRichiestePerSimboli(List.of(new Prezzi.SimboloIstante("BTC", istante))))
            assertEquals("", r.exchangePreferito);
    }

    /** Movimento gia' formato: data al minuto in [1], gambe in [8]/[9] e [11]/[12]. */
    private static String[] movimento(String uscita, String tipoUscita, String entrata, String tipoEntrata) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[1] = "2025-06-10 14:30";
        v[8] = uscita;
        v[9] = tipoUscita;
        v[11] = entrata;
        v[12] = tipoEntrata;
        return v;
    }

    @Test
    void binanceChiedeLeDueGambeCryptoMaNonQuelleCambiateConValuta() {
        long istante = FunzioniDate.ConvertiDatainLongMinuto("2025-06-10 14:30");
        List<Prezzi.SimboloIstante> coppie = Prezzi.SimboliDaMovimenti(List.<String[]>of(
                movimento("USDT", "Crypto", "BTC", "Crypto"),
                movimento("EUR", "FIAT", "ETH", "Crypto"),
                movimento("", "", "BNB", "Crypto")));
        assertEquals(List.of(new Prezzi.SimboloIstante("USDT", istante), new Prezzi.SimboloIstante("BTC", istante),
                new Prezzi.SimboloIstante("BNB", istante)), coppie);
    }

    @Test
    void laConversioneASeccoNonValorizzaERipristinaAllUscita() {
        Moneta euro = new Moneta();
        euro.InserisciValori("EUR", "10", "", "FIAT");
        long istante = FunzioniDate.ConvertiDatainLongSecondo(DATA);

        assertNotNull(Prezzi.DammiPrezzoInfoTransazione(euro, null, istante, null, "okx"));
        assertNull(Prezzi.SenzaValorizzare(() -> Prezzi.DammiPrezzoInfoTransazione(euro, null, istante, null, "okx")));
        //Annidata: all'uscita di quella interna si resta a secco, all'uscita di quella esterna no
        assertNull(Prezzi.SenzaValorizzare(() -> {
            Prezzi.SenzaValorizzare(() -> null);
            return Prezzi.DammiPrezzoInfoTransazione(euro, null, istante, null, "okx");
        }));
        Prezzi.InfoPrezzo dopo = Prezzi.DammiPrezzoInfoTransazione(euro, null, istante, null, "okx");
        assertNotNull(dopo);
        assertEquals(0, new java.math.BigDecimal("10").compareTo(dopo.prezzoQta));
    }
}
