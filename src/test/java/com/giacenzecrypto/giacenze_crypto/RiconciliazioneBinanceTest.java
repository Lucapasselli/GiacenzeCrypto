package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * L'integrazione fra le due fonti di Binance (CSV e API): quello che l'archivio ha già, in un'altra forma, non rientra.
 * Dati inventati, con la forma reale delle righe viste su un conto vero (2026-11-14).
 */
class RiconciliazioneBinanceTest {

    /** Riga di movimento a 46 campi, solo quelli che la riconciliazione legge. */
    private static String[] riga(String id, String tipo, String causale, String monU, String qtaU, String monE, String qtaE,
            String chiave43) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = id;
        v[3] = "Binance";
        v[5] = tipo;
        v[7] = causale;
        v[8] = monU;
        v[10] = qtaU;
        v[11] = monE;
        v[13] = qtaE;
        v[43] = chiave43;
        return v;
    }

    private static String[] commissione(String id, String moneta, String qta, String chiave) {
        return riga(id, "COMMISSIONI", "", moneta, qta, "", "", chiave);
    }

    private static List<String[]> l(String[]... r) {
        return new ArrayList<>(List.of(r));
    }

    @Test
    void ConvertConUnSecondoDiScartoEGiaPresente() {
        List<String[]> archivio = l(riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "convert/tradeFlow (API)", "EUR", "-310.40", "XRP", "150", ""));
        List<String[]> arrivo = l(riga("20250221122222_Binance_000_001_AC", "ACQUISTO CRYPTO", "Binance Convert - Binance Convert", "EUR", "-310.40", "XRP", "150", ""));
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertTrue(r.nuove.isEmpty());
        assertEquals(1, r.giaPresenti.size());
    }

    @Test
    void QuantitaDiversaNonEUnDoppione() {
        List<String[]> archivio = l(riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-310.40", "XRP", "150", ""));
        List<String[]> arrivo = l(riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-310.40", "XRP", "151", ""));
        assertEquals(1, RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.size());
    }

    @Test
    void DueConvertIdenticiInArrivoControUnoInArchivioNeFaEntrareUno() {
        List<String[]> archivio = l(riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", ""));
        List<String[]> arrivo = l(
                riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", "") ,
                riga("20250221122222_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", ""));
        // le due righe hanno lo stesso secondo solo se compaiono insieme: qui sono secondi diversi, quindi due unità
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertEquals(1, r.nuove.size());
        assertEquals(1, r.giaPresenti.size());
    }

    @Test
    void DepositoFiatLordoConCommissioneContraNettoDelCsv() {
        // archivio da API: lordo 301 e commissione 0,60 collegata; arrivo dal CSV: netto 300,40, orario due giorni prima
        List<String[]> archivio = l(
                riga("20240221142216_Binance_000_001_DF", "DEPOSITO FIAT", "Metodo di pagamento", "", "", "EUR", "301", "k1"),
                commissione("20240221142216_BinanceC_001_002_CM", "EUR", "-0.6", "k1"));
        List<String[]> arrivo = l(riga("20240219142216_Binance_000_001_DF", "DEPOSITO FIAT", "Deposit", "", "", "EUR", "300.4", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.isEmpty());
    }

    @Test
    void DepositoFiatNettoDelCsvPoiLordoEcommissioneDallApiNonRientrano() {
        List<String[]> archivio = l(riga("20240219142216_Binance_000_001_DF", "DEPOSITO FIAT", "Deposit", "", "", "EUR", "300.4", ""));
        List<String[]> arrivo = l(
                riga("20240221142216_Binance_000_001_DF", "DEPOSITO FIAT", "Metodo di pagamento", "", "", "EUR", "301", "k1"),
                commissione("20240221142216_BinanceC_001_002_CM", "EUR", "-0.6", "k1"));
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertTrue(r.nuove.isEmpty(), "il deposito e la sua commissione seguono lo stesso destino");
        assertEquals(2, r.giaPresenti.size());
    }

    @Test
    void PrelievoCriptoConCommissioneCompresaNelCsvEOrarioDiversoEGiaPresente() {
        List<String[]> archivio = l(
                riga("20250708144315_Binance_001_001_PC", "PRELIEVO CRYPTO", "", "ADA", "-404.065", "", "", "k2"),
                commissione("20250708144315_BinanceC_001_002_CM", "ADA", "-1.5", "k2"));
        List<String[]> arrivo = l(riga("20250708145405_Binance_001_001_PC", "PRELIEVO CRYPTO", "Withdraw", "ADA", "-405.565", "", "", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.isEmpty());
    }

    @Test
    void TradeEseguitoAPezziDallApiContoUnaRigaDelCsvConLeCommissioni() {
        // archivio da API: due esecuzioni dello stesso secondo, ognuna con la sua commissione
        List<String[]> archivio = l(
                riga("20250410155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-0.052", "USDT", "6.05", "a"),
                commissione("20250410155630_BinanceC_001_002_CM", "USDT", "-0.0060", "a"),
                riga("20250410155630_Binance_002_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-0.1", "USDT", "12", "b"),
                commissione("20250410155630_BinanceC_002_002_CM", "USDT", "-0.012", "b"));
        // arrivo dal CSV: una riga sola per secondo, commissioni sommate in una riga sola con la stessa chiave
        List<String[]> arrivo = l(
                riga("20250410155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "Transaction Sold", "SOL", "-0.152", "USDT", "18.05", "c"),
                commissione("20250410155630_BinanceC_001_002_CM", "USDT", "-0.0180", "c"));
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertTrue(r.nuove.isEmpty(), "scambio e commissione già presenti: " + r.perMotivo);
    }

    @Test
    void TradeSpezzatoInArrivoControRigaUnicaInArchivioEGiaPresente() {
        List<String[]> archivio = l(riga("20250410155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-0.152", "USDT", "18.05", ""));
        List<String[]> arrivo = l(
                riga("20250410155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-0.052", "USDT", "6.05", "a"),
                riga("20250410155630_Binance_002_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-0.1", "USDT", "12", "b"),
                commissione("20250410155630_BinanceC_001_002_CM", "USDT", "-0.0060", "a"));
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertTrue(r.nuove.isEmpty(), "la commissione segue lo scambio: " + r.perMotivo);
    }

    @Test
    void CommissioneDiUnMovimentoNuovoRestaECommissioneOrfanaNonSiScarta() {
        List<String[]> archivio = l(riga("20250410155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-1", "USDT", "100", "a"));
        List<String[]> arrivo = l(
                riga("20250411155630_Binance_001_001_SC", "SCAMBIO CRYPTO", "", "SOL", "-2", "USDT", "200", "z"),
                commissione("20250411155630_BinanceC_001_002_CM", "USDT", "-0.2", "z"));
        assertEquals(2, RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.size());
    }

    @Test
    void PremiLockedDelGiornoSiConfrontanoPerSomma() {
        // archivio da API: tre accrediti dello stesso giorno; arrivo dal CSV: una riga con la somma
        List<String[]> archivio = l(
                riga("20260219010101_Binance_001_001_RW", "EARN", "LOCKED_REWARD", "", "", "ADA", "0.004", ""),
                riga("20260219020202_Binance_001_001_RW", "EARN", "LOCKED_REWARD", "", "", "ADA", "0.004", ""),
                riga("20260219030303_Binance_001_001_RW", "EARN", "LOCKED_REWARD", "", "", "ADA", "0.00105", ""));
        List<String[]> arrivo = l(riga("20260219025959_Binance_001_001_RW", "EARN", "Simple Earn Locked Rewards", "", "", "ADA", "0.00905", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.isEmpty());
    }

    @Test
    void PremioRealtimeConGiornoSlittatoDiUnoEGiaPresenteMaAltraSommaNo() {
        List<String[]> archivio = l(riga("20250219015959_Binance_001_001_RW", "EARN", "REALTIME", "", "", "DOGE", "0.00333", ""));
        List<String[]> stessa = l(riga("20250218020000_Binance_001_001_RW", "EARN", "Real-time APR Rewards", "", "", "DOGE", "0.00333", ""));
        List<String[]> altra = l(riga("20250218020000_Binance_001_001_RW", "EARN", "Real-time APR Rewards", "", "", "DOGE", "0.002", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(stessa, archivio).nuove.isEmpty());
        assertEquals(1, RiconciliazioneBinance.Riconcilia(altra, archivio).nuove.size());
    }

    @Test
    void FamiglieDiPremioDiverseNonSiScambiano() {
        List<String[]> archivio = l(riga("20260219010101_Binance_001_001_RW", "EARN", "LOCKED_REWARD", "", "", "ADA", "0.01", ""));
        List<String[]> arrivo = l(riga("20260219010101_Binance_001_001_RW", "EARN", "REALTIME", "", "", "ADA", "0.01", ""));
        assertEquals(1, RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.size());
    }

    @Test
    void AcquistoConSaldoInUnaRigaContoDueRigheDelCsv() {
        List<String[]> archivio = l(riga("20240509110419_Binance_001_001_AC", "ACQUISTO CRYPTO", "Cash Balance", "EUR", "-100", "SOL", "1.5", ""));
        List<String[]> arrivo = l(
                riga("20240509110419_Binance_001_001_PF", "PRELIEVO FIAT", "Buy Crypto With Fiat", "EUR", "-100", "", "", ""),
                riga("20240509110420_Binance_001_001_DC", "DEPOSITO CRYPTO", "Buy Crypto With Fiat", "", "", "SOL", "1.5", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.isEmpty());
    }

    @Test
    void AirdropConAltraCausaleEGiorniDiScartoEGiaPresente() {
        List<String[]> archivio = l(riga("20250608044246_Binance_001_001_RW", "EARN", "SOL claim boost reward", "", "", "PIXEL", "3.25", ""));
        List<String[]> arrivo = l(riga("20250608120000_Binance_001_001_RW", "STAKING REWARD", "BNSOL Staking - Extra Rewards", "", "", "PIXEL", "3.25", ""));
        assertTrue(RiconciliazioneBinance.Riconcilia(arrivo, archivio).nuove.isEmpty());
    }

    @Test
    void ILuoghiCheNonSonoBinanceNonSiToccano() {
        String[] altro = riga("20250221122221_Bybit_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", "");
        altro[3] = "Bybit";
        List<String[]> archivio = l(altro);
        String[] stesso = altro.clone();
        List<String[]> arrivo = l(stesso);
        RiconciliazioneBinance.Risultato r = RiconciliazioneBinance.Riconcilia(arrivo, archivio);
        assertEquals(1, r.nuove.size());
        assertSame(stesso, r.nuove.get(0));
    }

    @Test
    void ArchivioSenzaBinanceNonScartaNulla() {
        List<String[]> arrivo = l(riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", ""));
        assertEquals(1, RiconciliazioneBinance.Riconcilia(arrivo, new ArrayList<>()).nuove.size());
    }

    @Test
    void IlMovimentoEsistenteNonVieneMaiModificato() {
        String[] esistente = riga("20250221122221_Binance_001_001_AC", "ACQUISTO CRYPTO", "x", "EUR", "-10", "XRP", "3", "");
        String[] copia = esistente.clone();
        List<String[]> arrivo = l(riga("20250221122222_Binance_001_001_AC", "ACQUISTO CRYPTO", "y", "EUR", "-10", "XRP", "3", ""));
        RiconciliazioneBinance.Riconcilia(arrivo, l(esistente));
        assertArrayEquals(copia, esistente);
    }
}
