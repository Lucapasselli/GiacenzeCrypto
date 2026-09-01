package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Verifica che {@link SegnalazioneScrub#redigi} tolga le forme identificanti note e — altrettanto
 * importante — che <b>non</b> tocchi il contenuto diagnostico legittimo (ID movimento, ID Earn,
 * billId OKX). &Egrave; il test che d&agrave; sostanza alla parola "anonimizzato".
 */
class SegnalazioneScrubTest {

    @Test
    void rimuoveIndirizziHashPathCredenzialiEmailIban() {
        String log = String.join("\n",
                "2026-08-01 wallet 0x1234567890abcdefABCDEF1234567890deadBEEF ricevuto",
                "tx 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08 confermata",
                "solana account 4Nd1mBQtrMJVYVfKf2PJy9NkxUZdD1Y9K9rQ2mWn7Hq8 saldo",
                "caricato /home/luca/Documenti/NetBeans/Giacenze_Crypto2/test/2025/movimenti.crypto.db",
                "path windows C:\\Users\\Luca\\AppData\\Roaming\\GiacenzeCrypto",
                "Binance_Trades.js apiKey=AKIA1234567890SECRETKEY secret=abcd1234efgh5678",
                "explorer https://api.etherscan.io/api?module=account&apikey=ABCD1234EFGH5678IJKL&address=x",
                "contatto utente mario.rossi@example.com",
                "bonifico IT60X0542811101000000123456 eseguito");

        String out = SegnalazioneScrub.redigi(log);

        assertFalse(out.contains("0x1234567890abcdefABCDEF1234567890deadBEEF"), "indirizzo EVM");
        assertFalse(out.contains("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"), "hash tx");
        assertFalse(out.contains("4Nd1mBQtrMJVYVfKf2PJy9NkxUZdD1Y9K9rQ2mWn7Hq8"), "indirizzo Solana");
        assertFalse(out.contains("/home/luca/"), "path home unix");
        assertTrue(out.contains("/home/utente/"), "il prefisso /home/ resta, il nome no");
        assertFalse(out.contains("C:\\Users\\Luca\\"), "path home windows");
        assertFalse(out.contains("AKIA1234567890SECRETKEY"), "valore apiKey");
        assertFalse(out.contains("abcd1234efgh5678"), "valore secret");
        assertFalse(out.contains("ABCD1234EFGH5678IJKL"), "apikey in query-string");
        assertFalse(out.contains("mario.rossi@example.com"), "email");
        assertFalse(out.contains("IT60X0542811101000000123456"), "IBAN");
    }

    @Test
    void nonRedigeGliIdentificativiDiagnosticiLegittimi() {
        String log = String.join("\n",
                "movimento 20260719120000_1_AC importato con successo",
                "OKX ordId 3792016468867482301 billId 209876543210",
                "Earn EARN-CRO-20260719 consolidato",
                "categoria RW, campo5 STAKING REWARDS, campo18 vuoto",
                "scaricati 1234 movimenti da CoinTracking CSV, 5 sconosciuti");

        String out = SegnalazioneScrub.redigi(log);

        assertTrue(out.contains("20260719120000_1_AC"), "ID movimento deve sopravvivere");
        assertTrue(out.contains("3792016468867482301"), "ordId OKX deve sopravvivere");
        assertTrue(out.contains("EARN-CRO-20260719"), "ID Earn deve sopravvivere");
        assertTrue(out.contains("STAKING REWARDS"), "testo diagnostico intatto");
        assertEquals(log, out, "nessuna modifica su questo blocco");
    }

    @Test
    void nullDiventaStringaVuota() {
        assertEquals("", SegnalazioneScrub.redigi(null));
    }
}
