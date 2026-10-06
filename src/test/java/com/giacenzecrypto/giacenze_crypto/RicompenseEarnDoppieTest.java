package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link RicompenseEarnDoppie}: le righe del conto Earn di Binance ("Rewards Income") importate insieme alla loro
 * gemella del conto Spot ("Interest" / "Rewards", stessa moneta e quantità) sono lo stesso Bonus Tiered APR scritto due
 * volte, da togliere. Una riga Earn senza gemella, troppo lontana o in un gruppo classificato non si tocca, e ogni riga
 * Spot fa da gemella a una sola Earn.
 */
class RicompenseEarnDoppieTest {

    private final Map<String, String[]> Mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private String[] ricompensa(String ID, String Causale, String Moneta, String Qta) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = "EARN";
        v[7] = Causale;
        v[11] = Moneta;
        v[13] = Qta;
        v[15] = "0.10";
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        Mappa.put(ID, v);
        return v;
    }

    private static final String EARN_F = "Simple Earn Flexible - Rewards Income";
    private static final String SPOT_F = "Simple Earn Flexible Interest";
    private static final String EARN_L = "Simple Earn Locked - Rewards Income";
    private static final String SPOT_L = "Simple Earn Locked Rewards";

    @Test
    void laRigaEarnConLaGemellaSpotEUnDoppione() {
        String[] e = ricompensa("20240315043528_Binance_001_001_RW", EARN_F, "USDC", "0.25");
        String[] s = ricompensa("20240315053626_Binance_001_001_RW", SPOT_F, "USDC", "0.250");
        String[] el = ricompensa("20240315061000_Binance_001_001_RW", EARN_L, "ETH", "0.0001");
        ricompensa("20240315061000_Binance_002_001_RW", SPOT_L, "ETH", "0.0001");

        List<String[]> d = RicompenseEarnDoppie.Trova(Mappa);

        assertEquals(List.of(e, el), d, "la quantità si confronta come numero");
        assertFalse(d.contains(s), "la riga Spot resta");
        assertEquals(0, new BigDecimal("0.20").compareTo(RicompenseEarnDoppie.Valore(d)));
    }

    @Test
    void senzaGemella_oTroppoLontana_oAltraMoneta_restano() {
        ricompensa("20240315043528_Binance_001_001_RW", EARN_F, "USDC", "0.25");
        ricompensa("20240317053626_Binance_001_001_RW", SPOT_F, "USDC", "0.25");
        ricompensa("20240318043528_Binance_001_001_RW", EARN_F, "BNB", "0.001");
        ricompensa("20240318053528_Binance_001_001_RW", SPOT_F, "ETH", "0.001");
        ricompensa("20240319043528_Binance_001_001_RW", EARN_F, "BTC", "0.001");
        ricompensa("20240319053528_Binance_001_001_RW", SPOT_L, "BTC", "0.001");

        assertTrue(RicompenseEarnDoppie.Trova(Mappa).isEmpty(), "due giorni dopo, altra moneta, altro prodotto");
    }

    @Test
    void unaRigaSpotFaDaGemellaAUnaSolaEarn() {
        String[] e1 = ricompensa("20240315043528_Binance_001_001_RW", EARN_F, "BNB", "0.0000035");
        ricompensa("20240315043530_Binance_001_001_RW", EARN_F, "BNB", "0.0000035");
        ricompensa("20240315053626_Binance_001_001_RW", SPOT_F, "BNB", "0.0000035");

        assertEquals(List.<String[]>of(e1), RicompenseEarnDoppie.Trova(Mappa), "la seconda riga Earn non ha più una gemella libera");
    }

    @Test
    void unaRigaEarnInUnGruppoClassificatoNonSiTocca() {
        String[] e = ricompensa("20240315043528_Binance_001_001_RW", EARN_F, "USDC", "0.25");
        ricompensa("20240315053626_Binance_001_001_RW", SPOT_F, "USDC", "0.25");
        e[20] = "20240315043528_Binance_002_001_PC";

        assertTrue(RicompenseEarnDoppie.Trova(Mappa).isEmpty());
    }

    @Test
    void laPuliziaToglieSoloLeRigheEarn() {
        String[] e = ricompensa("20240315043528_Binance_001_001_RW", EARN_F, "USDC", "0.25");
        String[] s = ricompensa("20240315053626_Binance_001_001_RW", SPOT_F, "USDC", "0.25");
        Principale.MappaCryptoWallet.clear();
        Principale.MappaCryptoWallet.putAll(Mappa);
        try {
            assertEquals(1, RicompenseEarnDoppie.Rimuovi(RicompenseEarnDoppie.Trova(Principale.MappaCryptoWallet)));
            assertNull(Principale.MappaCryptoWallet.get(e[0]));
            assertSame(s, Principale.MappaCryptoWallet.get(s[0]));
            assertTrue(RicompenseEarnDoppie.Trova(Principale.MappaCryptoWallet).isEmpty(), "una seconda volta non c'è nulla");
        } finally {
            Principale.MappaCryptoWallet.clear();
            MovimentiStorico.AzzeraBuffer();
        }
    }
}
