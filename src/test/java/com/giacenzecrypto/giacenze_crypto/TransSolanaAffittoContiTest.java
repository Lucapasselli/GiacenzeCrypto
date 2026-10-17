package com.giacenzecrypto.giacenze_crypto;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L'affitto dei conti token Solana del wallet stesso non deve entrare nella gamba SOL
 * ({@link Trans_Solana#VariazioneSolContiTokenPropri}). Transazioni costruite a mano nel formato "enhanced" di Helius,
 * ricalcate su casi reali (swap Raydium che apre il conto USDC, conto aperto vuoto, invio a un exchange che apre il
 * conto del destinatario, chiusura), con indirizzi inventati.
 */
class TransSolanaAffittoContiTest {

    private static final String TOKEN = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    private static final String ATA = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";
    private static final String SYSTEM = "11111111111111111111111111111111";
    private static final String USDC = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";

    private static final String WALLET = indirizzo(7);
    private static final String ALTRO = indirizzo(8);
    private static final String CONTO_USDC = indirizzo(9);
    private static final String CONTO_WSOL_TEMP = indirizzo(10);
    private static final String VAULT_POOL = indirizzo(11);
    private static final String CONTO_ALTRUI = indirizzo(12);

    /** SOL che entrano nella gamba SOL, come in parseTransactions: variazione del wallet + commissione + conti propri */
    private static BigDecimal gambaSol(JSONObject tx) {
        BigDecimal wallet = BigDecimal.ZERO;
        JSONArray ad = tx.getJSONArray("accountData");
        for (int i = 0; i < ad.length(); i++) {
            if (ad.getJSONObject(i).getString("account").equals(WALLET)) {
                wallet = ad.getJSONObject(i).getBigDecimal("nativeBalanceChange");
            }
        }
        BigDecimal fee = tx.getString("feePayer").equals(WALLET) ? tx.getBigDecimal("fee") : BigDecimal.ZERO;
        return wallet.add(fee).divide(new BigDecimal(1_000_000_000))
                .add(Trans_Solana.VariazioneSolContiTokenPropri(tx, WALLET)).stripTrailingZeros();
    }

    @Test
    void scambioSolUsdcCheApreIlContoUsdcCedeSoloITreSol() {
        JSONObject tx = tx(WALLET, 15000,
                conto(WALLET, -3002054280L),
                conto(CONTO_WSOL_TEMP, 0),                //wSOL temporaneo: aperto e chiuso nella stessa transazione
                conto(CONTO_USDC, 2039280L, saldoToken(CONTO_USDC, WALLET, USDC, "150000000", 6)),
                conto(VAULT_POOL, 3000000000L))
                .put("instructions", new JSONArray()
                        .put(istr(SYSTEM, "createAccount", WALLET, CONTO_WSOL_TEMP))
                        .put(istr(TOKEN, "2", CONTO_WSOL_TEMP, Trans_Solana.MINT_WSOL, WALLET, "SysvarRent111111111111111111111111111111111"))
                        .put(istr(TOKEN, "2", CONTO_USDC, USDC, WALLET, "SysvarRent111111111111111111111111111111111"))
                        .put(istr(TOKEN, "A", CONTO_WSOL_TEMP, WALLET, WALLET)));

        assertEquals(new BigDecimal("0.00203928"), Trans_Solana.VariazioneSolContiTokenPropri(tx, WALLET));
        assertEquals(new BigDecimal("-3"), gambaSol(tx));
    }

    @Test
    void contoApertoVuotoConIlProgrammaAtaNonProduceMovimenti() {
        JSONObject tx = tx(WALLET, 5000,
                conto(WALLET, -2044280L),
                conto(CONTO_USDC, 2039280L))
                .put("instructions", new JSONArray().put(
                        istr(ATA, "2", WALLET, CONTO_USDC, WALLET, USDC, SYSTEM, TOKEN)
                                .put("innerInstructions", new JSONArray()
                                        .put(istr(SYSTEM, "createAccount", WALLET, CONTO_USDC))
                                        .put(istr(TOKEN, base58(dati(18, WALLET)), CONTO_USDC, USDC)))));

        assertEquals(0, gambaSol(tx).signum());
    }

    @Test
    void contoApertoConInitializeAccount3VieneRiconosciutoDaiDati() {
        JSONObject tx = tx(WALLET, 5000,
                conto(WALLET, -2044280L),
                conto(CONTO_USDC, 2039280L))
                .put("instructions", new JSONArray()
                        .put(istr(TOKEN, base58(dati(18, WALLET)), CONTO_USDC, USDC)));

        assertTrue(Trans_Solana.ContiTokenDelWallet(tx, WALLET).contains(CONTO_USDC));
        assertEquals(0, gambaSol(tx).signum());
    }

    @Test
    void affittoPagatoPerIlContoDelDestinatarioRestaUnCosto() {
        JSONObject tx = tx(WALLET, 5000,
                conto(WALLET, -2044280L, saldoToken(CONTO_USDC, WALLET, USDC, "-10000000", 6)),
                conto(CONTO_ALTRUI, 2039280L, saldoToken(CONTO_ALTRUI, ALTRO, USDC, "10000000", 6)))
                .put("instructions", new JSONArray()
                        .put(istr(ATA, "2", WALLET, CONTO_ALTRUI, ALTRO, USDC, SYSTEM, TOKEN)));

        assertFalse(Trans_Solana.ContiTokenDelWallet(tx, WALLET).contains(CONTO_ALTRUI));
        assertEquals(new BigDecimal("-0.00203928"), gambaSol(tx));
    }

    @Test
    void chiusuraDelProprioContoNonEUnIncassoDiSol() {
        JSONObject tx = tx(WALLET, 5000,
                conto(WALLET, 2039280L - 5000),
                conto(CONTO_USDC, -2039280L))
                .put("instructions", new JSONArray()
                        .put(istr(TOKEN, "A", CONTO_USDC, WALLET, WALLET)));

        assertEquals(0, gambaSol(tx).signum());
    }

    @Test
    void contoApertoDaAltriNonDiventaSolRicevuto() {
        //Chi manda il token apre il conto del wallet e paga lui: il wallet non firma e non ha ceduto nulla
        JSONObject tx = tx(ALTRO, 5000,
                conto(ALTRO, -2044280L, saldoToken(CONTO_ALTRUI, ALTRO, USDC, "-1000000", 6)),
                conto(CONTO_USDC, 2039280L, saldoToken(CONTO_USDC, WALLET, USDC, "1000000", 6)))
                .put("instructions", new JSONArray()
                        .put(istr(ATA, "2", ALTRO, CONTO_USDC, WALLET, USDC, SYSTEM, TOKEN)));

        assertEquals(BigDecimal.ZERO, Trans_Solana.VariazioneSolContiTokenPropri(tx, WALLET));
    }

    @Test
    void decodificaBase58ConservaGliZeriIniziali() {
        assertArrayEquals(new byte[32], Trans_Solana.DecodificaBase58(SYSTEM));
        assertArrayEquals(new byte[]{9}, Trans_Solana.DecodificaBase58("A"));
        assertNull(Trans_Solana.DecodificaBase58("0OIl"));
    }

    // ---------------------------------------------------------------- costruzione delle transazioni

    private static JSONObject tx(String feePayer, long fee, JSONObject... conti) {
        JSONArray ad = new JSONArray();
        for (JSONObject c : conti) ad.put(c);
        return new JSONObject().put("feePayer", feePayer).put("fee", fee).put("accountData", ad);
    }

    private static JSONObject conto(String account, long lamport, JSONObject... saldi) {
        JSONArray tb = new JSONArray();
        for (JSONObject s : saldi) tb.put(s);
        return new JSONObject().put("account", account).put("nativeBalanceChange", lamport).put("tokenBalanceChanges", tb);
    }

    private static JSONObject saldoToken(String tokenAccount, String userAccount, String mint, String quantita, int decimali) {
        return new JSONObject().put("tokenAccount", tokenAccount).put("userAccount", userAccount).put("mint", mint)
                .put("rawTokenAmount", new JSONObject().put("tokenAmount", quantita).put("decimals", decimali));
    }

    private static JSONObject istr(String programma, String dati, String... account) {
        return new JSONObject().put("programId", programma).put("data", dati)
                .put("accounts", new JSONArray(Arrays.asList(account))).put("innerInstructions", new JSONArray());
    }

    private static byte[] dati(int istruzione, String proprietario) {
        byte[] p = Trans_Solana.DecodificaBase58(proprietario);
        byte[] d = new byte[1 + p.length];
        d[0] = (byte) istruzione;
        System.arraycopy(p, 0, d, 1, p.length);
        return d;
    }

    private static String indirizzo(int seme) {
        byte[] b = new byte[32];
        Arrays.fill(b, (byte) seme);
        b[0] = (byte) (0x80 | seme);
        return base58(b);
    }

    private static String base58(byte[] b) {
        String alfabeto = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        StringBuilder s = new StringBuilder();
        BigInteger v = new BigInteger(1, b);
        BigInteger base = BigInteger.valueOf(58);
        while (v.signum() > 0) {
            BigInteger[] qr = v.divideAndRemainder(base);
            s.append(alfabeto.charAt(qr[1].intValue()));
            v = qr[0];
        }
        for (int i = 0; i < b.length && b[i] == 0; i++) s.append('1');
        return s.reverse().toString();
    }
}
