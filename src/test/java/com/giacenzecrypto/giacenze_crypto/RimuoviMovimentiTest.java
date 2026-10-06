package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link Funzioni#RimuoviMovimenti}: la rimozione di più movimenti segue le righe e non gli ID raccolti
 * all'inizio. Annullare uno scambio differito rinomina il deposito (toglie il prefisso {@code 04}): un ciclo sugli
 * ID non lo trovava più, e selezionando prelievo e deposito il deposito restava. Vale per la cancellazione dalla
 * tabella movimenti, per i movimenti dei token SCAM e per la cancellazione per wallet e date.
 */
class RimuoviMovimentiTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuota() throws Exception {
        MappaCryptoWallet.clear();
        MovimentiStorico.AzzeraBuffer();
        String sql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address) "
                + "KEY (timestamp, exchange, symbol, rete, address) VALUES (?, ?, ?, ?, '', '')";
        try (java.sql.PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (String c : new String[]{"BTC", "USDT"}) {
                ps.setLong(1, FunzioniDate.ConvertiDatainLongMinuto("2022-05-24 08:33:41"));
                ps.setString(2, "binance");
                ps.setString(3, c);
                ps.setBigDecimal(4, BigDecimal.ONE);
                ps.executeUpdate();
            }
        }
    }

    private static String[] movimento(String ID, String moneta, String qta, String data) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = ID.endsWith("_PC") ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        if (ID.endsWith("_PC")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    /** Uno scambio differito classificato: prelievo e deposito rinominati, tre movimenti generati. */
    private static String[][] scambioDifferito() {
        String[] p = movimento("20220512062450_Binance_001_001_PC", "USDT", "100", "2022-05-12 06:24:50");
        String[] s = movimento("20220524083341_Binance_002_001_DC", "BTC", "0.0035", "2022-05-24 08:33:41");
        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(p[0], s[0]));
        assertEquals(5, MappaCryptoWallet.size());
        return new String[][]{p, s};
    }

    @Test
    void prelievoEDepositoDiUnoScambioDifferito_vengonoRimossiEntrambi() {
        String[][] m = scambioDifferito();

        int rimossi = Funzioni.RimuoviMovimenti(List.of(m[0][0], m[1][0]));

        assertEquals(2, rimossi);
        assertTrue(MappaCryptoWallet.isEmpty(), "resta " + MappaCryptoWallet.keySet());
    }

    @Test
    void ancheNellOrdineInverso() {
        String[][] m = scambioDifferito();

        Funzioni.RimuoviMovimenti(List.of(m[1][0], m[0][0]));

        assertTrue(MappaCryptoWallet.isEmpty(), "resta " + MappaCryptoWallet.keySet());
    }

    @Test
    void unGeneratoSelezionatoInsiemeAlGruppo_nonSiContaDueVolte() {
        String[][] m = scambioDifferito();
        String generato = m[0][20].split(",")[1];

        int rimossi = Funzioni.RimuoviMovimenti(List.of(m[0][0], generato, m[1][0]));

        assertEquals(2, rimossi, "il generato sparisce col gruppo del prelievo e non si conta");
        assertTrue(MappaCryptoWallet.isEmpty());
    }

    @Test
    void unMovimentoIdenticoMaNonSelezionato_resta() {
        String[] a = movimento("20230101100000_Binance_001_001_PC", "BTC", "0.01", "2023-01-01 10:00:00");
        String[] b = movimento("20230101100000_Binance_001_002_PC", "BTC", "0.01", "2023-01-01 10:00:00");

        assertEquals(1, Funzioni.RimuoviMovimenti(List.of(a[0])));

        assertNull(MappaCryptoWallet.get(a[0]));
        assertSame(b, MappaCryptoWallet.get(b[0]));
    }

    @Test
    void ilLignaggioDelMovimentoRimosso_vaNelloStoricoDaCancellare() {
        String[] a = movimento("20230101100000_Binance_001_001_PC", "BTC", "0.01", "2023-01-01 10:00:00");
        MovimentiStorico.AssicuraLignaggio(a);
        assertEquals(0, MovimentiStorico.VociInAttesa());

        Funzioni.RimuoviMovimenti(List.of(a[0]));

        assertEquals(1, MovimentiStorico.VociInAttesa(), "la cancellazione del lignaggio è in attesa del salvataggio");
    }

    @Test
    void cancellazionePerWallet_rimuoveTuttoLoScambioEContaLeRigheUscite() {
        scambioDifferito();
        movimento("20220601100000_Kraken_009_001_PC", "BTC", "0.01", "2022-06-01 10:00:00");
        MappaCryptoWallet.get("20220601100000_Kraken_009_001_PC")[3] = "Kraken";

        int cancellati = Funzioni.CancellaMovimentazioniXWallet("Binance", 0, 0);

        assertEquals(5, cancellati, "prelievo, deposito e i tre generati, in un giro solo");
        assertEquals(1, MappaCryptoWallet.size(), "resta solo il movimento dell'altro wallet");
    }
}
