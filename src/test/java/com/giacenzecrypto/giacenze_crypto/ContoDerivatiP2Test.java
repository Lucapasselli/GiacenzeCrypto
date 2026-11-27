package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Il conto derivati come derivato, opzione P2 di D15 ({@link ContoDerivati}), contro P1 sugli esempi del documento delle decisioni.
 *
 * <p>Dati inventati: 1.000 USDT comprati a 0,92 euro (920), il 1/3 un perpetuo aperto con 500 USDT di margine (USDT a 0,90), il 31/5 la
 * chiusura con +300 o -300 USDT (USDT a 0,88), il 30/6 la vendita di tutti gli USDT a 0,88. Totale economico: +224 o -304 euro.</p>
 * <ul>
 *   <li>P1: profitto c-quater +264, c-sexies -40; perdita -264 e -40.</li>
 *   <li>P2 nella forma "solo margine" (profitto e perdita sullo spot, l'import di OKX fino al 21/11/2026, oggi solo i fill senza margine):
 *       profitto +254 e -30; perdita -274 e -30.</li>
 *   <li>P2 nella forma con i profitti sul conto derivati (Bybit, Binance): gli stessi totali.</li>
 * </ul>
 */
class ContoDerivatiP2Test {

    private static final String ACQUISTO = "20250201100000_OKX_001_001_AC";
    private static final String MARGINE_ESCE = "20250301100000_OKX_901_001_TI";
    private static final String MARGINE_ENTRA = "20250301100000_OKX_902_001_TI";
    private static final String RIENTRO_ESCE = "20250531100000_OKX_901_001_TI";
    private static final String RIENTRO_ENTRA = "20250531100000_OKX_902_001_TI";
    private static final String RISULTATO = "20250531100000_OKX_001_001_";
    private static final String VENDITA = "20250630100000_OKX_001_001_VC";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apre() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase());
    }

    @AfterAll
    static void chiude() throws Exception {
        ContoDerivati.P1Disponibile = false;
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void archivio() {
        MappaCryptoWallet.clear();
        Principale.Mappa_EMoney.clear();
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        DatabaseH2.Pers_Opzioni_Scrivi("PlusXWallet", "NO");
        DatabaseH2.Pers_Opzioni_Scrivi("Plusvalenze_NoPlusvalenzeCommissioni", "NO");
        ContoDerivati.P1Disponibile = true;   //P2 e' l'unico metodo valido: questi test provano anche P1
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
    }

    @AfterEach
    void rimetteP1() {
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
    }

    private static String[] movimento(String id, String data, String sottoWallet, String campo5, String monetaU, String qtaU,
            String monetaE, String qtaE, String valore, String derivato, String campo18) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "OKX";
        v[4] = sottoWallet;
        v[5] = campo5;
        v[8] = monetaU;
        v[9] = monetaU.isEmpty() ? "" : (monetaU.equals("EUR") ? "FIAT" : "Crypto");
        v[10] = qtaU;
        v[11] = monetaE;
        v[12] = monetaE.isEmpty() ? "" : (monetaE.equals("EUR") ? "FIAT" : "Crypto");
        v[13] = qtaE;
        v[15] = valore;
        v[18] = campo18;
        v[22] = "A";
        v[Derivati.CAMPO] = derivato;
        MappaCryptoWallet.put(id, v);
        return v;
    }

    /**
     * L'esempio intero. Con {@code risultatoSulConto} profitto o perdita stanno sul sotto-wallet dei derivati e rientra tutto quello che
     * c'e' (forma Bybit/Binance, e OKX dal 22/11/2026), altrimenti stanno sullo spot e rientra il margine parcheggiato (forma "solo margine").
     */
    private static void esempio(boolean profitto, boolean risultatoSulConto) {
        String spot = "Principale", conto = OKXDerivati.SOTTOWALLET;
        movimento(ACQUISTO, "2025-02-01 10:00", spot, "ACQUISTO CRYPTO", "EUR", "-920", "USDT", "1000", "920.00", "", "");
        movimento(MARGINE_ESCE, "2025-03-01 10:00", spot, "TRASFERIMENTO INTERNO", "USDT", "-500", "", "", "450.00", Derivati.MARGINE, "");
        movimento(MARGINE_ENTRA, "2025-03-01 10:00", conto, "TRASFERIMENTO INTERNO", "", "", "USDT", "500", "450.00", Derivati.MARGINE, "");
        String dove = risultatoSulConto ? conto : spot;
        if (profitto) {
            movimento(RISULTATO + "DC", "2025-05-31 10:00", dove, Derivati.CAMPO5_ACCREDITO, "", "", "USDT", "300", "264.00",
                    Derivati.PNL, Derivati.CAMPO18_ACCREDITO);
        } else {
            movimento(RISULTATO + "PC", "2025-05-31 10:00", dove, Derivati.CAMPO5_ADDEBITO, "USDT", "-300", "", "", "264.00",
                    Derivati.PNL, Derivati.CAMPO18_ADDEBITO);
        }
        //Rientro: il margine parcheggiato (OKX) o tutto il conto (Bybit/Binance)
        String rientro = !risultatoSulConto ? "500" : profitto ? "800" : "200";
        String valoreRientro = !risultatoSulConto ? "440.00" : profitto ? "704.00" : "176.00";
        movimento(RIENTRO_ESCE, "2025-05-31 10:01", conto, "TRASFERIMENTO INTERNO", "USDT", "-" + rientro, "", "", valoreRientro,
                Derivati.MARGINE, "");
        movimento(RIENTRO_ENTRA, "2025-05-31 10:01", spot, "TRASFERIMENTO INTERNO", "", "", "USDT", rientro, valoreRientro,
                Derivati.MARGINE, "");
        String venduti = profitto ? "1300" : "700";
        String incasso = profitto ? "1144.00" : "616.00";
        movimento(VENDITA, "2025-06-30 10:00", spot, "VENDITA CRYPTO", "USDT", "-" + venduti, "EUR", incasso, incasso, "", "");
    }

    /** Somma delle plusvalenze cripto (c-sexies) che contano. */
    private static BigDecimal criptoAttivita() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            if ("S".equalsIgnoreCase(v[33]) && Funzioni.isNumeric(v[19], false)) t = t.add(new BigDecimal(v[19]));
        }
        return t;
    }

    /** Somma dei redditi da derivati (c-quater). */
    private static BigDecimal derivati() {
        BigDecimal t = BigDecimal.ZERO;
        for (String[] v : MappaCryptoWallet.values()) {
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    private static void calcola(boolean p2) {
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, p2 ? "SI" : "NO");
        Calcoli_PlusvalenzeNew.AggiornaPlusvalenze();
    }

    private static void totali(String derivati, String cripto) {
        assertEquals(0, new BigDecimal(derivati).compareTo(derivati()), "derivati " + derivati());
        assertEquals(0, new BigDecimal(cripto).compareTo(criptoAttivita()), "cripto-attivita' " + criptoAttivita());
    }

    @Test
    void p1_profitto_ilMargineNonHaEffetto() {
        esempio(true, false);
        calcola(false);
        totali("264", "-40");
        assertEquals("", MappaCryptoWallet.get(MARGINE_ESCE)[Derivati.CAMPO_RISULTATO]);
        assertNotEquals("S", MappaCryptoWallet.get(MARGINE_ESCE)[33]);
    }

    @Test
    void p1_perdita() {
        esempio(false, false);
        calcola(false);
        totali("-264", "-40");
    }

    @Test
    void p2_profitto_formaOkx_ilGirocontoEUnaCessioneEIlRientroUnReddito() {
        esempio(true, false);
        calcola(true);
        totali("254", "-30");
        String[] esce = MappaCryptoWallet.get(MARGINE_ESCE);
        assertEquals("S", esce[33]);
        assertEquals(0, new BigDecimal("-10").compareTo(new BigDecimal(esce[19])), "500 USDT valgono 450 e costano 460");
        assertEquals("450.00", MappaCryptoWallet.get(MARGINE_ENTRA)[17], "entrano nella pila del conto al valore");
        String[] rientra = MappaCryptoWallet.get(RIENTRO_ESCE);
        assertEquals("-10.00", rientra[Derivati.CAMPO_RISULTATO], "valgono 440 e nella pila del conto costano 450");
        assertNotEquals("S", rientra[33]);
        assertEquals("440.00", MappaCryptoWallet.get(RIENTRO_ENTRA)[17]);
    }

    @Test
    void p2_perdita_formaOkx() {
        esempio(false, false);
        calcola(true);
        totali("-274", "-30");
    }

    @Test
    void p2_profitto_sulConto_soloDerivatiDentroLaPila() {
        esempio(true, true);
        calcola(true);
        totali("254", "-30");
        String[] pnl = MappaCryptoWallet.get(RISULTATO + "DC");
        assertEquals("264.00", pnl[Derivati.CAMPO_RISULTATO]);
        assertEquals("-10.00", MappaCryptoWallet.get(RIENTRO_ESCE)[Derivati.CAMPO_RISULTATO], "800 USDT valgono 704 e costano 450 + 264");
    }

    @Test
    void p2_perdita_sulConto_lOnereEIlCostoDeiLottiScaricati() {
        esempio(false, true);
        calcola(true);
        totali("-274", "-30");
        String[] pnl = MappaCryptoWallet.get(RISULTATO + "PC");
        assertEquals("-270.00", pnl[Derivati.CAMPO_RISULTATO], "300 USDT del margine, caricati a 0,90");
        assertNotEquals("S", pnl[33], "nessuna cessione di cripto-attivita'");
        assertEquals("-4.00", MappaCryptoWallet.get(RIENTRO_ESCE)[Derivati.CAMPO_RISULTATO], "200 USDT valgono 176 e costano 180");
    }

    /**
     * Il costo delle rimanenze di "Giacenze a data" ricostruisce le pile come il motore: con P2, a posizione aperta, 500 USDT restano
     * nello spot a 460 e 500 sono nel conto a 450 (il valore del giroconto). Dal 2026-11-27 sono <b>due righe</b>, USDT e USDT.DERIVATI,
     * ognuna col suo costo (prima una riga sola da 1.000 USDT a 910). Con P1 il conto non e' una riga a parte e i 1.000 USDT costano 920.
     */
    @Test
    void costoDelleRimanenze_seguitoLaPilaDelConto() {
        esempio(true, false);
        long aprile = FunzioniDate.ConvertiDatainLong("2025-04-01");
        String spot = Principale_GiacenzeaData.ChiaveRiga("USDT", "Crypto", "", "");
        String conto = Principale_GiacenzeaData.ChiaveRiga("USDT" + ContoDerivati.SUFFISSO_CHIAVE, "Crypto", "", "");
        calcola(true);
        Principale_GiacenzeaData.CostiCaricoRimanenze costi = Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(aprile, null);
        assertEquals("460.00", costi.CostoDelleRimanenze("Tutti", spot, "500"));
        assertEquals("450.00", costi.CostoDelleRimanenze("Tutti", conto, "500"));
        //Due righe anche nella somma delle quantita' della tabella
        java.util.Map<String, Moneta> righe = Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(), aprile, "Tutti", "Tutti", true);
        assertEquals(0, new BigDecimal("500").compareTo(new BigDecimal(righe.get(spot).Qta)), "spot");
        assertEquals(0, new BigDecimal("500").compareTo(new BigDecimal(righe.get(conto).Qta)), "conto");
        assertEquals("USDT" + ContoDerivati.SUFFISSO_CHIAVE, righe.get(conto).Moneta);
        //Chi non separa (Costi netti, confronto con i nodi) vede ancora un token solo
        assertEquals(0, new BigDecimal("1000").compareTo(new BigDecimal(
                Principale_GiacenzeaData.SommaQuantitaAData(MappaCryptoWallet.values(), aprile, "Tutti", "Tutti").get(spot).Qta)));
        //Dopo la chiusura: 500 USDT a 460, 500 rientrati a 440, 300 di profitto a 264, tutto sullo spot e il conto vuoto
        long giugno = FunzioniDate.ConvertiDatainLong("2025-06-15");
        costi = Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(giugno, null);
        assertEquals("1164.00", costi.CostoDelleRimanenze("Tutti", spot, "1300"));
        assertEquals("0.00", costi.CostoDelleRimanenze("Tutti", conto, "0"));
        calcola(false);
        costi = Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(aprile, null);
        assertEquals("920.00", costi.CostoDelleRimanenze("Tutti", spot, "1000"));
        assertEquals("1184.00", Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(giugno, null).CostoDelleRimanenze("Tutti", spot, "1300"));
    }

    private static String[][] girocontoMinuscolo(String prezzo) {
        String spot = "Principale", conto = OKXDerivati.SOTTOWALLET;
        movimento(ACQUISTO, "2025-02-01 10:00", spot, "ACQUISTO CRYPTO", "EUR", "-920", "USDT", "1000", "920.00", "", "");
        //0,0009 USDT valgono meno di un centesimo: v[15] (due decimali) e' "0.00", come per il deficit di una liquidazione
        String[] esce = movimento(MARGINE_ESCE, "2025-03-01 10:00", spot, "TRASFERIMENTO INTERNO", "USDT", "-0.0009", "", "", "0.00", Derivati.MARGINE, "");
        String[] entra = movimento(MARGINE_ENTRA, "2025-03-01 10:00", conto, "TRASFERIMENTO INTERNO", "", "", "USDT", "0.0009", "0.00", Derivati.MARGINE, "");
        if (prezzo != null) {
            esce[40] = "USDT|1740823200000|" + prezzo + "|binance";
            entra[40] = esce[40];
        }
        return new String[][]{esce, entra};
    }

    /** Un giroconto di frazioni di centesimo con il suo prezzo non e' un errore dati: il valore arrotondato e' zero ma il prezzo c'e'. */
    @Test
    void unGirocontoDiFrazioniDiCentesimoConPrezzoNonEUnErroreDati() {
        String[][] g = girocontoMinuscolo("0.92");
        calcola(true);
        assertFalse(g[0][38].contains("E"), "uscita: " + g[0][38]);
        assertFalse(g[1][38].contains("E"), "entrata: " + g[1][38]);
    }

    /** Senza il prezzo il valore zero resta una segnalazione: il movimento non e' valorizzabile. */
    @Test
    void unGirocontoSenzaValoreNeAPrezzoEUnErroreDati() {
        String[][] g = girocontoMinuscolo(null);
        calcola(true);
        assertTrue(g[0][38].contains("E"), "uscita: " + g[0][38]);
        assertTrue(g[1][38].contains("E"), "entrata: " + g[1][38]);
    }

    /**
     * Il dettaglio dei movimenti di una riga ha le stesse pile della tabella: l'ultimo costo residuo della riga spot e di quella del conto
     * (col suffisso) coincide con quello della tabella principale, ognuna sui propri movimenti.
     */
    @Test
    void dettaglioDelleDueRighe_coincideConLaTabellaPrincipale() {
        esempio(true, false);
        calcola(true);
        long aprile = FunzioniDate.ConvertiDatainLong("2025-04-01");
        Principale_GiacenzeaData.CostiDettaglioToken spot = new Principale_GiacenzeaData.CostiDettaglioToken("Tutti", "USDT", "Crypto", "", "");
        Principale_GiacenzeaData.CostiDettaglioToken conto = new Principale_GiacenzeaData.CostiDettaglioToken("Tutti",
                "USDT" + ContoDerivati.SUFFISSO_CHIAVE, "Crypto", "", "");
        for (String[] v : MappaCryptoWallet.values()) {
            if (FunzioniDate.ConvertiDatainLong(v[1]) >= aprile) continue;
            spot.Avanza(v);
            conto.Avanza(v);
        }
        assertEquals("460.00", spot.CostoResiduo("500"));
        assertEquals("450.00", conto.CostoResiduo("500"));
        //Il dettaglio di un movimento: ogni gamba del giroconto e' nella sua riga
        java.util.List<Principale_GiacenzeaData.GiacenzeMoneta> usciti = Principale_GiacenzeaData.GiacenzeAttornoAlMovimento(MARGINE_ESCE);
        java.util.List<Principale_GiacenzeaData.GiacenzeMoneta> entrati = Principale_GiacenzeaData.GiacenzeAttornoAlMovimento(MARGINE_ENTRA);
        assertEquals("USDT", usciti.get(0).Moneta, "la gamba in uscita e' dello spot");
        assertEquals("USDT" + ContoDerivati.SUFFISSO_CHIAVE, entrati.get(0).Moneta, "la gamba in entrata e' del conto");
        assertEquals(0, new BigDecimal("500").compareTo(entrati.get(0).QtaDopo[Principale_GiacenzeaData.LIVELLO_GLOBALE]),
                "il conto aveva zero e ha 500, non 1.500");
    }

    /**
     * Rigo codice 9 con P2: il conto dal primo ingresso (1/3, 500 USDT a 0,90) al giorno in cui si svuota (31/5, a 0,88), 92 giorni. Con
     * P1 e senza le posizioni lo stesso conto da' colonna 7 = conto all'ingresso, colonna 8 zero (stampata come 1), mai solo monitoraggio. Stato a fiscalita' privilegiata: IVAFE al 4 per mille.
     */
    @Test
    void ilRigoCodice9_conP2ValeIlConto_conP1SenzaPosizioniColonna8Zero() {
        esempio(true, false);
        calcola(true);
        Calcoli_RW_Derivati.Prezzo prezzo = (m, ist) -> "USDT".equals(m)
                ? (ist < FunzioniDate.ConvertiDatainLongMinuto("2025-04-01 00:00") ? new BigDecimal("0.90") : new BigDecimal("0.88")) : null;
        Calcoli_RW_Derivati.Rigo p2 = Calcoli_RW_Derivati.Calcola("2025", MappaCryptoWallet, true, prezzo, (g, d) -> "189")
                .values().iterator().next();
        assertEquals(0, new BigDecimal("450").compareTo(p2.valoreIniziale));
        assertEquals(0, new BigDecimal("440").compareTo(p2.valoreFinale), "valore appena prima che il conto si svuoti");
        assertEquals(92, p2.giorni);
        assertEquals("189", p2.stato);
        assertEquals(0, new BigDecimal("0.44").compareTo(p2.ivafe), "440 x 0,4% x 92 / 365");
        assertEquals("2025-05-31", p2.prospetto.get(0)[Calcoli_RW_Derivati.P_DATA_FINE]);

        Calcoli_RW_Derivati.Rigo p1 = Calcoli_RW_Derivati.Calcola("2025", MappaCryptoWallet, false, prezzo, (g, d) -> "189")
                .values().iterator().next();
        assertEquals(0, BigDecimal.ZERO.compareTo(p1.valoreFinale));
        assertEquals("1", Calcoli_RW_Derivati.ValoreStampa(p1.valoreFinale), "lo zero si stampa come 1");
        assertEquals(0, new BigDecimal("450").compareTo(p1.valoreIniziale));
        assertEquals(0, BigDecimal.ZERO.compareTo(p1.ivafe));
        assertEquals(92, p1.giorni, "giorni compilati come per gli altri righi");
        assertTrue(p1.prospetto.get(0)[Calcoli_RW_Derivati.P_VOCE].contains("posizioni non disponibili"));
        assertTrue(Calcoli_RW_Derivati.Calcola("2026", MappaCryptoWallet, true, prezzo, (g, d) -> "").isEmpty(),
                "nessun rigo in un anno senza conto");
    }

    /**
     * Rigo codice 9 con P2 e due contratti nell'anno, ciascuno aperto e chiuso nello stesso mese, il conto vuoto in mezzo (USDT a 1 euro):
     * marzo 1.000 di margine, +50 e -2 di commissione, rientro 1.048; luglio 2.000 di margine, -100 e -4, rientro 1.896. Due tratti
     * sommati: colonna 7 3.000, colonna 8 2.944, giorni 31 (media pesata sui valori finali), IVAFE 1.048 x 0,2% x 31/365 + 1.896 x 0,2% x
     * 31/365 = 0,50. Prima dei tratti: una voce dal 1/3 al 31/7, 1.000 / 1.896, 153 giorni, IVAFE 1,59. Con P1 senza posizioni il rigo
     * resta un tratto solo, dal primo ingresso all'ultimo giorno con un saldo.
     */
    @Test
    void ilRigoCodice9_conP2_unaVocePerOgniTrattoConSaldo() {
        String spot = "Principale", conto = OKXDerivati.SOTTOWALLET;
        movimento("20250201100000_OKX_001_001_AC", "2025-02-01 10:00", spot, "ACQUISTO CRYPTO", "EUR", "-3000", "USDT", "3000", "3000.00", "", "");
        //marzo
        movimento("20250301100000_OKX_901_001_TI", "2025-03-01 10:00", spot, "TRASFERIMENTO INTERNO", "USDT", "-1000", "", "", "1000.00", Derivati.MARGINE, "");
        movimento("20250301100000_OKX_902_001_TI", "2025-03-01 10:00", conto, "TRASFERIMENTO INTERNO", "", "", "USDT", "1000", "1000.00", Derivati.MARGINE, "");
        movimento("20250331100000_OKX_801_001_DC", "2025-03-31 10:00", conto, Derivati.CAMPO5_ACCREDITO, "", "", "USDT", "50", "50.00", Derivati.PNL, Derivati.CAMPO18_ACCREDITO);
        movimento("20250331100000_OKX_802_001_CM", "2025-03-31 10:00", conto, "COMMISSIONI", "USDT", "-2", "", "", "2.00", Derivati.COMMISSIONE, "");
        movimento("20250331100000_OKX_901_001_TI", "2025-03-31 10:00", conto, "TRASFERIMENTO INTERNO", "USDT", "-1048", "", "", "1048.00", Derivati.MARGINE, "");
        movimento("20250331100000_OKX_902_001_TI", "2025-03-31 10:00", spot, "TRASFERIMENTO INTERNO", "", "", "USDT", "1048", "1048.00", Derivati.MARGINE, "");
        //luglio
        movimento("20250701100000_OKX_901_001_TI", "2025-07-01 10:00", spot, "TRASFERIMENTO INTERNO", "USDT", "-2000", "", "", "2000.00", Derivati.MARGINE, "");
        movimento("20250701100000_OKX_902_001_TI", "2025-07-01 10:00", conto, "TRASFERIMENTO INTERNO", "", "", "USDT", "2000", "2000.00", Derivati.MARGINE, "");
        movimento("20250731100000_OKX_801_001_PC", "2025-07-31 10:00", conto, Derivati.CAMPO5_ADDEBITO, "USDT", "-100", "", "", "100.00", Derivati.PNL, Derivati.CAMPO18_ADDEBITO);
        movimento("20250731100000_OKX_802_001_CM", "2025-07-31 10:00", conto, "COMMISSIONI", "USDT", "-4", "", "", "4.00", Derivati.COMMISSIONE, "");
        movimento("20250731100000_OKX_901_001_TI", "2025-07-31 10:00", conto, "TRASFERIMENTO INTERNO", "USDT", "-1896", "", "", "1896.00", Derivati.MARGINE, "");
        movimento("20250731100000_OKX_902_001_TI", "2025-07-31 10:00", spot, "TRASFERIMENTO INTERNO", "", "", "USDT", "1896", "1896.00", Derivati.MARGINE, "");
        Calcoli_RW_Derivati.Prezzo prezzo = (m, ist) -> "USDT".equals(m) ? BigDecimal.ONE : null;

        Calcoli_RW_Derivati.Rigo p2 = Calcoli_RW_Derivati.Calcola("2025", MappaCryptoWallet, true, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(2, p2.prospetto.size(), "una voce per tratto");
        String[] marzo = p2.prospetto.get(0), luglio = p2.prospetto.get(1);
        assertEquals("2025-03-01", marzo[Calcoli_RW_Derivati.P_DATA_INI]);
        assertEquals("2025-03-31", marzo[Calcoli_RW_Derivati.P_DATA_FINE]);
        assertEquals("1000.00", marzo[Calcoli_RW_Derivati.P_VAL_INI]);
        assertEquals("1048.00", marzo[Calcoli_RW_Derivati.P_VAL_FIN]);
        assertEquals("31", marzo[Calcoli_RW_Derivati.P_GIORNI]);
        assertEquals(4, marzo[Calcoli_RW_Derivati.P_ID_MOVIMENTI].split(",").length, "le quattro righe del conto di marzo");
        assertEquals("2025-07-01", luglio[Calcoli_RW_Derivati.P_DATA_INI]);
        assertEquals("2025-07-31", luglio[Calcoli_RW_Derivati.P_DATA_FINE]);
        assertEquals("2000.00", luglio[Calcoli_RW_Derivati.P_VAL_INI]);
        assertEquals("1896.00", luglio[Calcoli_RW_Derivati.P_VAL_FIN]);
        assertEquals(0, new BigDecimal("3000").compareTo(p2.valoreIniziale));
        assertEquals(0, new BigDecimal("2944").compareTo(p2.valoreFinale));
        assertEquals(31, p2.giorni, "i giorni a conto vuoto non contano");
        assertEquals(0, new BigDecimal("0.50").compareTo(p2.ivafe), "IVAFE " + p2.ivafe);

        //Opzione del periodo unico: una voce dal primo ingresso all'ultimo svuotamento, aprile-giugno compresi
        Calcoli_RW_Derivati.Rigo unico = Calcoli_RW_Derivati.Calcola("2025", MappaCryptoWallet, true, true, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(1, unico.prospetto.size());
        assertEquals("2025-03-01", unico.prospetto.get(0)[Calcoli_RW_Derivati.P_DATA_INI]);
        assertEquals("2025-07-31", unico.prospetto.get(0)[Calcoli_RW_Derivati.P_DATA_FINE]);
        assertEquals(8, unico.prospetto.get(0)[Calcoli_RW_Derivati.P_ID_MOVIMENTI].split(",").length, "le righe del conto di entrambi");
        assertEquals(0, new BigDecimal("1000").compareTo(unico.valoreIniziale));
        assertEquals(0, new BigDecimal("1896").compareTo(unico.valoreFinale));
        assertEquals(153, unico.giorni);
        assertEquals(0, new BigDecimal("1.59").compareTo(unico.ivafe), "1.896 x 0,2% x 153 / 365, IVAFE " + unico.ivafe);

        Calcoli_RW_Derivati.Rigo p1 = Calcoli_RW_Derivati.Calcola("2025", MappaCryptoWallet, false, prezzo, (g, d) -> "")
                .values().iterator().next();
        assertEquals(0, BigDecimal.ZERO.compareTo(p1.valoreFinale));
        assertEquals(1, p1.prospetto.size());
        assertEquals(0, new BigDecimal("1000").compareTo(p1.valoreIniziale), "valore al primo ingresso, come prima");
        assertEquals(153, p1.giorni);
        assertEquals("2025-07-31", p1.prospetto.get(0)[Calcoli_RW_Derivati.P_DATA_FINE]);
    }

    /**
     * Quadro W/RW con P2 (Calcoli_RW.AggiornaRWFR): il giroconto del margine chiude il rigo cripto dei 500 USDT ("Giroconto al conto
     * derivati") e il rientro ne apre uno nuovo; il conto non ha righi suoi. Con P1 nessun rigo si chiude per il margine.
     */
    @Test
    void quadroRW_conP2IlGirocontoChiudeIlRigoCripto() {
        esempio(true, false);
        DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", "D");
        try {
            for (boolean p2 : new boolean[]{true, false}) {
                calcola(p2);
                assertTrue(Calcoli_RW.AggiornaRWFR("2025", null));
                java.util.List<String[]> righi = new java.util.ArrayList<>();
                for (java.util.List<String[]> l : Principale.Mappa_RW_ListeXGruppoWallet.values()) righi.addAll(l);
                long chiusi = righi.stream().filter(r -> r.length > 12 && r[12] != null && r[12].contains("Giroconto al conto derivati")).count();
                if (p2) {
                    assertEquals(1, chiusi, "il lotto dei 500 USDT si chiude al giroconto");
                    assertTrue(righi.stream().anyMatch(r -> RIENTRO_ENTRA.equals(r[13])), "il rientro apre un rigo nuovo");
                } else {
                    assertEquals(0, chiusi);
                    assertTrue(righi.stream().noneMatch(r -> RIENTRO_ENTRA.equals(r[13])));
                }
            }
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi("RW_Rilevanza", "A");
        }
    }

    /** Con P2 il conto derivati non e' fra le giacenze cripto del quadro W/RW: a posizione aperta mancano i 500 USDT del conto. */
    @Test
    void conP2IlContoNonEFraLeGiacenzeCripto() {
        esempio(true, false);
        DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "SI");
        try {
            java.util.Map<String, java.util.List<String[]>> g = Calcoli_RW_Giacenze.GiacenzePerTratti("2025",
                    gr -> Calcoli_RW_PeriodiCrypto.trattiCrypto(gr, "2025"), (m, ist) -> "1");
            for (java.util.List<String[]> righe : g.values()) {
                for (String[] r : righe) assertNotEquals(OKXDerivati.SOTTOWALLET, r[1]);
            }
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi(ContoDerivati.OPZIONE, "NO");
        }
    }

    /** L'opzione e' fra gli ingressi del motore (Epoca): cambiarla rifa' tutto lo storico, anche con il ricalcolo incrementale. */
    @Test
    void cambiareOpzioneRicalcolaTutto() {
        esempio(true, false);
        calcola(true);
        totali("254", "-30");
        calcola(false);
        totali("264", "-40");
        calcola(true);
        totali("254", "-30");
    }

    /**
     * Spostare un movimento sul sotto-wallet dei derivati (<i>Marca come derivato</i> cambia solo {@code [4]}) con P2 attiva deve
     * sporcare l'impronta: la passata incrementale riparte da li' e da' gli stessi numeri della passata completa. Prima della
     * correzione l'impronta non conteneva {@code [4]} e il profitto restava nella pila dello spot.
     */
    @Test
    void spostareSulContoConP2_incrementaleUgualeACompleto() {
        DatabaseH2.Pers_Opzioni_Scrivi(Calcoli_PlusvalenzeNew.OPZIONE_INCREMENTALE, "SI");
        esempio(true, true);
        String[] pnl = MappaCryptoWallet.get(RISULTATO + "DC");
        pnl[4] = "Principale";              //importato prima della 1.0.67, sul wallet principale
        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        //Le prime passate popolano le tabelle dell'epoca (gruppi wallet letti pigramente): si ripete finche' una passata non trova
        //nulla da rifare, da li' la prossima e' incrementale
        for (int i = 0; i < 4 && (i == 0 || Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati > 0); i++) calcola(true);
        assertEquals(0,Calcoli_PlusvalenzeNew.UltimaPassata_Elaborati, "senza modifiche non si rielabora nulla");
        String prima = MappaCryptoWallet.get(RIENTRO_ESCE)[Derivati.CAMPO_RISULTATO];

        pnl[4] = OKXDerivati.SOTTOWALLET;   //lo spostamento, come Principale_DerivatiMarcatura.Applica
        calcola(true);                      //incrementale
        java.util.Map<String, String> incrementale = new java.util.TreeMap<>();
        for (String[] v : MappaCryptoWallet.values()) incrementale.put(v[0], Esiti(v));
        assertNotEquals(prima, MappaCryptoWallet.get(RIENTRO_ESCE)[Derivati.CAMPO_RISULTATO], "lo spostamento cambia la pila del conto");

        Calcoli_PlusvalenzeNew.InvalidaStatoIncrementale();
        calcola(true);                      //completa
        for (String[] v : MappaCryptoWallet.values()) assertEquals(Esiti(v), incrementale.get(v[0]), v[0]);
        totali("254", "-30");
    }

    private static String Esiti(String[] v) {
        return v[16] + "|" + v[17] + "|" + v[19] + "|" + v[33] + "|" + v[38] + "|" + v[Derivati.CAMPO_RISULTATO];
    }

    /** Toglie la classificazione al profitto o alla perdita dell'esempio: resta marcato come derivato, campo 18 vuoto. */
    private static String[] daClassificare(boolean profitto) {
        String[] pnl = MappaCryptoWallet.get(RISULTATO + (profitto ? "DC" : "PC"));
        pnl[5] = profitto ? "DEPOSITO CRYPTO" : "PRELIEVO CRYPTO";
        pnl[18] = "";
        return pnl;
    }

    /**
     * Con P2 un movimento su derivati ancora da classificare che sta sul conto conta come classificato (decisione del 21/11/2026):
     * stessi totali e stesso {@code [46]} della forma classificata, e l'avviso non lo conta fra i derivati trattati come cripto.
     */
    @Test
    void p2_daClassificareSulConto_contaComeClassificato() {
        for (boolean profitto : new boolean[]{true, false}) {
            archivio();
            esempio(profitto, true);
            String[] pnl = daClassificare(profitto);
            calcola(true);
            if (profitto) {
                totali("254", "-30");
                assertEquals("264.00", pnl[Derivati.CAMPO_RISULTATO]);
                assertEquals("264.00", pnl[17], "entra nella pila del conto al valore");
            } else {
                totali("-274", "-30");
                assertEquals("-270.00", pnl[Derivati.CAMPO_RISULTATO], "onere pari al costo dei lotti scaricati");
            }
            assertNotEquals("S", pnl[33], "nessuna plusvalenza cripto");
            assertEquals(0, Derivati.Conta("2025")[2], "calcolato come derivato: non e' fra quelli trattati come cripto");
        }
    }

    /** Con P1, o con P2 sullo spot, il movimento da classificare resta una cripto-attivita' come prima. */
    @Test
    void daClassificare_conP1oSulloSpot_restaCripto() {
        esempio(true, true);
        String[] pnl = daClassificare(true);
        calcola(false);
        assertEquals("", pnl[Derivati.CAMPO_RISULTATO]);
        assertEquals(1, Derivati.Conta("2025")[2]);

        archivio();
        esempio(true, false);
        pnl = daClassificare(true);
        calcola(true);
        assertEquals("", pnl[Derivati.CAMPO_RISULTATO], "sullo spot (forma OKX) resta da classificare");
        assertEquals(1, Derivati.Conta("2025")[2]);
    }

    /**
     * Con i movimenti non classificati fuori dal calcolo un deposito da classificare sul conto resta fuori come ogni altro, una
     * commissione da classificare conta sempre (come ogni commissione).
     */
    @Test
    void daClassificare_senzaMovimentiNonClassificati_soloLeCommissioni() {
        DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "NO");
        try {
            esempio(true, true);
            String[] pnl = daClassificare(true);
            String[] fee = movimento("20250415100000_OKX_C01_001_CM", "2025-04-15 10:00", OKXDerivati.SOTTOWALLET, "COMMISSIONI",
                    "USDT", "-10", "", "", "8.90", Derivati.COMMISSIONE, "");
            calcola(true);
            assertEquals("", pnl[Derivati.CAMPO_RISULTATO]);
            assertEquals(1, Derivati.Conta("2025")[2], "il deposito non e' calcolato come derivato");
            assertEquals("-9.00", fee[Derivati.CAMPO_RISULTATO], "10 USDT del margine, caricati a 0,90");
            assertNotEquals("S", fee[33]);
        } finally {
            DatabaseH2.Pers_Opzioni_Scrivi("PL_CosiderareMovimentiNC", "SI");
        }
    }

    /** Con P1 il sotto-wallet sta nella pila del gruppo: nessuna pila in piu' nella mappa del motore. */
    @Test
    void p1_nessunaPilaDelConto() {
        esempio(true, false);
        calcola(false);
        assertTrue(MappaCryptoWallet.values().stream().noneMatch(v -> v[38] != null && v[38].contains("A")),
                "nessun movimento con la pila mancante");
    }
}
