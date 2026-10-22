package com.giacenzecrypto.giacenze_crypto;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Indirizzi Bitcoin ricavati da una chiave pubblica estesa (xpub, ypub, zpub), con il solo JDK.
 *
 * <p>Sostituisce bitcoinj, che con bouncycastle, guava e protobuf pesava 14 MB del jar per queste
 * poche funzioni: derivazione pubblica BIP32 (solo figli non hardened, gli unici ricavabili da una
 * chiave pubblica), Base58Check, RIPEMD-160, Bech32 (BIP173) e i tre formati di indirizzo P2PKH (1...),
 * P2SH-P2WPKH (3...) e P2WPKH (bc1q...). Niente chiavi private: qui non si firma niente.
 *
 * <p>Il JDK non ha più la curva secp256k1 (tolta da SunEC nel JDK 16) né RIPEMD-160, quindi stanno qui.
 * L'aritmetica sulla curva è quella dei libri, in coordinate affini con {@link BigInteger}: non è a
 * tempo costante, e non serve, perché lavora solo su dati pubblici. Ricavare un indirizzo costa qualche
 * millisecondo, contro i 120 ms di pausa che {@link Trans_Bitcoin} fa comunque fra due interrogazioni.
 *
 * <p>Verificata contro bitcoinj su chiavi casuali e con i vettori di prova di BIP32, BIP49 e BIP84
 * ({@code IndirizziBitcoinTest}).
 */
final class IndirizziBitcoin {

    private IndirizziBitcoin() {
    }

    /** Il formato dell'indirizzo, che per le chiavi estese si deduce dal prefisso (BIP44, BIP49, BIP84). */
    enum Formato {
        /** Legacy, 1... (xpub) */
        P2PKH,
        /** SegWit compatibile, 3... (ypub) */
        P2SH_P2WPKH,
        /** SegWit nativo, bc1q... (zpub) */
        P2WPKH
    }

    // =================================================================================================
    // CURVA secp256k1
    // =================================================================================================
    private static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    private static final BigInteger N = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final BigInteger GX = new BigInteger(
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    private static final BigInteger GY = new BigInteger(
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    private static final BigInteger SETTE = BigInteger.valueOf(7);

    /** Un punto della curva in coordinate affini; {@code null} è il punto all'infinito. */
    private record Punto(BigInteger x, BigInteger y) {
    }

    private static final Punto G = new Punto(GX, GY);

    private static Punto Somma(Punto a, Punto b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        BigInteger lambda;
        if (a.x.equals(b.x)) {
            if (!a.y.equals(b.y) || a.y.signum() == 0) {
                return null;   //a + (-a)
            }
            lambda = a.x.pow(2).multiply(BigInteger.valueOf(3))
                    .multiply(a.y.shiftLeft(1).modInverse(P)).mod(P);
        } else {
            lambda = b.y.subtract(a.y).multiply(b.x.subtract(a.x).modInverse(P)).mod(P);
        }
        BigInteger x = lambda.pow(2).subtract(a.x).subtract(b.x).mod(P);
        BigInteger y = lambda.multiply(a.x.subtract(x)).subtract(a.y).mod(P);
        return new Punto(x, y);
    }

    private static Punto Moltiplica(Punto p, BigInteger k) {
        Punto risultato = null;
        Punto addendo = p;
        for (int i = 0; i < k.bitLength(); i++) {
            if (k.testBit(i)) {
                risultato = Somma(risultato, addendo);
            }
            addendo = Somma(addendo, addendo);
        }
        return risultato;
    }

    /** Chiave pubblica compressa (33 byte, 02/03 + x). */
    private static byte[] Comprimi(Punto p) {
        byte[] out = new byte[33];
        out[0] = (byte) (p.y.testBit(0) ? 0x03 : 0x02);
        System.arraycopy(A32Byte(p.x), 0, out, 1, 32);
        return out;
    }

    private static Punto Decomprimi(byte[] chiave) {
        if (chiave.length != 33 || (chiave[0] != 0x02 && chiave[0] != 0x03)) {
            throw new IllegalArgumentException("chiave pubblica non compressa o non valida");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(chiave, 1, 33));
        if (x.compareTo(P) >= 0) {
            throw new IllegalArgumentException("chiave pubblica fuori dalla curva");
        }
        BigInteger y2 = x.pow(3).add(SETTE).mod(P);
        BigInteger y = y2.modPow(P.add(BigInteger.ONE).shiftRight(2), P);
        if (!y.pow(2).mod(P).equals(y2)) {
            throw new IllegalArgumentException("chiave pubblica fuori dalla curva");
        }
        if (y.testBit(0) != (chiave[0] == 0x03)) {
            y = P.subtract(y);
        }
        return new Punto(x, y);
    }

    private static byte[] A32Byte(BigInteger v) {
        byte[] b = v.toByteArray();
        if (b.length == 32) {
            return b;
        }
        byte[] out = new byte[32];
        int da = Math.max(0, b.length - 32);
        System.arraycopy(b, da, out, 32 - (b.length - da), b.length - da);
        return out;
    }

    // =================================================================================================
    // CHIAVI ESTESE (BIP32, solo parte pubblica)
    // =================================================================================================
    private static final int VERSIONE_XPUB = 0x0488B21E;
    private static final int VERSIONE_YPUB = 0x049D7CB2;
    private static final int VERSIONE_ZPUB = 0x04B24746;

    /**
     * Una chiave pubblica estesa: chiave compressa e chain code, più profondità, impronta del padre e
     * indice, che servono solo a serializzarla di nuovo.
     */
    static final class ChiaveEstesa {

        private final byte[] Chiave;
        private final byte[] ChainCode;
        private final int Profondita;
        private final byte[] ImprontaPadre;
        private final int Indice;

        private ChiaveEstesa(byte[] Chiave, byte[] ChainCode, int Profondita, byte[] ImprontaPadre, int Indice) {
            this.Chiave = Chiave;
            this.ChainCode = ChainCode;
            this.Profondita = Profondita;
            this.ImprontaPadre = ImprontaPadre;
            this.Indice = Indice;
        }

        /**
         * Legge una xpub, ypub o zpub di mainnet. Il prefisso cambia solo la versione: la chiave e il
         * chain code sono gli stessi, il formato dell'indirizzo lo decide chi chiama.
         *
         * @throws IllegalArgumentException se il checksum non torna, la versione non è una delle tre o la
         *         chiave non sta sulla curva (una tpub di testnet viene rifiutata, come faceva bitcoinj)
         */
        static ChiaveEstesa Leggi(String Base58) {
            byte[] b = DecodificaBase58Verificata(Base58.trim());
            if (b.length != 78) {
                throw new IllegalArgumentException("chiave estesa di lunghezza errata (" + b.length + " byte)");
            }
            ByteBuffer bb = ByteBuffer.wrap(b);
            int versione = bb.getInt();
            if (versione != VERSIONE_XPUB && versione != VERSIONE_YPUB && versione != VERSIONE_ZPUB) {
                throw new IllegalArgumentException("chiave estesa non supportata (serve una xpub, ypub o zpub di mainnet)");
            }
            int profondita = b[4] & 0xFF;
            byte[] impronta = Arrays.copyOfRange(b, 5, 9);
            int indice = ByteBuffer.wrap(b, 9, 4).getInt();
            byte[] chainCode = Arrays.copyOfRange(b, 13, 45);
            byte[] chiave = Arrays.copyOfRange(b, 45, 78);
            Decomprimi(chiave);   //controlla che sia un punto della curva
            return new ChiaveEstesa(chiave, chainCode, profondita, impronta, indice);
        }

        /**
         * Figlio non hardened {@code i} (CKDpub di BIP32).
         *
         * @throws IllegalArgumentException per un indice hardened, che da una chiave pubblica non si
         *         ricava, o nel caso (probabilità circa 2^-127) di un figlio non valido
         */
        ChiaveEstesa Figlia(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("indice hardened: non ricavabile da una chiave pubblica");
            }
            byte[] dati = ByteBuffer.allocate(37).put(Chiave).putInt(i).array();
            byte[] hmac = HmacSha512(ChainCode, dati);
            BigInteger il = new BigInteger(1, Arrays.copyOfRange(hmac, 0, 32));
            if (il.compareTo(N) >= 0) {
                throw new IllegalArgumentException("figlio " + i + " non valido");
            }
            Punto figlio = Somma(Moltiplica(G, il), Decomprimi(Chiave));
            if (figlio == null) {
                throw new IllegalArgumentException("figlio " + i + " non valido");
            }
            return new ChiaveEstesa(Comprimi(figlio), Arrays.copyOfRange(hmac, 32, 64), Profondita + 1,
                    Arrays.copyOfRange(Hash160(Chiave), 0, 4), i);
        }

        /** @return la chiave pubblica compressa, 33 byte */
        byte[] Chiave() {
            return Chiave.clone();
        }

        String Indirizzo(Formato F) {
            byte[] h = Hash160(Chiave);
            return switch (F) {
                case P2PKH ->
                    CodificaBase58Verificata(Concatena(new byte[]{0x00}, h));
                case P2SH_P2WPKH ->
                    CodificaBase58Verificata(Concatena(new byte[]{0x05}, Hash160(Concatena(new byte[]{0x00, 0x14}, h))));
                case P2WPKH ->
                    IndirizzoSegwit("bc", 0, h);
            };
        }

        /** La stessa chiave in Base58Check, con la versione di una xpub. */
        String SerializzaXpub() {
            byte[] b = ByteBuffer.allocate(78).putInt(VERSIONE_XPUB).put((byte) Profondita).put(ImprontaPadre)
                    .putInt(Indice).put(ChainCode).put(Chiave).array();
            return CodificaBase58Verificata(b);
        }
    }

    // =================================================================================================
    // BASE58
    // =================================================================================================
    private static final String ALFABETO_BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger CINQUANTOTTO = BigInteger.valueOf(58);

    static String CodificaBase58(byte[] Dati) {
        StringBuilder sb = new StringBuilder();
        BigInteger v = new BigInteger(1, Dati);
        while (v.signum() > 0) {
            BigInteger[] qr = v.divideAndRemainder(CINQUANTOTTO);
            sb.append(ALFABETO_BASE58.charAt(qr[1].intValue()));
            v = qr[0];
        }
        for (int i = 0; i < Dati.length && Dati[i] == 0; i++) {
            sb.append('1');
        }
        return sb.reverse().toString();
    }

    /** @throws IllegalArgumentException se la stringa contiene un carattere fuori dall'alfabeto Base58 */
    static byte[] DecodificaBase58(String Testo) {
        BigInteger v = BigInteger.ZERO;
        for (int i = 0; i < Testo.length(); i++) {
            int c = ALFABETO_BASE58.indexOf(Testo.charAt(i));
            if (c < 0) {
                throw new IllegalArgumentException("carattere non Base58 in posizione " + i);
            }
            v = v.multiply(CINQUANTOTTO).add(BigInteger.valueOf(c));
        }
        int zeri = 0;
        while (zeri < Testo.length() && Testo.charAt(zeri) == '1') {
            zeri++;
        }
        byte[] corpo = v.signum() == 0 ? new byte[0] : v.toByteArray();
        if (corpo.length > 0 && corpo[0] == 0) {
            corpo = Arrays.copyOfRange(corpo, 1, corpo.length);   //il byte di segno di BigInteger
        }
        return Concatena(new byte[zeri], corpo);
    }

    static String CodificaBase58Verificata(byte[] Dati) {
        return CodificaBase58(Concatena(Dati, Arrays.copyOf(Sha256(Sha256(Dati)), 4)));
    }

    /** @throws IllegalArgumentException se la stringa non è Base58 o il checksum non torna */
    static byte[] DecodificaBase58Verificata(String Testo) {
        byte[] b = DecodificaBase58(Testo);
        if (b.length < 4) {
            throw new IllegalArgumentException("Base58Check troppo corto");
        }
        byte[] dati = Arrays.copyOf(b, b.length - 4);
        if (!Arrays.equals(Arrays.copyOf(Sha256(Sha256(dati)), 4), Arrays.copyOfRange(b, b.length - 4, b.length))) {
            throw new IllegalArgumentException("checksum Base58Check errato");
        }
        return dati;
    }

    // =================================================================================================
    // BECH32 (BIP173: versione 0 di SegWit, non Bech32m)
    // =================================================================================================
    private static final String ALFABETO_BECH32 = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] GENERATORE_BECH32 = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

    static String IndirizzoSegwit(String Hrp, int Versione, byte[] Programma) {
        if (Versione != 0) {
            throw new IllegalArgumentException("solo SegWit versione 0 (Bech32)");
        }
        byte[] cinque = ConvertiBit(Programma, 8, 5);
        byte[] dati = new byte[cinque.length + 1];
        dati[0] = (byte) Versione;
        System.arraycopy(cinque, 0, dati, 1, cinque.length);
        byte[] hrp = EspandiHrp(Hrp);
        byte[] perChecksum = new byte[hrp.length + dati.length + 6];
        System.arraycopy(hrp, 0, perChecksum, 0, hrp.length);
        System.arraycopy(dati, 0, perChecksum, hrp.length, dati.length);
        int polimod = Polimod(perChecksum) ^ 1;
        StringBuilder sb = new StringBuilder(Hrp).append('1');
        for (byte d : dati) {
            sb.append(ALFABETO_BECH32.charAt(d));
        }
        for (int i = 0; i < 6; i++) {
            sb.append(ALFABETO_BECH32.charAt((polimod >>> (5 * (5 - i))) & 31));
        }
        return sb.toString();
    }

    private static int Polimod(byte[] Valori) {
        int chk = 1;
        for (byte v : Valori) {
            int alto = chk >>> 25;
            chk = ((chk & 0x1ffffff) << 5) ^ v;
            for (int i = 0; i < 5; i++) {
                if (((alto >>> i) & 1) == 1) {
                    chk ^= GENERATORE_BECH32[i];
                }
            }
        }
        return chk;
    }

    private static byte[] EspandiHrp(String Hrp) {
        byte[] out = new byte[Hrp.length() * 2 + 1];
        for (int i = 0; i < Hrp.length(); i++) {
            out[i] = (byte) (Hrp.charAt(i) >>> 5);
            out[i + Hrp.length() + 1] = (byte) (Hrp.charAt(i) & 31);
        }
        return out;
    }

    /** Raggruppa i bit da gruppi di {@code Da} a gruppi di {@code A}, con riempimento finale. */
    private static byte[] ConvertiBit(byte[] Dati, int Da, int A) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int acc = 0;
        int bit = 0;
        int max = (1 << A) - 1;
        for (byte b : Dati) {
            acc = (acc << Da) | (b & 0xFF);
            bit += Da;
            while (bit >= A) {
                bit -= A;
                out.write((acc >>> bit) & max);
            }
        }
        if (bit > 0) {
            out.write((acc << (A - bit)) & max);
        }
        return out.toByteArray();
    }

    // =================================================================================================
    // HASH
    // =================================================================================================
    /** RIPEMD-160 di SHA-256: l'impronta di una chiave o di uno script negli indirizzi. */
    static byte[] Hash160(byte[] Dati) {
        return Ripemd160(Sha256(Dati));
    }

    static byte[] Sha256(byte[] Dati) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(Dati);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] HmacSha512(byte[] Chiave, byte[] Dati) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(Chiave, "HmacSHA512"));
            return mac.doFinal(Dati);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] Concatena(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    // RIPEMD-160 (Dobbertin, Bosselaers, Preneel), dalla specifica originale.
    private static final int[] RL = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
        4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13};
    private static final int[] RR = {
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
        12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11};
    private static final int[] SL = {
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
        9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6};
    private static final int[] SR = {
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
        8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11};
    private static final int[] KL = {0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xA953FD4E};
    private static final int[] KR = {0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000};

    private static int F(int j, int x, int y, int z) {
        return switch (j / 16) {
            case 0 -> x ^ y ^ z;
            case 1 -> (x & y) | (~x & z);
            case 2 -> (x | ~y) ^ z;
            case 3 -> (x & z) | (y & ~z);
            default -> x ^ (y | ~z);
        };
    }

    static byte[] Ripemd160(byte[] Messaggio) {
        //Riempimento come MD4/MD5: 0x80, zeri, lunghezza in bit a 64 bit little endian
        int blocchi = (Messaggio.length + 8) / 64 + 1;
        byte[] m = Arrays.copyOf(Messaggio, blocchi * 64);
        m[Messaggio.length] = (byte) 0x80;
        long bit = (long) Messaggio.length * 8;
        for (int i = 0; i < 8; i++) {
            m[m.length - 8 + i] = (byte) (bit >>> (8 * i));
        }
        int h0 = 0x67452301, h1 = 0xEFCDAB89, h2 = 0x98BADCFE, h3 = 0x10325476, h4 = 0xC3D2E1F0;
        int[] x = new int[16];
        for (int b = 0; b < blocchi; b++) {
            for (int i = 0; i < 16; i++) {
                int o = b * 64 + i * 4;
                x[i] = (m[o] & 0xFF) | (m[o + 1] & 0xFF) << 8 | (m[o + 2] & 0xFF) << 16 | (m[o + 3] & 0xFF) << 24;
            }
            int al = h0, bl = h1, cl = h2, dl = h3, el = h4;
            int ar = h0, br = h1, cr = h2, dr = h3, er = h4;
            for (int j = 0; j < 80; j++) {
                int t = Integer.rotateLeft(al + F(j, bl, cl, dl) + x[RL[j]] + KL[j / 16], SL[j]) + el;
                al = el;
                el = dl;
                dl = Integer.rotateLeft(cl, 10);
                cl = bl;
                bl = t;
                t = Integer.rotateLeft(ar + F(79 - j, br, cr, dr) + x[RR[j]] + KR[j / 16], SR[j]) + er;
                ar = er;
                er = dr;
                dr = Integer.rotateLeft(cr, 10);
                cr = br;
                br = t;
            }
            int t = h1 + cl + dr;
            h1 = h2 + dl + er;
            h2 = h3 + el + ar;
            h3 = h4 + al + br;
            h4 = h0 + bl + cr;
            h0 = t;
        }
        ByteBuffer out = ByteBuffer.allocate(20).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        out.putInt(h0).putInt(h1).putInt(h2).putInt(h3).putInt(h4);
        return out.array();
    }
}
