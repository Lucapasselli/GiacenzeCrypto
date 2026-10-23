package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Importazione XRP Ledger ({@link Trans_XRPL}): variazioni ricavate dai metadati su transazioni costruite a mano
 * (indirizzi generati, nessun wallet vero nel repository) e, se ci sono, sulle storie complete di wallet pubblici in
 * {@code nocommit/DatiTestChain/XRP/<indirizzo>/} ({@code conto.json} con conto, linee e NFT, {@code transazioni.json}):
 * lì la somma deve dare esattamente saldo XRP, linee di fiducia ed elenco degli NFT.
 */
class Trans_XRPLTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/XRP");
    private static final String ALFABETO = "rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz";

    private static final String W = Indirizzo(1);
    private static final String ALTRO = Indirizzo(2);
    private static final String EMITTENTE = Indirizzo(3);
    private static final String FALSO = Indirizzo(4);
    private static final String SOLO_HEX = "534F4C4F00000000000000000000000000000000";

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    /** Un indirizzo XRP Ledger valido dall'id di conto fatto di byte {@code n}. */
    private static String Indirizzo(int n) {
        try {
            byte[] dati = new byte[21];
            Arrays.fill(dati, 1, 21, (byte) n);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] h = sha.digest(sha.digest(dati));
            byte[] tutto = Arrays.copyOf(dati, 25);
            System.arraycopy(h, 0, tutto, 21, 4);
            BigInteger x = new BigInteger(1, tutto);
            StringBuilder sb = new StringBuilder();
            while (x.signum() > 0) {
                BigInteger[] qr = x.divideAndRemainder(BigInteger.valueOf(58));
                sb.append(ALFABETO.charAt(qr[1].intValue()));
                x = qr[0];
            }
            for (byte b : tutto) {
                if (b != 0) break;
                sb.append(ALFABETO.charAt(0));
            }
            return sb.reverse().toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static JSONObject voce(String hash, long ledger, String tipo, String firma, long feeDrops, String esito, JSONObject... nodi) {
        JSONObject tx = new JSONObject().put("hash", hash).put("ledger_index", ledger).put("date", 800_000_000L)
                .put("TransactionType", tipo).put("Account", firma).put("Fee", String.valueOf(feeDrops));
        JSONArray an = new JSONArray();
        for (JSONObject n : nodi) an.put(n);
        return new JSONObject().put("tx", tx).put("meta", new JSONObject().put("TransactionResult", esito)
                .put("TransactionIndex", 0).put("AffectedNodes", an));
    }

    private static JSONObject conto(String tipo, String account, String prima, String dopo) {
        JSONObject n = new JSONObject().put("LedgerEntryType", "AccountRoot")
                .put(tipo.equals("CreatedNode") ? "NewFields" : "FinalFields", new JSONObject().put("Account", account).put("Balance", dopo));
        if (prima != null) n.put("PreviousFields", new JSONObject().put("Balance", prima));
        return new JSONObject().put(tipo, n);
    }

    private static JSONObject linea(String tipo, String basso, String alto, String valuta, String prima, String dopo) {
        JSONObject fin = new JSONObject().put("LowLimit", new JSONObject().put("issuer", basso))
                .put("HighLimit", new JSONObject().put("issuer", alto))
                .put("Balance", new JSONObject().put("currency", valuta).put("value", dopo));
        JSONObject n = new JSONObject().put("LedgerEntryType", "RippleState").put(tipo.equals("CreatedNode") ? "NewFields" : "FinalFields", fin);
        if (prima != null) n.put("PreviousFields", new JSONObject().put("Balance", new JSONObject().put("currency", valuta).put("value", prima)));
        return new JSONObject().put(tipo, n);
    }

    private static Trans_XRPL.Operazione unica(JSONObject... voci) {
        List<Trans_XRPL.Operazione> op = Trans_XRPL.Analizza(List.of(voci), W, new ArrayList<>());
        assertEquals(1, op.size());
        return op.get(0);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    // ------------------------------------------------------------------
    // Variazioni dai metadati
    // ------------------------------------------------------------------

    @Test
    void unPagamentoInUscitaTogliePoiLaCommissione() {
        JSONObject v = voce("A", 10, "Payment", W, 12, "tesSUCCESS",
                conto("ModifiedNode", W, "100000000", "89999988"), conto("ModifiedNode", ALTRO, "0", "10000000"));
        v.getJSONObject("tx").put("Destination", ALTRO);
        Trans_XRPL.Operazione o = unica(v);
        assertEquals(0, bd("-10").compareTo(o.variazioni().get("XRP")), "la commissione non e' nella gamba");
        assertEquals(0, bd("0.000012").compareTo(o.commissione()));
        assertTrue(o.riuscita());
        assertEquals(ALTRO, o.controparte());
    }

    @Test
    void unPagamentoInEntrataNonHaCommissioniPerIlWallet() {
        Trans_XRPL.Operazione o = unica(voce("B", 11, "Payment", ALTRO, 12, "tesSUCCESS",
                conto("ModifiedNode", W, "1000000", "3500000")));
        assertEquals(0, bd("2.5").compareTo(o.variazioni().get("XRP")));
        assertEquals(BigDecimal.ZERO, o.commissione());
    }

    @Test
    void lAttivazioneDelContoEUnDeposito() {
        Trans_XRPL.Operazione o = unica(voce("C", 12, "Payment", ALTRO, 12, "tesSUCCESS", conto("CreatedNode", W, null, "20000000")));
        assertEquals(0, bd("20").compareTo(o.variazioni().get("XRP")));
    }

    @Test
    void ilSaldoDellaLineaSiGiraPerIlContoAlto() {
        //Il wallet e' "alto": un saldo che passa da -5 a -8 (dal punto di vista del basso) vuol dire +3 per lui
        Trans_XRPL.Operazione o = unica(voce("D", 13, "Payment", ALTRO, 12, "tesSUCCESS",
                linea("ModifiedNode", EMITTENTE, W, "USD", "-5", "-8")));
        assertEquals(0, bd("3").compareTo(o.variazioni().get("USD." + EMITTENTE)));
        Trans_XRPL.Operazione b = unica(voce("E", 14, "Payment", ALTRO, 12, "tesSUCCESS",
                linea("ModifiedNode", W, EMITTENTE, "USD", "5", "8")));
        assertEquals(0, bd("3").compareTo(b.variazioni().get("USD." + EMITTENTE)), "e per il conto basso no");
    }

    @Test
    void unOrdineEseguitoDaAltriEUnoScambioSenzaCommissione() {
        //Un'offerta del wallet sul DEX consumata dalla transazione di un altro: XRP escono, SOLO entra
        Trans_XRPL.Operazione o = unica(voce("F", 15, "OfferCreate", ALTRO, 12, "tesSUCCESS",
                conto("ModifiedNode", W, "50000000", "40000000"),
                linea("ModifiedNode", W, EMITTENTE, SOLO_HEX, "0", "120.5")));
        assertEquals(0, bd("-10").compareTo(o.variazioni().get("XRP")));
        assertEquals(0, bd("120.5").compareTo(o.variazioni().get(SOLO_HEX + "." + EMITTENTE)));
        assertEquals(BigDecimal.ZERO, o.commissione());
    }

    @Test
    void unaTransazioneFallitaCostaSoloLaCommissione() {
        Trans_XRPL.Operazione o = unica(voce("G", 16, "OfferCreate", W, 15, "tecUNFUNDED_OFFER",
                conto("ModifiedNode", W, "50000000", "49999985")));
        assertFalse(o.riuscita());
        assertTrue(o.variazioni().isEmpty());
        assertEquals(0, bd("0.000015").compareTo(o.commissione()));
    }

    @Test
    void unaLineaCreataPartedaZero() {
        Trans_XRPL.Operazione o = unica(voce("H", 17, "Payment", ALTRO, 12, "tesSUCCESS",
                linea("CreatedNode", W, EMITTENTE, "EUR", null, "7")));
        assertEquals(0, bd("7").compareTo(o.variazioni().get("EUR." + EMITTENTE)));
    }

    @Test
    void gliNftSiLeggonoDallePagineDelProprietario() {
        String id = "000800001111111111111111111111111111111111111111111111111111ABCD";
        String pagina = Trans_XRPL.IdConto(W) + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        JSONObject nft = new JSONObject().put("NFToken", new JSONObject().put("NFTokenID", id));
        JSONObject entra = new JSONObject().put("ModifiedNode", new JSONObject().put("LedgerEntryType", "NFTokenPage")
                .put("LedgerIndex", pagina).put("FinalFields", new JSONObject().put("NFTokens", new JSONArray().put(nft)))
                .put("PreviousFields", new JSONObject().put("NFTokens", new JSONArray())));
        Trans_XRPL.Operazione o = unica(voce("I", 18, "NFTokenAcceptOffer", W, 12, "tesSUCCESS",
                conto("ModifiedNode", W, "50000000", "39999988"), entra));
        assertEquals(BigDecimal.ONE, o.variazioni().get("NFT:" + id));
        assertEquals(0, bd("-10").compareTo(o.variazioni().get("XRP")));

        //La pagina di un altro conto non conta
        JSONObject altrui = new JSONObject(entra.toString());
        altrui.getJSONObject("ModifiedNode").put("LedgerIndex", Trans_XRPL.IdConto(ALTRO) + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        assertNull(unica(voce("J", 19, "NFTokenAcceptOffer", W, 12, "tesSUCCESS",
                conto("ModifiedNode", W, "50000000", "49999988"), altrui)).variazioni().get("NFT:" + id));
    }

    @Test
    void unaTransazioneSenzaEffettiSulWalletSiScarta() {
        assertTrue(Trans_XRPL.Analizza(List.of(voce("K", 20, "Payment", ALTRO, 12, "tesSUCCESS",
                conto("ModifiedNode", ALTRO, "5", "3"))), W, new ArrayList<>()).isEmpty());
    }

    @Test
    void laVerificaDeiSaldiDiceCosaNonTorna() {
        List<Trans_XRPL.Operazione> op = Trans_XRPL.Analizza(List.of(
                voce("L", 21, "Payment", ALTRO, 12, "tesSUCCESS", conto("CreatedNode", W, null, "20000000")),
                voce("M", 22, "Payment", ALTRO, 12, "tesSUCCESS", linea("CreatedNode", W, EMITTENTE, "USD", null, "4"))), W, new ArrayList<>());
        JSONObject conto = new JSONObject().put("Balance", "20000000");
        JSONArray linee = new JSONArray().put(new JSONObject().put("currency", "USD").put("account", EMITTENTE).put("balance", "4"));
        assertEquals(List.of(), Trans_XRPL.VerificaSaldi(op, conto, linee, new JSONArray(), Map.of()));
        conto.put("Balance", "21000000");
        List<String> avvisi = Trans_XRPL.VerificaSaldi(op, conto, linee, new JSONArray(), Map.of());
        assertEquals(List.of("XRP: i movimenti importati danno 20, il saldo sulla blockchain è 21."), avvisi);
    }

    // ------------------------------------------------------------------
    // Codici, simboli, indirizzi
    // ------------------------------------------------------------------

    @Test
    void iCodiciEsadecimaliDiventanoTesto() {
        assertEquals("SOLO", Trans_XRPL.Codice(SOLO_HEX));
        assertEquals("USD", Trans_XRPL.Codice("USD"));
        String lp = "034317801B08500446BC533B718359BC014D6B37";
        assertEquals(lp, Trans_XRPL.Codice(lp), "un token LP non e' testo");
    }

    @Test
    void lostessoCodiceDiDueEmittentiNonSiMescola() {
        Trans_XRPL.Operazione a = new Trans_XRPL.Operazione("A", 1, 1, "Payment", "", true,
                Map.of("USD." + EMITTENTE, BigDecimal.ONE), BigDecimal.ZERO);
        Trans_XRPL.Operazione b = new Trans_XRPL.Operazione("B", 2, 2, "Payment", "", true,
                Map.of("USD." + FALSO, BigDecimal.ONE, "XRP." + EMITTENTE, BigDecimal.ONE), BigDecimal.ZERO);
        Map<String, String> s = Trans_XRPL.Simboli(List.of(a, b), Map.of(), Map.of());
        assertEquals("USD", s.get("USD." + EMITTENTE), "il primo emittente tiene il codice");
        assertEquals("USD (" + FALSO + ")", s.get("USD." + FALSO));
        assertEquals("XRP (" + EMITTENTE + ")", s.get("XRP." + EMITTENTE), "un token non si confonde con XRP");

        //Se nell'archivio "USD" e' gia' di un altro emittente, anche il primo di questa importazione prende il suffisso
        Map<String, String> t = Trans_XRPL.Simboli(List.of(a), Map.of(), Map.of("USD", "USD." + FALSO));
        assertEquals("USD (" + EMITTENTE + ")", t.get("USD." + EMITTENTE));

        Trans_XRPL.Operazione lp = new Trans_XRPL.Operazione("C", 3, 3, "AMMDeposit", "", true,
                Map.of("03AA." + EMITTENTE, BigDecimal.ONE), BigDecimal.ZERO);
        assertEquals("LP XRP/SOLO", Trans_XRPL.Simboli(List.of(lp), Map.of(EMITTENTE, "LP XRP/SOLO"), Map.of()).get("03AA." + EMITTENTE));
    }

    @Test
    void indirizziETokenEIdDelConto() {
        //Il conto genesi del ledger, pubblico: id noto
        String genesi = "rHb9CJAWyB4rj91VRWn96DkukG4bwdtyTh";
        assertTrue(Trans_XRPL.isIndirizzoConto(genesi));
        assertEquals("B5F762798A53D543A014CAF8B297CFF8F2F937E8", Trans_XRPL.IdConto(genesi));
        assertFalse(Trans_XRPL.isIndirizzoConto("rHb9CJAWyB4rj91VRWn96DkukG4bwdtyTi"), "il controllo deve scartarlo");
        assertTrue(Trans_XRPL.isIndirizzoConto(W));
        assertTrue(Trans_XRPL.isToken(SOLO_HEX + "." + EMITTENTE));
        assertTrue(Trans_XRPL.isToken("USD." + EMITTENTE));
        assertFalse(Trans_XRPL.isToken("XRP." + EMITTENTE));
        assertFalse(Trans_XRPL.isToken("USD.rNonValido"));

        assertTrue(Funzioni_WalletDeFi.isValidAddress("USD." + EMITTENTE, Trans_XRPL.RETE));
        assertFalse(Funzioni_WalletDeFi.isValidAddress(Trans_XRPL.MONETA, Trans_XRPL.RETE));
        //Un NFT si prezza per indirizzo: per simbolo "NFT" troverebbe APENFT sugli exchange
        assertTrue(Funzioni_WalletDeFi.isValidAddress("000800002222222222222222222222222222222222222222222222222222ABCD", Trans_XRPL.RETE));
        assertFalse(Funzioni_WalletDeFi.isValidDefiWallet("000800002222222222222222222222222222222222222222222222222222ABCD (XRP)"));
        assertTrue(Funzioni_WalletDeFi.isValidDefiWallet(W + " (XRP)"));
        assertFalse(Funzioni_WalletDeFi.isValidDefiWallet("USD." + EMITTENTE + " (XRP)"));
        assertTrue(Reti.isDefi(Trans_XRPL.RETE));
        assertEquals(Trans_XRPL.PROVIDER, Importazioni.DeFi_ProviderDefault(Trans_XRPL.RETE));
    }

    // ------------------------------------------------------------------
    // Storie complete di wallet pubblici (nocommit, non nel repository)
    // ------------------------------------------------------------------

    @Test
    void sulleStorieRealiLaSommaDaISaldiDelConto() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(DATI_REALI), "dati di prova XRP assenti (" + DATI_REALI + ")");
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
            List<JSONObject> voci = new ArrayList<>();
            for (int i = 0; i < tx.length(); i++) voci.add(tx.getJSONObject(i));
            List<String> avvisi = new ArrayList<>();
            List<Trans_XRPL.Operazione> op = Trans_XRPL.Analizza(voci, wallet, avvisi);
            assertFalse(op.isEmpty(), wallet);
            assertEquals(List.of(), avvisi);
            Map<String, String> simboli = Trans_XRPL.Simboli(op, Map.of(), Map.of());
            assertEquals(List.of(), Trans_XRPL.VerificaSaldi(op, conto.getJSONObject("account_info").getJSONObject("account_data"),
                    conto.getJSONArray("lines"), conto.optJSONArray("nfts"), simboli), "saldi del wallet " + wallet);
        }
    }
}
