package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fasi 2 e 3 degli aggiornamenti: capire come è installato il programma, scaricare il pacchetto giusto verificandone
 * lo sha256, e preparare lo script che lo installa <b>dopo</b> la chiusura del programma e lo riavvia.
 *
 * <p>Chi possiede i file decide cosa si può fare ({@link Tipo#Automatico}): l'installer Windows e le versioni portable
 * si aggiornano da sé, i pacchetti gestiti dal sistema (AUR, {@code .deb}, flatpak) e il DMG non firmato di macOS solo
 * con un avviso. Il programma non scarica mai nulla senza che l'utente lo abbia chiesto da {@link GUI_Aggiornamento}.
 *
 * <p>Le installazioni portable si aggiornano <b>copiando sopra</b> i file nuovi, senza cancellare nulla: la directory
 * di lavoro predefinita è quella da cui si avvia il programma, quindi i dati dell'utente possono stare nella stessa
 * cartella. Analisi: {@code nocommit/Documentazione/Analisi_Aggiornamenti_Automatici.md}.
 */
public final class AggiornamentoInstallazione {

    private AggiornamentoInstallazione() {}

    /** Nome del jar in tutti i pacchetti (lo fissa {@code release.yml}). */
    static final String NOME_JAR = "Giacenze_Crypto.jar";
    /** Nome del lanciatore jpackage (Windows con {@code .exe}, Linux in {@code bin/}). */
    static final String NOME_LANCIATORE = "Giacenze_Crypto";

    private static final int TIMEOUT_MS = 20000;

    /** Come è installato il programma, e quindi chi lo aggiorna. */
    public enum Tipo {
        /** Installer Inno Setup per utente: si aggiorna da sé con l'installer nuovo in modalità silenziosa. */
        INSTALLER_WINDOWS(true, "_Installer_Windows.exe", "installazione Windows"),
        /** App-image jpackage per Windows, decompressa dall'utente. */
        PORTABLE_WINDOWS(true, "_Portable_Windows.zip", "versione portable per Windows"),
        /** App-image jpackage per Linux, decompressa dall'utente. */
        PORTABLE_LINUX(true, "_Portable_Linux.zip", "versione portable per Linux"),
        /** Jar con il Java di sistema (pacchetto "Portable Multipiattaforma"). */
        PORTABLE_JAR(true, "_Portable_Multipiattaforma.zip", "versione portable multipiattaforma"),
        /** Pacchetto {@code .deb}: lo aggiorna il gestore pacchetti. */
        DEB(false, "_Deb_Linux.deb", "pacchetto .deb"),
        /** Jar sotto {@code /usr} o {@code /opt} senza jpackage: in pratica il pacchetto AUR. */
        PACCHETTO_SISTEMA(false, null, "pacchetto di sistema"),
        FLATPAK(false, ".flatpak", "flatpak"),
        /** DMG non firmato: la quarantena di macOS bloccherebbe un'app modificata. */
        MACOS(false, null, "macOS"),
        STORE(false, null, "Microsoft Store"),
        /** Avvio da sorgenti o da {@code target/classes}. */
        SVILUPPO(false, null, "avvio da sorgenti");

        /** {@code true} se il programma può installare da sé l'aggiornamento. */
        public final boolean Automatico;
        /** Fine del nome dell'allegato della release per questo tipo, o {@code null}. */
        final String Suffisso;
        public final String Descrizione;

        Tipo(boolean Automatico, String Suffisso, String Descrizione) {
            this.Automatico = Automatico;
            this.Suffisso = Suffisso;
            this.Descrizione = Descrizione;
        }
    }

    /**
     * @param Tipo come è installato
     * @param Radice la cartella del programma da aggiornare ({@code null} per i tipi non automatici)
     * @param Avviabile il lanciatore jpackage o il jar, per il riavvio
     */
    public record Installazione(Tipo Tipo, Path Radice, Path Avviabile) {}

    //=== RILEVAMENTO ===

    /** @return come è installato il programma in esecuzione */
    public static Installazione Rileva() {
        return Rileva(System.getProperty("os.name", ""), System.getProperty("jpackage.app-path"), PercorsoJar(),
                System.getenv("FLATPAK_ID") != null, VarStatiche.EdizioneStore());
    }

    /**
     * @param Os {@code os.name}
     * @param AppPath {@code jpackage.app-path}, impostata dal lanciatore jpackage (installer, portable, .deb)
     * @param Jar il jar da cui gira il programma, o la cartella delle classi
     * @param Flatpak {@code true} dentro la sandbox flatpak
     * @param Store edizione Microsoft Store
     */
    static Installazione Rileva(String Os, String AppPath, Path Jar, boolean Flatpak, boolean Store) {
        String os = Os == null ? "" : Os.toLowerCase(Locale.ROOT);
        boolean windows = os.startsWith("windows");
        if (Store) return new Installazione(Tipo.STORE, null, null);
        if (Flatpak) return new Installazione(Tipo.FLATPAK, null, null);
        if (os.contains("mac") || os.contains("darwin")) return new Installazione(Tipo.MACOS, null, null);
        if (AppPath != null && !AppPath.isBlank()) {
            Path lanciatore = Path.of(AppPath).toAbsolutePath().normalize();
            if (windows) {
                Path radice = lanciatore.getParent();
                //Inno mette il disinstallatore (unins000.exe) nella cartella che ha installato
                return new Installazione(HaDisinstallatoreInno(radice) ? Tipo.INSTALLER_WINDOWS : Tipo.PORTABLE_WINDOWS,
                        radice, lanciatore);
            }
            //Linux: <radice>/bin/Giacenze_Crypto
            Path radice = lanciatore.getParent() == null ? null : lanciatore.getParent().getParent();
            if (radice == null || isCartellaDiSistema(radice)) return new Installazione(Tipo.DEB, null, null);
            return new Installazione(Tipo.PORTABLE_LINUX, radice, lanciatore);
        }
        if (Jar == null || !Jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return new Installazione(Tipo.SVILUPPO, null, null);
        }
        Path jar = Jar.toAbsolutePath().normalize();
        if (!windows && isCartellaDiSistema(jar)) return new Installazione(Tipo.PACCHETTO_SISTEMA, null, null);
        return new Installazione(Tipo.PORTABLE_JAR, jar.getParent(), jar);
    }

    private static boolean isCartellaDiSistema(Path p) {
        String s = p.toString();
        return s.startsWith("/usr/") || s.startsWith("/opt/");
    }

    static boolean HaDisinstallatoreInno(Path Cartella) {
        if (Cartella == null || !Files.isDirectory(Cartella)) return false;
        try (Stream<Path> s = Files.list(Cartella)) {
            return s.anyMatch(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).matches("unins\\d{3}\\.exe"));
        } catch (IOException ex) {
            return false;
        }
    }

    private static Path PercorsoJar() {
        try {
            return Path.of(AggiornamentoInstallazione.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * @return l'allegato della release adatto a questa installazione, o {@code null} se la release non ne ha uno (una
     *         beta non ha il {@code .deb} né il DMG)
     */
    static Aggiornamenti.Allegato AllegatoPer(Tipo Tipo, Aggiornamenti.Rilascio Rilascio, String Arch) {
        if (Rilascio == null) return null;
        String suffisso = Tipo.Suffisso;
        if (Tipo == AggiornamentoInstallazione.Tipo.MACOS) {
            String a = Arch == null ? "" : Arch.toLowerCase(Locale.ROOT);
            suffisso = a.contains("aarch64") || a.contains("arm") ? "_DMG_MacOS_AppleSilicon.dmg" : "_DMG_MacOS_Intel.dmg";
        }
        if (suffisso == null) return null;
        for (Aggiornamenti.Allegato a : Rilascio.Allegati()) {
            if (a.Nome().endsWith(suffisso)) return a;
        }
        return null;
    }

    public static Aggiornamenti.Allegato AllegatoPer(Tipo Tipo, Aggiornamenti.Rilascio Rilascio) {
        return AllegatoPer(Tipo, Rilascio, System.getProperty("os.arch", ""));
    }

    /**
     * Cosa dire a chi non può aggiornare da qui. Mai consigli su come trattare i dati: solo dove trovare la versione.
     */
    public static String IstruzioniManuali(Tipo Tipo) {
        return switch (Tipo) {
            case PACCHETTO_SISTEMA -> "Il programma è installato come pacchetto di sistema (per esempio dall'AUR): "
                    + "si aggiorna con il gestore pacchetti della distribuzione.";
            case DEB -> "Il programma è installato dal pacchetto .deb: scaricare il nuovo pacchetto dalla pagina della "
                    + "versione e installarlo come il precedente.";
            case FLATPAK -> "Il programma è installato come flatpak: scaricare il nuovo pacchetto .flatpak dalla pagina "
                    + "della versione e installarlo come il precedente.";
            case MACOS -> "Su macOS l'applicazione non è firmata e non può aggiornarsi da sola: scaricare il nuovo DMG "
                    + "dalla pagina della versione e sostituire l'applicazione.";
            case SVILUPPO -> "Il programma è avviato dai sorgenti: aggiornare il codice e ricompilare.";
            default -> "Scaricare la nuova versione dalla pagina della versione.";
        };
    }

    //=== SCARICAMENTO ===

    /**
     * Scarica l'allegato in {@code Destinazione} calcolandone lo sha256 durante lo scaricamento, e lo tiene solo se
     * corrisponde a quello pubblicato da GitHub. Un file parziale o diverso viene cancellato.
     *
     * @param Avanzamento riceve i byte scaricati finora
     * @param Interrotto {@code true} quando l'utente ha annullato
     * @return {@code false} se l'utente ha annullato
     * @throws IOException errore di rete o sha256 diverso
     */
    public static boolean Scarica(Aggiornamenti.Allegato Allegato, Path Destinazione, LongConsumer Avanzamento,
            BooleanSupplier Interrotto) throws IOException {
        if (Allegato.Sha256() == null || Allegato.Sha256().isBlank()) {
            throw new IOException("La release non pubblica lo sha256 di " + Allegato.Nome() + ": file non verificabile.");
        }
        Files.createDirectories(Destinazione.getParent());
        Path parziale = Destinazione.resolveSibling(Destinazione.getFileName() + ".parziale");
        MessageDigest sha = Sha256();
        HttpURLConnection conn = (HttpURLConnection) URI.create(Allegato.Url()).toURL().openConnection();
        boolean completato = false;
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "GiacenzeCrypto/" + VarStatiche.Versione);
            int codice = conn.getResponseCode();
            if (codice != HttpURLConnection.HTTP_OK) throw new IOException("Scaricamento: risposta " + codice);
            try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(parziale)) {
                byte[] buf = new byte[64 * 1024];
                long totale = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (Interrotto.getAsBoolean()) return false;
                    out.write(buf, 0, n);
                    sha.update(buf, 0, n);
                    totale += n;
                    Avanzamento.accept(totale);
                }
            }
            String calcolato = HexFormat.of().formatHex(sha.digest());
            if (!calcolato.equalsIgnoreCase(Allegato.Sha256().trim())) {
                throw new IOException("Il file scaricato non corrisponde a quello pubblicato (sha256 diverso).");
            }
            Files.move(parziale, Destinazione, StandardCopyOption.REPLACE_EXISTING);
            completato = true;
            return true;
        } finally {
            conn.disconnect();
            if (!completato) Files.deleteIfExists(parziale);
        }
    }

    private static MessageDigest Sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    //=== ESTRAZIONE ===

    /**
     * Decomprime lo zip in {@code Cartella}, rifiutando le voci che ne uscirebbero ({@code ../}). Su Linux i file
     * nelle cartelle {@code bin/} e i programmi di supporto del runtime diventano eseguibili: lo zip di Java non
     * conserva i permessi.
     */
    public static void Estrai(Path Zip, Path Cartella) throws IOException {
        Path base = Cartella.toAbsolutePath().normalize();
        Files.createDirectories(base);
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(Zip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                Path dest = base.resolve(e.getName()).normalize();
                if (!dest.startsWith(base)) throw new IOException("Voce dello zip fuori cartella: " + e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zin, dest, StandardCopyOption.REPLACE_EXISTING);
                    if (isDaRendereEseguibile(base.relativize(dest))) dest.toFile().setExecutable(true, false);
                }
            }
        }
    }

    static boolean isDaRendereEseguibile(Path Relativo) {
        String nome = Relativo.getFileName().toString();
        Path padre = Relativo.getParent();
        return (padre != null && padre.getFileName().toString().equals("bin"))
                || nome.equals("jspawnhelper") || nome.equals("jexec");
    }

    /**
     * @return la cartella, dentro l'estratto, che corrisponde alla radice dell'installazione (gli zip hanno o no una
     *         cartella {@code GiacenzeCrypto/} in cima), o {@code null} se lo zip non è del tipo atteso
     */
    public static Path RadiceEstratta(Path Cartella, Tipo Tipo) throws IOException {
        String segnale = switch (Tipo) {
            case PORTABLE_JAR -> NOME_JAR;
            case PORTABLE_WINDOWS -> NOME_LANCIATORE + ".exe";
            case PORTABLE_LINUX -> "bin/" + NOME_LANCIATORE;
            default -> null;
        };
        if (segnale == null) return null;
        try (Stream<Path> s = Files.walk(Cartella, 3)) {
            return s.filter(Files::isDirectory).filter(d -> Files.isRegularFile(d.resolve(segnale)))
                    .findFirst().orElse(null);
        }
    }

    //=== CARTELLA DI LAVORO E ESITO ===

    /** File che ricorda, fra la chiusura e il riavvio, quale aggiornamento era in corso. */
    static final String FILE_IN_CORSO = "in_corso.txt";
    static final String FILE_LOG = "aggiornamento.log";

    /** @return la cartella dei file dell'aggiornamento, sotto {@code Temporanei/} */
    public static Path Cartella() {
        return Path.of(VarStatiche.getCartella_Temporanei(), "Aggiornamento");
    }

    /** Ricorda l'aggiornamento che sta per essere installato, per verificarne l'esito al prossimo avvio. */
    static void ScriviInCorso(Path Cartella, String Da, String A) throws IOException {
        Files.writeString(Cartella.resolve(FILE_IN_CORSO), Da + "\n" + A + "\n", StandardCharsets.UTF_8);
    }

    /**
     * L'esito di un aggiornamento installato prima di questo avvio.
     * @param Riuscito {@code true} se la versione in uso è almeno quella che si stava installando
     */
    public record EsitoPrecedente(String Da, String A, boolean Riuscito, Path Log) {}

    /**
     * Legge l'esito dell'ultimo aggiornamento e pulisce la cartella. Se è riuscito si cancella tutto, se no si
     * conserva il solo registro, che l'utente può allegare a una segnalazione.
     *
     * @return l'esito, o {@code null} se non c'era un aggiornamento in corso
     */
    public static EsitoPrecedente LeggiEPulisci(Path Cartella, String VersioneInUso) {
        Path inCorso = Cartella.resolve(FILE_IN_CORSO);
        if (!Files.isRegularFile(inCorso)) {
            //Scaricamenti annullati alla conferma finale: decine di MB che nessuno userà più
            Svuota(Cartella, false);
            return null;
        }
        EsitoPrecedente esito;
        try {
            List<String> righe = Files.readAllLines(inCorso, StandardCharsets.UTF_8);
            String da = righe.size() > 0 ? righe.get(0).trim() : "";
            String a = righe.size() > 1 ? righe.get(1).trim() : "";
            boolean riuscito = Aggiornamenti.isVersioneValida(VersioneInUso) && Aggiornamenti.isVersioneValida(a)
                    && Funzioni.ConfrontaVersioni(VersioneInUso, a) >= 0;
            esito = new EsitoPrecedente(da, a, riuscito, Cartella.resolve(FILE_LOG));
        } catch (IOException ex) {
            esito = null;
        }
        Svuota(Cartella, esito != null && !esito.Riuscito());
        return esito;
    }

    /** Cancella il contenuto della cartella, conservando a richiesta il registro dell'installazione. */
    private static void Svuota(Path Cartella, boolean ConservaLog) {
        if (!Files.isDirectory(Cartella)) return;
        try (Stream<Path> s = Files.walk(Cartella)) {
            s.sorted(java.util.Comparator.reverseOrder())
                    .filter(f -> !f.equals(Cartella))
                    .filter(f -> !(ConservaLog && f.getFileName().toString().equals(FILE_LOG)))
                    .forEach(f -> f.toFile().delete());
        } catch (IOException ex) {
            System.out.println("Aggiornamento: pulizia di " + Cartella + " non riuscita: " + ex.getMessage());
        }
    }

    //=== INSTALLAZIONE ===

    /** @return il comando che riavvia il programma dopo l'aggiornamento */
    public static List<String> ComandoRiavvio(Installazione Inst) {
        List<String> c = new ArrayList<>();
        if (Inst.Tipo() == Tipo.PORTABLE_JAR) {
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
            c.add(Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java").toString());
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                //Un debugger collegato non va riaperto
                if (!a.startsWith("-agentlib:") && !a.startsWith("-javaagent:")) c.add(a);
            }
            c.add("-jar");
            c.add(Inst.Avviabile().toString());
            c.addAll(List.of(VarStatiche.ArgomentiAvvio));
        } else {
            //Il lanciatore jpackage rilegge da sé gli argomenti del suo .cfg (--workdir, --risorse)
            c.add(Inst.Avviabile().toString());
        }
        return c;
    }

    /**
     * Lo script PowerShell che, chiuso il programma, installa e riavvia.
     *
     * @param Pid il processo da attendere (il lanciatore jpackage ospita la JVM, quindi è lui)
     * @param Pacchetto l'installer da eseguire, oppure la radice estratta da copiare sopra
     * @param Riavvio comando di riavvio
     */
    static String ScriptWindows(Tipo Tipo, long Pid, Path Pacchetto, Path Radice, List<String> Riavvio,
            Path CartellaLavoro, Path Log) {
        StringBuilder s = new StringBuilder();
        s.append("$ErrorActionPreference = 'Continue'\r\n");
        s.append("Start-Transcript -Path ").append(Ps(Log.toString())).append(" -Append | Out-Null\r\n");
        //Senza limite di tempo, come su Linux: la compattazione dei database alla chiusura può durare a lungo, e
        //installare con il programma ancora aperto troverebbe i file bloccati
        s.append("while (Get-Process -Id ").append(Pid).append(" -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 1 }\r\n");
        s.append("Start-Sleep -Seconds 2\r\n");
        if (Tipo == AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS) {
            //Stesso AppId: Inno aggiorna sul posto e non tocca i file che non ha installato lui (i dati)
            s.append("$p = Start-Process -FilePath ").append(Ps(Pacchetto.toString()))
                    .append(" -ArgumentList '/SILENT','/SUPPRESSMSGBOXES','/NORESTART','/SP-' -Wait -PassThru\r\n");
            s.append("\"Installer terminato con codice $($p.ExitCode)\"\r\n");
        } else {
            //robocopy /E unisce senza cancellare nulla (niente /PURGE): i dati nella cartella restano
            s.append("robocopy ").append(Ps(Pacchetto.toString())).append(' ').append(Ps(Radice.toString()))
                    .append(" /E /R:5 /W:2 /NFL /NDL /NP\r\n");
            s.append("\"robocopy terminato con codice $LASTEXITCODE\"\r\n");
        }
        s.append("Start-Process -FilePath ").append(Ps(Riavvio.get(0)));
        if (Riavvio.size() > 1) {
            s.append(" -ArgumentList ");
            List<String> argomenti = new ArrayList<>();
            //Start-Process unisce gli argomenti con spazi: quelli con spazi vanno racchiusi tra virgolette
            for (String a : Riavvio.subList(1, Riavvio.size())) {
                argomenti.add(Ps(a.contains(" ") ? "\"" + a + "\"" : a));
            }
            s.append(String.join(",", argomenti));
        }
        s.append(" -WorkingDirectory ").append(Ps(CartellaLavoro.toString())).append("\r\n");
        s.append("Stop-Transcript | Out-Null\r\n");
        return s.toString();
    }

    /** Lo script {@code sh} per Linux: attende la chiusura, copia sopra senza cancellare, riavvia. */
    static String ScriptLinux(long Pid, Path Sorgente, Path Radice, List<String> Riavvio, Path CartellaLavoro,
            Path Log) {
        StringBuilder s = new StringBuilder();
        s.append("#!/bin/sh\n");
        s.append("exec >>").append(Sh(Log.toString())).append(" 2>&1\n");
        s.append("echo \"Aggiornamento: attesa della chiusura del processo ").append(Pid).append("\"\n");
        s.append("while kill -0 ").append(Pid).append(" 2>/dev/null; do sleep 1; done\n");
        s.append("sleep 1\n");
        //cp -R senza -p: un file che esiste già conserva i suoi permessi (eseguibili compresi)
        //Anche se la copia non riesce si riavvia: la versione precedente ripartirà e al suo avvio segnalerà che
        //l'aggiornamento non è riuscito, invece di lasciare l'utente senza programma e senza spiegazioni
        s.append("if cp -R ").append(Sh(Sorgente + "/.")).append(' ').append(Sh(Radice + "/"))
                .append("; then echo \"Copia completata\"; else echo \"Copia non riuscita\"; fi\n");
        s.append("cd ").append(Sh(CartellaLavoro.toString())).append(" || cd /\n");
        s.append("nohup");
        for (String a : Riavvio) s.append(' ').append(Sh(a));
        s.append(" >/dev/null 2>&1 &\n");
        return s.toString();
    }

    /** Stringa PowerShell fra apici singoli. */
    static String Ps(String v) {
        return "'" + v.replace("'", "''") + "'";
    }

    /** Stringa sh fra apici singoli. */
    static String Sh(String v) {
        return "'" + v.replace("'", "'\\''") + "'";
    }

    /**
     * Scrive lo script e lo avvia staccato dal programma: continuerà dopo la sua chiusura, aspettandola.
     *
     * @param Pacchetto installer scaricato, o radice estratta dello zip
     */
    public static void AvviaInstallazione(Installazione Inst, Path Pacchetto, Path Cartella) throws IOException {
        long pid = ProcessHandle.current().pid();
        List<String> riavvio = ComandoRiavvio(Inst);
        Path lavoro = Path.of(System.getProperty("user.dir"));
        Path log = Cartella.resolve(FILE_LOG);
        ProcessBuilder pb;
        if (Inst.Tipo() == Tipo.INSTALLER_WINDOWS || Inst.Tipo() == Tipo.PORTABLE_WINDOWS
                || (Inst.Tipo() == Tipo.PORTABLE_JAR && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"))) {
            Path script = Cartella.resolve("aggiorna.ps1");
            //BOM: senza, PowerShell 5 legge lo script come ANSI e rovina i percorsi con lettere accentate
            Files.writeString(script, "﻿" + ScriptWindows(Inst.Tipo(), pid, Pacchetto, Inst.Radice(), riavvio, lavoro, log),
                    StandardCharsets.UTF_8);
            pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", script.toString());
        } else {
            Path script = Cartella.resolve("aggiorna.sh");
            Files.writeString(script, ScriptLinux(pid, Pacchetto, Inst.Radice(), riavvio, lavoro, log), StandardCharsets.UTF_8);
            script.toFile().setExecutable(true, true);
            //setsid: lo script sopravvive anche alla chiusura del terminale da cui era partito il programma
            pb = Files.isExecutable(Path.of("/usr/bin/setsid"))
                    ? new ProcessBuilder("/usr/bin/setsid", "/bin/sh", script.toString())
                    : new ProcessBuilder("/bin/sh", script.toString());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.directory(Cartella.toFile());
        pb.start();
    }
}
