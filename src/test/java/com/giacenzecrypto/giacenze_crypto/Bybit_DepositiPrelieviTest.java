package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa {@link Bybit_DepositiPrelievi}: il file "Withdraw &amp; Deposit History" completa i depositi e i
 * prelievi già importati dagli Asset Change Details, senza crearne. Righe sintetiche, nessun dato reale.
 *
 * <p>Le date del file sono UTC, gli ID dei movimenti ora di Roma: gennaio = UTC+1.</p>
 */
class Bybit_DepositiPrelieviTest {

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /** Movimento Bybit da classificare: {@code qta} negativa = prelievo (PC), positiva = deposito (DC). */
    private static String[] movimento(String prefissoID, String causale, String moneta, String qta) {
        boolean prelievo = qta.startsWith("-");
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = prefissoID + "_Bybit_001_001_" + (prelievo ? "PC" : "DC");
        v[1] = prefissoID.substring(0, 4) + "-" + prefissoID.substring(4, 6) + "-" + prefissoID.substring(6, 8)
                + " " + prefissoID.substring(8, 10) + ":" + prefissoID.substring(10, 12);
        v[3] = "Bybit";
        v[4] = "Principale";
        v[5] = "TRASFERIMENTO-CRYPTO";
        v[7] = causale;
        if (prelievo) {
            v[8] = moneta; v[9] = "Crypto"; v[10] = qta;
        } else {
            v[11] = moneta; v[12] = "Crypto"; v[13] = qta;
        }
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(v[0], v);
        return v;
    }

    /** {@code Uid,Date,Type,Asset,Chain,Amount,Tx ID,Status,Received Address} */
    private static String[] riga(String data, String tipo, String moneta, String rete, String qta, String hash, String stato) {
        return new String[]{"1", data, tipo, moneta, rete, qta, hash, stato, "INDIRIZZO_" + hash};
    }

    private static Bybit_DepositiPrelievi.Esito arricchisci(String[]... righe) {
        return Bybit_DepositiPrelievi.Arricchisci(new ArrayList<>(List.of(righe)), new HashSet<>());
    }

    @Test
    void unDepositoArrivatoDopoLeConfermeRiceveHashIndirizzoEReteInNota() {
        //Riga 10:00:00 UTC, accredito 11:05:00 Roma = 10:05 UTC
        String[] dep = movimento("20240115110500", "Deposit", "USDT", "100");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC (BEP20)", "100.000000", "HASH1", "Completed"));
        assertEquals(1, e.arricchiti);
        assertEquals("HASH1", dep[24]);
        assertEquals("INDIRIZZO_HASH1", dep[30]);
        assertEquals("Rete Bybit: BSC (BEP20)", dep[21]);
        assertEquals("", dep[34], "la rete del token non si tocca");
    }

    @Test
    void unPrelievoAlLordoSiAbbinaAllaRigaAlNetto() {
        String[] pre = movimento("20240115110000", "", "XLM", "-176.5233");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Withdraw", "XLM", "XLM", "176.5033", "HASH2", "Transferred successfully"));
        assertEquals(1, e.arricchiti);
        assertEquals("HASH2", pre[24]);
    }

    @Test
    void unPrelievoMinoreDellImportoNettoNonSiAbbina() {
        movimento("20240115110000", "", "XLM", "-100");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Withdraw", "XLM", "XLM", "176.5033", "HASH2", "Transferred successfully"));
        assertEquals(0, e.arricchiti);
        assertEquals(1, e.nonTrovati);
    }

    @Test
    void laCausaleEscludeGirocontiELaunchpadDiPariImporto() {
        //Giroconto fra conti (nel file reale mappato IGNORA, qui presente per ipotesi) più vicino nel tempo
        String[] giroconto = movimento("20240330084400", "internalAccountTransferDeposit", "USDT", "9");
        String[] launchpad = movimento("20240330084300", "commitmentForLaunchpad", "USDT", "-9");
        String[] dep = movimento("20240330084424", "Deposit", "USDT", "9");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-03-30 07:32:04", "Deposit", "USDT", "Polygon PoS", "9", "HASH3", "Completed"),
                riga("2024-03-30 07:43:00", "Withdraw", "USDT", "Polygon PoS", "9", "HASH4", "Transferred successfully"));
        assertEquals("HASH3", dep[24]);
        assertEquals("", giroconto[24]);
        assertEquals("", launchpad[24]);
        assertEquals(1, e.arricchiti);
        assertEquals(1, e.nonTrovati);
    }

    @Test
    void unDepositoConQuantitaDiversaOFuoriFinestraNonSiAbbina() {
        movimento("20240115110500", "Deposit", "USDT", "99.9");
        movimento("20240115130000", "Deposit", "USDT", "100");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC", "100", "HASH5", "Completed"));
        assertEquals(0, e.arricchiti);
        assertEquals(1, e.nonTrovati);
    }

    @Test
    void dueCandidatiLascianoLaRigaNonAbbinata() {
        String[] a = movimento("20240115110100", "Deposit", "USDT", "100");
        String[] b = movimento("20240115110200", "Deposit", "USDT", "100");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC", "100", "HASH6", "Completed"));
        assertEquals(1, e.ambigui);
        assertEquals("", a[24]);
        assertEquals("", b[24]);
    }

    @Test
    void unaRigaNonConclusaNonCercaNessunMovimento() {
        String[] dep = movimento("20240115110100", "Deposit", "USDT", "100");
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC", "100", "HASH7", "Pending"));
        assertEquals(1, e.nonCompletati);
        assertEquals("", dep[24]);
    }

    @Test
    void scriveSoloICampiVuotiERilanciareNonCambiaNulla() {
        String[] dep = movimento("20240115110100", "Deposit", "USDT", "100");
        dep[24] = "HASH_GIA_PRESENTE";
        dep[21] = "Nota dell'utente";
        String[][] righe = {riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC", "100", "HASH8", "Completed")};
        assertEquals(1, arricchisci(righe).arricchiti);
        assertEquals("HASH_GIA_PRESENTE", dep[24]);
        assertEquals("INDIRIZZO_HASH8", dep[30]);
        assertEquals("Nota dell'utente<br>Rete Bybit: BSC", dep[21]);

        Bybit_DepositiPrelievi.Esito rilancio = arricchisci(righe);
        assertEquals(0, rilancio.arricchiti);
        assertEquals(1, rilancio.giaArricchiti);
        assertEquals("Nota dell'utente<br>Rete Bybit: BSC", dep[21]);
    }

    @Test
    void ilMovimentoDiUnAltroExchangeOCreatoInAutomaticoNonSiTocca() {
        String[] altro = movimento("20240115110100", "Deposit", "USDT", "100");
        altro[3] = "Binance";
        String[] auto = movimento("20240115110200", "Deposit", "USDT", "100");
        auto[22] = "AU";
        Bybit_DepositiPrelievi.Esito e = arricchisci(
                riga("2024-01-15 10:00:00", "Deposit", "USDT", "BSC", "100", "HASH9", "Completed"));
        assertEquals(1, e.nonTrovati);
        assertEquals("", altro[24]);
        assertEquals("", auto[24]);
    }
}
