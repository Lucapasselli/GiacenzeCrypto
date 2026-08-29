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

        // [15] dello scambio = valore della gamba PRIORITARIA (DammiMonetaPrioritaria), che con due
        // cripto non prioritarie è la PRIMA, cioè quella in USCITA. Su questo file la gamba in uscita
        // di un Convert ha sempre commissione 0 (la fee è addebitata sulla gamba in entrata), quindi
        // Total - Fee coincide col Subtotal: 1.08597 -> "1.09". Il ricalcolo Total - Fee sulla gamba
        // in ENTRATA incide su [15] solo quando è quella in entrata a diventare prioritaria
        // (stablecoin / EUR / cripto ad alta capitalizzazione). Nota inoltre che per uno scambio
        // cripto/cripto omogeneo post-2023 il motore LIFO (Tipologia 1) NON legge [15]: il costo di
        // carico viene trasferito dalla moneta ceduta, plusvalenza 0.
        assertEquals("1.09", m[15]);
    }
}
