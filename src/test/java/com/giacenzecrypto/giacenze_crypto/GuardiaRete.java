package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Estensione applicata a <b>tutte</b> le classi di test (autodetection, come {@link IsolamentoConnessioniH2}):
 * i test non vanno in rete, a meno che la classe lo dichiari con {@code @Tag("rete")}.
 *
 * <p>Fino al 2026-10-15 cinque classi di importazione (Nexo, Nexo Pro, KuCoin, Gate.io, Dual Investment)
 * lasciavano un movimento senza prezzo e la valorizzazione andava a cercarlo: in una {@code @TempDir} vuota
 * voleva dire scaricare Node, installare ccxt, scaricare la mappa dei token CoinMarketCap e chiedere i
 * prezzi. Circa 85 dei 150 secondi della suite, e una suite che senza rete falliva o restava appesa.
 *
 * <p>Due difese, perche' una sola non basta:
 * <ul>
 *   <li>{@link Funzioni#ReteDisabilitataPerTest} acceso prima di ogni classe non marcata: il controllo di
 *       connessione risponde "assente" e la valorizzazione ripiega senza provarci. E' la via ordinaria.</li>
 *   <li>un {@link ProxySelector} di default che intercetta ogni connessione verso un host non locale
 *       aperta da OkHttp, {@code HttpURLConnection} o {@code java.net.http}, cioe' i percorsi che non
 *       passano dal controllo di connessione. La registra e la manda su un proxy morto (rifiuto immediato,
 *       una normale {@code IOException} per il codice). Il test fallisce in {@code afterEach}: un'eccezione
 *       lanciata dal selettore verrebbe inghiottita dai {@code catch (Exception)} del programma.</li>
 * </ul>
 * In piu' un thread guarda i processi figli ogni {@value #PASSO_PROCESSI_MS} ms: Node che esegue uno script
 * di {@code Scripts/} (ccxt, prezzi) o {@code npm install} va in rete fuori dalla JVM, dove il selettore non
 * arriva. Gli script finti di una {@code @TempDir} ({@code ServizioNodePrezziTest}) non contano.
 *
 * <p>Il selettore si installa nell'inizializzatore statico, cioe' quando JUnit carica le estensioni, prima
 * di ogni classe del programma: i client OkHttp {@code static final} leggono
 * {@code ProxySelector.getDefault()} quando vengono costruiti, e uno costruito prima lo ignorerebbe.
 *
 * <p>{@code -DguardiaRete=registra} lascia passare le connessioni e si limita a stamparle (righe
 * {@code GUARDIA-RETE}), per censire chi va in rete. {@code ./mvnw test -DexcludedGroups=rete} esegue la
 * suite senza le classi che la rete la provano di proposito.
 */
public class GuardiaRete implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    static final String TAG = "rete";

    private static final boolean SOLO_REGISTRA = "registra".equalsIgnoreCase(System.getProperty("guardiaRete"));
    private static final ProxySelector ORIGINALE = ProxySelector.getDefault();
    private static final Proxy PROXY_MORTO = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 9));
    /** Per i socket nudi ({@code socket://}) il JDK ignora un proxy che non sia SOCKS e si collegherebbe davvero. */
    private static final Proxy SOCKS_MORTO = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9));

    private static volatile boolean permessa = true;
    private static volatile String chi = "(fuori dai test)";
    private static final List<String> tentativi = new ArrayList<>();
    private static final long PASSO_PROCESSI_MS = 50;
    private static final String CARTELLA_SCRIPT = Paths.get(System.getProperty("user.dir"), "Scripts")
            .toAbsolutePath().normalize().toString();
    /** Processi gia' visti, per non attribuire a una classe il servizio Node avviato da una precedente. */
    private static final Set<Long> processiVisti = new HashSet<>();

    static {
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if (permessa || Locale(uri)) return ORIGINALE.select(uri);
                synchronized (tentativi) {
                    tentativi.add(chi + " -> " + uri.getScheme() + "://" + uri.getHost()
                            + (uri.getPath() == null ? "" : uri.getPath()));
                }
                if (SOLO_REGISTRA) return ORIGINALE.select(uri);
                return List.of("socket".equalsIgnoreCase(uri.getScheme()) ? SOCKS_MORTO : PROXY_MORTO);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                if (permessa || Locale(uri)) ORIGINALE.connectFailed(uri, sa, ioe);
            }
        });
        Thread sorvegliante = new Thread(() -> {
            while (true) {
                GuardaProcessi();
                try {
                    Thread.sleep(PASSO_PROCESSI_MS);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }, "GuardiaRete-processi");
        sorvegliante.setDaemon(true);
        sorvegliante.start();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        permessa = context.getTags().contains(TAG);
        Funzioni.ReteDisabilitataPerTest = !permessa;
        chi = context.getDisplayName();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        chi = context.getRequiredTestClass().getSimpleName() + "#" + context.getRequiredTestMethod().getName();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Verifica(context.getRequiredTestClass().getSimpleName() + "#" + context.getRequiredTestMethod().getName());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        //Anche @BeforeAll e @AfterAll della classe possono andare in rete
        GuardaProcessi();
        Verifica(context.getDisplayName());
        permessa = true;
        Funzioni.ReteDisabilitataPerTest = false;
        chi = "(fuori dai test)";
    }

    private static void Verifica(String dove) {
        List<String> copia;
        synchronized (tentativi) {
            copia = List.copyOf(tentativi);
            tentativi.clear();
        }
        if (!copia.isEmpty()) {
            Segnala(dove + ": " + copia.size() + " connessioni in rete senza @Tag(\"" + TAG + "\"): "
                    + copia.stream().distinct().collect(Collectors.joining(", ")));
        }
    }

    private static void Segnala(String messaggio) {
        System.out.println("GUARDIA-RETE " + messaggio);
        if (!SOLO_REGISTRA) {
            throw new AssertionError(messaggio + ". Dare ai movimenti un controvalore o un prezzo in cache,"
                    + " oppure marcare la classe con @Tag(\"" + TAG + "\") se la rete e' cio' che prova.");
        }
    }

    private static synchronized void GuardaProcessi() {
        for (ProcessHandle p : ProcessHandle.current().descendants().toList()) {
            if (!processiVisti.add(p.pid()) || permessa) continue;
            ProcessHandle.Info info = p.info();
            String comando = info.command().orElse("");
            if (!comando.contains("node") && !comando.contains("npm")) continue;
            String argomenti = String.join(" ", info.arguments().orElse(new String[0]));
            if (argomenti.contains(CARTELLA_SCRIPT) || argomenti.contains("install")) {
                synchronized (tentativi) {
                    tentativi.add(chi + " -> processo " + Paths.get(comando).getFileName() + " " + argomenti);
                }
            }
        }
    }

    private static boolean Locale(URI uri) {
        String host = uri.getHost();
        if (host == null) return true;
        if (host.equalsIgnoreCase("localhost")) return true;
        try {
            //Solo letterali: risolvere un nome qui vorrebbe dire andare in rete per decidere se andarci
            if (!host.matches("[0-9.]+|\\[?[0-9a-fA-F:]+\\]?")) return false;
            InetAddress a = InetAddress.getByName(host.replace("[", "").replace("]", ""));
            return a.isLoopbackAddress() || a.isAnyLocalAddress();
        } catch (IOException ex) {
            return false;
        }
    }
}
