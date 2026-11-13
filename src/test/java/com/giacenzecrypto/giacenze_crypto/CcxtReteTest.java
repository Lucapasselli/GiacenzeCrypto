package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La correzione della connessione di ccxt ({@code Scripts/Ccxt_Rete.js}, caricata con {@code --require}):
 * ccxt 4.5.77 chiede 10 ms per ogni tentativo di connessione e un exchange con ping sopra ~10 ms
 * risponde "fetch failed:" (13/11/2026). Nessuna rete: un ccxt finto con lo stesso metodo
 * {@code getDispatcherOptions}. Che la connessione vera funzioni si e' provato a mano contro OKX, Binance,
 * KuCoin, Coinbase e Crypto.com.
 */
class CcxtReteTest {

    @TempDir
    Path cartella;

    // ---- le opzioni di Node messe da Java -------------------------------------------------------

    @Test
    void preloadVieneMessoPrimaDelloScriptESeNonEsisteNonSiAggiungeNulla() throws IOException {
        Path script = cartella.resolve("Prova.js");
        Files.writeString(script, "// vuoto");
        assertEquals(List.of(), ServizioNodePrezzi.OpzioniPreload(script));

        Path preload = cartella.resolve(ServizioNodePrezzi.SCRIPT_RETE_CCXT);
        Files.writeString(preload, "// vuoto");
        assertEquals(List.of("--require", preload.toAbsolutePath().toString()), ServizioNodePrezzi.OpzioniPreload(script));

        //Gli argomenti dello script sono posizionali: il preload sta fra node e lo script, mai dopo
        List<String> comando = ServizioNodePrezzi.ProcessoScript(Path.of("node"), script, List.of("a", "b")).command();
        assertEquals(List.of("node", "--require", preload.toAbsolutePath().toString(),
                script.toAbsolutePath().toString(), "a", "b"), comando);
    }

    @Test
    void loScriptRealeEsisteNellaCartellaScripts() {
        assertTrue(Files.isRegularFile(Path.of("Scripts", ServizioNodePrezzi.SCRIPT_RETE_CCXT)),
                "Scripts/Ccxt_Rete.js deve stare accanto agli altri script: viene distribuito con la cartella");
    }

    // ---- il comportamento dello script, con un ccxt finto ---------------------------------------

    @Test
    void alzaLAttesaDelTentativoESoloQuellaEResteraIdempotente() throws Exception {
        Path node = nodeDisponibile();
        String uscita = esegui(node, """
                require('%s');
                require('%s'); // caricata due volte: non deve incatenarsi
                const { Exchange } = require('ccxt');
                const lenta = new Exchange().getDispatcherOptions(true);
                console.log(JSON.stringify({ lenta, chiamate: global.__chiamate }));
                """.formatted(js(preload()), js(preload())), ccxtFintoCartella(10));
        assertTrue(uscita.contains("\"autoSelectFamilyAttemptTimeout\":250"), uscita);
        assertTrue(uscita.contains("\"keepAliveTimeout\":60000"), "le altre opzioni di ccxt restano: " + uscita);
        assertTrue(uscita.contains("\"chiamate\":1"), "il metodo originale gira una volta sola, anche caricando due volte: " + uscita);
    }

    @Test
    void unValoreGiaAlto_NonSiTocca() throws Exception {
        Path node = nodeDisponibile();
        String uscita = esegui(node, """
                require('%s');
                const { Exchange } = require('ccxt');
                console.log(JSON.stringify(new Exchange().getDispatcherOptions(true)));
                """.formatted(js(preload())), ccxtFintoCartella(5000));
        assertTrue(uscita.contains("\"autoSelectFamilyAttemptTimeout\":5000"), uscita);
    }

    @Test
    void senzaCcxtOCcxtDiversoNonRompeIlProcesso() throws Exception {
        Path node = nodeDisponibile();
        //Nessun ccxt nel NODE_PATH: la prima installazione avviene dopo, lo script deve solo tacere
        String uscita = esegui(node, "require('" + js(preload()) + "'); console.log('vivo');", null);
        assertEquals("vivo", uscita.trim());

        //ccxt senza il metodo (versione futura): idem
        Path altro = cartella.resolve("altro");
        Files.createDirectories(altro.resolve("ccxt"));
        Files.writeString(altro.resolve("ccxt").resolve("index.js"), "module.exports = { Exchange: class {} };");
        String uscita2 = esegui(node, "require('" + js(preload()) + "'); console.log('vivo2');", altro);
        assertEquals("vivo2", uscita2.trim());
    }

    // ---- supporto -------------------------------------------------------------------------------

    private static Path preload() {
        return Path.of("Scripts", ServizioNodePrezzi.SCRIPT_RETE_CCXT).toAbsolutePath();
    }

    /** Percorso scritto in un letterale JS (anche con backslash di Windows). */
    private static String js(Path p) {
        return p.toString().replace("\\", "\\\\").replace("'", "\\'");
    }

    private Path ccxtFintoCartella(int attesa) throws IOException {
        Path modules = cartella.resolve("modules" + attesa);
        Files.createDirectories(modules.resolve("ccxt"));
        //Come ccxt 4.5.77: il metodo sta sul prototipo e ogni chiamata costruisce un oggetto nuovo
        Files.writeString(modules.resolve("ccxt").resolve("index.js"), """
                global.__chiamate = 0;
                class Exchange {
                    getDispatcherOptions(isPlainAgent = false) {
                        global.__chiamate++;
                        return { keepAliveTimeout: 60000, autoSelectFamily: true, autoSelectFamilyAttemptTimeout: %d };
                    }
                }
                module.exports = { Exchange };
                """.formatted(attesa), StandardCharsets.UTF_8);
        return modules;
    }

    private String esegui(Path node, String codice, Path nodeModules) throws Exception {
        Path file = Files.createTempFile(cartella, "prova", ".js");
        Files.writeString(file, codice, StandardCharsets.UTF_8);
        ProcessBuilder pb = new ProcessBuilder(node.toString(), file.toString());
        if (nodeModules != null) pb.environment().put("NODE_PATH", nodeModules.toString());
        else pb.environment().remove("NODE_PATH");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String uscita = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(30, TimeUnit.SECONDS), "node non ha finito");
        assertEquals(0, p.exitValue(), uscita);
        return uscita;
    }

    /** Un Node vero: quello degli archivi di prova sotto {@code test/tools}, altrimenti quello del PATH. */
    private static Path nodeDisponibile() {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path radice = Path.of(System.getProperty("user.dir"), "test");
        if (Files.isDirectory(radice)) {
            try (var s = Files.find(radice, 6, (p, a) -> a.isRegularFile()
                    && p.endsWith(windows ? Path.of("node.exe") : Path.of("bin", "node")) && p.toString().contains("tools"))) {
                var trovato = s.filter(Files::isExecutable).findFirst();
                if (trovato.isPresent()) return trovato.get();
            } catch (IOException ignorato) {
                //si passa al PATH
            }
        }
        try {
            Process p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            if (p.waitFor(15, TimeUnit.SECONDS) && p.exitValue() == 0) return Path.of("node");
        } catch (Exception ignorato) {
            //nessun node
        }
        Assumptions.assumeTrue(false, "nessun eseguibile Node disponibile");
        return null;
    }
}
