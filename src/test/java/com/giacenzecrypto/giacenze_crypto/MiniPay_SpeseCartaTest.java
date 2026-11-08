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
        assertTrue(righe.get(0).isPagamentoInAttesa());
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
        assertEquals(1, p.pagamentiInAttesa);
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
        assertTrue(html.contains("Pagamenti in attesa, ignorati: <b>1</b>"), html);
        assertFalse(html.contains("Non abbinati"), html);
    }
}
