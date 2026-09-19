package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa l'avviso fiscale sui prodotti derivati (Dual Investment e futuri casi analoghi): una causale
 * elencata in {@code causaliAllertaDerivati} (config JSON) fa comparire, a fine importazione, un
 * avviso indipendente dall'esito dell'abbinamento delle singole righe — il programma non calcola i
 * redditi da derivati (art. 67, c. 1, lett. c-quater TUIR), quindi l'utente va avvisato anche quando
 * l'abbinamento a un altro movimento riesce perfettamente.
 *
 * <p>Punti fissati:</p>
 * <ul>
 *   <li>il match è sulla causale <b>grezza</b> del CSV ({@link ImportazioneGenerica.ConfigurazioneImport#getCausaleCSV}),
 *       non su quella mappata — stesso trattamento di {@code causaliDifferite};</li>
 *   <li>{@link Importazioni#CausaliDerivatiSegnalate} è indipendente da {@link Importazioni#movimentiSconosciuti}:
 *       qui i movimenti sono importati correttamente, non sono errori di riconoscimento;</li>
 *   <li>{@link Importazioni#AzzeraContatori()} lo svuota, come ogni altro contatore di sessione.</li>
 * </ul>
 */
class ImportazioneGenericaAvvisoDerivatiTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void azzeraStato() {
        Importazioni.AzzeraContatori();
    }

    private static ConfigurazioneImport cfgBinanceVeloce() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Binance CSV.json");
        c.colonnaValoreEuro = 7; // controvalore sintetico: niente ricerca prezzi online
        return c;
    }

    /** {@code User_ID,UTC_Time,Account,Operation,Coin,Change,Remark} + [7]=controvalore sintetico */
    private static String[] rigaBinance(String time, String operation, String coin, String change) {
        return new String[]{"1", time, "Spot", operation, coin, change, "", "1.00"};
    }

    @Test
    void configurazioneReale_elencaLeCausaliDualInvestment() throws Exception {
        ConfigurazioneImport c = cfgBinanceVeloce();
        assertTrue(c.causaliAllertaDerivati.contains("Dual Savings Purchase"));
        assertTrue(c.causaliAllertaDerivati.contains("Dual Savings Settlement"));
    }

    @Test
    void consolidaGruppo_causaleInLista_segnalaAvviso() throws Exception {
        ConfigurazioneImport cfg = cfgBinanceVeloce();
        List<String[]> differiti = new ArrayList<>();
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2022-05-12 06:24:50", "Dual Savings Purchase", "USDT", "-100.00000000"));

        ImportazioneGenerica.consolidaGruppo(gruppo, cfg, differiti);

        assertTrue(Importazioni.CausaliDerivatiSegnalate.contains("Dual Savings Purchase"));
        assertFalse(Importazioni.TestoAvvisoDerivati().isBlank());
        assertTrue(Importazioni.TestoAvvisoDerivati().contains("Dual Savings Purchase"));
    }

    @Test
    void consolidaGruppo_causaleNonInLista_nonSegnalaNulla() throws Exception {
        ConfigurazioneImport cfg = cfgBinanceVeloce();
        List<String[]> differiti = new ArrayList<>();
        List<String[]> gruppo = new ArrayList<>();
        gruppo.add(rigaBinance("2022-05-12 06:24:50", "Staking Rewards", "BTC", "0.001"));

        ImportazioneGenerica.consolidaGruppo(gruppo, cfg, differiti);

        assertTrue(Importazioni.CausaliDerivatiSegnalate.isEmpty());
        assertTrue(Importazioni.TestoAvvisoDerivati().isBlank());
    }

    @Test
    void azzeraContatori_svuotaLeCausaliSegnalate() {
        Importazioni.SegnalaCausaleDerivato("Dual Savings Purchase");
        assertFalse(Importazioni.CausaliDerivatiSegnalate.isEmpty());

        Importazioni.AzzeraContatori();

        assertTrue(Importazioni.CausaliDerivatiSegnalate.isEmpty());
        assertTrue(Importazioni.TestoAvvisoDerivati().isBlank());
    }

    @Test
    void testoAvviso_elencaTutteLeCausaliSegnalate() {
        Importazioni.SegnalaCausaleDerivato("Dual Savings Purchase");
        Importazioni.SegnalaCausaleDerivato("Dual Savings Settlement");

        String testo = Importazioni.TestoAvvisoDerivati();

        assertTrue(testo.contains("Dual Savings Purchase"));
        assertTrue(testo.contains("Dual Savings Settlement"));
        assertTrue(testo.contains("c-quater"), "deve citare il riferimento normativo");
    }
}
