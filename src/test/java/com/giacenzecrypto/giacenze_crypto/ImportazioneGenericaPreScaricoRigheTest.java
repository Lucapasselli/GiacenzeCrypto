package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova la raccolta del pre-scarico prezzi degli import CSV
 * ({@link ImportazioneGenerica#RaccogliRichiestePerRighe}): quali coppie (moneta, ora) vengono chieste
 * agli exchange partendo dalle righe grezze di un CSV, prima che i movimenti esistano.
 *
 * <p>È la gemella di {@code PrezziPreScaricoMovimentiTest} sull'altra raccolta, e prova le stesse due
 * regole che qui mancavano: le <b>tre</b> ore di {@code CambioXXXEUR} (non una) e la normalizzazione
 * del simbolo con {@link Principale#Mappa_MoneteStessoPrezzo}. Come là, nessun database, nessuna rete
 * e nessun processo Node: si prova la decisione, non lo scaricamento.
 */
class ImportazioneGenericaPreScaricoRigheTest {

    @BeforeAll
    static void apre() {
        //La mappa wrapped/nativo: nell'applicazione la riempie l'avvio, qui la chiamiamo a mano.
        Prezzi.CompilaMoneteStessoPrezzo();
        //Nessuna operazione interrompibile aperta: con una richiesta di interruzione pendente la
        //raccolta uscirebbe al primo giro e i test passerebbero per finta.
        Interruzione.Azzera();
    }

    /** Configurazione minima: data in colonna 0, moneta in colonna 1, formato standard. */
    private static ConfigurazioneImport cfg() {
        ConfigurazioneImport cfg = new ConfigurazioneImport();
        cfg.colonnaData = 0;
        cfg.colonnaMoneta = 1;
        cfg.colonnaMonetaFee = 2;
        return cfg;
    }

    private static String[] riga(String data, String moneta) {
        return new String[]{data, moneta, ""};
    }

    /**
     * L'ora attesa si ricava dalla configurazione stessa e non da una costante: {@code formatoData} e
     * il fuso dell'import sono sue, e scrivere qui un millisecondo calcolato a mano legherebbe il test
     * al fuso della macchina che lo esegue.
     */
    private static long ora(ConfigurazioneImport cfg, String data) {
        return FunzioniDate.InizioOraRoma(cfg.convertiDataInMillis(data));
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

    /**
     * Il difetto: la raccolta chiedeva una sola ora, mentre chi valorizza cerca il prezzo a ±5 minuti
     * dall'istante e quindi copre anche l'ora accanto. Una riga di CSV nei primi cinque minuti di
     * un'ora restava scoperta proprio lì, e si riscaricava il prezzo da sola.
     */
    @Test
    void unaRigaNeiPrimiCinqueMinutiChiedeAncheLOraPrecedente() {
        ConfigurazioneImport cfg = cfg();
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:02:00", "BNB")), cfg);

        assertEquals(Set.of(ora(cfg, "2022-11-18 04:30:00"), ora(cfg, "2022-11-18 05:02:00")),
                oreChieste(richieste, "BNB"));
    }

    /** Il simmetrico, sul bordo superiore dell'ora. */
    @Test
    void unaRigaNegliUltimiCinqueMinutiChiedeAncheLOraSuccessiva() {
        ConfigurazioneImport cfg = cfg();
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:57:00", "BNB")), cfg);

        assertEquals(Set.of(ora(cfg, "2022-11-18 05:57:00"), ora(cfg, "2022-11-18 06:30:00")),
                oreChieste(richieste, "BNB"));
    }

    /** Ed è ciò che impedisce alla correzione di moltiplicare le richieste per tre. */
    @Test
    void unaRigaLontanaDaiBordiChiedeUnOraSola() {
        ConfigurazioneImport cfg = cfg();
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:30:00", "BNB")), cfg);

        assertEquals(1, richieste.size());
        assertEquals(ora(cfg, "2022-11-18 05:30:00"), richieste.get(0).since);
    }

    /**
     * La seconda regola che mancava: dopo la rinomina della configurazione
     * ({@code normalizzaMoneta}) va applicata quella di {@code CambioXXXEUR}, o la cache si riempie
     * sotto WETH mentre la valorizzazione cercherà ETH.
     */
    @Test
    void unSimboloWrappedSiChiedeColNomeNormalizzato() {
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:30:00", "WETH")), cfg());

        assertEquals(Set.of("ETH"), simboliChiesti(richieste));
    }

    /** Le esclusioni che c'erano già: euro, dollaro e token marcati SCAM. */
    @Test
    void euroDollaroEScamRestanoFuori() {
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:30:00", "EUR"),
                        riga("2022-11-18 05:30:00", "USD"),
                        riga("2022-11-18 05:30:00", "TRUFFA **")), cfg());

        assertTrue(richieste.isEmpty());
    }

    /** La colonna della commissione è una delle tre da guardare: dimenticarla costa richieste vere. */
    @Test
    void ancheLaMonetaDellaCommissioneVieneRaccolta() {
        List<Prezzi.RichiestaPrezzo> richieste = ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(new String[]{"2022-11-18 05:30:00", "ADA", "BNB"}), cfg());

        assertEquals(Set.of("ADA", "BNB"), simboliChiesti(richieste));
    }

    @Test
    void unaRaccoltaSenzaRigheNonChiedeNulla() {
        assertTrue(ImportazioneGenerica.RaccogliRichiestePerRighe(null, cfg()).isEmpty());
        assertTrue(ImportazioneGenerica.RaccogliRichiestePerRighe(List.of(), cfg()).isEmpty());
        assertTrue(ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("2022-11-18 05:30:00", "BNB")), null).isEmpty());
    }

    /** Una data che il formato configurato non sa leggere non produce richieste, e non esplode. */
    @Test
    void unaDataNonParsabileVieneSaltata() {
        assertTrue(ImportazioneGenerica.RaccogliRichiestePerRighe(
                List.<String[]>of(riga("non è una data", "BNB")), cfg()).isEmpty());
    }
}
