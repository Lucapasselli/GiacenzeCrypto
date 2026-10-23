package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Copre le regole specifiche di {@code config/import/Coinbase CSV.json}:
 * <ul>
 *   <li><b>controvalore al netto della sola commissione</b> per {@code Buy}/{@code Convert}: il costo
 *       di carico è {@code Total - commissione}, non il {@code Subtotal} — lo spread resta nel costo
 *       di carico, la commissione (non deducibile per le cripto) no;</li>
 *   <li><b>Buy</b> = riga con la sola gamba crypto in entrata: si sintetizza la gamba FIAT in uscita
 *       (l'acquisto diventa un vero scambio EUR→crypto, categoria {@code AC}) e la commissione esce
 *       come movimento {@code COMMISSIONI} a sé in EUR;</li>
 *   <li><b>Convert</b>: monete già nette, commissione solo in euro → nessun movimento
 *       {@code COMMISSIONI}, si corregge solo il controvalore delle due gambe;</li>
 *   <li>le altre causali ({@code Send}/{@code Receive}/…) non emettono nessuna commissione in euro e
 *       tengono il controvalore dal {@code Subtotal};</li>
 *   <li><b>raggruppamento dei Convert sulla colonna Notes</b>: le due gambe di una conversione
 *       condividono la stessa stringa Notes ma sull'export reale cadono a 1-2 secondi di distanza, e
 *       il raggruppo sul timestamp esatto le spezzerebbe; conversioni diverse e adiacenti non si
 *       mescolano perché una gamba con Notes diverso apre sempre un gruppo nuovo;</li>
 *   <li>il campo <b>Notes</b> del CSV finisce nel campo Note del movimento ({@code [21]}).</li>
 * </ul>
 */
class ImportazioneGenericaCoinbaseTest {

    /** 0=ID 1=Timestamp 2=Type 3=Asset 4=Qty 5=PriceCur 6=PriceAt 7=Subtotal 8=Total 9=Fees 10=Notes 11=Sender 12=Recipient */
    private static ConfigurazioneImport cfg() {
        ConfigurazioneImport cfg = new ConfigurazioneImport();
        cfg.nomeExchange = "Coinbase";
        cfg.nomeWallet = "Principale";
        cfg.colonnaIDTransazione = 0;
        cfg.colonnaIDGruppo = 1;
        cfg.colonnaData = 1;
        cfg.colonnaCausale = 2;
        cfg.colonnaMoneta = 3;
        cfg.colonnaQuantita = 4;
        cfg.colonnaValoreEuro = 7;
        cfg.colonnaNote = 10;
        cfg.formatoData = "yyyy-MM-dd HH:mm:ss 'UTC'";
        cfg.fuso = "UTC";
        cfg.consolidaRigheStessaData = true;
        cfg.tolleranzaSecondiConsolidamento = 60;
        cfg.causaliDifferite.add("Convert");
        cfg.raggruppamentoPerCausale.put("Convert", 10);
        cfg.colControvaloreTotale = 8;
        cfg.colControvaloreCommissione = 9;
        cfg.colControvaloreValuta = 5;
        cfg.causaliControvaloreNetto.add("Buy");
        cfg.causaliControvaloreNetto.add("Convert");
        cfg.causaliControvaloreConCommissione.add("Buy");
        cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                "Receive", "from Coinbase Earn", "EARN"));
        cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                "Receive", "from Coinbase Referral", "REWARD"));
        cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                "Receive", "airdrop", "AIRDROP"));
        cfg.mappaCausali.put("Buy", "ACQUISTO CRYPTO");
        cfg.mappaCausali.put("Convert", "SCAMBIO CRYPTO-CRYPTO");
        cfg.mappaCausali.put("Send", "TRASFERIMENTO-CRYPTO");
        cfg.mappaCausali.put("Receive", "TRASFERIMENTO-CRYPTO");
        cfg.causaliChiuse.add("TRASFERIMENTO-CRYPTO");
        cfg.causaliChiuse.add("ACQUISTO CRYPTO");
        cfg.causaliChiuse.add("COMMISSIONI");
        cfg.noteAcquistoDaSaldo.add("EUR Wallet");
        return cfg;
    }

    private static String[] riga(String id, String ts, String type, String asset, String qty,
            String priceCur, String priceAt, String subtotal, String total, String fees, String note) {
        return new String[]{id, ts, type, asset, qty, priceCur, priceAt, subtotal, total, fees, note, "", ""};
    }

    // ---- controvalore al netto della sola commissione (funzione pura) -------------------------------

    @Test
    void controvaloreAlNetto_totaleMenoCommissione_spreadIncluso() {
        // Buy ETH: Total 10,00 - commissione 0,0769563498 = 9,9230436502 (Subtotal 8,93304 NON usato)
        String[] r = riga("x", "2018-02-12 16:40:24 UTC", "Buy", "ETH", "0.01274947",
                "EUR", "€700.66", "€8.93304", "€10.00", "0.0769563498",
                "Bought 0.01274947 ETH for 10 EUR using EUR Wallet");
        assertEquals(0, new BigDecimal("9.9230436502").compareTo(
                new BigDecimal(ImportazioneGenerica.controvaloreAlNetto(r, cfg()))));
    }

    @Test
    void controvaloreAlNetto_commissioneNegativaODust_contaComeZero() {
        // riga dust 2025: la col Fees porta un rapporto di spread con segno (-0,000189...), non una
        // commissione: va ignorata, il controvalore resta il Total.
        String[] r = riga("x", "2025-10-21 14:45:08 UTC", "Buy", "BTC", "0.00000053",
                "EUR", "€94696.41", "€0.05019", "€0.10", "-0.0001891012242288327276495",
                "Bought 0.00000053 BTC for 0.1 EUR using EUR Wallet");
        assertEquals(0, new BigDecimal("0.10").compareTo(
                new BigDecimal(ImportazioneGenerica.controvaloreAlNetto(r, cfg()))));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                ImportazioneGenerica.commissioneControvalore(r, cfg())));
    }

    @Test
    void controvaloreAlNetto_nullPerLeCausaliNonElencate() {
        String[] r = riga("x", "2026-03-26 06:35:22 UTC", "Send", "BTC", "-0.0000245",
                "EUR", "€60547.38", "-€1.48341", "-€1.48038", "€0.00302754900126025", "Sent 0.0000245 BTC");
        assertNull(ImportazioneGenerica.controvaloreAlNetto(r, cfg()));
    }

    // ---- Buy: scambio EUR->crypto sintetico + movimento COMMISSIONI --------------------------------

    @Test
    void buy_diventaScambioEuroCrypto_conCommissioneASeEControvaloreAlNetto() {
        String note = "Bought 0.01274947 ETH for 10 EUR using EUR Wallet";
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("5a81c37875d25e0001762bfd", "2018-02-12 16:40:24 UTC", "Buy", "ETH", "0.01274947",
                        "EUR", "€700.66", "€8.93304", "€10.00", "0.0769563498", note),
                null, cfg());

        assertNotNull(movs);
        assertEquals(2, movs.size(), "lo scambio EUR->ETH + il movimento COMMISSIONI");

        String[] scambio = movs.get(0);
        assertEquals("EUR", scambio[8], "gamba FIAT in uscita sintetizzata");
        assertEquals("FIAT", scambio[9]);
        assertEquals("ETH", scambio[11]);
        assertEquals("0.01274947", scambio[13], "quantita crypto invariata");
        assertEquals("9.92", scambio[15], "costo di carico = Total - commissione (10,00 - 0,0769563498), arrotondato");
        assertTrue(scambio[0].endsWith("_AC"), "categoria ACQUISTO CRYPTO, non deposito crypto");
        assertEquals(note, scambio[21], "il campo Notes finisce in [21]");

        String[] comm = movs.get(1);
        assertTrue(comm[5].toUpperCase().contains("COMMISSION"), "movimento COMMISSIONI");
        assertEquals("EUR", comm[8], "commissione pagata in EUR");
        assertEquals(0, new BigDecimal("0.0769563498").compareTo(new BigDecimal(comm[10]).abs()));
        assertEquals(note, comm[21]);

        //La commissione e' collegata allo scambio della stessa riga (CommissioniCollegate)
        assertFalse(CommissioniCollegate.Chiave(scambio).isEmpty(), "lo scambio porta la chiave delle commissioni");
        assertEquals(CommissioniCollegate.Chiave(scambio), CommissioniCollegate.Chiave(comm));
    }

    // ---- config reale ---------------------------------------------------------------------------

    @Test
    void configReale_dichiaraNoteAcquistoDaSaldo() throws Exception {
        var c = ConfigurazioneImport.carica("config/import/Coinbase CSV.json");
        assertTrue(c.noteAcquistoDaSaldo.contains("EUR Wallet"),
                "il marcatore di pagamento dal saldo deve essere dichiarato nel JSON reale");
        assertTrue(c.causaliControvaloreConCommissione.contains("Buy"));
    }

    // ---- Buy pagato con carta / strumento esterno al saldo ---------------------------------------

    @Test
    void buyConCarta_nessunaGambaEuro_nessunaCommissione_campo18DAC() {
        String note = "Bought 0.05769095 LTC for 10 EUR using MasterCard ********0198";
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("5aa573f20562f604ce1817ff", "2018-03-11 18:22:42 UTC", "Buy", "LTC", "0.05769095",
                        "EUR", "€155.67", "€8.98075", "€10.00", "0.0292498135", note),
                null, cfg());

        assertNotNull(movs);
        assertEquals(1, movs.size(), "un solo movimento: niente gamba EUR sintetica, niente COMMISSIONI");
        String[] m = movs.get(0);
        assertEquals("", m[8], "nessuna moneta in uscita: gli euro non si muovono");
        assertEquals("LTC", m[11], "gamba crypto in entrata");
        assertEquals("0.05769095", m[13], "quantita crypto invariata");
        assertTrue(m[0].endsWith("_AC"), "categoria forzata ad ACQUISTO CRYPTO nell'ID: " + m[0]);
        assertEquals("ACQUISTO CRYPTO", m[5]);
        assertEquals("", m[18], "campo18 vuoto: il motore instrada su TipoID=AC, non serve il marcatore DAC");
        assertEquals("9.97", m[15], "costo di carico = Total - commissione (10,00 - 0,0292498135), arrotondato");
        assertEquals(note, m[21]);
    }

    @Test
    void buyDaSaldoEuro_restaScambioEuroCryptoConCommissione() {
        // regressione: con 'noteAcquistoDaSaldo' attivo, un Buy 'using EUR Wallet' NON cambia
        String note = "Bought 0.01274947 ETH for 10 EUR using EUR Wallet";
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("5a81c37875d25e0001762bfd", "2018-02-12 16:40:24 UTC", "Buy", "ETH", "0.01274947",
                        "EUR", "€700.66", "€8.93304", "€10.00", "0.0769563498", note),
                null, cfg());

        assertNotNull(movs);
        assertEquals(2, movs.size(), "scambio EUR->ETH + movimento COMMISSIONI, come prima");
        assertEquals("EUR", movs.get(0)[8], "gamba FIAT in uscita ancora sintetizzata");
        assertTrue(movs.get(0)[0].endsWith("_AC"));
        assertEquals("", movs.get(0)[18], "nessun marcatore DAC per un acquisto dal saldo");
    }

    @Test
    void buyNotaVuota_comportamentoInvariato_gambaEuroSintetizzata() {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("x", "2018-03-11 18:22:42 UTC", "Buy", "LTC", "0.05769095",
                        "EUR", "€155.67", "€8.98075", "€10.00", "0.0292498135", ""),
                null, cfg());
        assertNotNull(movs);
        assertEquals(2, movs.size(), "nota vuota: non si puo' concludere 'esterno' -> resta scambio + COMMISSIONI");
    }

    // ---- Send: nessuna commissione in euro, controvalore dal Subtotal -----------------------------

    @Test
    void send_nessunaCommissioneInEuro_controvaloreDalSubtotal() {
        String note = "Sent 0.0000245 BTC to lnbc... (to lnbc2...686ux)";
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("69c4d3aa1e03864b6c6f9023", "2026-03-26 06:35:22 UTC", "Send", "BTC", "-0.0000245",
                        "EUR", "€60547.38", "-€1.48341", "-€1.48038", "€0.00302754900126025", note),
                null, cfg());

        assertNotNull(movs);
        assertEquals(1, movs.size(), "solo il trasferimento: nessuna riga COMMISSIONI in EUR");
        String[] m = movs.get(0);
        assertEquals("BTC", m[8]);
        assertEquals("", m[11], "trasferimento a gamba singola");
        assertEquals("1.48", m[15], "controvalore dal Subtotal (col 7), non ricalcolato");
        assertEquals(note, m[21]);
    }

    // ---- classificazione da testo libero delle note ---------------------------------------------

    @Test
    void receiveConNotaCoinbaseEarn_diventaEarn() {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("5c853275711fa80314ed2b58", "2019-03-10 15:51:17 UTC", "Receive", "ZEC", "0.02",
                        "EUR", "€44.40", "€0.88803", "€0.88803", "€0.00", "Received 0.02 ZEC from Coinbase Earn"),
                null, cfg());

        assertNotNull(movs);
        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("ZEC", m[11], "gamba in entrata");
        assertTrue(m[0].endsWith("_RW"), "categoria RW (reddito), non trasferimento crypto");
        assertEquals("EARN", m[5]);
        assertEquals("Received 0.02 ZEC from Coinbase Earn", m[21]);
    }

    @Test
    void receiveConNotaReferral_diventaReward_conNotaAirdrop_diventaAirdrop() {
        String[] ref = ImportazioneGenerica.costruisciMovimenti(
                riga("r1", "2021-06-01 10:00:00 UTC", "Receive", "BTC", "0.0001",
                        "EUR", "€30000", "€3.00", "€3.00", "€0.00", "Received 0.0001 BTC from Coinbase Referral"),
                null, cfg()).get(0);
        assertTrue(ref[0].endsWith("_RW"));
        assertEquals("REWARD", ref[5]);

        String[] air = ImportazioneGenerica.costruisciMovimenti(
                riga("r2", "2023-01-09 10:00:00 UTC", "Receive", "FLR", "100",
                        "EUR", "€0.02", "€2.00", "€2.00", "€0.00", "Received 100 FLR from Flare Airdrop"),
                null, cfg()).get(0);
        assertTrue(air[0].endsWith("_RW"));
        assertEquals("AIRDROP", air[5]);
    }

    @Test
    void receiveNormale_restaTrasferimentoCrypto() {
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                riga("x", "2025-12-21 11:51:50 UTC", "Receive", "BTC", "0.00001176",
                        "EUR", "€75648.12", "€0.88962", "€0.88962", "€0.00",
                        "Received 0.00001176 BTC from an external account (to lnbc1...73hmv)"),
                null, cfg());

        assertNotNull(movs);
        String[] m = movs.get(0);
        assertFalse(m[0].endsWith("_RW"), "una Receive senza nota Earn resta un trasferimento");
    }

    // ---- raggruppamento Convert sulla colonna Notes ----------------------------------------------

    @Test
    void convert_gambeADistanzaDiUnSecondo_raggruppatePerNotes_senzaMescolarsi() {
        String notaA = "Converted 22 ALEPH to 4.68065032 ZETA";
        String notaB = "Converted 4.942508 ALEO to 2.87098308 ZETA";
        List<String[]> righe = new ArrayList<>();
        // conversione A: gambe a :05 e :06 (come nell'export reale)
        righe.add(riga("67f13729479c971d62667f5d", "2025-04-05 13:59:05 UTC", "Convert", "ALEPH", "-22",
                "EUR", "€0.0493", "€1.08597", "€1.08597", "0", notaA));
        righe.add(riga("67f1372aa227f2c79b05a9cd", "2025-04-05 13:59:06 UTC", "Convert", "ZETA", "4.68065032",
                "EUR", "€0.2243", "€1.04975", "€1.08241", "0.022402128672", notaA));
        // conversione B, adiacente: Notes diverso -> gruppo separato
        righe.add(riga("67f13673479c971d62667068", "2025-04-05 13:56:03 UTC", "Convert", "ALEO", "-4.942508",
                "EUR", "€0.1355", "€0.66968", "€0.66968", "0", notaB));
        righe.add(riga("67f136740a9bd7f24aba73ee", "2025-04-05 13:56:04 UTC", "Convert", "ZETA", "2.87098308",
                "EUR", "€0.2240", "€0.64310", "€0.66781", "0.0142056020295", notaB));

        // le righe arrivano a raggruppaRighe già ordinate per data (come dopo la FASE 3 di leggiCSV)
        righe.sort((a, b) -> a[1].compareTo(b[1]));

        List<List<String[]>> gruppi = ImportazioneGenerica.raggruppaRighe(righe, cfg());
        assertEquals(2, gruppi.size(), "due conversioni distinte, due gruppi");
        for (List<String[]> g : gruppi) {
            assertEquals(2, g.size(), "ogni gruppo ha esattamente le sue due gambe");
            assertEquals(g.get(0)[10], g.get(1)[10], "le due gambe di un gruppo condividono la stringa Notes");
        }
    }

    @Test
    void convert_gruppoDiDueGambe_unSoloScambio_nessunaCommissione_noteRiportate() {
        String nota = "Converted 22 ALEPH to 4.68065032 ZETA";
        List<String[]> gruppo = List.of(
                riga("67f13729479c971d62667f5d", "2025-04-05 13:59:05 UTC", "Convert", "ALEPH", "-22",
                        "EUR", "€0.0493", "€1.08597", "€1.08597", "0", nota),
                riga("67f1372aa227f2c79b05a9cd", "2025-04-05 13:59:06 UTC", "Convert", "ZETA", "4.68065032",
                        "EUR", "€0.2243", "€1.04975", "€1.08241", "0.022402128672", nota));

        List<String[]> movs = ImportazioneGenerica.consolidaGruppo(gruppo, cfg(), new ArrayList<>());

        assertNotNull(movs);
        assertEquals(1, movs.size(), "un solo movimento di scambio, non due gambe sciolte");
        String[] m = movs.get(0);
        assertEquals("ALEPH", m[8], "moneta ceduta");
        assertEquals("ZETA", m[11], "moneta acquisita");
        assertTrue(movs.stream().noneMatch(x -> "EUR".equals(x[8]) && x[5].toUpperCase().contains("COMMISSION")),
                "per i Convert non si emette nessun movimento COMMISSIONI in euro");
        assertEquals(nota, m[21], "il campo Notes finisce in [21] anche per lo scambio consolidato");

        // [15] dello scambio = valore della gamba in ENTRATA (DammiMonetaPrioritaria, dal 2026-09-29:
        // circolare 30/E, valore normale della cripto-attivita' ricevuta). Per la gamba in entrata di un
        // Convert il controvalore e' Total - Fee: 1.08241 - 0.022402 = 1.06. Fino al 2026-09-29 vinceva la
        // gamba in uscita (1.08597 -> "1.09"). Nota che per uno scambio cripto/cripto omogeneo post-2023
        // il motore LIFO (Tipologia 1) NON legge [15]: il costo di carico viene trasferito dalla moneta
        // ceduta, plusvalenza 0.
        assertEquals("1.06", m[15]);
    }

    // ---- causali aggiunte con la v1.008 (segnalazione #19), sulla config reale --------------------

    /** Righe ordinate per data come dopo leggiCSV, raggruppate e consolidate con la config reale. */
    private static List<String[]> importaConConfigReale(List<String[]> righe) throws Exception {
        var c = ConfigurazioneImport.carica("config/import/Coinbase CSV.json");
        List<String[]> ordinate = new ArrayList<>(righe);
        ordinate.sort((a, b) -> a[1].compareTo(b[1]));
        List<String[]> movs = new ArrayList<>();
        for (List<String[]> g : ImportazioneGenerica.raggruppaRighe(ordinate, c)) {
            List<String[]> m = ImportazioneGenerica.consolidaGruppo(g, c, new ArrayList<>());
            if (m != null) movs.addAll(m);
        }
        return movs;
    }

    @Test
    void withdrawal_prelievoFiatVersoLaBanca() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000101", "2024-12-16 10:00:00 UTC", "Withdrawal", "EUR", "-100",
                        "EUR", "€1.00", "€100.00", "€100.00", "€0.00", "Withdrawal to Banca (****0000)")));

        assertEquals(1, movs.size());
        String[] m = movs.get(0);
        assertEquals("EUR", m[8], "gamba in uscita");
        assertEquals("-100", m[10]);
        assertEquals("", m[11], "nessuna gamba in entrata");
        assertTrue(m[0].endsWith("_PF"), "prelievo FIAT, era: " + m[0]);
    }

    @Test
    void eth2DeprecationEAssetMigration_unoScambioCryptoCryptoPerCoppia() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000102", "2025-01-30 19:00:00 UTC", "Retail Eth2 Deprecation", "ETH",
                        "0.5", "EUR", "€3000.00", "€1500.00", "€1500.00", "€0.00", ""),
                riga("000000000000000000000103", "2025-01-30 19:00:00 UTC", "Retail Eth2 Deprecation", "ETH2",
                        "-0.5", "EUR", "€3000.00", "-€1500.00", "-€1500.00", "€0.00", ""),
                riga("000000000000000000000104", "2025-10-15 22:00:00 UTC", "Asset Migration", "POL",
                        "100", "EUR", "€0.20", "€20.00", "€20.00", "€0.00", ""),
                riga("000000000000000000000105", "2025-10-15 22:00:00 UTC", "Asset Migration", "MATIC",
                        "-100", "EUR", "€0.20", "-€20.00", "-€20.00", "€0.00", "")));

        assertEquals(2, movs.size(), "una conversione per coppia, non quattro gambe sciolte");
        String[] eth = movs.get(0);
        assertEquals("ETH2", eth[8]);
        assertEquals("-0.5", eth[10]);
        assertEquals("ETH", eth[11]);
        assertEquals("0.5", eth[13]);
        assertTrue(eth[0].endsWith("_SC"), "scambio crypto-crypto, era: " + eth[0]);
        String[] pol = movs.get(1);
        assertEquals("MATIC", pol[8]);
        assertEquals("POL", pol[11]);
        assertEquals("100", pol[13]);
        assertTrue(pol[0].endsWith("_SC"), "scambio crypto-crypto, era: " + pol[0]);
    }

    // ---- contropartita letta dalla nota: Advanced Trade Buy/Sell e Sell (v1.009) -------------------

    @Test
    void configReale_contropartitaDaNotaFuoriDaMappaCausali() throws Exception {
        var c = ConfigurazioneImport.carica("config/import/Coinbase CSV.json");
        assertEquals("ACQUISTO CRYPTO", c.contropartitaDaNota.get("Advanced Trade Buy"));
        assertEquals("VENDITA CRYPTO", c.contropartitaDaNota.get("Advanced Trade Sell"));
        assertEquals("VENDITA CRYPTO", c.contropartitaDaNota.get("Sell"));
        assertEquals(9, c.colonnaCommissioneEuroContropartita);
        //Nella mappaCausali una versione che non legge la chiave importerebbe la sola gamba crypto
        assertNull(c.mappaCausali.get("Advanced Trade Buy"));
        assertNull(c.mappaCausali.get("Advanced Trade Sell"));
        assertNull(c.mappaCausali.get("Sell"));
        assertTrue(c.chiaviSconosciute.isEmpty(), "chiavi non lette: " + c.chiaviSconosciute);
    }

    @Test
    void advancedTradeBuyInEuro_acquistoAlLordoPiuCommissioneInEuro() throws Exception {
        String note = "Bought 0.00025 BTC for 9.054 EUR on BTC-EUR at 36000 EUR/BTC";
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000201", "2022-03-14 10:20:30 UTC", "Advanced Trade Buy", "BTC", "0.00025",
                        "EUR", "€36000.00", "€9.00000", "€9.05400", "€0.054", note)));

        assertEquals(2, movs.size(), "acquisto + COMMISSIONI");
        String[] acq = movs.get(0);
        assertTrue(acq[0].endsWith("_AC"), "acquisto crypto, era: " + acq[0]);
        assertEquals("EUR", acq[8]);
        assertEquals(0, new BigDecimal("-9").compareTo(new BigDecimal(acq[10])), "lordo = 0,00025 x 36000");
        assertEquals("BTC", acq[11]);
        assertEquals("0.00025", acq[13]);
        assertEquals("9.00", acq[15], "controvalore dal Subtotal");
        assertEquals("Advanced Trade Buy", acq[7]);

        String[] comm = movs.get(1);
        assertTrue(comm[5].toUpperCase().contains("COMMISSION"));
        assertEquals("EUR", comm[8]);
        assertEquals(0, new BigDecimal("-0.054").compareTo(new BigDecimal(comm[10])), "Y - lordo");
        assertEquals(CommissioniCollegate.Chiave(acq), CommissioniCollegate.Chiave(comm));
    }

    @Test
    void advancedTradeSellInUsdc_scambioCryptoCryptoConCommissioneInUsdc() throws Exception {
        String note = "Sold 2.5 FORTH for 12.922 USDC on FORTH-USDC at 5.2 USDC/FORTH";
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000202", "2024-04-10 07:05:00 UTC", "Advanced Trade Sell", "FORTH", "-2.5",
                        "EUR", "€4.80", "-€12.00000", "-€11.92800", "€0.072", note)));

        assertEquals(2, movs.size(), "scambio + COMMISSIONI");
        String[] sc = movs.get(0);
        assertTrue(sc[0].endsWith("_SC"), "contro USDC e' uno scambio crypto-crypto, era: " + sc[0]);
        assertEquals("FORTH", sc[8]);
        assertEquals("-2.5", sc[10]);
        assertEquals("USDC", sc[11], "la moneta viene dalla nota, non dalla colonna 5 (sempre EUR)");
        assertEquals(0, new BigDecimal("13").compareTo(new BigDecimal(sc[13])), "lordo = 2,5 x 5,2");
        assertEquals("12.00", sc[15]);

        String[] comm = movs.get(1);
        assertEquals("USDC", comm[8], "la commissione e' pagata nella valuta della coppia");
        assertEquals(0, new BigDecimal("-0.078").compareTo(new BigDecimal(comm[10])), "lordo - Y");
        assertEquals("0.07", comm[15], "controvalore dalla commissione in euro della colonna 9");
    }

    @Test
    void advancedTradeTreEsecuzioniStessoSecondo_treAcquistiETreCommissioni() throws Exception {
        //Esecuzioni dello stesso ordine allo stesso secondo: raggruppate dal timestamp, ognuna deve tenere la sua
        //contropartita (l'accumulo dei gruppi leggerebbe la sola gamba crypto).
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000203", "2023-05-11 09:15:42 UTC", "Advanced Trade Buy", "DOT",
                        "0.0012", "EUR", "€4.50", "€0.00540", "€0.00546", "€0.0000648",
                        "Bought 0.0012 DOT for 0.0054648 EUR on DOT-EUR at 4.5 EUR/DOT"),
                riga("000000000000000000000204", "2023-05-11 09:15:42 UTC", "Advanced Trade Buy", "DOT",
                        "0.8", "EUR", "€4.50", "€3.60000", "€3.64320", "€0.0432",
                        "Bought 0.8 DOT for 3.6432 EUR on DOT-EUR at 4.5 EUR/DOT"),
                riga("000000000000000000000205", "2023-05-11 09:15:42 UTC", "Advanced Trade Buy", "DOT",
                        "0.003", "EUR", "€4.50", "€0.01350", "€0.01366", "€0.000162",
                        "Bought 0.003 DOT for 0.013662 EUR on DOT-EUR at 4.5 EUR/DOT")));

        assertEquals(6, movs.size(), "tre acquisti e tre commissioni");
        BigDecimal dot = BigDecimal.ZERO, eur = BigDecimal.ZERO;
        for (String[] m : movs) {
            if ("DOT".equals(m[11])) dot = dot.add(new BigDecimal(m[13]));
            if ("EUR".equals(m[8])) eur = eur.add(new BigDecimal(m[10]));
        }
        assertEquals(0, new BigDecimal("0.8042").compareTo(dot));
        assertEquals(0, new BigDecimal("-3.6623268").compareTo(eur), "euro usciti = somma degli importi delle note");
    }

    @Test
    void sellSemplice_senzaPrezzo_lordoDalSubtotal() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000206", "2024-10-21 16:40:00 UTC", "Sell", "EOS", "-0.0525",
                        "EUR", "€0.40", "€0.02", "€0.01", "€0.01", "Sold 0.0525 EOS for 0.01 EUR"),
                riga("000000000000000000000207", "2024-11-25 11:30:00 UTC", "Sell", "USDC", "-50",
                        "EUR", "€0.93", "€46.50", "€46.50", "€0.00", "Sold 50 USDC for 46.5 EUR")));

        assertEquals(3, movs.size(), "vendita EOS + sua commissione, vendita USDC senza commissione");
        String[] eos = movs.get(0);
        assertTrue(eos[0].endsWith("_VC"), "vendita crypto, era: " + eos[0]);
        assertEquals("EOS", eos[8]);
        assertEquals("EUR", eos[11]);
        assertEquals(0, new BigDecimal("0.02").compareTo(new BigDecimal(eos[13])), "lordo = |Subtotal|");
        assertEquals(0, new BigDecimal("-0.01").compareTo(new BigDecimal(movs.get(1)[10])));
        String[] usdc = movs.get(2);
        assertEquals("USDC", usdc[8]);
        assertEquals(0, new BigDecimal("46.5").compareTo(new BigDecimal(usdc[13])));
    }

    @Test
    void notaIncoerente_rigaScartataNonIndovinata() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                //quantita' della nota diversa da quella della colonna
                riga("000000000000000000000208", "2022-03-01 12:00:00 UTC", "Advanced Trade Buy", "FORTH", "0.5",
                        "EUR", "€6.00", "€3.00000", "€3.01500", "€0.015",
                        "Bought 0.4 FORTH for 3.015 EUR on FORTH-EUR at 6 EUR/FORTH"),
                //verbo opposto al segno
                riga("000000000000000000000209", "2022-03-02 12:00:00 UTC", "Advanced Trade Sell", "FORTH", "-1.5",
                        "EUR", "€6.60", "-€9.90000", "-€9.85050", "€0.0495",
                        "Bought 1.5 FORTH for 9.8505 EUR on FORTH-EUR at 6.6 EUR/FORTH"),
                //'Sell' senza prezzo contro una valuta diversa dall'euro: il Subtotal non e' nella sua moneta
                riga("000000000000000000000210", "2024-11-09 10:00:00 UTC", "Sell", "XLM", "-10",
                        "EUR", "€0.40", "€4.00", "€4.00", "€0.00", "Sold 10 XLM for 4.3 USDC")));
        assertTrue(movs.isEmpty(), "nessun movimento a gamba singola: " + movs.size());
    }

    @Test
    void stakingTransfer_stessaMonetaNessunMovimento_ethInEth2Scambio() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000211", "2023-06-14 12:00:00 UTC", "Retail Staking Transfer", "ATOM",
                        "-3.5", "EUR", "€9.00", "-€31.50000", "-€31.50000", "€0.00", ""),
                riga("000000000000000000000212", "2023-06-14 12:00:00 UTC", "Retail Staking Transfer", "ATOM",
                        "3.5", "EUR", "€9.00", "€31.50000", "€31.50000", "€0.00", ""),
                riga("000000000000000000000213", "2024-05-20 15:30:00 UTC", "Retail Staking Transfer", "ETH",
                        "-0.25", "EUR", "€2000.00", "-€500.00000", "-€500.00000", "€0.00", ""),
                riga("000000000000000000000214", "2024-05-20 15:30:00 UTC", "Retail Staking Transfer", "ETH2",
                        "0.25", "EUR", "€2000.00", "€500.00000", "€500.00000", "€0.00", ""),
                riga("000000000000000000000215", "2025-09-02 08:00:00 UTC", "Retail Unstaking Transfer", "XTZ",
                        "-15", "EUR", "€0.80", "-€12.00000", "-€12.00000", "€0.00", ""),
                riga("000000000000000000000216", "2025-09-02 08:00:00 UTC", "Retail Unstaking Transfer", "XTZ",
                        "15", "EUR", "€0.80", "€12.00000", "€12.00000", "€0.00", "")));

        assertEquals(1, movs.size(), "le coppie della stessa moneta si annullano, resta ETH -> ETH2");
        String[] m = movs.get(0);
        assertEquals("ETH", m[8]);
        assertEquals("-0.25", m[10]);
        assertEquals("ETH2", m[11]);
        assertEquals("0.25", m[13]);
        assertTrue(m[0].endsWith("_SC"), "scambio crypto-crypto, era: " + m[0]);
    }

    @Test
    void buyUsingPortafoglioEuro_pagatoDalSaldo() throws Exception {
        List<String[]> movs = importaConConfigReale(List.<String[]>of(
                riga("000000000000000000000217", "2021-11-15 10:00:00 UTC", "Buy", "BTC", "0.0002",
                        "EUR", "€45000.00", "€9.00", "€10.00", "€1.00",
                        "Bought 0.0002 BTC for 10 EUR using Portafoglio Euro")));
        assertEquals(2, movs.size(), "scambio EUR -> BTC + COMMISSIONI, non un deposito BTC a se'");
        assertEquals("EUR", movs.get(0)[8]);
    }
}
