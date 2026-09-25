package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Processo Node persistente per i prezzi degli exchange: {@code Historical_Multi_Eur.js --servizio},
 * che resta vivo e serve un lotto di richieste per riga invece di nascere e morire a ogni lotto.
 *
 * <p><b>Perche' esiste.</b> Ogni invocazione dello script pagava ~1,5 s fissi ({@code require('ccxt')},
 * costruzione delle istanze, lettura dei markets) anche per scaricare un'ora sola, ed e' proprio il caso
 * delle richieste singole di {@link Prezzi#CambioXXXEUR}: centinaia di processi in un'importazione. A
 * caldo una richiesta costa 0,3-0,4 s (misurato il 2026-09-25, output identico alla modalita' lotto).
 *
 * <p><b>Vita del processo.</b> Parte alla prima richiesta, si chiude dopo {@link #INATTIVITA_MAX_MS} senza
 * richieste (a regime tiene ~260 MB), e alla chiusura dell'applicazione. Chiudere significa chiudere il
 * suo stdin: lo script termina da solo quando lo vede chiuso, quindi anche se la JVM muore senza passare
 * dal gancio di chiusura non restano processi Node orfani.
 *
 * <p><b>Errori.</b> Tre esiti distinti, perche' il chiamante li tratta diversamente:
 * <ul>
 * <li>risposta valida: l'array degli esiti, identico a quello della modalita' {@code --lotto};</li>
 * <li>{@code null}: il lotto e' fallito (timeout, errore dello script, processo morto a meta' dopo aver
 *     gia' servito altri lotti) — stesso trattamento di un processo singolo fallito, cioe' nessuna ora
 *     marcata. Il processo viene chiuso e la richiesta successiva ne avvia uno nuovo;</li>
 * <li>{@link NonDisponibile}: il servizio non e' riuscito a partire o e' morto prima di rispondere a
 *     qualunque cosa. Il chiamante ripiega sul processo singolo di sempre, cosi' un problema del solo
 *     servizio non lascia l'utente senza prezzi.</li>
 * </ul>
 *
 * <p><b>Un lotto alla volta, ma nessuno aspetta.</b> Lo script serve i messaggi in fila; se un secondo
 * thread chiede prezzi mentre un lotto e' in corso (fino a 100 richieste, decine di secondi) riceve
 * {@link NonDisponibile} e usa il processo singolo — cioe' esattamente il comportamento di prima, quando
 * ogni chiamante aveva il suo processo. Farlo aspettare avrebbe potuto bloccare la GUI dietro a un
 * pre-scarico.
 */
final class ServizioNodePrezzi {

    /** Dopo quanto tempo senza richieste il processo si chiude (scelta dell'utente, 2026-09-25). */
    static final long INATTIVITA_MAX_MS = TimeUnit.MINUTES.toMillis(10);

    /** Riga messa in coda dal lettore di stdout quando il processo chiude l'uscita (e' morto). Si confronta
     *  per identita' ({@code ==}), quindi nessuna riga letta dallo script puo' essere scambiata per lei. */
    @SuppressWarnings("StringOperationCanBeSimplified")
    private static final String FINE_USCITA = new String("FINE-USCITA");

    /** Il servizio non e' partito, o e' morto prima di rispondere: il chiamante usi il processo singolo. */
    static final class NonDisponibile extends Exception {
        NonDisponibile(String messaggio) {
            super(messaggio);
        }
    }

    /** Un processo del servizio con i suoi canali; se ne crea uno nuovo a ogni avvio. */
    private static final class Istanza {
        final Process processo;
        /** node e script con cui e' stato avviato: se il chiamante ne passa altri, si riavvia. */
        final String avviatoCon;
        final BufferedWriter ingresso;
        final LinkedBlockingQueue<String> uscite = new LinkedBlockingQueue<>();
        int risposte;

        Istanza(Process processo, String avviatoCon) {
            this.processo = processo;
            this.avviatoCon = avviatoCon;
            this.ingresso = new BufferedWriter(new OutputStreamWriter(processo.getOutputStream(), StandardCharsets.UTF_8));
        }
    }

    /** volatile: il gancio di chiusura della JVM lo legge senza prendere il lock, che una richiesta in
     *  corso puo' tenere per minuti. */
    private static volatile Istanza corrente;
    private static long ultimoUso;
    private static long prossimoId;
    private static ScheduledExecutorService sorveglianza;
    /** Protegge tutto lo stato qui sopra (salvo {@link #corrente}, letto anche senza, vedi sopra). */
    private static final ReentrantLock LOCK = new ReentrantLock();

    private ServizioNodePrezzi() {
    }

    /**
     * Serve un lotto di richieste nel processo persistente, avviandolo se serve.
     *
     * @param nodePath eseguibile node
     * @param scriptPath {@code Historical_Multi_Eur.js}
     * @param exchanges id ccxt separati da virgola
     * @param richieste lo stesso array JSON della modalita' {@code --lotto}
     * @param tutti {@code true} = tutti gli exchange in parallelo, senza cascata (il pulsante "Riscarica")
     * @param timeoutMs attesa massima della risposta
     * @return gli esiti (array parallelo a {@code richieste}), oppure {@code null} se il lotto e' fallito
     * @throws NonDisponibile se il servizio non puo' rispondere: il chiamante ripieghi sul processo singolo
     */
    static JsonArray Lotto(Path nodePath, Path scriptPath, String exchanges, JsonArray richieste,
            long timeoutMs) throws NonDisponibile {
        return Lotto(nodePath, scriptPath, exchanges, richieste, false, timeoutMs);
    }

    /** Come {@link #Lotto(Path, Path, String, JsonArray, long)}, scegliendo se usare la cascata. */
    static JsonArray Lotto(Path nodePath, Path scriptPath, String exchanges, JsonArray richieste,
            boolean tutti, long timeoutMs) throws NonDisponibile {
        if (!LOCK.tryLock()) throw new NonDisponibile("occupato da un altro lotto");
        try {
            Istanza ist = istanzaViva(nodePath, scriptPath);
            long id = ++prossimoId;
            JsonObject messaggio = new JsonObject();
            messaggio.addProperty("id", id);
            messaggio.addProperty("exchanges", exchanges);
            messaggio.addProperty("timeframe", "1m");
            messaggio.addProperty("tutti", tutti);
            messaggio.add("richieste", richieste);
            try {
                ist.ingresso.write(messaggio.toString());
                ist.ingresso.write('\n');
                ist.ingresso.flush();
            } catch (IOException ex) {
                //Il processo e' morto fra un lotto e l'altro senza che ce ne accorgessimo: si riparte una
                //volta sola, poi si lascia al processo singolo.
                chiudi("processo non piu' raggiungibile");
                ist = istanzaViva(nodePath, scriptPath);
                try {
                    ist.ingresso.write(messaggio.toString());
                    ist.ingresso.write('\n');
                    ist.ingresso.flush();
                } catch (IOException ex2) {
                    chiudi("scrittura fallita");
                    throw new NonDisponibile("scrittura verso il servizio fallita: " + ex2.getMessage());
                }
            }
            return attendiRisposta(ist, id, timeoutMs);
        } finally {
            ultimoUso = System.currentTimeMillis();
            LOCK.unlock();
        }
    }

    private static JsonArray attendiRisposta(Istanza ist, long id, long timeoutMs) throws NonDisponibile {
        long scadenza = System.currentTimeMillis() + timeoutMs;
        while (true) {
            long resta = scadenza - System.currentTimeMillis();
            String riga;
            try {
                riga = resta > 0 ? ist.uscite.poll(resta, TimeUnit.MILLISECONDS) : null;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                chiudi("attesa interrotta");
                return null;
            }
            if (riga == null) {
                System.err.println("Servizio prezzi: nessuna risposta entro " + (timeoutMs / 1000)
                        + " s (probabile problema di connessione), chiudo il processo");
                chiudi("timeout");
                return null;
            }
            if (riga == FINE_USCITA) {
                boolean maiRisposto = ist.risposte == 0;
                chiudi("processo terminato");
                if (maiRisposto) {
                    throw new NonDisponibile("il processo e' terminato prima di rispondere");
                }
                System.err.println("Servizio prezzi: processo terminato durante il lotto " + id);
                return null;
            }
            JsonObject risposta;
            try {
                JsonElement el = JsonParser.parseString(riga);
                if (!el.isJsonObject()) throw new IllegalStateException("non e' un oggetto");
                risposta = el.getAsJsonObject();
            } catch (RuntimeException ex) {
                //stdout e' riservato alle risposte, ma una libreria potrebbe scriverci comunque: la riga
                //finisce nel log invece di far fallire il lotto.
                System.out.println("[NODE] " + riga);
                continue;
            }
            if (!risposta.has("id") || risposta.get("id").isJsonNull() || risposta.get("id").getAsLong() != id) {
                System.out.println("[NODE] risposta inattesa ignorata: "
                        + (riga.length() > 200 ? riga.substring(0, 200) + "..." : riga));
                continue;
            }
            ist.risposte++;
            if (risposta.has("errore")) {
                System.err.println("Servizio prezzi: lotto " + id + " fallito: " + risposta.get("errore").getAsString());
                return null;
            }
            if (!risposta.has("esiti") || !risposta.get("esiti").isJsonArray()) {
                System.err.println("Servizio prezzi: risposta al lotto " + id + " senza esiti");
                return null;
            }
            return risposta.getAsJsonArray("esiti");
        }
    }

    /** Il processo corrente se e' vivo, altrimenti uno nuovo. */
    private static Istanza istanzaViva(Path nodePath, Path scriptPath) throws NonDisponibile {
        String avviatoCon = nodePath.toAbsolutePath() + "|" + scriptPath.toAbsolutePath();
        Istanza ist = corrente;
        if (ist != null && ist.processo.isAlive() && ist.avviatoCon.equals(avviatoCon)) return ist;
        if (ist != null) chiudi(ist.processo.isAlive() ? "cambiati node o script" : "processo non piu' vivo");

        ProcessBuilder pb = ProcessoScript(nodePath, scriptPath, List.of("--servizio"));
        Process processo;
        try {
            processo = pb.start();
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
            throw new NonDisponibile("avvio del processo fallito: " + ex.getMessage());
        }
        Istanza nuova = new Istanza(processo, avviatoCon);
        avviaLettori(nuova);
        corrente = nuova;
        avviaSorveglianza();
        System.out.println("Servizio prezzi: avviato processo Node persistente (pid " + processo.pid() + ")");
        return nuova;
    }

    private static void avviaLettori(Istanza ist) {
        Thread uscita = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(ist.processo.getInputStream(), StandardCharsets.UTF_8))) {
                String riga;
                while ((riga = r.readLine()) != null) ist.uscite.add(riga);
            } catch (IOException ignorata) {
                //stream chiuso da chiudi(): e' la fine normale
            } finally {
                ist.uscite.add(FINE_USCITA);
            }
        }, "servizio-prezzi-stdout");
        uscita.setDaemon(true);
        uscita.start();

        //stderr va letto di continuo e su un thread suo: se si riempie il buffer della pipe lo script si
        //blocca scrivendo un log, e con lui la risposta attesa su stdout.
        Thread errori = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(ist.processo.getErrorStream(), StandardCharsets.UTF_8))) {
                String riga;
                while ((riga = r.readLine()) != null) System.out.println("[NODE] " + riga);
            } catch (IOException ignorata) {
                //stream chiuso da chiudi()
            }
        }, "servizio-prezzi-stderr");
        errori.setDaemon(true);
        errori.start();
    }

    /** Un solo thread demone per tutta la sessione: controlla l'inattivita' e, alla chiusura
     *  dell'applicazione, spegne il processo. */
    private static void avviaSorveglianza() {
        if (sorveglianza != null) return;
        sorveglianza = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "servizio-prezzi-inattivita");
            t.setDaemon(true);
            return t;
        });
        sorveglianza.scheduleWithFixedDelay(ServizioNodePrezzi::ChiudiSeInattivo, 1, 1, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //Senza lock: una richiesta in corso lo terrebbe, e la chiusura dell'app non deve aspettarla.
            Istanza ist = corrente;
            if (ist != null) termina(ist);
        }, "servizio-prezzi-chiusura"));
    }

    private static void ChiudiSeInattivo() {
        ChiudiSeInattivo(System.currentTimeMillis());
    }

    /** Chiude il processo se all'istante {@code adesso} e' inattivo da piu' di {@link #INATTIVITA_MAX_MS}.
     *  L'istante e' un parametro solo perche' i test non debbano aspettare dieci minuti veri. */
    static void ChiudiSeInattivo(long adesso) {
        //Occupato vuol dire non inattivo: si riguarda al prossimo giro invece di aspettare il lotto.
        if (!LOCK.tryLock()) return;
        try {
            if (corrente != null && adesso - ultimoUso > INATTIVITA_MAX_MS) {
                chiudi("inattivo da " + TimeUnit.MILLISECONDS.toMinutes(INATTIVITA_MAX_MS) + " minuti");
            }
        } finally {
            LOCK.unlock();
        }
    }

    /** Il pid del processo attivo, o -1 se il servizio e' spento. */
    static long PidAttivo() {
        Istanza ist = corrente;
        return ist != null && ist.processo.isAlive() ? ist.processo.pid() : -1;
    }

    /** Chiude il servizio, se attivo. La richiesta successiva ne avvia uno nuovo. */
    static void Chiudi() {
        LOCK.lock();
        try {
            chiudi("richiesta di chiusura");
        } finally {
            LOCK.unlock();
        }
    }

    private static void chiudi(String motivo) {
        Istanza ist = corrente;
        corrente = null;
        if (ist == null) return;
        System.out.println("Servizio prezzi: chiudo il processo Node (" + motivo + ")");
        termina(ist);
    }

    /** Chiude stdin (lo script esce da solo) e, se non basta entro 2 s, termina il processo e i suoi figli. */
    private static void termina(Istanza ist) {
        try {
            ist.ingresso.close();
        } catch (IOException ignorata) {
            //gia' chiuso o processo gia' morto
        }
        try {
            if (ist.processo.waitFor(2, TimeUnit.SECONDS)) return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        ist.processo.descendants().forEach(ProcessHandle::destroyForcibly);
        ist.processo.destroyForcibly();
    }

    /**
     * Il {@link ProcessBuilder} per lanciare {@code Historical_Multi_Eur.js} con {@code argomenti}: cartella
     * dello script e {@code NODE_PATH} verso i moduli installati da {@link CcxtInterop}. Condiviso dal
     * servizio e dal processo singolo del percorso a lotti.
     */
    static ProcessBuilder ProcessoScript(Path nodePath, Path scriptPath, List<String> argomenti) {
        List<String> comando = new java.util.ArrayList<>();
        comando.add(nodePath.toString());
        comando.add(scriptPath.toAbsolutePath().toString());
        comando.addAll(argomenti);
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.directory(scriptPath.getParent().toFile());
        Map<String, String> env = pb.environment();
        String nodeModules = CcxtInterop.getNodeDir().resolve("node_modules").toAbsolutePath().toString();
        String esistente = env.get("NODE_PATH");
        env.put("NODE_PATH", esistente == null || esistente.isEmpty() ? nodeModules
                : nodeModules + File.pathSeparator + esistente);
        return pb;
    }
}
