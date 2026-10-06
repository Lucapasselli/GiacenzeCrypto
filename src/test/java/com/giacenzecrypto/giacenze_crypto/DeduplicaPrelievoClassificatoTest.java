package com.giacenzecrypto.giacenze_crypto;

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
 * Fissa la deduplica dei reimport senza «sovrascrivi» su un prelievo classificato come trasferimento fra wallet.
 * La classificazione toglie al prelievo la commissione generata (o gli aggiunge la reward), il file riporta la
 * quantità originale: la chiave logica non coincideva più e la riga del file rientrava come doppione (15 prelievi
 * sull'archivio reale 2026). Ora conta anche la quantità che il prelievo aveva prima della classificazione, la
 * stessa che l'annullamento gli restituisce.
 */
class DeduplicaPrelievoClassificatoTest {

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
    }

    private static final String ID_P = "20240315101500_Binance_001_001_PC";
    private static final String ID_D = "20240315101530_0xWallet_001_001_DC";

    /** Una riga come la produce l'import del file, non classificata. */
    private static String[] riga(String ID, String exchange, String moneta, String qta) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2024-03-15 10:15";
        v[2] = "1 di 1";
        v[3] = exchange;
        v[4] = "Principale";
        boolean prelievo = ID.endsWith("_PC") || ID.endsWith("_PF");
        v[5] = prelievo ? "PRELIEVO CRYPTO" : "DEPOSITO CRYPTO";
        if (prelievo) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = "-" + qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        v[15] = "100.00";
        v[22] = "A";
        v[32] = "SI";
        Importazioni.RiempiVuotiArray(v);
        return v;
    }

    private static void archivio(String qtaPrelievo, String qtaDeposito) {
        MappaCryptoWallet.put(ID_P, riga(ID_P, "Binance", "USDC", qtaPrelievo));
        MappaCryptoWallet.put(ID_D, riga(ID_D, "0xWallet", "USDC", qtaDeposito));
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(ID_P, ID_D);
    }

    private static List<String[]> reimport(String[]... righe) {
        return Importazioni.F_ritornaSoloElementiNuovi(new java.util.ArrayList<>(List.of(righe)));
    }

    @Test
    void conCommissione_laRigaDelFileNonRientra() {
        archivio("10", "9.5");
        assertEquals(3, MappaCryptoWallet.size(), "prelievo, deposito e commissione generata");
        assertEquals("-9.5", MappaCryptoWallet.get(ID_P)[10], "la classificazione ha tolto la commissione");

        assertTrue(reimport(riga(ID_P, "Binance", "USDC", "10")).isEmpty());
    }

    @Test
    void conReward_laRigaDelFileNonRientra() {
        archivio("10", "10.005");
        assertEquals("-10.005", MappaCryptoWallet.get(ID_P)[10], "la classificazione ha aggiunto la reward");

        assertTrue(reimport(riga(ID_P, "Binance", "USDC", "10")).isEmpty());
    }

    @Test
    void unMovimentoDiversoNelloStessoSecondo_entraComunque() {
        archivio("10", "9.5");

        assertEquals(1, reimport(riga(ID_P, "Binance", "USDC", "3")).size());
        assertEquals(1, reimport(riga(ID_P, "Binance", "ETH", "10")).size(), "altra moneta");
    }

    @Test
    void laQuantitaOriginaleELaStessaCheRestituisceLAnnullamento() {
        archivio("10", "9.5");
        String[] p = MappaCryptoWallet.get(ID_P);
        String attesa = GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(p)[10];

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale((ID_P + "," + p[20]).split(","), ID_P);

        assertEquals(0, new java.math.BigDecimal(attesa).compareTo(new java.math.BigDecimal(p[10])));
        assertEquals(0, new java.math.BigDecimal("-10").compareTo(new java.math.BigDecimal(p[10])));
        assertEquals(2, MappaCryptoWallet.size(), "la commissione generata sparisce");
        assertNull(GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(p), "non più classificato");
    }

    @Test
    void trasferimentoSenzaDifferenza_nessunaSecondaChiave() {
        archivio("10", "10");

        assertNull(GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(MappaCryptoWallet.get(ID_P)));
        assertNull(GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(MappaCryptoWallet.get(ID_D)),
                "il deposito non è mai stato modificato");
        assertTrue(reimport(riga(ID_P, "Binance", "USDC", "10")).isEmpty(), "la chiave normale basta");
    }
}
