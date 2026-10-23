package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Importazione Algorand ({@link Trans_Algorand}): regole del saldo su transazioni costruite a mano (indirizzi
 * generati, nessun wallet vero nel repository) e, se ci sono, sulle storie complete di wallet pubblici salvate in
 * {@code nocommit/DatiTestChain/ALGO/<indirizzo>/} ({@code conto.json}, {@code transazioni.json}, {@code asset.json}):
 * lì la somma delle operazioni deve dare esattamente i saldi del conto.
 */
class Trans_AlgorandTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/ALGO");

    private static final String W = Indirizzo(1);
    private static final String ALTRO = Indirizzo(2);
    private static final String APP = Indirizzo(3);
    private static final String CHIUSURA = Indirizzo(4);
    private static final long USDC = 31566704L;

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    /** Un indirizzo Algorand valido dalla chiave pubblica fatta di byte {@code n}. */
    private static String Indirizzo(int n) {
        try {
            byte[] chiave = new byte[32];
            Arrays.fill(chiave, (byte) n);
            byte[] h = MessageDigest.getInstance("SHA-512/256").digest(chiave);
            byte[] tutto = new byte[36];
            System.arraycopy(chiave, 0, tutto, 0, 32);
            System.arraycopy(h, h.length - 4, tutto, 32, 4);
            return Base32(tutto);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String Base32(byte[] b) {
        String alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bit = 0;
        for (byte x : b) {
            buffer = (buffer << 8) | (x & 0xff);
            bit += 8;
            while (bit >= 5) {
                bit -= 5;
                sb.append(alfabeto.charAt((buffer >> bit) & 31));
            }
        }
        if (bit > 0) sb.append(alfabeto.charAt((buffer << (5 - bit)) & 31));
        return sb.toString();
    }

    private static JSONObject tx(String id, long round, long offset, String tipo, String mittente, long fee) {
        return new JSONObject().put("id", id).put("confirmed-round", round).put("intra-round-offset", offset)
                .put("round-time", 1_700_000_000L + round).put("tx-type", tipo).put("sender", mittente).put("fee", fee);
    }

    private static JSONObject pagamento(String id, long round, String da, String a, long importo, long fee) {
        return tx(id, round, 0, "pay", da, fee)
                .put("payment-transaction", new JSONObject().put("receiver", a).put("amount", importo));
    }

    private static JSONObject trasferimento(String id, long round, String da, String a, long asset, long importo, long fee) {
        return tx(id, round, 0, "axfer", da, fee)
                .put("asset-transfer-transaction", new JSONObject().put("asset-id", asset).put("receiver", a).put("amount", importo));
    }

    private static Trans_Algorand.Operazione unica(List<JSONObject> transazioni) {
        List<Trans_Algorand.Operazione> op = Trans_Algorand.Analizza(transazioni, W, Map.of());
        assertEquals(1, op.size());
        return op.get(0);
    }

    // ------------------------------------------------------------------
    // Regole del saldo
    // ------------------------------------------------------------------

    @Test
    void unGruppoConTransazioniInterneEUnoScambioSolo() {
        //Swap su un DEX: il wallet manda ALGO alla pool e chiama l'app, che gli manda USDC con una transazione interna
        JSONObject a = pagamento("A", 100, W, APP, 10_000_000, 1000).put("group", "G1");
        JSONObject b = tx("B", 100, 1, "appl", W, 2000).put("group", "G1")
                .put("application-transaction", new JSONObject().put("application-id", 1002541853L))
                .put("inner-txns", new JSONArray().put(trasferimento("", 100, APP, W, USDC, 5_000_000, 0)));
        Trans_Algorand.Operazione o = unica(List.of(b, a));
        assertEquals("A", o.hash(), "l'hash e' quello della prima transazione del gruppo");
        assertEquals(BigInteger.valueOf(-10_000_000), o.variazioni().get(Trans_Algorand.MONETA));
        assertEquals(BigInteger.valueOf(5_000_000), o.variazioni().get(String.valueOf(USDC)));
        assertEquals(BigInteger.valueOf(3000), o.commissione());
        assertEquals("pay, appl 1002541853", o.tipo());
        assertFalse(o.tipo().contains("("), "TransazioneDefi taglia il tipo alla prima parentesi");
    }

    @Test
    void laChiusuraDelContoMandaViaAncheIlResto() {
        JSONObject p = pagamento("C", 5, W, ALTRO, 1_000_000, 1000).put("closing-amount", 3_000_000);
        p.getJSONObject("payment-transaction").put("close-remainder-to", CHIUSURA);
        Trans_Algorand.Operazione o = unica(List.of(p));
        assertEquals(BigInteger.valueOf(-4_000_000), o.variazioni().get(Trans_Algorand.MONETA));

        //Visto dal conto che riceve il resto
        List<Trans_Algorand.Operazione> r = Trans_Algorand.Analizza(List.of(p), CHIUSURA, Map.of());
        assertEquals(BigInteger.valueOf(3_000_000), r.get(0).variazioni().get(Trans_Algorand.MONETA));
        assertEquals(BigInteger.ZERO, r.get(0).commissione());
    }

    @Test
    void laChiusuraDiUnAssetMandaViaIlResto() {
        JSONObject t = trasferimento("D", 6, W, ALTRO, USDC, 0, 1000);
        t.getJSONObject("asset-transfer-transaction").put("close-to", ALTRO).put("close-amount", 250);
        assertEquals(BigInteger.valueOf(-250), unica(List.of(t)).variazioni().get(String.valueOf(USDC)));
    }

    @Test
    void nelClawbackPagaIlContoIndicatoNonChiFirma() {
        JSONObject t = trasferimento("E", 7, ALTRO, ALTRO, USDC, 7, 1000);
        t.getJSONObject("asset-transfer-transaction").put("sender", W);
        Trans_Algorand.Operazione o = unica(List.of(t));
        assertEquals(BigInteger.valueOf(-7), o.variazioni().get(String.valueOf(USDC)));
        assertEquals(BigInteger.ZERO, o.commissione(), "la commissione la paga chi firma");
    }

    @Test
    void lOptInESolaCommissione() {
        Trans_Algorand.Operazione o = unica(List.of(trasferimento("F", 8, W, W, USDC, 0, 1000)));
        assertTrue(o.variazioni().isEmpty(), "nessuna variazione a zero: TransazioneDefi la leggerebbe come entrata");
        assertEquals(BigInteger.valueOf(1000), o.commissione());
    }

    @Test
    void unInvioASeStessiNonMuoveNulla() {
        Trans_Algorand.Operazione o = unica(List.of(pagamento("G", 9, W, W, 5_000_000, 1000)));
        assertTrue(o.variazioni().isEmpty());
        assertEquals(BigInteger.valueOf(1000), o.commissione());
    }

    @Test
    void creazioneEDistruzioneDiUnAsset() {
        BigInteger emissione = new BigInteger("18446744073709551615"); //2^64-1: oltre il long
        JSONObject crea = tx("H", 10, 0, "acfg", W, 1000).put("created-asset-index", 99)
                .put("asset-config-transaction", new JSONObject().put("asset-id", 0)
                        .put("params", new JSONObject().put("creator", W).put("total", emissione).put("decimals", 0)));
        JSONObject distruggi = tx("I", 11, 0, "acfg", W, 1000)
                .put("asset-config-transaction", new JSONObject().put("asset-id", 99));
        Map<Long, Trans_Algorand.Asset> asset = Map.of(99L, new Trans_Algorand.Asset(99, "X", "X", 0, emissione, W));
        List<Trans_Algorand.Operazione> op = Trans_Algorand.Analizza(List.of(crea, distruggi), W, asset);
        assertEquals(emissione, op.get(0).variazioni().get("99"));
        assertEquals(emissione.negate(), op.get(1).variazioni().get("99"));
    }

    @Test
    void unAssetCreatoDaUnApplicazioneNonVaAlWallet() {
        JSONObject crea = tx("J", 12, 0, "acfg", APP, 1000).put("created-asset-index", 98)
                .put("asset-config-transaction", new JSONObject().put("asset-id", 0)
                        .put("params", new JSONObject().put("creator", APP).put("total", 1000)));
        JSONObject chiamata = tx("K", 12, 0, "appl", W, 1000)
                .put("application-transaction", new JSONObject().put("application-id", 5))
                .put("inner-txns", new JSONArray().put(crea));
        assertNull(unica(List.of(chiamata)).variazioni().get("98"));
    }

    @Test
    void leRicompenseDiPartecipazioneSonoAParte() {
        JSONObject p = pagamento("L", 13, ALTRO, W, 2_000_000, 1000).put("receiver-rewards", 1234);
        Trans_Algorand.Operazione o = unica(List.of(p));
        assertEquals(BigInteger.valueOf(2_000_000), o.variazioni().get(Trans_Algorand.MONETA), "le ricompense non entrano nel deposito");
        assertEquals(BigInteger.valueOf(1234), o.ricompense());
        assertEquals(ALTRO, o.controparte());
    }

    @Test
    void unaTransazioneDiAltriSenzaEffettiSiScarta() {
        //L'indexer la restituisce perche' il wallet compare come account dell'applicazione, ma non muove nulla
        JSONObject chiamata = tx("M", 14, 0, "appl", ALTRO, 1000)
                .put("application-transaction", new JSONObject().put("application-id", 5).put("accounts", new JSONArray().put(W)));
        assertTrue(Trans_Algorand.Analizza(List.of(chiamata), W, Map.of()).isEmpty());
    }

    @Test
    void laVerificaDeiSaldiDiceCosaNonTorna() {
        List<Trans_Algorand.Operazione> op = Trans_Algorand.Analizza(List.of(
                pagamento("N", 15, ALTRO, W, 5_000_000, 1000),
                trasferimento("O", 16, ALTRO, W, USDC, 2_000_000, 1000)), W, Map.of());
        JSONObject conto = new JSONObject().put("account", new JSONObject().put("amount-without-pending-rewards", 5_000_000)
                .put("assets", new JSONArray().put(new JSONObject().put("asset-id", USDC).put("amount", 2_000_000))));
        Map<Long, Trans_Algorand.Asset> asset = Map.of(USDC, new Trans_Algorand.Asset(USDC, "USDC", "USDC", 6, BigInteger.TEN, ""));
        assertEquals(List.of(), Trans_Algorand.VerificaSaldi(op, conto, asset));

        conto.getJSONObject("account").put("amount-without-pending-rewards", 6_000_000);
        assertEquals(List.of("ALGO: i movimenti importati danno 5, il saldo sulla blockchain è 6."),
                Trans_Algorand.VerificaSaldi(op, conto, asset));
    }

    // ------------------------------------------------------------------
    // Asset, simboli, indirizzi
    // ------------------------------------------------------------------

    @Test
    void unAssetCancellatoPrendeIDatiDallaCreazione() {
        JSONObject cancellato = new JSONObject().put("index", 77).put("deleted", true)
                .put("params", new JSONObject().put("decimals", 0).put("total", 0));
        JSONObject creazione = tx("P", 1, 0, "acfg", W, 1000).put("created-asset-index", 77)
                .put("asset-config-transaction", new JSONObject().put("asset-id", 0).put("params", new JSONObject()
                        .put("unit-name", "SPORT").put("name", "Sports").put("decimals", 2).put("total", 500).put("creator", W)));
        Trans_Algorand.Asset a = Trans_Algorand.RisolviAsset(77, cancellato, creazione);
        assertEquals("SPORT", a.simbolo());
        assertEquals(2, a.decimali());
        assertEquals(BigInteger.valueOf(500), a.totale());
        assertEquals(W, a.creatore());

        JSONObject interna = tx("Q", 1, 0, "appl", APP, 1000).put("inner-txns", new JSONArray().put(creazione));
        assertSame(creazione, Trans_Algorand.Creazione(interna, 77), "la creazione si cerca anche fra le transazioni interne");
    }

    @Test
    void iSimboliCondivisiPortanoLId() {
        assertEquals("TMPOOL2 (1002585026)", Trans_Algorand.Simbolo(1002585026L, "TMPOOL2", "TinymanPool2.0 STBL-USDC", false));
        assertEquals("TM1POOL (359314135)", Trans_Algorand.Simbolo(359314135L, "TM1POOL", "Tinyman Pool", false));
        assertEquals("ALPHA-Y (3)", Trans_Algorand.Simbolo(3, "ALPHA-Y", "Alpha Market 1 Yes", false));
        assertEquals("ALGO (5)", Trans_Algorand.Simbolo(5, "ALGO", "Finto ALGO", false), "un asset non si confonde con la moneta della rete");
        assertEquals("ASA (6)", Trans_Algorand.Simbolo(6, "", "", false));
        assertEquals("Nome", Trans_Algorand.Simbolo(7, "", "Nome", false));
        assertEquals("USDC", Trans_Algorand.Simbolo(USDC, "USDC", "USDC", false));
        assertEquals("AlgoBlow", Trans_Algorand.Simbolo(8, "AlgoBlow", "Bronze #9", true), "l'id agli NFT lo aggiunge RitornaIDToken");
    }

    @Test
    void indirizziEIdDegliAsset() {
        assertTrue(Trans_Algorand.isIndirizzoConto(W));
        char c = W.charAt(10) == 'A' ? 'B' : 'A';
        assertFalse(Trans_Algorand.isIndirizzoConto(W.substring(0, 10) + c + W.substring(11)), "il controllo deve scartarlo");
        assertFalse(Trans_Algorand.isIndirizzoConto(W.toLowerCase()));
        assertFalse(Trans_Algorand.isIndirizzoConto("0x0000000000000000000000000000000000000001"));
        assertTrue(Trans_Algorand.isIdAsset("31566704"));
        assertTrue(Trans_Algorand.isIdAsset("18446744073709551615"));
        assertFalse(Trans_Algorand.isIdAsset("18446744073709551616"));
        assertFalse(Trans_Algorand.isIdAsset("0"));
        assertFalse(Trans_Algorand.isIdAsset("ALGO"));

        //Il percorso prezzi accetta l'id dell'asset, un nome di wallet no
        assertTrue(Funzioni_WalletDeFi.isValidAddress("31566704", Trans_Algorand.RETE));
        assertFalse(Funzioni_WalletDeFi.isValidAddress(Trans_Algorand.MONETA, Trans_Algorand.RETE));
        assertTrue(Funzioni_WalletDeFi.isValidDefiWallet(W + " (ALGO)"));
        assertFalse(Funzioni_WalletDeFi.isValidDefiWallet("31566704 (ALGO)"));
    }

    @Test
    void importNativoValeSoloPerLeRetiConUnaClasse() {
        assertTrue(Reti.isDefi(Trans_Algorand.RETE), "nel file ALGO e' importNativo");
        String json = "{\"reti\":[{\"id\":\"ALGO\",\"nome\":\"Algorand\",\"importNativo\":true,\"urlTransazione\":\"https://x/{hash}\"},"
                + "{\"id\":\"ZEC\",\"nome\":\"Zcash\",\"importNativo\":true,\"urlTransazione\":\"https://x/{hash}\"}]}";
        Reti.Dati d = Reti.Interpreta(json, "prova");
        assertTrue(d.perId().get("ALGO").defi());
        assertFalse(d.perId().get("ZEC").defi(), "senza una classe d'importazione la rete resta solo riconosciuta");
        assertEquals(Trans_Algorand.PROVIDER, Importazioni.DeFi_ProviderDefault(Trans_Algorand.RETE));
    }

    // ------------------------------------------------------------------
    // Storie complete di wallet pubblici (nocommit, non nel repository)
    // ------------------------------------------------------------------

    @Test
    void sulleStorieRealiLaSommaDaISaldiDelConto() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(DATI_REALI), "dati di prova Algorand assenti (" + DATI_REALI + ")");
        List<Path> cartelle;
        try (Stream<Path> s = Files.list(DATI_REALI)) {
            cartelle = s.filter(Files::isDirectory).sorted().toList();
        }
        Assumptions.assumeFalse(cartelle.isEmpty());
        for (Path c : cartelle) {
            String wallet = c.getFileName().toString();
            JSONObject conto = new JSONObject(Files.readString(c.resolve("conto.json"), StandardCharsets.UTF_8));
            JSONArray tx = new JSONObject(Files.readString(c.resolve("transazioni.json"), StandardCharsets.UTF_8))
                    .getJSONArray("transactions");
            JSONObject datiAsset = new JSONObject(Files.readString(c.resolve("asset.json"), StandardCharsets.UTF_8));
            List<JSONObject> transazioni = new ArrayList<>();
            for (int i = 0; i < tx.length(); i++) transazioni.add(tx.getJSONObject(i));
            Map<Long, Trans_Algorand.Asset> asset = new HashMap<>();
            for (String k : datiAsset.keySet()) {
                JSONObject a = datiAsset.optJSONObject(k);
                asset.put(Long.parseLong(k), Trans_Algorand.RisolviAsset(Long.parseLong(k), a,
                        a == null ? null : a.optJSONObject("creazione")));
            }
            List<Trans_Algorand.Operazione> op = Trans_Algorand.Analizza(transazioni, wallet, asset);
            assertFalse(op.isEmpty(), wallet);
            assertEquals(List.of(), Trans_Algorand.VerificaSaldi(op, conto, asset), "saldi del wallet " + wallet);
            for (Trans_Algorand.Asset a : asset.values()) {
                assertFalse(a.simbolo().isBlank(), "asset " + a.id() + " senza simbolo");
            }
        }
    }
}
