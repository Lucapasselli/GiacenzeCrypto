package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fasi 2 e 3 degli aggiornamenti ({@link AggiornamentoInstallazione}): riconoscimento dell'installazione, scelta del
 * pacchetto, estrazione sicura, script di installazione. Nessuna rete.
 */
class AggiornamentoInstallazioneTest {

    @TempDir
    Path tmp;

    private static Aggiornamenti.Rilascio rilascio(String... nomi) {
        List<Aggiornamenti.Allegato> a = new java.util.ArrayList<>();
        for (String n : nomi) a.add(new Aggiornamenti.Allegato(n, "https://x/" + n, "abc", 10));
        return new Aggiornamenti.Rilascio("1.0.66", false, "https://x", a);
    }

    //=== RILEVAMENTO ===

    @Test
    void rileva_windows_conDisinstallatoreInno_installer() throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("Giacenze_Crypto"));
        Files.createFile(dir.resolve("unins000.exe"));
        AggiornamentoInstallazione.Installazione i = AggiornamentoInstallazione.Rileva("Windows 11",
                dir.resolve("Giacenze_Crypto.exe").toString(), null, false, false);
        assertEquals(AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS, i.Tipo());
        assertEquals(dir.toAbsolutePath().normalize(), i.Radice());
    }

    @Test
    void rileva_windows_senzaDisinstallatore_portable() throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("GiacenzeCrypto"));
        AggiornamentoInstallazione.Installazione i = AggiornamentoInstallazione.Rileva("Windows 10",
                dir.resolve("Giacenze_Crypto.exe").toString(), null, false, false);
        assertEquals(AggiornamentoInstallazione.Tipo.PORTABLE_WINDOWS, i.Tipo());
    }

    @Test
    void rileva_linux_appImage_portableODeb() {
        AggiornamentoInstallazione.Installazione p = AggiornamentoInstallazione.Rileva("Linux",
                "/home/u/GiacenzeCrypto/bin/Giacenze_Crypto", null, false, false);
        assertEquals(AggiornamentoInstallazione.Tipo.PORTABLE_LINUX, p.Tipo());
        assertEquals(Path.of("/home/u/GiacenzeCrypto"), p.Radice());
        assertEquals(AggiornamentoInstallazione.Tipo.DEB, AggiornamentoInstallazione.Rileva("Linux",
                "/opt/giacenze-crypto/bin/Giacenze_Crypto", null, false, false).Tipo());
    }

    @Test
    void rileva_jar_portableOPacchettoDiSistema() {
        AggiornamentoInstallazione.Installazione p = AggiornamentoInstallazione.Rileva("Linux", null,
                Path.of("/home/u/GiacenzeCrypto/Giacenze_Crypto.jar"), false, false);
        assertEquals(AggiornamentoInstallazione.Tipo.PORTABLE_JAR, p.Tipo());
        assertEquals(Path.of("/home/u/GiacenzeCrypto"), p.Radice());
        assertEquals(AggiornamentoInstallazione.Tipo.PACCHETTO_SISTEMA, AggiornamentoInstallazione.Rileva("Linux",
                null, Path.of("/usr/share/giacenze-crypto/Giacenze_Crypto.jar"), false, false).Tipo(), "AUR");
    }

    @Test
    void rileva_casiSoloAvviso() {
        assertEquals(AggiornamentoInstallazione.Tipo.STORE,
                AggiornamentoInstallazione.Rileva("Windows 11", "C:\\x\\a.exe", null, false, true).Tipo());
        assertEquals(AggiornamentoInstallazione.Tipo.FLATPAK, AggiornamentoInstallazione.Rileva("Linux", null,
                Path.of("/app/share/giacenze-crypto/Giacenze_Crypto.jar"), true, false).Tipo());
        assertEquals(AggiornamentoInstallazione.Tipo.MACOS,
                AggiornamentoInstallazione.Rileva("Mac OS X", "/Applications/G.app/Contents/MacOS/G", null, false, false).Tipo());
        assertEquals(AggiornamentoInstallazione.Tipo.SVILUPPO,
                AggiornamentoInstallazione.Rileva("Linux", null, Path.of("/home/u/progetto/target/classes"), false, false).Tipo());
        for (AggiornamentoInstallazione.Tipo t : List.of(AggiornamentoInstallazione.Tipo.STORE,
                AggiornamentoInstallazione.Tipo.FLATPAK, AggiornamentoInstallazione.Tipo.MACOS,
                AggiornamentoInstallazione.Tipo.DEB, AggiornamentoInstallazione.Tipo.PACCHETTO_SISTEMA,
                AggiornamentoInstallazione.Tipo.SVILUPPO)) {
            assertFalse(t.Automatico, t.name());
        }
    }

    //=== PACCHETTO ===

    @Test
    void allegatoPer_sceglieIlPacchettoDelTipo() {
        Aggiornamenti.Rilascio r = rilascio("GiacenzeCrypto_1.0.66_Installer_Windows.exe",
                "GiacenzeCrypto_1.0.66_Portable_Windows.zip", "GiacenzeCrypto_1.0.66_Portable_Linux.zip",
                "GiacenzeCrypto_1.0.66_Portable_Multipiattaforma.zip", "GiacenzeCrypto_1.0.66_Deb_Linux.deb",
                "GiacenzeCrypto_1.0.66_DMG_MacOS_AppleSilicon.dmg", "GiacenzeCrypto_1.0.66_DMG_MacOS_Intel.dmg");
        assertEquals("GiacenzeCrypto_1.0.66_Installer_Windows.exe", AggiornamentoInstallazione
                .AllegatoPer(AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS, r, "amd64").Nome());
        assertEquals("GiacenzeCrypto_1.0.66_Portable_Linux.zip", AggiornamentoInstallazione
                .AllegatoPer(AggiornamentoInstallazione.Tipo.PORTABLE_LINUX, r, "amd64").Nome());
        assertEquals("GiacenzeCrypto_1.0.66_Portable_Multipiattaforma.zip", AggiornamentoInstallazione
                .AllegatoPer(AggiornamentoInstallazione.Tipo.PORTABLE_JAR, r, "amd64").Nome());
        assertEquals("GiacenzeCrypto_1.0.66_DMG_MacOS_AppleSilicon.dmg", AggiornamentoInstallazione
                .AllegatoPer(AggiornamentoInstallazione.Tipo.MACOS, r, "aarch64").Nome());
        assertEquals("GiacenzeCrypto_1.0.66_DMG_MacOS_Intel.dmg", AggiornamentoInstallazione
                .AllegatoPer(AggiornamentoInstallazione.Tipo.MACOS, r, "x86_64").Nome());
        assertNull(AggiornamentoInstallazione.AllegatoPer(AggiornamentoInstallazione.Tipo.PACCHETTO_SISTEMA, r, "amd64"));
    }

    @Test
    void allegatoPer_betaSenzaDeb_null() {
        Aggiornamenti.Rilascio beta = rilascio("GiacenzeCrypto_1.0.65.08_Installer_Windows.exe",
                "GiacenzeCrypto_1.0.65.08_Portable_Linux.zip");
        assertNull(AggiornamentoInstallazione.AllegatoPer(AggiornamentoInstallazione.Tipo.DEB, beta, "amd64"));
    }

    //=== ESTRAZIONE ===

    private Path zip(String nome, String... voci) throws IOException {
        Path z = tmp.resolve(nome);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(z))) {
            for (String v : voci) {
                out.putNextEntry(new ZipEntry(v));
                if (!v.endsWith("/")) out.write(("contenuto " + v).getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return z;
    }

    @Test
    void estrai_rifiutaLeVociFuoriCartella() throws IOException {
        Path z = zip("cattivo.zip", "ok.txt", "../fuori.txt");
        assertThrows(IOException.class, () -> AggiornamentoInstallazione.Estrai(z, tmp.resolve("estratto")));
        assertFalse(Files.exists(tmp.resolve("fuori.txt")));
    }

    @Test
    void radiceEstratta_treFormeDegliZip() throws IOException {
        //Multipiattaforma e Windows hanno GiacenzeCrypto/ in cima, Linux no (vedi release.yml)
        Path multi = tmp.resolve("multi");
        AggiornamentoInstallazione.Estrai(zip("m.zip", "GiacenzeCrypto/Giacenze_Crypto.jar", "GiacenzeCrypto/logo.png"), multi);
        assertEquals(multi.resolve("GiacenzeCrypto"),
                AggiornamentoInstallazione.RadiceEstratta(multi, AggiornamentoInstallazione.Tipo.PORTABLE_JAR));

        Path win = tmp.resolve("win");
        AggiornamentoInstallazione.Estrai(zip("w.zip", "GiacenzeCrypto/Giacenze_Crypto.exe", "GiacenzeCrypto/app/Giacenze_Crypto.jar"), win);
        assertEquals(win.resolve("GiacenzeCrypto"),
                AggiornamentoInstallazione.RadiceEstratta(win, AggiornamentoInstallazione.Tipo.PORTABLE_WINDOWS));

        Path lin = tmp.resolve("lin");
        AggiornamentoInstallazione.Estrai(zip("l.zip", "bin/Giacenze_Crypto", "lib/app/Giacenze_Crypto.jar",
                "lib/runtime/bin/java", "lib/runtime/lib/jspawnhelper"), lin);
        assertEquals(lin, AggiornamentoInstallazione.RadiceEstratta(lin, AggiornamentoInstallazione.Tipo.PORTABLE_LINUX));
        assertTrue(Files.isExecutable(lin.resolve("bin/Giacenze_Crypto")), "lo zip non conserva i permessi");
        assertTrue(Files.isExecutable(lin.resolve("lib/runtime/bin/java")));
        assertTrue(Files.isExecutable(lin.resolve("lib/runtime/lib/jspawnhelper")));

        assertNull(AggiornamentoInstallazione.RadiceEstratta(multi, AggiornamentoInstallazione.Tipo.PORTABLE_LINUX),
                "uno zip del tipo sbagliato non va installato");
    }

    //=== SCRIPT ===

    @Test
    void scriptWindows_installer_silenzioso_eApiciRaddoppiati() {
        String s = AggiornamentoInstallazione.ScriptWindows(AggiornamentoInstallazione.Tipo.INSTALLER_WINDOWS, 4242,
                Path.of("C:/Users/Dell'Acqua/setup.exe"), Path.of("C:/P"), List.of("C:/P/Giacenze_Crypto.exe"),
                Path.of("C:/P"), Path.of("C:/P/log.txt"));
        assertTrue(s.contains("while (Get-Process -Id 4242"), "attesa senza limite di tempo");
        assertFalse(s.contains("-Timeout"));
        assertTrue(s.contains("'/SILENT'"));
        assertTrue(s.contains("Dell''Acqua"), "in PowerShell l'apice si raddoppia");
        assertFalse(s.contains("robocopy"));
    }

    @Test
    void scriptWindows_portable_robocopySenzaPurge() {
        String s = AggiornamentoInstallazione.ScriptWindows(AggiornamentoInstallazione.Tipo.PORTABLE_WINDOWS, 1,
                Path.of("C:/T/estratto/GiacenzeCrypto"), Path.of("C:/P"), List.of("C:/P/Giacenze_Crypto.exe"),
                Path.of("C:/P"), Path.of("C:/P/log.txt"));
        assertTrue(s.contains("robocopy"));
        assertTrue(s.contains("/E"));
        assertFalse(s.contains("/PURGE") || s.contains("/MIR"), "i dati nella cartella non vanno cancellati");
    }

    @Test
    void sh_apiceSingolo() {
        assertEquals("'a'\\''b'", AggiornamentoInstallazione.Sh("a'b"));
    }

    /**
     * Esegue davvero lo script Linux: attende un processo, copia sopra senza cancellare i dati, conserva i permessi dei
     * file esistenti e lancia il comando di riavvio.
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    void scriptLinux_eseguito_copiaSopraERiavvia() throws Exception {
        Path radice = Files.createDirectories(tmp.resolve("installato con spazio"));
        Files.createDirectories(radice.resolve("bin"));
        Path lanciatore = Files.writeString(radice.resolve("bin/Giacenze_Crypto"), "vecchio");
        lanciatore.toFile().setExecutable(true);
        Files.writeString(radice.resolve("movimenti.crypto.db"), "dati dell'utente");

        Path nuovo = Files.createDirectories(tmp.resolve("estratto"));
        Files.createDirectories(nuovo.resolve("bin"));
        Files.writeString(nuovo.resolve("bin/Giacenze_Crypto"), "nuovo");
        Files.writeString(nuovo.resolve("nuovo.txt"), "aggiunto");

        Path riavviato = tmp.resolve("riavviato");
        Process attesa = new ProcessBuilder("sleep", "2").start();
        String script = AggiornamentoInstallazione.ScriptLinux(attesa.pid(), nuovo, radice,
                List.of("/bin/sh", "-c", "echo ok > \"$0\"", riavviato.toString()), tmp, tmp.resolve("log.txt"));
        Path file = Files.writeString(tmp.resolve("aggiorna.sh"), script);
        Process p = new ProcessBuilder("/bin/sh", file.toString()).start();
        assertTrue(p.waitFor(20, TimeUnit.SECONDS));
        assertFalse(attesa.isAlive(), "lo script deve aspettare la fine del processo");
        for (int i = 0; i < 50 && !Files.exists(riavviato); i++) Thread.sleep(100);

        assertEquals("nuovo", Files.readString(lanciatore));
        assertTrue(Files.isExecutable(lanciatore), "un file esistente conserva i suoi permessi");
        assertEquals("aggiunto", Files.readString(radice.resolve("nuovo.txt")));
        assertEquals("dati dell'utente", Files.readString(radice.resolve("movimenti.crypto.db")));
        assertTrue(Files.exists(riavviato), "il comando di riavvio va eseguito");
    }

    //=== ESITO ===

    @Test
    void leggiEPulisci_riuscito_cancellaTutto() throws IOException {
        Path c = Files.createDirectories(tmp.resolve("Aggiornamento"));
        Files.createDirectories(c.resolve("1.0.66/estratto"));
        Files.writeString(c.resolve("1.0.66/pacchetto.zip"), "x");
        Files.writeString(c.resolve(AggiornamentoInstallazione.FILE_LOG), "log");
        AggiornamentoInstallazione.ScriviInCorso(c, "1.0.65", "1.0.66");

        AggiornamentoInstallazione.EsitoPrecedente e = AggiornamentoInstallazione.LeggiEPulisci(c, "1.0.66");
        assertTrue(e.Riuscito());
        try (var s = Files.list(c)) {
            assertEquals(0, s.count());
        }
        assertNull(AggiornamentoInstallazione.LeggiEPulisci(c, "1.0.66"), "una volta letto, l'esito non si ripete");
    }

    @Test
    void leggiEPulisci_nonRiuscito_conservaIlLog() throws IOException {
        Path c = Files.createDirectories(tmp.resolve("Aggiornamento"));
        Files.writeString(c.resolve("pacchetto.exe"), "x");
        Files.writeString(c.resolve(AggiornamentoInstallazione.FILE_LOG), "log");
        AggiornamentoInstallazione.ScriviInCorso(c, "1.0.65.07", "1.0.65.08");

        AggiornamentoInstallazione.EsitoPrecedente e = AggiornamentoInstallazione.LeggiEPulisci(c, "1.0.65.07");
        assertFalse(e.Riuscito());
        assertEquals("1.0.65.08", e.A());
        assertTrue(Files.exists(c.resolve(AggiornamentoInstallazione.FILE_LOG)));
        assertFalse(Files.exists(c.resolve("pacchetto.exe")));
    }

    @Test
    void leggiEPulisci_scaricamentoAnnullato_cancellaIlPacchetto() throws IOException {
        //Annullato a "Chiudi e installa": nessun in_corso.txt, ma il pacchetto scaricato non deve restare
        Path c = Files.createDirectories(tmp.resolve("Aggiornamento"));
        Files.createDirectories(c.resolve("1.0.66/estratto"));
        Files.writeString(c.resolve("1.0.66/pacchetto.zip"), "x");
        assertNull(AggiornamentoInstallazione.LeggiEPulisci(c, "1.0.65"));
        try (var s = Files.list(c)) {
            assertEquals(0, s.count());
        }
    }

    /** Una copia non riuscita riavvia comunque la versione precedente, che segnalerà il fallimento. */
    @Test
    @EnabledOnOs(OS.LINUX)
    void scriptLinux_copiaNonRiuscita_riavviaComunque() throws Exception {
        Path riavviato = tmp.resolve("riavviato");
        String script = AggiornamentoInstallazione.ScriptLinux(999999999L, tmp.resolve("inesistente"),
                tmp.resolve("radice"), List.of("/bin/sh", "-c", "echo ok > \"$0\"", riavviato.toString()), tmp,
                tmp.resolve("log.txt"));
        Path file = Files.writeString(tmp.resolve("aggiorna.sh"), script);
        assertTrue(new ProcessBuilder("/bin/sh", file.toString()).start().waitFor(20, TimeUnit.SECONDS));
        for (int i = 0; i < 50 && !Files.exists(riavviato); i++) Thread.sleep(100);
        assertTrue(Files.exists(riavviato));
        assertTrue(Files.readString(tmp.resolve("log.txt")).contains("Copia non riuscita"));
    }

    @Test
    void leggiEPulisci_nessunAggiornamento_null() throws IOException {
        assertNull(AggiornamentoInstallazione.LeggiEPulisci(Files.createDirectories(tmp.resolve("vuota")), "1.0.66"));
        assertNull(AggiornamentoInstallazione.LeggiEPulisci(tmp.resolve("inesistente"), "1.0.66"));
    }

    @Test
    void scarica_senzaSha_rifiutato() {
        Aggiornamenti.Allegato a = new Aggiornamenti.Allegato("x.zip", "https://x/x.zip", "", 1);
        IOException ex = assertThrows(IOException.class,
                () -> AggiornamentoInstallazione.Scarica(a, tmp.resolve("x.zip"), b -> {}, () -> false));
        assertTrue(ex.getMessage().contains("sha256"));
    }
}
