package com.giacenzecrypto.giacenze_crypto;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author lucap
 */
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.graalvm.polyglot.*;

public class CcxtInterop {
    
    private static final String NODE_VERSION = "v20.13.1";
    private static final Path NODE_DIR = Paths.get(Statiche.getWorkingDirectory()+"tools", "node").toAbsolutePath().normalize();;
    

    
    public static void ensureNodeInstalled() throws IOException {
        if (Files.exists(NODE_DIR)) {
            System.out.println("✅ Node.js già presente");
            return;
        }

        System.out.println("⬇️ Scarico Node.js standalone...");
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").contains("64") ? "x64" : "x86";

        String platform;
        String extension;
        if (os.contains("win")) {
            platform = "win-" + arch;
            extension = "zip";
        } else if (os.contains("mac")) {
            platform = "darwin-" + arch;
            extension = "tar.xz";
        } else {
            platform = "linux-" + arch;
            extension = "tar.xz";
        }

        String filename = "node-" + NODE_VERSION + "-" + platform + "." + extension;
        String url = "https://nodejs.org/dist/" + NODE_VERSION + "/" + filename;

        Path downloadPath = Paths.get(Statiche.getPathRisorse()+"tools", filename);
        Files.createDirectories(downloadPath.getParent());

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Scaricato: " + filename);
        extractArchive(downloadPath, NODE_DIR, extension);
        System.out.println("Estratto Node.js");

        // su Windows, dentro la cartella estratta ci sarà node.exe + npm.cmd
        // su Linux/macOS, sono in /bin
    }
    
    
    public static void installCcxt() throws IOException, InterruptedException {
    Path nodeModulesDir = NODE_DIR.resolve("node_modules");
    Path ccxtDir = nodeModulesDir.resolve("ccxt");

    if (Files.exists(ccxtDir) && Files.isDirectory(ccxtDir)) {
        System.out.println("CCXT già installato in: " + ccxtDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    Path nodePath = getNodeExePath();

    System.out.println("Installo ccxt...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", "ccxt");
    System.out.println(Statiche.getWorkingDirectory() + "tools/node");
    builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
    System.out.println("Comando: " + String.join(" ", builder.command()));
    System.out.println("Working directory: " + builder.directory().getAbsolutePath());
    Map<String, String> env = builder.environment();

    // Inserisci la directory che contiene node.exe nel PATH
    String currentPath = env.get("PATH");
    env.put("PATH", nodePath.getParent().toAbsolutePath().toString() + File.pathSeparator + currentPath);

    Process process = builder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
        String line;
        while ((line = reader.readLine()) != null) System.out.println("[npm] " + line);
        while ((line = errReader.readLine()) != null) System.err.println("[npm-err] " + line);
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) throw new RuntimeException("npm install ccxt fallito");
    System.out.println("ccxt installato con successo");
}

    private static Path getNpmPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-win-x64").resolve("npm.cmd");
        } else {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-" + (os.contains("mac") ? "darwin-x64" : "linux-x64"))
                           .resolve("bin").resolve("npm");
        }
    }

    
private static Path getNodeExePath() {
    String os = System.getProperty("os.name").toLowerCase();
    String nodeExecutable = os.contains("win") ? "node.exe" : "node";

    // Supponiamo di avere le distribuzioni in una cartella "nodejs" interna al progetto
    // es. nodejs/node-v18.20.3-win-x64/node.exe oppure nodejs/node-v18.20.3-linux-x64/bin/node
    String baseName;
    if (os.contains("win")) {
        baseName = "node-" + NODE_VERSION + "-win-x64";
        return NODE_DIR.resolve(baseName).resolve("node.exe");
    } else if (os.contains("mac")) {
        baseName = "node-" + NODE_VERSION + "-darwin-x64";
        return NODE_DIR.resolve(baseName).resolve("bin").resolve("node");
    } else {
        // Linux
        baseName = "node-" + NODE_VERSION + "-linux-x64";
        return NODE_DIR.resolve(baseName).resolve("bin").resolve("node");
    }
}
   
    
    
    private static void extractArchive(Path archive, Path targetDir, String extension) throws IOException {
        if (extension.equals("zip")) {
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(archive.toFile()))) {
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    Path filePath = targetDir.resolve(entry.getName()).normalize();
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zipIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            
        } else {
            // Richiede tar + xz nel sistema (presente su Linux/macOS)
            new ProcessBuilder("tar", "-xf", archive.toString(), "-C", targetDir.getParent().toString())
                .inheritIO().start();
        }
    }
    
    

    public static void fetchMovimenti(String exchangeId, String apiKey, String secret) {
    try {
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get("src/main/resources/js/FetchMovimenti.js");

        if (!Files.exists(nodePath)) {
            System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
            return;
        }
        if (!Files.exists(scriptPath)) {
            System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
            return;
        }

        ProcessBuilder builder = new ProcessBuilder(
                nodePath.toString(),
                scriptPath.toAbsolutePath().toString(),
                exchangeId, apiKey, secret
        );
        builder.directory(scriptPath.getParent().toFile());
        builder.redirectErrorStream(true); // stderr → stdout
        
        // Calcola la cartella base di Node in modo multipiattaforma
        Path nodeBaseDir = nodePath.getParent(); // es: .../node-vXX-PLATFORM[/bin]
        if (!nodeBaseDir.getFileName().toString().equals("bin")) {
            // Se siamo su Windows, node.exe sta direttamente in base dir, altrimenti sotto bin
            nodeBaseDir = nodeBaseDir.getParent();
        }

        Path nodeModulesPath = nodeBaseDir.resolve("node_modules").toAbsolutePath();

        Map<String, String> env = builder.environment();

        // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
        String existingNodePath = env.get("NODE_PATH");
        String newNodePath = nodeModulesPath.toString();
        if (existingNodePath != null && !existingNodePath.isEmpty()) {
            newNodePath += File.pathSeparator + existingNodePath;
        }
        env.put("NODE_PATH", newNodePath);


        Process process = builder.start();


        // Leggi e stampa output dello script
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[Node] " + line);  // ← stampa output da Node.js
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Errore: processo Node.js terminato con codice " + exitCode);
            return;
        }

        // Parsing JSON di risposta con Gson
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(output.toString(), JsonObject.class);

        if (json.has("error") && !json.get("error").isJsonNull()) {
            System.err.println("Errore: " + json.get("error").getAsString());
        } else {
            System.out.println("Depositi:");
            JsonArray deposits = json.getAsJsonArray("deposits");
            for (JsonElement d : deposits) {
                System.out.println(d.toString());
            }

            System.out.println("Prelievi:");
            JsonArray withdrawals = json.getAsJsonArray("withdrawals");
            for (JsonElement w : withdrawals) {
                System.out.println(w.toString());
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}



    public static void main(String[] args) {

    }
}

