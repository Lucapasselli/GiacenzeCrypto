package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L'uso della soglia nel quadro T/RT ({@link Principale_RT_Soglia}): quando la riga dell'anno va a zero e quale nota
 * scrive. Gli esiti si costruiscono con {@link SogliaGiacenzaAnte2023#Calcola} su gambe e prezzi inventati: nessun
 * database, nessuna rete.
 */
class Principale_RT_SogliaTest {

    private static SogliaGiacenzaAnte2023.Gamba gamba(String giorno, String simbolo, String qta) {
        Moneta m = new Moneta();
        m.InserisciValori(simbolo, qta, "", "Crypto");
        m.Rete = "";
        return new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse(giorno), m, new BigDecimal(qta));
    }

    private static SogliaGiacenzaAnte2023.Esito esito(int anno, SogliaGiacenzaAnte2023.Gamba... gambe) {
        // BTC vale 100 euro, tutto il resto non ha prezzo
        return SogliaGiacenzaAnte2023.Calcola(anno, Arrays.asList(gambe),
                (m, i) -> m.Moneta.equals("BTC") ? new BigDecimal("100").multiply(new BigDecimal(m.Qta)).toPlainString() : null,
                d -> false);
    }

    /** Riga tabella: anno, costi, corrispettivi, plusvalenza, latente, valore di fine anno, errori. */
    private static String[] riga(String anno) {
        return new String[] {anno, "1000.50", "5200.75", "4200.25", "300", "8000", ""};
    }

    @Test
    void sottoSogliaVerificata_azzeraCostiCorrispettiviEPlus_mantieneLatenteERimanenze() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100"));
        String[] r = riga("2021");
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertEquals(Principale_RT_Soglia.Stato.NON_SUPERATA, ris.stato);
        assertTrue(ris.azzerato());
        assertEquals("0", r[1]);
        assertEquals("0", r[2]);
        assertEquals("0", r[3]);
        assertEquals("300", r[4], "il latente non dipende dalla soglia");
        assertEquals("8000", r[5], "il valore di fine anno non dipende dalla soglia");
        assertEquals("4200.25", ris.plusvalenzaCalcolata);
        assertTrue(ris.nota.contains("non superata"));
        assertTrue(ris.nota.contains("non concorrono al reddito"));
        assertTrue(ris.nota.contains("massimo 10.000,00 euro il 01/03/2021"), ris.nota);
        assertTrue(ris.nota.contains("plusvalenza 4.200,25 euro"), ris.nota);
        assertTrue(ris.nota.contains("corrispettivi 5.200,75 euro"), ris.nota);
        assertTrue(ris.nota.contains("costi 1.000,50 euro"), ris.nota);
        assertFalse(ris.nota.contains(";"), "niente punto e virgola nei testi per l'utente");
        assertEquals("Soglia 51.645,69 euro non superata\n(massimo 10.000,00 euro il 01/03/2021)\nanno azzerato", ris.notaBreve);
    }

    @Test
    void sopraSoglia_nonTocca_eDiceQuando() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-20", "BTC", "-600"));
        String[] r = riga("2021");
        String[] prima = r.clone();
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertEquals(Principale_RT_Soglia.Stato.SUPERATA, ris.stato);
        assertFalse(ris.azzerato());
        assertArrayEquals(prima, r);
        assertTrue(ris.nota.contains("dal 01/03/2021 al 09/03/2021"), ris.nota);
        assertEquals("Soglia 51.645,69 euro superata\ndal 01/03/2021 al 09/03/2021", ris.notaBreve);
        assertTrue(ris.nota.contains("concorrono al reddito"));
    }

    @Test
    void sottoSogliaMaTokenSenzaPrezzo_nonAzzera_eAvvisa() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-01", "MISTERO", "5"),
                gamba("2021-03-20", "BTC", "-100"));
        String[] r = riga("2021");
        String[] prima = r.clone();
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertEquals(Principale_RT_Soglia.Stato.NON_VERIFICABILE, ris.stato);
        assertFalse(ris.azzerato());
        assertArrayEquals(prima, r, "con prezzi mancanti la riga non si tocca");
        assertTrue(ris.nota.contains("non verificabile"));
        assertTrue(ris.nota.contains("MISTERO"));
        assertTrue(ris.nota.contains("non è stata azzerata"));
        assertEquals("Soglia 51.645,69 euro non verificabile\nmancano i prezzi di 1 token\n(pulsante Prezzi del 1° gennaio)", ris.notaBreve);
    }

    @Test
    void tokenAPrezzoDiAcquisto_siVerifica_eLaNotaLoDichiara() {
        Moneta m = new Moneta();
        m.InserisciValori("NUOVO", "10", "0xabc", "Crypto");
        m.Rete = "BSC";
        var e = SogliaGiacenzaAnte2023.Calcola(2021, List.of(
                new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse("2021-03-01"), m, new BigDecimal("10"), new BigDecimal("500")),
                new SogliaGiacenzaAnte2023.Gamba(LocalDate.parse("2021-03-20"), m, new BigDecimal("-10"))),
                (mm, i) -> null, d -> false);
        String[] r = riga("2021");
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertEquals(Principale_RT_Soglia.Stato.NON_SUPERATA, ris.stato, "con il prezzo di acquisto l'anno e' verificabile");
        assertTrue(ris.azzerato());
        assertTrue(ris.nota.contains("Un token è stato valorizzato al prezzo di acquisto"), ris.nota);
        assertTrue(ris.notaBreve.endsWith("\n1 token al prezzo di acquisto"), ris.notaBreve);
        assertFalse(ris.nota.contains(";"));
    }

    @Test
    void senzaEsito_nessunEffettoNessunaNota() {
        String[] r = riga("2023");
        String[] prima = r.clone();
        var ris = Principale_RT_Soglia.Applica(r, null);
        assertEquals(Principale_RT_Soglia.Stato.NON_APPLICABILE, ris.stato);
        assertEquals("", ris.nota);
        assertArrayEquals(prima, r);
    }

    @Test
    void annoSenzaGiacenza_azzerato_conMassimoZero() {
        var e = esito(2021);
        var ris = Principale_RT_Soglia.Applica(riga("2021"), e);
        assertEquals(Principale_RT_Soglia.Stato.NON_SUPERATA, ris.stato);
        assertTrue(ris.nota.contains("massimo 0,00 euro"), ris.nota);
    }

    @Test
    void calcolaEsiti_soloAnniFinoAl2022_eSoloConOpzioneAttiva() {
        List<Integer> chiamati = new ArrayList<>();
        var esiti = Principale_RT_Soglia.CalcolaEsiti(List.of("2021", "2022", "2023", "2025"), () -> false, true,
                a -> {
                    chiamati.add(a);
                    return esito(a);
                });
        assertEquals(List.of(2021, 2022), chiamati);
        assertEquals(List.of("2021", "2022"), List.copyOf(esiti.keySet()));

        var spenta = Principale_RT_Soglia.CalcolaEsiti(List.of("2021"), () -> false, false, a -> {
            fail("con l'opzione spenta non si calcola nulla");
            return null;
        });
        assertTrue(spenta.isEmpty());
    }

    @Test
    void calcolaEsiti_siFermaSeInterrotto() {
        List<Integer> chiamati = new ArrayList<>();
        Map<String, SogliaGiacenzaAnte2023.Esito> esiti = Principale_RT_Soglia.CalcolaEsiti(List.of("2021", "2022"),
                () -> !chiamati.isEmpty(), true, a -> {
                    chiamati.add(a);
                    return esito(a);
                });
        assertEquals(1, esiti.size());
    }

    @Test
    void laNotaStaInColonnaErrori_maNonConta_comeErrore() {
        String nota = "Soglia di 51.645,69 euro non superata (massimo 0,00 euro): <b>x</b> & y";
        String conNota = Principale_RT_Soglia.ConNota("", nota);
        assertTrue(conNota.startsWith("<html>") && conNota.endsWith("</html>"));
        assertTrue(conNota.contains("&lt;b&gt;x&lt;/b&gt; &amp; y"), "la nota è sempre testo, mai HTML dell'utente");
        assertEquals("", Principale_RT_Soglia.SenzaNota(conNota).replace("<html>", "").replace("</html>", "").trim());

        // Con un errore vero già presente: l'errore resta, la nota si toglie
        String conErrore = Principale_RT_Soglia.ConNota("<html>Movimenti senza prezzo</html>", nota);
        String ripulita = Principale_RT_Soglia.SenzaNota(conErrore).replaceAll("<[^>]*>", "").replace("<br>", " ").trim();
        assertEquals("Movimenti senza prezzo", ripulita);

        assertTrue(Principale_RT_Soglia.ConNota("", "riga uno\nriga due").contains("riga uno<br>riga due"),
                "le righe della nota diventano <br>, che la tabella conta per l'altezza");
        assertEquals("<html>x</html>", Principale_RT_Soglia.ConNota("<html>x</html>", ""),
                "senza nota la colonna non cambia");
        assertEquals("", Principale_RT_Soglia.SenzaNota(null));
    }
}
