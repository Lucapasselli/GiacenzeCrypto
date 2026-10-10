package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ID dei movimenti di rettifica creati da "Giacenze a data"
 * ({@link Principale_GiacenzeaData#RendiUnivocoIDRettifica}).
 * <p>
 * {@code IncDecID} verifica l'unicità con la categoria provvisoria (DC/PC), poi {@code creaMovimento} la
 * sostituisce con quella del tipo scelto: una seconda rettifica "Provento" sullo stesso movimento prendeva
 * l'ID della prima ({@code ..._Binance$_001_001_RW}) e la sovrascriveva.
 */
class RettificaGiacenzaIDTest {

    private static final String PRELIEVO = "20260624220331_Binance_001_001_PC";
    private static final String PRIMA_RETTIFICA = "20260624220331_Binance$_001_001_RW";

    @BeforeEach
    void setUp() {
        Principale.MappaCryptoWallet.clear();
        Principale.MappaCryptoWallet.put(PRELIEVO, new String[]{PRELIEVO});
        Principale.MappaCryptoWallet.put(PRIMA_RETTIFICA, new String[]{PRIMA_RETTIFICA});
    }

    /** Lo stesso giro della funzione: ID provvisorio DC, poi la categoria del tipo EARN. */
    private static String[] secondaRettificaProvento() {
        String[] Parti = PRELIEVO.split("_");
        Parti[4] = "DC";
        String Provvisorio = MovimentiCrypto.IncDecID(String.join("_", Parti), 1, false);
        assertNotNull(Provvisorio);
        String[] p = Provvisorio.split("_");
        return new String[]{p[0] + "_" + p[1] + "_" + p[2] + "_" + p[3] + "_RW"};
    }

    @Test
    void unaSecondaRettificaNonSovrascriveLaPrima() {
        String[] RT = secondaRettificaProvento();
        assertEquals(PRIMA_RETTIFICA, RT[0], "premessa: senza correzione l'ID collide");

        assertTrue(Principale_GiacenzeaData.RendiUnivocoIDRettifica(RT, PRELIEVO, false));

        assertNotEquals(PRIMA_RETTIFICA, RT[0]);
        assertNull(Principale.MappaCryptoWallet.get(RT[0]));
        assertTrue(RT[0].endsWith("_RW"));
        Principale.MappaCryptoWallet.put(RT[0], RT);
        //Resta prima del movimento selezionato, dopo la rettifica precedente
        List<String> Ordine = new ArrayList<>(Principale.MappaCryptoWallet.keySet());
        assertEquals(List.of(PRIMA_RETTIFICA, RT[0], PRELIEVO), Ordine);
    }

    @Test
    void unIDLiberoNonVieneToccato() {
        String[] RT = {"20260624220331_Binance~_001_001_PC"};
        assertTrue(Principale_GiacenzeaData.RendiUnivocoIDRettifica(RT, PRELIEVO, true));
        assertEquals("20260624220331_Binance~_001_001_PC", RT[0]);
    }

    @Test
    void unaRettificaDopoIlMovimentoRestaDopo() {
        String Occupato = "20260624220331_Binance~_001_001_CM";
        Principale.MappaCryptoWallet.put(Occupato, new String[]{Occupato});
        String[] RT = {Occupato};

        assertTrue(Principale_GiacenzeaData.RendiUnivocoIDRettifica(RT, PRELIEVO, true));

        assertNotEquals(Occupato, RT[0]);
        Principale.MappaCryptoWallet.put(RT[0], RT);
        //Subito dopo il movimento selezionato, prima della rettifica che occupava l'ID
        assertEquals(List.of(PRIMA_RETTIFICA, PRELIEVO, RT[0], Occupato),
                new ArrayList<>(Principale.MappaCryptoWallet.keySet()));
    }
}
