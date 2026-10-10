package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link GiacenzeBlockchain}: scelta del blocco, lettura dei saldi con e senza Multicall3, e soprattutto che un saldo
 * non letto non diventi mai zero. Il nodo e' finto: nessuna rete.
 */
class GiacenzeBlockchainTest {

    static final String WALLET = "0x1111111111111111111111111111111111111111";
    static final String TOKEN_A = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    static final String TOKEN_B = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @BeforeAll
    static void reti() {
        Principale.Mappa_ChainExplorer.put("CRO", new String[]{"", "", "CRO", "cronos", "cronos"});
    }

    /**
     * Catena finta: blocco {@code i} al secondo {@code 1000 + 10*i}, ultimo blocco 1000. Multicall3 esiste dal blocco
     * {@code multicallDal}; il token B non risponde.
     */
    static final class NodoFinto implements NodoPubblicoDefi.Nodo {
        final long multicallDal;
        final List<String> blocchiLetti = new ArrayList<>();
        int chiamate = 0;

        NodoFinto(long multicallDal) {
            this.multicallDal = multicallDal;
        }

        @Override
        public JsonElement chiama(NodoPubblicoDefi.Uso uso, String metodo, JsonArray p) throws Exception {
            chiamate++;
            switch (metodo) {
                case "eth_blockNumber":
                    return new JsonPrimitive("0x3e8");
                case "eth_getBlockByNumber": {
                    long b = Long.decode(p.get(0).getAsString());
                    JsonObject o = new JsonObject();
                    o.addProperty("timestamp", "0x" + Long.toHexString(1000 + 10 * b));
                    return o;
                }
                case "eth_getCode":
                    return new JsonPrimitive(Long.decode(p.get(1).getAsString()) >= multicallDal ? "0x6080" : "0x");
                case "eth_getBalance":
                    blocchiLetti.add(p.get(1).getAsString());
                    return new JsonPrimitive("0xde0b6b3a7640000"); //1 moneta a 18 decimali
                case "eth_call": {
                    JsonObject c = p.get(0).getAsJsonObject();
                    String to = c.get("to").getAsString().toLowerCase();
                    String dati = c.get("data").getAsString();
                    blocchiLetti.add(p.get(1).getAsString());
                    if (to.equals(GiacenzeBlockchain.MULTICALL3)) {
                        //native, A saldo, A decimali, B saldo (fallisce), B decimali
                        int n = new BigInteger(dati.substring(2 + 8 + 64, 2 + 8 + 128), 16).intValue();
                        assertEquals(5, n);
                        return new JsonPrimitive(Risultato(new BigInteger[]{
                            new BigInteger("2000000000000000000"), BigInteger.valueOf(1_500_000), BigInteger.valueOf(6),
                            null, BigInteger.valueOf(18)}));
                    }
                    if (to.equals(TOKEN_B)) throw new NodoPubblicoDefi.NodiPubblici.ErroreDefinitivo("execution reverted");
                    if (dati.startsWith("0x" + Trans_XLayer.SEL_DECIMALS)) return new JsonPrimitive("0x" + Trans_XLayer.Parola("6"));
                    return new JsonPrimitive("0x" + Trans_XLayer.Parola(Long.toHexString(2_500_000)));
                }
                default:
                    throw new Exception("metodo inatteso " + metodo);
            }
        }
    }

    /** Codifica ABI di {@code Result[]} di {@code aggregate3}; {@code null} = chiamata fallita. */
    static String Risultato(BigInteger[] valori) {
        int n = valori.length;
        List<String> el = new ArrayList<>();
        for (BigInteger v : valori) {
            if (v == null) el.add(Trans_XLayer.Parola("0") + Trans_XLayer.Parola("40") + Trans_XLayer.Parola("0"));
            else el.add(Trans_XLayer.Parola("1") + Trans_XLayer.Parola("40") + Trans_XLayer.Parola("20")
                    + Trans_XLayer.Parola(v.toString(16)));
        }
        StringBuilder sb = new StringBuilder("0x").append(Trans_XLayer.Parola("20")).append(Trans_XLayer.Parola(Integer.toHexString(n)));
        int off = n * 32;
        for (String e : el) {
            sb.append(Trans_XLayer.Parola(Integer.toHexString(off)));
            off += e.length() / 2;
        }
        el.forEach(sb::append);
        return sb.toString();
    }

    static List<GiacenzeBlockchain.Richiesta> Righe() {
        return List.of(
                new GiacenzeBlockchain.Richiesta("CRO", "CRO", "Crypto", "CRO"),
                new GiacenzeBlockchain.Richiesta("A", "TKA", "Crypto", TOKEN_A),
                new GiacenzeBlockchain.Richiesta("B", "TKB", "Crypto", TOKEN_B),
                new GiacenzeBlockchain.Richiesta("NFT", "Gatto #1", "NFT", TOKEN_A),
                new GiacenzeBlockchain.Richiesta("EXCH", "USDT", "Crypto", ""));
    }

    @Test
    void ilBloccoEUltimoConTimestampStrettamenteMinoreDellaData() throws Exception {
        NodoFinto n = new NodoFinto(0);
        OKX_WalletCarta.Rpc rpc = (m, p) -> n.chiama(NodoPubblicoDefi.Uso.STATO, m, p);
        //blocco 50 al secondo 1500: una transazione nel blocco 50 e' "dopo" la mezzanotte del secondo 1500
        assertEquals(49, GiacenzeBlockchain.UltimoBloccoPrima("CRO", 1500, rpc));
        assertEquals(50, GiacenzeBlockchain.UltimoBloccoPrima("CRO", 1501, rpc));
        assertEquals(50, GiacenzeBlockchain.UltimoBloccoPrima("CRO", 1509, rpc));
    }

    @Test
    void conMulticallUnSaldoFallitoENonDisponibileENonZero() {
        NodoFinto n = new NodoFinto(0);
        //secondo 1505 -> blocco 50
        GiacenzeBlockchain.Esito e = GiacenzeBlockchain.Leggi(WALLET, "CRO", 1_505_000L, Righe(), null, n);
        assertEquals(50, e.blocco());
        assertTrue(e.avvisi().isEmpty(), e.avvisi().toString());
        assertEquals("2", e.testi().get("CRO"));
        assertEquals("1.5", e.testi().get("A"));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, e.testi().get("B"));
        //NFT e moneta senza indirizzo non si leggono
        assertFalse(e.testi().containsKey("NFT"));
        assertFalse(e.testi().containsKey("EXCH"));
        assertTrue(n.blocchiLetti.stream().allMatch("0x32"::equals), n.blocchiLetti.toString());
    }

    @Test
    void laDataDiOggiLeggeLUltimoBloccoSenzaCercarlo() {
        NodoFinto n = new NodoFinto(0);
        GiacenzeBlockchain.Esito e = GiacenzeBlockchain.Leggi(WALLET, "CRO", 0, Righe(), null, n);
        assertEquals(1000, e.blocco());
        assertEquals("1.5", e.testi().get("A"));
        assertTrue(n.blocchiLetti.stream().allMatch("0x3e8"::equals), n.blocchiLetti.toString());
    }

    @Test
    void senzaMulticallSiLeggeUnSaldoAllaVolta() {
        NodoFinto n = new NodoFinto(900);
        GiacenzeBlockchain.Esito e = GiacenzeBlockchain.Leggi(WALLET, "CRO", 1_505_000L, Righe(), null, n);
        assertEquals("1", e.testi().get("CRO"));
        assertEquals("2.5", e.testi().get("A"));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, e.testi().get("B"));
        assertTrue(n.blocchiLetti.stream().allMatch("0x32"::equals), n.blocchiLetti.toString());
    }

    @Test
    void unNodoCheNonRispondeDaNonDisponibileSuTutteLeRighe() {
        NodoPubblicoDefi.Nodo rotto = (u, m, p) -> {
            throw new Exception("HTTP 503");
        };
        GiacenzeBlockchain.Esito e = GiacenzeBlockchain.Leggi(WALLET, "CRO", 1_505_000L, Righe(), null, rotto);
        assertEquals(3, e.testi().size());
        assertTrue(e.testi().values().stream().allMatch(GiacenzeBlockchain.NON_DISPONIBILE::equals));
        assertFalse(e.avvisi().isEmpty());
    }

    @Test
    void reteSenzaStoricoNonLeggeLeDatePassate() {
        Principale.Mappa_ChainExplorer.put("MONAD", new String[]{"", "", "MON", "monad", "monad"});
        NodoFinto n = new NodoFinto(0);
        GiacenzeBlockchain.Esito e = GiacenzeBlockchain.Leggi(WALLET, "MONAD", 1_505_000L,
                List.of(new GiacenzeBlockchain.Richiesta("MON", "MON", "Crypto", "MON")), null, n);
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, e.testi().get("MON"));
        assertEquals(0, n.chiamate);
    }

    @Test
    void decodificaAggregate3DistingueFallimentoDaZero() {
        List<BigInteger> v = GiacenzeBlockchain.DecodificaAggregate3(Risultato(new BigInteger[]{BigInteger.ZERO, null, BigInteger.TEN}));
        assertEquals(BigInteger.ZERO, v.get(0));
        assertNull(v.get(1));
        assertEquals(BigInteger.TEN, v.get(2));
    }

    @Test
    void quantitaECoincidenza() {
        assertEquals("0.000001", GiacenzeBlockchain.Quantita(BigInteger.ONE, BigInteger.valueOf(6)));
        assertEquals("0", GiacenzeBlockchain.Quantita(BigInteger.ZERO, BigInteger.valueOf(18)));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, GiacenzeBlockchain.Quantita(BigInteger.ONE, null));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, GiacenzeBlockchain.Quantita(BigInteger.ONE, BigInteger.valueOf(200)));
        //notazione scientifica dell'archivio
        assertEquals(Boolean.TRUE, GiacenzeBlockchain.Coincide("2.5E-9", "0.0000000025"));
        assertEquals(Boolean.FALSE, GiacenzeBlockchain.Coincide("1.0", "1.000000000000000001"));
        assertNull(GiacenzeBlockchain.Coincide("1", GiacenzeBlockchain.NON_DISPONIBILE));
        assertNull(GiacenzeBlockchain.Coincide("1", GiacenzeBlockchain.IN_LETTURA));
        assertNull(GiacenzeBlockchain.Coincide("1", ""));
    }

    @Test
    void siConfrontaSoloIlSottoWalletDellIndirizzo() {
        List<String> SoloIndirizzo = List.of("Wallet");
        List<String> ConPiattaforma = List.of("Wallet", "Piattaforma/DeFi");
        assertArrayEquals(new String[]{WALLET, "CRO"}, GiacenzeBlockchain.WalletDaLeggere(WALLET + " (CRO)", "Tutti", SoloIndirizzo));
        assertArrayEquals(new String[]{WALLET, "CRO"}, GiacenzeBlockchain.WalletDaLeggere(WALLET + " (CRO)", "Wallet", ConPiattaforma));
        //Con "Tutti" la Qta comprende i token messi in una piattaforma, che non sono piu' all'indirizzo
        assertNull(GiacenzeBlockchain.WalletDaLeggere(WALLET + " (CRO)", "Tutti", ConPiattaforma));
        assertNull(GiacenzeBlockchain.WalletDaLeggere(WALLET + " (CRO)", "Piattaforma/DeFi", ConPiattaforma));
        assertNull(GiacenzeBlockchain.WalletDaLeggere("Tutti", "Tutti", null));
        assertNull(GiacenzeBlockchain.WalletDaLeggere("Binance", "Tutti", null));
        assertNull(GiacenzeBlockchain.WalletDaLeggere("Gruppo : Wallet 01 ( Binance )", "Tutti", null));
        //SOL e BTC: nessun nodo con gli stati
        assertNull(GiacenzeBlockchain.WalletDaLeggere("E2ebWw5CLJGbgDLvnEk19ES6QceouQLMbxHJRzieM6FL (SOL)", "Tutti", null));
    }

    static String[] Movimento(String ID, String Data, String Blocco, String MonU, String QtaU, String AddrU,
            String MonE, String QtaE, String AddrE) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = ID;
        v[1] = Data;
        v[3] = WALLET + " (CRO)";
        v[4] = "Wallet";
        v[8] = MonU;
        v[9] = MonU.isEmpty() ? "" : "Crypto";
        v[10] = QtaU;
        v[11] = MonE;
        v[12] = MonE.isEmpty() ? "" : "Crypto";
        v[13] = QtaE;
        v[23] = Blocco;
        v[26] = AddrU;
        v[28] = AddrE;
        v[34] = "CRO";
        return v;
    }

    @Test
    void attornoAlMovimentoSiConfrontaConLArchivioPrimaEDopoIlBlocco() {
        String Dep = "20240101100000_BC.CRO.X_1_1_DC";
        String Swap = "20240101120000_BC.CRO.X_1_1_SC";
        String Fee = "20240101120000_BC.CRO.X_2_1_CM";
        Principale.MappaCryptoWallet.put(Dep, Movimento(Dep, "2024-01-01 10:00", "10", "", "", "", "CRO", "3", "CRO"));
        Principale.MappaCryptoWallet.put(Swap, Movimento(Swap, "2024-01-01 12:00", "50", "CRO", "-1", "CRO", "TKA", "1.5", TOKEN_A));
        Principale.MappaCryptoWallet.put(Fee, Movimento(Fee, "2024-01-01 12:00", "50", "CRO", "-0.5", "CRO", "", "", ""));
        //Il token messo in una piattaforma resta del wallet nell'archivio ma non e' all'indirizzo: non conta
        String Farm = "20240101110000_BC.CRO.X_1A_1_DC";
        String[] InFarm = Movimento(Farm, "2024-01-01 11:00", "", "", "", "", "TKA", "7", TOKEN_A);
        InFarm[4] = "Piattaforma/DeFi";
        Principale.MappaCryptoWallet.put(Farm, InFarm);
        try {
            //Blocco 49: 3 CRO e nessun TKA; blocco 50: 1,5 CRO (scambio e commissione) e 1,5 TKA (6 decimali)
            NodoPubblicoDefi.Nodo n = (u, m, p) -> {
                if (m.equals("eth_getCode")) return new JsonPrimitive("0x6080");
                long b = Long.decode(p.get(1).getAsString());
                return new JsonPrimitive(Risultato(b == 49
                        ? new BigInteger[]{new BigInteger("3000000000000000000"), BigInteger.ZERO, BigInteger.valueOf(6)}
                        : new BigInteger[]{new BigInteger("1500000000000000000"), BigInteger.valueOf(1_500_000), BigInteger.valueOf(6)}));
            };
            GiacenzeBlockchain.SaldiMovimento s = GiacenzeBlockchain.LeggiAttornoAlMovimento(Swap, n);
            String Cro = Principale_GiacenzeaData.ChiaveRiga("CRO", "Crypto", "CRO", "CRO");
            String Tka = Principale_GiacenzeaData.ChiaveRiga("TKA", "Crypto", TOKEN_A, "CRO");
            assertEquals(50, s.blocco());
            assertEquals(1, s.altriMovimenti());
            assertEquals("3", s.Testo(Cro, false));
            assertEquals("1.5", s.Testo(Cro, true));
            assertEquals("0", s.Testo(Tka, false));
            assertEquals("1.5", s.Testo(Tka, true));
            //La commissione dello stesso secondo e' nel confine "dopo" dell'archivio, come nel blocco
            assertEquals(Boolean.TRUE, s.Coincide(Cro, false));
            assertEquals(Boolean.TRUE, s.Coincide(Cro, true));
            assertEquals(Boolean.TRUE, s.Coincide(Tka, false));
            assertEquals(Boolean.TRUE, s.Coincide(Tka, true));
            //Un nodo che non risponde: n.d., mai zero, e niente colore
            GiacenzeBlockchain.SaldiMovimento r = GiacenzeBlockchain.LeggiAttornoAlMovimento(Swap, (u, m, p) -> {
                throw new Exception("HTTP 503");
            });
            assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, r.Testo(Tka, false));
            assertNull(r.Coincide(Tka, true));
            //Un movimento senza blocco non ha righe della blockchain
            Principale.MappaCryptoWallet.get(Dep)[23] = "";
            assertNull(GiacenzeBlockchain.InLettura(Dep));
            assertNotNull(GiacenzeBlockchain.InLettura(Swap));
        } finally {
            Principale.MappaCryptoWallet.remove(Dep);
            Principale.MappaCryptoWallet.remove(Swap);
            Principale.MappaCryptoWallet.remove(Fee);
            Principale.MappaCryptoWallet.remove(Farm);
        }
    }

    /**
     * Catena finta per la tabella dettaglio: a fine blocco {@code b} il wallet ha {@code b} monete della rete e
     * {@code b/100} TKA (6 decimali); dal blocco {@code guastoDal} il nodo non risponde.
     */
    static final class NodoBlocchi implements NodoPubblicoDefi.Nodo {
        final long guastoDal;
        final List<String> chiamate = new ArrayList<>();

        NodoBlocchi(long guastoDal) {
            this.guastoDal = guastoDal;
        }

        @Override
        public JsonElement chiama(NodoPubblicoDefi.Uso uso, String metodo, JsonArray p) throws Exception {
            long b = Long.decode(p.get(1).getAsString());
            if (b >= guastoDal) throw new Exception("HTTP 503");
            if (metodo.equals("eth_getBalance")) {
                chiamate.add("saldo " + b);
                return new JsonPrimitive("0x" + BigInteger.valueOf(b).multiply(BigInteger.TEN.pow(18)).toString(16));
            }
            String dati = p.get(0).getAsJsonObject().get("data").getAsString();
            if (dati.startsWith("0x" + Trans_XLayer.SEL_DECIMALS)) {
                chiamate.add("decimali " + b);
                return new JsonPrimitive("0x" + Trans_XLayer.Parola("6"));
            }
            chiamate.add("token " + b);
            return new JsonPrimitive("0x" + Trans_XLayer.Parola(Long.toHexString(b * 10_000)));
        }
    }

    @Test
    void dettaglioLeRigheDiUnSecondoSiConfrontanoConLaQtaResiduaAFineSecondo() {
        List<GiacenzeBlockchain.RigaDettaglio> Righe = List.of(
                //commissione e scambio della stessa transazione: la riga di mezzo non e' mai esistita sulla blockchain
                new GiacenzeBlockchain.RigaDettaglio("20240101120000_A_1_CM", 50, 0, "9.99"),
                new GiacenzeBlockchain.RigaDettaglio("20240101120000_A_2_SC", 50, 1, "4.99"),
                //rettifica a mano, senza blocco: nessun confronto
                new GiacenzeBlockchain.RigaDettaglio("20240101130000_M_1_RW", -1, 2, "5.99"),
                //riga generata senza blocco nello stesso secondo di una con il blocco, che il filtro non mostra
                new GiacenzeBlockchain.RigaDettaglio("20240101140000_A_1_PC", 60, -1, "-1"),
                new GiacenzeBlockchain.RigaDettaglio("20240101140000_A_2_CM", -1, 3, "-1.5"),
                //secondo con il blocco ma nessuna riga mostrata
                new GiacenzeBlockchain.RigaDettaglio("20240101150000_A_1_DC", 70, -1, "2"));
        List<GiacenzeBlockchain.BloccoDettaglio> b = GiacenzeBlockchain.BlocchiDettaglio(Righe);
        assertEquals(2, b.size());
        assertEquals(50, b.get(0).blocco());
        assertEquals("4.99", b.get(0).archivio());
        assertEquals(List.of(0, 1), b.get(0).righe());
        assertEquals(List.of("20240101120000_A_1_CM", "20240101120000_A_2_SC"), b.get(0).id());
        assertEquals(60, b.get(1).blocco());
        assertEquals("-1.5", b.get(1).archivio());
        assertEquals(List.of(3), b.get(1).righe());
    }

    @Test
    void dettaglioUnSaldoPerBloccoEIDecimaliUnaVolta() {
        NodoBlocchi n = new NodoBlocchi(Long.MAX_VALUE);
        java.util.Map<Long, String> Saldi = new java.util.TreeMap<>();
        List<String> avvisi = new ArrayList<>();
        GiacenzeBlockchain.LeggiAiBlocchi(WALLET, "CRO", new GiacenzeBlockchain.Richiesta("A", "TKA", "Crypto", TOKEN_A),
                List.of(60L, 50L, 60L), null, n, Saldi::putAll, avvisi);
        assertTrue(avvisi.isEmpty(), avvisi.toString());
        assertEquals(java.util.Map.of(50L, "0.5", 60L, "0.6"), Saldi);
        assertEquals(List.of("decimali 60", "token 50", "token 60"), n.chiamate);

        Saldi.clear();
        GiacenzeBlockchain.LeggiAiBlocchi(WALLET, "CRO", new GiacenzeBlockchain.Richiesta("CRO", "CRO", "Crypto", "CRO"),
                List.of(50L), null, n, Saldi::putAll, avvisi);
        assertEquals(java.util.Map.of(50L, "50"), Saldi);
    }

    @Test
    void dettaglioUnNodoCheSiGuastaFermaLaLetturaEIRestantiSonoNonDisponibili() {
        NodoBlocchi n = new NodoBlocchi(55);
        java.util.Map<Long, String> Saldi = new java.util.TreeMap<>();
        List<String> avvisi = new ArrayList<>();
        GiacenzeBlockchain.LeggiAiBlocchi(WALLET, "CRO", new GiacenzeBlockchain.Richiesta("CRO", "CRO", "Crypto", "CRO"),
                List.of(50L, 60L, 70L), null, n, Saldi::putAll, avvisi);
        assertEquals("50", Saldi.get(50L));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, Saldi.get(60L));
        assertEquals(GiacenzeBlockchain.NON_DISPONIBILE, Saldi.get(70L));
        //dopo il primo guasto non si chiede piu' nulla
        assertEquals(List.of("saldo 50"), n.chiamate);
        assertEquals(1, avvisi.size());
    }

    @Test
    void dettaglioLaMonetaDiUnAltraReteONftNonSiLegge() {
        assertNull(GiacenzeBlockchain.RichiestaDettaglio("TKA", "Crypto", TOKEN_A, "BSC", "CRO"));
        assertNull(GiacenzeBlockchain.RichiestaDettaglio("Gatto #1", "NFT", TOKEN_A, "CRO", "CRO"));
        assertNotNull(GiacenzeBlockchain.RichiestaDettaglio("TKA", "Crypto", TOKEN_A, "CRO", "CRO"));
        assertNotNull(GiacenzeBlockchain.RichiestaDettaglio("CRO", "Crypto", "CRO", "CRO", "CRO"));
    }

    @Test
    void sistemaQtaProponeLaGiacenzaCheAllineaLArchivioAFineBlocco() {
        //ultima riga del secondo: la proposta e' il saldo della blockchain
        assertEquals(0, new java.math.BigDecimal("1.5").compareTo(GiacenzeBlockchain.GiacenzaPerAllineare("1.2", "1.2", "1.5")));
        //riga di mezzo (commissione prima dello scambio): si sposta della stessa differenza, lo scambio resta
        assertEquals(0, new java.math.BigDecimal("10.3").compareTo(GiacenzeBlockchain.GiacenzaPerAllineare("10", "1.2", "1.5")));
        //notazione scientifica nell'archivio
        assertEquals(0, new java.math.BigDecimal("0").compareTo(GiacenzeBlockchain.GiacenzaPerAllineare("8.1E-10", "8.1E-10", "0")));
        //niente da allineare: uguale, non letto, in lettura, senza confronto
        assertNull(GiacenzeBlockchain.GiacenzaPerAllineare("1.5", "1.50", "1.5"));
        assertNull(GiacenzeBlockchain.GiacenzaPerAllineare("1.2", "1.2", GiacenzeBlockchain.NON_DISPONIBILE));
        assertNull(GiacenzeBlockchain.GiacenzaPerAllineare("1.2", "1.2", GiacenzeBlockchain.IN_LETTURA));
        assertNull(GiacenzeBlockchain.GiacenzaPerAllineare("1.2", null, "1.5"));
    }
}
