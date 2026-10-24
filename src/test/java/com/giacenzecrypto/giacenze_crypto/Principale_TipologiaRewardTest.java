package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Le scelte di "Cambia Tipologia Reward" ({@link Principale_TipologiaReward}). <i>Deposito da classificare</i>: un
 * provento importato torna deposito crypto non classificato, con tutto il resto del movimento invariato. La tipologia
 * del provento: solo sui proventi, su tutta la selezione, senza perdere il prefisso dei rimborsi.
 *
 * <p>Il caso è quello della segnalazione: il BTTC che Binance registra come "Airdrop Assets" tre ore dopo
 * l'uscita del BTT, e che la finestra dello scambio differito non proponeva perché era un RW.
 */
class Principale_TipologiaRewardTest {

    private static final String ID_BTTC = "20220124120000_Binance_001_001_RW";
    private static final String ID_BTT = "20220124100000_Binance_001_001_PC";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        //Solo per l'ultimo test: lo scambio differito cerca il prezzo della moneta ricevuta, che qui e' un
        //prezzo personalizzato (nessuna chiamata di rete)
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        long Deposito = FunzioniDate.ConvertiDatainLongMinuto("2022-01-24 12:00");
        assertTrue(DatabaseH2.InserisciPrezzoPresonalizzato(Deposito, "Personalizzato (TUTTI)",
                "BTTC", "0.000005", "", "", "TUTTI", Deposito));
    }

    @AfterAll
    static void chiude() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
        MovimentiStorico.AzzeraBuffer();
    }

    private static String[] movimento(String ID, String Campo5, String MonetaU, String TipoU, String QtaU,
            String MonetaE, String TipoE, String QtaE) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = "2022-01-24 12:00";
        v[2] = "1 di 1";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = Campo5;
        v[7] = "Airdrop Assets";
        v[8] = MonetaU;
        v[9] = TipoU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = TipoE;
        v[13] = QtaE;
        v[15] = "5.00";
        v[16] = "5.00";
        v[17] = "5.00";
        v[19] = "5.00";
        v[22] = "A";
        v[32] = "SI";
        v[40] = "|||binance";
        v[41] = "7";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    private static String[] rewardBTTC() {
        return movimento(ID_BTTC, "REWARD", "", "", "", "BTTC", "Crypto", "1000000");
    }

    @Test
    void ilProventoDiventaUnDepositoDaClassificare() {
        String Prima[] = rewardBTTC().clone();

        Map<String, String> Scartati = Principale_TipologiaReward.ConvertiInDepositi(List.of(ID_BTTC));

        assertTrue(Scartati.isEmpty(), Scartati.toString());
        assertNull(MappaCryptoWallet.get(ID_BTTC), "il vecchio ID non deve restare");
        String Nuovo[] = MappaCryptoWallet.get("20220124120000_Binance_001_001_DC");
        assertNotNull(Nuovo, "cambia solo la categoria in coda all'ID");
        assertEquals("DEPOSITO CRYPTO", Nuovo[5]);
        assertEquals("", Nuovo[18]);
        for (int Campo : new int[]{16, 17, 19, 31, 33, 38}) {
            assertEquals("", Nuovo[Campo], "campo del motore " + Campo + " da ricalcolare");
        }
        //Tutto il resto resta: prezzo, causale originale, documento di origine, quantità
        for (int Campo : new int[]{1, 3, 4, 7, 11, 12, 13, 15, 22, 32, 40, 41}) {
            assertEquals(Prima[Campo], Nuovo[Campo], "campo " + Campo);
        }
        //Ed è un deposito come quelli importati: classificabile, anche come scambio differito
        assertTrue(Funzioni.isDepositoPrelievoClassificabile(null, Nuovo, false));
        assertTrue(Importazioni.EDepositoDaClassificare(Nuovo));
    }

    @Test
    void laModificaFiniscenelloStoricoConIlMovimentoDiPrima() {
        rewardBTTC();
        Principale_TipologiaReward.ConvertiInDepositi(List.of(ID_BTTC));

        String Nuovo[] = MappaCryptoWallet.get("20220124120000_Binance_001_001_DC");
        assertFalse(Funzioni.noData(Nuovo[MovimentiStorico.CAMPO_LIGNAGGIO]), "il movimento riceve un lignaggio");
        assertEquals(1, MovimentiStorico.VociInAttesa());
    }

    @Test
    void unIDGiaOccupatoVieneResoUnivoco() {
        rewardBTTC();
        movimento("20220124120000_Binance_001_001_DC", "DEPOSITO CRYPTO", "", "", "", "BTT", "Crypto", "5");

        assertTrue(Principale_TipologiaReward.ConvertiInDepositi(List.of(ID_BTTC)).isEmpty());
        assertEquals("BTTC", MappaCryptoWallet.get("20220124120000_Binance_001_002_DC")[11]);
        assertEquals("BTT", MappaCryptoWallet.get("20220124120000_Binance_001_001_DC")[11], "l'altro movimento non si tocca");
    }

    @Test
    void soloProventiInEntrataImportati() {
        movimento(ID_BTT, "PRELIEVO CRYPTO", "BTT", "Crypto", "-1000", "", "", "");
        String Generato[] = movimento("20220124120006_Binance_001_001_RW", "REWARD", "", "", "", "BNB", "Crypto", "0.1");
        Generato[22] = "AU";
        String Collegato[] = movimento("20220124120007_Binance_001_001_RW", "REWARD", "", "", "", "BNB", "Crypto", "0.1");
        Collegato[20] = "20220124110000_Binance_001_001_PC";
        movimento("20220124120008_Binance_001_001_RW", "RIMBORSO REWARD", "BNB", "Crypto", "-0.1", "", "", "");
        movimento("20220124120009_Binance_001_001_RW", "CASHBACK", "", "", "", "EUR", "FIAT", "3");
        int Movimenti = MappaCryptoWallet.size();

        Map<String, String> Scartati = Principale_TipologiaReward.ConvertiInDepositi(List.copyOf(MappaCryptoWallet.keySet()));

        assertEquals(Movimenti, Scartati.size(), "nessuno dei cinque si converte: " + Scartati);
        assertEquals(Movimenti, MappaCryptoWallet.size());
        assertEquals(0, MovimentiStorico.VociInAttesa());
        assertTrue(Scartati.get(ID_BTT).contains("provento"));
        assertTrue(Scartati.get("20220124120006_Binance_001_001_RW").contains("classificazione"));
        assertTrue(Scartati.get("20220124120007_Binance_001_001_RW").contains("collegato"));
        assertTrue(Scartati.get("20220124120008_Binance_001_001_RW").contains("sola entrata"));
        assertTrue(Scartati.get("20220124120009_Binance_001_001_RW").contains("cripto-attività"));
    }

    @Test
    void ilDepositoSiAbbinaAlPrelievoComeScambioDifferito() {
        //Il caso della segnalazione per intero: BTT in uscita, BTTC arrivato come reward qualche ora dopo
        String BTT[] = movimento(ID_BTT, "PRELIEVO CRYPTO", "BTT", "Crypto", "-1000", "", "", "");
        BTT[1] = "2022-01-24 10:00";
        rewardBTTC();
        assertTrue(Principale_TipologiaReward.ConvertiInDepositi(List.of(ID_BTTC)).isEmpty());

        assertTrue(GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(ID_BTT, "20220124120000_Binance_001_001_DC"));

        long Prelievi = MappaCryptoWallet.values().stream().filter(v -> "PTW - Scambio Differito".equals(v[18])).count();
        long Depositi = MappaCryptoWallet.values().stream().filter(v -> "DTW - Scambio Differito".equals(v[18])).count();
        assertEquals(1, Prelievi, "il BTT diventa il trasferimento verso lo scambio");
        assertEquals(1, Depositi, "il BTTC il trasferimento di ritorno");
        assertTrue(MappaCryptoWallet.values().stream().anyMatch(v -> "BTT".equals(v[8]) && "BTTC".equals(v[11])),
                "e in mezzo c'e' lo scambio BTT -> BTTC");
    }

    @Test
    void laTipologiaCambiaSuTuttaLaSelezioneMaSoloSuiProventi() {
        movimento("20220124120006_Binance_001_001_RW", "REWARD", "", "", "", "BNB", "Crypto", "0.1");
        movimento("20220124120007_Binance_001_001_RW", "AIRDROP", "", "", "", "SGB", "Crypto", "9");
        String Classificato[] = movimento("20220124120008_Binance_001_001_DC", "REWARD", "", "", "", "ETH", "Crypto", "1");
        Classificato[18] = "DAI - Airdrop,Cashback,Rewards etc.. ";
        String Prelievo[] = movimento(ID_BTT, "PRELIEVO CRYPTO", "BTT", "Crypto", "-1000", "", "", "");
        String Deposito[] = movimento("20220124120009_Binance_001_001_DC", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "1");

        Map<String, String> Scartati = Principale_TipologiaReward.CambiaTipologia(List.copyOf(MappaCryptoWallet.keySet()), "STAKING REWARD");

        assertEquals("STAKING REWARD", MappaCryptoWallet.get("20220124120006_Binance_001_001_RW")[5]);
        assertEquals("STAKING REWARD", MappaCryptoWallet.get("20220124120007_Binance_001_001_RW")[5], "non solo la prima riga");
        assertEquals("STAKING REWARD", Classificato[5], "anche un deposito classificato come provento");
        //Prima un prelievo o un deposito non classificato potevano ricevere la descrizione di una reward
        assertEquals("PRELIEVO CRYPTO", Prelievo[5]);
        assertEquals("DEPOSITO CRYPTO", Deposito[5]);
        assertEquals(2, Scartati.size());
        assertTrue(Scartati.get(ID_BTT).contains("provento"));
        assertEquals(3, MovimentiStorico.VociInAttesa(), "una voce di storico per ogni provento modificato");
    }

    @Test
    void unRimborsoConservaIlPrefisso() {
        String Rimborso[] = movimento("20220124120008_Binance_001_001_RW", "RIMBORSO REWARD", "BNB", "Crypto", "-0.1", "", "", "");

        assertTrue(Principale_TipologiaReward.CambiaTipologia(List.of(Rimborso[0]), "CASHBACK").isEmpty());

        assertEquals("RIMBORSO CASHBACK", Rimborso[5]);
    }

    @Test
    void laTipologiaGiaAssegnataNonScriveNulla() {
        rewardBTTC();
        assertTrue(Principale_TipologiaReward.CambiaTipologia(List.of(ID_BTTC), "REWARD").isEmpty());
        assertEquals(0, MovimentiStorico.VociInAttesa());
        assertThrows(IllegalArgumentException.class, () -> Principale_TipologiaReward.CambiaTipologia(List.of(ID_BTTC), "PROVENTO"));
    }

    @Test
    void laVoceDelMenuSiAbilitaSoloSuiProventi() {
        assertTrue(Principale_TipologiaReward.isReward(rewardBTTC()));
        String Classificato[] = movimento("20220124120008_Binance_001_001_DC", "REWARD", "", "", "", "ETH", "Crypto", "1");
        Classificato[18] = "DAI - Airdrop,Cashback,Rewards etc.. ";
        assertTrue(Principale_TipologiaReward.isReward(Classificato));
        assertFalse(Principale_TipologiaReward.isReward(movimento("20220124120009_Binance_001_001_DC", "DEPOSITO CRYPTO", "", "", "", "BTC", "Crypto", "1")));
        assertFalse(Principale_TipologiaReward.isReward(null), "una riga di tabella che non e' un movimento");
    }
}
