package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * "Ricalcola Prezzi": quali movimenti della selezione sono ricalcolabili e, soprattutto, quali righe di
 * {@code PrezziKO} vengono cancellate — solo i token coinvolti agli istanti dei movimenti selezionati —
 * e rimesse identiche se l'utente annulla. La ricerca dei prezzi in rete non si esercita qui (serve Node).
 */
class Principale_Movimenti_RicalcolaPrezziTest {

    @TempDir
    static Path tempDir;

    private static final String ID_A = "20240101120000_1_Kraken_1_DC";
    private static final String ID_DICHIARATO = "20240101130000_1_Kraken_1_DC";
    private static final String ID_ALTRO = "20240201120000_1_Kraken_1_DC";
    private static final String ADDRESS = "0x1111111111111111111111111111111111111111";

    private static long istante(String[] v) {
        return FunzioniDate.ConvertiDatainLongMinuto(v[1]);
    }

    private static String[] riga(String id, String data, String moneta, String moneta2) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[8] = moneta;
        v[9] = "Crypto";
        v[11] = moneta2;
        v[12] = moneta2.isBlank() ? "" : "Crypto";
        v[15] = "0.00";
        v[32] = "NO";
        return v;
    }

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void preparaDati() throws Exception {
        Principale.MappaCryptoWallet.clear();
        try (var st = DatabaseH2.connectionPrezzi.createStatement()) {
            st.execute("DELETE FROM PrezziKO");
        }
        Principale.MappaCryptoWallet.put(ID_A, riga(ID_A, "2024-01-01 12:00", "ABC", "XYZ"));
        String[] dichiarato = riga(ID_DICHIARATO, "2024-01-01 13:00", "ABC", "");
        dichiarato[14] = "10.00";
        Principale.MappaCryptoWallet.put(ID_DICHIARATO, dichiarato);
        Principale.MappaCryptoWallet.put(ID_ALTRO, riga(ID_ALTRO, "2024-02-01 12:00", "ABC", ""));
    }

    @AfterEach
    void pulisce() {
        Principale.MappaCryptoWallet.clear();
    }

    private static void scriviKO(String simbolo, long ts, String rete, String address) throws Exception {
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(
                "INSERT INTO PrezziKO (symbol, timestamp, rete, address) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, simbolo);
            ps.setLong(2, ts);
            ps.setString(3, rete);
            ps.setString(4, address);
            ps.executeUpdate();
        }
    }

    private static int contaKO() throws Exception {
        try (var st = DatabaseH2.connectionPrezzi.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM PrezziKO")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void sonoRicalcolabiliSoloIMovimentiSenzaControvaloreDichiarato() {
        assertTrue(Principale_Movimenti_RicalcolaPrezzi.isRicalcolabile(List.of(ID_A)));
        assertTrue(Principale_Movimenti_RicalcolaPrezzi.isRicalcolabile(List.of(ID_DICHIARATO, ID_A)));
        assertFalse(Principale_Movimenti_RicalcolaPrezzi.isRicalcolabile(List.of(ID_DICHIARATO)));
        assertFalse(Principale_Movimenti_RicalcolaPrezzi.isRicalcolabile(List.of("IDinesistente")));
        assertFalse(Principale_Movimenti_RicalcolaPrezzi.isRicalcolabile(List.of()));
        assertEquals(1, Principale_Movimenti_RicalcolaPrezzi.MovimentiRicalcolabili(List.of(ID_A, ID_A, ID_DICHIARATO)).size(),
                "un ID ripetuto conta una volta, e quello con [14] dichiarato non conta");
    }

    @Test
    void siTrovanoSoloIKODeiTokenCoinvoltiAgliIstantiDelMovimento() throws Exception {
        long t = istante(Principale.MappaCryptoWallet.get(ID_A));
        scriviKO("ABC", t, "", "");            //token e istante del movimento: coinvolta
        scriviKO("XYZ", t, "", "");            //seconda moneta: coinvolta
        scriviKO("ABC", t + 60000, "", "");    //stesso token, altro istante: NON coinvolta
        scriviKO("QQQ", t, "", "");            //altro token, stesso istante: NON coinvolta

        var trovate = Principale_Movimenti_RicalcolaPrezzi.TrovaKO(
                Principale_Movimenti_RicalcolaPrezzi.MovimentiRicalcolabili(List.of(ID_A)));

        assertEquals(2, trovate.size());
        assertTrue(trovate.stream().allMatch(r -> r.timestamp() == t));
        assertEquals(4, contaKO(), "TrovaKO non deve cancellare nulla");
    }

    @Test
    void siTrovaAnchePerAddressEIlSimboloVuotoNonCatturaGliAltri() throws Exception {
        String[] v = Principale.MappaCryptoWallet.get(ID_A);
        v[26] = ADDRESS;
        v[34] = "ETH";
        long t = istante(v);
        scriviKO("", t, "ETH", ADDRESS.toUpperCase().replace("0X", "0x"));  //riga di CambioAddressEUR
        scriviKO("", t, "ETH", "0x2222222222222222222222222222222222222222"); //altro token
        scriviKO("ABC", t + 1000, "", "");

        var trovate = Principale_Movimenti_RicalcolaPrezzi.TrovaKO(java.util.Collections.singletonList(v));

        assertEquals(1, trovate.size());
        assertEquals("", trovate.get(0).symbol());
        assertEquals("ETH", trovate.get(0).rete());
    }

    @Test
    void ilSimboloNormalizzatoEQuelloDiUnaltraVarianteSonoCoperti() throws Exception {
        Principale.Mappa_MoneteStessoPrezzo.put("WABC", "ABC");
        try {
            String[] v = riga("20240301120000_1_Kraken_1_DC", "2024-03-01 12:00", "WABC", "");
            long t = istante(v);
            scriviKO("ABC", t, "", "");   //CambioXXXEUR scrive il simbolo normalizzato
            var trovate = Principale_Movimenti_RicalcolaPrezzi.TrovaKO(java.util.Collections.singletonList(v));
            assertEquals(1, trovate.size());
        } finally {
            Principale.Mappa_MoneteStessoPrezzo.remove("WABC");
        }
    }

    @Test
    void eliminaERipristinaRimettonoLeStesseRighe() throws Exception {
        long t = istante(Principale.MappaCryptoWallet.get(ID_A));
        scriviKO("ABC", t, "", "");
        scriviKO("XYZ", t, "", "");
        scriviKO("QQQ", t, "", "");
        var coinvolte = Principale_Movimenti_RicalcolaPrezzi.TrovaKO(
                Principale_Movimenti_RicalcolaPrezzi.MovimentiRicalcolabili(List.of(ID_A)));

        assertEquals(2, Principale_Movimenti_RicalcolaPrezzi.EliminaKO(coinvolte));
        assertEquals(1, contaKO(), "resta solo il KO del token non coinvolto");
        assertFalse(Prezzi.PrezzoIrrecuperabileDaDB_Leggi("ABC", t, "", ""));

        Principale_Movimenti_RicalcolaPrezzi.RipristinaKO(coinvolte);
        assertEquals(3, contaKO());
        assertTrue(Prezzi.PrezzoIrrecuperabileDaDB_Leggi("ABC", t, "", ""));

        //Ripristinare due volte, o dopo che il ciclo ha già riscritto la riga, non deve fallire
        Principale_Movimenti_RicalcolaPrezzi.RipristinaKO(coinvolte);
        assertEquals(3, contaKO());
    }
}
