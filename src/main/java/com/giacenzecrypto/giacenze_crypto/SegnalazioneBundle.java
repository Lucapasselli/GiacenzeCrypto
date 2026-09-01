package com.giacenzecrypto.giacenze_crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Costruisce il testo da spedire con una segnalazione di log, in due modalit&agrave; (Decisione 4
 * di nocommit/Documentazione/Analisi_Segnalazioni_Log.md), e lo comprime in gzip.
 *
 * <ul>
 *   <li><b>Bundle diagnostico</b> — intestazione (versione/OS/Java/data), elenco delle
 *       configurazioni di import presenti, e le ultime righe del solo log corrente, tutto passato
 *       per {@link SegnalazioneScrub}. Niente log grezzo integrale.</li>
 *   <li><b>Log completo redatto</b> — il file di log corrente e, se selezionati, i ruotati
 *       {@code .1}…{@code .5}, ognuno con la sua intestazione, tutto passato per la redazione.</li>
 * </ul>
 *
 * Il testo prodotto qui &egrave; solo il valore iniziale mostrato nell'anteprima: ci&ograve; che
 * viene realmente spedito &egrave; il testo come l'utente lo lascia dopo averlo eventualmente
 * modificato (vedi {@link GUI_InviaLog}).
 */
public final class SegnalazioneBundle {

    /** Numero massimo di log ruotati selezionabili. */
    public static final int MAX_RUOTATI = 5;
    /** Righe di coda del log corrente incluse nel bundle diagnostico. */
    private static final int RIGHE_BUNDLE = 400;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SegnalazioneBundle() {
    }

    /**
     * @return i file di log dell'applicazione ({@code GiacenzeCrypto.log*} nella directory di
     *         lavoro), ordinati dal pi&ugrave; recente (quello corrente) al pi&ugrave; vecchio.
     *         Lista vuota se non ne esiste nessuno.
     */
    public static List<File> fileLog() {
        File cartella = new File(VarStatiche.getWorkingDirectory());
        File[] trovati = cartella.listFiles((dir, nome) -> nome.startsWith("GiacenzeCrypto.log"));
        if (trovati == null || trovati.length == 0) {
            return List.of();
        }
        List<File> lista = new ArrayList<>(Arrays.asList(trovati));
        lista.sort(Comparator.comparingLong(File::lastModified).reversed());
        return lista;
    }

    private static String leggiFile(File f) {
        try {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ex) {
            return "[impossibile leggere " + f.getName() + ": " + ex.getMessage() + "]";
        }
    }

    private static String intestazione() {
        return "Versione: " + VarStatiche.Versione + "\n"
                + "Sistema: " + System.getProperty("os.name", "?") + " "
                + System.getProperty("os.version", "?") + " (" + System.getProperty("os.arch", "?") + ")\n"
                + "Java: " + System.getProperty("java.version", "?") + "\n"
                + "Data: " + LocalDateTime.now().format(DATA) + "\n";
    }

    /**
     * @return il testo del "log completo redatto": sempre il file corrente, pi&ugrave; i ruotati
     *         per cui {@code includiRuotati[i]} &egrave; {@code true} (i = 0 → primo ruotato).
     */
    public static String logCompleto(boolean[] includiRuotati) {
        List<File> files = fileLog();
        StringBuilder sb = new StringBuilder();
        sb.append("=== SEGNALAZIONE GIACENZE CRYPTO — LOG COMPLETO (REDATTO) ===\n");
        sb.append(intestazione()).append("\n");
        if (files.isEmpty()) {
            sb.append("(nessun file di log trovato in ").append(VarStatiche.getWorkingDirectory()).append(")\n");
            return sb.toString();
        }
        for (int i = 0; i < files.size(); i++) {
            boolean includi = (i == 0)
                    || (includiRuotati != null && i - 1 < includiRuotati.length && includiRuotati[i - 1]);
            if (!includi) {
                continue;
            }
            sb.append("===== ").append(files.get(i).getName()).append(" =====\n");
            sb.append(SegnalazioneScrub.redigi(leggiFile(files.get(i)))).append("\n\n");
        }
        return sb.toString();
    }

    /** @return il testo del "bundle diagnostico". */
    public static String bundleDiagnostico() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SEGNALAZIONE GIACENZE CRYPTO — BUNDLE DIAGNOSTICO ===\n");
        sb.append(intestazione()).append("\n");

        sb.append("--- Configurazioni di import presenti ---\n");
        File cfgDir = new File(VarStatiche.getCartella_ImportConfig());
        File[] cfg = cfgDir.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".json"));
        if (cfg == null || cfg.length == 0) {
            sb.append("(nessuna)\n");
        } else {
            Arrays.sort(cfg, Comparator.comparing(File::getName));
            for (File f : cfg) {
                sb.append(f.getName()).append("\n");
            }
        }

        sb.append("\n--- Ultime ").append(RIGHE_BUNDLE).append(" righe del log corrente (redatte) ---\n");
        List<File> log = fileLog();
        if (log.isEmpty()) {
            sb.append("(nessun file di log trovato)\n");
        } else {
            sb.append(ultimeRighe(SegnalazioneScrub.redigi(leggiFile(log.get(0))), RIGHE_BUNDLE));
        }
        return sb.toString();
    }

    private static String ultimeRighe(String testo, int quante) {
        String[] righe = testo.split("\n", -1);
        int da = Math.max(0, righe.length - quante);
        StringBuilder sb = new StringBuilder();
        for (int i = da; i < righe.length; i++) {
            sb.append(righe[i]).append('\n');
        }
        return sb.toString();
    }

    /** Comprime {@code testo} (UTF-8) in gzip. */
    public static byte[] gzip(String testo) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(testo.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return bos.toByteArray();
    }
}
