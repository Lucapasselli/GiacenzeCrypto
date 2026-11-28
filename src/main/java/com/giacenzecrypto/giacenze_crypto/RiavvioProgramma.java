package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Riavvio del programma su richiesta dell'utente (oggi: dopo il cambio di tema).
 *
 * <p>Il nuovo processo parte <b>prima</b> che il vecchio finisca di chiudersi e aspetta la sua fine
 * ({@value #ARGOMENTO_ATTESA} {@code <pid>}, letto all'inizio di {@code Giacenze_Crypto.main}). Non si poteva fare
 * al contrario: la chiusura compatta i database e dopo {@code System.exit} nessuno lancia più niente, mentre un nuovo
 * processo avviato troppo presto troverebbe {@code database.mv.db} ancora bloccato da H2 e si fermerebbe con
 * "è già aperta un'altra sessione". Aspettare la fine del processo evita sia uno script esterno (diverso su Windows e
 * Linux, come quello dell'aggiornamento) sia un ritardo fisso a caso.</p>
 *
 * <p>Il comando è quello con cui il programma è partito: il lanciatore jpackage da solo (rilegge da sé gli argomenti del
 * suo {@code .cfg}, come per l'aggiornamento automatico), oppure {@code java} con le stesse opzioni, il jar o le
 * classi e gli argomenti di avvio. Flatpak e Microsoft Store non si riavviano da qui: nel primo il processo figlio
 * muore con la sandbox, nel secondo lanciare direttamente l'eseguibile fa perdere l'identità del pacchetto.</p>
 */
public final class RiavvioProgramma {

    private RiavvioProgramma() {}

    /** Argomento che dice al nuovo processo di aspettare la fine del vecchio (il pid segue). */
    public static final String ARGOMENTO_ATTESA = "--attendiPid";

    /** Quanto aspetta al massimo il nuovo processo: la chiusura compatta i database, può richiedere qualche decina di secondi. */
    static final long ATTESA_MASSIMA_SECONDI = 120;

    /**
     * @return {@code true} se questa installazione può riavviarsi da sola. {@code false} per flatpak e Microsoft Store,
     *         dove il chiamante deve chiedere all'utente di riaprire il programma a mano.
     */
    public static boolean Disponibile() {
        AggiornamentoInstallazione.Tipo tipo = AggiornamentoInstallazione.Rileva().Tipo();
        return tipo != AggiornamentoInstallazione.Tipo.FLATPAK && tipo != AggiornamentoInstallazione.Tipo.STORE;
    }

    /**
     * Avvia il nuovo processo, che resta in attesa della fine di questo. Da chiamare dentro la chiusura ordinata del
     * programma (dopo i salvataggi): se lancia un'eccezione non è partito niente e il programma può restare aperto.
     */
    public static void Avvia() throws IOException {
        List<String> comando = Comando(System.getProperty("jpackage.app-path"), AggiornamentoInstallazione.PercorsoJar(),
                System.getProperty("java.home"), System.getProperty("os.name", ""),
                ManagementFactory.getRuntimeMXBean().getInputArguments(), VarStatiche.ArgomentiAvvio,
                Giacenze_Crypto.class.getName(), ProcessHandle.current().pid());
        System.out.println("Riavvio: " + String.join(" ", comando));
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.directory(Path.of(System.getProperty("user.dir")).toFile());
        //Staccato dal programma che si chiude: niente output da leggere, e non deve riempire un buffer
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectInput(ProcessBuilder.Redirect.from(nullDevice()));
        pb.start();
    }

    private static java.io.File nullDevice() {
        return new java.io.File(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                ? "NUL" : "/dev/null");
    }

    /**
     * Il comando di riavvio, funzione pura (provabile senza lanciare niente).
     *
     * @param AppPath proprietà {@code jpackage.app-path}, impostata dal lanciatore jpackage, o {@code null}
     * @param Jar il jar da cui gira il programma, o la cartella delle classi
     * @param JavaHome {@code java.home}
     * @param Os {@code os.name}
     * @param OpzioniJvm le opzioni con cui è partita la JVM (senza gli argomenti del programma)
     * @param ArgomentiAvvio gli argomenti del programma, senza {@value #ARGOMENTO_ATTESA}
     * @param ClassePrincipale la classe con il {@code main}, usata solo se il programma non gira da un jar
     * @param Pid il processo da aspettare
     */
    static List<String> Comando(String AppPath, Path Jar, String JavaHome, String Os, List<String> OpzioniJvm,
            String[] ArgomentiAvvio, String ClassePrincipale, long Pid) {
        List<String> c = new ArrayList<>();
        if (AppPath != null && !AppPath.isBlank()) {
            //Il lanciatore jpackage rilegge da sé gli argomenti del suo .cfg (--workdir, --risorse)
            c.add(AppPath);
        } else {
            boolean windows = Os != null && Os.toLowerCase(Locale.ROOT).startsWith("windows");
            c.add(Path.of(JavaHome, "bin", windows ? "javaw.exe" : "java").toString());
            for (int i = 0; i < OpzioniJvm.size(); i++) {
                String a = OpzioniJvm.get(i);
                //Un debugger collegato non va riaperto. Il classpath si rimette sotto, uguale per jar e classi
                if (a.startsWith("-agentlib:") || a.startsWith("-javaagent:")) continue;
                if (a.equals("-cp") || a.equals("-classpath") || a.equals("--class-path")) {
                    i++;
                    continue;
                }
                if (a.startsWith("-Djava.class.path=")) continue;
                c.add(a);
            }
            if (Jar != null && Jar.getFileName() != null && Jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                c.add("-jar");
                c.add(Jar.toAbsolutePath().normalize().toString());
            } else {
                c.add("-cp");
                c.add(System.getProperty("java.class.path"));
                c.add(ClassePrincipale);
            }
            c.addAll(List.of(ArgomentiAvvio));
        }
        c.add(ARGOMENTO_ATTESA);
        c.add(Long.toString(Pid));
        return c;
    }

    /**
     * Da chiamare per prima in {@code main}: se il programma è stato avviato da un riavvio aspetta la fine del processo
     * precedente, poi restituisce gli argomenti <b>senza</b> {@value #ARGOMENTO_ATTESA}, perché un secondo riavvio non
     * erediti il pid del primo.
     */
    public static String[] AttendiChiusuraPrecedente(String[] Argomenti) {
        List<String> puliti = new ArrayList<>();
        long pid = -1;
        for (int i = 0; i < Argomenti.length; i++) {
            if (Argomenti[i].equals(ARGOMENTO_ATTESA)) {
                if (i + 1 < Argomenti.length) {
                    try {
                        pid = Long.parseLong(Argomenti[i + 1]);
                    } catch (NumberFormatException ex) {
                        System.err.println("Valore non valido per " + ARGOMENTO_ATTESA + ": " + Argomenti[i + 1]);
                    }
                    i++;
                }
                continue;
            }
            puliti.add(Argomenti[i]);
        }
        if (pid > 0) {
            Attendi(pid, ATTESA_MASSIMA_SECONDI);
        }
        return puliti.toArray(new String[0]);
    }

    /** @return {@code true} se il processo è finito (o non esiste più), {@code false} se è scaduto il tempo */
    static boolean Attendi(long Pid, long Secondi) {
        try {
            var processo = ProcessHandle.of(Pid);
            if (processo.isEmpty() || !processo.get().isAlive()) return true;
            processo.get().onExit().get(Secondi, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception ex) {
            //Scaduto il tempo: si prosegue comunque, se il database è ancora bloccato lo dirà l'apertura
            System.err.println("Riavvio: il processo precedente non è finito entro " + Secondi + " secondi");
            return false;
        }
    }
}
