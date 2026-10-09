package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Raccolta di {@link PrezziImportDeFi}: quali prezzi un'importazione DeFi chiedera', ricavati dai dati appena
 * scaricati. Nessuna rete e nessun database: solo il passaggio dal JSON alle due liste (address per DefiLlama,
 * simboli per gli exchange), con gli alias predefiniti di {@link AliasPrezziToken}.
 */
class PrezziImportDeFiTest {

    /** 31/12/2025 23:00 UTC. */
    private static final long FINE_2025 = 1767222000000L;
    private static final String SECONDI = "1767222000";

    private static final String WALLET = "0x00000000000000000000000000000000000000aa";
    private static final String ALTRO = "0x00000000000000000000000000000000000000bb";
    private static final String GM = "0x47c031236e19d024b42f8ae6780e44a573170703";
    /** USDT su Cronos: ha un alias predefinito, si prezza come USDT sugli exchange. */
    private static final String USDT_CRO = "0x66e428c3f67a68878562e79A0234c1F83c208770";

    private static Map<String, String> aliasPrima, stessoPrezzoPrima;
    private static Map<String, String[]> riferimentiPrima;
    private static Map<String, Long> dalAliasPrima, dalStessoPrezzoPrima;

    @BeforeAll
    static void alias() {
        aliasPrima = Principale.Mappa_AddressRete_Nome;
        stessoPrezzoPrima = Principale.Mappa_MoneteStessoPrezzo;
        riferimentiPrima = AliasPrezziToken.Riferimenti;
        dalAliasPrima = AliasPrezziToken.DalAlias;
        dalStessoPrezzoPrima = AliasPrezziToken.DalStessoPrezzo;
        AliasPrezziToken.Applica(AliasPrezziToken.Predefinite());
    }

    @AfterAll
    static void ripristina() {
        Principale.Mappa_AddressRete_Nome = aliasPrima;
        Principale.Mappa_MoneteStessoPrezzo = stessoPrezzoPrima;
        AliasPrezziToken.Riferimenti = riferimentiPrima;
        AliasPrezziToken.DalAlias = dalAliasPrima;
        AliasPrezziToken.DalStessoPrezzo = dalStessoPrezzoPrima;
    }

    private static JSONObject tx(String from, String value) {
        return new JSONObject().put("from", from).put("value", value).put("timeStamp", SECONDI);
    }

    @Test
    void lIstanteEQuelloConCuiTransazioneDefiPrezza() {
        assertEquals(FINE_2025, PrezziImportDeFi.IstanteImport(FINE_2025));
        assertEquals(FINE_2025, PrezziImportDeFi.IstanteImport(FINE_2025 + 400), "i millisecondi si perdono nel giro dal testo");
    }

    @Test
    void laMonetaDellaReteServeSeLaTransazioneMuoveValoreOSeLaMandaIlWallet() {
        JSONArray txlist = new JSONArray()
                .put(tx(ALTRO, "1000000000000000000"))   //CRO ricevuti
                .put(tx(WALLET, "0"))                     //chiamata a un contratto: solo commissione
                .put(tx(ALTRO, "0"));                     //nessun valore e commissione pagata da altri
        PrezziImportDeFi.Raccolta r = PrezziImportDeFi.RaccogliEVM(txlist, null, null, WALLET, "CRO", "CRO");
        assertEquals(2, r.simboli().size());
        assertEquals(new Prezzi.SimboloIstante("CRO", FINE_2025), r.simboli().get(0));
        assertTrue(r.address().isEmpty());
    }

    @Test
    void leTransazioniInterneContanoSoloConValore() {
        JSONArray interne = new JSONArray().put(tx(ALTRO, "5")).put(tx(ALTRO, "0"));
        assertEquals(1, PrezziImportDeFi.RaccogliEVM(null, interne, null, WALLET, "CRO", "CRO").simboli().size());
    }

    @Test
    void unTokenConAliasVaAgliExchangeGliAltriADefiLlama() {
        JSONArray tokentx = new JSONArray()
                .put(new JSONObject().put("contractAddress", USDT_CRO).put("timeStamp", SECONDI))
                .put(new JSONObject().put("contractAddress", GM).put("timeStamp", SECONDI))
                .put(new JSONObject().put("contractAddress", "").put("timeStamp", SECONDI));
        PrezziImportDeFi.Raccolta r = PrezziImportDeFi.RaccogliEVM(null, null, tokentx, WALLET, "CRO", "CRO");
        assertEquals(List.of(new Prezzi.SimboloIstante("USDT", FINE_2025)), r.simboli());
        assertEquals(List.of(new PrezziDefiLlama.Coppia(GM, "CRO", FINE_2025)), r.address());
    }

    @Test
    void moralisLeggeERC20NativiECommissione() {
        JSONObject conErc20 = new JSONObject().put("block_timestamp", "2025-12-31T23:00:00.000Z").put("from_address", ALTRO)
                .put("erc20_transfers", new JSONArray().put(new JSONObject().put("address", GM)));
        JSONObject conNativi = new JSONObject().put("block_timestamp", "2025-12-31T23:00:00.000Z").put("from_address", ALTRO)
                .put("native_transfers", new JSONArray().put(new JSONObject()));
        JSONObject conCommissione = new JSONObject().put("block_timestamp", "2025-12-31T22:00:00.000Z").put("from_address", WALLET)
                .put("transaction_fee", "0.0001");
        PrezziImportDeFi.Raccolta r = PrezziImportDeFi.RaccogliMoralis(
                new JSONArray().put(conErc20).put(conNativi).put(conCommissione).put(new JSONObject()), WALLET, "BSC", "BNB");
        assertEquals(List.of(new PrezziDefiLlama.Coppia(GM, "BSC", FINE_2025)), r.address());
        assertEquals(List.of(new Prezzi.SimboloIstante("BNB", FINE_2025), new Prezzi.SimboloIstante("BNB", FINE_2025 - 3600000L)),
                r.simboli());
    }

    @Test
    void solanaTieneIlWalletOIProprietariNonDichiaratiESol() {
        String wallet = "WaLLet111";
        JSONObject tx = new JSONObject().put("timestamp", 1767222000L).put("feePayer", "Altro").put("accountData", new JSONArray()
                .put(new JSONObject().put("account", wallet).put("nativeBalanceChange", -5000)
                        .put("tokenBalanceChanges", new JSONArray()
                                .put(new JSONObject().put("userAccount", wallet).put("mint", "MintMio"))
                                .put(new JSONObject().put("userAccount", "Altro").put("mint", "MintAltrui"))
                                .put(new JSONObject().put("mint", "MintSenzaProprietario")))));
        PrezziImportDeFi.Raccolta r = PrezziImportDeFi.RaccogliSolana(new JSONArray().put(tx), wallet);
        assertEquals(List.of(new PrezziDefiLlama.Coppia("MintMio", "SOL", FINE_2025),
                new PrezziDefiLlama.Coppia("MintSenzaProprietario", "SOL", FINE_2025)), r.address());
        assertEquals(List.of(new Prezzi.SimboloIstante("SOL", FINE_2025)), r.simboli());
    }

    @Test
    void solanaSenzaCommissioneNeSaldoNativoNonChiedeSol() {
        JSONObject tx = new JSONObject().put("timestamp", 1767222000L).put("feePayer", "Altro");
        assertTrue(PrezziImportDeFi.RaccogliSolana(new JSONArray().put(tx), "WaLLet111").simboli().isEmpty());
    }

    @Test
    void laRaccoltaPerSimboliCopreLOraEScartaIlFuturo() {
        List<Prezzi.RichiestaPrezzo> r = Prezzi.RaccogliRichiestePerSimboli(List.of(
                new Prezzi.SimboloIstante("CRO", FINE_2025 + 30 * 60000L),
                new Prezzi.SimboloIstante("CRO", FINE_2025 + 40 * 60000L),
                new Prezzi.SimboloIstante("CRO", System.currentTimeMillis() + 86400000L)));
        assertEquals(1, r.size(), "due movimenti nella stessa ora sono una richiesta, il futuro nessuna");
        assertEquals("CRO", r.get(0).simbolo);
        assertEquals(FINE_2025, r.get(0).since);
    }
}
