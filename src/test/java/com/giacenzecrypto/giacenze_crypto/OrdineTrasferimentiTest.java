package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OrdineTrasferimenti}: un trasferimento fra wallet con prelievo e deposito nello stesso secondo ha il
 * deposito prima del prelievo se il nome del suo wallet ordina prima. Dati inventati.
 *
 * <p>Fissa: la rinomina avviene dopo l'abbinamento e non durante (i cicli di classificazione automatica scorrono
 * ID raccolti prima), che il marcatore è un {@code ~} sul segmento 1, che la guardia rinuncia quando un movimento
 * dello stesso wallet e token seguirebbe il deposito, e che l'annullamento della classificazione restituisce l'ID.</p>
 */
class OrdineTrasferimentiTest {

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
    void svuota() {
        MappaCryptoWallet.clear();
        //Scarica le coppie annotate da un test precedente: sulla mappa vuota nessuna riga coincide e vengono ignorate
        OrdineTrasferimenti.Riordina(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    private static final String SECONDO = "20250314101530";

    /** {@code qta} negativa = uscita, positiva = entrata. */
    private static String[] movimento(String ID, String exchange, String moneta, String qta) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2025-03-14 10:15";
        v[2] = "1 di 1";
        v[3] = exchange;
        v[4] = "Principale";
        if (qta.startsWith("-")) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    private static String[] prelievo(String exchange) {
        return movimento(SECONDO + "_" + exchange + "_001_001_PC", exchange, "BTC", "-1");
    }

    private static String[] deposito(String exchange) {
        return movimento(SECONDO + "_" + exchange + "_001_001_DC", exchange, "BTC", "1");
    }

    /** Mette i due movimenti in mappa e li abbina come fa la classificazione. */
    private static void abbina(String[] p, String[] d) {
        MappaCryptoWallet.put(p[0], p);
        MappaCryptoWallet.put(d[0], d);
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);
    }

    @Test
    void depositoPrimaDelPrelievo_vieneSpostatoDopo_soloAlRiordino() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        String idVecchio = d[0];
        assertTrue(String.CASE_INSENSITIVE_ORDER.compare(d[0], p[0]) < 0, "il caso di partenza è invertito");

        abbina(p, d);
        //Durante l'abbinamento l'ID non cambia: un ciclo che tiene gli ID raccolti prima deve trovare ancora il movimento
        assertSame(d, MappaCryptoWallet.get(idVecchio));

        assertEquals(1, OrdineTrasferimenti.Riordina(MappaCryptoWallet));

        assertEquals(SECONDO + "_~Binance_001_001_DC", d[0]);
        assertNull(MappaCryptoWallet.get(idVecchio));
        assertSame(d, MappaCryptoWallet.get(d[0]));
        assertEquals(p[0], MappaCryptoWallet.firstKey(), "il prelievo precede il deposito");
        assertEquals(d[0], MappaCryptoWallet.lastKey());
        //Il riferimento del prelievo punta al nuovo ID, e quello del deposito al prelievo
        assertEquals(d[0], p[20]);
        assertEquals(p[0], d[20]);
        assertEquals("Binance", d[3], "il wallet vero resta in [3]");
    }

    @Test
    void ordineGiaGiusto_nonSiTocca() {
        String[] p = prelievo("Binance");
        String[] d = deposito("Ledger");
        abbina(p, d);

        assertEquals(0, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
        assertEquals(SECONDO + "_Ledger_001_001_DC", d[0]);
    }

    @Test
    void secondiDiversi_nonSiTocca() {
        String[] p = prelievo("Ledger");
        String[] d = movimento("20250314101529_Binance_001_001_DC", "Binance", "BTC", "1");
        abbina(p, d);

        assertEquals(0, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
        assertEquals("20250314101529_Binance_001_001_DC", d[0]);
    }

    @Test
    void guardia_usciteDelloStessoTokenSulWalletDelDeposito_impediscono() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        //Vende sul wallet che riceve, nello stesso secondo, subito dopo il deposito: spostarlo lo precederebbe
        String[] vendita = movimento(SECONDO + "_Binance_001_002_PC", "Binance", "BTC", "-0.5");
        MappaCryptoWallet.put(vendita[0], vendita);
        abbina(p, d);

        assertEquals(0, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
        assertEquals(SECONDO + "_Binance_001_001_DC", d[0], "resta invertito, senza errori");
    }

    @Test
    void guardia_altroTokenOEntrata_nonImpediscono() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        MappaCryptoWallet.put(SECONDO + "_Binance_001_002_PC",
                movimento(SECONDO + "_Binance_001_002_PC", "Binance", "ETH", "-3"));
        MappaCryptoWallet.put(SECONDO + "_Binance_001_003_DC",
                movimento(SECONDO + "_Binance_001_003_DC", "Binance", "BTC", "2"));
        //Un altro wallet con lo stesso token non c'entra
        MappaCryptoWallet.put(SECONDO + "_Kraken_001_001_PC",
                movimento(SECONDO + "_Kraken_001_001_PC", "Kraken", "BTC", "-4"));
        abbina(p, d);

        assertEquals(1, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
        assertEquals(SECONDO + "_~Binance_001_001_DC", d[0]);
    }

    @Test
    void rigaSostituitaFraAbbinamentoERiordino_siIgnora() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        abbina(p, d);
        //Un'altra riga con la stessa chiave (per esempio una riga ricaricata): la coppia annotata non vale più
        MappaCryptoWallet.put(d[0], d.clone());

        assertEquals(0, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
    }

    @Test
    void idMarcatoGiaPresente_siIncrementaIlQuartoSegmento() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        String[] occupante = movimento(SECONDO + "_~Binance_001_001_DC", "Binance", "ETH", "7");
        MappaCryptoWallet.put(occupante[0], occupante);
        abbina(p, d);

        assertEquals(1, OrdineTrasferimenti.Riordina(MappaCryptoWallet));
        assertEquals(SECONDO + "_~Binance_001_002_DC", d[0]);
        assertSame(occupante, MappaCryptoWallet.get(occupante[0]), "l'altro movimento non viene toccato");
    }

    @Test
    void commissioneGenerata_seguePrelievoEDepositoRinominato() {
        String[] p = movimento(SECONDO + "_Ledger_001_001_PC", "Ledger", "BTC", "-1.001");
        String[] d = deposito("Binance");
        abbina(p, d);
        String idCommissione = SECONDO + "_Ledger_2_1_CM";
        assertNotNull(MappaCryptoWallet.get(idCommissione), "la commissione nasce dall'abbinamento");

        assertEquals(1, OrdineTrasferimenti.Riordina(MappaCryptoWallet));

        assertTrue(MappaCryptoWallet.get(idCommissione)[20].contains(d[0]), "la commissione cita il nuovo ID");
        assertFalse(MappaCryptoWallet.get(idCommissione)[20].contains(SECONDO + "_Binance_"));
        assertTrue(p[20].contains(d[0]));
    }

    @Test
    void annullamentoDellaClassificazione_restituisceLIDDiPartenza() {
        String[] p = prelievo("Ledger");
        String[] d = deposito("Binance");
        String idVecchio = d[0];
        abbina(p, d);
        OrdineTrasferimenti.Riordina(MappaCryptoWallet);
        String idNuovo = d[0];
        assertNotEquals(idVecchio, idNuovo);

        String ritorno = GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(
                new String[]{p[0], idNuovo}, idNuovo);

        assertEquals(idVecchio, ritorno);
        assertSame(d, MappaCryptoWallet.get(idVecchio));
        assertNull(MappaCryptoWallet.get(idNuovo));
        assertEquals("", d[20]);
        assertEquals("", d[18]);
    }

    @Test
    void marcatore_siRiconoscePerIlSoloSegmentoWallet() {
        assertTrue(OrdineTrasferimenti.HaMarcatore(SECONDO + "_~Binance_001_001_DC"));
        assertEquals(SECONDO + "_Binance_001_001_DC", OrdineTrasferimenti.IDSenzaMarcatore(SECONDO + "_~Binance_001_001_DC"));
        //La rettifica di giacenza usa "~" come suffisso del wallet, non è un deposito spostato
        assertFalse(OrdineTrasferimenti.HaMarcatore(SECONDO + "_Binance~_001_001_DF"));
        assertNull(OrdineTrasferimenti.IDSenzaMarcatore(SECONDO + "_Binance~_001_001_DF"));
        assertNull(OrdineTrasferimenti.IDSenzaMarcatore(SECONDO + "_Binance_~001_001_DC"));
        assertNull(OrdineTrasferimenti.IDSenzaMarcatore(null));
        assertNull(OrdineTrasferimenti.IDSenzaMarcatore("non un ID"));
    }

    @Test
    void normalizzaNome_toglieSoloIlTildeIniziale() {
        assertEquals("Binance", MovimentiCrypto.normalizzaNome("~Binance"));
        assertEquals("Binance", MovimentiCrypto.normalizzaNome("  ~~Binance "));
        assertEquals("Binance", MovimentiCrypto.normalizzaNome("_~Binance"), "dopo aver tolto gli underscore");
        assertEquals("Bin~ance", MovimentiCrypto.normalizzaNome("Bin~ance"));
        assertEquals("Binance~", MovimentiCrypto.normalizzaNome("Binance~"));
    }

    @Test
    void creaMovimento_conWalletConTildeIniziale_nonProduceUnIDMarcato() {
        Moneta m = new Moneta();
        m.Moneta = "BTC";
        m.Tipo = "Crypto";
        m.Qta = "1";
        String[] mov = MovimentiCrypto.creaMovimento(null, m, "~Binance", "", 1710495000000L,
                "100.00", null, 1, 1, null, "", "M", "", null, null);

        assertNotNull(mov);
        assertFalse(OrdineTrasferimenti.HaMarcatore(mov[0]), mov[0]);
        assertEquals("Binance", mov[0].split("_")[1]);
    }
}
