package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Importazione del wallet della carta OKX su X Layer ({@link Trans_XLayer}): classificazione dai log delle ricevute
 * (interessi Aave, pagamento con la carta, deposito, prelievo da Aave) e scansione per stati su un nodo finto.
 * Indirizzi inventati: niente wallet o hash veri dell'utente nei test.
 */
class Trans_XLayerTest {

    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String HOT = "0xbbbb000000000000000000000000000000000002";
    private static final String RACCOGLITORE = "0xcccc000000000000000000000000000000000003";
    private static final String REGOLAMENTO = "0xdddd000000000000000000000000000000000004";
    private static final String POOL = "0xeeee000000000000000000000000000000000005";
    private static final String DEBITO = "0xffff000000000000000000000000000000000006";
    private static final String USDG = "0x4ae46a509f6b1d9056937ba4500cb143933d2dc8";
    private static final String AUSDG = "0x228765a3c18065c923f23a0ccb6c7cefb3ea2223";
    private static final String ZERO = Trans_XLayer.INDIRIZZO_NULLO;

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

    /** Campo data di un log: un indirizzo (stringa) o un numero per parola. */
    private static String dati(Object... parole) {
        StringBuilder sb = new StringBuilder("0x");
        for (Object p : parole) {
            sb.append(Trans_XLayer.Parola(p instanceof String s ? s : Long.toHexString(((Number) p).longValue())));
        }
        return sb.toString();
    }

    private static JsonObject log(String indirizzo, String dati, String... topics) {
        JsonObject l = new JsonObject();
        l.addProperty("address", indirizzo);
        JsonArray t = new JsonArray();
        for (String x : topics) t.add(x);
        l.add("topics", t);
        l.addProperty("data", dati);
        return l;
    }

    private static JsonObject transfer(String token, String da, String a, long valore) {
        return log(token, dati(valore), Trans_XLayer.TOPIC_TRANSFER, topic(da), topic(a));
    }

    private static JsonObject mint(String token, String caller, String perConto, long valore, long interesse) {
        return log(token, dati(valore, interesse, 1_000_000_000L), Trans_XLayer.TOPIC_MINT_ATOKEN, topic(caller), topic(perConto));
    }

    private static List<Trans_XLayer.Movimento> classifica(JsonObject... logs) {
        JsonArray a = new JsonArray();
        for (JsonObject l : logs) a.add(l);
        return Trans_XLayer.Classifica(W, a, new HashMap<>(Trans_XLayer.TOKEN), null, new ArrayList<>());
    }

    // ------------------------------------------------------------------
    // Costanti
    // ------------------------------------------------------------------

    @Test
    void iTopicEISelettoriSonoQuelliDelleFirme() {
        assertEquals(keccak("Mint(address,address,uint256,uint256,uint256)"), Trans_XLayer.TOPIC_MINT_ATOKEN);
        assertEquals(keccak("Burn(address,address,uint256,uint256,uint256)"), Trans_XLayer.TOPIC_BURN_ATOKEN);
        assertEquals(keccak("Claimed(address,address,address,uint256)"), Trans_XLayer.TOPIC_CLAIMED);
        assertEquals(keccak("Transfer(address,address,uint256)"), Trans_XLayer.TOPIC_TRANSFER);
        assertTrue(keccak("getNonce(address,uint192)").startsWith("0x" + Trans_XLayer.SEL_GET_NONCE));
        assertTrue(keccak("balanceOf(address)").startsWith("0x" + Trans_XLayer.SEL_BALANCE_OF));
        assertTrue(keccak("scaledBalanceOf(address)").startsWith("0x" + Trans_XLayer.SEL_SCALED_BALANCE_OF));
        assertTrue(keccak("getEthBalance(address)").startsWith("0x" + Trans_XLayer.SEL_GET_ETH_BALANCE));
        assertTrue(keccak("aggregate3((address,bool,bytes)[])").startsWith("0x" + Trans_XLayer.SEL_AGGREGATE3));
        assertTrue(keccak("symbol()").startsWith("0x" + Trans_XLayer.SEL_SYMBOL));
        assertTrue(keccak("decimals()").startsWith("0x" + Trans_XLayer.SEL_DECIMALS));
    }

    // ------------------------------------------------------------------
    // Classificazione
    // ------------------------------------------------------------------

    @Test
    void ilPagamentoConLaCartaEInteressiPiuCashoutConGliInteressiPrima() {
        //Come nel pagamento reale: Aave accredita gli interessi maturati (Transfer da 0x0 + Mint) e poi l'aToken esce
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(AUSDG, ZERO, W, 34_676),
                mint(AUSDG, W, W, 34_676, 34_676),
                transfer(AUSDG, W, RACCOGLITORE, 4_819_278),
                log(REGOLAMENTO, dati(AUSDG, 4_819_278L), Trans_XLayer.TOPIC_CLAIMED, topic(W), topic(RACCOGLITORE)));
        assertEquals(2, m.size());
        assertEquals("REWARD", m.get(0).tipo());
        assertEquals("aXlrUSDG", m.get(0).entrata().simbolo());
        assertEquals(BigInteger.valueOf(34_676), m.get(0).qtaEntrata());
        assertEquals("CASHOUT O SIMILARE", m.get(1).tipo());
        assertEquals(BigInteger.valueOf(4_819_278), m.get(1).qtaUscita(), "la quantita' spesa, non il netto con gli interessi");
        assertEquals(RACCOGLITORE, m.get(1).controparte());
    }

    @Test
    void ilDepositoSuAaveEUnoScambioSenzaInteressi() {
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(USDG, W, POOL, 111_590_000),
                transfer(AUSDG, ZERO, W, 111_589_999),
                mint(AUSDG, POOL, W, 111_589_999, 0));
        assertEquals(1, m.size());
        assertNull(m.get(0).tipo(), "lo decide creaMovimento dalle gambe");
        assertEquals("USDG", m.get(0).uscita().simbolo());
        assertEquals(BigInteger.valueOf(111_590_000), m.get(0).qtaUscita());
        assertEquals("aXlrUSDG", m.get(0).entrata().simbolo());
        assertEquals(BigInteger.valueOf(111_589_999), m.get(0).qtaEntrata());
    }

    @Test
    void unSecondoDepositoSuAaveSeparaGliInteressiDalCapitale() {
        //Mint di Aave al deposito: value = importo + interessi maturati, e il Transfer da 0x0 e' dello stesso valore
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(USDG, W, POOL, 50_000_000),
                transfer(AUSDG, ZERO, W, 50_120_000),
                mint(AUSDG, POOL, W, 50_120_000, 120_000));
        assertEquals(2, m.size());
        assertEquals("REWARD", m.get(0).tipo());
        assertEquals(BigInteger.valueOf(120_000), m.get(0).qtaEntrata());
        assertEquals(BigInteger.valueOf(50_000_000), m.get(1).qtaEntrata());
    }

    @Test
    void unPrelievoDaAaveConBurnSeparaGliInteressi() {
        //Prelievo di 30 con 0,2 di interessi: Aave brucia 29,8 e lo dichiara nel Burn
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(AUSDG, W, ZERO, 29_800_000),
                log(AUSDG, dati(29_800_000L, 200_000L, 1L), Trans_XLayer.TOPIC_BURN_ATOKEN, topic(W), topic(W)),
                transfer(USDG, POOL, W, 30_000_000));
        assertEquals(2, m.size());
        assertEquals("REWARD", m.get(0).tipo());
        assertEquals(BigInteger.valueOf(200_000), m.get(0).qtaEntrata());
        assertEquals("aXlrUSDG", m.get(1).uscita().simbolo());
        assertEquals(BigInteger.valueOf(30_000_000), m.get(1).qtaUscita());
        assertEquals(BigInteger.valueOf(30_000_000), m.get(1).qtaEntrata());
    }

    @Test
    void unPrelievoConInteressiMaggioriDellImportoArrivaComeMint() {
        //Interessi 0,5 su un prelievo di 0,3: Aave conia la differenza (0,2) invece di bruciare
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(AUSDG, ZERO, W, 200_000),
                mint(AUSDG, W, W, 200_000, 500_000),
                transfer(USDG, POOL, W, 300_000));
        assertEquals(2, m.size());
        assertEquals(BigInteger.valueOf(500_000), m.get(0).qtaEntrata());
        assertEquals(BigInteger.valueOf(300_000), m.get(1).qtaUscita());
        assertEquals("USDG", m.get(1).entrata().simbolo());
    }

    @Test
    void unaRicaricaOUnCashbackSonoUnDepositoConIlMittente() {
        List<Trans_XLayer.Movimento> m = classifica(transfer(USDG, HOT, W, 100_000));
        assertEquals(1, m.size());
        assertNull(m.get(0).tipo());
        assertNull(m.get(0).uscita());
        assertEquals(HOT, m.get(0).controparte());
    }

    @Test
    void ilMintDiUnaMonetaCheNonEUnATokenNonEUnInteresse() {
        //Un token di debito Aave emette lo stesso Mint, ma lì balanceIncrease sono interessi passivi
        List<Trans_XLayer.Movimento> m = classifica(
                log(DEBITO, dati(1_000L, 1_000L, 1L), Trans_XLayer.TOPIC_MINT_ATOKEN, topic(W), topic(W)),
                transfer(USDG, HOT, W, 100_000));
        assertEquals(1, m.size());
        assertNull(m.get(0).tipo());
    }

    @Test
    void aXlrUSDGSiPrezzaComeUSDGMaNonNeEIlRiferimentoESoloDal2026() throws Exception {
        String contenuto = java.nio.file.Files.readString(java.nio.file.Path.of("config/varie/" + AliasPrezziToken.NOME + ".json"));
        AliasPrezziToken.Tabelle t = AliasPrezziToken.Interpreta(contenuto, AliasPrezziToken.NOME);
        assertEquals("USDG", t.alias().get(AUSDG + "_XLAYER"));
        assertEquals("USDG", t.alias().get(USDG + "_XLAYER"));
        assertFalse(t.riferimenti().containsKey(AUSDG + "_XLAYER"), "un aToken non e' mai il riferimento di USDG");
        assertTrue(t.riferimenti().containsKey(USDG + "_XLAYER"));
        assertNotNull(t.dalAlias().get(AUSDG + "_XLAYER"), "voce nuova: dal obbligatorio, gli anni dichiarati non cambiano");
        assertEquals(t.dalAlias().get("0x79a02482a880bce3f13e09da970dc34db4cd24d1_WORLD"), t.dalAlias().get(AUSDG + "_XLAYER"),
                "dal 2026-01-01 come le altre voci aggiunte nel 2026");
    }

    // ------------------------------------------------------------------
    // Multicall e scansione per stati
    // ------------------------------------------------------------------

    /** Risultato ABI di aggregate3 con un uint256 per chiamata. */
    private static String risultatoAggregate3(List<BigInteger> valori) {
        int n = valori.size();
        StringBuilder sb = new StringBuilder("0x").append(Trans_XLayer.Parola("20")).append(Trans_XLayer.Parola(Integer.toHexString(n)));
        int dimElemento = 4 * 32;   //success, offset, lunghezza, valore
        for (int i = 0; i < n; i++) sb.append(Trans_XLayer.Parola(Integer.toHexString(n * 32 + i * dimElemento)));
        for (BigInteger v : valori) {
            sb.append(Trans_XLayer.Parola("1")).append(Trans_XLayer.Parola("40")).append(Trans_XLayer.Parola("20"))
                    .append(Trans_XLayer.Parola(v.toString(16)));
        }
        return sb.toString();
    }

    @Test
    void laDecodificaDiAggregate3LeggeUnValorePerChiamataEVuotoSeMulticallNonEsiste() {
        List<BigInteger> v = List.of(BigInteger.TWO, BigInteger.ZERO, BigInteger.valueOf(111_589_999));
        assertEquals(v, Trans_XLayer.DecodificaAggregate3(risultatoAggregate3(v)));
        assertTrue(Trans_XLayer.DecodificaAggregate3("0x").isEmpty());
        String dati = Trans_XLayer.DatiStato(W);
        assertTrue(dati.startsWith("0x" + Trans_XLayer.SEL_AGGREGATE3));
        assertTrue(dati.contains(Trans_XLayer.SEL_SCALED_BALANCE_OF + Trans_XLayer.Parola(W)), "aToken letto scalato");
    }

    /** Nodo finto: lo stato cambia ai blocchi delle transazioni; un blocco al secondo. */
    private static final class Nodo implements OKX_WalletCarta.Rpc {
        final long ultimo;
        final TreeMap<Long, List<BigInteger>> stati = new TreeMap<>();
        final Map<Long, String> hashAlBlocco = new HashMap<>();
        final Map<String, JsonArray> ricevute = new HashMap<>();
        int chiamate;

        Nodo(long ultimo) {
            this.ultimo = ultimo;
            stati.put(0L, zeri());
        }

        static List<BigInteger> zeri() {
            List<BigInteger> z = new ArrayList<>();
            for (int i = 0; i < Trans_XLayer.TOKEN.size() + 2; i++) z.add(BigInteger.ZERO);
            return z;
        }

        void transazione(long blocco, String hash, int indiceStato, long nuovoValore, JsonObject... logs) {
            List<BigInteger> s = new ArrayList<>(stati.floorEntry(blocco).getValue());
            s.set(indiceStato, BigInteger.valueOf(nuovoValore));
            stati.put(blocco, s);
            hashAlBlocco.put(blocco, hash);
            JsonArray a = new JsonArray();
            for (JsonObject l : logs) a.add(l);
            ricevute.put(hash, a);
        }

        @Override
        public JsonElement chiama(String metodo, JsonArray p) {
            chiamate++;
            switch (metodo) {
                case "eth_blockNumber":
                    return new JsonPrimitive("0x" + Long.toHexString(ultimo));
                case "eth_call": {
                    long b = Long.decode(p.get(1).getAsString());
                    return new JsonPrimitive(b == 0 ? "0x" : risultatoAggregate3(stati.floorEntry(b).getValue()));
                }
                case "eth_getLogs": {
                    JsonObject f = p.get(0).getAsJsonObject();
                    long da = Long.decode(f.get("fromBlock").getAsString()), a = Long.decode(f.get("toBlock").getAsString());
                    assertTrue(a - da < Trans_XLayer.BLOCCHI_PER_RICHIESTA, "intervallo oltre il limite del nodo");
                    JsonArray out = new JsonArray();
                    for (Map.Entry<Long, String> e : hashAlBlocco.entrySet()) {
                        if (e.getKey() < da || e.getKey() > a) continue;
                        JsonObject l = new JsonObject();
                        l.addProperty("blockNumber", "0x" + Long.toHexString(e.getKey()));
                        l.addProperty("transactionHash", e.getValue());
                        out.add(l);
                    }
                    return out;
                }
                case "eth_getTransactionReceipt": {
                    JsonObject r = new JsonObject();
                    r.addProperty("status", "0x1");
                    r.add("logs", ricevute.get(p.get(0).getAsString()));
                    return r;
                }
                case "eth_getBlockByNumber": {
                    JsonObject b = new JsonObject();
                    b.addProperty("timestamp", "0x" + Long.toHexString(1_791_000_000L + Long.decode(p.get(0).getAsString())));
                    return b;
                }
                default:
                    throw new IllegalStateException(metodo);
            }
        }
    }

    @Test
    void laScansionePerStatiTrovaLeTransazioniConPocheRichieste() {
        Nodo nodo = new Nodo(70_000_000L);
        int iUSDG = 1, iAUSDG = Trans_XLayer.TOKEN.size();   //nonce in 0, aXlrUSDG ultimo dei token
        nodo.transazione(40_000_123L, "0x01", iUSDG, 111_590_000, transfer(USDG, HOT, W, 111_590_000));
        nodo.transazione(40_000_300L, "0x02", iAUSDG, 110_416_470,
                transfer(USDG, W, POOL, 111_590_000), transfer(AUSDG, ZERO, W, 111_589_999), mint(AUSDG, POOL, W, 111_589_999, 0));
        //nello stesso blocco anche l'USDG torna a zero: lo stato del 40_000_300 ha entrambi i cambi
        nodo.stati.get(40_000_300L).set(iUSDG, BigInteger.ZERO);
        nodo.transazione(65_432_100L, "0x03", iAUSDG, 105_649_355,
                transfer(AUSDG, ZERO, W, 34_676), mint(AUSDG, W, W, 34_676, 34_676), transfer(AUSDG, W, RACCOGLITORE, 4_819_278),
                log(REGOLAMENTO, dati(AUSDG, 4_819_278L), Trans_XLayer.TOPIC_CLAIMED, topic(W), topic(RACCOGLITORE)));

        Trans_XLayer.Esito e = Trans_XLayer.Scarica(W, 1, nodo, null, 0);
        assertTrue(e.completo(), e.avvisi().toString());
        assertEquals(List.of("0x01", "0x02", "0x03"), e.transazioni().stream().map(Trans_XLayer.Transazione::hash).toList());
        assertEquals(2, e.transazioni().get(2).movimenti().size(), "interessi + pagamento");
        assertEquals(1_791_000_000L + 65_432_100L, e.transazioni().get(2).timestamp());
        assertTrue(nodo.chiamate < 200, "scansione per stati, non a tratti fissi: " + nodo.chiamate + " richieste");

        //Dall'ultimo blocco importato in poi: niente di nuovo con due sole letture di stato
        nodo.chiamate = 0;
        Trans_XLayer.Esito niente = Trans_XLayer.Scarica(W, 65_432_101L, nodo, null, 0);
        assertTrue(niente.transazioni().isEmpty());
        assertEquals(3, nodo.chiamate, "ultimo blocco e due stati");
    }

    @Test
    void ilBloccoDiRipartenzaEIlPiuAltoFraIMovimentiDelWallet() {
        TreeMap<String, String[]> archivio = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String[][] righe = {{W + " (XLAYER)", "72762807"}, {W.toUpperCase().replace("0X", "0x") + " (XLAYER)", "72763840"},
            {W + " (XLAYER)", ""}, {"OKX", "99999999"}, {HOT + " (XLAYER)", "80000000"}};
        int i = 0;
        for (String[] r : righe) {
            String[] v = new String[Importazioni.ColonneTabella];
            java.util.Arrays.fill(v, "");
            v[0] = "2026100909440" + (i++) + "_X_001_001_DC";
            v[3] = r[0];
            v[23] = r[1];
            archivio.put(v[0], v);
        }
        assertEquals(72_763_840L, Trans_XLayer.UltimoBloccoImportato(archivio, W));
        assertEquals(0L, Trans_XLayer.UltimoBloccoImportato(archivio, RACCOGLITORE));
    }

    @Test
    void unaScansioneInterrottaNonECompleta() {
        Nodo nodo = new Nodo(1_000_000L);
        nodo.transazione(500_000L, "0x01", 1, 5, transfer(USDG, HOT, W, 5));
        Trans_XLayer.Esito e = Trans_XLayer.Scarica(W, 1, nodo, () -> true, 0);
        assertFalse(e.completo());
        assertTrue(e.transazioni().isEmpty());
    }

    @Test
    void leRigheDelPagamentoSonoRewardPoiCashoutConLaRewardPrimaNellOrdine() {
        List<Trans_XLayer.Movimento> m = classifica(
                transfer(AUSDG, ZERO, W, 34_676),
                mint(AUSDG, W, W, 34_676, 34_676),
                transfer(AUSDG, W, RACCOGLITORE, 4_819_278),
                log(REGOLAMENTO, dati(AUSDG, 4_819_278L), Trans_XLayer.TOPIC_CLAIMED, topic(W), topic(RACCOGLITORE)));
        Trans_XLayer.Transazione t = new Trans_XLayer.Transazione("0x0abc", 72_762_807L, 1_791_531_843L, m);
        List<String[]> righe = Trans_XLayer.Righe(W, t, "1.00");
        assertEquals(2, righe.size());
        String[] reward = righe.get(0), cashout = righe.get(1);
        assertTrue(reward[0].endsWith("_RW"), reward[0]);
        assertEquals("REWARD", reward[5]);
        assertEquals("aXlrUSDG", reward[11]);
        assertEquals("0.034676", reward[13]);
        assertTrue(cashout[0].endsWith("_PC"), cashout[0]);
        assertEquals("CASHOUT O SIMILARE", cashout[5]);
        assertTrue(cashout[18].startsWith("PCO"), cashout[18]);
        assertEquals("-4.819278", cashout[10]);
        assertEquals(W + " (XLAYER)", cashout[3]);
        assertEquals("72762807", cashout[23]);
        assertEquals(RACCOGLITORE, cashout[30]);
        assertEquals(Trans_XLayer.CAUSALE_CARTA, cashout[7]);
        assertEquals("XLAYER", reward[34]);
        //Stesso secondo: nell'archivio (TreeMap sull'ID) la reward viene prima e il LIFO del cashout la consuma
        TreeMap<String, String[]> archivio = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        archivio.put(cashout[0], cashout);
        archivio.put(reward[0], reward);
        assertSame(reward, archivio.firstEntry().getValue());
        assertTrue(MovimentiCrypto.getIDUnivoco(archivio, reward[0]).compareTo(cashout[0]) < 0,
                "anche reso univoco l'ID della reward resta prima del cashout");
    }
}
