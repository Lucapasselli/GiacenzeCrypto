package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Due movimenti identici (stessa data al secondo, stessa descrizione, stesso importo) nello stesso file della carta o
 * del Fiat Wallet Crypto.com sono due movimenti veri: la chiave di deduplica ne teneva uno solo e il saldo restava più
 * basso dell'importo perso (due rimborsi uguali, dati inventati). Rileggere lo stesso file non deve duplicare.
 */
class CDC_RigheIdenticheCartaFiatTest {

    @TempDir
    Path cartella;

    private static final String ADDEBITO = "2026-02-17 20:19:19,Negozio Uno,EUR,-100.0,,,EUR,-100.0,-110.0,,";
    private static final String RIMBORSO = "2026-02-18 04:59:56,Refund: Negozio Uno,EUR,100.0,,,EUR,100.0,110.0,,";

    private Path scrivi(String nome, String... righe) throws IOException {
        Path p = cartella.resolve(nome);
        Files.write(p, java.util.List.of(righe));
        return p;
    }

    private static BigDecimal somma(Map<String, String> righe) {
        BigDecimal s = BigDecimal.ZERO;
        for (String r : righe.values()) s = s.add(new BigDecimal(r.split(",")[3]));
        return s;
    }

    @Test
    void carta_dueRigheIdenticheNelloStessoFileSonoDueMovimenti() throws IOException {
        Path f = scrivi("carta.csv", ADDEBITO, ADDEBITO, ADDEBITO, RIMBORSO, RIMBORSO);
        Map<String, String> righe = CDC_FiatECardWallet.LeggiRigheCarta(f.toString());
        assertEquals(5, righe.size());
        assertEquals(0, new BigDecimal("-100.0").compareTo(somma(righe)));
    }

    @Test
    void carta_rileggereLoStessoFileNonDuplica() throws IOException {
        Path f = scrivi("carta.csv", ADDEBITO, RIMBORSO, RIMBORSO);
        Map<String, String> memoria = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        memoria.putAll(CDC_FiatECardWallet.LeggiRigheCarta(f.toString()));
        memoria.putAll(CDC_FiatECardWallet.LeggiRigheCarta(f.toString()));
        assertEquals(3, memoria.size());
    }

    @Test
    void carta_unFileCheHaSoloUnaDelleDueNonAggiungeLaSeconda() throws IOException {
        Path completo = scrivi("completo.csv", ADDEBITO, RIMBORSO, RIMBORSO);
        Path tagliato = scrivi("tagliato.csv", ADDEBITO, RIMBORSO);
        Map<String, String> memoria = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        memoria.putAll(CDC_FiatECardWallet.LeggiRigheCarta(tagliato.toString()));
        memoria.putAll(CDC_FiatECardWallet.LeggiRigheCarta(completo.toString()));
        assertEquals(3, memoria.size());
    }

    @Test
    void carta_righeDiversePerDataODescrizioneRestanoSeparate() throws IOException {
        Path f = scrivi("carta.csv", ADDEBITO, RIMBORSO,
                "2026-02-18 04:59:57,Refund: Negozio Uno,EUR,100.0,,,EUR,100.0,110.0,,");
        assertEquals(3, CDC_FiatECardWallet.LeggiRigheCarta(f.toString()).size());
    }

    @Test
    void fiatWallet_dueRigheIdenticheNelloStessoFileSonoDueMovimenti() throws IOException {
        String top = "2026-01-05 14:07:46,Top Up Card,EUR,300.0,EUR,300.0,EUR,300.0,336.8,viban_card_top_up";
        Path f = scrivi("fiat.csv", top, top);
        Map<String, String> righe = CDC_FiatECardWallet.LeggiRigheFiatWallet(f.toString());
        assertEquals(2, righe.size());
        Map<String, String> memoria = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        memoria.putAll(righe);
        memoria.putAll(CDC_FiatECardWallet.LeggiRigheFiatWallet(f.toString()));
        assertEquals(2, memoria.size());
    }
}
