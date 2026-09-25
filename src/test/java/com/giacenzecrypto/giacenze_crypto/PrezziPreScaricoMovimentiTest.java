package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova la <b>raccolta</b> del pre-scarico prezzi ({@link Prezzi#RaccogliRichiestePerMovimenti}):
 * quali coppie (moneta, ora) vengono chieste agli exchange partendo da un insieme di movimenti.
 *
 * <p><b>Perché è separata dal resto del percorso.</b> Gli altri test dei prezzi
 * ({@code PrezziLottoCCXTTest}) devono lanciare Node e si auto-saltano dove non c'è; qui si prova
 * solo la decisione, che è codice puro: nessun database, nessuna rete, nessun processo figlio.
 * Serve perché è esattamente il punto dove il pre-scarico può sbagliare in silenzio — chiedere una
 * coppia di troppo è una richiesta sprecata, chiederne una di meno è un movimento che si riscarica
 * il prezzo da solo con un processo Node dedicato, e in nessuno dei due casi cambia un prezzo, che
 * è ciò che rende il difetto invisibile senza una prova come questa.
 *
 * <p>I due casi che hanno motivato la classe sono documentati sui rispettivi metodi: le ore di
 * confine (17/09/2026: sette invocazioni singole in coda a un lotto da 312) e i token con address,
 * che vanno esclusi <b>tranne</b> quelli di {@link Principale#Mappa_AddressRete_Nome}.
 */
class PrezziPreScaricoMovimentiTest {

    /** Address del BUSD su BSC: è in {@link Principale#Mappa_AddressRete_Nome}, mappato su "BUSD". */
    private static final String BUSD_BSC = "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56";
    /** Address di un token qualunque su BSC, non mappato: il suo prezzo arriva da DefiLlama. */
    private static final String TOKEN_QUALUNQUE_BSC = "0x0523215DCa5FeC5F0Db5f4b9B5dcF6b3A2cE3A11";

    @BeforeAll
    static void apre() {
        //Le due mappe che la raccolta interroga; entrambe sono elenchi scritti nel codice, non
        //letture dal database, quindi qui basta chiamarle come fa l'avvio dell'applicazione.
        VarCondivise.CompilaMappaChain();
        VarCondivise.CompilaMappaRetiSupportate();
        Prezzi.CompilaMoneteStessoPrezzo();
        //Nessuna operazione interrompibile aperta: altrimenti un test precedente potrebbe aver
        //lasciato una richiesta di interruzione e la raccolta uscirebbe subito, passando per finta.
        Interruzione.Azzera();
    }

    /**
     * Movimento minimo per la raccolta: solo i campi che legge.
     *
     * @param data   formato {@code yyyy-MM-dd HH:mm} (la precisione del campo [1])
     * @param rete   campo [34]; blank per un movimento senza blockchain
     */
    private static String[] movimento(String data, String rete,
            String moneta1, String tipo1, String address1,
            String moneta2, String tipo2, String address2) {
        String[] v = new String[Importazioni.ColonneTabella];
        Importazioni.RiempiVuotiArray(v);
        v[0] = "20221118050200_Test_00";
        v[1] = data;
        v[8] = moneta1;
        v[9] = tipo1;
        v[10] = "1";
        v[11] = moneta2;
        v[12] = tipo2;
        v[13] = "1";
        v[26] = address1;
        v[28] = address2;
        v[34] = rete;
        return v;
    }

    /** Movimento senza rete né address: il caso normale di un exchange centralizzato. */
    private static String[] movimentoSemplice(String data, String moneta) {
        return movimento(data, "", moneta, "Crypto", "", "", "", "");
    }

    private static Set<Long> oreChieste(List<Prezzi.RichiestaPrezzo> richieste, String simbolo) {
        return richieste.stream()
                .filter(r -> r.simbolo.equalsIgnoreCase(simbolo))
                .map(r -> r.since)
                .collect(Collectors.toSet());
    }

    private static Set<String> simboliChiesti(List<Prezzi.RichiestaPrezzo> richieste) {
        return richieste.stream().map(r -> r.simbolo).collect(Collectors.toSet());
    }

    private static long ora(String data) {
        return FunzioniDate.InizioOraRoma(FunzioniDate.ConvertiDatainLongMinuto(data));
    }

    /**
     * Il difetto misurato il 17/09/2026: {@code CambioXXXEUR} cerca il prezzo a ±5 minuti
     * dall'istante, quindi un movimento nei primi cinque minuti dell'ora si serve anche dell'ora
     * precedente. Il pre-scarico ne chiedeva una sola, e quei movimenti finivano al percorso lento —
     * un processo Node ciascuno, ~3.300 ms contro ~188 ms dentro un lotto.
     */
    @Test
    void unMovimentoNeiPrimiCinqueMinutiChiedeAncheLOraPrecedente() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimentoSemplice("2022-11-18 05:02", "BNB")), 0);

        assertEquals(Set.of(ora("2022-11-18 04:00"), ora("2022-11-18 05:00")),
                oreChieste(richieste, "BNB"),
                "a 05:02 la finestra a -5 minuti cade nell'ora precedente, che va scaricata");
    }

    /** Il simmetrico: a 05:57 la finestra a +5 minuti sconfina nell'ora successiva. */
    @Test
    void unMovimentoNegliUltimiCinqueMinutiChiedeAncheLOraSuccessiva() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimentoSemplice("2022-11-18 05:57", "BNB")), 0);

        assertEquals(Set.of(ora("2022-11-18 05:00"), ora("2022-11-18 06:00")),
                oreChieste(richieste, "BNB"));
    }

    /**
     * Le due ore di confine non sono simmetriche: l'ora precedente finisce con la candela delle 04:59,
     * quindi serve solo fino alle 05:04:00 comprese. Fra le 05:04 e le 05:05 la vecchia regola
     * ({@code InizioOraRoma(istante - 5min)}) la chiedeva lo stesso: nessun exchange poteva coprirla e
     * la cascata dei lotti la girava su tutti e otto (osservato il 25/09/2026 su un'importazione OKX).
     */
    @Test
    void lOraPrecedenteSiChiedeSoloSeLaSuaUltimaCandelaEEntroCinqueMinuti() {
        long cinqueEQuattro = FunzioniDate.ConvertiDatainLongMinuto("2022-11-18 05:04");
        long oraPrecedente = ora("2022-11-18 04:00");
        long oraCorrente = ora("2022-11-18 05:00");

        assertEquals(Set.of(oraCorrente, oraPrecedente), Prezzi.OreDaCoprire(cinqueEQuattro),
                "alle 05:04:00 la candela delle 04:59 e' a 5 minuti esatti: l'ora precedente serve");
        assertEquals(Set.of(oraCorrente), Prezzi.OreDaCoprire(cinqueEQuattro + 1),
                "un millisecondo dopo la candela delle 04:59 e' oltre i 5 minuti");
        assertEquals(Set.of(oraCorrente), Prezzi.OreDaCoprire(cinqueEQuattro + 30_000),
                "alle 05:04:30 l'ora precedente non puo' dare nulla");
        //Il lato dell'ora successiva era gia' esatto: la sua prima candela e' quella delle 06:00.
        long cinqueECinquantacinque = FunzioniDate.ConvertiDatainLongMinuto("2022-11-18 05:55");
        assertEquals(Set.of(oraCorrente, ora("2022-11-18 06:00")), Prezzi.OreDaCoprire(cinqueECinquantacinque));
        assertEquals(Set.of(oraCorrente), Prezzi.OreDaCoprire(cinqueECinquantacinque - 1));
    }

    /**
     * L'altra metà della regola, ed è ciò che impedisce alla correzione di triplicare le richieste:
     * lontano dai bordi le tre ore di {@code CambioXXXEUR} coincidono e l'insieme si richiude su una.
     */
    @Test
    void unMovimentoLontanoDaiBordiChiedeUnOraSola() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimentoSemplice("2022-11-18 05:30", "BNB")), 0);

        assertEquals(1, richieste.size(), "una sola ora: le finestre a ±5 minuti non escono dall'ora");
        assertEquals(ora("2022-11-18 05:00"), richieste.get(0).since);
    }

    /**
     * Un token con address e rete validi non passa da CCXT ma da {@code CambioAddressEUR}, che legge
     * i prezzi on-chain: chiederlo agli exchange riempie la cache a una chiave che nessuno andrà a
     * leggere. È quello che il 17/09/2026 ha speso richieste per WBNB e simili, seguite comunque
     * dalle chiamate a DefiLlama.
     */
    @Test
    void unTokenConAddressERetePassaDagliOnChainENonSiChiede() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimento("2022-11-18 05:30", "BSC",
                        "BNB", "Crypto", "BNB",
                        "TOKENX", "Crypto", TOKEN_QUALUNQUE_BSC)), 0);

        assertFalse(simboliChiesti(richieste).contains("TOKENX"),
                "il prezzo di questo token arriva da DefiLlama: la richiesta agli exchange è sprecata");
        assertTrue(simboliChiesti(richieste).contains("BNB"),
                "il nativo della chain non ha un address di contratto valido e resta sugli exchange");
    }

    /**
     * L'eccezione voluta: gli address di {@link Principale#Mappa_AddressRete_Nome} sono quelli per cui
     * {@code DammiPrezzoInfoTransazione} sostituisce il simbolo e azzera l'address apposta per cercare
     * il prezzo sugli exchange (più preciso e uguale su tutte le chain). Quelli vanno chiesti — e vanno
     * chiesti <b>col simbolo mappato</b>, altrimenti la cache si riempie sotto il nome locale e la
     * valorizzazione, che cerca quello mappato, non trova nulla.
     */
    @Test
    void unTokenDiMappaAddressReteSiChiedeColSimboloMappato() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimento("2022-11-18 05:30", "BSC",
                        "", "", "",
                        "BUSD-LOCALE", "Crypto", BUSD_BSC.toLowerCase())), 0);

        assertEquals(Set.of("BUSD"), simboliChiesti(richieste),
                "l'address è in Mappa_AddressRete_Nome (che è case-insensitive): si chiede BUSD");
    }

    /**
     * Se la rete non è tra quelle supportate la valorizzazione azzera gli address e torna a cercare
     * per simbolo ({@code DammiPrezzoDaTransazione}): la raccolta deve fare lo stesso, o lascerebbe
     * scoperti proprio i movimenti che gli exchange possono servire.
     */
    @Test
    void unaReteNonSupportataRiportaIlTokenSulPercorsoPerSimbolo() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimento("2022-11-18 05:30", "RETEIGNOTA",
                        "", "", "",
                        "TOKENX", "Crypto", TOKEN_QUALUNQUE_BSC)), 0);

        assertEquals(Set.of("TOKENX"), simboliChiesti(richieste));
    }

    /** Le esclusioni che c'erano già: FIAT, SCAM, controvalore dichiarato in [14]. */
    @Test
    void fiatScamEControvaloreDichiaratoRestanoFuori() {
        String[] conControvalore = movimentoSemplice("2022-11-18 05:30", "BNB");
        conControvalore[14] = "123,45";

        List<String[]> movimenti = new ArrayList<>();
        movimenti.add(conControvalore);
        movimenti.add(movimento("2022-11-18 05:30", "", "EUR", "FIAT", "", "SCAMMONE **", "Crypto", ""));

        assertTrue(Prezzi.RaccogliRichiestePerMovimenti(movimenti, 0).isEmpty(),
                "nessuna di queste tre categorie verrà mai valorizzata dagli exchange");
    }

    /** Il filtro per anno minimo legge le prime quattro cifre dell'ID, non la data. */
    @Test
    void ilFiltroPerAnnoMinimoScartaIMovimentiPiuVecchi() {
        String[] vecchio = movimentoSemplice("2022-11-18 05:30", "BNB");
        vecchio[0] = "20221118053000_Test_00";

        assertTrue(Prezzi.RaccogliRichiestePerMovimenti(List.<String[]>of(vecchio), 2023).isEmpty());
        assertFalse(Prezzi.RaccogliRichiestePerMovimenti(List.<String[]>of(vecchio), 2022).isEmpty());
    }

    /**
     * Due movimenti nella stessa ora sulla stessa moneta condividono una sola richiesta: è il
     * guadagno vero del pre-scarico su un archivio denso, e la ragione per cui la raccolta tiene una
     * chiave (simbolo, ora) invece di una richiesta per movimento.
     */
    @Test
    void piuMovimentiNellaStessaOraCondividonoUnaSolaRichiesta() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(List.of(
                movimentoSemplice("2022-11-18 05:20", "BNB"),
                movimentoSemplice("2022-11-18 05:40", "BNB")), 0);

        assertEquals(1, richieste.size());
    }

    /**
     * {@code CambioXXXEUR} normalizza il simbolo con {@link Principale#Mappa_MoneteStessoPrezzo}
     * prima di qualunque ricerca: un wrapped vale quanto il suo nativo. Chiedendolo sotto il nome non
     * normalizzato la cache si riempirebbe a una chiave che la valorizzazione non guarderà mai — il
     * pre-scarico farebbe il lavoro e il movimento se lo rifarebbe comunque da solo.
     */
    @Test
    void unSimboloWrappedSiChiedeColNomeNormalizzato() {
        List<Prezzi.RichiestaPrezzo> richieste = Prezzi.RaccogliRichiestePerMovimenti(
                List.<String[]>of(movimentoSemplice("2022-11-18 05:30", "WETH")), 0);

        assertEquals(Set.of("ETH"), simboliChiesti(richieste),
                "WETH ha lo stesso prezzo di ETH ed è sotto ETH che verrà cercato");
    }

    @Test
    void unaRaccoltaSenzaMovimentiNonChiedeNulla() {
        assertTrue(Prezzi.RaccogliRichiestePerMovimenti(null, 0).isEmpty());
        assertTrue(Prezzi.RaccogliRichiestePerMovimenti(List.of(), 0).isEmpty());
    }
}
