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
 * {@link EtherFi_SpeseCarta}: lettura del CSV della carta ether.fi e abbinamento per tempo e importo coerente. CSV e
 * archivio sono interamente inventati (esercenti, importi e date di fantasia), con la stessa forma di un export vero.
 */
class EtherFi_SpeseCartaTest {

    private static final String CSV = """
            timestamp,type,description,status,amount,currency,card,card holder name,original amount,original currency,cashback earned,cashback currency,category,spending mode
            2030-01-16 17:00:00 UTC,card_spend,PK00,PENDING,2,EUR,0000,TITOLARE,2,EUR,0.05,EUR,7523 - Parking Lots and Garages,Direct Pay
            2030-01-15 22:00:00 UTC,completed_withdrawal,USDC,,500,USD,,,500,USDC,,,,
            2030-01-15 12:00:00 UTC,card_spend,BAR DI PROVA,CLEARED,15,EUR,0000,TITOLARE,15,EUR,0.5,EUR,Eating Places and Restaurants,Direct Pay
            2030-01-14 20:00:00 UTC,card_spend,NEGOZIO ONLINE,CLEARED,50,EUR,0000,TITOLARE,50,EUR,1.5,EUR,Miscellaneous & Specialty Retail Stores,Direct Pay
            2030-01-14 06:00:00 UTC,card_spend,PAYPAL,CANCELLED,1,USD,0000,TITOLARE,1,USD,0,,Miscellaneous & Specialty Retail Stores,Direct Pay
            """;

    /** Cambio fisso: 1 USD = 0,90 EUR. */
    private static final BiFunction<String, String, String> CAMBIO =
            (v, g) -> new BigDecimal(v).multiply(new BigDecimal("0.90")).toPlainString();

    private static final String WALLET = "0xSafeEtherFi";

    private static String[] spesa(String id, String dataRoma, String usd, String valore) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        //come nell'archivio vero: l'ID comincia con l'istante al secondo, il [1] e' al minuto
        v[0] = dataRoma.replaceAll("[- :]", "") + "_" + id + "_PC";
        v[1] = dataRoma.substring(0, 16);
        v[3] = WALLET + " (OP)";
        v[7] = Trans_EtherFi.CAUSALE_CARTA + (usd == null ? "" : " (USD " + usd + ")");
        v[8] = "weEUR";
        v[10] = "-12";
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
        List<EtherFi_SpeseCarta.Riga> righe = EtherFi_SpeseCarta.Leggi(CSV);
        assertEquals(5, righe.size());
        EtherFi_SpeseCarta.Riga bar = righe.get(2);
        assertTrue(bar.isSpesaConclusa());
        assertEquals(new BigDecimal("15"), bar.importo());
        assertEquals(new BigDecimal("0.5"), bar.cashback());
        assertEquals(java.time.Instant.parse("2030-01-15T12:00:00Z").toEpochMilli(), bar.istanteMs());
        assertTrue(righe.get(0).isSpesaInAttesa());
        assertTrue(righe.get(4).isSpesaAnnullata());
        assertFalse(righe.get(1).isSpesaConclusa(), "un prelievo non e' una spesa");
        assertEquals(java.time.Instant.parse("2030-01-15T12:00:00Z").toEpochMilli(), EtherFi_SpeseCarta.UltimaSpesaConclusaMs(righe));
    }

    @Test
    void intestazioneSbagliataELanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> EtherFi_SpeseCarta.Leggi("a,b,c\n1,2,3\n"));
    }

    @Test
    void abbinaPerTempoEImportoCoerente() {
        // 15 EUR alle 12:00:00Z = 13:00:00 Roma. USD 16,70 x 0,90 = 15,03 (0,2% di scarto)
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 13:00:06", "16.7", "14.80"));
        EtherFi_SpeseCarta.Piano p = EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), a, WALLET, CAMBIO);
        assertEquals(1, p.abbinamenti.size(), p.nonAbbinati.toString());
        EtherFi_SpeseCarta.Abbinamento x = p.abbinamenti.get(0);
        assertEquals(new BigDecimal("15.00"), x.reale);
        assertEquals(new BigDecimal("-0.20"), x.spread());
        assertEquals(6_000L, x.scartoMs);
        assertEquals(1, p.inAttesa);
        assertEquals(1, p.annullate);
        assertEquals(1, p.altreRighe);
        // l'altra spesa conclusa (il negozio online) non ha un movimento: e' nell'elenco dei non abbinati
        assertEquals(1, p.nonAbbinati.size());
    }

    @Test
    void importoIncoerenteNonSiAbbina() {
        // USD 30 x 0,90 = 27 EUR contro 15 addebitati
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 13:00:06", "30", "26.00"));
        EtherFi_SpeseCarta.Piano p = EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), a, WALLET, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
    }

    @Test
    void fuoriDallaFinestraDiTempoNonSiAbbina() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 14:00:06", "16.7", "14.80"));
        EtherFi_SpeseCarta.Piano p = EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), a, WALLET, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
    }

    @Test
    void dueCandidatiPlausibiliNonSiScelgono() {
        String csv = """
                timestamp,type,description,status,amount,currency,card,card holder name,original amount,original currency,cashback earned,cashback currency,category,spending mode
                2030-01-15 12:10:00 UTC,card_spend,UNO,CLEARED,10,EUR,0000,T,10,EUR,0,EUR,x,Direct Pay
                2030-01-15 12:10:20 UTC,card_spend,DUE,CLEARED,10.2,EUR,0000,T,10.2,EUR,0,EUR,x,Direct Pay
                """;
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 13:10:10", "11.2", "9.80"));
        EtherFi_SpeseCarta.Piano p = EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(csv), a, WALLET, CAMBIO);
        assertTrue(p.abbinamenti.isEmpty());
        assertTrue(p.nonAbbinati.stream().anyMatch(s -> s.contains("ugualmente plausibili")), p.nonAbbinati.toString());
    }

    @Test
    void senzaDollariNellaCausaleServeUnoScartoDiTempoMinimo() {
        Map<String, String[]> lontano = archivio(spesa("A", "2030-01-15 13:04:41", null, "14.80"));
        assertTrue(EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), lontano, WALLET, CAMBIO).abbinamenti.isEmpty());
        Map<String, String[]> vicino = archivio(spesa("A", "2030-01-15 13:00:31", null, "14.80"));
        assertEquals(1, EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), vicino, WALLET, CAMBIO).abbinamenti.size());
    }

    @Test
    void applicaScriveValoreNoteCashbackESiPuoRipetere() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 13:00:06", "16.7", "14.80"));
        List<EtherFi_SpeseCarta.Riga> righe = EtherFi_SpeseCarta.Leggi(CSV);
        EtherFi_SpeseCarta.Piano p = EtherFi_SpeseCarta.Pianifica(righe, a, WALLET, CAMBIO);
        assertEquals(1, EtherFi_SpeseCarta.Applica(p, 0));
        String[] v = a.values().iterator().next();
        assertEquals("15.00", v[15]);
        assertEquals("SI", v[32]);
        assertTrue(v[21].contains("BAR DI PROVA, Eating Places and Restaurants"), v[21]);
        assertTrue(v[21].contains("cashback maturato 0,50 EUR"), v[21]);
        assertTrue(v[21].contains("spread -0,20 EUR (-1,33%)"), v[21]);
        assertFalse(v[21].contains(";"));
        assertFalse(v[21].contains("TITOLARE"), "il nome del titolare non entra nelle note");
        assertFalse(v[42].isBlank());

        EtherFi_SpeseCarta.Piano ancora = EtherFi_SpeseCarta.Pianifica(righe, a, WALLET, CAMBIO);
        assertTrue(ancora.abbinamenti.isEmpty(), "secondo passaggio: niente da rifare");
        assertEquals(1, ancora.movimentiGiaAbbinati);
        assertEquals(1, ancora.pagamentiGiaAbbinati);
    }

    @Test
    void ignoraLeSpeseDiUnAltroWallet() {
        String[] v = spesa("A", "2030-01-15 13:00:06", "16.7", "14.80");
        v[3] = "0xAltro (OP)";
        assertTrue(EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), archivio(v), WALLET, CAMBIO).abbinamenti.isEmpty());
    }

    @Test
    void walletCompatibiliSonoSoloLeSafeRiconosciute() {
        List<String> tabella = List.of("0xSafeEtherFi;OP;x", "0xAltroOP;OP;x", "0xSafeEtherFi;ETH;x");
        assertEquals(List.of("0xSafeEtherFi"), EtherFi_SpeseCarta.WalletCompatibili(tabella, Set.of("0xsafeetherfi")));
        assertTrue(EtherFi_SpeseCarta.WalletCompatibili(tabella, Set.of()).isEmpty());
    }

    @Test
    void riepilogoMostraTotaliScartoECashback() {
        Map<String, String[]> a = archivio(spesa("A", "2030-01-15 13:00:06", "16.7", "14.80"));
        String html = EtherFi_SpeseCarta.RiepilogoHtml(
                EtherFi_SpeseCarta.Pianifica(EtherFi_SpeseCarta.Leggi(CSV), a, WALLET, CAMBIO), 15);
        assertTrue(html.contains("Spread complessivo: <b>-0,20 EUR (-1,33%)</b>"), html);
        assertTrue(html.contains("Cashback maturato nelle stesse spese: 0,50 EUR"), html);
        assertTrue(html.contains("Spese in attesa, ignorate: <b>1</b>"), html);
    }

    @Test
    void dollariDellaCausale() {
        assertEquals(new BigDecimal("40.123456"), EtherFi_SpeseCarta.DollariDellaCausale("Pagamento con carta ether.fi (USD 40.123456)"));
        assertNull(EtherFi_SpeseCarta.DollariDellaCausale("Pagamento con carta ether.fi"));
    }
}
