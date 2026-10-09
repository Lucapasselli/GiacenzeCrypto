package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abbinamento dei movimenti della carta OKX ({@link OKX_CartaAbbina}): il bill 325 dell'exchange con la ricarica sul
 * wallet della carta, e il cashback riconosciuto dal mittente delle ricariche. Indirizzi inventati.
 */
class OKX_CartaAbbinaTest {

    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String CARTA = W + " (XLAYER)";
    private static final String HOT = "0xbbbb000000000000000000000000000000000002";
    private static final String ALTRO = "0xcccc000000000000000000000000000000000003";

    @BeforeEach
    @AfterEach
    void pulisce() {
        MappaCryptoWallet.clear();
    }

    private static String[] riga(String id) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        MappaCryptoWallet.put(id, v);
        return v;
    }

    private static String[] bill325(String quando, String qta) {
        String[] v = riga(quando + "_OKX200061141986_001_001_PC");
        v[3] = "OKX";
        v[4] = "Principale";
        v[7] = OKX_CartaAbbina.CAUSALE_325 + " - Transfer from exchange to smart wallet";
        v[8] = "USDG";
        v[10] = "-" + qta;
        return v;
    }

    private static String[] depositoCarta(String quando, String qta, String mittente) {
        String[] v = riga(quando + "_" + CARTA + "_001_001_DC");
        v[3] = CARTA;
        v[4] = "Wallet";
        v[5] = "DEPOSITO CRYPTO";
        v[11] = "USDG";
        v[13] = qta;
        v[22] = "A";
        v[30] = mittente;
        return v;
    }

    private static void movimentoOKX(String quando) {
        String[] v = riga(quando + "_OKX3994783864294264834_001_001_SC");
        v[3] = "OKX";
    }

    @Test
    void ilBill325ELaRicaricaDiventanoUnTrasferimentoEIlDepositoSuccessivoEIlCashback() {
        String[] p = bill325("20261004121642", "111.59");
        String[] ricarica = depositoCarta("20261004121716", "111.59", HOT);
        String[] cashback = depositoCarta("20261009100116", "0.1", HOT);
        movimentoOKX("20261009163036");

        OKX_CartaAbbina.Esito e = OKX_CartaAbbina.Abbina(MappaCryptoWallet, W);
        assertEquals(1, e.ricariche());
        assertEquals(1, e.cashback());
        assertTrue(p[18].startsWith("PTW"), p[18]);
        assertTrue(ricarica[18].startsWith("DTW"), ricarica[18]);
        assertEquals(p[0], ricarica[20]);
        assertEquals("CASHBACK", cashback[5]);
        assertEquals(OKX_CartaAbbina.DETTAGLIO_CASHBACK, cashback[18]);

        //Rifatto non cambia nulla
        OKX_CartaAbbina.Esito ancora = OKX_CartaAbbina.Abbina(MappaCryptoWallet, W);
        assertEquals(0, ancora.ricariche() + ancora.cashback());
    }

    @Test
    void ilCashbackSiDecideSoloQuandoLArchivioOKXArrivaOltreQuellIstante() {
        bill325("20261004121642", "111.59");
        depositoCarta("20261004121716", "111.59", HOT);
        String[] dopo = depositoCarta("20261009100116", "0.1", HOT);
        movimentoOKX("20261009100000");   //l'archivio OKX si ferma prima: il 325 di questo deposito potrebbe mancare

        OKX_CartaAbbina.Esito e = OKX_CartaAbbina.Abbina(MappaCryptoWallet, W);
        assertEquals(1, e.ricariche());
        assertEquals(0, e.cashback());
        assertTrue(dopo[18].isBlank());

        movimentoOKX("20261009120000");
        assertEquals(1, OKX_CartaAbbina.Abbina(MappaCryptoWallet, W).cashback());
    }

    @Test
    void unaRicaricaSenzaIlSuo325NonEUnCashbackSeIl325ArrivaDopo() {
        //Importato prima il wallet della carta: il deposito resta un deposito finché OKX non arriva oltre
        String[] ricarica = depositoCarta("20261004121716", "111.59", HOT);
        assertEquals(0, OKX_CartaAbbina.Abbina(MappaCryptoWallet, W).cashback());
        bill325("20261004121642", "111.59");
        movimentoOKX("20261005080000");
        OKX_CartaAbbina.Esito e = OKX_CartaAbbina.Abbina(MappaCryptoWallet, W);
        assertEquals(1, e.ricariche());
        assertEquals(0, e.cashback());
        assertTrue(ricarica[18].startsWith("DTW"));
    }

    @Test
    void unDepositoDaUnAltroIndirizzoNonEUnCashbackEUnaQuantitaDiversaNonEUnaRicarica() {
        String[] p = bill325("20261004121642", "111.59");
        String[] diverso = depositoCarta("20261004121716", "111.58", HOT);
        String[] altro = depositoCarta("20261009100116", "0.1", ALTRO);
        movimentoOKX("20261010000000");
        OKX_CartaAbbina.Esito e = OKX_CartaAbbina.Abbina(MappaCryptoWallet, W);
        assertEquals(0, e.ricariche());
        assertEquals(0, e.cashback(), "nessuna ricarica abbinata: il mittente delle ricariche non e' noto");
        assertTrue(p[18].isBlank());
        assertTrue(diverso[18].isBlank());
        assertTrue(altro[18].isBlank());
    }

    @Test
    void iBill325NonAbbinatiDellArchivioServonoATrovareIlWallet() {
        bill325("20261004121642", "111.59");
        String[] abbinato = bill325("20261001080000", "5");
        abbinato[18] = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        java.util.List<OKX_WalletCarta.Trasferimento> t = OKX_CartaAbbina.TrasferimentiDaArchivio(MappaCryptoWallet);
        assertEquals(1, t.size(), "solo quello non ancora abbinato");
        assertEquals("USDG", t.get(0).moneta());
        assertEquals(0, new java.math.BigDecimal("111.59").compareTo(t.get(0).qta()));
        assertEquals(FunzioniDate.ConvertiDataIDinLong("20261004121642"), t.get(0).ts());
    }

    @Test
    void unDepositoFuoriDallaFinestraDelBillNonEUnaRicarica() {
        bill325("20261004121642", "111.59");
        String[] tardi = depositoCarta("20261004123500", "111.59", HOT);   //18 minuti dopo
        assertEquals(0, OKX_CartaAbbina.Abbina(MappaCryptoWallet, W).ricariche());
        assertTrue(tardi[18].isBlank());
    }
}
