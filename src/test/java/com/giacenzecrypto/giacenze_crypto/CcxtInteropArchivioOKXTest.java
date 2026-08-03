package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test dell'archivio storico trimestrale di OKX ({@code Scripts/OKX_Archivio.js}), cioè della parte
 * <b>deterministica e priva di dipendenze</b>: enumerazione dei trimestri, traduzione
 * {@code instType}/{@code subType} → {@code type} e conversione delle righe del CSV.
 *
 * <p>I valori attesi non sono inventati: vengono dall'archivio reale 2026 Q2 scaricato il 03/08/2026, i cui
 * 112 {@code billId} risultano tutti presenti anche fra i bill scaricati via API. È quella coincidenza a
 * rendere automatica la deduplica fra archivio e scaricamento ordinario, ed è ciò che questi test presidiano.
 */
class CcxtInteropArchivioOKXTest {

    private static JsonArray righe(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    /** @return il timestamp di mezzogiorno del giorno indicato, per stare lontano dai confini di fuso */
    private static long quando(int anno, int mese, int giorno) {
        return LocalDate.of(anno, mese, giorno).atTime(12, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ==================== enumerazione dei trimestri ====================

    @Test
    void ilTrimestreInCorsoNonVieneMaiRichiesto() {
        //3 agosto 2026 = Q3: il più recente richiedibile è Q2, che è anche il primo dell'elenco
        List<String> t = CcxtInterop.trimestriArchivioOKX(quando(2026, 1, 15), quando(2026, 8, 3));

        assertFalse(t.contains("2026Q3"), "il trimestre in corso non è generabile da OKX");
        assertEquals("2026Q2", t.get(0), "il più recente va chiesto per primo: è quello che serve prima");
        assertEquals(List.of("2026Q2", "2026Q1"), t);
    }

    @Test
    void aCavalloDiAnnoSiRisaleAlQuartoTrimestrePrecedente() {
        //gennaio 2026 = Q1 in corso, quindi il primo richiedibile è il Q4 del 2025
        List<String> t = CcxtInterop.trimestriArchivioOKX(quando(2025, 10, 5), quando(2026, 1, 20));

        assertEquals(List.of("2025Q4"), t);
    }

    @Test
    void nonSiScendeSottoIlPrimoAnnoCopertoDaOKX() {
        //L'archivio parte da febbraio 2021: chiedere il 2019 non ha senso e produrrebbe solo richieste
        //sprecate su un endpoint a quota strettissima
        List<String> t = CcxtInterop.trimestriArchivioOKX(quando(2019, 3, 1), quando(2026, 8, 3));

        assertEquals("2021Q1", t.get(t.size() - 1));
        assertFalse(t.stream().anyMatch(s -> s.startsWith("2020")));
    }

    @Test
    void seNonManacNullaLElencoEVuoto() {
        //Data di partenza dentro il trimestre in corso: non c'è alcun trimestre chiuso da recuperare
        List<String> t = CcxtInterop.trimestriArchivioOKX(quando(2026, 7, 20), quando(2026, 8, 3));

        assertTrue(t.isEmpty(), "atteso nessun trimestre, trovati: " + t);
    }

    // ==================== instType/subType -> type ====================

    /**
     * La corrispondenza non è dedotta dalla documentazione ma ricavata incrociando i 112 {@code billId}
     * dell'archivio 2026 Q2 con gli stessi bill scaricati via API.
     */
    @Test
    void laCorrispondenzaSubTypeTipoEQuellaRicavataDaiDatiReali() {
        assertEquals("2", CcxtInterop.tipoDaArchivioOKX("SPOT", "1"));
        assertEquals("2", CcxtInterop.tipoDaArchivioOKX("SPOT", "2"));
        assertEquals("1", CcxtInterop.tipoDaArchivioOKX("-", "11"));
        assertEquals("1", CcxtInterop.tipoDaArchivioOKX("-", "12"));
        assertEquals("12", CcxtInterop.tipoDaArchivioOKX("-", "200"));
        assertEquals("12", CcxtInterop.tipoDaArchivioOKX("-", "202"));
    }

    @Test
    void unaCombinazioneIgnotaNonVieneClassificataAcaso() {
        assertEquals("", CcxtInterop.tipoDaArchivioOKX("-", "999"));
        assertEquals("", CcxtInterop.tipoDaArchivioOKX("-", ""));
        assertEquals("", CcxtInterop.tipoDaArchivioOKX(null, null));
    }

    // ==================== conversione delle righe ====================

    /**
     * Riga reale dell'archivio 2026 Q2. L'apostrofo iniziale del {@code billId} è la forzatura a testo per
     * Excel: se non viene tolto, l'identificativo non coincide con quello dello scaricamento via API e il
     * movimento entra <b>una seconda volta</b> invece di essere riconosciuto come già presente.
     */
    @Test
    void lApostrofoDelBillIdVieneTolto() {
        List<String[]> r = CcxtInterop.convertOKXArchivio(righe("""
            [{"instType":"SPOT","billId":"'3702022358013911043","subType":"1","ts":"1782831263000",
              "balChg":"0.0004626628199999993","sz":"0.00046359","fee":"-0.00000092718","ccy":"BTC"}]
            """));

        assertEquals(1, r.size());
        assertEquals("3702022358013911043", r.get(0)[14],
                "senza lo strip dell'apostrofo la deduplica contro i bill API non funziona");
    }

    /**
     * È il test che presidia la deduplica: lo stesso movimento, preso dalle due strade, deve arrivare
     * identico. I due {@code balChg} qui sotto sono i valori <b>realmente</b> restituiti dalle due fonti per
     * il bill 3702022358013911043 — l'API lo dà pulito, il CSV con la sbavatura in virgola mobile — e devono
     * comunque convergere sullo stesso movimento.
     */
    @Test
    void unaRigaDiArchivioProduceLoStessoMovimentoDelBillJSON() {
        String dalCsv = """
            [{"instType":"SPOT","billId":"'3702022358013911043","subType":"1","ts":"1782831263000",
              "balChg":"0.0004626628199999993","sz":"0.00046359","fee":"-0.00000092718","ccy":"BTC"}]
            """;
        String dallApi = """
            [{"billId":"3702022358013911043","type":"2","subType":"1","ts":"1782831263000",
              "balChg":"0.00046266282","sz":"0.00046359","fee":"-0.00000092718","ccy":"BTC"}]
            """;

        String[] a = CcxtInterop.convertOKXArchivio(righe(dalCsv)).get(0);
        String[] b = CcxtInterop.convertOKXBills(righe(dallApi), "Trading").get(0);

        assertArrayEquals(b, a, "archivio e API devono produrre il medesimo movimento");

        //Il lordo è il netto più la commissione, e coincide con sz
        assertEquals(0, new BigDecimal("0.00046359").compareTo(new BigDecimal(a[6])));
        assertEquals("BTC", a[11]);
        assertEquals(0, new BigDecimal("-0.00000092718").compareTo(new BigDecimal(a[12])));
    }

    /**
     * Il CSV riporta {@code balChg} con la sbavatura di un passaggio in virgola mobile
     * ({@code -27.120432230999995}) mentre l'API dà lo stesso numero pulito ({@code -27.120432231}).
     * {@code sz} coincide invece esattamente nelle due fonti, quindi è da lì che si ricostruisce.
     */
    @Test
    void laQuantitaVienePulitaUsandoSzCheNelleDueFontiCoincide() {
        List<String[]> r = CcxtInterop.convertOKXArchivio(righe("""
            [{"instType":"SPOT","billId":"'3702022358013911044","subType":"2","ts":"1782831263000",
              "balChg":"-27.120432230999995","sz":"27.120432231","fee":"0","ccy":"USDC"}]
            """));

        assertEquals("-27.120432231", r.get(0)[6], "atteso il valore pulito, non quello del CSV");
    }

    /**
     * Sulle righe di giroconto {@code sz} vale zero: non c'è nulla da cui ricostruire e il valore
     * dichiarato va lasciato intatto. Sono comunque righe che l'import scarta come NON CONSIDERARE.
     */
    @Test
    void senzaSzLaQuantitaRestaQuellaDichiarata() {
        List<String[]> r = CcxtInterop.convertOKXArchivio(righe("""
            [{"instType":"-","billId":"'3685373875806871555","subType":"12","ts":"1782831263000",
              "balChg":"-145.9999999999999","sz":"0","fee":"0","ccy":"USDC"}]
            """));

        assertEquals(0, new BigDecimal("-145.9999999999999").compareTo(new BigDecimal(r.get(0)[6])));
        assertEquals("Transfer out", r.get(0)[4]);
    }

    @Test
    void unSubTypeIgnotoArrivaNelRiepilogoConIlProprioCodice() {
        List<String[]> r = CcxtInterop.convertOKXArchivio(righe("""
            [{"instType":"-","billId":"'999","subType":"777","ts":"1782831263000",
              "balChg":"1","ccy":"ETH","fee":"0"}]
            """));

        assertEquals(1, r.size());
        assertTrue(r.get(0)[4].contains("777"),
                "il codice non decodificato va riportato in causale, era: " + r.get(0)[4]);
    }

    // ==================== stabilità dell'identificativo ====================

    /**
     * Regressione del difetto che ha prodotto <b>44 commissioni duplicate in giugno 2026</b>: lo stesso bill,
     * scaricato una volta dai 3 mesi di {@code account/bills-archive} e una volta dall'archivio trimestrale
     * (due finestre che si sovrappongono), finiva in posizioni diverse del gruppo di consolidamento e
     * riceveva due identificativi diversi, quindi entrava due volte.
     */
    @Test
    void lIdentificativoDellaCommissioneNonDipendeDaCosaEStatoScaricatoInsieme() {
        String bill = "3687378114812911616";

        //Stesso bill, due posizioni diverse nel gruppo: il numero di movimento deve restare lo stesso
        assertEquals(Importazioni.Ex_OKX_NumMovimentoCommissione(bill, 0),
                     Importazioni.Ex_OKX_NumMovimentoCommissione(bill, 10),
                     "la posizione nel gruppo dipende dalla finestra di scaricamento, non dal movimento");
        assertEquals(1, Importazioni.Ex_OKX_NumMovimentoCommissione(bill, 7));
    }

    /**
     * Senza ID originale il contatore è l'unica cosa che distingue due commissioni nello stesso istante:
     * lì il vecchio comportamento va conservato.
     */
    @Test
    void senzaIDOriginaleIlContatoreRestaLunicaDistinzione() {
        assertEquals(1, Importazioni.Ex_OKX_NumMovimentoCommissione("", 0));
        assertEquals(3, Importazioni.Ex_OKX_NumMovimentoCommissione("", 2));
        assertEquals(3, Importazioni.Ex_OKX_NumMovimentoCommissione(null, 2));
    }

    // ==================== notes ====================

    /** {@code type=75} è stato riconosciuto dal campo {@code notes} dei bill reali del Funding. */
    @Test
    void laSottoscrizioneSimpleEarnEUnGirocontoInterno() {
        assertEquals("Transfer out", CcxtInterop.causaleBillOKX("75", "-1004.38", false));
        assertEquals("Transfer in", CcxtInterop.causaleBillOKX("75", "1004.38", false));
        //e la mappa causali la scarta, come già fa per il riscatto (76)
        assertEquals("NON CONSIDERARE", Importazioni.Ex_OKX_MappaCausali().get("Transfer out"));
    }

    @Test
    void unTipoNonMappatoPortaConSeLEtichettaInChiaroDiOKX() {
        String c = CcxtInterop.causaleBillOKX("999", "1", false, "Qualcosa di nuovo");

        assertTrue(c.contains("999"), c);
        assertTrue(c.contains("Qualcosa di nuovo"), "notes rende autoesplicativo il codice ignoto: " + c);
    }
}
