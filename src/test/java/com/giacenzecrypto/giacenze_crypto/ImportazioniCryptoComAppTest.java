package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link Importazioni#Ex_CDCAPP_Consolida} su righe con la forma di quelle della segnalazione 27 (export dell'app Crypto.com
 * di novembre 2026; importi e istanti inventati), che finivano tutte fra le sconosciute: gli acquisti ricorrenti scrivono gli euro pagati con importo positivo, mentre
 * l'interesse del lockup CRO e il delisting di un token del basket erano causali assenti dalla mappa.
 * Ogni riga ha il controvalore in EUR, quindi nessuna ricerca prezzi.
 */
class ImportazioniCryptoComAppTest {

    private Map<String, String> mappa;

    @BeforeEach
    void prepara() {
        Importazioni.AzzeraContatori();
        mappa = Importazioni.Ex_CDCAPP_MappaCausali();
        assertNotNull(mappa);
    }

    @AfterEach
    void azzera() {
        Importazioni.AzzeraContatori();
    }

    private List<String[]> consolida(String riga) {
        return Importazioni.Ex_CDCAPP_Consolida(List.of(riga), mappa);
    }

    private static String tipo(String[] mov) {
        return mov[0].split("_")[4];
    }

    @Test
    void acquistoRicorrenteConEuroPositivi_eUnAcquistoPiuLArrivoDegliEuro() {
        List<String[]> movimenti = consolida(
                "2025-03-01 10:00:00,Bought BTC,EUR,50.00,BTC,0.0005,EUR,50.0,55.0,recurring_buy_order,");

        assertEquals(0, Importazioni.TrasazioniSconosciute, Importazioni.Sconosciuti());
        assertEquals(2, movimenti.size());
        String[] acquisto = movimenti.stream().filter(m -> tipo(m).equals("AC")).findFirst().orElseThrow();
        assertEquals("EUR", acquisto[8]);
        assertTrue(Funzioni.isNegativo(acquisto[10]), "gli euro escono: " + acquisto[10]);
        assertEquals("BTC", acquisto[11]);
        assertEquals("0.0005", acquisto[13]);
        String[] arrivo = movimenti.stream().filter(m -> m != acquisto).findFirst().orElseThrow();
        assertEquals("DF", tipo(arrivo));
        assertEquals("EUR", arrivo[11]);
    }

    @Test
    void acquistoConEuroNegativi_restaComePrima() {
        List<String[]> movimenti = consolida(
                "2025-03-01 10:00:00,Bought BTC,EUR,-50.00,BTC,0.0005,EUR,50.0,55.0,viban_purchase,");

        assertEquals(0, Importazioni.TrasazioniSconosciute, Importazioni.Sconosciuti());
        assertEquals(2, movimenti.size());
        assertTrue(movimenti.stream().anyMatch(m -> tipo(m).equals("AC") && m[8].equals("EUR")));
    }

    @Test
    void interesseLockupCRO_eUnaReward() {
        List<String[]> movimenti = consolida("2026-04-01 12:00:00,Cardholder CRO Stake Reward,CRO,2,,,EUR,"
                + "0.1,0.11,finance.lockup.dpos_non_compound_interest.crypto_wallet,");

        assertEquals(0, Importazioni.TrasazioniSconosciute, Importazioni.Sconosciuti());
        assertEquals(1, movimenti.size());
        assertEquals("RW", tipo(movimenti.get(0)));
        assertEquals("CRO", movimenti.get(0)[11]);
    }

    @Test
    void delistingDalBasket_uscitaDalBasketVersoIlCryptoWallet_accreditoIgnorato() {
        List<String[]> addebito = consolida("2026-05-01 08:00:00,TON Delisted,TON,-4,,,EUR,5.2,"
                + "5.9,finance.crypto_basket.delist.debit,");
        List<String[]> accredito = consolida("2026-05-01 08:00:02,Featured 10 Basket (TON) -> TON,TON,4,,,EUR,"
                + "5.25,5.95,finance.crypto_basket.delist.credit,");

        assertEquals(0, Importazioni.TrasazioniSconosciute, Importazioni.Sconosciuti());
        assertEquals(2, addebito.size());
        assertTrue(addebito.stream().allMatch(m -> tipo(m).equals("TI")));
        String[] uscita = addebito.stream().filter(m -> !m[8].isBlank()).findFirst().orElseThrow();
        String[] entrata = addebito.stream().filter(m -> !m[11].isBlank()).findFirst().orElseThrow();
        assertEquals("Crypto Basket", uscita[4]);
        assertEquals("Crypto Wallet", entrata[4]);
        assertTrue(accredito.isEmpty(), "l'accredito ripete l'addebito: il trasferimento e' gia' tutto nell'addebito");
    }
}
