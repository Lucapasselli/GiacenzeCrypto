package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Fissa la macchina a stati del campo {@code [32]} ("prezzato") letta da
 * {@link Prezzi#isMovimentoPrezzato}, e in particolare l'auto-riparazione aggiunta il 2026-09-12.
 *
 * <p><b>Il campo ha tre valori e non due.</b> {@code "SI"} vuol dire prezzato; {@code ""} (o
 * {@code null}) vuol dire <i>da ricontrollare</i>, ed è la convenzione con cui la marcatura SCAM chiede
 * una nuova valutazione ({@code Principale.GiacenzeaData_Funzione_IdentificaComeScam} e simili);
 * {@code "NO"} vuol dire <i>già cercato, non chiedere più</i>, ed è quello che evita di rilanciare una
 * ricerca di rete a ogni caricamento della tabella per i token che un prezzo non ce l'hanno.
 *
 * <p><b>La combinazione contraddittoria.</b> Un movimento con {@code "NO"} e {@code [15]} diverso da
 * {@code 0.00} è valorizzato ma marcato come non valorizzato: il motore delle plusvalenze usa quel
 * controvalore (legge {@code v[15]}), mentre il contatore "Transazione senza prezzo" e il filtro
 * relativo continuano a segnalarlo. Non nasce così — {@code MovimentiCrypto.creaMovimento} scrive
 * {@code "NO"} solo insieme a {@code Prezzo = "0.00"} — ma la produce chi riscrive {@code [15]} a
 * posteriori, cioè il ricalcolo prezzi di <i>Opzioni → Varie</i>, che è proprio la strada con cui si
 * recupera un archivio importato senza prezzi (vedi {@link AttesaConnessione}).
 *
 * <p>I test non toccano la rete: il ramo di ricerca prezzo si raggiunge solo con {@code [32]} vuoto
 * <b>e</b> {@code [15]} a zero, caso qui deliberatamente non esercitato.
 */
class PrezziFlagValorizzatoTest {

    /** Movimento minimo: solo i campi che {@code isMovimentoPrezzato} legge. */
    private static String[] movimento(String flag32, String valore15) {
        String[] m = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < m.length; i++) m[i] = "";
        m[0] = "20220512062450_Test_001_001_PC";
        m[8] = "USDT";        //moneta in uscita, serve al controllo SCAM
        m[15] = valore15;
        m[32] = flag32;
        return m;
    }

    // ---- l'auto-riparazione ----------------------------------------------------------------------

    @Test
    void valorizzatoMaMarcatoNo_vieneCorrettoARitornaPrezzato() {
        String[] m = movimento("NO", "102.00");
        assertTrue(Prezzi.isMovimentoPrezzato(m), "un movimento con controvalore è prezzato");
        assertEquals("SI", m[32], "il flag contraddittorio va corretto, non lasciato com'è");
    }

    /**
     * È il caso che tiene in piedi il significato di {@code "NO"}: senza controvalore il movimento
     * resta non prezzato, e soprattutto <b>non</b> si rilancia una ricerca di prezzo — sarebbe una
     * chiamata di rete per movimento a ogni caricamento della tabella.
     */
    @Test
    void nonValorizzatoEMarcatoNo_restaCosiSenzaRicercarePrezzo() {
        String[] m = movimento("NO", "0.00");
        assertFalse(Prezzi.isMovimentoPrezzato(m));
        assertEquals("NO", m[32], "\"NO\" significa \"gia' cercato\": non va rimesso in discussione");
    }

    // ---- gli altri stati -------------------------------------------------------------------------

    @Test
    void giaMarcatoSi_restaPrezzato() {
        String[] m = movimento("SI", "102.00");
        assertTrue(Prezzi.isMovimentoPrezzato(m));
        assertEquals("SI", m[32]);
    }

    /** Il "Si" minuscolo dei dati vecchi viene normalizzato senza perdere l'esito. */
    @Test
    void siMinuscolo_vieneNormalizzato() {
        String[] m = movimento("Si", "102.00");
        assertTrue(Prezzi.isMovimentoPrezzato(m));
        assertEquals("SI", m[32]);
    }

    /**
     * Il flag vuoto è la richiesta di ricontrollo usata dalla marcatura SCAM: con un controvalore già
     * presente si risolve subito in {@code "SI"}, senza cercare nulla.
     */
    @Test
    void flagVuotoConControvalore_diventaSiSenzaCercare() {
        String[] m = movimento("", "102.00");
        assertTrue(Prezzi.isMovimentoPrezzato(m));
        assertEquals("SI", m[32]);
    }

    // ---- il percorso reale: il ricalcolo prezzi ---------------------------------------------------

    /**
     * Riproduce quello che fa il ciclo di <i>Opzioni → Varie → Ricalcola i prezzi</i> su un movimento
     * importato senza prezzo: scrive il controvalore e svuota il flag. Fissa il fatto che le due
     * riparazioni siano ridondanti fra loro — con il flag svuotato si arriva a {@code "SI"} per il ramo
     * del ricontrollo, senza bisogno di quello dell'auto-riparazione.
     */
    @Test
    void dopoIlRicalcoloPrezziIlMovimentoNonEPiuFraGliErrori() {
        String[] m = movimento("NO", "0.00");
        assertFalse(Prezzi.isMovimentoPrezzato(m), "prima del ricalcolo è un errore \"senza prezzo\"");

        //Le due righe del ciclo di Opzioni_Varie_RicalcolaPrezziActionPerformed
        m[15] = "102.00";
        m[32] = "";

        assertTrue(Prezzi.isMovimentoPrezzato(m), "dopo il ricalcolo non deve più contare come errore");
        assertEquals("SI", m[32]);
    }
}
