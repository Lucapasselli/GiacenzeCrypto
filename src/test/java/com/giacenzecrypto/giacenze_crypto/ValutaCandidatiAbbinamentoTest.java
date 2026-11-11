package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link GUI_ClassificazioneMovimento#ValutaCandidati}: quali candidati di un abbinamento (trasferimento fra wallet o
 * scambio differito) si evidenziano come più probabili, per data e valore in euro. Solo dati inventati.
 */
class ValutaCandidatiAbbinamentoTest {

    private static final long ORA = 3_600_000L;
    private static final long GIORNO = 24 * ORA;

    /** Movimento minimo: ID con la categoria, data, moneta e quantità della gamba che conta, valore in {@code [15]}. */
    private static String[] mov(String id, String data, String moneta, String qta, String valore) {
        String[] v = new String[Importazioni.ColonneTabella];
        java.util.Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        if (id.endsWith("_PC")) {
            v[8] = moneta;
            v[10] = "-" + qta;
        } else {
            v[11] = moneta;
            v[13] = qta;
        }
        v[15] = valore;
        return v;
    }

    private static String[] prelievo(String n, String data, String qta, String valore) {
        return mov("2024010100000" + n + "_Ex_001_001_PC", data, "BTC", qta, valore);
    }

    private static String[] deposito(String n, String data, String qta, String valore) {
        return mov("2024010100000" + n + "_Wal_001_001_DC", data, "BTC", qta, valore);
    }

    @Test
    void trasferimentoVinceIlPiuVicinoNelTempoFraDueDelloStessoValore() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        String[] vicino = deposito("2", "2024-03-10 10:20", "0.99", "99.00");
        String[] lontano = deposito("3", "2024-03-10 20:00", "0.99", "99.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(vicino, lontano), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(vicino[0]), ev.probabili());
    }

    @Test
    void trasferimentoVinceIlValoreMigliorePiuDelTempo() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        String[] preciso = deposito("2", "2024-03-10 11:00", "1", "100.00");
        String[] impreciso = deposito("3", "2024-03-10 10:10", "0.96", "96.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(preciso, impreciso), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(preciso[0]), ev.probabili());
    }

    @Test
    void duePariMeritoSiEvidenzianoEntrambi() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        String[] a = deposito("2", "2024-03-10 10:30", "1", "100.00");
        String[] b = deposito("3", "2024-03-10 10:40", "1", "100.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(a, b), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(a[0], b[0]), ev.probabili());
    }

    @Test
    void ilDepositoPrimaDelPrelievoPesaDiPiu() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        String[] dopo = deposito("2", "2024-03-10 12:00", "1", "100.00");
        String[] prima = deposito("3", "2024-03-10 08:00", "1", "100.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(prima, dopo), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(dopo[0]), ev.probabili());
    }

    @Test
    void ilValoreFuoriTolleranzaNonESuggeritoMaCandidatoUnicoSi() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        String[] fuori = deposito("2", "2024-03-10 10:01", "0.80", "80.00");
        assertTrue(GUI_ClassificazioneMovimento.ValutaCandidati(p, List.<String[]>of(fuori), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS).probabili().isEmpty());
        String[] dentro = deposito("3", "2024-03-10 23:00", "0.97", "97.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(fuori, dentro), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(dentro[0]), ev.probabili());
        assertEquals(Set.of(dentro[0]), ev.valoriVicini());
    }

    @Test
    void trasferimentoDiUnTokenSenzaPrezzoUsaLaQuantita() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1000", "0.00");
        String[] giusto = deposito("2", "2024-03-10 10:30", "999", "0.00");
        String[] sbagliato = deposito("3", "2024-03-10 10:10", "400", "0.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(giusto, sbagliato), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(giusto[0]), ev.probabili());
        assertTrue(ev.valoriVicini().isEmpty(), "senza valore non c'è nulla da dire sul verde della colonna valore");
    }

    @Test
    void scambioDifferitoSenzaValoreNonEValutabile() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1000", "0.00");
        String[] d = mov("20240101000002_Wal_001_001_DC", "2024-03-12 10:00", "NUOVO", "1000", "0.00");
        assertTrue(GUI_ClassificazioneMovimento.ValutaCandidati(p, List.<String[]>of(d), false, 15 * GIORNO).probabili().isEmpty());
    }

    @Test
    void scambioDifferitoFraMoltiGiorniSceglieIlValoreVicinoELOraVicina() {
        String[] p = prelievo("1", "2024-03-01 10:00", "1000", "500.00");
        String[] giusto = mov("20240101000002_Wal_001_001_DC", "2024-03-05 10:00", "NUOVO", "800", "498.00");
        String[] tardi = mov("20240101000003_Wal_001_001_DC", "2024-03-14 10:00", "NUOVO", "800", "499.00");
        String[] altroValore = mov("20240101000004_Wal_001_001_DC", "2024-03-02 10:00", "NUOVO", "100", "60.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(giusto, tardi, altroValore), false, 15 * GIORNO);
        assertEquals(Set.of(giusto[0]), ev.probabili());
        assertEquals(Set.of(giusto[0], tardi[0]), ev.valoriVicini());
    }

    @Test
    void ilValoreScelatoAManoNonSostituisceQuelloOriginale() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        //Già classificato come acquisto a 300 euro: l'originale che il programma aveva calcolato sta in [35]
        String[] d = deposito("2", "2024-03-10 10:05", "1", "300.00");
        d[35] = "100.00";
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.<String[]>of(d), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertEquals(Set.of(d[0]), ev.probabili());
    }

    @Test
    void senzaCandidatiNonSiEvidenziaNulla() {
        String[] p = prelievo("1", "2024-03-10 10:00", "1", "100.00");
        var ev = GUI_ClassificazioneMovimento.ValutaCandidati(p, List.of(), true,
                GUI_ClassificazioneMovimento.FINESTRA_TRASFERIMENTO_MS);
        assertTrue(ev.probabili().isEmpty());
        assertTrue(ev.valoriVicini().isEmpty());
    }
}
