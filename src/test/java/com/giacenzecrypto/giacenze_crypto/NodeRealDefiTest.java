package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Fissa l'adattatore NodeReal -> formato Etherscan.
 * <p>Qui non si va in rete: il valore di {@code NodeRealDefi} sta tutto nel fatto che le righe che
 * produce vengono parsate dal codice esistente di {@code Importazioni}, quindi cio' che conta e' che i
 * nomi e i formati dei campi siano quelli che quel parser si aspetta. Le conversioni verificate sono
 * esattamente quelle in cui un errore non produrrebbe nessun messaggio: il segno invertito di
 * {@code isError}, i valori esadecimali e il timestamp numerico.
 */
public class NodeRealDefiTest {

    /** Record NodeReal minimo ma realistico, con i tipi veri della risposta (numeri non stringhe). */
    private JSONObject RecordBase() {
        JSONObject R = new JSONObject();
        R.put("category", "20");
        R.put("blockNum", "0x2faf080");
        R.put("from", "0x0d86b90952d65ed56a4124dfe2f77cb508adbe99");
        R.put("to", "0x10ed43c718714eb63d5aa57b78b54704e256024e");
        R.put("value", "0x00000000000000000000000000000000000000000000000006db4058e94abd3f");
        R.put("asset", "WBNB");
        R.put("name", "Wrapped BNB");
        R.put("hash", "0x491ad3caef862e5606a352d971e3bab60adcc8fab4c8f92e85cd9936de3c3640");
        R.put("contractAddress", "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c");
        R.put("decimal", "18");
        R.put("blockTimeStamp", 1747743395L);
        R.put("gasPrice", 1000000000L);
        R.put("gasUsed", 106696L);
        R.put("receiptsStatus", 1);
        return R;
    }

    @Test
    public void leAzioniEtherscanSiTraduconoNelleCategorieNodeReal() {
        assertEquals("external", NodeRealDefi.Categoria("txlist"));
        assertEquals("internal", NodeRealDefi.Categoria("txlistinternal"));
        assertEquals("20", NodeRealDefi.Categoria("tokentx"));
        assertEquals("721", NodeRealDefi.Categoria("tokennfttx"));
        assertEquals("1155", NodeRealDefi.Categoria("token1155tx"));
        assertNull(NodeRealDefi.Categoria("balance"), "un'azione non gestita deve dire di no, non inventare");
        assertNull(NodeRealDefi.Categoria(null));
    }

    @Test
    public void soloBscHaUnEndpointEServeLaChiave() {
        assertTrue(NodeRealDefi.Endpoint("BSC", "kkk").startsWith("https://bsc-mainnet.nodereal.io/v1/"));
        assertTrue(NodeRealDefi.ReteSupportata("bsc"));
        assertNull(NodeRealDefi.Endpoint("ETH", "kkk"));
        assertFalse(NodeRealDefi.ReteSupportata("ETH"));
        //senza chiave non si compone un URL a meta': l'endpoint E' la chiave
        assertNull(NodeRealDefi.Endpoint("BSC", ""));
        assertNull(NodeRealDefi.Endpoint("BSC", null));
    }

    @Test
    public void iValoriEsadecimaliDiventanoDecimali() {
        //il parser fa new BigDecimal(getString("value")): su uno 0x... lancerebbe
        assertEquals("494059334742490431", NodeRealDefi.Decimale(
                "0x00000000000000000000000000000000000000000000000006db4058e94abd3f"));
        assertEquals("50000000", NodeRealDefi.Decimale("0x2faf080"));
        //i numeri JSON arrivano gia' decimali e devono passare inalterati
        assertEquals("106696", NodeRealDefi.Decimale(106696L));
        //campo assente, null o vuoto: "0", mai un'eccezione dentro il ciclo di importazione
        assertEquals("0", NodeRealDefi.Decimale(null));
        assertEquals("0", NodeRealDefi.Decimale(""));
        assertEquals("0", NodeRealDefi.Decimale("0x"));
        assertEquals("0", NodeRealDefi.Decimale("non un numero"));
    }

    @Test
    public void isErrorEInvertitoRispettoAReceiptsStatus() {
        //Etherscan: "0" = riuscita. NodeReal: receiptsStatus 1 = riuscita. Scambiarli marcherebbe come
        //fallita ogni transazione riuscita, e il parser lo legge senza mai segnalare nulla.
        JSONObject Ok = NodeRealDefi.ConvertiRecord(RecordBase(), "txlist").getJSONObject(0);
        assertEquals("0", Ok.getString("isError"));

        JSONObject Fallita = RecordBase();
        Fallita.put("receiptsStatus", 0);
        assertEquals("1", NodeRealDefi.ConvertiRecord(Fallita, "txlist").getJSONObject(0).getString("isError"));

        //campo assente: si assume riuscita, come fa Etherscan quando non lo manda
        JSONObject Senza = RecordBase();
        Senza.remove("receiptsStatus");
        assertEquals("0", NodeRealDefi.ConvertiRecord(Senza, "txlist").getJSONObject(0).getString("isError"));
    }

    @Test
    public void laRigaConvertitaHaINomiDiCampoCheIlParserLegge() {
        JSONObject T = NodeRealDefi.ConvertiRecord(RecordBase(), "tokentx").getJSONObject(0);
        assertEquals("0x491ad3caef862e5606a352d971e3bab60adcc8fab4c8f92e85cd9936de3c3640", T.getString("hash"));
        assertEquals("50000000", T.getString("blockNumber"));
        //timeStamp come STRINGA: il parser fa Long.parseLong(getString("timeStamp")) e getString su un
        //numero lancerebbe
        assertEquals("1747743395", T.getString("timeStamp"));
        assertEquals("494059334742490431", T.getString("value"));
        assertEquals("WBNB", T.getString("tokenSymbol"));
        assertEquals("Wrapped BNB", T.getString("tokenName"));
        assertEquals("18", T.getString("tokenDecimal"));
        assertEquals("0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c", T.getString("contractAddress"));
        assertEquals("106696", T.getString("gasUsed"));
        assertEquals("1000000000", T.getString("gasPrice"));
        //functionName non esiste su NodeReal ed e' letto con optString: deve comunque esserci, vuoto
        assertEquals("", T.getString("functionName"));
    }

    @Test
    public void ilDestinatarioMancanteDiventaStringaVuota() {
        //sulle creazioni di contratto "to" non c'e'; il parser fa getString("to"), che su un campo
        //assente lancerebbe e farebbe saltare l'intera importazione
        JSONObject R = RecordBase();
        R.remove("to");
        JSONObject T = NodeRealDefi.ConvertiRecord(R, "txlist").getJSONObject(0);
        assertEquals("", T.getString("to"));
    }

    @Test
    public void unNftPortaIlTokenIdInDecimaleEQuantitaUno() {
        JSONObject R = RecordBase();
        R.put("category", "721");
        R.put("erc721TokenId", "0x0000000000000000000000000000000000000000000000000000000000004f48");
        JSONObject T = NodeRealDefi.ConvertiRecord(R, "tokennfttx").getJSONObject(0);
        assertEquals("20296", T.getString("tokenID"));
        assertEquals("1", T.getString("value"));
    }

    @Test
    public void unRecord1155DiventaUnaRigaPerOgniTokenId() {
        //Etherscan emette una riga per tokenId, NodeReal raggruppa tutto in erc1155Metadata: senza
        //l'espansione un trasferimento multiplo perderebbe tutti i token tranne il primo
        JSONObject R = RecordBase();
        R.put("category", "1155");
        R.remove("value");
        JSONArray Meta = new JSONArray();
        Meta.put(new JSONObject().put("tokenId", "0x0a").put("value", "0x0f43fc2c04ee0000"));
        Meta.put(new JSONObject().put("tokenId", "0x0b").put("value", "0x02"));
        R.put("erc1155Metadata", Meta);

        JSONArray Righe = NodeRealDefi.ConvertiRecord(R, "token1155tx");
        assertEquals(2, Righe.length());
        assertEquals("10", Righe.getJSONObject(0).getString("tokenID"));
        assertEquals("1100000000000000000", Righe.getJSONObject(0).getString("tokenValue"));
        assertEquals("11", Righe.getJSONObject(1).getString("tokenID"));
        assertEquals("2", Righe.getJSONObject(1).getString("tokenValue"));
        //l'hash e' lo stesso: le due righe appartengono alla stessa transazione
        assertEquals(Righe.getJSONObject(0).getString("hash"), Righe.getJSONObject(1).getString("hash"));
    }

    @Test
    public void unRecordSenzaHashVieneScartato() {
        //senza hash la riga non e' accorpabile a nessuna transazione: tenerla creerebbe un movimento
        //con chiave vuota che ne assorbirebbe altri
        JSONObject R = RecordBase();
        R.remove("hash");
        assertEquals(0, NodeRealDefi.ConvertiRecord(R, "txlist").length());
        assertEquals(0, NodeRealDefi.ConvertiRecord(null, "txlist").length());
    }

    @Test
    public void laPrimaImportazioneNonMandaLaFinestraDiBlocchi() {
        //fromBlock senza toBlock e' rifiutato dal server, e una finestratura da blocco 1 sprecherebbe
        //decine di richieste su intervalli vuoti: alla prima importazione si omettono entrambi e si
        //pagina col solo pageKey
        JSONObject Tutta = NodeRealDefi.ComponiRichiesta("0xabc", "external", true, 1, 0, "");
        JSONObject P = Tutta.getJSONArray("params").getJSONObject(0);
        assertFalse(P.has("fromBlock"));
        assertFalse(P.has("toBlock"));
        assertFalse(P.has("pageKey"));
        assertEquals("nr_getTransactionByAddress", Tutta.getString("method"));
        assertEquals("asc", P.getString("order"));
        assertEquals("0x3e8", P.getString("maxCount"));

        JSONObject Finestra = NodeRealDefi.ComponiRichiesta("0xabc", "20", false, 1000, 2000, "PK");
        JSONObject Q = Finestra.getJSONArray("params").getJSONObject(0);
        assertEquals("0x3e8", Q.getString("maxCount"));
        assertEquals(Long.toHexString(1000), Q.getString("fromBlock").substring(2));
        assertEquals(Long.toHexString(2000), Q.getString("toBlock").substring(2));
        assertEquals("PK", Q.getString("pageKey"));
    }

    @Test
    public void laFinestraRestaSottoIlLimiteDelServer() {
        //il server rifiuta con "range must be less than 2000000" e il confronto e' stretto: 2.000.000
        //esatti vengono rifiutati
        assertTrue(NodeRealDefi.AMPIEZZA_FINESTRA < 2000000);
        //l'ampiezza e' fromBlock..fromBlock+AMPIEZZA compresi, quindi il conto dei blocchi e' +1
        assertTrue(NodeRealDefi.AMPIEZZA_FINESTRA + 1 <= 2000000);
    }
}
