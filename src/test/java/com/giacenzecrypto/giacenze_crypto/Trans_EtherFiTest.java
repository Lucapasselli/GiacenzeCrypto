package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Importazione dei conti ether.fi Cash ({@link Trans_EtherFi}): classificazione dai log (ricarica, scambi dei moduli,
 * swap in due tempi, spesa con la carta, interessi dalle quote dello spoke, NFT SCAM, movimenti non riconosciuti),
 * lettura a tratti dei log, abbinamento delle ricariche. Indirizzi inventati: niente wallet o hash veri dell'utente nei
 * test. La storia vera di un Safe, se c'è, sta in {@code nocommit/DatiTestChain/ETHERFI/<safe>/} (fuori dal repository).
 */
class Trans_EtherFiTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/ETHERFI");

    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String LG = "0xbbbb000000000000000000000000000000000002";
    private static final String SPOKE = "0xcccc000000000000000000000000000000000003";
    private static final String TOPUP = "0xdddd000000000000000000000000000000000004";
    private static final String EMITTER = "0xeeee000000000000000000000000000000000005";
    private static final String MIDAS = "0xffff000000000000000000000000000000000006";
    private static final String SWAP = "0x1111000000000000000000000000000000000007";
    private static final String USDC = "0x2222000000000000000000000000000000000008";
    private static final String EURC = "0x3333000000000000000000000000000000000009";
    private static final String VAULT = "0x444400000000000000000000000000000000000a";
    private static final String ESTERNO = "0x555500000000000000000000000000000000000b";
    private static final String REGOLAMENTO = "0x666600000000000000000000000000000000000c";
    private static final String NFT = "0x777700000000000000000000000000000000000d";
    private static final String ZERO = Trans_EtherFi.INDIRIZZO_NULLO;
    private static final String VAULT_MIDAS = "0x888800000000000000000000000000000000000e";
    private static final String CODA_LIQUID = "0x999900000000000000000000000000000000000f";
    private static final String WBTC_ETH = "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599";
    private static final String TRADING_SAFE = "0xaaaa0000000000000000000000000000000000d1";

    private static final Trans_EtherFi.Config CFG = new Trans_EtherFi.Config(List.of("finto"), 1, "0x00", LG, 0, SPOKE, TOPUP,
            EMITTER, Map.of(MIDAS, "Midas", SWAP, "Swap"), Map.of(), "");

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

    private static String keccak(String firma) {
        byte[] h = new Keccak.Digest256().digest(firma.getBytes());
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String topic(String indirizzo) {
        return "0x" + Trans_XLayer.Parola(indirizzo);
    }

    private static String num(long n) {
        return "0x" + Trans_XLayer.Parola(Long.toHexString(n));
    }

    /** Campo data: un indirizzo/parola esadecimale (stringa) o un numero per parola. */
    private static String dati(Object... parole) {
        StringBuilder sb = new StringBuilder("0x");
        for (Object p : parole) {
            sb.append(Trans_XLayer.Parola(p instanceof String s ? s : Long.toHexString(((Number) p).longValue())));
        }
        return sb.toString();
    }

    private JsonObject log(String tx, long blocco, String indirizzo, String dati, String... topics) {
        JsonObject l = new JsonObject();
        l.addProperty("address", indirizzo);
        JsonArray t = new JsonArray();
        for (String x : topics) t.add(x);
        l.add("topics", t);
        l.addProperty("data", dati);
        l.addProperty("transactionHash", tx);
        l.addProperty("blockNumber", "0x" + Long.toHexString(blocco));
        l.addProperty("blockTimestamp", "0x" + Long.toHexString(1_792_800_000L + blocco));
        l.addProperty("logIndex", "0x" + Integer.toHexString(indice++));
        return l;
    }

    private JsonObject transfer(String tx, long b, String token, String da, String a, long v) {
        return log(tx, b, token, dati(v), Trans_EtherFi.TOPIC_TRANSFER, topic(da), topic(a));
    }

    private JsonObject supplied(String tx, long b, String asset, long v) {
        return log(tx, b, LG, dati(v), Trans_EtherFi.TOPIC_SUPPLIED, topic(W), topic(asset));
    }

    private JsonObject withdrawn(String tx, long b, String asset, long v, String verso) {
        return log(tx, b, LG, dati(v), Trans_EtherFi.TOPIC_WITHDRAWN, topic(W), topic(asset), topic(verso));
    }

    private JsonObject spoke(String tx, long b, boolean supply, long riserva, long quote, long importo) {
        return log(tx, b, SPOKE, dati(quote, importo), supply ? Trans_EtherFi.TOPIC_SUPPLY : Trans_EtherFi.TOPIC_WITHDRAW,
                num(riserva), topic(LG), topic(W));
    }

    private static String hash(int n) {
        return "0x" + Trans_XLayer.Parola(Integer.toHexString(n));
    }

    /** Fonte finta: USDC e EURC a 6 decimali, riserve 0 e 3; ogni altro contratto è un NFT senza decimali. */
    private static final Trans_EtherFi.Fonte FONTE = new Trans_EtherFi.Fonte() {
        @Override
        public Trans_EtherFi.Token token(String a) {
            if (a.equals(USDC)) return new Trans_EtherFi.Token(a, "USDC", 6);
            if (a.equals(EURC)) return new Trans_EtherFi.Token(a, "EURC", 6);
            if (a.equals(VAULT)) return new Trans_EtherFi.Token(a, "liquidRWA", 6);
            return new Trans_EtherFi.Token(a, "NFTX", 0);
        }

        @Override
        public BigInteger riserva(String asset) {
            return asset.equals(USDC) ? BigInteger.ZERO : asset.equals(EURC) ? BigInteger.valueOf(3) : BigInteger.valueOf(18);
        }

        @Override
        public long tempo(long blocco) {
            return blocco;
        }
    };

    private static Trans_EtherFi.Ricostruzione ricostruisci(List<JsonObject> logs, List<String> avvisi) throws Exception {
        return Trans_EtherFi.Ricostruisci(W, logs, CFG, FONTE, avvisi);
    }

    /** Una ricarica di {@code v} USDC nella transazione {@code tx}: TopUp, arrivo nel Safe, deposito nel collaterale. */
    private List<JsonObject> ricarica(String tx, long b, long v, long quote) {
        List<JsonObject> l = new ArrayList<>();
        l.add(transfer(tx, b, USDC, TOPUP, W, v));
        l.add(transfer(tx, b, USDC, W, LG, v));
        l.add(spoke(tx, b, true, 0, quote, v));
        l.add(supplied(tx, b, USDC, v));
        l.add(log(tx, b, TOPUP, dati(hash(0xabc), 42161, v), Trans_EtherFi.TOPIC_TOPUP, hash(1), topic(W), topic(USDC)));
        return l;
    }

    // ------------------------------------------------------------------
    // Costanti
    // ------------------------------------------------------------------

    @Test
    void iTopicSonoQuelliDelleFirme() {
        assertEquals(keccak("Transfer(address,address,uint256)"), Trans_EtherFi.TOPIC_TRANSFER);
        assertEquals(keccak("TransferSingle(address,address,address,uint256,uint256)"), Trans_EtherFi.TOPIC_TRANSFER_SINGLE);
        assertEquals(keccak("TransferBatch(address,address,address,uint256[],uint256[])"), Trans_EtherFi.TOPIC_TRANSFER_BATCH);
        assertEquals(keccak("Supplied(address,address,uint256)"), Trans_EtherFi.TOPIC_SUPPLIED);
        assertEquals(keccak("Withdrawn(address,address,uint256,address)"), Trans_EtherFi.TOPIC_WITHDRAWN);
        assertEquals(keccak("Supply(uint256,address,address,uint256,uint256)"), Trans_EtherFi.TOPIC_SUPPLY);
        assertEquals(keccak("Withdraw(uint256,address,address,uint256,uint256)"), Trans_EtherFi.TOPIC_WITHDRAW);
        assertEquals(keccak("TopUp(bytes32,address,address,bytes32,uint256,uint256)"), Trans_EtherFi.TOPIC_TOPUP);
        assertEquals(keccak("Spend(address,bytes32,uint8,address[],uint256[],uint256[],uint256,uint8)"), Trans_EtherFi.TOPIC_SPEND);
        assertEquals(keccak("isEtherFiSafe(address)").substring(2, 10), Trans_EtherFi.SEL_IS_ETHERFI_SAFE);
        assertEquals(keccak("suppliedOf(address,address)").substring(2, 10), Trans_EtherFi.SEL_SUPPLIED_OF);
        assertEquals(keccak("debtOf(address,address)").substring(2, 10), Trans_EtherFi.SEL_DEBT_OF);
        assertEquals(keccak("reserveIdOf(address)").substring(2, 10), Trans_EtherFi.SEL_RESERVE_ID_OF);
    }

    @Test
    void laConfigurazioneSiLeggeDalJar() {
        Trans_EtherFi.Config c = Trans_EtherFi.Configurazione();
        assertNotNull(c, "config/varie/EtherFi.json deve essere leggibile (copia nel jar)");
        assertFalse(c.nodi().isEmpty());
        assertTrue(c.nodi().get(0).startsWith("https://"));
        assertEquals("0x01f8cdfb1694ea8fe4ed6c38a0fd78d1188e03f4", c.lendGateway());
        assertTrue(c.bloccoInizio() > 0 && c.bloccoInizio() < c.bloccoLendGateway());
        assertEquals("Midas", c.moduliScambio().get("0x1052fdce6cbe3d2ad265050e1426d41e23fc54dc"));
    }

    // ------------------------------------------------------------------
    // Classificazione
    // ------------------------------------------------------------------

    @Test
    void unaRicaricaEUnDepositoConLaTransazioneDOrigineInCausale() throws Exception {
        List<String> avvisi = new ArrayList<>();
        Trans_EtherFi.Ricostruzione r = ricostruisci(ricarica(hash(10), 100, 10_500_000, 10_000_000), avvisi);
        assertEquals(1, r.transazioni().size());
        List<Trans_EtherFi.Movimento> m = r.transazioni().get(0).movimenti();
        assertEquals(1, m.size());
        assertEquals(USDC, m.get(0).entrata().indirizzo());
        assertEquals(BigInteger.valueOf(10_500_000), m.get(0).qtaEntrata());
        assertNull(m.get(0).uscita());
        assertTrue(m.get(0).causale().startsWith(Trans_EtherFi.CAUSALE_RICARICA), m.get(0).causale());
        assertTrue(m.get(0).causale().contains(hash(0xabc)), m.get(0).causale());
        assertTrue(m.get(0).causale().contains("ARB"), m.get(0).causale());
        assertEquals(BigInteger.valueOf(10_500_000), r.saldi().get(USDC));
        assertEquals(List.of(), avvisi);
    }

    @Test
    void unDepositoMidasEUnoScambioEGliInteressiVengonoPrimaDelMovimento() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 100_000_000, 100_000_000));
        //Più tardi la quota vale 1,1: 100 USDC prelevati sono 90,909090 quote, e le 100 quote valgono 110
        String tx = hash(11);
        logs.add(spoke(tx, 200, false, 0, 90_909_090, 100_000_000));
        logs.add(withdrawn(tx, 200, USDC, 100_000_000, W));
        logs.add(transfer(tx, 200, USDC, LG, W, 100_000_000));
        logs.add(transfer(tx, 200, USDC, W, ESTERNO, 100_000_000));
        logs.add(transfer(tx, 200, VAULT, ZERO, W, 97_500_000));
        logs.add(transfer(tx, 200, VAULT, W, LG, 97_500_000));
        logs.add(supplied(tx, 200, VAULT, 97_500_000));
        logs.add(log(tx, 200, MIDAS, dati(100_000_000, 97_500_000), keccak("Deposit(address,address,uint256,address,uint256)"),
                topic(W), topic(USDC), topic(VAULT)));
        List<String> avvisi = new ArrayList<>();
        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, avvisi);
        List<Trans_EtherFi.Movimento> m = r.transazioni().get(1).movimenti();
        assertEquals(2, m.size());
        assertEquals("REWARD", m.get(0).tipo());
        //100 quote a 1,1000001 (100.000.000 / 90.909.090) meno i 100 già contati
        assertEquals(BigInteger.valueOf(10_000_001), m.get(0).qtaEntrata());
        assertNull(m.get(1).tipo());
        assertEquals(USDC, m.get(1).uscita().indirizzo());
        assertEquals(VAULT, m.get(1).entrata().indirizzo());
        assertEquals(BigInteger.valueOf(97_500_000), m.get(1).qtaEntrata());
        assertEquals(Trans_EtherFi.CAUSALE_SCAMBIO + " Midas", m.get(1).causale());
        assertEquals(ESTERNO, m.get(1).controparte(), "la controparte è chi riceve, non il LendGateway");
        assertEquals(BigInteger.valueOf(10_000_001), r.saldi().get(USDC));
        assertEquals(List.of(), avvisi);
    }

    @Test
    void loSwapInDueTempiDiventaUnoScambioSoloAllEsecuzione() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 223_500_000, 223_500_000));
        String richiesta = hash(12), esecuzione = hash(13);
        logs.add(spoke(richiesta, 200, false, 0, 223_500_000, 223_500_000));
        logs.add(withdrawn(richiesta, 200, USDC, 223_500_000, W));
        logs.add(transfer(richiesta, 200, USDC, LG, W, 223_500_000));
        logs.add(log(richiesta, 200, SWAP, dati(USDC, 223_500_000), keccak("SwapRequested(address,bytes32,address,uint256,uint256,address,address,uint256,uint256)"),
                topic(W), hash(77)));
        logs.add(transfer(esecuzione, 210, USDC, W, ESTERNO, 223_500_000));
        logs.add(transfer(esecuzione, 210, EURC, ESTERNO, W, 199_961_043));
        logs.add(transfer(esecuzione, 210, EURC, W, LG, 199_961_043));
        logs.add(spoke(esecuzione, 210, true, 3, 199_954_759, 199_961_043));
        logs.add(supplied(esecuzione, 210, EURC, 199_961_043));
        logs.add(log(esecuzione, 210, SWAP, dati(10, 199_000_000), keccak("SwapExecuted(address,bytes32,uint256,address,uint256)"),
                topic(W), hash(77), topic(EURC)));
        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        assertEquals(2, r.transazioni().size(), "la richiesta non produce movimenti");
        List<Trans_EtherFi.Movimento> m = r.transazioni().get(1).movimenti();
        assertEquals(1, m.size());
        assertEquals(USDC, m.get(0).uscita().indirizzo());
        assertEquals(BigInteger.valueOf(223_500_000), m.get(0).qtaUscita());
        assertEquals(EURC, m.get(0).entrata().indirizzo());
        assertEquals(BigInteger.valueOf(199_961_043), m.get(0).qtaEntrata());
        assertEquals(Trans_EtherFi.CAUSALE_SCAMBIO + " Swap", m.get(0).causale());
        assertEquals(BigInteger.ZERO, r.saldi().get(USDC));
    }

    /** Il primo movimento della transazione con quell'hash (le transazioni a netto zero non producono righe). */
    private static Trans_EtherFi.Movimento di(Trans_EtherFi.Ricostruzione r, String hash) {
        for (Trans_EtherFi.Transazione x : r.transazioni()) if (x.hash().equals(hash)) return x.movimenti().get(0);
        fail("nessuna riga per " + hash);
        return null;
    }

    @Test
    void ilRiscattoMidasPortaLaStessaChiaveNellaRichiestaENelPagamento() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 50_000_000, 50_000_000));
        //quote Midas (VAULT) nel conto, poi la richiesta di riscatto (numero 7) le manda al vault
        logs.add(transfer(hash(40), 120, VAULT, ESTERNO, W, 20_000_000));
        logs.add(transfer(hash(40), 120, VAULT, W, LG, 20_000_000));
        logs.add(supplied(hash(40), 120, VAULT, 20_000_000));
        String richiesta = hash(41), pagamento = hash(42);
        logs.add(withdrawn(richiesta, 200, VAULT, 20_000_000, W));
        logs.add(transfer(richiesta, 200, VAULT, LG, W, 20_000_000));
        logs.add(transfer(richiesta, 200, VAULT, W, VAULT_MIDAS, 20_000_000));
        logs.add(log(richiesta, 200, VAULT_MIDAS, dati(W, 20_000_000, 0), Trans_EtherFi.TOPIC_MIDAS_RICHIESTA, num(7), topic(W), topic(EURC)));
        //il pagamento arriva da un altro contratto, e l'approvazione nomina solo il numero della richiesta (altre 2 sono di altri utenti)
        logs.add(transfer(pagamento, 400, EURC, ESTERNO, W, 20_500_000));
        logs.add(log(pagamento, 400, VAULT_MIDAS, dati(1_025_000_000_000_000_000L), Trans_EtherFi.TOPIC_MIDAS_APPROVATA, num(7)));
        logs.add(log(pagamento, 400, VAULT_MIDAS, dati(1_025_000_000_000_000_000L), Trans_EtherFi.TOPIC_MIDAS_APPROVATA, num(8)));

        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        Trans_EtherFi.Movimento rich = di(r, richiesta);
        Trans_EtherFi.Movimento pag = di(r, pagamento);

        String chiave = ", contratto " + VAULT_MIDAS + ", n. 7";
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + "Midas" + chiave, rich.causale());
        assertEquals(VAULT, rich.uscita().indirizzo());
        assertEquals(BigInteger.valueOf(20_000_000), rich.qtaUscita());
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + "Midas" + chiave, pag.causale());
        assertEquals(EURC, pag.entrata().indirizzo());
        assertEquals(BigInteger.valueOf(20_500_000), pag.qtaEntrata());
        //Il saldo del conto tiene conto della richiesta (le quote non ci sono piu') e del pagamento
        assertEquals(BigInteger.ZERO, r.saldi().get(VAULT));
        assertEquals(BigInteger.valueOf(20_500_000), r.saldi().get(EURC));
    }

    @Test
    void leApprovazioniMidasSiCercanoSulVaultPerNumeroDiRichiesta() throws Exception {
        String richiesta = hash(41), pagamento = hash(42);
        List<JsonObject> logs = new ArrayList<>();
        logs.add(log(richiesta, 200, VAULT_MIDAS, dati(W, 20_000_000, 0), Trans_EtherFi.TOPIC_MIDAS_RICHIESTA, num(7), topic(W), topic(EURC)));
        //una richiesta di un altro utente, che non va cercata
        logs.add(log(hash(43), 201, VAULT_MIDAS, dati(ESTERNO, 1, 0), Trans_EtherFi.TOPIC_MIDAS_RICHIESTA, num(6), topic(ESTERNO), topic(EURC)));
        List<JsonObject> richieste = new ArrayList<>();
        OKX_WalletCarta.Rpc rpc = (metodo, parametri) -> {
            assertEquals("eth_getLogs", metodo);
            JsonObject filtro = parametri.get(0).getAsJsonObject();
            richieste.add(filtro);
            JsonArray r = new JsonArray();
            if (filtro.get("address").getAsString().equals(VAULT_MIDAS)
                    && filtro.getAsJsonArray("topics").get(1).getAsString().equals(num(7))) {
                r.add(log(pagamento, 400, VAULT_MIDAS, dati(1_025_000_000_000_000_000L), Trans_EtherFi.TOPIC_MIDAS_APPROVATA, num(7)));
            }
            return r;
        };

        List<JsonObject> tutti = Trans_EtherFi.ConApprovazioniMidas(W, logs, 1000, rpc, null, 0, null);

        assertEquals(1, richieste.size(), "una ricerca per ogni richiesta del Safe, nessuna per quelle degli altri");
        assertEquals(num(7), richieste.get(0).getAsJsonArray("topics").get(1).getAsString());
        assertEquals(3, tutti.size());
        assertEquals(Trans_EtherFi.TOPIC_MIDAS_APPROVATA, tutti.get(2).getAsJsonArray("topics").get(0).getAsString());
        //senza richieste del Safe nessuna ricerca
        richieste.clear();
        assertEquals(1, Trans_EtherFi.ConApprovazioniMidas(W, logs.subList(1, 2), 1000, rpc, null, 0, null).size());
        assertTrue(richieste.isEmpty());
    }

    @Test
    void laCodaDiLiquidDistingueRichiestaPagamentoEAnnullamento() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 50_000_000, 50_000_000));
        logs.add(transfer(hash(50), 120, VAULT, ESTERNO, W, 30_000_000));
        logs.add(transfer(hash(50), 120, VAULT, W, LG, 30_000_000));
        logs.add(supplied(hash(50), 120, VAULT, 30_000_000));
        //richiesta 1: poi pagata in USDC
        String r1 = hash(51), p1 = hash(52);
        logs.add(withdrawn(r1, 200, VAULT, 10_000_000, W));
        logs.add(transfer(r1, 200, VAULT, LG, W, 10_000_000));
        logs.add(transfer(r1, 200, VAULT, W, CODA_LIQUID, 10_000_000));
        logs.add(log(r1, 200, CODA_LIQUID, dati(0, 10_000_000, 12_000_000, 1, 3600, 259200), Trans_EtherFi.TOPIC_LIQUID_RICHIESTA,
                hash(500), topic(W), topic(USDC)));
        logs.add(transfer(p1, 300, USDC, CODA_LIQUID, W, 12_000_000));
        logs.add(log(p1, 300, CODA_LIQUID, dati(1), Trans_EtherFi.TOPIC_LIQUID_RISOLTA, hash(500), topic(W)));
        //richiesta 2: annullata, le quote tornano
        String r2 = hash(53), a2 = hash(54);
        logs.add(withdrawn(r2, 400, VAULT, 5_000_000, W));
        logs.add(transfer(r2, 400, VAULT, LG, W, 5_000_000));
        logs.add(transfer(r2, 400, VAULT, W, CODA_LIQUID, 5_000_000));
        logs.add(log(r2, 400, CODA_LIQUID, dati(0, 5_000_000, 6_000_000, 1, 3600, 259200), Trans_EtherFi.TOPIC_LIQUID_RICHIESTA,
                hash(501), topic(W), topic(USDC)));
        logs.add(transfer(a2, 500, VAULT, CODA_LIQUID, W, 5_000_000));
        logs.add(log(a2, 500, CODA_LIQUID, dati(1), Trans_EtherFi.TOPIC_LIQUID_ANNULLATA, hash(501), topic(W)));

        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        String n500 = Trans_EtherFi.NumeroRichiesta(hash(500));
        String n501 = Trans_EtherFi.NumeroRichiesta(hash(501));
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + "EtherFi Liquid, contratto " + CODA_LIQUID + ", n. " + n500,
                di(r, r1).causale());
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_PAGATO + "EtherFi Liquid, contratto " + CODA_LIQUID + ", n. " + n500,
                di(r, p1).causale());
        assertEquals(USDC, di(r, p1).entrata().indirizzo());
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_RICHIESTA + "EtherFi Liquid, contratto " + CODA_LIQUID + ", n. " + n501,
                di(r, r2).causale());
        Trans_EtherFi.Movimento annullata = di(r, a2);
        assertEquals(Trans_EtherFi.CAUSALE_RISCATTO_ANNULLATO + "EtherFi Liquid, contratto " + CODA_LIQUID + ", n. " + n501, annullata.causale());
        assertEquals(VAULT, annullata.entrata().indirizzo());
    }

    @Test
    void unoSwapVersoUnAltraCatenaEsceComePrelievoConIlDestinatarioInCausale() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 300_000_000, 300_000_000));
        String richiesta = hash(60), esecuzione = hash(61);
        logs.add(withdrawn(richiesta, 200, USDC, 240_000_000, W));
        logs.add(transfer(richiesta, 200, USDC, LG, W, 240_000_000));
        //srcToken, srcAmount, catena di destinazione (1 = Ethereum), dstToken, destinatario, minOut, scadenza
        logs.add(log(richiesta, 200, SWAP, dati(USDC, 240_000_000, 1, WBTC_ETH, TRADING_SAFE, 295_000, 1_800_000_000L),
                Trans_EtherFi.TOPIC_SWAP_RICHIESTO, topic(W), hash(78)));
        logs.add(transfer(esecuzione, 210, USDC, W, ESTERNO, 240_000_000));
        logs.add(log(esecuzione, 210, SWAP, dati(1, 295_000), Trans_EtherFi.TOPIC_SWAP_ESEGUITO, topic(W), hash(78), topic(WBTC_ETH)));

        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        Trans_EtherFi.Movimento m = di(r, esecuzione);
        assertNull(m.entrata(), "la moneta ricevuta non arriva su questa catena");
        assertEquals(USDC, m.uscita().indirizzo());
        assertEquals(BigInteger.valueOf(240_000_000), m.qtaUscita());
        assertEquals(TRADING_SAFE, m.controparte());
        assertEquals(Trans_EtherFi.CAUSALE_SWAP_FRA_CATENE + " verso ETH, destinatario " + TRADING_SAFE + ", token " + WBTC_ETH
                + ", almeno 295000 unita', scadenza 1800000000", m.causale());
        assertEquals(BigInteger.valueOf(60_000_000), r.saldi().get(USDC));
    }

    @Test
    void unaSpesaConLaCartaEUnCashoutVersoIlRegolamento() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 50_000_000, 50_000_000));
        String tx = hash(14);
        logs.add(spoke(tx, 300, false, 0, 34_134_965, 34_134_965));
        logs.add(withdrawn(tx, 300, USDC, 34_134_965, REGOLAMENTO));
        //Spend: tokens[], amounts[], amountInUsd[] (offset 0xa0, 0xe0, 0x120), totalUsdAmt, mode
        logs.add(log(tx, 300, EMITTER, dati(0xa0, 0xe0, 0x120, 39_176_904, 1, 1, USDC, 1, 34_134_965, 1, 39_176_904),
                Trans_EtherFi.TOPIC_SPEND, topic(W), hash(99), num(1)));
        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        List<Trans_EtherFi.Movimento> m = r.transazioni().get(1).movimenti();
        assertEquals(1, m.size());
        assertEquals("CASHOUT O SIMILARE", m.get(0).tipo());
        assertEquals(BigInteger.valueOf(34_134_965), m.get(0).qtaUscita());
        assertEquals(REGOLAMENTO, m.get(0).controparte());
        assertTrue(m.get(0).causale().contains("USD 39.176904"), m.get(0).causale());
        assertEquals(BigInteger.valueOf(15_865_035), r.saldi().get(USDC));
    }

    @Test
    void gliNftArrivatiSenzaUnOperazioneEtherFiSonoScam() throws Exception {
        List<JsonObject> logs = new ArrayList<>();
        logs.add(log(hash(20), 400, NFT, dati(5, 1), Trans_EtherFi.TOPIC_TRANSFER_SINGLE, topic(ESTERNO), topic(ZERO), topic(W)));
        String conEtherFi = hash(21);
        logs.add(log(conEtherFi, 401, NFT, dati(6, 1), Trans_EtherFi.TOPIC_TRANSFER_SINGLE, topic(ESTERNO), topic(ZERO), topic(W)));
        logs.add(log(conEtherFi, 401, EMITTER, dati(0), keccak("Qualcosa(address)"), topic(W)));
        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        assertEquals(2, r.transazioni().size());
        Trans_EtherFi.Movimento spam = r.transazioni().get(0).movimenti().get(0);
        assertTrue(spam.nft());
        assertTrue(spam.scam());
        assertEquals(BigInteger.ONE, spam.qtaEntrata());
        assertFalse(r.transazioni().get(1).movimenti().get(0).scam());
        assertTrue(r.saldi().isEmpty(), "gli NFT non entrano nei saldi verificati");
    }

    @Test
    void unaVariazioneSenzaEventiNotiNonSiPerde() throws Exception {
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 100, 20_000_000, 20_000_000));
        //Prelievo verso l'esterno senza nessun evento conosciuto: dal Safe esce USDC
        String tx = hash(30);
        logs.add(transfer(tx, 500, USDC, W, ESTERNO, 5_000_000));
        Trans_EtherFi.Ricostruzione r = ricostruisci(logs, new ArrayList<>());
        Trans_EtherFi.Movimento m = r.transazioni().get(1).movimenti().get(0);
        assertEquals("Prelievo", m.causale());
        assertEquals(BigInteger.valueOf(5_000_000), m.qtaUscita());
        assertEquals(ESTERNO, m.controparte());
    }

    @Test
    void ilSaldoTornaSeLeQuoteCoincidonoEIlContoHaInPiuSoloInteressi() {
        BigInteger ric = BigInteger.valueOf(110_000_000);
        assertNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(110_000_000), null));
        assertNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(110_000_001), null));
        assertNotNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(110_003_407), null), "fuori dallo spoke non ci sono interessi");
        //Quote uguali: qualunque interesse maturato dopo l'ultimo movimento, anche dopo mesi
        assertNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(110_003_407), true));
        assertNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(130_000_000), true));
        assertNotNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(100_000_000), true), "il conto ha meno");
        assertNotNull(Trans_EtherFi.EsitoSaldo(ric, BigInteger.valueOf(110_000_000), false), "quote diverse");
    }

    // ------------------------------------------------------------------
    // Lettura dei log
    // ------------------------------------------------------------------

    /** Nodo finto che rifiuta i tratti oltre {@code limite} blocchi con il messaggio indicato e conta le richieste. */
    private static final class NodoLimitato implements OKX_WalletCarta.Rpc {
        final long limite, creazione;
        final String errore;
        final List<JsonObject> logs;
        int richieste;
        long primoBloccoChiesto = Long.MAX_VALUE;

        NodoLimitato(long limite, String errore, List<JsonObject> logs, long creazione) {
            this.limite = limite;
            this.errore = errore;
            this.logs = logs;
            this.creazione = creazione;
        }

        @Override
        public JsonElement chiama(String metodo, JsonArray par) throws Exception {
            if (metodo.equals("eth_getCode")) {
                long b = Long.decode(par.get(1).getAsString());
                return new JsonPrimitive(b >= creazione ? "0x6080" : "0x");
            }
            richieste++;
            JsonObject f = par.get(0).getAsJsonObject();
            long da = Long.decode(f.get("fromBlock").getAsString()), a = Long.decode(f.get("toBlock").getAsString());
            if (a - da + 1 > limite) throw new Exception(errore);
            primoBloccoChiesto = Math.min(primoBloccoChiesto, da);
            JsonArray tp = f.getAsJsonArray("topics");
            int pos = tp.size() - 1;
            String cercato = tp.get(pos).getAsString();
            JsonArray out = new JsonArray();
            for (JsonObject l : logs) {
                long b = Trans_EtherFi.Blocco(l);
                JsonArray t = l.getAsJsonArray("topics");
                if (b >= da && b <= a && t.size() > pos && t.get(pos).getAsString().equals(cercato)) out.add(l);
            }
            return out;
        }
    }

    @Test
    void ilTrattoSiDivideQuandoIlNodoLoRifiutaEISiTolgonoIDoppioni() throws Exception {
        //Safe creato al blocco 40.000, su una storia letta dal blocco 1 al 50.000
        List<JsonObject> logs = new ArrayList<>(ricarica(hash(10), 41_000, 1_000_000, 1_000_000));
        //Un trasferimento del Safe a se stesso compare in due posizioni: deve restare uno
        logs.add(transfer(hash(11), 45_000, USDC, W, W, 1));
        NodoLimitato tenderly = new NodoLimitato(30_000, "{\"code\":-32602,\"message\":\"invalid params\",\"data\":\"Query returned more than 20000 results\"}", logs, 40_000);
        List<JsonObject> letti = Trans_EtherFi.Log(W, 1, 50_000, tenderly, null, 0, null);
        assertEquals(logs.size(), letti.size());
        assertTrue(tenderly.richieste > 3);

        //Nodo con limite di blocchi: la prima richiesta rifiutata fa cercare la creazione del Safe, e da lì si legge
        //a tratti di 10.000 blocchi, non dal blocco 1
        NodoLimitato ufficiale = new NodoLimitato(10_000, "Block range is too large", logs, 40_000);
        List<JsonObject> letti2 = Trans_EtherFi.Log(W, 1, 50_000, ufficiale, null, 0, null);
        assertEquals(logs.size(), letti2.size());
        assertEquals(40_000, ufficiale.primoBloccoChiesto);
        assertTrue(ufficiale.richieste <= 1 + 3 * 3, "richieste: " + ufficiale.richieste);
        assertEquals(40_000, Trans_EtherFi.Creazione(W, 1, 50_000, ufficiale));
    }

    // ------------------------------------------------------------------
    // Abbinamento delle ricariche
    // ------------------------------------------------------------------

    private static String[] riga(String id) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void laRicaricaSiAbbinaAlPrelievoConLHashDOrigine() {
        String origine = hash(0xabc);
        String[] p = riga("20261024000121_0x9999 (ARB)_001_001_PC");
        p[3] = "0x9999 (ARB)";
        p[8] = "USDC";
        p[10] = "-10.5";
        p[15] = "9.37";
        p[24] = origine.toUpperCase().replace("0X", "0x");
        String[] altro = riga("20261024000121_0x8888 (ARB)_001_001_PC");
        altro[3] = "0x8888 (ARB)";
        altro[8] = "USDC";
        altro[10] = "-10.5";
        altro[24] = hash(0xdef);
        String[] d = riga("20261024000123_" + W + " (OP)_001_001_DC");
        d[3] = W + " (OP)";
        d[7] = Trans_EtherFi.CAUSALE_RICARICA + " da ARB, transazione " + origine;
        d[11] = "USDC";
        d[13] = "10.5";
        d[15] = "9.37";

        assertEquals(1, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertTrue(p[18].startsWith("PTW"), p[18]);
        assertTrue(d[18].startsWith("DTW"), d[18]);
        assertEquals(d[0], p[20]);
        assertEquals(p[0], d[20]);
        assertTrue(altro[18].isBlank());
        //Rifatto non cambia nulla
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
    }

    @Test
    void senzaIlPrelievoDOrigineLaRicaricaRestaDaClassificare() {
        String[] d = riga("20261024000123_" + W + " (OP)_001_001_DC");
        d[3] = W + " (OP)";
        d[7] = Trans_EtherFi.CAUSALE_RICARICA + " da ARB, transazione " + hash(0xabc);
        d[11] = "USDC";
        d[13] = "10.5";
        assertEquals(0, Trans_EtherFi.Abbina(MappaCryptoWallet, Set.of(W)));
        assertTrue(d[18].isBlank());
    }

    // ------------------------------------------------------------------
    // Storia vera di un Safe (nocommit, non nel repository)
    // ------------------------------------------------------------------

    @Test
    void sullaStoriaVeraISaldiTornanoConQuelliDelConto() throws IOException, Exception {
        Assumptions.assumeTrue(Files.isDirectory(DATI_REALI), "dati di prova ether.fi assenti (" + DATI_REALI + ")");
        List<Path> cartelle;
        try (Stream<Path> s = Files.list(DATI_REALI)) {
            cartelle = s.filter(Files::isDirectory).sorted().toList();
        }
        Assumptions.assumeFalse(cartelle.isEmpty());
        Trans_EtherFi.Config cfg = Trans_EtherFi.Configurazione();
        assertNotNull(cfg);
        for (Path c : cartelle) {
            String w = c.getFileName().toString();
            List<JsonObject> logs = new ArrayList<>();
            for (JsonElement el : JsonParser.parseString(Files.readString(c.resolve("logs.json"), StandardCharsets.UTF_8)).getAsJsonArray()) {
                logs.add(el.getAsJsonObject());
            }
            JsonObject fonte = JsonParser.parseString(Files.readString(c.resolve("fonte.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject saldi = JsonParser.parseString(Files.readString(c.resolve("saldi.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Trans_EtherFi.Token> token = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : fonte.getAsJsonObject("token").entrySet()) {
                JsonObject t = e.getValue().getAsJsonObject();
                String s = cfg.simboli().getOrDefault(e.getKey(), t.get("simbolo").getAsString());
                token.put(e.getKey(), new Trans_EtherFi.Token(e.getKey(), s.isBlank() ? e.getKey() : s, t.get("decimali").getAsInt()));
            }
            JsonObject riserve = fonte.getAsJsonObject("riserve");
            Trans_EtherFi.Fonte f = new Trans_EtherFi.Fonte() {
                @Override
                public Trans_EtherFi.Token token(String a) {
                    return token.getOrDefault(a, new Trans_EtherFi.Token(a, a, 0));
                }

                @Override
                public BigInteger riserva(String asset) {
                    return new BigInteger(riserve.get(asset).getAsString());
                }

                @Override
                public long tempo(long blocco) {
                    throw new IllegalStateException("i log salvati portano blockTimestamp");
                }
            };
            List<String> avvisi = new ArrayList<>();
            Trans_EtherFi.Ricostruzione r = Trans_EtherFi.Ricostruisci(w, logs, cfg, f, avvisi);
            assertFalse(r.transazioni().isEmpty(), w);
            JsonObject quote = fonte.getAsJsonObject("quote");
            for (Map.Entry<String, JsonElement> e : saldi.entrySet()) {
                BigInteger vero = new BigInteger(e.getValue().getAsString());
                BigInteger ricostruito = r.saldi().getOrDefault(e.getKey(), BigInteger.ZERO);
                BigInteger q = new BigInteger(quote.get(e.getKey()).getAsString());
                assertEquals(q, r.quote().get(e.getKey()), w + " quote di " + e.getKey());
                assertNull(Trans_EtherFi.EsitoSaldo(ricostruito, vero, true), w + " " + e.getKey() + ": " + ricostruito + " contro " + vero);
            }
            assertEquals(List.of(), avvisi, w);
            for (Trans_EtherFi.Transazione t : r.transazioni()) {
                for (Trans_EtherFi.Movimento m : t.movimenti()) {
                    //Ogni uscita e' spiegata (ricarica, scambio, spesa, richiesta di riscatto, swap fra catene). Un deposito
                    //da un contratto sconosciuto (un bonifico accreditato, una consegna da altra catena) resta possibile
                    assertFalse(m.causale().startsWith("Prelievo"), "uscita non riconosciuta in " + t.hash() + ": " + m.causale());
                }
            }
        }
    }
}
