package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Wallet BTC multi-indirizzo: lettura di una transazione con due insiemi di indirizzi e confronto che
 * decide quali movimenti gia' importati cambiano quando si aggiunge o si toglie un indirizzo.
 * Nessuna rete e nessun database: il piano non costruisce movimenti (costruirli vuol dire prezzarli).
 */
class WalletBtcMultiIndirizzoTest {

    private static final String A = "indirizzoA";
    private static final String N = "indirizzoN";
    private static final String X = "esternoX";
    private static final String Y = "esternoY";
    private static final String NOME = "Ledger vecchio";

    /** Transazione mempool.space: input e output come coppie indirizzo, satoshi */
    private static JSONObject tx(String txid, int blocco, Object[][] input, Object[][] output, long fee) {
        JSONArray vin = new JSONArray();
        for (Object[] i : input) {
            vin.put(new JSONObject().put("prevout",
                    new JSONObject().put("scriptpubkey_address", i[0]).put("value", i[1])));
        }
        JSONArray vout = new JSONArray();
        for (Object[] o : output) {
            vout.put(new JSONObject().put("scriptpubkey_address", o[0]).put("value", o[1]));
        }
        return new JSONObject().put("txid", txid).put("fee", fee)
                .put("status", new JSONObject().put("confirmed", true).put("block_height", blocco).put("block_time", 1700000000L))
                .put("vin", vin).put("vout", vout);
    }

    /** A spende 1 BTC: 0,3 a X, 0,6999 di resto su N, 0,0001 di commissione */
    private static JSONObject pagamentoConRestoSuN(int blocco) {
        return tx("t_resto", blocco, new Object[][]{{A, 100_000_000L}},
                new Object[][]{{X, 30_000_000L}, {N, 69_990_000L}}, 10_000L);
    }

    private static String[] movimento(String id, String hash, int blocco) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[3] = WalletBtcMultiIndirizzo.CampoWallet(NOME);
        v[8] = "BTC";
        v[23] = String.valueOf(blocco);
        v[24] = hash;
        return v;
    }

    @Test
    void conUnSoloIndirizzoIlRestoSuUnIndirizzoPropioSembraUnUscitaVersoLEsterno() {
        Trans_Bitcoin.Analisi a = Trans_Bitcoin.analizza(pagamentoConRestoSuN(100), Set.of(A));
        assertEquals(new Trans_Bitcoin.Analisi(false, 100_000_000L, 0L, 10_000L, X), a);
        //uscita netta 0,9999: tutto meno la commissione
        assertEquals(99_990_000L, a.InviatiSats() - a.RicevutiSats() - a.FeeSats());
    }

    @Test
    void conEntrambiGliIndirizziIlRestoRestaNelWallet() {
        Trans_Bitcoin.Analisi a = Trans_Bitcoin.analizza(pagamentoConRestoSuN(100), Set.of(A, N));
        assertEquals(69_990_000L, a.RicevutiSats());
        assertEquals(30_000_000L, a.InviatiSats() - a.RicevutiSats() - a.FeeSats());
        assertEquals(X, a.Controparte());
        assertFalse(a.SoloCommissione());
    }

    @Test
    void unConsolidamentoFraIndirizziPropriESoloCommissione() {
        JSONObject t = tx("t_cons", 100, new Object[][]{{A, 50_000L}}, new Object[][]{{N, 40_000L}}, 10_000L);
        assertTrue(Trans_Bitcoin.analizza(t, Set.of(A, N)).SoloCommissione());
        assertFalse(Trans_Bitcoin.analizza(t, Set.of(A)).SoloCommissione());
        assertNull(Trans_Bitcoin.analizza(t, Set.of(Y)));
    }

    @Test
    void aggiungereUnIndirizzoCambiaSoloLeTransazioniFinoAllUltimoBloccoImportato() {
        Map<String, String[]> mov = new TreeMap<>();
        mov.put("20231114_resto_PC", movimento("20231114_resto_PC", "t_resto", 100));
        mov.put("20231110_altro_DC", movimento("20231110_altro_DC", "t_altro", 80));
        //transazione vecchia di A e N, gia' letta con A ma cancellata dall'utente: non va resuscitata
        JSONObject cancellata = tx("t_cancellata", 90, new Object[][]{{A, 10_000L}},
                new Object[][]{{Y, 5_000L}, {N, 4_000L}}, 1_000L);

        List<JSONObject> cronologiaN = List.of(
                pagamentoConRestoSuN(100),
                tx("t_soloN", 95, new Object[][]{{Y, 20_000L}}, new Object[][]{{N, 19_000L}}, 1_000L),
                tx("t_futura", 150, new Object[][]{{Y, 20_000L}}, new Object[][]{{N, 19_000L}}, 1_000L),
                cancellata);

        int ultimo = WalletBtcMultiIndirizzo.UltimoBloccoImportato(NOME, mov);
        assertEquals(100, ultimo);

        WalletBtcMultiIndirizzo.Piano p = WalletBtcMultiIndirizzo.CalcolaVariazioni(
                NOME, Set.of(A), Set.of(A, N), cronologiaN, mov, ultimo);

        Map<String, WalletBtcMultiIndirizzo.Variazione> perTx = new HashMap<>();
        for (WalletBtcMultiIndirizzo.Variazione v : p.Variazioni) perTx.put(v.Txid, v);

        assertEquals(2, p.Variazioni.size(), "solo t_resto e t_soloN");
        assertEquals(WalletBtcMultiIndirizzo.TipoVariazione.SOSTITUITA, perTx.get("t_resto").Tipo);
        assertEquals(List.of("20231114_resto_PC"), perTx.get("t_resto").IdVecchi);
        assertEquals(WalletBtcMultiIndirizzo.TipoVariazione.AGGIUNTA, perTx.get("t_soloN").Tipo);
        assertFalse(perTx.containsKey("t_futura"), "oltre l'ultimo blocco la prende lo scarico normale");
        assertFalse(perTx.containsKey("t_cancellata"), "cancellata dall'utente, resta cancellata");
        assertFalse(perTx.get("t_resto").ToccataAMano);
    }

    @Test
    void toglierUnIndirizzoRimuoveLeTransazioniCheToccavanoSoloQuello() {
        Map<String, String[]> mov = new TreeMap<>();
        mov.put("20231114_resto_PC", movimento("20231114_resto_PC", "t_resto", 100));
        String[] soloN = movimento("20231112_soloN_DC", "t_soloN", 95);
        soloN[20] = "20231112_altroWallet_PC"; //abbinato a mano a un prelievo di un altro wallet
        mov.put(soloN[0], soloN);

        List<JSONObject> cronologiaN = List.of(
                pagamentoConRestoSuN(100),
                tx("t_soloN", 95, new Object[][]{{Y, 20_000L}}, new Object[][]{{N, 19_000L}}, 1_000L));

        WalletBtcMultiIndirizzo.Piano p = WalletBtcMultiIndirizzo.CalcolaVariazioni(
                NOME, Set.of(A, N), Set.of(A), cronologiaN, mov, 100);

        Map<String, WalletBtcMultiIndirizzo.Variazione> perTx = new HashMap<>();
        for (WalletBtcMultiIndirizzo.Variazione v : p.Variazioni) perTx.put(v.Txid, v);
        assertEquals(WalletBtcMultiIndirizzo.TipoVariazione.SOSTITUITA, perTx.get("t_resto").Tipo);
        assertEquals(WalletBtcMultiIndirizzo.TipoVariazione.RIMOSSA, perTx.get("t_soloN").Tipo);
        assertTrue(perTx.get("t_soloN").ToccataAMano);
        assertEquals(1, p.ContaToccate());
    }

    @Test
    void unaTransazioneCheDiventaUnConsolidamentoInternoVieneTolta() {
        //A manda tutto a N: con il solo A era un prelievo, con A e N resta solo la commissione
        Map<String, String[]> mov = new TreeMap<>();
        mov.put("20231114_cons_PC", movimento("20231114_cons_PC", "t_cons", 100));
        mov.put("20231114_consC_CM", movimento("20231114_consC_CM", "t_cons", 100));
        JSONObject t = tx("t_cons", 100, new Object[][]{{A, 50_000L}}, new Object[][]{{N, 40_000L}}, 10_000L);

        WalletBtcMultiIndirizzo.Piano p = WalletBtcMultiIndirizzo.CalcolaVariazioni(
                NOME, Set.of(A), Set.of(A, N), List.of(t), mov, 100);
        assertEquals(1, p.Variazioni.size());
        assertEquals(WalletBtcMultiIndirizzo.TipoVariazione.RIMOSSA, p.Variazioni.get(0).Tipo);
        assertEquals(2, p.Variazioni.get(0).IdVecchi.size(), "prelievo e commissione");
    }

    @Test
    void iMovimentiDiTokenNonVengonoToccati() {
        Map<String, String[]> mov = new TreeMap<>();
        String[] ordi = movimento("20231114_resto_DC", "t_resto", 100);
        ordi[8] = "";
        ordi[11] = "ORDI";
        mov.put(ordi[0], ordi);
        WalletBtcMultiIndirizzo.Piano p = WalletBtcMultiIndirizzo.CalcolaVariazioni(
                NOME, Set.of(A), Set.of(A, N), List.of(pagamentoConRestoSuN(100)), mov, 100);
        //il movimento BTC di t_resto non c'e' e t_resto toccava gia' A: nessuna variazione
        assertTrue(p.Variazioni.isEmpty());
        assertEquals(1, p.TokenEsistenti);
    }

    @Test
    void unIndirizzoNonPuoStareInDueWallet() {
        String indirizzo = "1BoatSLRHtKNngkdXEeobR76b53LETtpyT";
        Map<String, String> wallets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> gruppi = new TreeMap<>();

        assertNull(WalletBtcMultiIndirizzo.ValidaIndirizzi(NOME, Set.of(indirizzo), wallets, gruppi));

        assertNotNull(WalletBtcMultiIndirizzo.ValidaIndirizzi(NOME, Set.of("non un indirizzo"), wallets, gruppi));

        gruppi.put(indirizzo, "Altro wallet");
        assertNotNull(WalletBtcMultiIndirizzo.ValidaIndirizzi(NOME, Set.of(indirizzo), wallets, gruppi));
        gruppi.put(indirizzo, NOME);
        assertNull(WalletBtcMultiIndirizzo.ValidaIndirizzi(NOME, Set.of(indirizzo), wallets, gruppi));

        wallets.put(indirizzo + "_BTC", indirizzo + ";BTC");
        assertNotNull(WalletBtcMultiIndirizzo.ValidaIndirizzi(NOME, Set.of(indirizzo), wallets, gruppi));
    }

    @Test
    void gliIndirizziBech32InMaiuscoloVengonoPortatiInMinuscolo() {
        Set<String> e = WalletBtcMultiIndirizzo.LeggiElenco(
                " BC1QAR0SRRR7XFKVY5L643LYDNW9RE59GTZZWF5MDQ \n\n1BoatSLRHtKNngkdXEeobR76b53LETtpyT; 3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy");
        assertEquals(List.of("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                "1BoatSLRHtKNngkdXEeobR76b53LETtpyT", "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"), List.copyOf(e));
    }

    @Test
    void ilNomeDelWalletNonPuoConfondersiConUnIndirizzoOUnaChiave() {
        assertNull(Trans_Bitcoin.isNomeWalletValido("Ledger vecchio"));
        assertNotNull(Trans_Bitcoin.isNomeWalletValido("  "));
        assertNotNull(Trans_Bitcoin.isNomeWalletValido("1BoatSLRHtKNngkdXEeobR76b53LETtpyT"));
        assertNotNull(Trans_Bitcoin.isNomeWalletValido("Zpub Ledger"));
        assertNotNull(Trans_Bitcoin.isNomeWalletValido("Ledger (casa)"));
        assertNotNull(Trans_Bitcoin.isNomeWalletValido("Ledger;casa"));
    }
}
