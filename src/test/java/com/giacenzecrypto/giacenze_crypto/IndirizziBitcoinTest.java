package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * Indirizzi Bitcoin da xpub/ypub/zpub senza bitcoinj: vettori di prova ufficiali (BIP32, la frase
 * "abandon ... about" di BIP44/49/84, RIPEMD-160 dalla specifica) e i casi da rifiutare.
 *
 * <p>Prima di togliere bitcoinj l'implementazione è stata confrontata con esso su 150 chiavi casuali
 * (7400 fra indirizzi, figli serializzati e stringhe Base58): nessuna differenza.
 */
class IndirizziBitcoinTest {

    // Frase di prova "abandon abandon ... about" (BIP39), conti m/44'/0'/0', m/49'/0'/0', m/84'/0'/0'
    private static final String XPUB_44 = "xpub6BosfCnifzxcFwrSzQiqu2DBVTshkCXacvNsWGYJVVhhawA7d4R5WSWGFNbi8Aw6ZRc1brxMyWMzG3DSSSSoekkudhUd9yLb6qx39T9nMdj";
    private static final String YPUB_49 = "ypub6Ww3ibxVfGzLrAH1PNcjyAWenMTbbAosGNB6VvmSEgytSER9azLDWCxoJwW7Ke7icmizBMXrzBx9979FfaHxHcrArf3zbeJJJUZPf663zsP";
    private static final String ZPUB_84 = "zpub6rFR7y4Q2AijBEqTUquhVz398htDFrtymD9xYYfG1m4wAcvPhXNfE3EfH1r1ADqtfSdVCToUG868RvUUkgDKf31mGDtKsAYz2oz2AGutZYs";

    private static String indirizzo(String chiave, int catena, int indice, IndirizziBitcoin.Formato f) {
        return IndirizziBitcoin.ChiaveEstesa.Leggi(chiave).Figlia(catena).Figlia(indice).Indirizzo(f);
    }

    @Test
    void legacyDaXpubBip44() {
        assertEquals("1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA", indirizzo(XPUB_44, 0, 0, IndirizziBitcoin.Formato.P2PKH));
        assertEquals("1Ak8PffB2meyfYnbXZR9EGfLfFZVpzJvQP", indirizzo(XPUB_44, 0, 1, IndirizziBitcoin.Formato.P2PKH));
        assertEquals("1J3J6EvPrv8q6AC3VCjWV45Uf3nssNMRtH", indirizzo(XPUB_44, 1, 0, IndirizziBitcoin.Formato.P2PKH));
    }

    @Test
    void segwitCompatibileDaYpubBip49() {
        assertEquals("37VucYSaXLCAsxYyAPfbSi9eh4iEcbShgf", indirizzo(YPUB_49, 0, 0, IndirizziBitcoin.Formato.P2SH_P2WPKH));
        assertEquals("3LtMnn87fqUeHBUG414p9CWwnoV6E2pNKS", indirizzo(YPUB_49, 0, 1, IndirizziBitcoin.Formato.P2SH_P2WPKH));
        assertEquals("34K56kSjgUCUSD8GTtuF7c9Zzwokbs6uZ7", indirizzo(YPUB_49, 1, 0, IndirizziBitcoin.Formato.P2SH_P2WPKH));
    }

    @Test
    void segwitNativoDaZpubBip84() {
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", indirizzo(ZPUB_84, 0, 0, IndirizziBitcoin.Formato.P2WPKH));
        assertEquals("bc1qnjg0jd8228aq7egyzacy8cys3knf9xvrerkf9g", indirizzo(ZPUB_84, 0, 1, IndirizziBitcoin.Formato.P2WPKH));
        assertEquals("bc1q8c6fshw2dlwun7ekn9qwf37cu2rn755upcp6el", indirizzo(ZPUB_84, 1, 0, IndirizziBitcoin.Formato.P2WPKH));
    }

    /** Vettori 1 e 2 di BIP32: figlio non hardened di una xpub, serializzato di nuovo per intero. */
    @Test
    void figlioPubblicoBip32() {
        assertEquals("xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ",
                IndirizziBitcoin.ChiaveEstesa.Leggi(
                        "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw")
                        .Figlia(1).SerializzaXpub());
        assertEquals("xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH",
                IndirizziBitcoin.ChiaveEstesa.Leggi(
                        "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB")
                        .Figlia(0).SerializzaXpub());
    }

    @Test
    void chiaviDaRifiutare() {
        //un carattere cambiato: checksum errato
        String alterata = ZPUB_84.substring(0, 20) + (ZPUB_84.charAt(20) == 'a' ? 'b' : 'a') + ZPUB_84.substring(21);
        assertThrows(IllegalArgumentException.class, () -> IndirizziBitcoin.ChiaveEstesa.Leggi(alterata));
        //la stessa chiave con la versione tpub di testnet, checksum corretto: rifiutata per la versione,
        //come faceva bitcoinj con MainNetParams
        byte[] dati = IndirizziBitcoin.DecodificaBase58Verificata(XPUB_44);
        dati[0] = 0x04;
        dati[1] = 0x35;
        dati[2] = (byte) 0x87;
        dati[3] = (byte) 0xCF;
        String tpub = IndirizziBitcoin.CodificaBase58Verificata(dati);
        assertTrue(tpub.startsWith("tpub"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> IndirizziBitcoin.ChiaveEstesa.Leggi(tpub));
        assertTrue(e.getMessage().contains("non supportata"), e.getMessage());
        //caratteri fuori dall'alfabeto Base58 (0, O, I, l)
        assertThrows(IllegalArgumentException.class, () -> IndirizziBitcoin.ChiaveEstesa.Leggi("xpub0OIl"));
        //indice hardened: da una chiave pubblica non si ricava
        IndirizziBitcoin.ChiaveEstesa k = IndirizziBitcoin.ChiaveEstesa.Leggi(XPUB_44);
        assertThrows(IllegalArgumentException.class, () -> k.Figlia(0x80000000));
    }

    /** Base58 senza checksum, usato per riconoscere gli indirizzi Solana (32 byte), zeri iniziali compresi. */
    @Test
    void base58Grezzo() {
        assertArrayEquals(new byte[32], IndirizziBitcoin.DecodificaBase58("11111111111111111111111111111111"));
        byte[] tokenProgram = IndirizziBitcoin.DecodificaBase58("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA");
        assertEquals(32, tokenProgram.length);
        assertEquals("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", IndirizziBitcoin.CodificaBase58(tokenProgram));
        assertEquals("1112", IndirizziBitcoin.CodificaBase58(new byte[]{0, 0, 0, 1}));
        assertArrayEquals(new byte[]{0, 0, 0, 1}, IndirizziBitcoin.DecodificaBase58("1112"));
        assertArrayEquals(new byte[0], IndirizziBitcoin.DecodificaBase58(""));
        assertThrows(IllegalArgumentException.class, () -> IndirizziBitcoin.DecodificaBase58("abc0"));
    }

    /** Vettori della specifica di RIPEMD-160, compreso il milione di "a" (molti blocchi). */
    @Test
    void ripemd160() {
        assertEquals("9c1185a5c5e9fc54612808977ee8f548b2258d31", ripemd(""));
        assertEquals("0bdc9d2d256b3ee9daae347be6f4dc835a467ffe", ripemd("a"));
        assertEquals("8eb208f7e05d987a9b044a8e98c6b087f15a0bfc", ripemd("abc"));
        assertEquals("5d0689ef49d2fae572b881b123a85ffa21595f36", ripemd("message digest"));
        assertEquals("12a053384a9c0c88e405a06c27dcf49ada62eb2b",
                ripemd("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"));
        assertEquals("b0e20b6e3116640286ed3a87a5713079b21f5189",
                ripemd("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"));
        assertEquals("52783243c1697bdbe16d37f97f68f08325dc1528", ripemd("a".repeat(1_000_000)));
    }

    private static String ripemd(String s) {
        return HexFormat.of().formatHex(IndirizziBitcoin.Ripemd160(s.getBytes(StandardCharsets.US_ASCII)));
    }
}
