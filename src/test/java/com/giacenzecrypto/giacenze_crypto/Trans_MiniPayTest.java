package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Importazione di un wallet MiniPay su Celo e della Safe della sua carta ({@link Trans_MiniPay}): commissione CIP-64 pagata
 * in USDT, commissione in CELO, ricariche e prelievi fra wallet e carta, spese e accrediti della carta, riconoscimento dei
 * moduli, abbinamento. Indirizzi inventati: niente wallet o hash veri dell'utente nei test. La storia vera, se c'è, sta in
 * {@code nocommit/DatiTestChain/MiniPay/storia.json} (fuori dal repository).
 */
class Trans_MiniPayTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/MiniPay/storia.json");

    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String SAFE = "0xbbbb000000000000000000000000000000000002";
    private static final String USDT = "0xcccc000000000000000000000000000000000003";
    private static final String ADATTATORE = "0xdddd000000000000000000000000000000000004";
    private static final String REGOLAMENTO = "0xeeee000000000000000000000000000000000005";
    private static final String ACCREDITI = "0xffff000000000000000000000000000000000006";
    private static final String DAIMO = "0x1111000000000000000000000000000000000007";
    private static final String OPERATORE = "0x2222000000000000000000000000000000000008";
    private static final String GESTORE_FEE = "0x3333000000000000000000000000000000000009";
    private static final String ESTERNO = "0x444400000000000000000000000000000000000a";
    private static final String DELAY = "0x555500000000000000000000000000000000000b";
    private static final String BOOST = "0x666600000000000000000000000000000000000c";
    private static final String CE1O = Trans_MiniPay.INDIRIZZO_COMMISSIONI;

    private static final Trans_MiniPay.Config CFG = new Trans_MiniPay.Config(List.of("finto"), 0, DELAY, "",
            Set.of(REGOLAMENTO), Set.of(ACCREDITI), Set.of(DAIMO), Set.of(BOOST), Map.of(USDT, "USDT"));

    private int indice;

    @BeforeEach
    @AfterEach
    void pulisce() {
        MappaCryptoWallet.clear();
        indice = 0;
    }

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    private static String topic(String indirizzo) {
        return "0x" + Trans_XLayer.Parola(indirizzo);
    }

    private static String hash(int n) {
        return "0x" + Trans_XLayer.Parola(Integer.toHexString(n));
    }

    private JsonObject transfer(String tx, long blocco, String token, String da, String a, long v) {
        JsonObject l = new JsonObject();
        l.addProperty("address", token);
        JsonArray t = new JsonArray();
        t.add(Trans_MiniPay.TOPIC_TRANSFER);
        t.add(topic(da));
        t.add(topic(a));
        l.add("topics", t);
        l.addProperty("data", "0x" + Trans_XLayer.Parola(Long.toHexString(v)));
        l.addProperty("transactionHash", tx);
        l.addProperty("blockNumber", "0x" + Long.toHexString(blocco));
        l.addProperty("blockTimestamp", "0x" + Long.toHexString(1_772_000_000L + blocco));
        l.addProperty("logIndex", "0x" + Integer.toHexString(indice++));
        return l;
    }

    /** Nodo finto: transazioni, ricevute e token in memoria. */
    private static final class FonteMemoria implements Trans_MiniPay.Fonte {
        final Map<String, JsonObject> tx = new HashMap<>(), ricevute = new HashMap<>();
        final Map<String, Trans_MiniPay.Token> token = new HashMap<>();

        @Override
        public Trans_MiniPay.Token token(String a) {
            return token.getOrDefault(a, new Trans_MiniPay.Token(a, a, 18));
        }

        @Override
        public JsonObject transazione(String hash) throws Exception {
            JsonObject t = tx.get(hash);
            if (t == null) throw new Exception("transazione sconosciuta " + hash);
            return t;
        }

        @Override
        public JsonObject ricevuta(String hash) throws Exception {
            JsonObject r = ricevute.get(hash);
            if (r == null) throw new Exception("ricevuta sconosciuta " + hash);
            return r;
        }

        @Override
        public long tempo(long blocco) {
            return 0;
        }
    }

    private static JsonObject tx(String da, String a, String feeCurrency, long valore) {
        JsonObject t = new JsonObject();
        t.addProperty("from", da);
        t.addProperty("to", a);
        if (feeCurrency != null) t.addProperty("feeCurrency", feeCurrency);
        t.addProperty("value", "0x" + Long.toHexString(valore));
        return t;
    }

    private static JsonObject ricevuta(List<JsonObject> logs, long gasUsato, long prezzoGas) {
        JsonObject r = new JsonObject();
        JsonArray a = new JsonArray();
        logs.forEach(a::add);
        r.add("logs", a);
        r.addProperty("status", "0x1");
        r.addProperty("gasUsed", "0x" + Long.toHexString(gasUsato));
        r.addProperty("effectiveGasPrice", "0x" + Long.toHexString(prezzoGas));
        return r;
    }

    /**
     * La storia di prova: una ricarica Daimo sul wallet, la ricarica della carta pagata in USDT (CIP-64), poi sulla carta una
     * spesa, un accredito e un prelievo che torna al wallet.
     */
    private final FonteMemoria fonte = new FonteMemoria();
    private final List<JsonObject> logWallet = new ArrayList<>(), logSafe = new ArrayList<>();
    private final String txDaimo = hash(1), txRicarica = hash(2), txSpesa = hash(3), txAccredito = hash(4), txPrelievo = hash(5);

    private void storia() {
        fonte.token.put(USDT, new Trans_MiniPay.Token(USDT, "USDT", 6));
        logWallet.add(transfer(txDaimo, 10, USDT, DAIMO, W, 2_000_000));
        fonte.tx.put(txDaimo, tx(OPERATORE, DAIMO, null, 0));

        //CIP-64: prelievo del massimo, trasferimento, accrediti del nodo, restituzione al wallet (ultimo log)
        List<JsonObject> ric = new ArrayList<>();
        ric.add(transfer(txRicarica, 20, USDT, W, CE1O, 3_000));
        JsonObject versoSafe = transfer(txRicarica, 20, USDT, W, SAFE, 1_400_000);
        ric.add(versoSafe);
        ric.add(transfer(txRicarica, 20, USDT, CE1O, GESTORE_FEE, 2_000));
        ric.add(transfer(txRicarica, 20, USDT, CE1O, W, 1_000));
        logWallet.add(ric.get(0));
        logWallet.add(versoSafe);
        logWallet.add(ric.get(3));
        logSafe.add(versoSafe);
        fonte.tx.put(txRicarica, tx(W, USDT, ADATTATORE, 0));
        fonte.ricevute.put(txRicarica, ricevuta(ric, 100_000, 20_000_000_000L));

        logSafe.add(transfer(txSpesa, 30, USDT, SAFE, REGOLAMENTO, 400_000));
        logSafe.add(transfer(txAccredito, 40, USDT, ACCREDITI, SAFE, 100));
        JsonObject prelievo = transfer(txPrelievo, 50, USDT, SAFE, W, 1_000);
        logSafe.add(prelievo);
        logWallet.add(prelievo);
        fonte.tx.put(txPrelievo, tx(OPERATORE, SAFE, null, 0));
    }

    // ------------------------------------------------------------------
    // Ricostruzione
    // ------------------------------------------------------------------

    @Test
    void laCommissioneCip64EIlPrelievoMenoLaRestituzioneEINonSonoMovimenti() throws Exception {
        storia();
        Map<String, BigInteger> saldi = new LinkedHashMap<>();
        int[] firmate = {0};
        List<String> avvisi = new ArrayList<>();
        List<Trans_MiniPay.Transazione> t = Trans_MiniPay.Ricostruisci(W, SAFE, false, logWallet, fonte, CFG, saldi, firmate, avvisi);
        assertTrue(avvisi.isEmpty(), avvisi.toString());
        assertEquals(1, firmate[0]);
        assertEquals(3, t.size());

        Trans_MiniPay.Movimento daimo = t.get(0).movimenti().get(0);
        assertEquals(Trans_MiniPay.CAUSALE_DAIMO, daimo.causale());
        assertEquals(BigInteger.valueOf(2_000_000), daimo.qtaEntrata());

        List<Trans_MiniPay.Movimento> ricarica = t.get(1).movimenti();
        assertEquals(2, ricarica.size(), ricarica.toString());
        assertEquals("COMMISSIONE", ricarica.get(0).tipo());
        assertEquals(BigInteger.valueOf(2_000), ricarica.get(0).qtaUscita());
        assertEquals(USDT, ricarica.get(0).uscita().indirizzo(), "la commissione e' in USDT, non in CELO");
        assertEquals(Trans_MiniPay.CAUSALE_RICARICA_CARTA, ricarica.get(1).causale());
        assertEquals(BigInteger.valueOf(1_400_000), ricarica.get(1).qtaUscita());

        Trans_MiniPay.Movimento dallaCarta = t.get(2).movimenti().get(0);
        assertEquals(Trans_MiniPay.CAUSALE_PRELIEVO_CARTA, dallaCarta.causale());
        assertEquals(BigInteger.valueOf(1_000), dallaCarta.qtaEntrata());

        assertEquals(BigInteger.valueOf(2_000_000 - 2_000 - 1_400_000 + 1_000), saldi.get(USDT));
        assertNull(saldi.get(Trans_MiniPay.MONETA), "nessun CELO speso: il gas l'ha pagato l'USDT");
    }

    @Test
    void laCommissioneSiConfrontaConIlGasInUnitaA18Decimali() {
        JsonObject r = ricevuta(List.of(), 100_000, 20_000_000_000L);
        assertNull(Trans_MiniPay.ControllaCommissione(BigInteger.valueOf(2_000), 6, r));
        assertNotNull(Trans_MiniPay.ControllaCommissione(BigInteger.valueOf(2_002), 6, r));
    }

    @Test
    void unaRicevutaSenzaIlPrelievoInizialeNonDaCommissione() {
        JsonObject r = ricevuta(List.of(transfer(hash(9), 1, USDT, W, ESTERNO, 5)), 1, 1);
        assertNull(Trans_MiniPay.CommissioneCip64(W, r));
    }

    @Test
    void sullaCartaSpesaAccreditoRicaricaEPrelievo() throws Exception {
        storia();
        Map<String, BigInteger> saldi = new LinkedHashMap<>();
        List<String> avvisi = new ArrayList<>();
        List<Trans_MiniPay.Transazione> t = Trans_MiniPay.Ricostruisci(SAFE, W, true, logSafe, fonte, CFG, saldi, null, avvisi);
        assertTrue(avvisi.isEmpty(), avvisi.toString());
        assertEquals(4, t.size());
        assertEquals(Trans_MiniPay.CAUSALE_RICARICA_CARTA, t.get(0).movimenti().get(0).causale());
        assertEquals(BigInteger.valueOf(1_400_000), t.get(0).movimenti().get(0).qtaEntrata());
        assertEquals(1, t.get(0).movimenti().size(), "la commissione la paga il wallet, non la carta");

        Trans_MiniPay.Movimento spesa = t.get(1).movimenti().get(0);
        assertEquals("CASHOUT O SIMILARE", spesa.tipo());
        assertEquals(Trans_MiniPay.CAUSALE_CARTA, spesa.causale());
        assertEquals(REGOLAMENTO, spesa.controparte());
        String[] riga = Trans_MiniPay.Righe(t.get(1), "1.00").get(0);
        assertEquals("SPESA CON CARTA", riga[5]);
        assertEquals("PCO - SPESA CON CARTA", riga[18]);
        assertEquals(SAFE + " (CELO)", riga[3]);

        assertEquals(Trans_MiniPay.CAUSALE_ACCREDITO_CARTA, t.get(2).movimenti().get(0).causale());
        assertEquals(Trans_MiniPay.CAUSALE_PRELIEVO_CARTA, t.get(3).movimenti().get(0).causale());
        assertEquals(BigInteger.valueOf(1_000), t.get(3).movimenti().get(0).qtaUscita());
        assertEquals(BigInteger.valueOf(1_400_000 - 400_000 + 100 - 1_000), saldi.get(USDT));
    }

    @Test
    void senzaFeeCurrencyLaCommissioneEIlValoreInviatoSonoInCelo() throws Exception {
        String h = hash(7);
        logWallet.add(transfer(h, 5, USDT, ESTERNO, W, 10));
        fonte.tx.put(h, tx(W, ESTERNO, null, 3_000));
        fonte.ricevute.put(h, ricevuta(List.of(), 21_000, 25_000_000_000L));
        Map<String, BigInteger> saldi = new LinkedHashMap<>();
        List<Trans_MiniPay.Transazione> t = Trans_MiniPay.Ricostruisci(W, null, false, logWallet, fonte, CFG, saldi, null, new ArrayList<>());
        List<Trans_MiniPay.Movimento> m = t.get(0).movimenti();
        assertEquals("COMMISSIONE", m.get(0).tipo());
        assertSame(Trans_MiniPay.CELO_NATIVO, m.get(0).uscita());
        assertEquals(BigInteger.valueOf(21_000L * 25_000_000_000L), m.get(0).qtaUscita());
        //CELO uscito e USDT entrato nella stessa transazione: uno scambio
        assertEquals("Scambio", m.get(1).causale());
        assertEquals(BigInteger.valueOf(3_000), m.get(1).qtaUscita());
        assertEquals(BigInteger.valueOf(-(21_000L * 25_000_000_000L) - 3_000), saldi.get(Trans_MiniPay.MONETA));
    }

    @Test
    void ilBoostDiMiniPayEUnaReward() throws Exception {
        fonte.token.put(USDT, new Trans_MiniPay.Token(USDT, "USDT", 6));
        String h = hash(8);
        logWallet.add(transfer(h, 60, USDT, BOOST, W, 50_000));
        fonte.tx.put(h, tx(OPERATORE, BOOST, null, 0));
        Map<String, BigInteger> saldi = new LinkedHashMap<>();
        List<Trans_MiniPay.Transazione> t = Trans_MiniPay.Ricostruisci(W, SAFE, false, logWallet, fonte, CFG, saldi, null, new ArrayList<>());
        Trans_MiniPay.Movimento m = t.get(0).movimenti().get(0);
        assertEquals("REWARD", m.tipo());
        assertEquals(Trans_MiniPay.CAUSALE_BOOST, m.causale());
        assertEquals(BigInteger.valueOf(50_000), m.qtaEntrata());
        String[] riga = Trans_MiniPay.Righe(t.get(0), "1.00").get(0);
        assertTrue(riga[0].endsWith("_RW"), riga[0]);
        assertEquals(Trans_MiniPay.CAUSALE_BOOST, riga[7]);
        assertEquals(BigInteger.valueOf(50_000), saldi.get(USDT));
    }

    // ------------------------------------------------------------------
    // Riconoscimento della Safe
    // ------------------------------------------------------------------

    @Test
    void ilProxyMinimoDiceLImplementazione() {
        assertEquals(DELAY, Trans_MiniPay.ImplementazioneEip1167("0x363d3d373d3d3d363d73" + DELAY.substring(2)
                + "5af43d82803e903d91602b57fd5bf3"));
        assertNull(Trans_MiniPay.ImplementazioneEip1167("0x6080604052"));
        assertNull(Trans_MiniPay.ImplementazioneEip1167("0x"));
    }

    @Test
    void iModuliSiLeggonoDaGetModulesPaginated() {
        String r = "0x" + Trans_XLayer.Parola("40") + Trans_XLayer.Parola("1") + Trans_XLayer.Parola("2")
                + Trans_XLayer.Parola(DELAY) + Trans_XLayer.Parola(OPERATORE);
        assertEquals(List.of(DELAY, OPERATORE), Trans_MiniPay.Moduli(r));
        assertTrue(Trans_MiniPay.Moduli("0x").isEmpty());
    }

    @Test
    void leContropartiSonoQuelleDeiTrasferimentiSenzaIlWalletNeZero() {
        storia();
        assertEquals(List.of(DAIMO, SAFE), Trans_MiniPay.Controparti(W, logWallet));
    }

    // ------------------------------------------------------------------
    // Abbinamento
    // ------------------------------------------------------------------

    private static String[] riga(String id, String wallet, String causale, String hash) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[3] = wallet + " (CELO)";
        v[7] = causale;
        v[24] = hash;
        v[15] = "1.99";
        MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void ricaricaEPrelievoDellaCartaDiventanoTrasferimentiFraWalletPropri() {
        String[] p = riga("20260301120000_" + W + " (CELO)_2_2_PC", W, Trans_MiniPay.CAUSALE_RICARICA_CARTA, hash(2));
        p[8] = "USDT";
        p[10] = "-1.5";
        String[] d = riga("20260301120000_" + SAFE + " (CELO)_1_1_DC", SAFE, Trans_MiniPay.CAUSALE_RICARICA_CARTA, hash(2));
        d[11] = "USDT";
        d[13] = "1.5";
        //Stessa transazione ma quantita' diversa: non e' la stessa cosa
        String[] p2 = riga("20260302120000_" + SAFE + " (CELO)_1_1_PC", SAFE, Trans_MiniPay.CAUSALE_PRELIEVO_CARTA, hash(5));
        p2[8] = "USDT";
        p2[10] = "-0.001";
        String[] d2 = riga("20260302120000_" + W + " (CELO)_1_1_DC", W, Trans_MiniPay.CAUSALE_PRELIEVO_CARTA, hash(5));
        d2[11] = "USDT";
        d2[13] = "0.002";

        assertEquals(1, Trans_MiniPay.Abbina(MappaCryptoWallet, Map.of(W, SAFE)));
        assertTrue(p[18].startsWith("PTW"), p[18]);
        assertTrue(d[18].startsWith("DTW"), d[18]);
        assertEquals(d[0], p[20]);
        assertTrue(p2[18].isBlank());
        assertTrue(d2[18].isBlank());
        assertEquals(0, Trans_MiniPay.Abbina(MappaCryptoWallet, Map.of(W, SAFE)), "rifatto non cambia nulla");
    }

    // ------------------------------------------------------------------
    // Storia vera (nocommit, non nel repository)
    // ------------------------------------------------------------------

    @Test
    void sullaStoriaVeraISaldiTornanoConQuelliDellaCatena() throws Exception {
        Assumptions.assumeTrue(Files.isRegularFile(DATI_REALI), "dati di prova MiniPay assenti (" + DATI_REALI + ")");
        JsonObject d = JsonParser.parseString(Files.readString(DATI_REALI, StandardCharsets.UTF_8)).getAsJsonObject();
        Trans_MiniPay.Config cfg = Trans_MiniPay.Configurazione();
        assertNotNull(cfg);
        FonteMemoria f = new FonteMemoria();
        for (Map.Entry<String, JsonElement> e : d.getAsJsonObject("transazioni").entrySet()) f.tx.put(e.getKey(), e.getValue().getAsJsonObject());
        for (Map.Entry<String, JsonElement> e : d.getAsJsonObject("ricevute").entrySet()) f.ricevute.put(e.getKey(), e.getValue().getAsJsonObject());
        for (Map.Entry<String, JsonElement> e : d.getAsJsonObject("token").entrySet()) {
            f.token.put(e.getKey(), new Trans_MiniPay.Token(e.getKey(), cfg.simboli().getOrDefault(e.getKey(), e.getKey()), e.getValue().getAsInt()));
        }
        String w = d.get("wallet").getAsString(), safe = d.get("safe").getAsString();
        for (String[] chi : new String[][]{{w, safe, "logWallet"}, {safe, w, "logSafe"}}) {
            List<JsonObject> logs = new ArrayList<>();
            d.getAsJsonArray(chi[2]).forEach(x -> logs.add(x.getAsJsonObject()));
            Map<String, BigInteger> saldi = new LinkedHashMap<>();
            List<String> avvisi = new ArrayList<>();
            List<Trans_MiniPay.Transazione> t = Trans_MiniPay.Ricostruisci(chi[0], chi[1], chi[0].equals(safe), logs, f, cfg, saldi, new int[1], avvisi);
            assertTrue(avvisi.isEmpty(), avvisi.toString());
            assertFalse(t.isEmpty());
            for (Trans_MiniPay.Transazione x : t) {
                for (Trans_MiniPay.Movimento m : x.movimenti()) {
                    assertNotEquals("Deposito", m.causale(), "entrata non riconosciuta nella transazione " + x.hash());
                }
            }
            JsonObject veri = d.getAsJsonObject("saldi").getAsJsonObject(chi[0]);
            for (Map.Entry<String, JsonElement> e : veri.entrySet()) {
                assertEquals(new BigInteger(e.getValue().getAsString()), saldi.getOrDefault(e.getKey(), BigInteger.ZERO),
                        chi[2] + ", moneta " + e.getKey());
            }
        }
    }
}
