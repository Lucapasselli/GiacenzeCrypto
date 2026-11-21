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
    void sottoSogliaVerificata_tabellaConValoriVeri_stampaAZero_mantieneLatenteERimanenze() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100"));
        String[] r = riga("2021");
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertEquals(Principale_RT_Soglia.Stato.NON_SUPERATA, ris.stato);
        assertTrue(ris.azzerato());
        assertEquals("1000.50", r[1], "la tabella mostra i valori calcolati");
        assertEquals("5200.75", r[2]);
        assertEquals("4200.25", r[3]);
        assertEquals("0", ris.valoreStampato(1, r[1]), "nel quadro RT i costi vanno a zero");
        assertEquals("0", ris.valoreStampato(2, r[2]));
        assertEquals("0", ris.valoreStampato(3, r[3]));
        assertEquals("300", ris.valoreStampato(4, r[4]), "il latente non dipende dalla soglia");
        assertEquals("8000", ris.valoreStampato(5, r[5]), "il valore di fine anno non dipende dalla soglia");
        assertEquals("2021", ris.valoreStampato(0, r[0]));
        assertTrue(Principale_RT_Soglia.RigaAzzerata(Principale_RT_Soglia.ConNota("", ris.notaBreve, true)));
        assertFalse(Principale_RT_Soglia.RigaAzzerata(Principale_RT_Soglia.ConNota("", ris.notaBreve, false)));
        assertEquals("", Principale_RT_Soglia.SenzaNota(Principale_RT_Soglia.ConNota("", ris.notaBreve, true)).replace("<html>", "").replace("</html>", ""),
                "la nota non e' un errore, anche col segno del giallo");
        assertEquals("4200.25", ris.plusvalenzaCalcolata);
        assertTrue(ris.nota.contains("non superata"));
        assertTrue(ris.nota.contains("non concorrono al reddito"));
        assertTrue(ris.nota.contains("massimo 10.000,00 euro il 01/03/2021"), ris.nota);
        assertFalse(ris.nota.contains("Valori calcolati"), "i valori stanno nei righi della tabella e dell'archivio, non nella nota");
        assertFalse(ris.nota.contains("4.200,25"), ris.nota);
        assertTrue(ris.nota.contains("la tabella e l'archivio riportano i valori calcolati"), ris.nota);
        assertFalse(ris.nota.contains(";"), "niente punto e virgola nei testi per l'utente");
        assertEquals("Soglia 51.645,69 euro non superata\n(massimo 10.000,00 euro il 01/03/2021)\nPlusvalenza non riportata in RT", ris.notaBreve);
    }

    @Test
    void sottoSoglia_conMinusvalenza_ladicituraSegueIlSegno() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100"));
        String[] r = riga("2021");
        r[3] = "-800.00";
        var ris = Principale_RT_Soglia.Applica(r, e);

        assertTrue(ris.azzerato());
        assertTrue(ris.notaBreve.contains("\nMinusvalenza non riportata in RT"), ris.notaBreve);
        assertFalse(ris.notaBreve.contains("non tassabile"), "vale per plusvalenze e minusvalenze: niente 'non tassabile'");
        assertEquals("-800.00", r[3]);
    }

    @Test
    void righeExcel_soloPerUnAnnoAzzerato_conRiferimentiNormativi() {
        var sotto = Principale_RT_Soglia.Applica(riga("2021"),
                esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100")));
        String[] righe = sotto.righeExcel();
        assertEquals(3, righe.length);
        assertTrue(righe[0].contains("valori di questo prospetto sono quelli calcolati") && righe[0].contains("riportati a zero"), righe[0]);
        assertEquals(sotto.nota, righe[1]);
        assertTrue(righe[2].contains("circolare 30/E del 27 ottobre 2023"), righe[2]);
        assertTrue(righe[2].contains("paragrafo 2.3.1"), righe[2]);
        assertTrue(righe[2].contains("art. 67, comma 1, lettera c-ter), e comma 1-ter, del TUIR"), righe[2]);
        assertTrue(righe[2].contains("non si rendono deducibili neppure le minusvalenze eventualmente realizzate"),
                "la frase della circolare va riportata com'e', senza parafrasi: " + righe[2]);
        assertTrue(righe[2].contains("art. 68, comma 5"), righe[2]);
        for (String s : righe) {
            assertFalse(s.contains(";"), "niente punto e virgola nei testi per l'utente");
        }

        var sopra = Principale_RT_Soglia.Applica(riga("2021"),
                esito(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-20", "BTC", "-600")));
        assertEquals(0, sopra.righeExcel().length);
        assertEquals(0, Principale_RT_Soglia.Applica(riga("2023"), null).righeExcel().length);
    }

    @Test
    void sottoSogliaConDerivatiNellAnno_lanotaDiceCheSonoAzzeratiConIlResto() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100"));
        var con = Principale_RT_Soglia.Applica(riga("2021"), e, true);
        assertTrue(con.azzerato());
        assertTrue(con.nota.contains("Nell'anno ci sono movimenti su derivati"), con.nota);
        assertTrue(con.nota.contains("paragrafo 2.3.1") && con.nota.contains("paragrafo 2.3.2"), con.nota);
        assertTrue(con.notaBreve.endsWith("\nDerivati dell'anno azzerati con il resto"), con.notaBreve);
        assertFalse(con.nota.contains(";"), "niente punto e virgola nei testi per l'utente");
        assertEquals(con.nota, con.righeExcel()[1], "l'Excel e l'archivio riportano la stessa nota");

        var senza = Principale_RT_Soglia.Applica(riga("2021"), e, false);
        assertFalse(senza.nota.contains("derivati"));
        assertFalse(senza.notaBreve.contains("Derivati"));
        assertEquals(senza.nota, Principale_RT_Soglia.Applica(riga("2021"), e).nota);
    }

    @Test
    void soprasogliaConDerivati_nessunAvviso_perche_nonSiAzzeraNulla() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "600"), gamba("2021-03-20", "BTC", "-600"));
        var ris = Principale_RT_Soglia.Applica(riga("2021"), e, true);
        assertFalse(ris.nota.contains("derivati"), ris.nota);
        assertFalse(ris.notaBreve.contains("Derivati"), ris.notaBreve);
    }

    @Test
    void vociDaEvidenziare_soloSeLaDichiarazioneHaLaNotaDellaSoglia() {
        var con = Principale_RT_Soglia.VociDaEvidenziare(List.of("Dichiarazione", "Plusvalenze realizzate",
                Principale_RT_Soglia.TOTALE_NOTA, "Rigo 1 - VENDITA CRYPTO"));
        assertEquals(java.util.Set.of("Plusvalenze realizzate", Principale_RT_Soglia.TOTALE_NOTA), con);
        assertTrue(Principale_RT_Soglia.VociDaEvidenziare(List.of("Dichiarazione", "Plusvalenze realizzate", "Rigo 1 - VENDITA CRYPTO")).isEmpty(),
                "un anno normale non ha nulla in giallo");
    }

    @Test
    void leNote_vannoACapoDopoOgniFrase_eHtmlLeConvertePerGliAvvisi() {
        var e = esito(2021, gamba("2021-03-01", "BTC", "100"), gamba("2021-03-20", "BTC", "-100"));
        var ris = Principale_RT_Soglia.Applica(riga("2021"), e, true);
        String[] frasi = ris.nota.split("\n");
        assertTrue(frasi.length >= 4, "soglia, quadro, valori, derivati: " + ris.nota);
        for (String f : frasi) {
            assertTrue(f.endsWith(".") || f.endsWith(".)"), "ogni riga finisce con un punto: " + f);
        }
        assertFalse(Principale_RT_Soglia.Html(ris.nota).contains("\n"));
        assertTrue(Principale_RT_Soglia.Html(ris.nota).contains(".<br>"));
        String rif = ris.righeExcel()[2];
        assertEquals(3, rif.split("\n").length, "riferimenti: circolare, precisazione sulle minusvalenze, art. 68: " + rif);
    }

    @Test
    void notaElenco_soloConLaNotaDellaSoglia_eSegueIlSegno() {
        java.util.Map<String, String> totali = new java.util.LinkedHashMap<>();
        totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "1400.00");
        assertEquals("", Principale_RT_Soglia.NotaElenco(totali), "un anno normale non ha nota");
        totali.put(Principale_RT_Soglia.TOTALE_NOTA, "testo della nota");
        assertEquals("Soglia 51.645,69 euro non superata, plusvalenza non riportata in RT", Principale_RT_Soglia.NotaElenco(totali));
        totali.put(Principale_Dichiarazioni.TOT_RT_PLUS, "-800.00");
        assertEquals("Soglia 51.645,69 euro non superata, minusvalenza non riportata in RT", Principale_RT_Soglia.NotaElenco(totali));

        Dichiarazioni.Dichiarazione d = new Dichiarazioni.Dichiarazione();
        d.Totali.putAll(totali);
        assertEquals("Soglia 51.645,69 euro non superata, minusvalenza non riportata in RT", Principale_Dichiarazioni.NotaElenco(d));
        d.Nota = "Presentata col\ncommercialista";
        assertEquals("Soglia 51.645,69 euro non superata, minusvalenza non riportata in RT - Presentata col commercialista",
                Principale_Dichiarazioni.NotaElenco(d));
        d.Totali.remove(Principale_RT_Soglia.TOTALE_NOTA);
        assertEquals("Presentata col commercialista", Principale_Dichiarazioni.NotaElenco(d), "senza soglia resta la nota dell'utente");
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
        assertEquals("1000.50", ris.valoreStampato(1, r[1]), "superata: il quadro RT riporta i valori");
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
