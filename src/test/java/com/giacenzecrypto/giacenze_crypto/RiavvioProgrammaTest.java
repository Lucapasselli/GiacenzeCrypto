package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Il riavvio dopo il cambio di tema: il comando che rilancia il programma e l'attesa della chiusura precedente. */
class RiavvioProgrammaTest {

    private static final String[] ARGOMENTI = {"--workdir", "/dati/", "--fontSize", "14"};

    @Test
    void ilLanciatoreJpackageRipartePerConto()  {
        // Gli argomenti del suo .cfg li rilegge da solo: rimetterli qui li duplicherebbe
        List<String> c = RiavvioProgramma.Comando("/opt/GC/bin/Giacenze_Crypto", Path.of("/opt/GC/lib/app/Giacenze_Crypto.jar"),
                "/opt/GC/lib/runtime", "Linux", List.of("-Xmx2g"), ARGOMENTI, "Principale", 4242);
        assertEquals(List.of("/opt/GC/bin/Giacenze_Crypto", "--attendiPid", "4242"), c);
    }

    @Test
    void ilJarRipartConLeStesseOpzioniEGliStessiArgomenti() {
        List<String> c = RiavvioProgramma.Comando(null, Path.of("/prog/Giacenze_Crypto.jar"), "/jdk", "Linux",
                List.of("-Xmx4g", "--enable-native-access=ALL-UNNAMED", "-agentlib:jdwp=transport=dt_socket,server=y",
                        "-javaagent:/x/agent.jar", "-Dprova=1"),
                ARGOMENTI, "Principale", 7);
        assertEquals(Path.of("/jdk", "bin", "java").toString(), c.get(0));
        // Debugger e agenti non si riaprono, le altre opzioni sì
        assertEquals(List.of("-Xmx4g", "--enable-native-access=ALL-UNNAMED", "-Dprova=1"), c.subList(1, 4));
        assertEquals(List.of("-jar", Path.of("/prog/Giacenze_Crypto.jar").toString()), c.subList(4, 6));
        assertEquals(List.of("--workdir", "/dati/", "--fontSize", "14", "--attendiPid", "7"), c.subList(6, c.size()));
    }

    @Test
    void suWindowsSiUsaJavaw() {
        List<String> c = RiavvioProgramma.Comando(null, Path.of("C:/GC/Giacenze_Crypto.jar"), "C:/jdk", "Windows 11",
                List.of(), new String[0], "Principale", 1);
        assertEquals(Path.of("C:/jdk", "bin", "javaw.exe").toString(), c.get(0));
    }

    @Test
    void daClassiSenzaJarSiRimetteIlClasspathUnaVolta() {
        // L'avvio da NetBeans o da Maven: classi in una cartella, classpath passato con -cp fra le opzioni
        List<String> c = RiavvioProgramma.Comando(null, Path.of("/progetto/target/classes"), "/jdk", "Linux",
                List.of("-cp", "/vecchio/classpath", "-Xmx2g"), ARGOMENTI,
                "com.giacenzecrypto.giacenze_crypto.Giacenze_Crypto", 9);
        assertEquals(1, c.stream().filter(a -> a.equals("-cp")).count());
        assertFalse(c.contains("/vecchio/classpath"));
        assertTrue(c.contains("-Xmx2g"));
        int i = c.indexOf("-cp");
        assertEquals(System.getProperty("java.class.path"), c.get(i + 1));
        assertEquals("com.giacenzecrypto.giacenze_crypto.Giacenze_Crypto", c.get(i + 2));
        assertFalse(c.contains("-jar"));
    }

    @Test
    void ilPidDelVecchioProcessoNonSiPortaDietroAlSecondoRiavvio() {
        String[] puliti = RiavvioProgramma.AttendiChiusuraPrecedente(
                new String[] {"--workdir", "/dati/", "--attendiPid", Long.toString(deadPid()), "--debug"});
        assertArrayEquals(new String[] {"--workdir", "/dati/", "--debug"}, puliti);
        // Senza l'argomento non cambia niente e non si aspetta
        assertArrayEquals(ARGOMENTI, RiavvioProgramma.AttendiChiusuraPrecedente(ARGOMENTI));
        // Un pid illeggibile si ignora, ma l'argomento sparisce lo stesso
        assertArrayEquals(new String[] {"--debug"},
                RiavvioProgramma.AttendiChiusuraPrecedente(new String[] {"--attendiPid", "xyz", "--debug"}));
    }

    @Test
    void unProcessoGiaFinitoNonSiAspetta() throws Exception {
        long t = System.nanoTime();
        assertTrue(RiavvioProgramma.Attendi(deadPid(), 30));
        assertTrue((System.nanoTime() - t) < 5_000_000_000L, "un processo finito non deve far aspettare");
    }

    @Test
    void unProcessoCheNonFinisceScadeDopoIlTempoMassimo() {
        // Il processo dei test è vivo: dopo il tempo concesso si prosegue comunque (lo dirà l'apertura del database)
        assertFalse(RiavvioProgramma.Attendi(ProcessHandle.current().pid(), 1));
    }

    /** Il pid di un processo appena terminato. */
    private static long deadPid() {
        try {
            Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version")
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            long pid = p.pid();
            p.waitFor();
            return pid;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
