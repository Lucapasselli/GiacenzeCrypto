package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GUI_ClassificazioneMovimento#CreaMovimentiTrasferimentoAVaultNonPresidiati}: un prelievo di un token LP verso una
 * piattaforma DeFi e i suoi rientri (stesso contratto, stesso token), classificati insieme. Dati inventati.
 *
 * <p>Il blocco che classifica i rientri stava <b>dentro</b> il ciclo che li cerca: a ogni elemento della lista dei movimenti
 * da classificare (non solo a ogni rientro trovato) rifaceva tutta la classificazione di quelli già raccolti, e ogni
 * classificazione scorre l'intera mappa per sommare il saldo della piattaforma. Il risultato era lo stesso, il costo
 * proporzionale a (elementi della lista) x (rientri). Il test fissa il risultato e conta le scansioni della mappa.</p>
 */
class VaultRientriClassificazioneTest {

    @TempDir
    static Path tempDir;

    /** La mappa dei movimenti che conta quante volte viene scorsa per intero ({@code values()}). */
    static final class MappaContaScansioni extends TreeMap<String, String[]> {

        int scansioni;

        MappaContaScansioni() {
            super(String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        public Collection<String[]> values() {
            scansioni++;
            return super.values();
        }
    }

    private TreeMap<String, String[]> mappaOriginale;
    private List<String> listaOriginale;

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
    void ricordaLoStatoStatico() {
        mappaOriginale = Principale.MappaCryptoWallet;
        listaOriginale = Principale.DepositiPrelieviDaCategorizzare;
    }

    @AfterEach
    void rimettiLoStatoStatico() {
        Principale.MappaCryptoWallet = mappaOriginale;
        Principale.DepositiPrelieviDaCategorizzare = listaOriginale;
    }

    private static final String CONTRATTO = "0xcontrattoprova";

    private static String[] movimento(String ID, String data, String moneta, String qta, String contratto) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = data;
        v[2] = "1 di 1";
        v[3] = "WalletProva (BSC)";
        v[4] = "Wallet";
        boolean uscita = qta.startsWith("-");
        v[5] = uscita ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        if (uscita) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "30.00";
        v[22] = "A";
        v[23] = "100";
        v[24] = "0xhash" + ID.hashCode();
        v[29] = "1741600000000";
        v[30] = contratto;
        v[32] = "SI";
        v[34] = "BSC";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    /** Un prelievo di 10 LP e {@code rientri} rientri da 3, ciascuno al suo minuto, più {@code altri} movimenti da classificare. */
    private MappaContaScansioni preparaScenario(int rientri, int altri) {
        MappaContaScansioni mappa = new MappaContaScansioni();
        List<String> lista = new ArrayList<>();
        String idPrelievo = "20250310100000_WalletProva_001_001_PC";
        mappa.put(idPrelievo, movimento(idPrelievo, "2025-03-10 10:00", "LPTOK-LP", "-10", CONTRATTO));
        lista.add(idPrelievo);
        for (int k = 1; k <= rientri; k++) {
            String id = String.format("20250310%02d0000_WalletProva_001_001_DC", 10 + k);
            mappa.put(id, movimento(id, String.format("2025-03-10 %d:00", 10 + k), "LPTOK-LP", "3", CONTRATTO));
            lista.add(id);
        }
        //Movimenti non classificati di un altro contratto: stanno nella lista ma non c'entrano
        for (int k = 0; k < altri; k++) {
            String id = String.format("2025031114%04d_AltroWallet_001_001_DC", k);
            String[] v = movimento(id, "2025-03-11 14:00", "ALTRO", "1", "0xaltrocontratto");
            v[3] = "AltroWallet (BSC)";
            mappa.put(id, v);
            lista.add(id);
        }
        //Archivio già classificato: la scansione della mappa costa in proporzione
        for (int k = 0; k < 3000; k++) {
            String id = String.format("2024%010d_Archivio_001_001_DC", k);
            String[] v = movimento(id, "2024-01-01 10:00", "ARCH", "1", "");
            v[3] = "Archivio";
            v[18] = "DAI - qualcosa";
            mappa.put(id, v);
        }
        Principale.MappaCryptoWallet = mappa;
        Principale.DepositiPrelieviDaCategorizzare = lista;
        return mappa;
    }

    @Test
    void prelievoLPeRientri_siClassificanoTuttiEDuplicatiNonNascono() {
        MappaContaScansioni mappa = preparaScenario(4, 30);
        String idPrelievo = "20250310100000_WalletProva_001_001_PC";

        int modificati = GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentoAVaultNonPresidiati(idPrelievo);

        assertEquals(5, modificati, "il prelievo e i 4 rientri");
        assertTrue(mappa.get(idPrelievo)[18].startsWith("PTW"));
        for (int k = 1; k <= 4; k++) {
            String id = String.format("20250310%02d0000_WalletProva_001_001_DC", 10 + k);
            assertTrue(mappa.get(id)[18].startsWith("DTW"), id);
        }
        //Righe generate: 1 deposito sulla piattaforma per il prelievo, 1 prelievo dalla piattaforma per ogni rientro, e la reward
        //che compensa il rientro che porta il saldo della piattaforma sotto zero (il quarto: 10 - 3 - 3 - 3 - 3 = -2)
        long generate = mappa.values().stream().filter(v -> "AU".equals(v[22])).count();
        assertEquals(1 + 4 + 1, generate, "righe generate: nessun doppione");
        //Una classificazione per rientro (e una per il prelievo): ognuna scorre la mappa una volta per sommare il saldo della piattaforma
        assertTrue(mappa.scansioni <= 3 * 5, "scansioni della mappa: " + mappa.scansioni);
    }

    @Test
    void lePassateSullaMappa_nonDipendonoDaQuantiElementiHaLaLista() {
        MappaContaScansioni pochi = preparaScenario(4, 5);
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentoAVaultNonPresidiati("20250310100000_WalletProva_001_001_PC");
        int conPochi = pochi.scansioni;

        MappaContaScansioni molti = preparaScenario(4, 200);
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentoAVaultNonPresidiati("20250310100000_WalletProva_001_001_PC");
        int conMolti = molti.scansioni;

        System.out.println("Scansioni: " + conPochi + " con 5 altri movimenti in lista, " + conMolti + " con 200");
        assertEquals(conPochi, conMolti, "classificare i rientri costa quanto i rientri, non quanto la lista");
    }
}
