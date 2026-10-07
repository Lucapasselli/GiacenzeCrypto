package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ricostruzione degli interessi Simple Earn di OKX ({@link OKX_InteressiEarn}): totale esatto per periodo,
 * nessun doppione alla seconda passata, anni chiusi tolti dopo la ripartizione, controlli sui dati incompleti.
 */
class OKX_InteressiEarnTest {

    private static OKX_InteressiEarn.Flusso flusso(String giorno, String qta) {
        long ts = FunzioniDate.ConvertiDatainLongSecondo(giorno + " 10:00:00");
        return new OKX_InteressiEarn.Flusso(ts, giorno, new BigDecimal(qta));
    }

    /** Un centesimo al giorno registrato dall'11 al 30 settembre (venti giorni). */
    private static Map<String, Map<String, BigDecimal>> registratiSettembre() {
        Map<String, BigDecimal> giorni = new TreeMap<>();
        for (LocalDate d = LocalDate.parse("2026-09-11"); !d.isAfter(LocalDate.parse("2026-09-30")); d = d.plusDays(1)) {
            giorni.put(d.toString(), new BigDecimal("0.01"));
        }
        Map<String, Map<String, BigDecimal>> r = new TreeMap<>();
        r.put("USDC", giorni);
        return r;
    }

    private static BigDecimal somma(List<String[]> righe) {
        return righe.stream().map(r -> new BigDecimal(r[6])).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Periodo aperto: saldo + riscatti - sottoscrizioni meno il registrato va tutto sui giorni scoperti. */
    @Test
    void ilTotaleDelPeriodoApertoTornaEsatto() {
        var esito = OKX_InteressiEarn.Calcola(
                Map.of("USDC", List.of(flusso("2026-09-01", "1000"))),
                Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("1000.31"), new BigDecimal("0.31"))),
                registratiSettembre(), Map.of("USDC", new BigDecimal("0.01")), "2026-10-01", "2026-01-01");

        assertTrue(esito.anomalie.isEmpty(), esito.anomalie.toString());
        assertEquals(10, esito.righe.size());                                 //dal 01/09 al 10/09
        assertEquals(0, somma(esito.righe).compareTo(new BigDecimal("0.10")));  //0.31 - 0.20 registrati - 0.01 di oggi
        for (String[] r : esito.righe) {
            assertEquals(OKX_InteressiEarn.CAUSALE, r[4]);
            assertTrue(r[14].startsWith("EARN-USDC-202609") && r[14].endsWith(OKX_InteressiEarn.SUFFISSO), r[14]);
            assertEquals(r[14], r[13]);
        }
        assertEquals(1, esito.ricostruiti.size());
        assertTrue(OKX_InteressiEarn.TestoAvviso(esito).contains("USDC"));
        assertFalse(OKX_InteressiEarn.TestoAvviso(esito).contains(";"));
    }

    /** Un giorno ricostruito non e' piu' scoperto: la seconda passata non aggiunge nulla. */
    @Test
    void laSecondaPassataNonAggiungeNulla() {
        var flussi = Map.of("USDC", List.of(flusso("2026-09-01", "1000")));
        var saldi = Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("1000.31"), new BigDecimal("0.31")));
        var registrati = registratiSettembre();
        var prima = OKX_InteressiEarn.Calcola(flussi, saldi, registrati, Map.of("USDC", new BigDecimal("0.01")), "2026-10-01", null);

        List<String[]> archivio = new ArrayList<>();
        for (String[] r : prima.righe) {
            String[] mov = new String[46];
            java.util.Arrays.fill(mov, "");
            mov[3] = "OKX";
            mov[13] = r[6];
            mov[24] = r[14];
            archivio.add(mov);
        }
        var giaRegistrati = OKX_InteressiEarn.Registrati(List.of(), archivio);
        giaRegistrati.get("USDC").putAll(registrati.get("USDC"));
        var seconda = OKX_InteressiEarn.Calcola(flussi, saldi, giaRegistrati, Map.of("USDC", new BigDecimal("0.01")), "2026-10-01", null);
        assertTrue(seconda.righe.isEmpty());
        assertTrue(seconda.vuoto());
    }

    /**
     * Gli anni chiusi si tolgono DOPO la ripartizione: la quota dei giorni 2023 resta loro e non scivola sui
     * giorni 2024, e le due parti insieme fanno il totale del periodo.
     */
    @Test
    void gliAnniChiusiSiTolgonoDopoLaRipartizione() {
        var flussi = Map.of("USDT", List.of(flusso("2023-12-22", "100"), flusso("2024-01-11", "-100.42")));
        var esito = OKX_InteressiEarn.Calcola(flussi, Map.of(), Map.of(), Map.of(), "2026-10-07", "2024-01-01");

        assertTrue(esito.anomalie.isEmpty(), esito.anomalie.toString());
        assertEquals(11, esito.righe.size());                     //dal 01/01 all'11/01/2024
        assertTrue(esito.righe.stream().allMatch(r -> r[0].startsWith("2024-")));
        assertEquals(1, esito.anniPassati.size());
        BigDecimal passati = esito.anniPassati.get(0).mancanti();
        assertEquals(10, esito.anniPassati.get(0).giorni());      //dal 22 al 31/12/2023
        assertEquals(0, passati.add(somma(esito.righe)).compareTo(new BigDecimal("0.42")));
        assertEquals(0, passati.compareTo(new BigDecimal("0.20")));   //10 giorni su 21 a capitale costante

        //Con l'opzione spenta nell'anno in corso nulla del periodo si registra
        var spenta = OKX_InteressiEarn.Calcola(flussi, Map.of(), Map.of(), Map.of(), "2026-10-07", "2026-01-01");
        assertTrue(spenta.righe.isEmpty());
        assertEquals(0, spenta.anniPassati.get(0).mancanti().compareTo(new BigDecimal("0.42")));
        assertTrue(OKX_InteressiEarn.TestoAvviso(spenta).contains("anni fino al 2025"));
    }

    /** I rendimenti appena scaricati contano come registrati, anche se non sono ancora in archivio. */
    @Test
    void iRendimentiDelloScaricamentoInCorsoContanoComeRegistrati() {
        String[] scaricato = new String[19];
        java.util.Arrays.fill(scaricato, "");
        scaricato[6] = "0.05";
        scaricato[14] = "EARN-USDC-20260905";
        var registrati = OKX_InteressiEarn.Registrati(List.<String[]>of(scaricato), List.of());
        assertEquals(0, registrati.get("USDC").get("2026-09-05").compareTo(new BigDecimal("0.05")));

        var esito = OKX_InteressiEarn.Calcola(Map.of("USDC", List.of(flusso("2026-09-01", "1000"))),
                Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("1000.10"), null)),
                registrati, Map.of(), "2026-09-07", null);
        assertTrue(esito.righe.stream().noneMatch(r -> r[0].startsWith("2026-09-05")));
        assertEquals(0, somma(esito.righe).compareTo(new BigDecimal("0.05")));
    }

    /** Dati che non tornano: non si registra nulla e si avvisa. */
    @Test
    void conDatiIncompletiNonSiRicostruisce() {
        //Cumulato OKX diverso dal calcolo
        var diverso = OKX_InteressiEarn.Calcola(Map.of("USDC", List.of(flusso("2026-09-01", "1000"))),
                Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("1000.31"), new BigDecimal("0.20"))),
                Map.of(), Map.of(), "2026-10-01", null);
        assertTrue(diverso.righe.isEmpty());
        assertEquals(1, diverso.anomalie.size());

        //Riscatto senza sottoscrizione
        var orfano = OKX_InteressiEarn.Calcola(Map.of("BTC", List.of(flusso("2026-09-01", "-0.1"))),
                Map.of(), Map.of(), Map.of(), "2026-10-01", null);
        assertTrue(orfano.righe.isEmpty());
        assertEquals(1, orfano.anomalie.size());

        //Interessi implausibili rispetto al capitale (sottoscrizione mancante)
        var implausibile = OKX_InteressiEarn.Calcola(Map.of("ETH", List.of(flusso("2026-09-01", "1"), flusso("2026-09-10", "-3"))),
                Map.of(), Map.of(), Map.of(), "2026-10-01", null);
        assertTrue(implausibile.righe.isEmpty());
        assertEquals(1, implausibile.anomalie.size());
    }

    /**
     * Riscatto e nuova sottoscrizione lo stesso giorno: il giorno appartiene a due periodi. Scoperto, riceve una
     * riga sola con le due quote; registrato, si conta una volta sola, nel periodo che comincia.
     */
    @Test
    void unGiornoFraDuePeriodiDaUnaRigaSolaEContaUnaVolta() {
        var flussi = Map.of("USDC", List.of(flusso("2026-09-01", "100"), flusso("2026-09-03", "-100.02"),
                flusso("2026-09-03", "200")));
        var saldi = Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("200.04"), new BigDecimal("0.04")));

        var scoperto = OKX_InteressiEarn.Calcola(flussi, saldi, Map.of(), Map.of(), "2026-09-06", null);
        assertTrue(scoperto.anomalie.isEmpty(), scoperto.anomalie.toString());
        assertEquals(5, scoperto.righe.size());                   //dal 01/09 al 05/09, il 03 una volta
        assertEquals(5, scoperto.righe.stream().map(r -> r[14]).distinct().count());
        assertEquals(0, somma(scoperto.righe).compareTo(new BigDecimal("0.06")));

        Map<String, Map<String, BigDecimal>> registrati = new TreeMap<>();
        registrati.put("USDC", new TreeMap<>(Map.of("2026-09-03", new BigDecimal("0.01"))));
        var coperto = OKX_InteressiEarn.Calcola(flussi, saldi, registrati, Map.of(), "2026-09-06", null);
        assertEquals(4, coperto.righe.size());
        assertEquals(0, somma(coperto.righe).compareTo(new BigDecimal("0.05")));
    }

    /** Il giorno piu' vecchio di una finestra che parte dopo il giorno richiesto e' tagliato: si scarta. */
    @Test
    void ilPrimoGiornoDiUnaFinestraTagliataSiScarta() {
        List<String[]> righe = new ArrayList<>();
        for (String g : List.of("20260907", "20260908", "20260909")) {
            String[] r = new String[19];
            java.util.Arrays.fill(r, "");
            r[5] = "USDC";
            r[6] = "0.006";
            r[14] = "EARN-USDC-" + g;
            righe.add(r);
        }
        List<String[]> copia = new ArrayList<>(righe);
        assertEquals(0, OKX_InteressiEarn.ScartaPrimoGiornoTroncato(copia,
                FunzioniDate.ConvertiDatainLongSecondo("2026-09-07 00:00:00"), java.util.Set.of("USDC")));
        assertEquals(3, copia.size());

        //Moneta che la ricostruzione non puo' trattare: la riga parziale resta
        assertEquals(0, OKX_InteressiEarn.ScartaPrimoGiornoTroncato(copia,
                FunzioniDate.ConvertiDatainLongSecondo("2026-08-20 00:00:00"), java.util.Set.of()));
        assertEquals(3, copia.size());

        assertEquals(1, OKX_InteressiEarn.ScartaPrimoGiornoTroncato(righe,
                FunzioniDate.ConvertiDatainLongSecondo("2026-08-20 00:00:00"), java.util.Set.of("USDC")));
        assertEquals(2, righe.size());
        assertEquals("EARN-USDC-20260908", righe.get(0)[14]);
    }

    /**
     * Giorni dentro la finestra appena scaricata senza interessi (BTC dal 28/09 al 07/10: record a zero) li hanno
     * avuti davvero a zero: niente quote, e il totale resta esatto sugli altri giorni scoperti.
     */
    @Test
    void iGiorniCopertiDallaFinestraSenzaInteressiNonRicevonoQuote() {
        var coperti = new java.util.HashSet<String>();
        for (LocalDate d = LocalDate.parse("2026-09-06"); d.isBefore(LocalDate.parse("2026-09-10")); d = d.plusDays(1)) coperti.add(d.toString());
        var esito = OKX_InteressiEarn.Calcola(Map.of("BTC", List.of(flusso("2026-09-01", "0.1"))),
                Map.of("BTC", new OKX_InteressiEarn.Saldo(new BigDecimal("0.10000500"), null)),
                Map.of(), Map.of(), coperti, "2026-09-10", null);
        assertEquals(5, esito.righe.size());                      //dal 01/09 al 05/09
        assertTrue(esito.righe.stream().allMatch(r -> r[0].compareTo("2026-09-06") < 0));
        assertEquals(0, somma(esito.righe).compareTo(new BigDecimal("0.000005")));
    }

    /**
     * Con il calcolo spento si usa solo lo storico ufficiale: se comincia dopo il giorno richiesto, l'avviso dice da
     * quando parte. Se copre il periodo richiesto, niente avviso.
     */
    @Test
    void conIlCalcoloSpentoLoStoricoCortoVieneSegnalato() {
        long ts = FunzioniDate.ConvertiDatainLongSecondo("2026-09-07 09:00:41");
        var json = com.google.gson.JsonParser.parseString(
                "{\"savings_lending\":[{\"ccy\":\"USDC\",\"earnings\":\"0.0002\",\"ts\":\"" + ts + "\"}]}").getAsJsonObject();
        String corto = OKX_InteressiEarn.TestoAvvisoStoricoCorto(json, FunzioniDate.ConvertiDatainLongSecondo("2026-08-18 00:00:00"));
        assertTrue(corto.contains("2026-09-07") && corto.contains("2026-08-18"), corto);
        assertFalse(corto.contains(";"));
        assertEquals("", OKX_InteressiEarn.TestoAvvisoStoricoCorto(json, FunzioniDate.ConvertiDatainLongSecondo("2026-09-07 00:00:00")));
        assertEquals("", OKX_InteressiEarn.TestoAvvisoStoricoCorto(new com.google.gson.JsonObject(), 0L));
    }

    /** Due monete ricostruite nello stesso giorno non cadono nello stesso secondo ne' nello stesso ordine. */
    @Test
    void dueMoneteNelloStessoGiornoRestanoMovimentiDistinti() {
        var esito = OKX_InteressiEarn.Calcola(
                Map.of("USDC", List.of(flusso("2026-09-01", "1000")), "BTC", List.of(flusso("2026-09-01", "0.1"))),
                Map.of("USDC", new OKX_InteressiEarn.Saldo(new BigDecimal("1000.06"), null),
                       "BTC", new OKX_InteressiEarn.Saldo(new BigDecimal("0.10000600"), null)),
                Map.of(), Map.of(), "2026-09-04", null);
        String[] usdc = esito.righe.stream().filter(r -> r[5].equals("USDC") && r[0].startsWith("2026-09-02")).findFirst().orElseThrow();
        String[] btc = esito.righe.stream().filter(r -> r[5].equals("BTC") && r[0].startsWith("2026-09-02")).findFirst().orElseThrow();
        assertNotEquals(usdc[0], btc[0]);
        assertNotEquals(usdc[13], btc[13]);
    }
}
