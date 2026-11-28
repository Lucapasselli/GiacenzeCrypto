package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Il riepilogo per anno della scheda Dashboard ({@link Principale_Dashboard}): somma i campi già calcolati dal motore con le
 * regole dei tre riquadri della tabella movimenti, tiene i derivati a parte e non perde righe. Dati inventati.
 */
class Principale_DashboardTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    /** Una riga: data, exchange, wallet, vendita, costo, plusvalenza e flag "con effetto sul riepilogo" ({@code v[33]}). */
    private static String[] riga(String data, String exchange, String wallet, String vendita, String costo, String plus, String flag) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = data.replace("-", "") + "000000_" + exchange + "_" + wallet + "_1_TR";
        v[1] = data;
        v[3] = exchange;
        v[4] = wallet;
        v[15] = vendita;
        v[16] = costo;
        v[19] = plus;
        v[33] = flag;
        return v;
    }

    private static List<String[]> registro() {
        List<String[]> r = new ArrayList<>();
        r.add(riga("2024-03-01 10:00", "Binance", "Principale", "100", "60", "40", "S"));
        r.add(riga("2024-05-01 10:00", "Binance", "Principale", "50", "80", "-30", "S"));
        // Un reddito (reward): ha una plusvalenza ma non entra fra costi e corrispettivi
        r.add(riga("2024-06-01 10:00", "Ledger", "", "7", "0", "5", ""));
        r.add(riga("2025-01-10 10:00", "Binance", "Principale", "200", "150", "50", "S"));
        return r;
    }

    @Test
    void ilRiepilogoDiUnAnnoEQuelloDeiRiquadriDellaTabella() {
        Principale_Dashboard.Riepilogo r = Principale_Dashboard.Calcola(registro(), Map.of());
        Principale_Dashboard.RigaAnno a2024 = r.anno(2024);
        Principale_PlusvalenzeAliquote.Riga riquadri = Principale_PlusvalenzeAliquote.Totali(
                registro().stream().filter(v -> v[1].startsWith("2024")).toList());
        assertEquals(riquadri.costi, a2024.costi());
        assertEquals(riquadri.vendite, a2024.corrispettivi());
        assertEquals(riquadri.plusvalenza, a2024.netto());
        assertEquals(3, a2024.movimenti());
    }

    @Test
    void plusvalenzeEMinusvalenzeSonoLaParteDiOgniRigaNonIlSegnoDelTotale() {
        Principale_Dashboard.RigaAnno a = Principale_Dashboard.Calcola(registro(), Map.of()).anno(2024);
        assertEquals(0, bd("45").compareTo(a.plusvalenze()), "40 + 5");
        assertEquals(0, bd("-30").compareTo(a.minusvalenze()));
        assertEquals(0, bd("15").compareTo(a.netto()));
        // Il reddito senza effetto sul riepilogo non porta costi né corrispettivi
        assertEquals(0, bd("150").compareTo(a.corrispettivi()));
        assertEquals(0, bd("140").compareTo(a.costi()));
    }

    @Test
    void ilTotaleStoricoEIlTotaleDelleRigheAnnuali() {
        Principale_Dashboard.Riepilogo r = Principale_Dashboard.Calcola(registro(), Map.of());
        assertEquals(2, r.anni().size());
        assertEquals(2024, r.anni().get(0).anno(), "dal più vecchio");
        BigDecimal netti = r.anni().stream().map(Principale_Dashboard.RigaAnno::netto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, netti.compareTo(r.totale().netto()));
        assertEquals(r.anni().stream().mapToInt(Principale_Dashboard.RigaAnno::movimenti).sum(), r.totale().movimenti());
        assertEquals(0, r.totale().anno());
    }

    @Test
    void unaRigaSenzaDataNonEntraInNessunAnnoMaSiConta() {
        List<String[]> r = registro();
        r.add(riga("", "Binance", "Principale", "10", "5", "5", "S"));
        r.add(riga("data guasta", "Binance", "Principale", "10", "5", "5", "S"));
        Principale_Dashboard.Riepilogo ri = Principale_Dashboard.Calcola(r, Map.of());
        assertEquals(2, ri.senzaData());
        assertEquals(registro().size(), ri.totale().movimenti(), "il totale resta la somma degli anni");
    }

    @Test
    void iDerivatiStannoAPartePerAnnoENonEntranoNellaPlusvalenza() {
        List<String[]> r = registro();
        String[] d = riga("2025-02-01 10:00", "OKX", "Derivati", "", "", "", "");
        d[46] = "12.50";
        r.add(d);
        Principale_Dashboard.Riepilogo ri = Principale_Dashboard.Calcola(r, Map.of());
        assertNull(ri.anno(2024).derivati(), "un anno senza derivati non mostra zero");
        assertEquals(0, bd("12.50").compareTo(ri.anno(2025).derivati()));
        assertEquals(0, bd("50").compareTo(ri.anno(2025).netto()), "[46] non è in v[19]");
        assertEquals(0, bd("12.50").compareTo(ri.totale().derivati()));
    }

    @Test
    void walletEDocumentiSonoDistintiPerAnno() {
        List<String[]> r = registro();
        r.get(0)[41] = "3|5,6";
        r.get(1)[41] = "3";
        r.get(3)[41] = "9";
        Principale_Dashboard.Riepilogo ri = Principale_Dashboard.Calcola(r, Map.of());
        assertEquals(2, ri.anno(2024).wallet(), "Binance / Principale e Ledger");
        assertEquals(3, ri.anno(2024).documenti(), "origine 3 più i supporti 5 e 6, il 3 una volta sola");
        assertEquals(1, ri.anno(2025).documenti());
        assertEquals(4, ri.totale().documenti());
    }

    @Test
    void ledichiarazioniDichiaranoLoStatoMigliorePerFamiglia() {
        List<Dichiarazioni.Dichiarazione> elenco = new ArrayList<>();
        elenco.add(dich(Dichiarazioni.Famiglia.RW, "2024", false));
        elenco.add(dich(Dichiarazioni.Famiglia.RW, "2024", true));
        elenco.add(dich(Dichiarazioni.Famiglia.RT, "2024", false));
        elenco.add(dich(Dichiarazioni.Famiglia.RT, "2025", false));
        elenco.add(dich(Dichiarazioni.Famiglia.RT, "", true));
        Map<Integer, String> t = Principale_Dashboard.DichiarazioniPerAnno(elenco);
        assertEquals("RT archiviata · RW definitiva", t.get(2024));
        assertEquals("RT archiviata", t.get(2025));
        assertEquals(2, t.size(), "una dichiarazione senza anno non si attribuisce a nessuno");
        Principale_Dashboard.RigaAnno a = Principale_Dashboard.Calcola(registro(), t).anno(2024);
        assertNotNull(a);
        assertEquals("RT archiviata · RW definitiva", a.dichiarazioni());
    }

    @Test
    void gliImportiSiScrivonoComeNellaScheda() {
        assertEquals("€ 1.234,50", Principale_Dashboard.Euro(bd("1234.5")));
        assertEquals("-€ 1.234,50", Principale_Dashboard.Euro(bd("-1234.5")));
        assertEquals("+€ 40,00", Principale_Dashboard.EuroConSegno(bd("40")));
        assertEquals("-€ 30,00", Principale_Dashboard.EuroConSegno(bd("-30")));
        assertEquals("€ 0,00", Principale_Dashboard.EuroConSegno(BigDecimal.ZERO));
        assertEquals("-", Principale_Dashboard.Euro(null));
    }

    /** Un movimento del tipo dato (DC, PC...) con i campi che contano per gli errori. */
    private static String[] conTipo(String data, String tipo) {
        String[] v = riga(data, "Binance", "Principale", "10", "5", "5", "S");
        v[0] = data.replace("-", "").replace(" ", "").replace(":", "") + "00_Binance_Principale_1_" + tipo;
        v[32] = "SI";
        return v;
    }

    @Test
    void gliErroriDiUnAnnoSonoQuelliCheStannoNelleRighe() {
        List<String[]> r = new ArrayList<>();
        String[] nonClassificato = conTipo("2024-03-01 10:00", "DC");
        nonClassificato[11] = "BTC";
        nonClassificato[13] = "1";
        r.add(nonClassificato);
        String[] classificato = conTipo("2024-03-02 10:00", "PC");
        classificato[8] = "BTC";
        classificato[10] = "-1";
        classificato[18] = "PWN - PRELIEVO";
        r.add(classificato);
        String[] scam = conTipo("2024-03-03 10:00", "DC");
        scam[11] = "FAKE **";
        scam[13] = "1";
        r.add(scam);
        String[] automatico = conTipo("2024-03-04 10:00", "DC");
        automatico[11] = "BTC";
        automatico[13] = "1";
        automatico[22] = "AU";
        r.add(automatico);
        String[] senzaPrezzo = conTipo("2024-04-01 10:00", "TR");
        senzaPrezzo[32] = "NO";
        senzaPrezzo[15] = "0.00";
        r.add(senzaPrezzo);
        String[] lifo = conTipo("2025-01-01 10:00", "VC");
        lifo[38] = "AB";
        r.add(lifo);
        Principale_Dashboard.Riepilogo ri = Principale_Dashboard.Calcola(r, Map.of());
        Principale_Dashboard.ErroriAnno e24 = ri.anno(2024).errori();
        assertEquals(1, e24.nonClassificati(), "solo il deposito senza causale, non SCAM, non automatico");
        assertEquals(1, e24.senzaPrezzo());
        assertEquals(0, e24.lifoMancante());
        assertEquals(1, ri.anno(2025).errori().lifoMancante(), "la lettera A fra le altre");
        assertEquals(0, ri.anno(2025).errori().nonClassificati());
        // La somma degli anni è il totale: con righe tutte datate è anche quello che conta il pulsante
        int somma = ri.anni().stream().mapToInt(a -> a.errori().totale()).sum();
        assertEquals(somma, ri.totale().errori().totale());
        assertEquals(3, somma);
    }

    @Test
    void ilConteggioSenzaPrezzoDiCiOCheDiceLaFunzioneOriginale() {
        // I casi in cui la funzione non cerca niente in rete: il campo 32 pieno, oppure vuoto con un valore
        String[][] casi = {{"SI", "5"}, {"Si", "5"}, {"SI", "0.00"}, {"NO", "5"}, {"NO", "0.00"}, {"", "5"}, {"X", "5"}, {"X", "0.00"}};
        for (String[] c : casi) {
            for (String scam : new String[] {"BTC", "FAKE **"}) {
                String[] v = riga("2024-03-01 10:00", "Binance", "Principale", c[1], "", "", "");
                v[32] = c[0];
                v[11] = scam;
                boolean attesa = !Prezzi.isMovimentoPrezzato(v.clone());
                assertEquals(attesa, Principale_Errori.SenzaPrezzo(v), "campo 32 '" + c[0] + "', valore " + c[1] + ", moneta " + scam);
            }
        }
    }

    @Test
    void ilValoreDellAnnoSommaInizioEFineEContaIPrezziMancanti() {
        Map<String, List<String[]>> righe = new java.util.TreeMap<>();
        List<String[]> g1 = new ArrayList<>();
        g1.add(rigaGiacenza("BTC", "100.50", "250.25", ""));
        g1.add(rigaGiacenza("ETH", null, "10", ""));
        g1.add(rigaGiacenza("DOGE", "0.0000", "0.0000", Calcoli_RW_Giacenze.ERRORE_GIACENZA_NEGATIVA));
        righe.put("Wallet 1", g1);
        righe.put("Wallet 2", List.of(rigaGiacenza("BTC", "1", null, ""), rigaGiacenza("SOL", "2", "3", ""),
                rigaGiacenza("FAKE **", null, null, "")));
        Principale_Dashboard.ValoreAnno v = Principale_Dashboard.ValoreDaRighe(righe, true, "2024-12-31");
        assertEquals(0, bd("103.50").compareTo(v.inizio()), "100.50 + 0 + 0 + 1 + 2, il prezzo mancante non è zero ma non si somma");
        assertEquals(0, bd("263.25").compareTo(v.fine()), "250.25 + 10 + 0 + 3");
        assertEquals(2, v.senzaPrezzo(), "ETH all'inizio e BTC alla fine del secondo gruppo, ognuno una volta, e il token SCAM non conta");
        assertEquals(1, v.negative(), "lo zero con giacenza negativa non è un prezzo mancante");
        Principale_Dashboard.ValoreAnno soloInizio = Principale_Dashboard.ValoreDaRighe(righe, false, null);
        assertNull(soloInizio.fine());
        assertEquals(1, soloInizio.senzaPrezzo(), "solo ETH: la fine non si guarda");
    }

    private static String[] rigaGiacenza(String moneta, String inizio, String fine, String errore) {
        String[] r = new String[17];
        Arrays.fill(r, "");
        r[2] = moneta;
        r[7] = moneta;
        r[5] = inizio;
        r[10] = fine;
        r[15] = errore;
        return r;
    }

    @Test
    void ilFineAnnoDellAnnoInCorsoEIeri() {
        LocalDate oggi = LocalDate.of(2026, 11, 27);
        assertEquals("2025-12-31", Principale_Dashboard.FineValutazione(2025, oggi));
        assertEquals("2026-11-26", Principale_Dashboard.FineValutazione(2026, oggi));
        assertNull(Principale_Dashboard.FineValutazione(2027, oggi), "un anno futuro non si valuta");
        assertNull(Principale_Dashboard.FineValutazione(2026, LocalDate.of(2026, 1, 1)), "il 1° gennaio non c'è ancora un ieri dell'anno");
        assertTrue(Principale_Dashboard.FineValutazione(2026, LocalDate.of(2026, 1, 2)).equals("2026-01-01"));
    }

    private static Dichiarazioni.Dichiarazione dich(Dichiarazioni.Famiglia fam, String anno, boolean definitiva) {
        Dichiarazioni.Dichiarazione d = new Dichiarazioni.Dichiarazione();
        d.Fam = fam;
        d.Anno = anno;
        d.Definitiva = definitiva;
        return d;
    }
}
