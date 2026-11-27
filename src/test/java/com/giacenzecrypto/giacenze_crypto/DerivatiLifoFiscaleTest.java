package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Il reddito dei derivati e' quello del LIFO ({@link DerivatiLifo#AggiornaRisultati}, passo finale di
 * {@code AggiornaPlusvalenze}): {@code [46]} della chiusura del token = differenziale in LIFO per il prezzo della moneta, e
 * {@code [46]} delle righe di pnl classificate dello stesso fill = zero, per non contare due volte il profitto dell'exchange.
 * Senza i dati il fill ripiega sul profitto dell'exchange. Dati inventati (caso D3: lotti a 30.000 e 40.000, valuta a 0,90 euro).
 */
class DerivatiLifoFiscaleTest {

    @TempDir
    static Path tempDir;

    private static final String CONTRATTO = "BTC-USD_UM_XPERP-991231";
    private static final long DIC = 1_796_000_000_000L;
    private static final long GEN = 1_799_000_000_000L;
    private static int progressivo = 0;

    @BeforeAll
    static void apreDatabase() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        ContoDerivati.P1Disponibile = false;
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void setUp() {
        Principale.InterrompiCiclo = false;
        Principale.MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        ContoDerivati.P1Disponibile = true;   //P2 e' l'unico metodo valido: questi test provano anche P1
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
        OKXDerivati.AzzeraCacheValore();
        ValoriContratti.ImpostaPerTest(java.util.List.of());   //le regole vere hanno una prova a parte
        OKXDerivati.FonteValoreContratto = id -> "1";
    }

    @AfterEach
    void ripristina() {
        OKXDerivati.FonteValoreContratto = id -> "";
        ValoriContratti.ImpostaPerTest(null);
    }

    private static JSONObject fill(String billId, long ts, String subType, String sz, String px, String posBalChg, String pnl) {
        BigDecimal bal = new BigDecimal(posBalChg).negate().add(new BigDecimal(pnl));
        return new JSONObject().put("billId", billId).put("instId", CONTRATTO).put("instType", "FUTURES").put("ccy", "USDC")
                .put("type", "2").put("subType", subType).put("sz", sz).put("px", px).put("balChg", bal.toPlainString())
                .put("posBalChg", posBalChg).put("pnl", pnl).put("fee", "0").put("mgnMode", "isolated")
                .put("ordId", "7" + billId).put("tradeId", "9" + billId).put("ts", String.valueOf(ts));
    }

    private static void acquistoUsdc() {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = "20260101100000_TESTOKX_" + String.format("%03d", ++progressivo) + "_001_AC";
        m[1] = "2026-01-01 10:00";
        m[3] = "OKX";
        m[4] = "Principale";
        m[5] = "ACQUISTO";
        m[8] = "EUR"; m[9] = "FIAT"; m[10] = "-90000";
        m[11] = "USDC"; m[12] = "Crypto"; m[13] = "100000";
        m[15] = "90000";
        m[22] = "M";
        Principale.MappaCryptoWallet.put(m[0], m);
    }

    /** Importa i fill come farebbe lo scaricamento, poi dice ai movimenti il prezzo della valuta e classifica i pnl. */
    private static void importa(boolean conPrezzoMoneta, JSONObject... bills) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bills.length; i++) sb.append(i > 0 ? "," : "").append(bills[i]);
        JsonArray arr = JsonParser.parseString(sb.append("]").toString()).getAsJsonArray();
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(arr, "Trading")));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!"OKX".equals(v[3]) || !Derivati.isDerivato(v)) continue;
            if (conPrezzoMoneta && Derivati.IsRigaContrattoOMargine(v)) {
                Map<String, String> d = Derivati.Dati(v);
                d.put(DerivatiLifo.CHIAVE_PREZZO_MONETA, "0.9");
                Derivati.ImpostaDati(v, d);
            }
            if (Derivati.Tipo(v).equals(Derivati.PNL)) {
                boolean entrata = !v[13].isBlank();
                BigDecimal q = new BigDecimal(entrata ? v[13] : v[10]).abs();
                v[15] = q.multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                v[18] = entrata ? Derivati.CAMPO18_ACCREDITO : Derivati.CAMPO18_ADDEBITO;
                v[5] = entrata ? Derivati.CAMPO5_ACCREDITO : Derivati.CAMPO5_ADDEBITO;
                v[32] = "SI";
            }
        }
    }

    private static List<JSONObject> d3() {
        return List.of(
                fill("1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill("2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill("3", DIC, "2", "1", "45000", "-35000", "10000"),
                fill("4", GEN, "2", "1", "38000", "-35000", "3000"));
    }

    private static String[] riga(String bill, String tipo) {
        return Principale.MappaCryptoWallet.values().stream()
                .filter(v -> bill.equals(v[24]) && Derivati.Tipo(v).equals(tipo)).findFirst().orElseThrow();
    }

    @Test
    void ilRedditoDelContrattoELDelLifoPerIlPrezzoDellaMoneta() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        //5.000 e 8.000 USDC in LIFO (l'exchange ha riconosciuto 10.000 e 3.000), a 0,90 euro
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("7200.00", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        //le aperture non hanno risultato
        assertEquals("", riga("1", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        //il profitto dell'exchange e' denaro vero (doppio effetto sulla valuta) ma non e' reddito dei derivati una seconda volta
        assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        //il margine non ha mai un risultato
        assertEquals("", riga("3", Derivati.MARGINE)[Derivati.CAMPO_RISULTATO]);
    }

    /**
     * Rigo codice 9 del quadro W/RW con P1 (D7, 21/11/2026): colonna 7 = somma dei margini delle aperture dell'anno piu' quello
     * parcheggiato al 1/1, colonna 8 = guadagno non realizzato al 31/12 (prezzo a pronti del sottostante) o guadagno della chiusura, se positivi. Stessi fill di d3:
     * aperture a 30.000 e 40.000, chiusura LIFO di un contratto a dicembre, l'altro chiuso a gennaio a 38.000.
     */
    @Test
    void ilRigoCodice9ConP1_margineEGuadagnoNonRealizzatoPoiChiusura() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            //il valore in euro dei margini (l'importazione vera lo cerca, qui niente rete): 1 USDC = 0,90 euro
            if (Derivati.Tipo(v).equals(Derivati.MARGINE)) {
                String q = !v[13].isBlank() ? v[13] : v[10];
                v[15] = new BigDecimal(q).abs().multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
        }
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Calcoli_RW_Derivati.Prezzo prezzo = (m, ist) -> switch (m) {
            case "BTC" -> new BigDecimal("45000");
            case "USD" -> BigDecimal.ONE;
            case "USDC" -> new BigDecimal("0.9");
            default -> null;
        };
        String annoApertura = riga("1", Derivati.CONTRATTO)[1].substring(0, 4);
        String annoChiusura = riga("4", Derivati.CONTRATTO)[1].substring(0, 4);
        assertNotEquals(annoApertura, annoChiusura);

        Calcoli_RW_Derivati.Rigo dic = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, false, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, new BigDecimal("63000").compareTo(dic.valoreIniziale), "margini delle due aperture (30.000 + 40.000 USDC) a 0,90");
        assertEquals(0, new BigDecimal("13500").compareTo(dic.valoreFinale), "1 contratto aperto a 30.000, 45.000 al 31/12: 15.000 USDC");
        assertEquals("3", dic.colonna14, "redditi da derivati nell'anno (la chiusura di dicembre)");
        assertTrue(dic.avvisi.stream().anyMatch(t -> t.contains("prezzo a pronti")));
        assertTrue(dic.avvisi.stream().anyMatch(t -> t.contains("Stato estero")));
        assertEquals(1, dic.prospetto.size());
        assertTrue(dic.prospetto.get(0)[Calcoli_RW_Derivati.P_VOCE].startsWith("Contratto " + CONTRATTO));
        BigDecimal attesa = new BigDecimal("13500").multiply(new BigDecimal("0.002")).multiply(BigDecimal.valueOf(dic.giorni))
                .divide(BigDecimal.valueOf(365), 2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, attesa.compareTo(dic.ivafe));

        Calcoli_RW_Derivati.Rigo gen = Calcoli_RW_Derivati.Calcola(annoChiusura, Principale.MappaCryptoWallet, false, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, new BigDecimal("31500").compareTo(gen.valoreIniziale), "35.000 USDC ancora parcheggiati al 1/1");
        assertEquals(0, new BigDecimal("7200").compareTo(gen.valoreFinale), "chiusura in LIFO: (38.000 - 30.000) a 0,90");
        assertEquals(annoChiusura + "-01-01", gen.prospetto.get(0)[Calcoli_RW_Derivati.P_DATA_INI]);
    }

    /**
     * Rigo codice 9 con P2, continuita' fra gli anni (23/11/2026): una posizione aperta al 31/12 (stessi fill di d3) porta il guadagno non
     * realizzato nella colonna 8 dell'anno, e lo stesso importo entra nella colonna 7 del tratto che parte dal 1/1 dell'anno dopo, cosi'
     * il valore finale di un anno e il valore iniziale del successivo coincidono. Con P1 resta com'e' (colonna 7 solo il margine).
     */
    @Test
    void ilRigoCodice9ConP2_ilValoreInizialeDellAnnoDopoComprendeIlNonRealizzato() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (Derivati.Tipo(v).equals(Derivati.MARGINE)) {
                String q = !v[13].isBlank() ? v[13] : v[10];
                v[15] = new BigDecimal(q).abs().multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
        }
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Calcoli_RW_Derivati.Prezzo prezzo = (m, ist) -> switch (m) {
            case "BTC" -> new BigDecimal("45000");
            case "USD" -> BigDecimal.ONE;
            case "USDC" -> new BigDecimal("0.9");
            default -> null;
        };
        String annoApertura = riga("1", Derivati.CONTRATTO)[1].substring(0, 4);
        String annoChiusura = riga("4", Derivati.CONTRATTO)[1].substring(0, 4);

        Calcoli_RW_Derivati.Rigo dic = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, true, prezzo, (g, d) -> "")
                .values().iterator().next();
        Calcoli_RW_Derivati.Rigo gen = Calcoli_RW_Derivati.Calcola(annoChiusura, Principale.MappaCryptoWallet, true, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, dic.valoreFinale.compareTo(gen.valoreIniziale),
                "valore finale " + dic.valoreFinale + " e valore iniziale dell'anno dopo " + gen.valoreIniziale);
        assertEquals(0, new BigDecimal("13500").add(new BigDecimal("31500")).compareTo(gen.valoreIniziale),
                "35.000 USDC ancora nel conto al 1/1 (31.500) piu' il non realizzato (13.500): " + gen.valoreIniziale);
        assertTrue(gen.prospetto.get(0)[Calcoli_RW_Derivati.P_AVVISI].contains("risultato non realizzato al 31/12 dell'anno prima"));

        //Il primo anno non ha niente prima: il valore iniziale non cambia
        Calcoli_RW_Derivati.Rigo p1 = Calcoli_RW_Derivati.Calcola(annoChiusura, Principale.MappaCryptoWallet, false, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, new BigDecimal("31500").compareTo(p1.valoreIniziale), "P1 invariato: solo il margine");
    }

    /**
     * Rigo codice 9 con P2 (24/11/2026): il risultato non realizzato delle posizioni aperte al 31/12 entra nel valore finale con il suo segno,
     * come il fair value del conto. Stessi fill di d3: con BTC a 45.000 il contratto aperto a 30.000 vale +15.000 USDC (13.500 euro), con BTC a
     * 20.000 vale -10.000 USDC (-9.000 euro), quindi i due valori finali distano 22.500 euro.
     */
    @Test
    void ilRigoCodice9ConP2_ilNonRealizzatoNegativoSiSottraeAlValoreFinale() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (Derivati.Tipo(v).equals(Derivati.MARGINE)) {
                String q = !v[13].isBlank() ? v[13] : v[10];
                v[15] = new BigDecimal(q).abs().multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
        }
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        java.util.function.Function<String, Calcoli_RW_Derivati.Prezzo> conBtc = btc -> (m, ist) -> switch (m) {
            case "BTC" -> new BigDecimal(btc);
            case "USD" -> BigDecimal.ONE;
            case "USDC" -> new BigDecimal("0.9");
            default -> null;
        };
        String annoApertura = riga("1", Derivati.CONTRATTO)[1].substring(0, 4);
        Calcoli_RW_Derivati.Rigo su = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, true, conBtc.apply("45000"), (g, d) -> "")
                .values().iterator().next();
        Calcoli_RW_Derivati.Rigo giu = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, true, conBtc.apply("20000"), (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, new BigDecimal("22500").compareTo(su.valoreFinale.subtract(giu.valoreFinale)),
                "con BTC a 45.000 " + su.valoreFinale + ", con BTC a 20.000 " + giu.valoreFinale);
        assertTrue(giu.valoreFinale.signum() > 0);
        assertTrue(giu.prospetto.get(giu.prospetto.size() - 1)[Calcoli_RW_Derivati.P_AVVISI].contains("risultato non realizzato"));

    }

    /**
     * Con P2 il non realizzato delle posizioni aperte al 31/12 si valuta con il mark price dell'exchange (OKX, simbolo nativo dal campo dati) e solo
     * se manca, o l'exchange non e' supportato, con lo spot del sottostante, e l'avviso dice quale dei due. Stessi fill di d3: con BTC a 45.000 il
     * contratto aperto a 30.000 vale +15.000 USDC, con un mark price di 40.000 +10.000: 4.500 euro in meno nel valore finale.
     */
    @Test
    void ilRigoCodice9ConP2_ilNonRealizzatoUsaIlMarkPriceDellExchange() throws Exception {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (Derivati.Tipo(v).equals(Derivati.MARGINE)) {
                String q = !v[13].isBlank() ? v[13] : v[10];
                v[15] = new BigDecimal(q).abs().multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
        }
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        Calcoli_RW_Derivati.Prezzo prezzo = (m, ist) -> switch (m) {
            case "BTC" -> new BigDecimal("45000");
            case "USD" -> BigDecimal.ONE;
            case "USDC" -> new BigDecimal("0.9");
            default -> null;
        };
        String annoApertura = riga("1", Derivati.CONTRATTO)[1].substring(0, 4);
        MarkPriceDerivati.Fonte originale = MarkPriceDerivati.FONTE;
        try {
            MarkPriceDerivati.AzzeraMemoria();
            Calcoli_RW_Derivati.Rigo spot = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, true, prezzo, (g, d) -> "")
                    .values().iterator().next();
            assertTrue(spot.avvisi.stream().anyMatch(t -> t.contains("prezzo a pronti del sottostante")), "senza mark price lo spot, e lo dice");
            assertTrue(spot.avvisi.stream().anyMatch(t -> t.contains("mark price non disponibile")), "OKX e' supportato: il mark price manca");

            java.util.List<String> chieste = new java.util.ArrayList<>();
            MarkPriceDerivati.FONTE = (p, s, inverso, minuto) -> {
                chieste.add(p + " " + s);
                return MarkPriceDerivati.Esito.trovato(new BigDecimal("40000"));
            };
            Calcoli_RW_Derivati.Rigo mark = Calcoli_RW_Derivati.Calcola(annoApertura, Principale.MappaCryptoWallet, true, prezzo, (g, d) -> "")
                    .values().iterator().next();
            assertEquals(0, new BigDecimal("4500").compareTo(spot.valoreFinale.subtract(mark.valoreFinale)),
                    "spot " + spot.valoreFinale + ", mark price " + mark.valoreFinale);
            assertTrue(mark.avvisi.stream().anyMatch(t -> t.contains("valutato con il mark price di OKX")), mark.avvisi.toString());
            assertFalse(chieste.isEmpty());
            assertTrue(chieste.get(0).startsWith("OKX " + CONTRATTO), "il simbolo e' quello nativo: " + chieste);
        } finally {
            MarkPriceDerivati.FONTE = originale;
            MarkPriceDerivati.AzzeraMemoria();
            try (java.sql.Statement st = DatabaseH2.connectionPrezzi.createStatement()) {
                st.execute("DELETE FROM PrezziMarkDerivati");
            }
        }
    }

    /** Con la vista filtrata sul solo wallet Principale il reddito in LIFO (sulle righe del token, nel sotto-wallet) resta fuori: la nota lo conta. */
    @Test
    void ilRedditoFuoriVistaContaLeChiusureNelSottoWallet() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        List<String[]> tutti = new java.util.ArrayList<>(Principale.MappaCryptoWallet.values());
        List<String[]> principale = tutti.stream().filter(v -> !OKXDerivati.SOTTOWALLET.equals(v[4])).toList();
        assertEquals(0, DerivatiLifo.RedditiFuoriVista(tutti, Principale.MappaCryptoWallet));
        assertEquals(2, DerivatiLifo.RedditiFuoriVista(principale, Principale.MappaCryptoWallet));
        assertEquals(0, DerivatiLifo.RedditiFuoriVista(List.of(), Principale.MappaCryptoWallet));
    }

    /**
     * Inversione in net mode (forma vista il 19/11/2026 su un conto vero, numeri inventati): long di 1, poi un solo ordine di vendita
     * da 2. OKX lo divide in due bill dello stesso ordine e dello stesso millisecondo, la chiusura del long (bill minore) e
     * l'apertura dello short. Due token distinti a zero, sotto-wallet a zero, LIFO di ogni chiusura uguale al profitto di OKX.
     */
    @Test
    void lInversioneInNetModeSonoDueBillChiusuraEApertura() {
        acquistoUsdc();
        long t = DIC;
        JSONObject chiudeLong = fill("12", t + 60_000L, "2", "1", "29990", "-30000", "-10");
        JSONObject apreShort = fill("13", t + 60_000L, "2", "1", "29990", "29990", "0");
        chiudeLong.put("ordId", "799");
        apreShort.put("ordId", "799");
        importa(true,
                fill("11", t, "1", "1", "30000", "30000", "0"),
                apreShort, chiudeLong,
                fill("14", t + 120_000L, "1", "1", "29980", "-29990", "10"));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();

        Map<String, BigDecimal> saldi = new java.util.TreeMap<>();
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!OKXDerivati.SOTTOWALLET.equals(v[4])) continue;
            if (!v[10].isBlank()) saldi.merge(v[8], new BigDecimal(v[10]), BigDecimal::add);
            if (!v[13].isBlank()) saldi.merge(v[11], new BigDecimal(v[13]), BigDecimal::add);
        }
        assertEquals(3, saldi.size(), "token LONG, token SHORT e USDC: " + saldi);
        saldi.forEach((k, q) -> assertEquals(0, q.signum(), k + " " + q));
        assertTrue(saldi.keySet().stream().anyMatch(k -> k.endsWith(".LONG")));
        assertTrue(saldi.keySet().stream().anyMatch(k -> k.endsWith(".SHORT")));
        //-10 e +10 USDC a 0,90 euro, in LIFO come per OKX
        assertEquals("-9.00", riga("12", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("9.00", riga("14", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("12", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("14", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("", riga("13", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO], "l'apertura dello short non ha risultato");
    }

    @Test
    void ilRedditoDiOgniAnnoEQuelloDelLifoNonQuelloDelPrezzoMedio() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        BigDecimal anno2026 = BigDecimal.ZERO;
        BigDecimal anno2027 = BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            BigDecimal r = Derivati.Risultato(v);
            if (r == null || !"OKX".equals(v[3])) continue;
            if (v[1].startsWith("2026")) anno2026 = anno2026.add(r);
            else if (v[1].startsWith("2027")) anno2027 = anno2027.add(r);
        }
        //con il prezzo medio sarebbero 9.000 e 2.700: il LIFO sposta 4.500 euro dal 2026 al 2027, il totale e' lo stesso
        assertEquals(0, new BigDecimal("4500").compareTo(anno2026));
        assertEquals(0, new BigDecimal("7200").compareTo(anno2027));
    }

    @Test
    void senzaIlValoreDelContrattoResta_ilProfittoDellExchange() {
        acquistoUsdc();
        OKXDerivati.FonteValoreContratto = id -> "";   //il valore del contratto non si e' letto: il LIFO non si puo' calcolare
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        //i pnl restano i profitti dell'exchange, 10.000 e 3.000 USDC a 0,90 euro
        assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("2700.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    /** Un futures che OKX non elenca piu' prende il valore dalla tabella di config/varie, e il LIFO si calcola. */
    @Test
    void unFuturesScadutoPrendeIlValoreDallaTabella() {
        acquistoUsdc();
        OKXDerivati.FonteValoreContratto = id -> "";   //OKX non lo elenca
        ValoriContratti.ImpostaPerTest(java.util.List.of(new ValoriContratti.Regola("BTC-USD_UM_XPERP", "FUTURES", null, null, "1")));
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("1", Derivati.Dati(riga("3", Derivati.CONTRATTO)).get("valoreContratto"));
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    /** Un valore sbagliato in tabella non cambia il reddito: il controllo per posizione lo scarta. */
    @Test
    void unValoreSbagliatoInTabellaLasciaIlProfittoDellExchange() {
        acquistoUsdc();
        OKXDerivati.FonteValoreContratto = id -> "";
        ValoriContratti.ImpostaPerTest(java.util.List.of(new ValoriContratti.Regola("BTC-USD_UM_XPERP", "FUTURES", null, null, "100")));
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("2700.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void ilRicalcoloIncrementaleNonRompeIlRisultato() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        //una modifica che riguarda un movimento qualunque: la passata incrementale riscrive i [46] dei movimenti rielaborati
        Principale.MappaCryptoWallet.firstEntry().getValue()[15] = "89999";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    @Test
    void conIPrezziIncoerentiRestaIlProfittoDellExchange() {
        acquistoUsdc();
        //il profitto di 5.000 non si ricostruisce dai prezzi (che darebbero 15): niente LIFO, resta il pnl dell'exchange (4.500 euro a 0,90)
        importa(true,
                fill("1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill("2", DIC, "2", "1", "30015", "-30000", "5000"));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("2", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("4500.00", riga("2", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    /**
     * Un episodio aperto al 31/12 la cui chiusura dell'anno dopo non torna con i prezzi (D18.9): la chiusura di dicembre resta in
     * LIFO (il suo anno può essere già dichiarato), quella di gennaio torna al profitto dell'exchange e riceve il conguaglio, la
     * differenza fra exchange e LIFO di dicembre. Stesso risultato con la passata incrementale e con quella completa: il punto di
     * controllo a ogni movimento fa ripartire la passata davvero dopo la prima chiusura (bug B14).
     */
    @Test
    void unaChiusuraDellAnnoDopoCheNonTornaLasciaLAnnoPrimaEPortaIlConguaglio() {
        int vecchio = Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT;
        Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = 1;
        try {
            acquistoUsdc();
            List<JSONObject> f = d3();
            importa(true, f.get(0), f.get(1), f.get(2));
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            //un'apertura in piu': la prima importazione cambia anche le tabelle che il motore legge, da qui le passate sono incrementali
            importa(true, fill("5", DIC + 1_000_000L, "1", "1", "41000", "41000", "0"));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            //la chiusura di gennaio dichiara 5.000 dove i prezzi non lo danno: l'episodio non torna piu' nel 2027
            importa(true, fill("4", GEN, "2", "1", "38000", "-35000", "5000"));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa, "la prova vale solo se la passata e' incrementale");
            ControllaConguaglioDiGennaio();
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            ControllaConguaglioDiGennaio();
        } finally {
            Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = vecchio;
        }
    }

    private static void ControllaConguaglioDiGennaio() {
        //dicembre non cambia: 5.000 USDC in LIFO a 0,90 euro, il pnl dell'exchange a zero
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        //gennaio: profitto dell'exchange (5.000 a 0,90) piu' il conguaglio, exchange meno LIFO di dicembre (9.000 - 4.500)
        assertEquals("4500.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        String[] gennaio = riga("4", Derivati.CONTRATTO);
        assertEquals("4500.00", gennaio[Derivati.CAMPO_RISULTATO]);
        assertEquals("4500.00", Derivati.Dati(gennaio).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
        assertEquals("2026", Derivati.Dati(gennaio).get(DerivatiLifo.CHIAVE_CONGUAGLIO_ANNI));
        assertTrue(DerivatiLifo.TestoConguaglio(gennaio).startsWith("Conguaglio di 4500.00 euro."));
        assertFalse(DerivatiLifo.TestoConguaglio(gennaio).contains(";"));
        assertEquals("", DerivatiLifo.TestoConguaglio(riga("3", Derivati.CONTRATTO)));
        //il totale dell'episodio e' quello dell'exchange: 10.000 + 5.000 USDC a 0,90
        assertEquals(0, new BigDecimal("13500").compareTo(TotaleOKX()));
    }

    private static BigDecimal TotaleOKX() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            BigDecimal r = Derivati.Risultato(v);
            if (r != null && "OKX".equals(v[3])) t = t.add(r);
        }
        return t;
    }

    /** Senza la chiusura che non tornava il conguaglio sparisce, anche dal campo dati, e gennaio torna in LIFO. */
    @Test
    void tolta_laChiusuraCheNonTornava_ilConguaglioSparisce() {
        acquistoUsdc();
        List<JSONObject> f = d3();
        importa(true, f.get(0), f.get(1), f.get(2), fill("4", GEN, "2", "1", "38000", "-35000", "5000"));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("4500.00", Derivati.Dati(riga("4", Derivati.CONTRATTO)).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
        //la chiusura giusta al posto di quella sbagliata (stesso fill, profitto 3.000 come danno i prezzi)
        Principale.MappaCryptoWallet.values().removeIf(v -> "4".equals(v[24]) || "4-DRV".equals(v[24]));
        importa(true, f.get(3));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        String[] gennaio = riga("4", Derivati.CONTRATTO);
        assertEquals("7200.00", gennaio[Derivati.CAMPO_RISULTATO]);
        assertNull(Derivati.Dati(gennaio).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
        assertEquals("", DerivatiLifo.TestoConguaglio(gennaio));
    }

    /**
     * Il conguaglio sta nel primo anno che non torna anche se la chiusura di quell'anno ha il pnl non ancora classificato: prima
     * scivolava sulla prima chiusura classificata, un anno dopo. Tre aperture (30.000, 40.000, 50.000, medio 40.000): dicembre chiude a
     * 45.000 (exchange +5.000, LIFO -5.000 sul lotto a 50.000), gennaio dichiara 5.000 dove i prezzi danno -2.000, l'anno dopo chiude
     * a 41.000 con +1.000.
     */
    @Test
    void ilConguaglioRestaNelPrimoAnnoCheNonTornaAncheConIlPnlDaClassificare() {
        acquistoUsdc();
        acquistoUsdc();   //120.000 di margine
        long annoDopo = GEN + 365L * 86_400_000L;
        importa(true,
                fill("1", DIC - 30_000_000L, "1", "1", "30000", "30000", "0"),
                fill("2", DIC - 20_000_000L, "1", "1", "40000", "40000", "0"),
                fill("3", DIC - 10_000_000L, "1", "1", "50000", "50000", "0"),
                fill("4", DIC, "2", "1", "45000", "-40000", "5000"),
                fill("5", GEN, "2", "1", "38000", "-40000", "5000"),
                fill("6", annoDopo, "2", "1", "41000", "-40000", "1000"));
        String[] pnlGennaio = riga("5", Derivati.PNL);
        pnlGennaio[18] = "";
        pnlGennaio[5] = "DEPOSITO CRYPTO";
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        //dicembre in LIFO: -5.000 a 0,90
        assertEquals("-4500.00", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        //gennaio: il conguaglio, exchange meno LIFO di dicembre (4.500 + 4.500), sulla riga di token
        String[] gennaio = riga("5", Derivati.CONTRATTO);
        assertEquals("9000.00", gennaio[Derivati.CAMPO_RISULTATO]);
        assertEquals("9000.00", Derivati.Dati(gennaio).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
        assertEquals("", riga("5", Derivati.PNL)[Derivati.CAMPO_RISULTATO], "pnl da classificare: nessun reddito da derivati");
        //l'anno dopo resta al profitto dell'exchange, senza conguaglio
        String[] dopo = riga("6", Derivati.CONTRATTO);
        assertEquals("", dopo[Derivati.CAMPO_RISULTATO]);
        assertNull(Derivati.Dati(dopo).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
        assertEquals("900.00", riga("6", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
    }

    /** Un movimento di pnl di un altro exchange con lo stesso identificativo di fill non e' toccato dal LIFO di OKX. */
    @Test
    void lIdentificativoDelFillValePerExchange() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        String[] altro = new String[Importazioni.ColonneTabella];
        Arrays.fill(altro, "");
        altro[0] = "20261130100000_TESTALTRO_001_001_DC";
        altro[1] = "2026-11-30 10:00";
        altro[3] = "AltroExchange";
        altro[4] = "Principale";
        altro[5] = Derivati.CAMPO5_ACCREDITO;
        altro[11] = "USDC"; altro[12] = "Crypto"; altro[13] = "100";
        altro[15] = "90.00";
        altro[18] = Derivati.CAMPO18_ACCREDITO;
        altro[22] = "A";
        altro[24] = "3";
        altro[32] = "SI";
        altro[Derivati.CAMPO] = Derivati.PNL;
        Principale.MappaCryptoWallet.put(altro[0], altro);
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("90.00", altro[Derivati.CAMPO_RISULTATO]);
    }

    /** Due chiusure nello stesso anno, la seconda non torna: tutte e due al profitto dell'exchange, nessun conguaglio. */
    @Test
    void nelloStessoAnnoUnaChiusuraCheNonTornaRiportaTuttoAllExchangeSenzaConguaglio() {
        acquistoUsdc();
        importa(true,
                fill("1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                fill("2", DIC - 10_000_000L, "1", "1", "40000", "40000", "0"),
                fill("3", DIC, "2", "1", "45000", "-35000", "10000"),
                fill("4", DIC + 1_000_000L, "2", "1", "38000", "-35000", "5000"));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals("", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("4500.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertNull(Derivati.Dati(riga("4", Derivati.CONTRATTO)).get(DerivatiLifo.CHIAVE_CONGUAGLIO));
    }

    /**
     * La verifica vale per posizione (episodio: da un'apertura al ritorno a zero contratti), non per l'intera vita del token:
     * una chiusura che non torna in una posizione successiva non cambia il reddito di una posizione già chiusa. Stesso esito con la
     * passata incrementale e con quella completa.
     */
    @Test
    void unaChiusuraCheNonTornaInUnaPosizioneSuccessivaNonCambiaQuellaPrima() {
        int vecchio = Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT;
        Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = 1;
        try {
            acquistoUsdc();
            //prima posizione: aperta a 30.000, chiusa a 35.000, profitto 5.000 che torna con i prezzi
            importa(true,
                    fill("1", DIC - 20_000_000L, "1", "1", "30000", "30000", "0"),
                    fill("2", DIC - 10_000_000L, "2", "1", "35000", "-30000", "5000"));
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("4500.00", riga("2", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            importa(true, fill("3", DIC, "1", "1", "40000", "40000", "0"));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            //seconda posizione: chiusa a 38.000 con un profitto dichiarato di 5.000 dove i prezzi danno una perdita di 2.000
            importa(true, fill("4", GEN, "2", "1", "38000", "-40000", "5000"));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("4500.00", riga("2", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            assertEquals("0.00", riga("2", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            assertEquals("", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            assertEquals("4500.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("4500.00", riga("2", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            assertEquals("0.00", riga("2", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            assertEquals("", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
            assertEquals("4500.00", riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        } finally {
            Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = vecchio;
        }
    }

    /**
     * Se le righe di token e di margine di un fill spariscono (cancellate a mano) la sua riga di pnl torna al profitto
     * dell'exchange anche con la passata incrementale, che ripartendo dalle righe tolte non la rielabora: prima restava a 0.00
     * e il reddito di quella chiusura spariva. Stesso esito della passata completa.
     */
    @Test
    void senzaPiuIRigheDiTokenIlPnlTornaAlProfittoDellExchange() {
        int vecchio = Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT;
        Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = 1;
        try {
            acquistoUsdc();
            importa(true, d3().toArray(new JSONObject[0]));
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            //una passata incrementale qualunque, perche' la prossima lo sia davvero
            importa(true, fill("5", GEN + 1_000_000L, "1", "1", "41000", "41000", "0"));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            //le righe tolte hanno un ID che viene dopo quello della riga di pnl dello stesso fill
            Principale.MappaCryptoWallet.values().removeIf(v -> "3".equals(v[24]) && Derivati.IsRigaContrattoOMargine(v));
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertFalse(Calcoli_PlusvalenzeNew.UltimaPassata_Completa, "la prova vale solo se la passata e' incrementale");
            assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            String pnl4 = riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO];
            String token4 = riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO];
            Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            assertEquals(pnl4, riga("4", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
            assertEquals(token4, riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        } finally {
            Calcoli_PlusvalenzeNew.MOVIMENTI_PER_CHECKPOINT = vecchio;
        }
    }

    /** Il reddito in LIFO si conta solo se il profitto dell'exchange dello stesso fill e' classificato come derivato. */
    @Test
    void senzaIlPnlClassificatoIlTokenNonHaReddito() {
        acquistoUsdc();
        importa(true, d3().toArray(new JSONObject[0]));
        String[] pnl3 = riga("3", Derivati.PNL);
        pnl3[18] = "";
        pnl3[5] = "DEPOSITO CRYPTO";
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        //l'altra chiusura, con il pnl classificato, resta in LIFO
        assertEquals("7200.00", riga("4", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
    }

    /** L'import OKX classifica da se' il profitto dell'exchange: il LIFO del token si applica senza alcun intervento. */
    @Test
    void lImportOKXClassificaDaSeIlProfittoDellExchange() {
        acquistoUsdc();
        StringBuilder sb = new StringBuilder("[");
        List<JSONObject> f = d3();
        for (int i = 0; i < f.size(); i++) sb.append(i > 0 ? "," : "").append(f.get(i));
        JsonArray arr = JsonParser.parseString(sb.append("]").toString()).getAsJsonArray();
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(CcxtInterop.convertOKXBills(arr, "Trading")));
        assertEquals(Derivati.CAMPO18_ACCREDITO, riga("3", Derivati.PNL)[18]);
        assertEquals(Derivati.CAMPO18_ACCREDITO, riga("4", Derivati.PNL)[18]);
        //le righe di contratto e di margine restano come le scrive l'import
        assertEquals("", riga("3", Derivati.MARGINE)[18]);
    }

    /**
     * Il valore del contratto non letto per un errore di rete non resta vuoto per sempre: non si ricorda l'errore, e il primo
     * import OKX dopo completa le righe gia' salvate e rifa' il LIFO, anche se non porta nessun movimento nuovo.
     */
    @Test
    void ilValoreDelContrattoNonLettoSiCompletaAlPrimoImportDopo() {
        acquistoUsdc();
        OKXDerivati.FonteValoreContratto = id -> null;   //rete assente
        importa(true, d3().toArray(new JSONObject[0]));
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("9000.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertNull(Derivati.Dati(riga("3", Derivati.CONTRATTO)).get("valoreContratto"));

        OKXDerivati.FonteValoreContratto = id -> "1";   //la rete torna
        Prezzi.SenzaValorizzare(() -> Importazioni.Ex_OKX_ImportaDaAPI(new java.util.ArrayList<>()));
        assertEquals("1", Derivati.Dati(riga("3", Derivati.CONTRATTO)).get("valoreContratto"));
        assertEquals("1", Derivati.Dati(riga("3", Derivati.MARGINE)).get("valoreContratto"));
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
        assertEquals("0.00", riga("3", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        //e la passata seguente, che non vede modifiche, non lo perde
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertEquals("4500.00", riga("3", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO]);
    }

    /** Un contratto che OKX non elenca (scaduto) si ricorda: non si richiede a ogni fill. */
    @Test
    void unContrattoNonElencatoSiChiedeUnaVolta() {
        int[] chiamate = {0};
        OKXDerivati.FonteValoreContratto = id -> { chiamate[0]++; return ""; };
        assertEquals("", OKXDerivati.ValoreContratto("BTC-USDT-250101"));
        assertEquals("", OKXDerivati.ValoreContratto("BTC-USDT-250101"));
        assertEquals(1, chiamate[0]);
        OKXDerivati.FonteValoreContratto = id -> { chiamate[0]++; return null; };
        OKXDerivati.ValoreContratto("ETH-USDT-250101");
        OKXDerivati.ValoreContratto("ETH-USDT-250101");
        assertEquals(3, chiamate[0], "un errore di rete si riprova");
    }

    // ---------------------------------------------------------------------------------------------
    // Forma "conto" di OKX con le due letture del conto derivati (decisione dell'utente del 22/11/2026)
    // ---------------------------------------------------------------------------------------------

    private static void movimentoManuale(String id, String data, String monetaU, String qtaU, String monetaE, String qtaE, String valore,
            String campo5) {
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        m[1] = data;
        m[3] = "OKX";
        m[4] = "Principale";
        m[5] = campo5;
        m[8] = monetaU; m[9] = monetaU.equals("EUR") ? "FIAT" : monetaU.isEmpty() ? "" : "Crypto"; m[10] = qtaU;
        m[11] = monetaE; m[12] = monetaE.equals("EUR") ? "FIAT" : monetaE.isEmpty() ? "" : "Crypto"; m[13] = qtaE;
        m[15] = valore;
        m[22] = "M";
        Principale.MappaCryptoWallet.put(m[0], m);
    }

    /**
     * L'esempio del documento per il forum, importato da OKX: 1.000 USDC comprati a 0,92 euro, il 1/3 un long di 1 contratto a 60.000
     * con 500 USDC di margine (USDC a 0,90), il 31/5 la chiusura a 59.800 con una perdita di 200 USDC (USDC a 0,88), il 30/6 la vendita
     * degli 800 USDC a 0,88. Nella forma "conto" la perdita esce dal sotto-wallet dei derivati e rientrano 300 USDC.
     */
    private static void esempioPerdita() {
        movimentoManuale("20250201100000_TESTOKX_001_001_AC", "2025-02-01 10:00", "EUR", "-920", "USDC", "1000", "920.00", "ACQUISTO CRYPTO");
        long marzo = 1_740_823_200_000L, maggio = 1_748_685_600_000L;
        importa(false, fill("11", marzo, "1", "1", "60000", "500", "0"), fill("12", maggio, "2", "1", "59800", "-500", "-200"));
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!"OKX".equals(v[3]) || !Derivati.isDerivato(v) || Derivati.Tipo(v).equals(Derivati.CONTRATTO)) continue;
            BigDecimal prezzo = new BigDecimal("11".equals(v[24]) ? "0.90" : "0.88");
            BigDecimal q = new BigDecimal(!v[13].isBlank() ? v[13] : v[10]).abs();
            v[15] = q.multiply(prezzo).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (!"OKX".equals(v[3]) || !Derivati.IsRigaContrattoOMargine(v)) continue;
            Map<String, String> d = Derivati.Dati(v);
            d.put(DerivatiLifo.CHIAVE_PREZZO_MONETA, "11".equals(v[24]) ? "0.90" : "0.88");
            Derivati.ImpostaDati(v, d);
        }
        movimentoManuale("20250630100000_TESTOKX_002_001_VC", "2025-06-30 10:00", "USDC", "-800", "EUR", "704", "704.00", "VENDITA CRYPTO");
    }

    private static BigDecimal sommaDerivati() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    private static BigDecimal sommaCripto() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if ("S".equalsIgnoreCase(v[33]) && Funzioni.isNumeric(v[19], false)) t = t.add(new BigDecimal(v[19]));
        }
        return t;
    }

    private static void ricalcola(boolean contoDerivati) {
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, contoDerivati ? "SI" : "NO");
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
    }

    @Test
    void formaConto_laPerditaEsceDalContoERientraIlNetto() {
        esempioPerdita();
        String[] perdita = riga("12", Derivati.PNL);
        assertEquals(OKXDerivati.SOTTOWALLET, perdita[4], "la perdita la paga il conto derivati");
        assertEquals(Derivati.CAMPO18_ADDEBITO, perdita[18], "nasce classificata come ogni perdita dei derivati");
        List<String[]> giro = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> "12".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.MARGINE)).toList();
        assertEquals(2, giro.size());
        for (String[] v : giro) {
            boolean entra = !v[13].isBlank();
            assertEquals(entra ? "Principale" : OKXDerivati.SOTTOWALLET, v[4]);
            assertEquals(0, new BigDecimal("300").compareTo(new BigDecimal(entra ? v[13] : v[10]).abs()), "rientrano 500 - 200 USDC");
        }
        //la perdita viene prima del giroconto in uscita, nello stesso secondo
        assertTrue(perdita[0].compareToIgnoreCase(giro.stream().filter(v -> !v[10].isBlank()).findFirst().orElseThrow()[0]) < 0);
    }

    @Test
    void formaConto_letturaA_ilMargineResteCriptoEIlLifoValeLaPerditaAlValore() {
        esempioPerdita();
        ricalcola(false);
        assertEquals("-176.00", riga("12", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO], "-200 USDC in LIFO a 0,88");
        assertEquals("0.00", riga("12", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        assertEquals(0, new BigDecimal("-176").compareTo(sommaDerivati()), "c-quater " + sommaDerivati());
        //-8 sui 200 USDC persi (valgono 176 e costano 184) e -32 alla vendita (704 contro 736)
        assertEquals(0, new BigDecimal("-40").compareTo(sommaCripto()), "c-sexies " + sommaCripto());
    }

    @Test
    void formaConto_letturaB_comeBybitEBinance_ilLifoNonPerdeLaDifferenzaFraCostoEValore() {
        esempioPerdita();
        ricalcola(true);
        assertEquals("-176.00", riga("12", Derivati.CONTRATTO)[Derivati.CAMPO_RISULTATO], "-200 USDC in LIFO a 0,88");
        //la perdita pagata dal conto vale il costo dei 200 USDC (180): sulla riga resta la differenza col valore (176)
        assertEquals("-4.00", riga("12", Derivati.PNL)[Derivati.CAMPO_RISULTATO]);
        String[] rientro = Principale.MappaCryptoWallet.values().stream()
                .filter(v -> "12".equals(v[24]) && Derivati.Tipo(v).equals(Derivati.MARGINE) && !v[10].isBlank()).findFirst().orElseThrow();
        assertEquals("-6.00", rientro[Derivati.CAMPO_RISULTATO], "300 USDC valgono 264 e nel conto costano 270");
        assertEquals(0, new BigDecimal("-186").compareTo(sommaDerivati()), "c-quater " + sommaDerivati());
        //-10 al passaggio al conto (450 contro 460) e -20 alla vendita (704 contro 460 + 264)
        assertEquals(0, new BigDecimal("-30").compareTo(sommaCripto()), "c-sexies " + sommaCripto());
    }
}
