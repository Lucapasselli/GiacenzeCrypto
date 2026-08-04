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

    // ==================== anno di partenza scelto dall'utente ====================

    /**
     * L'anno scelto dall'utente alza il pavimento e accorcia l'elenco. È la leva che conta: la richiesta di
     * generazione di un trimestre è la chiamata più limitata dell'integrazione con OKX, quindi ogni
     * trimestre tolto è una richiesta risparmiata.
     */
    @Test
    void lAnnoSceltoDallUtenteAccorciaLElencoDeiTrimestri() {
        long dal = quando(2021, 6, 1), adesso = quando(2026, 8, 3);

        //Senza indicazioni si parte dal primo anno coperto da OKX: 2021Q2 → 2026Q2
        assertEquals(21, CcxtInterop.trimestriArchivioOKX(dal, adesso).size());

        //Chi ha aperto il conto nel 2025 ne chiede 6 invece di 21
        List<String> dal2025 = CcxtInterop.trimestriArchivioOKX(dal, adesso, 2025);
        assertEquals(List.of("2026Q2", "2026Q1", "2025Q4", "2025Q3", "2025Q2", "2025Q1"), dal2025);
    }

    /**
     * È un <b>pavimento</b>, non una data di partenza: abbassarlo non allunga l'elenco oltre la data da cui
     * lo scaricamento parte davvero. Serve a rendere innocua la scelta di un anno troppo indietro.
     */
    @Test
    void unAnnoPiuBassoDellaDataDiPartenzaNonAllungaNulla() {
        long dal = quando(2025, 7, 1), adesso = quando(2026, 8, 3);

        assertEquals(CcxtInterop.trimestriArchivioOKX(dal, adesso),
                CcxtInterop.trimestriArchivioOKX(dal, adesso, 2021));
        //E nemmeno un anno anteriore a quello in cui l'archivio di OKX comincia
        assertEquals(CcxtInterop.trimestriArchivioOKX(dal, adesso),
                CcxtInterop.trimestriArchivioOKX(dal, adesso, 1999));
    }

    @Test
    void lAnnoSalvatoVieneRiportatoDentroIlimiti() {
        //Mai scelto, illeggibile o assente: si torna al comportamento di sistema
        assertEquals(CcxtInterop.ANNO_MIN_ARCHIVIO_OKX, CcxtInterop.annoInizioArchivioOKX(null, 2026));
        assertEquals(CcxtInterop.ANNO_MIN_ARCHIVIO_OKX, CcxtInterop.annoInizioArchivioOKX("", 2026));
        assertEquals(CcxtInterop.ANNO_MIN_ARCHIVIO_OKX, CcxtInterop.annoInizioArchivioOKX("boh", 2026));
        //Prima del 2021 l'archivio non ha dati
        assertEquals(CcxtInterop.ANNO_MIN_ARCHIVIO_OKX, CcxtInterop.annoInizioArchivioOKX("2015", 2026));
        //Un anno futuro non lascerebbe alcun trimestre: vale come "quest'anno"
        assertEquals(2026, CcxtInterop.annoInizioArchivioOKX("2030", 2026));
        //Valore valido: passa così com'è, spazi compresi
        assertEquals(2024, CcxtInterop.annoInizioArchivioOKX(" 2024 ", 2026));
    }

    // ==================== data di partenza ====================

    /**
     * Dal 04/08/2026 la data di partenza non si sceglie più: è sempre quella dell'ultimo movimento OKX già
     * scaricato. È quindi lei, e non l'anno minimo, a decidere quanto si torna indietro — l'anno resta un
     * semplice pavimento, che può solo accorciare l'elenco.
     */
    @Test
    void iTrimestriLiDecideLaDataDiPartenzaNonLAnnoMinimo() {
        long adesso = quando(2026, 8, 4);
        long ultimoMovimento = quando(2026, 6, 15);

        //Fermi all'ultimo movimento: l'anno minimo al 2025 non aggiunge nulla, perché è solo un pavimento
        assertEquals(CcxtInterop.trimestriArchivioOKX(ultimoMovimento, adesso, 2021),
                CcxtInterop.trimestriArchivioOKX(ultimoMovimento, adesso, 2025));
        assertEquals(List.of("2026Q2"), CcxtInterop.trimestriArchivioOKX(ultimoMovimento, adesso, 2025));

        //È la partenza più vecchia del primo scaricamento a estendere il recupero: da lì l'anno scelto
        //dall'utente torna a contare, ed è l'unico momento in cui gli viene chiesto
        long primoScaricamento = quando(2017, 1, 1);
        assertEquals(List.of("2026Q2", "2026Q1", "2025Q4", "2025Q3", "2025Q2", "2025Q1"),
                CcxtInterop.trimestriArchivioOKX(primoScaricamento, adesso, 2025));
    }

    /**
     * L'elenco che viene davvero chiesto a OKX è ancorato all'anno scelto, non alla data di partenza dello
     * scaricamento. È ciò che permette a un recupero rimasto a metà di completarsi alla corsa successiva:
     * partendo dall'ultimo movimento — che a quel punto è recente — i trimestri vecchi non verrebbero più
     * elencati e resterebbero irrecuperabili.
     */
    @Test
    void lArchivioPartesempreDallAnnoSceltoENonDalloScaricamento() {
        List<String> t = CcxtInterop.trimestriArchivioOKX();

        assertFalse(t.isEmpty());
        //Senza preferenza salvata il pavimento è il primo anno coperto da OKX, e l'elenco ci arriva
        assertEquals(CcxtInterop.ANNO_MIN_ARCHIVIO_OKX + "Q1", t.get(t.size() - 1));
        assertEquals(t, CcxtInterop.trimestriArchivioOKX(0, System.currentTimeMillis(),
                CcxtInterop.ANNO_MIN_ARCHIVIO_OKX));
    }

    /**
     * L'anno di partenza viene chiesto solo al primo scaricamento, e a riconoscerlo è questo controllo: se
     * dicesse "vuoto" con dei movimenti OKX già presenti, la domanda tornerebbe a ogni recupero
     * dell'archivio; se dicesse il contrario, non verrebbe fatta proprio quando è l'unica utile.
     */
    @Test
    void lArchivioEVuotoFinoAlPrimoMovimentoOKX() {
        Principale.MappaCryptoWallet.clear();
        try {
            assertTrue(CcxtInterop.archivioOKXVuoto(), "senza movimenti l'archivio è vuoto");

            //Un movimento di un altro exchange non conta: è l'archivio di OKX che deve essere vuoto
            Principale.MappaCryptoWallet.put("altro", movimentoDi("Binance"));
            assertTrue(CcxtInterop.archivioOKXVuoto());

            Principale.MappaCryptoWallet.put("okx", movimentoDi("okx"));
            assertFalse(CcxtInterop.archivioOKXVuoto(), "il confronto non deve dipendere dal maiuscolo");
        } finally {
            Principale.MappaCryptoWallet.clear();
        }
    }

    /** @return un movimento con il solo campo che qui conta, l'exchange in posizione {@code [3]} */
    private static String[] movimentoDi(String exchange) {
        String[] Trans = new String[39];
        java.util.Arrays.fill(Trans, "");
        Trans[3] = exchange;
        return Trans;
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
        //290: unico subType che l'incrocio dei billId non copriva, perché l'archivio è l'unica fonte che lo
        //espone. È l'uscita verso il Funding, riconosciuta dalla contropartita type=311 del 20/01/2024.
        assertEquals("1", CcxtInterop.tipoDaArchivioOKX("-", "290"));
        assertEquals("Transfer out", CcxtInterop.causaleBillOKX("1", "-0.000000654", true));
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
