package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compone {@code docs/llms-full.txt}: tutti i manuali di {@code docs/documentazione/} in un solo file di testo, per
 * chi li fa leggere a un'IA (convenzione llms.txt). L'elenco e l'ordine dei manuali li decide {@code docs/llms.txt},
 * che e' scritto a mano: qui si prendono, nell'ordine, i suoi collegamenti a {@code documentazione/<nome>.html} che
 * stanno fuori dalla sezione {@code ## Optional}.
 * <p>Lanciato da {@code docs/strumenti/genera-pdf.sh} insieme ai PDF, non fa parte dell'applicazione.
 * {@code DocumentazionePerIATest} confronta il file pubblicato con {@link #TestoCompleto}: un manuale modificato
 * senza rigenerarlo fa fallire i test.
 */
public class DocumentazionePerIA {

    private static final Pattern MANUALE = Pattern.compile(
            "\\]\\(" + Pattern.quote(DocumentiAiuto.BASE) + "([a-z0-9-]+)\\.html\\)");
    private static final Pattern FRONT_MATTER = Pattern.compile("^---\\n.*?\\n---\\n+", Pattern.DOTALL);
    private static final Pattern ANCORA = Pattern.compile("[ \\t]*\\{#[^}]*\\}[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern LINK_RELATIVO = Pattern.compile("\\]\\((?!https?:|mailto:)([^)\\s]+)\\)");
    private static final String RIGA = "=".repeat(80);

    private DocumentazionePerIA() {
    }

    public static void main(String[] args) throws Exception {
        File docs = new File(args.length > 0 ? args[0] : "docs");
        Files.writeString(new File(docs, "llms-full.txt").toPath(), TestoCompleto(docs), StandardCharsets.UTF_8);
        System.out.println("scritto llms-full.txt");
    }

    /** I nomi dei manuali (senza estensione) collegati da {@code llms.txt}, nel suo ordine, sezione Optional esclusa */
    static List<String> Manuali(File docs) throws IOException {
        String indice = Files.readString(new File(docs, "llms.txt").toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        int opzionale = indice.indexOf("\n## Optional");
        if (opzionale >= 0) indice = indice.substring(0, opzionale);
        List<String> nomi = new ArrayList<>();
        Matcher m = MANUALE.matcher(indice);
        while (m.find()) {
            if (!nomi.contains(m.group(1))) nomi.add(m.group(1));
        }
        return nomi;
    }

    /** Il contenuto di {@code llms-full.txt} per la cartella {@code docs} */
    static String TestoCompleto(File docs) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Giacenze Crypto - manuali completi\n\n");
        sb.append("> Tutti i manuali del programma Giacenze Crypto in un solo file, nell'ordine di ")
                .append(DocumentiAiuto.BASE.replace("documentazione/", "llms.txt")).append(" .\n");
        sb.append("> Generato dalle pagine Markdown di docs/documentazione/ da docs/strumenti/genera-pdf.sh: non modificarlo a mano.\n");
        for (String nome : Manuali(docs)) {
            File md = new File(docs, "documentazione/" + nome + ".md");
            String testo = Files.readString(md.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            testo = FRONT_MATTER.matcher(testo).replaceFirst("");
            testo = ANCORA.matcher(testo).replaceAll("");
            testo = testo.replaceAll("(?m)^\\[Torna all'indice della documentazione\\]\\(\\./\\)\\s*$", "");
            String pagina = DocumentiAiuto.BASE + nome + ".html";
            testo = LINK_RELATIVO.matcher(testo).replaceAll(r -> {
                String destinazione = r.group(1);
                String assoluto = destinazione.startsWith("#") ? pagina + destinazione
                        : DocumentiAiuto.BASE + destinazione.replaceFirst("^\\./", "");
                return Matcher.quoteReplacement("](" + assoluto + ")");
            });
            sb.append("\n").append(RIGA).append("\n");
            sb.append("Fonte: ").append(pagina).append("\n");
            sb.append(RIGA).append("\n\n");
            sb.append(testo.strip()).append("\n");
        }
        return sb.toString();
    }
}
