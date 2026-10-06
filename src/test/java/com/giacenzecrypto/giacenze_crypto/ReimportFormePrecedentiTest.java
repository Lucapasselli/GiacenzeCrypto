package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa il reimport di movimenti che in archivio non hanno più la forma del file ({@link FormeImportate}): ID in un
 * formato di una versione precedente, movimenti spostati con Trasla Orario o modificati a mano. Senza «sovrascrivi» la
 * riga del file non deve rientrare come doppione, con «sovrascrivi» deve prendere il posto del movimento (sovrascrive
 * sempre tutto, anche la correzione fatta a mano). Due righe identiche del file non si prendono lo stesso movimento.
 */
class ReimportFormePrecedentiTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        MovimentiStorico.AzzeraBuffer();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuota() {
        MappaCryptoWallet.clear();
        MovimentiStorico.AzzeraBuffer();
        Importazioni.AzzeraContatori();
    }

    /** Una riga come la produce l'import del file. */
    private static String[] riga(String ID, String moneta, String qta) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-15 10:15";
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        if (ID.endsWith("_PC") || ID.endsWith("_CM")) {
            v[5] = "PRELIEVO CRYPTO";
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[5] = ID.endsWith("_RW") ? "EARN" : "DEPOSITO CRYPTO";
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "1.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    private static String[] inArchivio(String ID, String moneta, String qta) {
        String[] v = riga(ID, moneta, qta);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static void sovrascrivi(String[]... righe) {
        Importazioni.ScriviListaSuMappaCrypto(new ArrayList<>(List.of(righe)), true);
    }

    private static List<String[]> nuove(String[]... righe) {
        return Importazioni.F_ritornaSoloElementiNuovi(new ArrayList<>(List.of(righe)));
    }

    @Test
    void sovrascrivendo_unIDDelFormatoPrecedenteSiRitrovaPerChiave() {
        String[] vecchio = inArchivio("20240315101500_Binance_1_1_RW", "BNB", "0.0000005");
        String[] file = riga("20240315101500_Binance_001_001_RW", "BNB", "0.0000005");

        sovrascrivi(file);

        assertEquals(1, MappaCryptoWallet.size(), MappaCryptoWallet.keySet().toString());
        assertSame(file, MappaCryptoWallet.get(file[0]));
        assertNull(MappaCryptoWallet.get(vecchio[0]));
    }

    @Test
    void sovrascrivendo_righeIdentiche_ognunaPrendeUnMovimentoSolo() {
        for (int i = 1; i <= 3; i++) inArchivio("20240315101500_Binance_" + i + "_1_DC", "USDC", "5");

        sovrascrivi(riga("20240315101500_Binance_001_001_DC", "USDC", "5"),
                riga("20240315101500_Binance_002_001_DC", "USDC", "5"),
                riga("20240315101500_Binance_003_001_DC", "USDC", "5"),
                riga("20240315101500_Binance_004_001_DC", "USDC", "5"));

        assertEquals(4, MappaCryptoWallet.size(), "tre sostituite, la quarta è nuova: " + MappaCryptoWallet.keySet());
        for (String k : MappaCryptoWallet.keySet()) assertTrue(k.contains("_00"), k);
    }

    @Test
    void sovrascrivendo_chiRitrovaIlProprioIDNonVienePortatoViaDaUnAltraRiga() {
        //Stessa chiave: X col formato di oggi, Y col formato precedente
        String[] x = inArchivio("20240315101500_Binance_001_001_DC", "USDC", "5");
        String[] y = inArchivio("20240315101500_Binance_2_1_DC", "USDC", "5");
        String[] fileZ = riga("20240315101500_Binance_002_001_DC", "USDC", "5");
        String[] fileX = riga(x[0], "USDC", "5");

        //La riga senza ID in archivio arriva per prima: non deve prendersi X, che ha la stessa chiave e viene prima
        assertSame(y, FormeImportate.Indice(List.of(fileZ, fileX)).Trova(fileZ));
        sovrascrivi(fileZ, fileX);

        assertEquals(2, MappaCryptoWallet.size(), MappaCryptoWallet.keySet().toString());
        assertSame(fileX, MappaCryptoWallet.get(x[0]));
        assertSame(fileZ, MappaCryptoWallet.get(fileZ[0]));
    }

    @Test
    void traslaOrario_senzaSovrascrivi_laRigaDelFileNonRientra() {
        String[] file = riga("20240316120000_Binance_001_001_DC", "ETH", "0.0065");
        MappaCryptoWallet.put(file[0], riga(file[0], "ETH", "0.0065"));
        assertEquals(1, Principale_TraslaOrario.EseguiTraslazione(List.of(file[0]), 3600_000L));
        assertNull(MappaCryptoWallet.get(file[0]), "spostato di un'ora, ha un altro ID");

        assertTrue(nuove(file).isEmpty());
    }

    @Test
    void traslaOrario_conSovrascrivi_laRigaDelFilePrendeIlSuoPosto() {
        String[] file = riga("20240316120000_Binance_001_001_DC", "ETH", "0.0065");
        MappaCryptoWallet.put(file[0], riga(file[0], "ETH", "0.0065"));
        Principale_TraslaOrario.EseguiTraslazione(List.of(file[0]), 3600_000L);

        sovrascrivi(file);

        assertEquals(1, MappaCryptoWallet.size(), "sovrascrive tutto, anche lo spostamento: " + MappaCryptoWallet.keySet());
        assertSame(file, MappaCryptoWallet.get(file[0]));
    }

    @Test
    void sovrascrivendoUnMovimentoConStorico_laRigaDelFileLoEredita() {
        String[] file = riga("20240316120000_Binance_001_001_DC", "ETH", "0.0065");
        MappaCryptoWallet.put(file[0], riga(file[0], "ETH", "0.0065"));
        Principale_TraslaOrario.EseguiTraslazione(List.of(file[0]), 3600_000L);
        String[] spostato = MappaCryptoWallet.values().iterator().next();
        String Lignaggio = spostato[MovimentiStorico.CAMPO_LIGNAGGIO];
        assertFalse(Funzioni.noData(Lignaggio), "Trasla Orario ha aperto lo storico");

        sovrascrivi(file);

        assertEquals(Lignaggio, file[MovimentiStorico.CAMPO_LIGNAGGIO], "la riga del file continua la stessa storia");
        List<String[]> Versioni = MovimentiStorico.Versioni(Lignaggio);
        assertEquals(2, Versioni.size(), "lo spostamento e la sovrascrittura");
        //Nel test le due voci possono cadere nello stesso millisecondo: si cerca per operazione, non per posizione
        String[] Ultima = Versioni.stream().filter(r -> MovimentiStorico.OP_SOVRASCRITTURA.equals(r[3])).findFirst().orElseThrow();
        assertEquals(spostato[0], Importazioni.DeserializzaRiga(Ultima[2])[0], "conserva il movimento spostato, scartato");
        assertEquals(file[0], Ultima[4]);
    }

    @Test
    void sovrascrivendoUnMovimentoMaiModificato_nessunoStorico() {
        String[] file = riga("20240315101500_Binance_001_001_RW", "BNB", "0.0000005");
        inArchivio("20240315101500_Binance_1_1_RW", "BNB", "0.0000005");
        inArchivio("20240315110000_Binance_001_001_RW", "ETH", "1");

        sovrascrivi(file, riga("20240315110000_Binance_001_001_RW", "ETH", "1"));

        assertEquals("", file[MovimentiStorico.CAMPO_LIGNAGGIO]);
        assertEquals(0, MovimentiStorico.VociInAttesa());
    }

    @Test
    void quantitaModificataAMano_senzaSovrascrivi_laRigaDelFileNonRientra() {
        String[] v = inArchivio("20240316120000_Binance_001_001_DC", "ETH", "0.0065");
        //Come GUI_ModificaMovimento: lignaggio, versione precedente nello storico, poi la modifica sul posto
        String Lignaggio = MovimentiStorico.AssicuraLignaggio(v);
        MovimentiStorico.AccodaModifica(Lignaggio, v[0], v[0], Importazioni.SerializzaRiga(v), MovimentiStorico.OP_IN_PLACE);
        v[13] = "0.0066";

        assertTrue(nuove(riga(v[0], "ETH", "0.0065")).isEmpty());
        assertEquals(1, nuove(riga(v[0], "ETH", "0.5")).size(), "un movimento diverso entra");
    }

    @Test
    void sovrascrivendo_unaRigaNonSiPrendeUnMovimentoGenerato() {
        String[] p = inArchivio("20240315101500_Binance_001_001_PC", "USDC", "10");
        String[] d = inArchivio("20240315110000_Wallet_001_001_DC", "USDC", "9.5");
        d[3] = "0xWallet";
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], d[0]);
        assertEquals(3, MappaCryptoWallet.size());
        //Una riga di commissione del file, con l'istante e la quantità di quella generata
        String[] fee = riga("20240315101500_Binance_009_001_CM", "USDC", "0.5");

        sovrascrivi(fee);

        assertEquals(4, MappaCryptoWallet.size(), "la commissione generata resta con il suo trasferimento");
        assertEquals("PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)", p[18]);
    }
}
