package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MiniPay_SpeseCarta}: lettura del CSV della carta e abbinamento alle spese della catena. CSV e archivio sono
 * interamente inventati (esercenti, importi, date, indirizzi e hash di fantasia), con la stessa forma di un export vero.
 */
class MiniPay_SpeseCartaTest {

    private static final String CSV = """
            Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference
            2030-01-15T16:00:00Z,Payment,pending,PAYPAL                   0000000000   LU,PayPal,LU,LU,5999,0.00,USD,Debit,0.00,USD,,aaaaaaaa-0000-0000-0000-000000000001
            2030-01-15T14:00:00Z,Payment,approved,NEGOZIO DI PROVA  CITTA      ITA,Negozio,CITTA      XX,IT,5411,10.00,USD,Debit,9.00,EUR,,aaaaaaaa-0000-0000-0000-000000000002
            2030-01-15T13:00:00Z,Payment,approved,PK000                    CITTA        IT,Parcheggio di Prova,CITTA,IT,7523,2.00,USD,Debit,1.80,EUR,,aaaaaaaa-0000-0000-0000-000000000003
            2030-01-14T22:00:00Z,Deposit,approved,Deposit from 0xabcdef01...,,,,,300.00,USDT,Credit,,,0xabababababababababababababababababababababababababababababababab,aaaaaaaa-0000-0000-0000-000000000004
            """;

    /** Cambio fisso: 1 USD = 0,90 EUR. */
    private static final BiFunction<String, String, String> CAMBIO =
            (v, g) -> new BigDecimal(v).multiply(new BigDecimal("0.90")).toPlainString();

    private static final Set<String> NOMI = Set.of("0xsafe (celo)");

    private static String[] spesa(String id, String data, String usdt, String valore) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        //come nell'archivio vero: l'ID comincia con l'istante al secondo, il [1] e' al minuto
        v[0] = data.replaceAll("[- :]", "") + "_" + id + "_PC";
        v[1] = data.substring(0, 16);
        v[3] = "0xsafe (CELO)";
        v[7] = Trans_MiniPay.CAUSALE_CARTA;
        v[8] = "USDT";
        v[10] = "-" + usdt;
        v[15] = valore;
        return v;
    }

    private static Map<String, String[]> archivio(String[]... righe) {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (String[] r : righe) m.put(r[0], r);
        return m;
    }

    @Test
    void leggeIlFileDiProva() {
        List<MiniPay_SpeseCarta.Riga> righe = MiniPay_SpeseCarta.Leggi(CSV);
        assertEquals(4, righe.size());
        assertEquals("0xabcdef01", MiniPay_SpeseCarta.PrefissoWallet(righe));
        MiniPay_SpeseCarta.Riga coop = righe.get(1);
        assertEquals(new BigDecimal("10.00"), coop.importo());
        assertEquals(new BigDecimal("9.00"), coop.importoOriginale());
        assertEquals("CITTA XX", coop.citta(), "gli spazi in eccesso si riducono");
        assertTrue(coop.isPagamentoApprovato());
        assertTrue(righe.get(0).isPreautorizzazione());
        assertFalse(righe.get(0).isPagamentoInAttesa(), "importo zero: non e' un pagamento da abbinare");
        assertFalse(righe.get(0).isPagamentoApprovato());
    }

    @Test
    void intestazioneSbagliataELanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> MiniPay_SpeseCarta.Leggi("a,b,c\n1,2,3\n"));
    }

    @Test
    void abbinaPerImportoEIgnoraIlPending() {
        Map<String, String[]> a = archivio(
                spesa("A", "2030-01-15 15:46:00", "10.00", "8.50"),
                spesa("B", "2030-01-15 15:30:00", "2.00", "1.70"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), a, NOMI, CAMBIO);
        assertEquals(2, p.abbinamenti.size());
        assertEquals(1, p.preautorizzazioniIgnorate, "il PayPal a zero e' una preautorizzazione");
        assertEquals(0, p.pagamentiInAttesa);
        assertTrue(p.nonAbbinati.isEmpty(), p.nonAbbinati.toString());
        assertEquals(new BigDecimal("10.20"), p.totaleSalvato());
        assertEquals(new BigDecimal("10.80"), p.totaleReale());
        // spread = salvato - reale: -0,60 su 10,80 = -5,56%
        assertEquals(new BigDecimal("-5.56"), p.spreadPercentuale());
        MiniPay_SpeseCarta.Abbinamento coop = p.abbinamenti.stream()
                .filter(x -> x.movimento[0].contains("_A_")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-0.50"), coop.spread());
    }

    @Test
    void unMovimentoPuoRaccogliereDuePagamenti() {
        // 10,00 + 2,00 = 12,00 in un solo trasferimento della Safe
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:50:00", "12.00", "10.00"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), a, NOMI, CAMBIO);
        assertEquals(1, p.abbinamenti.size());
        assertEquals(2, p.abbinamenti.get(0).pagamenti.size());
        assertEquals(new BigDecimal("10.80"), p.abbinamenti.get(0).reale);
    }

    @Test
    void nonAbbinaMaiPerImportoApprossimato() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:46:00", "10.01", "8.50"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), a, NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
        assertEquals(3, p.nonAbbinati.size(), "il movimento e i due pagamenti approvati");
    }

    @Test
    void unPagamentoNonPuoEssereRegolatoPrimaDiEssereFatto() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 10:00:00", "10.00", "8.50"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), a, NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
    }

    @Test
    void valutaNonEuroUsaIlCambioDelGiorno() {
        String csv = """
                Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference
                2030-01-15T14:00:00Z,Payment,approved,X,Shop,NYC,US,5999,10.00,USD,Debit,10.00,USD,,aaaaaaaa-0000-0000-0000-000000000005
                """;
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:46:00", "10.00", "8.00"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(csv), a, NOMI, CAMBIO);
        assertEquals(new BigDecimal("9.00"), p.abbinamenti.get(0).reale);
        assertTrue(MiniPay_SpeseCarta.Note(p.abbinamenti.get(0)).contains("cambio BCE"));
    }

    @Test
    void senzaCambioIlMovimentoNonSiTocca() {
        String csv = """
                Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference
                2030-01-15T14:00:00Z,Payment,approved,X,Shop,NYC,US,5999,10.00,USD,Debit,10.00,USD,,aaaaaaaa-0000-0000-0000-000000000005
                """;
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:46:00", "10.00", "8.00"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(csv), a, NOMI, (v, g) -> null);
        assertTrue(p.abbinamenti.isEmpty());
        assertFalse(p.nonAbbinati.isEmpty());
    }

    @Test
    void importiIdenticiSiAbbinanoPerOrdine() {
        String csv = """
                Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference
                2030-01-15T14:00:00Z,Payment,approved,P,Parcheggio,CITTA,IT,7523,2.00,USD,Debit,1.80,EUR,,aaaaaaaa-0000-0000-0000-000000000001
                2030-01-15T14:10:00Z,Payment,approved,P,Parcheggio,CITTA,IT,7523,2.00,USD,Debit,1.80,EUR,,aaaaaaaa-0000-0000-0000-000000000002
                """;
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:30:00", "2.00", "1.70"),
                spesa("B", "2030-01-15 15:40:00", "2.00", "1.70"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(csv), a, NOMI, CAMBIO);
        assertEquals(2, p.abbinamenti.size(), p.nonAbbinati.toString());
        assertNotEquals(p.abbinamenti.get(0).pagamenti.get(0).riferimento(), p.abbinamenti.get(1).pagamenti.get(0).riferimento());
    }

    @Test
    void applicaScriveValoreNoteEDocumentoESiPuoRipetere() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:46:00", "10.00", "8.50"));
        List<MiniPay_SpeseCarta.Riga> righe = MiniPay_SpeseCarta.Leggi(CSV);
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(righe, a, NOMI, CAMBIO);
        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        String[] v = a.values().iterator().next();
        assertEquals("9.00", v[15]);
        assertEquals("SI", v[32]);
        assertTrue(v[21].contains("Negozio, CITTA XX (IT), MCC 5411"), v[21]);
        assertTrue(v[21].contains("importo originale 9,00 EUR"), v[21]);
        assertTrue(v[21].contains("spread -0,50 EUR (-5,56%)"), v[21]);
        assertFalse(v[21].contains(";"));
        assertFalse(v[42].isBlank(), "la modifica entra nello storico: il lignaggio si assegna");

        MiniPay_SpeseCarta.Piano ancora = MiniPay_SpeseCarta.Pianifica(righe, a, NOMI, CAMBIO);
        assertTrue(ancora.abbinamenti.isEmpty(), "secondo passaggio: niente da rifare");
        assertEquals(1, ancora.movimentiGiaAbbinati);
        assertEquals(1, ancora.pagamentiGiaAbbinati);
    }

    @Test
    void ignoraLeSpeseDiUnAltroWallet() {
        String[] v = spesa("A", "2030-01-15 15:46:00", "10.00", "8.50");
        v[3] = "0xaltro (CELO)";
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), archivio(v), NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
    }

    @Test
    void walletCompatibiliConfrontaIlPrefissoSenzaCambiareLeMaiuscole() {
        List<String> tabella = List.of("0xABCDEF01aaaa;CELO;x", "0xABCDEF01bbbb;ETH;x", "0x12345678cc;CELO;x");
        assertEquals(List.of("0xABCDEF01aaaa"), MiniPay_SpeseCarta.WalletCompatibili(tabella, "0xabcdef01"));
        assertEquals(List.of("0xABCDEF01aaaa", "0x12345678cc"), MiniPay_SpeseCarta.WalletCompatibili(tabella, null));
        assertTrue(MiniPay_SpeseCarta.WalletCompatibili(tabella, "0x99999999").isEmpty());
    }

    @Test
    void ultimaRigaApprovataIgnoraIPending() {
        long ms = MiniPay_SpeseCarta.UltimoApprovatoMs(MiniPay_SpeseCarta.Leggi(CSV));
        // la riga approvata piu' recente e' il pagamento del negozio delle 14:00:00Z, non il PayPal pending delle 16:00:00Z
        assertEquals(java.time.Instant.parse("2030-01-15T14:00:00Z").toEpochMilli(), ms);
    }

    @Test
    void ultimoMovimentoDelWalletESafe() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 15:46:00", "10.00", "8.50"));
        assertEquals(FunzioniDate.ConvertiDatainLongSecondo("2030-01-15 15:46:00"),
                MiniPay_SpeseCarta.UltimoMovimentoMs(a, NOMI));
        assertEquals(0, MiniPay_SpeseCarta.UltimoMovimentoMs(a, Set.of("altro (celo)")));
    }

    @Test
    void riepilogoMostraTotaliESpreadComplessivo() {
        Map<String, String[]> a = archivio(
                spesa("A", "2030-01-15 15:46:00", "10.00", "8.50"),
                spesa("B", "2030-01-15 15:30:00", "2.00", "1.70"));
        String html = MiniPay_SpeseCarta.RiepilogoHtml(
                MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(CSV), a, NOMI, CAMBIO), 15);
        assertTrue(html.contains("Spread complessivo: <b>-0,60 EUR (-5,56%)</b>"), html);
        assertTrue(html.contains("Preautorizzazioni a importo zero, ignorate: <b>1</b>"), html);
        assertFalse(html.contains("Non abbinati"), html);
    }

    /** Nel file vero dell'app i pagamenti conclusi sono {@code settled}, non {@code approved} (dati inventati, forma reale). */
    @Test
    void ipagamentiSettledContanoEUnTrasferimentoPuoRaccoglierneDue() {
        String csv = """
                Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference
                2030-01-15T14:40:00Z,Payment,settled,NEGOZIO,Negozio,CITTA,IT,5411,7.00,USD,Debit,6.30,EUR,,bbbbbbbb-0000-0000-0000-000000000001
                2030-01-15T14:20:00Z,Payment,settled,PK000,Parcheggio,CITTA,IT,7523,2.00,USD,Debit,1.80,EUR,,bbbbbbbb-0000-0000-0000-000000000002
                2030-01-15T16:00:00Z,Payment,pending,PAYPAL,PayPal,US,US,4899,0.00,USD,Debit,0.00,EUR,,bbbbbbbb-0000-0000-0000-000000000003
                """;
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 19:02:00", "9.00", "7.50"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(csv), a, NOMI, CAMBIO);
        assertEquals(1, p.abbinamenti.size(), p.nonAbbinati.toString());
        assertEquals(2, p.abbinamenti.get(0).pagamenti.size());
        assertEquals(new BigDecimal("8.10"), p.abbinamenti.get(0).reale);
        assertEquals(java.time.Instant.parse("2030-01-15T14:40:00Z").toEpochMilli(),
                MiniPay_SpeseCarta.UltimoApprovatoMs(MiniPay_SpeseCarta.Leggi(csv)));
    }

    //=== pagamenti pending con un importo (13/11/2026): escono dalla Safe prima di diventare settled ===

    private static final String INTESTAZIONE_CSV =
            "Date,Type,Status,Description,Merchant,Merchant city,Merchant country,MCC,Amount,Currency,Direction,Original amount,Original currency,Transaction hash,Reference\n";

    private static String csvPagamento(String stato, String importo, String originale) {
        return INTESTAZIONE_CSV
                + "2030-01-15T19:30:00Z,Payment," + stato + ",NEGOZIO,Negozio,CITTA,IT,5399," + importo + ",USD,Debit," + originale
                + ",EUR,,cccccccc-0000-0000-0000-000000000001\n"
                + "2030-01-15T20:00:00Z,Payment,pending,PAYPAL,PayPal,US,US,4899,0.00,USD,Debit,0.00,EUR,,cccccccc-0000-0000-0000-000000000009\n";
    }

    @Test
    void unPendingGiaUscitoDallaSafeSiAbbinaESiSegnaInAttesa() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-16 06:00:00", "30.00", "27.50"));
        List<MiniPay_SpeseCarta.Riga> righe = MiniPay_SpeseCarta.Leggi(csvPagamento("pending", "30.00", "27.00"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(righe, a, NOMI, CAMBIO);
        assertEquals(1, p.abbinamenti.size(), p.nonAbbinati.toString());
        assertTrue(p.abbinamenti.get(0).inAttesa());
        assertEquals(1, p.preautorizzazioniIgnorate, "il PayPal a zero resta ignorato");
        assertEquals(new BigDecimal("27.00"), p.abbinamenti.get(0).reale);
        assertTrue(MiniPay_SpeseCarta.Note(p.abbinamenti.get(0)).contains(", " + MiniPay_SpeseCarta.ATTESA + ", rif. cccccccc"));
        assertTrue(MiniPay_SpeseCarta.RiepilogoHtml(p, 15).contains("(in attesa)"));

        //Applicato, un secondo passaggio con lo stesso file non rifa' niente e non propone conferme
        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        MiniPay_SpeseCarta.Piano ancora = MiniPay_SpeseCarta.Pianifica(righe, a, NOMI, CAMBIO);
        assertTrue(ancora.abbinamenti.isEmpty());
        assertTrue(ancora.conferme.isEmpty());
        assertEquals(1, ancora.giaAbbinatiInAttesa);
    }

    @Test
    void unPendingNonAncoraUscitoNonTrovaIlMovimentoEResta() {
        //La catena arriva solo a un altro importo: il pending non e' ancora uscito, e non e' un'anomalia da elencare
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 18:02:00", "5.00", "4.50"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(
                MiniPay_SpeseCarta.Leggi(csvPagamento("pending", "30.00", "27.00")), a, NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
        assertEquals(1, p.pagamentiInAttesa);
        assertEquals(1, p.nonAbbinati.size(), "solo il movimento da 5,00 senza pagamento, non il pending");
        assertTrue(p.nonAbbinati.get(0).startsWith("Movimento"), p.nonAbbinati.toString());
    }

    @Test
    void quandoIlFileDiventaSettledLaNotaSiConfermaSenzaToccareIlValore() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-16 06:00:00", "30.00", "27.50"));
        MiniPay_SpeseCarta.Applica(MiniPay_SpeseCarta.Pianifica(
                MiniPay_SpeseCarta.Leggi(csvPagamento("pending", "30.00", "27.00")), a, NOMI, CAMBIO), 0);
        String[] v = a.values().iterator().next();
        assertTrue(v[21].contains(MiniPay_SpeseCarta.ATTESA));

        List<MiniPay_SpeseCarta.Riga> settled = MiniPay_SpeseCarta.Leggi(csvPagamento("settled", "30.00", "27.00"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(settled, a, NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
        assertEquals(1, p.conferme.size());
        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        assertFalse(v[21].contains(MiniPay_SpeseCarta.ATTESA), v[21]);
        assertTrue(v[21].contains("rif. cccccccc-0000-0000-0000-000000000001"), "il riferimento resta: la rilettura e' idempotente");
        assertEquals("27.00", v[15], "il valore non cambia");

        MiniPay_SpeseCarta.Piano ancora = MiniPay_SpeseCarta.Pianifica(settled, a, NOMI, CAMBIO);
        assertTrue(ancora.conferme.isEmpty());
        assertTrue(ancora.abbinamenti.isEmpty());
    }

    @Test
    void seLImportoCambiaFraAttesaEConclusioneNonSiTocca() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-16 06:00:00", "30.00", "27.50"));
        MiniPay_SpeseCarta.Applica(MiniPay_SpeseCarta.Pianifica(
                MiniPay_SpeseCarta.Leggi(csvPagamento("pending", "30.00", "27.00")), a, NOMI, CAMBIO), 0);
        String[] v = a.values().iterator().next();
        String nota = v[21];

        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(
                MiniPay_SpeseCarta.Leggi(csvPagamento("settled", "30.00", "28.00")), a, NOMI, CAMBIO);
        assertTrue(p.conferme.isEmpty());
        assertEquals(1, p.nonAbbinati.size(), p.nonAbbinati.toString());
        assertTrue(p.nonAbbinati.get(0).contains("importo diverso"), p.nonAbbinati.toString());
        assertEquals(0, MiniPay_SpeseCarta.Applica(p, 0));
        assertEquals(nota, v[21]);
    }

    //=== data del pagamento: divisione e spostamento d'orario (13/11/2026) ===

    private static final String WALLET = "0xsafe (CELO)";

    /** Una spesa come arriva dalla catena: ID nella forma vera, blocco e hash della transazione, nessun legame con altri. */
    private static String[] spesaCatena(String dataLocale, String usdt, String valore, String hash) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = dataLocale.replaceAll("[- :]", "") + "_0xsafe_001_001_PC";
        v[1] = dataLocale.substring(0, 16);
        v[3] = WALLET;
        v[7] = Trans_MiniPay.CAUSALE_CARTA;
        v[8] = "USDT";
        v[10] = "-" + usdt;
        v[15] = valore;
        v[22] = "A";
        v[23] = "1000";
        v[24] = hash;
        v[26] = "0xtoken";
        v[29] = String.valueOf(FunzioniDate.ConvertiDatainLongSecondo(dataLocale));
        return v;
    }

    private static String[] depositoCatena(String dataLocale, String usdt) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = dataLocale.replaceAll("[- :]", "") + "_0xsafe_001_001_DC";
        v[1] = dataLocale.substring(0, 16);
        v[3] = WALLET;
        v[11] = "USDT";
        v[13] = usdt;
        v[22] = "A";
        v[28] = "0xtoken";
        return v;
    }

    private static String csv(String... righe) {
        return INTESTAZIONE_CSV + String.join("\n", righe) + "\n";
    }

    private static String pag(String istanteUtc, String stato, String usd, String eur, int n) {
        return istanteUtc + ",Payment," + stato + ",NEGOZIO " + n + ",Negozio " + n + ",CITTA,IT,5411," + usd + ",USD,Debit," + eur
                + ",EUR,,dddddddd-0000-0000-0000-00000000000" + n;
    }

    private static List<String[]> spese(Map<String, String[]> a) {
        return a.values().stream().filter(MiniPay_SpeseCarta::isSpesaCarta)
                .sorted(java.util.Comparator.comparing(x -> x[0])).toList();
    }

    private static final String DUE_PAGAMENTI = csv(
            pag("2030-01-15T14:40:00Z", "settled", "7.00", "6.30", 2),
            pag("2030-01-15T14:20:00Z", "settled", "2.00", "1.80", 1));

    @Test
    void duePagamentiDividonoIlMovimentoConLOraDiOgniPagamento() {
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"),
                depositoCatena("2030-01-14 22:00:00", "300"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.DIVISIONE, p.abbinamenti.get(0).esecuzione, p.abbinamenti.get(0).motivo);
        assertEquals(1, p.movimentiDaDividere());
        assertEquals(2, p.pezziDaCreare());

        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        List<String[]> r = spese(a);
        assertEquals(2, r.size(), "il movimento unico e' sostituito dai due pezzi");
        String[] primo = r.get(0), secondo = r.get(1);
        //14:20Z e 14:40Z sono le 15:20 e le 15:40 in Italia a gennaio
        assertEquals("2030-01-15 15:20", primo[1]);
        assertEquals("2030-01-15 15:40", secondo[1]);
        assertTrue(primo[0].startsWith("20300115152000_"), primo[0]);
        assertEquals("-2", primo[10]);
        assertEquals("-7", secondo[10]);
        assertEquals("1.80", primo[15]);
        assertEquals("6.30", secondo[15]);
        assertEquals(String.valueOf(java.time.Instant.parse("2030-01-15T14:20:00Z").toEpochMilli()), primo[29]);
        for (String[] pz : r) {
            assertEquals("0xhash1", pz[24], "l'hash della transazione resta");
            assertEquals("1000", pz[23], "il blocco resta: e' il cursore dello scaricamento");
            assertEquals("SI", pz[32]);
            assertEquals(MiniPay_SpeseCarta.FONTE_PREZZO, pz[40]);
            assertEquals("", pz[19], "i campi del motore si ricalcolano");
        }
        assertFalse(primo[42].isBlank());
        assertEquals(primo[42], secondo[42], "lignaggio condiviso: lo storico conserva la riga della catena");
        assertTrue(primo[21].contains("parte 1/2"), primo[21]);
        //quote proporzionali del valore salvato (8,00 su 9 USDT): 2/9 = 1,78, il resto all'ultimo
        assertTrue(primo[21].contains("valore salvato 1,78 EUR"), primo[21]);
        assertTrue(secondo[21].contains("valore salvato 6,22 EUR"), secondo[21]);
        assertFalse(primo[21].contains("NEGOZIO 2") || primo[21].contains("Negozio 2"), "ogni pezzo ha solo le note del suo pagamento");
        assertTrue(a.containsKey(depositoCatena("2030-01-14 22:00:00", "300")[0]), "il resto dell'archivio non si tocca");
    }

    @Test
    void ripetereLoStessoFileONaFileConPiuRigheNonDuplicaNiente() {
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        MiniPay_SpeseCarta.Applica(MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO), 0);
        assertEquals(2, spese(a).size());

        //Lo stesso file
        MiniPay_SpeseCarta.Piano ancora = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertTrue(ancora.abbinamenti.isEmpty());
        assertTrue(ancora.conferme.isEmpty());
        assertEquals(2, ancora.movimentiGiaAbbinati);
        assertEquals(0, MiniPay_SpeseCarta.Applica(ancora, 0));
        assertEquals(2, spese(a).size());

        //Un file piu' recente che lo contiene e ha altri pagamenti: gli altri restano senza movimento, quelli di prima non si toccano
        String piuRecente = csv(pag("2030-01-16T10:00:00Z", "pending", "5.00", "4.50", 3),
                pag("2030-01-15T14:40:00Z", "settled", "7.00", "6.30", 2),
                pag("2030-01-15T14:20:00Z", "settled", "2.00", "1.80", 1));
        MiniPay_SpeseCarta.Piano ancora2 = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(piuRecente), a, NOMI, CAMBIO);
        assertTrue(ancora2.abbinamenti.isEmpty());
        assertEquals(0, MiniPay_SpeseCarta.Applica(ancora2, 0));
        assertEquals(2, spese(a).size());
    }

    @Test
    void unoScaricamentoCheRiparteDaPiuIndietroNonRicreaLaSpesaDivisa() {
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xHASH1"));
        MiniPay_SpeseCarta.Applica(MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO), 0);
        Set<String> abbinati = MiniPay_SpeseCarta.HashAbbinati(a.values());
        assertEquals(Set.of("0xsafe (celo)|0xhash1"), abbinati, "wallet e hash, minuscoli");

        //La stessa transazione riletta dalla catena, con il cursore a zero
        Trans_MiniPay.Transazione t = new Trans_MiniPay.Transazione("0xsafe", "0xHash1", 1000, 0, List.of());
        Trans_MiniPay.Transazione altra = new Trans_MiniPay.Transazione("0xsafe", "0xaltra", 1001, 0, List.of());
        List<Trans_MiniPay.Transazione> nuove = Trans_MiniPay.NuoveTransazioni(List.of(t, altra), w -> 0L, abbinati);
        assertEquals(List.of(altra), nuove, "quella gia' abbinata non rientra, l'altra si");
        //Senza il filtro dell'hash la riscarico sarebbe passata: e' il blocco del cursore che la copre di solito
        assertEquals(2, Trans_MiniPay.NuoveTransazioni(List.of(t, altra), w -> 0L, Set.of()).size());
        assertEquals(List.of(altra), Trans_MiniPay.NuoveTransazioni(List.of(t, altra), w -> 1000L, Set.of()));
    }

    @Test
    void unPagamentoSoloSpostaIlMovimentoAllOraDelPagamento() {
        String file = csv(pag("2030-01-15T14:40:00Z", "settled", "7.00", "6.30", 2));
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "7", "6.50", "0xhash1"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(file), a, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.ORARIO, p.abbinamenti.get(0).esecuzione);
        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        String[] v = spese(a).get(0);
        assertEquals(1, spese(a).size());
        assertEquals("2030-01-15 15:40", v[1]);
        assertTrue(v[0].startsWith("20300115154000_"), v[0]);
        assertEquals("6.30", v[15]);
        assertFalse(v[42].isBlank());
        assertTrue(v[21].contains("orario del movimento sulla catena 2030-01-15 19:02:07"), v[21]);
        assertEquals("0xhash1", v[24]);
        //Ripassando non cambia piu' niente
        MiniPay_SpeseCarta.Piano ancora = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(file), a, NOMI, CAMBIO);
        assertTrue(ancora.abbinamenti.isEmpty());
        assertEquals(0, MiniPay_SpeseCarta.Applica(ancora, 0));
    }

    @Test
    void seLeSommeNonTornanoNonSiAbbinaNullaENonSiDivide() {
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9.01", "8.00", "0xhash1"));
        String idPrima = a.keySet().iterator().next();
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
        assertEquals(0, MiniPay_SpeseCarta.Applica(p, 0));
        assertEquals(Set.of(idPrima), a.keySet());
        assertEquals("", a.get(idPrima)[42], "nessuna modifica, nessuno storico");
    }

    @Test
    void unPagamentoInAttesaDaSoloIlValoreEPoiLaDivisioneQuandoIlFileELoDaConcluso() {
        String conAttesa = csv(pag("2030-01-15T14:40:00Z", "settled", "7.00", "6.30", 2),
                pag("2030-01-15T14:20:00Z", "pending", "2.00", "1.80", 1));
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(conAttesa), a, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.SOLO_VALORE, p.abbinamenti.get(0).esecuzione);
        assertTrue(p.abbinamenti.get(0).motivo.contains("in attesa"), p.abbinamenti.get(0).motivo);
        MiniPay_SpeseCarta.Applica(p, 0);
        assertEquals(1, spese(a).size());
        String[] v = spese(a).get(0);
        assertEquals("2030-01-15 19:02", v[1], "l'orario della catena resta");
        assertEquals("8.10", v[15]);
        assertTrue(v[21].contains(MiniPay_SpeseCarta.ATTESA));

        //Il file dopo: tutto concluso. Il movimento gia' abbinato si divide, con le note ricostruite e il valore salvato letto dalla nota
        MiniPay_SpeseCarta.Piano dopo = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertEquals(1, dopo.abbinamenti.size(), dopo.nonAbbinati.toString());
        assertTrue(dopo.abbinamenti.get(0).giaScritto);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.DIVISIONE, dopo.abbinamenti.get(0).esecuzione);
        assertEquals(1, MiniPay_SpeseCarta.Applica(dopo, 0));
        List<String[]> r = spese(a);
        assertEquals(2, r.size());
        for (String[] pz : r) {
            assertFalse(pz[21].contains(MiniPay_SpeseCarta.ATTESA), pz[21]);
            assertEquals(1, pz[21].split("rif\\. ").length - 1, "una sola riga di pagamento per pezzo: " + pz[21]);
        }
        assertEquals("2030-01-15 15:20", r.get(0)[1]);
        assertEquals(new BigDecimal("8.10"), new BigDecimal(r.get(0)[15]).add(new BigDecimal(r.get(1)[15])));
        assertTrue(r.get(0)[21].contains("valore salvato 1,78 EUR"), r.get(0)[21]);

        //E dopo ancora non cambia piu' niente
        MiniPay_SpeseCarta.Piano ultimo = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertTrue(ultimo.abbinamenti.isEmpty());
        assertTrue(ultimo.conferme.isEmpty());
    }

    @Test
    void unAbbinamentoPassatoSenzaValoreSalvatoNellaNotaNonFaFallireTotaliERiepilogo() {
        //Abbinato con un pagamento in attesa, poi l'utente cancella dalla nota il valore salvato: l'abbinamento passato
        //non lo legge piu' (salvato == null) ma la divisione e' ancora possibile, e l'anteprima deve poterla mostrare
        String conAttesa = csv(pag("2030-01-15T14:40:00Z", "settled", "7.00", "6.30", 2),
                pag("2030-01-15T14:20:00Z", "pending", "2.00", "1.80", 1));
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        MiniPay_SpeseCarta.Applica(MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(conAttesa), a, NOMI, CAMBIO), 0);
        String[] v = spese(a).get(0);
        assertNotNull(MiniPay_SpeseCarta.SalvatoDaNota(v[21]), "prima il valore salvato si legge: " + v[21]);
        v[21] = v[21].replaceAll("valore salvato [0-9.,]+ EUR", "valore cancellato dall'utente");
        assertNull(MiniPay_SpeseCarta.SalvatoDaNota(v[21]), v[21]);

        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertEquals(1, p.abbinamenti.size(), p.nonAbbinati.toString());
        MiniPay_SpeseCarta.Abbinamento ab = p.abbinamenti.get(0);
        assertTrue(ab.giaScritto);
        assertNull(ab.salvato, "il caso che rompeva i totali");
        assertEquals(ab.reale, ab.salvatoOReale());
        assertEquals(0, ab.spread().signum(), "senza salvato nessuno spread");

        assertEquals(ab.reale, p.totaleSalvato());
        assertEquals(0, p.spreadPercentuale().signum());
        assertEquals(ab.reale, p.totali(true)[0].add(p.totali(false)[0]));
        String html = MiniPay_SpeseCarta.RiepilogoHtml(p, 15);
        assertFalse(html.isEmpty());

        //E la divisione si applica lo stesso
        assertEquals(1, MiniPay_SpeseCarta.Applica(p, 0));
        assertEquals(2, spese(a).size());
    }

    @Test
    void unMovimentoAbbinatoConLaVecchiaRegolaSiDividePerUnaImportazioneSuccessiva() {
        //Com'era prima di questa funzione: valore e note di due pagamenti sul movimento unico, orario della catena
        Map<String, String[]> a = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        MiniPay_SpeseCarta.Piano vecchio = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        MiniPay_SpeseCarta.Abbinamento ab = vecchio.abbinamenti.get(0);
        String[] v = ab.movimento;
        GUI_ModificaMovimento.ScriviSoloPrezzoENote(v, ab.reale.toPlainString(), MiniPay_SpeseCarta.Note(ab), true,
                MiniPay_SpeseCarta.FONTE_PREZZO);
        assertEquals(1, spese(a).size());
        assertEquals(2, v[21].split("rif\\. ").length - 1);

        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.DIVISIONE, p.abbinamenti.get(0).esecuzione);
        MiniPay_SpeseCarta.Applica(p, 0);
        assertEquals(2, spese(a).size());
        assertEquals(spese(a).get(0)[42], spese(a).get(1)[42]);
    }

    @Test
    void sePrimaDelGiroArrivaUnaRicaricaCheServeAlSaldoIlMovimentoRestaUnitoEDiceIlPerche() {
        //Nessun fondo prima della ricarica delle 18:30: anticipare i pagamenti alle 15:20 e 15:40 renderebbe il saldo negativo
        Map<String, String[]> a = archivio(depositoCatena("2030-01-15 18:30:00", "9"),
                spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        MiniPay_SpeseCarta.Abbinamento ab = p.abbinamenti.get(0);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.SOLO_VALORE, ab.esecuzione);
        assertTrue(ab.motivo.contains("saldo"), ab.motivo);
        assertTrue(MiniPay_SpeseCarta.RiepilogoHtml(p, 15).contains("saldo"));
        MiniPay_SpeseCarta.Applica(p, 0);
        assertEquals(1, spese(a).size(), "solo valore e note");
        assertEquals("2030-01-15 19:02", spese(a).get(0)[1]);
        assertEquals("8.10", spese(a).get(0)[15]);

        //Stessa situazione ma con fondi gia' presenti: si divide
        Map<String, String[]> b = archivio(depositoCatena("2030-01-10 10:00:00", "100"),
                depositoCatena("2030-01-15 18:30:00", "9"), spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"));
        assertEquals(MiniPay_SpeseCarta.Esecuzione.DIVISIONE,
                MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), b, NOMI, CAMBIO).abbinamenti.get(0).esecuzione);
    }

    @Test
    void unMovimentoCollegatoAdAltriNonSiDivide() {
        String[] v = spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1");
        v[20] = "20300115190207_0xsafe_002_001_CM";
        Map<String, String[]> a = archivio(v);
        MiniPay_SpeseCarta.Piano p = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), a, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.SOLO_VALORE, p.abbinamenti.get(0).esecuzione);
        assertTrue(p.abbinamenti.get(0).motivo.contains("collegato"), p.abbinamenti.get(0).motivo);

        //Un'altra riga con lo stesso hash (una commissione) lo rende non autonomo
        String[] fee = spesaCatena("2030-01-15 19:02:07", "0.01", "0.01", "0xhash1");
        fee[0] = "20300115190207_0xsafe_002_002_CM";
        Map<String, String[]> b = archivio(spesaCatena("2030-01-15 19:02:07", "9", "8.00", "0xhash1"), fee);
        MiniPay_SpeseCarta.Piano q = MiniPay_SpeseCarta.Pianifica(MiniPay_SpeseCarta.Leggi(DUE_PAGAMENTI), b, NOMI, CAMBIO);
        assertEquals(MiniPay_SpeseCarta.Esecuzione.SOLO_VALORE, q.abbinamenti.get(0).esecuzione);
    }

    @Test
    void nellOraDiOttobreCheSiRipeteIlMovimentoGiaAllOraDelPagamentoNonSiSposta() {
        //Alle 02:30 locali del 27/10/2030 ci sono due istanti, un'ora di distanza, con lo stesso prefisso dell'ID
        long primo = java.time.Instant.parse("2030-10-27T00:30:00Z").toEpochMilli();
        long secondo = java.time.Instant.parse("2030-10-27T01:30:00Z").toEpochMilli();
        String[] v = spesaCatena("2030-10-27 02:30:00", "7", "6.50", "0xhash1");
        assertTrue(MiniPay_SpeseCarta.AllOraDi(v, primo));
        assertTrue(MiniPay_SpeseCarta.AllOraDi(v, secondo), "stessa forma memorizzata: non si sposta a ogni importazione");
        assertFalse(MiniPay_SpeseCarta.AllOraDi(v, primo + 3600_000L * 2));
    }
}
