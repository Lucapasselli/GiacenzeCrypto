package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le plusvalenze divise per aliquota ({@link Principale_PlusvalenzeAliquote}): tutto al 26% fino al 31/12/2025, dal
 * 01/01/2026 al 33% salvo la plusvalenza che nasce da un token di moneta elettronica in euro. Dati inventati.
 */
class Principale_PlusvalenzeAliquoteTest {

    private Map<String, String> emoneyPrima;
    private Map<String, String> casePrima;

    /** EURe e' in euro, USDC e' E-Money ma non in euro, EURX non e' nell'elenco. */
    private static final EmoneyToken.Elenco ELENCO =
            new EmoneyToken.Elenco(List.of("EURe", "USDC"), "2023-01-01", Set.of("eure"));

    @BeforeEach
    void preparaSezione() {
        emoneyPrima = new HashMap<>(Principale.Mappa_EMoney);
        casePrima = new HashMap<>(Principale.Mappa_EMoney_CaseSensitive);
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_EMoney.put("EURe", "2023-01-01");
        Principale.Mappa_EMoney.put("USDC", "2023-01-01");
        Principale.Mappa_EMoney.put("EURX", "2023-01-01");   //E-Money per l'utente, ma non in euro per l'elenco
    }

    @AfterEach
    void ripristinaSezione() {
        Principale.Mappa_EMoney.clear();
        Principale.Mappa_EMoney.putAll(emoneyPrima);
        Principale.Mappa_EMoney_CaseSensitive.clear();
        Principale.Mappa_EMoney_CaseSensitive.putAll(casePrima);
    }

    /** Un movimento: data come in v[1], gamba uscita e gamba entrata, valore, costo e plusvalenza. */
    private static String[] mov(String data, String uscita, String entrata, String vendita, String costo, String plus, String calcolata) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = data.replace("-", "") + "100000_TEST_001_001_SC";
        v[1] = data + " 10:00";
        v[8] = uscita;
        v[9] = uscita.isEmpty() ? "" : "Crypto";
        v[11] = entrata;
        v[12] = entrata.isEmpty() ? "" : "Crypto";
        v[15] = vendita;
        v[16] = costo;
        v[19] = plus;
        v[33] = calcolata;
        return v;
    }

    private static Principale_PlusvalenzeAliquote.Risultato calcola(String[]... movimenti) {
        return Principale_PlusvalenzeAliquote.Calcola(List.of(movimenti), ELENCO);
    }

    private static Principale_PlusvalenzeAliquote.Categoria cat(String[] v) {
        return Principale_PlusvalenzeAliquote.Categoria(v, ELENCO);
    }

    @Test
    void finoAl2025ESempreAl26ComunqueSiaIlToken() {
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.FINO_2025, cat(mov("2025-12-31", "BTC", "ETH", "100", "60", "40", "S")));
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.FINO_2025, cat(mov("2025-12-31", "USDC", "BTC", "100", "99", "1", "S")));
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.FINO_2025, cat(mov("2024-05-01", "", "EURe", "10", "0", "10", "S")));
    }

    @Test
    void dalPrimoGennaio2026LAliquotaDipendeDalLaGambaDellaPlusvalenza() {
        //vendo BTC e ricevo EURe: il reddito nasce dal BTC
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-01-01", "BTC", "EURe", "100", "60", "40", "S")));
        //cedo EURe e ricevo BTC: il reddito nasce dall'EURe
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO, cat(mov("2026-01-01", "EURe", "BTC", "100", "99.5", "0.5", "S")));
        //reward in EURe: nessuna gamba uscita, conta quella entrata
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO, cat(mov("2026-03-01", "", "EURe", "5", "0", "5", "S")));
        //reward in BTC
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-03-01", "", "BTC", "5", "0", "5", "S")));
    }

    @Test
    void ilNomeNonBastaServeSiaEMoneyDellUtenteSiaInEuroNellElenco() {
        //USDC: E-Money ma non in euro
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-03-01", "", "USDC", "5", "0", "5", "S")));
        //EURX: E-Money per l'utente ma non nell'elenco, quindi non in euro nonostante "EUR" nel simbolo
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-03-01", "", "EURX", "5", "0", "5", "S")));
        //EURe nell'elenco come in euro ma non E-Money per l'utente: e' una cripto-attivita' qualsiasi
        Principale.Mappa_EMoney.remove("EURe");
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-03-01", "", "EURe", "5", "0", "5", "S")));
    }

    @Test
    void laDecorrenzaEMoneyDellUtenteContaEMaiuscoleNo() {
        Principale.Mappa_EMoney.put("EURe", "2026-06-01");
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.ALTRE, cat(mov("2026-03-01", "", "EURe", "5", "0", "5", "S")));
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO, cat(mov("2026-07-01", "", "EURE", "5", "0", "5", "S")));
    }

    @Test
    void senzaElencoNessunTokenEInEuro() {
        Principale_PlusvalenzeAliquote.Risultato r = Principale_PlusvalenzeAliquote.Calcola(
                java.util.Collections.singletonList(mov("2026-03-01", "", "EURe", "5", "0", "5", "S")), null);
        assertFalse(r.elencoDisponibile);
        assertEquals(1, r.riga(Principale_PlusvalenzeAliquote.Categoria.ALTRE).movimenti);
        assertEquals(0, r.riga(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO).movimenti);
    }

    @Test
    void leSommeRicalcanoIRiquadriDellaTabella() {
        //come il ciclo di Principale: plusvalenza = somma dei v[19] numerici, vendite e costi solo con v[33]="S"
        String[] a = mov("2025-06-01", "BTC", "EUR", "100", "60", "40", "S");
        String[] b = mov("2026-02-01", "BTC", "ETH", "200", "250", "-50", "S");
        String[] c = mov("2026-03-01", "EURe", "BTC", "100", "99", "1", "S");
        String[] senzaPlus = mov("2026-03-02", "EUR", "BTC", "300", "300", "0.00", "N");    //acquisto: non conta
        String[] nonNumerica = mov("2026-03-03", "BTC", "ETH", "1000", "1000", "n.d.", "N");

        Principale_PlusvalenzeAliquote.Risultato r = calcola(a, b, c, senzaPlus, nonNumerica);

        Principale_PlusvalenzeAliquote.Riga r26 = r.riga(Principale_PlusvalenzeAliquote.Categoria.FINO_2025);
        assertEquals(1, r26.movimenti);
        assertEquals(0, new BigDecimal("100").compareTo(r26.vendite));
        assertEquals(0, new BigDecimal("60").compareTo(r26.costi));
        assertEquals(0, new BigDecimal("40").compareTo(r26.plusvalenza));

        Principale_PlusvalenzeAliquote.Riga r33 = r.riga(Principale_PlusvalenzeAliquote.Categoria.ALTRE);
        assertEquals(1, r33.movimenti, "l'acquisto senza plusvalenza e la riga non numerica non contano");
        assertEquals(0, new BigDecimal("200").compareTo(r33.vendite));
        assertEquals(0, new BigDecimal("250").compareTo(r33.costi));
        assertEquals(0, new BigDecimal("-50").compareTo(r33.plusvalenza));

        Principale_PlusvalenzeAliquote.Riga emt = r.riga(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO);
        assertEquals(1, emt.movimenti);
        assertEquals(0, new BigDecimal("1").compareTo(emt.plusvalenza));

        //il totale e' la somma dei tre gruppi: 40 - 50 + 1
        assertEquals(3, r.totale.movimenti);
        assertEquals(0, new BigDecimal("-9").compareTo(r.totale.plusvalenza));
        assertEquals(0, new BigDecimal("400").compareTo(r.totale.vendite));
        assertEquals(0, new BigDecimal("409").compareTo(r.totale.costi));
    }

    @Test
    void perAliquotaSiSommanoGliAnniEIlGruppoEmtEuroVaCol26() {
        String[] a = mov("2025-06-01", "BTC", "EUR", "100", "60", "40", "S");
        String[] b = mov("2026-02-01", "BTC", "ETH", "200", "150", "50", "S");
        String[] c = mov("2026-03-01", "", "EURe", "5", "0", "5", "S");
        Principale_PlusvalenzeAliquote.Risultato r = calcola(a, b, c);
        assertEquals(List.of(26, 33), new ArrayList<>(r.perAliquota.keySet()));
        //26%: il 2025 e l'EMT in euro del 2026, senza distinzione per anno
        assertEquals(2, r.rigaAliquota(26).movimenti);
        assertEquals(0, new BigDecimal("45").compareTo(r.rigaAliquota(26).plusvalenza));
        assertEquals(1, r.rigaAliquota(33).movimenti);
        //i movimenti dietro ogni riga sono quelli che l'hanno determinata, e il gruppo resta quello vero
        assertEquals(List.of(a, c), r.voci(26).stream().map(Principale_PlusvalenzeAliquote.Voce::movimento).toList());
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.EMT_EURO, r.voci(26).get(1).categoria());
        assertEquals(3, r.voci(0).size(), "aliquota 0 = tutti i movimenti");
        assertTrue(Principale_PlusvalenzeAliquote.Calcola(List.of(), ELENCO).voci(33).isEmpty());
    }

    @Test
    void lImpostaESoloSuLePlusvalenzeEUnaMinusvalenzaDaZero() {
        assertEquals(0, new BigDecimal("10.40").compareTo(Principale_PlusvalenzeAliquote.Imposta(new BigDecimal("40"), 26)));
        assertEquals(0, new BigDecimal("16.50").compareTo(Principale_PlusvalenzeAliquote.Imposta(new BigDecimal("50"), 33)));
        assertEquals(0, new BigDecimal("0.33").compareTo(Principale_PlusvalenzeAliquote.Imposta(new BigDecimal("1.005"), 33)));
        assertEquals(0, BigDecimal.ZERO.compareTo(Principale_PlusvalenzeAliquote.Imposta(new BigDecimal("-50"), 33)));
        //nessuna compensazione fra aliquote: +40 al 26% e -50 al 33% pagano solo i 10,40
        Principale_PlusvalenzeAliquote.Risultato r = calcola(
                mov("2025-06-01", "BTC", "EUR", "100", "60", "40", "S"),
                mov("2026-02-01", "BTC", "ETH", "200", "250", "-50", "S"));
        assertEquals(0, new BigDecimal("10.40").compareTo(r.impostaTotale()));
    }

    @Test
    void ITokenDelDialogoSiDividonoFraInEuroENon() {
        Principale_PlusvalenzeAliquote.Risultato r = calcola(
                mov("2026-03-01", "", "EURe", "5", "0", "5", "S"),
                mov("2026-03-02", "EURe", "BTC", "100", "99", "1", "S"),
                mov("2026-03-03", "", "USDC", "7", "0", "7", "S"),
                mov("2026-03-04", "", "BTC", "9", "0", "9", "S"),
                mov("2025-03-04", "", "USDC", "3", "0", "3", "S"));
        assertEquals(Map.of("EURe", 2), r.tokenEuro);
        assertEquals(Map.of("USDC", 1), r.tokenEmtNonEuro, "USDC del 2025 non e' fra quelli dal 2026, BTC non e' E-Money");
    }

    @Test
    void unaDataNonLeggibileCadeSul26EUnaRigaCortaNonRompeNiente() {
        String[] senzaData = mov("2026-03-01", "BTC", "ETH", "100", "60", "40", "S");
        senzaData[1] = "";
        senzaData[0] = "";
        assertEquals(Principale_PlusvalenzeAliquote.Categoria.FINO_2025, cat(senzaData));
        Principale_PlusvalenzeAliquote.Risultato r = Principale_PlusvalenzeAliquote.Calcola(
                Arrays.asList(new String[] {"corta"}, null, senzaData), ELENCO);
        assertEquals(1, r.totale.movimenti);
    }

    @Test
    void iTotaliDeiRiquadriSommanoComeIlDialogoEVendiCostiSoloSeCalcolata() {
        String[] a = mov("2026-03-02", "BTC", "ETH", "100", "60", "40", "S");
        String[] b = mov("2025-03-02", "BTC", "ETH", "50", "70", "-20", "S");
        String[] senzaPlus = mov("2025-03-03", "", "BTC", "999", "888", "", "");  //vendite e costi senza "S" non contano
        Principale_PlusvalenzeAliquote.Riga t = Principale_PlusvalenzeAliquote.Totali(List.of(a, b, senzaPlus));
        assertEquals(0, new BigDecimal("20").compareTo(t.plusvalenza));
        assertEquals(0, new BigDecimal("150").compareTo(t.vendite));
        assertEquals(0, new BigDecimal("130").compareTo(t.costi));
        Principale_PlusvalenzeAliquote.Risultato r = calcola(a, b, senzaPlus);
        assertEquals(0, t.plusvalenza.compareTo(r.totale.plusvalenza));
        assertEquals(0, t.vendite.compareTo(r.totale.vendite));
        assertEquals(0, t.costi.compareTo(r.totale.costi));
    }

    @Test
    void conUnFiltroDiRigaContanoSoloLeRigheInVistaNellOrdineDellaTabella() {
        String[] a = mov("2026-03-02", "BTC", "ETH", "100", "60", "40", "S");
        String[] b = mov("2025-03-02", "BTC", "ETH", "50", "70", "-20", "S");
        String[] c = mov("2025-04-02", "BTC", "ETH", "10", "5", "5", "S");
        //il sorter ha lasciato in vista solo c e a (in quest'ordine) e un ID che non e' fra i caricati va ignorato
        List<String[]> inVista = Principale_PlusvalenzeAliquote.InOrdineDiVista(List.of(a, b, c),
                List.of(c[0], "ID_SCONOSCIUTO", a[0]));
        assertEquals(2, inVista.size());
        assertSame(c, inVista.get(0));
        assertSame(a, inVista.get(1));
        assertEquals(0, new BigDecimal("45").compareTo(Principale_PlusvalenzeAliquote.Totali(inVista).plusvalenza));
        assertEquals(0, Principale_PlusvalenzeAliquote.Totali(
                Principale_PlusvalenzeAliquote.InOrdineDiVista(List.of(a, b, c), List.of())).movimenti);
    }

    @Test
    void ilDialogoDiceSempreIlPeriodoEIFiltriOCheNonCeNeSono() {
        String senza = GUI_PlusvalenzeAliquote.TestoRigheConsiderate("2021-01-01  »  2026-12-31", List.of(), List.of());
        assertTrue(senza.contains("2021-01-01"));
        assertTrue(senza.contains("Filtri della finestra Filtri: nessuno"));
        assertTrue(senza.contains("Ricerca e filtri di colonna: nessuno"));
        String con = GUI_PlusvalenzeAliquote.TestoRigheConsiderate("2026-01-01  »  2026-12-31",
                List.of("Moneta : BTC"), List.of("Ricerca nel testo : <usdc>"));
        assertTrue(con.contains("Moneta : BTC"));
        assertTrue(con.contains("Ricerca nel testo : &lt;usdc&gt;"), "il testo dell'utente va neutralizzato");
        assertTrue(GUI_PlusvalenzeAliquote.TestoRigheConsiderate("", null, null).contains("Periodo: non impostato"));
    }
}
