package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Il token del contratto dai CSV di Bybit ({@link DerivatiDaCsv}, chiave {@code contrattiDerivati} di {@code Bybit Derivati.json} e
 * {@code Bybit UTA.json}). Dati inventati, USDT a 1 euro (controvalore sintetico in coda alla riga, niente rete).
 *
 * <p>Esempio del conto Derivati: long BTCUSDT, apre 2 a 100 e 1 a 130 (prezzo medio 110), chiude 1 a 150 (Bybit: +40, LIFO sul lotto a
 * 130: +20) e 2 a 120 (Bybit: +20, LIFO sui due a 100: +40). Totale 60 in entrambi i metodi. Commissioni 0,10 0,05 0,05 0,10, comprese
 * nella variazione di saldo: il netto dell'exchange e' 59,70. Il token della chiusura porta il LIFO meno la commissione compresa nella sua
 * riga del profitto (19,95 e 39,90), che torna a zero.</p>
 */
class DerivatiDaCsvTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
        Funzioni.ReteDisabilitataPerTest = true;
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiude() throws Exception {
        ContoDerivati.P1Disponibile = false;
        Funzioni.ReteDisabilitataPerTest = false;
        System.clearProperty("prezzi.servizio.abilitato");
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void archivio() {
        MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        ContoDerivati.P1Disponibile = true;   //P2 e' l'unico metodo valido: questi test provano anche P1
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
    }

    private static ConfigurazioneImport contratti() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit Derivati.json");
        c.colonnaValoreEuro = 16;
        return c;
    }

    private static ConfigurazioneImport uta() throws Exception {
        ConfigurazioneImport c = ConfigurazioneImport.carica("config/import/Bybit UTA.json");
        c.colonnaValoreEuro = 15;
        return c;
    }

    /**
     * Conto Derivati: {@code Time,Currency,Contract,Type,Direction,Quantity,Position,Filled Price,Funding,Fee Paid,Cash Flow,Change,
     * Wallet Balance,Fee Rate,Trade ID,Order ID} + [16] controvalore = |Change|.
     */
    private static String[] rigaContratti(String time, String direction, String qty, String pos, String prezzo, String fee, String cashFlow,
            String tradeId) {
        BigDecimal change = new BigDecimal(cashFlow).subtract(new BigDecimal(fee));
        return new String[]{time, "USDT", "BTCUSDT", "trade", direction, qty, pos, prezzo, "0", fee, cashFlow, change.toPlainString(), "0",
            "0.0005", tradeId, "ord-" + tradeId, change.abs().toPlainString()};
    }

    private static List<String[]> esempioContratti() {
        List<String[]> r = new ArrayList<>();
        //nell'ordine del file, dal piu' recente
        r.add(rigaContratti("2025-03-04 10:00:00", "Close Long", "2", "0", "120", "0.10", "20", "id-4"));
        r.add(rigaContratti("2025-03-03 10:00:00", "Close Long", "1", "2", "150", "0.05", "40", "id-3"));
        r.add(rigaContratti("2025-03-02 10:00:00", "Open Long", "1", "3", "130", "0.05", "0", "id-2"));
        r.add(rigaContratti("2025-03-01 10:00:00", "Open Long", "2", "2", "100", "0.10", "0", "id-1"));
        return r;
    }

    private static void importa(List<String[]> righe, ConfigurazioneImport c) {
        Importazioni.AzzeraContatori();
        assertTrue(ImportazioneGenerica.importaRighe(new ArrayList<>(righe), c, false, null, false));
    }

    private static List<String[]> token() {
        List<String[]> t = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) if (DerivatiLifo.IsRigaToken(v)) t.add(v);
        return t;
    }

    private static String[] tokenDi(String id) {
        for (String[] v : token()) if (id.equals(v[24])) return v;
        return null;
    }

    private static List<String[]> profittiDi(String id) {
        List<String[]> l = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (!DerivatiLifo.IsRigaToken(v) && id.equals(v[24]) && Derivati.Tipo(v).equals(Derivati.PNL)) l.add(v);
        }
        return l;
    }

    private static BigDecimal totaleDerivati() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    private static void assertImporto(String atteso, String valore, String messaggio) {
        assertNotNull(valore, messaggio);
        assertFalse(valore.isBlank(), messaggio);
        assertEquals(0, new BigDecimal(atteso).compareTo(new BigDecimal(valore)), messaggio + ": " + valore);
    }

    @Test
    void contoDerivati_unTokenPerEsecuzione_lifoNegliEpisodiVerificati_alNettoDellaCommissione() throws Exception {
        importa(esempioContratti(), contratti());
        assertEquals(4, token().size());
        String[] apertura = tokenDi("id-1");
        assertNotNull(apertura);
        assertEquals("DERIV.BTC-USDT-BYBIT.LONG", apertura[11]);
        assertEquals(OKXDerivati.SOTTOWALLET, apertura[4]);
        assertEquals("Bybit", Derivati.Dati(apertura).get("piattaforma"));
        assertEquals("1", Derivati.Dati(apertura).get("valoreContratto"));
        for (String id : List.of("id-1", "id-2", "id-3", "id-4")) assertEquals(1, profittiDi(id).size(), "riga del profitto collegata a " + id);

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        DerivatiLifo.Posizione p = DerivatiLifo.Calcola(MappaCryptoWallet.values()).get("DERIV.BTC-USDT-BYBIT.LONG");
        assertTrue(p.verificata(), "il Cash Flow torna con il prezzo medio sui prezzi del file");
        assertImporto("19.95", tokenDi("id-3")[Derivati.CAMPO_RISULTATO], "chiusura di 1 sul lotto a 130, meno la commissione");
        assertImporto("39.90", tokenDi("id-4")[Derivati.CAMPO_RISULTATO], "chiusura di 2 sui lotti a 100, meno la commissione");
        assertImporto("0.00", profittiDi("id-3").get(0)[Derivati.CAMPO_RISULTATO], "il profitto netto dell'exchange azzerato");
        assertImporto("0.00", profittiDi("id-4").get(0)[Derivati.CAMPO_RISULTATO], "il profitto netto dell'exchange azzerato");
        assertImporto("59.70", totaleDerivati().toPlainString(), "totale = netto dell'exchange, commissioni comprese");
    }

    /** Con il conto derivati come derivato (P2) il LIFO delle chiusure e' lo stesso: il token sta sul conto ma non ha valore. */
    @Test
    void contoDerivatiConP2_stessoLifo() throws Exception {
        importa(esempioContratti(), contratti());
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "SI");
        try {
            Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
            assertImporto("19.95", tokenDi("id-3")[Derivati.CAMPO_RISULTATO], "chiusura di 1 sul lotto a 130");
            assertImporto("39.90", tokenDi("id-4")[Derivati.CAMPO_RISULTATO], "chiusura di 2 sui lotti a 100");
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
        }
    }

    @Test
    void reimportare_nessunDoppione() throws Exception {
        importa(esempioContratti(), contratti());
        int movimenti = MappaCryptoWallet.size();
        importa(esempioContratti(), contratti());
        assertEquals(movimenti, MappaCryptoWallet.size());
        assertEquals(4, token().size());
    }

    @Test
    void archivioImportatoSenzaToken_reimportareAggiungeITokenEliCollega() throws Exception {
        ConfigurazioneImport senza = contratti();
        senza.contrattiDerivati = null;
        importa(esempioContratti(), senza);
        assertEquals(0, token().size());
        int movimenti = MappaCryptoWallet.size();
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertImporto("59.70", totaleDerivati().toPlainString(), "prima: il profitto dell'exchange");

        importa(esempioContratti(), contratti());
        assertEquals(movimenti + 4, MappaCryptoWallet.size(), "solo i token");
        for (String id : List.of("id-1", "id-2", "id-3", "id-4")) assertEquals(1, profittiDi(id).size(), "collegata " + id);
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertImporto("19.95", tokenDi("id-3")[Derivati.CAMPO_RISULTATO], "LIFO");
        assertImporto("59.70", totaleDerivati().toPlainString(), "stesso totale");
    }

    @Test
    void episodioNonVerificato_restaIlProfittoDellExchange() throws Exception {
        List<String[]> r = esempioContratti();
        //prezzo di chiusura in un'altra unita' (come i primi mesi del 2022): il Cash Flow non torna
        r.set(1, rigaContratti("2025-03-03 10:00:00", "Close Long", "1", "2", "0.0015", "0.05", "40", "id-3"));
        importa(r, contratti());
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(Funzioni.noData(tokenDi("id-3")[Derivati.CAMPO_RISULTATO]));
        assertTrue(Funzioni.noData(tokenDi("id-4")[Derivati.CAMPO_RISULTATO]));
        assertImporto("39.95", profittiDi("id-3").get(0)[Derivati.CAMPO_RISULTATO], "il netto dell'exchange");
        assertImporto("59.70", totaleDerivati().toPlainString(), "totale dell'exchange");
    }

    @Test
    void chiusuraSenzaRigaDelProfittoCollegata_restaIlProfittoDellExchange() throws Exception {
        importa(esempioContratti(), contratti());
        for (String[] v : profittiDi("id-3")) v[24] = "";
        //il collegamento ora manca: la riga c'e' ma il fill del token non la vede
        String[] t = tokenDi("id-3");
        t[24] = "altro";
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(Funzioni.noData(t[Derivati.CAMPO_RISULTATO]), "senza il profitto collegato niente LIFO: conterebbe due volte");
    }

    /** Una chiusura senza apertura fra i token (posizione aperta prima del file) non diventa un token: il token non va sotto zero. */
    @Test
    void chiusuraSenzaApertura_nessunToken_chiusuraPiuGrande_ridotta() throws Exception {
        List<String[]> r = esempioContratti();
        //prima di tutto, una chiusura di una posizione aperta prima del file
        r.add(rigaContratti("2025-02-20 10:00:00", "Close Long", "1", "0", "90", "0.05", "-5", "id-0"));
        //e l'ultima chiusura dice 3 contratti invece di 2 (uno aperto prima del file)
        //(Bybit calcola il profitto sui 3 contratti: 3 x (120 - 110) = 30)
        r.set(0, rigaContratti("2025-03-04 10:00:00", "Close Long", "3", "0", "120", "0.10", "30", "id-4"));
        importa(r, contratti());
        assertNull(tokenDi("id-0"), "nessun token per la chiusura senza apertura");
        assertEquals(1, profittiDi("id-0").size(), "il profitto resta, collegato");
        String[] ridotta = tokenDi("id-4");
        assertEquals("-2", ridotta[10], "chiude solo i 2 contratti aperti fra i token");
        assertEquals("3", Derivati.Dati(ridotta).get(DerivatiDaCsv.CHIAVE_CONTRATTI_BORSA));
        BigDecimal saldo = BigDecimal.ZERO;
        for (String[] v : token()) saldo = saldo.add(new BigDecimal(Funzioni.noData(v[13]) ? v[10] : v[13]));
        assertEquals(0, saldo.signum(), "il token torna a zero, mai sotto");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        assertTrue(Funzioni.noData(ridotta[Derivati.CAMPO_RISULTATO]), "l'episodio non torna: resta il profitto dell'exchange");
        assertImporto("29.90", profittiDi("id-4").get(0)[Derivati.CAMPO_RISULTATO], "il netto dell'exchange");
    }

    /**
     * UTA: {@code Uid,Currency,Contract,Type,Direction,Quantity,Position,Filled Price,Funding,Fee Paid,Cash Flow,Change,Wallet Balance,
     * Action,Time(UTC)} + [15] controvalore. Due aperture nello stesso secondo (nel file nell'ordine sbagliato), una chiusura.
     */
    private static String[] rigaUta(String direction, String action, String qty, String pos, String prezzo, String fee, String cashFlow,
            String time) {
        BigDecimal change = new BigDecimal(cashFlow).add(new BigDecimal(fee));
        //il movimento del profitto e' il lordo (Cash Flow): il controvalore sintetico e' il suo, cosi' l'USDT vale 1 euro
        BigDecimal valore = new BigDecimal(cashFlow).signum() != 0 ? new BigDecimal(cashFlow).abs() : new BigDecimal(fee).abs();
        return new String[]{"1", "USDT", "ETHUSDT", "TRADE", direction, qty, pos, prezzo, "0", fee, cashFlow, change.toPlainString(), "0",
            action, time, valore.toPlainString()};
    }

    @Test
    void uta_aperturNelloStessoSecondo_ordinateDallaPosizione_idSintetico_commissioneASe() throws Exception {
        List<String[]> r = new ArrayList<>();
        r.add(rigaUta("SELL", "CLOSE", "3", "0", "20", "-0.03", "24", "2025-05-02 09:00:00"));
        r.add(rigaUta("BUY", "OPEN", "2", "2", "10", "-0.02", "0", "2025-05-01 09:00:00"));
        r.add(rigaUta("BUY", "OPEN", "1", "3", "16", "-0.01", "0", "2025-05-01 09:00:00"));
        importa(r, uta());
        List<String[]> t = token();
        assertEquals(3, t.size());
        t.sort(OKXDerivati.ORDINE_ESECUZIONE);
        assertEquals("2", Derivati.Dati(t.get(0)).get("posizione"), "prima l'apertura che porta la posizione a 2");
        assertEquals("3", Derivati.Dati(t.get(1)).get("posizione"));
        assertTrue(t.get(0)[24].startsWith("BYBIT-"), "identificativo sintetico");
        String idChiusura = t.get(2)[24];
        assertEquals(1, profittiDi(idChiusura).size(), "il profitto lordo collegato");

        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
        //prezzo medio 12: 3 x (20 - 12) = 24, come il Cash Flow. LIFO: 1 x (20 - 16) + 2 x (20 - 10) = 24
        assertImporto("24.00", t.get(2)[Derivati.CAMPO_RISULTATO], "LIFO della chiusura");
        assertImporto("0.00", profittiDi(idChiusura).get(0)[Derivati.CAMPO_RISULTATO], "il profitto lordo azzerato");
        //le commissioni sono movimenti a parte (CM marcati COMMISSIONE), che il passo non tocca: token e profitti danno il Cash Flow
        BigDecimal contrattoEProfitti = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            String tipo = Derivati.Tipo(v);
            BigDecimal ris = Derivati.Risultato(v);
            if (ris != null && (tipo.equals(Derivati.PNL) || tipo.equals(Derivati.CONTRATTO))) contrattoEProfitti = contrattoEProfitti.add(ris);
        }
        assertImporto("24.00", contrattoEProfitti.toPlainString(), "token e profitti");
        assertTrue(MappaCryptoWallet.values().stream().anyMatch(v -> Derivati.Tipo(v).equals(Derivati.COMMISSIONE)), "commissioni a parte");
    }

    @Test
    void contratti_formeRiconosciute() {
        assertEquals("BTC-USDT-BYBIT", DerivatiDaCsv.NormalizzaContratto("BTCUSDT", "USDT", "BYBIT"));
        assertEquals("1000PEPE-USDT-BYBIT", DerivatiDaCsv.NormalizzaContratto("1000PEPEUSDT", "USDT", "BYBIT"));
        assertEquals("BTC-USD-BYBIT", DerivatiDaCsv.NormalizzaContratto("BTCUSD", "BTC", "BYBIT"));
        assertEquals("BTC-USD-Z22-BYBIT", DerivatiDaCsv.NormalizzaContratto("BTCUSDZ22", "BTC", "BYBIT"));
        assertEquals("BTC-USDC-BYBIT", DerivatiDaCsv.NormalizzaContratto("BTCPERP", "USDC", "BYBIT"));
        assertEquals("BTC-USDC-27DEC24-BYBIT", DerivatiDaCsv.NormalizzaContratto("BTC-27DEC24", "USDC", "BYBIT"));
        assertNull(DerivatiDaCsv.NormalizzaContratto("BTCUSD", "USDT", "BYBIT"), "inverso regolato in un'altra moneta");
        assertTrue(DerivatiLifo.Inverso("BTC-USD-BYBIT", "BTC"));
        assertFalse(DerivatiLifo.Inverso("BTC-USDT-BYBIT", "USDT"));
    }
}
