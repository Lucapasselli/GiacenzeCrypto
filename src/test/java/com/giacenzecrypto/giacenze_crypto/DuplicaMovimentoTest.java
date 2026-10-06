package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link Funzioni#DuplicaMovimento}: il duplicato è un movimento indipendente. Un movimento che fa parte di un
 * gruppo classificato non si duplica (il duplicato si dichiarerebbe parte di un gruppo che non lo cita, e un deposito
 * fra gruppi wallet diversi farebbe spostare due volte il costo di carico); negli altri casi il duplicato tiene la
 * classificazione singola e il documento di origine, ma non lo storico, le commissioni e l'operazione dell'originale.
 */
class DuplicaMovimentoTest {

    @BeforeEach
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String ID) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2023-01-01 10:00";
        v[3] = "Binance";
        v[5] = "DEPOSITO CRYPTO";
        v[11] = "BTC";
        v[13] = "0.01";
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    @Test
    void unMovimentoDiUnGruppo_nonSiDuplica() {
        String[] d = movimento("20230101100000_Binance_001_001_DC");
        d[18] = "DTW - Trasferimento tra Wallet";
        d[20] = "20230101095000_Kraken_001_001_PC";

        assertFalse(Funzioni.isDuplicabile(d));
        assertFalse(Funzioni.DuplicaMovimento(d[0]));
        assertEquals(1, MappaCryptoWallet.size());
    }

    @Test
    void unGenerato_nonSiDuplica() {
        String[] g = movimento("20230101100000_Binance_001A_001_DC");
        g[22] = "AU";

        assertFalse(Funzioni.isDuplicabile(g));
        assertFalse(Funzioni.DuplicaMovimento(g[0]));
    }

    @Test
    void ilDuplicato_tieneClassificazioneSingolaEDocumento_nonStoricoNeGruppi() {
        String[] v = movimento("20230101100000_Binance_001_001_DC");
        v[5] = "REWARD";
        v[18] = "DAI - Reward";
        v[41] = "3";
        v[MovimentiStorico.CAMPO_LIGNAGGIO] = "lignaggio-originale";
        v[CommissioniCollegate.CAMPO] = "commissione";
        GruppoOperazione.Scrivi(v, "DUAL-7");

        assertTrue(Funzioni.DuplicaMovimento(v[0]));

        String[] d = null;
        for (String[] m : MappaCryptoWallet.values()) if (m != v) d = m;
        assertNotNull(d);
        assertEquals("M", d[22]);
        assertEquals("DAI - Reward", d[18], "la classificazione di un movimento singolo resta");
        assertEquals("3", d[41], "il documento di origine resta");
        assertEquals("", d[MovimentiStorico.CAMPO_LIGNAGGIO], "storico delle modifiche suo");
        assertEquals("", CommissioniCollegate.Chiave(d));
        assertEquals("", GruppoOperazione.Chiave(d));
        assertEquals("lignaggio-originale", v[MovimentiStorico.CAMPO_LIGNAGGIO], "l'originale non cambia");
    }
}
