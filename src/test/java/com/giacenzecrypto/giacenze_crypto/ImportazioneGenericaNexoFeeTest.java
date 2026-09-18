package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa il trattamento della fee sugli 'Exchange' nel formato Nexo a 11 colonne
 * ({@code config/import/Nexo CSV (11 colonne).json}).
 *
 * <p>Le commissioni sulle cripto-attività non sono deducibili (art. 68 c.9-bis TUIR): non si può
 * scalarle dal costo di carico dello scambio, va emesso un movimento {@code COMMISSIONI} separato.
 * Verificato confrontando due righe reali con i prezzi storici Binance al secondo della
 * transazione (vedi {@code nocommit/Documentazione/StoricoModifiche.md}, 2026-09-18/19): 'Input
 * Amount' è già il totale addebitato sul wallet, di cui solo (Input − Fee) confluisce davvero nello
 * scambio — da cui {@code ricostruisciLordoSeFeeSuMonetaUscita: true} nella config, che scorpora la
 * fee dalla gamba in uscita invece di sommarla.</p>
 */
class ImportazioneGenericaNexoFeeTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static ImportazioneGenerica.ConfigurazioneImport cfg() throws Exception {
        return ImportazioneGenerica.ConfigurazioneImport.carica("config/import/Nexo CSV (11 colonne).json");
    }

    private static String[] rigaCon(String monetaIn, String qtaIn, String monetaOut, String qtaOut,
            String fee, String monetaFee, String data) {
        return new String[]{
            "NXTtest", "Exchange", monetaIn, qtaIn, monetaOut, qtaOut,
            "$0", fee, monetaFee, "approved / Exchange test", data
        };
    }

    /** Quantità di {@code moneta} nel movimento la cui categoria (ultimo campo dell'ID) è {@code categoria}. */
    private static BigDecimal quantitaCategoria(List<String[]> movs, String categoria, String moneta) {
        for (String[] m : movs) {
            if (!m[0].split("_")[4].equals(categoria)) continue;
            if (moneta.equals(m[8])) return new BigDecimal(m[10]);
            if (moneta.equals(m[11])) return new BigDecimal(m[13]);
        }
        fail("nessun movimento di categoria " + categoria + " con moneta " + moneta + " tra: "
                + movs.stream().map(m -> m[0]).toList());
        return null;
    }

    @Test
    void exchangeConFeeReale_NEXO_produceScambioNettoPiuCommissioneSeparata() throws Exception {
        // Riga reale (2025-12-10): Input -10 NEXO, fee 0.99685741 NEXO, Output 0.00009444 BTC.
        String[] riga = rigaCon("NEXO", "-10", "BTC", "0.00009444",
                "0.99685741", "NEXO", "2025-12-10 7:36:55");

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(riga, null, cfg());
        assertNotNull(movs);
        assertEquals(2, movs.size(), "scambio + commissione: " + movs.stream().map(m -> m[0]).toList());

        BigDecimal nexoScambio = quantitaCategoria(movs, "SC", "NEXO");
        BigDecimal nexoCommissione = quantitaCategoria(movs, "CM", "NEXO");
        BigDecimal btcScambio = quantitaCategoria(movs, "SC", "BTC");

        assertEquals(0, new BigDecimal("-9.00314259").compareTo(nexoScambio),
                "la gamba di scambio deve essere l'Input al netto della fee");
        assertEquals(0, new BigDecimal("-0.99685741").compareTo(nexoCommissione),
                "la commissione deve avere l'esatta quantità della colonna Fee");
        assertEquals(0, new BigDecimal("0.00009444").compareTo(btcScambio));

        // Invariante fiscale: le due gambe NEXO devono sommare esattamente all'Input Amount
        // originale, altrimenti si perde o duplica cripto nelle rimanenze.
        assertEquals(0, new BigDecimal("-10").compareTo(nexoScambio.add(nexoCommissione)),
                "scambio + commissione deve tornare all'Input Amount originale (-10), non di più né di meno");
    }

    @Test
    void exchangeConFeeReale_LTC_produceScambioNettoPiuCommissioneSeparata() throws Exception {
        // Riga reale (2025-02-05): Input -1.03330512 LTC, fee 0.01860711 LTC, Output 0.00107905 BTC.
        String[] riga = rigaCon("LTC", "-1.03330512", "BTC", "0.00107905",
                "0.01860711", "LTC", "2025-02-05 15:05:33");

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(riga, null, cfg());
        assertNotNull(movs);
        assertEquals(2, movs.size());

        BigDecimal ltcScambio = quantitaCategoria(movs, "SC", "LTC");
        BigDecimal ltcCommissione = quantitaCategoria(movs, "CM", "LTC");

        assertEquals(0, new BigDecimal("-1.01469801").compareTo(ltcScambio));
        assertEquals(0, new BigDecimal("-0.01860711").compareTo(ltcCommissione));
        assertEquals(0, new BigDecimal("-1.03330512").compareTo(ltcScambio.add(ltcCommissione)),
                "scambio + commissione deve tornare all'Input Amount originale");
    }

    @Test
    void exchangeSenzaFeeReale_restaUnSoloMovimento() throws Exception {
        // Fee "-" (placeholder Nexo per "nessuna commissione"): comportamento invariato, una sola
        // riga di scambio, nessuna commissione emessa.
        String[] riga = rigaCon("USDT", "-1.8", "BTC", "0.00001806",
                "-", "-", "2025-06-01 10:00:00");

        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(riga, null, cfg());
        assertNotNull(movs);
        assertEquals(1, movs.size(), "senza una fee reale non deve comparire nessuna riga COMMISSIONI");
        assertEquals("SC", movs.get(0)[0].split("_")[4]);
    }
}
