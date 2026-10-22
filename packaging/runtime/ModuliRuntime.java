import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;

/**
 * Prepara le opzioni di jpackage per il runtime dei pacchetti, controllando l'elenco dei moduli.
 *
 * <p>Legge {@code moduli.txt} (accanto a questo file), esegue jdeps sul jar con le dipendenze e si
 * ferma con errore se jdeps trova un modulo che non è nell'elenco, o se un modulo dell'elenco non
 * esiste nel JDK in uso. Se è tutto a posto scrive su stdout due righe {@code NOME=valore}, pensate per
 * essere accodate a {@code $GITHUB_ENV}:
 * <pre>
 *   MODULI_RUNTIME=java.base,java.desktop,...
 *   OPZIONI_JLINK=--strip-native-commands --strip-debug --no-man-pages --no-header-files --include-locales=it,en
 * </pre>
 *
 * <p>Uso, dalla radice del repository: {@code java packaging/runtime/ModuliRuntime.java <jar-with-dependencies>}
 */
public class ModuliRuntime {

    /** Le opzioni che jpackage passa a jlink di suo, più le sole lingue che servono. */
    static final String OPZIONI_JLINK = "--strip-native-commands --strip-debug --no-man-pages --no-header-files"
            + " --include-locales=it,en";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java packaging/runtime/ModuliRuntime.java <jar-with-dependencies>");
            System.exit(2);
        }
        Path elenco = Path.of("packaging", "runtime", "moduli.txt");
        Set<String> moduli = new LinkedHashSet<>();
        for (String riga : Files.readAllLines(elenco)) {
            String m = riga.strip();
            if (!m.isEmpty() && !m.startsWith("#")) {
                moduli.add(m);
            }
        }

        List<String> errori = new ArrayList<>();
        ModuleFinder sistema = ModuleFinder.ofSystem();
        for (String m : moduli) {
            if (sistema.find(m).isEmpty()) {
                errori.add("il modulo " + m + " di " + elenco + " non esiste in questo JDK (" + Runtime.version() + ")");
            }
        }

        ToolProvider jdeps = ToolProvider.findFirst("jdeps")
                .orElseThrow(() -> new IllegalStateException("jdeps non disponibile: serve un JDK completo"));
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int esito = jdeps.run(new PrintWriter(out), new PrintWriter(err), "--multi-release",
                String.valueOf(Runtime.version().feature()), "--ignore-missing-deps", "--print-module-deps", args[0]);
        String[] righe = out.toString().strip().split("\\R");
        String richiesti = righe[righe.length - 1].strip();
        if (esito != 0 || richiesti.isEmpty()) {
            System.err.println("jdeps non è riuscito ad analizzare " + args[0] + ":\n" + out + err);
            System.exit(1);
        }
        for (String m : richiesti.split(",")) {
            if (!moduli.contains(m.strip())) {
                errori.add("jdeps trova il modulo " + m.strip() + ", che manca in " + elenco);
            }
        }

        if (!errori.isEmpty()) {
            errori.forEach(e -> System.err.println("ERRORE: " + e));
            System.err.println("Moduli richiesti da jdeps: " + richiesti);
            System.exit(1);
        }
        System.err.println("Moduli richiesti da jdeps: " + richiesti);
        System.err.println("Moduli del runtime (" + moduli.size() + "): " + String.join(",", moduli));
        System.out.println("MODULI_RUNTIME=" + String.join(",", moduli));
        System.out.println("OPZIONI_JLINK=" + OPZIONI_JLINK);
    }
}
