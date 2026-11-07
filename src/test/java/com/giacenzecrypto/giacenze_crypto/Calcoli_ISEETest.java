package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I rapporti del Quadro FC2 ({@link Calcoli_ISEE}): valore al 31/12 dalla fotografia del quadro W/RW, un rapporto
 * per operatore, liquidità nel 99 o in un 01 a parte, date, dati dell'utente e media giornaliera. Fonti iniettate:
 * nessun accesso a rete, cache o database. Le regole sono quelle decise con l'utente il 2026-11-04
 * ({@code nocommit/Documentazione/Analisi_ISEE_DSU.md} §19-25).
 */
class Calcoli_ISEETest {

    private static int progressivo = 0;

    private final List<String[]> movimenti = new ArrayList<>();
    private final Map<String, List<String[]>> foto = new HashMap<>();
    private final Map<String, List<String[]>> periodi = new HashMap<>();
    private final Map<String, TreeMap<LocalDate, BigDecimal>> euro = new HashMap<>();
    private final Map<String, Calcoli_ISEE.DatiUtente> dati = new HashMap<>();

    @BeforeEach
    void azzera() {
        progressivo = 0;
    }

    // --- costruzione dei dati ------------------------------------------------

    /** Movimento minimale: solo i campi letti dal motore. Nel test il gruppo è il nome scritto nel campo 3. */
    private void mov(String data, String gruppo, String monetaU, String qtaU, String monetaE, String qtaE) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = data.replace("-", "").replace(" ", "").replace(":", "") + "_Test_" + (++progressivo) + "_1_DC";
        v[1] = data;
        v[3] = gruppo;
        v[8] = monetaU;
        v[9] = monetaU.isEmpty() ? "" : "Crypto";
        v[10] = qtaU;
        v[11] = monetaE;
        v[12] = monetaE.isEmpty() ? "" : "Crypto";
        v[13] = qtaE;
        movimenti.add(v);
    }

    private void deposito(String data, String gruppo, String moneta, String qta) {
        mov(data, gruppo, "", "", moneta, qta);
    }

    private void prelievo(String data, String gruppo, String moneta, String qta) {
        mov(data, gruppo, moneta, "-" + qta, "", "");
    }

    /** Un rigo della fotografia di fine anno ({@code String[17]} di {@link Calcoli_RW_Giacenze}). */
    private void fotografia(String gruppo, String moneta, String qta, String valore) {
        String[] r = new String[17];
        java.util.Arrays.fill(r, "");
        r[7] = moneta;
        r[8] = qta;
        r[10] = valore;
        foto.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(r);
    }

    /** Un periodo FIAT in forma GUI. */
    private void periodoFiat(String gruppo, int prog, String inizio, String fine, String stato, String ident,
            String contoCorrente) {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_TIPO] = Principale_GruppiWalletRW.TIPO_FIAT;
        r[Principale_GruppiWalletRW.COL_PROGRESSIVO] = String.valueOf(prog);
        r[Principale_GruppiWalletRW.COL_DATA_INIZIO] = inizio;
        r[Principale_GruppiWalletRW.COL_DATA_FINE] = fine;
        r[Principale_GruppiWalletRW.COL_STATO_ESTERO] = stato;
        r[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = ident;
        r[Principale_GruppiWalletRW.COL_E_CONTO_CORRENTE] = contoCorrente;
        periodi.computeIfAbsent(gruppo, k -> new ArrayList<>()).add(r);
    }

    /** Una variazione della liquidità in euro del gruppo. */
    private void liquidita(String gruppo, String giorno, String importo) {
        euro.computeIfAbsent(gruppo, k -> new TreeMap<>()).merge(LocalDate.parse(giorno), new BigDecimal(importo),
                BigDecimal::add);
    }

    /** Liquidità in soli euro dalle variazioni del test: stessa definizione di {@link Calcoli_RW_Fiat.Liquidita}. */
    private final Calcoli_ISEE.LiquiditaFiat liq = new Calcoli_ISEE.LiquiditaFiat() {
        @Override
        public boolean presente(String gruppo) {
            return euro.containsKey(gruppo);
        }

        @Override
        public java.util.Set<String> gruppi() {
            return euro.keySet();
        }

        @Override
        public LocalDate primo(String gruppo) {
            return euro.containsKey(gruppo) ? euro.get(gruppo).firstKey() : null;
        }

        @Override
        public LocalDate ultimoFinoA(String gruppo, LocalDate limite) {
            return euro.containsKey(gruppo) ? euro.get(gruppo).floorKey(limite) : null;
        }

        @Override
        public BigDecimal saldoEUR(String gruppo, LocalDate giorno, LocalDate dataCambio, List<String> avvisi) {
            BigDecimal s = BigDecimal.ZERO;
            for (Map.Entry<LocalDate, BigDecimal> e : euro.getOrDefault(gruppo, new TreeMap<>()).entrySet()) {
                if (!e.getKey().isAfter(giorno)) {
                    s = s.add(e.getValue());
                }
            }
            return s;
        }

        @Override
        public BigDecimal mediaAnnua(String gruppo, LocalDate da, LocalDate a, LocalDate dataCambio, List<String> avvisi) {
            BigDecimal somma = BigDecimal.ZERO;
            for (LocalDate g = da; !g.isAfter(a); g = g.plusDays(1)) {
                somma = somma.add(saldoEUR(gruppo, g, dataCambio, avvisi).max(BigDecimal.ZERO));
            }
            return somma.divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        }
    };

    private Calcoli_ISEE.Risultato calcola(int anno) {
        return calcola(anno, v -> v[3]);
    }

    private Calcoli_ISEE.Risultato calcola(int anno, Function<String[], String> gruppoDi) {
        return Calcoli_ISEE.calcola(anno, movimenti, gruppoDi, foto, g -> periodi.getOrDefault(g, List.of()), liq,
                dati::get, g -> g);
    }

    private static Calcoli_ISEE.ValoreFineGiornata prezziCostanti(String prezzo) {
        return (m, qta, g) -> qta.multiply(new BigDecimal(prezzo));
    }

    private static void assertNumero(String atteso, BigDecimal reale) {
        assertNotNull(reale, "valore assente");
        assertEquals(0, new BigDecimal(atteso).compareTo(reale), "atteso " + atteso + " ma è " + reale.toPlainString());
    }

    private static boolean haNota(Calcoli_ISEE.Rapporto r, String pezzo) {
        return r.note.stream().anyMatch(n -> n.contains(pezzo));
    }

    // --- valore al 31/12 --------------------------------------------------------

    @Test
    void valoreAl3112DallaFotografia_prezzoMancanteN_d_eSenzaPeriodiOperatoreEND() {
        deposito("2023-05-10 10:00", "Coinbase", "ETH", "1");
        deposito("2024-02-10 10:00", "Coinbase", "ETH", "1");
        deposito("2024-03-10 10:00", "Coinbase", "XYZ", "50");
        // un movimento successivo all'anno di riferimento non conta
        deposito("2025-01-05 10:00", "Coinbase", "ETH", "5");
        fotografia("Coinbase", "ETH", "2", "6000");
        fotografia("Coinbase", "XYZ", "50", null);

        Calcoli_ISEE.Risultato ris = calcola(2024);

        assertEquals(1, ris.rapporti.size());
        Calcoli_ISEE.Rapporto r = ris.rapporti.get(0);
        assertEquals("99", r.tipo);
        assertEquals("Coinbase|99|NESSUN_PERIODO", r.chiave);
        assertEquals("END", r.operatore);
        assertEquals("Coinbase", r.identificativo, "senza identificativo dell'utente: il nome del gruppo");
        assertNumero("6000", r.valore);
        assertTrue(r.valoreIncompleto, "senza prezzo il valore non è zero ma incompleto");
        assertNull(r.posizioni.get(1).valoreFine);
        assertNull(r.dataInizio, "aperto prima dell'anno");
        assertNull(r.dataFine);
        assertNull(r.giacenzaMedia, "la media non è stata chiesta");
        assertTrue(haNota(r, "nessun periodo FIAT"));
        assertTrue(haNota(r, "identificativo del rapporto da completare"));
        assertFalse(haNota(r, "fotografia"), "quantità dei movimenti e della fotografia coincidono");
    }

    @Test
    void quantitaDiversaDallaFotografia_eUnaNota() {
        deposito("2024-02-10 10:00", "Coinbase", "ETH", "1");
        fotografia("Coinbase", "ETH", "2", "6000");

        Calcoli_ISEE.Rapporto r = calcola(2024).rapporti.get(0);

        assertTrue(haNota(r, "quantità di ETH al 31/12 diversa dalla fotografia del quadro W/RW (1 contro 2)"));
    }

    @Test
    void giacenzaNegativaContaZeroEsiSegnala() {
        prelievo("2024-01-10 10:00", "Binance", "ETH", "1");
        fotografia("Binance", "ETH", "-1", "0.0000");

        Calcoli_ISEE.Rapporto r = calcola(2024).rapporti.get(0);

        assertTrue(r.haNegative());
        assertNumero("0", r.valore);
        assertTrue(haNota(r, "giacenza negativa di -1 ETH"));
    }

    // --- un rapporto per operatore ------------------------------------------------

    @Test
    void coinbaseCambioDiSocieta_dueRapporti_ilValoreTuttoAlSecondo() {
        // DSU 2027, anno 2025: 0,5 BTC dal 2023, Coinbase Irlanda fino al 19/06/2025, Lussemburgo dal 20/06/2025.
        periodoFiat("Coinbase", 1, "", "2025-06-19", "040", "675475", "");
        periodoFiat("Coinbase", 2, "2025-06-20", "", "092", "LU36476644", "");
        deposito("2023-03-01 10:00", "Coinbase", "BTC", "0.5");
        fotografia("Coinbase", "BTC", "0.5", "40000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(2, r.size());
        Calcoli_ISEE.Rapporto irlanda = r.get(0);
        assertEquals("E675475", irlanda.operatore);
        assertEquals("040", irlanda.statoEstero);
        assertNumero("0", irlanda.valore);
        assertFalse(irlanda.copreFineAnno);
        assertNull(irlanda.dataInizio, "aperto prima del 2025");
        assertEquals(LocalDate.of(2025, 6, 19), irlanda.dataFine);
        Calcoli_ISEE.Rapporto lussemburgo = r.get(1);
        assertEquals("ELU36476644", lussemburgo.operatore);
        assertNumero("40000", lussemburgo.valore);
        assertEquals(LocalDate.of(2025, 6, 20), lussemburgo.dataInizio);
        assertNull(lussemburgo.dataFine);
        assertEquals("Coinbase|99|P1", irlanda.chiave, "la chiave segue il periodo, non l'identificativo fiscale");
        assertEquals("Coinbase|99|P2", lussemburgo.chiave);
    }

    @Test
    void coinbase_clienteArrivatoDopoIlCambio_unSoloRapporto() {
        periodoFiat("Coinbase", 1, "", "2025-06-19", "040", "675475", "");
        periodoFiat("Coinbase", 2, "2025-06-20", "", "092", "LU36476644", "");
        deposito("2025-09-01 10:00", "Coinbase", "BTC", "0.5");
        fotografia("Coinbase", "BTC", "0.5", "40000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(1, r.size());
        assertEquals("ELU36476644", r.get(0).operatore);
        assertEquals(LocalDate.of(2025, 9, 1), r.get(0).dataInizio, "il primo movimento, non l'inizio del periodo");
    }

    @Test
    void okx_laDataDellaLicenzaNonEUnApertura() {
        // Periodo predefinito dal 27/01/2025 (licenza MiCA della stessa società), cliente dal 2022.
        periodoFiat("OKX", 1, "2025-01-27", "", "105", "C88193", "");
        deposito("2022-04-01 10:00", "OKX", "ETH", "1");
        fotografia("OKX", "ETH", "1", "3000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(1, r.size());
        assertEquals("EC88193", r.get(0).operatore, "i giorni prima del periodo restano allo stesso rapporto");
        assertNull(r.get(0).dataInizio);
        assertNull(r.get(0).dataFine);
        assertNumero("3000", r.get(0).valore);
    }

    @Test
    void periodoPredefinito_laChiaveEQuellaDelPredefinito() {
        periodoFiat("Coinbase", 1, "", "2025-06-19", "040", "675475", "");
        periodi.get("Coinbase").get(0)[Principale_GruppiWalletRW.COL_CHIAVE_DEFAULT] = "coinbase-irlanda";
        deposito("2024-03-01 10:00", "Coinbase", "BTC", "0.5");
        fotografia("Coinbase", "BTC", "0.5", "40000");

        assertEquals("Coinbase|99|coinbase-irlanda", calcola(2025).rapporti.get(0).chiave);
    }

    @Test
    void tokenScam_nonContaNelValoreENonLoRendeIncompleto() {
        deposito("2024-02-01 10:00", "Ledger", "ETH", "1");
        deposito("2024-03-01 10:00", "Ledger", "ACX **", "3144");
        fotografia("Ledger", "ETH", "1", "3000");
        fotografia("Ledger", "ACX **", "3144", null);

        Calcoli_ISEE.Rapporto r = calcola(2024).rapporti.get(0);

        assertNumero("3000", r.valore);
        assertFalse(r.valoreIncompleto);
        assertEquals(1, r.posizioni.size(), "il token SCAM non è fra le monete del rapporto");
        assertTrue(haNota(r, "1 token segnato SCAM"));
        assertFalse(haNota(r, "fotografia"), "le quantità restano coerenti con i movimenti");
    }

    @Test
    void periodoAggiuntoPrimaConLoStessoOperatore_iDatiScrittiRestanoAlRapporto() {
        // L'IBAN era stato scritto quando c'era solo P1. Poi l'utente aggiunge P2, più vecchio, stesso intermediario:
        // il tratto ora comincia da P2, ma i dati si trovano ancora sotto P1 e il tab li riscrive lì.
        periodoFiat("Kraken", 2, "", "2023-12-31", "105", "C88193", "");
        periodoFiat("Kraken", 1, "2024-01-01", "", "105", "C88193", "");
        deposito("2023-03-01 10:00", "Kraken", "ETH", "1");
        fotografia("Kraken", "ETH", "1", "3000");
        Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
        d.identificativo = "KR-123";
        dati.put("Kraken|99|P1", d);

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(1, r.size());
        assertEquals("KR-123", r.get(0).identificativo);
        assertEquals("Kraken|99|P1", r.get(0).chiave);
    }

    @Test
    void datiScrittiPrimaDelPrimoPeriodoFiat_restanoAlPrimoRapporto() {
        periodoFiat("Kraken", 1, "", "", "105", "C88193", "");
        deposito("2023-03-01 10:00", "Kraken", "ETH", "1");
        fotografia("Kraken", "ETH", "1", "3000");
        Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
        d.identificativo = "KR-123";
        dati.put("Kraken|99|NESSUN_PERIODO", d);

        Calcoli_ISEE.Rapporto r = calcola(2025).rapporti.get(0);

        assertEquals("KR-123", r.identificativo);
        assertEquals("Kraken|99|NESSUN_PERIODO", r.chiave);
    }

    @Test
    void periodoInItalia_ilCodiceFiscaleSenzaE() {
        periodoFiat("Young", 1, "", "", "IT", "12345678901", "");
        deposito("2024-02-01 10:00", "Young", "ETH", "1");
        fotografia("Young", "ETH", "1", "3000");

        assertEquals("12345678901", calcola(2024).rapporti.get(0).operatore);
    }

    @Test
    void operatoreDelPeriodo_soloLettereECifreDopoLaE() {
        String[] r = new String[Principale_GruppiWalletRW.COLONNE_PERIODO];
        java.util.Arrays.fill(r, "");
        r[Principale_GruppiWalletRW.COL_STATO_ESTERO] = "092";
        r[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = "lu 364-766 44";
        assertEquals("ELU36476644", Calcoli_ISEE.operatoreDelPeriodo(r));
        r[Principale_GruppiWalletRW.COL_IDENT_FISCALE] = "";
        assertEquals("END", Calcoli_ISEE.operatoreDelPeriodo(r));
        assertEquals("END", Calcoli_ISEE.operatoreDelPeriodo(null));
    }

    // --- date --------------------------------------------------------------------

    @Test
    void gruppoApertoESvuotatoNellAnno_dataFineNonDaiMovimentiMaUnaNota() {
        deposito("2025-03-15 10:00", "Kraken", "ETH", "1");
        prelievo("2025-09-20 18:30", "Kraken", "ETH", "1");

        Calcoli_ISEE.Rapporto r = calcola(2025).rapporti.get(0);

        assertEquals(LocalDate.of(2025, 3, 15), r.dataInizio);
        assertNull(r.dataFine, "un conto a saldo zero di solito resta aperto");
        assertNumero("0", r.valore);
        assertTrue(haNota(r, "saldo a zero dal 20/09/2025"));
    }

    @Test
    void gruppoVuotoDaPrimaDellAnno_nonCompare() {
        deposito("2023-03-15 10:00", "Kraken", "ETH", "1");
        prelievo("2023-09-20 18:30", "Kraken", "ETH", "1");

        assertTrue(calcola(2025).rapporti.isEmpty());
    }

    @Test
    void datiDellUtente_identificativoOperatoreEChiusura() {
        deposito("2025-03-15 10:00", "Kraken", "ETH", "1");
        prelievo("2025-09-20 18:30", "Kraken", "ETH", "1");
        Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
        d.identificativo = "AA1234";
        d.operatore = "e999";
        d.chiusura = LocalDate.of(2025, 10, 1);
        dati.put("Kraken|99|NESSUN_PERIODO", d);

        Calcoli_ISEE.Rapporto r = calcola(2025).rapporti.get(0);

        assertEquals("AA1234", r.identificativo);
        assertEquals("E999", r.operatore);
        assertEquals("END", r.operatorePeriodo);
        assertEquals(LocalDate.of(2025, 10, 1), r.dataFine);
        assertFalse(haNota(r, "saldo a zero"), "la chiusura c'è già");
        assertFalse(haNota(r, "identificativo"));
        assertFalse(haNota(r, "operatore"));
    }

    @Test
    void chiusoInUnAnnoPrecedente_nonCompare_salvoUnSaldoNeiMovimenti() {
        deposito("2024-03-15 10:00", "Kraken", "ETH", "1");
        prelievo("2025-02-01 10:00", "Kraken", "ETH", "1");
        Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
        d.chiusura = LocalDate.of(2024, 12, 15);
        dati.put("Kraken|99|NESSUN_PERIODO", d);
        // il prelievo del 2025 è un movimento nell'anno, ma il rapporto è chiuso dal 2024 e il gruppo è vuoto
        assertTrue(calcola(2025).rapporti.isEmpty());

        // nel 2025 i movimenti mostrano ancora 1 ETH al 31/12: il valore resta, con la nota
        deposito("2025-06-01 10:00", "Kraken", "ETH", "1");
        fotografia("Kraken", "ETH", "1", "3000");
        Calcoli_ISEE.Rapporto r = calcola(2025).rapporti.get(0);
        assertNumero("3000", r.valore);
        assertTrue(haNota(r, "rapporto chiuso il 15/12/2024 ma al 31/12 il gruppo ha ancora un saldo"));
    }

    // --- liquidità -----------------------------------------------------------------

    @Test
    void contoCorrente_unRapporto01ConLaGiacenzaMediaEUno99PerLeCripto() {
        // Crypto.com 2025: Fiat Wallet come conto corrente. 500 euro fino al 1/7, 100 euro dal 2/7.
        //  1/1-1/7  : 182 giorni x 500 = 91.000
        //  2/7-31/12: 183 giorni x 100 = 18.300
        //  109.300 / 365 = 299,45
        periodoFiat("Crypto.com", 1, "2025-01-27", "", "105", "C88392", "SI");
        liquidita("Crypto.com", "2024-06-01", "500");
        liquidita("Crypto.com", "2025-07-02", "-400");
        deposito("2024-06-02 10:00", "Crypto.com", "ETH", "1");
        fotografia("Crypto.com", "ETH", "1", "3000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(2, r.size());
        Calcoli_ISEE.Rapporto conto = r.get(0);
        assertEquals("01", conto.tipo);
        assertEquals("Crypto.com|01|P1", conto.chiave);
        assertEquals("Crypto.com|99|P1", r.get(1).chiave);
        assertNumero("100", conto.valore);
        assertNumero("299.45", conto.giacenzaMedia);
        Calcoli_ISEE.Rapporto cripto = r.get(1);
        assertEquals("99", cripto.tipo);
        assertNumero("3000", cripto.valore);
        assertNumero("0", cripto.liquidita);
        assertFalse(cripto.conLiquidita);
    }

    @Test
    void fiatWalletApertoPrimaDelPrimoMovimentoCripto_dateEGiorniDallaLiquidita() {
        // Crypto.com aperto il 1/3/2025 con 300 euro sul Fiat Wallet, primo acquisto di cripto il 1/7.
        //  1/3-31/12 = 306 giorni x 300 = 91.800 / 365 = 251,51
        periodoFiat("Crypto.com", 1, "2025-01-27", "", "105", "C88392", "SI");
        liquidita("Crypto.com", "2025-03-01", "300");
        deposito("2025-07-01 10:00", "Crypto.com", "ETH", "1");
        fotografia("Crypto.com", "ETH", "1", "3000");

        Calcoli_ISEE.Rapporto conto = calcola(2025).rapporti.get(0);

        assertEquals("01", conto.tipo);
        assertEquals(LocalDate.of(2025, 3, 1), conto.dataInizio);
        assertNumero("251.51", conto.giacenzaMedia);
    }

    @Test
    void gruppoConSolaLiquidita_compare_eSvuotatoNellAnnoHaLaMedia() {
        // Solo euro, 1.000 dal 2024, prelevati il 1/4/2025: 31+28+31 = 90 giorni x 1.000 / 365 = 246,58
        periodoFiat("Revolut", 1, "", "", "LT", "305799582", "SI");
        liquidita("Revolut", "2024-05-01", "1000");
        liquidita("Revolut", "2025-04-01", "-1000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(1, r.size(), "solo il 01: niente cripto");
        assertEquals("01", r.get(0).tipo);
        assertNumero("0", r.get(0).valore);
        assertNumero("246.58", r.get(0).giacenzaMedia);
        assertTrue(haNota(r.get(0), "saldo a zero dal 01/04/2025"));
    }

    @Test
    void senzaContoCorrente_laLiquiditaStaNel99() {
        periodoFiat("Crypto.com", 1, "2025-01-27", "", "105", "C88392", "NO");
        liquidita("Crypto.com", "2024-06-01", "500");
        liquidita("Crypto.com", "2025-07-02", "-400");
        deposito("2024-06-02 10:00", "Crypto.com", "ETH", "1");
        fotografia("Crypto.com", "ETH", "1", "3000");

        List<Calcoli_ISEE.Rapporto> r = calcola(2025).rapporti;

        assertEquals(1, r.size());
        assertNumero("3100", r.get(0).valore);
        assertNumero("100", r.get(0).liquidita);
    }

    @Test
    void liquiditaNegativa_contaZeroConNota() {
        liquidita("Bitpanda", "2025-03-01", "-50");
        deposito("2025-03-01 10:00", "Bitpanda", "ETH", "1");
        fotografia("Bitpanda", "ETH", "1", "3000");

        Calcoli_ISEE.Rapporto r = calcola(2025).rapporti.get(0);

        assertNumero("3000", r.valore);
        assertTrue(haNota(r, "saldo in valuta negativo al 31/12 (-50.00 EUR"));
    }

    // --- media giornaliera dei rapporti 99 --------------------------------------------

    @Test
    void mediaGiornalieraDiUnSaldoCostanteNelBisestile() {
        // 1 ETH per tutto il 2024 a 3.000: 366 giorni x 3.000 / 365 (il divisore è sempre 365)
        deposito("2023-12-01 10:00", "Coinbase", "ETH", "1");
        fotografia("Coinbase", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2024);

        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));

        assertNumero("3008.21917808", ris.rapporti.get(0).giacenzaMedia);
        assertFalse(ris.rapporti.get(0).mediaIncompleta);
    }

    @Test
    void mediaGiornalieraConVenditaParzialeEAcquistoAggiuntivo() {
        // 2 ETH dal 2023. Vendita di 1 ETH il 30/06, acquisto di 1 ETH il 1/7. Prezzo 2.000 fino al 30/06, poi 3.000.
        //  1/1-29/06 : 181 giorni x 2 ETH x 2.000 = 724.000
        //  30/06     :   1 giorno  x 1 ETH x 2.000 =   2.000   (a fine giornata ne resta 1)
        //  1/7-31/12 : 184 giorni x 2 ETH x 3.000 = 1.104.000
        //  somma 1.830.000 / 365 = 5.013,69863014
        deposito("2023-12-01 10:00", "Coinbase", "ETH", "2");
        prelievo("2024-06-30 12:00", "Coinbase", "ETH", "1");
        deposito("2024-07-01 09:00", "Coinbase", "ETH", "1");
        fotografia("Coinbase", "ETH", "2", "6000");
        Calcoli_ISEE.Risultato ris = calcola(2024);

        Calcoli_ISEE.calcolaMediaGiornaliera(ris, (m, qta, g) ->
                qta.multiply(g.isAfter(LocalDate.of(2024, 6, 30)) ? new BigDecimal("3000") : new BigDecimal("2000")));

        assertNumero("5013.69863014", ris.rapporti.get(0).giacenzaMedia);
        assertNumero("6000", ris.rapporti.get(0).valore);
    }

    @Test
    void ilGiroContoFraDueGruppiNonCambiaLaSommaDelleMedie() {
        // 1 ETH su Coinbase fino al 30/09, trasferito il 1/10 su Ledger: la somma delle due medie è quella
        // di un solo rapporto che lo tiene tutto l'anno (366 x 3.000 / 365)
        deposito("2023-12-01 10:00", "Coinbase", "ETH", "1");
        prelievo("2024-10-01 10:00", "Coinbase", "ETH", "1");
        deposito("2024-10-01 10:20", "Ledger", "ETH", "1");
        fotografia("Ledger", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2024);

        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));

        Calcoli_ISEE.Rapporto coinbase = ris.delGruppo("Coinbase").get(0);
        Calcoli_ISEE.Rapporto ledger = ris.delGruppo("Ledger").get(0);
        // Coinbase: 1/1-30/09 = 274 giorni. Ledger: 1/10-31/12 = 92 giorni.
        assertNumero("2252.05479452", coinbase.giacenzaMedia);
        assertNumero("756.16438356", ledger.giacenzaMedia);
        assertNumero("0", coinbase.valore);
        assertNumero("3000", ledger.valore);
        assertNumero("3008.21917808", coinbase.giacenzaMedia.add(ledger.giacenzaMedia));
    }

    @Test
    void piuWalletDelloStessoGruppoSonoUnSoloRapportoEIlTrasferimentoFraLoroENeutro() {
        // Ledger A e Ledger B stanno nello stesso gruppo "Wallet 02": un rapporto solo. Il 1/10 1 ETH passa da A a B.
        deposito("2023-12-01 10:00", "Ledger A", "ETH", "1");
        prelievo("2024-10-01 10:00", "Ledger A", "ETH", "1");
        deposito("2024-10-01 10:20", "Ledger B", "ETH", "1");
        deposito("2024-02-01 10:00", "Coinbase", "ETH", "2");
        fotografia("Wallet 02", "ETH", "1", "3000");
        fotografia("Wallet 01", "ETH", "2", "6000");

        Calcoli_ISEE.Risultato ris = calcola(2024, v -> v[3].startsWith("Ledger") ? "Wallet 02" : "Wallet 01");
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));

        assertEquals(2, ris.rapporti.size());
        assertEquals("Wallet 01", ris.rapporti.get(0).gruppo, "ordine dei gruppi");
        Calcoli_ISEE.Rapporto w2 = ris.delGruppo("Wallet 02").get(0);
        assertNumero("3000", w2.valore);
        // 1 ETH tutto l'anno senza interruzioni: 366 giorni x 3.000 / 365, senza buchi né doppi conteggi al passaggio
        assertNumero("3008.21917808", w2.giacenzaMedia);
        assertEquals(LocalDate.of(2024, 2, 1), ris.delGruppo("Wallet 01").get(0).dataInizio);
    }

    @Test
    void laMediaSiDivideFraIRapportiDelCambioDiSocieta() {
        // 1 ETH tutto il 2025 a 3.000: Irlanda 1/1-19/06 = 170 giorni, Lussemburgo 20/06-31/12 = 195 giorni.
        periodoFiat("Coinbase", 1, "", "2025-06-19", "040", "675475", "");
        periodoFiat("Coinbase", 2, "2025-06-20", "", "092", "LU36476644", "");
        deposito("2024-01-10 10:00", "Coinbase", "ETH", "1");
        fotografia("Coinbase", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2025);

        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));

        assertNumero("1397.26027397", ris.rapporti.get(0).giacenzaMedia);   // 170 x 3.000 / 365
        assertNumero("1602.73972603", ris.rapporti.get(1).giacenzaMedia);   // 195 x 3.000 / 365
    }

    @Test
    void laMediaDel99ComprendeLaLiquiditaSenzaContoCorrente() {
        // 1 ETH a 3.000 e 365 euro tutto il 2025: 3.000 + 365 x 365 / 365 = 3.365
        liquidita("Bitpanda", "2024-01-01", "365");
        deposito("2024-01-01 10:00", "Bitpanda", "ETH", "1");
        fotografia("Bitpanda", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2025);

        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));

        assertNumero("3365", ris.rapporti.get(0).giacenzaMedia);
        assertNumero("3365", ris.rapporti.get(0).valore);
    }

    @Test
    void giorniSenzaPrezzoRendonoLaMediaIncompleta() {
        deposito("2023-12-01 10:00", "Coinbase", "ETH", "1");
        fotografia("Coinbase", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2024);

        // prezzi solo per le prime due settimane di gennaio
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, (mo, qta, g) ->
                g.isBefore(LocalDate.of(2024, 1, 15)) ? qta.multiply(new BigDecimal("3000")) : null);

        Calcoli_ISEE.Rapporto c = ris.rapporti.get(0);
        assertTrue(c.mediaIncompleta);
        assertEquals(366 - 14, c.giorniSenzaPrezzo);
        // 14 giorni x 3.000 / 365: i giorni senza prezzo non valgono zero "di nascosto", sono segnalati
        assertNumero("115.06849315", c.giacenzaMedia);
        // e finiscono nell'elenco dei prezzi da sistemare: dal 15/01 al 31/12, 1 ETH al giorno
        assertEquals(366 - 14, ris.prezziMancanti.size());
        assertEquals(LocalDate.of(2024, 1, 15), ris.prezziMancanti.get(0).giorno);
        assertEquals(LocalDate.of(2024, 12, 31), ris.prezziMancanti.get(ris.prezziMancanti.size() - 1).giorno);
        assertNumero("1", ris.prezziMancanti.get(0).qta);
        assertEquals(java.util.Set.of("Coinbase"), ris.prezziMancanti.get(0).gruppi);

        // prezzi scritti per tutti i giorni: ripetuta sullo stesso risultato, la media è completa
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, prezziCostanti("3000"));
        assertFalse(c.mediaIncompleta);
        assertTrue(ris.prezziMancanti.isEmpty());
        assertNumero("3008.21917808", c.giacenzaMedia);
    }

    @Test
    void ilValoreSiChiedeSoloPerIGiorniConGiacenza_eIToken_SCAM_nonContano() {
        // ETH comprato il 1/12/2024: 31 giorni di giacenza. Un token SCAM tenuto tutto l'anno non si prezza mai.
        deposito("2024-12-01 10:00", "Coinbase", "ETH", "1");
        deposito("2023-06-01 10:00", "Coinbase", "FAKE **", "1000");
        fotografia("Coinbase", "ETH", "1", "3000");
        Calcoli_ISEE.Risultato ris = calcola(2024);

        java.util.List<String> chiesti = new java.util.ArrayList<>();
        Calcoli_ISEE.calcolaMediaGiornaliera(ris, (m, qta, g) -> {
            chiesti.add(m.Moneta + " " + g);
            return qta.multiply(new BigDecimal("3000"));
        });

        assertEquals(31, chiesti.size(), chiesti.toString());
        assertTrue(chiesti.stream().allMatch(c -> c.startsWith("ETH 2024-12-")), chiesti.toString());
        assertEquals(31, Calcoli_ISEE.giorniConGiacenza(ris).size(), "il pre-scarico chiede gli stessi giorni");
        // 31 x 3.000 / 365
        assertNumero("254.79452055", ris.rapporti.get(0).giacenzaMedia);
        assertFalse(ris.rapporti.get(0).mediaIncompleta);
    }

    @Test
    void ilPrezzoDelGiornoEQuelloDelleOreZeroDelGiornoDopo_eIl31_12EIlValoreDiFineAnno() {
        assertEquals(Calcoli_ISEE.istanteFineAnno(2024), Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 12, 31)));
        assertEquals(FunzioniDate.ConvertiDatainLongMinuto("2024-03-16 00:00"),
                Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 3, 15)));
    }

    @Test
    void arrotondamentoAllEuro() {
        assertNumero("299", Calcoli_ISEE.euro(new BigDecimal("299.45")));
        assertNumero("300", Calcoli_ISEE.euro(new BigDecimal("299.50")));
    }
}
