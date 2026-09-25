package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La gestione del processo Node persistente ({@link ServizioNodePrezzi}), con script finti al posto di
 * {@code Historical_Multi_Eur.js}: nessuna rete, nessun database, esiti deterministici. Che il servizio
 * vero dia gli stessi prezzi della modalita' lotto lo verifica {@code PrezziLottoCCXTTest}; qui si
 * verifica cio' che quel test non puo' provocare — un processo che muore, che non risponde, che
 * risponde male, che resta inattivo.
 *
 * <p>Serve un eseguibile Node vero: lo si cerca negli archivi di prova sotto {@code test/}, e la classe
 * si auto-salta se non c'e', come {@code PrezziLottoCCXTTest}.
 */
class ServizioNodePrezziTest {

    @TempDir
    static Path cartella;

    private static Path node;

    /** Risponde a ogni riga con il proprio pid; prima scrive una riga non JSON e una risposta con un
     *  id sbagliato, che il servizio deve ignorare. */
    private static final String ECO = """
            const rl = require('readline').createInterface({ input: process.stdin, terminal: false });
            rl.on('line', riga => {
                const msg = JSON.parse(riga);
                process.stdout.write('rumore non JSON\\n');
                process.stdout.write(JSON.stringify({ id: -1, esiti: [] }) + '\\n');
                process.stdout.write(JSON.stringify({ id: msg.id, esiti: [{ pid: process.pid, n: msg.richieste.length }] }) + '\\n');
            });
            rl.on('close', () => process.exit(0));
            """;

    @BeforeAll
    static void trovaNode() throws IOException {
        Path radice = Path.of(System.getProperty("user.dir"), "test");
        Optional<Path> trovato = Optional.empty();
        //Gli archivi contengono sia la distribuzione Linux sia quella Windows: va presa quella del sistema
        //su cui gira il test, altrimenti il processo parte e muore subito.
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path eseguibile = windows ? Path.of("node.exe") : Path.of("bin", "node");
        if (Files.isDirectory(radice)) {
            try (Stream<Path> s = Files.find(radice, 6, (p, a) -> a.isRegularFile()
                    && p.endsWith(eseguibile) && p.toString().contains("tools"))) {
                trovato = s.filter(Files::isExecutable).findFirst();
            }
        }
        Assumptions.assumeTrue(trovato.isPresent(), "Node non presente negli archivi di prova: classe saltata");
        node = trovato.get();
    }

    @AfterEach
    void spegne() {
        ServizioNodePrezzi.Chiudi();
    }

    private static Path script(String nome, String corpo) throws IOException {
        Path p = cartella.resolve(nome);
        Files.writeString(p, corpo);
        return p;
    }

    private static JsonArray richieste(int n) {
        JsonArray a = new JsonArray();
        for (int i = 0; i < n; i++) a.add(i);
        return a;
    }

    private static long pidDi(JsonArray esiti) {
        return esiti.get(0).getAsJsonObject().get("pid").getAsLong();
    }

    @Test
    void piuLottiUsanoLoStessoProcessoEIgnoranoLeRigheEstranee() throws Exception {
        Path eco = script("eco.js", ECO);
        JsonArray primo = ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(3), 10_000);
        JsonArray secondo = ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(1), 10_000);
        assertNotNull(primo);
        assertNotNull(secondo);
        assertEquals(3, primo.get(0).getAsJsonObject().get("n").getAsInt());
        assertEquals(pidDi(primo), pidDi(secondo), "il secondo lotto deve riusare il processo del primo");
    }

    @Test
    void unProcessoMortoFraDueLottiVieneRiavviato() throws Exception {
        Path eco = script("eco.js", ECO);
        long primo = pidDi(ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(1), 10_000));
        ProcessHandle ph = ProcessHandle.of(primo).orElseThrow();
        ph.destroyForcibly();
        ph.onExit().get(5, TimeUnit.SECONDS);

        JsonArray dopo = ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(1), 10_000);
        assertNotNull(dopo, "dopo la morte del processo il lotto successivo deve riuscire");
        assertNotEquals(primo, pidDi(dopo));
    }

    @Test
    void unServizioCheMuoreSubitoNonEDisponibile() throws Exception {
        Path muore = script("muore.js", "process.exit(3);\n");
        assertThrows(ServizioNodePrezzi.NonDisponibile.class,
                () -> ServizioNodePrezzi.Lotto(node, muore, "binance", richieste(1), 10_000),
                "senza nessuna risposta il chiamante deve poter ripiegare sul processo singolo");
        assertEquals(-1, ServizioNodePrezzi.PidAttivo());
    }

    @Test
    void unServizioCheNonRispondeScadeEVieneChiuso() throws Exception {
        Path muto = script("muto.js", "process.stdin.resume();\n");
        long t = System.currentTimeMillis();
        assertNull(ServizioNodePrezzi.Lotto(node, muto, "binance", richieste(1), 1_500));
        assertTrue(System.currentTimeMillis() - t < 8_000, "il timeout deve essere rispettato");
        assertEquals(-1, ServizioNodePrezzi.PidAttivo(), "dopo un timeout il processo va chiuso");
    }

    @Test
    void unErroreDelloScriptFallisceIlLottoMaNonIlProcesso() throws Exception {
        Path errore = script("errore.js", """
                const rl = require('readline').createInterface({ input: process.stdin, terminal: false });
                rl.on('line', r => process.stdout.write(JSON.stringify({ id: JSON.parse(r).id, errore: 'prova' }) + '\\n'));
                rl.on('close', () => process.exit(0));
                """);
        assertNull(ServizioNodePrezzi.Lotto(node, errore, "binance", richieste(1), 10_000));
        assertNotEquals(-1, ServizioNodePrezzi.PidAttivo(), "un lotto fallito non e' un processo rotto");
    }

    @Test
    void dopoDieciMinutiDiInattivitaIlProcessoSiChiudeETermina() throws Exception {
        Path eco = script("eco.js", ECO);
        long pid = pidDi(ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(1), 10_000));
        long adesso = System.currentTimeMillis();

        ServizioNodePrezzi.ChiudiSeInattivo(adesso + TimeUnit.MINUTES.toMillis(9));
        assertEquals(pid, ServizioNodePrezzi.PidAttivo(), "a 9 minuti deve essere ancora vivo");

        ServizioNodePrezzi.ChiudiSeInattivo(adesso + TimeUnit.MINUTES.toMillis(11));
        assertEquals(-1, ServizioNodePrezzi.PidAttivo());
        //Chiudere stdin deve bastare a far uscire lo script: nessun processo orfano.
        Optional<ProcessHandle> ph = ProcessHandle.of(pid);
        if (ph.isPresent()) ph.get().onExit().get(5, TimeUnit.SECONDS);
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test
    void unSecondoChiamanteNonAspettaIlLottoInCorso() throws Exception {
        Path muto = script("muto.js", "process.stdin.resume();\n");
        Thread primo = new Thread(() -> {
            try {
                ServizioNodePrezzi.Lotto(node, muto, "binance", richieste(1), 3_000);
            } catch (ServizioNodePrezzi.NonDisponibile ignorata) {
            }
        });
        primo.start();
        //attende che il primo lotto sia davvero in volo (processo avviato)
        for (int i = 0; i < 100 && ServizioNodePrezzi.PidAttivo() == -1; i++) Thread.sleep(20);
        assertNotEquals(-1, ServizioNodePrezzi.PidAttivo());

        long t = System.currentTimeMillis();
        ServizioNodePrezzi.NonDisponibile nd = assertThrows(ServizioNodePrezzi.NonDisponibile.class,
                () -> ServizioNodePrezzi.Lotto(node, muto, "binance", richieste(1), 3_000));
        assertTrue(System.currentTimeMillis() - t < 500, "il secondo chiamante deve ripiegare subito");
        assertTrue(nd.getMessage().contains("occupato"));
        primo.join(10_000);
    }

    /** "Riscarica tutti i prezzi dalle fonti" chiede tutti gli exchange senza cascata: l'opzione deve
     *  arrivare allo script, e i lotti normali devono continuare a non chiederla. */
    @Test
    void lOpzioneTuttiArrivaAlloScript() throws Exception {
        Path tutti = script("tutti.js", """
                const rl = require('readline').createInterface({ input: process.stdin, terminal: false });
                rl.on('line', r => { const m = JSON.parse(r);
                    process.stdout.write(JSON.stringify({ id: m.id, esiti: [{ tutti: m.tutti }] }) + '\\n'); });
                rl.on('close', () => process.exit(0));
                """);
        JsonArray conTutti = ServizioNodePrezzi.Lotto(node, tutti, "binance", richieste(1), true, 10_000);
        JsonArray normale = ServizioNodePrezzi.Lotto(node, tutti, "binance", richieste(1), 10_000);
        assertTrue(conTutti.get(0).getAsJsonObject().get("tutti").getAsBoolean());
        assertFalse(normale.get(0).getAsJsonObject().get("tutti").getAsBoolean());
    }

    @Test
    void cambiareScriptRiavviaIlProcesso() throws Exception {
        Path eco = script("eco.js", ECO);
        Path eco2 = script("eco2.js", ECO);
        long primo = pidDi(ServizioNodePrezzi.Lotto(node, eco, "binance", richieste(1), 10_000));
        long secondo = pidDi(ServizioNodePrezzi.Lotto(node, eco2, "binance", richieste(1), 10_000));
        assertNotEquals(primo, secondo);
    }
}
