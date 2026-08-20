package com.giacenzecrypto.giacenze_crypto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test di caratterizzazione delle mappe causali degli import nativi, ora lette da
 * {@code config/importmappe/} invece che scritte nel codice (vedi {@link MappeCausali}).
 * <p>Non fissa il numero di causali: aggiungerne o correggerne una è esattamente ciò che i file JSON
 * devono permettere senza toccare il programma. Fissa invece quello che non deve cambiare per sbaglio:
 * che ogni mappa esista e non sia vuota, che le categorie usate siano quelle riconosciute dal
 * consolidamento, e che alcune conversioni di riferimento restino quelle attese.
 */
public class MappeCausaliTest {

    /**
     * Categorie interne ammesse come valore di una mappa causali. Una categoria fuori da questo elenco
     * verrebbe ignorata silenziosamente dal consolidamento: qui invece fa fallire il test, che è la rete
     * di protezione contro un refuso in un file modificato a mano.
     */
    static final Set<String> CATEGORIE_AMMESSE = new HashSet<>(Arrays.asList(
            "ACQUISTO CRYPTO", "AIRDROP", "ALTRE-REWARD", "CASHBACK", "COMMISSIONI", "DEPOSITO FIAT",
            "DUST-CONVERSION", "EARN", "IGNORA", "NON CONSIDERARE", "PRELIEVO FIAT", "REWARD",
            "SCAMBIO CRYPTO", "SCAMBIO CRYPTO-CRYPTO", "SCAMBIO DIFFERITO", "STAKING REWARD",
            "STAKING REWARDS", "TRASFERIMENTO-CRYPTO", "TRASFERIMENTO-CRYPTO-INTERNO", "VENDITA CRYPTO"));

    @Test
    public void ogniMappaDiSistemaSiCaricaEdENonVuota() {
        for (String nome : MappeCausali.MAPPE_DI_SISTEMA) {
            Map<String, String> mappa = MappeCausali.Carica(nome);
            assertNotNull(mappa, "mappa non caricata: " + nome);
            assertFalse(mappa.isEmpty(), "mappa vuota: " + nome);
        }
    }

    /**
     * Le altre verifiche passano da {@code Carica()}, che trova i file su disco e non arriva mai al ripiego
     * sulla copia nel jar: se la voce {@code <resource>} di {@code pom.xml} che copia
     * {@code config/importmappe} in {@code /ImportMappe/} si rompesse, il jar uscirebbe senza default e
     * nessun test se ne accorgerebbe. Qui si guarda direttamente il classpath, che
     * {@code process-resources} popola prima dei test.
     */
    @Test
    public void leMappeDiDefaultSonoIncluseTraLeRisorseDelProgramma() throws Exception {
        //FILE_DI_SISTEMA e non MAPPE_DI_SISTEMA: in config/importmappe/ vivono anche tabelle di forma
        //diversa dalle mappe causali (OKX_Tipi), e anche quelle devono entrare nel jar come default
        for (String nome : MappeCausali.FILE_DI_SISTEMA) {
            try (java.io.InputStream in = MappeCausali.class.getResourceAsStream("/ImportMappe/" + nome + ".json")) {
                assertNotNull(in, "manca la copia di default nel jar per la mappa " + nome
                        + " (controllare la sezione <resources> del pom.xml)");
                assertTrue(in.readAllBytes().length > 0, "copia di default vuota per la mappa " + nome);
            }
        }
    }

    @Test
    public void ogniCausaleUsaUnaCategoriaRiconosciuta() {
        for (String nome : MappeCausali.MAPPE_DI_SISTEMA) {
            Map<String, String> mappa = MappeCausali.Carica(nome);
            assertNotNull(mappa, "mappa non caricata: " + nome);
            for (Map.Entry<String, String> voce : mappa.entrySet()) {
                assertTrue(CATEGORIE_AMMESSE.contains(voce.getValue()),
                        "categoria non riconosciuta \"" + voce.getValue() + "\" per la causale \""
                        + voce.getKey() + "\" nella mappa " + nome);
            }
        }
    }

    @Test
    public void leMappeSonoCaseInsensitiveComeQuelleCheSostituiscono() {
        //Gli import confrontano la causale del CSV così come arriva dal file: Binance ha scritto sia
        //"withdraw" sia "Withdraw" in export diversi, e devono risolversi entrambe
        Map<String, String> binance = MappeCausali.Carica(MappeCausali.BINANCE_OLD);
        assertNotNull(binance);
        assertEquals("TRASFERIMENTO-CRYPTO", binance.get("withdraw"));
        assertEquals("TRASFERIMENTO-CRYPTO", binance.get("WITHDRAW"));
        assertEquals("TRASFERIMENTO-CRYPTO", binance.get("Withdraw"));
    }

    @Test
    public void conversioniDiRiferimentoBinance() {
        Map<String, String> m = MappeCausali.Carica(MappeCausali.BINANCE_OLD);
        assertNotNull(m);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", m.get("Binance Convert"));
        assertEquals("DUST-CONVERSION", m.get("Small Assets Exchange BNB"));
        assertEquals("SCAMBIO DIFFERITO", m.get("Auto-Invest Transaction"));
        assertEquals("COMMISSIONI", m.get("Transaction Fee"));
        assertEquals("STAKING REWARDS", m.get("Staking Rewards"));
        assertEquals("TRASFERIMENTO-CRYPTO-INTERNO", m.get("Simple Earn Flexible Subscription"));
    }

    @Test
    public void conversioniDiRiferimentoOKX() {
        Map<String, String> m = MappeCausali.Carica(MappeCausali.OKX);
        assertNotNull(m);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", m.get("Convert"));
        assertEquals("TRASFERIMENTO-CRYPTO", m.get("Received"));
        assertEquals("REWARD", m.get("Deposit yield"));
        assertEquals("NON CONSIDERARE", m.get("Transfer out"));
        assertEquals("TRASFERIMENTO-CRYPTO-INTERNO", m.get("Stake"));
        //La causale vuota è una chiave legittima: i bill OKX senza descrizione vanno scartati
        assertEquals("NON CONSIDERARE", m.get(""));
    }

    @Test
    public void conversioniDiRiferimentoCryptoCom() {
        Map<String, String> app = MappeCausali.Carica(MappeCausali.CRYPTOCOM_APP);
        assertNotNull(app);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", app.get("crypto_exchange"));
        assertEquals("ACQUISTO CRYPTO", app.get("viban_purchase"));
        assertEquals("VENDITA CRYPTO", app.get("crypto_viban_exchange"));
        assertEquals("DUST-CONVERSION", app.get("dust_conversion_debited"));
        assertEquals("STAKING REWARD", app.get("mco_stake_reward"));

        Map<String, String> exchange = MappeCausali.Carica(MappeCausali.CRYPTOCOM_EXCHANGE);
        assertNotNull(exchange);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", exchange.get("TRADING"));
        assertEquals("COMMISSIONI", exchange.get("TRADE_FEE"));
        assertEquals("NON CONSIDERARE", exchange.get("STAKING"));
    }

    @Test
    public void conversioniDiRiferimentoTataxEBinanceReport() {
        Map<String, String> tatax = MappeCausali.Carica(MappeCausali.TATAX_OLD);
        assertNotNull(tatax);
        assertEquals("SCAMBIO CRYPTO-CRYPTO", tatax.get("CREDIT"));
        assertEquals("SCAMBIO CRYPTO-CRYPTO", tatax.get("DEBIT"));
        assertEquals("COMMISSIONI", tatax.get("BLOCKCHAIN_FEE"));
        assertEquals("STAKING REWARDS", tatax.get("STAKING"));

        Map<String, String> report = MappeCausali.Carica(MappeCausali.BINANCE_FINANCIAL_REPORT);
        assertNotNull(report);
        //Le chiavi sono la concatenazione delle colonne 2, 3 e 4: i punti restano anche se una è vuota
        assertEquals("ACQUISTO CRYPTO", report.get("BUY..SPOT"));
        assertEquals("VENDITA CRYPTO", report.get("SELL..CONVERT"));
        assertEquals("TRASFERIMENTO-CRYPTO", report.get("SEND..CRYPTO_WITHDRAWAL"));
        assertEquals("PRELIEVO FIAT", report.get("WITHDRAWAL..FIAT"));
    }

    /**
     * Gli import passano da questi metodi, non direttamente da {@link MappeCausali}: verifica che il
     * ponte fra i due sia intatto per tutte e sei le mappe.
     */
    @Test
    public void gliImportNativiVedonoLeMappeCaricateDaFile() {
        assertNotNull(Importazioni.Ex_Binance_MappaCausali());
        assertNotNull(Importazioni.Ex_BinanceTaxReport_MappaCausali());
        assertNotNull(Importazioni.Ex_OKX_MappaCausali());
        assertNotNull(Importazioni.Ex_CDCAPP_MappaCausali());
        assertNotNull(Importazioni.Ex_CryptoComExchange_MappaCausali());
        assertNotNull(Importazioni.Ex_Tatax_MappaCausali());
    }
}
