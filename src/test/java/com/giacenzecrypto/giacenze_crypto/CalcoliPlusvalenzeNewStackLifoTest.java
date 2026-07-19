package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di CARATTERIZZAZIONE dello stack LIFO del motore plusvalenze
 * ({@link Calcoli_PlusvalenzeNew#StackLIFO_TogliQta} e
 * {@link Calcoli_PlusvalenzeNew#StackLIFO_InserisciValore}).
 *
 * Questi test fotografano il comportamento ATTUALE del motore, inclusi i
 * comportamenti discutibili documentati in Documentazione/Analisi_Bug_Criticita.md
 * (voci C2, A2...). Se una correzione futura cambia deliberatamente uno di questi
 * comportamenti, il test corrispondente va aggiornato in modo esplicito insieme
 * alla correzione: un fallimento qui significa che un risultato fiscale è cambiato.
 *
 * Convenzioni sui campi del movimento (String[45], vedi Importazioni.ColonneTabella):
 *   v[0]  = ID transazione        v[1]  = data "yyyy-MM-dd HH:mm"
 *   v[3]  = wallet                v[5]  = descrizione operazione
 *   v[8]  = moneta uscita         v[10] = quantità uscita
 *   v[11] = moneta entrata        v[13] = quantità entrata
 *   v[15] = valore transazione    v[16] = vecchio costo di carico
 *   v[17] = nuovo costo di carico v[18] = classificazione (DTW, PTW, DAI...)
 *   v[19] = plusvalenza           v[22] = origine (A/M/AU)
 *   v[33] = flag calcolo plusvalenza (S/N)
 *   v[38] = flag anomalia LIFO ("A" = giacenza LIFO insufficiente)
 *
 * Ogni voce dello stack LIFO è uno String[4]: {moneta, quantità, costo, IDTransazione}.
 */
class CalcoliPlusvalenzeNewStackLifoTest {

    /** Mappa GruppoWallet->(Moneta->stack) usata dal motore; qui ne creiamo una locale per test. */
    private Map<String, ArrayDeque<String[]>> stacks;

    /** Contatore per generare ID unici: MappaIDTrans_LifoxID è statica e non azzerabile dai test. */
    private static int progressivoID = 0;

    @BeforeEach
    void setUp() {
        stacks = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Principale.MappaCryptoWallet.clear();
    }

    /** Crea un movimento minimo valido (tutti i campi a "") e lo registra in MappaCryptoWallet. */
    private static String[] nuovoMovimento() {
        String id = "TEST_2024-01-01 00.00_WALLET_001_XX_" + (++progressivoID);
        String[] m = new String[Importazioni.ColonneTabella];
        Arrays.fill(m, "");
        m[0] = id;
        Principale.MappaCryptoWallet.put(id, m);
        return m;
    }

    private void inserisci(String moneta, String qta, String costo) {
        Calcoli_PlusvalenzeNew.StackLIFO_InserisciValore(stacks, moneta, qta, costo, nuovoMovimento()[0]);
    }

    // ------------------------------------------------------------------
    // StackLIFO_InserisciValore
    // ------------------------------------------------------------------

    @Test
    void inserimento_creaLoStackEMemorizzaIlLotto() {
        String[] mov = nuovoMovimento();
        Calcoli_PlusvalenzeNew.StackLIFO_InserisciValore(stacks, "BTC", "10", "100", mov[0]);

        ArrayDeque<String[]> stack = stacks.get("BTC");
        assertNotNull(stack);
        assertEquals(1, stack.size());
        String[] lotto = stack.peek();
        assertEquals("BTC", lotto[0]);
        assertEquals("10", lotto[1]);
        assertEquals("100", lotto[2]);
        assertEquals(mov[0], lotto[3]);

        // Il dettaglio per-transazione (LifoXID) registra il lotto entrato
        Calcoli_PlusvalenzeNew.LifoXID lifo = Calcoli_PlusvalenzeNew.getIDLiFo(mov[0]);
        assertNotNull(lifo);
        assertEquals(1, lifo.Get_CryptoStackEntrato().size());
        // Lo snapshot pre-movimento è lo stack PRIMA dell'inserimento (vuoto)
        assertEquals(0, lifo.Get_CryptoStackEntratoPreMovimento().size());
    }

    @Test
    void inserimento_quantitaNegativaVieneResaAssoluta() {
        inserisci("BTC", "-5", "50");
        assertEquals("5", stacks.get("BTC").peek()[1]);
    }

    // ------------------------------------------------------------------
    // StackLIFO_TogliQta - casi base
    // ------------------------------------------------------------------

    @Test
    void prelievoEsatto_ritornaTuttoIlCostoESvuotaLoStack() {
        inserisci("BTC", "10", "100");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "10", true, mov[0]);

        assertEquals("100.00", costo); // scala DecimaliPlus=2
        assertTrue(stacks.get("BTC").isEmpty());
        assertEquals("", mov[38]); // nessuna anomalia
    }

    @Test
    void prelievo_consumaPrimaIlLottoPiuRecente_LIFO() {
        inserisci("BTC", "10", "100"); // primo lotto (più vecchio)
        inserisci("BTC", "5", "80");   // secondo lotto (più recente)
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "5", true, mov[0]);

        // Deve aver consumato il lotto da 80 (l'ultimo entrato), non quello da 100
        assertEquals("80.00", costo);
        ArrayDeque<String[]> stack = stacks.get("BTC");
        assertEquals(1, stack.size());
        assertEquals("10", stack.peek()[1]);
        assertEquals("100", stack.peek()[2]);
    }

    @Test
    void prelievoParziale_splittaIlLottoEProporzionaIlCosto() {
        inserisci("BTC", "10", "100");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "4", true, mov[0]);

        // costo unitario 100/10=10 -> usati 40, restano 6 a costo 60
        assertEquals("40.00", costo);
        ArrayDeque<String[]> stack = stacks.get("BTC");
        assertEquals(1, stack.size());
        assertEquals("6", stack.peek()[1]);
        assertEquals("60", stack.peek()[2]);
    }

    @Test
    void prelievoSuPiuLotti_sommaICostiNellOrdineGiusto() {
        inserisci("ETH", "10", "100"); // lotto vecchio
        inserisci("ETH", "5", "80");   // lotto recente
        String[] mov = nuovoMovimento();

        // 5 dal lotto recente (80) + 2 dal lotto vecchio (2*10=20) = 100
        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "ETH", "7", true, mov[0]);

        assertEquals("100.00", costo);
        ArrayDeque<String[]> stack = stacks.get("ETH");
        assertEquals(1, stack.size());
        assertEquals("8", stack.peek()[1]);
        assertEquals("80", stack.peek()[2]);
    }

    @Test
    void prelievo_quantitaNegativaTrattataComeAssoluta() {
        inserisci("BTC", "10", "100");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "-4", true, mov[0]);

        assertEquals("40.00", costo);
        assertEquals("6", stacks.get("BTC").peek()[1]);
    }

    // ------------------------------------------------------------------
    // StackLIFO_TogliQta - arrotondamenti
    // ------------------------------------------------------------------

    @Test
    void splitConDivisioneNonEsatta_usaScala30EHalfUp() {
        // 3 unità a costo 1: costo unitario = 0.333... periodico
        inserisci("XRP", "3", "1");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "XRP", "1", true, mov[0]);

        // Riproduco il calcolo del motore: il residuo sul lotto viene arrotondato
        // a DecimaliCalcoli (30) decimali, il costo usato è la differenza esatta.
        BigDecimal unitario = new BigDecimal("1").divide(new BigDecimal("3"),
                VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP);
        BigDecimal residuo = unitario.multiply(new BigDecimal("2"))
                .setScale(VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        BigDecimal usato = new BigDecimal("1").subtract(residuo);

        assertEquals(usato.setScale(VarStatiche.DecimaliPlus, RoundingMode.HALF_UP).toPlainString(), costo);
        assertEquals("0.33", costo);
        ArrayDeque<String[]> stack = stacks.get("XRP");
        assertEquals("2", stack.peek()[1]);
        assertEquals(residuo.toPlainString(), stack.peek()[2]);
        // Invariante fiscale: costo usato + costo residuo = costo originale del lotto
        assertEquals(0, usato.add(new BigDecimal(stack.peek()[2])).compareTo(new BigDecimal("1")));
    }

    // ------------------------------------------------------------------
    // StackLIFO_TogliQta - giacenza insufficiente
    // ------------------------------------------------------------------

    @Test
    void prelievoOltreGiacenza_ritornaSoloIlCostoCopertoEMarcaAnomalia() {
        inserisci("BTC", "5", "50");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "8", true, mov[0]);

        // Coperti solo 5 su 8: il costo è quello del lotto disponibile
        assertEquals("50.00", costo);
        // Il movimento viene marcato con anomalia LIFO
        assertEquals("A", mov[38]);

        // Nel dettaglio LIFO della transazione: il lotto consumato + la parte scoperta a costo 0
        Calcoli_PlusvalenzeNew.LifoXID lifo = Calcoli_PlusvalenzeNew.getIDLiFo(mov[0]);
        assertEquals(2, lifo.Get_CryptoStackUscito().size());
        String[] scoperta = lifo.Get_CryptoStackUscito().peekLast();
        assertEquals("3", scoperta[1]);
        assertEquals("0", scoperta[2]);
        assertEquals("", scoperta[3]); // nessuna transazione di origine
    }

    @Test
    void prelievoSuMonetaMaiVista_ritornaZeroEMarcaAnomalia() {
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "DOGE", "5", true, mov[0]);

        assertEquals("0.00", costo);
        assertEquals("A", mov[38]);
    }

    @Test
    void prelievoOltreGiacenzaDiTokenScam_nonMarcaAnomalia() {
        // I token marcati SCAM (suffisso " **") non devono segnalare LIFO mancante
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "FAKE **", "5", true, mov[0]);

        assertEquals("0.00", costo);
        assertEquals("", mov[38]);
    }

    @Test
    void prelievoRiuscito_azzeraUnaAnomaliaPrecedente() {
        inserisci("BTC", "10", "100");
        String[] mov = nuovoMovimento();
        mov[38] = "A"; // anomalia rimasta da un ricalcolo precedente

        Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "5", true, mov[0]);

        assertEquals("", mov[38]);
    }

    // ------------------------------------------------------------------
    // StackLIFO_TogliQta - modalità "solo lettura" (toglidaStack=false)
    // ------------------------------------------------------------------

    @Test
    void toglidaStackFalse_calcolaIlCostoSenzaModificareLoStack() {
        inserisci("BTC", "10", "100");
        String[] mov = nuovoMovimento();

        String costo = Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "4", false, mov[0]);

        // Stesso costo della modalità reale...
        assertEquals("40.00", costo);
        // ...ma lo stack originale resta intatto
        ArrayDeque<String[]> stack = stacks.get("BTC");
        assertEquals(1, stack.size());
        assertEquals("10", stack.peek()[1]);
        assertEquals("100", stack.peek()[2]);
    }

    // ------------------------------------------------------------------
    // StackLIFO_TogliQta - input degeneri (correzioni C2 e A2)
    // ------------------------------------------------------------------

    @Test
    void monetaOQuantitaVuote_ritornanoStringaVuota() {
        String[] mov = nuovoMovimento();
        assertEquals("", Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "", "5", true, mov[0]));
        assertEquals("", Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "", true, mov[0]));
    }

    @Test
    void quantitaNonNumerica_nonCrashaESegnalaConFlagE() {
        // Correzione C2: una quantità non numerica non lancia più
        // NumberFormatException; il movimento resta senza effetto sul LIFO
        // e viene segnalato con la lettera "E" nel campo 38.
        String[] mov = nuovoMovimento();
        assertEquals("", Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "abc", true, mov[0]));
        assertTrue(mov[38].contains("E"), "atteso flag E nel campo 38, trovato: \"" + mov[38] + "\"");
    }

    @Test
    void movimentoAssenteDallaMappa_conGiacenzaInsufficiente_nonCrasha() {
        // Correzione A2: se l'ID non esiste in MappaCryptoWallet la giacenza
        // insufficiente non causa più NPE (non c'è un movimento su cui segnalare
        // l'anomalia, ma il costo recuperabile viene comunque ritornato).
        assertDoesNotThrow(() -> Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(
                stacks, "BTC", "5", true, "ID_INESISTENTE_" + (++progressivoID)));
    }

    @Test
    void quantitaNonNumericaInInserisciValore_nonEntraNelloStackESegnalaConFlagE() {
        // Correzione C2 su StackLIFO_InserisciValore: quantità non numerica
        // -> nessun inserimento nello stack e flag "E" sul movimento.
        String[] mov = nuovoMovimento();
        Calcoli_PlusvalenzeNew.StackLIFO_InserisciValore(stacks, "BTC", "xyz", "100", mov[0]);
        assertTrue(stacks.get("BTC") == null || stacks.get("BTC").isEmpty());
        assertTrue(mov[38].contains("E"), "atteso flag E nel campo 38, trovato: \"" + mov[38] + "\"");
    }

    @Test
    void flagAnomalia_AedE_coesistonoENonSiSovrascrivono() {
        // I flag del campo 38 sono cumulativi: la "E" (dato non valido) non deve
        // cancellare la "A" (giacenza insufficiente) e viceversa.
        String[] mov = nuovoMovimento();
        // giacenza insufficiente -> "A"
        Calcoli_PlusvalenzeNew.StackLIFO_InserisciValore(stacks, "BTC", "1", "100", mov[0]);
        Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "5", true, mov[0]);
        assertTrue(mov[38].contains("A"));
        // quantità non numerica sullo stesso movimento -> si aggiunge la "E"
        Calcoli_PlusvalenzeNew.StackLIFO_TogliQta(stacks, "BTC", "abc", true, mov[0]);
        assertTrue(mov[38].contains("A") && mov[38].contains("E"),
                "attesi flag A ed E nel campo 38, trovato: \"" + mov[38] + "\"");
    }
}
