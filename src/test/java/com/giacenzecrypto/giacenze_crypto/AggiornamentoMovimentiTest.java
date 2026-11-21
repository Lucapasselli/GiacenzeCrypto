package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.giacenzecrypto.giacenze_crypto.AggiornamentoMovimenti.Innesco;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Il coordinatore che consuma {@code TabellaCryptodaAggiornare} anche senza evento di focus. Nessun Swing: l'ambiente e
 * l'orologio sono finti, quindi gira anche in headless.
 */
class AggiornamentoMovimentiTest {

    private static class Finto implements AggiornamentoMovimenti.Ambiente {
        boolean flag;
        String impedimento;
        int aggiornamenti;
        RuntimeException errore;
        boolean flagVistoDentro;

        @Override
        public boolean inSospeso() {
            return flag;
        }

        @Override
        public void abbassa() {
            flag = false;
        }

        @Override
        public String impedimento() {
            return impedimento;
        }

        @Override
        public void aggiorna() {
            aggiornamenti++;
            flagVistoDentro = flag;
            if (errore != null) {
                throw errore;
            }
        }
    }

    private Finto amb;
    private long t;
    private AggiornamentoMovimenti coord;

    @BeforeEach
    void prepara() {
        amb = new Finto();
        t = 1_000_000;
        coord = new AggiornamentoMovimenti(amb, () -> t);
    }

    @Test
    void senzaFlagNonFaNiente() {
        for (Innesco i : Innesco.values()) {
            assertFalse(coord.Prova(i));
        }
        coord.Tick();
        assertEquals(0, amb.aggiornamenti);
    }

    @Test
    void ilFocusConsumaSubitoESempreAncheConUnDialogoAperto() {
        amb.flag = true;
        amb.impedimento = "dialogo modale aperto";
        assertTrue(coord.Prova(Innesco.FOCUS), "il focus e' come prima: nessuna condizione");
        assertEquals(1, amb.aggiornamenti);
        assertFalse(amb.flag);
        assertFalse(amb.flagVistoDentro, "il flag va abbassato prima di ricalcolare: chi lo alza durante il ricalcolo non si perde");
    }

    @Test
    void ilTimerAspettaCheIlFlagSiaStabile() {
        amb.flag = true;
        coord.Tick();
        assertEquals(0, amb.aggiornamenti, "appena visto: potrebbe essere a meta' di un'operazione");
        t += 999;
        coord.Tick();
        assertEquals(0, amb.aggiornamenti);
        t += 1;
        coord.Tick();
        assertEquals(1, amb.aggiornamenti, "dopo ATTESA_MINIMA_MS senza impedimenti ricalcola, senza focus");
        assertFalse(amb.flag);
        t += 5000;
        coord.Tick();
        assertEquals(1, amb.aggiornamenti, "flag basso: niente ricalcoli ripetuti");
    }

    @Test
    void conUnImpedimentoNonRicalcolaMaAppenaSiLiberaSi() {
        amb.flag = true;
        amb.impedimento = "finestra di avanzamento aperta";
        for (int i = 0; i < 40; i++) {
            t += 500;
            coord.Tick();
        }
        assertEquals(0, amb.aggiornamenti, "20 s sotto un dialogo: mai un ricalcolo sotto l'import");
        amb.impedimento = null;
        assertTrue(coord.Prova(Innesco.CHIUSURA_DIALOGO), "alla chiusura del dialogo il flag e' in attesa da tempo: subito");
        assertEquals(1, amb.aggiornamenti);
    }

    @Test
    void laChiusuraDiUnDialogoDopoUnRialzoRecenteAspettaIlSecondo() {
        amb.flag = true;
        assertFalse(coord.Prova(Innesco.CHIUSURA_DIALOGO), "visto ora per la prima volta");
        t += 1000;
        assertTrue(coord.Prova(Innesco.CHIUSURA_DIALOGO));
    }

    @Test
    void unRialzoDopoIlRicalcoloRiparteDaCapo() {
        amb.flag = true;
        coord.Prova(Innesco.FOCUS);
        amb.flag = true;
        coord.Tick();
        assertEquals(1, amb.aggiornamenti, "il nuovo rialzo ha la sua attesa");
        t += 1000;
        coord.Tick();
        assertEquals(2, amb.aggiornamenti);
    }

    @Test
    void unaEccezioneNelRicalcoloNonLasciaIlFlagAltoNeMartellaIlTimer() {
        PrintStream vecchio = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        try {
            amb.errore = new IllegalStateException("prova");
            amb.flag = true;
            assertTrue(coord.Prova(Innesco.FOCUS), "non deve propagare l'eccezione al chiamante");
            assertFalse(amb.flag);
            for (int i = 0; i < 10; i++) {
                t += 500;
                coord.Tick();
            }
            assertEquals(1, amb.aggiornamenti);
            amb.errore = null;
            amb.flag = true;
            t += 1;
            coord.Tick();
            t += 1000;
            coord.Tick();
            assertEquals(2, amb.aggiornamenti, "dopo l'errore il coordinatore funziona ancora");
        } finally {
            System.setErr(vecchio);
        }
    }

    @Test
    void unRicalcoloGiaInCorsoNonSiRientra() {
        amb.flag = true;
        final boolean[] annidato = {false};
        Finto f = new Finto() {
            @Override
            public void aggiorna() {
                super.aggiorna();
                flag = true;
                annidato[0] = coord.Prova(Innesco.FOCUS);
            }
        };
        t = 1;
        coord = new AggiornamentoMovimenti(f, () -> t);
        f.flag = true;
        assertTrue(coord.Prova(Innesco.FOCUS));
        assertFalse(annidato[0], "un dialogo aperto durante il ricalcolo (loop annidato dell'EDT) non lo rilancia");
        assertEquals(1, f.aggiornamenti);
    }

    @Test
    void ilLogDiceChiHaConsumatoIlFlag() {
        PrintStream vecchio = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true));
        try {
            amb.flag = true;
            coord.Tick();
            t += 1000;
            coord.Tick();
        } finally {
            System.setOut(vecchio);
        }
        String log = buf.toString();
        assertTrue(log.contains("[Aggiornamento] in sospeso"), log);
        assertTrue(log.contains("eseguito da: timer"), log);
    }

    @Test
    void unBloccoLungoSiSegnalaNelLogConIlMotivo() {
        PrintStream vecchio = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true));
        try {
            amb.flag = true;
            amb.impedimento = "dialogo modale aperto (Prova)";
            for (int i = 0; i < 30; i++) {
                coord.Tick();
                t += 500;
            }
        } finally {
            System.setOut(vecchio);
        }
        assertTrue(buf.toString().contains("ANCORA in sospeso") && buf.toString().contains("dialogo modale aperto (Prova)"), buf.toString());
    }
}
